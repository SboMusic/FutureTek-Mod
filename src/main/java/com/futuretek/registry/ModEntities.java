package com.futuretek.registry;

import com.futuretek.FutureTek;
import com.futuretek.rail.CargoCart;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Entita' di FutureTek. */
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, FutureTek.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<CargoCart>> CARGO_CART =
            ENTITIES.register("cargo_cart", () -> EntityType.Builder.<CargoCart>of(CargoCart::new, MobCategory.MISC)
                    .sized(0.98F, 0.7F).clientTrackingRange(8).build("cargo_cart"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.futuretek.robot.RobotEntity>> ROBOT =
            ENTITIES.register("robot", () -> EntityType.Builder.<com.futuretek.robot.RobotEntity>of(
                    com.futuretek.robot.RobotEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.7F).clientTrackingRange(10).build("robot"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.futuretek.nuclear.NukeBlock.Primed>> PRIMED_NUKE =
            ENTITIES.register("primed_nuke", () -> EntityType.Builder.<com.futuretek.nuclear.NukeBlock.Primed>of(
                    com.futuretek.nuclear.NukeBlock.Primed::new, MobCategory.MISC).fireImmune()
                    .sized(0.98F, 0.98F).clientTrackingRange(16).updateInterval(10).build("primed_nuke"));

    private ModEntities() {
    }
}
