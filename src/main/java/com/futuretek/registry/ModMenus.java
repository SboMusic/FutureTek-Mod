package com.futuretek.registry;

import com.futuretek.FutureTek;
import com.futuretek.bee.ApiaryMenu;
import com.futuretek.menu.AutoCrafterMenu;
import com.futuretek.menu.BatteryBoxMenu;
import com.futuretek.menu.FilterMenu;
import com.futuretek.menu.TerminalMenu;
import com.futuretek.menu.CombustionGeneratorMenu;
import com.futuretek.menu.GeothermalGeneratorMenu;
import com.futuretek.menu.ProcessingMachineMenu;
import com.futuretek.menu.ReactorMenu;
import com.futuretek.menu.ReplicatorMenu;
import com.futuretek.menu.SynthesisTableMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Tipi di interfaccia (menu) delle macchine. */
public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, FutureTek.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<CombustionGeneratorMenu>> COMBUSTION_GENERATOR =
            MENUS.register("combustion_generator", () -> IMenuTypeExtension.create(CombustionGeneratorMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GeothermalGeneratorMenu>> GEOTHERMAL_GENERATOR =
            MENUS.register("geothermal_generator", () -> IMenuTypeExtension.create(GeothermalGeneratorMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<BatteryBoxMenu>> BATTERY_BOX =
            MENUS.register("battery_box", () -> IMenuTypeExtension.create(BatteryBoxMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ProcessingMachineMenu>> PROCESSING_MACHINE =
            MENUS.register("processing_machine", () -> IMenuTypeExtension.create(ProcessingMachineMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<SynthesisTableMenu>> SYNTHESIS_TABLE =
            MENUS.register("synthesis_table", () -> IMenuTypeExtension.create(SynthesisTableMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<ReplicatorMenu>> REPLICATOR =
            MENUS.register("replicator", () -> IMenuTypeExtension.create(ReplicatorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FilterMenu>> FILTER =
            MENUS.register("filter", () -> IMenuTypeExtension.create(FilterMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<com.futuretek.logistics.AnalyzerMenu>> ANALYZER =
            MENUS.register("analyzer_conduit", () -> IMenuTypeExtension.create(com.futuretek.logistics.AnalyzerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<com.futuretek.automation.SlotMachines.Menu>> SLOT_MACHINE =
            MENUS.register("slot_machine", () -> IMenuTypeExtension.create(com.futuretek.automation.SlotMachines.Menu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<TerminalMenu>> TERMINAL =
            MENUS.register("network_terminal", () -> IMenuTypeExtension.create(TerminalMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<AutoCrafterMenu>> AUTO_CRAFTER =
            MENUS.register("auto_crafter", () -> IMenuTypeExtension.create(AutoCrafterMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ReactorMenu>> REACTOR =
            MENUS.register("reactor_core", () -> IMenuTypeExtension.create(ReactorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ApiaryMenu>> APIARY =
            MENUS.register("apiary", () -> IMenuTypeExtension.create(ApiaryMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<com.futuretek.storage.Storage.ChestMenu>> FT_CHEST =
            MENUS.register("chest", () -> IMenuTypeExtension.create(com.futuretek.storage.Storage.ChestMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<com.futuretek.power.CharcoalKiln.KilnMenu>> CHARCOAL_KILN =
            MENUS.register("charcoal_kiln", () -> IMenuTypeExtension.create(com.futuretek.power.CharcoalKiln.KilnMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<com.futuretek.mining.MiningMole.MoleMenu>> MINING_MOLE =
            MENUS.register("mining_mole", () -> IMenuTypeExtension.create(com.futuretek.mining.MiningMole.MoleMenu::new));

    private ModMenus() {
    }
}
