package fr.leho.wisplight.entity;

import fr.leho.wisplight.WisplightConfig;
import fr.leho.wisplight.block.TraceLightBlock;
import fr.leho.wisplight.registry.ModItems;
import fr.leho.wisplight.registry.ModSounds;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Feu follet : petite créature volante et passive. Fuit les joueurs menaçants,
 * s'approche des joueurs accroupis, éclaire autour d'elle et se capture avec un bocal vide.
 */
public class WispEntity extends Mob {
    private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(WispEntity.class, EntityDataSerializers.INT);

    @Nullable
    private Vec3 target;
    private boolean fleeing;
    @Nullable
    private BlockPos attractor;
    private int attractTicks;

    public WispEntity(EntityType<? extends WispEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FLYING_SPEED, 0.4);
    }

    // ------------------------------------------------------------------ données

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, WispVariant.AZUR.ordinal());
    }

    public WispVariant getVariant() {
        return WispVariant.byIndex(entityData.get(DATA_VARIANT));
    }

    public void setVariant(WispVariant variant) {
        entityData.set(DATA_VARIANT, variant.ordinal());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Variant", getVariant().id());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setVariant(WispVariant.byId(tag.getString("Variant")));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData groupData) {
        setVariant(WispVariant.forBiome(level.getBiome(blockPosition())));
        return super.finalizeSpawn(level, difficulty, spawnType, groupData);
    }

    // ------------------------------------------------------------------ apparition

    public static boolean checkWispSpawnRules(EntityType<WispEntity> type, ServerLevelAccessor level,
                                              MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (spawnType == MobSpawnType.SPAWNER || spawnType == MobSpawnType.SPAWN_EGG || spawnType == MobSpawnType.COMMAND) {
            return true;
        }
        if (!level.getBlockState(pos).isAir()) {
            return false;
        }
        int nearby = level.getEntitiesOfClass(WispEntity.class, new AABB(pos).inflate(32.0)).size();
        if (nearby >= WisplightConfig.maxWispsNearby()) {
            return false;
        }
        boolean nether = level.getBiome(pos).is(WispVariant.BRAISE.biomeTag());
        if (nether) {
            return random.nextInt(3) == 0;
        }
        ServerLevel server = level.getLevel();
        return !server.isDay()
                && server.canSeeSky(pos)
                && random.nextInt(2) == 0;
    }

    // ------------------------------------------------------------------ comportement

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        Level level = level();
        Vec3 pos = position();

        Player player = level.getNearestPlayer(this, 10.0);
        fleeing = false;
        if (player != null && !player.isSpectator() && scares(player)) {
            Vec3 away = pos.subtract(player.position()).multiply(1, 0, 1);
            if (away.lengthSqr() < 1.0E-4) {
                away = new Vec3(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5);
            }
            away = away.normalize();
            target = pos.add(away.x * 7.0, 1.5, away.z * 7.0);
            fleeing = true;
        } else if (player != null && player.isShiftKeyDown() && player.distanceToSqr(this) < 49.0) {
            target = player.getEyePosition().add(player.getLookAngle().scale(1.6));
        } else if (attractor != null && attractTicks > 0) {
            if (target == null || random.nextInt(40) == 0 || target.distanceToSqr(pos) < 0.8) {
                double angle = random.nextDouble() * Math.PI * 2;
                target = Vec3.atCenterOf(attractor).add(Math.cos(angle) * 2.2, 0.8 + random.nextDouble(), Math.sin(angle) * 2.2);
            }
        } else if (target == null || random.nextInt(80) == 0 || target.distanceToSqr(pos) < 0.8) {
            target = pickWanderTarget();
        }
        if (attractTicks > 0 && --attractTicks == 0) {
            attractor = null;
        }

        Vec3 diff = target.subtract(pos);
        double dist = diff.length();
        double speed = fleeing ? 0.11 : 0.035;
        Vec3 desired = dist > 1.0E-3 ? diff.scale(Math.min(speed, dist) / dist) : Vec3.ZERO;
        Vec3 motion = getDeltaMovement();
        setDeltaMovement(motion.add(desired.subtract(motion).scale(0.18)));
        if (dist > 0.1) {
            float yaw = (float) (Mth.atan2(diff.z, diff.x) * Mth.RAD_TO_DEG) - 90.0F;
            setYRot(yaw);
            yBodyRot = yaw;
            yHeadRot = yaw;
        }

        // Lumière dynamique
        if (WisplightConfig.dynamicLight() && tickCount % 4 == 0) {
            TraceLightBlock.refresh(level, BlockPos.containing(getX(), getY() + 0.25, getZ()));
        }

        // Le jour, les follets de surface se dissipent
        if (!getVariant().isNether() && level.isDay() && !isPersistenceRequired() && !hasCustomName()
                && level.canSeeSky(blockPosition()) && random.nextInt(200) == 0) {
            dissipate();
        }
    }

    /** Attire le follet vers une lanterne d'appel pendant un certain nombre de ticks. */
    public void attractTo(BlockPos pos, int ticks) {
        if (attractor == null || !attractor.equals(pos)) {
            target = null;
        }
        attractor = pos.immutable();
        attractTicks = Math.max(attractTicks, ticks);
    }

    @Nullable
    public BlockPos getAttractor() {
        return attractTicks > 0 ? attractor : null;
    }

    private Vec3 pickWanderTarget() {
        double x = getX() + (random.nextDouble() - 0.5) * 12.0;
        double z = getZ() + (random.nextDouble() - 0.5) * 12.0;
        double ground = findGroundY(BlockPos.containing(x, getY(), z));
        double y = ground + 1.5 + random.nextDouble() * 2.5;
        return new Vec3(x, y, z);
    }

    /** Cherche le sol sous une position (jusqu'à 10 blocs), sinon garde l'altitude actuelle. */
    private double findGroundY(BlockPos start) {
        BlockPos.MutableBlockPos cursor = start.mutable();
        for (int i = 0; i < 10; i++) {
            BlockState state = level().getBlockState(cursor);
            if (!state.isAir() && !state.getCollisionShape(level(), cursor).isEmpty()) {
                return cursor.getY() + 1.0;
            }
            cursor.move(0, -1, 0);
        }
        return getY() - 2.0;
    }

    /** Un joueur qui sprinte ou tient une arme fait fuir le follet (sauf s'il est accroupi). */
    public static boolean scares(Player player) {
        if (player.isShiftKeyDown()) {
            return false;
        }
        return player.isSprinting() || isWeapon(player.getMainHandItem());
    }

    public static boolean isWeapon(ItemStack stack) {
        return stack.getItem() instanceof SwordItem
                || stack.getItem() instanceof AxeItem
                || stack.getItem() instanceof TridentItem
                || stack.getItem() instanceof MaceItem
                || stack.getItem() instanceof ProjectileWeaponItem;
    }

    public boolean isFleeing() {
        return fleeing;
    }

    /** Disparition en fumée (lever du jour). */
    public void dissipate() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.25, getZ(), 8, 0.2, 0.2, 0.2, 0.02);
            playSound(ModSounds.WISP_RELEASE.get(), 0.6F, 1.4F);
        }
        discard();
    }

    // ------------------------------------------------------------------ capture

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(ModItems.EMPTY_JAR.get())) {
            return super.mobInteract(player, hand);
        }
        if (!player.isShiftKeyDown()) {
            if (!level().isClientSide) {
                player.displayClientMessage(Component.translatable("message.wisplight.wisp_restless"), true);
            }
            return InteractionResult.FAIL;
        }
        if (!level().isClientSide) {
            capture(player, stack);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    /** Met le follet en bocal et donne le bocal au joueur. */
    public void capture(Player player, ItemStack jarStack) {
        ItemStack filled = new ItemStack(ModItems.WISP_JARS.get(getVariant()).get());
        if (!player.getAbilities().instabuild) {
            jarStack.shrink(1);
        }
        if (!player.getInventory().add(filled)) {
            player.drop(filled, false);
        }
        level().playSound(null, getX(), getY(), getZ(), ModSounds.WISP_CAPTURE.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        if (level() instanceof ServerLevel server) {
            WispVariant v = getVariant();
            server.sendParticles(new DustParticleOptions(new Vector3f(v.red(), v.green(), v.blue()), 1.0F),
                    getX(), getY() + 0.25, getZ(), 16, 0.25, 0.25, 0.25, 0.02);
        }
        discard();
    }

    // ------------------------------------------------------------------ client & divers

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && random.nextInt(2) == 0) {
            WispVariant v = getVariant();
            level().addParticle(new DustParticleOptions(new Vector3f(v.red(), v.green(), v.blue()), 0.6F),
                    getX() + (random.nextDouble() - 0.5) * 0.3,
                    getY() + 0.1 + random.nextDouble() * 0.3,
                    getZ() + (random.nextDouble() - 0.5) * 0.3,
                    0.0, 0.0, 0.0);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide && source.getEntity() instanceof Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 1200, 0));
        }
    }

    @Override
    public boolean fireImmune() {
        return getVariant().isNether() || super.fireImmune();
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !isPersistenceRequired() && !hasCustomName();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.WISP_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.WISP_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.WISP_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.5F;
    }
}
