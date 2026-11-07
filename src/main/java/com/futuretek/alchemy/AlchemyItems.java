package com.futuretek.alchemy;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.futuretek.energy.EnergyUtil;
import com.futuretek.essence.EssenceManager;
import com.futuretek.essence.EssenceValues;
import com.futuretek.menu.SynthesisTableMenu;
import com.futuretek.registry.ModAttachments;
import com.futuretek.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/** Gli oggetti alchemici di FutureTek (ispirati alle meccaniche dell'alchimia, con nomi e regole nostri). */
public final class AlchemyItems {
    private AlchemyItems() {
    }

    /** Box intorno al giocatore */
    private static AABB around(Player player, double r) {
        return player.getBoundingBox().inflate(r);
    }

    // =====================================================================
    // Braci: combustibili potenti (anche per i generatori a combustione)
    // =====================================================================
    public static class Ember extends net.minecraft.world.item.Item {
        private final int burnTime;

        public Ember(int burnTime, Properties properties) {
            super(properties);
            this.burnTime = burnTime;
        }

        @Override
        public int getBurnTime(ItemStack stack, @Nullable net.minecraft.world.item.crafting.RecipeType<?> recipeType) {
            return burnTime;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("tooltip.futuretek.ember", burnTime / 200).withStyle(ChatFormatting.GRAY));
        }
    }

    // =====================================================================
    // Catalizzatore del Vuoto: rompe un'area davanti a te
    // =====================================================================
    public static class VoidCatalyst extends AlchemyItem {
        public static final long COST_PER_BLOCK = 4;
        public static final int MAX_CHARGE = 4;

        public VoidCatalyst(Properties properties) {
            super(false, properties);
        }

        /** Carica 1-4: area 3x3x3, 5x5x5, 7x7x7, 9x9x9 */
        public static int charge(ItemStack stack) {
            return Math.max(1, Math.min(MAX_CHARGE, stack.getOrDefault(ModDataComponents.CHARGE.get(), 1)));
        }

        /** Click destro in aria: aumenta la carica (dopo la 4 torna alla 1) */
        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide) {
                int next = charge(stack) % MAX_CHARGE + 1;
                stack.set(ModDataComponents.CHARGE.get(), next);
                int side = next * 2 + 1;
                level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6F,
                        0.8F + next * 0.2F);
                player.displayClientMessage(Component.translatable("message.futuretek.catalyst_charge", next, side, side, side),
                        true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            Player player = context.getPlayer();
            if (player == null) {
                return InteractionResult.PASS;
            }
            if (context.getLevel() instanceof ServerLevel level) {
                int charge = charge(context.getItemInHand());
                // con Shift: solo lo strato che tocchi (utile per spianare un muro senza scavare in profondita')
                int depth = player.isShiftKeyDown() ? 1 : charge * 2 + 1;
                Direction into = context.getClickedFace().getOpposite();
                List<BlockPos> area = AreaBreaker.box(context.getClickedPos(), into, charge, depth);
                int broken = AreaBreaker.breakBlocks(level, area, player, context.getClickedPos(), COST_PER_BLOCK);
                if (broken > 0) {
                    level.playSound(null, context.getClickedPos(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS,
                            0.6F, 1.3F);
                    player.getCooldowns().addCooldown(this, 4 + charge * 2);
                }
            }
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }

        @Override
        public boolean isBarVisible(ItemStack stack) {
            return true;
        }

        @Override
        public int getBarWidth(ItemStack stack) {
            return Math.round(13.0F * charge(stack) / MAX_CHARGE);
        }

        @Override
        public int getBarColor(ItemStack stack) {
            return 0xFF5A3C;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            super.appendHoverText(stack, context, tooltip, flag);
            int side = charge(stack) * 2 + 1;
            tooltip.add(Component.translatable("tooltip.futuretek.catalyst_charge", charge(stack), side, side, side)
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    // =====================================================================
    // Lente Cinetica: colpisce a distanza e scava una sfera
    // =====================================================================
    public static class KineticLens extends AlchemyItem {
        public static final long COST_PER_BLOCK = 8;

        public KineticLens(Properties properties) {
            super(false, properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level instanceof ServerLevel server) {
                Vec3 eye = player.getEyePosition();
                Vec3 end = eye.add(player.getLookAngle().scale(64));
                BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
                if (hit.getType() == HitResult.Type.BLOCK) {
                    BlockPos center = hit.getBlockPos();
                    server.sendParticles(ParticleTypes.EXPLOSION, center.getX() + 0.5, center.getY() + 0.5,
                            center.getZ() + 0.5, 3, 1, 1, 1, 0);
                    server.playSound(null, center, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
                    AreaBreaker.breakBlocks(server, AreaBreaker.sphere(center, 2), player, center, COST_PER_BLOCK);
                    player.getCooldowns().addCooldown(this, 10);
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }

    // =====================================================================
    // Bisaccia Dimensionale: 54 slot personali, uguali ovunque
    // =====================================================================
    public static class Satchel extends AlchemyItem {
        public Satchel(Properties properties) {
            super(false, properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            if (!level.isClientSide) {
                SatchelInventory inventory = player.getData(ModAttachments.SATCHEL);
                player.openMenu(new SimpleMenuProvider((id, inv, p) -> ChestMenu.sixRows(id, inv, inventory),
                        player.getItemInHand(hand).getHoverName()));
                level.playSound(null, player.blockPosition(), SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 0.6F, 1.0F);
            }
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
        }
    }

    // =====================================================================
    // Anello Attrattore: attira oggetti e sfere di esperienza
    // =====================================================================
    public static class AttractorRing extends AlchemyItem {
        public AttractorRing(Properties properties) {
            super(true, properties);
        }

        @Override
        protected void activeTick(ItemStack stack, ServerLevel level, Player player) {
            if (level.getGameTime() % 2 != 0) {
                return;
            }
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, around(player, 8))) {
                item.setPos(player.getX(), player.getY(), player.getZ());
                item.setNoPickUpDelay();
            }
            for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, around(player, 8))) {
                orb.setPos(player.getX(), player.getY(), player.getZ());
            }
        }
    }

    // =====================================================================
    // Anello dei Venti: volare come in creativa (costa Essenza mentre voli)
    // =====================================================================
    public static class WindriderRing extends AlchemyItem {
        private static final String TAG = "futuretek_ring_flight";
        public static final long COST_PER_SECOND = 2;

        public WindriderRing(Properties properties) {
            super(true, properties);
        }

        @Override
        protected void activeTick(ItemStack stack, ServerLevel level, Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return;
            }
            player.getPersistentData().putLong(TAG, level.getGameTime());
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
            if (player.getAbilities().flying && level.getGameTime() % 20 == 0
                    && !EssenceManager.consumeOrWarn(player, COST_PER_SECOND)) {
                setActive(stack, false);
                stopFlight(player);
            }
        }

        @Override
        protected void onDeactivated(ItemStack stack, Player player) {
            stopFlight(player);
        }

        private static void stopFlight(Player player) {
            player.getPersistentData().remove(TAG);
            if (!player.isCreative() && !player.isSpectator()) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }

        /** Se l'anello non e' piu' nell'inventario (gettato, messo in un baule...) il volo finisce */
        public static void onPlayerTick(PlayerTickEvent.Post event) {
            Player player = event.getEntity();
            if (!player.level().isClientSide && player.getPersistentData().contains(TAG)
                    && player.level().getGameTime() - player.getPersistentData().getLong(TAG) > 5) {
                stopFlight(player);
            }
        }
    }

    // =====================================================================
    // Anello Ardente: immune al fuoco; click destro lancia una palla di fuoco
    // =====================================================================
    public static class EmberRing extends AlchemyItem {
        public static final long COST = 16;

        public EmberRing(Properties properties) {
            super(false, properties);
        }

        @Override
        protected void passiveTick(ItemStack stack, ServerLevel level, Player player) {
            if (level.getGameTime() % 20 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, true, false));
            }
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide && EssenceManager.consumeOrWarn(player, COST)) {
                Vec3 look = player.getLookAngle();
                SmallFireball fireball = new SmallFireball(level, player, look.normalize());
                fireball.setPos(player.getX() + look.x, player.getEyeY() + look.y * 0.5, player.getZ() + look.z);
                level.addFreshEntity(fireball);
                level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
                player.getCooldowns().addCooldown(this, 8);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }

    // =====================================================================
    // Anello Glaciale: cammini sull'acqua ghiacciandola; click destro congela tutto intorno
    // =====================================================================
    public static class FrostRing extends AlchemyItem {
        public static final long COST = 32;

        public FrostRing(Properties properties) {
            super(false, properties);
        }

        @Override
        protected void passiveTick(ItemStack stack, ServerLevel level, Player player) {
            if (!player.onGround() && !player.isInWater()) {
                return;
            }
            BlockPos feet = player.blockPosition().below();
            for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-3, 0, -3), feet.offset(3, 0, 3))) {
                BlockState state = level.getBlockState(pos);
                if (state.is(Blocks.WATER) && state.getFluidState().isSource() && level.getBlockState(pos.above()).isAir()
                        && pos.distSqr(feet) <= 10) {
                    level.setBlockAndUpdate(pos, Blocks.FROSTED_ICE.defaultBlockState());
                    level.scheduleTick(pos.immutable(), Blocks.FROSTED_ICE, 60 + level.random.nextInt(60));
                }
            }
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level instanceof ServerLevel server && EssenceManager.consumeOrWarn(player, COST)) {
                for (BlockPos pos : AreaBreaker.sphere(player.blockPosition(), 4)) {
                    BlockState state = server.getBlockState(pos);
                    if (state.is(Blocks.WATER) && state.getFluidState().isSource()) {
                        server.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
                    } else if (state.is(Blocks.LAVA)) {
                        server.setBlockAndUpdate(pos, state.getFluidState().isSource()
                                ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
                    } else if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
                        server.removeBlock(pos, false);
                    }
                }
                player.clearFire();
                server.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1, player.getZ(), 60, 3, 1, 3, 0.02);
                server.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 0.6F);
                player.getCooldowns().addCooldown(this, 20);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }

    // =====================================================================
    // Anello Verdeggiante: fa crescere piante e coltivazioni intorno a te
    // =====================================================================
    public static class VerdantRing extends AlchemyItem {
        public VerdantRing(Properties properties) {
            super(true, properties);
        }

        public static boolean isGrowable(Block block) {
            return block instanceof CropBlock || block instanceof StemBlock || block instanceof SaplingBlock
                    || block instanceof SweetBerryBushBlock || block instanceof CocoaBlock || block instanceof NetherWartBlock
                    || block instanceof SugarCaneBlock || block instanceof CactusBlock || block instanceof BambooStalkBlock;
        }

        @Override
        protected void activeTick(ItemStack stack, ServerLevel level, Player player) {
            if (level.getGameTime() % 10 != 0) {
                return;
            }
            BlockPos center = player.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-5, -2, -5), center.offset(5, 2, 5))) {
                BlockState state = level.getBlockState(pos);
                if (isGrowable(state.getBlock())) {
                    state.randomTick(level, pos.immutable(), level.random);
                    if (level.random.nextInt(12) == 0) {
                        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.7,
                                pos.getZ() + 0.5, 2, 0.3, 0.2, 0.3, 0);
                    }
                }
            }
        }
    }

    // =====================================================================
    // Clessidra Arcana: accelera macchine e blocchi intorno a te (costa Essenza)
    // =====================================================================
    public static class ArcaneHourglass extends AlchemyItem {
        public static final int RADIUS = 4;
        public static final int EXTRA_TICKS = 3;
        public static final long COST_PER_SECOND = 4;

        public ArcaneHourglass(Properties properties) {
            super(true, properties);
        }

        @Override
        protected void activeTick(ItemStack stack, ServerLevel level, Player player) {
            if (level.getGameTime() % 20 == 0 && !EssenceManager.consumeOrWarn(player, COST_PER_SECOND)) {
                setActive(stack, false);
                return;
            }
            accelerate(level, player.blockPosition(), RADIUS, EXTRA_TICKS);
        }

        /** Fa "girare" piu' volte i blocchi con un cervello (macchine, fornaci...) e i blocchi che crescono */
        @SuppressWarnings("unchecked")
        public static void accelerate(ServerLevel level, BlockPos center, int radius, int extraTicks) {
            List<BlockEntity> entities = new ArrayList<>();
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be != null) {
                    entities.add(be);
                }
            }
            for (BlockEntity be : entities) {
                if (be.isRemoved()) {
                    continue;
                }
                BlockState state = be.getBlockState();
                BlockEntityTicker<BlockEntity> ticker = state.getTicker(level, (BlockEntityType<BlockEntity>) be.getType());
                if (ticker != null) {
                    for (int i = 0; i < extraTicks && !be.isRemoved(); i++) {
                        ticker.tick(level, be.getBlockPos(), state, be);
                    }
                }
            }
            for (int i = 0; i < 6; i++) {
                BlockPos pos = center.offset(level.random.nextInt(radius * 2 + 1) - radius,
                        level.random.nextInt(radius * 2 + 1) - radius, level.random.nextInt(radius * 2 + 1) - radius);
                BlockState state = level.getBlockState(pos);
                if (state.isRandomlyTicking()) {
                    state.randomTick(level, pos, level.random);
                }
            }
        }
    }

    // =====================================================================
    // Amuleto Vitale: ti cura e ti sfama (costa Essenza)
    // =====================================================================
    public static class VitalityAmulet extends AlchemyItem {
        public static final long FOOD_COST = 16;
        public static final long HEAL_COST = 32;

        public VitalityAmulet(Properties properties) {
            super(true, properties);
        }

        @Override
        protected void activeTick(ItemStack stack, ServerLevel level, Player player) {
            if (level.getGameTime() % 40 != 0) {
                return;
            }
            if (player.getFoodData().needsFood() && EssenceManager.consume(player, FOOD_COST)) {
                player.getFoodData().eat(1, 0.3F);
            }
            if (player.getHealth() < player.getMaxHealth() && EssenceManager.consume(player, HEAL_COST)) {
                player.heal(1.0F);
            }
        }
    }

    // =====================================================================
    // Gemma della Memoria: conserva l'esperienza
    // =====================================================================
    public static class MemoryGem extends AlchemyItem {
        public MemoryGem(Properties properties) {
            super(false, properties);
        }

        public static int stored(ItemStack stack) {
            return stack.getOrDefault(ModDataComponents.STORED_XP.get(), 0);
        }

        /** Punti esperienza totali del giocatore (livelli + barra) */
        public static int totalXp(Player player) {
            int level = player.experienceLevel;
            long forLevels;
            if (level <= 16) {
                forLevels = (long) level * level + 6L * level;
            } else if (level <= 31) {
                forLevels = (long) (2.5 * level * level - 40.5 * level + 360);
            } else {
                forLevels = (long) (4.5 * level * level - 162.5 * level + 2220);
            }
            return (int) Math.min(Integer.MAX_VALUE,
                    forLevels + Math.round(player.experienceProgress * player.getXpNeededForNextLevel()));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (player instanceof ServerPlayer sp) {
                if (player.isShiftKeyDown()) {
                    int xp = stored(stack);
                    if (xp > 0) {
                        stack.set(ModDataComponents.STORED_XP.get(), 0);
                        sp.giveExperiencePoints(xp);
                        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.0F);
                    }
                } else {
                    int xp = totalXp(player);
                    if (xp > 0) {
                        long total = (long) stored(stack) + xp;
                        stack.set(ModDataComponents.STORED_XP.get(), (int) Math.min(Integer.MAX_VALUE, total));
                        sp.setExperienceLevels(0);
                        sp.setExperiencePoints(0);
                        player.totalExperience = 0;
                        level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 0.6F);
                    }
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return stored(stack) > 0;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            super.appendHoverText(stack, context, tooltip, flag);
            tooltip.add(Component.translatable("tooltip.futuretek.stored_xp", stored(stack)).withStyle(ChatFormatting.GREEN));
        }
    }

    // =====================================================================
    // Amuleti delle Maree e del Magma: piazzano acqua / lava
    // =====================================================================
    public static class FluidAmulet extends AlchemyItem {
        private final boolean lava;
        public static final long LAVA_COST = 64;

        public FluidAmulet(boolean lava, Properties properties) {
            super(false, properties);
            this.lava = lava;
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            Level level = context.getLevel();
            Player player = context.getPlayer();
            BlockPos pos = level.getBlockState(context.getClickedPos()).canBeReplaced()
                    ? context.getClickedPos() : context.getClickedPos().relative(context.getClickedFace());
            if (!level.getBlockState(pos).canBeReplaced()) {
                return InteractionResult.FAIL;
            }
            if (!level.isClientSide && player != null) {
                if (!lava && level.dimensionType().ultraWarm()) {
                    level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F);
                    return InteractionResult.SUCCESS;
                }
                if (lava && !EssenceManager.consumeOrWarn(player, LAVA_COST)) {
                    return InteractionResult.FAIL;
                }
                level.setBlock(pos, (lava ? Blocks.LAVA : Blocks.WATER).defaultBlockState(), Block.UPDATE_ALL);
                level.playSound(null, pos, lava ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY,
                        SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    /** L'Amuleto delle Maree e' una fonte d'acqua infinita anche per le macchine (es. bagno chimico) */
    public static class InfiniteWater implements IFluidHandlerItem {
        private final ItemStack container;

        public InfiniteWater(ItemStack container) {
            this.container = container;
        }

        @Override
        public ItemStack getContainer() {
            return container;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return new FluidStack(Fluids.WATER, 1000);
        }

        @Override
        public int getTankCapacity(int tank) {
            return 1000;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
            return resource.is(Fluids.WATER) ? resource.copy() : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
            return new FluidStack(Fluids.WATER, Math.min(maxDrain, 1000));
        }
    }

    // =====================================================================
    // Bacchetta Rabdomante: quanta Essenza c'e' sotto (o dietro) il blocco
    // =====================================================================
    public static class DowsingRod extends AlchemyItem {
        public DowsingRod(Properties properties) {
            super(false, properties);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            Level level = context.getLevel();
            Player player = context.getPlayer();
            if (!level.isClientSide && player != null) {
                long total = 0;
                long best = 0;
                ItemStack bestItem = ItemStack.EMPTY;
                for (BlockPos pos : AreaBreaker.box(context.getClickedPos(), context.getClickedFace().getOpposite(), 1, 8)) {
                    ItemStack asItem = new ItemStack(level.getBlockState(pos).getBlock().asItem());
                    long value = EssenceValues.get(false, asItem.getItem());
                    total += value;
                    if (value > best) {
                        best = value;
                        bestItem = asItem;
                    }
                }
                player.displayClientMessage(bestItem.isEmpty()
                        ? Component.translatable("message.futuretek.dowsing_none")
                        : Component.translatable("message.futuretek.dowsing", EnergyUtil.format(total),
                                bestItem.getHoverName(), EnergyUtil.format(best)), false);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    // =====================================================================
    // Bacchetta del Costruttore: piazza blocchi pagandoli in Essenza (il blocco in mano sinistra fa da modello)
    // =====================================================================
    public static class BuilderWand extends AlchemyItem {
        public BuilderWand(Properties properties) {
            super(false, properties);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            Level level = context.getLevel();
            Player player = context.getPlayer();
            if (player == null) {
                return InteractionResult.PASS;
            }
            ItemStack model = player.getOffhandItem();
            if (!(model.getItem() instanceof BlockItem blockItem)) {
                if (!level.isClientSide) {
                    player.displayClientMessage(Component.translatable("message.futuretek.builder_no_block")
                            .withStyle(ChatFormatting.RED), true);
                }
                return InteractionResult.FAIL;
            }
            if (!level.isClientSide) {
                long cost = Math.max(1, EssenceValues.get(false, blockItem));
                BlockPos start = context.getClickedPos().relative(context.getClickedFace());
                int radius = player.isShiftKeyDown() ? 0 : 1;
                BlockState place = blockItem.getBlock().defaultBlockState();
                int placed = 0;
                for (BlockPos pos : AreaBreaker.box(start, context.getClickedFace(), radius, 1)) {
                    if (!level.getBlockState(pos).canBeReplaced() || !level.mayInteract(player, pos)
                            || !place.canSurvive(level, pos)) {
                        continue;
                    }
                    if (!EssenceManager.consumeOrWarn(player, cost)) {
                        break;
                    }
                    level.setBlock(pos, place, Block.UPDATE_ALL);
                    placed++;
                }
                if (placed > 0) {
                    level.playSound(null, start, place.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    // =====================================================================
    // Talismano Riparatore: ripara piano piano gli oggetti nell'inventario
    // =====================================================================
    public static class RepairTalisman extends AlchemyItem {
        public RepairTalisman(Properties properties) {
            super(false, properties);
        }

        @Override
        protected void passiveTick(ItemStack stack, ServerLevel level, Player player) {
            if (level.getGameTime() % 20 != 0) {
                return;
            }
            var inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack other = inventory.getItem(i);
                if (other.isDamaged() && !(player.swinging && other == player.getMainHandItem())) {
                    other.setDamageValue(other.getDamageValue() - 1);
                }
            }
        }
    }

    // =====================================================================
    // Tavoletta di Sintesi: il Tavolo di Sintesi in tasca
    // =====================================================================
    public static class SynthesisTablet extends AlchemyItem {
        public SynthesisTablet(Properties properties) {
            super(false, properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            if (!level.isClientSide) {
                player.openMenu(new SimpleMenuProvider(
                        (id, inventory, p) -> new SynthesisTableMenu(id, inventory, ContainerLevelAccess.NULL, true),
                        player.getItemInHand(hand).getHoverName()));
            }
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
        }
    }

    // =====================================================================
    // Tomo dell'Onniscienza: impari tutti gli oggetti (si consuma)
    // =====================================================================
    public static class OmniscienceTome extends AlchemyItem {
        public OmniscienceTome(Properties properties) {
            super(false, properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide) {
                int added = EssenceManager.learnAll(player);
                player.displayClientMessage(Component.translatable("message.futuretek.learned_all", added)
                        .withStyle(ChatFormatting.LIGHT_PURPLE), false);
                level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
                stack.consume(1, player);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return true;
        }
    }
}
