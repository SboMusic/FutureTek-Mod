package com.futuretek.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * Base delle interfacce delle macchine (la parte "logica", che esiste sia sul server sia sul client).
 * Il server riempie i dati veri, il client li riceve automaticamente tramite ContainerData.
 */
public abstract class MachineMenu extends AbstractContainerMenu {
    public final BlockPos pos;
    protected final ContainerData data;
    private final ContainerLevelAccess access;
    private final Block block;
    /** Quanti slot appartengono alla macchina (sono i primi della lista) */
    private int machineSlots;

    protected MachineMenu(MenuType<?> type, int id, Inventory inventory, BlockPos pos, ContainerData data) {
        super(type, id);
        this.pos = pos;
        this.data = data;
        this.access = ContainerLevelAccess.create(inventory.player.level(), pos);
        this.block = inventory.player.level().getBlockState(pos).getBlock();
        addDataSlots(data);
    }

    /** Da chiamare DOPO aver aggiunto gli slot della macchina */
    protected void addPlayerInventory(Inventory inventory) {
        addPlayerInventory(inventory, 84);
    }

    /** Inventario del giocatore a partire dall'altezza y (la barra veloce 58 pixel sotto) */
    protected void addPlayerInventory(Inventory inventory, int y) {
        machineSlots = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, y + 58));
        }
    }

    public int getEnergy() {
        return (data.get(0) & 0xFFFF) | ((data.get(1) & 0xFFFF) << 16);
    }

    public int getCapacity() {
        return (data.get(2) & 0xFFFF) | ((data.get(3) & 0xFFFF) << 16);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, block);
    }

    /** Shift+click: sposta gli oggetti tra macchina e inventario del giocatore */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int playerStart = machineSlots;
        int hotbarStart = machineSlots + 27;
        int end = machineSlots + 36;

        if (index < machineSlots) {
            if (!moveItemStackTo(stack, playerStart, end, true)) {
                return ItemStack.EMPTY;
            }
        } else if (machineSlots > 0 && moveItemStackTo(stack, 0, machineSlots, false)) {
            // spostato nella macchina
        } else if (index < hotbarStart) {
            if (!moveItemStackTo(stack, hotbarStart, end, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, playerStart, hotbarStart, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }
}
