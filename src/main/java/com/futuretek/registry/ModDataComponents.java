package com.futuretek.registry;

import com.futuretek.FutureTek;
import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * "Componenti dati": informazioni salvate dentro un oggetto (ItemStack).
 * Esempio: quanta energia c'e' in una cella portatile.
 */
public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, FutureTek.MODID);

    /** Energia contenuta in un oggetto (TU) */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY =
            COMPONENTS.registerComponentType("energy",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Essenza contenuta in un Cristallo d'Essenza */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> ESSENCE =
            COMPONENTS.registerComponentType("essence",
                    builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    /** Specie di un'ape (regina o fuco) */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> BEE_SPECIES =
            COMPONENTS.registerComponentType("bee_species",
                    builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));
    /** Cicli di vita rimasti a una regina */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BEE_LIFE =
            COMPONENTS.registerComponentType("bee_life",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Oggetti alchemici accesi/spenti; attrezzi con lo scavo ad area attivo */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> ACTIVE =
            COMPONENTS.registerComponentType("active",
                    builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
    /** Punti esperienza conservati nella Gemma della Memoria */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> STORED_XP =
            COMPONENTS.registerComponentType("stored_xp",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Livello di carica (Catalizzatore del Vuoto: 1-4) */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CHARGE =
            COMPONENTS.registerComponentType("charge",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    private ModDataComponents() {
    }
}
