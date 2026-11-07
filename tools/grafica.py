"""
FutureTek - Grafica v2 (texture originali in stile Minecraft moderno)
====================================================================
Ridisegna attrezzi, armi, materiali, minerali, blocchi e macchine con:
  * attrezzi in DIAGONALE come quelli di Minecraft (manico in basso a sinistra, testa in alto a destra)
  * contorno scuro di 1 pixel e luce che arriva da in alto a sinistra
  * macchine con involucro d'acciaio, cornice, bulloni e pannello frontale incassato
    (lo "spirito" delle vecchie macchine industriali di Tekkit, ma disegnato da zero)

Tutti i disegni sono ORIGINALI: costruiti qui con forme geometriche e tavolozze nostre,
nessuna texture di Minecraft o di altre mod viene copiata.

Uso (dalla cartella del progetto):  python tools/grafica.py
ATTENZIONE: sovrascrive le texture elencate qui sotto (le altre restano come sono).
"""
import math
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/futuretek/textures"


# ---------------------------------------------------------------------------
# Colori
# ---------------------------------------------------------------------------
def mix(a, b, t):
    return tuple(round(x + (y - x) * t) for x, y in zip(a[:3], b[:3]))


def rampa(base, n=6):
    """Tavolozza da scuro a chiaro attorno al colore base (indice 3 ~ base)."""
    nero, bianco = (12, 10, 18), (255, 255, 250)
    passi = [0.62, 0.42, 0.20, 0.0, 0.22, 0.45]
    out = []
    for i, t in enumerate(passi[:n]):
        out.append(mix(base, nero, t) if i < 3 else mix(base, bianco, t))
    return out


MATERIALI = {
    # nome: colore base della parte metallica
    "bronze": (196, 128, 66),
    "steel": (150, 158, 170),
    "nocturnite": (88, 58, 120),
    "ardorite": (196, 44, 52),
    "tin": (190, 200, 210),
    "lead": (98, 104, 140),
    "silver": (215, 225, 235),
    "uranium": (120, 225, 70),
    "iron": (200, 200, 200),
    "gold": (240, 200, 60),
    "copper": (210, 120, 80),
}
LEGNO = rampa((132, 92, 52))
ACCIAIO = rampa((136, 142, 152))


def tela():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def put(img, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), tuple(c[:3]) + (255,))


def contorno(img, colore):
    """Contorno scuro di 1 pixel attorno alla figura (solo dove c'e' trasparenza), come negli oggetti di Minecraft."""
    pieni = {(x, y) for x in range(16) for y in range(16) if img.getpixel((x, y))[3] > 0}
    for (x, y) in list(pieni):
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < 16 and 0 <= ny < 16 and (nx, ny) not in pieni:
                put(img, nx, ny, colore)
    return img


def ombreggia(img, maschera, r):
    """Colora i pixel della maschera con la rampa r: chiaro sui bordi in alto/sinistra, scuro in basso/destra."""
    for (x, y) in maschera:
        su = (x, y - 1) in maschera
        sx = (x - 1, y) in maschera
        giu = (x, y + 1) in maschera
        dx = (x + 1, y) in maschera
        if not su or not sx:
            c = r[5] if (not su and not sx) else r[4]
        elif not giu or not dx:
            c = r[2] if (not giu and not dx) else r[3]
        else:
            c = r[4] if (x + y) % 5 == 0 else r[3]
        put(img, x, y, c)


# ---------------------------------------------------------------------------
# Attrezzi in diagonale
# ---------------------------------------------------------------------------
def manico(img, da, a, r):
    """Bastone in diagonale a "scaletta" (2 pixel per gradino), due toni alternati come il legno."""
    (x0, y0), (x1, y1) = da, a
    n = max(abs(x1 - x0), abs(y1 - y0))
    for i in range(n + 1):
        x = round(x0 + (x1 - x0) * i / n)
        y = round(y0 + (y1 - y0) * i / n)
        put(img, x, y, r[4] if i % 2 == 0 else r[3])
        put(img, x, y - 1, r[3] if i % 2 == 0 else r[2])


def maschera_da_disegno(disegno):
    return {(x, y) for y, riga in enumerate(disegno) for x, ch in enumerate(riga) if ch != "."}


# Coordinate "ruotate" lungo il manico (che va da (2,13) verso l'alto a destra):
#   u = distanza lungo il manico, v = distanza di lato (negativo = verso l'alto a sinistra)
R2 = math.sqrt(2)


def uv(x, y):
    return ((x - 2) - (y - 13)) / R2, ((x - 2) + (y - 13)) / R2


def forma(test):
    return {(x, y) for x in range(16) for y in range(16) if test(*uv(x + 0.5, y + 0.5))}


def maschera_testa(tipo):
    if tipo == "pickaxe":
        # arco: centro di curvatura in basso a sinistra
        return {(x, y) for x in range(16) for y in range(16)
                if 8.7 <= math.hypot(x + 0.5 - 2.5, y + 0.5 - 13.5) <= 11.0
                and abs(math.atan2(-(y + 0.5 - 13.5), x + 0.5 - 2.5) - (math.pi / 4 - 0.1)) < 0.86}
    if tipo == "axe":
        # lama a ventaglio verso l'alto a sinistra, attaccata alla cima del manico
        return forma(lambda u, v: 10.0 <= u <= 14.0 and -1.6 <= v <= 1.6
                     or (-5.6 <= v < -1.6 and 10.0 - (-1.6 - v) * 0.45 <= u <= 14.0 + (-1.6 - v) * 0.25))
    if tipo == "shovel":
        return forma(lambda u, v: ((u - 13.6) / 3.1) ** 2 + (v / 2.3) ** 2 <= 1.0)
    if tipo == "hoe":
        return forma(lambda u, v: 12.4 <= u <= 14.4 and -5.8 <= v <= 1.2)
    if tipo == "hammer":
        return forma(lambda u, v: 11.0 <= u <= 15.2 and -4.6 <= v <= 4.6)
    raise ValueError(tipo)


def attrezzo(tipo, metallo, legno=LEGNO):
    img = tela()
    r = rampa(metallo)
    if tipo == "sword":
        return spada(r, legno)
    fine = {"pickaxe": (9, 6), "axe": (10, 5), "shovel": (9, 6), "hoe": (10, 5), "hammer": (9, 6)}[tipo]
    manico(img, (2, 14), fine, legno)
    testa = maschera_testa(tipo)
    ombreggia(img, testa, r)
    if tipo == "hammer":
        # fascia scura nel mezzo della testa del maglio
        for (x, y) in testa:
            u, v = uv(x + 0.5, y + 0.5)
            if abs(u - 13.1) < 0.6:
                put(img, x, y, r[2])
    return contorno(img, mix(r[0], (20, 14, 24), 0.5))


def spada(r, legno):
    img = tela()
    lama = set()
    for i in range(10):
        x, y = 5 + i, 10 - i
        lama |= {(x, y), (x + 1, y), (x, y - 1)} if i < 9 else {(x, y)}
    lama -= {(15, 1), (15, 0), (14, 0)}
    for (x, y) in lama:
        # filo della lama chiaro, centro medio, dorso scuro
        d = (x - 5) - (10 - y)
        c = r[5] if d < 0 else r[4] if d == 0 else r[3]
        put(img, x, y, c)
    put(img, 14, 1, r[5])
    # guardia
    g = rampa(mix(r[3], (40, 30, 30), 0.3))
    for (x, y) in [(3, 9), (4, 10), (5, 11), (6, 12), (4, 9), (6, 11)]:
        put(img, x, y, g[3])
    put(img, 3, 9, g[4])
    put(img, 6, 12, g[2])
    # impugnatura e pomolo
    for i, (x, y) in enumerate([(4, 12), (3, 13), (3, 12)]):
        put(img, x, y, legno[4] if i % 2 == 0 else legno[3])
    put(img, 2, 14, g[3])
    put(img, 2, 13, g[4])
    return contorno(img, mix(r[0], (20, 14, 24), 0.5))


# ---------------------------------------------------------------------------
# Armature (icone)
# ---------------------------------------------------------------------------
ARMATURE = {
    "helmet": [
        "................",
        "................",
        "................",
        "....########....",
        "...##########...",
        "..############..",
        "..####....####..",
        "..###......###..",
        "..###......###..",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    "chestplate": [
        "................",
        "..####....####..",
        ".######..######.",
        ".##############.",
        ".##############.",
        "..############..",
        "...##########...",
        "...##########...",
        "...##########...",
        "...##########...",
        "...##########...",
        "...##########...",
        "....########....",
        "................",
        "................",
        "................",
    ],
    "leggings": [
        "................",
        "...##########...",
        "...##########...",
        "...##########...",
        "...####..####...",
        "...###....###...",
        "...###....###...",
        "...###....###...",
        "...###....###...",
        "...###....###...",
        "...###....###...",
        "...###....###...",
        "...###....###...",
        "................",
        "................",
        "................",
    ],
    "boots": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "...###....###...",
        "...###....###...",
        "...###....###...",
        "...###....###...",
        "..####....####..",
        ".#####....#####.",
        ".#####....#####.",
        "................",
        "................",
        "................",
    ],
}


def armatura(tipo, colore):
    img = tela()
    r = rampa(colore)
    m = maschera_da_disegno(ARMATURE[tipo])
    ombreggia(img, m, r)
    if tipo == "chestplate":
        for y in range(4, 12):
            put(img, 7, y, r[2])
            put(img, 8, y, r[4])
    if tipo == "leggings":
        for x in range(4, 12):
            put(img, x, 2, r[2])
    return contorno(img, mix(r[0], (20, 14, 24), 0.5))


def armatura_indossata(colore, strato):
    """Texture 64x32 indossata: piastre con bordi chiari e scuri (originale, a fasce)."""
    r = rampa(colore)
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    rng = random.Random(f"{colore}{strato}")
    for y in range(32):
        for x in range(64):
            if strato == 1 and x >= 32 and y < 16:
                continue
            fy, fx = y % 8, x % 8
            c = r[4] if fy == 0 or fx == 0 else r[2] if fy == 7 or fx == 7 else r[3]
            if c == r[3] and rng.random() < 0.08:
                c = r[4]
            img.putpixel((x, y), c + (255,))
    return img


# ---------------------------------------------------------------------------
# Materiali (lingotti, pepite, polveri, piastre, grezzi, gemme)
# ---------------------------------------------------------------------------
LINGOTTO = [
    "................",
    "................",
    "................",
    "................",
    "........####....",
    "......#######...",
    "....##########..",
    "..###########...",
    ".###########....",
    "..########......",
    "...######.......",
    "....###.........",
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
    "........###.....",
    ".......#####....",
    "....###.####....",
    "...#####........",
    "...#####..##....",
    "....###..####...",
    ".........###....",
    "................",
    "................",
    "................",
    "................",
]
GREZZO = [
    "................",
    "................",
    "................",
    "......####......",
    "....#######.....",
    "...#########....",
    "..###########...",
    "..############..",
    "..###########...",
    "...##########...",
    "....########....",
    "......####......",
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
    ".......####.....",
    ".....########...",
    "...############.",
    ".############...",
    "...########.....",
    ".....####.......",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
]


def lingotto(colore):
    img = tela()
    r = rampa(colore)
    ombreggia(img, maschera_da_disegno(LINGOTTO), r)
    # faccia superiore chiara
    for (x, y) in [(9, 5), (10, 5), (8, 6), (9, 6), (10, 6), (7, 7), (8, 7), (6, 7)]:
        put(img, x, y, r[5])
    return contorno(img, r[0])


def pepita(colore):
    img = tela()
    r = rampa(colore)
    ombreggia(img, maschera_da_disegno(PEPITA), r)
    return contorno(img, r[0])


def piastra(colore):
    img = tela()
    r = rampa(colore)
    ombreggia(img, maschera_da_disegno(PIASTRA), r)
    for (x, y) in [(5, 6), (6, 6), (7, 6), (8, 6)]:
        put(img, x, y, r[5])
    return contorno(img, r[0])


def grezzo(colore, seme):
    rng = random.Random(seme)
    img = tela()
    r = rampa(colore)
    pietra = rampa((120, 116, 110))
    m = maschera_da_disegno(GREZZO)
    ombreggia(img, m, r)
    for (x, y) in m:
        if rng.random() < 0.18:
            put(img, x, y, pietra[3] if rng.random() < 0.6 else pietra[2])
    return contorno(img, r[0])


def polvere(colore, seme):
    rng = random.Random(seme)
    img = tela()
    r = rampa(colore)
    for y in range(6, 14):
        larghezza = (y - 5) * 0.9
        for x in range(16):
            if abs(x - 7.5) < larghezza and not (y == 13 and abs(x - 7.5) > larghezza - 1):
                c = r[4] if y < 9 else r[3]
                if rng.random() < 0.25:
                    c = r[5] if rng.random() < 0.5 else r[2]
                put(img, x, y, c)
    return contorno(img, r[1])


# ---------------------------------------------------------------------------
# Blocchi: pietra, minerali, blocchi di metallo
# ---------------------------------------------------------------------------
def rumore_morbido(seme, celle=4):
    """Rumore "a macchie" morbido 16x16 (valori -1..1): griglia casuale interpolata, ripetibile ai bordi."""
    rng = random.Random(seme)
    g = [[rng.uniform(-1, 1) for _ in range(celle + 1)] for _ in range(celle + 1)]
    passo = 16 / celle
    out = [[0.0] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            gx, gy = x / passo, y / passo
            i, j = int(gx) % celle, int(gy) % celle
            fx, fy = gx - int(gx), gy - int(gy)
            fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
            a = g[j][i] * (1 - fx) + g[j][(i + 1) % celle] * fx
            b = g[(j + 1) % celle][i] * (1 - fx) + g[(j + 1) % celle][(i + 1) % celle] * fx
            out[y][x] = a * (1 - fy) + b * fy
    return out


def grana(seme, forza=1.0):
    """Rumore a due scale + granello per pixel (-1..1): da' alle superfici l'aspetto "materico" dei blocchi moderni."""
    a = rumore_morbido(seme + "_a", 4)
    b = rumore_morbido(seme + "_b", 8)
    rng = random.Random(seme + "_g")
    return [[max(-1.0, min(1.0, (a[y][x] * 0.55 + b[y][x] * 0.35 + rng.uniform(-0.45, 0.45)) * forza))
             for x in range(16)] for y in range(16)]


def tono(r, v):
    """Da un valore -1..1 a un colore della rampa fra r[2] (scuro) e r[4] (chiaro), con passaggi morbidi."""
    if v < 0:
        return mix(r[3], r[2], min(1.0, -v))
    return mix(r[3], r[4], min(1.0, v))


def scurisci(c, t):
    return mix(c, (14, 12, 20), t)


def schiarisci(c, t):
    return mix(c, (255, 255, 248), t)


def pietra(seme, profonda=False):
    """Roccia originale: macchie morbide a 5 toni e granello fine; la versione profonda ha strati orizzontali."""
    base = (74, 74, 82) if profonda else (122, 122, 124)
    r = rampa(base)
    n = grana(seme)
    rng = random.Random(seme + "_p")
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            v = n[y][x]
            if profonda:
                v += 0.35 * math.sin((y + rng.uniform(-0.6, 0.6)) * 1.25)  # strati
            q = round(v * 2.2) / 2.2  # pochi toni, come i blocchi veri
            c = tono(r, q * 0.75)
            img.putpixel((x, y), c + (255,))
    return img


GRUMI_MINERALE = [(2, 2), (9, 1), (5, 7), (11, 7), (2, 12), (8, 12)]
FORME_GRUMI = [
    [(0, 0), (1, 0), (0, 1), (1, 1), (2, 1), (1, 2)],
    [(1, 0), (2, 0), (0, 1), (1, 1), (2, 1), (1, 2), (2, 2)],
    [(0, 0), (1, 0), (2, 0), (1, 1), (2, 1)],
]


# Ogni minerale ha la sua "forma" (per riconoscerlo a colpo d'occhio, non solo dal colore)
STILI_MINERALE = {
    # nome: (forma dei grumi, alone luminoso, scintille)
    "tin": ("scaglie", False, False),
    "lead": ("blocchi", False, False),
    "silver": ("scaglie", False, True),
    "uranium": ("tondi", True, False),
    "voltite": ("cristalli", True, True),
}
FORME_STILE = {
    "scaglie": [[(0, 0), (1, 0), (1, 1), (2, 1)], [(1, 0), (0, 1), (1, 1)], [(0, 0), (1, 1), (2, 1), (2, 2)]],
    "blocchi": [[(0, 0), (1, 0), (2, 0), (0, 1), (1, 1), (2, 1)], [(0, 0), (1, 0), (0, 1), (1, 1), (0, 2), (1, 2)]],
    "tondi": [[(1, 0), (0, 1), (1, 1), (2, 1), (1, 2)], [(0, 0), (1, 0), (0, 1), (1, 1)]],
    "cristalli": [[(2, 0), (1, 1), (2, 1), (0, 2), (1, 2)], [(0, 0), (1, 1), (2, 2), (1, 2), (2, 3)]],
}


def _blob(rng, stile, dim):
    """Fa crescere un grumo di minerale con la forma tipica dello stile (celle relative a (0,0))."""
    celle = {(0, 0)}
    passi = {"scaglie": [(1, 0), (1, 1), (0, 1), (-1, 0)], "blocchi": [(1, 0), (0, 1), (-1, 0), (0, -1)],
             "tondi": [(1, 0), (0, 1), (-1, 0), (0, -1), (1, 1)], "cristalli": [(1, -1), (1, -1), (0, 1), (1, 0)],
             "grumi": [(1, 0), (0, 1), (-1, 0), (0, -1)]}[stile]
    while len(celle) < dim:
        x, y = rng.choice(sorted(celle))
        dx, dy = rng.choice(passi)
        celle.add((x + dx, y + dy))
    if stile == "blocchi":  # riempi i buchi: blocchetti pieni
        xs = [c[0] for c in celle]
        ys = [c[1] for c in celle]
        celle = {(x, y) for x in range(min(xs), max(xs) + 1) for y in range(min(ys), max(ys) + 1)
                 if len(celle) >= 4} or celle
    return celle


def _grumi(rng, forme, n=5, stile=None):
    """Sceglie n grumi (di 4-7 pixel) senza sovrapporli (bordo di 1 pixel libero)."""
    presi, out = set(), []
    for _ in range(200):
        if len(out) >= n:
            break
        if stile:
            f = _blob(rng, stile, rng.randint(4, 7) if stile != "blocchi" else rng.randint(3, 5))
        else:
            f = set(rng.choice(forme))
        x0, y0 = rng.randint(0, 14), rng.randint(0, 14)
        celle = {(x0 + dx, y0 + dy) for dx, dy in f}
        if any(not (0 <= x < 16 and 0 <= y < 16) for x, y in celle):
            continue
        zona = {(x + a, y + b) for x, y in celle for a in (-1, 0, 1) for b in (-1, 0, 1)}
        if zona & presi:
            continue
        presi |= celle
        out.append(celle)
    return out


def _incastona(img, celle, r, alone_colore=None):
    """Disegna un grumo di minerale incastonato: ombra intorno, luce in alto a sinistra, faccette scure in basso."""
    for (x, y) in celle:
        for (sx, sy) in ((x + 1, y), (x, y + 1), (x + 1, y + 1), (x - 1, y), (x, y - 1)):
            if (sx, sy) not in celle and 0 <= sx < 16 and 0 <= sy < 16:
                forza = 0.42 if (sx > x or sy > y) else 0.18
                put(img, sx, sy, scurisci(img.getpixel((sx, sy)), forza))
    if alone_colore:
        for (x, y) in celle:
            for a in (-2, -1, 0, 1, 2):
                for b in (-2, -1, 0, 1, 2):
                    p = (x + a, y + b)
                    if p not in celle and 0 <= p[0] < 16 and 0 <= p[1] < 16 and abs(a) + abs(b) <= 2:
                        put(img, p[0], p[1], mix(img.getpixel(p), alone_colore, 0.12))
    for (x, y) in celle:
        su, sx_ = (x, y - 1) in celle, (x - 1, y) in celle
        giu, dx_ = (x, y + 1) in celle, (x + 1, y) in celle
        if not su and not sx_:
            c = r[5]
        elif not su or not sx_:
            c = r[4]
        elif not giu and not dx_:
            c = mix(r[1], r[2], 0.5)
        elif not giu or not dx_:
            c = mix(r[2], r[3], 0.4)
        else:
            c = r[3]
        put(img, x, y, c)


def minerale_stile(nome, colore, seme, profonda=False):
    """Minerale con forma, alone e scintille propri (grumi incastonati nella roccia)"""
    forma_nome, alone, scintille = STILI_MINERALE.get(nome, ("scaglie", False, False))
    img = pietra(seme + "_pietra", profonda)
    r = rampa(colore)
    rng = random.Random(seme)
    for celle in _grumi(rng, FORME_STILE[forma_nome], 6, forma_nome):
        _incastona(img, celle, r, colore if alone else None)
        if scintille:
            x, y = min(celle)
            put(img, x, y, (255, 255, 255))
    return img


def minerale(colore, seme, profonda=False):
    img = pietra(seme + "_pietra", profonda)
    r = rampa(colore)
    rng = random.Random(seme)
    for celle in _grumi(rng, FORME_GRUMI, 6, "grumi"):
        _incastona(img, celle, r)
    return img


def blocco_metallo(colore, seme):
    """Blocco di metallo: quattro lastre con smusso, giunture e riflesso diagonale; superficie appena granulosa."""
    r = rampa(colore)
    n = grana(seme + "_mb", 0.5)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            lx, ly = x % 8, y % 8  # posizione dentro la lastra
            v = n[y][x] * 0.5
            if (x + y) % 16 in (5, 6) or (x + y) % 16 == 14:
                v += 0.6  # riflesso
            c = tono(r, v)
            if lx == 0 or ly == 0:
                c = mix(r[4], r[5], 0.4)
            elif lx == 7 or ly == 7:
                c = r[1] if (lx == 7 and ly == 7) else r[2]
            elif lx == 1 and ly == 1:
                c = r[5]
            img.putpixel((x, y), c + (255,))
    return img


def blocco_grezzo(colore, seme):
    """Blocco di minerale grezzo: grosse pepite a celle, ognuna con luce in alto a sinistra e ombra in basso."""
    rng = random.Random(seme)
    r = rampa(colore)
    semi = [(rng.uniform(0, 16), rng.uniform(0, 16), rng.uniform(-0.6, 0.6)) for _ in range(9)]

    def cella(x, y):
        best, second, k = 99, 99, 0
        for i, (sx, sy, _) in enumerate(semi):
            for ox in (-16, 0, 16):
                for oy in (-16, 0, 16):
                    d = math.hypot(x - sx - ox, y - sy - oy)
                    if d < best:
                        best, second, k = d, best, i
                    elif d < second:
                        second = d
        return k, second - best

    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            k, bordo = cella(x + 0.5, y + 0.5)
            k_su, _ = cella(x + 0.5, y - 0.5)
            k_sx, _ = cella(x - 0.5, y + 0.5)
            k_giu, _ = cella(x + 0.5, y + 1.5)
            k_dx, _ = cella(x + 1.5, y + 0.5)
            v = semi[k][2] + rng.uniform(-0.2, 0.2)
            c = tono(r, v)
            if bordo < 1.0:
                c = r[1]
            elif k_giu != k or k_dx != k:
                c = r[2]
            elif k_su != k or k_sx != k:
                c = r[5] if rng.random() < 0.4 else r[4]
            img.putpixel((x, y), c + (255,))
    return img


# ---------------------------------------------------------------------------
# Macchine
# ---------------------------------------------------------------------------
def involucro(r=ACCIAIO, bulloni=True, giunta=False, seme="m"):
    """Lato di una macchina: lastra metallica con smusso sottile (luce in alto a sinistra), grana fine e rivetti."""
    n = grana(seme + "_inv", 0.55)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            v = n[y][x] * 0.4 + 0.18 - (y - 7.5) * 0.025
            c = tono(r, v)
            if x == 15 or y == 15:
                c = r[1]
            elif x == 0 or y == 0:
                c = mix(r[4], r[5], 0.3)
            elif x == 14 or y == 14:
                c = mix(r[2], r[3], 0.35)
            elif x == 1 or y == 1:
                c = tono(r, 0.7)
            img.putpixel((x, y), c + (255,))
    if giunta:
        for x in range(2, 14):
            put(img, x, 7, mix(r[1], r[2], 0.4))
            put(img, x, 8, tono(r, 0.75))
    if bulloni:
        for (x, y) in [(2, 2), (13, 2), (2, 13), (13, 13)]:
            put(img, x, y, r[5])
            put(img, x + 1, y + 1, r[1])
            put(img, x + 1, y, r[2])
            put(img, x, y + 1, r[2])
    return img


def pannello(img, x0, y0, x1, y1, r=ACCIAIO, fondo=None):
    """Riquadro incassato: ombra spessa in alto/sinistra, filo di luce in basso/destra, fondo sfumato."""
    base = fondo if fondo else mix(r[1], r[2], 0.6)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if y == y0 or x == x0:
                c = scurisci(r[1], 0.35)
            elif y == y1 or x == x1:
                c = mix(r[4], r[5], 0.3)
            elif y == y0 + 1 or x == x0 + 1:
                c = scurisci(base, 0.35)  # ombra interna
            else:
                c = mix(base, schiarisci(base, 0.12), (y - y0) / max(1, y1 - y0))
            put(img, x, y, c)
    return img


def griglia(img, x0, y0, x1, y1, r=ACCIAIO):
    """Griglia di aerazione: fessure scure con il bordo inferiore illuminato."""
    pannello(img, x0, y0, x1, y1, r, scurisci(r[1], 0.3))
    for y in range(y0 + 1, y1):
        if (y - y0) % 2 == 0:
            for x in range(x0 + 1, x1):
                put(img, x, y, r[3] if x > x0 + 1 else r[2])
            put(img, x1 - 1, y, r[4])
    return img


def bagliore(c, acceso):
    return c if acceso else mix(c, (40, 40, 44), 0.7)


def fronte(nome, acceso):
    img = involucro(seme=nome)
    a = acceso
    if nome == "combustion_generator":
        griglia(img, 3, 3, 12, 6)
        pannello(img, 4, 8, 11, 12)
        for x in range(5, 11):
            for y in range(9, 12):
                if a:
                    c = (255, 210, 80) if y == 11 else (240, 120, 30) if (x + y) % 2 else (200, 70, 20)
                else:
                    c = (40, 36, 34) if (x + y) % 2 else (30, 28, 26)
                put(img, x, y, c)
    elif nome == "geothermal_generator":
        pannello(img, 3, 3, 12, 12)
        for y in range(4, 12):
            for x in range(4, 12):
                lava = [(255, 200, 60), (240, 120, 20), (180, 60, 10)]
                c = lava[(x * 3 + y * 5) % 3] if a else mix(lava[(x + y) % 3], (40, 30, 30), 0.75)
                put(img, x, y, c)
        for x in range(4, 12):
            put(img, x, 7, ACCIAIO[2])
    elif nome == "crusher":
        # due mascelle dentate che si incastrano al centro
        pannello(img, 3, 3, 12, 12, fondo=(30, 30, 34))
        for x in range(4, 12):
            k = (x - 4) % 3
            giu = [1, 3, 1][k]
            su = [3, 1, 1][(k + 1) % 3]
            for y in range(4, 5 + giu):
                put(img, x, y, (226, 230, 236) if y == 4 + giu - 1 else ACCIAIO[5] if k == 1 else ACCIAIO[4])
            for y in range(11 - su, 12):
                put(img, x, y, (226, 230, 236) if y == 11 - su else ACCIAIO[4] if k == 0 else ACCIAIO[3])
        if a:
            for (x, y) in [(6, 7), (9, 8), (8, 7), (11, 8), (5, 8)]:
                put(img, x, y, (255, 180, 70))
    elif nome == "electric_furnace":
        pannello(img, 3, 6, 12, 12)
        for x in range(4, 12):
            for y in range(7, 12):
                c = (255, 140, 40) if a and y % 2 == 1 else (230, 80, 30) if a else ((70, 60, 58) if y % 2 else (40, 36, 36))
                put(img, x, y, c)
        for x in range(4, 12):
            put(img, x, 4, ACCIAIO[2])
        put(img, 11, 4, (255, 60, 50) if a else (90, 40, 40))
    elif nome == "compressor":
        # due piastre che schiacciano un cubo, con frecce
        pannello(img, 3, 3, 12, 12, fondo=(34, 34, 38))
        p = 1 if a else 0
        for x in range(4, 12):
            put(img, x, 4 + p, ACCIAIO[5])
            put(img, x, 5 + p, ACCIAIO[3])
            put(img, x, 10 - p, ACCIAIO[4])
            put(img, x, 11 - p, ACCIAIO[2])
        for y in range(7, 9):
            for x in range(6, 10):
                put(img, x, y, (90, 200, 255) if a else (110, 120, 130))
        if not a:
            for x in range(4, 12):
                put(img, x, 6, (34, 34, 38))
    elif nome == "extractor":
        pannello(img, 3, 3, 12, 12)
        for y in range(4, 9):
            for x in range(4 + (y - 4), 12 - (y - 4)):
                put(img, x, y, ACCIAIO[4] if x == 4 + (y - 4) else ACCIAIO[3])
        for y in range(9, 12):
            put(img, 7, y, (130, 220, 90) if a else ACCIAIO[2])
            put(img, 8, y, (100, 190, 70) if a else ACCIAIO[2])
    elif nome == "wire_press":
        pannello(img, 3, 3, 12, 12)
        for cy in (5.5, 9.5):
            for y in range(4, 12):
                for x in range(4, 12):
                    if abs(y - cy) < 1.6:
                        put(img, x, y, ACCIAIO[4] if y < cy else ACCIAIO[3])
        for x in range(4, 12):
            put(img, x, 7, (230, 140, 80) if a else (150, 100, 70))
            put(img, x, 8, (200, 110, 60) if a else (120, 80, 60))
    elif nome == "chemical_bath":
        pannello(img, 3, 3, 12, 12)
        for y in range(5, 12):
            for x in range(4, 12):
                c = (60, 140, 230) if (x + y) % 3 else (90, 170, 245)
                if a and (x * 7 + y * 3) % 11 == 0:
                    c = (200, 235, 255)
                put(img, x, y, c if a or y > 7 else ACCIAIO[2])
    elif nome == "thermal_centrifuge":
        pannello(img, 3, 3, 12, 12)
        for y in range(4, 12):
            for x in range(4, 12):
                d = math.hypot(x - 7.5, y - 7.5)
                if d < 3.8:
                    c = ((255, 120, 40) if d < 1.5 else (200, 60, 30)) if a else (ACCIAIO[4] if d < 1.5 else ACCIAIO[3])
                    if 2.4 < d < 3.0:
                        c = ACCIAIO[1]
                    put(img, x, y, c)
    elif nome == "growth_accelerator":
        pannello(img, 3, 3, 12, 12, fondo=(40, 60, 40))
        for (x, y) in [(7, 10), (7, 9), (8, 8), (7, 7), (6, 6), (9, 6), (8, 5), (6, 5)]:
            put(img, x, y, (120, 230, 90) if a else (70, 120, 60))
    elif nome == "harvester":
        # falce rotante a quattro lame
        pannello(img, 3, 3, 12, 12)
        lama = (230, 236, 240) if a else ACCIAIO[4]
        for i in range(4):
            put(img, 7 - i, 7 - i if i < 3 else 5, lama)
            put(img, 8 + i if i < 3 else 10, 7 - i, lama)
            put(img, 8 + i, 8 + i if i < 3 else 10, lama)
            put(img, 7 - i if i < 3 else 5, 8 + i, lama)
        for (x, y) in [(7, 7), (8, 7), (7, 8), (8, 8)]:
            put(img, x, y, (255, 200, 60) if a else ACCIAIO[2])
    elif nome == "quarry":
        pannello(img, 3, 3, 12, 12)
        for y in range(4, 12):
            w = max(0, 4 - (y - 4) // 2)
            for x in range(8 - w, 8 + w):
                put(img, x, y, (120, 220, 230) if a and (x + y) % 2 else ACCIAIO[4] if (x + y) % 2 else ACCIAIO[3])
    elif nome == "reactor_core":
        r = rampa((90, 100, 96))
        img = involucro(r, seme=nome)
        pannello(img, 3, 3, 12, 12, r)
        for y in range(4, 12):
            for x in range(4, 12):
                if (x - 4) % 3 != 2 and (y - 4) % 3 != 2:
                    put(img, x, y, (120, 255, 100) if a else (50, 90, 50))
    elif nome == "network_terminal":
        pannello(img, 3, 3, 12, 10, fondo=(20, 40, 50))
        for y in range(4, 10):
            for x in range(4, 12):
                if a and (x + y * 3) % 4 == 0:
                    put(img, x, y, (90, 230, 210))
        for x in range(5, 11, 2):
            put(img, x, 12, ACCIAIO[5])
    return img


def cima_macchina():
    img = involucro(seme="top")
    griglia(img, 4, 4, 11, 11)
    return img


def fondo_macchina():
    return involucro(rampa((100, 104, 112)), bulloni=False, seme="bottom")


def _celle_solari(img, celle, passo, linea_col, x0=2, x1=13):
    """Celle fotovoltaiche: blu con riflesso diagonale e fili d'argento fra le celle."""
    rc = rampa(celle)
    for y in range(x0, x1 + 1):
        for x in range(x0, x1 + 1):
            if (x - x0) % passo == 0 or (y - x0) % passo == 0:
                c = linea_col
            else:
                d = (x + y) % 12
                c = rc[4] if d in (0, 1) else rc[3] if d < 6 else rc[2]
                if (x - x0) % passo == 1 and (y - x0) % passo == 1:
                    c = rc[5]
            put(img, x, y, c)


def pannello_solare():
    img = involucro(seme="solar")
    _celle_solari(img, (40, 80, 170), 4, ACCIAIO[4])
    return img


def banco(livello, parte):
    colori = {1: (150, 100, 60), 2: (80, 110, 160), 3: (90, 60, 130)}
    r = rampa(colori[livello])
    img = involucro(r, giunta=(parte == "side"), seme=f"bb{livello}{parte}")
    if parte == "front":
        pannello(img, 5, 5, 10, 10, r, r[1])
        for (x, y) in [(7, 7), (8, 7), (7, 8), (8, 8)]:
            put(img, x, y, (255, 220, 80))
    elif parte == "top":
        for x in range(4, 12):
            put(img, x, 6, r[1])
            put(img, x, 9, r[1])
        put(img, 5, 7, (240, 70, 60))
        put(img, 10, 7, (60, 180, 240))
    return img


def logistica_lato():
    r = rampa((96, 108, 116))
    img = involucro(r, giunta=True, seme="logi")
    return img


def cima_assemblatore():
    img = involucro(seme="ac_top")
    pannello(img, 3, 3, 12, 12, fondo=(120, 90, 60))
    for i in range(4, 12):
        put(img, i, 7, (90, 66, 44))
        put(img, 7, i, (90, 66, 44))
    return img


# ---------------------------------------------------------------------------
# Cavi, condotti, blocchi alchemici
# ---------------------------------------------------------------------------
def cavo(base, striscia):
    """Cavo: cilindro con luce in alto, anelli in rilievo ogni 4 pixel, grana leggera, eventuale striscia colorata."""
    r = rampa(base)
    n = grana("cavo" + str(base), 0.4)
    img = Image.new("RGBA", (16, 16))
    profilo = [1, 2, 3, 4, 5, 5, 4, 4, 3, 3, 3, 3, 2, 2, 1, 1]
    for y in range(16):
        for x in range(16):
            i = profilo[x]
            c = mix(r[min(5, i)], r[min(5, i + 1)] if n[y][x] > 0 else r[max(0, i - 1)], abs(n[y][x]) * 0.4)
            if y % 4 == 3:
                c = r[max(0, i - 1)]
            elif y % 4 == 0:
                c = r[min(5, i + 1)] if i < 5 else c
            if striscia and x in (6, 7) and y % 4 != 3:
                c = striscia if x == 6 else mix(striscia, (0, 0, 0), 0.25)
            img.putpixel((x, y), c + (255,))
    return img


def condotto(base, vetro):
    """Tubo: anelli di metallo con smusso e tratti di vetro con riflessi."""
    rm = rampa(base)
    rv = rampa(vetro)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            k = x % 5
            if k == 0 or y in (0, 15):
                c = rm[4] if y < 6 else rm[3] if y < 11 else rm[2]
                if y in (0, 15):
                    c = rm[1] if y == 15 else rm[4]
                if k == 0 and y in (0, 15):
                    c = rm[1]
            else:
                c = rv[4] if y in (2, 3) else rv[3] if y < 11 else rv[2]
                if y in (2, 3) and k in (1, 2):
                    c = rv[5]
                if y == 1 or y == 14:
                    c = rm[2]
                if k == 4 and 3 < y < 13:
                    c = mix(c, rv[2], 0.5)
            img.putpixel((x, y), c + (255,))
    return img


def tavolo_sintesi(parte):
    pietra = rampa((70, 52, 92))
    img = involucro(pietra, bulloni=(parte != "top"), giunta=(parte == "side"), seme="st" + parte)
    if parte == "top":
        oro = rampa((220, 170, 255))
        for y in range(16):
            for x in range(16):
                d = math.hypot(x - 7.5, y - 7.5)
                if 4.6 < d < 5.6:
                    put(img, x, y, oro[4])
                ang = math.atan2(y - 7.5, x - 7.5)
                for k in range(3):
                    a0 = ang - (k * 2 * math.pi / 3 + 0.3)
                    if d < 4.6 and abs(math.sin(a0)) * d < 0.6 and math.cos(a0) > 0:
                        put(img, x, y, oro[3])
        put(img, 7, 7, oro[5])
        put(img, 8, 8, oro[5])
    return img


def captatore(colore):
    r = rampa(colore)
    img = involucro(rampa((70, 60, 80)), seme=f"cap{colore}")
    for y in range(3, 13):
        for x in range(3, 13):
            d = abs(x - 7.5) + abs(y - 7.5)
            if d < 5.5:
                put(img, x, y, r[5] if d < 1.6 else r[4] if d < 3 else r[3] if d < 4.5 else r[2])
    return img


def replicatore(parte):
    r = rampa((90, 70, 112))
    img = involucro(r, giunta=(parte == "side"), seme="rep" + parte)
    if parte == "top":
        pannello(img, 4, 4, 11, 11, r, (40, 20, 60))
        for (x, y) in [(6, 6), (9, 6), (7, 8), (8, 8), (6, 9), (9, 9), (7, 7)]:
            put(img, x, y, (200, 120, 255))
    return img


def carica_stellare(parte):
    r = rampa((70, 50, 100))
    img = Image.new("RGBA", (16, 16))
    oro = rampa((220, 180, 70))
    for y in range(16):
        for x in range(16):
            if parte == "side":
                c = oro[3] if y in (0, 1, 14, 15) else oro[4] if y in (2, 13) else r[3]
                if 3 <= y <= 12 and (x + y) % 6 == 0:
                    c = (255, 235, 140)
                if 3 <= y <= 12 and x % 4 == 0:
                    c = r[2]
            else:
                d = math.hypot(x - 7.5, y - 7.5)
                c = (255, 245, 190) if d < 2 else oro[4] if d < 3.2 else r[3] if d < 6.5 else oro[2]
            img.putpixel((x, y), c + (255,))
    return img


# ---------------------------------------------------------------------------
# Comodita': fornace di ferro, rilevatori, sacca del minatore
# ---------------------------------------------------------------------------
FERRO = rampa((176, 178, 182))


def fornace_ferro(parte, acceso=False):
    """Fornace di ferro: involucro di ferro chiaro; davanti bocca del fuoco con griglia."""
    img = involucro(FERRO, giunta=(parte == "side"), seme="if" + parte)
    if parte == "top":
        griglia(img, 4, 4, 11, 11, FERRO)
    elif parte == "front":
        # sportello superiore con fessura
        for x in range(4, 12):
            put(img, x, 4, FERRO[1])
        put(img, 7, 3, FERRO[5])
        put(img, 8, 3, FERRO[5])
        pannello(img, 3, 6, 12, 12, FERRO, (24, 22, 22))
        for x in range(4, 12):
            for y in range(7, 12):
                if acceso:
                    c = (255, 220, 110) if y == 11 else (250, 150, 40) if (x * 3 + y) % 4 else (210, 80, 20)
                    if y < 9 and (x + y) % 3 == 0:
                        c = (24, 22, 22)
                else:
                    c = (24, 22, 22)
                put(img, x, y, c)
        for x in range(4, 12, 2):
            put(img, x, 9, FERRO[2])
    return img


def rilevatore(colore):
    """Rilevatore di minerali: bacchetta di ferro in diagonale con un cristallo colorato e un anello."""
    img = tela()
    r = rampa(colore)
    manico(img, (2, 14), (8, 8), rampa((150, 152, 160)))
    cristallo = forma(lambda u, v: ((u - 13.2) / 2.6) ** 2 + (v / 2.0) ** 2 <= 1.0)
    ombreggia(img, cristallo, r)
    for (x, y) in [(8, 7), (9, 8), (7, 8), (8, 9)]:
        put(img, x, y, (230, 190, 70))
    put(img, 11, 3, (255, 255, 255))
    return contorno(img, mix(r[0], (20, 14, 24), 0.5))


SACCA = [
    "................",
    "......####......",
    ".....#....#.....",
    "....######......",
    "...##########...",
    "..############..",
    "..############..",
    ".##############.",
    ".##############.",
    ".##############.",
    ".##############.",
    ".##############.",
    "..############..",
    "...##########...",
    "................",
    "................",
]


def sacca():
    img = tela()
    cuoio = rampa((140, 96, 58))
    m = maschera_da_disegno(SACCA)
    ombreggia(img, m, cuoio)
    # laccio e tre puntini colorati (le tre polveri di rilevamento)
    for x in range(4, 12):
        put(img, x, 5, cuoio[1])
    for (x, y), c in zip([(5, 9), (8, 10), (11, 9)], [(90, 210, 90), (70, 120, 240), (80, 230, 230)]):
        put(img, x, y, c)
        put(img, x, y + 1, mix(c, (0, 0, 0), 0.3))
    return contorno(img, cuoio[0])


COLORI_RILEVAMENTO = {1: (80, 200, 80), 2: (60, 110, 230), 3: (70, 220, 225)}


# ---------------------------------------------------------------------------
# Fase 10: identita' delle macchine (fascia colorata + simbolo sul lato) e nuovi blocchi
# ---------------------------------------------------------------------------
SIMBOLI = {
    # 5x5 (x = pixel acceso)
    "fiamma": ["..x..", ".xx..", ".xxx.", "xxxxx", ".xxx."],
    "ingranaggio": [".x.x.", "xxxxx", ".x.x.", "xxxxx", ".x.x."],
    "freccia_giu": ["..x..", "..x..", "xxxxx", ".xxx.", "..x.."],
    "goccia": ["..x..", ".xxx.", "xxxxx", "xxxxx", ".xxx."],
    "filo": ["x...x", ".x.x.", "..x..", ".x.x.", "x...x"],
    "giro": [".xxx.", "x...x", "x.x.x", "x...x", ".xxx."],
    "fulmine": ["...xx", "..xx.", ".xxxx", "..xx.", ".xx.."],
    "sole": ["x.x.x", ".xxx.", "xxxxx", ".xxx.", "x.x.x"],
    "onda": [".....", "x.x.x", ".x.x.", "x.x.x", "....."],
    "pala": ["x...x", ".x.x.", "..x..", ".x.x.", "x...x"],
    "rete": ["xx.xx", "x...x", "..x..", "x...x", "xx.xx"],
    "punta": ["xxxxx", ".xxx.", ".xxx.", "..x..", "..x.."],
    "foglia": ["...xx", "..xxx", ".xxx.", "xxx..", "x...."],
    "doppia_freccia": ["x...x", "xx.xx", "xxxxx", "xx.xx", "x...x"],
    "albero": [".xxx.", "xxxxx", ".xxx.", "..x..", "..x.."],
    "seme": ["x.x.x", ".....", "x.x.x", ".....", "x.x.x"],
    "osso": ["x...x", ".x.x.", "..x..", ".x.x.", "x...x"],
    "leva": ["...x.", "..x..", ".x...", "xxxxx", "xxxxx"],
    "stella": ["..x..", ".xxx.", "xxxxx", ".xxx.", "x...x"],
    "anima": [".xxx.", "x.x.x", "xxxxx", ".x.x.", "x.x.x"],
    "mirino": ["..x..", ".x.x.", "xx.xx", ".x.x.", "..x.."],
    "robot": [".xxx.", "x.x.x", "xxxxx", ".x.x.", ".x.x."],
}
IDENTITA = {
    # macchina: (colore della fascia, simbolo)
    "crusher": ((226, 128, 44), "ingranaggio"),
    "electric_furnace": ((214, 64, 48), "fiamma"),
    "compressor": ((70, 120, 220), "freccia_giu"),
    "extractor": ((90, 180, 70), "goccia"),
    "wire_press": ((210, 120, 70), "filo"),
    "chemical_bath": ((60, 180, 210), "goccia"),
    "thermal_centrifuge": ((200, 70, 170), "giro"),
    "advanced_crusher": ((255, 150, 30), "doppia_freccia"),
    "induction_furnace": ((255, 70, 90), "doppia_freccia"),
    "combustion_generator": ((240, 190, 40), "fulmine"),
    "geothermal_generator": ((240, 110, 30), "fulmine"),
    "advanced_generator": ((255, 220, 60), "fulmine"),
    "growth_accelerator": ((100, 200, 80), "foglia"),
    "harvester": ((160, 210, 60), "foglia"),
    "logistics_core": ((60, 200, 190), "rete"),
    "mining_mole": ((170, 120, 70), "punta"),
    "tree_farm": ((70, 150, 60), "albero"),
    "planter": ((190, 170, 60), "seme"),
    "auto_fertilizer": ((230, 230, 210), "osso"),
    "energy_switch": ((230, 200, 60), "leva"),
    "electric_enchanter": ((160, 80, 230), "stella"),
    "mob_incubator": ((70, 200, 200), "anima"),
    "laser_turret": ((230, 50, 50), "mirino"),
    "robot_dock": ((80, 160, 240), "robot"),
    "reactor_maintainer": ((90, 210, 120), "ingranaggio"),
    "enchant_researcher": ((120, 90, 220), "stella"),
}


def lato_identita(nome):
    """Lato di una macchina: involucro d'acciaio + fascia incassata del suo colore + simbolo luminoso al centro."""
    colore, simbolo = IDENTITA[nome]
    r = rampa(colore)
    img = involucro(seme=nome + "_side")
    n = grana(nome + "_fascia", 0.5)
    for x in range(3, 13):
        put(img, x, 5, scurisci(r[1], 0.2))
        put(img, x, 6, r[2])
        for y in range(7, 10):
            put(img, x, y, tono(r, n[y][x] * 0.5 + (0.2 if y == 7 else 0)))
        put(img, x, 10, r[4])
    pannello(img, 5, 5, 11, 11, ACCIAIO, (26, 26, 30))
    for j, riga in enumerate(SIMBOLI[simbolo]):
        for i, ch in enumerate(riga):
            if ch == "x":
                put(img, 6 + i, 6 + j, r[5] if j < 2 else r[4])
    return img


def fronte_extra(nome, acceso):
    """Fronti delle macchine nuove"""
    a = acceso
    if nome == "advanced_crusher":
        img = fronte("crusher", a)
        for x in range(3, 13):
            put(img, x, 2, (255, 150, 30) if a else (150, 100, 50))
            put(img, x, 13, (255, 150, 30) if a else (150, 100, 50))
        return img
    if nome == "induction_furnace":
        img = involucro(seme="ind")
        pannello(img, 3, 3, 12, 12, fondo=(30, 24, 24))
        for y in range(4, 12):
            for x in range(4, 12):
                spira = (y % 2 == 0)
                if spira:
                    put(img, x, y, (255, 120, 60) if a else (150, 90, 70))
                elif a and (x + y) % 3 == 0:
                    put(img, x, y, (255, 230, 150))
        return img
    if nome == "advanced_generator":
        img = involucro(rampa((120, 110, 70)), seme="agen")
        griglia(img, 3, 3, 12, 5, rampa((120, 110, 70)))
        pannello(img, 3, 7, 12, 12, fondo=(26, 22, 20))
        for x in range(4, 12):
            for y in range(8, 12):
                c = ((255, 230, 120) if y == 11 else (255, 160, 40) if (x + y) % 2 else (230, 90, 20)) if a \
                    else ((44, 38, 34) if (x + y) % 2 else (30, 28, 26))
                put(img, x, y, c)
        return img
    if nome == "logistics_core":
        r = rampa((60, 200, 190))
        img = involucro(rampa((80, 92, 100)), seme="core")
        pannello(img, 3, 3, 12, 12, fondo=(16, 30, 34))
        for (x, y) in [(5, 5), (10, 5), (5, 10), (10, 10), (7, 7), (8, 8), (7, 8), (8, 7)]:
            put(img, x, y, r[5] if a else r[1])
        for i in range(6, 10):
            put(img, i, 5, r[3] if a else r[0])
            put(img, i, 10, r[3] if a else r[0])
            put(img, 5, i, r[3] if a else r[0])
            put(img, 10, i, r[3] if a else r[0])
        return img
    if nome == "mining_mole":
        r = rampa((170, 120, 70))
        img = involucro(r, seme="mole")
        # punta di trapano a spirale
        for y in range(3, 13):
            w = max(0, 5 - abs(y - 7))
            for x in range(8 - w, 8 + w):
                put(img, x, y, ACCIAIO[5] if (x + y) % 3 == 0 else ACCIAIO[3])
        put(img, 7, 7, (255, 200, 60) if a else ACCIAIO[1])
        put(img, 8, 7, (255, 200, 60) if a else ACCIAIO[1])
        return img
    if nome == "water_mill":
        img = involucro(rampa((110, 80, 50)), seme="wm")
        for y in range(16):
            for x in range(16):
                d = math.hypot(x - 7.5, y - 7.5)
                if 5.2 < d < 6.2:
                    put(img, x, y, (90, 60, 36))
                ang = math.atan2(y - 7.5, x - 7.5)
                if d < 5.2 and abs(math.sin(ang * 4)) < 0.25:
                    put(img, x, y, (150, 110, 70))
        put(img, 7, 7, ACCIAIO[4])
        put(img, 8, 8, ACCIAIO[2])
        if a:
            for x in range(3, 13, 3):
                put(img, x, 13, (90, 150, 230))
        return img
    if nome == "wind_mill":
        img = involucro(seme="wind")
        for i in range(-5, 6):
            put(img, 8 + i, 8 + i // 3, (236, 236, 240))
            put(img, 8 - i // 3, 8 + i, (236, 236, 240))
        put(img, 8, 8, (200, 60, 50) if a else ACCIAIO[2])
        return img
    raise ValueError(nome)


def pannello_solare_livello(livello):
    img = involucro(rampa((60, 64, 80)) if livello == 3 else ACCIAIO, seme=f"solar{livello}")
    celle = (80, 50, 160) if livello == 3 else (30, 96, 180)
    _celle_solari(img, celle, 3 if livello == 3 else 4, (220, 200, 90) if livello == 3 else ACCIAIO[5])
    return img


def baule(materiale, parte):
    """Bauli: corpo del materiale con grana, coperchio separato, angolari scuri e serratura."""
    colori = {"copper": (196, 108, 68), "iron": (186, 188, 194), "gold": (238, 196, 62),
              "diamond": (96, 214, 214), "obsidian": (52, 38, 72)}
    r = rampa(colori[materiale])
    ferr = rampa((70, 70, 78)) if materiale != "obsidian" else rampa((120, 70, 170))
    n = grana("baule" + materiale + parte, 0.7)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            v = n[y][x] * 0.5
            if materiale in ("copper", "obsidian"):
                v += 0.25 * math.sin(x * 1.7 + y * 0.3)  # venature
            c = tono(r, v)
            if x in (0, 15) or y in (0, 15):
                c = r[1]
            elif x == 1 or y == 1:
                c = mix(r[4], r[5], 0.3)
            elif x == 14 or y == 14:
                c = r[2]
            if parte != "top":
                if y == 5:
                    c = r[1]
                elif y == 6:
                    c = r[4]
            else:
                if (x in (3, 12) or y in (3, 12)) and 2 <= x <= 13 and 2 <= y <= 13:
                    c = r[2]
            img.putpixel((x, y), c + (255,))
    # angolari di metallo a "L" sugli spigoli
    for (cx, cy, sx, sy) in [(0, 0, 1, 1), (15, 0, -1, 1), (0, 15, 1, -1), (15, 15, -1, -1)]:
        for k in range(3):
            put(img, cx + sx * k, cy, ferr[2] if k < 2 else ferr[3])
            put(img, cx, cy + sy * k, ferr[2] if k < 2 else ferr[3])
        put(img, cx + sx, cy + sy, ferr[4])
    if parte == "front":
        for x in range(6, 10):
            for y in range(3, 9):
                put(img, x, y, ferr[4] if (x == 6 or y == 3) else ferr[1] if (x == 9 or y == 8) else ferr[3])
        put(img, 7, 5, (20, 20, 24) if materiale != "obsidian" else (210, 170, 255))
        put(img, 7, 6, (20, 20, 24) if materiale != "obsidian" else (170, 120, 230))
    return img


def carbonaia(parte, acceso=False):
    """Carbonaia: mattoni scuri ognuno con il suo tono, luce sul bordo alto e ombra sotto, malta granulosa."""
    mattone = rampa((124, 68, 50))
    rng = random.Random("kiln" + parte)
    toni = {}
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            riga = y // 4
            sfasa = 4 if riga % 2 else 0
            col = (x + sfasa) // 8
            malta = y % 4 == 3 or (x + sfasa) % 8 == 7
            if malta:
                c = mix((96, 90, 84), (120, 114, 106), rng.random() * 0.6)
            else:
                k = (riga, col % 2)
                if k not in toni:
                    toni[k] = rng.uniform(-0.6, 0.5)
                c = tono(mattone, toni[k] + rng.uniform(-0.15, 0.15))
                if y % 4 == 0:
                    c = schiarisci(c, 0.15)
                elif y % 4 == 2:
                    c = scurisci(c, 0.15)
            img.putpixel((x, y), c + (255,))
    if parte == "top":
        pannello(img, 5, 5, 10, 10, rampa((80, 76, 72)), (20, 18, 18))
    elif parte == "side":
        pannello(img, 4, 8, 11, 13, rampa((80, 76, 72)), (24, 20, 18))
        if acceso:
            for x in range(5, 11):
                for y in range(9, 13):
                    put(img, x, y, (255, 200, 80) if y == 12 else (255, 160, 50) if (x + y) % 2 else (220, 90, 20))
    return img


def segnalino():
    img = tela()
    for y in range(6, 16):
        put(img, 7, y, (120, 90, 60))
        put(img, 8, y, (90, 66, 44))
    for (x, y) in [(7, 4), (8, 4), (6, 5), (7, 5), (8, 5), (9, 5), (7, 6), (8, 6)]:
        put(img, x, y, (90, 150, 255))
    put(img, 7, 4, (220, 240, 255))
    return img


def telaio():
    """Strisce diagonali giallo/grafite: qualunque pezzetto 4x4 (le barre sottili) resta a strisce"""
    img = tela()
    r = rampa((230, 190, 40))
    for y in range(16):
        for x in range(16):
            giallo = (x + y) % 8 < 4
            c = (r[4] if (x + y) % 8 in (0, 1) else r[3]) if giallo else ((52, 52, 58) if (x + y) % 8 != 7 else (70, 70, 78))
            put(img, x, y, c)
    return img


def testa_trapano():
    img = involucro(seme="drill", bulloni=False)
    for y in range(16):
        for x in range(4, 12):
            put(img, x, y, ACCIAIO[5] if (x + y) % 4 == 0 else ACCIAIO[3])
    return img


def circuito_avanzato():
    img = tela()
    for y in range(2, 14):
        for x in range(2, 14):
            put(img, x, y, (30, 40, 110) if (x + y) % 2 else (36, 48, 124))
    for i in range(3, 13):
        put(img, i, 5, (240, 200, 70))
        put(img, i, 10, (240, 200, 70))
    for (x, y) in [(5, 7), (6, 7), (9, 8), (10, 8)]:
        put(img, x, y, (200, 50, 50))
    pannello(img, 6, 6, 9, 9, rampa((30, 30, 30)), (20, 20, 20))
    put(img, 7, 7, (170, 90, 230))
    return contorno(img, (14, 16, 40))


# ---------------------------------------------------------------------------
# Fase 11: utensili e armi elettrici (disegni originali)
# ---------------------------------------------------------------------------
ARANCIO = rampa((222, 120, 40))
GRAFITE = rampa((60, 64, 74))


def trapano(punta):
    """Corpo arancio con impugnatura, punta conica a spirale verso l'alto a destra"""
    img = tela()
    rp = rampa(punta)
    corpo = forma(lambda u, v: 2.2 <= u <= 8.6 and abs(v) <= 2.4)
    impugnatura = forma(lambda u, v: 3.0 <= u <= 5.2 and 2.4 < v <= 5.6)
    cono = forma(lambda u, v: 8.6 < u <= 15.4 and abs(v) <= (15.6 - u) * 0.36 + 0.3)
    ombreggia(img, impugnatura, GRAFITE)
    ombreggia(img, corpo, ARANCIO)
    for (x, y) in corpo:            # fessure di raffreddamento
        u, v = uv(x + 0.5, y + 0.5)
        if 4.0 <= u <= 7.0 and abs(v) < 1.2 and round(u) % 2 == 0:
            put(img, x, y, ARANCIO[1])
    ombreggia(img, cono, rp)
    for (x, y) in cono:             # spirale
        u, v = uv(x + 0.5, y + 0.5)
        if round(u * 1.4 + v) % 3 == 0:
            put(img, x, y, rp[1])
    put(img, *[(x, y) for (x, y) in corpo if uv(x + 0.5, y + 0.5)[0] < 3.5][0], (80, 220, 255))
    return contorno(img, mix(GRAFITE[0], (10, 10, 14), 0.5))


def motosega():
    img = tela()
    corpo = forma(lambda u, v: 1.8 <= u <= 7.6 and -2.6 <= v <= 3.2)
    barra = forma(lambda u, v: 7.6 < u <= 15.2 and abs(v + 0.3) <= 1.5)
    ombreggia(img, corpo, ARANCIO)
    ombreggia(img, barra, ACCIAIO)
    for (x, y) in barra:            # denti della catena sul bordo
        u, v = uv(x + 0.5, y + 0.5)
        if abs(v + 0.3) > 0.8 and (x + y) % 2 == 0:
            put(img, x, y, GRAFITE[1])
    for (x, y) in corpo:
        u, v = uv(x + 0.5, y + 0.5)
        if 2.6 <= u <= 4.0 and v > 1.2:
            put(img, x, y, GRAFITE[2])
    return contorno(img, mix(GRAFITE[0], (10, 10, 14), 0.5))


def lama_plasma():
    img = tela()
    elsa = forma(lambda u, v: 0.6 <= u <= 5.0 and abs(v) <= 1.3)
    guardia = forma(lambda u, v: 4.6 <= u <= 5.8 and abs(v) <= 3.0)
    lama = forma(lambda u, v: 5.8 < u <= 15.8 and abs(v) <= 1.2)
    ombreggia(img, elsa, GRAFITE)
    ombreggia(img, guardia, ACCIAIO)
    for (x, y) in lama:
        u, v = uv(x + 0.5, y + 0.5)
        put(img, x, y, (235, 255, 255) if abs(v) < 0.55 else (70, 220, 255))
    for (x, y) in elsa:
        u, v = uv(x + 0.5, y + 0.5)
        if round(u) % 2 == 0:
            put(img, x, y, GRAFITE[4])
    return contorno(img, (12, 30, 60))


FUCILE = [
    "................",
    "................",
    "................",
    "................",
    "..........CC....",
    ".AAAAAAAAAAAAAA.",
    "AAGGGGGGGGGGGGCC",
    "AAGGGGGGYYYGGG..",
    ".GGGG.GAAA......",
    ".GGG..GA........",
    ".GGG............",
    ".GG.............",
    "................",
    "................",
    "................",
    "................",
]


def fucile():
    img = tela()
    for y, riga in enumerate(FUCILE):
        for x, ch in enumerate(riga):
            if ch == "A":
                put(img, x, y, ACCIAIO[4] if y <= 5 else ACCIAIO[2])
            elif ch == "G":
                put(img, x, y, GRAFITE[3] if y < 8 else GRAFITE[2])
            elif ch == "C":
                put(img, x, y, (90, 230, 255))
            elif ch == "Y":
                put(img, x, y, (170, 90, 230))
    return contorno(img, (12, 12, 18))


def armatura_exo(tipo, colore, luce):
    """Pezzo dell'esoarmatura: piastre col colore del livello e una striscia luminosa"""
    img = armatura(tipo, colore)
    m = maschera_da_disegno(ARMATURE[tipo])
    righe = sorted({y for (_, y) in m})
    y = righe[len(righe) // 2]
    for (x, yy) in m:
        if yy == y:
            put(img, x, yy, luce)
    return img


def armatura_exo_indossata(colore, luce, strato):
    img = armatura_indossata(colore, strato)
    for x in range(64):
        for y in (4, 20):
            if img.getpixel((x, y))[3] and x % 8 in (3, 4):
                img.putpixel((x, y), luce + (255,))
    return img


COLORI_EXO_LIVELLO = {"exo_mk1": ((176, 180, 188), (90, 230, 255)), "exo_mk3": ((52, 46, 56), (190, 110, 255))}


def fase11():
    n = 0
    salva(trapano((200, 200, 205)), "item/electric_drill.png")
    salva(trapano((120, 235, 235)), "item/diamond_drill.png")
    salva(motosega(), "item/chainsaw.png")
    salva(lama_plasma(), "item/plasma_saber.png")
    salva(fucile(), "item/pulse_rifle.png")
    n += 5
    for nome, (colore, luce) in COLORI_EXO_LIVELLO.items():
        for tipo in ARMATURE:
            salva(armatura_exo(tipo, colore, luce), f"item/{nome}_{tipo}.png")
            n += 1
        for strato in (1, 2):
            salva(armatura_exo_indossata(colore, luce, strato), f"models/armor/{nome}_layer_{strato}.png")
            n += 1
    return n


# ---------------------------------------------------------------------------
# Fasi 12-14: casa automatica, Nether, robot
# ---------------------------------------------------------------------------
def fronte_nuove(nome, a):
    colore, simbolo = IDENTITA[nome]
    r = rampa(colore)
    img = involucro(seme=nome + "_f")
    pannello(img, 3, 3, 12, 12, fondo=(22, 24, 30))
    if nome == "tree_farm":
        for y in range(4, 12):
            for x in range(4, 12):
                if y >= 9 and x in (7, 8):
                    put(img, x, y, (110, 80, 50))
                elif y < 9 and (x - 7.5) ** 2 + (y - 6) ** 2 < 9:
                    put(img, x, y, (90, 190, 70) if a else (50, 100, 44))
    elif nome == "planter":
        for x in range(4, 12):
            put(img, x, 11, (110, 80, 50))
            if x % 2 == 0:
                put(img, x, 10, (120, 210, 70) if a else (70, 120, 50))
                put(img, x, 9, (120, 210, 70) if a else (60, 100, 44))
        put(img, 7, 5, (230, 200, 90))
        put(img, 8, 6, (230, 200, 90))
    elif nome == "auto_fertilizer":
        for i in range(4, 12):
            put(img, i, i, (240, 240, 230))
            put(img, i, 15 - i, (240, 240, 230))
        if a:
            for (x, y) in [(5, 9), (10, 6), (7, 4), (9, 11)]:
                put(img, x, y, (110, 230, 90))
    elif nome == "energy_switch":
        put(img, 7, 10, ACCIAIO[2])
        put(img, 8, 10, ACCIAIO[2])
        for i in range(4):
            put(img, (9 + i) if a else (6 - i), 9 - i, (120, 90, 60))
        put(img, 12 if a else 3, 5, (255, 80, 60) if a else (120, 50, 40))
    elif nome == "electric_enchanter":
        for y in range(6, 11):
            for x in range(4, 12):
                put(img, x, y, (120, 40, 40) if x != 7 and x != 8 else (230, 220, 200))
        if a or True:
            for (x, y) in [(5, 4), (10, 4), (7, 3), (4, 12), (11, 12)]:
                put(img, x, y, r[5])
    elif nome == "mob_incubator":
        for y in range(4, 12):
            for x in range(4, 12):
                put(img, x, y, (40, 120, 130) if (x + y) % 3 else (60, 160, 170))
        for (x, y) in [(7, 6), (8, 6), (6, 7), (9, 7), (7, 8), (8, 8), (7, 9), (8, 9), (6, 10), (9, 10)]:
            put(img, x, y, (190, 255, 250) if a else (20, 60, 66))
    elif nome == "laser_turret":
        for y in range(4, 12):
            for x in range(4, 12):
                d = math.hypot(x - 7.5, y - 7.5)
                if d < 3.8:
                    put(img, x, y, (255, 90, 80) if a and d < 1.6 else (180, 30, 30) if d < 2.6 else ACCIAIO[1])
    elif nome == "reactor_maintainer":
        for i, c in enumerate([(70, 160, 230), (120, 230, 90), (70, 160, 230)]):
            for y in range(5, 11):
                put(img, 4 + i * 3, y, c if a or y > 7 else mix(c, (30, 30, 30), 0.5))
                put(img, 5 + i * 3, y, mix(c, (255, 255, 255), 0.3) if a or y > 7 else mix(c, (30, 30, 30), 0.6))
        for x in range(4, 12):
            put(img, x, 11, ACCIAIO[4])
    elif nome == "enchant_researcher":
        # libro aperto con le pagine che brillano quando ricerca
        for y in range(5, 11):
            for x in range(4, 12):
                if x in (7, 8):
                    put(img, x, y, (90, 50, 40))
                else:
                    put(img, x, y, (235, 225, 200) if (y % 2 or not a) else (200, 170, 255))
        for x in range(4, 12):
            put(img, x, 11, (120, 70, 50))
        if a:
            for (x, y) in [(5, 3), (10, 3), (8, 2), (3, 6), (12, 6)]:
                put(img, x, y, (200, 160, 255))
    elif nome == "robot_dock":
        for x in range(4, 12):
            put(img, x, 11, r[3])
            put(img, x, 10, r[2])
        for (x, y) in [(7, 5), (8, 5), (6, 6), (9, 6), (7, 7), (8, 8), (7, 9)]:
            put(img, x, y, (255, 230, 90) if a else ACCIAIO[2])
    return img


def cima_lato(nome):
    """Cima delle macchine agricole: griglia con il colore della macchina"""
    colore, _ = IDENTITA[nome]
    r = rampa(colore)
    img = involucro(seme=nome + "_top")
    griglia(img, 3, 3, 12, 12, r)
    return img


def lampada(acceso, pannello_sottile=False):
    """Lampada: cornice metallica smussata e diffusore con bagliore piu' forte al centro."""
    img = tela()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                c = ACCIAIO[2] if x == 15 or y == 15 else ACCIAIO[4]
            elif x in (1, 14) or y in (1, 14):
                c = ACCIAIO[3]
            else:
                d = math.hypot(x - 7.5, y - 7.5)
                if acceso:
                    c = mix((255, 252, 230), (255, 214, 140), min(1.0, d / 8))
                else:
                    c = mix((168, 166, 156), (124, 122, 116), min(1.0, d / 8))
                if (x + y) % 6 == 0:
                    c = schiarisci(c, 0.08) if acceso else scurisci(c, 0.06)
            put(img, x, y, c)
    if not pannello_sottile:
        for i in range(2, 14):
            put(img, i, 8, ACCIAIO[3] if not acceso else (255, 220, 150))
            put(img, 8, i, ACCIAIO[3] if not acceso else (255, 220, 150))
    return img


def nucleo(colore, centro):
    img = tela()
    r = rampa(colore)
    m = {(x, y) for x in range(16) for y in range(16) if 2.0 <= math.hypot(x - 7.5, y - 7.5) <= 6.0}
    ombreggia(img, m, r)
    for (x, y) in {(x, y) for x in range(16) for y in range(16) if math.hypot(x - 7.5, y - 7.5) < 2.2}:
        put(img, x, y, centro)
    return contorno(img, mix(r[0], (0, 0, 0), 0.5))


def campionatore():
    img = tela()
    vetro = {(x, y) for x in range(5, 11) for y in range(5, 13)}
    ombreggia(img, vetro, rampa((150, 200, 220)))
    for (x, y) in vetro:
        if y >= 9:
            put(img, x, y, (190, 80, 230) if (x + y) % 3 else (230, 140, 255))
    for x in range(6, 10):
        put(img, x, 4, ACCIAIO[3])
        put(img, x, 3, ACCIAIO[4])
    for y in range(0, 3):
        put(img, 7, y, (240, 160, 40))
        put(img, 8, y, (200, 120, 30))
    return contorno(img, (20, 20, 30))


def testa_robot(colore, occhi):
    img = tela()
    r = rampa(colore)
    testa = {(x, y) for x in range(3, 13) for y in range(3, 13)}
    ombreggia(img, testa, r)
    for x in range(4, 12):
        put(img, x, 6, (20, 22, 28))
        put(img, x, 7, (20, 22, 28))
    for x in (5, 6, 9, 10):
        put(img, x, 6, occhi)
    put(img, 7, 1, ACCIAIO[4])
    put(img, 8, 2, ACCIAIO[3])
    put(img, 7, 2, ACCIAIO[3])
    for x in range(5, 11):
        put(img, x, 10, r[1] if x % 2 else r[4])
    return contorno(img, (14, 16, 22))


COLORI_ROBOT = [(120, 170, 80), (70, 110, 200), (200, 140, 50), (150, 110, 80), (170, 60, 50)]


def pelle_robot(colore):
    """Texture 64x64 (stessa disposizione di un "umanoide"): corpo metallico col colore del lavoro"""
    r = rampa(colore)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    rng = random.Random(str(colore))

    def area(x0, y0, w, h, base, motivo=True):
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                c = base[3]
                if motivo and ((x - x0) % 4 == 0 or (y - y0) % 4 == 0):
                    c = base[2]
                if rng.random() < 0.05:
                    c = base[4]
                img.putpixel((x, y), c + (255,))

    # testa (0,0)-(32,16): acciaio, faccia frontale con visore
    area(0, 0, 32, 16, ACCIAIO)
    for x in range(9, 15):
        for y in (11, 12):
            img.putpixel((x, y), (20, 22, 28, 255))
    for x in (10, 13):
        img.putpixel((x, 11), (120, 240, 255, 255))
    # corpo (16,16)-(40,32) col colore del lavoro, petto con luce
    area(16, 16, 24, 16, r)
    for y in range(22, 25):
        for x in range(22, 26):
            img.putpixel((x, y), (120, 240, 255, 255))
    # braccia (40,16)-(56,32) e (32,48)-(48,64): acciaio
    area(40, 16, 16, 16, ACCIAIO)
    area(32, 48, 16, 16, ACCIAIO)
    # gambe (0,16)-(16,32) e (16,48)-(32,64): grafite
    area(0, 16, 16, 16, GRAFITE)
    area(16, 48, 16, 16, GRAFITE)
    return img


def archivio_incantesimi():
    """Tavoletta-libro elettronica viola con righe di testo luminose"""
    img = tela()
    corpo = {(x, y) for x in range(3, 13) for y in range(2, 15)}
    ombreggia(img, corpo, rampa((90, 60, 150)))
    for y in range(4, 13):
        for x in range(5, 11):
            put(img, x, y, (40, 30, 70))
    for y in (5, 7, 9, 11):
        for x in range(5, 5 + (3 + (y * 7) % 5)):
            put(img, x, y, (200, 160, 255))
    put(img, 12, 3, (255, 220, 120))
    return contorno(img, (20, 12, 36))


def pannello_controllo():
    """Tablet con schermo azzurro e antenna"""
    img = tela()
    corpo = {(x, y) for x in range(3, 13) for y in range(4, 15)}
    ombreggia(img, corpo, GRAFITE)
    for x in range(4, 12):
        for y in range(5, 11):
            put(img, x, y, (60, 190, 230) if (x + y) % 5 else (150, 235, 255))
    for (x, y) in [(5, 7), (6, 8), (7, 7), (8, 8), (9, 7), (10, 8)]:
        put(img, x, y, (20, 60, 90))
    put(img, 5, 12, (230, 70, 60))
    put(img, 7, 12, (90, 220, 90))
    put(img, 9, 12, (240, 200, 60))
    for y in range(0, 4):
        put(img, 11, y, ACCIAIO[3])
    put(img, 11, 0, (230, 70, 60))
    return contorno(img, (14, 16, 22))


def tnt_nucleare(parte):
    """TNT nucleare: verde scuro con fasce gialle e il simbolo della radioattivita'"""
    img = tela()
    verde = rampa((60, 110, 50))
    giallo = (240, 210, 40)
    for y in range(16):
        for x in range(16):
            c = verde[3] if (x * 7 + y * 3) % 11 else verde[2]
            put(img, x, y, c)
    if parte == "side":
        for x in range(16):
            for y in (2, 3, 12, 13):
                put(img, x, y, giallo if (x + y) % 4 < 2 else (30, 30, 30))
    centro = (7.5, 7.5)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - centro[0], y - centro[1])
            ang = math.degrees(math.atan2(y - centro[1], x - centro[0])) % 360
            if parte != "bottom" and (d < 1.3 or (2.4 < d < 4.6 and int((ang + 90) // 60) % 2 == 0)):
                put(img, x, y, (30, 30, 30))
            elif parte != "bottom" and d < 5.2 and parte == "top":
                put(img, x, y, giallo)
    return img


def fase12_14():
    n = 0
    for nome in ["tree_farm", "planter", "auto_fertilizer", "energy_switch", "electric_enchanter", "mob_incubator",
                 "enchant_researcher",
                 "laser_turret", "robot_dock"]:
        salva(lato_identita(nome), f"block/{nome}_side.png")
        salva(fronte_nuove(nome, False), f"block/{nome}_front.png")
        salva(fronte_nuove(nome, True), f"block/{nome}_front_on.png")
        salva(cima_lato(nome), f"block/{nome}_top.png")
        n += 4
    salva(lampada(False), "block/electric_lamp.png")
    salva(lampada(True), "block/electric_lamp_on.png")
    salva(lampada(False, True), "block/light_panel.png")
    salva(lampada(True, True), "block/light_panel_on.png")
    salva(nucleo((70, 200, 200), (210, 255, 250)), "item/soul_core.png")
    salva(nucleo((180, 90, 240), (255, 255, 255)), "item/infinite_core.png")
    salva(campionatore(), "item/mob_sampler.png")
    salva(testa_robot(ACCIAIO[3], (120, 240, 255)), "item/robot.png")
    salva(testa_robot((90, 94, 104), (60, 60, 70)), "item/robot_chassis.png")
    salva(pannello_controllo(), "item/robot_controller.png")
    salva(archivio_incantesimi(), "item/enchant_archive.png")
    for parte in ("side", "top", "bottom"):
        salva(tnt_nucleare(parte), f"block/nuclear_tnt_{parte}.png")
    salva(lato_identita("reactor_maintainer"), "block/reactor_maintainer_side.png")
    salva(fronte_nuove("reactor_maintainer", False), "block/reactor_maintainer_front.png")
    salva(fronte_nuove("reactor_maintainer", True), "block/reactor_maintainer_front_on.png")
    salva(cima_lato("reactor_maintainer"), "block/reactor_maintainer_top.png")
    n += 9
    for i, c in enumerate(COLORI_ROBOT):
        salva(pelle_robot(c), f"entity/robot_{i}.png")
        n += 1
    return n


# ---------------------------------------------------------------------------
# Scrittura
# ---------------------------------------------------------------------------
def salva(img, rel):
    p = TEX / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    img.save(p)


def main():
    n = 0
    # attrezzi e armi
    manici = {"bronze": LEGNO, "steel": LEGNO, "nocturnite": rampa((120, 200, 230)), "ardorite": rampa((88, 58, 120))}
    for m, legno in manici.items():
        for tipo in ["sword", "pickaxe", "axe", "shovel", "hoe"] + (["hammer"] if m in ("nocturnite", "ardorite") else []):
            salva(attrezzo(tipo, MATERIALI[m], legno), f"item/{m}_{tipo}.png")
            n += 1
        for tipo in ARMATURE:
            salva(armatura(tipo, MATERIALI[m]), f"item/{m}_{tipo}.png")
            n += 1
        for strato in (1, 2):
            salva(armatura_indossata(MATERIALI[m], strato), f"models/armor/{m}_layer_{strato}.png")
    salva(attrezzo("hammer", (150, 150, 155)), "item/forge_hammer.png")
    n += 1
    # materiali dei metalli
    for m in ["tin", "lead", "silver", "uranium", "bronze", "steel"]:
        c = MATERIALI[m]
        salva(lingotto(c), f"item/{m}_ingot.png")
        salva(pepita(c), f"item/{m}_nugget.png")
        salva(piastra(c), f"item/{m}_plate.png")
        salva(polvere(c, m), f"item/{m}_dust.png")
        salva(blocco_metallo(c, m), f"block/{m}_block.png")
        n += 5
        if m in ("tin", "lead", "silver", "uranium"):
            salva(grezzo(c, m), f"item/raw_{m}.png")
            salva(blocco_grezzo(c, m), f"block/raw_{m}_block.png")
            salva(minerale_stile(m, c, m), f"block/{m}_ore.png")
            salva(minerale_stile(m, c, m + "_d", True), f"block/deepslate_{m}_ore.png")
            n += 4
    for m in ["iron", "gold", "copper"]:
        salva(piastra(MATERIALI[m]), f"item/{m}_plate.png")
        salva(polvere(MATERIALI[m], m), f"item/{m}_dust.png")
        n += 2
    salva(polvere((50, 50, 54), "coal"), "item/coal_dust.png")
    salva(polvere((120, 230, 230), "diamond"), "item/diamond_dust.png")
    salva(polvere((60, 40, 90), "obsidian"), "item/obsidian_dust.png")
    voltite = (170, 90, 230)
    salva(minerale_stile("voltite", voltite, "voltite"), "block/voltite_ore.png")
    salva(minerale_stile("voltite", voltite, "voltite_d", True), "block/deepslate_voltite_ore.png")
    n += 5
    # macchine
    salva(involucro(giunta=True, seme="side"), "block/machine_side.png")
    salva(cima_macchina(), "block/machine_top.png")
    salva(fondo_macchina(), "block/machine_bottom.png")
    frame = involucro(seme="frame")
    griglia(frame, 4, 4, 11, 11)
    salva(frame, "block/machine_frame.png")
    n += 4
    for nome in ["combustion_generator", "geothermal_generator", "crusher", "electric_furnace", "compressor",
                 "extractor", "wire_press", "chemical_bath", "thermal_centrifuge", "growth_accelerator", "harvester",
                 "quarry", "reactor_core"]:
        salva(fronte(nome, False), f"block/{nome}_front.png")
        salva(fronte(nome, True), f"block/{nome}_front_on.png")
        n += 2
    salva(fronte("network_terminal", True), "block/network_terminal_front.png")
    salva(pannello_solare(), "block/solar_panel_top.png")
    rs = rampa((90, 100, 96))
    salva(involucro(rs, giunta=True, seme="reactor_side"), "block/reactor_side.png")
    camera = involucro(rs, seme="chamber")
    griglia(camera, 3, 3, 12, 12, rs)
    salva(camera, "block/reactor_chamber.png")
    salva(involucro(rampa((170, 140, 60)), giunta=True, seme="qs"), "block/quarry_side.png")
    for liv, suff in [(1, ""), (2, "_2"), (3, "_3")]:
        for parte in ["front", "side", "top"]:
            salva(banco(liv, parte), f"block/battery_box{suff}_{parte}.png")
            n += 1
    salva(logistica_lato(), "block/logistics_side.png")
    salva(involucro(seme="acs", giunta=True), "block/auto_crafter_side.png")
    salva(cima_assemblatore(), "block/auto_crafter_top.png")
    n += 8
    # cavi e condotti
    for nome, (base, striscia) in {
        "tin_cable": ((176, 184, 192), None), "insulated_copper_cable": ((52, 50, 56), (226, 123, 82)),
        "insulated_gold_cable": ((52, 50, 56), (240, 200, 70)), "voltite_fiber": ((170, 90, 230), (240, 210, 255)),
    }.items():
        salva(cavo(base, striscia), f"block/{nome}.png")
        n += 1
    for nome, (base, vetro) in {
        "item_conduit": ((150, 150, 150), (190, 215, 228)), "extractor_conduit": ((150, 112, 80), (220, 170, 100)),
        "fast_extractor_conduit": ((210, 175, 60), (255, 230, 130)), "sorting_conduit": ((110, 90, 160), (190, 160, 240)),
        "analyzer_conduit": ((50, 120, 110), (120, 230, 200)),
    }.items():
        salva(condotto(base, vetro), f"block/{nome}.png")
        n += 1
    # alchimia
    salva(tavolo_sintesi("top"), "block/synthesis_table_top.png")
    salva(tavolo_sintesi("side"), "block/synthesis_table_side.png")
    for nome, colore in [("star_collector", (240, 200, 70)), ("star_collector_2", (240, 130, 60)),
                         ("star_collector_3", (190, 100, 255))]:
        salva(captatore(colore), f"block/{nome}.png")
    salva(replicatore("top"), "block/replicator_top.png")
    salva(replicatore("side"), "block/replicator_side.png")
    salva(blocco_metallo((88, 58, 120), "noc"), "block/nocturnite_block.png")
    salva(blocco_metallo((196, 44, 52), "ard"), "block/ardorite_block.png")
    salva(carica_stellare("top"), "block/star_charge_top.png")
    salva(carica_stellare("side"), "block/star_charge_side.png")
    n += 11
    # comodita'
    salva(fornace_ferro("front"), "block/iron_furnace_front.png")
    salva(fornace_ferro("front", True), "block/iron_furnace_front_on.png")
    salva(fornace_ferro("side"), "block/iron_furnace_side.png")
    salva(fornace_ferro("top"), "block/iron_furnace_top.png")
    for liv, colore in COLORI_RILEVAMENTO.items():
        salva(polvere(colore, f"scan{liv}"), f"item/scanning_dust_{liv}.png")
        salva(rilevatore(colore), f"item/ore_scanner_{liv}.png")
    salva(sacca(), "item/miner_bag.png")
    n += 11
    # Fase 10
    for nome in IDENTITA:
        salva(lato_identita(nome), f"block/{nome}_side.png")
        n += 1
    for nome in ["advanced_crusher", "induction_furnace", "advanced_generator", "logistics_core", "mining_mole",
                 "water_mill", "wind_mill"]:
        salva(fronte_extra(nome, False), f"block/{nome}_front.png")
        salva(fronte_extra(nome, True), f"block/{nome}_front_on.png")
        n += 2
    salva(pannello_solare_livello(2), "block/solar_2_top.png")
    salva(pannello_solare_livello(3), "block/solar_3_top.png")
    for m in ["copper", "iron", "gold", "diamond", "obsidian"]:
        for parte in ["front", "side", "top"]:
            salva(baule(m, parte), f"block/{m}_chest_{parte}.png")
            n += 1
    salva(carbonaia("top"), "block/charcoal_kiln_top.png")
    salva(carbonaia("side"), "block/charcoal_kiln_side.png")
    salva(carbonaia("side", True), "block/charcoal_kiln_side_on.png")
    salva(segnalino(), "block/area_marker.png")
    salva(telaio(), "block/quarry_frame.png")
    salva(testa_trapano(), "block/quarry_drill.png")
    salva(circuito_avanzato(), "item/advanced_circuit.png")
    n += 9
    n += fase11()
    n += fase12_14()
    print(f"Grafica v2: {n} texture ridisegnate")


if __name__ == "__main__":
    main()
