#!/usr/bin/env python3
"""UI art for Factory Ascension's screens (stdlib only): the emblem on the item-browser button.

The emblem: a dark hexagon with an amber rim, ringed by gear teeth (the factory), with a rising double chevron inside
(the ascension). Drawn as shapes, 8x supersampled, so the edges are smooth.
Run from the repo root:  python3 tools/ui_art.py
"""
import math
import os
import struct
import zlib

OUT = os.path.join(os.path.dirname(__file__), "..", "mod", "src", "main", "resources", "assets", "factoryascension", "textures", "gui")
AMBER, AMBER_DARK, FACE, FACE_DARK, EDGE = (201, 165, 90), (122, 100, 54), (30, 30, 28), (14, 14, 13), (58, 58, 56)


def write_png(path, px):
    raw = b"".join(b"\x00" + bytes(c for p in row for c in p) for row in px)

    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n")
        f.write(chunk(b"IHDR", struct.pack(">IIBBBBB", len(px[0]), len(px), 8, 6, 0, 0, 0)))
        f.write(chunk(b"IDAT", zlib.compress(raw, 9)))
        f.write(chunk(b"IEND", b""))


def hexagon_dist(x, y, r):
    """Signed distance-ish to a flat-topped hexagon of 'radius' r (negative inside)."""
    x, y = abs(x), abs(y)
    return max(x * 0.866 + y * 0.5, y) - r * 0.866


def chevron(x, y, cy, w, t):
    """Inside a chevron pointing up, tip at (0, cy), arms w wide each side, t thick?"""
    d = y - (cy + abs(x) * 0.9)
    return abs(x) <= w and 0 <= d <= t


def shade(x, y):
    """Colour of the emblem at (x, y) in -1..1, or None outside."""
    r = math.hypot(x, y)
    ang = math.atan2(y, x)
    teeth = 0.86 + 0.08 * (1 if math.cos(ang * 8) > 0.2 else 0)
    if r > teeth:
        return None
    if r > 0.84:
        return EDGE if r < teeth - 0.02 else AMBER_DARK
    hd = hexagon_dist(x, y, 0.80)
    if hd > 0:
        return FACE_DARK
    if hd > -0.07:
        return AMBER
    # Face: lit from the top
    t = (y + 1) / 2
    face = tuple(int(FACE[i] * (1 - t * 0.5) + FACE_DARK[i] * t * 0.5) for i in range(3))
    if chevron(x, y, -0.42, 0.42, 0.16):
        return AMBER
    if chevron(x, y, -0.08, 0.42, 0.16):
        return AMBER_DARK
    if abs(x) < 0.07 and 0.22 < y < 0.52:
        return AMBER_DARK
    return face


def emblem(size):
    ss = 8
    px = []
    for py in range(size):
        row = []
        for qx in range(size):
            acc = [0, 0, 0, 0]
            for sy in range(ss):
                for sx in range(ss):
                    x = ((qx + (sx + 0.5) / ss) / size) * 2 - 1
                    y = ((py + (sy + 0.5) / ss) / size) * 2 - 1
                    c = shade(x, y)
                    if c is not None:
                        acc[0] += c[0]; acc[1] += c[1]; acc[2] += c[2]; acc[3] += 1
            n = acc[3]
            row.append((acc[0] // n, acc[1] // n, acc[2] // n, 255 * n // (ss * ss)) if n else (0, 0, 0, 0))
        px.append(row)
    return px


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    write_png(os.path.join(OUT, "fa_emblem.png"), emblem(32))
    print("wrote", os.path.join(OUT, "fa_emblem.png"))
