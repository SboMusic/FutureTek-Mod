package com.futuretek.blockentity;

import com.futuretek.menu.FilterMenu;
import com.futuretek.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Condotto smistatore: ha un filtro di 9 oggetti "fantasma" (non vengono consumati).
 * I contenitori accanto a questo condotto ricevono dalla rete SOLO gli oggetti del filtro.
 * Filtro vuoto = non accetta niente.
 */
public class SortingConduitBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler filter = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public SortingConduitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SORTING_CONDUIT.get(), pos, state);
    }

    protected SortingConduitBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos,
            BlockState state) {
        super(type, pos, state);
    }

    public boolean accepts(ItemStack stack) {
        for (int i = 0; i < filter.getSlots(); i++) {
            ItemStack f = filter.getStackInSlot(i);
            if (!f.isEmpty() && ItemStack.isSameItem(f, stack)) {
                return true;
            }
        }
        return false;
    }

    public ItemStackHandler getFilter() {
        return filter;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Filter", filter.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        filter.deserializeNBT(registries, tag.getCompound("Filter"));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new FilterMenu(id, inventory, worldPosition, filter);
    }
}
