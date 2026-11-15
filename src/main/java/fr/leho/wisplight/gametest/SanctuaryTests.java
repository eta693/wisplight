package fr.leho.wisplight.gametest;

import fr.leho.wisplight.Wisplight;
import fr.leho.wisplight.WisplightWorldData;
import fr.leho.wisplight.block.RunestoneBlock;
import fr.leho.wisplight.blockentity.SanctuaryAltarBlockEntity;
import fr.leho.wisplight.entity.HollowWardenEntity;
import fr.leho.wisplight.entity.WispVariant;
import fr.leho.wisplight.item.SanctuaryCompassItem;
import fr.leho.wisplight.registry.ModBlocks;
import fr.leho.wisplight.registry.ModEntities;
import fr.leho.wisplight.registry.ModItems;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Wisplight.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SanctuaryTests {
    private static final BlockPos ALTAR = new BlockPos(4, 2, 4);
    private static final BlockPos[] RUNES = {
            new BlockPos(4, 2, 1), new BlockPos(7, 2, 4), new BlockPos(4, 2, 7), new BlockPos(1, 2, 4)};

    private SanctuaryTests() {}

    private static SanctuaryAltarBlockEntity buildRitual(GameTestHelper helper, int chargedRunes) {
        helper.setBlock(ALTAR, ModBlocks.SANCTUARY_ALTAR.get());
        WispVariant[] variants = WispVariant.values();
        for (int i = 0; i < RUNES.length; i++) {
            helper.setBlock(RUNES[i], ModBlocks.RUNESTONES.get(variants[i]).get().defaultBlockState()
                    .setValue(RunestoneBlock.CHARGED, i < chargedRunes));
        }
        return helper.getBlockEntity(ALTAR);
    }

    /** Quatre runes chargées + un bocal → le Veilleur Éteint apparaît, les runes se vident. */
    @GameTest(template = "platform", timeoutTicks = 200)
    public static void ritualSummonsWarden(GameTestHelper helper) {
        SanctuaryAltarBlockEntity altar = buildRitual(helper, 4);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(altar.tryStartRitual(helper.getLevel(), player, true), "Le rituel doit commencer");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertEntityPresent(ModEntities.HOLLOW_WARDEN.get()))
                .thenExecute(() -> {
                    for (BlockPos rune : RUNES) {
                        helper.assertBlockProperty(rune, RunestoneBlock.CHARGED, false);
                    }
                    helper.getLevel().getEntitiesOfClass(HollowWardenEntity.class,
                            new net.minecraft.world.phys.AABB(helper.absolutePos(ALTAR)).inflate(16)).forEach(e -> e.discard());
                })
                .thenSucceed();
    }

    /** Avec seulement 3 runes chargées, rien ne se passe. */
    @GameTest(template = "platform")
    public static void ritualNeedsFourRunes(GameTestHelper helper) {
        SanctuaryAltarBlockEntity altar = buildRitual(helper, 3);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(!altar.tryStartRitual(helper.getLevel(), player, true), "Le rituel doit être refusé");
        helper.assertTrue(!altar.isRitualRunning(), "Aucun rituel en cours");
        helper.succeed();
    }

    /** Une essence du bon type charge la pierre runique. */
    @GameTest(template = "empty")
    public static void essenceChargesRunestone(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, ModBlocks.RUNESTONES.get(WispVariant.BRAISE).get());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.item.ItemStack(ModItems.ESSENCES.get(WispVariant.AZUR).get()));
        helper.useBlock(pos, player);
        helper.assertBlockProperty(pos, RunestoneBlock.CHARGED, false);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.item.ItemStack(ModItems.ESSENCES.get(WispVariant.BRAISE).get()));
        helper.useBlock(pos, player);
        helper.assertBlockProperty(pos, RunestoneBlock.CHARGED, true);
        helper.succeed();
    }

    /** Les phases suivent la vie restante. */
    @GameTest(template = "empty")
    public static void wardenPhases(GameTestHelper helper) {
        float max = HollowWardenEntity.MAX_HEALTH;
        helper.assertTrue(HollowWardenEntity.phaseFor(max, max) == 1, "Pleine vie : phase 1");
        helper.assertTrue(HollowWardenEntity.phaseFor(max * 0.5F, max) == 2, "Moitié : phase 2");
        helper.assertTrue(HollowWardenEntity.phaseFor(max * 0.2F, max) == 3, "Faible : phase 3");
        helper.succeed();
    }

    /** En phase 2, le Veilleur invoque des Ombres et éteint les torches. */
    @GameTest(template = "platform")
    public static void wardenSummonsAndDarkens(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 1), Blocks.TORCH);
        HollowWardenEntity warden = helper.spawn(ModEntities.HOLLOW_WARDEN.get(), new BlockPos(4, 2, 4));
        int shades = warden.summonShades(helper.getLevel(), 3);
        helper.assertTrue(shades == 3, "3 Ombres attendues, obtenu " + shades);
        int torches = warden.extinguishTorches(helper.getLevel(), 12);
        helper.assertTrue(torches >= 1, "La torche doit s'éteindre");
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(1, 2, 1));
        warden.nearbyShades().forEach(e -> e.discard());
        warden.discard();
        helper.succeed();
    }

    /** À sa mort, le Veilleur lâche le Cœur de lanterne et ravive la flamme du monde. */
    @GameTest(template = "platform")
    public static void wardenDropsHeart(GameTestHelper helper) {
        HollowWardenEntity warden = helper.spawn(ModEntities.HOLLOW_WARDEN.get(), new BlockPos(4, 2, 4));
        warden.kill();
        helper.assertItemEntityPresent(ModItems.LANTERN_HEART.get(), new BlockPos(4, 2, 4), 4.0);
        helper.assertTrue(WisplightWorldData.get(helper.getLevel().getServer()).isRekindled(), "Flamme ravivée");
        helper.succeed();
    }

    /** Le Sanctuaire oublié se génère réellement dans le monde (commande /place structure). */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void sanctuaryGenerates(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(3000, 0, 3000);
        net.minecraft.world.level.ChunkPos center = new net.minecraft.world.level.ChunkPos(base);
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                level.setChunkForced(center.x + dx, center.z + dz, true);
            }
        }
        helper.startSequence()
                .thenWaitUntil(() -> {
                    for (int dx = -4; dx <= 4; dx++) {
                        for (int dz = -4; dz <= 4; dz++) {
                            helper.assertTrue(level.hasChunk(center.x + dx, center.z + dz), "Chunk pas encore chargé");
                        }
                    }
                })
                .thenExecute(() -> {
                    CommandSourceStack source = level.getServer().createCommandSourceStack()
                            .withLevel(level).withPermission(4).withSuppressedOutput();
                    try {
                        level.getServer().getCommands().getDispatcher().execute(
                                "place structure wisplight:sanctuary " + base.getX() + " " + base.getY() + " " + base.getZ(), source);
                    } catch (CommandSyntaxException e) {
                        helper.fail("Commande refusée : " + e.getMessage());
                    }
                    BlockPos altar = BlockPos.betweenClosedStream(base.offset(-48, -24, -48), base.offset(48, 48, 48))
                            .filter(p -> level.getBlockState(p).is(ModBlocks.SANCTUARY_ALTAR.get()))
                            .map(BlockPos::immutable).findFirst().orElse(null);
                    helper.assertTrue(altar != null, "L'autel du sanctuaire doit avoir été généré");
                    int runes = 0;
                    for (BlockPos p : BlockPos.betweenClosed(altar.offset(-4, -1, -4), altar.offset(4, 1, 4))) {
                        if (level.getBlockState(p).getBlock() instanceof RunestoneBlock) {
                            runes++;
                        }
                    }
                    helper.assertTrue(runes == 4, "4 pierres runiques attendues autour de l'autel, trouvées : " + runes);
                    // La crypte est 6 blocs sous l'autel. Dans le monde plat de test, elle passe sous la limite du
                    // monde (Y -64) : on vérifie alors le générateur dans le modèle lui-même.
                    boolean spawner;
                    if (altar.getY() - 7 >= level.getMinBuildHeight()) {
                        spawner = BlockPos.betweenClosedStream(altar.offset(-4, -8, -4), altar.offset(4, -1, 4))
                                .anyMatch(p -> level.getBlockState(p).is(Blocks.SPAWNER));
                    } else {
                        spawner = level.getStructureManager().get(Wisplight.id("sanctuary/main"))
                                .map(t -> !t.filterBlocks(BlockPos.ZERO,
                                        new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                                        Blocks.SPAWNER).isEmpty())
                                .orElse(false);
                    }
                    helper.assertTrue(spawner, "La crypte doit contenir un générateur d'Ombres");
                    for (int dx = -4; dx <= 4; dx++) {
                        for (int dz = -4; dz <= 4; dz++) {
                            level.setChunkForced(center.x + dx, center.z + dz, false);
                        }
                    }
                })
                .thenSucceed();
    }

    /** La boussole donne la bonne direction, et le modèle du sanctuaire existe. */
    @GameTest(template = "empty")
    public static void compassAndSanctuaryTemplate(GameTestHelper helper) {
        String north = ((TranslatableContents) SanctuaryCompassItem.direction(0, -10).getContents()).getKey();
        String southEast = ((TranslatableContents) SanctuaryCompassItem.direction(10, 10).getContents()).getKey();
        helper.assertTrue(north.endsWith(".n"), "Nord attendu, obtenu " + north);
        helper.assertTrue(southEast.endsWith(".se"), "Sud-est attendu, obtenu " + southEast);
        helper.assertTrue(helper.getLevel().getStructureManager().get(Wisplight.id("sanctuary/main")).isPresent(),
                "Le modèle du sanctuaire doit exister");
        helper.succeed();
    }
}
