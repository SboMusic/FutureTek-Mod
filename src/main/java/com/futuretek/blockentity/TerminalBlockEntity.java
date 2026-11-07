package com.futuretek.blockentity;

import com.futuretek.logistics.ItemNetworkManager;
import com.futuretek.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Il terminale non conserva dati: il BlockEntity serve solo a farsi riconoscere dalla rete. */
public class TerminalBlockEntity extends BlockEntity implements ItemNetworkManager.NetworkDevice {
    public TerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TERMINAL.get(), pos, state);
    }
}
