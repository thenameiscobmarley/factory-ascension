#!/usr/bin/env python3
"""Art and models for the v25 gear: the flashbang, night vision goggles (icons and the worn helmet), the plate carrier
(icon and worn layer) and its plates, the drugs (items, the cannabis and coca crops, seeds, leaves, the Chem Lab), the
raid blocks (ballistic concrete, glass, panes, steel door and trapdoor) and the effect icons, plus their names.

  python3 tools/v25_art.py      (make_mod_assets.py runs it)
"""
import json
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))
import make_textures as T  # noqa: E402

NS = "factoryascension"
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
A = os.path.join(ROOT, "mod", "src", "main", "resources", "assets", NS)


def clamp(v):
    return max(0, min(255, int(round(v))))


def shade(c, f):
    if f >= 0:
        return tuple(clamp(c[i] + (255 - c[i]) * f) for i in range(3)) + (255,)
    return tuple(clamp(c[i] * (1 + f)) for i in range(3)) + (255,)


def canvas(n=16):
    return [[(0, 0, 0, 0) for _ in range(n)] for _ in range(n)]


def rect(px, x0, y0, x1, y1, c, f=None):
    if c is not None and len(c) == 3:
        c = tuple(c) + (255,)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if 0 <= y < len(px) and 0 <= x < len(px[0]):
                px[y][x] = f(x, y) if f else c


def ell(px, cx, cy, rx, ry, col, lit=0.25, dark=-0.3):
    for y in range(len(px)):
        for x in range(len(px[0])):
            dx, dy = (x + 0.5 - cx) / rx, (y + 0.5 - cy) / ry
            d = dx * dx + dy * dy
            if d <= 1:
                f = lit * max(0, -dx - dy) * 0.7 + dark * max(0, dx + dy) * 0.7
                px[y][x] = shade(col, f)


def line(px, x0, y0, x1, y1, c):
    n = max(abs(x1 - x0), abs(y1 - y0), 1)
    for i in range(n + 1):
        x, y = round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n)
        if 0 <= y < len(px) and 0 <= x < len(px[0]):
            px[y][x] = c


def save_item(name, px, edge):
    T.png(os.path.join(A, "textures", "item", name + ".png"), T.hd_item(px, edge, name))
    model(name, {"parent": "minecraft:item/generated", "textures": {"layer0": NS + ":item/" + name}})


def model(name, data, kind="item"):
    p = os.path.join(A, "models", kind, name + ".json")
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w") as f:
        json.dump(data, f, indent=1)
        f.write("\n")


def blockstate(name, data):
    with open(os.path.join(A, "blockstates", name + ".json"), "w") as f:
        json.dump(data, f, indent=1)
        f.write("\n")


def tex(path, px):
    p = os.path.join(A, "textures", path + ".png")
    os.makedirs(os.path.dirname(p), exist_ok=True)
    T.png(p, px)


# ---------------- Items ----------------
def flashbang():
    px = canvas()
    body = (70, 74, 80)
    rect(px, 5, 4, 10, 14, None, lambda x, y: shade(body, 0.3 if x == 5 else (-0.3 if x == 10 else (0.1 if y % 3 == 0 else 0))))
    for y in (6, 9, 12):
        for x in range(5, 11):
            px[y][x] = shade((40, 42, 46), 0)          # the vent holes' band
            if x in (6, 8):
                px[y][x] = (10, 10, 10, 255)
    rect(px, 6, 2, 9, 3, (160, 160, 160, 255))
    line(px, 9, 2, 12, 1, (190, 190, 190, 255))
    px[1][13] = px[2][13] = (200, 200, 200, 255)
    px[14][6] = px[14][9] = shade(body, -0.4)
    save_item("flashbang", px, (40, 44, 50, 255))


def nods(name, tube):
    px = canvas()
    strap = (40, 42, 38)
    rect(px, 1, 3, 14, 4, strap)
    for x in (4, 10):
        rect(px, x - 2, 5, x + 2, 12, None, lambda xx, yy: shade((54, 58, 52), 0.25 if xx == x - 2 else (-0.3 if xx == x + 2 else 0)))
        ell(px, x + 0.5, 13.0, 2.6, 2.2, (60, 64, 58))
        ell(px, x + 0.5, 13.0, 1.6, 1.3, tube, 0.5, -0.2)
        px[12][x - 1] = shade(tube, 0.7)
    rect(px, 6, 6, 8, 9, (70, 74, 68, 255))       # the bridge and mount
    rect(px, 7, 4, 7, 5, (90, 90, 90, 255))
    save_item(name, px, (30, 34, 28, 255))


def carrier():
    px = canvas()
    c = (62, 70, 50)
    for y in range(2, 15):
        for x in range(2, 14):
            arm = (y < 6 and 5 <= x <= 10)
            if arm:
                continue
            f = 0.2 if x < 4 else (-0.25 if x > 11 else 0)
            if y in (8, 11) and 4 <= x <= 11:
                f -= 0.25                           # MOLLE rows
            px[y][x] = shade(c, f + (0.05 if (x * 7 + y * 3) % 5 == 0 else 0))
    for x in (3, 12):
        rect(px, x - 1, 1, x, 2, shade(c, -0.1))      # shoulder straps
    rect(px, 5, 9, 10, 10, shade(c, -0.35))          # pouch tops
    rect(px, 7, 12, 8, 13, (120, 110, 80, 255))      # buckle
    save_item("plate_carrier", px, (34, 40, 26, 255))


PLATES = {
    "polyethylene_plate": ((200, 200, 192), "plain"),
    "steel_plate": ((120, 126, 132), "steel"),
    "titanium_plate": ((150, 156, 170), "brushed"),
    "ceramic_plate": ((196, 180, 150), "tiles"),
    "hybrid_plate": ((90, 96, 92), "hybrid"),
    "ascendite_plate": ((80, 200, 220), "glow"),
}


def plate(name, c, style):
    px = canvas()
    for y in range(2, 15):
        for x in range(3, 13):
            # A plate's shape: shoulder cuts at the top corners
            if y < 5 and (x < 3 + (5 - y) or x > 12 - (5 - y)):
                continue
            f = 0.25 if x < 5 else (-0.28 if x > 10 else 0)
            if style == "tiles" and (x % 3 == 0 or y % 3 == 0):
                f -= 0.18
            if style == "brushed" and y % 2 == 0:
                f += 0.06
            if style == "hybrid" and y > 9:
                f -= 0.1
            if style == "steel" and (x + y) % 5 == 0:
                f += 0.05
            px[y][x] = shade(c, f)
    if style == "hybrid":
        rect(px, 4, 10, 11, 10, (190, 176, 140, 255))
    if style == "glow":
        line(px, 5, 6, 10, 12, (200, 255, 255, 255))
        line(px, 10, 6, 6, 11, (150, 240, 250, 255))
    save_item(name, px, (40, 40, 40, 255))


def joint():
    px = canvas()
    for i in range(10):
        x, y = 3 + i, 12 - i
        px[y][x] = shade((236, 230, 214), 0.15 if i % 2 else 0)
        px[y + 1][x] = shade((220, 214, 196), -0.15)
    px[12][3] = px[13][3] = (180, 130, 70, 255)     # the filter
    px[2][13] = (255, 120, 30, 255)                 # the ember
    px[3][12] = (200, 70, 20, 255)
    px[1][13] = (120, 120, 120, 255)
    px[0][14] = (90, 90, 90, 255)
    save_item("joint", px, (120, 110, 90, 255))


def baggie(name, powder, label):
    px = canvas()
    for y in range(4, 15):
        for x in range(3, 13):
            px[y][x] = (230, 236, 240, 150)         # clear plastic
    rect(px, 3, 3, 12, 4, (200, 70, 60, 255) if label else (90, 150, 220, 255))   # the zip
    for y in range(9, 14):
        for x in range(4, 12):
            px[y][x] = shade(powder, 0.15 if (x + y) % 3 == 0 else (-0.12 if x > 9 else 0))
    for x in range(5, 11):
        px[8][x] = shade(powder, 0.25)
    if name == "methamphetamine":
        for (x, y) in ((5, 10), (8, 11), (10, 9), (6, 12)):
            px[y][x] = (200, 240, 255, 255)        # crystal glints
    save_item(name, px, (110, 120, 128, 255))


def syringe(name, fluid):
    px = canvas()
    for i in range(9):
        x, y = 4 + i, 11 - i
        px[y][x] = (220, 228, 232, 255)
        px[y + 1][x] = (190, 198, 204, 255)
        if 2 <= i <= 6:
            px[y][x] = shade(fluid, 0.15)
            px[y + 1][x] = shade(fluid, -0.1)
    line(px, 13, 2, 15, 0, (200, 200, 210, 255))   # the needle
    rect(px, 2, 12, 4, 14, (90, 90, 100, 255))     # the plunger
    px[13][1] = px[11][5] = (120, 120, 130, 255)
    save_item(name, px, (80, 80, 90, 255))


def mushrooms():
    px = canvas()
    for (cx, cy, r) in ((6, 7, 3.6), (11, 9, 2.8)):
        rect(px, int(cx) - 1, int(cy), int(cx), int(cy + 5), (230, 220, 196, 255))
        ell(px, cx, cy, r, r * 0.6, (180, 130, 80), 0.3, -0.35)
        px[int(cy) - 1][int(cx) - 1] = (240, 220, 180, 255)
    rect(px, 3, 14, 13, 14, (90, 70, 50, 255))
    save_item("magic_mushrooms", px, (80, 54, 30, 255))


def leaf(name, c, kind):
    px = canvas()
    if kind == "cannabis":
        # Seven fingers from one point
        for k, (ang, ln) in enumerate(((-90, 7), (-55, 6), (-125, 6), (-25, 5), (-155, 5), (5, 3.5), (-185, 3.5))):
            a = math.radians(ang)
            for t in range(int(ln * 2) + 1):
                d = t / 2
                w = 1.1 * math.sin(math.pi * d / ln) + 0.2
                x, y = 8 + math.cos(a) * d, 11 + math.sin(a) * d
                for s in (-w, 0, w):
                    xx, yy = round(x - math.sin(a) * s), round(y + math.cos(a) * s)
                    if 0 <= xx < 16 and 0 <= yy < 16:
                        px[yy][xx] = shade(c, 0.15 if s < 0 else (-0.2 if s > 0 else -0.05))
        line(px, 8, 11, 8, 15, shade(c, -0.3))
    else:
        ell(px, 8, 8, 3.4, 6.2, c)
        line(px, 8, 3, 8, 15, shade(c, -0.25))
    save_item(name + "_leaf", px, shade(c, -0.6))


def seeds(name, c):
    px = canvas()
    rng = random.Random(name)
    for _ in range(5):
        x, y = rng.randint(4, 10), rng.randint(5, 11)
        ell(px, x + 0.5, y + 0.5, 1.8, 1.3, c, 0.35, -0.3)
    save_item(name + "_seeds", px, shade(c, -0.6))


# ---------------- Crops ----------------
def crop(name, c, bud):
    for stage in range(4):
        px = canvas()
        h = 3 + stage * 3.5
        rng = random.Random(name + str(stage))
        for stem in (4, 8, 12):
            hh = h * (0.8 + rng.random() * 0.3)
            for y in range(16 - int(hh), 16):
                px[y][stem] = shade(c, -0.35)
            for y in range(16 - int(hh), 15, 2):
                spread = 1 + int((y - (16 - hh)) / 4)
                for dx in range(-spread, spread + 1):
                    xx = stem + dx
                    if 0 <= xx < 16 and dx != 0:
                        px[y][xx] = shade(c, 0.12 if dx < 0 else -0.12)
            if stage == 3 and bud:
                top = 16 - int(hh)
                for dy in range(3):
                    if 0 <= top + dy < 16:
                        px[top + dy][stem] = shade(bud, 0.1 * dy)
                        if stem + 1 < 16:
                            px[top + dy][stem + 1] = shade(bud, -0.15)
        tex("block/%s_stage%d" % (name, stage), px)
        model("%s_stage%d" % (name, stage), {"parent": "minecraft:block/crop", "render_type": "minecraft:cutout",
                                              "textures": {"crop": "%s:block/%s_stage%d" % (NS, name, stage)}}, "block")
    blockstate(name, {"variants": {"age=%d" % a: {"model": "%s:block/%s_stage%d" % (NS, name, a // 2)} for a in range(8)}})


# ---------------- Blocks ----------------
def concrete():
    rng = random.Random(7)
    px = canvas()
    for y in range(16):
        for x in range(16):
            f = (rng.random() - 0.5) * 0.12
            if x in (0, 15) or y in (0, 15):
                f -= 0.15
            if (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12)):
                f -= 0.4                            # the rebar caps
            px[y][x] = shade((128, 132, 128), f)
    for x in range(1, 15):
        px[7][x] = shade(px[7][x], -0.06)
    tex("block/ballistic_concrete", px)
    model("ballistic_concrete", {"parent": "minecraft:block/cube_all", "textures": {"all": NS + ":block/ballistic_concrete"}}, "block")
    blockstate("ballistic_concrete", {"variants": {"": {"model": NS + ":block/ballistic_concrete"}}})
    model("ballistic_concrete", {"parent": NS + ":block/ballistic_concrete"})


def glass():
    px = canvas()
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            if edge:
                px[y][x] = shade((70, 76, 82), 0.1 if x == 0 or y == 0 else -0.2)
            else:
                a = 70 + (40 if (x + y) in (6, 7, 18, 19) else 0)    # streaks
                px[y][x] = (150, 176, 190, a)
    rect(px, 1, 1, 14, 1, (110, 120, 128, 255))
    tex("block/ballistic_glass", px)
    model("ballistic_glass", {"parent": "minecraft:block/cube_all", "render_type": "minecraft:translucent",
                               "textures": {"all": NS + ":block/ballistic_glass"}}, "block")
    blockstate("ballistic_glass", {"variants": {"": {"model": NS + ":block/ballistic_glass"}}})
    model("ballistic_glass", {"parent": NS + ":block/ballistic_glass"})
    # The pane: vanilla's pane templates
    edge = canvas()
    rect(edge, 7, 0, 8, 15, (80, 86, 92, 255))
    tex("block/ballistic_glass_pane_top", edge)
    t = {"pane": NS + ":block/ballistic_glass", "edge": NS + ":block/ballistic_glass_pane_top"}
    for part in ("post", "side", "side_alt", "noside", "noside_alt"):
        model("ballistic_glass_pane_" + part, {"parent": "minecraft:block/template_glass_pane_" + part, "render_type": "minecraft:translucent", "textures": t}, "block")
    m = lambda p, **k: dict({"model": NS + ":block/ballistic_glass_pane_" + p}, **k)
    blockstate("ballistic_glass_pane", {"multipart": [
        {"apply": m("post")},
        {"when": {"north": "true"}, "apply": m("side")},
        {"when": {"east": "true"}, "apply": m("side", y=90)},
        {"when": {"south": "true"}, "apply": m("side_alt")},
        {"when": {"west": "true"}, "apply": m("side_alt", y=90)},
        {"when": {"north": "false"}, "apply": m("noside")},
        {"when": {"east": "false"}, "apply": m("noside_alt")},
        {"when": {"south": "false"}, "apply": m("noside_alt", y=90)},
        {"when": {"west": "false"}, "apply": m("noside", y=270)}]})
    model("ballistic_glass_pane", {"parent": "minecraft:item/generated", "render_type": "minecraft:translucent",
                                    "textures": {"layer0": NS + ":block/ballistic_glass"}})


def steel(px, x0, y0, x1, y1, base=(92, 98, 104)):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            f = 0.03 * ((x * 13 + y * 7) % 3 - 1)
            if x in (x0, x1) or y in (y0, y1):
                f += 0.18 if (x == x0 or y == y0) else -0.3
            px[y][x] = shade(base, f)


def door():
    top, bot = canvas(), canvas()
    steel(top, 0, 0, 15, 15)
    steel(bot, 0, 0, 15, 15)
    # A small armoured viewing slit up top, a heavy handle and bolts below
    rect(top, 4, 6, 11, 8, (30, 34, 38, 255))
    rect(top, 4, 6, 11, 6, (150, 176, 190, 255))
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        top[y][x] = shade((150, 150, 150), 0.2)
        bot[y][x] = shade((150, 150, 150), 0.2)
    rect(bot, 12, 3, 13, 7, (60, 60, 64, 255))
    rect(bot, 12, 4, 13, 4, (170, 170, 176, 255))
    for y in (10, 11):
        for x in range(2, 14):
            bot[y][x] = shade(bot[y][x], -0.12)
    tex("block/steel_door_top", top)
    tex("block/steel_door_bottom", bot)
    item = canvas()
    for y in range(1, 15):
        for x in range(4, 12):
            item[y][x] = shade((92, 98, 104), 0.2 if x == 4 else (-0.3 if x == 11 else 0))
    rect(item, 5, 3, 10, 4, (150, 176, 190, 255))
    rect(item, 10, 8, 10, 10, (170, 170, 176, 255))
    save_item("steel_door", item, (40, 44, 48, 255))
    for half in ("bottom", "top"):
        for hinge in ("left", "right"):
            for o in ("", "_open"):
                n = "door_%s_%s%s" % (half, hinge, o)
                model("steel_" + n, {"parent": "minecraft:block/" + n, "render_type": "minecraft:cutout",
                                      "textures": {"bottom": NS + ":block/steel_door_bottom", "top": NS + ":block/steel_door_top"}}, "block")
    base = {"east": 0, "south": 90, "west": 180, "north": 270}
    v = {}
    for f, b in base.items():
        for half, hn in (("lower", "bottom"), ("upper", "top")):
            for hinge in ("left", "right"):
                for op in ("false", "true"):
                    y = b if op == "false" else (b + (90 if hinge == "left" else 270)) % 360
                    entry = {"model": "%s:block/steel_door_%s_%s%s" % (NS, hn, hinge, "_open" if op == "true" else "")}
                    if y:
                        entry["y"] = y
                    v["facing=%s,half=%s,hinge=%s,open=%s" % (f, half, hinge, op)] = entry
    blockstate("steel_door", {"variants": v})


def trapdoor():
    px = canvas()
    steel(px, 0, 0, 15, 15)
    for y in (4, 8, 11):
        for x in range(2, 14):
            px[y][x] = shade(px[y][x], -0.15)
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        px[y][x] = shade((150, 150, 150), 0.2)
    tex("block/steel_trapdoor", px)
    for part in ("bottom", "top", "open"):
        model("steel_trapdoor_" + part, {"parent": "minecraft:block/template_orientable_trapdoor_" + part, "render_type": "minecraft:cutout",
                                          "textures": {"texture": NS + ":block/steel_trapdoor"}}, "block")
    base = {"north": 0, "east": 90, "south": 180, "west": 270}
    v = {}
    for f, b in base.items():
        for half in ("bottom", "top"):
            for op in ("false", "true"):
                if op == "false":
                    e = {"model": "%s:block/steel_trapdoor_%s" % (NS, half)}
                    y = b
                elif half == "bottom":
                    e = {"model": "%s:block/steel_trapdoor_open" % NS}
                    y = b
                else:
                    e = {"model": "%s:block/steel_trapdoor_open" % NS, "x": 180}
                    y = (b + 180) % 360
                if y:
                    e["y"] = y
                v["facing=%s,half=%s,open=%s" % (f, half, op)] = e
    blockstate("steel_trapdoor", {"variants": v})
    model("steel_trapdoor", {"parent": NS + ":block/steel_trapdoor_bottom"})


def chem_lab():
    # Textures: steel table top, glass, a burner
    top = canvas()
    steel(top, 0, 0, 15, 15, (150, 156, 160))
    for (x, y) in ((4, 5), (9, 10), (11, 3)):
        top[y][x] = (110, 140, 90, 255)               # stains
    tex("block/chem_lab_top", top)
    side = canvas()
    steel(side, 0, 0, 15, 15, (70, 74, 78))
    rect(side, 2, 3, 13, 4, (40, 42, 44, 255))
    tex("block/chem_lab_side", side)
    gl = canvas()
    for y in range(16):
        for x in range(16):
            gl[y][x] = (200, 230, 236, 120) if x not in (0, 15) else (230, 250, 255, 200)
    tex("block/chem_lab_glass", gl)
    fl = canvas()
    for y in range(16):
        for x in range(16):
            fl[y][x] = (90, 200, 220, 210) if (x + y) % 5 else (170, 240, 250, 230)
    tex("block/chem_lab_fluid", fl)
    T_ = lambda t: "#" + t
    def box(f, t, tx, uv=None):
        faces = {}
        for d in ("north", "south", "east", "west", "up", "down"):
            faces[d] = {"texture": T_(tx)}
        return {"from": f, "to": t, "faces": faces}
    elements = [
        box([0, 10, 0], [16, 12, 16], "top"),                         # the table
        box([1, 0, 1], [3, 10, 3], "side"), box([13, 0, 1], [15, 10, 3], "side"),
        box([1, 0, 13], [3, 10, 15], "side"), box([13, 0, 13], [15, 10, 15], "side"),
        box([2, 3, 2], [14, 4, 14], "side"),                          # a shelf
        box([3, 12, 3], [7, 18, 7], "glass"),                         # a flask
        box([3.5, 12, 3.5], [6.5, 15, 6.5], "fluid"),
        box([4.5, 18, 4.5], [5.5, 21, 5.5], "glass"),                 # its neck
        box([9, 12, 8], [13, 13, 12], "side"),                        # the burner
        box([10, 13, 9], [12, 19, 11], "glass"),                      # a column
        box([10.5, 13, 9.5], [11.5, 16, 10.5], "fluid"),
        box([5, 20, 4.5], [11, 21, 5.5], "glass"),                    # tubing between them
        box([10.5, 19, 4.5], [11.5, 20, 10.5], "glass"),
        box([3, 12, 11], [6, 14, 14], "side"),                        # a scale
    ]
    model("chem_lab", {"parent": "minecraft:block/block", "render_type": "minecraft:translucent",
                       "textures": {"particle": NS + ":block/chem_lab_side", "top": NS + ":block/chem_lab_top", "side": NS + ":block/chem_lab_side",
                                    "glass": NS + ":block/chem_lab_glass", "fluid": NS + ":block/chem_lab_fluid"},
                       "elements": elements}, "block")
    blockstate("chem_lab", {"variants": {"": {"model": NS + ":block/chem_lab"}}})
    model("chem_lab", {"parent": NS + ":block/chem_lab"})


# ---------------- Worn ----------------
def armor_layer(paint):
    px = [[(0, 0, 0, 0) for _ in range(64)] for _ in range(32)]

    def box(u, v, w, h, d, fn):
        faces = {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
                 "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}
        for face, (fx, fy, fw, fh) in faces.items():
            for y in range(fh):
                for x in range(fw):
                    c = fn(face, x, y, fw, fh)
                    if c is not None:
                        px[fy + y][fx + x] = c
    paint(box)
    return px


def carrier_layer():
    c = (62, 70, 50)

    def body(face, x, y, w, h):
        if face in ("top", "bottom"):
            return None
        if face in ("front", "back"):
            if y > 9:
                return None
            f = (-0.25 if y in (4, 7) else 0) + (0.12 if x in (0, w - 1) else 0)
            if face == "front" and 2 <= y <= 3 and 1 <= x <= 6:
                f -= 0.3                                       # the mag pouches
            return shade(c, f + (0.05 if (x * 3 + y) % 4 == 0 else 0))
        return shade(c, -0.15) if 2 <= y <= 8 else None        # cummerbund sides

    def arms(face, x, y, w, h):
        return shade(c, -0.05) if face == "top" or (face != "bottom" and y < 1) else None
    return armor_layer(lambda box: (box(16, 16, 8, 12, 4, body), box(40, 16, 4, 12, 4, arms)))


def nods_layer(down):
    c, t = (52, 56, 50), (60, 64, 58)

    def head(face, x, y, w, h):
        # A strap round the head, the mount at the front and, down, the tubes over the eyes
        if face == "top":
            return shade(c, -0.1) if 3 <= x <= 4 or 3 <= y <= 4 else None
        if face == "bottom":
            return None
        if y in (1, 2):
            return shade(c, -0.05)
        if face == "front":
            if down and 3 <= y <= 5 and x in (1, 2, 5, 6):
                return (110, 220, 120, 255) if y == 4 and x in (1, 6) else shade(t, 0.1 if x in (1, 5) else -0.2)
            if not down and y == 0 and x in (1, 2, 5, 6):
                return shade(t, 0)
            if 3 <= x <= 4 and y <= 3:
                return shade((80, 84, 78), 0)
        return None
    return armor_layer(lambda box: box(0, 0, 8, 8, 8, head))


# ---------------- Effects ----------------
def icon(draw):
    px = [[(0, 0, 0, 0) for _ in range(18)] for _ in range(18)]
    draw(px)
    return px


def effects():
    out = {}
    out["flashed"] = icon(lambda p: [ell(p, 9, 9, 7, 7, (255, 250, 220), 0.3, -0.1), ell(p, 9, 9, 3, 3, (255, 255, 255), 0, 0)])

    def marked(p):
        for i in range(-6, 7):
            for w in (-1, 0, 1):
                if 0 <= 9 + i < 18 and 0 <= 9 + i + w < 18:
                    p[9 + i][9 + i + w] = shade((200, 30, 20), -0.1 * w)
                    p[9 + i][9 - i + w] = shade((200, 30, 20), -0.1 * w)
    out["marked"] = icon(marked)
    out["stoned"] = icon(lambda p: [ell(p, 9, 10, 6, 5, (106, 160, 64)), ell(p, 6, 6, 2.5, 2.5, (220, 220, 210), 0.1, -0.1), ell(p, 11, 4, 2, 2, (200, 200, 196))])
    out["wired"] = icon(lambda p: [ell(p, 9, 9, 7, 5, (240, 240, 248), 0.1, -0.2), ell(p, 9, 9, 3.5, 3.5, (20, 20, 24), 0, 0), ell(p, 8, 8, 1, 1, (255, 255, 255), 0, 0)])

    def tweak(p):
        for x in range(2, 16):
            y = 9 + (3 if x % 4 < 2 else -3) * (1 if x % 2 else 0.5)
            p[int(y)][x] = (150, 220, 245, 255)
            p[int(y) + 1][x] = (90, 170, 210, 255)
    out["tweaking"] = icon(tweak)
    out["crash"] = icon(lambda p: [ell(p, 9, 10, 6.5, 6.5, (90, 80, 72)), rect(p, 5, 9, 12, 9, (40, 36, 34, 255))])
    out["numb"] = icon(lambda p: [rect(p, 3, 7, 14, 10, (216, 200, 160, 255)), rect(p, 8, 4, 9, 13, (216, 200, 160, 255))])

    def trip(p):
        for y in range(18):
            for x in range(18):
                d = math.hypot(x - 8.5, y - 8.5)
                if d < 8:
                    h = (d * 0.6 + math.atan2(y - 8.5, x - 8.5)) % (2 * math.pi)
                    p[y][x] = (clamp(128 + 120 * math.sin(h)), clamp(128 + 120 * math.sin(h + 2.1)), clamp(128 + 120 * math.sin(h + 4.2)), 255)
    out["tripping"] = icon(trip)

    def psych(p):
        ell(p, 9, 9, 7, 8, (42, 58, 48), 0.1, -0.3)
        rect(p, 5, 7, 6, 8, (230, 230, 220, 255))
        rect(p, 11, 7, 12, 8, (230, 230, 220, 255))
        line(p, 6, 13, 12, 12, (20, 20, 20, 255))
    out["psychosis"] = icon(psych)
    for k, px in out.items():
        tex("mob_effect/" + k, px)
    return list(out)


def main():
    flashbang()
    nods("nods_green", (110, 230, 120))
    nods("nods_white", (200, 230, 255))
    carrier()
    for n, (c, s) in PLATES.items():
        plate(n, c, s)
    joint()
    baggie("cocaine", (244, 244, 240), True)
    baggie("methamphetamine", (200, 230, 240), False)
    syringe("morphine", (230, 220, 160))
    syringe("adrenaline_shot", (220, 60, 50))
    mushrooms()
    leaf("cannabis", (70, 140, 50), "cannabis")
    leaf("coca", (60, 130, 60), "coca")
    seeds("cannabis", (110, 100, 70))
    seeds("coca", (150, 60, 40))
    crop("cannabis", (70, 140, 50), (150, 170, 90))
    crop("coca", (60, 130, 60), (200, 50, 40))
    concrete()
    glass()
    door()
    trapdoor()
    chem_lab()
    tex("models/armor/plate_carrier_layer_1", carrier_layer())
    tex("models/armor/nods_down", nods_layer(True))
    tex("models/armor/nods_up", nods_layer(False))
    fx = effects()
    lang_path = os.path.join(A, "lang", "en_us.json")
    lang = json.load(open(lang_path))
    names = {
        "item": {"flashbang": "Flashbang", "nods_green": "Night Vision Goggles (Green)", "nods_white": "Night Vision Goggles (White Phosphor)",
                 "plate_carrier": "Plate Carrier", "polyethylene_plate": "Polyethylene Plate", "steel_plate": "Steel Plate",
                 "titanium_plate": "Titanium Plate", "ceramic_plate": "Ceramic Plate", "hybrid_plate": "Hybrid Plate",
                 "ascendite_plate": "Ascendite Plate", "joint": "Joint", "cocaine": "Cocaine", "methamphetamine": "Methamphetamine",
                 "morphine": "Morphine", "adrenaline_shot": "Adrenaline Shot", "magic_mushrooms": "Magic Mushrooms",
                 "cannabis_seeds": "Cannabis Seeds", "coca_seeds": "Coca Seeds", "cannabis_leaf": "Cannabis Leaf", "coca_leaf": "Coca Leaf"},
        "block": {"cannabis": "Cannabis", "coca": "Coca", "chem_lab": "Chem Lab", "ballistic_concrete": "Ballistic Concrete",
                  "ballistic_glass": "Ballistic Glass", "ballistic_glass_pane": "Ballistic Glass Pane", "steel_door": "Steel Door",
                  "steel_trapdoor": "Steel Trapdoor"},
        "effect": {"flashed": "Flashed", "marked": "Marked", "stoned": "Stoned", "wired": "Wired", "tweaking": "Tweaking",
                   "crash": "Comedown", "numb": "Numb", "tripping": "Tripping", "psychosis": "Psychosis"},
    }
    for kind, m in names.items():
        for k, v in m.items():
            lang["%s.%s.%s" % (kind, NS, k)] = v
    lang["entity.%s.flashbang" % NS] = "Flashbang"
    lang["entity.%s.operator_corpse" % NS] = "Body"
    lang["key.factoryascension.nods"] = "Night vision goggles (flip up/down)"
    lang["key.factoryascension.lean_left"] = lang.get("key.factoryascension.lean_left", "Lean left (while aiming)")
    lang["key.factoryascension.lean_right"] = lang.get("key.factoryascension.lean_right", "Lean right (while aiming)")
    with open(lang_path, "w") as f:
        json.dump(lang, f, indent=1)
        f.write("\n")
    print("v25 art: items, crops, raid blocks, chem lab, worn layers, %d effect icons" % len(fx))


if __name__ == "__main__":
    main()
