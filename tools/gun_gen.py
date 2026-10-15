#!/usr/bin/env python3
"""The shotgun (mod/.../item/ShotgunItem.java): a 3D item model built from ~40 parts, its textures, and the shell.

Pump shotgun along +x: rubber recoil pad, walnut stock dropping to the butt, wrist, blued receiver with a gold inlay
line, a violet gem and a dark ejection port, trigger guard with a brass trigger, octagonal barrel over a magazine tube,
a grooved walnut pump, a barrel band, a crowned muzzle with a dark bore, a brass front bead and a rear sight.
Element rotations stay at 0 / +-22.5 / +-45 (anything else makes Minecraft reject the whole model).
Run from the repo root (make_mod_assets.py runs it):  python3 tools/gun_gen.py
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))
import make_mod_assets as A  # noqa: E402
import make_textures as T  # noqa: E402

NS = "factoryascension"
TEX = os.path.join(A.ASSETS, "textures", "item", "gun")
N = 32


def clamp(v):
    return max(0, min(255, int(round(v))))


def rgb(c, f=0.0):
    """Shade a color: f > 0 lighter, f < 0 darker."""
    if f >= 0:
        return tuple(clamp(c[i] + (255 - c[i]) * f) for i in range(3)) + (255,)
    return tuple(clamp(c[i] * (1 + f)) for i in range(3)) + (255,)


def metal(base, seed, streak=0.06, light=False):
    """Blued steel: fine horizontal brushing, a soft highlight band near the top, darker edges."""
    rnd = random.Random(seed)
    rows = [rnd.uniform(-1, 1) for _ in range(N)]
    px = []
    for y in range(N):
        row = []
        for x in range(N):
            f = rows[y] * streak + rnd.uniform(-0.03, 0.03)
            band = math.exp(-((y - 9) / 4.0) ** 2) * (0.22 if light else 0.14)
            edge = -0.18 if y in (0, N - 1) else (-0.08 if y in (1, N - 2) else 0)
            row.append(rgb(base, f + band + edge))
        px.append(row)
    return px


def wood(base, seed, dark=False):
    """Walnut: long wavy grain along x, a few darker figure lines, a satin sheen."""
    rnd = random.Random(seed)
    phase = [rnd.uniform(0, 6.28) for _ in range(4)]
    px = []
    for y in range(N):
        row = []
        for x in range(N):
            g = math.sin(y * 0.9 + 1.6 * math.sin(x * 0.18 + phase[0]) + phase[1])
            fig = math.sin(y * 0.33 + 2.2 * math.sin(x * 0.11 + phase[2]))
            f = 0.10 * g - (0.16 if fig > 0.93 else 0) + rnd.uniform(-0.03, 0.03)
            f += 0.10 * math.exp(-((y - 8) / 5.0) ** 2)
            if dark:
                f -= 0.35
            row.append(rgb(base, f))
        px.append(row)
    return px


def rubber():
    px = []
    for y in range(N):
        px.append([rgb((34, 30, 30), (-0.25 if y % 4 == 0 else 0.0) + (0.05 if y % 4 == 1 else 0)) for x in range(N)])
    return px


def gold():
    rnd = random.Random(7)
    return [[rgb((232, 176, 58), 0.25 * math.exp(-((y - 8) / 6.0) ** 2) - 0.25 * (y / N) + rnd.uniform(-0.03, 0.03)) for x in range(N)] for y in range(N)]


def bore():
    px = []
    for y in range(N):
        row = []
        for x in range(N):
            d = math.hypot(x - 15.5, y - 15.5)
            if d < 9:
                row.append(rgb((8, 8, 10), -0.2 + 0.03 * d / 9))
            elif d < 11:
                row.append(rgb((70, 74, 84), 0.15))     # the crown's bright ring
            else:
                row.append(rgb((40, 42, 50), -0.1))
        px.append(row)
    return px


def port():
    px = metal((22, 22, 28), 31, 0.02)
    for y in range(N):
        for x in range(N):
            if 4 <= y <= 27 and 3 <= x <= 28:
                px[y][x] = rgb((6, 6, 8), 0.05 if y < 8 else 0)
            if 8 <= y <= 22 and 10 <= x <= 22:
                px[y][x] = rgb((190, 140, 60), -0.1 + 0.2 * (y < 12))   # a brass shell head showing in the port
    return px


def gem():
    px = []
    for y in range(N):
        row = []
        for x in range(N):
            d = abs(x - 15.5) + abs(y - 15.5)
            if d > 15:
                row.append(rgb((232, 176, 58), -0.1))
            else:
                row.append(rgb((170, 96, 255), 0.45 - d / 22 if y < 16 else -0.15 - d / 40))
        px.append(row)
    px[9][10] = px[9][11] = px[10][10] = (255, 255, 255, 255)
    return px


TEXTURES = {
    "metal": lambda: metal((44, 50, 64), 1),          # blued barrel
    "receiver": lambda: metal((56, 62, 78), 2, light=True),
    "dark": lambda: metal((24, 26, 32), 3, 0.03),
    "wood": lambda: wood((122, 70, 38), 4),
    "wood_dark": lambda: wood((122, 70, 38), 5, dark=True),
    "rubber": rubber, "gold": gold, "bore": bore, "port": port, "gem": gem,
}


def box(frm, to, tex, faces=None, rot=None, full=False):
    """A part with every face (or only `faces`) on texture `tex`, UVs covering the face's own size (max 16)."""
    out = {}
    sx, sy, sz = (to[i] - frm[i] for i in range(3))
    sizes = {"north": (sx, sy), "south": (sx, sy), "east": (sz, sy), "west": (sz, sy), "up": (sx, sz), "down": (sx, sz)}
    for f, (w, h) in sizes.items():
        if faces and f not in faces:
            continue
        w2, h2 = (16, 16) if full else (min(16, max(0.5, w * 1.2)), min(16, max(0.5, h * 2.5)))
        out[f] = {"uv": [0, 0, round(w2, 2), round(h2, 2)], "texture": "#" + tex}
    el = {"from": [round(v, 3) for v in frm], "to": [round(v, 3) for v in to], "faces": out}
    if rot:
        el["rotation"] = {"axis": rot[0], "angle": rot[1], "origin": rot[2]}
    return el


def octa(x0, x1, cy, cz, r, tex):
    """A round-ish rod along x: a square and the same square turned 45 degrees."""
    a = r * 0.92
    return [box((x0, cy - a, cz - a), (x1, cy + a, cz + a), tex),
            box((x0, cy - a, cz - a), (x1, cy + a, cz + a), tex, faces=["north", "south", "up", "down"], rot=("x", 45, [x0, cy, cz]))]


def model():
    Z0, Z1 = 7.0, 9.0          # body width
    els = []
    stock_rot = ("z", -22.5, [6.0, 9.4, 8])
    # Butt: a stock that tapers from the wrist to the butt and drops toward the shoulder (all turned together),
    # its comb on top, a rubber recoil pad at the end
    for i, (x0, x1) in enumerate(((3.2, 6.2), (1.2, 3.2), (-0.8, 1.2), (-2.8, -0.8), (-4.6, -2.8))):
        els.append(box((x0, 6.6 - 0.36 * i, 7.2 - 0.03 * i), (x1, 9.4, 8.8 + 0.03 * i), "wood", rot=stock_rot))
    els.append(box((-4.6, 9.4, 7.45), (3.0, 9.75, 8.55), "wood", faces=["up", "north", "south", "west"], rot=stock_rot))
    els.append(box((-5.4, 4.9, 7.05), (-4.6, 9.75, 8.95), "rubber", rot=stock_rot))
    # Wrist (grip) and its checkering band
    els.append(box((5.4, 6.8, 7.25), (8.4, 9.6, 8.75), "wood"))
    els.append(box((5.9, 7.1, 7.2), (7.7, 8.3, 8.8), "wood_dark", faces=["north", "south"]))
    # Receiver
    els.append(box((8.0, 7.0, Z0 - 0.1), (15.0, 10.6, Z1 + 0.1), "receiver"))
    els.append(box((8.4, 10.6, 7.55), (14.8, 10.9, 8.45), "dark"))                         # top rail
    els.append(box((11.0, 8.7, Z1 + 0.1), (13.2, 9.7, Z1 + 0.16), "port", faces=["south"], full=True))  # ejection port
    els.append(box((8.3, 7.55, Z0 - 0.16), (14.7, 7.8, Z1 + 0.16), "gold", faces=["north", "south"]))   # inlay line
    els.append(box((9.0, 8.5, Z0 - 0.18), (10.2, 9.7, Z1 + 0.18), "gem", faces=["north", "south"], full=True))     # violet gem
    els.append(box((10.0, 6.75, 7.5), (14.0, 7.0, 8.5), "dark"))                            # loading port
    # Trigger guard and trigger
    els.append(box((8.6, 5.2, 7.7), (12.2, 5.7, 8.3), "dark"))
    els.append(box((11.7, 5.2, 7.7), (12.2, 7.0, 8.3), "dark"))
    els.append(box((8.6, 5.2, 7.7), (9.1, 7.0, 8.3), "dark"))
    els.append(box((10.0, 5.9, 7.8), (10.5, 7.0, 8.2), "gold", rot=("z", 22.5, [10.25, 6.4, 8])))
    # Barrel (octagonal) and magazine tube
    els += octa(15.0, 30.0, 9.75, 8.0, 0.85, "metal")
    els += octa(15.0, 27.0, 8.1, 8.0, 0.7, "metal")
    # Pump: walnut forend with four grooves
    els.append(box((17.0, 7.0, 6.85), (24.0, 9.2, 9.15), "wood"))
    for gx in (18.2, 19.6, 21.0, 22.4):
        els.append(box((gx, 6.95, 6.8), (gx + 0.35, 9.25, 9.2), "wood_dark"))
    els.append(box((17.0, 9.2, 7.3), (24.0, 9.45, 8.7), "wood", faces=["up", "north", "south"]))
    # Barrel band, muzzle crown and bore, sights
    els.append(box((26.2, 7.25, 7.05), (27.2, 10.75, 8.95), "dark"))
    els.append(box((29.4, 8.8, 7.1), (30.4, 10.7, 8.9), "dark"))
    els.append(box((30.4, 8.85, 7.15), (30.45, 10.65, 8.85), "bore", faces=["east"], full=True))
    els.append(box((28.7, 10.55, 7.75), (29.3, 11.15, 8.25), "gold"))                        # front bead
    els.append(box((13.4, 10.9, 7.5), (14.3, 11.6, 7.75), "dark"))                           # rear sight posts
    els.append(box((13.4, 10.9, 8.25), (14.3, 11.6, 8.5), "dark"))
    # Sling swivels
    els.append(box((25.0, 6.9, 7.9), (25.5, 7.3, 8.1), "dark"))
    els.append(box((-1.0, 5.55, 7.9), (-0.5, 5.95, 8.1), "dark", rot=stock_rot))
    t = NS + ":item/gun/"
    return {
        "textures": dict({k: t + k for k in TEXTURES}, particle=t + "metal"),
        "elements": els,
        "gui_light": "front",
        "display": {
            "gui": {"rotation": [0, 0, 35], "translation": [-1.7, -1.2, 0], "scale": [0.42, 0.42, 0.42]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.35, 0.35, 0.35]},
            "fixed": {"rotation": [0, 0, 35], "translation": [-1.7, -1.2, 0], "scale": [0.45, 0.45, 0.45]},
            # First person: the model's +x (muzzle) turned to the look direction, grip in the hand (client/GunClient
            # moves it to the eye while aiming)
            "firstperson_righthand": {"rotation": [0, 90, 0], "translation": [0, 2.0, 0], "scale": [0.7, 0.7, 0.7]},
            "firstperson_lefthand": {"rotation": [0, 90, 0], "translation": [0, 2.0, 0], "scale": [0.7, 0.7, 0.7]},
            # Third person, in the two-handed crossbow pose: worked out from vanilla's crossbow transform
            # ([-90, 0, -60] on a sprite drawn at 45 degrees), so the muzzle points down the arms and the top faces up
            "thirdperson_righthand": {"rotation": [-90, 0, -15], "translation": [0, 0.5, -1.5], "scale": [0.6, 0.6, 0.6]},
            "thirdperson_lefthand": {"rotation": [-90, 0, -15], "translation": [0, 0.5, -1.5], "scale": [0.6, 0.6, 0.6]},
            "head": {"rotation": [0, 90, 0], "translation": [0, 8, 0], "scale": [0.6, 0.6, 0.6]},
        },
    }


def shell():
    """The shell item: a red hull with a brass head, standing slightly tilted."""
    px = T.blank()
    for y in range(2, 14):
        for x in range(6, 10):
            if y >= 11:
                px[y][x] = rgb((214, 168, 70), 0.25 if x == 6 else (-0.2 if x == 9 else 0))   # brass head
            else:
                px[y][x] = rgb((178, 36, 36), 0.25 if x == 6 else (-0.25 if x == 9 else 0))  # red hull
    for x in range(5, 11):
        px[13][x] = rgb((180, 136, 52), -0.15)        # rim
    for x in range(6, 10):
        px[2][x] = rgb((120, 20, 20), 0)              # crimp
    px[4][7] = (255, 255, 255, 255)
    return T.hd_item(px, (178, 36, 36, 255), "shotgun_shell")


def main():
    import json
    os.makedirs(TEX, exist_ok=True)
    for name, fn in TEXTURES.items():
        T.png(os.path.join(TEX, name + ".png"), fn())
    m = model()
    bad = [e for e in m["elements"] if "rotation" in e and e["rotation"]["angle"] not in (0, 22.5, -22.5, 45, -45)]
    assert not bad, "rotations Minecraft rejects"
    out = [e for e in m["elements"] for v in e["from"] + e["to"] if not -16 <= v <= 32]
    assert not out, "parts outside -16..32"
    A.write_json(os.path.join(A.ASSETS, "models", "item", "shotgun.json"), m)
    T.png(os.path.join(A.ASSETS, "textures", "item", "shotgun_shell.png"), shell())
    A.write_json(os.path.join(A.ASSETS, "models", "item", "shotgun_shell.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": NS + ":item/shotgun_shell"}})
    lang_path = os.path.join(A.ASSETS, "lang", "en_us.json")
    lang = json.load(open(lang_path))
    lang.update({"item.factoryascension.shotgun": "Ascension Shotgun", "item.factoryascension.shotgun_shell": "Shotgun Shell",
                 "key.factoryascension.reload": "Reload gun", "key.categories.factoryascension": "Factory Ascension"})
    A.write_json(lang_path, lang)
    print("wrote the shotgun: %d parts, %d textures, and the shell" % (len(m["elements"]), len(TEXTURES)))


if __name__ == "__main__":
    main()
