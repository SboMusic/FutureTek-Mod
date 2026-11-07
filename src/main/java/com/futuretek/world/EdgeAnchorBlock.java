package com.futuretek.world;

import com.futuretek.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ancora del Margine: click destro per viaggiare tra l'Overworld e il Margine Cristallino.
 * All'arrivo, se non c'e' gia', viene creata un'ancora accanto al giocatore per poter tornare.
 */
public class EdgeAnchorBlock extends Block {
    public EdgeAnchorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        boolean inEdge = level.dimension() == EdgeDimension.KEY;
        ServerLevel destination = serverPlayer.server.getLevel(inEdge ? Level.OVERWORLD : EdgeDimension.KEY);
        if (destination == null) {
            player.displayClientMessage(Component.translatable("message.futuretek.edge_missing"), true);
            return InteractionResult.FAIL;
        }
        BlockPos arrival = EdgeDimension.safeSurface(destination, pos.getX() + 2, pos.getZ(),
                ModBlocks.CRYSTAL_STONE.get());
        BlockPos anchorPos = arrival.west(2);
        if (!destination.getBlockState(anchorPos).is(this)) {
            destination.setBlockAndUpdate(anchorPos, defaultBlockState());
        }
        serverPlayer.changeDimension(new DimensionTransition(destination, Vec3.atBottomCenterOf(arrival), Vec3.ZERO,
                player.getYRot(), player.getXRot(), DimensionTransition.DO_NOTHING));
        destination.playSound(null, arrival, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 0.8F);
        return InteractionResult.CONSUME;
    }
}
