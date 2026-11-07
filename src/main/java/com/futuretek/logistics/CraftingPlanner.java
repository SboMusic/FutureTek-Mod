package com.futuretek.logistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.futuretek.blockentity.AutoCrafterBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/**
 * Pianifica una richiesta di fabbricazione sulla rete ("fammi 64 cavi").
 * Se mancano ingredienti che un altro banco sa fabbricare, li mette in coda prima (a catena).
 */
public final class CraftingPlanner {
    private static final int MAX_DEPTH = 8;

    private CraftingPlanner() {
    }

    /** Esito: ok = tutto pianificato; missing = cosa manca (oggetto -> quantita') */
    public record Plan(boolean ok, List<ItemStack> missing) {
    }

    /** I banchi di assemblaggio collegati alla rete */
    public static List<AutoCrafterBlockEntity> crafters(ItemNetwork network) {
        List<AutoCrafterBlockEntity> list = new ArrayList<>();
        for (BlockPos pos : network.getDevices()) {
            if (network.getLevel().getBlockEntity(pos) instanceof AutoCrafterBlockEntity crafter
                    && !crafter.result().isEmpty()) {
                list.add(crafter);
            }
        }
        return list;
    }

    public static Plan request(ItemNetwork network, ItemStack wanted, int amount) {
        Map<AutoCrafterBlockEntity, Integer> jobs = new HashMap<>();
        Map<String, Long> virtualStock = new HashMap<>();
        List<ItemStack> missing = new ArrayList<>();
        boolean ok = plan(network, wanted, amount, jobs, virtualStock, missing, 0);
        if (ok) {
            jobs.forEach(AutoCrafterBlockEntity::enqueue);
        }
        return new Plan(ok, missing);
    }

    private static String key(ItemStack stack) {
        return stack.getItem().toString() + "#" + ItemStack.hashItemAndComponents(stack);
    }

    private static boolean plan(ItemNetwork network, ItemStack wanted, int amount,
            Map<AutoCrafterBlockEntity, Integer> jobs, Map<String, Long> virtualStock, List<ItemStack> missing,
            int depth) {
        AutoCrafterBlockEntity crafter = null;
        for (AutoCrafterBlockEntity c : crafters(network)) {
            if (ItemStack.isSameItemSameComponents(c.result(), wanted)) {
                crafter = c;
                break;
            }
        }
        if (crafter == null || depth > MAX_DEPTH) {
            missing.add(wanted.copyWithCount(amount));
            return false;
        }
        int perCraft = crafter.result().getCount();
        int crafts = (amount + perCraft - 1) / perCraft;
        boolean ok = true;
        // somma gli ingredienti necessari
        Map<String, ItemStack> samples = new HashMap<>();
        Map<String, Integer> needed = new HashMap<>();
        for (ItemStack ingredient : crafter.ingredients()) {
            samples.putIfAbsent(key(ingredient), ingredient);
            needed.merge(key(ingredient), crafts, Integer::sum);
        }
        for (Map.Entry<String, Integer> e : needed.entrySet()) {
            ItemStack sample = samples.get(e.getKey());
            long available = virtualStock.computeIfAbsent(e.getKey(), k -> network.count(sample));
            long fromStock = Math.min(available, e.getValue());
            virtualStock.put(e.getKey(), available - fromStock);
            int toCraft = (int) (e.getValue() - fromStock);
            if (toCraft > 0 && !plan(network, sample, toCraft, jobs, virtualStock, missing, depth + 1)) {
                ok = false;
            }
        }
        if (ok) {
            jobs.merge(crafter, crafts, Integer::sum);
        }
        return ok;
    }
}
