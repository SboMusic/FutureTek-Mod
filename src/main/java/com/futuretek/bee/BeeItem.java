package com.futuretek.bee;

import java.util.List;

import com.futuretek.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Ape regina o fuco. La specie e' salvata nell'oggetto (componente "bee_species"). */
public class BeeItem extends Item {
    private final boolean queen;

    public BeeItem(boolean queen, Properties properties) {
        super(properties.stacksTo(queen ? 1 : 16));
        this.queen = queen;
    }

    public boolean isQueen() {
        return queen;
    }

    public static BeeSpecies species(ItemStack stack) {
        return BeeSpecies.byId(stack.getOrDefault(ModDataComponents.BEE_SPECIES.get(), "common"));
    }

    public static ItemStack of(Item item, BeeSpecies species, int count) {
        ItemStack stack = new ItemStack(item, count);
        stack.set(ModDataComponents.BEE_SPECIES.get(), species.id());
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId(), Component.translatable("bee.futuretek." + species(stack).id()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (queen) {
            int life = stack.getOrDefault(ModDataComponents.BEE_LIFE.get(), ApiaryBlockEntity.QUEEN_LIFE);
            tooltip.add(Component.translatable("tooltip.futuretek.bee_life", life).withStyle(ChatFormatting.GRAY));
        }
    }
}
