package com.futuretek.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Potenziamento per le macchine di lavorazione (si mette negli slot in basso dell'interfaccia).
 */
public class UpgradeItem extends Item {
    public enum Type {
        /** +50% velocita' ciascuno, ma +60% consumo per tick. Massimo 4. */
        OVERCLOCK("overclock", 4),
        /** -20% consumo ciascuno. Massimo 4. */
        EFFICIENCY("efficiency", 4),
        /** +10.000 TU di capacita' ciascuno. Massimo 4. */
        ENERGY_STORAGE("energy_storage", 4),
        /** Spinge i prodotti finiti nei contenitori vicini (bauli, tubi). Ne basta 1. */
        EJECTOR("ejector", 1);

        public final String id;
        public final int maxPerMachine;

        Type(String id, int maxPerMachine) {
            this.id = id;
            this.maxPerMachine = maxPerMachine;
        }
    }

    private final Type type;

    public UpgradeItem(Type type, Properties properties) {
        super(properties.stacksTo(type.maxPerMachine == 1 ? 16 : 64));
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.futuretek.upgrade." + type.id).withStyle(ChatFormatting.GRAY));
    }
}
