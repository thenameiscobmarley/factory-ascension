"""Tome covers for Factory Ascension: every enchant tome gets its own design.

Each of the 86 base enchants (Ascended, Mythic, Ultimate) has a hand-designed emblem that shows what it does,
drawn from simple shapes in a unit square and rasterized with 4x4 supersampling into crisp pixel art.
Fusion tomes split the cover corner to corner and carry both parents' emblems, so every pair looks different too.
Books are drawn natively at 32x32; make_textures.py then runs its detail pass on them.

An enchant without a designed emblem (a new one added to the config) still gets a unique rune made from its name.
"""
import math
import zlib

S = 32  # tome texture size
STROKE = 1.3  # line widths are scaled up so thin emblems stay readable at 14 pixels


# ---- Shapes: each returns a test f(u, v) -> bool on the unit square (u right, v down) ----

def seg(x0, y0, x1, y1, w=0.12):
    w *= STROKE
    dx, dy = x1 - x0, y1 - y0
    ll = dx * dx + dy * dy or 1e-9

    def f(u, v):
        t = max(0.0, min(1.0, ((u - x0) * dx + (v - y0) * dy) / ll))
        px, py = x0 + t * dx - u, y0 + t * dy - v
        return px * px + py * py <= (w / 2) ** 2
    return f


def circ(cx, cy, r):
    return lambda u, v: (u - cx) ** 2 + (v - cy) ** 2 <= r * r


def ell(cx, cy, rx, ry):
    return lambda u, v: ((u - cx) / rx) ** 2 + ((v - cy) / ry) ** 2 <= 1


def ring(cx, cy, r, w=0.1):
    w *= STROKE
    return lambda u, v: abs(math.hypot(u - cx, v - cy) - r) <= w / 2


def arc(cx, cy, r, a0, a1, w=0.1):
    """Ring segment from angle a0 to a1 in degrees (0 = right, 90 = down)."""
    w *= STROKE

    def f(u, v):
        if abs(math.hypot(u - cx, v - cy) - r) > w / 2:
            return False
        a = math.degrees(math.atan2(v - cy, u - cx)) % 360
        lo, hi = a0 % 360, a1 % 360
        return lo <= a <= hi if lo <= hi else a >= lo or a <= hi
    return f


def rect(x0, y0, x1, y1):
    return lambda u, v: x0 <= u <= x1 and y0 <= v <= y1


def poly(pts):
    def f(u, v):
        inside = False
        j = len(pts) - 1
        for i in range(len(pts)):
            xi, yi = pts[i]
            xj, yj = pts[j]
            if (yi > v) != (yj > v) and u < (xj - xi) * (v - yi) / (yj - yi) + xi:
                inside = not inside
            j = i
        return inside
    return f


def star_pts(cx, cy, ro, ri, n, rot=-90):
    pts = []
    for i in range(n * 2):
        r = ro if i % 2 == 0 else ri
        a = math.radians(rot + i * 180.0 / n)
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return pts


def heart(cx, cy, s):
    return [('+', circ(cx - s * 0.5, cy - s * 0.15, s * 0.55)), ('+', circ(cx + s * 0.5, cy - s * 0.15, s * 0.55)),
            ('+', poly([(cx - s * 1.02, cy), (cx + s * 1.02, cy), (cx, cy + s * 1.15)]))]


def add(*fs):
    return [('+', f) for f in fs]


def cut(*fs):
    return [('-', f) for f in fs]


def shield(cx=0.5, cy=0.5, s=1.0):
    return poly([(cx - 0.34 * s, cy - 0.38 * s), (cx + 0.34 * s, cy - 0.38 * s), (cx + 0.34 * s, cy + 0.02 * s),
                 (cx, cy + 0.42 * s), (cx - 0.34 * s, cy + 0.02 * s)])


def shield_ring(s=1.0):
    """Shield outline: the ward enchants put their threat inside it."""
    return add(shield(s=s)) + cut(shield(s=s * 0.72))


def skull(cx, cy, s):
    return (add(circ(cx, cy - 0.04 * s, 0.3 * s), rect(cx - 0.17 * s, cy + 0.1 * s, cx + 0.17 * s, cy + 0.3 * s)) +
            cut(circ(cx - 0.12 * s, cy - 0.02 * s, 0.08 * s), circ(cx + 0.12 * s, cy - 0.02 * s, 0.08 * s),
                poly([(cx, cy + 0.06 * s), (cx - 0.04 * s, cy + 0.14 * s), (cx + 0.04 * s, cy + 0.14 * s)]),
                rect(cx - 0.02 * s, cy + 0.2 * s, cx + 0.02 * s, cy + 0.3 * s)))


def flame(cx, cy, s):
    return add(poly([(cx, cy - 0.42 * s), (cx + 0.12 * s, cy - 0.18 * s), (cx + 0.3 * s, cy - 0.24 * s),
                     (cx + 0.3 * s, cy + 0.12 * s), (cx + 0.18 * s, cy + 0.36 * s), (cx - 0.18 * s, cy + 0.36 * s),
                     (cx - 0.3 * s, cy + 0.12 * s), (cx - 0.24 * s, cy - 0.14 * s), (cx - 0.1 * s, cy - 0.04 * s)])) + \
        cut(poly([(cx, cy - 0.06 * s), (cx + 0.12 * s, cy + 0.14 * s), (cx + 0.08 * s, cy + 0.3 * s),
                  (cx - 0.08 * s, cy + 0.3 * s), (cx - 0.12 * s, cy + 0.14 * s)]))


def bolt(cx, cy, s):
    return add(poly([(cx + 0.08 * s, cy - 0.46 * s), (cx - 0.24 * s, cy + 0.04 * s), (cx - 0.02 * s, cy + 0.04 * s),
                     (cx - 0.1 * s, cy + 0.46 * s), (cx + 0.24 * s, cy - 0.08 * s), (cx + 0.02 * s, cy - 0.08 * s)]))


def drop(cx, cy, s):
    return add(circ(cx, cy + 0.1 * s, 0.24 * s), poly([(cx, cy - 0.4 * s), (cx - 0.22 * s, cy + 0.04 * s), (cx + 0.22 * s, cy + 0.04 * s)]))


def snowflake(cx, cy, s, w=0.09):
    out = []
    for k in range(3):
        a = math.radians(90 + k * 60)
        dx, dy = math.cos(a) * 0.42 * s, math.sin(a) * 0.42 * s
        out += add(seg(cx - dx, cy - dy, cx + dx, cy + dy, w))
        for sgn in (1, -1):
            bx, by = cx + sgn * dx * 0.6, cy + sgn * dy * 0.6
            for t in (-1, 1):
                b = a + t * math.radians(40) + (math.pi if sgn < 0 else 0)
                out += add(seg(bx, by, bx + math.cos(b) * 0.14 * s, by + math.sin(b) * 0.14 * s, w * 0.9))
    return out


def sword(x0, y0, x1, y1, w=0.12, guard=0.18):
    """Blade from (x0, y0) hilt end to (x1, y1) tip, with a crossguard and pommel."""
    dx, dy = x1 - x0, y1 - y0
    ln = math.hypot(dx, dy)
    ux, uy = dx / ln, dy / ln
    gx, gy = x0 + ux * ln * 0.22, y0 + uy * ln * 0.22
    return add(seg(gx, gy, x1, y1, w), seg(gx - uy * guard, gy + ux * guard, gx + uy * guard, gy - ux * guard, w * 0.9),
               seg(x0, y0, gx, gy, w * 0.7), circ(x0, y0, w * 0.7))


def arrow(x0, y0, x1, y1, w=0.1, head=0.18):
    dx, dy = x1 - x0, y1 - y0
    ln = math.hypot(dx, dy)
    ux, uy = dx / ln, dy / ln
    bx, by = x1 - ux * head, y1 - uy * head
    return add(seg(x0, y0, bx, by, w), poly([(x1, y1), (bx - uy * head * 0.7, by + ux * head * 0.7), (bx + uy * head * 0.7, by - ux * head * 0.7)]))


def wing(cx, cy, s, flip=1):
    """One feathered wing; flip=-1 mirrors it."""
    pts = [(0.0, 0.0), (0.42, -0.3), (0.46, -0.1), (0.4, 0.04), (0.42, 0.12), (0.32, 0.18), (0.32, 0.28), (0.14, 0.26)]
    return add(poly([(cx + flip * x * s, cy + y * s) for x, y in pts]))


def chevron(cx, cy, s, w=0.11):
    return add(seg(cx - 0.14 * s, cy - 0.22 * s, cx + 0.12 * s, cy, w), seg(cx + 0.12 * s, cy, cx - 0.14 * s, cy + 0.22 * s, w))


def gear(cx, cy, r, teeth=8):
    out = add(circ(cx, cy, r * 0.75))
    for k in range(teeth):
        a = math.radians(k * 360.0 / teeth)
        out += add(seg(cx + math.cos(a) * r * 0.6, cy + math.sin(a) * r * 0.6, cx + math.cos(a) * r, cy + math.sin(a) * r, r * 0.34))
    return out + cut(circ(cx, cy, r * 0.32))


def pick(cx, cy, s):
    return add(arc(cx + 0.02 * s, cy + 0.32 * s, 0.42 * s, 215, 325, 0.11 * s), seg(cx - 0.22 * s, cy + 0.42 * s, cx + 0.12 * s, cy - 0.06 * s, 0.1 * s))


# ---- The emblems: one per base enchant, all on a 0..1 square ----
EMBLEMS = {
    # Damage
    'sharpness': lambda: sword(0.2, 0.8, 0.84, 0.16),
    'smite': lambda: add(rect(0.42, 0.1, 0.58, 0.9), rect(0.2, 0.3, 0.8, 0.44), ring(0.5, 0.37, 0.14, 0.05)) + cut(circ(0.5, 0.37, 0.06)),
    'bane': lambda: add(ell(0.5, 0.58, 0.16, 0.2), circ(0.5, 0.32, 0.09),
                        *[seg(0.5, 0.5 + dy, 0.5 + sx * 0.36, 0.5 + dy + ey, 0.07) for sx in (1, -1) for dy, ey in ((-0.08, -0.16), (0.0, -0.02), (0.08, 0.12), (0.14, 0.28))]),
    'giant_slayer': lambda: heart(0.5, 0.5, 0.36) + cut(seg(0.15, 0.82, 0.86, 0.12, 0.08)) + add(seg(0.2, 0.78, 0.82, 0.16, 0.05)),
    'hellbane': lambda: skull(0.5, 0.56, 1.0) + add(poly([(0.24, 0.42), (0.1, 0.12), (0.34, 0.3)]), poly([(0.76, 0.42), (0.9, 0.12), (0.66, 0.3)])),
    'raidbreaker': lambda: add(shield()) + cut(poly([(0.48, 0.1), (0.56, 0.36), (0.44, 0.5), (0.56, 0.7), (0.5, 0.92), (0.46, 0.72), (0.36, 0.5), (0.48, 0.36)])),
    'impaling': lambda: add(seg(0.5, 0.95, 0.5, 0.36, 0.1), seg(0.28, 0.4, 0.72, 0.4, 0.09), seg(0.28, 0.12, 0.28, 0.42, 0.08),
                            seg(0.72, 0.12, 0.72, 0.42, 0.08), seg(0.5, 0.06, 0.5, 0.4, 0.09)),
    'skyhunter': lambda: add(ring(0.5, 0.5, 0.36, 0.07), seg(0.5, 0.04, 0.5, 0.2, 0.07), seg(0.5, 0.8, 0.5, 0.96, 0.07),
                             seg(0.04, 0.5, 0.2, 0.5, 0.07), seg(0.8, 0.5, 0.96, 0.5, 0.07), arc(0.38, 0.6, 0.14, 200, 340, 0.07), arc(0.62, 0.6, 0.14, 200, 340, 0.07)),
    'first_strike': lambda: add(seg(0.56, 0.12, 0.56, 0.86, 0.15), seg(0.56, 0.14, 0.32, 0.32, 0.12), seg(0.34, 0.86, 0.78, 0.86, 0.11)),
    'executioner': lambda: add(seg(0.26, 0.92, 0.62, 0.22, 0.09), poly([(0.5, 0.06), (0.92, 0.16), (0.86, 0.52), (0.56, 0.4)])),
    'berserk': lambda: add(*[seg(0.2 + k * 0.22, 0.14, 0.42 + k * 0.22, 0.86, 0.1) for k in range(3)]),
    'power': lambda: add(arc(0.08, 0.5, 0.5, -58, 58, 0.11), seg(0.34, 0.08, 0.34, 0.92, 0.04)) + arrow(0.2, 0.5, 0.96, 0.5, 0.07, 0.2),
    'assassin': lambda: add(poly([(0.42, 0.12), (0.58, 0.12), (0.5, 0.72)]), seg(0.3, 0.72, 0.7, 0.72, 0.08), seg(0.5, 0.72, 0.5, 0.92, 0.09)) +
    add(arc(0.5, 0.42, 0.42, 190, 350, 0.08)),
    'critical': lambda: add(poly(star_pts(0.5, 0.5, 0.48, 0.2, 8))) + cut(rect(0.45, 0.24, 0.55, 0.56), circ(0.5, 0.68, 0.06)),
    'reaper': lambda: add(seg(0.66, 0.94, 0.42, 0.08, 0.08), poly([(0.42, 0.06), (0.06, 0.22), (0.1, 0.32), (0.44, 0.2)])),
    'cleave': lambda: add(arc(0.5, 0.62, 0.38, 200, 340, 0.12)) + sword(0.5, 0.92, 0.5, 0.34, 0.1, 0.12),
    'lifesteal': lambda: drop(0.38, 0.55, 0.9) + add(poly([(0.62, 0.12), (0.92, 0.12), (0.77, 0.5)])),
    # Hit effects
    'frostbite': lambda: snowflake(0.5, 0.5, 1.0),
    'enfeeble': lambda: sword(0.18, 0.82, 0.5, 0.5, 0.11) + add(seg(0.6, 0.4, 0.84, 0.16, 0.11)) + arrow(0.8, 0.5, 0.8, 0.92, 0.07, 0.12),
    'withering': lambda: skull(0.5, 0.48, 1.0) + add(ring(0.5, 0.46, 0.44, 0.05)),
    'venom': lambda: add(poly([(0.22, 0.08), (0.44, 0.08), (0.33, 0.48)]), poly([(0.56, 0.08), (0.78, 0.08), (0.67, 0.48)])) + drop(0.5, 0.74, 0.55),
    'skyward': lambda: arrow(0.5, 0.92, 0.5, 0.08, 0.13, 0.26) + add(seg(0.14, 0.62, 0.3, 0.62, 0.06), seg(0.7, 0.42, 0.86, 0.42, 0.06), seg(0.12, 0.8, 0.26, 0.8, 0.06)),
    'fire_aspect': lambda: flame(0.5, 0.5, 1.0),
    'knockback': lambda: add(circ(0.36, 0.5, 0.2), rect(0.36, 0.38, 0.56, 0.62)) + chevron(0.68, 0.5, 1.0) + chevron(0.84, 0.5, 0.8),
    'second_wind': lambda: add(arc(0.46, 0.5, 0.36, 180, 450, 0.08), arc(0.5, 0.5, 0.2, 0, 270, 0.08), seg(0.1, 0.5, 0.1, 0.66, 0.08)),
    'adrenaline': lambda: heart(0.5, 0.48, 0.38) + cut(seg(0.06, 0.48, 0.3, 0.48, 0.07), seg(0.3, 0.48, 0.4, 0.24, 0.07),
                                                   seg(0.4, 0.24, 0.54, 0.72, 0.07), seg(0.54, 0.72, 0.64, 0.48, 0.07), seg(0.64, 0.48, 0.94, 0.48, 0.07)),
    'plunder': lambda: add(rect(0.12, 0.44, 0.88, 0.86), arc(0.5, 0.48, 0.38, 180, 360, 0.16)) + cut(rect(0.12, 0.42, 0.88, 0.48), rect(0.44, 0.52, 0.56, 0.66)),
    'soul_eater': lambda: drop(0.5, 0.5, 1.15) + cut(circ(0.42, 0.56, 0.05), circ(0.58, 0.56, 0.05), ell(0.5, 0.7, 0.07, 0.04)),
    'wisdom': lambda: add(poly([(0.04, 0.5), (0.3, 0.26), (0.7, 0.26), (0.96, 0.5), (0.7, 0.74), (0.3, 0.74)])) + cut(ring(0.5, 0.5, 0.2, 0.08), circ(0.56, 0.44, 0.05)),
    'midas': lambda: add(ell(0.5, 0.76, 0.34, 0.1), ell(0.5, 0.62, 0.34, 0.1), ell(0.5, 0.48, 0.34, 0.1), circ(0.5, 0.26, 0.2)) +
    cut(ring(0.5, 0.26, 0.12, 0.04)),
    'gem_hunter': lambda: add(poly([(0.24, 0.18), (0.76, 0.18), (0.94, 0.4), (0.5, 0.9), (0.06, 0.4)])) +
    cut(seg(0.06, 0.4, 0.94, 0.4, 0.04), seg(0.38, 0.18, 0.3, 0.4, 0.04), seg(0.62, 0.18, 0.7, 0.4, 0.04), seg(0.3, 0.4, 0.5, 0.88, 0.04), seg(0.7, 0.4, 0.5, 0.88, 0.04)),
    # Defense
    'protection': lambda: add(shield()) + cut(shield(s=0.78)) + add(rect(0.45, 0.24, 0.55, 0.66), rect(0.3, 0.36, 0.7, 0.46)),
    'fire_ward': lambda: shield_ring() + flame(0.5, 0.48, 0.55),
    'featherweight': lambda: add(poly([(0.84, 0.08), (0.62, 0.14), (0.34, 0.4), (0.22, 0.66), (0.3, 0.7), (0.56, 0.5), (0.78, 0.3)]), seg(0.12, 0.92, 0.7, 0.24, 0.05)),
    'blast_ward': lambda: shield_ring() + add(circ(0.5, 0.52, 0.16), seg(0.56, 0.36, 0.66, 0.24, 0.05)),
    'arrow_ward': lambda: shield_ring() + arrow(0.24, 0.76, 0.6, 0.38, 0.06, 0.13),
    'spellward': lambda: shield_ring() + add(circ(0.5, 0.56, 0.13), rect(0.46, 0.3, 0.54, 0.46)),
    'wither_ward': lambda: shield_ring() + skull(0.5, 0.48, 0.5),
    'bulwark': lambda: add(rect(0.14, 0.32, 0.86, 0.9), *[rect(0.14 + k * 0.2, 0.14, 0.26 + k * 0.2, 0.34) for k in range(4)]) + cut(arc(0.5, 0.9, 0.16, 180, 360, 0.3)),
    'vitality': lambda: heart(0.5, 0.5, 0.42),
    'retribution': lambda: add(arc(0.5, 0.5, 0.32, 200, 520, 0.1)) + add(poly([(0.12, 0.5), (0.32, 0.34), (0.32, 0.62)])) + add(poly(star_pts(0.5, 0.5, 0.16, 0.07, 4))),
    'evasion': lambda: add(seg(0.1, 0.3, 0.4, 0.3, 0.07), seg(0.04, 0.5, 0.36, 0.5, 0.07), seg(0.1, 0.7, 0.4, 0.7, 0.07)) + chevron(0.66, 0.5, 1.4, 0.13),
    'undying': lambda: add(rect(0.34, 0.12, 0.66, 0.42), rect(0.36, 0.42, 0.64, 0.86), poly([(0.36, 0.48), (0.08, 0.6), (0.36, 0.66)]), poly([(0.64, 0.48), (0.92, 0.6), (0.64, 0.66)])) +
    cut(rect(0.4, 0.22, 0.46, 0.3), rect(0.54, 0.22, 0.6, 0.3), rect(0.42, 0.34, 0.58, 0.37)),
    'renewal': lambda: add(seg(0.5, 0.94, 0.5, 0.4, 0.07), ell(0.32, 0.44, 0.2, 0.11), ell(0.68, 0.3, 0.2, 0.11)) + add(arc(0.5, 0.86, 0.22, 200, 340, 0.06)),
    'night_sight': lambda: add(circ(0.42, 0.42, 0.34)) + cut(circ(0.58, 0.32, 0.3)) + add(poly(star_pts(0.78, 0.7, 0.14, 0.05, 4))),
    'gills': lambda: add(ring(0.32, 0.66, 0.16, 0.07), ring(0.66, 0.42, 0.12, 0.07), ring(0.4, 0.22, 0.08, 0.06), arc(0.5, 0.92, 0.42, 200, 340, 0.06)),
    'fireproof': lambda: add(ring(0.5, 0.5, 0.42, 0.08)) + flame(0.5, 0.52, 0.62),
    'industrious': lambda: gear(0.5, 0.5, 0.44),
    'leaping': lambda: add(arc(0.5, 0.86, 0.38, 195, 330, 0.09), rect(0.1, 0.86, 0.9, 0.92)) + arrow(0.78, 0.58, 0.84, 0.82, 0.08, 0.16),
    'sustenance': lambda: add(circ(0.4, 0.6, 0.26), circ(0.62, 0.6, 0.26), seg(0.5, 0.36, 0.56, 0.12, 0.06), ell(0.7, 0.2, 0.14, 0.07)),
    'magnetism': lambda: add(arc(0.5, 0.44, 0.3, 0, 180, 0.2), rect(0.1, 0.12, 0.3, 0.44), rect(0.7, 0.12, 0.9, 0.44)) + cut(rect(0.1, 0.1, 0.9, 0.22)) +
    add(rect(0.1, 0.06, 0.3, 0.18), rect(0.7, 0.06, 0.9, 0.18)),
    'restoration': lambda: add(rect(0.2, 0.12, 0.72, 0.34), seg(0.46, 0.34, 0.46, 0.92, 0.11)) + add(poly(star_pts(0.82, 0.66, 0.14, 0.05, 4))),
    'swiftness': lambda: chevron(0.36, 0.5, 1.6, 0.13) + chevron(0.66, 0.5, 1.6, 0.13),
    'reach': lambda: arrow(0.5, 0.5, 0.9, 0.1, 0.09, 0.2) + arrow(0.5, 0.5, 0.1, 0.9, 0.09, 0.2),
    'stride': lambda: add(ell(0.3, 0.66, 0.15, 0.24), ell(0.7, 0.36, 0.15, 0.24), circ(0.22, 0.34, 0.06), circ(0.36, 0.34, 0.06),
                          circ(0.62, 0.04, 0.06), circ(0.76, 0.04, 0.06)),
    'frenzy': lambda: sword(0.16, 0.84, 0.84, 0.16, 0.1) + sword(0.84, 0.84, 0.16, 0.16, 0.1),
    'lucky': lambda: add(circ(0.36, 0.32, 0.16), circ(0.64, 0.32, 0.16), circ(0.36, 0.6, 0.16), circ(0.64, 0.6, 0.16), seg(0.5, 0.5, 0.74, 0.94, 0.07)),
    'excavation': lambda: pick(0.5, 0.5, 1.0) + add(rect(0.08, 0.86, 0.38, 0.94)),
    'eternal': lambda: add(ring(0.3, 0.5, 0.2, 0.1), ring(0.7, 0.5, 0.2, 0.1)),
    # Mythic
    'wings': lambda: wing(0.46, 0.5, 1.0, -1) + wing(0.54, 0.5, 1.0, 1),
    'thunderlord': lambda: bolt(0.5, 0.5, 1.0) + add(arc(0.5, 0.5, 0.46, 200, 250, 0.05), arc(0.5, 0.5, 0.46, 20, 70, 0.05)),
    'black_hole': lambda: add(circ(0.5, 0.5, 0.42)) + cut(arc(0.5, 0.5, 0.3, 0, 200, 0.07), arc(0.5, 0.5, 0.16, 180, 380, 0.07), circ(0.5, 0.5, 0.06)),
    'meteor': lambda: add(circ(0.66, 0.66, 0.22), seg(0.1, 0.1, 0.56, 0.56, 0.08), seg(0.26, 0.06, 0.62, 0.42, 0.05), seg(0.06, 0.28, 0.42, 0.62, 0.05)) +
    cut(circ(0.6, 0.6, 0.05), circ(0.74, 0.72, 0.04)),
    'chronos': lambda: add(poly([(0.2, 0.08), (0.8, 0.08), (0.5, 0.5)]), poly([(0.2, 0.92), (0.8, 0.92), (0.5, 0.5)]), rect(0.14, 0.04, 0.86, 0.1), rect(0.14, 0.9, 0.86, 0.96)) +
    cut(poly([(0.3, 0.14), (0.7, 0.14), (0.5, 0.38)])),
    'phoenix': lambda: wing(0.5, 0.44, 0.9, -1) + wing(0.5, 0.44, 0.9, 1) + add(circ(0.5, 0.36, 0.08)) + flame(0.5, 0.78, 0.4),
    'voidwalker': lambda: add(ell(0.5, 0.5, 0.44, 0.3)) + cut(ell(0.5, 0.5, 0.3, 0.18)) + add(circ(0.5, 0.5, 0.1)),
    'soul_harvest': lambda: drop(0.24, 0.5, 0.6) + drop(0.5, 0.36, 0.7) + drop(0.76, 0.5, 0.6) + add(arc(0.5, 0.6, 0.4, 20, 160, 0.08)),
    'overkill': lambda: add(poly(star_pts(0.5, 0.5, 0.5, 0.26, 10))) + cut(seg(0.32, 0.32, 0.68, 0.68, 0.08), seg(0.68, 0.32, 0.32, 0.68, 0.08)),
    'rampage': lambda: add(arc(0.5, 0.62, 0.32, 180, 360, 0.14), poly([(0.18, 0.62), (0.04, 0.2), (0.24, 0.5)]), poly([(0.82, 0.62), (0.96, 0.2), (0.76, 0.5)]),
                           circ(0.5, 0.66, 0.16)) + cut(circ(0.44, 0.66, 0.04), circ(0.56, 0.66, 0.04)),
    'vein_miner': lambda: add(poly([(0.2, 0.14), (0.34, 0.28), (0.2, 0.42), (0.06, 0.28)]), poly([(0.5, 0.36), (0.64, 0.5), (0.5, 0.64), (0.36, 0.5)]),
                              poly([(0.8, 0.58), (0.94, 0.72), (0.8, 0.86), (0.66, 0.72)]), seg(0.2, 0.28, 0.8, 0.72, 0.05)),
    'timber': lambda: add(poly([(0.5, 0.04), (0.84, 0.46), (0.66, 0.46), (0.9, 0.74), (0.1, 0.74), (0.34, 0.46), (0.16, 0.46)]), rect(0.42, 0.74, 0.58, 0.96)),
    'radiance': lambda: add(circ(0.5, 0.5, 0.22), *[seg(0.5 + math.cos(math.radians(k * 45)) * 0.32, 0.5 + math.sin(math.radians(k * 45)) * 0.32,
                                                      0.5 + math.cos(math.radians(k * 45)) * 0.48, 0.5 + math.sin(math.radians(k * 45)) * 0.48, 0.08) for k in range(8)]),
    'headhunter': lambda: add(rect(0.14, 0.14, 0.86, 0.86)) + cut(rect(0.26, 0.28, 0.42, 0.44), rect(0.58, 0.28, 0.74, 0.44), rect(0.42, 0.44, 0.58, 0.6),
                                                                 rect(0.32, 0.56, 0.42, 0.76), rect(0.58, 0.56, 0.68, 0.76)),
    'gravity_slam': lambda: arrow(0.5, 0.04, 0.5, 0.66, 0.16, 0.26) + add(rect(0.06, 0.76, 0.94, 0.82)) + add(seg(0.3, 0.82, 0.2, 0.96, 0.05), seg(0.7, 0.82, 0.8, 0.96, 0.05), seg(0.5, 0.82, 0.5, 0.96, 0.05)),
    # Ultimates
    'thunder_waltz': lambda: bolt(0.5, 0.5, 0.86) + add(arc(0.5, 0.5, 0.44, 120, 230, 0.07), arc(0.5, 0.5, 0.44, 300, 410, 0.07)) +
    add(poly(star_pts(0.14, 0.2, 0.1, 0.04, 4)), poly(star_pts(0.86, 0.8, 0.1, 0.04, 4))),
    'thousand_cuts': lambda: add(*[seg(0.08 + k * 0.17, 0.92, 0.32 + k * 0.17, 0.08, 0.06) for k in range(4)], seg(0.06, 0.4, 0.94, 0.6, 0.06)),
    'heaven_splitter': lambda: sword(0.5, 0.04, 0.5, 0.96, 0.14, 0.24) + add(seg(0.04, 0.6, 0.36, 0.56, 0.05), seg(0.64, 0.56, 0.96, 0.6, 0.05)),
    'dragon_spiral': lambda: add(arc(0.5, 0.5, 0.42, 0, 300, 0.1), arc(0.5, 0.5, 0.26, 60, 360, 0.1), circ(0.5, 0.5, 0.1)) + flame(0.86, 0.3, 0.32),
    'void_collapse': lambda: add(ring(0.5, 0.5, 0.42, 0.06), ring(0.5, 0.5, 0.26, 0.06), circ(0.5, 0.5, 0.1)) + arrow(0.04, 0.04, 0.3, 0.3, 0.06, 0.12) + arrow(0.96, 0.96, 0.7, 0.7, 0.06, 0.12),
    'starfall': lambda: add(poly(star_pts(0.6, 0.62, 0.34, 0.14, 5)), poly(star_pts(0.18, 0.2, 0.16, 0.07, 5)),
                            seg(0.04, 0.04, 0.4, 0.4, 0.07), seg(0.3, 0.02, 0.5, 0.22, 0.05)),
    'shadow_barrage': lambda: add(*[f for k in range(3) for f in (circ(0.2 + k * 0.3, 0.26, 0.12), poly([(0.04 + k * 0.3, 0.92), (0.36 + k * 0.3, 0.92), (0.3 + k * 0.3, 0.44), (0.1 + k * 0.3, 0.44)]))]),
    'frozen_eternity': lambda: add(poly([(0.5 + 0.46 * math.cos(math.radians(30 + k * 60)), 0.5 + 0.46 * math.sin(math.radians(30 + k * 60))) for k in range(6)])) +
    cut(poly([(0.5 + 0.36 * math.cos(math.radians(30 + k * 60)), 0.5 + 0.36 * math.sin(math.radians(30 + k * 60))) for k in range(6)])) + snowflake(0.5, 0.5, 0.7, 0.07),
    'spirit_bomb': lambda: add(circ(0.5, 0.42, 0.3), ring(0.5, 0.42, 0.4, 0.04)) + cut(circ(0.42, 0.34, 0.08)) + add(seg(0.3, 0.94, 0.4, 0.74, 0.08), seg(0.7, 0.94, 0.6, 0.74, 0.08)),
    'blade_storm': lambda: add(circ(0.5, 0.5, 0.08), *[seg(0.5 + math.cos(math.radians(k * 90 + 20)) * 0.14, 0.5 + math.sin(math.radians(k * 90 + 20)) * 0.14,
                                                         0.5 + math.cos(math.radians(k * 90 + 60)) * 0.46, 0.5 + math.sin(math.radians(k * 90 + 60)) * 0.46, 0.09) for k in range(4)]),
    'eclipse': lambda: add(ring(0.5, 0.5, 0.44, 0.06), circ(0.5, 0.5, 0.32)) + cut(circ(0.6, 0.44, 0.28)) +
    add(*[seg(0.5 + math.cos(math.radians(k * 60)) * 0.44, 0.5 + math.sin(math.radians(k * 60)) * 0.44, 0.5 + math.cos(math.radians(k * 60)) * 0.5, 0.5 + math.sin(math.radians(k * 60)) * 0.5, 0.06) for k in range(6)]),
    'phoenix_dive': lambda: wing(0.5, 0.6, 0.8, -1) + wing(0.5, 0.6, 0.8, 1) + arrow(0.5, 0.04, 0.5, 0.86, 0.1, 0.2) + flame(0.5, 0.18, 0.3),
}


def rune(name):
    """A unique fallback emblem for an enchant with no designed one: a ring and three strokes from its name's hash."""
    h = zlib.crc32(name.encode())
    pts = [((h >> (i * 4)) & 15) / 15.0 * 0.7 + 0.15 for i in range(8)]
    return add(ring(0.5, 0.5, 0.42, 0.07), seg(pts[0], pts[1], pts[2], pts[3], 0.09), seg(pts[2], pts[3], pts[4], pts[5], 0.09), seg(pts[4], pts[5], pts[6], pts[7], 0.09))


def raster(shapes, size, ss=4):
    """Supersampled mask: a pixel is on when at least ~half its subsamples are inside the shape list."""
    mask = []
    for py in range(size):
        row = []
        for px in range(size):
            hits = 0
            for sy in range(ss):
                v = (py + (sy + 0.5) / ss) / size
                for sx in range(ss):
                    u = (px + (sx + 0.5) / ss) / size
                    inside = False
                    for op, f in shapes:
                        if f(u, v):
                            inside = op == '+'
                    hits += inside
            row.append(hits * 2 >= ss * ss)
        mask.append(row)
    return mask


_cache = {}


def emblem_mask(name, size):
    key = (name, size)
    if key not in _cache:
        _cache[key] = raster(EMBLEMS[name]() if name in EMBLEMS else rune(name), size)
    return _cache[key]


# ---- Drawing the book ----

def _lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


CREAM, CREAM_DARK = (238, 228, 198), (200, 186, 150)
SILVER, GOLD, CRIMSON = (205, 212, 224), (242, 196, 72), (170, 24, 40)
X0, X1, Y0, Y1 = 5, 24, 2, 27      # cover
SPINE = 8                          # spine is X0..SPINE


def _blank():
    return [[(0, 0, 0, 0)] * S for _ in range(S)]


def _cover(px, color_at, metal, frame=1, kind='ascended'):
    """Cover, spine with bands, page edges, inset frame and corner caps."""
    for y in range(Y0, Y1 + 1):
        for x in range(X0, X1 + 1):
            c = color_at(x, y)
            if x <= SPINE:
                c = _lerp(c, (0, 0, 0), 0.42 if x < SPINE else 0.55)
            px[y][x] = c + (255,)
    # Page block: right side and bottom, with page lines
    for y in range(Y0 + 1, Y1 + 3):
        for x in (X1 + 1, X1 + 2):
            px[y][x] = (CREAM if (y + x) % 3 else CREAM_DARK) + (255,)
    for x in range(X0 + 2, X1 + 3):
        for y in (Y1 + 1, Y1 + 2):
            px[y][x] = (CREAM if (x + y) % 3 else CREAM_DARK) + (255,)
    # Spine bands
    for by in (Y0 + 4, Y1 - 4):
        for x in range(X0, SPINE + 1):
            px[by][x] = _lerp(metal, (0, 0, 0), 0.15) + (255,)
    # Inset frame(s) on the front
    for k in range(frame):
        # Ultimates get a double frame hugging the edge, so the big emblem has room
        inset = 1 + k if frame == 2 else 2
        fx0, fx1, fy0, fy1 = SPINE + inset, X1 - inset, Y0 + inset, Y1 - inset
        col = metal if kind != 'ascended' else _lerp(color_at(fx0, fy0), (255, 255, 255), 0.22)
        if kind == 'ultimate' and k == 1:
            col = CRIMSON
        for x in range(fx0, fx1 + 1):
            px[fy0][x] = px[fy1][x] = col + (255,)
        for y in range(fy0, fy1 + 1):
            px[y][fx0] = px[y][fx1] = col + (255,)
    # Corner caps on the outer corners
    for cx, cy, sx, sy in ((X1, Y0, -1, 1), (X1, Y1, -1, -1)):
        for i in range(3):
            for j in range(3 - i):
                px[cy + sy * i][cx + sx * j] = metal + (255,)


def _paint(px, mask, ox, oy, ink, edge, glow=None, region=None):
    """Emblem pixels in the ink color, lit from the top-left, with a dark rim and an optional glow around it."""
    n = len(mask)
    on = lambda x, y: 0 <= x < n and 0 <= y < n and mask[y][x]
    allowed = lambda X, Y: region is None or region(X, Y)
    if glow:
        for y in range(-2, n + 2):
            for x in range(-2, n + 2):
                if on(x, y):
                    continue
                near = any(on(x + dx, y + dy) for dx in range(-2, 3) for dy in range(-2, 3))
                X, Y = ox + x, oy + y
                if near and 0 <= X < S and 0 <= Y < S and px[Y][X][3] and allowed(X, Y):
                    px[Y][X] = _lerp(px[Y][X][:3], glow, 0.4) + (255,)
    for y in range(-1, n + 1):
        for x in range(-1, n + 1):
            if on(x, y):
                continue
            if any(on(x + dx, y + dy) for dx in (-1, 0, 1) for dy in (-1, 0, 1)):
                X, Y = ox + x, oy + y
                if 0 <= X < S and 0 <= Y < S and px[Y][X][3] and allowed(X, Y):
                    px[Y][X] = edge + (255,)
    for y in range(n):
        for x in range(n):
            if not on(x, y):
                continue
            c = ink
            if not on(x - 1, y) or not on(x, y - 1):
                c = _lerp(c, (255, 255, 255), 0.38)
            elif not on(x + 1, y) or not on(x, y + 1):
                c = _lerp(c, (0, 0, 0), 0.28)
            X, Y = ox + x, oy + y
            if allowed(X, Y):
                px[Y][X] = c + (255,)


def tome(name, cover, ink, kind):
    """kind: 'ascended', 'mythic' or 'ultimate'."""
    px = _blank()
    metal = SILVER if kind == 'ascended' else GOLD
    _cover(px, lambda x, y: cover, metal, frame=2 if kind == 'ultimate' else 1, kind=kind)
    size = 15 if kind == 'ultimate' else 14
    ox = (SPINE + 1 + X1) // 2 - size // 2 + 1
    oy = (Y0 + Y1) // 2 - size // 2 + 1
    edge = _lerp(cover, (0, 0, 0), 0.65)
    glow = None if kind == 'ascended' else _lerp(ink, (255, 255, 255), 0.25)
    _paint(px, emblem_mask(name, size), ox, oy, ink, edge, glow)
    if kind == 'ultimate':
        # A crimson ribbon bookmark hanging out of the pages
        for y in range(Y1 + 1, S - 1):
            for x in (19, 20):
                px[y][x] = CRIMSON + (255,)
        px[S - 1][19] = CRIMSON + (255,)
    return px


def fusion_tome(a, b, ca, cb):
    """Cover split corner to corner: parent A's color and emblem top-left, parent B's bottom-right, bright seam between."""
    px = _blank()
    split = (SPINE + 1 + X1 + Y0 + Y1) // 2
    in_a = lambda x, y: x + y < split
    _cover(px, lambda x, y: ca[0] if in_a(x, y) else cb[0], SILVER, frame=1, kind='ascended')
    for y in range(Y0, Y1 + 1):
        for x in range(SPINE + 1, X1 + 1):
            if x + y == split:
                px[y][x] = (230, 248, 255, 255)
    size = 10
    _paint(px, emblem_mask(a, size), SPINE + 3, Y0 + 3, ca[1], _lerp(ca[0], (0, 0, 0), 0.65), region=lambda x, y: x + y < split)
    _paint(px, emblem_mask(b, size), X1 - 2 - size, Y1 - 2 - size, cb[1], _lerp(cb[0], (0, 0, 0), 0.65), region=lambda x, y: x + y > split)
    return px


def triad_tome(parts, colors):
    """Three bands, one per parent: its cover color and its emblem, with bright seams between them."""
    px = _blank()
    face = (Y1 - Y0 + 1)
    band = lambda y: min(2, (y - Y0) * 3 // face)
    _cover(px, lambda x, y: colors[band(y)][0], GOLD, frame=1, kind='mythic')
    for y in range(Y0 + 1, Y1):
        if band(y) != band(y - 1):
            for x in range(SPINE + 1, X1 + 1):
                px[y][x] = (230, 248, 255, 255)
    size = 7
    for i, (pid, col) in enumerate(zip(parts, colors)):
        top = Y0 + i * face // 3 + (face // 3 - size) // 2 + 1
        left = (SPINE + 1 + X1) // 2 - size // 2 + (i - 1) * 3
        _paint(px, emblem_mask(pid, size), left, top, col[1], _lerp(col[0], (0, 0, 0), 0.65), region=lambda x, y, i=i: band(y) == i)
    return px


def blank_tome():
    px = _blank()
    _cover(px, lambda x, y: (112, 74, 46), SILVER, frame=1)
    return px


def chaos_tome():
    """Rainbow cover that shifts corner to corner, gold frame, a white starburst emblem."""
    import colorsys
    px = _blank()
    rainbow = lambda x, y: tuple(int(c * 255) for c in colorsys.hsv_to_rgb(((x + y) / 40.0) % 1.0, 0.62, 0.78))
    _cover(px, rainbow, GOLD, frame=1, kind='mythic')
    _paint(px, raster(add(poly(star_pts(0.5, 0.5, 0.48, 0.18, 6))) + cut(circ(0.5, 0.5, 0.08)), 13), 12, 8, (255, 255, 255), (40, 20, 60), (255, 255, 255))
    return px


# ---- Foils: a see-through overlay drawn over a tome's cover (the tome model's second layer) ----

def _rainbow(t):
    import colorsys
    return tuple(int(c * 255) for c in colorsys.hsv_to_rgb(t % 1.0, 0.7, 1.0))


def foil_overlay(foil):
    """32x32 RGBA overlay for one foil, covering the tome's front cover. Better foils are bolder and busier."""
    import random
    rnd = random.Random(foil)
    px = [[(0, 0, 0, 0)] * S for _ in range(S)]
    fx0, fx1, fy0, fy1 = SPINE + 1, X1, Y0, Y1
    face = lambda x, y: fx0 <= x <= fx1 and fy0 <= y <= fy1
    border = lambda x, y, w: face(x, y) and (x - fx0 < w or fx1 - x < w or y - fy0 < w or fy1 - y < w)
    put = lambda x, y, c, a: px[y].__setitem__(x, tuple(c) + (a,))
    for y in range(S):
        for x in range(S):
            if not face(x, y):
                continue
            d = (x + y) / 40.0
            if foil == 'bronze':
                if border(x, y, 1): put(x, y, (214, 130, 70), 210)
                elif abs((x - y) - 4) < 2: put(x, y, (255, 200, 150), 80)
            elif foil == 'silver':
                if border(x, y, 1): put(x, y, (235, 240, 250), 220)
                elif abs((x - y) - 6) < 2 or abs((x - y) + 6) < 1: put(x, y, (255, 255, 255), 95)
            elif foil == 'gold':
                if border(x, y, 2): put(x, y, (255, 214, 70) if border(x, y, 1) else (200, 140, 30), 235)
                elif abs((x - y) - 3) < 2: put(x, y, (255, 240, 170), 100)
            elif foil == 'holo':
                if border(x, y, 1): put(x, y, _rainbow(d * 2), 230)
                else: put(x, y, _rainbow(d * 3 + ((x - y) % 6) / 18.0), 85)
            elif foil == 'prismatic':
                if border(x, y, 2): put(x, y, _rainbow(d * 2 + 0.3), 240)
                else: put(x, y, _rainbow(d * 4), 100)
            elif foil == 'galaxy':
                if border(x, y, 1): put(x, y, (190, 120, 255), 235)
                else:
                    t = (y - fy0) / float(fy1 - fy0)
                    put(x, y, (int(40 + 60 * t), int(20 + 10 * t), int(90 + 60 * (1 - t))), 150)
            elif foil == 'void':
                if border(x, y, 2): put(x, y, (200, 90, 255) if border(x, y, 1) else (110, 30, 170), 245)
                else: put(x, y, (8, 4, 14), 165)
            elif foil == 'celestial':
                if border(x, y, 2): put(x, y, (255, 236, 150) if border(x, y, 1) else (255, 255, 255), 245)
                else:
                    cx, cy = (fx0 + fx1) / 2.0, (fy0 + fy1) / 2.0
                    import math
                    ang = math.atan2(y - cy, x - cx)
                    ray = (math.cos(ang * 8) + 1) / 2
                    put(x, y, (255, 250, 220), int(60 + 80 * ray))
    # Sparkles on the better foils
    stars = {'gold': 3, 'prismatic': 5, 'galaxy': 14, 'void': 6, 'celestial': 10}.get(foil, 0)
    for _ in range(stars):
        x, y = rnd.randint(fx0 + 2, fx1 - 2), rnd.randint(fy0 + 2, fy1 - 2)
        c = (200, 120, 255) if foil == 'void' else (255, 255, 255)
        put(x, y, c, 255)
        if foil in ('galaxy', 'celestial') and rnd.random() < 0.4:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if face(x + dx, y + dy): put(x + dx, y + dy, c, 150)
    return px


def foil_sheet(foil):
    """A Foil Sheet icon: a slightly curled sheet showing the foil's finish."""
    over = foil_overlay(foil)
    px = [[(0, 0, 0, 0)] * S for _ in range(S)]
    for y in range(4, 28):
        for x in range(6, 26):
            curl = x > 21 and y < 4 + (x - 21)
            if curl:
                continue
            base = (210, 214, 224)
            # Sample the overlay from the cover area, stretched over the sheet
            ox = SPINE + 1 + int((x - 6) / 20.0 * (X1 - SPINE - 1))
            oy = Y0 + int((y - 4) / 24.0 * (Y1 - Y0))
            r, g, b, a = over[oy][ox]
            if a == 0 and foil in ('bronze', 'silver', 'gold'):
                tint = {'bronze': (200, 130, 80), 'silver': (220, 226, 236), 'gold': (240, 200, 90)}[foil]
                c = tuple(int(base[i] * 0.3 + tint[i] * 0.7) for i in range(3))
            else:
                c = tuple(int(base[i] * (1 - a / 255.0) + (r, g, b)[i] * (a / 255.0)) for i in range(3))
            px[y][x] = c + (255,)
    for x in range(22, 26):
        for y in range(4, 4 + (x - 21)):
            if y < 28 and x - 21 > y - 4:
                px[y][x] = (150, 150, 160, 255)
    return px

