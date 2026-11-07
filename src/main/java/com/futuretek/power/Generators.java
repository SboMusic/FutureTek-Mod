package com.futuretek.power;

import com.futuretek.block.MachineBlock;
import com.futuretek.blockentity.CombustionGeneratorBlockEntity;
import com.futuretek.blockentity.MachineBlockEntity;
import com.futuretek.energy.EnergyTier;
import com.futuretek.energy.EnergyUtil;
import com.futuretek.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Generatori della Fase 10:
 *  - Generatore avanzato: 64 TU/t, il combustibile dura la meta' ma rende 1,6 volte di piu'
 *  - Pannelli solari II e III (di notte producono un po')
 *  - Mulino ad acqua: 2 TU/t per ogni lato a contatto con l'acqua (massimo 12)
 *  - Mulino a vento: piu' e' in alto e piu' produce (da y 64 in su), di piu' con la pioggia e il temporale
 */
public final class Generators {
    private Generators() {
    }

    public enum Kind {
        ADVANCED_GENERATOR, SOLAR_2, SOLAR_3, WATER_MILL, WIND_MILL
    }

    public static class Block extends MachineBlock {
        private final Kind kind;

        public Block(Kind kind, Properties properties) {
            super(properties);
            this.kind = kind;
        }

        public Kind getKind() {
            return kind;
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return simpleCodec(p -> new Block(Kind.WATER_MILL, p));
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return kind == Kind.ADVANCED_GENERATOR ? new AdvancedGenerator(pos, state) : new Passive(pos, state);
        }

        /** Click destro sui generatori senza interfaccia: mostra quanto stanno producendo */
        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                BlockHitResult hit) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof Passive passive) {
                player.displayClientMessage(Component.translatable("message.futuretek.generator_rate",
                        passive.currentRate(), EnergyUtil.format(passive.getEnergy().getEnergyStored()),
                        EnergyUtil.format(passive.getEnergy().getMaxEnergyStored())), true);
                return InteractionResult.SUCCESS;
            }
            return super.useWithoutItem(state, level, pos, player, hit);
        }
    }

    /** Generatore a combustione avanzato (stessa interfaccia del generatore normale) */
    public static class AdvancedGenerator extends CombustionGeneratorBlockEntity {
        public AdvancedGenerator(BlockPos pos, BlockState state) {
            super(ModBlockEntities.ADVANCED_GENERATOR.get(), pos, state, 40_000, EnergyTier.MEDIUM.rate);
        }

        @Override
        protected int rate() {
            return 64;
        }

        @Override
        protected int burnSpeed() {
            return 2;
        }

        @Override
        protected int outputRate() {
            return EnergyTier.MEDIUM.rate;
        }
    }

    /** Generatori "passivi": solari, acqua, vento */
    public static class Passive extends MachineBlockEntity {
        private int rate;

        public Passive(BlockPos pos, BlockState state) {
            super(ModBlockEntities.PASSIVE_GENERATOR.get(), pos, state, 20_000, 0, EnergyTier.MEDIUM.rate);
        }

        private Kind kind() {
            return getBlockState().getBlock() instanceof Block b ? b.getKind() : Kind.WATER_MILL;
        }

        public int currentRate() {
            return rate;
        }

        @Override
        public void serverTick() {
            if (level.getGameTime() % 20 == 0) {
                rate = computeRate(level, worldPosition, kind());
            }
            if (rate > 0) {
                energy.generate(rate);
            }
            setLit(rate > 0);
            EnergyUtil.pushToNeighbors(level, worldPosition, energy, EnergyTier.MEDIUM.rate);
        }

        /** Quanti TU per tick produce adesso, in questa posizione */
        public static int computeRate(Level level, BlockPos pos, Kind kind) {
            switch (kind) {
                case SOLAR_2, SOLAR_3 -> {
                    int day = kind == Kind.SOLAR_2 ? 32 : 128;
                    if (!level.canSeeSky(pos.above())) {
                        return 0;
                    }
                    int r = level.isDay() ? day : day / 8;
                    return level.isRaining() ? Math.max(1, r / 2) : r;
                }
                case WATER_MILL -> {
                    int sides = 0;
                    for (Direction dir : Direction.values()) {
                        FluidState fluid = level.getFluidState(pos.relative(dir));
                        if (fluid.is(net.minecraft.tags.FluidTags.WATER)) {
                            sides++;
                        }
                    }
                    return sides; // 1 TU/t per lato bagnato (max 6)
                }
                case WIND_MILL -> {
                    if (pos.getY() < 64) {
                        return 0;
                    }
                    // il vento e' "bloccato" dai blocchi intorno (cubo 5x5x5)
                    int blocked = 0;
                    for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) {
                        if (!p.equals(pos) && !level.getBlockState(p).isAir()) {
                            blocked++;
                        }
                    }
                    int r = Math.min(8, (pos.getY() - 64) / 16 + 1);
                    r = Math.max(0, r - blocked / 4);
                    if (level.isThundering()) {
                        r *= 2;
                    } else if (level.isRaining()) {
                        r = r * 3 / 2;
                    }
                    return r;
                }
                default -> {
                    return 0;
                }
            }
        }
    }
}
