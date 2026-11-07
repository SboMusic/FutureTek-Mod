package com.futuretek.logistics;

import javax.annotation.Nullable;

import com.futuretek.menu.FilterMenu;
import com.futuretek.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Menu del tubo analizzatore: filtro extra + "Lascia 1" + "Dimentica" */
public class AnalyzerMenu extends FilterMenu {
    private final ContainerData data;
    @Nullable private final AnalyzerConduitBlockEntity analyzer;

    public AnalyzerMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new ItemStackHandler(9), new SimpleContainerData(2), null);
    }

    public AnalyzerMenu(int id, Inventory inventory, BlockPos pos, IItemHandler filter, ContainerData data,
            @Nullable AnalyzerConduitBlockEntity analyzer) {
        super(ModMenus.ANALYZER.get(), id, inventory, pos, filter);
        this.data = data;
        this.analyzer = analyzer;
        addDataSlots(data);
    }

    public boolean keepsOne() {
        return data.get(0) == 1;
    }

    public int memorySize() {
        return data.get(1);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (analyzer != null) {
            analyzer.buttonPressed(id);
            return true;
        }
        return false;
    }
}
