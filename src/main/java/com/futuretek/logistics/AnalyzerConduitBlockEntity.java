package com.futuretek.logistics;

import java.util.ArrayList;
import java.util.List;

import com.futuretek.block.ConduitBlock;
import com.futuretek.blockentity.SortingConduitBlockEntity;
import com.futuretek.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Tubo analizzatore: il "magazzino automatico" in un solo tubo.
 *  - IMPARA da solo che oggetti ci sono nel baule che tocca e se li ricorda (anche se poi il baule si svuota);
 *  - il baule riceve dalla rete SOLO gli oggetti che il tubo ricorda, piu' quelli del filtro (9 caselle fantasma);
 *  - con il nucleo logistico, tutto quello che arriva nel baule principale viene mandato qui da solo;
 *  - opzione "Lascia 1": il terminale e il crafting automatico non prendono mai l'ultimo oggetto di ogni tipo.
 */
public class AnalyzerConduitBlockEntity extends SortingConduitBlockEntity {
    public static final int MAX_MEMORY = 81;
    public static final int BUTTON_KEEP_ONE = 0;
    public static final int BUTTON_FORGET = 1;

    private final List<Item> memory = new ArrayList<>();
    private boolean keepOne = true;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? (keepOne ? 1 : 0) : memory.size();
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public AnalyzerConduitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANALYZER_CONDUIT.get(), pos, state);
    }

    @Override
    public boolean accepts(ItemStack stack) {
        return memory.contains(stack.getItem()) || super.accepts(stack);
    }

    public boolean keepsOne() {
        return keepOne;
    }

    public List<Item> getMemory() {
        return memory;
    }

    public void buttonPressed(int id) {
        if (id == BUTTON_KEEP_ONE) {
            keepOne = !keepOne;
        } else if (id == BUTTON_FORGET) {
            memory.clear();
            learn(); // ricomincia da quello che c'e' adesso nel baule
        }
        setChanged();
    }

    /** Ogni 2 secondi guarda nei contenitori che tocca e impara gli oggetti nuovi */
    public void serverTick() {
        if (level.getGameTime() % 40 == Math.floorMod(worldPosition.asLong(), 40) && learn()) {
            setChanged();
        }
    }

    private boolean learn() {
        boolean changed = false;
        for (Direction dir : Direction.values()) {
            BlockPos next = worldPosition.relative(dir);
            if (level.getBlockState(next).getBlock() instanceof ConduitBlock) {
                continue;
            }
            IItemHandler h = level.getCapability(Capabilities.ItemHandler.BLOCK, next, dir.getOpposite());
            if (h == null) {
                continue;
            }
            for (int i = 0; i < h.getSlots() && memory.size() < MAX_MEMORY; i++) {
                ItemStack s = h.getStackInSlot(i);
                if (!s.isEmpty() && !memory.contains(s.getItem())) {
                    memory.add(s.getItem());
                    changed = true;
                }
            }
        }
        return changed;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (Item item : memory) {
            list.add(StringTag.valueOf(BuiltInRegistries.ITEM.getKey(item).toString()));
        }
        tag.put("Memory", list);
        tag.putBoolean("KeepOne", keepOne);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        memory.clear();
        for (Tag t : tag.getList("Memory", Tag.TAG_STRING)) {
            ResourceLocation id = ResourceLocation.tryParse(t.getAsString());
            if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                memory.add(BuiltInRegistries.ITEM.get(id));
            }
        }
        keepOne = !tag.contains("KeepOne") || tag.getBoolean("KeepOne");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AnalyzerMenu(id, inventory, worldPosition, getFilter(), data, this);
    }
}
