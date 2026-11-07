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

/**
 * Esoarmatura: armatura elettrica in 3 livelli (I = ferro, II = diamante, III = netherite).
 * Con l'energia attiva i suoi poteri (vedi ArmorAbilities): visione notturna, scudo energetico, scatto,
 * salto potenziato, niente danni da caduta; il livello III anche respirazione, sazieta' e resistenza al fuoco.
 * Si ricarica nel banco accumulatore. Non si consuma con l'uso (consuma energia).
 */
public class ExoArmorItem extends ArmorItem implements EnergyItem {
    public static final int CAPACITY = 1_000_000;
    /** 1, 2 o 3 */
    public final int tier;
    private final int capacity;

    public ExoArmorItem(Holder<ArmorMaterial> material, Type type, int tier, Properties properties) {
        super(material, type, properties.stacksTo(1));
        this.tier = tier;
        this.capacity = tier == 1 ? 300_000 : tier == 2 ? CAPACITY : 3_000_000;
    }

    public static int tierOf(ItemStack stack) {
        return stack.getItem() instanceof ExoArmorItem exo ? exo.tier : 0;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public int getRate() {
        return tier == 1 ? 256 : tier == 2 ? 512 : 2048;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * EnergyItem.getEnergy(stack) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x40C8FF;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gui.futuretek.energy", EnergyUtil.format(EnergyItem.getEnergy(stack)),
                EnergyUtil.format(capacity)).withStyle(ChatFormatting.GRAY));
        String set = tier == 1 ? "exo_mk1" : tier == 3 ? "exo_mk3" : "exo";
        tooltip.add(Component.translatable("tooltip.futuretek." + set + "." + getType().getName()).withStyle(ChatFormatting.AQUA));
    }
}
