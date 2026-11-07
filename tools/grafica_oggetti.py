"""
FutureTek - Grafica degli OGGETTI in stile Minecraft (v3)
=========================================================
Attrezzi, armi, armature e materiali con LE STESSE FORME degli oggetti di Minecraft:
un piccone FutureTek ha la sagoma di un piccone normale e cambia solo il colore (la tavolozza del materiale).

Come funziona:
  * ogni sagoma e' un disegno 16x16 fatto a mano qui sotto (lettere: M = metallo, W = legno, G = guardia, ...);
  * l'ombreggiatura e' automatica: luce da in alto a sinistra, bordo chiaro in alto, scuro in basso a destra;
  * il contorno scuro di 1 pixel viene aggiunto da solo (marrone attorno al legno, metallo scuro attorno al resto).
Tutti i disegni sono ORIGINALI (fatti qui a mano seguendo le convenzioni di Minecraft:
manico in diagonale dal basso a sinistra, testa in alto a destra), nessuna texture viene copiata.

Uso (dalla cartella del progetto):  python tools/grafica_oggetti.py
Va lanciato DOPO tools/grafica.py: sovrascrive solo le texture degli oggetti elencate in main().
"""
import random
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from grafica import TEX, mix, rampa  # noqa: E402

# ---------------------------------------------------------------------------
# Tavolozze
# ---------------------------------------------------------------------------
LEGNO = [(52, 34, 18), (84, 56, 30), (112, 78, 42), (140, 100, 56), (166, 124, 72)]
CONTORNO_LEGNO = (40, 26, 14)

# colore base di ogni materiale (la rampa va da scuro a chiaro)
MATERIALI = {
    "bronze": (198, 126, 62),
    "steel": (150, 160, 172),
    "nocturnite": (96, 62, 132),
    "ardorite": (200, 46, 54),
    "tin": (196, 206, 214),
    "lead": (100, 106, 142),
    "silver": (214, 224, 236),
    "uranium": (112, 214, 64),
    "iron": (202, 202, 202),
    "gold": (242, 200, 56),
    "copper": (212, 122, 78),
    "obsidian": (60, 44, 88),
    "diamond": (96, 224, 220),
    "coal": (58, 58, 64),
}
# legno dei manici: gli attrezzi "magici" hanno un manico scuro con riflessi (come le altre parti della mod)
MANICI = {
    "nocturnite": [(24, 18, 34), (40, 30, 56), (58, 44, 80), (78, 62, 106), (98, 84, 130)],
    "ardorite": [(30, 16, 14), (52, 28, 24), (74, 40, 34), (96, 56, 44), (120, 72, 56)],
}


def tavolozza(base):
    """5 toni dal piu' scuro al piu' chiaro + colore del contorno"""
    r = rampa(base)
    return [r[1], r[2], r[3], r[4], r[5]], mix(r[0], (16, 10, 22), 0.45)


# ---------------------------------------------------------------------------
# Disegni (16x16). Lettere:
#   M metallo  W legno (manico)  G guardia/rifinitura (metallo piu' scuro)  L luce (riflesso del metallo)
#   . vuoto
# ---------------------------------------------------------------------------
PICCONE = [
    "................",
    "....MMMMMM......",
    "..MMMMMMMMMM....",
    ".MMM.....MMMM...",
    ".MM.......WMMM..",
    "..........W.MMM.",
    ".........W...MM.",
    "........W....MM.",
    ".......W.....MM.",
    "......W.......M.",
    ".....W..........",
    "....W...........",
    "...W............",
    "..W.............",
    ".W..............",
    "................",
]
ASCIA = [
    "................",
    ".....MM.........",
    "....MMMMM..M....",
    "...MMMMMMMMMM...",
    "...MMMMMMMWMM...",
    "...MMMMMMW.M....",
    "....MMMMW.......",
    ".....MMW........",
    "......W.........",
    ".....W..........",
    "....W...........",
    "...W............",
    "..W.............",
    ".W..............",
    "................",
    "................",
]
PALA = [
    "................",
    "...........MMM..",
    "..........MMMMM.",
    ".........MMMMMM.",
    "........MMMMMMM.",
    ".........MMMMM..",
    "........W.MMM...",
    ".......W........",
    "......W.........",
    ".....W..........",
    "....W...........",
    "...W............",
    "..W.............",
    ".W..............",
    "................",
    "................",
]
ZAPPA = [
    "................",
    ".......MMMMM....",
    "......MMMMMMM...",
    "......MM...W....",
    "..........W.....",
    ".........W......",
    "........W.......",
    ".......W........",
    "......W.........",
    ".....W..........",
    "....W...........",
    "...W............",
    "..W.............",
    ".W..............",
    "................",
    "................",
]
SPADA = [
    "................",
    ".............MM.",
    "............MMM.",
    "...........MMM..",
    "..........MMM...",
    ".........MMM....",
    "........MMM.....",
    ".......MMM......",
    "......MMM.......",
    "..G..MMM........",
    "...GMMM.........",
    "....GM..........",
    "...W.G..........",
    "..W...G.........",
    ".G..............",
    "................",
]
def _martello():
    """Testa a blocco PERPENDICOLARE al manico (fascia "\\" attorno a (10,5)), manico "/" sotto"""
    righe = []
    for y in range(16):
        riga = ""
        for x in range(16):
            testa = abs((x - y) - 5) <= 1.6 and abs((x + y) - 15) <= 6.5
            manico = (x + y) == 14 and 1 <= x <= 8
            riga += "G" if testa and abs((x + y) - 15) <= 0.5 else "M" if testa else "W" if manico else "."
        righe.append(riga)
    return righe


MARTELLO = _martello()

ELMO = [
    "................",
    "................",
    "................",
    "....MMMMMMMM....",
    "...MMMMMMMMMM...",
    "...MMMMMMMMMM...",
    "...MMM....MMM...",
    "...MMM....MMM...",
    "...MM......MM...",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
]
CORAZZA = [
    "................",
    "..MMMM....MMMM..",
    ".MMMMMMMMMMMMMM.",
    ".MMMMMMMMMMMMMM.",
    ".MMMMMMMMMMMMMM.",
    "..MMMMMMMMMMMM..",
    "...MMMMMMMMMM...",
    "...MMMMMMMMMM...",
    "...MMMMMMMMMM...",
    "...MMMMMMMMMM...",
    "...MMMMMMMMMM...",
    "...MMMMMMMMMM...",
    "...MMMMMMMMMM...",
    "................",
    "................",
    "................",
]
GAMBALI = [
    "................",
    "...MMMMMMMMMM...",
    "...MMMMMMMMMM...",
    "...MMMMMMMMMM...",
    "...MMMM..MMMM...",
    "...MMM....MMM...",
    "...MMM....MMM...",
    "...MMM....MMM...",
    "...MMM....MMM...",
    "...MMM....MMM...",
    "...MMM....MMM...",
    "...MMM....MMM...",
    "...MMM....MMM...",
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
    "................",
    "...MMM....MMM...",
    "...MMM....MMM...",
    "...MMM....MMM...",
    "...MMM....MMM...",
    "..MMMM....MMMM..",
    ".MMMMM....MMMMM.",
    ".MMMMM....MMMMM.",
    "................",
    "................",
    "................",
]
ARMATURE = {"helmet": ELMO, "chestplate": CORAZZA, "leggings": GAMBALI, "boots": STIVALI}

# lingotto: disegnato direttamente coi toni (5 = faccia di sopra chiara ... 1 = fianco in ombra)
LINGOTTO = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "........55554...",
    "......554444433.",
    "....5544444433..",
    "...444444443322.",
    "..33333333322...",
    "..2222222221....",
    "...11111111.....",
    "................",
    "................",
    "................",
    "................",
]
PEPITA = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    ".........MM.....",
    "........MMMM....",
    ".....MM.MMMM....",
    "....MMMM.MM.....",
    "....MMMM........",
    ".....MM..MMM....",
    ".........MMM....",
    "................",
    "................",
    "................",
]
GEMMA = [
    "................",
    "................",
    "................",
    ".....MMMMMM.....",
    "....MMMMMMMM....",
    "...MMMMMMMMMM...",
    "...MMMMMMMMMM...",
    "....MMMMMMMM....",
    ".....MMMMMM.....",
    "......MMMM......",
    ".......MM.......",
    "................",
    "................",
    "................",
    "................",
    "................",
]
SCHEGGIA = [
    "................",
    "................",
    "...........MM...",
    "..........MMMM..",
    ".........MMMMM..",
    "........MMMMM...",
    ".......MMMMM....",
    "......MMMMM.....",
    ".....MMMMM......",
    "....MMMMM.......",
    "...MMMMM........",
    "..MMMMM.........",
    "..MMMM..........",
    "...MM...........",
    "................",
    "................",
]
POLVERE = [
    "................",
    "................",
    "................",
    "................",
    "................",
    ".......MM.......",
    "......MMMM......",
    ".....MMMMMM.....",
    "....MMMMMMMM....",
    "...MMMMMMMMMM...",
    "..MMMMMMMMMMMM..",
    "..MMMMMMMMMMMM..",
    "................",
    "................",
    "................",
    "................",
]
PIASTRA = [
    "................",
    "................",
    "................",
    "................",
    "........MMM.....",
    "......MMMMMMM...",
    "....MMMMMMMMMMM.",
    "..MMMMMMMMMMMMM.",
    ".MMMMMMMMMMMM...",
    ".MMMMMMMMMM.....",
    "...MMMMMM.......",
    ".....MM.........",
    "................",
    "................",
    "................",
    "................",
]
FILO = [
    "................",
    "................",
    "....MMMMMMM.....",
    "...MM.....MM....",
    "..MM.MMMMM.MM...",
    "..M.MM...MM.M...",
    "..M.M.....M.M...",
    "..M.M.....M.M...",
    "..M.MM...MM.M...",
    "..MM.MMMMM.MM...",
    "...MM.....MM....",
    "....MMMMMMM.WW..",
    "............WW..",
    "................",
    "................",
    "................",
]
GREZZO = [
    "................",
    "................",
    "................",
    "......MMMM......",
    "....MMMMMMMM....",
    "...MMMMMMMMMM...",
    "..MMMMMMMMMMM...",
    "..MMMMMMMMMMMM..",
    "...MMMMMMMMMMM..",
    "...MMMMMMMMMM...",
    "....MMMMMMMM....",
    "......MMMM......",
    "................",
    "................",
    "................",
    "................",
]
FRANTUMATO = [
    "................",
    "................",
    "................",
    "................",
    ".....MMM........",
    "....MMMMM.MMM...",
    "....MMMMMMMMMM..",
    "...MMMMMMMMMMM..",
    "..MMMMMM.MMMMM..",
    "..MMMMM...MMM...",
    "...MMM..MMM.....",
    "........MMMM....",
    ".........MM.....",
    "................",
    "................",
    "................",
]


# ---------------------------------------------------------------------------
# Disegno e ombreggiatura
# ---------------------------------------------------------------------------
def celle(disegno, lettere):
    return {(x, y) for y, riga in enumerate(disegno) for x, ch in enumerate(riga) if ch in lettere}


def put(img, x, y, c):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), tuple(c[:3]) + (255,))


def tono_bevel(x, y, m):
    """0..4: chiaro dove il pixel guarda in alto/sinistra, scuro dove guarda in basso/destra"""
    su = (x, y - 1) in m
    sx = (x - 1, y) in m
    giu = (x, y + 1) in m
    dx = (x + 1, y) in m
    diag_su = (x - 1, y - 1) in m
    if not su and not sx:
        return 4
    if not su or not sx:
        return 3 if diag_su or not su else 3
    if not giu and not dx:
        return 0
    if not giu or not dx:
        return 1
    return 2


def ombreggia(img, m, toni, rumore=0.0, seme="x"):
    rng = random.Random(seme)
    for (x, y) in m:
        t = tono_bevel(x, y, m)
        if rumore and t == 2 and rng.random() < rumore:
            t = 3 if rng.random() < 0.5 else 1
        put(img, x, y, toni[t])


def contorno(img, colore_di, pieni=None):
    """Contorno di 1 pixel: colore_di(x, y) sceglie il colore guardando il pixel pieno vicino"""
    if pieni is None:
        pieni = {(x, y) for x in range(img.width) for y in range(img.height) if img.getpixel((x, y))[3] > 0}
    nuovi = {}
    for (x, y) in pieni:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            p = (x + dx, y + dy)
            if 0 <= p[0] < img.width and 0 <= p[1] < img.height and p not in pieni and p not in nuovi:
                nuovi[p] = colore_di(x, y)
    for (x, y), c in nuovi.items():
        put(img, x, y, c)
    return img


def disegna_attrezzo(disegno, materiale, nome):
    """Testa di metallo + manico di legno, ciascuno ombreggiato a parte, e contorno del colore giusto"""
    toni, scuro = tavolozza(MATERIALI[materiale])
    legno = MANICI.get(materiale, LEGNO)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    manico = celle(disegno, "W")
    metallo = celle(disegno, "M")
    guardia = celle(disegno, "G")
    # manico: a scaletta, due toni alternati come il legno di Minecraft
    for (x, y) in manico:
        put(img, x, y, legno[3] if (x + y) % 2 == 0 and x % 2 == 0 else legno[2])
    ombreggia(img, metallo, toni, seme=nome)
    # riflesso: una linea chiara lungo il bordo alto della testa
    for (x, y) in metallo:
        if (x, y - 1) not in metallo and (x - 1, y - 1) not in metallo and (x + 1, y) in metallo:
            put(img, x, y, mix(toni[4], (255, 255, 255), 0.35))
    g = [mix(c, (40, 30, 34), 0.35) for c in toni]
    for (x, y) in guardia:
        put(img, x, y, g[3] if (x - 1, y) not in guardia or (x, y - 1) not in guardia else g[1])
    pieni = manico | metallo | guardia

    def col(x, y):
        return CONTORNO_LEGNO if (x, y) in manico else scuro

    return contorno(img, col, pieni)


def disegna_semplice(disegno, base, nome, rumore=0.0, macchie=None):
    toni, scuro = tavolozza(base)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    m = celle(disegno, "MW")
    ombreggia(img, m, toni, rumore=rumore, seme=nome)
    if macchie:
        rng = random.Random(nome + "m")
        for (x, y) in m:
            if tono_bevel(x, y, m) == 2 and rng.random() < macchie[0]:
                put(img, x, y, macchie[1][rng.randrange(len(macchie[1]))])
    return contorno(img, lambda x, y: scuro, m)


# ---------------------------------------------------------------------------
# Attrezzi
# ---------------------------------------------------------------------------
ATTREZZI = {"pickaxe": PICCONE, "axe": ASCIA, "shovel": PALA, "hoe": ZAPPA, "sword": SPADA, "hammer": MARTELLO}


def attrezzo(tipo, materiale):
    """v4: sagome con le proporzioni degli attrezzi di Minecraft (vedi SAGOME_V4 piu' sotto)"""
    return disegna_attrezzo_v4(tipo, materiale)


# ---------------------------------------------------------------------------
# Armature
# ---------------------------------------------------------------------------
def armatura(tipo, base, luce=None, nome="a"):
    toni, scuro = tavolozza(base)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    m = celle(ARMATURE[tipo], "M")
    ombreggia(img, m, toni, seme=nome)
    # dettagli in stile Minecraft: cuciture/bordi interni
    if tipo == "chestplate":
        for y in range(5, 12):
            put(img, 7, y, toni[1])
            put(img, 8, y, toni[3])
        for x in (2, 13):
            put(img, x, 3, toni[1])
    elif tipo == "helmet":
        for x in range(4, 12):
            put(img, x, 5, toni[1] if x in (4, 11) else toni[2])
    elif tipo == "leggings":
        for x in range(4, 12):
            put(img, x, 3, toni[1])
    elif tipo == "boots":
        put(img, 4, 10, toni[1])
        put(img, 11, 10, toni[1])
    if luce:
        riga = {"helmet": 4, "chestplate": 7, "leggings": 2, "boots": 9}[tipo]
        for (x, y) in m:
            if y == riga and tono_bevel(x, y, m) >= 2:
                put(img, x, y, luce)
    return contorno(img, lambda x, y: scuro, m)


def armatura_indossata(base, strato, luce=None):
    """Texture 64x32 indossata (stessa disposizione di quelle di Minecraft): piastre con bordo chiaro e scuro"""
    toni, scuro = tavolozza(base)
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    rng = random.Random(f"{base}{strato}")
    # rettangoli delle facce dei pezzi (x, y, larghezza, altezza) nella disposizione "umanoide" 64x32
    if strato == 1:
        pezzi = [
            (0, 8, 8, 8), (8, 8, 8, 8), (16, 8, 8, 8), (24, 8, 8, 8), (8, 0, 8, 8), (16, 0, 8, 8),  # testa
            (16, 20, 4, 12), (20, 20, 8, 12), (28, 20, 4, 12), (32, 20, 8, 12), (20, 16, 8, 4), (28, 16, 8, 4),  # corpo
            (40, 20, 4, 12), (44, 20, 4, 12), (48, 20, 4, 12), (52, 20, 4, 12), (44, 16, 4, 4), (48, 16, 4, 4),  # braccio
            (0, 20, 4, 12), (4, 20, 4, 12), (8, 20, 4, 12), (12, 20, 4, 12), (4, 16, 4, 4), (8, 16, 4, 4),  # stivali
        ]
    else:
        pezzi = [
            (16, 20, 4, 12), (20, 20, 8, 12), (28, 20, 4, 12), (32, 20, 8, 12), (20, 16, 8, 4), (28, 16, 8, 4),  # cintura
            (0, 20, 4, 12), (4, 20, 4, 12), (8, 20, 4, 12), (12, 20, 4, 12), (4, 16, 4, 4), (8, 16, 4, 4),  # gambe
        ]
    for (x0, y0, w, h) in pezzi:
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                bordo_su = y == y0
                bordo_sx = x == x0
                bordo_giu = y == y0 + h - 1
                bordo_dx = x == x0 + w - 1
                if bordo_su or bordo_sx:
                    c = toni[3]
                elif bordo_giu or bordo_dx:
                    c = toni[0]
                else:
                    c = toni[2] if rng.random() > 0.12 else toni[1 if rng.random() < 0.5 else 3]
                # piastre: una fascia scura ogni 4 righe (giunture)
                if not (bordo_su or bordo_giu) and h >= 8 and (y - y0) % 4 == 0 and w > 2:
                    c = toni[1]
                img.putpixel((x, y), c + (255,))
        if luce and h >= 8:
            yl = y0 + h // 2
            for x in range(x0 + 1, x0 + w - 1):
                img.putpixel((x, yl), luce + (255,))
    # elmo: visiera scura sulla faccia davanti
    if strato == 1:
        for x in range(9, 15):
            for y in (12, 13):
                img.putpixel((x, y), (toni[0] if not luce else luce) + (255,))
    return img


# ---------------------------------------------------------------------------
# Materiali
# ---------------------------------------------------------------------------
def lingotto(base, nome):
    toni, scuro = tavolozza(base)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    m = set()
    for y, riga in enumerate(LINGOTTO):
        for x, ch in enumerate(riga):
            if ch.isdigit():
                put(img, x, y, toni[int(ch) - 1])
                m.add((x, y))
    return contorno(img, lambda x, y: scuro, m)


def pepita(base, nome):
    return disegna_semplice(PEPITA, base, nome)


def gemma(base, nome):
    toni, scuro = tavolozza(base)
    img = disegna_semplice(GEMMA, base, nome)
    # sfaccettature
    for (x, y) in [(5, 4), (6, 4), (5, 5), (4, 5)]:
        put(img, x, y, mix(toni[4], (255, 255, 255), 0.5))
    for x in range(4, 12):
        put(img, x, 6, toni[3])
    for (x, y) in [(7, 7), (8, 8), (7, 9)]:
        put(img, x, y, toni[1])
    return img


def scheggia(base, nome):
    toni, scuro = tavolozza(base)
    img = disegna_semplice(SCHEGGIA, base, nome)
    m = celle(SCHEGGIA, "M")
    for (x, y) in m:
        if x + y == 15:
            put(img, x, y, mix(toni[4], (255, 255, 255), 0.4))
    return img


def polvere(base, nome):
    toni, scuro = tavolozza(base)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    m = celle(POLVERE, "M")
    rng = random.Random(nome)
    for (x, y) in m:
        t = tono_bevel(x, y, m)
        if t == 2:
            t = rng.choice([1, 2, 2, 3, 3, 4])
        put(img, x, y, toni[t])
    return contorno(img, lambda x, y: scuro, m)


def piastra(base, nome):
    toni, scuro = tavolozza(base)
    img = disegna_semplice(PIASTRA, base, nome)
    m = celle(PIASTRA, "M")
    for (x, y) in m:
        if y == 7 and 4 <= x <= 10:
            put(img, x, y, toni[4])
        if y == 9 and 3 <= x <= 8:
            put(img, x, y, toni[1])
    return img


def filo(base, nome):
    toni, scuro = tavolozza(base)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    m = celle(FILO, "M")
    ombreggia(img, m, toni, seme=nome)
    for (x, y) in celle(FILO, "W"):
        put(img, x, y, toni[1])
    return contorno(img, lambda x, y: scuro, m | celle(FILO, "W"))


def grezzo(base, nome):
    pietra = [(70, 68, 66), (96, 92, 88), (120, 116, 110)]
    return disegna_semplice(GREZZO, base, nome, macchie=(0.22, pietra))


def frantumato(base, nome):
    return disegna_semplice(FRANTUMATO, base, nome, rumore=0.3)


# ---------------------------------------------------------------------------
# Scrittura
# ---------------------------------------------------------------------------
# ---------------------------------------------------------------------------
# ATTREZZI v4: stesse proporzioni degli attrezzi di Minecraft (disegno nostro)
#  - manico largo 2 pixel lungo la diagonale (lato in alto a sinistra medio, centro chiaro) + contorno scuro
#  - testa SPESSA (2-3 pixel di materiale) con contorno scuro tutto attorno, punte corte e tozze
#  - 4 toni di materiale + contorno, tanto contrasto, luce da in alto a sinistra
# Le sagome sono generate da forme geometriche (archi, ellissi, bande diagonali) definite qui sotto.
# ---------------------------------------------------------------------------
import math as _math

LEGNO_V4 = {"k": (42, 29, 13), "h": (98, 70, 36), "H": (156, 118, 60)}
LEGNO_V4_SCURO = {"k": (16, 10, 20), "h": (52, 38, 70), "H": (92, 72, 122)}   # nocturnite
LEGNO_V4_ROSSO = {"k": (22, 10, 10), "h": (70, 34, 30), "H": (118, 62, 48)}   # ardorite
MANICI_V4 = {"nocturnite": LEGNO_V4_SCURO, "ardorite": LEGNO_V4_ROSSO}


def _manico(g, y_alto, y_basso, linea=16, cappuccio=True):
    """Manico a scaletta lungo x+y=linea: medio a sinistra, chiaro al centro, contorno scuro a destra/sotto"""
    for y in range(y_alto, y_basso + 1):
        x = linea - y
        for dx, c in ((-1, "h"), (0, "H"), (1, "k")):
            if 0 <= x + dx < 16:
                g.setdefault((x + dx, y), c)
    if cappuccio:  # fine del manico in basso: contorno scuro sotto
        x = linea - y_basso
        for dx in (-1, 0):
            if 0 <= x + dx < 16 and y_basso + 1 < 16:
                g.setdefault((x + dx, y_basso + 1), "k")
        if 0 <= x - 2 < 16:
            g.setdefault((x - 2, y_basso), "k")
    return g


def _materiale_da(celle_toni):
    """celle_toni: {(x,y): tono 1..4}. Ritorna dict pixel->carattere con il contorno 'o' attorno"""
    out = {p: str(t) for p, t in celle_toni.items()}
    for (x, y) in celle_toni:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            q = (x + dx, y + dy)
            if 0 <= q[0] < 16 and 0 <= q[1] < 16 and q not in celle_toni:
                out.setdefault(q, "o")
    return out


def _unisci(manico, testa):
    """Testa sopra il manico; il manico copre solo il contorno della testa (entra nella testa)"""
    g = dict(manico)
    for p, c in testa.items():
        if c == "o" and p in manico and manico[p] in "hH":
            continue
        g[p] = c
    return g


def _tono_radiale(f, u, chiaro_fino=0.5):
    """f 0=dentro 1=fuori, u 0..1 lungo la testa: fuori chiaro, dentro scuro, piu' scuro verso la fine"""
    t = 3 if f > 0.5 else 2
    if u > 0.62:
        t -= 1
    if f > 0.78 and u < chiaro_fino:
        t = 4
    return max(1, min(4, t))


def _arco(cx, cy, rin, rout, a0, a1, taper=18, minimo=0.45, esponente=2.0):
    """Arco (esponente 2 = cerchio; piu' alto = arco piu' "squadrato", con l'angolo piu' in fuori)"""
    celle = {}
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = (abs(dx) ** esponente + abs(dy) ** esponente) ** (1 / esponente)
            a = _math.degrees(_math.atan2(dy, dx))
            if not (a0 <= a <= a1):
                continue
            t = min(a - a0, a1 - a) / taper
            mid, half = (rin + rout) / 2, (rout - rin) / 2 * min(1.0, minimo + t)
            if abs(r - mid) <= half:
                celle[(x, y)] = ((r - rin) / (rout - rin), (a - a0) / (a1 - a0))
    return celle


def _tono_luce(f, u):
    """Testa ad arco illuminata da in alto a sinistra: nella parte alta e' chiaro il bordo esterno (sopra),
    nella parte che scende a destra e' chiaro il bordo interno (a sinistra) e scuro quello esterno"""
    if u < 0.55:
        t = 3 if f > 0.5 else 2
        if f > 0.5 and u < 0.35:
            t = 4
    else:
        t = 3 if f < 0.5 else 1
        if f < 0.5 and u > 0.9:
            t = 2
    return t


def sagoma_piccone():
    testa = _arco(5.8, 10.4, 7.0, 9.0, -96, 6, taper=10, minimo=0.6, esponente=3.0)
    toni = {p: _tono_luce(f, u) for p, (f, u) in testa.items()}
    return _unisci(_manico({}, 1, 13), _materiale_da(toni))


def sagoma_martello():
    toni = {}
    for y in range(16):
        for x in range(16):
            s, p = x - y, x + y
            if 4.5 <= s <= 7.5 and 9 <= p <= 21:
                bordo_alto = s >= 6.6
                toni[(x, y)] = 4 if (bordo_alto and p <= 13) else 3 if bordo_alto else 2 if s >= 5.5 else 1
    return _unisci(_manico({}, 4, 13), _materiale_da(toni))


def sagoma_ascia():
    toni = {}
    for y in range(16):
        for x in range(16):
            s, p = x - y, 16 - (x + y)          # s: lungo il manico (alto = grande), p: distanza a sinistra del manico
            larg = 4.2 + (s - 4) * 0.35
            if 3 <= s <= 12 and 1 <= p <= larg and not (s >= 11.5 and p > larg - 1.5) and not (s <= 4 and p > larg - 1.5):
                filo = p > larg - 1.3                         # il filo della lama: chiaro
                toni[(x, y)] = 4 if (filo and s >= 8) else 3 if filo else 2 if p > 1.5 else 1
            if 8 <= s <= 11 and -1.6 <= p <= -0.5:            # piccolo dorso dall'altra parte del manico
                toni[(x, y)] = 2
    return _unisci(_manico({}, 1, 13), _materiale_da(toni))


def sagoma_pala():
    toni = {}
    cx, cy = 11.3, 4.7
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            lungo = (dx - dy) / _math.sqrt(2)     # lungo il manico
            largo = (dx + dy) / _math.sqrt(2)
            if (lungo / 3.6) ** 2 + (largo / 2.45) ** 2 <= 1.0:
                toni[(x, y)] = 4 if (largo < -1.2 and lungo > -0.5) else 3 if largo < -0.2 else 2 if largo < 1.0 else 1
    return _unisci(_manico({}, 6, 13), _materiale_da(toni))


def sagoma_zappa():
    toni = {}
    for x in range(6, 14):
        toni[(x, 2)] = 4 if x < 9 else 3
        toni[(x, 3)] = 2 if x < 12 else 1
    for (x, y) in ((6, 4), (7, 4), (6, 5)):
        toni[(x, y)] = 2 if x == 6 else 1
    return _unisci(_manico({}, 3, 13), _materiale_da(toni))


def sagoma_spada():
    toni = {}
    for y in range(1, 10):
        a, b = 15 - y, 16 - y
        toni[(a, y)] = 4 if y in (3, 4, 6) else 3
        if b < 16:
            toni[(b, y)] = 2 if y < 7 else 1
    guardia = {(4, 9), (5, 10), (6, 11), (7, 12), (3, 9), (4, 10), (5, 11), (6, 12)}
    g = {}
    _manico(g, 12, 13, linea=16, cappuccio=False)
    testa = _materiale_da(toni)
    out = _unisci(g, testa)
    gt = _materiale_da({p: 1 for p in guardia})
    for p, c in gt.items():
        if c == "o" and p in out and out[p] != "o":
            continue
        out[p] = "g" if c != "o" else "o"
    # pomolo
    for p in ((2, 14), (1, 14), (2, 15)):
        out[p] = "g"
    for p in ((0, 14), (1, 15), (3, 15), (1, 13)):
        out.setdefault(p, "o")
    return out


SAGOME_V4 = {"pickaxe": sagoma_piccone, "axe": sagoma_ascia, "shovel": sagoma_pala, "hoe": sagoma_zappa,
             "sword": sagoma_spada, "hammer": sagoma_martello}


# ---------------------------------------------------------------------------
# ATTREZZI v5: ascia, pala, zappa e spada disegnate a mano sulla griglia 16x16
# con le stesse convenzioni del piccone v4 (manico a scaletta identico, testa spessa, contorno scuro).
# Legenda: . vuoto  o contorno  1-4 toni del materiale (4 = il piu' chiaro)
# ---------------------------------------------------------------------------
def _griglia(righe):
    g = {}
    for y, riga in enumerate(righe):
        for x, c in enumerate(riga):
            if c != ".":
                g[(x, y)] = c
    return g


TESTA_ASCIA = [
    "......oooo......",
    ".....o4443o.....",
    "....o44333oo....",
    "....o4332222o...",
    "....o43222o.....",
    "....o4321o......",
    ".....o21o.......",
    "......oo........",
]
TESTA_PALA = [
    "..........oooo..",
    ".........o4443o.",
    "........o444333o",
    ".......o4443332o",
    ".......o4433221o",
    ".......o432221o.",
    "........o3221o..",
    ".........o21o...",
    "..........oo....",
]
TESTA_ZAPPA = [
    "......ooooooo...",
    ".....o4444333o..",
    ".....o4333222o..",
    ".....o4321oooo..",
    ".....o321o......",
    "......ooo.......",
]


def _con_manico(testa_righe, y_manico):
    return _unisci(_manico({}, y_manico, 13), _griglia(testa_righe))


def sagoma_ascia_v5():
    return _con_manico(TESTA_ASCIA, 2)


def sagoma_pala_v5():
    return _con_manico(TESTA_PALA, 7)


def sagoma_zappa_v5():
    return _con_manico(TESTA_ZAPPA, 2)


def sagoma_spada_v5():
    """Lama larga 3 pixel lungo la diagonale (riga centrale chiara), guardia di traverso, impugnatura e pomolo"""
    toni = {}
    for y in range(16):
        for x in range(16):
            somma, diff = x + y, x - y
            if 14 <= somma <= 16 and -1 <= diff <= 14:
                if diff >= 13 and somma != 15:
                    continue                      # punta
                toni[(x, y)] = 4 if somma == 15 and diff > 4 else 3 if somma == 15 else 3 if somma == 14 else 1
                if somma == 14 and diff > 6:
                    toni[(x, y)] = 4
    lama = _materiale_da(toni)
    out = {}
    # impugnatura in legno (stessa scaletta del manico, corta)
    _manico(out, 11, 12, linea=16, cappuccio=False)
    for p, c in lama.items():
        out[p] = c
    # guardia di traverso: diff = -2..-3, somma 12..18
    guardia = {}
    for y in range(16):
        for x in range(16):
            if x - y in (-2, -3) and 12 <= x + y <= 18:
                guardia[(x, y)] = "g"
    for p in guardia:
        out[p] = "2"
    for (x, y) in list(guardia):
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            q = (x + dx, y + dy)
            if 0 <= q[0] < 16 and 0 <= q[1] < 16 and q not in out:
                out[q] = "o"
    # pomolo
    for p in ((2, 13), (3, 13), (2, 14)):
        out[p] = "2"
    for p in ((1, 13), (1, 14), (2, 15), (3, 14), (4, 13), (2, 12), (3, 12)):
        out.setdefault(p, "o")
    return out


SAGOME_V4.update({"axe": sagoma_ascia_v5, "shovel": sagoma_pala_v5, "hoe": sagoma_zappa_v5, "sword": sagoma_spada_v5})


# --- Attrezzi v6: proporzioni da attrezzo vanilla (manico sottile a scaletta, testa compatta) ---
PALA_V6 = [
    "................",
    "..........ooo...",
    ".........o443o..",
    "........o4432o..",
    ".......o33322o..",
    "........o322o...",
    ".........h1o....",
    "........Hko.....",
    ".......Hk.......",
    "......hk........",
    ".....Hk.........",
    "....Hk..........",
    "...hk...........",
    "..Hk............",
    ".k..............",
    "................",
]
ASCIA_V6 = [
    "................",
    ".....oooo.......",
    "....o4433o..ooo.",
    "...o44332oooo1o.",
    "...o4332211Hkoo.",
    "...o432o.oHk....",
    "....o3o..hk.....",
    ".....o..Hk......",
    ".......Hk.......",
    "......hk........",
    ".....Hk.........",
    "....Hk..........",
    "...hk...........",
    "..Hk............",
    ".k..............",
    "................",
]
ZAPPA_V6 = [
    "................",
    ".......ooooo....",
    "......o44333o...",
    "......o22221o...",
    "......o2oooHk...",
    "......o1o.Hk....",
    ".......o.hk.....",
    "........Hk......",
    ".......Hk.......",
    "......hk........",
    ".....Hk.........",
    "....Hk..........",
    "...hk...........",
    "..Hk............",
    ".k..............",
    "................",
]
SPADA_V6 = [
    ".............ooo",
    "............o44o",
    "...........o432o",
    "..........o432o.",
    ".........o432o..",
    "........o432o...",
    ".......o432o....",
    "..oo..o432o.....",
    "..ogoo432o......",
    "...oggg2o.......",
    "....oggo........",
    "...ookggo.......",
    "..oHko.oo.......",
    ".oHko...........",
    "oggo............",
    ".oo.............",
]


def sagoma_pala_v6():
    return _griglia(PALA_V6)


def sagoma_ascia_v6():
    return _griglia(ASCIA_V6)


def sagoma_zappa_v6():
    return _griglia(ZAPPA_V6)


def sagoma_spada_v6():
    return _griglia(SPADA_V6)


SAGOME_V4.update({"axe": sagoma_ascia_v6, "shovel": sagoma_pala_v6, "hoe": sagoma_zappa_v6, "sword": sagoma_spada_v6})


def disegna_attrezzo_v4(tipo, materiale):
    toni, _ = tavolozza(MATERIALI[materiale])
    r = rampa(MATERIALI[materiale])
    colori = {
        "o": mix(r[0], (10, 8, 14), 0.55),
        "1": r[1], "2": r[2], "3": r[4], "4": mix(r[5], (255, 255, 255), 0.35),
        "g": mix(r[1], (34, 26, 30), 0.45),
    }
    legno = MANICI_V4.get(materiale, LEGNO_V4)
    colori.update(legno)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for (x, y), c in SAGOME_V4[tipo]().items():
        put(img, x, y, colori[c])
    return img


def salva(img, rel):
    p = TEX / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    img.save(p)


COLORI_EXO = {"exo": ((70, 120, 160), (90, 230, 255)), "exo_mk1": ((176, 180, 188), (90, 230, 255)),
              "exo_mk3": ((56, 48, 62), (190, 110, 255))}


def main():
    n = 0
    item = TEX / "item"
    # attrezzi
    for mat in ("bronze", "steel", "nocturnite", "ardorite"):
        tipi = ["pickaxe", "axe", "shovel", "hoe", "sword"] + (["hammer"] if mat in ("nocturnite", "ardorite") else [])
        for tipo in tipi:
            salva(attrezzo(tipo, mat), f"item/{mat}_{tipo}.png")
            n += 1
    salva(disegna_attrezzo_v4("hammer", "iron"), "item/forge_hammer.png")
    n += 1
    # armature
    for mat in ("bronze", "steel", "nocturnite", "ardorite"):
        for tipo in ARMATURE:
            salva(armatura(tipo, MATERIALI[mat], nome=mat), f"item/{mat}_{tipo}.png")
            n += 1
        for strato in (1, 2):
            salva(armatura_indossata(MATERIALI[mat], strato), f"models/armor/{mat}_layer_{strato}.png")
            n += 1
    for nome, (base, luce) in COLORI_EXO.items():
        for tipo in ARMATURE:
            salva(armatura(tipo, base, luce, nome), f"item/{nome}_{tipo}.png")
            n += 1
        for strato in (1, 2):
            salva(armatura_indossata(base, strato, luce), f"models/armor/{nome}_layer_{strato}.png")
            n += 1
    # materiali: solo quelli che esistono gia' (stessi nomi file)
    def se_esiste(nome, img):
        nonlocal n
        if (item / f"{nome}.png").exists():
            salva(img, f"item/{nome}.png")
            n += 1

    for mat in ("bronze", "steel", "tin", "lead", "silver", "uranium"):
        se_esiste(f"{mat}_ingot", lingotto(MATERIALI[mat], mat))
        se_esiste(f"{mat}_nugget", pepita(MATERIALI[mat], mat + "n"))
    for mat in ("bronze", "steel", "tin", "lead", "silver", "uranium", "iron", "gold", "copper", "obsidian",
                "diamond", "coal"):
        se_esiste(f"{mat}_dust", polvere(MATERIALI[mat], mat + "d"))
    for mat in ("bronze", "steel", "tin", "lead", "silver", "uranium", "iron", "gold", "copper"):
        se_esiste(f"{mat}_plate", piastra(MATERIALI[mat], mat + "p"))
    for mat in ("tin", "copper", "gold"):
        se_esiste(f"{mat}_wire", filo(MATERIALI[mat], mat + "w"))
    for mat in ("tin", "lead", "silver", "uranium"):
        se_esiste(f"raw_{mat}", grezzo(MATERIALI[mat], "raw" + mat))
    for mat in ("iron", "gold", "copper", "tin", "lead", "silver", "uranium"):
        se_esiste(f"purified_crushed_{mat}", frantumato(MATERIALI[mat], "pc" + mat))
    se_esiste("nocturnite", gemma(MATERIALI["nocturnite"], "gn"))
    se_esiste("ardorite", gemma(MATERIALI["ardorite"], "ga"))
    se_esiste("voltite_shard", scheggia((150, 90, 230), "volt"))
    print(f"Grafica oggetti v4: {n} texture ridisegnate")
    return n


if __name__ == "__main__":
    main()
