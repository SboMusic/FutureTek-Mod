package com.futuretek.item;

import com.futuretek.block.ResinLogBlock;
import com.futuretek.registry.ModBlocks;
import com.futuretek.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Rubinetto: click destro su un tronco della resina con una vena piena
 * -> 1-3 resina grezza. La vena si svuota e si riformera' col tempo.
 */
public class TreeTapItem extends Item {
    public TreeTapItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (!state.is(ModBlocks.RESIN_LOG.get()) || !state.getValue(ResinLogBlock.RESIN)) {
            return InteractionResult.PASS;
        }

        // Tutta la logica avviene sul SERVER; il client mostra solo l'animazione.
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(ResinLogBlock.RESIN, false), Block.UPDATE_ALL);
            int amount = 1 + level.random.nextInt(3);
            Block.popResourceFromFace(level, pos, context.getClickedFace(),
                    new ItemStack(ModItems.STICKY_RESIN.get(), amount));
            level.playSound(null, pos, SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.BLOCKS, 1.0F, 1.0F);

            Player player = context.getPlayer();
            if (player != null) {
                context.getItemInHand().hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
