package fr.leho.wisplight.gametest;

import fr.leho.wisplight.Wisplight;
import fr.leho.wisplight.entity.CreepingShadeEntity;
import fr.leho.wisplight.entity.WispEntity;
import fr.leho.wisplight.entity.WispVariant;
import fr.leho.wisplight.item.WatcherArmor;
import fr.leho.wisplight.registry.ModEntities;
import fr.leho.wisplight.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Wisplight.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShadeTests {
    private ShadeTests() {}

    /** Le lumen inflige des dégâts doublés aux Ombres ; le fer, non. */
    @GameTest(template = "platform")
    public static void lumenHurtsShadesTwice(GameTestHelper helper) {
        CreepingShadeEntity a = helper.spawn(ModEntities.CREEPING_SHADE.get(), new BlockPos(2, 2, 2));
        CreepingShadeEntity b = helper.spawn(ModEntities.CREEPING_SHADE.get(), new BlockPos(6, 2, 6));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LUMEN_SWORD.get()));
        a.hurt(player.damageSources().playerAttack(player), 2.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        b.hurt(player.damageSources().playerAttack(player), 2.0F);
        float lostA = a.getMaxHealth() - a.getHealth();
        float lostB = b.getMaxHealth() - b.getHealth();
        helper.assertTrue(Math.abs(lostA - 4.0F) < 0.01F, "Lumen : 4 dégâts attendus, obtenu " + lostA);
        helper.assertTrue(Math.abs(lostB - 2.0F) < 0.01F, "Fer : 2 dégâts attendus, obtenu " + lostB);
        helper.succeed();
    }

    /** Une Ombre vole une essence et la rend à sa mort. */
    @GameTest(template = "platform")
    public static void shadeStealsEssence(GameTestHelper helper) {
        CreepingShadeEntity shade = helper.spawn(ModEntities.CREEPING_SHADE.get(), new BlockPos(4, 2, 4));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().add(new ItemStack(ModItems.ESSENCES.get(WispVariant.AZUR).get(), 3));
        helper.assertTrue(shade.stealEssence(player), "Le vol doit réussir");
        helper.assertTrue(shade.stolenCount() == 1, "Une essence volée");
        helper.assertTrue(player.getInventory().countItem(ModItems.ESSENCES.get(WispVariant.AZUR).get()) == 2,
                "Il reste 2 essences au joueur");
        helper.succeed();
    }

    /** Le bocal renforcé capture un follet même sans être accroupi. */
    @GameTest(template = "empty")
    public static void reinforcedJarCaptures(GameTestHelper helper) {
        WispEntity wisp = helper.spawn(ModEntities.WISP.get(), new BlockPos(2, 2, 2));
        wisp.setVariant(WispVariant.BRAISE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.REINFORCED_JAR.get()));
        wisp.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(wisp.isRemoved(), "Capture réussie");
        helper.assertTrue(player.getInventory().contains(new ItemStack(ModItems.WISP_JARS.get(WispVariant.BRAISE).get())),
                "Bocal Braise reçu");
        helper.succeed();
    }

    /** L'Armure du Veilleur complète rassure les follets, même arme à la main. */
    @GameTest(template = "empty")
    public static void watcherSetCalmsWisps(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LUMEN_SWORD.get()));
        helper.assertTrue(WispEntity.scares(player), "Sans armure, l'épée effraie");
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.WATCHER_HELMET.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.WATCHER_CHESTPLATE.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.WATCHER_LEGGINGS.get()));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.WATCHER_BOOTS.get()));
        helper.assertTrue(WatcherArmor.hasFullSet(player), "Set complet détecté");
        helper.assertTrue(!WispEntity.scares(player), "Avec le set, les follets restent calmes");
        helper.succeed();
    }
}
