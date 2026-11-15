package fr.leho.wisplight.infusion;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fr.leho.wisplight.entity.WispVariant;
import fr.leho.wisplight.registry.ModComponents;
import fr.leho.wisplight.registry.ModRecipes;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Recette d'infusion (type {@code wisplight:infusion}). Deux formes :
 * <ul>
 *   <li>fabrication : {@code result} donne un nouvel objet ;</li>
 *   <li>infusion d'équipement : {@code infuse} copie l'objet d'entrée et lui pose l'infusion.</li>
 * </ul>
 * Les deux essences peuvent être placées dans n'importe quel ordre.
 */
public record InfusionRecipe(Ingredient ingredient, Ingredient essenceA, Ingredient essenceB,
                             Optional<ItemStack> result, Optional<WispVariant> infuse) implements Recipe<InfusionInput> {

    public static final MapCodec<InfusionRecipe> CODEC = RecordCodecBuilder.<InfusionRecipe>mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(InfusionRecipe::ingredient),
            Ingredient.CODEC_NONEMPTY.fieldOf("essence_a").forGetter(InfusionRecipe::essenceA),
            Ingredient.CODEC_NONEMPTY.fieldOf("essence_b").forGetter(InfusionRecipe::essenceB),
            ItemStack.STRICT_CODEC.optionalFieldOf("result").forGetter(InfusionRecipe::result),
            WispVariant.CODEC.optionalFieldOf("infuse").forGetter(InfusionRecipe::infuse)
    ).apply(instance, InfusionRecipe::new)).validate(recipe -> recipe.result.isPresent() == recipe.infuse.isPresent()
            ? DataResult.error(() -> "Une recette d'infusion a soit 'result', soit 'infuse'")
            : DataResult.success(recipe));

    public static final StreamCodec<RegistryFriendlyByteBuf, InfusionRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, InfusionRecipe::ingredient,
            Ingredient.CONTENTS_STREAM_CODEC, InfusionRecipe::essenceA,
            Ingredient.CONTENTS_STREAM_CODEC, InfusionRecipe::essenceB,
            ByteBufCodecs.optional(ItemStack.STREAM_CODEC), InfusionRecipe::result,
            ByteBufCodecs.optional(WispVariant.STREAM_CODEC), InfusionRecipe::infuse,
            InfusionRecipe::new);

    @Override
    public boolean matches(InfusionInput input, Level level) {
        if (!ingredient.test(input.item())) {
            return false;
        }
        if (infuse.isPresent() && ModComponents.infusion(input.item()) == infuse.get()) {
            return false; // déjà infusé avec cette essence : inutile de gaspiller
        }
        return (essenceA.test(input.essenceA()) && essenceB.test(input.essenceB()))
                || (essenceA.test(input.essenceB()) && essenceB.test(input.essenceA()));
    }

    @Override
    public ItemStack assemble(InfusionInput input, HolderLookup.Provider registries) {
        if (infuse.isPresent()) {
            return ModComponents.infuse(input.item().copyWithCount(1), infuse.get());
        }
        return result.map(ItemStack::copy).orElse(ItemStack.EMPTY);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result.map(ItemStack::copy).orElse(ItemStack.EMPTY);
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.INFUSION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.INFUSION_TYPE.get();
    }

    public static final class Serializer implements RecipeSerializer<InfusionRecipe> {
        @Override
        public MapCodec<InfusionRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, InfusionRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
