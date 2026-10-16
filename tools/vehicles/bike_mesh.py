#!/usr/bin/env python3
"""Smooth meshes for the dirt bike's parts (drawn by client/vehicle/DirtBikeRenderer with client/gun/GunMesh): every
part is its own mesh, so the bike looks like what's fitted to it. Model units (16 = one block); the bike faces +x, the
ground is y = 0, z = 0 is its middle. Each frame's anchor points (axles, steering head, engine mount, seat) are in
FRAMES; the Java side has the same numbers (vehicle/BikeParts.java), checked by tools/test.

  python3 tools/vehicles/bike_mesh.py      (make_mod_assets.py runs it)
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "guns"))
import mesh as M  # noqa: E402

OUT = os.path.join(HERE, "..", "..", "mod", "src", "main", "resources", "assets", "factoryascension", "gunmesh", "bike")

WHEEL_R = 7.4
# Frame anchors: front axle, rear axle, steering head (bottom, top), engine mount, seat (front x, back x, y)
FRAMES = {
    "frame_standard": {"front": (14.0, WHEEL_R), "rear": (-13.0, WHEEL_R), "head": ((8.0, 15.5), (9.6, 21.0)), "engine": (0.5, 9.5),
                       "seat": (3.0, -12.0, 17.0), "max_engine": 3},
    "frame_long": {"front": (19.0, WHEEL_R), "rear": (-18.0, WHEEL_R), "head": ((13.0, 16.5), (14.6, 22.0)), "engine": (0.5, 10.0),
                   "seat": (6.0, -16.0, 18.5), "max_engine": 4},
}


def ring_pts(cx, cy, r, n, z):
    return [(cx + r * math.cos(2 * math.pi * k / n), cy + r * math.sin(2 * math.pi * k / n), z) for k in range(n)]


def wheel():
    m = M.Mesh()
    R, sec = WHEEL_R, 1.35
    rc = R - sec
    prof = [(1.05 * math.sin(2 * math.pi * k / 24), rc + sec * math.cos(2 * math.pi * k / 24)) for k in range(25)]
    M.lathe(m, "z", prof, 0, 0, "tyre", seg=48)
    # Knobs all round the tread
    for k in range(32):
        ang = 360.0 * k / 32
        xf = M.Xf(M.rot_matrix("z", ang))
        zoff = 0.45 if k % 2 else -0.45
        M.rbox(m, (R - 0.15, -0.55, zoff - 0.45), (R + 0.45, 0.55, zoff + 0.45), "tyre", 0.18, xf=xf)
    # Rim, hub, spokes, brake disc
    M.lathe(m, "z", [(-0.7, rc - 1.0), (-0.7, rc - 0.25), (0.7, rc - 0.25), (0.7, rc - 1.0), (-0.7, rc - 1.0)], 0, 0, "engine_alu", seg=48)
    M.cylinder(m, "z", -1.4, 1.4, 0, 0, 1.0, "engine_alu")
    M.cylinder(m, "z", -1.9, 1.9, 0, 0, 0.35, "steel_light")
    for k in range(18):
        a = 2 * math.pi * k / 18
        side = 1.2 if k % 2 else -1.2
        hub = (math.cos(a + 0.2) * 0.9, math.sin(a + 0.2) * 0.9, side)
        rim = (math.cos(a) * (rc - 1.05), math.sin(a) * (rc - 1.05), side * 0.35)
        M.pipe(m, [hub, rim], 0.09, "steel_light", seg=6)
    M.lathe(m, "z", [(1.45, 1.2), (1.45, 3.4), (1.6, 3.4), (1.6, 1.2)], 0, 0, "stainless", seg=40)
    return m


def fork(frame):
    """Fork tubes, triple clamps, bars, front fender and number plate: everything that turns with the bars. Built in
    the frame's coordinates (it's drawn turned about the steering axis)."""
    m = M.Mesh()
    F = FRAMES[frame]
    (bx, by), (tx, ty) = F["head"]
    fx, fy = F["front"]
    for z in (-2.1, 2.1):
        # Upper tube (fat) from the top clamp down past the head, lower (chrome) slider to the axle
        M.pipe(m, [(tx + 0.3, ty + 0.6, z), (bx - 0.2 + (fx - bx) * 0.35, by + (fy - by) * 0.35, z)], 0.75, "plastic_black", seg=16)
        M.pipe(m, [(bx + (fx - bx) * 0.3, by + (fy - by) * 0.3, z), (fx, fy, z)], 0.55, "chrome", seg=16)
        M.rbox(m, (fx - 1.0, fy - 0.9, z - 0.7), (fx + 1.0, fy + 0.9, z + 0.7), "engine_alu", 0.3)
    for (px, py) in ((tx + 0.3, ty + 0.4), (bx + 0.05, by + 0.2)):
        M.rbox(m, (px - 1.4, py - 0.45, -2.9), (px + 1.4, py + 0.45, 2.9), "engine_alu", 0.3)
    M.cylinder(m, "z", -1.9, 1.9, fx, fy, 0.3, "steel_light")
    # Bars, grips, levers, and a little speedo
    bar = [(tx - 0.8, ty + 1.6, -6.6), (tx - 0.4, ty + 1.3, -4.0), (tx + 0.1, ty + 0.9, -1.0), (tx + 0.1, ty + 0.9, 1.0), (tx - 0.4, ty + 1.3, 4.0), (tx - 0.8, ty + 1.6, 6.6)]
    M.pipe(m, M.spline([b for b in bar], per=4) if hasattr(M, "spline") and False else bar, 0.32, "steel_dark", seg=12)
    for z in (-1, 1):
        M.cylinder(m, "z", 5.2 * z, 7.0 * z, tx - 0.85, ty + 1.65, 0.48, "rubber")
        M.pipe(m, [(tx - 0.2, ty + 1.4, 4.4 * z), (tx + 1.6, ty + 1.2, 6.2 * z)], 0.12, "steel_light", seg=6)
    M.rbox(m, (tx - 0.2, ty + 1.2, -0.9), (tx + 0.9, ty + 2.4, 0.9), "plastic_black", 0.3)
    # Front fender (a long curved blade over the wheel) and the number plate
    fender = []
    for k in range(9):
        a = math.radians(40 + 110 * k / 8)
        fender.append((fx + math.cos(a) * (WHEEL_R + 1.4), fy + math.sin(a) * (WHEEL_R + 1.4) * 0.55 + 2.6))
    M.sweep(m, fender[::-1], 2.0, 0.22, "plastic_tint", z=0.0, rad=0.4)
    M.plate(m, [(tx + 0.7, ty - 3.5), (tx + 2.3, ty - 3.0), (tx + 2.0, ty + 1.2), (tx + 0.6, ty + 1.0)], -3.2, 3.2, "plastic_tint", bevel=0.2,
            xf=M.Xf(M.rot_matrix("y", 90), (tx + 1.4, ty - 1.0, 0)))
    return m


def frame(name):
    m = M.Mesh()
    F = FRAMES[name]
    (bx, by), (tx, ty) = F["head"]
    rx, ry = F["rear"]
    ex, ey = F["engine"]
    sf, sb, sy = F["seat"]
    long = name == "frame_long"
    # Steering head
    M.pipe(m, [(bx, by, 0), (tx, ty, 0)], 0.9, "parkerized", seg=16)
    # Twin spars from the head back to the swingarm pivot, a cradle under the engine
    piv = (ex - (7.0 if long else 5.5), ey + 1.0)
    for z in (-1.9, 1.9):
        M.pipe(m, [(tx - 0.4, ty - 0.8, z * 0.4), (tx - 4.0, ty - 2.2, z), (piv[0] + 1.0, piv[1] + 5.0, z), (piv[0], piv[1], z)], 0.55, "parkerized", seg=12)
        M.pipe(m, [(bx - 0.3, by + 0.5, 0), (bx - 2.5, by - 4.5, z * 0.7), (ex + 2.0, ey - 5.0, z), (piv[0] + 1.0, ey - 5.0, z), (piv[0], piv[1], z)], 0.45, "parkerized", seg=12)
        # Subframe under the seat
        M.pipe(m, [(piv[0] + 1.5, piv[1] + 4.5, z), (sb + 1.0, sy - 2.0, z * 1.1)], 0.3, "engine_alu", seg=8)
        # Swingarm to the rear axle (a tapered box arm)
        M.sweep(m, [(piv[0], piv[1]), (rx, ry)], [0.45, 0.35], [1.0, 0.7], "engine_alu", z=z * 1.25, rad=0.4)
    M.cylinder(m, "z", -2.6, 2.6, piv[0], piv[1], 0.55, "steel_light")
    M.cylinder(m, "z", -2.2, 2.2, rx, ry, 0.35, "steel_light")
    # Rear shock: spring and body, swingarm to the frame
    sh0, sh1 = (piv[0] - 3.0, piv[1] + 0.6), (piv[0] + 0.5, piv[1] + 6.0)
    M.pipe(m, [(sh0[0], sh0[1], 0), (sh1[0], sh1[1], 0)], 0.45, "engine_alu", seg=12)
    for k in range(9):
        t = k / 8
        c = (sh0[0] + (sh1[0] - sh0[0]) * t, sh0[1] + (sh1[1] - sh0[1]) * t)
        M.pipe(m, [(c[0] - 0.05, c[1], -0.8), (c[0] + 0.05, c[1] + 0.25, 0.8)], 0.14, "paint_red", seg=6)
    # Tank, seat, side panels, rear fender (painted)
    M.rbox(m, (ex - 1.5, ty - 4.8, -3.2), (tx - 1.0, ty - 0.8, 3.2), "plastic_tint", 1.4)
    M.cylinder(m, "y", ty - 0.9, ty - 0.4, tx - 4.5, 0, 0.8, "steel_dark")
    seat = [(sf, sy - 0.2), (sf - 4, sy + 0.3), (sb + 4, sy + 0.5), (sb, sy + 0.9)]
    M.sweep(m, seat, 2.7, 0.8, "seat", z=0.0, rad=0.6)
    for z in (-1, 1):
        M.plate(m, [(sb + 3, sy - 3.0), (sf - 3, sy - 3.4), (sf - 5, sy - 0.8), (sb + 1.5, sy - 0.2)], 2.5 * z, 2.9 * z, "plastic_tint", bevel=0.15)
        M.plate(m, [(ex + 3.5, ty - 5.5), (tx - 0.5, ty - 1.5), (tx - 2.0, ty - 7.5), (ex + 2.0, ty - 8.5)], 3.0 * z, 3.4 * z, "plastic_black", bevel=0.15)
    rear = [(sb + 3, sy - 0.4), (sb - 1, sy + 0.2), (sb - 6, sy + 1.3)]
    M.sweep(m, rear, 2.3, 0.2, "plastic_tint", z=0.0, rad=0.4)
    # Chain: the front sprocket at the engine, the rear one at the axle
    fs = (ex - 2.0, ey - 1.5)
    loop = []
    for k in range(12):
        a = math.pi / 2 + math.pi * k / 11
        loop.append((fs[0] + math.cos(a) * 1.2, fs[1] + math.sin(a) * 1.2, 2.6))
    for k in range(12):
        a = -math.pi / 2 + math.pi * k / 11
        loop.append((rx + math.cos(a) * 2.6, ry + math.sin(a) * 2.6, 2.6))
    M.pipe(m, loop, 0.18, "chain", seg=6, closed=True)
    M.lathe(m, "z", [(2.4, 0), (2.4, 2.7), (2.8, 2.7), (2.8, 0)], rx, ry, "steel_light", seg=32)
    # Footpegs
    for z in (-1, 1):
        M.rbox(m, (piv[0] + 1.5, ey - 4.0, 2.5 * z), (piv[0] + 3.5, ey - 3.3, 4.8 * z), "steel_light", 0.2)
    return m


def engine(name):
    """Drawn at the frame's engine mount (0, 0 here)."""
    m = M.Mesh()
    if name == "engine_v12":
        # A long crankcase with two banks of six, intake trumpets, and twelve headers into twin pipes
        M.rbox(m, (-9.0, -4.5, -2.6), (11.0, 1.5, 2.6), "engine_alu", 0.8)
        for side in (-1, 1):
            xf = M.Xf(M.rot_matrix("x", 32 * side), (0, 1.0, 0))
            M.rbox(m, (-8.5, 1.0, -1.7), (10.5, 6.5, 1.7), "steel_dark", 0.6, xf=xf)
            M.rbox(m, (-8.8, 6.3, -2.0), (10.8, 7.6, 2.0), "cerakote_red", 0.5, xf=xf)
            for k in range(6):
                cx = -7.0 + k * 3.2
                M.lathe(m, "y", [(7.6, 0), (7.6, 0.8), (8.6, 0.7), (9.8, 1.05), (9.9, 0.9), (8.7, 0.55)], cx, 0, "chrome", seg=20, xf=xf)
                hx = cx
                M.pipe(m, [(hx, 3.0, 3.6 * side), (hx + 0.5, -1.0, 4.2 * side), (hx - 2.0, -3.6, 3.6 * side), (-11.0, -4.2, 3.2 * side)], 0.38, "stainless", seg=10)
        for side in (-1, 1):
            M.pipe(m, [(-11.0, -4.2, 3.2 * side), (-16.0, -2.0, 4.0 * side), (-22.0, 2.0, 4.2 * side)], 0.8, "stainless", seg=16)
            M.lathe(m, "x", [(-26.0, 0), (-26.0, 1.0), (-25.0, 1.4), (-20.0, 1.4), (-19.0, 0.9)], 2.0, 4.2 * side, "carbon", seg=24)
        return m
    size = {"engine_125": 0.75, "engine_250": 0.9, "engine_450": 1.08}[name]
    s = size
    # Crankcase, gearbox cover, cylinder with cooling fins, head, carb
    M.rbox(m, (-4.0 * s, -4.0 * s, -2.2 * s), (4.0 * s, 0.5 * s, 2.2 * s), "engine_alu", 0.9 * s)
    M.lathe(m, "z", [(2.2 * s, 0), (2.2 * s, 2.6 * s), (2.9 * s, 2.4 * s), (2.9 * s, 0)], -1.5 * s, -1.8 * s, "engine_alu", seg=28)
    cyl = M.Xf(M.rot_matrix("z", -18), (1.0 * s, 0.5 * s, 0))
    fins = []
    for k in range(7 if name != "engine_450" else 4):
        y = 0.6 * s + k * 0.75 * s
        fins += [(y, 0), (y, 2.6 * s), (y + 0.35 * s, 2.6 * s), (y + 0.35 * s, 1.8 * s)]
    fins += [(6.0 * s, 1.8 * s), (6.0 * s, 0)]
    M.lathe(m, "y", fins, 1.0 * s, 0, "engine_alu" if name != "engine_450" else "steel_dark", seg=28, xf=cyl)
    M.rbox(m, (-1.4 * s, 5.8 * s, -2.0 * s), (3.4 * s, 7.4 * s, 2.0 * s), "steel_dark", 0.5 * s, xf=cyl)
    M.cylinder(m, "x", -4.0 * s, -1.0 * s, 4.0 * s, 0, 0.9 * s, "steel_dark", xf=cyl)
    if name == "engine_450":
        # Radiators either side, behind shrouds
        for z in (-1, 1):
            M.rbox(m, (3.5, 2.0, 3.0 * z), (6.0, 9.0, 3.8 * z), "steel_dark", 0.3)
    # Exhaust: a two-stroke's expansion chamber (125, 250) or a four-stroke header, up to a silencer under the seat
    big = name == "engine_450"
    path = [(3.5 * s, 4.0 * s, 0.0), (5.0 * s, 1.0, 2.0), (3.0, -3.5, 3.0), (-4.0, -2.5, 3.2), (-9.0, 4.0, 3.4), (-13.0, 7.0, 3.5)]
    M.pipe(m, path, 0.55 if big else 0.75, "steel_light" if big else "parkerized", seg=14)
    M.lathe(m, "x", [(-21.0, 0), (-21.0, 0.9), (-20.0, 1.25), (-13.5, 1.25), (-12.5, 0.8)], 7.6, 3.5, "carbon" if big else "engine_alu", seg=24,
            xf=M.Xf(M.rot_matrix("z", 14), (-13.0, 7.0, 3.5)))
    return m


PARTS = {"wheel": wheel, "fork_standard": lambda: fork("frame_standard"), "fork_long": lambda: fork("frame_long"),
         "frame_standard": lambda: frame("frame_standard"), "frame_long": lambda: frame("frame_long"),
         "engine_125": lambda: engine("engine_125"), "engine_250": lambda: engine("engine_250"),
         "engine_450": lambda: engine("engine_450"), "engine_v12": lambda: engine("engine_v12")}


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, make in PARTS.items():
        m = make()
        lo, hi = m.bounds()
        assert all(-64 <= v <= 64 for v in lo + hi), (name, lo, hi)
        size = m.write(os.path.join(OUT, name + ".fam"))
        print("bike %-15s %6d verts %6d quads %7d bytes" % (name, len(m.verts), len(m.quads), size))


if __name__ == "__main__":
    main()
