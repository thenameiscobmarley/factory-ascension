#!/usr/bin/env python3
"""Art and names for the v26 systems: helmets (icons and worn layers), thermal goggles, medical kit (splint,
tourniquet, surgical kit, canteen), the weapon cleaning kit, the raid map, cash and trader contracts, the quad, the
truck and the wrench; the night hunter's and the traders' skins; the new effect icons; and every name.

  python3 tools/v26_art.py      (make_mod_assets.py runs it)
"""
import json
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))
import operator_art as O  # noqa: E402
import v25_art as V  # noqa: E402
from v25_art import canvas, ell, line, rect, save_item, shade, tex  # noqa: E402

NS = V.NS
A = V.A


# ---------------- Helmets ----------------
HELMETS = {  # id: (colour, style)
    "steel_helmet": ((84, 92, 70), "steel"),
    "pasgt_helmet": ((92, 96, 72), "pasgt"),
    "fast_helmet": ((150, 132, 96), "fast"),
    "altyn_helmet": ((70, 78, 66), "altyn"),
}


def helmet_icon(name, c, style):
    px = canvas()
    # The dome
    for y in range(16):
        for x in range(16):
            dx, dy = (x + 0.5 - 8) / 6.6, (y + 0.5 - (9 if style != "fast" else 8.5)) / 5.6
            if dx * dx + dy * dy <= 1 and y <= (11 if style == "steel" else 12):
                f = 0.3 * max(0, -dx - dy * 0.6) - 0.35 * max(0, dx + dy * 0.5)
                px[y][x] = shade(c, f + (0.04 if (x * 7 + y * 3) % 5 == 0 else 0))
    if style == "steel":
        rect(px, 0, 11, 15, 11, shade(c, -0.25))                       # the wide brim
        line(px, 1, 12, 14, 12, shade(c, -0.45))
    elif style == "pasgt":
        rect(px, 2, 12, 13, 12, shade(c, -0.3))
        rect(px, 2, 10, 3, 13, shade(c, -0.2))
        rect(px, 12, 10, 13, 13, shade(c, -0.2))
        for x in range(3, 13, 2):
            px[7][x] = shade(c, -0.2)                                 # cover's band
    elif style == "fast":
        rect(px, 2, 9, 3, 11, (60, 60, 58, 255))                       # rails
        rect(px, 12, 9, 13, 11, (60, 60, 58, 255))
        rect(px, 6, 3, 9, 4, (52, 52, 50, 255))                        # the NVG shroud
        ell(px, 3, 11.5, 1.8, 1.8, (40, 40, 40))                       # ear cups
        ell(px, 13, 11.5, 1.8, 1.8, (40, 40, 40))
    elif style == "altyn":
        rect(px, 3, 9, 12, 14, None, lambda x, y: shade((60, 66, 58), 0.15 if x == 3 else (-0.25 if x == 12 else 0)))   # the visor
        rect(px, 5, 10, 10, 11, (30, 40, 44, 255))
        rect(px, 4, 9, 11, 9, shade(c, -0.3))
    save_item(name, px, (30, 32, 28, 255))


def helmet_layer(c, style):
    def head(face, x, y, w, h):
        if face == "bottom":
            return None
        n = 0.04 if (x * 5 + y * 3) % 7 == 0 else 0
        if face == "top":
            return shade(c, 0.05 + n)
        low = {"steel": 3, "pasgt": 4, "fast": 3, "altyn": 4}[style]
        if face == "front":
            if style == "altyn":
                if 2 <= y <= 7 and 1 <= x <= 6:
                    return (40, 54, 58, 210) if 3 <= y <= 4 else shade((58, 64, 56), -0.1)
                return shade(c, n) if y < 2 else None
            if style == "fast" and y == 0 and 3 <= x <= 4:
                return (40, 40, 40, 255)
            return shade(c, n - (0.2 if y == low - 1 else 0)) if y < low else None
        if face in ("left", "right"):
            if style == "fast" and 3 <= y <= 5 and 2 <= x <= 5:
                return (36, 36, 36, 255)                               # ear cups
            if style == "fast" and y == 2:
                return (64, 64, 60, 255)                               # rails
            return shade(c, n - 0.08) if y < low + 1 else None
        if face == "back":
            return shade(c, n - 0.12) if y < low + 2 else None
        return None
    return V.armor_layer(lambda box: box(0, 0, 8, 8, 8, head))


def thermal_layer(down):
    c, t = (44, 46, 48), (54, 56, 60)

    def head(face, x, y, w, h):
        if face == "top":
            return shade(c, -0.1) if 3 <= x <= 4 or 3 <= y <= 4 else None
        if face == "bottom":
            return None
        if y in (1, 2):
            return shade(c, -0.05)
        if face == "front":
            if down and 3 <= y <= 5 and 1 <= x <= 6:
                return (255, 150, 60, 255) if y == 4 and x in (2, 5) else shade(t, 0.1 if y == 3 else -0.2)   # one wide housing, amber glow
            if not down and y == 0 and 1 <= x <= 6:
                return shade(t, 0)
            if 3 <= x <= 4 and y <= 3:
                return shade((74, 76, 80), 0)
        return None
    return V.armor_layer(lambda box: box(0, 0, 8, 8, 8, head))


def thermal_icon():
    px = canvas()
    rect(px, 1, 3, 14, 4, (40, 42, 44, 255))
    rect(px, 2, 6, 13, 12, None, lambda x, y: shade((54, 56, 60), 0.25 if y == 6 else (-0.3 if y == 12 else 0)))
    for cx in (5, 10):
        ell(px, cx + 0.5, 9.5, 2.3, 2.0, (30, 30, 32))
        ell(px, cx + 0.5, 9.5, 1.4, 1.2, (255, 140, 50), 0.5, -0.2)
    rect(px, 6, 5, 9, 5, (74, 76, 80, 255))
    save_item("thermal_goggles", px, (24, 24, 26, 255))


# ---------------- Medical and kit ----------------
def splint():
    px = canvas()
    for x0 in (4, 10):
        rect(px, x0, 1, x0 + 1, 14, None, lambda x, y: shade((176, 140, 90), 0.2 if x == x0 else -0.15))
    for y in (3, 8, 12):
        rect(px, 3, y, 12, y + 1, None, lambda x, y2: shade((230, 226, 214), 0.1 if y2 == y else -0.1))
    save_item("splint", px, (90, 70, 44, 255))


def tourniquet():
    px = canvas()
    for k in range(40):
        a = 2 * math.pi * k / 40
        x, y = 8 + 5.2 * math.cos(a), 8.5 + 4.2 * math.sin(a)
        rect(px, int(x), int(y), int(x) + 1, int(y), shade((30, 30, 30), 0.15 if math.sin(a) < 0 else -0.1))
    rect(px, 3, 6, 12, 7, (220, 120, 30, 255))                         # the windlass
    rect(px, 3, 6, 3, 7, (240, 160, 70, 255))
    rect(px, 6, 9, 9, 11, (60, 60, 60, 255))
    save_item("tourniquet", px, (16, 16, 16, 255))


def surgical_kit():
    px = canvas()
    rect(px, 1, 4, 14, 13, None, lambda x, y: shade((70, 90, 70), 0.25 if y == 4 else (-0.3 if y == 13 or x == 14 else 0)))
    rect(px, 6, 6, 9, 11, (220, 220, 220, 255))
    rect(px, 4, 8, 11, 9, (220, 220, 220, 255))
    rect(px, 7, 7, 8, 10, (200, 40, 36, 255))
    rect(px, 5, 8, 10, 8, (200, 40, 36, 255))
    rect(px, 5, 2, 10, 3, (40, 40, 40, 255))
    save_item("surgical_kit", px, (30, 40, 30, 255))


def canteen():
    px = canvas()
    ell(px, 8, 9.5, 5.5, 5.8, (80, 92, 60))
    rect(px, 6, 1, 9, 3, (60, 60, 58, 255))
    rect(px, 7, 0, 8, 0, (90, 90, 88, 255))
    line(px, 3, 9, 13, 9, shade((80, 92, 60), -0.3))                   # the seam
    rect(px, 4, 6, 4, 12, shade((80, 92, 60), 0.2))
    save_item("canteen", px, (40, 46, 30, 255))


def cleaning_kit():
    px = canvas()
    rect(px, 2, 7, 13, 13, None, lambda x, y: shade((60, 62, 52), 0.2 if y == 7 else (-0.3 if y == 13 else 0)))
    line(px, 1, 5, 14, 2, (170, 170, 176, 255))                        # the cleaning rod
    line(px, 1, 6, 14, 3, (110, 110, 116, 255))
    rect(px, 13, 1, 15, 3, (200, 60, 40, 255))                         # its brush
    rect(px, 4, 9, 6, 11, (210, 200, 160, 255))                        # patches
    rect(px, 9, 9, 11, 11, (40, 40, 44, 255))                          # oil
    px[9][10] = (120, 130, 60, 255)
    save_item("weapon_cleaning_kit", px, (26, 28, 22, 255))


def raid_map():
    px = canvas()
    rect(px, 1, 2, 14, 13, None, lambda x, y: shade((214, 200, 160), (-0.12 if x in (5, 10) else 0) + (0.05 if (x * 3 + y) % 7 == 0 else 0)))
    for (x, y) in ((3, 4), (4, 5), (5, 5), (6, 6), (7, 8), (8, 9), (9, 9), (10, 10), (11, 11)):
        px[y][x] = (120, 90, 60, 255)                                  # a road
    rect(px, 2, 9, 4, 11, (110, 140, 90, 255))
    rect(px, 9, 3, 12, 5, (100, 130, 170, 255))
    for (x, y) in ((11, 11), (12, 12), (12, 10), (13, 11), (11, 12)):
        px[y][x] = (200, 30, 30, 255)                                  # an X
    px[6][3] = px[4][12] = (40, 160, 60, 255)                          # extraction points
    save_item("raid_map", px, (110, 96, 60, 255))


def cash():
    px = canvas()
    for k, oy in enumerate((9, 6, 3)):
        rect(px, 1 + k, oy, 13 + k, oy + 5, None, lambda x, y: shade((120, 150, 110), (0.15 if y == oy else -0.15 if y == oy + 5 else 0) - 0.05 * k))
        rect(px, 6 + k, oy + 1, 8 + k, oy + 4, shade((90, 120, 90), -0.1 * k))
    rect(px, 6, 3, 8, 14, (190, 160, 90, 255))                         # the band
    save_item("cash_bundle", px, (40, 60, 40, 255))


def contract(name, seal):
    px = canvas()
    rect(px, 3, 1, 12, 14, None, lambda x, y: shade((232, 226, 206), -0.1 if x == 12 or y == 14 else 0))
    for y in (3, 5, 7, 9):
        line(px, 5, y, 10, y, (120, 116, 106, 255))
    ell(px, 10, 12, 2.4, 2.4, seal, 0.3, -0.3)
    line(px, 4, 11, 7, 11, (40, 40, 70, 255))                           # the signature
    save_item(name, px, (120, 110, 90, 255))


def quad_icon():
    px = canvas()
    for cx in (3, 12):
        ell(px, cx + 0.5, 12, 2.8, 2.8, (30, 30, 30), 0.1, -0.2)
        ell(px, cx + 0.5, 12, 1.1, 1.1, (90, 90, 90), 0, 0)
    rect(px, 2, 7, 13, 9, None, lambda x, y: shade((200, 140, 40), 0.2 if y == 7 else -0.2 if y == 9 else 0))
    rect(px, 5, 5, 9, 6, (40, 40, 40, 255))                            # seat
    line(px, 10, 6, 12, 3, (50, 50, 50, 255))
    rect(px, 11, 2, 13, 2, (50, 50, 50, 255))
    rect(px, 6, 10, 9, 11, (150, 150, 152, 255))
    save_item("quad_bike", px, (40, 30, 10, 255))


def pickup_icon():
    px = canvas()
    rect(px, 0, 7, 15, 11, None, lambda x, y: shade((96, 110, 74), 0.2 if y == 7 else -0.25 if y == 11 else 0))
    rect(px, 7, 3, 12, 7, None, lambda x, y: shade((96, 110, 74), 0.15 if y == 3 else 0))
    rect(px, 8, 4, 11, 6, (110, 170, 180, 255))
    for cx in (3, 12):
        ell(px, cx + 0.5, 12.5, 2.4, 2.4, (26, 26, 26), 0.1, -0.2)
        ell(px, cx + 0.5, 12.5, 0.9, 0.9, (140, 140, 140), 0, 0)
    line(px, 1, 4, 5, 3, (40, 40, 40, 255))                             # the gun on the bed
    rect(px, 3, 4, 4, 6, (50, 50, 50, 255))
    px[9][15] = (240, 230, 180, 255)
    save_item("pickup_truck", px, (30, 36, 24, 255))


def wrench():
    px = canvas()
    for k in range(10):
        x, y = 3 + k, 12 - k
        rect(px, x, y, x + 1, y + 1, shade((170, 172, 178), 0.2 if k % 3 == 0 else 0))
    ell(px, 12.5, 3.5, 2.8, 2.8, (170, 172, 178), 0.3, -0.3)
    rect(px, 12, 2, 13, 4, (0, 0, 0, 0))
    rect(px, 13, 1, 15, 2, (0, 0, 0, 0))
    ell(px, 3, 13, 2, 2, (150, 152, 158), 0.3, -0.3)
    save_item("wrench", px, (60, 60, 66, 255))


# ---------------- Skins ----------------
TRADERS = {
    "trader_quartermaster": dict(top=0x4E5A3A, bottom=0x3E4430, accent=0xC8A040, camo="woodland", head="cap", vest="rig"),
    "trader_medic": dict(top=0xE6E6E0, bottom=0x4A5A6A, accent=0xC02020, camo="plain", head="bare_beard", vest="none", medic=True),
    "trader_fence": dict(top=0x2A2624, bottom=0x22201E, accent=0x6A5A4A, camo="plain", head="hood", vest="none", bag=True),
}


def night_hunter():
    s = O.Skin(4242)
    rnd = s.rnd
    hide = (62, 60, 58)

    def flesh(face, x, y, w, h):
        f = rnd.uniform(-0.12, 0.06)
        if (x + y * 2) % 5 == 0:
            f -= 0.15                                                   # ribs, sinews
        return shade(hide, f)

    def head(face, x, y, w, h):
        if face == "front":
            if y == 3 and x in (2, 5):
                return (10, 10, 10, 255)                                # sockets (the eyes glow on their own layer)
            if y >= 5 and 1 <= x <= 6:
                return (24, 18, 16, 255) if y == 5 or (y == 6 and x % 2) else (210, 200, 180, 255) if y == 6 else shade(hide, -0.2)   # the teeth
        return flesh(face, x, y, w, h)
    s.box(*O.HEAD, head)
    for b in (O.BODY, O.RARM, O.LARM, O.RLEG, O.LLEG):
        s.box(*b, lambda face, x, y, w, h, b=b: shade((30, 28, 26), rnd.uniform(-0.1, 0.05)) if b in (O.RARM, O.LARM) and y >= 10 else flesh(face, x, y, w, h))
    s.save(os.path.join(A, "textures", "entity", "night_hunter.png"))
    eyes = [[(0, 0, 0, 0) for _ in range(64)] for _ in range(64)]
    for x in (8 + 2, 8 + 5):
        eyes[8 + 3][x] = (235, 240, 225, 255)
    tex("entity/night_hunter_eyes", eyes)


# ---------------- Effects ----------------
def effect_icons():
    def bleed(p):
        for y in range(18):
            for x in range(18):
                dx, dy = (x - 8.5) / 5.5, (y - 10.5) / 6
                if dx * dx + dy * dy <= 1 or (abs(x - 8.5) < (y - 1) * 0.55 and y < 10):
                    p[y][x] = shade((150, 10, 10), 0.25 if dx < -0.3 and dy < 0 else 0)
        rect(p, 6, 11, 11, 12, (90, 0, 0, 255))
    tex("mob_effect/heavy_bleeding", V.icon(bleed))

    def frac(p):
        rect(p, 2, 7, 15, 10, (230, 226, 210, 255))
        ell(p, 2.5, 6.5, 2.2, 2.2, (230, 226, 210), 0.1, -0.1)
        ell(p, 2.5, 10.5, 2.2, 2.2, (230, 226, 210), 0.1, -0.1)
        for y in range(6, 12):
            p[y][8 + (y % 2)] = (0, 0, 0, 0)                           # the break
            p[y][9 - (y % 2)] = (60, 40, 40, 255)
    tex("mob_effect/fracture", V.icon(frac))

    def dry(p):
        for y in range(18):
            for x in range(18):
                dx, dy = (x - 8.5) / 5.5, (y - 11) / 5.5
                if dx * dx + dy * dy <= 1 or (abs(x - 8.5) < (y - 2) * 0.6 and 2 <= y < 11):
                    p[y][x] = shade((150, 120, 70), 0.2 if dx < 0 else -0.1)
        line(p, 3, 15, 14, 3, (120, 30, 20, 255))
    tex("mob_effect/dehydrated", V.icon(dry))


def main():
    for name, (c, style) in HELMETS.items():
        helmet_icon(name, c, style)
        tex("models/armor/" + name, helmet_layer(c, style))
    thermal_icon()
    tex("models/armor/thermal_down", thermal_layer(True))
    tex("models/armor/thermal_up", thermal_layer(False))
    splint()
    tourniquet()
    surgical_kit()
    canteen()
    cleaning_kit()
    raid_map()
    cash()
    contract("trader_contract_quartermaster", (200, 160, 60))
    contract("trader_contract_medic", (200, 40, 40))
    contract("trader_contract_fence", (60, 60, 60))
    quad_icon()
    pickup_icon()
    wrench()
    night_hunter()
    for i, (k, spec) in enumerate(TRADERS.items()):
        O.make_skin(k, spec, 7000 + i * 31).save(os.path.join(A, "textures", "entity", k + ".png"))
    V.model("night_hunter_spawn_egg", {"parent": "minecraft:item/template_spawn_egg"})
    effect_icons()
    lang_path = os.path.join(A, "lang", "en_us.json")
    lang = json.load(open(lang_path))
    item = {"steel_helmet": "Steel Helmet", "pasgt_helmet": "PASGT Helmet", "fast_helmet": "FAST Ballistic Helmet", "altyn_helmet": "Altyn Helmet",
            "thermal_goggles": "Thermal Goggles", "splint": "Splint", "tourniquet": "Tourniquet", "surgical_kit": "Surgical Kit",
            "canteen": "Canteen", "weapon_cleaning_kit": "Weapon Cleaning Kit", "raid_map": "Raid Map", "cash_bundle": "Cash Bundle",
            "trader_contract_quartermaster": "Trader Contract: Quartermaster", "trader_contract_medic": "Trader Contract: Medic",
            "trader_contract_fence": "Trader Contract: The Fence", "quad_bike": "Quad Bike", "pickup_truck": "Pickup Truck", "wrench": "Wrench",
            "night_hunter_spawn_egg": "Night Hunter Spawn Egg"}
    for k, v in item.items():
        lang["item.%s.%s" % (NS, k)] = v
    for k, v in {"night_hunter": "Night Hunter", "body_bag": "Body Bag", "trader": "Trader", "quad_bike": "Quad Bike", "pickup_truck": "Pickup Truck"}.items():
        lang["entity.%s.%s" % (NS, k)] = v
    for k, v in {"heavy_bleeding": "Heavy Bleeding", "fracture": "Fracture", "dehydrated": "Dehydrated"}.items():
        lang["effect.%s.%s" % (NS, k)] = v
    with open(lang_path, "w") as f:
        json.dump(lang, f, indent=1)
        f.write("\n")
    print("v26 art: %d item icons, 4 helmet layers, thermal layers, hunter + 3 trader skins, 3 effect icons" % (len(item) - 1))


if __name__ == "__main__":
    main()
