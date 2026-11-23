#!/usr/bin/env python3
"""Mascotte dell'armadietto dai fogli LPC (gatti, cani, uccellini).

Uso:
  python3 build_pets.py <cartella_fogli_lpc> app/src/main/resources

Fogli da OpenGameArt: lpc-cats-and-dogs (cat_0.png, dog_2.png; bluecarrot16,
CC-BY 3.0 / GPL / OGA-BY) e lpc-birds (bird_1_brown.png; CC-BY / CC-BY-SA / GPL / OGA-BY).
Per ogni mascotte scrive sprites/pets/<id>.png: due fotogrammi 32x32 affiancati,
di profilo verso destra. Drago e robot non esistono in LPC e restano disegnati dall'app.
"""
import colorsys
import sys
from pathlib import Path

from PIL import Image, ImageOps

T = 32


def frames(sheet: Image.Image, cols, row, mirror=False):
    out = Image.new("RGBA", (T * 2, T))
    for i, c in enumerate(cols):
        f = sheet.crop((c * T, row * T, c * T + T, row * T + T))
        out.alpha_composite(ImageOps.mirror(f) if mirror else f, (i * T, 0))
    return out


def to_fox(img: Image.Image) -> Image.Image:
    """Cane dorato -> volpe: tonalità arancio-rossa più satura, chiari quasi bianchi."""
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if not a:
                continue
            h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
            if s > 0.25:  # pelo colorato, non contorni scuri né occhi
                h, s = 0.055, min(1.0, s * 1.25)
                l = min(0.95, l * 1.25) if l > 0.75 else l
            r, g, b = (int(v * 255) for v in colorsys.hls_to_rgb(h, l, s))
            px[x, y] = (r, g, b, a)
    return img


def main():
    src, res = Path(sys.argv[1]), Path(sys.argv[2])
    out = res / "sprites" / "pets"
    out.mkdir(parents=True, exist_ok=True)
    cat = Image.open(src / "cat_0.png").convert("RGBA")
    dog = Image.open(src / "dog_2.png").convert("RGBA")
    bird = Image.open(src / "bird_1_brown.png").convert("RGBA")
    # Riga 3 = di profilo verso destra; colonne di colore: 0 bianco, 4 arancio/marrone, 8 dorato, 12 nero
    pets = {
        "pet_cat": frames(cat, (4, 5), 3),
        "pet_dog": frames(dog, (4, 5), 3),
        "pet_fox": to_fox(frames(dog, (8, 9), 3)),
        "pet_owl": frames(bird, (0, 1), 4, mirror=True),  # uccellino a terra, girato verso destra
    }
    for pid, img in pets.items():
        img.save(out / f"{pid}.png", optimize=True)
    (res / "sprites" / "CREDITS-pets.txt").write_text(
        '"[LPC] Cats and Dogs" by bluecarrot16 - CC-BY 3.0 / GPL 3.0 / GPL 2.0 / OGA-BY 3.0\n'
        "https://opengameart.org/content/lpc-cats-and-dogs\n"
        '"[LPC] Birds" - CC-BY 3.0 / CC-BY-SA 3.0 / GPL 3.0 / OGA-BY 3.0\n'
        "https://opengameart.org/content/lpc-birds\n"
        "Volpe: ricolorazione del cane dorato fatta da tools/art/build_pets.py.\n",
        encoding="utf-8",
    )
    print(len(pets), "mascotte")


if __name__ == "__main__":
    main()
