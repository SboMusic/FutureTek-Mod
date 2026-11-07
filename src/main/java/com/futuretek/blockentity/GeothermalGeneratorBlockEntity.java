package com.futuretek.blockentity;

import com.futuretek.FutureTekConfig;
import com.futuretek.energy.EnergyTier;
import com.futuretek.energy.EnergyUtil;
import com.futuretek.menu.GeothermalGeneratorMenu;
import com.futuretek.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Generatore geotermico: trasforma la lava in energia.
 * Si riempie con un secchio di lava (click destro) o con tubi di fluidi di altre mod.
 * Con i valori di base: 1 secchio (1000 mB) = 20.000 TU, prodotte a 24 TU/tick.
 */
public class GeothermalGeneratorBlockEntity extends MachineBlockEntity implements MenuProvider {
    public static final int CAPACITY = 24000;
    public static final int TANK_CAPACITY = 8000;

    private final FluidTank tank = new FluidTank(TANK_CAPACITY, fluid -> fluid.is(FluidTags.LAVA)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    /** TU prodotte ma non ancora "pagate" in lava */
    private int lavaDebt;

    private final ContainerData data = new MachineData() {
        @Override
        protected int getExtra(int index) {
            return index == 0 ? tank.getFluidAmount() : 0;
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public GeothermalGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GEOTHERMAL_GENERATOR.get(), pos, state, CAPACITY, 0, EnergyTier.LOW.rate);
    }

    @Override
    public void serverTick() {
        boolean running = false;
        if (tank.getFluidAmount() > 0 && !energy.isFull()) {
            int produced = energy.generate(FutureTekConfig.GEOTHERMAL_RATE.getAsInt());
            lavaDebt += produced;
            int energyPerMb = FutureTekConfig.LAVA_ENERGY_PER_MB.getAsInt();
            int mb = lavaDebt / energyPerMb;
            if (mb > 0) {
                tank.drain(mb, IFluidHandler.FluidAction.EXECUTE);
                lavaDebt -= mb * energyPerMb;
            }
            running = produced > 0;
        }
        setLit(running);
        EnergyUtil.pushToNeighbors(level, worldPosition, energy, EnergyTier.LOW.rate);
    }

    public FluidTank getTank() {
        return tank;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("LavaDebt", lavaDebt);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag.getCompound("Tank"));
        lavaDebt = tag.getInt("LavaDebt");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new GeothermalGeneratorMenu(id, inventory, worldPosition, data);
    }
}
