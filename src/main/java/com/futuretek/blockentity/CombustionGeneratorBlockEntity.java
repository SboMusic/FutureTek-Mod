package com.futuretek.blockentity;

import com.futuretek.FutureTekConfig;
import com.futuretek.energy.EnergyTier;
import com.futuretek.energy.EnergyUtil;
import com.futuretek.menu.CombustionGeneratorMenu;
import com.futuretek.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Generatore a combustione: brucia tutto cio' che brucia in una fornace.
 * Produce COMBUSTION_RATE TU/tick (config) per tutta la durata del combustibile.
 * Esempio: carbone = 1600 tick x 20 TU = 32.000 TU.
 */
public class CombustionGeneratorBlockEntity extends MachineBlockEntity implements MenuProvider {
    public static final int CAPACITY = 4000;

    private final ItemStackHandler items = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getBurnTime(RecipeType.SMELTING) > 0;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private int burnTime;
    private int burnTimeTotal;

    private final ContainerData data = new MachineData() {
        @Override
        protected int getExtra(int index) {
            return switch (index) {
                case 0 -> Math.min(burnTime, Short.MAX_VALUE);
                case 1 -> Math.min(burnTimeTotal, Short.MAX_VALUE);
                default -> 0;
            };
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public CombustionGeneratorBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.COMBUSTION_GENERATOR.get(), pos, state, CAPACITY, EnergyTier.LOW.rate);
    }

    /** Per il generatore avanzato */
    protected CombustionGeneratorBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos,
            BlockState state, int capacity, int maxOutput) {
        super(type, pos, state, capacity, 0, maxOutput);
    }

    /** TU prodotti a ogni tick di combustione */
    protected int rate() {
        return FutureTekConfig.COMBUSTION_RATE.getAsInt();
    }

    /** Quanti "tick di combustibile" consuma ogni tick (1 = come una fornace) */
    protected int burnSpeed() {
        return 1;
    }

    protected int outputRate() {
        return EnergyTier.LOW.rate;
    }

    @Override
    public void serverTick() {
        // se non sta bruciando e c'e' spazio per l'energia, prende un nuovo combustibile
        if (burnTime <= 0 && !energy.isFull()) {
            ItemStack fuel = items.getStackInSlot(0);
            int fuelTime = fuel.getBurnTime(RecipeType.SMELTING);
            if (fuelTime > 0) {
                ItemStack leftover = fuel.getCraftingRemainingItem(); // es. secchio di lava -> secchio vuoto
                items.extractItem(0, 1, false);
                if (!leftover.isEmpty() && items.getStackInSlot(0).isEmpty()) {
                    items.setStackInSlot(0, leftover);
                }
                burnTime = fuelTime;
                burnTimeTotal = fuelTime;
                setChanged();
            }
        }
        if (burnTime > 0) {
            burnTime = Math.max(0, burnTime - burnSpeed());
            energy.generate(rate());
        }
        setLit(burnTime > 0);
        EnergyUtil.pushToNeighbors(level, worldPosition, energy, outputRate());
    }

    @Override
    public ItemStackHandler getItems() {
        return items;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnTimeTotal", burnTimeTotal);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        burnTime = tag.getInt("BurnTime");
        burnTimeTotal = tag.getInt("BurnTimeTotal");
    }

    // --- Interfaccia ---
    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new CombustionGeneratorMenu(id, inventory, worldPosition, items, data);
    }
}
