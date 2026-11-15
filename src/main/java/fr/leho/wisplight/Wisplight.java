package fr.leho.wisplight;

import com.mojang.logging.LogUtils;
import fr.leho.wisplight.registry.ModArmorMaterials;
import fr.leho.wisplight.registry.ModBlockEntities;
import fr.leho.wisplight.registry.ModBlocks;
import fr.leho.wisplight.registry.ModEntities;
import fr.leho.wisplight.registry.ModItems;
import fr.leho.wisplight.registry.ModSounds;
import fr.leho.wisplight.registry.ModTabs;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(Wisplight.MOD_ID)
public final class Wisplight {
    public static final String MOD_ID = "wisplight";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Wisplight(IEventBus modBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModArmorMaterials.ARMOR_MATERIALS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModTabs.TABS.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, WisplightConfig.SPEC);
        LOGGER.info("Wisplight : les follets s'éveillent.");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
