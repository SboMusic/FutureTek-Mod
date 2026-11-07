package com.futuretek.utility;

import com.futuretek.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fornace di ferro: funziona come la fornace di Minecraft (stesse ricette, stessa interfaccia, tramogge)
 * ma e' 1,5 volte piu' veloce: ogni 2 tick di gioco ne fa 3. Il combustibile dura lo stesso numero di oggetti.
 */
public final class IronFurnace {
    private IronFurnace() {
    }

    public static class Block extends AbstractFurnaceBlock {
        public static final MapCodec<Block> CODEC = simpleCodec(Block::new);

        public Block(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends AbstractFurnaceBlock> codec() {
            return CODEC;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new Entity(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            if (level.isClientSide || type != ModBlockEntities.IRON_FURNACE.get()) {
                return null;
            }
            return (l, pos, s, be) -> {
                AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) be;
                AbstractFurnaceBlockEntity.serverTick(l, pos, l.getBlockState(pos), furnace);
                if (l.getGameTime() % 2 == 0 && !furnace.isRemoved()) {
                    AbstractFurnaceBlockEntity.serverTick(l, pos, l.getBlockState(pos), furnace); // il tick in piu'
                }
            };
        }

        @Override
        protected void openContainer(Level level, BlockPos pos, Player player) {
            if (level.getBlockEntity(pos) instanceof Entity furnace) {
                player.openMenu(furnace);
            }
        }
    }

    public static class Entity extends AbstractFurnaceBlockEntity {
        public Entity(BlockPos pos, BlockState state) {
            super(ModBlockEntities.IRON_FURNACE.get(), pos, state, RecipeType.SMELTING);
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("block.futuretek.iron_furnace");
        }

        @Override
        protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
            return new FurnaceMenu(id, inventory, this, this.dataAccess);
        }
    }
}
