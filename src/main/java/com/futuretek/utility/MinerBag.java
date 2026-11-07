package com.futuretek.utility;

import java.util.List;

import com.futuretek.FutureTek;
import com.futuretek.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

/**
 * Sacca del Minatore: 27 slot (il contenuto resta DENTRO la sacca, come in una shulker box).
 * - Click destro: apre la sacca.
 * - Shift + click destro: accende/spegne la raccolta automatica.
 * - Raccolta automatica: i minerali, i grezzi, le gemme, le polveri e il carbone che raccogli da terra
 *   finiscono direttamente nella sacca (elenco nel tag futuretek:miner_bag_collects, modificabile).
 */
public class MinerBag extends Item {
    public static final int SIZE = 27;
    public static final TagKey<Item> COLLECTS =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(FutureTek.MODID, "miner_bag_collects"));

    public MinerBag(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static boolean autoCollect(ItemStack bag) {
        return bag.getOrDefault(ModDataComponents.ACTIVE.get(), true);
    }

    public static NonNullList<ItemStack> load(ItemStack bag) {
        NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        return items;
    }

    public static void save(ItemStack bag, List<ItemStack> items) {
        bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }

    /** Mette "stack" nella sacca; restituisce cio' che non e' entrato */
    public static ItemStack insert(ItemStack bag, ItemStack stack) {
        NonNullList<ItemStack> items = load(bag);
        ItemStack rest = stack.copy();
        for (int pass = 0; pass < 2 && !rest.isEmpty(); pass++) {
            for (int i = 0; i < SIZE && !rest.isEmpty(); i++) {
                ItemStack slot = items.get(i);
                if (pass == 0 && !slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, rest)) {
                    int moved = Math.min(rest.getCount(), slot.getMaxStackSize() - slot.getCount());
                    slot.grow(moved);
                    rest.shrink(moved);
                } else if (pass == 1 && slot.isEmpty()) {
                    items.set(i, rest.copy());
                    rest = ItemStack.EMPTY;
                }
            }
        }
        if (rest.getCount() != stack.getCount()) {
            save(bag, items);
        }
        return rest;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack bag = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                boolean now = !autoCollect(bag);
                bag.set(ModDataComponents.ACTIVE.get(), now);
                player.displayClientMessage(Component.translatable(now ? "message.futuretek.bag_auto_on"
                        : "message.futuretek.bag_auto_off"), true);
            } else {
                int locked = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : -1;
                player.openMenu(new SimpleMenuProvider((id, inv, p) -> new Menu(id, inv, bag, locked), bag.getHoverName()));
                level.playSound(null, player.blockPosition(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 0.8F, 1.0F);
            }
        }
        return InteractionResultHolder.sidedSuccess(bag, level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        long used = load(stack).stream().filter(s -> !s.isEmpty()).count();
        tooltip.add(Component.translatable("tooltip.futuretek.miner_bag").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.futuretek.miner_bag_slots", used, SIZE).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(autoCollect(stack) ? "tooltip.futuretek.bag_auto_on" : "tooltip.futuretek.bag_auto_off")
                .withStyle(autoCollect(stack) ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
    }

    /** Raccolta automatica: chiamato quando il giocatore tocca un oggetto a terra */
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        ItemEntity entity = event.getItemEntity();
        if (player.level().isClientSide || entity.hasPickUpDelay() || !entity.getItem().is(COLLECTS)) {
            return;
        }
        Inventory inventory = player.getInventory();
        ItemStack stack = entity.getItem();
        int before = stack.getCount();
        for (int i = 0; i < inventory.getContainerSize() && !stack.isEmpty(); i++) {
            ItemStack bag = inventory.getItem(i);
            if (bag.getItem() instanceof MinerBag && autoCollect(bag)) {
                stack = insert(bag, stack);
            }
        }
        if (stack.getCount() == before) {
            return;
        }
        player.take(entity, before - stack.getCount());
        player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F,
                (player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 1.4F + 2.0F);
        if (stack.isEmpty()) {
            entity.discard();
            event.setCanPickup(TriState.FALSE);
        } else {
            entity.setItem(stack);
        }
    }

    /**
     * Interfaccia della sacca: uguale a un baule da 27 slot (sul client usa il baule di Minecraft).
     * La sacca aperta e' bloccata nel suo slot, e dentro la sacca non si puo' mettere un'altra sacca.
     */
    public static class Menu extends AbstractContainerMenu {
        private final ItemStack bag;
        private final SimpleContainer container;

        public Menu(int id, Inventory inventory, ItemStack bag, int lockedSlot) {
            super(MenuType.GENERIC_9x3, id);
            this.bag = bag;
            this.container = new SimpleContainer(SIZE) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    save(bag, getItems());
                }
            };
            NonNullList<ItemStack> items = load(bag);
            for (int i = 0; i < SIZE; i++) {
                container.getItems().set(i, items.get(i));
            }
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    addSlot(new Slot(container, col + row * 9, 8 + col * 18, 18 + row * 18) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return !(stack.getItem() instanceof MinerBag);
                        }
                    });
                }
            }
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    addSlot(lockable(inventory, col + row * 9 + 9, 8 + col * 18, 85 + row * 18, lockedSlot));
                }
            }
            for (int col = 0; col < 9; col++) {
                addSlot(lockable(inventory, col, 8 + col * 18, 143, lockedSlot));
            }
        }

        private static Slot lockable(Container inventory, int index, int x, int y, int locked) {
            return new Slot(inventory, index, x, y) {
                @Override
                public boolean mayPickup(Player player) {
                    return index != locked;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return index != locked;
                }
            };
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            Slot slot = slots.get(index);
            if (!slot.hasItem() || !slot.mayPickup(player)) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = slot.getItem();
            ItemStack copy = stack.copy();
            if (index < SIZE) {
                if (!moveItemStackTo(stack, SIZE, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.getItem() instanceof MinerBag || !moveItemStackTo(stack, 0, SIZE, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            container.setChanged();
            return copy;
        }

        @Override
        public boolean stillValid(Player player) {
            return !bag.isEmpty() && (player.getMainHandItem() == bag || player.getOffhandItem() == bag);
        }
    }
}
