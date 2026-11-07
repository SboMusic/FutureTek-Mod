package com.futuretek.automation;

import java.util.function.BiFunction;

import com.futuretek.block.MachineBlock;
import com.futuretek.blockentity.MachineBlockEntity;
import com.futuretek.logistics.ItemOutput;
import com.futuretek.menu.MachineMenu;
import com.futuretek.registry.ModMenus;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Base comune delle macchine delle Fasi 12-14: un inventario (fino a 16 caselle), la barra dell'energia,
 * una riga di "stato" e fino a 3 pulsanti. Cosi' ogni nuova macchina scrive solo il suo comportamento.
 * Testi: gui.futuretek.ID.statusN e gui.futuretek.ID.buttonN (con i valori v1, v2, v3).
 */
public final class SlotMachines {
    private SlotMachines() {
    }

    /** Blocco generico: apre l'interfaccia della sua macchina */
    public static class Block extends MachineBlock {
        private final BiFunction<BlockPos, BlockState, ? extends Entity> factory;

        public Block(Properties properties, BiFunction<BlockPos, BlockState, ? extends Entity> factory) {
            super(properties);
            this.factory = factory;
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return simpleCodec(p -> new Block(p, factory));
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return factory.apply(pos, state);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                BlockHitResult hit) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof Entity machine
                    && player instanceof ServerPlayer sp) {
                sp.openMenu(machine, buf -> {
                    buf.writeBlockPos(pos);
                    buf.writeVarInt(machine.slotCount());
                    buf.writeVarInt(machine.buttonCount());
                    buf.writeUtf(machine.machineId());
                });
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    /** La parte "che pensa" della macchina */
    public abstract static class Entity extends MachineBlockEntity implements MenuProvider {
        protected final ItemStackHandler items;
        /** Stato da mostrare (0 = fermo, gli altri numeri li sceglie la macchina) */
        protected int status;
        /** Tre valori liberi da mostrare nei testi */
        protected int v1, v2, v3;

        protected Entity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity, int receive, int slots) {
            super(type, pos, state, capacity, receive, 0);
            this.items = new ItemStackHandler(slots) {
                @Override
                public boolean isItemValid(int slot, ItemStack stack) {
                    return accepts(slot, stack);
                }

                @Override
                protected void onContentsChanged(int slot) {
                    setChanged();
                }
            };
        }

        /** Quali oggetti puo' contenere ogni casella */
        protected boolean accepts(int slot, ItemStack stack) {
            return true;
        }

        public int slotCount() {
            return items.getSlots();
        }

        public int buttonCount() {
            return 0;
        }

        /** Pulsante premuto nell'interfaccia (sul server) */
        public void onButton(Player player, int id) {
        }

        public String machineId() {
            return BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).getPath();
        }

        public int getStatus() {
            return status;
        }

        @Override
        public ItemStackHandler getItems() {
            return items;
        }

        /** Per tramogge e tubi: di base tutto l'inventario */
        public IItemHandler automation() {
            return items;
        }

        /** Mette un oggetto nei contenitori accanto; se non c'e' posto, nelle caselle della macchina */
        protected ItemStack output(ItemStack stack, int firstSlot) {
            ItemStack rest = ItemOutput.insert(level, worldPosition, stack, false);
            for (int i = firstSlot; i < items.getSlots() && !rest.isEmpty(); i++) {
                rest = items.insertItem(i, rest, false);
            }
            return rest;
        }

        protected boolean canOutput(ItemStack stack, int firstSlot) {
            ItemStack rest = ItemOutput.insert(level, worldPosition, stack, true);
            for (int i = firstSlot; i < items.getSlots() && !rest.isEmpty(); i++) {
                rest = items.insertItem(i, rest, true);
            }
            return rest.isEmpty();
        }

        private final ContainerData data = new MachineData() {
            @Override
            protected int getExtra(int index) {
                return switch (index) {
                    case 0 -> status;
                    case 1 -> v1;
                    case 2 -> v2;
                    default -> v3;
                };
            }

            @Override
            public int getCount() {
                return 8;
            }
        };

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putInt("Status", status);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            status = tag.getInt("Status");
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable(getBlockState().getBlock().getDescriptionId());
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
            return new Menu(id, inventory, worldPosition, items, data, buttonCount(), machineId(), this);
        }
    }

    /** Interfaccia generica (lato logico, server e client) */
    public static class Menu extends MachineMenu {
        public static final int COLUMNS = 8;
        public final int slotCount;
        public final int buttons;
        public final String machineId;
        private final Entity machine;

        public Menu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
            this(id, inventory, buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(), buf.readUtf());
        }

        private Menu(int id, Inventory inventory, BlockPos pos, int slots, int buttons, String machineId) {
            this(id, inventory, pos, new ItemStackHandler(slots), new SimpleContainerData(8), buttons, machineId, null);
        }

        public Menu(int id, Inventory inventory, BlockPos pos, IItemHandler items, ContainerData data, int buttons,
                String machineId, Entity machine) {
            super(ModMenus.SLOT_MACHINE.get(), id, inventory, pos, data);
            this.slotCount = items.getSlots();
            this.buttons = buttons;
            this.machineId = machineId;
            this.machine = machine;
            for (int i = 0; i < slotCount; i++) {
                addSlot(new SlotItemHandler(items, i, 8 + (i % COLUMNS) * 18, 18 + (i / COLUMNS) * 18));
            }
            addPlayerInventory(inventory);
        }

        public int rows() {
            return (slotCount + COLUMNS - 1) / COLUMNS;
        }

        public int status() {
            return data.get(4);
        }

        public int value(int i) {
            return data.get(5 + i);
        }

        @Override
        public boolean clickMenuButton(Player player, int id) {
            if (machine != null && id >= 0 && id < buttons) {
                machine.onButton(player, id);
                return true;
            }
            return false;
        }
    }
}
