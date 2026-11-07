package com.futuretek.gametest;

import com.futuretek.FutureTek;
import com.futuretek.block.MachineBlock;
import com.futuretek.blockentity.QuarryBlockEntity;
import com.futuretek.logistics.AnalyzerConduitBlockEntity;
import com.futuretek.logistics.ItemNetwork;
import com.futuretek.logistics.ItemNetworkManager;
import com.futuretek.logistics.LogisticsCoreBlockEntity;
import com.futuretek.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Correzioni dopo i test in gioco: escavatore che sputa gli oggetti, telaio che sparisce, tubo analizzatore. */
@GameTestHolder(FutureTek.MODID)
@PrefixGameTestTemplate(false)
public class Phase10bGameTests {
    private static final String EMPTY = "empty";

    private static BlockPos placeQuarry(GameTestHelper helper) {
        BlockPos pos = new BlockPos(4, 3, 1);
        helper.setBlock(pos, ModBlocks.QUARRY.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        helper.setBlock(new BlockPos(5, 2, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 3, 1), ModBlocks.AREA_MARKER.get());
        helper.setBlock(new BlockPos(1, 2, 5), Blocks.STONE);
        helper.setBlock(new BlockPos(1, 3, 5), ModBlocks.AREA_MARKER.get());
        helper.setBlock(new BlockPos(3, 2, 3), Blocks.STONE);
        return pos;
    }

    /** Senza baule ne' tubi l'escavatore non si ferma: butta fuori gli oggetti dall'alto */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void quarryEjectsWithoutChest(GameTestHelper helper) {
        BlockPos pos = placeQuarry(helper);
        QuarryBlockEntity quarry = helper.getBlockEntity(pos);
        helper.onEachTick(() -> quarry.getEnergy().setEnergy(4_000)); // energia sempre piena
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(3, 2, 3));
            helper.assertEntityPresent(EntityType.ITEM);
            // telaio a gabbia: anello in basso e colonna d'angolo (l'anello alto e' fuori dalla zona di prova)
            helper.assertBlockPresent(ModBlocks.QUARRY_FRAME.get(), new BlockPos(1, 3, 1));
            helper.assertBlockPresent(ModBlocks.QUARRY_FRAME.get(), new BlockPos(1, 4, 1));
        });
    }

    /** Rompendo l'escavatore sparisce tutto il telaio */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void quarryFrameRemovedWithQuarry(GameTestHelper helper) {
        BlockPos pos = placeQuarry(helper);
        helper.setBlock(pos.above(), Blocks.CHEST);
        QuarryBlockEntity quarry = helper.getBlockEntity(pos);
        quarry.getEnergy().setEnergy(4_000);
        helper.onEachTick(() -> {
            if (helper.getLevel().getBlockEntity(helper.absolutePos(pos)) instanceof QuarryBlockEntity q
                    && q.getStatus() != QuarryBlockEntity.Status.FRAME) {
                q.getEnergy().setEnergy(4_000);
            }
        });
        helper.runAfterDelay(120, () -> {
            helper.assertBlockPresent(ModBlocks.QUARRY_FRAME.get(), new BlockPos(1, 3, 1));
            helper.destroyBlock(pos);
            helper.runAfterDelay(2, () -> {
                helper.assertBlockPresent(Blocks.AIR, new BlockPos(1, 3, 1));
                helper.assertBlockPresent(Blocks.AIR, new BlockPos(1, 4, 1));
                helper.succeed();
            });
        });
    }

    /** Tubo analizzatore: impara il baule, il nucleo ci manda gli oggetti, "Lascia 1" tiene l'ultimo */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void analyzerLearnsAndKeepsOne(GameTestHelper helper) {
        BlockPos core = new BlockPos(1, 1, 1);
        helper.setBlock(core, ModBlocks.LOGISTICS_CORE.get());
        helper.setBlock(core.north(), Blocks.CHEST);
        helper.setBlock(core.east(), ModBlocks.ITEM_CONDUIT.get());
        helper.setBlock(core.east(2), ModBlocks.ANALYZER_CONDUIT.get());
        BlockPos storePos = core.east(2).south();
        helper.setBlock(storePos, Blocks.CHEST);
        LogisticsCoreBlockEntity coreBe = helper.getBlockEntity(core);
        helper.onEachTick(() -> coreBe.getEnergy().setEnergy(10_000));
        ChestBlockEntity main = helper.getBlockEntity(core.north());
        ChestBlockEntity store = helper.getBlockEntity(storePos);
        store.setItem(0, new ItemStack(Items.COBBLESTONE, 1)); // oggetto di esempio
        main.setItem(0, new ItemStack(Items.COBBLESTONE, 10));
        main.setItem(1, new ItemStack(Items.DIRT, 3));
        AnalyzerConduitBlockEntity analyzer = helper.getBlockEntity(core.east(2));
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(analyzer.getMemory().contains(Items.COBBLESTONE), "L'analizzatore non ha imparato il pietrisco");
            helper.assertTrue(store.countItem(Items.COBBLESTONE) == 11, "Pietrisco nel baule: " + store.countItem(Items.COBBLESTONE));
            helper.assertTrue(store.countItem(Items.DIRT) == 0, "La terra non deve arrivare nel baule analizzato");
            ItemNetwork net = ItemNetworkManager.get(helper.getLevel(), helper.absolutePos(core.east()));
            helper.assertTrue(net.count(new ItemStack(Items.COBBLESTONE)) == 10, "Disponibili (l'ultimo e' prenotato): "
                    + net.count(new ItemStack(Items.COBBLESTONE)));
            ItemStack taken = net.extractExact(new ItemStack(Items.COBBLESTONE), 64, false);
            helper.assertTrue(taken.getCount() == 10 && store.countItem(Items.COBBLESTONE) == 1,
                    "Lascia 1: presi " + taken.getCount() + ", rimasti " + store.countItem(Items.COBBLESTONE));
            // anche svuotato, il baule resta la destinazione del pietrisco
            store.clearContent();
            helper.runAfterDelay(41, () -> {
                ItemStack rest = net.insert(new ItemStack(Items.COBBLESTONE, 4), null, false);
                helper.assertTrue(rest.isEmpty() && store.countItem(Items.COBBLESTONE) == 4,
                        "Il pietrisco non torna nel baule svuotato: " + store.countItem(Items.COBBLESTONE));
                helper.succeed();
            });
        });
    }
}
