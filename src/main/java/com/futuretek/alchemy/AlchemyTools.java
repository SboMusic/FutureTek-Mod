package com.futuretek.alchemy;

import java.util.List;

import com.futuretek.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Attrezzi e armature di Nocturnite (livello 1) e Ardorite (livello 2).
 * - Non si rompono mai.
 * - Piccone, pala, ascia e zappa: Shift + click destro in aria attiva/disattiva lo SCAVO AD AREA 3x3
 *   (tenendo premuto Shift mentre scavi, rompi un blocco solo).
 * - Maglio: scava sempre 3x3 (Ardorite: 5x5).
 * - Set completo di armatura: niente danni da caduta e immunita' al fuoco; Ardorite anche -50% danni e respiro in acqua.
 */
public final class AlchemyTools {
    private AlchemyTools() {
    }

    public static final Tier NOCTURNITE = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
            4000, 14.0F, 5.0F, 18, () -> net.minecraft.world.item.crafting.Ingredient.EMPTY);
    public static final Tier ARDORITE = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
            8000, 22.0F, 8.0F, 22, () -> net.minecraft.world.item.crafting.Ingredient.EMPTY);

    /** Proprieta' comuni: indistruttibile */
    public static Item.Properties unbreakable(Item.Properties props) {
        return props.component(net.minecraft.core.component.DataComponents.UNBREAKABLE, new Unbreakable(false))
                .fireResistant();
    }

    /** Oggetti che scavano ad area: raggio (0 = blocco singolo) */
    public interface AreaTool {
        int areaRadius(ItemStack stack);
    }

    public static int toggleRadius(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ACTIVE.get(), false) ? 1 : 0;
    }

    public static InteractionResultHolder<ItemStack> toggleArea(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            boolean now = !stack.getOrDefault(ModDataComponents.ACTIVE.get(), false);
            stack.set(ModDataComponents.ACTIVE.get(), now);
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F,
                    now ? 1.4F : 0.7F);
            player.displayClientMessage(Component.translatable(now ? "message.futuretek.area_on" : "message.futuretek.area_off"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    static void tooltip(Item item, ItemStack stack, List<Component> tooltip, boolean toggle) {
        AlchemyItem.addDescription(item, tooltip);
        if (toggle) {
            boolean on = stack.getOrDefault(ModDataComponents.ACTIVE.get(), false);
            tooltip.add(Component.translatable(on ? "tooltip.futuretek.area_on" : "tooltip.futuretek.area_off")
                    .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        }
    }

    // --- attrezzi ---
    public static class Pickaxe extends PickaxeItem implements AreaTool {
        public Pickaxe(Tier tier, Properties props) {
            super(tier, unbreakable(props).attributes(DiggerItem.createAttributes(tier, 1.0F, -2.8F)));
        }

        @Override
        public int areaRadius(ItemStack stack) {
            return toggleRadius(stack);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            return toggleArea(level, player, hand);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            tooltip(this, stack, tooltip, true);
        }
    }

    public static class Shovel extends ShovelItem implements AreaTool {
        public Shovel(Tier tier, Properties props) {
            super(tier, unbreakable(props).attributes(DiggerItem.createAttributes(tier, 1.5F, -3.0F)));
        }

        @Override
        public int areaRadius(ItemStack stack) {
            return toggleRadius(stack);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            return toggleArea(level, player, hand);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            tooltip(this, stack, tooltip, true);
        }
    }

    public static class Axe extends AxeItem implements AreaTool {
        public Axe(Tier tier, Properties props) {
            super(tier, unbreakable(props).attributes(DiggerItem.createAttributes(tier, 6.0F, -3.0F)));
        }

        @Override
        public int areaRadius(ItemStack stack) {
            return toggleRadius(stack);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            return toggleArea(level, player, hand);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            tooltip(this, stack, tooltip, true);
        }
    }

    public static class Hoe extends HoeItem {
        public Hoe(Tier tier, Properties props) {
            super(tier, unbreakable(props).attributes(DiggerItem.createAttributes(tier, -3.0F, 0.0F)));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            return toggleArea(level, player, hand);
        }

        /** In modalita' area ara un quadrato 3x3 */
        @Override
        public InteractionResult useOn(UseOnContext context) {
            InteractionResult result = super.useOn(context);
            if (toggleRadius(context.getItemInHand()) > 0 && result.consumesAction() && context.getPlayer() != null) {
                BlockPos center = context.getClickedPos();
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dz == 0) {
                            continue;
                        }
                        BlockPos pos = center.offset(dx, 0, dz);
                        super.useOn(new UseOnContext(context.getLevel(), context.getPlayer(), context.getHand(),
                                context.getItemInHand(), new BlockHitResult(pos.getCenter(), Direction.UP, pos, false)));
                    }
                }
            }
            return result;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            tooltip(this, stack, tooltip, true);
        }
    }

    public static class Sword extends SwordItem {
        public Sword(Tier tier, Properties props) {
            super(tier, unbreakable(props).attributes(SwordItem.createAttributes(tier, 3, -2.4F)));
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            tooltip(this, stack, tooltip, false);
        }
    }

    /** Maglio: scava sempre ad area (anche terra e sabbia) */
    public static class Hammer extends PickaxeItem implements AreaTool {
        private final int radius;

        public Hammer(Tier tier, int radius, Properties props) {
            super(tier, unbreakable(props).attributes(DiggerItem.createAttributes(tier, 4.0F, -3.2F)));
            this.radius = radius;
        }

        @Override
        public int areaRadius(ItemStack stack) {
            return radius;
        }

        @Override
        public float getDestroySpeed(ItemStack stack, BlockState state) {
            return state.is(BlockTags.MINEABLE_WITH_SHOVEL) ? getTier().getSpeed() * 0.6F : super.getDestroySpeed(stack, state);
        }

        @Override
        public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
            return state.is(BlockTags.MINEABLE_WITH_SHOVEL) || super.isCorrectToolForDrops(stack, state);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            tooltip(this, stack, tooltip, false);
        }
    }

    // --- scavo ad area ---
    private static final ThreadLocal<Boolean> BUSY = ThreadLocal.withInitial(() -> false);

    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || BUSY.get() || player.isShiftKeyDown()) {
            return;
        }
        ItemStack tool = player.getMainHandItem();
        if (!(tool.getItem() instanceof AreaTool areaTool)) {
            return;
        }
        int radius = areaTool.areaRadius(tool);
        BlockState centerState = event.getState();
        if (radius <= 0 || !tool.isCorrectToolForDrops(centerState)) {
            return;
        }
        HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
        Direction face = hit instanceof BlockHitResult blockHit ? blockHit.getDirection() : Direction.UP;
        BlockPos center = event.getPos();
        float centerHardness = centerState.getDestroySpeed(event.getLevel(), center);
        BUSY.set(true);
        try {
            for (BlockPos pos : AreaBreaker.box(center, face, radius, 1)) {
                if (pos.equals(center)) {
                    continue;
                }
                BlockState state = event.getLevel().getBlockState(pos);
                float hardness = state.getDestroySpeed(event.getLevel(), pos);
                if (state.isAir() || hardness < 0 || hardness > centerHardness + 3 || !tool.isCorrectToolForDrops(state)) {
                    continue;
                }
                player.gameMode.destroyBlock(pos);
            }
        } finally {
            BUSY.set(false);
        }
    }

    // --- armature ---
    public static class Armor extends ArmorItem {
        /** 1 = Nocturnite, 2 = Ardorite */
        public final int level;

        public Armor(Holder<ArmorMaterial> material, Type type, int level, Properties props) {
            super(material, type, unbreakable(props).durability(type.getDurability(level == 1 ? 50 : 80)));
            this.level = level;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable(level == 1 ? "tooltip.futuretek.nocturnite_armor"
                    : "tooltip.futuretek.ardorite_armor").withStyle(ChatFormatting.GRAY));
        }
    }

    /** Livello del set completo indossato: 0 = nessuno, 1 = Nocturnite (o misto), 2 = tutto Ardorite */
    public static int fullSetLevel(Player player) {
        int min = 2;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                EquipmentSlot.FEET}) {
            if (!(player.getItemBySlot(slot).getItem() instanceof Armor armor)) {
                return 0;
            }
            min = Math.min(min, armor.level);
        }
        return min;
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.level().getGameTime() % 20 != 0) {
            return;
        }
        int set = fullSetLevel(player);
        if (set >= 1) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, true, false));
        }
        if (set >= 2) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 60, 0, true, false));
        }
    }

    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && fullSetLevel(player) >= 1) {
            event.setDistance(0);
            event.setCanceled(true);
        }
    }

    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && fullSetLevel(player) >= 2) {
            event.setAmount(event.getAmount() * 0.5F);
        }
    }
}
