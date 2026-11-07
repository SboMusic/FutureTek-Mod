package com.futuretek.item;

import com.futuretek.registry.ModDataComponents;

import net.minecraft.world.item.ItemStack;

/**
 * Oggetti che contengono energia (celle, esoarmatura, zaino a reazione).
 * Tutti si ricaricano nel banco accumulatore e mostrano la barretta della carica.
 */
public interface EnergyItem {
    int getCapacity();

    int getRate();

    static int getEnergy(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ENERGY.get(), 0);
    }

    static void setEnergy(ItemStack stack, int value) {
        stack.set(ModDataComponents.ENERGY.get(), Math.max(0, value));
    }

    /** Consuma energia dall'oggetto; restituisce true se ce n'era abbastanza */
    static boolean use(ItemStack stack, int amount) {
        int energy = getEnergy(stack);
        if (energy < amount) {
            return false;
        }
        setEnergy(stack, energy - amount);
        return true;
    }
}
