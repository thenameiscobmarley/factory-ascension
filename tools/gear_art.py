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
    'iron': ((240, 240, 244), (172, 174, 182), (88, 90, 100)),
    'wood': ((176, 128, 80), (122, 86, 50), (70, 48, 28)),
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


HAMMERS = {'rock_hammer': rock_hammer, 'ascendite_rock_hammer': ascendite_rock_hammer}

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
        """Gradient plate with a lit top-left edge and a shadowed bottom-right edge."""
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                t = 0.3 + 0.35 * ((x - x0) / max(1, w) + (y - y0) / max(1, h)) / 2
                if x == x0 or y == y0:
                    t -= 0.15
                if x == x0 + w - 1 or y == y0 + h - 1:
                    t += 0.15
                self.put(x, y, _shade(mat, t))

    def trim(self, x0, y0, w, h, sides='tblr', mat='gold'):
        for x in range(x0, x0 + w):
            if 't' in sides:
                self.put(x, y0, _shade(mat, 0.25))
            if 'b' in sides:
                self.put(x, y0 + h - 1, _shade(mat, 0.6))
        for y in range(y0, y0 + h):
            if 'l' in sides:
                self.put(x0, y, _shade(mat, 0.35))
            if 'r' in sides:
                self.put(x0 + w - 1, y, _shade(mat, 0.55))

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
        for y in range(cy - r, cy + r):
            for x in range(cx - r, cx + r):
                if abs(x - cx + 0.5) + abs(y - cy + 0.5) <= r + 0.2:
                    self.put(x, y, _shade('gem', 0.25 + 0.15 * ((x - cx) + (y - cy))))
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
