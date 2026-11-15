package fr.leho.wisplight.registry;

import fr.leho.wisplight.Wisplight;
import fr.leho.wisplight.entity.WispVariant;
import fr.leho.wisplight.item.CharmItem;
import fr.leho.wisplight.item.EternalLanternItem;
import fr.leho.wisplight.item.GrimoireItem;
import fr.leho.wisplight.item.SanctuaryCompassItem;
import fr.leho.wisplight.item.WatcherStaffItem;
import fr.leho.wisplight.item.WispStaffItem;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Wisplight.MOD_ID);

    // --- Guide ---
    public static final DeferredItem<GrimoireItem> GRIMOIRE = ITEMS.register("grimoire",
            () -> new GrimoireItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    // --- Ressources ---
    public static final DeferredItem<Item> LUMEN_SHARD = simple("lumen_shard");
    public static final DeferredItem<Item> LUMEN_INGOT = simple("lumen_ingot");
    public static final DeferredItem<Item> WISP_DUST = simple("wisp_dust");

    public static final Map<WispVariant, DeferredItem<Item>> ESSENCES =
            Util.make(new EnumMap<>(WispVariant.class), map -> {
                for (WispVariant variant : WispVariant.values()) {
                    map.put(variant, ITEMS.register("essence_" + variant.id(),
                            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON))));
                }
            });

    // --- Blocs ---
    public static final DeferredItem<BlockItem> LUMEN_ORE = block("lumen_ore", ModBlocks.LUMEN_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_LUMEN_ORE = block("deepslate_lumen_ore", ModBlocks.DEEPSLATE_LUMEN_ORE);
    public static final DeferredItem<BlockItem> LUMEN_BLOCK = block("lumen_block", ModBlocks.LUMEN_BLOCK);

    // --- Bocaux ---
    public static final DeferredItem<Item> EMPTY_JAR = ITEMS.register("empty_jar",
            () -> new Item(new Item.Properties().stacksTo(16)));

    public static final Map<WispVariant, DeferredItem<BlockItem>> WISP_JARS =
            Util.make(new EnumMap<>(WispVariant.class), map -> {
                for (WispVariant variant : WispVariant.values()) {
                    map.put(variant, ITEMS.register("wisp_jar_" + variant.id(),
                            () -> new BlockItem(ModBlocks.WISP_JARS.get(variant).get(),
                                    new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON))));
                }
            });

    // --- Outils en lumen ---
    public static final DeferredItem<SwordItem> LUMEN_SWORD = ITEMS.register("lumen_sword",
            () -> new SwordItem(ModTiers.LUMEN, new Item.Properties()
                    .attributes(SwordItem.createAttributes(ModTiers.LUMEN, 3, -2.4F))));
    public static final DeferredItem<PickaxeItem> LUMEN_PICKAXE = ITEMS.register("lumen_pickaxe",
            () -> new PickaxeItem(ModTiers.LUMEN, new Item.Properties()
                    .attributes(DiggerItem.createAttributes(ModTiers.LUMEN, 1.0F, -2.8F))));
    public static final DeferredItem<AxeItem> LUMEN_AXE = ITEMS.register("lumen_axe",
            () -> new AxeItem(ModTiers.LUMEN, new Item.Properties()
                    .attributes(DiggerItem.createAttributes(ModTiers.LUMEN, 5.5F, -3.05F))));
    public static final DeferredItem<ShovelItem> LUMEN_SHOVEL = ITEMS.register("lumen_shovel",
            () -> new ShovelItem(ModTiers.LUMEN, new Item.Properties()
                    .attributes(DiggerItem.createAttributes(ModTiers.LUMEN, 1.5F, -3.0F))));
    public static final DeferredItem<HoeItem> LUMEN_HOE = ITEMS.register("lumen_hoe",
            () -> new HoeItem(ModTiers.LUMEN, new Item.Properties()
                    .attributes(DiggerItem.createAttributes(ModTiers.LUMEN, -2.5F, -0.5F))));

    // --- Armure en lumen ---
    public static final DeferredItem<ArmorItem> LUMEN_HELMET = armor("lumen_helmet", ArmorItem.Type.HELMET);
    public static final DeferredItem<ArmorItem> LUMEN_CHESTPLATE = armor("lumen_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<ArmorItem> LUMEN_LEGGINGS = armor("lumen_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<ArmorItem> LUMEN_BOOTS = armor("lumen_boots", ArmorItem.Type.BOOTS);

    // --- Infusion ---
    public static final DeferredItem<BlockItem> INFUSION_ALTAR = block("infusion_altar", ModBlocks.INFUSION_ALTAR);
    public static final DeferredItem<BlockItem> CALLING_LANTERN = block("calling_lantern", ModBlocks.CALLING_LANTERN);
    public static final Map<WispVariant, DeferredItem<BlockItem>> LUMEN_LANTERNS =
            Util.make(new EnumMap<>(WispVariant.class), map -> {
                for (WispVariant variant : WispVariant.values()) {
                    map.put(variant, block("lumen_lantern_" + variant.id(), ModBlocks.LUMEN_LANTERNS.get(variant)));
                }
            });

    public static final Map<WispVariant, DeferredItem<CharmItem>> CHARMS =
            Util.make(new EnumMap<>(WispVariant.class), map -> {
                for (WispVariant variant : WispVariant.values()) {
                    map.put(variant, ITEMS.register("charm_" + variant.id(),
                            () -> new CharmItem(variant, new Item.Properties().stacksTo(1).rarity(Rarity.RARE))));
                }
            });

    public static final DeferredItem<WispStaffItem> WISP_STAFF = ITEMS.register("wisp_staff",
            () -> new WispStaffItem(new Item.Properties().durability(250).rarity(Rarity.UNCOMMON)));

    // --- Ombres ---
    public static final DeferredItem<Item> SHADOW_FRAGMENT = ITEMS.register("shadow_fragment",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> REINFORCED_JAR = ITEMS.register("reinforced_jar",
            () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<ArmorItem> WATCHER_HELMET = watcher("watcher_helmet", ArmorItem.Type.HELMET);
    public static final DeferredItem<ArmorItem> WATCHER_CHESTPLATE = watcher("watcher_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<ArmorItem> WATCHER_LEGGINGS = watcher("watcher_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<ArmorItem> WATCHER_BOOTS = watcher("watcher_boots", ArmorItem.Type.BOOTS);
    public static final DeferredItem<DeferredSpawnEggItem> CREEPING_SHADE_SPAWN_EGG = ITEMS.register("creeping_shade_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.CREEPING_SHADE, 0x1A1424, 0xF4F0FF, new Item.Properties()));

    // --- Sanctuaire & Veilleur ---
    public static final DeferredItem<BlockItem> SANCTUARY_BRICKS = block("sanctuary_bricks", ModBlocks.SANCTUARY_BRICKS);
    public static final DeferredItem<BlockItem> CRACKED_SANCTUARY_BRICKS = block("cracked_sanctuary_bricks", ModBlocks.CRACKED_SANCTUARY_BRICKS);
    public static final DeferredItem<BlockItem> SANCTUARY_BRICK_STAIRS = block("sanctuary_brick_stairs", ModBlocks.SANCTUARY_BRICK_STAIRS);
    public static final DeferredItem<BlockItem> SANCTUARY_BRICK_SLAB = block("sanctuary_brick_slab", ModBlocks.SANCTUARY_BRICK_SLAB);
    public static final DeferredItem<BlockItem> SANCTUARY_BRICK_WALL = block("sanctuary_brick_wall", ModBlocks.SANCTUARY_BRICK_WALL);
    public static final Map<WispVariant, DeferredItem<BlockItem>> RUNESTONES =
            Util.make(new EnumMap<>(WispVariant.class), map -> {
                for (WispVariant variant : WispVariant.values()) {
                    map.put(variant, block("runestone_" + variant.id(), ModBlocks.RUNESTONES.get(variant)));
                }
            });
    public static final DeferredItem<BlockItem> SANCTUARY_ALTAR = block("sanctuary_altar", ModBlocks.SANCTUARY_ALTAR);
    public static final DeferredItem<SanctuaryCompassItem> SANCTUARY_COMPASS = ITEMS.register("sanctuary_compass",
            () -> new SanctuaryCompassItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> LANTERN_HEART = ITEMS.register("lantern_heart",
            () -> new Item(new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<EternalLanternItem> ETERNAL_LANTERN = ITEMS.register("eternal_lantern",
            () -> new EternalLanternItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<WatcherStaffItem> WATCHER_STAFF = ITEMS.register("watcher_staff",
            () -> new WatcherStaffItem(new Item.Properties().durability(600).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<DeferredSpawnEggItem> HOLLOW_WARDEN_SPAWN_EGG = ITEMS.register("hollow_warden_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.HOLLOW_WARDEN, 0x2B2D33, 0xFFE08A, new Item.Properties()));

    // --- Divers ---
    public static final DeferredItem<DeferredSpawnEggItem> WISP_SPAWN_EGG = ITEMS.register("wisp_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.WISP, 0x1D4E89, 0xBDF3FF, new Item.Properties()));

    private static DeferredItem<Item> simple(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    private static DeferredItem<BlockItem> block(String name, Supplier<? extends Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static DeferredItem<ArmorItem> armor(String name, ArmorItem.Type type) {
        return ITEMS.register(name, () -> new ArmorItem(ModArmorMaterials.LUMEN, type,
                new Item.Properties().durability(type.getDurability(22))));
    }

    private static DeferredItem<ArmorItem> watcher(String name, ArmorItem.Type type) {
        return ITEMS.register(name, () -> new ArmorItem(ModArmorMaterials.WATCHER, type,
                new Item.Properties().durability(type.getDurability(30)).rarity(Rarity.RARE)));
    }

    private ModItems() {}
}
