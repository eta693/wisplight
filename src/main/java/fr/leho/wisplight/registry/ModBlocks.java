package fr.leho.wisplight.registry;

import fr.leho.wisplight.Wisplight;
import fr.leho.wisplight.block.CallingLanternBlock;
import fr.leho.wisplight.block.InfusionAltarBlock;
import fr.leho.wisplight.block.LingeringGlowBlock;
import fr.leho.wisplight.block.RunestoneBlock;
import fr.leho.wisplight.block.SanctuaryAltarBlock;
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
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
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

    public static final DeferredBlock<InfusionAltarBlock> INFUSION_ALTAR = BLOCKS.register("infusion_altar",
            () -> new InfusionAltarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_DEEPSLATE)
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(3.0F, 9.0F)
                    .noOcclusion()
                    .lightLevel(state -> 6)));

    public static final DeferredBlock<LingeringGlowBlock> LINGERING_GLOW = BLOCKS.register("lingering_glow",
            () -> new LingeringGlowBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE)
                    .replaceable()
                    .noCollission()
                    .noLootTable()
                    .noOcclusion()
                    .instabreak()
                    .sound(SoundType.AMETHYST_CLUSTER)
                    .pushReaction(PushReaction.DESTROY)
                    .lightLevel(state -> 12)));

    public static final DeferredBlock<CallingLanternBlock> CALLING_LANTERN = BLOCKS.register("calling_lantern",
            () -> new CallingLanternBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN).noOcclusion()));

    public static final Map<WispVariant, DeferredBlock<LanternBlock>> LUMEN_LANTERNS =
            Util.make(new EnumMap<>(WispVariant.class), map -> {
                for (WispVariant variant : WispVariant.values()) {
                    map.put(variant, BLOCKS.register("lumen_lantern_" + variant.id(),
                            () -> new LanternBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN).noOcclusion())));
                }
            });

    // --- Sanctuaire oublié ---
    public static final DeferredBlock<Block> SANCTUARY_BRICKS = BLOCKS.register("sanctuary_bricks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).mapColor(MapColor.COLOR_GRAY)));
    public static final DeferredBlock<Block> CRACKED_SANCTUARY_BRICKS = BLOCKS.register("cracked_sanctuary_bricks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CRACKED_STONE_BRICKS).mapColor(MapColor.COLOR_GRAY)));
    public static final DeferredBlock<StairBlock> SANCTUARY_BRICK_STAIRS = BLOCKS.register("sanctuary_brick_stairs",
            () -> new StairBlock(SANCTUARY_BRICKS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICK_STAIRS)));
    public static final DeferredBlock<SlabBlock> SANCTUARY_BRICK_SLAB = BLOCKS.register("sanctuary_brick_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICK_SLAB)));
    public static final DeferredBlock<WallBlock> SANCTUARY_BRICK_WALL = BLOCKS.register("sanctuary_brick_wall",
            () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICK_WALL)));

    public static final Map<WispVariant, DeferredBlock<RunestoneBlock>> RUNESTONES =
            Util.make(new EnumMap<>(WispVariant.class), map -> {
                for (WispVariant variant : WispVariant.values()) {
                    map.put(variant, BLOCKS.register("runestone_" + variant.id(),
                            () -> new RunestoneBlock(variant, BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_DEEPSLATE)
                                    .lightLevel(RunestoneBlock::lightLevel))));
                }
            });

    public static final DeferredBlock<SanctuaryAltarBlock> SANCTUARY_ALTAR = BLOCKS.register("sanctuary_altar",
            () -> new SanctuaryAltarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK)
                    .mapColor(MapColor.COLOR_GRAY)
                    .sound(SoundType.DEEPSLATE_BRICKS)
                    .lightLevel(state -> 8)));

    private ModBlocks() {}
}
