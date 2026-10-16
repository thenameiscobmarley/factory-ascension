#!/usr/bin/env python3
"""The Tarkov inventory's items: the flare gun (3D, like the shotgun), red and green flares, and the duffel bag you hold
when the inventory opens (closed, and with its zip open: custom_model_data 1).

Element rotations stay at 0 / +-22.5 / +-45 (anything else makes Minecraft reject the whole model).
Run from the repo root (make_mod_assets.py runs it):  python3 tools/tarkov_gen.py
"""
import json
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))
import make_mod_assets as A  # noqa: E402
import make_textures as T  # noqa: E402
import gun_gen as G  # noqa: E402

NS = "factoryascension"
TEX = os.path.join(A.ASSETS, "textures", "item", "tarkov")
N = 32
rgb = G.rgb


def plastic(base, seed):
    """Moulded orange plastic: a soft top highlight, fine speckle, darker edges."""
    rnd = random.Random(seed)
    px = []
    for y in range(N):
        row = []
        for x in range(N):
            f = 0.18 * math.exp(-((y - 8) / 5.0) ** 2) - 0.12 * (y / N) + rnd.uniform(-0.025, 0.025)
            f += -0.14 if y in (0, N - 1) or x in (0, N - 1) else 0
            row.append(rgb(base, f))
        px.append(row)
    return px


def canvas(base, seed):
    """Olive canvas: a tight weave, a few darker wear patches."""
    rnd = random.Random(seed)
    patches = [(rnd.uniform(0, N), rnd.uniform(0, N), rnd.uniform(3, 7)) for _ in range(3)]
    px = []
    for y in range(N):
        row = []
        for x in range(N):
            weave = 0.06 if (x + y) % 2 == 0 else -0.04
            weave += 0.04 if y % 4 == 0 else 0
            wear = sum(-0.08 * math.exp(-((x - a) ** 2 + (y - b) ** 2) / (r * r)) for a, b, r in patches)
            row.append(rgb(base, weave + wear + rnd.uniform(-0.03, 0.03)))
        px.append(row)
    return px


def webbing():
    """Strap webbing: dark olive with lengthwise ribs and stitched edges."""
    px = []
    for y in range(N):
        row = []
        for x in range(N):
            f = 0.08 if y % 3 == 0 else (-0.06 if y % 3 == 2 else 0)
            if y in (2, N - 3) and x % 4 < 2:
                f = 0.3                                   # stitching
            row.append(rgb((64, 70, 42), f))
        px.append(row)
    return px


def zipper():
    """Zip tape with brass teeth down the middle."""
    px = []
    for y in range(N):
        row = []
        for x in range(N):
            if 12 <= y <= 19:
                tooth = (x // 2) % 2 == (0 if y < 16 else 1)
                row.append(rgb((214, 168, 70), 0.2 if y in (12, 13) else -0.1) if tooth else rgb((52, 50, 40), 0))
            else:
                row.append(rgb((58, 56, 44), 0.05 if y % 2 else -0.03))
        px.append(row)
    return px


def lining():
    """Inside the bag: a dark olive lining in shadow, lighter toward the opening."""
    rnd = random.Random(9)
    return [[rgb((70, 78, 50), -0.45 + 0.25 * (y / N) + rnd.uniform(-0.03, 0.03)) for x in range(N)] for y in range(N)]


def tag():
    """A tan name patch."""
    px = []
    for y in range(N):
        row = []
        for x in range(N):
            edge = x in (0, 1, N - 2, N - 1) or y in (0, 1, N - 2, N - 1)
            row.append(rgb((176, 150, 104), -0.2 if edge else (0.05 if (x + y) % 3 else 0)))
        px.append(row)
    return px


TEXTURES = {
    "orange": lambda: plastic((232, 108, 28), 11),
    "orange_dark": lambda: plastic((176, 72, 18), 12),
    "grip": lambda: G.rubber(),
    "steel": lambda: G.metal((70, 74, 84), 13, light=True),
    "bore": G.bore,
    "canvas": lambda: canvas((104, 112, 70), 14),
    "canvas_dark": lambda: canvas((84, 92, 56), 15),
    "webbing": webbing, "zipper": zipper, "lining": lining, "tag": tag, "brass": G.gold,
}

box, octa = G.box, G.octa


def flare_gun():
    els = []
    # Grip: angled back, rubber panels on the sides
    grip = ("z", 22.5, [6.0, 7.0, 8])
    els.append(box((4.2, 1.5, 7.0), (7.4, 7.2, 9.0), "orange", rot=grip))
    els.append(box((4.6, 2.2, 6.9), (7.0, 6.4, 9.1), "grip", faces=["north", "south"], rot=grip))
    # Frame and the fat barrel (a 26.5 mm flare tube) with a hinge, a hammer and a trigger guard
    els.append(box((5.4, 6.4, 7.1), (11.4, 9.2, 8.9), "orange"))
    els += octa(9.0, 21.0, 9.4, 8.0, 1.75, "orange")
    els.append(box((20.0, 7.7, 6.3), (21.2, 11.1, 9.7), "orange_dark"))                    # muzzle ring
    els.append(box((21.2, 8.2, 6.8), (21.25, 10.6, 9.2), "bore", faces=["east"], full=True))
    els.append(box((10.4, 7.3, 6.15), (11.4, 8.3, 9.85), "steel"))                         # hinge pin
    els.append(box((4.6, 9.2, 7.6), (6.2, 10.6, 8.4), "steel", rot=("z", -22.5, [5.4, 9.2, 8])))   # hammer
    els.append(box((6.6, 4.4, 7.7), (10.2, 4.9, 8.3), "orange_dark"))                     # trigger guard
    els.append(box((9.7, 4.4, 7.7), (10.2, 6.4, 8.3), "orange_dark"))
    els.append(box((8.0, 5.0, 7.8), (8.5, 6.4, 8.2), "steel", rot=("z", 22.5, [8.25, 5.7, 8])))
    els.append(box((15.0, 11.15, 7.7), (16.0, 11.7, 8.3), "steel"))                       # sight
    t = NS + ":item/tarkov/"
    used = sorted({f["texture"][1:] for e in els for f in e["faces"].values()})
    return {
        "textures": dict({k: t + k for k in used}, particle=t + "orange"),
        "elements": els,
        "gui_light": "front",
        "display": {
            "gui": {"rotation": [0, 0, 0], "translation": [-1.0, 0.5, 0], "scale": [0.72, 0.72, 0.72]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.45, 0.45, 0.45]},
            "fixed": {"rotation": [0, 0, 0], "translation": [-1.0, 0.5, 0], "scale": [0.75, 0.75, 0.75]},
            "firstperson_righthand": {"rotation": [0, 90, 0], "translation": [0, 3.0, 2.0], "scale": [0.7, 0.7, 0.7]},
            "firstperson_lefthand": {"rotation": [0, 90, 0], "translation": [0, 3.0, 2.0], "scale": [0.7, 0.7, 0.7]},
            "thirdperson_righthand": {"rotation": [0, 90, 0], "translation": [0, 3.5, 1.5], "scale": [0.6, 0.6, 0.6]},
            "thirdperson_lefthand": {"rotation": [0, 90, 0], "translation": [0, 3.5, 1.5], "scale": [0.6, 0.6, 0.6]},
            "head": {"rotation": [0, 90, 0], "translation": [0, 8, 0], "scale": [0.6, 0.6, 0.6]},
        },
    }


def bag(open_):
    """The duffel bag, long along x: canvas body, darker end caps, two carry handles, a zip along the top (open: the two
    top flaps folded out and the dark lining showing), a name patch and a shoulder strap."""
    els = []
    X0, X1, Y0, Y1, Z0, Z1 = 1.0, 15.0, 3.0, 9.6, 4.6, 11.4
    # Body: the main block and a turned copy for the rounded long edges
    els.append(box((X0, Y0, Z0), (X1, Y1 - 1.2, Z1), "canvas", full=True))
    els.append(box((X0, Y0 + 0.6, Z0 + 1.2), (X1, Y1, Z1 - 1.2), "canvas", full=True))
    els.append(box((X0 + 0.01, Y0 + 0.2, 6.0), (X1 - 0.01, Y1 - 0.3, 10.0), "canvas", faces=["north", "south", "up", "down"],
                   rot=("x", 45, [8, (Y0 + Y1) / 2, 8]), full=True))
    # End caps (darker panels) with a pull loop each
    for x0, x1 in ((X0 - 0.3, X0 + 0.5), (X1 - 0.5, X1 + 0.3)):
        els.append(box((x0, Y0 + 0.4, Z0 + 0.4), (x1, Y1 - 0.6, Z1 - 0.4), "canvas_dark", full=True))
    els.append(box((X0 - 0.8, 5.6, 7.4), (X0 - 0.3, 7.0, 8.6), "webbing"))
    els.append(box((X1 + 0.3, 5.6, 7.4), (X1 + 0.8, 7.0, 8.6), "webbing"))
    # Straps around the body, and the carry handles over the top
    for sx in (4.0, 11.0):
        els.append(box((sx, Y0 - 0.1, Z0 - 0.15), (sx + 1.2, Y1 - 1.0, Z1 + 0.15), "webbing", faces=["north", "south", "down"]))
        els.append(box((sx, Y1 + 1.6, 6.6), (sx + 1.2, Y1 + 2.1, 9.4), "webbing"))              # handle grip
        els.append(box((sx, Y1 - 0.2, 6.6), (sx + 1.2, Y1 + 2.1, 7.1), "webbing"))
        els.append(box((sx, Y1 - 0.2, 8.9), (sx + 1.2, Y1 + 2.1, 9.4), "webbing"))
    # Name patch on the side
    els.append(box((6.2, 4.6, Z1 + 0.01), (9.8, 6.8, Z1 + 0.06), "tag", faces=["south"], full=True))
    if not open_:
        els.append(box((X0 + 0.8, Y1 - 0.02, 7.4), (X1 - 0.8, Y1 + 0.08, 8.6), "zipper", faces=["up"], full=True))
        els.append(box((X1 - 2.4, Y1, 8.3), (X1 - 1.6, Y1 + 0.3, 9.6), "brass"))                 # the zip pull
    else:
        # The top opens: the lining shows between the flaps, which fold out to each side
        els.append(box((X0 + 0.8, Y1 - 0.02, 6.4), (X1 - 0.8, Y1 + 0.04, 9.6), "lining", faces=["up"], full=True))
        els.append(box((X0 + 0.8, Y1 - 0.4, 3.2), (X1 - 0.8, Y1 + 0.2, 6.6), "canvas", rot=("x", -45, [8, Y1, 6.6]), full=True))
        els.append(box((X0 + 0.8, Y1 - 0.4, 9.4), (X1 - 0.8, Y1 + 0.2, 12.8), "canvas", rot=("x", 45, [8, Y1, 9.4]), full=True))
        els.append(box((X0 + 0.8, Y1 + 0.2, 6.2), (X1 - 0.8, Y1 + 0.35, 6.6), "zipper", faces=["up", "north"], rot=("x", -45, [8, Y1, 6.6])))
        els.append(box((X0 + 0.8, Y1 + 0.2, 9.4), (X1 - 0.8, Y1 + 0.35, 9.8), "zipper", faces=["up", "south"], rot=("x", 45, [8, Y1, 9.4])))
        els.append(box((X0 + 1.2, Y1 + 0.1, 5.2), (X0 + 2.0, Y1 + 0.4, 6.4), "brass"))
    t = NS + ":item/tarkov/"
    used = sorted({f["texture"][1:] for e in els for f in e["faces"].values()})
    m = {
        "textures": dict({k: t + k for k in used}, particle=t + "canvas"),
        "elements": els,
        "display": {
            "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.62, 0.62, 0.62]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.4, 0.4, 0.4]},
            "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.6, 0.6, 0.6]},
        },
    }
    if not open_:
        m["overrides"] = [{"predicate": {"custom_model_data": 1}, "model": NS + ":item/duffel_bag_open"}]
    return m


def flare(colour):
    """A fat flare cartridge: coloured hull, brass base, a white band with the colour's cap."""
    px = T.blank()
    for y in range(3, 14):
        for x in range(5, 11):
            if y >= 11:
                px[y][x] = rgb((214, 168, 70), 0.25 if x == 5 else (-0.2 if x == 10 else 0))
            elif y in (8, 9):
                px[y][x] = rgb((236, 232, 220), 0.1 if x == 5 else (-0.15 if x == 10 else 0))
            else:
                px[y][x] = rgb(colour, 0.25 if x == 5 else (-0.25 if x == 10 else 0))
    for x in range(4, 12):
        px[13][x] = rgb((180, 136, 52), -0.15)
    for x in range(6, 10):
        px[3][x] = rgb(colour, -0.35)
    px[5][6] = (255, 255, 255, 255)
    return px


def check(m, name):
    bad = [e for e in m["elements"] if "rotation" in e and e["rotation"]["angle"] not in (0, 22.5, -22.5, 45, -45)]
    assert not bad, name + ": rotations Minecraft rejects"
    out = [e for e in m["elements"] for v in e["from"] + e["to"] if not -16 <= v <= 32]
    assert not out, name + ": parts outside -16..32"


def main():
    os.makedirs(TEX, exist_ok=True)
    for name, fn in TEXTURES.items():
        T.png(os.path.join(TEX, name + ".png"), fn())
    models = {"flare_gun": flare_gun(), "duffel_bag": bag(False), "duffel_bag_open": bag(True)}
    for name, m in models.items():
        check(m, name)
        A.write_json(os.path.join(A.ASSETS, "models", "item", name + ".json"), m)
    for name, colour in (("flare_red", (214, 40, 30)), ("flare_green", (40, 190, 60))):
        T.png(os.path.join(A.ASSETS, "textures", "item", name + ".png"), T.hd_item(flare(colour), colour + (255,), name))
        A.write_json(os.path.join(A.ASSETS, "models", "item", name + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": NS + ":item/" + name}})
    lang_path = os.path.join(A.ASSETS, "lang", "en_us.json")
    lang = json.load(open(lang_path))
    lang.update({"item.factoryascension.flare_gun": "Flare Gun", "item.factoryascension.flare_red": "Red Flare",
                 "item.factoryascension.flare_green": "Green Flare (Airdrop)", "item.factoryascension.duffel_bag": "Duffel Bag"})
    A.write_json(lang_path, lang)
    print("wrote the flare gun (%d parts), the flares and the duffel bag (%d / %d parts)" % (
        len(models["flare_gun"]["elements"]), len(models["duffel_bag"]["elements"]), len(models["duffel_bag_open"]["elements"])))


if __name__ == "__main__":
    main()
