package com.futuretek.menu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.futuretek.blockentity.AutoCrafterBlockEntity;
import com.futuretek.logistics.CraftingPlanner;
import com.futuretek.logistics.ItemNetwork;
import com.futuretek.logistics.ItemNetworkManager;
import com.futuretek.logistics.TerminalPayloads;
import com.futuretek.registry.ModBlocks;
import com.futuretek.registry.ModMenus;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Terminale di rete: sul server invia ogni mezzo secondo l'elenco degli oggetti della rete al giocatore;
 * riceve le richieste ("dammi" / "fabbrica"). Lo Shift+click dall'inventario mette l'oggetto nella rete.
 */
public class TerminalMenu extends AbstractContainerMenu {
    public static final int INVENTORY_TOP = 140;

    private final ContainerLevelAccess access;
    private final BlockPos pos;
    private final Player player;
    private int ticks;

    public TerminalMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos());
    }

    public TerminalMenu(int id, Inventory inventory, BlockPos pos) {
        super(ModMenus.TERMINAL.get(), id);
        this.pos = pos;
        this.player = inventory.player;
        this.access = ContainerLevelAccess.create(player.level(), pos);
        GhostMenus.addPlayerInventory(inventory, this::addSlot, INVENTORY_TOP);
    }

    private ItemNetwork network() {
        return ItemNetworkManager.getAdjacent(player.level(), pos);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (player instanceof ServerPlayer serverPlayer && ticks++ % 10 == 0) {
            sendContents(serverPlayer);
        }
    }

    private void sendContents(ServerPlayer target) {
        ItemNetwork network = network();
        List<TerminalPayloads.Entry> entries = new ArrayList<>();
        List<ItemStack> craftables = new ArrayList<>();
        if (network != null) {
            for (Map.Entry<ItemStack, Long> e : network.contents()) {
                entries.add(new TerminalPayloads.Entry(e.getKey(), e.getValue()));
            }
            entries.sort(Comparator.comparingLong(TerminalPayloads.Entry::count).reversed());
            for (AutoCrafterBlockEntity crafter : CraftingPlanner.crafters(network)) {
                craftables.add(crafter.result().copyWithCount(1));
            }
        }
        PacketDistributor.sendToPlayer(target, new TerminalPayloads.Contents(entries, craftables));
    }

    /** Richiesta dal client (sul server) */
    public void handleRequest(ServerPlayer requester, TerminalPayloads.Request request) {
        ItemNetwork network = network();
        if (network == null || request.item().isEmpty() || request.amount() <= 0) {
            return;
        }
        if (request.craft()) {
            CraftingPlanner.Plan plan = CraftingPlanner.request(network, request.item(), request.amount());
            if (plan.ok()) {
                requester.displayClientMessage(Component.translatable("message.futuretek.craft_started",
                        request.amount(), request.item().getHoverName()).withStyle(ChatFormatting.GREEN), true);
            } else {
                Component missing = Component.empty();
                for (ItemStack m : plan.missing()) {
                    missing = missing.copy().append(" " + m.getCount() + "x ").append(m.getHoverName());
                }
                requester.displayClientMessage(Component.translatable("message.futuretek.craft_missing", missing)
                        .withStyle(ChatFormatting.RED), false);
            }
            return;
        }
        int amount = Math.min(request.amount(), request.item().getMaxStackSize());
        ItemStack taken = network.extractExact(request.item(), amount, false);
        if (!taken.isEmpty() && !requester.getInventory().add(taken)) {
            requester.drop(taken, false);
        }
        sendContents(requester);
    }

    /** Shift+click su un oggetto dell'inventario: lo mette nella rete */
    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        ItemNetwork network = network();
        if (network == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack rest = network.insert(slot.getItem(), null, false);
        slot.set(rest);
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player p) {
        return stillValid(access, p, ModBlocks.NETWORK_TERMINAL.get());
    }
}
