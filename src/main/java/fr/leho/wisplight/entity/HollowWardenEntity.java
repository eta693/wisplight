package fr.leho.wisplight.entity;

import fr.leho.wisplight.WisplightWorldData;
import fr.leho.wisplight.registry.ModEntities;
import fr.leho.wisplight.registry.ModSounds;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Le Veilleur Éteint : lanterne géante, 300 PV, trois phases.
 * <ol>
 *   <li>La flamme vacille : orbes téléguidées, coup de chaîne au sol.</li>
 *   <li>Les ombres répondent : invoque des Ombres, éteint les torches proches.</li>
 *   <li>Dernier souffle : obscurité, charges, explosion de lumière à esquiver (derrière un mur).</li>
 * </ol>
 */
public class HollowWardenEntity extends Monster {
    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(HollowWardenEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_CHARGE =
            SynchedEntityData.defineId(HollowWardenEntity.class, EntityDataSerializers.INT);

    public static final float MAX_HEALTH = 300.0F;
    public static final int LEASH_RANGE = 48;
    public static final int BURST_WINDUP = 30;

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            Component.translatable("entity.wisplight.hollow_warden"), BossEvent.BossBarColor.YELLOW,
            BossEvent.BossBarOverlay.NOTCHED_6).setDarkenScreen(true);

    @Nullable
    private BlockPos home;
    private int orbCooldown = 40;
    private int slamCooldown = 60;
    private int summonCooldown = 0;
    private int dashCooldown = 80;
    private int burstCooldown = 120;
    private int aloneTicks;

    public HollowWardenEntity(EntityType<? extends HollowWardenEntity> type, Level level) {
        super(type, level);
        this.xpReward = 100;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PHASE, 1);
        builder.define(DATA_CHARGE, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    public void setHome(BlockPos pos) {
        this.home = pos.immutable();
    }

    /** Phase selon la vie restante : 1 (> 66 %), 2 (33-66 %), 3 (< 33 %). */
    public static int phaseFor(float health, float max) {
        float ratio = health / max;
        return ratio > 0.66F ? 1 : ratio > 0.33F ? 2 : 3;
    }

    public int getPhase() {
        return entityData.get(DATA_PHASE);
    }

    /** Progression de la charge de l'explosion de lumière (0 = aucune), pour le rendu. */
    public int getBurstCharge() {
        return entityData.get(DATA_CHARGE);
    }

    // ------------------------------------------------------------------ boucle

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel level = (ServerLevel) level();
        bossEvent.setProgress(getHealth() / getMaxHealth());

        int phase = phaseFor(getHealth(), getMaxHealth());
        if (phase != getPhase()) {
            enterPhase(level, phase);
        }

        // Le rituel échoue si le joueur s'éloigne
        Player nearest = level.getNearestPlayer(this, LEASH_RANGE);
        if (nearest == null || nearest.isSpectator()) {
            if (++aloneTicks > 100) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.5, getZ(), 40, 0.6, 1.0, 0.6, 0.02);
                discard();
                return;
            }
        } else {
            aloneTicks = 0;
        }

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        if (--orbCooldown <= 0) {
            int count = phase == 3 ? 2 : 1;
            for (int i = 0; i < count; i++) {
                shootOrb(level, target);
            }
            orbCooldown = phase == 1 ? 60 : 80;
        }
        if (--slamCooldown <= 0 && distanceToSqr(target) < 16.0) {
            slam(level);
            slamCooldown = 80;
        }
        if (phase >= 2 && --summonCooldown <= 0) {
            summonShades(level, 2);
            summonCooldown = 240;
        }
        if (phase == 3) {
            if (tickCount % 20 == 0) {
                for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(32))) {
                    p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false), this);
                }
            }
            if (--dashCooldown <= 0) {
                dash(target);
                dashCooldown = 100;
            }
            tickBurst(level);
        }
    }

    private void enterPhase(ServerLevel level, int phase) {
        entityData.set(DATA_PHASE, phase);
        level.playSound(null, getX(), getY(), getZ(), ModSounds.WARDEN_PHASE.get(), SoundSource.HOSTILE, 2.0F, 1.0F);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 1.8, getZ(), 50, 0.8, 1.0, 0.8, 0.05);
        if (phase == 2) {
            bossEvent.setColor(BossEvent.BossBarColor.PURPLE);
            summonShades(level, 3);
            summonCooldown = 240;
            extinguishTorches(level, 12);
        } else if (phase == 3) {
            bossEvent.setColor(BossEvent.BossBarColor.RED);
            burstCooldown = 60;
        }
    }

    private void shootOrb(ServerLevel level, LivingEntity target) {
        WardenOrbEntity orb = new WardenOrbEntity(level, this, target);
        orb.setPos(getX() + (random.nextDouble() - 0.5), getY() + 2.4, getZ() + (random.nextDouble() - 0.5));
        Vec3 dir = target.getEyePosition().subtract(orb.position()).normalize().scale(0.45);
        orb.setDeltaMovement(dir.add(0, 0.2, 0));
        level.addFreshEntity(orb);
        level.playSound(null, getX(), getY(), getZ(), ModSounds.STAFF_SHOOT.get(), SoundSource.HOSTILE, 1.0F, 0.6F);
    }

    /** Coup de chaîne : dégâts et projection autour du Veilleur. */
    private void slam(ServerLevel level) {
        level.playSound(null, getX(), getY(), getZ(), ModSounds.WARDEN_SLAM.get(), SoundSource.HOSTILE, 1.5F, 1.0F);
        level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.2, getZ(), 6, 2.0, 0.1, 2.0, 0.0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(4.0, 1.0, 4.0),
                e -> e != this && !(e instanceof CreepingShadeEntity))) {
            e.hurt(damageSources().mobAttack(this), 8.0F);
            Vec3 push = e.position().subtract(position()).multiply(1, 0, 1).normalize().scale(1.2);
            e.push(push.x, 0.5, push.z);
        }
    }

    public int summonShades(ServerLevel level, int count) {
        int existing = level.getEntitiesOfClass(CreepingShadeEntity.class, getBoundingBox().inflate(24)).size();
        int spawned = 0;
        for (int i = 0; i < count && existing + spawned < 5; i++) {
            CreepingShadeEntity shade = ModEntities.CREEPING_SHADE.get().create(level);
            if (shade == null) {
                continue;
            }
            double angle = random.nextDouble() * Math.PI * 2;
            shade.moveTo(getX() + Math.cos(angle) * 3, getY(), getZ() + Math.sin(angle) * 3, random.nextFloat() * 360F, 0F);
            shade.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            if (getTarget() != null) {
                shade.setTarget(getTarget());
            }
            level.addFreshEntity(shade);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, shade.getX(), shade.getY() + 1, shade.getZ(), 15, 0.3, 0.6, 0.3, 0.01);
            spawned++;
        }
        return spawned;
    }

    /** Éteint torches et lanternes dans un rayon donné (elles tombent au sol). */
    public int extinguishTorches(ServerLevel level, int radius) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(blockPosition().offset(-radius, -4, -radius), blockPosition().offset(radius, 6, radius))) {
            BlockState state = level.getBlockState(p);
            if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH) || state.is(Blocks.SOUL_TORCH)
                    || state.is(Blocks.SOUL_WALL_TORCH) || state.is(Blocks.LANTERN) || state.is(Blocks.SOUL_LANTERN)) {
                level.destroyBlock(p, true);
                level.sendParticles(ParticleTypes.SMOKE, p.getX() + 0.5, p.getY() + 0.6, p.getZ() + 0.5, 6, 0.1, 0.1, 0.1, 0.01);
                n++;
            }
        }
        return n;
    }

    private void dash(LivingEntity target) {
        Vec3 dir = target.position().subtract(position()).multiply(1, 0, 1).normalize();
        setDeltaMovement(dir.x * 1.6, 0.25, dir.z * 1.6);
        hasImpulse = true;
        playSound(ModSounds.WARDEN_SLAM.get(), 1.0F, 1.4F);
    }

    /** Explosion de lumière : 30 ticks de préparation, puis dégâts aux joueurs à vue dans 7 blocs. */
    private void tickBurst(ServerLevel level) {
        int charge = getBurstCharge();
        if (charge == 0) {
            if (--burstCooldown <= 0) {
                entityData.set(DATA_CHARGE, 1);
                level.playSound(null, getX(), getY(), getZ(), ModSounds.WARDEN_CHARGE.get(), SoundSource.HOSTILE, 2.0F, 1.0F);
            }
            return;
        }
        entityData.set(DATA_CHARGE, charge + 1);
        level.sendParticles(new DustParticleOptions(new Vector3f(1.0F, 0.85F, 0.4F), 1.5F),
                getX(), getY() + 1.8, getZ(), 6, 0.6 + charge * 0.05, 0.8, 0.6 + charge * 0.05, 0.0);
        if (charge >= BURST_WINDUP) {
            entityData.set(DATA_CHARGE, 0);
            burstCooldown = 140;
            level.playSound(null, getX(), getY(), getZ(), ModSounds.WARDEN_BURST.get(), SoundSource.HOSTILE, 2.5F, 1.0F);
            level.sendParticles(ParticleTypes.FLASH, getX(), getY() + 1.8, getZ(), 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.8, getZ(), 80, 0.3, 0.3, 0.3, 0.4);
            Vec3 core = position().add(0, 1.8, 0);
            for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(7.0))) {
                HitResult sight = level.clip(new ClipContext(core, p.getEyePosition(), ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, this));
                if (sight.getType() == HitResult.Type.MISS) {
                    p.hurt(damageSources().indirectMagic(this, this), 10.0F);
                    p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), this);
                }
            }
        }
    }

    // ------------------------------------------------------------------ dégâts & mort

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean lumen = source.getDirectEntity() instanceof WispOrbEntity
                || (source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker
                    && CreepingShadeEntity.isLumenWeapon(attacker.getMainHandItem()));
        return super.hurt(source, lumen ? amount * 1.25F : amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel server) {
            WisplightWorldData.get(server.getServer()).setRekindled(true);
            for (Player p : server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(48))) {
                p.displayClientMessage(Component.translatable("message.wisplight.warden.defeated"), false);
            }
            server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.8, getZ(), 120, 0.5, 1.0, 0.5, 0.3);
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        bossEvent.setName(getDisplayName());
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canChangeDimensions(Level from, Level to) {
        return false;
    }

    @Override
    protected boolean canRide(net.minecraft.world.entity.Entity vehicle) {
        return false;
    }

    @Override
    public boolean addEffect(MobEffectInstance effect, @Nullable net.minecraft.world.entity.Entity source) {
        return false; // insensible aux effets
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) {
            tag.put("Home", NbtUtils.writeBlockPos(home));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NbtUtils.readBlockPos(tag, "Home").ifPresent(p -> home = p);
        if (hasCustomName()) {
            bossEvent.setName(getDisplayName());
        }
        entityData.set(DATA_PHASE, phaseFor(getHealth(), getMaxHealth()));
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.WARDEN_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.WARDEN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.WARDEN_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 2.0F;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            int phase = getPhase();
            Vector3f color = phase == 1 ? new Vector3f(0.8F, 0.9F, 1.0F)
                    : phase == 2 ? new Vector3f(0.6F, 0.4F, 0.9F) : new Vector3f(1.0F, 0.5F, 0.2F);
            if (random.nextInt(2) == 0) {
                level().addParticle(new DustParticleOptions(color, 1.0F),
                        getRandomX(0.6), getY() + 1.2 + random.nextDouble(), getRandomZ(0.6), 0, 0.02, 0);
            }
        }
    }

    /** Liste des Ombres invoquées à portée (pour les tests). */
    public List<CreepingShadeEntity> nearbyShades() {
        return level().getEntitiesOfClass(CreepingShadeEntity.class, getBoundingBox().inflate(24));
    }
}
