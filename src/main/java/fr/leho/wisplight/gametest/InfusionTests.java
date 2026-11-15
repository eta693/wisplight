package fr.leho.wisplight.gametest;

import fr.leho.wisplight.Wisplight;
import fr.leho.wisplight.blockentity.CallingLanternBlockEntity;
import fr.leho.wisplight.blockentity.InfusionAltarBlockEntity;
import fr.leho.wisplight.entity.WispEntity;
import fr.leho.wisplight.entity.WispOrbEntity;
import fr.leho.wisplight.entity.WispVariant;
import fr.leho.wisplight.item.CharmItem;
import fr.leho.wisplight.registry.ModBlocks;
import fr.leho.wisplight.registry.ModComponents;
import fr.leho.wisplight.registry.ModEntities;
import fr.leho.wisplight.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

@GameTestHolder(Wisplight.MOD_ID)
@PrefixGameTestTemplate(false)
public final class InfusionTests {
    private static final BlockPos ALTAR = new BlockPos(2, 1, 2);
    private static final BlockPos JAR = new BlockPos(4, 1, 2);

    private InfusionTests() {}

    private static InfusionAltarBlockEntity altar(GameTestHelper helper, boolean withJar) {
        helper.setBlock(ALTAR, ModBlocks.INFUSION_ALTAR.get());
        if (withJar) {
            helper.setBlock(JAR, ModBlocks.WISP_JARS.get(WispVariant.AZUR).get());
        }
        return helper.getBlockEntity(ALTAR);
    }

    private static void fill(InfusionAltarBlockEntity altar, ItemStack item, ItemStack a, ItemStack b) {
        ItemStackHandler inv = altar.getInventory();
        inv.setStackInSlot(InfusionAltarBlockEntity.SLOT_ITEM, item);
        inv.setStackInSlot(InfusionAltarBlockEntity.SLOT_ESSENCE_A, a);
        inv.setStackInSlot(InfusionAltarBlockEntity.SLOT_ESSENCE_B, b);
    }

    private static void work(GameTestHelper helper, InfusionAltarBlockEntity altar) {
        for (int i = 0; i < InfusionAltarBlockEntity.WORK_TICKS + 5; i++) {
            altar.tickServer(helper.getLevel());
        }
    }

    private static ItemStack essence(WispVariant v, int count) {
        return new ItemStack(ModItems.ESSENCES.get(v).get(), count);
    }

    /** Lingot de lumen + 2 essences de vie → Charme de vie. */
    @GameTest(template = "empty")
    public static void altarCraftsCharm(GameTestHelper helper) {
        InfusionAltarBlockEntity altar = altar(helper, true);
        fill(altar, new ItemStack(ModItems.LUMEN_INGOT.get()), essence(WispVariant.EMERAUDE, 1), essence(WispVariant.EMERAUDE, 1));
        work(helper, altar);
        ItemStack out = altar.getInventory().getStackInSlot(InfusionAltarBlockEntity.SLOT_RESULT);
        helper.assertTrue(out.is(ModItems.CHARMS.get(WispVariant.EMERAUDE).get()), "Résultat attendu : charme de vie, obtenu " + out);
        helper.assertTrue(altar.getInventory().getStackInSlot(InfusionAltarBlockEntity.SLOT_ITEM).isEmpty(), "Lingot consommé");
        helper.assertTrue(altar.getInventory().getStackInSlot(InfusionAltarBlockEntity.SLOT_ESSENCE_A).isEmpty(), "Essence A consommée");
        helper.succeed();
    }

    /** Sans bocal à proximité, l'autel ne fait rien. */
    @GameTest(template = "empty")
    public static void altarNeedsJar(GameTestHelper helper) {
        InfusionAltarBlockEntity altar = altar(helper, false);
        fill(altar, new ItemStack(ModItems.LUMEN_INGOT.get()), essence(WispVariant.AZUR, 1), essence(WispVariant.AZUR, 1));
        work(helper, altar);
        helper.assertTrue(altar.getInventory().getStackInSlot(InfusionAltarBlockEntity.SLOT_RESULT).isEmpty(),
                "Aucun résultat sans follet en bocal");
        helper.assertTrue(!altar.hasJar(), "Pas de bocal détecté");
        helper.succeed();
    }

    /** Des essences différentes ne donnent pas de charme. */
    @GameTest(template = "empty")
    public static void mixedEssencesDoNothing(GameTestHelper helper) {
        InfusionAltarBlockEntity altar = altar(helper, true);
        fill(altar, new ItemStack(ModItems.LUMEN_INGOT.get()), essence(WispVariant.AZUR, 1), essence(WispVariant.BRAISE, 1));
        work(helper, altar);
        helper.assertTrue(altar.getInventory().getStackInSlot(InfusionAltarBlockEntity.SLOT_RESULT).isEmpty(),
                "Mélange d'essences : pas de recette");
        helper.succeed();
    }

    /** Épée en lumen + 2 essences de feu → épée infusée au Feu, qui conserve ses données. */
    @GameTest(template = "empty")
    public static void altarInfusesSword(GameTestHelper helper) {
        InfusionAltarBlockEntity altar = altar(helper, true);
        ItemStack sword = new ItemStack(ModItems.LUMEN_SWORD.get());
        sword.setDamageValue(42);
        fill(altar, sword, essence(WispVariant.BRAISE, 2), essence(WispVariant.BRAISE, 2));
        work(helper, altar);
        ItemStack out = altar.getInventory().getStackInSlot(InfusionAltarBlockEntity.SLOT_RESULT);
        helper.assertTrue(out.is(ModItems.LUMEN_SWORD.get()), "L'épée sort de l'autel");
        helper.assertTrue(ModComponents.infusion(out) == WispVariant.BRAISE, "Infusion Feu posée");
        helper.assertTrue(out.getDamageValue() == 42, "L'usure de l'épée est conservée");
        helper.assertTrue(altar.getInventory().getStackInSlot(InfusionAltarBlockEntity.SLOT_ESSENCE_A).getCount() == 1,
                "Une seule essence consommée par emplacement");
        helper.succeed();
    }

    /** Une épée infusée au Feu enflamme sa cible. */
    @GameTest(template = "platform")
    public static void fireSwordIgnites(GameTestHelper helper) {
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(4, 1, 4));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, ModComponents.infuse(new ItemStack(ModItems.LUMEN_SWORD.get()), WispVariant.BRAISE));
        pig.hurt(player.damageSources().playerAttack(player), 1.0F);
        helper.assertTrue(pig.isOnFire(), "Le cochon doit brûler");
        helper.succeed();
    }

    /** Une épée infusée au Froid ralentit sa cible. */
    @GameTest(template = "platform")
    public static void frostSwordSlows(GameTestHelper helper) {
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(4, 1, 4));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, ModComponents.infuse(new ItemStack(ModItems.LUMEN_SWORD.get()), WispVariant.GIVRE));
        pig.hurt(player.damageSources().playerAttack(player), 1.0F);
        helper.assertTrue(pig.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Le cochon doit être ralenti");
        helper.succeed();
    }

    /** La pioche infusée au Feu fait fondre le minerai de fer. */
    @GameTest(template = "empty")
    public static void firePickaxeSmelts(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, Blocks.IRON_ORE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack pick = ModComponents.infuse(new ItemStack(ModItems.LUMEN_PICKAXE.get()), WispVariant.BRAISE);
        player.setItemInHand(InteractionHand.MAIN_HAND, pick);
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(pos);
        BlockState state = level.getBlockState(abs);
        Block.dropResources(state, level, abs, null, player, pick);
        helper.assertItemEntityPresent(Items.IRON_INGOT, pos, 2.0);
        helper.assertItemEntityNotPresent(Items.RAW_IRON, pos, 2.0);
        helper.succeed();
    }

    /** Le charme de feu donne Résistance au feu à un joueur en flammes. */
    @GameTest(template = "empty")
    public static void fireCharmProtects(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.igniteForSeconds(5.0F);
        CharmItem charm = ModItems.CHARMS.get(WispVariant.BRAISE).get();
        charm.applyEffect(player, helper.getLevel());
        helper.assertTrue(player.hasEffect(MobEffects.FIRE_RESISTANCE), "Résistance au feu attendue");
        helper.assertTrue(player.getCooldowns().isOnCooldown(charm), "Le charme se recharge");
        helper.succeed();
    }

    /** Un orbe simple qui touche un bloc laisse une lueur temporaire. */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void plainOrbLeavesGlow(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        WispOrbEntity orb = new WispOrbEntity(helper.getLevel(), player, null);
        Vec3 start = Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 3, 2)));
        orb.setPos(start.x, start.y, start.z);
        orb.setDeltaMovement(0, -0.8, 0);
        helper.getLevel().addFreshEntity(orb);
        helper.succeedWhen(() -> helper.assertBlockPresent(ModBlocks.LINGERING_GLOW.get(), new BlockPos(2, 1, 2)));
    }

    /** La lanterne d'appel attire les follets proches la nuit. */
    @GameTest(template = "empty")
    public static void callingLanternAttracts(GameTestHelper helper) {
        BlockPos lanternPos = new BlockPos(2, 1, 2);
        helper.setBlock(lanternPos, ModBlocks.CALLING_LANTERN.get());
        CallingLanternBlockEntity lantern = helper.getBlockEntity(lanternPos);
        WispEntity wisp = helper.spawn(ModEntities.WISP.get(), new BlockPos(1, 3, 1));
        int attracted = lantern.attract(helper.getLevel());
        helper.assertTrue(attracted >= 1, "Au moins un follet attiré");
        helper.assertTrue(helper.absolutePos(lanternPos).equals(wisp.getAttractor()), "Le follet vise la lanterne");
        helper.succeed();
    }
}
