package com.futuretek.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Fertilizzante: come la farina d'ossa, ma agisce 3 volte su un'area 3x3. */
public class FertilizerItem extends Item {
    public FertilizerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos center = context.getClickedPos();
        boolean grew = false;
        if (!level.isClientSide) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int i = 0; i < 3; i++) {
                        // growCrop consuma la pila che riceve: gli diamo una copia "usa e getta"
                        if (BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, center.offset(dx, 0, dz))) {
                            grew = true;
                        }
                    }
                }
            }
            if (grew) {
                level.levelEvent(1505, center, 15); // particelle verdi
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
