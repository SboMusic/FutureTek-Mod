package com.futuretek.gametest;

import com.futuretek.FutureTek;
import com.futuretek.blockentity.AutoCrafterBlockEntity;
import com.futuretek.blockentity.SortingConduitBlockEntity;
import com.futuretek.block.LogicGateBlock;
import com.futuretek.logistics.CraftingPlanner;
import com.futuretek.logistics.ItemNetwork;
import com.futuretek.logistics.ItemNetworkManager;
import com.futuretek.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Test automatici della logistica. */
@GameTestHolder(FutureTek.MODID)
@PrefixGameTestTemplate(false)
public class LogisticsGameTests {
    private static final String EMPTY = "empty";

    private static ChestBlockEntity chest(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.CHEST);
        return helper.getBlockEntity(pos);
    }

    /** Baule A -> condotto estrattore -> 2 condotti -> baule B */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void extractorMovesItems(GameTestHelper helper) {
        ChestBlockEntity a = chest(helper, new BlockPos(0, 1, 1));
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.EXTRACTOR_CONDUIT.get());
        helper.setBlock(new BlockPos(2, 1, 1), ModBlocks.ITEM_CONDUIT.get());
        helper.setBlock(new BlockPos(3, 1, 1), ModBlocks.ITEM_CONDUIT.get());
        ChestBlockEntity b = chest(helper, new BlockPos(4, 1, 1));
        a.setItem(0, new ItemStack(Items.COBBLESTONE, 16));
        helper.succeedWhen(() -> {
            helper.assertTrue(b.countItem(Items.COBBLESTONE) == 16, "Il pietrisco non e' arrivato nel baule B");
            helper.assertTrue(a.countItem(Items.COBBLESTONE) == 0, "Il baule A non si e' svuotato");
        });
    }

    /** Smistatore: il ferro va nel baule col filtro "ferro", il resto nel baule normale */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void sortingConduitFilters(GameTestHelper helper) {
        ChestBlockEntity source = chest(helper, new BlockPos(0, 1, 1));
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.EXTRACTOR_CONDUIT.get());
        helper.setBlock(new BlockPos(2, 1, 1), ModBlocks.ITEM_CONDUIT.get());
        helper.setBlock(new BlockPos(3, 1, 1), ModBlocks.SORTING_CONDUIT.get());
        ChestBlockEntity ironChest = chest(helper, new BlockPos(4, 1, 1));
        ChestBlockEntity otherChest = chest(helper, new BlockPos(2, 1, 2));
        SortingConduitBlockEntity sorter = helper.getBlockEntity(new BlockPos(3, 1, 1));
        sorter.getFilter().setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
        source.setItem(0, new ItemStack(Items.IRON_INGOT, 5));
        source.setItem(1, new ItemStack(Items.DIRT, 5));
        helper.succeedWhen(() -> {
            helper.assertTrue(ironChest.countItem(Items.IRON_INGOT) == 5, "Il ferro non e' nel baule filtrato");
            helper.assertTrue(ironChest.countItem(Items.DIRT) == 0, "La terra e' finita nel baule del ferro");
            helper.assertTrue(otherChest.countItem(Items.DIRT) == 5, "La terra non e' nel baule normale");
        });
    }

    /** Smistamento automatico: un oggetto va nel baule che lo contiene gia' */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void autoSortToMatchingChest(GameTestHelper helper) {
        ChestBlockEntity source = chest(helper, new BlockPos(0, 1, 1));
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.EXTRACTOR_CONDUIT.get());
        helper.setBlock(new BlockPos(2, 1, 1), ModBlocks.ITEM_CONDUIT.get());
        ChestBlockEntity empty = chest(helper, new BlockPos(2, 1, 0));
        ChestBlockEntity sand = chest(helper, new BlockPos(2, 1, 2));
        sand.setItem(5, new ItemStack(Items.SAND, 1));
        source.setItem(0, new ItemStack(Items.SAND, 8));
        helper.succeedWhen(() -> {
            helper.assertTrue(sand.countItem(Items.SAND) == 9, "La sabbia non e' andata nel baule con la sabbia");
            helper.assertTrue(empty.countItem(Items.SAND) == 0, "La sabbia e' finita nel baule vuoto");
        });
    }

    /** Crafting automatico a catena: chiedo 4 bastoncini avendo solo tronchi (tronco -> assi -> bastoncini) */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void autoCraftingChain(GameTestHelper helper) {
        ChestBlockEntity storage = chest(helper, new BlockPos(0, 1, 1));
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.ITEM_CONDUIT.get());
        helper.setBlock(new BlockPos(2, 1, 1), ModBlocks.ITEM_CONDUIT.get());
        helper.setBlock(new BlockPos(1, 1, 2), ModBlocks.AUTO_CRAFTER.get());
        helper.setBlock(new BlockPos(2, 1, 2), ModBlocks.AUTO_CRAFTER.get());
        AutoCrafterBlockEntity planks = helper.getBlockEntity(new BlockPos(1, 1, 2));
        AutoCrafterBlockEntity sticks = helper.getBlockEntity(new BlockPos(2, 1, 2));
        planks.getPattern().setStackInSlot(0, new ItemStack(Items.OAK_LOG));
        sticks.getPattern().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS));
        sticks.getPattern().setStackInSlot(3, new ItemStack(Items.OAK_PLANKS));
        storage.setItem(0, new ItemStack(Items.OAK_LOG, 1));
        ItemNetwork network = ItemNetworkManager.get(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)));
        helper.assertTrue(network != null, "Rete non trovata");
        CraftingPlanner.Plan plan = CraftingPlanner.request(network, new ItemStack(Items.STICK), 4);
        helper.assertTrue(plan.ok(), "La richiesta non e' stata pianificata: mancano " + plan.missing());
        helper.succeedWhen(() -> helper.assertTrue(storage.countItem(Items.STICK) >= 4,
                "I bastoncini non sono stati fabbricati"));
    }

    /** Una richiesta impossibile segnala cosa manca */
    @GameTest(template = EMPTY)
    public static void autoCraftingReportsMissing(GameTestHelper helper) {
        chest(helper, new BlockPos(0, 1, 1));
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.ITEM_CONDUIT.get());
        helper.setBlock(new BlockPos(1, 1, 2), ModBlocks.AUTO_CRAFTER.get());
        AutoCrafterBlockEntity planks = helper.getBlockEntity(new BlockPos(1, 1, 2));
        planks.getPattern().setStackInSlot(0, new ItemStack(Items.OAK_LOG));
        ItemNetwork network = ItemNetworkManager.get(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)));
        CraftingPlanner.Plan plan = CraftingPlanner.request(network, new ItemStack(Items.OAK_PLANKS), 4);
        helper.assertTrue(!plan.ok() && !plan.missing().isEmpty(), "Doveva segnalare i tronchi mancanti");
        helper.assertTrue(planks.getQueued() == 0, "Non deve mettere in coda lavori impossibili");
        helper.succeed();
    }

    /** Porta AND: si accende solo con entrambi gli ingressi */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void andGateWorks(GameTestHelper helper) {
        BlockPos gate = new BlockPos(2, 1, 2);
        helper.setBlock(gate.below(), Blocks.STONE);
        helper.setBlock(gate, ModBlocks.GATES.get(LogicGateBlock.Type.AND).get());
        helper.setBlock(gate.west(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(!helper.getBlockState(gate).getValue(LogicGateBlock.POWERED), "AND acceso con un solo ingresso");
            helper.setBlock(gate.east(), Blocks.REDSTONE_BLOCK);
        });
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(helper.getBlockState(gate).getValue(LogicGateBlock.POWERED), "AND spento con due ingressi");
            int signal = helper.getLevel().getSignal(helper.absolutePos(gate), Direction.SOUTH);
            helper.assertTrue(signal == 15, "Uscita della porta errata: " + signal);
            helper.succeed();
        });
    }
}
