package com.futuretek.menu;

import com.futuretek.blockentity.BatteryBoxBlockEntity;
import com.futuretek.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class BatteryBoxMenu extends MachineMenu {
    public BatteryBoxMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new ItemStackHandler(2) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return stack.getCapability(Capabilities.EnergyStorage.ITEM) != null;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        }, new SimpleContainerData(5));
    }

    public BatteryBoxMenu(int id, Inventory inventory, BlockPos pos, IItemHandler items, ContainerData data) {
        super(ModMenus.BATTERY_BOX.get(), id, inventory, pos, data);
        addSlot(new SlotItemHandler(items, BatteryBoxBlockEntity.SLOT_CHARGE, 56, 17));
        addSlot(new SlotItemHandler(items, BatteryBoxBlockEntity.SLOT_DISCHARGE, 56, 53));
        addPlayerInventory(inventory);
    }

    /** 0 = I, 1 = II, 2 = III */
    public int getTierIndex() {
        return data.get(4);
    }
}
