package com.futuretek.gametest;

import com.futuretek.FutureTek;
import com.futuretek.blockentity.ReplicatorBlockEntity;
import com.futuretek.blockentity.StarCollectorBlockEntity;
import com.futuretek.essence.EssenceManager;
import com.futuretek.essence.EssenceValues;
import com.futuretek.registry.Metal;
import com.futuretek.registry.ModBlocks;
import com.futuretek.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Test automatici della trasmutazione (Essenza). */
@GameTestHolder(FutureTek.MODID)
@PrefixGameTestTemplate(false)
public class EssenceGameTests {
    private static final String EMPTY = "empty";

    /** Valori fissi dal JSON e valori calcolati dalle ricette */
    @GameTest(template = EMPTY)
    public static void valuesAreComputed(GameTestHelper helper) {
        long iron = EssenceValues.get(false, Items.IRON_INGOT);
        long ironBlock = EssenceValues.get(false, Items.IRON_BLOCK);
        long tinPlate = EssenceValues.get(false, ModItems.PLATE.get(Metal.TIN).get());
        long planks = EssenceValues.get(false, Items.OAK_PLANKS);
        long stone = EssenceValues.get(false, Items.STONE);
        long stairs = EssenceValues.get(false, Items.STONE_BRICK_STAIRS);
        long netheriteSword = EssenceValues.get(false, Items.NETHERITE_SWORD);
        helper.assertTrue(stone == 1, "Pietra: attesa 1, trovata " + stone);
        helper.assertTrue(iron == 256, "Lingotto di ferro: atteso 256, trovato " + iron);
        helper.assertTrue(ironBlock == 9 * 256, "Blocco di ferro: atteso 2304, trovato " + ironBlock);
        helper.assertTrue(tinPlate == 160, "Piastra di stagno (il martello non conta): attesa 160, trovata " + tinPlate);
        helper.assertTrue(planks == 8, "Assi di quercia: attese 8 (32/4), trovate " + planks);
        helper.assertTrue(stairs >= 1, "Scale di mattoni (tagliapietre): valore mancante");
        helper.assertTrue(netheriteSword > EssenceValues.get(false, Items.DIAMOND_SWORD),
                "Spada di netherite (forgiatura): valore mancante o troppo basso: " + netheriteSword);
        helper.succeed();
    }

    /** Sciogliere insegna l'oggetto e da' Essenza; creare la consuma */
    @GameTest(template = EMPTY)
    public static void dissolveAndCreate(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        long gained = EssenceManager.dissolve(player, new ItemStack(Items.DIAMOND, 2));
        helper.assertTrue(gained == 2 * 8192, "Essenza da 2 diamanti errata: " + gained);
        helper.assertTrue(EssenceManager.get(player).known().size() == 1, "Il diamante non e' stato imparato");
        int created = EssenceManager.create(player, Items.DIAMOND, 5);
        helper.assertTrue(created == 2, "Dovevano essere creati 2 diamanti (Essenza sufficiente solo per 2): " + created);
        helper.assertTrue(EssenceManager.get(player).essence() == 0, "Essenza residua errata");
        helper.assertTrue(EssenceManager.create(player, Items.GOLD_INGOT, 1) == 0,
                "Non si puo' creare un oggetto mai imparato");
        helper.succeed();
    }

    /** Replicatore: con il modello "pietrisco" trasforma la terra in pietrisco */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void replicatorCopies(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.REPLICATOR.get());
        ReplicatorBlockEntity replicator = helper.getBlockEntity(pos);
        replicator.getItems().setStackInSlot(ReplicatorBlockEntity.SLOT_TEMPLATE, new ItemStack(Items.COBBLESTONE));
        replicator.getItems().setStackInSlot(ReplicatorBlockEntity.FIRST_INPUT, new ItemStack(Items.DIRT, 10));
        helper.succeedWhen(() -> helper.assertTrue(
                replicator.getItems().getStackInSlot(ReplicatorBlockEntity.SLOT_OUTPUT).getCount() >= 10,
                "Il replicatore non ha prodotto 10 pietrisco"));
    }

    /** Captatore Stellare a cielo aperto raccoglie Essenza e la passa al Replicatore accanto */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void collectorFeedsReplicator(GameTestHelper helper) {
        BlockPos collectorPos = new BlockPos(1, 1, 1);
        helper.setBlock(collectorPos, ModBlocks.STAR_COLLECTOR_3.get());
        helper.setBlock(collectorPos.east(), ModBlocks.REPLICATOR.get());
        ReplicatorBlockEntity replicator = helper.getBlockEntity(collectorPos.east());
        StarCollectorBlockEntity collector = helper.getBlockEntity(collectorPos);
        helper.succeedWhen(() -> helper.assertTrue(replicator.getBuffer() > 0 && collector.getStored() == 0,
                "Il captatore non passa Essenza al replicatore (buffer " + replicator.getBuffer() + ")"));
    }
}
