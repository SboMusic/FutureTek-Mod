package com.futuretek.energy;

import net.neoforged.neoforge.energy.EnergyStorage;

/**
 * Il "serbatoio" di energia delle nostre macchine.
 * - receiveEnergy / extractEnergy: usati dall'ESTERNO (cavi, altre mod), rispettano i limiti al tick.
 * - generate / consume: usati dalla macchina stessa (produrre o spendere), senza limiti di ingresso/uscita.
 */
public class FTEnergyStorage extends EnergyStorage {
    private final Runnable onChange;

    public FTEnergyStorage(int capacity, int maxReceive, int maxExtract, Runnable onChange) {
        super(capacity, maxReceive, maxExtract);
        this.onChange = onChange;
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        int received = super.receiveEnergy(toReceive, simulate);
        if (received > 0 && !simulate) {
            onChange.run();
        }
        return received;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        int extracted = super.extractEnergy(toExtract, simulate);
        if (extracted > 0 && !simulate) {
            onChange.run();
        }
        return extracted;
    }

    /** La macchina produce energia (es. un generatore). Restituisce quanta ne e' entrata davvero. */
    public int generate(int amount) {
        int added = Math.max(0, Math.min(capacity - energy, amount));
        if (added > 0) {
            energy += added;
            onChange.run();
        }
        return added;
    }

    /** La macchina spende energia. Restituisce quanta ne e' stata tolta davvero. */
    public int consume(int amount) {
        int removed = Math.max(0, Math.min(energy, amount));
        if (removed > 0) {
            energy -= removed;
            onChange.run();
        }
        return removed;
    }

    public void setEnergy(int value) {
        energy = Math.max(0, Math.min(capacity, value));
    }

    /** Cambia la capacita' (es. potenziamenti). L'energia in eccesso va persa. */
    public void setCapacity(int newCapacity) {
        capacity = Math.max(1, newCapacity);
        energy = Math.min(energy, capacity);
    }

    public boolean isFull() {
        return energy >= capacity;
    }

    public int getSpace() {
        return capacity - energy;
    }
}
