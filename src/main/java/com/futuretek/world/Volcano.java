package com.futuretek.world;

import com.futuretek.FutureTek;
import com.futuretek.registry.ModBuildingBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Vulcano: un cono di roccia vulcanica con un cratere pieno di lava, magma intorno alla bocca e un camino di
 * lava che scende sotto terra (circondato da ossidiana). Raro, compare in superficie nei biomi di terra.
 * Il centro è sempre al centro del chunk, così il cono (raggio massimo 14) resta nella zona che la
 * generazione può modificare in sicurezza.
 */
public final class Volcano extends Feature<NoneFeatureConfiguration> {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, FutureTek.MODID);
    public static final DeferredHolder<Feature<?>, Volcano> VOLCANO = FEATURES.register("volcano", Volcano::new);

    public Volcano() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource rnd = ctx.random();
        BlockPos o = ctx.origin();
        int cx = (o.getX() & ~15) + 8;
        int cz = (o.getZ() & ~15) + 8;
        int ground = floor(level, cx, cz);
        BlockPos center = new BlockPos(cx, ground, cz);
        // niente vulcani in acqua o sui pendii troppo ripidi
        if (!level.getFluidState(center).isEmpty() || !level.getFluidState(center.below()).isEmpty()) {
            return false;
        }
        int radius = 10 + rnd.nextInt(5);                // 10..14
        int height = radius + 6 + rnd.nextInt(7);        // cono alto 16..26
        int crater = 2 + rnd.nextInt(2);
        int top = ground + height;
        int rim = ground + (int) Math.round(height * Math.pow(1.0 - (double) crater / radius, 1.1)) + 1;
        int lavaTop = rim - 2;
        int minGround = Integer.MAX_VALUE;
        int maxGround = Integer.MIN_VALUE;
        for (int dx = -radius; dx <= radius; dx += 3) {
            for (int dz = -radius; dz <= radius; dz += 3) {
                int g = floor(level, cx + dx, cz + dz);
                minGround = Math.min(minGround, g);
                maxGround = Math.max(maxGround, g);
            }
        }
        if (maxGround - minGround > 18 || top >= level.getMaxBuildHeight() - 4) {
            return false;
        }

        BlockState rock = ModBuildingBlocks.VOLCANIC_ROCK.get().defaultBlockState();
        BlockState cobbled = ModBuildingBlocks.COBBLED_VOLCANIC_ROCK.get().defaultBlockState();
        BlockState magma = Blocks.MAGMA_BLOCK.defaultBlockState();
        BlockState lava = Blocks.LAVA.defaultBlockState();
        BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();

        for (int dx = -radius - 2; dx <= radius + 2; dx++) {
            for (int dz = -radius - 2; dz <= radius + 2; dz++) {
                double exact = Math.sqrt(dx * dx + dz * dz);
                double d = exact + (rnd.nextDouble() - 0.5) * 0.9;
                if (d > radius) {
                    continue;
                }
                double t = Math.max(0, 1.0 - d / radius);
                int surface = ground + (int) Math.round(height * Math.pow(t, 1.1));
                boolean inCrater = exact < crater;
                if (inCrater) {
                    surface = lavaTop - 3;                // fondo del cratere
                } else {
                    surface = Math.min(surface, rim);
                    if (exact < crater + 1.2) {
                        surface = rim;                    // bordo del cratere: tiene dentro la lava
                    }
                }
                d = inCrater ? Math.min(d, crater - 0.01) : Math.max(d, crater);
                int x = cx + dx;
                int z = cz + dz;
                int g = floor(level, x, z);
                // riempie dal terreno (anche un po' sotto, per non lasciare buchi) fino alla superficie del cono
                for (int y = Math.min(g, ground) - 3; y <= rim + 3; y++) {
                    p.set(x, y, z);
                    if (y > surface) {
                        if (!inCrater && y <= surface + 3 && !level.getBlockState(p).isAir()
                                && !level.getBlockState(p).is(BlockTags.LOGS)) {
                            level.setBlock(p, air, Block.UPDATE_CLIENTS);   // via erba, fiori e foglie sul cono
                        }
                        continue;
                    }
                    BlockState here = level.getBlockState(p);
                    if (y < g - 3 || here.is(Blocks.BEDROCK)) {
                        continue;
                    }
                    BlockState s = rock;
                    if (y == surface && rnd.nextFloat() < 0.18) {
                        s = cobbled;
                    }
                    if (y >= surface - 1 && d < crater + 2.2 && d >= crater && rnd.nextFloat() < 0.6) {
                        s = magma;                                         // bocca rovente
                    }
                    level.setBlock(p, s, Block.UPDATE_CLIENTS);
                }
                // il cratere: lago di lava
                if (inCrater) {
                    for (int y = lavaTop - 2; y <= lavaTop; y++) {
                        setLava(level, p.set(x, y, z), lava);
                    }
                }
            }
        }
        // camino di lava: dal cratere fino a 14 blocchi sotto la base, con un guscio di ossidiana
        for (int y = ground - 14; y < lavaTop - 2; y++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    int dd = dx * dx + dz * dz;
                    p.set(cx + dx, y, cz + dz);
                    if (level.getBlockState(p).is(Blocks.BEDROCK)) {
                        continue;
                    }
                    if (dd <= 1) {
                        setLava(level, p, lava);
                    } else if (dd <= 5) {
                        level.setBlock(p, rnd.nextFloat() < 0.35 ? obsidian : rock, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        // qualche colata di lava raffreddata (pietrisco e magma) giù per i fianchi
        int flows = 2 + rnd.nextInt(3);
        for (int f = 0; f < flows; f++) {
            double a = rnd.nextDouble() * Mth.TWO_PI;
            for (double r = crater + 1; r < radius; r += 0.7) {
                int x = cx + (int) Math.round(Math.cos(a) * r);
                int z = cz + (int) Math.round(Math.sin(a) * r);
                int y = surface(level, x, z) - 1;
                p.set(x, y, z);
                if (level.getBlockState(p).is(rock.getBlock()) || level.getBlockState(p).is(cobbled.getBlock())) {
                    level.setBlock(p, rnd.nextFloat() < 0.25 ? magma : cobbled, Block.UPDATE_CLIENTS);
                }
                a += (rnd.nextDouble() - 0.5) * 0.25;
            }
        }
        return true;
    }

    /** Durante la generazione ci sono le mappe d'altezza "_WG"; in un mondo già fatto (comando /place) quelle normali */
    private static int floor(WorldGenLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
        return y > level.getMinBuildHeight() ? y : level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
    }

    private static int surface(WorldGenLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        return y > level.getMinBuildHeight() ? y : level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
    }

    private static void setLava(WorldGenLevel level, BlockPos pos, BlockState lava) {
        level.setBlock(pos, lava, Block.UPDATE_CLIENTS);
        level.scheduleTick(pos, Fluids.LAVA, 0);
    }
}
