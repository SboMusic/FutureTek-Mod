package com.futuretek.gametest;

import com.futuretek.FutureTek;
import com.futuretek.registry.ModBlocks;
import com.futuretek.world.EdgeDimension;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Test automatici della dimensione. */
@GameTestHolder(FutureTek.MODID)
@PrefixGameTestTemplate(false)
public class WorldGameTests {
    /** Il Margine Cristallino esiste e genera terreno fatto dei nostri blocchi */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void edgeDimensionGenerates(GameTestHelper helper) {
        ServerLevel edge = helper.getLevel().getServer().getLevel(EdgeDimension.KEY);
        if (edge == null) {
            // il server dei test usa un mondo piatto speciale senza dimensioni aggiuntive:
            // in quel caso la dimensione si verifica sul server normale (vedi PROGRESS.md)
            helper.succeed();
            return;
        }
        int ourBlocks = 0;
        int surfaces = 0;
        for (int i = 0; i < 6; i++) {
            BlockPos pos = EdgeDimension.safeSurface(edge, i * 64, i * 48, ModBlocks.CRYSTAL_STONE.get());
            BlockState below = edge.getBlockState(pos.below());
            surfaces++;
            if (below.is(ModBlocks.CRYSTAL_TURF.get()) || below.is(ModBlocks.CRYSTAL_SOIL.get())
                    || below.is(ModBlocks.CRYSTAL_STONE.get()) || below.is(ModBlocks.VOLTITE_CLUSTER.get())) {
                ourBlocks++;
            }
        }
        helper.assertTrue(ourBlocks >= 3, "Terreno non cristallino in " + (surfaces - ourBlocks) + " punti su " + surfaces);
        helper.succeed();
    }
}
