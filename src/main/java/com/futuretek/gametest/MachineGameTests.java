package com.futuretek.gametest;

import com.futuretek.FutureTek;
import com.futuretek.blockentity.ProcessingMachineBlockEntity;
import com.futuretek.item.UpgradeItem;
import com.futuretek.machine.ProcessingMachine;
import com.futuretek.registry.Metal;
import com.futuretek.registry.ModBlocks;
import com.futuretek.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Test automatici delle macchine di lavorazione. */
@GameTestHolder(FutureTek.MODID)
@PrefixGameTestTemplate(false)
public class MachineGameTests {
    private static final String EMPTY = "empty";
    private static final BlockPos POS = new BlockPos(1, 1, 1);

    /** Piazza una macchina carica di energia con un oggetto in ingresso */
    private static ProcessingMachineBlockEntity machine(GameTestHelper helper, ProcessingMachine type, ItemStack input) {
        helper.setBlock(POS, ModBlocks.PROCESSING.get(type).get());
        ProcessingMachineBlockEntity be = helper.getBlockEntity(POS);
        be.getEnergy().setEnergy(10_000);
        be.getItems().setStackInSlot(ProcessingMachineBlockEntity.SLOT_INPUT, input);
        return be;
    }

    private static void expectOutput(GameTestHelper helper, ProcessingMachineBlockEntity be, Item item, int min) {
        helper.succeedWhen(() -> {
            ItemStack out = be.getItems().getStackInSlot(ProcessingMachineBlockEntity.SLOT_OUTPUT);
            helper.assertTrue(out.is(item) && out.getCount() >= min,
                    "Atteso almeno " + min + " x " + item + ", trovato " + out);
        });
    }

    /** Frantumatore: 1 stagno grezzo -> 2 polveri di stagno (raddoppio) */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void crusherDoublesOre(GameTestHelper helper) {
        var be = machine(helper, ProcessingMachine.CRUSHER, new ItemStack(ModItems.RAW.get(Metal.TIN).get()));
        expectOutput(helper, be, ModItems.DUST.get(Metal.TIN).get(), 2);
    }

    /** Forno elettrico: usa le ricette della fornace (ferro grezzo -> lingotto) */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void electricFurnaceSmelts(GameTestHelper helper) {
        var be = machine(helper, ProcessingMachine.ELECTRIC_FURNACE, new ItemStack(Items.RAW_IRON));
        expectOutput(helper, be, Items.IRON_INGOT, 1);
    }

    /** Estrattore: 1 resina -> 3 gomma */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void extractorMakesRubber(GameTestHelper helper) {
        var be = machine(helper, ProcessingMachine.EXTRACTOR, new ItemStack(ModItems.STICKY_RESIN.get()));
        expectOutput(helper, be, ModItems.RUBBER.get(), 3);
    }

    /** Pressa per fili: 1 piastra di stagno -> 3 fili */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void wirePressMakesWire(GameTestHelper helper) {
        var be = machine(helper, ProcessingMachine.WIRE_PRESS, new ItemStack(ModItems.PLATE.get(Metal.TIN).get()));
        expectOutput(helper, be, ModItems.WIRE.get("tin").get(), 3);
    }

    /** Senza energia la macchina non lavora */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void noEnergyNoWork(GameTestHelper helper) {
        var be = machine(helper, ProcessingMachine.ELECTRIC_FURNACE, new ItemStack(Items.RAW_IRON));
        be.getEnergy().setEnergy(0);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(be.getProgress() == 0, "La macchina lavora senza energia");
            helper.succeed();
        });
    }

    /** 4 overclock: il frantumatore (300 tick) deve finire in circa 100 tick */
    @GameTest(template = EMPTY, timeoutTicks = 130)
    public static void overclockIsFaster(GameTestHelper helper) {
        var be = machine(helper, ProcessingMachine.CRUSHER, new ItemStack(ModItems.RAW.get(Metal.TIN).get()));
        be.getItems().setStackInSlot(ProcessingMachineBlockEntity.FIRST_UPGRADE,
                new ItemStack(ModItems.UPGRADES.get(UpgradeItem.Type.OVERCLOCK).get(), 4));
        expectOutput(helper, be, ModItems.DUST.get(Metal.TIN).get(), 2);
    }

    /** Il potenziamento di espulsione sposta i prodotti in un baule vicino */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void ejectorFillsChest(GameTestHelper helper) {
        var be = machine(helper, ProcessingMachine.EXTRACTOR, new ItemStack(ModItems.STICKY_RESIN.get()));
        be.getItems().setStackInSlot(ProcessingMachineBlockEntity.FIRST_UPGRADE,
                new ItemStack(ModItems.UPGRADES.get(UpgradeItem.Type.EJECTOR).get()));
        BlockPos chestPos = POS.above();
        helper.setBlock(chestPos, Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(chestPos);
        helper.succeedWhen(() -> helper.assertTrue(chest.countItem(ModItems.RUBBER.get()) >= 3,
                "La gomma non e' arrivata nel baule"));
    }

    /** Bagno chimico: senza acqua non lavora; con l'acqua 1 grezzo -> 3 frantumati puri */
    @GameTest(template = EMPTY, timeoutTicks = 500)
    public static void chemicalBathTriples(GameTestHelper helper) {
        var be = machine(helper, ProcessingMachine.CHEMICAL_BATH, new ItemStack(ModItems.RAW.get(Metal.TIN).get()));
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(be.getProgress() == 0, "Il bagno lavora senza acqua");
            be.getTank().fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000),
                    net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        });
        helper.succeedWhen(() -> {
            ItemStack out = be.getItems().getStackInSlot(ProcessingMachineBlockEntity.SLOT_OUTPUT);
            helper.assertTrue(out.is(ModItems.PURIFIED.get("tin").get()) && out.getCount() == 3, "Attesi 3 frantumati puri: " + out);
            helper.assertTrue(be.getTank().getFluidAmount() == 750, "Acqua non consumata correttamente");
        });
    }

    /** Centrifuga: 1 frantumato puro -> 1 polvere */
    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void centrifugeMakesDust(GameTestHelper helper) {
        var be = machine(helper, ProcessingMachine.THERMAL_CENTRIFUGE, new ItemStack(ModItems.PURIFIED.get("iron").get()));
        expectOutput(helper, be, ModItems.VANILLA_DUST.get("iron").get(), 1);
    }
}
