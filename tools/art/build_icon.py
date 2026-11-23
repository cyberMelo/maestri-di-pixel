#!/usr/bin/env python3
"""Icona dell'app in pixel art: l'avatar dell'app con un libro aperto e una stella d'oro.

Uso:
  python3 build_icon.py app/src/main/res

Disegna su una griglia di 108 "pixel" (= 108 dp dell'icona adattiva) e scrive:
- drawable-nodpi/ic_launcher_fg.png e ic_launcher_bg.png (432 px, livelli adattivi),
- drawable-nodpi/ic_launcher_mono.png (sagoma per le icone a tema),
- mipmap-*/ic_launcher.png e ic_launcher_round.png per Android 7 (API 24-25).
"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw

HERE = Path(__file__).resolve().parent
RES_SPRITES = HERE.parent.parent / "app" / "src" / "main" / "resources" / "sprites"
G = 108          # griglia dell'icona adattiva (dp)
OUT = 432        # xxxhdpi: 4 px per dp
SAFE = 66        # zona sempre visibile al centro, qualunque forma usi il launcher


def background() -> Image.Image:
    img = Image.new("RGBA", (G, G))
    d = ImageDraw.Draw(img)
    top, bottom = (30, 41, 110), (88, 52, 160)
    for y in range(G):
        t = y / (G - 1)
        d.line([(0, y), (G, y)], fill=tuple(int(a + (b - a) * t) for a, b in zip(top, bottom)) + (255,))
    for x, y, big in [(14, 16, 1), (30, 9, 0), (78, 12, 1), (95, 26, 0), (20, 40, 0), (88, 44, 1), (60, 6, 0), (10, 70, 0), (99, 66, 0)]:
        d.point((x, y), fill=(255, 255, 255, 255))
        if big:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                d.point((x + dx, y + dy), fill=(190, 210, 255, 255))
    # Collina del cortile in basso
    for x in range(G):
        hy = int(90 + 6 * ((x - 54) / 54) ** 2)
        d.line([(x, hy), (x, G)], fill=(52, 140, 72, 255))
        d.point((x, hy), fill=(98, 190, 96, 255))
    return img


def foreground() -> Image.Image:
    img = Image.new("RGBA", (G, G))
    sheet = Image.open(RES_SPRITES / "characters" / "avatar_0.png").convert("RGBA")
    # Fotogramma di fronte (riga 2, colonna 0), ritagliato su testa e busto
    frame = sheet.crop((0, 128, 64, 192))
    bbox = frame.getbbox()
    bust = frame.crop((bbox[0], bbox[1], bbox[2], bbox[1] + 27))
    scale = 2
    bust = bust.resize((bust.width * scale, bust.height * scale), Image.NEAREST)
    bx = (G - bust.width) // 2
    by = (G - SAFE) // 2 + 4
    img.alpha_composite(bust, (bx, by))
    d = ImageDraw.Draw(img)
    # Libro aperto davanti al busto
    cx, top = G // 2, by + bust.height - 15
    d.polygon([(cx - 20, top + 2), (cx - 1, top + 5), (cx - 1, top + 19), (cx - 20, top + 16)], fill=(250, 246, 232, 255), outline=(60, 40, 30, 255))
    d.polygon([(cx + 1, top + 5), (cx + 20, top + 2), (cx + 20, top + 16), (cx + 1, top + 19)], fill=(250, 246, 232, 255), outline=(60, 40, 30, 255))
    for i in range(3):
        d.line([(cx - 16, top + 7 + i * 3), (cx - 5, top + 9 + i * 3)], fill=(150, 150, 170, 255))
        d.line([(cx + 5, top + 9 + i * 3), (cx + 16, top + 7 + i * 3)], fill=(150, 150, 170, 255))
    d.rectangle((cx - 21, top + 17, cx + 21, top + 20), fill=(180, 60, 50, 255))
    # Stella d'oro in alto a destra, dentro la zona sicura
    sx, sy = G // 2 + 19, G // 2 - 19
    star = [(0, -7), (2, -2), (7, -2), (3, 1), (5, 7), (0, 3), (-5, 7), (-3, 1), (-7, -2), (-2, -2)]
    d.polygon([(sx + x, sy + y) for x, y in star], fill=(253, 208, 64, 255), outline=(150, 90, 10, 255))
    d.point((sx - 1, sy - 2), fill=(255, 245, 190, 255))
    return img


def up(img: Image.Image, size: int) -> Image.Image:
    return img.resize((size, size), Image.NEAREST if size % G == 0 else Image.LANCZOS)


def main():
    res = Path(sys.argv[1])
    bg, fg = background(), foreground()
    nodpi = res / "drawable-nodpi"
    nodpi.mkdir(exist_ok=True)
    up(bg, OUT).save(nodpi / "ic_launcher_bg.png", optimize=True)
    up(fg, OUT).save(nodpi / "ic_launcher_fg.png", optimize=True)
    mono = Image.new("RGBA", fg.size)
    mono.putalpha(fg.getchannel("A"))
    up(mono, OUT).save(nodpi / "ic_launcher_mono.png", optimize=True)
    # Icone classiche per Android 7: livelli fusi, ritaglio centrale (le icone legacy sono 48 dp su 72)
    full = bg.copy()
    full.alpha_composite(fg)
    legacy = full.crop((18, 18, 90, 90))
    for folder, size in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
        icon = legacy.resize((size, size), Image.LANCZOS)
        sq = Image.new("RGBA", (size, size))
        ImageDraw.Draw(sq).rounded_rectangle((0, 0, size - 1, size - 1), radius=size // 6, fill=(255, 255, 255, 255))
        out = Image.new("RGBA", (size, size))
        out.paste(icon, (0, 0), sq)
        out.save(res / f"mipmap-{folder}" / "ic_launcher.png", optimize=True)
        circle = Image.new("L", (size, size))
        ImageDraw.Draw(circle).ellipse((0, 0, size - 1, size - 1), fill=255)
        rnd = Image.new("RGBA", (size, size))
        rnd.paste(icon, (0, 0), circle)
        rnd.save(res / f"mipmap-{folder}" / "ic_launcher_round.png", optimize=True)
    print("icona scritta")


if __name__ == "__main__":
    main()
