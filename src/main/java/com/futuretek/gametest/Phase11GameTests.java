package com.futuretek.gametest;

import com.futuretek.FutureTek;
import com.futuretek.item.ElectricTools;
import com.futuretek.item.EnergyItem;
import com.futuretek.item.ExoArmorItem;
import com.futuretek.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Fase 11: utensili, armi ed esoarmatura elettrici */
@GameTestHolder(FutureTek.MODID)
@PrefixGameTestTemplate(false)
public class Phase11GameTests {
    private static final String EMPTY = "empty";

    private static ItemStack charged(ItemStack stack) {
        EnergyItem.setEnergy(stack, ((EnergyItem) stack.getItem()).getCapacity());
        return stack;
    }

    /** I trapani funzionano solo carichi, hanno il livello giusto e consumano energia a ogni blocco */
    @GameTest(template = EMPTY)
    public static void drillsNeedEnergy(GameTestHelper helper) {
        ItemStack drill = charged(new ItemStack(ModItems.ELECTRIC_DRILL.get()));
        ItemStack diamond = charged(new ItemStack(ModItems.DIAMOND_DRILL.get()));
        BlockState iron = Blocks.IRON_ORE.defaultBlockState();
        BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        helper.assertTrue(drill.isCorrectToolForDrops(iron), "Il trapano carico deve raccogliere il ferro");
        helper.assertTrue(drill.getDestroySpeed(dirt) > 1, "Il trapano scava anche come una pala");
        helper.assertTrue(!drill.isCorrectToolForDrops(obsidian), "Il trapano di ferro non raccoglie l'ossidiana");
        helper.assertTrue(diamond.isCorrectToolForDrops(obsidian), "Il trapano di diamante raccoglie l'ossidiana");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        int before = EnergyItem.getEnergy(drill);
        drill.mineBlock(helper.getLevel(), Blocks.STONE.defaultBlockState(), pos, player);
        helper.assertTrue(EnergyItem.getEnergy(drill) == before - 50, "Energia consumata: " + (before - EnergyItem.getEnergy(drill)));
        ItemStack empty = new ItemStack(ModItems.ELECTRIC_DRILL.get());
        helper.assertTrue(!empty.isCorrectToolForDrops(iron) && empty.getDestroySpeed(iron) == 1.0F,
                "Il trapano scarico non deve funzionare");
        helper.assertTrue(empty.getCapability(Capabilities.EnergyStorage.ITEM) != null, "Il trapano si deve ricaricare");
        helper.succeed();
    }

    /** La lama al plasma consuma energia a ogni colpo; scarica fa pochissimo danno */
    @GameTest(template = EMPTY)
    public static void plasmaSaberUsesEnergy(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack saber = charged(new ItemStack(ModItems.PLASMA_SABER.get()));
        player.setItemInHand(InteractionHand.MAIN_HAND, saber);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        float hp = zombie.getHealth();
        zombie.hurt(helper.getLevel().damageSources().playerAttack(player), 13);
        helper.assertTrue(hp - zombie.getHealth() >= 10, "Danno da carica troppo basso: " + (hp - zombie.getHealth()));
        helper.assertTrue(EnergyItem.getEnergy(saber) == ElectricTools.PlasmaSaber.CAPACITY - ElectricTools.PlasmaSaber.COST,
                "Energia del colpo: " + EnergyItem.getEnergy(saber));
        EnergyItem.setEnergy(saber, 0);
        Zombie other = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 2, 3));
        float hp2 = other.getHealth();
        other.hurt(helper.getLevel().damageSources().playerAttack(player), 13);
        helper.assertTrue(hp2 - other.getHealth() <= ElectricTools.PlasmaSaber.EMPTY_DAMAGE + 0.01F,
                "Scarica fa troppo danno: " + (hp2 - other.getHealth()));
        helper.succeed();
    }

    /** Esoarmatura I/II/III: protezione come ferro/diamante/netherite, batterie sempre piu' grandi */
    @GameTest(template = EMPTY)
    public static void exoArmorTiers(GameTestHelper helper) {
        ExoArmorItem mk1 = ModItems.EXO_MK1.get(ArmorItem.Type.CHESTPLATE).get();
        ExoArmorItem mk2 = ModItems.EXO_CHESTPLATE.get();
        ExoArmorItem mk3 = ModItems.EXO_MK3.get(ArmorItem.Type.CHESTPLATE).get();
        helper.assertTrue(mk1.getDefense() == 6 && mk2.getDefense() == 8 && mk3.getDefense() == 8,
                "Protezione: " + mk1.getDefense() + "/" + mk2.getDefense() + "/" + mk3.getDefense());
        helper.assertTrue(mk3.getToughness() >= 3, "Il livello III deve essere robusto come la netherite");
        helper.assertTrue(mk1.getCapacity() < mk2.getCapacity() && mk2.getCapacity() < mk3.getCapacity(), "Capacita'");
        helper.assertTrue(mk1.tier == 1 && mk2.tier == 2 && mk3.tier == 3, "Livelli");
        helper.succeed();
    }
}
