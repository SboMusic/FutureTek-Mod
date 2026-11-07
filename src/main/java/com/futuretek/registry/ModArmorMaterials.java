package com.futuretek.registry;

import java.util.EnumMap;
import java.util.List;

import com.futuretek.FutureTek;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Materiali delle armature. La texture indossata e' in
 * assets/futuretek/textures/models/armor/NOME_layer_1.png (testa, corpo, braccia, piedi)
 * e NOME_layer_2.png (gambe).
 */
public final class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, FutureTek.MODID);

    /** Bronzo: protezione come il ferro */
    public static final Holder<ArmorMaterial> BRONZE = register("bronze", 2, 5, 6, 2, 12, 0.0F, "c:ingots/bronze");
    /** Acciaio: piu' protettivo del ferro, con un po' di "robustezza" */
    public static final Holder<ArmorMaterial> STEEL = register("steel", 2, 6, 7, 3, 10, 1.0F, "c:ingots/steel");

    /** Esoarmatura I: come il ferro */
    public static final Holder<ArmorMaterial> EXO_MK1 = register("exo_mk1", 2, 5, 6, 2, 12, 0.5F, "c:ingots/steel");
    /** Esoarmatura III: come la netherite */
    public static final Holder<ArmorMaterial> EXO_MK3 = register("exo_mk3", 3, 6, 8, 3, 18, 3.0F, "c:ingots/steel", 0.1F);
    /** Esoarmatura (II): come il diamante, con piu' robustezza */
    public static final Holder<ArmorMaterial> EXO = register("exo", 3, 6, 8, 3, 15, 2.5F, "c:ingots/steel");
    /** Zaino a reazione: protegge poco */
    public static final Holder<ArmorMaterial> JETPACK = register("jetpack", 0, 0, 3, 0, 5, 0.0F, "c:ingots/steel");

    /** Nocturnite e Ardorite (alchimia): piu' forti della netherite, non si riparano (sono indistruttibili) */
    public static final Holder<ArmorMaterial> NOCTURNITE = register("nocturnite", 3, 6, 8, 3, 18, 3.5F, "futuretek:nocturnite", 0.1F);
    public static final Holder<ArmorMaterial> ARDORITE = register("ardorite", 4, 7, 9, 4, 22, 5.0F, "futuretek:ardorite", 0.2F);

    private static Holder<ArmorMaterial> register(String name, int boots, int leggings, int chestplate, int helmet,
            int enchantability, float toughness, String repairTag) {
        return register(name, boots, leggings, chestplate, helmet, enchantability, toughness, repairTag, 0.0F);
    }

    private static Holder<ArmorMaterial> register(String name, int boots, int leggings, int chestplate, int helmet,
            int enchantability, float toughness, String repairTag, float knockbackResistance) {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.BOOTS, boots);
        defense.put(ArmorItem.Type.LEGGINGS, leggings);
        defense.put(ArmorItem.Type.CHESTPLATE, chestplate);
        defense.put(ArmorItem.Type.HELMET, helmet);
        defense.put(ArmorItem.Type.BODY, chestplate);
        return MATERIALS.register(name, () -> new ArmorMaterial(
                defense,
                enchantability,
                SoundEvents.ARMOR_EQUIP_IRON,
                () -> Ingredient.of(ItemTags.create(ResourceLocation.parse(repairTag))),
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(FutureTek.MODID, name))),
                toughness,
                knockbackResistance));
    }

    private ModArmorMaterials() {
    }
}
