"""
FutureTek - Grafica degli ALTRI OGGETTI in stile Minecraft (v5)
================================================================
Tutti gli oggetti che non sono attrezzi, armature o materiali: attrezzi elettrici, circuiti, celle e batterie,
pezzi del reattore, oggetti d'alchimia (anelli, amuleti, braci, cristalli...), favi e api, scanner, borse,
jetpack, robot, ecc.

Stile (lo stesso degli oggetti di Minecraft):
  * ogni oggetto e' un disegno 16x16 fatto a mano qui sotto, con lettere = "zone" di colore;
  * ogni zona ha 5 toni; l'ombreggiatura e' automatica: bordo chiaro in alto/sinistra, scuro in basso/destra;
  * contorno scuro di 1 pixel attorno alla sagoma (il tono piu' scuro della zona che tocca);
  * pochi toni, niente sfumature morbide.
Disegni ORIGINALI (fatti qui a mano seguendo le convenzioni degli oggetti di Minecraft), nessuna texture copiata.

Uso (dalla cartella del progetto):  python tools/grafica_varie.py
Va lanciato PER ULTIMO: dopo grafica.py, grafica_oggetti.py e grafica_materiali.py.
"""
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from grafica import TEX  # noqa: E402

ITEM = TEX / "item"


# ---------------------------------------------------------------------------
# colori
# ---------------------------------------------------------------------------
def _clip(v):
    return max(0, min(255, int(round(v))))


def scurisci(c, f):
    return tuple(_clip(v * f) for v in c)


def schiarisci(c, f):
    return tuple(_clip(v + (255 - v) * f) for v in c)


def toni(base):
    """5 toni: 0 contorno, 1 ombra profonda, 2 ombra, 3 base, 4 luce"""
    return [scurisci(base, 0.36), scurisci(base, 0.62), scurisci(base, 0.8), tuple(base), schiarisci(base, 0.38)]


class Fisso(tuple):
    """Colore fisso (non ombreggiato), es. pixel luminosi"""


def fisso(c):
    return Fisso(c)


# metalli e materiali ricorrenti
FERRO = (178, 182, 190)
ACCIAIO = (138, 146, 158)
GRAFITE = (66, 70, 80)
ORO = (236, 190, 58)
ARGENTO = (206, 214, 226)
RAME = (204, 118, 74)
LEGNO = (140, 100, 56)
CUOIO = (128, 84, 50)
VETRO = (196, 222, 230)
VERDE_PCB = (60, 140, 70)
LUCE = fisso((255, 255, 255))


# ---------------------------------------------------------------------------
# motore di disegno
# ---------------------------------------------------------------------------
def disegna(arte, zone, senza_contorno="", contorno=None):
    """
    arte: 16 righe di 16 caratteri ('.' = vuoto)
    zone: lettera -> colore base (zona ombreggiata) oppure Fisso (colore esatto)
    senza_contorno: lettere che non ricevono il contorno (catenine, scintille...)
    contorno: colore del contorno per tutta la sagoma (se None: tono scuro della zona vicina)
    """
    assert len(arte) == 16, f"{len(arte)} righe"
    for r in arte:
        assert len(r) == 16, f"riga di {len(r)}: {r!r}"
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))

    def ch(x, y):
        return arte[y][x] if 0 <= x < 16 and 0 <= y < 16 else "."

    for y in range(16):
        for x in range(16):
            c = arte[y][x]
            if c == ".":
                continue
            z = zone[c]
            if isinstance(z, Fisso):
                img.putpixel((x, y), tuple(z) + (255,))
                continue
            t = toni(z)
            su, sx, giu, dx = ch(x, y - 1) == c, ch(x - 1, y) == c, ch(x, y + 1) == c, ch(x + 1, y) == c
            alto = not su or not sx
            basso = not giu or not dx
            if alto and basso:
                k = 3
            elif alto:
                k = 4
            elif not giu and not dx:
                k = 1
            elif basso:
                k = 2
            else:
                k = 3
            img.putpixel((x, y), t[k] + (255,))
    # contorno
    for y in range(16):
        for x in range(16):
            if arte[y][x] != ".":
                continue
            scelto = None
            for dx, dy in ((0, -1), (-1, 0), (1, 0), (0, 1)):
                c = ch(x + dx, y + dy)
                if c == "." or c in senza_contorno:
                    continue
                z = zone[c]
                col = contorno or (scurisci(z, 0.45) if isinstance(z, Fisso) else toni(z)[0])
                if scelto is None or sum(col) < sum(scelto):
                    scelto = col
            if scelto is not None:
                img.putpixel((x, y), tuple(scelto) + (255,))
    return img


def salva(img, nome):
    img.save(ITEM / f"{nome}.png")


# ---------------------------------------------------------------------------
# DISEGNI
# ---------------------------------------------------------------------------
ANELLO = [
    "................",
    "......gggg......",
    ".....g!ggggg....",
    ".....gggggg.....",
    "......gggg......",
    ".....mmmmmm.....",
    "....mmmmmmmm....",
    "...mmm....mmm...",
    "...mm......mm...",
    "...mm......mm...",
    "...mm......mm...",
    "...mmm....mmm...",
    "....mmmmmmmm....",
    ".....mmmmmm.....",
    "................",
    "................",
]
ANELLO = [r[:16] for r in ANELLO]
ANELLO[2] = ".....g!gggg....."

AMULETO = [
    "................",
    "..c..........c..",
    "...c........c...",
    "....c......c....",
    ".....c....c.....",
    "......c..c......",
    ".....pppppp.....",
    "....pp!gggpp....",
    "....pggggggp....",
    "....pggggggp....",
    "....pggggggp....",
    "....ppggggpp....",
    ".....pppppp.....",
    "................",
    "................",
    "................",
]

FAVO = [
    "................",
    "................",
    "....cccccccc....",
    "..cccccccccccc..",
    "..cchhcchhcchc..",
    ".ccchhcchhcchcc.",
    ".cccccccccccccc.",
    ".cchhcchhcchhcc.",
    ".cchhcchhcchhcc.",
    ".cccccccccccccc.",
    "..cchhcchhcchc..",
    "..cchhcchhcchc..",
    "...cccccccccc...",
    ".....cccccc.....",
    "................",
    "................",
]

BATTERIA = [
    "................",
    "......tttt......",
    ".....bbbbbb.....",
    ".....b!bbbb.....",
    ".....bbbbbb.....",
    ".....llllll.....",
    ".....l*ll*l.....",
    ".....llllll.....",
    ".....bbbbbb.....",
    ".....bbbbbb.....",
    ".....bbbbbb.....",
    ".....bbbbbb.....",
    ".....bbbbbb.....",
    ".....bbbbbb.....",
    "................",
    "................",
]

BATTERIA_AVANZATA = [
    "................",
    ".....tttttt.....",
    "....bbbbbbbb....",
    "....b!bbbbbb....",
    "....llllllll....",
    "....l*l**l*l....",
    "....llllllll....",
    "....bbbbbbbb....",
    "....bbbbbbbb....",
    "....llllllll....",
    "....l*l**l*l....",
    "....llllllll....",
    "....bbbbbbbb....",
    "....bbbbbbbb....",
    "................",
    "................",
]


def cella_refrigerante(fasce):
    arte = [
        "................",
        ".....mmmmmm.....",
        "....mmmmmmmm....",
        "....m!qqqqqm....",
        "....mqqqqqqm....",
        "....mqqqqqqm....",
        "....mqqqqqqm....",
        "....mqqqqqqm....",
        "....mqqqqqqm....",
        "....mqqqqqqm....",
        "....mqqqqqqm....",
        "....mqqqqqqm....",
        "....mmmmmmmm....",
        ".....mmmmmm.....",
        "................",
        "................",
    ]
    for r in fasce:
        arte[r] = "....mmmmmmmm...."
    return arte


def barre(n):
    """1, 2 o 4 barre di uranio in tubi di vetro con tappi di metallo"""
    colonne = {1: [6], 2: [3, 9], 4: [1, 4, 8, 11]}[n]
    righe = [["."] * 16 for _ in range(16)]
    for c in colonne:
        for y in range(1, 15):
            for dx in range(4 if n < 4 else 3):
                x = c + dx
                if y in (1, 2, 13, 14):
                    righe[y][x] = "m"
                elif dx in (0, (3 if n < 4 else 2)):
                    righe[y][x] = "g"
                else:
                    righe[y][x] = "u"
        righe[4][c + 1] = "!"
    if n == 2:
        for x in range(6, 10):
            righe[7][x] = "m"
            righe[8][x] = "m"
    return ["".join(r) for r in righe]


DISSIPATORE = [
    "................",
    "................",
    "..mmmmmmmmmmmm..",
    "..mddddddddddm..",
    "..mssssssssssm..",
    "..mddddddddddm..",
    "..mssssssssssm..",
    "..mddddddddddm..",
    "..mssssssssssm..",
    "..mddddddddddm..",
    "..mssssssssssm..",
    "..mddddddddddm..",
    "..mmmmmmmmmmmm..",
    "................",
    "................",
    "................",
]

PIASTRA_REATTORE = [
    "................",
    "................",
    "..pppppppppppp..",
    "..prpppppppprp..",
    "..pp!ppppppppp..",
    "..ppp!pppppppp..",
    "..pppppppppppp..",
    "..pppppppppppp..",
    "..pppppppppppp..",
    "..pppppppppppp..",
    "..pppppppppppp..",
    "..prpppppppprp..",
    "..pppppppppppp..",
    "................",
    "................",
    "................",
]

CIRCUITO = [
    "................",
    "................",
    "..pppppppppppp..",
    "..ptttttpppptp..",
    "..pppppptpppTp..",
    "..pkkkkpptpppp..",
    "..pkccktttttpp..",
    "..pkccktpppppp..",
    "..pkkkkppkkkkp..",
    "..pptpppkcccc...",
    "..pptttpkcccckp.",
    "..ppppppkkkkkp..",
    "..pppppppppppp..",
    "................",
    "................",
    "................",
]
CIRCUITO = [
    "................",
    "................",
    "..pppppppppppp..",
    "..ptttttppppTp..",
    "..pppppptppppp..",
    "..pkkkkpptpppp..",
    "..pkcckttttttp..",
    "..pkcckpppppTp..",
    "..pkkkkppkkkkp..",
    "..pptppppkcckp..",
    "..ppttttpkcckp..",
    "..ppppppkkkkkp..",
    "..pppppppppppp..",
    ".....o.o.o.o....",
    "................",
    "................",
]

MODULO = [
    "................",
    "................",
    "...bbbbbbbbbb...",
    "...bbbbbbbbbb...",
    "...bb......bb...",
    "...bb......bb...",
    "...bb......bb...",
    "...bb......bb...",
    "...bb......bb...",
    "...bb......bb...",
    "...bbbbbbbbbb...",
    "...bbbbbbbbbb...",
    "....kkkkkkkk....",
    "....k.k.k.k.....",
    "................",
    "................",
]
SIMBOLI_MODULO = {
    # dentro la finestra (colonne 5-10, righe 4-9)
    "efficiency_upgrade": ["..ss..", ".ssss.", "ssssss", "..ss..", "..ss..", "..ss.."],
    "overclock_upgrade": ["...ss.", "..ss..", ".ssss.", "..ss..", ".ss...", "ss...."],
    "energy_storage_upgrade": ["..ss..", ".ssss.", ".s..s.", ".ssss.", ".ssss.", ".ssss."],
    "ejector_upgrade": ["..s...", "..ss..", "ssssss", "ssssss", "..ss..", "..s..."],
}

SCANNER = [
    "................",
    "...........a....",
    "...........a....",
    "....bbbbbbbbb...",
    "...bbbbbbbbbbb..",
    "...bsssssssssb..",
    "...bs*sssssssb..",
    "...bsss*sssssb..",
    "...bsssss**ssb..",
    "...bsssssssssb..",
    "...bbbbbbbbbbb..",
    "...bbkbbbbkbbb..",
    "...bbbbbbbbbbb..",
    "....bbbbbbbbb...",
    "................",
    "................",
]

TRAPANO = [
    "................",
    "............x...",
    "...........xxx..",
    "..........xxyxx.",
    ".........xxyxx..",
    "........xxyxx...",
    ".......bbxxx....",
    "......bbbbx.....",
    ".....bbbbbbb....",
    "....bbbbbbbb....",
    "....hbbbbbbb....",
    "...hhh.bbbb.....",
    "..hhh...........",
    ".hhh............",
    ".hh.............",
    "................",
]

MOTOSEGA = [
    "................",
    "...........sss..",
    "..........sssss.",
    ".........ssstss.",
    "........ssstss..",
    ".......ssstss...",
    "......bbbtss....",
    ".....bbbbbs.....",
    "....bbbbbbb.....",
    "...hhbbbbbb.....",
    "..hhhbbbbb......",
    "..hh..bbb.......",
    ".hh.............",
    ".h..............",
    "................",
    "................",
]

SPADA_PLASMA = [
    "................",
    "............ll..",
    "...........l!ll.",
    "..........l!ll..",
    ".........l!ll...",
    "........l!ll....",
    ".......l!ll.....",
    "......l!ll......",
    "....gg!ll.......",
    "....gggl........",
    "....ghgg........",
    "...hhh.g........",
    "..hhh...........",
    ".hhh............",
    ".hh.............",
    "................",
]

FUCILE = [
    "................",
    "................",
    "................",
    "................",
    "..........ee....",
    ".bbbbbbbbbbbbbbb",
    "bbbbbbbbbbbbbbaa",
    "bb!bbbbbcccbbb..",
    ".ggggg.gbbb.....",
    ".gggg..gb.......",
    ".ggg............",
    ".gg.............",
    "................",
    "................",
    "................",
    "................",
]
FUCILE = [
    "................",
    "................",
    "................",
    "................",
    "..........ee....",
    "..bbbbbbbbbbbbb.",
    ".bbbbbbbbbbbbbaa",
    ".bbbbbbbcccbbb..",
    ".gggg.gbbbb.....",
    ".ggg..gb........",
    ".gg.............",
    ".g..............",
    "................",
    "................",
    "................",
    "................",
]

MULTIMETRO = [
    "................",
    "................",
    "...bbbbbbbbbb...",
    "..bbbbbbbbbbbb..",
    "..bsssssssssbb..",
    "..bs*ssssssssb..",
    "..bss*sssssssb..",
    "..bsssssssssbb..",
    "..bbbbbbbbbbbb..",
    "..bbbddddddbbb..",
    "..bbdkkkkkkdbb..",
    "..bbbddddddbbb..",
    "..bbbbbbbbbbbb..",
    "...bbbbbbbbbb...",
    "....r......n....",
    "....r......n....",
]

CHIAVE_INGLESE = [
    "................",
    "...........mm...",
    "..........mmmm..",
    "..........m..mm.",
    "...........m.mm.",
    "..........mmmm..",
    ".........mmmm...",
    "........mmm.....",
    ".......mmm......",
    "......mmm.......",
    ".....mmm........",
    "....mmm.........",
    "...mmm..........",
    "..mmm...........",
    "..mm............",
    "................",
]

TRONCHESE = [
    "................",
    "................",
    "...ss.....ss....",
    "....ss...ss.....",
    ".....ss.ss......",
    "......sss.......",
    "......sks.......",
    ".....rr.rr......",
    "....rr...rr.....",
    "....rr...rr.....",
    "...rr.....rr....",
    "...rr.....rr....",
    "..rr.......rr...",
    "..rr.......rr...",
    "................",
    "................",
]

RUBINETTO = [
    "................",
    "................",
    "................",
    "....wwwwwwww....",
    "...wwwwwwwwww...",
    "...wwwwwwwwwww..",
    "...wwwwwwwwwww..",
    "...wwwwwwwwww...",
    "....wwwwww......",
    ".........ww.....",
    ".........ww.....",
    ".........ww.....",
    "..........r.....",
    "..........r.....",
    "................",
    "................",
]

BACCHETTA = [
    "................",
    "...........gg...",
    "..........g!gg..",
    "..........gggg..",
    "...........gg...",
    "..........w.....",
    ".........w......",
    "........w.......",
    ".......w........",
    "......w.........",
    ".....w..........",
    "....w...........",
    "...w............",
    "..w.............",
    "................",
    "................",
]

RABDOMANTE = [
    "................",
    "..w.........w...",
    "..w.........w...",
    "...w.......w....",
    "...w.......w....",
    "....w.....w.....",
    "....w.....w.....",
    ".....w...w......",
    "......w.w.......",
    ".......w........",
    ".......w........",
    ".......w........",
    ".......w........",
    ".......w........",
    "................",
    "................",
]

TALISMANO = [
    "................",
    ".......c........",
    "......c.c.......",
    ".....c...c......",
    ".......p........",
    "......ppp.......",
    ".....pp!pp......",
    "....ppgggpp.....",
    "...ppggggspp....",
    "....ppgsspp.....",
    ".....ppspp......",
    "......ppp.......",
    ".......p........",
    "................",
    "................",
    "................",
]

LENTE = [
    "................",
    ".......ffff.....",
    "......ffffff....",
    ".....ffllllff...",
    ".....fl!lllff...",
    ".....fllllllf...",
    ".....fllllllf...",
    ".....ffllllff...",
    "......ffffff....",
    ".....hhffff.....",
    "....hhh.........",
    "...hhh..........",
    "..hhh...........",
    "..hh............",
    "................",
    "................",
]

CLESSIDRA = [
    "................",
    "....wwwwwwww....",
    "....wwwwwwww....",
    ".....g....g.....",
    ".....gssssg.....",
    "......gssg......",
    ".......gg.......",
    ".......gg.......",
    "......g..g......",
    ".....g.ss.g.....",
    ".....gssssg.....",
    "....wwwwwwww....",
    "....wwwwwwww....",
    "................",
    "................",
    "................",
]

LIBRO = [
    "................",
    "..cccccccccc....",
    "..cccccccccck...",
    "..cccceecccck...",
    "..ccceooeccck...",
    "..cccceecccck...",
    "..cccccccccck...",
    "..cccccccccck...",
    "..cccccccccck...",
    "..cccccccccck...",
    "..cccccccccck...",
    "..cccccccccck...",
    "..ppppppppppk...",
    "...kkkkkkkkkk...",
    "................",
    "................",
]

TAVOLETTA = [
    "................",
    "................",
    "...ssssssssss...",
    "..ssssssssssss..",
    "..ss........ss..",
    "..ss........ss..",
    "..ss........ss..",
    "..ss........ss..",
    "..ss........ss..",
    "..ss........ss..",
    "..ssssssssssss..",
    "...ssssssssss...",
    "................",
    "................",
    "................",
    "................",
]

SACCA = [
    "................",
    ".......ss.......",
    "......s..s......",
    "....bbbbbbbb....",
    "...bbbbbbbbbb...",
    "...bbbkkkkbbb...",
    "..bbbbkggkbbbb..",
    "..bbbbkkkkbbbb..",
    "..bbbbbbbbbbbb..",
    "..bbbbbbbbbbbb..",
    "..bbbbbbbbbbbb..",
    "..bbbbbbbbbbbb..",
    "...bbbbbbbbbb...",
    "....bbbbbbbb....",
    "................",
    "................",
]

JETPACK = [
    "................",
    "....s......s....",
    "...tttssssttt...",
    "..tttttsstttt...",
    "..t!ttt..t!tt...",
    "..ttttt..tttt...",
    "..tttttsstttt...",
    "..ttttt..tttt...",
    "..ttttt..tttt...",
    "..tttttsstttt...",
    "...nnn....nnn...",
    "...nnn....nnn...",
    "....f......f....",
    "...fff....fff...",
    "....f......f....",
    "................",
]
JETPACK = [
    "................",
    "....s......s....",
    "...ttt.ss.ttt...",
    "..ttttsssstttt..",
    "..t!tt.ss.t!tt..",
    "..tttt....tttt..",
    "..ttttsssstttt..",
    "..tttt....tttt..",
    "..tttt....tttt..",
    "..ttttsssstttt..",
    "...nn......nn...",
    "...nn......nn...",
    "...ff......ff...",
    "..ffff....ffff..",
    "...ff......ff...",
    "................",
]

CARRELLO = [
    "................",
    "................",
    "................",
    "....wwwwwwww....",
    "....wkwwwwkw....",
    "....wwwwwwww....",
    "..mmmmmmmmmmmm..",
    "..mwwwwwwwwwwm..",
    "..mmmmmmmmmmmm..",
    "..mmmmmmmmmmmm..",
    "..mmmmmmmmmmmm..",
    "...mmmmmmmmmm...",
    "...rr......rr...",
    "...rr......rr...",
    "................",
    "................",
]

TESTA_ROBOT = [
    "................",
    ".......a........",
    ".......a........",
    "...mmmmmmmmmm...",
    "..mmmmmmmmmmmm..",
    "..mm!mmmmmmmmm..",
    "..mvvvvvvvvvvm..",
    "..mveevvvveevm..",
    "..mvvvvvvvvvvm..",
    "..mmmmmmmmmmmm..",
    "..mmkkkkkkkkmm..",
    "..mmmmmmmmmmmm..",
    "...mmmmmmmmmm...",
    "....mm....mm....",
    "................",
    "................",
]

TELECOMANDO = [
    "............a...",
    "............a...",
    "...bbbbbbbbbbb..",
    "...bbbbbbbbbbb..",
    "...bsssssssssb..",
    "...bs*sssssssb..",
    "...bssss*ssssb..",
    "...bsssssssssb..",
    "...bsssssssssb..",
    "...bbbbbbbbbbb..",
    "...bbrbbgbbybb..",
    "...bbbbbbbbbbb..",
    "...bbbbbbbbbbb..",
    "....bbbbbbbbb...",
    "................",
    "................",
]

ARCHIVIO = [
    "................",
    "...bbbbbbbbbb...",
    "..bbbbbbbbbbbb..",
    "..bssssssssssb..",
    "..bs*****ssssb..",
    "..bssssssssssb..",
    "..bs***ssssssb..",
    "..bssssssssssb..",
    "..bs******sssb..",
    "..bssssssssssb..",
    "..bs****sssssb..",
    "..bssssssssssb..",
    "..bbbbbbbbbbbb..",
    "...bbbbbbbbbb...",
    "................",
    "................",
]

SFERA = [
    "................",
    "................",
    ".....oooooo.....",
    "....oooooooo....",
    "...oo!!ooooooo..",
    "...o!ooooooooo..",
    "..oooooccooooo..",
    "..ooooccccoooo..",
    "..ooooccccoooo..",
    "..oooooccooooo..",
    "...oooooooooo...",
    "...oooooooooo...",
    "....oooooooo....",
    ".....oooooo.....",
    "................",
    "................",
]
SFERA = [
    "................",
    "................",
    ".....oooooo.....",
    "....oooooooo....",
    "...oo!!ooooooo..",
    "...o!oooooooo...",
    "..oooooccooooo..",
    "..ooooccccoooo..",
    "..ooooccccoooo..",
    "..oooooccooooo..",
    "...oooooooooo...",
    "...oooooooooo...",
    "....oooooooo....",
    ".....oooooo.....",
    "................",
    "................",
]

FIALA = [
    "................",
    ".......nn.......",
    "......kkkk......",
    "......kkkk......",
    ".......gg.......",
    "......gggg......",
    ".....g!gggg.....",
    ".....gggggg.....",
    ".....qqqqqq.....",
    "....qqqqqqqq....",
    "....qqqqqqqq....",
    "....qqqqqqqq....",
    "....qqqqqqqq....",
    ".....qqqqqq.....",
    "................",
    "................",
]

GEMMA = [
    "................",
    "................",
    "................",
    ".....gggggg.....",
    "....gg!!gggg....",
    "...gg!gggggdg...",
    "..gggggggggddg..",
    "..ddgggggggddd..",
    "...ddgggggddd...",
    "....ddgggddd....",
    ".....ddgddd.....",
    "......dddd......",
    ".......dd.......",
    "................",
    "................",
    "................",
]

PALLINA = [
    "................",
    "................",
    "................",
    "................",
    ".....rrrrrr.....",
    "....rr!rrrrr....",
    "...rr!rrrrrrr...",
    "...rrrrrrrrrr...",
    "...rrrrrrrrrr...",
    "...rrrrrrrrrr...",
    "....rrrrrrrr....",
    ".....rrrrrr.....",
    "................",
    "................",
    "................",
    "................",
]

GOCCIA = [
    "................",
    "................",
    ".......r........",
    "......rrr.......",
    "......rrr.......",
    ".....rrrrr......",
    ".....r!rrr......",
    "....rr!rrrr.....",
    "....rrrrrrr.....",
    "....rrrrrrr.....",
    "....rrrrrrr.....",
    ".....rrrrr......",
    "......rrr.......",
    "................",
    "................",
    "................",
]

CONCIME = [
    "................",
    ".......v........",
    "......vvv.v.....",
    "........vvv.....",
    ".....kkkkkk.....",
    "....ssssssss....",
    "...ssssssssss...",
    "...ss!sssssss...",
    "..ssssssssssss..",
    "..sssslllsssss..",
    "..ssslllllssss..",
    "..sssslllsssss..",
    "..ssssssssssss..",
    "...ssssssssss...",
    "................",
    "................",
]


def cristallo(n):
    """1-4 cristalli: piu' sono, piu' e' potente"""
    if n == 1:
        return [
            "................",
            "................",
            ".......cc.......",
            "......cccc......",
            ".....c!cccc.....",
            ".....c!cccd.....",
            ".....ccccdd.....",
            ".....ccccdd.....",
            ".....ccccdd.....",
            ".....ccccdd.....",
            ".....cccddd.....",
            "......ccdd......",
            ".......cd.......",
            "................",
            "................",
            "................",
        ]
    if n == 2:
        return [
            "................",
            ".........cc.....",
            "........cccc....",
            "...cc...c!ccd...",
            "..cccc..c!ccd...",
            "..c!ccd.cccdd...",
            "..c!ccd.cccdd...",
            "..cccdd.cccdd...",
            "..cccdd.cccdd...",
            "..cccdd..ccd....",
            "...ccd...ccd....",
            "...ccd....d.....",
            "....d...........",
            "................",
            "................",
            "................",
        ]
    if n == 3:
        return [
            "................",
            ".......cc.......",
            "......cccc......",
            "......c!ccd.....",
            "..cc..c!ccd.cc..",
            ".cccc.cccdd.ccc.",
            ".c!ccdcccddc!ccd",
            ".c!ccdcccddcccdd",
            ".cccddcccddcccdd",
            "..ccdd.ccd.cccd.",
            "..ccd..ccd..ccd.",
            "...d....d....d..",
            "...bbbbbbbbbbb..",
            "..bbbbbbbbbbbbb.",
            "................",
            "................",
        ]
    return [
        "......cc........",
        ".....cccc..cc...",
        ".....c!ccdcccc..",
        "..cc.c!ccdc!ccd.",
        ".ccccccccdc!ccd.",
        ".c!ccdcccdcccdd.",
        ".c!ccdcccddccdd.",
        ".cccddcccddccdd.",
        ".cccddcccddccdd.",
        "..ccddcccdd.ccd.",
        "..ccd.cccd..ccd.",
        "...d...cd....d..",
        "..bbbbbbbbbbbb..",
        ".bbbbbbbbbbbbbb.",
        "................",
        "................",
    ]


BRACE = [
    "................",
    ".......*........",
    "..*....o....*...",
    "......ooo.......",
    ".....ooooo......",
    "....oo!oooo.....",
    "*..oo!ooooo..*..",
    "...ooooccooo....",
    "...oooccccoo....",
    "...oooccccoo....",
    "....oooccoo.....",
    ".....ooooo......",
    "..*...ooo...*...",
    ".......o........",
    ".......*........",
    "................",
]

CATALIZZATORE = [
    "................",
    ".......s........",
    "..s....o....s...",
    "...s.oooooo.s...",
    "....oooooooo....",
    "...oo!oooooo....",
    "...o!occccooo...",
    "s.ooocvvvvcoo..s",
    "..ooocvvvvcooo..",
    "...oocccccooo...",
    "...oooooooooo...",
    "....oooooooo....",
    "...s.oooooo.s...",
    "..s....o....s...",
    ".......s........",
    "................",
]

PIETRA = [
    "................",
    "................",
    "................",
    ".....ssssss.....",
    "....ssssssss....",
    "...ss!sssssss...",
    "...sssrrrssss...",
    "..sssrsssrssss..",
    "..ssssrrrsssss..",
    "..sssssssrssss..",
    "..ssssrrrsssss..",
    "...ssssssssss...",
    "....ssssssss....",
    "................",
    "................",
    "................",
]

TELAIO_ROBOT = TESTA_ROBOT

# api: livello 0 = contorno, ali, occhi, pungiglione (non colorato); livello 1 = corpo in grigi (colorato dal gioco)
APE_CORPO = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "....bbbbbbb.....",
    "...bbsbbsbbb....",
    "...bbsbbsbbbb...",
    "...bbsbbsbbbb...",
    "...bbsbbsbbb....",
    "....bbbbbbb.....",
    "................",
    "................",
    "................",
    "................",
    "................",
]
APE_ALI = [
    "................",
    "................",
    "....ww...ww.....",
    "...wwww.wwww....",
    "...wwww.wwww....",
    "....www.www.....",
    "................",
    "..e.............",
    "................",
    "................",
    "................",
    "............p...",
    "................",
    "................",
    "................",
    "................",
]
REGINA_CORPO = [
    "................",
    "................",
    "................",
    "................",
    "...bbbbbbbbb....",
    "..bbsbbsbbsbb...",
    "..bbsbbsbbsbbb..",
    "..bbsbbsbbsbbb..",
    "..bbsbbsbbsbbb..",
    "..bbsbbsbbsbb...",
    "...bbbbbbbbb....",
    "................",
    "................",
    "................",
    "................",
    "................",
]
REGINA_ALI = [
    "...o.o.o........",
    "...ooooo........",
    "....www..www....",
    "...wwwww.wwwww..",
    "...wwwww.wwwww..",
    "....www...www...",
    "................",
    "................",
    ".e..............",
    "................",
    "................",
    "..............p.",
    "................",
    "................",
    "................",
    "................",
]


def ape(corpo, ali, nome):
    """Il corpo (livello 1) e' in grigi: il gioco lo colora con il colore della specie"""
    grigio = (200, 200, 200)
    img_corpo = disegna(corpo, {"b": grigio, "s": (110, 110, 110)}, contorno=(40, 40, 40))
    salva(img_corpo, nome + "_body")
    img = disegna(ali, {"w": fisso((226, 240, 255)), "o": fisso((250, 210, 70)), "e": fisso((24, 24, 28)),
                        "p": fisso((40, 40, 40))}, senza_contorno="wope")
    # contorno delle ali sopra il corpo
    for y in range(16):
        for x in range(16):
            if ali[y][x] == "w":
                for dx, dy in ((0, -1), (-1, 0), (1, 0)):
                    xx, yy = x + dx, y + dy
                    if 0 <= xx < 16 and 0 <= yy < 16 and ali[yy][xx] == "." and corpo[yy][xx] == ".":
                        img.putpixel((xx, yy), (120, 140, 160, 255))
    salva(img, nome)


# ---------------------------------------------------------------------------
# elenco degli oggetti
# ---------------------------------------------------------------------------
def main():
    n = 0

    def s(arte, zone, nome, **kw):
        nonlocal n
        salva(disegna(arte, zone, **kw), nome)
        n += 1

    # anelli (fascia + pietra)
    for nome, fascia, pietra in [("attractor_ring", ORO, (90, 210, 110)), ("ember_ring", ORO, (240, 110, 40)),
                                 ("frost_ring", ARGENTO, (140, 210, 250)), ("verdant_ring", ORO, (70, 180, 60)),
                                 ("windrider_ring", ARGENTO, (220, 240, 245)), ("essence_band", ARGENTO, (90, 120, 230))]:
        s(ANELLO, {"m": fascia, "g": pietra, "!": LUCE}, nome)
    # amuleti
    for nome, pietra in [("magma_amulet", (240, 100, 30)), ("tide_amulet", (50, 110, 220)), ("vitality_amulet", (220, 40, 70))]:
        s(AMULETO, {"c": fisso((214, 170, 60)), "p": ORO, "g": pietra, "!": LUCE}, nome, senza_contorno="c")
    # favi
    for nome, c in [("abyssal_comb", (70, 60, 140)), ("ember_comb", (230, 110, 40)), ("frozen_comb", (150, 210, 240)),
                    ("metallic_comb", (170, 176, 186)), ("mineral_comb", (150, 120, 90)), ("stellar_comb", (240, 220, 110)),
                    ("voltaic_comb", (90, 200, 230))]:
        s(FAVO, {"c": c, "h": scurisci(c, 0.6)}, nome)
    # api
    ape(APE_CORPO, APE_ALI, "bee_drone")
    ape(REGINA_CORPO, REGINA_ALI, "bee_queen")
    n += 4
    # batterie
    s(BATTERIA, {"t": FERRO, "b": RAME, "l": GRAFITE, "*": fisso((120, 240, 90)), "!": LUCE}, "battery_cell")
    s(BATTERIA_AVANZATA, {"t": FERRO, "b": (70, 110, 190), "l": GRAFITE, "*": fisso((120, 240, 255)), "!": LUCE},
      "advanced_battery_cell")
    # celle refrigeranti (esauste = liquido rosso)
    for nome, fasce, liquido in [("coolant_cell", [], (70, 140, 230)), ("coolant_cell_30k", [7], (50, 100, 210)),
                                 ("coolant_cell_60k", [6, 9], (110, 70, 210)), ("spent_coolant_cell", [], (210, 80, 40)),
                                 ("spent_coolant_cell_30k", [7], (200, 60, 30)), ("spent_coolant_cell_60k", [6, 9], (190, 40, 40))]:
        s(cella_refrigerante(fasce), {"m": ACCIAIO, "q": liquido, "!": LUCE}, nome)
    # barre di uranio
    verde = (110, 220, 70)
    s(barre(1), {"m": ACCIAIO, "g": VETRO, "u": verde, "!": LUCE}, "fuel_rod")
    s(barre(2), {"m": ACCIAIO, "g": VETRO, "u": verde, "!": LUCE}, "dual_fuel_rod")
    s(barre(4), {"m": ACCIAIO, "g": VETRO, "u": verde, "!": LUCE}, "quad_fuel_rod")
    s(barre(1), {"m": ACCIAIO, "g": VETRO, "u": (96, 104, 92), "!": fisso((200, 200, 200))}, "depleted_fuel_rod")
    s(DISSIPATORE, {"m": ACCIAIO, "d": GRAFITE, "s": FERRO}, "heat_vent")
    s(PIASTRA_REATTORE, {"p": (110, 130, 120), "r": FERRO, "!": fisso((200, 220, 210))}, "reactor_plating")
    # circuiti
    s(CIRCUITO, {"p": VERDE_PCB, "t": ORO, "T": fisso((255, 230, 120)), "k": GRAFITE, "c": (30, 30, 36), "o": ORO},
      "basic_circuit", senza_contorno="o")
    s(CIRCUITO, {"p": (160, 50, 60), "t": ORO, "T": fisso((255, 230, 120)), "k": (40, 40, 50), "c": (90, 170, 230), "o": ORO},
      "advanced_circuit", senza_contorno="o")
    # moduli di potenziamento
    for nome, simbolo in SIMBOLI_MODULO.items():
        colore = {"efficiency_upgrade": (90, 210, 90), "overclock_upgrade": (240, 80, 60),
                  "energy_storage_upgrade": (240, 210, 60), "ejector_upgrade": (240, 150, 50)}[nome]
        arte = [list(r) for r in MODULO]
        for y in range(6):
            for x in range(6):
                arte[4 + y][5 + x] = "s" if simbolo[y][x] == "s" else "f"
        s(["".join(r) for r in arte], {"b": (120, 130, 150), "k": ORO, "f": (40, 44, 54), "s": colore}, nome,
          senza_contorno="")
    # scanner
    for nome, corpo in [("ore_scanner_1", (80, 170, 80)), ("ore_scanner_2", (70, 110, 200)), ("ore_scanner_3", (60, 190, 200))]:
        s(SCANNER, {"b": corpo, "s": (24, 40, 30), "*": fisso((140, 255, 140)), "k": GRAFITE, "a": FERRO}, nome)
    # attrezzi elettrici
    s(TRAPANO, {"b": (222, 120, 40), "x": FERRO, "y": GRAFITE, "h": GRAFITE}, "electric_drill")
    s(TRAPANO, {"b": (222, 120, 40), "x": (110, 230, 225), "y": (40, 130, 130), "h": GRAFITE}, "diamond_drill")
    s(MOTOSEGA, {"b": (222, 120, 40), "s": FERRO, "t": GRAFITE, "h": GRAFITE}, "chainsaw")
    s(SPADA_PLASMA, {"l": (70, 210, 250), "!": fisso((230, 255, 255)), "g": ACCIAIO, "h": GRAFITE}, "plasma_saber")
    s(FUCILE, {"b": GRAFITE, "g": (90, 70, 60), "c": fisso((180, 110, 250)), "e": ACCIAIO, "a": ACCIAIO}, "pulse_rifle")
    s(MULTIMETRO, {"b": (230, 190, 50), "s": (30, 40, 34), "*": fisso((140, 255, 140)), "d": GRAFITE, "k": FERRO,
                   "r": fisso((200, 40, 40)), "n": fisso((30, 30, 30))}, "multimeter", senza_contorno="rn")
    s(CHIAVE_INGLESE, {"m": FERRO}, "wrench")
    s(TRONCHESE, {"s": FERRO, "k": GRAFITE, "r": (200, 50, 50)}, "cutters")
    s(RUBINETTO, {"w": LEGNO, "r": fisso((240, 170, 60))}, "tree_tap", senza_contorno="r")
    s(BACCHETTA, {"w": (90, 60, 120), "g": (200, 120, 250), "!": LUCE}, "builder_wand")
    s(RABDOMANTE, {"w": LEGNO}, "dowsing_rod")
    s(TALISMANO, {"c": fisso((214, 170, 60)), "p": ORO, "g": (120, 200, 255), "s": (70, 120, 200), "!": LUCE},
      "repair_talisman", senza_contorno="c")
    s(LENTE, {"f": ORO, "l": (170, 220, 240), "!": LUCE, "h": LEGNO}, "kinetic_lens")
    s(CLESSIDRA, {"w": LEGNO, "g": VETRO, "s": (190, 120, 240)}, "arcane_hourglass")
    s(LIBRO, {"c": (100, 50, 150), "e": ORO, "o": fisso((250, 230, 120)), "p": fisso((236, 228, 206)), "k": (60, 30, 90)},
      "omniscience_tome")
    tav = [list(r) for r in TAVOLETTA]
    for (x, y) in [(6, 5), (7, 5), (8, 5), (9, 5), (5, 6), (10, 6), (5, 7), (10, 7), (6, 8), (7, 8), (8, 8), (9, 8),
                   (7, 6), (8, 7)]:
        tav[y][x] = "r"
    for y in range(4, 10):
        for x in range(4, 12):
            if tav[y][x] == ".":
                tav[y][x] = "f"
    s(["".join(r) for r in tav], {"s": (150, 140, 130), "f": (120, 112, 104), "r": fisso((200, 140, 255))}, "synthesis_tablet")
    s(SACCA, {"b": (100, 60, 150), "s": (200, 170, 90), "k": ORO, "g": fisso((250, 220, 120))}, "dimensional_satchel",
      senza_contorno="s")
    s(SACCA, {"b": CUOIO, "s": (90, 60, 40), "k": FERRO, "g": fisso((120, 220, 255))}, "miner_bag", senza_contorno="s")
    s(JETPACK, {"t": ACCIAIO, "s": GRAFITE, "n": GRAFITE, "f": fisso((255, 170, 50)), "!": LUCE}, "jetpack",
      senza_contorno="f")
    s(CARRELLO, {"w": LEGNO, "k": FERRO, "m": (120, 124, 134), "r": GRAFITE}, "cargo_cart")
    s(TESTA_ROBOT, {"m": FERRO, "v": (24, 30, 40), "e": fisso((120, 240, 255)), "k": GRAFITE, "a": FERRO, "!": LUCE},
      "robot")
    s(TESTA_ROBOT, {"m": (110, 114, 124), "v": (30, 32, 38), "e": fisso((70, 74, 84)), "k": GRAFITE, "a": GRAFITE,
                    "!": fisso((160, 164, 170))}, "robot_chassis")
    s(TELECOMANDO, {"b": GRAFITE, "s": (40, 140, 190), "*": fisso((210, 250, 255)), "a": FERRO,
                    "r": fisso((230, 70, 60)), "g": fisso((90, 220, 90)), "y": fisso((240, 200, 60))}, "robot_controller")
    s(ARCHIVIO, {"b": (90, 60, 150), "s": (36, 28, 64), "*": fisso((200, 160, 255))}, "enchant_archive")
    s(SFERA, {"o": (150, 80, 230), "c": fisso((255, 255, 255)), "!": fisso((230, 200, 255))}, "infinite_core")
    s(SFERA, {"o": (60, 190, 190), "c": fisso((210, 255, 250)), "!": fisso((200, 255, 250))}, "soul_core")
    s(FIALA, {"n": (150, 110, 70), "k": FERRO, "g": VETRO, "q": (180, 80, 220), "!": LUCE}, "mob_sampler")
    s(GEMMA, {"g": (90, 220, 110), "d": (40, 150, 70), "!": LUCE}, "memory_gem")
    s(PALLINA, {"r": (50, 50, 56), "!": fisso((120, 120, 130))}, "rubber")
    s(GOCCIA, {"r": (232, 156, 44), "!": fisso((255, 230, 150))}, "sticky_resin")
    s(CONCIME, {"v": (90, 190, 60), "k": (120, 90, 50), "s": (196, 170, 120), "l": (90, 160, 60), "!": fisso((240, 225, 190))},
      "fertilizer")
    # cristalli d'essenza
    for i, (nome, c) in enumerate([("essence_crystal", (160, 100, 230)), ("essence_crystal_2", (240, 120, 170)),
                                   ("essence_crystal_3", (90, 170, 240)), ("essence_crystal_4", (230, 60, 70))]):
        s(cristallo(i + 1), {"c": c, "d": scurisci(c, 0.7), "!": LUCE, "b": (100, 96, 110)}, nome)
    # braci, catalizzatore, pietra mutevole
    s(BRACE, {"o": (200, 70, 160), "c": fisso((255, 220, 250)), "!": fisso((255, 190, 240)), "*": fisso((255, 140, 220))},
      "arcane_ember", senza_contorno="*")
    s(BRACE, {"o": (240, 120, 30), "c": fisso((255, 240, 160)), "!": fisso((255, 220, 120)), "*": fisso((255, 170, 60))},
      "eternal_ember", senza_contorno="*")
    s(BRACE, {"o": (240, 210, 80), "c": fisso((255, 255, 230)), "!": fisso((255, 250, 200)), "*": fisso((255, 240, 150))},
      "stellar_ember", senza_contorno="*")
    s(CATALIZZATORE, {"o": (40, 30, 60), "c": (110, 60, 170), "v": fisso((220, 160, 255)), "!": fisso((140, 110, 190)),
                      "s": fisso((170, 110, 240))}, "void_catalyst", senza_contorno="s")
    s(PIETRA, {"s": (120, 116, 130), "r": fisso((140, 230, 255)), "!": fisso((180, 176, 190))}, "shifting_stone")
    return n


if __name__ == "__main__":
    print(f"Grafica varie v5: {main()} texture ridisegnate")
