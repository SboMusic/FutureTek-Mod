package com.futuretek.logistics;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.futuretek.FutureTek;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Animazione degli oggetti che viaggiano nei tubi.
 * Il server calcola il percorso (baule di partenza -> tubi -> baule di arrivo) e lo manda ai giocatori vicini;
 * il client disegna l'oggetto che scorre lungo il percorso. E' solo grafica: lo spostamento vero e' immediato.
 */
public final class ItemTransit {
    private ItemTransit() {
    }

    /** Lunghezza massima di un percorso animato (in blocchi) */
    public static final int MAX_PATH = 96;

    /** Pacchetto server -> client: un oggetto che parte e il suo percorso */
    public record Payload(ItemStack item, List<BlockPos> path) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FutureTek.MODID, "item_transit"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, Payload::item,
                BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_PATH + 2)), Payload::path,
                Payload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Chiamato dalla rete dopo un vero spostamento */
    static void animate(Level level, ItemNetwork network, @Nullable BlockPos sourcePos, ItemNetwork.Endpoint target,
            ItemStack moved) {
        if (!(level instanceof ServerLevel server) || sourcePos == null || moved.isEmpty()) {
            return;
        }
        BlockPos startConduit = null;
        for (ItemNetwork.Endpoint e : network.getEndpoints()) {
            if (e.pos().equals(sourcePos)) {
                startConduit = e.conduit();
                break;
            }
        }
        if (startConduit == null) {
            for (Direction dir : Direction.values()) {
                if (network.getConduits().contains(sourcePos.relative(dir))) {
                    startConduit = sourcePos.relative(dir);
                    break;
                }
            }
        }
        if (startConduit == null) {
            return;
        }
        List<BlockPos> tubes = path(network, startConduit, target.conduit());
        if (tubes == null) {
            return;
        }
        List<BlockPos> full = new ArrayList<>(tubes.size() + 2);
        full.add(sourcePos);
        full.addAll(tubes);
        full.add(target.pos());
        BlockPos mid = tubes.get(tubes.size() / 2);
        PacketDistributor.sendToPlayersNear(server, null, mid.getX(), mid.getY(), mid.getZ(), 48,
                new Payload(moved.copyWithCount(1), full));
    }

    /** Percorso piu' breve tra due tubi della rete (ricerca in ampiezza) */
    @Nullable
    static List<BlockPos> path(ItemNetwork network, BlockPos from, BlockPos to) {
        if (!network.getConduits().contains(from)) {
            return null;
        }
        Map<BlockPos, BlockPos> parent = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(from);
        parent.put(from, from);
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            if (current.equals(to)) {
                List<BlockPos> list = new ArrayList<>();
                for (BlockPos p = to; ; p = parent.get(p)) {
                    list.add(0, p);
                    if (p.equals(from)) {
                        break;
                    }
                }
                return list.size() <= MAX_PATH ? list : null;
            }
            for (Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);
                if (network.getConduits().contains(next) && !parent.containsKey(next)) {
                    parent.put(next, current);
                    queue.add(next);
                }
            }
        }
        return null;
    }
}
