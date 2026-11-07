package com.futuretek.logistics;

import com.futuretek.blockentity.MachineBlockEntity;
import com.futuretek.energy.EnergyTier;
import com.futuretek.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Nucleo logistico: il "cervello" della rete di tubi. Funziona a energia (2 TU/t + 2 TU per ogni oggetto spostato).
 * - Il baule che tocca il nucleo diventa il BAULE PRINCIPALE: tutto cio' che entra nella rete e non ha
 *   una destinazione finisce li'.
 * - Ogni mezzo secondo il nucleo guarda nel baule principale e manda alle destinazioni gli oggetti che hanno
 *   un posto: i tubi di destinazione con quel filtro, oppure i bauli che contengono gia' quell'oggetto.
 * Senza energia il nucleo si spegne e la rete torna a funzionare come una rete normale.
 */
public class LogisticsCoreBlockEntity extends MachineBlockEntity implements ItemNetworkManager.NetworkDevice {
    public static final int IDLE_COST = 20;    // ogni 10 tick (= 2 TU/t)
    public static final int ITEM_COST = 2;
    public static final int ITEMS_PER_CYCLE = 64;

    private boolean powered;

    public LogisticsCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LOGISTICS_CORE.get(), pos, state, 20_000, EnergyTier.MEDIUM.rate, 0);
    }

    public boolean isPowered() {
        return powered;
    }

    @Override
    public void serverTick() {
        if (level.getGameTime() % 10 != 0) {
            return;
        }
        boolean nowPowered = energy.getEnergyStored() >= IDLE_COST;
        if (nowPowered) {
            energy.consume(IDLE_COST);
        }
        if (nowPowered != powered) {
            powered = nowPowered;
            setLit(powered);
            setChanged();
        }
        if (!powered) {
            return;
        }
        ItemNetwork network = ItemNetworkManager.getAdjacent(level, worldPosition);
        if (network == null) {
            return;
        }
        ItemNetwork.Endpoint main = network.mainChest();
        IItemHandler chest = main == null ? null : network.handlerOf(main);
        if (chest == null) {
            return;
        }
        int budget = ITEMS_PER_CYCLE;
        for (int slot = 0; slot < chest.getSlots() && budget > 0; slot++) {
            ItemStack inSlot = chest.getStackInSlot(slot);
            if (inSlot.isEmpty()) {
                continue;
            }
            int affordable = energy.getEnergyStored() / ITEM_COST;
            ItemStack available = chest.extractItem(slot, Math.min(budget, affordable), true);
            if (available.isEmpty()) {
                continue;
            }
            ItemStack rest = network.insertToDestinations(available, main.pos(), true);
            int movable = available.getCount() - rest.getCount();
            if (movable <= 0) {
                continue;
            }
            ItemStack taken = chest.extractItem(slot, movable, false);
            ItemStack leftover = network.insertToDestinations(taken, main.pos(), false);
            if (!leftover.isEmpty()) {
                chest.insertItem(slot, leftover, false);
            }
            int moved = taken.getCount() - leftover.getCount();
            energy.consume(moved * ITEM_COST);
            budget -= moved;
        }
    }
}
