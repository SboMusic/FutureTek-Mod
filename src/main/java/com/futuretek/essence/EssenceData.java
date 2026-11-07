package com.futuretek.essence;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * Cio' che ogni giocatore possiede nel sistema dell'Essenza:
 * - essence: quanta Essenza ha accumulato
 * - known: gli oggetti che ha "imparato" sciogliendoli nel Tavolo di Sintesi
 * E' "immutabile": per cambiarla se ne crea una nuova (cosi' il gioco la sincronizza col client).
 */
public record EssenceData(long essence, List<ResourceLocation> known) {
    public static final EssenceData EMPTY = new EssenceData(0, List.of());

    public static final Codec<EssenceData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("essence").forGetter(EssenceData::essence),
            ResourceLocation.CODEC.listOf().fieldOf("known").forGetter(EssenceData::known)
    ).apply(i, EssenceData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EssenceData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, EssenceData::essence,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), EssenceData::known,
            EssenceData::new);

    public EssenceData withEssence(long value) {
        return new EssenceData(Math.max(0, value), known);
    }

    public EssenceData learn(ResourceLocation id) {
        if (known.contains(id)) {
            return this;
        }
        List<ResourceLocation> list = new ArrayList<>(known);
        list.add(id);
        return new EssenceData(essence, List.copyOf(list));
    }
}
