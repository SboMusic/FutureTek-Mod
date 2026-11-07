package com.futuretek.block;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.futuretek.blockentity.ExtractorConduitBlockEntity;
import com.futuretek.blockentity.SortingConduitBlockEntity;
import com.futuretek.logistics.ItemNetworkManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * Condotto per oggetti. Tre tipi:
 *  - NORMAL: collega contenitori alla rete
 *  - EXTRACTOR (lento/veloce): tira fuori oggetti dai contenitori accanto e li manda nella rete
 *  - SORTING: i contenitori accanto ricevono SOLO gli oggetti del suo filtro (click destro per impostarlo)
 *  - ANALYZER: impara da solo cosa c'e' nel baule che tocca e gli manda solo quegli oggetti (piu' il filtro)
 */
public class ConduitBlock extends Block implements EntityBlock {
    public enum Kind { NORMAL, EXTRACTOR, FAST_EXTRACTOR, SORTING, ANALYZER }

    public static final Map<Direction, BooleanProperty> CONNECTIONS = PipeBlock.PROPERTY_BY_DIRECTION;
    private static final double MIN = 5;
    private static final double MAX = 11;

    private final Kind kind;
    private final Map<BlockState, VoxelShape> shapeCache = new ConcurrentHashMap<>();

    public ConduitBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        BlockState state = stateDefinition.any();
        for (BooleanProperty property : CONNECTIONS.values()) {
            state = state.setValue(property, false);
        }
        registerDefaultState(state);
    }

    public Kind getKind() {
        return kind;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        CONNECTIONS.values().forEach(builder::add);
    }

    private static boolean connectsTo(LevelAccessor level, BlockPos pos, Direction dir) {
        BlockPos other = pos.relative(dir);
        if (level.getBlockState(other).getBlock() instanceof ConduitBlock) {
            return true;
        }
        if (level.getBlockEntity(other) instanceof ItemNetworkManager.NetworkDevice) {
            return true;
        }
        return level instanceof Level real
                && real.getCapability(Capabilities.ItemHandler.BLOCK, other, dir.getOpposite()) != null;
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
        ItemNetworkManager.invalidate(level);
        return state.setValue(CONNECTIONS.get(direction), connectsTo(level, pos, direction));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        ItemNetworkManager.invalidate(level);
        BlockState updated = withConnections(state, level, pos);
        if (updated != state) {
            level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        super.onRemove(state, level, pos, newState, moved);
        ItemNetworkManager.invalidate(level);
    }

    // --- BlockEntity solo per estrattori e smistatori ---
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (kind) {
            case EXTRACTOR, FAST_EXTRACTOR -> new ExtractorConduitBlockEntity(pos, state);
            case SORTING -> new SortingConduitBlockEntity(pos, state);
            case ANALYZER -> new com.futuretek.logistics.AnalyzerConduitBlockEntity(pos, state);
            default -> null;
        };
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide || kind == Kind.NORMAL || kind == Kind.SORTING) {
            return null;
        }
        return (l, p, s, be) -> {
            if (be instanceof ExtractorConduitBlockEntity extractor) {
                extractor.serverTick();
            } else if (be instanceof com.futuretek.logistics.AnalyzerConduitBlockEntity analyzer) {
                analyzer.serverTick();
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (kind != Kind.SORTING && kind != Kind.ANALYZER) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SortingConduitBlockEntity sorter) {
            player.openMenu(sorter, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // --- forma ---
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeCache.computeIfAbsent(state, ConduitBlock::computeShape);
    }

    private static VoxelShape computeShape(BlockState state) {
        VoxelShape shape = Block.box(MIN, MIN, MIN, MAX, MAX, MAX);
        for (Direction dir : Direction.values()) {
            if (!state.getValue(CONNECTIONS.get(dir))) {
                continue;
            }
            shape = Shapes.or(shape, switch (dir) {
                case NORTH -> Block.box(MIN, MIN, 0, MAX, MAX, MIN);
                case SOUTH -> Block.box(MIN, MIN, MAX, MAX, MAX, 16);
                case WEST -> Block.box(0, MIN, MIN, MIN, MAX, MAX);
                case EAST -> Block.box(MAX, MIN, MIN, 16, MAX, MAX);
                case DOWN -> Block.box(MIN, 0, MIN, MAX, MIN, MAX);
                case UP -> Block.box(MIN, MAX, MIN, MAX, 16, MAX);
            });
        }
        return shape;
    }
}
