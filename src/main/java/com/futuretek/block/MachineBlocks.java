package com.futuretek.block;

import com.futuretek.blockentity.CombustionGeneratorBlockEntity;
import com.futuretek.blockentity.GeothermalGeneratorBlockEntity;
import com.futuretek.blockentity.SolarPanelBlockEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

/**
 * I blocchi-macchina della Fase 2. Ognuno dice solo quale "cervello" (BlockEntity) usare;
 * tutto il resto e' in MachineBlock.
 */
public final class MachineBlocks {
    private MachineBlocks() {
    }

    /** Generatore a combustione */
    public static class CombustionGenerator extends MachineBlock {
        public static final MapCodec<CombustionGenerator> CODEC = simpleCodec(CombustionGenerator::new);

        public CombustionGenerator(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new CombustionGeneratorBlockEntity(pos, state);
        }
    }

    /** Pannello solare */
    public static class SolarPanel extends MachineBlock {
        public static final MapCodec<SolarPanel> CODEC = simpleCodec(SolarPanel::new);

        public SolarPanel(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new SolarPanelBlockEntity(pos, state);
        }
    }

    /** Nucleo del reattore a fissione */
    public static class Reactor extends MachineBlock {
        public static final MapCodec<Reactor> CODEC = simpleCodec(Reactor::new);

        public Reactor(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new com.futuretek.blockentity.ReactorBlockEntity(pos, state);
        }
    }

    /** Escavatore automatico: click destro mostra lo stato */
    public static class Quarry extends MachineBlock {
        public static final MapCodec<Quarry> CODEC = simpleCodec(Quarry::new);

        public Quarry(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new com.futuretek.blockentity.QuarryBlockEntity(pos, state);
        }

        @Override
        protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                Player player, BlockHitResult hit) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof com.futuretek.blockentity.QuarryBlockEntity q) {
                var status = q.getStatus();
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "message.futuretek.quarry_status." + status.name().toLowerCase(java.util.Locale.ROOT),
                        q.getLayer() == Integer.MIN_VALUE ? "-" : String.valueOf(q.getLayer()),
                        com.futuretek.energy.EnergyUtil.format(q.getEnergy().getEnergyStored()),
                        com.futuretek.FutureTekConfig.QUARRY_ENERGY_PER_BLOCK.getAsInt(), q.width(), q.length()), false);
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    /** Acceleratore di crescita */
    public static class GrowthAccelerator extends MachineBlock {
        public GrowthAccelerator(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return simpleCodec(GrowthAccelerator::new);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new com.futuretek.farm.FarmBlockEntities.GrowthAccelerator(pos, state);
        }
    }

    /** Raccoglitore automatico */
    public static class Harvester extends MachineBlock {
        public Harvester(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return simpleCodec(Harvester::new);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new com.futuretek.farm.FarmBlockEntities.Harvester(pos, state);
        }
    }

    /** Generatore geotermico: click destro con un secchio di lava per riempirlo */
    public static class GeothermalGenerator extends MachineBlock {
        public static final MapCodec<GeothermalGenerator> CODEC = simpleCodec(GeothermalGenerator::new);

        public GeothermalGenerator(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new GeothermalGeneratorBlockEntity(pos, state);
        }

        @Override
        protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                Player player, InteractionHand hand, BlockHitResult hit) {
            if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
    }
}
