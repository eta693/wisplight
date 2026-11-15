package fr.leho.wisplight.tour;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.leho.wisplight.Wisplight;
import fr.leho.wisplight.block.RunestoneBlock;
import fr.leho.wisplight.blockentity.InfusionAltarBlockEntity;
import fr.leho.wisplight.entity.CreepingShadeEntity;
import fr.leho.wisplight.entity.HollowWardenEntity;
import fr.leho.wisplight.entity.WispEntity;
import fr.leho.wisplight.entity.WispVariant;
import fr.leho.wisplight.registry.ModBlocks;
import fr.leho.wisplight.registry.ModComponents;
import fr.leho.wisplight.registry.ModEntities;
import fr.leho.wisplight.registry.ModItems;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * « Visite » automatique pour la CI : n'agit que si la propriété système wisplight.tour est définie.
 * Construit une scène par étape, y place le joueur et signale au client (message système) de prendre
 * une capture d'écran. Sans effet en jeu normal.
 */
@EventBusSubscriber(modid = Wisplight.MOD_ID)
public final class TourDirector {
    public static final boolean ENABLED = Boolean.getBoolean("wisplight.tour");
    public static final boolean PREGEN = Boolean.getBoolean("wisplight.tour.pregen");
    public static final String PREFIX = "[tour] ";
    static final int WARMUP = 200;
    static final int SCENE_TICKS = 220;

    private static ServerPlayer player;
    private static int tick = -1;

    private record Scene(String name, BiConsumer<ServerLevel, BlockPos> build, double camX, double camY, double camZ,
                         float yaw, float pitch, long time) {}

    private static final List<Scene> SCENES = List.of(
            new Scene("01_follets_nuit", TourDirector::wisps, 0, 3.0, -8, 0, 8, 18000),
            new Scene("02_bocaux_lanternes", TourDirector::jars, 0, 2.6, -4.5, 0, 18, 18000),
            new Scene("03_lumen_equipement", TourDirector::lumen, 0, 2.2, -5.5, 0, 8, 6000),
            new Scene("04_autel_runes", TourDirector::altarScene, 0, 3.2, -6, 0, 22, 13000),
            new Scene("05_interface_autel", TourDirector::altarGui, 0, 2.6, -3, 0, 30, 6000),
            new Scene("06_ombre_rampante", TourDirector::shade, 0, 2.6, -5, 0, 10, 18000),
            new Scene("07_veilleur_phase1", TourDirector::warden, 0, 3.2, -9, 0, 6, 18000),
            new Scene("08_veilleur_phase3", TourDirector::wardenPhase3, 0, 3.2, -9, 0, 6, 18000),
            new Scene("09_sanctuaire", TourDirector::sanctuary, 0, -72, -18, 0, 45, 6000),
            new Scene("10_grimoire", TourDirector::nothing, 0, 2, -3, 0, 0, 6000));

    private TourDirector() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (PREGEN) {
            Wisplight.LOGGER.info("Visite : monde pré-généré, arrêt du serveur.");
            event.getServer().halt(false);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!ENABLED || PREGEN || !(event.getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        player = sp;
        tick = 0;
        MinecraftServer server = sp.server;
        GameRules rules = server.getGameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
        sp.setGameMode(GameType.CREATIVE);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (player == null || tick < 0) {
            return;
        }
        tick++;
        player.removeAllEffects();
        if (!player.getAbilities().flying) {
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        }
        if (tick < WARMUP) {
            return;
        }
        int index = (tick - WARMUP) / SCENE_TICKS;
        int local = (tick - WARMUP) % SCENE_TICKS;
        ServerLevel level = player.serverLevel();
        if (index < SCENES.size()) {
            Scene scene = SCENES.get(index);
            BlockPos origin = new BlockPos(2000 + index * 96, 100, 0);
            if (local == 0) {
                level.setDayTime(scene.time());
                player.teleportTo(level, origin.getX() + 0.5 + scene.camX(), origin.getY() + scene.camY(),
                        origin.getZ() + 0.5 + scene.camZ(), scene.yaw(), scene.pitch());
            } else if (local == 40) {
                try {
                    scene.build().accept(level, origin);
                    Wisplight.LOGGER.info("Visite : scène {} construite en {}", scene.name(), origin);
                } catch (Exception e) {
                    Wisplight.LOGGER.error("Visite : la scène {} a échoué", scene.name(), e);
                }
                player.teleportTo(level, origin.getX() + 0.5 + scene.camX(), origin.getY() + scene.camY(),
                        origin.getZ() + 0.5 + scene.camZ(), scene.yaw(), scene.pitch());
                player.sendSystemMessage(Component.literal(PREFIX + scene.name()));
            }
        } else if (index == SCENES.size() && local == 0) {
            player.sendSystemMessage(Component.literal(PREFIX + "end"));
            tick = -1;
            player = null;
        }
    }

    // ------------------------------------------------------------------ outils de construction

    private static void platform(ServerLevel level, BlockPos center, int radius, BlockState floor, boolean cage) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                level.setBlockAndUpdate(center.offset(x, 0, z), floor);
                for (int y = 1; y <= 6; y++) {
                    boolean edge = Math.abs(x) == radius || Math.abs(z) == radius;
                    level.setBlockAndUpdate(center.offset(x, y, z),
                            cage && edge ? Blocks.BARRIER.defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
                if (cage) {
                    level.setBlockAndUpdate(center.offset(x, 7, z), Blocks.BARRIER.defaultBlockState());
                }
            }
        }
    }

    private static WispEntity wisp(ServerLevel level, BlockPos pos, WispVariant variant) {
        WispEntity wisp = ModEntities.WISP.get().create(level);
        wisp.setVariant(variant);
        wisp.setPersistenceRequired();
        wisp.moveTo(pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 180F, 0F);
        level.addFreshEntity(wisp);
        return wisp;
    }

    private static void stand(ServerLevel level, BlockPos pos, ItemStack head, ItemStack chest, ItemStack legs,
                              ItemStack feet) {
        ArmorStand stand = EntityType.ARMOR_STAND.create(level);
        stand.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 180F, 0F);
        stand.setItemSlot(EquipmentSlot.HEAD, head);
        stand.setItemSlot(EquipmentSlot.CHEST, chest);
        stand.setItemSlot(EquipmentSlot.LEGS, legs);
        stand.setItemSlot(EquipmentSlot.FEET, feet);
        level.addFreshEntity(stand);
    }

    // ------------------------------------------------------------------ scènes

    private static void nothing(ServerLevel level, BlockPos o) {
        platform(level, o, 3, Blocks.GRASS_BLOCK.defaultBlockState(), false);
    }

    private static void wisps(ServerLevel level, BlockPos o) {
        platform(level, o, 6, Blocks.GRASS_BLOCK.defaultBlockState(), true);
        WispVariant[] v = WispVariant.values();
        for (int i = 0; i < v.length; i++) {
            wisp(level, o.offset(-3 + i * 2, 2, 1), v[i]);
        }
        level.setBlockAndUpdate(o.offset(-4, 1, 3), Blocks.OAK_LEAVES.defaultBlockState());
        level.setBlockAndUpdate(o.offset(4, 1, 3), ModBlocks.CALLING_LANTERN.get().defaultBlockState());
    }

    private static void jars(ServerLevel level, BlockPos o) {
        platform(level, o, 5, Blocks.STONE_BRICKS.defaultBlockState(), false);
        WispVariant[] v = WispVariant.values();
        for (int i = 0; i < v.length; i++) {
            level.setBlockAndUpdate(o.offset(-3 + i * 2, 1, 0), Blocks.POLISHED_DEEPSLATE.defaultBlockState());
            level.setBlockAndUpdate(o.offset(-3 + i * 2, 2, 0), ModBlocks.WISP_JARS.get(v[i]).get().defaultBlockState());
            level.setBlockAndUpdate(o.offset(-2 + i * 2, 1, 2), ModBlocks.LUMEN_LANTERNS.get(v[i]).get().defaultBlockState());
        }
        level.setBlockAndUpdate(o.offset(0, 4, 1), Blocks.STONE_BRICKS.defaultBlockState());
        level.setBlockAndUpdate(o.offset(0, 3, 1), ModBlocks.LUMEN_LANTERNS.get(WispVariant.BRAISE).get()
                .defaultBlockState().setValue(LanternBlock.HANGING, true));
    }

    private static void lumen(ServerLevel level, BlockPos o) {
        platform(level, o, 5, Blocks.GRASS_BLOCK.defaultBlockState(), false);
        stand(level, o.offset(-2, 1, 0), new ItemStack(ModItems.LUMEN_HELMET.get()), new ItemStack(ModItems.LUMEN_CHESTPLATE.get()),
                new ItemStack(ModItems.LUMEN_LEGGINGS.get()), new ItemStack(ModItems.LUMEN_BOOTS.get()));
        stand(level, o.offset(2, 1, 0),
                ModComponents.infuse(new ItemStack(ModItems.WATCHER_HELMET.get()), WispVariant.AZUR),
                ModComponents.infuse(new ItemStack(ModItems.WATCHER_CHESTPLATE.get()), WispVariant.AZUR),
                new ItemStack(ModItems.WATCHER_LEGGINGS.get()), new ItemStack(ModItems.WATCHER_BOOTS.get()));
        level.setBlockAndUpdate(o.offset(-1, 1, 2), ModBlocks.LUMEN_ORE.get().defaultBlockState());
        level.setBlockAndUpdate(o.offset(0, 1, 2), ModBlocks.DEEPSLATE_LUMEN_ORE.get().defaultBlockState());
        level.setBlockAndUpdate(o.offset(1, 1, 2), ModBlocks.LUMEN_BLOCK.get().defaultBlockState());
    }

    private static void altarScene(ServerLevel level, BlockPos o) {
        platform(level, o, 6, ModBlocks.SANCTUARY_BRICKS.get().defaultBlockState(), false);
        level.setBlockAndUpdate(o.offset(-2, 1, 0), ModBlocks.INFUSION_ALTAR.get().defaultBlockState());
        level.setBlockAndUpdate(o.offset(-4, 1, 0), ModBlocks.WISP_JARS.get(WispVariant.EMERAUDE).get().defaultBlockState());
        level.setBlockAndUpdate(o.offset(2, 1, 0), ModBlocks.SANCTUARY_ALTAR.get().defaultBlockState());
        WispVariant[] v = WispVariant.values();
        int[][] spots = {{2, -2}, {4, 0}, {2, 2}, {0, 0}};
        for (int i = 0; i < 4; i++) {
            BlockState rune = ModBlocks.RUNESTONES.get(v[i]).get().defaultBlockState()
                    .setValue(RunestoneBlock.CHARGED, i > 0).setValue(RunestoneBlock.LIT, i > 1);
            level.setBlockAndUpdate(o.offset(spots[i][0], 1, spots[i][1]), rune);
        }
        level.setBlockAndUpdate(o.offset(-5, 1, 3), ModBlocks.SANCTUARY_BRICK_STAIRS.get().defaultBlockState());
        level.setBlockAndUpdate(o.offset(-4, 1, 3), ModBlocks.SANCTUARY_BRICK_SLAB.get().defaultBlockState());
        level.setBlockAndUpdate(o.offset(-3, 1, 3), ModBlocks.SANCTUARY_BRICK_WALL.get().defaultBlockState());
        level.setBlockAndUpdate(o.offset(-2, 1, 3), ModBlocks.CRACKED_SANCTUARY_BRICKS.get().defaultBlockState());
    }

    private static void altarGui(ServerLevel level, BlockPos o) {
        platform(level, o, 3, Blocks.STONE.defaultBlockState(), false);
        BlockPos altarPos = o.offset(0, 1, 0);
        level.setBlockAndUpdate(altarPos, ModBlocks.INFUSION_ALTAR.get().defaultBlockState());
        level.setBlockAndUpdate(o.offset(2, 1, 0), ModBlocks.WISP_JARS.get(WispVariant.BRAISE).get().defaultBlockState());
        if (level.getBlockEntity(altarPos) instanceof InfusionAltarBlockEntity altar) {
            altar.getInventory().setStackInSlot(InfusionAltarBlockEntity.SLOT_ITEM, new ItemStack(ModItems.LUMEN_SWORD.get()));
            altar.getInventory().setStackInSlot(InfusionAltarBlockEntity.SLOT_ESSENCE_A, new ItemStack(ModItems.ESSENCES.get(WispVariant.BRAISE).get(), 3));
            altar.getInventory().setStackInSlot(InfusionAltarBlockEntity.SLOT_ESSENCE_B, new ItemStack(ModItems.ESSENCES.get(WispVariant.BRAISE).get(), 2));
            player.openMenu(altar, altarPos);
        }
    }

    private static void shade(ServerLevel level, BlockPos o) {
        platform(level, o, 5, Blocks.DEEPSLATE.defaultBlockState(), true);
        CreepingShadeEntity shade = ModEntities.CREEPING_SHADE.get().create(level);
        shade.moveTo(o.getX() + 0.5, o.getY() + 1, o.getZ() + 1.5, 180F, 0F);
        shade.setPersistenceRequired();
        level.addFreshEntity(shade);
        wisp(level, o.offset(2, 2, 2), WispVariant.AZUR);
    }

    private static void warden(ServerLevel level, BlockPos o) {
        platform(level, o, 6, ModBlocks.SANCTUARY_BRICKS.get().defaultBlockState(), true);
        HollowWardenEntity warden = ModEntities.HOLLOW_WARDEN.get().create(level);
        warden.moveTo(o.getX() + 0.5, o.getY() + 1, o.getZ() + 0.5, 180F, 0F);
        warden.setPersistenceRequired();
        level.addFreshEntity(warden);
    }

    private static void wardenPhase3(ServerLevel level, BlockPos o) {
        warden(level, o);
        level.getEntitiesOfClass(HollowWardenEntity.class, new net.minecraft.world.phys.AABB(o).inflate(8))
                .forEach(w -> w.setHealth(HollowWardenEntity.MAX_HEALTH * 0.2F));
    }

    private static void sanctuary(ServerLevel level, BlockPos o) {
        BlockPos ground = new BlockPos(o.getX(), 0, o.getZ() + 4);
        net.minecraft.world.level.ChunkPos c = new net.minecraft.world.level.ChunkPos(ground);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                level.getChunk(c.x + dx, c.z + dz);
            }
        }
        try {
            level.getServer().getCommands().getDispatcher().execute(
                    "place structure wisplight:sanctuary " + ground.getX() + " 0 " + ground.getZ(),
                    level.getServer().createCommandSourceStack().withLevel(level).withPermission(4).withSuppressedOutput());
        } catch (CommandSyntaxException e) {
            Wisplight.LOGGER.error("Visite : placement du sanctuaire impossible", e);
        }
    }
}
