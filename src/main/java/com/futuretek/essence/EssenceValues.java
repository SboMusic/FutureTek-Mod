package com.futuretek.essence;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.futuretek.FutureTek;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Il valore in Essenza di ogni oggetto.
 * 1) I valori "di base" sono nei file JSON data/<mod>/essence_values/*.json (modificabili):
 *      { "values": { "minecraft:cobblestone": 1, "#minecraft:logs": 16 } }
 * 2) Tutti gli altri oggetti ricevono un valore calcolato dalle ricette (banco da lavoro, fornaci, tagliapietre,
 *    forgiatura): valore = somma degli ingredienti / quantita' prodotta (si sceglie la ricetta piu' economica).
 * Il server calcola, poi invia la tabella ai client (per i suggerimenti e il Tavolo di Sintesi).
 */
public final class EssenceValues {
    private EssenceValues() {
    }

    /** Righe lette dai JSON: chiave (id o #tag) -> valore */
    private static final Map<String, Long> RAW = new HashMap<>();
    /** Copie dai JSON: oggetto -> oggetto di cui prende il valore (rame ossidato = rame, cemento = polvere...) */
    private static final Map<String, String> COPIES = new HashMap<>();
    private static final Map<Item, Long> SERVER = new HashMap<>();
    private static final Map<Item, Long> CLIENT = new HashMap<>();
    private static boolean dirty = true;

    // --- caricamento JSON (chiamato dal reload listener) ---
    static void load(Map<ResourceLocation, JsonElement> files) {
        RAW.clear();
        COPIES.clear();
        files.forEach((file, json) -> {
            try {
                JsonObject values = json.getAsJsonObject().getAsJsonObject("values");
                values.entrySet().forEach(e -> RAW.put(e.getKey(), e.getValue().getAsLong()));
                JsonObject copies = json.getAsJsonObject().getAsJsonObject("copies");
                if (copies != null) {
                    copies.entrySet().forEach(e -> COPIES.put(e.getKey(), e.getValue().getAsString()));
                }
            } catch (Exception e) {
                FutureTek.LOGGER.error("FutureTek: file di valori dell'Essenza non valido: {}", file, e);
            }
        });
        dirty = true;
    }

    public static void markDirty() {
        dirty = true;
    }

    // --- lettura ---
    public static long get(Level level, ItemStack stack) {
        return stack.isEmpty() ? 0 : get(level.isClientSide, stack.getItem());
    }

    public static long get(boolean clientSide, Item item) {
        if (clientSide) {
            return CLIENT.getOrDefault(item, 0L);
        }
        ensureComputed();
        return SERVER.getOrDefault(item, 0L);
    }

    public static Map<Item, Long> serverValues() {
        ensureComputed();
        return SERVER;
    }

    public static void setClientValues(Map<ResourceLocation, Long> values) {
        CLIENT.clear();
        values.forEach((id, v) -> BuiltInRegistries.ITEM.getOptional(id).ifPresent(item -> CLIENT.put(item, v)));
    }

    // --- calcolo ---
    private static void ensureComputed() {
        if (!dirty) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        compute(server);
    }

    public static synchronized void compute(MinecraftServer server) {
        SERVER.clear();
        // 1) valori fissi dai JSON (i tag valgono per tutti i loro oggetti; un id preciso vince sul tag)
        RAW.forEach((key, value) -> {
            if (key.startsWith("#")) {
                TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.parse(key.substring(1)));
                BuiltInRegistries.ITEM.getTagOrEmpty(tag).forEach(holder -> SERVER.putIfAbsent(holder.value(), value));
            }
        });
        RAW.forEach((key, value) -> {
            if (!key.startsWith("#")) {
                BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(key)).ifPresent(item -> SERVER.put(item, value));
            }
        });
        Map<Item, Long> fixed = new HashMap<>(SERVER);

        // 2) valori calcolati dalle ricette, in piu' passate (gli oggetti dipendono da altri oggetti).
        //    Ricette usate: banco da lavoro, fornaci (normale, altoforno, affumicatore, falo'), tagliapietre, forgiatura.
        //    Le ricette delle macchine NON contano: raddoppiano i minerali, e se contassero creerebbero Essenza dal nulla.
        java.util.List<Conversion> conversions = new java.util.ArrayList<>();
        for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            Conversion c = Conversion.of(holder.value(), server);
            if (c != null && !fixed.containsKey(c.result())) {
                conversions.add(c);
            }
        }
        for (int round = 0; round < 4; round++) {
            runPasses(conversions);
            // copie: solo per gli oggetti rimasti senza valore
            boolean copied = false;
            for (Map.Entry<String, String> copy : COPIES.entrySet()) {
                Item target = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(copy.getKey())).orElse(null);
                Item source = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(copy.getValue())).orElse(null);
                if (target != null && source != null && !SERVER.containsKey(target) && SERVER.containsKey(source)) {
                    SERVER.put(target, SERVER.get(source));
                    copied = true;
                }
            }
            if (!copied) {
                break;
            }
        }
        dirty = false;
        FutureTek.LOGGER.info("FutureTek: valori dell'Essenza calcolati per {} oggetti", SERVER.size());
    }

    private static void runPasses(java.util.List<Conversion> conversions) {
        for (int pass = 0; pass < 30; pass++) {
            boolean changed = false;
            for (Conversion c : conversions) {
                long total = 0;
                boolean complete = true;
                for (Ingredient ingredient : c.inputs()) {
                    long cheapest = Long.MAX_VALUE;
                    for (ItemStack option : ingredient.getItems()) {
                        Long v = SERVER.get(option.getItem());
                        if (option.hasCraftingRemainingItem()) {
                            // resta qualcosa nel banco (secchio vuoto, attrezzi...): conta solo la differenza
                            ItemStack rest = option.getCraftingRemainingItem();
                            long restValue = rest.isEmpty() ? 0 : SERVER.getOrDefault(rest.getItem(), 0L);
                            cheapest = Math.min(cheapest, Math.max(0, (v == null ? restValue : v) - restValue));
                            continue;
                        }
                        if (v != null && v > 0) {
                            cheapest = Math.min(cheapest, v);
                        }
                    }
                    if (cheapest == Long.MAX_VALUE) {
                        complete = false;
                        break;
                    }
                    total += cheapest;
                }
                if (!complete || total <= 0) {
                    continue;
                }
                long perItem = Math.max(1, total / c.count());
                Long old = SERVER.get(c.result());
                if (old == null || perItem < old) {
                    SERVER.put(c.result(), perItem);
                    changed = true;
                }
            }
            if (!changed) {
                break;
            }
        }
    }

    /** Una ricetta ridotta all'osso: ingredienti -> "count" copie di "result" */
    private record Conversion(java.util.List<Ingredient> inputs, Item result, int count) {
        static Conversion of(Recipe<?> recipe, MinecraftServer server) {
            java.util.List<Ingredient> inputs = new java.util.ArrayList<>();
            ItemStack result;
            if (recipe instanceof net.minecraft.world.item.crafting.SmithingTransformRecipe smithing) {
                // la forgiatura non elenca i suoi ingredienti: li leggiamo direttamente dalla ricetta
                for (String field : new String[]{"template", "base", "addition"}) {
                    Ingredient ingredient = readIngredient(smithing, field);
                    if (ingredient == null) {
                        return null;
                    }
                    inputs.add(ingredient);
                }
                result = recipe.getResultItem(server.registryAccess());
            } else if (recipe.getType() == RecipeType.CRAFTING
                    || recipe instanceof net.minecraft.world.item.crafting.AbstractCookingRecipe
                    || recipe.getType() == RecipeType.STONECUTTING) {
                for (Ingredient ingredient : recipe.getIngredients()) {
                    if (!ingredient.isEmpty()) {
                        inputs.add(ingredient);
                    }
                }
                result = recipe.getResultItem(server.registryAccess());
            } else {
                return null;
            }
            if (result.isEmpty() || inputs.isEmpty()) {
                return null;
            }
            return new Conversion(inputs, result.getItem(), result.getCount());
        }

        private static Ingredient readIngredient(Object recipe, String name) {
            try {
                java.lang.reflect.Field field = recipe.getClass().getDeclaredField(name);
                field.setAccessible(true);
                return field.get(recipe) instanceof Ingredient i ? i : null;
            } catch (ReflectiveOperationException | RuntimeException e) {
                return null;
            }
        }
    }

    /**
     * Oggetti che si possono SCIOGLIERE ma non CREARE con l'Essenza (tag futuretek:essence_not_creatable):
     * minerali, grezzi, polveri, frantumati, favi e api. Le macchine li moltiplicano (1 grezzo -> 3 lingotti):
     * se si potessero creare, il giro "crea grezzo -> macchine -> sciogli lingotti" darebbe Essenza infinita.
     */
    public static final TagKey<Item> NOT_CREATABLE =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(FutureTek.MODID, "essence_not_creatable"));

    /** true se l'oggetto si puo' imparare e creare (Tavolo di Sintesi, Replicatore) */
    public static boolean isCreatable(boolean clientSide, Item item) {
        return get(clientSide, item) > 0 && !item.builtInRegistryHolder().is(NOT_CREATABLE);
    }

    /** Essenza in piu' per ogni livello di incantesimo (anche nei libri incantati) */
    public static final long ENCHANTMENT_LEVEL_VALUE = 2048;

    /**
     * Valore di una pila reale: tiene conto di usura, incantesimi ed Essenza contenuta (cristalli).
     * Esempio: un piccone di ferro consumato a meta' vale meta'.
     */
    public static long stackValue(boolean clientSide, ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        long base = get(clientSide, stack.getItem());
        if (base > 0 && stack.isDamageableItem() && stack.getDamageValue() > 0) {
            base = Math.max(1, base * (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage());
        }
        long extra = 0;
        for (var component : new net.minecraft.core.component.DataComponentType[]{
                net.minecraft.core.component.DataComponents.ENCHANTMENTS,
                net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS}) {
            Object value = stack.get(component);
            if (value instanceof net.minecraft.world.item.enchantment.ItemEnchantments enchantments) {
                for (var entry : enchantments.entrySet()) {
                    extra += ENCHANTMENT_LEVEL_VALUE * entry.getIntValue();
                }
            }
        }
        if (stack.getItem() instanceof com.futuretek.item.EssenceCrystalItem) {
            extra += com.futuretek.item.EssenceCrystalItem.getStored(stack);
        }
        if (base <= 0 && extra <= 0) {
            return 0;
        }
        return (base + extra) * stack.getCount();
    }

    /** Oggetti del gioco che non hanno (ancora) un valore: per il comando /futuretek essenza mancanti */
    public static java.util.List<ResourceLocation> missing() {
        Map<Item, Long> values = serverValues();
        java.util.List<ResourceLocation> list = new java.util.ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item != net.minecraft.world.item.Items.AIR && !values.containsKey(item)) {
                list.add(BuiltInRegistries.ITEM.getKey(item));
            }
        }
        list.sort(java.util.Comparator.comparing(ResourceLocation::toString));
        return list;
    }

    /** Tabella da inviare ai client */
    public static Map<ResourceLocation, Long> exportForClient() {
        Map<ResourceLocation, Long> out = new HashMap<>();
        serverValues().forEach((item, v) -> out.put(BuiltInRegistries.ITEM.getKey(item), v));
        return out;
    }
}
