package fr.leho.wisplight.block;

import com.mojang.serialization.MapCodec;
import fr.leho.wisplight.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Bloc de lumière invisible qui suit les follets. Il vieillit toutes les 5 ticks
 * et disparaît seul après ~20 ticks si aucun follet ne le rafraîchit.
 */
public class TraceLightBlock extends Block {
    public static final MapCodec<TraceLightBlock> CODEC = simpleCodec(TraceLightBlock::new);
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    public static final int TICK_DELAY = 5;

    public TraceLightBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (age >= 3) {
            level.removeBlock(pos, false);
        } else {
            level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
            level.scheduleTick(pos, this, TICK_DELAY);
        }
    }

    /** Pose ou rafraîchit une trace lumineuse à cette position si l'espace est libre. */
    public static void refresh(Level level, BlockPos pos) {
        BlockState current = level.getBlockState(pos);
        TraceLightBlock block = ModBlocks.TRACE_LIGHT.get();
        if (current.is(block)) {
            if (current.getValue(AGE) != 0) {
                level.setBlock(pos, current.setValue(AGE, 0), Block.UPDATE_CLIENTS);
            }
        } else if (current.isAir()) {
            level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
            level.scheduleTick(pos, block, TICK_DELAY);
        }
    }
}
