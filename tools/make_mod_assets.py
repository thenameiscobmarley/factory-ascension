#!/usr/bin/env python3
"""Textures, models, block states, loot tables and tags for the Factory Ascension mod (the Full download's jar).

Blocks match the pack's gear art: gunmetal plates, gold trim, glowing teal channels, violet gems.
Run from the repo root:  python3 tools/make_mod_assets.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import gear_art  # noqa: E402
from gear_art import _shade, _mix  # noqa: E402
import make_textures  # noqa: E402

RES = os.path.join(os.path.dirname(__file__), "..", "mod", "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", "factoryascension")
DATA = os.path.join(RES, "data", "factoryascension")
S = 32
BLOCKS = ["ascension_vault", "card_binder", "essence_forge", "pack_opener", "salvager", "tome_fusion_table", "super_smelter",
          "engine_core", "engine_coil", "engine_gearbox", "engine_burner", "storage_terminal", "ascension_workbench", "mini_crusher"]
gear_art.MATERIALS.setdefault("copper", ((255, 196, 150), (200, 110, 62), (96, 44, 24)))


def canvas():
    return [[(0, 0, 0, 255)] * S for _ in range(S)]


def plate(px, x0, y0, w, h, mat="steel"):
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            t = 0.3 + 0.35 * ((x - x0) / max(1, w) + (y - y0) / max(1, h)) / 2
            if x == x0 or y == y0:
                t -= 0.18
            if x == x0 + w - 1 or y == y0 + h - 1:
                t += 0.2
            px[y][x] = _shade(mat, t) + (255,)


def frame(px, mat="gold", inset=0):
    for i in range(inset, S - inset):
        for j in (inset, S - 1 - inset):
            px[j][i] = _shade(mat, 0.2 if j == inset else 0.6) + (255,)
            px[i][j] = _shade(mat, 0.3 if j == inset else 0.6) + (255,)


def rivets(px):
    for x, y in ((4, 4), (27, 4), (4, 27), (27, 27)):
        px[y][x] = _shade("gold", 0.15) + (255,)
        px[y + 1][x + 1] = _shade("gold", 0.7) + (255,)


def line(px, x0, y0, x1, y1, mat="teal"):
    n = max(abs(x1 - x0), abs(y1 - y0), 1)
    for k in range(n + 1):
        x, y = round(x0 + (x1 - x0) * k / n), round(y0 + (y1 - y0) * k / n)
        px[y][x] = _shade(mat, 0.3) + (255,)


def gem(px, cx, cy, r=2):
    for y in range(cy - r - 1, cy + r + 1):
        for x in range(cx - r - 1, cx + r + 1):
            if abs(x - cx + 0.5) + abs(y - cy + 0.5) <= r + 1.2:
                px[y][x] = _shade("gold", 0.55) + (255,)
    for y in range(cy - r, cy + r):
        for x in range(cx - r, cx + r):
            if abs(x - cx + 0.5) + abs(y - cy + 0.5) <= r + 0.2:
                px[y][x] = _shade("gem", 0.15 if y < cy else 0.6) + (255,)
    px[cy - 1][cx - 1] = (255, 255, 255, 255)


def side():
    px = canvas()
    plate(px, 0, 0, S, S)
    frame(px)
    rivets(px)
    line(px, 6, 16, 25, 16)
    return px


def top():
    px = canvas()
    plate(px, 0, 0, S, S, "steel_dk")
    frame(px)
    plate(px, 6, 6, 20, 20)
    gem(px, 16, 16, 3)
    return px


def vault_front():
    px = side()
    plate(px, 5, 5, 22, 22, "steel_dk")            # door
    frame(px, "gold", 5)
    for y in range(9, 24):                           # door seam
        px[y][16] = _shade("steel_dk", 0.85) + (255,)
    for x in (10, 22):                               # hinges
        for y in (8, 23):
            px[y][x] = _shade("gold", 0.3) + (255,)
    gem(px, 16, 15, 3)                               # glowing lock
    line(px, 12, 20, 20, 20)
    return px


def binder_top():
    px = canvas()
    plate(px, 0, 0, S, S, "wood")
    frame(px)
    # An open binder: two pages of card pockets
    for page in (3, 17):
        plate(px, page, 5, 12, 22, "cloth")
        for cy in (7, 15):
            for cx in (page + 1, page + 6):
                for y in range(cy, cy + 6):
                    for x in range(cx, cx + 4):
                        px[y][x] = _mix(_shade("gem", 0.4), (255, 255, 255), 0.25) + (255,) if (x + y) % 5 else (255, 255, 255, 255)
    for y in range(5, 27):
        px[y][16] = _shade("gold", 0.5) + (255,)
    return px


def binder_side():
    px = canvas()
    plate(px, 0, 0, S, S, "wood")
    frame(px)
    plate(px, 4, 20, 24, 8, "steel")
    line(px, 6, 24, 25, 24, "gem")
    return px


def forge_front():
    px = side()
    plate(px, 7, 6, 18, 20, "steel_dk")
    frame(px, "gold", 6)
    for y in range(9, 23):                           # glowing core
        for x in range(10, 22):
            d = abs(x - 15.5) + abs(y - 16)
            if d < 7:
                px[y][x] = _shade("teal", 0.1 + d / 10) + (255,)
    gem(px, 16, 16, 2)
    return px


def forge_top():
    px = top()
    for y in range(10, 22):                          # an anvil-like working surface with a channel
        px[y][15] = _shade("teal", 0.2) + (255,)
        px[y][16] = _shade("teal", 0.4) + (255,)
    return px


def opener_front():
    px = side()
    plate(px, 6, 6, 20, 8, "steel_dk")               # pack slot
    for x in range(9, 23):
        px[10][x] = (10, 10, 14, 255)
    for i, cx in enumerate((8, 13, 18, 23)):         # cards fanning out
        for y in range(17, 26):
            for x in range(cx - 2, cx + 2):
                px[y][x] = (_shade("gold", 0.3) if i % 2 else _shade("gem", 0.3)) + (255,)
        px[18][cx - 1] = (255, 255, 255, 255)
    return px


def shaft_socket(px, cx=16, cy=16):
    """A round Create shaft hole with an andesite collar: where the rotation goes in or out."""
    for y in range(S):
        for x in range(S):
            d = ((x - cx + 0.5) ** 2 + (y - cy + 0.5) ** 2) ** 0.5
            if d < 8:
                px[y][x] = _shade("iron", 0.25 + d / 20) + (255,)
            if d < 5.5:
                px[y][x] = _shade("steel_dk", 0.75) + (255,)
            if d < 3.2:
                px[y][x] = _shade("wood", 0.35 + (x - cx) / 12) + (255,)
    for k in (-2, 2):                                # the shaft's square key
        px[cy][cx + k] = _shade("steel_dk", 0.9) + (255,)


def salvager_front():
    px = side()
    plate(px, 5, 6, 22, 18, "steel_dk")              # grinder mouth
    frame(px, "gold", 5)
    for i, x in enumerate(range(8, 25, 4)):          # two rows of teeth
        for y in range(9, 13):
            px[y][x + (y - 9) // 2] = _shade("iron", 0.15 + 0.1 * (i % 2)) + (255,)
        for y in range(17, 21):
            px[y][x + 2 - (y - 17) // 2] = _shade("iron", 0.2) + (255,)
    for x in range(8, 25):                           # glowing seam where the loot goes
        px[14][x] = _shade("teal", 0.2) + (255,)
        px[15][x] = _shade("teal", 0.45) + (255,)
    plate(px, 9, 25, 14, 3, "gold")                  # core chute
    return px


def salvager_top():
    px = top()
    plate(px, 7, 7, 18, 18, "steel_dk")              # hopper opening
    for y in range(10, 22):
        for x in range(10, 22):
            d = max(abs(x - 15.5), abs(y - 15.5))
            px[y][x] = _shade("steel_dk", 0.5 + d / 14) + (255,)
    for k in range(10, 22, 3):
        px[15][k] = _shade("iron", 0.2) + (255,)
    return px


def fusion_top():
    px = canvas()
    plate(px, 0, 0, S, S, "wood")
    frame(px)
    for page, flip in ((4, 0), (17, 1)):             # an open tome
        plate(px, page, 6, 11, 20, "cloth")
        for y in range(9, 24, 3):                    # glowing rune lines
            for x in range(page + 2, page + 9):
                if (x * 7 + y * 3 + flip) % 5:
                    px[y][x] = _mix(_shade("gem", 0.3), _shade("teal", 0.3), (x - page) / 9) + (255,)
    for y in range(6, 26):
        px[y][15] = _shade("gold", 0.6) + (255,)
        px[y][16] = _shade("gold", 0.3) + (255,)
    gem(px, 16, 4, 1)
    return px


def fusion_side():
    px = canvas()
    plate(px, 0, 0, S, S, "wood")
    frame(px)
    rivets(px)
    for i, x0 in enumerate((5, 12, 19)):             # three tome spines merging into one glow
        col = ("red", "teal", "gem")[i]
        plate(px, x0, 7, 6, 13, col)
        for y in (9, 17):
            for x in range(x0, x0 + 6):
                px[y][x] = _shade("gold", 0.4) + (255,)
    line(px, 8, 22, 16, 26, "gem")
    line(px, 15, 22, 16, 26, "gem")
    line(px, 22, 22, 16, 26, "gem")
    gem(px, 16, 26, 1)
    return px


def smelter_front(lit=False):
    px = side()
    plate(px, 5, 6, 22, 20, "steel_dk")
    frame(px, "gold", 5)
    for y in range(10, 23):                          # a wide furnace mouth (it smelts in rows)
        for x in range(8, 24):
            if lit:
                heat = (y - 10) / 12
                px[y][x] = _mix(_shade("fire", 0.1 + 0.6 * (1 - heat)), (255, 240, 160), 0.25 if (x + y) % 4 == 0 else 0) + (255,)
            else:
                px[y][x] = _shade("steel_dk", 0.8 + 0.1 * ((x + y) % 2)) + (255,)
    for x in range(8, 24, 4):                        # grate bars
        for y in range(10, 23):
            px[y][x] = _shade("iron", 0.35) + (255,)
    for x in range(8, 24):
        px[16][x] = _shade("gold", 0.5) + (255,)     # the lane divider
    return px


def smelter_back():
    px = side()
    shaft_socket(px)
    return px


def smelter_top():
    px = top()
    for x in range(8, 25, 4):                        # vents
        for y in range(7, 25):
            px[y][x] = _shade("steel_dk", 0.85) + (255,)
    return px


def engine_front():
    px = canvas()
    plate(px, 0, 0, S, S, "iron")
    frame(px, "copper")
    rivets(px)
    shaft_socket(px)
    return px


def engine_side():
    px = canvas()
    plate(px, 0, 0, S, S, "iron")
    frame(px, "copper")
    rivets(px)
    for y in range(8, 25, 4):                        # cooling fins
        for x in range(6, 26):
            px[y][x] = _shade("steel_dk", 0.6) + (255,)
            px[y + 1][x] = _shade("iron", 0.15) + (255,)
    line(px, 16, 6, 16, 26, "teal")
    return px


def engine_top():
    px = canvas()
    plate(px, 0, 0, S, S, "iron")
    frame(px, "copper")
    plate(px, 9, 9, 14, 14, "steel_dk")
    gem(px, 16, 16, 2)
    return px


def coil_side():
    px = canvas()
    plate(px, 0, 0, S, S, "steel_dk")
    for y in range(3, 29):                           # copper windings
        for x in range(4, 28):
            t = 0.15 + 0.6 * (y % 3) / 2 + 0.1 * ((x - 4) / 24)
            px[y][x] = _shade("copper", t) + (255,)
    for x in range(4, 28):
        px[15][x] = _shade("teal", 0.25) + (255,)    # the charged band
        px[16][x] = _shade("teal", 0.5) + (255,)
    frame(px, "iron")
    return px


def gearbox_side():
    px = canvas()
    plate(px, 0, 0, S, S, "iron")
    frame(px, "copper")
    for y in range(S):                               # a big gear with teeth
        for x in range(S):
            dx, dy = x - 15.5, y - 15.5
            d = (dx * dx + dy * dy) ** 0.5
            import math
            a = math.atan2(dy, dx)
            r = 10 + (1.6 if math.cos(a * 10) > 0.2 else 0)
            if d < r:
                px[y][x] = _shade("gold", 0.25 + d / 25) + (255,)
            if d < 5:
                px[y][x] = _shade("steel_dk", 0.6) + (255,)
            if d < 2.5:
                px[y][x] = _shade("teal", 0.3) + (255,)
    rivets(px)
    return px


def burner_side(lit=False):
    px = canvas()
    plate(px, 0, 0, S, S, "iron")
    frame(px, "copper")
    plate(px, 6, 10, 20, 15, "steel_dk")
    for y in range(13, 23):
        for x in range(8, 24):
            if lit:
                px[y][x] = _shade("fire", 0.05 + 0.08 * abs(x - 15.5) + 0.03 * (22 - y)) + (255,)
            else:
                px[y][x] = _shade("steel_dk", 0.85) + (255,)
    for x in range(8, 24, 3):                        # grate
        for y in range(13, 23):
            px[y][x] = _shade("iron", 0.4) + (255,)
    for x in range(6, 26):
        px[6][x] = _shade("copper", 0.3) + (255,)
        px[7][x] = _shade("copper", 0.6) + (255,)
    return px


def burner_top():
    px = canvas()
    plate(px, 0, 0, S, S, "iron")
    frame(px, "copper")
    plate(px, 8, 8, 16, 16, "steel_dk")
    for k in range(10, 22, 3):
        for j in range(10, 22):
            px[j][k] = _shade("iron", 0.3) + (255,)
    return px


def terminal_front():
    """A screen showing a grid of stored items, with a teal glow."""
    px = side()
    plate(px, 4, 4, 24, 20, "steel_dk")
    frame(px, "gold", 4)
    for y in range(6, 22):
        for x in range(6, 26):
            px[y][x] = _shade("teal", 0.85 + 0.05 * ((x + y) % 2)) + (255,)
    colors = ("gold", "gem", "teal", "iron", "copper", "gold", "gem", "iron")
    for i in range(8):                                # stored items in the grid
        cx, cy = 8 + (i % 4) * 5, 8 + (i // 4) * 6
        for y in range(cy, cy + 3):
            for x in range(cx, cx + 3):
                px[y][x] = _shade(colors[i], 0.25 + 0.15 * (y - cy)) + (255,)
    for x in range(6, 26):                            # search bar
        px[20][x] = _shade("teal", 0.3) + (255,)
    plate(px, 9, 25, 14, 3, "gold")                   # keypad strip
    for x in (11, 15, 19):
        px[26][x] = _shade("teal", 0.2) + (255,)
    return px


def terminal_top():
    px = top()
    for x in range(6, 26):                            # link cables out to the chests
        px[16][x] = _shade("teal", 0.3) + (255,)
        px[x][16] = _shade("teal", 0.3) + (255,)
    gem(px, 16, 16, 3)
    return px


def linker_item():
    """Storage Linker: a gold-banded wand with a teal crystal tip."""
    px = [[(0, 0, 0, 0)] * S for _ in range(S)]
    for k in range(4, 22):                            # handle, bottom-left to top-right
        for w in range(-1, 2):
            x, y = k + w, 31 - k
            if 0 <= x < S and 0 <= y < S:
                px[y][x] = _shade("steel_dk" if w else "steel", 0.3 + 0.02 * k) + (255,)
    for k in (8, 13, 18):                             # gold bands
        for w in range(-2, 3):
            x, y = k + w, 31 - k
            if 0 <= x < S and 0 <= y < S:
                px[y][x] = _shade("gold", 0.35) + (255,)
    for y in range(2, 13):                            # crystal tip
        for x in range(19, 30):
            if abs(x - 24.5) + abs(y - 7.5) <= 5:
                px[y][x] = _shade("teal", 0.15 + 0.08 * (y - 2)) + (255,)
    for x, y in ((22, 5), (23, 5), (22, 6)):
        px[y][x] = (255, 255, 255, 255)
    return px


def bench_top():
    """The workbench top: dark wood with a gold-ringed teal rune circle (where gear is enchanted)."""
    px = canvas()
    plate(px, 0, 0, S, S, "wood")
    for y in range(3, S, 7):
        for x in range(S):
            px[y][x] = _shade("wood", 0.78) + (255,)
    import math
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - 15.5, y - 15.5)
            if 10 <= d < 11.5:
                px[y][x] = _shade("gold", 0.3) + (255,)
            elif 6.5 <= d < 7.5 or (d < 10 and abs((math.degrees(math.atan2(y - 15.5, x - 15.5)) % 72) - 36) < 3):
                px[y][x] = _shade("teal", 0.3) + (255,)
    gem(px, 16, 16, 2)
    frame(px)
    return px


def bench_front():
    """The workbench front: two drawers with gold pulls."""
    px = canvas()
    plate(px, 0, 0, S, S, "wood")
    frame(px)
    for y0 in (6, 18):
        plate(px, 4, y0, 24, 9, "steel_dk")
        frame_rect = [(x, y0) for x in range(4, 28)]
        for x in range(13, 19):
            px[y0 + 4][x] = _shade("gold", 0.3) + (255,)
    return px


def bench_side():
    px = canvas()
    plate(px, 0, 0, S, S, "wood")
    frame(px)
    rivets(px)
    return px


def crusher_front():
    """Mini Crusher front: a window onto two small toothed crushing wheels."""
    import math
    px = side()
    plate(px, 5, 6, 22, 16, "steel_dk")
    frame(px, "gold", 5)
    for cx in (11.5, 20.5):
        for y in range(7, 21):
            for x in range(6, 26):
                d = math.hypot(x - cx, y - 13.5)
                a = math.atan2(y - 13.5, x - cx)
                if d < 4.2 + (1.0 if math.cos(a * 8) > 0.3 else 0):
                    px[y][x] = _shade("iron", 0.2 + d / 10) + (255,)
                if d < 1.6:
                    px[y][x] = _shade("steel_dk", 0.7) + (255,)
    plate(px, 9, 24, 14, 4, "steel_dk")              # output chute
    return px


def crusher_top():
    """A small hopper mouth."""
    px = top()
    plate(px, 8, 8, 16, 16, "steel_dk")
    for k in range(10, 22, 3):
        for j in range(10, 22):
            px[j][k] = _shade("iron", 0.35) + (255,)
    return px


def crusher_motor():
    """The motor housing on the side: copper windings behind a grille, a teal power light."""
    px = side()
    plate(px, 6, 6, 20, 20, "copper")
    for y in range(8, 24, 3):
        for x in range(8, 24):
            px[y][x] = _shade("steel_dk", 0.6) + (255,)
    gem(px, 22, 9, 1)
    return px


def opener_pack():
    import item_models
    return item_models.pack_face()


def terminal_keys():
    px = canvas()
    plate(px, 0, 0, S, S, "steel_dk")
    for y in range(4, 28, 6):
        for x in range(3, 29, 5):
            plate(px, x, y, 4, 4, "teal" if (x + y) % 3 == 0 else "steel")
    return px


FACES = {
    "ascension_vault": {"front": vault_front, "side": side, "top": top},
    "card_binder": {"front": binder_side, "side": binder_side, "top": binder_top},
    "essence_forge": {"front": forge_front, "side": side, "top": forge_top},
    "pack_opener": {"front": opener_front, "side": side, "top": top, "pack": opener_pack},
    "salvager": {"front": salvager_front, "side": side, "top": salvager_top},
    "tome_fusion_table": {"front": fusion_side, "side": fusion_side, "top": fusion_top},
    "super_smelter": {"front": smelter_front, "front_lit": lambda: smelter_front(True), "back": smelter_back, "side": side, "top": smelter_top},
    "engine_core": {"front": engine_front, "side": engine_side, "top": engine_top},
    "engine_coil": {"side": coil_side, "top": engine_top},
    "engine_gearbox": {"side": gearbox_side, "top": engine_top},
    "engine_burner": {"side": burner_side, "side_lit": lambda: burner_side(True), "top": burner_top},
    "ascension_workbench": {"front": bench_front, "side": bench_side, "top": bench_top},
    "mini_crusher": {"front": crusher_front, "side": crusher_motor, "top": crusher_top},
    "storage_terminal": {"front": terminal_front, "side": side, "top": terminal_top, "keys": terminal_keys},
}
NAMES = {"ascension_vault": "Ascension Vault", "card_binder": "Card Binder", "essence_forge": "Essence Forge", "pack_opener": "Pack Opener",
         "salvager": "Salvager", "tome_fusion_table": "Tome Fusion Table", "super_smelter": "Super Smelter",
         "engine_core": "Modular Engine Core", "engine_coil": "Engine Coil Module", "engine_gearbox": "Engine Gearbox Module",
         "engine_burner": "Engine Burner Module", "storage_terminal": "Universal Storage Terminal", "ascension_workbench": "Ascension Workbench", "mini_crusher": "Mini Crusher"}
FACINGS = (("north", 0), ("east", 90), ("south", 180), ("west", 270))


def cube(north, south, side, up, down=None):
    t = lambda n: "factoryascension:block/" + n
    return {"parent": "minecraft:block/cube", "textures": {"particle": t(side), "north": t(north), "south": t(south), "east": t(side),
                                                           "west": t(side), "up": t(up), "down": t(down or up)}}


def shaped_model(b, variant=""):
    """A block's 3D model: its parts, its own face textures and the shared materials."""
    t = "factoryascension:block/"
    tex = {k: t + b + "_" + k for k in FACES[b]}
    if variant:
        for k in list(tex):
            if k + variant in FACES[b]:
                tex[k + variant] = t + b + "_" + k + variant
    tex.update({"mat_" + m: t + "mat_" + m for m in MATERIALS})
    tex["particle"] = tex.get("side", tex.get("top"))
    m = {"parent": "minecraft:block/block", "textures": tex, "elements": shape(b, variant)}
    if any(e["faces"].get(f, {}).get("texture") == "#mat_glass" for e in m["elements"] for f in e["faces"]):
        m["render_type"] = "minecraft:translucent"
    return m


def rod_models(b, m):
    """An engine module: its segment along the rod plus a flange on each open end, as a multipart blockstate
    (axis x as drawn, z turned 90; the cap is drawn for the -x end and turned 180 for +x). The item shows both caps."""
    t = "factoryascension:block/"
    cap = shaped_model("engine_cap") if False else None
    tex = {"mat_" + k: t + "mat_" + k for k in MATERIALS}
    tex["particle"] = t + "mat_copper"
    m["engine_cap"] = {"parent": "minecraft:block/block", "textures": tex, "elements": shape("engine_cap")}
    lit = b == "engine_burner"
    parts = []
    for axis, rot in (("x", 0), ("z", 90)):
        for l in (("false", "true") if lit else (None,)):
            when = {"axis": axis}
            if l:
                when["lit"] = l
            parts.append({"when": when, "apply": {"model": t + b + ("_lit" if l == "true" else ""), **({"y": rot} if rot else {})}})
        parts.append({"when": {"axis": axis, "neg": "false"}, "apply": {"model": t + "engine_cap", **({"y": rot} if rot else {})}})
        parts.append({"when": {"axis": axis, "pos": "false"}, "apply": {"model": t + "engine_cap", "y": rot + 180}})
    # The item: the segment with both caps
    item = dict(m[b])
    item["elements"] = m[b]["elements"] + shape("engine_cap") + [rot180(e) for e in shape("engine_cap")]
    item["textures"] = dict(m[b]["textures"])
    m[b + "_item"] = item
    return m, {"multipart": parts}


def rot180(e):
    """A part turned 180 degrees about the block's vertical center line (x -> 16 - x, z -> 16 - z)."""
    f, t = e["from"], e["to"]
    swap = {"north": "south", "south": "north", "east": "west", "west": "east"}
    return {"from": [16 - t[0], f[1], 16 - t[2]], "to": [16 - f[0], t[1], 16 - f[2]], "faces": {swap.get(k, k): v for k, v in e["faces"].items()}}


def models(b):
    """{model name: model json} for a block, and its blockstate variants."""
    t = "factoryascension:block/"
    if shape(b) is not None:
        lit = {"super_smelter": "_lit", "engine_burner": "_lit"}.get(b)
        m = {b: shaped_model(b)}
        if lit:
            m[b + lit] = shaped_model(b, lit)
        if b == "super_smelter":
            return m, {"facing=%s,lit=%s" % (d, l): {"model": t + b + ("_lit" if l == "true" else ""), **({"y": r} if r else {})}
                       for d, r in FACINGS for l in ("false", "true")}
        if b in ("engine_coil", "engine_gearbox", "engine_burner"):
            return rod_models(b, m)
        if b == "ascension_workbench":
            m[b + "_side"] = shaped_model(b, "_side")
            return m, {"facing=%s,main=%s" % (d, mn): {"model": t + b + ("" if mn == "true" else "_side"), **({"y": r} if r else {})}
                       for d, r in FACINGS for mn in ("true", "false")}
        return m, {"facing=%s" % d: {"model": t + b, **({"y": r} if r else {})} for d, r in FACINGS}
    if b == "super_smelter":
        m = {b: cube(b + "_front", b + "_back", b + "_side", b + "_top"), b + "_lit": cube(b + "_front_lit", b + "_back", b + "_side", b + "_top")}
        v = {"facing=%s,lit=%s" % (d, lit): {"model": t + b + ("_lit" if lit == "true" else ""), **({"y": r} if r else {})}
             for d, r in FACINGS for lit in ("false", "true")}
        return m, v
    if b == "engine_core":
        return {b: cube(b + "_front", b + "_side", b + "_side", b + "_top")}, \
            {"facing=%s" % d: {"model": t + b, **({"y": r} if r else {})} for d, r in FACINGS}
    if b in ("engine_coil", "engine_gearbox"):
        return {b: {"parent": "minecraft:block/cube_column", "textures": {"side": t + b + "_side", "end": t + b + "_top"}}}, {"": {"model": t + b}}
    if b == "engine_burner":
        col = lambda sd: {"parent": "minecraft:block/cube_column", "textures": {"side": t + sd, "end": t + b + "_top"}}
        return {b: col(b + "_side"), b + "_lit": col(b + "_side_lit")}, {"lit=false": {"model": t + b}, "lit=true": {"model": t + b + "_lit"}}
    return {b: {"parent": "minecraft:block/orientable", "textures": {"front": t + b + "_front", "side": t + b + "_side", "top": t + b + "_top"}}}, \
        {"facing=%s" % d: {"model": t + b, **({"y": r} if r else {})} for d, r in FACINGS}


# ---- 3D block models: every machine is built from parts (boxes), not a plain cube ----
# Texture keys: the block's own faces (front, side, top, back, ...) and shared materials (mat_<name>).
MATERIALS = ["steel", "steel_dk", "gold", "teal", "wood", "copper", "iron", "gem", "cloth", "fire", "glass"]


def material(mat):
    px = canvas()
    if mat == "glass":
        for y in range(S):
            for x in range(S):
                edge = x in (0, S - 1) or y in (0, S - 1)
                px[y][x] = (_shade("teal", 0.1) + (200,)) if edge else ((200, 236, 245, 70) if (x - y) % 13 > 1 else (255, 255, 255, 150))
        return px
    plate(px, 0, 0, S, S, mat)
    if mat == "wood":
        for y in range(3, S, 6):
            for x in range(S):
                px[y][x] = _shade("wood", 0.75) + (255,)
    return px


FACE_KEYS = {"n": "north", "s": "south", "e": "east", "w": "west", "u": "up", "d": "down"}


def E(frm, to, tex, rot=None):
    """One part. tex: a texture key for every face, or {face letter or '*': key}. UVs follow the part's position, so a
    part shows the matching piece of its texture (a door panel shows the middle of the front)."""
    faces = {}
    for k, name in FACE_KEYS.items():
        key = tex if isinstance(tex, str) else tex.get(k, tex.get("*"))
        if key:
            faces[name] = {"texture": "#" + key}
    el = {"from": list(frm), "to": list(to), "faces": faces}
    if rot:
        el["rotation"] = {"origin": rot[2] if len(rot) > 2 else [8, 8, 8], "axis": rot[0], "angle": rot[1]}
    return el


def corners(x0, x1, y0, y1, w, tex):
    """Four posts at the corners of a w-wide square frame."""
    return [E((x, y0, z), (x + w, y1, z + w), tex) for x in (x0, x1 - w) for z in (x0, x1 - w)]


def shape(b, variant=""):
    """The parts of block b (variant: "_lit" for the lit look). Front faces north."""
    F = {"*": "side", "n": "front", "u": "top", "d": "mat_steel_dk"}
    if b == "ascension_vault":
        return [E((0, 0, 0), (16, 1, 16), "mat_steel_dk"), E((1, 1, 1), (15, 15, 15), F),
                E((0.5, 15, 0.5), (15.5, 16, 15.5), {"*": "mat_gold", "u": "top"}),
                E((3, 3, 0.4), (13, 13, 1), {"*": "mat_steel", "n": "front"}),
                E((7, 7, -0.1), (9, 9, 0.4), "mat_gold"),
                E((12.6, 4, 0.2), (13.6, 6, 1), "mat_gold"), E((12.6, 10, 0.2), (13.6, 12, 1), "mat_gold")] + \
            [E((x, 1, 0.5), (x + 1.5, 15, 2), "mat_gold") for x in (0.5, 14)]
    if b == "card_binder":
        return [E((3, 0, 3), (13, 2, 13), "mat_wood"), E((6, 2, 6), (10, 9, 10), {"*": "side", "u": "mat_wood"}),
                E((1, 9, 1), (15, 11, 15), {"*": "mat_wood", "u": "top"}, ("x", -22.5, [8, 9, 8])),
                E((7.5, 11, 1.5), (8.5, 11.6, 14.5), "mat_gold", ("x", -22.5, [8, 9, 8]))]
    if b == "essence_forge":
        return [E((1, 0, 1), (15, 3, 15), {"*": "side", "u": "mat_steel_dk"}), E((3, 3, 3), (13, 9, 13), "mat_glass"),
                E((4, 3, 4), (12, 8, 12), "mat_gem"),
                E((0, 9, 2), (16, 14, 14), {"*": "side", "n": "front", "u": "top", "d": "mat_steel_dk"}),
                E((2, 14, 5), (14, 15, 11), "mat_steel_dk")] + corners(1, 15, 3, 9, 1.5, "mat_gold")
    if b == "pack_opener":
        return [E((1, 0, 1), (15, 11, 15), {"*": "side", "n": "front", "u": "top", "d": "mat_steel_dk"}),
                E((2, 11, 2), (14, 13, 14), "mat_steel_dk"), E((4, 13, 6.5), (12, 14, 9.5), "mat_gold"),
                E((5, 13, 7.25), (11, 18, 8.75), {"*": "mat_gem", "n": "pack", "s": "pack"})]
    if b == "salvager":
        return [E((1, 0, 1), (15, 10, 15), {"*": "side", "n": "front", "d": "mat_steel_dk", "u": "top"}),
                E((0, 10, 0), (16, 15, 2), "mat_steel"), E((0, 10, 14), (16, 15, 16), "mat_steel"),
                E((0, 10, 2), (2, 15, 14), "mat_steel"), E((14, 10, 2), (16, 15, 14), "mat_steel"),
                E((0, 14.5, 0), (16, 15.5, 1), "mat_gold"), E((6, 10, -0.5), (10, 12, 0.5), "mat_gold")]
    if b == "tome_fusion_table":
        return [E((0, 9, 0), (16, 12, 16), {"*": "side", "u": "top", "d": "mat_wood"}),
                E((1, 6, 1), (15, 9, 15), "mat_wood")] + corners(1, 15, 0, 9, 2, "mat_wood") + \
            [E((2, 12, 11), (6, 13.5, 14), "mat_cloth"), E((10, 12, 11), (14, 13.5, 14), "mat_teal"),
             E((7.5, 12, 1.5), (8.5, 14, 2.5), "mat_gem")]
    if b == "super_smelter":
        front = "front" + variant
        return [E((0, 0, 1), (16, 14, 15), {"*": "side", "n": front, "s": "back", "u": "top", "d": "mat_steel_dk"}),
                E((2, 12, 0.2), (14, 13.5, 1), "mat_steel_dk"), E((10, 14, 10), (14, 16, 14), "mat_steel_dk"),
                E((9.5, 15.5, 9.5), (14.5, 16.5, 14.5), "mat_gold"), E((0, 0, 0), (2, 3, 1), "mat_gold"), E((14, 0, 0), (16, 3, 1), "mat_gold")]
    if b == "engine_core":
        # Shaft out of the front; the engine rod couples on at the back
        return [E((1, 1, 1), (15, 15, 15), {"*": "side", "n": "front", "u": "top", "d": "top"}),
                E((3, 3, -0.5), (13, 13, 1), {"*": "mat_copper", "n": "front"}), E((3, 3, 15), (13, 13, 16), "mat_copper"),
                E((5, 5, 15.5), (11, 11, 16.01), "mat_steel_dk")] + corners(0, 16, 0, 16, 2, "mat_copper")
    # Engine modules: one segment of the rod, running along X (the blockstate turns it for Z). Caps are separate parts.
    if b == "engine_coil":
        return [E((0, 5, 5), (16, 11, 11), "mat_steel_dk")] + \
            [E((x0, 3, 3), (x0 + 2.6, 13, 13), {"*": "mat_copper", "e": "side", "w": "side"}) for x0 in (1.2, 4.9, 8.6, 12.3)] + \
            [E((0, 13, 7.4), (16, 13.6, 8.6), "mat_teal")]
    if b == "engine_gearbox":
        return [E((0, 4, 4), (16, 12, 12), "mat_steel")] + \
            [E((x0, 1.5, 1.5), (x0 + 2.5, 14.5, 14.5), {"*": "mat_gold", "e": "side", "w": "side"}) for x0 in (2.5, 11)] + \
            [E((6, 3, 3), (10, 13, 13), {"*": "mat_steel_dk", "e": "side", "w": "side"})]
    if b == "engine_burner":
        return [E((0, 5, 5), (16, 11, 11), "mat_steel_dk"), E((2, 0, 1.5), (14, 12, 14.5), {"*": "side" + variant, "u": "top", "d": "mat_steel_dk"}),
                E((9, 12, 6), (12, 16, 9), "mat_steel_dk"), E((8.5, 15.5, 5.5), (12.5, 16.5, 9.5), "mat_copper")]
    if b == "engine_cap":
        # The flange on an open end of the rod: -x end (the blockstate mirrors it for +x with a 180 turn)
        return [E((0, 2, 2), (1.5, 14, 14), "mat_copper"), E((-0.01, 6, 6), (0.5, 10, 10), "mat_steel_dk")] + \
            [E((0.5, y, z), (1.6, y + 1, z + 1), "mat_gold") for y, z in ((3, 3), (12, 3), (3, 12), (12, 12))]
    if b == "ascension_workbench":
        top = [E((0, 12, 0), (16, 15, 16), {"*": "side", "n": "mat_wood", "u": "top", "d": "mat_wood"})]
        if variant == "_side":
            # The left half: legs on the far corners, a pegboard of tools behind, a lamp
            return top + [E((x, 0, z), (x + 2, 12, z + 2), "mat_wood") for x in (0,) for z in (0, 14)] + \
                [E((0, 4, 1), (16, 12, 15), {"*": "side", "n": "front"}),
                 E((1, 15, 14), (15, 26, 15.5), "mat_wood"), E((3, 18, 13.5), (4, 24, 14), "mat_iron"), E((6, 20, 13.5), (9, 23, 14), "mat_gold"),
                 E((11, 17, 13.5), (12, 25, 14), "mat_steel"), E((12.5, 15, 3), (14, 20, 4.5), "mat_steel_dk"), E((11.5, 19, 2), (15, 21, 5.5), "mat_gold")]
        # The right half: legs, a vise, the rune circle
        return top + [E((x, 0, z), (x + 2, 12, z + 2), "mat_wood") for x in (14,) for z in (0, 14)] + \
            [E((2, 15, 9), (7, 18, 13), "mat_steel"), E((3, 18, 10), (6, 19, 12), "mat_gold"), E((4, 15, 7.5), (5, 16.5, 9), "mat_iron")]
    if b == "mini_crusher":
        return [E((2, 0, 2), (14, 2, 14), "mat_steel_dk"), E((3, 2, 3), (13, 11, 13), {"*": "mat_steel", "n": "front", "u": "mat_steel_dk"}),
                E((2, 11, 2), (14, 13, 14), "mat_gold"), E((3, 13, 3), (13, 16, 13), {"*": "mat_steel", "u": "top"}),
                E((13, 3, 4), (16, 10, 12), {"*": "mat_copper", "e": "side"}), E((0, 4, 6), (3, 9, 10), "mat_iron"),
                E((6, 0.5, -0.5), (10, 3, 3), "mat_steel_dk")]
    if b == "storage_terminal":
        return [E((1, 0, 3), (15, 7, 15), {"*": "side", "u": "mat_steel_dk", "d": "mat_steel_dk"}),
                E((2, 7, 6), (14, 8, 13), {"*": "mat_steel_dk", "u": "keys"}), E((6, 7, 2), (10, 9, 4), "mat_steel"),
                E((1, 7, 1), (15, 16, 3), {"*": "mat_steel", "n": "front"}, ("x", 22.5, [8, 7, 3])),
                E((2, 15.5, 2.5), (14, 16, 3), "mat_teal", ("x", 22.5, [8, 7, 3]))]
    return None


# ---- Ascension backpacks (Sophisticated Backpacks tiers; the Java side registers them only when it's installed) ----
BACKPACK_TIERS = [("ascendite", "Ascendite Backpack", "teal"), ("voidsteel", "Voidsteel Backpack", "gem"),
                  ("celestium", "Celestium Backpack", "glass"), ("ascendium", "Ascendium Backpack", "gold"),
                  ("singularity", "Singularity Backpack", "fire"), ("ascended", "Ascended Backpack", "cloth")]


def backpack_assets(lang):
    """Clips texture (our own colors drawn on Sophisticated's clip layout), block/item models that reuse its backpack
    model, blockstates, loot tables that keep the contents, and back-slot tags."""
    import zipfile
    from pngread import read as read_png
    jar = os.path.join(os.path.dirname(__file__), "..", "mod", "libs", "sophisticatedbackpacks-1.20.1-3.26.12.2206.jar")
    layout = None
    if os.path.exists(jar):
        tmp = os.path.join(os.path.dirname(__file__), "..", "mod", "build", "sb_clips_layout.png")
        os.makedirs(os.path.dirname(tmp), exist_ok=True)
        with zipfile.ZipFile(jar) as z, open(tmp, "wb") as f:
            f.write(z.read("assets/sophisticatedbackpacks/textures/block/netherite_clips.png"))
        layout = read_png(tmp)
    t = "factoryascension:block/"
    for tier, name, mat in BACKPACK_TIERS:
        bid = tier + "_backpack"
        if layout:
            px = [[(0, 0, 0, 0)] * len(layout[0]) for _ in layout]
            for y, row in enumerate(layout):
                for x, p in enumerate(row):
                    if p[3]:
                        edge = any(not (0 <= y + dy < len(layout) and 0 <= x + dx < len(row) and layout[y + dy][x + dx][3])
                                   for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                        px[y][x] = _shade(mat, 0.75 if edge else 0.25 + 0.3 * (y % 4) / 3) + (p[3],)
            make_textures.png(os.path.join(ASSETS, "textures", "block", tier + "_clips.png"), px)
        write_json(os.path.join(ASSETS, "models", "block", bid + ".json"),
                   {"parent": "sophisticatedbackpacks:block/backpack", "textures": {"clips": t + tier + "_clips"}})
        write_json(os.path.join(ASSETS, "models", "item", bid + ".json"), {"parent": t + bid})
        write_json(os.path.join(ASSETS, "blockstates", bid + ".json"),
                   {"variants": {"facing=%s" % d: {"model": t + bid, **({"y": r} if r else {})} for d, r in FACINGS}})
        write_json(os.path.join(DATA, "loot_tables", "blocks", bid + ".json"), {"type": "minecraft:block", "pools": [{
            "rolls": 1.0, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:item", "name": "factoryascension:" + bid}],
            "functions": [{"function": "sophisticatedbackpacks:copy_backpack_data"}], "name": "main"}]})
        lang["item.factoryascension." + bid] = name
        lang["block.factoryascension." + bid] = name
    ids = ["factoryascension:%s_backpack" % b[0] for b in BACKPACK_TIERS]
    for ns in ("curios", "accessories"):
        write_json(os.path.join(RES, "data", ns, "tags", "items", "back.json"), {"replace": False, "values": [{"id": i, "required": False} for i in ids]})


def mod_id_list():
    """The mod's block and item ids, for the scripts' placeholders (kubejs/startup_scripts/03_fa_mod_ids.js): if the
    jar is ever missing, the scripts register stand-ins under these ids so a world doesn't drop the mod's things."""
    lang = json.load(open(os.path.join(ASSETS, "lang", "en_us.json")))
    blocks = sorted(k.split(".")[2] for k in lang if k.startswith("block.factoryascension."))
    items = sorted(k.split(".")[2] for k in lang if k.startswith("item.factoryascension."))
    names = {k.split(".")[2]: v for k, v in lang.items() if k.startswith(("block.factoryascension.", "item.factoryascension."))}
    path = os.path.join(os.path.dirname(__file__), "..", "kubejs", "startup_scripts", "03_fa_mod_ids.js")
    with open(path, "w") as f:
        f.write("// priority: 990\n// Factory Ascension - the mod's block and item ids and names, for placeholders when its jar is missing.\n")
        f.write("// Generated by tools/make_mod_assets.py (from the mod's lang file).\n")
        f.write("global.FA_MOD_CONTENT = " + json.dumps({"blocks": blocks, "items": items, "names": names}, indent=1) + "\n")


def kubejs_block_models():
    """3D models for the pack's own KubeJS blocks, replacing the cubes KubeJS would generate for them."""
    out = os.path.join(os.path.dirname(__file__), "..", "kubejs", "assets", "kubejs", "models", "block")
    # Their own copies of the shared materials: these blocks exist in the Half download too, which has no mod jar
    kt = os.path.join(os.path.dirname(__file__), "..", "kubejs", "assets", "kubejs", "textures", "block")
    for m in ("gold", "glass"):
        make_textures.png(os.path.join(kt, "fa_mat_%s.png" % m), material(m))
    t = "kubejs:block/fa_mat_"
    altar = {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
             "textures": {"particle": "kubejs:block/ascension_altar", "a": "kubejs:block/ascension_altar", "gold": t + "gold",
                          "gem": "minecraft:block/amethyst_block", "crystal": "minecraft:block/amethyst_cluster"},
             "elements": [E((0, 0, 0), (16, 3, 16), "a"), E((4, 3, 4), (12, 13, 12), "a"), E((1, 13, 1), (15, 16, 15), "a"),
                          E((3.5, 12, 3.5), (12.5, 13, 12.5), "gold"), E((0.5, 2.5, 0.5), (15.5, 3.5, 15.5), "gold")] +
                         [E((x, 3, z), (x + 2, 8, z + 2), "gem") for x in (1, 13) for z in (1, 13)]}
    cond = {"parent": "minecraft:block/block", "render_type": "minecraft:translucent",
            "textures": {"particle": "kubejs:block/essence_condenser", "c": "kubejs:block/essence_condenser", "glass": t + "glass",
                         "core": "kubejs:block/condensed_essence_0", "gold": t + "gold"},
            "elements": [E((0, 0, 0), (16, 2, 16), "c"), E((0, 14, 0), (16, 16, 16), "c"), E((4, 2, 4), (12, 14, 12), "core"),
                         E((1, 2, 1), (15, 14, 15), "glass")] + corners(0, 16, 2, 14, 2, "c") +
                        [E((3, 13.5, 3), (13, 14.2, 13), "gold")]}
    # Written under the blocks' own names, which is how KubeJS lets you replace a block's generated model
    for old in ("ascension_altar_3d", "essence_condenser_3d"):
        for d in (out, os.path.join(RES, "assets", "kubejs", "models", "block")):
            if os.path.exists(os.path.join(d, old + ".json")):
                os.remove(os.path.join(d, old + ".json"))
    for name, m in (("ascension_altar", altar), ("essence_condenser", cond)):
        write_json(os.path.join(out, name + ".json"), m)
        # The jar carries a copy too, so the Full download never depends on the kubejs folder for these
        write_json(os.path.join(RES, "assets", "kubejs", "models", "block", name + ".json"), m)


# ---- Connected glass: 16 frame textures per kind (a frame edge only where nothing matching is next to it) ----
GLASS = {
    "factory_glass": {"name": "Factory Glass", "frame": "steel", "tint": (200, 226, 240, 60)},
    "ascendite_glass": {"name": "Ascendite Glass", "frame": "teal", "tint": (120, 230, 220, 70)},
    "gilded_glass": {"name": "Gilded Glass", "frame": "gold", "tint": (255, 236, 190, 55)},
    "smoked_glass": {"name": "Smoked Glass", "frame": "steel_dk", "tint": (40, 40, 52, 150)},
}
# For each face: which side of the block each texture edge (top, right, bottom, left) looks at
FACE_EDGES = {"up": ("north", "east", "south", "west"), "down": ("south", "east", "north", "west"),
              "north": ("up", "west", "down", "east"), "south": ("up", "east", "down", "west"),
              "east": ("up", "north", "down", "south"), "west": ("up", "south", "down", "north")}


def glass_tex(kind, code):
    """code bits: 1 top, 2 right, 4 bottom, 8 left joined to more glass (no frame there)."""
    g = GLASS[kind]
    px = [[g["tint"]] * S for _ in range(S)]
    for y in range(S):
        for x in range(S):
            if (x + y) % 11 == 0 and 4 < x < 28 and 4 < y < 28 and (x - y) % 3 == 0:
                px[y][x] = (255, 255, 255, 110)        # a glint streak
    edges = [code & 1, code & 2, code & 4, code & 8]
    for y in range(S):
        for x in range(S):
            on = (not edges[0] and y < 2) or (not edges[1] and x >= S - 2) or (not edges[2] and y >= S - 2) or (not edges[3] and x < 2)
            if on:
                px[y][x] = _shade(g["frame"], 0.25 if (x < 2 or y < 2) else 0.6) + (255,)
    return px


def glass_assets():
    t = "factoryascension:block/"
    for kind, g in GLASS.items():
        for code in range(16):
            make_textures.png(os.path.join(ASSETS, "textures", "block", "%s_%d.png" % (kind, code)), glass_tex(kind, code))
        parts = []
        for face, edges in FACE_EDGES.items():
            for code in range(16):
                name = "%s_%s_%d" % (kind, face, code)
                write_json(os.path.join(ASSETS, "models", "block", "glass", name + ".json"), {
                    "parent": "minecraft:block/block", "render_type": "minecraft:translucent",
                    "textures": {"particle": t + "%s_0" % kind, "glass": t + "%s_%d" % (kind, code)},
                    "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": {face: {"texture": "#glass", "cullface": face}}}]})
                parts.append({"when": {edges[i]: "true" if code & (1 << i) else "false" for i in range(4)},
                              "apply": {"model": "factoryascension:block/glass/" + name}})
        write_json(os.path.join(ASSETS, "blockstates", kind + ".json"), {"multipart": parts})
        write_json(os.path.join(ASSETS, "models", "item", kind + ".json"), {
            "parent": "minecraft:block/cube_all", "render_type": "minecraft:translucent", "textures": {"all": t + kind + "_0"}})
        write_json(os.path.join(DATA, "loot_tables", "blocks", kind + ".json"), {
            "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "factoryascension:" + kind}],
                                                  "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def main():
    tex = os.path.join(ASSETS, "textures", "block")
    os.makedirs(tex, exist_ok=True)
    for m in MATERIALS:
        make_textures.png(os.path.join(tex, "mat_%s.png" % m), material(m))
    kubejs_block_models()
    for b, faces in FACES.items():
        for face, draw in faces.items():
            make_textures.png(os.path.join(tex, "%s_%s.png" % (b, face)), draw())
        mods, variants = models(b)
        for name, model in mods.items():
            write_json(os.path.join(ASSETS, "models", "block", name + ".json"), model)
        write_json(os.path.join(ASSETS, "models", "item", b + ".json"), {"parent": "factoryascension:block/" + b + ("_item" if b + "_item" in mods else "")})
        write_json(os.path.join(ASSETS, "blockstates", b + ".json"), variants if "multipart" in variants else {"variants": variants})
        conds = [{"condition": "minecraft:survives_explosion"}]
        if b == "ascension_workbench":   # two halves, one drop
            conds.append({"condition": "minecraft:block_state_property", "block": "factoryascension:" + b, "properties": {"main": "true"}})
        write_json(os.path.join(DATA, "loot_tables", "blocks", b + ".json"), {
            "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "factoryascension:" + b}],
                                                  "conditions": conds}]})
    write_json(os.path.join(RES, "data", "minecraft", "tags", "blocks", "mineable", "pickaxe.json"),
               {"replace": False, "values": ["factoryascension:" + b for b in BLOCKS]})
    write_json(os.path.join(RES, "data", "minecraft", "tags", "blocks", "needs_iron_tool.json"),
               {"replace": False, "values": ["factoryascension:" + b for b in BLOCKS if not b.startswith("engine_")]})   # engine parts: any pickaxe
    # Storage Linker (a plain item)
    os.makedirs(os.path.join(ASSETS, "textures", "item"), exist_ok=True)
    make_textures.png(os.path.join(ASSETS, "textures", "item", "storage_linker.png"), linker_item())
    write_json(os.path.join(ASSETS, "models", "item", "storage_linker.json"),
               {"parent": "minecraft:item/handheld", "textures": {"layer0": "factoryascension:item/storage_linker"}})
    lang = {"itemGroup.factoryascension": "Factory Ascension", "item.factoryascension.storage_linker": "Storage Linker"}
    for b in BLOCKS:
        lang["block.factoryascension." + b] = NAMES[b]
    lang.update({
        "gui.factoryascension.search": "Search...",
        "gui.factoryascension.terminal_links": "%s chests",
        "gui.factoryascension.bench_apply": "Apply",
        "gui.factoryascension.crusher_motor": "Motor",
        "gui.factoryascension.crusher_no_battery": "No battery",
        "gui.factoryascension.bench_star": "Star Up",
        "gui.factoryascension.bench_legendary": "%s (%s / %s stars)",
        "gui.factoryascension.bench_max": "Fully legendary!",
        "gui.factoryascension.linker_bound": "Linker set to this terminal (%s chests linked). Now right-click chests to link them.",
        "gui.factoryascension.linker_unbound": "Right-click a Universal Storage Terminal first to set the linker",
        "gui.factoryascension.linker_lost": "The linker's terminal isn't here (broken, unloaded or another dimension)",
        "gui.factoryascension.linker_linked": "Linked! %s / %s chests",
        "gui.factoryascension.linker_unlinked": "Unlinked. %s / %s chests",
        "gui.factoryascension.linker_too_far": "Too far from the terminal (%3$s blocks max)",
        "gui.factoryascension.linker_full": "This terminal is full (%2$s chests max)",
        "gui.factoryascension.linker_cleared": "Linker cleared",
        "gui.factoryascension.linker_tip_bound": "Set to the terminal at %s, %s, %s",
        "gui.factoryascension.linker_tip_unbound": "Not set: right-click a Universal Storage Terminal",
        "gui.factoryascension.sort": "Sort",
        "gui.factoryascension.collection": "Collection: %s / %s",
        "gui.factoryascension.tank": "Liquid Essence: %s / %s mB",
        "gui.factoryascension.forge_working": "Leveling: +%s levels/s",
        "gui.factoryascension.forge_idle": "Put gear in and fill the tank",
        "gui.factoryascension.forge_total": "Levels forged: %s",
        "gui.factoryascension.packs": "Packs",
        "gui.factoryascension.cards": "Cards",
        "gui.factoryascension.opened": "Opened: %s",
        "gui.factoryascension.salvager_loot": "Loot in, cores out",
        "gui.factoryascension.salvager_total": "Salvaged %s items: %s Essence",
        "gui.factoryascension.fuse": "Fuse",
        "gui.factoryascension.fusion_help": "Put in copies of one tome to merge them, or a Fusion's parent tomes and cores",
        "gui.factoryascension.smelter_tier": "Tier %s: %s at once",
        "gui.factoryascension.smelter_stressed": "Overstressed: needs more power",
        "gui.factoryascension.smelter_idle": "Turn the shaft in the back",
        "gui.factoryascension.smelter_rpm": "%s RPM, %s smelted",
        "gui.factoryascension.engine_jammed": "Two Engine Cores share these modules: use one core per engine",
        "gui.factoryascension.engine_no_coils": "Attach Coil Modules (and a Burner) to the core",
        "gui.factoryascension.engine_no_fuel": "No fuel: fill a Burner Module (%s attached)",
        "gui.factoryascension.engine_status": "%s RPM, %s SU from %s coils, %s gearboxes, %s/%s burners lit",
        "gui.factoryascension.engine_modules": "Modules: %s coils, %s gearboxes, %s burners (%s/%s)",
        "gui.factoryascension.engine_too_big": "Too many modules: the extra ones do nothing",
        "gui.factoryascension.burner_status": "Fuel: %s %s, about %ss of burning",
        "gui.factoryascension.burner_idle": "Not burning",
        "gui.factoryascension.burner_lit": "Burning",
        "gui.factoryascension.burner_left": "%sm %ss left",
        "gui.factoryascension.coil_each": "added by every powered coil",
        "gui.factoryascension.coil_powered": "Coils powered: %s of %s",
        "gui.factoryascension.engine_how": "Put it next to an Engine Core, or next to modules that touch one.",
        "gui.factoryascension.engine_jammed_short": "Jammed: two cores share these modules",
        "gui.factoryascension.engine_modules_bar": "Modules used: %s / %s",
        "gui.factoryascension.engine_need_burners": "Add %s more lit Burner(s) for all coils",
        "gui.factoryascension.engine_need_coils": "No coils: attach Coil Modules",
        "gui.factoryascension.engine_need_fuel": "No fuel: fill a Burner",
        "gui.factoryascension.engine_not_connected": "Not connected to an engine",
        "gui.factoryascension.engine_running": "Running at full power",
        "gui.factoryascension.engine_summary": "Engine: %s RPM, %s SU",
        "gui.factoryascension.gearbox_count": "%s gearboxes on this engine",
        "gui.factoryascension.gearbox_next": "Next gearbox: %s RPM",
        "gui.factoryascension.gearbox_top": "Top speed reached",
        "gui.factoryascension.gearbox_same_su": "Power stays %s SU at any speed",
    })
    for kind, g in GLASS.items():
        lang["block.factoryascension." + kind] = g["name"]
    glass_assets()
    # Keep the mobs' names (tools/mob_art.py adds them)
    old = os.path.join(ASSETS, "lang", "en_us.json")
    if os.path.exists(old):
        for k, v in json.load(open(old)).items():
            if k.startswith("entity.") or k.endswith("_spawn_egg"):
                lang.setdefault(k, v)
    backpack_assets(lang)
    write_json(os.path.join(ASSETS, "lang", "en_us.json"), lang)
    import realm_gen
    realm_gen.main()          # the Ascended Realm's blocks and worldgen (adds to the lang file)
    mod_id_list()
    print("wrote textures, models, block states, loot tables, tags and lang for %d blocks" % len(BLOCKS))


if __name__ == "__main__":
    main()
