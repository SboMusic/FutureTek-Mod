package com.futuretek.power;

import com.futuretek.registry.ModBlockEntities;
import com.futuretek.registry.ModMenus;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;

/**
 * Carbonaia: ci metti tante pile di tronchi (18 slot) e li trasforma in carbonella (18 slot), senza combustibile.
 * Ogni 10 secondi cuoce fino a 16 tronchi insieme: 1 tronco = 1 carbonella.
 * Tramogge e condotti: i tronchi entrano da sopra e dai lati, la carbonella esce da sotto.
 */
public final class CharcoalKiln {
    private CharcoalKiln() {
    }

    public static final int SLOTS = 18;
    public static final int CYCLE = 200;
    public static final int PER_CYCLE = 16;

    public static class KilnBlock extends BaseEntityBlock {
        public static final BooleanProperty LIT = BlockStateProperties.LIT;

        public KilnBlock(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(LIT, false));
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return simpleCodec(KilnBlock::new);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(LIT);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState();
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new KilnEntity(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            return level.isClientSide ? null : (l, p, s, be) -> {
                if (be instanceof KilnEntity kiln) {
                    kiln.serverTick();
                }
            };
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                BlockHitResult hit) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof KilnEntity kiln) {
                player.openMenu(kiln, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
            if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof KilnEntity kiln) {
                for (int i = 0; i < kiln.items.getSlots(); i++) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), kiln.items.getStackInSlot(i));
                }
            }
            super.onRemove(state, level, pos, newState, moved);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (state.getValue(LIT)) {
                level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                        0, 0.05, 0);
            }
        }
    }

    public static class KilnEntity extends BlockEntity implements MenuProvider {
        /** slot 0-17: tronchi, 18-35: carbonella */
        final ItemStackHandler items = new ItemStackHandler(SLOTS * 2) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot < SLOTS ? stack.is(ItemTags.LOGS_THAT_BURN) : stack.is(Items.CHARCOAL);
            }

            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
        private final IItemHandler inputSide = new RangedWrapper(items, 0, SLOTS) {
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }
        };
        private final IItemHandler outputSide = new RangedWrapper(items, SLOTS, SLOTS * 2) {
            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return stack;
            }
        };
        private int progress;
        private final ContainerData data = new ContainerData() {
            @Override
            public int get(int index) {
                return progress;
            }

            @Override
            public void set(int index, int value) {
                progress = value;
            }

            @Override
            public int getCount() {
                return 1;
            }
        };

        public KilnEntity(BlockPos pos, BlockState state) {
            super(ModBlockEntities.CHARCOAL_KILN.get(), pos, state);
        }

        public ItemStackHandler getItems() {
            return items;
        }

        public IItemHandler handlerFor(Direction side) {
            return side == Direction.DOWN ? outputSide : side == null ? items : inputSide;
        }

        private int logs() {
            int n = 0;
            for (int i = 0; i < SLOTS; i++) {
                n += items.getStackInSlot(i).getCount();
            }
            return n;
        }

        private int outputSpace() {
            int space = 0;
            for (int i = SLOTS; i < SLOTS * 2; i++) {
                ItemStack s = items.getStackInSlot(i);
                space += s.isEmpty() ? 64 : s.is(Items.CHARCOAL) ? 64 - s.getCount() : 0;
            }
            return space;
        }

        void serverTick() {
            boolean working = logs() > 0 && outputSpace() > 0;
            if (working) {
                progress++;
                if (progress >= CYCLE) {
                    progress = 0;
                    int todo = Math.min(PER_CYCLE, Math.min(logs(), outputSpace()));
                    for (int i = 0; i < SLOTS && todo > 0; i++) {
                        int take = Math.min(todo, items.getStackInSlot(i).getCount());
                        if (take > 0) {
                            items.extractItem(i, take, false);
                            ItemStack charcoal = new ItemStack(Items.CHARCOAL, take);
                            for (int o = SLOTS; o < SLOTS * 2 && !charcoal.isEmpty(); o++) {
                                charcoal = items.insertItem(o, charcoal, false);
                            }
                            todo -= take;
                        }
                    }
                }
            } else if (progress > 0) {
                progress = 0;
            }
            BlockState state = getBlockState();
            if (state.getValue(KilnBlock.LIT) != working) {
                level.setBlock(worldPosition, state.setValue(KilnBlock.LIT, working), Block.UPDATE_ALL);
            }
            if (working && level instanceof ServerLevel server && level.getGameTime() % 40 == 0) {
                server.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1,
                        worldPosition.getZ() + 0.5, 3, 0.2, 0.1, 0.2, 0.01);
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.put("Items", items.serializeNBT(registries));
            tag.putInt("Progress", progress);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            items.deserializeNBT(registries, tag.getCompound("Items"));
            progress = tag.getInt("Progress");
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable(getBlockState().getBlock().getDescriptionId());
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
            return new KilnMenu(id, inventory, worldPosition, items, data);
        }
    }

    public static class KilnMenu extends AbstractContainerMenu {
        private final ContainerData data;
        private final BlockPos pos;

        public KilnMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
            this(id, inventory, buf.readBlockPos(), new ItemStackHandler(SLOTS * 2), new SimpleContainerData(1));
        }

        public KilnMenu(int id, Inventory inventory, BlockPos pos, IItemHandler items, ContainerData data) {
            super(ModMenus.CHARCOAL_KILN.get(), id);
            this.data = data;
            this.pos = pos;
            addDataSlots(data);
            for (int i = 0; i < SLOTS; i++) {
                addSlot(new SlotItemHandler(items, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18));
            }
            for (int i = 0; i < SLOTS; i++) {
                addSlot(new SlotItemHandler(items, SLOTS + i, 8 + (i % 9) * 18, 66 + (i / 9) * 18));
            }
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 116 + row * 18));
                }
            }
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col, 8 + col * 18, 174));
            }
        }

        public int getProgress() {
            return data.get(0);
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            Slot slot = slots.get(index);
            if (!slot.hasItem()) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = slot.getItem();
            ItemStack copy = stack.copy();
            if (index < SLOTS * 2) {
                if (!moveItemStackTo(stack, SLOTS * 2, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, SLOTS, false)) {
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
            return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
        }
    }
}
