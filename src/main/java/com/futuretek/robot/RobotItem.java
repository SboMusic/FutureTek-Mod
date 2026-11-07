package com.futuretek.robot;

import com.futuretek.registry.ModItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Trasforma un robot in oggetto (e viceversa) conservando energia e lavoro */
public final class RobotItem {
    private RobotItem() {
    }

    public static ItemStack fromRobot(RobotEntity robot) {
        ItemStack stack = new ItemStack(ModItems.ROBOT.get());
        CompoundTag tag = new CompoundTag();
        tag.putInt("Energy", robot.getEnergy());
        tag.putInt("Mode", robot.getMode());
        tag.putBoolean("Infinite", robot.isInfinite());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }
}
