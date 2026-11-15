package fr.leho.wisplight.blockentity;

import fr.leho.wisplight.block.WispJarBlock;
import fr.leho.wisplight.entity.WispVariant;
import fr.leho.wisplight.infusion.InfusionInput;
import fr.leho.wisplight.infusion.InfusionRecipe;
import fr.leho.wisplight.menu.InfusionAltarMenu;
import fr.leho.wisplight.registry.ModBlockEntities;
import fr.leho.wisplight.registry.ModRecipes;
import fr.leho.wisplight.registry.ModSounds;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.joml.Vector3f;

/** Autel d'infusion : 4 emplacements (objet, essence, essence, résultat) et 10 s de travail. */
public class InfusionAltarBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_ITEM = 0;
    public static final int SLOT_ESSENCE_A = 1;
    public static final int SLOT_ESSENCE_B = 2;
    public static final int SLOT_RESULT = 3;
    public static final int WORK_TICKS = 200;
    public static final int JAR_RADIUS = 3;

    private final ItemStackHandler inventory = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private int progress;
    private int jarCheckCooldown;
    @Nullable
    private BlockPos jarPos;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> WORK_TICKS;
                case 2 -> jarPos != null ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                progress = value;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public InfusionAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INFUSION_ALTAR.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public int getProgress() {
        return progress;
    }

    public boolean hasJar() {
        return jarPos != null;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, InfusionAltarBlockEntity altar) {
        altar.tickServer(level);
    }

    /** Un tick de travail (public pour les GameTests). */
    public void tickServer(Level level) {
        if (jarCheckCooldown-- <= 0) {
            jarPos = findJar(level);
            jarCheckCooldown = 20;
        }
        Optional<RecipeHolder<InfusionRecipe>> recipe = currentRecipe(level);
        if (jarPos == null || recipe.isEmpty()) {
            if (progress != 0) {
                progress = 0;
                setChanged();
            }
            return;
        }
        ItemStack result = recipe.get().value().assemble(input(), level.registryAccess());
        if (!canOutput(result)) {
            return;
        }
        progress++;
        if (level instanceof ServerLevel server && progress % 4 == 0) {
            beam(server);
        }
        if (progress == 1) {
            level.playSound(null, worldPosition, ModSounds.ALTAR_WORKING.get(), SoundSource.BLOCKS, 0.7F, 1.0F);
        }
        if (progress >= WORK_TICKS) {
            craft(result);
            level.playSound(null, worldPosition, ModSounds.ALTAR_DONE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            progress = 0;
        }
        setChanged();
    }

    private InfusionInput input() {
        return new InfusionInput(inventory.getStackInSlot(SLOT_ITEM),
                inventory.getStackInSlot(SLOT_ESSENCE_A), inventory.getStackInSlot(SLOT_ESSENCE_B));
    }

    public Optional<RecipeHolder<InfusionRecipe>> currentRecipe(Level level) {
        InfusionInput input = input();
        if (input.item().isEmpty() || input.essenceA().isEmpty() || input.essenceB().isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(ModRecipes.INFUSION_TYPE.get(), input, level);
    }

    private boolean canOutput(ItemStack result) {
        if (result.isEmpty()) {
            return false;
        }
        ItemStack current = inventory.getStackInSlot(SLOT_RESULT);
        if (current.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(current, result)
                && current.getCount() + result.getCount() <= current.getMaxStackSize();
    }

    private void craft(ItemStack result) {
        inventory.extractItem(SLOT_ITEM, 1, false);
        inventory.extractItem(SLOT_ESSENCE_A, 1, false);
        inventory.extractItem(SLOT_ESSENCE_B, 1, false);
        ItemStack current = inventory.getStackInSlot(SLOT_RESULT);
        if (current.isEmpty()) {
            inventory.setStackInSlot(SLOT_RESULT, result);
        } else {
            current.grow(result.getCount());
            inventory.setStackInSlot(SLOT_RESULT, current);
        }
    }

    @Nullable
    private BlockPos findJar(Level level) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-JAR_RADIUS, -1, -JAR_RADIUS),
                worldPosition.offset(JAR_RADIUS, 2, JAR_RADIUS))) {
            if (level.getBlockState(p).getBlock() instanceof WispJarBlock) {
                double d = p.distSqr(worldPosition);
                if (d < bestDist) {
                    bestDist = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }

    /** Faisceau de particules du bocal vers l'autel. */
    private void beam(ServerLevel level) {
        if (jarPos == null || !(level.getBlockState(jarPos).getBlock() instanceof WispJarBlock jar)) {
            return;
        }
        WispVariant v = jar.variant();
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(v.red(), v.green(), v.blue()), 0.8F);
        Vec3 from = Vec3.atCenterOf(jarPos).add(0, 0.1, 0);
        Vec3 to = Vec3.atCenterOf(worldPosition).add(0, 0.45, 0);
        for (int i = 0; i <= 8; i++) {
            Vec3 p = from.lerp(to, i / 8.0);
            level.sendParticles(dust, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inventory.getStackInSlot(i));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        progress = tag.getInt("Progress");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.wisplight.infusion_altar");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new InfusionAltarMenu(containerId, playerInventory, inventory, data,
                ContainerLevelAccess.create(player.level(), worldPosition));
    }
}
