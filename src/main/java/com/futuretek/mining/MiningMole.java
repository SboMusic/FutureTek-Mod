package com.futuretek.mining;

import java.util.List;

import javax.annotation.Nullable;

import com.futuretek.registry.ModBlockEntities;
import com.futuretek.registry.ModMenus;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Talpa mineraria: un piccolo scavatore che si MUOVE da solo (come una tartaruga), blocco dopo blocco.
 * 1. Mettila accanto a un baule (il suo "baule di casa") e metti del combustibile nello slot o nel baule.
 * 2. Apri l'interfaccia, scegli il raggio (1 = 3x3 ... 8 = 17x17) e premi Avvia.
 * 3. Scava sotto di se' uno strato alla volta, fino in fondo al mondo. Quando e' piena o ha poco carburante
 *    torna a casa, svuota il carico nel baule, prende combustibile dal baule e riparte da dove era rimasta.
 * Ogni movimento consuma carburante: 1 carbone = 80 blocchi (carbonella, tronchi, braci... vanno bene tutti).
 * Non rompe bauli, macchine e blocchi indistruttibili: li salta.
 */
public final class MiningMole {
    private MiningMole() {
    }

    public static final int STORAGE = 18;
    public static final int FUEL_SLOT = STORAGE;
    public static final int MAX_RADIUS = 8;
    public static final int STEP_TICKS = 8;
    public static final int FUEL_DIVISOR = 20;
    public static final int MAX_FUEL = 4000;

    public enum State { IDLE, MINING, RETURNING, AT_HOME, RESUMING, DONE, NO_CHEST, STUCK }

    // =====================================================================
    public static class MoleBlock extends BaseEntityBlock {
        public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
        public static final BooleanProperty LIT = BlockStateProperties.LIT;

        public MoleBlock(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return simpleCodec(MoleBlock::new);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, LIT);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new MoleEntity(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            return level.isClientSide ? null : (l, p, s, be) -> {
                if (be instanceof MoleEntity mole) {
                    mole.serverTick();
                }
            };
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                BlockHitResult hit) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof MoleEntity mole) {
                player.openMenu(mole, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
            if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MoleEntity mole && !mole.moving) {
                for (int i = 0; i < mole.items.getSlots(); i++) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), mole.items.getStackInSlot(i));
                }
            }
            super.onRemove(state, level, pos, newState, moved);
        }
    }

    // =====================================================================
    public static class MoleEntity extends BlockEntity implements MenuProvider {
        final ItemStackHandler items = new ItemStackHandler(STORAGE + 1) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot != FUEL_SLOT || stack.getBurnTime(RecipeType.SMELTING) > 0;
            }

            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
        int radius = 2;
        int fuel;
        State state = State.IDLE;
        @Nullable BlockPos home;      // dove la talpa e' partita (accanto al baule)
        @Nullable BlockPos chest;     // il baule di casa
        Direction away = Direction.NORTH; // verso cui si allarga l'area (lontano dal baule)
        int layer;                    // y dello strato che sta scavando
        int cursor;                   // indice della cella nello strato
        @Nullable BlockPos resume;    // dove riprendere dopo essere tornata a casa
        int mined;
        boolean moving;
        private int wait;

        private final ContainerData data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> radius;
                    case 1 -> state.ordinal();
                    case 2 -> Math.min(fuel, Short.MAX_VALUE);
                    case 3 -> home == null ? 0 : home.getY() - layer + 1;
                    case 4 -> Math.min(mined, Short.MAX_VALUE);
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 5;
            }
        };

        public MoleEntity(BlockPos pos, BlockState state) {
            super(ModBlockEntities.MINING_MOLE.get(), pos, state);
        }

        public ItemStackHandler getItems() {
            return items;
        }

        public State getState() {
            return state;
        }

        public int getRadius() {
            return radius;
        }

        public void setRadius(int value) {
            if (state == State.IDLE || state == State.DONE || state == State.NO_CHEST) {
                radius = Math.max(1, Math.min(MAX_RADIUS, value));
                setChanged();
            }
        }

        public int getMined() {
            return mined;
        }

        /** Avvia (o ferma) lo scavo */
        public void toggle() {
            if (state == State.IDLE || state == State.DONE || state == State.NO_CHEST || state == State.STUCK) {
                home = worldPosition;
                chest = findChest(worldPosition);
                if (chest == null) {
                    state = State.NO_CHEST;
                } else {
                    away = awayFromChest();
                    layer = worldPosition.getY();
                    cursor = 0;
                    resume = null;
                    mined = 0;
                    state = State.MINING;
                }
            } else {
                state = State.RETURNING; // "Ferma": torna a casa
                resume = null;
            }
            setChanged();
        }

        @Nullable
        private BlockPos findChest(BlockPos around) {
            for (Direction dir : Direction.values()) {
                BlockPos p = around.relative(dir);
                if (!(level.getBlockEntity(p) instanceof MoleEntity)
                        && level.getCapability(Capabilities.ItemHandler.BLOCK, p, dir.getOpposite()) != null) {
                    return p.immutable();
                }
            }
            return null;
        }

        /**
         * L'area parte dalla talpa e si allarga dalla parte opposta al baule:
         * con raggio 1 e il baule a sud, la talpa e' al centro della prima fila di un 3x3 che va verso nord.
         */
        private Direction awayFromChest() {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                if (worldPosition.relative(dir).equals(chest)) {
                    return dir.getOpposite();
                }
            }
            BlockState bs = getBlockState();
            return bs.hasProperty(MoleBlock.FACING) ? bs.getValue(MoleBlock.FACING) : Direction.NORTH;
        }

        // --- l'area da scavare: celle dello strato a serpentina, uno strato si' e uno no al contrario ---
        private int side() {
            return radius * 2 + 1;
        }

        private BlockPos cell(int index) {
            int s = side();
            int depth = home.getY() - layer;
            int i = depth % 2 == 0 ? index : s * s - 1 - index;
            int row = i / s;
            int col = row % 2 == 0 ? i % s : s - 1 - i % s;
            Direction side = away.getClockWise();
            BlockPos p = home.relative(away, row).relative(side, col - radius);
            return new BlockPos(p.getX(), layer, p.getZ());
        }

        private int distanceHome() {
            return home == null ? 0 : Math.abs(worldPosition.getX() - home.getX()) + Math.abs(worldPosition.getY() - home.getY())
                    + Math.abs(worldPosition.getZ() - home.getZ());
        }

        private boolean full() {
            for (int i = 0; i < STORAGE; i++) {
                if (items.getStackInSlot(i).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        /** Prende carburante dallo slot apposito */
        private void refuel() {
            while (fuel < MAX_FUEL - 400) {
                ItemStack f = items.getStackInSlot(FUEL_SLOT);
                int burn = f.getBurnTime(RecipeType.SMELTING);
                if (f.isEmpty() || burn <= 0) {
                    return;
                }
                ItemStack rest = f.getCraftingRemainingItem();
                items.extractItem(FUEL_SLOT, 1, false);
                if (!rest.isEmpty() && items.getStackInSlot(FUEL_SLOT).isEmpty()) {
                    items.setStackInSlot(FUEL_SLOT, rest);
                }
                fuel += Math.max(1, burn / FUEL_DIVISOR);
            }
        }

        void serverTick() {
            if (state == State.IDLE || state == State.DONE || state == State.NO_CHEST || state == State.STUCK || home == null) {
                setLit(false);
                return;
            }
            if (++wait < STEP_TICKS) {
                return;
            }
            wait = 0;
            if (com.futuretek.FutureTekConfig.DEBUG_LOGGING.getAsBoolean() && (level.getGameTime() / STEP_TICKS) % 5 == 0) {
                com.futuretek.FutureTek.LOGGER.info("Talpa {} stato {} carburante {} strato {} cella {} scavati {}",
                        worldPosition, state, fuel, layer, cursor, mined);
            }
            setLit(true);
            refuel();
            switch (state) {
                case MINING -> mineStep();
                case RETURNING -> travel(home, State.AT_HOME);
                case RESUMING -> {
                    if (resume == null) {
                        state = State.MINING;
                    } else {
                        travel(resume, State.MINING);
                    }
                }
                case AT_HOME -> atHome();
                default -> {
                }
            }
        }

        private void setLit(boolean lit) {
            BlockState bs = getBlockState();
            if (bs.hasProperty(MoleBlock.LIT) && bs.getValue(MoleBlock.LIT) != lit) {
                level.setBlock(worldPosition, bs.setValue(MoleBlock.LIT, lit), Block.UPDATE_CLIENTS);
            }
        }

        private void mineStep() {
            if (full() || fuel <= distanceHome() + 4) {
                resume = worldPosition.immutable();
                state = State.RETURNING;
                return;
            }
            int cells = side() * side();
            while (cursor < cells) {
                BlockPos target = cell(cursor);
                if (target.equals(worldPosition) || nothingToMine(target)) {
                    cursor++; // aria o liquido: niente da scavare, non serve andarci
                    continue;
                }
                if (!canEnter(target)) {
                    cursor++; // bloccata (roccia madre, baule...): la salta
                    continue;
                }
                if (!isAdjacent(target)) {
                    // si avvicina di un passo (prima in orizzontale, poi in verticale)
                    BlockPos step = stepToward(target);
                    if (step == null || !canEnter(step)) {
                        cursor++;
                        continue;
                    }
                    moveTo(step);
                    return;
                }
                moveTo(target);
                return;
            }
            // strato finito: si scende
            cursor = 0;
            layer--;
            if (layer < level.getMinBuildHeight()) {
                resume = null;
                state = State.RETURNING;
                mined = -mined - 1; // segna "finito" (vedi atHome)
            }
        }

        private boolean nothingToMine(BlockPos target) {
            BlockState bs = level.getBlockState(target);
            return bs.isAir() || bs.getBlock() instanceof LiquidBlock;
        }

        @Nullable
        private BlockPos stepToward(BlockPos target) {
            int x = worldPosition.getX();
            int z = worldPosition.getZ();
            if (x != target.getX()) {
                return worldPosition.relative(target.getX() > x ? Direction.EAST : Direction.WEST);
            }
            if (z != target.getZ()) {
                return worldPosition.relative(target.getZ() > z ? Direction.SOUTH : Direction.NORTH);
            }
            if (worldPosition.getY() != target.getY()) {
                return target.getY() < worldPosition.getY() ? worldPosition.below() : worldPosition.above();
            }
            return null;
        }

        private boolean isAdjacent(BlockPos target) {
            return worldPosition.distManhattan(target) == 1;
        }

        private boolean canEnter(BlockPos target) {
            if (!level.isLoaded(target) || !level.isInWorldBounds(target)) {
                return false;
            }
            BlockState bs = level.getBlockState(target);
            if (bs.isAir() || bs.getBlock() instanceof LiquidBlock || bs.canBeReplaced()) {
                return true;
            }
            return bs.getDestroySpeed(level, target) >= 0 && !bs.hasBlockEntity();
        }

        /** Viaggia verso "target" passando per gli strati gia' scavati */
        private void travel(BlockPos target, State arrived) {
            if (worldPosition.equals(target)) {
                state = arrived;
                return;
            }
            if (fuel <= 0) {
                return; // senza carburante resta ferma (rimetti combustibile nello slot)
            }
            int travelY = home.getY();
            BlockPos next;
            int x = worldPosition.getX();
            int y = worldPosition.getY();
            int z = worldPosition.getZ();
            boolean sameColumn = x == target.getX() && z == target.getZ();
            if (!sameColumn && y < travelY) {
                next = worldPosition.above();
            } else if (!sameColumn && y > travelY) {
                next = worldPosition.below();
            } else if (x != target.getX()) {
                next = worldPosition.relative(target.getX() > x ? Direction.EAST : Direction.WEST);
            } else if (z != target.getZ()) {
                next = worldPosition.relative(target.getZ() > z ? Direction.SOUTH : Direction.NORTH);
            } else {
                next = target.getY() > y ? worldPosition.above() : worldPosition.below();
            }
            if (!canEnter(next)) {
                state = State.STUCK;
                return;
            }
            moveTo(next);
        }

        private void atHome() {
            IItemHandler box = chest == null ? null : level.getCapability(Capabilities.ItemHandler.BLOCK, chest, null);
            if (box == null) {
                state = State.NO_CHEST;
                return;
            }
            // 1) scarica tutto
            boolean emptied = true;
            for (int i = 0; i < STORAGE; i++) {
                ItemStack s = items.getStackInSlot(i);
                if (!s.isEmpty()) {
                    ItemStack rest = ItemHandlerHelper.insertItemStacked(box, s, false);
                    items.setStackInSlot(i, rest);
                    emptied &= rest.isEmpty();
                }
            }
            // 2) prende combustibile dal baule
            for (int i = 0; i < box.getSlots(); i++) {
                ItemStack s = box.getStackInSlot(i);
                if (s.getBurnTime(RecipeType.SMELTING) > 0 && !s.is(Items.LAVA_BUCKET)) {
                    ItemStack taken = box.extractItem(i, 64, true);
                    ItemStack rest = items.insertItem(FUEL_SLOT, taken, true);
                    int n = taken.getCount() - rest.getCount();
                    if (n > 0) {
                        items.insertItem(FUEL_SLOT, box.extractItem(i, n, false), false);
                    }
                }
            }
            refuel();
            if (mined < 0) { // aveva finito
                mined = -mined - 1;
                state = State.DONE;
                return;
            }
            if (resume == null && layer >= home.getY() && cursor == 0 && mined > 0) {
                state = State.IDLE; // fermata dal giocatore
                return;
            }
            if (resume == null) {
                state = State.IDLE;
                return;
            }
            if (emptied && fuel > 64) {
                state = State.RESUMING; // riparte da dove era rimasta
            }
        }

        /** Rompe il blocco in "target" (raccogliendo cio' che lascia) e ci si sposta dentro */
        private void moveTo(BlockPos target) {
            if (!(level instanceof ServerLevel server)) {
                return;
            }
            BlockState bs = level.getBlockState(target);
            if (!bs.isAir() && !(bs.getBlock() instanceof LiquidBlock) && !bs.canBeReplaced()) {
                List<ItemStack> drops = Block.getDrops(bs, server, target, null, null, new ItemStack(Items.DIAMOND_PICKAXE));
                for (ItemStack drop : drops) {
                    ItemStack rest = drop;
                    for (int i = 0; i < STORAGE && !rest.isEmpty(); i++) {
                        rest = items.insertItem(i, rest, false);
                    }
                    if (!rest.isEmpty()) {
                        Block.popResource(level, worldPosition, rest);
                    }
                }
                level.levelEvent(2001, target, Block.getId(bs));
                mined++;
            }
            fuel--;
            if (state == State.MINING && cell(cursor).equals(target)) {
                cursor++;
            }
            // trasloco: copia i dati nel nuovo posto, poi toglie la talpa da quello vecchio
            CompoundTag tag = saveWithoutMetadata(level.registryAccess());
            BlockPos old = worldPosition;
            BlockState moleState = getBlockState();
            level.setBlock(target, moleState, Block.UPDATE_ALL);
            if (level.getBlockEntity(target) instanceof MoleEntity copy) {
                copy.loadWithComponents(tag, level.registryAccess());
                copy.wait = 0;
                copy.setChanged();
            }
            moving = true;
            level.setBlock(old, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            server.sendParticles(ParticleTypes.CLOUD, target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, 2,
                    0.2, 0.2, 0.2, 0.0);
            server.playSound(null, target, moleState.getSoundType().getHitSound(), SoundSource.BLOCKS, 0.4F, 0.8F);
        }

        // --- salvataggio ---
        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.put("Items", items.serializeNBT(registries));
            tag.putInt("Radius", radius);
            tag.putString("Away", away.getSerializedName());
            tag.putInt("Fuel", fuel);
            tag.putString("State", state.name());
            tag.putInt("Layer", layer);
            tag.putInt("Cursor", cursor);
            tag.putInt("Mined", mined);
            if (home != null) {
                tag.put("Home", NbtUtils.writeBlockPos(home));
            }
            if (chest != null) {
                tag.put("Chest", NbtUtils.writeBlockPos(chest));
            }
            if (resume != null) {
                tag.put("Resume", NbtUtils.writeBlockPos(resume));
            }
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            items.deserializeNBT(registries, tag.getCompound("Items"));
            radius = Math.max(1, tag.getInt("Radius"));
            Direction d = Direction.byName(tag.getString("Away"));
            away = d == null || d.getAxis().isVertical() ? Direction.NORTH : d;
            fuel = tag.getInt("Fuel");
            try {
                state = State.valueOf(tag.getString("State"));
            } catch (IllegalArgumentException e) {
                state = State.IDLE;
            }
            layer = tag.getInt("Layer");
            cursor = tag.getInt("Cursor");
            mined = tag.getInt("Mined");
            home = NbtUtils.readBlockPos(tag, "Home").orElse(null);
            chest = NbtUtils.readBlockPos(tag, "Chest").orElse(null);
            resume = NbtUtils.readBlockPos(tag, "Resume").orElse(null);
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable(getBlockState().getBlock().getDescriptionId());
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
            return new MoleMenu(id, inventory, worldPosition, items, data, this);
        }
    }

    // =====================================================================
    public static class MoleMenu extends AbstractContainerMenu {
        public static final int BUTTON_TOGGLE = 0;
        public static final int BUTTON_LESS = 1;
        public static final int BUTTON_MORE = 2;

        private final ContainerData data;
        private final BlockPos pos;
        @Nullable private final MoleEntity mole;

        public MoleMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
            this(id, inventory, buf.readBlockPos(), new ItemStackHandler(STORAGE + 1), new SimpleContainerData(5), null);
        }

        public MoleMenu(int id, Inventory inventory, BlockPos pos, IItemHandler items, ContainerData data,
                @Nullable MoleEntity mole) {
            super(ModMenus.MINING_MOLE.get(), id);
            this.data = data;
            this.pos = pos;
            this.mole = mole;
            addDataSlots(data);
            for (int i = 0; i < STORAGE; i++) {
                addSlot(new SlotItemHandler(items, i, 8 + (i % 9) * 18, 50 + (i / 9) * 18));
            }
            addSlot(new SlotItemHandler(items, FUEL_SLOT, 152, 18));
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 102 + row * 18));
                }
            }
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col, 8 + col * 18, 160));
            }
        }

        public int getRadius() {
            return data.get(0);
        }

        public State getState() {
            return State.values()[Math.max(0, Math.min(State.values().length - 1, data.get(1)))];
        }

        public int getFuel() {
            return data.get(2);
        }

        public int getDepth() {
            return data.get(3);
        }

        public int getMined() {
            return data.get(4);
        }

        @Override
        public boolean clickMenuButton(Player player, int id) {
            if (mole == null) {
                return false;
            }
            switch (id) {
                case BUTTON_TOGGLE -> mole.toggle();
                case BUTTON_LESS -> mole.setRadius(mole.getRadius() - 1);
                case BUTTON_MORE -> mole.setRadius(mole.getRadius() + 1);
                default -> {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            Slot slot = slots.get(index);
            if (!slot.hasItem()) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = slot.getItem();
            ItemStack copy = stack.copy();
            int own = STORAGE + 1;
            if (index < own) {
                if (!moveItemStackTo(stack, own, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.getBurnTime(RecipeType.SMELTING) > 0) {
                if (!moveItemStackTo(stack, FUEL_SLOT, FUEL_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            return copy;
        }

        @Override
        public boolean stillValid(Player player) {
            // la talpa si muove: va bene finche' il giocatore e' abbastanza vicino a lei
            if (mole != null) {
                if (mole.isRemoved()) {
                    return false;
                }
                BlockPos p = mole.getBlockPos();
                return player.distanceToSqr(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5) <= 4096;
            }
            return true;
        }
    }
}
