package fr.leho.wisplight.registry;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/** Matériau d'outil en lumen : entre le fer et le diamant. */
public enum ModTiers implements Tier {
    LUMEN(800, 7.0F, 2.5F, 18);

    private final int uses;
    private final float speed;
    private final float damage;
    private final int enchantability;

    ModTiers(int uses, float speed, float damage, int enchantability) {
        this.uses = uses;
        this.speed = speed;
        this.damage = damage;
        this.enchantability = enchantability;
    }

    @Override
    public int getUses() {
        return uses;
    }

    @Override
    public float getSpeed() {
        return speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return damage;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return BlockTags.INCORRECT_FOR_IRON_TOOL;
    }

    @Override
    public int getEnchantmentValue() {
        return enchantability;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(ModItems.LUMEN_INGOT.get());
    }
}
