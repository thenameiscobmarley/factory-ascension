"""64x64 textures for the guns: metals, polymers, woods, rubber, rails, glass. Each is even enough that any patch of
it can go on any face (the models pick random patches), with fine detail so up close it reads as a real surface."""
import math
import random

N = 64


def clamp(v):
    return max(0, min(255, int(round(v))))


def rgb(c, f=0.0):
    if f >= 0:
        return tuple(clamp(c[i] + (255 - c[i]) * f) for i in range(3)) + (255,)
    return tuple(clamp(c[i] * (1 + f)) for i in range(3)) + (255,)


def value_noise(seed, scale):
    """Smooth noise in -1..1, tiling every N pixels."""
    rnd = random.Random(seed)
    cells = max(1, N // scale)
    grid = [[rnd.uniform(-1, 1) for _ in range(cells)] for _ in range(cells)]

    def at(x, y):
        fx, fy = x / scale, y / scale
        x0, y0 = int(math.floor(fx)) % cells, int(math.floor(fy)) % cells
        x1, y1 = (x0 + 1) % cells, (y0 + 1) % cells
        tx, ty = fx - math.floor(fx), fy - math.floor(fy)
        tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
        a = grid[y0][x0] + (grid[y0][x1] - grid[y0][x0]) * tx
        b = grid[y1][x0] + (grid[y1][x1] - grid[y1][x0]) * tx
        return a + (b - a) * ty
    return at


def surface(base, seed, grain=0.05, blotch=0.06, brush=0.0, speck=0.0, scale=16):
    rnd = random.Random(seed)
    n1 = value_noise(seed, scale)
    n2 = value_noise(seed + 7, 4)
    rows = [rnd.uniform(-1, 1) for _ in range(N)]
    px = []
    for y in range(N):
        row = []
        for x in range(N):
            f = n1(x, y) * blotch + n2(x, y) * grain * 0.6 + rnd.uniform(-grain, grain) * 0.6 + rows[y] * brush
            if speck and rnd.random() < speck:
                f += rnd.choice((-0.08, 0.07))
            row.append(rgb(base, f))
        px.append(row)
    return px


def wood(base, seed, figure=1.0):
    rnd = random.Random(seed)
    ph = [rnd.uniform(0, 6.28) for _ in range(4)]
    n = value_noise(seed, 16)
    px = []
    for y in range(N):
        row = []
        for x in range(N):
            g = math.sin(y * 0.55 + 2.0 * math.sin(x * 0.07 + ph[0]) + 1.2 * n(x, y) + ph[1])
            fig = math.sin(y * 0.21 + 3.0 * math.sin(x * 0.05 + ph[2]))
            f = 0.09 * g - (0.14 * figure if fig > 0.94 else 0) + rnd.uniform(-0.025, 0.025) + 0.05 * n(x, y)
            row.append(rgb(base, f))
        px.append(row)
    return px


def checker(base, seed, size=4, depth=0.18):
    rnd = random.Random(seed)
    px = []
    for y in range(N):
        row = []
        for x in range(N):
            a, b = (x + y) % size, (x - y) % size
            f = (depth if a == 0 or b == 0 else -depth * 0.3) + rnd.uniform(-0.03, 0.03)
            row.append(rgb(base, f))
        px.append(row)
    return px


def knurl(base, seed):
    return checker(base, seed, size=3, depth=0.3)


def rail(base, seed):
    rnd = random.Random(seed)
    px = []
    for y in range(N):
        row = []
        for x in range(N):
            f = (0.12 if y % 8 in (0, 1) else 0) + (-0.12 if x % 8 == 7 else 0) + rnd.uniform(-0.03, 0.03)
            row.append(rgb(base, f))
        px.append(row)
    return px


def rubber(seed):
    rnd = random.Random(seed)
    return [[rgb((30, 30, 30), (-0.2 if y % 6 == 0 else 0.03) + rnd.uniform(-0.04, 0.04)) for x in range(N)] for y in range(N)]


def bore():
    return [[rgb((7, 7, 9), 0.02 * math.sin(x * 0.3)) for x in range(N)] for y in range(N)]


def lens():
    px = []
    for y in range(N):
        row = []
        for x in range(N):
            d = math.hypot(x - 31.5, y - 31.5) / 32
            f = 0.35 * math.exp(-((x - 20) ** 2 + (y - 20) ** 2) / 60) - 0.3 * d
            row.append(rgb((40, 110, 120), f))
        px.append(row)
    return px


def red_dot():
    px = []
    for y in range(N):
        row = []
        for x in range(N):
            d = math.hypot(x - 31.5, y - 31.5)
            row.append(rgb((255, 40, 30), 0.6) if d < 3 else rgb((30, 70, 80), -0.2 + 0.25 * math.exp(-((x - 18) ** 2 + (y - 18) ** 2) / 90)))
        px.append(row)
    return px


TEXTURES = {
    # Metals
    "steel": lambda: surface((66, 68, 72), 1, grain=0.07, blotch=0.07),                # parkerized
    "steel_dark": lambda: surface((40, 41, 44), 2, grain=0.06, blotch=0.05),          # black oxide
    "steel_light": lambda: surface((150, 152, 156), 3, grain=0.04, brush=0.05),       # bare, machined
    "blued": lambda: surface((36, 40, 54), 4, grain=0.04, brush=0.05),
    "chrome": lambda: surface((190, 192, 198), 5, grain=0.03, brush=0.08, blotch=0.12),
    "alu": lambda: surface((58, 60, 64), 6, grain=0.035, blotch=0.04, speck=0.01),    # black anodised
    "alu_tan": lambda: surface((168, 146, 108), 7, grain=0.04, blotch=0.04),         # flat dark earth
    "alu_od": lambda: surface((86, 92, 66), 8, grain=0.04, blotch=0.05),
    "brass": lambda: surface((206, 160, 66), 9, grain=0.04, brush=0.06),
    # Polymers
    "polymer": lambda: surface((46, 46, 47), 10, grain=0.09, blotch=0.03, speck=0.03),
    "polymer_tan": lambda: surface((156, 134, 98), 11, grain=0.08, blotch=0.04, speck=0.02),
    "polymer_od": lambda: surface((80, 86, 60), 12, grain=0.08, blotch=0.04, speck=0.02),
    "polymer_plum": lambda: surface((96, 52, 44), 13, grain=0.07, blotch=0.05, speck=0.02),   # AK-74 "plum" (reddish brown)
    "polymer_grey": lambda: surface((70, 72, 74), 14, grain=0.08, blotch=0.03, speck=0.02),
    # Woods
    "walnut": lambda: wood((112, 64, 36), 20),
    "walnut_dark": lambda: wood((76, 44, 26), 21),
    "birch": lambda: wood((176, 120, 68), 22, figure=0.6),        # AKM laminated, orange-brown
    "beech": lambda: wood((150, 96, 52), 23),
    # Grips, rails, rubber, glass, the bore
    "grip_check": lambda: checker((44, 44, 45), 30),
    "grip_check_tan": lambda: checker((150, 128, 94), 31),
    "wood_check": lambda: checker((92, 52, 30), 32, size=3),
    "knurl": lambda: knurl((60, 62, 66), 33),
    "rail": lambda: rail((52, 54, 58), 34),
    "rail_tan": lambda: rail((160, 138, 102), 35),
    "rubber": lambda: rubber(36),
    "bore": bore,
    "lens": lens,
    "red_dot": red_dot,
    "scope_body": lambda: surface((40, 41, 44), 40, grain=0.03, blotch=0.04),
    "spring": lambda: [[rgb((120, 122, 126), 0.25 if y % 4 < 2 else -0.3) for x in range(N)] for y in range(N)],
    "tint": lambda: surface((214, 214, 216), 43, grain=0.05, blotch=0.05),     # takes an attachment's finish colour
    "paint_red": lambda: surface((170, 30, 26), 41, grain=0.04),
    "paint_white": lambda: surface((220, 220, 214), 42, grain=0.03),
}
