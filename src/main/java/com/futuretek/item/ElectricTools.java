package com.futuretek.item;

import java.util.List;

import com.futuretek.alchemy.AlchemyItem;
import com.futuretek.alchemy.AlchemyTools;
import com.futuretek.energy.EnergyUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Fase 11 - utensili e armi ELETTRICI. Non si consumano: usano energia (TU) e si ricaricano
 * nel banco accumulatore (la casella in alto). Scarichi diventano lentissimi e non raccolgono i minerali.
 *  - Trapano elettrico: piccone + pala di livello FERRO
 *  - Trapano di diamante: piccone + pala di livello DIAMANTE; Shift + click destro = modalita' 3x3
 *  - Motosega: ascia veloce (abbatte anche gli alberi interi)
 *  - Lama al plasma: spada fortissima finche' e' carica
 *  - Fucile a impulsi: colpisce a distanza (fino a 32 blocchi) con un raggio di energia
 */
public final class ElectricTools {
    private ElectricTools() {
    }

    public static final Tier IRON_ELECTRIC = new SimpleTier(BlockTags.INCORRECT_FOR_IRON_TOOL, 0, 9.0F, 2.0F, 8,
            () -> net.minecraft.world.item.crafting.Ingredient.EMPTY);
    public static final Tier DIAMOND_ELECTRIC = new SimpleTier(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 0, 16.0F, 3.0F, 10,
            () -> net.minecraft.world.item.crafting.Ingredient.EMPTY);

    /** Blocchi che si scavano col trapano: quelli da piccone e quelli da pala */
    public static final TagKey<Block> DRILL_MINEABLE = BlockTags.create(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("futuretek", "mineable/drill"));

    // --- parti comuni (barretta, descrizione) ---
    static int bar(ItemStack stack, int capacity) {
        return Math.round(13.0F * EnergyItem.getEnergy(stack) / capacity);
    }

    static void tooltip(Item item, ItemStack stack, int capacity, int cost, List<Component> tooltip) {
        tooltip.add(Component.translatable("gui.futuretek.energy", EnergyUtil.format(EnergyItem.getEnergy(stack)),
                EnergyUtil.format(capacity)).withStyle(ChatFormatting.GRAY));
        AlchemyItem.addDescription(item, tooltip);
        if (EnergyItem.getEnergy(stack) < cost) {
            tooltip.add(Component.translatable("tooltip.futuretek.electric_empty").withStyle(ChatFormatting.RED));
        }
    }

    static boolean charged(ItemStack stack, int cost) {
        return EnergyItem.getEnergy(stack) >= cost;
    }

    /** Trapani (iron e diamante) */
    public static class Drill extends DiggerItem implements EnergyItem, AlchemyTools.AreaTool {
        private final int capacity;
        private final int cost;
        private final int rate;
        private final boolean area;

        public Drill(Tier tier, int capacity, int cost, int rate, boolean area, Properties props) {
            super(tier, DRILL_MINEABLE, AlchemyTools.unbreakable(props.stacksTo(1))
                    .attributes(DiggerItem.createAttributes(tier, 1.0F, -2.8F)));
            this.capacity = capacity;
            this.cost = cost;
            this.rate = rate;
            this.area = area;
        }

        @Override
        public float getDestroySpeed(ItemStack stack, BlockState state) {
            return charged(stack, cost) ? super.getDestroySpeed(stack, state) : 1.0F;
        }

        @Override
        public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
            return charged(stack, cost) && super.isCorrectToolForDrops(stack, state);
        }

        @Override
        public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
            if (!level.isClientSide && state.getDestroySpeed(level, pos) != 0 && !(miner instanceof Player p && p.isCreative())) {
                EnergyItem.use(stack, cost);
            }
            return true;
        }

        @Override
        public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        }

        @Override
        public int areaRadius(ItemStack stack) {
            return area ? AlchemyTools.toggleRadius(stack) : 0;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            return area ? AlchemyTools.toggleArea(level, player, hand) : super.use(level, player, hand);
        }

        @Override
        public int getCapacity() {
            return capacity;
        }

        @Override
        public int getRate() {
            return rate;
        }

        @Override
        public boolean isBarVisible(ItemStack stack) {
            return true;
        }

        @Override
        public int getBarWidth(ItemStack stack) {
            return bar(stack, capacity);
        }

        @Override
        public int getBarColor(ItemStack stack) {
            return 0x40C8FF;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            tooltip(this, stack, capacity, cost, tooltip);
            if (area) {
                boolean on = AlchemyTools.toggleRadius(stack) > 0;
                tooltip.add(Component.translatable(on ? "tooltip.futuretek.area_on" : "tooltip.futuretek.area_off")
                        .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
            }
        }
    }

    /** Motosega: un'ascia elettrica veloce */
    public static class Chainsaw extends AxeItem implements EnergyItem {
        public static final int CAPACITY = 40_000;
        public static final int COST = 50;

        public Chainsaw(Properties props) {
            super(IRON_ELECTRIC, AlchemyTools.unbreakable(props.stacksTo(1))
                    .attributes(DiggerItem.createAttributes(IRON_ELECTRIC, 5.0F, -3.0F)));
        }

        @Override
        public float getDestroySpeed(ItemStack stack, BlockState state) {
            if (!charged(stack, COST)) {
                return 1.0F;
            }
            return state.is(BlockTags.LEAVES) || state.is(BlockTags.WOOL) ? 15.0F : super.getDestroySpeed(stack, state) * 1.4F;
        }

        @Override
        public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
            return charged(stack, COST) && super.isCorrectToolForDrops(stack, state);
        }

        @Override
        public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
            if (!level.isClientSide && state.getDestroySpeed(level, pos) != 0 && !(miner instanceof Player p && p.isCreative())) {
                EnergyItem.use(stack, COST);
            }
            return true;
        }

        @Override
        public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        }

        @Override
        public int getCapacity() {
            return CAPACITY;
        }

        @Override
        public int getRate() {
            return 128;
        }

        @Override
        public boolean isBarVisible(ItemStack stack) {
            return true;
        }

        @Override
        public int getBarWidth(ItemStack stack) {
            return bar(stack, CAPACITY);
        }

        @Override
        public int getBarColor(ItemStack stack) {
            return 0x40C8FF;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            tooltip(this, stack, CAPACITY, COST, tooltip);
        }
    }

    /** Lama al plasma: 13 di danno da carica (250 TU a colpo), scarica fa solo 3 */
    public static class PlasmaSaber extends SwordItem implements EnergyItem {
        public static final int CAPACITY = 80_000;
        public static final int COST = 250;
        public static final float EMPTY_DAMAGE = 3.0F;

        public PlasmaSaber(Properties props) {
            super(DIAMOND_ELECTRIC, AlchemyTools.unbreakable(props.stacksTo(1))
                    .attributes(SwordItem.createAttributes(DIAMOND_ELECTRIC, 9, -2.2F)));
        }

        @Override
        public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        }

        @Override
        public int getCapacity() {
            return CAPACITY;
        }

        @Override
        public int getRate() {
            return 512;
        }

        @Override
        public boolean isBarVisible(ItemStack stack) {
            return true;
        }

        @Override
        public int getBarWidth(ItemStack stack) {
            return bar(stack, CAPACITY);
        }

        @Override
        public int getBarColor(ItemStack stack) {
            return 0x40C8FF;
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return charged(stack, COST);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            tooltip(this, stack, CAPACITY, COST, tooltip);
        }
    }

    /** Fucile a impulsi: click destro = raggio fino a 32 blocchi, 8 di danno, 400 TU */
    public static class PulseRifle extends Item implements EnergyItem {
        public static final int CAPACITY = 100_000;
        public static final int COST = 400;
        public static final double RANGE = 32;
        public static final float DAMAGE = 8.0F;

        public PulseRifle(Properties props) {
            super(props.stacksTo(1));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!player.isCreative() && !charged(stack, COST)) {
                if (!level.isClientSide) {
                    player.displayClientMessage(Component.translatable("tooltip.futuretek.electric_empty"), true);
                }
                return InteractionResultHolder.fail(stack);
            }
            if (!level.isClientSide) {
                fire((ServerLevel) level, player);
                if (!player.isCreative()) {
                    EnergyItem.use(stack, COST);
                }
            }
            player.getCooldowns().addCooldown(this, 10);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        /** Il colpo: il primo mostro sulla linea di mira (fermato dai blocchi) */
        public static void fire(ServerLevel level, Player player) {
            Vec3 from = player.getEyePosition();
            Vec3 dir = player.getViewVector(1.0F);
            Vec3 to = from.add(dir.scale(RANGE));
            HitResult block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (block.getType() != HitResult.Type.MISS) {
                to = block.getLocation();
            }
            AABB box = player.getBoundingBox().expandTowards(to.subtract(from)).inflate(1.0);
            EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, from, to, box,
                    e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && e != player, 0.3F);
            if (hit != null) {
                to = hit.getLocation();
                Entity target = hit.getEntity();
                target.hurt(level.damageSources().playerAttack(player), DAMAGE);
                if (target instanceof LivingEntity living) {
                    living.knockback(0.5, -dir.x, -dir.z);
                }
            }
            double length = from.distanceTo(to);
            for (double d = 1; d < length; d += 0.5) {
                Vec3 p = from.add(dir.scale(d));
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y - 0.15, p.z, 1, 0, 0, 0, 0);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.6F, 1.8F);
        }

        @Override
        public int getCapacity() {
            return CAPACITY;
        }

        @Override
        public int getRate() {
            return 512;
        }

        @Override
        public boolean isBarVisible(ItemStack stack) {
            return true;
        }

        @Override
        public int getBarWidth(ItemStack stack) {
            return bar(stack, CAPACITY);
        }

        @Override
        public int getBarColor(ItemStack stack) {
            return 0x40C8FF;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            tooltip(this, stack, CAPACITY, COST, tooltip);
        }
    }

    /** La lama al plasma consuma energia a ogni colpo; scarica colpisce piano */
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof Player player) || event.getSource().isDirect() == false) {
            return;
        }
        ItemStack weapon = player.getMainHandItem();
        if (weapon.getItem() instanceof PlasmaSaber) {
            if (player.isCreative() || EnergyItem.use(weapon, PlasmaSaber.COST)) {
                return;
            }
            event.setAmount(Math.min(event.getAmount(), PlasmaSaber.EMPTY_DAMAGE));
        }
    }
}
