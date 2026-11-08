package com.futuretek;

import org.slf4j.Logger;

import com.futuretek.bee.BeeData;
import com.futuretek.energy.network.EnergyNetworkManager;
import com.futuretek.essence.EssenceValueLoader;
import com.futuretek.essence.EssenceValues;
import com.futuretek.essence.EssenceValuesPayload;
import com.futuretek.essence.SynthesisRequestPayload;
import com.futuretek.essence.EssenceCommands;
import com.futuretek.item.ArmorAbilities;
import com.futuretek.logistics.ItemNetworkManager;
import com.futuretek.logistics.TerminalClientData;
import com.futuretek.logistics.TerminalPayloads;
import com.futuretek.menu.TerminalMenu;
import com.futuretek.registry.ModAttachments;
import com.futuretek.registry.ModEntities;
import com.futuretek.registry.ModArmorMaterials;
import com.futuretek.registry.ModBlockEntities;
import com.futuretek.registry.ModCapabilities;
import com.futuretek.registry.ModDataComponents;
import com.futuretek.registry.ModMenus;
import com.futuretek.registry.ModRecipes;
import com.futuretek.registry.ModBlocks;
import com.futuretek.registry.ModCreativeTabs;
import com.futuretek.registry.ModItems;
import com.mojang.logging.LogUtils;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

/**
 * Classe principale di FutureTek.
 * E' il primo codice della mod che viene eseguito, sia sul client sia sul server.
 * Qui, nelle prossime fasi, registreremo blocchi, oggetti, macchine, ecc.
 */
@Mod(FutureTek.MODID)
public class FutureTek {
    /** Identificativo della mod: deve coincidere con mod_id in gradle.properties. */
    public static final String MODID = "futuretek";

    /** Il "registro di bordo": scrive messaggi nella console e nei file di log. */
    public static final Logger LOGGER = LogUtils.getLogger();

    public FutureTek(IEventBus modEventBus, ModContainer modContainer) {
        // Eventi di caricamento della mod (avvengono una volta, all'avvio del gioco)
        modEventBus.addListener(this::commonSetup);

        // Registra blocchi, oggetti e schede creative della mod
        ModArmorMaterials.MATERIALS.register(modEventBus);
        com.futuretek.registry.ModBuildingBlocks.init(); // blocchi da costruzione
        ModBlocks.BLOCKS.register(modEventBus);
        com.futuretek.world.Volcano.FEATURES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        ModDataComponents.COMPONENTS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModRecipes.RECIPE_TYPES.register(modEventBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        modEventBus.addListener(ModCapabilities::register);
        ModAttachments.ATTACHMENTS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        modEventBus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) ->
                event.put(ModEntities.ROBOT.get(), com.futuretek.robot.RobotEntity.createAttributes().build()));

        // Essenza: valori da JSON, invio della tabella ai giocatori
        modEventBus.addListener((RegisterPayloadHandlersEvent event) -> event.registrar("1")
                .playToClient(EssenceValuesPayload.TYPE, EssenceValuesPayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> EssenceValues.setClientValues(payload.values())))
                .playToClient(TerminalPayloads.Contents.TYPE, TerminalPayloads.Contents.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> TerminalClientData.contents = payload))
                .playToServer(TerminalPayloads.Request.TYPE, TerminalPayloads.Request.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> {
                            if (context.player() instanceof ServerPlayer sp && sp.containerMenu instanceof TerminalMenu menu) {
                                menu.handleRequest(sp, payload);
                            }
                        }))
                .playToClient(com.futuretek.logistics.ItemTransit.Payload.TYPE, com.futuretek.logistics.ItemTransit.Payload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> com.futuretek.logistics.TransitClientBridge.receive(payload)))
                .playToServer(SynthesisRequestPayload.TYPE, SynthesisRequestPayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> payload.handle(context.player())))
                .playToServer(ArmorAbilities.JetpackInput.TYPE, ArmorAbilities.JetpackInput.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(
                                () -> ArmorAbilities.setFlying(context.player(), payload.flying()))));
        NeoForge.EVENT_BUS.addListener(EssenceCommands::register);
        // Alchimia: scavo ad area, set di armatura, anello dei venti
        NeoForge.EVENT_BUS.addListener(com.futuretek.alchemy.AlchemyTools::onBreak);
        NeoForge.EVENT_BUS.addListener(com.futuretek.alchemy.AlchemyTools::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(com.futuretek.alchemy.AlchemyTools::onFall);
        NeoForge.EVENT_BUS.addListener(com.futuretek.alchemy.AlchemyTools::onDamage);
        NeoForge.EVENT_BUS.addListener(com.futuretek.alchemy.AlchemyItems.WindriderRing::onPlayerTick);
        // Comodita': abbattimento degli alberi, raccolta automatica della sacca del minatore
        NeoForge.EVENT_BUS.addListener(com.futuretek.utility.TreeFelling::onBreak);
        NeoForge.EVENT_BUS.addListener(com.futuretek.utility.MinerBag::onPickup);
        NeoForge.EVENT_BUS.addListener(ArmorAbilities::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(ArmorAbilities::onDamage);
        NeoForge.EVENT_BUS.addListener(com.futuretek.item.ElectricTools::onDamage);
        NeoForge.EVENT_BUS.addListener(com.futuretek.nuclear.NuclearBlast::onServerTick);
        NeoForge.EVENT_BUS.addListener(com.futuretek.registry.ModBuildingBlocks::onToolUse);
        NeoForge.EVENT_BUS.addListener(com.futuretek.utility.Shelter::onBreak);
        NeoForge.EVENT_BUS.addListener(com.futuretek.utility.Shelter::onExplosion);
        // fabbrica abbandonata (la regola del mondo si registra caricando la classe)
        com.futuretek.FutureTek.LOGGER.debug("Regola fabbrica: {}", com.futuretek.world.AbandonedFactory.RULE);
        NeoForge.EVENT_BUS.addListener(com.futuretek.world.AbandonedFactory::onServerStarted);
        NeoForge.EVENT_BUS.addListener(com.futuretek.world.AbandonedFactory::onLogin);
        NeoForge.EVENT_BUS.addListener(com.futuretek.world.AbandonedFactory::registerCommands);
        NeoForge.EVENT_BUS.addListener(ArmorAbilities::onFall);
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent event) -> {
            event.addListener(new EssenceValueLoader());
            event.addListener(new BeeData.Loader());
        });
        NeoForge.EVENT_BUS.addListener((OnDatapackSyncEvent event) -> {
            if (event.getPlayer() == null) {
                EssenceValues.markDirty(); // dopo /reload: ricalcola
            }
            EssenceValuesPayload payload = new EssenceValuesPayload(EssenceValues.exportForClient());
            event.getRelevantPlayers()
                    .filter(player -> player.connection.hasChannel(EssenceValuesPayload.TYPE))
                    .forEach(player -> PacketDistributor.sendToPlayer(player, payload));
        });

        // Quando un mondo viene chiuso, dimentichiamo le sue reti di cavi
        NeoForge.EVENT_BUS.addListener((LevelEvent.Unload event) -> {
            EnergyNetworkManager.unload(event.getLevel());
            ItemNetworkManager.unload(event.getLevel());
        });

        // Eventi di gioco (avvio server, tick, ecc.)
        NeoForge.EVENT_BUS.register(this);

        // Crea il file config/futuretek-common.toml con le nostre impostazioni
        modContainer.registerConfig(ModConfig.Type.COMMON, FutureTekConfig.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("FutureTek: caricamento completato.");
        if (FutureTekConfig.DEBUG_LOGGING.getAsBoolean()) {
            LOGGER.info("FutureTek: log di debug attivo (si disattiva in config/futuretek-common.toml).");
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("FutureTek: il server si sta avviando, la mod e' attiva.");
    }
}
