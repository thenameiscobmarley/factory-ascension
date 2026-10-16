"""An automatic detail pass run on every gun after its design (tools/guns/build.py): roll-marked name and caliber on
both sides of the receiver, and cross pins through it above the trigger and the grip. Placed from the gun's own mesh
(the receiver's side surface is found by its vertices), so it fits each gun."""
import mesh as M

# A 3x5 stamping font (ours): rows top to bottom, '#' = stamped
FONT = {
    "A": ["###", "#.#", "###", "#.#", "#.#"], "B": ["##.", "#.#", "##.", "#.#", "##."], "C": ["###", "#..", "#..", "#..", "###"],
    "D": ["##.", "#.#", "#.#", "#.#", "##."], "E": ["###", "#..", "##.", "#..", "###"], "F": ["###", "#..", "##.", "#..", "#.."],
    "G": ["###", "#..", "#.#", "#.#", "###"], "H": ["#.#", "#.#", "###", "#.#", "#.#"], "I": ["###", ".#.", ".#.", ".#.", "###"],
    "J": ["..#", "..#", "..#", "#.#", "###"], "K": ["#.#", "##.", "#..", "##.", "#.#"], "L": ["#..", "#..", "#..", "#..", "###"],
    "M": ["#.#", "###", "###", "#.#", "#.#"], "N": ["##.", "#.#", "#.#", "#.#", "#.#"], "O": ["###", "#.#", "#.#", "#.#", "###"],
    "P": ["###", "#.#", "###", "#..", "#.."], "Q": ["###", "#.#", "#.#", "###", "..#"], "R": ["###", "#.#", "##.", "#.#", "#.#"],
    "S": ["###", "#..", "###", "..#", "###"], "T": ["###", ".#.", ".#.", ".#.", ".#."], "U": ["#.#", "#.#", "#.#", "#.#", "###"],
    "V": ["#.#", "#.#", "#.#", "#.#", ".#."], "W": ["#.#", "#.#", "###", "###", "#.#"], "X": ["#.#", "#.#", ".#.", "#.#", "#.#"],
    "Y": ["#.#", "#.#", ".#.", ".#.", ".#."], "Z": ["###", "..#", ".#.", "#..", "###"],
    "0": ["###", "#.#", "#.#", "#.#", "###"], "1": [".#.", "##.", ".#.", ".#.", "###"], "2": ["###", "..#", "###", "#..", "###"],
    "3": ["###", "..#", "###", "..#", "###"], "4": ["#.#", "#.#", "###", "..#", "..#"], "5": ["###", "#..", "###", "..#", "###"],
    "6": ["###", "#..", "###", "#.#", "###"], "7": ["###", "..#", "..#", "..#", "..#"], "8": ["###", "#.#", "###", "#.#", "###"],
    "9": ["###", "#.#", "###", "..#", "###"], "-": ["...", "...", "###", "...", "..."], ".": ["...", "...", "...", "...", ".#."],
    " ": ["...", "...", "...", "...", "..."], "/": ["..#", "..#", ".#.", "#..", "#.."],
}
M.MATS.setdefault("rollmark", ("metal", (128, 130, 134), M.NOWEAR))


def surface_z(mesh, x0, x1, y0, y1, side):
    """The receiver's side face in this box: the outermost z of vertices facing that way, or None."""
    best = None
    for k in mesh.verts:
        x, y, z = k[0] / 500, k[1] / 500, k[2] / 500
        nz = k[5] / 127
        if x0 <= x <= x1 and y0 <= y <= y1 and nz * side > 0.85:
            if best is None or z * side > best * side:
                best = z
    return best


def stamp(mesh, text, x, y, z, px, side):
    """Text, left to right along +x, its top at y, flush on the face at z facing `side` (+1 / -1)."""
    mat = "rollmark"
    cx = x
    face = ["south"] if side > 0 else ["north"]
    for ch in text.upper():
        g = FONT.get(ch)
        if g is None:
            cx += px * 4
            continue
        cols = range(3) if side > 0 else range(2, -1, -1)
        for r, row in enumerate(g):
            for ci, c in enumerate(row):
                if c != "#":
                    continue
                gx = cx + (ci if side > 0 else 2 - ci) * px
                gy = y - r * px
                z0, z1 = (z, z + 0.012) if side > 0 else (z - 0.012, z)
                M.rbox(mesh, (gx, gy - px * 0.9, z0), (gx + px * 0.9, gy, z1), mat, 0.0, faces=face)
        cx += px * 4


def flat_map(mesh, side, cell=0.1):
    """Cells (x, y) -> the frontmost z of flat faces looking toward `side`, rasterized from the quads themselves (a big
    flat face has vertices only at its corners), and -> how many faces cover it (a step or a cut shows as a z jump)."""
    import math
    zmax, zmin, mats = {}, {}, {}
    for q in mesh.quads:
        pa, pb, pc, pd = (mesh.pos(i) for i in q)
        e1 = M.sub(pc, pa)
        e2 = M.sub(pd, pb)
        n = M.cross(e1, e2)
        ln = math.sqrt(M.dot(n, n))
        if ln < 1e-9:
            continue
        nz = n[2] / ln
        # quads are wound to face their vertex normals; trust the vertex normal's sign
        vn = mesh.verts[q[0]][5] / 127
        if vn * side < 0.9:
            continue
        la = max(M.dist(pa, pb), M.dist(pd, pc))
        lb = max(M.dist(pa, pd), M.dist(pb, pc))
        nu, nv = max(1, int(la / cell) + 1), max(1, int(lb / cell) + 1)
        for i in range(nu + 1):
            u = i / nu
            e0 = (pa[0] + (pb[0] - pa[0]) * u, pa[1] + (pb[1] - pa[1]) * u, pa[2] + (pb[2] - pa[2]) * u)
            e3 = (pd[0] + (pc[0] - pd[0]) * u, pd[1] + (pc[1] - pd[1]) * u, pd[2] + (pc[2] - pd[2]) * u)
            for j in range(nv + 1):
                v = j / nv
                x, y, z = (e0[k] + (e3[k] - e0[k]) * v for k in range(3))
                key = (int(math.floor(x / cell)), int(math.floor(y / cell)))
                if key not in zmax or z * side > zmax[key] * side:
                    zmax[key] = z
                    mats[key] = mesh.verts[q[0]][6]
                if key not in zmin or z * side < zmin[key] * side:
                    zmin[key] = z
    return zmax, cell, mats


def find_spot(fm, x_lo, x_hi, y_lo, y_hi, w, h, prefer, side):
    """The flat, unbroken patch w x h nearest `prefer` (x, y) inside the search box: (x0, ytop, z) or None."""
    zmax, cell, mats = fm
    best, best_d = None, 1e9
    y = y_hi
    while y - h >= y_lo:
        x = x_lo
        while x + w <= x_hi:
            zs = []
            ms = set()
            ok = True
            cx = x
            while cx <= x + w and ok:
                cy = y - h
                while cy <= y and ok:
                    z = zmax.get((int(cx // cell), int(cy // cell)))
                    if z is None:
                        ok = False
                    else:
                        zs.append(z)
                        ms.add(mats.get((int(cx // cell), int(cy // cell))))
                    cy += cell * 1.5
                cx += cell * 1.5
            if ok and zs and len(ms) == 1 and max(zs) - min(zs) < 0.02 and abs(zs[0] - 8) > 0.4:
                d = abs(x + w / 2 - prefer[0]) + 1.5 * abs(y - prefer[1])
                if d < best_d:
                    best, best_d = (x, y, max(zs) if side > 0 else min(zs)), d
            x += 0.25
        y -= 0.2
    return best


def run(m, name, caliber, geo_mounts):
    """geo_mounts: the slot -> (x, y, z) dict build.py measures."""
    mesh = m.mesh
    lo, hi = m.bounds()
    length = hi[0] - lo[0]
    pistol = length < 20
    px = 0.085 if pistol else 0.11
    text = ((name.split()[0] if pistol else name) + " " + caliber.replace("mm", "").replace("MM", "")).upper()
    width = len(text) * px * 4
    mx, my = geo_mounts["MAGAZINE"][0], geo_mounts["MAGAZINE"][1]
    ox, oy = geo_mounts["OPTIC"][0], geo_mounts["OPTIC"][1]
    # The roll mark: along the receiver side, between the grip and the front of the magazine, below the top
    # Cross pins through the receiver above the trigger and the grip
    tx, ty = geo_mounts["TRIGGER"][0], geo_mounts["TRIGGER"][1]
    for (pxx, pyy) in ((tx + 0.7, ty + 1.7), (tx - 1.4, ty + 1.9)):
        zp = surface_z(mesh, pxx - 0.4, pxx + 0.4, pyy - 0.4, pyy + 0.4, 1)
        zn = surface_z(mesh, pxx - 0.4, pxx + 0.4, pyy - 0.4, pyy + 0.4, -1)
        if zp is None or zn is None or zp - zn < 0.6:
            continue
        M.cylinder(mesh, "z", zn - 0.04, zp + 0.04, pxx, pyy, 0.14, "steel_light", seg=12)
    h = px * 5
    for side in (1, -1):
        fm = flat_map(mesh, side)
        spot = find_spot(fm, lo[0] + 1, hi[0] - 1, lo[1], oy + 0.5, width + px, h + px, (min(mx, ox), oy - 1.0), side)
        if spot is None and len(text) > 8:
            text = name.split()[0].upper()
            width = len(text) * px * 4
            spot = find_spot(fm, lo[0] + 1, hi[0] - 1, lo[1], oy + 0.5, width + px, h + px, (min(mx, ox), oy - 1.0), side)
        if spot is None:
            continue
        x0, ytop, z = spot
        stamp(mesh, text, x0 + px * 0.5, ytop - px * 0.5, z, px, side)
