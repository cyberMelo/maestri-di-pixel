#!/usr/bin/env python3
"""Compone i personaggi dell'app dai livelli LPC (Universal LPC Spritesheet Character Generator).

Uso:
  python3 build_characters.py <cartella_generatore_lpc> app/src/main/resources

Per ogni personaggio produce sprites/characters/<id>.png: ciclo di camminata LPC
(4 righe: su, sinistra, giù, destra; 9 fotogrammi da 64x64). Per i cappelli
dell'armadietto produce sprites/hats/<id>.png, già allineati alla testa da bambino.
Scrive anche sprites/CREDITS-characters.csv con autori e licenze dei livelli usati.
"""
import csv
import json
import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
PAL = {k: json.loads((HERE / "palettes" / f"{k}_ulpc.json").read_text()) for k in ("hair", "body", "cloth", "eye", "metal")}
# Palette di partenza dei livelli LPC "base" (da ricolorare)
BASE = {"hair": "orange", "body": "light", "cloth": "white", "eye": "blue", "metal": "steel"}


def hexrgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def tint(im, palette):
    """Ricolora per luminosità: i colori del livello, dal più scuro al più chiaro, vanno sulla palette."""
    dst = [hexrgb(c) for c in palette]
    colors = sorted({c[1][:3] for c in im.getcolors(65536) if c[1][3]}, key=lambda c: 299 * c[0] + 587 * c[1] + 114 * c[2])
    mapping = {c: dst[min(len(dst) - 1, i * len(dst) // max(1, len(colors)))] for i, c in enumerate(colors)}
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a:
                px[x, y] = mapping[(r, g, b)] + (a,)
    return im


def recolor(im, kind, target):
    if kind == "tint":
        return tint(im, PAL["cloth"][target])
    if target is None or target == BASE[kind]:
        return im
    src = [hexrgb(c) for c in PAL[kind][BASE[kind]]]
    dst = [hexrgb(c) for c in PAL[kind][target]]
    mapping = dict(zip(src, dst))
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a and (r, g, b) in mapping:
                px[x, y] = mapping[(r, g, b)] + (a,)
    return im


class Composer:
    def __init__(self, root: Path):
        self.root = root / "spritesheets"
        self.used = set()

    def layer(self, rel, kind=None, color=None, post=None):
        path = self.root / rel
        self.used.add(rel)
        im = Image.open(path).convert("RGBA")
        im = recolor(im, kind, color) if kind else im
        return add_child_shoes(im) if post == "shoes" else im

    def build(self, layers):
        out = Image.new("RGBA", (576, 256))
        for spec in layers:
            out.alpha_composite(self.layer(*spec))
        return out


SKIN = "body"


def add_child_shoes(im, shoe=((40, 24, 32), (77, 74, 93))):
    """LPC non ha scarpe da bambino: colora le ultime 2 righe di ogni piede (scuro + riflesso)."""
    px = im.load()
    for row in range(4):
        for col in range(9):
            x0, y0 = col * 64, row * 64
            bbox = im.crop((x0, y0, x0 + 64, y0 + 64)).getbbox()
            if not bbox:
                continue
            bottom = y0 + bbox[3] - 1
            for y in (bottom - 1, bottom):
                for x in range(x0, x0 + 64):
                    r, g, b, a = px[x, y]
                    if a:
                        px[x, y] = (shoe[0] if (r + g + b) < 300 or y == bottom else shoe[1]) + (a,)
    return im


def child(skin, hair_style, hair_color, shirt, bottom, bottom_color, glasses=False):
    layers = [
        ("body/bodies/child/walk.png", SKIN, skin, "shoes"),
        ("head/heads/human/child/walk.png", SKIN, skin),
        ("eyes/human/child/walk/brown.png",),
        (f"legs/{bottom}/child/walk/{bottom_color}.png",),
        (f"torso/clothes/shirt/child/walk/{shirt}.png",),
        (f"hair/{hair_style}/child/walk.png", "hair", hair_color),
    ]
    if glasses:
        layers.append(("facial/glasses/glasses/child/walk.png",))
    return layers


def adult(sex, skin, hair_style, hair_color, torso, torso_color, legs, legs_color, extra=()):
    body = "male" if sex == "m" else "female"
    fit = "male" if sex == "m" else "thin"
    layers = [
        (f"body/bodies/{body}/walk.png", SKIN, skin),
        (f"head/heads/human/{body}/walk.png", SKIN, skin),
        (f"feet/shoes/basic/{fit}/walk.png", "cloth", "black"),
        (f"legs/{legs}/{fit}/walk.png", "cloth", legs_color),
    ]
    layers.append((torso.format(body=body), "cloth", torso_color) if torso_color else (torso.format(body=body),))
    layers.extend(extra)
    layers.append((f"hair/{hair_style}/adult/walk.png", "hair", hair_color))
    return layers


LONG = "torso/clothes/longsleeve/longsleeve/{body}/walk.png"
SHORT = "torso/clothes/shortsleeve/shortsleeve/{body}/walk.png"

CHARACTERS = {
    # Avatar giocabili (bambini di 4a-5a elementare)
    "avatar_0": child("light", "swoop_side", "chestnut", "blue", "pants", "darkblue"),
    "avatar_1": child("light", "wavy", "light_brown", "pink", "skirts", "maroon"),
    "avatar_2": child("bronze", "jewfro", "black", "green", "pants", "brown", glasses=True),
    "avatar_3": child("amber", "parted_side_bangs", "ginger", "lavender", "skirts", "darkblue"),
    "npc_classmate": child("light", "relm_short", "dark_brown", "lightblue", "skirts", "green"),
    # Maestri e personale della scuola
    "npc_history": adult("m", "light", "balding", "white", LONG, "brown", "pants", "tan",
                         extra=[("beards/beard/medium/walk.png", "hair", "white"),
                                ("facial/glasses/round/adult/walk.png",)]),
    "npc_science": adult("f", "light", "bob", "dark_brown", LONG, "white", "skirts/plain", "green"),
    "npc_math": adult("m", "olive", "buzzcut", "black", SHORT, "red", "pants", "charcoal"),
    "npc_library": adult("f", "light", "bangs_bun", "purple", LONG, "purple", "skirts/plain", "navy",
                         extra=[("facial/glasses/round/adult/walk.png",)]),
    "npc_geo": adult("m", "taupe", "parted", "light_brown", SHORT, "sky", "pants", "tan",
                     extra=[("hat/cloth/leather_cap/adult/walk/brown.png",)]),
    "npc_english": adult("f", "light", "long_straight", "ginger", LONG, "blue", "skirts/plain", "white"),
    "npc_boss": adult("f", "amber", "bob_side_part", "gray", "torso/clothes/robe/female/walk/purple.png", None,
                      "pants", "charcoal"),
    "npc_janitor": adult("m", "light", "plain", "gray", LONG, "white", "pants", "navy",
                         extra=[("torso/aprons/overalls/male/walk/navy.png",),
                                ("hat/cloth/leather_cap/adult/walk/blue.png",)]),
}

# (livello, spostamento extra verso l'alto in px: i cappelli adulti sono grandi per la testa da bambino)
HATS = {
    "hat_cap": ([("hat/cloth/leather_cap/adult/walk/red.png",)], 1),
    "hat_wizard": ([("hat/magic/wizard/base/adult/walk.png", "tint", "purple")], 4),
    "hat_astro": ([("hat/helmet/bascinet_round/adult/walk.png", "tint", "white")], 2),
    "hat_crown": ([("hat/formal/crown/adult/walk.png",)], 0),
}


def head_top(im, col, row):
    """Prima riga non trasparente del fotogramma (cima della testa)."""
    frame = im.crop((col * 64, row * 64, col * 64 + 64, row * 64 + 64))
    bbox = frame.getbbox()
    return bbox[1] if bbox else 0


def child_hat(composer, spec, lift):
    """I cappelli LPC esistono solo per teste adulte: si abbassano fotogramma per fotogramma
    della differenza di altezza tra testa adulta e testa da bambino."""
    hat = composer.build(spec)
    adult_head = composer.layer("head/heads/human/male/walk.png")
    child_head = composer.layer("head/heads/human/child/walk.png")
    out = Image.new("RGBA", hat.size)
    for row in range(4):
        for col in range(9):
            dy = head_top(child_head, col, row) - head_top(adult_head, col, row) - lift
            frame = hat.crop((col * 64, row * 64, col * 64 + 64, row * 64 + 64))
            out.alpha_composite(frame, (col * 64, row * 64 + dy))
    return out


def write_credits(root: Path, used, out_csv: Path):
    """Per ogni livello usato copia la riga di CREDITS.csv del generatore: prima cerca
    '<cartella>/walk.png', poi la riga più vicina risalendo le cartelle."""
    rows = list(csv.reader((root / "CREDITS.csv").open(encoding="utf-8")))
    header, body = rows[0], [[c.strip() for c in r] for r in rows[1:]]
    by_name = {r[0]: r for r in body}
    picked = {}
    for u in sorted(used):
        base = u.split("/walk", 1)[0]
        row = by_name.get(f"{base}/walk.png")
        prefix = base
        while row is None and "/" in prefix:
            row = next((r for r in body if r[0].startswith(prefix + "/")), None)
            prefix = prefix.rsplit("/", 1)[0]
        picked[base] = row or [u, "nessuna riga in CREDITS.csv", "", "", ""]
    with out_csv.open("w", encoding="utf-8", newline="") as f:
        w = csv.writer(f)
        w.writerow(header)
        for base in sorted(picked):
            w.writerow([base] + picked[base][1:])


def main():
    root, assets = Path(sys.argv[1]), Path(sys.argv[2])
    composer = Composer(root)
    out_chars = assets / "sprites" / "characters"
    out_hats = assets / "sprites" / "hats"
    out_chars.mkdir(parents=True, exist_ok=True)
    out_hats.mkdir(parents=True, exist_ok=True)
    for cid, layers in CHARACTERS.items():
        composer.build(layers).save(out_chars / f"{cid}.png", optimize=True)
    for hid, (spec, lift) in HATS.items():
        child_hat(composer, spec, lift).save(out_hats / f"{hid}.png", optimize=True)
    write_credits(root, composer.used, assets / "sprites" / "CREDITS-characters.csv")
    print(f"{len(CHARACTERS)} personaggi, {len(HATS)} cappelli")


if __name__ == "__main__":
    main()
