package com.futuretek.block;

import com.futuretek.blockentity.ReplicatorBlockEntity;
import com.futuretek.blockentity.StarCollectorBlockEntity;
import com.futuretek.energy.EnergyUtil;
import com.futuretek.item.EssenceCrystalItem;
import com.futuretek.menu.SynthesisTableMenu;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** I blocchi della trasmutazione (Fase 4). */
public final class EssenceBlocks {
    private EssenceBlocks() {
    }

    /** Tavolo di Sintesi: scioglie oggetti in Essenza e crea quelli conosciuti */
    public static class SynthesisTable extends Block {
        public SynthesisTable(Properties properties) {
            super(properties);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                BlockHitResult hit) {
            if (!level.isClientSide) {
                player.openMenu(new SimpleMenuProvider(
                        (id, inventory, p) -> new SynthesisTableMenu(id, inventory, ContainerLevelAccess.create(level, pos), false),
                        Component.translatable(getDescriptionId())));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    /** Replicatore */
    public static class Replicator extends BaseEntityBlock {
        public static final MapCodec<Replicator> CODEC = simpleCodec(Replicator::new);

        public Replicator(Properties properties) {
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
            return new ReplicatorBlockEntity(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                BlockEntityType<T> type) {
            return level.isClientSide ? null : (l, p, s, be) -> {
                if (be instanceof ReplicatorBlockEntity replicator) {
                    replicator.serverTick();
                }
            };
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                BlockHitResult hit) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof ReplicatorBlockEntity replicator) {
                player.openMenu(replicator, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
            if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ReplicatorBlockEntity replicator) {
                replicator.dropContents();
            }
            super.onRemove(state, level, pos, newState, moved);
        }
    }

    /** Usato dal Captatore: click destro con un cristallo lo ricarica */
    static ItemInteractionResult chargeCrystal(ItemStack stack, Level level, BlockPos pos, Player player) {
        if (!(stack.getItem() instanceof EssenceCrystalItem crystal)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof StarCollectorBlockEntity collector) {
            long space = crystal.getCapacity() - EssenceCrystalItem.getStored(stack);
            long moved = crystal.fill(stack, collector.take(space));
            player.displayClientMessage(Component.translatable("message.futuretek.crystal_charged",
                    EnergyUtil.format(moved), EnergyUtil.format(EssenceCrystalItem.getStored(stack))), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    static InteractionResult showCollector(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof StarCollectorBlockEntity collector) {
            player.displayClientMessage(Component.translatable("message.futuretek.collector",
                    EnergyUtil.format(collector.getStored()), EnergyUtil.format(collector.getCapacity())), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
