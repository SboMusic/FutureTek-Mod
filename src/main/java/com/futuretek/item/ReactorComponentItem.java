package com.futuretek.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Componente del reattore a fissione (va nella griglia del nucleo).
 * La "durata" (barretta) indica: per le barre il combustibile rimasto, per dissipatori e celle il calore accumulato.
 */
public class ReactorComponentItem extends Item {
    public enum Type {
        /** Barre di uranio: 1, 2 o 4 barre nello stesso oggetto. Durata = secondi di funzionamento. */
        FUEL_ROD(1, 3000), DUAL_FUEL_ROD(2, 3000), QUAD_FUEL_ROD(4, 3000),
        /** Dissipatore: smaltisce 12 calore/s; si rompe se accumula piu' di 1000 calore */
        HEAT_VENT(0, 1000),
        /** Celle di raffreddamento: assorbono calore (10k / 30k / 60k). Piene diventano "esauste": si ricaricano con l'acqua */
        COOLANT_CELL(0, 10000), COOLANT_CELL_30K(0, 30000), COOLANT_CELL_60K(0, 60000),
        /** Piastra del reattore: +1000 calore massimo dello scafo e 1 calore/s smaltito */
        REACTOR_PLATING(0, 0);

        public final int rods;
        public final int durability;

        Type(int rods, int durability) {
            this.rods = rods;
            this.durability = durability;
        }

        public boolean isFuel() {
            return rods > 0;
        }

        public boolean isCoolant() {
            return this == COOLANT_CELL || this == COOLANT_CELL_30K || this == COOLANT_CELL_60K;
        }

        public boolean acceptsHeat() {
            return this == HEAT_VENT || isCoolant();
        }
    }

    private final Type type;

    public ReactorComponentItem(Type type, Properties properties) {
        super(type.durability > 0 ? properties.durability(type.durability).setNoRepair() : properties.stacksTo(16));
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.futuretek.reactor." + type.name().toLowerCase(java.util.Locale.ROOT))
                .withStyle(ChatFormatting.GRAY));
    }
}
