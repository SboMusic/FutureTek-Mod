package com.futuretek.gametest;

import com.futuretek.FutureTek;
import com.futuretek.block.MachineBlock;
import com.futuretek.blockentity.QuarryBlockEntity;
import com.futuretek.blockentity.ReactorBlockEntity;
import com.futuretek.item.EnergyItem;
import com.futuretek.item.ReactorComponentItem;
import com.futuretek.registry.ModBlocks;
import com.futuretek.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Test automatici della tecnologia avanzata. */
@GameTestHolder(FutureTek.MODID)
@PrefixGameTestTemplate(false)
public class AdvancedGameTests {
    private static final String EMPTY = "empty";

    private static ItemStack part(ReactorComponentItem.Type type) {
        return new ItemStack(ModItems.REACTOR_PARTS.get(type).get());
    }

    /** Reattore acceso con una barra e due dissipatori: produce energia e non si surriscalda */
    @GameTest(template = EMPTY, timeoutTicks = 120)
    public static void reactorProducesEnergySafely(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.REACTOR.get());
        helper.setBlock(pos.above(), Blocks.REDSTONE_BLOCK);
        ReactorBlockEntity reactor = helper.getBlockEntity(pos);
        reactor.getItems().setStackInSlot(10, part(ReactorComponentItem.Type.FUEL_ROD));
        reactor.getItems().setStackInSlot(9, part(ReactorComponentItem.Type.HEAT_VENT));
        reactor.getItems().setStackInSlot(11, part(ReactorComponentItem.Type.HEAT_VENT));
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(reactor.getEnergy().getEnergyStored() > 0, "Il reattore non produce energia");
            helper.assertTrue(reactor.getHeat() == 0, "Lo scafo si scalda nonostante i dissipatori: " + reactor.getHeat());
            helper.succeed();
        });
    }

    /** Senza segnale redstone il reattore resta spento */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void reactorNeedsRedstone(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.REACTOR.get());
        ReactorBlockEntity reactor = helper.getBlockEntity(pos);
        reactor.getItems().setStackInSlot(0, part(ReactorComponentItem.Type.QUAD_FUEL_ROD));
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(reactor.getEnergy().getEnergyStored() == 0, "Il reattore funziona senza redstone");
            helper.succeed();
        });
    }

    /** Surriscaldamento (esplosioni disattivate): spegnimento d'emergenza, barre distrutte, nessuna esplosione */
    @GameTest(template = EMPTY, timeoutTicks = 60, batch = "reattore_spento")
    public static void reactorScramsOnOverheat(GameTestHelper helper) {
        com.futuretek.FutureTekConfig.REACTOR_EXPLOSIONS.set(false); // in questa prova le esplosioni sono spente
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.REACTOR.get());
        helper.setBlock(pos.above(), Blocks.REDSTONE_BLOCK);
        ReactorBlockEntity reactor = helper.getBlockEntity(pos);
        for (int slot : new int[] {0, 1, 9, 10}) {
            reactor.getItems().setStackInSlot(slot, part(ReactorComponentItem.Type.QUAD_FUEL_ROD));
        }
        reactor.setHeat(9_900);
        helper.runAfterDelay(25, () -> {
            helper.assertBlockPresent(ModBlocks.REACTOR.get(), pos);
            helper.assertTrue(reactor.getItems().getStackInSlot(0).isEmpty(), "Le barre non sono state distrutte");
            com.futuretek.FutureTekConfig.REACTOR_EXPLOSIONS.set(true);
            helper.succeed();
        });
    }

    /** Escavatore con i segnalini: costruisce il telaio, scava la pietra nell'area e la mette nel baule accanto */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void quarryMinesIntoChest(GameTestHelper helper) {
        BlockPos pos = new BlockPos(4, 3, 1);
        helper.setBlock(pos, ModBlocks.QUARRY.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        helper.setBlock(pos.above(), Blocks.CHEST);
        helper.setBlock(new BlockPos(5, 2, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 3, 1), ModBlocks.AREA_MARKER.get());
        helper.setBlock(new BlockPos(1, 2, 5), Blocks.STONE);
        helper.setBlock(new BlockPos(1, 3, 5), ModBlocks.AREA_MARKER.get());
        ChestBlockEntity chest = helper.getBlockEntity(pos.above());
        helper.setBlock(new BlockPos(3, 2, 3), Blocks.STONE);
        QuarryBlockEntity quarry = helper.getBlockEntity(pos);
        quarry.getEnergy().setEnergy(50_000);
        helper.succeedWhen(() -> {
            helper.assertTrue(quarry.width() == 3 && quarry.length() == 3, "Area dei segnalini errata: "
                    + quarry.width() + "x" + quarry.length());
            helper.assertBlockPresent(ModBlocks.QUARRY_FRAME.get(), new BlockPos(1, 3, 1));
            helper.assertTrue(chest.countItem(Blocks.COBBLESTONE.asItem()) >= 1, "Il pietrisco non e' nel baule");
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(3, 2, 3));
        });
    }

    /** L'esoarmatura si ricarica come una cella (capability energia) */
    @GameTest(template = EMPTY)
    public static void exoArmorHasEnergy(GameTestHelper helper) {
        ItemStack helmet = new ItemStack(ModItems.EXO_HELMET.get());
        var storage = helmet.getCapability(Capabilities.EnergyStorage.ITEM);
        helper.assertTrue(storage != null, "L'elmo non ha la capability energia");
        storage.receiveEnergy(1000, false);
        helper.assertTrue(EnergyItem.getEnergy(helmet) == 512, "Ricarica errata (limite 512/t): " + EnergyItem.getEnergy(helmet));
        helper.succeed();
    }
}
