package fr.leho.wisplight.client;

import fr.leho.wisplight.Wisplight;
import fr.leho.wisplight.item.GrimoireItem;
import fr.leho.wisplight.tour.TourDirector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Côté client de la visite automatique : prend une capture quand le serveur annonce une scène. */
@EventBusSubscriber(modid = Wisplight.MOD_ID, value = Dist.CLIENT)
public final class ClientTour {
    private static final int SHOT_DELAY = 110;
    private static String pending;
    private static int countdown = -1;
    private static int stopCountdown = -1;

    private ClientTour() {}

    @SubscribeEvent
    public static void onChat(ClientChatReceivedEvent event) {
        if (!TourDirector.ENABLED) {
            return;
        }
        String text = event.getMessage().getString();
        if (!text.startsWith(TourDirector.PREFIX)) {
            return;
        }
        event.setCanceled(true);
        String name = text.substring(TourDirector.PREFIX.length()).trim();
        Minecraft mc = Minecraft.getInstance();
        if (name.equals("end")) {
            stopCountdown = 40;
            return;
        }
        if (mc.screen != null && !name.contains("interface")) {
            mc.setScreen(null);
        }
        if (name.contains("grimoire")) {
            ClientHooks.openGrimoire(GrimoireItem.PAGES);
        }
        pending = name;
        countdown = SHOT_DELAY;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!TourDirector.ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        mc.options.hideGui = true;
        if (mc.player != null && !mc.player.getAbilities().flying) {
            mc.player.getAbilities().flying = true;
            mc.player.onUpdateAbilities();
        }
        if (countdown > 0 && --countdown == 0 && pending != null) {
            Screenshot.grab(mc.gameDirectory, pending + ".png", mc.getMainRenderTarget(),
                    msg -> Wisplight.LOGGER.info("Visite : {}", msg.getString()));
            if (pending.contains("grimoire")) {
                stopCountdown = 30; // le livre met le jeu en pause : on termine ici
            }
            pending = null;
        }
        if (stopCountdown > 0 && --stopCountdown == 0) {
            Wisplight.LOGGER.info("Visite terminée, fermeture du client.");
            mc.stop();
        }
    }
}
