package fr.leho.wisplight.blockentity;

import fr.leho.wisplight.block.RunestoneBlock;
import fr.leho.wisplight.entity.HollowWardenEntity;
import fr.leho.wisplight.entity.WispVariant;
import fr.leho.wisplight.registry.ModBlockEntities;
import fr.leho.wisplight.registry.ModEntities;
import fr.leho.wisplight.registry.ModSounds;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

/**
 * Rituel : 4 pierres runiques chargées (une par essence) dans un rayon de 4 blocs, la nuit.
 * Les runes s'allument une à une (toutes les secondes), puis le Veilleur Éteint apparaît.
 */
public class SanctuaryAltarBlockEntity extends BlockEntity {
    public static final int RUNE_RADIUS = 4;
    public static final int STEP_TICKS = 20;
    public static final int SUMMON_TICK = STEP_TICKS * 5;

    private int ritualTicks;
    private final List<BlockPos> runes = new ArrayList<>();

    public SanctuaryAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SANCTUARY_ALTAR.get(), pos, state);
    }

    public boolean isRitualRunning() {
        return ritualTicks > 0;
    }

    /** Cherche une pierre runique chargée de chaque essence autour de l'autel. */
    public Map<WispVariant, BlockPos> findChargedRunes(Level level) {
        Map<WispVariant, BlockPos> found = new EnumMap<>(WispVariant.class);
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-RUNE_RADIUS, -1, -RUNE_RADIUS),
                worldPosition.offset(RUNE_RADIUS, 1, RUNE_RADIUS))) {
            BlockState state = level.getBlockState(p);
            if (state.getBlock() instanceof RunestoneBlock rune && state.getValue(RunestoneBlock.CHARGED)) {
                found.putIfAbsent(rune.variant(), p.immutable());
            }
        }
        return found;
    }

    /**
     * Tente de lancer le rituel. {@code force} ignore la condition de nuit (GameTests).
     * Renvoie true si le rituel commence (le bocal offert est alors consommé).
     */
    public boolean tryStartRitual(ServerLevel level, Player player, boolean force) {
        if (isRitualRunning()) {
            return false;
        }
        if (!level.getEntitiesOfClass(HollowWardenEntity.class, new AABB(worldPosition).inflate(64)).isEmpty()) {
            player.displayClientMessage(Component.translatable("message.wisplight.ritual.already_awake"), true);
            return false;
        }
        if (!force && level.isDay()) {
            player.displayClientMessage(Component.translatable("message.wisplight.ritual.night_only"), true);
            return false;
        }
        Map<WispVariant, BlockPos> charged = findChargedRunes(level);
        if (charged.size() < WispVariant.values().length) {
            player.displayClientMessage(Component.translatable("message.wisplight.ritual.missing_runes",
                    charged.size(), WispVariant.values().length), true);
            return false;
        }
        runes.clear();
        for (WispVariant v : WispVariant.values()) {
            runes.add(charged.get(v));
        }
        ritualTicks = 1;
        level.playSound(null, worldPosition, ModSounds.RITUAL_START.get(), SoundSource.BLOCKS, 1.5F, 1.0F);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2,
                worldPosition.getZ() + 0.5, 30, 0.3, 0.4, 0.3, 0.02);
        setChanged();
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SanctuaryAltarBlockEntity altar) {
        if (level instanceof ServerLevel server && altar.ritualTicks > 0) {
            altar.tickRitual(server);
        }
    }

    public void tickRitual(ServerLevel level) {
        ritualTicks++;
        if (ritualTicks % STEP_TICKS == 0 && ritualTicks < SUMMON_TICK) {
            int index = ritualTicks / STEP_TICKS - 1;
            if (index >= 0 && index < runes.size()) {
                BlockPos rune = runes.get(index);
                BlockState state = level.getBlockState(rune);
                if (state.getBlock() instanceof RunestoneBlock block) {
                    level.setBlockAndUpdate(rune, state.setValue(RunestoneBlock.LIT, true));
                    WispVariant v = block.variant();
                    level.sendParticles(new DustParticleOptions(new Vector3f(v.red(), v.green(), v.blue()), 1.5F),
                            rune.getX() + 0.5, rune.getY() + 1.2, rune.getZ() + 0.5, 25, 0.3, 0.5, 0.3, 0.02);
                    level.playSound(null, rune, ModSounds.RUNE_CHARGE.get(), SoundSource.BLOCKS, 1.2F, 0.8F + 0.15F * index);
                }
            }
        }
        if (ritualTicks >= SUMMON_TICK) {
            summon(level);
        }
        setChanged();
    }

    private void summon(ServerLevel level) {
        for (BlockPos rune : runes) {
            BlockState state = level.getBlockState(rune);
            if (state.getBlock() instanceof RunestoneBlock) {
                level.setBlockAndUpdate(rune, state.setValue(RunestoneBlock.LIT, false).setValue(RunestoneBlock.CHARGED, false));
            }
        }
        runes.clear();
        ritualTicks = 0;
        HollowWardenEntity warden = ModEntities.HOLLOW_WARDEN.get().create(level);
        if (warden != null) {
            warden.moveTo(worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, 0F, 0F);
            warden.finalizeSpawn(level, level.getCurrentDifficultyAt(worldPosition), MobSpawnType.EVENT, null);
            warden.setHome(worldPosition);
            level.addFreshEntity(warden);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5,
                    worldPosition.getZ() + 0.5, 60, 0.8, 1.2, 0.8, 0.02);
            for (Player p : level.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(48))) {
                p.displayClientMessage(Component.translatable("message.wisplight.ritual.awakened"), true);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("RitualTicks", ritualTicks);
        ListTag list = new ListTag();
        for (BlockPos p : runes) {
            list.add(LongTag.valueOf(p.asLong()));
        }
        tag.put("Runes", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ritualTicks = tag.getInt("RitualTicks");
        runes.clear();
        ListTag list = tag.getList("Runes", Tag.TAG_LONG);
        for (int i = 0; i < list.size(); i++) {
            runes.add(BlockPos.of(((LongTag) list.get(i)).getAsLong()));
        }
    }
}
