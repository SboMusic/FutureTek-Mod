package com.futuretek.logistics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.futuretek.blockentity.SortingConduitBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * Una rete di condotti per oggetti e tutti i contenitori collegati (bauli, macchine, ecc.).
 * Gli oggetti "viaggiano" istantaneamente da un contenitore all'altro.
 *
 * Dove va un oggetto inserito nella rete (in ordine di priorita'):
 *  1. nei contenitori collegati a un CONDOTTO SMISTATORE il cui filtro accetta quell'oggetto;
 *  2. nei contenitori che contengono GIA' quell'oggetto (smistamento automatico);
 *  3. in qualsiasi altro contenitore collegato a un condotto normale.
 * I contenitori dietro uno smistatore ricevono SOLO gli oggetti del loro filtro.
 *
 * Con un NUCLEO LOGISTICO acceso nella rete, il baule che tocca il nucleo diventa il "baule principale":
 * il punto 3 vale solo per lui (tutto cio' che non ha una destinazione finisce li'), e il nucleo
 * sposta da solo verso le destinazioni (punti 1 e 2) gli oggetti che ci metti dentro.
 * Ogni spostamento viene mostrato ai giocatori vicini con un'animazione lungo i tubi.
 */
public class ItemNetwork {
    /** Un contenitore collegato: posizione, lato toccato dal condotto e condotto che lo tocca */
    public record Endpoint(BlockPos pos, Direction face, BlockPos conduit, boolean main) {
        public Endpoint(BlockPos pos, Direction face, BlockPos conduit) {
            this(pos, face, conduit, false);
        }
    }

    private final Level level;
    private final Set<BlockPos> conduits;
    private final List<Endpoint> endpoints;
    private final List<BlockPos> devices;

    public ItemNetwork(Level level, Set<BlockPos> conduits, List<Endpoint> endpoints, List<BlockPos> devices) {
        this.level = level;
        this.conduits = conduits;
        this.endpoints = endpoints;
        this.devices = devices;
    }

    public Set<BlockPos> getConduits() {
        return conduits;
    }

    public List<Endpoint> getEndpoints() {
        return endpoints;
    }

    /** Terminali e banchi di assemblaggio collegati alla rete */
    public List<BlockPos> getDevices() {
        return devices;
    }

    public Level getLevel() {
        return level;
    }

    @Nullable
    private IItemHandler handler(Endpoint e) {
        return level.getCapability(Capabilities.ItemHandler.BLOCK, e.pos(), e.face());
    }

    @Nullable
    private SortingConduitBlockEntity sorter(Endpoint e) {
        return level.getBlockEntity(e.conduit()) instanceof SortingConduitBlockEntity s ? s : null;
    }

    /** Questo contenitore e' dietro un tubo analizzatore con "Lascia 1"? */
    private boolean keepsOne(Endpoint e) {
        return level.getBlockEntity(e.conduit()) instanceof AnalyzerConduitBlockEntity a && a.keepsOne();
    }

    private static int countIn(IItemHandler handler, ItemStack template) {
        int n = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack s = handler.getStackInSlot(i);
            if (ItemStack.isSameItemSameComponents(s, template)) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static boolean contains(IItemHandler handler, ItemStack stack) {
        for (int i = 0; i < handler.getSlots(); i++) {
            if (ItemStack.isSameItemSameComponents(handler.getStackInSlot(i), stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Inserisce un oggetto nella rete.
     * @param exclude contenitore da cui proviene (non ci torna dentro)
     * @return cio' che NON e' stato possibile inserire
     */
    public ItemStack insert(ItemStack stack, @Nullable BlockPos exclude, boolean simulate) {
        return insert(stack, exclude, simulate, false);
    }

    /** Solo verso le destinazioni (filtri e bauli che contengono gia' l'oggetto): usato dal nucleo logistico */
    public ItemStack insertToDestinations(ItemStack stack, @Nullable BlockPos exclude, boolean simulate) {
        return insert(stack, exclude, simulate, true);
    }

    /** C'e' un nucleo logistico acceso in questa rete? */
    public boolean hasPoweredCore() {
        for (BlockPos device : devices) {
            if (level.getBlockEntity(device) instanceof LogisticsCoreBlockEntity core && core.isPowered()) {
                return true;
            }
        }
        return false;
    }

    private ItemStack insert(ItemStack stack, @Nullable BlockPos exclude, boolean simulate, boolean destinationsOnly) {
        if (stack.isEmpty()) {
            return stack;
        }
        boolean core = hasPoweredCore();
        List<Endpoint> filtered = new ArrayList<>();
        List<Endpoint> alreadyHas = new ArrayList<>();
        List<Endpoint> mainChest = new ArrayList<>();
        List<Endpoint> generic = new ArrayList<>();
        for (Endpoint e : endpoints) {
            if (e.pos().equals(exclude)) {
                continue;
            }
            IItemHandler h = handler(e);
            if (h == null) {
                continue;
            }
            SortingConduitBlockEntity sorter = sorter(e);
            if (sorter != null) {
                if (sorter.accepts(stack)) {
                    filtered.add(e);
                }
            } else if (e.main() && core) {
                mainChest.add(e);
            } else if (contains(h, stack)) {
                alreadyHas.add(e);
            } else {
                generic.add(e);
            }
        }
        List<List<Endpoint>> order = destinationsOnly ? List.of(filtered, alreadyHas)
                : core ? List.of(filtered, alreadyHas, mainChest) : List.of(filtered, alreadyHas, generic);
        ItemStack rest = stack.copy();
        for (List<Endpoint> group : order) {
            for (Endpoint e : group) {
                IItemHandler h = handler(e);
                int before = rest.getCount();
                rest = ItemHandlerHelper.insertItemStacked(h, rest, simulate);
                if (!simulate && rest.getCount() < before) {
                    ItemTransit.animate(level, this, exclude, e, stack.copyWithCount(before - rest.getCount()));
                }
                if (rest.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }
        return rest;
    }

    /** Il baule principale (quello che tocca un nucleo logistico), se c'e' */
    @Nullable
    public Endpoint mainChest() {
        for (Endpoint e : endpoints) {
            if (e.main()) {
                return e;
            }
        }
        return null;
    }

    @Nullable
    IItemHandler handlerOf(Endpoint e) {
        return handler(e);
    }

    /** Toglie dalla rete fino a "amount" oggetti che soddisfano il filtro (tutti uguali tra loro) */
    public ItemStack extract(Predicate<ItemStack> filter, int amount, boolean simulate) {
        ItemStack result = ItemStack.EMPTY;
        for (Endpoint e : endpoints) {
            IItemHandler h = handler(e);
            if (h == null) {
                continue;
            }
            boolean keep = keepsOne(e);
            int takenHere = 0;
            for (int slot = 0; slot < h.getSlots() && (result.isEmpty() || result.getCount() < amount); slot++) {
                ItemStack inSlot = h.getStackInSlot(slot);
                if (inSlot.isEmpty() || !filter.test(inSlot)) {
                    continue;
                }
                if (!result.isEmpty() && !ItemStack.isSameItemSameComponents(result, inSlot)) {
                    continue;
                }
                int want = amount - result.getCount();
                if (keep) {
                    // lascia sempre 1 oggetto di questo tipo nel baule
                    int allowed = countIn(h, inSlot) - 1 - (simulate ? takenHere : 0);
                    want = Math.min(want, allowed);
                    if (want <= 0) {
                        continue;
                    }
                }
                ItemStack got = h.extractItem(slot, want, simulate);
                takenHere += got.getCount();
                if (got.isEmpty()) {
                    continue;
                }
                if (result.isEmpty()) {
                    result = got.copy();
                } else {
                    result.grow(got.getCount());
                }
            }
            if (!result.isEmpty() && result.getCount() >= amount) {
                break;
            }
        }
        return result;
    }

    /** Toglie esattamente un oggetto uguale a "template" */
    public ItemStack extractExact(ItemStack template, int amount, boolean simulate) {
        return extract(s -> ItemStack.isSameItemSameComponents(s, template), amount, simulate);
    }

    /** Quanti oggetti uguali a "template" ci sono nella rete */
    public long count(ItemStack template) {
        long total = 0;
        for (Endpoint e : endpoints) {
            IItemHandler h = handler(e);
            if (h == null) {
                continue;
            }
            int here = countIn(h, template);
            total += keepsOne(e) ? Math.max(0, here - 1) : here; // l'ultimo "prenotato" non si conta
        }
        return total;
    }

    /** Elenco riassuntivo di tutto cio' che c'e' nella rete (oggetto -> quantita'), senza doppioni */
    public List<Map.Entry<ItemStack, Long>> contents() {
        Map<ItemKey, Long> totals = new LinkedHashMap<>();
        Map<ItemKey, ItemStack> samples = new LinkedHashMap<>();
        for (Endpoint e : endpoints) {
            IItemHandler h = handler(e);
            if (h == null) {
                continue;
            }
            Map<ItemKey, Long> here = new LinkedHashMap<>();
            for (int slot = 0; slot < h.getSlots(); slot++) {
                ItemStack s = h.getStackInSlot(slot);
                if (s.isEmpty()) {
                    continue;
                }
                ItemKey key = new ItemKey(s);
                here.merge(key, (long) s.getCount(), Long::sum);
                samples.putIfAbsent(key, s.copyWithCount(1));
            }
            boolean keep = keepsOne(e);
            here.forEach((key, n) -> totals.merge(key, keep ? n - 1 : n, Long::sum));
        }
        List<Map.Entry<ItemStack, Long>> list = new ArrayList<>();
        totals.forEach((key, count) -> {
            if (count > 0) {
                list.add(Map.entry(samples.get(key), count));
            }
        });
        return list;
    }

    /** Chiave per confrontare oggetti (stesso oggetto + stessi dati) */
    private static final class ItemKey {
        private final ItemStack stack;
        private final int hash;

        ItemKey(ItemStack stack) {
            this.stack = stack;
            this.hash = ItemStack.hashItemAndComponents(stack);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof ItemKey other && ItemStack.isSameItemSameComponents(stack, other.stack);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
