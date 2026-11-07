package com.futuretek.blockentity;

import com.futuretek.block.ConduitBlock;
import com.futuretek.logistics.ItemNetwork;
import com.futuretek.logistics.ItemNetworkManager;
import com.futuretek.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Condotto estrattore: a intervalli regolari prende oggetti dai contenitori accanto
 * e li manda nella rete (che li smista). Normale: 8 oggetti/secondo. Veloce: 32 ogni mezzo secondo.
 * Si ferma se riceve un segnale redstone.
 */
public class ExtractorConduitBlockEntity extends BlockEntity {
    private int cursor;

    public ExtractorConduitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXTRACTOR_CONDUIT.get(), pos, state);
    }

    private boolean fast() {
        return getBlockState().getBlock() instanceof ConduitBlock c && c.getKind() == ConduitBlock.Kind.FAST_EXTRACTOR;
    }

    public void serverTick() {
        int interval = fast() ? 10 : 20;
        if (level.getGameTime() % interval != 0 || level.hasNeighborSignal(worldPosition)) {
            return;
        }
        ItemNetwork network = ItemNetworkManager.get(level, worldPosition);
        if (network == null) {
            return;
        }
        int budget = fast() ? 32 : 8;
        for (Direction dir : Direction.values()) {
            BlockPos source = worldPosition.relative(dir);
            if (level.getBlockState(source).getBlock() instanceof ConduitBlock) {
                continue;
            }
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, source, dir.getOpposite());
            if (handler == null || handler.getSlots() == 0) {
                continue;
            }
            for (int n = 0; n < handler.getSlots() && budget > 0; n++) {
                int slot = (cursor + n) % handler.getSlots();
                ItemStack available = handler.extractItem(slot, budget, true);
                if (available.isEmpty()) {
                    continue;
                }
                ItemStack rest = network.insert(available, source, true);
                int movable = available.getCount() - rest.getCount();
                if (movable <= 0) {
                    continue;
                }
                ItemStack taken = handler.extractItem(slot, movable, false);
                ItemStack leftover = network.insert(taken, source, false);
                if (!leftover.isEmpty()) {
                    handler.insertItem(slot, leftover, false); // rimette cio' che non e' entrato
                }
                budget -= taken.getCount() - leftover.getCount();
                cursor = slot + 1;
            }
        }
    }
}
