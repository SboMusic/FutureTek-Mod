package com.futuretek.world;

import com.futuretek.FutureTek;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Il Margine Cristallino: la dimensione di FutureTek.
 * La forma del terreno, il bioma e il cielo sono definiti nei file JSON in data/futuretek/dimension*,
 * data/futuretek/worldgen/noise_settings e data/futuretek/worldgen/biome.
 */
public final class EdgeDimension {
    public static final ResourceKey<Level> KEY =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(FutureTek.MODID, "crystal_edge"));

    private EdgeDimension() {
    }

    /** Trova un punto sicuro in superficie in (x, z); se sotto c'e' il vuoto, costruisce una piattaforma */
    public static BlockPos safeSurface(ServerLevel level, int x, int z, Block platform) {
        level.getChunk(x >> 4, z >> 4); // genera il chunk se non esiste
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        if (y <= level.getMinBuildHeight() + 1) {
            pos = new BlockPos(x, 100, z);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    level.setBlockAndUpdate(pos.offset(dx, -1, dz), platform.defaultBlockState());
                }
            }
        }
        return pos;
    }
}
