package com.futuretek.alchemy;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.futuretek.essence.EssenceManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Rompe tanti blocchi insieme (Catalizzatore del Vuoto, Lente Cinetica, Carica Stellare)
 * e raccoglie TUTTO quello che lasciano: niente va perso come con il TNT.
 * Non tocca: aria, liquidi, blocchi indistruttibili (roccia madre) e blocchi con un contenuto (bauli, macchine...).
 */
public final class AreaBreaker {
    private AreaBreaker() {
    }

    /** Il "quadrato" di lato 2r+1 perpendicolare alla direzione, profondo depth blocchi */
    public static List<BlockPos> box(BlockPos start, Direction into, int radius, int depth) {
        List<BlockPos> list = new ArrayList<>();
        for (int d = 0; d < depth; d++) {
            BlockPos center = start.relative(into, d);
            for (int a = -radius; a <= radius; a++) {
                for (int b = -radius; b <= radius; b++) {
                    list.add(switch (into.getAxis()) {
                        case X -> center.offset(0, a, b);
                        case Y -> center.offset(a, 0, b);
                        case Z -> center.offset(a, b, 0);
                    });
                }
            }
        }
        return list;
    }

    /** Sfera di raggio r */
    public static List<BlockPos> sphere(BlockPos center, int radius) {
        List<BlockPos> list = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (pos.distSqr(center) <= radius * radius + 0.5) {
                list.add(pos.immutable());
            }
        }
        return list;
    }

    public static boolean canBreak(ServerLevel level, BlockPos pos, BlockState state) {
        return !state.isAir() && !(state.getBlock() instanceof LiquidBlock) && state.getDestroySpeed(level, pos) >= 0
                && !state.hasBlockEntity() && level.isInWorldBounds(pos);
    }

    /**
     * Rompe i blocchi.
     * @param player chi usa l'oggetto (paga l'Essenza e riceve gli oggetti); null = nessuno (gli oggetti cadono in dropAt)
     * @param costPerBlock Essenza per ogni blocco rotto
     * @return quanti blocchi sono stati rotti
     */
    public static int breakBlocks(ServerLevel level, List<BlockPos> positions, @Nullable Player player, BlockPos dropAt,
            long costPerBlock) {
        List<ItemStack> drops = new ArrayList<>();
        int broken = 0;
        for (BlockPos pos : positions) {
            BlockState state = level.getBlockState(pos);
            if (!canBreak(level, pos, state)) {
                continue;
            }
            if (player != null) {
                if (!level.mayInteract(player, pos)
                        || NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled()) {
                    continue;
                }
                if (!EssenceManager.consume(player, costPerBlock)) {
                    EssenceManager.consumeOrWarn(player, costPerBlock); // mostra il messaggio
                    break;
                }
            }
            for (ItemStack drop : Block.getDrops(state, level, pos, null, player, ItemStack.EMPTY)) {
                merge(drops, drop);
            }
            if (broken < 16) {
                level.levelEvent(2001, pos, Block.getId(state)); // particelle di rottura (solo le prime, per non esagerare)
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            broken++;
        }
        for (ItemStack stack : drops) {
            if (player == null || !player.getInventory().add(stack)) {
                Block.popResource(level, player != null ? player.blockPosition() : dropAt, stack);
            }
        }
        return broken;
    }

    private static void merge(List<ItemStack> list, ItemStack stack) {
        for (ItemStack existing : list) {
            if (ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < existing.getMaxStackSize()) {
                int moved = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
                existing.grow(moved);
                stack.shrink(moved);
                if (stack.isEmpty()) {
                    return;
                }
            }
        }
        list.add(stack.copy());
    }
}
