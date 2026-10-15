#!/usr/bin/env python3
"""Textures for the mod's effect particles (client/render/FxParticles): soft smoke puffs, white and see-through at the
edges so the game can tint them. Registered as the particle type factoryascension:soft_smoke so they're in the
particle atlas. Run from the repo root (make_mod_assets.py runs it):  python3 tools/fx_art.py
"""
import json
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))
import make_mod_assets as A  # noqa: E402
import make_textures  # noqa: E402

N = 32


def puff(seed):
    rnd = random.Random(seed)
    blobs = [(rnd.uniform(10, 22), rnd.uniform(10, 22), rnd.uniform(5, 9)) for _ in range(5)]
    px = []
    for y in range(N):
        row = []
        for x in range(N):
            d = math.hypot(x - 15.5, y - 15.5) / 15.5
            a = max(0.0, 1 - d) ** 1.6
            # Lumpy: a few soft blobs and some grain, so puffs don't look like discs
            lump = sum(math.exp(-((x - bx) ** 2 + (y - by) ** 2) / (2 * r * r)) for bx, by, r in blobs) / 2.2
            a *= min(1.0, 0.45 + lump) * (0.85 + 0.15 * rnd.random())
            v = int(215 + 40 * min(1.0, lump))
            row.append((v, v, v, int(255 * min(1.0, a))))
        px.append(row)
    return px


def main():
    out = os.path.join(A.ASSETS, "textures", "particle")
    os.makedirs(out, exist_ok=True)
    names = []
    for i in range(4):
        make_textures.png(os.path.join(out, "soft_smoke_%d.png" % i), puff(100 + i))
        names.append("factoryascension:soft_smoke_%d" % i)
    A.write_json(os.path.join(A.ASSETS, "particles", "soft_smoke.json"), {"textures": names})
    print("wrote %d smoke puffs" % len(names))


if __name__ == "__main__":
    main()
