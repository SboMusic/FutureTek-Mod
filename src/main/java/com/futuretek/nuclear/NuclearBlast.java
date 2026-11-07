package com.futuretek.nuclear;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Esplosione nucleare: cancella una SFERA di blocchi (con il bordo irregolare, come un cratere).
 * Per non bloccare il server lavora "a fette": alcuni strati orizzontali per tick, dall'alto in basso.
 * - i blocchi indistruttibili (bedrock...) restano
 * - i contenitori NON lasciano cadere il contenuto (sparisce tutto: niente migliaia di oggetti per terra)
 * - le creature vicine prendono danni enormi; il fondo del cratere prende fuoco qua e la'
 */
public final class NuclearBlast {
    private static final List<NuclearBlast> ACTIVE = new ArrayList<>();
    private static final int SLICES_PER_TICK = 3;

    private final ServerLevel level;
    private final BlockPos center;
    private final int radius;
    private final RandomSource random;
    private int slice; // da +radius a -radius

    private NuclearBlast(ServerLevel level, BlockPos center, int radius) {
        this.level = level;
        this.center = center.immutable();
        this.radius = radius;
        this.random = RandomSource.create(center.asLong());
        this.slice = radius;
    }

    /** Fa partire un'esplosione nucleare */
    public static void start(ServerLevel level, BlockPos center, int radius, Entity source) {
        level.playSound(null, center, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 16.0F, 0.5F);
        level.playSound(null, center, SoundEvents.WITHER_DEATH, SoundSource.BLOCKS, 8.0F, 0.6F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5,
                12, radius / 3.0, radius / 4.0, radius / 3.0, 0);
        level.sendParticles(ParticleTypes.FLASH, center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5, 3, 0, 0, 0, 0);
        // danni alle creature: fortissimi al centro, nulli a 1,5 volte il raggio
        double reach = radius * 1.5;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(center).inflate(reach))) {
            double d = Math.sqrt(e.distanceToSqr(center.getCenter()));
            if (d < reach) {
                float damage = (float) (120 * (1 - d / reach)) + 4;
                e.hurt(level.damageSources().explosion(source, source), damage);
                e.setRemainingFireTicks(200);
            }
        }
        ACTIVE.add(new NuclearBlast(level, center, radius));
    }

    public static boolean isRunning() {
        return !ACTIVE.isEmpty();
    }

    /** Ogni tick del server: ogni esplosione toglie qualche strato */
    public static void onServerTick(ServerTickEvent.Post event) {
        Iterator<NuclearBlast> it = ACTIVE.iterator();
        while (it.hasNext()) {
            NuclearBlast blast = it.next();
            if (blast.step()) {
                it.remove();
            }
        }
    }

    /** true = finita */
    private boolean step() {
        for (int n = 0; n < SLICES_PER_TICK; n++) {
            if (slice < -radius) {
                finish();
                return true;
            }
            clearSlice(slice--);
        }
        return false;
    }

    private void clearSlice(int dy) {
        int y = center.getY() + dy;
        if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) {
            return;
        }
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -radius - 2; dx <= radius + 2; dx++) {
            for (int dz = -radius - 2; dz <= radius + 2; dz++) {
                double d2 = dx * dx + dy * dy * 1.3 + dz * dz; // un po' schiacciata in verticale
                // bordo irregolare: il raggio varia di qualche blocco
                double r = radius + Mth.sin(dx * 0.37F + dz * 0.21F) * 1.5 + random.nextFloat() * 1.2;
                if (d2 > r * r) {
                    continue;
                }
                p.set(center.getX() + dx, y, center.getZ() + dz);
                BlockState s = level.getBlockState(p);
                if (s.isAir() || s.getDestroySpeed(level, p) < 0) {
                    continue;
                }
                if (level.getBlockEntity(p) != null) {
                    level.removeBlockEntity(p); // niente oggetti sparsi ovunque
                }
                level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }

    /** Fuoco sul fondo e nuvola di fumo */
    private void finish() {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = 0; i < radius * radius; i++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            if (dx * dx + dz * dz > radius * radius) {
                continue;
            }
            p.set(center.getX() + dx, center.getY(), center.getZ() + dz);
            while (p.getY() > level.getMinBuildHeight() && level.getBlockState(p).isAir()) {
                p.move(0, -1, 0);
            }
            BlockPos fire = p.above();
            if (random.nextInt(4) == 0 && level.getBlockState(fire).isAir()
                    && Blocks.FIRE.defaultBlockState().canSurvive(level, fire)) {
                level.setBlock(fire, Blocks.FIRE.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, center.getX() + 0.5, center.getY() + radius * 0.5,
                center.getZ() + 0.5, 60, radius / 2.0, radius / 3.0, radius / 2.0, 0.05);
    }
}
