package com.futuretek.gametest;

import com.futuretek.FutureTek;
import com.futuretek.block.ConduitBlock;
import com.futuretek.blockentity.SortingConduitBlockEntity;
import com.futuretek.logistics.LogisticsCoreBlockEntity;
import com.futuretek.machine.ProcessingMachine;
import com.futuretek.mining.MiningMole;
import com.futuretek.power.CharcoalKiln;
import com.futuretek.power.Generators;
import com.futuretek.registry.ModBlocks;
import com.futuretek.storage.Storage;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Test automatici della Fase 10: bauli, generatori, carbonaia, nucleo logistico, talpa mineraria. */
@GameTestHolder(FutureTek.MODID)
@PrefixGameTestTemplate(false)
public class Phase10GameTests {
    private static final String EMPTY = "empty";

    /** I bauli hanno la grandezza giusta e "Ordina" raggruppa le pile */
    @GameTest(template = EMPTY)
    public static void chestsSizeAndSort(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.CHESTS.get(Storage.Tier.DIAMOND).get());
        Storage.ChestEntity chest = helper.getBlockEntity(pos);
        helper.assertTrue(chest.getItems().getSlots() == 108, "Il baule di diamante deve avere 108 slot");
        chest.getItems().setStackInSlot(5, new ItemStack(Items.COBBLESTONE, 10));
        chest.getItems().setStackInSlot(70, new ItemStack(Items.COBBLESTONE, 20));
        chest.getItems().setStackInSlot(30, new ItemStack(Items.APPLE, 3));
        Storage.ChestMenu.sort(chest.getItems());
        helper.assertTrue(chest.getItems().getStackInSlot(0).is(Items.APPLE), "Dopo l'ordinamento la mela va per prima");
        helper.assertTrue(chest.getItems().getStackInSlot(1).is(Items.COBBLESTONE)
                && chest.getItems().getStackInSlot(1).getCount() == 30, "Il pietrisco doveva essere raggruppato in 30");
        helper.assertTrue(chest.getItems().getStackInSlot(2).isEmpty(), "Dopo l'ordinamento il resto e' vuoto");
        helper.succeed();
    }

    /** Mulino ad acqua: 2 TU/t per lato bagnato; mulino a vento: niente sotto y 64 */
    @GameTest(template = EMPTY)
    public static void renewableGenerators(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, ModBlocks.GENERATORS.get(Generators.Kind.WATER_MILL).get());
        helper.setBlock(pos.east(), Blocks.WATER);
        helper.setBlock(pos.west(), Blocks.WATER);
        int water = Generators.Passive.computeRate(helper.getLevel(), helper.absolutePos(pos), Generators.Kind.WATER_MILL);
        helper.assertTrue(water == 2, "Mulino ad acqua con 2 lati d'acqua: atteso 2, trovato " + water);
        int wind = Generators.Passive.computeRate(helper.getLevel(), helper.absolutePos(pos).atY(40), Generators.Kind.WIND_MILL);
        helper.assertTrue(wind == 0, "Mulino a vento a y 40 non deve produrre: " + wind);
        helper.assertTrue(ProcessingMachine.ADVANCED_CRUSHER.time * 4 == ProcessingMachine.CRUSHER.time,
                "Il frantumatore avanzato deve essere 4 volte piu' veloce");
        helper.succeed();
    }

    /** Carbonaia: 16 tronchi diventano 16 carbonelle in un ciclo */
    @GameTest(template = EMPTY, timeoutTicks = CharcoalKiln.CYCLE + 40)
    public static void kilnMakesCharcoal(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.CHARCOAL_KILN.get());
        CharcoalKiln.KilnEntity kiln = helper.getBlockEntity(pos);
        kiln.getItems().setStackInSlot(0, new ItemStack(Items.OAK_LOG, 20));
        helper.succeedWhen(() -> {
            int charcoal = 0;
            for (int i = CharcoalKiln.SLOTS; i < CharcoalKiln.SLOTS * 2; i++) {
                charcoal += kiln.getItems().getStackInSlot(i).getCount();
            }
            helper.assertTrue(charcoal == 16, "Carbonella prodotta: " + charcoal);
        });
    }

    /** Nucleo logistico: il diamante messo nel baule principale va da solo nel baule del tubo di destinazione */
    @GameTest(template = EMPTY, timeoutTicks = 80)
    public static void logisticsCoreRoutesFromMainChest(GameTestHelper helper) {
        BlockPos core = new BlockPos(1, 1, 1);
        helper.setBlock(core, ModBlocks.LOGISTICS_CORE.get());
        helper.setBlock(core.north(), Blocks.CHEST);                       // baule principale (tocca il nucleo)
        helper.setBlock(core.east(), ModBlocks.ITEM_CONDUIT.get());
        helper.setBlock(core.east(2), ModBlocks.ITEM_CONDUIT.get());
        helper.setBlock(core.east(3), ModBlocks.SORTING_CONDUIT.get());
        helper.setBlock(core.east(3).south(), Blocks.CHEST);               // destinazione dei diamanti
        LogisticsCoreBlockEntity coreBe = helper.getBlockEntity(core);
        coreBe.getEnergy().setEnergy(10_000);
        SortingConduitBlockEntity sorter = helper.getBlockEntity(core.east(3));
        sorter.getFilter().setStackInSlot(0, new ItemStack(Items.DIAMOND));
        ChestBlockEntity main = helper.getBlockEntity(core.north());
        main.setItem(0, new ItemStack(Items.DIAMOND, 5));
        main.setItem(1, new ItemStack(Items.DIRT, 3));
        ChestBlockEntity target = helper.getBlockEntity(core.east(3).south());
        helper.succeedWhen(() -> {
            helper.assertTrue(target.countItem(Items.DIAMOND) == 5, "Diamanti arrivati: " + target.countItem(Items.DIAMOND));
            helper.assertTrue(main.countItem(Items.DIRT) == 3, "La terra (senza destinazione) deve restare nel baule principale");
        });
    }

    /** Talpa mineraria: parte accanto al baule e comincia a scavare (consumando carburante) */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void moleDigs(GameTestHelper helper) {
        BlockPos chestPos = new BlockPos(2, 3, 3);
        BlockPos molePos = new BlockPos(3, 3, 3);
        for (BlockPos p : BlockPos.betweenClosed(new BlockPos(1, 1, 1), new BlockPos(5, 2, 5))) {
            helper.setBlock(p, Blocks.STONE);
        }
        helper.setBlock(chestPos, Blocks.CHEST);
        helper.setBlock(molePos, ModBlocks.MINING_MOLE.get());
        MiningMole.MoleEntity mole = helper.getBlockEntity(molePos);
        mole.getItems().setStackInSlot(MiningMole.FUEL_SLOT, new ItemStack(Items.COAL, 2));
        mole.setRadius(1);
        mole.toggle();
        helper.assertTrue(mole.getState() == MiningMole.State.MINING, "La talpa non e' partita: " + mole.getState());
        helper.succeedWhen(() -> {
            // la talpa si e' spostata: cerchiamola sotto la posizione iniziale
            MiningMole.MoleEntity moved = null;
            for (BlockPos p : BlockPos.betweenClosed(helper.absolutePos(molePos).offset(-2, -4, -2),
                    helper.absolutePos(molePos).offset(2, 0, 2))) {
                if (helper.getLevel().getBlockEntity(p) instanceof MiningMole.MoleEntity m) {
                    moved = m;
                }
            }
            helper.assertTrue(moved != null && moved.getMined() >= 3, "La talpa non ha scavato abbastanza: "
                    + (moved == null ? "non trovata" : moved.getState() + " scavati " + moved.getMined() + " a " + moved.getBlockPos()));
            // l'area va dalla parte opposta al baule: sotto il baule la pietra resta
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(2, 2, 3));
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(1, 2, 3));
            moved.toggle(); // fermala, cosi' non continua a scavare dopo il test
        });
    }

    /** Il tubo smistatore e' ancora un condotto della rete (compatibilita' con i mondi vecchi) */
    @GameTest(template = EMPTY)
    public static void sortingPipeIsConduit(GameTestHelper helper) {
        helper.assertTrue(ModBlocks.SORTING_CONDUIT.get() instanceof ConduitBlock, "Il tubo di destinazione deve essere un condotto");
        helper.succeed();
    }
}
