package com.futuretek.gametest;

import com.futuretek.FutureTek;
import com.futuretek.rail.CargoCart;
import com.futuretek.registry.ModBlocks;
import com.futuretek.registry.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Test automatici delle ferrovie. */
@GameTestHolder(FutureTek.MODID)
@PrefixGameTestTemplate(false)
public class RailGameTests {
    private static final String EMPTY = "empty";

    /** Binario di carico: sposta gli oggetti dal baule sotto al carrello con baule */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void loaderRailFillsCart(GameTestHelper helper) {
        BlockPos rail = new BlockPos(2, 2, 2);
        helper.setBlock(rail.below(), Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(rail.below());
        chest.setItem(0, new ItemStack(Items.IRON_INGOT, 32));
        helper.setBlock(rail, ModBlocks.LOADER_RAIL.get());
        MinecartChest cart = helper.spawn(EntityType.CHEST_MINECART, rail);
        helper.succeedWhen(() -> {
            helper.assertTrue(cart.countItem(Items.IRON_INGOT) == 32, "Il carrello ha " + cart.countItem(Items.IRON_INGOT));
            helper.assertTrue(chest.isEmpty(), "Il baule non si e' svuotato");
        });
    }

    /** Binario di scarico + carrello merci (54 slot): svuota il carrello nel baule sotto */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void unloaderRailEmptiesCargoCart(GameTestHelper helper) {
        BlockPos rail = new BlockPos(2, 2, 2);
        helper.setBlock(rail.below(), Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(rail.below());
        helper.setBlock(rail, ModBlocks.UNLOADER_RAIL.get());
        CargoCart cart = helper.spawn(ModEntities.CARGO_CART.get(), rail);
        helper.assertTrue(cart.getContainerSize() == 54, "Il carrello merci non ha 54 slot");
        cart.setItem(50, new ItemStack(Items.COAL, 20));
        helper.succeedWhen(() -> helper.assertTrue(chest.countItem(Items.COAL) == 20, "Il carbone non e' nel baule"));
    }
}
