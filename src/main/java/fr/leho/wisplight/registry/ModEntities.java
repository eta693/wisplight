package fr.leho.wisplight.registry;

import fr.leho.wisplight.Wisplight;
import fr.leho.wisplight.entity.CreepingShadeEntity;
import fr.leho.wisplight.entity.HollowWardenEntity;
import fr.leho.wisplight.entity.WardenOrbEntity;
import fr.leho.wisplight.entity.WispEntity;
import fr.leho.wisplight.entity.WispOrbEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Wisplight.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<WispEntity>> WISP = ENTITIES.register("wisp",
            () -> EntityType.Builder.<WispEntity>of(WispEntity::new, MobCategory.AMBIENT)
                    .sized(0.5F, 0.5F)
                    .eyeHeight(0.25F)
                    .clientTrackingRange(8)
                    .build("wisp"));

    public static final DeferredHolder<EntityType<?>, EntityType<WispOrbEntity>> WISP_ORB = ENTITIES.register("wisp_orb",
            () -> EntityType.Builder.<WispOrbEntity>of(WispOrbEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F)
                    .clientTrackingRange(6)
                    .updateInterval(10)
                    .build("wisp_orb"));

    public static final DeferredHolder<EntityType<?>, EntityType<CreepingShadeEntity>> CREEPING_SHADE =
            ENTITIES.register("creeping_shade", () -> EntityType.Builder.<CreepingShadeEntity>of(CreepingShadeEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 2.1F)
                    .eyeHeight(1.85F)
                    .clientTrackingRange(8)
                    .build("creeping_shade"));

    public static final DeferredHolder<EntityType<?>, EntityType<HollowWardenEntity>> HOLLOW_WARDEN =
            ENTITIES.register("hollow_warden", () -> EntityType.Builder.<HollowWardenEntity>of(HollowWardenEntity::new, MobCategory.MONSTER)
                    .sized(1.4F, 3.0F)
                    .eyeHeight(2.4F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build("hollow_warden"));

    public static final DeferredHolder<EntityType<?>, EntityType<WardenOrbEntity>> WARDEN_ORB =
            ENTITIES.register("warden_orb", () -> EntityType.Builder.<WardenOrbEntity>of(WardenOrbEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(6)
                    .updateInterval(2)
                    .build("warden_orb"));

    private ModEntities() {}
}
