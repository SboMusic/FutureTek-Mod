package com.futuretek.bee;

import com.futuretek.menu.GhostMenus;
import com.futuretek.registry.ModBlocks;
import com.futuretek.registry.ModItems;
import com.futuretek.registry.ModMenus;

import net.minecraft.core.BlockPos;
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

public class ApiaryMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public ApiaryMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new ItemStackHandler(11) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot == 0 ? stack.is(ModItems.BEE_QUEEN.get()) : slot == 1 && stack.is(ModItems.BEE_DRONE.get());
            }
        }, new SimpleContainerData(1));
    }

    public ApiaryMenu(int id, Inventory inventory, BlockPos pos, IItemHandler items, ContainerData data) {
        super(ModMenus.APIARY.get(), id);
        this.access = ContainerLevelAccess.create(inventory.player.level(), pos);
        this.data = data;
        addDataSlots(data);
        addSlot(new SlotItemHandler(items, ApiaryBlockEntity.SLOT_QUEEN, 26, 22));
        addSlot(new SlotItemHandler(items, ApiaryBlockEntity.SLOT_DRONE, 26, 48));
        for (int i = 0; i < 9; i++) {
            addSlot(new SlotItemHandler(items, ApiaryBlockEntity.FIRST_OUTPUT + i, 98 + (i % 3) * 18, 17 + (i / 3) * 18));
        }
        GhostMenus.addPlayerInventory(inventory, this::addSlot, 84);
    }

    public int getProgress() {
        return data.get(0);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < 11) {
            if (!moveItemStackTo(stack, 11, 47, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, 2, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.APIARY.get());
    }
}
