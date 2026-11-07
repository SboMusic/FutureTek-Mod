package com.futuretek.blockentity;

import javax.annotation.Nullable;

import com.futuretek.block.MachineBlock;
import com.futuretek.energy.FTEnergyStorage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Base comune di tutte le macchine di FutureTek.
 * Un "BlockEntity" e' la parte di un blocco che ricorda dati (energia, oggetti...)
 * e che puo' "pensare" a ogni tick (20 volte al secondo) sul server.
 */
public abstract class MachineBlockEntity extends BlockEntity {
    protected final FTEnergyStorage energy;

    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
            int capacity, int maxReceive, int maxExtract) {
        super(type, pos, state);
        this.energy = new FTEnergyStorage(capacity, maxReceive, maxExtract, this::setChanged);
    }

    /** Eseguito 20 volte al secondo, solo sul server */
    public abstract void serverTick();

    public FTEnergyStorage getEnergy() {
        return energy;
    }

    /** Energia vista da un certo lato (null = dall'interno / multimetro). Le macchine con lati diversi lo ridefiniscono. */
    public IEnergyStorage getEnergyHandler(@Nullable Direction side) {
        return energy;
    }

    /** Inventario della macchina (null se non ne ha) */
    @Nullable
    public ItemStackHandler getItems() {
        return null;
    }

    /** Quando il blocco viene rotto, gli oggetti dentro cadono a terra */
    public void dropContents(Level level, BlockPos pos) {
        ItemStackHandler items = getItems();
        if (items == null) {
            return;
        }
        for (int i = 0; i < items.getSlots(); i++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), items.getStackInSlot(i));
        }
    }

    /** Accende/spegne la "luce" del blocco (es. il fuoco visibile nel generatore) */
    protected void setLit(boolean lit) {
        BlockState state = getBlockState();
        if (level != null && state.hasProperty(MachineBlock.LIT) && state.getValue(MachineBlock.LIT) != lit) {
            level.setBlock(worldPosition, state.setValue(MachineBlock.LIT, lit), 3);
        }
    }

    public Direction getFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(MachineBlock.FACING) ? state.getValue(MachineBlock.FACING) : Direction.NORTH;
    }

    // --- Salvataggio ---
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", energy.getEnergyStored());
        ItemStackHandler items = getItems();
        if (items != null) {
            tag.put("Items", items.serializeNBT(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.setEnergy(tag.getInt("Energy"));
        ItemStackHandler items = getItems();
        if (items != null && tag.contains("Items")) {
            items.deserializeNBT(registries, tag.getCompound("Items"));
        }
    }

    /**
     * Dati da mostrare nell'interfaccia. Il gioco li manda al client come numeri a 16 bit,
     * quindi l'energia (che puo' superare 65.535) viene divisa in due meta'.
     * Indici: 0-1 energia, 2-3 capacita', dal 4 in poi dati extra della macchina.
     */
    protected abstract class MachineData implements ContainerData {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energy.getEnergyStored() & 0xFFFF;
                case 1 -> energy.getEnergyStored() >>> 16;
                case 2 -> energy.getMaxEnergyStored() & 0xFFFF;
                case 3 -> energy.getMaxEnergyStored() >>> 16;
                default -> getExtra(index - 4);
            };
        }

        protected abstract int getExtra(int index);

        @Override
        public void set(int index, int value) {
        }
    }
}
