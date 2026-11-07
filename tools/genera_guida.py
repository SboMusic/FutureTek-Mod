"""
FutureTek - Generatore della Guida ai craft
===========================================
Legge TUTTE le ricette generate (src/generated/resources/data/futuretek/recipe), i nomi italiani e le texture,
e scrive una pagina HTML unica con la guida grafica: docs/guida_craft.html

Uso (dalla cartella del progetto, DOPO tools/genera_risorse.py):  python tools/genera_guida.py
"""
import base64
import io
import json
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
GEN = ROOT / "src/generated/resources"
RIC = GEN / "data/futuretek/recipe"
TEX = ROOT / "src/main/resources/assets/futuretek/textures"
MOD = GEN / "assets/futuretek/models"
OUT = ROOT / "docs/guida_craft.html"

LINGUA = json.loads((GEN / "assets/futuretek/lang/it_it.json").read_text(encoding="utf-8"))

# Nomi italiani degli oggetti di Minecraft usati nelle ricette (le loro immagini non vengono copiate:
# nella guida compaiono come tessere colorate con il nome)
VANILLA = {
    "amethyst_shard": ("Frammento d'ametista", "#9a6fd6"), "blast_furnace": ("Altoforno", "#5b5b63"),
    "blaze_powder": ("Polvere di blaze", "#f0a020"), "blaze_rod": ("Bastone di blaze", "#f0b030"),
    "blue_ice": ("Ghiaccio blu", "#74b4f0"), "bone": ("Osso", "#e6e2cf"), "bone_block": ("Blocco d'ossa", "#e0dcc4"),
    "bone_meal": ("Farina d'ossa", "#f2f0e6"), "book": ("Libro", "#8a5a34"), "bucket": ("Secchio", "#b8bcc2"),
    "cauldron": ("Calderone", "#4a4a50"), "chest_minecart": ("Carrello con baule", "#8a6a40"),
    "chorus_fruit": ("Frutto di chorus", "#8e6a9a"), "clock": ("Orologio", "#e0b840"), "coal": ("Carbone", "#2e2e32"),
    "cobblestone": ("Pietrisco", "#7a7a7a"), "comparator": ("Comparatore", "#b0aaa4"),
    "copper_ingot": ("Lingotto di rame", "#d27c50"), "crafting_table": ("Banco da lavoro", "#9a6a3a"),
    "crying_obsidian": ("Ossidiana piangente", "#4a2a7a"), "diamond": ("Diamante", "#5ee0e0"),
    "diamond_block": ("Blocco di diamante", "#6ae8e2"), "diamond_pickaxe": ("Piccone di diamante", "#5ee0e0"),
    "ender_chest": ("Baule dell'End", "#1e3a3a"), "ender_pearl": ("Perla dell'End", "#2a8a7a"),
    "feather": ("Piuma", "#f0f0f0"), "flint": ("Selce", "#4a4a4a"), "furnace": ("Fornace", "#6a6a6a"),
    "glowstone": ("Pietra luminosa", "#e8c860"), "glowstone_dust": ("Polvere di pietra luminosa", "#f0d870"),
    "gold_ingot": ("Lingotto d'oro", "#f0c840"), "golden_apple": ("Mela d'oro", "#f0c030"), "gravel": ("Ghiaia", "#8a8480"),
    "gunpowder": ("Polvere da sparo", "#5a5a5a"), "honeycomb": ("Favo", "#e8a828"), "hopper": ("Tramoggia", "#4a4a4e"),
    "ice": ("Ghiaccio", "#a8c8f0"), "iron_bars": ("Sbarre di ferro", "#9a9a9a"), "iron_hoe": ("Zappa di ferro", "#d0d0d0"),
    "iron_ingot": ("Lingotto di ferro", "#d8d8d8"), "lantern": ("Lanterna", "#e8b040"),
    "lapis_block": ("Blocco di lapislazzuli", "#2a50b0"), "leather": ("Cuoio", "#a05a30"), "lever": ("Leva", "#8a7a60"),
    "magma_cream": ("Crema di magma", "#e06020"), "nether_star": ("Stella del Nether", "#f4f4ff"),
    "obsidian": ("Ossidiana", "#1e1428"), "packed_ice": ("Ghiaccio compatto", "#8ab0e8"),
    "prismarine_shard": ("Frammento di prismarino", "#5aa89a"), "redstone_torch": ("Torcia di redstone", "#d03020"),
    "rotten_flesh": ("Carne marcia", "#9a5a3a"), "sand": ("Sabbia", "#dccf9e"), "sandstone": ("Arenaria", "#d8c890"),
    "shears": ("Cesoie", "#c8c8c8"), "slime_ball": ("Palla di slime", "#7ac860"), "slime_block": ("Blocco di slime", "#6ab850"),
    "smooth_stone_slab": ("Lastra di pietra liscia", "#9a9a9a"), "snow_block": ("Blocco di neve", "#f4f8f8"),
    "snowball": ("Palla di neve", "#f0f6f6"), "stone": ("Pietra", "#7e7e7e"),
    "stone_pressure_plate": ("Pedana di pietra", "#8a8a8a"), "string": ("Filo", "#e8e8e8"), "tnt": ("TNT", "#d03a2a"),
    "water_bucket": ("Secchio d'acqua", "#3a6ad8"), "bricks": ("Mattoni", "#a0583e"),
    "lapis_lazuli": ("Lapislazzuli", "#2a50b0"),
}
MATERIALI_IT = {"tin": "stagno", "lead": "piombo", "silver": "argento", "uranium": "uranio", "bronze": "bronzo",
                "steel": "acciaio", "iron": "ferro", "gold": "oro", "copper": "rame", "coal": "carbone",
                "diamond": "diamante", "emerald": "smeraldo", "quartz": "quarzo", "voltite": "voltite",
                "redstone": "redstone", "glowstone": "pietra luminosa"}
FORME_TAG = {  # c:<forma>/<materiale> -> (nome, id FutureTek che fa da icona)
    "ingots": ("Lingotto di {m}", "{m}_ingot"), "nuggets": ("Pepita di {m}", "{m}_nugget"),
    "dusts": ("Polvere di {m}", "{m}_dust"), "plates": ("Piastra di {m}", "{m}_plate"),
    "raw_materials": ("{M} grezzo", "raw_{m}"), "ores": ("Minerale di {m}", "{m}_ore"),
    "wires": ("Filo di {m}", "{m}_wire"), "gems": ("{M}", None),
}
TAG_SPECIALI = {
    "c:rods/wooden": ("Bastoncino", "#9a6a3a"), "c:glass_blocks": ("Vetro (qualsiasi)", "#c8e4ec"),
    "c:glass_panes": ("Pannello di vetro", "#c8e4ec"), "c:chests/wooden": ("Baule", "#9a6a3a"),
    "c:cobblestones": ("Pietrisco", "#7a7a7a"), "c:gravels": ("Ghiaia", "#8a8480"), "c:sands": ("Sabbia", "#dccf9e"),
    "c:stones": ("Pietra", "#7e7e7e"), "minecraft:coals": ("Carbone o carbonella", "#2e2e32"), "c:obsidians": ("Ossidiana", "#1e1428"), "minecraft:planks": ("Assi (qualsiasi)", "#b08850"),
    "minecraft:saplings": ("Alberello (qualsiasi)", "#4a8a3a"), "minecraft:wool": ("Lana (qualsiasi)", "#ececec"),
    "c:gems/diamond": ("Diamante", "#5ee0e0"), "c:gems/emerald": ("Smeraldo", "#30c060"),
    "c:gems/quartz": ("Quarzo", "#ece4dc"), "c:gems/voltite": ("Frammento di voltite", None),
    "c:dusts/redstone": ("Redstone", "#c02018"), "c:dusts/glowstone": ("Polvere di pietra luminosa", "#f0d870"),
}
COLORI_VANILLA_MAT = {"iron": "#d8d8d8", "gold": "#f0c840", "copper": "#d27c50", "coal": "#2e2e32"}

# --- icone (texture della mod, mai quelle di Minecraft) ---
_icone = {}


def png_uri(img):
    buf = io.BytesIO()
    img.save(buf, "PNG", optimize=True)
    return "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()


def tex(path):
    """'futuretek:block/x' -> immagine 16x16"""
    if not path.startswith("futuretek:"):
        return None
    p = TEX / (path.split(":", 1)[1] + ".png")
    if not p.exists():
        return None
    img = Image.open(p).convert("RGBA")
    return img.crop((0, 0, 16, 16))


def cubo(top, left, right):
    """Cubo isometrico 32x32 (come un blocco visto in un inventario)"""
    out = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    for y in range(32):
        for x in range(32):
            # faccia sinistra
            if x < 16:
                v = (y - 8 - x * 0.5) / 16
                if 0 <= v < 1 and y >= 8 + x * 0.5 - 0.01:
                    c = left.getpixel((min(15, int(x)), min(15, int(v * 16))))
                    out.putpixel((x, y), tuple(int(k * 0.82) for k in c[:3]) + (255,))
                    continue
            else:
                v = (y - 16 + (x - 16) * 0.5) / 16
                if 0 <= v < 1:
                    c = right.getpixel((min(15, int(x - 16)), min(15, int(v * 16))))
                    out.putpixel((x, y), tuple(int(k * 0.62) for k in c[:3]) + (255,))
                    continue
            u = (x - 16) / 32 + y / 16
            w = y / 16 - (x - 16) / 32
            if 0 <= u < 1 and 0 <= w < 1:
                c = top.getpixel((min(15, int(u * 16)), min(15, int(w * 16))))
                out.putpixel((x, y), c[:3] + (255,))
    return out


def modello(nome):
    p = MOD / (nome.split(":", 1)[1] + ".json") if ":" in nome else None
    if p is None or not p.exists():
        return None
    return json.loads(p.read_text(encoding="utf-8"))


def icona(id_):
    """Icona di un oggetto FutureTek (data URI) o None"""
    if id_ in _icone:
        return _icone[id_]
    nome = id_.split(":")[1]
    img = None
    m = modello(f"futuretek:item/{nome}")
    if m:
        t = m.get("textures", {})
        if "layer0" in t:
            img = tex(t["layer0"])
        elif m.get("parent", "").startswith("futuretek:block/"):
            b = modello(m["parent"].replace("futuretek:block/", "futuretek:block/"))
            bt = b.get("textures", {}) if b else {}
            par = b.get("parent", "") if b else ""
            if "all" in bt and not b.get("elements") and "cube_all" in par:
                f = tex(bt["all"])
                img = cubo(f, f, f) if f else None
            elif "front" in bt and "side" in bt:
                img = cubo(tex(bt.get("top", bt["side"])), tex(bt["front"]), tex(bt["side"]))
            elif "top" in bt and "side" in bt and "cube" in par:
                img = cubo(tex(bt["top"]), tex(bt["side"]), tex(bt["side"]))
            else:
                for k in ("all", "cable", "texture", "rail", "front", "side", "top", "cross", "particle"):
                    if k in bt and tex(bt[k]) is not None:
                        img = tex(bt[k])
                        break
    if img is None:
        for cart in ("item", "block"):
            if (TEX / cart / f"{nome}.png").exists():
                img = tex(f"futuretek:{cart}/{nome}")
                break
    _icone[id_] = png_uri(img) if img else None
    return _icone[id_]


def nome_ft(id_):
    n = id_.split(":")[1]
    return LINGUA.get(f"item.futuretek.{n}") or LINGUA.get(f"block.futuretek.{n}") or n


def descrivi(ing):
    """Ingrediente -> {n: nome, i: icona o None, c: colore, ft: id FutureTek per i collegamenti}"""
    if isinstance(ing, list):
        ing = ing[0]
    if "tag" in ing:
        t = ing["tag"]
        if t in TAG_SPECIALI:
            n, col = TAG_SPECIALI[t]
            if t == "c:gems/voltite":
                return {"n": n, "i": icona("futuretek:voltite_shard"), "ft": "futuretek:voltite_shard"}
            return {"n": n, "c": col}
        if t.startswith("c:storage_blocks/raw_"):
            m = t.split("raw_")[1]
            fid = f"futuretek:raw_{m}_block"
            if (MOD / f"item/raw_{m}_block.json").exists():
                return {"n": nome_ft(fid), "i": icona(fid), "ft": fid}
            return {"n": f"Blocco di {MATERIALI_IT.get(m, m)} grezzo", "c": COLORI_VANILLA_MAT.get(m, "#999")}
        forma, _, m = t.partition(":")[2].partition("/")
        if forma in FORME_TAG:
            n_t, id_t = FORME_TAG[forma]
            mi = MATERIALI_IT.get(m, m)
            n = n_t.format(m=mi, M=mi.capitalize())
            if id_t:
                fid = "futuretek:" + id_t.format(m=m)
                if (MOD / f"item/{fid.split(':')[1]}.json").exists():
                    return {"n": n, "i": icona(fid), "ft": fid}
            return {"n": n, "c": COLORI_VANILLA_MAT.get(m, "#9a9a9a")}
        return {"n": t, "c": "#9a9a9a"}
    id_ = ing.get("item") or ing.get("id")
    if id_.startswith("futuretek:"):
        return {"n": nome_ft(id_), "i": icona(id_), "ft": id_}
    n, col = VANILLA.get(id_.split(":")[1], (id_.split(":")[1].replace("_", " "), "#9a9a9a"))
    return {"n": n, "c": col}


# --- categorie (in ordine di gioco) ---
CATEGORIE = [
    ("materiali", "Materiali, metalli e bauli", "Lingotti, pepite, piastre, polveri, leghe, blocchi, resina e bauli grandi."),
    ("attrezzi", "Attrezzi e armature", "Bronzo e acciaio, martello, tagliacavi, rubinetto, rilevatori di minerali e sacca del minatore."),
    ("energia", "Energia", "Generatori, cavi, accumulatori, celle e componenti elettrici."),
    ("macchine", "Macchine", "Fornace di ferro, macchine di lavorazione e potenziamenti."),
    ("lavorazioni", "Lavorazioni delle macchine", "Cosa entra e cosa esce da ogni macchina."),
    ("logistica", "Logistica e logica", "Condotti, smistamento, terminale, crafting automatico e porte logiche."),
    ("avanzata", "Tecnologia avanzata", "Reattore, escavatore, esoarmatura e zaino a reazione."),
    ("ferrovie", "Ferrovie", "Binari speciali e carrello merci."),
    ("agricoltura", "Agricoltura e api", "Coltivazioni automatiche, fertilizzante e arnia."),
    ("alchimia", "Alchimia (Essenza)", "Trasmutazione, braci, Nocturnite, Ardorite, anelli e amuleti."),
    ("esplorazione", "Esplorazione", "Il Margine Cristallino."),
]


def categoria(out, nome_file):
    o = out.split(":")[1]
    if nome_file.startswith("shift_") or any(k in o for k in [
            "essence", "ember", "nocturnite", "ardorite", "synthesis", "star_collector", "replicator", "shifting",
            "catalyst", "lens", "satchel", "_ring", "hourglass", "amulet", "memory_gem", "dowsing", "builder_wand",
            "talisman", "tome", "star_charge", "repulsion"]):
        return "alchimia"
    if any(k in o for k in ["sword", "pickaxe", "_axe", "shovel", "_hoe", "helmet", "chestplate", "leggings", "boots",
                            "forge_hammer", "cutters", "tree_tap", "scanner", "scanning_dust", "miner_bag"]) \
            and not o.startswith("exo_"):
        return "attrezzi"
    if o.endswith("_chest"):
        return "materiali"
    if o.startswith("exo_") or any(k in o for k in ["reactor", "fuel_rod", "heat_vent", "coolant", "quarry", "jetpack",
                                                    "mining_mole", "area_marker"]):
        return "avanzata"
    if "rail" in o or "cargo" in o:
        return "ferrovie"
    if any(k in o for k in ["conduit", "terminal", "auto_crafter", "_gate", "logistics_core"]):
        return "logistica"
    if any(k in o for k in ["growth", "harvester", "fertilizer", "apiary"]):
        return "agricoltura"
    if "edge_anchor" in o:
        return "esplorazione"
    if any(k in o for k in ["iron_furnace", "charcoal_kiln", "advanced_crusher", "induction_furnace", "crusher", "electric_furnace", "compressor", "extractor", "wire_press", "chemical_bath",
                            "thermal_centrifuge", "upgrade"]):
        return "macchine"
    if any(k in o for k in ["generator", "solar", "water_mill", "wind_mill", "battery", "cable", "fiber", "_wire", "circuit", "machine_frame",
                            "wrench", "multimeter"]):
        return "energia"
    return "materiali"


MACCHINE = {"crushing": "crusher", "compressing": "compressor", "extracting": "extractor",
            "wire_pressing": "wire_press", "washing": "chemical_bath", "centrifuging": "thermal_centrifuge"}


def raccogli():
    ricette = []
    cottura = {}
    for f in sorted(RIC.rglob("*.json")):
        d = json.loads(f.read_text(encoding="utf-8"))
        t = d["type"]
        if t == "minecraft:crafting_shaped":
            out = d["result"]["id"]
            chiavi = {k: descrivi(v) for k, v in d["key"].items()}
            griglia = [[chiavi.get(ch) if ch != " " else None for ch in riga.ljust(3)] for riga in d["pattern"]]
            while len(griglia) < 3:
                griglia.append([None, None, None])
            ricette.append({"k": "banco", "g": griglia, "o": descrivi({"item": out}), "q": d["result"].get("count", 1),
                            "cat": categoria(out, f.stem), "id": out})
        elif t == "minecraft:crafting_shapeless":
            out = d["result"]["id"]
            ing = [descrivi(i) for i in d["ingredients"]]
            g = [[ing[r * 3 + c] if r * 3 + c < len(ing) else None for c in range(3)] for r in range(3)]
            ricette.append({"k": "senza", "g": g, "o": descrivi({"item": out}), "q": d["result"].get("count", 1),
                            "cat": categoria(out, f.stem), "id": out})
        elif t in ("minecraft:smelting", "minecraft:blasting"):
            out = d["result"]["id"]
            chiave = (json.dumps(d["ingredient"], sort_keys=True), out)
            cottura.setdefault(chiave, {"k": "forno", "in": descrivi(d["ingredient"]), "o": descrivi({"item": out}),
                                        "q": 1, "cat": categoria(out, f.stem), "id": out, "alto": False})
            if t == "minecraft:blasting":
                cottura[chiave]["alto"] = True
        elif t.startswith("futuretek:"):
            tipo = t.split(":")[1]
            m = "futuretek:" + MACCHINE[tipo]
            r = {"k": "macchina", "m": tipo, "mn": nome_ft(m), "mi": icona(m), "in": descrivi(d["ingredient"]),
                 "qi": d["ingredient"].get("count", 1), "o": descrivi({"id": d["result"]["id"]}),
                 "q": d["result"].get("count", 1), "cat": "lavorazioni", "id": d["result"]["id"]}
            if "secondary" in d:
                s = d["secondary"]
                r["x"] = descrivi({"id": s["item"]["id"]})
                r["xq"] = s["item"].get("count", 1)
                r["xp"] = round(s["chance"] * 100)
            ricette.append(r)
    ricette += list(cottura.values())
    return ricette


def main():
    ricette = raccogli()
    dati = {"categorie": [{"id": c, "nome": n, "desc": d} for c, n, d in CATEGORIE],
            "macchine": [{"id": k, "nome": nome_ft("futuretek:" + v), "icona": icona("futuretek:" + v)}
                         for k, v in MACCHINE.items()],
            "ricette": ricette}
    modello_html = (ROOT / "tools/guida_modello.html").read_text(encoding="utf-8")
    html = modello_html.replace("/*__DATI__*/null", json.dumps(dati, ensure_ascii=False, separators=(",", ":")))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(html, encoding="utf-8")
    print(f"Guida: {len(ricette)} ricette, {sum(1 for v in _icone.values() if v)} icone -> {OUT}")


if __name__ == "__main__":
    main()
