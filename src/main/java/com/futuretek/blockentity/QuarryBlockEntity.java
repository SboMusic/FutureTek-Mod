package com.futuretek.blockentity;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.futuretek.FutureTekConfig;
import com.futuretek.block.ConduitBlock;
import com.futuretek.energy.EnergyTier;
import com.futuretek.logistics.ItemNetwork;
import com.futuretek.logistics.ItemNetworkManager;
import com.futuretek.mining.QuarryParts;
import com.futuretek.registry.ModBlockEntities;
import com.futuretek.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * Escavatore automatico (stile "quarry" classico).
 * AREA:
 *  - con i SEGNALINI: uno accanto all'escavatore e uno nell'angolo opposto (stessa altezza, max 64 blocchi):
 *    scava tutto il rettangolo dentro i due segnalini;
 *  - senza segnalini: un quadrato davanti a se' (lato = config quarrySize).
 * FUNZIONAMENTO:
 *  1. costruisce un TELAIO a barre sottili intorno all'area: un anello in basso, 4 colonne e un anello in alto;
 *  2. un CARRELLO scorre sull'anello in alto e cala la trivella su ogni blocco, strato per strato fino al fondo;
 *     (il carrello e la trivella sono disegnati dal client: vedi client/QuarryRenderer)
 *  3. cio' che scava va nei contenitori accanto (prima quello sopra) o nella rete di tubi;
 *     se non c'e' posto lo "sputa" fuori dall'alto, cosi' non si ferma mai.
 * VELOCITA': ogni blocco costa quarryEnergyPerBlock TU. Piu' energia arriva, piu' blocchi scava per tick
 * (fino a quarryMaxBlocksPerTick). La riserva interna e' piccola, quindi conta l'energia che entra.
 * Un segnale di redstone lo mette in pausa. Rompendolo spariscono anche telaio e trivella.
 * Non scava liquidi, aria, blocchi indistruttibili ne' blocchi con contenuto (bauli, macchine...).
 */
public class QuarryBlockEntity extends MachineBlockEntity {
    public static final int MARKER_RANGE = 64;
    public static final int FRAME_COST = 40;
    /** Altezza dell'anello alto del telaio sopra l'escavatore */
    public static final int FRAME_HEIGHT = 4;

    private boolean areaSet;
    private int minX, maxX, minZ, maxZ, topY;
    private int frameIndex;
    private int cursor;
    private int layerY = Integer.MIN_VALUE;
    private boolean finished;
    @Nullable private BlockPos drill;      // vecchia testa a blocco (solo mondi vecchi): viene tolta
    @Nullable private BlockPos target;     // ultimo blocco scavato (dove sta la trivella)
    private boolean dirty;

    // usati solo dal client per far scorrere il carrello in modo fluido
    public float renderX = Float.NaN, renderY, renderZ;

    /** Cosa sta facendo (si vede con click destro sull'escavatore) */
    public enum Status { WORKING, FRAME, NO_ENERGY, FULL, PAUSED, DONE, EJECTING }

    private Status status = Status.WORKING;

    public Status getStatus() {
        return finished ? Status.DONE : status;
    }

    public QuarryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.QUARRY.get(), pos, state, 4_000, EnergyTier.HIGH.rate, 0);
    }

    // --- area ---
    private void setupArea() {
        BlockPos m1 = null;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(worldPosition.relative(dir)).is(ModBlocks.AREA_MARKER.get())) {
                m1 = worldPosition.relative(dir);
                break;
            }
        }
        BlockPos m2 = m1 == null ? null : findOtherMarker(m1);
        if (m1 != null && m2 != null) {
            minX = Math.min(m1.getX(), m2.getX()) + 1;
            maxX = Math.max(m1.getX(), m2.getX()) - 1;
            minZ = Math.min(m1.getZ(), m2.getZ()) + 1;
            maxZ = Math.max(m1.getZ(), m2.getZ()) - 1;
            for (BlockPos m : new BlockPos[]{m1, m2}) {
                level.destroyBlock(m, true); // il segnalino torna al giocatore come oggetto
            }
        } else {
            int size = FutureTekConfig.QUARRY_SIZE.getAsInt();
            Direction front = getFacing();
            Direction side = front.getClockWise();
            BlockPos a = worldPosition.relative(front, 2).relative(side, -(size / 2));
            BlockPos b = a.relative(front, size - 1).relative(side, size - 1);
            minX = Math.min(a.getX(), b.getX());
            maxX = Math.max(a.getX(), b.getX());
            minZ = Math.min(a.getZ(), b.getZ());
            maxZ = Math.max(a.getZ(), b.getZ());
        }
        topY = worldPosition.getY();
        layerY = topY;
        cursor = 0;
        frameIndex = 0;
        areaSet = true;
        dirty = true;
        setChanged();
    }

    @Nullable
    private BlockPos findOtherMarker(BlockPos m1) {
        BlockPos best = null;
        for (int dx = -MARKER_RANGE; dx <= MARKER_RANGE; dx++) {
            for (int dz = -MARKER_RANGE; dz <= MARKER_RANGE; dz++) {
                if (Math.abs(dx) < 2 || Math.abs(dz) < 2) {
                    continue;
                }
                BlockPos p = m1.offset(dx, 0, dz);
                if (level.isLoaded(p) && level.getBlockState(p).is(ModBlocks.AREA_MARKER.get())
                        && (best == null || p.distSqr(m1) < best.distSqr(m1))) {
                    best = p;
                }
            }
        }
        return best;
    }

    /** Un anello del telaio (un blocco fuori dall'area) all'altezza y */
    private void ring(List<BlockPos> list, int y) {
        for (int x = minX - 1; x <= maxX + 1; x++) {
            list.add(new BlockPos(x, y, minZ - 1));
            list.add(new BlockPos(x, y, maxZ + 1));
        }
        for (int z = minZ; z <= maxZ; z++) {
            list.add(new BlockPos(minX - 1, y, z));
            list.add(new BlockPos(maxX + 1, y, z));
        }
    }

    /** I blocchi del telaio: anello in basso, 4 colonne agli angoli, anello in alto */
    public List<BlockPos> framePositions() {
        List<BlockPos> list = new ArrayList<>();
        ring(list, topY);
        for (int y = topY + 1; y < topY + FRAME_HEIGHT; y++) {
            list.add(new BlockPos(minX - 1, y, minZ - 1));
            list.add(new BlockPos(maxX + 1, y, minZ - 1));
            list.add(new BlockPos(minX - 1, y, maxZ + 1));
            list.add(new BlockPos(maxX + 1, y, maxZ + 1));
        }
        ring(list, topY + FRAME_HEIGHT);
        return list;
    }

    public int width() {
        return maxX - minX + 1;
    }

    public int length() {
        return maxZ - minZ + 1;
    }

    // dati per il disegno del carrello (client)
    public boolean hasGantry() {
        return areaSet && !finished && frameIndex >= framePositions().size() && target != null;
    }

    public int getMinX() { return minX; }
    public int getMaxX() { return maxX; }
    public int getMinZ() { return minZ; }
    public int getMaxZ() { return maxZ; }
    public int getTopY() { return topY; }
    @Nullable public BlockPos getTarget() { return target; }

    // --- funzionamento ---
    @Override
    public void serverTick() {
        if (finished) {
            return;
        }
        try {
            work();
        } finally {
            if (dirty) {
                dirty = false;
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private void work() {
        if (level.hasNeighborSignal(worldPosition)) {
            status = Status.PAUSED;
            setLit(false);
            return;
        }
        if (!areaSet) {
            setupArea();
        }
        // 1) telaio: 4 pezzi ogni 4 tick
        List<BlockPos> frame = framePositions();
        if (frameIndex < frame.size()) {
            if (level.getGameTime() % 4 != 0) {
                return;
            }
            for (int n = 0; n < 4 && frameIndex < frame.size(); n++) {
                if (energy.getEnergyStored() < FRAME_COST) {
                    status = Status.NO_ENERGY;
                    setLit(false);
                    return;
                }
                status = Status.FRAME;
                BlockPos p = frame.get(frameIndex++);
                BlockState there = level.getBlockState(p);
                if (there.isAir() || there.canBeReplaced() || there.is(ModBlocks.AREA_MARKER.get())) {
                    BlockState f = ModBlocks.QUARRY_FRAME.get().defaultBlockState();
                    level.setBlock(p, QuarryParts.Frame.connected(f, level, p), Block.UPDATE_ALL);
                    energy.consume(FRAME_COST);
                } else if (there.is(ModBlocks.QUARRY_FRAME.get())) {
                    level.setBlock(p, QuarryParts.Frame.connected(there, level, p), Block.UPDATE_ALL); // mondi vecchi
                }
            }
            removeDrill();
            setLit(true);
            setChanged();
            dirty |= frameIndex >= frame.size();
            return;
        }
        // 2) scavo: tanti blocchi quanti ne paga l'energia arrivata (fino al massimo per tick)
        int cost = Math.max(0, FutureTekConfig.QUARRY_ENERGY_PER_BLOCK.getAsInt());
        int max = FutureTekConfig.QUARRY_MAX_BLOCKS_PER_TICK.getAsInt();
        int done = 0;
        boolean ejected = false;
        while (done < max) {
            if (energy.getEnergyStored() < cost) {
                if (done == 0) {
                    status = Status.NO_ENERGY;
                    setLit(false);
                }
                break;
            }
            BlockPos next = nextTarget();
            if (next == null) {
                return; // finito
            }
            BlockState state = level.getBlockState(next);
            List<ItemStack> drops = Block.getDrops(state, (ServerLevel) level, next, null);
            for (ItemStack drop : drops) {
                ItemStack rest = insert(drop, false);
                if (!rest.isEmpty()) {
                    eject(rest);
                    ejected = true;
                }
            }
            level.destroyBlock(next, false);
            energy.consume(cost);
            target = next;
            cursor++;
            done++;
        }
        if (done > 0) {
            status = ejected ? Status.EJECTING : Status.WORKING;
            setLit(true);
            dirty = true;
            setChanged();
        }
    }

    /** Il prossimo blocco da scavare (salta aria, liquidi e blocchi intoccabili). null = finito */
    @Nullable
    private BlockPos nextTarget() {
        int w = width();
        int l = length();
        for (int tries = 0; tries < w * l * 4; tries++) {
            if (cursor >= w * l) {
                cursor = 0;
                layerY--;
                if (layerY < level.getMinBuildHeight()) {
                    finished = true;
                    target = null;
                    dirty = true;
                    setLit(false);
                    setChanged();
                    return null;
                }
            }
            int row = cursor / w;
            int col = row % 2 == 0 ? cursor % w : w - 1 - cursor % w; // a serpentina
            BlockPos p = new BlockPos(minX + col, layerY, minZ + row);
            if (canMine(p, level.getBlockState(p))) {
                return p;
            }
            cursor++;
        }
        return null; // strato pieno di blocchi da saltare: riprova al prossimo tick
    }

    /** Senza posto per gli oggetti, li butta fuori dall'alto dell'escavatore */
    private void eject(ItemStack stack) {
        ItemEntity item = new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1,
                worldPosition.getZ() + 0.5, stack.copy());
        item.setDeltaMovement((level.random.nextDouble() - 0.5) * 0.1, 0.25, (level.random.nextDouble() - 0.5) * 0.1);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }

    private void removeDrill() {
        if (drill != null && level.getBlockState(drill).is(ModBlocks.QUARRY_DRILL.get())) {
            level.setBlock(drill, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        drill = null;
    }

    /** Quando l'escavatore viene rotto spariscono anche il telaio e la vecchia testa */
    @Override
    public void dropContents(Level level, BlockPos pos) {
        super.dropContents(level, pos);
        removeDrill();
        if (areaSet) {
            for (BlockPos p : framePositions()) {
                if (level.getBlockState(p).is(ModBlocks.QUARRY_FRAME.get())) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private boolean canMine(BlockPos pos, BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && state.getDestroySpeed(level, pos) >= 0
                && level.getBlockEntity(pos) == null && !state.is(ModBlocks.QUARRY_FRAME.get())
                && !state.is(ModBlocks.QUARRY_DRILL.get()) && !pos.equals(worldPosition);
    }

    /** Ordine delle uscite: prima sopra, poi gli altri lati */
    private static final Direction[] OUTPUTS = {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST,
            Direction.WEST, Direction.DOWN};

    private ItemStack insert(ItemStack stack, boolean simulate) {
        ItemStack rest = stack.copy();
        for (Direction dir : OUTPUTS) {
            BlockPos next = worldPosition.relative(dir);
            if (level.getBlockState(next).getBlock() instanceof ConduitBlock) {
                ItemNetwork network = ItemNetworkManager.get(level, next);
                if (network != null) {
                    rest = network.insert(rest, worldPosition, simulate);
                }
            } else {
                IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, next, dir.getOpposite());
                if (handler != null) {
                    rest = ItemHandlerHelper.insertItemStacked(handler, rest, simulate);
                }
            }
            if (rest.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return rest;
    }

    public int getLayer() {
        return layerY;
    }

    public boolean isFinished() {
        return finished;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Cursor", cursor);
        tag.putInt("Layer", layerY);
        tag.putBoolean("Finished", finished);
        tag.putBoolean("AreaSet", areaSet);
        tag.putIntArray("Area", new int[]{minX, maxX, minZ, maxZ, topY});
        tag.putInt("Frame", frameIndex);
        if (drill != null) {
            tag.put("Drill", NbtUtils.writeBlockPos(drill));
        }
        if (target != null) {
            tag.put("Target", NbtUtils.writeBlockPos(target));
        }
    }

    // --- sincronizzazione col client (per disegnare il carrello) ---
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Finished", finished);
        tag.putBoolean("AreaSet", areaSet);
        tag.putIntArray("Area", new int[]{minX, maxX, minZ, maxZ, topY});
        tag.putInt("Frame", frameIndex);
        tag.putInt("Layer", layerY);
        if (target != null) {
            tag.put("Target", NbtUtils.writeBlockPos(target));
        }
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        cursor = tag.getInt("Cursor");
        layerY = tag.contains("Layer") ? tag.getInt("Layer") : Integer.MIN_VALUE;
        finished = tag.getBoolean("Finished");
        areaSet = tag.getBoolean("AreaSet");
        int[] a = tag.getIntArray("Area");
        if (a.length == 5) {
            minX = a[0];
            maxX = a[1];
            minZ = a[2];
            maxZ = a[3];
            topY = a[4];
        } else {
            areaSet = false;
        }
        frameIndex = tag.getInt("Frame");
        drill = NbtUtils.readBlockPos(tag, "Drill").orElse(null);
        target = NbtUtils.readBlockPos(tag, "Target").orElse(null);
    }
}
