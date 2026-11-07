"""
FutureTek - Blocchi da costruzione (ispirati alle pietre e ai legni colorati dei vecchi modpack tecnici,
ma con nomi, disegni e implementazione NOSTRI).

Disegna da zero le texture 16x16 in stile Minecraft attuale (rampe di 5-6 toni, rumore morbido,
fughe dei mattoni, smussature) e scrive la specifica tools/blocchi_costruzione.json che la sessione
di integrazione usa per registrare blocchi, scale, lastre, muretti, staccionate, ricette e generazione.

Uso (dalla cartella del progetto):  python tools/blocchi_costruzione.py [--anteprima FILE.png]
Nessuna texture e' copiata o ricalcata da Minecraft o da altre mod: tutto e' generato da formule.
"""
import json
import math
import random
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/futuretek/textures/block"
SPEC = ROOT / "tools/blocchi_costruzione.json"
S = 16


# ---------------------------------------------------------------------------
# strumenti di base
# ---------------------------------------------------------------------------
def clamp(v):
    return max(0, min(255, int(round(v))))


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


def rampa(base, n=6, scuro=0.45, chiaro=0.35):
    """n toni dal piu' scuro al piu' chiaro attorno al colore base (leggero spostamento di tinta come vanilla)"""
    out = []
    for i in range(n):
        t = i / (n - 1)
        if t < 0.5:
            c = mix(tuple(int(x * (1 - scuro)) for x in base), base, t * 2)
            c = mix(c, (c[0] * 0.9, c[1] * 0.92, c[2] * 1.05), 0.3 * (1 - t * 2))  # ombre un po' fredde
        else:
            c = mix(base, mix(base, (255, 250, 235), chiaro), (t - 0.5) * 2)  # luci un po' calde
        out.append(c)
    return out


def tela():
    return Image.new("RGBA", (S, S), (0, 0, 0, 255))


def put(img, x, y, c):
    img.putpixel((x % S, y % S), (c[0], c[1], c[2], 255))


def get(img, x, y):
    return img.getpixel((x % S, y % S))[:3]


class Rumore:
    """Rumore a valori RIPETIBILE (la texture si ripete senza cuciture): griglia periodica + interpolazione morbida"""

    def __init__(self, seme, celle=4):
        rng = random.Random(seme)
        self.c = celle
        self.v = [[rng.random() for _ in range(celle)] for _ in range(celle)]

    def __call__(self, x, y):
        fx = x / S * self.c
        fy = y / S * self.c
        x0, y0 = int(math.floor(fx)), int(math.floor(fy))
        tx, ty = fx - x0, fy - y0
        tx = tx * tx * (3 - 2 * tx)
        ty = ty * ty * (3 - 2 * ty)
        c = self.c
        a = self.v[y0 % c][x0 % c]
        b = self.v[y0 % c][(x0 + 1) % c]
        d = self.v[(y0 + 1) % c][x0 % c]
        e = self.v[(y0 + 1) % c][(x0 + 1) % c]
        return (a * (1 - tx) + b * tx) * (1 - ty) + (d * (1 - tx) + e * tx) * ty


def frattale(seme, x, y, ottave=((4, 0.6), (8, 0.3), (16, 0.1))):
    return sum(Rumore(f"{seme}-{c}", c)(x, y) * p for c, p in ottave)


def tono(r, v):
    """valore 0..1 -> tono della rampa"""
    return r[max(0, min(len(r) - 1, int(v * len(r))))]


# ---------------------------------------------------------------------------
# GRANA "stile vanilla": macchie piccole (2-4 pixel) + rumore pixel per pixel, quantizzato su 5-7 toni.
# Niente superfici lisce: ogni pixel varia un po' rispetto ai vicini.
# ---------------------------------------------------------------------------
def grana(seme, x, y, macchia=0.8, pixel=0.2):
    """valore 0..1 con macchie a 4-8 celle e grana a 16 celle (= 1 pixel), ripetibile senza cuciture"""
    v = Rumore(seme + "g4", 4)(x, y) * 0.4 + Rumore(seme + "g8", 8)(x, y) * 0.6
    p = Rumore(seme + "g16", 16)(x, y)  # ~ casuale per pixel ma periodico
    return v * macchia + p * pixel


def q(r, v):
    """valore -> tono della rampa (clamp)"""
    return r[max(0, min(len(r) - 1, int(v * len(r))))]


def granuloso(seme, r, lo=0.0, hi=1.0, contrasto=1.25):
    """tela riempita di grana, toni fra lo e hi della rampa"""
    img = tela()
    for y in range(S):
        for x in range(S):
            v = (grana(seme, x, y) - 0.5) * contrasto + 0.5
            put(img, x, y, q(r, lo + (hi - lo) * max(0.0, min(0.999, v))))
    return img


def scurisci(c, t):
    return mix(c, (0, 0, 0), t)


def schiarisci(c, t):
    return mix(c, (255, 255, 255), t)


# ---------------------------------------------------------------------------
# PIETRE
# ---------------------------------------------------------------------------
def pietra(seme, r, macchie=None, vene=None, strati=None):
    """Pietra naturale granulosa come pietra/deepslate: macchie, pori, vene sottili spezzate o strati"""
    img = tela()
    rng = random.Random(seme)
    for y in range(S):
        for x in range(S):
            v = grana(seme, x, y)
            if strati:
                ond = Rumore(seme + "s", 4)(x, y) * 2.0
                v = v * 0.6 + (0.5 + 0.5 * math.sin((y + ond) * strati)) * 0.4
            v = (v - 0.5) * 1.45 + 0.5
            put(img, x, y, q(r, max(0.0, min(0.999, v))))
    if vene:
        colore_vena, _ = vene
        # vene sottili che si spezzano e cambiano direzione (passeggiate periodiche)
        # due vene lunghe e sottili in diagonale (curve periodiche), un po' spezzate e di intensita' variabile
        for pend, amp, fase, inizio in ((1, 2.0, 0.4, 2),):
            for x in range(S):
                yc = inizio + pend * x + amp * math.sin(2 * math.pi * x / S + fase)
                y = int(round(yc))
                f = 0.3 + 0.35 * Rumore(seme + "v", 4)(x, y)
                if rng.random() < 0.9:
                    put(img, x, y, mix(get(img, x, y), colore_vena, f))
                if rng.random() < 0.35:
                    put(img, x, y + 1, mix(get(img, x, y + 1), colore_vena, f * 0.5))
    if macchie:
        colore, quante = macchie
        for _ in range(quante):  # pori / inclusioni a gruppetti
            x, y = rng.randrange(S), rng.randrange(S)
            for dx, dy in ((0, 0), (1, 0), (0, 1)):
                if rng.random() < (1.0 if (dx, dy) == (0, 0) else 0.45):
                    put(img, x + dx, y + dy, mix(colore, get(img, x + dx, y + dy), rng.uniform(0.0, 0.35)))
            put(img, x - 1, y - 1, mix(get(img, x - 1, y - 1), r[-1], 0.3))  # bordino chiaro del poro
    return img


def pietrisco(seme, r):
    """Pietrisco: sassi irregolari granulosi, fughe scure strette e irregolari, luce in alto a sinistra"""
    img = tela()
    rng = random.Random(seme)
    punti = [(rng.uniform(0, S), rng.uniform(0, S)) for _ in range(10)]
    toni_sasso = [rng.uniform(-0.12, 0.12) for _ in punti]

    def vicini(x, y):
        dist = []
        for i, (px, py) in enumerate(punti):
            dx = (x - px + S / 2) % S - S / 2
            dy = (y - py + S / 2) % S - S / 2
            dist.append((dx * dx + dy * dy, i, dx, dy))
        dist.sort()
        return dist

    for y in range(S):
        for x in range(S):
            d = vicini(x, y)
            bordo = math.sqrt(d[1][0]) - math.sqrt(d[0][0])
            i, rx, ry = d[0][1], d[0][2], d[0][3]
            g = grana(seme, x, y, 0.4, 0.6)
            luce = 0.5 + toni_sasso[i] - (rx + ry) * 0.035 + (g - 0.5) * 0.7
            if bordo < 0.75 + (g - 0.5) * 0.6:
                c = q(r, 0.05 + g * 0.15)           # fuga
            elif bordo < 1.5:
                c = q(r, 0.2 + luce * 0.35)          # bordo del sasso in ombra
            else:
                c = q(r, max(0.0, min(0.999, luce)))
            put(img, x, y, c)
    return img


def cornice(img, r, forza=0.35):
    """bordo appena accennato: luce in alto/sinistra, ombra in basso/destra, con un po' di rumore"""
    rng = random.Random(str(r))
    for i in range(S):
        for (x, y, c, t) in ((i, 0, r[-1], forza), (0, i, r[-1], forza * 0.8),
                             (i, S - 1, r[0], forza), (S - 1, i, r[0], forza * 0.8)):
            put(img, x, y, mix(get(img, x, y), c, t * rng.uniform(0.6, 1.2)))


def levigata(seme, r):
    """Pietra levigata: grana fine (meno contrasto della pietra) + bordo smussato appena accennato"""
    img = granuloso(seme, r, 0.25, 0.8, 1.1)
    cornice(img, r, 0.55)
    return img


def mattoni(seme, r, fuga, piccoli=False):
    """Mattoni stile vanilla: file da 4 (3 mattone + 1 fuga), mattoni lunghi 8 sfalsati di 4; ogni mattone granuloso,
    luce sul bordo alto e ombra su quello basso; la fuga e' scura ma non uniforme"""
    img = tela()
    rng = random.Random(seme)
    lung = 8 if not piccoli else 4
    for y in range(S):
        fila = y // 4
        off = 4 * (fila % 2) + (1 if fila == 3 else 0)
        for x in range(S):
            xx = (x + off) % S
            col = xx // lung
            iy, ix = y % 4, xx % lung
            g = grana(seme, x, y)
            if iy == 3 or ix == lung - 1:
                put(img, x, y, mix(fuga, r[1], 0.25 + max(0.0, (g - 0.4)) * 0.6))
                continue
            shade = ((fila * 7 + col * 13) % 5 - 2) * 0.04
            v = 0.5 + shade + (g - 0.5) * 1.0
            if iy == 0:
                v += 0.18
            elif iy == 2:
                v -= 0.12
            if ix == 0:
                v += 0.08
            elif ix == lung - 2:
                v -= 0.08
            put(img, x, y, q(r, max(0.0, min(0.999, v))))
    return img


def crepe(img, seme, colore, quante=3):
    """Crepe scure irregolari con un bordino chiaro (come i mattoni crepati vanilla)"""
    rng = random.Random(seme)
    img = img.copy()
    for _ in range(quante):
        x, y = rng.randrange(S), rng.randrange(S)
        for _ in range(rng.randint(5, 10)):
            put(img, x, y, colore)
            if rng.random() < 0.5:
                put(img, x - 1, y, mix(get(img, x - 1, y), (255, 255, 255), 0.12))
            x += rng.choice((-1, 0, 1, 1))
            y += rng.choice((0, 1, 1))
    return img


def piastrelle(seme, r, fuga):
    """Piastrelle 2x2 granulose: fuga stretta e irregolare, bordo luce/ombra appena accennato"""
    img = tela()
    for y in range(S):
        for x in range(S):
            tx, ty = x // 8, y // 8
            ix, iy = x % 8, y % 8
            g = grana(seme, x, y)
            if ix == 7 or iy == 7:
                put(img, x, y, mix(fuga, r[1], 0.2 + max(0.0, g - 0.45) * 0.6))
                continue
            v = 0.5 + ((tx + ty * 2) % 3 - 1) * 0.04 + (g - 0.5) * 0.9
            if ix == 0 or iy == 0:
                v += 0.15
            elif ix == 6 or iy == 6:
                v -= 0.12
            put(img, x, y, q(r, max(0.0, min(0.999, v))))
    return img


def cesellata(seme, r, fuga):
    """Cesellata: base levigata granulosa; cornice incisa a 2 pixel dal bordo, quadrato centrale in rilievo
    (luce sopra/sinistra, ombra sotto/destra) con un piccolo incavo al centro"""
    img = levigata(seme, r)

    def tinta(x, y, c, t):
        put(img, x, y, mix(get(img, x, y), c, t))

    for i in range(2, 14):            # cornice incisa: solco scuro + bordo chiaro sotto/destra
        tinta(i, 2, r[0], 0.6)
        tinta(2, i, r[0], 0.6)
        tinta(i, 13, r[-1], 0.35)
        tinta(13, i, r[-1], 0.35)
    for i in range(5, 11):            # quadrato in rilievo
        tinta(i, 5, r[-1], 0.4)
        tinta(5, i, r[-1], 0.35)
        tinta(i, 10, r[0], 0.45)
        tinta(10, i, r[0], 0.4)
    for y in range(7, 9):             # incavo centrale
        for x in range(7, 9):
            tinta(x, y, r[0], 0.5)
    tinta(9, 9, r[-1], 0.2)
    return img


# ---------------------------------------------------------------------------
# LEGNI
# ---------------------------------------------------------------------------
def assi(seme, r):
    """Assi stile vanilla: 4 tavole da 4 pixel (3 legno + 1 fessura), UNA giunta per tavola in posizioni sfalsate;
    ogni riga di pixel ha la sua venatura lunga (2-3 toni), con pochissima grana"""
    img = tela()
    rng = random.Random(seme)
    giunte = [rng.randrange(1, 5), rng.randrange(9, 13), rng.randrange(5, 8), rng.randrange(12, 16)]
    for y in range(S):
        tavola, iy = y // 4, y % 4
        luce = (0.03, -0.04, 0.05, -0.02)[tavola]
        riga = Rumore(f"{seme}r{y}", 2)
        for x in range(S):
            g = Rumore(seme + "p", 16)(x, y)
            v = 0.55 + luce + (riga(x, 0) - 0.5) * 0.35 + (g - 0.5) * 0.12
            if iy == 0:
                v += 0.08
            elif iy == 2:
                v -= 0.06
            if iy == 3:
                c = mix(r[1], r[2], 0.3 + g * 0.3)       # fessura fra le tavole
            elif x == giunte[tavola]:
                c = mix(r[1], r[2], 0.5)                  # fine della tavola
            else:
                c = q(r[1:6], max(0.2, min(0.999, v)))
            put(img, x, y, c)
    return img


def corteccia(seme, r):
    """Corteccia: strisce verticali irregolari (solchi scuri che serpeggiano) e grana pixel per pixel"""
    img = tela()
    rng = random.Random(seme)
    solchi = sorted(rng.sample(range(S), 5))
    for y in range(S):
        for x in range(S):
            g = Rumore(seme + "p", 16)(x, y)
            col = Rumore(seme + "c", 16)(x, 0)          # ogni colonna ha il suo tono: strisce verticali
            lungo = Rumore(seme + "v", 4)(x * 4, y)      # variazione lenta lungo la striscia
            v = 0.5 + (col - 0.5) * 0.6 + (lungo - 0.5) * 0.3 + (g - 0.5) * 0.1
            put(img, x, y, q(r, max(0.05, min(0.999, v))))
    for sx in solchi:
        x = sx
        for y in range(S):
            put(img, x, y, mix(r[0], r[1], rng.random() * 0.5))
            if rng.random() < 0.6:
                put(img, x + 1, y, mix(get(img, x + 1, y), r[-1], 0.2))  # spigolo illuminato
            if rng.random() < 0.12:
                x += rng.choice((-1, 1))
    for _ in range(2):  # nodi
        nx, ny = rng.randrange(S), rng.randrange(S)
        put(img, nx, ny, r[0])
        put(img, nx + 1, ny, r[1])
        put(img, nx, ny + 1, r[1])
        put(img, nx - 1, ny - 1, r[-2])
    return img


def cima_tronco(seme, r_assi, r_corteccia):
    """Cima del tronco: anelli irregolari granulosi + bordo di corteccia spesso 1 pixel"""
    img = tela()
    rng = random.Random(seme)
    for y in range(S):
        for x in range(S):
            g = Rumore(seme + "p", 16)(x, y)
            if x in (0, S - 1) or y in (0, S - 1):
                put(img, x, y, q(r_corteccia, 0.1 + g * 0.5))
                continue
            d = max(abs(x - 7.5), abs(y - 7.5)) * 0.65 + math.hypot(x - 7.5, y - 7.5) * 0.35
            anello = (d + Rumore(seme, 4)(x, y) * 0.9) % 2.0
            v = (0.62 if anello < 1.0 else 0.38) + (g - 0.5) * 0.35
            if d < 1.0:
                v = 0.3
            put(img, x, y, q(r_assi, max(0.05, min(0.999, v))))
    return img


# ---------------------------------------------------------------------------
# CATALOGO
# ---------------------------------------------------------------------------
PIETRE = [
    {
        "id": "volcanic_rock", "en": "Volcanic Rock", "it": "Roccia vulcanica",
        "colore": (58, 56, 64), "fuga": (24, 22, 28), "durezza": 1.8,
        "extra": {"macchie": ((30, 28, 34), 10)},
        "generazione": {"tipo": "vena", "dimensione": 64, "per_chunk": 2, "y_min": -48, "y_max": 24,
                        "nota": "anche vicino ai laghi di lava sotterranei"},
    },
    {
        "id": "white_marble", "en": "White Marble", "it": "Marmo bianco",
        "colore": (224, 222, 216), "fuga": (150, 148, 146), "durezza": 1.5,
        "extra": {"vene": ((132, 132, 142), 0.16)},
        "generazione": {"tipo": "vena", "dimensione": 64, "per_chunk": 2, "y_min": 0, "y_max": 72},
    },
    {
        "id": "travertine", "en": "Travertine", "it": "Travertino",
        "colore": (214, 190, 150), "fuga": (138, 112, 82), "durezza": 1.5,
        "extra": {"strati": 1.6, "macchie": ((168, 140, 104), 8)},
        "generazione": {"tipo": "vena", "dimensione": 56, "per_chunk": 1, "y_min": 32, "y_max": 110,
                        "nota": "piu' frequente nei deserti e nelle savane (per_chunk 3)"},
    },
    {
        "id": "slate", "en": "Slate", "it": "Ardesia",
        "colore": (88, 98, 112), "fuga": (40, 44, 54), "durezza": 1.8,
        "extra": {"strati": 2.4},
        "generazione": {"tipo": "vena", "dimensione": 64, "per_chunk": 2, "y_min": -64, "y_max": 16},
    },
]

LEGNI = [
    # id, inglese, italiano, colore assi, colore corteccia, colorante
    ("ruby", "Ruby", "rubino", (168, 52, 52), (92, 30, 34), "minecraft:red_dye"),
    ("amber", "Amber", "ambra", (204, 120, 48), (110, 64, 30), "minecraft:orange_dye"),
    ("honey", "Honey", "miele", (214, 178, 76), (118, 92, 44), "minecraft:yellow_dye"),
    ("jade", "Jade", "giada", (70, 140, 92), (38, 74, 52), "minecraft:green_dye"),
    ("azure", "Azure", "azzurro", (72, 148, 196), (40, 78, 108), "minecraft:light_blue_dye"),
    ("violet", "Violet", "violetto", (128, 76, 160), (70, 42, 90), "minecraft:purple_dye"),
    ("ivory", "Ivory", "avorio", (226, 216, 192), (150, 140, 120), "minecraft:white_dye"),
    ("shadow", "Shadow", "ombra", (74, 60, 60), (40, 32, 34), "minecraft:black_dye"),
]


def tex(nome):
    return f"futuretek:block/{nome}"


def costruisci(salva=True):
    """Disegna tutte le texture e restituisce la specifica (lista di blocchi)"""
    blocchi = []
    immagini = {}

    def aggiungi(img, nome):
        immagini[nome] = img
        if salva:
            TEX.mkdir(parents=True, exist_ok=True)
            img.save(TEX / f"{nome}.png")

    def forme(base_id, en, it, texture, materiale, durezza, taglio):
        """scale, lastra e muretto (o staccionata/cancello per il legno) di un blocco"""
        out = [
            {"id": f"{base_id}_stairs", "nome_en": f"{en} Stairs", "nome_it": f"Scale di {it.lower()}",
             "tipo": "scale", "base": base_id, "texture": tex(texture), "materiale": materiale, "durezza": durezza,
             "ricette": {"crafting": {"schema": ["X  ", "XX ", "XXX"], "X": f"futuretek:{base_id}", "quanti": 4},
                         **({"stonecutter": {"da": f"futuretek:{base_id}", "quanti": 1}} if taglio else {})}},
            {"id": f"{base_id}_slab", "nome_en": f"{en} Slab", "nome_it": f"Lastra di {it.lower()}",
             "tipo": "lastra", "base": base_id, "texture": tex(texture), "materiale": materiale, "durezza": durezza,
             "ricette": {"crafting": {"schema": ["XXX"], "X": f"futuretek:{base_id}", "quanti": 6},
                         **({"stonecutter": {"da": f"futuretek:{base_id}", "quanti": 2}} if taglio else {})}},
        ]
        if materiale == "pietra":
            out.append({"id": f"{base_id}_wall", "nome_en": f"{en} Wall", "nome_it": f"Muretto di {it.lower()}",
                        "tipo": "muro", "base": base_id, "texture": tex(texture), "materiale": materiale,
                        "durezza": durezza,
                        "ricette": {"crafting": {"schema": ["XXX", "XXX"], "X": f"futuretek:{base_id}", "quanti": 6},
                                    "stonecutter": {"da": f"futuretek:{base_id}", "quanti": 1}}})
        return out

    # ---------------- pietre ----------------
    for p in PIETRE:
        pid, en, it = p["id"], p["en"], p["it"]
        r = rampa(p["colore"], scuro=0.34, chiaro=0.24)
        f = p["fuga"]
        d = p["durezza"]
        ex = p["extra"]
        aggiungi(pietra(pid, r, ex.get("macchie"), ex.get("vene"), ex.get("strati")), pid)
        aggiungi(pietrisco(pid + "c", rampa(mix(p["colore"], (0, 0, 0), 0.06), scuro=0.4, chiaro=0.3)), f"cobbled_{pid}")
        aggiungi(levigata(pid + "p", r), f"polished_{pid}")
        aggiungi(mattoni(pid + "b", r, f), f"{pid}_bricks")
        aggiungi(crepe(mattoni(pid + "b", r, f), pid + "k", f), f"cracked_{pid}_bricks")
        aggiungi(piastrelle(pid + "t", r, f), f"{pid}_tiles")
        aggiungi(cesellata(pid + "h", r, f), f"chiseled_{pid}")

        varianti = [
            (pid, en, it, "cubo", None),
            (f"cobbled_{pid}", f"Cobbled {en}", f"Pietrisco di {it.lower()}", "cubo", None),
            (f"polished_{pid}", f"Polished {en}", f"{it} levigata" if it.endswith("a") else f"{it} levigato", "cubo", None),
            (f"{pid}_bricks", f"{en} Bricks", f"Mattoni di {it.lower()}", "cubo", None),
            (f"cracked_{pid}_bricks", f"Cracked {en} Bricks", f"Mattoni di {it.lower()} crepati", "cubo", None),
            (f"{pid}_tiles", f"{en} Tiles", f"Piastrelle di {it.lower()}", "cubo", None),
            (f"chiseled_{pid}", f"Chiseled {en}", f"{it} cesellata" if it.endswith("a") else f"{it} cesellato", "cubo", None),
        ]
        ricette = {
            pid: {"fornace": {"da": f"futuretek:cobbled_{pid}", "esperienza": 0.1}},
            f"cobbled_{pid}": {},
            f"polished_{pid}": {"crafting": {"schema": ["XX", "XX"], "X": f"futuretek:{pid}", "quanti": 4},
                                "stonecutter": {"da": f"futuretek:{pid}", "quanti": 1}},
            f"{pid}_bricks": {"crafting": {"schema": ["XX", "XX"], "X": f"futuretek:polished_{pid}", "quanti": 4},
                              "stonecutter": {"da": f"futuretek:{pid}", "quanti": 1}},
            f"cracked_{pid}_bricks": {"fornace": {"da": f"futuretek:{pid}_bricks", "esperienza": 0.1}},
            f"{pid}_tiles": {"crafting": {"schema": ["XX", "XX"], "X": f"futuretek:{pid}_bricks", "quanti": 4},
                             "stonecutter": {"da": f"futuretek:{pid}", "quanti": 1}},
            f"chiseled_{pid}": {"crafting": {"schema": ["X", "X"], "X": f"futuretek:{pid}_slab", "quanti": 1},
                                "stonecutter": {"da": f"futuretek:{pid}", "quanti": 1}},
        }
        for vid, ven, vit, tipo, _ in varianti:
            b = {"id": vid, "nome_en": ven, "nome_it": vit, "tipo": tipo, "texture": tex(vid), "materiale": "pietra",
                 "durezza": d, "resistenza": 6.0, "attrezzo": "piccone", "ricette": ricette[vid]}
            if vid == pid:
                b["generazione"] = p["generazione"]
                b["drop"] = f"futuretek:cobbled_{pid}"
            blocchi.append(b)
        for vid, ven, vit in [(pid, en, it), (f"cobbled_{pid}", f"Cobbled {en}", f"pietrisco di {it.lower()}"),
                              (f"polished_{pid}", f"Polished {en}", f"{it.lower()} levigata"),
                              (f"{pid}_bricks", f"{en} Brick", f"mattoni di {it.lower()}"),
                              (f"{pid}_tiles", f"{en} Tile", f"piastrelle di {it.lower()}")]:
            for fb in forme(vid, ven, vit, vid, "pietra", d, True):
                # i nomi delle forme dei blocchi derivati: "Scale di mattoni di ardesia"
                fb["nome_it"] = fb["nome_it"].replace("di di ", "di ")
                blocchi.append(fb)

    # ---------------- legni ----------------
    for lid, en, it, c_assi, c_cort, colorante in LEGNI:
        ra = rampa(c_assi, scuro=0.36, chiaro=0.22)
        rc = rampa(c_cort, scuro=0.35, chiaro=0.35)
        aggiungi(assi(lid + "a", ra), f"{lid}_planks")
        aggiungi(corteccia(lid + "c", rc), f"{lid}_log")
        aggiungi(cima_tronco(lid + "t", ra, rc), f"{lid}_log_top")
        blocchi.append({
            "id": f"{lid}_planks", "nome_en": f"{en} Planks", "nome_it": f"Assi color {it}", "tipo": "cubo",
            "texture": tex(f"{lid}_planks"), "materiale": "legno", "durezza": 2.0, "resistenza": 3.0,
            "attrezzo": "ascia", "infiammabile": True,
            "ricette": {"crafting": {"schema": ["PPP", "PDP", "PPP"], "P": "#minecraft:planks", "D": colorante,
                                     "quanti": 8},
                        "crafting_2": {"senza_forma": [f"futuretek:{lid}_log"], "quanti": 4}},
            "tag": ["minecraft:planks"],
        })
        blocchi.append({
            "id": f"{lid}_log", "nome_en": f"{en} Log", "nome_it": f"Tronco color {it}", "tipo": "tronco",
            "texture": tex(f"{lid}_log"), "texture_cima": tex(f"{lid}_log_top"), "materiale": "legno",
            "durezza": 2.0, "resistenza": 2.0, "attrezzo": "ascia", "infiammabile": True,
            "ricette": {"crafting": {"schema": ["LLL", "LDL", "LLL"], "L": "#minecraft:logs_that_burn",
                                     "D": colorante, "quanti": 8}},
            "tag": ["minecraft:logs", "minecraft:logs_that_burn"],
        })
        for fb in forme(f"{lid}_planks", en, f"assi color {it}", f"{lid}_planks", "legno", 2.0, False):
            fb["id"] = fb["id"].replace("_planks_", "_")  # ruby_stairs, ruby_slab
            fb["nome_en"] = fb["nome_en"].replace(" Planks", "")
            blocchi.append(fb)
        blocchi.append({
            "id": f"{lid}_fence", "nome_en": f"{en} Fence", "nome_it": f"Staccionata color {it}", "tipo": "staccionata",
            "base": f"{lid}_planks", "texture": tex(f"{lid}_planks"), "materiale": "legno", "durezza": 2.0,
            "ricette": {"crafting": {"schema": ["PSP", "PSP"], "P": f"futuretek:{lid}_planks", "S": "minecraft:stick",
                                     "quanti": 3}},
            "tag": ["minecraft:wooden_fences"],
        })
        blocchi.append({
            "id": f"{lid}_fence_gate", "nome_en": f"{en} Fence Gate", "nome_it": f"Cancello color {it}",
            "tipo": "cancello", "base": f"{lid}_planks", "texture": tex(f"{lid}_planks"), "materiale": "legno",
            "durezza": 2.0,
            "ricette": {"crafting": {"schema": ["SPS", "SPS"], "P": f"futuretek:{lid}_planks", "S": "minecraft:stick",
                                     "quanti": 1}},
            "tag": ["minecraft:fence_gates"],
        })
    return blocchi, immagini


FONTI = [
    {"titolo": "Blue Power - generazione del vulcano (codice sorgente, cono di basalto con colonna di lava)",
     "url": "https://github.com/Qmunity/BluePower/blob/master/src/main/java/com/bluepowermod/world/WorldGenVolcano.java"},
    {"titolo": "Blue Power - registrazione della generazione del mondo",
     "url": "https://github.com/Qmunity/BluePower/blob/master/src/main/java/com/bluepowermod/world/BPWorldGen.java"},
    {"titolo": "Blue Power - Official Feed The Beast Wiki (basalto, marmo, mattoni e piastrelle decorativi)",
     "url": "https://ftb.fandom.com/wiki/Blue_Power"},
    {"titolo": "Basalt (RedPower 2) - FTB Wiki", "url": "https://ftbwiki.org/Basalt_(RedPower_2)"},
    {"titolo": "Arboriculture / specie di alberi di Forestry - FTB Wiki", "url": "https://ftbwiki.org/Arboriculture"},
]


def anteprima(immagini, percorso):
    """Foglio: ogni texture ingrandita x6 + una prova 3x3 affiancata per controllare che si ripetano"""
    nomi = list(immagini)
    cella = 16 * 6
    prova = 16 * 3 * 2
    w = cella + prova + 24
    h = len(nomi) * (prova + 8) + 8
    col = 2
    righe = (len(nomi) + col - 1) // col
    foglio = Image.new("RGBA", ((w + 8) * col, righe * (prova + 8) + 8), (40, 42, 48, 255))
    for i, nome in enumerate(nomi):
        img = immagini[nome]
        x0 = (i % col) * (w + 8) + 8
        y0 = (i // col) * (prova + 8) + 8
        foglio.paste(img.resize((cella, cella), Image.NEAREST), (x0, y0))
        muro = Image.new("RGBA", (48, 48))
        for a in range(3):
            for b in range(3):
                muro.paste(img, (a * 16, b * 16))
        foglio.paste(muro.resize((prova, prova), Image.NEAREST), (x0 + cella + 12, y0))
    foglio.save(percorso)


if __name__ == "__main__":
    blocchi, immagini = costruisci(salva=True)
    spec = {
        "descrizione": "Blocchi da costruzione FutureTek (Fase 16). Nomi e texture originali; ispirazione: pietre "
                       "vulcaniche/marmo e legni colorati dei vecchi modpack tecnici.",
        "fonti_ricerca": FONTI,
        "note_integrazione": [
            "cubo: Block con proprieta' copiate da Blocks.STONE (pietra) o Blocks.OAK_PLANKS (legno), durezza/resistenza dal JSON",
            "tronco: RotatedPillarBlock (texture lato + texture_cima), tag minecraft:logs",
            "scale: StairBlock(base.defaultBlockState(), props), modello come le scale vanilla con la texture del blocco base",
            "lastra: SlabBlock, modello bottom/top + doppia = modello del blocco base",
            "muro: WallBlock (tag minecraft:walls), modelli wall_post/side/side_tall + inventory",
            "staccionata: FenceBlock (tag wooden_fences), cancello: FenceGateBlock(WoodType.OAK, props)",
            "pietra base: lascia cadere il pietrisco (drop), si scava col piccone (tag mineable/pickaxe); legno: mineable/axe",
            "generazione: vene come granito/diorite (configured_feature ore con target stone/deepslate + placed_feature)",
        ],
        "blocchi": blocchi,
    }
    SPEC.write_text(json.dumps(spec, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"{len(immagini)} texture, {len(blocchi)} blocchi -> {SPEC}")
    if len(sys.argv) > 2 and sys.argv[1] == "--anteprima":
        anteprima(immagini, sys.argv[2])
        print("anteprima:", sys.argv[2])
