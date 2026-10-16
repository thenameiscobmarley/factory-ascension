#!/usr/bin/env python3
"""Builds every gun model (gun/Guns.java) from code: high-detail cuboid models, 64x64 textures, and each gun's
sight line and stock position (so aiming puts the sights on the crosshair and never the camera inside the stock).

  python3 tools/guns/build.py            all of them
  python3 tools/guns/build.py m4a1 ak74  just those (and the shared textures)

Writes models/item/<id>.json, textures/item/gun2/*.png and mod/.../gun/GunGeometry.java.
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, ".."))
import make_textures as T  # noqa: E402
from core import Model, NS  # noqa: E402
from mesh import MeshModel  # noqa: E402
import mesh_tex  # noqa: E402
import tex  # noqa: E402
import designs  # noqa: E402
import designs2  # noqa: E402
import attach  # noqa: E402

SLOTS = ["MUZZLE", "BARREL", "HANDGUARD", "FOREGRIP", "OPTIC", "STOCK", "MAGAZINE", "UNDERBARREL", "TACTICAL", "GRIP", "TRIGGER", "CHARM"]


def mounts(m, gid):
    """Where each attachment slot sits on this gun, measured from its parts (model units)."""
    lo, hi = m.bounds()
    els = m.els

    def covering(x, zc=8.0):
        return [e for e in els if e["from"][0] <= x <= e["to"][0] and e["from"][2] <= zc + 0.3 and e["to"][2] >= zc - 0.3 and "rotation" not in e]

    def top(x):
        c = covering(x)
        return max((e["to"][1] for e in c), default=m.sight[1] - 1)

    def bottom(x):
        c = covering(x)
        return min((e["from"][1] for e in c), default=m.sight[1] - 3)

    def side(x):
        c = [e for e in els if e["from"][0] <= x <= e["to"][0]]
        return max((e["to"][2] for e in c), default=9)
    bores = [e for e in els if "east" in e["faces"] and e["faces"]["east"]["texture"] == "#bore" and e["from"][0] > (lo[0] + hi[0]) / 2]
    if bores:
        e = max(bores, key=lambda e: e["to"][0])
        mx, my = e["to"][0], (e["from"][1] + e["to"][1]) / 2
    else:
        mx, my = hi[0], m.sight[1] - 1.5
    pistol = hi[0] - lo[0] < 20
    xo = 6.0 if pistol else 5.0
    xu = 10.0 if pistol else min(mx - 5, 18.0)
    xm = 3.0 if pistol else 9.0
    pts = {
        "MUZZLE": (mx, my, 8), "BARREL": (mx - 3, my, 8), "HANDGUARD": (xu, my, 8), "FOREGRIP": (xu, bottom(xu), 8),
        "OPTIC": (xo, top(xo), 8), "STOCK": (lo[0], m.sight[1] - 2.5, 8), "MAGAZINE": (xm + (0 if pistol else 0.5), bottom(xm + 0.5) + 0.4, 8),
        "UNDERBARREL": (xu + 1.5, bottom(xu + 1.5), 8), "TACTICAL": (xu - 1, my, side(xu - 1)), "GRIP": (4.0, 5.5, 8),
        "TRIGGER": (5.0, 6.0, 8), "CHARM": (2.0, top(2.0) - 1.6, side(2.0)),
    }
    return [round(v, 2) for k in SLOTS for v in pts[k]]

ROOT = os.path.join(HERE, "..", "..")
ASSETS = os.path.join(ROOT, "mod", "src", "main", "resources", "assets", NS)
TEXDIR = os.path.join(ASSETS, "textures", "item", "gun2")
JAVA = os.path.join(ROOT, "mod", "src", "main", "java", "dev", "factoryascension", "gun", "GunGeometry.java")

# First-person hold: the model's +x (muzzle) turned to face ahead; T is how high it sits, S its size
FP_T, FP_S = 2.0, 0.6


def display(m, scale_fp):
    lo, hi = m.bounds()
    length = hi[0] - lo[0]
    cx, cy = (lo[0] + hi[0]) / 2, (lo[1] + hi[1]) / 2
    gs = round(min(1.0, 15.5 / max(length, (hi[1] - lo[1]) * 1.6)), 3)     # fills the 16 px icon along its length
    gui_t = [round(-gs * (cx - 8), 2), round(-gs * (cy - 8), 2), 0]
    fp = {"rotation": [0, 90, 0], "translation": [0, FP_T, 0], "scale": [scale_fp] * 3}
    return {
        "gui": {"rotation": [0, 0, 0], "translation": gui_t, "scale": [gs] * 3},
        "fixed": {"rotation": [0, 0, 0], "translation": gui_t, "scale": [gs] * 3},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [round(gs * 0.8, 3)] * 3},
        "firstperson_righthand": fp,
        "firstperson_lefthand": fp,
        # Held in third person (both arms forward): the muzzle along the arms, the top of the gun up
        "thirdperson_righthand": {"rotation": [0, 90, 0], "translation": [0, 2.5, 1.0], "scale": [round(scale_fp * 0.85, 3)] * 3},
        "thirdperson_lefthand": {"rotation": [0, 90, 0], "translation": [0, 2.5, 1.0], "scale": [round(scale_fp * 0.85, 3)] * 3},
        "head": {"rotation": [0, 90, 0], "translation": [0, 8, 0], "scale": [0.5] * 3},
    }


def geometry(m, scale_fp, relief):
    """Where to hold it when aiming: the sight line on the screen's centre and the rear of the gun `relief` blocks
    ahead of the eye (so the near clipping plane never cuts into the stock)."""
    sx, sy = m.sight
    rear = m.stock_rear if m.stock_rear is not None else m.bounds()[0][0]
    ads_y = -(FP_T / 16 + (sy - 8) * scale_fp / 16)
    ads_z = -(relief + (8 - rear) * scale_fp / 16)
    return round(ads_y, 4), round(ads_z, 4)


def bench_model():
    """The Weapons Workbench: a steel bench with a rubber mat, a vise, a pegboard of tools behind it, a rifle on the
    mat and two ammo boxes on the shelf underneath."""
    m = Model("weapons_workbench", seed=7)
    for x, z in ((0.5, 1.5), (14.0, 1.5), (0.5, 13.0), (14.0, 13.0)):
        m.box((x, 0, z), (x + 1.5, 12.5, z + 1.5), "steel")
    m.box((0, 12.5, 0), (16, 14.0, 16), "steel")
    m.box((1, 14.0, 3), (15, 14.3, 15), "rubber")                                   # mat
    m.box((1.5, 4.0, 2.0), (14.5, 4.8, 14.5), "steel_dark")                         # shelf
    m.box((2.5, 4.8, 8.0), (6.5, 7.6, 12.5), "alu_od")                               # ammo cans
    m.box((7.5, 4.8, 8.5), (11.5, 7.2, 12.5), "alu_od")
    m.box((3.0, 7.6, 9.5), (6.0, 8.0, 11.0), "steel_dark")
    m.box((0, 14.0, 0), (16, 27.0, 1.2), "polymer_grey")                            # pegboard
    for gx in range(1, 16, 2):
        for gy in range(15, 27, 2):
            m.box((gx, gy, 1.2), (gx + 0.35, gy + 0.35, 1.22), "bore", faces=["south"])
    m.box((2.0, 18.0, 1.2), (2.6, 25.0, 1.8), "steel_light")                         # tools on the board
    m.box((1.4, 24.0, 1.2), (3.2, 25.4, 2.0), "steel_dark")
    m.box((4.5, 19.0, 1.2), (5.0, 25.5, 1.7), "walnut")
    m.box((4.0, 24.5, 1.2), (5.6, 26.0, 2.2), "steel")
    m.box((7.0, 17.0, 1.2), (13.5, 19.0, 1.8), "steel_dark")                         # a spare barrel
    m.box((8.0, 21.0, 1.2), (12.5, 24.5, 1.9), "alu")                               # a scope
    m.box((7.6, 22.2, 1.2), (8.0, 23.4, 1.9), "lens", faces=["west", "south"])
    m.box((11.5, 14.3, 3.0), (15.5, 16.4, 6.0), "steel_dark")                        # vise
    m.box((12.2, 16.4, 3.4), (14.8, 17.6, 4.3), "steel")
    m.box((12.2, 16.4, 4.7), (14.8, 17.6, 5.6), "steel")
    m.cyl("z", 6.0, 8.5, 13.5, 15.4, 0.3, "steel_light", sides=8)
    # A rifle on the mat
    m.box((2.0, 14.3, 9.0), (11.5, 15.3, 10.2), "alu")
    m.box((-0.5, 14.3, 9.1), (2.0, 15.6, 10.1), "polymer")
    m.cyl("x", 11.5, 17.0, 14.8, 9.6, 0.3, "steel_dark", sides=8)
    m.box((5.5, 14.3, 10.2), (7.0, 14.6, 12.6), "alu")                               # mag lying flat
    m.box((3.0, 15.3, 9.3), (9.0, 15.6, 9.9), "rail")
    m.check()
    return m


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent=1)
        f.write("\n")


def main(only):
    os.makedirs(TEXDIR, exist_ok=True)
    for name, fn in tex.TEXTURES.items():
        T.png(os.path.join(TEXDIR, name + ".png"), fn())
    geo = {}
    geo_path = os.path.join(HERE, "geometry.json")
    if os.path.exists(geo_path):
        geo = json.load(open(geo_path))
    meshdir = os.path.join(ASSETS, "gunmesh")
    os.makedirs(os.path.join(meshdir, "attach"), exist_ok=True)
    mtex = os.path.join(ASSETS, "textures", "gunmesh")
    os.makedirs(mtex, exist_ok=True)
    for name, fn in mesh_tex.TEXTURES.items():
        if not only or not os.path.exists(os.path.join(mtex, name + ".png")):
            T.png(os.path.join(mtex, name + ".png"), fn())
    for gid, (fn, scale_fp, relief) in dict(designs.ALL, **designs2.ALL).items():
        if only and gid not in only:
            continue
        m = MeshModel(gid, seed=sum(map(ord, gid)) * 131 & 0xFFFF)
        fn(m)
        m.check()
        assert m.sight, gid + ": no sight line"
        t = NS + ":item/gun2/"
        # The gun's own parts go in gunbase/; the item model draws them (and its attachments) through the gun renderer
        base = {"textures": dict({k: t + k for k in sorted(m.used)}, particle=t + "steel"), "elements": m.els}
        write_json(os.path.join(ASSETS, "models", "item", "gunbase", gid + ".json"), base)
        model = {"parent": "builtin/entity", "textures": {"particle": t + "steel"}, "gui_light": "front", "display": display(m, scale_fp)}
        write_json(os.path.join(ASSETS, "models", "item", gid + ".json"), model)
        geo[gid] = list(geometry(m, scale_fp, relief)) + [scale_fp] + mounts(m, gid)
        size = m.mesh.write(os.path.join(meshdir, gid + ".fam"))
        print("%-14s %4d parts %6d quads %4d KB  sight %s  ads %s" % (gid, len(m.els), len(m.mesh.quads), size // 1024, tuple(round(v, 2) for v in m.sight), geo[gid][:3]))
    write_json(geo_path, geo)
    # The Weapons Workbench block, the attachment item
    bm = bench_model()
    write_json(os.path.join(ASSETS, "models", "block", "weapons_workbench.json"),
               {"parent": "block/block", "textures": dict({k: t + k for k in sorted(bm.used)}, particle=t + "steel"), "elements": bm.els,
                "display": {"gui": {"rotation": [30, 45, 0], "translation": [0, -1.5, 0], "scale": [0.42] * 3},
                            "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.45] * 3},
                            "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25] * 3},
                            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.3] * 3},
                            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.35] * 3}}})
    write_json(os.path.join(ASSETS, "models", "item", "weapons_workbench.json"), {"parent": NS + ":block/weapons_workbench"})
    write_json(os.path.join(ASSETS, "blockstates", "weapons_workbench.json"), {"variants": {
        "facing=north": {"model": NS + ":block/weapons_workbench", "y": 180}, "facing=south": {"model": NS + ":block/weapons_workbench"},
        "facing=west": {"model": NS + ":block/weapons_workbench", "y": 90}, "facing=east": {"model": NS + ":block/weapons_workbench", "y": 270}}})
    data = os.path.join(ROOT, "mod", "src", "main", "resources", "data")
    write_json(os.path.join(data, NS, "loot_tables", "blocks", "weapons_workbench.json"), {"type": "minecraft:block", "pools": [{
        "rolls": 1, "entries": [{"type": "minecraft:item", "name": NS + ":weapons_workbench"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    for tag in ("mineable/pickaxe", "needs_iron_tool"):
        tp = os.path.join(data, "minecraft", "tags", "blocks", tag + ".json")
        tj = json.load(open(tp)) if os.path.exists(tp) else {"replace": False, "values": []}
        if NS + ":weapons_workbench" not in tj["values"]:
            tj["values"].append(NS + ":weapons_workbench")
        write_json(tp, tj)
    att_px = [[(0, 0, 0, 0)] * 16 for _ in range(16)]
    for y in range(5, 11):
        for x in range(2, 14):
            v = 230 if y == 5 else (150 if y == 10 else 200)
            att_px[y][x] = (v, v, v, 255)
    for y in range(3, 5):
        for x in range(9, 13):
            att_px[y][x] = (190, 190, 190, 255)
    for x in range(3, 13, 2):
        att_px[7][x] = (110, 110, 110, 255)
    T.png(os.path.join(ASSETS, "textures", "item", "attachment.png"), [[att_px[y // 2][x // 2] for x in range(32)] for y in range(32)])
    write_json(os.path.join(ASSETS, "models", "item", "attachment.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": NS + ":item/attachment"}})
    # Attachment shapes
    for name in attach.SHAPES:
        am = attach.build(name)
        if am.mesh.quads:
            am.mesh.write(os.path.join(meshdir, "attach", name + ".fam"))
        write_json(os.path.join(ASSETS, "models", "item", "attach", name + ".json"),
                   {"textures": dict({k: t + k for k in sorted(am.used)}, particle=t + "tint"), "elements": am.els})
    lines = ["package dev.factoryascension.gun;", "",
             "/** Generated by tools/guns/build.py from the models: where each gun is held when aimed (adsY, adsZ), its",
             "    first-person size, and where each attachment slot sits on it (x, y, z per slot, model units). */",
             "public final class GunGeometry {", "    private GunGeometry() {", "    }", "",
             "    public static final String[] SHAPES = {" + ", ".join('"%s"' % n for n in attach.SHAPES) + "};", "",
             "    public static void apply() {"]
    for gid, g in sorted(geo.items()):
        lines.append('        set("%s", %sf, %sf, %sf, new float[] {%s});' % (gid, g[0], g[1], g[2], ", ".join("%sf" % v for v in g[3:])))
    lines += ["    }", "", "    private static void set(String id, float y, float z, float scale, float[] mounts) {",
              "        GunSpec g = Guns.BY_ID.get(id);", "        if (g == null) return;", "        g.ads(y, z);", "        g.fpScale = scale;",
              "        g.mounts = mounts;", "    }", "}", ""]
    with open(JAVA, "w") as f:
        f.write("\n".join(lines))
    # Names, from the gun list
    import re
    src = open(os.path.join(ROOT, "mod", "src", "main", "java", "dev", "factoryascension", "gun", "Guns.java")).read()
    lang_path = os.path.join(ASSETS, "lang", "en_us.json")
    lang = json.load(open(lang_path))
    for gid, name in re.findall(r'new GunSpec\("(\w+)", "([^"]+)"', src):
        lang["item.%s.%s" % (NS, gid)] = name
    lang["item.%s.cartridge" % NS] = "Round"
    lang["item.%s.attachment" % NS] = "Attachment"
    lang["block.%s.weapons_workbench" % NS] = "Weapons Workbench"
    lang["enchantment.%s.jury_rigged" % NS] = "Jury-Rigged"
    lang["enchantment.%s.jury_rigged.desc" % NS] = "The gun takes any attachment, anywhere, up to four a slot."
    write_json(lang_path, lang)


if __name__ == "__main__":
    main(set(sys.argv[1:]))
