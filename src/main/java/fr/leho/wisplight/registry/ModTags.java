package fr.leho.wisplight.registry;

import fr.leho.wisplight.Wisplight;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModTags {
    public static final TagKey<Item> ESSENCES = item("essences");
    public static final TagKey<Item> INFUSABLE = item("infusable");

    private static TagKey<Item> item(String name) {
        return TagKey.create(Registries.ITEM, Wisplight.id(name));
    }

    private ModTags() {}
}
