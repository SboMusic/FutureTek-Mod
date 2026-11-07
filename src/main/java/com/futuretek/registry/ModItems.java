package com.futuretek.registry;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.futuretek.FutureTek;
import com.futuretek.alchemy.AlchemyItems;
import com.futuretek.alchemy.AlchemyTools;
import com.futuretek.energy.EnergyTier;
import com.futuretek.item.BatteryItem;
import com.futuretek.item.CraftingToolItem;
import com.futuretek.item.EssenceCrystalItem;
import com.futuretek.item.ExoArmorItem;
import com.futuretek.item.ElectricTools;
import com.futuretek.item.JetpackItem;
import com.futuretek.item.ReactorComponentItem;
import com.futuretek.item.ShiftingStoneItem;
import com.futuretek.item.MultimeterItem;
import com.futuretek.item.WrenchItem;
import com.futuretek.item.TreeTapItem;
import com.futuretek.bee.BeeItem;
import com.futuretek.farm.FertilizerItem;
import com.futuretek.item.UpgradeItem;
import com.futuretek.rail.CargoCartItem;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Elenco di tutti gli OGGETTI di FutureTek.
 * "DeferredRegister" significa: li prepariamo qui e NeoForge li registra
 * nel gioco al momento giusto dell'avvio.
 */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FutureTek.MODID);

    // --- Forme dei nostri metalli ---
    public static final Map<Metal, DeferredItem<Item>> RAW = new EnumMap<>(Metal.class);
    public static final Map<Metal, DeferredItem<Item>> INGOT = new EnumMap<>(Metal.class);
    public static final Map<Metal, DeferredItem<Item>> NUGGET = new EnumMap<>(Metal.class);
    public static final Map<Metal, DeferredItem<Item>> DUST = new EnumMap<>(Metal.class);
    public static final Map<Metal, DeferredItem<Item>> PLATE = new EnumMap<>(Metal.class);

    // --- Polveri e piastre di materiali di Minecraft ---
    /** Materiali di Minecraft che hanno una polvere FutureTek */
    public static final String[] VANILLA_DUSTS = {"iron", "gold", "copper", "coal", "diamond", "obsidian"};
    /** Materiali di Minecraft che hanno una piastra FutureTek */
    public static final String[] VANILLA_PLATES = {"iron", "gold", "copper"};
    public static final Map<String, DeferredItem<Item>> VANILLA_DUST = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<Item>> VANILLA_PLATE = new LinkedHashMap<>();

    static {
        for (Metal metal : Metal.values()) {
            if (metal.fromOre) {
                RAW.put(metal, ITEMS.registerSimpleItem("raw_" + metal.id));
            }
            INGOT.put(metal, ITEMS.registerSimpleItem(metal.id + "_ingot"));
            NUGGET.put(metal, ITEMS.registerSimpleItem(metal.id + "_nugget"));
            DUST.put(metal, ITEMS.registerSimpleItem(metal.id + "_dust"));
            PLATE.put(metal, ITEMS.registerSimpleItem(metal.id + "_plate"));
        }
        for (String name : VANILLA_DUSTS) {
            VANILLA_DUST.put(name, ITEMS.registerSimpleItem(name + "_dust"));
        }
        for (String name : VANILLA_PLATES) {
            VANILLA_PLATE.put(name, ITEMS.registerSimpleItem(name + "_plate"));
        }
    }

    // --- Cristalli ---
    /** Frammento di voltite: cristallo energetico (servira' per circuiti ed energia) */
    public static final DeferredItem<Item> VOLTITE_SHARD = ITEMS.registerSimpleItem("voltite_shard");

    // --- Attrezzi di lavorazione ---
    /** Martello da forgia: 256 utilizzi */
    public static final DeferredItem<CraftingToolItem> FORGE_HAMMER = ITEMS.registerItem(
            "forge_hammer", CraftingToolItem::new, new Item.Properties().durability(256));
    /** Rubinetto per raccogliere la resina: 128 utilizzi */
    public static final DeferredItem<TreeTapItem> TREE_TAP = ITEMS.registerItem(
            "tree_tap", TreeTapItem::new, new Item.Properties().durability(128));

    // --- Resina e gomma ---
    public static final DeferredItem<Item> STICKY_RESIN = ITEMS.registerSimpleItem("sticky_resin");
    public static final DeferredItem<Item> RUBBER = ITEMS.registerSimpleItem("rubber");

    // --- Fase 2: energia ---
    /** Tagliacavi: piastra -> 2 fili. 256 utilizzi */
    public static final DeferredItem<CraftingToolItem> CUTTERS = ITEMS.registerItem(
            "cutters", CraftingToolItem::new, new Item.Properties().durability(256));
    /** Fili metallici: servono per fare i cavi */
    public static final String[] WIRE_METALS = {"tin", "copper", "gold"};
    public static final Map<String, DeferredItem<Item>> WIRE = new LinkedHashMap<>();

    static {
        for (String metal : WIRE_METALS) {
            WIRE.put(metal, ITEMS.registerSimpleItem(metal + "_wire"));
        }
    }

    /** Circuito di base: componente di quasi tutte le macchine */
    public static final DeferredItem<Item> BASIC_CIRCUIT = ITEMS.registerSimpleItem("basic_circuit");
    /** Circuito avanzato: per i generatori e le macchine avanzate */
    public static final DeferredItem<Item> ADVANCED_CIRCUIT = ITEMS.registerSimpleItem("advanced_circuit");
    /** Cella portatile: 10.000 TU, carica/scarica 32 TU/t */
    public static final DeferredItem<BatteryItem> BATTERY_CELL = ITEMS.registerItem("battery_cell",
            props -> new BatteryItem(10_000, EnergyTier.LOW.rate, props));
    /** Cella avanzata: 100.000 TU, carica/scarica 128 TU/t */
    public static final DeferredItem<BatteryItem> ADVANCED_BATTERY_CELL = ITEMS.registerItem("advanced_battery_cell",
            props -> new BatteryItem(100_000, EnergyTier.MEDIUM.rate, props));
    public static final DeferredItem<WrenchItem> WRENCH = ITEMS.registerItem("wrench", WrenchItem::new);
    public static final DeferredItem<MultimeterItem> MULTIMETER = ITEMS.registerItem("multimeter", MultimeterItem::new);

    // --- Fase 3b: frantumati purificati (triplicazione dei minerali) ---
    public static final String[] PURIFIED_METALS = {"tin", "lead", "silver", "uranium", "iron", "gold", "copper"};
    public static final Map<String, DeferredItem<Item>> PURIFIED = new LinkedHashMap<>();

    static {
        for (String metal : PURIFIED_METALS) {
            PURIFIED.put(metal, ITEMS.registerSimpleItem("purified_crushed_" + metal));
        }
    }

    // --- Fase 4: trasmutazione ---
    public static final DeferredItem<ShiftingStoneItem> SHIFTING_STONE = ITEMS.registerItem("shifting_stone",
            ShiftingStoneItem::new);
    public static final DeferredItem<EssenceCrystalItem> ESSENCE_CRYSTAL = ITEMS.registerItem("essence_crystal",
            props -> new EssenceCrystalItem(100_000, props));
    public static final DeferredItem<EssenceCrystalItem> ESSENCE_CRYSTAL_2 = ITEMS.registerItem("essence_crystal_2",
            props -> new EssenceCrystalItem(2_000_000, props));
    public static final DeferredItem<EssenceCrystalItem> ESSENCE_CRYSTAL_3 = ITEMS.registerItem("essence_crystal_3",
            props -> new EssenceCrystalItem(50_000_000, props));

    public static final DeferredItem<EssenceCrystalItem> ESSENCE_CRYSTAL_4 = ITEMS.registerItem("essence_crystal_4",
            props -> new EssenceCrystalItem(1_000_000_000L, props));

    // --- Fase 4b: alchimia avanzata ---
    /** Braci: combustibili (durata in tick: carbone = 1600) */
    public static final DeferredItem<AlchemyItems.Ember> ARCANE_EMBER = ITEMS.registerItem("arcane_ember",
            props -> new AlchemyItems.Ember(6400, props));
    public static final DeferredItem<AlchemyItems.Ember> STELLAR_EMBER = ITEMS.registerItem("stellar_ember",
            props -> new AlchemyItems.Ember(25600, props));
    public static final DeferredItem<AlchemyItems.Ember> ETERNAL_EMBER = ITEMS.registerItem("eternal_ember",
            props -> new AlchemyItems.Ember(102400, props.fireResistant()));
    /** Materiali alchemici: Nocturnite (livello 1) e Ardorite (livello 2) */
    public static final DeferredItem<Item> NOCTURNITE = ITEMS.registerItem("nocturnite", Item::new,
            new Item.Properties().fireResistant());
    public static final DeferredItem<Item> ARDORITE = ITEMS.registerItem("ardorite", Item::new,
            new Item.Properties().fireResistant());
    /** Fascia d'Essenza: base di anelli e amuleti */
    public static final DeferredItem<Item> ESSENCE_BAND = ITEMS.registerSimpleItem("essence_band");

    public static final DeferredItem<AlchemyTools.Pickaxe> NOCTURNITE_PICKAXE = ITEMS.registerItem("nocturnite_pickaxe",
            props -> new AlchemyTools.Pickaxe(AlchemyTools.NOCTURNITE, props));
    public static final DeferredItem<AlchemyTools.Axe> NOCTURNITE_AXE = ITEMS.registerItem("nocturnite_axe",
            props -> new AlchemyTools.Axe(AlchemyTools.NOCTURNITE, props));
    public static final DeferredItem<AlchemyTools.Shovel> NOCTURNITE_SHOVEL = ITEMS.registerItem("nocturnite_shovel",
            props -> new AlchemyTools.Shovel(AlchemyTools.NOCTURNITE, props));
    public static final DeferredItem<AlchemyTools.Hoe> NOCTURNITE_HOE = ITEMS.registerItem("nocturnite_hoe",
            props -> new AlchemyTools.Hoe(AlchemyTools.NOCTURNITE, props));
    public static final DeferredItem<AlchemyTools.Sword> NOCTURNITE_SWORD = ITEMS.registerItem("nocturnite_sword",
            props -> new AlchemyTools.Sword(AlchemyTools.NOCTURNITE, props));
    public static final DeferredItem<AlchemyTools.Hammer> NOCTURNITE_HAMMER = ITEMS.registerItem("nocturnite_hammer",
            props -> new AlchemyTools.Hammer(AlchemyTools.NOCTURNITE, 1, props));
    public static final DeferredItem<AlchemyTools.Pickaxe> ARDORITE_PICKAXE = ITEMS.registerItem("ardorite_pickaxe",
            props -> new AlchemyTools.Pickaxe(AlchemyTools.ARDORITE, props));
    public static final DeferredItem<AlchemyTools.Axe> ARDORITE_AXE = ITEMS.registerItem("ardorite_axe",
            props -> new AlchemyTools.Axe(AlchemyTools.ARDORITE, props));
    public static final DeferredItem<AlchemyTools.Shovel> ARDORITE_SHOVEL = ITEMS.registerItem("ardorite_shovel",
            props -> new AlchemyTools.Shovel(AlchemyTools.ARDORITE, props));
    public static final DeferredItem<AlchemyTools.Hoe> ARDORITE_HOE = ITEMS.registerItem("ardorite_hoe",
            props -> new AlchemyTools.Hoe(AlchemyTools.ARDORITE, props));
    public static final DeferredItem<AlchemyTools.Sword> ARDORITE_SWORD = ITEMS.registerItem("ardorite_sword",
            props -> new AlchemyTools.Sword(AlchemyTools.ARDORITE, props));
    public static final DeferredItem<AlchemyTools.Hammer> ARDORITE_HAMMER = ITEMS.registerItem("ardorite_hammer",
            props -> new AlchemyTools.Hammer(AlchemyTools.ARDORITE, 2, props));

    public static final Map<ArmorItem.Type, DeferredItem<AlchemyTools.Armor>> NOCTURNITE_ARMOR = new java.util.EnumMap<>(ArmorItem.Type.class);
    public static final Map<ArmorItem.Type, DeferredItem<AlchemyTools.Armor>> ARDORITE_ARMOR = new java.util.EnumMap<>(ArmorItem.Type.class);

    static {
        for (ArmorItem.Type type : new ArmorItem.Type[]{ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE,
                ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS}) {
            NOCTURNITE_ARMOR.put(type, ITEMS.registerItem("nocturnite_" + type.getName(),
                    props -> new AlchemyTools.Armor(ModArmorMaterials.NOCTURNITE, type, 1, props)));
            ARDORITE_ARMOR.put(type, ITEMS.registerItem("ardorite_" + type.getName(),
                    props -> new AlchemyTools.Armor(ModArmorMaterials.ARDORITE, type, 2, props)));
        }
    }

    public static final DeferredItem<AlchemyItems.VoidCatalyst> VOID_CATALYST = ITEMS.registerItem("void_catalyst",
            AlchemyItems.VoidCatalyst::new);
    public static final DeferredItem<AlchemyItems.KineticLens> KINETIC_LENS = ITEMS.registerItem("kinetic_lens",
            AlchemyItems.KineticLens::new);
    public static final DeferredItem<AlchemyItems.Satchel> DIMENSIONAL_SATCHEL = ITEMS.registerItem("dimensional_satchel",
            AlchemyItems.Satchel::new);
    public static final DeferredItem<AlchemyItems.AttractorRing> ATTRACTOR_RING = ITEMS.registerItem("attractor_ring",
            AlchemyItems.AttractorRing::new);
    public static final DeferredItem<AlchemyItems.WindriderRing> WINDRIDER_RING = ITEMS.registerItem("windrider_ring",
            AlchemyItems.WindriderRing::new);
    public static final DeferredItem<AlchemyItems.EmberRing> EMBER_RING = ITEMS.registerItem("ember_ring",
            AlchemyItems.EmberRing::new);
    public static final DeferredItem<AlchemyItems.FrostRing> FROST_RING = ITEMS.registerItem("frost_ring",
            AlchemyItems.FrostRing::new);
    public static final DeferredItem<AlchemyItems.VerdantRing> VERDANT_RING = ITEMS.registerItem("verdant_ring",
            AlchemyItems.VerdantRing::new);
    public static final DeferredItem<AlchemyItems.ArcaneHourglass> ARCANE_HOURGLASS = ITEMS.registerItem("arcane_hourglass",
            AlchemyItems.ArcaneHourglass::new);
    public static final DeferredItem<AlchemyItems.VitalityAmulet> VITALITY_AMULET = ITEMS.registerItem("vitality_amulet",
            AlchemyItems.VitalityAmulet::new);
    public static final DeferredItem<AlchemyItems.MemoryGem> MEMORY_GEM = ITEMS.registerItem("memory_gem",
            AlchemyItems.MemoryGem::new);
    public static final DeferredItem<AlchemyItems.FluidAmulet> TIDE_AMULET = ITEMS.registerItem("tide_amulet",
            props -> new AlchemyItems.FluidAmulet(false, props));
    public static final DeferredItem<AlchemyItems.FluidAmulet> MAGMA_AMULET = ITEMS.registerItem("magma_amulet",
            props -> new AlchemyItems.FluidAmulet(true, props));
    public static final DeferredItem<AlchemyItems.DowsingRod> DOWSING_ROD = ITEMS.registerItem("dowsing_rod",
            AlchemyItems.DowsingRod::new);
    public static final DeferredItem<AlchemyItems.BuilderWand> BUILDER_WAND = ITEMS.registerItem("builder_wand",
            AlchemyItems.BuilderWand::new);
    public static final DeferredItem<AlchemyItems.RepairTalisman> REPAIR_TALISMAN = ITEMS.registerItem("repair_talisman",
            AlchemyItems.RepairTalisman::new);
    public static final DeferredItem<AlchemyItems.SynthesisTablet> SYNTHESIS_TABLET = ITEMS.registerItem("synthesis_tablet",
            AlchemyItems.SynthesisTablet::new);
    public static final DeferredItem<AlchemyItems.OmniscienceTome> OMNISCIENCE_TOME = ITEMS.registerItem("omniscience_tome",
            props -> new AlchemyItems.OmniscienceTome(props.rarity(net.minecraft.world.item.Rarity.EPIC)));

    // --- Comodita': rilevatori di minerali e sacca del minatore ---
    /** Polveri di rilevamento: I (carbone + pietra), II (redstone + ferro), III (ferro + diamante) */
    public static final DeferredItem<Item> SCANNING_DUST_1 = ITEMS.registerSimpleItem("scanning_dust_1");
    public static final DeferredItem<Item> SCANNING_DUST_2 = ITEMS.registerSimpleItem("scanning_dust_2");
    public static final DeferredItem<Item> SCANNING_DUST_3 = ITEMS.registerSimpleItem("scanning_dust_3");
    /** Rilevatori: area 3x3 (16 in profondita'), 9x9 (32), 16x16 (64) */
    public static final DeferredItem<com.futuretek.utility.OreScanner> ORE_SCANNER_1 = ITEMS.registerItem("ore_scanner_1",
            props -> new com.futuretek.utility.OreScanner(3, 16, 20, props));
    public static final DeferredItem<com.futuretek.utility.OreScanner> ORE_SCANNER_2 = ITEMS.registerItem("ore_scanner_2",
            props -> new com.futuretek.utility.OreScanner(9, 32, 40, props));
    public static final DeferredItem<com.futuretek.utility.OreScanner> ORE_SCANNER_3 = ITEMS.registerItem("ore_scanner_3",
            props -> new com.futuretek.utility.OreScanner(16, 64, 60, props));
    public static final DeferredItem<com.futuretek.utility.MinerBag> MINER_BAG = ITEMS.registerItem("miner_bag",
            com.futuretek.utility.MinerBag::new);

    // --- Fase 6: reattore, esoarmatura, zaino a reazione ---
    public static final Map<ReactorComponentItem.Type, DeferredItem<ReactorComponentItem>> REACTOR_PARTS =
            new java.util.EnumMap<>(ReactorComponentItem.Type.class);

    static {
        for (ReactorComponentItem.Type type : ReactorComponentItem.Type.values()) {
            REACTOR_PARTS.put(type, ITEMS.registerItem(type.name().toLowerCase(java.util.Locale.ROOT),
                    props -> new ReactorComponentItem(type, props)));
        }
    }

    public static final DeferredItem<Item> DEPLETED_FUEL_ROD = ITEMS.registerSimpleItem("depleted_fuel_rod");
    /** Celle di raffreddamento esauste (piene di calore): con un secchio d'acqua tornano nuove */
    public static final DeferredItem<Item> SPENT_COOLANT_CELL = ITEMS.registerSimpleItem("spent_coolant_cell");
    public static final DeferredItem<Item> SPENT_COOLANT_CELL_30K = ITEMS.registerSimpleItem("spent_coolant_cell_30k");
    public static final DeferredItem<Item> SPENT_COOLANT_CELL_60K = ITEMS.registerSimpleItem("spent_coolant_cell_60k");
    public static final DeferredItem<ExoArmorItem> EXO_HELMET = ITEMS.registerItem("exo_helmet",
            props -> new ExoArmorItem(ModArmorMaterials.EXO, ArmorItem.Type.HELMET, 2, props));
    public static final DeferredItem<ExoArmorItem> EXO_CHESTPLATE = ITEMS.registerItem("exo_chestplate",
            props -> new ExoArmorItem(ModArmorMaterials.EXO, ArmorItem.Type.CHESTPLATE, 2, props));
    public static final DeferredItem<ExoArmorItem> EXO_LEGGINGS = ITEMS.registerItem("exo_leggings",
            props -> new ExoArmorItem(ModArmorMaterials.EXO, ArmorItem.Type.LEGGINGS, 2, props));
    public static final DeferredItem<ExoArmorItem> EXO_BOOTS = ITEMS.registerItem("exo_boots",
            props -> new ExoArmorItem(ModArmorMaterials.EXO, ArmorItem.Type.BOOTS, 2, props));

    // --- Fase 11: utensili e armi elettrici, esoarmatura I e III ---
    public static final DeferredItem<ElectricTools.Drill> ELECTRIC_DRILL = ITEMS.registerItem("electric_drill",
            props -> new ElectricTools.Drill(ElectricTools.IRON_ELECTRIC, 40_000, 50, 128, false, props));
    public static final DeferredItem<ElectricTools.Drill> DIAMOND_DRILL = ITEMS.registerItem("diamond_drill",
            props -> new ElectricTools.Drill(ElectricTools.DIAMOND_ELECTRIC, 200_000, 80, 512, true, props));
    public static final DeferredItem<ElectricTools.Chainsaw> CHAINSAW = ITEMS.registerItem("chainsaw",
            ElectricTools.Chainsaw::new);
    public static final DeferredItem<ElectricTools.PlasmaSaber> PLASMA_SABER = ITEMS.registerItem("plasma_saber",
            ElectricTools.PlasmaSaber::new);
    public static final DeferredItem<ElectricTools.PulseRifle> PULSE_RIFLE = ITEMS.registerItem("pulse_rifle",
            ElectricTools.PulseRifle::new);
    public static final Map<ArmorItem.Type, DeferredItem<ExoArmorItem>> EXO_MK1 = new EnumMap<>(ArmorItem.Type.class);
    public static final Map<ArmorItem.Type, DeferredItem<ExoArmorItem>> EXO_MK3 = new EnumMap<>(ArmorItem.Type.class);

    static {
        for (ArmorItem.Type type : new ArmorItem.Type[]{ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE,
                ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS}) {
            EXO_MK1.put(type, ITEMS.registerItem("exo_mk1_" + type.getName(),
                    props -> new ExoArmorItem(ModArmorMaterials.EXO_MK1, type, 1, props)));
            EXO_MK3.put(type, ITEMS.registerItem("exo_mk3_" + type.getName(),
                    props -> new ExoArmorItem(ModArmorMaterials.EXO_MK3, type, 3, props.fireResistant())));
        }
    }
    public static final DeferredItem<JetpackItem> JETPACK = ITEMS.registerItem("jetpack",
            props -> new JetpackItem(ModArmorMaterials.JETPACK, props));

    // --- Fase 7: carrello merci ---
    public static final DeferredItem<CargoCartItem> CARGO_CART = ITEMS.registerItem("cargo_cart", CargoCartItem::new);

    // --- Fase 8: agricoltura e api ---
    public static final DeferredItem<FertilizerItem> FERTILIZER = ITEMS.registerItem("fertilizer", FertilizerItem::new);
    public static final DeferredItem<BeeItem> BEE_QUEEN = ITEMS.registerItem("bee_queen", props -> new BeeItem(true, props));
    public static final DeferredItem<BeeItem> BEE_DRONE = ITEMS.registerItem("bee_drone", props -> new BeeItem(false, props));
    /** Favi delle specie speciali (si lavorano nella centrifuga termica) */
    public static final String[] COMBS = {"mineral", "metallic", "frozen", "ember", "abyssal", "voltaic", "stellar"};
    public static final Map<String, DeferredItem<Item>> COMB = new LinkedHashMap<>();

    static {
        for (String comb : COMBS) {
            COMB.put(comb, ITEMS.registerSimpleItem(comb + "_comb"));
        }
    }

    // --- Fase 3: potenziamenti delle macchine ---
    public static final Map<UpgradeItem.Type, DeferredItem<UpgradeItem>> UPGRADES = new java.util.EnumMap<>(UpgradeItem.Type.class);

    static {
        for (UpgradeItem.Type type : UpgradeItem.Type.values()) {
            UPGRADES.put(type, ITEMS.registerItem(type.id + "_upgrade", props -> new UpgradeItem(type, props)));
        }
    }

    // --- Strumenti di bronzo e acciaio ---
    public static final DeferredItem<SwordItem> BRONZE_SWORD = sword("bronze", ModTiers.BRONZE);
    public static final DeferredItem<PickaxeItem> BRONZE_PICKAXE = pickaxe("bronze", ModTiers.BRONZE);
    public static final DeferredItem<AxeItem> BRONZE_AXE = axe("bronze", ModTiers.BRONZE);
    public static final DeferredItem<ShovelItem> BRONZE_SHOVEL = shovel("bronze", ModTiers.BRONZE);
    public static final DeferredItem<HoeItem> BRONZE_HOE = hoe("bronze", ModTiers.BRONZE);
    public static final DeferredItem<SwordItem> STEEL_SWORD = sword("steel", ModTiers.STEEL);
    public static final DeferredItem<PickaxeItem> STEEL_PICKAXE = pickaxe("steel", ModTiers.STEEL);
    public static final DeferredItem<AxeItem> STEEL_AXE = axe("steel", ModTiers.STEEL);
    public static final DeferredItem<ShovelItem> STEEL_SHOVEL = shovel("steel", ModTiers.STEEL);
    public static final DeferredItem<HoeItem> STEEL_HOE = hoe("steel", ModTiers.STEEL);

    // --- Armature (durata: ferro = 15, diamante = 33) ---
    public static final DeferredItem<ArmorItem> BRONZE_HELMET = armor("bronze", ModArmorMaterials.BRONZE, ArmorItem.Type.HELMET, 15);
    public static final DeferredItem<ArmorItem> BRONZE_CHESTPLATE = armor("bronze", ModArmorMaterials.BRONZE, ArmorItem.Type.CHESTPLATE, 15);
    public static final DeferredItem<ArmorItem> BRONZE_LEGGINGS = armor("bronze", ModArmorMaterials.BRONZE, ArmorItem.Type.LEGGINGS, 15);
    public static final DeferredItem<ArmorItem> BRONZE_BOOTS = armor("bronze", ModArmorMaterials.BRONZE, ArmorItem.Type.BOOTS, 15);
    public static final DeferredItem<ArmorItem> STEEL_HELMET = armor("steel", ModArmorMaterials.STEEL, ArmorItem.Type.HELMET, 25);
    public static final DeferredItem<ArmorItem> STEEL_CHESTPLATE = armor("steel", ModArmorMaterials.STEEL, ArmorItem.Type.CHESTPLATE, 25);
    public static final DeferredItem<ArmorItem> STEEL_LEGGINGS = armor("steel", ModArmorMaterials.STEEL, ArmorItem.Type.LEGGINGS, 25);
    public static final DeferredItem<ArmorItem> STEEL_BOOTS = armor("steel", ModArmorMaterials.STEEL, ArmorItem.Type.BOOTS, 25);

    // --- Funzioni di aiuto per creare strumenti e armature (numeri uguali a quelli del ferro) ---
    private static DeferredItem<SwordItem> sword(String metal, Tier tier) {
        return ITEMS.registerItem(metal + "_sword", props -> new SwordItem(tier,
                props.attributes(SwordItem.createAttributes(tier, 3, -2.4F))));
    }

    private static DeferredItem<PickaxeItem> pickaxe(String metal, Tier tier) {
        return ITEMS.registerItem(metal + "_pickaxe", props -> new PickaxeItem(tier,
                props.attributes(DiggerItem.createAttributes(tier, 1.0F, -2.8F))));
    }

    private static DeferredItem<AxeItem> axe(String metal, Tier tier) {
        return ITEMS.registerItem(metal + "_axe", props -> new AxeItem(tier,
                props.attributes(DiggerItem.createAttributes(tier, 6.0F, -3.1F))));
    }

    private static DeferredItem<ShovelItem> shovel(String metal, Tier tier) {
        return ITEMS.registerItem(metal + "_shovel", props -> new ShovelItem(tier,
                props.attributes(DiggerItem.createAttributes(tier, 1.5F, -3.0F))));
    }

    private static DeferredItem<HoeItem> hoe(String metal, Tier tier) {
        return ITEMS.registerItem(metal + "_hoe", props -> new HoeItem(tier,
                props.attributes(DiggerItem.createAttributes(tier, -2.0F, -1.0F))));
    }

    private static DeferredItem<ArmorItem> armor(String metal, Holder<ArmorMaterial> material, ArmorItem.Type type,
            int durabilityMultiplier) {
        return ITEMS.registerItem(metal + "_" + type.getName(), props -> new ArmorItem(material, type,
                props.durability(type.getDurability(durabilityMultiplier))));
    }

    // --- Fasi 13-14 ---
    public static final DeferredItem<Item> SOUL_CORE = ITEMS.registerSimpleItem("soul_core");
    public static final DeferredItem<com.futuretek.automation.NetherMachines.MobSampler> MOB_SAMPLER = ITEMS.registerItem(
            "mob_sampler", com.futuretek.automation.NetherMachines.MobSampler::new);
    public static final DeferredItem<Item> ROBOT_CHASSIS = ITEMS.registerSimpleItem("robot_chassis");
    public static final DeferredItem<com.futuretek.robot.RobotParts.RobotItemBase> ROBOT = ITEMS.registerItem("robot",
            com.futuretek.robot.RobotParts.RobotItemBase::new);
    public static final DeferredItem<com.futuretek.robot.RobotParts.Controller> ROBOT_CONTROLLER = ITEMS.registerItem(
            "robot_controller", com.futuretek.robot.RobotParts.Controller::new);
    public static final DeferredItem<com.futuretek.automation.Enchanting.Archive> ENCHANT_ARCHIVE = ITEMS.registerItem(
            "enchant_archive", com.futuretek.automation.Enchanting.Archive::new);
    public static final DeferredItem<Item> INFINITE_CORE = ITEMS.registerItem("infinite_core", Item::new,
            new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant());

    private ModItems() {
    }
}
