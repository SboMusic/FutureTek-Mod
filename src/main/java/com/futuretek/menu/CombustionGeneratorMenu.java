package com.futuretek.menu;

import com.futuretek.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class CombustionGeneratorMenu extends MachineMenu {
    /** Costruttore lato CLIENT: i dati veri arriveranno dal server */
    public CombustionGeneratorMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new ItemStackHandler(1) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return stack.getBurnTime(RecipeType.SMELTING) > 0;
            }
        }, new SimpleContainerData(6));
    }

    /** Costruttore lato SERVER */
    public CombustionGeneratorMenu(int id, Inventory inventory, BlockPos pos, IItemHandler items, ContainerData data) {
        super(ModMenus.COMBUSTION_GENERATOR.get(), id, inventory, pos, data);
        addSlot(new SlotItemHandler(items, 0, 80, 53));
        addPlayerInventory(inventory);
    }

    public int getBurnTime() {
        return data.get(4);
    }

    public int getBurnTimeTotal() {
        return data.get(5);
    }
}
