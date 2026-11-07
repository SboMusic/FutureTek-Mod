package com.futuretek.energy.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Una "rete" = un gruppo di cavi collegati tra loro.
 * Quando un generatore spinge energia in un cavo, la rete la divide in parti uguali
 * tra tutti i blocchi collegati che possono riceverla (gli "estremi").
 * La rete trasporta al massimo "capacity" TU per tick: e' il limite del cavo piu' debole.
 */
public class EnergyNetwork {
    /** Un blocco collegato alla rete: posizione + lato da cui il cavo lo tocca */
    public record Endpoint(BlockPos pos, Direction face) {
    }

    private final Level level;
    private final Set<BlockPos> cables;
    private final List<Endpoint> endpoints;
    private final int capacity;

    private long currentTick = -1;
    private int transferredThisTick;
    private int transferredLastTick;

    public EnergyNetwork(Level level, Set<BlockPos> cables, List<Endpoint> endpoints, int capacity) {
        this.level = level;
        this.cables = cables;
        this.endpoints = endpoints;
        this.capacity = capacity;
    }

    /**
     * Distribuisce energia ai blocchi collegati.
     * @param exclude posizione di chi sta inviando (non gli restituiamo la sua stessa energia)
     * @return quanta energia e' stata accettata
     */
    public int distribute(int amount, boolean simulate, @Nullable BlockPos exclude) {
        updateTick();
        int allowed = Math.min(amount, capacity - transferredThisTick);
        if (allowed <= 0) {
            return 0;
        }

        List<IEnergyStorage> targets = new ArrayList<>();
        for (Endpoint endpoint : endpoints) {
            if (endpoint.pos().equals(exclude)) {
                continue;
            }
            IEnergyStorage handler = level.getCapability(Capabilities.EnergyStorage.BLOCK, endpoint.pos(),
                    endpoint.face());
            if (handler != null && handler.canReceive()) {
                targets.add(handler);
            }
        }
        if (targets.isEmpty()) {
            return 0;
        }

        int remaining = allowed;
        // 1° giro: divisione in parti uguali
        for (int i = 0; i < targets.size() && remaining > 0; i++) {
            int share = (int) Math.ceil(remaining / (double) (targets.size() - i));
            remaining -= targets.get(i).receiveEnergy(share, simulate);
        }
        // 2° giro: quello che avanza va a chi ha ancora spazio
        for (int i = 0; i < targets.size() && remaining > 0; i++) {
            remaining -= targets.get(i).receiveEnergy(remaining, simulate);
        }

        int given = allowed - remaining;
        if (!simulate) {
            transferredThisTick += given;
        }
        return given;
    }

    private void updateTick() {
        long now = level.getGameTime();
        if (now != currentTick) {
            transferredLastTick = currentTick == now - 1 ? transferredThisTick : 0;
            transferredThisTick = 0;
            currentTick = now;
        }
    }

    public Set<BlockPos> getCables() {
        return cables;
    }

    public int getCableCount() {
        return cables.size();
    }

    public int getEndpointCount() {
        return endpoints.size();
    }

    public int getCapacity() {
        return capacity;
    }

    /** Energia passata nell'ultimo tick completo (per il multimetro) */
    public int getTransferredLastTick() {
        updateTick();
        return transferredLastTick;
    }
}
