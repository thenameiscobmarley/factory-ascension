"""Mob food sprites for Factory Ascension: 13 raw animal parts, and a roasted version of each.

Each raw sprite is a few layers of simple shapes (from tome_art), each layer in its own color, drawn at 16x16
(make_textures.py then renders it at 32x32 with its detail pass). Cooked sprites are the raw ones roasted:
flesh browns, fat and skin turn golden, and a few char marks appear.
"""
import math
import random

from tome_art import add, cut, arc, circ, ell, poly, raster, rect, seg

N = 16

FLESH, FLESH_DK, FAT = (222, 112, 124), (150, 42, 58), (244, 222, 210)
BONE, BONE_DK = (236, 226, 196), (180, 166, 132)


def _layers(spec, seed):
    """spec: [(color, shape list)], later layers on top. A little per-pixel variation keeps it organic."""
    rnd = random.Random(seed)
    px = [[(0, 0, 0, 0)] * N for _ in range(N)]
    for color, shapes in spec:
        mask = raster(shapes, N)
        for y in range(N):
            for x in range(N):
                if mask[y][x]:
                    j = rnd.uniform(-0.06, 0.06)
                    px[y][x] = tuple(max(0, min(255, int(c * (1 + j)))) for c in color) + (255,)
    return px


def _veins(px, color, pts):
    for x, y in pts:
        if px[y][x][3]:
            px[y][x] = color + (255,)
    return px


def pig_eyes():
    px = _layers([
        ((246, 240, 236), add(circ(0.3, 0.55, 0.22), circ(0.72, 0.42, 0.22))),
        ((120, 70, 50), add(circ(0.33, 0.55, 0.11), circ(0.75, 0.42, 0.11))),
        ((20, 14, 18), add(circ(0.34, 0.56, 0.05), circ(0.76, 0.43, 0.05))),
        (FLESH, add(arc(0.3, 0.55, 0.22, 100, 200, 0.07), arc(0.72, 0.42, 0.22, 60, 140, 0.07))),
    ], 1)
    return _veins(px, (210, 60, 70), [(2, 8), (3, 10), (13, 5), (12, 4)])


def pig_brain():
    return _layers([
        ((236, 150, 170), add(ell(0.5, 0.52, 0.42, 0.32))),
        ((190, 92, 120), add(arc(0.36, 0.44, 0.14, 180, 360, 0.06), arc(0.62, 0.42, 0.13, 160, 340, 0.06), arc(0.44, 0.66, 0.13, 0, 180, 0.06),
                             arc(0.7, 0.62, 0.12, 200, 380, 0.06), seg(0.5, 0.22, 0.5, 0.8, 0.05))),
        ((250, 196, 210), add(circ(0.3, 0.36, 0.05), circ(0.64, 0.32, 0.04))),
    ], 2)


def cow_tongue():
    return _layers([
        ((214, 100, 130), add(arc(0.4, 0.4, 0.34, 330, 500, 0.3), circ(0.74, 0.76, 0.14))),
        ((176, 66, 98), add(arc(0.4, 0.4, 0.34, 340, 490, 0.05))),
        ((236, 150, 170), add(circ(0.2, 0.3, 0.05), circ(0.64, 0.82, 0.04))),
    ], 3)


def cow_heart():
    return _layers([
        ((168, 34, 52), add(circ(0.4, 0.5, 0.26), circ(0.62, 0.5, 0.24), poly([(0.16, 0.56), (0.86, 0.56), (0.5, 0.94)]))),
        ((210, 200, 190), add(rect(0.34, 0.08, 0.44, 0.34), rect(0.54, 0.12, 0.62, 0.34))),
        ((110, 16, 32), add(seg(0.5, 0.42, 0.42, 0.78, 0.06), seg(0.62, 0.5, 0.7, 0.7, 0.05))),
        ((230, 90, 100), add(circ(0.32, 0.42, 0.06))),
    ], 4)


def sheep_liver():
    return _layers([
        ((112, 36, 34), add(ell(0.38, 0.5, 0.34, 0.26), ell(0.68, 0.56, 0.26, 0.22))),
        ((84, 22, 24), add(seg(0.52, 0.3, 0.56, 0.78, 0.05))),
        ((158, 70, 64), add(circ(0.28, 0.42, 0.06), circ(0.68, 0.48, 0.05))),
    ], 5)


def chicken_feet():
    toes = lambda x, y: add(seg(x, y, x, y - 0.42, 0.11), seg(x, y - 0.12, x - 0.18, y + 0.1, 0.09), seg(x, y - 0.12, x + 0.18, y + 0.1, 0.09),
                            seg(x, y - 0.12, x, y + 0.14, 0.09))
    return _layers([
        ((236, 178, 60), toes(0.32, 0.72) + toes(0.7, 0.62)),
        ((190, 120, 30), add(circ(0.32, 0.6, 0.05), circ(0.7, 0.5, 0.05))),
        ((70, 50, 40), add(circ(0.14, 0.82, 0.03), circ(0.5, 0.82, 0.03), circ(0.52, 0.72, 0.03), circ(0.88, 0.72, 0.03))),
    ], 6)


def rabbit_leg():
    return _layers([
        (BONE, add(seg(0.2, 0.84, 0.46, 0.56, 0.12), circ(0.16, 0.88, 0.07), circ(0.24, 0.9, 0.06))),
        ((206, 120, 120), add(ell(0.62, 0.38, 0.28, 0.24))),
        ((150, 112, 80), add(arc(0.62, 0.38, 0.28, 200, 330, 0.12), circ(0.82, 0.22, 0.08))),
        ((240, 170, 170), add(circ(0.56, 0.36, 0.05))),
    ], 7)


def spider_legs():
    leg = lambda y, flip: add(seg(0.1, y, 0.46, y - 0.18 * flip, 0.09), seg(0.46, y - 0.18 * flip, 0.9, y + 0.06 * flip, 0.08))
    return _layers([
        ((62, 46, 56), leg(0.3, 1) + leg(0.6, 1) + leg(0.88, 1)),
        ((180, 20, 30), add(circ(0.9, 0.36, 0.04), circ(0.9, 0.66, 0.04), circ(0.9, 0.94, 0.04))),
        ((120, 100, 110), add(circ(0.46, 0.12, 0.03), circ(0.46, 0.42, 0.03), circ(0.46, 0.7, 0.03))),
    ], 8)


def zombie_brain():
    return _layers([
        ((150, 168, 120), add(ell(0.5, 0.52, 0.42, 0.32))),
        ((96, 120, 72), add(arc(0.36, 0.44, 0.14, 180, 360, 0.06), arc(0.62, 0.42, 0.13, 160, 340, 0.06), arc(0.44, 0.66, 0.13, 0, 180, 0.06),
                            seg(0.5, 0.22, 0.5, 0.8, 0.05))),
        ((60, 90, 44), add(circ(0.7, 0.62, 0.08), circ(0.26, 0.6, 0.06))),
        ((120, 30, 40), add(circ(0.62, 0.3, 0.04))),
    ], 9)


def marrow_bone():
    return _layers([
        (BONE, add(seg(0.22, 0.78, 0.74, 0.26, 0.2), circ(0.16, 0.74, 0.1), circ(0.26, 0.86, 0.1))),
        (BONE, add(ell(0.78, 0.22, 0.2, 0.2))),
        ((196, 52, 66), add(ell(0.78, 0.22, 0.12, 0.12))),
        ((240, 120, 120), add(circ(0.74, 0.18, 0.04))),
        (BONE_DK, add(seg(0.28, 0.82, 0.7, 0.4, 0.04))),
    ], 10)


def creeper_heart():
    return _layers([
        ((92, 186, 76), add(circ(0.36, 0.42, 0.24), circ(0.64, 0.42, 0.24), poly([(0.12, 0.5), (0.88, 0.5), (0.5, 0.92)]))),
        ((40, 104, 40), add(rect(0.3, 0.36, 0.4, 0.46), rect(0.6, 0.36, 0.7, 0.46), rect(0.42, 0.5, 0.58, 0.62), rect(0.36, 0.58, 0.44, 0.72), rect(0.56, 0.58, 0.64, 0.72))),
        ((170, 230, 140), add(circ(0.28, 0.32, 0.05))),
    ], 11)


def squid_tentacle():
    return _layers([
        ((58, 72, 104), add(arc(0.42, 0.56, 0.3, 200, 470, 0.2), circ(0.64, 0.84, 0.08))),
        ((180, 200, 220), add(*[circ(0.42 + 0.3 * math.cos(math.radians(a)), 0.56 + 0.3 * math.sin(math.radians(a)), 0.04) for a in (220, 270, 320, 370, 420)])),
    ], 12)


def hoglin_ribs():
    return _layers([
        ((168, 82, 70), add(rect(0.12, 0.32, 0.88, 0.74))),
        (BONE, add(*[seg(0.18 + k * 0.2, 0.18, 0.18 + k * 0.2, 0.86, 0.08) for k in range(4)])),
        ((120, 50, 44), add(rect(0.12, 0.62, 0.88, 0.74))),
    ], 13)


RAW = {
    'pig_eyes': pig_eyes, 'pig_brain': pig_brain, 'cow_tongue': cow_tongue, 'cow_heart': cow_heart, 'sheep_liver': sheep_liver,
    'chicken_feet': chicken_feet, 'rabbit_leg': rabbit_leg, 'spider_legs': spider_legs, 'zombie_brain': zombie_brain,
    'marrow_bone': marrow_bone, 'creeper_heart': creeper_heart, 'squid_tentacle': squid_tentacle, 'hoglin_ribs': hoglin_ribs,
}


def cooked(px, seed):
    """Roast a raw sprite: reds and pinks brown, pale parts turn golden, a few char marks and a glazed highlight."""
    rnd = random.Random(seed)
    out = [row[:] for row in px]
    for y in range(N):
        for x in range(N):
            r, g, b, a = px[y][x]
            if not a:
                continue
            lum = (r * 3 + g * 6 + b) / 2550.0
            roast = (int(150 + 90 * lum), int(78 + 80 * lum), int(30 + 40 * lum))
            c = tuple(int(px[y][x][i] * 0.3 + roast[i] * 0.7) for i in range(3))
            if (r, g, b) in ((20, 14, 18),):
                c = (r, g, b)
            if rnd.random() < 0.08:
                c = tuple(int(v * 0.55) for v in c)   # char
            out[y][x] = c + (255,)
    # Glaze: a warm highlight on the top-left of the shape
    for y in range(N):
        for x in range(N):
            if out[y][x][3] and (y == 0 or not out[y - 1][x][3]) and x < 10:
                out[y][x] = tuple(min(255, int(v * 1.25 + 20)) for v in out[y][x][:3]) + (255,)
    return out


def textures():
    """{texture name: 16x16 px} for every raw part and its cooked version. Cooked names come from the config."""
    return {name: fn() for name, fn in RAW.items()}
