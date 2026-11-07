package com.futuretek.rail;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Oggetto del carrello merci: click destro su un binario per piazzarlo. */
public class CargoCartItem extends Item {
    public CargoCartItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.getBlockState(pos).is(BlockTags.RAILS)) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            level.addFreshEntity(new CargoCart(level, pos.getX() + 0.5, pos.getY() + 0.0625, pos.getZ() + 0.5));
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
