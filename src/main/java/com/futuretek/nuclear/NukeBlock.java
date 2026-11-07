package com.futuretek.nuclear;

import javax.annotation.Nullable;

import com.futuretek.FutureTekConfig;
import com.futuretek.registry.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * TNT nucleare: si accende come la TNT normale (acciarino, redstone, fuoco, altre esplosioni)
 * ma ha una miccia di 10 secondi e cancella una sfera enorme (raggio nella config, di base 32 blocchi).
 */
public class NukeBlock extends TntBlock {
    public NukeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction face, @Nullable LivingEntity igniter) {
        prime(level, pos, 200);
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        prime(level, pos, 60 + level.random.nextInt(40));
    }

    static void prime(Level level, BlockPos pos, int fuse) {
        if (level.isClientSide) {
            return;
        }
        if (!FutureTekConfig.NUKES_ENABLED.getAsBoolean()) {
            level.removeBlock(pos, false);
            return;
        }
        Primed primed = new Primed(ModEntities.PRIMED_NUKE.get(), level);
        primed.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        primed.setFuse(fuse);
        primed.setBlockState(level.getBlockState(pos));
        level.removeBlock(pos, false);
        level.addFreshEntity(primed);
        level.playSound(null, pos, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 0.6F);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2.0F, 0.5F);
        level.gameEvent(null, GameEvent.PRIME_FUSE, pos);
    }

    /** La TNT nucleare accesa */
    public static class Primed extends PrimedTnt {
        public Primed(EntityType<? extends PrimedTnt> type, Level level) {
            super(type, level);
        }

        @Override
        public void tick() {
            super.tick();
            if (!level().isClientSide && getFuse() % 20 == 0 && getFuse() > 0) {
                level().playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 2.0F, 0.5F);
            }
        }

        @Override
        protected void explode() {
            if (level() instanceof ServerLevel server) {
                NuclearBlast.start(server, blockPosition(), FutureTekConfig.NUKE_RADIUS.getAsInt(), this);
            }
        }
    }
}
