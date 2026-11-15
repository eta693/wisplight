package fr.leho.wisplight;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Données du monde : la flamme a-t-elle été ravivée (Veilleur vaincu au moins une fois) ? */
public class WisplightWorldData extends SavedData {
    private static final String NAME = "wisplight";
    private boolean rekindled;

    public static WisplightWorldData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(WisplightWorldData::new, WisplightWorldData::load, null), NAME);
    }

    private static WisplightWorldData load(CompoundTag tag, HolderLookup.Provider registries) {
        WisplightWorldData data = new WisplightWorldData();
        data.rekindled = tag.getBoolean("Rekindled");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("Rekindled", rekindled);
        return tag;
    }

    public boolean isRekindled() {
        return rekindled;
    }

    public void setRekindled(boolean value) {
        if (rekindled != value) {
            rekindled = value;
            setDirty();
        }
    }
}
