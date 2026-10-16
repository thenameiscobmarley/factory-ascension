"""Smooth, high-detail gun meshes (drawn by client/gun/GunMesh.java instead of Minecraft's block-style cuboids).

A MeshModel is a core.Model that, besides the cuboid model (still written: it's the inventory icon and what other
players' guns look like from afar), builds a real mesh of the same parts:
  - every box gets rounded edges (a quarter circle in 2-4 steps, so edges catch the light like machined metal),
  - cylinders are truly round (24-64 sides by size), with chamfered ends,
  - any rotation is allowed (cuboids only take 0 / 22.5 / 45),
and it has shapes the cuboids can't do: lathe (a turned profile: barrels, suppressors, scopes, muzzle devices), sweep
(a rounded section along a curve: stocks, grips, curved magazines, handguards), pipe (a round bar along a 3D path:
trigger guards, wire stocks, slings, levers) and plate (a flat outline with rounded edges: triggers, levers, plates).

Coordinates are model units, like core.Model (16 = one block). Materials are core's texture names; each has a base
colour and a surface class (metal, polymer, wood, ...): the mesh's vertex colours carry the colour, a small tiling
detail texture per class carries the grain, brushing, stippling or checkering, and edges get a little wear.

File (.fam, gzip): "FAM2", materials (name, class, rgb, flags), vertices (int16 x y z / 500, int8 normal, material,
wear), quads (4 vertex indices, u16 or u32).
"""
import gzip
import math
import struct

from core import Model, ANGLES

# ---- Materials: core texture name -> (class, rgb, flags) ----
CLASSES = ["metal", "polymer", "wood", "rubber", "plain", "glass", "glow", "knurl", "check", "fabric", "rail"]
TINT, SHINY, NOWEAR = 1, 2, 4

MATS = {
    "steel": ("metal", (66, 68, 72), 0), "steel_dark": ("metal", (40, 41, 44), 0), "steel_light": ("metal", (150, 152, 156), SHINY),
    "blued": ("metal", (38, 42, 58), SHINY), "chrome": ("metal", (196, 198, 204), SHINY), "alu": ("metal", (54, 56, 60), 0),
    "alu_tan": ("polymer", (168, 146, 108), 0), "alu_od": ("polymer", (86, 92, 66), 0), "brass": ("metal", (206, 160, 66), SHINY),
    "polymer": ("polymer", (46, 46, 47), 0), "polymer_tan": ("polymer", (156, 134, 98), 0), "polymer_od": ("polymer", (80, 86, 60), 0),
    "polymer_plum": ("polymer", (98, 52, 44), 0), "polymer_grey": ("polymer", (72, 74, 76), 0),
    "walnut": ("wood", (118, 68, 38), 0), "walnut_dark": ("wood", (80, 46, 28), 0), "birch": ("wood", (178, 116, 62), 0), "beech": ("wood", (152, 98, 54), 0),
    "grip_check": ("check", (46, 46, 47), NOWEAR), "grip_check_tan": ("check", (150, 128, 94), NOWEAR), "wood_check": ("check", (96, 54, 30), NOWEAR),
    "knurl": ("knurl", (62, 64, 68), 0), "rail": ("rail", (50, 52, 56), 0), "rail_tan": ("rail", (160, 138, 102), 0),
    "rubber": ("rubber", (32, 32, 32), NOWEAR), "bore": ("plain", (8, 8, 10), NOWEAR), "lens": ("glass", (60, 140, 150), NOWEAR),
    "red_dot": ("glow", (255, 50, 36), NOWEAR), "scope_body": ("metal", (40, 41, 44), 0), "spring": ("metal", (130, 132, 136), SHINY),
    "tint": ("metal", (214, 214, 216), TINT), "paint_red": ("polymer", (170, 30, 26), 0), "paint_white": ("glow", (235, 235, 225), NOWEAR),
    # Only in the meshes
    "copper": ("metal", (184, 110, 64), SHINY), "gold": ("metal", (222, 176, 64), SHINY), "nickel": ("metal", (176, 178, 176), SHINY),
    "stainless": ("metal", (168, 170, 172), SHINY), "parkerized": ("metal", (72, 74, 70), 0), "gunmetal": ("metal", (58, 62, 68), SHINY),
    "bakelite": ("polymer", (78, 40, 26), SHINY), "polymer_black": ("polymer", (30, 30, 31), 0), "polymer_fde": ("polymer", (168, 146, 110), 0),
    "polymer_green": ("polymer", (66, 76, 52), 0), "polymer_sand": ("polymer", (190, 168, 128), 0), "polymer_white": ("polymer", (200, 200, 196), 0),
    "cerakote_grey": ("polymer", (98, 102, 104), 0), "cerakote_red": ("polymer", (124, 40, 32), 0),
    "carbon": ("fabric", (36, 36, 38), SHINY), "canvas": ("fabric", (104, 98, 72), NOWEAR), "leather": ("fabric", (98, 58, 34), NOWEAR),
    "tape": ("fabric", (40, 40, 42), NOWEAR), "paracord": ("fabric", (70, 80, 58), NOWEAR),
    "walnut_light": ("wood", (150, 94, 52), 0), "teak": ("wood", (132, 82, 44), 0), "maple": ("wood", (196, 150, 98), 0), "rosewood": ("wood", (96, 44, 32), 0),
    # Vehicles: painted plastic takes the vehicle's colour (TINT), the rest is fixed
    "plastic_tint": ("polymer", (232, 232, 228), TINT), "plastic_black": ("polymer", (28, 28, 30), 0), "tyre": ("rubber", (24, 24, 24), NOWEAR),
    "seat": ("fabric", (30, 30, 32), NOWEAR), "chain": ("metal", (60, 58, 56), 0), "engine_alu": ("metal", (150, 152, 154), SHINY),
    "glow_green": ("glow", (90, 255, 120), NOWEAR), "glow_amber": ("glow", (255, 176, 60), NOWEAR), "glow_white": ("glow", (240, 240, 235), NOWEAR),
}


def mat_of(name):
    return MATS.get(name, MATS["steel"])


# ---- Small vector helpers ----
def add(a, b):
    return (a[0] + b[0], a[1] + b[1], a[2] + b[2])


def sub(a, b):
    return (a[0] - b[0], a[1] - b[1], a[2] - b[2])


def mul(a, s):
    return (a[0] * s, a[1] * s, a[2] * s)


def dot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def norm(a):
    l = math.sqrt(dot(a, a))
    return (a[0] / l, a[1] / l, a[2] / l) if l > 1e-9 else (0.0, 1.0, 0.0)


AXES = {"x": 0, "y": 1, "z": 2}


def rot_matrix(axis, deg):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    if axis == "x":
        return ((1, 0, 0), (0, c, -s), (0, s, c))
    if axis == "y":
        return ((c, 0, s), (0, 1, 0), (-s, 0, c))
    return ((c, -s, 0), (s, c, 0), (0, 0, 1))


def mat_apply(m, v):
    return (dot(m[0], v), dot(m[1], v), dot(m[2], v))


def mat_mul(a, b):
    return tuple(tuple(sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)) for i in range(3))


IDENT = ((1, 0, 0), (0, 1, 0), (0, 0, 1))


class Xf:
    """A rotation about a point (like a cuboid's rotation, but any angle and any number of them)."""

    def __init__(self, m=IDENT, origin=(0, 0, 0), then=None):
        self.m, self.o, self.then = m, origin, then

    def p(self, v):
        r = add(mat_apply(self.m, sub(v, self.o)), self.o)
        return self.then.p(r) if self.then else r

    def n(self, v):
        r = mat_apply(self.m, v)
        return self.then.n(r) if self.then else r


def make_xf(rot):
    """core-style rot=(axis, angle, origin), or a list of them applied in order, or None."""
    if not rot:
        return None
    if isinstance(rot[0], str):
        rot = [rot]
    xf = None
    for axis, ang, origin in reversed(rot):
        xf = Xf(rot_matrix(axis, ang), tuple(origin), xf)
    return xf


class Mesh:
    def __init__(self):
        self.mats = []          # names
        self.mat_index = {}
        self.verts = []         # (x, y, z, nx, ny, nz, mat, wear)
        self.vindex = {}
        self.quads = []

    def mat(self, name):
        if name not in self.mat_index:
            self.mat_index[name] = len(self.mats)
            self.mats.append(name)
        return self.mat_index[name]

    def v(self, p, n, mat, wear=0.0, xf=None):
        if xf:
            p, n = xf.p(p), xf.n(n)
        n = norm(n)
        key = (round(p[0] * 500), round(p[1] * 500), round(p[2] * 500), round(n[0] * 127), round(n[1] * 127), round(n[2] * 127), mat, round(wear * 255))
        i = self.vindex.get(key)
        if i is None:
            i = len(self.verts)
            self.vindex[key] = i
            self.verts.append(key)
        return i

    def pos(self, i):
        k = self.verts[i]
        return (k[0] / 500, k[1] / 500, k[2] / 500)

    def quad(self, a, b, c, d):
        """A quad (d == c for a triangle), wound so it faces where its vertex normals point."""
        if len({a, b, c, d}) < 3:
            return
        pa, pb, pc, pd = self.pos(a), self.pos(b), self.pos(c), self.pos(d)
        n1 = cross(sub(pb, pa), sub(pc, pa))
        n2 = cross(sub(pc, pa), sub(pd, pa))
        n = add(n1, n2)
        if dot(n, n) < 1e-12:
            return
        vn = (0, 0, 0)
        for i in (a, b, c, d):
            k = self.verts[i]
            vn = add(vn, (k[3], k[4], k[5]))
        if dot(n, vn) < 0:
            a, b, c, d = d, c, b, a
        self.quads.append((a, b, c, d))

    def tri(self, a, b, c):
        self.quad(a, b, c, c)

    def bounds(self):
        lo = [min(v[i] for v in self.verts) / 500 for i in range(3)]
        hi = [max(v[i] for v in self.verts) / 500 for i in range(3)]
        return lo, hi

    def write(self, path):
        out = bytearray(b"FAM2")
        out += struct.pack("<H", len(self.mats))
        for name in self.mats:
            cls, rgb, flags = mat_of(name)
            nb = name.encode()
            out += struct.pack("<B", len(nb)) + nb + struct.pack("<BBBBB", CLASSES.index(cls), rgb[0], rgb[1], rgb[2], flags)
        out += struct.pack("<I", len(self.verts))
        for k in self.verts:
            out += struct.pack("<hhhbbbBB", *k)
        big = len(self.verts) > 65535
        out += struct.pack("<IB", len(self.quads), 1 if big else 0)
        fmt = "<IIII" if big else "<HHHH"
        for q in self.quads:
            out += struct.pack(fmt, *q)
        with gzip.open(path, "wb", 9) as f:
            f.write(bytes(out))
        return len(out)


# ---- Rounded boxes ----
def band(lo, hi, r, steps):
    """Grid lines along one axis: the flat middle plus `steps` lines into each rounded edge (spaced so the quarter
    circle is cut into equal angles)."""
    if r <= 1e-6:
        return [lo, hi]
    pts = [lo]
    for k in range(steps - 1, 0, -1):
        pts.append(lo + r - r * math.tan(math.radians(45.0 * k / steps)))
    pts.append(lo + r)
    if hi - r > lo + r + 1e-6:
        pts.append(hi - r)
    for k in range(1, steps):
        pts.append(hi - r + r * math.tan(math.radians(45.0 * k / steps)))
    pts.append(hi)
    out = []
    for p in pts:
        if not out or p - out[-1] > 1e-6:
            out.append(p)
    return out


FACES = {  # face: (axis, side, a-axis, b-axis) with a x b = outward normal
    "east": (0, 1, 1, 2), "west": (0, -1, 2, 1), "up": (1, 1, 2, 0), "down": (1, -1, 0, 2), "south": (2, 1, 0, 1), "north": (2, -1, 1, 0),
}


def rbox(mesh, frm, to, mat, r, steps=2, faces=None, xf=None, wear=0.0):
    lo = [min(frm[i], to[i]) for i in range(3)]
    hi = [max(frm[i], to[i]) for i in range(3)]
    r = max(0.0, min(r, min(hi[i] - lo[i] for i in range(3)) * 0.49))
    if faces:
        r = 0.0          # a decal (one or a few faces): flat
    mi = mesh.mat(mat)
    grids = [band(lo[i], hi[i], r, steps) for i in range(3)]
    inner_lo = [lo[i] + r for i in range(3)]
    inner_hi = [hi[i] - r for i in range(3)]
    for fname, (ax, side, aa, bb) in FACES.items():
        if faces and fname not in faces:
            continue
        fixed = hi[ax] if side > 0 else lo[ax]
        ga, gb = grids[aa], grids[bb]
        idx = {}
        for i, a in enumerate(ga):
            for j, b in enumerate(gb):
                p = [0.0, 0.0, 0.0]
                p[ax], p[aa], p[bb] = fixed, a, b
                if r > 0:
                    c = [min(max(p[k], inner_lo[k]), inner_hi[k]) for k in range(3)]
                    d = sub(p, c)
                    n = norm(d)
                    pos = add(c, mul(n, r))
                    edge = 1.0 if (abs(d[aa]) > 1e-6 or abs(d[bb]) > 1e-6) else 0.0
                else:
                    n = [0.0, 0.0, 0.0]
                    n[ax] = float(side)
                    pos, edge = tuple(p), 0.0
                idx[i, j] = mesh.v(pos, n, mi, wear + edge * 0.6, xf)
        for i in range(len(ga) - 1):
            for j in range(len(gb) - 1):
                mesh.quad(idx[i, j], idx[i + 1, j], idx[i + 1, j + 1], idx[i, j + 1])


# ---- Lathe (a turned profile) ----
def lathe(mesh, axis, prof, c1, c2, mat, seg=None, xf=None, mats=None, phase=0.0, hard=40.0):
    """Revolve a profile [(a, r), ...] about `axis` through (c1, c2). Start and end at r = 0 for closed ends. `mats`
    optionally gives a material per profile segment. Corners sharper than `hard` degrees stay crisp."""
    ax = AXES[axis]
    o1, o2 = [i for i in range(3) if i != ax]
    rmax = max(r for _, r in prof)
    if seg is None:
        seg = max(16, min(64, int(round(rmax * 36 / 4)) * 4))
    # Each profile segment's normal in (a, r)
    segn = []
    for i in range(len(prof) - 1):
        da, dr = prof[i + 1][0] - prof[i][0], prof[i + 1][1] - prof[i][1]
        l = math.hypot(da, dr)
        segn.append((-dr / l, da / l) if l > 1e-9 else None)
    cosh = math.cos(math.radians(hard))
    ring_cache = {}

    def ring(pi, si):
        """The ring of vertices at profile point pi, with segment si's normal (or a smooth one)."""
        key = (pi, si)
        if key in ring_cache:
            return ring_cache[key]
        a, r = prof[pi]
        n_prev = segn[pi - 1] if pi > 0 else None
        n_next = segn[pi] if pi < len(segn) else None
        n = segn[si] if segn[si] else (n_prev or n_next)
        if n_prev and n_next and (n_prev[0] * n_next[0] + n_prev[1] * n_next[1]) > cosh:
            n = (n_prev[0] + n_next[0], n_prev[1] + n_next[1])
        m = mesh.mat(mats[si] if mats else mat)
        out = []
        for k in range(seg + 1):
            t = 2 * math.pi * (k % seg) / seg + phase
            ca, sa = math.cos(t), math.sin(t)
            p = [0.0, 0.0, 0.0]
            p[ax], p[o1], p[o2] = a, c1 + ca * r, c2 + sa * r
            nn = [0.0, 0.0, 0.0]
            nn[ax], nn[o1], nn[o2] = n[0], ca * n[1], sa * n[1]
            out.append(mesh.v(tuple(p), tuple(nn), m, 0.0, xf))
        ring_cache[key] = out
        return out

    for si in range(len(prof) - 1):
        if segn[si] is None:
            continue
        r0, r1 = ring(si, si), ring(si + 1, si)
        for k in range(seg):
            mesh.quad(r0[k], r0[k + 1], r1[k + 1], r1[k])


def cylinder(mesh, axis, a0, a1, c1, c2, r, mat, end_mat=None, ends=True, chamfer=None, seg=None, xf=None, hollow=0.0):
    """A round bar with slightly chamfered ends; `hollow` > 0 bores it out to that radius (a tube)."""
    if a1 < a0:
        a0, a1 = a1, a0
    L = a1 - a0
    b = chamfer if chamfer is not None else min(0.08, r * 0.18, L * 0.25)
    em = end_mat or mat
    if hollow > 0:
        prof = [(a0, hollow), (a0, r - b), (a0 + b, r), (a1 - b, r), (a1, r - b), (a1, hollow), (a0, hollow)]
        mats = [em, em, mat, em, em, "bore"]
        lathe(mesh, axis, prof, c1, c2, mat, seg=seg, xf=xf, mats=mats)
        return
    if not ends:
        lathe(mesh, axis, [(a0, r), (a1, r)], c1, c2, mat, seg=seg, xf=xf)
        return
    prof = [(a0, 0), (a0, r - b), (a0 + b, r), (a1 - b, r), (a1, r - b), (a1, 0)]
    lathe(mesh, axis, prof, c1, c2, mat, seg=seg, xf=xf, mats=[em, em, mat, em, em])


# ---- Sweeps ----
def rrect(hw, hh, rad, n=4):
    """A rounded rectangle (half width hw along u, half height hh along v) as [(u, v, nu, nv)], counter-clockwise."""
    rad = max(0.0, min(rad, hw * 0.98, hh * 0.98))
    pts = []
    for cx, cy, a0 in ((hw - rad, hh - rad, 0), (-hw + rad, hh - rad, 90), (-hw + rad, -hh + rad, 180), (hw - rad, -hh + rad, 270)):
        for k in range(n + 1):
            a = math.radians(a0 + 90.0 * k / n)
            ca, sa = math.cos(a), math.sin(a)
            pts.append((cx + ca * rad, cy + sa * rad, ca, sa))
    return pts


def surface(mesh, P, H, mat, xf=None):
    """Quads over a grid of points P[i][k] (k wraps around); normals worked out from the grid, facing like H[i][k]."""
    m = mesh.mat(mat) if isinstance(mat, str) else mat
    ni, nk = len(P), len(P[0])
    ids = []
    for i in range(ni):
        row = []
        for k in range(nk):
            t1 = sub(P[min(ni - 1, i + 1)][k], P[max(0, i - 1)][k])
            t2 = sub(P[i][(k + 1) % nk], P[i][(k - 1) % nk])
            n = cross(t1, t2)
            if dot(n, n) < 1e-14:
                n = H[i][k]
            elif dot(n, H[i][k]) < 0:
                n = mul(n, -1)
            row.append(mesh.v(P[i][k], n, m, 0.0, xf))
        ids.append(row)
    for i in range(ni - 1):
        for k in range(nk):
            mesh.quad(ids[i][k], ids[i][(k + 1) % nk], ids[i + 1][(k + 1) % nk], ids[i + 1][k])


def cap(mesh, pts, centre, n, mat, xf=None):
    m = mesh.mat(mat) if isinstance(mat, str) else mat
    ids = [mesh.v(p, n, m, 0.0, xf) for p in pts]
    c = mesh.v(centre, n, m, 0.0, xf)
    for k in range(len(ids)):
        mesh.tri(c, ids[k], ids[(k + 1) % len(ids)])


def sweep(mesh, path, hu, hv, mat, z=8.0, rad=0.35, n=4, caps=True, xf=None, end_mat=None):
    """A rounded-rectangle section swept along a path in the x-y plane. At each path point, `hv` is the half size
    across the path (in x-y) and `hu` the half size in z (both lists, or numbers). rad: corner radius, as a fraction of
    the smaller half size."""
    npts = len(path)
    hu = hu if isinstance(hu, (list, tuple)) else [hu] * npts
    hv = hv if isinstance(hv, (list, tuple)) else [hv] * npts
    P, H, T = [], [], []
    for i in range(npts):
        a = path[max(0, i - 1)]
        b = path[min(npts - 1, i + 1)]
        tx, ty = b[0] - a[0], b[1] - a[1]
        l = math.hypot(tx, ty) or 1
        tx, ty = tx / l, ty / l
        T.append((tx, ty))
        nx, ny = -ty, tx        # the path's left side in x-y
        sec = rrect(hu[i], hv[i], max(0.02, rad * min(hu[i], hv[i])), n)
        P.append([(path[i][0] + nx * v, path[i][1] + ny * v, z + u) for (u, v, _, _) in sec])
        H.append([(nx * nv, ny * nv, nu) for (_, _, nu, nv) in sec])
    surface(mesh, P, H, mat, xf)
    if caps:
        for i, sgn in ((0, -1), (npts - 1, 1)):
            cap(mesh, P[i], (path[i][0], path[i][1], z), (T[i][0] * sgn, T[i][1] * sgn, 0), end_mat or mat, xf)


def pipe(mesh, pts, r, mat, seg=12, caps=True, xf=None, closed=False):
    """A round bar along a 3D path (parallel-transport frames, so it never twists)."""
    m = mesh.mat(mat)
    n = len(pts)
    tans = []
    for i in range(n):
        if closed:
            a, b = pts[(i - 1) % n], pts[(i + 1) % n]
        else:
            a, b = pts[max(0, i - 1)], pts[min(n - 1, i + 1)]
        tans.append(norm(sub(b, a)))
    ref = (0, 1, 0) if abs(tans[0][1]) < 0.9 else (1, 0, 0)
    u = norm(cross(tans[0], ref))
    rings = []
    for i in range(n):
        if i > 0:
            u = norm(sub(u, mul(tans[i], dot(u, tans[i]))))
        v = cross(tans[i], u)
        ring = []
        for k in range(seg + 1):
            t = 2 * math.pi * (k % seg) / seg
            d = add(mul(u, math.cos(t)), mul(v, math.sin(t)))
            ring.append(mesh.v(add(pts[i], mul(d, r)), d, m, 0.0, xf))
        rings.append(ring)
    rng = range(n) if closed else range(n - 1)
    for i in rng:
        r0, r1 = rings[i], rings[(i + 1) % n]
        for k in range(seg):
            mesh.quad(r0[k], r0[k + 1], r1[k + 1], r1[k])
    if caps and not closed:
        for i, sgn in ((0, -1), (n - 1, 1)):
            nn = mul(tans[i], sgn)
            c = mesh.v(pts[i], nn, m, 0.0, xf)
            ids = [mesh.v(mesh.pos(rings[i][k]), nn, m, 0.0) for k in range(seg)]
            for k in range(seg):
                mesh.tri(c, ids[k], ids[(k + 1) % seg])


# ---- Plates (flat outlines with rounded edges) ----
def area2(poly):
    return sum(poly[i][0] * poly[(i + 1) % len(poly)][1] - poly[(i + 1) % len(poly)][0] * poly[i][1] for i in range(len(poly)))


def clean_poly(poly, eps=1e-4):
    """Drop repeated and straight-through points (they break ear clipping)."""
    pts = []
    for p in poly:
        if not pts or abs(p[0] - pts[-1][0]) > eps or abs(p[1] - pts[-1][1]) > eps:
            pts.append(p)
    if len(pts) > 1 and abs(pts[0][0] - pts[-1][0]) <= eps and abs(pts[0][1] - pts[-1][1]) <= eps:
        pts.pop()
    changed = True
    while changed and len(pts) > 3:
        changed = False
        for i in range(len(pts)):
            a, b, c = pts[i - 1], pts[i], pts[(i + 1) % len(pts)]
            cr = (b[0] - a[0]) * (c[1] - b[1]) - (b[1] - a[1]) * (c[0] - b[0])
            la = math.hypot(b[0] - a[0], b[1] - a[1]) * math.hypot(c[0] - b[0], c[1] - b[1])
            if la < 1e-12 or abs(cr) / la < 1e-4:
                pts.pop(i)
                changed = True
                break
    return pts


def triangulate(poly):
    """Ear clipping; poly counter-clockwise (and cleaned). Returns index triples."""
    idx = list(range(len(poly)))
    out = []

    def inside(p, a, b, c):
        d1 = (p[0] - b[0]) * (a[1] - b[1]) - (a[0] - b[0]) * (p[1] - b[1])
        d2 = (p[0] - c[0]) * (b[1] - c[1]) - (b[0] - c[0]) * (p[1] - c[1])
        d3 = (p[0] - a[0]) * (c[1] - a[1]) - (c[0] - a[0]) * (p[1] - a[1])
        e = 1e-9
        neg = d1 < -e or d2 < -e or d3 < -e
        pos = d1 > e or d2 > e or d3 > e
        return not (neg and pos)
    guard = 0
    while len(idx) > 3 and guard < 20000:
        guard += 1
        found = False
        for k in range(len(idx)):
            i0, i1, i2 = idx[k - 1], idx[k], idx[(k + 1) % len(idx)]
            a, b, c = poly[i0], poly[i1], poly[i2]
            if (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]) <= 1e-12:
                continue
            bad = False
            for j in idx:
                if j in (i0, i1, i2):
                    continue
                q = poly[j]
                if q == a or q == b or q == c:
                    continue
                if inside(q, a, b, c):
                    bad = True
                    break
            if bad:
                continue
            out.append((i0, i1, i2))
            idx.pop(k)
            found = True
            break
        if not found:
            # Not a simple polygon any more (an inset that crossed itself): close what's left as a fan
            for k in range(1, len(idx) - 1):
                out.append((idx[0], idx[k], idx[k + 1]))
            return out
    if len(idx) == 3:
        out.append(tuple(idx))
    return out


def outline_normals(poly):
    n = len(poly)
    out = []
    for i in range(n):
        a, b, c = poly[i - 1], poly[i], poly[(i + 1) % n]
        e1 = norm((b[0] - a[0], b[1] - a[1], 0))
        e2 = norm((c[0] - b[0], c[1] - b[1], 0))
        n1, n2 = (e1[1], -e1[0]), (e2[1], -e2[0])          # outward for a counter-clockwise outline
        out.append((n1, n2))
    return out


def plate(mesh, poly, z0, z1, mat, bevel=0.12, xf=None, hard=35.0, side_mat=None):
    """A flat shape: outline poly [(x, y), ...] in the x-y plane, from z0 to z1, its edges rounded by `bevel`."""
    poly = clean_poly(poly)
    if len(poly) < 3:
        return
    if area2(poly) < 0:
        poly = poly[::-1]
    m = mesh.mat(mat)
    sm = mesh.mat(side_mat or mat)
    t = z1 - z0
    b = max(0.0, min(bevel, t * 0.45))
    ns = outline_normals(poly)
    cosh = math.cos(math.radians(hard))
    # Per outline point: one or two side normals (crisp corners split)
    sides = []
    for (n1, n2) in ns:
        if n1[0] * n2[0] + n1[1] * n2[1] > cosh:
            s = norm((n1[0] + n2[0], n1[1] + n2[1], 0))
            sides.append([(s[0], s[1]), (s[0], s[1])])
        else:
            sides.append([n1, n2])
    # Inset outline for the caps (miter)
    inset = []
    for i, (n1, n2) in enumerate(ns):
        s = (n1[0] + n2[0], n1[1] + n2[1])
        d = 1 + n1[0] * n2[0] + n1[1] * n2[1]
        k = b / max(0.35, d) if d > 1e-6 else b
        inset.append((poly[i][0] - s[0] * k, poly[i][1] - s[1] * k))
    tris = triangulate(inset)
    for zc, nz in ((z1, 1), (z0, -1)):
        ids = [mesh.v((p[0], p[1], zc), (0, 0, nz), m, 0.0, xf) for p in inset]
        for (a, bb, c) in tris:
            mesh.tri(ids[a], ids[bb], ids[c])
    # Rounded sides: rings from the top cap edge, around the bevel, down the side, to the bottom cap edge
    steps = [(-1, 1.0)] if b <= 1e-6 else [(z1, 90), (z1 - b * (1 - math.cos(math.radians(45))), 45), (z1 - b, 0), (z0 + b, 0), (z0 + b * (1 - math.cos(math.radians(45))), -45), (z0, -90)]
    if b <= 1e-6:
        steps = [(z1, 0), (z0, 0)]
    n = len(poly)
    for i in range(n):
        j = (i + 1) % n
        ni, nj = sides[i][1], sides[j][0]
        col = []
        for (zz, ang) in steps:
            ca, sa = math.cos(math.radians(ang)), math.sin(math.radians(ang))
            out_i = b * ca if b > 0 else 0
            out_j = out_i
            pi = (inset[i][0] + (poly[i][0] - inset[i][0]) * (ca if b > 0 else 1), inset[i][1] + (poly[i][1] - inset[i][1]) * (ca if b > 0 else 1), zz)
            pj = (inset[j][0] + (poly[j][0] - inset[j][0]) * (ca if b > 0 else 1), inset[j][1] + (poly[j][1] - inset[j][1]) * (ca if b > 0 else 1), zz)
            col.append((mesh.v(pi, (ni[0] * ca, ni[1] * ca, sa), sm, 0.5 if abs(ang) == 45 else 0.0, xf),
                        mesh.v(pj, (nj[0] * ca, nj[1] * ca, sa), sm, 0.5 if abs(ang) == 45 else 0.0, xf)))
            del out_i, out_j
        for k in range(len(col) - 1):
            mesh.quad(col[k][0], col[k][1], col[k + 1][1], col[k + 1][0])


# ---- Curves ----
def spline(pts, per=6, closed=False):
    """Catmull-Rom through the points (2D or 3D), `per` steps between each pair."""
    n = len(pts)
    dim = len(pts[0])
    out = []
    rng = n if closed else n - 1
    for i in range(rng):
        p0 = pts[(i - 1) % n] if closed else pts[max(0, i - 1)]
        p1 = pts[i]
        p2 = pts[(i + 1) % n]
        p3 = pts[(i + 2) % n] if closed else pts[min(n - 1, i + 2)]
        for s in range(per):
            t = s / per
            t2, t3 = t * t, t * t * t
            out.append(tuple(0.5 * (2 * p1[d] + (-p0[d] + p2[d]) * t + (2 * p0[d] - 5 * p1[d] + 4 * p2[d] - p3[d]) * t2
                                    + (-p0[d] + 3 * p1[d] - 3 * p2[d] + p3[d]) * t3) for d in range(dim)))
    if not closed:
        out.append(tuple(pts[-1]))
    return out


def arc(cx, cy, r, a0, a1, n=12):
    return [(cx + r * math.cos(math.radians(a0 + (a1 - a0) * k / n)), cy + r * math.sin(math.radians(a0 + (a1 - a0) * k / n))) for k in range(n + 1)]


def round_corners(poly, r, n=4):
    """Round every corner of a polygon by radius r (or a list of radii, one per corner)."""
    out = []
    m = len(poly)
    for i in range(m):
        a, b, c = poly[i - 1], poly[i], poly[(i + 1) % m]
        rr = r[i] if isinstance(r, (list, tuple)) else r
        if rr <= 0:
            out.append(b)
            continue
        e1 = (a[0] - b[0], a[1] - b[1])
        e2 = (c[0] - b[0], c[1] - b[1])
        l1, l2 = math.hypot(*e1), math.hypot(*e2)
        if l1 < 1e-6 or l2 < 1e-6:
            out.append(b)
            continue
        e1, e2 = (e1[0] / l1, e1[1] / l1), (e2[0] / l2, e2[1] / l2)
        cosang = max(-1, min(1, e1[0] * e2[0] + e1[1] * e2[1]))
        ang = math.acos(cosang)
        if ang < 1e-3 or abs(ang - math.pi) < 1e-3:
            out.append(b)
            continue
        d = min(rr / math.tan(ang / 2), l1 * 0.45, l2 * 0.45)
        p1 = (b[0] + e1[0] * d, b[1] + e1[1] * d)
        p2 = (b[0] + e2[0] * d, b[1] + e2[1] * d)
        for k in range(n + 1):
            t = k / n
            # quadratic Bezier p1 -> b -> p2
            x = (1 - t) ** 2 * p1[0] + 2 * (1 - t) * t * b[0] + t * t * p2[0]
            y = (1 - t) ** 2 * p1[1] + 2 * (1 - t) * t * b[1] + t * t * p2[1]
            out.append((x, y))
    return out


# ---- The model: cuboids + mesh together ----
def snap(ang):
    return min(ANGLES, key=lambda a: abs(a - ang))


class MeshModel(Model):
    """core.Model's cuboids (the icon / far model), plus a smooth mesh of the same parts."""

    def __init__(self, name, seed=1):
        super().__init__(name, seed)
        self.mesh = Mesh()
        self._quiet = 0          # > 0: cuboid only (inside a primitive that builds its own mesh)
        self.extra_xf = None     # applied to every mesh part (for groups)

    # Overridden core primitives -------------------------------------------------------------------------
    def box(self, frm, to, tex, faces=None, rot=None, full=False, uvscale=1.0, shade=True, r=None, mesh_only=False):
        el = None
        if not mesh_only:
            jrot = None
            if rot:
                rr = rot if isinstance(rot[0], str) else rot[0]
                jrot = (rr[0], snap(rr[1]), rr[2])
            el = super().box(frm, to, tex, faces=faces, rot=jrot, full=full, uvscale=uvscale, shade=shade)
        if self._quiet:
            return el
        cls = mat_of(tex)[0]
        size = min(abs(to[i] - frm[i]) for i in range(3))
        if r is None:
            rmax = 0.16 if cls in ("metal", "rail", "knurl") else 0.32
            r = min(rmax, size * 0.3)
        steps = 3 if r > 0.2 else (2 if r > 0.07 else 1)
        if size < 0.05 or r < 0.02:
            r = 0
        rbox(self.mesh, frm, to, tex, r, steps=steps, faces=faces, xf=self._xf(rot))
        return el

    def _xf(self, rot):
        xf = make_xf(rot)
        if self.extra_xf:
            if xf:
                last = xf
                while last.then:
                    last = last.then
                last.then = self.extra_xf
            else:
                xf = self.extra_xf
        return xf

    def cyl(self, axis, a0, a1, c1, c2, r, tex, sides=8, end_tex=None, ends=True, rot=None, hollow=0.0, mesh_only=False):
        if not mesh_only:
            self._quiet += 1
            super().cyl(axis, a0, a1, c1, c2, r, tex, sides=sides, end_tex=end_tex, ends=ends)
            self._quiet -= 1
        if not self._quiet:
            cylinder(self.mesh, axis, a0, a1, c1, c2, r, tex, end_mat=end_tex, ends=ends, xf=self._xf(rot), hollow=hollow)

    def tube(self, x0, x1, cy, cz, r, tex, bore="bore", sides=8, wall=0.25):
        self._quiet += 1
        super().tube(x0, x1, cy, cz, r, tex, bore=bore, sides=sides, wall=wall)
        self._quiet -= 1
        if self._quiet:
            return
        br = max(0.12, r - wall)
        # A barrel with a recessed crown and a dark bore
        prof = [(x0, 0), (x0, r), (x1 - 0.06, r), (x1, r - 0.06), (x1, br + 0.04), (x1 - 0.05, br), (x1 - 0.6, br), (x1 - 0.6, 0)]
        lathe(self.mesh, "x", prof, cy, cz, tex, xf=self._xf(None), mats=[tex, tex, tex, tex, tex, bore, bore])

    def profile(self, pts, w, tex, step=0.8, cz=8.0, taper=None):
        self._quiet += 1
        super().profile(pts, w, tex, step=step, cz=cz, taper=taper)
        self._quiet -= 1
        if self._quiet:
            return
        # A smooth loft: the centre line through the outline's middles, half heights and widths from it
        xs = []
        x = pts[0][0]
        while x < pts[-1][0] - 1e-6:
            xs.append(x)
            x += min(step, 0.5)
        xs.append(pts[-1][0])
        path, hv, hu = [], [], []
        for x in xs:
            for i in range(len(pts) - 1):
                if pts[i][0] <= x <= pts[i + 1][0] + 1e-9:
                    a, b = pts[i], pts[i + 1]
                    k = (x - a[0]) / max(1e-6, b[0] - a[0])
                    lo, hi = a[1] + (b[1] - a[1]) * k, a[2] + (b[2] - a[2]) * k
                    break
            ww = w
            if taper:
                for i in range(len(taper) - 1):
                    if taper[i][0] <= x <= taper[i + 1][0] + 1e-9:
                        k = (x - taper[i][0]) / max(1e-6, taper[i + 1][0] - taper[i][0])
                        ww = taper[i][1] + (taper[i + 1][1] - taper[i][1]) * k
            path.append((x, (lo + hi) / 2))
            hv.append(max(0.05, (hi - lo) / 2))
            hu.append(ww / 2)
        # Keep the section upright (a straight path in x): sweep along x with sections in y-z
        self._loft_x(path, hu, hv, tex, cz)

    def _loft_x(self, path, hu, hv, tex, cz, rad=0.45):
        """Sections stay vertical (in y-z) along x, so a stock's top and bottom lines follow its outline exactly."""
        xf = self._xf(None)
        P, H = [], []
        for i, (x, yc) in enumerate(path):
            sec = rrect(hu[i], hv[i], max(0.02, rad * min(hu[i], hv[i])), 4)
            P.append([(x, yc + v, cz + u) for (u, v, _, _) in sec])
            H.append([(0, nv, nu) for (_, _, nu, nv) in sec])
        surface(self.mesh, P, H, tex, xf)
        for i, sgn in ((0, -1), (len(path) - 1, 1)):
            cap(self.mesh, P[i], (path[i][0], path[i][1], cz), (sgn, 0, 0), tex, xf)

    def mag(self, x, y_top, length, depth, z0, z1, tex, curve=0, base="steel_dark", ribs=0, rib_tex=None):
        self._quiet += 1
        super().mag(x, y_top, length, depth, z0, z1, tex, curve=curve, base=base, ribs=ribs, rib_tex=rib_tex)
        self._quiet -= 1
        if self._quiet:
            return
        # A smooth magazine: its centre line bends forward along an arc (curve 0 straight, 1, 2 more)
        bend = [0, 32, 58][min(2, curve)]
        n = 10
        path = []
        cx, cy = x + length / 2, y_top + 0.05
        for k in range(n + 1):
            t = k / n
            ang = math.radians(bend * t * t)
            path.append((cx, cy))
            step = depth / n
            cx += math.sin(ang) * step
            cy -= math.cos(ang) * step
        hw = (z1 - z0) / 2
        zc = (z0 + z1) / 2
        hv = [length / 2 * (1 + 0.06 * (k / n)) for k in range(n + 1)]
        sweep(self.mesh, path, hw, hv, tex, z=zc, rad=0.3, xf=self._xf(None))
        # Base plate, turned with the end of the curve
        ex, ey = path[-1]
        tx, ty = path[-1][0] - path[-2][0], path[-1][1] - path[-2][1]
        l = math.hypot(tx, ty) or 1
        tx, ty = tx / l, ty / l
        bp = [(ex - tx * 0.0, ey - ty * 0.0), (ex + tx * 0.5, ey + ty * 0.5)]
        sweep(self.mesh, bp, hw + 0.18, hv[-1] + 0.22, base, z=zc, rad=0.35, xf=self._xf(None))
        if ribs:
            for k in range(ribs):
                i = int((k + 1) * n * 0.6 / (ribs + 1)) + 1
                px, py = path[i]
                for zz, sgn in ((z1, 1), (z0, -1)):
                    self.box((px - hv[i] + 0.3, py - 0.12, zz if sgn > 0 else zz - 0.06), (px + hv[i] - 0.3, py + 0.12, zz + 0.06 if sgn > 0 else zz),
                             rib_tex or tex, faces=["south" if sgn > 0 else "north"], mesh_only=True)

    # Mesh-only shapes, with a cuboid stand-in for the icon -----------------------------------------------
    def lathe(self, axis, prof, c1, c2, tex, mats=None, seg=None, rot=None, icon=True):
        if icon:
            self._quiet += 1
            for i in range(len(prof) - 1):
                (a0, r0), (a1, r1) = prof[i], prof[i + 1]
                if a1 - a0 > 0.2 and max(r0, r1) > 0.05:
                    super().cyl(axis, a0, a1, c1, c2, max(0.05, (r0 + r1) / 2), mats[i] if mats else tex, sides=8, ends=True)
            self._quiet -= 1
        if not self._quiet:
            lathe(self.mesh, axis, prof, c1, c2, tex, seg=seg, xf=self._xf(rot), mats=mats)

    def sweep(self, path, hu, hv, tex, z=8.0, rad=0.35, caps=True, end_tex=None, icon=True):
        if icon:
            self._icon_path(path, hu, hv, tex, z)
        if not self._quiet:
            sweep(self.mesh, path, hu, hv, tex, z=z, rad=rad, caps=caps, xf=self._xf(None), end_mat=end_tex)

    def _icon_path(self, path, hu, hv, tex, z):
        n = len(path)
        hu = hu if isinstance(hu, (list, tuple)) else [hu] * n
        hv = hv if isinstance(hv, (list, tuple)) else [hv] * n
        self._quiet += 1
        step = max(1, n // 6)
        for i in range(0, n - 1, step):
            j = min(n - 1, i + step)
            a, b = path[i], path[j]
            mx, my = (a[0] + b[0]) / 2, (a[1] + b[1]) / 2
            L = math.hypot(b[0] - a[0], b[1] - a[1])
            ang = snap(math.degrees(math.atan2(b[1] - a[1], b[0] - a[0])))
            h = (hv[i] + hv[j]) / 2
            w = (hu[i] + hu[j]) / 2
            if L < 0.05:
                continue
            Model.box(self, (mx - L / 2 - 0.1, my - h, z - w), (mx + L / 2 + 0.1, my + h, z + w), tex, rot=("z", ang, (mx, my, z)) if ang else None)
        self._quiet -= 1

    def pipe(self, pts, r, tex, seg=12, caps=True, closed=False, icon=True):
        if icon:
            self._quiet += 1
            for i in range(len(pts) - 1):
                a, b = pts[i], pts[i + 1]
                if i % 2:
                    continue
                lo = [min(a[k], b[k]) - r for k in range(3)]
                hi = [max(a[k], b[k]) + r for k in range(3)]
                Model.box(self, lo, hi, tex)
            self._quiet -= 1
        if not self._quiet:
            pipe(self.mesh, pts, r, tex, seg=seg, caps=caps, xf=self._xf(None), closed=closed)

    def plate(self, poly, z0, z1, tex, bevel=0.12, rot=None, icon=True, side_tex=None, corner=0.18):
        if icon:
            self._quiet += 1
            xs = [p[0] for p in poly]
            x0, x1 = min(xs), max(xs)
            nstrip = max(1, min(8, int((x1 - x0) / 0.8)))
            for s in range(nstrip):
                xa, xb = x0 + (x1 - x0) * s / nstrip, x0 + (x1 - x0) * (s + 1) / nstrip
                xm = (xa + xb) / 2
                ys = []
                for i in range(len(poly)):
                    p, q = poly[i], poly[(i + 1) % len(poly)]
                    if (p[0] - xm) * (q[0] - xm) <= 0 and p[0] != q[0]:
                        t = (xm - p[0]) / (q[0] - p[0])
                        ys.append(p[1] + (q[1] - p[1]) * t)
                if len(ys) >= 2:
                    jr = None
                    if rot:
                        rr = rot if isinstance(rot[0], str) else rot[0]
                        jr = (rr[0], snap(rr[1]), rr[2]) if snap(rr[1]) else None
                    Model.box(self, (xa, min(ys), z0), (xb, max(ys), z1), tex, rot=jr)
            self._quiet -= 1
        if not self._quiet:
            # Corners at least as round as the edge bevel, or the inset outline would fold over itself
            c = max(corner, bevel * 1.15) if corner > 0 else 0
            outline = round_corners(poly, c, 4) if c > 0 and len(poly) > 3 else poly
            plate(self.mesh, outline, z0, z1, tex, bevel=bevel, xf=self._xf(rot), side_mat=side_tex)

    def check(self):
        super().check()
        if self.mesh.verts:
            lo, hi = self.mesh.bounds()
            assert min(lo) >= -64 and max(hi) <= 64, "%s: mesh outside the file's range %s %s" % (self.name, lo, hi)
