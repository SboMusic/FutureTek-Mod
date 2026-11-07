package com.futuretek.item;

import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Pietra del Mutamento.
 * - Nel banco da lavoro converte i metalli (es. 8 ferro -> 1 oro) e NON si consuma.
 * - Click destro su un blocco: lo trasforma (pietra <-> pietrisco, sabbia <-> ghiaia...).
 *   Con Shift trasforma un'area 3x3 sulla faccia colpita.
 */
public class ShiftingStoneItem extends Item {
    /** Trasformazioni nel mondo: blocco -> nuovo blocco */
    private static final Map<Block, Block> SHIFTS = Map.ofEntries(
            Map.entry(Blocks.STONE, Blocks.COBBLESTONE),
            Map.entry(Blocks.COBBLESTONE, Blocks.STONE),
            Map.entry(Blocks.SAND, Blocks.GRAVEL),
            Map.entry(Blocks.GRAVEL, Blocks.SAND),
            Map.entry(Blocks.DIRT, Blocks.GRASS_BLOCK),
            Map.entry(Blocks.GRASS_BLOCK, Blocks.DIRT),
            Map.entry(Blocks.GLASS, Blocks.SAND),
            Map.entry(Blocks.PUMPKIN, Blocks.MELON),
            Map.entry(Blocks.MELON, Blocks.PUMPKIN),
            Map.entry(Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE),
            Map.entry(Blocks.COBBLED_DEEPSLATE, Blocks.DEEPSLATE),
            Map.entry(Blocks.NETHERRACK, Blocks.COBBLESTONE),
            Map.entry(Blocks.OAK_LOG, Blocks.BIRCH_LOG),
            Map.entry(Blocks.BIRCH_LOG, Blocks.SPRUCE_LOG),
            Map.entry(Blocks.SPRUCE_LOG, Blocks.JUNGLE_LOG),
            Map.entry(Blocks.JUNGLE_LOG, Blocks.ACACIA_LOG),
            Map.entry(Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG),
            Map.entry(Blocks.DARK_OAK_LOG, Blocks.OAK_LOG));

    public ShiftingStoneItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    // resta nel banco da lavoro
    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return stack.copy();
    }

    /** Shift + click destro in aria: apre un banco da lavoro portatile */
    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return net.minecraft.world.InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            var access = net.minecraft.world.inventory.ContainerLevelAccess.create(level, player.blockPosition());
            player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inventory, p) ->
                    new net.minecraft.world.inventory.CraftingMenu(id, inventory, access) {
                        @Override
                        public boolean stillValid(net.minecraft.world.entity.player.Player who) {
                            return true;
                        }
                    }, Component.translatable("container.crafting")));
        }
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos center = context.getClickedPos();
        if (!SHIFTS.containsKey(level.getBlockState(center).getBlock())) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            boolean area = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
            int r = area ? 1 : 0;
            var axis = context.getClickedFace().getAxis();
            Block source = level.getBlockState(center).getBlock();
            for (int a = -r; a <= r; a++) {
                for (int b = -r; b <= r; b++) {
                    BlockPos pos = switch (axis) {
                        case X -> center.offset(0, a, b);
                        case Y -> center.offset(a, 0, b);
                        case Z -> center.offset(a, b, 0);
                    };
                    if (level.getBlockState(pos).is(source)) {
                        level.setBlock(pos, SHIFTS.get(source).defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
            level.playSound(null, center, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.2F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.futuretek.shifting_stone").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.futuretek.shifting_stone_craft").withStyle(ChatFormatting.GRAY));
    }
}
