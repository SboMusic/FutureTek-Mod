package com.futuretek.logistics;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import com.futuretek.block.ConduitBlock;
import com.futuretek.blockentity.SortingConduitBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * Tiene in memoria le reti di condotti di ogni mondo (come EnergyNetworkManager per i cavi).
 * Le reti si ricostruiscono quando un condotto o un blocco vicino cambia.
 */
public final class ItemNetworkManager {
    private static final int MAX_CONDUITS = 8192;
    private static final Map<LevelAccessor, Map<BlockPos, ItemNetwork>> NETWORKS = new WeakHashMap<>();

    private ItemNetworkManager() {
    }

    /** La rete di cui fa parte il condotto in "pos" */
    @Nullable
    public static ItemNetwork get(Level level, BlockPos pos) {
        if (level.isClientSide || !(level.getBlockState(pos).getBlock() instanceof ConduitBlock)) {
            return null;
        }
        Map<BlockPos, ItemNetwork> map = NETWORKS.computeIfAbsent(level, l -> new HashMap<>());
        ItemNetwork network = map.get(pos);
        if (network == null) {
            network = build(level, pos);
            for (BlockPos conduit : network.getConduits()) {
                map.put(conduit, network);
            }
        }
        return network;
    }

    /** La rete collegata a un dispositivo (terminale, banco di assemblaggio): cerca un condotto accanto */
    @Nullable
    public static ItemNetwork getAdjacent(Level level, BlockPos devicePos) {
        for (Direction dir : Direction.values()) {
            ItemNetwork network = get(level, devicePos.relative(dir));
            if (network != null) {
                return network;
            }
        }
        return null;
    }

    public static void invalidate(LevelAccessor level) {
        Map<BlockPos, ItemNetwork> map = NETWORKS.get(level);
        if (map != null) {
            map.clear();
        }
    }

    public static void unload(LevelAccessor level) {
        NETWORKS.remove(level);
    }

    private static ItemNetwork build(Level level, BlockPos start) {
        Set<BlockPos> conduits = new HashSet<>();
        List<ItemNetwork.Endpoint> endpoints = new ArrayList<>();
        List<BlockPos> devices = new ArrayList<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        conduits.add(start);
        while (!queue.isEmpty() && conduits.size() < MAX_CONDUITS) {
            BlockPos current = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);
                if (!level.isLoaded(next)) {
                    continue;
                }
                if (level.getBlockState(next).getBlock() instanceof ConduitBlock) {
                    if (conduits.add(next)) {
                        queue.add(next);
                    }
                } else if (level.getBlockEntity(next) instanceof NetworkDevice) {
                    if (!devices.contains(next)) {
                        devices.add(next.immutable());
                        // il baule che tocca un nucleo logistico e' il "baule principale" della rete
                        if (level.getBlockEntity(next) instanceof LogisticsCoreBlockEntity) {
                            for (Direction side : Direction.values()) {
                                BlockPos chest = next.relative(side);
                                if (level.getBlockState(chest).getBlock() instanceof ConduitBlock
                                        || level.getBlockEntity(chest) instanceof NetworkDevice) {
                                    continue;
                                }
                                if (level.getCapability(Capabilities.ItemHandler.BLOCK, chest, side.getOpposite()) != null) {
                                    endpoints.removeIf(e -> e.pos().equals(chest));
                                    endpoints.add(new ItemNetwork.Endpoint(chest.immutable(), side.getOpposite(),
                                            current.immutable(), true));
                                    break;
                                }
                            }
                        }
                    }
                } else if (level.getCapability(Capabilities.ItemHandler.BLOCK, next, dir.getOpposite()) != null
                        && endpoints.stream().noneMatch(e -> e.main() && e.pos().equals(next))) {
                    // un contenitore conta una volta sola: se lo tocca un tubo di destinazione/analizzatore vince quello
                    boolean special = level.getBlockEntity(current) instanceof SortingConduitBlockEntity;
                    ItemNetwork.Endpoint existing = endpoints.stream().filter(e -> e.pos().equals(next)).findFirst().orElse(null);
                    if (existing == null) {
                        endpoints.add(new ItemNetwork.Endpoint(next.immutable(), dir.getOpposite(), current.immutable()));
                    } else if (special && !(level.getBlockEntity(existing.conduit()) instanceof SortingConduitBlockEntity)) {
                        endpoints.remove(existing);
                        endpoints.add(new ItemNetwork.Endpoint(next.immutable(), dir.getOpposite(), current.immutable()));
                    }
                }
            }
        }
        return new ItemNetwork(level, conduits, endpoints, devices);
    }

    /** Segna i blocchi che partecipano alla rete senza essere contenitori (terminale, banco di assemblaggio) */
    public interface NetworkDevice {
    }
}
