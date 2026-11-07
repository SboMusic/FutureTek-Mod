package com.futuretek.gametest;

import java.util.List;

import com.futuretek.FutureTek;
import com.futuretek.alchemy.AlchemyBlocks;
import com.futuretek.alchemy.AlchemyItems;
import com.futuretek.alchemy.AreaBreaker;
import com.futuretek.alchemy.SatchelInventory;
import com.futuretek.essence.EssenceManager;
import com.futuretek.essence.EssenceValues;
import com.futuretek.registry.ModBlocks;
import com.futuretek.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Test automatici dell'alchimia avanzata (Fase 4b). */
@GameTestHolder(FutureTek.MODID)
@PrefixGameTestTemplate(false)
public class AlchemyGameTests {
    private static final String EMPTY = "empty";

    /** Oggetti consumati valgono meno, quelli incantati di piu' */
    @GameTest(template = EMPTY)
    public static void stackValueWearAndEnchantments(GameTestHelper helper) {
        ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
        long fresh = EssenceValues.stackValue(false, pick);
        pick.setDamageValue(pick.getMaxDamage() / 2);
        long worn = EssenceValues.stackValue(false, pick);
        helper.assertTrue(fresh > 0 && worn < fresh && worn >= fresh / 2 - 2,
                "Usura: nuovo " + fresh + ", a meta' " + worn);
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        long plain = EssenceValues.stackValue(false, sword);
        sword.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SHARPNESS), 2);
        long enchanted = EssenceValues.stackValue(false, sword);
        helper.assertTrue(enchanted == plain + 2 * EssenceValues.ENCHANTMENT_LEVEL_VALUE,
                "Incantesimi: senza " + plain + ", con Affilatezza II " + enchanted);
        helper.succeed();
    }

    /** Braci: valore dalle ricette e durata come combustibile */
    @GameTest(template = EMPTY)
    public static void embers(GameTestHelper helper) {
        long arcane = EssenceValues.get(false, ModItems.ARCANE_EMBER.get());
        long eternal = EssenceValues.get(false, ModItems.ETERNAL_EMBER.get());
        helper.assertTrue(arcane == 4 * 128, "Brace Arcana: attesa 512, trovata " + arcane);
        helper.assertTrue(eternal == 64 * 128, "Brace Eterna: attesa 8192, trovata " + eternal);
        helper.assertTrue(new ItemStack(ModItems.ARCANE_EMBER.get()).getBurnTime(RecipeType.SMELTING) == 6400,
                "Brace Arcana: durata come combustibile errata");
        helper.assertTrue(EssenceValues.get(false, ModItems.NOCTURNITE_PICKAXE.get()) > 0,
                "Il piccone di Nocturnite non ha un valore");
        helper.succeed();
    }

    /** Il Catalizzatore rompe un cubo 3x3x3, mette tutto nell'inventario e fa pagare l'Essenza */
    @GameTest(template = EMPTY)
    public static void voidCatalystArea(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        EssenceManager.addEssence(player, 1000);
        BlockPos start = new BlockPos(2, 1, 2);
        for (BlockPos pos : BlockPos.betweenClosed(start, start.offset(2, 2, 2))) {
            helper.setBlock(pos, Blocks.STONE);
        }
        List<BlockPos> area = AreaBreaker.box(helper.absolutePos(start.offset(1, 2, 1)), Direction.DOWN, 1, 3);
        int broken = AreaBreaker.breakBlocks(helper.getLevel(), area, player, helper.absolutePos(start),
                AlchemyItems.VoidCatalyst.COST_PER_BLOCK);
        helper.assertTrue(broken == 27, "Blocchi rotti: attesi 27, trovati " + broken);
        helper.assertBlockPresent(Blocks.AIR, start.offset(1, 1, 1));
        helper.assertTrue(player.getInventory().countItem(Items.COBBLESTONE) == 27,
                "Pietrisco nell'inventario: " + player.getInventory().countItem(Items.COBBLESTONE));
        helper.assertTrue(EssenceManager.get(player).essence() == 1000 - 27 * 4,
                "Essenza rimasta errata: " + EssenceManager.get(player).essence());
        helper.succeed();
    }

    /** Senza Essenza il catalizzatore non rompe niente */
    @GameTest(template = EMPTY)
    public static void voidCatalystNeedsEssence(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        int broken = AreaBreaker.breakBlocks(helper.getLevel(), List.of(helper.absolutePos(new BlockPos(2, 1, 2))),
                player, helper.absolutePos(new BlockPos(2, 1, 2)), 4);
        helper.assertTrue(broken == 0, "Ha rotto blocchi senza Essenza");
        helper.assertBlockPresent(Blocks.STONE, new BlockPos(2, 1, 2));
        helper.succeed();
    }

    /** Carica Stellare: un segnale di redstone la innesca */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void starChargePrimesWithRedstone(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, ModBlocks.STAR_CHARGE.get());
        helper.setBlock(pos.east(), Blocks.REDSTONE_BLOCK);
        helper.succeedWhen(() -> helper.assertBlockProperty(pos, AlchemyBlocks.StarCharge.LIT, true));
    }

    /** Clessidra Arcana: una fornace accelerata cuoce subito */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void hourglassAcceleratesFurnace(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, Blocks.FURNACE);
        FurnaceBlockEntity furnace = helper.getBlockEntity(pos);
        furnace.setItem(0, new ItemStack(Items.RAW_IRON));
        furnace.setItem(1, new ItemStack(Items.COAL));
        AlchemyItems.ArcaneHourglass.accelerate(helper.getLevel(), helper.absolutePos(pos), 1, 210);
        helper.assertTrue(furnace.getItem(2).is(Items.IRON_INGOT), "La fornace accelerata non ha cotto il ferro");
        helper.succeed();
    }

    /** Gemma della Memoria: conserva e restituisce l'esperienza */
    @GameTest(template = EMPTY)
    public static void memoryGemStoresXp(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.giveExperienceLevels(10);
        int before = AlchemyItems.MemoryGem.totalXp(player);
        ItemStack gem = new ItemStack(ModItems.MEMORY_GEM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, gem);
        gem.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.experienceLevel == 0, "Livelli rimasti: " + player.experienceLevel);
        helper.assertTrue(AlchemyItems.MemoryGem.stored(gem) == before,
                "Esperienza conservata " + AlchemyItems.MemoryGem.stored(gem) + " invece di " + before);
        player.setShiftKeyDown(true);
        gem.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.experienceLevel == 10, "Livelli restituiti: " + player.experienceLevel);
        helper.succeed();
    }

    /** Bisaccia: il contenuto si salva e si ricarica allo stesso posto */
    @GameTest(template = EMPTY)
    public static void satchelSaves(GameTestHelper helper) {
        SatchelInventory satchel = new SatchelInventory();
        satchel.setItem(40, new ItemStack(Items.DIAMOND, 7));
        var tag = satchel.serializeNBT(helper.getLevel().registryAccess());
        SatchelInventory loaded = new SatchelInventory();
        loaded.deserializeNBT(helper.getLevel().registryAccess(), tag);
        helper.assertTrue(loaded.getItem(40).is(Items.DIAMOND) && loaded.getItem(40).getCount() == 7,
                "Il contenuto della bisaccia non e' stato ricaricato");
        helper.succeed();
    }

    /** Il Tomo dell'Onniscienza insegna tutti gli oggetti con un valore */
    @GameTest(template = EMPTY)
    public static void tomeLearnsEverything(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        int learned = EssenceManager.learnAll(player);
        helper.assertTrue(learned > 1000 && learned < EssenceValues.serverValues().size(),
                "Oggetti imparati: " + learned);
        helper.succeed();
    }

    /** Grezzi e polveri si sciolgono ma non si creano (altrimenti: Essenza infinita con le macchine) */
    @GameTest(template = EMPTY)
    public static void rawMaterialsNotCreatable(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        long gained = EssenceManager.dissolve(player, new ItemStack(Items.RAW_IRON, 2));
        helper.assertTrue(gained == 512, "Sciogliere 2 ferro grezzo deve dare 512, dato " + gained);
        helper.assertTrue(EssenceManager.get(player).known().isEmpty(), "Il ferro grezzo non deve essere imparato");
        helper.assertTrue(!EssenceValues.isCreatable(false, ModItems.DUST.get(com.futuretek.registry.Metal.TIN).get()),
                "La polvere di stagno non deve essere creabile");
        helper.assertTrue(EssenceValues.isCreatable(false, Items.IRON_INGOT), "Il lingotto di ferro deve essere creabile");
        helper.succeed();
    }

    /** Tutti gli oggetti del gioco hanno un valore, tranne quelli solo creativi (uova generatrici, comandi...) */
    @GameTest(template = EMPTY)
    public static void everyObtainableItemHasValue(GameTestHelper helper) {
        List<String> unexpected = new java.util.ArrayList<>();
        for (var id : EssenceValues.missing()) {
            if (!id.getPath().endsWith("_spawn_egg") && !CREATIVE_ONLY.contains(id.toString())) {
                unexpected.add(id.toString());
            }
        }
        helper.assertTrue(unexpected.isEmpty(), "Oggetti senza valore: " + unexpected);
        helper.succeed();
    }

    /** Oggetti che restano senza valore apposta: non si ottengono giocando normalmente */
    private static final java.util.Set<String> CREATIVE_ONLY = java.util.Set.of(
            "minecraft:barrier", "minecraft:bedrock", "minecraft:chain_command_block", "minecraft:command_block",
            "minecraft:command_block_minecart", "minecraft:debug_stick", "minecraft:jigsaw", "minecraft:knowledge_book",
            "minecraft:light", "minecraft:petrified_oak_slab", "minecraft:repeating_command_block", "minecraft:spawner",
            "minecraft:structure_block", "minecraft:structure_void", "minecraft:trial_spawner", "minecraft:vault");
}
