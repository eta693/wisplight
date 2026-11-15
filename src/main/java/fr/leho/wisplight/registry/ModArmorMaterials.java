package fr.leho.wisplight.registry;

import fr.leho.wisplight.Wisplight;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, Wisplight.MOD_ID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> LUMEN = ARMOR_MATERIALS.register("lumen",
            () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.BOOTS, 2);
                        map.put(ArmorItem.Type.LEGGINGS, 6);
                        map.put(ArmorItem.Type.CHESTPLATE, 7);
                        map.put(ArmorItem.Type.HELMET, 3);
                        map.put(ArmorItem.Type.BODY, 7);
                    }),
                    18,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    () -> Ingredient.of(ModItems.LUMEN_INGOT.get()),
                    List.of(new ArmorMaterial.Layer(Wisplight.id("lumen"))),
                    1.0F,
                    0.0F));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> WATCHER = ARMOR_MATERIALS.register("watcher",
            () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.BOOTS, 3);
                        map.put(ArmorItem.Type.LEGGINGS, 6);
                        map.put(ArmorItem.Type.CHESTPLATE, 8);
                        map.put(ArmorItem.Type.HELMET, 3);
                        map.put(ArmorItem.Type.BODY, 8);
                    }),
                    15,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(ModItems.SHADOW_FRAGMENT.get()),
                    List.of(new ArmorMaterial.Layer(Wisplight.id("watcher"))),
                    2.0F,
                    0.05F));

    private ModArmorMaterials() {}
}
