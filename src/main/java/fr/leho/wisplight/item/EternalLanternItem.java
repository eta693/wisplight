package fr.leho.wisplight.item;

import fr.leho.wisplight.block.TraceLightBlock;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Lanterne éternelle : tenue en main (principale ou secondaire), elle éclaire autour du joueur. */
public class EternalLanternItem extends Item {
    public EternalLanternItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player) || player.tickCount % 4 != 0) {
            return;
        }
        if (player.getMainHandItem() == stack || player.getOffhandItem() == stack) {
            TraceLightBlock.refresh(level, BlockPos.containing(player.getX(), player.getEyeY(), player.getZ()));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.wisplight.eternal_lantern").withStyle(ChatFormatting.GRAY));
    }
}
