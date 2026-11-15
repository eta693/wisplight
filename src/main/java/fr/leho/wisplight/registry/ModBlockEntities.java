package fr.leho.wisplight.registry;

import fr.leho.wisplight.Wisplight;
import fr.leho.wisplight.blockentity.WispJarBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Wisplight.MOD_ID);

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WispJarBlockEntity>> WISP_JAR =
            BLOCK_ENTITIES.register("wisp_jar", () -> BlockEntityType.Builder.of(WispJarBlockEntity::new,
                    ModBlocks.WISP_JARS.values().stream().map(h -> (Block) h.get()).toArray(Block[]::new)).build(null));

    private ModBlockEntities() {}
}
