package com.futuretek.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Attrezzo da banco da lavoro (martello, tagliacavi): si usa insieme a un materiale
 * (es. martello + lingotto -> piastra). Dopo ogni lavorazione NON sparisce:
 * torna indietro con un punto di usura in meno, finche' non si rompe.
 */
public class CraftingToolItem extends Item {
    public CraftingToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        ItemStack used = stack.copy();
        used.setDamageValue(used.getDamageValue() + 1);
        if (used.getDamageValue() >= used.getMaxDamage()) {
            return ItemStack.EMPTY; // l attrezzo si e rotto
        }
        return used;
    }
}
