package com.futuretek.energy.network;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Cio' che un generatore "vede" quando guarda un cavo:
 * un ricevitore che inoltra subito l'energia alla rete. Il cavo non accumula niente.
 */
public class CableEnergyHandler implements IEnergyStorage {
    private final Level level;
    private final BlockPos cablePos;
    @Nullable
    private final Direction side;

    public CableEnergyHandler(Level level, BlockPos cablePos, @Nullable Direction side) {
        this.level = level;
        this.cablePos = cablePos;
        this.side = side;
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        EnergyNetwork network = EnergyNetworkManager.get(level, cablePos);
        if (network == null) {
            return 0;
        }
        BlockPos sender = side == null ? null : cablePos.relative(side);
        return network.distribute(toReceive, simulate, sender);
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return 0;
    }

    @Override
    public int getMaxEnergyStored() {
        return 0;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return !level.isClientSide;
    }
}
