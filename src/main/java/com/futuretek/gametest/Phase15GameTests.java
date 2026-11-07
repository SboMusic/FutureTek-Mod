package com.futuretek.gametest;

import com.futuretek.FutureTek;
import com.futuretek.blockentity.ReactorBlockEntity;
import com.futuretek.item.ReactorComponentItem;
import com.futuretek.nuclear.NuclearBlast;
import com.futuretek.nuclear.ReactorMaintainer;
import com.futuretek.registry.ModBlocks;
import com.futuretek.registry.ModEntities;
import com.futuretek.registry.ModItems;
import com.futuretek.utility.Shelter;
import com.futuretek.world.AbandonedFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Fase 15: nucleare e fabbrica abbandonata */
@GameTestHolder(FutureTek.MODID)
@PrefixGameTestTemplate(false)
public class Phase15GameTests {
    private static final String EMPTY = "empty";

    private static ItemStack part(ReactorComponentItem.Type type) {
        return new ItemStack(ModItems.REACTOR_PARTS.get(type).get());
    }

    /** L'esplosione nucleare cancella una sfera di blocchi ma non la bedrock */
    @GameTest(template = EMPTY, timeoutTicks = 60, batch = "nucleare")
    public static void nuclearBlastClearsSphere(GameTestHelper helper) {
        for (BlockPos p : BlockPos.betweenClosed(new BlockPos(0, 1, 0), new BlockPos(7, 3, 7))) {
            helper.setBlock(p, Blocks.STONE);
        }
        helper.setBlock(new BlockPos(4, 2, 3), Blocks.BEDROCK);
        NuclearBlast.start(helper.getLevel(), helper.absolutePos(new BlockPos(4, 2, 4)), 2, null);
        helper.succeedWhen(() -> {
            helper.assertTrue(!NuclearBlast.isRunning(), "Esplosione ancora in corso");
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(4, 2, 4));
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(5, 2, 4));
            helper.assertBlockPresent(Blocks.BEDROCK, new BlockPos(4, 2, 3));
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(0, 1, 0)); // lontano: intatto
        });
    }

    /** La TNT nucleare si accende come la TNT */
    @GameTest(template = EMPTY)
    public static void nuclearTntPrimes(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, ModBlocks.NUCLEAR_TNT.get());
        Block block = ModBlocks.NUCLEAR_TNT.get();
        block.onCaughtFire(helper.getBlockState(pos), helper.getLevel(), helper.absolutePos(pos), null, null);
        helper.assertEntityPresent(ModEntities.PRIMED_NUKE.get());
        helper.assertBlockPresent(Blocks.AIR, pos);
        helper.killAllEntitiesOfClass(com.futuretek.nuclear.NukeBlock.Primed.class);
        helper.succeed();
    }

    /** Ogni camera attaccata al nucleo aggiunge una colonna */
    @GameTest(template = EMPTY)
    public static void reactorChambersAddColumns(GameTestHelper helper) {
        BlockPos core = new BlockPos(3, 2, 3);
        helper.setBlock(core, ModBlocks.REACTOR.get());
        helper.setBlock(core.east(), ModBlocks.REACTOR_CHAMBER.get());
        helper.setBlock(core.west(), ModBlocks.REACTOR_CHAMBER.get());
        ReactorBlockEntity reactor = helper.getBlockEntity(core);
        reactor.simulateSecond();
        helper.assertTrue(reactor.getColumns() == 5, "Colonne: " + reactor.getColumns());
        helper.assertTrue(reactor.isActiveSlot(4) && !reactor.isActiveSlot(5), "Caselle attive sbagliate");
        helper.assertTrue(reactor.getItems().getSlots() == 54, "La griglia deve avere 54 caselle");
        helper.succeed();
    }

    /** Le celle di raffreddamento piene diventano esauste */
    @GameTest(template = EMPTY)
    public static void coolantCellsGetSpent(GameTestHelper helper) {
        BlockPos core = new BlockPos(3, 2, 3);
        helper.setBlock(core, ModBlocks.REACTOR.get());
        helper.setBlock(core.above(), Blocks.REDSTONE_BLOCK);
        ReactorBlockEntity reactor = helper.getBlockEntity(core);
        reactor.getItems().setStackInSlot(0, part(ReactorComponentItem.Type.FUEL_ROD));
        ItemStack cell = part(ReactorComponentItem.Type.COOLANT_CELL);
        cell.setDamageValue(cell.getMaxDamage() - 1);
        reactor.getItems().setStackInSlot(1, cell);
        reactor.simulateSecond();
        helper.assertTrue(reactor.getItems().getStackInSlot(1).is(ModItems.SPENT_COOLANT_CELL.get()),
                "La cella piena non e' diventata esausta: " + reactor.getItems().getStackInSlot(1));
        helper.succeed();
    }

    /** Al 100% di calore il reattore esplode (non si spegne da solo) */
    @GameTest(template = EMPTY, timeoutTicks = 100, batch = "nucleare_reattore")
    public static void reactorExplodesWhenCritical(GameTestHelper helper) {
        BlockPos core = new BlockPos(4, 2, 4);
        helper.setBlock(core, ModBlocks.REACTOR.get());
        helper.setBlock(core.above(), Blocks.REDSTONE_BLOCK);
        ReactorBlockEntity reactor = helper.getBlockEntity(core);
        reactor.getItems().setStackInSlot(1, part(ReactorComponentItem.Type.FUEL_ROD));
        reactor.setHeat(ReactorBlockEntity.BASE_MAX_HEAT - 1);
        reactor.simulateSecond();
        helper.succeedWhen(() -> {
            helper.assertBlockNotPresent(ModBlocks.REACTOR.get(), core);
            helper.assertTrue(!NuclearBlast.isRunning(), "Esplosione in corso");
        });
    }

    /** Il manutentore cambia la cella calda con una nuova */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void maintainerSwapsCoolant(GameTestHelper helper) {
        BlockPos core = new BlockPos(3, 2, 3);
        helper.setBlock(core, ModBlocks.REACTOR.get());
        helper.setBlock(core.east(), ModBlocks.REACTOR_MAINTAINER.get());
        ReactorBlockEntity reactor = helper.getBlockEntity(core);
        ReactorMaintainer maintainer = helper.getBlockEntity(core.east());
        maintainer.getEnergy().setEnergy(10_000);
        ItemStack hot = part(ReactorComponentItem.Type.COOLANT_CELL);
        hot.setDamageValue((int) (hot.getMaxDamage() * 0.95));
        reactor.getItems().setStackInSlot(0, hot);
        reactor.getItems().setStackInSlot(1, new ItemStack(ModItems.SPENT_COOLANT_CELL.get()));
        maintainer.getItems().setStackInSlot(0, part(ReactorComponentItem.Type.COOLANT_CELL));
        maintainer.getItems().setStackInSlot(1, part(ReactorComponentItem.Type.COOLANT_CELL));
        helper.succeedWhen(() -> {
            ItemStack s0 = reactor.getItems().getStackInSlot(0);
            ItemStack s1 = reactor.getItems().getStackInSlot(1);
            helper.assertTrue(s0.getDamageValue() == 0 && s1.is(ModItems.REACTOR_PARTS.get(ReactorComponentItem.Type.COOLANT_CELL).get()),
                    "Celle non cambiate: " + s0 + " / " + s1);
        });
    }

    /** Nella fabbrica abbandonata c'e' ogni macchina importante */
    @GameTest(template = EMPTY)
    public static void factoryHasEveryMachine(GameTestHelper helper) {
        var all = AbandonedFactory.stations().values().stream().flatMap(java.util.List::stream).toList();
        for (Block b : new Block[]{ModBlocks.REACTOR.get(), ModBlocks.REACTOR_CHAMBER.get(), ModBlocks.QUARRY.get(),
                ModBlocks.LOGISTICS_CORE.get(), ModBlocks.ROBOT_DOCK.get(), ModBlocks.TREE_FARM.get(),
                ModBlocks.LASER_TURRET.get(), ModBlocks.REACTOR_MAINTAINER.get()}) {
            helper.assertTrue(all.contains(b), "Manca nella fabbrica: " + b);
        }
        helper.assertTrue(!all.contains(ModBlocks.NUCLEAR_TNT.get()), "La TNT nucleare NON deve stare nella fabbrica!");
        helper.succeed();
    }

    /** Rifugio portatile: costruisce la casa, consuma 1 uso, rotto il nucleo la casa sparisce e torna l'oggetto carico */
    @GameTest(template = EMPTY, timeoutTicks = 100, batch = "rifugio")
    public static void shelterBuildsAndDisappears(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos ground = helper.absolutePos(new BlockPos(0, 1, 0)).offset(-300, 0, 300);
        for (BlockPos p : BlockPos.betweenClosed(ground.offset(-5, 0, -5), ground.offset(5, 0, 5))) {
            level.setBlockAndUpdate(p, Blocks.STONE.defaultBlockState());
            for (int y = 1; y <= 6; y++) level.setBlockAndUpdate(p.above(y), Blocks.AIR.defaultBlockState());
        }
        level.setBlockAndUpdate(ground.above().east(), Blocks.SHORT_GRASS.defaultBlockState()); // l'erba sparisce
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack empty = new ItemStack(ModBlocks.SHELTER_CORE.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, empty);
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(ground),
                net.minecraft.core.Direction.UP, ground, false);
        // scarico: non costruisce
        empty.useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
        helper.assertTrue(level.getBlockState(ground.above()).isAir(), "Ha costruito senza energia");
        ItemStack full = new ItemStack(ModBlocks.SHELTER_CORE.get());
        int cap = Shelter.capacity();
        com.futuretek.item.EnergyItem.setEnergy(full, cap);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, full);
        full.useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
        BlockPos core = ground.above();
        helper.assertTrue(level.getBlockEntity(core) instanceof Shelter.CoreEntity, "Nucleo non piazzato");
        Shelter.CoreEntity be = (Shelter.CoreEntity) level.getBlockEntity(core);
        helper.assertTrue(be.energy() == cap - Shelter.cost(), "Energia non consumata: " + be.energy());
        int beds = 0, tables = 0, furnaces = 0, walls = 0, roofs = 0;
        for (BlockPos p : BlockPos.betweenClosed(core.offset(-3, 0, -3), core.offset(3, 3, 3))) {
            var st = level.getBlockState(p);
            if (st.is(net.minecraft.tags.BlockTags.BEDS)) beds++;
            if (st.is(Blocks.CRAFTING_TABLE)) tables++;
            if (st.is(Blocks.FURNACE)) furnaces++;
            if (st.is(ModBlocks.SHELTER_WALL.get())) walls++;
            if (st.is(ModBlocks.SHELTER_ROOF.get())) roofs++;
        }
        helper.assertTrue(beds == 2 && tables == 1 && furnaces == 1 && roofs == 49 && walls > 50,
                "Casa sbagliata: letti " + beds + " banchi " + tables + " fornaci " + furnaces + " muri " + walls + " tetto " + roofs);
        helper.assertTrue(level.getBlockState(ground.above().east()).isAir(), "L'erba dentro casa e' rimasta");
        level.destroyBlock(core, false);
        int left = 0;
        for (BlockPos p : BlockPos.betweenClosed(core.offset(-3, 0, -3), core.offset(3, 3, 3))) {
            if (!level.getBlockState(p).isAir()) left++;
        }
        helper.assertTrue(left == 0, "La casa non e' sparita: " + left + " blocchi");
        helper.assertTrue(level.getBlockState(ground).is(Blocks.STONE), "Il terreno e' stato toccato");
        var drops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(core).inflate(2));
        helper.assertTrue(drops.size() == 1 && drops.get(0).getItem().is(ModBlocks.SHELTER_CORE.get().asItem())
                && com.futuretek.item.EnergyItem.getEnergy(drops.get(0).getItem()) == cap - Shelter.cost(),
                "Il nucleo non e' tornato con la carica: " + drops.stream().map(d -> d.getItem() + "=" + com.futuretek.item.EnergyItem.getEnergy(d.getItem())).toList());
        drops.forEach(net.minecraft.world.entity.Entity::discard);
        helper.succeed();
    }

    /** Costruzione vera della fabbrica (lontano dalle altre prove): ci sono macchine, cavi e cartelli */
    @GameTest(template = EMPTY, timeoutTicks = 100, batch = "fabbrica")
    public static void factoryBuilds(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos corner = helper.absolutePos(new BlockPos(0, 1, 0)).offset(300, 0, 300);
        BlockPos entrance = AbandonedFactory.build(level, corner, net.minecraft.util.RandomSource.create(1));
        int cables = 0, signs = 0, reactors = 0;
        for (BlockPos p : BlockPos.betweenClosed(corner, entrance.offset(30, 3, 0))) {
            var s = level.getBlockState(p);
            if (s.getBlock() instanceof com.futuretek.block.CableBlock) cables++;
            if (s.is(Blocks.OAK_SIGN)) signs++;
            if (s.is(ModBlocks.REACTOR.get())) reactors++;
        }
        helper.assertTrue(cables > 50 && signs > 20 && reactors == 1,
                "Fabbrica: cavi " + cables + ", cartelli " + signs + ", reattori " + reactors);
        helper.succeed();
    }

    /** Ricercatore: legge un libro incantato e lo salva nell'archivio; l'incantatore poi mette quell'incantesimo */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void enchantResearchAndApply(GameTestHelper helper) {
        var regs = helper.getLevel().registryAccess();
        var fortune = regs.registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE);
        BlockPos r = new BlockPos(1, 2, 1);
        helper.setBlock(r, ModBlocks.ENCHANT_RESEARCHER.get());
        com.futuretek.automation.Enchanting.Researcher researcher = helper.getBlockEntity(r);
        researcher.getEnergy().setEnergy(100_000);
        researcher.getItems().setStackInSlot(0, new ItemStack(ModItems.ENCHANT_ARCHIVE.get()));
        ItemStack book = net.minecraft.world.item.EnchantedBookItem.createForEnchantment(
                new net.minecraft.world.item.enchantment.EnchantmentInstance(fortune, 3));
        researcher.getItems().setStackInSlot(1, book);
        helper.runAfterDelay(5, () -> {
            ItemStack archive = researcher.getItems().getStackInSlot(0);
            var known = com.futuretek.automation.Enchanting.Archive.known(archive);
            helper.assertTrue(known.getOrDefault(net.minecraft.resources.ResourceLocation.withDefaultNamespace("fortune"), 0) == 3,
                    "Fortuna III non salvata: " + known);
            helper.assertTrue(researcher.getItems().getStackInSlot(2).is(net.minecraft.world.item.Items.BOOK), "Il libro vuoto non e' uscito");
            // incantatore con l'archivio: piccone di ferro + Fortuna III
            BlockPos e = new BlockPos(4, 2, 4);
            helper.setBlock(e, ModBlocks.ELECTRIC_ENCHANTER.get());
            com.futuretek.automation.NetherMachines.Enchanter enchanter = helper.getBlockEntity(e);
            enchanter.getEnergy().setEnergy(250_000);
            enchanter.getItems().setStackInSlot(0, new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE));
            enchanter.getItems().setStackInSlot(1, new ItemStack(net.minecraft.world.item.Items.LAPIS_LAZULI, 5));
            enchanter.getItems().setStackInSlot(2, archive.copy());
            var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            player.experienceLevel = 20;
            helper.runAfterDelay(2, () -> {
                enchanter.onButton(player, com.futuretek.automation.NetherMachines.Enchanter.LEVEL_UP);
                enchanter.onButton(player, com.futuretek.automation.NetherMachines.Enchanter.APPLY);
                ItemStack pick = enchanter.getItems().getStackInSlot(0);
                helper.assertTrue(pick.getEnchantmentLevel(fortune) == 3, "Il piccone non ha Fortuna III: " + pick.getEnchantments());
                helper.assertTrue(player.experienceLevel == 11, "Livelli rimasti: " + player.experienceLevel);
                helper.succeed();
            });
        });
    }
}
