package fr.leho.wisplight.registry;

import fr.leho.wisplight.Wisplight;
import fr.leho.wisplight.block.TraceLightBlock;
import fr.leho.wisplight.block.WispJarBlock;
import fr.leho.wisplight.entity.WispVariant;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Wisplight.MOD_ID);

    public static final DeferredBlock<Block> LUMEN_ORE = BLOCKS.register("lumen_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 5),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE).lightLevel(state -> 3)));

    public static final DeferredBlock<Block> DEEPSLATE_LUMEN_ORE = BLOCKS.register("deepslate_lumen_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 5),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_IRON_ORE).lightLevel(state -> 3)));

    public static final DeferredBlock<Block> LUMEN_BLOCK = BLOCKS.register("lumen_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .mapColor(MapColor.GOLD)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 12)));

    /** Lumière invisible et temporaire laissée par les follets (lumière dynamique). */
    public static final DeferredBlock<TraceLightBlock> TRACE_LIGHT = BLOCKS.register("trace_light",
            () -> new TraceLightBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE)
                    .replaceable()
                    .noCollission()
                    .noLootTable()
                    .noOcclusion()
                    .instabreak()
                    .pushReaction(PushReaction.DESTROY)
                    .lightLevel(state -> 10)));

    public static final Map<WispVariant, DeferredBlock<WispJarBlock>> WISP_JARS =
            Util.make(new EnumMap<>(WispVariant.class), map -> {
                for (WispVariant variant : WispVariant.values()) {
                    map.put(variant, BLOCKS.register("wisp_jar_" + variant.id(),
                            () -> new WispJarBlock(variant, BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.NONE)
                                    .strength(0.3F)
                                    .sound(SoundType.GLASS)
                                    .noOcclusion()
                                    .lightLevel(state -> 15))));
                }
            });

    private ModBlocks() {}
}
