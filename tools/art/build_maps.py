#!/usr/bin/env python3
"""Compone gli sfondi del Cortile e dell'Atrio dalle piastrelle LPC (32x32).

Uso:
  python3 build_maps.py <cartella_sorgenti_lpc> app/src/main/resources

La pianta logica (dove si cammina, porte, oggetti) è in resources/maps/<id>.txt ed è la
stessa che legge l'app: qui viene solo "dipinta". Ogni sfondo ha un bordo di MARGIN
piastrelle oltre la mappa, così la telecamera non mostra mai il vuoto.

Sorgenti (CC-BY-SA 3.0 / GPL 3.0 / OGA-BY, vedi CREDITS-maps.txt), da OpenGameArt:
  lpc-tile-atlas (Atlas_0), lpc-tile-atlas2 (Atlas2), lpc-city-inside,
  lpc-house-interior-and-decorations, lpc-wooden-furniture (clean_furniture.png),
  flowers.png dalla pagina lpc-tile-atlas
"""
import random
import sys
from pathlib import Path

from PIL import Image, ImageDraw

T = 32
MARGIN = 6

SRC_FILES = {
    "terrain": "Atlas_0/terrain_atlas.png",
    "base": "Atlas_0/base_out_atlas.png",
    "build": "Atlas2/build_atlas.png",
    "city": "LPC_city_inside/LPC_city_inside/city_inside.png",
    "city_preview": "LPC_city_inside/LPC_city_inside/preview1.png",
    "house": "LPC_house_interior_0/LPC_house_interior/interior.png",
    "furniture": "clean_furniture.png",
    "flowers": "flowers.png",
}


class Sheets:
    def __init__(self, root: Path):
        self.im = {k: Image.open(root / v).convert("RGBA") for k, v in SRC_FILES.items()}

    def tile(self, sheet, col, row, w=1, h=1):
        return self.im[sheet].crop((col * T, row * T, (col + w) * T, (row + h) * T))

    def px(self, sheet, x, y, w, h):
        return self.im[sheet].crop((x, y, x + w, y + h))


def load_layout(path: Path):
    rows = [line.rstrip("\n") for line in path.read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.startswith("#")]
    return rows


class Canvas:
    def __init__(self, layout):
        self.layout = layout
        self.h = len(layout)
        self.w = len(layout[0])
        self.img = Image.new("RGBA", ((self.w + 2 * MARGIN) * T, (self.h + 2 * MARGIN) * T), (0, 0, 0, 255))

    def at(self, x, y, default="#"):
        if 0 <= y < self.h and 0 <= x < self.w:
            return self.layout[y][x]
        return default

    def put(self, sprite, x, y, anchor="top", dx=0, dy=0):
        """Disegna lo sprite sulla cella (x, y). anchor='bottom': il fondo dello sprite coincide
        col fondo della cella e lo sprite è centrato in orizzontale (oggetti alti o larghi)."""
        px = (x + MARGIN) * T + dx
        py = (y + MARGIN) * T + dy
        if anchor == "bottom":
            px += (T - sprite.width) // 2
            py += T - sprite.height
        self.img.alpha_composite(sprite, (px, py))

    def rect(self, x, y, color, box=(0, 0, T, T)):
        d = ImageDraw.Draw(self.img)
        ox, oy = (x + MARGIN) * T, (y + MARGIN) * T
        d.rectangle((ox + box[0], oy + box[1], ox + box[2] - 1, oy + box[3] - 1), fill=color)


# ---------------------------------------------------------------- Atrio

CARPET = (178, 58, 58, 255)
CARPET_DARK = (140, 40, 44, 255)
GOLD = (236, 164, 58, 255)
GOLD_LIGHT = (252, 214, 122, 255)
CAP = (74, 70, 88, 255)
CAP_EDGE = (150, 146, 166, 255)
CAP_DARK = (44, 40, 54, 255)

WALLS = set("tw")
WALL_FACE = set("wo123456DLN")
DOORS = set("123456D")


def is_carpet(ch):
    return ch in "cmSCd"


def draw_carpet(cv, x, y):
    cv.rect(x, y, CARPET)
    # Trama leggera
    d = ImageDraw.Draw(cv.img)
    ox, oy = (x + MARGIN) * T, (y + MARGIN) * T
    for i in range(0, T, 4):
        d.point((ox + i, oy + (i * 3) % T), fill=CARPET_DARK)
    sides = {
        "n": not is_carpet(cv.at(x, y - 1)), "s": not is_carpet(cv.at(x, y + 1)),
        "w": not is_carpet(cv.at(x - 1, y)), "e": not is_carpet(cv.at(x + 1, y)),
    }
    for side, edge in sides.items():
        if not edge:
            continue
        if side == "n":
            cv.rect(x, y, GOLD, (0, 0, T, 3)); cv.rect(x, y, GOLD_LIGHT, (0, 4, T, 5))
        if side == "s":
            cv.rect(x, y, GOLD, (0, T - 3, T, T)); cv.rect(x, y, GOLD_LIGHT, (0, T - 5, T, T - 4))
        if side == "w":
            cv.rect(x, y, GOLD, (0, 0, 3, T)); cv.rect(x, y, GOLD_LIGHT, (4, 0, 5, T))
        if side == "e":
            cv.rect(x, y, GOLD, (T - 3, 0, T, T)); cv.rect(x, y, GOLD_LIGHT, (T - 5, 0, T - 4, T))


def draw_cap(cv, x, y):
    """Sommità dei muri vista dall'alto: blocco scuro con bordo chiaro verso le stanze."""
    cv.rect(x, y, CAP)
    for (nx, ny, box) in ((x, y - 1, (0, 0, T, 2)), (x, y + 1, (0, T - 3, T, T)),
                          (x - 1, y, (0, 0, 2, T)), (x + 1, y, (T - 2, 0, T, T))):
        if cv.at(nx, ny) not in WALLS and cv.at(nx, ny) != "#":
            cv.rect(x, y, CAP_EDGE, box)
    cv.rect(x, y, CAP_DARK, (6, 6, T - 6, T - 6)) if False else None


def bake_hallway(sh: Sheets, layout):
    cv = Canvas(layout)
    floor = sh.px("city_preview", 62, 532, T, T)
    plaster_up = sh.px("city_preview", 62, 404, T, T)
    plaster_low = sh.px("city_preview", 62, 468, T, T)
    plaster_top = sh.px("city_preview", 62, 372, T, T)
    window = sh.px("city_preview", 128, 384, 30, 50)
    door = sh.tile("build", 5, 9, 1, 2)
    exit_door = sh.tile("build", 3, 9, 1, 2)
    plant = sh.tile("city", 10, 3, 1, 2)
    bookshelf = sh.tile("house", 1, 6, 1, 2)
    cabinet = sh.tile("house", 2, 6, 1, 2)
    locker = sh.tile("house", 0, 6, 1, 2)
    board = sh.tile("city", 15, 1)
    bench = sh.tile("furniture", 4, 8, 2, 1)
    table = sh.tile("furniture", 13, 7)
    clock = sh.tile("house", 1, 10)

    # Bordo esterno: sommità dei muri
    for y in range(-MARGIN, cv.h + MARGIN):
        for x in range(-MARGIN, cv.w + MARGIN):
            if not (0 <= x < cv.w and 0 <= y < cv.h):
                cv.rect(x, y, CAP_DARK)

    deferred = []
    for y in range(cv.h):
        for x in range(cv.w):
            ch = cv.at(x, y)
            if ch == "t" or (ch == "w" and y == cv.h - 1) or (ch == "D" and y == cv.h - 1 and False):
                draw_cap(cv, x, y)
                continue
            if ch in WALL_FACE and not (ch == "D" and y == cv.h - 1):
                above, below = cv.at(x, y - 1), cv.at(x, y + 1)
                if below not in WALL_FACE:
                    cv.put(plaster_low, x, y)
                elif above in WALLS or above == "#":
                    cv.put(plaster_top, x, y)
                else:
                    cv.put(plaster_up, x, y)
                if ch == "o":
                    deferred.append((0, y, lambda x=x, y=y: cv.put(window, x, y, dx=1, dy=-2)))
                elif ch in DOORS:
                    deferred.append((1, y, lambda x=x, y=y: cv.put(door, x, y, anchor="bottom")))
                elif ch == "L":
                    deferred.append((1, y, lambda x=x, y=y: cv.put(locker, x, y, anchor="bottom")))
                elif ch == "N":
                    deferred.append((1, y, lambda x=x, y=y: cv.put(board, x, y, dy=4)))
                continue
            # Pavimento o tappeto
            carpet_base = ch == "c" or (ch in "SCdmPn" and sum(is_carpet(cv.at(x + a, y + b)) for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))) >= 2)
            if carpet_base and ch != "m":
                draw_carpet(cv, x, y)
            else:
                cv.put(floor, x, y)
            if ch == "m":
                cv.rect(x, y, (120, 82, 54, 255), (4, 9, T - 4, T - 9))
                cv.rect(x, y, (160, 112, 70, 255), (6, 11, T - 6, T - 11))
            elif ch == "P":
                deferred.append((2, y, lambda x=x, y=y: cv.put(plant, x, y, anchor="bottom")))
            elif ch == "S":
                deferred.append((2, y, lambda x=x, y=y: cv.put(bookshelf, x, y, anchor="bottom")))
            elif ch == "C":
                deferred.append((2, y, lambda x=x, y=y: (cv.put(cabinet, x, y, anchor="bottom"), draw_trophy(cv, x, y - 1))))
            elif ch == "d":
                deferred.append((2, y, lambda x=x, y=y: cv.put(table, x, y)))
            elif ch == "n":
                deferred.append((2, y, lambda x=x, y=y: cv.put(bench, x, y, anchor="bottom")))
            elif ch == "D":
                pass

    # Uscita verso il cortile nel muro sud: varco con zerbino
    for x in range(cv.w):
        if cv.at(x, cv.h - 1) == "D":
            cv.put(floor, x, cv.h - 1)
            cv.rect(x, cv.h - 1, (120, 82, 54, 255), (3, 4, T - 3, T - 2))
            cv.rect(x, cv.h - 1, (160, 112, 70, 255), (5, 6, T - 5, T - 4))

    for _, _, fn in sorted(deferred, key=lambda d: (d[1], d[0])):
        fn()
    # Orologio a muro sopra l'uscita dell'armadietto
    return cv.img


def draw_trophy(cv, x, y):
    d = ImageDraw.Draw(cv.img)
    ox, oy = (x + MARGIN) * T + 10, (y + MARGIN) * T + 8
    gold, light, dark = (232, 178, 40, 255), (255, 228, 120, 255), (150, 100, 20, 255)
    d.rectangle((ox, oy, ox + 11, oy + 7), fill=gold)
    d.rectangle((ox + 2, oy + 1, ox + 4, oy + 5), fill=light)
    d.rectangle((ox - 2, oy + 1, ox - 1, oy + 4), fill=dark)
    d.rectangle((ox + 12, oy + 1, ox + 13, oy + 4), fill=dark)
    d.rectangle((ox + 4, oy + 8, ox + 7, oy + 11), fill=dark)
    d.rectangle((ox + 1, oy + 12, ox + 10, oy + 14), fill=gold)


# ---------------------------------------------------------------- Cortile

def make_tree(sh: Sheets, canopy_row):
    """Chioma 3x3 (tonda alla riga 12, abete alla riga 15) sopra il tronco con ombra."""
    canopy = sh.tile("base", 24, canopy_row, 3, 3)
    trunk = sh.tile("base", 25, 19, 3, 2)  # tronco al centro, ombra intera sui lati
    tree = Image.new("RGBA", (3 * T, 3 * T + 40))
    tree.alpha_composite(trunk, (0, tree.height - 2 * T))
    tree.alpha_composite(canopy, (0, 0))
    return tree


ROOF = (184, 76, 50, 255)
ROOF_DARK = (128, 44, 34, 255)
ROOF_LIGHT = (222, 120, 80, 255)


def draw_roof(cv, x, y):
    """Tetto a coppi rossi: file sfalsate di tegole, colmo scuro in alto e gronda in basso."""
    cv.rect(x, y, ROOF)
    for row in range(4):
        top = row * 8
        cv.rect(x, y, ROOF_DARK, (0, top + 7, T, top + 8))
        cv.rect(x, y, ROOF_LIGHT, (0, top + 1, T, top + 2))
        off = 0 if (row + y) % 2 == 0 else 4
        for sx in range(off, T, 8):
            cv.rect(x, y, ROOF_DARK, (sx, top + 1, sx + 1, top + 7))
    if cv.at(x, y - 1) != "R":
        cv.rect(x, y, (92, 34, 30, 255), (0, 0, T, 4))
    if cv.at(x, y + 1) != "R":
        cv.rect(x, y, (70, 40, 36, 255), (0, T - 3, T, T))


def bake_yard(sh: Sheets, layout):
    cv = Canvas(layout)
    rnd = random.Random(7)
    grass = [sh.tile("terrain", 22, 3)] * 5 + [sh.tile("terrain", c, 5) for c in (21, 22, 23)]
    paving = sh.tile("terrain", 2, 17)
    flowers = [sh.tile("flowers", c, r) for c, r in ((1, 0), (1, 2), (5, 1), (7, 1))]
    trees = [make_tree(sh, 12), make_tree(sh, 15)]
    hedge_top, hedge_front = sh.tile("terrain", 1, 22), sh.tile("terrain", 1, 24)
    brick = sh.tile("build", 1, 10)
    brick_top = sh.tile("build", 1, 9)
    brick_l, brick_r = sh.tile("build", 0, 10), sh.tile("build", 2, 10)
    window = sh.tile("build", 2, 0)
    door = sh.tile("build", 5, 9, 1, 2)
    bench = sh.tile("furniture", 4, 8, 2, 1)
    topiary = sh.tile("build", 7, 5, 1, 2)

    for y in range(-MARGIN, cv.h + MARGIN):
        for x in range(-MARGIN, cv.w + MARGIN):
            cv.put(rnd.choice(grass), x, y)

    for y in range(cv.h):
        for x in range(cv.w):
            ch = cv.at(x, y)
            if ch == "p":
                cv.put(paving, x, y)
                for (nx, ny, box) in ((x, y - 1, (0, 0, T, 2)), (x, y + 1, (0, T - 2, T, T)),
                                      (x - 1, y, (0, 0, 2, T)), (x + 1, y, (T - 2, 0, T, T))):
                    if cv.at(nx, ny) not in "pm":
                        cv.rect(x, y, (92, 98, 88, 255), box)
            elif ch == "f":
                cv.put(rnd.choice(flowers), x, y)
            elif ch == "m":
                cv.put(paving, x, y)
            elif ch == "R":
                draw_roof(cv, x, y)
            elif ch in "WOD":
                left, right = cv.at(x - 1, y) not in "WOD", cv.at(x + 1, y) not in "WOD"
                tile = brick_l if left else brick_r if right else (brick_top if cv.at(x, y - 1) == "R" else brick)
                cv.put(tile, x, y)
                if cv.at(x, y + 1) not in "WOD":
                    cv.rect(x, y, (96, 84, 80, 255), (0, T - 4, T, T))
    # Ombra del tetto sul muro
    for y in range(cv.h):
        for x in range(cv.w):
            if cv.at(x, y) in "WOD" and cv.at(x, y - 1) == "R":
                cv.rect(x, y, (40, 30, 30, 90), (0, 0, T, 5))

    sprites = []
    for y in range(cv.h):
        for x in range(cv.w):
            ch = cv.at(x, y)
            if ch == "O":
                sprites.append((y, lambda x=x, y=y: cv.put(window, x, y)))
            elif ch == "D":
                sprites.append((y, lambda x=x, y=y: cv.put(door, x, y, anchor="bottom")))
            elif ch == "B":
                sprites.append((y, lambda x=x, y=y: cv.put(bench, x, y, anchor="bottom")))
            elif ch == "=":
                sprites.append((y, lambda x=x, y=y: (cv.put(hedge_top, x, y - 1), cv.put(hedge_front, x, y))))
    # Alberi: bosco fitto sul bordo (anche oltre la mappa), disegnati dall'alto in basso
    spots = []
    for y in range(-MARGIN, cv.h + MARGIN):
        for x in range(-MARGIN, cv.w + MARGIN):
            inside = 0 <= x < cv.w and 0 <= y < cv.h
            if (inside and cv.at(x, y) == "T") or (not inside and (y < cv.h - 1 or y >= cv.h + 3)):
                if (x + 2 * y) % 2 == 0 or (inside and (x in (0, cv.w - 1))):
                    spots.append((y, x))
    for y, x in spots:
        tree = trees[(x * 7 + y * 3) % 5 == 0]
        sprites.append((y + 0.5, lambda x=x, y=y, tree=tree: cv.put(tree, x, y, anchor="bottom", dy=6)))
    for _, fn in sorted(sprites, key=lambda s: s[0]):
        fn()
    return cv.img


CREDIT_SOURCES = [
    ("LPC Tile Atlas", "https://opengameart.org/content/lpc-tile-atlas", "Atlas_0/Attribution.txt"),
    ("LPC Tile Atlas 2", "https://opengameart.org/content/lpc-tile-atlas2", "Atlas2/Attribution2.txt"),
    ("LPC City Inside", "https://opengameart.org/content/lpc-city-inside", "LPC_city_inside/LPC_city_inside/credits.txt"),
    ("LPC House Interior and Decorations", "https://opengameart.org/content/lpc-house-interior-and-decorations",
     "LPC_house_interior_0/LPC_house_interior/credits.txt"),
    ("LPC Wooden Furniture", "https://opengameart.org/content/lpc-wooden-furniture", "credits-furniture.txt_/CREDITS-furniture.txt"),
    ("LPC Flowers (flowers.png)", "https://opengameart.org/content/lpc-tile-atlas", None),
]


ROOM_W, ROOM_H = 30, 7  # piastrelle: 4 file di parete (cornice, intonaco x2, battiscopa) + 3 di pavimento


def bake_room(sh: Sheets, room_id: str) -> Image.Image:
    """Sfondo del "palco" in cima ai mini-giochi: parete dell'aula + pavimento, senza bordo extra.
    Lavagna, scaffale e altri elementi che cambiano durante il gioco li disegna l'app sopra."""
    img = Image.new("RGBA", (ROOM_W * T, ROOM_H * T))

    def put(sprite, x, y, bottom=False, dx=0, dy=0):
        px, py = x * T + dx, y * T + dy
        if bottom:
            px += (T - sprite.width) // 2
            py += T - sprite.height
        img.alpha_composite(sprite, (px, py))

    plaster_top = sh.px("city_preview", 62, 372, T, T)
    plaster_up = sh.px("city_preview", 62, 404, T, T)
    plaster_low = sh.px("city_preview", 62, 468, T, T)
    window = sh.px("city_preview", 128, 384, 30, 50)
    floors = {
        "english": sh.tile("house", 0, 3),
        "library": sh.tile("house", 0, 3),
        "lab": sh.px("city_preview", 62, 532, T, T),
    }
    for x in range(ROOM_W):
        put(plaster_top, x, 0)
        put(plaster_up, x, 1)
        put(plaster_up, x, 2)
        put(plaster_low, x, 3)
        for y in range(4, ROOM_H):
            put(floors[room_id], x, y)
    # Ombra della parete sul pavimento
    shade = Image.new("RGBA", (ROOM_W * T, 6), (30, 20, 20, 70))
    img.alpha_composite(shade, (0, 4 * T))

    plant = sh.tile("city", 10, 3, 1, 2)
    if room_id == "english":
        for x in (2, 6, 23, 27):
            put(window, x, 1, dx=1, dy=6)
        put(plant, 0, 4, bottom=True, dy=-8)
        put(plant, ROOM_W - 1, 4, bottom=True, dy=-8)
    elif room_id == "library":
        shelves = [sh.tile("house", 1, 6, 1, 2), sh.tile("house", 5, 6, 1, 2)]
        for x in range(ROOM_W):
            put(shelves[(x * 7) % 3 == 0], x, 3, bottom=True)
        rug = Image.new("RGBA", (ROOM_W * T, T), (120, 40, 60, 255))
        ImageDraw.Draw(rug).rectangle((0, 3, ROOM_W * T, T - 4), outline=(236, 164, 58, 255), width=2)
        img.alpha_composite(rug, (0, 5 * T))
    elif room_id == "lab":
        for x in (2, 6):
            put(window, x, 1, dx=1, dy=6)
        put(sh.tile("house", 2, 6, 1, 2), 10, 3, bottom=True)
        put(sh.tile("house", 0, 6, 1, 2), 11, 3, bottom=True)
        put(sh.tile("house", 2, 6, 1, 2), 18, 3, bottom=True)
        put(sh.tile("house", 1, 6, 1, 2), 19, 3, bottom=True)
        for x in (0, 14, ROOM_W - 1):
            put(plant, x, 4, bottom=True, dy=-8)
    return img


def write_credits(src: Path, out: Path):
    """Raccoglie le attribuzioni dei pacchetti usati: le licenze CC-BY-SA / GPL / OGA-BY le richiedono."""
    parts = ["Sfondi del Cortile e dell'Atrio composti da tools/art/build_maps.py con piastrelle LPC.",
             "Licenze dei pacchetti: CC-BY-SA 3.0 / GPL 3.0 (alcune parti anche CC-BY 3.0 / OGA-BY 3.0).",
             "Tetto a coppi, tappeti, sommità dei muri e trofeo disegnati via codice nello script.", ""]
    for title, url, rel in CREDIT_SOURCES:
        parts += ["=" * 70, f"{title} - {url}", "=" * 70]
        if rel:
            parts.append((src / rel).read_text(encoding="utf-8", errors="replace").strip())
        parts.append("")
    out.write_text("\n".join(parts), encoding="utf-8")


def main():
    src, res = Path(sys.argv[1]), Path(sys.argv[2])
    sh = Sheets(src)
    out = res / "sprites" / "maps"
    out.mkdir(parents=True, exist_ok=True)
    for map_id, baker in (("school_hallway", bake_hallway), ("school_yard", bake_yard)):
        layout = load_layout(res / "maps" / f"{map_id}.txt")
        baker(sh, layout).save(out / f"{map_id}.png", optimize=True)
        print(map_id, len(layout[0]), "x", len(layout))
    rooms = res / "sprites" / "rooms"
    rooms.mkdir(parents=True, exist_ok=True)
    for room_id in ("english", "library", "lab"):
        bake_room(sh, room_id).save(rooms / f"{room_id}.png", optimize=True)
        print("aula", room_id)
    write_credits(src, res / "sprites" / "CREDITS-maps.txt")


if __name__ == "__main__":
    main()
