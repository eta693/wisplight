package fr.leho.wisplight.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fr.leho.wisplight.entity.WispVariant;
import fr.leho.wisplight.registry.ModItems;
import fr.leho.wisplight.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Pierre runique gravée d'une essence. Une essence du bon type la charge (elle luit faiblement) ;
 * pendant le rituel, elle s'illumine pleinement.
 */
public class RunestoneBlock extends Block {
    public static final MapCodec<RunestoneBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            WispVariant.CODEC.fieldOf("variant").forGetter(RunestoneBlock::variant),
            propertiesCodec()
    ).apply(instance, RunestoneBlock::new));
    public static final BooleanProperty CHARGED = BooleanProperty.create("charged");
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    private final WispVariant variant;

    public RunestoneBlock(WispVariant variant, Properties properties) {
        super(properties);
        this.variant = variant;
        registerDefaultState(stateDefinition.any().setValue(CHARGED, false).setValue(LIT, false));
    }

    public WispVariant variant() {
        return variant;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGED, LIT);
    }

    public static int lightLevel(BlockState state) {
        return state.getValue(LIT) ? 13 : state.getValue(CHARGED) ? 7 : 0;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(CHARGED) || !stack.is(ModItems.ESSENCES.get(variant).get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            level.setBlockAndUpdate(pos, state.setValue(CHARGED, true));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.playSound(null, pos, ModSounds.RUNE_CHARGE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!state.getValue(CHARGED) || state.getValue(LIT) || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.setBlockAndUpdate(pos, state.setValue(CHARGED, false));
            ItemStack essence = new ItemStack(ModItems.ESSENCES.get(variant).get());
            if (!player.getInventory().add(essence)) {
                player.drop(essence, false);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
