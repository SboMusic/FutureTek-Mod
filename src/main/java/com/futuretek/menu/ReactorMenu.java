package com.futuretek.menu;

import com.futuretek.blockentity.ReactorBlockEntity;
import com.futuretek.item.ReactorComponentItem;
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

public class ReactorMenu extends MachineMenu {
    public static final int GRID_Y = 18;
    public static final int INVENTORY_Y = 140;

    public ReactorMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new ItemStackHandler(ReactorBlockEntity.COLS * ReactorBlockEntity.ROWS) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return stack.getItem() instanceof ReactorComponentItem;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        }, new SimpleContainerData(11));
    }

    public ReactorMenu(int id, Inventory inventory, BlockPos pos, IItemHandler grid, ContainerData data) {
        super(ModMenus.REACTOR.get(), id, inventory, pos, data);
        for (int i = 0; i < ReactorBlockEntity.COLS * ReactorBlockEntity.ROWS; i++) {
            int col = i % ReactorBlockEntity.COLS;
            addSlot(new SlotItemHandler(grid, i, 8 + col * 18, GRID_Y + (i / ReactorBlockEntity.COLS) * 18) {
                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public boolean isActive() {
                    return col < getColumns(); // le colonne in piu' arrivano con le camere del reattore
                }
            });
        }
        addPlayerInventory(inventory, INVENTORY_Y);
    }

    public int getHeat() {
        return (data.get(4) & 0xFFFF) | ((data.get(5) & 0xFFFF) << 16);
    }

    public int getMaxHeat() {
        return (data.get(6) & 0xFFFF) | ((data.get(7) & 0xFFFF) << 16);
    }

    public int getOutput() {
        return data.get(8);
    }

    public int getColumns() {
        int c = data.get(9);
        return c <= 0 ? ReactorBlockEntity.BASE_COLS : c;
    }

    public boolean isRunning() {
        return data.get(10) == 1;
    }
}
