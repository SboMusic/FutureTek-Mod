package com.futuretek.bee;

import java.util.Locale;

/**
 * Le specie di api di FutureTek. Qui sta solo "chi sono" (nome e colore);
 * prodotti e incroci sono nel file JSON data/futuretek/bee_data/bees.json (modificabile).
 */
public enum BeeSpecies {
    COMMON(0xE8C04A),
    FOREST(0x5E9E3A),
    MEADOW(0xE88AC8),
    MINERAL(0x9A9A9A),
    METALLIC(0xC8D2DC),
    GLACIAL(0x9EE0FF),
    EMBER(0xFF6A1A),
    ABYSSAL(0x3A2A5A),
    VOLTAIC(0xBE5AFF),
    STELLAR(0xFFF4A0);

    public final int color;

    BeeSpecies(int color) {
        this.color = color;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static BeeSpecies byId(String id) {
        for (BeeSpecies s : values()) {
            if (s.id().equals(id)) {
                return s;
            }
        }
        return COMMON;
    }
}
