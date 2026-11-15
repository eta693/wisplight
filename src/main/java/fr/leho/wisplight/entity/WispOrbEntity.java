package fr.leho.wisplight.entity;

import fr.leho.wisplight.block.LingeringGlowBlock;
import fr.leho.wisplight.infusion.InfusionEffects;
import fr.leho.wisplight.registry.ModEntities;
import fr.leho.wisplight.registry.ModSounds;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Vector3f;

/** Orbe tiré par le bâton des follets. Variante -1 = orbe simple (lumière courte). */
public class WispOrbEntity extends ThrowableProjectile {
    private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(WispOrbEntity.class, EntityDataSerializers.INT);
    public static final int PLAIN_GLOW_TICKS = 600;
    public static final int LIGHT_GLOW_TICKS = 2400;
    public static final float BASE_DAMAGE = 4.0F;
    private float damageBonus;

    public WispOrbEntity(EntityType<? extends WispOrbEntity> type, Level level) {
        super(type, level);
    }

    public WispOrbEntity(Level level, LivingEntity owner, @Nullable WispVariant variant) {
        super(ModEntities.WISP_ORB.get(), owner, level);
        setOrbVariant(variant);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_VARIANT, -1);
    }

    @Nullable
    public WispVariant getOrbVariant() {
        int v = entityData.get(DATA_VARIANT);
        return v < 0 ? null : WispVariant.byIndex(v);
    }

    public void setOrbVariant(@Nullable WispVariant variant) {
        entityData.set(DATA_VARIANT, variant == null ? -1 : variant.ordinal());
    }

    public void setDamageBonus(float bonus) {
        this.damageBonus = bonus;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            WispVariant v = getOrbVariant();
            Vector3f color = v == null ? new Vector3f(1.0F, 0.89F, 0.48F) : new Vector3f(v.red(), v.green(), v.blue());
            level().addParticle(new DustParticleOptions(color, 0.9F), getX(), getY() + 0.1, getZ(), 0, 0, 0);
        } else if (tickCount > 80) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        WispVariant v = getOrbVariant();
        Entity owner = getOwner();
        if (v == WispVariant.EMERAUDE && target instanceof LivingEntity living && !(target instanceof Enemy)) {
            living.heal(4.0F + damageBonus / 2.0F);
            return;
        }
        float damage = (v == WispVariant.BRAISE ? BASE_DAMAGE + 1.0F : BASE_DAMAGE) + damageBonus;
        target.hurt(damageSources().thrown(this, owner), damage);
        if (target instanceof LivingEntity living) {
            if (v == WispVariant.AZUR || v == null) {
                living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
            } else if (v == WispVariant.BRAISE) {
                living.igniteForSeconds(5.0F);
            } else if (v == WispVariant.GIVRE) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        BlockPos spot = result.getBlockPos().relative(result.getDirection());
        WispVariant v = getOrbVariant();
        if (v == WispVariant.BRAISE) {
            if (level().getBlockState(spot).isAir()) {
                level().setBlockAndUpdate(spot, BaseFireBlock.getState(level(), spot));
            }
        } else if (v == WispVariant.GIVRE) {
            InfusionEffects.freezeWater(level(), result.getBlockPos(), 2);
        } else if (v == null || v == WispVariant.AZUR) {
            LingeringGlowBlock.place(level(), spot, v == null ? PLAIN_GLOW_TICKS : LIGHT_GLOW_TICKS);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            level().playSound(null, getX(), getY(), getZ(), ModSounds.ORB_IMPACT.get(), SoundSource.NEUTRAL, 0.8F, 1.0F);
            if (level() instanceof ServerLevel server) {
                WispVariant v = getOrbVariant();
                Vector3f color = v == null ? new Vector3f(1.0F, 0.89F, 0.48F) : new Vector3f(v.red(), v.green(), v.blue());
                server.sendParticles(new DustParticleOptions(color, 1.2F), getX(), getY(), getZ(), 12, 0.3, 0.3, 0.3, 0.05);
            }
            discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("OrbVariant", entityData.get(DATA_VARIANT));
        tag.putFloat("DamageBonus", damageBonus);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_VARIANT, tag.getInt("OrbVariant"));
        damageBonus = tag.getFloat("DamageBonus");
    }
}
