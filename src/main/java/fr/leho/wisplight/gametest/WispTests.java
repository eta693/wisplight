package fr.leho.wisplight.gametest;

import fr.leho.wisplight.Wisplight;
import fr.leho.wisplight.block.TraceLightBlock;
import fr.leho.wisplight.blockentity.WispJarBlockEntity;
import fr.leho.wisplight.entity.WispEntity;
import fr.leho.wisplight.entity.WispVariant;
import fr.leho.wisplight.registry.ModBlocks;
import fr.leho.wisplight.registry.ModEntities;
import fr.leho.wisplight.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Wisplight.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WispTests {
    private static final BlockPos JAR = new BlockPos(2, 1, 2);

    private WispTests() {}

    private static boolean has(Player player, net.minecraft.world.item.Item item) {
        return player.getInventory().contains(new ItemStack(item));
    }

    /** Un bocal produit une essence quand sa progression atteint le seuil. */
    @GameTest(template = "empty")
    public static void jarProducesEssence(GameTestHelper helper) {
        helper.setBlock(JAR, ModBlocks.WISP_JARS.get(WispVariant.AZUR).get());
        WispJarBlockEntity jar = helper.getBlockEntity(JAR);
        helper.assertTrue(jar.getEssences() == 0, "Le bocal neuf doit être vide");
        jar.addProgress(WispJarBlockEntity.progressPerEssence() - 1);
        helper.assertTrue(jar.getEssences() == 0, "Pas d'essence avant le seuil");
        jar.addProgress(1);
        helper.assertTrue(jar.getEssences() == 1, "Une essence au seuil exact");
        jar.addProgress(WispJarBlockEntity.progressPerEssence() * 10);
        helper.assertTrue(jar.getEssences() == WispJarBlockEntity.MAX_ESSENCES, "Plafond de 4 essences");
        helper.succeed();
    }

    /** La vitesse est doublée dans le biome d'origine (le test tourne en plaines : biome Azur). */
    @GameTest(template = "empty")
    public static void jarHomeBiomeBonus(GameTestHelper helper) {
        helper.setBlock(JAR, ModBlocks.WISP_JARS.get(WispVariant.AZUR).get());
        WispJarBlockEntity azur = helper.getBlockEntity(JAR);
        int rate = azur.currentRate();
        helper.assertTrue(rate == WispJarBlockEntity.RATE_HOME_BIOME
                        || rate == WispJarBlockEntity.RATE_NORMAL
                        || rate == WispJarBlockEntity.RATE_DAYLIGHT,
                "Vitesse inattendue : " + rate);
        helper.setBlock(JAR, ModBlocks.WISP_JARS.get(WispVariant.BRAISE).get());
        WispJarBlockEntity braise = helper.getBlockEntity(JAR);
        helper.assertTrue(braise.currentRate() == WispJarBlockEntity.RATE_NORMAL,
                "Un follet Braise hors du Nether produit à vitesse normale");
        helper.succeed();
    }

    /** Une fiole vide récolte l'essence ; le comparateur suit le remplissage. */
    @GameTest(template = "empty")
    public static void bottleHarvestsEssence(GameTestHelper helper) {
        helper.setBlock(JAR, ModBlocks.WISP_JARS.get(WispVariant.EMERAUDE).get());
        WispJarBlockEntity jar = helper.getBlockEntity(JAR);
        jar.addProgress(WispJarBlockEntity.progressPerEssence() * 2);
        int signal = helper.getLevel().getBlockState(helper.absolutePos(JAR))
                .getAnalogOutputSignal(helper.getLevel(), helper.absolutePos(JAR));
        helper.assertTrue(signal == 7, "Signal de comparateur attendu 7, obtenu " + signal);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE, 4));
        helper.useBlock(JAR, player);
        helper.assertTrue(jar.getEssences() == 1, "Une essence doit avoir été retirée");
        helper.assertTrue(has(player, ModItems.ESSENCES.get(WispVariant.EMERAUDE).get()),
                "Le joueur doit recevoir une essence de vie");
        helper.assertTrue(player.getMainHandItem().getCount() == 3, "Une fiole consommée");
        helper.succeed();
    }

    /** Un joueur accroupi capture un follet avec un bocal vide. */
    @GameTest(template = "empty")
    public static void sneakingPlayerCapturesWisp(GameTestHelper helper) {
        WispEntity wisp = helper.spawn(ModEntities.WISP.get(), new BlockPos(2, 2, 2));
        wisp.setVariant(WispVariant.GIVRE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.EMPTY_JAR.get()));
        player.setShiftKeyDown(true);
        wisp.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(wisp.isRemoved(), "Le follet doit être dans le bocal");
        helper.assertTrue(has(player, ModItems.WISP_JARS.get(WispVariant.GIVRE).get()),
                "Le joueur doit recevoir un bocal à follet Givre");
        helper.succeed();
    }

    /** Sans être accroupi, la capture échoue. */
    @GameTest(template = "empty")
    public static void standingPlayerCannotCapture(GameTestHelper helper) {
        WispEntity wisp = helper.spawn(ModEntities.WISP.get(), new BlockPos(2, 2, 2));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.EMPTY_JAR.get()));
        player.setShiftKeyDown(false);
        wisp.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(!wisp.isRemoved(), "Le follet agité ne se laisse pas capturer");
        helper.succeed();
    }

    /** Relâcher un bocal (accroupi, main vide) libère le follet et rend le bocal vide. */
    @GameTest(template = "empty")
    public static void releaseFromJar(GameTestHelper helper) {
        helper.setBlock(JAR, ModBlocks.WISP_JARS.get(WispVariant.AZUR).get());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        helper.useBlock(JAR, player);
        helper.assertBlockPresent(Blocks.AIR, JAR);
        helper.assertEntityPresent(ModEntities.WISP.get());
        helper.assertTrue(has(player, ModItems.EMPTY_JAR.get()), "Le joueur récupère un bocal vide");
        helper.succeed();
    }

    /** La trace lumineuse disparaît seule si aucun follet ne la rafraîchit. */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void traceLightFades(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        TraceLightBlock.refresh(helper.getLevel(), helper.absolutePos(pos));
        helper.assertBlockPresent(ModBlocks.TRACE_LIGHT.get(), pos);
        helper.startSequence()
                .thenIdle(30)
                .thenExecute(() -> helper.assertBlockPresent(Blocks.AIR, pos))
                .thenSucceed();
    }

    /** Un joueur qui sprinte ou tient une épée effraie les follets ; accroupi, non. */
    @GameTest(template = "empty")
    public static void weaponsScareWisps(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(!WispEntity.scares(player), "Main vide : pas de peur");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LUMEN_SWORD.get()));
        helper.assertTrue(WispEntity.scares(player), "Une épée fait peur");
        player.setShiftKeyDown(true);
        helper.assertTrue(!WispEntity.scares(player), "Accroupi, le joueur reste calme");
        helper.succeed();
    }
}
