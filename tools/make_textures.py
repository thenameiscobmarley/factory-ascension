#!/usr/bin/env python3
"""Generate the item and block textures for Factory Ascension (stdlib only).

Every sprite is drawn at 16x16, then rendered at 32x32 with a detail pass (see hd_item / hd_block):
smoothed edges, top-left lighting with highlights and shadows, a fine material grain and a thin colored outline.

Run from the repo root:  python3 tools/make_textures.py
"""
import colorsys
import math
import os
import random
import re
import struct
import zlib

import food_art
import gear_art
import tome_art

OUT = os.path.join(os.path.dirname(__file__), "..", "kubejs", "assets", "kubejs", "textures", "item")
N = 16


def png(path, px):
    # Detail pass at 2x, chosen by what the texture is
    if isinstance(px, Outlined):
        px = hd_item(px.base, px.edge, path)
    elif os.sep + "armor" + os.sep in path:
        px = hd_armor(px, path)
    elif os.sep + "block" + os.sep in path:
        px = hd_block(px, path)
    raw = b"".join(b"\x00" + bytes(c for p in row for c in p) for row in px)

    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n")
        f.write(chunk(b"IHDR", struct.pack(">IIBBBBB", len(px[0]), len(px), 8, 6, 0, 0, 0)))
        f.write(chunk(b"IDAT", zlib.compress(raw, 9)))
        f.write(chunk(b"IEND", b""))


# ---- 32x32 detail pass ----

def _grain(seed, x, y):
    """Deterministic noise in [-1, 1] per pixel, different for every texture."""
    h = (x * 374761393 + y * 668265263 + seed * 2246822519) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFFFF) / 32767.5 - 1


def _seed(path):
    return zlib.crc32(os.path.basename(path).encode()) & 0xFFFF


def _shade(c, f):
    """Lighten (f > 0, toward white) or darken (f < 0, toward black) an RGBA color."""
    r, g, b, a = c
    if f >= 0:
        return (int(r + (255 - r) * f), int(g + (255 - g) * f), int(b + (255 - b) * f), a)
    return (int(r * (1 + f)), int(g * (1 + f)), int(b * (1 + f)), a)


def _similar(a, b):
    if a[3] == 0 or b[3] == 0:
        return a[3] == b[3]
    return abs(a[0] - b[0]) + abs(a[1] - b[1]) + abs(a[2] - b[2]) < 48


def scale2x(px):
    """Scale2x (EPX) for RGBA pixel art: doubles the size and rounds off staircase edges."""
    h, w = len(px), len(px[0])
    out = [[None] * (w * 2) for _ in range(h * 2)]
    get = lambda x, y: px[min(max(y, 0), h - 1)][min(max(x, 0), w - 1)]
    for y in range(h):
        for x in range(w):
            p = px[y][x]
            a, b, c, d = get(x, y - 1), get(x + 1, y), get(x - 1, y), get(x, y + 1)
            e1 = e2 = e3 = e4 = p
            if _similar(c, a) and not _similar(c, d) and not _similar(a, b):
                e1 = a
            if _similar(a, b) and not _similar(a, c) and not _similar(b, d):
                e2 = b
            if _similar(d, c) and not _similar(d, b) and not _similar(c, a):
                e3 = c
            if _similar(b, d) and not _similar(b, a) and not _similar(d, c):
                e4 = d
            out[y * 2][x * 2], out[y * 2][x * 2 + 1], out[y * 2 + 1][x * 2], out[y * 2 + 1][x * 2 + 1] = e1, e2, e3, e4
    return out


def _lum(c):
    return (c[0] * 299 + c[1] * 587 + c[2] * 114) / 255000.0


def emboss(px, strength):
    """Raise features inside a sprite: where it gets brighter toward the top-left light, the edge catches light;
    where it gets darker, it falls into shadow. Gives carved, layered detail to gems, runes, guards and specks."""
    n_y, n_x = len(px), len(px[0])
    out = [row[:] for row in px]
    for y in range(1, n_y - 1):
        for x in range(1, n_x - 1):
            c = px[y][x]
            ul, dr = px[y - 1][x - 1], px[y + 1][x + 1]
            # Bright symbols, shine and page edges stay crisp
            if not (c[3] and ul[3] and dr[3]) or _lum(c) > 0.72:
                continue
            d = _lum(dr) - _lum(ul)
            if abs(d) > 0.04:
                out[y][x] = _shade(c, max(-0.35, min(0.35, d * strength)))
    return out


def hd_item(base, edge, path):
    seed = _seed(path)
    # 16x16 sprites are doubled with Scale2x; tomes are already drawn at 32x32 (tome_art.py)
    px = emboss(scale2x(base) if len(base) == N else base, 1.1)
    n = len(px)
    opaque = lambda x, y: 0 <= x < n and 0 <= y < n and px[y][x][3] > 0
    out = [row[:] for row in px]
    for y in range(n):
        for x in range(n):
            if not opaque(x, y):
                continue
            c = px[y][x]
            # Light from the top-left: rims facing it catch light, rims facing away fall into shadow
            if not opaque(x - 1, y) or not opaque(x, y - 1):
                c = _shade(c, 0.24)
            elif not opaque(x - 1, y - 1) or not opaque(x - 2, y) or not opaque(x, y - 2):
                c = _shade(c, 0.1)
            # Light colors (pages, shine, pale gems) only shade a little, so they don't go muddy
            light = _lum(px[y][x]) > 0.62
            if not opaque(x + 1, y) or not opaque(x, y + 1):
                c = _shade(c, -0.1 if light else -0.3)
            elif not opaque(x + 1, y + 1) or not opaque(x + 2, y) or not opaque(x, y + 2):
                c = _shade(c, -0.04 if light else -0.12)
            # Fine grain, skipped on near-white shine so highlights stay crisp
            if sum(c[:3]) < 690:
                c = _shade(c, 0.05 * _grain(seed, x, y))
            out[y][x] = c
    # Thin outline: a dark version of the color it wraps, tinted by the sprite's edge color
    final = [row[:] for row in out]
    for y in range(n):
        for x in range(n):
            if opaque(x, y):
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if opaque(x + dx, y + dy):
                    inner = _shade(out[y + dy][x + dx], -0.72)
                    final[y][x] = lerp(inner[:3], edge[:3], 0.5) + (255,)
                    break
    return final


def hd_block(px, path):
    """Blocks tile, so no outline or rim light: smoothed detail plus grain at 2x."""
    seed = _seed(path)
    up = emboss(scale2x(px), 0.8)
    return [[_shade(c, 0.045 * _grain(seed, x, y)) if c[3] else c for x, c in enumerate(row)] for y, row in enumerate(up)]


def hd_armor(px, path):
    """Worn armor: plain 2x (its UV layout must stay exact) with grain."""
    seed = _seed(path)
    # Drawn at 64x32 needs doubling; gear_art draws it at 128x64 already
    up = px if len(px[0]) >= 128 else [[c for c in row for _ in range(2)] for row in px for _ in range(2)]
    return [[_shade(c, 0.06 * _grain(seed, x, y)) if c[3] else c for x, c in enumerate(row)] for y, row in enumerate(up)]


def blank():
    return [[(0, 0, 0, 0) for _ in range(N)] for _ in range(N)]


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))


class Outlined(list):
    """A 16x16 sprite with a 1px outline, remembering the sprite underneath so the 32x32 pass can redraw the outline thinner."""

    def __init__(self, px, base, edge):
        super().__init__(px)
        self.base = base
        self.edge = edge


def outline(px, color):
    """Dark 1px outline around every opaque pixel, like vanilla items."""
    return Outlined(outline_px(px, color), px, color)


def outline_px(px, color):
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
            for m in re.finditer(r"^\s+(\w+): \{ name: '[^']*', enchant: '\w+', color: \[([\d, ]+)\]", text, re.M)}


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


def fusions():
    """[(id, parent A, parent B)] from the config's fusions list."""
    text = open(CONFIG).read()
    return re.findall(r"^\s+\['([a-z0-9_]+)', '[^']+', '([a-z0-9_]+)', '([a-z0-9_]+)'\]", text, re.M)


def triads():
    """[(id, parent A, parent B, parent C)] from the config's triads list."""
    text = open(CONFIG).read()
    return re.findall(r"\['(triad_\w+)', (?:'[^']*'|\"[^\"]*\"), '(\w+)', '(\w+)', '(\w+)'\]", text)


def fusion_book(pa, pb):
    """Diagonally split cover: parent A top-left, parent B bottom-right, with a bright seam and a two-tone gem."""
    px = book(pa[0], pa[1])
    other = book(pb[0], pb[1])
    for y in range(N):
        for x in range(N):
            if px[y][x][3] and x - 3 > y - 2 + 1 and other[y][x][3]:
                px[y][x] = other[y][x]
            if px[y][x][3] and 5 <= x <= 11 and x - 3 == y - 1:
                px[y][x] = (230, 250, 255, 255)
    for x, y in ((8, 6), (7, 7), (8, 7)):
        px[y][x] = pa[1] + (255,)
    for x, y in ((9, 7), (8, 8)):
        px[y][x] = pb[1] + (255,)
    return px


def condensed_tiers():
    """How many dense tiers the config names (the block list is generated from it)."""
    text = open(CONFIG).read()
    block = text[text.index("names: ['Dense'"):]
    block = block[:block.index("]")]
    return len(re.findall(r"'[^']+'", block))


def condenser_block():
    """Brass frame around a glass window of swirling essence."""
    px = [[None] * N for _ in range(N)]
    for y in range(N):
        for x in range(N):
            edge = x in (0, 1, 14, 15) or y in (0, 1, 14, 15)
            if edge:
                px[y][x] = lerp((200, 150, 60), (120, 80, 30), (x + y) / 30) + (255,)
            else:
                swirl = (math.sin(x * 0.9 + y * 0.5) + math.cos(y * 0.8 - x * 0.3)) / 4 + 0.5
                px[y][x] = lerp((60, 20, 110), (190, 110, 255), swirl) + (255,)
    for i in range(2, 14):
        px[i][i] = (235, 210, 255, 255)
    for x, y in ((0, 0), (15, 0), (0, 15), (15, 15), (7, 1), (8, 14)):
        px[y][x] = (255, 220, 120, 255)
    return px


def condensed_block(tier, tiers):
    """Denser tiers shift purple -> magenta -> gold -> white-hot -> cosmic black with stars."""
    f = tier / max(1, tiers)
    rnd = random.Random(1000 + tier)
    if f < 0.35:
        base = lerp((120, 50, 200), (210, 60, 170), f / 0.35)
    elif f < 0.6:
        base = lerp((210, 60, 170), (255, 180, 60), (f - 0.35) / 0.25)
    elif f < 0.78:
        base = lerp((255, 180, 60), (255, 245, 225), (f - 0.6) / 0.18)
    else:
        base = lerp((40, 10, 70), (6, 2, 14), (f - 0.78) / 0.22)
    cosmic = f >= 0.78
    dark = lerp(base, (0, 0, 0), 0.45)
    light = lerp(base, (255, 255, 255), 0.45)
    px = [[None] * N for _ in range(N)]
    for y in range(N):
        for x in range(N):
            v = rnd.random()
            px[y][x] = lerp(base, dark, v * 0.35) + (255,)
    # Veins: more of them the denser it is
    veins = 2 + int(f * 10)
    for _ in range(veins):
        x, y = rnd.randrange(N), rnd.randrange(N)
        for _ in range(6 + int(f * 8)):
            px[y][x] = (light if not cosmic else (190, 120, 255)) + (255,)
            x = max(0, min(N - 1, x + rnd.choice((-1, 0, 1))))
            y = max(0, min(N - 1, y + rnd.choice((-1, 0, 1))))
    if cosmic:
        for _ in range(4 + int(f * 10)):
            px[rnd.randrange(N)][rnd.randrange(N)] = (255, 255, 255, 255)
        for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
            px[y][x] = (255, 236, 180, 255)
    # Frame
    frame = lerp(base, (0, 0, 0), 0.6) if not cosmic else (120, 70, 200)
    for i in range(N):
        px[0][i] = px[N - 1][i] = px[i][0] = px[i][N - 1] = frame + (255,)
    return px


def soul():
    """A pale blue spirit flame with a bright core."""
    px = blank()
    for y in range(N):
        for x in range(N):
            dx, dy = (x - 7.5) / 4.2, (y - 9.5) / 5.5
            wob = 0.25 * math.sin(y * 1.3)
            if dx * dx + dy * dy <= 1 and y > 2 or (abs(x - 7.5 - wob * 3) < (y - 1) / 3.5 and 1 < y < 9):
                t = max(0, min(1, (y - 2) / 12))
                px[y][x] = lerp((230, 250, 255), (60, 140, 255), t) + (255,)
    for x, y in ((7, 9), (8, 9), (7, 10), (8, 10)):
        px[y][x] = (255, 255, 255, 255)
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
    # Names can be in single or double quotes ("Thunder God's Waltz")
    for m in re.finditer(r"^\s{4}(\w+):\s*\{ name: (?:'[^']*'|\"[^\"]*\"), type: '(\w+)'(.*)$", text, re.M):
        name, type_ = m.group(1), m.group(2)
        if "ultimate: true" in m.group(3):
            # Ultimate: black cover, crimson-gold gem (book() adds the gold trim)
            out[name] = ((14, 8, 10), tuple(int(c * 255) for c in colorsys.hsv_to_rgb((zlib.crc32(name.encode()) % 100) / 100.0 * 0.12, 0.8, 1.0)), 'ultimate')
            continue
        if "mythic: true" in m.group(3):
            # Mythic: near-black cover, bright gem, gold trim (see book())
            hue = TYPE_HUES.get(type_, 0.5)
            cover = tuple(int(c * 255) for c in colorsys.hsv_to_rgb(hue, 0.6, 0.18))
            gem_col = tuple(int(c * 255) for c in colorsys.hsv_to_rgb(hue, 0.65, 1.0))
            out[name] = (cover, gem_col, 'mythic')
            continue
        jitter = (zlib.crc32(name.encode()) % 1000) / 1000.0
        hue = (TYPE_HUES.get(type_, 0.5) + (jitter - 0.5) * 0.08) % 1.0
        val = 0.42 + jitter * 0.2
        cover = tuple(int(c * 255) for c in colorsys.hsv_to_rgb(hue, 0.75, val))
        gem_col = tuple(int(c * 255) for c in colorsys.hsv_to_rgb((hue + 0.5) % 1.0 if type_ == "eternal" else hue, 0.5, 1.0))
        out[name] = (cover, gem_col, 'ascended')
    return out



# ---- Ascendite: teal metal with a violet sheen ----
ASC_LIGHT, ASC_MID, ASC_DARK = (150, 245, 225), (45, 180, 175), (18, 88, 104)
ASC_VIOLET = (175, 115, 255)
ARMOR_OUT = os.path.join(os.path.dirname(__file__), "..", "kubejs", "assets", "kubejs", "textures", "models", "armor")


def asc(t):
    """Ascendite metal shade, t = 0 (bright) .. 1 (dark)."""
    return lerp(ASC_LIGHT, ASC_MID, min(1, t * 2)) if t < 0.5 else lerp(ASC_MID, ASC_DARK, (t - 0.5) * 2)


def from_mask(rows, shade):
    """16x16 sprite from an ASCII mask: '#' metal (shaded top-left to bottom-right), 'h' handle, 'v' violet, 'w' shine."""
    px = blank()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == '#':
                px[y][x] = shade((x + y) / 30.0) + (255,)
            elif ch == 'h':
                px[y][x] = lerp((137, 103, 62), (73, 54, 30), (x + y) / 30.0) + (255,)
            elif ch == 'v':
                px[y][x] = ASC_VIOLET + (255,)
            elif ch == 'w':
                px[y][x] = (235, 255, 250, 255)
    return px


ASC_SPRITES = {
    "ascendite_ingot": [
        "................", "................", "................", "................",
        "................", ".......www......", ".....w#####.....", "...w##########..",
        "..#############.", "..############v.", "...##########v..", ".....#######....",
        "................", "................", "................", "................"],
    "ascendite_nugget": [
        "................", "................", "................", "................",
        "................", "................", "......ww........", ".....w###.......",
        "....#####v......", "....######......", ".....####.......", "................",
        "................", "................", "................", "................"],
    "ascendite_sword": [
        "..............w#", ".............w##", "............w##.", "...........###..",
        "..........###...", ".........###....", "........###.....", ".......###......",
        "..v...###.......", "...v.###........", "....v##.........", ".....hv.........",
        "....h..v........", "...h............", ".hh.............", ".h.............."],
    "ascendite_pickaxe": [
        "................", "....w#####......", "...########.....", "..##.....h##....",
        "........h..##...", ".......h....#v..", "......h.....##..", ".....h.......#..",
        "....h........#..", "...h............", "..h.............", ".h..............",
        "................", "................", "................", "................"],
    "ascendite_axe": [
        "........w#......", ".......w###.....", "......#####v....", ".....######h....",
        ".....#####h##...", "......###h.##...", ".......#h.......", ".......h........",
        "......h.........", ".....h..........", "....h...........", "...h............",
        "..h.............", ".h..............", "................", "................"],
    "ascendite_shovel": [
        "................", "...........ww#..", "..........####..", ".........#####..",
        "..........###v..", ".........h##....", "........h.......", ".......h........",
        "......h.........", ".....h..........", "....h...........", "...h............",
        "..h.............", ".h..............", "................", "................"],
    "ascendite_hoe": [
        "................", "........ww#.....", ".......####v....", "..........#h....",
        ".........h......", "........h.......", ".......h........", "......h.........",
        ".....h..........", "....h...........", "...h............", "..h.............",
        ".h..............", "................", "................", "................"],
    "ascendite_helmet": [
        "................", "................", "................", "....w#######....",
        "...##########...", "..############..", "..####vvvv####..", "..###......###..",
        "..###......###..", "..##........##..", "................", "................",
        "................", "................", "................", "................"],
    "ascendite_chestplate": [
        "................", "..www....###....", ".####....####...", ".#####..#####...",
        ".##############.", "..############..", "...####vv####...", "...###vvvv###...",
        "...##########...", "...##########...", "...##########...", "...##########...",
        "....########....", "................", "................", "................"],
    "ascendite_leggings": [
        "................", "...w#########...", "...##vvvvvv##...", "...##########...",
        "...####..####...", "...###....###...", "...###....###...", "...###....###...",
        "...###....###...", "...###....###...", "...###....###...", "...###....###...",
        "................", "................", "................", "................"],
    "ascendite_boots": [
        "................", "................", "................", "................",
        "................", "................", "..w##......##w..", "..###......###..",
        "..###......###..", "..###......###..", ".####v....v####.", ".#####....#####.",
        ".#####....#####.", "................", "................", "................"],
}


def raw_ascendite():
    return cluster((95, 200, 190), (25, 95, 110), [ASC_LIGHT, ASC_VIOLET, (230, 255, 250)], 11)


def crushed_ascendite():
    rnd = random.Random(12)
    px = blank()
    for cx, cy, r in ((5, 6, 3.2), (10, 5, 2.6), (8, 11, 3.0), (12, 11, 2.0)):
        for y in range(N):
            for x in range(N):
                if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                    px[y][x] = asc((y - cy + r) / (2 * r) * 0.8 + 0.2 * rnd.random()) + (255,)
    for x, y in ((4, 5), (9, 4), (7, 10), (12, 10)):
        px[y][x] = (235, 255, 250, 255)
    px[12][8] = ASC_VIOLET + (255,)
    return px


def ascension_shard():
    """A slim violet-teal crystal splinter."""
    px = blank()
    for y in range(2, 15):
        w = 1 if y < 4 or y > 12 else 2
        cx = 4 + (y - 2) * 0.6
        for x in range(int(cx), int(cx) + w + 1):
            t = (y - 2) / 12.0
            px[15 - y][x] = lerp((240, 200, 255), ASC_VIOLET, t) + (255,) if x == int(cx) else lerp(ASC_LIGHT, ASC_MID, t) + (255,)
    px[3][11] = px[4][10] = (255, 255, 255, 255)
    return px


def ascendite_ore(deep):
    rnd = random.Random(21 if deep else 20)
    px = [[None] * N for _ in range(N)]
    for y in range(N):
        for x in range(N):
            v = rnd.random()
            if deep:
                base = lerp((70, 70, 78), (40, 40, 46), v * 0.8 + (0.2 if y % 4 == 0 else 0))
            else:
                base = lerp((140, 140, 140), (105, 105, 105), v)
            px[y][x] = base + (255,)
    for cx, cy in ((4, 4), (11, 5), (6, 11), (12, 12)):
        for dx, dy in ((0, 0), (1, 0), (0, 1), (-1, 0), (1, 1)):
            x, y = cx + dx, cy + dy
            if 0 <= x < N and 0 <= y < N:
                px[y][x] = asc(0.25 + 0.5 * rnd.random()) + (255,)
        px[cy][cx] = ASC_LIGHT + (255,)
    px[5][12] = px[12][7] = ASC_VIOLET + (255,)
    return px


def ascendite_storage_block():
    rnd = random.Random(22)
    px = [[None] * N for _ in range(N)]
    for y in range(N):
        for x in range(N):
            px[y][x] = asc(0.25 + 0.3 * (x + y) / 30.0 + 0.08 * rnd.random()) + (255,)
    for i in range(N):
        px[0][i] = px[i][0] = ASC_LIGHT + (255,)
        px[N - 1][i] = px[i][N - 1] = ASC_DARK + (255,)
    for i in range(3, 13):
        px[i][i] = lerp(ASC_VIOLET, ASC_LIGHT, 0.3) + (255,)
    return px


def armor_layer(layer):
    """64x32 worn-armor texture. Layer 1: helmet (head), chestplate (body + arms), boots (bottom of legs).
    Layer 2: leggings (waist + legs). Unused parts of the sheet stay transparent."""
    W, H = 64, 32
    rnd = random.Random(30 + layer)
    px = [[(0, 0, 0, 0)] * W for _ in range(H)]

    def fill(x0, y0, x1, y1, rows=None):
        for y in range(y0, y1):
            if rows and not rows(y):
                continue
            for x in range(x0, x1):
                t = 0.25 + 0.45 * (y - y0) / max(1, y1 - y0) + 0.1 * rnd.random()
                c = asc(t)
                if y in (y0, y1 - 1) or x in (x0, x1 - 1):
                    c = lerp(c, ASC_DARK, 0.5)
                px[y][x] = c + (255,)

    if layer == 1:
        fill(0, 0, 32, 16)                      # head box
        for y in range(8, 16):                  # visor slit on the face
            if y == 12:
                for x in range(9, 15):
                    px[y][x] = (20, 30, 40, 255)
        fill(16, 16, 40, 32)                    # body
        fill(40, 16, 56, 32)                    # arms
        fill(0, 16, 16, 32, rows=lambda y: y >= 26 or y < 20)   # boots: lower legs + soles
        for x in range(20, 28):                 # violet chest gem
            px[24][x] = ASC_VIOLET + (255,)
    else:
        fill(16, 16, 40, 28)                    # waist / hips
        fill(0, 16, 16, 30)                     # legs
        for x in range(20, 28):
            px[17][x] = ASC_VIOLET + (255,)     # belt
    return px


def _room_config():
    cfg = open(os.path.join(os.path.dirname(__file__), "..", "kubejs", "startup_scripts", "00_fa_config.js")).read()
    i = cfg.index("roomCharges: {")
    return cfg[i:cfg.index("\n  },", i)]


def room_styles():
    """(id, wall color, light color) for each roomCharges style in the config."""
    import re
    return [(m.group(1), tuple(int(v) for v in m.group(2).split(",")), tuple(int(v) for v in m.group(3).split(",")))
            for m in re.finditer(r"\{ id: '(\w+)'.*?color: \[([\d, ]+)\], lightColor: \[([\d, ]+)\]", _room_config())]


def room_sizes():
    import re
    return re.findall(r"\{ id: '(\d+)', size:", _room_config())


def ascendite_textures(textures):
    for name, rows in ASC_SPRITES.items():
        textures[name] = outline(from_mask(rows, asc), (8, 40, 48, 255))
    # Tools and armor: the detailed 32x32 designs from gear_art.py (gunmetal, teal energy, gold trim, violet gems)
    for kind, draw in gear_art.ICONS.items():
        px = draw()
        textures["ascendite_" + kind] = Outlined(px, px, (10, 12, 20, 255))
    # Higher gear tiers: the same designs in each tier's colors, plus its ingot
    for tier, palette in gear_art.TIER_PALETTES.items():
        for kind, draw in gear_art.ICONS.items():
            px = gear_art.with_palette(palette, draw)
            textures[tier + "_" + kind] = Outlined(px, px, (10, 12, 20, 255))
        px = gear_art.with_palette(palette, gear_art.ingot)
        textures[tier + "_ingot"] = Outlined(px, px, (10, 12, 20, 255))
    for name, draw in gear_art.ATTACHMENTS.items():
        px = draw()
        textures["attachment_" + name] = Outlined(px, px, (10, 12, 20, 255))
    for name, draw in gear_art.HAMMERS.items():
        px = draw()
        textures[name] = Outlined(px, px, (10, 12, 20, 255))
    # Factory Room TNT: one icon per style and size, from the config's roomCharges
    for style, shell, light in room_styles():
        for i, size in enumerate(room_sizes()):
            px = gear_art.room_tnt(shell, light, i)
            textures["room_tnt_%s_%s" % (style, size)] = Outlined(px, px, (24, 8, 6, 255))
    textures["raw_ascendite"] = outline(raw_ascendite(), (8, 40, 48, 255))
    textures["crushed_raw_ascendite"] = outline(crushed_ascendite(), (8, 40, 48, 255))
    textures["ascension_shard"] = outline(ascension_shard(), (30, 10, 50, 255))
    png(os.path.join(BLOCK_OUT, "ascendite_ore.png"), ascendite_ore(False))
    png(os.path.join(BLOCK_OUT, "deepslate_ascendite_ore.png"), ascendite_ore(True))
    png(os.path.join(BLOCK_OUT, "ascendite_block.png"), ascendite_storage_block())
    os.makedirs(ARMOR_OUT, exist_ok=True)
    for layer in (1, 2):
        png(os.path.join(ARMOR_OUT, "ascendite_layer_%d.png" % layer), gear_art.armor_layer(layer))
        for tier, palette in gear_art.TIER_PALETTES.items():
            png(os.path.join(ARMOR_OUT, "%s_layer_%d.png" % (tier, layer)), gear_art.with_palette(palette, gear_art.armor_layer, layer))
    print("wrote Ascendite ore, block, armor layers and %d items" % (len(ASC_SPRITES) + 3))


MODEL_OUT = os.path.join(os.path.dirname(__file__), "..", "kubejs", "assets", "kubejs", "models", "item")


def write_tome_models(tome_ids, foil_ids):
    """Each tome's model picks a foil overlay by CustomModelData (1 = first foil ...). The overlay is layer1 over the tome."""
    import json
    for f in foil_ids:
        os.makedirs(os.path.join(MODEL_OUT, "foil", f), exist_ok=True)
    for tid in tome_ids:
        name = "tome_" + tid
        base = {"parent": "minecraft:item/generated", "textures": {"layer0": "kubejs:item/" + name},
                "overrides": [{"predicate": {"custom_model_data": i + 1}, "model": "kubejs:item/foil/%s/%s" % (f, name)} for i, f in enumerate(foil_ids)]}
        with open(os.path.join(MODEL_OUT, name + ".json"), "w") as out:
            json.dump(base, out, separators=(",", ":"))
        for f in foil_ids:
            with open(os.path.join(MODEL_OUT, "foil", f, name + ".json"), "w") as out:
                json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "kubejs:item/" + name, "layer1": "kubejs:item/foil_overlay_" + f}},
                          out, separators=(",", ":"))
    print("wrote %d tome models with %d foil looks each" % (len(tome_ids), len(foil_ids)))


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
    # Tomes: drawn at 32x32 by tome_art.py, every one a different design
    tome32 = lambda px: Outlined(px, px, edge)
    textures["blank_ascension_tome"] = tome32(tome_art.blank_tome())
    tomes = tome_colors()
    if len(tomes) < 9:
        raise SystemExit('could not read enchants from ' + CONFIG)
    masks = {}
    for name in tomes:
        m = tuple(tuple(r) for r in tome_art.emblem_mask(name, 14))
        if m in masks:
            raise SystemExit('emblems for %s and %s are identical' % (masks[m], name))
        masks[m] = name
    missing = [n for n in tomes if n not in tome_art.EMBLEMS]
    if missing:
        print('no designed emblem (using a rune) for: ' + ', '.join(missing))
    for name, (cover, gem_color, kind) in tomes.items():
        textures["tome_" + name] = tome32(tome_art.tome(name, cover, gem_color, kind))
    # Fusion tomes: split cover, each half in a parent's color with its emblem
    for fid, a, b in fusions():
        textures["tome_" + fid] = tome32(tome_art.fusion_tome(a, b, tomes[a], tomes[b]))
    # Triad tomes: three bands with the three parents' colors and emblems
    for tid, a, b, c in triads():
        textures["tome_" + tid] = tome32(tome_art.triad_tome([a, b, c], [tomes[a], tomes[b], tomes[c]]))
    # Foils: an overlay per foil (the tome model's second layer) and a Foil Sheet icon each
    foil_ids = re.findall(r"\{ id: '(\w+)', name: '[^']*', mult: [\d.]+", open(CONFIG).read())
    if len(foil_ids) < 8:
        raise SystemExit('could not read foils from ' + CONFIG)
    for f in foil_ids:
        png(os.path.join(OUT, "foil_overlay_" + f + ".png"), tome_art.foil_overlay(f))
        sheet = tome_art.foil_sheet(f)
        textures["foil_sheet_" + f] = Outlined(sheet, sheet, edge)
    write_tome_models([n[5:] for n in textures if n.startswith("tome_")], foil_ids)
    seen = {}
    for name, px in textures.items():
        if name.startswith("tome_"):
            key = tuple(tuple(r) for r in px)
            if key in seen:
                raise SystemExit('tome textures %s and %s are identical' % (seen[key], name))
            seen[key] = name
    textures["mythic_fragment"] = outline(fragment(), (30, 8, 40, 255))
    textures["ascended_soul"] = outline(soul(), (10, 20, 40, 255))
    # Mob food: raw sprite + its roasted version, names from the config
    # A food line may name its sprite: art: ['shape', [main rgb], [accent rgb]] (shapes in food_art.SHAPES)
    foods = []
    for line in open(CONFIG).read().splitlines():
        m = re.search(r"\{ raw: '(\w+)', rawName: (?:\"[^\"]*\"|'[^']*'), cooked: '(\w+)'", line)
        if not m:
            continue
        art = re.search(r"art: \['(\w+)', \[([\d, ]+)\], \[([\d, ]+)\]\]", line)
        foods.append((m.group(1), m.group(2), art and (art.group(1), tuple(int(v) for v in art.group(2).split(',')), tuple(int(v) for v in art.group(3).split(',')))))
    if len(foods) < 13:
        raise SystemExit('could not read mobFood foods from ' + CONFIG)
    for i, (raw, done, art) in enumerate(foods):
        if art:
            px = food_art.shape(art[0], art[1], art[2], i)
        elif raw in food_art.RAW:
            px = food_art.RAW[raw]()
        else:
            raise SystemExit('no sprite for mob food ' + raw + ' (give it art: in the config, or a function in tools/food_art.py)')
        textures[raw] = outline(px, (40, 16, 20, 255))
        textures[done] = outline(food_art.cooked(px, i), (36, 20, 10, 255))
    textures["chaos_tome"] = Outlined(tome_art.chaos_tome(), tome_art.chaos_tome(), edge)
    gems = gem_colors()
    if len(gems) < 6:
        raise SystemExit('could not read gem colors from ' + CONFIG)
    for name, color in gems.items():
        for tier in (1, 2, 3):
            textures["gem_%s_%d" % (name, tier)] = outline(gem_item(color, tier), (20, 16, 24, 255))
    os.makedirs(BLOCK_OUT, exist_ok=True)
    ascendite_textures(textures)
    png(os.path.join(BLOCK_OUT, "ascension_altar.png"), altar_block())
    png(os.path.join(BLOCK_OUT, "essence_condenser.png"), condenser_block())
    tiers = condensed_tiers()
    for tier in range(tiers + 1):
        png(os.path.join(BLOCK_OUT, "condensed_essence_%d.png" % tier), condensed_block(tier, tiers))
    print("wrote block/ascension_altar.png, essence_condenser.png and %d condensed essence blocks" % (tiers + 1))
    for name, px in textures.items():
        png(os.path.join(OUT, name + ".png"), px)
        print("wrote", name + ".png")


if __name__ == "__main__":
    main()
