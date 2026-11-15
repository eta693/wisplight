package fr.leho.wisplight.registry;

import fr.leho.wisplight.Wisplight;
import fr.leho.wisplight.menu.InfusionAltarMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Wisplight.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<InfusionAltarMenu>> INFUSION_ALTAR =
            MENUS.register("infusion_altar", () -> IMenuTypeExtension.create(InfusionAltarMenu::new));

    private ModMenus() {}
}
