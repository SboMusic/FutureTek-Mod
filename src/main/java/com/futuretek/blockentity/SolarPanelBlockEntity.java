package com.futuretek.blockentity;

import com.futuretek.FutureTekConfig;
import com.futuretek.energy.EnergyTier;
import com.futuretek.energy.EnergyUtil;
import com.futuretek.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pannello solare: produce energia di giorno se sopra c'e' il cielo libero.
 * Con la pioggia produce la meta'. Non ha interfaccia: click destro mostra l'energia.
 */
public class SolarPanelBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 1000;

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_PANEL.get(), pos, state, CAPACITY, 0, EnergyTier.LOW.rate);
    }

    @Override
    public void serverTick() {
        boolean producing = level.isDay() && level.canSeeSky(worldPosition.above());
        if (producing) {
            int rate = FutureTekConfig.SOLAR_RATE.getAsInt();
            energy.generate(level.isRaining() ? Math.max(1, rate / 2) : rate);
        }
        setLit(producing);
        EnergyUtil.pushToNeighbors(level, worldPosition, energy, EnergyTier.LOW.rate);
    }
}
