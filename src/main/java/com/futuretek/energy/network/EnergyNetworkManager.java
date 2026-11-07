package com.futuretek.energy.network;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import com.futuretek.block.CableBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * Tiene in memoria le reti di cavi di ogni mondo.
 * Le reti si ricostruiscono da sole quando qualcosa cambia (un cavo o una macchina
 * viene piazzata o rotta vicino a un cavo): basta chiamare invalidate().
 * Funziona solo sul server: il client non sposta mai energia.
 */
public final class EnergyNetworkManager {
    /** Limite di sicurezza: una rete piu' grande di cosi' viene tagliata */
    private static final int MAX_CABLES = 8192;

    private static final Map<LevelAccessor, Map<BlockPos, EnergyNetwork>> NETWORKS = new WeakHashMap<>();

    private EnergyNetworkManager() {
    }

    /** Restituisce la rete di cui fa parte il cavo in "pos" (la costruisce se serve) */
    @Nullable
    public static EnergyNetwork get(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return null;
        }
        Map<BlockPos, EnergyNetwork> map = NETWORKS.computeIfAbsent(level, l -> new HashMap<>());
        EnergyNetwork network = map.get(pos);
        if (network == null) {
            network = build(level, pos);
            if (network != null) {
                for (BlockPos cable : network.getCables()) {
                    map.put(cable, network);
                }
            }
        }
        return network;
    }

    /** Da chiamare quando cambia qualcosa: tutte le reti di quel mondo verranno ricostruite */
    public static void invalidate(LevelAccessor level) {
        Map<BlockPos, EnergyNetwork> map = NETWORKS.get(level);
        if (map != null) {
            map.clear();
        }
    }

    public static void unload(LevelAccessor level) {
        NETWORKS.remove(level);
    }

    /** Ricerca "a macchia d'olio" di tutti i cavi collegati a partire da "start" */
    @Nullable
    private static EnergyNetwork build(Level level, BlockPos start) {
        BlockState startState = level.getBlockState(start);
        if (!(startState.getBlock() instanceof CableBlock)) {
            return null;
        }
        Set<BlockPos> cables = new HashSet<>();
        List<EnergyNetwork.Endpoint> endpoints = new ArrayList<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        cables.add(start);
        int capacity = Integer.MAX_VALUE;

        while (!queue.isEmpty() && cables.size() < MAX_CABLES) {
            BlockPos current = queue.poll();
            BlockState state = level.getBlockState(current);
            if (state.getBlock() instanceof CableBlock cable) {
                capacity = Math.min(capacity, cable.getTier().rate);
            }
            for (Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);
                if (!level.isLoaded(next)) {
                    continue;
                }
                BlockState nextState = level.getBlockState(next);
                if (nextState.getBlock() instanceof CableBlock) {
                    if (cables.add(next)) {
                        queue.add(next);
                    }
                } else if (level.getCapability(Capabilities.EnergyStorage.BLOCK, next, dir.getOpposite()) != null) {
                    endpoints.add(new EnergyNetwork.Endpoint(next.immutable(), dir.getOpposite()));
                }
            }
        }
        return new EnergyNetwork(level, cables, endpoints, capacity);
    }
}
