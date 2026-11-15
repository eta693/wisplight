package fr.leho.wisplight.block;

import com.mojang.serialization.MapCodec;
import fr.leho.wisplight.blockentity.CallingLanternBlockEntity;
import fr.leho.wisplight.registry.ModBlockEntities;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Lanterne d'appel : la nuit, attire les follets alentour et en fait parfois apparaître. */
public class CallingLanternBlock extends BaseEntityBlock {
    public static final MapCodec<CallingLanternBlock> CODEC = simpleCodec(CallingLanternBlock::new);
    public static final BooleanProperty HANGING = BlockStateProperties.HANGING;
    private static final VoxelShape STANDING = Shapes.or(Block.box(5, 0, 5, 11, 7, 11), Block.box(6, 7, 6, 10, 9, 10));
    private static final VoxelShape HANGING_SHAPE = Shapes.or(Block.box(5, 1, 5, 11, 8, 11), Block.box(6, 8, 6, 10, 10, 10));

    public CallingLanternBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HANGING, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HANGING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        for (Direction dir : context.getNearestLookingDirections()) {
            if (dir.getAxis() == Direction.Axis.Y) {
                BlockState state = defaultBlockState().setValue(HANGING, dir == Direction.UP);
                if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
                    return state;
                }
            }
        }
        return null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction dir = state.getValue(HANGING) ? Direction.UP : Direction.DOWN;
        return Block.canSupportCenter(level, pos.relative(dir), dir.getOpposite());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        Direction support = state.getValue(HANGING) ? Direction.UP : Direction.DOWN;
        if (direction == support && !canSurvive(state, level, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HANGING) ? HANGING_SHAPE : STANDING;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CallingLanternBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, ModBlockEntities.CALLING_LANTERN.get(), CallingLanternBlockEntity::serverTick);
    }
}
