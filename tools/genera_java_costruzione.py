"""
Dalla specifica tools/blocchi_costruzione.json scrive
src/main/java/com/futuretek/registry/ModBuildingBlocks.java (registrazione dei blocchi da costruzione).
Uso: python tools/genera_java_costruzione.py
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SPEC = json.loads((ROOT / "tools/blocchi_costruzione.json").read_text(encoding="utf-8"))["blocchi"]
OUT = ROOT / "src/main/java/com/futuretek/registry/ModBuildingBlocks.java"


def campo(id_):
    return id_.upper()


righe = []
strip = []
for b in SPEC:
    if b.get("scortecciato"):
        strip.append(f"        STRIP.put({campo(b['id'])}.get(), {campo(b['scortecciato'])}.get());")
for b in SPEC:
    id_, tipo = b["id"], b["tipo"]
    legno = b["materiale"] == "legno"
    props = "wood()" if legno else "stone()"
    dur = b.get("durezza", 2.0)
    res = b.get("resistenza", 6.0 if not legno else 3.0)
    p = f"{props}.strength({dur}F, {res}F)"
    if tipo == "cubo":
        ctor = f"p -> new Block(p), {p}"
    elif tipo == "tronco":
        ctor = f"RotatedPillarBlock::new, {p}"
    elif tipo == "scale":
        ctor = f"p -> new StairBlock({campo(b['base'])}.get().defaultBlockState(), p), {p}"
    elif tipo == "lastra":
        ctor = f"SlabBlock::new, {p}"
    elif tipo == "muro":
        ctor = f"WallBlock::new, {p}.forceSolidOn()"
    elif tipo == "staccionata":
        ctor = f"FenceBlock::new, {p}.forceSolidOn()"
    elif tipo == "cancello":
        ctor = f"p -> new FenceGateBlock(WoodType.OAK, p), {p}.forceSolidOn()"
    elif tipo == "porta":
        ctor = f"p -> new DoorBlock(BlockSetType.OAK, p), {p}.noOcclusion().pushReaction(PushReaction.DESTROY)"
    elif tipo == "botola":
        ctor = f"p -> new TrapDoorBlock(BlockSetType.OAK, p), {p}.noOcclusion().isValidSpawn((s, g, q, t) -> false)"
    elif tipo == "bottone":
        set_ = "OAK" if legno else "STONE"
        ticks = 30 if legno else 20
        base = "button()" if not legno else "wood()"
        ctor = (f"p -> new ButtonBlock(BlockSetType.{set_}, {ticks}, p), "
                f"{base}.strength({dur}F).noCollission().pushReaction(PushReaction.DESTROY)")
    elif tipo == "pedana":
        set_ = "OAK" if legno else "STONE"
        ctor = (f"p -> new PressurePlateBlock(BlockSetType.{set_}, p), "
                f"{p}.noCollission().forceSolidOn().pushReaction(PushReaction.DESTROY)")
    else:
        raise SystemExit("tipo sconosciuto " + tipo)
    righe.append(f'    public static final DeferredBlock<Block> {campo(id_)} = reg("{id_}", {ctor});')

java = f'''package com.futuretek.registry;

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
public final class ModBuildingBlocks {{
    private ModBuildingBlocks() {{
    }}

    /** Basta chiamarlo una volta (dal costruttore della mod) per registrare tutto */
    public static void init() {{
    }}

    private static final Map<Block, Block> STRIP = new IdentityHashMap<>();

    /** L'ascia toglie la corteccia ai nostri tronchi (come con quelli di Minecraft) */
    public static void onToolUse(net.neoforged.neoforge.event.level.BlockEvent.BlockToolModificationEvent event) {{
        if (event.getItemAbility() != net.neoforged.neoforge.common.ItemAbilities.AXE_STRIP) {{
            return;
        }}
        if (STRIP.isEmpty()) {{
{chr(10).join(strip)}
        }}
        Block stripped = STRIP.get(event.getState().getBlock());
        if (stripped != null) {{
            event.setFinalState(stripped.defaultBlockState().setValue(RotatedPillarBlock.AXIS,
                    event.getState().getValue(RotatedPillarBlock.AXIS)));
        }}
    }}

    private static BlockBehaviour.Properties button() {{
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).sound(SoundType.STONE);
    }}

    private static BlockBehaviour.Properties stone() {{
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().sound(SoundType.STONE);
    }}

    private static BlockBehaviour.Properties wood() {{
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS)
                .sound(SoundType.WOOD).ignitedByLava();
    }}

    private static DeferredBlock<Block> reg(String name, Function<BlockBehaviour.Properties, ? extends Block> factory,
            BlockBehaviour.Properties props) {{
        DeferredBlock<Block> block = ModBlocks.BLOCKS.registerBlock(name, factory, props);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }}

{chr(10).join(righe)}
}}
'''
OUT.write_text(java, encoding="utf-8")
print(f"{len(righe)} blocchi -> {OUT}")
