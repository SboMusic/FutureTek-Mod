package com.futuretek.item;

import java.util.List;

import com.futuretek.energy.EnergyUtil;
import com.futuretek.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Cella di energia portatile. Si ricarica nel banco accumulatore (slot in alto)
 * e si scarica nello slot in basso. La barretta sotto l'icona mostra la carica.
 */
public class BatteryItem extends Item implements EnergyItem {
    private final int capacity;
    private final int rate;

    public BatteryItem(int capacity, int rate, Properties properties) {
        super(properties.stacksTo(1));
        this.capacity = capacity;
        this.rate = rate;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public int getRate() {
        return rate;
    }

    public static int getEnergy(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ENERGY.get(), 0);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getEnergy(stack) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xE8402A;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gui.futuretek.energy",
                EnergyUtil.format(getEnergy(stack)), EnergyUtil.format(capacity)).withStyle(ChatFormatting.GRAY));
    }
}
