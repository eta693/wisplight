package fr.leho.wisplight.item;

import fr.leho.wisplight.registry.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/** Bonus de l'Armure du Veilleur complète : les follets ne fuient plus, les Ombres voient mal. */
public final class WatcherArmor {
    private WatcherArmor() {}

    public static boolean hasFullSet(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.WATCHER_HELMET.get())
                && entity.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.WATCHER_CHESTPLATE.get())
                && entity.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.WATCHER_LEGGINGS.get())
                && entity.getItemBySlot(EquipmentSlot.FEET).is(ModItems.WATCHER_BOOTS.get());
    }
}
