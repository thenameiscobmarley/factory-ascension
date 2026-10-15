#!/usr/bin/env python3
"""Pixel art for the website, in the style of the pack's tome textures: leather covers with a dark spine and metal
clasps on the left, cream page edges on the right and bottom, gold trim for the special ones, and a dark wood desk.

Writes site/art/*.png. The page scales every art pixel up 3x (see --px in site.css).
Run from the repo root:  python3 tools/site_art.py
"""
import os
import sys
import zlib

sys.path.insert(0, os.path.dirname(__file__))
import make_textures  # noqa: E402  (its png() writes plain PNGs for paths outside block/ and armor/)

OUT = os.path.join(os.path.dirname(__file__), "..", "site", "art")

# Leather: (light, mid, dark); the same hues as the tome covers
LEATHER = {
    "red": ((168, 62, 70), (124, 36, 46), (74, 18, 28)),
    "brown": ((160, 104, 62), (118, 72, 40), (70, 40, 22)),
    "teal": ((58, 118, 122), (30, 78, 84), (14, 42, 48)),
    "purple": ((116, 74, 160), (74, 44, 110), (40, 22, 64)),
    "green": ((86, 128, 62), (54, 88, 38), (28, 50, 20)),
    "steel": ((104, 112, 132), (60, 66, 82), (30, 33, 42)),
}
PAGE = ((240, 228, 196), (214, 196, 156), (168, 146, 106))
METAL = ((226, 226, 236), (170, 172, 186), (96, 98, 112))
GOLD = ((255, 230, 140), (232, 176, 58), (140, 84, 22))


def grain(seed, x, y):
    h = (x * 374761393 + y * 668265263 + seed * 2246822519) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFFFF) / 32767.5 - 1


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def tone(pal, t):
    """0 = light, 0.5 = mid, 1 = dark."""
    t = max(0.0, min(1.0, t))
    return mix(pal[0], pal[1], t * 2) if t < 0.5 else mix(pal[1], pal[2], (t - 0.5) * 2)


def leather(pal, seed, x, y):
    # Mottled leather: two scales of noise, like the tome covers
    n = 0.5 + 0.10 * grain(seed, x, y) + 0.08 * grain(seed + 7, x // 3, y // 3)
    return tone(pal, n)


# A cover is 32 x 32 art pixels, sliced for CSS border-image: spine 9 (left), pages 5 (right), top 3, pages 6 (bottom).
W = H = 32
SPINE, RIGHT, TOP, BOTTOM = 9, 5, 3, 6


def cover(name):
    pal = LEATHER[name]
    seed = zlib.crc32(name.encode()) & 0xFFFF
    px = [[leather(pal, seed, x, y) + (255,) for x in range(W)] for y in range(H)]
    for y in range(H):
        for x in range(W):
            if x < SPINE:                                     # spine: darker, with a raised band
                c = tone(pal, 0.72 + 0.06 * grain(seed + 3, x, y))
                if x in (1, SPINE - 2):
                    c = tone(pal, 0.9)
                if x == SPINE - 1:
                    c = tone(pal, 0.98)                       # the fold
                px[y][x] = c + (255,)
            if x >= W - RIGHT and y < H - 1:                  # page edges, right
                if x == W - RIGHT:
                    px[y][x] = tone(pal, 0.95) + (255,)
                else:
                    px[y][x] = tone(PAGE, 0.25 + (0.35 if (x + y // 2) % 2 else 0)) + (255,)
            if y >= H - BOTTOM and x >= SPINE - 1:            # page edges, bottom
                if y == H - BOTTOM:
                    px[y][x] = tone(pal, 0.95) + (255,)
                elif y == H - 1:
                    px[y][x] = tone(pal, 0.9) + (255,)
                else:
                    px[y][x] = tone(PAGE, 0.2 + (0.4 if (y + x // 2) % 2 else 0)) + (255,)
            if y < TOP and x >= SPINE - 1 and x < W - RIGHT:  # lit top edge
                px[y][x] = tone(pal, 0.2 if y == 0 else 0.35) + (255,)
    # Metal clasps on the spine (they repeat along the edge, like studs)
    for cy in (6, 22):
        for y in range(cy, cy + 4):
            for x in range(2, SPINE - 2):
                edge = y in (cy, cy + 3) or x in (2, SPINE - 3)
                px[y][x] = tone(METAL, 0.75 if edge else 0.25 + 0.1 * (x - 2)) + (255,)
        px[cy + 1][3] = (255, 255, 255, 255)
    # Outline, like the item textures
    for x in range(W):
        px[0][x] = (24, 14, 10, 255)
        px[H - 1][x] = (24, 14, 10, 255)
    for y in range(H):
        px[y][0] = (24, 14, 10, 255)
        px[y][W - 1] = (24, 14, 10, 255)
    return px


def gold_frame():
    """12 x 12, sliced 4: the gold trim the rare tomes have, with corner studs."""
    n = 12
    px = [[(0, 0, 0, 0)] * n for _ in range(n)]
    for y in range(n):
        for x in range(n):
            d = min(x, y, n - 1 - x, n - 1 - y)
            if d == 0:
                px[y][x] = (40, 22, 6, 255)
            elif d == 1:
                px[y][x] = tone(GOLD, 0.15 if (x < n / 2 and y < n / 2) else 0.55) + (255,)
            elif d == 2:
                px[y][x] = tone(GOLD, 0.8) + (255,)
    for cx, cy in ((1, 1), (n - 3, 1), (1, n - 3), (n - 3, n - 3)):   # studs
        for y in range(cy, cy + 2):
            for x in range(cx, cx + 2):
                px[y][x] = tone(GOLD, 0.0 if (x == cx and y == cy) else 0.4) + (255,)
    return px


def desk():
    """16 x 16 dark wood planks for the page background."""
    pal = ((70, 44, 28), (44, 28, 18), (22, 14, 9))
    px = []
    for y in range(16):
        row = []
        for x in range(16):
            plank = y // 4
            t = 0.62 + 0.08 * grain(9 + plank, x // 2, y) + 0.05 * grain(3, x, plank)
            if y % 4 == 3:
                t = 0.95                                       # gap between planks
            if (x + plank * 5) % 16 == 0 and y % 4 != 3:
                t = 0.9                                        # plank ends
            row.append(tone(pal, t) + (255,))
        px.append(row)
    return px


# ---- The "Tome" theme: covers like the enchant tomes themselves ----
GEM = ((245, 214, 255), (170, 96, 255), (78, 30, 150))
TEAL = ((210, 255, 246), (64, 232, 212), (16, 150, 150))


def tome_cover(name):
    """A tome cover: richer leather, a gold foil frame inset on the cover with violet gem corners, a glowing teal rune
    band down the spine, gold clasps and gilt page edges."""
    px = cover(name)
    # The middle of a cover repeats to fill the panel, so the gold foil frame goes on the edges between the slices:
    # along the spine's fold, under the top edge, above the bottom page edges and beside the right ones
    for x in range(SPINE - 1, W - RIGHT + 1):
        px[TOP - 1][x] = tone(GOLD, 0.2) + (255,)
        px[H - BOTTOM][x] = tone(GOLD, 0.55) + (255,)
    for y in range(TOP - 1, H - BOTTOM + 1):
        px[y][SPINE - 1] = tone(GOLD, 0.35) + (255,)
        px[y][W - RIGHT] = tone(GOLD, 0.6) + (255,)
    for cx, cy in ((SPINE - 2, TOP - 2), (W - RIGHT - 1, TOP - 2), (SPINE - 2, H - BOTTOM - 1), (W - RIGHT - 1, H - BOTTOM - 1)):
        for y in range(cy, cy + 2):
            for x in range(cx, cx + 2):
                if 0 < x < W - 1 and 0 < y < H - 1:
                    px[y][x] = tone(GEM, 0.1 if (x == cx and y == cy) else 0.55) + (255,)
    for y in range(2, H - 2):                                  # rune band on the spine
        if y % 5 != 0:
            px[y][4] = tone(TEAL, 0.25 + 0.2 * ((y // 5) % 2)) + (255,)
    for cy in (6, 22):                                         # gold clasps
        for y in range(cy, cy + 4):
            for x in range(2, SPINE - 2):
                if x != 4:
                    px[y][x] = tone(GOLD, 0.7 if (y in (cy, cy + 3) or x in (2, SPINE - 3)) else 0.3) + (255,)
    for y in range(H - 1):                                     # gilt page edges
        for x in range(W - RIGHT + 1, W - 1):
            if (x + y) % 3 == 0:
                px[y][x] = tone(GOLD, 0.35) + (255,)
    return px


def tome_desk():
    """16 x 16 deep violet velvet with a faint gold rune lattice, for the Tome theme's page background."""
    pal = ((60, 34, 84), (34, 18, 50), (16, 8, 26))
    px = []
    for y in range(16):
        row = []
        for x in range(16):
            t = 0.6 + 0.07 * grain(41, x, y) + 0.05 * grain(42, x // 2, y // 2)
            c = tone(pal, t)
            if (x + y) % 8 == 0 or (x - y) % 8 == 0:
                c = mix(c, GOLD[1], 0.12)
            row.append(c + (255,))
        px.append(row)
    return px


def main():
    os.makedirs(OUT, exist_ok=True)
    for name in LEATHER:
        make_textures.png(os.path.join(OUT, "tome_cover_%s.png" % name), tome_cover(name))
    make_textures.png(os.path.join(OUT, "tome_desk.png"), tome_desk())
    for name in LEATHER:
        make_textures.png(os.path.join(OUT, "cover_%s.png" % name), cover(name))
    make_textures.png(os.path.join(OUT, "frame_gold.png"), gold_frame())
    make_textures.png(os.path.join(OUT, "desk.png"), desk())
    print("wrote %d covers, the gold frame and the desk to site/art" % len(LEATHER))


if __name__ == "__main__":
    main()
