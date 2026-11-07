package com.futuretek.menu;

import com.futuretek.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Menu del filtro del condotto smistatore: 9 slot fantasma. */
public class FilterMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;

    public FilterMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new ItemStackHandler(9));
    }

    public FilterMenu(int id, Inventory inventory, BlockPos pos, IItemHandler filter) {
        this(ModMenus.FILTER.get(), id, inventory, pos, filter);
    }

    protected FilterMenu(net.minecraft.world.inventory.MenuType<?> type, int id, Inventory inventory, BlockPos pos,
            IItemHandler filter) {
        super(type, id);
        this.access = ContainerLevelAccess.create(inventory.player.level(), pos);
        for (int i = 0; i < 9; i++) {
            addSlot(new GhostSlot(filter, i, 8 + i * 18, 35));
        }
        GhostMenus.addPlayerInventory(inventory, this::addSlot, 84);
    }

    @Override
    public void clicked(int slotId, int button, ClickType type, Player player) {
        if (!GhostMenus.handleClick(this, slotId, type, player)) {
            super.clicked(slotId, button, type, player);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) -> player.distanceToSqr(pos.getCenter()) < 64, true);
    }
}
