package com.futuretek.recipe;

import com.futuretek.registry.ModRecipes;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * I tipi di lavorazione con ricette proprie di FutureTek.
 * (Il forno elettrico usa invece le normali ricette della fornace di Minecraft.)
 */
public enum ProcessingKind {
    CRUSHING("crushing"),
    COMPRESSING("compressing"),
    EXTRACTING("extracting"),
    WIRE_PRESSING("wire_pressing"),
    WASHING("washing"),
    CENTRIFUGING("centrifuging");

    public final String id;

    ProcessingKind(String id) {
        this.id = id;
    }

    public RecipeType<ProcessingRecipe> type() {
        return ModRecipes.TYPES.get(this).get();
    }

    public RecipeSerializer<ProcessingRecipe> serializer() {
        return ModRecipes.SERIALIZERS.get(this).get();
    }
}
