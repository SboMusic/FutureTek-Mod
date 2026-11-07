package com.futuretek.rail;

import com.futuretek.registry.ModEntities;
import com.futuretek.registry.ModItems;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Carrello merci: un carrello con inventario grande (54 slot, come un baule doppio). */
public class CargoCart extends MinecartChest {
    /** Il carrello normale ha 36 slot: noi usiamo una lista nostra da 54 */
    private final NonNullList<ItemStack> bigStacks = NonNullList.withSize(54, ItemStack.EMPTY);

    public CargoCart(EntityType<? extends MinecartChest> type, Level level) {
        super(type, level);
    }

    public CargoCart(Level level, double x, double y, double z) {
        this(ModEntities.CARGO_CART.get(), level);
        setPos(x, y, z);
        xo = x;
        yo = y;
        zo = z;
    }

    @Override
    public NonNullList<ItemStack> getItemStacks() {
        return bigStacks;
    }

    @Override
    public void clearItemStacks() {
        bigStacks.clear();
    }

    @Override
    public int getContainerSize() {
        return 54;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return ChestMenu.sixRows(id, inventory, this);
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return Blocks.BARREL.defaultBlockState();
    }

    @Override
    protected Item getDropItem() {
        return ModItems.CARGO_CART.get();
    }
}
