package com.futuretek.bee;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Arnia tecnica (gli alveari selvatici sono blocchi semplici: la loro loot table da' regina e fuco). */
public class BeeBlocks {
    public static class Apiary extends BaseEntityBlock {
        public static final MapCodec<Apiary> CODEC = simpleCodec(Apiary::new);

        public Apiary(Properties properties) {
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
            return new ApiaryBlockEntity(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                BlockEntityType<T> type) {
            return level.isClientSide ? null : (l, p, s, be) -> {
                if (be instanceof ApiaryBlockEntity apiary) {
                    apiary.serverTick();
                }
            };
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                BlockHitResult hit) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof ApiaryBlockEntity apiary) {
                player.openMenu(apiary, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
            if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ApiaryBlockEntity apiary) {
                apiary.dropContents();
            }
            super.onRemove(state, level, pos, newState, moved);
        }
    }
}
