#!/usr/bin/env python3
"""Art for the armed operators (mod/.../npc/): a player-style skin for each of the 20 kinds (64x64, using the
second layer for helmets, plate carriers, pouches and hoods so they stand off the body), the items they carry (frag
grenade, bandage, medkit, dog tag), the Bleeding effect's icon, spawn egg models and the names.

  python3 tools/operator_art.py      (make_mod_assets.py runs it)
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
ASSETS = os.path.join(ROOT, "mod", "src", "main", "resources", "assets", NS)


def clamp(v):
    return max(0, min(255, int(round(v))))


def shade(c, f):
    if f >= 0:
        return tuple(clamp(c[i] + (255 - c[i]) * f) for i in range(3)) + (255,)
    return tuple(clamp(c[i] * (1 + f)) for i in range(3)) + (255,)


def hexc(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


class Skin:
    """A 64x64 skin with box painters (u, v of the box's net; w, h, d its size)."""

    def __init__(self, seed):
        self.px = [[(0, 0, 0, 0) for _ in range(64)] for _ in range(64)]
        self.rnd = random.Random(seed)

    def box(self, u, v, w, h, d, paint):
        faces = {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
                 "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}
        for face, (fx, fy, fw, fh) in faces.items():
            for y in range(fh):
                for x in range(fw):
                    c = paint(face, x, y, fw, fh)
                    if c is not None:
                        self.px[fy + y][fx + x] = c

    def save(self, path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        T.png(path, self.px)


# Box nets: (u, v, w, h, d)
HEAD, HAT = (0, 0, 8, 8, 8), (32, 0, 8, 8, 8)
BODY, JACKET = (16, 16, 8, 12, 4), (16, 32, 8, 12, 4)
RARM, RSLEEVE = (40, 16, 4, 12, 4), (40, 32, 4, 12, 4)
LARM, LSLEEVE = (32, 48, 4, 12, 4), (48, 48, 4, 12, 4)
RLEG, RPANTS = (0, 16, 4, 12, 4), (0, 32, 4, 12, 4)
LLEG, LPANTS = (16, 48, 4, 12, 4), (0, 48, 4, 12, 4)

TONES = [(232, 190, 160), (214, 168, 132), (176, 128, 96), (124, 86, 62), (96, 66, 48)]

# Per kind: clothes (top, bottom, accent), camo pattern, head (beanie, cap, balaclava, helmet, hood, bare, mask),
# vest (none, rig, plate), and extras
KINDS = {
    "scav": dict(top=0x4A5A8A, bottom=0x3A3A44, accent=0xE8E8E8, camo="tracksuit", head="beanie", vest="none"),
    "scav_rifleman": dict(top=0x5E6A44, bottom=0x4A4E3A, accent=0x8A6A3A, camo="woodland", head="cap", vest="rig"),
    "scav_sniper": dict(top=0x5A6A3E, bottom=0x4E5A36, accent=0x2E2E26, camo="ghillie", head="hood", vest="none"),
    "looter": dict(top=0x6A5E52, bottom=0x34343A, accent=0xC8C8C8, camo="plain", head="balaclava", vest="none"),
    "raider": dict(top=0x6A6E4A, bottom=0x5A5E40, accent=0x2A2A26, camo="multicam", head="helmet", vest="plate"),
    "raider_medic": dict(top=0x6A6E4A, bottom=0x5A5E40, accent=0xC02020, camo="multicam", head="helmet", vest="plate", medic=True),
    "breacher": dict(top=0x3A3E36, bottom=0x34362E, accent=0x6A6A6A, camo="plain", head="helmet", vest="plate", visor=True),
    "grenadier": dict(top=0x6A6E4A, bottom=0x5A5E40, accent=0xC08020, camo="multicam", head="helmet", vest="rig"),
    "marksman": dict(top=0x56603E, bottom=0x4A5236, accent=0x2A3A2A, camo="woodland", head="cap", vest="rig"),
    "machine_gunner": dict(top=0x4E4E40, bottom=0x44443A, accent=0x8A7A40, camo="multicam", head="helmet", vest="plate", belt=True),
    "rogue": dict(top=0x4A5054, bottom=0x3E4448, accent=0x7A8A90, camo="urban", head="helmet", vest="plate", nvg=True),
    "rogue_sniper": dict(top=0x4A5054, bottom=0x3E4448, accent=0x9AA2A6, camo="urban", head="cap", vest="rig", nvg=False),
    "cultist": dict(top=0x241E1A, bottom=0x1E1A16, accent=0x7A1A12, camo="robe", head="cult", vest="none"),
    "ghost": dict(top=0x2E3234, bottom=0x2A2E30, accent=0x4A5A4A, camo="plain", head="balaclava", vest="rig", nvg=True),
    "juggernaut": dict(top=0x2A2A2A, bottom=0x262626, accent=0x8A2A1A, camo="plain", head="jugg", vest="plate", heavy=True),
    "hunter": dict(top=0x5A4E3A, bottom=0x4A4232, accent=0xA08050, camo="woodland", head="helmet", vest="plate", nvg=True),
    "warlord": dict(top=0x2A2420, bottom=0x22201C, accent=0xD8B048, camo="plain", head="bare_beard", vest="none", gold=True),
    "bodyguard": dict(top=0x26262A, bottom=0x22222A, accent=0xC8A040, camo="plain", head="balaclava", vest="plate"),
    "smuggler": dict(top=0x6A5440, bottom=0x3A4A5A, accent=0x2A6A6A, camo="plain", head="cap", vest="none", bag=True),
    "deserter": dict(top=0x5E6844, bottom=0x52583C, accent=0x3A3A2A, camo="flora", head="beanie", vest="rig"),
}


def fabric(rnd, base, kind, x, y, face):
    """A clothing colour at a pixel: camo patterns, tracksuit stripes, a little weave noise."""
    n = rnd.uniform(-0.05, 0.05)
    if kind == "woodland":
        k = (math.sin(x * 1.3 + y * 0.7 + rnd.random() * 0.2) + math.sin(y * 0.9 - x * 0.4)) * 0.5
        c = shade(base, -0.25) if k > 0.55 else shade(base, 0.12) if k < -0.55 else shade(base, 0)
    elif kind == "multicam":
        k = math.sin(x * 0.9 + y * 0.5) * math.cos(y * 0.8 - x * 0.3)
        c = shade((150, 130, 90), -0.1) if k > 0.4 else shade(base, -0.2) if k < -0.4 else shade(base, 0.05)
    elif kind == "urban":
        k = math.sin(x * 1.1 + y * 1.3) + math.sin(y * 0.6)
        c = shade(base, 0.2) if k > 1.0 else shade(base, -0.3) if k < -1.0 else shade(base, 0)
    elif kind == "flora":
        k = math.sin(x * 2.1) * math.sin(y * 1.7 + x)
        c = shade((140, 120, 70), 0) if k > 0.6 else shade(base, -0.2) if k < -0.5 else shade(base, 0)
    elif kind == "ghillie":
        k = rnd.random()
        c = shade(base, -0.35) if k < 0.3 else shade((110, 100, 60), 0) if k > 0.8 else shade(base, 0)
    else:
        c = shade(base, 0)
    return shade(c[:3], n)


def make_skin(kind, spec, seed):
    s = Skin(seed)
    rnd = s.rnd
    top, bottom, accent = hexc(spec["top"]), hexc(spec["bottom"]), hexc(spec["accent"])
    tone = TONES[rnd.randrange(len(TONES))]
    camo = spec["camo"]
    head = spec["head"]

    # ---- Head ----
    def head_paint(face, x, y, w, h):
        skin = shade(tone, rnd.uniform(-0.04, 0.03))
        if face == "front":
            if y == 3 and x in (2, 5):
                return (240, 240, 240, 255) if head not in ("cult",) else (20, 16, 14, 255)
            if y == 3 and x in (1, 6) and head not in ("cult",):
                return shade((60, 46, 36), 0)
            if y == 2 and 1 <= x <= 6:
                return shade(tone, -0.25)          # brows
            if y == 5 and 2 <= x <= 5:
                return shade(tone, -0.3)           # mouth line
            if head == "bare_beard" and y >= 5:
                return shade((40, 30, 24), rnd.uniform(-0.1, 0.1))
            if head in ("beanie", "cap", "hood", "helmet") and y >= 6 and rnd.random() < 0.35:
                return shade(tone, -0.35)          # stubble
        if face == "top" or (face in ("back", "left", "right") and y < 3):
            return shade((54, 40, 30), rnd.uniform(-0.1, 0.1))   # hair
        if face == "back":
            return shade((54, 40, 30), rnd.uniform(-0.1, 0.1)) if y < 6 else skin
        return skin
    s.box(*HEAD, head_paint)

    # ---- Headgear (hat layer) ----
    def hat_paint(face, x, y, w, h):
        if head == "beanie":
            if face == "bottom":
                return None
            if face == "top" or y < 3:
                return shade(accent if kind == "deserter" else (40, 40, 44), rnd.uniform(-0.06, 0.06)) if y < 3 or face == "top" else None
            return None
        if head == "cap":
            if face == "top" or (face != "bottom" and y < 2):
                return fabric(rnd, top, camo, x, y, face)
            if face == "front" and y == 2:
                return shade(top, -0.3)         # the brim's shadow
            return None
        if head in ("helmet", "jugg"):
            col = (70, 74, 62) if head == "helmet" else (34, 34, 34)
            if kind in ("rogue", "hunter", "ghost"):
                col = (64, 68, 70) if kind != "hunter" else (110, 96, 70)
            if face == "bottom":
                return None
            if face == "top":
                return shade(col, rnd.uniform(-0.05, 0.05))
            if face == "front":
                if head == "jugg":
                    if 2 <= y <= 3 and 1 <= x <= 6:
                        return (20, 20, 22, 255) if (x + y) % 3 else (60, 60, 64, 255)   # visor slits
                    return shade(col, rnd.uniform(-0.05, 0.05))
                if spec.get("nvg") and y < 3 and 2 <= x <= 5:
                    return (24, 26, 22, 255) if y < 2 else (60, 200, 90, 255) if x in (3, 4) else (30, 32, 28, 255)
                if spec.get("visor") and 2 <= y <= 4:
                    return (40, 50, 60, 230)
                return shade(col, rnd.uniform(-0.05, 0.05)) if y < 2 else None
            if face in ("left", "right"):
                if 3 <= y <= 5 and 2 <= x <= 5:
                    return (30, 30, 30, 255)              # headset ear cup
                return shade(col, rnd.uniform(-0.05, 0.05)) if y < 3 or x > 4 else None
            if face == "back":
                return shade(col, rnd.uniform(-0.05, 0.05)) if y < 4 else ((40, 40, 38, 255) if y == 4 and 2 <= x <= 5 else None)
            return None
        if head == "balaclava":
            if face == "bottom":
                return None
            if face == "front" and 2 <= y <= 4 and 1 <= x <= 6:
                return None                               # the eye slot shows the face
            return shade((30, 30, 32) if kind != "bodyguard" else (20, 20, 22), rnd.uniform(-0.05, 0.05))
        if head == "hood":
            if face == "bottom" or (face == "front" and y >= 2 and 1 <= x <= 6):
                return None
            return fabric(rnd, top, camo, x, y, face)
        if head == "cult":
            if face == "bottom":
                return None
            if face == "front":
                if 1 <= x <= 6 and 1 <= y <= 6:
                    # A pale mask with dark eye holes
                    if y == 3 and x in (2, 5):
                        return (10, 8, 8, 255)
                    return shade((216, 208, 190), rnd.uniform(-0.05, 0.03))
                return shade((30, 24, 20), rnd.uniform(-0.05, 0.05))
            return shade((30, 24, 20), rnd.uniform(-0.05, 0.05))
        if head == "bare_beard":
            if face == "front" and y == 3 and 1 <= x <= 6:
                return (16, 16, 18, 255) if x != 4 else (40, 40, 44, 255)   # sunglasses
            return None
        return None
    s.box(*HAT, hat_paint)

    # ---- Body and arms, legs (clothes) ----
    def body_paint(face, x, y, w, h):
        c = fabric(rnd, top, camo, x, y, face)
        if camo == "tracksuit" and face in ("front", "back") and x in (0, w - 1):
            return shade(accent, 0)
        if camo == "robe" and face == "front" and x in (3, 4):
            return shade(accent, -0.2)
        if spec.get("gold") and face == "front" and (y == 2 or y == 3) and 1 <= x <= 6 and (x + y) % 2 == 0:
            return shade((220, 180, 70), 0.1)        # gold chain
        return c
    s.box(*BODY, body_paint)

    def arm_paint(face, x, y, w, h):
        if y >= 10:
            return shade((40, 40, 38), rnd.uniform(-0.05, 0.05)) if kind not in ("scav", "warlord") else shade(tone, 0)   # gloves / hands
        c = fabric(rnd, top, camo, x, y, face)
        if camo == "tracksuit" and face in ("right", "left") and x == 1:
            return shade(accent, 0)
        if spec.get("medic") and face in ("right", "left") and 2 <= y <= 4 and 1 <= x <= 2:
            return (200, 30, 30, 255) if (x == 1 or y == 3) else (240, 240, 240, 255)
        return c
    s.box(*RARM, arm_paint)
    s.box(*LARM, arm_paint)

    def leg_paint(face, x, y, w, h):
        if y >= 9:
            return shade((44, 34, 26) if kind not in ("rogue", "ghost", "bodyguard", "juggernaut") else (24, 24, 24), rnd.uniform(-0.06, 0.06))   # boots
        c = fabric(rnd, bottom, camo if camo not in ("tracksuit",) else "plain", x, y, face)
        if camo == "tracksuit" and face in ("right", "left") and x == 1:
            return shade(accent, 0)
        if face == "front" and 5 <= y <= 6 and spec["vest"] == "plate":
            return shade((40, 40, 38), 0)                # knee pads
        return c
    s.box(*RLEG, leg_paint)
    s.box(*LLEG, leg_paint)

    # ---- Overlays: vest (jacket layer), sleeves, pants ----
    vest = spec["vest"]

    def jacket_paint(face, x, y, w, h):
        if vest == "none":
            if spec.get("bag") and face == "back" and 2 <= y <= 9 and 1 <= x <= 6:
                return shade((90, 70, 50), rnd.uniform(-0.08, 0.05))   # a backpack
            if camo in ("ghillie",) and rnd.random() < 0.35 and face != "bottom":
                return shade((90, 96, 56), rnd.uniform(-0.2, 0.1))
            return None
        if face in ("top", "bottom"):
            return None
        col = hexc(0x3A3E30) if kind not in ("rogue", "ghost", "bodyguard", "juggernaut") else hexc(0x2A2C2E)
        if kind == "hunter":
            col = hexc(0x6A5A40)
        if vest == "plate":
            if face == "front":
                if 1 <= y <= 8:
                    if y >= 6 and x in (1, 2, 3, 5, 6) and x != 4:
                        return shade(col, -0.25 if (x % 2) else -0.1)          # mag pouches
                    if y == 1 and x in (0, 7):
                        return shade(col, -0.2)                                  # shoulder straps
                    return shade(col, rnd.uniform(-0.06, 0.06))
                if spec.get("belt") and y in (9, 10):
                    return shade((200, 160, 70), -0.1 if x % 2 else 0.1)          # an ammo belt across
                return None
            if face == "back":
                if 1 <= y <= 8:
                    if 2 <= y <= 5 and 5 <= x <= 6:
                        return (34, 36, 32, 255)                                  # radio
                    return shade(col, rnd.uniform(-0.06, 0.06))
                return None
            if face in ("left", "right"):
                return shade(col, -0.1) if 2 <= y <= 7 else None
        if vest == "rig":
            if face == "front" and 5 <= y <= 9:
                return shade(col, -0.2 if x % 2 else 0) if x not in (0, 7) else shade(col, -0.3)
            if face in ("front", "back") and x in (1, 6) and y < 5:
                return shade(col, -0.25)                                           # straps
            return None
        return None
    s.box(*JACKET, jacket_paint)

    def sleeve_paint(face, x, y, w, h):
        if spec.get("heavy") and y < 5 and face != "bottom":
            return shade((30, 30, 30), rnd.uniform(-0.05, 0.05))                 # shoulder armour
        if camo == "ghillie" and face != "bottom" and rnd.random() < 0.3:
            return shade((90, 96, 56), rnd.uniform(-0.2, 0.1))
        return None
    s.box(*RSLEEVE, sleeve_paint)
    s.box(*LSLEEVE, sleeve_paint)

    def pants_paint(face, x, y, w, h):
        if spec.get("heavy") and 2 <= y <= 8 and face != "bottom" and face != "top":
            return shade((34, 34, 34), rnd.uniform(-0.05, 0.05))                 # leg armour
        if vest != "none" and face in ("right", "left") and 1 <= y <= 4 and x <= 2 and kind != "cultist":
            return shade((40, 42, 36), -0.1)                                       # thigh pouch
        return None
    s.box(*RPANTS, pants_paint)
    s.box(*LPANTS, pants_paint)
    return s


# ---- Items ----
def grenade():
    px = T.blank()
    body = (74, 84, 56)
    for y in range(5, 15):
        for x in range(4, 12):
            dx, dy = x - 7.5, (y - 10) * 1.1
            if dx * dx + dy * dy <= 16:
                f = 0.25 if dx < -1.5 and dy < 0 else (-0.25 if dx > 2 or dy > 2.5 else 0)
                px[y][x] = shade(body, f + (0.06 if (x + y) % 3 == 0 else 0))
    for y in range(2, 6):
        for x in range(6, 10):
            px[y][x] = shade((150, 150, 150), 0.2 if x == 6 else -0.1)          # the spoon and top
    for x in range(9, 13):
        px[3][x] = shade((170, 170, 170), 0)
    px[1][11] = px[2][12] = px[1][12] = (200, 200, 200, 255)                 # the ring
    px[2][10] = (200, 200, 200, 255)
    return T.hd_item(px, (60, 70, 46, 255), "frag_grenade")


def bandage():
    px = T.blank()
    for y in range(4, 13):
        for x in range(3, 13):
            dx, dy = x - 7.5, y - 8.5
            if dx * dx / 22 + dy * dy / 18 <= 1:
                px[y][x] = shade((236, 232, 220), 0.1 if dx < 0 else -0.1 - (0.1 if (x + y) % 2 else 0))
    for y in range(6, 11):
        for x in range(6, 10):
            px[y][x] = shade((220, 214, 200), -0.25 if x in (6, 9) or y in (6, 10) else -0.05)   # the roll's core
    for x in range(10, 15):
        px[11][x] = shade((236, 232, 220), 0)                                                 # the loose end
    return T.hd_item(px, (170, 160, 140, 255), "bandage")


def medkit():
    px = T.blank()
    for y in range(4, 14):
        for x in range(2, 14):
            px[y][x] = shade((176, 32, 28), 0.15 if y < 6 else (-0.2 if y > 11 else 0))
    for y in range(6, 12):
        for x in range(7, 9):
            px[y][x] = (240, 240, 236, 255)
    for y in range(8, 10):
        for x in range(5, 11):
            px[y][x] = (240, 240, 236, 255)
    for x in range(6, 10):
        px[3][x] = shade((60, 60, 60), 0)                                                   # handle
    px[2][6] = px[2][9] = shade((60, 60, 60), 0)
    return T.hd_item(px, (110, 20, 18, 255), "medkit")


def dog_tag():
    px = T.blank()
    for y in range(3, 13):
        for x in range(4, 12):
            dx, dy = x - 7.5, y - 8
            if abs(dx) <= 3.6 and abs(dy) <= 4.6 and not (abs(dx) > 2.6 and abs(dy) > 3.6):
                px[y][x] = shade((176, 180, 184), 0.25 if dx < -1 else (-0.15 if dx > 2 else 0))
    for y in range(6, 11):
        for x in range(5, 11):
            if (y in (6, 8, 10)) and 5 <= x <= 10 - (y % 3):
                px[y][x] = shade((90, 94, 98), 0)                                            # stamped lines
    px[3][7] = px[3][8] = (0, 0, 0, 0)
    for x in range(6, 12):
        px[1][x] = shade((150, 150, 150), -0.1 if x % 2 else 0.1)                            # the chain
    px[2][6] = px[2][11] = shade((150, 150, 150), 0)
    return T.hd_item(px, (110, 114, 118, 255), "dog_tag")


def bleeding_icon():
    px = [[(0, 0, 0, 0) for _ in range(18)] for _ in range(18)]
    for y in range(18):
        for x in range(18):
            dx, dy = x - 8.5, y - 10.5
            top = y < 9 and abs(dx) <= (y - 1) * 0.55
            drop = dx * dx + dy * dy <= 20 and y >= 6
            if top or drop:
                f = 0.3 if dx < -1.5 and dy < 0 else (-0.35 if dx > 1.5 or dy > 2 else 0)
                px[y][x] = shade((178, 16, 16), f)
    px[9][6] = px[10][6] = (255, 140, 140, 255)
    return px


def main():
    edir = os.path.join(ASSETS, "textures", "entity", "operator")
    os.makedirs(edir, exist_ok=True)
    for i, (kind, spec) in enumerate(KINDS.items()):
        make_skin(kind, spec, 1000 + i * 17).save(os.path.join(edir, kind + ".png"))
    idir = os.path.join(ASSETS, "textures", "item")
    for name, fn in (("frag_grenade", grenade), ("bandage", bandage), ("medkit", medkit), ("dog_tag", dog_tag)):
        T.png(os.path.join(idir, name + ".png"), fn())
        with open(os.path.join(ASSETS, "models", "item", name + ".json"), "w") as f:
            json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": NS + ":item/" + name}}, f, indent=1)
            f.write("\n")
    os.makedirs(os.path.join(ASSETS, "textures", "mob_effect"), exist_ok=True)
    T.png(os.path.join(ASSETS, "textures", "mob_effect", "bleeding.png"), bleeding_icon())
    for kind in KINDS:
        with open(os.path.join(ASSETS, "models", "item", kind + "_spawn_egg.json"), "w") as f:
            json.dump({"parent": "minecraft:item/template_spawn_egg"}, f, indent=1)
            f.write("\n")
    # Names
    src = open(os.path.join(ROOT, "mod", "src", "main", "java", "dev", "factoryascension", "npc", "OpType.java")).read()
    import re
    names = dict(re.findall(r'\w+\("(\w+)", "([^"]+)"', src))
    lang_path = os.path.join(ASSETS, "lang", "en_us.json")
    lang = json.load(open(lang_path))
    for kind in KINDS:
        lang["entity.%s.%s" % (NS, kind)] = names.get(kind, kind)
        lang["item.%s.%s_spawn_egg" % (NS, kind)] = names.get(kind, kind) + " Spawn Egg"
    lang.update({
        "entity.%s.frag_grenade" % NS: "Frag Grenade",
        "item.%s.frag_grenade" % NS: "Frag Grenade",
        "item.%s.bandage" % NS: "Bandage",
        "item.%s.medkit" % NS: "Medkit",
        "item.%s.dog_tag" % NS: "Dog Tag",
        "effect.%s.bleeding" % NS: "Bleeding",
        "key.factoryascension.realism": "Realism camera (on/off)",
    })
    with open(lang_path, "w") as f:
        json.dump(lang, f, indent=1)
        f.write("\n")
    print("operators: %d skins, 4 items, the bleeding icon" % len(KINDS))


if __name__ == "__main__":
    main()
