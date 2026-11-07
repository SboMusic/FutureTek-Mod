package com.futuretek.item;

import java.util.List;

import com.futuretek.energy.EnergyUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Zaino a reazione elettrico: si indossa sul petto; tieni premuto SALTO per volare (8 TU/t). */
public class JetpackItem extends ArmorItem implements EnergyItem {
    public static final int CAPACITY = 200_000;
    public static final int COST_PER_TICK = 8;

    public JetpackItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.CHESTPLATE, properties.stacksTo(1));
    }

    @Override
    public int getCapacity() {
        return CAPACITY;
    }

    @Override
    public int getRate() {
        return 128;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * EnergyItem.getEnergy(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFF9A1F;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gui.futuretek.energy", EnergyUtil.format(EnergyItem.getEnergy(stack)),
                EnergyUtil.format(CAPACITY)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.futuretek.jetpack").withStyle(ChatFormatting.GOLD));
    }
}
