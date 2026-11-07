package com.futuretek.gametest;

import com.futuretek.FutureTek;
import com.futuretek.block.MachineBlock;
import com.futuretek.blockentity.BatteryBoxBlockEntity;
import com.futuretek.blockentity.CombustionGeneratorBlockEntity;
import com.futuretek.blockentity.GeothermalGeneratorBlockEntity;
import com.futuretek.item.BatteryItem;
import com.futuretek.registry.ModBlocks;
import com.futuretek.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Test automatici dell'energia. Si eseguono sul server di prova con il comando
 * "/test runall" (o automaticamente con il server di test). Non servono per giocare.
 */
@GameTestHolder(FutureTek.MODID)
@PrefixGameTestTemplate(false)
public class EnergyGameTests {
    private static final String EMPTY = "empty";

    private static Block cable(String name) {
        return ModBlocks.CABLES.get(name).get();
    }

    /** Un generatore con carbone produce energia */
    @GameTest(template = EMPTY)
    public static void generatorBurnsCoal(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.COMBUSTION_GENERATOR.get());
        CombustionGeneratorBlockEntity generator = helper.getBlockEntity(pos);
        generator.getItems().insertItem(0, new ItemStack(Items.COAL), false);
        helper.succeedWhen(() -> helper.assertTrue(generator.getEnergy().getEnergyStored() > 0,
                "Il generatore non produce energia"));
    }

    /** Generatore -> 3 cavi di stagno -> banco accumulatore */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void cableCarriesEnergy(GameTestHelper helper) {
        helper.setBlock(new BlockPos(0, 1, 1), ModBlocks.COMBUSTION_GENERATOR.get());
        for (int x = 1; x <= 3; x++) {
            helper.setBlock(new BlockPos(x, 1, 1), cable("tin_cable"));
        }
        helper.setBlock(new BlockPos(4, 1, 1),
                ModBlocks.BATTERY_BOX.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        CombustionGeneratorBlockEntity generator = helper.getBlockEntity(new BlockPos(0, 1, 1));
        generator.getItems().insertItem(0, new ItemStack(Items.COAL), false);
        BatteryBoxBlockEntity battery = helper.getBlockEntity(new BlockPos(4, 1, 1));
        helper.succeedWhen(() -> helper.assertTrue(battery.getEnergy().getEnergyStored() >= 500,
                "L'energia non arriva al banco attraverso i cavi (" + battery.getEnergy().getEnergyStored() + ")"));
    }

    /** Il banco rilascia energia SOLO dal lato frontale: banco A (fronte a est) -> cavo -> banco B */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void batteryOutputsFromFront(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1),
                ModBlocks.BATTERY_BOX.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        helper.setBlock(new BlockPos(2, 1, 1), cable("insulated_copper_cable"));
        helper.setBlock(new BlockPos(3, 1, 1),
                ModBlocks.BATTERY_BOX.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        BatteryBoxBlockEntity source = helper.getBlockEntity(new BlockPos(1, 1, 1));
        BatteryBoxBlockEntity target = helper.getBlockEntity(new BlockPos(3, 1, 1));
        source.getEnergy().setEnergy(20_000);
        helper.succeedWhen(() -> helper.assertTrue(target.getEnergy().getEnergyStored() >= 1_000,
                "Il banco B non riceve energia dal banco A"));
    }

    /** Il generatore geotermico produce energia consumando lava */
    @GameTest(template = EMPTY)
    public static void geothermalUsesLava(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.GEOTHERMAL_GENERATOR.get());
        GeothermalGeneratorBlockEntity generator = helper.getBlockEntity(pos);
        int filled = generator.getTank().fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(filled == 1000, "Il serbatoio non accetta la lava");
        int refused = generator.getTank().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(refused == 0, "Il serbatoio accetta l'acqua (dovrebbe accettare solo lava)");
        helper.succeedWhen(() -> {
            helper.assertTrue(generator.getEnergy().getEnergyStored() > 0, "Nessuna energia dalla lava");
            helper.assertTrue(generator.getTank().getFluidAmount() < 1000, "La lava non viene consumata");
        });
    }

    /** Il banco ricarica una cella portatile messa nello slot in alto */
    @GameTest(template = EMPTY)
    public static void batteryChargesCell(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.BATTERY_BOX.get());
        BatteryBoxBlockEntity battery = helper.getBlockEntity(pos);
        battery.getEnergy().setEnergy(5_000);
        battery.getItems().insertItem(BatteryBoxBlockEntity.SLOT_CHARGE, new ItemStack(ModItems.BATTERY_CELL.get()), false);
        helper.succeedWhen(() -> helper.assertTrue(
                BatteryItem.getEnergy(battery.getItems().getStackInSlot(BatteryBoxBlockEntity.SLOT_CHARGE)) > 0,
                "La cella non si ricarica"));
    }

    /** Una rete di cavi di stagno non trasporta piu' di 32 TU al tick */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void tinCableLimitsThroughput(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1),
                ModBlocks.BATTERY_BOX_2.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        helper.setBlock(new BlockPos(2, 1, 1), cable("tin_cable"));
        helper.setBlock(new BlockPos(3, 1, 1),
                ModBlocks.BATTERY_BOX_2.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        BatteryBoxBlockEntity source = helper.getBlockEntity(new BlockPos(1, 1, 1));
        BatteryBoxBlockEntity target = helper.getBlockEntity(new BlockPos(3, 1, 1));
        source.getEnergy().setEnergy(100_000);
        helper.runAfterDelay(40, () -> {
            int received = target.getEnergy().getEnergyStored();
            helper.assertTrue(received > 0, "Nessuna energia passata");
            helper.assertTrue(received <= 41 * 32, "Il cavo di stagno ha superato 32 TU/tick: " + received);
            helper.succeed();
        });
    }
}
