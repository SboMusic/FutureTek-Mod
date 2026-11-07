package com.futuretek.menu;

import com.futuretek.blockentity.GeothermalGeneratorBlockEntity;
import com.futuretek.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

public class GeothermalGeneratorMenu extends MachineMenu {
    public GeothermalGeneratorMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new SimpleContainerData(5));
    }

    public GeothermalGeneratorMenu(int id, Inventory inventory, BlockPos pos, ContainerData data) {
        super(ModMenus.GEOTHERMAL_GENERATOR.get(), id, inventory, pos, data);
        addPlayerInventory(inventory);
    }

    public int getLava() {
        return data.get(4) & 0xFFFF;
    }

    public int getTankCapacity() {
        return GeothermalGeneratorBlockEntity.TANK_CAPACITY;
    }
}
