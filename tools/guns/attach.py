"""Attachment shapes (gun/Attachments.java names them): one small model per shape, built around its mount point at
(8, 8, 8): muzzle parts reach forward (+x) from it, optics sit on top of it, underbarrel and magazine parts hang below,
tactical units stick out to the right (+z), stocks reach back (-x). Faces on the "tint" texture take the attachment's
finish colour in game (tintindex 0); lenses, bores and rubber keep their own colours."""
from mesh import MeshModel as Model
import parts as P

C = 8.0


def tinted(m):
    """Mark every face on the tint texture as tinted."""
    for e in m.els:
        for f in e["faces"].values():
            if f["texture"] == "#tint":
                f["tintindex"] = 0


def muzzle_can(m, length, r, holes=False):
    m.cyl("x", C, C + length, C, C, r, "tint", sides=16)
    m.box((C + length, C - r * 0.4, C - r * 0.4), (C + length + 0.02, C + r * 0.4, C + r * 0.4), "bore", faces=["east"], full=True)
    for k in range(int(length / 1.2)):
        x = C + 0.6 + k * 1.2
        m.box((x, C - r - 0.04, C - r * 0.7), (x + 0.15, C + r + 0.04, C + r * 0.7), "steel_dark", faces=["up", "down", "north", "south"])
    if holes:
        for k in range(3):
            x = C + 0.5 + k * (length - 1) / 3
            m.box((x, C - 0.3, C + r), (x + 0.4, C + 0.3, C + r + 0.03), "bore", faces=["south"])
            m.box((x, C - 0.3, C - r - 0.03), (x + 0.4, C + 0.3, C - r), "bore", faces=["north"])
            m.box((x, C + r, C - 0.3), (x + 0.4, C + r + 0.03, C + 0.3), "bore", faces=["up"])


def scope(m, length, r, bell):
    base = C
    m.box((C - 1.5, base, C - 0.7), (C - 0.5, base + 0.8, C + 0.7), "steel_dark")
    m.box((C + length - 3.5, base, C - 0.7), (C + length - 2.5, base + 0.8, C + 0.7), "steel_dark")
    m.scope(C - 3, C - 3 + length, base + 0.8 + r * bell, C, r, tex="tint", bell=bell, mounts=(C - 1, C + length - 3), mount_tex="steel_dark", base_y=base)


SHAPES = {
    "comp": lambda m: (muzzle_can(m, 2.0, 0.62, holes=True)),
    "brake": lambda m: (m.box((C, C - 0.7, C - 0.8), (C + 2.4, C + 0.7, C + 0.8), "tint"),
                        [m.box((C + 0.3 + k * 0.7, C - 0.6, C + 0.8), (C + 0.6 + k * 0.7, C + 0.6, C + 0.83), "bore", faces=["south"]) for k in range(3)],
                        [m.box((C + 0.3 + k * 0.7, C - 0.6, C - 0.83), (C + 0.6 + k * 0.7, C + 0.6, C - 0.8), "bore", faces=["north"]) for k in range(3)],
                        m.box((C + 2.4, C - 0.3, C - 0.3), (C + 2.42, C + 0.3, C + 0.3), "bore", faces=["east"], full=True)),
    "flash": lambda m: (m.cyl("x", C, C + 1.8, C, C, 0.55, "tint", sides=16),
                        [m.box((C + 0.3, C + 0.5, C - 0.12), (C + 1.6, C + 0.58, C + 0.12), "bore", faces=["up"]) for _ in range(1)],
                        m.box((C + 1.8, C - 0.3, C - 0.3), (C + 1.82, C + 0.3, C + 0.3), "bore", faces=["east"], full=True)),
    "supp_short": lambda m: muzzle_can(m, 4.0, 0.8),
    "supp_long": lambda m: muzzle_can(m, 6.5, 0.85),
    "supp_fat": lambda m: muzzle_can(m, 6.0, 1.25),
    "choke": lambda m: (m.cyl("x", C, C + 1.2, C, C, 0.75, "tint", sides=16), m.box((C + 1.2, C - 0.5, C - 0.5), (C + 1.22, C + 0.5, C + 0.5), "bore", faces=["east"], full=True)),
    "bayonet": lambda m: (m.box((C - 1.0, C - 1.3, C - 0.25), (C + 0.6, C - 0.5, C + 0.25), "tint"),
                          m.box((C + 0.6, C - 1.2, C - 0.08), (C + 6.0, C - 0.6, C + 0.08), "steel_light"),
                          m.box((C + 6.0, C - 1.1, C - 0.06), (C + 6.8, C - 0.7, C + 0.06), "steel_light", rot=("z", -22.5, (C + 6.0, C - 0.9, C)))),
    "cap": lambda m: m.cyl("x", C, C + 0.6, C, C, 0.5, "tint", sides=16),
    "barrel_short": lambda m: (m.cyl("x", C - 2, C, C, C, 0.62, "tint", sides=16)),
    "barrel_long": lambda m: (m.cyl("x", C, C + 4.0, C, C, 0.5, "tint", sides=16), m.box((C + 4, C - 0.3, C - 0.3), (C + 4.02, C + 0.3, C + 0.3), "bore", faces=["east"], full=True)),
    "barrel_heavy": lambda m: (m.cyl("x", C - 3, C + 2.0, C, C, 0.8, "tint", sides=16), m.box((C + 2, C - 0.4, C - 0.4), (C + 2.02, C + 0.4, C + 0.4), "bore", faces=["east"], full=True)),
    "barrel_fluted": lambda m: (m.cyl("x", C - 3, C + 1.5, C, C, 0.65, "tint", sides=16),
                                [m.box((C - 2.6, C + 0.62, C - 0.1 + k * 0.4 - 0.4), (C + 1.2, C + 0.68, C + 0.1 + k * 0.4 - 0.4), "bore", faces=["up"]) for k in range(3)]),
    "hg_rail": lambda m: (m.box((C - 4, C - 1.2, C - 1.4), (C + 4, C + 1.2, C + 1.4), "tint"), m.rail(C - 3.8, C + 3.8, C + 1.2, C, w=1.4, tex="rail"),
                          m.side_rail(C - 3.6, C + 3.6, C, C + 1.4, "south", h=1.0)),
    "hg_slim": lambda m: (m.box((C - 4, C - 1.1, C - 1.2), (C + 4, C + 1.1, C + 1.2), "tint"),
                          [m.box((C - 3.4 + k * 1.5, C - 0.3, C + 1.2), (C - 2.6 + k * 1.5, C + 0.3, C + 1.23), "bore", faces=["south"]) for k in range(5)]),
    "hg_wood": lambda m: (m.box((C - 4, C - 1.2, C - 1.3), (C + 4, C + 1.0, C + 1.3), "walnut"), m.box((C - 4, C + 1.0, C - 1.0), (C + 4, C + 1.3, C + 1.0), "walnut")),
    "fg_vertical": lambda m: (m.box((C - 0.6, C - 3.6, C - 0.55), (C + 0.6, C, C + 0.55), "tint"), m.box((C - 0.8, C - 0.4, C - 0.7), (C + 0.8, C, C + 0.7), "steel_dark"),
                              m.checkering(C - 0.5, C + 0.5, C - 3.2, C - 0.6, C - 0.55, C + 0.55)),
    "fg_angled": lambda m: (m.box((C - 2.0, C - 1.5, C - 0.55), (C + 1.2, C, C + 0.55), "tint"), m.box((C + 0.2, C - 2.6, C - 0.5), (C + 1.2, C - 1.0, C + 0.5), "tint", rot=("z", 45, (C + 0.7, C - 1.5, C)))),
    "fg_stubby": lambda m: (m.box((C - 0.6, C - 1.8, C - 0.55), (C + 0.6, C, C + 0.55), "tint")),
    "fg_stop": lambda m: (m.box((C - 0.8, C - 0.9, C - 0.5), (C + 0.6, C, C + 0.5), "tint")),
    "opt_dot": lambda m: (m.box((C - 1.2, C, C - 0.7), (C + 1.2, C + 0.5, C + 0.7), "steel_dark"),
                          m.box((C - 0.6, C + 0.5, C - 0.75), (C + 0.6, C + 2.0, C + 0.75), "tint"),
                          m.box((C - 0.62, C + 0.75, C - 0.5), (C - 0.6, C + 1.75, C + 0.5), "red_dot", faces=["west"], full=True),
                          m.box((C + 0.6, C + 0.75, C - 0.5), (C + 0.62, C + 1.75, C + 0.5), "lens", faces=["east"], full=True)),
    "opt_holo": lambda m: (m.box((C - 2.0, C, C - 0.85), (C + 2.0, C + 0.9, C + 0.85), "tint"),
                           m.box((C + 0.4, C + 0.9, C - 0.85), (C + 2.0, C + 2.4, C - 0.65), "tint"),
                           m.box((C + 0.4, C + 0.9, C + 0.65), (C + 2.0, C + 2.4, C + 0.85), "tint"),
                           m.box((C + 0.4, C + 2.2, C - 0.85), (C + 2.0, C + 2.45, C + 0.85), "tint"),
                           m.box((C + 1.0, C + 0.95, C - 0.65), (C + 1.02, C + 2.2, C + 0.65), "lens", faces=["east", "west"], full=True)),
    "opt_prism": lambda m: (m.box((C - 1.6, C, C - 0.75), (C + 1.6, C + 0.6, C + 0.75), "steel_dark"),
                            m.box((C - 1.6, C + 0.6, C - 0.85), (C + 1.6, C + 2.4, C + 0.85), "tint"),
                            m.lens(C + 1.6, C + 1.5, C, 0.75), m.lens(C - 1.6, C + 1.5, C, 0.6, facing="west")),
    "opt_acog": lambda m: (m.box((C - 2.2, C, C - 0.8), (C + 2.2, C + 0.6, C + 0.8), "tint"),
                           m.box((C - 2.2, C + 0.6, C - 0.85), (C + 1.4, C + 2.2, C + 0.85), "tint"),
                           m.cyl("x", C + 1.4, C + 2.6, C + 1.55, C, 0.85, "tint", sides=16),
                           m.lens(C + 2.6, C + 1.55, C, 0.8), m.lens(C - 2.2, C + 1.55, C, 0.55, facing="west"),
                           m.box((C - 0.6, C + 2.2, C - 0.15), (C + 0.6, C + 2.6, C + 0.15), "lens")),
    "opt_scope": lambda m: scope(m, 9.0, 0.7, 1.3),
    "opt_scope_big": lambda m: scope(m, 11.5, 0.85, 1.5),
    "opt_irons": lambda m: (m.box((C - 0.5, C, C + 0.6), (C + 0.5, C + 0.6, C + 1.6), "tint", rot=("x", -45, (C, C, C + 0.6))),
                            m.box((C + 3.5, C, C + 0.6), (C + 4.3, C + 0.6, C + 1.4), "tint", rot=("x", -45, (C + 3.9, C, C + 0.6)))),
    "stock_pad": lambda m: (m.box((C - 0.8, C - 2.6, C - 1.1), (C, C + 1.6, C + 1.1), "tint"), m.box((C - 1.1, C - 2.8, C - 1.15), (C - 0.8, C + 1.8, C + 1.15), "rubber")),
    "stock_cheek": lambda m: (m.box((C + 1.0, C + 1.4, C - 0.8), (C + 6.0, C + 2.4, C + 0.8), "tint"),
                              m.box((C - 0.8, C - 2.6, C - 1.1), (C, C + 1.6, C + 1.1), "tint"), m.box((C - 1.1, C - 2.8, C - 1.15), (C - 0.8, C + 1.8, C + 1.15), "rubber")),
    "mag_ext": lambda m: (m.box((C - 1.2, C - 3.0, C - 0.65), (C + 1.2, C, C + 0.65), "tint"), m.box((C - 1.35, C - 3.4, C - 0.75), (C + 1.35, C - 3.0, C + 0.75), "steel_dark")),
    "mag_drum": lambda m: (m.box((C - 0.8, C - 1.2, C - 0.6), (C + 0.8, C, C + 0.6), "tint"),
                           m.cyl("z", C - 1.6, C + 1.6, C, C - 3.4, 2.3, "tint", sides=16, end_tex="steel_dark")),
    "mag_coupled": lambda m: (m.box((C - 1.2, C - 3.0, C - 0.65), (C + 1.2, C, C + 0.65), "tint"), m.box((C - 1.2, C - 3.6, C + 0.7), (C + 1.2, C - 0.4, C + 2.0), "tint"),
                              m.box((C - 1.3, C - 2.0, C - 0.7), (C + 1.3, C - 1.4, C + 2.05), "steel_dark")),
    "mag_short": lambda m: m.box((C - 1.25, C - 0.8, C - 0.7), (C + 1.25, C, C + 0.7), "tint"),
    "under_bipod": lambda m: (m.box((C - 0.6, C - 0.7, C - 0.6), (C + 0.6, C, C + 0.6), "tint"),
                              m.box((C - 0.3, C - 5.6, C - 0.95), (C + 0.2, C - 0.7, C - 0.55), "tint", rot=("x", -22.5, (C, C - 0.7, C))),
                              m.box((C - 0.3, C - 5.6, C + 0.55), (C + 0.2, C - 0.7, C + 0.95), "tint", rot=("x", 22.5, (C, C - 0.7, C)))),
    "under_launcher": lambda m: (m.box((C - 3.0, C - 1.0, C - 0.9), (C + 2.0, C, C + 0.9), "tint"),
                                 m.tube(C - 1.0, C + 4.5, C - 1.9, C, 1.0, "tint", sides=16, wall=0.15),
                                 m.box((C - 3.6, C - 4.0, C - 0.5), (C - 2.4, C - 1.0, C + 0.5), "tint", rot=("z", -22.5, (C - 3.0, C - 1.0, C)))),
    "under_shotgun": lambda m: (m.box((C - 3.0, C - 0.9, C - 0.8), (C + 1.0, C, C + 0.8), "tint"),
                                m.tube(C - 2.0, C + 5.0, C - 1.6, C, 0.6, "steel_dark", sides=16), m.cyl("x", C - 1.0, C + 1.6, C - 1.6, C, 0.8, "tint", sides=16)),
    "under_cover": lambda m: m.box((C - 3.0, C - 0.4, C - 0.8), (C + 3.0, C, C + 0.8), "tint"),
    "tac_laser": lambda m: (m.box((C - 1.2, C - 0.55, C), (C + 1.2, C + 0.55, C + 1.0), "tint"),
                            m.box((C + 1.2, C - 0.2, C + 0.3), (C + 1.22, C + 0.2, C + 0.7), "red_dot", faces=["east"], full=True)),
    "tac_light": lambda m: (m.cyl("x", C - 1.2, C + 1.4, C, C + 0.8, 0.55, "tint", sides=16),
                            m.box((C + 1.4, C - 0.45, C + 0.35), (C + 1.42, C + 0.45, C + 1.25), "paint_white", faces=["east"], full=True)),
    "tac_combo": lambda m: (m.box((C - 1.4, C - 0.7, C), (C + 1.4, C + 0.7, C + 1.4), "tint"),
                            m.box((C + 1.4, C - 0.5, C + 0.2), (C + 1.42, C, C + 0.7), "red_dot", faces=["east"], full=True),
                            m.box((C + 1.4, C + 0.05, C + 0.3), (C + 1.42, C + 0.55, C + 1.2), "paint_white", faces=["east"], full=True)),
    "grip_wrap": lambda m: (m.box((C - 1.1, C - 3.0, C - 0.85), (C + 1.1, C, C + 0.85), "tint", rot=("z", -22.5, (C, C, C)))),
    "charm": lambda m: (m.box((C - 0.08, C - 1.2, C - 0.08), (C + 0.08, C, C + 0.08), "steel_light"),
                        m.box((C - 0.45, C - 2.0, C - 0.45), (C + 0.45, C - 1.1, C + 0.45), "tint")),
    # ---- Cursed ----
    "cursed_box": lambda m: (m.box((C - 2.5, C - 5.0, C - 1.6), (C + 2.5, C, C + 1.6), "alu_od", r=0.3),
                             m.box((C - 2.6, C - 0.6, C - 1.7), (C + 2.6, C - 0.2, C + 1.7), "steel_dark", r=0.1),
                             m.box((C - 0.6, C - 2.5, C + 1.6), (C + 0.6, C - 1.5, C + 1.9), "steel", r=0.1),
                             [P.cartridge(m, C - 2.0 + k * 0.5, C - 0.1, C + 1.4, length=1.6, r=0.19) for k in range(8)]),
    "cursed_drum": lambda m: (m.lathe("y", [(C - 9, 0), (C - 9, 3.4), (C - 8.6, 3.6), (C - 0.4, 3.6), (C, 3.4), (C, 0)], C, C, "tint"),
                              [m.lathe("y", [(C - 8 + k * 2.2, 3.6), (C - 8 + k * 2.2, 3.75), (C - 7.6 + k * 2.2, 3.75), (C - 7.6 + k * 2.2, 3.6)], C, C, "steel_dark", icon=False) for k in range(4)]),
    "cursed_scope": lambda m: (m.box((C - 3, C, C - 0.8), (C - 2, C + 1.2, C + 0.8), "steel_dark"), m.box((C + 8, C, C - 0.8), (C + 9, C + 1.2, C + 0.8), "steel_dark"),
                               m.lathe("x", [(C - 6, 1.2), (C - 6, 1.6), (C - 3, 1.6), (C - 2, 1.2), (C + 10, 1.2), (C + 14, 2.6), (C + 18, 2.8), (C + 18, 2.4)], C + 3.0, C, "tint"),
                               m.lathe("x", [(C + 17.6, 2.4), (C + 17.6, 0)], C + 3.0, C, "lens", icon=False),
                               m.cyl("y", C + 4.2, C + 5.6, C + 3, C, 0.9, "knurl")),
    "cursed_barrel": lambda m: (m.lathe("x", [(C, 0), (C, 0.7), (C + 23.5, 0.62), (C + 23.5, 0)], C, C, "tint"),
                                m.lathe("x", [(C + 23.5, 0.3), (C + 23.5, 0.6), (C + 52, 0.55), (C + 52, 0.3), (C + 51, 0.28), (C + 51, 0)], C, C, "tint",
                                        mats=["tint", "tint", "tint", "bore", "bore"], icon=False),
                                [m.lathe("x", [(C + 6 + k * 9, 0.62), (C + 6 + k * 9, 0.9), (C + 7 + k * 9, 0.9), (C + 7 + k * 9, 0.62)], C, C, "steel_dark", icon=False) for k in range(5)]),
    "cursed_barrel_wide": lambda m: (m.lathe("x", [(C, 0), (C, 0.7), (C + 3, 1.2), (C + 6, 2.2), (C + 6, 1.9), (C + 3.5, 0)], C, C, "tint"),),
    "cursed_light": lambda m: (m.box((C - 1.2, C - 0.6, C), (C + 1.2, C + 0.6, C + 1.0), "steel_dark"),
                               m.lathe("x", [(C - 3, 0), (C - 3, 1.4), (C + 2, 1.6), (C + 3.5, 2.6), (C + 3.5, 0)], C, C + 3.2, "tint",
                                       mats=["tint", "tint", "tint", "paint_white"]),
                               m.lathe("x", [(C + 3.45, 2.4), (C + 3.45, 0)], C, C + 3.2, "glow_white", icon=False)),
    "cursed_coil": lambda m: (m.lathe("x", [(C, 0), (C, 0.6), (C + 14, 0.6), (C + 14, 0)], C, C, "steel_dark"),
                              [m.lathe("x", [(C + 0.5 + k * 1.1, 0.6), (C + 0.5 + k * 1.1, 1.3), (C + 1.1 + k * 1.1, 1.3), (C + 1.1 + k * 1.1, 0.6)], C, C, "copper", icon=False)
                               for k in range(12)],
                              [m.cyl("x", C + 0.2 + k * 1.1, C + 0.5 + k * 1.1, C, C, 1.35, "glow_green", mesh_only=True, ends=False) for k in range(0, 12, 3)]),
    "cursed_jet": lambda m: (m.box((C - 3, C - 2, C - 2.4), (C, C + 2, C + 2.4), "tint", r=0.4),
                             [m.lathe("x", [(C - 6, 0.5), (C - 6, 1.1), (C - 3, 1.0), (C - 3, 0)], C - 0.5, C + s2 * 1.4, "steel_dark") for s2 in (-1, 1)],
                             [m.lathe("x", [(C - 6.1, 0.9), (C - 6.1, 0)], C - 0.5, C + s2 * 1.4, "glow_amber", icon=False) for s2 in (-1, 1)]),
    "cursed_saw": lambda m: (m.box((C - 1, C - 1.6, C - 0.6), (C + 2, C, C + 0.6), "paint_red", r=0.3),
                             m.plate([(C + 1.5, C - 1.6), (C + 10, C - 1.4), (C + 10.6, C - 0.9), (C + 10, C - 0.4), (C + 1.5, C - 0.2)], C - 0.12, C + 0.12, "steel_light", bevel=0.04),
                             [m.box((C + 2 + k * 0.6, C - 1.75, C - 0.15), (C + 2.3 + k * 0.6, C - 1.55, C + 0.15), "steel_dark", r=0.02) for k in range(14)]),
    "cursed_stack": lambda m: [muzzle_can(m, 0, 0) if False else m.lathe("x", [(C + k * 4.0, 0), (C + k * 4.0, 0.85), (C + k * 4.0 + 3.9, 0.85), (C + k * 4.0 + 3.9, 0)],
                                                                              C, C, "tint" if k % 2 else "steel_dark", icon=k < 5) for k in range(6)],
    "cursed_duck": lambda m: (m.box((C - 0.08, C - 1.2, C - 0.08), (C + 0.08, C, C + 0.08), "steel_light"),
                              m.lathe("y", [(C - 2.6, 0), (C - 2.6, 0.55), (C - 2.0, 0.6), (C - 1.6, 0)], C, C, "polymer_sand"),
                              m.lathe("y", [(C - 1.7, 0), (C - 1.7, 0.35), (C - 1.2, 0)], C + 0.3, C, "polymer_sand"),
                              m.box((C + 0.55, C - 1.55, C - 0.12), (C + 0.85, C - 1.4, C + 0.12), "paint_red", r=0.04)),
    "cursed_toaster": lambda m: (m.box((C - 2, C - 2.6, C - 1.3), (C + 2, C - 0.2, C + 1.3), "chrome", r=0.4),
                                 m.box((C - 1.4, C - 0.3, C - 0.8), (C + 1.4, C - 0.15, C - 0.3), "bore", faces=["up"]),
                                 m.box((C - 1.4, C - 0.3, C + 0.3), (C + 1.4, C - 0.15, C + 0.8), "bore", faces=["up"]),
                                 m.box((C - 1.2, C - 0.2, C - 0.7), (C + 1.2, C + 0.6, C - 0.4), "beech", r=0.1),
                                 m.box((C + 2, C - 1.6, C - 0.2), (C + 2.4, C - 1.2, C + 0.2), "steel_dark", r=0.05)),
    "cursed_bipod": lambda m: (m.box((C - 0.7, C - 0.8, C - 0.7), (C + 0.7, C, C + 0.7), "tint"),
                               [m.pipe([(C, C - 0.6, C + s2 * 0.4), (C + 2, C - 30, C + s2 * 9)], 0.22, "tint", icon=False) for s2 in (-1, 1)],
                               m.box((C - 0.3, C - 6, C - 0.4), (C + 0.3, C - 0.8, C + 0.4), "tint")),
    "none": lambda m: m.box((C - 0.01, C - 0.01, C - 0.01), (C + 0.01, C + 0.01, C + 0.01), "tint", faces=["up"]),
}


def build(name):
    m = Model("attach_" + name, seed=sum(map(ord, name)) * 131 & 0xFFFF)
    SHAPES[name](m)
    tinted(m)
    m.check()
    return m
