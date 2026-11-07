"""
FutureTek - grafica v5 di ARMATURE e MATERIALI, nello stile degli oggetti di Minecraft:
contorno scuro, 3-4 toni del materiale, luce da in alto a sinistra, proporzioni "vanilla".
Tutte le sagome sono disegnate a mano qui sotto (griglie di lettere) o costruite con regole
semplici: nessuna texture di altri giochi o mod viene copiata.

Gira DOPO tools/grafica_oggetti.py (sovrascrive armature e materiali).
Uso:  python tools/grafica_materiali.py
"""
import random
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from grafica import mix, rampa  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/futuretek/textures"

# colore base di ogni materiale
COLORI = {
    "bronze": (198, 126, 62), "steel": (150, 160, 172), "nocturnite": (110, 70, 150), "ardorite": (204, 50, 56),
    "tin": (196, 206, 214), "lead": (104, 110, 148), "silver": (214, 224, 236), "uranium": (112, 214, 64),
    "iron": (206, 206, 206), "gold": (246, 204, 60), "copper": (214, 124, 80), "obsidian": (64, 46, 96),
    "diamond": (96, 224, 220), "coal": (62, 62, 68),
}
EXO = {"exo": ((70, 120, 160), (90, 230, 255)), "exo_mk1": ((176, 180, 188), (90, 230, 255)),
       "exo_mk3": ((60, 52, 68), (190, 110, 255))}


def toni(base):
    """o = contorno, 1..4 dal piu' scuro al piu' chiaro, h = riflesso"""
    r = rampa(base)
    return {"o": mix(r[0], (10, 8, 14), 0.5), "1": r[1], "2": r[2], "3": r[3], "4": r[4],
            "h": mix(r[5], (255, 255, 255), 0.4)}


def da_griglia(righe, colori, extra=None):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    colori = dict(colori, **(extra or {}))
    for y, riga in enumerate(righe):
        for x, ch in enumerate(riga):
            if ch in colori:
                img.putpixel((x, y), colori[ch] + (255,))
    return img


def contorna(celle):
    """Aggiunge 'o' intorno alle celle piene (dizionario (x,y)->lettera)"""
    out = dict(celle)
    for (x, y) in celle:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            q = (x + dx, y + dy)
            if 0 <= q[0] < 16 and 0 <= q[1] < 16 and q not in celle:
                out[q] = "o"
    return out


def da_celle(celle, colori):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for (x, y), ch in celle.items():
        img.putpixel((x, y), colori[ch] + (255,))
    return img


# ---------------------------------------------------------------------------
# MATERIALI
# ---------------------------------------------------------------------------
LINGOTTO = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "....ooooooooooo.",
    "...ohhh4444443oo",
    "..oh444444443o2o",
    "..o3333333333o1o",
    "..o3222222222o1o",
    "..o2222222222oo.",
    "..o1111111111o..",
    "..oooooooooooo..",
    "................",
    "................",
    "................",
]


def lingotto(base):
    """Barra in prospettiva: faccia sopra chiara, davanti media, fianco destro scuro"""
    return da_mappa(LINGOTTO, base)


def piastra(base):
    """Lastra quadrata smussata: luce in alto a sinistra, ombra in basso a destra, riflesso diagonale, 4 rivetti"""
    celle = {}
    x0, x1, y0, y1 = 3, 12, 3, 12
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if y == y0 or x == x0:
                t = "h" if (y == y0 and x < x1) or (x == x0 and y < y1) else "3"
            elif y == y1 or x == x1:
                t = "1"
            elif y == y1 - 1 or x == x1 - 1:
                t = "2"
            else:
                t = "4" if (x + y) in (x0 + y0 + 4, x0 + y0 + 5) else "3"
            celle[(x, y)] = t
    for (x, y) in ((x0 + 2, y0 + 2), (x1 - 2, y0 + 2), (x0 + 2, y1 - 2), (x1 - 2, y1 - 2)):
        celle[(x, y)] = "1"
    # spessore della lastra sotto e a destra
    for x in range(x0 + 1, x1 + 2):
        celle[(x, y1 + 1)] = "1"
    for y in range(y0 + 1, y1 + 2):
        celle[(x1 + 1, y)] = "1"
    return da_celle(contorna(celle), toni(base))


PEPITA = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "........oo......",
    ".......o4ho.....",
    "...oo..o43o.....",
    "..o4ho.o321ooo..",
    "..o431o.oo4h3o..",
    "...o21o..o4321o.",
    "....oo...o3221o.",
    ".....ooo..o11o..",
    "....o4h3o..oo...",
    "....o3221o......",
    ".....oooo.......",
]

POLVERE = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "........o.......",
    ".......o4o......",
    "......o4h3o.....",
    ".....o4h4c3o....",
    "....o44333c2o...",
    "...o4c3332221o..",
    "..o4433c222c11o.",
    ".o433332c221111o",
    ".oooooooooooooo.",
    "................",
]

GEMMA = [
    "................",
    "................",
    "................",
    ".....oooooo.....",
    "....ohh4433o....",
    "...oh444333o....",
    "..oooooooooooo..",
    "..o4hh44333221o.",
    "...o44433322o...",
    "....o433322o....",
    ".....o3322o.....",
    "......o21o......",
    ".......oo.......",
    "................",
    "................",
    "................",
]

SCHEGGIA = [
    "................",
    "..........oo....",
    ".........ohho...",
    "........oh43o...",
    ".......oh432o...",
    "......oh4321o...",
    ".....oh4321o....",
    "....oh4321o.....",
    "...o44321o..oo..",
    "...o4321o..oh3o.",
    "..o4321o..oh32o.",
    "..o321o..oh321o.",
    "..o21o...o3221o.",
    "...oo.....o11o..",
    "...........oo...",
    "................",
]

GREZZO = [
    "................",
    "................",
    "................",
    "......oooo......",
    "....oo4hh3oo....",
    "...o44h44c321o..",
    "..o4h44c33221o..",
    "..o44c3332221o..",
    ".o44333222c21o..",
    ".o4c332c22211o..",
    ".o3332222c11o...",
    "..o32c21111o....",
    "...oo1111oo.....",
    ".....oooo.......",
    "................",
    "................",
]

FRANTUMATO = [
    "................",
    "................",
    "................",
    "................",
    "........oo......",
    "...oo..o4ho.....",
    "..o4ho.o431o.oo.",
    "..o431o.oo..o4ho",
    "...oo..oo...o321",
    ".....oo4hoo..oo.",
    "....o4h4321o....",
    ".oo.o443221o.oo.",
    "o4ho.o3211o.o4ho",
    "o321o.oooo..o321",
    ".oo..........oo.",
    "................",
]


def filo(base):
    """Bobina: rotolo di filo avvolto su un rocchetto di legno"""
    legno = {"w": (110, 78, 44), "W": (150, 108, 62), "d": (70, 48, 26)}
    righe = [
        "................",
        "................",
        "....oooooooo....",
        "...oWWWWWWWWo...",
        "...oddddddddo...",
        "...o4h443321o...",
        "...o32221110o...",
        "...o4h443321o...",
        "...o32221110o...",
        "...o4h443321o...",
        "...o32221110o...",
        "...oddddddddo...",
        "...oWWWWWWWWo...",
        "....oooooooo....",
        "................",
        "................",
    ]
    c = toni(base)
    c["0"] = mix(c["1"], (0, 0, 0), 0.35)
    return da_griglia(righe, c, legno)


def da_mappa(righe, base, extra=None):
    c = toni(base)
    c["c"] = mix(c["2"], (0, 0, 0), 0.3)
    return da_griglia(righe, c, extra)


# ---------------------------------------------------------------------------
# ARMATURE (icone)
# ---------------------------------------------------------------------------
# Maschere: '#' metallo, 'd' interno scuro, '.' vuoto. Il contorno e la lavorazione del metallo si calcolano dopo.
ELMO = [
    "................",
    "................",
    "................",
    "....########....",
    "...##########...",
    "..############..",
    "..############..",
    "..####dddd####..",
    "..###dddddd###..",
    "..###dddddd###..",
    "..###d....d###..",
    "...##......##...",
    "................",
    "................",
    "................",
    "................",
]
CORAZZA = [
    "................",
    ".####......####.",
    ".#####dddd#####.",
    ".######dd######.",
    ".##############.",
    ".##############.",
    "..############..",
    "...##########...",
    "...##########...",
    "...##########...",
    "...##########...",
    "...##########...",
    "...##########...",
    "................",
    "................",
    "................",
]
GAMBALI = [
    "................",
    "...##########...",
    "...##########...",
    "...##########...",
    "...####dd####...",
    "...####dd####...",
    "...####..####...",
    "...####..####...",
    "...####..####...",
    "...####..####...",
    "...####..####...",
    "...####..####...",
    "...####..####...",
    "................",
    "................",
    "................",
]
STIVALI = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "...####..####...",
    "...####..####...",
    "...####..####...",
    "...####..####...",
    "...####..####...",
    "..#####..#####..",
    ".######..######.",
    ".######..######.",
    "................",
    "................",
    "................",
]
# Seconda sagoma, piu' massiccia (armature di livello alto e scure): corazza squadrata a scaglie,
# elmo tondo e chiuso con fessura per gli occhi, gambali spessi, stivali a punta
ELMO_PESANTE = [
    "................",
    "................",
    "................",
    ".....######.....",
    "....########....",
    "...##########...",
    "...##########...",
    "...###dddd###...",
    "...##dddddd##...",
    "...##dd##dd##...",
    "...###....###...",
    "....##....##....",
    "................",
    "................",
    "................",
    "................",
]
CORAZZA_PESANTE = [
    "................",
    ".#####....#####.",
    ".######dd######.",
    ".##############.",
    ".##############.",
    ".##############.",
    "..############..",
    "..############..",
    "..############..",
    "...##########...",
    "...##########...",
    "...##########...",
    "....########....",
    "................",
    "................",
    "................",
]
GAMBALI_PESANTI = [
    "................",
    "..############..",
    "..############..",
    "..############..",
    "..#####dd#####..",
    "..#####..#####..",
    "..#####..#####..",
    "..#####..#####..",
    "..#####..#####..",
    "..#####..#####..",
    "..#####..#####..",
    "..#####..#####..",
    "..#####..#####..",
    "................",
    "................",
    "................",
]
STIVALI_PESANTI = [
    "................",
    "................",
    "................",
    "................",
    "....##....##....",
    "...###....###...",
    "...###....###...",
    "...####..####...",
    "...####..####...",
    "..#####..#####..",
    "..#####..#####..",
    ".######..######.",
    ".######..######.",
    "................",
    "................",
    "................",
]
PESANTI = {"nocturnite", "ardorite", "exo_mk3"}

# riga della striscia luminosa delle exo (o giuntura nelle altre) per ogni pezzo
STRISCIA = {"helmet": 6, "chestplate": 6, "leggings": 2, "boots": 8}


def armatura_icona(righe, base, luce, pezzo="chestplate", scaglie=False):
    """Icona in stile vanilla: contorno scuro marcato, metallo 'lavorato' (riflessi e macchie), interno nero"""
    c = toni(base)
    rng = random.Random(f"{pezzo}{base}")
    metal = {(x, y) for y, r in enumerate(righe) for x, ch in enumerate(r) if ch == "#"}
    dark = {(x, y) for y, r in enumerate(righe) for x, ch in enumerate(r) if ch == "d"}
    piene = metal | dark
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    # contorno
    for (x, y) in piene:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            q = (x + dx, y + dy)
            if 0 <= q[0] < 16 and 0 <= q[1] < 16 and q not in piene:
                img.putpixel(q, c["o"] + (255,))
    xs = [x for x, _ in metal]
    ys = [y for _, y in metal]
    x0, x1, y0, y1 = min(xs), max(xs), min(ys), max(ys)
    stripe = STRISCIA.get(pezzo)
    for (x, y) in metal:
        # luce da in alto a sinistra: valore 0..1
        t = 1 - ((x - x0) / max(1, x1 - x0) * 0.55 + (y - y0) / max(1, y1 - y0) * 0.45)
        # bordi: lato alto/sinistro chiaro, basso/destro scuro (come una piastra battuta)
        edge_hi = (x - 1, y) not in metal or (x, y - 1) not in metal
        edge_lo = (x + 1, y) not in metal or (x, y + 1) not in metal
        if edge_hi and not edge_lo:
            t += 0.25
        if edge_lo:
            t -= 0.3
        # scaglie (sagoma pesante): righe a mattoncini con il bordo basso scuro
        if scaglie and pezzo in ("chestplate", "leggings") and (y - y0) % 3 == 2 and (x + (y // 3)) % 2 == 0:
            t -= 0.35
        # lavorazione: macchie chiare/scure fisse
        r = rng.random()
        if r < 0.07:
            t += 0.3
        elif r < 0.13:
            t -= 0.25
        tono = "1" if t < 0.2 else "2" if t < 0.45 else "3" if t < 0.75 else "4" if t < 1.0 else "h"
        col = c[tono]
        if stripe is not None and y == stripe and not edge_lo:
            col = luce if luce else c["2"]
        img.putpixel((x, y), col + (255,))
    # interno scuro (con un filo di riflesso in alto)
    for (x, y) in dark:
        top = (x, y - 1) in metal
        col = (34, 30, 38) if top else (18, 16, 22)
        img.putpixel((x, y), col + (255,))
    return img


# ---------------------------------------------------------------------------
# ARMATURE INDOSSATE (64x32, disposizione UV standard di un modello umanoide)
# ---------------------------------------------------------------------------
def facce(u, v, w, h, d):
    """rettangoli (x, y, larghezza, altezza, nome) delle 6 facce di un cubo nella texture"""
    return [(u + d, v, w, d, "top"), (u + d + w, v, w, d, "bottom"), (u, v + d, d, h, "right"),
            (u + d, v + d, w, h, "front"), (u + d + w, v + d, d, h, "left"), (u + 2 * d + w, v + d, w, h, "back")]


def piastra_faccia(img, rect, c, rng, righe=None, giunture=4, luce=None, striscia=None):
    """Riempie una faccia con una piastra: bordo scuro, luce in alto a sinistra, grana leggera, giunture"""
    x0, y0, w, h, nome = rect
    for yy in range(h):
        if righe is not None and yy not in righe:
            continue
        for xx in range(w):
            if nome in ("top", "bottom"):
                t = "3" if (xx + yy) % 5 else "4"
            else:
                t = "3"
                if yy % giunture == 0 and yy > 0:
                    t = "2"
                if xx == 0 or yy == 0:
                    t = "4"
                if xx == w - 1 or yy == h - 1:
                    t = "1"
            col = c[t]
            r = rng.random()
            if t == "3" and r < 0.10:
                col = mix(col, c["4"], 0.5)   # riflesso del metallo battuto
            elif t == "3" and r < 0.18:
                col = mix(col, c["2"], 0.6)   # ammaccatura
            if striscia is not None and yy == striscia and nome in ("front", "back", "left", "right") and luce:
                col = luce
            img.putpixel((x0 + xx, y0 + yy), col + (255,))


def armatura_indossata(base, strato, luce=None):
    c = toni(base)
    rng = random.Random(f"{base}{strato}")
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    if strato == 1:
        # elmo: tutte le facce della testa; sul davanti resta aperto il viso
        for rect in facce(0, 0, 8, 8, 8):
            piastra_faccia(img, rect, c, rng, giunture=8, luce=luce, striscia=3 if luce else None)
        for yy in range(12, 16):           # apertura del viso (davanti, 2 righe sotto la fronte)
            for xx in range(10, 14):
                img.putpixel((xx, yy), (0, 0, 0, 0))
        for xx in range(9, 15):
            img.putpixel((xx, 11), c["1"] + (255,))
        # corazza: busto e braccia (spalline)
        for rect in facce(16, 16, 8, 12, 4):
            piastra_faccia(img, rect, c, rng, giunture=4, luce=luce, striscia=4 if luce else None)
        for rect in facce(40, 16, 4, 12, 4):
            piastra_faccia(img, rect, c, rng, righe=range(0, 8), giunture=4)
        # stivali: parte bassa delle gambe
        for rect in facce(0, 16, 4, 12, 4):
            x0, y0, w, h, nome = rect
            if nome in ("top",):
                continue
            righe = range(0, h) if nome == "bottom" else range(h - 5, h)
            piastra_faccia(img, rect, c, rng, righe=righe, giunture=6, luce=luce,
                           striscia=(h - 4) if (luce and nome != "bottom") else None)
    else:
        # gambali: cintura sul busto e gambe fino al ginocchio basso
        for rect in facce(16, 16, 8, 12, 4):
            x0, y0, w, h, nome = rect
            if nome in ("top", "bottom"):
                continue
            piastra_faccia(img, rect, c, rng, righe=range(h - 4, h), giunture=3, luce=luce,
                           striscia=(h - 3) if luce else None)
        for rect in facce(0, 16, 4, 12, 4):
            x0, y0, w, h, nome = rect
            if nome == "bottom":
                continue
            piastra_faccia(img, rect, c, rng, righe=range(0, 10) if nome != "top" else range(0, 4), giunture=4)
    return img


# ---------------------------------------------------------------------------
def salva(img, rel):
    p = TEX / rel
    if p.parent.exists():
        img.save(p)
        return 1
    return 0


def esiste(rel):
    return (TEX / rel).exists()


def main():
    n = 0
    # armature
    leggeri = {"helmet": ELMO, "chestplate": CORAZZA, "leggings": GAMBALI, "boots": STIVALI}
    pesanti = {"helmet": ELMO_PESANTE, "chestplate": CORAZZA_PESANTE, "leggings": GAMBALI_PESANTI,
               "boots": STIVALI_PESANTI}
    for mat in ("bronze", "steel", "nocturnite", "ardorite"):
        pezzi = pesanti if mat in PESANTI else leggeri
        for pezzo, righe in pezzi.items():
            if esiste(f"item/{mat}_{pezzo}.png"):
                n += salva(armatura_icona(righe, COLORI[mat], None, pezzo, mat in PESANTI), f"item/{mat}_{pezzo}.png")
        for s in (1, 2):
            n += salva(armatura_indossata(COLORI[mat], s), f"models/armor/{mat}_layer_{s}.png")
    for nome, (base, luce) in EXO.items():
        pezzi = pesanti if nome in PESANTI else leggeri
        for pezzo, righe in pezzi.items():
            if esiste(f"item/{nome}_{pezzo}.png"):
                n += salva(armatura_icona(righe, base, luce, pezzo, nome in PESANTI), f"item/{nome}_{pezzo}.png")
        for s in (1, 2):
            n += salva(armatura_indossata(base, s, luce), f"models/armor/{nome}_layer_{s}.png")
    # materiali
    for mat, base in COLORI.items():
        for rel, fn in [(f"item/{mat}_ingot.png", lambda b: lingotto(b)),
                        (f"item/{mat}_plate.png", piastra),
                        (f"item/{mat}_nugget.png", lambda b: da_mappa(PEPITA, b)),
                        (f"item/{mat}_dust.png", lambda b: da_mappa(POLVERE, b)),
                        (f"item/{mat}_wire.png", filo),
                        (f"item/raw_{mat}.png", lambda b: da_mappa(GREZZO, b)),
                        (f"item/purified_crushed_{mat}.png", lambda b: da_mappa(FRANTUMATO, b))]:
            if esiste(rel):
                n += salva(fn(base), rel)
    for i, base in enumerate([(90, 200, 90), (80, 130, 230), (80, 220, 220)], start=1):
        if esiste(f"item/scanning_dust_{i}.png"):
            n += salva(da_mappa(POLVERE, base), f"item/scanning_dust_{i}.png")
    n += salva(da_mappa(GEMMA, COLORI["nocturnite"]), "item/nocturnite.png")
    n += salva(da_mappa(GEMMA, COLORI["ardorite"]), "item/ardorite.png")
    n += salva(da_mappa(SCHEGGIA, (150, 110, 240)), "item/voltite_shard.png")
    print(f"Grafica materiali v5: {n} texture")
    return n


if __name__ == "__main__":
    main()
