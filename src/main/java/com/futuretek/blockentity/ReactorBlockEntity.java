package com.futuretek.blockentity;

import java.util.ArrayList;
import java.util.List;

import com.futuretek.FutureTekConfig;
import com.futuretek.energy.EnergyTier;
import com.futuretek.energy.EnergyUtil;
import com.futuretek.item.ReactorComponentItem;
import com.futuretek.menu.ReactorMenu;
import com.futuretek.nuclear.NuclearBlast;
import com.futuretek.registry.ModBlockEntities;
import com.futuretek.registry.ModBlocks;
import com.futuretek.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Nucleo del reattore a fissione (stile classico).
 * GRIGLIA: 6 righe; il nucleo da solo ha 3 colonne, ogni CAMERA DEL REATTORE attaccata al nucleo ne aggiunge una
 * (fino a 6 camere = 9 colonne = 54 caselle). Funziona solo con un segnale redstone (leva = acceso).
 * Ogni secondo:
 *  - ogni barra fa "impulsi" = sue barre + barre vicine (sopra/sotto/destra/sinistra): piu' sono vicine,
 *    piu' energia (5 TU/t per impulso) e MOLTO piu' calore (3 x impulsi^2);
 *  - il calore va ai dissipatori e alle celle di raffreddamento accanto alla barra, altrimenti allo scafo;
 *  - i dissipatori smaltiscono 12 calore/s; le CELLE DI RAFFREDDAMENTO assorbono e basta: quando sono piene
 *    diventano "esauste" e vanno cambiate (a mano o col Manutentore del reattore);
 *  - lo scafo si raffredda da solo di poco (1/s + 1 per ogni piastra);
 *  - con lo scafo al 100% il reattore ESPLODE (cratere nucleare). Si puo' disattivare nella config.
 */
public class ReactorBlockEntity extends MachineBlockEntity implements MenuProvider {
    public static final int COLS = 9;
    public static final int ROWS = 6;
    public static final int BASE_COLS = 3;
    public static final int BASE_MAX_HEAT = 10_000;

    private final ItemStackHandler grid = new ItemStackHandler(COLS * ROWS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof ReactorComponentItem && (slot % COLS) < columns;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private int heat;
    private int maxHeat = BASE_MAX_HEAT;
    private int output; // TU/t prodotte nell'ultimo secondo
    private boolean active;
    private int columns = BASE_COLS;
    private boolean warned;

    private final ContainerData data = new MachineData() {
        @Override
        protected int getExtra(int index) {
            return switch (index) {
                case 0 -> heat & 0xFFFF;
                case 1 -> heat >>> 16;
                case 2 -> maxHeat & 0xFFFF;
                case 3 -> maxHeat >>> 16;
                case 4 -> output;
                case 5 -> columns;
                case 6 -> active ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public int getCount() {
            return 11;
        }
    };

    public ReactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REACTOR.get(), pos, state, 1_000_000, 0, EnergyTier.EXTREME.rate);
    }

    public int getColumns() {
        return columns;
    }

    public boolean isActiveSlot(int slot) {
        return slot % COLS < columns;
    }

    private ReactorComponentItem.Type typeAt(int slot) {
        if (!isActiveSlot(slot)) {
            return null;
        }
        return grid.getStackInSlot(slot).getItem() instanceof ReactorComponentItem c ? c.getType() : null;
    }

    private int[] neighbors(int slot) {
        int col = slot % COLS;
        int row = slot / COLS;
        int[] result = new int[4];
        int n = 0;
        if (col > 0) result[n++] = slot - 1;
        if (col < columns - 1) result[n++] = slot + 1;
        if (row > 0) result[n++] = slot - COLS;
        if (row < ROWS - 1) result[n++] = slot + COLS;
        return java.util.Arrays.copyOf(result, n);
    }

    /** Le camere del reattore attaccate al nucleo */
    public List<BlockPos> chambers() {
        List<BlockPos> list = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            if (level.getBlockState(worldPosition.relative(dir)).is(ModBlocks.REACTOR_CHAMBER.get())) {
                list.add(worldPosition.relative(dir));
            }
        }
        return list;
    }

    /** Oggetto in cui si trasforma una cella di raffreddamento piena */
    public static Item spentOf(ReactorComponentItem.Type type) {
        return switch (type) {
            case COOLANT_CELL_30K -> ModItems.SPENT_COOLANT_CELL_30K.get();
            case COOLANT_CELL_60K -> ModItems.SPENT_COOLANT_CELL_60K.get();
            default -> ModItems.SPENT_COOLANT_CELL.get();
        };
    }

    /** Aggiunge calore a un componente; pieno: il dissipatore si fonde, la cella diventa esausta */
    private void heatComponent(int slot, int amount) {
        ItemStack stack = grid.getStackInSlot(slot);
        ReactorComponentItem.Type type = typeAt(slot);
        int newDamage = stack.getDamageValue() + amount;
        if (newDamage >= stack.getMaxDamage()) {
            grid.setStackInSlot(slot, type != null && type.isCoolant() ? new ItemStack(spentOf(type)) : ItemStack.EMPTY);
            heat += newDamage - stack.getMaxDamage(); // il calore in eccesso va allo scafo
        } else {
            ItemStack copy = stack.copy();
            copy.setDamageValue(Math.max(0, newDamage));
            grid.setStackInSlot(slot, copy);
        }
    }

    @Override
    public void serverTick() {
        if (level.getGameTime() % 20 == 0) {
            simulateSecond();
        }
        if (active) {
            energy.generate(output);
        }
        EnergyUtil.pushToNeighbors(level, worldPosition, energy, EnergyTier.EXTREME.rate);
        for (BlockPos chamber : chambers()) { // anche i cavi attaccati alle camere prendono energia
            EnergyUtil.pushToNeighbors(level, chamber, energy, EnergyTier.EXTREME.rate);
        }
    }

    /** Un secondo di simulazione: energia, calore, raffreddamento */
    public void simulateSecond() {
        active = level.hasNeighborSignal(worldPosition);
        int chambers = chambers().size();
        columns = Math.min(COLS, BASE_COLS + chambers);
        int platings = 0;
        int pulsesTotal = 0;
        for (int slot = 0; slot < grid.getSlots(); slot++) {
            ReactorComponentItem.Type type = typeAt(slot);
            if (type == ReactorComponentItem.Type.REACTOR_PLATING) {
                platings += grid.getStackInSlot(slot).getCount();
            }
            if (!active || type == null || !type.isFuel()) {
                continue;
            }
            int pulses = type.rods;
            for (int n : neighbors(slot)) {
                ReactorComponentItem.Type other = typeAt(n);
                if (other != null && other.isFuel()) {
                    pulses += other.rods;
                }
            }
            pulsesTotal += pulses * type.rods;
            // calore della barra: ai componenti vicini che lo accettano, altrimenti allo scafo
            int rodHeat = 3 * pulses * pulses * type.rods;
            List<Integer> sinks = new ArrayList<>();
            for (int n : neighbors(slot)) {
                ReactorComponentItem.Type other = typeAt(n);
                if (other != null && other.acceptsHeat()) {
                    sinks.add(n);
                }
            }
            if (sinks.isEmpty()) {
                heat += rodHeat;
            } else {
                for (int sink : sinks) {
                    heatComponent(sink, rodHeat / sinks.size());
                }
            }
            // consumo del combustibile
            ItemStack rod = grid.getStackInSlot(slot);
            if (rod.getDamageValue() + 1 >= rod.getMaxDamage()) {
                grid.setStackInSlot(slot, new ItemStack(ModItems.DEPLETED_FUEL_ROD.get(), type.rods));
            } else {
                ItemStack copy = rod.copy();
                copy.setDamageValue(rod.getDamageValue() + 1);
                grid.setStackInSlot(slot, copy);
            }
        }
        output = pulsesTotal * 5;
        // i dissipatori smaltiscono (le celle di raffreddamento no: si riempiono e basta)
        for (int slot = 0; slot < grid.getSlots(); slot++) {
            if (typeAt(slot) == ReactorComponentItem.Type.HEAT_VENT) {
                heatComponent(slot, -12);
            }
        }
        // scafo
        maxHeat = BASE_MAX_HEAT + 1000 * platings;
        heat = Math.max(0, heat - 1 - platings);
        warnings();
        if (heat >= maxHeat) {
            meltdown();
            return;
        }
        setLit(active && output > 0);
        setChanged();
    }

    /** Sopra il 70%: allarme e fumo; sopra l'85%: fiamme intorno */
    private void warnings() {
        int pct = maxHeat > 0 ? heat * 100 / maxHeat : 0;
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (pct >= 70) {
            server.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 3.0F, 0.5F);
            server.sendParticles(ParticleTypes.LARGE_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1,
                    worldPosition.getZ() + 0.5, 6, 0.4, 0.2, 0.4, 0.02);
            if (!warned) {
                warned = true;
                for (Player player : level.players()) {
                    if (player.distanceToSqr(worldPosition.getCenter()) < 64 * 64) {
                        player.displayClientMessage(Component.translatable("message.futuretek.reactor_danger")
                                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD), false);
                    }
                }
            }
        } else {
            warned = false;
        }
        if (pct >= 85 && level.random.nextInt(3) == 0) {
            BlockPos p = worldPosition.offset(level.random.nextInt(5) - 2, level.random.nextInt(3) - 1, level.random.nextInt(5) - 2);
            if (level.getBlockState(p).isAir() && Blocks.FIRE.defaultBlockState().canSurvive(level, p)) {
                level.setBlock(p, Blocks.FIRE.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    private int rodCount() {
        int rods = 0;
        for (int i = 0; i < grid.getSlots(); i++) {
            ReactorComponentItem.Type type = typeAt(i);
            if (type != null && type.isFuel()) {
                rods += type.rods;
            }
        }
        return rods;
    }

    private void meltdown() {
        if (FutureTekConfig.REACTOR_EXPLOSIONS.getAsBoolean() && level instanceof ServerLevel server) {
            int radius = Math.min(FutureTekConfig.REACTOR_BLAST_RADIUS.getAsInt(), 8 + rodCount());
            for (int i = 0; i < grid.getSlots(); i++) {
                grid.setStackInSlot(i, ItemStack.EMPTY);
            }
            for (BlockPos chamber : chambers()) {
                level.removeBlock(chamber, false);
            }
            BlockPos center = worldPosition;
            level.removeBlock(worldPosition, false);
            NuclearBlast.start(server, center, radius, null);
            return;
        }
        // esplosioni disattivate nella config: spegnimento d'emergenza, le barre si distruggono
        for (int i = 0; i < grid.getSlots(); i++) {
            ReactorComponentItem.Type type = typeAt(i);
            if (type != null && type.isFuel()) {
                grid.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        heat = maxHeat / 2;
        for (Player player : level.players()) {
            if (player.distanceToSqr(worldPosition.getCenter()) < 32 * 32) {
                player.displayClientMessage(Component.translatable("message.futuretek.reactor_scram")
                        .withStyle(ChatFormatting.RED), false);
            }
        }
    }

    public int getHeat() {
        return heat;
    }

    public void setHeat(int value) {
        heat = value;
    }

    public int getMaxHeat() {
        return maxHeat;
    }

    public int getOutput() {
        return output;
    }

    @Override
    public ItemStackHandler getItems() {
        return grid;
    }

    /** Per tubi e tramogge: solo le caselle attive */
    public IItemHandler automation() {
        return grid;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Heat", heat);
        tag.putInt("Columns", columns);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        // i mondi vecchi avevano 27 caselle: le ricopio nelle stesse posizioni della griglia nuova
        if (tag.contains("Items") && tag.getCompound("Items").getInt("Size") < COLS * ROWS) {
            tag.getCompound("Items").putInt("Size", COLS * ROWS);
        }
        super.loadAdditional(tag, registries);
        heat = tag.getInt("Heat");
        columns = tag.contains("Columns") ? tag.getInt("Columns") : BASE_COLS;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ReactorMenu(id, inventory, worldPosition, grid, data);
    }
}
