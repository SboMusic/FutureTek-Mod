package com.futuretek.essence;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.futuretek.registry.ModAttachments;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Le operazioni dell'Essenza su un giocatore: sciogliere, creare, aggiungere. */
public final class EssenceManager {
    private EssenceManager() {
    }

    public static EssenceData get(Player player) {
        return player.getData(ModAttachments.ESSENCE);
    }

    private static void set(Player player, EssenceData data) {
        player.setData(ModAttachments.ESSENCE, data);
    }

    public static void addEssence(Player player, long amount) {
        EssenceData data = get(player);
        set(player, data.withEssence(data.essence() + amount));
    }

    /**
     * Scioglie un oggetto: il giocatore riceve la sua Essenza e lo "impara".
     * @return Essenza ottenuta (0 = oggetto senza valore, non viene consumato)
     */
    public static long dissolve(Player player, ItemStack stack) {
        long gained = EssenceValues.stackValue(false, stack);
        if (gained <= 0) {
            return 0;
        }
        EssenceData data = get(player);
        if (EssenceValues.isCreatable(false, stack.getItem())) {
            data = data.learn(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        }
        set(player, data.withEssence(data.essence() + gained));
        return gained;
    }

    /**
     * Paga "amount" Essenza per usare un oggetto alchemico: prima dalla riserva personale,
     * poi dai Cristalli d'Essenza nell'inventario. In modalita' creativa e' gratis.
     * @return true se il pagamento e' riuscito (se non basta, non viene tolto niente)
     */
    public static boolean consume(Player player, long amount) {
        if (amount <= 0 || player.getAbilities().instabuild) {
            return true;
        }
        long reserve = get(player).essence();
        long inCrystals = 0;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).getItem() instanceof com.futuretek.item.EssenceCrystalItem) {
                inCrystals += com.futuretek.item.EssenceCrystalItem.getStored(inventory.getItem(i));
            }
        }
        if (reserve + inCrystals < amount) {
            return false;
        }
        long fromReserve = Math.min(reserve, amount);
        set(player, get(player).withEssence(reserve - fromReserve));
        long left = amount - fromReserve;
        for (int i = 0; i < inventory.getContainerSize() && left > 0; i++) {
            ItemStack crystal = inventory.getItem(i);
            if (crystal.getItem() instanceof com.futuretek.item.EssenceCrystalItem) {
                long stored = com.futuretek.item.EssenceCrystalItem.getStored(crystal);
                long taken = Math.min(stored, left);
                com.futuretek.item.EssenceCrystalItem.setStored(crystal, stored - taken);
                left -= taken;
            }
        }
        return true;
    }

    /** Come consume, ma se l'Essenza non basta avvisa il giocatore */
    public static boolean consumeOrWarn(Player player, long amount) {
        if (consume(player, amount)) {
            return true;
        }
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "message.futuretek.not_enough_essence", com.futuretek.energy.EnergyUtil.format(amount))
                .withStyle(net.minecraft.ChatFormatting.RED), true);
        return false;
    }

    /** Impara tutti gli oggetti che hanno un valore (Tomo dell'Onniscienza) */
    public static int learnAll(Player player) {
        EssenceData data = get(player);
        java.util.List<ResourceLocation> known = new ArrayList<>(data.known());
        java.util.Set<ResourceLocation> already = new java.util.HashSet<>(known);
        int added = 0;
        for (Item item : EssenceValues.serverValues().keySet()) {
            if (!EssenceValues.isCreatable(false, item)) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (already.add(id)) {
                known.add(id);
                added++;
            }
        }
        set(player, new EssenceData(data.essence(), List.copyOf(known)));
        return added;
    }

    /**
     * Crea "count" copie di un oggetto conosciuto pagando la sua Essenza.
     * @return quante copie sono state create (potrebbero essere meno se l'Essenza non basta)
     */
    public static int create(Player player, Item item, int count) {
        EssenceData data = get(player);
        long value = EssenceValues.get(false, item);
        if (value <= 0 || !EssenceValues.isCreatable(false, item)
                || !data.known().contains(BuiltInRegistries.ITEM.getKey(item))) {
            return 0;
        }
        int affordable = (int) Math.min(count, data.essence() / value);
        if (affordable <= 0) {
            return 0;
        }
        set(player, data.withEssence(data.essence() - affordable * value));
        ItemStack stack = new ItemStack(item, affordable);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        return affordable;
    }

    /** Elenco degli oggetti conosciuti, dal piu' prezioso al meno prezioso (uguale su client e server) */
    public static List<Item> sortedKnown(EssenceData data, boolean clientSide) {
        List<Item> list = new ArrayList<>();
        for (ResourceLocation id : data.known()) {
            BuiltInRegistries.ITEM.getOptional(id).filter(item -> EssenceValues.isCreatable(clientSide, item))
                    .ifPresent(list::add);
        }
        list.sort(Comparator.<Item>comparingLong(item -> -EssenceValues.get(clientSide, item))
                .thenComparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()));
        return list;
    }
}
