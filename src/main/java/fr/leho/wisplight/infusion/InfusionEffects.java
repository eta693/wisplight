package fr.leho.wisplight.infusion;

import fr.leho.wisplight.Wisplight;
import fr.leho.wisplight.block.LingeringGlowBlock;
import fr.leho.wisplight.block.TraceLightBlock;
import fr.leho.wisplight.entity.WispVariant;
import fr.leho.wisplight.item.CharmItem;
import fr.leho.wisplight.item.WispStaffItem;
import fr.leho.wisplight.registry.ModComponents;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Tous les effets des infusions (armes, outils, armures) et l'affichage dans les infobulles. */
@EventBusSubscriber(modid = Wisplight.MOD_ID)
public final class InfusionEffects {
    private InfusionEffects() {}

    public static boolean isWeapon(ItemStack stack) {
        return stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem;
    }

    public static boolean isTool(ItemStack stack) {
        return stack.getItem() instanceof DiggerItem;
    }

    public static int armorPieces(LivingEntity entity, WispVariant variant) {
        int n = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (ModComponents.hasInfusion(entity.getItemBySlot(slot), variant)) {
                n++;
            }
        }
        return n;
    }

    // ------------------------------------------------------------------ armes

    @SubscribeEvent
    public static void onDamageDealt(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide || !(event.getSource().getEntity() instanceof LivingEntity attacker)
                || event.getSource().getDirectEntity() != attacker) {
            return;
        }
        ItemStack weapon = attacker.getMainHandItem();
        WispVariant v = ModComponents.infusion(weapon);
        if (v == null || !isWeapon(weapon)) {
            return;
        }
        switch (v) {
            case AZUR -> target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0), attacker);
            case EMERAUDE -> attacker.heal(event.getNewDamage() * 0.10F);
            case BRAISE -> target.igniteForSeconds(4.0F);
            case GIVRE -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), attacker);
        }
    }

    // ------------------------------------------------------------------ armures

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }
        entity.getPersistentData().putLong(CharmItem.LAST_HURT_KEY, entity.level().getGameTime());
        if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
            int pieces = armorPieces(entity, WispVariant.BRAISE);
            if (pieces > 0) {
                event.setAmount(event.getAmount() * (1.0F - 0.15F * pieces));
            }
        }
    }

    @SubscribeEvent
    public static void onAttributeModifiers(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof ArmorItem armor && ModComponents.hasInfusion(stack, WispVariant.EMERAUDE)) {
            EquipmentSlot slot = armor.getEquipmentSlot();
            event.addModifier(Attributes.MAX_HEALTH,
                    new AttributeModifier(Wisplight.id("life_infusion_" + slot.getName()), 2.0, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.bySlot(slot));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide) {
            return;
        }
        // Armure de Lumière : lumière dynamique
        if (player.tickCount % 4 == 0 && armorPieces(player, WispVariant.AZUR) > 0) {
            TraceLightBlock.refresh(level, BlockPos.containing(player.getX(), player.getEyeY(), player.getZ()));
        }
        // Bottes de Froid : l'eau gèle sous les pas
        if (player.onGround() && ModComponents.hasInfusion(player.getItemBySlot(EquipmentSlot.FEET), WispVariant.GIVRE)) {
            freezeWater(level, player.blockPosition().below(), 2);
        }
        // Outils de Vie : réparation lente au soleil
        if (player.tickCount % 100 == 0 && level.isDay() && level.canSeeSky(player.blockPosition())) {
            for (ItemStack stack : player.getInventory().items) {
                if (stack.isDamaged() && ModComponents.hasInfusion(stack, WispVariant.EMERAUDE)
                        && (isTool(stack) || isWeapon(stack) || stack.getItem() instanceof WispStaffItem)) {
                    stack.setDamageValue(stack.getDamageValue() - 1);
                }
            }
        }
    }

    /** Transforme l'eau (source) autour d'une position en glace givrée, comme Semelles givrantes. */
    public static void freezeWater(Level level, BlockPos center, int radius) {
        BlockState frosted = Blocks.FROSTED_ICE.defaultBlockState();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -1, -radius), center.offset(radius, 1, radius))) {
            if (pos.closerToCenterThan(center.getCenter(), radius + 0.5)) {
                BlockState state = level.getBlockState(pos);
                if (state.is(Blocks.WATER) && state.getFluidState().is(Fluids.WATER) && state.getFluidState().isSource()
                        && level.getBlockState(pos.above()).isAir()
                        && frosted.canSurvive(level, pos)
                        && level.isUnobstructed(frosted, pos, CollisionContext.empty())) {
                    level.setBlockAndUpdate(pos, frosted);
                    level.scheduleTick(pos, Blocks.FROSTED_ICE, Mth.nextInt(level.random, 60, 120));
                }
            }
        }
    }

    // ------------------------------------------------------------------ outils

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getMainHandItem();
        if (!(event.getLevel() instanceof ServerLevel level) || !isTool(tool)
                || !ModComponents.hasInfusion(tool, WispVariant.AZUR)) {
            return;
        }
        BlockPos pos = event.getPos().immutable();
        if (level.getMaxLocalRawBrightness(pos) < 5 || level.getMaxLocalRawBrightness(player.blockPosition()) < 5) {
            MinecraftServer server = level.getServer();
            server.tell(new TickTask(server.getTickCount() + 1, () -> LingeringGlowBlock.place(level, pos, 0)));
        }
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        ItemStack tool = event.getTool();
        if (!(tool.getItem() instanceof PickaxeItem) || !ModComponents.hasInfusion(tool, WispVariant.BRAISE)) {
            return;
        }
        ServerLevel level = event.getLevel();
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            SingleRecipeInput input = new SingleRecipeInput(stack);
            Optional<RecipeHolder<SmeltingRecipe>> recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level);
            recipe.ifPresent(holder -> {
                ItemStack smelted = holder.value().assemble(input, level.registryAccess());
                if (!smelted.isEmpty()) {
                    drop.setItem(smelted.copyWithCount(smelted.getCount() * stack.getCount()));
                }
            });
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        ItemStack tool = event.getEntity().getMainHandItem();
        if ((tool.getItem() instanceof AxeItem || tool.getItem() instanceof ShovelItem)
                && ModComponents.hasInfusion(tool, WispVariant.GIVRE)) {
            BlockState state = event.getState();
            if (state.is(BlockTags.SNOW) || state.is(BlockTags.ICE)) {
                event.setNewSpeed(event.getNewSpeed() * 1.2F);
            }
        }
    }

    // ------------------------------------------------------------------ infobulles

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        WispVariant v = ModComponents.infusion(stack);
        if (v == null) {
            return;
        }
        List<Component> tooltip = event.getToolTip();
        int at = Math.min(1, tooltip.size());
        tooltip.add(at++, Component.translatable("tooltip.wisplight.infusion", v.essenceName()).withStyle(ChatFormatting.GRAY));
        if (stack.getItem() instanceof WispStaffItem) {
            tooltip.add(at++, effectLine(v, "staff"));
        }
        if (isWeapon(stack)) {
            tooltip.add(at++, effectLine(v, "weapon"));
        }
        if (isTool(stack)) {
            tooltip.add(at++, effectLine(v, "tool"));
        }
        if (stack.getItem() instanceof ArmorItem) {
            tooltip.add(at, effectLine(v, "armor"));
        }
    }

    private static Component effectLine(WispVariant v, String category) {
        return Component.literal(" ").append(Component.translatable("tooltip.wisplight.infusion." + v.id() + "." + category))
                .withStyle(ChatFormatting.DARK_GRAY);
    }
}
