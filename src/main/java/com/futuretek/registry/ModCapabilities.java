package com.futuretek.registry;

import com.futuretek.blockentity.MachineBlockEntity;
import com.futuretek.energy.network.CableEnergyHandler;
import com.futuretek.item.EnergyItem;

import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.ComponentEnergyStorage;

/**
 * "Capability" = il modo standard di NeoForge per dire "questo blocco ha energia / oggetti / fluidi".
 * Grazie a queste, cavi, tubi e macchine di ALTRE mod possono collegarsi alle nostre.
 */
public final class ModCapabilities {
    private ModCapabilities() {
    }

    public static void register(RegisterCapabilitiesEvent event) {
        // Energia delle macchine
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.COMBUSTION_GENERATOR.get(),
                MachineBlockEntity::getEnergyHandler);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.SOLAR_PANEL.get(),
                MachineBlockEntity::getEnergyHandler);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.GEOTHERMAL_GENERATOR.get(),
                MachineBlockEntity::getEnergyHandler);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.BATTERY_BOX.get(),
                MachineBlockEntity::getEnergyHandler);

        // Inventari (per tramogge e tubi)
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.COMBUSTION_GENERATOR.get(),
                (be, side) -> be.getItems());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.BATTERY_BOX.get(),
                (be, side) -> be.getItems());

        // Macchine di lavorazione: energia in ingresso, oggetti (ingresso / prodotti) per tubi e tramogge
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.PROCESSING_MACHINE.get(),
                MachineBlockEntity::getEnergyHandler);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.PROCESSING_MACHINE.get(),
                (be, side) -> be.getAutomationItems());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.PROCESSING_MACHINE.get(),
                (be, side) -> be.getTank());

        // Replicatore: materiali in ingresso, copie in uscita
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.REPLICATOR.get(),
                (be, side) -> be.getAutomationItems());

        // Reattore ed escavatore
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.REACTOR.get(),
                MachineBlockEntity::getEnergyHandler);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.REACTOR.get(),
                (be, side) -> be.getItems());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.QUARRY.get(),
                MachineBlockEntity::getEnergyHandler);

        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.INDUCTION_RAIL.get(),
                MachineBlockEntity::getEnergyHandler);

        // Agricoltura e api
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.GROWTH_ACCELERATOR.get(),
                MachineBlockEntity::getEnergyHandler);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.HARVESTER.get(),
                MachineBlockEntity::getEnergyHandler);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.APIARY.get(),
                (be, side) -> be.getAutomation());

        // Serbatoio di lava del generatore geotermico
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.GEOTHERMAL_GENERATOR.get(),
                (be, side) -> be.getTank());

        // Cavi: inoltrano l'energia alla loro rete
        event.registerBlock(Capabilities.EnergyStorage.BLOCK,
                (level, pos, state, be, side) -> new CableEnergyHandler(level, pos, side),
                ModBlocks.CABLES.values().stream().map(holder -> (Block) holder.get()).toArray(Block[]::new));

        // Fornace di ferro: tramogge e condotti (sopra = ingresso, lato = combustibile, sotto = prodotto)
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.IRON_FURNACE.get(),
                (be, side) -> side == null ? new net.neoforged.neoforge.items.wrapper.InvWrapper(be)
                        : new net.neoforged.neoforge.items.wrapper.SidedInvWrapper(be, side));

        // Fase 10: generatori e carbonaia
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.ADVANCED_GENERATOR.get(),
                MachineBlockEntity::getEnergyHandler);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.ADVANCED_GENERATOR.get(),
                (be, side) -> be.getItems());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.PASSIVE_GENERATOR.get(),
                MachineBlockEntity::getEnergyHandler);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.CHARCOAL_KILN.get(),
                (be, side) -> be.handlerFor(side));

        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.LOGISTICS_CORE.get(),
                MachineBlockEntity::getEnergyHandler);

        // Fase 15: manutentore del reattore, camere che passano oggetti ed energia al nucleo
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.REACTOR_MAINTAINER.get(),
                MachineBlockEntity::getEnergyHandler);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.REACTOR_MAINTAINER.get(), (be, side) -> be.automation());
        event.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, be, side) -> {
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
                if (level.getBlockEntity(pos.relative(d)) instanceof com.futuretek.blockentity.ReactorBlockEntity r) {
                    return r.automation();
                }
            }
            return null;
        }, ModBlocks.REACTOR_CHAMBER.get());

        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.ENCHANT_RESEARCHER.get(),
                MachineBlockEntity::getEnergyHandler);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.ENCHANT_RESEARCHER.get(), (be, side) -> be.automation());

        // Fasi 12-14
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.TREE_FARM.get(),
                MachineBlockEntity::getEnergyHandler);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.TREE_FARM.get(), (be, side) -> be.automation());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.PLANTER.get(),
                MachineBlockEntity::getEnergyHandler);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.PLANTER.get(), (be, side) -> be.automation());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.FERTILIZER.get(),
                MachineBlockEntity::getEnergyHandler);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.FERTILIZER.get(), (be, side) -> be.automation());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.ELECTRIC_ENCHANTER.get(),
                MachineBlockEntity::getEnergyHandler);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.ELECTRIC_ENCHANTER.get(), (be, side) -> be.automation());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.MOB_INCUBATOR.get(),
                MachineBlockEntity::getEnergyHandler);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.MOB_INCUBATOR.get(), (be, side) -> be.automation());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.LASER_TURRET.get(),
                MachineBlockEntity::getEnergyHandler);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.LASER_TURRET.get(), (be, side) -> be.automation());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.ELECTRIC_LAMP.get(),
                MachineBlockEntity::getEnergyHandler);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.ENERGY_SWITCH.get(),
                MachineBlockEntity::getEnergyHandler);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.ROBOT_DOCK.get(),
                MachineBlockEntity::getEnergyHandler);

        // Talpa mineraria: le tramogge possono metterci combustibile
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.MINING_MOLE.get(), (be, side) -> be.getItems());

        // Bauli FutureTek
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.FT_CHEST.get(), (be, side) -> be.getItems());

        // Amuleto delle Maree: acqua infinita per le macchine (es. bagno chimico)
        event.registerItem(Capabilities.FluidHandler.ITEM,
                (stack, ctx) -> new com.futuretek.alchemy.AlchemyItems.InfiniteWater(stack), ModItems.TIDE_AMULET.get());

        // Celle di energia portatili
        // tutti gli oggetti con energia (celle, esoarmatura, zaino a reazione...)
        for (var entry : ModItems.ITEMS.getEntries()) {
            if (entry.get() instanceof EnergyItem energyItem) {
                event.registerItem(Capabilities.EnergyStorage.ITEM,
                        (stack, ctx) -> new ComponentEnergyStorage(stack, ModDataComponents.ENERGY.get(),
                                energyItem.getCapacity(), energyItem.getRate(), energyItem.getRate()),
                        entry.get());
            }
        }
    }
}
