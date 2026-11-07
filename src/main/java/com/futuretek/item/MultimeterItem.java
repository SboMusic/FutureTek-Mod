package com.futuretek.item;

import com.futuretek.block.CableBlock;
import com.futuretek.energy.EnergyUtil;
import com.futuretek.energy.network.EnergyNetwork;
import com.futuretek.energy.network.EnergyNetworkManager;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Multimetro: click destro su un blocco per leggere l'energia.
 * Su un cavo mostra i dati della rete (cavi, collegamenti, energia passata nell'ultimo tick).
 * Funziona anche con le macchine di altre mod.
 */
public class MultimeterItem extends Item {
    public MultimeterItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        BlockPos pos = context.getClickedPos();
        Component message;
        if (level.getBlockState(pos).getBlock() instanceof CableBlock) {
            EnergyNetwork network = EnergyNetworkManager.get(level, pos);
            message = network == null ? Component.translatable("message.futuretek.multimeter.no_energy")
                    : Component.translatable("message.futuretek.multimeter.network",
                            network.getCableCount(), network.getEndpointCount(),
                            EnergyUtil.format(network.getCapacity()),
                            EnergyUtil.format(network.getTransferredLastTick()));
        } else {
            IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
            if (storage == null) {
                storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, context.getClickedFace());
            }
            message = storage == null ? Component.translatable("message.futuretek.multimeter.no_energy")
                    : Component.translatable("message.futuretek.energy",
                            EnergyUtil.format(storage.getEnergyStored()),
                            EnergyUtil.format(storage.getMaxEnergyStored()));
        }
        player.displayClientMessage(message, false);
        return InteractionResult.CONSUME;
    }
}
