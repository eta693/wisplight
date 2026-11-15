package fr.leho.wisplight.registry;

import fr.leho.wisplight.Wisplight;
import fr.leho.wisplight.entity.WispEntity;
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

    private ModEntities() {}
}
