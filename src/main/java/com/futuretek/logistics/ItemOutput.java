package com.futuretek.logistics;

import com.futuretek.block.ConduitBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** Aiuto per le macchine: mette oggetti nei contenitori accanto o nella rete di condotti. */
public final class ItemOutput {
    private ItemOutput() {
    }

    /** @return cio' che non e' stato possibile mettere da nessuna parte */
    public static ItemStack insert(Level level, BlockPos machine, ItemStack stack, boolean simulate) {
        ItemStack rest = stack.copy();
        for (Direction dir : Direction.values()) {
            BlockPos next = machine.relative(dir);
            if (level.getBlockState(next).getBlock() instanceof ConduitBlock) {
                ItemNetwork network = ItemNetworkManager.get(level, next);
                if (network != null) {
                    rest = network.insert(rest, machine, simulate);
                }
            } else {
                IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, next, dir.getOpposite());
                if (handler != null) {
                    rest = ItemHandlerHelper.insertItemStacked(handler, rest, simulate);
                }
            }
            if (rest.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return rest;
    }
}
