#!/usr/bin/env python3
"""Renders block and item model JSONs (cuboid elements) to a PNG, to check 3D models without starting the game.

  python3 tools/preview_models.py out.png kubejs:item/enchantment_pack factoryascension:block/salvager ...

Each model is drawn the way the inventory shows it (its gui display rotation, or the block view), lit from the top
like the game, textures sampled per texel, see-through pixels blended. A row of models goes into one image.
"""
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import make_textures  # noqa: E402
from pngread import read as read_png  # noqa: E402

ROOT = os.path.join(os.path.dirname(__file__), "..")
ASSETS = {"kubejs": os.path.join(ROOT, "kubejs", "assets", "kubejs"),
          "factoryascension": os.path.join(ROOT, "mod", "src", "main", "resources", "assets", "factoryascension")}
CELL = int(os.environ.get("PREVIEW_CELL", "192"))
# PREVIEW_VIEW="rx,ry,rz" looks from that angle instead of the gui one; PREVIEW_FIT=1 centers and fills the cell
VIEW = os.environ.get("PREVIEW_VIEW")
FIT = os.environ.get("PREVIEW_FIT") == "1"
SHADE = {"up": 1.0, "down": 0.5, "north": 0.8, "south": 0.8, "east": 0.6, "west": 0.6}


def res(ref, kind, ext):
    ns, path = ref.split(":", 1) if ":" in ref else ("minecraft", ref)
    return os.path.join(ASSETS.get(ns, ""), kind, path + ext)


def load_model(ref):
    m = json.load(open(res(ref, "models", ".json")))
    parent = m.get("parent")
    if parent and parent.split(":")[0] in ASSETS:
        base = load_model(parent)
        tex = dict(base.get("textures", {}))
        tex.update(m.get("textures", {}))
        merged = dict(base)
        merged.update(m)
        merged["textures"] = tex
        if "elements" not in m:
            merged["elements"] = base.get("elements", [])
        return merged
    return m


def resolve(textures, key):
    seen = 0
    while key.startswith("#") and seen < 10:
        key = textures.get(key[1:], key)
        seen += 1
    return key


_tex_cache = {}


def texture(ref):
    if ref not in _tex_cache:
        path = res(ref, "textures", ".png")
        _tex_cache[ref] = read_png(path) if os.path.exists(path) else None
    return _tex_cache[ref]


def rot_point(p, axis, ang, origin):
    a = math.radians(ang)
    x, y, z = (p[i] - origin[i] for i in range(3))
    c, s = math.cos(a), math.sin(a)
    if axis == "x":
        y, z = y * c - z * s, y * s + z * c
    elif axis == "y":
        x, z = x * c + z * s, -x * s + z * c
    else:
        x, y = x * c - y * s, x * s + y * c
    return (x + origin[0], y + origin[1], z + origin[2])


FACE_CORNERS = {  # corners in uv order: (u0,v0) (u1,v0) (u1,v1) (u0,v1)
    "north": lambda f, t: [(t[0], t[1], f[2]), (f[0], t[1], f[2]), (f[0], f[1], f[2]), (t[0], f[1], f[2])],
    "south": lambda f, t: [(f[0], t[1], t[2]), (t[0], t[1], t[2]), (t[0], f[1], t[2]), (f[0], f[1], t[2])],
    "east": lambda f, t: [(t[0], t[1], t[2]), (t[0], t[1], f[2]), (t[0], f[1], f[2]), (t[0], f[1], t[2])],
    "west": lambda f, t: [(f[0], t[1], f[2]), (f[0], t[1], t[2]), (f[0], f[1], t[2]), (f[0], f[1], f[2])],
    "up": lambda f, t: [(f[0], t[1], f[2]), (t[0], t[1], f[2]), (t[0], t[1], t[2]), (f[0], t[1], t[2])],
    "down": lambda f, t: [(f[0], f[1], t[2]), (t[0], f[1], t[2]), (t[0], f[1], f[2]), (f[0], f[1], f[2])],
}


def view(p, rx, ry, rz=0, c=(8, 8, 8)):
    """Turn a model point (0-16 space, centered) the way the gui display does: y rotation, then x, then z."""
    x, y, z = p[0] - c[0], p[1] - c[1], p[2] - c[2]
    a = math.radians(ry)
    x, z = x * math.cos(a) + z * math.sin(a), -x * math.sin(a) + z * math.cos(a)
    b = math.radians(rx)
    y, z = y * math.cos(b) - z * math.sin(b), y * math.sin(b) + z * math.cos(b)
    g = math.radians(rz)
    x, y = x * math.cos(g) - y * math.sin(g), x * math.sin(g) + y * math.cos(g)
    return x, y, z


def render(ref, img, ox):
    m = load_model(ref)
    gui = m.get("display", {}).get("gui")
    rx, ry, rz = (gui["rotation"][0], gui["rotation"][1], gui["rotation"][2]) if gui else (30, 225, 0)
    if VIEW:
        rx, ry, rz = (float(v) for v in VIEW.split(","))
    scale = (gui["scale"][0] if gui else 0.625) * CELL / 16.0 * 0.95
    center = (8, 8, 8)
    if FIT:
        pts = [p for el in m.get("elements", []) for p in (el["from"], el["to"])]
        lo = [min(p[k] for p in pts) for k in range(3)]
        hi = [max(p[k] for p in pts) for k in range(3)]
        center = tuple((lo[k] + hi[k]) / 2 for k in range(3))
        scale = CELL * 0.9 / max(hi[k] - lo[k] for k in range(3))
    quads = []
    for el in m.get("elements", []):
        f, t = el["from"], el["to"]
        r = el.get("rotation")
        for fname, fd in el.get("faces", {}).items():
            corners = FACE_CORNERS[fname](f, t)
            if r:
                corners = [rot_point(c, r["axis"], r["angle"], r["origin"]) for c in corners]
            tex = texture(resolve(m.get("textures", {}), fd["texture"]))
            if tex is None:
                continue
            uv = fd.get("uv", [0, 0, 16, 16])
            quads.append(([view(c, rx, ry, rz, center) for c in corners], tex, uv, SHADE[fname]))
    # Split every face into texel cells and paint them back to front
    cells = []
    for corners, tex, uv, shade in quads:
        th, tw = len(tex), len(tex[0])
        u0, v0, u1, v1 = uv
        nu = max(1, int(abs(u1 - u0) / 16.0 * tw))
        nv = max(1, int(abs(v1 - v0) / 16.0 * th))
        a, b, c, d = corners
        for j in range(nv):
            for i in range(nu):
                def at(s, q):
                    return tuple(a[k] + (b[k] - a[k]) * s + (d[k] - a[k]) * q + (a[k] - b[k] + c[k] - d[k]) * s * q for k in range(3))
                p = [at(i / nu, j / nv), at((i + 1) / nu, j / nv), at((i + 1) / nu, (j + 1) / nv), at(i / nu, (j + 1) / nv)]
                tu = u0 + (u1 - u0) * (i + 0.5) / nu
                tv = v0 + (v1 - v0) * (j + 0.5) / nv
                px = tex[min(th - 1, max(0, int(tv / 16.0 * th)))][min(tw - 1, max(0, int(tu / 16.0 * tw)))]
                if px[3] == 0:
                    continue
                depth = sum(q[2] for q in p) / 4
                cells.append((depth, p, px, shade))
    cells.sort(key=lambda c: c[0])
    for depth, p, px, shade in cells:
        pts = [(ox + CELL / 2 + q[0] * scale, CELL / 2 - q[1] * scale) for q in p]
        fill(img, pts, tuple(int(px[k] * shade) for k in range(3)), px[3] / 255.0)


def fill(img, pts, color, alpha):
    xs, ys = [p[0] for p in pts], [p[1] for p in pts]
    for y in range(max(0, int(min(ys))), min(len(img), int(max(ys)) + 2)):
        for x in range(max(0, int(min(xs))), min(len(img[0]), int(max(xs)) + 2)):
            if inside(x + 0.5, y + 0.5, pts):
                old = img[y][x]
                img[y][x] = tuple(int(old[k] * (1 - alpha) + color[k] * alpha) for k in range(3)) + (255,)


def inside(x, y, pts):
    sign = 0
    for i in range(4):
        x1, y1 = pts[i]
        x2, y2 = pts[(i + 1) % 4]
        cross = (x2 - x1) * (y - y1) - (y2 - y1) * (x - x1)
        if abs(cross) < 1e-9:
            continue
        s = 1 if cross > 0 else -1
        if sign == 0:
            sign = s
        elif s != sign:
            return False
    return True


def main():
    out, refs = sys.argv[1], sys.argv[2:]
    img = [[(58, 60, 72, 255)] * (CELL * len(refs)) for _ in range(CELL)]
    for i, ref in enumerate(refs):
        render(ref, img, i * CELL)
    make_textures.png(out, img)


if __name__ == "__main__":
    main()
