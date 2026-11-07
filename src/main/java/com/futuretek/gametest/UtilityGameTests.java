package com.futuretek.gametest;

import java.util.Map;

import com.futuretek.FutureTek;
import com.futuretek.alchemy.AlchemyItems;
import com.futuretek.registry.ModBlocks;
import com.futuretek.registry.ModItems;
import com.futuretek.utility.IronFurnace;
import com.futuretek.utility.MinerBag;
import com.futuretek.utility.OreScanner;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Test automatici delle comodita': alberi, fornace di ferro, rilevatori, sacca, catalizzatore caricabile. */
@GameTestHolder(FutureTek.MODID)
@PrefixGameTestTemplate(false)
public class UtilityGameTests {
    private static final String EMPTY = "empty";

    private static void tree(GameTestHelper helper, BlockPos base, int height, boolean leaves) {
        for (int y = 0; y < height; y++) {
            helper.setBlock(base.above(y), Blocks.OAK_LOG);
        }
        if (leaves) {
            BlockPos top = base.above(height - 1);
            for (BlockPos p : new BlockPos[]{top.east(), top.west(), top.north(), top.south(), top.above()}) {
                helper.setBlock(p, Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false));
            }
        }
    }

    private static ServerPlayer axePlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        return player;
    }

    /** Un'ascia abbatte tutto l'albero */
    @GameTest(template = EMPTY)
    public static void axeFellsWholeTree(GameTestHelper helper) {
        BlockPos base = new BlockPos(3, 1, 3);
        tree(helper, base, 5, true);
        ServerPlayer player = axePlayer(helper);
        player.gameMode.destroyBlock(helper.absolutePos(base));
        for (int y = 0; y < 5; y++) {
            helper.assertBlockNotPresent(Blocks.OAK_LOG, base.above(y));
        }
        helper.succeed();
    }

    /** Senza foglie naturali (es. una casa di legno) si rompe un tronco solo */
    @GameTest(template = EMPTY)
    public static void axeSparesHouses(GameTestHelper helper) {
        BlockPos base = new BlockPos(3, 1, 3);
        tree(helper, base, 3, false);
        ServerPlayer player = axePlayer(helper);
        player.gameMode.destroyBlock(helper.absolutePos(base));
        helper.assertBlockPresent(Blocks.OAK_LOG, base.above());
        helper.assertBlockPresent(Blocks.OAK_LOG, base.above(2));
        helper.succeed();
    }

    /** Fornace di ferro: cuoce in meno di 7 secondi (la fornace normale ne impiega 10) */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void ironFurnaceIsFaster(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.IRON_FURNACE.get());
        IronFurnace.Entity furnace = helper.getBlockEntity(pos);
        furnace.setItem(0, new ItemStack(Items.RAW_IRON, 2));
        furnace.setItem(1, new ItemStack(Items.COAL));
        helper.runAtTickTime(100, () -> helper.assertTrue(furnace.getItem(2).isEmpty(), "Troppo veloce: gia' cotto a 100 tick"));
        helper.runAtTickTime(140, () -> {
            helper.assertTrue(furnace.getItem(2).is(Items.IRON_INGOT), "A 140 tick non ha ancora cotto (troppo lenta)");
            helper.succeed();
        });
    }

    /** Il rilevatore trova i minerali e il piu' vicino */
    @GameTest(template = EMPTY)
    public static void scannerFindsOres(GameTestHelper helper) {
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.IRON_ORE);
        helper.setBlock(new BlockPos(4, 1, 3), Blocks.IRON_ORE);
        helper.setBlock(new BlockPos(3, 1, 4), ModBlocks.ORE.get(com.futuretek.registry.Metal.TIN).get());
        Map<Block, OreScanner.Found> found = OreScanner.scan(helper.getLevel(), helper.absolutePos(new BlockPos(3, 3, 3)), 9, 8);
        helper.assertTrue(found.containsKey(Blocks.IRON_ORE) && found.get(Blocks.IRON_ORE).count() == 2,
                "Ferro non trovato correttamente: " + found.keySet());
        helper.assertTrue(found.containsKey(ModBlocks.ORE.get(com.futuretek.registry.Metal.TIN).get()),
                "Stagno non trovato");
        helper.assertTrue(ModItems.ORE_SCANNER_3.get().getSize() == 16, "Il rilevatore III deve coprire 16x16");
        helper.succeed();
    }

    /** Sacca del minatore: accetta minerali e li tiene dentro */
    @GameTest(template = EMPTY)
    public static void minerBagStores(GameTestHelper helper) {
        ItemStack bag = new ItemStack(ModItems.MINER_BAG.get());
        ItemStack rest = MinerBag.insert(bag, new ItemStack(Items.RAW_IRON, 70));
        helper.assertTrue(rest.isEmpty(), "Non e' entrato tutto");
        long total = MinerBag.load(bag).stream().filter(s -> s.is(Items.RAW_IRON)).mapToInt(ItemStack::getCount).sum();
        helper.assertTrue(total == 70, "Nella sacca ci sono " + total + " ferro grezzo");
        helper.assertTrue(new ItemStack(Items.RAW_IRON).is(MinerBag.COLLECTS) && new ItemStack(Items.DIAMOND).is(MinerBag.COLLECTS)
                && !new ItemStack(Items.COBBLESTONE).is(MinerBag.COLLECTS), "Tag della raccolta automatica errato");
        helper.succeed();
    }

    /** Catalizzatore: la carica va da 1 a 4 e poi ricomincia */
    @GameTest(template = EMPTY)
    public static void catalystCharges(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack catalyst = new ItemStack(ModItems.VOID_CATALYST.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, catalyst);
        helper.assertTrue(AlchemyItems.VoidCatalyst.charge(catalyst) == 1, "Carica iniziale diversa da 1");
        for (int i = 0; i < 3; i++) {
            catalyst.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        }
        helper.assertTrue(AlchemyItems.VoidCatalyst.charge(catalyst) == 4, "Dopo 3 click la carica deve essere 4");
        catalyst.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(AlchemyItems.VoidCatalyst.charge(catalyst) == 1, "Dopo la 4 deve tornare a 1");
        helper.succeed();
    }
}
