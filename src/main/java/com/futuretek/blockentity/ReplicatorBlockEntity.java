package com.futuretek.blockentity;

import com.futuretek.essence.EssenceValues;
import com.futuretek.menu.ReplicatorMenu;
import com.futuretek.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Replicatore: copia l'oggetto "modello".
 * Slot 0 = modello (non viene consumato), slot 1-9 = materiali da sciogliere, slot 10 = copie prodotte.
 * Tutto cio' che entra nei materiali diventa Essenza interna; anche i Captatori Stellari vicini la riempiono.
 */
public class ReplicatorBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_TEMPLATE = 0;
    public static final int FIRST_INPUT = 1;
    public static final int INPUTS = 9;
    public static final int SLOT_OUTPUT = 10;
    public static final long MAX_BUFFER = 1_000_000_000L;

    private long buffer;

    private final ItemStackHandler items = new ItemStackHandler(11) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_OUTPUT) {
                return false;
            }
            if (level == null) {
                return true;
            }
            return slot == SLOT_TEMPLATE ? EssenceValues.isCreatable(level.isClientSide, stack.getItem())
                    : EssenceValues.get(level.isClientSide, stack.getItem()) > 0;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == SLOT_TEMPLATE ? 1 : 64;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    /** Per tubi e tramogge: si inseriscono materiali, si estraggono le copie */
    private final IItemHandler automationItems = new IItemHandler() {
        @Override
        public int getSlots() {
            return INPUTS + 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot < INPUTS ? items.getStackInSlot(FIRST_INPUT + slot) : items.getStackInSlot(SLOT_OUTPUT);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot < INPUTS ? items.insertItem(FIRST_INPUT + slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot < INPUTS ? ItemStack.EMPTY : items.extractItem(SLOT_OUTPUT, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot < INPUTS && items.isItemValid(FIRST_INPUT, stack);
        }
    };

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            long templateValue = templateValue();
            return switch (index) {
                case 0 -> (int) (Math.min(buffer, 0xFFFFFFFFL) & 0xFFFF);
                case 1 -> (int) ((Math.min(buffer, 0xFFFFFFFFL) >>> 16) & 0xFFFF);
                case 2 -> (int) (Math.min(templateValue, 0xFFFFFFFFL) & 0xFFFF);
                case 3 -> (int) ((Math.min(templateValue, 0xFFFFFFFFL) >>> 16) & 0xFFFF);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public ReplicatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REPLICATOR.get(), pos, state);
    }

    private long templateValue() {
        ItemStack template = items.getStackInSlot(SLOT_TEMPLATE);
        return template.isEmpty() || level == null ? 0 : EssenceValues.get(level.isClientSide, template.getItem());
    }

    /** Riceve Essenza dall'esterno (Captatori); restituisce quanta ne e' entrata */
    public long receiveEssence(long amount) {
        long added = Math.max(0, Math.min(amount, MAX_BUFFER - buffer));
        buffer += added;
        setChanged();
        return added;
    }

    public void serverTick() {
        // 1) scioglie un materiale per tick
        for (int i = FIRST_INPUT; i < FIRST_INPUT + INPUTS; i++) {
            ItemStack stack = items.getStackInSlot(i);
            long value = EssenceValues.get(false, stack.getItem());
            if (!stack.isEmpty() && value > 0 && buffer + value <= MAX_BUFFER) {
                items.extractItem(i, 1, false);
                buffer += value;
                break;
            }
        }
        // 2) crea una copia per tick se c'e' abbastanza Essenza e spazio
        ItemStack template = items.getStackInSlot(SLOT_TEMPLATE);
        long cost = templateValue();
        if (cost > 0 && buffer >= cost) {
            ItemStack out = items.getStackInSlot(SLOT_OUTPUT);
            boolean fits = out.isEmpty() || (ItemStack.isSameItemSameComponents(out, template)
                    && out.getCount() < out.getMaxStackSize());
            if (fits) {
                buffer -= cost;
                items.setStackInSlot(SLOT_OUTPUT, out.isEmpty() ? template.copyWithCount(1)
                        : out.copyWithCount(out.getCount() + 1));
            }
        }
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public IItemHandler getAutomationItems() {
        return automationItems;
    }

    public long getBuffer() {
        return buffer;
    }

    public void dropContents() {
        for (int i = 0; i < items.getSlots(); i++) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                    items.getStackInSlot(i));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("Buffer", buffer);
        tag.put("Items", items.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        buffer = tag.getLong("Buffer");
        items.deserializeNBT(registries, tag.getCompound("Items"));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ReplicatorMenu(id, inventory, worldPosition, items, data);
    }
}
