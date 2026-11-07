package com.futuretek.alchemy;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * Il contenuto della Bisaccia Dimensionale: 54 slot salvati SUL GIOCATORE
 * (quindi qualunque bisaccia apri, trovi sempre le stesse cose; resta anche dopo la morte).
 */
public class SatchelInventory extends SimpleContainer implements INBTSerializable<CompoundTag> {
    public static final int SIZE = 54;

    public SatchelInventory() {
        super(SIZE);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        ContainerHelper.saveAllItems(tag, getItems(), provider);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        clearContent();
        ContainerHelper.loadAllItems(tag, getItems(), provider);
    }
}
