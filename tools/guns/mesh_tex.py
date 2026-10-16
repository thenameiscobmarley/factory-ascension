"""Detail textures for the gun meshes (textures/gunmesh/<class>.png): grey, tiling, 256x256 over 8 model units.
The mesh's vertex colours give the colour; these only add the surface (brushed metal, stippled polymer, wood grain,
checkering...). Each averages about 0.88 (GunMesh scales colours up by 1/0.88 to match)."""
import math
import random

N = 256


def vnoise(seed, cells):
    rnd = random.Random(seed)
    g = [[rnd.uniform(-1, 1) for _ in range(cells)] for _ in range(cells)]
    s = N / cells

    def at(x, y):
        fx, fy = x / s, y / s
        x0, y0 = int(fx) % cells, int(fy) % cells
        x1, y1 = (x0 + 1) % cells, (y0 + 1) % cells
        tx, ty = fx - int(fx), fy - int(fy)
        tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
        a = g[y0][x0] + (g[y0][x1] - g[y0][x0]) * tx
        b = g[y1][x0] + (g[y1][x1] - g[y1][x0]) * tx
        return a + (b - a) * ty
    return at


def fbm(seed, base=4, octaves=4):
    ns = [vnoise(seed + i, base * 2 ** i) for i in range(octaves)]

    def at(x, y):
        return sum(n(x, y) * 0.5 ** i for i, n in enumerate(ns)) / 1.875
    return at


def to_px(f, tint=(1.0, 1.0, 1.0)):
    rows = []
    for y in range(N):
        row = []
        for x in range(N):
            v = max(0.0, min(1.0, f(x, y)))
            row.append(tuple(int(round(255 * v * tint[i])) for i in range(3)) + (255,))
        rows.append(row)
    return rows


def metal():
    rnd = random.Random(1)
    n1, n2 = fbm(11, 4), vnoise(12, 64)
    streak = [rnd.uniform(-1, 1) for _ in range(N)]
    scratches = set()
    for _ in range(70):
        x, y = rnd.randrange(N), rnd.randrange(N)
        ang = rnd.uniform(-0.4, 0.4) + (0 if rnd.random() < 0.7 else math.pi / 2)
        L = rnd.randint(6, 40)
        for t in range(L):
            scratches.add((int(x + math.cos(ang) * t) % N, int(y + math.sin(ang) * t) % N))
    return to_px(lambda x, y: 0.88 + 0.035 * n1(x, y) + 0.02 * n2(x, y) + 0.018 * streak[y] + (0.07 if (x, y) in scratches else 0) + rnd.uniform(-0.012, 0.012))


def polymer():
    rnd = random.Random(2)
    n1, n2 = fbm(21, 4), vnoise(22, 128)
    return to_px(lambda x, y: 0.88 + 0.03 * n1(x, y) + 0.05 * n2(x, y) + rnd.uniform(-0.04, 0.04))


def wood():
    rnd = random.Random(3)
    warp, n2 = fbm(31, 2, 3), vnoise(32, 32)
    pores = vnoise(33, 128)

    def f(x, y):
        w = warp(x, y) * 3.0
        g = math.sin((y / N) * 2 * math.pi * 14 + w * 2.2 + 1.5 * math.sin(x / N * 2 * math.pi * 2))
        late = max(0.0, g) ** 6
        return 0.9 - 0.16 * late + 0.04 * n2(x, y) - (0.05 if pores(x, y) > 0.72 else 0) + rnd.uniform(-0.015, 0.015)
    return to_px(f)


def rubber():
    rnd = random.Random(4)
    n = vnoise(41, 128)
    return to_px(lambda x, y: 0.88 + 0.03 * n(x, y) + rnd.uniform(-0.03, 0.03))


def plain():
    rnd = random.Random(5)
    return to_px(lambda x, y: 0.88 + rnd.uniform(-0.01, 0.01))


def glass():
    n = fbm(61, 2, 2)
    return to_px(lambda x, y: 0.82 + 0.12 * math.exp(-(((x + y) % N) - 90) ** 2 / 900) + 0.03 * n(x, y))


def glow():
    return to_px(lambda x, y: 1.0)


def knurl():
    def f(x, y):
        a, b = (x + y) % 10, (x - y) % 10
        d = min(a, 10 - a, b, 10 - b)
        return 0.62 + 0.36 * min(1.0, d / 3.0)
    return to_px(f)


def check():
    rnd = random.Random(9)

    def f(x, y):
        a, b = (x + y) % 8, (x - y) % 8
        d = min(a, 8 - a, b, 8 - b)
        return 0.66 + 0.3 * min(1.0, d / 2.2) + rnd.uniform(-0.03, 0.03)
    return to_px(f)


def fabric():
    rnd = random.Random(10)
    n = vnoise(101, 32)

    def f(x, y):
        tw = ((x // 3) + (y // 3)) % 4 < 2
        thread = math.sin(x * math.pi / 3) ** 2 if tw else math.sin(y * math.pi / 3) ** 2
        return 0.8 + 0.14 * thread + 0.03 * n(x, y) + rnd.uniform(-0.02, 0.02)
    return to_px(f)


def rail():
    rnd = random.Random(11)
    n = fbm(111, 4)
    return to_px(lambda x, y: 0.88 + 0.03 * n(x, y) + rnd.uniform(-0.015, 0.015))


TEXTURES = {"metal": metal, "polymer": polymer, "wood": wood, "rubber": rubber, "plain": plain, "glass": glass, "glow": glow,
            "knurl": knurl, "check": check, "fabric": fabric, "rail": rail}
