package com.futuretek.block;

import com.futuretek.blockentity.AutoCrafterBlockEntity;
import com.futuretek.logistics.ItemNetworkManager;
import com.futuretek.menu.TerminalMenu;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Blocchi della logistica che si appoggiano a un condotto: Terminale e Banco di assemblaggio. */
public final class LogisticsBlocks {
    private LogisticsBlocks() {
    }

    /** Terminale di rete: va messo accanto a un condotto */
    public static class Terminal extends BaseEntityBlock {
        public static final MapCodec<Terminal> CODEC = simpleCodec(Terminal::new);

        public Terminal(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new com.futuretek.blockentity.TerminalBlockEntity(pos, state);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                BlockHitResult hit) {
            if (!level.isClientSide) {
                if (ItemNetworkManager.getAdjacent(level, pos) == null) {
                    player.displayClientMessage(Component.translatable("message.futuretek.no_network"), true);
                } else {
                    player.openMenu(new SimpleMenuProvider((id, inv, p) -> new TerminalMenu(id, inv, pos),
                            Component.translatable(getDescriptionId())), pos);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
            super.onPlace(state, level, pos, old, moved);
            ItemNetworkManager.invalidate(level);
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
            super.onRemove(state, level, pos, newState, moved);
            ItemNetworkManager.invalidate(level);
        }
    }

    /** Banco di assemblaggio automatico: va messo accanto a un condotto */
    public static class AutoCrafter extends BaseEntityBlock {
        public static final MapCodec<AutoCrafter> CODEC = simpleCodec(AutoCrafter::new);

        public AutoCrafter(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new AutoCrafterBlockEntity(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                BlockEntityType<T> type) {
            return level.isClientSide ? null : (l, p, s, be) -> {
                if (be instanceof AutoCrafterBlockEntity crafter) {
                    crafter.serverTick();
                }
            };
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                BlockHitResult hit) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof AutoCrafterBlockEntity crafter) {
                player.openMenu(crafter, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
            super.onPlace(state, level, pos, old, moved);
            ItemNetworkManager.invalidate(level);
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
            super.onRemove(state, level, pos, newState, moved);
            ItemNetworkManager.invalidate(level);
        }
    }

    /** Nucleo logistico: va a energia e decide qual e' il baule principale (quello che lo tocca) */
    public static class Core extends MachineBlock {
        public static final MapCodec<Core> CODEC = simpleCodec(Core::new);

        public Core(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new com.futuretek.logistics.LogisticsCoreBlockEntity(pos, state);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                BlockHitResult hit) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof com.futuretek.logistics.LogisticsCoreBlockEntity core) {
                var network = ItemNetworkManager.getAdjacent(level, pos);
                var main = network == null ? null : network.mainChest();
                net.minecraft.network.chat.Component status = !core.isPowered()
                        ? net.minecraft.network.chat.Component.translatable("message.futuretek.core_unpowered")
                        : network == null ? net.minecraft.network.chat.Component.translatable("message.futuretek.core_no_pipes")
                        : main == null ? net.minecraft.network.chat.Component.translatable("message.futuretek.core_no_chest")
                        : net.minecraft.network.chat.Component.translatable("message.futuretek.core_ok",
                                network.getEndpoints().size() - 1, com.futuretek.energy.EnergyUtil.format(core.getEnergy().getEnergyStored()));
                player.displayClientMessage(status, false);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
            super.onPlace(state, level, pos, old, moved);
            ItemNetworkManager.invalidate(level);
        }

        @Override
        protected void neighborChanged(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.block.Block block,
                BlockPos from, boolean moving) {
            super.neighborChanged(state, level, pos, block, from, moving);
            ItemNetworkManager.invalidate(level); // un baule messo o tolto accanto al nucleo cambia il baule principale
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
            super.onRemove(state, level, pos, newState, moved);
            ItemNetworkManager.invalidate(level);
        }
    }
}
