"""High-detail gun parts for the mesh guns (designs2.py). Every function takes a mesh.MeshModel and builds one real
part out of smooth shapes: turned barrels and muzzle devices with real ports cut through them, rails with fine teeth,
swept grips with finger grooves and palm swells, lofted stocks, scopes with bells, turrets, rings and glass.

Conventions (as designs.py): model units, muzzle toward +x, z = 8 is the gun's middle, +z ("south") is its right side,
rifles' bore axis around y = 9.2 and the pistol grip's top near x 3..6, y 5.6.
"""
import math

from mesh import spline

Z = 8.0


def zs(w, z=Z):
    return z - w / 2, z + w / 2


def sample(fn_pts, xs):
    """Interpolate a polyline [(x, v), ...] at each x."""
    out = []
    for x in xs:
        v = fn_pts[-1][1]
        for i in range(len(fn_pts) - 1):
            a, b = fn_pts[i], fn_pts[i + 1]
            if a[0] <= x <= b[0]:
                k = (x - a[0]) / max(1e-6, b[0] - a[0])
                v = a[1] + (b[1] - a[1]) * k
                break
        else:
            if x < fn_pts[0][0]:
                v = fn_pts[0][1]
        out.append(v)
    return out


def smooth_line(pts, per=5):
    """A smooth curve through (x, v) points (Catmull-Rom), as a polyline."""
    return spline(pts, per)


# ---------------------------------------------------------------------------------------------------------------
# Barrels and muzzle devices
# ---------------------------------------------------------------------------------------------------------------
def barrel(m, x0, x1, y, r, tex="steel_dark", steps=None, bore=None, crown=True):
    """A turned barrel from x0 to x1 (radius r, or `steps` = [(x, r), ...] for a stepped / tapered profile), a
    recessed crown and a dark bore at the muzzle."""
    br = bore if bore is not None else max(0.14, r * 0.42)
    prof = [(x0, 0), (x0, r)]
    mats = [tex]
    if steps:
        for (x, rr) in steps:
            prof.append((x, rr))
            mats.append(tex)
        r = steps[-1][1]
    end = [(x1 - 0.05, r), (x1, r - 0.05), (x1, br + 0.06), (x1 - 0.06, br), (x1 - 1.2, br), (x1 - 1.2, 0)]
    if not crown:
        end = [(x1, r), (x1, br), (x1 - 1.2, br), (x1 - 1.2, 0)]
    for p in end:
        prof.append(p)
    mats += [tex] * (len(end) - 3) + ["bore", "bore", "bore"]
    mats = mats[:len(prof) - 1]
    while len(mats) < len(prof) - 1:
        mats.append("bore")
    m.lathe("x", prof, y, Z, tex, mats=mats)


def birdcage(m, x, y, r=0.7, L=2.3, tex="steel_dark"):
    """An A2 birdcage: a sleeve with slots cut around it (real gaps: prongs between them), closed at the bottom."""
    br = r * 0.5
    m.lathe("x", [(x, 0), (x, r * 0.9), (x + 0.5, r), (x + 0.7, r), (x + 0.7, br), (x + L, br), (x + L, 0)], y, Z, tex,
            mats=[tex, tex, tex, tex, "bore", "bore"])
    # The prongs (five; the bottom one is solid so it doesn't kick up dust)
    for k in range(6):
        ang = 90 + k * 60
        w = 0.32 if k != 3 else 0.9
        m.box((x + 0.7, y + r - 0.24, Z - w / 2), (x + L, y + r, Z + w / 2), tex, rot=("x", ang - 90, (x, y, Z)), r=0.05)
    m.lathe("x", [(x + L - 0.25, br), (x + L - 0.25, r), (x + L, r), (x + L, br)], y, Z, tex, icon=False)


def brake(m, x, y, r=0.75, L=2.8, ports=3, tex="steel_dark", flat=False):
    """A muzzle brake with ports cut right through the sides: rings between the ports, top and bottom bars joining them,
    the bore core showing through."""
    m.cyl("x", x, x + L, y, Z, r * 0.42, "bore", mesh_only=True)
    pw = (L - 0.5) / (ports * 2 + 1)
    xx = x
    for k in range(ports + 1):
        w = pw * (1.6 if k == 0 else 1)
        if flat:
            m.box((xx, y - r, Z - r), (xx + w, y + r, Z + r), tex, r=0.12)
        else:
            m.lathe("x", [(xx, r * 0.42), (xx, r - 0.06), (xx + 0.06, r), (xx + w - 0.06, r), (xx + w, r - 0.06), (xx + w, r * 0.42)], y, Z, tex)
        xx += w + pw
    xx -= pw
    if flat:
        m.box((x, y + r * 0.45, Z - r), (xx, y + r, Z + r), tex, r=0.1)
        m.box((x, y - r, Z - r), (xx, y - r * 0.45, Z + r), tex, r=0.1)
    else:
        m.box((x, y + r * 0.5, Z - r * 0.55), (xx, y + r * 0.97, Z + r * 0.55), tex, r=0.1)
        m.box((x, y - r * 0.97, Z - r * 0.55), (xx, y - r * 0.5, Z + r * 0.55), tex, r=0.1)


def suppressor(m, x, y, r=0.95, L=7.0, tex="steel_dark", knurl=True):
    """A can: rounded end caps, a knurled grip ring near the mount, the bore at the front."""
    b = min(0.3, r * 0.3)
    m.lathe("x", [(x, 0), (x, r - b), (x + b * 0.3, r - b * 0.3), (x + b, r), (x + L - b, r), (x + L - b * 0.3, r - b * 0.3),
                  (x + L, r - b), (x + L, 0.28), (x + L - 0.05, 0.2), (x + L - 1, 0.2), (x + L - 1, 0)], y, Z, tex,
            mats=[tex, tex, tex, tex, tex, tex, tex, "bore", "bore", "bore"])
    if knurl:
        m.cyl("x", x + 0.4, x + 1.4, y, Z, r + 0.04, "knurl", ends=True)
    for k in range(int(L / 1.6)):
        xx = x + 2.0 + k * 1.6
        if xx + 0.08 > x + L - 0.6:
            break
        m.cyl("x", xx, xx + 0.08, y, Z, r + 0.01, "steel", ends=False)


def flash_hider(m, x, y, r=0.6, L=1.8, prongs=4, tex="steel_dark"):
    """A pronged flash hider: a base ring and prongs with gaps (the bore visible between)."""
    m.lathe("x", [(x, 0), (x, r), (x + 0.6, r), (x + 0.6, r * 0.45), (x + L, r * 0.45), (x + L, 0)], y, Z, tex,
            mats=[tex, tex, tex, "bore", "bore"])
    for k in range(prongs):
        ang = 45 + k * 360 / prongs
        m.box((x + 0.55, y + r * 0.5, Z - 0.17), (x + L, y + r, Z + 0.17), tex, rot=("x", ang, (x, y, Z)), r=0.06)


# ---------------------------------------------------------------------------------------------------------------
# Rails, handguards
# ---------------------------------------------------------------------------------------------------------------
def rail(m, x0, x1, y, w=1.5, tex="rail", pitch=0.55, axis="up", z=Z, h=0.32):
    """A picatinny rail: base, dovetail and fine teeth. axis: up (on top), down, south / north (on a side)."""
    def put(frm, to, rr=0.06):
        # frm / to: (x, height out from the surface, across) in an "up" rail's terms, mapped to the real axis
        (xa, ya, za), (xb, yb, zb) = frm, to
        v0, v1, c0, c1 = ya - y, yb - y, za - z, zb - z
        if axis == "up":
            m.box((xa, y + v0, z + c0), (xb, y + v1, z + c1), tex, r=rr)
        elif axis == "down":
            m.box((xa, y - v1, z + c0), (xb, y - v0, z + c1), tex, r=rr)
        elif axis == "south":
            m.box((xa, y + c0, z + v0), (xb, y + c1, z + v1), tex, r=rr)
        else:
            m.box((xa, y + c0, z - v1), (xb, y + c1, z - v0), tex, r=rr)
    put((x0, y, z - w * 0.36), (x1, y + h * 0.6, z + w * 0.36), 0.05)
    x = x0 + 0.12
    while x + pitch * 0.55 <= x1 - 0.05:
        put((x, y + h * 0.5, z - w / 2), (x + pitch * 0.55, y + h * 1.35, z + w / 2), 0.07)
        x += pitch


def mlok(m, x0, x1, yc, hh, hw, tex, slots=True, top_rail=True, bottom=True):
    """A slim M-LOK handguard: a rounded tube (sweep), slots on the sides and bottom, a full top rail."""
    xs = [x0 + (x1 - x0) * k / 12 for k in range(13)]
    m.sweep([(x, yc) for x in xs], hw, hh, tex, rad=0.55)
    if slots:
        n = max(2, int((x1 - x0 - 1.5) / 1.6))
        for k in range(n):
            xx = x0 + 1.0 + k * 1.6
            for zz, f in ((Z + hw, "south"), (Z - hw, "north")):
                m.plate([(xx, yc - 0.28), (xx + 1.0, yc - 0.28), (xx + 1.0, yc + 0.28), (xx, yc + 0.28)],
                        zz - 0.012 if f == "south" else zz - 0.03, zz + 0.03 if f == "south" else zz + 0.012, "bore", bevel=0.0, icon=False)
            if bottom and k % 2 == 0:
                m.box((xx, yc - hh - 0.01, Z - 0.25), (xx + 1.0, yc - hh + 0.02, Z + 0.25), "bore", faces=["down"])
    if top_rail:
        rail(m, x0 + 0.2, x1 - 0.1, yc + hh - 0.08, w=1.5)


def quad_rail(m, x0, x1, yc, hh, hw, tex):
    """A quad rail: a squarer body with rails top, bottom and both sides."""
    m.box((x0, yc - hh, Z - hw), (x1, yc + hh, Z + hw), tex, r=0.35)
    rail(m, x0 + 0.2, x1 - 0.2, yc + hh - 0.05, w=1.4)
    rail(m, x0 + 0.4, x1 - 0.4, yc - hh + 0.05, w=1.4, axis="down")
    rail(m, x0 + 0.4, x1 - 0.4, yc, w=1.2, axis="south", z=Z + hw - 0.05)
    rail(m, x0 + 0.4, x1 - 0.4, yc, w=1.2, axis="north", z=Z - hw + 0.05)


def wood_handguard(m, x0, x1, yc, hh, hw, tex="walnut", grooves=3):
    """A wooden handguard: rounded, a little fatter in the middle, with finger grooves."""
    xs = [x0 + (x1 - x0) * k / 14 for k in range(15)]
    hu = [hw * (0.92 + 0.08 * math.sin(math.pi * k / 14)) for k in range(15)]
    hv = [hh * (0.94 + 0.06 * math.sin(math.pi * k / 14)) for k in range(15)]
    m.sweep([(x, yc) for x in xs], hu, hv, tex, rad=0.7)
    for k in range(grooves):
        xx = x0 + (x1 - x0) * (k + 1) / (grooves + 1)
        for zz, sgn in ((Z + hw * 0.97, 1), (Z - hw * 0.97, -1)):
            m.plate([(xx - 0.12, yc - hh * 0.55), (xx + 0.12, yc - hh * 0.55), (xx + 0.12, yc + hh * 0.2), (xx - 0.12, yc + hh * 0.2)],
                    zz - 0.03 if sgn > 0 else zz - 0.02, zz + 0.02 if sgn > 0 else zz + 0.03, "walnut_dark", bevel=0.0, icon=False)


# ---------------------------------------------------------------------------------------------------------------
# Grips, triggers, guards, stocks
# ---------------------------------------------------------------------------------------------------------------
def grip(m, x, y, length=4.8, rake=20, w=1.5, depth=2.2, tex="polymer", grooves=3, swell=0.18, base=None, stipple=None, z=Z):
    """A swept pistol grip hanging from its top front point (x, y), raked back `rake` degrees: finger grooves on the
    front, a palm swell at the back, rounded all over; `base` adds a plate at the bottom (a magazine's)."""
    a = math.radians(rake)
    dx, dy = -math.sin(a), -math.cos(a)        # down the grip
    fx, fy = math.cos(a), -math.sin(a)         # toward the front
    n = 18
    path, hv, hu = [], [], []
    for k in range(n + 1):
        t = k / n
        g = 0
        if grooves and 0.15 < t < 0.85:
            g = 0.12 * max(0, math.sin((t - 0.15) / 0.7 * math.pi * grooves))
        s = swell * math.sin(t * math.pi)
        half = depth / 2 * (1.0 - 0.06 * t)
        c = (x - fx * half + dx * length * t, y + dy * length * t - fy * half)
        # Grooves bite into the front edge; the swell bulges the back
        shift = (g + s) / 2
        path.append((c[0] - fx * shift, c[1] - fy * shift))
        hv.append(half + (s - g) / 2)
        hu.append(w / 2 * (0.95 + 0.08 * math.sin(t * math.pi)))
    m.sweep(path, hu, hv, tex, z=z, rad=0.7)
    if stipple:
        mid = path[n // 2]
        for zz, sgn in ((z + w / 2 * 0.98, 1), (z - w / 2 * 0.98, -1)):
            pts = []
            for k in range(4, n - 3):
                pts.append((path[k][0] - fx * hv[k] * 0.55, path[k][1] - fy * hv[k] * 0.55))
            for k in range(n - 4, 3, -1):
                pts.append((path[k][0] + fx * hv[k] * 0.5, path[k][1] + fy * hv[k] * 0.5))
            m.plate(pts, zz - 0.02 if sgn > 0 else zz - 0.05, zz + 0.05 if sgn > 0 else zz + 0.02, stipple, bevel=0.0, icon=False)
        del mid
    if base:
        end = path[-1]
        m.sweep([end, (end[0] + dx * 0.45, end[1] + dy * 0.45)], w / 2 + 0.12, hv[-1] + 0.15, base, z=z, rad=0.5)
    return path[-1]


def trigger(m, x, y, tex="steel", z=Z, size=1.0):
    """A curved trigger blade hanging from (x, y)."""
    pts = []
    for k in range(9):
        t = k / 8
        pts.append((x + 0.35 * size * math.sin(t * 1.4) - 0.1, y - 1.35 * size * t))
    back = [(px + 0.28 * size * (1 - 0.4 * (k / 8)), py) for k, (px, py) in enumerate(pts)][::-1]
    m.plate(pts + back, z - 0.16 * size, z + 0.16 * size, tex, bevel=0.07)


def trigger_guard(m, x0, x1, y_top, depth=1.7, r=0.2, tex="steel_dark", z=Z, front_rise=True):
    """A rounded trigger guard (a bar swept along a U under the receiver)."""
    pts2 = [(x1, y_top), (x1 + 0.1, y_top - depth * 0.6), (x1 - 0.5, y_top - depth), ((x0 + x1) / 2, y_top - depth - 0.05),
            (x0 + 0.4, y_top - depth + 0.05), (x0, y_top - depth * (0.55 if front_rise else 0.9))]
    path = [(p[0], p[1], z) for p in spline(pts2, 5)]
    m.pipe(path, r, tex, seg=10)
    trigger(m, x0 + (x1 - x0) * 0.55, y_top, "steel", z)


def stock_loft(m, top, bot, width, tex, z=Z, step=0.35):
    """A stock (or any long rounded body) from its top and bottom lines [(x, y), ...] and its width [(x, w), ...],
    all along x; smooth curves through the points."""
    top, bot, width = sorted(top), sorted(bot), sorted(width)
    tl = smooth_line(top, 5)
    bl = smooth_line(bot, 5)
    x0 = max(tl[0][0], bl[0][0])
    x1 = min(tl[-1][0], bl[-1][0])
    xs = []
    x = x0
    while x < x1 - 1e-6:
        xs.append(x)
        x += step
    xs.append(x1)
    tops, bots = sample(tl, xs), sample(bl, xs)
    ws = sample(smooth_line(width, 4) if len(width) > 2 else width, xs)
    pts = [(xs[i], bots[i], tops[i]) for i in range(len(xs))]
    taper = [(xs[i], ws[i]) for i in range(len(xs))]
    m.profile(pts, max(ws), tex, step=step, cz=z, taper=taper)


def butt_pad(m, x, y0, y1, w, tex="rubber", t=0.5, z=Z):
    m.box((x - t, y0, z - w / 2), (x, y1, z + w / 2), tex, r=min(0.3, w * 0.3))


def sling_loop(m, x, y, z, r=0.45, axis="z", tex="steel_dark"):
    """A sling swivel: a round loop."""
    pts = []
    for k in range(13):
        a = 2 * math.pi * k / 12
        if axis == "z":
            pts.append((x + math.cos(a) * r, y - r + math.sin(a) * r, z))
        else:
            pts.append((x + math.cos(a) * r, y, z + math.sin(a) * r))
    m.pipe(pts[:-1], 0.07, tex, seg=8, closed=True)


# ---------------------------------------------------------------------------------------------------------------
# Small hardware
# ---------------------------------------------------------------------------------------------------------------
def screw(m, x, y, z, side=1, r=0.2, tex="steel"):
    """A round screw head on a side face (side +1: the +z face at z, -1: the -z face)."""
    a0, a1 = (z, z + 0.08) if side > 0 else (z - 0.08, z)
    m.lathe("z", [(a0, 0), (a0, r), (a1 - 0.03, r), (a1, r * 0.75), (a1, 0)] if side > 0 else
            [(a0, 0), (a0, r * 0.75), (a0 + 0.03, r), (a1, r), (a1, 0)], x, y, tex, icon=False)
    zz = a1 + 0.002 if side > 0 else a0 - 0.012
    m.box((x - r * 0.8, y - 0.035, zz), (x + r * 0.8, y + 0.035, zz + 0.01), "bore", faces=["south" if side > 0 else "north"], mesh_only=True)


def pin(m, x, y, z0, z1, r=0.16, tex="steel_light"):
    """A cross pin showing on both sides."""
    m.cyl("z", z0 - 0.04, z1 + 0.04, x, y, r, tex, mesh_only=True)


def rivet(m, x, y, z, side=1, r=0.16, tex="steel"):
    a = z
    if side > 0:
        prof = [(a, 0), (a, r), (a + r * 0.3, r * 0.85), (a + r * 0.5, r * 0.4), (a + r * 0.55, 0)]
    else:
        prof = [(a - r * 0.55, 0), (a - r * 0.5, r * 0.4), (a - r * 0.3, r * 0.85), (a, r), (a, 0)]
    m.lathe("z", prof, x, y, tex, icon=False)


def knob(m, x, y, z, r=0.45, tex="steel_dark"):
    """A ball (a bolt knob)."""
    prof = [(z - r, 0)]
    for k in range(1, 8):
        a = math.pi * k / 8
        prof.append((z - r * math.cos(a), r * math.sin(a)))
    prof.append((z + r, 0))
    m.lathe("z", prof, x, y, tex)


def cartridge(m, x, y, z, length=1.6, r=0.2, case="brass", tip="copper", axis="x"):
    """A round: a brass case with a rim and shoulder, the bullet's ogive."""
    L = length
    body = L * 0.62
    prof = [(x, 0), (x, r * 1.05), (x + 0.06, r * 1.05), (x + 0.08, r * 0.85), (x + 0.12, r), (x + body, r), (x + body + 0.12, r * 0.72),
            (x + body + 0.22, r * 0.72)]
    mats = [case] * (len(prof) - 1)
    ogive = [(x + body + 0.22 + (L - body - 0.22) * k / 5, r * 0.72 * math.cos(k / 5 * math.pi / 2) ** 0.7) for k in range(1, 6)]
    prof += ogive
    mats += [tip] * len(ogive)
    if axis == "x":
        m.lathe("x", prof, y, z, case, mats=mats, seg=12, icon=False)
    else:
        prof2 = [(p[0] - x + y, p[1]) for p in prof]
        m.lathe("y", prof2, x, z, case, mats=mats, seg=12, icon=False)


# ---------------------------------------------------------------------------------------------------------------
# Sights and optics
# ---------------------------------------------------------------------------------------------------------------
def scope(m, x0, x1, y, r=0.62, bell_front=1.45, bell_rear=1.25, tex="scope_body", rings=None, base_y=None, turrets=True, sunshade=0.0):
    """A riflescope: eyepiece with its rubber, tube, saddle and turrets, objective bell, glass both ends, rings and
    bases down to base_y."""
    L = x1 - x0
    rf, rr = r * bell_front, r * bell_rear
    prof = [(x0, rr * 0.82), (x0, rr), (x0 + L * 0.12, rr), (x0 + L * 0.2, r * 1.08), (x0 + L * 0.24, r), (x1 - L * 0.32, r),
            (x1 - L * 0.2, rf * 0.96), (x1 - L * 0.04, rf), (x1, rf), (x1, rf * 0.84)]
    mats = [tex] * (len(prof) - 1)
    m.lathe("x", prof, y, Z, tex, mats=mats)
    m.cyl("x", x0 + L * 0.02, x0 + L * 0.08, y, Z, rr + 0.04, "rubber", mesh_only=True)
    m.lathe("x", [(x0 + 0.25, 0), (x0 + 0.25, rr * 0.82)], y, Z, "lens", icon=False)          # eyepiece glass, set back
    m.lathe("x", [(x1 - 0.3, rf * 0.84), (x1 - 0.3, 0)], y, Z, "lens", icon=False)          # objective glass
    if sunshade:
        m.lathe("x", [(x1, rf * 0.84), (x1, rf), (x1 + sunshade, rf), (x1 + sunshade, rf * 0.92)], y, Z, tex, icon=False)
    if turrets:
        mid = x0 + L * 0.47
        m.box((mid - 1.0, y - r * 0.9, Z - r * 0.95), (mid + 1.0, y + r * 0.9, Z + r * 0.95), tex, r=0.3)             # saddle
        m.cyl("y", y + r * 0.6, y + r + 0.75, mid, Z, 0.62, tex)
        m.cyl("y", y + r + 0.35, y + r + 0.8, mid, Z, 0.66, "knurl")
        m.cyl("z", Z + r * 0.6, Z + r + 0.75, mid, y, 0.62, tex)
        m.cyl("z", Z + r + 0.35, Z + r + 0.8, mid, y, 0.66, "knurl")
        m.cyl("z", Z - r - 0.55, Z - r * 0.6, mid - 0.2, y, 0.48, tex)                     # parallax knob
    for mx in (rings or (x0 + L * 0.3, x1 - L * 0.38)):
        m.lathe("x", [(mx - 0.4, r), (mx - 0.4, r + 0.2), (mx + 0.4, r + 0.2), (mx + 0.4, r)], y, Z, "steel_dark")
        if base_y is not None:
            m.box((mx - 0.4, base_y, Z - r * 0.75), (mx + 0.4, y - r + 0.05, Z + r * 0.75), "steel_dark", r=0.1)
            m.box((mx - 0.55, base_y, Z - r - 0.35), (mx + 0.55, base_y + 0.5, Z + r + 0.35), "steel_dark", r=0.12)
            screw(m, mx, base_y + 0.25, Z + r + 0.35, 1, r=0.14)
    return y + r * 0.1


def red_dot(m, x, y, tex="alu", big=False):
    """A tube red dot on a low mount: (x, y) is the base's middle on the rail top. Returns the sight line's height."""
    r = 0.75 if big else 0.62
    cy = y + 0.75 + r
    m.box((x - 1.3, y, Z - 0.75), (x + 1.3, y + 0.55, Z + 0.75), "steel_dark", r=0.12)
    m.box((x - 0.8, y + 0.4, Z - 0.6), (x + 0.8, cy - r * 0.5, Z + 0.6), tex, r=0.2)
    m.lathe("x", [(x - 1.5, r * 0.8), (x - 1.5, r), (x + 1.5, r), (x + 1.5, r * 0.8)], cy, Z, tex)
    m.lathe("x", [(x + 1.2, r * 0.8), (x + 1.2, 0)], cy, Z, "lens", icon=False)
    m.lathe("x", [(x - 1.2, 0), (x - 1.2, r * 0.8)], cy, Z, "lens", icon=False)
    m.cyl("y", cy + r * 0.6, cy + r + 0.35, x, Z, 0.32, "knurl", mesh_only=True)
    m.cyl("z", Z + r * 0.6, Z + r + 0.35, x, cy, 0.32, tex, mesh_only=True)
    m.cyl("x", x + 0.5, x + 0.52, cy, Z, 0.06, "red_dot", mesh_only=True)                       # the dot
    return cy


def holo(m, x, y, tex="polymer_black"):
    """A holographic sight: a box body, the hood, the window glass. Returns the sight line's height."""
    m.box((x - 2.0, y, Z - 0.85), (x + 1.6, y + 0.9, Z + 0.85), tex, r=0.25)
    m.box((x - 2.0, y + 0.9, Z - 0.85), (x - 0.9, y + 1.25, Z + 0.85), tex, r=0.15)
    m.plate([(x + 0.1, y + 0.8), (x + 1.6, y + 0.8), (x + 1.6, y + 2.35), (x + 0.1, y + 2.35), (x + 0.1, y + 2.1), (x + 1.35, y + 2.1),
             (x + 1.35, y + 0.98), (x + 0.1, y + 0.98)], Z - 0.85, Z + 0.85, tex, bevel=0.1)
    m.box((x + 0.1, y + 0.8, Z - 0.85), (x + 1.6, y + 2.35, Z - 0.68), tex, r=0.06)
    m.box((x + 0.1, y + 0.8, Z + 0.68), (x + 1.6, y + 2.35, Z + 0.85), tex, r=0.06)
    m.box((x + 0.75, y + 1.0, Z - 0.66), (x + 0.79, y + 2.08, Z + 0.66), "lens", mesh_only=True)
    m.box((x + 0.76, y + 1.5, Z - 0.05), (x + 0.8, y + 1.6, Z + 0.05), "red_dot", mesh_only=True)
    for k in range(3):
        m.box((x - 1.7 + k * 0.4, y + 0.9, Z + 0.85), (x - 1.5 + k * 0.4, y + 1.1, Z + 0.88), "steel_dark", faces=["south"], mesh_only=True)
    return y + 1.55


def iron_post(m, x, y, h=1.2, tex="steel_dark", hood=True):
    """A front sight: a base, a post with a white dot, wings protecting it. Returns the post top."""
    m.box((x - 0.45, y, Z - 0.5), (x + 0.45, y + h * 0.35, Z + 0.5), tex, r=0.1)
    m.box((x - 0.1, y + h * 0.3, Z - 0.09), (x + 0.1, y + h, Z + 0.09), tex, r=0.04)
    if hood:
        for s in (-1, 1):
            m.box((x - 0.35, y + h * 0.3, Z + s * 0.55 - 0.09), (x + 0.35, y + h * 1.12, Z + s * 0.55 + 0.09), tex, r=0.06)
    return y + h


def iron_rear(m, x, y, h=1.0, tex="steel_dark"):
    """A rear aperture sight: base, two ears, the ring."""
    m.box((x - 0.5, y, Z - 0.7), (x + 0.5, y + 0.4, Z + 0.7), tex, r=0.1)
    m.lathe("x", [(x - 0.12, 0.12), (x - 0.12, 0.32), (x + 0.12, 0.32), (x + 0.12, 0.12)], y + h * 0.75, Z, tex)
    m.box((x - 0.15, y + 0.35, Z - 0.12), (x + 0.15, y + h * 0.75 - 0.25, Z + 0.12), tex, r=0.04)
    for s in (-1, 1):
        m.box((x - 0.35, y + 0.3, Z + s * 0.58 - 0.1), (x + 0.35, y + h, Z + s * 0.58 + 0.1), tex, r=0.06)
    return y + h * 0.75


# ---------------------------------------------------------------------------------------------------------------
# Magazines and feeds
# ---------------------------------------------------------------------------------------------------------------
def drum(m, x, y, r=2.2, depth=2.4, tex="steel_dark", z=Z, ribs=True):
    """A drum magazine: a round can across the gun (axis z), ribbed face, winding key."""
    z0, z1 = z - depth / 2, z + depth / 2
    m.lathe("z", [(z0, 0), (z0, r - 0.2), (z0 + 0.2, r), (z1 - 0.2, r), (z1, r - 0.2), (z1, 0)], x, y, tex)
    if ribs:
        for k in range(3):
            rr = r * (0.35 + k * 0.2)
            m.lathe("z", [(z1, rr), (z1 + 0.06, rr), (z1 + 0.06, rr + 0.12), (z1, rr + 0.12)], x, y, tex, icon=False)
    m.cyl("z", z1, z1 + 0.35, x, y, 0.3, "steel_light", mesh_only=True)
    m.box((x - 0.12, y - 0.6, z1 + 0.3), (x + 0.12, y + 0.6, z1 + 0.42), "steel_light", r=0.04, mesh_only=True)


def belt(m, x0, y0, n=6, dx=0.42, droop=0.25, tex="brass", z=Z + 0.3):
    """A short length of belted rounds hanging from the feed tray, across the gun (+z side)."""
    for k in range(n):
        xx = x0 + k * 0.05
        yy = y0 - k * dx - droop * (k / n) ** 2
        zz = z + 1.0 + k * 0.1
        cartridge(m, xx, yy, zz - 0.6, length=1.7, r=0.19, axis="x")
        m.box((xx + 0.35, yy - 0.12, zz - 0.62), (xx + 0.75, yy + 0.12, zz - 0.58), "steel_dark", r=0.02, mesh_only=True)
