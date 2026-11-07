package com.futuretek.bee;

import java.util.List;

import com.futuretek.registry.ModBlockEntities;
import com.futuretek.registry.ModDataComponents;
import com.futuretek.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Arnia tecnica. Slot 0 = regina, slot 1 = fuco, slot 2-10 = prodotti.
 * Con regina + fuco, di giorno e con il cielo sopra libero, ogni ciclo (30 s) produce i favi della specie della regina.
 * Dopo 6 cicli la regina muore: nasce una nuova regina (a volte di una SPECIE NUOVA, vedi incroci)
 * e 2 fuchi; il fuco usato viene consumato.
 */
public class ApiaryBlockEntity extends BlockEntity implements MenuProvider {
    public static final int QUEEN_LIFE = 6;
    public static final int CYCLE_TICKS = 600;
    public static final int SLOT_QUEEN = 0;
    public static final int SLOT_DRONE = 1;
    public static final int FIRST_OUTPUT = 2;

    private int progress;

    private final ItemStackHandler items = new ItemStackHandler(11) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_QUEEN) {
                return stack.is(ModItems.BEE_QUEEN.get());
            }
            if (slot == SLOT_DRONE) {
                return stack.is(ModItems.BEE_DRONE.get());
            }
            return false;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    /** Per tubi/tramogge: si estraggono solo i prodotti */
    private final IItemHandler automation = new IItemHandler() {
        @Override
        public int getSlots() {
            return 9;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(FIRST_OUTPUT + slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return items.extractItem(FIRST_OUTPUT + slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return progress;
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    public ApiaryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.APIARY.get(), pos, state);
    }

    public boolean canWork() {
        return !items.getStackInSlot(SLOT_QUEEN).isEmpty() && !items.getStackInSlot(SLOT_DRONE).isEmpty()
                && level.isDay() && level.canSeeSky(worldPosition.above());
    }

    public void serverTick() {
        if (!canWork()) {
            return;
        }
        if (++progress >= CYCLE_TICKS) {
            progress = 0;
            runCycle();
        }
        setChanged();
    }

    /** Un ciclo di lavoro delle api (pubblico per i test) */
    public void runCycle() {
        ItemStack queen = items.getStackInSlot(SLOT_QUEEN);
        ItemStack drone = items.getStackInSlot(SLOT_DRONE);
        if (queen.isEmpty() || drone.isEmpty()) {
            return;
        }
        BeeSpecies queenSpecies = BeeItem.species(queen);
        for (ItemStack product : BeeData.rollProducts(queenSpecies, level.random)) {
            addOutput(product);
        }
        int life = queen.getOrDefault(ModDataComponents.BEE_LIFE.get(), QUEEN_LIFE) - 1;
        if (life > 0) {
            ItemStack aged = queen.copy();
            aged.set(ModDataComponents.BEE_LIFE.get(), life);
            items.setStackInSlot(SLOT_QUEEN, aged);
            return;
        }
        // la regina muore: nuova generazione
        BeeSpecies child = BeeData.offspring(queenSpecies, BeeItem.species(drone), level.random);
        items.setStackInSlot(SLOT_QUEEN, BeeItem.of(ModItems.BEE_QUEEN.get(), child, 1));
        items.extractItem(SLOT_DRONE, 1, false);
        addOutput(BeeItem.of(ModItems.BEE_DRONE.get(), child, 2));
    }

    private void addOutput(ItemStack stack) {
        ItemStack rest = stack;
        for (int i = FIRST_OUTPUT; i < items.getSlots() && !rest.isEmpty(); i++) {
            ItemStack current = items.getStackInSlot(i);
            if (current.isEmpty()) {
                items.setStackInSlot(i, rest);
                rest = ItemStack.EMPTY;
            } else if (ItemStack.isSameItemSameComponents(current, rest) && current.getCount() < current.getMaxStackSize()) {
                int move = Math.min(rest.getCount(), current.getMaxStackSize() - current.getCount());
                items.setStackInSlot(i, current.copyWithCount(current.getCount() + move));
                rest = rest.copyWithCount(rest.getCount() - move);
            }
        }
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public IItemHandler getAutomation() {
        return automation;
    }

    public void dropContents() {
        for (int i = 0; i < items.getSlots(); i++) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                    items.getStackInSlot(i));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("Items"));
        progress = tag.getInt("Progress");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ApiaryMenu(id, inventory, worldPosition, items, data);
    }

}
