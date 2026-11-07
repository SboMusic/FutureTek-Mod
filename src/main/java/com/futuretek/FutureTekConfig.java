package com.futuretek;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Impostazioni modificabili dall'utente.
 * Il file viene creato automaticamente in config/futuretek-common.toml
 * (nel gioco di sviluppo: runs/client/config o runs/server/config).
 */
public class FutureTekConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue DEBUG_LOGGING = BUILDER
            .comment("Scrive messaggi extra nel log, utili per trovare problemi.")
            .define("debugLogging", false);

    static {
        BUILDER.comment("Generatori di energia (valori in TU per tick; 1 TU = 1 FE)").push("generators");
    }

    public static final ModConfigSpec.IntValue COMBUSTION_RATE = BUILDER
            .comment("Produzione del generatore a combustione mentre brucia")
            .defineInRange("combustionRate", 12, 1, 10000);

    public static final ModConfigSpec.IntValue SOLAR_RATE = BUILDER
            .comment("Produzione del pannello solare di giorno, a cielo aperto (con la pioggia: la meta')")
            .defineInRange("solarRate", 4, 1, 10000);

    public static final ModConfigSpec.IntValue GEOTHERMAL_RATE = BUILDER
            .comment("Produzione del generatore geotermico mentre ha lava")
            .defineInRange("geothermalRate", 16, 1, 10000);

    public static final ModConfigSpec.IntValue LAVA_ENERGY_PER_MB = BUILDER
            .comment("TU prodotte da 1 mB di lava (1 secchio = 1000 mB)")
            .defineInRange("lavaEnergyPerMb", 20, 1, 10000);

    static {
        BUILDER.pop();
        BUILDER.comment("Reattore a fissione e tecnologia avanzata").push("advanced");
    }

    public static final ModConfigSpec.BooleanValue NUKES_ENABLED = BUILDER
            .comment("TNT nucleare attiva (false = si spegne senza esplodere)")
            .define("nukesEnabled", true);

    public static final ModConfigSpec.IntValue NUKE_RADIUS = BUILDER
            .comment("Raggio del cratere della TNT nucleare (blocchi)")
            .defineInRange("nukeRadius", 32, 8, 64);

    public static final ModConfigSpec.BooleanValue REACTOR_EXPLOSIONS = BUILDER
            .comment("Se il reattore si surriscalda al 100% esplode (true) oppure si spegne d'emergenza (false)")
            .define("reactorExplosions", true);

    public static final ModConfigSpec.IntValue REACTOR_BLAST_RADIUS = BUILDER
            .comment("Raggio MASSIMO del cratere quando il reattore esplode (8 + 1 per ogni barra di uranio)")
            .defineInRange("reactorBlastRadius", 24, 4, 48);

    public static final ModConfigSpec.IntValue SHELTER_ENERGY_PER_USE = BUILDER
            .comment("TU consumate dal rifugio portatile ogni volta che costruisce la casa")
            .defineInRange("shelterEnergyPerUse", 25000, 0, 1000000);

    public static final ModConfigSpec.IntValue SHELTER_USES = BUILDER
            .comment("Quante case costruisce il rifugio portatile con la carica piena")
            .defineInRange("shelterUses", 6, 1, 100);

    public static final ModConfigSpec.IntValue QUARRY_ENERGY_PER_BLOCK = BUILDER
            .comment("TU consumate dall'escavatore per ogni blocco scavato")
            .defineInRange("quarryEnergyPerBlock", 64, 0, 100000);

    public static final ModConfigSpec.IntValue QUARRY_SIZE = BUILDER
            .comment("Lato dell'area quadrata scavata dall'escavatore")
            .defineInRange("quarrySize", 9, 3, 64);

    public static final ModConfigSpec.IntValue QUARRY_MAX_BLOCKS_PER_TICK = BUILDER
            .comment("Massimo di blocchi scavati per tick dall'escavatore: piu' energia riceve, piu' va veloce fino a questo limite")
            .defineInRange("quarryMaxBlocksPerTick", 8, 1, 64);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.comment("Comodita' di gioco").push("gameplay");
    }

    public static final ModConfigSpec.BooleanValue TREE_FELLING = BUILDER
            .comment("Con un'ascia si abbatte l'intero albero in un colpo (tieni Shift per rompere un tronco solo)")
            .define("treeFelling", true);

    public static final ModConfigSpec.IntValue TREE_FELLING_MAX_LOGS = BUILDER
            .comment("Numero massimo di tronchi abbattuti in un colpo")
            .defineInRange("treeFellingMaxLogs", 256, 1, 2048);

    static {
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();
}
