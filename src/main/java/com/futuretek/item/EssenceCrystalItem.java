package com.futuretek.item;

import java.util.List;

import com.futuretek.energy.EnergyUtil;
import com.futuretek.essence.EssenceManager;
import com.futuretek.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Cristallo d'Essenza: una "batteria" di Essenza.
 * Si carica click-destro su un Captatore Stellare; click destro in aria per versare
 * tutta l'Essenza nella propria riserva personale (quella del Tavolo di Sintesi).
 */
public class EssenceCrystalItem extends Item {
    private final long capacity;

    public EssenceCrystalItem(long capacity, Properties properties) {
        super(properties.stacksTo(1));
        this.capacity = capacity;
    }

    public long getCapacity() {
        return capacity;
    }

    public static long getStored(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ESSENCE.get(), 0L);
    }

    public static void setStored(ItemStack stack, long value) {
        stack.set(ModDataComponents.ESSENCE.get(), value);
    }

    /** Aggiunge Essenza al cristallo; restituisce quanta ne e' entrata */
    public long fill(ItemStack stack, long amount) {
        long added = Math.max(0, Math.min(amount, capacity - getStored(stack)));
        setStored(stack, getStored(stack) + added);
        return added;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        long stored = getStored(stack);
        if (stored <= 0) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            EssenceManager.addEssence(player, stored);
            setStored(stack, 0);
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0F, 1.5F);
            player.displayClientMessage(Component.translatable("message.futuretek.crystal_emptied",
                    EnergyUtil.format(stored)), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (int) Math.round(13.0 * getStored(stack) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xB05AFF;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.futuretek.essence_stored",
                EnergyUtil.format(getStored(stack)), EnergyUtil.format(capacity)).withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
