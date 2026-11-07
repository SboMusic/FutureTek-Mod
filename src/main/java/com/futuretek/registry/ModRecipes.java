package com.futuretek.registry;

import java.util.EnumMap;
import java.util.Map;

import com.futuretek.FutureTek;
import com.futuretek.recipe.ProcessingKind;
import com.futuretek.recipe.ProcessingRecipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Tipi di ricetta delle macchine (es. "futuretek:crushing") e i loro lettori JSON. */
public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, FutureTek.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, FutureTek.MODID);

    public static final Map<ProcessingKind, DeferredHolder<RecipeType<?>, RecipeType<ProcessingRecipe>>> TYPES =
            new EnumMap<>(ProcessingKind.class);
    public static final Map<ProcessingKind, DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ProcessingRecipe>>> SERIALIZERS =
            new EnumMap<>(ProcessingKind.class);

    static {
        for (ProcessingKind kind : ProcessingKind.values()) {
            TYPES.put(kind, RECIPE_TYPES.register(kind.id,
                    () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(FutureTek.MODID, kind.id))));
            SERIALIZERS.put(kind, RECIPE_SERIALIZERS.register(kind.id, () -> new ProcessingRecipe.Serializer(kind)));
        }
    }

    private ModRecipes() {
    }
}
