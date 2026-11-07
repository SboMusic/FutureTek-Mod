package com.futuretek.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Porta logica (redstone avanzata). La freccia sul blocco indica l'USCITA (davanti).
 * Ingressi: SINISTRA e DESTRA (rispetto alla freccia) e DIETRO.
 *  AND   = sinistra E destra          OR     = almeno uno dei tre
 *  XOR   = sinistra OPPURE destra (non entrambi)   NAND = NON (sinistra E destra)
 *  NOT   = il contrario di dietro      LATCH  = memoria: sinistra accende, destra spegne
 *  TIMER = impulso ogni secondo (si ferma se dietro e' acceso)
 *  RANDOM = quando dietro si accende, uscita a caso
 */
public class LogicGateBlock extends Block {
    public enum Type { AND, OR, XOR, NAND, NOT, LATCH, TIMER, RANDOM }

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    /** Per RANDOM: ricorda se l'ingresso dietro era acceso (per accorgersi del fronte di salita) */
    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);

    private final Type type;

    public LogicGateBlock(Type type, Properties properties) {
        super(properties);
        this.type = type;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false)
                .setValue(TRIGGERED, false));
    }

    public Type getType() {
        return type;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, TRIGGERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return canSupportRigidBlock(level, pos.below());
    }

    // --- ingressi ---
    private static boolean input(Level level, BlockPos pos, Direction side) {
        return level.getSignal(pos.relative(side), side) > 0;
    }

    /** Calcola l'uscita a partire dagli ingressi (e dallo stato attuale per memoria e timer) */
    private boolean compute(BlockState state, Level level, BlockPos pos) {
        Direction front = state.getValue(FACING);
        boolean left = input(level, pos, front.getCounterClockWise());
        boolean right = input(level, pos, front.getClockWise());
        boolean back = input(level, pos, front.getOpposite());
        boolean current = state.getValue(POWERED);
        return switch (type) {
            case AND -> left && right;
            case OR -> left || right || back;
            case XOR -> left ^ right;
            case NAND -> !(left && right);
            case NOT -> !back;
            case LATCH -> left || (current && !right);
            case TIMER -> !back && !current;
            case RANDOM -> current;
        };
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
            return;
        }
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, 2);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        level.scheduleTick(pos, this, 2);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState newState = state;
        if (type == Type.RANDOM) {
            boolean back = input(level, pos, state.getValue(FACING).getOpposite());
            if (back && !state.getValue(TRIGGERED)) {
                newState = newState.setValue(POWERED, random.nextBoolean());
            }
            newState = newState.setValue(TRIGGERED, back);
        } else {
            newState = newState.setValue(POWERED, compute(state, level, pos));
        }
        if (newState != state) {
            level.setBlock(pos, newState, Block.UPDATE_ALL);
            updateFront(level, pos, newState);
        }
        if (type == Type.TIMER) {
            level.scheduleTick(pos, this, 10); // mezzo secondo acceso, mezzo spento
        }
    }

    private void updateFront(Level level, BlockPos pos, BlockState state) {
        Direction front = state.getValue(FACING);
        BlockPos target = pos.relative(front);
        level.neighborChanged(target, this, pos);
        level.updateNeighborsAtExceptFromFacing(target, this, front.getOpposite());
    }

    // --- uscita ---
    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) && direction == state.getValue(FACING).getOpposite() ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return direction != null;
    }
}
