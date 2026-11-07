package com.futuretek.farm;

import java.util.List;

import com.futuretek.blockentity.MachineBlockEntity;
import com.futuretek.energy.EnergyTier;
import com.futuretek.logistics.ItemOutput;
import com.futuretek.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Macchine agricole (Fase 8). */
public final class FarmBlockEntities {
    private FarmBlockEntities() {
    }

    /**
     * Acceleratore di crescita: ogni secondo fa "crescere" 16 piante a caso in un'area 9x9
     * (da 2 blocchi sotto a 1 sopra la macchina). Costa 40 TU al secondo.
     */
    public static class GrowthAccelerator extends MachineBlockEntity {
        public GrowthAccelerator(BlockPos pos, BlockState state) {
            super(ModBlockEntities.GROWTH_ACCELERATOR.get(), pos, state, 20_000, EnergyTier.LOW.rate, 0);
        }

        @Override
        public void serverTick() {
            boolean working = false;
            if (level.getGameTime() % 20 == 0 && energy.getEnergyStored() >= 40) {
                ServerLevel server = (ServerLevel) level;
                for (int i = 0; i < 16; i++) {
                    BlockPos target = worldPosition.offset(level.random.nextInt(9) - 4, level.random.nextInt(4) - 2,
                            level.random.nextInt(9) - 4);
                    BlockState state = level.getBlockState(target);
                    if (state.getBlock() instanceof BonemealableBlock || state.isRandomlyTicking()) {
                        if (state.isRandomlyTicking() && !state.is(net.minecraft.tags.BlockTags.LEAVES)) {
                            state.randomTick(server, target, level.random);
                            working = true;
                        }
                    }
                }
                energy.consume(40);
                setLit(working);
            }
        }
    }

    /**
     * Raccoglitore automatico: ogni 2 secondi raccoglie le colture mature in un'area 9x9 allo stesso livello
     * (grano, carote, patate, barbabietole...: le ripianta da solo), zucche, angurie, canna e cactus.
     * Mette il raccolto nei contenitori accanto o nella rete. 50 TU per pianta raccolta.
     */
    public static class Harvester extends MachineBlockEntity {
        public Harvester(BlockPos pos, BlockState state) {
            super(ModBlockEntities.HARVESTER.get(), pos, state, 20_000, EnergyTier.LOW.rate, 0);
        }

        @Override
        public void serverTick() {
            if (level.getGameTime() % 40 != 0) {
                return;
            }
            boolean working = false;
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (energy.getEnergyStored() < 50) {
                            setLit(working);
                            return;
                        }
                        BlockPos target = worldPosition.offset(dx, dy, dz);
                        if (harvest(target)) {
                            energy.consume(50);
                            working = true;
                        }
                    }
                }
            }
            setLit(working);
        }

        /** Raccoglie una pianta se e' pronta. */
        public boolean harvest(BlockPos target) {
            BlockState state = level.getBlockState(target);
            Block block = state.getBlock();
            boolean replant = false;
            if (block instanceof CropBlock crop) {
                if (!crop.isMaxAge(state)) {
                    return false;
                }
                replant = true;
            } else if (block instanceof SugarCaneBlock || block instanceof CactusBlock) {
                if (!level.getBlockState(target.below()).is(block)) {
                    return false; // lascia la base per farla ricrescere
                }
            } else if (!(state.is(net.minecraft.world.level.block.Blocks.PUMPKIN)
                    || state.is(net.minecraft.world.level.block.Blocks.MELON))) {
                return false;
            }
            List<ItemStack> drops = Block.getDrops(state, (ServerLevel) level, target, null);
            if (replant) {
                ItemStack seed = block.getCloneItemStack(level, target, state);
                for (ItemStack drop : drops) {
                    if (ItemStack.isSameItem(drop, seed)) {
                        drop.shrink(1); // un seme torna nel terreno
                        break;
                    }
                }
            }
            for (ItemStack drop : drops) {
                if (!drop.isEmpty() && !ItemOutput.insert(level, worldPosition, drop, true).isEmpty()) {
                    return false; // niente spazio: non raccoglie
                }
            }
            drops.forEach(drop -> ItemOutput.insert(level, worldPosition, drop, false));
            if (replant) {
                level.setBlock(target, ((CropBlock) block).getStateForAge(0), Block.UPDATE_ALL);
            } else {
                level.destroyBlock(target, false);
            }
            return true;
        }
    }
}
