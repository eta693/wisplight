package fr.leho.wisplight;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Wisplight.MOD_ID)
public final class Wisplight {
    public static final String MOD_ID = "wisplight";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Wisplight(IEventBus modBus, ModContainer container) {
        LOGGER.info("Wisplight : les follets s'éveillent.");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
