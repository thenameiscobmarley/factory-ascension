#!/usr/bin/env python3
"""Smooth, detailed meshes for every attachment shape (gunmesh/attach/<shape>.fam), drawn on the guns up close and as
the attachment items' icons. Same anchor and directions as attach.py's cuboid models: the mount point is (8, 8, 8),
muzzle parts reach forward (+x), optics sit on top, underbarrel and magazine parts hang below, tactical units stick out
to the right (+z), stocks reach back (-x). "tint" parts take the attachment's finish colour. Optics are hollow: a
clear lens and a glowing reticle you can see through when aiming.

  python3 tools/guns/attach_mesh.py      (tools/guns/build.py runs it after the cuboid models)
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mesh as M  # noqa: E402

OUT = os.path.join(HERE, "..", "..", "mod", "src", "main", "resources", "assets", "factoryascension", "gunmesh", "attach")
C = 8.0
RX = lambda d: M.rot_matrix("x", d)   # noqa: E731
RZ = lambda d: M.rot_matrix("z", d)   # noqa: E731


def X(rot, origin=(C, C, C)):
    return M.Xf(rot, origin)


# ---------------- Building blocks ----------------
def bore(m, x, r, y=C, z=C):
    """The dark hole a round leaves by: a short black tube face-on at x."""
    M.cylinder(m, "x", x - 0.3, x + 0.01, y, z, r, "bore", ends=False)
    M.lathe(m, "x", [(x + 0.01, r), (x + 0.01, 0)], y, z, "bore")


def can(m, length, r, front_taper=0.18, rings=3, end_bore=0.32):
    """A suppressor: a tube with chamfered ends, a few machined grooves, a front cap and its bore."""
    x0, x1 = C, C + length
    prof = [(x0, 0.0), (x0, r * 0.7), (x0 + 0.25, r), (x1 - front_taper * r * 2, r), (x1, r * 0.82), (x1, 0.0)]
    M.lathe(m, "x", prof, C, C, "tint", seg=28)
    for k in range(rings):
        gx = x0 + 0.9 + k * 0.42
        M.lathe(m, "x", [(gx, r + 0.004), (gx, r - 0.06), (gx + 0.18, r - 0.06), (gx + 0.18, r + 0.004)], C, C, "steel_dark", seg=28)
    # Wrench flats and the mount at the back
    M.cylinder(m, "x", x0 - 0.45, x0 + 0.05, C, C, r * 0.62, "steel_dark", seg=6)
    bore(m, x1 + 0.005, end_bore)


def ports(m, x0, x1, r, n, rows=(90, 270), mat="bore", w=0.22, h=0.45):
    """Slots cut into a round part (dark insets at the given angles round the x axis)."""
    for k in range(n):
        x = x0 + (k + 0.5) * (x1 - x0) / n
        for ang in rows:
            xf = X(RX(ang))
            M.rbox(m, (x - w / 2, C + r - 0.05, C - h / 2), (x + w / 2, C + r + 0.012, C + h / 2), mat, 0.03, xf=xf)


def rail_on(m, x0, x1, y, z=C, w=1.0):
    """A picatinny rail: a base and its teeth."""
    M.rbox(m, (x0, y - 0.25, z - w / 2), (x1, y, z + w / 2), "steel_dark", 0.06)
    n = max(2, int((x1 - x0) / 0.5))
    for k in range(n):
        x = x0 + 0.1 + k * (x1 - x0 - 0.2) / n
        M.rbox(m, (x, y, z - w / 2), (x + 0.26, y + 0.22, z + w / 2), "steel_dark", 0.04)


def mount_base(m, x0, x1, w=1.3, h=0.55):
    """An optic's mount: clamps onto the rail with a cross bolt and a lever."""
    M.rbox(m, (x0, C, C - w / 2), (x1, C + h, C + w / 2), "steel_dark", 0.12)
    M.cylinder(m, "z", C - w / 2 - 0.12, C + w / 2 + 0.12, (x0 + x1) / 2, C + 0.2, 0.16, "steel_light", seg=10)
    M.rbox(m, ((x0 + x1) / 2 - 0.5, C + 0.05, C + w / 2 + 0.1), ((x0 + x1) / 2 + 0.6, C + 0.3, C + w / 2 + 0.22), "steel_dark", 0.05)


def window(m, x0, x1, yc, hw, hh, wall=0.16, mat="tint"):
    """A hollow rectangular hood along x: four rounded bars round an open window."""
    M.rbox(m, (x0, yc + hh, C - hw - wall), (x1, yc + hh + wall, C + hw + wall), mat, 0.08)
    M.rbox(m, (x0, yc - hh - wall, C - hw - wall), (x1, yc - hh, C + hw + wall), mat, 0.08)
    M.rbox(m, (x0, yc - hh, C - hw - wall), (x1, yc + hh, C - hw), mat, 0.06)
    M.rbox(m, (x0, yc - hh, C + hw), (x1, yc + hh, C + hw + wall), mat, 0.06)


def lens(m, x, yc, r, zc=C, tint="lens"):
    """A thin round glass across the x axis."""
    M.cylinder(m, "x", x - 0.02, x + 0.02, yc, zc, r, tint, seg=24)


def pane(m, x, yc, hw, hh):
    """A thin rectangular glass across x."""
    M.rbox(m, (x - 0.02, yc - hh, C - hw), (x + 0.02, yc + hh, C + hw), "lens", 0.01)


def dot(m, x, yc, r=0.06, mat="red_dot"):
    """A reticle dot facing the shooter (both ways, so it's seen through the glass)."""
    M.rbox(m, (x - 0.01, yc - r, C - r), (x + 0.01, yc + r, C + r), mat, 0.0)


def ring_reticle(m, x, yc, r, mat="red_dot", n=20, t=0.035):
    for k in range(n):
        a0, a1 = 2 * math.pi * k / n, 2 * math.pi * (k + 0.55) / n
        p0 = (x, yc + r * math.sin(a0), C + r * math.cos(a0))
        p1 = (x, yc + r * math.sin(a1), C + r * math.cos(a1))
        M.pipe(m, [p0, p1], t, mat, seg=4)


def tube_scope(m, length, r, bell, eye_bell=None, turrets=True, reticle="red_dot"):
    """A rifle scope: objective bell in front, a tube, the turrets, an eyepiece; glass at both ends, a reticle."""
    x0, x1 = C - length * 0.38, C + length * 0.62
    eb = eye_bell or bell * 0.85
    yc = C + 0.9 + r * bell
    prof = [(x0, 0.0), (x0, r * eb - 0.12), (x0 + 0.1, r * eb), (x0 + 1.4, r * eb), (x0 + 2.2, r), (x1 - 2.4, r), (x1 - 1.3, r * bell),
            (x1, r * bell), (x1, r * bell - 0.12)]
    # Hollow: the profile comes back along the inside so you see through it
    inner = [(x1, r * bell - 0.2), (x0, r * eb - 0.2)]
    M.lathe(m, "x", prof + inner + [(x0, r * eb - 0.12)], yc, C, "tint", seg=32)
    lens(m, x1 - 0.12, yc, r * bell - 0.2)
    lens(m, x0 + 0.12, yc, r * eb - 0.2)
    # Reticle: a fine cross and a dot, in the middle of the tube
    xm = (x0 + x1) / 2
    M.rbox(m, (xm - 0.01, yc - 0.012, C - r * 0.85), (xm + 0.01, yc + 0.012, C + r * 0.85), "steel_dark", 0.0)
    M.rbox(m, (xm - 0.01, yc - r * 0.85, C - 0.012), (xm + 0.01, yc + r * 0.85, C + 0.012), "steel_dark", 0.0)
    dot(m, xm + 0.02, yc, 0.04, reticle)
    if turrets:
        M.cylinder(m, "y", yc + r - 0.05, yc + r + 0.55, xm, C, 0.42, "tint", seg=20)
        M.cylinder(m, "y", yc + r + 0.5, yc + r + 0.62, xm, C, 0.45, "steel_dark", seg=20)
        M.cylinder(m, "z", C + r - 0.05, C + r + 0.55, xm, yc, 0.42, "tint", seg=20)
        M.cylinder(m, "x", xm - 2.0, xm - 1.6, yc, C, r + 0.06, "steel_dark", seg=28)           # power ring
        for k in range(10):
            a = 2 * math.pi * k / 10
            M.rbox(m, (xm - 2.0, yc + (r + 0.05) * math.sin(a) - 0.05, C + (r + 0.05) * math.cos(a) - 0.05),
                   (xm - 1.6, yc + (r + 0.05) * math.sin(a) + 0.05, C + (r + 0.05) * math.cos(a) + 0.05), "steel_dark", 0.02)
    # Rings and their bases on the rail
    for rx in (C - length * 0.12, C + length * 0.28):
        M.cylinder(m, "x", rx - 0.35, rx + 0.35, yc, C, r + 0.12, "steel_dark", seg=28, hollow=r + 0.01)
        M.rbox(m, (rx - 0.4, C, C - 0.55), (rx + 0.4, yc - r + 0.1, C + 0.55), "steel_dark", 0.08)
        M.rbox(m, (rx - 0.45, C, C - 0.75), (rx + 0.45, C + 0.35, C + 0.75), "steel_dark", 0.08)
        M.cylinder(m, "z", C - 0.85, C + 0.85, rx, C + 0.18, 0.12, "steel_light", seg=8)


# ---------------- The shapes ----------------
def comp(m):
    M.lathe(m, "x", [(C, 0), (C, 0.55), (C + 0.15, 0.62), (C + 2.0, 0.62), (C + 2.15, 0.5), (C + 2.15, 0)], C, C, "tint", seg=24)
    ports(m, C + 0.35, C + 1.9, 0.62, 3, rows=(90, 60, 120), w=0.28, h=0.22)
    bore(m, C + 2.16, 0.28)


def brake(m):
    M.rbox(m, (C, C - 0.62, C - 0.78), (C + 2.5, C + 0.62, C + 0.78), "tint", 0.25)
    for k in range(3):
        x = C + 0.35 + k * 0.7
        for s in (-1, 1):
            M.rbox(m, (x, C - 0.5, C + s * 0.6 - 0.25), (x + 0.38, C + 0.5, C + s * 0.6 + 0.25), "bore", 0.05)
    bore(m, C + 2.51, 0.28)


def flash(m):
    M.cylinder(m, "x", C, C + 0.6, C, C, 0.5, "tint", seg=24)
    for k in range(5):
        xf = X(RX(72 * k))
        M.rbox(m, (C + 0.55, C + 0.28, C - 0.1), (C + 1.9, C + 0.5, C + 0.1), "tint", 0.05, xf=xf)
    bore(m, C + 0.61, 0.28)


def choke(m):
    M.lathe(m, "x", [(C, 0), (C, 0.62), (C + 0.9, 0.75), (C + 1.25, 0.75), (C + 1.3, 0.65), (C + 1.3, 0)], C, C, "tint", seg=28)
    for k in range(8):
        M.rbox(m, (C + 0.9, C + 0.7, C - 0.06), (C + 1.25, C + 0.78, C + 0.06), "steel_dark", 0.02, xf=X(RX(45 * k)))
    bore(m, C + 1.31, 0.45)


def bayonet(m):
    M.rbox(m, (C - 1.0, C - 1.3, C - 0.28), (C + 0.6, C - 0.4, C + 0.28), "tint", 0.12)
    M.cylinder(m, "x", C - 1.0, C + 0.6, C - 0.2, C, 0.38, "steel_dark", hollow=0.3)
    blade = [(C + 0.6, C - 1.15), (C + 6.0, C - 1.15), (C + 6.9, C - 0.9), (C + 6.0, C - 0.62), (C + 0.6, C - 0.62)]
    M.plate(m, blade, C - 0.07, C + 0.07, "steel_light", bevel=0.05)
    M.rbox(m, (C + 0.6, C - 0.95, C - 0.085), (C + 4.5, C - 0.9, C + 0.085), "steel_dark", 0.0)      # fuller


def cap(m):
    M.lathe(m, "x", [(C, 0), (C, 0.45), (C + 0.1, 0.5), (C + 0.5, 0.5), (C + 0.62, 0.4), (C + 0.62, 0)], C, C, "tint", seg=24)


def barrel(m, x0, x1, r, flutes=0):
    M.cylinder(m, "x", x0, x1, C, C, r, "tint", seg=24)
    for k in range(flutes):
        M.rbox(m, (x0 + 0.4, C + r - 0.06, C - 0.07), (x1 - 0.4, C + r + 0.01, C + 0.07), "steel_dark", 0.02, xf=X(RX(360 / flutes * k)))
    bore(m, x1 + 0.01, r * 0.45)


def hg(m, mat, rails, length=8.0, hh=1.15, hw=1.25):
    x0, x1 = C - length / 2, C + length / 2
    M.rbox(m, (x0, C - hh, C - hw), (x1, C + hh, C + hw), mat, 0.45)
    if rails:
        rail_on(m, x0 + 0.2, x1 - 0.2, C + hh + 0.22, w=0.9)
        for k in range(7):                                   # M-LOK slots on the sides
            x = x0 + 0.7 + k * (length - 1.4) / 7
            for s in (-1, 1):
                M.rbox(m, (x, C - 0.35, C + s * hw - 0.03), (x + 0.6, C + 0.15, C + s * hw + 0.03), "bore", 0.05)
    else:
        for k in range(6):                                   # grip grooves
            x = x0 + 1.0 + k * (length - 2) / 6
            M.rbox(m, (x, C - hh * 0.7, C - hw - 0.02), (x + 0.12, C + hh * 0.4, C + hw + 0.02), "walnut_dark" if mat.startswith("walnut") else "steel_dark", 0.03)


def fg_vertical(m):
    M.lathe(m, "y", [(C - 3.7, 0), (C - 3.7, 0.5), (C - 3.5, 0.6), (C - 0.6, 0.55), (C - 0.3, 0.62), (C, 0.62), (C, 0)], C, C, "tint", seg=20)
    for k in range(8):
        y = C - 3.2 + k * 0.32
        M.lathe(m, "y", [(y, 0.585), (y, 0.62), (y + 0.12, 0.62), (y + 0.12, 0.585)], C, C, "rubber", seg=20)
    M.rbox(m, (C - 0.85, C - 0.35, C - 0.7), (C + 0.85, C, C + 0.7), "steel_dark", 0.08)


def fg_angled(m):
    M.rbox(m, (C - 1.8, C - 0.35, C - 0.62), (C + 1.4, C, C + 0.62), "steel_dark", 0.08)
    M.plate(m, [(C - 1.8, C - 0.35), (C + 1.4, C - 0.35), (C + 0.9, C - 1.9), (C - 0.4, C - 1.9)], C - 0.55, C + 0.55, "tint", bevel=0.18)


def fg_stubby(m):
    M.lathe(m, "y", [(C - 1.9, 0), (C - 1.9, 0.55), (C - 1.75, 0.62), (C - 0.3, 0.62), (C, 0.7), (C, 0)], C, C, "tint", seg=20)


def fg_stop(m):
    M.plate(m, [(C - 0.9, C), (C + 0.7, C), (C + 0.5, C - 0.9), (C - 0.4, C - 0.75)], C - 0.5, C + 0.5, "tint", bevel=0.15)


def opt_dot(m):
    """A micro red dot: a short round tube on a mount, a lens front and back, the dot in the middle."""
    mount_base(m, C - 1.2, C + 1.2)
    yc = C + 1.25
    M.cylinder(m, "x", C - 0.75, C + 0.85, yc, C, 0.62, "tint", seg=28, hollow=0.5)
    M.cylinder(m, "y", yc + 0.55, yc + 0.85, C + 0.05, C, 0.25, "tint", seg=16)                    # top turret
    M.cylinder(m, "z", C + 0.55, C + 0.85, C + 0.05, yc, 0.25, "tint", seg=16)                     # side turret
    M.rbox(m, (C - 0.75, C + 0.5, C - 0.4), (C + 0.85, yc - 0.45, C + 0.4), "tint", 0.1)
    lens(m, C + 0.82, yc, 0.5)
    dot(m, C + 0.05, yc, 0.045)


def opt_holo(m):
    """A holographic sight: a long base with the battery, a rectangular hood, a pane, and its ring-and-dot reticle."""
    M.rbox(m, (C - 2.0, C, C - 0.85), (C + 2.0, C + 0.8, C + 0.85), "tint", 0.2)
    M.rbox(m, (C - 1.8, C + 0.8, C - 0.6), (C - 0.3, C + 1.25, C + 0.6), "tint", 0.18)            # battery cap and buttons
    for k in range(2):
        M.rbox(m, (C - 1.6 + k * 0.6, C + 1.25, C - 0.2), (C - 1.3 + k * 0.6, C + 1.35, C + 0.2), "rubber", 0.04)
    yc = C + 1.6
    window(m, C + 0.1, C + 2.0, yc, 0.6, 0.55, wall=0.14)
    pane(m, C + 1.9, yc, 0.6, 0.55)
    ring_reticle(m, C + 1.0, yc, 0.24)
    dot(m, C + 1.0, yc, 0.035)


def opt_prism(m):
    """A 2x prism: a squat box body, a lens front and back, an illuminated chevron."""
    mount_base(m, C - 1.6, C + 1.6, w=1.4)
    yc = C + 1.45
    M.rbox(m, (C - 1.3, C + 0.55, C - 0.85), (C + 1.1, yc + 0.7, C + 0.85), "tint", 0.3)
    M.cylinder(m, "x", C + 1.1, C + 1.7, yc, C, 0.62, "tint", seg=28, hollow=0.52)
    M.cylinder(m, "x", C - 1.8, C - 1.3, yc, C, 0.55, "rubber", seg=28, hollow=0.45)
    M.cylinder(m, "z", C + 0.85, C + 1.15, C, yc + 0.1, 0.3, "steel_dark", seg=16)
    lens(m, C + 1.68, yc, 0.52)
    lens(m, C - 1.78, yc, 0.45)
    M.pipe(m, [(C - 1.0, yc - 0.12, C - 0.12), (C - 1.0, yc, C), (C - 1.0, yc - 0.12, C + 0.12)], 0.025, "red_dot", seg=4)


def opt_acog(m):
    """A 4x combat scope: a forged body, a ribbed tube, the fibre on top, a lens each end."""
    yc = C + 1.55
    M.rbox(m, (C - 2.2, C, C - 0.8), (C + 2.2, C + 0.55, C + 0.8), "steel_dark", 0.1)
    M.rbox(m, (C - 2.0, C + 0.55, C - 0.8), (C + 1.2, yc + 0.55, C + 0.8), "tint", 0.35)
    M.lathe(m, "x", [(C + 1.0, 0.6), (C + 1.6, 0.86), (C + 2.7, 0.86), (C + 2.7, 0.74), (C + 1.0, 0.5)], yc, C, "tint", seg=28)
    M.cylinder(m, "x", C - 2.6, C - 2.0, yc, C, 0.62, "rubber", seg=28, hollow=0.5)
    M.rbox(m, (C - 1.2, yc + 0.55, C - 0.35), (C + 0.8, yc + 0.8, C + 0.35), "polymer", 0.1)        # the fibre housing
    M.rbox(m, (C - 1.0, yc + 0.8, C - 0.08), (C + 0.6, yc + 0.84, C + 0.08), "glow_amber", 0.0)
    lens(m, C + 2.66, yc, 0.74)
    lens(m, C - 2.58, yc, 0.5)
    M.pipe(m, [(C, yc - 0.14, C - 0.14), (C, yc, C), (C, yc - 0.14, C + 0.14)], 0.022, "red_dot", seg=4)
    M.cylinder(m, "z", C - 1.0, C + 1.0, C - 1.0, C + 0.25, 0.14, "steel_light", seg=8)


def opt_irons(m):
    """Flip-up iron sights: a rear aperture and a front post, folded half up."""
    M.rbox(m, (C - 0.6, C, C - 0.55), (C + 0.6, C + 0.35, C + 0.55), "steel_dark", 0.08)
    rear = [(C - 0.5, C + 0.3), (C - 0.4, C + 1.45), (C + 0.4, C + 1.45), (C + 0.5, C + 0.3)]
    M.plate(m, [(z, y) for z, y in rear], C - 0.08, C + 0.08, "tint", bevel=0.04, xf=M.Xf(M.rot_matrix("y", 90), (C, C, C)))
    M.cylinder(m, "x", C - 0.12, C + 0.12, C + 1.05, C, 0.18, "bore", seg=12)                         # the aperture
    M.rbox(m, (C + 3.6, C, C - 0.45), (C + 4.4, C + 0.35, C + 0.45), "steel_dark", 0.08)
    M.rbox(m, (C + 3.92, C + 0.35, C - 0.07), (C + 4.08, C + 1.35, C + 0.07), "tint", 0.03)
    M.rbox(m, (C + 3.75, C + 0.35, C - 0.4), (C + 4.25, C + 1.2, C - 0.3), "tint", 0.03)                 # its ears
    M.rbox(m, (C + 3.75, C + 0.35, C + 0.3), (C + 4.25, C + 1.2, C + 0.4), "tint", 0.03)


def stock(m, cheek):
    """A stock: a skeletal frame back from the buffer tube, its butt pad, and a cheek riser."""
    M.cylinder(m, "x", C + 0.0, C + 3.5, C + 0.6, C, 0.42, "steel_dark", seg=20)                        # buffer tube
    M.rbox(m, (C - 0.9, C - 2.6, C - 1.0), (C + 0.0, C + 1.5, C + 1.0), "tint", 0.35)
    M.rbox(m, (C - 1.2, C - 2.8, C - 1.08), (C - 0.9, C + 1.7, C + 1.08), "rubber", 0.15)
    for k in range(5):
        M.rbox(m, (C - 1.22, C - 2.4 + k * 0.85, C - 1.0), (C - 1.18, C - 2.3 + k * 0.85, C + 1.0), "steel_dark", 0.0)
    M.plate(m, [(C + 0.0, C + 1.1), (C + 3.4, C + 0.9), (C + 3.4, C + 0.4), (C + 0.0, C - 1.6)], C - 0.75, C + 0.75, "tint", bevel=0.2)
    M.cylinder(m, "z", C - 0.8, C + 0.8, C + 1.4, C - 0.3, 0.22, "bore", seg=12)                         # sling slot
    if cheek:
        M.rbox(m, (C + 0.6, C + 1.3, C - 0.75), (C + 5.5, C + 2.2, C + 0.75), "polymer", 0.35)
        M.cylinder(m, "y", C + 0.7, C + 1.35, C + 1.5, C, 0.12, "steel_light", seg=8)
        M.cylinder(m, "y", C + 0.7, C + 1.35, C + 4.6, C, 0.12, "steel_light", seg=8)


def mag(m, length=3.0, curve=0.0, w=0.65):
    pts = []
    for k in range(7):
        t = k / 6
        pts.append((C + curve * t * t * 2.0, C - t * length))
    M.sweep(m, pts, w, 1.15, "tint", z=C, rad=0.3)
    bx = C + curve * 2.0
    M.rbox(m, (bx - 1.3, C - length - 0.45, C - w - 0.08), (bx + 1.3, C - length + 0.05, C + w + 0.08), "steel_dark", 0.15)
    for k in range(3):
        y = C - 0.8 - k * 0.7
        M.rbox(m, (C - 0.9 + curve * 0.5 * k, y, C + w - 0.02), (C + 0.9 + curve * 0.5 * k, y + 0.12, C + w + 0.02), "steel_dark", 0.02)
    M.rbox(m, (C - 0.6, C - 0.2, C - w * 0.6), (C + 0.6, C + 0.05, C + w * 0.6), "brass", 0.08)            # top round


def mag_drum(m):
    M.rbox(m, (C - 0.8, C - 1.3, C - 0.6), (C + 0.8, C + 0.05, C + 0.6), "tint", 0.15)
    M.lathe(m, "z", [(C - 1.6, 0), (C - 1.6, 2.1), (C - 1.45, 2.3), (C + 1.45, 2.3), (C + 1.6, 2.1), (C + 1.6, 0)], C, C - 3.4, "tint", seg=40)
    M.cylinder(m, "z", C + 1.55, C + 1.75, C, C - 3.4, 0.6, "steel_dark", seg=20)
    M.rbox(m, (C - 0.15, C - 3.5, C + 1.7), (C + 1.4, C - 3.3, C + 1.85), "steel_light", 0.05)          # the winding key
    for k in range(12):
        M.rbox(m, (C - 0.08, C - 3.4 + 2.3, C - 1.5), (C + 0.08, C - 3.4 + 2.36, C + 1.5), "steel_dark", 0.02, xf=X(M.rot_matrix("z", 30 * k), (C, C - 3.4, C)))


def mag_coupled(m):
    mag(m, 3.0, 0.15)
    # The second mag beside it (+z), clamped
    M.sweep(m, [(C, C - 0.3), (C + 0.3, C - 3.0)], 0.65, 1.15, "tint", z=C + 1.45, rad=0.3)
    for y in (C - 1.0, C - 2.0):
        M.rbox(m, (C - 1.25, y, C - 0.75), (C + 1.25, y + 0.5, C + 2.15), "polymer", 0.12)


def under_bipod(m):
    M.rbox(m, (C - 0.8, C - 0.8, C - 0.75), (C + 0.8, C, C + 0.75), "tint", 0.15)
    M.cylinder(m, "z", C - 0.9, C + 0.9, C, C - 0.55, 0.22, "steel_light", seg=12)
    for s in (-1, 1):
        M.pipe(m, [(C, C - 0.6, C + s * 0.55), (C + 0.6, C - 3.2, C + s * 1.6)], 0.2, "tint", seg=10)
        M.pipe(m, [(C + 0.6, C - 3.2, C + s * 1.6), (C + 0.75, C - 4.6, C + s * 2.0)], 0.13, "steel_light", seg=10)
        M.lathe(m, "y", [(C - 4.9, 0), (C - 4.9, 0.25), (C - 4.6, 0.25), (C - 4.5, 0)], C + 0.75, C + s * 2.0, "rubber", seg=12)


def under_launcher(m):
    M.cylinder(m, "x", C - 2.0, C + 3.5, C - 1.05, C, 0.85, "tint", seg=28, hollow=0.62)
    M.rbox(m, (C - 2.4, C - 0.4, C - 0.75), (C + 1.6, C, C + 0.75), "steel_dark", 0.1)
    M.plate(m, [(C - 2.2, C - 1.8), (C - 1.4, C - 1.8), (C - 1.7, C - 3.6), (C - 2.6, C - 3.4)], C - 0.45, C + 0.45, "polymer", bevel=0.18)
    M.rbox(m, (C - 1.5, C - 2.0, C - 0.08), (C - 1.1, C - 1.7, C + 0.08), "steel_light", 0.03)
    for k in range(4):
        M.rbox(m, (C - 1.6 + k * 1.1, C - 1.95, C - 0.88), (C - 1.3 + k * 1.1, C - 0.2, C + 0.88), "steel_dark", 0.05)


def under_shotgun(m):
    M.cylinder(m, "x", C - 2.0, C + 3.4, C - 0.85, C, 0.55, "tint", seg=24)
    bore(m, C + 3.41, 0.32)
    M.cylinder(m, "x", C - 1.6, C + 2.6, C - 1.75, C, 0.45, "steel_dark", seg=20)                        # the magazine tube
    M.rbox(m, (C - 1.0, C - 2.4, C - 0.65), (C + 1.6, C - 1.2, C + 0.65), "polymer", 0.25)               # pump
    for k in range(6):
        M.rbox(m, (C - 0.8 + k * 0.4, C - 2.42, C - 0.66), (C - 0.62 + k * 0.4, C - 1.2, C + 0.66), "rubber", 0.03)
    M.rbox(m, (C - 2.4, C - 0.45, C - 0.6), (C + 1.2, C, C + 0.6), "steel_dark", 0.08)


def under_cover(m):
    M.rbox(m, (C - 2.5, C - 0.45, C - 0.8), (C + 2.5, C, C + 0.8), "rubber", 0.18)
    for k in range(8):
        M.rbox(m, (C - 2.3 + k * 0.6, C - 0.5, C - 0.82), (C - 2.05 + k * 0.6, C - 0.4, C + 0.82), "polymer", 0.03)


def tac_light(m, laser=False, light=True):
    M.rbox(m, (C - 0.9, C - 0.4, C), (C + 0.9, C + 0.4, C + 0.55), "steel_dark", 0.1)                   # rail clamp
    zc = C + 1.1
    if light:
        M.lathe(m, "x", [(C - 1.6, 0), (C - 1.6, 0.4), (C - 1.4, 0.48), (C + 0.8, 0.48), (C + 1.3, 0.66), (C + 1.9, 0.66), (C + 1.9, 0)],
                C, zc, "tint", seg=28)
        M.lathe(m, "x", [(C + 1.91, 0), (C + 1.91, 0.54)], C, zc, "glow_white", seg=24)
        for k in range(6):
            M.rbox(m, (C + 1.35, C + 0.62, zc - 0.05), (C + 1.85, C + 0.7, zc + 0.05), "steel_dark", 0.02, xf=M.Xf(RX(60 * k), (C, C, zc)))
        M.cylinder(m, "x", C - 1.75, C - 1.55, C, zc, 0.3, "rubber", seg=16)                            # the tail button
    if laser:
        zl = zc if not light else zc + 0.75
        yl = C - 0.15 if light else C
        M.rbox(m, (C - 0.9, yl - 0.35, zl - 0.38), (C + 1.1, yl + 0.35, zl + 0.38), "polymer", 0.12)
        M.cylinder(m, "x", C + 1.1, C + 1.35, yl, zl, 0.2, "steel_dark", seg=16)
        M.lathe(m, "x", [(C + 1.36, 0), (C + 1.36, 0.1)], yl, zl, "red_dot", seg=12)


def grip_wrap(m):
    for k in range(9):
        M.lathe(m, "y", [(C - 2.6 + k * 0.32, 0.58), (C - 2.6 + k * 0.32, 0.68), (C - 2.36 + k * 0.32, 0.68), (C - 2.36 + k * 0.32, 0.58)],
                C, C, "tape" if k % 2 else "rubber", seg=16)


def charm(m):
    M.pipe(m, [(C, C, C), (C + 0.1, C - 0.7, C + 0.05), (C + 0.05, C - 1.2, C)], 0.04, "steel_light", seg=6)
    M.lathe(m, "y", [(C - 1.25, 0.0), (C - 1.25, 0.12), (C - 1.2, 0.16), (C - 1.15, 0.12), (C - 1.15, 0.0)], C + 0.05, C, "steel_light", seg=12)
    M.plate(m, [(C - 0.4, C - 1.3), (C + 0.4, C - 1.3), (C + 0.45, C - 2.2), (C, C - 2.5), (C - 0.45, C - 2.2)], C - 0.06, C + 0.06, "tint", bevel=0.05)


# ---- The cursed ones: still built properly ----
def cursed_box(m):
    M.rbox(m, (C - 1.5, C - 1.6, C - 1.5), (C + 1.5, C + 1.2, C + 1.5), "cerakote_red", 0.2)
    M.rbox(m, (C - 1.55, C + 1.0, C - 1.55), (C + 1.55, C + 1.25, C + 1.55), "steel_dark", 0.08)
    for s in (-1, 1):
        M.pipe(m, [(C - 0.6, C + 1.25, C + s * 0.9), (C - 0.6, C + 1.7, C + s * 0.9), (C + 0.6, C + 1.7, C + s * 0.9), (C + 0.6, C + 1.25, C + s * 0.9)], 0.07, "steel_light", seg=6)
    M.rbox(m, (C - 0.2, C - 0.4, C + 1.5), (C + 0.2, C + 0.4, C + 1.56), "brass", 0.04)


def cursed_drum(m):
    M.lathe(m, "z", [(C - 2.2, 0), (C - 2.2, 3.2), (C - 2.0, 3.4), (C + 2.0, 3.4), (C + 2.2, 3.2), (C + 2.2, 0)], C, C - 4.5, "tint", seg=48)
    for k in range(16):
        M.rbox(m, (C - 0.12, C - 4.5 + 3.3, C - 2.0), (C + 0.12, C - 4.5 + 3.45, C + 2.0), "brass", 0.03, xf=X(M.rot_matrix("z", 22.5 * k), (C, C - 4.5, C)))
    M.rbox(m, (C - 0.8, C - 1.3, C - 0.6), (C + 0.8, C + 0.05, C + 0.6), "steel_dark", 0.12)


def cursed_scope(m):
    tube_scope(m, 22.0, 0.8, 2.2, turrets=True)


def cursed_barrel(m, wide=False):
    r = 1.4 if wide else 0.55
    L = 14.0 if not wide else 6.0
    M.cylinder(m, "x", C, C + L, C, C, r, "tint", seg=32, hollow=r * 0.55 if wide else 0)
    for k in range(int(L / 1.5)):
        M.cylinder(m, "x", C + 0.5 + k * 1.5, C + 0.8 + k * 1.5, C, C, r + 0.1, "steel_dark", seg=32)
    if not wide:
        bore(m, C + L + 0.01, 0.28)
    else:
        M.lathe(m, "x", [(C + L, r), (C + L + 1.2, r * 1.6), (C + L + 1.3, r * 1.6), (C + L + 0.2, r * 0.55)], C, C, "tint", seg=32)


def cursed_light(m):
    M.rbox(m, (C - 0.9, C - 0.5, C), (C + 0.9, C + 0.5, C + 0.6), "steel_dark", 0.1)
    zc = C + 2.4
    M.lathe(m, "x", [(C - 2.5, 0), (C - 2.5, 1.2), (C + 0.5, 1.2), (C + 1.6, 2.2), (C + 2.0, 2.2), (C + 2.0, 0)], C, zc, "tint", seg=40)
    M.lathe(m, "x", [(C + 2.01, 0), (C + 2.01, 2.0)], C, zc, "glow_white", seg=36)
    for k in range(10):
        M.rbox(m, (C - 2.3 + k * 0.3, C + 1.15, zc - 1.25), (C - 2.15 + k * 0.3, C + 1.3, zc + 1.25), "steel_dark", 0.03)


def cursed_coil(m):
    pts = [(C + 0.3 * t, C + 0.9 * math.sin(t * 1.3), C + 0.9 * math.cos(t * 1.3)) for t in [k * 0.25 for k in range(60)]]
    M.pipe(m, pts, 0.14, "copper", seg=8, caps=True)
    M.cylinder(m, "x", C, C + 18 * 0.25 * 0.3 * 4, C, C, 0.35, "steel_dark", seg=16)


def cursed_jet(m):
    M.lathe(m, "x", [(C - 3.0, 0), (C - 3.0, 0.9), (C - 2.0, 1.2), (C + 1.5, 1.2), (C + 2.2, 0.9), (C + 2.2, 0)], C, C + 2.4, "steel_light", seg=32)
    M.lathe(m, "x", [(C - 3.01, 0), (C - 3.01, 0.8)], C, C + 2.4, "glow_amber", seg=28)
    M.rbox(m, (C - 1.0, C - 0.4, C), (C + 1.0, C + 0.4, C + 1.3), "steel_dark", 0.1)


def cursed_saw(m):
    M.rbox(m, (C - 1.0, C - 1.0, C - 0.8), (C + 4.0, C + 0.6, C + 0.8), "cerakote_red", 0.3)
    bar = [(C + 3.5, C - 0.2), (C + 12.0, C - 0.2), (C + 12.6, C - 0.6), (C + 12.0, C - 1.0), (C + 3.5, C - 1.0)]
    M.plate(m, bar, C - 0.1, C + 0.1, "steel_light", bevel=0.05)
    for k in range(26):
        x = C + 3.6 + k * 0.34
        M.rbox(m, (x, C - 0.15, C - 0.13), (x + 0.2, C + 0.05, C + 0.13), "steel_dark", 0.02)
        M.rbox(m, (x, C - 1.25, C - 0.13), (x + 0.2, C - 1.05, C + 0.13), "steel_dark", 0.02)


def cursed_stack(m):
    for k in range(4):
        M.cylinder(m, "x", C - 2.0, C + 3.5, C - 0.9 - k * 0.95, C, 0.42, "tint" if k % 2 == 0 else "steel_dark", seg=20)
        bore(m, C + 3.51, 0.22, y=C - 0.9 - k * 0.95)
    M.rbox(m, (C - 2.2, C - 4.4, C - 0.6), (C - 1.6, C - 0.3, C + 0.6), "steel_dark", 0.1)


def cursed_duck(m):
    yc = C + 1.6
    M.lathe(m, "y", [(C, 0), (C, 1.0), (C + 0.6, 1.3), (C + 1.6, 1.1), (C + 2.2, 0.0)], C, C, "polymer_sand", seg=24)
    M.lathe(m, "y", [(C + 1.8, 0), (C + 2.0, 0.65), (C + 2.8, 0.75), (C + 3.4, 0.0)], C + 0.7, C, "polymer_sand", seg=24)
    M.rbox(m, (C + 1.35, C + 2.55, C - 0.3), (C + 2.1, C + 2.75, C + 0.3), "paint_red", 0.08)
    for s in (-1, 1):
        M.lathe(m, "x", [(C + 1.05, 0), (C + 1.05, 0.12), (C + 1.1, 0.0)], C + 2.9, C + s * 0.45, "bore", seg=10)


def cursed_toaster(m):
    M.rbox(m, (C - 1.8, C, C - 1.3), (C + 1.8, C + 2.4, C + 1.3), "chrome", 0.45)
    for s in (-0.5, 0.5):
        M.rbox(m, (C - 1.3, C + 2.38, C + s - 0.18), (C + 1.3, C + 2.42, C + s + 0.18), "bore", 0.03)
        M.rbox(m, (C - 1.0, C + 2.42, C + s - 0.12), (C + 1.0, C + 2.9, C + s + 0.12), "beech", 0.08)
    M.rbox(m, (C + 1.8, C + 1.2, C - 0.15), (C + 1.95, C + 1.8, C + 0.15), "polymer", 0.04)


def cursed_bipod(m):
    M.rbox(m, (C - 0.7, C - 0.8, C - 0.7), (C + 0.7, C, C + 0.7), "tint", 0.15)
    for s in (-1, 1):
        M.pipe(m, [(C, C - 0.6, C + s * 0.4), (C + 2, C - 30, C + s * 9)], 0.22, "tint", seg=10)
        M.lathe(m, "y", [(C - 30.3, 0), (C - 30.3, 0.4), (C - 29.9, 0.4), (C - 29.8, 0)], C + 2, C + s * 9, "rubber", seg=12)


def none(m):
    pass


SHAPES = {
    "comp": comp, "brake": brake, "flash": flash,
    "supp_short": lambda m: can(m, 4.0, 0.8), "supp_long": lambda m: can(m, 6.5, 0.85, rings=4), "supp_fat": lambda m: can(m, 6.0, 1.25, rings=4),
    "choke": choke, "bayonet": bayonet, "cap": cap,
    "barrel_short": lambda m: barrel(m, C - 2, C, 0.62), "barrel_long": lambda m: barrel(m, C, C + 4.0, 0.5),
    "barrel_heavy": lambda m: barrel(m, C, C + 3.0, 0.78), "barrel_fluted": lambda m: barrel(m, C - 3, C + 1.5, 0.65, flutes=6),
    "hg_rail": lambda m: hg(m, "tint", True), "hg_slim": lambda m: hg(m, "tint", True, hh=1.05, hw=1.1),
    "hg_wood": lambda m: hg(m, "walnut", False),
    "fg_vertical": fg_vertical, "fg_angled": fg_angled, "fg_stubby": fg_stubby, "fg_stop": fg_stop,
    "opt_dot": opt_dot, "opt_holo": opt_holo, "opt_prism": opt_prism, "opt_acog": opt_acog,
    "opt_scope": lambda m: tube_scope(m, 9.0, 0.62, 1.3), "opt_scope_big": lambda m: tube_scope(m, 11.5, 0.75, 1.5),
    "opt_irons": opt_irons,
    "stock_pad": lambda m: stock(m, False), "stock_cheek": lambda m: stock(m, True),
    "mag_ext": lambda m: mag(m, 3.4, 0.1), "mag_short": lambda m: mag(m, 1.8, 0.05), "mag_drum": mag_drum, "mag_coupled": mag_coupled,
    "under_bipod": under_bipod, "under_launcher": under_launcher, "under_shotgun": under_shotgun, "under_cover": under_cover,
    "tac_laser": lambda m: tac_light(m, laser=True, light=False), "tac_light": lambda m: tac_light(m), "tac_combo": lambda m: tac_light(m, laser=True),
    "grip_wrap": grip_wrap, "charm": charm,
    "cursed_box": cursed_box, "cursed_drum": cursed_drum, "cursed_scope": cursed_scope, "cursed_barrel": cursed_barrel,
    "cursed_barrel_wide": lambda m: cursed_barrel(m, True), "cursed_light": cursed_light, "cursed_coil": cursed_coil, "cursed_jet": cursed_jet,
    "cursed_saw": cursed_saw, "cursed_stack": cursed_stack, "cursed_duck": cursed_duck, "cursed_toaster": cursed_toaster,
    "cursed_bipod": cursed_bipod, "none": none,
}


def main(only=None):
    os.makedirs(OUT, exist_ok=True)
    n = 0
    for name, make in SHAPES.items():
        if only and name not in only:
            continue
        m = M.Mesh()
        make(m)
        if not m.quads:
            continue
        m.write(os.path.join(OUT, name + ".fam"))
        n += 1
    print("attachments: %d smooth meshes" % n)


if __name__ == "__main__":
    main(sys.argv[1:] or None)
