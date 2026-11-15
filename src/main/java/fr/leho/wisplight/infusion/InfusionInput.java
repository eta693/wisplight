package fr.leho.wisplight.infusion;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** Contenu de l'autel : l'objet à transformer et les deux essences. */
public record InfusionInput(ItemStack item, ItemStack essenceA, ItemStack essenceB) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> item;
            case 1 -> essenceA;
            case 2 -> essenceB;
            default -> throw new IllegalArgumentException("Index d'infusion invalide : " + index);
        };
    }

    @Override
    public int size() {
        return 3;
    }
}
