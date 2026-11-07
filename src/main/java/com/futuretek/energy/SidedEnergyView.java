package com.futuretek.energy;

import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * "Finestra" su un serbatoio che permette solo di inserire, solo di estrarre, o entrambi.
 * Serve per le macchine che hanno lati diversi, es. l'accumulatore:
 * entra energia da 5 lati, esce solo dal lato frontale.
 */
public class SidedEnergyView implements IEnergyStorage {
    private final IEnergyStorage storage;
    private final boolean allowReceive;
    private final boolean allowExtract;

    public SidedEnergyView(IEnergyStorage storage, boolean allowReceive, boolean allowExtract) {
        this.storage = storage;
        this.allowReceive = allowReceive;
        this.allowExtract = allowExtract;
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        return allowReceive ? storage.receiveEnergy(toReceive, simulate) : 0;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        return allowExtract ? storage.extractEnergy(toExtract, simulate) : 0;
    }

    @Override
    public int getEnergyStored() {
        return storage.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored() {
        return storage.getMaxEnergyStored();
    }

    @Override
    public boolean canExtract() {
        return allowExtract && storage.canExtract();
    }

    @Override
    public boolean canReceive() {
        return allowReceive && storage.canReceive();
    }
}
