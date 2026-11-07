package com.futuretek.blockentity;

import com.futuretek.block.StarCollectorBlock;
import com.futuretek.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Captatore Stellare: raccoglie Essenza dalla luce del cielo (di giorno, a cielo aperto; di notte 1/4).
 * La spinge da solo nei Replicatori vicini, oppure si svuota in un Cristallo d'Essenza (click destro).
 */
public class StarCollectorBlockEntity extends BlockEntity {
    private long stored;

    public StarCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STAR_COLLECTOR.get(), pos, state);
    }

    private StarCollectorBlock block() {
        return (StarCollectorBlock) getBlockState().getBlock();
    }

    public long getStored() {
        return stored;
    }

    public long getCapacity() {
        return block().getCapacity();
    }

    public long take(long max) {
        long taken = Math.min(max, stored);
        stored -= taken;
        setChanged();
        return taken;
    }

    public void serverTick() {
        if (level.getGameTime() % 20 != 0) {
            return;
        }
        if (level.canSeeSky(worldPosition.above())) {
            int perSecond = block().getRate();
            long gain = level.isDay() ? perSecond : Math.max(1, perSecond / 4);
            stored = Math.min(getCapacity(), stored + gain);
            setChanged();
        }
        // spinge l'Essenza nei replicatori vicini
        for (Direction dir : Direction.values()) {
            if (stored <= 0) {
                break;
            }
            if (level.getBlockEntity(worldPosition.relative(dir)) instanceof ReplicatorBlockEntity replicator) {
                stored -= replicator.receiveEssence(stored);
                setChanged();
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("Essence", stored);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stored = tag.getLong("Essence");
    }
}
