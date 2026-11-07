package com.futuretek.nuclear;

import javax.annotation.Nullable;

import com.futuretek.automation.SlotMachines;
import com.futuretek.blockentity.ReactorBlockEntity;
import com.futuretek.item.ReactorComponentItem;
import com.futuretek.registry.ModBlockEntities;
import com.futuretek.registry.ModBlocks;
import com.futuretek.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Manutentore del reattore: si mette attaccato al nucleo (o a una camera) e cambia da solo i pezzi consumati.
 *  - celle di raffreddamento oltre il 90% di calore (o gia' esauste) -> le sostituisce con una nuova dello stesso tipo
 *  - dissipatori oltre il 90% -> li sostituisce con uno nuovo
 *  - barre di uranio esaurite -> le sostituisce con barre nuove dello stesso numero di barre
 * I pezzi nuovi li prende dalle sue 16 caselle; quelli usati li rimette li' (svuotalo con una tramoggia o un tubo).
 * Se TOCCA L'ACQUA (una sorgente accanto) ricarica da solo le celle esauste: cosi' il ciclo non si ferma mai.
 * Consuma 50 TU per ogni cambio.
 */
public class ReactorMaintainer extends SlotMachines.Entity {
    public static final int COST = 50;
    public static final int REFILL_TICKS = 100;
    public static final double THRESHOLD = 0.9;
    private int refill;

    public ReactorMaintainer(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REACTOR_MAINTAINER.get(), pos, state, 20_000, 256, 16);
    }

    @Nullable
    ReactorBlockEntity reactor() {
        for (Direction d : Direction.values()) {
            BlockPos p = worldPosition.relative(d);
            if (level.getBlockEntity(p) instanceof ReactorBlockEntity r) {
                return r;
            }
            if (level.getBlockState(p).is(ModBlocks.REACTOR_CHAMBER.get())) {
                for (Direction d2 : Direction.values()) {
                    if (level.getBlockEntity(p.relative(d2)) instanceof ReactorBlockEntity r) {
                        return r;
                    }
                }
            }
        }
        return null;
    }

    boolean touchesWater() {
        for (Direction d : Direction.values()) {
            if (level.getFluidState(worldPosition.relative(d)).is(Fluids.WATER)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void serverTick() {
        // celle esauste -> nuove, se c'e' l'acqua
        if (touchesWater() && ++refill >= REFILL_TICKS) {
            refill = 0;
            for (int i = 0; i < items.getSlots(); i++) {
                Item fresh = freshOfSpent(items.getStackInSlot(i).getItem());
                if (fresh != null) {
                    items.setStackInSlot(i, new ItemStack(fresh));
                    break;
                }
            }
        }
        if (level.getGameTime() % 10 != 0) {
            return;
        }
        ReactorBlockEntity reactor = reactor();
        if (reactor == null) {
            status = 0;
            setLit(false);
            return;
        }
        ItemStackHandler grid = reactor.getItems();
        boolean missing = false;
        int swaps = 0;
        for (int slot = 0; slot < grid.getSlots() && energy.getEnergyStored() >= COST; slot++) {
            if (!reactor.isActiveSlot(slot)) {
                continue;
            }
            ItemStack in = grid.getStackInSlot(slot);
            Item want = replacementFor(in);
            if (want == null) {
                continue;
            }
            int spare = findFresh(want, in);
            if (spare < 0 || !hasRoomFor(in)) {
                missing = true;
                continue;
            }
            ItemStack fresh = items.extractItem(spare, 1, false);
            ItemStack old = in.copy();
            grid.setStackInSlot(slot, fresh);
            store(old);
            energy.consume(COST);
            swaps++;
        }
        v1 += swaps;
        status = missing ? 2 : 1;
        setLit(swaps > 0);
    }

    /** Che pezzo nuovo serve al posto di questo (null = va bene cosi') */
    @Nullable
    static Item replacementFor(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Item spentFresh = freshOfSpent(stack.getItem());
        if (spentFresh != null) {
            return spentFresh;
        }
        if (stack.is(ModItems.DEPLETED_FUEL_ROD.get())) {
            ReactorComponentItem.Type t = switch (stack.getCount()) {
                case 2 -> ReactorComponentItem.Type.DUAL_FUEL_ROD;
                case 4 -> ReactorComponentItem.Type.QUAD_FUEL_ROD;
                default -> ReactorComponentItem.Type.FUEL_ROD;
            };
            return ModItems.REACTOR_PARTS.get(t).get();
        }
        if (stack.getItem() instanceof ReactorComponentItem c && (c.getType().isCoolant()
                || c.getType() == ReactorComponentItem.Type.HEAT_VENT)
                && stack.getDamageValue() >= stack.getMaxDamage() * THRESHOLD) {
            return stack.getItem();
        }
        return null;
    }

    @Nullable
    static Item freshOfSpent(Item item) {
        if (item == ModItems.SPENT_COOLANT_CELL.get()) {
            return ModItems.REACTOR_PARTS.get(ReactorComponentItem.Type.COOLANT_CELL).get();
        }
        if (item == ModItems.SPENT_COOLANT_CELL_30K.get()) {
            return ModItems.REACTOR_PARTS.get(ReactorComponentItem.Type.COOLANT_CELL_30K).get();
        }
        if (item == ModItems.SPENT_COOLANT_CELL_60K.get()) {
            return ModItems.REACTOR_PARTS.get(ReactorComponentItem.Type.COOLANT_CELL_60K).get();
        }
        return null;
    }

    /** Una casella con un pezzo NUOVO (poco usato) di quel tipo */
    int findFresh(Item want, ItemStack old) {
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack s = items.getStackInSlot(i);
            if (s.is(want) && (!s.isDamageableItem() || s.getDamageValue() < s.getMaxDamage() * 0.5)) {
                return i;
            }
        }
        return -1;
    }

    boolean hasRoomFor(ItemStack old) {
        for (int i = 0; i < items.getSlots(); i++) {
            if (items.insertItem(i, old.copy(), true).isEmpty()) {
                return true;
            }
        }
        // c'e' posto dove sta il pezzo nuovo che togliamo?
        return true;
    }

    void store(ItemStack old) {
        ItemStack rest = old;
        for (int i = 0; i < items.getSlots() && !rest.isEmpty(); i++) {
            rest = items.insertItem(i, rest, false);
        }
        if (!rest.isEmpty()) {
            rest = output(rest, 16); // contenitori accanto
        }
        if (!rest.isEmpty()) {
            net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1,
                    worldPosition.getZ() + 0.5, rest);
        }
    }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return stack.getItem() instanceof ReactorComponentItem || freshOfSpent(stack.getItem()) != null
                || stack.is(ModItems.DEPLETED_FUEL_ROD.get());
    }
}
