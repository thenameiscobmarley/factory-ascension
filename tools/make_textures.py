#!/usr/bin/env python3
"""Generate the 16x16 item textures for Factory Ascension (stdlib only).

Run from the repo root:  python3 tools/make_textures.py
"""
import colorsys
import math
import os
import random
import re
import struct
import zlib

OUT = os.path.join(os.path.dirname(__file__), "..", "kubejs", "assets", "kubejs", "textures", "item")
N = 16


def png(path, px):
    raw = b"".join(b"\x00" + bytes(c for p in row for c in p) for row in px)

    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n")
        f.write(chunk(b"IHDR", struct.pack(">IIBBBBB", N, N, 8, 6, 0, 0, 0)))
        f.write(chunk(b"IDAT", zlib.compress(raw, 9)))
        f.write(chunk(b"IEND", b""))


def blank():
    return [[(0, 0, 0, 0) for _ in range(N)] for _ in range(N)]


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))


def outline(px, color):
    """Dark 1px outline around every opaque pixel, like vanilla items."""
    out = [row[:] for row in px]
    for y in range(N):
        for x in range(N):
            if px[y][x][3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < N and 0 <= ny < N and px[ny][nx][3] and px[ny][nx] != color:
                    out[y][x] = color
                    break
    return out


def gem(radius, top, bottom, shine, dim=1.0):
    """Diamond-shaped gem with a vertical gradient and a highlight."""
    px = blank()
    c = 7.5
    for y in range(N):
        for x in range(N):
            d = abs(x - c) + abs(y - c)
            if d <= radius:
                t = (y - (c - radius)) / (2 * radius)
                col = lerp(top, bottom, max(0.0, min(1.0, t)))
                # Facet: left half a bit darker
                if x < c:
                    col = lerp(col, (0, 0, 0), 0.18)
                # Core glow
                if d <= radius * 0.35:
                    col = lerp(col, shine, 0.65)
                col = tuple(int(v * dim) for v in col)
                px[y][x] = col + (255,)
    # Highlight pixels on the upper-left facet
    for x, y in ((6, 4), (5, 5), (6, 5)):
        if px[y][x][3]:
            px[y][x] = shine + (255,)
    return px


def ring(px, radius, color):
    for a in range(0, 360, 4):
        x = int(round(7.5 + radius * math.cos(math.radians(a))))
        y = int(round(7.5 + radius * math.sin(math.radians(a))))
        if 0 <= x < N and 0 <= y < N and not px[y][x][3]:
            px[y][x] = color + (255,)
    return px


def singularity():
    px = blank()
    for y in range(N):
        for x in range(N):
            dx, dy = x - 7.5, y - 7.5
            r = math.hypot(dx, dy)
            if r > 6.6:
                continue
            ang = math.atan2(dy, dx)
            swirl = (math.sin(ang * 3 + r * 1.3) + 1) / 2
            base = lerp((20, 6, 34), (90, 30, 140), 1 - r / 6.6)
            col = lerp(base, (255, 196, 64), swirl ** 4 * 0.9)
            if r < 1.6:
                col = (255, 244, 210)
            px[y][x] = col + (255,)
    return px


def cluster(rock_light, rock_dark, specks, seed):
    rnd = random.Random(seed)
    px = blank()
    for y in range(N):
        for x in range(N):
            dx, dy = (x - 7.5) / 6.4, (y - 8.0) / 5.6
            wobble = 0.12 * math.sin(x * 1.7 + seed) + 0.1 * math.cos(y * 2.1 + seed)
            if dx * dx + dy * dy <= 1 + wobble:
                t = (y + x * 0.5) / (N * 1.5)
                col = lerp(rock_light, rock_dark, t)
                if rnd.random() < 0.18:
                    col = lerp(col, (0, 0, 0), 0.2)
                px[y][x] = col + (255,)
    filled = [(x, y) for y in range(N) for x in range(N) if px[y][x][3]]
    rnd.shuffle(filled)
    for i, (x, y) in enumerate(filled[: len(specks) * 4]):
        color = specks[i % len(specks)]
        px[y][x] = color + (255,)
        # Small sparkle next to some specks
        if i % 3 == 0 and x + 1 < N and px[y][x + 1][3]:
            px[y][x + 1] = lerp(color, (255, 255, 255), 0.5) + (255,)
    return px


def book(cover, gem_color, mythic=False):
    """Closed book: colored cover, cream page edge, gem on the front. Mythic: gold trim + bigger gem."""
    px = blank()
    dark = lerp(cover, (0, 0, 0), 0.45)
    light = lerp(cover, (255, 255, 255), 0.25)
    for y in range(2, 14):
        for x in range(3, 13):
            col = cover
            if x == 3 or y == 2:
                col = light
            if x <= 4:
                col = dark  # spine
            px[y][x] = col + (255,)
    # Page edge along the bottom and right
    for x in range(5, 13):
        px[13][x] = (236, 226, 196, 255)
    for y in range(3, 14):
        px[y][12] = (236, 226, 196, 255)
    # Gold corners
    for x, y in ((11, 2), (5, 2), (11, 12)):
        px[y][x] = (240, 200, 80, 255)
    # Gem in the middle of the cover
    for x, y in ((8, 6), (7, 7), (8, 7), (9, 7), (8, 8)):
        px[y][x] = gem_color + (255,)
    px[6][8] = lerp(gem_color, (255, 255, 255), 0.6) + (255,)
    if mythic:
        gold = (240, 200, 80, 255)
        for x in range(5, 12):
            px[2][x] = gold
        for y in range(2, 13):
            px[y][11] = gold
        for x, y in ((8, 5), (6, 7), (10, 7), (8, 9)):
            px[y][x] = lerp(gem_color, (0, 0, 0), 0.25) + (255,)
    return px


def chaos_book():
    """Book with shifting rainbow bands and a white star: the random-enchant tome."""
    px = book((40, 20, 60), (255, 255, 255), True)
    for y in range(3, 13):
        for x in range(5, 11):
            hue = ((x + y) / 14.0) % 1.0
            px[y][x] = tuple(int(c * 255) for c in colorsys.hsv_to_rgb(hue, 0.65, 0.85)) + (255,)
    for x, y in ((8, 5), (7, 7), (8, 7), (9, 7), (8, 9), (8, 6), (8, 8)):
        px[y][x] = (255, 255, 255, 255)
    return px


BLOCK_OUT = os.path.join(os.path.dirname(__file__), "..", "kubejs", "assets", "kubejs", "textures", "block")


def gem_colors():
    """Gem types and colors, read from the config so new gems get textures automatically."""
    text = open(CONFIG).read()
    return {m.group(1): tuple(int(v) for v in m.group(2).split(","))
            for m in re.finditer(r"^\s+(\w+): \{ name: '[^']*', enchant: '\w+', color: \[([\d, ]+)\] \}", text, re.M)}


def gem_item(color, tier):
    """Cut gem: bigger and with more facets per tier."""
    px = blank()
    r = {1: 3.6, 2: 4.8, 3: 6.0}[tier]
    c = 7.5
    light = lerp(color, (255, 255, 255), 0.55)
    dark = lerp(color, (0, 0, 0), 0.45)
    for y in range(N):
        for x in range(N):
            dx, dy = x - c, y - c
            if abs(dx) + abs(dy) * (1.25 if tier == 1 else 1.0) > r * 1.25 or abs(dx) > r or abs(dy) > r:
                continue
            col = color
            if dy < -r * 0.3:
                col = lerp(color, light, 0.5)
            elif dx > 0 and dy > 0:
                col = dark
            px[y][x] = col + (255,)
    for x, y in ((6, 5), (7, 5), (6, 6)):
        if px[y][x][3]:
            px[y][x] = (255, 255, 255, 255)
    if tier == 3:
        for x, y in ((3, 3), (12, 4), (11, 12)):
            px[y][x] = (255, 250, 220, 255)
    return px


def altar_block():
    """Obsidian block with a glowing amethyst rune ring."""
    rnd = random.Random(7)
    px = [[None] * N for _ in range(N)]
    for y in range(N):
        for x in range(N):
            v = rnd.random()
            px[y][x] = lerp((22, 14, 34), (48, 30, 70), v) + (255,)
    for i in range(N):
        px[0][i] = px[N - 1][i] = px[i][0] = px[i][N - 1] = (90, 60, 120, 255)
    for a in range(0, 360, 10):
        x = int(round(7.5 + 4.5 * math.cos(math.radians(a))))
        y = int(round(7.5 + 4.5 * math.sin(math.radians(a))))
        px[y][x] = (200, 140, 255, 255)
    for x, y in ((7, 7), (8, 7), (7, 8), (8, 8), (7, 5), (8, 10), (5, 8), (10, 7)):
        px[y][x] = (255, 220, 120, 255)
    return px


def fragment():
    """Jagged glowing shard."""
    px = blank()
    shape = [(7, 2), (8, 2), (6, 3), (7, 3), (8, 3), (6, 4), (7, 4), (8, 4), (9, 4), (5, 5), (6, 5), (7, 5), (8, 5), (9, 5),
             (5, 6), (6, 6), (7, 6), (8, 6), (9, 6), (10, 6), (6, 7), (7, 7), (8, 7), (9, 7), (10, 7), (6, 8), (7, 8), (8, 8), (9, 8),
             (7, 9), (8, 9), (9, 9), (7, 10), (8, 10), (8, 11), (7, 12), (8, 12)]
    for x, y in shape:
        t = y / 13
        px[y][x] = lerp((255, 220, 120), (170, 60, 255), t) + (255,)
    for x, y in ((7, 3), (6, 5), (7, 6)):
        px[y][x] = (255, 255, 235, 255)
    return px


CONFIG = os.path.join(os.path.dirname(__file__), "..", "kubejs", "startup_scripts", "00_fa_config.js")

# Cover hue per enchant type, so tomes of the same kind look related
TYPE_HUES = {
    "damage": 0.0, "critical": 0.97, "reaper": 0.93, "cleave": 0.03,
    "lifesteal": 0.98, "hit_effect": 0.80, "self_effect": 0.75, "ignite": 0.07, "hit_heal": 0.30,
    "plunder": 0.13, "kill_heal": 0.35, "xp": 0.20, "drop": 0.15,
    "resist": 0.55, "vitality": 0.33, "retribution": 0.08, "dodge": 0.50, "undying": 0.11,
    "regen": 0.38, "passive": 0.42, "magnet": 0.62, "repair": 0.65,
    "attribute": 0.58, "mining": 0.66, "eternal": 0.78,
    # Mythic types: the gem color (covers are dark)
    "flight": 0.55, "chain_lightning": 0.16, "gravity_well": 0.76, "death_nova": 0.05, "time_stop": 0.50,
    "phoenix": 0.08, "projectile_teleport": 0.82, "souls": 0.47, "overkill": 0.98, "rampage": 0.0,
    "vein": 0.30, "aura": 0.14, "heads": 0.33, "slam": 0.62,
}


def tome_colors():
    """(id -> (cover, gem)) for every enchant in the config, varied a little per enchant."""
    text = open(CONFIG).read()
    out = {}
    for m in re.finditer(r"^\s{4}(\w+):\s*\{ name: '[^']*', type: '(\w+)'(.*)$", text, re.M):
        name, type_ = m.group(1), m.group(2)
        if "mythic: true" in m.group(3):
            # Mythic: near-black cover, bright gem, gold trim (see book())
            hue = TYPE_HUES.get(type_, 0.5)
            cover = tuple(int(c * 255) for c in colorsys.hsv_to_rgb(hue, 0.6, 0.18))
            gem_col = tuple(int(c * 255) for c in colorsys.hsv_to_rgb(hue, 0.65, 1.0))
            out[name] = (cover, gem_col, True)
            continue
        jitter = (zlib.crc32(name.encode()) % 1000) / 1000.0
        hue = (TYPE_HUES.get(type_, 0.5) + (jitter - 0.5) * 0.08) % 1.0
        val = 0.42 + jitter * 0.2
        cover = tuple(int(c * 255) for c in colorsys.hsv_to_rgb(hue, 0.75, val))
        gem_col = tuple(int(c * 255) for c in colorsys.hsv_to_rgb((hue + 0.5) % 1.0 if type_ == "eternal" else hue, 0.5, 1.0))
        out[name] = (cover, gem_col, False)
    return out



def main():
    os.makedirs(OUT, exist_ok=True)
    edge = (24, 10, 36, 255)
    textures = {
        "ascension_core": outline(gem(5.5, (190, 120, 255), (110, 40, 190), (255, 230, 140)), edge),
        "dense_ascension_core": outline(ring(gem(5.0, (255, 170, 230), (150, 40, 170), (255, 245, 180)), 7.0, (255, 196, 64)), edge),
        "ascension_singularity": outline(singularity(), (8, 2, 14, 255)),
        "incomplete_ascension_core": outline(gem(5.5, (150, 140, 160), (80, 70, 95), (200, 180, 150), 0.85), (20, 18, 24, 255)),
        "common_mineral_cluster": outline(cluster((150, 150, 150), (85, 85, 90),
                                                  [(216, 175, 147), (230, 120, 70), (120, 200, 190), (250, 215, 80)], 1), (30, 30, 34, 255)),
        "gem_mineral_cluster": outline(cluster((110, 110, 120), (55, 55, 65),
                                               [(220, 30, 30), (40, 80, 220), (110, 240, 230), (40, 210, 90)], 2), (20, 20, 26, 255)),
        "exotic_mineral_cluster": outline(cluster((60, 55, 70), (25, 22, 30),
                                                  [(150, 200, 255), (170, 170, 190), (120, 255, 120), (210, 140, 255)], 3), (10, 8, 14, 255)),
    }
    textures["blank_ascension_tome"] = outline(book((105, 70, 45), (190, 120, 255)), edge)
    tomes = tome_colors()
    if len(tomes) < 9:
        raise SystemExit('could not read enchants from ' + CONFIG)
    for name, (cover, gem_color, mythic) in tomes.items():
        textures["tome_" + name] = outline(book(cover, gem_color, mythic), edge)
    textures["mythic_fragment"] = outline(fragment(), (30, 8, 40, 255))
    textures["chaos_tome"] = outline(chaos_book(), edge)
    gems = gem_colors()
    for name, color in gems.items():
        for tier in (1, 2, 3):
            textures["gem_%s_%d" % (name, tier)] = outline(gem_item(color, tier), (20, 16, 24, 255))
    os.makedirs(BLOCK_OUT, exist_ok=True)
    png(os.path.join(BLOCK_OUT, "ascension_altar.png"), altar_block())
    print("wrote block/ascension_altar.png")
    for name, px in textures.items():
        png(os.path.join(OUT, name + ".png"), px)
        print("wrote", name + ".png")


if __name__ == "__main__":
    main()
