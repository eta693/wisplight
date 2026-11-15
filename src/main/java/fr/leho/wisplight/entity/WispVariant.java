package fr.leho.wisplight.entity;

import com.mojang.serialization.Codec;
import fr.leho.wisplight.Wisplight;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.biome.Biome;

/** Les quatre variantes de feux follets et leur essence. */
public enum WispVariant implements StringRepresentable {
    AZUR("azur", 0x7FD0FF, false),
    EMERAUDE("emeraude", 0x6FE39A, false),
    BRAISE("braise", 0xFF9A3C, true),
    GIVRE("givre", 0xCFEAFF, false);

    public static final Codec<WispVariant> CODEC = StringRepresentable.fromEnum(WispVariant::values);

    private final String id;
    private final int color;
    private final boolean nether;
    private final TagKey<Biome> biomes;

    WispVariant(String id, int color, boolean nether) {
        this.id = id;
        this.color = color;
        this.nether = nether;
        this.biomes = TagKey.create(Registries.BIOME, Wisplight.id("wisp_" + id + "_biomes"));
    }

    public String id() {
        return id;
    }

    /** Couleur RGB (0xRRGGBB) de la lueur. */
    public int color() {
        return color;
    }

    public float red() {
        return ((color >> 16) & 0xFF) / 255f;
    }

    public float green() {
        return ((color >> 8) & 0xFF) / 255f;
    }

    public float blue() {
        return (color & 0xFF) / 255f;
    }

    public boolean isNether() {
        return nether;
    }

    /** Biomes d'origine : apparition naturelle et bonus de production du bocal. */
    public TagKey<Biome> biomeTag() {
        return biomes;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public static WispVariant byIndex(int index) {
        WispVariant[] values = values();
        return index >= 0 && index < values.length ? values[index] : AZUR;
    }

    public static WispVariant byId(String id) {
        for (WispVariant v : values()) {
            if (v.id.equals(id)) {
                return v;
            }
        }
        return AZUR;
    }

    /** Choisit la variante adaptée au biome (Braise > Émeraude > Givre > Azur). */
    public static WispVariant forBiome(Holder<Biome> biome) {
        if (biome.is(BRAISE.biomes)) return BRAISE;
        if (biome.is(EMERAUDE.biomes)) return EMERAUDE;
        if (biome.is(GIVRE.biomes)) return GIVRE;
        return AZUR;
    }
}
