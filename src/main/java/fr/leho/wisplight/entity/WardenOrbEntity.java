package fr.leho.wisplight.entity;

import fr.leho.wisplight.registry.ModEntities;
import fr.leho.wisplight.registry.ModSounds;
import javax.annotation.Nullable;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Orbe téléguidé du Veilleur Éteint. Peut être détruit d'un coup. */
public class WardenOrbEntity extends ThrowableProjectile {
    public static final float DAMAGE = 6.0F;
    private static final int HOMING_TICKS = 60;
    private static final int LIFETIME = 120;

    @Nullable
    private LivingEntity target;

    public WardenOrbEntity(EntityType<? extends WardenOrbEntity> type, Level level) {
        super(type, level);
    }

    public WardenOrbEntity(Level level, LivingEntity owner, @Nullable LivingEntity target) {
        super(ModEntities.WARDEN_ORB.get(), owner, level);
        this.target = target;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            level().addParticle(new DustParticleOptions(new Vector3f(0.75F, 0.6F, 1.0F), 1.0F), getX(), getY() + 0.15, getZ(), 0, 0, 0);
            return;
        }
        if (target != null && target.isAlive() && tickCount < HOMING_TICKS) {
            Vec3 wanted = target.getEyePosition().subtract(position()).normalize().scale(0.45);
            setDeltaMovement(getDeltaMovement().scale(0.9).add(wanted.scale(0.1)));
        }
        if (tickCount > LIFETIME) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && !(entity instanceof HollowWardenEntity) && !(entity instanceof CreepingShadeEntity);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity hit = result.getEntity();
        hit.hurt(damageSources().indirectMagic(this, getOwner()), DAMAGE);
        if (hit instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0), this);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            burst();
        }
    }

    private void burst() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY(), getZ(), 10, 0.2, 0.2, 0.2, 0.03);
        }
        level().playSound(null, getX(), getY(), getZ(), ModSounds.ORB_IMPACT.get(), SoundSource.HOSTILE, 1.0F, 0.6F);
        discard();
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && source.getEntity() != getOwner()) {
            burst();
        }
        return true;
    }
}
