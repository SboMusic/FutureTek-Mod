package com.futuretek.rail;

import javax.annotation.Nullable;

import com.futuretek.blockentity.InductionRailBlockEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

/** I binari di FutureTek (Fase 7). */
public final class RailBlocks {
    private RailBlocks() {
    }

    /** Binario rinforzato: velocita' massima doppia rispetto a un binario normale */
    public static class Reinforced extends RailBlock {
        public Reinforced(Properties properties) {
            super(properties);
        }

        @Override
        public float getRailMaxSpeed(BlockState state, Level level, BlockPos pos, AbstractMinecart cart) {
            return 0.8F;
        }
    }

    /**
     * Binario a induzione: accelera i carrelli usando energia (20 TU per passaggio), non la redstone.
     * Si "accende" da solo quando ha energia (va collegato a un cavo).
     */
    public static class Induction extends PoweredRailBlock implements EntityBlock {
        public static final int COST = 20;

        public Induction(Properties properties) {
            super(properties, true);
        }

        @Override
        protected void updateState(BlockState state, Level level, BlockPos pos, Block block) {
            // lo stato "acceso" lo decide l'energia (InductionRailBlockEntity), non la redstone
        }

        @Override
        public float getRailMaxSpeed(BlockState state, Level level, BlockPos pos, AbstractMinecart cart) {
            return 1.0F;
        }

        @Override
        public void onMinecartPass(BlockState state, Level level, BlockPos pos, AbstractMinecart cart) {
            if (!level.isClientSide && state.getValue(POWERED)
                    && level.getBlockEntity(pos) instanceof InductionRailBlockEntity rail && rail.consume(COST)) {
                Vec3 motion = cart.getDeltaMovement();
                double speed = motion.horizontalDistance();
                if (speed > 0.01) {
                    cart.setDeltaMovement(motion.add(motion.x / speed * 0.08, 0, motion.z / speed * 0.08));
                }
            }
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new InductionRailBlockEntity(pos, state);
        }

        @Nullable
        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                BlockEntityType<T> type) {
            return level.isClientSide ? null : (l, p, s, be) -> {
                if (be instanceof InductionRailBlockEntity rail) {
                    rail.serverTick();
                }
            };
        }

        @Override
        public MapCodec<PoweredRailBlock> codec() {
            return simpleCodec(Induction::new);
        }
    }

    /**
     * Binario di blocco: senza redstone FERMA il carrello e lo tiene li'; con la redstone lo lancia
     * come un binario alimentato.
     */
    public static class Holding extends PoweredRailBlock {
        public Holding(Properties properties) {
            super(properties, true);
        }

        @Override
        public void onMinecartPass(BlockState state, Level level, BlockPos pos, AbstractMinecart cart) {
            if (!state.getValue(POWERED)) {
                cart.setDeltaMovement(Vec3.ZERO);
                cart.setPos(pos.getX() + 0.5, cart.getY(), pos.getZ() + 0.5);
            }
        }

        @Override
        public MapCodec<PoweredRailBlock> codec() {
            return simpleCodec(Holding::new);
        }
    }

    /**
     * Binario di carico / scarico: trasferisce oggetti tra il contenitore SOTTO il binario e un carrello
     * con inventario. Tiene fermo il carrello finche' c'e' qualcosa da spostare, poi lo lascia ripartire.
     */
    public static class Transfer extends RailBlock {
        private final boolean load;

        public Transfer(boolean load, Properties properties) {
            super(properties);
            this.load = load;
        }

        @Override
        public void onMinecartPass(BlockState state, Level level, BlockPos pos, AbstractMinecart cart) {
            if (level.isClientSide || !(cart instanceof Container container)) {
                return;
            }
            IItemHandler below = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.below(), Direction.UP);
            if (below == null) {
                return;
            }
            IItemHandler cartItems = new InvWrapper(container);
            IItemHandler from = load ? below : cartItems;
            IItemHandler to = load ? cartItems : below;
            int moved = 0;
            for (int slot = 0; slot < from.getSlots() && moved < 16; slot++) {
                ItemStack available = from.extractItem(slot, 16 - moved, true);
                if (available.isEmpty()) {
                    continue;
                }
                ItemStack rest = ItemHandlerHelper.insertItemStacked(to, available, true);
                int amount = available.getCount() - rest.getCount();
                if (amount > 0) {
                    ItemHandlerHelper.insertItemStacked(to, from.extractItem(slot, amount, false), false);
                    moved += amount;
                }
            }
            if (moved > 0) {
                // ancora lavoro: tieni fermo il carrello sul binario
                cart.setDeltaMovement(Vec3.ZERO);
                cart.setPos(pos.getX() + 0.5, cart.getY(), pos.getZ() + 0.5);
            } else if (cart.getDeltaMovement().lengthSqr() < 0.001) {
                // finito: riparte nella direzione in cui stava andando
                Direction dir = cart.getMotionDirection();
                cart.setDeltaMovement(dir.getStepX() * 0.4, 0, dir.getStepZ() * 0.4);
            }
        }
    }
}
