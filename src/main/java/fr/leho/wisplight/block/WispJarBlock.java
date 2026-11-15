package fr.leho.wisplight.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fr.leho.wisplight.blockentity.WispJarBlockEntity;
import fr.leho.wisplight.entity.WispEntity;
import fr.leho.wisplight.entity.WispVariant;
import fr.leho.wisplight.registry.ModBlockEntities;
import fr.leho.wisplight.registry.ModEntities;
import fr.leho.wisplight.registry.ModItems;
import fr.leho.wisplight.registry.ModSounds;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/** Bocal contenant un follet vivant : produit des essences, se récolte avec une fiole. */
public class WispJarBlock extends BaseEntityBlock {
    public static final MapCodec<WispJarBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            WispVariant.CODEC.fieldOf("variant").forGetter(WispJarBlock::variant),
            propertiesCodec()
    ).apply(instance, WispJarBlock::new));

    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 14, 13);

    private final WispVariant variant;

    public WispJarBlock(WispVariant variant, Properties properties) {
        super(properties);
        this.variant = variant;
    }

    public WispVariant variant() {
        return variant;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WispJarBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.WISP_JAR.get(), WispJarBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.GLASS_BOTTLE)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(level.getBlockEntity(pos) instanceof WispJarBlockEntity jar) || jar.getEssences() <= 0) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            jar.takeEssence();
            ItemStack essence = new ItemStack(ModItems.ESSENCES.get(variant).get());
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            if (!player.getInventory().add(essence)) {
                player.drop(essence, false);
            }
            if (level.random.nextInt(4) == 0) {
                ItemStack dust = new ItemStack(ModItems.WISP_DUST.get());
                if (!player.getInventory().add(dust)) {
                    player.drop(dust, false);
                }
            }
            level.playSound(null, pos, ModSounds.JAR_HARVEST.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            release(server, pos, variant);
            level.removeBlock(pos, false);
            ItemStack empty = new ItemStack(ModItems.EMPTY_JAR.get());
            if (!player.getInventory().add(empty)) {
                player.drop(empty, false);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Relâche un follet de cette variante au-dessus de la position donnée. */
    public static void release(ServerLevel level, BlockPos pos, WispVariant variant) {
        WispEntity wisp = ModEntities.WISP.get().create(level);
        if (wisp == null) {
            return;
        }
        wisp.setVariant(variant);
        wisp.moveTo(pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, level.random.nextFloat() * 360F, 0F);
        wisp.setPersistenceRequired();
        level.addFreshEntity(wisp);
        level.playSound(null, pos, ModSounds.WISP_RELEASE.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.sendParticles(new DustParticleOptions(new Vector3f(variant.red(), variant.green(), variant.blue()), 1.0F),
                pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.02);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof WispJarBlockEntity jar) {
            return jar.getEssences() * 15 / WispJarBlockEntity.MAX_ESSENCES;
        }
        return 0;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(new DustParticleOptions(new Vector3f(variant.red(), variant.green(), variant.blue()), 0.5F),
                    pos.getX() + 0.3 + random.nextDouble() * 0.4,
                    pos.getY() + 0.2 + random.nextDouble() * 0.5,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4,
                    0.0, 0.01, 0.0);
        }
    }
}
