"""Ascendite tools and armor for Factory Ascension, drawn natively at 32x32 (icons) and 128x64 (worn armor).

The look: dark gunmetal steel, glowing teal energy channels, gold trim and collars, violet gems, and
leather-wrapped grips with gold bands. Each piece is a stack of layers (shapes from tome_art + a material);
every material shades itself, lit from the top-left like the rest of the pack's textures.
"""
import math

from tome_art import add, cut, arc, circ, ell, poly, raster, rect, seg

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
}


def _shade(mat, t):
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


ICONS = {'sword': sword, 'pickaxe': pickaxe, 'axe': axe, 'shovel': shovel, 'hoe': hoe,
         'helmet': helmet, 'chestplate': chestplate, 'leggings': leggings, 'boots': boots}


# ---- Worn armor (128x64; UV layout of the vanilla 64x32 armor sheet, doubled) ----

def _face(px, x0, y0, w, h, front=False, gem=False, visor=False, rows=None):
    """Paint one box face (64x32 units): steel plate, gold trim on its edges, teal channel down the middle of fronts."""
    X0, Y0, W, H = x0 * 2, y0 * 2, w * 2, h * 2
    for y in range(Y0, Y0 + H):
        if rows and not rows(y // 2):
            continue
        for x in range(X0, X0 + W):
            t = 0.3 + 0.4 * ((x - X0) + (y - Y0)) / float(W + H)
            c = _shade('steel', t)
            if x in (X0, X0 + W - 1) or y in (Y0, Y0 + H - 1):
                c = _shade('gold', 0.25 + 0.5 * (y - Y0) / H)
            px[y][x] = c + (255,)
    if front and W >= 8:
        cx = X0 + W // 2
        for y in range(Y0 + 2, Y0 + H - 2):
            if rows and not rows(y // 2):
                continue
            for x in (cx - 1, cx):
                px[y][x] = _shade('teal', 0.35) + (255,)
    if visor:
        vy = Y0 + H // 2 - 1
        for y in (vy, vy + 1):
            for x in range(X0 + 3, X0 + W - 3):
                px[y][x] = _shade('teal', 0.2) + (255,)
    if gem:
        cx, cy = X0 + W // 2, Y0 + H // 3
        for dx in (-2, -1, 0, 1):
            for dy in (-2, -1, 0, 1):
                if abs(dx + 0.5) + abs(dy + 0.5) <= 2.2:
                    px[cy + dy][cx + dx] = _shade('gem', 0.3 + 0.1 * (dx + dy)) + (255,)
        px[cy - 1][cx - 1] = (255, 255, 255, 255)


def _box(px, ox, oy, w, h, d, front_kw=None, rows=None, faces=('top', 'bottom', 'right', 'front', 'left', 'back')):
    """Vanilla box UV: top/bottom across the top strip, then right, front, left, back below."""
    spec = {
        'top': (ox + d, oy, w, d), 'bottom': (ox + d + w, oy, w, d),
        'right': (ox, oy + d, d, h), 'front': (ox + d, oy + d, w, h), 'left': (ox + d + w, oy + d, d, h), 'back': (ox + d + w + d, oy + d, w, h),
    }
    for f in faces:
        x0, y0, fw, fh = spec[f]
        kw = (front_kw or {}) if f == 'front' else {}
        _face(px, x0, y0, fw, fh, front=(f in ('front', 'back')), rows=rows if f not in ('top', 'bottom') else None, **kw)


def armor_layer(layer):
    px = [[(0, 0, 0, 0)] * 128 for _ in range(64)]
    if layer == 1:
        _box(px, 0, 0, 8, 8, 8, {'visor': True, 'gem': False})          # helmet
        _box(px, 16, 16, 8, 12, 4, {'gem': True})                      # chestplate body
        _box(px, 40, 16, 4, 12, 4)                                     # chestplate arms
        _box(px, 0, 16, 4, 12, 4, rows=lambda y: y >= 27, faces=('bottom', 'right', 'front', 'left', 'back'))   # boots: lower legs
    else:
        _box(px, 16, 16, 8, 12, 4, rows=lambda y: y <= 24, faces=('top', 'right', 'front', 'left', 'back'))     # leggings waist
        _box(px, 0, 16, 4, 12, 4, rows=lambda y: y <= 30)                                                       # leggings legs
    return px
