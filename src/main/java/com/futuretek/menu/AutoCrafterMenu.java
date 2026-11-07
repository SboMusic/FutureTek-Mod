package com.futuretek.menu;

import com.futuretek.registry.ModBlocks;
import com.futuretek.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Menu del banco di assemblaggio automatico: griglia 3x3 fantasma (la ricetta da eseguire). */
public class AutoCrafterMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final IItemHandler pattern;

    public AutoCrafterMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new ItemStackHandler(9), new SimpleContainerData(2), null);
    }

    /** Pulsanti: 0 = fabbrica 1, 1 = fabbrica 16, 2 = scorta -8, 3 = scorta +8, 4 = scorta spenta */
    public static final int BUTTON_CRAFT_1 = 0;
    public static final int BUTTON_CRAFT_16 = 1;
    public static final int BUTTON_STOCK_DOWN = 2;
    public static final int BUTTON_STOCK_UP = 3;
    public static final int BUTTON_STOCK_OFF = 4;

    private final com.futuretek.blockentity.AutoCrafterBlockEntity crafter;

    public AutoCrafterMenu(int id, Inventory inventory, BlockPos pos, IItemHandler pattern, ContainerData data,
            com.futuretek.blockentity.AutoCrafterBlockEntity crafter) {
        super(ModMenus.AUTO_CRAFTER.get(), id);
        this.access = ContainerLevelAccess.create(inventory.player.level(), pos);
        this.data = data;
        this.pattern = pattern;
        this.crafter = crafter;
        addDataSlots(data);
        for (int i = 0; i < 9; i++) {
            addSlot(new GhostSlot(pattern, i, 30 + (i % 3) * 18, 17 + (i / 3) * 18));
        }
        GhostMenus.addPlayerInventory(inventory, this::addSlot, 84);
    }

    public IItemHandler getPattern() {
        return pattern;
    }

    public int getQueued() {
        return data.get(0);
    }

    public int getStock() {
        return data.get(1);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (crafter == null) {
            return false;
        }
        int step = com.futuretek.blockentity.AutoCrafterBlockEntity.STOCK_STEP;
        switch (id) {
            case BUTTON_CRAFT_1 -> crafter.enqueue(1);
            case BUTTON_CRAFT_16 -> crafter.enqueue(16);
            case BUTTON_STOCK_DOWN -> crafter.setStock(crafter.getStock() - step);
            case BUTTON_STOCK_UP -> crafter.setStock(crafter.getStock() + step);
            case BUTTON_STOCK_OFF -> crafter.setStock(0);
            default -> {
                return false;
            }
        }
        return true;
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
        return stillValid(access, player, ModBlocks.AUTO_CRAFTER.get());
    }
}
