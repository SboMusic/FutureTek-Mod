package com.futuretek.menu;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Slot "fantasma": mostra un oggetto ma non lo contiene davvero.
 * Cliccando con un oggetto in mano ne memorizza una copia; cliccando a mano vuota lo svuota.
 * Usato da filtri e modelli di ricetta. La gestione del click e' in {@link GhostMenus}.
 */
public class GhostSlot extends SlotItemHandler {
    public GhostSlot(IItemHandler handler, int index, int x, int y) {
        super(handler, index, x, y);
    }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }
}
