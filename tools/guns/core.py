"""Building blocks for the gun models (tools/guns/build.py).

Coordinates are Minecraft model units (16 = one block), each axis -16..32. Guns point down +x (muzzle at high x),
up is +y, and the gun's width is z, centred on z = 8. The pistol grip sits around x 5..8 so the hand (the model's
centre) is on it. Element rotations are only 0 / +-22.5 / +-45 (anything else and Minecraft rejects the model).

Textures are 64x64; a face's UVs cover its own size (1 unit = 4 texels), so every texture shows at the same scale,
and each part starts at a random spot in the texture so neighbours don't repeat.
"""
import math
import random

NS = "factoryascension"
ANGLES = (0, 22.5, -22.5, 45, -45)


class Model:
    def __init__(self, name, seed=1):
        self.name = name
        self.els = []
        self.rnd = random.Random(seed)
        self.used = set()
        self.sight = None          # (x, y) of the sight line's front point, set by the builder
        self.stock_rear = None     # the rearmost x that's at eye height (stock comb / back of the gun)

    # ---- The one primitive: a box ----
    def box(self, frm, to, tex, faces=None, rot=None, full=False, uvscale=1.0, shade=True):
        frm = [round(v, 3) for v in frm]
        to = [round(v, 3) for v in to]
        for i in range(3):
            if to[i] < frm[i]:
                frm[i], to[i] = to[i], frm[i]
            if to[i] - frm[i] < 0.01:
                to[i] = frm[i] + 0.01
        sx, sy, sz = (to[i] - frm[i] for i in range(3))
        sizes = {"north": (sx, sy), "south": (sx, sy), "east": (sz, sy), "west": (sz, sy), "up": (sx, sz), "down": (sx, sz)}
        out = {}
        texes = tex if isinstance(tex, dict) else None
        for f, (w, h) in sizes.items():
            if faces and f not in faces:
                continue
            t = texes.get(f, texes.get("*")) if texes else tex
            if t is None:
                continue
            self.used.add(t)
            if full:
                uv = [0, 0, 16, 16]
            else:
                w2 = min(16, max(0.25, w * uvscale))
                h2 = min(16, max(0.25, h * uvscale))
                u = self.rnd.uniform(0, 16 - w2)
                v = self.rnd.uniform(0, 16 - h2)
                uv = [round(u, 2), round(v, 2), round(u + w2, 2), round(v + h2, 2)]
            face = {"uv": uv, "texture": "#" + t}
            out[f] = face
        if not out:
            return None
        el = {"from": frm, "to": to, "faces": out}
        if not shade:
            el["shade"] = False
        if rot:
            axis, ang, origin = rot
            assert ang in ANGLES, ang
            if ang != 0:
                el["rotation"] = {"axis": axis, "angle": ang, "origin": [round(v, 3) for v in origin]}
        self.els.append(el)
        return el

    # ---- Round things ----
    def cyl(self, axis, a0, a1, c1, c2, r, tex, sides=8, end_tex=None, ends=True):
        """A cylinder along `axis` from a0 to a1, centred on (c1, c2) of the other two axes, radius r.
        8 sides = a square and the same square turned 45; 16 sides adds the two 22.5 turns."""
        angles = (0, 45) if sides == 8 else (0, 22.5, 45, -22.5)
        half = r   # each square's half side is the radius to a flat; the turned copies round it off
        for i, ang in enumerate(angles):
            if axis == "x":
                frm, to = (a0, c1 - half, c2 - half), (a1, c1 + half, c2 + half)
                face_ends = ["east", "west"]
                faces = ["up", "down", "north", "south"]
                origin = (a0, c1, c2)
            elif axis == "y":
                frm, to = (c1 - half, a0, c2 - half), (c1 + half, a1, c2 + half)
                face_ends = ["up", "down"]
                faces = ["east", "west", "north", "south"]
                origin = (c1, a0, c2)
            else:
                frm, to = (c1 - half, c2 - half, a0), (c1 + half, c2 + half, a1)
                face_ends = ["north", "south"]
                faces = ["east", "west", "up", "down"]
                origin = (c1, c2, a0)
            fs = faces + (face_ends if ends and i == 0 else [])
            tx = {f: tex for f in faces}
            for f in face_ends:
                tx[f] = end_tex or tex
            self.box(frm, to, tx, faces=fs, rot=(axis, ang, origin) if ang else None)

    def tube(self, x0, x1, cy, cz, r, tex, bore="bore", sides=8, wall=0.25):
        """A barrel along x with a dark bore in the muzzle end."""
        self.cyl("x", x0, x1, cy, cz, r, tex, sides=sides)
        br = max(0.15, r - wall)
        self.box((x1 + 0.01, cy - br, cz - br), (x1 + 0.03, cy + br, cz + br), bore, faces=["east"], full=True)

    def screw(self, x, y, z, side="south", r=0.28, tex="steel_dark"):
        """A screw head on a side face (south = +z side, north = -z side)."""
        d = 0.06
        if side == "south":
            self.box((x - r, y - r, z), (x + r, y + r, z + d), tex, faces=["south", "up", "down", "east", "west"])
            self.box((x - r * 0.8, y - 0.05, z + d), (x + r * 0.8, y + 0.05, z + d + 0.01), "bore", faces=["south"])
        else:
            self.box((x - r, y - r, z - d), (x + r, y + r, z), tex, faces=["north", "up", "down", "east", "west"])
            self.box((x - r * 0.8, y - 0.05, z - d - 0.01), (x + r * 0.8, y + 0.05, z - d), "bore", faces=["north"])

    def screws_both(self, x, y, z0, z1, **kw):
        self.screw(x, y, z1, "south", **kw)
        self.screw(x, y, z0, "north", **kw)

    def rail(self, x0, x1, y, cz, w=1.6, tex="rail", teeth=True, pitch=0.9):
        """A picatinny rail on top at height y: the base, and its raised teeth with slots between."""
        self.box((x0, y, cz - w / 2), (x1, y + 0.45, cz + w / 2), tex)
        if not teeth:
            return
        x = x0 + 0.15
        while x + pitch * 0.55 <= x1:
            self.box((x, y + 0.45, cz - w / 2 - 0.08), (x + pitch * 0.55, y + 0.85, cz + w / 2 + 0.08), tex,
                     faces=["up", "east", "west", "north", "south"])
            x += pitch

    def side_rail(self, x0, x1, cy, z, side, h=1.4, tex="rail", pitch=0.9):
        """A rail on a side (z face), teeth pointing out."""
        d = 0.4 if side == "south" else -0.4
        z0, z1 = (z, z + d) if d > 0 else (z + d, z)
        self.box((x0, cy - h / 2, z0), (x1, cy + h / 2, z1), tex)
        x = x0 + 0.15
        while x + pitch * 0.55 <= x1:
            zz0, zz1 = (z1, z1 + 0.35) if d > 0 else (z0 - 0.35, z0)
            self.box((x, cy - h / 2 - 0.08, zz0), (x + pitch * 0.55, cy + h / 2 + 0.08, zz1), tex)
            x += pitch

    def serrations(self, x0, x1, y0, y1, z0, z1, n, tex="bore", depth=0.06):
        """Grip grooves cut across a slide: thin dark slots on both sides."""
        step = (x1 - x0) / n
        for i in range(n):
            x = x0 + i * step + step * 0.25
            self.box((x, y0, z1), (x + step * 0.45, y1, z1 + depth), tex, faces=["south"])
            self.box((x, y0, z0 - depth), (x + step * 0.45, y1, z0), tex, faces=["north"])

    def checkering(self, x0, x1, y0, y1, z0, z1, tex="grip_check"):
        """A checkered grip panel on both sides."""
        self.box((x0, y0, z1), (x1, y1, z1 + 0.08), tex, faces=["south"])
        self.box((x0, y0, z0 - 0.08), (x1, y1, z0), tex, faces=["north"])

    CURVE = 1   # +1: positive z rotation swings a part's bottom toward +x (checked in the preview)

    def mag(self, x, y_top, length, depth, z0, z1, tex, curve=0, base="steel_dark", ribs=0, rib_tex=None):
        """A magazine below `y_top`, front edge at x. curve 0 is straight; 1 and 2 bend its lower part forward (AK style)
        with parts turned 22.5 and then 45 degrees. A base plate at the bottom, ribs down the sides."""
        segs = 1 + curve
        seg_h = depth / segs
        tx, ty = x + length / 2, y_top          # top centre of the current part
        ang = 0
        for i in range(segs):
            ang = 0 if i == 0 else (22.5 if i == 1 else 45) * self.CURVE
            origin = (tx, ty, 8)
            self.box((tx - length / 2, ty - seg_h - 0.05, z0), (tx + length / 2, ty, z1), tex, rot=("z", ang, origin) if ang else None)
            if ribs and i == 0:
                for k in range(ribs):
                    yy = ty - (k + 1) * seg_h / (ribs + 1)
                    self.box((tx - length / 2 + 0.3, yy - 0.12, z1), (tx + length / 2 - 0.3, yy + 0.12, z1 + 0.06), rib_tex or tex, faces=["south"])
                    self.box((tx - length / 2 + 0.3, yy - 0.12, z0 - 0.06), (tx + length / 2 - 0.3, yy + 0.12, z0), rib_tex or tex, faces=["north"])
            a = math.radians(ang)
            # The part's bottom centre, after turning about its top centre (x' = -(-h) sin a, y' = -h cos a)
            tx, ty = tx + seg_h * math.sin(a), ty - seg_h * math.cos(a)
        self.box((tx - length / 2 - 0.2, ty - 0.5, z0 - 0.2), (tx + length / 2 + 0.2, ty, z1 + 0.2), base,
                 rot=("z", ang, (tx, ty, 8)) if ang else None)

    def grip(self, x, y, w, h, z0, z1, tex, angle=-22.5, check_tex=None, base=None):
        """A pistol grip hanging from (x, y), raked back by `angle`; `base` adds a magazine base plate at its bottom."""
        origin = (x + w / 2, y, 8)
        self.box((x, y - h, z0), (x + w, y, z1), tex, rot=("z", angle, origin))
        if base:
            self.box((x - 0.15, y - h - 0.55, z0 - 0.08), (x + w + 0.15, y - h, z1 + 0.08), base, rot=("z", angle, origin))
        if check_tex:
            self.box((x + 0.3, y - h + 0.6, z1), (x + w - 0.3, y - 0.8, z1 + 0.07), check_tex, faces=["south"], rot=("z", angle, origin))
            self.box((x + 0.3, y - h + 0.6, z0 - 0.07), (x + w - 0.3, y - 0.8, z0), check_tex, faces=["north"], rot=("z", angle, origin))

    def trigger_guard(self, x0, x1, y_top, depth, tex, z=8, t=0.35, trig_tex="steel"):
        """A trigger guard loop under the receiver and the trigger inside it."""
        self.box((x0, y_top - depth, z - 0.3), (x1, y_top - depth + t, z + 0.3), tex)
        self.box((x1 - t, y_top - depth, z - 0.3), (x1, y_top, z + 0.3), tex)
        self.box((x0, y_top - depth, z - 0.3), (x0 + t, y_top - depth * 0.45, z + 0.3), tex)
        tx = x0 + (x1 - x0) * 0.55
        self.box((tx, y_top - depth * 0.8, z - 0.2), (tx + 0.35, y_top, z + 0.2), trig_tex, rot=("z", 22.5, (tx, y_top - depth * 0.4, z)))

    def lens(self, x, cy, cz, r, tex="lens", facing="east"):
        """Glass at the end of a scope tube."""
        f = facing
        d = 0.02 if f == "east" else -0.02
        self.box((x, cy - r, cz - r), (x + d, cy + r, cz + r), tex, faces=[f], full=True)

    def scope(self, x0, x1, cy, cz, r, tex="scope_body", bell=1.35, turrets=True, mounts=None, mount_tex="steel_dark", base_y=None):
        """A riflescope: tube, objective and eyepiece bells, turrets, lenses and rings down to base_y."""
        L = x1 - x0
        self.cyl("x", x0 + L * 0.22, x1 - L * 0.28, cy, cz, r, tex, sides=16)
        self.cyl("x", x1 - L * 0.28, x1 - L * 0.18, cy, cz, (r + r * bell) / 2, tex, sides=16)
        self.cyl("x", x1 - L * 0.18, x1, cy, cz, r * bell, tex, sides=16, ends=False)
        self.cyl("x", x0 + L * 0.08, x0 + L * 0.22, cy, cz, (r + r * 1.2) / 2, tex, sides=16)
        self.cyl("x", x0, x0 + L * 0.08, cy, cz, r * 1.2, tex, sides=16, ends=False)
        self.cyl("x", x0 + L * 0.03, x0 + L * 0.06, cy, cz, r * 1.24, "rubber", sides=16, ends=False)      # eyepiece rubber
        self.lens(x1, cy, cz, r * bell * 0.92)
        self.lens(x0, cy, cz, r * 1.1, facing="west")
        if turrets:
            mid = x0 + L * 0.45
            self.cyl("y", cy + r * 0.7, cy + r + 0.9, mid, cz, 0.75, tex, sides=16, end_tex="knurl")
            self.cyl("z", cz + r * 0.7, cz + r + 0.9, mid, cy, 0.75, tex, sides=16, end_tex="knurl")
            self.cyl("z", cz - r - 0.9, cz - r * 0.7, mid - 1.6, cy, 0.55, tex, sides=8, end_tex="knurl")
        for mx in (mounts or (x0 + L * 0.3, x1 - L * 0.36)):
            if base_y is not None:
                self.box((mx - 0.5, base_y, cz - r * 0.9), (mx + 0.5, cy, cz + r * 0.9), mount_tex)
            self.box((mx - 0.5, cy - r - 0.15, cz - r - 0.15), (mx + 0.5, cy + r + 0.15, cz + r + 0.15), mount_tex,
                     faces=["up", "north", "south", "down"])
            self.screw(mx, cy - r * 0.2, cz + r + 0.15, "south", r=0.2)

    def iron_sights(self, xr, xf, y, cz, h=1.4, tex="steel_dark", post_tex="steel"):
        """A rear aperture (two ears) and a front post in a hood; the sight line runs over them."""
        self.box((xr - 0.5, y, cz - 0.9), (xr + 0.5, y + h * 0.55, cz + 0.9), tex)
        self.box((xr - 0.4, y + h * 0.55, cz - 0.9), (xr + 0.4, y + h, cz - 0.45), tex)
        self.box((xr - 0.4, y + h * 0.55, cz + 0.45), (xr + 0.4, y + h, cz + 0.9), tex)
        self.box((xr - 0.2, y + h * 0.55, cz - 0.45), (xr + 0.2, y + h * 0.8, cz + 0.45), tex)    # the aperture's bottom
        self.box((xf - 0.5, y, cz - 0.8), (xf + 0.5, y + h * 0.4, cz + 0.8), tex)
        self.box((xf - 0.4, y + h * 0.4, cz - 0.8), (xf + 0.4, y + h * 1.05, cz - 0.6), tex)       # hood wings
        self.box((xf - 0.4, y + h * 0.4, cz + 0.6), (xf + 0.4, y + h * 1.05, cz + 0.8), tex)
        self.box((xf - 0.12, y + h * 0.4, cz - 0.12), (xf + 0.12, y + h * 0.85, cz + 0.12), post_tex)
        self.sight = (xf, y + h * 0.82)

    def profile(self, pts, w, tex, step=0.8, cz=8.0, taper=None):
        """A shape along x from its outline: pts = [(x, y_low, y_high), ...] in order of x; it's cut into thin slices
        (each `step` wide) whose heights follow the outline, so stocks and grips get smooth curves. `taper` =
        (x, width) pairs makes the width change along it too."""
        xs = [p[0] for p in pts]
        x = xs[0]
        while x < xs[-1] - 1e-6:
            x1 = min(xs[-1], x + step)
            xm = (x + x1) / 2
            for i in range(len(pts) - 1):
                if pts[i][0] <= xm <= pts[i + 1][0]:
                    a, b = pts[i], pts[i + 1]
                    k = (xm - a[0]) / max(1e-6, b[0] - a[0])
                    lo, hi = a[1] + (b[1] - a[1]) * k, a[2] + (b[2] - a[2]) * k
                    break
            ww = w
            if taper:
                for i in range(len(taper) - 1):
                    if taper[i][0] <= xm <= taper[i + 1][0]:
                        k = (xm - taper[i][0]) / max(1e-6, taper[i + 1][0] - taper[i][0])
                        ww = taper[i][1] + (taper[i + 1][1] - taper[i][1]) * k
            self.box((x, lo, cz - ww / 2), (x1, hi, cz + ww / 2), tex)
            x = x1

    def bounds(self):
        lo = [min(e["from"][i] for e in self.els) for i in range(3)]
        hi = [max(e["to"][i] for e in self.els) for i in range(3)]
        return lo, hi

    def check(self):
        bad = [e for e in self.els if "rotation" in e and e["rotation"]["angle"] not in ANGLES]
        assert not bad, self.name + ": rotation Minecraft rejects"
        lo, hi = self.bounds()
        assert min(lo) >= -16 and max(hi) <= 32, "%s: outside -16..32 %s %s" % (self.name, lo, hi)
