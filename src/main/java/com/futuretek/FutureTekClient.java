package com.futuretek;

import com.futuretek.client.screen.EssenceScreens;
import com.futuretek.client.screen.LogisticsScreens;
import com.futuretek.client.screen.MachineScreens;
import com.futuretek.energy.EnergyUtil;
import com.futuretek.item.ArmorAbilities;
import com.futuretek.essence.EssenceValues;
import com.futuretek.bee.BeeItem;
import com.futuretek.registry.ModEntities;
import com.futuretek.registry.ModItems;
import com.futuretek.registry.ModMenus;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Codice SOLO per il client (grafica, interfacce, suoni).
 * Questa classe non viene mai caricata su un server dedicato:
 * e' cosi' che rispettiamo la separazione client/server.
 */
@Mod(value = FutureTek.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = FutureTek.MODID, value = Dist.CLIENT)
public class FutureTekClient {
    public FutureTekClient(ModContainer container) {
        // Aggiunge il pulsante "Config" nella lista delle mod (menu Mods)
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    /** Collega ogni tipo di interfaccia alla sua grafica */
    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.COMBUSTION_GENERATOR.get(), MachineScreens.CombustionGenerator::new);
        event.register(ModMenus.GEOTHERMAL_GENERATOR.get(), MachineScreens.GeothermalGenerator::new);
        event.register(ModMenus.BATTERY_BOX.get(), MachineScreens.BatteryBox::new);
        event.register(ModMenus.PROCESSING_MACHINE.get(), MachineScreens.ProcessingMachine::new);
        event.register(ModMenus.SYNTHESIS_TABLE.get(), EssenceScreens.SynthesisTable::new);
        event.register(ModMenus.FT_CHEST.get(), com.futuretek.client.screen.StorageScreen::new);
        event.register(ModMenus.CHARCOAL_KILN.get(), com.futuretek.client.screen.KilnScreen::new);
        event.register(ModMenus.MINING_MOLE.get(), com.futuretek.client.screen.MoleScreen::new);
        event.register(ModMenus.REPLICATOR.get(), EssenceScreens.Replicator::new);
        event.register(ModMenus.FILTER.get(), LogisticsScreens.Filter::new);
        event.register(ModMenus.ANALYZER.get(), LogisticsScreens.Analyzer::new);
        event.<com.futuretek.automation.SlotMachines.Menu, com.futuretek.client.screen.MachineScreen<com.futuretek.automation.SlotMachines.Menu>>register(
                ModMenus.SLOT_MACHINE.get(), (menu, inv, title) -> "electric_enchanter".equals(menu.machineId)
                        ? new com.futuretek.client.screen.EnchanterScreen(menu, inv, title)
                        : new com.futuretek.client.screen.SlotMachineScreen(menu, inv, title));
        event.register(ModMenus.TERMINAL.get(), LogisticsScreens.Terminal::new);
        event.register(ModMenus.AUTO_CRAFTER.get(), LogisticsScreens.AutoCrafter::new);
        event.register(ModMenus.REACTOR.get(), MachineScreens.Reactor::new);
        event.register(ModMenus.APIARY.get(), LogisticsScreens.Apiary::new);
    }

    /** Le api prendono il colore della loro specie (livello 1 dell'icona) */
    @SubscribeEvent
    static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, layer) -> layer == 1 ? 0xFF000000 | BeeItem.species(stack).color : 0xFFFFFFFF,
                ModItems.BEE_QUEEN.get(), ModItems.BEE_DRONE.get());
    }

    /** Il carrello merci si disegna come un carrello con un barile sopra */
    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CARGO_CART.get(),
                ctx -> new MinecartRenderer<>(ctx, ModelLayers.CHEST_MINECART));
        event.registerBlockEntityRenderer(com.futuretek.registry.ModBlockEntities.QUARRY.get(),
                com.futuretek.client.QuarryRenderer::new);
        event.registerEntityRenderer(com.futuretek.registry.ModEntities.ROBOT.get(), com.futuretek.client.RobotRenderer::new);
        event.registerEntityRenderer(com.futuretek.registry.ModEntities.PRIMED_NUKE.get(),
                net.minecraft.client.renderer.entity.TntRenderer::new);
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(com.futuretek.client.RobotRenderer.LAYER, com.futuretek.client.RobotRenderer::createLayer);
    }

    private static boolean wasFlying;

    /** Zaino a reazione: se il giocatore tiene premuto SALTO, lo spinge in alto (il movimento e' del client) */
    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        boolean flying = mc.options.keyJump.isDown() && ArmorAbilities.isWearingJetpack(mc.player)
                && !mc.player.getAbilities().flying;
        if (flying) {
            var motion = mc.player.getDeltaMovement();
            mc.player.setDeltaMovement(motion.x, Math.min(motion.y + 0.15, 0.45), motion.z);
            mc.player.fallDistance = 0;
        }
        if (flying != wasFlying) {
            PacketDistributor.sendToServer(new ArmorAbilities.JetpackInput(flying));
            wasFlying = flying;
        }
    }

    /** Sotto il nome di ogni oggetto con un valore, mostra quanta Essenza vale */
    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        // spiegazione dei blocchi della mod (macchine, generatori...): "tooltip.futuretek.<nome>"
        if (stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem) {
            var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
            String key = "tooltip.futuretek." + id.getPath();
            if (id.getNamespace().equals(FutureTek.MODID) && net.minecraft.client.resources.language.I18n.exists(key)) {
                for (String line : Component.translatable(key).getString().split("\n")) {
                    event.getToolTip().add(Component.literal(line).withStyle(ChatFormatting.GRAY));
                }
            }
        }
        // oggetti "semplici" della mod (materiali, celle esauste...) con una spiegazione
        if (stack.getItem().getClass() == net.minecraft.world.item.Item.class) {
            var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
            String key = "tooltip.futuretek." + id.getPath();
            if (id.getNamespace().equals(FutureTek.MODID) && net.minecraft.client.resources.language.I18n.exists(key)) {
                for (String line : Component.translatable(key).getString().split("\n")) {
                    event.getToolTip().add(Component.literal(line).withStyle(ChatFormatting.GRAY));
                }
            }
        }
        long total = EssenceValues.stackValue(true, stack);
        if (total > 0) {
            long value = total / Math.max(1, stack.getCount());
            event.getToolTip().add(Component.translatable("tooltip.futuretek.essence_value",
                    EnergyUtil.format(value), EnergyUtil.format(total)).withStyle(ChatFormatting.DARK_PURPLE));
            if (!EssenceValues.isCreatable(true, stack.getItem())) {
                event.getToolTip().add(Component.translatable("tooltip.futuretek.essence_not_creatable")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    /** Oggetti che viaggiano nei tubi logistici */
    @SubscribeEvent
    static void onRenderLevel(net.neoforged.neoforge.client.event.RenderLevelStageEvent event) {
        com.futuretek.client.TransitRenderer.onRender(event);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        FutureTek.LOGGER.info("FutureTek: preparazione del client completata.");
    }
}
