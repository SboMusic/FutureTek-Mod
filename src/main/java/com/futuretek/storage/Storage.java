package com.futuretek.storage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.futuretek.registry.ModBlockEntities;
import com.futuretek.registry.ModMenus;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Bauli di FutureTek: piu' grandi di quello di Minecraft.
 * Nell'interfaccia ci sono due pulsanti:
 *  - "Deposita tutto": porta dentro il baule tutti gli oggetti dell'inventario che il baule contiene GIA'
 *  - "Ordina": raggruppa e mette in ordine il contenuto
 * Funzionano con tramogge, condotti e tubi logistici.
 */
public final class Storage {
    private Storage() {
    }

    /** I tipi di baule: righe x colonne */
    public enum Tier {
        COPPER("copper_chest", 5, 9),
        IRON("iron_chest", 6, 9),
        // al massimo 6 righe (come il baule doppio), cosi' l'inventario del giocatore resta sempre visibile
        GOLD("gold_chest", 6, 14),
        DIAMOND("diamond_chest", 6, 18),
        OBSIDIAN("obsidian_chest", 6, 18);

        public final String id;
        public final int rows;
        public final int cols;

        Tier(String id, int rows, int cols) {
            this.id = id;
            this.rows = rows;
            this.cols = cols;
        }

        public int size() {
            return rows * cols;
        }
    }

    // =====================================================================
    public static class ChestBlock extends BaseEntityBlock {
        public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
        private final Tier tier;

        public ChestBlock(Tier tier, Properties properties) {
            super(properties);
            this.tier = tier;
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        public Tier getTier() {
            return tier;
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return simpleCodec(p -> new ChestBlock(Tier.IRON, p));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected BlockState rotate(BlockState state, Rotation rotation) {
            return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
        }

        @Override
        protected BlockState mirror(BlockState state, Mirror mirror) {
            return state.rotate(mirror.getRotation(state.getValue(FACING)));
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new ChestEntity(pos, state);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                BlockHitResult hit) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof ChestEntity chest) {
                player.openMenu(chest, buf -> {
                    buf.writeBlockPos(pos);
                    buf.writeVarInt(tier.ordinal());
                });
                level.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5F, 1.0F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
            if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ChestEntity chest) {
                for (int i = 0; i < chest.getItems().getSlots(); i++) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), chest.getItems().getStackInSlot(i));
                }
            }
            super.onRemove(state, level, pos, newState, moved);
        }

        @Override
        protected boolean hasAnalogOutputSignal(BlockState state) {
            return true;
        }

        @Override
        protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
            if (!(level.getBlockEntity(pos) instanceof ChestEntity chest)) {
                return 0;
            }
            float fill = 0;
            IItemHandler h = chest.getItems();
            for (int i = 0; i < h.getSlots(); i++) {
                ItemStack s = h.getStackInSlot(i);
                fill += s.isEmpty() ? 0 : (float) s.getCount() / Math.min(h.getSlotLimit(i), s.getMaxStackSize());
            }
            return fill == 0 ? 0 : 1 + (int) (fill / h.getSlots() * 14);
        }
    }

    // =====================================================================
    public static class ChestEntity extends BlockEntity implements MenuProvider {
        private final ItemStackHandler items;

        public ChestEntity(BlockPos pos, BlockState state) {
            super(ModBlockEntities.FT_CHEST.get(), pos, state);
            Tier tier = state.getBlock() instanceof ChestBlock c ? c.getTier() : Tier.IRON;
            this.items = new ItemStackHandler(tier.size()) {
                @Override
                protected void onContentsChanged(int slot) {
                    setChanged();
                }
            };
        }

        public ItemStackHandler getItems() {
            return items;
        }

        public Tier getTier() {
            return getBlockState().getBlock() instanceof ChestBlock c ? c.getTier() : Tier.IRON;
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.put("Items", items.serializeNBT(registries));
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            if (tag.contains("Items")) {
                int size = items.getSlots();
                items.deserializeNBT(registries, tag.getCompound("Items"));
                if (items.getSlots() != size) {
                    // salvato con un'altra grandezza: ricrea alla grandezza giusta senza perdere oggetti
                    List<ItemStack> old = new ArrayList<>();
                    for (int i = 0; i < items.getSlots(); i++) {
                        old.add(items.getStackInSlot(i));
                    }
                    items.setSize(size);
                    for (int i = 0; i < Math.min(size, old.size()); i++) {
                        items.setStackInSlot(i, old.get(i));
                    }
                }
            }
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable(getBlockState().getBlock().getDescriptionId());
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
            return new ChestMenu(id, inventory, worldPosition, getTier(), items);
        }
    }

    // =====================================================================
    public static class ChestMenu extends AbstractContainerMenu {
        public static final int BUTTON_DEPOSIT = 0;
        public static final int BUTTON_SORT = 1;

        private final Tier tier;
        private final BlockPos pos;
        private final IItemHandler items;

        public ChestMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
            this(id, inventory, buf.readBlockPos(), Tier.values()[buf.readVarInt()], null);
        }

        public ChestMenu(int id, Inventory inventory, BlockPos pos, Tier tier, IItemHandler handler) {
            super(ModMenus.FT_CHEST.get(), id);
            this.tier = tier;
            this.pos = pos;
            this.items = handler != null ? handler : new ItemStackHandler(tier.size());
            int left = left(tier);
            for (int row = 0; row < tier.rows; row++) {
                for (int col = 0; col < tier.cols; col++) {
                    addSlot(new SlotItemHandler(items, col + row * tier.cols, left + col * 18, 18 + row * 18));
                }
            }
            int invLeft = inventoryLeft(tier);
            int invTop = inventoryTop(tier);
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    addSlot(new Slot(inventory, col + row * 9 + 9, invLeft + col * 18, invTop + row * 18));
                }
            }
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col, invLeft + col * 18, invTop + 58));
            }
        }

        public Tier getTier() {
            return tier;
        }

        // --- disposizione (usata anche dalla schermata) ---
        public static int width(Tier tier) {
            return Math.max(176, tier.cols * 18 + 16);
        }

        public static int left(Tier tier) {
            return (width(tier) - tier.cols * 18) / 2 + 1;
        }

        public static int inventoryLeft(Tier tier) {
            return (width(tier) - 162) / 2 + 1;
        }

        public static int inventoryTop(Tier tier) {
            return 18 + tier.rows * 18 + 14;
        }

        public static int height(Tier tier) {
            return inventoryTop(tier) + 58 + 24;
        }

        @Override
        public boolean clickMenuButton(Player player, int id) {
            if (id == BUTTON_DEPOSIT) {
                depositAll(player);
                return true;
            }
            if (id == BUTTON_SORT) {
                sort(items);
                return true;
            }
            return false;
        }

        /** Sposta nel baule tutti gli oggetti dell'inventario (non la barra veloce) che il baule contiene gia' */
        private void depositAll(Player player) {
            Inventory inv = player.getInventory();
            for (int i = 9; i < 36; i++) {
                ItemStack stack = inv.getItem(i);
                if (stack.isEmpty() || !contains(stack)) {
                    continue;
                }
                ItemStack rest = net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(items, stack, false);
                inv.setItem(i, rest);
            }
            broadcastChanges();
        }

        private boolean contains(ItemStack stack) {
            for (int i = 0; i < items.getSlots(); i++) {
                if (ItemStack.isSameItemSameComponents(items.getStackInSlot(i), stack)) {
                    return true;
                }
            }
            return false;
        }

        /** Raggruppa le pile uguali e le ordina per nome dell'oggetto */
        public static void sort(IItemHandler items) {
            if (!(items instanceof ItemStackHandler handler)) {
                return;
            }
            List<ItemStack> all = new ArrayList<>();
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack s = handler.getStackInSlot(i);
                if (!s.isEmpty()) {
                    boolean merged = false;
                    for (ItemStack t : all) {
                        if (ItemStack.isSameItemSameComponents(t, s) && t.getCount() < t.getMaxStackSize()) {
                            int move = Math.min(s.getCount(), t.getMaxStackSize() - t.getCount());
                            t.grow(move);
                            s = s.copyWithCount(s.getCount() - move);
                            if (s.isEmpty()) {
                                merged = true;
                                break;
                            }
                        }
                    }
                    if (!merged) {
                        all.add(s.copy());
                    }
                }
            }
            all.sort(Comparator.comparing((ItemStack s) -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString())
                    .thenComparing(s -> -s.getCount()));
            for (int i = 0; i < handler.getSlots(); i++) {
                handler.setStackInSlot(i, i < all.size() ? all.get(i) : ItemStack.EMPTY);
            }
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            Slot slot = slots.get(index);
            if (!slot.hasItem()) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = slot.getItem();
            ItemStack copy = stack.copy();
            int chestSlots = tier.size();
            if (index < chestSlots) {
                if (!moveItemStackTo(stack, chestSlots, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, chestSlots, false)) {
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
            return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64
                    && player.level().getBlockState(pos).getBlock() instanceof ChestBlock;
        }
    }
}
