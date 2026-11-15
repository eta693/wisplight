package fr.leho.wisplight.registry;

import fr.leho.wisplight.Wisplight;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Succès accordés par le code (critère « code » de type minecraft:impossible). */
public final class ModAdvancements {
    public static final String SEE_WISP = "root";
    public static final String FIRST_INFUSION = "first_infusion";
    public static final String ON_THE_TRAIL = "on_the_trail";
    public static final String RITUAL = "the_ritual";

    private ModAdvancements() {}

    public static void award(Player player, String path) {
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.server != null) {
            AdvancementHolder holder = serverPlayer.server.getAdvancements().get(Wisplight.id(path));
            if (holder != null && !serverPlayer.getAdvancements().getOrStartProgress(holder).isDone()) {
                serverPlayer.getAdvancements().award(holder, "code");
            }
        }
    }
}
