package fr.leho.wisplight.item;

import fr.leho.wisplight.entity.WispOrbEntity;
import fr.leho.wisplight.registry.ModComponents;
import fr.leho.wisplight.registry.ModSounds;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Bâton des follets : tire un orbe de lumière dont l'effet dépend de l'infusion. */
public class WispStaffItem extends Item {
    public static final int COOLDOWN = 20;

    public WispStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            shoot(level, player, stack);
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.STAFF_SHOOT.get(),
                    SoundSource.PLAYERS, 0.7F, 0.9F + level.random.nextFloat() * 0.2F);
        }
        player.getCooldowns().addCooldown(this, COOLDOWN);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static WispOrbEntity shoot(Level level, LivingEntity shooter, ItemStack staff) {
        WispOrbEntity orb = new WispOrbEntity(level, shooter, ModComponents.infusion(staff));
        orb.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot(), 0.0F, 1.6F, 0.5F);
        level.addFreshEntity(orb);
        return orb;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (ModComponents.infusion(stack) == null) {
            tooltip.add(Component.translatable("tooltip.wisplight.staff.plain").withStyle(ChatFormatting.GRAY));
        }
    }
}
