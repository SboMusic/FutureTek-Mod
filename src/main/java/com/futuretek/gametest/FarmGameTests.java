package com.futuretek.gametest;

import com.futuretek.FutureTek;
import com.futuretek.bee.ApiaryBlockEntity;
import com.futuretek.bee.BeeData;
import com.futuretek.bee.BeeItem;
import com.futuretek.bee.BeeSpecies;
import com.futuretek.farm.FarmBlockEntities;
import com.futuretek.registry.ModBlocks;
import com.futuretek.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Test automatici di agricoltura e api. */
@GameTestHolder(FutureTek.MODID)
@PrefixGameTestTemplate(false)
public class FarmGameTests {
    private static final String EMPTY = "empty";

    /** Raccoglitore: raccoglie il grano maturo, lo mette nel baule e ripianta */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void harvesterHarvestsAndReplants(GameTestHelper helper) {
        BlockPos machine = new BlockPos(1, 2, 1);
        helper.setBlock(machine, ModBlocks.HARVESTER.get());
        helper.setBlock(machine.above(), Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(machine.above());
        BlockPos crop = new BlockPos(3, 2, 1);
        helper.setBlock(crop.below(), Blocks.FARMLAND);
        helper.setBlock(crop, ((CropBlock) Blocks.WHEAT).getStateForAge(7));
        FarmBlockEntities.Harvester harvester = helper.getBlockEntity(machine);
        harvester.getEnergy().setEnergy(10_000);
        helper.succeedWhen(() -> {
            helper.assertTrue(chest.countItem(Items.WHEAT) >= 1, "Il grano non e' nel baule");
            helper.assertBlockPresent(Blocks.WHEAT, crop);
            helper.assertTrue(((CropBlock) Blocks.WHEAT).getAge(helper.getBlockState(crop)) == 0, "Non ha ripiantato");
        });
    }

    /** Arnia: un ciclo produce favi della specie della regina; a fine vita nasce una nuova generazione */
    @GameTest(template = EMPTY)
    public static void apiaryProducesAndBreeds(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.APIARY.get());
        ApiaryBlockEntity apiary = helper.getBlockEntity(pos);
        apiary.getItems().setStackInSlot(ApiaryBlockEntity.SLOT_QUEEN,
                BeeItem.of(ModItems.BEE_QUEEN.get(), BeeSpecies.METALLIC, 1));
        apiary.getItems().setStackInSlot(ApiaryBlockEntity.SLOT_DRONE,
                BeeItem.of(ModItems.BEE_DRONE.get(), BeeSpecies.METALLIC, 1));
        for (int i = 0; i < 60; i++) {
            apiary.runCycle();
            if (apiary.getItems().getStackInSlot(ApiaryBlockEntity.SLOT_DRONE).isEmpty()) {
                apiary.getItems().setStackInSlot(ApiaryBlockEntity.SLOT_DRONE,
                        BeeItem.of(ModItems.BEE_DRONE.get(), BeeSpecies.METALLIC, 1));
            }
        }
        int combs = 0;
        int drones = 0;
        for (int i = ApiaryBlockEntity.FIRST_OUTPUT; i < 11; i++) {
            ItemStack s = apiary.getItems().getStackInSlot(i);
            if (s.is(ModItems.COMB.get("metallic").get())) {
                combs += s.getCount();
            }
            if (s.is(ModItems.BEE_DRONE.get())) {
                drones += s.getCount();
            }
        }
        helper.assertTrue(combs > 5, "Troppi pochi favi metallici: " + combs);
        helper.assertTrue(drones > 0, "Nessun fuco nato dalle nuove generazioni");
        helper.assertTrue(!apiary.getItems().getStackInSlot(ApiaryBlockEntity.SLOT_QUEEN).isEmpty(), "Regina sparita");
        helper.succeed();
    }

    /** Gli incroci del JSON sono caricati */
    @GameTest(template = EMPTY)
    public static void beeMutationsLoaded(GameTestHelper helper) {
        helper.assertTrue(BeeData.mutations().size() >= 7, "Incroci non caricati: " + BeeData.mutations().size());
        helper.assertTrue(!BeeData.products(BeeSpecies.STELLAR).isEmpty(), "Prodotti non caricati");
        helper.succeed();
    }
}
