"""Disegna le "foto" isometriche della fattoria di creature (per la guida).

Usa solo le texture della mod (niente texture di Minecraft): il prato e le
creature sono disegnati qui con colori semplici.

    python3 tools/disegna_fattoria.py      -> docs/img/fattoria_*.png
"""
import os
import random
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(ROOT, "src/main/resources/assets/futuretek/textures")
OUT = os.path.join(ROOT, "docs/img")
S = 48  # lato di un blocco in pixel sullo schermo

FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FONT_R = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"


def font(size, bold=True):
    try:
        return ImageFont.truetype(FONT if bold else FONT_R, size)
    except OSError:
        return ImageFont.load_default()


def tex(name):
    return Image.open(os.path.join(TEX, name + ".png")).convert("RGBA").crop((0, 0, 16, 16))


def erba():
    rnd = random.Random(7)
    top = Image.new("RGBA", (16, 16))
    side = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            g = rnd.randint(-14, 14)
            top.putpixel((x, y), (92 + g, 150 + g, 62 + g, 255))
            d = rnd.randint(-12, 12)
            side.putpixel((x, y), (124 + d, 88 + d, 60 + d, 255) if y > 3 + (x * 7 % 3)
                          else (92 + g, 150 + g, 62 + g, 255))
    return {"top": top, "left": side, "right": side}


def blocco(fronte, lato, sopra):
    return {"top": tex(sopra), "left": tex(fronte), "right": tex(lato)}


INCUBATORE = blocco("block/mob_incubator_front_on", "block/mob_incubator_side", "block/mob_incubator_top")
TORRETTA = blocco("block/laser_turret_front_on", "block/laser_turret_side", "block/laser_turret_top")
SOLARE = blocco("block/machine_side", "block/machine_side", "block/solar_3_top")
BAULE = blocco("block/iron_chest_front", "block/iron_chest_side", "block/iron_chest_top")
PRATO = erba()


class Scena:
    def __init__(self):
        self.cose = []      # (profondità, funzione di disegno)
        self.etichette = []  # (punto 3D, testo, (dx, dy), colore)
        self.zone = []      # (x0, z0, x1, z1, y, colore)

    # proiezione: x verso destra-giù, z verso sinistra-giù, y in su
    @staticmethod
    def p(x, y, z):
        return ((x - z) * S, (x + z) * S / 2 - y * S)

    def cubo(self, x, y, z, facce):
        self.cose.append((x + y + z + 1.5, y, lambda d, o: self._cubo(d, o, x, y, z, facce)))

    def scatola(self, x0, y0, z0, x1, y1, z1, colore):
        c = ((x0 + x1) / 2) + ((y0 + y1) / 2) + ((z0 + z1) / 2)
        self.cose.append((c + 0.6, y0, lambda d, o: self._scatola(d, o, x0, y0, z0, x1, y1, z1, colore)))

    def creatura(self, x, z, y=1.0, girata=False):
        """Una creatura generica a quattro zampe (non una mucca di Minecraft)."""
        corpo = (170, 120, 80)
        chiaro = (226, 210, 180)
        if girata:
            self.scatola(x - .3, y + .45, z - .2, x + .3, y + .95, z + .2, corpo)
            self.scatola(x + .3, y + .6, z - .17, x + .62, y + 1.0, z + .17, chiaro)
            for dx in (-.25, .15):
                for dz in (-.15, .07):
                    self.scatola(x + dx, y, z + dz, x + dx + .1, y + .45, z + dz + .08, corpo)
        else:
            self.scatola(x - .2, y + .45, z - .3, x + .2, y + .95, z + .3, corpo)
            self.scatola(x - .17, y + .6, z + .3, x + .17, y + 1.0, z + .62, chiaro)
            for dx in (-.15, .07):
                for dz in (-.25, .15):
                    self.scatola(x + dx, y, z + dz, x + dx + .08, y + .45, z + dz + .1, corpo)

    def zona(self, x0, z0, x1, z1, y, colore):
        self.zone.append((x0, z0, x1, z1, y, colore))

    def etichetta(self, punto, testo, spost, colore=(255, 255, 255)):
        self.etichette.append((punto, testo, spost, colore))

    # ---- disegno -----------------------------------------------------------------
    def _faccia(self, d, o, origine, du, dv, img, luce):
        for u in range(16):
            for v in range(16):
                r, g, b, a = img.getpixel((u, v))
                if a < 10:
                    continue
                pts = []
                for (uu, vv) in ((u, v), (u + 1, v), (u + 1, v + 1), (u, v + 1)):
                    X = origine[0] + du[0] * uu / 16 + dv[0] * vv / 16
                    Y = origine[1] + du[1] * uu / 16 + dv[1] * vv / 16
                    Z = origine[2] + du[2] * uu / 16 + dv[2] * vv / 16
                    sx, sy = self.p(X, Y, Z)
                    pts.append((sx + o[0], sy + o[1]))
                col = (int(r * luce), int(g * luce), int(b * luce))
                d.polygon(pts, fill=col, outline=col)

    def _cubo(self, d, o, x, y, z, f):
        self._faccia(d, o, (x, y + 1, z), (1, 0, 0), (0, 0, 1), f["top"], 1.0)
        self._faccia(d, o, (x, y + 1, z + 1), (1, 0, 0), (0, -1, 0), f["left"], 0.82)
        self._faccia(d, o, (x + 1, y + 1, z + 1), (0, 0, -1), (0, -1, 0), f["right"], 0.64)
        # bordino scuro per far "staccare" il blocco
        pts = [self.p(x, y + 1, z), self.p(x + 1, y + 1, z), self.p(x + 1, y, z), self.p(x + 1, y, z + 1),
               self.p(x, y, z + 1), self.p(x, y + 1, z + 1)]
        d.line([(a + o[0], b + o[1]) for a, b in pts + pts[:1]], fill=(30, 30, 30), width=1)

    def _scatola(self, d, o, x0, y0, z0, x1, y1, z1, c):
        def P(x, y, z):
            a, b = self.p(x, y, z)
            return (a + o[0], b + o[1])
        sc = lambda k: tuple(int(v * k) for v in c)
        d.polygon([P(x0, y1, z0), P(x1, y1, z0), P(x1, y1, z1), P(x0, y1, z1)], fill=c, outline=(40, 30, 20))
        d.polygon([P(x0, y1, z1), P(x1, y1, z1), P(x1, y0, z1), P(x0, y0, z1)], fill=sc(.8), outline=(40, 30, 20))
        d.polygon([P(x1, y1, z1), P(x1, y1, z0), P(x1, y0, z0), P(x1, y0, z1)], fill=sc(.62), outline=(40, 30, 20))

    def disegna(self, titolo=None, margine=40):
        # calcolo dei bordi con un disegno di prova
        xs, ys = [], []
        for k in self.cose:
            pass
        punti = []
        for (_, _, f) in self.cose:
            pass
        # bordi stimati dalle etichette e dalle forme: semplice e sufficiente
        prova = Image.new("RGBA", (4000, 4000), (0, 0, 0, 0))
        dp = ImageDraw.Draw(prova)
        o = (2000, 2000)
        for _, _, f in sorted(self.cose, key=lambda t: (t[0], t[1])):
            f(dp, o)
        bb = prova.getbbox() or (1990, 1990, 2010, 2010)
        for punto, testo, (dx, dy), _ in self.etichette:
            sx, sy = self.p(*punto)
            tx, ty = sx + o[0] + dx, sy + o[1] + dy
            w = font(17).getlength(testo) + 20
            bb = (min(bb[0], tx - w / 2), min(bb[1], ty - 16), max(bb[2], tx + w / 2), max(bb[3], ty + 16))
        top = 54 if titolo else 0
        W = int(bb[2] - bb[0] + 2 * margine)
        H = int(bb[3] - bb[1] + 2 * margine + top)
        img = Image.new("RGB", (W, H), (34, 38, 46))
        d = ImageDraw.Draw(img, "RGBA")
        o = (o[0] - bb[0] + margine, o[1] - bb[1] + margine + top)

        cose = sorted(self.cose, key=lambda t: (t[0], t[1]))
        # prima il terreno (y <= 0), poi le zone colorate, poi il resto
        for prof, y, f in cose:
            if y <= 0:
                f(d, o)
        for x0, z0, x1, z1, y, c in self.zone:
            P = lambda x, z: tuple(a + b for a, b in zip(self.p(x, y, z), o))
            d.polygon([P(x0, z0), P(x1, z0), P(x1, z1), P(x0, z1)], fill=c + (85,), outline=c + (255,), width=3)
        for prof, y, f in cose:
            if y > 0:
                f(d, o)
        for punto, testo, (dx, dy), col in self.etichette:
            sx, sy = self.p(*punto)
            a = (sx + o[0], sy + o[1])
            b = (a[0] + dx, a[1] + dy)
            d.line([a, b], fill=(255, 255, 255, 230), width=2)
            d.ellipse([a[0] - 4, a[1] - 4, a[0] + 4, a[1] + 4], fill=(255, 255, 255))
            f = font(17)
            w = f.getlength(testo)
            d.rounded_rectangle([b[0] - w / 2 - 9, b[1] - 15, b[0] + w / 2 + 9, b[1] + 15], 7,
                                fill=(15, 17, 22, 235), outline=col, width=2)
            d.text((b[0], b[1]), testo, font=f, fill=col, anchor="mm")
        if titolo:
            d.text((W / 2, 30), titolo, font=font(24), fill=(255, 255, 255), anchor="mm")
        return img


def terreno(sc, n=9):
    for x in range(n):
        for z in range(n):
            sc.cubo(x, 0, z, PRATO)


# Disposizione (coordinate dei blocchi):
#   incubatore (4,1,4) · pannello sopra (4,2,4) · pannello accanto (3,1,4)
#   torretta (5,1,4) attaccata all'incubatore · baule sopra la torretta (5,2,4)
#   pannello della torretta (5,1,3), dietro, così non copre niente
INC, TOR = (4, 1, 4), (5, 1, 4)


def passo_incubatore(sc):
    sc.cubo(*INC, INCUBATORE)
    sc.cubo(4, 2, 4, SOLARE)
    sc.cubo(3, 1, 4, SOLARE)


def passo_torretta(sc):
    sc.cubo(*TOR, TORRETTA)
    sc.cubo(5, 2, 4, BAULE)
    sc.cubo(5, 1, 3, SOLARE)


def icona_oggetto(nome, lato=96):
    return tex(nome).resize((lato, lato), Image.NEAREST)


def cosa_serve():
    voci = [("1 Incubatore\ndi creature", INCUBATORE), ("1 Torretta\nlaser", TORRETTA),
            ("3 Pannelli\nsolari III", SOLARE), ("1 Baule\n(va bene anche normale)", BAULE)]
    celle = []
    for testo, facce in voci:
        sc = Scena()
        sc.cubo(0, 0, 0, facce)
        celle.append((testo, sc.disegna(margine=10)))
    campione = icona_oggetto("item/mob_sampler", 80)
    larg = 230
    W, H = larg * 5, 310
    img = Image.new("RGB", (W, H), (34, 38, 46))
    d = ImageDraw.Draw(img)
    d.text((W / 2, 30), "Cosa ti serve", font=font(26), fill=(255, 255, 255), anchor="mm")
    for i, (testo, im) in enumerate(celle):
        cx = larg * i + larg // 2
        img.paste(im, (int(cx - im.width / 2), 70))
        d.multiline_text((cx, 222), testo, font=font(16), fill=(255, 230, 140), anchor="mm", align="center")
    cx = larg * 4 + larg // 2
    img.paste(campione, (cx - 40, 85), campione)
    d.multiline_text((cx, 222), "1 Campionatore\ndi creature", font=font(16), fill=(255, 230, 140), anchor="mm", align="center")
    d.text((W / 2, 282), "Niente cavi: i pannelli danno energia ai blocchi che toccano.",
           font=font(17, False), fill=(200, 210, 220), anchor="mm")
    return img


def passo1():
    """Prendere il campione: click destro sulla creatura col campionatore."""
    sc = Scena()
    for x in range(4):
        for z in range(3):
            sc.cubo(x, 0, z, PRATO)
    sc.creatura(2.5, 1.5, girata=True)
    sc.etichetta((2.5, 2.1, 1.5), "Click destro sulla creatura", (0, -60), (255, 230, 140))
    img = sc.disegna()
    img = img.crop((0, 0, img.width, img.height))
    W = img.width + 260
    out = Image.new("RGB", (W, img.height + 60), (34, 38, 46))
    out.paste(img, (260, 30))
    d = ImageDraw.Draw(out)
    d.text((W / 2, 28), "Passo 1 · Prendi il campione", font=font(24), fill=(255, 255, 255), anchor="mm")
    camp = icona_oggetto("item/mob_sampler", 112)
    out.paste(camp, (60, img.height // 2 - 70), camp)
    d.text((116, img.height // 2 + 62), "Campionatore", font=font(16), fill=(255, 255, 255), anchor="mm")
    d.text((116, img.height // 2 + 84), "in mano", font=font(16, False), fill=(200, 210, 220), anchor="mm")
    y = img.height // 2 - 14
    d.line([(190, y), (300, y)], fill=(255, 230, 140), width=6)
    d.polygon([(300, y - 14), (300, y + 14), (324, y)], fill=(255, 230, 140))
    d.text((W / 2, img.height + 38), "Il campionatore diventa luccicante: dentro c'è quella creatura.",
           font=font(17, False), fill=(200, 210, 220), anchor="mm")
    return out


def passo2():
    sc = Scena()
    terreno(sc)
    passo_incubatore(sc)
    sc.zona(2, 2, 7, 7, 1, (255, 215, 0))
    sc.etichetta((4.5, 1.5, 5), "Incubatore (campionatore dentro)", (170, 120), (255, 230, 140))
    sc.etichetta((4.5, 3, 4.5), "Pannello solare III sopra", (120, -55), (150, 220, 255))
    sc.etichetta((3.2, 2, 4.2), "Pannello solare III attaccato", (-190, -40), (150, 220, 255))
    sc.etichetta((2.6, 1, 6.6), "Qui compaiono le creature (5x5)", (-130, 90), (255, 215, 0))
    return sc.disegna(titolo="Passo 2 · Incubatore + 2 pannelli")


def passo3():
    sc = Scena()
    terreno(sc)
    passo_incubatore(sc)
    passo_torretta(sc)
    sc.zona(2, 1, 9, 8, 1, (255, 70, 70))
    sc.etichetta((5.5, 1.5, 5), "Torretta laser (attaccata)", (150, 110), (255, 140, 140))
    sc.etichetta((5.5, 3, 4.5), "Baule sopra la torretta", (150, -60), (255, 255, 255))
    sc.etichetta((5.9, 2, 3.2), "Pannello solare III (dietro)", (170, -20), (150, 220, 255))
    sc.etichetta((8.6, 1, 7.6), "Zona colpita dalla torretta (7x7)", (60, 70), (255, 140, 140))
    return sc.disegna(titolo="Passo 3 · Torretta + baule + pannello")


def passo4():
    sc = Scena()
    terreno(sc)
    passo_incubatore(sc)
    passo_torretta(sc)
    sc.creatura(2.5, 2.5, girata=True)
    sc.creatura(6.5, 6.4)
    sc.creatura(2.6, 6.3)
    sc.creatura(6.4, 2.5, girata=True)
    sc.etichetta((2.5, 2.1, 2.5), "Le creature compaiono…", (-130, -60), (255, 230, 140))
    sc.etichetta((6.5, 2.1, 6.4), "…la torretta le colpisce", (150, 40), (255, 140, 140))
    sc.etichetta((5.5, 3, 4.5), "Carne e pelle finiscono qui", (120, -70), (140, 255, 160))
    return sc.disegna(titolo="Passo 4 · Funziona da sola!")


def main():
    os.makedirs(OUT, exist_ok=True)
    for nome, f in (("fattoria_0_serve", cosa_serve), ("fattoria_1_campione", passo1),
                    ("fattoria_2_incubatore", passo2), ("fattoria_3_torretta", passo3),
                    ("fattoria_4_finita", passo4)):
        f().save(os.path.join(OUT, nome + ".png"))
        print("ok", nome)


if __name__ == "__main__":
    main()
