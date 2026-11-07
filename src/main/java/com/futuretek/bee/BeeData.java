package com.futuretek.bee;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.futuretek.FutureTek;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Prodotti e incroci delle api, letti da data/<mod>/bee_data/*.json:
 * {
 *   "products": { "metallic": [ { "item": "futuretek:metallic_comb", "chance": 0.4 } ] },
 *   "mutations": [ { "parents": ["mineral", "meadow"], "result": "metallic", "chance": 0.1 } ]
 * }
 */
public final class BeeData {
    public record Product(Item item, float chance) {
    }

    public record Mutation(BeeSpecies a, BeeSpecies b, BeeSpecies result, float chance) {
        boolean matches(BeeSpecies x, BeeSpecies y) {
            return (a == x && b == y) || (a == y && b == x);
        }
    }

    private static final Map<BeeSpecies, List<Product>> PRODUCTS = new EnumMap<>(BeeSpecies.class);
    private static final List<Mutation> MUTATIONS = new ArrayList<>();

    private BeeData() {
    }

    public static List<Product> products(BeeSpecies species) {
        return PRODUCTS.getOrDefault(species, List.of());
    }

    public static List<Mutation> mutations() {
        return MUTATIONS;
    }

    /** Specie della nuova regina: con una certa probabilita' nasce una specie nuova, altrimenti quella della regina */
    public static BeeSpecies offspring(BeeSpecies queen, BeeSpecies drone, RandomSource random) {
        for (Mutation m : MUTATIONS) {
            if (m.matches(queen, drone) && random.nextFloat() < m.chance()) {
                return m.result();
            }
        }
        return random.nextBoolean() ? queen : drone;
    }

    public static List<ItemStack> rollProducts(BeeSpecies species, RandomSource random) {
        List<ItemStack> out = new ArrayList<>();
        for (Product p : products(species)) {
            if (random.nextFloat() < p.chance()) {
                out.add(new ItemStack(p.item()));
            }
        }
        return out;
    }

    /** Legge i file JSON all'avvio e con /reload */
    public static class Loader extends SimpleJsonResourceReloadListener {
        public Loader() {
            super(new Gson(), "bee_data");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            PRODUCTS.clear();
            MUTATIONS.clear();
            files.forEach((file, json) -> {
                try {
                    JsonObject root = json.getAsJsonObject();
                    if (root.has("products")) {
                        root.getAsJsonObject("products").entrySet().forEach(e -> {
                            List<Product> list = PRODUCTS.computeIfAbsent(BeeSpecies.byId(e.getKey()), k -> new ArrayList<>());
                            e.getValue().getAsJsonArray().forEach(p -> {
                                JsonObject o = p.getAsJsonObject();
                                BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(o.get("item").getAsString()))
                                        .ifPresent(item -> list.add(new Product(item, o.get("chance").getAsFloat())));
                            });
                        });
                    }
                    if (root.has("mutations")) {
                        root.getAsJsonArray("mutations").forEach(m -> {
                            JsonObject o = m.getAsJsonObject();
                            var parents = o.getAsJsonArray("parents");
                            MUTATIONS.add(new Mutation(BeeSpecies.byId(parents.get(0).getAsString()),
                                    BeeSpecies.byId(parents.get(1).getAsString()),
                                    BeeSpecies.byId(o.get("result").getAsString()), o.get("chance").getAsFloat()));
                        });
                    }
                } catch (Exception e) {
                    FutureTek.LOGGER.error("FutureTek: file delle api non valido: {}", file, e);
                }
            });
        }
    }
}
