"""Immagini del rifugio portatile per la guida (fuori + spaccato). Solo texture della mod."""
import os
from PIL import Image, ImageDraw
import disegna_fattoria as df

MURO = df.blocco("block/shelter_wall", "block/shelter_wall", "block/shelter_wall")
TETTO = df.blocco("block/shelter_roof", "block/shelter_roof", "block/shelter_roof")
FIN = df.blocco("block/shelter_window", "block/shelter_window", "block/shelter_wall")
NUCLEO = df.blocco("block/shelter_core", "block/shelter_core", "block/shelter_core")
R = 3
C = (5, 1, 5)  # nucleo


def casa(sc, spaccato):
    cx, cy, cz = C
    for x in range(-R, R + 1):
        for z in range(-R, R + 1):
            bordo = abs(x) == R or abs(z) == R
            if not spaccato:
                sc.cubo(cx + x, cy + 3, cz + z, TETTO)
            if not bordo:
                continue
            # nello spaccato si tolgono i muri davanti (lato +x e +z, verso chi guarda)
            if spaccato and (x == R or z == R):
                continue
            for y in range(3):
                if z == R and x == 0 and y < 2:
                    continue  # porta
                win = y == 1 and ((abs(x) == R and z == 0) or (x == 0 and z == -R))
                sc.cubo(cx + x, cy + y, cz + z, FIN if win else MURO)
    sc.cubo(cx, cy, cz, NUCLEO)
    if spaccato:
        # letto (rosso), banco (legno), fornace (pietra): scatole semplici, niente texture vanilla
        sc.scatola(cx + 2, cy, cz - 2, cx + 3, cy + .56, cz, (200, 40, 40))
        sc.scatola(cx + 2, cy + .56, cz - 2, cx + 3, cy + .7, cz - 1.5, (235, 235, 235))
        sc.scatola(cx - 2, cy, cz - 2, cx - 1, cy + 1, cz - 1, (160, 110, 60))
        sc.scatola(cx - 1, cy, cz - 2, cx, cy + 1, cz - 1, (120, 120, 120))
    else:
        sc.scatola(cx - .05, cy, cz + R + .82, cx + 1.05, cy + 2, cz + R + .95, (110, 78, 50))


def fuori():
    sc = df.Scena()
    df.terreno(sc, 11)
    casa(sc, False)
    sc.etichetta((5.5, 3, 9), "Porta", (-150, 60), (255, 230, 140))
    sc.etichetta((9, 2.5, 5.5), "Finestre", (130, 40), (150, 220, 255))
    sc.etichetta((5.5, 5, 5.5), "Tetto", (0, -70), (255, 140, 140))
    return sc.disegna(titolo="Il rifugio visto da fuori (7x7)")


def dentro():
    sc = df.Scena()
    df.terreno(sc, 11)
    casa(sc, True)
    sc.etichetta((5.5, 2, 5.5), "Nucleo (rompilo = la casa sparisce)", (170, 110), (255, 230, 140))
    sc.etichetta((7.5, 1.6, 4), "Letto", (150, -60), (255, 140, 140))
    sc.etichetta((3.5, 2, 3.5), "Banco da lavoro", (-170, -60), (230, 190, 140))
    sc.etichetta((4.5, 2, 3.5), "Fornace", (-40, -110), (210, 210, 210))
    return sc.disegna(titolo="Dentro (senza tetto e muri davanti)")


if __name__ == "__main__":
    fuori().save(os.path.join(df.OUT, "rifugio_fuori.png"))
    dentro().save(os.path.join(df.OUT, "rifugio_dentro.png"))
