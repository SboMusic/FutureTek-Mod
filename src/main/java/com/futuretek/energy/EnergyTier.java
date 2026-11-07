package com.futuretek.energy;

/**
 * Livelli di tensione di FutureTek.
 * "rate" = quante TU (Tek Unit) al tick possono passare al massimo.
 * 1 TU = 1 FE (Forge Energy): le altre mod tecnologiche leggono la nostra energia senza convertitori.
 */
public enum EnergyTier {
    LOW("low", 32),
    MEDIUM("medium", 128),
    HIGH("high", 512),
    EXTREME("extreme", 2048);

    public final String id;
    public final int rate;

    EnergyTier(String id, int rate) {
        this.id = id;
        this.rate = rate;
    }
}
