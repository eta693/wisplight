package fr.leho.wisplight.item;

import fr.leho.wisplight.Wisplight;
import fr.leho.wisplight.registry.ModSounds;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

/** Boussole des lueurs : un clic droit la règle sur le Sanctuaire oublié le plus proche. */
public class SanctuaryCompassItem extends CompassItem {
    public static final TagKey<Structure> SANCTUARIES = TagKey.create(Registries.STRUCTURE, Wisplight.id("sanctuaries"));
    public static final int SEARCH_RADIUS = 100;
    public static final int COOLDOWN = 100;

    public SanctuaryCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            BlockPos found = server.findNearestMapStructure(SANCTUARIES, player.blockPosition(), SEARCH_RADIUS, false);
            if (found == null) {
                player.displayClientMessage(Component.translatable("message.wisplight.compass.none"), true);
            } else {
                stack.set(DataComponents.LODESTONE_TRACKER,
                        new LodestoneTracker(Optional.of(GlobalPos.of(level.dimension(), found)), false));
                int dx = found.getX() - player.getBlockX();
                int dz = found.getZ() - player.getBlockZ();
                int distance = (int) Math.sqrt((double) dx * dx + (double) dz * dz);
                player.displayClientMessage(Component.translatable("message.wisplight.compass.found",
                        distance, direction(dx, dz)), true);
                level.playSound(null, player.blockPosition(), ModSounds.COMPASS_LOCATE.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
                fr.leho.wisplight.registry.ModAdvancements.award(player, fr.leho.wisplight.registry.ModAdvancements.ON_THE_TRAIL);
            }
            player.getCooldowns().addCooldown(this, COOLDOWN);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Point cardinal (8 directions) vers lequel se trouve la cible. */
    public static Component direction(int dx, int dz) {
        double angle = Mth.atan2(dx, -dz) * Mth.RAD_TO_DEG;
        int sector = Math.floorMod((int) Math.round(angle / 45.0), 8);
        String[] keys = {"n", "ne", "e", "se", "s", "sw", "w", "nw"};
        return Component.translatable("direction.wisplight." + keys[sector]);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return InteractionResult.PASS; // pas de comportement « pierre d'aimant »
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return getDescriptionId();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.wisplight.compass").withStyle(ChatFormatting.GRAY));
    }
}
