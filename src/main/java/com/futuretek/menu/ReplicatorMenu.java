package com.futuretek.menu;

import com.futuretek.blockentity.ReplicatorBlockEntity;
import com.futuretek.registry.ModBlocks;
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

public class ReplicatorMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public ReplicatorMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new ItemStackHandler(11), new SimpleContainerData(4));
    }

    public ReplicatorMenu(int id, Inventory inventory, BlockPos pos, IItemHandler items, ContainerData data) {
        super(ModMenus.REPLICATOR.get(), id);
        this.access = ContainerLevelAccess.create(inventory.player.level(), pos);
        this.data = data;
        addDataSlots(data);
        addSlot(new SlotItemHandler(items, ReplicatorBlockEntity.SLOT_TEMPLATE, 26, 35));
        for (int i = 0; i < ReplicatorBlockEntity.INPUTS; i++) {
            addSlot(new SlotItemHandler(items, ReplicatorBlockEntity.FIRST_INPUT + i, 62 + (i % 3) * 18, 17 + (i / 3) * 18));
        }
        addSlot(new SlotItemHandler(items, ReplicatorBlockEntity.SLOT_OUTPUT, 134, 35));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    public long getBuffer() {
        return (data.get(0) & 0xFFFFL) | ((data.get(1) & 0xFFFFL) << 16);
    }

    public long getTemplateValue() {
        return (data.get(2) & 0xFFFFL) | ((data.get(3) & 0xFFFFL) << 16);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int machineSlots = 11;
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, machineSlots + 36, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 1, 10, false)) {
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
        return stillValid(access, player, ModBlocks.REPLICATOR.get());
    }
}
