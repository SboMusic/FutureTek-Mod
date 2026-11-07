package com.futuretek.essence;

import com.futuretek.FutureTek;
import com.futuretek.menu.SynthesisTableMenu;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Pacchetto client -> server: "nel Tavolo di Sintesi voglio creare questo oggetto".
 * Il server controlla tutto da solo (oggetto conosciuto, Essenza sufficiente, tavolo aperto).
 */
public record SynthesisRequestPayload(ResourceLocation item, boolean wholeStack) implements CustomPacketPayload {
    public static final Type<SynthesisRequestPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(FutureTek.MODID, "synthesis_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SynthesisRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, SynthesisRequestPayload::item,
            ByteBufCodecs.BOOL, SynthesisRequestPayload::wholeStack,
            SynthesisRequestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Eseguito sul server */
    public void handle(Player player) {
        if (!(player.containerMenu instanceof SynthesisTableMenu)) {
            return;
        }
        BuiltInRegistries.ITEM.getOptional(item).ifPresent(i ->
                EssenceManager.create(player, i, wholeStack ? i.getDefaultMaxStackSize() : 1));
    }
}
