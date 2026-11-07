"""Genera il logo segnaposto della mod (src/main/resources/futuretek_logo.png).
Uso: python tools/genera_logo.py   (richiede Pillow: pip install pillow)"""
from pathlib import Path
from PIL import Image, ImageDraw

out = Path(__file__).resolve().parent.parent / "src/main/resources/futuretek_logo.png"
W, H = 256, 128
img = Image.new("RGBA", (W, H), (18, 24, 38, 255))
d = ImageDraw.Draw(img)
# bordo e ingranaggio stilizzato
d.rectangle([2, 2, W - 3, H - 3], outline=(0, 200, 255, 255), width=3)
cx, cy, r = 64, 64, 34
for i in range(8):
    import math
    a = i * math.pi / 4
    x, y = cx + math.cos(a) * (r + 6), cy + math.sin(a) * (r + 6)
    d.rectangle([x - 6, y - 6, x + 6, y + 6], fill=(255, 170, 0, 255))
d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(255, 170, 0, 255))
d.ellipse([cx - 14, cy - 14, cx + 14, cy + 14], fill=(18, 24, 38, 255))
# "FT" a blocchi
def block_text(x0, y0, rows, color):
    for j, row in enumerate(rows):
        for i, ch in enumerate(row):
            if ch == "#":
                d.rectangle([x0 + i * 10, y0 + j * 10, x0 + i * 10 + 8, y0 + j * 10 + 8], fill=color)
F = ["#####", "#....", "####.", "#....", "#....", "#...."]
T = ["#####", "..#..", "..#..", "..#..", "..#..", "..#.."]
block_text(118, 34, F, (0, 200, 255, 255))
block_text(178, 34, T, (0, 200, 255, 255))
out.parent.mkdir(parents=True, exist_ok=True)
img.save(out)
print("Creato", out)
