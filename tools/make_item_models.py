#!/usr/bin/env python3
"""3D item models for Factory Ascension: every pack item pops out of its sprite in layers.

Vanilla flat items are a 1-pixel-thick cutout. Here each texture becomes boxes of three depths:
  - outline pixels: thin, like vanilla
  - the body: a little thicker
  - raised details (pixels brighter than their surroundings: gems, emblems, gold trim, glowing channels): thickest
Pixels of the same depth are merged into rectangles (greedy meshing) so models stay light, and side faces that are
hidden by a deeper neighbor are left out. In inventory slots items still look flat; in hand, on the ground and in
item frames they have real depth.

Run from the repo root after make_textures.py:  python3 tools/make_item_models.py
"""
import json
import os
import struct
import zlib

ROOT = os.path.join(os.path.dirname(__file__), "..")
TEX = os.path.join(ROOT, "kubejs", "assets", "kubejs", "textures", "item")
OUT = os.path.join(ROOT, "kubejs", "assets", "kubejs", "models", "item")

# Half-thickness in model units (16 units = one block; vanilla items are 1 unit thick = 0.5 each side)
EDGE, BODY, RAISED = 0.75, 0.75, 1.4
MIN_FEATURE = 4
# Items held like tools (angled in hand)
HANDHELD = {"ascendite_sword", "ascendite_pickaxe", "ascendite_axe", "ascendite_shovel", "ascendite_hoe",
            "rock_hammer", "ascendite_rock_hammer"}

GENERATED = {
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
    "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "fixed": {"rotation": [0, 180, 0], "scale": [1, 1, 1]},
}
HANDHELD_DISPLAY = dict(GENERATED, **{
    "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
    "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
})


def read_png(path):
    d = open(path, "rb").read()
    i, idat = 8, b""
    while i < len(d):
        ln = struct.unpack(">I", d[i:i + 4])[0]
        tag, chunk = d[i + 4:i + 8], d[i + 8:i + 8 + ln]
        if tag == b"IHDR":
            w, h = struct.unpack(">II", chunk[:8])
        elif tag == b"IDAT":
            idat += chunk
        i += 12 + ln
    raw = zlib.decompress(idat)
    st = w * 4 + 1
    return [[tuple(raw[y * st + 1 + x * 4:y * st + 5 + x * 4]) for x in range(w)] for y in range(h)]


def lum(c):
    return (c[0] * 299 + c[1] * 587 + c[2] * 114) / 255000.0


def depth_map(px):
    """Half-thickness per pixel (0 = transparent)."""
    n = len(px)
    on = lambda x, y: 0 <= x < n and 0 <= y < n and px[y][x][3] > 0
    depth = [[0.0] * n for _ in range(n)]
    for y in range(n):
        for x in range(n):
            if not on(x, y):
                continue
            if not all(on(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                depth[y][x] = EDGE
                continue
            # Raised when clearly brighter or more colorful than its neighborhood (gems, emblems, trim, glow)
            near = [px[y + dy][x + dx] for dy in range(-2, 3) for dx in range(-2, 3) if on(x + dx, y + dy)]
            avg = sum(lum(c) for c in near) / len(near)
            sat = lambda c: (max(c[:3]) - min(c[:3])) / 255.0
            avg_sat = sum(sat(c) for c in near) / len(near)
            raised = lum(px[y][x]) - avg > 0.1 or sat(px[y][x]) - avg_sat > 0.2
            depth[y][x] = RAISED if raised else BODY
    # Small raised specks are texture grain, not features: islands under MIN_FEATURE pixels drop back to the body
    seen = [[False] * n for _ in range(n)]
    for y in range(n):
        for x in range(n):
            if depth[y][x] != RAISED or seen[y][x]:
                continue
            island, todo = [], [(x, y)]
            seen[y][x] = True
            while todo:
                cx, cy = todo.pop()
                island.append((cx, cy))
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = cx + dx, cy + dy
                    if 0 <= nx < n and 0 <= ny < n and not seen[ny][nx] and depth[ny][nx] == RAISED:
                        seen[ny][nx] = True
                        todo.append((nx, ny))
            if len(island) < MIN_FEATURE:
                for cx, cy in island:
                    depth[cy][cx] = BODY
    return depth


def rects(depth):
    """Greedy meshing: cover equal-depth pixels with as few rectangles as possible. [(x0, y0, x1, y1, d)] (x1, y1 exclusive)."""
    n = len(depth)
    used = [[False] * n for _ in range(n)]
    out = []
    for y in range(n):
        for x in range(n):
            d = depth[y][x]
            if not d or used[y][x]:
                continue
            x1 = x
            while x1 < n and depth[y][x1] == d and not used[y][x1]:
                x1 += 1
            y1 = y + 1
            while y1 < n and all(depth[y1][k] == d and not used[y1][k] for k in range(x, x1)):
                y1 += 1
            for yy in range(y, y1):
                for xx in range(x, x1):
                    used[yy][xx] = True
            out.append((x, y, x1, y1, d))
    return out


def model(name, px):
    n = len(px)
    s = 16.0 / n  # model units (and UV units) per texture pixel
    depth = depth_map(px)
    dget = lambda x, y: depth[y][x] if 0 <= x < n and 0 <= y < n else 0
    tex = "#t"
    elements = []
    for x0, y0, x1, y1, d in rects(depth):
        faces = {
            "south": {"uv": [x0 * s, y0 * s, x1 * s, y1 * s], "texture": tex},
            "north": {"uv": [x1 * s, y0 * s, x0 * s, y1 * s], "texture": tex},
        }
        # Side faces only where some neighbor along that edge is shallower (otherwise it's hidden)
        if any(dget(x, y0 - 1) < d for x in range(x0, x1)):
            faces["up"] = {"uv": [x0 * s, y0 * s, x1 * s, (y0 + 1) * s], "texture": tex}
        if any(dget(x, y1) < d for x in range(x0, x1)):
            faces["down"] = {"uv": [x0 * s, (y1 - 1) * s, x1 * s, y1 * s], "texture": tex}
        if any(dget(x0 - 1, y) < d for y in range(y0, y1)):
            faces["west"] = {"uv": [x0 * s, y0 * s, (x0 + 1) * s, y1 * s], "texture": tex}
        if any(dget(x1, y) < d for y in range(y0, y1)):
            faces["east"] = {"uv": [(x1 - 1) * s, y0 * s, x1 * s, y1 * s], "texture": tex}
        elements.append({
            "from": [round(x0 * s, 4), round(16 - y1 * s, 4), round(8 - d, 4)],
            "to": [round(x1 * s, 4), round(16 - y0 * s, 4), round(8 + d, 4)],
            "faces": faces,
        })
    return {
        "textures": {"t": "kubejs:item/" + name, "particle": "kubejs:item/" + name},
        "gui_light": "front",
        "elements": elements,
        "display": HANDHELD_DISPLAY if name in HANDHELD else GENERATED,
    }


def main():
    os.makedirs(OUT, exist_ok=True)
    total = 0
    for fn in sorted(os.listdir(TEX)):
        if not fn.endswith(".png"):
            continue
        name = fn[:-4]
        m = model(name, read_png(os.path.join(TEX, fn)))
        total += len(m["elements"])
        with open(os.path.join(OUT, name + ".json"), "w") as f:
            json.dump(m, f, separators=(",", ":"))
    count = len([f for f in os.listdir(OUT) if f.endswith(".json")])
    print("wrote %d 3D item models (%d boxes, %.0f per item)" % (count, total, total / max(1, count)))


if __name__ == "__main__":
    main()
