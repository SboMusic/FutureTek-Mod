package com.futuretek.mining;

import java.util.List;

import com.futuretek.alchemy.AlchemyItem;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Le parti dell'escavatore: segnalino d'area, telaio e testa di perforazione. */
public final class QuarryParts {
    private QuarryParts() {
    }

    /**
     * Segnalino d'area: mettine uno accanto all'escavatore e un altro nell'angolo opposto
     * (alla stessa altezza, fino a 64 blocchi). L'escavatore scava il rettangolo tra i due.
     */
    public static class Marker extends Block {
        private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 10, 10);

        public Marker(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return simpleCodec(Marker::new);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
            return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 0, 0.02, 0);
            }
        }
    }

    /**
     * Telaio: barre sottili gialle e nere (come i telai classici) che si collegano da sole fra loro.
     * Lo costruisce l'escavatore e sparisce quando l'escavatore viene rotto.
     */
    public static class Frame extends Block {
        public static final java.util.Map<Direction, BooleanProperty> CONNECTIONS = PipeBlock.PROPERTY_BY_DIRECTION;
        private static final VoxelShape CORE = Block.box(6, 6, 6, 10, 10, 10);
        private static final java.util.Map<Direction, VoxelShape> ARMS = java.util.Map.of(
                Direction.NORTH, Block.box(6, 6, 0, 10, 10, 6), Direction.SOUTH, Block.box(6, 6, 10, 10, 10, 16),
                Direction.WEST, Block.box(0, 6, 6, 6, 10, 10), Direction.EAST, Block.box(10, 6, 6, 16, 10, 10),
                Direction.DOWN, Block.box(6, 0, 6, 10, 6, 10), Direction.UP, Block.box(6, 10, 6, 10, 16, 10));

        public Frame(Properties properties) {
            super(properties);
            BlockState state = stateDefinition.any();
            for (BooleanProperty property : CONNECTIONS.values()) {
                state = state.setValue(property, false);
            }
            registerDefaultState(state);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return simpleCodec(Frame::new);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            CONNECTIONS.values().forEach(builder::add);
        }

        /** Lo stato con i collegamenti verso i telai vicini */
        public static BlockState connected(BlockState state, LevelAccessor level, BlockPos pos) {
            for (Direction dir : Direction.values()) {
                state = state.setValue(CONNECTIONS.get(dir), level.getBlockState(pos.relative(dir)).getBlock() instanceof Frame);
            }
            return state;
        }

        @Override
        protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
            return state.setValue(CONNECTIONS.get(direction), neighborState.getBlock() instanceof Frame);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            VoxelShape shape = CORE;
            for (Direction dir : Direction.values()) {
                if (state.getValue(CONNECTIONS.get(dir))) {
                    shape = Shapes.or(shape, ARMS.get(dir));
                }
            }
            return shape;
        }
    }

    /** Testa di perforazione: si sposta sopra il blocco che l'escavatore sta scavando */
    public static class Drill extends Block {
        private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 16, 12);

        public Drill(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return simpleCodec(Drill::new);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, 0, 0.02, 0);
        }
    }
}
