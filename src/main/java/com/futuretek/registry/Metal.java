package com.futuretek.registry;

import net.minecraft.world.level.material.MapColor;

/**
 * I metalli di FutureTek.
 * Per ognuno la mod crea automaticamente lingotto, pepita, polvere, piastra e blocco.
 * I metalli "da minerale" hanno anche la forma grezza (raw) e il blocco di grezzo.
 *
 * ATTENZIONE: se aggiungi un metallo qui, aggiungilo anche in tools/genera_risorse.py
 * (che crea modelli, texture, ricette, tag e traduzioni).
 */
public enum Metal {
    TIN("tin", true, MapColor.CLAY, ToolTier.STONE),
    LEAD("lead", true, MapColor.TERRACOTTA_BLUE, ToolTier.STONE),
    SILVER("silver", true, MapColor.SNOW, ToolTier.IRON),
    URANIUM("uranium", true, MapColor.COLOR_LIGHT_GREEN, ToolTier.IRON),
    BRONZE("bronze", false, MapColor.COLOR_ORANGE, ToolTier.STONE),
    STEEL("steel", false, MapColor.METAL, ToolTier.IRON);

    /** Nome usato negli ID, es. "tin" -> futuretek:tin_ingot */
    public final String id;
    /** true = esiste come minerale (ha la forma grezza) */
    public final boolean fromOre;
    public final MapColor mapColor;
    /** Attrezzo minimo per raccogliere i blocchi di questo metallo */
    public final ToolTier tier;

    Metal(String id, boolean fromOre, MapColor mapColor, ToolTier tier) {
        this.id = id;
        this.fromOre = fromOre;
        this.mapColor = mapColor;
        this.tier = tier;
    }

    public enum ToolTier { STONE, IRON }
}
