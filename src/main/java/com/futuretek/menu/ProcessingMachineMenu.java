package com.futuretek.menu;

import com.futuretek.blockentity.ProcessingMachineBlockEntity;
import com.futuretek.item.UpgradeItem;
import com.futuretek.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class ProcessingMachineMenu extends MachineMenu {
    public ProcessingMachineMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new ItemStackHandler(
                ProcessingMachineBlockEntity.FIRST_UPGRADE + ProcessingMachineBlockEntity.UPGRADE_SLOTS) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (slot >= ProcessingMachineBlockEntity.FIRST_UPGRADE) {
                    return stack.getItem() instanceof UpgradeItem;
                }
                return slot == ProcessingMachineBlockEntity.SLOT_INPUT;
            }

            @Override
            public int getSlotLimit(int slot) {
                return slot >= ProcessingMachineBlockEntity.FIRST_UPGRADE ? 4 : 64;
            }
        }, new SimpleContainerData(8));
    }

    public ProcessingMachineMenu(int id, Inventory inventory, BlockPos pos, IItemHandler items, ContainerData data) {
        super(ModMenus.PROCESSING_MACHINE.get(), id, inventory, pos, data);
        addSlot(new SlotItemHandler(items, ProcessingMachineBlockEntity.SLOT_INPUT, 44, 26));
        addSlot(new SlotItemHandler(items, ProcessingMachineBlockEntity.SLOT_OUTPUT, 98, 26));
        addSlot(new SlotItemHandler(items, ProcessingMachineBlockEntity.SLOT_SECONDARY, 122, 26));
        for (int i = 0; i < ProcessingMachineBlockEntity.UPGRADE_SLOTS; i++) {
            addSlot(new SlotItemHandler(items, ProcessingMachineBlockEntity.FIRST_UPGRADE + i, 44 + i * 18, 54));
        }
        addPlayerInventory(inventory);
    }

    public int getProgress() {
        return data.get(4);
    }

    public int getMaxProgress() {
        return data.get(5);
    }

    public int getWater() {
        return data.get(6) & 0xFFFF;
    }

    public boolean usesWater() {
        return data.get(7) == 1;
    }
}
