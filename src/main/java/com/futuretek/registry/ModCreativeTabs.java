package com.futuretek.registry;

import com.futuretek.FutureTek;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Le schede dell'inventario creativo di FutureTek.
 */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FutureTek.MODID);

    /** "FutureTek - Risorse": contiene automaticamente TUTTI gli oggetti della mod, nell'ordine di registrazione. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> RESOURCES = TABS.register("resources",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.futuretek.resources"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> ModItems.INGOT.get(Metal.TIN).get().getDefaultInstance())
                    .displayItems((parameters, output) ->
                            ModItems.ITEMS.getEntries().forEach(item -> output.accept(item.get())))
                    .build());

    private ModCreativeTabs() {
    }
}
