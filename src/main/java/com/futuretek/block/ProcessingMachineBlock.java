package com.futuretek.block;

import com.futuretek.blockentity.ProcessingMachineBlockEntity;
import com.futuretek.machine.ProcessingMachine;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Blocco di una macchina di lavorazione: il tipo (frantumatore, forno...) e' deciso alla registrazione. */
public class ProcessingMachineBlock extends MachineBlock {
    private final ProcessingMachine machine;

    public ProcessingMachineBlock(ProcessingMachine machine, Properties properties) {
        super(properties);
        this.machine = machine;
    }

    public ProcessingMachine getMachine() {
        return machine;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(props -> new ProcessingMachineBlock(ProcessingMachine.CRUSHER, props));
    }

    /** Click destro con un secchio d'acqua: riempie il serbatoio (solo macchine che usano acqua) */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (machine.usesWater() && FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ProcessingMachineBlockEntity(pos, state);
    }
}
