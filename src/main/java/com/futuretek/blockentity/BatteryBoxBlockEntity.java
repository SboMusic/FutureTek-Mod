package com.futuretek.blockentity;

import javax.annotation.Nullable;

import com.futuretek.block.BatteryBoxBlock;
import com.futuretek.energy.EnergyTier;
import com.futuretek.energy.EnergyUtil;
import com.futuretek.energy.SidedEnergyView;
import com.futuretek.menu.BatteryBoxMenu;
import com.futuretek.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Banco accumulatore: conserva energia.
 * - Riceve energia da 5 lati, la rilascia SOLO dal lato frontale (quello con il simbolo).
 * - Slot in alto: ricarica una cella portatile. Slot in basso: scarica una cella nel banco.
 */
public class BatteryBoxBlockEntity extends MachineBlockEntity implements MenuProvider {
    public static final int SLOT_CHARGE = 0;
    public static final int SLOT_DISCHARGE = 1;

    private final EnergyTier tier;

    private final ItemStackHandler items = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getCapability(Capabilities.EnergyStorage.ITEM) != null;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final ContainerData data = new MachineData() {
        @Override
        protected int getExtra(int index) {
            return tier.ordinal();
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public BatteryBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTERY_BOX.get(), pos, state,
                ((BatteryBoxBlock) state.getBlock()).getCapacity(),
                ((BatteryBoxBlock) state.getBlock()).getTier().rate,
                ((BatteryBoxBlock) state.getBlock()).getTier().rate);
        this.tier = ((BatteryBoxBlock) state.getBlock()).getTier();
    }

    @Override
    public void serverTick() {
        // ricarica la cella nello slot in alto
        ItemStack toCharge = items.getStackInSlot(SLOT_CHARGE);
        IEnergyStorage chargeTarget = toCharge.getCapability(Capabilities.EnergyStorage.ITEM);
        if (chargeTarget != null && energy.getEnergyStored() > 0) {
            int moved = chargeTarget.receiveEnergy(Math.min(tier.rate, energy.getEnergyStored()), false);
            energy.consume(moved);
        }
        // scarica la cella nello slot in basso
        ItemStack toDischarge = items.getStackInSlot(SLOT_DISCHARGE);
        IEnergyStorage dischargeSource = toDischarge.getCapability(Capabilities.EnergyStorage.ITEM);
        if (dischargeSource != null && !energy.isFull()) {
            int moved = dischargeSource.extractEnergy(Math.min(tier.rate, energy.getSpace()), false);
            energy.generate(moved);
        }
        // uscita dal lato frontale
        EnergyUtil.pushToNeighbors(level, worldPosition, energy, tier.rate, getFacing());
    }

    @Override
    public IEnergyStorage getEnergyHandler(@Nullable Direction side) {
        if (side == null) {
            return energy;
        }
        boolean front = side == getFacing();
        return new SidedEnergyView(energy, !front, front);
    }

    public EnergyTier getTier() {
        return tier;
    }

    @Override
    public ItemStackHandler getItems() {
        return items;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BatteryBoxMenu(id, inventory, worldPosition, items, data);
    }
}
