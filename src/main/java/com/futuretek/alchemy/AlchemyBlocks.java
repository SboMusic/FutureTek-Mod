package com.futuretek.alchemy;

import java.util.List;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Blocchi alchemici: Carica Stellare e Lanterna Repulsiva. */
public final class AlchemyBlocks {
    private AlchemyBlocks() {
    }

    /**
     * Carica Stellare: un'esplosione "pulita" di raggio 6.
     * Si innesca con un segnale di redstone o con l'acciarino; dopo 4 secondi rompe tutto intorno
     * e lascia cadere TUTTI gli oggetti al centro (niente va distrutto). Non rompe bauli e macchine.
     */
    public static class StarCharge extends Block {
        public static final BooleanProperty LIT = BlockStateProperties.LIT;
        public static final int RADIUS = 6;
        public static final int FUSE_TICKS = 80;

        public StarCharge(Properties properties) {
            super(properties);
            registerDefaultState(defaultBlockState().setValue(LIT, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(LIT);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return simpleCodec(StarCharge::new);
        }

        public static void prime(Level level, BlockPos pos, BlockState state) {
            if (state.getValue(LIT) || level.isClientSide) {
                return;
            }
            level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
            level.scheduleTick(pos, state.getBlock(), FUSE_TICKS);
            level.playSound(null, pos, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 0.8F);
        }

        @Override
        protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
            if (level.hasNeighborSignal(pos)) {
                prime(level, pos, state);
            }
        }

        @Override
        protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
            if (!old.is(state.getBlock()) && level.hasNeighborSignal(pos)) {
                prime(level, pos, state);
            }
        }

        @Override
        protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                Player player, InteractionHand hand, BlockHitResult hit) {
            if (!stack.is(Items.FLINT_AND_STEEL) && !stack.is(Items.FIRE_CHARGE)) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            prime(level, pos, state);
            if (!player.isCreative()) {
                if (stack.is(Items.FLINT_AND_STEEL)) {
                    stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                } else {
                    stack.shrink(1);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (state.getValue(LIT)) {
                detonate(level, pos);
            }
        }

        public static void detonate(ServerLevel level, BlockPos pos) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
            level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0F, 0.9F);
            AreaBreaker.breakBlocks(level, AreaBreaker.sphere(pos, RADIUS), null, pos, 0);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(RADIUS))) {
                entity.hurt(level.damageSources().explosion(null, null), 6.0F);
            }
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (state.getValue(LIT)) {
                level.addParticle(ParticleTypes.END_ROD, pos.getX() + random.nextDouble(), pos.getY() + 1.05,
                        pos.getZ() + random.nextDouble(), 0, 0.05, 0);
            }
        }
    }

    /**
     * Lanterna Repulsiva: tiene lontani i mostri e respinge i loro proiettili (raggio 8).
     * Funziona da sola, senza energia: si "risveglia" ogni 2 tick.
     */
    public static class RepulsionLantern extends Block {
        public static final int RADIUS = 8;
        private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 11, 12);

        public RepulsionLantern(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return simpleCodec(RepulsionLantern::new);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
            level.scheduleTick(pos, this, 2);
        }

        @Override
        protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            repel(level, pos);
            level.scheduleTick(pos, this, 2);
        }

        public static void repel(ServerLevel level, BlockPos pos) {
            Vec3 center = Vec3.atCenterOf(pos);
            List<Entity> entities = level.getEntities((Entity) null, new AABB(pos).inflate(RADIUS),
                    e -> e instanceof Enemy || (e instanceof Projectile p && !(p.getOwner() instanceof Player)));
            for (Entity entity : entities) {
                Vec3 away = entity.position().subtract(center);
                if (away.lengthSqr() > RADIUS * RADIUS) {
                    continue;
                }
                Vec3 push = away.lengthSqr() < 0.01 ? new Vec3(0, 0.5, 0) : away.normalize().scale(0.6);
                entity.setDeltaMovement(push.x, Math.max(entity.getDeltaMovement().y, 0.15), push.z);
                entity.hurtMarked = true;
            }
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(4) == 0) {
                level.addParticle(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                        random.nextGaussian() * 0.5, 0.2, random.nextGaussian() * 0.5);
            }
        }
    }
}
