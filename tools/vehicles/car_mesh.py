#!/usr/bin/env python3
"""Meshes for the quad bike and the pickup truck (drawn by client/vehicle/CarRenderer with client/gun/GunMesh). Same
conventions as bike_mesh.py: model units (16 = one block), facing +x, ground y = 0, z = 0 the middle. The anchors
(axles, seats, the gun mount) are in CARS; vehicle/Car.java has the same numbers.

  python3 tools/vehicles/car_mesh.py      (make_mod_assets.py runs it)
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "guns"))
import mesh as M  # noqa: E402

OUT = os.path.join(HERE, "..", "..", "mod", "src", "main", "resources", "assets", "factoryascension", "gunmesh", "car")

CARS = {
    "quad": {"wheel_r": 6.2, "axles": (12.5, -12.0), "track": 11.0},
    "pickup": {"wheel_r": 7.6, "axles": (27.0, -24.0), "track": 13.5, "mount": (-14.0, 26.0)},
}


def tyre(R, width, rim_mat, knobs):
    """A wheel on the z axis: a fat tyre (knobbly or road tread), the rim, the hub with its nuts."""
    m = M.Mesh()
    sec = width * 0.5
    rc = R - sec * 0.9
    prof = [(sec * math.sin(2 * math.pi * k / 28), rc + sec * 0.9 * math.cos(2 * math.pi * k / 28)) for k in range(29)]
    M.lathe(m, "z", prof, 0, 0, "tyre", seg=48)
    n = 30 if knobs else 40
    for k in range(n):
        xf = M.Xf(M.rot_matrix("z", 360.0 * k / n))
        if knobs:
            for zoff in (-sec * 0.5, sec * 0.5):
                zz = zoff + (0.35 if k % 2 else -0.35)
                M.rbox(m, (R - 0.2, -0.6, zz - 0.55), (R + 0.5, 0.6, zz + 0.55), "tyre", 0.2, xf=xf)
        else:
            M.rbox(m, (R - 0.15, -0.25, -sec * 0.8), (R + 0.2, 0.25, sec * 0.8), "tyre", 0.1, xf=xf)
    # Rim (dished), the hub, five or six nuts
    rr = rc - 0.4
    M.lathe(m, "z", [(-sec * 0.7, rr), (-sec * 0.7, rr - 0.8), (sec * 0.2, rr - 1.2), (sec * 0.45, rr * 0.45), (sec * 0.5, 0.0)], 0, 0, rim_mat, seg=40)
    M.cylinder(m, "z", sec * 0.4, sec * 0.75, 0, 0, rr * 0.3, "steel_dark")
    nuts = 6 if R > 7 else 4
    for k in range(nuts):
        a = 2 * math.pi * k / nuts
        M.cylinder(m, "z", sec * 0.5, sec * 0.8, math.cos(a) * rr * 0.36, math.sin(a) * rr * 0.36, 0.28, "chrome", seg=6)
    for k in range(5 if R > 7 else 6):
        a = 2 * math.pi * k / (5 if R > 7 else 6)
        hole = (math.cos(a) * rr * 0.68, math.sin(a) * rr * 0.68)
        M.cylinder(m, "z", sec * 0.15, sec * 0.35, hole[0], hole[1], rr * 0.14, "bore", seg=12)
    return m


def quad():
    m = M.Mesh()
    C = CARS["quad"]
    fx, rx = C["axles"]
    R, T = C["wheel_r"], C["track"]
    # Chassis: tube frame rails, skid plate
    for z in (-3.5, 3.5):
        M.pipe(m, [(fx + 4, 5.0, z), (fx - 2, 4.0, z), (rx + 3, 4.0, z), (rx - 4, 6.0, z)], 0.55, "steel_dark", seg=10)
        M.pipe(m, [(fx + 1, 5.0, z), (fx - 2, 10.5, z), (rx + 4, 11.5, z), (rx - 3, 10.0, z), (rx - 4, 6.0, z)], 0.5, "steel_dark", seg=10)
    M.rbox(m, (rx - 2, 3.0, -4.2), (fx + 3, 3.8, 4.2), "steel_light", 0.3)
    # Engine and the exhaust out the back
    M.rbox(m, (-5.0, 4.5, -3.0), (4.0, 10.0, 3.0), "engine_alu", 0.8)
    for k in range(5):                                       # the cylinder, finned, leaning back
        M.rbox(m, (-2.6 + k * 0.1, 10.0 + k * 0.75, -2.6), (2.4 + k * 0.1, 10.35 + k * 0.75, 2.6), "engine_alu", 0.15)
    M.rbox(m, (-2.0, 13.6, -2.0), (2.4, 14.6, 2.0), "steel_dark", 0.3)
    M.pipe(m, [(2.0, 11.0, 1.5), (4.0, 9.0, 4.0), (-2.0, 7.5, 4.6), (rx - 1, 10.0, 4.6)], 0.5, "parkerized", seg=10)
    M.lathe(m, "x", [(rx - 6.5, 0), (rx - 6.5, 0.8), (rx - 5.5, 1.1), (rx - 0.5, 1.1), (rx, 0.6)], 10.0, 4.6, "carbon", seg=20)
    # Suspension: A-arms and shocks to every wheel
    for x in (fx, rx):
        for s in (-1, 1):
            hub = (x, R, s * (T - 2.5))
            for dy in (-1.2, 1.4):
                M.pipe(m, [(x + 2.2, 5.0 + dy, s * 3.6), (hub[0], hub[1] + dy * 0.6, hub[2])], 0.3, "steel_light", seg=8)
                M.pipe(m, [(x - 2.2, 5.0 + dy, s * 3.6), (hub[0], hub[1] + dy * 0.6, hub[2])], 0.3, "steel_light", seg=8)
            M.pipe(m, [(x + (1.0 if x > 0 else -1.0), 12.0, s * 4.0), (hub[0], hub[1] + 1.0, hub[2] - s * 1.0)], 0.55, "paint_red", seg=10)
            M.cylinder(m, "z", hub[2] - 1.2, hub[2] + 1.2, hub[0], hub[1], 0.9, "steel_dark")
    # Bodywork (painted): front and rear fenders with racks, the tank, the seat
    for x0, x1, top in ((fx - 7, fx + 9, 14.5), (rx - 8, rx + 7, 15.0)):
        M.rbox(m, (x0, top - 2.0, -T - 1.0), (x1, top, T + 1.0), "plastic_tint", 1.4)
        M.rbox(m, (x0 + 1, top - 5.0, -6.0), (x1 - 1, top - 1.5, 6.0), "plastic_tint", 1.2)
        for z in (-4.5, 0, 4.5):
            M.pipe(m, [(x0 + 1.5, top + 0.6, z), (x1 - 1.5, top + 0.6, z)], 0.28, "steel_dark", seg=8)
        for xx in (x0 + 1.5, x1 - 1.5):
            M.pipe(m, [(xx, top + 0.6, -T + 1), (xx, top + 0.6, T - 1)], 0.28, "steel_dark", seg=8)
    M.rbox(m, (-1.0, 11.0, -3.6), (6.5, 15.0, 3.6), "plastic_tint", 1.5)
    M.sweep(m, [(4.0, 15.2), (0.0, 15.6), (-6.0, 16.0), (-10.0, 16.4)], 3.2, 0.9, "seat", z=0.0, rad=0.7)
    # Bars, grips, the headlight pod, foot wells
    M.pipe(m, [(5.0, 14.5, 0), (6.5, 19.5, 0)], 0.7, "steel_dark", seg=12)
    M.pipe(m, [(5.5, 20.5, -7.5), (6.2, 19.8, -4.0), (6.6, 19.5, 0), (6.2, 19.8, 4.0), (5.5, 20.5, 7.5)], 0.35, "steel_dark", seg=10)
    for z in (-1, 1):
        M.cylinder(m, "z", 6.0 * z, 8.2 * z, 5.5, 20.5, 0.5, "rubber")
        M.rbox(m, (-5.0, 5.0, 4.8 * z), (3.0, 5.7, 8.5 * z), "steel_light", 0.2)
    M.rbox(m, (7.0, 18.0, -2.4), (9.0, 21.0, 2.4), "plastic_black", 0.6)
    for z in (-1.2, 1.2):
        M.cylinder(m, "x", 8.9, 9.3, 19.5, z, 0.8, "glow_white")
    M.cylinder(m, "x", rx - 8.1, rx - 7.8, 14.0, 0, 0.6, "paint_red")
    return m


def pickup():
    m = M.Mesh()
    C = CARS["pickup"]
    fx, rx = C["axles"]
    R, T = C["wheel_r"], C["track"]
    W = 15.5                     # body half width
    sill, belt, roof = 9.0, 21.0, 31.0
    # Ladder frame, axles, leaf springs, the diff
    for z in (-6.0, 6.0):
        M.rbox(m, (rx - 15, 6.5, z - 0.8), (fx + 12, 9.0, z + 0.8), "steel_dark", 0.3)
    M.cylinder(m, "z", -T + 2, T - 2, rx, R, 0.8, "steel_dark")
    M.lathe(m, "z", [(-2.2, 0), (-2.2, 2.4), (2.2, 2.4), (2.2, 0)], rx, R, "steel_dark", seg=24)
    for x in (fx, rx):
        for s in (-1, 1):
            for k in range(4):
                M.sweep(m, [(x - 7 + k * 0.4, R + 1.6 - k * 0.3), (x, R + 0.8 - k * 0.3), (x + 7 - k * 0.4, R + 1.6 - k * 0.3)], 0.25, 1.0, "steel", z=s * 6.0, rad=0.1)
            M.pipe(m, [(x + 2.0 * s, R + 0.5, s * (T - 3.5)), (x + 1.0 * s, 13.5, s * 7.5)], 0.55, "paint_red", seg=10)
    # Exhaust down the side
    M.pipe(m, [(fx - 6, 7.0, -4.0), (0.0, 6.0, -8.0), (rx - 4, 6.0, -8.0), (rx - 14, 6.5, -9.5)], 0.6, "steel_light", seg=10)
    # The body (painted): bonnet and wings, the cab, the bed; wheel arches cut by flares
    nose = fx + 14
    M.rbox(m, (fx - 8, sill, -W), (nose, belt - 1.0, W), "plastic_tint", 1.8)                 # front end
    M.sweep(m, [(fx - 9, belt + 0.6), (fx + 4, belt - 0.2), (nose - 1, belt - 1.6)], W - 1.0, 0.5, "plastic_tint", z=0.0, rad=0.8)   # bonnet
    cab0, cab1 = rx + 12, fx - 9
    M.rbox(m, (cab0, sill, -W), (cab1, belt, W), "plastic_tint", 1.6)                          # cab lower
    # Cab upper: pillars and roof, glass between
    M.rbox(m, (cab0 + 1.5, roof - 1.6, -W + 1.2), (cab1 - 5.5, roof, W - 1.2), "plastic_tint", 1.0)
    for z in (-W + 1.6, W - 1.6):
        M.pipe(m, [(cab1 - 0.5, belt, z), (cab1 - 6.0, roof - 0.8, z)], 0.75, "plastic_tint", seg=8)        # A pillar
        M.pipe(m, [(cab0 + 2.0, belt, z), (cab0 + 2.0, roof - 0.8, z)], 0.9, "plastic_tint", seg=8)       # C pillar
        M.pipe(m, [(cab0 + 15.0, belt, z), (cab0 + 14.0, roof - 0.8, z)], 0.6, "plastic_tint", seg=8)     # B pillar
    M.plate(m, [(cab1 - 0.8, belt + 0.4), (cab1 - 5.8, roof - 1.4), (cab1 - 6.6, roof - 1.4), (cab1 - 1.6, belt + 0.4)], -W + 2.2, W - 2.2, "lens", bevel=0.05)
    for z in (-1, 1):
        M.plate(m, [(cab0 + 2.8, belt + 0.3), (cab1 - 1.5, belt + 0.3), (cab1 - 6.0, roof - 1.8), (cab0 + 2.8, roof - 1.8)], z * (W - 0.6), z * (W - 0.3), "lens", bevel=0.05)
    M.plate(m, [(-0.1, belt + 0.5), (0.1, belt + 0.5), (0.1, roof - 2.0), (-0.1, roof - 2.0)], -W + 2.5, W - 2.5, "lens", bevel=0.02,
            xf=M.Xf(M.rot_matrix("y", 0), (cab0 + 0.9, 0, 0)))
    # The bed: floor, sides, tailgate, wheel tubs
    bed0, bed1 = rx - 15, cab0 - 0.5
    M.rbox(m, (bed0, sill, -W), (bed1, sill + 2.0, W), "plastic_tint", 0.6)
    for z in (-1, 1):
        M.rbox(m, (bed0, sill, z * (W - 1.2)), (bed1, belt - 0.5, z * W), "plastic_tint", 0.5)
        M.rbox(m, (rx - 9, sill + 1.5, z * (W - 6.0)), (rx + 9, R + 4.0, z * (W - 1.2)), "plastic_tint", 2.0)
        M.rbox(m, (bed0 + 1, belt - 0.4, z * (W - 1.0)), (bed1 - 1, belt + 0.4, z * W), "steel_dark", 0.3)
    M.rbox(m, (bed0, sill, -W), (bed0 + 1.2, belt - 0.5, W), "plastic_tint", 0.5)
    M.rbox(m, (bed0 + 2, sill + 2.0, -W + 1.5), (bed1 - 1, sill + 2.3, W - 1.5), "steel_dark", 0.1)
    # Wheel-arch flares, bumpers, grille, lights, mirrors, the roll bar and the gun mount
    for x in (fx, rx):
        for z in (-1, 1):
            arch = [(x + math.cos(math.radians(a)) * (R + 1.8), R + math.sin(math.radians(a)) * (R + 1.8)) for a in range(0, 181, 20)]
            M.sweep(m, arch, 0.9, 0.4, "plastic_black", z=z * (W + 0.4), rad=0.3)
    M.rbox(m, (nose - 0.5, 6.5, -W - 0.3), (nose + 2.5, 11.0, W + 0.3), "steel_dark", 0.8)
    M.rbox(m, (bed0 - 2.0, 7.0, -W), (bed0 + 0.5, 10.5, W), "steel_dark", 0.8)
    M.rbox(m, (nose - 0.2, 12.0, -9.0), (nose + 0.5, 18.5, 9.0), "plastic_black", 0.4)
    for k in range(6):
        M.rbox(m, (nose + 0.3, 12.8 + k, -8.4), (nose + 0.6, 13.2 + k, 8.4), "chrome", 0.1)
    for z in (-1, 1):
        M.rbox(m, (nose - 0.6, 15.0, z * 10.0), (nose + 0.6, 18.0, z * 14.5), "glow_white", 0.5)
        M.rbox(m, (bed0 - 0.4, 14.0, z * 11.5), (bed0 + 0.4, 19.0, z * 14.5), "paint_red", 0.3)
        M.pipe(m, [(cab1 - 1.5, belt + 0.8, z * W), (cab1 - 1.0, belt + 2.0, z * (W + 2.5))], 0.25, "plastic_black", seg=6)
        M.rbox(m, (cab1 - 2.5, belt + 1.5, z * (W + 2.0)), (cab1 - 0.8, belt + 4.5, z * (W + 3.5)), "plastic_black", 0.4)
        M.pipe(m, [(cab0 - 1.2, belt, z * (W - 1.0)), (cab0 - 1.2, roof + 3.5, z * (W - 1.5)), (cab0 - 1.2, roof + 3.5, 0)], 0.75, "steel_dark", seg=10)
    mx, my = C["mount"]
    M.lathe(m, "y", [(sill + 2.0, 0), (sill + 2.0, 2.5), (sill + 3.0, 2.5), (my - 4.0, 1.2), (my - 3.0, 2.0), (my - 2.4, 2.0), (my - 2.4, 0)], mx, 0, "steel_dark", seg=20)
    return m


def turret():
    """The mounted gun (a heavy machine gun on a pintle): drawn at the mount, turned to the gunner's aim. Barrel along +x."""
    m = M.Mesh()
    M.rbox(m, (-1.5, -0.5, -1.6), (1.5, 1.6, 1.6), "steel_dark", 0.3)                 # cradle
    for z in (-1, 1):
        M.rbox(m, (-1.2, 0.5, z * 1.6), (1.2, 4.0, z * 2.0), "steel_dark", 0.2)
    M.rbox(m, (-7.0, 2.2, -1.6), (5.0, 5.4, 1.6), "parkerized", 0.4)                  # receiver
    M.rbox(m, (-4.0, 5.4, -1.4), (3.5, 6.0, 1.4), "parkerized", 0.2)                  # feed cover
    M.cylinder(m, "x", 5.0, 9.5, 3.8, 0, 1.25, "parkerized")                            # barrel jacket
    M.cylinder(m, "x", 9.5, 26.0, 3.8, 0, 0.55, "gunmetal")                             # barrel
    M.lathe(m, "x", [(26.0, 0), (26.0, 0.95), (28.0, 0.95), (28.0, 0.3), (27.5, 0)], 3.8, 0, "steel_dark", seg=16)   # flash hider
    M.cylinder(m, "x", 26.0, 28.4, 3.8, 0, 0.3, "bore", seg=12)
    for z in (-1, 1):                                                                   # spade grips, butterfly trigger
        M.pipe(m, [(-7.0, 3.0, z * 1.2), (-9.0, 3.0, z * 1.6), (-9.6, 4.5, z * 1.6)], 0.35, "steel_dark", seg=8)
        M.cylinder(m, "y", 4.4, 6.6, -9.6, z * 1.6, 0.42, "rubber")
    M.rbox(m, (-10.0, 4.8, -0.6), (-9.2, 5.2, 0.6), "steel_light", 0.1)
    M.rbox(m, (-2.0, -2.0, 2.0), (3.0, 2.0, 5.5), "polymer_od", 0.4)                   # ammo can
    M.pipe(m, [(1.0, 2.0, 3.8), (0.5, 4.2, 2.6), (0.0, 4.8, 1.6)], 0.5, "brass", seg=6)  # the belt going in
    M.plate(m, [(-0.8, 5.8), (0.8, 5.8), (0.6, 8.2), (-0.6, 8.2)], -0.2, 0.2, "steel_dark", bevel=0.05)   # rear sight
    # The gun shield: two wings either side of the gun with a gap to aim through, angled back a little
    for side in (-1, 1):
        M.rbox(m, (-3.4, 1.5, side * 1.8), (-3.0, 8.5, side * 6.0), "parkerized", 0.15,
               xf=M.Xf(M.rot_matrix("y", side * 12), (0, 0, 0)))
    M.rbox(m, (-3.4, 1.5, -6.0), (-3.0, 2.6, 6.0), "parkerized", 0.15)
    return m


def body_bag():
    """A body bag: a long zipped bag, lumpy, with carry handles."""
    m = M.Mesh()
    M.rbox(m, (-14.0, 0.0, -5.0), (14.0, 4.5, 5.0), "canvas", 2.2)
    M.rbox(m, (-11.0, 3.0, -3.8), (-3.0, 6.0, 3.8), "canvas", 1.8)       # chest
    M.rbox(m, (6.0, 3.0, -3.0), (12.5, 5.8, 3.0), "canvas", 2.0)          # head end
    M.pipe(m, [(-13.0, 4.6, 0), (13.0, 4.6, 0)], 0.2, "steel_dark", seg=6)  # zip
    M.rbox(m, (8.5, 4.4, -0.6), (10.0, 5.2, 0.6), "steel_light", 0.2)
    for x in (-10.0, 0.0, 10.0):
        for z in (-1, 1):
            M.pipe(m, [(x - 1.5, 1.5, z * 5.0), (x, 2.5, z * 6.2), (x + 1.5, 1.5, z * 5.0)], 0.25, "tape", seg=6)
    return m


PARTS = {"body_bag": body_bag, "quad": quad, "pickup": pickup, "turret": turret,
         "wheel_quad": lambda: tyre(CARS["quad"]["wheel_r"], 5.0, "steel_dark", True),
         "wheel_pickup": lambda: tyre(CARS["pickup"]["wheel_r"], 5.2, "steel_light", False)}


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, make in PARTS.items():
        m = make()
        lo, hi = m.bounds()
        assert all(-64 <= v <= 64 for v in lo + hi), (name, lo, hi)
        size = m.write(os.path.join(OUT, name + ".fam"))
        print("car %-14s %6d verts %6d quads %7d bytes" % (name, len(m.verts), len(m.quads), size))


if __name__ == "__main__":
    main()
