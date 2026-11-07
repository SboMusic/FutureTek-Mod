"""Texture del rifugio portatile (disegnate da zero, stile pixel 16x16)."""
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/futuretek/textures/block"


def var(c, rng, f=10):
    d = rng.randint(-f, f)
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + (c[3] if len(c) > 3 else 255,)


def muro():
    """Assi orizzontali dipinte di azzurro-grigio (stile casetta di legno), ombra sotto ogni asse."""
    rng = random.Random(11)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        riga = y % 4
        for x in range(16):
            if riga == 3:
                c = (96, 116, 128)            # ombra tra le assi
            elif riga == 0:
                c = (206, 222, 228)           # bordo alto illuminato
            else:
                c = (172, 194, 204)
            img.putpixel((x, y), var(c, rng, 6))
    # giunte sfalsate delle assi
    for y0, x in ((4, 11), (12, 4)):
        for y in range(y0, y0 + 3):
            img.putpixel((x, y), (130, 150, 162, 255))
    return img


def tetto():
    """Tegole rosso mattone a squame."""
    rng = random.Random(12)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        fila = y // 4
        sfas = 4 if fila % 2 else 0
        for x in range(16):
            yy = y % 4
            xx = (x + sfas) % 8
            if yy == 3 or (yy >= 2 and xx in (0, 7)):
                c = (96, 38, 30)
            elif yy == 0:
                c = (196, 82, 60)
            else:
                c = (170, 66, 48)
            img.putpixel((x, y), var(c, rng, 8))
    return img


def finestra():
    """Telaio di legno scuro con croce, vetro azzurrino semitrasparente."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                c = (92, 66, 44, 255)
            elif x in (7, 8) or y in (7, 8):
                c = (120, 88, 58, 255)
            else:
                c = (176, 214, 232, 110)
            img.putpixel((x, y), c)
    for x, y in ((2, 4), (3, 3), (4, 2), (10, 12), (11, 11), (12, 10), (2, 12), (10, 4)):
        img.putpixel((x, y), (240, 250, 255, 190))
    return img


def nucleo():
    """Scatola di metallo con una casetta luminosa al centro."""
    rng = random.Random(13)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            bordo = x in (0, 15) or y in (0, 15)
            c = (58, 62, 70) if bordo else (128, 134, 146)
            if (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14)):
                c = (190, 196, 206)       # bulloni
            img.putpixel((x, y), var(c, rng, 5))
    # schermo
    for y in range(3, 13):
        for x in range(3, 13):
            img.putpixel((x, y), (24, 30, 40, 255))
    casa = [
        "....yy....",
        "...yyyy...",
        "..yyyyyy..",
        ".yyyyyyyy.",
        "..y....y..",
        "..y.ww.y..",
        "..y.ww.y..",
        "..yyyyyy..",
    ]
    for j, riga in enumerate(casa):
        for i, ch in enumerate(riga):
            if ch == "y":
                img.putpixel((3 + i, 4 + j), (255, 206, 84, 255))
            elif ch == "w":
                img.putpixel((3 + i, 4 + j), (255, 246, 200, 255))
    return img


def main():
    TEX.mkdir(parents=True, exist_ok=True)
    for nome, f in (("shelter_wall", muro), ("shelter_roof", tetto), ("shelter_window", finestra),
                    ("shelter_core", nucleo)):
        f().save(TEX / f"{nome}.png")


if __name__ == "__main__":
    main()
