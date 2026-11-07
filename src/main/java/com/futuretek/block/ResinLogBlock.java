package com.futuretek.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Tronco dell'albero della resina.
 * Alcuni tronchi hanno una "vena di resina" (RESIN = true): con il rubinetto
 * si raccoglie la resina e la vena si svuota. Col tempo la resina si riforma da sola.
 */
public class ResinLogBlock extends RotatedPillarBlock {
    public static final MapCodec<ResinLogBlock> CODEC = simpleCodec(ResinLogBlock::new);
    public static final BooleanProperty RESIN = BooleanProperty.create("resin");

    /** Probabilita' (1 su N) che una vena vuota si riempia a ogni "tick casuale" */
    private static final int REGROW_CHANCE = 15;

    public ResinLogBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.Y).setValue(RESIN, false));
    }

    @Override
    public MapCodec<? extends RotatedPillarBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(RESIN);
    }

    /** Solo i tronchi in piedi (verticali) possono riformare la resina */
    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return !state.getValue(RESIN) && state.getValue(AXIS) == Direction.Axis.Y;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(REGROW_CHANCE) == 0) {
            level.setBlock(pos, state.setValue(RESIN, true), Block.UPDATE_ALL);
        }
    }

    // --- Il legno brucia ---
    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 5;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 5;
    }
}
