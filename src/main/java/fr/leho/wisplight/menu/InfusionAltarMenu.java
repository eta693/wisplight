package fr.leho.wisplight.menu;

import fr.leho.wisplight.blockentity.InfusionAltarBlockEntity;
import fr.leho.wisplight.registry.ModBlocks;
import fr.leho.wisplight.registry.ModMenus;
import fr.leho.wisplight.registry.ModTags;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class InfusionAltarMenu extends AbstractContainerMenu {
    public static final int ITEM_X = 27, ITEM_Y = 35;
    public static final int ESSENCE_X = 63, ESSENCE_A_Y = 17, ESSENCE_B_Y = 53;
    public static final int RESULT_X = 134, RESULT_Y = 35;

    private static final int MACHINE_SLOTS = 4;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** Constructeur côté client. */
    public InfusionAltarMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, new ItemStackHandler(4), new SimpleContainerData(3), ContainerLevelAccess.NULL);
    }

    public InfusionAltarMenu(int containerId, Inventory playerInventory, IItemHandler handler,
                             ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.INFUSION_ALTAR.get(), containerId);
        this.data = data;
        this.access = access;
        addSlot(new SlotItemHandler(handler, InfusionAltarBlockEntity.SLOT_ITEM, ITEM_X, ITEM_Y));
        addSlot(new SlotItemHandler(handler, InfusionAltarBlockEntity.SLOT_ESSENCE_A, ESSENCE_X, ESSENCE_A_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModTags.ESSENCES);
            }
        });
        addSlot(new SlotItemHandler(handler, InfusionAltarBlockEntity.SLOT_ESSENCE_B, ESSENCE_X, ESSENCE_B_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModTags.ESSENCES);
            }
        });
        addSlot(new SlotItemHandler(handler, InfusionAltarBlockEntity.SLOT_RESULT, RESULT_X, RESULT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
        addDataSlots(data);
    }

    public float progress() {
        int max = data.get(1);
        return max <= 0 ? 0F : (float) data.get(0) / max;
    }

    public boolean hasJar() {
        return data.get(2) != 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int playerStart = MACHINE_SLOTS;
        int playerEnd = slots.size();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(ModTags.ESSENCES)) {
            if (!moveItemStackTo(stack, 1, 3, false) && !moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.INFUSION_ALTAR.get());
    }
}
