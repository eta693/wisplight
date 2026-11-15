package fr.leho.wisplight.blockentity;

import fr.leho.wisplight.WisplightConfig;
import fr.leho.wisplight.entity.WispEntity;
import fr.leho.wisplight.entity.WispVariant;
import fr.leho.wisplight.registry.ModBlockEntities;
import fr.leho.wisplight.registry.ModEntities;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class CallingLanternBlockEntity extends BlockEntity {
    public static final int ATTRACT_RADIUS = 32;
    private static final int ATTRACT_INTERVAL = 40;
    private static final int SPAWN_INTERVAL = 600;

    public CallingLanternBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CALLING_LANTERN.get(), pos, state);
    }

    /** La lanterne n'appelle que la nuit (ou toujours dans le Nether). */
    public static boolean isActive(Level level) {
        return !level.isDay();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CallingLanternBlockEntity lantern) {
        if (!(level instanceof ServerLevel server) || !isActive(level)) {
            return;
        }
        long time = level.getGameTime() + Math.floorMod(pos.asLong(), 20L);
        if (time % ATTRACT_INTERVAL == 0) {
            lantern.attract(server);
        }
        if (time % SPAWN_INTERVAL == 0 && level.random.nextInt(3) == 0) {
            lantern.trySpawn(server);
        }
    }

    public int attract(ServerLevel level) {
        List<WispEntity> wisps = level.getEntitiesOfClass(WispEntity.class, new AABB(worldPosition).inflate(ATTRACT_RADIUS));
        for (WispEntity wisp : wisps) {
            wisp.attractTo(worldPosition, ATTRACT_INTERVAL + 10);
        }
        return wisps.size();
    }

    public boolean trySpawn(ServerLevel level) {
        int near = level.getEntitiesOfClass(WispEntity.class, new AABB(worldPosition).inflate(16)).size();
        if (near >= 2 || near >= WisplightConfig.maxWispsNearby()) {
            return false;
        }
        BlockPos spot = worldPosition.offset(level.random.nextInt(9) - 4, 1 + level.random.nextInt(3), level.random.nextInt(9) - 4);
        if (!level.getBlockState(spot).isAir()) {
            return false;
        }
        WispEntity wisp = ModEntities.WISP.get().create(level);
        if (wisp == null) {
            return false;
        }
        wisp.moveTo(spot.getX() + 0.5, spot.getY() + 0.2, spot.getZ() + 0.5, level.random.nextFloat() * 360F, 0F);
        wisp.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), MobSpawnType.TRIGGERED, null);
        wisp.setVariant(WispVariant.forBiome(level.getBiome(spot)));
        level.addFreshEntity(wisp);
        return true;
    }
}
