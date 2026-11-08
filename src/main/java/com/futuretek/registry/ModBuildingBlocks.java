package com.futuretek.registry;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Blocchi da costruzione (ispirati ai blocchi decorativi di Tekkit Legends, con nomi e grafica nostri):
 * roccia vulcanica, marmo bianco, travertino, ardesia (con levigati, mattoni, piastrelle, cesellati,
 * scale, lastre, muretti, pulsanti, pedane) e 8 legni colorati (assi, tronchi, legno, scortecciati, scale, lastre,
 * staccionate, cancelli, porte, botole, pulsanti, pedane).
 * FILE GENERATO da tools/genera_java_costruzione.py partendo da tools/blocchi_costruzione.json: non modificarlo a mano.
 */
public final class ModBuildingBlocks {
    private ModBuildingBlocks() {
    }

    /** Basta chiamarlo una volta (dal costruttore della mod) per registrare tutto */
    public static void init() {
    }

    private static final Map<Block, Block> STRIP = new IdentityHashMap<>();

    /** L'ascia toglie la corteccia ai nostri tronchi (come con quelli di Minecraft) */
    public static void onToolUse(net.neoforged.neoforge.event.level.BlockEvent.BlockToolModificationEvent event) {
        if (event.getItemAbility() != net.neoforged.neoforge.common.ItemAbilities.AXE_STRIP) {
            return;
        }
        if (STRIP.isEmpty()) {
        STRIP.put(RUBY_WOOD.get(), STRIPPED_RUBY_WOOD.get());
        STRIP.put(RUBY_LOG.get(), STRIPPED_RUBY_LOG.get());
        STRIP.put(AMBER_WOOD.get(), STRIPPED_AMBER_WOOD.get());
        STRIP.put(AMBER_LOG.get(), STRIPPED_AMBER_LOG.get());
        STRIP.put(HONEY_WOOD.get(), STRIPPED_HONEY_WOOD.get());
        STRIP.put(HONEY_LOG.get(), STRIPPED_HONEY_LOG.get());
        STRIP.put(JADE_WOOD.get(), STRIPPED_JADE_WOOD.get());
        STRIP.put(JADE_LOG.get(), STRIPPED_JADE_LOG.get());
        STRIP.put(AZURE_WOOD.get(), STRIPPED_AZURE_WOOD.get());
        STRIP.put(AZURE_LOG.get(), STRIPPED_AZURE_LOG.get());
        STRIP.put(VIOLET_WOOD.get(), STRIPPED_VIOLET_WOOD.get());
        STRIP.put(VIOLET_LOG.get(), STRIPPED_VIOLET_LOG.get());
        STRIP.put(IVORY_WOOD.get(), STRIPPED_IVORY_WOOD.get());
        STRIP.put(IVORY_LOG.get(), STRIPPED_IVORY_LOG.get());
        STRIP.put(SHADOW_WOOD.get(), STRIPPED_SHADOW_WOOD.get());
        STRIP.put(SHADOW_LOG.get(), STRIPPED_SHADOW_LOG.get());
        }
        Block stripped = STRIP.get(event.getState().getBlock());
        if (stripped != null) {
            event.setFinalState(stripped.defaultBlockState().setValue(RotatedPillarBlock.AXIS,
                    event.getState().getValue(RotatedPillarBlock.AXIS)));
        }
    }

    private static BlockBehaviour.Properties button() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties stone() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties wood() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS)
                .sound(SoundType.WOOD).ignitedByLava();
    }

    private static DeferredBlock<Block> reg(String name, Function<BlockBehaviour.Properties, ? extends Block> factory,
            BlockBehaviour.Properties props) {
        DeferredBlock<Block> block = ModBlocks.BLOCKS.registerBlock(name, factory, props);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    public static final DeferredBlock<Block> VOLCANIC_ROCK = reg("volcanic_rock", p -> new Block(p), stone().strength(1.8F, 6.0F));
    public static final DeferredBlock<Block> COBBLED_VOLCANIC_ROCK = reg("cobbled_volcanic_rock", p -> new Block(p), stone().strength(1.8F, 6.0F));
    public static final DeferredBlock<Block> POLISHED_VOLCANIC_ROCK = reg("polished_volcanic_rock", p -> new Block(p), stone().strength(1.8F, 6.0F));
    public static final DeferredBlock<Block> VOLCANIC_ROCK_BRICKS = reg("volcanic_rock_bricks", p -> new Block(p), stone().strength(1.8F, 6.0F));
    public static final DeferredBlock<Block> CRACKED_VOLCANIC_ROCK_BRICKS = reg("cracked_volcanic_rock_bricks", p -> new Block(p), stone().strength(1.8F, 6.0F));
    public static final DeferredBlock<Block> VOLCANIC_ROCK_TILES = reg("volcanic_rock_tiles", p -> new Block(p), stone().strength(1.8F, 6.0F));
    public static final DeferredBlock<Block> CHISELED_VOLCANIC_ROCK = reg("chiseled_volcanic_rock", p -> new Block(p), stone().strength(1.8F, 6.0F));
    public static final DeferredBlock<Block> VOLCANIC_ROCK_BUTTON = reg("volcanic_rock_button", p -> new ButtonBlock(BlockSetType.STONE, 20, p), button().strength(0.5F).noCollission().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> VOLCANIC_ROCK_PRESSURE_PLATE = reg("volcanic_rock_pressure_plate", p -> new PressurePlateBlock(BlockSetType.STONE, p), stone().strength(0.5F, 0.5F).noCollission().forceSolidOn().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> VOLCANIC_ROCK_STAIRS = reg("volcanic_rock_stairs", p -> new StairBlock(VOLCANIC_ROCK.get().defaultBlockState(), p), stone().strength(1.8F, 6.0F));
    public static final DeferredBlock<Block> VOLCANIC_ROCK_SLAB = reg("volcanic_rock_slab", SlabBlock::new, stone().strength(1.8F, 6.0F));
    public static final DeferredBlock<Block> VOLCANIC_ROCK_WALL = reg("volcanic_rock_wall", WallBlock::new, stone().strength(1.8F, 6.0F).forceSolidOn());
    public static final DeferredBlock<Block> COBBLED_VOLCANIC_ROCK_STAIRS = reg("cobbled_volcanic_rock_stairs", p -> new StairBlock(COBBLED_VOLCANIC_ROCK.get().defaultBlockState(), p), stone().strength(1.8F, 6.0F));
    public static final DeferredBlock<Block> COBBLED_VOLCANIC_ROCK_SLAB = reg("cobbled_volcanic_rock_slab", SlabBlock::new, stone().strength(1.8F, 6.0F));
    public static final DeferredBlock<Block> COBBLED_VOLCANIC_ROCK_WALL = reg("cobbled_volcanic_rock_wall", WallBlock::new, stone().strength(1.8F, 6.0F).forceSolidOn());
    public static final DeferredBlock<Block> POLISHED_VOLCANIC_ROCK_STAIRS = reg("polished_volcanic_rock_stairs", p -> new StairBlock(POLISHED_VOLCANIC_ROCK.get().defaultBlockState(), p), stone().strength(1.8F, 6.0F));
    public static final DeferredBlock<Block> POLISHED_VOLCANIC_ROCK_SLAB = reg("polished_volcanic_rock_slab", SlabBlock::new, stone().strength(1.8F, 6.0F));
    public static final DeferredBlock<Block> POLISHED_VOLCANIC_ROCK_WALL = reg("polished_volcanic_rock_wall", WallBlock::new, stone().strength(1.8F, 6.0F).forceSolidOn());
    public static final DeferredBlock<Block> VOLCANIC_ROCK_BRICKS_STAIRS = reg("volcanic_rock_bricks_stairs", p -> new StairBlock(VOLCANIC_ROCK_BRICKS.get().defaultBlockState(), p), stone().strength(1.8F, 6.0F));
    public static final DeferredBlock<Block> VOLCANIC_ROCK_BRICKS_SLAB = reg("volcanic_rock_bricks_slab", SlabBlock::new, stone().strength(1.8F, 6.0F));
    public static final DeferredBlock<Block> VOLCANIC_ROCK_BRICKS_WALL = reg("volcanic_rock_bricks_wall", WallBlock::new, stone().strength(1.8F, 6.0F).forceSolidOn());
    public static final DeferredBlock<Block> VOLCANIC_ROCK_TILES_STAIRS = reg("volcanic_rock_tiles_stairs", p -> new StairBlock(VOLCANIC_ROCK_TILES.get().defaultBlockState(), p), stone().strength(1.8F, 6.0F));
    public static final DeferredBlock<Block> VOLCANIC_ROCK_TILES_SLAB = reg("volcanic_rock_tiles_slab", SlabBlock::new, stone().strength(1.8F, 6.0F));
    public static final DeferredBlock<Block> VOLCANIC_ROCK_TILES_WALL = reg("volcanic_rock_tiles_wall", WallBlock::new, stone().strength(1.8F, 6.0F).forceSolidOn());
    public static final DeferredBlock<Block> WHITE_MARBLE = reg("white_marble", p -> new Block(p), stone().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> COBBLED_WHITE_MARBLE = reg("cobbled_white_marble", p -> new Block(p), stone().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> POLISHED_WHITE_MARBLE = reg("polished_white_marble", p -> new Block(p), stone().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> WHITE_MARBLE_BRICKS = reg("white_marble_bricks", p -> new Block(p), stone().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> CRACKED_WHITE_MARBLE_BRICKS = reg("cracked_white_marble_bricks", p -> new Block(p), stone().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> WHITE_MARBLE_TILES = reg("white_marble_tiles", p -> new Block(p), stone().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> CHISELED_WHITE_MARBLE = reg("chiseled_white_marble", p -> new Block(p), stone().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> WHITE_MARBLE_BUTTON = reg("white_marble_button", p -> new ButtonBlock(BlockSetType.STONE, 20, p), button().strength(0.5F).noCollission().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> WHITE_MARBLE_PRESSURE_PLATE = reg("white_marble_pressure_plate", p -> new PressurePlateBlock(BlockSetType.STONE, p), stone().strength(0.5F, 0.5F).noCollission().forceSolidOn().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> WHITE_MARBLE_STAIRS = reg("white_marble_stairs", p -> new StairBlock(WHITE_MARBLE.get().defaultBlockState(), p), stone().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> WHITE_MARBLE_SLAB = reg("white_marble_slab", SlabBlock::new, stone().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> WHITE_MARBLE_WALL = reg("white_marble_wall", WallBlock::new, stone().strength(1.5F, 6.0F).forceSolidOn());
    public static final DeferredBlock<Block> COBBLED_WHITE_MARBLE_STAIRS = reg("cobbled_white_marble_stairs", p -> new StairBlock(COBBLED_WHITE_MARBLE.get().defaultBlockState(), p), stone().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> COBBLED_WHITE_MARBLE_SLAB = reg("cobbled_white_marble_slab", SlabBlock::new, stone().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> COBBLED_WHITE_MARBLE_WALL = reg("cobbled_white_marble_wall", WallBlock::new, stone().strength(1.5F, 6.0F).forceSolidOn());
    public static final DeferredBlock<Block> POLISHED_WHITE_MARBLE_STAIRS = reg("polished_white_marble_stairs", p -> new StairBlock(POLISHED_WHITE_MARBLE.get().defaultBlockState(), p), stone().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> POLISHED_WHITE_MARBLE_SLAB = reg("polished_white_marble_slab", SlabBlock::new, stone().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> POLISHED_WHITE_MARBLE_WALL = reg("polished_white_marble_wall", WallBlock::new, stone().strength(1.5F, 6.0F).forceSolidOn());
    public static final DeferredBlock<Block> WHITE_MARBLE_BRICKS_STAIRS = reg("white_marble_bricks_stairs", p -> new StairBlock(WHITE_MARBLE_BRICKS.get().defaultBlockState(), p), stone().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> WHITE_MARBLE_BRICKS_SLAB = reg("white_marble_bricks_slab", SlabBlock::new, stone().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> WHITE_MARBLE_BRICKS_WALL = reg("white_marble_bricks_wall", WallBlock::new, stone().strength(1.5F, 6.0F).forceSolidOn());
    public static final DeferredBlock<Block> WHITE_MARBLE_TILES_STAIRS = reg("white_marble_tiles_stairs", p -> new StairBlock(WHITE_MARBLE_TILES.get().defaultBlockState(), p), stone().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> WHITE_MARBLE_TILES_SLAB = reg("white_marble_tiles_slab", SlabBlock::new, stone().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> WHITE_MARBLE_TILES_WALL = reg("white_marble_tiles_wall", WallBlock::new, stone().strength(1.5F, 6.0F).forceSolidOn());
    public static final DeferredBlock<Block> TRAVERTINE = reg("travertine", p -> new Block(p), stone().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> COBBLED_TRAVERTINE = reg("cobbled_travertine", p -> new Block(p), stone().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> POLISHED_TRAVERTINE = reg("polished_travertine", p -> new Block(p), stone().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> TRAVERTINE_BRICKS = reg("travertine_bricks", p -> new Block(p), stone().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> CRACKED_TRAVERTINE_BRICKS = reg("cracked_travertine_bricks", p -> new Block(p), stone().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> TRAVERTINE_TILES = reg("travertine_tiles", p -> new Block(p), stone().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> CHISELED_TRAVERTINE = reg("chiseled_travertine", p -> new Block(p), stone().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> TRAVERTINE_BUTTON = reg("travertine_button", p -> new ButtonBlock(BlockSetType.STONE, 20, p), button().strength(0.5F).noCollission().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> TRAVERTINE_PRESSURE_PLATE = reg("travertine_pressure_plate", p -> new PressurePlateBlock(BlockSetType.STONE, p), stone().strength(0.5F, 0.5F).noCollission().forceSolidOn().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> TRAVERTINE_STAIRS = reg("travertine_stairs", p -> new StairBlock(TRAVERTINE.get().defaultBlockState(), p), stone().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> TRAVERTINE_SLAB = reg("travertine_slab", SlabBlock::new, stone().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> TRAVERTINE_WALL = reg("travertine_wall", WallBlock::new, stone().strength(1.5F, 6.0F).forceSolidOn());
    public static final DeferredBlock<Block> COBBLED_TRAVERTINE_STAIRS = reg("cobbled_travertine_stairs", p -> new StairBlock(COBBLED_TRAVERTINE.get().defaultBlockState(), p), stone().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> COBBLED_TRAVERTINE_SLAB = reg("cobbled_travertine_slab", SlabBlock::new, stone().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> COBBLED_TRAVERTINE_WALL = reg("cobbled_travertine_wall", WallBlock::new, stone().strength(1.5F, 6.0F).forceSolidOn());
    public static final DeferredBlock<Block> POLISHED_TRAVERTINE_STAIRS = reg("polished_travertine_stairs", p -> new StairBlock(POLISHED_TRAVERTINE.get().defaultBlockState(), p), stone().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> POLISHED_TRAVERTINE_SLAB = reg("polished_travertine_slab", SlabBlock::new, stone().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> POLISHED_TRAVERTINE_WALL = reg("polished_travertine_wall", WallBlock::new, stone().strength(1.5F, 6.0F).forceSolidOn());
    public static final DeferredBlock<Block> TRAVERTINE_BRICKS_STAIRS = reg("travertine_bricks_stairs", p -> new StairBlock(TRAVERTINE_BRICKS.get().defaultBlockState(), p), stone().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> TRAVERTINE_BRICKS_SLAB = reg("travertine_bricks_slab", SlabBlock::new, stone().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> TRAVERTINE_BRICKS_WALL = reg("travertine_bricks_wall", WallBlock::new, stone().strength(1.5F, 6.0F).forceSolidOn());
    public static final DeferredBlock<Block> TRAVERTINE_TILES_STAIRS = reg("travertine_tiles_stairs", p -> new StairBlock(TRAVERTINE_TILES.get().defaultBlockState(), p), stone().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> TRAVERTINE_TILES_SLAB = reg("travertine_tiles_slab", SlabBlock::new, stone().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> TRAVERTINE_TILES_WALL = reg("travertine_tiles_wall", WallBlock::new, stone().strength(1.5F, 6.0F).forceSolidOn());
    public static final DeferredBlock<Block> SLATE = reg("slate", p -> new Block(p), stone().strength(1.8F, 6.0F));
    public static final DeferredBlock<Block> COBBLED_SLATE = reg("cobbled_slate", p -> new Block(p), stone().strength(1.8F, 6.0F));
    public static final DeferredBlock<Block> POLISHED_SLATE = reg("polished_slate", p -> new Block(p), stone().strength(1.8F, 6.0F));
    public static final DeferredBlock<Block> SLATE_BRICKS = reg("slate_bricks", p -> new Block(p), stone().strength(1.8F, 6.0F));
    public static final DeferredBlock<Block> CRACKED_SLATE_BRICKS = reg("cracked_slate_bricks", p -> new Block(p), stone().strength(1.8F, 6.0F));
    public static final DeferredBlock<Block> SLATE_TILES = reg("slate_tiles", p -> new Block(p), stone().strength(1.8F, 6.0F));
    public static final DeferredBlock<Block> CHISELED_SLATE = reg("chiseled_slate", p -> new Block(p), stone().strength(1.8F, 6.0F));
    public static final DeferredBlock<Block> SLATE_BUTTON = reg("slate_button", p -> new ButtonBlock(BlockSetType.STONE, 20, p), button().strength(0.5F).noCollission().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> SLATE_PRESSURE_PLATE = reg("slate_pressure_plate", p -> new PressurePlateBlock(BlockSetType.STONE, p), stone().strength(0.5F, 0.5F).noCollission().forceSolidOn().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> SLATE_STAIRS = reg("slate_stairs", p -> new StairBlock(SLATE.get().defaultBlockState(), p), stone().strength(1.8F, 6.0F));
    public static final DeferredBlock<Block> SLATE_SLAB = reg("slate_slab", SlabBlock::new, stone().strength(1.8F, 6.0F));
    public static final DeferredBlock<Block> SLATE_WALL = reg("slate_wall", WallBlock::new, stone().strength(1.8F, 6.0F).forceSolidOn());
    public static final DeferredBlock<Block> COBBLED_SLATE_STAIRS = reg("cobbled_slate_stairs", p -> new StairBlock(COBBLED_SLATE.get().defaultBlockState(), p), stone().strength(1.8F, 6.0F));
    public static final DeferredBlock<Block> COBBLED_SLATE_SLAB = reg("cobbled_slate_slab", SlabBlock::new, stone().strength(1.8F, 6.0F));
    public static final DeferredBlock<Block> COBBLED_SLATE_WALL = reg("cobbled_slate_wall", WallBlock::new, stone().strength(1.8F, 6.0F).forceSolidOn());
    public static final DeferredBlock<Block> POLISHED_SLATE_STAIRS = reg("polished_slate_stairs", p -> new StairBlock(POLISHED_SLATE.get().defaultBlockState(), p), stone().strength(1.8F, 6.0F));
    public static final DeferredBlock<Block> POLISHED_SLATE_SLAB = reg("polished_slate_slab", SlabBlock::new, stone().strength(1.8F, 6.0F));
    public static final DeferredBlock<Block> POLISHED_SLATE_WALL = reg("polished_slate_wall", WallBlock::new, stone().strength(1.8F, 6.0F).forceSolidOn());
    public static final DeferredBlock<Block> SLATE_BRICKS_STAIRS = reg("slate_bricks_stairs", p -> new StairBlock(SLATE_BRICKS.get().defaultBlockState(), p), stone().strength(1.8F, 6.0F));
    public static final DeferredBlock<Block> SLATE_BRICKS_SLAB = reg("slate_bricks_slab", SlabBlock::new, stone().strength(1.8F, 6.0F));
    public static final DeferredBlock<Block> SLATE_BRICKS_WALL = reg("slate_bricks_wall", WallBlock::new, stone().strength(1.8F, 6.0F).forceSolidOn());
    public static final DeferredBlock<Block> SLATE_TILES_STAIRS = reg("slate_tiles_stairs", p -> new StairBlock(SLATE_TILES.get().defaultBlockState(), p), stone().strength(1.8F, 6.0F));
    public static final DeferredBlock<Block> SLATE_TILES_SLAB = reg("slate_tiles_slab", SlabBlock::new, stone().strength(1.8F, 6.0F));
    public static final DeferredBlock<Block> SLATE_TILES_WALL = reg("slate_tiles_wall", WallBlock::new, stone().strength(1.8F, 6.0F).forceSolidOn());
    public static final DeferredBlock<Block> RUBY_PLANKS = reg("ruby_planks", p -> new Block(p), wood().strength(2.0F, 3.0F));
    public static final DeferredBlock<Block> STRIPPED_RUBY_LOG = reg("stripped_ruby_log", RotatedPillarBlock::new, wood().strength(2.0F, 2.0F));
    public static final DeferredBlock<Block> RUBY_WOOD = reg("ruby_wood", RotatedPillarBlock::new, wood().strength(2.0F, 2.0F));
    public static final DeferredBlock<Block> STRIPPED_RUBY_WOOD = reg("stripped_ruby_wood", RotatedPillarBlock::new, wood().strength(2.0F, 2.0F));
    public static final DeferredBlock<Block> RUBY_DOOR = reg("ruby_door", p -> new DoorBlock(BlockSetType.OAK, p), wood().strength(3.0F, 3.0F).noOcclusion().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> RUBY_TRAPDOOR = reg("ruby_trapdoor", p -> new TrapDoorBlock(BlockSetType.OAK, p), wood().strength(3.0F, 3.0F).noOcclusion().isValidSpawn((s, g, q, t) -> false));
    public static final DeferredBlock<Block> RUBY_BUTTON = reg("ruby_button", p -> new ButtonBlock(BlockSetType.OAK, 30, p), wood().strength(0.5F).noCollission().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> RUBY_PRESSURE_PLATE = reg("ruby_pressure_plate", p -> new PressurePlateBlock(BlockSetType.OAK, p), wood().strength(0.5F, 0.5F).noCollission().forceSolidOn().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> RUBY_LOG = reg("ruby_log", RotatedPillarBlock::new, wood().strength(2.0F, 2.0F));
    public static final DeferredBlock<Block> RUBY_STAIRS = reg("ruby_stairs", p -> new StairBlock(RUBY_PLANKS.get().defaultBlockState(), p), wood().strength(2.0F, 3.0F));
    public static final DeferredBlock<Block> RUBY_SLAB = reg("ruby_slab", SlabBlock::new, wood().strength(2.0F, 3.0F));
    public static final DeferredBlock<Block> RUBY_FENCE = reg("ruby_fence", FenceBlock::new, wood().strength(2.0F, 3.0F).forceSolidOn());
    public static final DeferredBlock<Block> RUBY_FENCE_GATE = reg("ruby_fence_gate", p -> new FenceGateBlock(WoodType.OAK, p), wood().strength(2.0F, 3.0F).forceSolidOn());
    public static final DeferredBlock<Block> AMBER_PLANKS = reg("amber_planks", p -> new Block(p), wood().strength(2.0F, 3.0F));
    public static final DeferredBlock<Block> STRIPPED_AMBER_LOG = reg("stripped_amber_log", RotatedPillarBlock::new, wood().strength(2.0F, 2.0F));
    public static final DeferredBlock<Block> AMBER_WOOD = reg("amber_wood", RotatedPillarBlock::new, wood().strength(2.0F, 2.0F));
    public static final DeferredBlock<Block> STRIPPED_AMBER_WOOD = reg("stripped_amber_wood", RotatedPillarBlock::new, wood().strength(2.0F, 2.0F));
    public static final DeferredBlock<Block> AMBER_DOOR = reg("amber_door", p -> new DoorBlock(BlockSetType.OAK, p), wood().strength(3.0F, 3.0F).noOcclusion().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> AMBER_TRAPDOOR = reg("amber_trapdoor", p -> new TrapDoorBlock(BlockSetType.OAK, p), wood().strength(3.0F, 3.0F).noOcclusion().isValidSpawn((s, g, q, t) -> false));
    public static final DeferredBlock<Block> AMBER_BUTTON = reg("amber_button", p -> new ButtonBlock(BlockSetType.OAK, 30, p), wood().strength(0.5F).noCollission().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> AMBER_PRESSURE_PLATE = reg("amber_pressure_plate", p -> new PressurePlateBlock(BlockSetType.OAK, p), wood().strength(0.5F, 0.5F).noCollission().forceSolidOn().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> AMBER_LOG = reg("amber_log", RotatedPillarBlock::new, wood().strength(2.0F, 2.0F));
    public static final DeferredBlock<Block> AMBER_STAIRS = reg("amber_stairs", p -> new StairBlock(AMBER_PLANKS.get().defaultBlockState(), p), wood().strength(2.0F, 3.0F));
    public static final DeferredBlock<Block> AMBER_SLAB = reg("amber_slab", SlabBlock::new, wood().strength(2.0F, 3.0F));
    public static final DeferredBlock<Block> AMBER_FENCE = reg("amber_fence", FenceBlock::new, wood().strength(2.0F, 3.0F).forceSolidOn());
    public static final DeferredBlock<Block> AMBER_FENCE_GATE = reg("amber_fence_gate", p -> new FenceGateBlock(WoodType.OAK, p), wood().strength(2.0F, 3.0F).forceSolidOn());
    public static final DeferredBlock<Block> HONEY_PLANKS = reg("honey_planks", p -> new Block(p), wood().strength(2.0F, 3.0F));
    public static final DeferredBlock<Block> STRIPPED_HONEY_LOG = reg("stripped_honey_log", RotatedPillarBlock::new, wood().strength(2.0F, 2.0F));
    public static final DeferredBlock<Block> HONEY_WOOD = reg("honey_wood", RotatedPillarBlock::new, wood().strength(2.0F, 2.0F));
    public static final DeferredBlock<Block> STRIPPED_HONEY_WOOD = reg("stripped_honey_wood", RotatedPillarBlock::new, wood().strength(2.0F, 2.0F));
    public static final DeferredBlock<Block> HONEY_DOOR = reg("honey_door", p -> new DoorBlock(BlockSetType.OAK, p), wood().strength(3.0F, 3.0F).noOcclusion().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> HONEY_TRAPDOOR = reg("honey_trapdoor", p -> new TrapDoorBlock(BlockSetType.OAK, p), wood().strength(3.0F, 3.0F).noOcclusion().isValidSpawn((s, g, q, t) -> false));
    public static final DeferredBlock<Block> HONEY_BUTTON = reg("honey_button", p -> new ButtonBlock(BlockSetType.OAK, 30, p), wood().strength(0.5F).noCollission().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> HONEY_PRESSURE_PLATE = reg("honey_pressure_plate", p -> new PressurePlateBlock(BlockSetType.OAK, p), wood().strength(0.5F, 0.5F).noCollission().forceSolidOn().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> HONEY_LOG = reg("honey_log", RotatedPillarBlock::new, wood().strength(2.0F, 2.0F));
    public static final DeferredBlock<Block> HONEY_STAIRS = reg("honey_stairs", p -> new StairBlock(HONEY_PLANKS.get().defaultBlockState(), p), wood().strength(2.0F, 3.0F));
    public static final DeferredBlock<Block> HONEY_SLAB = reg("honey_slab", SlabBlock::new, wood().strength(2.0F, 3.0F));
    public static final DeferredBlock<Block> HONEY_FENCE = reg("honey_fence", FenceBlock::new, wood().strength(2.0F, 3.0F).forceSolidOn());
    public static final DeferredBlock<Block> HONEY_FENCE_GATE = reg("honey_fence_gate", p -> new FenceGateBlock(WoodType.OAK, p), wood().strength(2.0F, 3.0F).forceSolidOn());
    public static final DeferredBlock<Block> JADE_PLANKS = reg("jade_planks", p -> new Block(p), wood().strength(2.0F, 3.0F));
    public static final DeferredBlock<Block> STRIPPED_JADE_LOG = reg("stripped_jade_log", RotatedPillarBlock::new, wood().strength(2.0F, 2.0F));
    public static final DeferredBlock<Block> JADE_WOOD = reg("jade_wood", RotatedPillarBlock::new, wood().strength(2.0F, 2.0F));
    public static final DeferredBlock<Block> STRIPPED_JADE_WOOD = reg("stripped_jade_wood", RotatedPillarBlock::new, wood().strength(2.0F, 2.0F));
    public static final DeferredBlock<Block> JADE_DOOR = reg("jade_door", p -> new DoorBlock(BlockSetType.OAK, p), wood().strength(3.0F, 3.0F).noOcclusion().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> JADE_TRAPDOOR = reg("jade_trapdoor", p -> new TrapDoorBlock(BlockSetType.OAK, p), wood().strength(3.0F, 3.0F).noOcclusion().isValidSpawn((s, g, q, t) -> false));
    public static final DeferredBlock<Block> JADE_BUTTON = reg("jade_button", p -> new ButtonBlock(BlockSetType.OAK, 30, p), wood().strength(0.5F).noCollission().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> JADE_PRESSURE_PLATE = reg("jade_pressure_plate", p -> new PressurePlateBlock(BlockSetType.OAK, p), wood().strength(0.5F, 0.5F).noCollission().forceSolidOn().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> JADE_LOG = reg("jade_log", RotatedPillarBlock::new, wood().strength(2.0F, 2.0F));
    public static final DeferredBlock<Block> JADE_STAIRS = reg("jade_stairs", p -> new StairBlock(JADE_PLANKS.get().defaultBlockState(), p), wood().strength(2.0F, 3.0F));
    public static final DeferredBlock<Block> JADE_SLAB = reg("jade_slab", SlabBlock::new, wood().strength(2.0F, 3.0F));
    public static final DeferredBlock<Block> JADE_FENCE = reg("jade_fence", FenceBlock::new, wood().strength(2.0F, 3.0F).forceSolidOn());
    public static final DeferredBlock<Block> JADE_FENCE_GATE = reg("jade_fence_gate", p -> new FenceGateBlock(WoodType.OAK, p), wood().strength(2.0F, 3.0F).forceSolidOn());
    public static final DeferredBlock<Block> AZURE_PLANKS = reg("azure_planks", p -> new Block(p), wood().strength(2.0F, 3.0F));
    public static final DeferredBlock<Block> STRIPPED_AZURE_LOG = reg("stripped_azure_log", RotatedPillarBlock::new, wood().strength(2.0F, 2.0F));
    public static final DeferredBlock<Block> AZURE_WOOD = reg("azure_wood", RotatedPillarBlock::new, wood().strength(2.0F, 2.0F));
    public static final DeferredBlock<Block> STRIPPED_AZURE_WOOD = reg("stripped_azure_wood", RotatedPillarBlock::new, wood().strength(2.0F, 2.0F));
    public static final DeferredBlock<Block> AZURE_DOOR = reg("azure_door", p -> new DoorBlock(BlockSetType.OAK, p), wood().strength(3.0F, 3.0F).noOcclusion().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> AZURE_TRAPDOOR = reg("azure_trapdoor", p -> new TrapDoorBlock(BlockSetType.OAK, p), wood().strength(3.0F, 3.0F).noOcclusion().isValidSpawn((s, g, q, t) -> false));
    public static final DeferredBlock<Block> AZURE_BUTTON = reg("azure_button", p -> new ButtonBlock(BlockSetType.OAK, 30, p), wood().strength(0.5F).noCollission().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> AZURE_PRESSURE_PLATE = reg("azure_pressure_plate", p -> new PressurePlateBlock(BlockSetType.OAK, p), wood().strength(0.5F, 0.5F).noCollission().forceSolidOn().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> AZURE_LOG = reg("azure_log", RotatedPillarBlock::new, wood().strength(2.0F, 2.0F));
    public static final DeferredBlock<Block> AZURE_STAIRS = reg("azure_stairs", p -> new StairBlock(AZURE_PLANKS.get().defaultBlockState(), p), wood().strength(2.0F, 3.0F));
    public static final DeferredBlock<Block> AZURE_SLAB = reg("azure_slab", SlabBlock::new, wood().strength(2.0F, 3.0F));
    public static final DeferredBlock<Block> AZURE_FENCE = reg("azure_fence", FenceBlock::new, wood().strength(2.0F, 3.0F).forceSolidOn());
    public static final DeferredBlock<Block> AZURE_FENCE_GATE = reg("azure_fence_gate", p -> new FenceGateBlock(WoodType.OAK, p), wood().strength(2.0F, 3.0F).forceSolidOn());
    public static final DeferredBlock<Block> VIOLET_PLANKS = reg("violet_planks", p -> new Block(p), wood().strength(2.0F, 3.0F));
    public static final DeferredBlock<Block> STRIPPED_VIOLET_LOG = reg("stripped_violet_log", RotatedPillarBlock::new, wood().strength(2.0F, 2.0F));
    public static final DeferredBlock<Block> VIOLET_WOOD = reg("violet_wood", RotatedPillarBlock::new, wood().strength(2.0F, 2.0F));
    public static final DeferredBlock<Block> STRIPPED_VIOLET_WOOD = reg("stripped_violet_wood", RotatedPillarBlock::new, wood().strength(2.0F, 2.0F));
    public static final DeferredBlock<Block> VIOLET_DOOR = reg("violet_door", p -> new DoorBlock(BlockSetType.OAK, p), wood().strength(3.0F, 3.0F).noOcclusion().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> VIOLET_TRAPDOOR = reg("violet_trapdoor", p -> new TrapDoorBlock(BlockSetType.OAK, p), wood().strength(3.0F, 3.0F).noOcclusion().isValidSpawn((s, g, q, t) -> false));
    public static final DeferredBlock<Block> VIOLET_BUTTON = reg("violet_button", p -> new ButtonBlock(BlockSetType.OAK, 30, p), wood().strength(0.5F).noCollission().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> VIOLET_PRESSURE_PLATE = reg("violet_pressure_plate", p -> new PressurePlateBlock(BlockSetType.OAK, p), wood().strength(0.5F, 0.5F).noCollission().forceSolidOn().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> VIOLET_LOG = reg("violet_log", RotatedPillarBlock::new, wood().strength(2.0F, 2.0F));
    public static final DeferredBlock<Block> VIOLET_STAIRS = reg("violet_stairs", p -> new StairBlock(VIOLET_PLANKS.get().defaultBlockState(), p), wood().strength(2.0F, 3.0F));
    public static final DeferredBlock<Block> VIOLET_SLAB = reg("violet_slab", SlabBlock::new, wood().strength(2.0F, 3.0F));
    public static final DeferredBlock<Block> VIOLET_FENCE = reg("violet_fence", FenceBlock::new, wood().strength(2.0F, 3.0F).forceSolidOn());
    public static final DeferredBlock<Block> VIOLET_FENCE_GATE = reg("violet_fence_gate", p -> new FenceGateBlock(WoodType.OAK, p), wood().strength(2.0F, 3.0F).forceSolidOn());
    public static final DeferredBlock<Block> IVORY_PLANKS = reg("ivory_planks", p -> new Block(p), wood().strength(2.0F, 3.0F));
    public static final DeferredBlock<Block> STRIPPED_IVORY_LOG = reg("stripped_ivory_log", RotatedPillarBlock::new, wood().strength(2.0F, 2.0F));
    public static final DeferredBlock<Block> IVORY_WOOD = reg("ivory_wood", RotatedPillarBlock::new, wood().strength(2.0F, 2.0F));
    public static final DeferredBlock<Block> STRIPPED_IVORY_WOOD = reg("stripped_ivory_wood", RotatedPillarBlock::new, wood().strength(2.0F, 2.0F));
    public static final DeferredBlock<Block> IVORY_DOOR = reg("ivory_door", p -> new DoorBlock(BlockSetType.OAK, p), wood().strength(3.0F, 3.0F).noOcclusion().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> IVORY_TRAPDOOR = reg("ivory_trapdoor", p -> new TrapDoorBlock(BlockSetType.OAK, p), wood().strength(3.0F, 3.0F).noOcclusion().isValidSpawn((s, g, q, t) -> false));
    public static final DeferredBlock<Block> IVORY_BUTTON = reg("ivory_button", p -> new ButtonBlock(BlockSetType.OAK, 30, p), wood().strength(0.5F).noCollission().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> IVORY_PRESSURE_PLATE = reg("ivory_pressure_plate", p -> new PressurePlateBlock(BlockSetType.OAK, p), wood().strength(0.5F, 0.5F).noCollission().forceSolidOn().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> IVORY_LOG = reg("ivory_log", RotatedPillarBlock::new, wood().strength(2.0F, 2.0F));
    public static final DeferredBlock<Block> IVORY_STAIRS = reg("ivory_stairs", p -> new StairBlock(IVORY_PLANKS.get().defaultBlockState(), p), wood().strength(2.0F, 3.0F));
    public static final DeferredBlock<Block> IVORY_SLAB = reg("ivory_slab", SlabBlock::new, wood().strength(2.0F, 3.0F));
    public static final DeferredBlock<Block> IVORY_FENCE = reg("ivory_fence", FenceBlock::new, wood().strength(2.0F, 3.0F).forceSolidOn());
    public static final DeferredBlock<Block> IVORY_FENCE_GATE = reg("ivory_fence_gate", p -> new FenceGateBlock(WoodType.OAK, p), wood().strength(2.0F, 3.0F).forceSolidOn());
    public static final DeferredBlock<Block> SHADOW_PLANKS = reg("shadow_planks", p -> new Block(p), wood().strength(2.0F, 3.0F));
    public static final DeferredBlock<Block> STRIPPED_SHADOW_LOG = reg("stripped_shadow_log", RotatedPillarBlock::new, wood().strength(2.0F, 2.0F));
    public static final DeferredBlock<Block> SHADOW_WOOD = reg("shadow_wood", RotatedPillarBlock::new, wood().strength(2.0F, 2.0F));
    public static final DeferredBlock<Block> STRIPPED_SHADOW_WOOD = reg("stripped_shadow_wood", RotatedPillarBlock::new, wood().strength(2.0F, 2.0F));
    public static final DeferredBlock<Block> SHADOW_DOOR = reg("shadow_door", p -> new DoorBlock(BlockSetType.OAK, p), wood().strength(3.0F, 3.0F).noOcclusion().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> SHADOW_TRAPDOOR = reg("shadow_trapdoor", p -> new TrapDoorBlock(BlockSetType.OAK, p), wood().strength(3.0F, 3.0F).noOcclusion().isValidSpawn((s, g, q, t) -> false));
    public static final DeferredBlock<Block> SHADOW_BUTTON = reg("shadow_button", p -> new ButtonBlock(BlockSetType.OAK, 30, p), wood().strength(0.5F).noCollission().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> SHADOW_PRESSURE_PLATE = reg("shadow_pressure_plate", p -> new PressurePlateBlock(BlockSetType.OAK, p), wood().strength(0.5F, 0.5F).noCollission().forceSolidOn().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> SHADOW_LOG = reg("shadow_log", RotatedPillarBlock::new, wood().strength(2.0F, 2.0F));
    public static final DeferredBlock<Block> SHADOW_STAIRS = reg("shadow_stairs", p -> new StairBlock(SHADOW_PLANKS.get().defaultBlockState(), p), wood().strength(2.0F, 3.0F));
    public static final DeferredBlock<Block> SHADOW_SLAB = reg("shadow_slab", SlabBlock::new, wood().strength(2.0F, 3.0F));
    public static final DeferredBlock<Block> SHADOW_FENCE = reg("shadow_fence", FenceBlock::new, wood().strength(2.0F, 3.0F).forceSolidOn());
    public static final DeferredBlock<Block> SHADOW_FENCE_GATE = reg("shadow_fence_gate", p -> new FenceGateBlock(WoodType.OAK, p), wood().strength(2.0F, 3.0F).forceSolidOn());
}
