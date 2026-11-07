package com.futuretek.automation;

import java.util.List;

import com.futuretek.block.MachineBlock;
import com.futuretek.blockentity.MachineBlockEntity;
import com.futuretek.energy.EnergyUtil;
import com.futuretek.energy.SidedEnergyView;
import com.futuretek.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Fase 12 - automazione della casa:
 *  - Fattoria di alberi: pianta gli alberelli in griglia e taglia gli alberi cresciuti (tronchi e foglie)
 *  - Piantatrice: zappa la terra e pianta i semi (grano, carote, patate, barbabietole, zucche, angurie)
 *  - Fertilizzatore: usa la farina d'ossa sulle piante vicine
 *  - Lampade elettriche e pannelli luminosi: fanno luce solo con l'energia; si passano l'energia fra loro
 *  - Interruttore elettrico: lascia passare l'energia solo se acceso con una leva (redstone)
 * L'area delle macchine agricole e' un quadrato 9x9 centrato sulla macchina, allo stesso livello.
 */
public final class HomeMachines {
    private HomeMachines() {
    }

    public static final int RADIUS = 4;

    // =====================================================================
    // Fattoria di alberi
    // =====================================================================
    public static class TreeFarm extends SlotMachines.Entity {
        public static final int SAPLING_SLOTS = 4;
        public static final int CUT_COST = 30;
        public static final int PLANT_COST = 20;
        private static final int SCAN_RADIUS = RADIUS + 3;
        private static final int SCAN_HEIGHT = 32;
        private int cursor;

        public TreeFarm(BlockPos pos, BlockState state) {
            super(ModBlockEntities.TREE_FARM.get(), pos, state, 20_000, 256, SAPLING_SLOTS + 12);
        }

        @Override
        protected boolean accepts(int slot, ItemStack stack) {
            return slot >= SAPLING_SLOTS || stack.is(ItemTags.SAPLINGS);
        }

        @Override
        public void serverTick() {
            boolean worked = false;
            // 1) taglia: cerca 96 posizioni a tick nel volume sopra l'area
            int side = SCAN_RADIUS * 2 + 1;
            int volume = side * side * SCAN_HEIGHT;
            for (int n = 0; n < 96 && energy.getEnergyStored() >= CUT_COST; n++) {
                int i = cursor;
                cursor = (cursor + 1) % volume;
                int y = i / (side * side);
                int rest = i % (side * side);
                BlockPos p = worldPosition.offset(rest % side - SCAN_RADIUS, y, rest / side - SCAN_RADIUS);
                BlockState s = level.getBlockState(p);
                boolean leaf = s.getBlock() instanceof LeavesBlock && !s.getValue(LeavesBlock.PERSISTENT);
                if ((s.is(BlockTags.LOGS) && naturalTree(p)) || leaf) {
                    List<ItemStack> drops = Block.getDrops(s, (ServerLevel) level, p, null);
                    if (!drops.stream().allMatch(d -> canOutput(d, SAPLING_SLOTS) || d.is(ItemTags.SAPLINGS))) {
                        status = 3; // pieno
                        break;
                    }
                    for (ItemStack d : drops) {
                        ItemStack r = d;
                        if (d.is(ItemTags.SAPLINGS)) {
                            for (int k = 0; k < SAPLING_SLOTS && !r.isEmpty(); k++) {
                                r = items.insertItem(k, r, false); // prima ricarica gli alberelli
                            }
                        }
                        output(r, SAPLING_SLOTS);
                    }
                    level.destroyBlock(p, false);
                    energy.consume(CUT_COST);
                    worked = true;
                    status = 1;
                    break; // un blocco a tick: si vede l'albero che sparisce
                }
            }
            // 2) pianta: ogni secondo, sulle posizioni della griglia (una ogni 3 blocchi)
            if (level.getGameTime() % 20 == 0) {
                for (int dx = -RADIUS + 1; dx <= RADIUS; dx += 3) {
                    for (int dz = -RADIUS + 1; dz <= RADIUS; dz += 3) {
                        BlockPos p = worldPosition.offset(dx, 0, dz);
                        if (!level.getBlockState(p).isAir() || energy.getEnergyStored() < PLANT_COST) {
                            continue;
                        }
                        for (int k = 0; k < SAPLING_SLOTS; k++) {
                            ItemStack sap = items.getStackInSlot(k);
                            if (sap.getItem() instanceof BlockItem bi) {
                                BlockState plant = bi.getBlock().defaultBlockState();
                                if (plant.canSurvive(level, p)) {
                                    level.setBlock(p, plant, Block.UPDATE_ALL);
                                    sap.shrink(1);
                                    setChanged();
                                    energy.consume(PLANT_COST);
                                    worked = true;
                                    status = 2;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (!worked && status != 3) {
                    status = energy.getEnergyStored() < CUT_COST ? 4 : 0;
                }
            }
            setLit(worked);
        }

        /** Un tronco e' di un albero vero se ha foglie naturali vicino (cosi' non taglia le case di legno) */
        private boolean naturalTree(BlockPos log) {
            for (BlockPos q : BlockPos.betweenClosed(log.offset(-3, -1, -3), log.offset(3, 4, 3))) {
                BlockState s = level.getBlockState(q);
                if (s.getBlock() instanceof LeavesBlock && !s.getValue(LeavesBlock.PERSISTENT)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putInt("Cursor", cursor);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            cursor = tag.getInt("Cursor");
        }
    }

    // =====================================================================
    // Piantatrice
    // =====================================================================
    public static class Planter extends SlotMachines.Entity {
        public static final int COST = 15;

        public Planter(BlockPos pos, BlockState state) {
            super(ModBlockEntities.PLANTER.get(), pos, state, 20_000, 128, 8);
        }

        /** Un seme si pianta se il suo blocco e' una coltura (o un gambo di zucca/anguria) */
        public static boolean isSeed(ItemStack stack) {
            return stack.getItem() instanceof BlockItem bi
                    && (bi.getBlock() instanceof CropBlock || bi.getBlock() instanceof StemBlock);
        }

        @Override
        protected boolean accepts(int slot, ItemStack stack) {
            return isSeed(stack);
        }

        @Override
        public void serverTick() {
            if (level.getGameTime() % 10 != 0) {
                return;
            }
            boolean worked = false;
            for (int dx = -RADIUS; dx <= RADIUS && !worked; dx++) {
                for (int dz = -RADIUS; dz <= RADIUS && !worked; dz++) {
                    if (energy.getEnergyStored() < COST) {
                        break;
                    }
                    BlockPos p = worldPosition.offset(dx, 0, dz);
                    if (p.equals(worldPosition) || !level.getBlockState(p).isAir()) {
                        continue;
                    }
                    BlockState soil = level.getBlockState(p.below());
                    if ((soil.is(Blocks.DIRT) || soil.is(Blocks.GRASS_BLOCK) || soil.is(Blocks.DIRT_PATH))
                            && hasSeed()) {
                        level.setBlock(p.below(), Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL); // zappa
                        energy.consume(COST);
                        worked = true;
                    } else if (soil.is(Blocks.FARMLAND)) {
                        worked = plant(p);
                    }
                }
            }
            status = worked ? 1 : !hasSeed() ? 2 : energy.getEnergyStored() < COST ? 4 : 0;
            setLit(worked);
        }

        private boolean hasSeed() {
            for (int k = 0; k < items.getSlots(); k++) {
                if (!items.getStackInSlot(k).isEmpty()) {
                    return true;
                }
            }
            return false;
        }

        private boolean plant(BlockPos p) {
            for (int k = 0; k < items.getSlots(); k++) {
                ItemStack seed = items.getStackInSlot(k);
                if (seed.getItem() instanceof BlockItem bi) {
                    BlockState crop = bi.getBlock().defaultBlockState();
                    if (crop.canSurvive(level, p)) {
                        level.setBlock(p, crop, Block.UPDATE_ALL);
                        seed.shrink(1);
                        setChanged();
                        energy.consume(COST);
                        return true;
                    }
                }
            }
            return false;
        }
    }

    // =====================================================================
    // Fertilizzatore
    // =====================================================================
    public static class Fertilizer extends SlotMachines.Entity {
        public static final int COST = 60;

        public Fertilizer(BlockPos pos, BlockState state) {
            super(ModBlockEntities.FERTILIZER.get(), pos, state, 20_000, 128, 8);
        }

        @Override
        protected boolean accepts(int slot, ItemStack stack) {
            return stack.is(Items.BONE_MEAL);
        }

        @Override
        public void serverTick() {
            if (level.getGameTime() % 10 != 0) {
                return;
            }
            boolean worked = false;
            ItemStack meal = ItemStack.EMPTY;
            for (int k = 0; k < items.getSlots() && meal.isEmpty(); k++) {
                meal = items.getStackInSlot(k);
            }
            if (!meal.isEmpty() && energy.getEnergyStored() >= COST) {
                // prova qualche posizione a caso nell'area (piante allo stesso livello o uno sopra/sotto)
                for (int tries = 0; tries < 12 && !worked; tries++) {
                    BlockPos p = worldPosition.offset(level.random.nextInt(9) - RADIUS, level.random.nextInt(3) - 1,
                            level.random.nextInt(9) - RADIUS);
                    BlockState s = level.getBlockState(p);
                    if (s.getBlock() instanceof BonemealableBlock b && !(s.getBlock() instanceof GrassBlock)
                            && !s.is(Blocks.MOSS_BLOCK) && b.isValidBonemealTarget(level, p, s)) {
                        if (BoneMealItem.applyBonemeal(meal, level, p, FakePlayerFactory.getMinecraft((ServerLevel) level))) {
                            level.levelEvent(1505, p, 15); // particelle verdi
                            setChanged();
                            energy.consume(COST);
                            worked = true;
                        }
                    }
                }
            }
            status = worked ? 1 : meal.isEmpty() ? 2 : energy.getEnergyStored() < COST ? 4 : 0;
            setLit(worked);
        }
    }

    // =====================================================================
    // Lampade
    // =====================================================================
    /** Lampada elettrica (blocco pieno) o pannello luminoso (sottile, su qualsiasi faccia) */
    public static class LampBlock extends BaseEntityBlock {
        public static final BooleanProperty LIT = BlockStateProperties.LIT;
        public static final DirectionProperty FACING = BlockStateProperties.FACING;
        private final boolean panel;

        public LampBlock(Properties properties, boolean panel) {
            super(properties);
            this.panel = panel;
            registerDefaultState(stateDefinition.any().setValue(LIT, false).setValue(FACING, Direction.UP));
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return simpleCodec(p -> new LampBlock(p, panel));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(LIT, FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getClickedFace());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            if (!panel) {
                return super.getShape(state, level, pos, context);
            }
            return switch (state.getValue(FACING)) {
                case UP -> Block.box(1, 0, 1, 15, 2, 15);
                case DOWN -> Block.box(1, 14, 1, 15, 16, 15);
                case NORTH -> Block.box(1, 1, 14, 15, 15, 16);
                case SOUTH -> Block.box(1, 1, 0, 15, 15, 2);
                case WEST -> Block.box(14, 1, 1, 16, 15, 15);
                case EAST -> Block.box(0, 1, 1, 2, 15, 15);
            };
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new LampEntity(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            return level.isClientSide ? null : (l, p, s, be) -> {
                if (be instanceof LampEntity lamp) {
                    lamp.serverTick();
                }
            };
        }
    }

    public static class LampEntity extends MachineBlockEntity {
        public static final int COST = 2;

        public LampEntity(BlockPos pos, BlockState state) {
            super(ModBlockEntities.ELECTRIC_LAMP.get(), pos, state, 400, 64, 0);
        }

        @Override
        public void serverTick() {
            boolean lit = energy.getEnergyStored() >= COST;
            if (lit) {
                energy.consume(COST);
            }
            // le lampade che si toccano si dividono l'energia: basta collegarne una al cavo
            if (level.getGameTime() % 5 == 0) {
                for (Direction dir : Direction.values()) {
                    if (level.getBlockEntity(worldPosition.relative(dir)) instanceof LampEntity other) {
                        int diff = energy.getEnergyStored() - other.energy.getEnergyStored();
                        if (diff > 1) {
                            int moved = other.energy.receiveEnergy(diff / 2, false);
                            energy.consume(moved);
                        }
                    }
                }
            }
            BlockState state = getBlockState();
            if (state.getValue(LampBlock.LIT) != lit) {
                level.setBlock(worldPosition, state.setValue(LampBlock.LIT, lit), Block.UPDATE_ALL);
            }
        }
    }

    // =====================================================================
    // Interruttore elettrico
    // =====================================================================
    /** Prende energia da tutti i lati e la manda fuori dal davanti, ma solo se riceve un segnale redstone (leva) */
    public static class SwitchBlock extends MachineBlock {
        public SwitchBlock(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return simpleCodec(SwitchBlock::new);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new SwitchEntity(pos, state);
        }
    }

    public static class SwitchEntity extends MachineBlockEntity {
        public static final int RATE = 2048;

        public SwitchEntity(BlockPos pos, BlockState state) {
            super(ModBlockEntities.ENERGY_SWITCH.get(), pos, state, 4096, RATE, RATE);
        }

        private boolean on() {
            return level != null && level.hasNeighborSignal(worldPosition);
        }

        @Override
        public IEnergyStorage getEnergyHandler(Direction side) {
            if (side == null) {
                return energy;
            }
            if (side == getFacing()) {
                return new SidedEnergyView(energy, false, false);
            }
            return new SidedEnergyView(energy, on(), false) {
                @Override
                public int receiveEnergy(int toReceive, boolean simulate) {
                    return on() ? super.receiveEnergy(toReceive, simulate) : 0;
                }
            };
        }

        @Override
        public void serverTick() {
            boolean on = on();
            if (on) {
                EnergyUtil.pushToNeighbors(level, worldPosition, energy, RATE, getFacing());
            }
            setLit(on);
        }
    }
}
