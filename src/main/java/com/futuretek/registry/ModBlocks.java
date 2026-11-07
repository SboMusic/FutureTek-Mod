package com.futuretek.registry;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.futuretek.FutureTek;
import com.futuretek.bee.BeeBlocks;
import com.futuretek.block.BatteryBoxBlock;
import com.futuretek.block.CableBlock;
import com.futuretek.block.ConduitBlock;
import com.futuretek.block.LogicGateBlock;
import com.futuretek.block.LogisticsBlocks;
import com.futuretek.block.EssenceBlocks;
import com.futuretek.block.StarCollectorBlock;
import com.futuretek.block.MachineBlock;
import com.futuretek.block.MachineBlocks;
import com.futuretek.block.ProcessingMachineBlock;
import com.futuretek.block.ResinLogBlock;
import com.futuretek.machine.ProcessingMachine;
import com.futuretek.rail.RailBlocks;
import com.futuretek.world.EdgeAnchorBlock;
import com.futuretek.world.VoltiteClusterBlock;
import com.futuretek.energy.EnergyTier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Elenco di tutti i BLOCCHI di FutureTek.
 * Per ogni blocco registriamo anche il suo "BlockItem", cioe' l'oggetto
 * che tieni in mano e che piazzi nel mondo.
 */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FutureTek.MODID);

    /** Minerali nella pietra e nella deepslate (solo metalli "da minerale") */
    public static final Map<Metal, DeferredBlock<Block>> ORE = new EnumMap<>(Metal.class);
    public static final Map<Metal, DeferredBlock<Block>> DEEPSLATE_ORE = new EnumMap<>(Metal.class);
    /** Blocchi di metallo (9 lingotti) */
    public static final Map<Metal, DeferredBlock<Block>> STORAGE = new EnumMap<>(Metal.class);
    /** Blocchi di metallo grezzo (9 grezzi) */
    public static final Map<Metal, DeferredBlock<Block>> RAW_STORAGE = new EnumMap<>(Metal.class);

    /** Voltite: cristallo energetico originale di FutureTek. Rilascia frammenti ed esperienza. */
    public static final DeferredBlock<Block> VOLTITE_ORE = registerWithItem("voltite_ore",
            props -> new DropExperienceBlock(UniformInt.of(2, 5), props), stoneOre());
    public static final DeferredBlock<Block> DEEPSLATE_VOLTITE_ORE = registerWithItem("deepslate_voltite_ore",
            props -> new DropExperienceBlock(UniformInt.of(2, 5), props), deepslateOre());

    // --- Albero della resina ---
    /** L'albero che cresce dall'alberello: definito in data/futuretek/worldgen/configured_feature/resin_tree.json */
    public static final TreeGrower RESIN_TREE_GROWER = new TreeGrower(FutureTek.MODID + ":resin", Optional.empty(),
            Optional.of(ResourceKey.create(Registries.CONFIGURED_FEATURE,
                    ResourceLocation.fromNamespaceAndPath(FutureTek.MODID, "resin_tree"))),
            Optional.empty());

    public static final DeferredBlock<Block> RESIN_LOG = registerWithItem("resin_log", ResinLogBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_BROWN)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava());

    public static final DeferredBlock<Block> RESIN_PLANKS = registerWithItem("resin_planks",
            props -> new Block(props) {
                @Override
                public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
                    return 5;
                }

                @Override
                public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
                    return 20;
                }
            },
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava());

    public static final DeferredBlock<Block> RESIN_LEAVES = registerWithItem("resin_leaves",
            props -> new LeavesBlock(props) {
                @Override
                public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
                    return 60;
                }

                @Override
                public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
                    return 30;
                }
            },
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .strength(0.2F)
                    .randomTicks()
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .isValidSpawn((state, level, pos, type) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)
                    .isRedstoneConductor((state, level, pos) -> false));

    public static final DeferredBlock<Block> RESIN_SAPLING = registerWithItem("resin_sapling",
            props -> new SaplingBlock(RESIN_TREE_GROWER, props),
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .pushReaction(PushReaction.DESTROY));

    static {
        for (Metal metal : Metal.values()) {
            if (metal.fromOre) {
                ORE.put(metal, registerWithItem(metal.id + "_ore",
                        props -> new DropExperienceBlock(ConstantInt.of(0), props), stoneOre()));
                DEEPSLATE_ORE.put(metal, registerWithItem("deepslate_" + metal.id + "_ore",
                        props -> new DropExperienceBlock(ConstantInt.of(0), props), deepslateOre()));
            }
        }
        for (Metal metal : Metal.values()) {
            STORAGE.put(metal, registerWithItem(metal.id + "_block", Block::new, BlockBehaviour.Properties.of()
                    .mapColor(metal.mapColor)
                    .instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.METAL)));
            if (metal.fromOre) {
                RAW_STORAGE.put(metal, registerWithItem("raw_" + metal.id + "_block", Block::new,
                        BlockBehaviour.Properties.of()
                                .mapColor(metal.mapColor)
                                .instrument(NoteBlockInstrument.BASEDRUM)
                                .requiresCorrectToolForDrops()
                                .strength(5.0F, 6.0F)));
            }
        }
    }

    // --- Fase 2: energia ---
    /** Telaio macchina: componente base di tutte le macchine */
    public static final DeferredBlock<Block> MACHINE_FRAME = registerWithItem("machine_frame", Block::new,
            machineProperties());

    public static final DeferredBlock<Block> COMBUSTION_GENERATOR = registerWithItem("combustion_generator",
            MachineBlocks.CombustionGenerator::new, machineProperties().lightLevel(litLight(13)));
    public static final DeferredBlock<Block> SOLAR_PANEL = registerWithItem("solar_panel",
            MachineBlocks.SolarPanel::new, machineProperties());
    public static final DeferredBlock<Block> GEOTHERMAL_GENERATOR = registerWithItem("geothermal_generator",
            MachineBlocks.GeothermalGenerator::new, machineProperties().lightLevel(litLight(10)));

    /** Fase 10: generatori avanzati e rinnovabili */
    public static final Map<com.futuretek.power.Generators.Kind, DeferredBlock<Block>> GENERATORS =
            new EnumMap<>(com.futuretek.power.Generators.Kind.class);

    static {
        for (var kind : com.futuretek.power.Generators.Kind.values()) {
            GENERATORS.put(kind, registerWithItem(kind.name().toLowerCase(java.util.Locale.ROOT),
                    props -> new com.futuretek.power.Generators.Block(kind, props), machineProperties().lightLevel(litLight(10))));
        }
    }

    public static final DeferredBlock<Block> CHARCOAL_KILN = registerWithItem("charcoal_kiln",
            com.futuretek.power.CharcoalKiln.KilnBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN)
                    .requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(SoundType.STONE).lightLevel(litLight(12)));

    /** Banchi accumulatori I, II, III */
    public static final DeferredBlock<Block> BATTERY_BOX = registerWithItem("battery_box",
            props -> new BatteryBoxBlock(EnergyTier.LOW, 40_000, props), machineProperties());
    public static final DeferredBlock<Block> BATTERY_BOX_2 = registerWithItem("battery_box_2",
            props -> new BatteryBoxBlock(EnergyTier.MEDIUM, 600_000, props), machineProperties());
    public static final DeferredBlock<Block> BATTERY_BOX_3 = registerWithItem("battery_box_3",
            props -> new BatteryBoxBlock(EnergyTier.HIGH, 10_000_000, props), machineProperties());

    // --- Fase 3: macchine di lavorazione ---
    public static final Map<ProcessingMachine, DeferredBlock<Block>> PROCESSING = new EnumMap<>(ProcessingMachine.class);

    static {
        for (ProcessingMachine machine : ProcessingMachine.values()) {
            PROCESSING.put(machine, registerWithItem(machine.id,
                    props -> new ProcessingMachineBlock(machine, props), machineProperties().lightLevel(litLight(8))));
        }
    }

    // --- Fase 4: trasmutazione ---
    public static final DeferredBlock<Block> SYNTHESIS_TABLE = registerWithItem("synthesis_table",
            EssenceBlocks.SynthesisTable::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .requiresCorrectToolForDrops().strength(5.0F, 1200.0F).sound(SoundType.AMETHYST).lightLevel(s -> 7));
    public static final DeferredBlock<Block> STAR_COLLECTOR = registerWithItem("star_collector",
            props -> new StarCollectorBlock(8, 100_000, props), collectorProperties());
    public static final DeferredBlock<Block> STAR_COLLECTOR_2 = registerWithItem("star_collector_2",
            props -> new StarCollectorBlock(24, 400_000, props), collectorProperties());
    public static final DeferredBlock<Block> STAR_COLLECTOR_3 = registerWithItem("star_collector_3",
            props -> new StarCollectorBlock(80, 2_000_000, props), collectorProperties());
    public static final DeferredBlock<Block> REPLICATOR = registerWithItem("replicator",
            EssenceBlocks.Replicator::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .requiresCorrectToolForDrops().strength(5.0F, 1200.0F).sound(SoundType.METAL));

    // --- Comodita': fornace di ferro (1,5 volte piu' veloce) ---
    public static final DeferredBlock<Block> IRON_FURNACE = registerWithItem("iron_furnace",
            com.futuretek.utility.IronFurnace.Block::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops().strength(4.0F, 6.0F).sound(SoundType.METAL)
                    .lightLevel(s -> s.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 13 : 0));

    // --- Bauli FutureTek (piu' grandi) ---
    public static final Map<com.futuretek.storage.Storage.Tier, DeferredBlock<Block>> CHESTS =
            new EnumMap<>(com.futuretek.storage.Storage.Tier.class);

    static {
        for (var tier : com.futuretek.storage.Storage.Tier.values()) {
            boolean obsidian = tier == com.futuretek.storage.Storage.Tier.OBSIDIAN;
            CHESTS.put(tier, registerWithItem(tier.id, props -> new com.futuretek.storage.Storage.ChestBlock(tier, props),
                    BlockBehaviour.Properties.of().mapColor(obsidian ? MapColor.COLOR_BLACK : MapColor.METAL)
                            .strength(obsidian ? 20.0F : 3.0F, obsidian ? 1200.0F : 6.0F).sound(SoundType.METAL)));
        }
    }

    // --- Fase 4b: alchimia avanzata ---
    public static final DeferredBlock<Block> NOCTURNITE_BLOCK = registerWithItem("nocturnite_block", Block::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops()
                    .strength(30.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK));
    public static final DeferredBlock<Block> ARDORITE_BLOCK = registerWithItem("ardorite_block", Block::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).requiresCorrectToolForDrops()
                    .strength(40.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK).lightLevel(s -> 6));
    public static final DeferredBlock<Block> STAR_CHARGE = registerWithItem("star_charge",
            com.futuretek.alchemy.AlchemyBlocks.StarCharge::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW)
                    .instabreak().sound(SoundType.GRASS).lightLevel(s -> s.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 15 : 3));
    public static final DeferredBlock<Block> REPULSION_LANTERN = registerWithItem("repulsion_lantern",
            com.futuretek.alchemy.AlchemyBlocks.RepulsionLantern::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(1.5F).sound(SoundType.LANTERN).lightLevel(s -> 15).noOcclusion());

    private static BlockBehaviour.Properties collectorProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).requiresCorrectToolForDrops()
                .strength(3.0F, 6.0F).sound(SoundType.AMETHYST).lightLevel(s -> 10);
    }

    // --- Fase 5: logistica ---
    public static final DeferredBlock<Block> ITEM_CONDUIT = conduit("item_conduit", ConduitBlock.Kind.NORMAL);
    public static final DeferredBlock<Block> EXTRACTOR_CONDUIT = conduit("extractor_conduit", ConduitBlock.Kind.EXTRACTOR);
    public static final DeferredBlock<Block> FAST_EXTRACTOR_CONDUIT = conduit("fast_extractor_conduit",
            ConduitBlock.Kind.FAST_EXTRACTOR);
    public static final DeferredBlock<Block> SORTING_CONDUIT = conduit("sorting_conduit", ConduitBlock.Kind.SORTING);
    /** Tubo analizzatore: impara il contenuto del baule che tocca (magazzino automatico) */
    public static final DeferredBlock<Block> ANALYZER_CONDUIT = conduit("analyzer_conduit", ConduitBlock.Kind.ANALYZER);
    public static final DeferredBlock<Block> NETWORK_TERMINAL = registerWithItem("network_terminal",
            LogisticsBlocks.Terminal::new, machineProperties());
    public static final DeferredBlock<Block> AUTO_CRAFTER = registerWithItem("auto_crafter",
            LogisticsBlocks.AutoCrafter::new, machineProperties());
    public static final DeferredBlock<Block> LOGISTICS_CORE = registerWithItem("logistics_core",
            LogisticsBlocks.Core::new, machineProperties().lightLevel(litLight(9)));
    public static final Map<LogicGateBlock.Type, DeferredBlock<Block>> GATES = new EnumMap<>(LogicGateBlock.Type.class);

    static {
        for (LogicGateBlock.Type type : LogicGateBlock.Type.values()) {
            GATES.put(type, registerWithItem(type.name().toLowerCase(java.util.Locale.ROOT) + "_gate",
                    props -> new LogicGateBlock(type, props), BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                            .instabreak().sound(SoundType.STONE).noOcclusion()));
        }
    }

    private static DeferredBlock<Block> conduit(String name, ConduitBlock.Kind kind) {
        return registerWithItem(name, props -> new ConduitBlock(kind, props), BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE).strength(0.5F).sound(SoundType.STONE).noOcclusion());
    }

    // --- Fase 6: tecnologia avanzata ---
    public static final DeferredBlock<Block> REACTOR = registerWithItem("reactor_core",
            MachineBlocks.Reactor::new, machineProperties().strength(10.0F, 1200.0F).lightLevel(litLight(12)));
    public static final DeferredBlock<Block> REACTOR_CHAMBER = registerWithItem("reactor_chamber", Block::new,
            machineProperties().strength(10.0F, 1200.0F));
    public static final DeferredBlock<Block> QUARRY = registerWithItem("quarry",
            MachineBlocks.Quarry::new, machineProperties().lightLevel(litLight(8)));
    /** Fase 10: talpa mineraria (si muove da sola) */
    public static final DeferredBlock<Block> MINING_MOLE = registerWithItem("mining_mole",
            com.futuretek.mining.MiningMole.MoleBlock::new, machineProperties().lightLevel(litLight(7)));
    /** Fase 10: segnalino d'area, telaio e testa di perforazione dell'escavatore */
    public static final DeferredBlock<Block> AREA_MARKER = registerWithItem("area_marker",
            com.futuretek.mining.QuarryParts.Marker::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE)
                    .instabreak().noCollission().lightLevel(s -> 7).sound(SoundType.WOOD));
    public static final DeferredBlock<Block> QUARRY_FRAME = BLOCKS.registerBlock("quarry_frame",
            com.futuretek.mining.QuarryParts.Frame::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW)
                    .strength(0.5F).noOcclusion().sound(SoundType.METAL).noLootTable());
    public static final DeferredBlock<Block> QUARRY_DRILL = BLOCKS.registerBlock("quarry_drill",
            com.futuretek.mining.QuarryParts.Drill::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(-1.0F, 3600000.0F).noOcclusion().noLootTable().lightLevel(s -> 6).sound(SoundType.METAL));

    // --- Fase 7: binari ---
    public static final DeferredBlock<Block> REINFORCED_RAIL = registerWithItem("reinforced_rail",
            RailBlocks.Reinforced::new, railProperties());
    public static final DeferredBlock<Block> INDUCTION_RAIL = registerWithItem("induction_rail",
            RailBlocks.Induction::new, railProperties());
    public static final DeferredBlock<Block> HOLDING_RAIL = registerWithItem("holding_rail",
            RailBlocks.Holding::new, railProperties());
    public static final DeferredBlock<Block> LOADER_RAIL = registerWithItem("loader_rail",
            props -> new RailBlocks.Transfer(true, props), railProperties());
    public static final DeferredBlock<Block> UNLOADER_RAIL = registerWithItem("unloader_rail",
            props -> new RailBlocks.Transfer(false, props), railProperties());

    private static BlockBehaviour.Properties railProperties() {
        return BlockBehaviour.Properties.of().noCollission().strength(0.7F).sound(SoundType.METAL);
    }

    // --- Fase 8: agricoltura e api ---
    public static final DeferredBlock<Block> GROWTH_ACCELERATOR = registerWithItem("growth_accelerator",
            MachineBlocks.GrowthAccelerator::new, machineProperties().lightLevel(litLight(10)));
    public static final DeferredBlock<Block> HARVESTER = registerWithItem("harvester",
            MachineBlocks.Harvester::new, machineProperties().lightLevel(litLight(6)));
    public static final DeferredBlock<Block> APIARY = registerWithItem("apiary", BeeBlocks.Apiary::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD));
    /** Alveari selvatici: si trovano nel mondo e, rotti, danno regina e fuco */
    public static final DeferredBlock<Block> WILD_HIVE_COMMON = wildHive("wild_hive_common");
    public static final DeferredBlock<Block> WILD_HIVE_FOREST = wildHive("wild_hive_forest");
    public static final DeferredBlock<Block> WILD_HIVE_MEADOW = wildHive("wild_hive_meadow");

    private static DeferredBlock<Block> wildHive(String name) {
        return registerWithItem(name, Block::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW)
                .strength(0.6F).sound(SoundType.WOOD));
    }

    // --- Fase 9: il Margine Cristallino ---
    public static final DeferredBlock<Block> CRYSTAL_STONE = registerWithItem("crystal_stone", Block::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops()
                    .strength(1.8F, 6.0F).sound(SoundType.AMETHYST));
    public static final DeferredBlock<Block> CRYSTAL_SOIL = registerWithItem("crystal_soil", Block::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_MAGENTA).strength(0.6F).sound(SoundType.GRAVEL));
    public static final DeferredBlock<Block> CRYSTAL_TURF = registerWithItem("crystal_turf", Block::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.7F).sound(SoundType.MOSS));
    public static final DeferredBlock<Block> VOLTITE_BLOCK = registerWithItem("voltite_block", Block::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops()
                    .strength(4.0F, 6.0F).sound(SoundType.AMETHYST).lightLevel(s -> 6));
    public static final DeferredBlock<Block> VOLTITE_CLUSTER = registerWithItem("voltite_cluster",
            VoltiteClusterBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).noOcclusion()
                    .strength(1.0F).sound(SoundType.AMETHYST_CLUSTER).lightLevel(s -> 9));
    public static final DeferredBlock<Block> EDGE_ANCHOR = registerWithItem("edge_anchor", EdgeAnchorBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops()
                    .strength(5.0F, 1200.0F).sound(SoundType.AMETHYST).lightLevel(s -> 12));

    /** Cavi: nome -> blocco. Ordine: dal piu' debole al piu' forte */
    public static final Map<String, DeferredBlock<Block>> CABLES = new java.util.LinkedHashMap<>();

    static {
        cable("tin_cable", EnergyTier.LOW, 4);
        cable("insulated_copper_cable", EnergyTier.MEDIUM, 6);
        cable("insulated_gold_cable", EnergyTier.HIGH, 6);
        cable("voltite_fiber", EnergyTier.EXTREME, 4);
    }

    private static void cable(String name, EnergyTier tier, int thickness) {
        CABLES.put(name, registerWithItem(name, props -> new CableBlock(tier, thickness, props),
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_GRAY)
                        .strength(0.5F)
                        .sound(SoundType.WOOL)
                        .noOcclusion()));
    }

    private static BlockBehaviour.Properties machineProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .requiresCorrectToolForDrops()
                .strength(3.5F, 6.0F)
                .sound(SoundType.METAL);
    }

    /** Luce emessa solo quando la macchina e' accesa (proprieta' LIT) */
    private static java.util.function.ToIntFunction<BlockState> litLight(int level) {
        return state -> state.hasProperty(MachineBlock.LIT) && state.getValue(MachineBlock.LIT) ? level : 0;
    }

    /** Proprieta' di un minerale nella pietra (come i minerali di Minecraft) */
    private static BlockBehaviour.Properties stoneOre() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(3.0F, 3.0F);
    }

    /** Proprieta' di un minerale nella deepslate: piu' duro da scavare */
    private static BlockBehaviour.Properties deepslateOre() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.DEEPSLATE)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(4.5F, 3.0F)
                .sound(SoundType.DEEPSLATE);
    }

    // --- Fasi 12-14: casa automatica, Nether, robot ---
    public static final DeferredBlock<Block> TREE_FARM = registerWithItem("tree_farm",
            p -> new com.futuretek.automation.SlotMachines.Block(p, com.futuretek.automation.HomeMachines.TreeFarm::new),
            machineProperties().lightLevel(litLight(6)));
    public static final DeferredBlock<Block> PLANTER = registerWithItem("planter",
            p -> new com.futuretek.automation.SlotMachines.Block(p, com.futuretek.automation.HomeMachines.Planter::new),
            machineProperties().lightLevel(litLight(6)));
    public static final DeferredBlock<Block> FERTILIZER = registerWithItem("auto_fertilizer",
            p -> new com.futuretek.automation.SlotMachines.Block(p, com.futuretek.automation.HomeMachines.Fertilizer::new),
            machineProperties().lightLevel(litLight(6)));
    public static final DeferredBlock<Block> ELECTRIC_LAMP = registerWithItem("electric_lamp",
            p -> new com.futuretek.automation.HomeMachines.LampBlock(p, false),
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.8F).sound(SoundType.GLASS)
                    .lightLevel(state -> state.getValue(com.futuretek.automation.HomeMachines.LampBlock.LIT) ? 15 : 0));
    public static final DeferredBlock<Block> LIGHT_PANEL = registerWithItem("light_panel",
            p -> new com.futuretek.automation.HomeMachines.LampBlock(p, true),
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.5F).sound(SoundType.GLASS).noOcclusion()
                    .lightLevel(state -> state.getValue(com.futuretek.automation.HomeMachines.LampBlock.LIT) ? 14 : 0));
    public static final DeferredBlock<Block> ENERGY_SWITCH = registerWithItem("energy_switch",
            com.futuretek.automation.HomeMachines.SwitchBlock::new, machineProperties().lightLevel(litLight(4)));
    public static final DeferredBlock<Block> ELECTRIC_ENCHANTER = registerWithItem("electric_enchanter",
            p -> new com.futuretek.automation.SlotMachines.Block(p, com.futuretek.automation.NetherMachines.Enchanter::new),
            machineProperties().lightLevel(state -> 7));
    public static final DeferredBlock<Block> MOB_INCUBATOR = registerWithItem("mob_incubator",
            p -> new com.futuretek.automation.SlotMachines.Block(p, com.futuretek.automation.NetherMachines.Incubator::new),
            machineProperties().lightLevel(litLight(9)));
    public static final DeferredBlock<Block> LASER_TURRET = registerWithItem("laser_turret",
            p -> new com.futuretek.automation.SlotMachines.Block(p, com.futuretek.automation.NetherMachines.LaserTurret::new),
            machineProperties().lightLevel(litLight(10)));
    public static final DeferredBlock<Block> ROBOT_DOCK = registerWithItem("robot_dock",
            com.futuretek.robot.RobotParts.DockBlock::new, machineProperties().lightLevel(litLight(5)));

    public static final DeferredBlock<Block> REACTOR_MAINTAINER = registerWithItem("reactor_maintainer",
            p -> new com.futuretek.automation.SlotMachines.Block(p, com.futuretek.nuclear.ReactorMaintainer::new),
            machineProperties().lightLevel(litLight(6)));
    public static final DeferredBlock<Block> NUCLEAR_TNT = registerWithItem("nuclear_tnt",
            com.futuretek.nuclear.NukeBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).instabreak().sound(SoundType.GRASS).ignitedByLava());

    public static final DeferredBlock<Block> ENCHANT_RESEARCHER = registerWithItem("enchant_researcher",
            p -> new com.futuretek.automation.SlotMachines.Block(p, com.futuretek.automation.Enchanting.Researcher::new),
            machineProperties().lightLevel(litLight(8)));

    // Rifugio portatile: il nucleo costruisce la casa, muri/tetto/finestre non si scavano
    public static final DeferredBlock<Block> SHELTER_CORE = BLOCKS.registerBlock("shelter_core",
            com.futuretek.utility.Shelter.CoreBlock::new,
            BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.METAL)
                    .strength(1.5F, 1200F).sound(net.minecraft.world.level.block.SoundType.METAL).lightLevel(s -> 15));
    static {
        ModItems.ITEMS.registerItem("shelter_core", props -> new com.futuretek.utility.Shelter.CoreItem(SHELTER_CORE.get(), props));
    }
    public static final DeferredBlock<Block> SHELTER_WALL = BLOCKS.registerSimpleBlock("shelter_wall",
            BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.WOOD)
                    .strength(-1.0F, 3600000F).noLootTable().sound(net.minecraft.world.level.block.SoundType.WOOD)
                    .isValidSpawn((st, g, p, t) -> false));
    public static final DeferredBlock<Block> SHELTER_ROOF = BLOCKS.registerSimpleBlock("shelter_roof",
            BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.COLOR_RED)
                    .strength(-1.0F, 3600000F).noLootTable().sound(net.minecraft.world.level.block.SoundType.WOOD)
                    .isValidSpawn((st, g, p, t) -> false));
    public static final DeferredBlock<Block> SHELTER_WINDOW = BLOCKS.registerBlock("shelter_window",
            net.minecraft.world.level.block.TransparentBlock::new,
            BlockBehaviour.Properties.of().strength(-1.0F, 3600000F).noLootTable().noOcclusion()
                    .sound(net.minecraft.world.level.block.SoundType.GLASS)
                    .isValidSpawn((st, g, p, t) -> false).isRedstoneConductor((st, g, p) -> false)
                    .isSuffocating((st, g, p) -> false).isViewBlocking((st, g, p) -> false));

    /** Registra un blocco e il suo oggetto */
    private static DeferredBlock<Block> registerWithItem(String name,
            Function<BlockBehaviour.Properties, ? extends Block> factory, BlockBehaviour.Properties properties) {
        DeferredBlock<Block> block = BLOCKS.registerBlock(name, factory, properties);
        ModItems.ITEMS.registerItem(name, props -> new BlockItem(block.get(), props));
        return block;
    }

    private ModBlocks() {
    }
}
