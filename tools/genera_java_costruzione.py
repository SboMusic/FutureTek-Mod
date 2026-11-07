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
    else:
        raise SystemExit("tipo sconosciuto " + tipo)
    righe.append(f'    public static final DeferredBlock<Block> {campo(id_)} = reg("{id_}", {ctor});')

java = f'''package com.futuretek.registry;

import java.util.function.Function;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
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
 * scale, lastre e muretti) e 8 legni colorati (assi, tronchi, scale, lastre, staccionate, cancelli).
 * FILE GENERATO da tools/genera_java_costruzione.py partendo da tools/blocchi_costruzione.json: non modificarlo a mano.
 */
public final class ModBuildingBlocks {{
    private ModBuildingBlocks() {{
    }}

    /** Basta chiamarlo una volta (dal costruttore della mod) per registrare tutto */
    public static void init() {{
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
