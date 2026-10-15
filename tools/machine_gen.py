#!/usr/bin/env python3
"""Textures, models, block states and loot for the machines in mod/.../machine: Ascension Quarry, Mob Grinder, Warp
Hopper. Same style as the other machines (gunmetal plates, gold trim, teal channels, violet gems).
Run from the repo root (make_mod_assets.py runs it):  python3 tools/machine_gen.py
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import make_mod_assets as A  # noqa: E402
import make_textures  # noqa: E402

NS = "factoryascension"
S = A.S


def tex(name, px):
    make_textures.png(os.path.join(A.ASSETS, "textures", "block", name + ".png"), px)


def quarry_front():
    px = A.side()
    A.plate(px, 6, 6, 20, 20, "steel_dk")
    # A drill bit seen head-on: a spiral of gold
    for y in range(7, 25):
        for x in range(7, 25):
            a = math.atan2(y - 15.5, x - 15.5)
            d = math.hypot(x - 15.5, y - 15.5)
            if d < 9 and (math.sin(a * 2 + d * 0.9) > 0.3):
                px[y][x] = A._shade("gold", 0.2 + d / 20) + (255,)
    A.gem(px, 16, 16, 1)
    return px


def quarry_bottom():
    px = A.canvas()
    A.plate(px, 0, 0, S, S, "steel_dk")
    for y in range(S):
        for x in range(S):
            if (x + y) % 8 < 2 and 4 < x < 27 and 4 < y < 27:
                px[y][x] = A._shade("iron", 0.3) + (255,)
    A.frame(px, "gold")
    return px


def grinder_front():
    px = A.side()
    A.plate(px, 4, 4, 24, 24, "steel_dk")
    # Teeth: rows of iron triangles
    for row in range(4):
        y0 = 6 + row * 6
        for x in range(5, 27):
            h = 4 - abs((x % 6) - 3)
            for y in range(y0, y0 + max(0, h)):
                px[y][x] = A._shade("iron" if row % 2 else "steel", 0.15 + 0.1 * (y - y0)) + (255,)
    for x in range(4, 28):
        px[27][x] = A._shade("fire", 0.4) + (255,)
    return px


def warp_side():
    px = A.side()
    A.plate(px, 6, 6, 20, 20, "steel_dk")
    for y in range(7, 25):
        for x in range(7, 25):
            d = math.hypot(x - 15.5, y - 15.5)
            if d < 9:
                t = (math.sin(d * 1.3) + 1) / 2
                px[y][x] = A._mix(A._shade("gem", 0.2 + 0.4 * t), A._shade("teal", 0.2), 0.5 * t) + (255,)
    return px


def warp_top():
    px = A.canvas()
    A.plate(px, 0, 0, S, S, "steel")
    A.frame(px, "gold")
    for y in range(8, 24):
        for x in range(8, 24):
            px[y][x] = (10, 6, 18, 255)
    for y in range(10, 22):
        for x in range(10, 22):
            d = math.hypot(x - 15.5, y - 15.5)
            px[y][x] = A._shade("teal", 0.1 + d / 10) + (255,) if d < 6 else px[y][x]
    return px


MACHINES = {
    # id: (name, textures {face: png name}, facing?)
    "ascension_quarry": ("Ascension Quarry", {"front": "quarry_front", "side": "machine_side_plain", "top": "machine_top_plain", "bottom": "quarry_bottom"}, True),
    "mob_grinder": ("Mob Grinder", {"front": "grinder_front", "side": "machine_side_plain", "top": "machine_top_plain", "bottom": "machine_top_plain"}, True),
    "warp_hopper": ("Warp Hopper", {"side": "warp_hopper_side", "top": "warp_hopper_top", "bottom": "machine_top_plain"}, False),
}


def main():
    import json
    tex("quarry_front", quarry_front())
    tex("quarry_bottom", quarry_bottom())
    tex("grinder_front", grinder_front())
    tex("warp_hopper_side", warp_side())
    tex("warp_hopper_top", warp_top())
    tex("machine_side_plain", A.side())
    tex("machine_top_plain", A.top())
    lang_path = os.path.join(A.ASSETS, "lang", "en_us.json")
    lang = json.load(open(lang_path))
    t = NS + ":block/"
    for b, (name, faces, facing) in MACHINES.items():
        if facing:
            model = {"parent": "minecraft:block/orientable_with_bottom", "textures": {
                "front": t + faces["front"], "side": t + faces["side"], "top": t + faces["top"], "bottom": t + faces["bottom"]}}
            states = {"variants": {"facing=%s" % f: ({"model": t + b, "y": r} if r else {"model": t + b})
                                   for f, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}}
        else:
            model = {"parent": "minecraft:block/cube_bottom_top", "textures": {"side": t + faces["side"], "top": t + faces["top"], "bottom": t + faces["bottom"]}}
            states = {"variants": {"": {"model": t + b}}}
        A.write_json(os.path.join(A.ASSETS, "models", "block", b + ".json"), model)
        A.write_json(os.path.join(A.ASSETS, "models", "item", b + ".json"), {"parent": t + b})
        A.write_json(os.path.join(A.ASSETS, "blockstates", b + ".json"), states)
        A.write_json(os.path.join(A.DATA, "loot_tables", "blocks", b + ".json"), {"type": "minecraft:block", "pools": [
            {"rolls": 1, "entries": [{"type": "minecraft:item", "name": NS + ":" + b}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
        lang["block." + NS + "." + b] = name
    A.write_json(lang_path, lang)
    path = os.path.join(A.RES, "data", "minecraft", "tags", "blocks", "mineable", "pickaxe.json")
    cur = json.load(open(path))
    for b in MACHINES:
        if NS + ":" + b not in cur["values"]:
            cur["values"].append(NS + ":" + b)
    A.write_json(path, cur)
    print("wrote %d machines" % len(MACHINES))


if __name__ == "__main__":
    main()
