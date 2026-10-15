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
          "engine_core", "engine_coil", "engine_gearbox", "engine_burner"]
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


FACES = {
    "ascension_vault": {"front": vault_front, "side": side, "top": top},
    "card_binder": {"front": binder_side, "side": binder_side, "top": binder_top},
    "essence_forge": {"front": forge_front, "side": side, "top": forge_top},
    "pack_opener": {"front": opener_front, "side": side, "top": top},
    "salvager": {"front": salvager_front, "side": side, "top": salvager_top},
    "tome_fusion_table": {"front": fusion_side, "side": fusion_side, "top": fusion_top},
    "super_smelter": {"front": smelter_front, "front_lit": lambda: smelter_front(True), "back": smelter_back, "side": side, "top": smelter_top},
    "engine_core": {"front": engine_front, "side": engine_side, "top": engine_top},
    "engine_coil": {"side": coil_side, "top": engine_top},
    "engine_gearbox": {"side": gearbox_side, "top": engine_top},
    "engine_burner": {"side": burner_side, "side_lit": lambda: burner_side(True), "top": burner_top},
}
NAMES = {"ascension_vault": "Ascension Vault", "card_binder": "Card Binder", "essence_forge": "Essence Forge", "pack_opener": "Pack Opener",
         "salvager": "Salvager", "tome_fusion_table": "Tome Fusion Table", "super_smelter": "Super Smelter",
         "engine_core": "Modular Engine Core", "engine_coil": "Engine Coil Module", "engine_gearbox": "Engine Gearbox Module",
         "engine_burner": "Engine Burner Module"}
FACINGS = (("north", 0), ("east", 90), ("south", 180), ("west", 270))


def cube(north, south, side, up, down=None):
    t = lambda n: "factoryascension:block/" + n
    return {"parent": "minecraft:block/cube", "textures": {"particle": t(side), "north": t(north), "south": t(south), "east": t(side),
                                                           "west": t(side), "up": t(up), "down": t(down or up)}}


def models(b):
    """{model name: model json} for a block, and its blockstate variants."""
    t = "factoryascension:block/"
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
    for b, faces in FACES.items():
        for face, draw in faces.items():
            make_textures.png(os.path.join(tex, "%s_%s.png" % (b, face)), draw())
        mods, variants = models(b)
        for name, model in mods.items():
            write_json(os.path.join(ASSETS, "models", "block", name + ".json"), model)
        write_json(os.path.join(ASSETS, "models", "item", b + ".json"), {"parent": "factoryascension:block/" + b})
        write_json(os.path.join(ASSETS, "blockstates", b + ".json"), {"variants": variants})
        write_json(os.path.join(DATA, "loot_tables", "blocks", b + ".json"), {
            "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "factoryascension:" + b}],
                                                  "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    write_json(os.path.join(RES, "data", "minecraft", "tags", "blocks", "mineable", "pickaxe.json"),
               {"replace": False, "values": ["factoryascension:" + b for b in BLOCKS]})
    write_json(os.path.join(RES, "data", "minecraft", "tags", "blocks", "needs_iron_tool.json"),
               {"replace": False, "values": ["factoryascension:" + b for b in BLOCKS if not b.startswith("engine_")]})   # engine parts: any pickaxe
    lang = {"itemGroup.factoryascension": "Factory Ascension"}
    for b in BLOCKS:
        lang["block.factoryascension." + b] = NAMES[b]
    lang.update({
        "gui.factoryascension.search": "Search...",
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
    write_json(os.path.join(ASSETS, "lang", "en_us.json"), lang)
    print("wrote textures, models, block states, loot tables, tags and lang for %d blocks" % len(BLOCKS))


if __name__ == "__main__":
    main()
