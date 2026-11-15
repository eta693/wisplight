package fr.leho.wisplight.block;

import com.mojang.serialization.MapCodec;
import fr.leho.wisplight.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/**
 * Lueur flottante : une petite lumière (niveau 12) qui scintille. Posée par les outils infusés de Lumière
 * (persistante) ou par les orbes du bâton (temporaire).
 */
public class LingeringGlowBlock extends Block {
    public static final MapCodec<LingeringGlowBlock> CODEC = simpleCodec(LingeringGlowBlock::new);
    public static final BooleanProperty PERSISTENT = BooleanProperty.create("persistent");
    private static final VoxelShape SHAPE = Block.box(6, 6, 6, 10, 10, 10);
    private static final DustParticleOptions SPARK = new DustParticleOptions(new Vector3f(1.0F, 0.89F, 0.48F), 0.6F);

    public LingeringGlowBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PERSISTENT, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PERSISTENT);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(PERSISTENT)) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        level.addParticle(SPARK, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3,
                pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 0.3,
                pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3, 0, 0.005, 0);
    }

    /** Pose une lueur si l'emplacement est libre. Durée en ticks, ou persistante si durée ≤ 0. */
    public static boolean place(Level level, BlockPos pos, int durationTicks) {
        BlockState current = level.getBlockState(pos);
        if (!current.isAir() && !current.is(ModBlocks.TRACE_LIGHT.get())) {
            return false;
        }
        LingeringGlowBlock block = ModBlocks.LINGERING_GLOW.get();
        level.setBlock(pos, block.defaultBlockState().setValue(PERSISTENT, durationTicks <= 0), Block.UPDATE_ALL);
        if (durationTicks > 0) {
            level.scheduleTick(pos, block, durationTicks);
        }
        return true;
    }
}
