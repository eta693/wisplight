package fr.leho.wisplight;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Configuration commune : wisplight-common.toml */
public final class WisplightConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue DYNAMIC_LIGHT;
    public static final ModConfigSpec.IntValue JAR_SECONDS_PER_ESSENCE;
    public static final ModConfigSpec.IntValue MAX_WISPS_NEARBY;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("follets");
        DYNAMIC_LIGHT = b.comment("Les follets éclairent réellement autour d'eux (lumière dynamique).")
                .define("dynamicLight", true);
        MAX_WISPS_NEARBY = b.comment("Nombre maximum de follets dans un rayon de 32 blocs pour qu'un nouveau apparaisse.")
                .defineInRange("maxWispsNearby", 6, 1, 64);
        b.pop();
        b.push("bocaux");
        JAR_SECONDS_PER_ESSENCE = b.comment("Secondes nécessaires à un bocal pour produire une essence (hors bonus).")
                .defineInRange("secondsPerEssence", 300, 10, 36000);
        b.pop();
        SPEC = b.build();
    }

    private WisplightConfig() {}

    /** Lecture sûre, même si la configuration n'est pas encore chargée. */
    public static boolean dynamicLight() {
        return SPEC.isLoaded() ? DYNAMIC_LIGHT.get() : true;
    }

    public static int maxWispsNearby() {
        return SPEC.isLoaded() ? MAX_WISPS_NEARBY.get() : 6;
    }

    public static int jarTicksPerEssence() {
        return (SPEC.isLoaded() ? JAR_SECONDS_PER_ESSENCE.get() : 300) * 20;
    }
}
