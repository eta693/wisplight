package fr.leho.wisplight.client;

import fr.leho.wisplight.Wisplight;
import fr.leho.wisplight.registry.ModBlockEntities;
import fr.leho.wisplight.registry.ModEntities;
import fr.leho.wisplight.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Wisplight.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.WISP.get(), WispRenderer::new);
        event.registerEntityRenderer(ModEntities.WISP_ORB.get(), WispOrbRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.WISP_JAR.get(), WispJarRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.INFUSION_ALTAR.get(), InfusionAltarScreen::new);
    }
}
