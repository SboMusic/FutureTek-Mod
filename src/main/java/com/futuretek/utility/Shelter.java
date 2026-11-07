package com.futuretek.utility;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.futuretek.FutureTekConfig;
import com.futuretek.energy.EnergyUtil;
import com.futuretek.item.EnergyItem;
import com.futuretek.registry.ModBlockEntities;
import com.futuretek.registry.ModBlocks;
import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/**
 * Rifugio portatile: il nucleo costruisce una casetta 7x7 intorno a sé
 * (letto, banco da lavoro, fornace, porta, finestre). Se il nucleo viene rotto
 * la casa sparisce e il nucleo torna oggetto con la carica rimasta.
 * Ogni casa costa energia: con la carica piena si costruisce 6 volte (configurabile).
 */
public final class Shelter {
    public static final int R = 3;       // mezza larghezza: 7x7
    public static final int WALL_H = 3;  // muri alti 3, tetto al 4° blocco

    private Shelter() {
    }

    public static int cost() {
        try {
            return FutureTekConfig.SHELTER_ENERGY_PER_USE.get();
        } catch (IllegalStateException e) {
            return 25_000;
        }
    }

    public static int uses() {
        try {
            return FutureTekConfig.SHELTER_USES.get();
        } catch (IllegalStateException e) {
            return 6;
        }
    }

    public static int capacity() {
        return Math.max(1, cost() * uses());
    }

    /** Coordinate "locali": +z = verso la porta (verso il giocatore), +x = a destra */
    static BlockPos at(BlockPos core, Direction front, int x, int y, int z) {
        return core.relative(front.getClockWise(), x).relative(front, z).above(y);
    }

    static boolean freeForHouse(BlockState state) {
        return state.canBeReplaced() || state.is(BlockTags.LEAVES);
    }

    /** null se c'è posto, altrimenti la chiave del messaggio d'errore */
    @Nullable
    public static String check(Level level, BlockPos core, Direction front) {
        if (!level.isInWorldBounds(core.below()) || !level.isInWorldBounds(core.above(WALL_H))) {
            return "message.futuretek.shelter_no_space";
        }
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                for (int y = 0; y <= WALL_H; y++) {
                    BlockPos p = at(core, front, x, y, z);
                    if (p.equals(core)) {
                        continue;
                    }
                    if (!freeForHouse(level.getBlockState(p))) {
                        return "message.futuretek.shelter_no_space";
                    }
                    boolean shell = y == WALL_H || Math.abs(x) == R || Math.abs(z) == R;
                    if (shell && !level.getEntitiesOfClass(LivingEntity.class, new AABB(p)).isEmpty()) {
                        return "message.futuretek.shelter_in_the_way";
                    }
                }
            }
        }
        return null;
    }

    /** Blocchi che la casa può aver messo (solo questi vengono tolti quando sparisce) */
    static boolean isHousePart(BlockState s) {
        return s.is(ModBlocks.SHELTER_WALL.get()) || s.is(ModBlocks.SHELTER_ROOF.get())
                || s.is(ModBlocks.SHELTER_WINDOW.get()) || s.is(BlockTags.BEDS) || s.is(Blocks.CRAFTING_TABLE)
                || s.is(Blocks.FURNACE) || s.getBlock() instanceof DoorBlock;
    }

    /** Il nucleo che "possiede" quel blocco, cercato intorno (null se nessuno) */
    @Nullable
    static CoreEntity ownerOf(Level level, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2 * R, -WALL_H - 1, -2 * R), pos.offset(2 * R, 1, 2 * R))) {
            if (level.isLoaded(p) && level.getBlockEntity(p) instanceof CoreEntity core && core.owns(pos)) {
                return core;
            }
        }
        return null;
    }

    /** Nessuno può prendersi letto, banco, fornace o porta della casa: si rompe il nucleo */
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide || event.getPlayer().isCreative()) {
            return;
        }
        BlockState s = event.getState();
        if (s.is(BlockTags.BEDS) || s.is(Blocks.CRAFTING_TABLE) || s.is(Blocks.FURNACE) || s.getBlock() instanceof DoorBlock) {
            if (ownerOf(level, event.getPos()) != null) {
                event.setCanceled(true);
                event.getPlayer().displayClientMessage(
                        Component.translatable("message.futuretek.shelter_part").withStyle(ChatFormatting.YELLOW), true);
            }
        }
    }

    public static void onExplosion(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        event.getAffectedBlocks().removeIf(p -> {
            BlockState s = level.getBlockState(p);
            return (s.is(BlockTags.BEDS) || s.is(Blocks.CRAFTING_TABLE) || s.is(Blocks.FURNACE)
                    || s.getBlock() instanceof DoorBlock) && ownerOf(level, p) != null;
        });
    }

    // ---------------------------------------------------------------------------------------------
    public static class CoreBlock extends BaseEntityBlock {
        public CoreBlock(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return simpleCodec(CoreBlock::new);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new CoreEntity(pos, state);
        }

        @Override
        public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
            if (player.isCreative() && level.getBlockEntity(pos) instanceof CoreEntity core) {
                core.noDrop = true;
            }
            return super.playerWillDestroy(level, pos, state, player);
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
            if (!state.is(newState.getBlock()) && !level.isClientSide
                    && level.getBlockEntity(pos) instanceof CoreEntity core) {
                core.dissolve();
                if (!core.noDrop) {
                    ItemStack item = new ItemStack(this);
                    EnergyItem.setEnergy(item, core.energy);
                    Block.popResource(level, pos, item);
                }
            }
            super.onRemove(state, level, pos, newState, moved);
        }
    }

    public static class CoreEntity extends BlockEntity {
        int energy;
        boolean noDrop;
        private final List<BlockPos> parts = new ArrayList<>();

        public CoreEntity(BlockPos pos, BlockState state) {
            super(ModBlockEntities.SHELTER_CORE.get(), pos, state);
        }

        public int energy() {
            return energy;
        }

        public List<BlockPos> parts() {
            return parts;
        }

        boolean owns(BlockPos p) {
            return parts.contains(p);
        }

        private void put(BlockPos p, BlockState s) {
            level.setBlock(p, s, Block.UPDATE_ALL);
            parts.add(p.immutable());
        }

        /** Costruisce la casa; la porta guarda verso "front" */
        public void build(Direction front) {
            if (level == null || level.isClientSide) {
                return;
            }
            BlockPos c = worldPosition;
            BlockState wall = ModBlocks.SHELTER_WALL.get().defaultBlockState();
            BlockState roof = ModBlocks.SHELTER_ROOF.get().defaultBlockState();
            BlockState window = ModBlocks.SHELTER_WINDOW.get().defaultBlockState();
            for (int x = -R; x <= R; x++) {
                for (int z = -R; z <= R; z++) {
                    // pavimento solo dove manca il terreno (non si rovina il mondo)
                    BlockPos floor = at(c, front, x, -1, z);
                    if (freeForHouse(level.getBlockState(floor))) {
                        put(floor, wall);
                    }
                    put(at(c, front, x, WALL_H, z), roof);
                    if (Math.abs(x) != R && Math.abs(z) != R) {
                        continue;
                    }
                    for (int y = 0; y < WALL_H; y++) {
                        if (x == 0 && z == R && y < 2) {
                            continue; // porta
                        }
                        boolean win = y == 1 && ((Math.abs(x) == R && z == 0) || (x == 0 && z == -R));
                        put(at(c, front, x, y, z), win ? window : wall);
                    }
                }
            }
            for (int x = -R + 1; x < R; x++) {           // via erba e fiori dall'interno
                for (int z = -R + 1; z < R; z++) {
                    for (int y = 0; y < WALL_H; y++) {
                        BlockPos p = at(c, front, x, y, z);
                        if (!p.equals(c) && !level.getBlockState(p).isAir()) {
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                        }
                    }
                }
            }
            Direction back = front.getOpposite();
            // letto nell'angolo in fondo a sinistra (testa contro il muro)
            put(at(c, front, -2, 0, -1), Blocks.RED_BED.defaultBlockState()
                    .setValue(BedBlock.FACING, back).setValue(BedBlock.PART, BedPart.FOOT));
            put(at(c, front, -2, 0, -2), Blocks.RED_BED.defaultBlockState()
                    .setValue(BedBlock.FACING, back).setValue(BedBlock.PART, BedPart.HEAD));
            put(at(c, front, 2, 0, -2), Blocks.CRAFTING_TABLE.defaultBlockState());
            put(at(c, front, 1, 0, -2), Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, front));
            BlockState door = Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING, back)
                    .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT).setValue(DoorBlock.OPEN, false);
            put(at(c, front, 0, 0, R), door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
            put(at(c, front, 0, 1, R), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
            setChanged();
        }

        /** Toglie la casa (il contenuto della fornace cade per terra) */
        public void dissolve() {
            if (level == null || level.isClientSide) {
                return;
            }
            List<BlockPos> copy = new ArrayList<>(parts);
            parts.clear();
            for (BlockPos p : copy) {
                BlockState s = level.getBlockState(p);
                if (!isHousePart(s)) {
                    continue;
                }
                if (level.getBlockEntity(p) instanceof Container container) {
                    Containers.dropContents(level, p, container);
                }
                level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
            for (BlockPos p : copy) {   // aggiorna i vicini (luce, acqua...) dopo aver tolto tutto
                level.blockUpdated(p, Blocks.AIR);
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putInt("Energy", energy);
            tag.putLongArray("Parts", parts.stream().mapToLong(BlockPos::asLong).toArray());
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            energy = tag.getInt("Energy");
            parts.clear();
            for (long l : tag.getLongArray("Parts")) {
                parts.add(BlockPos.of(l));
            }
        }
    }

    /** Il nucleo come oggetto: si ricarica come le celle di energia */
    public static class CoreItem extends BlockItem implements EnergyItem {
        public CoreItem(Block block, Properties properties) {
            super(block, properties.stacksTo(1));
        }

        @Override
        public int getCapacity() {
            return capacity();
        }

        @Override
        public int getRate() {
            return 2048;
        }

        @Override
        public InteractionResult place(BlockPlaceContext ctx) {
            Level level = ctx.getLevel();
            if (!(level instanceof ServerLevel)) {
                return InteractionResult.SUCCESS;
            }
            Player player = ctx.getPlayer();
            boolean creative = player != null && player.getAbilities().instabuild;
            ItemStack stack = ctx.getItemInHand();
            int energy = EnergyItem.getEnergy(stack);
            int cost = cost();
            if (!creative && energy < cost) {
                tell(player, Component.translatable("message.futuretek.shelter_no_energy",
                        EnergyUtil.format(cost)).withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
            BlockPos pos = ctx.getClickedPos();
            Direction front = ctx.getHorizontalDirection().getOpposite();
            String problem = check(level, pos, front);
            if (problem != null) {
                tell(player, Component.translatable(problem).withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
            InteractionResult result = super.place(ctx);
            if (result.consumesAction() && level.getBlockEntity(pos) instanceof CoreEntity core) {
                core.energy = creative ? energy : energy - cost;
                core.build(front);
                tell(player, Component.translatable("message.futuretek.shelter_built",
                        cost > 0 ? core.energy / cost : uses()).withStyle(ChatFormatting.GREEN));
            }
            return result;
        }

        private static void tell(@Nullable Player player, Component msg) {
            if (player != null) {
                player.displayClientMessage(msg, true);
            }
        }

        @Override
        public boolean isBarVisible(ItemStack stack) {
            return true;
        }

        @Override
        public int getBarWidth(ItemStack stack) {
            return Math.round(13.0F * Math.min(EnergyItem.getEnergy(stack), capacity()) / capacity());
        }

        @Override
        public int getBarColor(ItemStack stack) {
            return 0xE8402A;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            int e = EnergyItem.getEnergy(stack);
            tooltip.add(Component.translatable("gui.futuretek.energy", EnergyUtil.format(e),
                    EnergyUtil.format(capacity())).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.futuretek.shelter_core.uses",
                    cost() > 0 ? e / cost() : uses(), uses()).withStyle(ChatFormatting.AQUA));
        }
    }
}
