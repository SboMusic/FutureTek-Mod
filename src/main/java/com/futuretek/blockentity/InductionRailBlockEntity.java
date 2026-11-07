package com.futuretek.blockentity;

import com.futuretek.energy.EnergyTier;
import com.futuretek.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Energia del binario a induzione: si accende (POWERED) quando ha abbastanza energia per un passaggio. */
public class InductionRailBlockEntity extends MachineBlockEntity {
    public InductionRailBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INDUCTION_RAIL.get(), pos, state, 2000, EnergyTier.MEDIUM.rate, 0);
    }

    public boolean consume(int amount) {
        return energy.consume(amount) == amount;
    }

    @Override
    public void serverTick() {
        boolean powered = energy.getEnergyStored() >= 20;
        BlockState state = getBlockState();
        if (state.getValue(PoweredRailBlock.POWERED) != powered) {
            level.setBlock(worldPosition, state.setValue(PoweredRailBlock.POWERED, powered), 3);
        }
    }
}
