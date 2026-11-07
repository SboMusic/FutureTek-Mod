package com.futuretek.logistics;

import java.util.List;

import com.futuretek.FutureTek;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Pacchetti tra client e server per il Terminale di rete. */
public final class TerminalPayloads {
    private TerminalPayloads() {
    }

    /** Una voce dell'elenco: oggetto (quantita' 1) + quanti ce ne sono nella rete */
    public record Entry(ItemStack item, long count) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, Entry::item,
                ByteBufCodecs.VAR_LONG, Entry::count,
                Entry::new);
    }

    /** Server -> client: contenuto della rete e oggetti fabbricabili */
    public record Contents(List<Entry> entries, List<ItemStack> craftables) implements CustomPacketPayload {
        public static final Type<Contents> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(FutureTek.MODID, "terminal_contents"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Contents> STREAM_CODEC = StreamCodec.composite(
                Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), Contents::entries,
                ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), Contents::craftables,
                Contents::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: "dammi questo oggetto" oppure "fabbricalo" */
    public record Request(ItemStack item, int amount, boolean craft) implements CustomPacketPayload {
        public static final Type<Request> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(FutureTek.MODID, "terminal_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, Request::item,
                ByteBufCodecs.VAR_INT, Request::amount,
                ByteBufCodecs.BOOL, Request::craft,
                Request::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
