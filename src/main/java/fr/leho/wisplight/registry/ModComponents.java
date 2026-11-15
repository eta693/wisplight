package fr.leho.wisplight.registry;

import fr.leho.wisplight.Wisplight;
import fr.leho.wisplight.entity.WispVariant;
import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Wisplight.MOD_ID);

    /** Infusion permanente posée par l'autel sur un équipement en lumen. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WispVariant>> INFUSION =
            COMPONENTS.register("infusion", () -> DataComponentType.<WispVariant>builder()
                    .persistent(WispVariant.CODEC)
                    .networkSynchronized(WispVariant.STREAM_CODEC)
                    .build());

    private ModComponents() {}

    @Nullable
    public static WispVariant infusion(ItemStack stack) {
        return stack.isEmpty() ? null : stack.get(INFUSION.get());
    }

    public static boolean hasInfusion(ItemStack stack, WispVariant variant) {
        return infusion(stack) == variant;
    }

    public static ItemStack infuse(ItemStack stack, WispVariant variant) {
        stack.set(INFUSION.get(), variant);
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }
}
