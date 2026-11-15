package fr.leho.wisplight.registry;

import fr.leho.wisplight.Wisplight;
import fr.leho.wisplight.infusion.InfusionRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Wisplight.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Wisplight.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<InfusionRecipe>> INFUSION_TYPE =
            TYPES.register("infusion", () -> RecipeType.simple(Wisplight.id("infusion")));

    public static final DeferredHolder<RecipeSerializer<?>, InfusionRecipe.Serializer> INFUSION_SERIALIZER =
            SERIALIZERS.register("infusion", InfusionRecipe.Serializer::new);

    private ModRecipes() {}
}
