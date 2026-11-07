package com.futuretek.item;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.futuretek.FutureTek;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * I poteri dell'esoarmatura e dello zaino a reazione (lato SERVER).
 * Il volo vero e proprio avviene sul client (il movimento del giocatore e' calcolato dal client):
 * il client manda al server "sto usando lo zaino", il server consuma l'energia e annulla i danni da caduta.
 */
public final class ArmorAbilities {
    private ArmorAbilities() {
    }

    /** Giocatori che in questo momento stanno usando lo zaino */
    private static final Map<UUID, Boolean> FLYING = new ConcurrentHashMap<>();

    /** Pacchetto client -> server: tasto salto premuto/rilasciato con lo zaino addosso */
    public record JetpackInput(boolean flying) implements CustomPacketPayload {
        public static final Type<JetpackInput> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(FutureTek.MODID, "jetpack_input"));
        public static final StreamCodec<RegistryFriendlyByteBuf, JetpackInput> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.BOOL, JetpackInput::flying, JetpackInput::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void setFlying(Player player, boolean flying) {
        FLYING.put(player.getUUID(), flying);
    }

    private static boolean wearing(Player player, EquipmentSlot slot, Class<?> type) {
        return type.isInstance(player.getItemBySlot(slot).getItem());
    }

    private static ItemStack exo(Player player, EquipmentSlot slot) {
        ItemStack stack = player.getItemBySlot(slot);
        return stack.getItem() instanceof ExoArmorItem ? stack : ItemStack.EMPTY;
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        long time = player.level().getGameTime();
        // elmo: visione notturna (5 TU/t); livello III anche respirazione sott'acqua e sazieta'
        ItemStack helmet = exo(player, EquipmentSlot.HEAD);
        if (!helmet.isEmpty() && time % 20 == 0 && EnergyItem.use(helmet, 100)) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false));
            if (ExoArmorItem.tierOf(helmet) >= 3) {
                if (player.isUnderWater() && EnergyItem.use(helmet, 100)) {
                    player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 40, 0, true, false));
                }
                if (player.getFoodData().needsFood() && EnergyItem.use(helmet, 2000)) {
                    player.getFoodData().eat(2, 0.5F);
                }
            }
        }
        // petto livello III: resistenza al fuoco (1 TU/t)
        ItemStack chestExo = exo(player, EquipmentSlot.CHEST);
        if (ExoArmorItem.tierOf(chestExo) >= 3 && time % 20 == 0 && EnergyItem.use(chestExo, 20)) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 30, 0, true, false));
        }
        // gambali: scatto potenziato mentre corri (2 TU/t); piu' forte col livello
        ItemStack legs = exo(player, EquipmentSlot.LEGS);
        if (!legs.isEmpty() && player.isSprinting() && time % 20 == 0 && EnergyItem.use(legs, 40)) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, ExoArmorItem.tierOf(legs) - 1, true, false));
        }
        // stivali: salto potenziato (1 TU/t); piu' forte col livello
        ItemStack boots = exo(player, EquipmentSlot.FEET);
        if (!boots.isEmpty() && time % 20 == 0 && EnergyItem.use(boots, 20)) {
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 30, ExoArmorItem.tierOf(boots) - 1, true, false));
        }
        // zaino a reazione
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof JetpackItem && FLYING.getOrDefault(player.getUUID(), false)) {
            if (EnergyItem.use(chest, JetpackItem.COST_PER_TICK)) {
                player.fallDistance = 0;
            } else {
                FLYING.put(player.getUUID(), false);
            }
        }
    }

    /** Petto dell'esoarmatura: scudo energetico, -15/30/45% danni secondo il livello (1000 TU per punto bloccato) */
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack chest = exo(player, EquipmentSlot.CHEST);
            if (!chest.isEmpty()) {
                float blocked = event.getAmount() * 0.15F * ExoArmorItem.tierOf(chest);
                if (EnergyItem.use(chest, (int) Math.ceil(blocked * 1000))) {
                    event.setAmount(event.getAmount() - blocked);
                }
            }
        }
    }

    /** Stivali dell'esoarmatura: niente danni da caduta (50 TU per blocco); il livello I li dimezza */
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack boots = exo(player, EquipmentSlot.FEET);
            if (!boots.isEmpty() && EnergyItem.use(boots, (int) (event.getDistance() * 50))) {
                if (ExoArmorItem.tierOf(boots) == 1) {
                    event.setDamageMultiplier(event.getDamageMultiplier() * 0.5F);
                } else {
                    event.setDistance(0);
                    event.setCanceled(true);
                }
            }
        }
    }

    public static boolean isWearingJetpack(Player player) {
        return wearing(player, EquipmentSlot.CHEST, JetpackItem.class)
                && EnergyItem.getEnergy(player.getItemBySlot(EquipmentSlot.CHEST)) >= JetpackItem.COST_PER_TICK;
    }
}
