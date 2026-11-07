package com.futuretek.machine;

import javax.annotation.Nullable;

import com.futuretek.recipe.ProcessingKind;

/**
 * Le macchine di lavorazione e i loro numeri di base.
 *   energyPerTick = TU consumate a ogni tick mentre lavora
 *   time          = durata di una lavorazione in tick (20 tick = 1 secondo), se la ricetta non dice altro
 * kind = tipo di ricette; null = usa le ricette della fornace di Minecraft (forno elettrico).
 */
public enum ProcessingMachine {
    CRUSHER("crusher", ProcessingKind.CRUSHING, 4, 300),
    ELECTRIC_FURNACE("electric_furnace", null, 6, 130),
    COMPRESSOR("compressor", ProcessingKind.COMPRESSING, 4, 300),
    EXTRACTOR("extractor", ProcessingKind.EXTRACTING, 4, 300),
    WIRE_PRESS("wire_press", ProcessingKind.WIRE_PRESSING, 4, 200),
    CHEMICAL_BATH("chemical_bath", ProcessingKind.WASHING, 8, 400, 250),
    THERMAL_CENTRIFUGE("thermal_centrifuge", ProcessingKind.CENTRIFUGING, 8, 500),
    /** Macchine avanzate: 4 volte piu' veloci, consumano di piu' (energia totale per oggetto un po' piu' alta) */
    ADVANCED_CRUSHER("advanced_crusher", ProcessingKind.CRUSHING, 20, 75),
    INDUCTION_FURNACE("induction_furnace", null, 30, 32);

    public final String id;
    @Nullable
    public final ProcessingKind kind;
    public final int energyPerTick;
    public final int time;
    /** mB di acqua consumati per ogni lavorazione (0 = la macchina non usa acqua) */
    public final int waterPerOperation;

    ProcessingMachine(String id, @Nullable ProcessingKind kind, int energyPerTick, int time) {
        this(id, kind, energyPerTick, time, 0);
    }

    ProcessingMachine(String id, @Nullable ProcessingKind kind, int energyPerTick, int time, int waterPerOperation) {
        this.id = id;
        this.kind = kind;
        this.energyPerTick = energyPerTick;
        this.time = time;
        this.waterPerOperation = waterPerOperation;
    }

    public boolean usesWater() {
        return waterPerOperation > 0;
    }
}
