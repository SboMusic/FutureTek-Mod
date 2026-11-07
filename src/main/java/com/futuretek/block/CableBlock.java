package com.futuretek.block;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.futuretek.energy.EnergyTier;
import com.futuretek.energy.network.EnergyNetworkManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * Cavo elettrico. Si collega da solo agli altri cavi e ai blocchi che hanno energia.
 * Le proprieta' north/south/... servono solo per il disegno: il trasporto vero
 * lo fa la rete (EnergyNetwork), che guarda direttamente i blocchi vicini.
 */
public class CableBlock extends Block {
    public static final Map<Direction, BooleanProperty> CONNECTIONS = PipeBlock.PROPERTY_BY_DIRECTION;

    private final EnergyTier tier;
    /** Spessore del cavo in pixel (su 16) */
    private final int thickness;
    private final Map<BlockState, VoxelShape> shapeCache = new ConcurrentHashMap<>();

    public CableBlock(EnergyTier tier, int thickness, Properties properties) {
        super(properties);
        this.tier = tier;
        this.thickness = thickness;
        BlockState state = stateDefinition.any();
        for (BooleanProperty property : CONNECTIONS.values()) {
            state = state.setValue(property, false);
        }
        registerDefaultState(state);
    }

    public EnergyTier getTier() {
        return tier;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        CONNECTIONS.values().forEach(builder::add);
    }

    // --- Collegamenti ---
    private static boolean connectsTo(LevelAccessor level, BlockPos pos, Direction dir) {
        BlockPos other = pos.relative(dir);
        if (level.getBlockState(other).getBlock() instanceof CableBlock) {
            return true;
        }
        return level instanceof Level realLevel
                && realLevel.getCapability(Capabilities.EnergyStorage.BLOCK, other, dir.getOpposite()) != null;
    }

    private static BlockState withConnections(BlockState state, LevelAccessor level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            state = state.setValue(CONNECTIONS.get(dir), connectsTo(level, pos, dir));
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnections(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        EnergyNetworkManager.invalidate(level);
        return state.setValue(CONNECTIONS.get(direction), connectsTo(level, pos, direction));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        EnergyNetworkManager.invalidate(level);
        // aggiorna i collegamenti anche se il cavo e' stato piazzato da un comando
        BlockState updated = withConnections(state, level, pos);
        if (updated != state) {
            level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        EnergyNetworkManager.invalidate(level);
    }

    // --- Forma (per colpire e attraversare il cavo) ---
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeCache.computeIfAbsent(state, this::computeShape);
    }

    private VoxelShape computeShape(BlockState state) {
        double min = (16 - thickness) / 2.0;
        double max = 16 - min;
        VoxelShape shape = Block.box(min, min, min, max, max, max);
        for (Direction dir : Direction.values()) {
            if (!state.getValue(CONNECTIONS.get(dir))) {
                continue;
            }
            VoxelShape arm = switch (dir) {
                case NORTH -> Block.box(min, min, 0, max, max, min);
                case SOUTH -> Block.box(min, min, max, max, max, 16);
                case WEST -> Block.box(0, min, min, min, max, max);
                case EAST -> Block.box(max, min, min, 16, max, max);
                case DOWN -> Block.box(min, 0, min, max, min, max);
                case UP -> Block.box(min, max, min, max, 16, max);
            };
            shape = Shapes.or(shape, arm);
        }
        return shape;
    }
}
