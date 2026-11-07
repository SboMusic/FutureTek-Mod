package com.futuretek.world;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.futuretek.FutureTek;
import com.futuretek.registry.ModBlocks;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * FABBRICA ABBANDONATA: in ogni mondo nuovo (regola del mondo "futuretekAbandonedFactory", attiva di base:
 * si cambia in "Altre opzioni del mondo -> Regole di gioco" quando crei il mondo) compare vicino al punto di
 * nascita un capannone in rovina con UNA COPIA DI OGNI MACCHINA della mod, spenta e con i cavi rotti qua e la'.
 * Il giocatore la rimette in funzione: aggiusta i cavi, mette il carbone nei generatori...
 * La fabbrica si costruisce leggendo l'elenco dei blocchi della mod: quando aggiungiamo una macchina nuova,
 * nelle fabbriche dei mondi nuovi c'e' automaticamente.
 * Comando: /futuretek fabbrica  (costruisce una fabbrica davanti a te, per le prove)
 */
public final class AbandonedFactory {
    private AbandonedFactory() {
    }

    public static final GameRules.Key<GameRules.BooleanValue> RULE = GameRules.register("futuretekAbandonedFactory",
            GameRules.Category.MISC, GameRules.BooleanValue.create(true));

    public static final int SPACING = 3;
    public static final int ROW_DEPTH = 5;
    public static final int PER_ROW = 14;
    public static final int HEIGHT = 6;

    /** Ordine e nomi dei reparti; ogni blocco va nel primo reparto che contiene una sua parola chiave */
    private static final Map<String, String[]> HALLS = new LinkedHashMap<>();

    static {
        HALLS.put("energia", new String[]{"generator", "solar", "mill", "battery", "transformer", "switch"});
        HALLS.put("lavorazione", new String[]{"crusher", "furnace", "compressor", "extractor", "press", "bath",
                "centrifuge", "kiln", "canning", "recycler", "macerator"});
        HALLS.put("logistica", new String[]{"logistics", "terminal", "crafter", "chest", "cart", "sorter"});
        HALLS.put("agricoltura", new String[]{"harvester", "growth", "tree_farm", "planter", "fertilizer", "apiary",
                "hive", "farm"});
        HALLS.put("miniera", new String[]{"quarry", "mole", "pump", "drill"});
        HALLS.put("nucleare", new String[]{"reactor", "nuclear"});
        HALLS.put("avanzate", new String[]{"enchanter", "incubator", "laser", "robot", "teleport", "lamp", "light"});
        HALLS.put("alchimia", new String[]{"synthesis", "essence", "star", "lantern", "alchem", "transmut", "collector"});
        HALLS.put("varie", new String[]{""});
    }

    /** Blocchi da esporre: quelli "con un cervello" (block entity) della mod, piu' la camera del reattore */
    public static Map<String, List<Block>> stations() {
        Map<String, List<Block>> map = new LinkedHashMap<>();
        HALLS.keySet().forEach(k -> map.put(k, new ArrayList<>()));
        for (var holder : ModBlocks.BLOCKS.getEntries()) {
            Block block = holder.get();
            String id = holder.getId().getPath();
            boolean special = block == ModBlocks.REACTOR_CHAMBER.get();
            if ((!(block instanceof EntityBlock) && !special) || block.asItem() == net.minecraft.world.item.Items.AIR
                    || id.contains("cable") || id.contains("conduit") || id.contains("quarry_frame")
                    || id.contains("quarry_drill") || id.contains("nuclear_tnt")) {
                continue;
            }
            for (var hall : HALLS.entrySet()) {
                boolean match = false;
                for (String key : hall.getValue()) {
                    if (id.contains(key)) {
                        match = true;
                        break;
                    }
                }
                if (match) {
                    map.get(hall.getKey()).add(block);
                    break;
                }
            }
        }
        map.values().removeIf(List::isEmpty);
        return map;
    }

    // =====================================================================
    // costruzione
    // =====================================================================
    /** Costruisce la fabbrica con l'angolo nord-ovest del pavimento in "corner"; restituisce l'entrata */
    public static BlockPos build(ServerLevel level, BlockPos corner, RandomSource random) {
        Map<String, List<Block>> stations = stations();
        List<List<Block>> rows = new ArrayList<>();
        List<String> rowNames = new ArrayList<>();
        for (var e : stations.entrySet()) {
            List<Block> list = e.getValue();
            for (int i = 0; i < list.size(); i += PER_ROW) {
                rows.add(list.subList(i, Math.min(list.size(), i + PER_ROW)));
                rowNames.add(e.getKey());
            }
        }
        int width = 4 + PER_ROW * SPACING;
        int depth = 4 + rows.size() * ROW_DEPTH;
        BlockState floor = Blocks.POLISHED_ANDESITE.defaultBlockState();
        // 1) terreno: pavimento, fondamenta e aria dentro
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                BlockPos f = corner.offset(x, 0, z);
                level.setBlock(f, random.nextInt(9) == 0 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState()
                        : random.nextInt(12) == 0 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : floor, Block.UPDATE_CLIENTS);
                for (int d = 1; d <= 24; d++) {
                    BlockPos b = f.below(d);
                    if (level.getBlockState(b).canBeReplaced() || !level.getFluidState(b).isEmpty()) {
                        level.setBlock(b, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                    } else {
                        break;
                    }
                }
                for (int y = 1; y <= HEIGHT + 4; y++) {
                    level.setBlock(f.above(y), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        // 2) muri con finestre rotte, entrata a sud
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                boolean edge = x == 0 || z == 0 || x == width - 1 || z == depth - 1;
                if (!edge) {
                    continue;
                }
                boolean door = z == depth - 1 && Math.abs(x - width / 2) <= 1;
                for (int y = 1; y <= HEIGHT; y++) {
                    BlockPos p = corner.offset(x, y, z);
                    if (door && y <= 3) {
                        continue;
                    }
                    BlockState wall = random.nextInt(6) == 0 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState()
                            : random.nextInt(8) == 0 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState()
                            : Blocks.STONE_BRICKS.defaultBlockState();
                    if (y == 3 && (x + z) % 4 == 0) {
                        wall = random.nextInt(3) == 0 ? Blocks.AIR.defaultBlockState() : Blocks.IRON_BARS.defaultBlockState();
                    }
                    if (y >= HEIGHT - 1 && random.nextInt(7) == 0) {
                        wall = Blocks.AIR.defaultBlockState(); // muro crollato in alto
                    }
                    level.setBlock(p, wall, Block.UPDATE_ALL);
                }
            }
        }
        // 3) reparti: macchine in fila, cavo davanti, cartello sopra
        Block cable = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(FutureTek.MODID, "tin_cable"));
        List<BlockPos> cables = new ArrayList<>();
        List<BlockPos> openSky = new ArrayList<>();
        for (int r = 0; r < rows.size(); r++) {
            int z = 2 + r * ROW_DEPTH;
            // cartello del reparto all'inizio della fila
            sign(level, corner.offset(1, 1, z), Component.translatable("gui.futuretek.factory_hall." + rowNames.get(r))
                    .withStyle(ChatFormatting.DARK_BLUE, ChatFormatting.BOLD), Component.empty());
            List<Block> row = rows.get(r);
            for (int i = 0; i < row.size(); i++) {
                BlockPos m = corner.offset(3 + i * SPACING, 1, z);
                BlockState state = row.get(i).defaultBlockState();
                if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
                    state = state.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH);
                } else if (state.hasProperty(BlockStateProperties.FACING)) {
                    state = state.setValue(BlockStateProperties.FACING, Direction.SOUTH);
                }
                level.setBlock(m, state, Block.UPDATE_ALL);
                String id = BuiltInRegistries.BLOCK.getKey(row.get(i)).getPath();
                if (id.contains("solar") || id.contains("wind")) {
                    openSky.add(m);
                } else {
                    sign(level, m.above(), Component.translatable(row.get(i).getDescriptionId()), Component.empty());
                }
                if (id.contains("water_mill")) {
                    level.setBlock(m.east(), Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
            for (int x = 2; x < 3 + row.size() * SPACING - 2; x++) {
                cables.add(corner.offset(x, 1, z + 1));
            }
        }
        // spina dorsale: collega tutte le file a sinistra
        for (int z = 3; z <= 3 + (rows.size() - 1) * ROW_DEPTH; z++) {
            cables.add(corner.offset(2, 1, z));
        }
        for (BlockPos c : cables) {
            // "in disuso": un cavo su 8 manca e va rimesso
            if (cable != Blocks.AIR && random.nextInt(8) != 0 && level.getBlockState(c).isAir()) {
                level.setBlock(c, cable.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        // 4) tetto con dei buchi (e niente tetto sopra pannelli e mulini a vento)
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                BlockPos p = corner.offset(x, HEIGHT + 1, z);
                boolean sky = false;
                for (BlockPos s : openSky) {
                    if (Math.abs(s.getX() - p.getX()) <= 1 && Math.abs(s.getZ() - p.getZ()) <= 1) {
                        sky = true;
                    }
                }
                if (!sky && random.nextInt(5) != 0) {
                    level.setBlock(p, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        // 5) ragnatele e muschio: e' abbandonata!
        for (int n = 0; n < width * depth / 12; n++) {
            BlockPos p = corner.offset(1 + random.nextInt(width - 2), HEIGHT - random.nextInt(2), 1 + random.nextInt(depth - 2));
            if (level.getBlockState(p).isAir()) {
                level.setBlock(p, Blocks.COBWEB.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        // 6) magazzino di partenza vicino all'entrata
        BlockPos chestPos = corner.offset(width / 2 + 3, 1, depth - 2);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            int slot = 0;
            for (String[] e : new String[][]{{"minecraft:coal", "64"}, {"minecraft:coal", "64"}, {"futuretek:tin_cable", "32"},
                    {"minecraft:iron_ingot", "16"}, {"minecraft:copper_ingot", "16"}, {"futuretek:tin_ingot", "16"},
                    {"minecraft:redstone", "16"}, {"minecraft:lever", "4"}, {"minecraft:water_bucket", "1"},
                    {"minecraft:oak_sapling", "8"}, {"minecraft:wheat_seeds", "16"}, {"minecraft:bone_meal", "16"}}) {
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(e[0]));
                if (item != net.minecraft.world.item.Items.AIR) {
                    chest.setItem(slot++, new ItemStack(item, Integer.parseInt(e[1])));
                }
            }
        }
        sign(level, chestPos.west(), Component.translatable("gui.futuretek.factory_title").withStyle(ChatFormatting.DARK_RED),
                Component.translatable("gui.futuretek.factory_hint"));
        return corner.offset(width / 2, 1, depth);
    }

    static void sign(ServerLevel level, BlockPos pos, Component line1, Component line2) {
        if (!level.getBlockState(pos).isAir()) {
            return;
        }
        level.setBlock(pos, Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, 8), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
            SignText text = new SignText().setMessage(1, line1).setMessage(2, line2);
            sign.setText(text, true);
            sign.setWaxed(true);
            sign.setChanged();
        }
    }

    // =====================================================================
    // una volta per mondo, vicino al punto di nascita
    // =====================================================================
    public static class Data extends SavedData {
        public BlockPos entrance;

        static Data load(CompoundTag tag, HolderLookup.Provider registries) {
            Data d = new Data();
            d.entrance = NbtUtils.readBlockPos(tag, "Entrance").orElse(null);
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            if (entrance != null) {
                tag.put("Entrance", NbtUtils.writeBlockPos(entrance));
            }
            tag.putBoolean("Done", true);
            return tag;
        }

        static Data get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Data::new, Data::load), "futuretek_factory");
        }
    }

    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        if (server instanceof net.minecraft.gametest.framework.GameTestServer) {
            return;
        }
        ServerLevel level = server.overworld();
        Data data = Data.get(level);
        if (data.entrance != null || !level.getGameRules().getBoolean(RULE) || level.getGameTime() > 20 * 60 * 5) {
            if (data.entrance == null) {
                data.entrance = BlockPos.ZERO.below(1000); // mondo vecchio o regola spenta: non costruire mai piu'
                data.setDirty();
            }
            return;
        }
        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos corner = findFlatSpot(level, spawn);
        data.entrance = build(level, corner, RandomSource.create(level.getSeed()));
        data.setDirty();
        FutureTek.LOGGER.info("Fabbrica abbandonata costruita a {}", data.entrance);
    }

    /**
     * Cerca intorno al punto di nascita il posto piu' PIATTO (e non in mezzo al mare, non su una montagna):
     * prova 16 posizioni a 24-72 blocchi e sceglie quella con meno differenza di altezza.
     */
    static BlockPos findFlatSpot(ServerLevel level, BlockPos spawn) {
        int width = 4 + PER_ROW * SPACING;
        int depth = 40;
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        for (int ring = 1; ring <= 2; ring++) {
            for (int k = 0; k < 8; k++) {
                double ang = Math.PI * 2 * k / 8 + ring * 0.4;
                int cx = spawn.getX() + (int) (Math.cos(ang) * (24 + ring * 24)) - width / 2;
                int cz = spawn.getZ() + (int) (Math.sin(ang) * (24 + ring * 24)) - depth / 2;
                int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE, sum = 0, n = 0, water = 0;
                for (int dx = 0; dx <= width; dx += width / 3) {
                    for (int dz = 0; dz <= depth; dz += depth / 3) {
                        int x = cx + dx;
                        int z = cz + dz;
                        level.getChunk(x >> 4, z >> 4);
                        int h = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
                        if (level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) > h + 1) {
                            water++; // acqua (o alberi) sopra il terreno
                        }
                        min = Math.min(min, h);
                        max = Math.max(max, h);
                        sum += h;
                        n++;
                    }
                }
                int avg = sum / n;
                double score = (max - min) + water * 2 + Math.max(0, avg - level.getSeaLevel() - 24) * 2
                        + Math.max(0, level.getSeaLevel() - avg) * 4;
                if (score < bestScore) {
                    bestScore = score;
                    best = new BlockPos(cx, Math.max(level.getSeaLevel(), avg), cz);
                }
            }
        }
        return best;
    }

    /** Al primo ingresso nel mondo dice dov'e' la fabbrica */
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CompoundTag persistent = player.getPersistentData();
        if (persistent.getBoolean("futuretek_factory_told")) {
            return;
        }
        Data data = Data.get(player.serverLevel().getServer().overworld());
        if (data.entrance != null && data.entrance.getY() > -500) {
            persistent.putBoolean("futuretek_factory_told", true);
            player.sendSystemMessage(Component.translatable("message.futuretek.factory_where", data.entrance.getX(),
                    data.entrance.getY(), data.entrance.getZ()).withStyle(ChatFormatting.GOLD));
        }
    }

    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("futuretek").then(Commands.literal("fabbrica")
                .requires(s -> s.hasPermission(2)).executes(AbandonedFactory::command)));
    }

    private static int command(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        ServerLevel level = src.getLevel();
        BlockPos at = BlockPos.containing(src.getPosition());
        Direction facing = src.getEntity() instanceof Player p ? p.getDirection() : Direction.NORTH;
        BlockPos corner = at.relative(facing, 4).offset(-(4 + PER_ROW * SPACING) / 2, -1, facing == Direction.NORTH ? -30 : 0);
        BlockPos entrance = build(level, corner, level.random);
        src.sendSuccess(() -> Component.translatable("message.futuretek.factory_built", entrance.getX(), entrance.getY(),
                entrance.getZ()), true);
        return 1;
    }
}
