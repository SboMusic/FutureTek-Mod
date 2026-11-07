"""
FutureTek - Generatore di risorse
=================================
Crea automaticamente:
  * texture segnaposto (PNG 16x16) in  src/main/resources/assets/futuretek/textures/
    -> NON sovrascrive quelle che esistono gia': se disegni una texture tua, resta tua.
       Per rigenerarle tutte: python tools/genera_risorse.py --forza-texture
  * modelli, stati dei blocchi, traduzioni   in src/generated/resources/assets/futuretek/
  * ricette, tag, loot table                  in src/generated/resources/data/
    -> questa cartella viene SVUOTATA e ricreata ogni volta: non modificare a mano
       i file li' dentro, modifica questo script.

Uso (dalla cartella del progetto):  python tools/genera_risorse.py
Richiede Pillow:                     pip install pillow

ATTENZIONE: gli elenchi qui sotto devono corrispondere a quelli nel codice Java
(com.futuretek.registry.Metal / ModItems / ModBlocks).
"""
import json
import random
import shutil
import sys
from pathlib import Path

from PIL import Image

MODID = "futuretek"
ROOT = Path(__file__).resolve().parent.parent
GEN = ROOT / "src/generated/resources"
TEX = ROOT / "src/main/resources/assets" / MODID / "textures"
FORZA_TEXTURE = "--forza-texture" in sys.argv

# ---------------------------------------------------------------------------
# DATI
# ---------------------------------------------------------------------------
# id: (nome inglese, nome italiano, colore base RGB)
MATERIALI = {
    "tin":      ("Tin",      "stagno",    (196, 206, 214)),
    "lead":     ("Lead",     "piombo",    (92, 98, 128)),
    "silver":   ("Silver",   "argento",   (226, 232, 240)),
    "uranium":  ("Uranium",  "uranio",    (120, 196, 72)),
    "bronze":   ("Bronze",   "bronzo",    (205, 138, 62)),
    "steel":    ("Steel",    "acciaio",   (118, 124, 134)),
    "iron":     ("Iron",     "ferro",     (216, 216, 216)),
    "gold":     ("Gold",     "oro",       (245, 212, 66)),
    "copper":   ("Copper",   "rame",      (226, 123, 82)),
    "coal":     ("Coal",     "carbone",   (52, 52, 56)),
    "diamond":  ("Diamond",  "diamante",  (92, 230, 224)),
    "obsidian": ("Obsidian", "ossidiana", (64, 44, 92)),
}

# Metalli di FutureTek: id -> (ha minerale?, attrezzo minimo "stone"/"iron")
METALLI = {
    "tin":     (True, "stone"),
    "lead":    (True, "stone"),
    "silver":  (True, "iron"),
    "uranium": (True, "iron"),
    "bronze":  (False, "stone"),
    "steel":   (False, "iron"),
}
# Voltite: cristallo energetico originale
COLORE_VOLTITE = (190, 90, 255)

# Generazione dei minerali nel mondo (Overworld)
#   id: (altezza minima, altezza massima, vene per chunk, grandezza vena, attrezzo minimo)
# Modifica questi numeri e rilancia lo script per cambiare quanto sono comuni i minerali.
# (y minima, y massima, vene per chunk, grandezza della vena, attrezzo minimo)
# Novembre 2026: l'utente li trovava troppo rari -> misurati: stagno ~70/chunk, piombo ~45, argento ~20,
# voltite ~17, uranio ~11 (ferro di Minecraft ~65-90)
MINERALI_MONDO = {
    "tin":     (-16, 96, 17, 10, "stone"),
    "lead":    (-48, 64, 9, 9, "stone"),
    "silver":  (-64, 32, 4, 8, "iron"),
    "uranium": (-64, 16, 3, 6, "iron"),
    "voltite": (-64, 32, 4, 7, "iron"),
}
POLVERI_VANILLA = ["iron", "gold", "copper", "coal", "diamond", "obsidian"]
PIASTRE_VANILLA = ["iron", "gold", "copper"]

# ---------------------------------------------------------------------------
# DISEGNI DELLE TEXTURE (16x16)
#   .  trasparente     o  contorno     #  colore base
#   +  chiaro          -  scuro
# ---------------------------------------------------------------------------
FORME = {
    "ingot": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "....oooooooooo..",
        "...o++++++++++o.",
        "..o+#########-o.",
        ".o+##########-o.",
        ".o###########-o.",
        ".o-----------oo.",
        "..oooooooooooo..",
        "................",
        "................",
        "................",
        "................",
    ],
    "nugget": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        ".....ooo........",
        "....o++#o.......",
        "....o#--o.ooo...",
        ".....ooo.o+#-o..",
        "...ooo...o#--o..",
        "..o+#-o...ooo...",
        "..o#--o.........",
        "...ooo..........",
        "................",
    ],
    "dust": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        ".......oo.......",
        "......o++o......",
        ".....o+#+#o.....",
        "....o+#+##-o....",
        "...o+##+#+#-o...",
        "..o+#+###+#--o..",
        ".o+##+#+###+--o.",
        ".oooooooooooooo.",
        "................",
    ],
    "plate": [
        "................",
        "................",
        "..oooooooooooo..",
        "..o++++++++++o..",
        "..o+########-o..",
        "..o+#+######-o..",
        "..o+########-o..",
        "..o+########-o..",
        "..o+########-o..",
        "..o+########-o..",
        "..o+######+#-o..",
        "..o+########-o..",
        "..o----------o..",
        "..oooooooooooo..",
        "................",
        "................",
    ],
    "raw": [
        "................",
        "................",
        "................",
        "................",
        "......ooo.......",
        "....oo+++oo.....",
        "...o++#+##+o....",
        "...o+##+###-o...",
        "..o+#+##+##-o...",
        "..o+##+##+#--o..",
        "..o###+##+#--o..",
        "...o-##+##--o...",
        "....o---o--o....",
        ".....ooo.oo.....",
        "................",
        "................",
    ],
}

# Martello: colori fissi (H/h/d testa, W/w manico)
MARTELLO = [
    "................",
    "..oooooooo......",
    ".oHHHHHHHHo.....",
    ".ohhhhhhhho.....",
    ".oddddddddo.....",
    "..ooooWwooo.....",
    ".......oWwo.....",
    "........oWwo....",
    ".........oWwo...",
    "..........oWwo..",
    "...........oWwo.",
    "............oWo.",
    ".............o..",
    "................",
    "................",
    "................",
]
COLORI_MARTELLO = {
    "o": (40, 34, 30), "H": (200, 204, 210), "h": (150, 154, 162), "d": (96, 100, 108),
    "W": (150, 104, 60), "w": (110, 74, 40),
}


# ---------------------------------------------------------------------------
# FUNZIONI DI AIUTO
# ---------------------------------------------------------------------------
def mix(c, verso, quanto):
    return tuple(round(a + (b - a) * quanto) for a, b in zip(c, verso))


def palette(base):
    return {
        "#": base,
        "+": mix(base, (255, 255, 255), 0.35),
        "-": mix(base, (0, 0, 0), 0.30),
        "o": mix(base, (0, 0, 0), 0.60),
    }


def rumore(c, rng, forza=0.06):
    f = 1 + rng.uniform(-forza, forza)
    return tuple(max(0, min(255, round(v * f))) for v in c)


def disegna(disegno, colori, seme):
    assert len(disegno) == 16 and all(len(r) == 16 for r in disegno), "il disegno deve essere 16x16"
    rng = random.Random(seme)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, riga in enumerate(disegno):
        for x, ch in enumerate(riga):
            if ch == ".":
                continue
            c = colori[ch]
            if ch == "#":
                c = rumore(c, rng)
            img.putpixel((x, y), c + (255,))
    return img


def texture_blocco(base, seme):
    """Blocco di metallo: bordo in rilievo e 4 rivetti."""
    rng = random.Random(seme)
    p = palette(base)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            if x == 0 or y == 0:
                c = p["+"]
            elif x == 15 or y == 15:
                c = p["-"]
            elif x == 1 or y == 1:
                c = mix(base, (255, 255, 255), 0.15)
            elif x == 14 or y == 14:
                c = mix(base, (0, 0, 0), 0.15)
            else:
                c = rumore(base, rng, 0.04)
            img.putpixel((x, y), c + (255,))
    for (x, y) in [(3, 3), (12, 3), (3, 12), (12, 12)]:
        img.putpixel((x, y), p["+"] + (255,))
        img.putpixel((x + 1, y + 1), p["-"] + (255,))
    return img


def texture_blocco_grezzo(base, seme):
    """Blocco di grezzo: macchie irregolari."""
    rng = random.Random(seme)
    p = palette(base)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            r = rng.random()
            c = p["-"] if r < 0.25 else p["+"] if r > 0.85 else p["#"]
            img.putpixel((x, y), rumore(c, rng, 0.05) + (255,))
    return img


def texture_roccia(profonda, seme):
    """Sfondo di roccia disegnato da noi: grigio (pietra) o grigio scuro a strati (deepslate)."""
    rng = random.Random(seme)
    base = (70, 70, 78) if profonda else (122, 122, 122)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        strato = (0.92 if y % 4 == 0 else 1.0) if profonda else 1.0
        for x in range(16):
            r = rng.random()
            f = 0.85 if r < 0.2 else 1.12 if r > 0.9 else 1.0
            c = tuple(max(0, min(255, round(v * f * strato))) for v in base)
            img.putpixel((x, y), c + (255,))
    return img


# posizioni delle "pepite" di minerale dentro la roccia (x, y)
MACCHIE_MINERALE = [(3, 3), (10, 2), (6, 7), (12, 8), (2, 11), (8, 12), (13, 13)]


def texture_minerale(colore, profonda, seme):
    img = texture_roccia(profonda, seme + "_roccia")
    p = palette(colore)
    for (x, y) in MACCHIE_MINERALE:
        for dx, dy, ch in [(0, 0, "+"), (1, 0, "#"), (0, 1, "#"), (1, 1, "-")]:
            img.putpixel((x + dx, y + dy), p[ch] + (255,))
    return img


def salva_texture(img, percorso):
    percorso.parent.mkdir(parents=True, exist_ok=True)
    if percorso.exists() and not FORZA_TEXTURE:
        return False
    img.save(percorso)
    return True


def scrivi_json(percorso_relativo, dati):
    p = GEN / percorso_relativo
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(dati, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


# ---------------------------------------------------------------------------
# RACCOLTA DI TUTTO CIO' CHE VA GENERATO
# ---------------------------------------------------------------------------
oggetti = []          # (id, modello: "generated"/"handheld"/"block")
blocchi = []          # (id, texture "cube_all")
lingua_en = {}
lingua_it = {}
tag_oggetti = {}      # "namespace:percorso" -> [valori]
tag_blocchi = {}
ricette = {}          # nome -> json
worldgen = {}         # percorso -> json (generazione del mondo)
modelli_speciali = {} # id blocco -> funzione che scrive blockstate + modelli
loot_speciali = {}    # id blocco -> loot table particolare
texture_create = 0


def tag(dizionario, nome, valore):
    dizionario.setdefault(nome, [])
    if valore not in dizionario[nome]:
        dizionario[nome].append(valore)


def oggetto(id_, en, it, modello="generated"):
    oggetti.append((id_, modello))
    lingua_en[f"item.{MODID}.{id_}"] = en
    lingua_it[f"item.{MODID}.{id_}"] = it


def blocco(id_, en, it):
    blocchi.append(id_)
    lingua_en[f"block.{MODID}.{id_}"] = en
    lingua_it[f"block.{MODID}.{id_}"] = it


def ing(x):
    """Ingrediente: '#c:ingots/tin' = tag, altrimenti oggetto."""
    return {"tag": x[1:]} if x.startswith("#") else {"item": x}


def r_senza_forma(nome, ingredienti, risultato, quantita=1):
    ricette[nome] = {
        "type": "minecraft:crafting_shapeless",
        "category": "misc",
        "ingredients": [ing(i) for i in ingredienti],
        "result": {"count": quantita, "id": risultato},
    }


def r_con_forma(nome, schema, chiavi, risultato, quantita=1):
    ricette[nome] = {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "key": {k: ing(v) for k, v in chiavi.items()},
        "pattern": schema,
        "result": {"count": quantita, "id": risultato},
    }


def r_cottura(nome, ingrediente, risultato, esperienza=0.5, fornace=True, altoforno=True):
    if fornace:
        ricette[nome + "_smelting"] = {
            "type": "minecraft:smelting", "category": "misc", "cookingtime": 200,
            "experience": esperienza, "ingredient": ing(ingrediente), "result": {"id": risultato},
        }
    if altoforno:
        ricette[nome + "_blasting"] = {
            "type": "minecraft:blasting", "category": "misc", "cookingtime": 100,
            "experience": esperienza, "ingredient": ing(ingrediente), "result": {"id": risultato},
        }


def ft(id_):
    return f"{MODID}:{id_}"


MARTELLO_ID = ft("forge_hammer")

# --- Metalli di FutureTek ---
for m, (da_minerale, attrezzo) in METALLI.items():
    en, it, colore = MATERIALI[m]
    forme = []
    if da_minerale:
        forme.append(("raw", f"raw_{m}", f"Raw {en}", f"{it.capitalize()} grezzo", "raw_materials"))
    forme += [
        ("ingot", f"{m}_ingot", f"{en} Ingot", f"Lingotto di {it}", "ingots"),
        ("nugget", f"{m}_nugget", f"{en} Nugget", f"Pepita di {it}", "nuggets"),
        ("dust", f"{m}_dust", f"{en} Dust", f"Polvere di {it}", "dusts"),
        ("plate", f"{m}_plate", f"{en} Plate", f"Piastra di {it}", "plates"),
    ]
    for forma, id_, n_en, n_it, gruppo in forme:
        oggetto(id_, n_en, n_it)
        if salva_texture(disegna(FORME[forma], palette(colore), id_), TEX / "item" / f"{id_}.png"):
            texture_create += 1
        tag(tag_oggetti, f"c:{gruppo}/{m}", ft(id_))
        tag(tag_oggetti, f"c:{gruppo}", f"#c:{gruppo}/{m}")

    # blocchi
    nomi_blocchi = [(f"{m}_block", f"Block of {en}", f"Blocco di {it}", f"storage_blocks/{m}", texture_blocco)]
    if da_minerale:
        nomi_blocchi.append((f"raw_{m}_block", f"Block of Raw {en}", f"Blocco di {it} grezzo",
                             f"storage_blocks/raw_{m}", texture_blocco_grezzo))
    for id_, n_en, n_it, gruppo, funzione in nomi_blocchi:
        blocco(id_, n_en, n_it)
        if salva_texture(funzione(colore, id_), TEX / "block" / f"{id_}.png"):
            texture_create += 1
        for d in (tag_oggetti, tag_blocchi):
            tag(d, f"c:{gruppo}", ft(id_))
            tag(d, "c:storage_blocks", f"#c:{gruppo}")
        tag(tag_blocchi, "minecraft:mineable/pickaxe", ft(id_))
        tag(tag_blocchi, f"minecraft:needs_{attrezzo}_tool", ft(id_))

    # ricette: blocco <-> lingotti, lingotto <-> pepite, piastra, polvere -> lingotto
    r_con_forma(f"{m}_block", ["###", "###", "###"], {"#": f"#c:ingots/{m}"}, ft(f"{m}_block"))
    r_senza_forma(f"{m}_ingot_from_block", [ft(f"{m}_block")], ft(f"{m}_ingot"), 9)
    r_con_forma(f"{m}_ingot_from_nuggets", ["###", "###", "###"], {"#": f"#c:nuggets/{m}"}, ft(f"{m}_ingot"))
    r_senza_forma(f"{m}_nugget", [f"#c:ingots/{m}"], ft(f"{m}_nugget"), 9)
    r_senza_forma(f"{m}_plate", [MARTELLO_ID, f"#c:ingots/{m}"], ft(f"{m}_plate"))
    # l'acciaio si fonde solo nell'altoforno
    r_cottura(f"{m}_ingot_from_dust", f"#c:dusts/{m}", ft(f"{m}_ingot"), 0.5, fornace=(m != "steel"))
    if da_minerale:
        r_con_forma(f"raw_{m}_block", ["###", "###", "###"], {"#": f"#c:raw_materials/{m}"}, ft(f"raw_{m}_block"))
        r_senza_forma(f"raw_{m}_from_block", [ft(f"raw_{m}_block")], ft(f"raw_{m}"), 9)
        r_cottura(f"{m}_ingot_from_raw", f"#c:raw_materials/{m}", ft(f"{m}_ingot"), 0.7)
        r_senza_forma(f"{m}_dust_from_raw", [MARTELLO_ID, f"#c:raw_materials/{m}"], ft(f"{m}_dust"))

# --- Leghe ---
r_senza_forma("bronze_dust", ["#c:dusts/copper", "#c:dusts/copper", "#c:dusts/copper", "#c:dusts/tin"],
              ft("bronze_dust"), 4)
r_senza_forma("steel_dust", ["#c:dusts/iron", "#c:dusts/coal", "#c:dusts/coal"], ft("steel_dust"))

# --- Polveri e piastre di materiali di Minecraft ---
for m in POLVERI_VANILLA:
    en, it, colore = MATERIALI[m]
    id_ = f"{m}_dust"
    oggetto(id_, f"{en} Dust", f"Polvere di {it}")
    if salva_texture(disegna(FORME["dust"], palette(colore), id_), TEX / "item" / f"{id_}.png"):
        texture_create += 1
    tag(tag_oggetti, f"c:dusts/{m}", ft(id_))
    tag(tag_oggetti, "c:dusts", f"#c:dusts/{m}")
for m in ["iron", "gold", "copper"]:
    r_senza_forma(f"{m}_dust_from_raw", [MARTELLO_ID, f"#c:raw_materials/{m}"], ft(f"{m}_dust"))
    r_cottura(f"{m}_ingot_from_dust", f"#c:dusts/{m}", f"minecraft:{m}_ingot", 0.5)
r_senza_forma("coal_dust", [MARTELLO_ID, "minecraft:coal"], ft("coal_dust"))

for m in PIASTRE_VANILLA:
    en, it, colore = MATERIALI[m]
    id_ = f"{m}_plate"
    oggetto(id_, f"{en} Plate", f"Piastra di {it}")
    if salva_texture(disegna(FORME["plate"], palette(colore), id_), TEX / "item" / f"{id_}.png"):
        texture_create += 1
    tag(tag_oggetti, f"c:plates/{m}", ft(id_))
    tag(tag_oggetti, "c:plates", f"#c:plates/{m}")
    r_senza_forma(f"{m}_plate", [MARTELLO_ID, f"#c:ingots/{m}"], ft(id_))

# --- Minerali e generazione nel mondo ---
FRAMMENTO = [
    "................",
    "................",
    "..........oo....",
    ".........o+#o...",
    "........o+##o...",
    ".......o+##-o...",
    "......o+##-o....",
    ".....o+##-o.....",
    "....o+##-o......",
    "...o+##-o.......",
    "...o##-o........",
    "...o#-o.........",
    "....oo..........",
    "................",
    "................",
    "................",
]
oggetto("voltite_shard", "Voltite Shard", "Frammento di voltite")
if salva_texture(disegna(FRAMMENTO, palette(COLORE_VOLTITE), "voltite_shard"), TEX / "item" / "voltite_shard.png"):
    texture_create += 1
tag(tag_oggetti, "c:gems/voltite", ft("voltite_shard"))
tag(tag_oggetti, "c:gems", "#c:gems/voltite")

loot_minerali = {}  # id blocco -> json della loot table (sovrascrive quella semplice)
SETA = {"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
    {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}

for m, (y_min, y_max, vene, grandezza, attrezzo) in MINERALI_MONDO.items():
    if m == "voltite":
        en, it, colore = "Voltite", "voltite", COLORE_VOLTITE
        drop, drop_funzioni = ft("voltite_shard"), [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 2.0, "max": 4.0}},
            {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune",
             "formula": "minecraft:uniform_bonus_count", "parameters": {"bonusMultiplier": 1}},
            {"function": "minecraft:explosion_decay"}]
        cottura = ft("voltite_shard")
        n_en, n_it = "Voltite Ore", "Minerale di voltite"
        nd_en, nd_it = "Deepslate Voltite Ore", "Minerale di voltite in deepslate"
    else:
        en, it, colore = MATERIALI[m]
        drop, drop_funzioni = ft(f"raw_{m}"), [
            {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
            {"function": "minecraft:explosion_decay"}]
        cottura = ft(f"{m}_ingot")
        n_en, n_it = f"{en} Ore", f"Minerale di {it}"
        nd_en, nd_it = f"Deepslate {en} Ore", f"Minerale di {it} in deepslate"

    for id_, nome_en, nome_it, profonda, terreno in [
        (f"{m}_ore", n_en, n_it, False, "stone"),
        (f"deepslate_{m}_ore", nd_en, nd_it, True, "deepslate"),
    ]:
        blocco(id_, nome_en, nome_it)
        if salva_texture(texture_minerale(colore, profonda, id_), TEX / "block" / f"{id_}.png"):
            texture_create += 1
        for d in (tag_oggetti, tag_blocchi):
            tag(d, f"c:ores/{m}", ft(id_))
            tag(d, "c:ores", f"#c:ores/{m}")
            tag(d, f"c:ores_in_ground/{terreno}", ft(id_))
        tag(tag_blocchi, "minecraft:mineable/pickaxe", ft(id_))
        tag(tag_blocchi, f"minecraft:needs_{attrezzo}_tool", ft(id_))
        loot_minerali[id_] = {
            "type": "minecraft:block",
            "pools": [{
                "bonus_rolls": 0.0,
                "entries": [{"type": "minecraft:alternatives", "children": [
                    {"type": "minecraft:item", "conditions": [SETA], "name": ft(id_)},
                    {"type": "minecraft:item", "functions": drop_funzioni, "name": drop},
                ]}],
                "rolls": 1.0,
            }],
            "random_sequence": f"{MODID}:blocks/{id_}",
        }

    r_cottura(f"{m}_from_ore", f"#c:ores/{m}", cottura, 0.7)

    # generazione nel mondo: "configured feature" (cosa) + "placed feature" (dove) + "biome modifier" (in quali biomi)
    worldgen[f"data/{MODID}/worldgen/configured_feature/ore_{m}.json"] = {
        "type": "minecraft:ore",
        "config": {
            "discard_chance_on_air_exposure": 0.0,
            "size": grandezza,
            "targets": [
                {"state": {"Name": ft(f"{m}_ore")},
                 "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"}},
                {"state": {"Name": ft(f"deepslate_{m}_ore")},
                 "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"}},
            ],
        },
    }
    worldgen[f"data/{MODID}/worldgen/placed_feature/ore_{m}.json"] = {
        "feature": ft(f"ore_{m}"),
        "placement": [
            {"type": "minecraft:count", "count": vene},
            {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range", "height": {
                "type": "minecraft:uniform",
                "min_inclusive": {"absolute": y_min},
                "max_inclusive": {"absolute": y_max}}},
            {"type": "minecraft:biome"},
        ],
    }
    worldgen[f"data/{MODID}/neoforge/biome_modifier/ore_{m}.json"] = {
        "type": "neoforge:add_features",
        "biomes": "#minecraft:is_overworld",
        "features": ft(f"ore_{m}"),
        "step": "underground_ores",
    }

# --- Albero della resina ---
LEGNO_CORTECCIA = (96, 72, 54)
LEGNO_INTERNO = (178, 134, 86)
COLORE_FOGLIE = (74, 142, 66)
COLORE_RESINA = (232, 156, 44)
COLORE_GOMMA = (52, 52, 58)


def texture_corteccia(seme, con_resina=False):
    rng = random.Random(seme)
    img = Image.new("RGBA", (16, 16))
    scure = {rng.randrange(16) for _ in range(5)}
    for x in range(16):
        for y in range(16):
            f = 0.78 if x in scure else 1.0
            f *= 1 + rng.uniform(-0.07, 0.07)
            c = tuple(max(0, min(255, round(v * f))) for v in LEGNO_CORTECCIA)
            img.putpixel((x, y), c + (255,))
    if con_resina:
        p = palette(COLORE_RESINA)
        # "vena" di resina: una colata verticale e alcune gocce
        for y in range(4, 12):
            img.putpixel((7, y), p["#"] + (255,))
            img.putpixel((8, y), p["+" if y < 8 else "-"] + (255,))
        for (x, y) in [(6, 6), (9, 9), (7, 12), (8, 13)]:
            img.putpixel((x, y), p["#"] + (255,))
    return img


def texture_sezione_tronco(seme):
    rng = random.Random(seme)
    img = Image.new("RGBA", (16, 16))
    chiaro = LEGNO_INTERNO
    scuro = mix(LEGNO_INTERNO, (0, 0, 0), 0.25)
    for y in range(16):
        for x in range(16):
            anello = min(x, y, 15 - x, 15 - y)
            if anello == 0:
                c = LEGNO_CORTECCIA
            else:
                c = scuro if anello % 2 == 0 else chiaro
            img.putpixel((x, y), rumore(c, rng, 0.04) + (255,))
    return img


def texture_assi(seme):
    rng = random.Random(seme)
    img = Image.new("RGBA", (16, 16))
    scuro = mix(LEGNO_INTERNO, (0, 0, 0), 0.35)
    for y in range(16):
        for x in range(16):
            giunta = (x == (3 if (y // 4) % 2 == 0 else 11))
            c = scuro if (y % 4 == 3 or giunta) else rumore(LEGNO_INTERNO, rng, 0.06)
            img.putpixel((x, y), c + (255,))
    return img


def texture_foglie(seme):
    rng = random.Random(seme)
    p = palette(COLORE_FOGLIE)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            r = rng.random()
            if r < 0.22:
                continue  # buco trasparente
            c = p["-"] if r < 0.45 else p["+"] if r > 0.9 else p["#"]
            img.putpixel((x, y), rumore(c, rng, 0.05) + (255,))
    return img


ALBERELLO = [
    "................",
    "................",
    "................",
    "......++........",
    ".....+##+..+#...",
    "....+###-.+##-..",
    "....+#-#-+##-...",
    ".....--+##--....",
    "......+#W#-.....",
    "...+#-.-W-......",
    "..+##-..W.......",
    "...--W.W........",
    ".....WWW........",
    "......W.........",
    "......W.........",
    "................",
]
RESINA = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "......oooo......",
    ".....o++##o.....",
    "....o+####-o....",
    "...o+######-o...",
    "...o+#####--o...",
    "...o######--o...",
    "....o#---o-o....",
    ".....ooo.oo.....",
    "................",
    "................",
    "................",
]
GOMMA = [
    "................",
    "................",
    "................",
    "......oooo......",
    "....oo++##oo....",
    "...o++####--o...",
    "..o+######---o..",
    "..o+#######--o..",
    "..o#######---o..",
    "..o######----o..",
    "...o####----o...",
    "....oo----oo....",
    "......oooo......",
    "................",
    "................",
    "................",
]
RUBINETTO = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "..oooooo........",
    "..oWWWWWoo......",
    "..owwwwwwWoo....",
    "..oooooowwWWo...",
    "........oowwWo..",
    "..........oowWo.",
    "............oo..",
    "............aa..",
    ".............a..",
    "................",
    "................",
]
COLORI_LEGNO = {"o": (46, 32, 22), "W": (150, 104, 60), "w": (110, 74, 40), "a": COLORE_RESINA}

blocco("resin_log", "Resin Log", "Tronco della resina")
blocco("resin_planks", "Resin Planks", "Assi della resina")
blocco("resin_leaves", "Resin Leaves", "Foglie della resina")
blocco("resin_sapling", "Resin Sapling", "Alberello della resina")
oggetto("sticky_resin", "Sticky Resin", "Resina grezza")
oggetto("rubber", "Rubber", "Gomma")
oggetto("tree_tap", "Tree Tap", "Rubinetto", "handheld")

for img, nome in [
    (texture_corteccia("resin_log"), "block/resin_log"),
    (texture_corteccia("resin_log", con_resina=True), "block/resin_log_resin"),
    (texture_sezione_tronco("resin_log_top"), "block/resin_log_top"),
    (texture_assi("resin_planks"), "block/resin_planks"),
    (texture_foglie("resin_leaves"), "block/resin_leaves"),
    (disegna(ALBERELLO, {**palette(COLORE_FOGLIE), "W": (110, 74, 40)}, "resin_sapling"), "block/resin_sapling"),
    (disegna(RESINA, palette(COLORE_RESINA), "sticky_resin"), "item/sticky_resin"),
    (disegna(GOMMA, palette(COLORE_GOMMA), "rubber"), "item/rubber"),
    (disegna(RUBINETTO, COLORI_LEGNO, "tree_tap"), "item/tree_tap"),
]:
    if salva_texture(img, TEX / f"{nome}.png"):
        texture_create += 1



def modelli_tronco():
    A_ = f"assets/{MODID}"
    for nome, lato in [("resin_log", "resin_log"), ("resin_log_resin", "resin_log_resin")]:
        scrivi_json(f"{A_}/models/block/{nome}.json", {"parent": "minecraft:block/cube_column", "textures": {
            "end": f"{MODID}:block/resin_log_top", "side": f"{MODID}:block/{lato}"}})
        scrivi_json(f"{A_}/models/block/{nome}_horizontal.json", {
            "parent": "minecraft:block/cube_column_horizontal", "textures": {
                "end": f"{MODID}:block/resin_log_top", "side": f"{MODID}:block/{lato}"}})
    varianti = {}
    for resina in ["false", "true"]:
        base = f"{MODID}:block/resin_log" + ("_resin" if resina == "true" else "")
        varianti[f"axis=y,resin={resina}"] = {"model": base}
        varianti[f"axis=z,resin={resina}"] = {"model": base + "_horizontal", "x": 90}
        varianti[f"axis=x,resin={resina}"] = {"model": base + "_horizontal", "x": 90, "y": 90}
    scrivi_json(f"{A_}/blockstates/resin_log.json", {"variants": varianti})
    scrivi_json(f"{A_}/models/item/resin_log.json", {"parent": f"{MODID}:block/resin_log"})


def modelli_foglie():
    A_ = f"assets/{MODID}"
    scrivi_json(f"{A_}/blockstates/resin_leaves.json", {"variants": {"": {"model": f"{MODID}:block/resin_leaves"}}})
    scrivi_json(f"{A_}/models/block/resin_leaves.json", {"parent": "minecraft:block/cube_all",
                                                         "render_type": "minecraft:cutout_mipped",
                                                         "textures": {"all": f"{MODID}:block/resin_leaves"}})
    scrivi_json(f"{A_}/models/item/resin_leaves.json", {"parent": f"{MODID}:block/resin_leaves"})


def modelli_alberello():
    A_ = f"assets/{MODID}"
    scrivi_json(f"{A_}/blockstates/resin_sapling.json", {"variants": {"": {"model": f"{MODID}:block/resin_sapling"}}})
    scrivi_json(f"{A_}/models/block/resin_sapling.json", {"parent": "minecraft:block/cross",
                                                          "render_type": "minecraft:cutout",
                                                          "textures": {"cross": f"{MODID}:block/resin_sapling"}})
    scrivi_json(f"{A_}/models/item/resin_sapling.json", {"parent": "minecraft:item/generated",
                                                         "textures": {"layer0": f"{MODID}:block/resin_sapling"}})


modelli_speciali.update({"resin_log": modelli_tronco, "resin_leaves": modelli_foglie,
                         "resin_sapling": modelli_alberello})

CESOIE_O_SETA = {"condition": "minecraft:any_of", "terms": [
    {"condition": "minecraft:match_tool", "predicate": {"items": "minecraft:shears"}}, SETA]}
loot_speciali["resin_leaves"] = {
    "type": "minecraft:block",
    "pools": [
        {"bonus_rolls": 0.0, "rolls": 1.0, "entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "conditions": [CESOIE_O_SETA], "name": ft("resin_leaves")},
            {"type": "minecraft:item", "name": ft("resin_sapling"), "conditions": [
                {"condition": "minecraft:survives_explosion"},
                {"condition": "minecraft:table_bonus", "enchantment": "minecraft:fortune",
                 "chances": [0.05, 0.0625, 0.083333336, 0.1]}]},
        ]}]},
        {"bonus_rolls": 0.0, "rolls": 1.0,
         "conditions": [{"condition": "minecraft:inverted", "term": CESOIE_O_SETA}],
         "entries": [{"type": "minecraft:item", "name": "minecraft:stick", "functions": [
             {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1.0, "max": 2.0}},
             {"function": "minecraft:explosion_decay"}],
             "conditions": [{"condition": "minecraft:table_bonus", "enchantment": "minecraft:fortune",
                             "chances": [0.02, 0.022222223, 0.025, 0.033333335, 0.1]}]}]},
    ],
    "random_sequence": f"{MODID}:blocks/resin_leaves",
}

for d in (tag_oggetti, tag_blocchi):
    tag(d, "minecraft:logs_that_burn", ft("resin_log"))
    tag(d, "minecraft:planks", ft("resin_planks"))
    tag(d, "minecraft:leaves", ft("resin_leaves"))
    tag(d, "minecraft:saplings", ft("resin_sapling"))
tag(tag_blocchi, "minecraft:mineable/axe", ft("resin_log"))
tag(tag_blocchi, "minecraft:mineable/axe", ft("resin_planks"))
tag(tag_blocchi, "minecraft:mineable/hoe", ft("resin_leaves"))
tag(tag_oggetti, "c:rubbers", ft("rubber"))  # tag usato da altre mod tecnologiche per la gomma

r_senza_forma("resin_planks", [ft("resin_log")], ft("resin_planks"), 4)
r_con_forma("tree_tap", [" P ", "PPP", "P  "], {"P": "#minecraft:planks"}, ft("tree_tap"))
r_cottura("rubber_from_resin", ft("sticky_resin"), ft("rubber"), 0.3, altoforno=False)

# l'albero: tronco dritto, chioma a cono; 1 tronco su 4 nasce con la vena di resina
worldgen[f"data/{MODID}/worldgen/configured_feature/resin_tree.json"] = {
    "type": "minecraft:tree",
    "config": {
        "decorators": [],
        "dirt_provider": {"type": "minecraft:simple_state_provider", "state": {"Name": "minecraft:dirt"}},
        "foliage_placer": {
            "type": "minecraft:spruce_foliage_placer",
            "offset": {"type": "minecraft:uniform", "min_inclusive": 0, "max_inclusive": 2},
            "radius": {"type": "minecraft:uniform", "min_inclusive": 2, "max_inclusive": 3},
            "trunk_height": {"type": "minecraft:uniform", "min_inclusive": 1, "max_inclusive": 2},
        },
        "foliage_provider": {"type": "minecraft:simple_state_provider", "state": {
            "Name": ft("resin_leaves"),
            "Properties": {"distance": "7", "persistent": "false", "waterlogged": "false"}}},
        "force_dirt": False,
        "ignore_vines": True,
        "minimum_size": {"type": "minecraft:two_layers_feature_size", "limit": 2, "lower_size": 0, "upper_size": 2},
        "trunk_placer": {"type": "minecraft:straight_trunk_placer", "base_height": 6,
                         "height_rand_a": 3, "height_rand_b": 1},
        "trunk_provider": {"type": "minecraft:weighted_state_provider", "entries": [
            {"data": {"Name": ft("resin_log"), "Properties": {"axis": "y", "resin": "false"}}, "weight": 3},
            {"data": {"Name": ft("resin_log"), "Properties": {"axis": "y", "resin": "true"}}, "weight": 1},
        ]},
    },
}
# quanti alberi: in media 1 ogni RARITA' chunk dei biomi scelti
RARITA_ALBERO = 2
worldgen[f"data/{MODID}/worldgen/placed_feature/resin_tree.json"] = {
    "feature": ft("resin_tree"),
    "placement": [
        {"type": "minecraft:rarity_filter", "chance": RARITA_ALBERO},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0},
        {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"},
        {"type": "minecraft:biome"},
        {"type": "minecraft:block_predicate_filter", "predicate": {
            "type": "minecraft:would_survive", "state": {"Name": ft("resin_sapling"), "Properties": {"stage": "0"}}}},
    ],
}
for bioma in ["is_forest", "is_jungle", "is_taiga"]:
    worldgen[f"data/{MODID}/neoforge/biome_modifier/resin_tree_{bioma}.json"] = {
        "type": "neoforge:add_features",
        "biomes": f"#minecraft:{bioma}",
        "features": ft("resin_tree"),
        "step": "vegetal_decoration",
    }

# --- Strumenti e armature di bronzo e acciaio ---
COLORI_MANICO = {"W": (150, 104, 60), "w": (110, 74, 40)}
STRUMENTI = {
    "sword": [
        "................",
        ".............oo.",
        "............o+o.",
        "...........o+#o.",
        "..........o+#o..",
        ".........o+#o...",
        "........o+#o....",
        ".......o+#o.....",
        "..oo..o+#o......",
        "..o#oo+#o.......",
        "...o#-#o........",
        "....o-o.........",
        "...oWo#o........",
        "..oWo.oo........",
        ".oWo............",
        "..o.............",
    ],
    "pickaxe": [
        "................",
        "....ooooooo.....",
        "...o+++++++o....",
        "..o+###-###+o...",
        "..o##ooWoo##o...",
        "..o-o.oWo.o-o...",
        "...o..oWo..o....",
        "......oWo.......",
        "......oWo.......",
        "......owo.......",
        "......oWo.......",
        "......owo.......",
        "......oWo.......",
        "......owo.......",
        ".......o........",
        "................",
    ],
    "axe": [
        "................",
        "....oooo........",
        "...o+++#oo......",
        "..o+####Wo......",
        "..o+###-Wo......",
        "..o###--Wo......",
        "...o---oWo......",
        "....ooooWo......",
        ".......owo......",
        ".......oWo......",
        ".......owo......",
        ".......oWo......",
        ".......owo......",
        ".......oWo......",
        "........o.......",
        "................",
    ],
    "shovel": [
        "................",
        "......oooo......",
        ".....o+++#o.....",
        ".....o+###o.....",
        ".....o###-o.....",
        ".....o##--o.....",
        "......o--o......",
        "......oWWo......",
        ".......oWo......",
        ".......owo......",
        ".......oWo......",
        ".......owo......",
        ".......oWo......",
        ".......owo......",
        "........o.......",
        "................",
    ],
    "hoe": [
        "................",
        "...ooooooo......",
        "..o+++++#Wo.....",
        "..o##---oWo.....",
        "...ooooooWo.....",
        "........oWo.....",
        "........owo.....",
        "........oWo.....",
        "........owo.....",
        "........oWo.....",
        "........owo.....",
        "........oWo.....",
        "........owo.....",
        "........oWo.....",
        ".........o......",
        "................",
    ],
}
ARMATURE = {
    "helmet": [
        "................",
        "................",
        "................",
        "................",
        "....oooooooo....",
        "...o++++++++o...",
        "..o+########-o..",
        "..o+########-o..",
        "..o##oooooo#-o..",
        "..o#o......o-o..",
        "..ooo......ooo..",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    "chestplate": [
        "................",
        "..ooo......ooo..",
        ".o+#oo....oo#-o.",
        ".o+##oooooo##-o.",
        ".o+##########-o.",
        ".oo+########-oo.",
        "..oo+######-oo..",
        "...o+######-o...",
        "...o+######-o...",
        "...o+######-o...",
        "...o+######-o...",
        "...o+######-o...",
        "...o--------o...",
        "...oooooooooo...",
        "................",
        "................",
    ],
    "leggings": [
        "................",
        "...oooooooooo...",
        "...o++++++++o...",
        "...o+######-o...",
        "...o+##oo##-o...",
        "...o+#o..o#-o...",
        "...o+#o..o#-o...",
        "...o+#o..o#-o...",
        "...o+#o..o#-o...",
        "...o+#o..o#-o...",
        "...o+#o..o#-o...",
        "...o+#o..o#-o...",
        "...o+#o..o#-o...",
        "...o--o..o--o...",
        "...oooo..oooo...",
        "................",
    ],
    "boots": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "...oooo..oooo...",
        "...o+#o..o+#o...",
        "...o+#o..o+#o...",
        "...o+#o..o+#o...",
        "..oo+#o..o+#oo..",
        ".o++##o..o+##-o.",
        ".o----o..o----o.",
        ".oooooo..oooooo.",
        "................",
    ],
}
NOMI_STRUMENTI = {"sword": ("Sword", "Spada"), "pickaxe": ("Pickaxe", "Piccone"), "axe": ("Axe", "Ascia"),
                  "shovel": ("Shovel", "Pala"), "hoe": ("Hoe", "Zappa")}
NOMI_ARMATURE = {"helmet": ("Helmet", "Elmo"), "chestplate": ("Chestplate", "Corazza"),
                 "leggings": ("Leggings", "Gambali"), "boots": ("Boots", "Stivali")}
SCHEMI_STRUMENTI = {
    "sword": ["M", "M", "S"], "pickaxe": ["MMM", " S ", " S "], "axe": ["MM", "MS", " S"],
    "shovel": ["M", "S", "S"], "hoe": ["MM", " S", " S"],
}
SCHEMI_ARMATURE = {"helmet": ["MMM", "M M"], "chestplate": ["M M", "MMM", "MMM"],
                   "leggings": ["MMM", "M M", "M M"], "boots": ["M M", "M M"]}
TAG_ARMATURE = {"helmet": "head_armor", "chestplate": "chest_armor", "leggings": "leg_armor", "boots": "foot_armor"}


def texture_armatura_indossata(colore, strato, seme):
    """Texture 64x32 dell'armatura indossata (segnaposto: tinta unita con rilievi)."""
    rng = random.Random(seme)
    p = palette(colore)
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    for y in range(32):
        for x in range(64):
            if strato == 1 and x >= 32 and y < 16:
                continue  # zona del "cappello" esterno: lasciata vuota
            c = p["+"] if y % 8 == 0 else p["-"] if y % 8 == 7 else rumore(colore, rng, 0.05)
            img.putpixel((x, y), c + (255,))
    return img


for m in ["bronze", "steel"]:
    en, it, colore = MATERIALI[m]
    colori = {**palette(colore), **COLORI_MANICO}
    for tipo, disegno in STRUMENTI.items():
        id_ = f"{m}_{tipo}"
        n_en, n_it = NOMI_STRUMENTI[tipo]
        oggetto(id_, f"{en} {n_en}", f"{n_it} di {it}", "handheld")
        if salva_texture(disegna(disegno, colori, id_), TEX / "item" / f"{id_}.png"):
            texture_create += 1
        tag(tag_oggetti, f"minecraft:{tipo}s" if tipo != "pickaxe" else "minecraft:pickaxes", ft(id_))
        r_con_forma(id_, SCHEMI_STRUMENTI[tipo], {"M": f"#c:ingots/{m}", "S": "#c:rods/wooden"}, ft(id_))
    for tipo, disegno in ARMATURE.items():
        id_ = f"{m}_{tipo}"
        n_en, n_it = NOMI_ARMATURE[tipo]
        oggetto(id_, f"{en} {n_en}", f"{n_it} di {it}")
        if salva_texture(disegna(disegno, palette(colore), id_), TEX / "item" / f"{id_}.png"):
            texture_create += 1
        tag(tag_oggetti, f"minecraft:{TAG_ARMATURE[tipo]}", ft(id_))
        r_con_forma(id_, SCHEMI_ARMATURE[tipo], {"M": f"#c:ingots/{m}"}, ft(id_))
    for strato in (1, 2):
        if salva_texture(texture_armatura_indossata(colore, strato, f"{m}_layer_{strato}"),
                         TEX / "models" / "armor" / f"{m}_layer_{strato}.png"):
            texture_create += 1

# ===========================================================================
# FASE 2 - ENERGIA
# ===========================================================================
ACCIAIO = MATERIALI["steel"][2]


def texture_carcassa(seme, colore=ACCIAIO):
    """Lato di una macchina: lamiera con bordo e 4 rivetti."""
    return texture_blocco(colore, seme)


def texture_griglia(seme, colore=ACCIAIO):
    """Parte superiore: griglia di aerazione."""
    img = texture_carcassa(seme, colore)
    scuro = mix(colore, (0, 0, 0), 0.55)
    for y in range(4, 12, 2):
        for x in range(4, 12):
            img.putpixel((x, y), scuro + (255,))
    return img


def finestra(img, x0, y0, x1, y1, colori, seme):
    """Disegna una finestrella con bordo scuro e contenuto colorato (lista di colori a caso)."""
    rng = random.Random(seme)
    bordo = (34, 34, 38)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if x in (x0, x1) or y in (y0, y1):
                c = bordo
            else:
                c = rng.choice(colori)
            img.putpixel((x, y), c + (255,))
    return img


def fronte_generatore(seme, acceso):
    img = texture_carcassa(seme)
    if acceso:
        colori = [(255, 160, 30), (255, 210, 70), (230, 90, 20), (255, 240, 140)]
    else:
        colori = [(40, 36, 34), (52, 46, 42), (30, 28, 26)]
    finestra(img, 3, 7, 12, 12, colori, seme)
    # griglia della fornace
    for x in range(4, 12, 2):
        img.putpixel((x, 8), (70, 70, 74, 255))
    # pannellino in alto
    finestra(img, 5, 3, 10, 4, [(60, 200, 90)] if acceso else [(30, 60, 36)], seme + "led")
    return img


def fronte_geotermico(seme, acceso):
    img = texture_carcassa(seme)
    colori = [(240, 110, 20), (255, 160, 40), (200, 60, 10)] if acceso else [(120, 40, 14), (90, 30, 10)]
    finestra(img, 3, 3, 12, 12, colori, seme)
    return img


def cima_solare(seme):
    img = texture_carcassa(seme, (90, 96, 108))
    rng = random.Random(seme)
    for y in range(2, 14):
        for x in range(2, 14):
            linea = (x - 2) % 4 == 3 or (y - 2) % 4 == 3
            c = (150, 170, 200) if linea else rumore((30, 52, 110), rng, 0.08)
            img.putpixel((x, y), c + (255,))
    return img


COLORI_BANCO = {"battery_box": (150, 104, 60), "battery_box_2": (70, 90, 150), "battery_box_3": (120, 70, 150)}


def lato_banco(seme, colore):
    img = texture_carcassa(seme, colore)
    # simbolo "+" chiaro
    chiaro = mix(colore, (255, 255, 255), 0.55)
    for i in range(5, 11):
        img.putpixel((i, 7), chiaro + (255,))
        img.putpixel((7, i), chiaro + (255,))
    return img


def fronte_banco(seme, colore):
    img = texture_carcassa(seme, colore)
    # presa di uscita: quadrato scuro con punto rosso al centro
    finestra(img, 4, 4, 11, 11, [(40, 40, 44)], seme)
    for x in range(7, 9):
        for y in range(7, 9):
            img.putpixel((x, y), (230, 50, 40, 255))
    return img


COLORI_CAVI = {
    "tin_cable": ((176, 184, 192), None),
    "insulated_copper_cable": ((44, 44, 48), (226, 123, 82)),
    "insulated_gold_cable": ((44, 44, 48), (245, 212, 66)),
    "voltite_fiber": ((190, 90, 255), (240, 210, 255)),
}


def texture_cavo(seme, base, striscia):
    rng = random.Random(seme)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = rumore(base, rng, 0.06)
            if striscia and x in (7, 8):
                c = striscia
            img.putpixel((x, y), c + (255,))
    return img


# --- blocchi della Fase 2 ---
blocco("machine_frame", "Machine Frame", "Telaio macchina")
blocco("combustion_generator", "Combustion Generator", "Generatore a combustione")
blocco("solar_panel", "Solar Panel", "Pannello solare")
blocco("geothermal_generator", "Geothermal Generator", "Generatore geotermico")
blocco("battery_box", "Battery Box", "Banco accumulatore")
blocco("battery_box_2", "Battery Box II", "Banco accumulatore II")
blocco("battery_box_3", "Battery Box III", "Banco accumulatore III")
NOMI_CAVI = {
    "tin_cable": ("Tin Cable", "Cavo di stagno"),
    "insulated_copper_cable": ("Insulated Copper Cable", "Cavo di rame isolato"),
    "insulated_gold_cable": ("Insulated Gold Cable", "Cavo d'oro isolato"),
    "voltite_fiber": ("Voltite Fiber", "Fibra di voltite"),
}
SPESSORE_CAVI = {"tin_cable": 4, "insulated_copper_cable": 6, "insulated_gold_cable": 6, "voltite_fiber": 4}
for id_, (n_en, n_it) in NOMI_CAVI.items():
    blocco(id_, n_en, n_it)

for img, nome in [
    (texture_carcassa("machine_side"), "block/machine_side"),
    (texture_griglia("machine_top"), "block/machine_top"),
    (texture_carcassa("machine_bottom", mix(ACCIAIO, (0, 0, 0), 0.2)), "block/machine_bottom"),
    (texture_blocco(mix(ACCIAIO, (255, 255, 255), 0.1), "machine_frame"), "block/machine_frame"),
    (fronte_generatore("combustion_generator_front", False), "block/combustion_generator_front"),
    (fronte_generatore("combustion_generator_front", True), "block/combustion_generator_front_on"),
    (fronte_geotermico("geothermal_generator_front", False), "block/geothermal_generator_front"),
    (fronte_geotermico("geothermal_generator_front", True), "block/geothermal_generator_front_on"),
    (cima_solare("solar_panel_top"), "block/solar_panel_top"),
]:
    if salva_texture(img, TEX / f"{nome}.png"):
        texture_create += 1
for id_, colore in COLORI_BANCO.items():
    for img, nome in [(lato_banco(id_ + "_side", colore), f"block/{id_}_side"),
                      (fronte_banco(id_ + "_front", colore), f"block/{id_}_front"),
                      (texture_griglia(id_ + "_top", colore), f"block/{id_}_top")]:
        if salva_texture(img, TEX / f"{nome}.png"):
            texture_create += 1
for id_, (base, striscia) in COLORI_CAVI.items():
    if salva_texture(texture_cavo(id_, base, striscia), TEX / "block" / f"{id_}.png"):
        texture_create += 1


def modelli_macchina(id_, front, front_on, top="machine_top", side="machine_side", bottom="machine_bottom"):
    """Macchina orientabile (4 direzioni) con versione accesa/spenta.
    Se esiste la texture block/<id>_side.png (fascia colorata + simbolo, grafica v2) la usa per i lati."""
    if side == "machine_side" and (TEX / "block" / f"{id_}_side.png").exists():
        side = f"{id_}_side"

    def scrivi():
        A_ = f"assets/{MODID}"
        for suffisso, fronte in [("", front), ("_on", front_on)]:
            scrivi_json(f"{A_}/models/block/{id_}{suffisso}.json", {
                "parent": "minecraft:block/orientable_with_bottom",
                "textures": {"front": f"{MODID}:block/{fronte}", "side": f"{MODID}:block/{side}",
                             "top": f"{MODID}:block/{top}", "bottom": f"{MODID}:block/{bottom}"}})
        rot = {"north": 0, "east": 90, "south": 180, "west": 270}
        varianti = {}
        for direzione, gradi in rot.items():
            for acceso in ["false", "true"]:
                v = {"model": f"{MODID}:block/{id_}" + ("_on" if acceso == "true" else "")}
                if gradi:
                    v["y"] = gradi
                varianti[f"facing={direzione},lit={acceso}"] = v
        scrivi_json(f"{A_}/blockstates/{id_}.json", {"variants": varianti})
        scrivi_json(f"{A_}/models/item/{id_}.json", {"parent": f"{MODID}:block/{id_}"})
    return scrivi


def modelli_pannello_solare():
    A_ = f"assets/{MODID}"
    scrivi_json(f"{A_}/models/block/solar_panel.json", {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {"top": f"{MODID}:block/solar_panel_top", "side": f"{MODID}:block/machine_side",
                     "bottom": f"{MODID}:block/machine_bottom"}})
    varianti = {f"facing={d},lit={l}": {"model": f"{MODID}:block/solar_panel"}
                for d in ["north", "east", "south", "west"] for l in ["false", "true"]}
    scrivi_json(f"{A_}/blockstates/solar_panel.json", {"variants": varianti})
    scrivi_json(f"{A_}/models/item/solar_panel.json", {"parent": f"{MODID}:block/solar_panel"})


def modelli_cavo(id_, spessore):
    def scrivi():
        A_ = f"assets/{MODID}"
        a = (16 - spessore) / 2
        b = 16 - a
        tex = f"{MODID}:block/{id_}"

        def facce(uv):
            return {f: {"texture": "#cable", "uv": uv} for f in ["north", "south", "east", "west", "up", "down"]}

        scrivi_json(f"{A_}/models/block/{id_}_core.json", {
            "textures": {"cable": tex, "particle": tex},
            "elements": [{"from": [a, a, a], "to": [b, b, b], "faces": facce([a, a, b, b])}]})
        scrivi_json(f"{A_}/models/block/{id_}_arm.json", {
            "textures": {"cable": tex, "particle": tex},
            "elements": [{"from": [a, a, 0], "to": [b, b, a], "faces": facce([a, 0, b, a])}]})
        scrivi_json(f"{A_}/models/item/{id_}.json", {
            "parent": "minecraft:block/block",
            "textures": {"cable": tex, "particle": tex},
            "elements": [{"from": [a, a, 0], "to": [b, b, 16], "faces": facce([a, 0, b, 16])}]})
        rot = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270},
               "up": {"x": 270}, "down": {"x": 90}}
        parti = [{"apply": {"model": f"{MODID}:block/{id_}_core"}}]
        for direzione, r in rot.items():
            parti.append({"when": {direzione: "true"}, "apply": {"model": f"{MODID}:block/{id_}_arm", **r}})
        scrivi_json(f"{A_}/blockstates/{id_}.json", {"multipart": parti})
    return scrivi


modelli_speciali.update({
    "combustion_generator": modelli_macchina("combustion_generator", "combustion_generator_front",
                                             "combustion_generator_front_on"),
    "geothermal_generator": modelli_macchina("geothermal_generator", "geothermal_generator_front",
                                             "geothermal_generator_front_on"),
    "solar_panel": modelli_pannello_solare,
})
for id_ in COLORI_BANCO:
    modelli_speciali[id_] = modelli_macchina(id_, f"{id_}_front", f"{id_}_front", top=f"{id_}_top",
                                             side=f"{id_}_side", bottom="machine_bottom")
for id_, spessore in SPESSORE_CAVI.items():
    modelli_speciali[id_] = modelli_cavo(id_, spessore)

for id_ in ["machine_frame", "combustion_generator", "solar_panel", "geothermal_generator", *COLORI_BANCO]:
    tag(tag_blocchi, "minecraft:mineable/pickaxe", ft(id_))

# --- oggetti della Fase 2 ---
FILO = [
    "................",
    "................",
    "................",
    "....oooooooo....",
    "...o+#+#+#+#o...",
    "..o#oooooooo-o..",
    "..o+o......o#o..",
    "..o#o......o-o..",
    "..o+o......o#o..",
    "..o#oooooooo-o..",
    "...o-#-#-#-#o...",
    "....oooooooo....",
    "................",
    "................",
    "................",
    "................",
]
CELLA = [
    "................",
    "......oooo......",
    ".....o++++o.....",
    "....oooooooo....",
    "....o+####-o....",
    "....o+####-o....",
    "....o+#++#-o....",
    "....o+#++#-o....",
    "....o+####-o....",
    "....o+####-o....",
    "....o+####-o....",
    "....o+####-o....",
    "....o------o....",
    "....oooooooo....",
    "................",
    "................",
]
CIRCUITO = [
    "................",
    "................",
    "..oooooooooooo..",
    "..oggggggggggo..",
    "..ogkkkggykkgo..",
    "..ogkkkyyykkgo..",
    "..ogkkkggyggyo..",
    "..oggyggggggyo..",
    "..ogyyyyyyyyyo..",
    "..ogggggyggggo..",
    "..ogkkggyggkko..",
    "..ogkkggyggkko..",
    "..oggggggggggo..",
    "..oooooooooooo..",
    "................",
    "................",
]
TAGLIACAVI = [
    "................",
    "..oo......oo....",
    ".o+#o....o+#o...",
    ".o#-o....o#-o...",
    "..o#-o..o#-o....",
    "...o#-oo#-o.....",
    "....o#--#o......",
    ".....o##o.......",
    "....oRooRo......",
    "...oRo..oRo.....",
    "..oRo....oRo....",
    ".oRRo....oRRo...",
    ".oRo......oRo...",
    "..o........o....",
    "................",
    "................",
]
CHIAVE = [
    "................",
    "..........oo.oo.",
    ".........o+#o#o.",
    ".........o+##-o.",
    "........o+##-o..",
    ".......o+##-o...",
    "......o+#-o.....",
    ".....o+#-o......",
    "....o+#-o.......",
    "...o+#-o........",
    "..o+#-o.........",
    ".o+#-o..........",
    ".o#-o...........",
    "..oo............",
    "................",
    "................",
]
MULTIMETRO = [
    "................",
    "...oooooooooo...",
    "...oYYYYYYYYo...",
    "...oYssssssYo...",
    "...oYsSSsssYo...",
    "...oYssssssYo...",
    "...oYYYYYYYYo...",
    "...oYYkYYkYYo...",
    "...oYkkkkkkYo...",
    "...oYYkYYkYYo...",
    "...oYYYYYYYYo...",
    "...oooooooooo...",
    "....R.....K.....",
    "....R.....K.....",
    "....R.....K.....",
    "................",
]
COLORI_CIRCUITO = {"o": (20, 40, 20), "g": (40, 120, 50), "k": (30, 30, 34), "y": (220, 180, 60)}
COLORI_MULTIMETRO = {"o": (30, 30, 30), "Y": (230, 190, 40), "s": (150, 200, 150), "S": (40, 70, 40),
                     "k": (40, 40, 40), "R": (200, 40, 40), "K": (20, 20, 20)}

for m in ["tin", "copper", "gold"]:
    en, it, colore = MATERIALI[m]
    id_ = f"{m}_wire"
    oggetto(id_, f"{en} Wire", f"Filo di {it}")
    if salva_texture(disegna(FILO, palette(colore), id_), TEX / "item" / f"{id_}.png"):
        texture_create += 1
    tag(tag_oggetti, f"c:wires/{m}", ft(id_))
    tag(tag_oggetti, "c:wires", f"#c:wires/{m}")
    r_senza_forma(id_, [ft("cutters"), f"#c:plates/{m}"], ft(id_), 2)

oggetto("cutters", "Cutters", "Tagliacavi", "handheld")
oggetto("basic_circuit", "Basic Circuit", "Circuito di base")
oggetto("battery_cell", "Battery Cell", "Cella portatile")
oggetto("advanced_battery_cell", "Advanced Battery Cell", "Cella avanzata")
oggetto("wrench", "Wrench", "Chiave inglese", "handheld")
oggetto("multimeter", "Multimeter", "Multimetro")
for img, nome in [
    (disegna(TAGLIACAVI, {**palette(ACCIAIO), "R": (190, 40, 40)}, "cutters"), "item/cutters"),
    (disegna(CIRCUITO, COLORI_CIRCUITO, "basic_circuit"), "item/basic_circuit"),
    (disegna(CELLA, palette(MATERIALI["tin"][2]), "battery_cell"), "item/battery_cell"),
    (disegna(CELLA, palette(MATERIALI["lead"][2]), "advanced_battery_cell"), "item/advanced_battery_cell"),
    (disegna(CHIAVE, palette(MATERIALI["bronze"][2]), "wrench"), "item/wrench"),
    (disegna(MULTIMETRO, COLORI_MULTIMETRO, "multimeter"), "item/multimeter"),
]:
    if salva_texture(img, TEX / f"{nome}.png"):
        texture_create += 1
tag(tag_oggetti, "c:tools/wrench", ft("wrench"))

# --- ricette della Fase 2 ---
PIASTRA_FERRO = "#c:plates/iron"
r_con_forma("cutters", ["P P", " P ", "S S"], {"P": PIASTRA_FERRO, "S": "#c:rods/wooden"}, ft("cutters"))
r_con_forma("tin_cable", ["WWW"], {"W": "#c:wires/tin"}, ft("tin_cable"), 6)
r_con_forma("insulated_copper_cable", ["RRR", "WWW", "RRR"], {"R": ft("rubber"), "W": "#c:wires/copper"},
            ft("insulated_copper_cable"), 6)
r_con_forma("insulated_gold_cable", ["RRR", "WWW", "RRR"], {"R": ft("rubber"), "W": "#c:wires/gold"},
            ft("insulated_gold_cable"), 6)
r_con_forma("voltite_fiber", ["GGG", "VVV", "GGG"], {"G": "#c:glass_blocks", "V": "#c:gems/voltite"},
            ft("voltite_fiber"), 4)
r_con_forma("basic_circuit", ["CCC", "RPR", "CCC"],
            {"C": ft("insulated_copper_cable"), "R": "#c:dusts/redstone", "P": PIASTRA_FERRO}, ft("basic_circuit"))
r_con_forma("machine_frame", ["PPP", "P P", "PPP"], {"P": PIASTRA_FERRO}, ft("machine_frame"))
r_con_forma("battery_cell", [" C ", "TRT", "TRT"],
            {"C": ft("tin_cable"), "T": "#c:plates/tin", "R": "#c:dusts/redstone"}, ft("battery_cell"))
r_con_forma("advanced_battery_cell", [" C ", "LVL", "LVL"],
            {"C": ft("insulated_copper_cable"), "L": "#c:plates/lead", "V": "#c:gems/voltite"},
            ft("advanced_battery_cell"))
r_con_forma("combustion_generator", [" B ", "PFP", " U "],
            {"B": ft("battery_cell"), "P": PIASTRA_FERRO, "F": ft("machine_frame"), "U": "minecraft:furnace"},
            ft("combustion_generator"))
r_con_forma("solar_panel", ["GGG", "DVD", "PCP"],
            {"G": "#c:glass_blocks", "D": "#c:dusts/coal", "V": "#c:gems/voltite", "P": PIASTRA_FERRO,
             "C": ft("basic_circuit")}, ft("solar_panel"))
r_con_forma("geothermal_generator", ["GBG", "GFG", "PCP"],
            {"G": "#c:glass_blocks", "B": "minecraft:bucket", "F": ft("combustion_generator"), "P": PIASTRA_FERRO,
             "C": ft("basic_circuit")}, ft("geothermal_generator"))
r_con_forma("battery_box", ["PCP", "BBB", "PPP"],
            {"P": "#minecraft:planks", "C": ft("tin_cable"), "B": ft("battery_cell")}, ft("battery_box"))
r_con_forma("battery_box_2", ["CAC", "ABA", "CAC"],
            {"C": ft("insulated_copper_cable"), "A": ft("advanced_battery_cell"), "B": ft("battery_box")},
            ft("battery_box_2"))
r_con_forma("battery_box_3", ["SAS", "ABA", "SAS"],
            {"S": "#c:plates/silver", "A": ft("advanced_battery_cell"), "B": ft("battery_box_2")},
            ft("battery_box_3"))
r_con_forma("wrench", ["B B", " B ", " B "], {"B": "#c:ingots/bronze"}, ft("wrench"))
r_con_forma("multimeter", ["TGT", "TCT", "W W"],
            {"T": "#c:plates/tin", "G": "#c:glass_panes", "C": ft("basic_circuit"), "W": ft("tin_cable")},
            ft("multimeter"))

# --- traduzioni della Fase 2 ---
lingua_en.update({
    "gui.futuretek.energy": "%s / %s TU",
    "gui.futuretek.lava": "Lava: %s / %s mB",
    "gui.futuretek.charge": "Charge",
    "gui.futuretek.discharge": "Discharge",
    "message.futuretek.energy": "Energy: %s / %s TU",
    "message.futuretek.multimeter.no_energy": "No energy here",
    "message.futuretek.multimeter.network": "Network: %s cables, %s connections, max %s TU/t, last tick %s TU",
    "futuretek.configuration.generators": "Generators",
    "futuretek.configuration.combustionRate": "Combustion generator (TU/t)",
    "futuretek.configuration.solarRate": "Solar panel (TU/t)",
    "futuretek.configuration.geothermalRate": "Geothermal generator (TU/t)",
    "futuretek.configuration.lavaEnergyPerMb": "TU per mB of lava",
})
lingua_it.update({
    "gui.futuretek.energy": "%s / %s TU",
    "gui.futuretek.lava": "Lava: %s / %s mB",
    "gui.futuretek.charge": "Carica",
    "gui.futuretek.discharge": "Scarica",
    "message.futuretek.energy": "Energia: %s / %s TU",
    "message.futuretek.multimeter.no_energy": "Nessuna energia qui",
    "message.futuretek.multimeter.network": "Rete: %s cavi, %s collegamenti, max %s TU/t, ultimo tick %s TU",
    "futuretek.configuration.generators": "Generatori",
    "futuretek.configuration.combustionRate": "Generatore a combustione (TU/t)",
    "futuretek.configuration.solarRate": "Pannello solare (TU/t)",
    "futuretek.configuration.geothermalRate": "Generatore geotermico (TU/t)",
    "futuretek.configuration.lavaEnergyPerMb": "TU per mB di lava",
})


# --- struttura vuota per i test automatici (formato NBT di Minecraft) ---
def nbt_struttura_vuota(dimensione=(8, 4, 8), versione_dati=3955):
    import gzip
    import struct

    def nome(n):
        b = n.encode()
        return struct.pack(">H", len(b)) + b

    def tag_int(n, v):
        return b"\x03" + nome(n) + struct.pack(">i", v)

    def lista_int(n, valori):
        return b"\x09" + nome(n) + b"\x03" + struct.pack(">i", len(valori)) + b"".join(
            struct.pack(">i", v) for v in valori)

    def lista_vuota(n):
        return b"\x09" + nome(n) + b"\x00" + struct.pack(">i", 0)

    palette_aria = (b"\x09" + nome("palette") + b"\x0a" + struct.pack(">i", 1)
                    + b"\x08" + nome("Name") + nome("minecraft:air") + b"\x00")
    radice = (b"\x0a" + nome("") + tag_int("DataVersion", versione_dati) + lista_int("size", list(dimensione))
              + lista_vuota("blocks") + lista_vuota("entities") + palette_aria + b"\x00")
    return gzip.compress(radice)


strutture_binarie = {f"data/{MODID}/structure/empty.nbt": nbt_struttura_vuota()}


# ===========================================================================
# FASE 3 - MACCHINE DI LAVORAZIONE
# ===========================================================================
def fronte_frantumatore(seme, acceso):
    img = texture_carcassa(seme)
    finestra(img, 3, 4, 12, 12, [(40, 40, 44)], seme)
    denti = (210, 210, 220) if acceso else (150, 150, 158)
    for x in range(4, 12):
        img.putpixel((x, 5 + (x % 2)), denti + (255,))      # mascella superiore a zig-zag
        img.putpixel((x, 11 - (x % 2)), denti + (255,))     # mascella inferiore
    if acceso:
        for (x, y) in [(6, 8), (8, 7), (9, 9)]:
            img.putpixel((x, y), (190, 170, 140, 255))      # polvere che vola
    return img


def fronte_forno_elettrico(seme, acceso):
    img = texture_carcassa(seme)
    colori = [(255, 120, 30), (255, 170, 60), (240, 90, 20)] if acceso else [(50, 30, 26), (60, 36, 30)]
    finestra(img, 3, 6, 12, 12, colori, seme)
    # resistenza elettrica a serpentina
    serp = (255, 230, 120) if acceso else (110, 70, 60)
    for x in range(4, 12):
        img.putpixel((x, 9 if (x // 2) % 2 else 10), serp + (255,))
    finestra(img, 5, 3, 10, 4, [(60, 200, 90)] if acceso else [(30, 60, 36)], seme + "led")
    return img


def fronte_compressore(seme, acceso):
    img = texture_carcassa(seme)
    piastra = (190, 190, 200)
    finestra(img, 3, 3, 12, 12, [(40, 40, 44)], seme)
    alto = 5 if acceso else 4
    for x in range(4, 12):
        for y in range(4, alto + 1):
            img.putpixel((x, y), piastra + (255,))
        for y in range(11 - (alto - 4), 12):
            img.putpixel((x, y), piastra + (255,))
    img.putpixel((7, 2), (90, 90, 96, 255))
    img.putpixel((8, 2), (90, 90, 96, 255))
    return img


def fronte_estrattore(seme, acceso):
    img = texture_carcassa(seme)
    finestra(img, 4, 3, 11, 7, [(70, 70, 78)], seme)
    # imbuto e goccia
    for i, y in enumerate(range(8, 11)):
        for x in range(5 + i, 11 - i):
            img.putpixel((x, y), (110, 110, 118, 255))
    goccia = (232, 156, 44) if acceso else (120, 90, 50)
    img.putpixel((7, 12), goccia + (255,))
    img.putpixel((8, 12), goccia + (255,))
    img.putpixel((7, 13), goccia + (255,))
    return img


def fronte_pressa_fili(seme, acceso):
    img = texture_carcassa(seme)
    finestra(img, 3, 3, 12, 12, [(40, 40, 44)], seme)
    rullo = (200, 200, 210) if acceso else (150, 150, 158)
    for (cx, cy) in [(7, 6), (7, 10)]:
        for dx in range(-2, 4):
            for dy in range(-1, 2):
                img.putpixel((cx + dx, cy + dy), rullo + (255,))
    filo = (226, 123, 82)
    for x in range(1, 15):
        img.putpixel((x, 8), filo + (255,))
    return img


MACCHINE = {
    "crusher": ("Crusher", "Frantumatore", fronte_frantumatore),
    "electric_furnace": ("Electric Furnace", "Forno elettrico", fronte_forno_elettrico),
    "compressor": ("Compressor", "Compressore", fronte_compressore),
    "extractor": ("Extractor", "Estrattore", fronte_estrattore),
    "wire_press": ("Wire Press", "Pressa per fili", fronte_pressa_fili),
}
for id_, (n_en, n_it, funzione) in MACCHINE.items():
    blocco(id_, n_en, n_it)
    for acceso, suffisso in [(False, ""), (True, "_on")]:
        if salva_texture(funzione(f"{id_}_front", acceso), TEX / "block" / f"{id_}_front{suffisso}.png"):
            texture_create += 1
    modelli_speciali[id_] = modelli_macchina(id_, f"{id_}_front", f"{id_}_front_on")
    tag(tag_blocchi, "minecraft:mineable/pickaxe", ft(id_))

# --- potenziamenti ---
POTENZIAMENTO = [
    "................",
    "................",
    "..oooooooooooo..",
    "..o++++++++++o..",
    "..o+gggggggg-o..",
    "..o+gSSSSSSg-o..",
    "..o+gS....Sg-o..",
    "..o+gS....Sg-o..",
    "..o+gS....Sg-o..",
    "..o+gS....Sg-o..",
    "..o+gSSSSSSg-o..",
    "..o+gggggggg-o..",
    "..o----------o..",
    "..oooooooooooo..",
    "................",
    "................",
]
# simbolo al centro (4x4) disegnato sopra lo sfondo
SIMBOLI = {
    "overclock": ["..X.", ".XX.", "XXXX", ".XX."],          # fulmine stilizzato
    "efficiency": [".XX.", "XXXX", "XXXX", ".XX."],          # goccia/cerchio
    "energy_storage": ["XXXX", "X..X", "X..X", "XXXX"],      # batteria
    "ejector": ["..X.", "XXXX", "XXXX", "..X."],             # freccia
}
COLORI_POTENZIAMENTI = {"overclock": (230, 70, 50), "efficiency": (60, 190, 90),
                        "energy_storage": (230, 190, 40), "ejector": (70, 140, 230)}
NOMI_POTENZIAMENTI = {
    "overclock": ("Overclocker Upgrade", "Potenziamento overclock",
                  "+50% speed, +60% energy per tick (max 4)", "+50% velocità, +60% energia al tick (max 4)"),
    "efficiency": ("Efficiency Upgrade", "Potenziamento efficienza",
                   "-20% energy per tick (max 4)", "-20% energia al tick (max 4)"),
    "energy_storage": ("Energy Storage Upgrade", "Potenziamento accumulo",
                       "+10,000 TU capacity (max 4)", "+10.000 TU di capacità (max 4)"),
    "ejector": ("Ejector Upgrade", "Potenziamento espulsione",
                "Pushes products into adjacent inventories", "Spinge i prodotti nei contenitori vicini"),
}
for tipo, (n_en, n_it, d_en, d_it) in NOMI_POTENZIAMENTI.items():
    id_ = f"{tipo}_upgrade"
    oggetto(id_, n_en, n_it)
    lingua_en[f"tooltip.futuretek.upgrade.{tipo}"] = d_en
    lingua_it[f"tooltip.futuretek.upgrade.{tipo}"] = d_it
    colori = {**palette(ACCIAIO), "g": (40, 120, 50), "S": (30, 80, 36)}
    img = disegna(POTENZIAMENTO, colori, id_)
    for dy, riga in enumerate(SIMBOLI[tipo]):
        for dx, ch in enumerate(riga):
            if ch == "X":
                img.putpixel((6 + dx, 6 + dy), COLORI_POTENZIAMENTI[tipo] + (255,))
    if salva_texture(img, TEX / "item" / f"{id_}.png"):
        texture_create += 1

# --- ricette per costruire le macchine e i potenziamenti ---
C = ft("basic_circuit")
M = ft("machine_frame")
r_con_forma("crusher", ["FFF", "CMC", "PPP"], {"F": "minecraft:flint", "C": C, "M": M, "P": PIASTRA_FERRO},
            ft("crusher"))
r_con_forma("electric_furnace", [" C ", "RFR", " M "], {"C": C, "R": "#c:dusts/redstone", "F": "minecraft:furnace",
                                                         "M": M}, ft("electric_furnace"))
r_con_forma("compressor", ["S S", "SMS", "SCS"], {"S": "#c:stones", "M": M, "C": C}, ft("compressor"))
r_con_forma("extractor", ["TMT", "TCT"], {"T": ft("tree_tap"), "M": M, "C": C}, ft("extractor"))
r_con_forma("wire_press", ["IXI", "CMC"], {"I": "#c:ingots/iron", "X": ft("cutters"), "C": C, "M": M},
            ft("wire_press"))
r_con_forma("overclock_upgrade", ["WWW", "PCP"], {"W": ft("insulated_copper_cable"), "P": "#c:plates/tin", "C": C},
            ft("overclock_upgrade"))
r_con_forma("efficiency_upgrade", ["VVV", "PCP"], {"V": "#c:gems/voltite", "P": "#c:plates/tin", "C": C},
            ft("efficiency_upgrade"))
r_con_forma("energy_storage_upgrade", ["PPP", "WBW", "PCP"],
            {"P": "#c:plates/tin", "W": ft("tin_cable"), "B": ft("battery_cell"), "C": C}, ft("energy_storage_upgrade"))
r_con_forma("ejector_upgrade", ["PHP", " C "], {"P": "#c:plates/tin", "H": "minecraft:hopper", "C": C},
            ft("ejector_upgrade"))


# --- ricette DELLE macchine (file JSON modificabili) ---
def r_macchina(tipo, nome, ingrediente, quantita_in, risultato, quantita_out, extra=None, probabilita=0.0, tempo=0):
    dati = {"type": f"{MODID}:{tipo}", "ingredient": {**ing(ingrediente), "count": quantita_in},
            "result": {"id": risultato, "count": quantita_out}}
    if extra:
        dati["secondary"] = {"item": {"id": extra, "count": 1}, "chance": probabilita}
    if tempo:
        dati["time"] = tempo
    ricette[f"{tipo}/{nome}"] = dati


# Frantumatore: RADDOPPIO dei minerali (grezzo -> 2 polveri) + 10% di polvere in piu'
for m in ["tin", "lead", "silver", "uranium", "iron", "gold", "copper"]:
    polvere = ft(f"{m}_dust")
    r_macchina("crushing", f"raw_{m}", f"#c:raw_materials/{m}", 1, polvere, 2, polvere, 0.1)
    r_macchina("crushing", f"{m}_ore", f"#c:ores/{m}", 1, polvere, 2, polvere, 0.1)
    r_macchina("crushing", f"raw_{m}_block", f"#c:storage_blocks/raw_{m}", 1, polvere, 18)
    r_macchina("crushing", f"{m}_ingot", f"#c:ingots/{m}", 1, polvere, 1, tempo=150)
for m in ["bronze", "steel"]:
    r_macchina("crushing", f"{m}_ingot", f"#c:ingots/{m}", 1, ft(f"{m}_dust"), 1, tempo=150)
r_macchina("crushing", "voltite_ore", "#c:ores/voltite", 1, ft("voltite_shard"), 5)
r_macchina("crushing", "coal", "minecraft:coal", 1, ft("coal_dust"), 1)
r_macchina("crushing", "diamond", "#c:gems/diamond", 1, ft("diamond_dust"), 1)
r_macchina("crushing", "obsidian", "#c:obsidians", 1, ft("obsidian_dust"), 2)
r_macchina("crushing", "cobblestone", "#c:cobblestones", 1, "minecraft:gravel", 1, tempo=100)
r_macchina("crushing", "gravel", "#c:gravels", 1, "minecraft:sand", 1, "minecraft:flint", 0.1, tempo=100)
r_macchina("crushing", "stone", "minecraft:stone", 1, "minecraft:cobblestone", 1, tempo=100)
r_macchina("crushing", "glowstone", "minecraft:glowstone", 1, "minecraft:glowstone_dust", 4, tempo=100)
r_macchina("crushing", "bone", "minecraft:bone", 1, "minecraft:bone_meal", 5, tempo=100)
r_macchina("crushing", "blaze_rod", "minecraft:blaze_rod", 1, "minecraft:blaze_powder", 5, tempo=100)
r_macchina("crushing", "wool", "#minecraft:wool", 1, "minecraft:string", 4, tempo=100)
# Compressore
for m in ["tin", "lead", "silver", "uranium", "bronze", "steel", "iron", "gold", "copper"]:
    r_macchina("compressing", f"{m}_plate", f"#c:ingots/{m}", 1, ft(f"{m}_plate"), 1)
r_macchina("compressing", "sandstone", "#c:sands", 4, "minecraft:sandstone", 1, tempo=200)
r_macchina("compressing", "snow_block", "minecraft:snowball", 4, "minecraft:snow_block", 1, tempo=100)
r_macchina("compressing", "ice", "minecraft:snow_block", 1, "minecraft:ice", 1, tempo=200)
r_macchina("compressing", "packed_ice", "minecraft:ice", 4, "minecraft:packed_ice", 1, tempo=200)
r_macchina("compressing", "obsidian", ft("obsidian_dust"), 4, "minecraft:obsidian", 1)
r_macchina("compressing", "diamond", ft("diamond_dust"), 1, "minecraft:diamond", 1)
r_macchina("compressing", "voltite_shard", ft("voltite_shard"), 9, "minecraft:amethyst_shard", 1)
# Estrattore
r_macchina("extracting", "rubber_from_resin", ft("sticky_resin"), 1, ft("rubber"), 3)
r_macchina("extracting", "rubber_from_log", ft("resin_log"), 1, ft("rubber"), 1)
r_macchina("extracting", "rubber_from_sapling", ft("resin_sapling"), 1, ft("rubber"), 1)
r_macchina("extracting", "gunpowder", "minecraft:tnt", 1, "minecraft:gunpowder", 5)
r_macchina("extracting", "slime_ball", "minecraft:slime_block", 1, "minecraft:slime_ball", 9, tempo=100)
# Pressa per fili
for m in ["tin", "copper", "gold"]:
    r_macchina("wire_pressing", f"{m}_wire", f"#c:plates/{m}", 1, ft(f"{m}_wire"), 3)

lingua_en.update({"gui.futuretek.upgrades": "Upgrades"})
lingua_it.update({"gui.futuretek.upgrades": "Potenziamenti"})

# ===========================================================================
# FASE 3b - TRIPLICAZIONE (bagno chimico + centrifuga termica)
# ===========================================================================
def fronte_bagno(seme, acceso):
    img = texture_carcassa(seme)
    colori = [(60, 120, 230), (90, 150, 240), (50, 100, 210)] if acceso else [(40, 70, 130), (36, 60, 110)]
    finestra(img, 3, 5, 12, 12, colori, seme)
    if acceso:
        for (x, y) in [(5, 7), (9, 8), (7, 10)]:
            img.putpixel((x, y), (200, 230, 255, 255))   # bollicine
    return img


def fronte_centrifuga(seme, acceso):
    img = texture_carcassa(seme)
    finestra(img, 3, 3, 12, 12, [(40, 40, 44)], seme)
    pala = (230, 120, 40) if acceso else (150, 150, 158)
    for i in range(4, 12):
        img.putpixel((i, i), pala + (255,))
        img.putpixel((15 - i, i), pala + (255,))
    img.putpixel((7, 7), (230, 230, 235, 255))
    img.putpixel((8, 8), (230, 230, 235, 255))
    return img


for id_, n_en, n_it, funzione in [
    ("chemical_bath", "Chemical Bath", "Bagno chimico", fronte_bagno),
    ("thermal_centrifuge", "Thermal Centrifuge", "Centrifuga termica", fronte_centrifuga),
]:
    blocco(id_, n_en, n_it)
    for acceso, suffisso in [(False, ""), (True, "_on")]:
        if salva_texture(funzione(f"{id_}_front", acceso), TEX / "block" / f"{id_}_front{suffisso}.png"):
            texture_create += 1
    modelli_speciali[id_] = modelli_macchina(id_, f"{id_}_front", f"{id_}_front_on")
    tag(tag_blocchi, "minecraft:mineable/pickaxe", ft(id_))

# sottoprodotti: cosa si trova "in piu'" lavorando ogni metallo
SOTTOPRODOTTO = {"tin": "lead", "lead": "silver", "silver": "lead", "uranium": "lead",
                 "iron": "tin", "gold": "silver", "copper": "gold"}
for m in ["tin", "lead", "silver", "uranium", "iron", "gold", "copper"]:
    en, it, colore = MATERIALI[m]
    id_ = f"purified_crushed_{m}"
    oggetto(id_, f"Purified Crushed {en}", f"Frantumato puro di {it}")
    colori = palette(mix(colore, (255, 255, 255), 0.15))
    if salva_texture(disegna(FORME["raw"], colori, id_ + "_p"), TEX / "item" / f"{id_}.png"):
        texture_create += 1
    extra = ft(f"{SOTTOPRODOTTO[m]}_dust")
    # bagno chimico: 1 grezzo (o minerale) -> 3 frantumati puri (+ 10% polvere di un altro metallo)
    r_macchina("washing", f"raw_{m}", f"#c:raw_materials/{m}", 1, ft(id_), 3, extra, 0.1)
    r_macchina("washing", f"{m}_ore", f"#c:ores/{m}", 1, ft(id_), 3, extra, 0.1)
    # centrifuga: 1 frantumato puro -> 1 polvere (+ 5% polvere extra)
    r_macchina("centrifuging", m, ft(id_), 1, ft(f"{m}_dust"), 1, extra, 0.05)

r_con_forma("chemical_bath", ["PKP", "CMC"], {"P": "#c:plates/tin", "K": "minecraft:cauldron", "C": C, "M": M},
            ft("chemical_bath"))
r_con_forma("thermal_centrifuge", ["SBS", "CMC", "SBS"],
            {"S": "#c:plates/steel", "B": "minecraft:blast_furnace", "C": C, "M": M}, ft("thermal_centrifuge"))
lingua_en.update({"gui.futuretek.water": "Water: %s / %s mB"})
lingua_it.update({"gui.futuretek.water": "Acqua: %s / %s mB"})

# ===========================================================================
# FASE 4 - TRASMUTAZIONE (Essenza)
# ===========================================================================
VIOLA = (150, 80, 210)


def texture_tavolo_sintesi_cima(seme):
    rng = random.Random(seme)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = rumore((70, 50, 90), rng, 0.06)
            img.putpixel((x, y), c + (255,))
    # cerchio di trasmutazione: anello + stella a 4 punte
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if 5.2 < d < 6.4 or (abs(x - 7.5) < 1 and d < 5) or (abs(y - 7.5) < 1 and d < 5):
                img.putpixel((x, y), (210, 150, 255, 255))
    return img


def texture_lato_viola(seme, colore=(70, 50, 90)):
    img = texture_blocco(colore, seme)
    return img


def texture_captatore(seme, colore):
    rng = random.Random(seme)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            d = abs(x - 7.5) + abs(y - 7.5)
            c = mix(colore, (255, 255, 255), 0.5) if d < 3 else colore if d < 6 else (60, 50, 70)
            img.putpixel((x, y), rumore(c, rng, 0.05) + (255,))
    return img


def texture_replicatore(seme, lato):
    img = texture_blocco((90, 70, 110), seme)
    if lato == "top":
        finestra(img, 4, 4, 11, 11, [(190, 110, 255), (150, 80, 210)], seme)
    return img


blocco("synthesis_table", "Synthesis Table", "Tavolo di Sintesi")
blocco("star_collector", "Star Collector", "Captatore Stellare")
blocco("star_collector_2", "Star Collector II", "Captatore Stellare II")
blocco("star_collector_3", "Star Collector III", "Captatore Stellare III")
blocco("replicator", "Replicator", "Replicatore")
for img, nome in [
    (texture_tavolo_sintesi_cima("synthesis_table_top"), "block/synthesis_table_top"),
    (texture_lato_viola("synthesis_table_side"), "block/synthesis_table_side"),
    (texture_captatore("star_collector", (230, 200, 60)), "block/star_collector"),
    (texture_captatore("star_collector_2", (230, 120, 60)), "block/star_collector_2"),
    (texture_captatore("star_collector_3", (190, 90, 255)), "block/star_collector_3"),
    (texture_replicatore("replicator_top", "top"), "block/replicator_top"),
    (texture_replicatore("replicator_side", "side"), "block/replicator_side"),
]:
    if salva_texture(img, TEX / f"{nome}.png"):
        texture_create += 1


def modello_alto_lato(id_, top, side, bottom=None):
    def scrivi():
        A_ = f"assets/{MODID}"
        scrivi_json(f"{A_}/models/block/{id_}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
            "top": f"{MODID}:block/{top}", "side": f"{MODID}:block/{side}",
            "bottom": f"{MODID}:block/{bottom or side}"}})
        scrivi_json(f"{A_}/blockstates/{id_}.json", {"variants": {"": {"model": f"{MODID}:block/{id_}"}}})
        scrivi_json(f"{A_}/models/item/{id_}.json", {"parent": f"{MODID}:block/{id_}"})
    return scrivi


modelli_speciali["synthesis_table"] = modello_alto_lato("synthesis_table", "synthesis_table_top", "synthesis_table_side")
modelli_speciali["replicator"] = modello_alto_lato("replicator", "replicator_top", "replicator_side")
for id_ in ["synthesis_table", "star_collector", "star_collector_2", "star_collector_3", "replicator"]:
    tag(tag_blocchi, "minecraft:mineable/pickaxe", ft(id_))

PIETRA_MUTAMENTO = [
    "................",
    "......oooo......",
    ".....o++++o.....",
    "....o+#++#-o....",
    "...o+##++##-o...",
    "..o+###++###-o..",
    "..o+++++++++-o..",
    "..o+++++++++-o..",
    "..o-###++###-o..",
    "...o-##++##-o...",
    "....o-#++#-o....",
    ".....o----o.....",
    "......oooo......",
    "................",
    "................",
    "................",
]
CRISTALLO = [
    "................",
    ".......oo.......",
    "......o++o......",
    ".....o+#+#o.....",
    "....o+##+##o....",
    "...o+###+##-o...",
    "...o+###+#--o...",
    "...o+##+##--o...",
    "...o+##+#---o...",
    "....o#+##--o....",
    ".....o+#--o.....",
    "......o--o......",
    ".......oo.......",
    "................",
    "................",
    "................",
]
oggetto("shifting_stone", "Shifting Stone", "Pietra del Mutamento")
for id_, n_en, n_it, colore in [
    ("essence_crystal", "Essence Crystal", "Cristallo d'Essenza", (180, 120, 230)),
    ("essence_crystal_2", "Essence Crystal II", "Cristallo d'Essenza II", (230, 110, 160)),
    ("essence_crystal_3", "Essence Crystal III", "Cristallo d'Essenza III", (110, 200, 255)),
]:
    oggetto(id_, n_en, n_it)
    if salva_texture(disegna(CRISTALLO, palette(colore), id_), TEX / "item" / f"{id_}.png"):
        texture_create += 1
if salva_texture(disegna(PIETRA_MUTAMENTO, palette((200, 40, 70)), "shifting_stone"), TEX / "item" / "shifting_stone.png"):
    texture_create += 1

# --- ricette ---
PIETRA = ft("shifting_stone")
r_con_forma("shifting_stone", ["RGR", "VDV", "RGR"], {"R": "#c:dusts/redstone", "G": "#c:dusts/glowstone",
                                                     "V": "#c:gems/voltite", "D": "#c:gems/diamond"}, PIETRA)
# conversioni con la Pietra del Mutamento (coerenti con i valori dell'Essenza)
r_senza_forma("shift_iron_to_gold", [PIETRA] + ["#c:ingots/iron"] * 8, "minecraft:gold_ingot")
r_senza_forma("shift_gold_to_iron", [PIETRA, "#c:ingots/gold"], "minecraft:iron_ingot", 8)
r_senza_forma("shift_gold_to_diamond", [PIETRA] + ["#c:ingots/gold"] * 4, "minecraft:diamond")
r_senza_forma("shift_diamond_to_gold", [PIETRA, "#c:gems/diamond"], "minecraft:gold_ingot", 4)
r_senza_forma("shift_coal_to_iron", [PIETRA, "minecraft:coal", "minecraft:coal"], "minecraft:iron_ingot")
r_senza_forma("shift_tin_to_silver", [PIETRA] + ["#c:ingots/tin"] * 5, ft("silver_ingot"))
r_senza_forma("shift_silver_to_tin", [PIETRA, "#c:ingots/silver"], ft("tin_ingot"), 4)
r_con_forma("synthesis_table", ["OSO", "SXS", "OSO"], {"O": "minecraft:obsidian", "S": "minecraft:stone",
                                                      "X": PIETRA}, ft("synthesis_table"))
r_con_forma("essence_crystal", ["VVV", "VDV", "VVV"], {"V": "#c:gems/voltite", "D": "#c:gems/diamond"},
            ft("essence_crystal"))
r_con_forma("essence_crystal_2", ["CCC", "CEC", "CCC"], {"C": ft("essence_crystal"), "E": "#c:gems/emerald"},
            ft("essence_crystal_2"))
r_con_forma("essence_crystal_3", ["CCC", "CNC", "CCC"], {"C": ft("essence_crystal_2"), "N": "minecraft:nether_star"},
            ft("essence_crystal_3"))
r_con_forma("star_collector", ["GGG", "GVG", "GSG"], {"G": "#c:glass_blocks", "V": ft("essence_crystal"),
                                                     "S": "minecraft:glowstone"}, ft("star_collector"))
r_con_forma("star_collector_2", ["CDC", "CDC", "CCC"], {"C": ft("star_collector"), "D": "#c:gems/diamond"},
            ft("star_collector_2"))
r_con_forma("star_collector_3", ["CEC", "CEC", "CCC"], {"C": ft("star_collector_2"), "E": "#c:gems/emerald"},
            ft("star_collector_3"))
r_con_forma("replicator", ["ODO", "DTD", "ODO"], {"O": "minecraft:obsidian", "D": "#c:gems/diamond",
                                                 "T": ft("synthesis_table")}, ft("replicator"))

# --- valori dell'Essenza ---
# Scala: pietra / terra / sabbia = 1, lingotto di ferro = 256 (servono 256 pietre per fare un lingotto).
# Qui ci sono solo i materiali "di partenza" (quelli che si trovano nel mondo): tutto cio' che si fabbrica
# riceve il valore da solo, sommando gli ingredienti della ricetta. Un id preciso vince su un #tag.
VALORI_ESSENZA = {
    # --- terra e pietra ---
    "minecraft:cobblestone": 1, "minecraft:stone": 1, "minecraft:dirt": 1, "minecraft:grass_block": 1,
    "minecraft:sand": 1, "minecraft:red_sand": 1, "minecraft:gravel": 4, "minecraft:netherrack": 1,
    "minecraft:end_stone": 2, "minecraft:cobbled_deepslate": 1, "minecraft:deepslate": 1,
    "minecraft:granite": 1, "minecraft:diorite": 1, "minecraft:andesite": 1, "minecraft:tuff": 1,
    "minecraft:calcite": 2, "minecraft:clay_ball": 16, "minecraft:flint": 4, "minecraft:snowball": 1,
    "minecraft:ice": 1, "minecraft:obsidian": 64, "minecraft:crying_obsidian": 768, "minecraft:soul_sand": 4,
    "minecraft:soul_soil": 4, "minecraft:blackstone": 1, "minecraft:basalt": 1, "minecraft:smooth_basalt": 1,
    "minecraft:mud": 1, "minecraft:moss_block": 8, "minecraft:dripstone_block": 4, "minecraft:pointed_dripstone": 4,
    "minecraft:podzol": 2, "minecraft:mycelium": 2, "minecraft:rooted_dirt": 2, "minecraft:coarse_dirt": 1,
    "minecraft:dirt_path": 1, "minecraft:farmland": 1, "minecraft:suspicious_sand": 1, "minecraft:suspicious_gravel": 4,
    "minecraft:magma_block": 128, "minecraft:cobweb": 12, "minecraft:sponge": 512, "minecraft:wet_sponge": 512,
    "minecraft:crimson_nylium": 2, "minecraft:warped_nylium": 2, "minecraft:sculk": 4, "minecraft:sculk_vein": 4,
    "minecraft:sculk_catalyst": 2048, "minecraft:sculk_shrieker": 2048, "minecraft:sculk_sensor": 1024,
    "minecraft:budding_amethyst": 4096, "minecraft:small_amethyst_bud": 32, "minecraft:medium_amethyst_bud": 48,
    "minecraft:large_amethyst_bud": 64, "minecraft:amethyst_cluster": 128, "minecraft:reinforced_deepslate": 8192,
    "minecraft:snow": 1, "minecraft:powder_snow_bucket": 769, "minecraft:gilded_blackstone": 512,
    "minecraft:infested_stone": 1, "minecraft:infested_cobblestone": 1, "minecraft:infested_deepslate": 1,
    "minecraft:infested_stone_bricks": 1, "minecraft:infested_mossy_stone_bricks": 8,
    "minecraft:infested_cracked_stone_bricks": 1, "minecraft:infested_chiseled_stone_bricks": 1,
    "minecraft:end_portal_frame": 4096, "minecraft:mushroom_stem": 32, "minecraft:brown_mushroom_block": 32,
    "minecraft:red_mushroom_block": 32, "minecraft:frogspawn": 32, "minecraft:turtle_egg": 192,
    "minecraft:sniffer_egg": 1024, "minecraft:dragon_egg": 262144, "minecraft:bell": 4096,
    # --- piante ---
    "#minecraft:logs": 32, "#minecraft:saplings": 32, "#minecraft:leaves": 1, "minecraft:wheat": 24,
    "minecraft:wheat_seeds": 16, "minecraft:sugar_cane": 32, "minecraft:apple": 128, "minecraft:carrot": 64,
    "minecraft:potato": 64, "minecraft:poisonous_potato": 64, "minecraft:beetroot": 64, "minecraft:beetroot_seeds": 16,
    "minecraft:cactus": 8, "minecraft:pumpkin": 144, "minecraft:melon_slice": 16, "minecraft:cocoa_beans": 64,
    "minecraft:kelp": 1, "minecraft:bamboo": 2, "minecraft:vine": 8, "minecraft:lily_pad": 16,
    "#minecraft:small_flowers": 16, "#minecraft:tall_flowers": 32, "minecraft:wither_rose": 256,
    "minecraft:short_grass": 1, "minecraft:tall_grass": 1, "minecraft:fern": 1, "minecraft:large_fern": 1,
    "minecraft:seagrass": 1, "minecraft:dead_bush": 1, "minecraft:brown_mushroom": 32, "minecraft:red_mushroom": 32,
    "minecraft:sweet_berries": 16, "minecraft:glow_berries": 16, "minecraft:nether_wart": 24,
    "minecraft:chorus_fruit": 192, "minecraft:chorus_flower": 192, "minecraft:chorus_plant": 64,
    "minecraft:crimson_fungus": 32, "minecraft:warped_fungus": 32, "minecraft:crimson_roots": 1,
    "minecraft:warped_roots": 1, "minecraft:nether_sprouts": 1, "minecraft:weeping_vines": 8,
    "minecraft:twisting_vines": 8, "minecraft:sea_pickle": 16, "minecraft:glow_lichen": 8,
    "minecraft:hanging_roots": 1, "minecraft:spore_blossom": 64, "minecraft:azalea": 32,
    "minecraft:flowering_azalea": 32, "minecraft:big_dripleaf": 32, "minecraft:small_dripleaf": 32,
    "minecraft:torchflower_seeds": 32, "minecraft:pitcher_pod": 32, "minecraft:pink_petals": 16,
    "minecraft:mangrove_roots": 4, "minecraft:nether_wart_block": 216, "minecraft:warped_wart_block": 216,
    "minecraft:shroomlight": 256, "minecraft:tube_coral": 16, "minecraft:brain_coral": 16, "minecraft:bubble_coral": 16,
    "minecraft:fire_coral": 16, "minecraft:horn_coral": 16, "#minecraft:corals": 16, "#minecraft:coral_blocks": 64,
    "minecraft:dead_tube_coral_block": 16, "minecraft:dead_brain_coral_block": 16, "minecraft:dead_bubble_coral_block": 16,
    "minecraft:dead_fire_coral_block": 16, "minecraft:dead_horn_coral_block": 16, "minecraft:dead_tube_coral": 4,
    "minecraft:dead_brain_coral": 4, "minecraft:dead_bubble_coral": 4, "minecraft:dead_fire_coral": 4,
    "minecraft:dead_horn_coral": 4, "minecraft:tube_coral_fan": 16, "minecraft:brain_coral_fan": 16,
    "minecraft:bubble_coral_fan": 16, "minecraft:fire_coral_fan": 16, "minecraft:horn_coral_fan": 16,
    "minecraft:dead_tube_coral_fan": 4, "minecraft:dead_brain_coral_fan": 4, "minecraft:dead_bubble_coral_fan": 4,
    "minecraft:dead_fire_coral_fan": 4, "minecraft:dead_horn_coral_fan": 4,
    # --- creature ---
    "minecraft:string": 12, "minecraft:feather": 48, "minecraft:leather": 64, "minecraft:bone": 144,
    "minecraft:rotten_flesh": 32, "minecraft:spider_eye": 128, "minecraft:gunpowder": 192, "minecraft:egg": 32,
    "minecraft:ender_pearl": 1024, "minecraft:blaze_rod": 1536, "minecraft:ghast_tear": 4096,
    "minecraft:slime_ball": 32, "minecraft:ink_sac": 16, "minecraft:glow_ink_sac": 32, "minecraft:honeycomb": 32,
    "minecraft:honey_bottle": 64, "minecraft:phantom_membrane": 192, "minecraft:beef": 64, "minecraft:porkchop": 64,
    "minecraft:chicken": 64, "minecraft:mutton": 64, "minecraft:rabbit": 64, "minecraft:cod": 64,
    "minecraft:salmon": 64, "minecraft:tropical_fish": 64, "minecraft:pufferfish": 64, "minecraft:rabbit_hide": 16,
    "minecraft:rabbit_foot": 128, "minecraft:shulker_shell": 4096, "minecraft:nether_star": 139264,
    "minecraft:dragon_breath": 768, "minecraft:turtle_scute": 96, "minecraft:armadillo_scute": 96,
    "minecraft:nautilus_shell": 1024, "minecraft:heart_of_the_sea": 16384, "minecraft:totem_of_undying": 8192,
    "minecraft:trident": 16384, "minecraft:elytra": 65536, "minecraft:saddle": 192, "minecraft:name_tag": 192,
    "minecraft:skeleton_skull": 1024, "minecraft:wither_skeleton_skull": 8192, "minecraft:zombie_head": 1024,
    "minecraft:creeper_head": 1024, "minecraft:piglin_head": 1024, "minecraft:player_head": 1024,
    "minecraft:dragon_head": 32768, "minecraft:goat_horn": 256, "minecraft:breeze_rod": 512,
    "minecraft:heavy_core": 65536, "minecraft:trial_key": 512, "minecraft:ominous_trial_key": 1024,
    "minecraft:ominous_bottle": 512, "minecraft:enchanted_golden_apple": 147456, "minecraft:experience_bottle": 64,
    "minecraft:milk_bucket": 784, "minecraft:water_bucket": 769, "minecraft:lava_bucket": 832,
    "minecraft:cod_bucket": 832, "minecraft:salmon_bucket": 832, "minecraft:tropical_fish_bucket": 832,
    "minecraft:pufferfish_bucket": 832, "minecraft:axolotl_bucket": 1024, "minecraft:tadpole_bucket": 800,
    "minecraft:potion": 32, "minecraft:splash_potion": 224, "minecraft:lingering_potion": 992, "minecraft:tipped_arrow": 16,
    "#c:music_discs": 2048, "minecraft:disc_fragment_5": 256, "#minecraft:decorated_pot_sherds": 256,
    "#minecraft:trim_templates": 16384, "minecraft:netherite_upgrade_smithing_template": 57344,
    "minecraft:chainmail_helmet": 1024, "minecraft:chainmail_chestplate": 2048, "minecraft:chainmail_leggings": 1792,
    "minecraft:chainmail_boots": 1024, "minecraft:iron_horse_armor": 1536, "minecraft:golden_horse_armor": 12288,
    "minecraft:diamond_horse_armor": 49152, "minecraft:enchanted_book": 192,
    # --- minerali di Minecraft ---
    "minecraft:coal": 128, "minecraft:charcoal": 128, "minecraft:redstone": 64, "minecraft:glowstone_dust": 384,
    "minecraft:lapis_lazuli": 864, "minecraft:quartz": 256, "minecraft:amethyst_shard": 64,
    "minecraft:raw_iron": 256, "minecraft:iron_ingot": 256, "minecraft:raw_copper": 128, "minecraft:copper_ingot": 128,
    "minecraft:raw_gold": 2048, "minecraft:gold_ingot": 2048, "minecraft:diamond": 8192, "minecraft:emerald": 16384,
    "minecraft:netherite_scrap": 12288, "minecraft:prismarine_shard": 64, "minecraft:prismarine_crystals": 128,
    "minecraft:echo_shard": 768, "minecraft:ancient_debris": 12288,
    "#c:ores/coal": 128, "#c:ores/iron": 256, "#c:ores/copper": 384, "#c:ores/gold": 2048, "#c:ores/redstone": 256,
    "#c:ores/lapis": 3456, "#c:ores/diamond": 8192, "#c:ores/emerald": 16384, "#c:ores/quartz": 256,
    "minecraft:nether_gold_ore": 1024,
    # --- FutureTek ---
    "futuretek:raw_tin": 160, "futuretek:tin_ingot": 160, "futuretek:raw_lead": 200, "futuretek:lead_ingot": 200,
    "futuretek:raw_silver": 768, "futuretek:silver_ingot": 768, "futuretek:raw_uranium": 3000,
    "futuretek:uranium_ingot": 3000, "futuretek:voltite_shard": 400, "futuretek:sticky_resin": 24,
    "futuretek:rubber": 8, "#c:ores/tin": 160, "#c:ores/lead": 200, "#c:ores/silver": 768, "#c:ores/uranium": 3000,
    "#c:ores/voltite": 800, "futuretek:purified_crushed_tin": 160, "futuretek:purified_crushed_lead": 200,
    "futuretek:purified_crushed_silver": 768, "futuretek:purified_crushed_uranium": 3000,
    "futuretek:purified_crushed_iron": 256, "futuretek:purified_crushed_gold": 2048,
    "futuretek:purified_crushed_copper": 128, "futuretek:depleted_fuel_rod": 256,
    "futuretek:crystal_stone": 1, "futuretek:crystal_soil": 1, "futuretek:crystal_turf": 1,
    "futuretek:voltite_cluster": 400, "futuretek:mineral_comb": 64, "futuretek:metallic_comb": 96,
    "futuretek:frozen_comb": 32, "futuretek:ember_comb": 256, "futuretek:abyssal_comb": 512,
    "futuretek:voltaic_comb": 400, "futuretek:stellar_comb": 4000,
    "futuretek:diamond_dust": 8192, "futuretek:obsidian_dust": 64, "futuretek:bee_queen": 1024,
    "futuretek:bee_drone": 256, "futuretek:wild_hive_common": 256, "futuretek:wild_hive_forest": 256,
    "futuretek:wild_hive_meadow": 256,
    # altri oggetti di Minecraft che non si fabbricano
    "minecraft:tube_coral_block": 64, "minecraft:brain_coral_block": 64, "minecraft:bubble_coral_block": 64,
    "minecraft:fire_coral_block": 64, "minecraft:horn_coral_block": 64, "minecraft:bee_nest": 256,
    "minecraft:ochre_froglight": 256, "minecraft:pearlescent_froglight": 256, "minecraft:verdant_froglight": 256,
    "minecraft:flow_banner_pattern": 512, "minecraft:globe_banner_pattern": 512, "minecraft:guster_banner_pattern": 512,
    "minecraft:piglin_banner_pattern": 512, "minecraft:suspicious_stew": 128, "minecraft:firework_star": 196,
}
# Oggetti che si ottengono nel mondo (ossidazione, acqua, cesoie...) e valgono come la loro versione "base"
COPIE_ESSENZA = {"minecraft:carved_pumpkin": "minecraft:pumpkin", "minecraft:stripped_bamboo_block": "minecraft:bamboo_block",
                 "minecraft:chipped_anvil": "minecraft:anvil", "minecraft:damaged_anvil": "minecraft:anvil",
                 "minecraft:written_book": "minecraft:writable_book", "minecraft:filled_map": "minecraft:map",
                 "futuretek:spent_coolant_cell": "futuretek:coolant_cell",
                 "futuretek:spent_coolant_cell_30k": "futuretek:coolant_cell_30k",
                 "futuretek:spent_coolant_cell_60k": "futuretek:coolant_cell_60k"}
for colore in ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan",
               "purple", "blue", "brown", "green", "red", "black"]:
    COPIE_ESSENZA[f"minecraft:{colore}_concrete"] = f"minecraft:{colore}_concrete_powder"
    COPIE_ESSENZA[f"minecraft:{colore}_shulker_box"] = "minecraft:shulker_box"
for stadio in ["exposed", "weathered", "oxidized"]:
    for forma in ["copper", "chiseled_copper", "copper_bulb", "copper_door", "copper_grate", "copper_trapdoor",
                  "cut_copper", "cut_copper_slab", "cut_copper_stairs"]:
        COPIE_ESSENZA[f"minecraft:{stadio}_{forma}"] = "minecraft:copper_block" if forma == "copper" else f"minecraft:{forma}"
strutture_binarie[f"data/{MODID}/essence_values/base.json"] = (json.dumps(
    {"_info": "Valori dell'Essenza di FutureTek. Gli oggetti non elencati ricevono un valore calcolato dalle ricette. "
              "copies: l'oggetto a sinistra vale quanto quello a destra.",
     "values": VALORI_ESSENZA, "copies": COPIE_ESSENZA}, indent=2, ensure_ascii=False) + "\n").encode("utf-8")

lingua_en.update({
    "tooltip.futuretek.shifting_stone": "Crafting: converts metals. Right-click: transforms blocks (Shift: 3x3)",
    "tooltip.futuretek.essence_stored": "Essence: %s / %s",
    "tooltip.futuretek.essence_value": "Essence: %s (stack: %s)",
    "tooltip.futuretek.essence_cost": "Cost: %s Essence",
    "tooltip.futuretek.synthesis_click": "Click: create 1 · Shift+click: create a stack",
    "tooltip.futuretek.dissolve_slot": "Put an item here to dissolve it into Essence and learn it",
    "message.futuretek.crystal_emptied": "+%s Essence added to your reserve",
    "message.futuretek.crystal_charged": "Crystal charged: +%s (total %s)",
    "message.futuretek.collector": "Collected Essence: %s / %s",
    "gui.futuretek.template": "Template",
})
lingua_it.update({
    "tooltip.futuretek.shifting_stone": "Banco: converte i metalli. Click destro: trasforma i blocchi (Shift: 3x3)",
    "tooltip.futuretek.essence_stored": "Essenza: %s / %s",
    "tooltip.futuretek.essence_value": "Essenza: %s (pila: %s)",
    "tooltip.futuretek.essence_cost": "Costo: %s Essenza",
    "tooltip.futuretek.synthesis_click": "Click: crea 1 · Shift+click: crea una pila",
    "tooltip.futuretek.dissolve_slot": "Metti qui un oggetto per scioglierlo in Essenza e impararlo",
    "message.futuretek.crystal_emptied": "+%s Essenza aggiunta alla tua riserva",
    "message.futuretek.crystal_charged": "Cristallo caricato: +%s (totale %s)",
    "message.futuretek.collector": "Essenza raccolta: %s / %s",
    "gui.futuretek.template": "Modello",
})

# ===========================================================================
# FASE 5 - LOGISTICA
# ===========================================================================
COLORI_CONDOTTI = {
    "item_conduit": ((150, 150, 150), (200, 220, 230)),
    "extractor_conduit": ((150, 120, 90), (220, 160, 80)),
    "fast_extractor_conduit": ((210, 180, 60), (255, 230, 120)),
    "sorting_conduit": ((110, 90, 160), (190, 160, 240)),
    "analyzer_conduit": ((50, 120, 110), (120, 230, 200)),
}
NOMI_CONDOTTI = {
    "item_conduit": ("Logistic Pipe", "Tubo logistico"),
    "extractor_conduit": ("Extractor Pipe", "Tubo estrattore"),
    "fast_extractor_conduit": ("Fast Extractor Pipe", "Tubo estrattore veloce"),
    "sorting_conduit": ("Destination Pipe", "Tubo di destinazione (filtro)"),
    "analyzer_conduit": ("Analyzer Pipe", "Tubo analizzatore"),
}


def texture_condotto(seme, base, vetro):
    rng = random.Random(seme)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            bordo = x in (0, 15) or y in (0, 15) or x % 5 == 0
            c = rumore(base, rng, 0.05) if bordo else rumore(vetro, rng, 0.04)
            img.putpixel((x, y), c + (255,))
    return img


for id_, (base, vetro) in COLORI_CONDOTTI.items():
    n_en, n_it = NOMI_CONDOTTI[id_]
    blocco(id_, n_en, n_it)
    if salva_texture(texture_condotto(id_, base, vetro), TEX / "block" / f"{id_}.png"):
        texture_create += 1
    modelli_speciali[id_] = modelli_cavo(id_, 6)

blocco("network_terminal", "Network Terminal", "Terminale di rete")
blocco("auto_crafter", "Auto Crafter", "Banco di assemblaggio automatico")


def fronte_terminale(seme, acceso):
    img = texture_carcassa(seme, (80, 96, 100))
    finestra(img, 2, 2, 13, 10, [(20, 60, 70), (30, 80, 90)], seme)
    for y in (4, 6, 8):
        for x in range(4, 12, 2):
            img.putpixel((x, y), (120, 240, 220, 255))
    for x in range(4, 12):
        img.putpixel((x, 12), (60, 60, 64, 255))
    return img


def cima_assemblatore(seme):
    img = texture_carcassa(seme, (110, 90, 70))
    for i in range(3, 13):
        for j in (5, 10):
            img.putpixel((i, j), (60, 44, 30, 255))
            img.putpixel((j, i), (60, 44, 30, 255))
    return img


for img, nome in [
    (fronte_terminale("network_terminal_front", False), "block/network_terminal_front"),
    (texture_carcassa("logistics_side", (80, 96, 100)), "block/logistics_side"),
    (cima_assemblatore("auto_crafter_top"), "block/auto_crafter_top"),
    (texture_carcassa("auto_crafter_side", (110, 90, 70)), "block/auto_crafter_side"),
]:
    if salva_texture(img, TEX / f"{nome}.png"):
        texture_create += 1


def modello_terminale():
    A_ = f"assets/{MODID}"
    scrivi_json(f"{A_}/models/block/network_terminal.json", {"parent": "minecraft:block/cube", "textures": {
        "north": f"{MODID}:block/network_terminal_front", "south": f"{MODID}:block/network_terminal_front",
        "east": f"{MODID}:block/network_terminal_front", "west": f"{MODID}:block/network_terminal_front",
        "up": f"{MODID}:block/logistics_side", "down": f"{MODID}:block/logistics_side",
        "particle": f"{MODID}:block/logistics_side"}})
    scrivi_json(f"{A_}/blockstates/network_terminal.json", {"variants": {"": {"model": f"{MODID}:block/network_terminal"}}})
    scrivi_json(f"{A_}/models/item/network_terminal.json", {"parent": f"{MODID}:block/network_terminal"})


modelli_speciali["network_terminal"] = modello_terminale
modelli_speciali["auto_crafter"] = modello_alto_lato("auto_crafter", "auto_crafter_top", "auto_crafter_side")
for id_ in ["network_terminal", "auto_crafter"]:
    tag(tag_blocchi, "minecraft:mineable/pickaxe", ft(id_))

# --- porte logiche ---
SIMBOLI_PORTE = {   # disegno 5x5 al centro della lastra
    "and": ["XXX..", "X..X.", "X...X", "X..X.", "XXX.."],
    "or": ["XXX..", ".X.X.", "..X.X", ".X.X.", "XXX.."],
    "xor": ["X.XX.", ".X.X.", "..X.X", ".X.X.", "X.XX."],
    "nand": ["XX..X", "X.X..", "X..XX", "X.X..", "XX..X"],
    "not": ["X....", "XXX..", "X..XX", "XXX..", "X...."],
    "latch": ["XXXXX", "X...X", "X.X.X", "X...X", "XXXXX"],
    "timer": [".XXX.", "X.X.X", "X.XXX", "X...X", ".XXX."],
    "random": ["XX.XX", "X...X", "..X..", "X...X", "XX.XX"],
}
NOMI_PORTE = {"and": ("AND Gate", "Porta AND"), "or": ("OR Gate", "Porta OR"), "xor": ("XOR Gate", "Porta XOR"),
              "nand": ("NAND Gate", "Porta NAND"), "not": ("NOT Gate", "Porta NOT"),
              "latch": ("Latch", "Memoria (latch)"), "timer": ("Timer", "Temporizzatore"),
              "random": ("Randomizer", "Generatore casuale")}


def texture_porta(tipo, acceso):
    rng = random.Random(tipo)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), rumore((150, 150, 150), rng, 0.04) + (255,))
    colore = (240, 40, 30) if acceso else (110, 30, 30)
    # freccia verso l'alto (= nord = uscita)
    for i, y in enumerate(range(1, 4)):
        for x in range(7 - i, 9 + i):
            img.putpixel((x, y), colore + (255,))
    for y in range(4, 6):
        for x in range(7, 9):
            img.putpixel((x, y), colore + (255,))
    for dy, riga in enumerate(SIMBOLI_PORTE[tipo]):
        for dx, ch in enumerate(riga):
            if ch == "X":
                img.putpixel((5 + dx, 8 + dy), (40, 40, 44, 255))
    return img


for tipo, (n_en, n_it) in NOMI_PORTE.items():
    id_ = f"{tipo}_gate"
    blocco(id_, n_en, n_it)
    for acceso, suffisso in [(False, ""), (True, "_on")]:
        if salva_texture(texture_porta(tipo, acceso), TEX / "block" / f"{id_}{suffisso}.png"):
            texture_create += 1

    def scrivi_porta(id_=id_):
        A_ = f"assets/{MODID}"
        for suffisso in ["", "_on"]:
            scrivi_json(f"{A_}/models/block/{id_}{suffisso}.json", {
                "parent": "minecraft:block/block",
                "textures": {"top": f"{MODID}:block/{id_}{suffisso}", "side": "minecraft:block/smooth_stone",
                             "particle": "minecraft:block/smooth_stone"},
                "elements": [{"from": [0, 0, 0], "to": [16, 2, 16], "faces": {
                    "up": {"texture": "#top", "uv": [0, 0, 16, 16]},
                    "down": {"texture": "#side", "uv": [0, 0, 16, 16], "cullface": "down"},
                    "north": {"texture": "#side", "uv": [0, 14, 16, 16], "cullface": "north"},
                    "south": {"texture": "#side", "uv": [0, 14, 16, 16], "cullface": "south"},
                    "east": {"texture": "#side", "uv": [0, 14, 16, 16], "cullface": "east"},
                    "west": {"texture": "#side", "uv": [0, 14, 16, 16], "cullface": "west"}}}]})
        rot = {"north": 0, "east": 90, "south": 180, "west": 270}
        varianti = {}
        for d, gradi in rot.items():
            for p in ["false", "true"]:
                for t in ["false", "true"]:
                    v = {"model": f"{MODID}:block/{id_}" + ("_on" if p == "true" else "")}
                    if gradi:
                        v["y"] = gradi
                    varianti[f"facing={d},powered={p},triggered={t}"] = v
        scrivi_json(f"{A_}/blockstates/{id_}.json", {"variants": varianti})
        scrivi_json(f"{A_}/models/item/{id_}.json", {"parent": f"{MODID}:block/{id_}"})

    modelli_speciali[id_] = scrivi_porta

# --- ricette ---
r_con_forma("item_conduit", ["SGS"], {"S": "#c:stones", "G": "#c:glass_blocks"}, ft("item_conduit"), 8)
r_senza_forma("extractor_conduit", [ft("item_conduit"), "minecraft:hopper"], ft("extractor_conduit"))
r_senza_forma("fast_extractor_conduit", [ft("extractor_conduit"), "#c:ingots/gold", "#c:dusts/redstone"],
              ft("fast_extractor_conduit"))
r_senza_forma("sorting_conduit", [ft("item_conduit"), "#c:ingots/iron", "minecraft:comparator"], ft("sorting_conduit"))
r_senza_forma("analyzer_conduit", [ft("item_conduit"), "minecraft:observer", "#c:dusts/redstone"], ft("analyzer_conduit"))
r_con_forma("network_terminal", ["PGP", "CMC", "PWP"],
            {"P": PIASTRA_FERRO, "G": "#c:glass_panes", "C": C, "M": M, "W": ft("item_conduit")}, ft("network_terminal"))
r_con_forma("auto_crafter", ["PTP", "CMC", "PWP"],
            {"P": PIASTRA_FERRO, "T": "minecraft:crafting_table", "C": C, "M": M, "W": ft("item_conduit")},
            ft("auto_crafter"))
INGREDIENTE_PORTE = {"and": "#c:ingots/iron", "or": "#c:nuggets/gold", "xor": "#c:gems/quartz",
                     "nand": "#c:ingots/copper", "not": "minecraft:redstone_torch", "latch": "minecraft:lever",
                     "timer": "minecraft:clock", "random": "#c:gems/voltite"}
for tipo, extra in INGREDIENTE_PORTE.items():
    r_con_forma(f"{tipo}_gate", [" X ", "RTR", "SSS"], {"X": extra, "R": "#c:dusts/redstone",
                                                      "T": "minecraft:redstone_torch", "S": "minecraft:smooth_stone_slab"},
                ft(f"{tipo}_gate"))

lingua_en.update({
    "gui.futuretek.filter_hint": "Click with an item to add it, empty hand to remove",
    "gui.futuretek.analyzer_memory": "Remembers %s item types · extra filter:",
    "gui.futuretek.keep_one.on": "Keep 1: YES",
    "gui.futuretek.keep_one.off": "Keep 1: NO",
    "gui.futuretek.keep_one.hint": "YES: the terminal and the auto crafter always leave 1 item of each type in this chest",
    "gui.futuretek.forget": "Forget",
    "gui.futuretek.forget.hint": "Forgets everything and learns again from what is in the chest now",
    "gui.futuretek.queued": "Queued: %s",
    "gui.futuretek.search": "Search...",
    "gui.futuretek.in_network": "In network: %s",
    "gui.futuretek.craft_hint": "Middle-click or Ctrl+click: craft (Shift: 64)",
    "message.futuretek.no_network": "Place this next to an item conduit",
    "message.futuretek.craft_started": "Crafting %s x %s...",
    "message.futuretek.craft_missing": "Missing ingredients:%s",
})
lingua_it.update({
    "gui.futuretek.filter_hint": "Click con un oggetto per aggiungerlo, a mano vuota per toglierlo",
    "gui.futuretek.analyzer_memory": "Ricorda %s tipi di oggetti · filtro extra:",
    "gui.futuretek.keep_one.on": "Lascia 1: SÌ",
    "gui.futuretek.keep_one.off": "Lascia 1: NO",
    "gui.futuretek.keep_one.hint": "SÌ: il terminale e il banco di assemblaggio lasciano sempre 1 oggetto di ogni tipo in questo baule",
    "gui.futuretek.forget": "Dimentica",
    "gui.futuretek.forget.hint": "Dimentica tutto e reimpara da quello che c'è adesso nel baule",
    "gui.futuretek.queued": "In coda: %s",
    "gui.futuretek.search": "Cerca...",
    "gui.futuretek.in_network": "Nella rete: %s",
    "gui.futuretek.craft_hint": "Click centrale o Ctrl+click: fabbrica (Shift: 64)",
    "message.futuretek.no_network": "Mettilo accanto a un condotto per oggetti",
    "message.futuretek.craft_started": "Fabbrico %s x %s...",
    "message.futuretek.craft_missing": "Mancano ingredienti:%s",
})

# ===========================================================================
# FASE 6 - TECNOLOGIA AVANZATA
# ===========================================================================
blocco("reactor_core", "Reactor Core", "Nucleo del reattore")
blocco("reactor_chamber", "Reactor Chamber", "Camera del reattore")
blocco("quarry", "Quarry", "Escavatore automatico")
PIOMBO = MATERIALI["lead"][2]


def fronte_reattore(seme, acceso):
    img = texture_carcassa(seme, PIOMBO)
    colori = [(80, 255, 120), (140, 255, 170), (60, 210, 100)] if acceso else [(30, 70, 40), (40, 90, 50)]
    finestra(img, 4, 4, 11, 11, colori, seme)
    # simbolo: tre spicchi
    for (x, y) in [(7, 5), (8, 5), (5, 9), (6, 10), (10, 9), (9, 10)]:
        img.putpixel((x, y), (30, 30, 30, 255))
    return img


def lato_camera(seme):
    img = texture_carcassa(seme, PIOMBO)
    for y in range(3, 13, 3):
        for x in range(3, 13):
            img.putpixel((x, y), mix(PIOMBO, (0, 0, 0), 0.4) + (255,))
    return img


def fronte_escavatore(seme, acceso):
    img = texture_carcassa(seme, (190, 150, 50))
    finestra(img, 3, 3, 12, 12, [(40, 40, 44)], seme)
    punta = (230, 230, 240) if acceso else (150, 150, 160)
    for i in range(5):
        for x in range(5 + i, 11 - i):
            img.putpixel((x, 5 + i), punta + (255,))
    return img


for img, nome in [
    (fronte_reattore("reactor_front", False), "block/reactor_core_front"),
    (fronte_reattore("reactor_front", True), "block/reactor_core_front_on"),
    (texture_carcassa("reactor_side", PIOMBO), "block/reactor_side"),
    (lato_camera("reactor_chamber"), "block/reactor_chamber"),
    (fronte_escavatore("quarry_front", False), "block/quarry_front"),
    (fronte_escavatore("quarry_front", True), "block/quarry_front_on"),
    (texture_carcassa("quarry_side", (190, 150, 50)), "block/quarry_side"),
]:
    if salva_texture(img, TEX / f"{nome}.png"):
        texture_create += 1
modelli_speciali["reactor_core"] = modelli_macchina("reactor_core", "reactor_core_front", "reactor_core_front_on",
                                                    top="reactor_side", side="reactor_side", bottom="reactor_side")
modelli_speciali["quarry"] = modelli_macchina("quarry", "quarry_front", "quarry_front_on", side="quarry_side")
for id_ in ["reactor_core", "reactor_chamber", "quarry"]:
    tag(tag_blocchi, "minecraft:mineable/pickaxe", ft(id_))
    tag(tag_blocchi, "minecraft:needs_iron_tool", ft(id_))

BARRA = [
    "................",
    "......oooo......",
    "......o++o......",
    ".....oooooo.....",
    ".....o+##-o.....",
    ".....o+gg-o.....",
    ".....o+gg-o.....",
    ".....o+gg-o.....",
    ".....o+gg-o.....",
    ".....o+gg-o.....",
    ".....o+gg-o.....",
    ".....o+##-o.....",
    ".....oooooo.....",
    "......o--o......",
    "......oooo......",
    "................",
]
GRIGLIA_VENT = [
    "................",
    ".oooooooooooooo.",
    ".o++++++++++++o.",
    ".o+kk+kk+kk+k-o.",
    ".o++++++++++++o.",
    ".o+kk+kk+kk+k-o.",
    ".o++++++++++++o.",
    ".o+kk+kk+kk+k-o.",
    ".o++++++++++++o.",
    ".o+kk+kk+kk+k-o.",
    ".o++++++++++++o.",
    ".o+kk+kk+kk+k-o.",
    ".o------------o.",
    ".oooooooooooooo.",
    "................",
    "................",
]
COMPONENTI = {
    "fuel_rod": ("Uranium Fuel Rod", "Barra di uranio", "rod1"),
    "dual_fuel_rod": ("Dual Uranium Fuel Rod", "Barra di uranio doppia", "rod2"),
    "quad_fuel_rod": ("Quad Uranium Fuel Rod", "Barra di uranio quadrupla", "rod4"),
    "heat_vent": ("Heat Vent", "Dissipatore di calore", "vent"),
    "coolant_cell": ("Coolant Cell", "Cella refrigerante", "coolant"),
    "reactor_plating": ("Reactor Plating", "Piastra del reattore", "plating"),
    "depleted_fuel_rod": ("Depleted Fuel Rod", "Barra esaurita", "depleted"),
}
for id_, (n_en, n_it, stile) in COMPONENTI.items():
    oggetto(id_, n_en, n_it)
    if stile.startswith("rod") or stile == "depleted":
        verde = (110, 110, 110) if stile == "depleted" else (120, 230, 90)
        img = disegna(BARRA, {**palette(MATERIALI["iron"][2]), "g": verde}, id_)
        if stile in ("rod2", "rod4"):
            # disegna barre in piu' affiancate
            base = img.copy()
            img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
            spostamenti = [-3, 3] if stile == "rod2" else [-4, -1, 2, 5]
            for dx in spostamenti:
                stretta = base.resize((8, 16), Image.NEAREST)
                img.alpha_composite(stretta, (4 + dx, 0))
    elif stile == "vent":
        img = disegna(GRIGLIA_VENT, {**palette(ACCIAIO), "k": (30, 30, 34)}, id_)
    elif stile == "coolant":
        img = disegna(CELLA, palette((70, 160, 230)), id_)
    else:
        img = disegna(FORME["plate"], palette(PIOMBO), id_)
    if salva_texture(img, TEX / "item" / f"{id_}.png"):
        texture_create += 1
lingua_en.update({
    "tooltip.futuretek.reactor.fuel_rod": "Fuel: more energy and MUCH more heat when next to other rods",
    "tooltip.futuretek.reactor.dual_fuel_rod": "Two rods in one",
    "tooltip.futuretek.reactor.quad_fuel_rod": "Four rods in one",
    "tooltip.futuretek.reactor.heat_vent": "Takes heat from adjacent rods, removes 12 heat/s",
    "tooltip.futuretek.reactor.coolant_cell": "Stores up to 10,000 heat from adjacent rods",
    "tooltip.futuretek.reactor.reactor_plating": "+1,000 max hull heat",
})
lingua_it.update({
    "tooltip.futuretek.reactor.fuel_rod": "Combustibile: più energia e MOLTO più calore se vicino ad altre barre",
    "tooltip.futuretek.reactor.dual_fuel_rod": "Due barre in una",
    "tooltip.futuretek.reactor.quad_fuel_rod": "Quattro barre in una",
    "tooltip.futuretek.reactor.heat_vent": "Prende il calore delle barre accanto e ne smaltisce 12/s",
    "tooltip.futuretek.reactor.coolant_cell": "Accumula fino a 10.000 calore delle barre accanto",
    "tooltip.futuretek.reactor.reactor_plating": "+1.000 calore massimo dello scafo",
})

# esoarmatura e zaino
COLORE_EXO = (60, 90, 120)
for tipo, disegno in ARMATURE.items():
    id_ = f"exo_{tipo}"
    n_en, n_it = NOMI_ARMATURE[tipo]
    oggetto(id_, f"Exo {n_en}", f"{n_it} esoarmatura")
    colori = {**palette(COLORE_EXO), "#": (70, 110, 150)}
    img = disegna(disegno, colori, id_)
    # linee luminose azzurre
    for x in range(16):
        for y in range(16):
            if img.getpixel((x, y))[3] and (x + y) % 7 == 0:
                img.putpixel((x, y), (80, 220, 255, 255))
    if salva_texture(img, TEX / "item" / f"{id_}.png"):
        texture_create += 1
    tag(tag_oggetti, f"minecraft:{TAG_ARMATURE[tipo]}", ft(id_))
ZAINO = [
    "................",
    "...oooo..oooo...",
    "..o+##-oo+##-o..",
    "..o+##-oo+##-o..",
    "..o+##-oo+##-o..",
    "..o+##-oo+##-o..",
    "..o+##-oo+##-o..",
    "..o+##-oo+##-o..",
    "..o----oo----o..",
    "...oooo..oooo...",
    "...oFFo..oFFo...",
    "...oFYo..oYFo...",
    "....FY....YF....",
    ".....Y....Y.....",
    "................",
    "................",
]
oggetto("jetpack", "Electric Jetpack", "Zaino a reazione")
if salva_texture(disegna(ZAINO, {**palette(ACCIAIO), "F": (255, 120, 30), "Y": (255, 220, 80)}, "jetpack"),
                 TEX / "item" / "jetpack.png"):
    texture_create += 1
for nome, colore in [("exo", COLORE_EXO), ("jetpack", ACCIAIO)]:
    for strato in (1, 2):
        if salva_texture(texture_armatura_indossata(colore, strato, f"{nome}_layer_{strato}"),
                         TEX / "models" / "armor" / f"{nome}_layer_{strato}.png"):
            texture_create += 1
lingua_en.update({
    "tooltip.futuretek.exo.helmet": "Night vision (5 TU/t)",
    "tooltip.futuretek.exo.chestplate": "Energy shield: -30% damage",
    "tooltip.futuretek.exo.leggings": "Sprint boost",
    "tooltip.futuretek.exo.boots": "Jump boost, no fall damage",
    "tooltip.futuretek.jetpack": "Hold JUMP to fly (8 TU/t)",
    "gui.futuretek.heat": "Heat: %s / %s",
    "message.futuretek.reactor_scram": "REACTOR EMERGENCY SHUTDOWN: overheating! Fuel rods destroyed.",
    "message.futuretek.quarry": "Quarry: layer Y=%s, energy %s TU",
    "message.futuretek.quarry_done": "Quarry: area completed",
})
lingua_it.update({
    "tooltip.futuretek.exo.helmet": "Visione notturna (5 TU/t)",
    "tooltip.futuretek.exo.chestplate": "Scudo energetico: -30% danni",
    "tooltip.futuretek.exo.leggings": "Scatto potenziato",
    "tooltip.futuretek.exo.boots": "Salto potenziato, niente danni da caduta",
    "tooltip.futuretek.jetpack": "Tieni premuto SALTO per volare (8 TU/t)",
    "gui.futuretek.heat": "Calore: %s / %s",
    "message.futuretek.reactor_scram": "SPEGNIMENTO D'EMERGENZA DEL REATTORE: surriscaldamento! Barre distrutte.",
    "message.futuretek.quarry": "Escavatore: strato Y=%s, energia %s TU",
    "message.futuretek.quarry_done": "Escavatore: area completata",
})

# ricette
r_con_forma("fuel_rod", ["PUP", "PUP", "PUP"], {"P": PIASTRA_FERRO, "U": "#c:ingots/uranium"}, ft("fuel_rod"))
r_con_forma("dual_fuel_rod", ["RPR"], {"R": ft("fuel_rod"), "P": PIASTRA_FERRO}, ft("dual_fuel_rod"))
r_con_forma("quad_fuel_rod", ["RPR", "PCP", "RPR"], {"R": ft("fuel_rod"), "P": PIASTRA_FERRO, "C": "#c:plates/copper"},
            ft("quad_fuel_rod"))
r_con_forma("heat_vent", ["PBP", "B B", "PBP"], {"P": PIASTRA_FERRO, "B": "minecraft:iron_bars"}, ft("heat_vent"))
r_con_forma("coolant_cell", [" T ", "TWT", " T "], {"T": "#c:plates/tin", "W": "minecraft:water_bucket"},
            ft("coolant_cell"))
r_con_forma("reactor_plating", ["LL", "LL"], {"L": "#c:plates/lead"}, ft("reactor_plating"))
r_con_forma("reactor_chamber", ["LPL", "PMP", "LPL"], {"L": "#c:plates/lead", "P": PIASTRA_FERRO, "M": M},
            ft("reactor_chamber"))
r_con_forma("reactor_core", ["CXC", "XMX", "CXC"],
            {"C": ft("reactor_chamber"), "X": ft("basic_circuit"), "M": ft("battery_box_2")}, ft("reactor_core"))
r_con_forma("quarry", ["CDC", "PMP", "PXP"], {"C": C, "D": "minecraft:diamond_pickaxe", "P": "#c:plates/steel",
                                             "M": M, "X": ft("ejector_upgrade")}, ft("quarry"))
SCHEMI_EXO = {"helmet": ["MCM", "M M"], "chestplate": ["M M", "MCM", "MMM"],
              "leggings": ["MCM", "M M", "M M"], "boots": ["M M", "MCM"]}
for tipo, schema in SCHEMI_EXO.items():
    r_con_forma(f"exo_{tipo}", schema, {"M": "#c:plates/steel", "C": ft("advanced_battery_cell")}, ft(f"exo_{tipo}"))
r_con_forma("jetpack", ["SCS", "SBS", "R R"], {"S": "#c:plates/steel", "C": C, "B": ft("battery_box"),
                                              "R": "#c:dusts/redstone"}, ft("jetpack"))

# ===========================================================================
# FASE 7 - FERROVIE
# ===========================================================================
def texture_binario(seme, colore_rotaie, colore_traversine=(110, 80, 50), segno=None):
    rng = random.Random(seme)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(0, 16, 3):            # traversine
        for x in range(1, 15):
            img.putpixel((x, y), rumore(colore_traversine, rng, 0.08) + (255,))
    for y in range(16):                   # rotaie
        for x in (2, 3, 12, 13):
            c = colore_rotaie if x in (2, 13) else mix(colore_rotaie, (255, 255, 255), 0.3)
            img.putpixel((x, y), c + (255,))
    if segno:
        for (x, y) in [(7, 7), (8, 7), (7, 8), (8, 8), (7, 6), (8, 9)]:
            img.putpixel((x, y), segno + (255,))
    return img


BINARI = {
    "reinforced_rail": ("Reinforced Rail", "Binario rinforzato", "curve", (120, 124, 134), None, None),
    "induction_rail": ("Induction Rail", "Binario a induzione", "powered", (226, 123, 82), (80, 60, 50), (255, 210, 80)),
    "holding_rail": ("Holding Rail", "Binario di blocco", "powered", (120, 124, 134), (70, 70, 74), (230, 50, 40)),
    "loader_rail": ("Loader Rail", "Binario di carico", "curve", (120, 124, 134), (60, 140, 70), None),
    "unloader_rail": ("Unloader Rail", "Binario di scarico", "curve", (120, 124, 134), (160, 70, 60), None),
}


def modelli_binario(id_, tipo):
    def scrivi():
        A_ = f"assets/{MODID}"
        varianti_tex = [("", f"{MODID}:block/{id_}")]
        if tipo == "powered":
            varianti_tex.append(("_on", f"{MODID}:block/{id_}_on"))
        for suffisso, tex in varianti_tex:
            for forma in ["rail_flat", "rail_raised_ne", "rail_raised_sw"] + (["rail_curved"] if tipo == "curve" else []):
                scrivi_json(f"{A_}/models/block/{id_}{suffisso}_{forma}.json",
                            {"parent": f"minecraft:block/{forma}", "render_type": "minecraft:cutout", "textures": {"rail": tex}})
        m = lambda forma, suff="": f"{MODID}:block/{id_}{suff}_{forma}"
        if tipo == "curve":
            mappa = {"north_south": (m("rail_flat"), 0), "east_west": (m("rail_flat"), 90),
                     "ascending_east": (m("rail_raised_ne"), 90), "ascending_west": (m("rail_raised_sw"), 90),
                     "ascending_north": (m("rail_raised_ne"), 0), "ascending_south": (m("rail_raised_sw"), 0),
                     "south_east": (m("rail_curved"), 0), "south_west": (m("rail_curved"), 90),
                     "north_west": (m("rail_curved"), 180), "north_east": (m("rail_curved"), 270)}
            varianti = {}
            for forma, (modello, y) in mappa.items():
                v = {"model": modello}
                if y:
                    v["y"] = y
                varianti[f"shape={forma}"] = v
        else:
            varianti = {}
            for acceso, suff in [("false", ""), ("true", "_on")]:
                mappa = {"north_south": ("rail_flat", 0), "east_west": ("rail_flat", 90),
                         "ascending_east": ("rail_raised_ne", 90), "ascending_west": ("rail_raised_sw", 90),
                         "ascending_north": ("rail_raised_ne", 0), "ascending_south": ("rail_raised_sw", 0)}
                for forma, (modello, y) in mappa.items():
                    v = {"model": m(modello, suff)}
                    if y:
                        v["y"] = y
                    varianti[f"powered={acceso},shape={forma}"] = v
        scrivi_json(f"{A_}/blockstates/{id_}.json", {"variants": varianti})
        scrivi_json(f"{A_}/models/item/{id_}.json", {"parent": "minecraft:item/generated",
                                                     "textures": {"layer0": f"{MODID}:block/{id_}"}})
    return scrivi


for id_, (n_en, n_it, tipo, rotaie, traversine, segno) in BINARI.items():
    blocco(id_, n_en, n_it)
    if salva_texture(texture_binario(id_, rotaie, traversine or (110, 80, 50), None if tipo == "powered" else segno),
                     TEX / "block" / f"{id_}.png"):
        texture_create += 1
    if tipo == "powered" and salva_texture(texture_binario(id_ + "_on", rotaie, traversine, segno),
                                           TEX / "block" / f"{id_}_on.png"):
        texture_create += 1
    modelli_speciali[id_] = modelli_binario(id_, tipo)
    for d in (tag_oggetti, tag_blocchi):
        tag(d, "minecraft:rails", ft(id_))
    tag(tag_blocchi, "minecraft:mineable/pickaxe", ft(id_))
    # i binari curvi/alimentati hanno la loro "trasparenza": render_type cutout

CARRELLO = [
    "................",
    "................",
    "................",
    "....oooooooo....",
    "...o+######-o...",
    "...o+#bbbb#-o...",
    "..oo+#bbbb#-oo..",
    "..o++++++++--o..",
    "..o+########-o..",
    "..o+########-o..",
    "..o----------o..",
    "...oo.oooo.oo...",
    "...ok.o..o.ko...",
    "....o......o....",
    "................",
    "................",
]
oggetto("cargo_cart", "Cargo Cart", "Carrello merci")
if salva_texture(disegna(CARRELLO, {**palette(ACCIAIO), "b": (140, 100, 60), "k": (30, 30, 30)}, "cargo_cart"),
                 TEX / "item" / "cargo_cart.png"):
    texture_create += 1
lingua_en["entity.futuretek.cargo_cart"] = "Cargo Cart"
lingua_it["entity.futuretek.cargo_cart"] = "Carrello merci"

ACCIAIO_ING = "#c:ingots/steel"
r_con_forma("reinforced_rail", ["S S", "SRS", "S S"], {"S": ACCIAIO_ING, "R": "#c:rods/wooden"}, ft("reinforced_rail"), 16)
r_con_forma("induction_rail", ["CRC", "RXR", "CRC"], {"C": ft("insulated_copper_cable"), "R": ft("reinforced_rail"),
                                                     "X": C}, ft("induction_rail"), 4)
r_con_forma("holding_rail", ["R R", "RPR", "R R"], {"R": ft("reinforced_rail"), "P": "minecraft:stone_pressure_plate"},
            ft("holding_rail"), 4)
r_senza_forma("loader_rail", [ft("reinforced_rail"), "minecraft:hopper"], ft("loader_rail"))
r_senza_forma("unloader_rail", [ft("reinforced_rail"), "minecraft:hopper", "#c:dusts/redstone"], ft("unloader_rail"))
r_senza_forma("cargo_cart", ["minecraft:chest_minecart", "#c:chests/wooden", "#c:plates/steel", "#c:plates/steel"],
              ft("cargo_cart"))

# ===========================================================================
# FASE 8 - AGRICOLTURA E API
# ===========================================================================
blocco("growth_accelerator", "Growth Accelerator", "Acceleratore di crescita")
blocco("harvester", "Harvester", "Raccoglitore automatico")
blocco("apiary", "Apiary", "Arnia tecnica")


def fronte_acceleratore(seme, acceso):
    img = texture_carcassa(seme, (70, 120, 70))
    colori = [(120, 255, 120), (80, 220, 90)] if acceso else [(40, 80, 40), (50, 90, 50)]
    finestra(img, 4, 4, 11, 11, colori, seme)
    for (x, y) in [(7, 9), (8, 9), (7, 8), (6, 7), (9, 7), (7, 6)]:
        img.putpixel((x, y), (30, 60, 30, 255))
    return img


def fronte_raccoglitore(seme, acceso):
    img = texture_carcassa(seme, (150, 120, 60))
    finestra(img, 3, 4, 12, 11, [(40, 40, 44)], seme)
    lama = (230, 230, 240) if acceso else (150, 150, 160)
    for x in range(4, 12):
        img.putpixel((x, 7 + (x % 2)), lama + (255,))
    return img


def texture_arnia(seme, lato):
    rng = random.Random(seme)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = (170, 130, 70) if (y // 4) % 2 == 0 else (150, 110, 60)
            img.putpixel((x, y), rumore(c, rng, 0.05) + (255,))
    if lato == "front":
        finestra(img, 6, 9, 9, 12, [(40, 30, 20)], seme)
    return img


def texture_alveare(seme, colore):
    rng = random.Random(seme)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            esagono = ((x + (y // 3) % 2 * 2) % 4 == 0) or y % 3 == 0
            c = mix(colore, (0, 0, 0), 0.35) if esagono else colore
            img.putpixel((x, y), rumore(c, rng, 0.06) + (255,))
    return img


for img, nome in [
    (fronte_acceleratore("growth_accelerator_front", False), "block/growth_accelerator_front"),
    (fronte_acceleratore("growth_accelerator_front", True), "block/growth_accelerator_front_on"),
    (fronte_raccoglitore("harvester_front", False), "block/harvester_front"),
    (fronte_raccoglitore("harvester_front", True), "block/harvester_front_on"),
    (texture_arnia("apiary_side", "side"), "block/apiary_side"),
    (texture_arnia("apiary_front", "front"), "block/apiary_front"),
    (texture_carcassa("apiary_top", (140, 100, 50)), "block/apiary_top"),
]:
    if salva_texture(img, TEX / f"{nome}.png"):
        texture_create += 1
modelli_speciali["growth_accelerator"] = modelli_macchina("growth_accelerator", "growth_accelerator_front",
                                                          "growth_accelerator_front_on")
modelli_speciali["harvester"] = modelli_macchina("harvester", "harvester_front", "harvester_front_on")


def modello_arnia():
    A_ = f"assets/{MODID}"
    scrivi_json(f"{A_}/models/block/apiary.json", {"parent": "minecraft:block/cube", "textures": {
        "north": f"{MODID}:block/apiary_front", "south": f"{MODID}:block/apiary_side",
        "east": f"{MODID}:block/apiary_side", "west": f"{MODID}:block/apiary_side",
        "up": f"{MODID}:block/apiary_top", "down": f"{MODID}:block/apiary_top",
        "particle": f"{MODID}:block/apiary_side"}})
    scrivi_json(f"{A_}/blockstates/apiary.json", {"variants": {"": {"model": f"{MODID}:block/apiary"}}})
    scrivi_json(f"{A_}/models/item/apiary.json", {"parent": f"{MODID}:block/apiary"})


modelli_speciali["apiary"] = modello_arnia
for id_ in ["growth_accelerator", "harvester"]:
    tag(tag_blocchi, "minecraft:mineable/pickaxe", ft(id_))
tag(tag_blocchi, "minecraft:mineable/axe", ft("apiary"))

# alveari selvatici
SPECIE = {"common": ("Common", "Comune"), "forest": ("Forest", "Forestale"), "meadow": ("Meadow", "Prateria"),
          "mineral": ("Mineral", "Minerale"), "metallic": ("Metallic", "Metallica"), "glacial": ("Glacial", "Glaciale"),
          "ember": ("Ember", "Ardente"), "abyssal": ("Abyssal", "Abissale"), "voltaic": ("Voltaic", "Voltaica"),
          "stellar": ("Stellar", "Stellare")}
for sp, (n_en, n_it) in SPECIE.items():
    lingua_en[f"bee.futuretek.{sp}"] = n_en
    lingua_it[f"bee.futuretek.{sp}"] = n_it
ALVEARI = {"common": (232, 192, 74), "forest": (110, 160, 70), "meadow": (230, 150, 200)}
for sp, colore in ALVEARI.items():
    id_ = f"wild_hive_{sp}"
    blocco(id_, f"Wild Hive ({SPECIE[sp][0]})", f"Alveare selvatico ({SPECIE[sp][1].lower()})")
    if salva_texture(texture_alveare(id_, colore), TEX / "block" / f"{id_}.png"):
        texture_create += 1
    tag(tag_blocchi, "minecraft:mineable/axe", ft(id_))
    componenti = {"futuretek:bee_species": sp}
    loot_speciali[id_] = {
        "type": "minecraft:block",
        "pools": [
            {"rolls": 1.0, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:item", "name": ft("bee_queen"),
                "functions": [{"function": "minecraft:set_components", "components": componenti}]}]},
            {"rolls": 1.0, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:item", "name": ft("bee_drone"),
                "functions": [{"function": "minecraft:set_components", "components": componenti},
                              {"function": "minecraft:set_count",
                               "count": {"type": "minecraft:uniform", "min": 1.0, "max": 3.0}}]}]},
        ],
        "random_sequence": f"{MODID}:blocks/{id_}",
    }
BIOMI_ALVEARI = {"common": "#minecraft:is_overworld", "forest": "#minecraft:is_forest", "meadow": "#c:is_plains"}
for sp, biomi in BIOMI_ALVEARI.items():
    id_ = f"wild_hive_{sp}"
    worldgen[f"data/{MODID}/worldgen/configured_feature/{id_}.json"] = {
        "type": "minecraft:simple_block",
        "config": {"to_place": {"type": "minecraft:simple_state_provider", "state": {"Name": ft(id_)}}}}
    worldgen[f"data/{MODID}/worldgen/placed_feature/{id_}.json"] = {
        "feature": ft(id_),
        "placement": [
            {"type": "minecraft:rarity_filter", "chance": 32 if sp == "common" else 16},
            {"type": "minecraft:in_square"},
            {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"},
            {"type": "minecraft:biome"},
            {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
                {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"},
                {"type": "minecraft:matching_block_tag", "offset": [0, -1, 0], "tag": "minecraft:dirt"}]}},
        ]}
    worldgen[f"data/{MODID}/neoforge/biome_modifier/{id_}.json"] = {
        "type": "neoforge:add_features", "biomes": biomi, "features": ft(id_), "step": "vegetal_decoration"}

# api (icona a 2 livelli: il livello 1 viene colorato in base alla specie)
APE_BASE = [
    "................",
    "................",
    "....oo....oo....",
    "...oWWo..oWWo...",
    "...oWWWooWWWo...",
    "....oWWWWWWo....",
    ".....oo..oo.....",
    "....o......o....",
    "...o........o...",
    "...o........o...",
    "...o........o...",
    "....o......o....",
    ".....o....o.....",
    "......oooo......",
    "................",
    "................",
]
APE_CORPO = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "......####......",
    ".....######.....",
    "....#kk##kk#....",
    "....########....",
    "....#kk##kk#....",
    ".....######.....",
    "......####......",
    "................",
    "................",
    "................",
]
REGINA_CORONA = [(6, 4), (8, 3), (10, 4)]
for tipo, n_en, n_it in [("bee_queen", "%s Queen", "Regina %s"), ("bee_drone", "%s Drone", "Fuco %s")]:
    oggetto(tipo, n_en, n_it)
    base = disegna(APE_BASE, {"o": (40, 40, 40), "W": (230, 240, 255)}, tipo)
    corpo = disegna(APE_CORPO, {"#": (255, 255, 255), "k": (90, 90, 90)}, tipo + "_corpo")
    if tipo == "bee_queen":
        for (x, y) in REGINA_CORONA:
            base.putpixel((x, y), (255, 215, 0, 255))
    for img, nome in [(base, f"item/{tipo}"), (corpo, f"item/{tipo}_body")]:
        if salva_texture(img, TEX / f"{nome}.png"):
            texture_create += 1
    modelli_speciali_oggetti = globals().setdefault("modelli_speciali_oggetti", {})
    modelli_speciali_oggetti[tipo] = {"parent": "minecraft:item/generated", "textures": {
        "layer0": f"{MODID}:item/{tipo}", "layer1": f"{MODID}:item/{tipo}_body"}}
lingua_en["tooltip.futuretek.bee_life"] = "Cycles left: %s"
lingua_it["tooltip.futuretek.bee_life"] = "Cicli rimasti: %s"
lingua_en.update({"gui.futuretek.queen": "Queen", "gui.futuretek.drone": "Drone"})
lingua_it.update({"gui.futuretek.queen": "Regina", "gui.futuretek.drone": "Fuco"})

# favi
FAVO = [
    "................",
    "................",
    "...oooooooooo...",
    "..o+#+#+#+#+#o..",
    "..o#k#k#k#k#-o..",
    "..o+#+#+#+#+#o..",
    "..o#k#k#k#k#-o..",
    "..o+#+#+#+#+#o..",
    "..o#k#k#k#k#-o..",
    "..o+#+#+#+#+#o..",
    "..o#k#k#k#k#-o..",
    "..o-#-#-#-#--o..",
    "...oooooooooo...",
    "................",
    "................",
    "................",
]
FAVI = {"mineral": ("Mineral Comb", "Favo minerale", (150, 150, 150)),
        "metallic": ("Metallic Comb", "Favo metallico", (200, 210, 220)),
        "frozen": ("Frozen Comb", "Favo ghiacciato", (160, 225, 255)),
        "ember": ("Ember Comb", "Favo ardente", (255, 110, 30)),
        "abyssal": ("Abyssal Comb", "Favo abissale", (70, 50, 100)),
        "voltaic": ("Voltaic Comb", "Favo voltaico", (190, 90, 255)),
        "stellar": ("Stellar Comb", "Favo stellare", (255, 240, 150))}
for f, (n_en, n_it, colore) in FAVI.items():
    id_ = f"{f}_comb"
    oggetto(id_, n_en, n_it)
    if salva_texture(disegna(FAVO, {**palette(colore), "k": mix(colore, (0, 0, 0), 0.5)}, id_),
                     TEX / "item" / f"{id_}.png"):
        texture_create += 1
# centrifuga termica: favi -> prodotti
r_macchina("centrifuging", "mineral_comb", ft("mineral_comb"), 1, ft("coal_dust"), 1, ft("iron_dust"), 0.25, 200)
r_macchina("centrifuging", "metallic_comb", ft("metallic_comb"), 1, ft("tin_dust"), 1, ft("silver_dust"), 0.2, 200)
r_macchina("centrifuging", "frozen_comb", ft("frozen_comb"), 1, "minecraft:snowball", 4, "minecraft:blue_ice", 0.05, 200)
r_macchina("centrifuging", "ember_comb", ft("ember_comb"), 1, "minecraft:blaze_powder", 1, "minecraft:magma_cream", 0.2, 200)
r_macchina("centrifuging", "abyssal_comb", ft("abyssal_comb"), 1, "minecraft:ender_pearl", 1, "minecraft:chorus_fruit", 0.3, 300)
r_macchina("centrifuging", "voltaic_comb", ft("voltaic_comb"), 1, ft("voltite_shard"), 2, ft("voltite_shard"), 0.3, 200)
r_macchina("centrifuging", "stellar_comb", ft("stellar_comb"), 1, "minecraft:glowstone_dust", 4, "minecraft:nether_star", 0.01, 400)

# prodotti e incroci (file JSON modificabile)
DATI_API = {
    "_info": "Prodotti (probabilita' per ciclo) e incroci delle api di FutureTek",
    "products": {
        "common": [{"item": "minecraft:honeycomb", "chance": 0.4}],
        "forest": [{"item": "minecraft:honeycomb", "chance": 0.3}, {"item": "futuretek:sticky_resin", "chance": 0.3}],
        "meadow": [{"item": "minecraft:honeycomb", "chance": 0.4}, {"item": "minecraft:bone_meal", "chance": 0.2}],
        "mineral": [{"item": "futuretek:mineral_comb", "chance": 0.35}],
        "metallic": [{"item": "futuretek:metallic_comb", "chance": 0.3}],
        "glacial": [{"item": "futuretek:frozen_comb", "chance": 0.35}],
        "ember": [{"item": "futuretek:ember_comb", "chance": 0.3}],
        "abyssal": [{"item": "futuretek:abyssal_comb", "chance": 0.2}],
        "voltaic": [{"item": "futuretek:voltaic_comb", "chance": 0.25}],
        "stellar": [{"item": "futuretek:stellar_comb", "chance": 0.15}],
    },
    "mutations": [
        {"parents": ["common", "forest"], "result": "mineral", "chance": 0.15},
        {"parents": ["common", "meadow"], "result": "glacial", "chance": 0.12},
        {"parents": ["mineral", "meadow"], "result": "metallic", "chance": 0.10},
        {"parents": ["mineral", "forest"], "result": "ember", "chance": 0.10},
        {"parents": ["metallic", "ember"], "result": "voltaic", "chance": 0.08},
        {"parents": ["glacial", "ember"], "result": "abyssal", "chance": 0.08},
        {"parents": ["voltaic", "abyssal"], "result": "stellar", "chance": 0.05},
    ],
}
strutture_binarie[f"data/{MODID}/bee_data/bees.json"] = (json.dumps(DATI_API, indent=2, ensure_ascii=False) + "\n").encode()

# fertilizzante e ricette
FERTILIZZANTE = [
    "................",
    "................",
    "....oooooooo....",
    "...o++++++++o...",
    "...o+gggggg-o...",
    "...o+gg##gg-o...",
    "...o+g####g-o...",
    "...o+gg##gg-o...",
    "...o+gggggg-o...",
    "...o+gggggg-o...",
    "...o--------o...",
    "....oooooooo....",
    "................",
    "................",
    "................",
    "................",
]
oggetto("fertilizer", "Fertilizer", "Fertilizzante")
if salva_texture(disegna(FERTILIZZANTE, {**palette((150, 110, 70)), "g": (110, 80, 50), "#": (90, 200, 80)},
                         "fertilizer"), TEX / "item" / "fertilizer.png"):
    texture_create += 1
r_senza_forma("fertilizer", ["minecraft:bone_meal", "minecraft:bone_meal", "#c:dusts/coal", "minecraft:rotten_flesh"],
              ft("fertilizer"), 4)
r_con_forma("growth_accelerator", ["GBG", "CMC", "GBG"], {"G": "#c:glass_blocks", "B": "minecraft:bone_block", "C": C,
                                                         "M": M}, ft("growth_accelerator"))
r_con_forma("harvester", ["HHH", "CMC", "PXP"], {"H": "minecraft:iron_hoe", "C": C, "M": M, "P": PIASTRA_FERRO,
                                                "X": "minecraft:shears"}, ft("harvester"))
r_con_forma("apiary", ["PPP", "HCH", "PPP"], {"P": "#minecraft:planks", "H": "minecraft:honeycomb", "C": "#c:chests/wooden"},
            ft("apiary"))

# ===========================================================================
# FASE 9 - IL MARGINE CRISTALLINO (dimensione)
# ===========================================================================
COLORE_PIETRA_CRISTALLO = (96, 70, 130)
for id_, n_en, n_it in [
    ("crystal_stone", "Crystal Stone", "Pietra cristallina"),
    ("crystal_soil", "Crystal Soil", "Terra cristallina"),
    ("crystal_turf", "Crystal Turf", "Prato cristallino"),
    ("voltite_block", "Block of Voltite", "Blocco di voltite"),
    ("voltite_cluster", "Voltite Cluster", "Grappolo di voltite"),
    ("edge_anchor", "Edge Anchor", "Ancora del Margine"),
]:
    blocco(id_, n_en, n_it)


def texture_pietra_cristallo(seme):
    rng = random.Random(seme)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            r = rng.random()
            c = (150, 120, 200) if r > 0.93 else mix(COLORE_PIETRA_CRISTALLO, (0, 0, 0), 0.2) if r < 0.25 else COLORE_PIETRA_CRISTALLO
            img.putpixel((x, y), rumore(c, rng, 0.05) + (255,))
    return img


def texture_terra_cristallo(seme):
    rng = random.Random(seme)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = (120, 80, 120) if rng.random() > 0.2 else (90, 60, 100)
            img.putpixel((x, y), rumore(c, rng, 0.07) + (255,))
    return img


def texture_prato_cima(seme):
    rng = random.Random(seme)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = (110, 200, 230) if rng.random() > 0.3 else (80, 160, 210)
            img.putpixel((x, y), rumore(c, rng, 0.06) + (255,))
    return img


def texture_prato_lato(seme):
    img = texture_terra_cristallo(seme + "_terra")
    rng = random.Random(seme)
    for x in range(16):
        h = 3 + rng.randrange(3)
        for y in range(h):
            img.putpixel((x, y), rumore((100, 190, 225), rng, 0.06) + (255,))
    return img


GRAPPOLO = [
    "................",
    "................",
    ".......oo.......",
    "......o+#o......",
    "..oo..o+#o..oo..",
    ".o+#o.o+#o.o+#o.",
    ".o+#o.o+#-oo+#o.",
    "..o#-oo+#-o+#-o.",
    "..o+#-o+#-o+#o..",
    "...o+#o+#-+#-o..",
    "...o+#-+#-##o...",
    "....o+##--#-o...",
    ".....o+##--o....",
    "......o###o.....",
    ".......ooo......",
    "................",
]


def texture_ancora(seme, lato):
    img = texture_blocco((60, 40, 90), seme)
    if lato == "top":
        for y in range(16):
            for x in range(16):
                d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
                if d < 3.2:
                    img.putpixel((x, y), (200, 140, 255, 255))
                elif 4.5 < d < 5.5:
                    img.putpixel((x, y), (150, 90, 220, 255))
    else:
        finestra(img, 6, 3, 9, 12, [(190, 90, 255), (220, 150, 255)], seme)
    return img


for img, nome in [
    (texture_pietra_cristallo("crystal_stone"), "block/crystal_stone"),
    (texture_terra_cristallo("crystal_soil"), "block/crystal_soil"),
    (texture_prato_cima("crystal_turf_top"), "block/crystal_turf_top"),
    (texture_prato_lato("crystal_turf_side"), "block/crystal_turf_side"),
    (texture_blocco(COLORE_VOLTITE, "voltite_block"), "block/voltite_block"),
    (disegna(GRAPPOLO, palette(COLORE_VOLTITE), "voltite_cluster"), "block/voltite_cluster"),
    (texture_ancora("edge_anchor_top", "top"), "block/edge_anchor_top"),
    (texture_ancora("edge_anchor_side", "side"), "block/edge_anchor_side"),
]:
    if salva_texture(img, TEX / f"{nome}.png"):
        texture_create += 1

modelli_speciali["crystal_turf"] = modello_alto_lato("crystal_turf", "crystal_turf_top", "crystal_turf_side", "crystal_soil")
modelli_speciali["edge_anchor"] = modello_alto_lato("edge_anchor", "edge_anchor_top", "edge_anchor_side")


def modello_grappolo():
    A_ = f"assets/{MODID}"
    scrivi_json(f"{A_}/models/block/voltite_cluster.json", {"parent": "minecraft:block/cross",
                                                            "render_type": "minecraft:cutout",
                                                            "textures": {"cross": f"{MODID}:block/voltite_cluster"}})
    scrivi_json(f"{A_}/blockstates/voltite_cluster.json", {"variants": {"": {"model": f"{MODID}:block/voltite_cluster"}}})
    scrivi_json(f"{A_}/models/item/voltite_cluster.json", {"parent": "minecraft:item/generated",
                                                           "textures": {"layer0": f"{MODID}:block/voltite_cluster"}})


modelli_speciali["voltite_cluster"] = modello_grappolo
for id_ in ["crystal_stone", "voltite_block", "voltite_cluster", "edge_anchor"]:
    tag(tag_blocchi, "minecraft:mineable/pickaxe", ft(id_))
for id_ in ["crystal_soil", "crystal_turf"]:
    tag(tag_blocchi, "minecraft:mineable/shovel", ft(id_))
for d in (tag_oggetti, tag_blocchi):
    tag(d, "c:storage_blocks/voltite", ft("voltite_block"))
    tag(d, "c:storage_blocks", "#c:storage_blocks/voltite")
# il prato si raccoglie come terra cristallina; il grappolo da' 2-4 frammenti
loot_speciali["crystal_turf"] = {"type": "minecraft:block", "pools": [{"rolls": 1.0, "bonus_rolls": 0.0, "entries": [
    {"type": "minecraft:alternatives", "children": [
        {"type": "minecraft:item", "conditions": [SETA], "name": ft("crystal_turf")},
        {"type": "minecraft:item", "name": ft("crystal_soil")}]}]}],
    "random_sequence": f"{MODID}:blocks/crystal_turf"}
loot_speciali["voltite_cluster"] = {"type": "minecraft:block", "pools": [{"rolls": 1.0, "bonus_rolls": 0.0, "entries": [
    {"type": "minecraft:alternatives", "children": [
        {"type": "minecraft:item", "conditions": [SETA], "name": ft("voltite_cluster")},
        {"type": "minecraft:item", "name": ft("voltite_shard"), "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 2.0, "max": 4.0}},
            {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:uniform_bonus_count",
             "parameters": {"bonusMultiplier": 1}}]}]}]}],
    "random_sequence": f"{MODID}:blocks/voltite_cluster"}

r_con_forma("voltite_block", ["###", "###", "###"], {"#": "#c:gems/voltite"}, ft("voltite_block"))
r_senza_forma("voltite_shard_from_block", [ft("voltite_block")], ft("voltite_shard"), 9)
r_con_forma("edge_anchor", ["VEV", "EOE", "VEV"], {"V": ft("voltite_block"), "E": "minecraft:ender_pearl",
                                                  "O": "minecraft:crying_obsidian"}, ft("edge_anchor"))
lingua_en["message.futuretek.edge_missing"] = "The Crystal Edge dimension is not available"
lingua_it["message.futuretek.edge_missing"] = "La dimensione del Margine Cristallino non e' disponibile"

# --- definizione della dimensione ---
worldgen[f"data/{MODID}/dimension_type/crystal_edge.json"] = {
    "ultrawarm": False, "natural": False, "coordinate_scale": 1.0, "has_skylight": True, "has_ceiling": False,
    "ambient_light": 0.15, "fixed_time": 12800, "monster_spawn_light_level": 0, "monster_spawn_block_light_limit": 0,
    "piglin_safe": False, "bed_works": False, "respawn_anchor_works": False, "has_raids": False,
    "logical_height": 256, "min_y": 0, "height": 256, "infiniburn": "#minecraft:infiniburn_overworld",
    "effects": "minecraft:overworld"}
COSTANTE0 = 0.0
densita = {"type": "minecraft:add",
           "argument1": {"type": "minecraft:y_clamped_gradient", "from_y": 40, "to_y": 200,
                         "from_value": 1.3, "to_value": -1.3},
           "argument2": {"type": "minecraft:mul", "argument1": 1.1,
                         "argument2": {"type": "minecraft:noise", "noise": "minecraft:ridge",
                                       "xz_scale": 0.55, "y_scale": 0.35}}}
worldgen[f"data/{MODID}/worldgen/noise_settings/crystal_edge.json"] = {
    "sea_level": 64, "disable_mob_generation": False, "aquifers_enabled": False, "ore_veins_enabled": False,
    "legacy_random_source": False,
    "default_block": {"Name": ft("crystal_stone")}, "default_fluid": {"Name": "minecraft:water", "Properties": {"level": "0"}},
    "noise": {"min_y": 0, "height": 256, "size_horizontal": 1, "size_vertical": 2},
    "noise_router": {
        "barrier": COSTANTE0, "fluid_level_floodedness": COSTANTE0, "fluid_level_spread": COSTANTE0, "lava": COSTANTE0,
        "temperature": COSTANTE0, "vegetation": COSTANTE0, "continents": COSTANTE0, "erosion": COSTANTE0,
        "depth": COSTANTE0, "ridges": COSTANTE0, "initial_density_without_jaggedness": densita,
        "final_density": {"type": "minecraft:interpolated", "argument": densita},
        "vein_toggle": COSTANTE0, "vein_ridged": COSTANTE0, "vein_gap": COSTANTE0},
    "spawn_target": [],
    "surface_rule": {"type": "minecraft:sequence", "sequence": [
        {"type": "minecraft:condition",
         "if_true": {"type": "minecraft:vertical_gradient", "random_name": f"{MODID}:edge_bedrock",
                     "true_at_and_below": {"above_bottom": 0}, "false_at_and_above": {"above_bottom": 4}},
         "then_run": {"type": "minecraft:block", "result_state": {"Name": "minecraft:bedrock"}}},
        {"type": "minecraft:condition",
         "if_true": {"type": "minecraft:stone_depth", "offset": 0, "add_surface_depth": False,
                     "secondary_depth_range": 0, "surface_type": "floor"},
         "then_run": {"type": "minecraft:condition",
                      "if_true": {"type": "minecraft:water", "offset": -1, "surface_depth_multiplier": 0,
                                  "add_stone_depth": False},
                      "then_run": {"type": "minecraft:block", "result_state": {"Name": ft("crystal_turf")}}}},
        {"type": "minecraft:condition",
         "if_true": {"type": "minecraft:stone_depth", "offset": 0, "add_surface_depth": True,
                     "secondary_depth_range": 0, "surface_type": "floor"},
         "then_run": {"type": "minecraft:block", "result_state": {"Name": ft("crystal_soil")}}},
    ]}}
worldgen[f"data/{MODID}/dimension/crystal_edge.json"] = {
    "type": f"{MODID}:crystal_edge",
    "generator": {"type": "minecraft:noise", "settings": f"{MODID}:crystal_edge",
                  "biome_source": {"type": "minecraft:fixed", "biome": f"{MODID}:crystal_highlands"}}}
# caratteristiche del bioma: minerali piu' ricchi e grappoli di voltite
for nome, minerale, vene, grandezza, ymax in [("edge_voltite", "voltite_ore", 10, 8, 200),
                                               ("edge_uranium", "uranium_ore", 4, 5, 90),
                                               ("edge_silver", "silver_ore", 6, 7, 140)]:
    worldgen[f"data/{MODID}/worldgen/configured_feature/{nome}.json"] = {"type": "minecraft:ore", "config": {
        "discard_chance_on_air_exposure": 0.0, "size": grandezza, "targets": [
            {"state": {"Name": ft(minerale)},
             "target": {"predicate_type": "minecraft:block_match", "block": ft("crystal_stone")}}]}}
    worldgen[f"data/{MODID}/worldgen/placed_feature/{nome}.json"] = {"feature": ft(nome), "placement": [
        {"type": "minecraft:count", "count": vene}, {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform",
                                                      "min_inclusive": {"absolute": 4}, "max_inclusive": {"absolute": ymax}}},
        {"type": "minecraft:biome"}]}
worldgen[f"data/{MODID}/worldgen/configured_feature/voltite_clusters.json"] = {"type": "minecraft:random_patch", "config": {
    "tries": 24, "xz_spread": 6, "y_spread": 3, "feature": {
        "feature": {"type": "minecraft:simple_block", "config": {
            "to_place": {"type": "minecraft:simple_state_provider", "state": {"Name": ft("voltite_cluster")}}}},
        "placement": [{"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
            {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"},
            {"type": "minecraft:matching_blocks", "offset": [0, -1, 0], "blocks": ft("crystal_turf")}]}}]}}}
worldgen[f"data/{MODID}/worldgen/placed_feature/voltite_clusters.json"] = {"feature": ft("voltite_clusters"), "placement": [
    {"type": "minecraft:count", "count": 2}, {"type": "minecraft:in_square"},
    {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"}, {"type": "minecraft:biome"}]}
worldgen[f"data/{MODID}/worldgen/biome/crystal_highlands.json"] = {
    "has_precipitation": False, "temperature": 0.6, "downfall": 0.0,
    "effects": {"sky_color": 0xB070FF, "fog_color": 0x8A60C8, "water_color": 0x8870FF, "water_fog_color": 0x3D1A7A,
                "grass_color": 0x70C8E8, "foliage_color": 0x70C8E8},
    "spawners": {"monster": [{"type": "minecraft:enderman", "weight": 10, "minCount": 1, "maxCount": 2},
                             {"type": "minecraft:endermite", "weight": 5, "minCount": 1, "maxCount": 3}]},
    "spawn_costs": {},
    "carvers": {},
    "features": [[], [], [], [], [], [], [ft("edge_voltite"), ft("edge_uranium"), ft("edge_silver")], [], [],
                 [ft("voltite_clusters")], []]}
lingua_en["biome.futuretek.crystal_highlands"] = "Crystal Highlands"
lingua_it["biome.futuretek.crystal_highlands"] = "Altopiani di cristallo"

# ===========================================================================
# FASE 4b - ALCHIMIA AVANZATA (oggetti ispirati alle meccaniche alchemiche, nomi e disegni nostri)
# ===========================================================================
def _vuota():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def _px(img, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), tuple(c[:3]) + (255,))


def sprite_brace(colore, nucleo, seme):
    """Brace: un grumo irregolare con un cuore luminoso"""
    rng = random.Random(seme)
    p = palette(colore)
    img = _vuota()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 / 1.1 + (y - 8.5) ** 2) ** 0.5 + rng.uniform(-0.6, 0.6)
            if d < 6.2:
                c = p["o"] if d > 5.4 else p["-"] if d > 4.2 else colore
                if d < 2.6:
                    c = nucleo
                elif d < 3.6 and rng.random() < 0.5:
                    c = mix(colore, nucleo, 0.5)
                _px(img, x, y, rumore(c, rng, 0.05))
    for (x, y) in [(5, 5), (6, 4), (10, 6)]:
        _px(img, x, y, p["+"])
    return img


GEMMA = [
    "................",
    "................",
    ".....oooooo.....",
    "....o++++++o....",
    "...o+++##+++o...",
    "..o++######-+o..",
    "..o+########-o..",
    "..o-########-o..",
    "...o-######-o...",
    "....o-####-o....",
    ".....o-##-o.....",
    "......o--o......",
    ".......oo.......",
    "................",
    "................",
    "................",
]


def sprite_anello(metallo, gemma, seme):
    """Anello: cerchio di metallo con una pietra in alto"""
    pm = palette(metallo)
    pg = palette(gemma)
    img = _vuota()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 9.0) ** 2) ** 0.5
            if 3.6 < d < 5.8:
                c = pm["o"] if d > 5.1 or d < 4.1 else (pm["+"] if x + y < 15 else pm["#"] if x + y < 19 else pm["-"])
                _px(img, x, y, c)
    for (x, y, ch) in [(7, 1, "o"), (8, 1, "o"), (6, 2, "o"), (7, 2, "+"), (8, 2, "#"), (9, 2, "o"),
                       (5, 3, "o"), (6, 3, "+"), (7, 3, "#"), (8, 3, "#"), (9, 3, "-"), (10, 3, "o"),
                       (6, 4, "o"), (7, 4, "-"), (8, 4, "-"), (9, 4, "o"), (7, 5, "o"), (8, 5, "o")]:
        _px(img, x, y, pg[ch])
    return img


def sprite_amuleto(gemma, seme):
    """Amuleto: catenella a V e pendente rotondo"""
    pg = palette(gemma)
    oro = palette((220, 180, 70))
    img = _vuota()
    for i in range(6):
        _px(img, 3 + i, 1 + i, oro["#"] if i % 2 == 0 else oro["-"])
        _px(img, 12 - i, 1 + i, oro["#"] if i % 2 == 0 else oro["-"])
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 10.5) ** 2) ** 0.5
            if d < 4.3:
                if d > 3.4:
                    c = oro["o"]
                elif d > 2.6:
                    c = oro["#"]
                else:
                    c = pg["+"] if x + y < 16 else pg["#"] if x + y < 20 else pg["-"]
                _px(img, x, y, c)
    return img


def sprite_lente(seme):
    img = _vuota()
    cornice = palette((200, 170, 80))
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 7:
                if d > 6:
                    c = cornice["o"]
                elif d > 5:
                    c = cornice["#"] if x + y < 15 else cornice["-"]
                else:
                    c = (120, 220, 240) if x + y > 12 else (200, 250, 255)
                    if 1.5 < d < 3 and x < 8 and y < 8:
                        c = (255, 255, 255)
                _px(img, x, y, c)
    return img


def sprite_catalizzatore(seme):
    rng = random.Random(seme)
    img = _vuota()
    scuro = palette((60, 40, 70))
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 5.5:
                c = scuro["o"] if d > 4.8 else scuro["+"] if x + y < 12 else scuro["#"]
                if d < 4.8 and rng.random() < 0.18:
                    c = (255, 90, 60)
                _px(img, x, y, rumore(c, rng, 0.04))
    for (x, y) in [(7, 0), (8, 0), (7, 1), (8, 1), (7, 14), (8, 14), (7, 15), (8, 15), (0, 7), (0, 8), (1, 7), (1, 8),
                   (14, 7), (14, 8), (15, 7), (15, 8)]:
        _px(img, x, y, (255, 140, 60))
    _px(img, 7, 7, (255, 230, 150))
    _px(img, 8, 8, (255, 200, 100))
    return img


CLESSIDRA = [
    "................",
    "...oooooooooo...",
    "...oWWWWWWWWo...",
    "....o++++++o....",
    "....o+YYYY-o....",
    ".....o+YY-o.....",
    "......o+Yo......",
    ".......oo.......",
    "......o+.o......",
    ".....o+.Y-o.....",
    "....o+.YYY-o....",
    "....o+YYYYYo....",
    "...oWWWWWWWWo...",
    "...oooooooooo...",
    "................",
    "................",
]
BISACCIA = [
    "................",
    "......oooo......",
    ".....o-..-o.....",
    "....o-....-o....",
    "...oooooooooo...",
    "..o++++++++++o..",
    "..o+########-o..",
    "..o+###PP###-o..",
    "..o+##PGGP##-o..",
    "..o+###PP###-o..",
    "..o+########-o..",
    "..o+########-o..",
    "..o-########-o..",
    "...o--------o...",
    "....oooooooo....",
    "................",
]
BACCHETTA = [
    "................",
    "...........oo...",
    "..........o++o..",
    "..........o#Go..",
    "...........oo...",
    "..........oW....",
    ".........oWo....",
    "........oWo.....",
    ".......oWo......",
    "......oWo.......",
    ".....oWo........",
    "....oWo.........",
    "...oWo..........",
    "..oWo...........",
    "..oo............",
    "................",
]
RABDOMANTE = [
    "................",
    "..oo........oo..",
    "..oWo......oWo..",
    "...oWo....oWo...",
    "....oWo..oWo....",
    ".....oWooWo.....",
    "......oWWo......",
    "......oWWo......",
    ".......oWo......",
    ".......oWo......",
    ".......owo......",
    ".......oWo......",
    ".......owo......",
    ".......oGo......",
    "........o.......",
    "................",
]
TALISMANO = [
    "................",
    ".......oo.......",
    "......o++o......",
    ".....o+##-o.....",
    "....o+#GG#-o....",
    "...o+#G##G#-o...",
    "..o+#G#GG#G#-o..",
    "..o+#G#GG#G#-o..",
    "...o-#G##G#-o...",
    "....o-#GG#-o....",
    ".....o-##-o.....",
    "......o--o......",
    ".......oo.......",
    "................",
    "................",
    "................",
]
TAVOLETTA = [
    "................",
    "..oooooooooooo..",
    "..o++++++++++o..",
    "..o+########-o..",
    "..o+##GGGG##-o..",
    "..o+#G####G#-o..",
    "..o+#G#GG#G#-o..",
    "..o+#G#GG#G#-o..",
    "..o+#G####G#-o..",
    "..o+##GGGG##-o..",
    "..o+########-o..",
    "..o+########-o..",
    "..o----------o..",
    "..oooooooooooo..",
    "................",
    "................",
]
TOMO = [
    "................",
    "...ooooooooooo..",
    "..o+++++++++Wo..",
    "..o+#######-Wo..",
    "..o+##GGG##-Wo..",
    "..o+#G###G#-Wo..",
    "..o+#G#G#G#-Wo..",
    "..o+#G###G#-Wo..",
    "..o+##GGG##-Wo..",
    "..o+#######-Wo..",
    "..o+#######-Wo..",
    "..o+#######-Wo..",
    "..o---------Wo..",
    "...ooooooooooo..",
    "................",
    "................",
]
MAGLIO = [
    "................",
    "..oooooooooooo..",
    ".o++++++++++++o.",
    ".o+##########-o.",
    ".o+##########-o.",
    ".o------------o.",
    "..ooooooWWoooo..",
    ".......oWWo.....",
    ".......oWwo.....",
    ".......oWWo.....",
    ".......oWwo.....",
    ".......oWWo.....",
    ".......oWwo.....",
    ".......oWWo.....",
    "........oo......",
    "................",
]

NOCTURNITE_C = (70, 45, 95)
ARDORITE_C = (200, 50, 50)
COLORI_ALCHIMIA = {"W": (130, 90, 55), "w": (95, 65, 40), "G": (200, 120, 255), "Y": (240, 210, 120),
                   "P": (190, 110, 240)}

# --- materiali ---
for id_, n_en, n_it, colore, nucleo in [
    ("arcane_ember", "Arcane Ember", "Brace Arcana", (150, 50, 70), (255, 140, 90)),
    ("stellar_ember", "Stellar Ember", "Brace Stellare", (200, 150, 40), (255, 250, 200)),
    ("eternal_ember", "Eternal Ember", "Brace Eterna", (50, 150, 190), (220, 255, 255)),
]:
    oggetto(id_, n_en, n_it)
    if salva_texture(sprite_brace(colore, nucleo, id_), TEX / "item" / f"{id_}.png"):
        texture_create += 1
for id_, n_en, n_it, colore in [("nocturnite", "Nocturnite", "Nocturnite", NOCTURNITE_C),
                                ("ardorite", "Ardorite", "Ardorite", ARDORITE_C)]:
    oggetto(id_, n_en, n_it)
    if salva_texture(disegna(GEMMA, palette(colore), id_), TEX / "item" / f"{id_}.png"):
        texture_create += 1
    blocco(f"{id_}_block", f"Block of {n_en}", f"Blocco di {n_it}")
    if salva_texture(texture_blocco(colore, f"{id_}_block"), TEX / "block" / f"{id_}_block.png"):
        texture_create += 1
    tag(tag_blocchi, "minecraft:mineable/pickaxe", ft(f"{id_}_block"))
    tag(tag_blocchi, "minecraft:needs_diamond_tool", ft(f"{id_}_block"))
    tag(tag_blocchi, "minecraft:beacon_base_blocks", ft(f"{id_}_block"))
    tag(tag_oggetti, "minecraft:beacon_payment_items", ft(id_))
    r_con_forma(f"{id_}_block", ["MMM", "MMM", "MMM"], {"M": ft(id_)}, ft(f"{id_}_block"))
    r_senza_forma(f"{id_}_from_block", [ft(f"{id_}_block")], ft(id_), 9)
oggetto("essence_band", "Essence Band", "Fascia d'Essenza")
if salva_texture(sprite_anello((170, 170, 180), (180, 110, 240), "essence_band"), TEX / "item" / "essence_band.png"):
    texture_create += 1
oggetto("essence_crystal_4", "Essence Crystal IV", "Cristallo d'Essenza IV")
if salva_texture(disegna(CRISTALLO, palette((255, 90, 90)), "essence_crystal_4"), TEX / "item" / "essence_crystal_4.png"):
    texture_create += 1

# --- attrezzi e armature ---
NOMI_TIPO_ALCHIMIA = {**NOMI_STRUMENTI, "hammer": ("Hammer", "Maglio")}
for m, n_en, n_it, colore, manico in [("nocturnite", "Nocturnite", "Nocturnite", NOCTURNITE_C, (150, 220, 240)),
                                      ("ardorite", "Ardorite", "Ardorite", ARDORITE_C, NOCTURNITE_C)]:
    colori = {**palette(colore), "W": manico, "w": mix(manico, (0, 0, 0), 0.3)}
    for tipo in ["sword", "pickaxe", "axe", "shovel", "hoe", "hammer"]:
        id_ = f"{m}_{tipo}"
        t_en, t_it = NOMI_TIPO_ALCHIMIA[tipo]
        oggetto(id_, f"{n_en} {t_en}", f"{t_it} di {n_it}", "handheld")
        disegno = MAGLIO if tipo == "hammer" else STRUMENTI[tipo]
        if salva_texture(disegna(disegno, colori, id_), TEX / "item" / f"{id_}.png"):
            texture_create += 1
        tag(tag_oggetti, "minecraft:pickaxes" if tipo in ("pickaxe", "hammer") else f"minecraft:{tipo}s", ft(id_))
        # manico: diamanti per la Nocturnite, Nocturnite per l'Ardorite
        manico_ing = "#c:gems/diamond" if m == "nocturnite" else ft("nocturnite")
        schema = ["MMM", "MSM", " S "] if tipo == "hammer" else SCHEMI_STRUMENTI[tipo]
        r_con_forma(id_, schema, {"M": ft(m), "S": manico_ing}, ft(id_))
    for tipo, disegno in ARMATURE.items():
        id_ = f"{m}_{tipo}"
        t_en, t_it = NOMI_ARMATURE[tipo]
        oggetto(id_, f"{n_en} {t_en}", f"{t_it} di {n_it}")
        if salva_texture(disegna(disegno, palette(colore), id_), TEX / "item" / f"{id_}.png"):
            texture_create += 1
        tag(tag_oggetti, f"minecraft:{TAG_ARMATURE[tipo]}", ft(id_))
        r_con_forma(id_, SCHEMI_ARMATURE[tipo], {"M": ft(m)}, ft(id_))
    for strato in (1, 2):
        if salva_texture(texture_armatura_indossata(colore, strato, f"{m}_layer_{strato}"),
                         TEX / "models" / "armor" / f"{m}_layer_{strato}.png"):
            texture_create += 1

# --- oggetti alchemici ---
OGGETTI_ALCHIMIA = [
    # id, inglese, italiano, immagine
    ("void_catalyst", "Void Catalyst", "Catalizzatore del Vuoto", sprite_catalizzatore("void_catalyst")),
    ("kinetic_lens", "Kinetic Lens", "Lente Cinetica", sprite_lente("kinetic_lens")),
    ("dimensional_satchel", "Dimensional Satchel", "Bisaccia Dimensionale",
     disegna(BISACCIA, {**palette((120, 80, 60)), **COLORI_ALCHIMIA}, "dimensional_satchel")),
    ("attractor_ring", "Attractor Ring", "Anello Attrattore", sprite_anello((90, 90, 100), (60, 200, 120), "attractor_ring")),
    ("windrider_ring", "Windrider Ring", "Anello dei Venti", sprite_anello((220, 220, 230), (150, 230, 255), "windrider_ring")),
    ("ember_ring", "Ember Ring", "Anello Ardente", sprite_anello((90, 70, 60), (255, 110, 40), "ember_ring")),
    ("frost_ring", "Frost Ring", "Anello Glaciale", sprite_anello((180, 200, 220), (120, 180, 255), "frost_ring")),
    ("verdant_ring", "Verdant Ring", "Anello Verdeggiante", sprite_anello((150, 110, 60), (90, 210, 70), "verdant_ring")),
    ("arcane_hourglass", "Arcane Hourglass", "Clessidra Arcana",
     disegna(CLESSIDRA, {**palette((170, 220, 240)), **COLORI_ALCHIMIA}, "arcane_hourglass")),
    ("vitality_amulet", "Vitality Amulet", "Amuleto Vitale", sprite_amuleto((230, 60, 90), "vitality_amulet")),
    ("memory_gem", "Memory Gem", "Gemma della Memoria", disegna(GEMMA, palette((90, 220, 90)), "memory_gem")),
    ("tide_amulet", "Tide Amulet", "Amuleto delle Maree", sprite_amuleto((50, 120, 230), "tide_amulet")),
    ("magma_amulet", "Magma Amulet", "Amuleto del Magma", sprite_amuleto((240, 100, 20), "magma_amulet")),
    ("dowsing_rod", "Dowsing Rod", "Bacchetta Rabdomante",
     disegna(RABDOMANTE, {**palette((130, 90, 55)), **COLORI_ALCHIMIA}, "dowsing_rod")),
    ("builder_wand", "Builder's Wand", "Bacchetta del Costruttore",
     disegna(BACCHETTA, {**palette((230, 190, 80)), **COLORI_ALCHIMIA}, "builder_wand")),
    ("repair_talisman", "Repair Talisman", "Talismano Riparatore",
     disegna(TALISMANO, {**palette((110, 110, 130)), **COLORI_ALCHIMIA}, "repair_talisman")),
    ("synthesis_tablet", "Synthesis Tablet", "Tavoletta di Sintesi",
     disegna(TAVOLETTA, {**palette((70, 50, 90)), **COLORI_ALCHIMIA}, "synthesis_tablet")),
    ("omniscience_tome", "Tome of Omniscience", "Tomo dell'Onniscienza",
     disegna(TOMO, {**palette((110, 40, 120)), **COLORI_ALCHIMIA, "G": (255, 215, 90), "W": (240, 230, 200)},
             "omniscience_tome")),
]
for id_, n_en, n_it, img in OGGETTI_ALCHIMIA:
    oggetto(id_, n_en, n_it, "handheld" if id_ in ("dowsing_rod", "builder_wand") else "generated")
    if salva_texture(img, TEX / "item" / f"{id_}.png"):
        texture_create += 1

# --- blocchi: Carica Stellare e Lanterna Repulsiva ---
blocco("star_charge", "Star Charge", "Carica Stellare")
blocco("repulsion_lantern", "Repulsion Lantern", "Lanterna Repulsiva")


def texture_carica(seme, lato):
    rng = random.Random(seme)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            if lato == "side":
                c = (60, 40, 90) if 4 <= y <= 11 else (200, 160, 60)
                if 4 <= y <= 11 and (x + y) % 5 == 0:
                    c = (255, 230, 120)
            else:
                d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
                c = (255, 240, 170) if d < 2.5 else (200, 160, 60) if d < 6 else (60, 40, 90)
            img.putpixel((x, y), rumore(c, rng, 0.05) + (255,))
    return img


def texture_lanterna(seme):
    rng = random.Random(seme)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            bordo = x in (0, 1, 14, 15) or y in (0, 1, 14, 15)
            c = (60, 50, 70) if bordo else (200, 140, 255) if (x + y) % 3 else (240, 210, 255)
            img.putpixel((x, y), rumore(c, rng, 0.04) + (255,))
    return img


for img, nome in [(texture_carica("star_charge_top", "top"), "block/star_charge_top"),
                  (texture_carica("star_charge_side", "side"), "block/star_charge_side"),
                  (texture_lanterna("repulsion_lantern"), "block/repulsion_lantern")]:
    if salva_texture(img, TEX / f"{nome}.png"):
        texture_create += 1


def modelli_carica():
    A_ = f"assets/{MODID}"
    scrivi_json(f"{A_}/models/block/star_charge.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": f"{MODID}:block/star_charge_top", "side": f"{MODID}:block/star_charge_side",
        "bottom": f"{MODID}:block/star_charge_top"}})
    scrivi_json(f"{A_}/blockstates/star_charge.json", {"variants": {
        "lit=false": {"model": f"{MODID}:block/star_charge"}, "lit=true": {"model": f"{MODID}:block/star_charge"}}})
    scrivi_json(f"{A_}/models/item/star_charge.json", {"parent": f"{MODID}:block/star_charge"})


def modelli_lanterna():
    A_ = f"assets/{MODID}"
    t = f"{MODID}:block/repulsion_lantern"
    facce = lambda uv: {f: {"texture": "#all", "uv": uv} for f in ["north", "south", "east", "west", "up", "down"]}
    scrivi_json(f"{A_}/models/block/repulsion_lantern.json", {
        "parent": "minecraft:block/block", "textures": {"all": t, "particle": t},
        "elements": [
            {"from": [4, 0, 4], "to": [12, 9, 12], "faces": facce([0, 0, 16, 16])},
            {"from": [6, 9, 6], "to": [10, 11, 10], "faces": facce([0, 0, 4, 4])},
        ]})
    scrivi_json(f"{A_}/blockstates/repulsion_lantern.json",
                {"variants": {"": {"model": f"{MODID}:block/repulsion_lantern"}}})
    scrivi_json(f"{A_}/models/item/repulsion_lantern.json", {"parent": f"{MODID}:block/repulsion_lantern"})


modelli_speciali["star_charge"] = modelli_carica
modelli_speciali["repulsion_lantern"] = modelli_lanterna
tag(tag_blocchi, "minecraft:mineable/pickaxe", ft("repulsion_lantern"))

# --- ricette ---
r_senza_forma("arcane_ember", [PIETRA] + ["minecraft:coal"] * 4, ft("arcane_ember"))
r_senza_forma("stellar_ember", [PIETRA] + [ft("arcane_ember")] * 4, ft("stellar_ember"))
r_senza_forma("eternal_ember", [PIETRA] + [ft("stellar_ember")] * 4, ft("eternal_ember"))
r_senza_forma("arcane_ember_to_coal", [PIETRA, ft("arcane_ember")], "minecraft:coal", 4)
r_senza_forma("stellar_ember_to_arcane", [PIETRA, ft("stellar_ember")], ft("arcane_ember"), 4)
r_senza_forma("eternal_ember_to_stellar", [PIETRA, ft("eternal_ember")], ft("stellar_ember"), 4)
r_con_forma("nocturnite", ["EEE", "EDE", "EEE"], {"E": ft("eternal_ember"), "D": "minecraft:diamond_block"},
            ft("nocturnite"))
r_con_forma("ardorite", ["NEN", "ENE", "NEN"], {"N": ft("nocturnite"), "E": ft("eternal_ember")}, ft("ardorite"))
r_con_forma("essence_band", ["III", "IVI", "III"], {"I": "#c:ingots/iron", "V": "#c:gems/voltite"}, ft("essence_band"))
r_con_forma("essence_crystal_4", ["CCC", "CAC", "CCC"], {"C": ft("essence_crystal_3"), "A": ft("ardorite")},
            ft("essence_crystal_4"))
B = ft("essence_band")
S_E = ft("stellar_ember")
A_E = ft("arcane_ember")
NOC = ft("nocturnite")
r_con_forma("void_catalyst", ["TST", "SNS", "TST"], {"T": "minecraft:tnt", "S": S_E, "N": NOC}, ft("void_catalyst"))
r_con_forma("kinetic_lens", ["GSG", "SCS", "GSG"], {"G": "#c:glass_panes", "S": S_E, "C": ft("void_catalyst")},
            ft("kinetic_lens"))
r_con_forma("star_charge", ["SAS", "ATA", "SAS"], {"S": "minecraft:sand", "A": A_E, "T": "minecraft:tnt"},
            ft("star_charge"), 2)
r_con_forma("dimensional_satchel", ["LXL", "WEW", "LLL"], {"L": "minecraft:leather", "X": ft("essence_crystal"),
                                                           "W": "#minecraft:wool", "E": "minecraft:ender_chest"},
            ft("dimensional_satchel"))
for id_, extra, centro in [("attractor_ring", "minecraft:ender_pearl", S_E), ("windrider_ring", "minecraft:feather", NOC),
                           ("ember_ring", "minecraft:blaze_rod", S_E), ("frost_ring", "minecraft:packed_ice", S_E),
                           ("verdant_ring", "#minecraft:saplings", S_E)]:
    r_con_forma(id_, ["XSX", "SBS", "XSX"], {"X": extra, "S": centro, "B": B}, ft(id_))
r_con_forma("arcane_hourglass", ["GNG", "SCS", "GNG"], {"G": "#c:glass_blocks", "N": NOC, "S": S_E, "C": "minecraft:clock"},
            ft("arcane_hourglass"))
r_con_forma("vitality_amulet", ["SAS", "ABA", "SAS"], {"S": S_E, "A": "minecraft:golden_apple", "B": B},
            ft("vitality_amulet"))
r_con_forma("memory_gem", ["SLS", "LBL", "SLS"], {"S": S_E, "L": "minecraft:lapis_block", "B": "minecraft:book"},
            ft("memory_gem"))
r_con_forma("tide_amulet", ["SPS", "PBP", "SPS"], {"S": S_E, "P": "minecraft:prismarine_shard", "B": B}, ft("tide_amulet"))
r_con_forma("magma_amulet", ["SPS", "PBP", "SPS"], {"S": S_E, "P": "minecraft:magma_cream", "B": B}, ft("magma_amulet"))
r_con_forma("dowsing_rod", ["V V", " S ", " S "], {"V": "#c:gems/voltite", "S": "#c:rods/wooden"}, ft("dowsing_rod"))
r_con_forma("builder_wand", ["  N", " S ", "S  "], {"N": NOC, "S": "#c:rods/wooden"}, ft("builder_wand"))
r_con_forma("repair_talisman", ["AOA", "OBO", "AOA"], {"A": A_E, "O": "minecraft:obsidian", "B": B}, ft("repair_talisman"))
r_con_forma("synthesis_tablet", ["OSO", "STS", "OSO"], {"O": "minecraft:obsidian", "S": S_E, "T": ft("synthesis_table")},
            ft("synthesis_tablet"))
r_con_forma("omniscience_tome", ["NAN", "ABA", "NAN"], {"N": "minecraft:nether_star", "A": ft("ardorite"),
                                                       "B": "minecraft:book"}, ft("omniscience_tome"))
r_con_forma("repulsion_lantern", ["GAG", "ALA", "GAG"], {"G": "#c:dusts/glowstone", "A": A_E, "L": "minecraft:lantern"},
            ft("repulsion_lantern"))

# --- traduzioni ---
SPIEGAZIONI = {
    # id: (inglese, italiano)
    "void_catalyst": ("Right-click a block: breaks a 3x3x3 area (Shift: 5x5x5)\nAll drops go to your inventory · 4 Essence per block",
                      "Click destro su un blocco: rompe un'area 3x3x3 (Shift: 5x5x5)\nTutto finisce nel tuo inventario · 4 Essenza a blocco"),
    "kinetic_lens": ("Right-click: digs a sphere where you look (up to 64 blocks)\n8 Essence per block",
                     "Click destro: scava una sfera dove guardi (fino a 64 blocchi)\n8 Essenza a blocco"),
    "dimensional_satchel": ("Right-click: opens your personal 54-slot storage\nEvery satchel opens the SAME storage, kept after death",
                            "Click destro: apre il tuo deposito personale da 54 slot\nOgni bisaccia apre LO STESSO deposito, resta anche dopo la morte"),
    "attractor_ring": ("Right-click: on/off · Pulls items and XP within 8 blocks",
                       "Click destro: accendi/spegni · Attira oggetti ed esperienza entro 8 blocchi"),
    "windrider_ring": ("Right-click: on/off · Lets you fly like in creative\n2 Essence per second while flying",
                       "Click destro: accendi/spegni · Puoi volare come in creativa\n2 Essenza al secondo mentre voli"),
    "ember_ring": ("In your inventory: immune to fire and lava\nRight-click: throws a fireball (16 Essence)",
                   "Nell'inventario: immune a fuoco e lava\nClick destro: lancia una palla di fuoco (16 Essenza)"),
    "frost_ring": ("In your inventory: water freezes under your feet\nRight-click: freezes water and lava around you (32 Essence)",
                   "Nell'inventario: l'acqua ghiaccia sotto i tuoi piedi\nClick destro: congela acqua e lava intorno a te (32 Essenza)"),
    "verdant_ring": ("Right-click: on/off · Makes crops and saplings grow faster around you",
                     "Click destro: accendi/spegni · Fa crescere più in fretta coltivazioni e alberelli intorno a te"),
    "arcane_hourglass": ("Right-click: on/off · Machines, furnaces and plants within 4 blocks work 4x faster\n4 Essence per second",
                         "Click destro: accendi/spegni · Macchine, fornaci e piante entro 4 blocchi vanno 4 volte più veloci\n4 Essenza al secondo"),
    "vitality_amulet": ("Right-click: on/off · Restores hunger (16 Essence) and health (32 Essence) over time",
                        "Click destro: accendi/spegni · Ti sfama (16 Essenza) e ti cura (32 Essenza) piano piano"),
    "memory_gem": ("Right-click: stores ALL your experience · Shift + right-click: gives it back",
                   "Click destro: conserva TUTTA la tua esperienza · Shift + click destro: te la restituisce"),
    "tide_amulet": ("Right-click: places water (free, infinite)\nRight-click on a chemical bath: fills its tank",
                    "Click destro: piazza acqua (gratis, infinita)\nClick destro su un bagno chimico: riempie il serbatoio"),
    "magma_amulet": ("Right-click: places lava (64 Essence)", "Click destro: piazza lava (64 Essenza)"),
    "dowsing_rod": ("Right-click a block: shows how much Essence is in the 3x3 area behind it (8 deep)",
                    "Click destro su un blocco: mostra quanta Essenza c'è nell'area 3x3 dietro (profonda 8)"),
    "builder_wand": ("Hold a block in your off hand. Right-click: places a 3x3 layer (Shift: 1 block)\nBlocks are paid in Essence, not taken from your inventory",
                     "Tieni un blocco nella mano sinistra. Click destro: piazza uno strato 3x3 (Shift: 1 blocco)\nI blocchi si pagano in Essenza, non vengono presi dall'inventario"),
    "repair_talisman": ("In your inventory: slowly repairs all your tools and armor",
                        "Nell'inventario: ripara piano piano tutti i tuoi attrezzi e armature"),
    "synthesis_tablet": ("Right-click: opens the Synthesis Table anywhere", "Click destro: apre il Tavolo di Sintesi ovunque"),
    "omniscience_tome": ("Right-click: you learn EVERY item with an Essence value (consumed)",
                         "Click destro: impari TUTTI gli oggetti con un valore in Essenza (si consuma)"),
    "star_charge": ("Light it with redstone or flint and steel: after 4 s it breaks everything within 6 blocks\nNothing is destroyed: all drops fall in the middle · does not break chests and machines",
                    "Si accende con la redstone o l'acciarino: dopo 4 s rompe tutto entro 6 blocchi\nNon distrugge niente: gli oggetti cadono al centro · non rompe bauli e macchine"),
    "repulsion_lantern": ("Keeps monsters and their projectiles away (8 blocks)",
                          "Tiene lontani i mostri e i loro proiettili (8 blocchi)"),
}
for tipo in ["pickaxe", "axe", "shovel", "hoe"]:
    for m in ["nocturnite", "ardorite"]:
        SPIEGAZIONI[f"{m}_{tipo}"] = ("Never breaks · Shift + right-click in the air: 3x3 area mode\n(hold Shift while digging: one block only)",
                                      "Non si rompe mai · Shift + click destro in aria: modalità area 3x3\n(tieni Shift mentre scavi: un blocco solo)")
for m in ["nocturnite", "ardorite"]:
    SPIEGAZIONI[f"{m}_sword"] = ("Never breaks", "Non si rompe mai")
SPIEGAZIONI["nocturnite_hammer"] = ("Never breaks · always digs 3x3 (stone, dirt, sand...)\nHold Shift: one block only",
                                    "Non si rompe mai · scava sempre 3x3 (pietra, terra, sabbia...)\nTieni Shift: un blocco solo")
SPIEGAZIONI["ardorite_hammer"] = ("Never breaks · always digs 5x5 (stone, dirt, sand...)\nHold Shift: one block only",
                                  "Non si rompe mai · scava sempre 5x5 (pietra, terra, sabbia...)\nTieni Shift: un blocco solo")
for id_, (en, it) in SPIEGAZIONI.items():
    lingua_en[f"tooltip.{MODID}.{id_}"] = en
    lingua_it[f"tooltip.{MODID}.{id_}"] = it
lingua_en.update({
    "tooltip.futuretek.ember": "Fuel: burns %s times longer than... (smelts that many items)",
    "tooltip.futuretek.active": "ON", "tooltip.futuretek.inactive": "OFF",
    "tooltip.futuretek.area_on": "Area mode: ON", "tooltip.futuretek.area_off": "Area mode: OFF",
    "tooltip.futuretek.nocturnite_armor": "Never breaks · Full set: no fall damage, fire immunity",
    "tooltip.futuretek.ardorite_armor": "Never breaks · Full set: no fall damage, fire immunity, -50% damage, water breathing",
    "tooltip.futuretek.stored_xp": "Stored experience: %s points",
    "tooltip.futuretek.shifting_stone_craft": "Shift + right-click in the air: portable crafting table",
    "tooltip.futuretek.known_count": "Items you have learned",
    "message.futuretek.toggle_on": "%s: ON", "message.futuretek.toggle_off": "%s: OFF",
    "message.futuretek.area_on": "Area mining: ON", "message.futuretek.area_off": "Area mining: OFF",
    "message.futuretek.not_enough_essence": "Not enough Essence (needs %s)",
    "message.futuretek.dowsing": "Essence in the area: %s · most valuable: %s (%s)",
    "message.futuretek.dowsing_none": "Nothing valuable here",
    "message.futuretek.builder_no_block": "Hold a block in your off hand",
    "message.futuretek.learned_all": "You learned %s new items!",
    "message.futuretek.essence_value_cmd": "%s: %s Essence each, %s for this stack",
    "message.futuretek.essence_missing_cmd": "Items without an Essence value: %s (with value: %s). List written to %s",
})
lingua_it.update({
    "tooltip.futuretek.ember": "Combustibile: cuoce %s oggetti",
    "tooltip.futuretek.active": "ACCESO", "tooltip.futuretek.inactive": "SPENTO",
    "tooltip.futuretek.area_on": "Modalità area: ATTIVA", "tooltip.futuretek.area_off": "Modalità area: spenta",
    "tooltip.futuretek.nocturnite_armor": "Non si rompe mai · Set completo: niente danni da caduta, immune al fuoco",
    "tooltip.futuretek.ardorite_armor": "Non si rompe mai · Set completo: niente danni da caduta, immune al fuoco, -50% danni, respiri sott'acqua",
    "tooltip.futuretek.stored_xp": "Esperienza conservata: %s punti",
    "tooltip.futuretek.shifting_stone_craft": "Shift + click destro in aria: banco da lavoro portatile",
    "tooltip.futuretek.known_count": "Oggetti che hai imparato",
    "message.futuretek.toggle_on": "%s: ACCESO", "message.futuretek.toggle_off": "%s: SPENTO",
    "message.futuretek.area_on": "Scavo ad area: ATTIVO", "message.futuretek.area_off": "Scavo ad area: spento",
    "message.futuretek.not_enough_essence": "Essenza insufficiente (ne servono %s)",
    "message.futuretek.dowsing": "Essenza nell'area: %s · il più prezioso: %s (%s)",
    "message.futuretek.dowsing_none": "Qui non c'è niente di prezioso",
    "message.futuretek.builder_no_block": "Tieni un blocco nella mano sinistra",
    "message.futuretek.learned_all": "Hai imparato %s nuovi oggetti!",
    "message.futuretek.essence_value_cmd": "%s: %s Essenza l'uno, %s per questa pila",
    "message.futuretek.essence_missing_cmd": "Oggetti senza valore dell'Essenza: %s (con valore: %s). Elenco scritto in %s",
})
lingua_en["tooltip.futuretek.ember"] = "Fuel: smelts %s items"
# Oggetti che si sciolgono ma NON si creano con l'Essenza (le macchine li moltiplicano: sarebbe Essenza infinita)
for voce in ["#c:raw_materials", "#c:ores", "minecraft:raw_iron_block", "minecraft:raw_gold_block",
             "minecraft:raw_copper_block", "futuretek:bee_queen", "futuretek:bee_drone"] \
        + [ft(f"raw_{m}_block") for m in ["tin", "lead", "silver", "uranium"]] \
        + [ft(f"{m}_dust") for m in ["tin", "lead", "silver", "uranium", "bronze", "steel", "iron", "gold", "copper",
                                     "coal", "diamond", "obsidian"]] \
        + [ft(f"purified_crushed_{m}") for m in ["tin", "lead", "silver", "uranium", "iron", "gold", "copper"]] \
        + [ft(f"{c}_comb") for c in ["mineral", "metallic", "frozen", "ember", "abyssal", "voltaic", "stellar"]] \
        + [ft(f"wild_hive_{h}") for h in ["common", "forest", "meadow"]]:
    tag(tag_oggetti, "futuretek:essence_not_creatable", voce)
lingua_en["tooltip.futuretek.essence_not_creatable"] = "Can be dissolved, but not created with Essence"
lingua_it["tooltip.futuretek.essence_not_creatable"] = "Si può sciogliere, ma non creare con l'Essenza"
# JEI (ricette delle macchine)
lingua_en.update({"jei.futuretek.chance": "Chance: %s%%", "jei.futuretek.info_water": "%s · %s mB water"})
lingua_it.update({"jei.futuretek.chance": "Probabilità: %s%%", "jei.futuretek.info_water": "%s · %s mB d'acqua"})


# ===========================================================================
# COMODITA' (richieste dell'utente, 2 nov 2026): fornace di ferro, rilevatori di minerali, sacca del minatore
# ===========================================================================
sys.path.insert(0, str(ROOT / "tools"))
import grafica as _g  # noqa: E402  (le texture di questi oggetti sono disegnate con la grafica v2)

blocco("iron_furnace", "Iron Furnace", "Fornace di ferro")
for img, nome in [(_g.fornace_ferro("front"), "block/iron_furnace_front"),
                  (_g.fornace_ferro("front", True), "block/iron_furnace_front_on"),
                  (_g.fornace_ferro("side"), "block/iron_furnace_side"), (_g.fornace_ferro("top"), "block/iron_furnace_top")]:
    if salva_texture(img, TEX / f"{nome}.png"):
        texture_create += 1
modelli_speciali["iron_furnace"] = modelli_macchina("iron_furnace", "iron_furnace_front", "iron_furnace_front_on",
                                                    top="iron_furnace_top", side="iron_furnace_side",
                                                    bottom="iron_furnace_side")
tag(tag_blocchi, "minecraft:mineable/pickaxe", ft("iron_furnace"))
r_con_forma("iron_furnace", ["III", "I I", "III"], {"I": "#c:ingots/iron"}, ft("iron_furnace"))

NOMI_RILEVAMENTO = {1: ("I", "verde"), 2: ("II", "blu"), 3: ("III", "ciano")}
for liv, (num, col) in NOMI_RILEVAMENTO.items():
    oggetto(f"scanning_dust_{liv}", f"Scanning Dust {num}", f"Polvere di rilevamento {num}")
    oggetto(f"ore_scanner_{liv}", f"Ore Scanner {num}", f"Rilevatore di minerali {num}", "handheld")
    colore = _g.COLORI_RILEVAMENTO[liv]
    if salva_texture(_g.polvere(colore, f"scan{liv}"), TEX / "item" / f"scanning_dust_{liv}.png"):
        texture_create += 1
    if salva_texture(_g.rilevatore(colore), TEX / "item" / f"ore_scanner_{liv}.png"):
        texture_create += 1
    r_con_forma(f"ore_scanner_{liv}", [" DD", " SD", "S  "], {"D": ft(f"scanning_dust_{liv}"), "S": "#c:rods/wooden"},
                ft(f"ore_scanner_{liv}"))
r_senza_forma("scanning_dust_1", ["#minecraft:coals", "#c:stones"], ft("scanning_dust_1"), 2)
r_senza_forma("scanning_dust_2", ["#c:dusts/redstone", "#c:ingots/iron"], ft("scanning_dust_2"), 2)
r_senza_forma("scanning_dust_3", ["#c:ingots/iron", "#c:gems/diamond"], ft("scanning_dust_3"), 2)

oggetto("miner_bag", "Miner's Bag", "Sacca del minatore")
if salva_texture(_g.sacca(), TEX / "item" / "miner_bag.png"):
    texture_create += 1
r_con_forma("miner_bag", ["W1W", "2C3", "WWW"], {"W": "#minecraft:wool", "C": "#c:chests/wooden",
                                               "1": ft("scanning_dust_1"), "2": ft("scanning_dust_2"),
                                               "3": ft("scanning_dust_3")}, ft("miner_bag"))
for voce in ["#c:ores", "#c:raw_materials", "#c:gems", "#c:dusts", "#minecraft:coals", "#c:nuggets",
             "minecraft:flint", "minecraft:clay_ball", "minecraft:amethyst_shard", "minecraft:redstone",
             "minecraft:lapis_lazuli", "minecraft:quartz", "minecraft:glowstone_dust", "minecraft:ancient_debris",
             ft("voltite_shard")]:
    tag(tag_oggetti, "futuretek:miner_bag_collects", voce)

lingua_en.update({
    "tooltip.futuretek.ore_scanner": "Right-click: finds ores in a %sx%s area around you, down to %s blocks deep",
    "tooltip.futuretek.miner_bag": "Right-click: open · Shift + right-click: auto-collect on/off",
    "tooltip.futuretek.miner_bag_slots": "Slots used: %s / %s",
    "tooltip.futuretek.bag_auto_on": "Auto-collect ores: ON", "tooltip.futuretek.bag_auto_off": "Auto-collect ores: OFF",
    "message.futuretek.bag_auto_on": "Miner's Bag: auto-collect ON", "message.futuretek.bag_auto_off": "Miner's Bag: auto-collect OFF",
    "message.futuretek.scanner_header": "Ores in %sx%s, %s blocks deep (nearest first):",
    "message.futuretek.scanner_none": "No ores here",
    "message.futuretek.catalyst_charge": "Void Catalyst: charge %s (%sx%sx%s)",
    "tooltip.futuretek.catalyst_charge": "Charge %s: breaks %sx%sx%s",
})
lingua_it.update({
    "tooltip.futuretek.ore_scanner": "Click destro: trova i minerali in un'area %sx%s attorno a te, fino a %s blocchi di profondità",
    "tooltip.futuretek.miner_bag": "Click destro: apri · Shift + click destro: raccolta automatica sì/no",
    "tooltip.futuretek.miner_bag_slots": "Slot occupati: %s / %s",
    "tooltip.futuretek.bag_auto_on": "Raccolta automatica dei minerali: ATTIVA",
    "tooltip.futuretek.bag_auto_off": "Raccolta automatica dei minerali: spenta",
    "message.futuretek.bag_auto_on": "Sacca del minatore: raccolta automatica ATTIVA",
    "message.futuretek.bag_auto_off": "Sacca del minatore: raccolta automatica spenta",
    "message.futuretek.scanner_header": "Minerali in %sx%s, fino a %s blocchi sotto (dal più vicino):",
    "message.futuretek.scanner_none": "Nessun minerale qui",
    "message.futuretek.catalyst_charge": "Catalizzatore del Vuoto: carica %s (%sx%sx%s)",
    "tooltip.futuretek.catalyst_charge": "Carica %s: rompe %sx%sx%s",
})
lingua_en["tooltip.futuretek.void_catalyst"] = ("Right-click in the air: charge 1→4 (bigger area)\n"
                                                "Right-click a block: breaks the area · Shift: only the front layer\n"
                                                "All drops go to your inventory · 4 Essence per block")
lingua_it["tooltip.futuretek.void_catalyst"] = ("Click destro in aria: carica 1→4 (area più grande)\n"
                                                "Click destro su un blocco: rompe l'area · Shift: solo lo strato davanti\n"
                                                "Tutto finisce nel tuo inventario · 4 Essenza a blocco")


# ===========================================================================
# FASE 10 (richieste dell'utente, 2 nov 2026): bauli, generatori, macchine avanzate, carbonaia,
# nucleo logistico, talpa mineraria, escavatore con segnalini/telaio/testa
# (le texture sono disegnate da tools/grafica.py)
# ===========================================================================
CIRC_AV = ft("advanced_circuit")
oggetto("advanced_circuit", "Advanced Circuit", "Circuito avanzato")
r_con_forma("advanced_circuit", ["RGR", "VCV", "RGR"], {"R": "#c:dusts/redstone", "G": "#c:ingots/gold",
                                                     "V": "#c:gems/voltite", "C": C}, CIRC_AV)

# macchine avanzate (stesso "cervello" delle macchine di lavorazione)
for id_, n_en, n_it, base in [("advanced_crusher", "Advanced Crusher", "Frantumatore avanzato", "crusher"),
                              ("induction_furnace", "Induction Furnace", "Forno a induzione", "electric_furnace")]:
    blocco(id_, n_en, n_it)
    modelli_speciali[id_] = modelli_macchina(id_, f"{id_}_front", f"{id_}_front_on")
    tag(tag_blocchi, "minecraft:mineable/pickaxe", ft(id_))
    r_con_forma(id_, ["PAP", "AXA", "PAP"], {"P": "#c:plates/steel", "A": CIRC_AV, "X": ft(base)}, ft(id_))

# generatori
GEN10 = {
    "advanced_generator": ("Advanced Generator", "Generatore avanzato"),
    "solar_2": ("Solar Panel II", "Pannello solare II"),
    "solar_3": ("Solar Panel III", "Pannello solare III"),
    "water_mill": ("Water Mill", "Mulino ad acqua"),
    "wind_mill": ("Wind Mill", "Mulino a vento"),
}
for id_, (n_en, n_it) in GEN10.items():
    blocco(id_, n_en, n_it)
    tag(tag_blocchi, "minecraft:mineable/pickaxe", ft(id_))
modelli_speciali["advanced_generator"] = modelli_macchina("advanced_generator", "advanced_generator_front",
                                                          "advanced_generator_front_on")
for liv in (2, 3):
    modelli_speciali[f"solar_{liv}"] = modelli_macchina(f"solar_{liv}", "machine_side", "machine_side",
                                                        top=f"solar_{liv}_top")
modelli_speciali["water_mill"] = modelli_macchina("water_mill", "water_mill_front", "water_mill_front_on")
modelli_speciali["wind_mill"] = modelli_macchina("wind_mill", "wind_mill_front", "wind_mill_front_on")
r_con_forma("advanced_generator", ["PAP", "GXG", "PAP"], {"P": "#c:plates/steel", "A": CIRC_AV, "G": "#c:ingots/gold",
                                                         "X": ft("combustion_generator")}, ft("advanced_generator"))
r_con_forma("solar_2", ["SGS", "GAG", "SGS"], {"S": ft("solar_panel"), "G": "#c:glass_blocks", "A": CIRC_AV},
            ft("solar_2"))
r_con_forma("solar_3", ["SVS", "VAV", "SVS"], {"S": ft("solar_2"), "V": ft("voltite_block"), "A": CIRC_AV}, ft("solar_3"))
r_con_forma("water_mill", ["PWP", "WMW", "PWP"], {"P": "#minecraft:planks", "W": "#c:rods/wooden", "M": M},
            ft("water_mill"))
r_con_forma("wind_mill", ["LSL", "SMS", "LSL"], {"L": "#minecraft:wool", "S": "#c:rods/wooden", "M": M}, ft("wind_mill"))

# carbonaia
blocco("charcoal_kiln", "Charcoal Kiln", "Carbonaia")
tag(tag_blocchi, "minecraft:mineable/pickaxe", ft("charcoal_kiln"))


def modelli_carbonaia():
    A_ = f"assets/{MODID}"
    for suff, lato in [("", "charcoal_kiln_side"), ("_on", "charcoal_kiln_side_on")]:
        scrivi_json(f"{A_}/models/block/charcoal_kiln{suff}.json", {"parent": "minecraft:block/cube_bottom_top",
            "textures": {"top": f"{MODID}:block/charcoal_kiln_top", "bottom": f"{MODID}:block/charcoal_kiln_top",
                         "side": f"{MODID}:block/{lato}"}})
    scrivi_json(f"{A_}/blockstates/charcoal_kiln.json", {"variants": {
        "lit=false": {"model": f"{MODID}:block/charcoal_kiln"}, "lit=true": {"model": f"{MODID}:block/charcoal_kiln_on"}}})
    scrivi_json(f"{A_}/models/item/charcoal_kiln.json", {"parent": f"{MODID}:block/charcoal_kiln"})


modelli_speciali["charcoal_kiln"] = modelli_carbonaia
r_con_forma("charcoal_kiln", ["BBB", "BFB", "BBB"], {"B": "minecraft:bricks", "F": "minecraft:furnace"}, ft("charcoal_kiln"))

# bauli
NOMI_BAULI = {"copper": ("Copper Chest", "Baule di rame"), "iron": ("Iron Chest", "Baule di ferro"),
              "gold": ("Gold Chest", "Baule d'oro"), "diamond": ("Diamond Chest", "Baule di diamante"),
              "obsidian": ("Obsidian Chest", "Baule di ossidiana")}


def modelli_baule(id_):
    def scrivi():
        A_ = f"assets/{MODID}"
        scrivi_json(f"{A_}/models/block/{id_}.json", {"parent": "minecraft:block/orientable", "textures": {
            "front": f"{MODID}:block/{id_}_front", "side": f"{MODID}:block/{id_}_side", "top": f"{MODID}:block/{id_}_top"}})
        rot = {"north": 0, "east": 90, "south": 180, "west": 270}
        scrivi_json(f"{A_}/blockstates/{id_}.json", {"variants": {
            f"facing={d}": ({"model": f"{MODID}:block/{id_}", "y": g} if g else {"model": f"{MODID}:block/{id_}"})
            for d, g in rot.items()}})
        scrivi_json(f"{A_}/models/item/{id_}.json", {"parent": f"{MODID}:block/{id_}"})
    return scrivi


for m, (n_en, n_it) in NOMI_BAULI.items():
    id_ = f"{m}_chest"
    blocco(id_, n_en, n_it)
    modelli_speciali[id_] = modelli_baule(id_)
    tag(tag_blocchi, "minecraft:mineable/pickaxe", ft(id_))
r_con_forma("copper_chest", ["III", "IXI", "III"], {"I": "#c:ingots/copper", "X": "#c:chests/wooden"}, ft("copper_chest"))
r_con_forma("iron_chest", ["III", "IXI", "III"], {"I": "#c:ingots/iron", "X": ft("copper_chest")}, ft("iron_chest"))
r_con_forma("gold_chest", ["III", "IXI", "III"], {"I": "#c:ingots/gold", "X": ft("iron_chest")}, ft("gold_chest"))
r_con_forma("diamond_chest", ["GGG", "DXD", "GGG"], {"G": "#c:glass_blocks", "D": "#c:gems/diamond",
                                                    "X": ft("gold_chest")}, ft("diamond_chest"))
r_con_forma("obsidian_chest", ["OOO", "OXO", "OOO"], {"O": "minecraft:obsidian", "X": ft("diamond_chest")},
            ft("obsidian_chest"))

# nucleo logistico e talpa mineraria
blocco("logistics_core", "Logistics Core", "Nucleo logistico")
modelli_speciali["logistics_core"] = modelli_macchina("logistics_core", "logistics_core_front", "logistics_core_front_on")
r_con_forma("logistics_core", ["PEP", "CMC", "PWP"], {"P": PIASTRA_FERRO, "E": "minecraft:ender_pearl", "C": C, "M": M,
                                                     "W": ft("item_conduit")}, ft("logistics_core"))
blocco("mining_mole", "Mining Mole", "Talpa mineraria")
modelli_speciali["mining_mole"] = modelli_macchina("mining_mole", "mining_mole_front", "mining_mole_front_on")
r_con_forma("mining_mole", ["PDP", "CMC", "PHP"], {"P": PIASTRA_FERRO, "D": "minecraft:diamond_pickaxe", "C": C, "M": M,
                                                  "H": "minecraft:hopper"}, ft("mining_mole"))
for id_ in ["logistics_core", "mining_mole"]:
    tag(tag_blocchi, "minecraft:mineable/pickaxe", ft(id_))

# escavatore: segnalino, telaio, testa
blocco("area_marker", "Area Marker", "Segnalino d'area")
blocco("quarry_frame", "Quarry Frame", "Telaio dell'escavatore")
blocco("quarry_drill", "Quarry Drill Head", "Testa di perforazione")


def modelli_parti_escavatore():
    A_ = f"assets/{MODID}"
    scrivi_json(f"{A_}/models/block/area_marker.json", {"parent": "minecraft:block/template_torch",
                                                         "textures": {"torch": f"{MODID}:block/area_marker"},
                                                         "render_type": "minecraft:cutout"})
    scrivi_json(f"{A_}/blockstates/area_marker.json", {"variants": {"": {"model": f"{MODID}:block/area_marker"}}})
    scrivi_json(f"{A_}/models/item/area_marker.json", {"parent": "minecraft:item/generated",
                                                        "textures": {"layer0": f"{MODID}:block/area_marker"}})
    modelli_cavo("quarry_frame", 4)()  # barre sottili che si collegano (come un cavo)
    t = f"{MODID}:block/quarry_drill"
    facce = {f: {"texture": "#d"} for f in ["north", "south", "east", "west", "up", "down"]}
    scrivi_json(f"{A_}/models/block/quarry_drill.json", {"parent": "minecraft:block/block",
        "textures": {"d": t, "particle": t},
        "elements": [{"from": [4, 2, 4], "to": [12, 16, 12], "faces": facce},
                     {"from": [6, 0, 6], "to": [10, 2, 10], "faces": facce}]})
    scrivi_json(f"{A_}/blockstates/quarry_drill.json", {"variants": {"": {"model": f"{MODID}:block/quarry_drill"}}})
    scrivi_json(f"{A_}/models/item/quarry_drill.json", {"parent": f"{MODID}:block/quarry_drill"})


modelli_speciali["area_marker"] = modelli_parti_escavatore
modelli_speciali["quarry_frame"] = lambda: None
loot_speciali["quarry_frame"] = {"type": "minecraft:block", "pools": []}
loot_speciali["quarry_drill"] = {"type": "minecraft:block", "pools": []}
modelli_speciali["quarry_drill"] = lambda: None
r_con_forma("area_marker", ["L", "S"], {"L": "minecraft:lapis_lazuli", "S": "#c:rods/wooden"}, ft("area_marker"), 2)

lingua_en.update({
    "gui.futuretek.deposit_all": "Deposit", "gui.futuretek.sort": "Sort",
    "gui.futuretek.deposit_all_tip": "Moves into the chest every item of your inventory that the chest already contains",
    "gui.futuretek.sort_tip": "Groups and sorts the contents",
    "gui.futuretek.kiln_hint": "Every %2$s seconds: up to %1$s logs become charcoal",
    "gui.futuretek.mole_start": "Start", "gui.futuretek.mole_stop": "Stop",
    "gui.futuretek.mole_info": "Depth %s · mined %s · fuel %s",
    "gui.futuretek.mole_fuel": "Fuel (coal, charcoal, logs...)",
    "gui.futuretek.mole_state.idle": "Ready: place it next to a chest and press Start",
    "gui.futuretek.mole_state.mining": "Mining...", "gui.futuretek.mole_state.returning": "Going home",
    "gui.futuretek.mole_state.at_home": "At home: unloading / waiting for fuel",
    "gui.futuretek.mole_state.resuming": "Going back to work", "gui.futuretek.mole_state.done": "Finished!",
    "gui.futuretek.mole_state.no_chest": "No chest next to it!", "gui.futuretek.mole_state.stuck": "Stuck: something is in the way",
    "gui.futuretek.stock": "Keep: %s", "gui.futuretek.stock_none": "Keep: off",
    "gui.futuretek.stock_off": "Turn off the automatic stock",
    "message.futuretek.generator_rate": "Producing %s TU/t · stored %s / %s TU",
    "message.futuretek.core_unpowered": "Logistics Core: NO ENERGY (connect a cable)",
    "message.futuretek.core_no_pipes": "Logistics Core: connect it to a logistic pipe",
    "message.futuretek.core_no_chest": "Logistics Core: place a chest touching the core (it becomes the main chest)",
    "message.futuretek.core_ok": "Logistics Core active: %s connected chests · energy %s TU",
})
lingua_it.update({
    "gui.futuretek.deposit_all": "Deposita", "gui.futuretek.sort": "Ordina",
    "gui.futuretek.deposit_all_tip": "Mette nel baule tutti gli oggetti del tuo inventario che il baule contiene già",
    "gui.futuretek.sort_tip": "Raggruppa e mette in ordine il contenuto",
    "gui.futuretek.kiln_hint": "Ogni %2$s secondi: fino a %1$s tronchi diventano carbonella",
    "gui.futuretek.mole_start": "Avvia", "gui.futuretek.mole_stop": "Ferma",
    "gui.futuretek.mole_info": "Profondità %s · scavati %s · carburante %s",
    "gui.futuretek.mole_fuel": "Combustibile (carbone, carbonella, tronchi...)",
    "gui.futuretek.mole_state.idle": "Pronta: mettila accanto a un baule e premi Avvia",
    "gui.futuretek.mole_state.mining": "Sto scavando...", "gui.futuretek.mole_state.returning": "Torno a casa",
    "gui.futuretek.mole_state.at_home": "A casa: scarico / aspetto combustibile",
    "gui.futuretek.mole_state.resuming": "Torno al lavoro", "gui.futuretek.mole_state.done": "Finito!",
    "gui.futuretek.mole_state.no_chest": "Nessun baule accanto!",
    "gui.futuretek.mole_state.stuck": "Bloccata: c'è qualcosa in mezzo",
    "gui.futuretek.stock": "Scorta: %s", "gui.futuretek.stock_none": "Scorta: spenta",
    "gui.futuretek.stock_off": "Spegni la scorta automatica",
    "message.futuretek.generator_rate": "Produce %s TU/t · accumulati %s / %s TU",
    "message.futuretek.core_unpowered": "Nucleo logistico: SENZA ENERGIA (collega un cavo)",
    "message.futuretek.core_no_pipes": "Nucleo logistico: collegalo a un tubo logistico",
    "message.futuretek.core_no_chest": "Nucleo logistico: metti un baule che tocca il nucleo (diventa il baule principale)",
    "message.futuretek.core_ok": "Nucleo logistico attivo: %s bauli collegati · energia %s TU",
})
SPIEGAZIONI_10 = {
    "area_marker": ("Place one next to the Quarry and one at the opposite corner (same height, up to 64 blocks)",
                    "Mettine uno accanto all'escavatore e uno nell'angolo opposto (stessa altezza, fino a 64 blocchi)"),
}
for id_, (en, it) in SPIEGAZIONI_10.items():
    lingua_en[f"tooltip.{MODID}.{id_}"] = en
    lingua_it[f"tooltip.{MODID}.{id_}"] = it


# --- Spiegazioni dei blocchi (compaiono passando il mouse sull'oggetto) ---
DESCRIZIONI_BLOCCHI = {
    "crusher": ("Raw ore → 2 dusts (ore doubling). Also: ingots → dust, cobblestone → gravel...",
                "Minerale grezzo → 2 polveri (raddoppio). Anche: lingotti → polvere, pietrisco → ghiaia..."),
    "advanced_crusher": ("Like the Crusher, but 4x faster (uses more energy)", "Come il Frantumatore, ma 4 volte più veloce (consuma di più)"),
    "electric_furnace": ("A furnace that runs on energy instead of coal", "Una fornace che va a energia invece che a carbone"),
    "induction_furnace": ("Electric furnace 4x faster", "Forno elettrico 4 volte più veloce"),
    "compressor": ("Compresses: 9 dust → plate, snowballs → ice...", "Comprime: polveri → piastre, palle di neve → ghiaccio..."),
    "extractor": ("Extracts: resin → 3 rubber...", "Estrae: resina → 3 gomma..."),
    "wire_press": ("Plate → wires (for cables)", "Piastra → fili (per i cavi)"),
    "chemical_bath": ("ORE TRIPLING, step 1: raw ore + water → 3 purified crushed ore\n"
                      "Fill it with a water bucket. Then use the Thermal Centrifuge",
                      "TRIPLICAZIONE DEI MINERALI, passo 1: minerale grezzo + acqua → 3 frantumati puri\n"
                      "Riempilo con un secchio d'acqua. Poi usa la Centrifuga termica"),
    "thermal_centrifuge": ("ORE TRIPLING, step 2: purified crushed ore → dust (+ bonus)\nThen smelt the dust: 1 raw = 3 ingots!",
                           "TRIPLICAZIONE DEI MINERALI, passo 2: frantumato puro → polvere (+ extra)\n"
                           "Poi cuoci la polvere: 1 grezzo = 3 lingotti!"),
    "combustion_generator": ("Burns fuel: 12 TU/t", "Brucia combustibile: 12 TU/t"),
    "advanced_generator": ("Burns fuel: 64 TU/t, fuel lasts longer", "Brucia combustibile: 64 TU/t, rende di più"),
    "geothermal_generator": ("Lava → 16 TU/t", "Lava → 16 TU/t"),
    "solar_panel": ("4 TU/t in daylight (open sky)", "4 TU/t di giorno (a cielo aperto)"),
    "solar_2": ("32 TU/t in daylight, a little at night", "32 TU/t di giorno, un po' di notte"),
    "solar_3": ("128 TU/t in daylight, a little at night", "128 TU/t di giorno, un po' di notte"),
    "water_mill": ("1 TU/t for each side touching water (max 6)", "1 TU/t per ogni lato che tocca l'acqua (max 6)"),
    "wind_mill": ("From y 64 up: the higher, the more energy (max 8, x2 in thunderstorms)\nNeeds free air around it",
                  "Da y 64 in su: più in alto = più energia (max 8, x2 col temporale)\nServe aria libera intorno"),
    "battery_box": ("Stores 40,000 TU · output from the front", "Accumula 40.000 TU · esce dal davanti"),
    "battery_box_2": ("Stores 600,000 TU · output from the front", "Accumula 600.000 TU · esce dal davanti"),
    "battery_box_3": ("Stores 10,000,000 TU · output from the front", "Accumula 10.000.000 TU · esce dal davanti"),
    "quarry": ("Digs a big area down to the bottom of the world · 64 TU per block\n"
               "More energy = faster (up to 8 blocks per tick)\n"
               "Area: one Area Marker next to it + one at the opposite corner\n"
               "Output: chest/pipe touching it (top first), otherwise it throws the items out the top · right-click: status",
               "Scava un'area grande fino in fondo al mondo · 64 TU a blocco\n"
               "Più energia = più veloce (fino a 8 blocchi per tick)\n"
               "Area: un Segnalino accanto + uno nell'angolo opposto\n"
               "Uscita: baule/tubo che lo tocca (prima sopra), altrimenti sputa gli oggetti dall'alto · click destro: stato"),
    "mining_mole": ("Digs by itself and comes back to the chest next to where you placed it\n"
                    "The area starts at the mole and grows away from the chest (radius 1 = 3x3)\nFuel: coal, charcoal, logs...",
                    "Scava da sola e torna al baule accanto a dove l'hai messa\n"
                    "L'area parte dalla talpa e va dalla parte opposta al baule (raggio 1 = 3x3)\nCarburante: carbone, carbonella, tronchi..."),
    "logistics_core": ("Needs energy. The chest touching it becomes the MAIN chest:\n"
                       "items in it are sent automatically to their destinations",
                       "Serve energia. Il baule che lo tocca diventa il BAULE PRINCIPALE:\n"
                       "gli oggetti dentro partono da soli verso le loro destinazioni"),
    "item_conduit": ("Connects chests and machines", "Collega bauli e macchine"),
    "extractor_conduit": ("Pulls 8 items/second out of the container it touches", "Tira fuori 8 oggetti al secondo dal contenitore che tocca"),
    "fast_extractor_conduit": ("Pulls 64 items/second", "Tira fuori 64 oggetti al secondo"),
    "sorting_conduit": ("Right-click: filter · the chest it touches gets ONLY those items",
                        "Click destro: filtro · il baule che tocca riceve SOLO quegli oggetti"),
    "analyzer_conduit": ("Learns what is in the chest it touches and sends it only those items\n"
                         "Put 1 sample item in the chest · right-click: extra filter, Keep 1, Forget",
                         "Impara cosa c'è nel baule che tocca e gli manda solo quegli oggetti\n"
                         "Metti 1 oggetto di esempio nel baule · click destro: filtro extra, Lascia 1, Dimentica"),
    "network_terminal": ("Shows everything in the chests of the network · take items · request crafts",
                         "Mostra tutto ciò che c'è nei bauli della rete · prendi oggetti · chiedi fabbricazioni"),
    "auto_crafter": ("Set a recipe: crafts with items from the network (on request or to keep a stock)",
                     "Imposta una ricetta: fabbrica con gli oggetti della rete (a richiesta o per tenere una scorta)"),
    "charcoal_kiln": ("Logs → charcoal, 16 every 10 seconds, no fuel", "Tronchi → carbonella, 16 ogni 10 secondi, senza combustibile"),
    "iron_furnace": ("A furnace 1.5x faster", "Una fornace 1,5 volte più veloce"),
    "growth_accelerator": ("Makes crops around it grow faster (energy)", "Fa crescere più in fretta le piante intorno (energia)"),
    "harvester": ("Harvests ripe crops around it (energy)", "Raccoglie le piante mature intorno (energia)"),
}
for id_, (en, it) in DESCRIZIONI_BLOCCHI.items():
    lingua_en[f"tooltip.{MODID}.{id_}"] = en
    lingua_it[f"tooltip.{MODID}.{id_}"] = it
lingua_en.update({
    "message.futuretek.quarry_status.working": "Quarry: digging layer Y=%s · energy %2$s TU · area %4$sx%5$s",
    "message.futuretek.quarry_status.frame": "Quarry: building the frame · energy %2$s TU",
    "message.futuretek.quarry_status.no_energy": "Quarry: WAITING FOR ENERGY (%2$s TU, needs %3$s per block). A solar panel is too slow: use generators or more panels",
    "message.futuretek.quarry_status.full": "Quarry: THE CHEST IS FULL · empty it or connect a bigger chest / pipes",
    "message.futuretek.quarry_status.paused": "Quarry: paused by a redstone signal",
    "message.futuretek.quarry_status.done": "Quarry: finished!",
    "message.futuretek.quarry_status.ejecting": "Quarry: digging layer Y=%s · no room for the items: throwing them out the top (put a chest or pipe on it)",
})
lingua_it.update({
    "message.futuretek.quarry_status.working": "Escavatore: scavo lo strato Y=%s · energia %2$s TU · area %4$sx%5$s",
    "message.futuretek.quarry_status.frame": "Escavatore: costruisco il telaio · energia %2$s TU",
    "message.futuretek.quarry_status.no_energy": "Escavatore: ASPETTO ENERGIA (%2$s TU, ne servono %3$s a blocco). Un pannello solare è troppo lento: usa generatori o più pannelli",
    "message.futuretek.quarry_status.full": "Escavatore: IL BAULE È PIENO · svuotalo o collega un baule più grande / dei tubi",
    "message.futuretek.quarry_status.paused": "Escavatore: in pausa per un segnale di redstone",
    "message.futuretek.quarry_status.done": "Escavatore: finito!",
    "message.futuretek.quarry_status.ejecting": "Escavatore: scavo lo strato Y=%s · non c'è posto per gli oggetti: li sputo fuori dall'alto (mettici sopra un baule o un tubo)",
})


# --- Attrezzi ---
oggetto("forge_hammer", "Forge Hammer", "Martello da forgia", "handheld")
if salva_texture(disegna(MARTELLO, COLORI_MARTELLO, "forge_hammer"), TEX / "item" / "forge_hammer.png"):
    texture_create += 1
r_con_forma("forge_hammer", [" II", " SI", "S  "], {"I": "#c:ingots/iron", "S": "#c:rods/wooden"}, MARTELLO_ID)

# --- Altre traduzioni ---
lingua_en.update({
    "itemGroup.futuretek.resources": "FutureTek - Resources",
    "futuretek.configuration.title": "FutureTek Settings",
    "futuretek.configuration.section.futuretek.common.toml": "FutureTek Settings",
    "futuretek.configuration.section.futuretek.common.toml.title": "FutureTek Settings",
    "futuretek.configuration.debugLogging": "Debug logging",
})
lingua_it.update({
    "itemGroup.futuretek.resources": "FutureTek - Risorse",
    "futuretek.configuration.title": "Impostazioni FutureTek",
    "futuretek.configuration.section.futuretek.common.toml": "Impostazioni FutureTek",
    "futuretek.configuration.section.futuretek.common.toml.title": "Impostazioni FutureTek",
    "futuretek.configuration.debugLogging": "Log di debug",
})

# ===========================================================================
# FASE 11 (4 nov 2026): utensili e armi elettrici, esoarmatura a 3 livelli
# ===========================================================================
for id_, en, it in [("electric_drill", "Electric Drill", "Trapano elettrico"),
                    ("diamond_drill", "Diamond Drill", "Trapano di diamante"),
                    ("chainsaw", "Chainsaw", "Motosega"),
                    ("plasma_saber", "Plasma Saber", "Lama al plasma"),
                    ("pulse_rifle", "Pulse Rifle", "Fucile a impulsi")]:
    oggetto(id_, en, it, "handheld")
NOMI_PEZZI = {"helmet": ("Helmet", "Elmo"), "chestplate": ("Chestplate", "Corazza"),
              "leggings": ("Leggings", "Gambali"), "boots": ("Boots", "Stivali")}
for livello, (n_en, n_it) in {"exo_mk1": ("Exo", "dell'esoarmatura I"), "exo_mk3": ("Exo", "dell'esoarmatura III")}.items():
    num = "I" if livello == "exo_mk1" else "III"
    for pezzo, (p_en, p_it) in NOMI_PEZZI.items():
        oggetto(f"{livello}_{pezzo}", f"{n_en} {p_en} {num}", f"{p_it} {n_it}")
# il trapano scava come piccone + pala
tag(tag_blocchi, "futuretek:mineable/drill", "#minecraft:mineable/pickaxe")
tag(tag_blocchi, "futuretek:mineable/drill", "#minecraft:mineable/shovel")
for id_ in ["electric_drill", "diamond_drill", "chainsaw", "plasma_saber", "pulse_rifle"]:
    tag(tag_oggetti, "futuretek:electric_tools", ft(id_))
tag(tag_oggetti, "minecraft:pickaxes", ft("electric_drill"))
tag(tag_oggetti, "minecraft:pickaxes", ft("diamond_drill"))
tag(tag_oggetti, "minecraft:axes", ft("chainsaw"))
tag(tag_oggetti, "minecraft:swords", ft("plasma_saber"))
tag(tag_oggetti, "minecraft:enchantable/mining", ft("electric_drill"))
tag(tag_oggetti, "minecraft:enchantable/mining", ft("diamond_drill"))
tag(tag_oggetti, "minecraft:enchantable/mining", ft("chainsaw"))
tag(tag_oggetti, "minecraft:enchantable/sword", ft("plasma_saber"))
for livello in ("exo_mk1", "exo_mk3"):
    for pezzo, chiave in [("helmet", "head"), ("chestplate", "chest"), ("leggings", "leg"), ("boots", "foot")]:
        tag(tag_oggetti, f"minecraft:{chiave}_armor", ft(f"{livello}_{pezzo}"))

lingua_en.update({
    "tooltip.futuretek.electric_drill": "Pickaxe + shovel, iron level · 50 TU per block\nRecharge it in a Battery Box",
    "tooltip.futuretek.diamond_drill": "Pickaxe + shovel, diamond level · 80 TU per block\nShift + right-click: 3x3 mode",
    "tooltip.futuretek.chainsaw": "Fast axe, cuts whole trees · 50 TU per block",
    "tooltip.futuretek.plasma_saber": "13 damage while charged (250 TU per hit), 3 when empty",
    "tooltip.futuretek.pulse_rifle": "Right-click: energy beam up to 32 blocks, 8 damage · 400 TU per shot",
    "tooltip.futuretek.electric_empty": "Out of energy: recharge it in a Battery Box",
    "tooltip.futuretek.exo_mk1.helmet": "Night vision (5 TU/t)",
    "tooltip.futuretek.exo_mk1.chestplate": "Energy shield: -15% damage",
    "tooltip.futuretek.exo_mk1.leggings": "Sprint boost (Speed I)",
    "tooltip.futuretek.exo_mk1.boots": "Jump boost I, half fall damage",
    "tooltip.futuretek.exo_mk3.helmet": "Night vision, water breathing, feeds you",
    "tooltip.futuretek.exo_mk3.chestplate": "Energy shield: -45% damage, fire resistance",
    "tooltip.futuretek.exo_mk3.leggings": "Sprint boost (Speed III)",
    "tooltip.futuretek.exo_mk3.boots": "Jump boost III, no fall damage",
})
lingua_it.update({
    "tooltip.futuretek.electric_drill": "Piccone + pala di livello ferro · 50 TU a blocco\nSi ricarica nel banco accumulatore",
    "tooltip.futuretek.diamond_drill": "Piccone + pala di livello diamante · 80 TU a blocco\nShift + click destro: modalità 3x3",
    "tooltip.futuretek.chainsaw": "Ascia veloce, abbatte alberi interi · 50 TU a blocco",
    "tooltip.futuretek.plasma_saber": "13 di danno da carica (250 TU a colpo), 3 da scarica",
    "tooltip.futuretek.pulse_rifle": "Click destro: raggio di energia fino a 32 blocchi, 8 di danno · 400 TU a colpo",
    "tooltip.futuretek.electric_empty": "Scarico: ricaricalo nel banco accumulatore",
    "tooltip.futuretek.exo_mk1.helmet": "Visione notturna (5 TU/t)",
    "tooltip.futuretek.exo_mk1.chestplate": "Scudo energetico: -15% danni",
    "tooltip.futuretek.exo_mk1.leggings": "Scatto potenziato (Velocità I)",
    "tooltip.futuretek.exo_mk1.boots": "Salto potenziato I, metà danni da caduta",
    "tooltip.futuretek.exo_mk3.helmet": "Visione notturna, respiri sott'acqua, ti sfama",
    "tooltip.futuretek.exo_mk3.chestplate": "Scudo energetico: -45% danni, resistenza al fuoco",
    "tooltip.futuretek.exo_mk3.leggings": "Scatto potenziato (Velocità III)",
    "tooltip.futuretek.exo_mk3.boots": "Salto potenziato III, niente danni da caduta",
})
lingua_en["item.futuretek.exo_helmet"] = "Exo Helmet II"
lingua_en["item.futuretek.exo_chestplate"] = "Exo Chestplate II"
lingua_en["item.futuretek.exo_leggings"] = "Exo Leggings II"
lingua_en["item.futuretek.exo_boots"] = "Exo Boots II"
for pezzo, (_, p_it) in NOMI_PEZZI.items():
    lingua_it[f"item.futuretek.exo_{pezzo}"] = f"{p_it} dell'esoarmatura II"

# ricette
r_con_forma("electric_drill", [" P ", "PCP", "PBP"],
            {"P": PIASTRA_FERRO, "C": ft("basic_circuit"), "B": ft("battery_cell")}, ft("electric_drill"))
r_con_forma("diamond_drill", [" D ", "DXD", " A "],
            {"D": "#c:gems/diamond", "X": ft("electric_drill"), "A": ft("advanced_circuit")}, ft("diamond_drill"))
r_con_forma("chainsaw", [" PP", "PCP", "BP "],
            {"P": PIASTRA_FERRO, "C": ft("basic_circuit"), "B": ft("battery_cell")}, ft("chainsaw"))
r_con_forma("plasma_saber", ["  V", " V ", "SA "],
            {"V": ft("voltite_shard"), "S": "#c:plates/steel", "A": ft("advanced_battery_cell")}, ft("plasma_saber"))
r_con_forma("pulse_rifle", ["SSS", "VBC", "  S"],
            {"S": "#c:plates/steel", "V": ft("voltite_shard"), "B": ft("advanced_battery_cell"),
             "C": ft("advanced_circuit")}, ft("pulse_rifle"))
for pezzo, schema in SCHEMI_EXO.items():
    r_con_forma(f"exo_mk1_{pezzo}", schema, {"M": PIASTRA_FERRO, "C": ft("battery_cell")}, ft(f"exo_mk1_{pezzo}"))
    r_senza_forma(f"exo_mk3_{pezzo}", [ft(f"exo_{pezzo}"), "minecraft:netherite_ingot", ft("advanced_circuit"),
                                       ft("advanced_circuit"), ft("advanced_battery_cell")], ft(f"exo_mk3_{pezzo}"))

# ===========================================================================
# FASI 12-14 (4 nov 2026): casa automatica, macchine del Nether, robot
# ===========================================================================
MACCHINE_12_14 = [
    ("tree_farm", "Tree Farm", "Fattoria di alberi"),
    ("planter", "Planter", "Piantatrice"),
    ("auto_fertilizer", "Auto Fertilizer", "Fertilizzatore automatico"),
    ("energy_switch", "Energy Switch", "Interruttore elettrico"),
    ("electric_enchanter", "Electric Enchanter", "Incantatore elettrico"),
    ("mob_incubator", "Creature Incubator", "Incubatore di creature"),
    ("laser_turret", "Laser Turret", "Torretta laser"),
    ("robot_dock", "Robot Charging Dock", "Base di ricarica per robot"),
]
for id_, en, it in MACCHINE_12_14:
    blocco(id_, en, it)
    modelli_speciali[id_] = modelli_macchina(id_, f"{id_}_front", f"{id_}_front_on", top=f"{id_}_top")
    tag(tag_blocchi, "minecraft:mineable/pickaxe", ft(id_))
blocco("electric_lamp", "Electric Lamp", "Lampada elettrica")
blocco("light_panel", "Light Panel", "Pannello luminoso")
for id_ in ["electric_lamp", "light_panel"]:
    tag(tag_blocchi, "minecraft:mineable/pickaxe", ft(id_))


def modelli_lampade():
    A_ = f"assets/{MODID}"
    for acceso in ["", "_on"]:
        scrivi_json(f"{A_}/models/block/electric_lamp{acceso}.json", {
            "parent": "minecraft:block/cube_all", "textures": {"all": f"{MODID}:block/electric_lamp{acceso}"}})
        tex = f"{MODID}:block/light_panel{acceso}"
        scrivi_json(f"{A_}/models/block/light_panel{acceso}.json", {
            "parent": "minecraft:block/block", "textures": {"particle": tex, "all": tex},
            "elements": [{"from": [1, 0, 1], "to": [15, 2, 15], "faces": {
                f: {"texture": "#all", **({"cullface": "down"} if f == "down" else {})}
                for f in ["down", "up", "north", "south", "east", "west"]}}]})
    varianti_l, varianti_p = {}, {}
    rot = {"up": {}, "down": {"x": 180}, "north": {"x": 90}, "south": {"x": 90, "y": 180},
           "west": {"x": 90, "y": 270}, "east": {"x": 90, "y": 90}}
    for faccia, r in rot.items():
        for acceso in ["false", "true"]:
            suf = "_on" if acceso == "true" else ""
            varianti_l[f"facing={faccia},lit={acceso}"] = {"model": f"{MODID}:block/electric_lamp{suf}"}
            varianti_p[f"facing={faccia},lit={acceso}"] = {"model": f"{MODID}:block/light_panel{suf}", **r}
    scrivi_json(f"{A_}/blockstates/electric_lamp.json", {"variants": varianti_l})
    scrivi_json(f"{A_}/blockstates/light_panel.json", {"variants": varianti_p})
    scrivi_json(f"{A_}/models/item/electric_lamp.json", {"parent": f"{MODID}:block/electric_lamp_on"})
    scrivi_json(f"{A_}/models/item/light_panel.json", {"parent": f"{MODID}:block/light_panel_on"})


modelli_speciali["electric_lamp"] = modelli_lampade
modelli_speciali["light_panel"] = lambda: None

for id_, en, it in [("soul_core", "Soul Core", "Nucleo d'anima"),
                    ("mob_sampler", "Creature Sampler", "Campionatore di creature"),
                    ("robot_chassis", "Robot Chassis", "Telaio di robot"),
                    ("robot", "Robot", "Robot"),
                    ("infinite_core", "Infinite Core", "Nucleo infinito"),
                    ("robot_controller", "Robot Control Panel", "Pannello di controllo robot")]:
    oggetto(id_, en, it)

TESTI_12_14 = {
    # chiave: (inglese, italiano)
    "tooltip.futuretek.tree_farm": ("Plants saplings in a 9x9 area around it (one every 3 blocks) and cuts the grown trees\n"
                                    "Saplings in the first 4 slots · wood goes to the chest next to it",
                                    "Pianta alberelli in un'area 9x9 intorno a sé (uno ogni 3 blocchi) e taglia gli alberi cresciuti\n"
                                    "Alberelli nelle prime 4 caselle · il legno va nel baule accanto"),
    "tooltip.futuretek.planter": ("Tills the dirt and plants seeds in a 9x9 area at its own level\nUse it with a Harvester",
                                  "Zappa la terra e pianta i semi in un'area 9x9 al suo livello\nUsala con il Raccoglitore"),
    "tooltip.futuretek.auto_fertilizer": ("Uses bone meal on the plants in a 9x9 area · 60 TU each",
                                     "Usa la farina d'ossa sulle piante in un'area 9x9 · 60 TU a colpo"),
    "tooltip.futuretek.electric_lamp": ("Bright light (15) while it has energy · 2 TU/t\nLamps that touch share the energy: connect one to a cable",
                                        "Luce forte (15) finché ha energia · 2 TU/t\nLe lampade che si toccano si passano l'energia: basta collegarne una"),
    "tooltip.futuretek.light_panel": ("Thin lamp for floors, walls and ceilings · 2 TU/t",
                                      "Lampada sottile per pavimenti, pareti e soffitti · 2 TU/t"),
    "tooltip.futuretek.energy_switch": ("Lets energy through (out of the front) only with a redstone signal\nPut a lever on it to turn your lights on and off",
                                        "Lascia passare l'energia (esce dal davanti) solo con un segnale redstone\nMettici una leva per accendere e spegnere le luci"),
    "tooltip.futuretek.electric_enchanter": ("Enchants with fewer experience levels, using a lot of energy\nLevel I: 2 levels · II: 5 levels · III: 9 levels",
                                             "Incanta con meno livelli di esperienza, usando tanta energia\nGrado I: 2 livelli · II: 5 livelli · III: 9 livelli"),
    "tooltip.futuretek.mob_incubator": ("Put a filled Creature Sampler inside: that creature appears on top\n6000 TU each · more energy = more often (max 6 nearby)",
                                        "Mettici un Campionatore pieno: quella creatura compare sopra\n6000 TU a creatura · più energia = più spesso (max 6 vicine)"),
    "tooltip.futuretek.laser_turret": ("Hits creatures in a 7x7 area (from 1 below to 3 above) · 150 TU per hit\nThey drop items and experience as if you killed them\nItems go into a chest touching the turret",
                                       "Colpisce le creature in un'area 7x7 (da 1 sotto a 3 sopra) · 150 TU a colpo\nLasciano oggetti ed esperienza come se le uccidessi tu\nGli oggetti finiscono in un baule attaccato alla torretta"),
    "tooltip.futuretek.robot_dock": ("Charges the robot standing on it · put a chest next to it for the robot's loot",
                                     "Ricarica il robot che ci sta sopra · mettici accanto un baule per il bottino del robot"),
    "tooltip.futuretek.soul_core": ("Nether material for the advanced machines", "Materiale del Nether per le macchine avanzate"),
    "tooltip.futuretek.mob_sampler": ("Right-click a creature to take a sample", "Click destro su una creatura per prenderne un campione"),
    "tooltip.futuretek.mob_sampler.has": ("Sample: %s", "Campione: %s"),
    "tooltip.futuretek.robot": ("Right-click a Charging Dock to turn it on: it always uses that dock and the chest next to it\n"
                                "Give it a pickaxe / axe / hoe (right-click it with the tool) · empty hand: change job\n"
                                "Shift + right-click: pick it up · Infinite Core: never needs charging",
                                "Click destro su una base di ricarica per accenderlo: userà sempre quella base e il baule accanto\n"
                                "Dagli un piccone / ascia / zappa (click destro sul robot con l'attrezzo) · mano vuota: cambia lavoro\n"
                                "Shift + click destro: lo riprendi · Nucleo infinito: non si scarica mai"),
    "tooltip.futuretek.robot_controller": ("Right-click a robot: link it · then right-click a chest (even far away): the robot uses that chest\n"
                                           "Right-click a charging dock: new dock · right-click in the air: status · Shift + right-click in the air: continue",
                                           "Click destro su un robot: lo colleghi · poi click destro su un baule (anche lontano): il robot userà quel baule\n"
                                           "Click destro su una base: nuova base · click destro in aria: stato · Shift + click destro in aria: continua"),
    "tooltip.futuretek.robot_controller.free": ("No robot linked", "Nessun robot collegato"),
    "tooltip.futuretek.robot_controller.linked": ("Linked to a robot", "Collegato a un robot"),
    "gui.futuretek.robot_mode.4": ("Lumberjack", "Taglialegna"),
    "gui.futuretek.robot_state.0": ("working", "lavora"),
    "gui.futuretek.robot_state.1": ("recharging", "si ricarica"),
    "gui.futuretek.robot_state.2": ("needs a tool (put one in its chest)", "gli serve un attrezzo (mettilo nel suo baule)"),
    "gui.futuretek.robot_state.3": ("CHEST FULL: empty it, then tell it to continue", "BAULE PIENO: svuotalo e poi digli di continuare"),
    "gui.futuretek.robot_state.4": ("no chest: put one next to the dock or link one with the panel", "nessun baule: mettine uno accanto alla base o collegalo col pannello"),
    "gui.futuretek.robot_state.5": ("job finished (tell it to continue for more)", "lavoro finito (digli di continuare per farne altro)"),
    "gui.futuretek.robot_state.6": ("going to take a tool", "va a prendere un attrezzo"),
    "gui.futuretek.robot_state.7": ("bringing things to the chest", "porta la roba nel baule"),
    "gui.futuretek.robot_state.8": ("nothing to do, waiting", "niente da fare, aspetta"),
    "message.futuretek.robot_status": ("Robot %s: %s · energy %s%% · %s", "Robot %s: %s · energia %s%% · %s"),
    "message.futuretek.robot_tool": ("%s (%s uses left)", "%s (ancora %s usi)"),
    "message.futuretek.robot_no_tool_held": ("no tool", "nessun attrezzo"),
    "message.futuretek.robot_resume": ("Robot: OK, I'll continue!", "Robot: ok, continuo!"),
    "message.futuretek.controller_linked": ("Panel linked to the robot (%s). Now right-click its chest or dock.",
                                            "Pannello collegato al robot (%s). Ora fai click destro sul suo baule o sulla sua base."),
    "message.futuretek.controller_none": ("Right-click a robot first to link it", "Prima fai click destro su un robot per collegarlo"),
    "message.futuretek.controller_far": ("The robot can't be reached (too far or unloaded)", "Il robot non si raggiunge (troppo lontano o non caricato)"),
    "message.futuretek.controller_dock": ("This is now the robot's charging dock", "Questa ora è la base di ricarica del robot"),
    "message.futuretek.controller_chest": ("The robot will bring everything to the chest at %s %s %s", "Il robot porterà tutto nel baule a %s %s %s"),
    "message.futuretek.controller_where": ("Robot at %s %s %s · chest: %s", "Robot a %s %s %s · baule: %s"),
    "tooltip.futuretek.robot_chassis": ("Base for building a robot", "Base per costruire un robot"),
    "tooltip.futuretek.infinite_core": ("Right-click a robot: it never runs out of energy", "Click destro su un robot: non si scarica mai"),
    "gui.futuretek.tree_farm.status0": ("Waiting for trees", "Aspetto che crescano gli alberi"),
    "gui.futuretek.tree_farm.status1": ("Cutting...", "Taglio..."),
    "gui.futuretek.tree_farm.status2": ("Planting saplings", "Pianto alberelli"),
    "gui.futuretek.tree_farm.status3": ("No room for the wood!", "Nessun posto per il legno!"),
    "gui.futuretek.tree_farm.status4": ("Not enough energy", "Energia insufficiente"),
    "gui.futuretek.planter.status0": ("Field ready", "Campo pronto"),
    "gui.futuretek.planter.status1": ("Planting...", "Pianto..."),
    "gui.futuretek.planter.status2": ("Put seeds in me", "Mettimi dei semi"),
    "gui.futuretek.planter.status4": ("Not enough energy", "Energia insufficiente"),
    "gui.futuretek.auto_fertilizer.status0": ("No plant to grow", "Nessuna pianta da far crescere"),
    "gui.futuretek.auto_fertilizer.status1": ("Fertilizing...", "Concimo..."),
    "gui.futuretek.auto_fertilizer.status2": ("Put bone meal in me", "Mettimi farina d'ossa"),
    "gui.futuretek.auto_fertilizer.status4": ("Not enough energy", "Energia insufficiente"),
    "gui.futuretek.electric_enchanter.status0": ("Item + lapis · energy %sk TU", "Oggetto + lapislazzuli · energia %sk TU"),
    "gui.futuretek.electric_enchanter.status1": ("Ready · energy %sk TU", "Pronto · energia %sk TU"),
    "gui.futuretek.electric_enchanter.status2": ("This item can't be enchanted", "Questo oggetto non si può incantare"),
    "gui.futuretek.electric_enchanter.status3": ("Done! · energy %sk TU", "Fatto! · energia %sk TU"),
    "gui.futuretek.electric_enchanter.button0": ("I · 2 lv · 25k", "I · 2 liv · 25k"),
    "gui.futuretek.electric_enchanter.button1": ("II · 5 lv · 75k", "II · 5 liv · 75k"),
    "gui.futuretek.electric_enchanter.button2": ("III · 9 lv · 200k", "III · 9 liv · 200k"),
    "gui.futuretek.electric_enchanter.button0.hint": ("Power 10 · 2 experience levels · 1 lapis · 25,000 TU",
                                                      "Potenza 10 · 2 livelli di esperienza · 1 lapislazzuli · 25.000 TU"),
    "gui.futuretek.electric_enchanter.button1.hint": ("Power 20 · 5 experience levels · 2 lapis · 75,000 TU",
                                                      "Potenza 20 · 5 livelli di esperienza · 2 lapislazzuli · 75.000 TU"),
    "gui.futuretek.electric_enchanter.button2.hint": ("Power 30 (like a full enchanting table) · 9 levels · 3 lapis · 200,000 TU",
                                                      "Potenza 30 (come un tavolo con tutte le librerie) · 9 livelli · 3 lapislazzuli · 200.000 TU"),
    "gui.futuretek.mob_incubator.status0": ("Put a filled sampler in me", "Mettimi un campionatore pieno"),
    "gui.futuretek.mob_incubator.status1": ("Creatures coming · energy %s%%", "Creature in arrivo · energia %s%%"),
    "gui.futuretek.mob_incubator.status2": ("Not enough energy (%s%%)", "Energia insufficiente (%s%%)"),
    "gui.futuretek.mob_incubator.status3": ("Too many creatures nearby", "Troppe creature vicino"),
    "gui.futuretek.mob_incubator.status4": ("No free space on top", "Nessuno spazio libero sopra"),
    "gui.futuretek.laser_turret.status0": ("Only monsters · no target", "Solo mostri · nessun bersaglio"),
    "gui.futuretek.laser_turret.status1": ("Only monsters · hitting %s", "Solo mostri · ne colpisco %s"),
    "gui.futuretek.laser_turret.status2": ("Only monsters · not enough energy", "Solo mostri · energia insufficiente"),
    "gui.futuretek.laser_turret.status10": ("All creatures · no target", "Tutte le creature · nessun bersaglio"),
    "gui.futuretek.laser_turret.status11": ("All creatures · hitting %s", "Tutte le creature · ne colpisco %s"),
    "gui.futuretek.laser_turret.status12": ("All creatures · not enough energy", "Tutte le creature · energia insufficiente"),
    "gui.futuretek.laser_turret.button0": ("Change targets", "Cambia bersagli"),
    "gui.futuretek.laser_turret.button0.hint": ("Only monsters / all creatures (never players, pets, villagers or named ones)",
                                                "Solo mostri / tutte le creature (mai giocatori, animali domestici, abitanti o creature col nome)"),
    "gui.futuretek.robot_mode.0": ("Farmer", "Contadino"),
    "gui.futuretek.robot_mode.1": ("Bodyguard", "Guardia del corpo"),
    "gui.futuretek.robot_mode.2": ("Sentry", "Sentinella"),
    "gui.futuretek.robot_mode.3": ("Miner", "Minatore"),
    "message.futuretek.robot_mode": ("Robot: %s · energy %s%%", "Robot: %s · energia %s%%"),
    "message.futuretek.robot_job": ("Job: %s", "Lavoro: %s"),
    "message.futuretek.robot_no_dock": ("No charging dock: this robot will not recharge (place it on a dock)",
                                        "Nessuna base: questo robot non si ricaricherà (mettilo su una base di ricarica)"),
    "message.futuretek.robot_infinite": ("Infinite Core installed: this robot never runs out of energy",
                                         "Nucleo infinito installato: questo robot non si scarica mai"),
    "message.futuretek.enchanter_missing": ("You need %s levels, %s lapis and %sk TU", "Servono %s livelli, %s lapislazzuli e %sk TU"),
    "message.futuretek.sampler_refused": ("You can't sample this creature", "Non puoi campionare questa creatura"),
    "message.futuretek.sampler_done": ("Sample taken: %s", "Campione preso: %s"),
}
for chiave, (en, it) in TESTI_12_14.items():
    lingua_en[chiave] = en
    lingua_it[chiave] = it

# ricette
r_con_forma("tree_farm", ["SAS", "CMC", "SHS"], {"S": "#minecraft:saplings", "A": "minecraft:iron_axe", "C": C, "M": M,
                                                "H": "minecraft:hopper"}, ft("tree_farm"))
r_con_forma("planter", ["WHW", "CMC", "WPW"], {"W": "minecraft:wheat_seeds", "H": "minecraft:iron_hoe", "C": C, "M": M,
                                              "P": "minecraft:hopper"}, ft("planter"))
r_con_forma("auto_fertilizer", ["BDB", "CMC", "BBB"], {"B": "minecraft:bone_meal", "D": "minecraft:dispenser", "C": C, "M": M},
            ft("auto_fertilizer"))
r_con_forma("electric_lamp", ["GGG", "GLG", "PWP"], {"G": "minecraft:glass", "L": "minecraft:glowstone_dust",
                                                    "P": PIASTRA_FERRO, "W": ft("tin_cable")}, ft("electric_lamp"), 4)
r_con_forma("light_panel", ["LLL"], {"L": ft("electric_lamp")}, ft("light_panel"), 6)
r_con_forma("energy_switch", ["PLP", "WCW", "PPP"], {"P": PIASTRA_FERRO, "L": "minecraft:lever", "W": ft("tin_cable"), "C": C},
            ft("energy_switch"))
r_con_forma("soul_core", ["SQS", "BTB", "SQS"], {"S": "minecraft:soul_sand", "Q": "minecraft:quartz",
                                                "B": "minecraft:blaze_powder", "T": "minecraft:ghast_tear"}, ft("soul_core"))
r_con_forma("mob_sampler", [" R ", "GNG", " G "], {"R": "minecraft:blaze_rod", "G": "minecraft:glass", "N": ft("soul_core")},
            ft("mob_sampler"))
r_con_forma("electric_enchanter", ["ABA", "DED", "OMO"], {"A": ft("advanced_circuit"), "B": "minecraft:book",
                                                         "D": "#c:gems/diamond", "E": "minecraft:enchanting_table",
                                                         "O": "minecraft:obsidian", "M": M}, ft("electric_enchanter"))
r_con_forma("mob_incubator", ["SNS", "AMA", "SNS"], {"S": "#c:plates/steel", "N": ft("soul_core"),
                                                    "A": ft("advanced_circuit"), "M": M}, ft("mob_incubator"))
r_con_forma("laser_turret", ["DVD", "AMA", "SBS"], {"D": "#c:gems/diamond", "V": ft("voltite_shard"),
                                                   "A": ft("advanced_circuit"), "M": M, "S": "#c:plates/steel",
                                                   "B": ft("advanced_battery_cell")}, ft("laser_turret"))
r_con_forma("robot_chassis", ["SHS", "SAS", "S S"], {"S": "#c:plates/steel", "H": "minecraft:iron_helmet",
                                                    "A": ft("advanced_circuit")}, ft("robot_chassis"))
r_senza_forma("robot", [ft("robot_chassis"), ft("advanced_battery_cell"), ft("advanced_circuit"),
                        "minecraft:redstone_block"], ft("robot"))
r_con_forma("robot_dock", ["PWP", "BMB", "PPP"], {"P": PIASTRA_FERRO, "W": ft("tin_cable"), "B": ft("battery_box"), "M": M},
            ft("robot_dock"))
r_con_forma("robot_controller", [" R ", "PGP", "PCP"], {"R": "minecraft:redstone_torch", "P": PIASTRA_FERRO,
                                                    "G": "minecraft:glass_pane", "C": ft("advanced_circuit")},
            ft("robot_controller"))
r_con_forma("infinite_core", ["XNX", "NSN", "XNX"], {"X": ft("advanced_battery_cell"), "N": ft("soul_core"),
                                                    "S": "minecraft:nether_star"}, ft("infinite_core"))

# ===========================================================================
# FASE 15 (5 nov 2026): nucleare (TNT, reattore a camere, celle che si consumano, manutentore) e fabbrica abbandonata
# ===========================================================================
blocco("nuclear_tnt", "Nuclear TNT", "TNT nucleare")


def modelli_tnt_nucleare():
    A_ = f"assets/{MODID}"
    scrivi_json(f"{A_}/models/block/nuclear_tnt.json", {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {"top": f"{MODID}:block/nuclear_tnt_top", "side": f"{MODID}:block/nuclear_tnt_side",
                     "bottom": f"{MODID}:block/nuclear_tnt_bottom"}})
    scrivi_json(f"{A_}/blockstates/nuclear_tnt.json", {"variants": {"unstable=false": {"model": f"{MODID}:block/nuclear_tnt"},
                                                                    "unstable=true": {"model": f"{MODID}:block/nuclear_tnt"}}})
    scrivi_json(f"{A_}/models/item/nuclear_tnt.json", {"parent": f"{MODID}:block/nuclear_tnt"})


modelli_speciali["nuclear_tnt"] = modelli_tnt_nucleare
blocco("reactor_maintainer", "Reactor Maintainer", "Manutentore del reattore")
modelli_speciali["reactor_maintainer"] = modelli_macchina("reactor_maintainer", "reactor_maintainer_front",
                                                          "reactor_maintainer_front_on", top="reactor_maintainer_top")
tag(tag_blocchi, "minecraft:mineable/pickaxe", ft("reactor_maintainer"))

CELLE_NUOVE = {
    "coolant_cell_30k": ("Coolant Cell 30k", "Cella refrigerante 30k", (60, 120, 220)),
    "coolant_cell_60k": ("Coolant Cell 60k", "Cella refrigerante 60k", (110, 80, 220)),
    "spent_coolant_cell": ("Spent Coolant Cell", "Cella refrigerante esausta", (200, 90, 50)),
    "spent_coolant_cell_30k": ("Spent Coolant Cell 30k", "Cella refrigerante esausta 30k", (210, 70, 40)),
    "spent_coolant_cell_60k": ("Spent Coolant Cell 60k", "Cella refrigerante esausta 60k", (220, 50, 50)),
}
for id_, (n_en, n_it, colore) in CELLE_NUOVE.items():
    oggetto(id_, n_en, n_it)
    img = disegna(CELLA, palette(colore), id_)
    if salva_texture(img, TEX / "item" / f"{id_}.png"):
        texture_create += 1
lingua_en["item.futuretek.coolant_cell"] = "Coolant Cell 10k"
lingua_it["item.futuretek.coolant_cell"] = "Cella refrigerante 10k"

TESTI_15 = {
    "tooltip.futuretek.reactor.coolant_cell": ("Absorbs up to 10,000 heat from adjacent rods. When full it becomes Spent",
                                               "Assorbe fino a 10.000 calore delle barre accanto. Piena diventa esausta"),
    "tooltip.futuretek.reactor.coolant_cell_30k": ("Absorbs up to 30,000 heat. When full it becomes Spent",
                                                   "Assorbe fino a 30.000 calore. Piena diventa esausta"),
    "tooltip.futuretek.reactor.coolant_cell_60k": ("Absorbs up to 60,000 heat. When full it becomes Spent",
                                                   "Assorbe fino a 60.000 calore. Piena diventa esausta"),
    "tooltip.futuretek.spent_coolant_cell": ("Full of heat: craft it with a water bucket (or let a Reactor Maintainer touching water refill it)",
                                             "Piena di calore: ricaricala con un secchio d'acqua (o con un Manutentore che tocca l'acqua)"),
    "tooltip.futuretek.spent_coolant_cell_30k": ("Full of heat: craft it with a water bucket", "Piena di calore: ricaricala con un secchio d'acqua"),
    "tooltip.futuretek.spent_coolant_cell_60k": ("Full of heat: craft it with a water bucket", "Piena di calore: ricaricala con un secchio d'acqua"),
    "tooltip.futuretek.nuclear_tnt": ("Light it like TNT: after 10 seconds it erases a huge sphere (radius 32)\nCan be turned off in the config",
                                      "Si accende come la TNT: dopo 10 secondi cancella una sfera enorme (raggio 32)\nSi può disattivare nella config"),
    "tooltip.futuretek.reactor_core": ("Needs a redstone signal (lever) to run · 6 rows x 3 columns\n"
                                       "Every Reactor Chamber touching it adds a column (up to 9) · at 100% heat it EXPLODES",
                                       "Funziona con un segnale redstone (leva) · 6 righe x 3 colonne\n"
                                       "Ogni Camera del reattore attaccata aggiunge una colonna (fino a 9) · al 100% di calore ESPLODE"),
    "tooltip.futuretek.reactor_chamber": ("Attach it to the Reactor Core: +1 column of slots · cables and pipes can connect to it",
                                          "Attaccala al nucleo: +1 colonna di caselle · ci puoi collegare cavi e tubi"),
    "tooltip.futuretek.reactor_maintainer": ("Touching the reactor: swaps hot coolant cells, vents and depleted rods with fresh ones from its slots\n"
                                             "Touching water: refills spent coolant cells by itself",
                                             "Attaccato al reattore: cambia celle calde, dissipatori e barre esaurite con quelli nuovi delle sue caselle\n"
                                             "Se tocca l'acqua: ricarica da solo le celle esauste"),
    "gui.futuretek.reactor_maintainer.status0": ("Touch a reactor core or chamber", "Attaccalo al nucleo o a una camera"),
    "gui.futuretek.reactor_maintainer.status1": ("Watching the reactor · %s swaps", "Controllo il reattore · %s cambi"),
    "gui.futuretek.reactor_maintainer.status2": ("Missing spare parts! · %s swaps", "Mancano pezzi di ricambio! · %s cambi"),
    "gui.futuretek.reactor_on": ("ON", "ACCESO"),
    "gui.futuretek.reactor_off": ("OFF (redstone)", "SPENTO (redstone)"),
    "gui.futuretek.reactor_more_columns": ("Attach Reactor Chambers to the core to unlock these columns",
                                           "Attacca delle Camere del reattore al nucleo per sbloccare queste colonne"),
    "message.futuretek.reactor_danger": ("REACTOR DANGER: heat over 70%! Turn it off (redstone) or add cooling before it explodes!",
                                         "PERICOLO REATTORE: calore oltre il 70%! Spegnilo (redstone) o raffreddalo prima che esploda!"),
    "gui.futuretek.factory_title": ("Abandoned Factory", "Fabbrica abbandonata"),
    "gui.futuretek.factory_hint": ("Fix the cables!", "Ripara i cavi!"),
    "gui.futuretek.factory_hall.energia": ("ENERGY", "ENERGIA"),
    "gui.futuretek.factory_hall.lavorazione": ("PROCESSING", "LAVORAZIONE"),
    "gui.futuretek.factory_hall.logistica": ("LOGISTICS", "LOGISTICA"),
    "gui.futuretek.factory_hall.agricoltura": ("FARMING", "AGRICOLTURA"),
    "gui.futuretek.factory_hall.miniera": ("MINING", "MINIERA"),
    "gui.futuretek.factory_hall.nucleare": ("NUCLEAR", "NUCLEARE"),
    "gui.futuretek.factory_hall.avanzate": ("ADVANCED", "AVANZATE"),
    "gui.futuretek.factory_hall.alchimia": ("ALCHEMY", "ALCHIMIA"),
    "gui.futuretek.factory_hall.varie": ("OTHER", "VARIE"),
    "message.futuretek.factory_where": ("An abandoned FutureTek factory is near spawn: entrance at %s %s %s. Bring it back to life!",
                                        "Vicino al punto di nascita c'è una fabbrica FutureTek abbandonata: entrata a %s %s %s. Rimettila in funzione!"),
    "message.futuretek.factory_built": ("Factory built: entrance at %s %s %s", "Fabbrica costruita: entrata a %s %s %s"),
    "gamerule.futuretekAbandonedFactory": ("FutureTek: abandoned factory near spawn", "FutureTek: fabbrica abbandonata vicino al punto di nascita"),
    "gamerule.futuretekAbandonedFactory.description": ("A ruined factory with one of every FutureTek machine, to repair",
                                                       "Un capannone in rovina con una copia di ogni macchina FutureTek, da rimettere in funzione"),
}
for chiave, (en, it) in TESTI_15.items():
    lingua_en[chiave] = en
    lingua_it[chiave] = it

r_con_forma("nuclear_tnt", ["UTU", "TCT", "UTU"], {"U": "#c:ingots/uranium", "T": "minecraft:tnt",
                                                  "C": ft("advanced_circuit")}, ft("nuclear_tnt"))
r_con_forma("reactor_maintainer", ["PHP", "CMC", "PRP"], {"P": "#c:plates/lead", "H": "minecraft:hopper",
                                                         "C": ft("advanced_circuit"), "M": M, "R": "minecraft:piston"},
            ft("reactor_maintainer"))
r_con_forma("coolant_cell_30k", ["TTT", "CCC", "TTT"], {"T": "#c:plates/tin", "C": ft("coolant_cell")}, ft("coolant_cell_30k"))
r_con_forma("coolant_cell_60k", ["TCT", "TPT", "TCT"], {"T": "#c:plates/tin", "C": ft("coolant_cell_30k"),
                                                       "P": "#c:plates/copper"}, ft("coolant_cell_60k"))
for spenta, nuova in [("spent_coolant_cell", "coolant_cell"), ("spent_coolant_cell_30k", "coolant_cell_30k"),
                      ("spent_coolant_cell_60k", "coolant_cell_60k")]:
    r_senza_forma(f"{nuova}_from_spent", [ft(spenta), "minecraft:water_bucket"], ft(nuova))

# ===========================================================================
# FASE 16 (7 nov 2026): blocchi da costruzione (specifica in tools/blocchi_costruzione.json)
# ===========================================================================
COSTRUZIONE = json.loads((Path(__file__).resolve().parent / "blocchi_costruzione.json").read_text(encoding="utf-8"))["blocchi"]
A16 = f"assets/{MODID}"


def tex_di(b):
    return b["texture"]


def m_scale(id_, tex):
    def f():
        for nome, parent in [("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")]:
            scrivi_json(f"{A16}/models/block/{id_}{nome}.json", {"parent": f"minecraft:block/{parent}",
                        "textures": {"bottom": tex, "top": tex, "side": tex}})
        base_y = {"east": 0, "south": 90, "west": 180, "north": 270}
        varianti = {}
        for facing, y0 in base_y.items():
            for half in ("bottom", "top"):
                for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                    model = f"{MODID}:block/{id_}" + ("_inner" if shape.startswith("inner") else
                                                     "_outer" if shape.startswith("outer") else "")
                    y = y0
                    if half == "bottom" and shape.endswith("left"):
                        y -= 90
                    if half == "top" and shape.endswith("right"):
                        y += 90
                    v = {"model": model}
                    if half == "top":
                        v["x"] = 180
                    if y % 360:
                        v["y"] = y % 360
                    if "x" in v or "y" in v:
                        v["uvlock"] = True
                    varianti[f"facing={facing},half={half},shape={shape}"] = v
        scrivi_json(f"{A16}/blockstates/{id_}.json", {"variants": varianti})
        scrivi_json(f"{A16}/models/item/{id_}.json", {"parent": f"{MODID}:block/{id_}"})
    return f


def m_lastra(id_, tex, base):
    def f():
        scrivi_json(f"{A16}/models/block/{id_}.json", {"parent": "minecraft:block/slab",
                    "textures": {"bottom": tex, "top": tex, "side": tex}})
        scrivi_json(f"{A16}/models/block/{id_}_top.json", {"parent": "minecraft:block/slab_top",
                    "textures": {"bottom": tex, "top": tex, "side": tex}})
        scrivi_json(f"{A16}/blockstates/{id_}.json", {"variants": {
            "type=bottom": {"model": f"{MODID}:block/{id_}"},
            "type=top": {"model": f"{MODID}:block/{id_}_top"},
            "type=double": {"model": f"{MODID}:block/{base}"}}})
        scrivi_json(f"{A16}/models/item/{id_}.json", {"parent": f"{MODID}:block/{id_}"})
    return f


def m_muro(id_, tex):
    def f():
        for nome, parent in [("_post", "template_wall_post"), ("_side", "template_wall_side"),
                             ("_side_tall", "template_wall_side_tall")]:
            scrivi_json(f"{A16}/models/block/{id_}{nome}.json", {"parent": f"minecraft:block/{parent}",
                        "textures": {"wall": tex}})
        parti = [{"when": {"up": "true"}, "apply": {"model": f"{MODID}:block/{id_}_post"}}]
        for lato, y in [("north", 0), ("east", 90), ("south", 180), ("west", 270)]:
            parti.append({"when": {lato: "low"}, "apply": {"model": f"{MODID}:block/{id_}_side", "y": y, "uvlock": True}})
            parti.append({"when": {lato: "tall"}, "apply": {"model": f"{MODID}:block/{id_}_side_tall", "y": y, "uvlock": True}})
        scrivi_json(f"{A16}/blockstates/{id_}.json", {"multipart": parti})
        scrivi_json(f"{A16}/models/item/{id_}.json", {"parent": "minecraft:block/wall_inventory", "textures": {"wall": tex}})
    return f


def m_staccionata(id_, tex):
    def f():
        scrivi_json(f"{A16}/models/block/{id_}_post.json", {"parent": "minecraft:block/fence_post", "textures": {"texture": tex}})
        scrivi_json(f"{A16}/models/block/{id_}_side.json", {"parent": "minecraft:block/fence_side", "textures": {"texture": tex}})
        parti = [{"apply": {"model": f"{MODID}:block/{id_}_post"}}]
        for lato, y in [("north", 0), ("east", 90), ("south", 180), ("west", 270)]:
            parti.append({"when": {lato: "true"}, "apply": {"model": f"{MODID}:block/{id_}_side", "y": y, "uvlock": True}})
        scrivi_json(f"{A16}/blockstates/{id_}.json", {"multipart": parti})
        scrivi_json(f"{A16}/models/item/{id_}.json", {"parent": "minecraft:block/fence_inventory", "textures": {"texture": tex}})
    return f


def m_cancello(id_, tex):
    def f():
        for nome, parent in [("", "template_fence_gate"), ("_open", "template_fence_gate_open"),
                             ("_wall", "template_fence_gate_wall"), ("_wall_open", "template_fence_gate_wall_open")]:
            scrivi_json(f"{A16}/models/block/{id_}{nome}.json", {"parent": f"minecraft:block/{parent}", "textures": {"texture": tex}})
        varianti = {}
        for facing, y in [("south", 0), ("west", 90), ("north", 180), ("east", 270)]:
            for in_wall in ("false", "true"):
                for aperto in ("false", "true"):
                    suf = ("_wall" if in_wall == "true" else "") + ("_open" if aperto == "true" else "")
                    v = {"model": f"{MODID}:block/{id_}{suf}", "uvlock": True}
                    if y:
                        v["y"] = y
                    varianti[f"facing={facing},in_wall={in_wall},open={aperto}"] = v
        scrivi_json(f"{A16}/blockstates/{id_}.json", {"variants": varianti})
        scrivi_json(f"{A16}/models/item/{id_}.json", {"parent": f"{MODID}:block/{id_}"})
    return f


def m_tronco(id_, lato, cima):
    def f():
        scrivi_json(f"{A16}/models/block/{id_}.json", {"parent": "minecraft:block/cube_column", "textures": {"end": cima, "side": lato}})
        scrivi_json(f"{A16}/models/block/{id_}_horizontal.json", {"parent": "minecraft:block/cube_column_horizontal",
                    "textures": {"end": cima, "side": lato}})
        scrivi_json(f"{A16}/blockstates/{id_}.json", {"variants": {
            "axis=y": {"model": f"{MODID}:block/{id_}"},
            "axis=z": {"model": f"{MODID}:block/{id_}_horizontal", "x": 90},
            "axis=x": {"model": f"{MODID}:block/{id_}_horizontal", "x": 90, "y": 90}}})
        scrivi_json(f"{A16}/models/item/{id_}.json", {"parent": f"{MODID}:block/{id_}"})
    return f


TAG_TIPO = {"scale": ["minecraft:stairs"], "lastra": ["minecraft:slabs"], "muro": ["minecraft:walls"],
            "staccionata": ["minecraft:fences"], "cancello": ["minecraft:fence_gates"]}
for b in COSTRUZIONE:
    id_, tipo, tex = b["id"], b["tipo"], tex_di(b)
    legno = b["materiale"] == "legno"
    blocco(id_, b["nome_en"], b["nome_it"])
    if tipo == "scale":
        modelli_speciali[id_] = m_scale(id_, tex)
    elif tipo == "lastra":
        modelli_speciali[id_] = m_lastra(id_, tex, b["base"])
        loot_speciali[id_] = {"type": "minecraft:block", "pools": [{"rolls": 1.0, "bonus_rolls": 0.0, "entries": [
            {"type": "minecraft:item", "name": ft(id_), "functions": [
                {"function": "minecraft:set_count", "count": 2.0, "add": False, "conditions": [
                    {"condition": "minecraft:block_state_property", "block": ft(id_), "properties": {"type": "double"}}]},
                {"function": "minecraft:explosion_decay"}]}]}]}
    elif tipo == "muro":
        modelli_speciali[id_] = m_muro(id_, tex)
    elif tipo == "staccionata":
        modelli_speciali[id_] = m_staccionata(id_, tex)
    elif tipo == "cancello":
        modelli_speciali[id_] = m_cancello(id_, tex)
    elif tipo == "tronco":
        modelli_speciali[id_] = m_tronco(id_, tex, b["texture_cima"])
    tag(tag_blocchi, "minecraft:mineable/axe" if legno else "minecraft:mineable/pickaxe", ft(id_))
    for t in TAG_TIPO.get(tipo, []) + b.get("tag", []):
        tag(tag_blocchi, t, ft(id_))
        if t not in ("minecraft:walls",):
            tag(tag_oggetti, t, ft(id_))
    if tipo == "muro":
        tag(tag_oggetti, "minecraft:walls", ft(id_))
    if legno and tipo == "scale":
        tag(tag_blocchi, "minecraft:wooden_stairs", ft(id_))
        tag(tag_oggetti, "minecraft:wooden_stairs", ft(id_))
    if legno and tipo == "lastra":
        tag(tag_blocchi, "minecraft:wooden_slabs", ft(id_))
        tag(tag_oggetti, "minecraft:wooden_slabs", ft(id_))
    if b.get("drop"):
        loot_speciali[id_] = {"type": "minecraft:block", "pools": [{"rolls": 1.0, "bonus_rolls": 0.0, "entries": [
            {"type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "conditions": [SETA], "name": ft(id_)},
                {"type": "minecraft:item", "name": b["drop"], "functions": [{"function": "minecraft:explosion_decay"}]}]}]}]}
    # ricette
    for chiave, r in b.get("ricette", {}).items():
        nome = id_ if chiave == "crafting" else f"{id_}_{chiave}"
        if chiave.startswith("crafting"):
            if "senza_forma" in r:
                r_senza_forma(nome, r["senza_forma"], ft(id_), r.get("quanti", 1))
            else:
                chiavi = {k: v for k, v in r.items() if len(k) == 1}
                r_con_forma(nome, r["schema"], chiavi, ft(id_), r.get("quanti", 1))
        elif chiave == "stonecutter":
            ricette[f"{id_}_from_{r['da'].split(':')[1]}_stonecutting"] = {
                "type": "minecraft:stonecutting", "ingredient": ing(r["da"]),
                "result": {"count": r.get("quanti", 1), "id": ft(id_)}}
        elif chiave == "fornace":
            r_cottura(f"{id_}_from_cooking", r["da"], ft(id_), r.get("esperienza", 0.1), altoforno=False)
    # generazione nel mondo: vene come granito e diorite
    g = b.get("generazione")
    if g:
        worldgen[f"data/{MODID}/worldgen/configured_feature/rock_{id_}.json"] = {
            "type": "minecraft:ore", "config": {"discard_chance_on_air_exposure": 0.0, "size": g["dimensione"], "targets": [
                {"state": {"Name": ft(id_)}, "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"}},
                {"state": {"Name": ft(id_)}, "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"}}]}}
        worldgen[f"data/{MODID}/worldgen/placed_feature/rock_{id_}.json"] = {
            "feature": ft(f"rock_{id_}"), "placement": [
                {"type": "minecraft:count", "count": g["per_chunk"]}, {"type": "minecraft:in_square"},
                {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform",
                 "min_inclusive": {"absolute": g["y_min"]}, "max_inclusive": {"absolute": g["y_max"]}}},
                {"type": "minecraft:biome"}]}
        worldgen[f"data/{MODID}/neoforge/biome_modifier/rock_{id_}.json"] = {
            "type": "neoforge:add_features", "biomes": "#minecraft:is_overworld",
            "features": ft(f"rock_{id_}"), "step": "underground_ores"}
        # il pietrisco vale come il pietrisco normale
        VALORI_ESSENZA[(b.get("drop") or ft(id_))] = 1

# ===========================================================================
# FASE 16b: ricerca degli incantesimi (archivio + ricercatore + incantatore a scelta)
# ===========================================================================
blocco("enchant_researcher", "Enchantment Researcher", "Ricercatore di incantesimi")
modelli_speciali["enchant_researcher"] = modelli_macchina("enchant_researcher", "enchant_researcher_front",
                                                          "enchant_researcher_front_on", top="enchant_researcher_top")
tag(tag_blocchi, "minecraft:mineable/pickaxe", ft("enchant_researcher"))
oggetto("enchant_archive", "Enchantment Archive", "Archivio degli incantesimi")
for chiave, (en, it) in {
    "tooltip.futuretek.enchant_archive": ("Electronic list of the enchantments you know. Fill it in the Enchantment Researcher,\nthen put it in the Electric Enchanter to choose an exact enchantment",
                                          "Lista elettronica degli incantesimi che conosci. Si riempie nel Ricercatore di incantesimi,\npoi mettila nell'Incantatore elettrico per scegliere l'incantesimo esatto"),
    "tooltip.futuretek.enchant_archive.count": ("Known enchantments: %s", "Incantesimi conosciuti: %s"),
    "tooltip.futuretek.enchant_researcher": ("Slot 1: Enchantment Archive · slot 2: an enchanted book (or item) to read instantly\nWith energy and time it discovers new enchantments by itself (1 minute, 60,000 TU each)",
                                             "Casella 1: Archivio degli incantesimi · casella 2: un libro (o oggetto) incantato da leggere subito\nCon energia e tempo scopre da solo incantesimi nuovi (1 minuto e 60.000 TU l'uno)"),
    "gui.futuretek.enchant_researcher.status0": ("Put an Enchantment Archive in the first slot", "Metti un Archivio degli incantesimi nella prima casella"),
    "gui.futuretek.enchant_researcher.status1": ("Researching... %s%% · %2$s known", "Ricerca in corso... %s%% · %2$s conosciuti"),
    "gui.futuretek.enchant_researcher.status2": ("Not enough energy · %2$s known", "Energia insufficiente · %2$s conosciuti"),
    "gui.futuretek.enchant_researcher.status3": ("You know every enchantment! (%2$s)", "Conosci tutti gli incantesimi! (%2$s)"),
    "gui.futuretek.enchant_researcher.status4": ("Book read and saved · %2$s known", "Libro letto e salvato · %2$s conosciuti"),
    "gui.futuretek.enchanter_apply": ("Enchant · %s lv · %s lapis · %sk", "Incanta · %s liv · %s lapis · %sk"),
    "gui.futuretek.enchanter_no_archive": ("Archive in the 3rd slot = choose the enchantment", "Archivio nella 3ª casella = scegli l'incantesimo"),
    "gui.futuretek.enchanter_put_item": ("Put an item or a book", "Metti un oggetto o un libro"),
    "gui.futuretek.enchanter_nothing": ("No known enchantment fits this item", "Nessun incantesimo conosciuto va su questo oggetto"),
    "gui.futuretek.energy_k": ("%sk TU", "%sk TU"),
    "message.futuretek.enchanter_incompatible": ("Not compatible with the enchantments it already has (or already at that level)",
                                                 "Non va d'accordo con gli incantesimi che ha già (o ce l'ha già a quel livello)"),
}.items():
    lingua_en[chiave] = en
    lingua_it[chiave] = it
r_con_forma("enchant_archive", ["GRG", "PBP", "GCG"], {"G": "#c:nuggets/gold", "R": "#c:dusts/redstone", "P": "minecraft:glass_pane",
                                                      "B": "minecraft:book", "C": ft("advanced_circuit")}, ft("enchant_archive"))
r_con_forma("enchant_researcher", ["BCB", "LML", "BCB"], {"B": "minecraft:bookshelf", "C": ft("advanced_circuit"),
                                                         "L": "#c:storage_blocks/lapis", "M": M}, ft("enchant_researcher"))

# ===========================================================================
# FASE 17: rifugio portatile (il nucleo costruisce una casetta, rotto il nucleo sparisce)
# ===========================================================================
blocco("shelter_core", "Portable Shelter", "Rifugio portatile")
tag(tag_blocchi, "minecraft:mineable/pickaxe", ft("shelter_core"))
loot_speciali["shelter_core"] = {"type": "minecraft:block", "pools": []}  # cade da solo, con la carica
for _id, _en, _it in (("shelter_wall", "Shelter Wall", "Parete del rifugio"),
                      ("shelter_roof", "Shelter Roof", "Tetto del rifugio"),
                      ("shelter_window", "Shelter Window", "Finestra del rifugio")):
    blocco(_id, _en, _it)
    loot_speciali[_id] = {"type": "minecraft:block", "pools": []}


def modelli_finestra_rifugio():
    A_ = f"assets/{MODID}"
    scrivi_json(f"{A_}/blockstates/shelter_window.json", {"variants": {"": {"model": f"{MODID}:block/shelter_window"}}})
    scrivi_json(f"{A_}/models/block/shelter_window.json", {"parent": "minecraft:block/cube_all",
                                                           "render_type": "minecraft:translucent",
                                                           "textures": {"all": f"{MODID}:block/shelter_window"}})


def modelli_senza_oggetto(id_):
    def f():
        A_ = f"assets/{MODID}"
        scrivi_json(f"{A_}/blockstates/{id_}.json", {"variants": {"": {"model": f"{MODID}:block/{id_}"}}})
        scrivi_json(f"{A_}/models/block/{id_}.json",
                    {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MODID}:block/{id_}"}})
    return f


modelli_speciali["shelter_window"] = modelli_finestra_rifugio
modelli_speciali["shelter_wall"] = modelli_senza_oggetto("shelter_wall")
modelli_speciali["shelter_roof"] = modelli_senza_oggetto("shelter_roof")
for chiave, (en, it) in {
    "tooltip.futuretek.shelter_core": ("Place it on flat ground: it builds a small house around itself\n(bed, crafting table, furnace, door). Break the core and the house disappears\nRecharge it in the Accumulator Bench like an energy cell",
                                      "Mettilo su un terreno piatto: costruisce una casetta intorno a sé\n(letto, banco da lavoro, fornace, porta). Rompi il nucleo e la casa sparisce\nSi ricarica nel banco accumulatore come una cella di energia"),
    "tooltip.futuretek.shelter_core.uses": ("Houses left: %s / %s", "Case rimaste: %s / %s"),
    "message.futuretek.shelter_no_energy": ("Not enough energy: each house needs %s TU. Recharge it!",
                                           "Energia insufficiente: ogni casa costa %s TU. Ricaricalo!"),
    "message.futuretek.shelter_no_space": ("No room: it needs free flat ground 7x7, 4 blocks high",
                                          "Non c'è posto: serve un terreno libero e piatto 7x7, alto 4 blocchi"),
    "message.futuretek.shelter_in_the_way": ("Someone is standing where the walls go: move closer to the core",
                                            "Qualcuno è dove vanno i muri: avvicinati al nucleo"),
    "message.futuretek.shelter_built": ("Shelter ready! Houses left: %s", "Rifugio pronto! Case rimaste: %s"),
    "message.futuretek.shelter_part": ("It's part of the shelter: break the core to remove the house",
                                      "Fa parte del rifugio: rompi il nucleo per togliere la casa"),
}.items():
    lingua_en[chiave] = en
    lingua_it[chiave] = it
r_con_forma("shelter_core", ["PWP", "FCT", "PBP"], {"P": "#c:plates/iron", "W": "#minecraft:wool", "F": "minecraft:furnace",
                                                   "C": ft("advanced_circuit"), "T": "minecraft:crafting_table",
                                                   "B": ft("battery_cell")}, ft("shelter_core"))

# ---------------------------------------------------------------------------
# SCRITTURA DEI FILE
# ---------------------------------------------------------------------------
if GEN.exists():
    shutil.rmtree(GEN)

A = f"assets/{MODID}"
modelli_speciali_oggetti = globals().get("modelli_speciali_oggetti", {})
for id_, modello in oggetti:
    if id_ in modelli_speciali_oggetti:
        scrivi_json(f"{A}/models/item/{id_}.json", modelli_speciali_oggetti[id_])
        continue
    parent = "minecraft:item/handheld" if modello == "handheld" else "minecraft:item/generated"
    scrivi_json(f"{A}/models/item/{id_}.json", {"parent": parent, "textures": {"layer0": f"{MODID}:item/{id_}"}})
for id_ in blocchi:
    if id_ in modelli_speciali:
        modelli_speciali[id_]()
    else:
        scrivi_json(f"{A}/blockstates/{id_}.json", {"variants": {"": {"model": f"{MODID}:block/{id_}"}}})
        scrivi_json(f"{A}/models/block/{id_}.json",
                    {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MODID}:block/{id_}"}})
        scrivi_json(f"{A}/models/item/{id_}.json", {"parent": f"{MODID}:block/{id_}"})
    scrivi_json(f"data/{MODID}/loot_table/blocks/{id_}.json", loot_speciali.get(id_) or loot_minerali.get(id_) or {
        "type": "minecraft:block",
        "pools": [{
            "bonus_rolls": 0.0,
            "conditions": [{"condition": "minecraft:survives_explosion"}],
            "entries": [{"type": "minecraft:item", "name": ft(id_)}],
            "rolls": 1.0,
        }],
        "random_sequence": f"{MODID}:blocks/{id_}",
    })

scrivi_json(f"{A}/lang/en_us.json", dict(sorted(lingua_en.items())))
scrivi_json(f"{A}/lang/it_it.json", dict(sorted(lingua_it.items())))

for nome, valori in tag_oggetti.items():
    ns, percorso = nome.split(":")
    scrivi_json(f"data/{ns}/tags/item/{percorso}.json", {"values": valori})
for nome, valori in tag_blocchi.items():
    ns, percorso = nome.split(":")
    scrivi_json(f"data/{ns}/tags/block/{percorso}.json", {"values": valori})
for nome, dati in ricette.items():
    scrivi_json(f"data/{MODID}/recipe/{nome}.json", dati)
for percorso, dati in worldgen.items():
    scrivi_json(percorso, dati)
strutture_binarie[f"data/{MODID}/essence_values/base.json"] = (json.dumps(
    {"_info": "Valori dell'Essenza di FutureTek. Gli oggetti non elencati ricevono un valore calcolato dalle ricette. "
              "copies: l'oggetto a sinistra vale quanto quello a destra.",
     "values": VALORI_ESSENZA, "copies": COPIE_ESSENZA}, indent=2, ensure_ascii=False) + "\n").encode("utf-8")
for percorso, dati in strutture_binarie.items():
    (GEN / percorso).parent.mkdir(parents=True, exist_ok=True)
    (GEN / percorso).write_bytes(dati)

# Elenco di controllo (usato per verificare che Java e script siano allineati)
(ROOT / "tools" / "elenco_registro.txt").write_text(
    "\n".join(sorted([f"item {i}" for i, _ in oggetti] + [f"block {b}" for b in blocchi])) + "\n", encoding="utf-8")

# Con --forza-texture si ridisegna anche la grafica v2 (attrezzi in diagonale, macchine, minerali...)
if FORZA_TEXTURE:
    sys.path.insert(0, str(ROOT / "tools"))
    import grafica
    grafica.main()
    import grafica_oggetti   # attrezzi, armature e materiali in stile vanilla (disegnati da zero)
    grafica_oggetti.main()
    for _nome in ("grafica_materiali", "grafica_varie"):  # armature/materiali, poi tutti gli altri oggetti
        if (ROOT / "tools" / f"{_nome}.py").exists():
            __import__(_nome).main()
    import grafica_rifugio  # rifugio portatile
    grafica_rifugio.main()
    import blocchi_costruzione  # texture dei blocchi da costruzione
    blocchi_costruzione.costruisci(salva=True)

print(f"Oggetti: {len(oggetti)}  Blocchi: {len(blocchi)}  Ricette: {len(ricette)}  "
      f"Tag: {len(tag_oggetti) + len(tag_blocchi)}  Texture nuove: {texture_create}")
