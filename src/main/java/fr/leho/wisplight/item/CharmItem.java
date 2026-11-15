package fr.leho.wisplight.item;

import fr.leho.wisplight.entity.WispVariant;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Charme : effet passif tant qu'il est dans la barre rapide ou en main secondaire. */
public class CharmItem extends Item {
    public static final String LAST_HURT_KEY = "wisplight:last_hurt";
    public static final int LIFE_DELAY_TICKS = 200;
    public static final int FIRE_COOLDOWN = 1200;

    private final WispVariant variant;

    public CharmItem(WispVariant variant, Properties properties) {
        super(properties);
        this.variant = variant;
    }

    public WispVariant variant() {
        return variant;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player) || slot >= 9 || player.tickCount % 10 != 0) {
            return;
        }
        applyEffect(player, level);
    }

    /** Applique l'effet du charme (public pour les GameTests). */
    public void applyEffect(Player player, Level level) {
        switch (variant) {
            case AZUR -> {
                if (level.getMaxLocalRawBrightness(player.blockPosition()) <= 3) {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, true));
                }
            }
            case EMERAUDE -> {
                long lastHurt = player.getPersistentData().getLong(LAST_HURT_KEY);
                if (level.getGameTime() - lastHurt > LIFE_DELAY_TICKS
                        && player.getHealth() < player.getMaxHealth()
                        && !player.hasEffect(MobEffects.REGENERATION)) {
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, true));
                }
            }
            case BRAISE -> {
                if (player.isOnFire() && !player.hasEffect(MobEffects.FIRE_RESISTANCE)
                        && !player.getCooldowns().isOnCooldown(this)) {
                    player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0, true, false, true));
                    player.getCooldowns().addCooldown(this, FIRE_COOLDOWN);
                }
            }
            case GIVRE -> {
                if (player.getTicksFrozen() > 0) {
                    player.setTicksFrozen(0);
                }
                BlockState below = level.getBlockState(player.getOnPos());
                if ((below.is(BlockTags.SNOW) || below.is(BlockTags.ICE)) && !player.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 0, true, false, true));
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.wisplight.charm." + variant.id()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.wisplight.charm.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
