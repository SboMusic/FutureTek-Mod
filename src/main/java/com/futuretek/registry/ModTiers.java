package com.futuretek.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;

/**
 * Livelli degli strumenti (come legno/pietra/ferro/diamante in Minecraft).
 *   uses   = utilizzi prima di rompersi (ferro 250, diamante 1561)
 *   speed  = velocita' di scavo (ferro 6, diamante 8)
 *   damage = danno extra (ferro 2, diamante 3)
 */
public final class ModTiers {
    /** Bronzo: scava come il ferro, dura un po' di piu', si incanta bene */
    public static final Tier BRONZE = new SimpleTier(BlockTags.INCORRECT_FOR_IRON_TOOL,
            350, 6.5F, 2.0F, 12,
            () -> Ingredient.of(ItemTags.create(ResourceLocation.parse("c:ingots/bronze"))));

    /** Acciaio: molto resistente, tra ferro e diamante */
    public static final Tier STEEL = new SimpleTier(BlockTags.INCORRECT_FOR_IRON_TOOL,
            900, 7.0F, 2.5F, 10,
            () -> Ingredient.of(ItemTags.create(ResourceLocation.parse("c:ingots/steel"))));

    private ModTiers() {
    }
}
