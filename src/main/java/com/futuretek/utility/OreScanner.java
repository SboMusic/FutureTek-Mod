package com.futuretek.utility;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Rilevatore di minerali (I, II, III).
 * Click destro: cerca i minerali in un'area quadrata attorno a te (3x3, 9x9 o 16x16 blocchi),
 * da qualche blocco sopra di te fino in profondita'. In chat dice quali minerali ci sono, quanti
 * e dove si trova il piu' vicino di ogni tipo; il piu' vicino in assoluto viene segnalato con delle scintille.
 */
public class OreScanner extends Item {
    /** Tutti i minerali (di Minecraft, di FutureTek e di altre mod) hanno il tag c:ores */
    public static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK, ResourceLocation.parse("c:ores"));

    private final int size;
    private final int depth;
    private final int cooldown;

    public OreScanner(int size, int depth, int cooldown, Properties properties) {
        super(properties.stacksTo(1));
        this.size = size;
        this.depth = depth;
        this.cooldown = cooldown;
    }

    public int getSize() {
        return size;
    }

    public int getDepth() {
        return depth;
    }

    /** Risultato della scansione: per ogni minerale, quanti blocchi e il piu' vicino */
    public record Found(Block block, int count, BlockPos nearest, double distance) {
    }

    /** Cerca i minerali: area size x size centrata sul giocatore, da 4 blocchi sopra a "depth" blocchi sotto */
    public static Map<Block, Found> scan(Level level, BlockPos center, int size, int depth) {
        Map<Block, Found> found = new LinkedHashMap<>();
        int low = -(size - 1) / 2;
        int high = size / 2;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(low, -depth, low), center.offset(high, 4, high))) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(ORES)) {
                continue;
            }
            double d = pos.distSqr(center);
            Found old = found.get(state.getBlock());
            if (old == null) {
                found.put(state.getBlock(), new Found(state.getBlock(), 1, pos.immutable(), d));
            } else {
                boolean closer = d < old.distance();
                found.put(state.getBlock(), new Found(state.getBlock(), old.count() + 1,
                        closer ? pos.immutable() : old.nearest(), closer ? d : old.distance()));
            }
        }
        return found;
    }

    /** "5 sotto, 3 a est, 2 a nord" */
    static Component direction(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        int dz = to.getZ() - from.getZ();
        StringBuilder text = new StringBuilder();
        if (dy != 0) {
            text.append(Math.abs(dy)).append(dy < 0 ? "↓ " : "↑ ");
        }
        if (dx != 0) {
            text.append(Math.abs(dx)).append(dx > 0 ? " E " : " O ");
        }
        if (dz != 0) {
            text.append(Math.abs(dz)).append(dz > 0 ? " S" : " N");
        }
        return Component.literal(text.toString().trim().isEmpty() ? "·" : text.toString().trim());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
            BlockPos center = player.blockPosition();
            Map<Block, Found> found = scan(level, center, size, depth);
            sp.sendSystemMessage(Component.translatable("message.futuretek.scanner_header", size, size, depth)
                    .withStyle(ChatFormatting.AQUA));
            if (found.isEmpty()) {
                sp.sendSystemMessage(Component.translatable("message.futuretek.scanner_none").withStyle(ChatFormatting.GRAY));
            }
            Found closest = null;
            for (Found f : found.values().stream().sorted((a, b) -> Double.compare(a.distance(), b.distance())).toList()) {
                sp.sendSystemMessage(Component.literal(" • ").append(f.block().getName().withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(" ×" + f.count() + "  ").withStyle(ChatFormatting.YELLOW))
                        .append(direction(center, f.nearest()).copy().withStyle(ChatFormatting.GRAY)));
                if (closest == null) {
                    closest = f;
                }
            }
            if (closest != null) {
                BlockPos p = closest.nearest();
                server.sendParticles(sp, ParticleTypes.END_ROD, true, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                        30, 0.4, 0.4, 0.4, 0.01);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F,
                    found.isEmpty() ? 0.6F : 1.4F);
            player.getCooldowns().addCooldown(this, cooldown);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.futuretek.ore_scanner", size, size, depth).withStyle(ChatFormatting.GRAY));
    }
}
