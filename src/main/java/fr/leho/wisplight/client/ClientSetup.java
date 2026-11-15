package fr.leho.wisplight.client;

import fr.leho.wisplight.Wisplight;
import fr.leho.wisplight.registry.ModBlockEntities;
import fr.leho.wisplight.registry.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = Wisplight.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.WISP.get(), WispRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.WISP_JAR.get(), WispJarRenderer::new);
    }
}
