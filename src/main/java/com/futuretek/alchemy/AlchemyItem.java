package com.futuretek.alchemy;

import java.util.List;

import com.futuretek.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Base di tutti gli oggetti alchemici di FutureTek.
 * - Mostra sempre una spiegazione: la traduzione "tooltip.futuretek.NOME" (piu' righe separate da \n).
 * - Se "toggle" e' vero, click destro in aria lo ACCENDE/SPEGNE (quando e' acceso brilla);
 *   mentre e' nell'inventario ed e' acceso, ogni tick viene chiamato activeTick (solo sul server).
 */
public class AlchemyItem extends Item {
    private final boolean toggle;

    public AlchemyItem(boolean toggle, Properties properties) {
        super(properties.stacksTo(1));
        this.toggle = toggle;
    }

    public static boolean isActive(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ACTIVE.get(), false);
    }

    public static void setActive(ItemStack stack, boolean active) {
        stack.set(ModDataComponents.ACTIVE.get(), active);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!toggle) {
            return super.use(level, player, hand);
        }
        if (!level.isClientSide) {
            boolean now = !isActive(stack);
            setActive(stack, now);
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                    0.8F, now ? 1.4F : 0.7F);
            player.displayClientMessage(Component.translatable(now ? "message.futuretek.toggle_on"
                    : "message.futuretek.toggle_off", stack.getHoverName()), true);
            if (!now) {
                onDeactivated(stack, player);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level instanceof ServerLevel server && entity instanceof Player player) {
            passiveTick(stack, server, player);
            if (toggle && isActive(stack)) {
                activeTick(stack, server, player);
            }
        }
    }

    /** Sempre, finche' l'oggetto e' nell'inventario (server) */
    protected void passiveTick(ItemStack stack, ServerLevel level, Player player) {
    }

    /** Solo se l'oggetto e' acceso (server) */
    protected void activeTick(ItemStack stack, ServerLevel level, Player player) {
    }

    protected void onDeactivated(ItemStack stack, Player player) {
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return (toggle && isActive(stack)) || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        addDescription(this, tooltip);
        if (toggle) {
            tooltip.add(Component.translatable(isActive(stack) ? "tooltip.futuretek.active" : "tooltip.futuretek.inactive")
                    .withStyle(isActive(stack) ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        }
    }

    /** Aggiunge le righe di spiegazione "tooltip.futuretek.NOME" (usato anche da attrezzi e blocchi) */
    public static void addDescription(Item item, List<Component> tooltip) {
        String key = "tooltip.futuretek." + BuiltInRegistries.ITEM.getKey(item).getPath();
        for (String line : Component.translatable(key).getString().split("\n")) {
            tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
        }
    }
}
