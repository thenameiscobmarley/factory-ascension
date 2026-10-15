"""Ascendite tools and armor for Factory Ascension, drawn natively at 32x32 (icons) and 128x64 (worn armor).

The look: dark gunmetal steel, glowing teal energy channels, gold trim and collars, violet gems, and
leather-wrapped grips with gold bands. Each piece is a stack of layers (shapes from tome_art + a material);
every material shades itself, lit from the top-left like the rest of the pack's textures.
"""
import math

from tome_art import add, cut, arc, circ, ell, poly, raster, rect, seg
from tome_art import flame as flame_shape

S = 32


def _mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


# Material: (light, mid, dark). Gradient runs top-left (light) to bottom-right (dark)
MATERIALS = {
    'steel': ((150, 162, 182), (72, 80, 98), (24, 26, 36)),
    'steel_dk': ((92, 100, 120), (44, 48, 62), (16, 17, 24)),
    'teal': ((210, 255, 246), (64, 232, 212), (16, 150, 150)),
    'gold': ((255, 236, 150), (232, 176, 58), (140, 84, 22)),
    'gem': ((245, 214, 255), (170, 96, 255), (78, 30, 150)),
    'grip': ((150, 104, 70), (96, 60, 40), (50, 30, 20)),
    'iron': ((240, 240, 244), (172, 174, 182), (88, 90, 100)),
    'wood': ((176, 128, 80), (122, 86, 50), (70, 48, 28)),
    'cloth': ((255, 255, 255), (222, 226, 236), (150, 156, 176)),
    'fire': ((255, 244, 170), (255, 146, 30), (190, 44, 10)),
    'glass': ((236, 252, 255), (164, 214, 236), (84, 126, 156)),
    'red': ((255, 170, 170), (226, 44, 44), (112, 12, 12)),
    'rubber': ((96, 96, 104), (52, 52, 60), (20, 20, 26)),
    'iron': ((240, 240, 244), (172, 174, 182), (88, 90, 100)),
}


def _shade(mat, t):
    t = max(0.0, min(1.0, t))
    light, mid, dark = MATERIALS[mat]
    return _mix(light, mid, t * 2) if t < 0.5 else _mix(mid, dark, (t - 0.5) * 2)


def draw(layers, w=S, h=S):
    """layers: [(material, shape list)]. Each layer: diagonal gradient, lit rim top-left, shadow rim bottom-right.
    Teal layers glow onto the pixels around them; gems get a white glint."""
    px = [[(0, 0, 0, 0)] * w for _ in range(h)]
    for mat, shapes in layers:
        mask = raster(shapes, w) if w == h else _raster_rect(shapes, w, h)
        on = lambda x, y: 0 <= x < w and 0 <= y < h and mask[y][x]
        if mat == 'teal':
            for y in range(h):
                for x in range(w):
                    if not on(x, y) and px[y][x][3] and any(on(x + dx, y + dy) for dx in (-1, 0, 1) for dy in (-1, 0, 1)):
                        px[y][x] = _mix(px[y][x][:3], MATERIALS['teal'][1], 0.22) + (255,)
        glint = None
        for y in range(h):
            for x in range(w):
                if not on(x, y):
                    continue
                t = 0.25 + 0.5 * (x + y) / float(w + h)
                if not on(x - 1, y) or not on(x, y - 1):
                    t -= 0.22
                if not on(x + 1, y) or not on(x, y + 1):
                    t += 0.22
                px[y][x] = _shade(mat, max(0.0, min(1.0, t))) + (255,)
                if glint is None:
                    glint = (x, y)
        if mat == 'gem' and glint:
            gx, gy = glint
            if on(gx + 1, gy + 1):
                px[gy + 1][gx + 1] = (255, 255, 255, 255)
    return px


def _raster_rect(shapes, w, h, ss=3):
    out = []
    for py in range(h):
        row = []
        for qx in range(w):
            hits = 0
            for sy in range(ss):
                for sx in range(ss):
                    u, v = (qx + (sx + 0.5) / ss) / w, (py + (sy + 0.5) / ss) / h
                    inside = False
                    for op, f in shapes:
                        if f(u, v):
                            inside = op == '+'
                    hits += inside
            row.append(hits * 2 >= ss * ss)
        out.append(row)
    return out


def _handle(x0, y0, x1, y1, bands=3):
    """Leather grip from (x0, y0) to (x1, y1) with gold bands and a gold end cap."""
    layers = [('grip', add(seg(x0, y0, x1, y1, 0.085)))]
    bl = []
    for k in range(1, bands + 1):
        t = k / (bands + 1.0)
        bx, by = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
        dx, dy = (x1 - x0), (y1 - y0)
        ln = math.hypot(dx, dy)
        nx, ny = -dy / ln * 0.06, dx / ln * 0.06
        bl += add(seg(bx - nx, by - ny, bx + nx, by + ny, 0.035))
    layers.append(('gold', bl + add(circ(x0, y0, 0.06))))
    return layers


# ---- Icons ----

def sword():
    return draw([
        ('steel', add(poly([(0.27, 0.58), (0.8, 0.05), (0.97, 0.03), (0.95, 0.2), (0.42, 0.73)]))),
        ('teal', add(seg(0.4, 0.6, 0.84, 0.16, 0.028))),
        ('gold', add(seg(0.2, 0.5, 0.5, 0.8, 0.075), poly([(0.17, 0.47), (0.25, 0.47), (0.21, 0.55)]), poly([(0.53, 0.83), (0.53, 0.75), (0.45, 0.79)]))),
        ('gem', add(circ(0.35, 0.65, 0.055))),
    ] + _handle(0.08, 0.92, 0.3, 0.7, 2))


def pickaxe():
    return draw(_handle(0.08, 0.92, 0.62, 0.38, 3) + [
        ('steel', add(arc(0.36, 0.64, 0.58, 252, 378, 0.19), poly([(0.2, 0.1), (0.3, 0.02), (0.36, 0.12)]), poly([(0.9, 0.8), (0.98, 0.7), (0.88, 0.64)]))),
        ('teal', add(arc(0.36, 0.64, 0.555, 266, 364, 0.026))),
        ('gold', add(circ(0.62, 0.38, 0.09))),
        ('gem', add(circ(0.62, 0.38, 0.045))),
    ])


def axe():
    return draw(_handle(0.08, 0.92, 0.68, 0.32, 3) + [
        ('steel', add(poly([(0.56, 0.1), (0.82, 0.02), (0.98, 0.2), (0.96, 0.52), (0.8, 0.62), (0.6, 0.42)]),
                      poly([(0.6, 0.3), (0.42, 0.2), (0.38, 0.32), (0.56, 0.4)]))),
        ('teal', add(arc(0.6, 0.32, 0.34, -62, 38, 0.035))),
        ('gold', add(circ(0.68, 0.32, 0.08))),
        ('gem', add(circ(0.68, 0.32, 0.04))),
    ])


def shovel():
    def spade(u, v):
        cu, cv = u - 0.74, v - 0.26
        a, b = (cu - cv) * 0.7071, (cu + cv) * 0.7071
        return (a / 0.17) ** 2 + (b / 0.24) ** 2 <= 1 and b > -0.18
    return draw(_handle(0.08, 0.92, 0.58, 0.42, 3) + [
        ('steel', [('+', spade)]),
        ('teal', add(seg(0.6, 0.4, 0.84, 0.16, 0.035))),
        ('gold', add(circ(0.58, 0.42, 0.08))),
        ('gem', add(circ(0.58, 0.42, 0.04))),
    ])


def hoe():
    return draw(_handle(0.08, 0.92, 0.66, 0.34, 3) + [
        ('steel', add(poly([(0.54, 0.16), (0.9, 0.08), (0.98, 0.18), (0.88, 0.36), (0.8, 0.26), (0.62, 0.28)]))),
        ('teal', add(seg(0.66, 0.2, 0.9, 0.16, 0.03))),
        ('gold', add(circ(0.66, 0.34, 0.08))),
        ('gem', add(circ(0.66, 0.34, 0.04))),
    ])


def helmet():
    return draw([
        ('steel', add(ell(0.5, 0.5, 0.4, 0.4), rect(0.1, 0.5, 0.9, 0.86)) + cut(rect(0.0, 0.88, 1.0, 1.0), rect(0.3, 0.6, 0.7, 0.9))),
        ('steel_dk', add(rect(0.3, 0.6, 0.7, 0.66))),
        ('teal', add(rect(0.3, 0.52, 0.7, 0.57))),
        ('gold', add(arc(0.5, 0.5, 0.4, 200, 340, 0.05), seg(0.5, 0.1, 0.5, 0.5, 0.07), rect(0.46, 0.5, 0.54, 0.78),
                     rect(0.1, 0.82, 0.3, 0.87), rect(0.7, 0.82, 0.9, 0.87),
                     poly([(0.12, 0.44), (0.0, 0.14), (0.22, 0.34)]), poly([(0.88, 0.44), (1.0, 0.14), (0.78, 0.34)]))),
        ('gem', add(circ(0.5, 0.32, 0.065))),
    ])


def chestplate():
    return draw([
        ('steel', add(poly([(0.22, 0.12), (0.4, 0.2), (0.6, 0.2), (0.78, 0.12), (0.96, 0.3), (0.84, 0.44), (0.8, 0.92), (0.2, 0.92), (0.16, 0.44), (0.04, 0.3)]))),
        ('steel_dk', add(rect(0.3, 0.62, 0.7, 0.66), rect(0.3, 0.74, 0.7, 0.78))),
        ('teal', add(seg(0.3, 0.3, 0.5, 0.52, 0.04), seg(0.7, 0.3, 0.5, 0.52, 0.04), seg(0.5, 0.52, 0.5, 0.86, 0.04))),
        ('gold', add(poly([(0.04, 0.3), (0.22, 0.12), (0.34, 0.18), (0.16, 0.4)]), poly([(0.96, 0.3), (0.78, 0.12), (0.66, 0.18), (0.84, 0.4)]),
                     arc(0.5, 0.18, 0.12, 0, 180, 0.05), rect(0.2, 0.88, 0.8, 0.93))),
        ('gem', add(circ(0.5, 0.5, 0.075))),
    ])


def leggings():
    return draw([
        ('steel', add(rect(0.2, 0.12, 0.8, 0.36), poly([(0.2, 0.3), (0.48, 0.3), (0.44, 0.92), (0.22, 0.92)]), poly([(0.52, 0.3), (0.8, 0.3), (0.78, 0.92), (0.56, 0.92)]))),
        ('teal', add(seg(0.26, 0.4, 0.27, 0.86, 0.035), seg(0.74, 0.4, 0.73, 0.86, 0.035))),
        ('gold', add(rect(0.18, 0.1, 0.82, 0.2), ell(0.34, 0.62, 0.09, 0.07), ell(0.66, 0.62, 0.09, 0.07))),
        ('gem', add(circ(0.5, 0.15, 0.06))),
    ])


def boots():
    boot = lambda x, f: add(rect(x - 0.12, 0.3, x + 0.1, 0.72), poly([(x - 0.12, 0.6), (x + 0.1 + 0.16 * f, 0.66), (x + 0.12 + 0.16 * f, 0.86), (x - 0.14, 0.86)]))
    return draw([
        ('steel', boot(0.3, -1) + boot(0.7, 1)),
        ('teal', add(rect(0.14, 0.8, 0.44, 0.83), rect(0.56, 0.8, 0.86, 0.83))),
        ('gold', add(rect(0.17, 0.28, 0.41, 0.36), rect(0.59, 0.28, 0.83, 0.36), circ(0.18, 0.76, 0.05), circ(0.82, 0.76, 0.05))),
        ('gem', add(circ(0.29, 0.5, 0.045), circ(0.71, 0.5, 0.045))),
    ])


def _hammer_head():
    return add(poly([(0.4, 0.3), (0.64, 0.06), (0.94, 0.36), (0.7, 0.6)]))


def rock_hammer():
    return draw([
        ('wood', add(seg(0.1, 0.9, 0.62, 0.38, 0.085))),
        ('iron', _hammer_head()),
        ('steel_dk', add(seg(0.48, 0.22, 0.78, 0.52, 0.04))),
    ])


def ascendite_rock_hammer():
    return draw(_handle(0.1, 0.9, 0.6, 0.4, 3) + [
        ('steel', _hammer_head()),
        ('gold', add(seg(0.46, 0.24, 0.76, 0.54, 0.07), poly([(0.64, 0.06), (0.72, 0.0), (0.98, 0.26), (0.94, 0.36)]))),
        ('teal', add(seg(0.56, 0.2, 0.82, 0.46, 0.03))),
        ('gem', add(circ(0.61, 0.39, 0.045))),
    ])


def polishing_cloth():
    """A folded cloth with a teal hem and a couple of sparkles."""
    from tome_art import star_pts
    return draw([
        ('cloth', add(poly([(0.14, 0.38), (0.62, 0.2), (0.88, 0.5), (0.4, 0.74)]))),
        ('cloth', add(poly([(0.14, 0.38), (0.4, 0.74), (0.38, 0.88), (0.1, 0.52)]))),
        ('teal', add(seg(0.4, 0.74, 0.88, 0.5, 0.035), seg(0.38, 0.86, 0.4, 0.74, 0.03))),
        ('gold', add(poly(star_pts(0.78, 0.18, 0.14, 0.05, 4)), poly(star_pts(0.22, 0.2, 0.08, 0.03, 4)))),
    ])


def _foil(x, y, t=0.0):
    """Holographic foil: hue sweeps diagonally, with a soft shine band."""
    import colorsys
    hue = ((x + y) / 44.0 + t) % 1.0
    r, g, b = colorsys.hsv_to_rgb(hue, 0.55, 0.95)
    shine = max(0.0, 1 - abs((x - y) - 4) / 3.0)
    return _mix((int(r * 255), int(g * 255), int(b * 255)), (255, 255, 255), shine * 0.6)


def enchantment_pack():
    """A foil booster pack: crimped top and bottom, rainbow foil body, a dark window with a gold star."""
    from tome_art import star_pts
    px = [[(0, 0, 0, 0)] * S for _ in range(S)]
    for y in range(3, 29):
        for x in range(7, 25):
            crimp = y < 6 or y > 25
            if crimp and (x + (0 if y < 6 else 1)) % 2 == 0 and y in (3, 28):
                continue
            c = _foil(x, y)
            if crimp:
                c = _mix(c, (200, 200, 210), 0.5)
                if y in (5, 26):
                    c = _mix(c, (90, 90, 110), 0.5)
            elif x in (7, 24):
                c = _mix(c, (60, 40, 90), 0.45)
            px[y][x] = c + (255,)
    window = raster(add(rect(0.32, 0.3, 0.68, 0.72)), S)
    star = raster(add(poly(star_pts(0.5, 0.5, 0.14, 0.06, 5))), S)
    for y in range(S):
        for x in range(S):
            if window[y][x] and px[y][x][3]:
                px[y][x] = _mix((30, 18, 50), (70, 40, 110), y / 32.0) + (255,)
            if star[y][x]:
                px[y][x] = _shade('gold', 0.2 + 0.4 * (y / 32.0)) + (255,)
    return px


def booster_box():
    """A dark box seen at an angle, with a foil band and a gold star."""
    from tome_art import star_pts
    px = [[(0, 0, 0, 0)] * S for _ in range(S)]
    for y in range(6, 28):
        for x in range(4, 28):
            top = y < 11
            side = x > 22
            c = (64, 40, 96) if not top else (92, 62, 132)
            if side:
                c = (42, 26, 64)
            if 15 <= y <= 18 and not top:
                c = _foil(x, y, 0.3) if not side else _mix(_foil(x, y, 0.3), (0, 0, 0), 0.35)
            if x == 4 or y == 6 or (top and y == 10) or x == 22:
                c = _mix(c, (255, 255, 255), 0.25)
            px[y][x] = c + (255,)
    star = raster(add(poly(star_pts(0.4, 0.73, 0.1, 0.04, 5))), S)
    for y in range(S):
        for x in range(S):
            if star[y][x] and px[y][x][3]:
                px[y][x] = _shade('gold', 0.3) + (255,)
    return px


def gear_cache():
    """A sealed wooden crate: planks, iron corner bands, a glowing teal seal with a gold rim."""
    return draw([
        ('wood', add(rect(0.12, 0.22, 0.88, 0.86))),
        ('grip', add(rect(0.12, 0.42, 0.88, 0.45), rect(0.12, 0.63, 0.88, 0.66))),
        ('steel', add(rect(0.1, 0.2, 0.22, 0.88), rect(0.78, 0.2, 0.9, 0.88), rect(0.1, 0.18, 0.9, 0.28))),
        ('gold', add(circ(0.5, 0.56, 0.15))),
        ('teal', add(circ(0.5, 0.56, 0.09))),
        ('gem', add(circ(0.5, 0.56, 0.04))),
    ])


# ---- Attachment icons ----
def _att(layers):
    return lambda: draw(layers)


def _spikes():
    out = []
    for k in range(4):
        x = 0.2 + k * 0.2
        out += add(poly([(x - 0.07, 0.36), (x + 0.07, 0.36), (x, 0.12)]))
    return out


ATTACHMENTS = {
    'headlamp': _att([('rubber', add(arc(0.5, 0.62, 0.38, 180, 360, 0.12))), ('steel', add(circ(0.5, 0.36, 0.17))),
                      ('glass', add(circ(0.5, 0.36, 0.11))), ('fire', add(poly([(0.42, 0.12), (0.58, 0.12), (0.7, 0.0), (0.3, 0.0)])))]),
    'rebreather': _att([('rubber', add(ell(0.5, 0.5, 0.3, 0.26))), ('glass', add(circ(0.38, 0.44, 0.09), circ(0.62, 0.44, 0.09))),
                        ('steel', add(rect(0.42, 0.6, 0.58, 0.8))), ('teal', add(arc(0.74, 0.74, 0.18, 0, 140, 0.07)))]),
    'thermal_visor': _att([('rubber', add(rect(0.08, 0.42, 0.92, 0.56))), ('steel', add(circ(0.32, 0.5, 0.16), circ(0.68, 0.5, 0.16))),
                           ('red', add(circ(0.32, 0.5, 0.1), circ(0.68, 0.5, 0.1)))]),
    'jetpack': _att([('steel', add(rect(0.2, 0.14, 0.44, 0.7), rect(0.56, 0.14, 0.8, 0.7))), ('gold', add(rect(0.2, 0.34, 0.8, 0.4))),
                     ('fire', add(poly([(0.22, 0.72), (0.42, 0.72), (0.32, 0.96)]), poly([(0.58, 0.72), (0.78, 0.72), (0.68, 0.96)])))]),
    'armor_plating': _att([('steel', add(rect(0.14, 0.14, 0.86, 0.86))), ('gold', add(circ(0.24, 0.24, 0.05), circ(0.76, 0.24, 0.05), circ(0.24, 0.76, 0.05), circ(0.76, 0.76, 0.05))),
                           ('steel_dk', add(rect(0.3, 0.46, 0.7, 0.54)))]),
    'spike_plating': _att([('steel', add(rect(0.1, 0.36, 0.9, 0.86))), ('iron', _spikes()), ('gold', add(circ(0.5, 0.6, 0.07)))]),
    'knee_pads': _att([('rubber', add(ell(0.5, 0.52, 0.32, 0.36))), ('steel', add(ell(0.5, 0.48, 0.2, 0.22))), ('teal', add(seg(0.36, 0.48, 0.64, 0.48, 0.04)))]),
    'hydraulics': _att([('steel', add(rect(0.34, 0.08, 0.66, 0.5))), ('iron', add(rect(0.42, 0.5, 0.58, 0.9))), ('gold', add(rect(0.28, 0.46, 0.72, 0.54), rect(0.3, 0.86, 0.7, 0.94)))]),
    'rocket_boots': _att([('steel', add(rect(0.26, 0.14, 0.56, 0.6), poly([(0.26, 0.5), (0.84, 0.56), (0.84, 0.74), (0.26, 0.74)]))),
                          ('fire', add(poly([(0.3, 0.76), (0.56, 0.76), (0.43, 0.98)]))), ('gold', add(rect(0.26, 0.14, 0.56, 0.22)))]),
    'spring_coils': _att([('iron', add(*[seg(0.3, 0.16 + k * 0.14, 0.7, 0.24 + k * 0.14, 0.07) for k in range(5)])), ('steel', add(rect(0.24, 0.84, 0.76, 0.92), rect(0.24, 0.08, 0.76, 0.16)))]),
    'chainsaw': _att([('steel', add(poly([(0.08, 0.6), (0.2, 0.4), (0.92, 0.4), (0.92, 0.6)]))), ('iron', add(*[poly([(0.24 + k * 0.12, 0.4), (0.32 + k * 0.12, 0.4), (0.28 + k * 0.12, 0.3)]) for k in range(6)])),
                      ('red', add(rect(0.08, 0.6, 0.36, 0.86))), ('gold', add(circ(0.22, 0.73, 0.06)))]),
    'taser': _att([('rubber', add(rect(0.2, 0.42, 0.8, 0.86))), ('gold', add(rect(0.3, 0.2, 0.38, 0.42), rect(0.62, 0.2, 0.7, 0.42))), ('teal', add(seg(0.34, 0.18, 0.66, 0.18, 0.05), seg(0.5, 0.04, 0.5, 0.18, 0.04)))]),
    'flamethrower': _att([('steel', add(rect(0.08, 0.44, 0.6, 0.64))), ('iron', add(poly([(0.6, 0.4), (0.74, 0.34), (0.74, 0.74), (0.6, 0.68)]))), ('fire', flame_shape(0.86, 0.54, 0.4))]),
    'serrated_edge': _att([('iron', add(poly([(0.1, 0.62), (0.9, 0.3), (0.9, 0.5)]))), ('iron', add(*[poly([(0.2 + k * 0.14, 0.62 - k * 0.06), (0.27 + k * 0.14, 0.74 - k * 0.06), (0.34 + k * 0.14, 0.56 - k * 0.06)]) for k in range(5)])),
                           ('steel', add(rect(0.06, 0.6, 0.18, 0.7)))]),
    'scope': _att([('steel', add(rect(0.12, 0.38, 0.88, 0.56))), ('glass', add(circ(0.86, 0.47, 0.1))), ('gold', add(rect(0.3, 0.34, 0.36, 0.6), rect(0.6, 0.34, 0.66, 0.6))), ('steel_dk', add(rect(0.42, 0.56, 0.52, 0.78)))]),
    'drill_bit': _att([('iron', add(poly([(0.3, 0.16), (0.7, 0.16), (0.5, 0.94)]))), ('steel', add(*[seg(0.34 + k * 0.03, 0.26 + k * 0.16, 0.66 - k * 0.03, 0.34 + k * 0.16, 0.04) for k in range(4)])),
                       ('gold', add(rect(0.28, 0.06, 0.72, 0.16)))]),
    'mining_lamp': _att([('steel', add(rect(0.3, 0.2, 0.7, 0.8))), ('glass', add(rect(0.36, 0.3, 0.64, 0.7))), ('fire', add(ell(0.5, 0.52, 0.08, 0.12))), ('steel', add(arc(0.5, 0.2, 0.14, 180, 360, 0.05)))]),
}

MATERIALS.setdefault('copper', ((255, 196, 150), (200, 110, 62), (96, 44, 24)))
MATERIALS.setdefault('bone', ((250, 248, 236), (214, 206, 180), (140, 130, 104)))
MATERIALS.setdefault('plasma', ((230, 250, 255), (120, 200, 255), (40, 90, 200)))


def _bolt(x, y, k=1.0):
    """A small lightning bolt centered near (x, y)."""
    return add(poly([(x - 0.04 * k, y - 0.16 * k), (x + 0.08 * k, y - 0.16 * k), (x + 0.01 * k, y - 0.02 * k), (x + 0.09 * k, y - 0.02 * k),
                     (x - 0.07 * k, y + 0.2 * k), (x - 0.01 * k, y + 0.03 * k), (x - 0.09 * k, y + 0.03 * k)]))


ATTACHMENTS.update({
    # Copper wire wound round a grip, iron spikes poking out, a spark
    'electric_spike_wrap': _att([('copper', add(*[seg(0.22, 0.3 + k * 0.1, 0.78, 0.38 + k * 0.1, 0.05) for k in range(5)])),
                                 ('iron', add(*[poly([(0.14, 0.32 + k * 0.14), (0.24, 0.28 + k * 0.14), (0.24, 0.38 + k * 0.14)]) for k in range(4)],
                                              *[poly([(0.86, 0.36 + k * 0.14), (0.76, 0.32 + k * 0.14), (0.76, 0.42 + k * 0.14)]) for k in range(4)])),
                                 ('teal', _bolt(0.5, 0.16, 0.9))]),
    # A blade edge glowing blue-white
    'plasma_edge': _att([('steel', add(poly([(0.08, 0.72), (0.86, 0.22), (0.92, 0.3), (0.16, 0.82)]))),
                         ('plasma', add(poly([(0.1, 0.66), (0.84, 0.18), (0.88, 0.22), (0.12, 0.72)]))), ('gold', add(rect(0.06, 0.74, 0.2, 0.86)))]),
    # A capacitor can with a charge gauge and a bolt
    'overcharger': _att([('steel', add(rect(0.26, 0.2, 0.74, 0.86))), ('gold', add(rect(0.22, 0.14, 0.78, 0.24), rect(0.22, 0.82, 0.78, 0.9))),
                         ('teal', add(rect(0.34, 0.36, 0.66, 0.42), rect(0.34, 0.5, 0.66, 0.56), rect(0.34, 0.64, 0.66, 0.7))), ('gem', _bolt(0.5, 0.06, 0.6))]),
    # A crossguard with two copper coils and an arc between them
    'tesla_guard': _att([('steel', add(rect(0.1, 0.54, 0.9, 0.66))), ('copper', add(*[rect(0.14, 0.22 + k * 0.08, 0.3, 0.27 + k * 0.08) for k in range(4)],
                                                                                   *[rect(0.7, 0.22 + k * 0.08, 0.86, 0.27 + k * 0.08) for k in range(4)])),
                         ('teal', add(arc(0.5, 0.3, 0.22, 180, 360, 0.04))), ('grip', add(rect(0.44, 0.66, 0.56, 0.94)))]),
    # Two white fangs on a red band
    'vampire_fangs': _att([('red', add(rect(0.12, 0.2, 0.88, 0.36))), ('bone', add(poly([(0.24, 0.36), (0.42, 0.36), (0.33, 0.86)]), poly([(0.58, 0.36), (0.76, 0.36), (0.67, 0.86)]))),
                           ('red', add(circ(0.33, 0.82, 0.04), circ(0.67, 0.82, 0.04)))]),
    # A heavy iron ball on a chain link
    'weighted_pommel': _att([('steel_dk', add(arc(0.5, 0.2, 0.1, 0, 360, 0.05))), ('iron', add(circ(0.5, 0.6, 0.28))), ('gold', add(rect(0.38, 0.3, 0.62, 0.36)))]),
    # A round lens with red crosshairs
    'targeting_lens': _att([('steel', add(circ(0.5, 0.5, 0.36))), ('glass', add(circ(0.5, 0.5, 0.27))),
                            ('red', add(rect(0.48, 0.26, 0.52, 0.74), rect(0.26, 0.48, 0.74, 0.52))), ('red', add(circ(0.5, 0.5, 0.06)))]),
})


def _battery(body, band, cells):
    """An upright battery: colored body, steel caps, a + terminal and teal charge cells."""
    return _att([(body, add(rect(0.28, 0.2, 0.72, 0.9))), ('steel', add(rect(0.26, 0.16, 0.74, 0.24), rect(0.26, 0.86, 0.74, 0.94), rect(0.42, 0.06, 0.58, 0.16))),
                 (band, add(rect(0.28, 0.3, 0.72, 0.36))),
                 ('teal', add(*[rect(0.38, 0.44 + k * (0.36 / cells), 0.62, 0.44 + k * (0.36 / cells) + 0.36 / cells - 0.03) for k in range(cells)]))])


BATTERIES = {
    'copper_battery': _battery('copper', 'steel_dk', 2),
    'ascendite_battery': _battery('steel_dk', 'gold', 3),
    'essence_cell': _att([('glass', add(rect(0.26, 0.16, 0.74, 0.9))), ('gem', add(rect(0.32, 0.3, 0.68, 0.84))), ('gold', add(rect(0.22, 0.1, 0.78, 0.18), rect(0.22, 0.88, 0.78, 0.96))),
                          ('teal', _bolt(0.5, 0.56, 1.1))]),
}


def room_tnt(shell, light, size_index):
    """Factory Room TNT: a TNT block whose label band is the room's wall block, with a glowing lamp (the room's lights),
    gold dots for the size (1, 2 or 3) and a lit fuse on top."""
    px = [[(0, 0, 0, 0)] * S for _ in range(S)]
    red = ((255, 120, 110), (200, 40, 36), (96, 14, 12))

    def tone(pal, t):
        t = max(0.0, min(1.0, t))
        return _mix(pal[0], pal[1], t * 2) if t < 0.5 else _mix(pal[1], pal[2], (t - 0.5) * 2)
    for y in range(8, 29):
        for x in range(5, 27):
            top = y < 12
            side = x > 21
            if top and (x - 5) < (12 - y) - 1:
                continue                                   # the top face's slanted left edge
            if top:
                c = tone(red, 0.25 + 0.05 * ((x + y) % 2))
            elif side:
                c = tone(red, 0.8)
            else:
                c = tone(red, 0.45 + 0.04 * ((x // 3) % 2))  # TNT sticks
                if (x - 5) % 4 == 0:
                    c = tone(red, 0.7)
            if 16 <= y <= 22 and not top:                  # label band in the wall block's color
                c = _mix(shell, (0, 0, 0), 0.38) if side else _mix(shell, (255, 255, 255), 0.12 if y == 16 else 0)
                if y == 22:
                    c = _mix(shell, (0, 0, 0), 0.5)
            if y == 12 and not side:
                c = tone(red, 0.9)
            px[y][x] = c + (255,)
    for y in range(17, 22):                                # the lamp
        for x in range(7, 11):
            px[y][x] = _mix(light, (255, 255, 255), 0.35 if (x, y) == (8, 18) else 0) + (255,)
    for i in range(size_index + 1):                        # size dots
        cx = 13 + i * 3
        px[19][cx] = _shade('gold', 0.2) + (255,)
        px[19][cx + 1] = _shade('gold', 0.45) + (255,)
        px[20][cx] = _shade('gold', 0.55) + (255,)
        px[20][cx + 1] = _shade('gold', 0.8) + (255,)
    for k, (x, y) in enumerate(((15, 10), (15, 9), (16, 8), (16, 7), (17, 6))):   # fuse
        px[y][x] = (60, 50, 44, 255)
    for x, y, c in ((18, 5, (255, 244, 170)), (17, 4, (255, 180, 60)), (19, 4, (255, 146, 30)), (18, 3, (255, 210, 120)), (19, 6, (255, 120, 30))):
        px[y][x] = c + (255,)
    return px


HAMMERS = {'rock_hammer': rock_hammer, 'ascendite_rock_hammer': ascendite_rock_hammer, 'polishing_cloth': polishing_cloth,
           'enchantment_pack': enchantment_pack, 'booster_box': booster_box, 'gear_cache': gear_cache}

def ingot():
    """A beveled ingot bar with a glowing seam."""
    return draw([
        ('steel', add(poly([(0.1, 0.62), (0.3, 0.42), (0.9, 0.42), (0.7, 0.62)]), poly([(0.1, 0.62), (0.7, 0.62), (0.7, 0.78), (0.1, 0.78)]),
                      poly([(0.7, 0.62), (0.9, 0.42), (0.9, 0.58), (0.7, 0.78)]))),
        ('teal', add(seg(0.2, 0.7, 0.62, 0.7, 0.03))),
        ('gem', add(circ(0.62, 0.52, 0.035))),
    ])


# Colors per gear tier: each replaces the base metal, the glowing channel, the trim and the gem
TIER_PALETTES = {
    'voidsteel': {
        'steel': ((128, 108, 160), (56, 40, 84), (16, 10, 30)), 'steel_dk': ((80, 62, 110), (36, 24, 56), (12, 8, 20)),
        'teal': ((246, 214, 255), (190, 104, 255), (96, 34, 170)), 'gold': ((245, 246, 252), (176, 182, 196), (86, 90, 104)),
        'gem': ((210, 255, 250), (60, 226, 214), (20, 112, 112)),
    },
    'celestium': {
        'steel': ((255, 253, 240), (226, 216, 184), (146, 134, 100)), 'steel_dk': ((206, 198, 166), (156, 146, 114), (94, 84, 62)),
        'teal': ((240, 255, 255), (130, 226, 255), (40, 126, 206)), 'gold': ((255, 236, 150), (232, 176, 58), (140, 84, 22)),
        'gem': ((214, 232, 255), (74, 124, 255), (22, 42, 146)),
    },
    'ascendium': {
        'steel': ((160, 66, 64), (76, 22, 26), (22, 6, 8)), 'steel_dk': ((96, 32, 32), (48, 12, 14), (16, 4, 6)),
        'teal': ((255, 244, 190), (255, 152, 40), (176, 62, 10)), 'gold': ((255, 236, 150), (232, 176, 58), (140, 84, 22)),
        'gem': ((255, 206, 214), (232, 40, 72), (112, 10, 30)),
    },
}


def with_palette(palette, fn, *args):
    """Run a drawing with a tier's colors swapped into the material table, then put the defaults back."""
    saved = {k: MATERIALS[k] for k in palette}
    MATERIALS.update(palette)
    try:
        return fn(*args)
    finally:
        MATERIALS.update(saved)


ICONS = {'sword': sword, 'pickaxe': pickaxe, 'axe': axe, 'shovel': shovel, 'hoe': hoe,
         'helmet': helmet, 'chestplate': chestplate, 'leggings': leggings, 'boots': boots}


# ---- Worn armor (128x64: the vanilla 64x32 armor sheet at 2x, so each model pixel is 2x2) ----
# Faces are painted to match the icons: gunmetal plates, gold trim, teal energy channels, violet gems.

class _Sheet:
    def __init__(self):
        self.px = [[(0, 0, 0, 0)] * 128 for _ in range(64)]

    def put(self, x, y, c):
        if 0 <= x < 128 and 0 <= y < 64:
            self.px[y][x] = tuple(c[:3]) + (255,)

    def plate(self, x0, y0, w, h, mat='steel'):
        """Sculpted plate: gradient, beveled edges (lit top-left, shadowed bottom-right, two pixels deep),
        brushed-metal streaks, and rivets in the corners of bigger plates."""
        import random
        rnd = random.Random(x0 * 131 + y0 * 17 + w)
        streak = [rnd.uniform(-0.05, 0.05) for _ in range(h)]
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                t = 0.3 + 0.35 * ((x - x0) / max(1, w) + (y - y0) / max(1, h)) / 2 + streak[y - y0] + rnd.uniform(-0.025, 0.025)
                dl, dt, dr, db = x - x0, y - y0, x0 + w - 1 - x, y0 + h - 1 - y
                if min(dl, dt) == 0:
                    t -= 0.2
                elif min(dl, dt) == 1 and w > 4 and h > 4:
                    t -= 0.08
                if min(dr, db) == 0:
                    t += 0.22
                elif min(dr, db) == 1 and w > 4 and h > 4:
                    t += 0.08
                self.put(x, y, _shade(mat, t))
        if mat in ('steel', 'steel_dk') and w >= 12 and h >= 12:
            for rx, ry in ((x0 + 2, y0 + 2), (x0 + w - 3, y0 + 2), (x0 + 2, y0 + h - 3), (x0 + w - 3, y0 + h - 3)):
                self.put(rx, ry, _shade('gold', 0.2))
                self.put(rx + 1, ry + 1, _shade('gold', 0.75))

    def trim(self, x0, y0, w, h, sides='tblr', mat='gold'):
        """Beveled trim: a bright outer line and, on big enough faces, a darker inner line, so it reads as a raised rim."""
        inner = w > 6 and h > 6
        for x in range(x0, x0 + w):
            if 't' in sides:
                self.put(x, y0, _shade(mat, 0.15))
                if inner:
                    self.put(x, y0 + 1, _mix(self.px[y0 + 1][x][:3], (0, 0, 0), 0.35))
            if 'b' in sides:
                self.put(x, y0 + h - 1, _shade(mat, 0.65))
        for y in range(y0, y0 + h):
            if 'l' in sides:
                self.put(x0, y, _shade(mat, 0.3))
                if inner and y0 < y < y0 + h - 1:
                    self.put(x0 + 1, y, _mix(self.px[y][x0 + 1][:3], (0, 0, 0), 0.3))
            if 'r' in sides:
                self.put(x0 + w - 1, y, _shade(mat, 0.6))

    def line(self, x0, y0, x1, y1, mat='teal', glow=True):
        n = max(abs(x1 - x0), abs(y1 - y0), 1)
        pts = [(round(x0 + (x1 - x0) * k / n), round(y0 + (y1 - y0) * k / n)) for k in range(n + 1)]
        if glow:
            for x, y in pts:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    if (x + dx, y + dy) not in pts and 0 <= x + dx < 128 and 0 <= y + dy < 64 and self.px[y + dy][x + dx][3]:
                        self.put(x + dx, y + dy, _mix(self.px[y + dy][x + dx][:3], MATERIALS['teal'][1], 0.3))
        for x, y in pts:
            self.put(x, y, _shade(mat, 0.3))

    def gem(self, cx, cy, r=2):
        """Faceted gem in a gold setting: dark setting ring, lit top facets, deep bottom facets, a glint."""
        for y in range(cy - r - 1, cy + r + 1):
            for x in range(cx - r - 1, cx + r + 1):
                if abs(x - cx + 0.5) + abs(y - cy + 0.5) <= r + 1.2:
                    self.put(x, y, _shade('gold', 0.55))
        for y in range(cy - r, cy + r):
            for x in range(cx - r, cx + r):
                if abs(x - cx + 0.5) + abs(y - cy + 0.5) <= r + 0.2:
                    self.put(x, y, _shade('gem', 0.15 if y < cy else 0.7 if x >= cx else 0.45))
        self.put(cx - 1, cy - 1, (255, 255, 255))


# Face rectangles in sheet pixels (x, y, w, h) for each box part
HEAD = {'top': (16, 0, 16, 16), 'bottom': (32, 0, 16, 16), 'right': (0, 16, 16, 16), 'front': (16, 16, 16, 16), 'left': (32, 16, 16, 16), 'back': (48, 16, 16, 16)}
BODY = {'top': (40, 32, 16, 8), 'bottom': (56, 32, 16, 8), 'right': (32, 40, 8, 24), 'front': (40, 40, 16, 24), 'left': (56, 40, 8, 24), 'back': (64, 40, 16, 24)}
ARM = {'top': (88, 32, 8, 8), 'bottom': (96, 32, 8, 8), 'right': (80, 40, 8, 24), 'front': (88, 40, 8, 24), 'left': (96, 40, 8, 24), 'back': (104, 40, 8, 24)}
LEG = {'top': (8, 32, 8, 8), 'bottom': (16, 32, 8, 8), 'right': (0, 40, 8, 24), 'front': (8, 40, 8, 24), 'left': (16, 40, 8, 24), 'back': (24, 40, 8, 24)}
SIDES = ('right', 'front', 'left', 'back')


def _helmet(sh):
    for f, (x, y, w, h) in HEAD.items():
        sh.plate(x, y, w, h)
        sh.trim(x, y, w, h, 'tblr' if f in ('top', 'bottom') else 'b')
    x, y, w, h = HEAD['top']
    for yy in range(y, y + h):                       # gold crest running front to back, teal core
        sh.put(x + 7, yy, _shade('gold', 0.3)); sh.put(x + 8, yy, _shade('gold', 0.5))
    sh.line(x + 7, y + 2, x + 7, y + h - 3)
    x, y, w, h = HEAD['front']
    sh.trim(x, y + 4, w, 2, 'tb')                    # gold brow band with the gem
    sh.gem(x + 8, y + 5, 2)
    for yy in range(y + 7, y + 9):                   # glowing eye slit
        for xx in range(x + 2, x + 14):
            if xx not in (x + 7, x + 8):
                sh.put(xx, yy, _shade('teal', 0.15))
    for yy in range(y + 9, y + 16):                  # dark face opening with a gold nose guard
        for xx in range(x + 4, x + 12):
            sh.put(xx, yy, _shade('steel_dk', 0.8))
    for yy in range(y + 6, y + 14):
        sh.put(x + 7, yy, _shade('gold', 0.3)); sh.put(x + 8, yy, _shade('gold', 0.55))
    for f in ('right', 'left'):                      # side fins and channels
        x, y, w, h = HEAD[f]
        sh.line(x + 2, y + 9, x + w - 3, y + 9)
        for k in range(5):
            sh.put(x + 3 + k, y + 4 + k // 2, _shade('gold', 0.3 + k * 0.05))
    x, y, w, h = HEAD['back']
    sh.line(x + 8, y + 2, x + 8, y + h - 3)


def _chest(sh):
    for f, (x, y, w, h) in BODY.items():
        sh.plate(x, y, w, h)
        sh.trim(x, y, w, h, 'tblr' if f in ('top', 'bottom') else 'tb')
    x, y, w, h = BODY['front']
    for k in range(4):                               # gold V collar
        sh.put(x + 3 + k, y + 1 + k, _shade('gold', 0.3)); sh.put(x + 12 - k, y + 1 + k, _shade('gold', 0.5))
    sh.line(x + 3, y + 5, x + 7, y + 10); sh.line(x + 12, y + 5, x + 8, y + 10)   # teal V to the core
    sh.gem(x + 8, y + 11, 2)
    sh.line(x + 8, y + 13, x + 8, y + 20)
    for yy in (y + 15, y + 18):                      # ab plates
        for xx in range(x + 3, x + 13):
            if xx not in (x + 7, x + 8):
                sh.put(xx, yy, _shade('steel_dk', 0.7))
    sh.trim(x, y + h - 3, w, 3, 'tb')                # gold belt line
    x, y, w, h = BODY['back']
    sh.line(x + 8, y + 2, x + 8, y + h - 4)
    for k in range(3):
        sh.put(x + 5 + k, y + 3 + k, _shade('gold', 0.4)); sh.put(x + 10 - k, y + 3 + k, _shade('gold', 0.4))
    for f in ('right', 'left'):
        x, y, w, h = BODY[f]
        sh.line(x + 4, y + 3, x + 4, y + h - 4)
    # Arms: gold pauldrons over the shoulders, a channel down the outside, gold cuffs
    for f, (x, y, w, h) in ARM.items():
        if f in ('top',):
            sh.plate(x, y, w, h, 'gold'); sh.trim(x, y, w, h, 'tblr', 'gold')
            continue
        sh.plate(x, y, w, h)
        if f == 'bottom':
            continue
        sh.plate(x, y, w, 7, 'gold'); sh.trim(x, y, w, 7, 'b')
        for xx in range(x, x + w):
            sh.put(xx, y + 7, _shade('steel_dk', 0.8))
        sh.trim(x, y + h - 3, w, 3, 'tb')
        if f in ('right', 'front', 'back'):
            sh.line(x + w // 2, y + 9, x + w // 2, y + h - 5)


def _boots(sh):
    top = 54                                         # boots cover the bottom 5 model pixels of the leg
    x, y, w, h = LEG['bottom']
    sh.plate(x, y, w, h, 'steel_dk'); sh.line(x + 1, y + 4, x + w - 2, y + 4)
    for f in SIDES:
        x, y, w, h = LEG[f]
        sh.plate(x, top, w, y + h - top)
        sh.trim(x, top, w, 2, 'tb')                  # gold cuff
        sh.line(x, y + h - 1, x + w - 1, y + h - 1, glow=False)   # glowing sole
        if f == 'front':
            sh.plate(x + 1, y + h - 5, w - 2, 4, 'gold')          # toe cap
            sh.gem(x + 4, top + 4, 1)


def _leggings(sh):
    waist = 54                                       # the belt sits at the bottom of the torso box
    x, y, w, h = BODY['bottom']
    sh.plate(x, y, w, h)
    for f in SIDES:
        x, y, w, h = BODY[f]
        sh.plate(x, waist, w, y + h - waist)
        sh.trim(x, waist, w, 4, 'tb')
        for yy in range(waist + 1, waist + 3):
            for xx in range(x, x + w):
                sh.put(xx, yy, _shade('gold', 0.35))
        if f == 'front':
            sh.gem(x + 8, waist + 2, 2)              # buckle
    for f in SIDES:                                  # legs down to the ankle (boots cover the rest)
        x, y, w, h = LEG[f]
        sh.plate(x, y, w, h - 4)
        sh.trim(x, y, w, h - 4, 'tb')
        sh.plate(x + 1, y + 9, w - 2, 4, 'gold')     # knee plates
        sh.trim(x + 1, y + 9, w - 2, 4, 'tblr')
        if f in ('right', 'left', 'back'):
            sh.line(x + w // 2, y + 2, x + w // 2, y + 8)
            sh.line(x + w // 2, y + 14, x + w // 2, y + h - 6)
    x, y, w, h = LEG['top']
    sh.plate(x, y, w, h)


def armor_layer(layer):
    sh = _Sheet()
    if layer == 1:
        _helmet(sh)
        _chest(sh)
        _boots(sh)
    else:
        _leggings(sh)
    return sh.px
