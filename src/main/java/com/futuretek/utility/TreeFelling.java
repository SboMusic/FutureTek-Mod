package com.futuretek.utility;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.futuretek.FutureTekConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Abbattimento degli alberi: rompendo un tronco con un'ascia (qualsiasi ascia, anche di Minecraft)
 * cade tutto l'albero. Tieni premuto Shift per rompere un tronco solo.
 * Per non distruggere le case di legno, funziona solo se l'albero ha foglie "naturali" (non piazzate a mano).
 * Ogni tronco consuma 1 di durata dell'ascia; ci si ferma prima che l'ascia si rompa.
 */
public final class TreeFelling {
    private TreeFelling() {
    }

    private static final ThreadLocal<Boolean> BUSY = ThreadLocal.withInitial(() -> false);

    public static void onBreak(BlockEvent.BreakEvent event) {
        if (BUSY.get() || !FutureTekConfig.TREE_FELLING.getAsBoolean()
                || !(event.getPlayer() instanceof ServerPlayer player) || player.isShiftKeyDown()) {
            return;
        }
        ItemStack tool = player.getMainHandItem();
        if (!tool.canPerformAction(ItemAbilities.AXE_DIG) || !event.getState().is(BlockTags.LOGS)) {
            return;
        }
        List<BlockPos> logs = findTree(event.getLevel(), event.getPos(), FutureTekConfig.TREE_FELLING_MAX_LOGS.getAsInt());
        if (logs.isEmpty()) {
            return;
        }
        BUSY.set(true);
        try {
            for (BlockPos pos : logs) {
                if (pos.equals(event.getPos())) {
                    continue;
                }
                ItemStack held = player.getMainHandItem();
                if (held.isEmpty() || (held.isDamageableItem() && held.getMaxDamage() - held.getDamageValue() <= 2)) {
                    break; // non rompiamo l'ascia del giocatore
                }
                player.gameMode.destroyBlock(pos);
            }
        } finally {
            BUSY.set(false);
        }
    }

    /**
     * I tronchi dell'albero che parte da "start" (collegati anche in diagonale, solo verso l'alto o di lato),
     * oppure una lista vuota se non sembra un albero (nessuna foglia naturale vicina).
     */
    public static List<BlockPos> findTree(LevelAccessor level, BlockPos start, int max) {
        List<BlockPos> logs = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        boolean leaves = false;
        while (!queue.isEmpty() && logs.size() < max) {
            BlockPos pos = queue.poll();
            logs.add(pos);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 0; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos next = pos.offset(dx, dy, dz);
                        if (!seen.add(next)) {
                            continue;
                        }
                        BlockState state = level.getBlockState(next);
                        if (state.is(BlockTags.LOGS) && next.getY() >= start.getY()) {
                            queue.add(next);
                        } else if (state.getBlock() instanceof LeavesBlock && !state.getValue(LeavesBlock.PERSISTENT)) {
                            leaves = true;
                        }
                    }
                }
            }
        }
        return leaves ? logs : List.of();
    }
}
