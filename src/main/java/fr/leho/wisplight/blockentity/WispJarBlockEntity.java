package fr.leho.wisplight.blockentity;

import fr.leho.wisplight.WisplightConfig;
import fr.leho.wisplight.block.WispJarBlock;
import fr.leho.wisplight.registry.ModBlockEntities;
import fr.leho.wisplight.registry.ModSounds;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Production d'essences. La progression avance de 4 unités par tick en temps normal,
 * 8 dans le biome d'origine du follet, 2 en plein jour à l'air libre.
 * Une essence coûte 4 × (ticks par essence) unités.
 */
public class WispJarBlockEntity extends BlockEntity {
    public static final int MAX_ESSENCES = 4;
    public static final int RATE_NORMAL = 4;
    public static final int RATE_HOME_BIOME = 8;
    public static final int RATE_DAYLIGHT = 2;
    private static final int CHECK_INTERVAL = 20;

    private int essences;
    private int progress;

    public WispJarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WISP_JAR.get(), pos, state);
    }

    public static int progressPerEssence() {
        return RATE_NORMAL * WisplightConfig.jarTicksPerEssence();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WispJarBlockEntity jar) {
        if (jar.essences >= MAX_ESSENCES || level.getGameTime() % CHECK_INTERVAL != 0) {
            return;
        }
        int amount = jar.currentRate() * CHECK_INTERVAL;
        if (level.getServer() != null && fr.leho.wisplight.WisplightWorldData.get(level.getServer()).isRekindled()) {
            amount = amount * 3 / 2; // la flamme ravivée : +50 %
        }
        jar.addProgress(amount);
    }

    /** Vitesse de production actuelle (unités par tick). */
    public int currentRate() {
        if (level == null || !(getBlockState().getBlock() instanceof WispJarBlock jar)) {
            return RATE_NORMAL;
        }
        if (level.getBiome(worldPosition).is(jar.variant().biomeTag())) {
            return RATE_HOME_BIOME;
        }
        if (!jar.variant().isNether() && level.isDay() && level.canSeeSky(worldPosition.above())) {
            return RATE_DAYLIGHT;
        }
        return RATE_NORMAL;
    }

    /** Ajoute de la progression ; produit des essences quand le seuil est atteint. */
    public void addProgress(int amount) {
        if (essences >= MAX_ESSENCES) {
            return;
        }
        progress += amount;
        int cost = progressPerEssence();
        boolean produced = false;
        while (progress >= cost && essences < MAX_ESSENCES) {
            progress -= cost;
            essences++;
            produced = true;
        }
        if (essences >= MAX_ESSENCES) {
            progress = 0;
        }
        setChanged();
        if (produced && level != null && !level.isClientSide) {
            level.playSound(null, worldPosition, ModSounds.JAR_ESSENCE_READY.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
            sync();
        }
    }

    public int getEssences() {
        return essences;
    }

    public int getProgress() {
        return progress;
    }

    /** Retire une essence ; renvoie false s'il n'y en a pas. */
    public boolean takeEssence() {
        if (essences <= 0) {
            return false;
        }
        essences--;
        setChanged();
        sync();
        return true;
    }

    private void sync() {
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
            level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Essences", essences);
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        essences = Math.min(tag.getInt("Essences"), MAX_ESSENCES);
        progress = tag.getInt("Progress");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
