#!/usr/bin/env python3
"""Shaped 3D item models (real boxes and cards, not flat sprites with depth) for the pack's collector items:

  Foil Sheets       a foil card sealed in a clear, lightly tinted wrapper with crimped ends (the card shows through)
  Foil Cases        the cased foils (config foils, cased: true): a foil card standing in a tinted clear case
  Enchantment Pack  a thick foil pack, its top torn open with the cards peeking out
  Booster Box       a box with a lid

Writes textures to kubejs/assets/kubejs/textures/item/model/ and models to kubejs/assets/kubejs/models/item/.
Run from the repo root:  python3 tools/item_models.py
"""
import json
import math
import os
import re
import sys

sys.path.insert(0, os.path.dirname(__file__))
import make_textures  # noqa: E402
import tome_art  # noqa: E402

ROOT = os.path.join(os.path.dirname(__file__), "..")
CONFIG = os.path.join(ROOT, "kubejs", "startup_scripts", "00_fa_config.js")
TEX = os.path.join(ROOT, "kubejs", "assets", "kubejs", "textures", "item", "model")
MODELS = os.path.join(ROOT, "kubejs", "assets", "kubejs", "models", "item")
S = 32

# How the models sit in hands, on the ground, in frames and in the inventory (turned a little so the depth shows)
DISPLAY = {
    "gui": {"rotation": [18, -28, 0], "translation": [0, 0, 0], "scale": [0.95, 0.95, 0.95]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
    "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
}


def foils():
    """[(id, cased, art)] in config order."""
    out = []
    for m in re.finditer(r"\{ id: '(\w+)', name: '[^']*', mult: [\d.]+[^\n]*", open(CONFIG).read()):
        line = m.group(0)
        art = re.search(r"art: \[\[([\d, ]+)\], \[([\d, ]+)\]\]", line)
        out.append((m.group(1), "cased: true" in line,
                    art and (tuple(int(v) for v in art.group(1).split(',')), tuple(int(v) for v in art.group(2).split(',')))))
    if len(out) < 8:
        raise SystemExit("could not read foils from " + CONFIG)
    return out


def blank(a=0):
    return [[(0, 0, 0, a)] * S for _ in range(S)]


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def card_face(foil, art):
    """A whole card face in the foil's finish: the tome cover overlay stretched over a pearl-white card."""
    over = tome_art.foil_overlay(foil, art)
    fx0, fx1, fy0, fy1 = tome_art.SPINE + 1, tome_art.X1, tome_art.Y0, tome_art.Y1
    px = blank()
    tints = {'bronze': (200, 130, 80), 'silver': (220, 226, 236), 'gold': (240, 200, 90)}
    for y in range(S):
        for x in range(S):
            ox = fx0 + int(x / float(S) * (fx1 - fx0 + 1))
            oy = fy0 + int(y / float(S) * (fy1 - fy0 + 1))
            r, g, b, a = over[oy][ox]
            base = (226, 230, 240)
            if a == 0 and foil in tints:
                c = mix(base, tints[foil], 0.7)
            else:
                c = mix(base, (r, g, b), a / 255.0)
            if x in (0, S - 1) or y in (0, S - 1):
                c = mix(c, (40, 40, 60), 0.5)
            px[y][x] = c + (255,)
    return px


def card_back():
    """The back of a tome card: dark violet with a gold diamond."""
    px = blank()
    for y in range(S):
        for x in range(S):
            c = mix((70, 40, 110), (30, 16, 52), (x + y) / 62.0)
            if x in (0, S - 1) or y in (0, S - 1):
                c = (200, 160, 60)
            elif abs(x - 15.5) + abs(y - 15.5) < 7:
                c = (232, 176, 58) if abs(x - 15.5) + abs(y - 15.5) > 4.5 else (255, 236, 150)
            px[y][x] = c + (255,)
    return px


def film(tint, alpha=46, edge=150):
    """Clear wrapper film: barely tinted so the card shows through, with glare streaks and a brighter edge."""
    px = blank()
    for y in range(S):
        for x in range(S):
            a, c = alpha, tint
            d = (x - y) % 22
            if d < 2 or 9 <= d < 10:
                a, c = 120, mix(tint, (255, 255, 255), 0.7)
            if x in (0, S - 1) or y in (0, S - 1):
                a, c = edge, mix(tint, (255, 255, 255), 0.4)
            px[y][x] = c + (a,)
    return px


def crimp(tint):
    """The sealed, crimped end of a wrapper: silver zigzag ridges."""
    px = blank()
    for y in range(S):
        for x in range(S):
            ridge = (x + (y // 4) * 2) % 4 < 2
            c = mix((200, 204, 216), tint, 0.25)
            px[y][x] = (mix(c, (255, 255, 255), 0.35) if ridge else mix(c, (90, 90, 110), 0.3)) + (220,)
    return px


def case_glass(main, deep):
    """Tinted clear case wall: colored glass, a bright beveled rim and a diagonal shine."""
    px = blank()
    for y in range(S):
        for x in range(S):
            edge = min(x, y, S - 1 - x, S - 1 - y)
            if edge == 0:
                px[y][x] = mix(main, (255, 255, 255), 0.45) + (235,)
            elif edge == 1:
                px[y][x] = mix(main, deep, 0.5) + (200,)
            else:
                shine = abs((x - y) - 6) < 2 or abs((x - y) + 10) < 1
                px[y][x] = (mix(main, (255, 255, 255), 0.75) + (130,)) if shine else (mix(main, deep, 0.25) + (70,))
    return px


def case_base(main, deep):
    """The case's solid stand: dark with the foil's color as a gold-trimmed band."""
    px = blank()
    for y in range(S):
        for x in range(S):
            c = mix(deep, (16, 14, 20), 0.5)
            if 10 <= y <= 21:
                c = mix(main, deep, 0.35 + 0.3 * (y - 10) / 11.0)
            if y in (9, 22) or x in (0, S - 1) or y in (0, S - 1):
                c = (232, 176, 58)
            px[y][x] = c + (255,)
    return px


def pack_face():
    """Enchantment Pack front: holographic violet foil with a gold star emblem and a teal stripe."""
    px = blank()
    for y in range(S):
        for x in range(S):
            t = (x + y) / 62.0
            c = mix((120, 70, 200), (40, 20, 90), t)
            holo = (math.sin((x - y) / 3.0) + 1) / 2
            c = mix(c, (150, 220, 255), 0.18 * holo)
            if 20 <= y <= 23:
                c = mix((64, 232, 212), (16, 150, 150), (y - 20) / 3.0)
            px[y][x] = c + (255,)
    star = tome_art.raster(tome_art.add(tome_art.poly(tome_art.star_pts(0.5, 0.36, 0.26, 0.11, 5))), S)
    for y in range(S):
        for x in range(S):
            if star[y][x]:
                px[y][x] = mix((255, 244, 180), (220, 150, 40), (y - 3) / 16.0) + (255,)
    for x in range(S):
        px[S - 1][x] = (40, 20, 70, 255)
    return px


def pack_side():
    """Pack edges: darker foil with a glued seam."""
    px = blank()
    for y in range(S):
        for x in range(S):
            c = mix((90, 50, 160), (34, 16, 70), y / 31.0)
            if 14 <= x <= 17:
                c = mix(c, (20, 10, 40), 0.4)
            px[y][x] = c + (255,)
    return px


def pack_inside():
    """Inside the torn-open pack: shadow."""
    return [[(24, 14, 40, 255)] * S for _ in range(S)]


def pack_flap():
    """The torn-off strip: crimped foil with a jagged torn edge (the ragged edge is see-through)."""
    px = crimp((150, 90, 230))
    for x in range(S):
        tear = 24 + int(3 * math.sin(x * 1.7) + 2 * math.sin(x * 0.6))
        for y in range(tear, S):
            px[y][x] = (0, 0, 0, 0)
        if 0 <= tear - 1 < S:
            px[tear - 1][x] = (240, 236, 255, 255)
    return px


def box_face():
    """Booster Box side: deep violet with a foil band and a gold star."""
    px = pack_face()
    for y in range(S):
        for x in range(S):
            if x in (0, S - 1) or y in (0, S - 1):
                px[y][x] = (200, 160, 60, 255)
    return px


def box_lid():
    px = blank()
    for y in range(S):
        for x in range(S):
            c = mix((70, 40, 110), (40, 20, 70), (x + y) / 62.0)
            if x in (0, 1, S - 2, S - 1) or y in (0, 1, S - 2, S - 1):
                c = (232, 176, 58)
            px[y][x] = c + (255,)
    return px


# ---- Models ----

def face(tex, uv=(0, 0, 16, 16), rot=0):
    f = {"texture": "#" + tex, "uv": list(uv)}
    if rot:
        f["rotation"] = rot
    return f


def box(frm, to, tex, faces="nsewud", uv=None, rotation=None, name=None):
    """A cuboid; tex is a texture key for every face, or {face letter: key}."""
    names = {"n": "north", "s": "south", "e": "east", "w": "west", "u": "up", "d": "down"}
    el = {"from": list(frm), "to": list(to), "faces": {}}
    for k in faces:
        t = tex[k] if isinstance(tex, dict) else tex
        el["faces"][names[k]] = face(t, (uv or {}).get(k, (0, 0, 16, 16)))
    if rotation:
        el["rotation"] = rotation
    if name:
        el["name"] = name
    return el


def model(textures, elements, translucent=True):
    m = {"textures": textures, "elements": elements, "display": DISPLAY, "gui_light": "front"}
    if translucent:
        m["render_type"] = "minecraft:translucent"
    return m


def wrapper_model(fid):
    t = "kubejs:item/model/"
    return model({"particle": "kubejs:item/foil_sheet_" + fid, "card": t + "card_" + fid, "back": t + "card_back",
                  "film": t + "film", "crimp": t + "crimp_" + fid}, [
        # The card, then the wrapper around it with crimped ends top and bottom
        box((3.5, 2.5, 7.75), (12.5, 13.5, 8.25), {"n": "back", "s": "card", "e": "back", "w": "back", "u": "back", "d": "back"}, name="card"),
        box((3, 2, 7.25), (13, 14, 8.75), "film", name="wrapper"),
        box((2.75, 14, 7.5), (13.25, 15.5, 8.5), "crimp", name="crimp_top"),
        box((2.75, 0.5, 7.5), (13.25, 2, 8.5), "crimp", name="crimp_bottom"),
    ])


def case_model(fid):
    t = "kubejs:item/model/"
    return model({"particle": "kubejs:item/foil_sheet_" + fid, "card": t + "card_" + fid, "back": t + "card_back",
                  "glass": t + "case_glass_" + fid, "base": t + "case_base_" + fid}, [
        box((2.5, 0.5, 5.5), (13.5, 3, 10.5), "base", name="stand"),
        box((4, 3.5, 7.6), (12, 13.5, 8.4), {"n": "back", "s": "card", "e": "back", "w": "back", "u": "back", "d": "back"}, name="card"),
        box((3, 3, 6), (13, 15, 10), "glass", name="case"),
    ])


def pack_model():
    t = "kubejs:item/model/"
    return model({"particle": "kubejs:item/enchantment_pack", "face": t + "pack_face", "side": t + "pack_side",
                  "inside": t + "pack_inside", "flap": t + "pack_flap", "back": t + "card_back", "card": t + "card_holo"}, [
        box((3, 0, 6), (13, 12.5, 10), {"n": "face", "s": "face", "e": "side", "w": "side", "u": "inside", "d": "side"}, name="body"),
        box((4, 10, 7), (12, 14.5, 7.4), {"n": "back", "s": "card", "e": "back", "w": "back", "u": "back", "d": "back"},
            rotation={"angle": -22.5, "axis": "x", "origin": [8, 10, 7]}, name="card_1"),
        box((4.5, 10, 8.2), (12.5, 13.5, 8.6), {"n": "back", "s": "back", "e": "back", "w": "back", "u": "back", "d": "back"},
            rotation={"angle": 22.5, "axis": "z", "origin": [8, 10, 8]}, name="card_2"),
        # The torn strip, still hanging on at the back and folded open
        box((3, 12.5, 9.6), (13, 15.5, 10), {"n": "flap", "s": "flap", "e": "flap", "w": "flap", "u": "flap", "d": "flap"},
            rotation={"angle": 45, "axis": "x", "origin": [8, 12.5, 10]}, name="torn_strip"),
    ])


def box_model():
    t = "kubejs:item/model/"
    return model({"particle": "kubejs:item/booster_box", "face": t + "box_face", "lid": t + "box_lid"}, [
        box((2, 0, 4), (14, 8, 12), "face", name="box"),
        box((1.75, 7, 3.75), (14.25, 10, 12.25), {"n": "lid", "s": "lid", "e": "lid", "w": "lid", "u": "lid", "d": "lid"}, name="lid"),
    ], translucent=False)


def check_model(path, m):
    """Minecraft rejects the whole model (purple and black in game) for a tilt that isn't 0, 22.5 or 45 degrees,
    or a part outside -16..32."""
    for e in m.get("elements", []):
        r = e.get("rotation")
        if r and r["angle"] not in (0, 22.5, -22.5, 45, -45):
            raise SystemExit("%s: rotation %s isn't allowed (only 0, +-22.5, +-45)" % (path, r["angle"]))
        if any(c < -16 or c > 32 for c in e["from"] + e["to"]):
            raise SystemExit("%s: a part goes outside -16..32" % path)


def write(path, obj):
    check_model(path, obj)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=1)
        f.write("\n")


def main():
    os.makedirs(TEX, exist_ok=True)
    png = lambda name, px: make_textures.png(os.path.join(TEX, name + ".png"), px)
    tints = {'bronze': (214, 140, 90), 'silver': (220, 230, 245), 'gold': (255, 214, 110), 'holo': (180, 230, 255),
             'prismatic': (240, 200, 255), 'galaxy': (150, 110, 230), 'void': (150, 90, 200), 'celestial': (255, 240, 190)}
    png("card_back", card_back())
    png("film", film((220, 236, 255)))
    png("card_holo", card_face("holo", None))
    for name, draw in (("pack_face", pack_face), ("pack_side", pack_side), ("pack_inside", pack_inside), ("pack_flap", pack_flap),
                       ("box_face", box_face), ("box_lid", box_lid)):
        png(name, draw())
    n = 0
    for fid, cased, art in foils():
        png("card_" + fid, card_face(fid, art))
        if cased:
            png("case_glass_" + fid, case_glass(*art))
            png("case_base_" + fid, case_base(*art))
            write(os.path.join(MODELS, "foil_sheet_" + fid + ".json"), case_model(fid))
        else:
            png("crimp_" + fid, crimp(tints.get(fid, (220, 220, 230))))
            write(os.path.join(MODELS, "foil_sheet_" + fid + ".json"), wrapper_model(fid))
        n += 1
    write(os.path.join(MODELS, "enchantment_pack.json"), pack_model())
    write(os.path.join(MODELS, "booster_box.json"), box_model())
    print("wrote 3D models for %d foils, the Enchantment Pack and the Booster Box" % n)


if __name__ == "__main__":
    main()
