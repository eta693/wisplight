package fr.leho.wisplight.event;

import fr.leho.wisplight.Wisplight;
import fr.leho.wisplight.entity.CreepingShadeEntity;
import fr.leho.wisplight.entity.HollowWardenEntity;
import fr.leho.wisplight.entity.WispEntity;
import fr.leho.wisplight.registry.ModEntities;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

/** Événements du bus du mod (attributs, règles d'apparition). */
@EventBusSubscriber(modid = Wisplight.MOD_ID)
public final class ModEvents {
    private ModEvents() {}

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.WISP.get(), WispEntity.createAttributes().build());
        event.put(ModEntities.CREEPING_SHADE.get(), CreepingShadeEntity.createAttributes().build());
        event.put(ModEntities.HOLLOW_WARDEN.get(), HollowWardenEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(ModEntities.WISP.get(), SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, WispEntity::checkWispSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.CREEPING_SHADE.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, CreepingShadeEntity::checkShadeSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
