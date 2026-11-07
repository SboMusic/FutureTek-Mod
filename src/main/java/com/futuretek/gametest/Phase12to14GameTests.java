package com.futuretek.gametest;

import com.futuretek.FutureTek;
import com.futuretek.automation.HomeMachines;
import com.futuretek.automation.NetherMachines;
import com.futuretek.blockentity.MachineBlockEntity;
import com.futuretek.registry.ModBlocks;
import com.futuretek.registry.ModEntities;
import com.futuretek.registry.ModItems;
import com.futuretek.robot.RobotEntity;
import com.futuretek.robot.RobotItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Fasi 12-14: casa automatica, macchine del Nether, robot */
@GameTestHolder(FutureTek.MODID)
@PrefixGameTestTemplate(false)
public class Phase12to14GameTests {
    private static final String EMPTY = "empty";

    private static void floor(GameTestHelper helper, net.minecraft.world.level.block.Block block) {
        for (BlockPos p : BlockPos.betweenClosed(new BlockPos(0, 1, 0), new BlockPos(8, 1, 8))) {
            helper.setBlock(p, block);
        }
    }

    private static void power(GameTestHelper helper, BlockPos pos) {
        MachineBlockEntity m = helper.getBlockEntity(pos);
        helper.onEachTick(() -> m.getEnergy().setEnergy(m.getEnergy().getMaxEnergyStored()));
    }

    /** Lampade: si accendono con l'energia e se la passano; l'interruttore spento non lascia passare */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void lampsAndSwitch(GameTestHelper helper) {
        BlockPos a = new BlockPos(1, 2, 1);
        helper.setBlock(a, ModBlocks.ELECTRIC_LAMP.get());
        helper.setBlock(a.east(), ModBlocks.ELECTRIC_LAMP.get());
        helper.setBlock(a.east(2), ModBlocks.LIGHT_PANEL.get());
        MachineBlockEntity first = helper.getBlockEntity(a);
        helper.onEachTick(() -> first.getEnergy().setEnergy(400));
        BlockPos sw = new BlockPos(5, 2, 5);
        helper.setBlock(sw, ModBlocks.ENERGY_SWITCH.get());
        HomeMachines.SwitchEntity switchBe = helper.getBlockEntity(sw);
        helper.assertTrue(switchBe.getEnergyHandler(Direction.SOUTH).receiveEnergy(100, false) == 0,
                "L'interruttore spento non deve ricevere energia");
        helper.succeedWhen(() -> {
            helper.assertBlockProperty(a.east(), HomeMachines.LampBlock.LIT, true);
            helper.assertBlockProperty(a.east(2), HomeMachines.LampBlock.LIT, true);
        });
    }

    /** Fattoria di alberi: pianta gli alberelli e taglia un albero (non le case di legno) */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void treeFarmPlantsAndCuts(GameTestHelper helper) {
        floor(helper, Blocks.DIRT);
        BlockPos farm = new BlockPos(4, 2, 4);
        helper.setBlock(farm, ModBlocks.TREE_FARM.get());
        helper.setBlock(farm.above(), Blocks.CHEST);
        power(helper, farm);
        HomeMachines.TreeFarm be = helper.getBlockEntity(farm);
        be.getItems().setStackInSlot(0, new ItemStack(Items.OAK_SAPLING, 8));
        // un "albero": tronco con foglie naturali
        helper.setBlock(new BlockPos(6, 2, 6), Blocks.OAK_LOG);
        helper.setBlock(new BlockPos(6, 3, 6), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false));
        // un tronco "di casa" senza foglie: non va tagliato
        helper.setBlock(new BlockPos(0, 2, 8), Blocks.OAK_LOG);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.OAK_SAPLING, new BlockPos(1, 2, 1));
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(6, 2, 6));
            helper.assertBlockPresent(Blocks.OAK_LOG, new BlockPos(0, 2, 8));
            ChestBlockEntity chest = helper.getBlockEntity(farm.above());
            helper.assertTrue(chest.countItem(Items.OAK_LOG) >= 1, "Il legno non e' nel baule");
        });
    }

    /** Piantatrice: zappa la terra e pianta il grano */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void planterTillsAndPlants(GameTestHelper helper) {
        floor(helper, Blocks.DIRT);
        BlockPos planter = new BlockPos(4, 2, 4);
        helper.setBlock(planter, ModBlocks.PLANTER.get());
        power(helper, planter);
        HomeMachines.Planter be = helper.getBlockEntity(planter);
        be.getItems().setStackInSlot(0, new ItemStack(Items.WHEAT_SEEDS, 64));
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.FARMLAND, new BlockPos(0, 1, 0));
            helper.assertBlockPresent(Blocks.WHEAT, new BlockPos(0, 2, 0));
        });
    }

    /** Fertilizzatore: fa crescere il grano con la farina d'ossa */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void fertilizerGrowsCrops(GameTestHelper helper) {
        floor(helper, Blocks.FARMLAND);
        for (int x = 2; x <= 6; x++) {
            for (int z = 2; z <= 6; z++) {
                if (x != 4 || z != 4) {
                    helper.setBlock(new BlockPos(x, 2, z), Blocks.WHEAT);
                }
            }
        }
        BlockPos f = new BlockPos(4, 2, 4);
        helper.setBlock(f, ModBlocks.FERTILIZER.get());
        power(helper, f);
        HomeMachines.Fertilizer be = helper.getBlockEntity(f);
        be.getItems().setStackInSlot(0, new ItemStack(Items.BONE_MEAL, 64));
        helper.succeedWhen(() -> {
            helper.assertTrue(be.getItems().getStackInSlot(0).getCount() < 64, "Non ha usato la farina d'ossa");
            boolean grown = false;
            for (int x = 2; x <= 6; x++) {
                for (int z = 2; z <= 6; z++) {
                    var s = helper.getBlockState(new BlockPos(x, 2, z));
                    if (s.is(Blocks.WHEAT) && s.getValue(CropBlock.AGE) > 0) {
                        grown = true;
                    }
                }
            }
            helper.assertTrue(grown, "Nessuna pianta cresciuta");
        });
    }

    /** Incantatore elettrico: incanta con 9 livelli invece di 30, consumando energia e lapislazzuli */
    @GameTest(template = EMPTY)
    public static void enchanterUsesFewerLevels(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, ModBlocks.ELECTRIC_ENCHANTER.get());
        NetherMachines.Enchanter be = helper.getBlockEntity(pos);
        be.getEnergy().setEnergy(250_000);
        be.getItems().setStackInSlot(0, new ItemStack(Items.DIAMOND_SWORD));
        be.getItems().setStackInSlot(1, new ItemStack(Items.LAPIS_LAZULI, 3));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 9;
        be.onButton(player, 2);
        ItemStack sword = be.getItems().getStackInSlot(0);
        helper.assertTrue(sword.isEnchanted(), "La spada non e' incantata");
        helper.assertTrue(player.experienceLevel == 0, "Livelli rimasti: " + player.experienceLevel);
        helper.assertTrue(be.getItems().getStackInSlot(1).isEmpty(), "Lapislazzuli non consumati");
        helper.assertTrue(be.getEnergy().getEnergyStored() == 50_000, "Energia: " + be.getEnergy().getEnergyStored());
        helper.succeed();
    }

    /** Incubatore: con il campione di mucca e l'energia fa nascere una mucca sopra */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void incubatorSpawnsSampledMob(GameTestHelper helper) {
        BlockPos pos = new BlockPos(4, 1, 4);
        helper.setBlock(pos, ModBlocks.MOB_INCUBATOR.get());
        power(helper, pos);
        NetherMachines.Incubator be = helper.getBlockEntity(pos);
        be.getItems().setStackInSlot(0, NetherMachines.sampleOf(ModItems.MOB_SAMPLER.get(), EntityType.COW));
        helper.succeedWhen(() -> helper.assertEntityPresent(EntityType.COW));
    }

    /** Torretta laser: elimina lo zombie e non tocca la mucca (modalita' "solo mostri") */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void laserTurretKillsMonsters(GameTestHelper helper) {
        BlockPos pos = new BlockPos(4, 2, 4);
        helper.setBlock(pos, ModBlocks.LASER_TURRET.get());
        power(helper, pos);
        helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(6, 2, 6));
        helper.succeedWhen(() -> {
            helper.assertEntityNotPresent(EntityType.ZOMBIE);
            helper.assertEntityPresent(EntityType.COW);
        });
    }

    private static RobotEntity robotOn(GameTestHelper helper, BlockPos dock, int job, ItemStack tool) {
        RobotEntity robot = helper.spawn(ModEntities.ROBOT.get(), dock.above());
        robot.setDock(helper.absolutePos(dock));
        robot.setEnergy(RobotEntity.CAPACITY);
        robot.setMode(job);
        robot.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, tool);
        return robot;
    }

    /** La torretta mette nel baule attaccato gli oggetti delle creature uccise */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void laserTurretCollectsDrops(GameTestHelper helper) {
        BlockPos pos = new BlockPos(4, 2, 4);
        helper.setBlock(pos, ModBlocks.LASER_TURRET.get());
        helper.setBlock(pos.above(), Blocks.CHEST);
        power(helper, pos);
        NetherMachines.LaserTurret turret = helper.getBlockEntity(pos);
        turret.onButton(helper.makeMockPlayer(GameType.CREATIVE), 0); // tutte le creature
        helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
        helper.succeedWhen(() -> {
            helper.assertEntityNotPresent(EntityType.COW);
            helper.assertTrue(((ChestBlockEntity) helper.getBlockEntity(pos.above())).countItem(Items.BEEF) >= 1,
                    "La carne non e' nel baule");
        });
    }

    /** Robot contadino (con la zappa): raccoglie il grano maturo e lo porta nel baule scelto col pannello, anche lontano */
    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void robotFarmerHarvests(GameTestHelper helper) {
        floor(helper, Blocks.FARMLAND);
        BlockPos dock = new BlockPos(1, 1, 1);
        helper.setBlock(dock, ModBlocks.ROBOT_DOCK.get());
        BlockPos farChest = new BlockPos(7, 2, 7);
        helper.setBlock(farChest, Blocks.CHEST);
        BlockPos crop = new BlockPos(5, 2, 5);
        helper.setBlock(crop, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
        RobotEntity robot = robotOn(helper, dock, RobotEntity.FARMER, new ItemStack(Items.IRON_HOE));
        robot.setChest(helper.absolutePos(farChest));
        helper.succeedWhen(() -> {
            var s = helper.getBlockState(crop);
            helper.assertTrue(s.is(Blocks.WHEAT) && s.getValue(CropBlock.AGE) == 0, "Il grano non e' stato raccolto");
            helper.assertTrue(((ChestBlockEntity) helper.getBlockEntity(farChest)).countItem(Items.WHEAT) >= 1,
                    "Il raccolto non e' nel baule collegato");
            helper.assertTrue(robot.getMainHandItem().getDamageValue() > 0, "La zappa non si e' consumata");
        });
    }

    /**
     * Robot minatore (col piccone). Le prove sono sul fondo del mondo (bedrock vicina), quindi qui la scala finisce
     * subito e il robot passa alle gallerie: deve scavarle e prendere i minerali che vede di lato.
     */
    @GameTest(template = EMPTY, timeoutTicks = 900)
    public static void robotMinerDigsStairs(GameTestHelper helper) {
        for (BlockPos p : BlockPos.betweenClosed(new BlockPos(0, 0, 0), new BlockPos(7, 2, 7))) {
            helper.setBlock(p, Blocks.STONE);
        }
        BlockPos dock = new BlockPos(3, 2, 1);
        helper.setBlock(dock, ModBlocks.ROBOT_DOCK.get());
        helper.setBlock(dock.north(), Blocks.CHEST); // baule a nord: la scala va verso sud
        helper.setBlock(new BlockPos(4, 2, 5), Blocks.IRON_ORE); // di lato alla galleria
        helper.setBlock(new BlockPos(3, 1, 6), Blocks.AIR); // buco nel pavimento della galleria: va tappato
        RobotEntity robot = robotOn(helper, dock, RobotEntity.MINER, new ItemStack(Items.DIAMOND_PICKAXE));
        helper.succeedWhen(() -> {
            helper.assertTrue(robot.getMinePath().size() >= 3, "Gradini scavati: " + robot.getMinePath() + " stato "
                    + robot.getState() + " pos " + helper.relativePos(robot.blockPosition()) + " energia " + robot.getEnergy()
                    + " attrezzo " + robot.getMainHandItem());
            String info = robot.getMinePath().stream().map(helper::relativePos).toList() + " stato " + robot.getState()
                    + " pos " + helper.relativePos(robot.blockPosition());
            helper.assertTrue(helper.getBlockState(new BlockPos(3, 2, 6)).isAir(), "Galleria non scavata: " + info);
            helper.assertTrue(helper.getBlockState(new BlockPos(4, 2, 5)).isAir(), "Ferro non scavato: " + info);
            helper.assertTrue(!helper.getBlockState(new BlockPos(3, 1, 6)).isAir(), "Buco nel pavimento non tappato: " + info);
            helper.assertTrue(robot.getInventory().countItem(Items.RAW_IRON) >= 1, "Non ha preso il ferro");
            helper.assertTrue(robot.getInventory().countItem(Items.COBBLESTONE) >= 1, "Non ha raccolto la pietra");
        });
    }

    /** Senza attrezzo il robot va al baule e prende un'ascia */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void robotTakesToolFromChest(GameTestHelper helper) {
        floor(helper, Blocks.STONE);
        BlockPos dock = new BlockPos(1, 1, 1);
        helper.setBlock(dock, ModBlocks.ROBOT_DOCK.get());
        helper.setBlock(dock.east(), Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(dock.east());
        chest.setItem(5, new ItemStack(Items.STONE_AXE));
        RobotEntity robot = robotOn(helper, dock, RobotEntity.LUMBERJACK, ItemStack.EMPTY);
        helper.succeedWhen(() -> {
            helper.assertTrue(robot.getMainHandItem().is(Items.STONE_AXE), "Il robot non ha preso l'ascia");
            helper.assertTrue(chest.countItem(Items.STONE_AXE) == 0, "L'ascia e' ancora nel baule");
        });
    }

    /** Baule pieno: il robot si ferma alla base finche' non gli dici di continuare */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void robotStopsWhenChestFull(GameTestHelper helper) {
        floor(helper, Blocks.STONE);
        BlockPos dock = new BlockPos(1, 1, 1);
        helper.setBlock(dock, ModBlocks.ROBOT_DOCK.get());
        helper.setBlock(dock.east(), Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(dock.east());
        for (int i = 0; i < chest.getContainerSize(); i++) {
            chest.setItem(i, new ItemStack(Items.STONE, 64));
        }
        RobotEntity robot = robotOn(helper, dock, RobotEntity.FARMER, new ItemStack(Items.IRON_HOE));
        for (int i = 0; i < robot.getInventory().getContainerSize(); i++) {
            robot.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
        }
        helper.succeedWhen(() -> helper.assertTrue(robot.getState() == RobotEntity.CHEST_FULL, "Stato: " + robot.getState()));
    }

    /** Il robot diventa oggetto e torna robot con la stessa energia e lo stesso lavoro */
    @GameTest(template = EMPTY)
    public static void robotItemKeepsData(GameTestHelper helper) {
        RobotEntity robot = helper.spawn(ModEntities.ROBOT.get(), new BlockPos(1, 2, 1));
        robot.setEnergy(12_345);
        robot.setMode(RobotEntity.MINER);
        robot.setInfinite(true);
        ItemStack item = RobotItem.fromRobot(robot);
        var tag = item.get(DataComponents.CUSTOM_DATA).copyTag();
        helper.assertTrue(tag.getInt("Energy") == 12_345 && tag.getInt("Mode") == RobotEntity.MINER && tag.getBoolean("Infinite"),
                "Dati del robot persi: " + tag);
        helper.assertTrue(robot.use(1_000_000), "Il nucleo infinito deve dare energia senza limiti");
        robot.discard();
        helper.succeed();
    }
}
