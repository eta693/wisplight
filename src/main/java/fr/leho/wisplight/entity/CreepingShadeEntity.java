package fr.leho.wisplight.entity;

import fr.leho.wisplight.item.WatcherArmor;
import fr.leho.wisplight.registry.ModSounds;
import fr.leho.wisplight.registry.ModTags;
import fr.leho.wisplight.registry.ModTiers;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Ombre rampante : naît dans le noir total, chasse les follets et les joueurs, recule devant
 * la lumière vive. Les armes en lumen et les orbes lui infligent des dégâts doublés.
 */
public class CreepingShadeEntity extends Monster {
    public static final int LIGHT_FEAR_LEVEL = 12;
    public static final float STEAL_CHANCE = 0.15F;

    private final java.util.List<ItemStack> stolen = new java.util.ArrayList<>();

    public CreepingShadeEntity(EntityType<? extends CreepingShadeEntity> type, Level level) {
        super(type, level);
        this.xpReward = 7;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new FleeLightGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                target -> !(target instanceof Player p) || !WatcherArmor.hasFullSet(p) || p.distanceToSqr(this) < 16.0));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, WispEntity.class, 10, true, false, null));
    }

    // ------------------------------------------------------------------ apparition

    public static boolean checkShadeSpawnRules(EntityType<CreepingShadeEntity> type, ServerLevelAccessor level,
                                               MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (!Monster.checkMonsterSpawnRules(type, level, spawnType, pos, random)) {
            return false;
        }
        if (spawnType != MobSpawnType.NATURAL) {
            return true;
        }
        if (level.getRawBrightness(pos, 0) > 0) {
            return false;
        }
        if (pos.getY() < 0) {
            return true;
        }
        boolean wispsNearby = !level.getEntitiesOfClass(WispEntity.class, new AABB(pos).inflate(24.0)).isEmpty();
        return wispsNearby ? random.nextInt(3) == 0 : random.nextInt(10) == 0;
    }

    // ------------------------------------------------------------------ combat

    public static boolean isLumenWeapon(ItemStack stack) {
        return stack.getItem() instanceof TieredItem tiered && tiered.getTier() == ModTiers.LUMEN;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean lumen = source.getDirectEntity() instanceof WispOrbEntity
                || (source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker
                    && isLumenWeapon(attacker.getMainHandItem()));
        return super.hurt(source, lumen ? amount * 2.0F : amount);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0), this);
            if (target instanceof Player player && random.nextFloat() < STEAL_CHANCE && level().getDifficulty() != Difficulty.PEACEFUL) {
                stealEssence(player);
            }
        }
        return hit;
    }

    /** Vole une essence dans l'inventaire du joueur ; elle sera rendue à la mort de l'Ombre. */
    public boolean stealEssence(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModTags.ESSENCES)) {
                stolen.add(stack.split(1));
                playSound(ModSounds.SHADE_STEAL.get(), 1.0F, 1.0F);
                return true;
            }
        }
        return false;
    }

    public int stolenCount() {
        return stolen.size();
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        for (ItemStack stack : stolen) {
            spawnAtLocation(stack);
        }
        stolen.clear();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && random.nextInt(3) == 0) {
            level().addParticle(ParticleTypes.SMOKE, getRandomX(0.5), getRandomY(), getRandomZ(0.5), 0, 0.01, 0);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ListTag list = new ListTag();
        for (ItemStack stack : stolen) {
            if (!stack.isEmpty()) {
                list.add(stack.save(registryAccess()));
            }
        }
        tag.put("Stolen", list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        stolen.clear();
        ListTag list = tag.getList("Stolen", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            ItemStack.parse(registryAccess(), list.getCompound(i)).ifPresent(stolen::add);
        }
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.SHADE_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SHADE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SHADE_DEATH.get();
    }

    // ------------------------------------------------------------------ peur de la lumière

    /** Fuit vers un endroit plus sombre quand la lumière dépasse le seuil. */
    static final class FleeLightGoal extends Goal {
        private final CreepingShadeEntity shade;
        @Nullable
        private Vec3 refuge;

        FleeLightGoal(CreepingShadeEntity shade) {
            this.shade = shade;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        private int light(BlockPos pos) {
            return shade.level().getMaxLocalRawBrightness(pos);
        }

        @Override
        public boolean canUse() {
            if (light(shade.blockPosition()) < LIGHT_FEAR_LEVEL) {
                return false;
            }
            for (int i = 0; i < 8; i++) {
                Vec3 candidate = DefaultRandomPos.getPos(shade, 10, 4);
                if (candidate != null && light(BlockPos.containing(candidate)) < LIGHT_FEAR_LEVEL - 4) {
                    refuge = candidate;
                    return true;
                }
            }
            return false;
        }

        @Override
        public void start() {
            if (refuge != null) {
                shade.getNavigation().moveTo(refuge.x, refuge.y, refuge.z, 1.3);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return !shade.getNavigation().isDone();
        }
    }
}
