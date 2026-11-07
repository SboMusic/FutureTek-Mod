package com.futuretek.essence;

import java.util.HashMap;
import java.util.Map;

import com.futuretek.FutureTek;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Pacchetto server -> client con la tabella dei valori dell'Essenza. */
public record EssenceValuesPayload(Map<ResourceLocation, Long> values) implements CustomPacketPayload {
    public static final Type<EssenceValuesPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(FutureTek.MODID, "essence_values"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EssenceValuesPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.VAR_LONG),
            EssenceValuesPayload::values,
            EssenceValuesPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
