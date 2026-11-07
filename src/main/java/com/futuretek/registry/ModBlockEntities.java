package com.futuretek.registry;

import com.futuretek.FutureTek;
import com.futuretek.bee.ApiaryBlockEntity;
import com.futuretek.farm.FarmBlockEntities;
import com.futuretek.blockentity.AutoCrafterBlockEntity;
import com.futuretek.blockentity.BatteryBoxBlockEntity;
import com.futuretek.blockentity.ExtractorConduitBlockEntity;
import com.futuretek.blockentity.InductionRailBlockEntity;
import com.futuretek.blockentity.SortingConduitBlockEntity;
import com.futuretek.blockentity.TerminalBlockEntity;
import com.futuretek.blockentity.CombustionGeneratorBlockEntity;
import com.futuretek.blockentity.GeothermalGeneratorBlockEntity;
import com.futuretek.blockentity.ProcessingMachineBlockEntity;
import com.futuretek.blockentity.QuarryBlockEntity;
import com.futuretek.blockentity.ReactorBlockEntity;
import com.futuretek.blockentity.ReplicatorBlockEntity;
import com.futuretek.blockentity.StarCollectorBlockEntity;
import com.futuretek.blockentity.SolarPanelBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Tipi di BlockEntity: collegano un blocco al suo "cervello". */
public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, FutureTek.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CombustionGeneratorBlockEntity>> COMBUSTION_GENERATOR =
            BLOCK_ENTITIES.register("combustion_generator", () -> BlockEntityType.Builder.of(
                    CombustionGeneratorBlockEntity::new, ModBlocks.COMBUSTION_GENERATOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL =
            BLOCK_ENTITIES.register("solar_panel", () -> BlockEntityType.Builder.of(
                    SolarPanelBlockEntity::new, ModBlocks.SOLAR_PANEL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeothermalGeneratorBlockEntity>> GEOTHERMAL_GENERATOR =
            BLOCK_ENTITIES.register("geothermal_generator", () -> BlockEntityType.Builder.of(
                    GeothermalGeneratorBlockEntity::new, ModBlocks.GEOTHERMAL_GENERATOR.get()).build(null));

    /** Un solo tipo per i tre banchi accumulatori: livello e capacita' li legge dal blocco */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatteryBoxBlockEntity>> BATTERY_BOX =
            BLOCK_ENTITIES.register("battery_box", () -> BlockEntityType.Builder.of(BatteryBoxBlockEntity::new,
                    ModBlocks.BATTERY_BOX.get(), ModBlocks.BATTERY_BOX_2.get(), ModBlocks.BATTERY_BOX_3.get())
                    .build(null));

    /** Un solo tipo per tutte le macchine di lavorazione: la macchina la legge dal blocco */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProcessingMachineBlockEntity>> PROCESSING_MACHINE =
            BLOCK_ENTITIES.register("processing_machine", () -> BlockEntityType.Builder.of(ProcessingMachineBlockEntity::new,
                    ModBlocks.PROCESSING.values().stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StarCollectorBlockEntity>> STAR_COLLECTOR =
            BLOCK_ENTITIES.register("star_collector", () -> BlockEntityType.Builder.of(StarCollectorBlockEntity::new,
                    ModBlocks.STAR_COLLECTOR.get(), ModBlocks.STAR_COLLECTOR_2.get(), ModBlocks.STAR_COLLECTOR_3.get())
                    .build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReplicatorBlockEntity>> REPLICATOR =
            BLOCK_ENTITIES.register("replicator", () -> BlockEntityType.Builder.of(ReplicatorBlockEntity::new,
                    ModBlocks.REPLICATOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ExtractorConduitBlockEntity>> EXTRACTOR_CONDUIT =
            BLOCK_ENTITIES.register("extractor_conduit", () -> BlockEntityType.Builder.of(ExtractorConduitBlockEntity::new,
                    ModBlocks.EXTRACTOR_CONDUIT.get(), ModBlocks.FAST_EXTRACTOR_CONDUIT.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SortingConduitBlockEntity>> SORTING_CONDUIT =
            BLOCK_ENTITIES.register("sorting_conduit", () -> BlockEntityType.Builder.of(SortingConduitBlockEntity::new,
                    ModBlocks.SORTING_CONDUIT.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.futuretek.logistics.AnalyzerConduitBlockEntity>> ANALYZER_CONDUIT =
            BLOCK_ENTITIES.register("analyzer_conduit", () -> BlockEntityType.Builder.of(
                    com.futuretek.logistics.AnalyzerConduitBlockEntity::new, ModBlocks.ANALYZER_CONDUIT.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TerminalBlockEntity>> TERMINAL =
            BLOCK_ENTITIES.register("network_terminal", () -> BlockEntityType.Builder.of(TerminalBlockEntity::new,
                    ModBlocks.NETWORK_TERMINAL.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AutoCrafterBlockEntity>> AUTO_CRAFTER =
            BLOCK_ENTITIES.register("auto_crafter", () -> BlockEntityType.Builder.of(AutoCrafterBlockEntity::new,
                    ModBlocks.AUTO_CRAFTER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorBlockEntity>> REACTOR =
            BLOCK_ENTITIES.register("reactor_core", () -> BlockEntityType.Builder.of(ReactorBlockEntity::new,
                    ModBlocks.REACTOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<QuarryBlockEntity>> QUARRY =
            BLOCK_ENTITIES.register("quarry", () -> BlockEntityType.Builder.of(QuarryBlockEntity::new,
                    ModBlocks.QUARRY.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InductionRailBlockEntity>> INDUCTION_RAIL =
            BLOCK_ENTITIES.register("induction_rail", () -> BlockEntityType.Builder.of(InductionRailBlockEntity::new,
                    ModBlocks.INDUCTION_RAIL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FarmBlockEntities.GrowthAccelerator>> GROWTH_ACCELERATOR =
            BLOCK_ENTITIES.register("growth_accelerator", () -> BlockEntityType.Builder.of(
                    FarmBlockEntities.GrowthAccelerator::new, ModBlocks.GROWTH_ACCELERATOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FarmBlockEntities.Harvester>> HARVESTER =
            BLOCK_ENTITIES.register("harvester", () -> BlockEntityType.Builder.of(
                    FarmBlockEntities.Harvester::new, ModBlocks.HARVESTER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ApiaryBlockEntity>> APIARY =
            BLOCK_ENTITIES.register("apiary", () -> BlockEntityType.Builder.of(ApiaryBlockEntity::new,
                    ModBlocks.APIARY.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.futuretek.utility.IronFurnace.Entity>> IRON_FURNACE =
            BLOCK_ENTITIES.register("iron_furnace", () -> BlockEntityType.Builder.of(
                    com.futuretek.utility.IronFurnace.Entity::new, ModBlocks.IRON_FURNACE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.futuretek.storage.Storage.ChestEntity>> FT_CHEST =
            BLOCK_ENTITIES.register("chest", () -> BlockEntityType.Builder.of(com.futuretek.storage.Storage.ChestEntity::new,
                    ModBlocks.CHESTS.values().stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.futuretek.power.Generators.AdvancedGenerator>> ADVANCED_GENERATOR =
            BLOCK_ENTITIES.register("advanced_generator", () -> BlockEntityType.Builder.of(
                    com.futuretek.power.Generators.AdvancedGenerator::new,
                    ModBlocks.GENERATORS.get(com.futuretek.power.Generators.Kind.ADVANCED_GENERATOR).get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.futuretek.power.Generators.Passive>> PASSIVE_GENERATOR =
            BLOCK_ENTITIES.register("passive_generator", () -> BlockEntityType.Builder.of(
                    com.futuretek.power.Generators.Passive::new, ModBlocks.GENERATORS.entrySet().stream()
                            .filter(e -> e.getKey() != com.futuretek.power.Generators.Kind.ADVANCED_GENERATOR)
                            .map(e -> e.getValue().get()).toArray(Block[]::new)).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.futuretek.power.CharcoalKiln.KilnEntity>> CHARCOAL_KILN =
            BLOCK_ENTITIES.register("charcoal_kiln", () -> BlockEntityType.Builder.of(
                    com.futuretek.power.CharcoalKiln.KilnEntity::new, ModBlocks.CHARCOAL_KILN.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.futuretek.logistics.LogisticsCoreBlockEntity>> LOGISTICS_CORE =
            BLOCK_ENTITIES.register("logistics_core", () -> BlockEntityType.Builder.of(
                    com.futuretek.logistics.LogisticsCoreBlockEntity::new, ModBlocks.LOGISTICS_CORE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.futuretek.mining.MiningMole.MoleEntity>> MINING_MOLE =
            BLOCK_ENTITIES.register("mining_mole", () -> BlockEntityType.Builder.of(
                    com.futuretek.mining.MiningMole.MoleEntity::new, ModBlocks.MINING_MOLE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.futuretek.automation.HomeMachines.TreeFarm>> TREE_FARM =
            BLOCK_ENTITIES.register("tree_farm", () -> BlockEntityType.Builder.of(
                    com.futuretek.automation.HomeMachines.TreeFarm::new, ModBlocks.TREE_FARM.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.futuretek.automation.HomeMachines.Planter>> PLANTER =
            BLOCK_ENTITIES.register("planter", () -> BlockEntityType.Builder.of(
                    com.futuretek.automation.HomeMachines.Planter::new, ModBlocks.PLANTER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.futuretek.automation.HomeMachines.Fertilizer>> FERTILIZER =
            BLOCK_ENTITIES.register("fertilizer", () -> BlockEntityType.Builder.of(
                    com.futuretek.automation.HomeMachines.Fertilizer::new, ModBlocks.FERTILIZER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.futuretek.automation.HomeMachines.LampEntity>> ELECTRIC_LAMP =
            BLOCK_ENTITIES.register("electric_lamp", () -> BlockEntityType.Builder.of(
                    com.futuretek.automation.HomeMachines.LampEntity::new, ModBlocks.ELECTRIC_LAMP.get(), ModBlocks.LIGHT_PANEL.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.futuretek.automation.HomeMachines.SwitchEntity>> ENERGY_SWITCH =
            BLOCK_ENTITIES.register("energy_switch", () -> BlockEntityType.Builder.of(
                    com.futuretek.automation.HomeMachines.SwitchEntity::new, ModBlocks.ENERGY_SWITCH.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.futuretek.automation.NetherMachines.Enchanter>> ELECTRIC_ENCHANTER =
            BLOCK_ENTITIES.register("electric_enchanter", () -> BlockEntityType.Builder.of(
                    com.futuretek.automation.NetherMachines.Enchanter::new, ModBlocks.ELECTRIC_ENCHANTER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.futuretek.automation.NetherMachines.Incubator>> MOB_INCUBATOR =
            BLOCK_ENTITIES.register("mob_incubator", () -> BlockEntityType.Builder.of(
                    com.futuretek.automation.NetherMachines.Incubator::new, ModBlocks.MOB_INCUBATOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.futuretek.automation.NetherMachines.LaserTurret>> LASER_TURRET =
            BLOCK_ENTITIES.register("laser_turret", () -> BlockEntityType.Builder.of(
                    com.futuretek.automation.NetherMachines.LaserTurret::new, ModBlocks.LASER_TURRET.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.futuretek.robot.RobotParts.DockEntity>> ROBOT_DOCK =
            BLOCK_ENTITIES.register("robot_dock", () -> BlockEntityType.Builder.of(
                    com.futuretek.robot.RobotParts.DockEntity::new, ModBlocks.ROBOT_DOCK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.futuretek.nuclear.ReactorMaintainer>> REACTOR_MAINTAINER =
            BLOCK_ENTITIES.register("reactor_maintainer", () -> BlockEntityType.Builder.of(
                    com.futuretek.nuclear.ReactorMaintainer::new, ModBlocks.REACTOR_MAINTAINER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.futuretek.utility.Shelter.CoreEntity>> SHELTER_CORE =
            BLOCK_ENTITIES.register("shelter_core", () -> BlockEntityType.Builder.of(
                    com.futuretek.utility.Shelter.CoreEntity::new, ModBlocks.SHELTER_CORE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.futuretek.automation.Enchanting.Researcher>> ENCHANT_RESEARCHER =
            BLOCK_ENTITIES.register("enchant_researcher", () -> BlockEntityType.Builder.of(
                    com.futuretek.automation.Enchanting.Researcher::new, ModBlocks.ENCHANT_RESEARCHER.get()).build(null));

    private ModBlockEntities() {
    }
}
