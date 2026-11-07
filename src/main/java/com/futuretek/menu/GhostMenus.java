package com.futuretek.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/** Funzioni comuni ai menu con slot fantasma. */
public final class GhostMenus {
    private GhostMenus() {
    }

    /**
     * Gestisce il click su uno slot fantasma.
     * @return true se il click e' stato gestito (il menu non deve fare altro)
     */
    public static boolean handleClick(AbstractContainerMenu menu, int slotId, ClickType type, Player player) {
        if (slotId < 0 || slotId >= menu.slots.size()) {
            return false;
        }
        Slot slot = menu.slots.get(slotId);
        if (!(slot instanceof GhostSlot ghost) || !(ghost.getItemHandler() instanceof IItemHandlerModifiable handler)) {
            return false;
        }
        if (type == ClickType.PICKUP || type == ClickType.QUICK_MOVE) {
            ItemStack carried = menu.getCarried();
            handler.setStackInSlot(ghost.getSlotIndex(), carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
            menu.broadcastChanges();
        }
        return true;
    }

    public static void addPlayerInventory(Inventory inventory, java.util.function.Consumer<Slot> add, int top) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                add.accept(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, top + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            add.accept(new Slot(inventory, col, 8 + col * 18, top + 58));
        }
    }
}
