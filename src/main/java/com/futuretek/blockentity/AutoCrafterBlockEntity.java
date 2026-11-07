package com.futuretek.blockentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.futuretek.logistics.ItemNetwork;
import com.futuretek.logistics.ItemNetworkManager;
import com.futuretek.menu.AutoCrafterMenu;
import com.futuretek.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Banco di assemblaggio automatico.
 * Si imposta una ricetta 3x3 (slot fantasma). Quando ha "lavori" in coda, ogni mezzo secondo
 * prende gli ingredienti dalla rete di condotti, fabbrica e rimette il risultato nella rete.
 * I lavori arrivano dal Terminale di rete ("fammi 64 cavi"), anche a catena tra piu' banchi.
 */
public class AutoCrafterBlockEntity extends BlockEntity implements MenuProvider, ItemNetworkManager.NetworkDevice {
    private final ItemStackHandler pattern = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            cachedRecipe = null;
            setChanged();
        }
    };
    private int queued;
    /** Scorta da tenere sempre pronta nella rete (0 = spenta): se ce n'e' di meno, fabbrica da solo */
    private int stock;
    public static final int STOCK_STEP = 8;
    public static final int STOCK_MAX = 4096;
    private Optional<RecipeHolder<CraftingRecipe>> cachedRecipe;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? Math.min(queued, Short.MAX_VALUE) : index == 1 ? stock : 0;
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public AutoCrafterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTO_CRAFTER.get(), pos, state);
    }

    public ItemStackHandler getPattern() {
        return pattern;
    }

    public int getQueued() {
        return queued;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int value) {
        stock = Math.max(0, Math.min(STOCK_MAX, value));
        setChanged();
    }

    public void enqueue(int crafts) {
        queued += crafts;
        setChanged();
    }

    private CraftingInput input() {
        List<ItemStack> items = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            items.add(pattern.getStackInSlot(i));
        }
        return CraftingInput.of(3, 3, items);
    }

    public Optional<RecipeHolder<CraftingRecipe>> recipe() {
        if (cachedRecipe == null) {
            cachedRecipe = level == null ? Optional.empty()
                    : level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input(), level);
        }
        return cachedRecipe;
    }

    /** Cosa produce una singola fabbricazione (vuoto se la ricetta non e' valida) */
    public ItemStack result() {
        return recipe().map(r -> r.value().assemble(input(), level.registryAccess())).orElse(ItemStack.EMPTY);
    }

    /** Ingredienti di una fabbricazione (uno per slot non vuoto del modello) */
    public List<ItemStack> ingredients() {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            if (!pattern.getStackInSlot(i).isEmpty()) {
                list.add(pattern.getStackInSlot(i).copyWithCount(1));
            }
        }
        return list;
    }

    public void serverTick() {
        // scorta automatica: ogni 2 secondi controlla quanti ce ne sono nella rete
        if (stock > 0 && queued <= 0 && level.getGameTime() % 40 == 0) {
            ItemNetwork network = ItemNetworkManager.getAdjacent(level, worldPosition);
            ItemStack result = result();
            if (network != null && !result.isEmpty()) {
                long have = network.count(result);
                if (have < stock) {
                    int crafts = (int) Math.min(16, (stock - have + result.getCount() - 1) / result.getCount());
                    enqueue(crafts);
                }
            }
        }
        if (queued <= 0 || level.getGameTime() % 10 != 0) {
            return;
        }
        ItemNetwork network = ItemNetworkManager.getAdjacent(level, worldPosition);
        ItemStack result = result();
        if (network == null || result.isEmpty()) {
            return;
        }
        // tutti gli ingredienti ci sono?
        List<ItemStack> needs = ingredients();
        for (ItemStack need : needs) {
            long required = needs.stream().filter(n -> ItemStack.isSameItemSameComponents(n, need)).count();
            if (network.count(need) < required) {
                return; // aspetta (magari un altro banco li sta fabbricando)
            }
        }
        // il risultato ci sta nella rete?
        if (!network.insert(result, null, true).isEmpty()) {
            return;
        }
        // preleva, fabbrica, restituisce gli avanzi (es. secchi, attrezzi)
        List<ItemStack> taken = new ArrayList<>();
        for (ItemStack need : needs) {
            taken.add(network.extractExact(need, 1, false));
        }
        CraftingInput real = input();
        NonNullList<ItemStack> remaining = recipe().get().value().getRemainingItems(real);
        network.insert(result.copy(), null, false);
        for (ItemStack rest : remaining) {
            if (!rest.isEmpty()) {
                network.insert(rest, null, false);
            }
        }
        queued--;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Pattern", pattern.serializeNBT(registries));
        tag.putInt("Queued", queued);
        tag.putInt("Stock", stock);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        pattern.deserializeNBT(registries, tag.getCompound("Pattern"));
        queued = tag.getInt("Queued");
        stock = tag.getInt("Stock");
        cachedRecipe = null;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AutoCrafterMenu(id, inventory, worldPosition, pattern, data, this);
    }
}
