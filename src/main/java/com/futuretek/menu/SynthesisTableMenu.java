package com.futuretek.menu;

import java.util.List;

import com.futuretek.essence.EssenceManager;
import com.futuretek.essence.EssenceValues;
import com.futuretek.registry.ModBlocks;
import com.futuretek.registry.ModMenus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Tavolo di Sintesi (anche nella versione portatile, la Tavoletta di Sintesi).
 * - Slot "Sciogli": l'oggetto messo qui sparisce, il giocatore guadagna la sua Essenza e lo impara.
 * - Griglia: gli oggetti conosciuti, con ricerca e pagine (gestite dal client).
 *   Click = crea 1 copia, Shift+click = crea una pila: il client manda un SynthesisRequestPayload.
 */
public class SynthesisTableMenu extends AbstractContainerMenu {
    public static final int INVENTORY_Y = 122;
    public static final int DISSOLVE_X = 152;
    public static final int DISSOLVE_Y = 30;

    private final ContainerLevelAccess access;
    /** true = aperto dalla Tavoletta portatile (non serve stare vicino a un tavolo) */
    private final boolean portable;
    private final Player player;
    private final SimpleContainer dissolveSlot = new SimpleContainer(1);

    public SynthesisTableMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, ContainerLevelAccess.NULL, true);
    }

    public SynthesisTableMenu(int id, Inventory inventory, ContainerLevelAccess access, boolean portable) {
        super(ModMenus.SYNTHESIS_TABLE.get(), id);
        this.access = access;
        this.portable = portable;
        this.player = inventory.player;

        addSlot(new Slot(dissolveSlot, 0, DISSOLVE_X, DISSOLVE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return EssenceValues.stackValue(player.level().isClientSide, stack) > 0;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                // sul server: scioglie subito l'oggetto
                if (!player.level().isClientSide && hasItem()) {
                    if (EssenceManager.dissolve(player, getItem()) > 0) {
                        set(ItemStack.EMPTY);
                    }
                }
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, INVENTORY_Y + 58));
        }
    }

    public List<Item> knownItems() {
        return EssenceManager.sortedKnown(EssenceManager.get(player), player.level().isClientSide);
    }

    /** Shift+click su un oggetto dell'inventario: lo scioglie tutto */
    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        if (index == 0 || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        if (slots.get(0).mayPlace(stack) && !slots.get(0).hasItem()) {
            ItemStack moved = stack.copy();
            slot.set(ItemStack.EMPTY);
            slots.get(0).set(moved);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player p) {
        return portable || stillValid(access, p, ModBlocks.SYNTHESIS_TABLE.get());
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        clearContainer(p, dissolveSlot);
    }
}
