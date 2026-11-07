package com.futuretek.block;

import com.futuretek.blockentity.BatteryBoxBlockEntity;
import com.futuretek.energy.EnergyTier;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Banco accumulatore. Livello e capacita' sono decisi al momento della registrazione
 * (I = 40.000 TU, II = 600.000 TU, III = 10.000.000 TU).
 */
public class BatteryBoxBlock extends MachineBlock {
    private final EnergyTier tier;
    private final int capacity;

    public BatteryBoxBlock(EnergyTier tier, int capacity, Properties properties) {
        super(properties);
        this.tier = tier;
        this.capacity = capacity;
    }

    public EnergyTier getTier() {
        return tier;
    }

    public int getCapacity() {
        return capacity;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        // Il codec serve solo a strumenti interni di Minecraft; usiamo il livello piu' basso come riferimento.
        return simpleCodec(props -> new BatteryBoxBlock(EnergyTier.LOW, 40_000, props));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BatteryBoxBlockEntity(pos, state);
    }
}
