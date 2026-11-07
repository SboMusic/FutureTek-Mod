package com.futuretek.blockentity;

import java.util.Optional;

import javax.annotation.Nullable;

import com.futuretek.block.ProcessingMachineBlock;
import com.futuretek.energy.EnergyTier;
import com.futuretek.item.UpgradeItem;
import com.futuretek.machine.ProcessingMachine;
import com.futuretek.menu.ProcessingMachineMenu;
import com.futuretek.recipe.ProcessingRecipe;
import com.futuretek.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.FluidTags;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Il "cervello" comune di tutte le macchine di lavorazione.
 * Slot: 0 = ingresso, 1 = prodotto, 2 = prodotto extra (a probabilita'), 3-6 = potenziamenti.
 * Ogni tick: cerca la ricetta per l'oggetto in ingresso, consuma energia, avanza;
 * a fine lavorazione toglie l'ingrediente e aggiunge i prodotti.
 */
public class ProcessingMachineBlockEntity extends MachineBlockEntity implements MenuProvider {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_SECONDARY = 2;
    public static final int FIRST_UPGRADE = 3;
    public static final int UPGRADE_SLOTS = 4;
    public static final int BASE_CAPACITY = 10_000;

    private final ProcessingMachine machine;

    private final ItemStackHandler items = new ItemStackHandler(FIRST_UPGRADE + UPGRADE_SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot >= FIRST_UPGRADE) {
                return stack.getItem() instanceof UpgradeItem;
            }
            return slot == SLOT_INPUT;
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot >= FIRST_UPGRADE) {
                return 4;
            }
            return super.getSlotLimit(slot);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (slot >= FIRST_UPGRADE) {
                refreshUpgrades();
            }
        }
    };

    /** Vista per tubi e tramogge: si inserisce solo nell'ingresso, si estrae solo dai prodotti */
    private final IItemHandler automationItems = new IItemHandler() {
        @Override
        public int getSlots() {
            return 3;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == SLOT_INPUT ? items.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == SLOT_INPUT ? ItemStack.EMPTY : items.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_INPUT;
        }
    };

    public static final int TANK_CAPACITY = 8000;

    /** Serbatoio d'acqua (usato solo dalle macchine che ne hanno bisogno, es. bagno chimico) */
    private final FluidTank tank = new FluidTank(TANK_CAPACITY, fluid -> fluid.is(FluidTags.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private int progress;
    private int maxProgress;
    /** Valori calcolati dai potenziamenti */
    private int overclocks;
    private int efficiencies;
    private boolean ejector;

    private final ContainerData data = new MachineData() {
        @Override
        protected int getExtra(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> tank.getFluidAmount();
                case 3 -> machine.usesWater() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public int getCount() {
            return 8;
        }
    };

    public ProcessingMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PROCESSING_MACHINE.get(), pos, state, BASE_CAPACITY, EnergyTier.MEDIUM.rate, 0);
        this.machine = ((ProcessingMachineBlock) state.getBlock()).getMachine();
    }

    public ProcessingMachine getMachine() {
        return machine;
    }

    // --- Potenziamenti ---
    private int countUpgrades(UpgradeItem.Type type) {
        int count = 0;
        for (int i = FIRST_UPGRADE; i < FIRST_UPGRADE + UPGRADE_SLOTS; i++) {
            ItemStack stack = items.getStackInSlot(i);
            if (stack.getItem() instanceof UpgradeItem upgrade && upgrade.getType() == type) {
                count += stack.getCount();
            }
        }
        return Math.min(count, type.maxPerMachine);
    }

    private void refreshUpgrades() {
        overclocks = countUpgrades(UpgradeItem.Type.OVERCLOCK);
        efficiencies = countUpgrades(UpgradeItem.Type.EFFICIENCY);
        ejector = countUpgrades(UpgradeItem.Type.EJECTOR) > 0;
        int capacity = BASE_CAPACITY + 10_000 * countUpgrades(UpgradeItem.Type.ENERGY_STORAGE);
        energy.setCapacity(capacity);
    }

    /** Durata con gli overclock: ognuno aggiunge +50% di velocita' */
    private int effectiveTime(int baseTime) {
        return Math.max(1, (int) Math.round(baseTime / (1.0 + 0.5 * overclocks)));
    }

    /** Consumo per tick: +60% per overclock, -20% per efficienza */
    public int effectiveEnergyPerTick() {
        double value = machine.energyPerTick * (1.0 + 0.6 * overclocks) * Math.pow(0.8, efficiencies);
        return Math.max(1, (int) Math.round(value));
    }

    // --- Ricette ---
    /** Risultato pronto per una lavorazione: ingrediente da consumare, prodotti, durata */
    private record Job(int inputCount, ItemStack result, Optional<ProcessingRecipe.ChanceOutput> secondary, int time) {
    }

    @Nullable
    private Job findJob() {
        ItemStack input = items.getStackInSlot(SLOT_INPUT);
        if (input.isEmpty() || level == null) {
            return null;
        }
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        if (machine.kind == null) {
            // forno elettrico: ricette della fornace di Minecraft
            Optional<RecipeHolder<net.minecraft.world.item.crafting.SmeltingRecipe>> smelting = level.getRecipeManager()
                    .getRecipeFor(RecipeType.SMELTING, recipeInput, level);
            return smelting.map(holder -> new Job(1, holder.value().assemble(recipeInput, level.registryAccess()),
                    Optional.empty(), machine.time)).orElse(null);
        }
        Optional<RecipeHolder<ProcessingRecipe>> recipe = level.getRecipeManager()
                .getRecipeFor(machine.kind.type(), recipeInput, level);
        return recipe.map(holder -> {
            ProcessingRecipe r = holder.value();
            int time = r.getTime() > 0 ? r.getTime() : machine.time;
            return new Job(r.getIngredient().count(), r.assemble(recipeInput, level.registryAccess()),
                    r.getSecondary(), time);
        }).orElse(null);
    }

    private boolean fits(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        ItemStack current = items.getStackInSlot(slot);
        if (current.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(current, stack)
                && current.getCount() + stack.getCount() <= current.getMaxStackSize();
    }

    @Override
    public void serverTick() {
        Job job = findJob();
        boolean working = false;
        if (job != null && items.getStackInSlot(SLOT_INPUT).getCount() >= job.inputCount()
                && tank.getFluidAmount() >= machine.waterPerOperation
                && fits(SLOT_OUTPUT, job.result())
                && fits(SLOT_SECONDARY, job.secondary().map(ProcessingRecipe.ChanceOutput::item).orElse(ItemStack.EMPTY))) {
            maxProgress = effectiveTime(job.time());
            int cost = effectiveEnergyPerTick();
            if (energy.getEnergyStored() >= cost) {
                energy.consume(cost);
                progress++;
                working = true;
                if (progress >= maxProgress) {
                    finish(job);
                    progress = 0;
                }
            }
        } else {
            progress = 0;
            maxProgress = 0;
        }
        setLit(working);
        if (ejector && level.getGameTime() % 20 == 0) {
            eject();
        }
    }

    private void finish(Job job) {
        items.extractItem(SLOT_INPUT, job.inputCount(), false);
        if (machine.usesWater()) {
            tank.drain(machine.waterPerOperation, IFluidHandler.FluidAction.EXECUTE);
        }
        addToSlot(SLOT_OUTPUT, job.result());
        job.secondary().ifPresent(extra -> {
            if (level.random.nextFloat() < extra.chance()) {
                addToSlot(SLOT_SECONDARY, extra.item());
            }
        });
    }

    /** Aggiunge un prodotto in uno slot di uscita (gli slot di uscita rifiutano gli inserimenti "normali") */
    private void addToSlot(int slot, ItemStack stack) {
        ItemStack current = items.getStackInSlot(slot);
        if (current.isEmpty()) {
            items.setStackInSlot(slot, stack.copy());
        } else {
            items.setStackInSlot(slot, current.copyWithCount(current.getCount() + stack.getCount()));
        }
    }

    /** Potenziamento "espulsione": sposta i prodotti nei contenitori vicini */
    private void eject() {
        for (Direction dir : Direction.values()) {
            IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.relative(dir),
                    dir.getOpposite());
            if (target == null) {
                continue;
            }
            for (int slot : new int[] {SLOT_OUTPUT, SLOT_SECONDARY}) {
                ItemStack stack = items.extractItem(slot, 64, true);
                if (stack.isEmpty()) {
                    continue;
                }
                ItemStack rest = ItemHandlerHelper.insertItemStacked(target, stack, false);
                items.extractItem(slot, stack.getCount() - rest.getCount(), false);
            }
        }
    }

    @Override
    public ItemStackHandler getItems() {
        return items;
    }

    public IItemHandler getAutomationItems() {
        return automationItems;
    }

    /** Serbatoio per tubi e secchi (null se la macchina non usa acqua) */
    @Nullable
    public FluidTank getTank() {
        return machine.usesWater() ? tank : null;
    }

    public int getProgress() {
        return progress;
    }

    // --- Salvataggio ---
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Progress", progress);
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        // prima gli oggetti (per sapere la capacita' dei potenziamenti), poi l'energia
        if (tag.contains("Items")) {
            items.deserializeNBT(registries, tag.getCompound("Items"));
        }
        refreshUpgrades();
        super.loadAdditional(tag, registries);
        progress = tag.getInt("Progress");
        tank.readFromNBT(registries, tag.getCompound("Tank"));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        refreshUpgrades();
    }

    // --- Interfaccia ---
    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ProcessingMachineMenu(id, inventory, worldPosition, items, data);
    }
}
