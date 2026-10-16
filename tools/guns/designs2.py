"""The second wave of guns (v25): built from parts.py's smooth parts, much more detailed than designs.py's.
ALL maps gun id -> (builder, first-person scale, eye relief), like designs.ALL.
Each builder sets m.sight (the sight line's front point) and m.stock_rear (the back of the gun at cheek height)."""
import math

import parts as P
from parts import Z, zs
from mesh import spline


# ===============================================================================================================
# AR family: HK416, MCX, (and the receivers of others)
# ===============================================================================================================
def ar_lower(m, finish="alu", furniture="polymer", w=1.8, mag_x=7.5):
    z0, z1 = zs(w)
    outline = [(11.6, 8.2), (0.2, 8.2), (0.0, 7.7), (0.0, 6.9), (0.6, 6.35), (2.4, 6.2), (2.7, 5.75), (4.9, 5.75), (5.2, 6.05),
               (mag_x - 0.2, 6.05), (mag_x - 0.1, 5.2), (mag_x - 0.25, 4.65), (mag_x - 0.05, 4.3), (mag_x + 3.25, 4.3),
               (mag_x + 3.4, 4.65), (mag_x + 3.25, 5.6), (11.1, 6.4), (11.8, 7.2)]
    m.plate(outline, z0, z1, finish, bevel=0.22, corner=0.25)
    # The mag well's flared lip, the front pivot lug, the trigger / hammer / takedown pins
    m.box((mag_x - 0.3, 4.2, z0 - 0.1), (mag_x + 3.5, 4.55, z1 + 0.1), finish, r=0.12)
    m.box((10.7, 6.9, Z - 0.5), (11.95, 8.1, Z + 0.5), finish, r=0.2)
    P.pin(m, 11.3, 7.55, z0, z1)
    P.pin(m, 1.2, 7.6, z0, z1)
    P.pin(m, 5.7, 6.9, z0, z1, r=0.12)
    P.pin(m, 6.8, 6.9, z0, z1, r=0.12)
    # Selector (left), bolt catch, mag release (right), markings
    m.plate([(2.4, 7.25), (4.2, 7.4), (4.2, 6.9), (2.4, 6.7)], z0 - 0.22, z0, "steel_dark", bevel=0.06)
    P.screw(m, 2.75, 7.0, z0 - 0.22, -1, r=0.22, tex="steel_dark")
    m.plate([(6.5, 6.9), (8.1, 6.9), (7.9, 7.6), (6.7, 7.6)], z0 - 0.25, z0, "steel_dark", bevel=0.06)
    m.cyl("z", z1, z1 + 0.32, 10.2, 6.3, 0.28, "steel_dark")
    m.lathe("z", [(z1 + 0.32, 0), (z1 + 0.32, 0.36), (z1 + 0.36, 0.3), (z1 + 0.36, 0)], 10.2, 6.3, "steel_dark", icon=False)
    m.box((2.0, 6.5, z1), (3.6, 7.5, z1 + 0.02), "steel_light", faces=["south"], mesh_only=True)
    # Trigger guard, trigger, grip
    P.trigger_guard(m, 4.95, 7.45, 6.05, depth=1.45, r=0.2, tex=finish)
    P.grip(m, 4.75, 5.95, length=4.8, rake=24, w=1.45, depth=2.2, tex=furniture, stipple="grip_check" if furniture.startswith("polymer") else None)
    # Buffer tower
    m.box((-0.6, 6.1, Z - 0.75), (0.25, 8.2, Z + 0.75), finish, r=0.2)


def ar_upper(m, finish="alu", length=12.0, w=2.0, fa=True):
    z0, z1 = zs(w)
    m.box((0.0, 8.2, z0), (length, 10.35, z1), finish, r=0.25)
    m.box((0.2, 8.15, z0 - 0.06), (length - 0.2, 8.3, z1 + 0.06), finish, r=0.05)              # where upper meets lower
    P.rail(m, 0.25, length - 0.05, 10.3, w=1.6)
    # Ejection port (open), the bolt carrier inside, the brass deflector, dust cover hanging open
    m.plate([(4.0, 8.75), (7.7, 8.75), (7.7, 9.85), (4.0, 9.85)], z1 - 0.06, z1 + 0.01, "bore", bevel=0.0, icon=False, corner=0.12)
    m.box((4.25, 8.95, z1 - 0.3), (7.45, 9.65, z1 + 0.04), "steel_light", r=0.12, mesh_only=True)                       # bolt carrier
    m.cyl("x", 6.6, 7.4, 9.3, z1 - 0.05, 0.16, "steel_dark", mesh_only=True)
    m.box((3.9, 7.95, z1 + 0.02), (7.8, 8.5, z1 + 0.15), finish, r=0.05)
    m.plate([(2.9, 8.6), (3.9, 8.6), (3.9, 10.1), (3.2, 10.1)], z1 - 0.1, z1 + 0.6, finish, bevel=0.12)
    if fa:
        m.cyl("z", z1 - 0.3, z1 + 0.85, 1.6, 9.45, 0.45, finish)
        m.cyl("z", z1 + 0.85, z1 + 1.0, 1.6, 9.45, 0.45, "knurl")
    # Charging handle
    m.plate([(-0.9, 9.85), (0.3, 9.85), (0.3, 10.3), (-0.9, 10.3)], Z - 0.7, Z + 0.7, finish, bevel=0.1)
    m.plate([(-0.9, 9.85), (-0.2, 9.85), (-0.2, 10.3), (-0.9, 10.3)], Z - 1.35, Z - 0.7, finish, bevel=0.1)


def ar_stock(m, kind="ctr", furniture="polymer"):
    """Buffer tube and a stock: ctr (a modern adjustable one), crane, fixed (A2 rifle), none."""
    m.lathe("x", [(-6.9, 0), (-6.9, 0.6), (-6.7, 0.66), (0.0, 0.66), (0.0, 0)], 8.95, Z, "alu")
    m.lathe("x", [(-0.6, 0.66), (-0.6, 0.95), (0.0, 0.95), (0.0, 0.66)], 8.95, Z, "steel_dark")          # castle nut
    m.box((-0.7, 7.9, Z - 0.95), (-0.55, 9.8, Z + 0.95), "steel_dark", r=0.05)                            # end plate
    P.sling_loop(m, -0.2, 7.9, Z + 1.1, r=0.35, axis="x")
    if kind == "none":
        m.stock_rear = -6.9
        return
    if kind == "fixed":
        P.stock_loft(m, [(-6.6, 10.0), (-10.0, 10.1), (-14.0, 10.2)], [(-6.6, 7.8), (-10.0, 6.7), (-14.0, 4.6)],
                     [(-14.0, 1.9), (-6.6, 1.7)], furniture)
        P.butt_pad(m, -14.0, 4.5, 10.3, 2.0)
        m.stock_rear = -14.5
        return
    # An adjustable stock: the body around the tube, a cheek riser, the toe, a thick pad, the release lever, sling points
    top = 10.15 if kind == "ctr" else 10.4
    m.plate([(-12.4, top), (-7.0, top - 0.1), (-6.4, 9.3), (-6.4, 7.9), (-8.4, 7.5), (-10.4, 6.2), (-12.4, 5.3)], Z - 0.95, Z + 0.95, furniture, bevel=0.32)
    if kind == "crane":
        m.lathe("x", [(-12.0, 0), (-12.0, 0.75), (-7.5, 0.75), (-7.5, 0)], 7.3, Z + 0.6, furniture, icon=False)   # battery tubes
        m.lathe("x", [(-12.0, 0), (-12.0, 0.75), (-7.5, 0.75), (-7.5, 0)], 7.3, Z - 0.6, furniture, icon=False)
    m.plate([(-11.6, 6.4), (-9.0, 7.2), (-9.0, 6.8), (-11.0, 6.0)], Z - 0.1, Z + 0.1, "bore", bevel=0.0, icon=False)   # lightening cut
    P.butt_pad(m, -12.4, 5.2, top + 0.05, 2.0, t=0.65)
    m.box((-9.6, 7.6, Z - 0.35), (-8.8, 8.0, Z + 0.35), "steel_dark", r=0.1)
    P.sling_loop(m, -11.6, 9.0, Z + 1.0, r=0.4, axis="x")
    m.stock_rear = -13.0


def hk416(m):
    ar_lower(m, "alu", "polymer")
    ar_upper(m, "alu", fa=True)
    ar_stock(m, "ctr")
    # Piston barrel: heavier profile, the gas block under the rail, the long quad rail
    P.barrel(m, 12.0, 29.0, 9.2, 0.58, steps=[(21.5, 0.58), (22.0, 0.5)])
    P.quad_rail(m, 12.0, 21.8, 9.15, 1.35, 1.3, "alu")
    m.box((11.8, 7.7, Z - 1.35), (12.4, 10.5, Z + 1.35), "alu", r=0.2)                            # barrel nut
    m.lathe("x", [(22.4, 0.5), (22.4, 0.8), (23.6, 0.8), (23.6, 0.5)], 9.2, Z, "steel_dark")       # gas block
    P.flash_hider(m, 29.0, 9.2, r=0.62, L=2.0, prongs=4)
    # Flip-up irons on the rail (up), the 416's markings plate
    front = P.iron_post(m, 20.8, 10.5, h=1.3)
    P.iron_rear(m, 1.2, 10.55, h=1.3)
    m.sight = (20.8, front - 0.1)
    m.mag(7.6, 4.6, 2.85, 6.0, Z - 0.75, Z + 0.75, "polymer_fde", curve=1, base="polymer_black", ribs=4, rib_tex="polymer_fde")


def mcx(m):
    ar_lower(m, "alu_tan", "polymer_fde")
    ar_upper(m, "alu_tan", fa=False)
    ar_stock(m, "crane", "polymer_fde")
    P.barrel(m, 12.0, 21.0, 9.2, 0.5)
    P.mlok(m, 12.0, 22.5, 9.15, 1.2, 1.15, "alu_tan")
    m.box((11.8, 7.8, Z - 1.2), (12.4, 10.4, Z + 1.2), "alu_tan", r=0.2)
    P.suppressor(m, 21.0, 9.2, r=1.0, L=7.6, tex="polymer_black")
    cy = P.holo(m, 4.5, 10.75)
    m.sight = (6.0, cy)
    m.mag(7.6, 4.6, 2.85, 5.6, Z - 0.75, Z + 0.75, "polymer_black", curve=1, base="polymer_black", ribs=3)


# ===============================================================================================================
# Pistols
# ===============================================================================================================
def pistol2(m, slide_len=11.4, slide_h=2.2, w=1.45, slide_tex="steel_dark", frame_tex="polymer_black", grip_tex=None, grip_stipple="grip_check",
            hammer=False, rail=True, comp=False, barrel_out=0.0, front_serr=True, beavertail=False, decock=False, mag_ext=0.0, auto_switch=False,
            slide_cut=False, grip_len=5.0, rake=18):
    x0, x1 = 1.0, 1.0 + slide_len
    z0, z1 = zs(w)
    yb, yt = 8.0, 8.0 + slide_h
    # Slide: a profile with a chamfered top, front taper, cocking serrations cut in (dark slots)
    outline = [(x0, yb), (x1, yb), (x1, yt - 0.6), (x1 - 0.5, yt), (x0 + 0.3, yt), (x0, yt - 0.35)]
    m.plate(outline, z0, z1, slide_tex, bevel=0.3)
    for k in range(8):
        x = x0 + 0.5 + k * 0.32
        for zz, s in ((z1, 1), (z0, -1)):
            m.box((x, yb + 0.4, zz - 0.04 if s > 0 else zz - 0.01), (x + 0.12, yt - 0.35, zz + 0.01 if s > 0 else zz + 0.04), "bore",
                  faces=["south" if s > 0 else "north"], mesh_only=True)
    if front_serr:
        for k in range(5):
            x = x1 - 2.6 + k * 0.32
            for zz, s in ((z1, 1), (z0, -1)):
                m.box((x, yb + 0.4, zz - 0.04 if s > 0 else zz - 0.01), (x + 0.12, yt - 0.45, zz + 0.01 if s > 0 else zz + 0.04), "bore",
                      faces=["south" if s > 0 else "north"], mesh_only=True)
    if slide_cut:
        m.plate([(x0 + 6.0, yt - 0.3), (x1 - 1.0, yt - 0.3), (x1 - 1.0, yt + 0.01), (x0 + 6.0, yt + 0.01)], Z - 0.35, Z + 0.35, "bore", bevel=0.0, icon=False)
    # Ejection port with the barrel hood showing
    m.plate([(x0 + 4.1, yb + 1.1), (x0 + 6.8, yb + 1.1), (x0 + 6.8, yt - 0.1), (x0 + 4.1, yt - 0.1)], z1 - 0.04, z1 + 0.01, "bore", bevel=0.0, icon=False, corner=0.1)
    m.box((x0 + 4.25, yb + 1.25, z1 - 0.4), (x0 + 6.65, yt - 0.2, z1 + 0.03), "steel_light", r=0.12, mesh_only=True)       # barrel hood
    m.plate([(x0 + 4.1, yt - 0.02), (x0 + 6.8, yt - 0.02), (x0 + 6.8, yt + 0.01), (x0 + 4.1, yt + 0.01)], Z - 0.1, z1 + 0.01, "bore", bevel=0.0, icon=False, corner=0)
    # Sights: a rear notch with dots, a front post with a dot
    m.plate([(x0 + 0.3, yt), (x0 + 1.2, yt), (x0 + 1.1, yt + 0.75), (x0 + 0.35, yt + 0.75)], Z - 0.6, Z + 0.6, "steel_dark", bevel=0.08)
    m.box((x0 + 0.25, yt + 0.4, Z - 0.17), (x0 + 1.25, yt + 0.8, Z + 0.17), "bore", mesh_only=True)
    for s in (-1, 1):
        m.cyl("x", x0 + 0.27, x0 + 0.31, yt + 0.5, Z + s * 0.38, 0.08, "glow_green", mesh_only=True)
    m.plate([(x1 - 1.1, yt), (x1 - 0.5, yt), (x1 - 0.55, yt + 0.85), (x1 - 0.95, yt + 0.85)], Z - 0.17, Z + 0.17, "steel_dark", bevel=0.06)
    m.cyl("x", x1 - 1.13, x1 - 1.08, yt + 0.62, Z, 0.08, "glow_green", mesh_only=True)
    m.sight = (x1 - 0.75, yt + 0.82)
    # The barrel's crown at the muzzle
    m.lathe("x", [(x1 - 0.3, 0.0), (x1 - 0.3, 0.2), (x1 - 0.02, 0.2), (x1 - 0.02, 0.45), (x1 + barrel_out, 0.45), (x1 + barrel_out, 0)],
            yb + 1.3, Z, "steel_dark", mats=["bore", "bore", "bore", "steel_dark", "steel_dark"], icon=False)
    if comp:
        m.box((x1, yb + 0.2, z0), (x1 + 2.0, yt - 0.2, z1), slide_tex, r=0.3)
        for k in range(3):
            m.box((x1 + 0.4 + k * 0.5, yt - 0.25, Z - 0.3), (x1 + 0.65 + k * 0.5, yt - 0.18, Z + 0.3), "bore", faces=["up"], mesh_only=True)
    # Frame: dust cover with a rail, trigger guard (squared front), grip, beavertail, mag base
    fz0, fz1 = zs(w - 0.12)
    frame = [(x1 - 0.5, yb), (x0 + 0.6, yb), (x0 + 0.3, yb - 0.4), (x0 + 0.9, yb - 1.5), (x0 + 4.0, yb - 1.55), (x1 - 0.5, yb - 1.4)]
    m.plate(frame, fz0, fz1, frame_tex, bevel=0.25)
    if rail:
        for k in range(3):
            x = x1 - 4.2 + k * 1.15
            m.box((x, yb - 1.8, Z - 0.6), (x + 0.6, yb - 1.4, Z + 0.6), frame_tex, r=0.08)
    P.trigger_guard(m, x0 + 4.1, x0 + 7.3, yb - 1.45, depth=1.75, r=0.24, tex=frame_tex)
    gx = x0 + 3.6
    end = P.grip(m, gx, yb - 1.2, length=grip_len, rake=rake, w=w - 0.05, depth=2.9, tex=grip_tex or frame_tex, grooves=3 if not grip_tex else 0,
                 swell=0.12, stipple=grip_stipple, base="polymer_black" if frame_tex.startswith("polymer") else "steel_dark")
    if mag_ext:
        m.box((end[0] - 1.2, end[1] - mag_ext, Z - 0.6), (end[0] + 1.0, end[1] - 0.2, Z + 0.6), "polymer_black", r=0.2)
    if beavertail:
        m.plate([(x0 - 0.6, yb - 0.2), (x0 + 1.0, yb - 0.2), (x0 + 1.0, yb - 1.1), (x0 + 0.2, yb - 1.0)], Z - 0.55, Z + 0.55, frame_tex, bevel=0.15)
    # Slide stop (left), mag release, takedown
    m.plate([(x0 + 4.5, yb - 0.6), (x0 + 7.2, yb - 0.55), (x0 + 7.2, yb - 0.2), (x0 + 4.7, yb - 0.25)], z0 - 0.2, z0, "steel_dark", bevel=0.05)
    m.cyl("z", z1 - 0.05, z1 + 0.18, x0 + 3.6, yb - 1.1, 0.25, "steel_dark")
    m.box((x0 + 7.6, yb - 0.6, z1 - 0.05), (x0 + 8.1, yb - 0.25, z1 + 0.12), "steel_dark", r=0.05)
    if auto_switch:
        m.plate([(x0 + 0.1, yt - 0.7), (x0 + 0.9, yt - 0.7), (x0 + 0.9, yt - 1.4), (x0 + 0.1, yt - 1.4)], z1, z1 + 0.25, "steel_light", bevel=0.05)
    if decock:
        m.cyl("z", z0 - 0.3, z1 + 0.3, x0 + 0.5, yt - 0.6, 0.18, "steel_dark")
    if hammer:
        m.plate([(x0 - 0.9, yt + 0.25), (x0 - 0.3, yt + 0.45), (x0 + 0.3, yt - 0.9), (x0 - 0.2, yt - 1.1)], Z - 0.25, Z + 0.25, "steel_dark", bevel=0.08)
    m.stock_rear = x0 - 1.0


def glock18(m):
    pistol2(m, slide_len=11.0, auto_switch=True, mag_ext=1.4)


def m9(m):
    pistol2(m, slide_len=12.8, slide_h=2.0, w=1.35, slide_tex="blued", frame_tex="alu", grip_tex="polymer_black", grip_stipple="grip_check",
            hammer=True, rail=False, front_serr=False, decock=True, slide_cut=True)


# ===============================================================================================================
# Pump shotguns
# ===============================================================================================================
def pump(m, barrel_end=30.0, tube_end=25.0, stock="wood", wood="walnut", finish="blued", forend_x=15.0, ghost=False, rail=False, shells=4):
    z0, z1 = zs(1.7)
    # Receiver: a long rounded slab, loading port underneath, ejection port right
    rec = [(0.0, 6.6), (10.5, 6.6), (11.0, 7.2), (11.0, 10.0), (10.4, 10.5), (0.6, 10.5), (0.0, 9.9)]
    m.plate(rec, z0, z1, finish, bevel=0.3)
    m.plate([(4.0, 8.7), (8.4, 8.7), (8.4, 10.0), (4.0, 10.0)], z1 - 0.05, z1 + 0.01, "bore", bevel=0.0, icon=False)
    m.box((4.3, 8.9, Z - 0.6), (8.1, 9.8, z1 - 0.02), "steel_light", r=0.08, mesh_only=True)
    m.box((3.5, 6.5, Z - 0.6), (9.0, 6.65, Z + 0.6), "bore", faces=["down"], mesh_only=True)
    # Safety button, slide release
    m.cyl("z", z0 - 0.15, z1 + 0.15, 1.3, 9.9, 0.2, "steel_dark")
    m.cyl("z", z0 - 0.15, z0 + 0.1, 3.6, 7.0, 0.26, "steel_dark")
    if rail:
        P.rail(m, 1.0, 10.0, 10.45, w=1.5)
    # Barrel with a bead (or ghost ring irons), the magazine tube below with its cap
    P.barrel(m, 10.8, barrel_end, 9.6, 0.62, bore=0.4)
    m.lathe("x", [(10.8, 0), (10.8, 0.6), (tube_end, 0.6), (tube_end + 0.2, 0.55), (tube_end + 0.2, 0)], 8.05, Z, finish)
    m.lathe("x", [(tube_end - 0.1, 0), (tube_end - 0.1, 0.7), (tube_end + 0.9, 0.7), (tube_end + 1.2, 0.5), (tube_end + 1.2, 0)], 8.05, Z, finish)
    m.box((tube_end - 0.3, 8.0, Z - 0.35), (tube_end + 0.5, 9.4, Z + 0.35), finish, r=0.12)                 # barrel clamp lug
    if ghost:
        front = P.iron_post(m, barrel_end - 1.0, 10.15, h=1.0)
        P.iron_rear(m, 2.0, 10.5, h=1.1)
        m.sight = (barrel_end - 1.0, front - 0.1)
    else:
        m.lathe("y", [(10.15, 0), (10.15, 0.14), (10.4, 0.14), (10.45, 0)], barrel_end - 0.8, Z, "brass", icon=False)
        m.sight = (barrel_end - 0.8, 10.42)
    # The pump: a ribbed forend on the tube with its action bars
    fx0, fx1 = forend_x, forend_x + 7.0
    xs = [fx0 + (fx1 - fx0) * k / 16 for k in range(17)]
    hv = [0.95 + (0.12 if (k % 3 == 1 and 3 < k < 14) else 0) for k in range(17)]
    m.sweep([(x, 8.0) for x in xs], 1.0, hv, wood if stock == "wood" else "polymer_black", rad=0.75)
    m.box((10.9, 7.75, Z + 0.6), (fx0, 8.05, Z + 0.75), "steel", r=0.05)
    m.box((10.9, 7.75, Z - 0.75), (fx0, 8.05, Z - 0.6), "steel", r=0.05)
    # Trigger group, guard, and the stock with its grip
    P.trigger_guard(m, 4.5, 7.6, 6.6, depth=1.5, r=0.22, tex=finish)
    if stock == "wood":
        P.stock_loft(m, [(0.4, 9.6), (-3.0, 9.4), (-8.0, 9.7), (-14.0, 10.0)], [(0.4, 6.6), (-1.5, 5.4), (-3.5, 5.6), (-8.0, 6.3), (-14.0, 4.8)],
                     [(-14.0, 1.9), (-4.0, 1.6), (0.4, 1.6)], wood)
        P.butt_pad(m, -14.0, 4.7, 10.1, 2.0, t=0.7)
        for k in range(5):
            m.box((-2.5 + k * 0.35, 5.7, Z + 0.6), (-2.35 + k * 0.35, 7.0, Z + 0.82), "wood_check", faces=["south"], mesh_only=True)
        m.stock_rear = -14.7
    else:
        P.grip(m, 3.0, 6.7, length=4.6, rake=24, w=1.45, depth=2.3, tex="polymer_black", stipple="grip_check")
        P.stock_loft(m, [(0.4, 9.8), (-12.5, 10.0)], [(0.4, 7.2), (-4.0, 7.6), (-12.5, 5.2)], [(-12.5, 1.8), (0.4, 1.5)], "polymer_black")
        P.butt_pad(m, -12.5, 5.1, 10.1, 1.9, t=0.8)
        m.stock_rear = -13.3
    # Shells in a side saddle
    for k in range(shells):
        P.cartridge(m, 1.0 + k * 0.85, 7.0, z0 - 0.45, length=2.0, r=0.36, case="paint_red", tip="brass", axis="y")
        m.cyl("y", 6.9, 7.3, 1.0 + k * 0.85, z0 - 0.45, 0.38, "brass", mesh_only=True)
    m.box((0.5, 6.8, z0 - 0.15), (0.5 + shells * 0.85, 9.2, z0), "polymer_black", r=0.08)


def m870(m):
    pump(m)


def ksg(m):
    # A bullpup pump with two tubes: built on the pump's parts but the action sits behind the grip
    z0, z1 = zs(2.0)
    body = [(-8.0, 5.6), (6.5, 5.6), (7.0, 6.4), (16.0, 6.6), (16.5, 7.6), (16.5, 10.3), (-7.6, 10.3), (-8.0, 9.8)]
    m.plate(body, z0, z1, "polymer_black", bevel=0.4)
    P.rail(m, -6.0, 16.0, 10.25, w=1.5)
    P.barrel(m, 16.0, 28.0, 9.3, 0.6, bore=0.4)
    for zz in (Z - 0.55, Z + 0.55):
        m.lathe("x", [(16.0, 0), (16.0, 0.55), (26.0, 0.55), (26.2, 0.45), (26.2, 0)], 7.6, zz, "steel_dark")
    xs = [17.0 + k * 0.5 for k in range(15)]
    m.sweep([(x, 7.5) for x in xs], 1.25, [1.0 + (0.1 if k % 3 == 1 else 0) for k in range(15)], "polymer_black", rad=0.7)
    P.grip(m, 9.0, 5.8, length=4.6, rake=20, w=1.5, depth=2.3, tex="polymer_black", stipple="grip_check")
    P.trigger_guard(m, 9.6, 12.8, 5.8, depth=1.6, r=0.22, tex="polymer_black")
    P.grip(m, 19.6, 6.6, length=3.4, rake=-8, w=1.3, depth=1.5, tex="polymer_black", grooves=0)
    P.butt_pad(m, -8.0, 5.5, 10.4, 2.1, t=0.7)
    m.plate([(-4.0, 8.8), (1.0, 8.8), (1.0, 9.9), (-4.0, 9.9)], z1 - 0.05, z1 + 0.01, "bore", bevel=0.0, icon=False)
    m.cyl("z", z0 - 0.2, z1 + 0.2, -0.5, 6.4, 0.3, "steel_dark")
    cy = P.red_dot(m, 6.0, 10.65)
    m.sight = (7.0, cy)
    m.stock_rear = -8.7


# ===============================================================================================================
# Bolt-action sniper: AWM
# ===============================================================================================================
def awm(m):
    # A chassis stock: the thumbhole body, adjustable cheek piece and butt, the action on top, a heavy barrel
    body = [(-14.0, 4.8), (-9.5, 5.6), (-7.0, 6.6), (-3.0, 6.4), (-2.0, 4.9), (0.5, 4.3), (1.6, 6.8), (16.0, 7.3), (16.6, 7.8),
            (16.6, 9.1), (2.0, 9.1), (-2.5, 8.8), (-6.0, 9.3), (-9.0, 9.4), (-14.0, 9.8)]
    m.plate(body, *zs(2.1), "polymer_green", bevel=0.45)
    m.plate([(-5.6, 7.3), (-3.4, 7.3), (-3.0, 6.9), (-2.4, 8.3), (-5.6, 8.5)], Z - 1.2, Z + 1.2, "bore", bevel=0.0, icon=False)   # thumbhole
    m.box((-13.4, 9.6, Z - 0.9), (-6.5, 10.6, Z + 0.9), "polymer_green", r=0.4)                  # cheek piece
    m.cyl("y", 9.2, 9.7, -12.0, Z, 0.25, "steel_dark", mesh_only=True)
    m.cyl("y", 9.2, 9.7, -7.9, Z, 0.25, "steel_dark", mesh_only=True)
    P.butt_pad(m, -14.0, 4.7, 9.9, 2.2, t=0.9)
    m.box((-14.6, 3.6, Z - 0.3), (-13.6, 5.0, Z + 0.3), "steel_dark", r=0.12)                     # monopod
    m.cyl("y", 2.6, 3.7, -14.1, Z, 0.25, "rubber")
    # Action: round receiver, bolt with a big knob, the scope rail
    m.lathe("x", [(0.0, 0), (0.0, 0.9), (0.4, 1.05), (11.0, 1.05), (11.4, 0.9), (11.4, 0)], 10.0, Z, "steel_dark")
    m.plate([(3.2, 9.4), (7.2, 9.4), (7.2, 10.6), (3.2, 10.6)], Z + 1.0, Z + 1.07, "bore", bevel=0.0, icon=False)
    m.box((3.4, 9.6, Z + 0.6), (7.0, 10.4, Z + 1.03), "steel_light", r=0.15, mesh_only=True)
    m.lathe("x", [(-1.6, 0), (-1.6, 0.55), (-0.6, 0.6), (0.0, 0.6), (0.0, 0)], 10.0, Z, "steel_dark")       # bolt shroud
    P.pipe = None
    m.pipe([(0.8, 10.0, Z + 0.6), (0.6, 9.3, Z + 1.3), (0.3, 8.6, Z + 1.75)], 0.18, "steel_light")
    P.knob(m, 0.25, 8.4, Z + 1.85, r=0.5, tex="polymer_black")
    P.rail(m, -0.4, 12.0, 10.95, w=1.6)
    # Heavy fluted barrel, the brake
    P.barrel(m, 11.4, 28.0, 10.0, 0.75, steps=[(13.0, 0.75), (14.0, 0.62)])
    for k in range(6):
        ang = k * 60
        m.box((15.0, 10.0 + 0.58, Z - 0.12), (24.0, 10.0 + 0.64, Z + 0.12), "bore", faces=["up"], rot=("x", ang, (15.0, 10.0, Z)), mesh_only=True)
    P.brake(m, 28.0, 10.0, r=0.95, L=3.0, ports=3)
    # Trigger, guard, mag
    P.trigger_guard(m, 1.4, 4.6, 6.8, depth=1.6, r=0.22, tex="steel_dark")
    m.mag(5.6, 7.0, 3.2, 3.0, Z - 0.8, Z + 0.8, "steel_dark", curve=0, base="steel_dark")
    # Bipod folded under the fore-end
    for s in (-1, 1):
        m.pipe([(15.5, 7.2, Z + s * 0.5), (22.5, 7.0, Z + s * 0.6)], 0.17, "steel_dark")
    m.box((14.8, 6.8, Z - 0.9), (16.2, 7.4, Z + 0.9), "steel_dark", r=0.15)
    # Scope
    cy = P.scope(m, 0.5, 12.5, 12.9, r=0.66, bell_front=1.55, bell_rear=1.3, base_y=11.2, sunshade=1.2)
    m.sight = (12.5, cy)
    m.stock_rear = -14.9


# ===============================================================================================================
# Bullpup SMG: P90
# ===============================================================================================================
def p90(m):
    # The whole gun is one smooth shell: thumbhole grip, the clear magazine on top
    shell = [(-6.5, 5.0), (-5.0, 4.2), (-2.6, 4.2), (-1.2, 6.2), (1.8, 6.2), (3.0, 4.6), (4.6, 4.3), (5.6, 5.4), (9.0, 6.6), (10.5, 7.1),
             (11.2, 8.4), (11.2, 9.3), (-6.0, 9.6), (-6.9, 8.6)]
    m.plate(shell, *zs(2.3), "polymer_black", bevel=0.6)
    m.plate([(-2.2, 5.0), (1.0, 5.0), (2.2, 6.0), (1.6, 6.6), (-1.6, 6.6)], Z - 1.3, Z + 1.3, "bore", bevel=0.0, icon=False)
    m.plate([(3.6, 5.0), (5.0, 5.0), (5.6, 6.0), (4.2, 6.6)], Z - 1.3, Z + 1.3, "bore", bevel=0.0, icon=False)
    # The magazine: translucent top with the rounds visible inside, sideways
    m.box((-5.6, 9.6, Z - 1.1), (6.4, 10.6, Z + 1.1), "lens", r=0.3)
    for k in range(14):
        P.cartridge(m, -5.2 + k * 0.8, 10.1, Z - 0.6, length=1.1, r=0.17, axis="y") if False else None
        m.lathe("z", [(Z - 0.9, 0), (Z - 0.9, 0.16), (Z + 0.5, 0.16), (Z + 0.9, 0.05), (Z + 0.9, 0)], -5.2 + k * 0.8, 10.1, "brass", icon=False)
    m.box((6.4, 9.6, Z - 1.15), (7.2, 10.7, Z + 1.15), "polymer_black", r=0.25)
    # Front: barrel shroud and flash hider, the optic housing
    P.barrel(m, 10.8, 14.5, 8.8, 0.45)
    P.flash_hider(m, 14.5, 8.8, r=0.5, L=1.4, prongs=3)
    m.box((3.0, 10.6, Z - 0.9), (9.6, 11.9, Z + 0.9), "polymer_black", r=0.35)
    m.lathe("x", [(9.6, 0.62), (9.6, 0), ], 11.25, Z, "lens", icon=False)
    m.cyl("x", 6.8, 6.85, 11.25, Z, 0.05, "red_dot", mesh_only=True)
    m.sight = (9.0, 11.6)
    m.stock_rear = -6.9
    # Cocking handles on both sides, selector disc under the trigger
    for s in (-1, 1):
        m.box((5.0, 8.4, Z + s * 1.2 - 0.12), (5.6, 8.9, Z + s * 1.2 + 0.25 * s + 0.12), "polymer_black", r=0.08)
    m.cyl("y", 4.6, 4.8, 2.8, Z, 0.55, "polymer_grey")
    P.trigger(m, 2.6, 6.1, "polymer_black")



# ===============================================================================================================
# AK family: AK-12, AKS-74U, RPK, Galil ACE, PP-19 Bizon (receiver), AS VAL / VSS (their own)
# ===============================================================================================================
def ak_receiver(m, length=13.0, finish="steel", cover="steel", rail_cover=False, rivets=True):
    z0, z1 = zs(1.75)
    rec = [(0.0, 9.3), (0.0, 7.6), (0.4, 6.7), (length, 6.7), (length, 9.3)]
    m.plate(rec, z0, z1, finish, bevel=0.14, corner=0.2)
    # Stamped magazine-well dimples, rivets, the trunnion block in front
    for zz, sgn in ((z1, 1), (z0, -1)):
        m.plate([(8.4, 7.3), (11.6, 7.3), (11.6, 8.5), (8.4, 8.5)], zz - 0.04 if sgn > 0 else zz - 0.06, zz + 0.06 if sgn > 0 else zz + 0.04, finish, bevel=0.03, icon=False)
        if rivets:
            for (rx, ry) in ((1.0, 7.2), (2.2, 7.2), (6.6, 7.0), (12.2, 7.1), (12.2, 8.9), (4.4, 8.85)):
                P.rivet(m, rx, ry, zz, sgn, r=0.14)
    # Dust cover: a rounded top along the receiver, ribbed, its rear button
    xs = [0.2 + (length - 0.4) * k / 14 for k in range(15)]
    if rail_cover:
        m.sweep([(x, 9.6) for x in xs], 0.95, 0.45, cover, rad=0.5)
        P.rail(m, 0.6, length - 0.4, 10.0, w=1.5)
    else:
        m.sweep([(x, 9.55) for x in xs], 0.92, 0.42, cover, rad=0.9)
        for k in range(3):
            m.box((3.0 + k * 0.5, 9.9, Z - 0.5), (3.2 + k * 0.5, 10.02, Z + 0.5), cover, r=0.04, mesh_only=True)
    m.box((-0.25, 9.3, Z - 0.3), (0.3, 9.75, Z + 0.3), "steel_dark", r=0.1)
    # Safety lever (big, right side), charging handle on the carrier, ejection port
    m.plate([(1.0, 9.0), (8.5, 8.6), (8.6, 8.2), (8.0, 7.9), (1.2, 8.6)], z1, z1 + 0.18, "steel_dark", bevel=0.05)
    m.plate([(4.4, 9.45), (9.6, 9.45), (9.6, 8.9), (4.4, 8.9)], z1 - 0.04, z1 + 0.01, "bore", bevel=0.0, icon=False, corner=0.1)
    m.box((4.6, 9.0, z1 - 0.3), (9.4, 9.4, z1 + 0.02), "steel_light", r=0.1, mesh_only=True)
    m.box((10.5, 9.0, z1), (11.3, 9.4, z1 + 0.6), "steel_light", r=0.12)
    m.cyl("z", z1 + 0.5, z1 + 1.1, 10.9, 9.2, 0.26, "steel_light")
    # Rear sight block on the trunnion
    m.box((length, 8.2, Z - 0.75), (length + 2.2, 9.7, Z + 0.75), "steel_dark", r=0.2)
    m.plate([(length + 0.2, 9.7), (length + 2.0, 9.7), (length + 1.9, 10.25), (length + 0.3, 10.25)], Z - 0.55, Z + 0.55, "steel_dark", bevel=0.06)
    m.box((length + 0.6, 10.2, Z - 0.6), (length + 1.2, 10.5, Z + 0.6), "steel_dark", r=0.05)
    return z0, z1


def ak_front(m, x0, barrel_end, wood="polymer_plum", lower_len=6.0, gas_len=7.5, brake="ak74", fs_x=None, rail=False):
    # Barrel, gas block, gas tube with the upper handguard, lower handguard, front sight block, muzzle device
    P.barrel(m, x0, barrel_end, 8.95, 0.5)
    hx1 = x0 + lower_len
    if rail:
        P.quad_rail(m, x0 + 0.2, hx1, 8.8, 1.25, 1.2, "alu")
    else:
        P.wood_handguard(m, x0 + 0.2, hx1, 8.3, 0.95, 1.05, wood, grooves=4)
        xs = [x0 + 0.2 + (gas_len - 0.4) * k / 12 for k in range(13)]
        m.sweep([(x, 9.85) for x in xs], 0.62, 0.5, wood, rad=0.8)
        m.box((x0 + 0.2, 7.6, Z - 1.15), (x0 + 0.6, 9.4, Z + 1.15), "steel_dark", r=0.12)     # retainer
        m.box((hx1 - 0.2, 7.4, Z - 1.1), (hx1 + 0.2, 9.3, Z + 1.1), "steel_dark", r=0.12)
    m.lathe("x", [(x0, 0), (x0, 0.38), (x0 + gas_len + 1.0, 0.38), (x0 + gas_len + 1.0, 0)], 10.0, Z, "steel_dark")
    gx = x0 + gas_len + 0.6
    m.box((gx, 8.5, Z - 0.55), (gx + 1.3, 10.4, Z + 0.55), "steel_dark", r=0.18)
    fx = fs_x if fs_x is not None else barrel_end - 1.2
    m.box((fx - 0.6, 8.4, Z - 0.6), (fx + 0.6, 9.5, Z + 0.6), "steel_dark", r=0.15)
    top = P.iron_post(m, fx, 9.5, h=1.2)
    m.box((fx - 0.8, 7.7, Z - 0.25), (fx + 0.3, 8.5, Z + 0.25), "steel_dark", r=0.08)                       # bayonet lug
    if brake == "ak74":
        m.lathe("x", [(barrel_end, 0), (barrel_end, 0.62), (barrel_end + 2.2, 0.62), (barrel_end + 2.5, 0.5), (barrel_end + 2.5, 0.24),
                      (barrel_end + 1.6, 0.24), (barrel_end + 1.6, 0)], 8.95, Z, "steel_dark", mats=["steel_dark"] * 4 + ["bore"] * 2)
        for k in range(2):
            x = barrel_end + 0.5 + k * 0.7
            for zz, f in ((Z + 0.62, "south"), (Z - 0.62, "north")):
                m.box((x, 8.7, zz - 0.02 if f == "south" else zz - 0.01), (x + 0.4, 9.2, zz + 0.01 if f == "south" else zz + 0.02), "bore", faces=[f], mesh_only=True)
        m.box((barrel_end + 0.4, 8.4, Z - 0.35), (barrel_end + 1.4, 8.42, Z + 0.35), "bore", faces=["down"], mesh_only=True)
    elif brake == "slant":
        m.lathe("x", [(barrel_end, 0), (barrel_end, 0.55), (barrel_end + 1.6, 0.55), (barrel_end + 1.6, 0.24), (barrel_end + 1.0, 0.24), (barrel_end + 1.0, 0)],
                8.95, Z, "steel_dark", mats=["steel_dark"] * 3 + ["bore"] * 2)
    elif brake == "booster":
        m.lathe("x", [(barrel_end, 0), (barrel_end, 0.7), (barrel_end + 0.3, 0.85), (barrel_end + 2.2, 0.85), (barrel_end + 2.6, 0.6), (barrel_end + 2.6, 0.3),
                      (barrel_end + 1.8, 0.3), (barrel_end + 1.8, 0)], 8.95, Z, "steel_dark", mats=["steel_dark"] * 5 + ["bore"] * 2)
    elif brake == "brake":
        P.brake(m, barrel_end, 8.95, r=0.7, L=2.6, ports=3)
    return fx, top


def ak_lower(m, wood="polymer_plum", mag_tex="polymer_plum", mag_depth=6.6, mag_curve=2, drum=False, grip_tex=None):
    z0, z1 = zs(1.75)
    # Stamped trigger guard (a flat loop) with the mag release paddle at its front
    m.plate([(3.6, 6.7), (4.0, 5.2), (4.6, 4.95), (7.6, 4.95), (8.1, 5.3), (8.2, 6.7), (7.8, 6.7), (7.6, 5.4), (4.7, 5.35), (4.3, 6.7)],
            Z - 0.28, Z + 0.28, "steel_dark", bevel=0.06, corner=0.15)
    m.plate([(8.0, 6.7), (8.6, 6.7), (8.7, 5.6), (8.3, 5.6)], Z - 0.45, Z + 0.45, "steel_dark", bevel=0.06)
    P.trigger(m, 6.0, 6.65, "steel")
    P.grip(m, 3.6, 6.75, length=4.6, rake=24, w=1.35, depth=2.0, tex=grip_tex or wood, grooves=3, stipple="grip_check" if (grip_tex or wood).startswith("polymer") else None)
    if drum:
        P.drum(m, 10.6, 3.6, r=2.6, depth=2.6, tex=mag_tex)
        m.box((9.0, 5.5, Z - 0.7), (12.2, 6.8, Z + 0.7), mag_tex, r=0.2)
    else:
        m.mag(8.7, 6.8, 2.9, mag_depth, Z - 0.72, Z + 0.72, mag_tex, curve=mag_curve, base=mag_tex, ribs=4, rib_tex=mag_tex)


def ak_stock(m, kind="wood", wood="polymer_plum"):
    if kind == "wood":
        P.stock_loft(m, [(0.2, 9.2), (-4.0, 8.9), (-13.0, 8.6)], [(0.2, 7.3), (-4.0, 6.6), (-13.0, 4.6)], [(-13.0, 1.7), (0.2, 1.5)], wood)
        m.box((-13.4, 4.5, Z - 0.85), (-13.0, 8.7, Z + 0.85), "steel_dark", r=0.1)
        P.sling_loop(m, -9.0, 6.0, Z, r=0.4, axis="x")
        m.stock_rear = -13.4
    elif kind == "folding":
        # A side-folding tube stock (AK-12 style), opened, with its cheek riser and pad
        m.lathe("x", [(-9.5, 0), (-9.5, 0.5), (0.0, 0.5), (0.0, 0)], 8.7, Z, "polymer_black")
        m.plate([(-12.6, 9.8), (-8.6, 9.6), (-8.0, 8.4), (-8.6, 7.6), (-10.6, 6.0), (-12.6, 5.0)], Z - 0.9, Z + 0.9, "polymer_black", bevel=0.3)
        P.butt_pad(m, -12.6, 4.9, 9.9, 1.9, t=0.6)
        m.box((-0.4, 7.6, Z - 1.0), (0.4, 9.4, Z + 1.0), "steel_dark", r=0.15)
        m.stock_rear = -13.2
    elif kind == "underfold":
        # The AKS-74U's skeleton stock (two struts and a butt plate)
        for yy in (8.7, 6.9):
            m.box((-10.0, yy - 0.25, Z - 0.35), (0.0, yy + 0.25, Z + 0.35), "steel_dark", r=0.12)
        m.plate([(-10.6, 9.4), (-10.0, 9.4), (-10.0, 6.2), (-10.6, 6.2)], Z - 0.8, Z + 0.8, "steel_dark", bevel=0.12)
        m.stock_rear = -10.6
    else:
        m.stock_rear = -0.3


def ak12(m):
    ak_receiver(m, rail_cover=True, finish="steel_dark", cover="steel_dark")
    ak_front(m, 15.2, 27.0, wood="polymer_black", lower_len=7.4, rail=True, brake="slant")
    ak_lower(m, wood="polymer_black", mag_tex="polymer_black")
    ak_stock(m, "folding")
    m.box((0.4, 10.2, Z - 0.4), (1.4, 11.3, Z + 0.4), "steel_dark", r=0.1)
    m.sight = (25.8, 10.6)


def aks74u(m):
    ak_receiver(m, finish="steel", cover="steel")
    # Short: the gas tube and handguard right up to a booster
    P.barrel(m, 15.2, 21.6, 8.95, 0.5)
    P.wood_handguard(m, 15.4, 19.6, 8.3, 0.95, 1.05, "polymer_plum", grooves=3)
    xs = [15.4 + 4.2 * k / 8 for k in range(9)]
    m.sweep([(x, 9.95) for x in xs], 0.62, 0.55, "polymer_plum", rad=0.8)
    m.box((19.8, 8.4, Z - 0.6), (20.9, 10.6, Z + 0.6), "steel_dark", r=0.2)
    m.lathe("x", [(21.6, 0), (21.6, 0.85), (21.9, 1.0), (23.6, 1.0), (24.0, 0.7), (24.0, 0.3), (23.0, 0.3), (23.0, 0)], 8.95, Z, "steel_dark",
            mats=["steel_dark"] * 5 + ["bore"] * 2)
    top = P.iron_post(m, 20.35, 10.6, h=0.9)
    # The flip-up rear sight on the top cover's hinge
    m.box((12.4, 10.0, Z - 0.6), (13.6, 10.3, Z + 0.6), "steel_dark", r=0.08)
    m.box((12.8, 10.3, Z - 0.5), (13.2, 11.3, Z + 0.5), "steel_dark", r=0.06)
    ak_lower(m, wood="polymer_plum", mag_tex="polymer_plum", mag_depth=6.2)
    ak_stock(m, "underfold")
    m.sight = (20.35, top - 0.1)


def rpk(m):
    ak_receiver(m, finish="steel", cover="steel")
    ak_front(m, 15.2, 30.0, wood="birch", lower_len=6.4, brake="slant", fs_x=28.6)
    ak_lower(m, wood="birch", mag_tex="steel_dark", drum=True)
    # The RPK's club-foot stock
    P.stock_loft(m, [(0.2, 9.2), (-5.0, 8.9), (-13.5, 8.8)], [(0.2, 7.3), (-3.5, 5.6), (-6.5, 6.4), (-9.0, 5.4), (-13.5, 4.2)], [(-13.5, 1.8), (0.2, 1.5)], "birch")
    m.box((-13.9, 4.1, Z - 0.9), (-13.5, 8.9, Z + 0.9), "steel_dark", r=0.12)
    m.stock_rear = -13.9
    # Folded bipod under the barrel
    for s in (-1, 1):
        m.pipe([(25.5, 8.4, Z + s * 0.35), (19.0, 7.8, Z + s * 0.45)], 0.15, "steel_dark")
    m.box((25.2, 8.1, Z - 0.6), (26.2, 8.8, Z + 0.6), "steel_dark", r=0.12)
    m.sight = (28.6, 10.6)


def galil(m):
    ak_receiver(m, finish="steel_dark", cover="steel_dark", rail_cover=True)
    ak_front(m, 15.2, 27.5, wood="polymer_black", lower_len=7.5, rail=True, brake="brake")
    ak_lower(m, wood="polymer_black", mag_tex="polymer_black", mag_curve=1, grip_tex="polymer_black")
    # Side-folding skeleton stock with a cheek rest, adjustable
    m.lathe("x", [(-10.0, 0), (-10.0, 0.45), (0.0, 0.45), (0.0, 0)], 8.9, Z, "polymer_black")
    m.plate([(-12.8, 9.9), (-7.0, 9.6), (-6.6, 9.0), (-12.8, 9.0)], Z - 0.8, Z + 0.8, "polymer_black", bevel=0.25)
    m.plate([(-12.8, 9.0), (-11.8, 9.0), (-11.6, 5.0), (-12.8, 5.0)], Z - 0.85, Z + 0.85, "polymer_black", bevel=0.25)
    P.butt_pad(m, -12.8, 4.9, 10.0, 1.9, t=0.6)
    m.stock_rear = -13.4
    cy = P.red_dot(m, 4.5, 10.45)
    m.sight = (5.7, cy)


def bizon(m):
    ak_receiver(m, finish="steel_dark", cover="steel_dark", length=12.0)
    P.barrel(m, 14.2, 21.0, 8.95, 0.48)
    P.wood_handguard(m, 14.4, 18.4, 8.6, 1.1, 1.05, "polymer_black", grooves=0)
    m.box((18.6, 8.4, Z - 0.6), (19.8, 10.4, Z + 0.6), "steel_dark", r=0.2)
    P.iron_post(m, 19.2, 10.4, h=0.9)
    P.flash_hider(m, 21.0, 8.95, r=0.5, L=1.4, prongs=3)
    # The helical magazine: a long cylinder under the barrel, ribbed, the spiral showing through its ribs
    m.lathe("x", [(7.0, 0), (7.0, 1.1), (7.3, 1.3), (18.5, 1.3), (18.8, 1.1), (18.8, 0)], 6.2, Z, "polymer_black")
    for k in range(14):
        x = 8.0 + k * 0.75
        m.lathe("x", [(x, 1.3), (x, 1.36), (x + 0.3, 1.36), (x + 0.3, 1.3)], 6.2, Z, "polymer_grey", icon=False)
    z0, z1 = zs(1.75)
    P.trigger_guard(m, 4.4, 7.0, 6.7, depth=1.3, r=0.2, tex="steel_dark")
    P.grip(m, 3.6, 6.75, length=4.5, rake=24, w=1.35, depth=2.0, tex="polymer_black", stipple="grip_check")
    ak_stock(m, "folding")
    m.sight = (19.2, 11.2)


def as_val(m, vss=False):
    # The integrally suppressed 9x39 rifles: a short receiver, the big can making up the front, a skeleton (VAL)
    # or wooden thumbhole (VSS) stock
    z0, z1 = zs(1.7)
    m.plate([(0.0, 9.3), (0.0, 7.4), (0.4, 6.7), (11.0, 6.7), (11.0, 9.3)], z0, z1, "steel_dark", bevel=0.14)
    xs = [0.2 + 10.6 * k / 12 for k in range(13)]
    m.sweep([(x, 9.55) for x in xs], 0.88, 0.42, "steel_dark", rad=0.9)
    m.plate([(4.0, 9.4), (8.0, 9.4), (8.0, 8.8), (4.0, 8.8)], z1 - 0.04, z1 + 0.01, "bore", bevel=0.0, icon=False, corner=0.1)
    m.box((8.6, 8.95, z1), (9.3, 9.35, z1 + 0.6), "steel_light", r=0.12)
    m.plate([(1.0, 9.0), (7.5, 8.6), (7.6, 8.2), (7.0, 7.9), (1.2, 8.6)], z1, z1 + 0.18, "steel_dark", bevel=0.05)
    P.suppressor(m, 11.0, 8.95, r=1.15, L=11.5 if not vss else 12.5, tex="steel_dark", knurl=False)
    for k in range(10):
        m.cyl("x", 12.0 + k * 1.0, 12.25 + k * 1.0, 8.95, Z, 1.17, "polymer_black", ends=False, mesh_only=True)
    m.box((11.0, 6.4, Z - 1.1), (17.0, 8.3, Z + 1.1), "polymer_black" if not vss else "walnut", r=0.5)       # fore-end
    top = P.iron_post(m, 21.5, 10.1, h=0.9)
    m.box((9.0, 9.8, Z - 0.6), (10.6, 10.4, Z + 0.6), "steel_dark", r=0.1)
    P.trigger_guard(m, 3.8, 6.8, 6.7, depth=1.4, r=0.2, tex="steel_dark")
    m.mag(7.2, 6.8, 2.6, 3.6, Z - 0.75, Z + 0.75, "polymer_black", curve=0, base="polymer_black", ribs=2)
    if vss:
        P.stock_loft(m, [(0.2, 9.3), (-5.0, 9.2), (-13.5, 9.4)], [(0.2, 4.4), (-1.5, 4.2), (-4.0, 5.6), (-7.0, 6.5), (-13.5, 4.6)], [(-13.5, 1.8), (0.2, 1.6)], "walnut")
        m.plate([(-3.6, 6.4), (-1.0, 6.4), (-0.6, 7.6), (-3.6, 7.8)], Z - 1.0, Z + 1.0, "bore", bevel=0.0, icon=False)
        m.box((-13.9, 4.5, Z - 0.9), (-13.5, 9.5, Z + 0.9), "rubber", r=0.2)
        m.stock_rear = -13.9
        cy = P.scope(m, 2.0, 10.0, 11.6, r=0.55, bell_front=1.4, bell_rear=1.3, base_y=9.9, turrets=True)
        m.sight = (10.0, cy)
    else:
        P.grip(m, 3.4, 6.75, length=4.4, rake=22, w=1.35, depth=2.0, tex="polymer_black", stipple="grip_check")
        for yy in (8.9, 7.3):
            m.box((-10.0, yy - 0.22, Z - 0.3), (0.0, yy + 0.22, Z + 0.3), "steel_dark", r=0.1)
        m.plate([(-10.6, 9.6), (-10.0, 9.6), (-10.0, 6.0), (-10.6, 6.0)], Z - 0.8, Z + 0.8, "steel_dark", bevel=0.12)
        m.stock_rear = -10.6
        m.sight = (21.5, top - 0.1)


def vss(m):
    as_val(m, vss=True)


# ===============================================================================================================
# More pistols and revolvers
# ===============================================================================================================
def usp45(m):
    pistol2(m, slide_len=12.2, slide_h=2.3, w=1.5, hammer=True, decock=True, frame_tex="polymer_black")


def five7(m):
    pistol2(m, slide_len=12.4, slide_h=2.0, w=1.35, slide_tex="polymer_black", frame_tex="polymer_fde", grip_stipple="grip_check_tan", front_serr=False, rake=14, grip_len=5.8)


def p226(m):
    pistol2(m, slide_len=12.2, slide_h=2.3, w=1.45, slide_tex="steel_dark", frame_tex="alu", grip_tex="polymer_black", hammer=True, decock=True)


def cz75(m):
    pistol2(m, slide_len=12.0, slide_h=1.9, w=1.4, slide_tex="blued", frame_tex="blued", grip_tex="rosewood", grip_stipple=None, hammer=True, rail=False, front_serr=False, beavertail=True)


def makarov(m):
    pistol2(m, slide_len=10.0, slide_h=2.0, w=1.3, slide_tex="blued", frame_tex="blued", grip_tex="bakelite", grip_stipple=None, hammer=True, rail=False, front_serr=False, decock=True, grip_len=4.4)


def tt33(m):
    pistol2(m, slide_len=12.0, slide_h=1.9, w=1.25, slide_tex="blued", frame_tex="blued", grip_tex="bakelite", grip_stipple=None, hammer=True, rail=False, front_serr=False, rake=12, grip_len=5.2)
    for zz in (Z - 0.66, Z + 0.66):
        m.lathe("z", [(zz - 0.04, 0), (zz - 0.04, 0.38), (zz + 0.04, 0.38), (zz + 0.04, 0)], 3.1, 4.6, "bakelite", icon=False)   # star medallions


def luger(m):
    # The toggle-lock: a round barrel, the jointed toggle on top, the raked grip
    z0, z1 = zs(1.2)
    P.barrel(m, 5.0, 13.2, 9.0, 0.5, steps=[(6.0, 0.62), (7.0, 0.5)])
    m.plate([(1.0, 8.2), (7.6, 8.2), (7.6, 9.6), (1.0, 9.6)], z0, z1, "blued", bevel=0.15)
    for (x0, x1) in ((0.6, 3.6), (3.4, 6.0)):
        m.lathe("x", [(x0, 0), (x0, 0.45), (x1, 0.45), (x1, 0)], 9.75, Z, "blued")
    for zz in (z0 - 0.3, z1 + 0.3):
        m.lathe("z", [(zz - 0.25, 0), (zz - 0.25, 0.48), (zz + 0.25, 0.48), (zz + 0.25, 0)], 0.6, 9.75, "knurl")
    m.plate([(1.0, 8.2), (4.0, 8.2), (5.2, 7.0), (6.4, 7.0), (7.4, 8.2), (7.6, 6.8), (5.0, 6.4), (3.2, 7.4)], z0 + 0.05, z1 - 0.05, "blued", bevel=0.1)
    P.trigger(m, 5.7, 7.9, "blued")
    P.grip(m, 2.6, 8.0, length=4.9, rake=32, w=1.3, depth=2.4, tex="walnut", grooves=0, swell=0.1, base="steel_dark")
    for zz, sgn in ((Z + 0.66, 1), (Z - 0.66, -1)):
        pass
    top = P.iron_post(m, 12.6, 9.45, h=0.5, hood=False)
    m.sight = (12.6, top)
    m.stock_rear = 0.0


def c96(m):
    # The Broomhandle: a box magazine in front of the trigger, the long barrel, a round-handle grip
    z0, z1 = zs(1.3)
    m.plate([(0.0, 7.0), (9.0, 7.0), (9.0, 6.0), (11.5, 6.0), (11.5, 9.8), (0.6, 9.8), (0.0, 9.2)], z0, z1, "blued", bevel=0.15)
    m.plate([(7.6, 7.0), (11.4, 7.0), (11.4, 4.0), (7.8, 4.0)], z0 - 0.08, z1 + 0.08, "blued", bevel=0.12)
    for zz, sgn in ((z1, 1), (z0, -1)):
        m.plate([(1.2, 7.4), (6.8, 7.4), (6.8, 9.2), (1.2, 9.2)], zz - 0.03 if sgn > 0 else zz - 0.06, zz + 0.06 if sgn > 0 else zz + 0.03, "blued", bevel=0.03, icon=False)
    P.barrel(m, 11.5, 19.0, 9.0, 0.45, steps=[(12.5, 0.62), (13.0, 0.45)])
    m.plate([(-0.6, 9.9), (0.2, 10.2), (0.6, 9.6), (0.2, 8.8)], Z - 0.3, Z + 0.3, "steel_dark", bevel=0.08)
    m.box((9.4, 9.8, Z - 0.5), (11.2, 10.4, Z + 0.5), "steel_dark", r=0.1)
    P.trigger_guard(m, 4.2, 7.6, 7.0, depth=1.5, r=0.2, tex="blued")
    P.grip(m, 3.2, 7.1, length=4.2, rake=10, w=1.3, depth=2.2, tex="walnut", grooves=0, swell=0.2)
    for k in range(9):
        y = 6.3 - k * 0.45
        for zz, f in ((Z + 0.66, "south"), (Z - 0.66, "north")):
            m.box((1.6 - k * 0.07, y, zz - 0.03 if f == "south" else zz - 0.04), (3.0 - k * 0.07, y + 0.12, zz + 0.04 if f == "south" else zz + 0.03), "walnut_dark", faces=[f], mesh_only=True)
    top = P.iron_post(m, 18.2, 9.45, h=0.55, hood=False)
    m.sight = (18.2, top)
    m.stock_rear = -0.6


def revolver(m, barrel_len=7.0, finish="stainless", grip_tex="rubber", rib=True, shroud=True, judge=False, heavy=False):
    z0, z1 = zs(1.55)
    cyl_r = 1.5 if not judge else 1.7
    cyl_x0, cyl_x1 = 3.4, 7.4 if not judge else 8.4
    fx1 = cyl_x1 + 0.9
    cy = 9.1
    # Frame around the cylinder window: the back with the recoil shield, the top strap, the front, the bottom (crane)
    m.plate([(0.6, 6.5), (0.6, 10.2), (1.4, 10.9), (cyl_x0, 10.9), (cyl_x0, 6.9), (2.6, 6.5)], z0, z1, finish, bevel=0.2, corner=0.3)
    m.box((cyl_x0 - 0.1, cy + cyl_r - 0.05, Z - 0.62), (fx1, 10.9, Z + 0.62), finish, r=0.2)
    m.plate([(cyl_x1, 7.0), (fx1 + 0.2, 7.0), (fx1 + 0.5, 7.8), (fx1 + 0.5, 10.9), (cyl_x1, 10.9)], Z - 0.7, Z + 0.7, finish, bevel=0.2)
    m.box((cyl_x0 - 0.1, 6.5, Z - 0.7), (cyl_x1 + 0.1, cy - cyl_r + 0.05, Z + 0.7), finish, r=0.2)
    # The cylinder, fluted, chambers loaded (brass at the back, dark at the front), its axis pin
    m.lathe("x", [(cyl_x0, 0), (cyl_x0, cyl_r - 0.12), (cyl_x0 + 0.12, cyl_r), (cyl_x1 - 0.3, cyl_r), (cyl_x1, cyl_r - 0.3), (cyl_x1, 0)], cy, Z, finish)
    for k in range(6):
        ang = 30 + k * 60
        m.box((cyl_x0 + 0.5, cy + cyl_r - 0.14, Z - 0.28), (cyl_x1 - 0.6, cy + cyl_r + 0.01, Z + 0.28), "steel_dark", rot=("x", ang, (cyl_x0, cy, Z)), r=0.1, mesh_only=True)
        a = math.radians(ang + 30)
        m.cyl("x", cyl_x1 - 0.02, cyl_x1 + 0.01, cy + math.cos(a) * cyl_r * 0.6, Z + math.sin(a) * cyl_r * 0.6, 0.3 if not judge else 0.45, "bore", mesh_only=True)
    m.cyl("x", cyl_x1, fx1 + 0.3, cy - 0.9, Z, 0.18, "steel_light")
    # Barrel: a full underlug shroud and a ventilated rib (or a plain heavy barrel)
    bx0, bx1 = fx1 + 0.5, fx1 + 0.5 + barrel_len
    P.barrel(m, bx0 - 0.2, bx1, cy + 0.35, 0.62 if not heavy else 0.72, bore=0.3 if not judge else 0.45)
    if shroud:
        m.plate([(bx0 - 0.2, 7.3), (bx1 - 0.3, 7.6), (bx1, 8.1), (bx1, cy + 0.4), (bx0 - 0.2, cy + 0.4)], Z - 0.62, Z + 0.62, finish, bevel=0.28, corner=0.3)
    if rib:
        m.box((bx0 - 0.2, cy + 0.85, Z - 0.4), (bx1, cy + 1.2, Z + 0.4), finish, r=0.1)
        for k in range(int(barrel_len / 0.9)):
            x = bx0 + 0.3 + k * 0.9
            m.box((x, cy + 0.88, Z - 0.41), (x + 0.45, cy + 1.08, Z + 0.41), "bore", faces=["north", "south"], mesh_only=True)
    top = P.iron_post(m, bx1 - 0.6, cy + 1.15, h=0.55, hood=False)
    m.box((1.0, 10.9, Z - 0.5), (2.0, 11.25, Z + 0.5), finish, r=0.08)
    m.box((1.1, 11.0, Z - 0.12), (1.9, 11.27, Z + 0.12), "bore", mesh_only=True)
    m.sight = (bx1 - 0.6, top)
    # Hammer with its spur, the cylinder latch (left), trigger and guard, the grip
    m.plate([(-0.4, 11.4), (0.4, 11.15), (1.2, 9.6), (0.8, 9.3), (0.1, 10.4), (-0.7, 10.9)], Z - 0.3, Z + 0.3, "steel_dark", bevel=0.08)
    m.box((1.6, 9.3, z0 - 0.15), (2.8, 9.9, z0), "steel_dark", r=0.08)
    P.trigger_guard(m, 2.6, 5.8, 6.55, depth=1.45, r=0.2, tex=finish)
    P.grip(m, 2.2, 6.9, length=4.5, rake=26, w=1.5, depth=2.4, tex=grip_tex, grooves=3 if grip_tex == "rubber" else 0, swell=0.22)
    m.stock_rear = -0.6


def python357(m):
    revolver(m, barrel_len=7.0, finish="blued", grip_tex="walnut", rib=True, shroud=True)


def m29(m):
    revolver(m, barrel_len=8.0, finish="stainless", grip_tex="rubber", rib=False, shroud=True, heavy=True)


def judge(m):
    revolver(m, barrel_len=4.0, finish="steel_dark", grip_tex="rubber", rib=False, shroud=True, judge=True)


# ===============================================================================================================
# SMGs: Vector, UMP-45, Thompson, PPSh-41, MAC-10, MP40
# ===============================================================================================================
def vector(m):
    # The Kriss: a tall angular lower that drops in front of the grip, the mag well forward, a low barrel
    z0, z1 = zs(1.9)
    body = [(0.0, 10.3), (12.5, 10.3), (13.2, 9.5), (13.2, 7.2), (12.6, 6.4), (11.6, 6.4), (11.2, 3.6), (8.4, 3.6), (8.6, 6.0), (6.6, 6.2),
            (5.0, 5.4), (2.8, 5.6), (0.0, 7.6)]
    m.plate(body, z0, z1, "polymer_black", bevel=0.35, corner=0.35)
    m.plate([(6.8, 6.4), (8.4, 6.4), (8.2, 4.4), (7.0, 4.6)], Z - 1.0, Z + 1.0, "bore", bevel=0.0, icon=False)
    P.rail(m, 0.4, 12.6, 10.25, w=1.6)
    P.barrel(m, 13.0, 17.5, 8.6, 0.5)
    P.flash_hider(m, 17.5, 8.6, r=0.55, L=1.5, prongs=3)
    m.box((11.6, 7.6, z1), (12.4, 8.2, z1 + 0.5), "steel_dark", r=0.1)
    P.trigger(m, 6.0, 6.2, "steel_dark")
    P.grip(m, 5.2, 5.6, length=4.4, rake=14, w=1.45, depth=2.1, tex="polymer_black", stipple="grip_check")
    m.mag(8.6, 3.8, 2.6, 4.2, Z - 0.75, Z + 0.75, "polymer_black", curve=0, base="polymer_black", ribs=2)
    m.lathe("x", [(-7.5, 0), (-7.5, 0.55), (0.0, 0.55), (0.0, 0)], 8.6, Z, "polymer_black")
    m.plate([(-11.0, 10.0), (-7.0, 9.8), (-7.0, 7.4), (-9.0, 6.6), (-11.0, 5.8)], Z - 0.9, Z + 0.9, "polymer_black", bevel=0.3)
    P.butt_pad(m, -11.0, 5.7, 10.1, 1.9, t=0.6)
    front = P.iron_post(m, 12.0, 10.6, h=1.1)
    P.iron_rear(m, 0.9, 10.6, h=1.1)
    m.sight = (12.0, front - 0.1)
    m.stock_rear = -11.6


def ump45(m):
    z0, z1 = zs(1.9)
    body = [(0.0, 10.2), (13.0, 10.2), (13.6, 9.6), (13.6, 7.0), (11.0, 6.6), (10.8, 6.0), (7.6, 6.0), (7.4, 6.6), (2.4, 6.6), (0.0, 7.4)]
    m.plate(body, z0, z1, "polymer_black", bevel=0.3)
    for k in range(4):
        m.box((9.2 + k * 1.0, 7.0, z1 - 0.04), (9.8 + k * 1.0, 9.0, z1 + 0.01), "bore", faces=["south"], mesh_only=True)
    P.rail(m, 0.6, 12.4, 10.15, w=1.5)
    P.barrel(m, 13.4, 17.6, 8.8, 0.5)
    m.lathe("x", [(17.6, 0), (17.6, 0.62), (18.6, 0.62), (18.6, 0.24), (18.0, 0.24), (18.0, 0)], 8.8, Z, "steel_dark", mats=["steel_dark"] * 3 + ["bore"] * 2)
    m.mag(7.7, 6.2, 2.8, 5.4, Z - 0.8, Z + 0.8, "polymer_black", curve=0, base="polymer_black", ribs=0)
    for k in range(4):
        m.box((7.9, 5.6 - k * 1.1, Z + 0.8), (10.3, 5.8 - k * 1.1, Z + 0.84), "polymer_grey", faces=["south"], mesh_only=True)
    P.trigger_guard(m, 3.6, 6.8, 6.6, depth=1.4, r=0.2, tex="polymer_black")
    P.grip(m, 3.2, 6.7, length=4.6, rake=20, w=1.45, depth=2.2, tex="polymer_black", stipple="grip_check")
    m.box((4.8, 9.2, z0 - 0.3), (5.6, 9.8, z0), "steel_dark", r=0.1)
    # Side-folding skeleton stock
    m.box((-9.6, 9.3, Z - 0.4), (0.0, 9.9, Z + 0.4), "polymer_black", r=0.2)
    m.box((-9.6, 6.6, Z - 0.4), (-1.0, 7.2, Z + 0.4), "polymer_black", r=0.2)
    m.plate([(-10.4, 10.0), (-9.4, 10.0), (-9.4, 6.0), (-10.4, 6.0)], Z - 0.85, Z + 0.85, "polymer_black", bevel=0.2)
    P.butt_pad(m, -10.4, 5.9, 10.1, 1.8, t=0.5)
    front = P.iron_post(m, 12.6, 10.5, h=1.1)
    P.iron_rear(m, 1.2, 10.5, h=1.1)
    m.sight = (12.6, front - 0.1)
    m.stock_rear = -10.9


def thompson(m):
    z0, z1 = zs(1.6)
    m.plate([(0.0, 9.9), (11.0, 9.9), (11.0, 7.0), (0.0, 7.0)], z0, z1, "blued", bevel=0.25)
    m.box((3.0, 9.8, Z - 0.45), (6.5, 10.35, Z + 0.45), "blued", r=0.12)
    m.box((6.2, 9.95, Z - 0.3), (7.0, 10.6, Z + 0.3), "steel_light", r=0.1)                            # cocking knob
    m.plate([(5.5, 9.6), (9.0, 9.6), (9.0, 8.6), (5.5, 8.6)], z1 - 0.04, z1 + 0.01, "bore", bevel=0.0, icon=False)
    m.plate([(0.6, 7.0), (11.0, 7.0), (11.0, 6.2), (6.5, 6.2), (0.6, 6.5)], Z - 0.7, Z + 0.7, "blued", bevel=0.2)
    # Finned barrel, the Cutts compensator
    P.barrel(m, 11.0, 23.0, 8.7, 0.48)
    for k in range(14):
        x = 11.4 + k * 0.42
        m.lathe("x", [(x, 0.48), (x, 0.75), (x + 0.18, 0.75), (x + 0.18, 0.48)], 8.7, Z, "blued", icon=False)
    m.box((22.9, 8.0, Z - 0.6), (25.2, 9.6, Z + 0.6), "blued", r=0.25)
    for k in range(4):
        m.box((23.3 + k * 0.45, 9.55, Z - 0.3), (23.5 + k * 0.45, 9.62, Z + 0.3), "bore", faces=["up"], mesh_only=True)
    top = P.iron_post(m, 24.2, 9.6, h=0.6, hood=False)
    m.box((1.0, 9.9, Z - 0.6), (2.4, 10.8, Z + 0.6), "steel_dark", r=0.1)
    m.sight = (24.2, top)
    # Front pistol grip, the trigger, the drum, the stock
    P.grip(m, 15.0, 7.9, length=3.6, rake=-6, w=1.3, depth=1.8, tex="walnut", grooves=4, swell=0.05)
    m.box((13.5, 7.6, Z - 0.6), (17.0, 8.3, Z + 0.6), "blued", r=0.15)
    P.trigger_guard(m, 3.6, 6.2, 6.3, depth=1.3, r=0.2, tex="blued")
    P.grip(m, 3.4, 6.4, length=4.0, rake=26, w=1.35, depth=2.0, tex="walnut", grooves=0, swell=0.2)
    P.drum(m, 8.4, 4.0, r=2.5, depth=2.4, tex="blued")
    P.stock_loft(m, [(0.4, 9.6), (-4.0, 9.2), (-12.0, 9.0)], [(0.4, 7.0), (-2.5, 6.0), (-6.0, 6.0), (-12.0, 4.6)], [(-12.0, 1.8), (0.4, 1.4)], "walnut")
    P.butt_pad(m, -12.0, 4.5, 9.1, 1.8, t=0.35)
    m.stock_rear = -12.4


def ppsh(m):
    # Wooden stock all along, the perforated barrel jacket, the slanted compensator, the drum
    P.stock_loft(m, [(9.0, 8.6), (2.0, 8.5), (-4.0, 8.6), (-12.0, 8.6)], [(9.0, 7.2), (6.0, 6.8), (2.0, 6.6), (0.0, 5.8), (-3.0, 5.6), (-6.0, 6.2), (-12.0, 4.4)],
                 [(-12.0, 1.8), (0.0, 1.6), (9.0, 1.5)], "birch")
    m.box((-12.4, 4.3, Z - 0.85), (-12.0, 8.7, Z + 0.85), "steel_dark", r=0.12)
    m.stock_rear = -12.4
    z0, z1 = zs(1.5)
    m.plate([(-0.5, 8.4), (9.5, 8.4), (9.5, 10.0), (0.5, 10.0), (-0.5, 9.6)], z0, z1, "blued", bevel=0.2)
    m.box((3.0, 9.6, z1 - 0.1), (3.8, 10.0, z1 + 0.5), "steel_light", r=0.1)
    # The jacket: a tube with rows of long cooling slots, ending in the slanted muzzle
    m.lathe("x", [(9.5, 0), (9.5, 0.95), (19.0, 0.95), (19.0, 0)], 9.2, Z, "blued")
    for row in range(4):
        ang = -60 + row * 40
        for k in range(5):
            x = 10.2 + k * 1.6
            m.box((x, 9.2 + 0.93, Z - 0.2), (x + 1.0, 9.2 + 0.96, Z + 0.2), "bore", faces=["up"], rot=("x", ang, (x, 9.2, Z)), mesh_only=True)
    m.plate([(18.5, 8.25), (19.6, 8.25), (20.2, 10.15), (18.5, 10.15)], Z - 0.95, Z + 0.95, "blued", bevel=0.12)
    m.cyl("x", 20.0, 20.05, 9.2, Z, 0.3, "bore", mesh_only=True)
    top = P.iron_post(m, 18.0, 10.15, h=0.6)
    m.box((4.5, 10.0, Z - 0.5), (5.8, 10.6, Z + 0.5), "steel_dark", r=0.1)
    m.sight = (18.0, top)
    P.trigger_guard(m, 2.8, 5.8, 6.7, depth=1.3, r=0.2, tex="blued")
    P.drum(m, 7.6, 4.4, r=2.6, depth=2.2, tex="blued")


def mac10(m):
    z0, z1 = zs(2.0)
    m.plate([(0.0, 10.0), (9.5, 10.0), (9.5, 7.0), (0.0, 7.0)], z0, z1, "steel_dark", bevel=0.2)
    m.box((3.0, 9.95, Z - 0.3), (6.5, 10.3, Z + 0.3), "steel_light", r=0.1)
    P.barrel(m, 9.5, 11.5, 9.0, 0.5)
    P.suppressor(m, 11.5, 9.0, r=0.95, L=7.5, tex="steel_dark")
    # The mag goes up the grip
    P.grip(m, 5.6, 7.1, length=5.6, rake=4, w=1.6, depth=2.5, tex="steel_dark", grooves=0, swell=0.0, base="steel_dark")
    m.mag(4.6, 6.4, 2.0, 5.0, Z - 0.6, Z + 0.6, "steel_dark", curve=0, base="steel_dark")
    P.trigger_guard(m, 7.4, 9.6, 7.0, depth=1.3, r=0.2, tex="steel_dark")
    m.plate([(8.6, 7.0), (9.4, 7.0), (9.4, 5.9), (9.0, 5.9)], Z - 0.4, Z + 0.4, "steel_dark", bevel=0.05)
    # Wire stock, extended
    for zz in (Z - 0.7, Z + 0.7):
        m.pipe([(0.0, 7.5, zz), (-6.0, 7.6, zz), (-9.0, 7.4, zz)], 0.14, "steel")
    m.pipe([(-9.0, 9.6, Z - 0.75), (-9.0, 5.6, Z - 0.75), (-9.0, 5.6, Z + 0.75), (-9.0, 9.6, Z + 0.75)], 0.16, "steel")
    m.box((-9.3, 5.4, Z - 0.9), (-8.8, 9.8, Z + 0.9), "rubber", r=0.15)
    front = P.iron_post(m, 9.0, 10.0, h=0.7)
    m.sight = (9.0, front)
    m.stock_rear = -9.3


def mp40(m):
    z0, z1 = zs(1.5)
    # Round receiver tube, the stamped lower with its grip frame, the bakelite grip, the barrel and its rest hook
    m.lathe("x", [(0.0, 0), (0.0, 0.85), (0.3, 0.95), (11.5, 0.95), (11.5, 0)], 9.3, Z, "blued")
    m.plate([(4.0, 9.9), (9.0, 9.9), (9.0, 8.8), (4.0, 8.8)], z1 - 0.3, z1 - 0.1, "bore", bevel=0.0, icon=False)
    m.box((2.5, 8.9, z1 - 0.2), (3.3, 9.6, z1 + 0.55), "steel_light", r=0.12)
    m.plate([(0.6, 8.4), (11.0, 8.4), (11.0, 7.2), (6.6, 7.0), (1.2, 7.2)], Z - 0.7, Z + 0.7, "blued", bevel=0.15)
    for k in range(4):
        m.box((7.0 + k * 0.9, 7.4, Z + 0.7), (7.5 + k * 0.9, 8.2, Z + 0.72), "bore", faces=["south"], mesh_only=True)
    P.barrel(m, 11.5, 19.0, 9.3, 0.42)
    m.lathe("x", [(11.5, 0), (11.5, 0.6), (13.0, 0.6), (13.0, 0)], 9.3, Z, "blued")
    m.box((12.0, 7.6, Z - 0.3), (12.8, 9.0, Z + 0.3), "blued", r=0.1)
    m.lathe("x", [(18.4, 0.42), (18.4, 0.55), (19.0, 0.55), (19.0, 0.42)], 9.3, Z, "blued", icon=False)
    top = P.iron_post(m, 18.6, 9.85, h=0.6)
    m.box((1.5, 10.1, Z - 0.5), (2.6, 10.8, Z + 0.5), "steel_dark", r=0.1)
    m.sight = (18.6, top)
    m.mag(8.8, 7.2, 1.7, 7.0, Z - 0.6, Z + 0.6, "blued", curve=0, base="steel_dark", ribs=5, rib_tex="steel_dark")
    m.box((8.4, 6.6, Z - 0.75), (10.9, 7.4, Z + 0.75), "blued", r=0.15)
    P.trigger_guard(m, 4.3, 7.0, 7.1, depth=1.3, r=0.2, tex="blued")
    P.grip(m, 3.7, 7.2, length=4.2, rake=22, w=1.4, depth=2.1, tex="bakelite", grooves=0, swell=0.15)
    # Underfolding stock, extended
    for zz in (Z - 0.55, Z + 0.55):
        m.pipe([(0.6, 7.6, zz), (-5.0, 7.0, zz), (-9.5, 6.6, zz)], 0.15, "blued")
    m.plate([(-10.0, 9.3), (-9.4, 9.3), (-9.4, 5.6), (-10.0, 5.6)], Z - 0.8, Z + 0.8, "blued", bevel=0.12)
    m.stock_rear = -10.0


# ===============================================================================================================
# Rifles: SCAR-L, G36, FAMAS, L85A2, Tavor, FAL, M1 Garand
# ===============================================================================================================
def scar_l(m):
    z0, z1 = zs(2.1)
    # The tall upper with its long top rail and side rails, the polymer lower
    m.box((0.0, 8.4, z0), (16.0, 10.6, z1), "alu_tan", r=0.3)
    P.rail(m, 0.2, 15.8, 10.55, w=1.6)
    P.rail(m, 11.0, 15.6, 9.4, w=1.1, axis="south", z=z1 - 0.02)
    P.rail(m, 11.0, 15.6, 9.4, w=1.1, axis="north", z=z0 + 0.02)
    P.rail(m, 11.4, 15.6, 8.4, w=1.3, axis="down")
    m.plate([(3.0, 9.0), (7.4, 9.0), (7.4, 10.1), (3.0, 10.1)], z1 - 0.04, z1 + 0.01, "bore", bevel=0.0, icon=False, corner=0.1)
    m.box((3.2, 9.15, z1 - 0.3), (7.2, 9.95, z1 + 0.03), "steel_light", r=0.12, mesh_only=True)
    m.box((8.6, 9.1, z0 - 0.6), (9.4, 9.8, z0), "steel_dark", r=0.15)                          # charging handle (left)
    lower = [(0.0, 8.4), (11.2, 8.4), (11.0, 6.4), (10.6, 4.4), (7.4, 4.4), (7.3, 6.2), (5.0, 6.3), (0.6, 7.0)]
    m.plate(lower, *zs(1.9), "polymer_fde", bevel=0.3)
    P.trigger_guard(m, 4.8, 7.2, 6.3, depth=1.4, r=0.2, tex="polymer_fde")
    P.grip(m, 4.6, 6.4, length=4.7, rake=22, w=1.45, depth=2.2, tex="polymer_fde", stipple="grip_check_tan")
    m.mag(7.6, 4.8, 2.8, 5.8, Z - 0.75, Z + 0.75, "alu_tan", curve=1, base="polymer_black", ribs=3, rib_tex="alu_tan")
    P.barrel(m, 16.0, 26.0, 9.3, 0.56)
    m.lathe("x", [(16.0, 0), (16.0, 0.85), (17.5, 0.85), (17.5, 0)], 9.3, Z, "steel_dark")
    P.flash_hider(m, 26.0, 9.3, r=0.62, L=1.9, prongs=3)
    # Side-folding adjustable stock
    m.plate([(-13.0, 10.6), (-6.0, 10.4), (-5.4, 10.0), (0.0, 10.0), (0.0, 8.0), (-5.4, 8.0), (-8.0, 7.0), (-13.0, 5.4)], Z - 0.9, Z + 0.9, "polymer_fde", bevel=0.35)
    m.plate([(-11.4, 9.4), (-6.4, 9.2), (-8.2, 7.6), (-11.0, 6.6)], Z - 0.95, Z + 0.95, "bore", bevel=0.0, icon=False)
    P.butt_pad(m, -13.0, 5.3, 10.7, 1.9, t=0.7)
    front = P.iron_post(m, 15.0, 10.95, h=1.2)
    P.iron_rear(m, 0.9, 10.95, h=1.2)
    m.sight = (15.0, front - 0.1)
    m.stock_rear = -13.7


def g36(m):
    z0, z1 = zs(2.0)
    # One polymer body: the carry handle with its optic on top, the see-through mag
    body = [(-0.6, 10.4), (13.0, 10.4), (14.4, 9.6), (19.0, 9.2), (19.0, 7.4), (13.0, 7.2), (11.4, 6.6), (11.2, 4.5), (8.0, 4.5), (7.9, 6.4),
            (6.2, 6.5), (2.0, 6.6), (-0.6, 7.6)]
    m.plate(body, z0, z1, "polymer_black", bevel=0.35, corner=0.3)
    for k in range(5):
        m.box((14.0 + k * 1.0, 7.8, z1 - 0.04), (14.5 + k * 1.0, 8.9, z1 + 0.01), "bore", faces=["south"], mesh_only=True)
    m.plate([(0.4, 10.3), (11.0, 10.3), (11.0, 12.6), (9.0, 12.6), (8.0, 11.0), (2.4, 11.0), (1.6, 12.6), (0.4, 12.6)], Z - 0.65, Z + 0.65, "polymer_black", bevel=0.25)
    m.lathe("x", [(1.0, 0.5), (1.0, 0.7), (8.8, 0.7), (8.8, 0.5)], 12.4, Z, "polymer_black")
    m.lathe("x", [(8.6, 0.6), (8.6, 0)], 12.4, Z, "lens", icon=False)
    m.plate([(4.0, 9.4), (7.6, 9.4), (7.6, 10.1), (4.0, 10.1)], z1 - 0.04, z1 + 0.01, "bore", bevel=0.0, icon=False)
    m.box((10.4, 10.2, Z - 0.3), (11.2, 10.6, z1 + 0.6), "polymer_black", r=0.15)
    P.barrel(m, 19.0, 26.0, 8.9, 0.52)
    P.flash_hider(m, 26.0, 8.9, r=0.62, L=1.8, prongs=4)
    m.mag(8.1, 4.9, 2.9, 5.6, Z - 0.75, Z + 0.75, "lens", curve=1, base="polymer_black")
    for k in range(6):
        m.lathe("z", [(Z - 0.6, 0), (Z - 0.6, 0.2), (Z + 0.6, 0.2), (Z + 0.6, 0)], 9.6 - k * 0.35, 4.5 - k * 0.0, "brass", icon=False) if False else None
    P.trigger(m, 6.6, 6.5, "polymer_black")
    P.grip(m, 5.8, 6.55, length=4.6, rake=18, w=1.45, depth=2.2, tex="polymer_black", stipple="grip_check")
    # The folding skeleton stock
    m.plate([(-12.6, 10.4), (-0.6, 10.4), (-0.6, 9.4), (-9.0, 9.4), (-9.6, 7.2), (-0.6, 7.6), (-0.6, 6.8), (-10.6, 6.4), (-12.6, 5.6)],
            Z - 0.8, Z + 0.8, "polymer_black", bevel=0.25)
    P.butt_pad(m, -12.6, 5.5, 10.5, 1.8, t=0.5)
    m.sight = (8.6, 12.4)
    m.stock_rear = -13.1


def bullpup_famas(m):
    z0, z1 = zs(2.0)
    body = [(-9.0, 5.4), (4.0, 5.4), (4.6, 6.6), (14.0, 7.0), (16.5, 8.2), (16.5, 9.6), (-8.6, 10.0), (-9.0, 9.4)]
    m.plate(body, z0, z1, "polymer_grey", bevel=0.4)
    # The long carry handle over the whole top, with sights at each end
    m.plate([(-6.0, 9.8), (14.0, 9.8), (14.4, 12.6), (12.4, 12.8), (11.8, 10.8), (-3.0, 10.8), (-3.6, 12.8), (-6.0, 12.6)], Z - 0.5, Z + 0.5, "polymer_grey", bevel=0.2)
    m.box((-3.4, 12.6, Z - 0.5), (12.2, 13.0, Z + 0.5), "polymer_grey", r=0.15)
    m.box((-5.2, 10.6, Z - 0.3), (-4.0, 13.6, Z + 0.3), "polymer_black", r=0.1)
    m.box((-5.6, 9.8, Z - 0.3), (-4.2, 10.6, Z + 0.3), "polymer_black", r=0.1)
    m.mag(-4.6, 5.6, 2.6, 4.2, Z - 0.75, Z + 0.75, "steel_dark", curve=0, base="steel_dark", ribs=2)
    P.grip(m, 5.6, 6.6, length=4.4, rake=18, w=1.45, depth=2.1, tex="polymer_black", stipple="grip_check")
    m.plate([(3.6, 6.6), (11.6, 6.6), (11.6, 5.8), (4.4, 4.6), (3.6, 4.6)], Z - 0.45, Z + 0.45, "polymer_grey", bevel=0.15)    # full-hand guard
    P.trigger(m, 7.2, 6.6, "polymer_black")
    P.barrel(m, 16.5, 25.0, 8.9, 0.5)
    m.lathe("x", [(25.0, 0), (25.0, 0.62), (26.6, 0.62), (26.6, 0.3), (25.8, 0.3), (25.8, 0)], 8.9, Z, "steel_dark", mats=["steel_dark"] * 3 + ["bore"] * 2)
    m.box((0.0, 9.6, z1), (1.0, 10.3, z1 + 0.4), "steel_dark", r=0.1)
    # Bipod legs folded along the sides
    for s in (-1, 1):
        m.box((8.0, 8.4, Z + s * 1.05 - 0.15), (16.0, 8.8, Z + s * 1.05 + 0.15), "steel_dark", r=0.1)
    P.butt_pad(m, -9.0, 5.3, 10.1, 2.1, t=0.6)
    m.sight = (13.4, 13.3)
    top = P.iron_post(m, 13.4, 12.8, h=0.6, hood=False)
    m.sight = (13.4, top)
    m.stock_rear = -9.6


def l85(m):
    z0, z1 = zs(1.9)
    rec = [(-9.0, 5.6), (-1.0, 5.6), (-1.0, 6.6), (12.0, 6.6), (12.0, 10.0), (-8.8, 10.0), (-9.0, 9.4)]
    m.plate(rec, z0, z1, "steel_dark", bevel=0.25)
    m.plate([(-5.0, 8.6), (-1.0, 8.6), (-1.0, 9.6), (-5.0, 9.6)], z1 - 0.04, z1 + 0.01, "bore", bevel=0.0, icon=False)
    m.box((-0.2, 8.8, z1), (0.6, 9.4, z1 + 0.6), "steel_light", r=0.12)
    m.mag(-5.6, 5.8, 2.8, 5.2, Z - 0.75, Z + 0.75, "steel_dark", curve=1, base="polymer_black", ribs=3)
    P.grip(m, 3.6, 6.7, length=4.6, rake=20, w=1.45, depth=2.2, tex="polymer_black", stipple="grip_check")
    P.trigger_guard(m, 3.8, 6.8, 6.6, depth=1.5, r=0.2, tex="polymer_black")
    P.wood_handguard(m, 12.0, 19.0, 8.7, 1.25, 1.15, "polymer_black", grooves=0)
    for k in range(5):
        m.box((13.0 + k * 1.1, 8.3, Z + 1.13), (13.5 + k * 1.1, 9.3, Z + 1.16), "bore", faces=["south"], mesh_only=True)
    P.barrel(m, 19.0, 25.5, 8.9, 0.5)
    P.flash_hider(m, 25.5, 8.9, r=0.6, L=1.8, prongs=4)
    P.butt_pad(m, -9.0, 5.5, 10.1, 2.0, t=0.6)
    m.box((-7.5, 10.0, Z - 0.7), (-1.5, 10.8, Z + 0.7), "polymer_black", r=0.3)                       # cheek piece
    # SUSAT: the boxy 4x scope on its mount
    m.box((2.0, 10.0, Z - 0.8), (8.0, 10.6, Z + 0.8), "steel_dark", r=0.15)
    m.plate([(1.2, 10.6), (9.4, 10.6), (9.4, 12.4), (8.8, 13.0), (1.8, 13.0), (1.2, 12.4)], Z - 1.0, Z + 1.0, "alu", bevel=0.35)
    m.lathe("x", [(9.4, 0), (9.4, 0.85), (10.2, 0.95), (10.2, 0.8)], 11.8, Z, "alu")
    m.lathe("x", [(10.0, 0.8), (10.0, 0)], 11.8, Z, "lens", icon=False)
    m.lathe("x", [(0.4, 0.55), (0.4, 0.75), (1.2, 0.75), (1.2, 0.55)], 11.8, Z, "rubber")
    m.lathe("x", [(0.5, 0), (0.5, 0.55)], 11.8, Z, "lens", icon=False)
    m.sight = (10.0, 11.8)
    m.stock_rear = -9.6


def tavor(m):
    z0, z1 = zs(2.1)
    body = [(-9.0, 5.2), (-1.0, 5.2), (0.6, 6.4), (5.0, 6.6), (6.0, 4.6), (14.0, 4.6), (16.6, 6.4), (17.0, 7.6), (17.0, 9.8), (-8.8, 10.3), (-9.0, 9.6)]
    m.plate(body, z0, z1, "polymer_black", bevel=0.5, corner=0.4)
    m.plate([(6.4, 5.2), (13.4, 5.2), (14.6, 6.4), (6.0, 6.6)], Z - 1.1, Z + 1.1, "bore", bevel=0.0, icon=False)       # the long guard's opening
    P.rail(m, -6.0, 16.6, 10.0, w=1.6)
    m.mag(-5.0, 5.4, 2.8, 5.6, Z - 0.75, Z + 0.75, "polymer_black", curve=1, base="polymer_black", ribs=3)
    P.grip(m, 9.0, 6.4, length=4.4, rake=14, w=1.45, depth=2.1, tex="polymer_black", stipple="grip_check")
    P.barrel(m, 17.0, 24.0, 8.9, 0.52)
    P.flash_hider(m, 24.0, 8.9, r=0.6, L=1.8, prongs=4)
    m.box((14.8, 9.8, z1 - 0.2), (15.8, 10.3, z1 + 0.5), "steel_dark", r=0.1)
    P.butt_pad(m, -9.0, 5.1, 10.4, 2.2, t=0.6)
    cy = P.red_dot(m, 6.0, 10.35, big=True)
    m.sight = (7.4, cy)
    m.stock_rear = -9.6


def fal(m):
    z0, z1 = zs(1.85)
    m.plate([(0.0, 10.2), (12.5, 10.2), (12.5, 7.0), (11.0, 6.4), (7.6, 6.4), (7.4, 7.0), (0.4, 7.0), (0.0, 7.6)], z0, z1, "parkerized", bevel=0.2)
    m.plate([(3.6, 9.0), (7.6, 9.0), (7.6, 9.9), (3.6, 9.9)], z1 - 0.04, z1 + 0.01, "bore", bevel=0.0, icon=False)
    m.box((8.2, 9.2, z0 - 0.7), (9.0, 9.8, z0), "steel_dark", r=0.15)                           # left charging handle
    m.box((0.6, 10.2, Z - 0.6), (4.0, 10.6, Z + 0.6), "parkerized", r=0.12)
    P.iron_rear(m, 1.6, 10.6, h=0.9)
    P.wood_handguard(m, 12.5, 19.5, 9.0, 1.15, 1.15, "polymer_black", grooves=0)
    for k in range(6):
        m.box((13.0 + k * 1.0, 9.4, Z + 1.13), (13.5 + k * 1.0, 10.0, Z + 1.16), "bore", faces=["south"], mesh_only=True)
    P.barrel(m, 19.5, 28.0, 9.2, 0.55)
    m.box((19.4, 8.6, Z - 0.6), (21.0, 10.8, Z + 0.6), "parkerized", r=0.2)
    top = P.iron_post(m, 27.0, 9.75, h=1.0)
    P.flash_hider(m, 28.0, 9.2, r=0.62, L=2.0, prongs=4)
    m.mag(7.7, 6.6, 3.1, 4.8, Z - 0.8, Z + 0.8, "steel_dark", curve=0, base="steel_dark", ribs=3)
    P.trigger_guard(m, 3.8, 7.0, 7.0, depth=1.5, r=0.2, tex="parkerized")
    P.grip(m, 3.4, 7.0, length=4.6, rake=24, w=1.45, depth=2.2, tex="polymer_black", stipple="grip_check")
    P.stock_loft(m, [(0.2, 9.9), (-5.0, 9.6), (-13.5, 9.6)], [(0.2, 7.2), (-4.0, 6.4), (-13.5, 4.6)], [(-13.5, 1.9), (0.2, 1.6)], "polymer_black")
    P.butt_pad(m, -13.5, 4.5, 9.7, 2.0, t=0.5)
    m.sight = (27.0, top - 0.1)
    m.stock_rear = -14.0


def m1_garand(m):
    # Walnut stock the whole length, the receiver with its op-rod, the en-bloc clip showing
    P.stock_loft(m, [(16.0, 9.3), (8.0, 9.2), (2.0, 9.2), (-4.0, 9.0), (-13.5, 9.2)], [(16.0, 8.0), (9.0, 7.4), (6.0, 7.2), (2.0, 6.6), (0.0, 5.6), (-3.0, 5.6), (-7.0, 6.4), (-13.5, 4.8)],
                 [(-13.5, 1.9), (-2.0, 1.7), (6.0, 1.9), (16.0, 1.6)], "walnut")
    m.box((-13.9, 4.7, Z - 0.9), (-13.5, 9.3, Z + 0.9), "parkerized", r=0.12)
    z0, z1 = zs(1.5)
    m.plate([(0.0, 9.1), (8.0, 9.1), (8.0, 10.4), (1.0, 10.4), (0.0, 10.0)], z0, z1, "parkerized", bevel=0.2)
    m.box((2.8, 10.3, Z - 0.6), (6.6, 10.5, Z + 0.6), "brass", r=0.05)                            # the clip's top, with rounds
    for k in range(4):
        P.cartridge(m, 3.0, 10.35, Z - 0.45 + k * 0.3, length=0.5, r=0.13, axis="y") if False else None
        m.lathe("x", [(3.0, 0), (3.0, 0.14), (6.4, 0.14), (6.6, 0)], 10.45, Z - 0.42 + k * 0.28, "brass", icon=False)
    m.box((4.0, 9.4, z1), (11.0, 9.9, z1 + 0.3), "parkerized", r=0.1)                              # op rod
    m.box((3.6, 9.3, z1), (4.4, 10.0, z1 + 0.7), "parkerized", r=0.12)
    P.iron_rear(m, 0.6, 10.4, h=1.1)
    m.lathe("x", [(15.5, 0), (15.5, 0.62), (21.0, 0.62), (21.0, 0)], 10.0, Z, "walnut")          # upper handguard
    P.barrel(m, 8.0, 29.0, 9.6, 0.55)
    m.box((27.0, 8.6, Z - 0.6), (28.6, 10.4, Z + 0.6), "parkerized", r=0.2)
    top = P.iron_post(m, 28.0, 10.4, h=0.9)
    P.trigger_guard(m, 3.0, 6.4, 7.2, depth=1.4, r=0.2, tex="parkerized")
    P.sling_loop(m, 14.0, 7.6, Z, r=0.5, axis="x")
    P.sling_loop(m, -7.0, 6.1, Z, r=0.5, axis="x")
    m.sight = (28.0, top - 0.1)
    m.stock_rear = -13.9


def mk14(m):
    # An M14 in an aluminium chassis: adjustable stock, pistol grip, rails everywhere
    z0, z1 = zs(1.7)
    m.plate([(0.0, 9.0), (10.0, 9.0), (10.0, 10.4), (1.0, 10.4), (0.0, 10.0)], z0, z1, "parkerized", bevel=0.2)
    m.box((4.0, 9.3, z1), (12.0, 9.8, z1 + 0.3), "parkerized", r=0.1)
    m.box((4.0, 9.2, z1), (4.8, 10.0, z1 + 0.7), "parkerized", r=0.12)
    chassis = [(-0.5, 9.0), (21.0, 9.0), (21.0, 7.2), (11.4, 7.0), (11.2, 6.2), (7.8, 6.2), (7.6, 7.0), (0.0, 7.0)]
    m.plate(chassis, *zs(2.3), "alu_tan", bevel=0.3)
    P.rail(m, -0.2, 21.0, 10.4, w=1.6)
    m.box((-0.2, 10.0, Z - 0.9), (10.0, 10.45, Z + 0.9), "alu_tan", r=0.12)
    m.box((10.0, 9.0, Z - 0.9), (21.0, 10.45, Z + 0.9), "alu_tan", r=0.2)
    P.rail(m, 12.0, 20.8, 8.1, w=1.1, axis="south", z=Z + 1.13)
    P.rail(m, 12.0, 20.8, 7.0, w=1.3, axis="down")
    P.barrel(m, 21.0, 28.5, 9.4, 0.58)
    P.flash_hider(m, 28.5, 9.4, r=0.65, L=1.9, prongs=4)
    m.mag(7.9, 6.4, 3.1, 4.6, Z - 0.8, Z + 0.8, "steel_dark", curve=0, base="steel_dark", ribs=3)
    P.trigger_guard(m, 4.0, 7.2, 7.0, depth=1.5, r=0.2, tex="alu_tan")
    P.grip(m, 3.6, 7.0, length=4.6, rake=22, w=1.45, depth=2.2, tex="polymer_black", stipple="grip_check")
    m.lathe("x", [(-7.0, 0), (-7.0, 0.62), (0.0, 0.62), (0.0, 0)], 8.6, Z, "alu_tan")
    m.plate([(-13.0, 10.6), (-6.5, 10.4), (-6.0, 9.6), (-6.0, 7.6), (-8.4, 7.0), (-13.0, 5.4)], Z - 0.95, Z + 0.95, "polymer_black", bevel=0.35)
    P.butt_pad(m, -13.0, 5.3, 10.7, 1.9, t=0.7)
    cy = P.scope(m, 1.0, 11.0, 12.8, r=0.62, bell_front=1.45, bell_rear=1.3, base_y=10.8)
    m.sight = (11.0, cy)
    m.stock_rear = -13.7


# ===============================================================================================================
# Snipers: M82, Kar98k, Winchester 1873
# ===============================================================================================================
def m82(m):
    # The Barrett: a big two-part receiver, the long barrel with its arrowhead brake, a bipod, a giant scope
    z0, z1 = zs(2.3)
    upper = [(-2.0, 9.0), (14.0, 9.0), (16.0, 9.6), (16.0, 10.8), (-2.0, 10.8)]
    m.plate(upper, z0, z1, "parkerized", bevel=0.3)
    for k in range(5):
        m.cyl("z", z0 - 0.02, z1 + 0.02, 9.4 + k * 1.2, 10.2, 0.32, "bore", mesh_only=True)
    lower = [(-8.0, 9.0), (11.0, 9.0), (11.0, 7.0), (9.6, 6.6), (6.4, 6.6), (6.2, 7.0), (2.0, 7.1), (-0.5, 7.6), (-8.0, 7.6)]
    m.plate(lower, *zs(2.0), "parkerized", bevel=0.3)
    P.rail(m, -1.5, 13.5, 10.75, w=1.7)
    P.barrel(m, 16.0, 26.0, 9.9, 0.75)
    m.lathe("x", [(16.0, 0), (16.0, 0.9), (17.0, 0.9), (17.0, 0)], 9.9, Z, "parkerized")
    # The arrowhead brake: two flared side plates around the bore
    m.lathe("x", [(26.0, 0), (26.0, 0.7), (29.6, 0.7), (29.6, 0)], 9.9, Z, "parkerized")
    for s2 in (-1, 1):
        m.plate([(26.4, 9.0), (29.2, 8.5), (30.4, 9.9), (29.2, 11.3), (26.4, 10.8)], Z + s2 * 0.75 - 0.18, Z + s2 * 0.75 + 0.18, "parkerized", bevel=0.08)
    m.cyl("x", 29.6, 29.62, 9.9, Z, 0.4, "bore", mesh_only=True)
    m.mag(6.6, 6.9, 3.0, 3.4, Z - 0.9, Z + 0.9, "parkerized", curve=0, base="steel_dark", ribs=2)
    P.trigger_guard(m, 2.4, 5.8, 7.1, depth=1.5, r=0.22, tex="parkerized")
    P.grip(m, 2.0, 7.2, length=4.6, rake=20, w=1.5, depth=2.3, tex="polymer_black", stipple="grip_check")
    # Stock: the tube, the monopod, a fat recoil pad
    m.lathe("x", [(-13.5, 0), (-13.5, 0.8), (-8.0, 0.8), (-8.0, 0)], 8.6, Z, "parkerized")
    m.plate([(-14.0, 10.0), (-8.0, 10.0), (-8.0, 9.4), (-14.0, 9.4)], Z - 0.9, Z + 0.9, "polymer_black", bevel=0.3)
    P.butt_pad(m, -13.5, 5.4, 10.4, 2.2, t=1.0)
    m.box((-12.6, 5.0, Z - 0.3), (-11.6, 7.8, Z + 0.3), "parkerized", r=0.12)
    m.cyl("y", 3.8, 5.0, -12.1, Z, 0.28, "rubber")
    # Bipod, folded forward under the barrel
    m.box((14.5, 8.4, Z - 1.0), (16.0, 9.0, Z + 1.0), "parkerized", r=0.15)
    for s2 in (-1, 1):
        m.pipe([(15.2, 8.6, Z + s2 * 0.9), (24.0, 8.7, Z + s2 * 0.9)], 0.2, "parkerized")
        m.box((23.5, 8.2, Z + s2 * 0.9 - 0.4), (24.5, 9.0, Z + s2 * 0.9 + 0.4), "rubber", r=0.2)
    m.box((9.0, 10.8, Z - 0.7), (11.0, 12.2, Z + 0.7), "parkerized", r=0.2)                 # carry handle
    cy = P.scope(m, -0.5, 12.5, 13.6, r=0.72, bell_front=1.6, bell_rear=1.3, base_y=11.2, sunshade=1.5)
    m.sight = (12.5, cy)
    m.stock_rear = -14.5


def kar98k(m):
    P.stock_loft(m, [(17.0, 8.9), (6.0, 8.9), (0.0, 8.9), (-5.0, 8.6), (-13.5, 8.9)],
                 [(17.0, 7.9), (8.0, 7.4), (3.0, 7.1), (0.5, 5.8), (-2.5, 5.5), (-6.0, 6.2), (-13.5, 4.6)],
                 [(-13.5, 1.8), (-2.0, 1.55), (6.0, 1.75), (17.0, 1.5)], "walnut")
    m.box((-13.9, 4.5, Z - 0.85), (-13.5, 9.0, Z + 0.85), "parkerized", r=0.12)
    # Receiver, the bolt with its turned-down handle, the stripper clip guide
    m.lathe("x", [(0.0, 0), (0.0, 0.62), (0.3, 0.7), (8.0, 0.7), (8.0, 0)], 9.45, Z, "blued")
    m.lathe("x", [(-1.6, 0), (-1.6, 0.45), (-0.4, 0.5), (0.0, 0.5), (0.0, 0)], 9.45, Z, "blued")
    m.plate([(-1.4, 9.9), (-0.6, 10.4), (-0.2, 9.9)], Z - 0.12, Z + 0.12, "steel_light", bevel=0.04)   # safety flag
    m.pipe([(1.0, 9.45, Z + 0.55), (0.9, 8.9, Z + 1.2), (0.7, 8.3, Z + 1.45)], 0.15, "blued")
    P.knob(m, 0.65, 8.1, Z + 1.5, r=0.42, tex="blued")
    P.barrel(m, 8.0, 29.5, 9.45, 0.52, steps=[(9.0, 0.6), (11.0, 0.52)])
    # Bands, the upper handguard, the front sight hood, the cleaning rod
    m.lathe("x", [(13.0, 0), (13.0, 0.55), (20.0, 0.55), (20.0, 0)], 9.95, Z, "walnut")
    for bx in (16.5, 23.0):
        m.lathe("x", [(bx, 0.6), (bx, 1.3), (bx + 0.6, 1.3), (bx + 0.6, 0.6)], 8.9, Z, "blued")
    m.box((26.2, 7.9, Z - 0.75), (27.4, 9.8, Z + 0.75), "blued", r=0.2)
    m.cyl("x", 17.0, 27.0, 8.25, Z, 0.16, "steel_light")
    top = P.iron_post(m, 28.8, 9.95, h=0.7, hood=True)
    m.box((8.6, 9.95, Z - 0.5), (10.6, 10.5, Z + 0.5), "blued", r=0.1)
    m.box((9.8, 10.4, Z - 0.4), (10.4, 10.8, Z + 0.4), "blued", r=0.06)
    P.trigger_guard(m, 1.8, 5.0, 7.1, depth=1.3, r=0.18, tex="blued")
    P.sling_loop(m, -7.0, 5.9, Z, r=0.45, axis="x")
    m.sight = (28.8, top - 0.05)
    m.stock_rear = -13.9


def winchester(m):
    # The 1873: a brass-framed lever gun, octagon barrel, the tube magazine underneath, the loop lever
    z0, z1 = zs(1.6)
    m.plate([(0.0, 6.8), (7.4, 6.8), (8.6, 7.6), (8.6, 10.2), (0.6, 10.2), (0.0, 9.6)], z0, z1, "brass", bevel=0.25)
    m.plate([(1.8, 9.0), (6.6, 9.0), (6.6, 10.25), (1.8, 10.25)], Z - 0.5, Z + 0.5, "blued", bevel=0.1)        # dust cover
    m.plate([(3.6, 8.0), (6.6, 8.0), (6.6, 8.8), (3.6, 8.8)], z1 - 0.04, z1 + 0.01, "bore", bevel=0.0, icon=False)   # loading gate
    m.plate([(-0.6, 10.4), (0.2, 10.3), (0.8, 9.2), (0.3, 9.0), (-0.3, 9.8)], Z - 0.28, Z + 0.28, "blued", bevel=0.08)   # hammer
    # Octagon barrel (an 8-sided lathe), the magazine tube
    m.lathe("x", [(8.6, 0), (8.6, 0.68), (29.0, 0.68), (29.0, 0.32), (28.4, 0.32), (28.4, 0)], 9.55, Z, "blued", seg=8, mats=["blued"] * 3 + ["bore"] * 2)
    m.lathe("x", [(8.6, 0), (8.6, 0.48), (27.0, 0.48), (27.2, 0.4), (27.2, 0)], 8.35, Z, "blued")
    m.box((26.6, 8.0, Z - 0.35), (27.4, 9.6, Z + 0.35), "blued", r=0.12)
    m.sweep([(8.6 + k * 0.6, 8.6) for k in range(12)], 1.0, 0.75, "walnut", rad=0.7)
    top = P.iron_post(m, 28.4, 10.2, h=0.6, hood=False)
    m.box((12.0, 10.2, Z - 0.5), (13.2, 10.8, Z + 0.5), "blued", r=0.1)
    # The loop lever and trigger
    lever = [(4.6, 6.8, Z), (5.0, 5.8, Z), (4.4, 4.8, Z), (2.6, 4.6, Z), (1.4, 5.2, Z), (1.4, 5.9, Z), (2.4, 6.0, Z), (3.8, 5.9, Z), (4.2, 6.6, Z)]
    m.pipe(spline_pts(lever), 0.2, "blued")
    P.trigger(m, 3.4, 6.8, "blued", size=0.8)
    P.stock_loft(m, [(0.3, 9.6), (-3.0, 9.2), (-13.0, 9.6)], [(0.3, 6.8), (-1.0, 6.2), (-4.0, 6.0), (-13.0, 4.8)], [(-13.0, 1.8), (0.3, 1.5)], "walnut")
    m.box((-13.4, 4.7, Z - 0.85), (-13.0, 9.7, Z + 0.85), "brass", r=0.12)
    m.sight = (28.4, top)
    m.stock_rear = -13.4


def spline_pts(pts, per=4):
    return [tuple(p) for p in spline(pts, per)]


# ===============================================================================================================
# More shotguns: M1014, SPAS-12, AA-12, Sawn-off
# ===============================================================================================================
def m1014(m):
    pump(m, barrel_end=28.0, tube_end=23.0, stock="polymer", finish="steel_dark", forend_x=12.0, ghost=True, rail=True, shells=0)


def spas12(m):
    pump(m, barrel_end=27.0, tube_end=23.5, stock="polymer", finish="parkerized", forend_x=13.0, ghost=True, rail=False, shells=0)
    # The SPAS's perforated heat shield over the barrel and its folding stock with the hook
    m.lathe("x", [(19.5, 0.62), (19.5, 0.9), (26.0, 0.9), (26.0, 0.62)], 9.6, Z, "parkerized")
    for k in range(6):
        for ang in (-35, 0, 35):
            x = 20.0 + k * 1.0
            m.box((x, 9.6 + 0.88, Z - 0.18), (x + 0.5, 9.6 + 0.92, Z + 0.18), "bore", faces=["up"], rot=("x", ang, (x, 9.6, Z)), mesh_only=True)
    m.plate([(-8.0, 10.4), (-7.0, 10.4), (-7.4, 8.6), (-8.6, 8.0)], Z - 0.3, Z + 0.3, "parkerized", bevel=0.08)


def aa12(m):
    z0, z1 = zs(2.2)
    body = [(-10.0, 5.6), (-8.0, 7.0), (0.0, 7.4), (2.0, 6.4), (12.0, 6.4), (13.0, 7.0), (19.0, 7.2), (19.0, 10.8), (-10.0, 10.8)]
    m.plate(body, z0, z1, "steel_dark", bevel=0.4)
    m.box((-6.0, 10.8, Z - 0.7), (16.0, 12.2, Z + 0.7), "steel_dark", r=0.35)                  # the carry handle rail
    P.rail(m, -4.0, 15.0, 12.15, w=1.5)
    P.butt_pad(m, -10.0, 5.5, 10.9, 2.2, t=0.7)
    P.barrel(m, 19.0, 26.0, 9.4, 0.72, bore=0.45)
    m.lathe("x", [(19.0, 0), (19.0, 1.0), (24.5, 1.0), (24.5, 0)], 9.4, Z, "steel_dark")
    for k in range(6):
        m.cyl("z", Z - 1.02, Z + 1.02, 19.8 + k * 0.8, 9.4, 0.22, "bore", mesh_only=True)
    P.grip(m, 3.0, 6.5, length=4.6, rake=18, w=1.5, depth=2.3, tex="polymer_black", stipple="grip_check")
    P.trigger_guard(m, 3.4, 6.6, 6.45, depth=1.5, r=0.22, tex="steel_dark")
    P.drum(m, 9.6, 3.4, r=2.8, depth=2.8, tex="polymer_black")
    m.box((7.6, 5.4, Z - 0.8), (11.6, 6.6, Z + 0.8), "polymer_black", r=0.2)
    m.box((-1.0, 9.0, z1), (0.2, 9.8, z1 + 0.6), "steel_light", r=0.12)
    front = P.iron_post(m, 15.4, 12.2, h=0.8)
    P.iron_rear(m, -3.0, 12.2, h=0.8)
    m.sight = (15.4, front - 0.05)
    m.stock_rear = -10.7


def sawnoff(m):
    # A double barrel cut down: short barrels, a pistol-grip stock cut off behind the wrist
    z0, z1 = zs(2.0)
    m.plate([(0.0, 7.6), (6.0, 7.6), (6.6, 8.3), (6.6, 10.3), (0.4, 10.6), (0.0, 10.0)], z0, z1, "blued", bevel=0.3)
    for zz in (Z - 0.52, Z + 0.52):
        P.barrel(m, 6.6, 16.0, 9.6, 0.62, bore=0.42)
    m.box((6.6, 9.4, Z - 0.3), (16.0, 10.5, Z + 0.3), "blued", r=0.1)
    m.box((6.6, 8.3, Z - 0.9), (12.0, 9.2, Z + 0.9), "walnut", r=0.35)
    m.plate([(0.6, 10.6), (1.6, 10.6), (2.8, 11.3), (2.2, 11.5), (0.6, 11.0)], Z - 0.3, Z + 0.3, "steel_dark", bevel=0.08)   # top lever
    for zz in (Z - 0.5, Z + 0.5):
        m.plate([(-0.6, 11.0), (0.2, 10.8), (0.8, 9.6), (0.3, 9.4), (-0.3, 10.4)], zz - 0.18, zz + 0.18, "steel_dark", bevel=0.06)   # hammers
    P.trigger_guard(m, 2.0, 5.2, 7.6, depth=1.4, r=0.2, tex="blued")
    P.stock_loft(m, [(0.3, 10.2), (-2.0, 9.8), (-4.5, 9.4)], [(0.3, 7.6), (-1.6, 6.6), (-3.4, 4.8), (-4.5, 4.4)], [(-4.5, 1.6), (0.3, 1.8)], "walnut")
    m.box((-5.0, 4.2, Z - 0.8), (-4.4, 6.2, Z + 0.8), "steel_dark", r=0.2)
    top = P.iron_post(m, 15.4, 10.5, h=0.25, hood=False)
    m.sight = (15.4, top)
    m.stock_rear = -1.0


# ===============================================================================================================
# Machine guns: MG 42, M60, DP-28, M134 Minigun
# ===============================================================================================================
def mg42(m):
    z0, z1 = zs(2.0)
    m.plate([(0.0, 7.6), (10.0, 7.6), (10.0, 10.6), (0.0, 10.6)], z0, z1, "steel_dark", bevel=0.25)
    m.plate([(1.0, 10.6), (9.6, 10.6), (9.4, 11.0), (1.2, 11.0)], Z - 0.95, Z + 0.95, "steel_dark", bevel=0.12)       # feed cover
    m.box((8.0, 9.4, Z - 1.6), (9.8, 10.4, Z + 1.6), "steel_dark", r=0.2)                                         # feed tray
    # The square, slotted barrel jacket, the muzzle booster
    m.box((10.0, 8.2, Z - 0.95), (24.0, 10.4, Z + 0.95), "steel_dark", r=0.25)
    for k in range(7):
        x = 10.8 + k * 1.8
        for zz, f in ((Z + 0.95, "south"), (Z - 0.95, "north")):
            m.plate([(x, 8.6), (x + 1.3, 8.6), (x + 1.3, 9.9), (x, 9.9)], zz - 0.05 if f == "south" else zz - 0.01, zz + 0.01 if f == "south" else zz + 0.05, "bore", bevel=0.0, icon=False, corner=0.4)
        m.box((x, 10.38, Z - 0.4), (x + 1.3, 10.42, Z + 0.4), "bore", faces=["up"], mesh_only=True)
    m.plate([(23.4, 8.4), (24.6, 8.4), (24.6, 10.2), (23.4, 10.2)], z0 - 0.15, z1 + 0.15, "steel_dark", bevel=0.15)    # barrel change latch
    m.lathe("x", [(24.0, 0), (24.0, 0.7), (25.8, 0.75), (26.4, 0.55), (26.4, 0.3), (25.4, 0.3), (25.4, 0)], 9.3, Z, "steel_dark", mats=["steel_dark"] * 4 + ["bore"] * 2)
    top = P.iron_post(m, 22.6, 10.4, h=1.0)
    m.box((3.0, 11.0, Z - 0.55), (4.4, 11.8, Z + 0.55), "steel_dark", r=0.1)
    m.sight = (22.6, top - 0.1)
    P.belt(m, 8.6, 9.4, n=7)
    P.trigger_guard(m, 3.4, 6.4, 7.6, depth=1.4, r=0.2, tex="steel_dark")
    P.grip(m, 3.0, 7.7, length=4.4, rake=22, w=1.4, depth=2.2, tex="bakelite", grooves=0, swell=0.15)
    P.stock_loft(m, [(0.2, 10.4), (-4.0, 10.0), (-12.5, 9.6)], [(0.2, 7.8), (-3.0, 7.4), (-12.5, 5.4)], [(-12.5, 1.8), (0.2, 1.6)], "bakelite")
    m.box((-12.9, 5.3, Z - 0.85), (-12.5, 9.7, Z + 0.85), "steel_dark", r=0.12)
    # Bipod folded back along the jacket
    m.box((21.0, 7.8, Z - 0.9), (22.4, 8.3, Z + 0.9), "steel_dark", r=0.12)
    for s2 in (-1, 1):
        m.pipe([(21.6, 7.9, Z + s2 * 0.7), (14.0, 7.6, Z + s2 * 0.8)], 0.17, "steel_dark")
    m.stock_rear = -12.9


def m60(m):
    z0, z1 = zs(2.0)
    m.plate([(0.0, 7.4), (12.0, 7.4), (12.0, 10.4), (0.0, 10.4)], z0, z1, "parkerized", bevel=0.25)
    m.plate([(1.0, 10.4), (10.0, 10.4), (9.8, 11.0), (1.2, 11.0)], Z - 0.95, Z + 0.95, "parkerized", bevel=0.12)
    m.box((9.0, 9.2, Z - 1.6), (11.6, 10.3, Z + 1.6), "parkerized", r=0.2)
    P.barrel(m, 12.0, 28.5, 9.2, 0.62, steps=[(13.0, 0.75), (14.0, 0.62)])
    m.box((12.0, 7.6, Z - 0.9), (19.0, 8.8, Z + 0.9), "polymer_black", r=0.4)                         # forearm
    m.box((19.0, 8.2, Z - 0.6), (21.0, 10.0, Z + 0.6), "parkerized", r=0.2)                          # gas cylinder block
    m.lathe("x", [(12.0, 0), (12.0, 0.42), (20.0, 0.42), (20.0, 0)], 8.25, Z, "parkerized")
    P.flash_hider(m, 28.5, 9.2, r=0.68, L=2.4, prongs=4)
    top = P.iron_post(m, 27.4, 9.8, h=0.9)
    m.box((24.0, 9.8, Z - 0.3), (25.4, 10.6, Z + 0.3), "parkerized", r=0.1)                          # barrel carry handle
    m.pipe([(23.6, 10.5, Z), (24.0, 11.6, Z), (25.6, 11.6, Z), (26.0, 10.5, Z)], 0.16, "parkerized")
    m.box((2.6, 11.0, Z - 0.55), (4.0, 11.8, Z + 0.55), "parkerized", r=0.1)
    m.sight = (27.4, top - 0.1)
    P.belt(m, 10.0, 9.2, n=7)
    P.trigger_guard(m, 3.4, 6.4, 7.4, depth=1.4, r=0.2, tex="parkerized")
    P.grip(m, 3.0, 7.5, length=4.4, rake=20, w=1.45, depth=2.2, tex="polymer_black", stipple="grip_check")
    P.stock_loft(m, [(0.2, 10.2), (-12.5, 10.0)], [(0.2, 7.6), (-4.0, 7.0), (-12.5, 5.2)], [(-12.5, 1.9), (0.2, 1.6)], "polymer_black")
    m.box((-12.9, 5.1, Z - 0.9), (-12.5, 10.1, Z + 0.9), "rubber", r=0.15)
    for s2 in (-1, 1):
        m.pipe([(20.0, 8.2, Z + s2 * 0.6), (14.0, 7.2, Z + s2 * 0.7)], 0.17, "parkerized")
    m.stock_rear = -12.9


def dp28(m):
    z0, z1 = zs(1.7)
    m.plate([(0.0, 7.6), (9.0, 7.6), (9.0, 9.8), (0.0, 9.8)], z0, z1, "blued", bevel=0.2)
    # The pan magazine on top, its centre and ribs
    m.lathe("y", [(9.8, 0), (9.8, 4.4), (10.3, 4.6), (10.8, 4.4), (11.2, 1.0), (11.4, 0.7), (11.4, 0)], 4.8, Z, "steel_dark")
    for k in range(12):
        ang = k * 30
        m.box((4.8 + 1.4, 10.62, Z - 0.1), (4.8 + 4.2, 10.72, Z + 0.1), "steel", rot=("y", ang, (4.8, 10.6, Z)), mesh_only=True)
    m.lathe("y", [(11.3, 0), (11.3, 0.5), (11.8, 0.5), (11.8, 0)], 4.8, Z, "steel_light", icon=False)
    # Barrel shroud with its big holes, the flash cone, the bipod
    m.lathe("x", [(9.0, 0), (9.0, 1.0), (18.0, 1.0), (18.8, 0.6), (18.8, 0)], 8.8, Z, "blued")
    for k in range(6):
        for ang in (0, 60, 120, 180, 240, 300):
            x = 10.0 + k * 1.3
            m.box((x, 8.8 + 0.98, Z - 0.3), (x + 0.6, 8.8 + 1.02, Z + 0.3), "bore", faces=["up"], rot=("x", ang, (x, 8.8, Z)), mesh_only=True)
    P.barrel(m, 18.8, 26.5, 8.8, 0.5)
    m.lathe("x", [(26.5, 0.3), (26.5, 0.5), (28.0, 0.95), (28.0, 0.8), (26.7, 0.3)], 8.8, Z, "blued")
    top = P.iron_post(m, 26.0, 9.3, h=1.1)
    m.sight = (26.0, top - 0.05)
    for s2 in (-1, 1):
        m.pipe([(18.0, 8.0, Z + s2 * 0.5), (16.0, 3.0, Z + s2 * 2.4)], 0.17, "blued")
        m.box((15.6, 2.6, Z + s2 * 2.4 - 0.5), (16.4, 3.0, Z + s2 * 2.4 + 0.5), "blued", r=0.1)
    P.trigger_guard(m, 2.6, 5.6, 7.6, depth=1.4, r=0.2, tex="blued")
    P.stock_loft(m, [(0.3, 9.6), (-4.0, 9.0), (-12.5, 9.2)], [(0.3, 7.6), (-1.0, 6.0), (-4.0, 6.0), (-12.5, 4.6)], [(-12.5, 1.9), (0.3, 1.6)], "birch")
    m.box((-12.9, 4.5, Z - 0.85), (-12.5, 9.3, Z + 0.85), "blued", r=0.12)
    m.stock_rear = -12.9


def minigun(m):
    # The M134: six barrels in a cluster, the housing and motor, a spade grip and the big ammo box with its chute
    cy, cz = 8.8, Z
    m.lathe("x", [(0.0, 0), (0.0, 1.9), (6.0, 1.9), (6.0, 0)], cy, cz, "steel_dark")
    m.lathe("x", [(-3.0, 0), (-3.0, 1.4), (0.0, 1.6), (0.0, 0)], cy, cz, "steel_dark")
    m.lathe("x", [(6.0, 0), (6.0, 1.7), (7.0, 1.7), (7.0, 0)], cy, cz, "steel")
    for k in range(6):
        a = math.radians(k * 60)
        by, bz = cy + math.cos(a) * 1.05, cz + math.sin(a) * 1.05
        P.barrel(m, 7.0, 29.0, by, 0.38, bore=0.2) if False else m.lathe("x", [(7.0, 0), (7.0, 0.38), (29.0, 0.38), (29.0, 0.22), (28.4, 0.2), (28.4, 0)], by, bz, "steel_dark",
                                                                         mats=["steel_dark"] * 3 + ["bore"] * 2)
    for bx in (14.0, 22.0, 28.0):
        m.lathe("x", [(bx, 0.6), (bx, 1.6), (bx + 0.6, 1.6), (bx + 0.6, 0.6)], cy, cz, "steel")
    m.lathe("x", [(7.0, 0), (7.0, 0.55), (29.0, 0.55), (29.0, 0)], cy, cz, "steel_dark", icon=False)
    # Motor on the side, spade grips at the back with the trigger buttons
    m.lathe("z", [(cz + 1.6, 0), (cz + 1.6, 0.9), (cz + 3.4, 0.9), (cz + 3.4, 0)], 3.0, cy + 1.2, "steel_dark")
    m.box((-6.0, 5.0, cz - 1.4), (-3.0, 12.2, cz - 0.9), "steel_dark", r=0.2)
    m.box((-6.0, 5.0, cz + 0.9), (-3.0, 12.2, cz + 1.4), "steel_dark", r=0.2)
    for yy in (5.6, 11.6):
        m.lathe("z", [(cz - 1.4, 0), (cz - 1.4, 0.55), (cz + 1.4, 0.55), (cz + 1.4, 0)], -5.2, yy, "rubber")
    m.box((-5.6, 11.6, cz - 0.4), (-4.8, 12.4, cz + 0.4), "paint_red", r=0.12)
    # The ammo box hanging under the left side and the flexible feed chute
    m.box((-4.0, 1.0, cz - 3.6), (4.0, 5.6, cz - 1.0), "alu_od", r=0.3)
    m.pipe([(1.0, 5.6, cz - 2.3), (1.5, 6.6, cz - 1.8), (2.0, 7.2, cz - 1.0), (2.5, 7.6, cz - 0.2)], 0.5, "steel_dark")
    for k in range(8):
        m.box((-3.4 + k * 0.9, 5.55, cz - 3.4), (-3.1 + k * 0.9, 5.62, cz - 1.2), "steel_dark", faces=["up"], mesh_only=True)
    m.box((2.0, 10.6, cz - 0.6), (5.0, 11.6, cz + 0.6), "steel_dark", r=0.2)
    cyy = P.red_dot(m, 3.5, 11.6)
    m.sight = (4.7, cyy)
    m.stock_rear = -6.2


# ===============================================================================================================
# The first 22, rebuilt with the detailed parts
# ===============================================================================================================
def m4a1_v2(m):
    ar_lower(m, "alu", "polymer")
    ar_upper(m, "alu", fa=True)
    ar_stock(m, "ctr")
    P.barrel(m, 12.0, 28.0, 9.2, 0.52, steps=[(19.5, 0.52), (20.2, 0.46), (21.4, 0.52)])
    P.quad_rail(m, 12.0, 19.6, 9.15, 1.3, 1.25, "alu")
    m.box((11.8, 7.8, Z - 1.3), (12.4, 10.5, Z + 1.3), "alu", r=0.2)
    # The A2 front sight tower on the gas block, with its bayonet lug
    gx = 20.6
    m.box((gx - 0.6, 8.5, Z - 0.55), (gx + 0.6, 9.9, Z + 0.55), "steel_dark", r=0.15)
    m.plate([(gx - 0.45, 9.9), (gx + 0.45, 9.9), (gx + 0.25, 11.0), (gx - 0.25, 11.0)], Z - 0.35, Z + 0.35, "steel_dark", bevel=0.08)
    top = P.iron_post(m, gx, 11.0, h=0.9)
    m.box((gx - 0.9, 7.6, Z - 0.3), (gx + 0.1, 8.5, Z + 0.3), "steel_dark", r=0.08)
    P.iron_rear(m, 1.2, 10.55, h=1.3)
    P.birdcage(m, 28.0, 9.2, r=0.66, L=2.3)
    m.sight = (gx, top - 0.1)
    m.mag(7.6, 4.6, 2.85, 6.0, Z - 0.75, Z + 0.75, "alu", curve=1, base="polymer", ribs=4, rib_tex="steel_dark")
    P.sling_loop(m, 19.0, 7.6, Z, r=0.4, axis="x")


def sr25_v2(m):
    ar_lower(m, "alu", "polymer")
    ar_upper(m, "alu", fa=True)
    ar_stock(m, "fixed")
    P.barrel(m, 12.0, 29.2, 9.2, 0.66, steps=[(22.0, 0.66), (23.0, 0.58)])
    P.mlok(m, 12.0, 23.5, 9.15, 1.25, 1.2, "alu")
    m.box((11.8, 7.8, Z - 1.25), (12.4, 10.5, Z + 1.25), "alu", r=0.2)
    P.flash_hider(m, 29.2, 9.2, r=0.66, L=1.6, prongs=4)
    m.mag(7.6, 4.6, 3.0, 4.6, Z - 0.8, Z + 0.8, "steel_dark", curve=0, base="polymer", ribs=2)
    for s2 in (-1, 1):
        m.pipe([(21.5, 7.5, Z + s2 * 0.5), (28.0, 7.3, Z + s2 * 0.6)], 0.16, "steel_dark")
    m.box((21.0, 7.2, Z - 0.8), (22.2, 7.9, Z + 0.8), "steel_dark", r=0.12)
    cy = P.scope(m, 0.4, 12.4, 12.6, r=0.62, bell_front=1.5, bell_rear=1.3, base_y=10.75)
    m.sight = (12.4, cy)


def scar_heavy(m):
    scar_l(m)


def ak74_v2(m):
    ak_receiver(m, finish="steel", cover="steel")
    fx, top = ak_front(m, 15.2, 27.0, wood="polymer_plum", lower_len=6.2, brake="ak74")
    ak_lower(m, wood="polymer_plum", mag_tex="polymer_plum")
    # The AK-74M's side-folding polymer stock
    P.stock_loft(m, [(0.2, 9.2), (-4.0, 8.9), (-12.8, 8.7)], [(0.2, 7.3), (-4.0, 6.7), (-12.8, 4.8)], [(-12.8, 1.6), (0.2, 1.4)], "polymer_plum")
    m.plate([(-10.4, 7.6), (-5.0, 8.0), (-5.0, 7.2), (-10.4, 6.4)], Z - 0.8, Z + 0.8, "bore", bevel=0.0, icon=False)
    P.butt_pad(m, -12.8, 4.7, 8.8, 1.7, t=0.45)
    m.stock_rear = -13.3
    m.sight = (fx, top - 0.1)


def akm_v2(m):
    ak_receiver(m, finish="steel", cover="steel")
    fx, top = ak_front(m, 15.2, 27.0, wood="birch", lower_len=6.2, brake="slant")
    ak_lower(m, wood="birch", mag_tex="steel", mag_depth=6.6)
    ak_stock(m, "wood", "birch")
    m.sight = (fx, top - 0.1)


def saiga_v2(m):
    ak_receiver(m, finish="steel_dark", cover="steel_dark")
    P.barrel(m, 15.2, 25.5, 8.95, 0.62, bore=0.42)
    P.wood_handguard(m, 15.4, 21.4, 8.4, 1.0, 1.1, "polymer_black", grooves=0)
    xs = [15.4 + 6.0 * k / 10 for k in range(11)]
    m.sweep([(x, 9.9) for x in xs], 0.64, 0.5, "polymer_black", rad=0.8)
    m.box((21.6, 8.4, Z - 0.6), (22.8, 10.4, Z + 0.6), "steel_dark", r=0.2)
    top = P.iron_post(m, 24.6, 9.6, h=0.8)
    m.box((24.0, 8.4, Z - 0.6), (25.2, 9.6, Z + 0.6), "steel_dark", r=0.15)
    z0, z1 = zs(1.75)
    m.plate([(3.6, 6.7), (4.0, 5.2), (4.6, 4.95), (7.6, 4.95), (8.1, 5.3), (8.2, 6.7), (7.8, 6.7), (7.6, 5.4), (4.7, 5.35), (4.3, 6.7)],
            Z - 0.28, Z + 0.28, "steel_dark", bevel=0.06, corner=0.15)
    P.trigger(m, 6.0, 6.65, "steel")
    P.grip(m, 3.6, 6.75, length=4.6, rake=22, w=1.4, depth=2.1, tex="polymer_black", stipple="grip_check")
    m.mag(8.6, 6.8, 3.6, 4.6, Z - 1.0, Z + 1.0, "polymer_black", curve=1, base="polymer_black", ribs=3)
    ak_stock(m, "folding")
    m.sight = (24.6, top - 0.05)


def svd_v2(m):
    ak_receiver(m, finish="steel_dark", cover="steel_dark", length=12.0)
    # The long, thin barrel, its wooden handguard, the slotted flash hider
    P.barrel(m, 14.2, 29.5, 9.0, 0.48, steps=[(15.0, 0.55), (16.0, 0.48)])
    P.wood_handguard(m, 14.4, 20.2, 8.9, 1.0, 1.0, "birch", grooves=0)
    for k in range(5):
        m.box((14.9 + k * 1.0, 8.6, Z + 0.98), (15.4 + k * 1.0, 9.6, Z + 1.0), "bore", faces=["south"], mesh_only=True)
    P.flash_hider(m, 29.5, 9.0, r=0.55, L=1.6, prongs=5)
    top = P.iron_post(m, 28.4, 9.5, h=0.8)
    z0, z1 = zs(1.7)
    P.trigger_guard(m, 3.6, 6.8, 6.7, depth=1.5, r=0.2, tex="steel_dark")
    m.mag(7.6, 6.8, 2.6, 3.6, Z - 0.75, Z + 0.75, "steel_dark", curve=0, base="steel_dark", ribs=3)
    # The skeleton thumbhole stock with its cheek rest
    m.plate([(0.0, 9.0), (0.0, 6.6), (-1.2, 5.4), (-3.2, 5.8), (-4.0, 6.6), (-13.0, 4.6), (-13.0, 9.2), (-6.0, 9.4)], Z - 0.8, Z + 0.8, "birch", bevel=0.35)
    m.plate([(-11.6, 8.4), (-5.4, 8.6), (-4.2, 7.2), (-11.0, 5.6)], Z - 0.9, Z + 0.9, "bore", bevel=0.0, icon=False)
    m.box((-10.5, 9.2, Z - 0.75), (-5.5, 10.1, Z + 0.75), "birch", r=0.35)
    m.box((-13.4, 4.5, Z - 0.85), (-13.0, 9.3, Z + 0.85), "rubber", r=0.15)
    m.stock_rear = -13.4
    # The PSO scope on its side rail mount, offset left
    m.box((4.0, 9.4, Z - 1.6), (9.0, 10.4, Z - 0.9), "steel_dark", r=0.12)
    cy = P.scope(m, 2.0, 11.2, 11.6, r=0.55, bell_front=1.45, bell_rear=1.35, base_y=10.0)
    m.sight = (11.2, cy)


def pkm_v2(m):
    z0, z1 = zs(1.8)
    m.plate([(0.0, 7.6), (11.0, 7.6), (11.0, 10.2), (0.0, 10.2)], z0, z1, "steel_dark", bevel=0.2)
    m.plate([(1.0, 10.2), (9.5, 10.2), (9.3, 10.8), (1.2, 10.8)], Z - 0.95, Z + 0.95, "steel_dark", bevel=0.12)
    for (rx, ry) in ((1.4, 8.0), (3.0, 8.0), (8.8, 8.0), (10.2, 9.6)):
        for zz, sg in ((z1, 1), (z0, -1)):
            P.rivet(m, rx, ry, zz, sg, r=0.14)
    P.barrel(m, 11.0, 28.5, 9.1, 0.58, steps=[(12.0, 0.7), (13.0, 0.58)])
    for k in range(10):
        m.lathe("x", [(12.6 + k * 0.5, 0.58), (12.6 + k * 0.5, 0.78), (12.8 + k * 0.5, 0.78), (12.8 + k * 0.5, 0.58)], 9.1, Z, "steel_dark", icon=False)
    P.flash_hider(m, 28.5, 9.1, r=0.6, L=2.2, prongs=5)
    m.lathe("x", [(11.0, 0), (11.0, 0.4), (21.0, 0.4), (21.0, 0)], 8.1, Z, "steel_dark")
    m.box((20.4, 7.6, Z - 0.6), (21.6, 9.8, Z + 0.6), "steel_dark", r=0.2)
    top = P.iron_post(m, 27.4, 9.65, h=1.0)
    m.pipe([(9.5, 10.4, Z), (10.0, 11.6, Z), (12.0, 11.6, Z), (12.5, 10.4, Z)], 0.16, "steel_dark")
    m.sight = (27.4, top - 0.1)
    # The box of belt hanging under the receiver
    m.box((5.4, 4.2, Z - 1.3), (9.6, 7.6, Z + 1.3), "alu_od", r=0.25)
    m.box((5.6, 7.3, Z - 1.35), (9.4, 7.6, Z + 1.35), "steel_dark", r=0.06)
    P.belt(m, 9.2, 9.2, n=5)
    P.trigger_guard(m, 2.8, 5.2, 7.6, depth=1.3, r=0.2, tex="steel_dark")
    P.grip(m, 2.4, 7.7, length=4.3, rake=22, w=1.4, depth=2.1, tex="polymer_plum", grooves=3)
    # The skeleton stock
    m.plate([(0.2, 9.8), (0.2, 7.0), (-1.8, 6.2), (-12.6, 5.0), (-12.6, 9.6), (-4.0, 10.0)], Z - 0.8, Z + 0.8, "polymer_plum", bevel=0.3)
    m.plate([(-11.4, 8.6), (-3.4, 9.0), (-2.4, 7.6), (-11.2, 6.2)], Z - 0.9, Z + 0.9, "bore", bevel=0.0, icon=False)
    m.box((-13.0, 4.9, Z - 0.85), (-12.6, 9.7, Z + 0.85), "steel_dark", r=0.12)
    m.stock_rear = -13.0
    for s2 in (-1, 1):
        m.pipe([(20.6, 7.7, Z + s2 * 0.4), (14.0, 7.3, Z + s2 * 0.5)], 0.15, "steel_dark")


def m249_v2(m):
    z0, z1 = zs(2.0)
    m.plate([(0.0, 7.0), (11.5, 7.0), (11.5, 10.6), (0.0, 10.6)], z0, z1, "steel_dark", bevel=0.25)
    m.plate([(1.2, 10.6), (9.8, 10.6), (9.6, 11.2), (1.4, 11.2)], Z - 1.0, Z + 1.0, "steel_dark", bevel=0.12)
    P.rail(m, 1.5, 9.4, 11.15, w=1.5)
    P.barrel(m, 11.5, 28.2, 9.2, 0.6, steps=[(13.0, 0.74), (14.0, 0.6)])
    m.box((11.5, 7.6, Z - 1.1), (17.0, 10.0, Z + 1.1), "polymer_black", r=0.45)                      # handguard
    m.lathe("x", [(17.0, 0.6), (17.0, 0.95), (19.5, 0.95), (19.5, 0.6)], 9.2, Z, "steel_dark")       # heat shield
    m.lathe("x", [(11.5, 0), (11.5, 0.42), (20.4, 0.42), (20.4, 0)], 8.2, Z, "steel_dark")
    P.flash_hider(m, 28.2, 9.2, r=0.62, L=2.0, prongs=4)
    top = P.iron_post(m, 27.0, 9.8, h=0.9)
    m.pipe([(13.5, 10.0, Z), (14.0, 11.6, Z), (16.0, 11.6, Z), (16.5, 10.0, Z)], 0.16, "polymer_black")
    m.sight = (27.0, top - 0.1)
    m.box((5.6, 3.6, Z - 1.4), (10.2, 7.0, Z + 1.4), "polymer_od", r=0.4)                            # soft ammo pouch
    P.belt(m, 9.6, 9.6, n=5)
    P.trigger_guard(m, 3.2, 5.8, 7.0, depth=1.4, r=0.2, tex="steel_dark")
    P.grip(m, 2.8, 7.1, length=4.5, rake=22, w=1.45, depth=2.2, tex="polymer_black", stipple="grip_check")
    P.stock_loft(m, [(0.2, 10.2), (-12.0, 9.8)], [(0.2, 7.4), (-4.0, 6.8), (-12.0, 5.6)], [(-12.0, 1.8), (0.2, 1.6)], "polymer_black")
    m.box((-12.4, 5.5, Z - 0.9), (-12.0, 9.9, Z + 0.9), "rubber", r=0.15)
    m.stock_rear = -12.4
    for s2 in (-1, 1):
        m.pipe([(19.8, 8.0, Z + s2 * 0.5), (13.0, 7.4, Z + s2 * 0.6)], 0.16, "steel_dark")


def mosin_v2(m):
    P.stock_loft(m, [(19.0, 9.0), (6.0, 9.0), (0.0, 9.0), (-5.0, 8.8), (-14.0, 9.0)],
                 [(19.0, 8.0), (8.0, 7.5), (3.0, 7.2), (0.6, 6.6), (-1.5, 6.2), (-6.0, 6.0), (-14.0, 4.6)],
                 [(-14.0, 1.7), (-2.0, 1.5), (6.0, 1.7), (19.0, 1.4)], "birch")
    m.box((-14.4, 4.5, Z - 0.8), (-14.0, 9.1, Z + 0.8), "steel_dark", r=0.12)
    m.lathe("x", [(0.0, 0), (0.0, 0.6), (8.0, 0.6), (8.0, 0)], 9.5, Z, "blued")
    m.lathe("x", [(-1.2, 0), (-1.2, 0.5), (0.0, 0.5), (0.0, 0)], 9.5, Z, "blued")
    m.pipe([(1.0, 9.5, Z + 0.55), (1.0, 9.5, Z + 1.6)], 0.14, "blued")                               # the straight bolt
    P.knob(m, 1.0, 9.5, Z + 1.75, r=0.36, tex="blued")
    P.barrel(m, 8.0, 31.2, 9.5, 0.5, steps=[(9.0, 0.58), (10.0, 0.5)])
    m.lathe("x", [(12.0, 0), (12.0, 0.55), (19.0, 0.55), (19.0, 0)], 10.0, Z, "birch")
    for bx in (15.0, 21.0):
        m.lathe("x", [(bx, 0.6), (bx, 1.25), (bx + 0.5, 1.25), (bx + 0.5, 0.6)], 8.9, Z, "blued")
    m.cyl("x", 19.0, 30.5, 8.35, Z, 0.15, "steel_light")
    m.lathe("x", [(30.4, 0.5), (30.4, 0.7), (31.2, 0.7), (31.2, 0.5)], 9.5, Z, "blued", icon=False)
    top = P.iron_post(m, 30.7, 10.0, h=0.6)
    m.box((9.0, 10.0, Z - 0.5), (11.2, 10.5, Z + 0.5), "blued", r=0.1)
    m.box((10.2, 10.4, Z - 0.35), (10.8, 10.75, Z + 0.35), "blued", r=0.05)
    P.trigger_guard(m, 1.8, 5.0, 7.1, depth=1.3, r=0.18, tex="blued")
    m.box((4.0, 6.4, Z - 0.6), (7.6, 7.2, Z + 0.6), "blued", r=0.15)                                # the magazine box
    m.sight = (30.7, top - 0.05)
    m.stock_rear = -14.4


def m700_v2(m):
    # A sporter stock in dark polymer, the round action with a turned-down bolt, a heavy barrel, a big scope
    body = [(-14.0, 4.8), (-9.0, 6.0), (-3.0, 6.4), (-1.0, 5.0), (1.2, 5.0), (2.0, 6.9), (17.0, 7.6), (18.0, 8.4), (18.0, 9.2), (2.0, 9.2), (-2.0, 9.0), (-8.0, 9.6), (-14.0, 9.8)]
    m.plate(body, *zs(1.9), "polymer_black", bevel=0.5)
    P.butt_pad(m, -14.0, 4.7, 9.9, 2.0, t=0.7)
    P.sling_loop(m, -9.0, 6.0, Z, r=0.4, axis="x")
    P.sling_loop(m, 15.0, 7.6, Z, r=0.4, axis="x")
    m.lathe("x", [(0.0, 0), (0.0, 0.9), (0.4, 1.0), (10.6, 1.0), (11.0, 0.9), (11.0, 0)], 10.0, Z, "blued")
    m.plate([(3.2, 9.4), (7.0, 9.4), (7.0, 10.6), (3.2, 10.6)], Z + 0.95, Z + 1.02, "bore", bevel=0.0, icon=False)
    m.lathe("x", [(-1.4, 0), (-1.4, 0.5), (0.0, 0.55), (0.0, 0)], 10.0, Z, "blued")
    m.pipe([(0.9, 10.0, Z + 0.6), (0.7, 9.3, Z + 1.2), (0.5, 8.7, Z + 1.55)], 0.16, "blued")
    P.knob(m, 0.45, 8.5, Z + 1.65, r=0.42, tex="blued")
    P.barrel(m, 11.0, 29.0, 10.0, 0.7, steps=[(12.5, 0.7), (13.5, 0.58)])
    P.trigger_guard(m, 1.6, 4.4, 6.9, depth=1.4, r=0.2, tex="blued")
    cy = P.scope(m, 0.5, 12.0, 12.6, r=0.62, bell_front=1.5, bell_rear=1.3, base_y=11.0)
    m.sight = (12.0, cy)
    m.stock_rear = -14.7


def ax338_v2(m):
    # The AX: a skeletal aluminium chassis in tan, a folding stock with a cheek riser, a long free-floating tube
    z0, z1 = zs(2.0)
    m.plate([(-2.0, 7.0), (18.0, 7.0), (18.6, 7.6), (18.6, 9.0), (-2.0, 9.0)], z0, z1, "alu_tan", bevel=0.3)
    for k in range(5):
        m.box((7.5 + k * 2.0, 7.4, z1 - 0.04), (8.7 + k * 2.0, 8.6, z1 + 0.01), "bore", faces=["south"], mesh_only=True)
        m.box((7.5 + k * 2.0, 7.4, z0 - 0.01), (8.7 + k * 2.0, 8.6, z0 + 0.04), "bore", faces=["north"], mesh_only=True)
    P.rail(m, -1.6, 18.4, 10.95, w=1.6)
    m.box((-1.8, 9.0, Z - 0.9), (18.4, 10.95, Z + 0.9), "alu_tan", r=0.2)
    m.lathe("x", [(0.0, 0), (0.0, 0.9), (11.0, 0.9), (11.0, 0)], 10.0, Z, "steel_dark")
    m.pipe([(0.9, 10.0, Z + 0.6), (0.7, 9.3, Z + 1.3), (0.5, 8.6, Z + 1.7)], 0.17, "steel_light")
    P.knob(m, 0.45, 8.4, Z + 1.8, r=0.48, tex="polymer_black")
    P.barrel(m, 18.6, 28.0, 10.0, 0.72)
    P.brake(m, 28.0, 10.0, r=0.9, L=2.6, ports=3)
    P.grip(m, 1.6, 7.2, length=4.6, rake=18, w=1.5, depth=2.3, tex="polymer_black", stipple="grip_check")
    P.trigger_guard(m, 2.0, 5.2, 7.0, depth=1.5, r=0.22, tex="alu_tan")
    m.mag(5.8, 7.2, 3.2, 3.2, Z - 0.85, Z + 0.85, "steel_dark", curve=0, base="steel_dark")
    m.plate([(-13.4, 10.6), (-6.0, 10.4), (-2.0, 9.6), (-2.0, 7.6), (-6.0, 7.0), (-10.4, 5.6), (-13.4, 5.0)], Z - 0.9, Z + 0.9, "alu_tan", bevel=0.35)
    m.plate([(-11.6, 9.4), (-6.4, 9.2), (-4.0, 8.4), (-6.4, 7.8), (-10.6, 6.6)], Z - 0.95, Z + 0.95, "bore", bevel=0.0, icon=False)
    P.butt_pad(m, -13.4, 4.9, 10.7, 1.9, t=0.8)
    m.box((-12.6, 3.8, Z - 0.3), (-11.6, 5.1, Z + 0.3), "steel_dark", r=0.12)
    for s2 in (-1, 1):
        m.pipe([(16.0, 7.0, Z + s2 * 0.6), (24.0, 6.9, Z + s2 * 0.7)], 0.18, "steel_dark")
    cy = P.scope(m, 0.4, 13.0, 13.0, r=0.66, bell_front=1.6, bell_rear=1.3, base_y=11.3, sunshade=1.0)
    m.sight = (13.0, cy)
    m.stock_rear = -14.2


def mp5_v2(m):
    z0, z1 = zs(1.7)
    # The round-topped stamped receiver with its cocking tube, the slim handguard, the curved mag
    m.plate([(0.0, 7.4), (12.0, 7.4), (12.0, 9.6), (0.0, 9.6)], z0, z1, "steel_dark", bevel=0.2)
    xs = [0.2 + 15.6 * k / 16 for k in range(17)]
    m.sweep([(x, 9.7) for x in xs], 0.82, 0.55, "steel_dark", rad=0.9)
    m.lathe("x", [(12.0, 0), (12.0, 0.45), (17.6, 0.45), (17.6, 0)], 10.2, Z, "steel_dark")
    m.box((13.0, 10.4, z1), (13.6, 10.8, z1 + 0.6), "steel_dark", r=0.1)                              # cocking handle, forward
    P.wood_handguard(m, 12.2, 17.4, 8.6, 1.0, 1.0, "polymer_black", grooves=0)
    for k in range(4):
        m.box((12.8 + k * 1.1, 8.0, Z + 0.98), (13.3 + k * 1.1, 9.1, Z + 1.01), "bore", faces=["south"], mesh_only=True)
    P.barrel(m, 17.4, 20.8, 9.1, 0.42)
    m.lathe("x", [(20.8, 0.42), (20.8, 0.58), (21.4, 0.58), (21.4, 0.42)], 9.1, Z, "steel_dark", icon=False)  # the three-lug mount
    m.box((17.0, 9.6, Z - 0.65), (18.0, 10.8, Z + 0.65), "steel_dark", r=0.15)
    m.lathe("x", [(17.2, 0.25), (17.2, 0.55), (17.8, 0.55), (17.8, 0.25)], 11.2, Z, "steel_dark", icon=False)  # hooded front post
    top = P.iron_post(m, 17.5, 10.8, h=0.6, hood=False)
    m.lathe("y", [(10.1, 0.0), (10.1, 0.45), (10.9, 0.45), (10.9, 0)], 1.2, Z, "steel_dark", icon=False)   # rotary diopter drum
    m.sight = (17.5, top)
    m.plate([(4.0, 8.6), (7.0, 8.6), (7.0, 9.4), (4.0, 9.4)], z1 - 0.04, z1 + 0.01, "bore", bevel=0.0, icon=False)
    m.mag(7.8, 7.6, 2.4, 6.2, Z - 0.65, Z + 0.65, "steel_dark", curve=2, base="steel_dark", ribs=4, rib_tex="steel")
    # Trigger group: polymer lower, grip, the selector
    m.plate([(0.6, 7.4), (6.8, 7.4), (6.6, 6.4), (5.0, 6.0), (1.2, 6.2)], Z - 0.8, Z + 0.8, "polymer_black", bevel=0.25)
    P.trigger_guard(m, 3.0, 6.2, 6.3, depth=1.3, r=0.22, tex="polymer_black")
    P.grip(m, 2.6, 6.4, length=4.4, rake=22, w=1.45, depth=2.1, tex="polymer_black", grooves=3)
    m.plate([(0.8, 7.0), (2.0, 7.4), (2.0, 6.8)], z0 - 0.2, z0, "steel_dark", bevel=0.05)
    # Retractable stock (the A3's two rails and its butt plate)
    for zz in (Z - 0.6, Z + 0.6):
        m.pipe([(0.0, 8.6, zz), (-8.0, 8.6, zz)], 0.18, "steel_dark")
    m.plate([(-8.6, 10.0), (-7.8, 10.0), (-7.8, 6.2), (-8.6, 6.2)], Z - 0.9, Z + 0.9, "steel_dark", bevel=0.15)
    m.box((-9.0, 6.1, Z - 0.95), (-8.6, 10.1, Z + 0.95), "rubber", r=0.15)
    m.stock_rear = -9.0


def mp7_v2(m):
    z0, z1 = zs(1.7)
    body = [(0.0, 10.2), (11.0, 10.2), (11.6, 9.6), (11.6, 7.4), (9.4, 6.8), (8.8, 6.0), (6.4, 6.0), (5.8, 6.8), (2.0, 7.0), (0.0, 8.0)]
    m.plate(body, z0, z1, "polymer_black", bevel=0.35)
    P.rail(m, 0.4, 10.6, 10.15, w=1.5)
    P.rail(m, 8.0, 11.2, 8.6, w=1.0, axis="south", z=z1 - 0.02)
    P.barrel(m, 11.6, 15.0, 8.7, 0.42)
    P.flash_hider(m, 15.0, 8.7, r=0.48, L=1.2, prongs=3)
    # The mag goes up the grip; the folding foregrip, down
    P.grip(m, 3.6, 7.0, length=5.0, rake=10, w=1.5, depth=2.4, tex="polymer_black", grooves=0, swell=0.05, base="polymer_black", stipple="grip_check")
    P.trigger_guard(m, 4.4, 7.0, 6.8, depth=1.4, r=0.2, tex="polymer_black")
    m.plate([(9.0, 6.2), (10.2, 6.2), (10.0, 3.4), (9.2, 3.4)], Z - 0.5, Z + 0.5, "polymer_black", bevel=0.2)
    # Retracted stock: two thin rods and the butt plate close behind
    for zz in (Z - 0.55, Z + 0.55):
        m.pipe([(0.0, 8.8, zz), (-4.0, 8.8, zz)], 0.16, "steel_dark")
    m.plate([(-4.6, 10.0), (-4.0, 10.0), (-4.0, 7.2), (-4.6, 7.2)], Z - 0.9, Z + 0.9, "polymer_black", bevel=0.15)
    front = P.iron_post(m, 10.2, 10.6, h=1.0)
    P.iron_rear(m, 1.0, 10.6, h=1.0)
    m.sight = (10.2, front - 0.1)
    m.stock_rear = -4.6


def uz9_v2(m):
    z0, z1 = zs(1.9)
    m.plate([(0.0, 7.6), (10.8, 7.6), (10.8, 10.2), (0.0, 10.2)], z0, z1, "steel_dark", bevel=0.25)
    for k in range(6):
        m.box((1.0 + k * 0.8, 10.15, Z - 0.5), (1.4 + k * 0.8, 10.25, Z + 0.5), "bore", faces=["up"], mesh_only=True)   # the top cover's ribs
    m.box((3.2, 10.2, Z - 0.3), (4.0, 10.7, Z + 0.3), "steel_light", r=0.1)
    P.barrel(m, 10.8, 13.6, 8.9, 0.46)
    m.lathe("x", [(10.8, 0), (10.8, 0.7), (11.6, 0.7), (11.6, 0)], 8.9, Z, "steel_dark")
    m.plate([(6.0, 7.6), (10.6, 7.6), (10.6, 6.6), (6.4, 6.6)], Z - 0.9, Z + 0.9, "polymer_black", bevel=0.3)                        # forend
    P.grip(m, 4.4, 7.4, length=5.6, rake=4, w=1.6, depth=2.4, tex="polymer_black", grooves=0, swell=0.0, base="steel_dark", stipple="grip_check")
    P.trigger_guard(m, 5.4, 7.6, 7.6, depth=1.3, r=0.2, tex="steel_dark")
    m.box((2.4, 5.0, Z - 0.75), (3.6, 7.4, Z + 0.75), "steel_dark", r=0.2)                              # grip safety
    # The folding metal stock, folded under
    for zz in (Z - 0.7, Z + 0.7):
        m.pipe([(0.0, 7.8, zz), (6.0, 6.8, zz)], 0.14, "steel")
    m.box((5.8, 6.4, Z - 0.9), (6.4, 7.0, Z + 0.9), "steel", r=0.1)
    top = P.iron_post(m, 9.8, 10.2, h=0.6)
    P.iron_rear(m, 0.8, 10.2, h=0.6)
    m.sight = (9.8, top - 0.05)
    m.stock_rear = -0.3


def kv45_v2(m):
    vector(m)


def aug_v2(m):
    z0, z1 = zs(2.2)
    # The green polymer shell: the bullpup stock, the trigger guard all the way round, the scope housing on top
    body = [(-9.4, 5.8), (-2.0, 5.8), (-1.0, 6.6), (6.0, 6.6), (6.4, 4.4), (11.0, 4.4), (12.4, 6.6), (14.0, 7.0), (14.6, 8.6), (14.6, 10.2),
            (-9.0, 10.4), (-9.4, 9.8)]
    m.plate(body, z0, z1, "polymer_od", bevel=0.55, corner=0.5)
    m.plate([(6.8, 5.0), (10.6, 5.0), (11.8, 6.6), (6.6, 6.6)], Z - 1.2, Z + 1.2, "bore", bevel=0.0, icon=False)
    m.mag(-4.4, 6.0, 2.8, 5.0, Z - 0.8, Z + 0.8, "lens", curve=1, base="polymer_black")
    m.box((-1.0, 8.6, z1 - 0.1), (0.4, 9.4, z1 + 0.5), "polymer_black", r=0.15)
    P.barrel(m, 14.6, 25.0, 8.9, 0.55)
    m.lathe("x", [(14.6, 0), (14.6, 0.85), (16.0, 0.85), (16.0, 0)], 8.9, Z, "steel_dark")
    m.box((14.2, 7.3, Z - 0.65), (17.4, 8.5, Z + 0.65), "polymer_od", r=0.3)                                      # its mount
    P.grip(m, 16.6, 7.5, length=3.4, rake=-10, w=1.4, depth=1.6, tex="polymer_od", grooves=0)                 # the folding front grip
    P.flash_hider(m, 25.0, 8.9, r=0.6, L=1.8, prongs=4)
    P.grip(m, 8.0, 6.8, length=4.2, rake=12, w=1.5, depth=2.0, tex="polymer_od", grooves=3)
    P.butt_pad(m, -9.4, 5.7, 10.5, 2.2, t=0.5)
    # The integrated 1.5x optic in its carry handle
    m.plate([(0.6, 10.2), (11.4, 10.2), (11.0, 11.0), (1.0, 11.0)], Z - 0.6, Z + 0.6, "polymer_od", bevel=0.2)
    m.lathe("x", [(-0.6, 0.55), (-0.6, 0.75), (0.6, 0.8), (11.0, 0.8), (11.6, 0.95), (12.4, 0.95), (12.4, 0.8)], 11.9, Z, "polymer_od")
    m.lathe("x", [(12.2, 0.8), (12.2, 0)], 11.9, Z, "lens", icon=False)
    m.lathe("x", [(-0.4, 0), (-0.4, 0.55)], 11.9, Z, "lens", icon=False)
    m.sight = (12.2, 11.9)
    m.stock_rear = -9.9


def p17_v2(m):
    pistol2(m, slide_len=11.6)


def m1911_v2(m):
    pistol2(m, slide_len=12.6, slide_h=2.0, w=1.3, slide_tex="blued", frame_tex="blued", grip_tex="walnut", grip_stipple="wood_check",
            hammer=True, rail=False, front_serr=False, beavertail=True, rake=16)
    m.lathe("x", [(13.6, 0.4), (13.6, 0.62), (13.95, 0.62), (13.95, 0.4)], 9.3, Z, "blued", icon=False)        # barrel bushing


def hawk50_v2(m):
    pistol2(m, slide_len=14.0, slide_h=2.7, w=1.7, slide_tex="chrome", frame_tex="chrome", grip_tex="rubber", grip_stipple=None,
            hammer=True, rail=False, front_serr=False, grip_len=5.4)
    # The big triangular-topped barrel over the slide's front, the gas port, the top rail
    m.plate([(10.5, 8.4), (15.4, 8.4), (15.4, 10.2), (15.0, 10.7), (10.5, 10.7)], Z - 0.95, Z + 0.95, "chrome", bevel=0.25)
    m.cyl("x", 15.38, 15.42, 9.4, Z, 0.42, "bore", mesh_only=True)
    P.rail(m, 3.2, 14.6, 10.7, w=1.2)


def r357_v2(m):
    revolver(m, barrel_len=5.0, finish="stainless", grip_tex="walnut", rib=True, shroud=True)


def double_barrel_v2(m):
    z0, z1 = zs(2.0)
    m.plate([(0.0, 7.6), (6.0, 7.6), (6.6, 8.3), (6.6, 10.3), (0.4, 10.6), (0.0, 10.0)], z0, z1, "blued", bevel=0.3)
    for zz in (Z - 0.52, Z + 0.52):
        m.lathe("x", [(6.6, 0), (6.6, 0.64), (30.2, 0.6), (30.6, 0.56), (30.6, 0.42), (30.0, 0.42), (30.0, 0)], 9.6, zz, "blued",
                mats=["blued", "blued", "blued", "blued", "bore", "bore"])
    m.box((6.6, 10.05, Z - 0.28), (30.4, 10.45, Z + 0.28), "blued", r=0.1)                          # the top rib
    m.lathe("y", [(10.45, 0), (10.45, 0.13), (10.7, 0.13), (10.75, 0)], 29.8, Z, "brass", icon=False)  # bead
    P.wood_handguard(m, 6.8, 14.0, 8.6, 0.8, 1.0, "walnut", grooves=0)
    m.plate([(0.6, 10.6), (1.6, 10.6), (2.8, 11.3), (2.2, 11.5), (0.6, 11.0)], Z - 0.3, Z + 0.3, "steel_dark", bevel=0.08)
    for zz in (Z - 0.5, Z + 0.5):
        m.plate([(-0.6, 11.0), (0.2, 10.8), (0.8, 9.6), (0.3, 9.4), (-0.3, 10.4)], zz - 0.18, zz + 0.18, "steel_dark", bevel=0.06)
    P.trigger_guard(m, 2.0, 5.2, 7.6, depth=1.4, r=0.2, tex="blued")
    P.stock_loft(m, [(0.3, 10.2), (-4.0, 9.8), (-14.0, 9.6)], [(0.3, 7.6), (-2.0, 6.6), (-5.0, 6.0), (-14.0, 4.6)], [(-14.0, 1.8), (0.3, 1.8)], "walnut")
    P.butt_pad(m, -14.0, 4.5, 9.7, 1.9, t=0.6)
    for k in range(5):
        m.box((-3.0 + k * 0.35, 6.4, Z + 0.88), (-2.85 + k * 0.35, 8.0, Z + 0.92), "wood_check", faces=["south"], mesh_only=True)
    m.sight = (29.8, 10.72)
    m.stock_rear = -14.6

ALL = {
    "hk416": (hk416, 0.6, 0.10),
    "mcx": (mcx, 0.6, 0.10),
    "glock18": (glock18, 0.72, 0.30),
    "m9": (m9, 0.7, 0.30),
    "m870": (m870, 0.56, 0.10),
    "ksg": (ksg, 0.6, 0.10),
    "awm": (awm, 0.54, 0.10),
    "p90": (p90, 0.64, 0.10),
    "ak12": (ak12, 0.6, 0.10),
    "aks74u": (aks74u, 0.64, 0.10),
    "rpk": (rpk, 0.56, 0.10),
    "galil": (galil, 0.6, 0.10),
    "bizon": (bizon, 0.62, 0.10),
    "as_val": (as_val, 0.6, 0.10),
    "vss": (vss, 0.56, 0.10),
    "usp45": (usp45, 0.7, 0.30),
    "five7": (five7, 0.7, 0.30),
    "p226": (p226, 0.7, 0.30),
    "cz75": (cz75, 0.72, 0.30),
    "makarov": (makarov, 0.74, 0.30),
    "tt33": (tt33, 0.72, 0.30),
    "luger": (luger, 0.72, 0.30),
    "c96": (c96, 0.68, 0.30),
    "python": (python357, 0.7, 0.30),
    "m29": (m29, 0.66, 0.30),
    "judge": (judge, 0.72, 0.30),
    "vector": (vector, 0.64, 0.10),
    "ump45": (ump45, 0.62, 0.10),
    "thompson": (thompson, 0.6, 0.10),
    "ppsh": (ppsh, 0.6, 0.10),
    "mac10": (mac10, 0.66, 0.20),
    "mp40": (mp40, 0.62, 0.10),
    "scar_l": (scar_l, 0.6, 0.10),
    "g36": (g36, 0.6, 0.10),
    "famas": (bullpup_famas, 0.6, 0.10),
    "l85": (l85, 0.6, 0.10),
    "tavor": (tavor, 0.6, 0.10),
    "fal": (fal, 0.58, 0.10),
    "m1_garand": (m1_garand, 0.56, 0.10),
    "mk14": (mk14, 0.56, 0.10),
    "m82": (m82, 0.52, 0.10),
    "kar98k": (kar98k, 0.55, 0.10),
    "winchester": (winchester, 0.56, 0.10),
    "m1014": (m1014, 0.56, 0.10),
    "spas12": (spas12, 0.56, 0.10),
    "aa12": (aa12, 0.56, 0.10),
    "sawnoff": (sawnoff, 0.66, 0.20),
    "mg42": (mg42, 0.55, 0.10),
    "m60": (m60, 0.55, 0.10),
    "dp28": (dp28, 0.55, 0.10),
    "minigun": (minigun, 0.55, 0.30),
    # The first 22, rebuilt
    "m4a1": (m4a1_v2, 0.6, 0.10),
    "sr25": (sr25_v2, 0.56, 0.10),
    "mk17": (scar_heavy, 0.58, 0.10),
    "ak74": (ak74_v2, 0.6, 0.10),
    "akm": (akm_v2, 0.6, 0.10),
    "saiga12": (saiga_v2, 0.6, 0.10),
    "svd": (svd_v2, 0.56, 0.10),
    "pkm": (pkm_v2, 0.56, 0.10),
    "m249": (m249_v2, 0.56, 0.10),
    "mosin": (mosin_v2, 0.55, 0.10),
    "m700": (m700_v2, 0.56, 0.10),
    "ax338": (ax338_v2, 0.55, 0.10),
    "mp5": (mp5_v2, 0.62, 0.10),
    "mp7": (mp7_v2, 0.66, 0.10),
    "uz9": (uz9_v2, 0.68, 0.24),
    "kv45": (kv45_v2, 0.64, 0.10),
    "aug": (aug_v2, 0.6, 0.10),
    "p17": (p17_v2, 0.72, 0.30),
    "m1911": (m1911_v2, 0.72, 0.30),
    "hawk50": (hawk50_v2, 0.7, 0.30),
    "r357": (r357_v2, 0.72, 0.30),
    "double_barrel": (double_barrel_v2, 0.56, 0.10),
}
