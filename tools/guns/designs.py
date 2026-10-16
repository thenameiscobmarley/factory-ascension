"""The guns, part by part. Each builder adds its parts to a Model and sets m.sight (the sight line's front point)
and m.stock_rear (the back of the stock at cheek height). ALL maps gun id -> (builder, first-person scale, eye relief).
Muzzle toward +x, z = 8 is the middle of the gun, the right side (ejection port side) faces +z ("south")."""

Z = 8.0


def zs(w):
    """z from / to for something w wide, centred."""
    return Z - w / 2, Z + w / 2


# ---------------------------------------------------------------------------------------------------------------
# AR platform: M4A1, SR-25
# ---------------------------------------------------------------------------------------------------------------
def ar_platform(m, barrel_end=29.0, handguard_end=22.0, rail_hg=True, finish="alu", furniture="polymer", mag_depth=6.4,
                mag_curve=1, stock="m4", optic=None, big=1.0, bipod=False):
    z0, z1 = zs(1.8 * big)
    uz0, uz1 = zs(2.0 * big)
    # ---- Lower receiver ----
    m.box((1.0, 5.4, z0), (11.2, 8.2, z1), finish)
    m.box((7.4, 3.9, z0 - 0.15), (10.5, 5.4, z1 + 0.15), finish)               # magazine well, flared
    m.box((7.2, 3.6, z0 - 0.25), (10.7, 4.0, z1 + 0.25), finish)
    m.box((0.2, 6.0, z0 + 0.1), (1.0, 8.2, z1 - 0.1), finish)                  # receiver extension housing
    m.box((10.4, 7.0, z0 - 0.05), (11.6, 8.2, z1 + 0.05), finish)              # front pivot lug
    m.screws_both(10.9, 7.6, z0 - 0.05, z1 + 0.05, r=0.26, tex="steel")
    m.screws_both(1.6, 7.6, z0, z1, r=0.26, tex="steel")                       # takedown pin
    # Mag release (right) and bolt catch (left), selector (left)
    m.box((10.6, 5.9, z1), (11.1, 6.4, z1 + 0.35), "steel_dark")
    m.box((6.6, 6.6, z0 - 0.35), (8.0, 7.3, z0), "steel_dark")
    m.box((2.7, 6.3, z0 - 0.25), (3.3, 6.9, z0), "steel_dark")
    m.box((2.6, 6.45, z0 - 0.45), (4.1, 6.75, z0 - 0.25), "steel_dark")        # selector lever
    m.box((2.4, 6.2, z1), (3.4, 7.0, z1 + 0.06), "steel_light", faces=["south"])  # selector markings plate
    # Trigger guard and trigger, pistol grip
    m.trigger_guard(3.4, 7.2, 5.4, 1.7, finish)
    m.grip(3.2, 5.6, 2.1, 4.6, Z - 0.75, Z + 0.75, furniture, angle=-22.5, check_tex="grip_check" if furniture == "polymer" else "grip_check_tan")
    m.box((3.0, 5.0, Z - 0.8), (5.4, 5.6, Z + 0.8), furniture)                 # grip's top where it meets the lower
    # ---- Upper receiver ----
    m.box((0.0, 8.2, uz0), (12.0, 10.4, uz1), finish)
    m.box((0.0, 8.2, uz0 - 0.1), (12.0, 8.45, uz1 + 0.1), finish)               # seam between upper and lower
    m.rail(0.3, 12.0, 10.4, Z, w=1.6 * big)
    # Ejection port with an open dust cover and the bolt carrier showing, the brass deflector
    m.box((4.0, 8.65, uz1), (7.6, 9.85, uz1 + 0.05), "bore", faces=["south"])
    m.box((4.6, 8.9, uz1 + 0.05), (7.0, 9.6, uz1 + 0.08), "steel_light", faces=["south"])
    m.box((4.0, 7.9, uz1 + 0.05), (7.6, 8.45, uz1 + 0.5), finish)               # dust cover, hanging open
    m.box((3.0, 8.7, uz1), (3.9, 10.0, uz1 + 0.6), finish)
    # Forward assist
    m.cyl("z", uz1 - 0.2, uz1 + 0.9, 1.5, 9.5, 0.42, finish, sides=8, end_tex="knurl")
    # Charging handle
    m.box((-0.6, 9.8, Z - 0.7), (0.2, 10.3, Z + 0.7), finish)
    m.box((-0.6, 9.8, Z - 1.2), (-0.1, 10.3, Z - 0.7), finish)
    # ---- Barrel, handguard, gas block, muzzle ----
    m.tube(12.0, barrel_end, 9.2, Z, 0.52 * big, "steel_dark", sides=16)
    hg_lo, hg_hi = 7.7, 10.6
    hz0, hz1 = zs(2.5 * big)
    if rail_hg:
        m.box((12.0, hg_lo + 0.3, hz0 + 0.3), (handguard_end, hg_hi - 0.3, hz1 - 0.3), finish)
        m.rail(12.1, handguard_end, hg_hi - 0.3, Z, w=1.6 * big)
        m.side_rail(12.4, handguard_end - 0.3, (hg_lo + hg_hi) / 2, hz1 - 0.3, "south")
        m.side_rail(12.4, handguard_end - 0.3, (hg_lo + hg_hi) / 2, hz0 + 0.3, "north")
        m.box((12.4, hg_lo, Z - 0.8), (handguard_end - 0.3, hg_lo + 0.3, Z + 0.8), "rail")    # bottom rail
        for k in range(int((handguard_end - 13) / 1.6)):
            x = 12.9 + k * 1.6
            m.box((x, hg_lo - 0.35, Z - 0.9), (x + 0.6, hg_lo, Z + 0.9), "rail")
        m.box((12.0, hg_lo, hz0), (12.6, hg_hi, hz1), finish)                  # delta ring
        for k in range(3):                                                     # vents
            x = 13.5 + k * 2.6
            m.box((x, 8.6, hz1 - 0.3), (x + 1.4, 9.6, hz1 - 0.24), "bore", faces=["south"])
            m.box((x, 8.6, hz0 + 0.24), (x + 1.4, 9.6, hz0 + 0.3), "bore", faces=["north"])
    else:
        m.cyl("x", 12.0, handguard_end, 9.15, Z, 1.35 * big, furniture, sides=16)
        for k in range(6):
            x = 12.8 + k * (handguard_end - 13.5) / 6
            m.box((x, 9.6, Z + 1.28 * big), (x + 0.5, 10.1, Z + 1.32 * big), "bore", faces=["south"])
    # Gas block with the A2 front sight tower (or a low gas block under an optic)
    gx = handguard_end + 0.7
    if optic is None:
        m.box((gx, 8.4, Z - 0.6), (gx + 1.1, 10.0, Z + 0.6), "steel_dark")
        m.box((gx + 0.1, 10.0, Z - 0.35), (gx + 1.0, 11.0, Z + 0.35), "steel_dark", rot=("z", 22.5, (gx + 0.55, 10.0, Z)))
        m.box((gx + 0.15, 11.0, Z - 0.75), (gx + 0.95, 12.1, Z - 0.55), "steel_dark")
        m.box((gx + 0.15, 11.0, Z + 0.55), (gx + 0.95, 12.1, Z + 0.75), "steel_dark")
        m.box((gx + 0.45, 11.0, Z - 0.12), (gx + 0.65, 11.85, Z + 0.12), "steel")
        m.box((gx - 0.2, 7.2, Z - 0.4), (gx + 0.8, 8.4, Z + 0.4), "steel_dark")      # bayonet lug
        # Flip-up rear sight on the receiver rail
        m.box((0.8, 10.85, Z - 0.9), (2.4, 11.4, Z + 0.9), "steel_dark")
        m.box((1.2, 11.4, Z - 0.9), (2.0, 12.4, Z - 0.5), "steel_dark")
        m.box((1.2, 11.4, Z + 0.5), (2.0, 12.4, Z + 0.9), "steel_dark")
        m.box((1.45, 11.4, Z - 0.5), (1.75, 11.75, Z + 0.5), "steel_dark")
        m.cyl("z", Z + 0.9, Z + 1.4, 1.6, 11.1, 0.35, "knurl", sides=8)
        m.sight = (gx + 0.55, 11.8)
    else:
        m.box((gx, 8.5, Z - 0.6), (gx + 1.0, 9.9, Z + 0.6), "steel_dark")
        optic(m)
    # Muzzle device: an A2 birdcage (slots cut on top and sides)
    mx = barrel_end
    m.cyl("x", mx, mx + 2.2, 9.2, Z, 0.72 * big, "steel_dark", sides=16, ends=False)
    m.box((mx + 2.19, 8.7, Z - 0.48), (mx + 2.21, 9.7, Z + 0.48), "bore", faces=["east"], full=True)
    for k in range(3):
        x = mx + 0.4 + k * 0.6
        m.box((x, 9.75, Z - 0.25), (x + 0.3, 9.95, Z + 0.25), "bore", faces=["up"])
        m.box((x, 8.9, Z + 0.62), (x + 0.3, 9.5, Z + 0.74), "bore", faces=["south"])
        m.box((x, 8.9, Z - 0.74), (x + 0.3, 9.5, Z - 0.62), "bore", faces=["north"])
    # ---- Buffer tube and stock ----
    m.cyl("x", -6.6, 0.2, 8.9, Z, 0.62 * big, "alu", sides=16)
    m.box((-0.6, 8.0, Z - 0.9), (0.2, 9.7, Z + 0.9), "steel_dark")             # castle nut and end plate
    if stock == "m4":
        m.box((-12.4, 6.6, Z - 0.95), (-6.2, 9.7, Z + 0.95), furniture)
        m.box((-12.4, 9.7, Z - 0.75), (-7.0, 10.35, Z + 0.75), furniture)       # cheek rest
        m.box((-12.4, 5.4, Z - 0.9), (-10.0, 6.6, Z + 0.9), furniture)          # toe
        m.box((-11.0, 5.9, Z - 0.6), (-7.2, 6.6, Z + 0.6), furniture)
        m.box((-12.9, 5.3, Z - 1.0), (-12.4, 10.4, Z + 1.0), "rubber")          # butt pad
        m.box((-9.4, 6.15, Z - 0.3), (-8.8, 6.6, Z + 0.3), "steel_dark")        # adjustment lever
        m.box((-11.4, 9.2, Z + 0.95), (-8.0, 9.5, Z + 1.05), "steel_dark")      # sling loop
        for k in range(4):
            m.box((-7.0 + k * 0.6, 8.25, Z + 0.6), (-6.75 + k * 0.6, 8.45, Z + 0.66), "bore", faces=["south"])   # position holes
        m.stock_rear = -12.9
    else:
        # A fixed A2-style rifle stock
        m.box((-14.2, 6.0, Z - 1.0), (-6.4, 9.9, Z + 1.0), furniture)
        m.box((-14.2, 4.8, Z - 1.0), (-11.4, 6.0, Z + 1.0), furniture, rot=("z", -22.5, (-11.4, 6.0, Z)))
        m.box((-14.7, 4.6, Z - 1.05), (-14.2, 10.0, Z + 1.05), "rubber")
        m.box((-13.7, 9.9, Z - 0.8), (-7.0, 10.4, Z + 0.8), furniture)
        m.stock_rear = -14.7
    # ---- Magazine ----
    m.mag(7.6, 3.9, 2.7, mag_depth, Z - 0.7, Z + 0.7, "alu" if finish == "alu" else finish, curve=mag_curve, base="polymer", ribs=3, rib_tex="steel_dark")
    if bipod:
        bx = handguard_end - 1.5
        m.box((bx, 7.0, Z - 0.6), (bx + 1.2, 7.7, Z + 0.6), "steel_dark")
        for s in (-1, 1):
            m.box((bx + 0.3, 2.0, Z + s * 0.9 - 0.25), (bx + 0.8, 7.0, Z + s * 0.9 + 0.25), "steel_dark", rot=("x", 22.5 * s, (bx + 0.55, 7.0, Z)))


# ---------------------------------------------------------------------------------------------------------------
# AK platform: AK-74M, AKM, Saiga-12
# ---------------------------------------------------------------------------------------------------------------
def ak_platform(m, wood="birch", stock="wood", mag_tex="birch", brake="ak74", barrel_end=28.5, mag_kind="curved", rail_cover=False):
    z0, z1 = zs(1.7)
    # Stamped receiver with its rivets and the magazine well dimple
    m.box((0.0, 6.6, z0), (13.0, 9.4, z1), "steel")
    m.box((0.3, 9.4, z0 + 0.05), (12.0, 9.9, z1 - 0.05), "steel")                      # dust cover sides
    m.cyl("x", 0.3, 12.0, 9.9, Z, 0.8, "steel", sides=16, ends=False)                    # rounded dust cover top
    m.box((0.0, 9.7, Z - 0.45), (0.6, 10.5, Z + 0.45), "steel")                          # rear of the cover (recoil spring button)
    for x in (2.0, 3.0, 10.4, 11.4):
        m.screws_both(x, 7.0, z0, z1, r=0.18, tex="steel_light")                         # rivets
    m.box((5.4, 7.6, z1), (8.6, 8.6, z1 + 0.04), "steel_dark", faces=["south"])          # stamped dimple
    m.box((5.4, 7.6, z0 - 0.04), (8.6, 8.6, z0), "steel_dark", faces=["north"])
    # Ejection port and the charging handle sticking out on the right
    m.box((6.8, 8.7, z1), (11.6, 9.6, z1 + 0.04), "bore", faces=["south"])
    m.box((10.4, 8.75, z1), (11.0, 9.25, z1 + 0.04), "steel_light", faces=["south"])
    m.box((10.3, 8.8, z1), (11.1, 9.3, z1 + 1.3), "steel_light")
    m.cyl("z", z1 + 1.0, z1 + 1.5, 10.7, 9.05, 0.38, "steel_light", sides=8)
    # The big selector lever on the right
    m.box((3.0, 8.45, z1), (10.2, 8.85, z1 + 0.18), "steel_dark")
    m.box((9.6, 7.8, z1), (10.4, 8.85, z1 + 0.25), "steel_dark")
    m.cyl("z", z1, z1 + 0.3, 3.2, 8.6, 0.42, "steel_dark", sides=8)
    # Front trunnion and the rear sight block with its leaf
    m.box((12.0, 7.2, z0 - 0.05), (14.4, 9.6, z1 + 0.05), "steel_dark")
    m.box((13.0, 9.6, Z - 0.75), (15.4, 10.4, Z + 0.75), "steel_dark")
    m.box((13.2, 10.4, Z - 0.6), (16.4, 10.75, Z + 0.6), "steel_dark", rot=("z", 0, (13.2, 10.4, Z)))   # the leaf
    m.box((14.8, 10.75, Z - 0.45), (15.2, 11.15, Z + 0.45), "steel_dark")             # slider
    m.box((13.2, 10.75, Z - 0.6), (13.6, 11.25, Z - 0.15), "steel_dark")              # notch ears
    m.box((13.2, 10.75, Z + 0.15), (13.6, 11.25, Z + 0.6), "steel_dark")
    # Trigger guard, grip, magazine catch
    m.trigger_guard(3.0, 6.6, 6.6, 1.8, "steel_dark")
    m.box((6.9, 5.6, Z - 0.35), (7.5, 6.6, Z + 0.35), "steel_dark")
    m.grip(1.4, 6.8, 2.0, 4.6, Z - 0.75, Z + 0.75, "polymer" if stock != "wood" else "polymer_plum" if wood == "polymer_plum" else "polymer", angle=-22.5,
           check_tex="grip_check")
    # Barrel, gas block, gas tube with the upper handguard, lower handguard
    m.tube(13.0, barrel_end, 9.1, Z, 0.55, "steel_dark", sides=16)
    m.box((22.0, 8.6, Z - 0.75), (23.6, 10.9, Z + 0.75), "steel_dark")               # gas block
    m.cyl("x", 15.4, 22.2, 10.35, Z, 0.62, "steel", sides=16)
    m.box((15.6, 9.85, Z - 1.0), (21.6, 11.0, Z + 1.0), wood)                          # upper handguard
    m.cyl("x", 15.6, 21.6, 10.6, Z, 0.85, wood, sides=8, ends=False)
    if rail_cover:
        m.rail(15.8, 21.4, 11.2, Z, w=1.5)
    m.box((14.4, 7.4, Z - 1.25), (21.8, 9.9, Z + 1.25), wood)                          # lower handguard
    for k in range(4):                                                                  # finger grooves
        x = 15.6 + k * 1.4
        m.box((x, 7.5, Z + 1.25), (x + 0.6, 9.3, Z + 1.3), "walnut_dark" if wood != "polymer" else "polymer_grey", faces=["south"])
        m.box((x, 7.5, Z - 1.3), (x + 0.6, 9.3, Z - 1.25), "walnut_dark" if wood != "polymer" else "polymer_grey", faces=["north"])
    m.box((14.4, 7.2, Z - 1.35), (15.0, 10.0, Z + 1.35), "steel_dark")                 # handguard retainer
    m.box((21.6, 7.2, Z - 1.3), (22.2, 9.9, Z + 1.3), "steel_dark")
    m.cyl("x", 14.0, barrel_end - 1.4, 8.15, Z, 0.16, "steel_light", sides=8)          # cleaning rod
    # Front sight block: the post between two ears, and the bayonet lug
    fx = barrel_end - 2.2
    m.box((fx, 8.3, Z - 0.65), (fx + 1.5, 9.9, Z + 0.65), "steel_dark")
    m.box((fx + 0.2, 9.9, Z - 0.75), (fx + 1.2, 11.4, Z - 0.5), "steel_dark")
    m.box((fx + 0.2, 9.9, Z + 0.5), (fx + 1.2, 11.4, Z + 0.75), "steel_dark")
    m.box((fx + 0.55, 9.9, Z - 0.12), (fx + 0.85, 11.15, Z + 0.12), "steel")
    m.box((fx + 0.1, 7.7, Z - 0.35), (fx + 1.2, 8.3, Z + 0.35), "steel_dark")
    m.sight = (fx + 0.7, 11.05)
    # Muzzle
    bx = barrel_end
    if brake == "ak74":
        m.cyl("x", bx, bx + 2.6, 9.1, Z, 0.78, "steel_dark", sides=16, ends=False)
        m.box((bx + 2.59, 8.6, Z - 0.5), (bx + 2.61, 9.6, Z + 0.5), "bore", faces=["east"], full=True)
        for k in range(2):
            x = bx + 0.5 + k * 0.9
            m.box((x, 8.7, Z + 0.68), (x + 0.45, 9.5, Z + 0.8), "bore", faces=["south"])
            m.box((x, 8.7, Z - 0.8), (x + 0.45, 9.5, Z - 0.68), "bore", faces=["north"])
        m.box((bx + 1.2, 9.8, Z - 0.35), (bx + 2.4, 9.9, Z + 0.35), "bore", faces=["up"])
    elif brake == "slant":
        m.cyl("x", bx, bx + 1.6, 9.1, Z, 0.68, "steel_dark", sides=16, ends=False)
        m.box((bx + 1.0, 9.1, Z - 0.7), (bx + 1.8, 9.8, Z + 0.7), "steel_dark", rot=("z", -22.5, (bx + 1.0, 9.1, Z)))
        m.box((bx + 1.59, 8.6, Z - 0.45), (bx + 1.61, 9.5, Z + 0.45), "bore", faces=["east"], full=True)
    else:   # a shotgun's plain big muzzle
        m.cyl("x", bx, bx + 1.2, 9.1, Z, 0.82, "steel_dark", sides=16, ends=False)
        m.box((bx + 1.19, 8.5, Z - 0.6), (bx + 1.21, 9.7, Z + 0.6), "bore", faces=["east"], full=True)
    # Stock
    if stock == "wood":
        # The classic AK stock: drops from the receiver to a tall butt, a slim wrist, a straight comb
        m.profile([(-12.6, 3.7, 8.7), (-9.0, 4.4, 8.8), (-5.0, 5.4, 8.95), (-2.0, 6.3, 9.05), (-0.2, 6.6, 9.2)], 1.9, wood,
                  taper=[(-12.6, 2.0), (-4.0, 1.75), (-0.2, 1.6)])
        m.box((-13.1, 3.6, Z - 1.05), (-12.6, 8.8, Z + 1.05), "steel_dark")              # butt plate
        m.box((-12.95, 7.6, Z - 0.5), (-12.6, 8.4, Z + 0.5), "steel_light")              # trap door
        m.box((-0.8, 6.5, Z - 0.9), (0.2, 9.3, Z + 0.9), "steel_dark")                  # tang
        m.box((-10.0, 8.5, Z + 1.0), (-8.6, 8.9, Z + 1.06), "steel_light", faces=["south"])   # sling swivel
        m.stock_rear = -13.1
    elif stock == "folding":
        # AK-74M: a side-folding polymer stock, open: a frame with a hole through it
        m.box((-12.4, 8.5, Z - 0.85), (-0.2, 9.4, Z + 0.85), "polymer")                  # top bar (comb)
        m.box((-12.4, 4.2, Z - 0.85), (-11.2, 9.4, Z + 0.85), "polymer")                # rear upright
        m.profile([(-11.2, 4.2, 5.6), (-6.0, 5.0, 6.3), (-0.2, 6.2, 7.4)], 1.7, "polymer")   # bottom bar, rising forward
        m.box((-1.4, 6.2, Z - 0.85), (-0.2, 9.4, Z + 0.85), "polymer")                  # front, at the hinge
        m.box((-12.9, 4.0, Z - 1.0), (-12.4, 9.6, Z + 1.0), "rubber")
        m.box((-0.6, 6.8, z1), (0.4, 9.2, z1 + 0.5), "steel_dark")                     # the folding hinge
        m.cyl("z", z1 + 0.2, z1 + 0.6, -0.1, 8.0, 0.45, "steel_dark", sides=8)
        m.stock_rear = -12.9
    # Magazine
    if mag_kind == "curved":
        m.mag(9.6, 6.6, 2.8, 7.2, Z - 0.8, Z + 0.8, mag_tex, curve=2, base="steel_dark", ribs=4, rib_tex=mag_tex)
    else:   # a shotgun's big box
        m.box((9.0, 1.4, Z - 1.3), (13.2, 6.6, Z + 1.3), mag_tex)
        m.box((8.8, 1.0, Z - 1.4), (13.4, 1.6, Z + 1.4), "steel_dark")
        for k in range(3):
            m.box((9.3, 2.0 + k * 1.4, Z + 1.3), (12.9, 2.3 + k * 1.4, Z + 1.36), "steel_dark", faces=["south"])


def ak74(m):
    ak_platform(m, wood="polymer_plum", stock="folding", mag_tex="polymer_plum", brake="ak74", rail_cover=False)


def akm(m):
    ak_platform(m, wood="birch", stock="wood", mag_tex="steel", brake="slant")


def saiga12(m):
    ak_platform(m, wood="polymer", stock="folding", mag_tex="polymer", brake="shotgun", barrel_end=26.0, mag_kind="box", rail_cover=True)


def m4a1(m):
    ar_platform(m)


# ---------------------------------------------------------------------------------------------------------------
# Pistols: P17, M1911, Hawk .50; the R357 revolver
# ---------------------------------------------------------------------------------------------------------------
def pistol(m, slide_len=12.0, slide_tex="steel_dark", frame_tex="polymer", grip_tex="polymer", grip_check="grip_check",
           slide_h=2.2, w=1.45, hammer=False, rail=True, bushing=False, beaver=False, big=False):
    x0, x1 = 1.0, 1.0 + slide_len
    z0, z1 = zs(w)
    yb, yt = 8.0, 8.0 + slide_h
    # Slide: body, rear and front serrations, the ejection port with the barrel hood, sights
    m.box((x0, yb, z0), (x1, yt, z1), slide_tex)
    m.box((x0 + 0.2, yt, z0 + 0.15), (x1 - 0.2, yt + 0.25, z1 - 0.15), slide_tex)        # the top's bevel
    m.serrations(x0 + 0.3, x0 + 2.8, yb + 0.3, yt - 0.2, z0, z1, 7)
    m.serrations(x1 - 2.4, x1 - 0.4, yb + 0.3, yt - 0.2, z0, z1, 5)
    m.box((x0 + 4.0, yt - 0.1, Z - 0.55), (x0 + 6.6, yt + 0.27, Z + 0.55), "bore", faces=["up"])
    m.box((x0 + 4.2, yt - 0.05, Z - 0.45), (x0 + 6.4, yt + 0.28, Z + 0.45), "steel_light", faces=["up", "south"])
    m.box((x0 + 4.0, yb + 0.9, z1), (x0 + 6.6, yt - 0.1, z1 + 0.04), "bore", faces=["south"])
    m.box((x0 + 4.3, yb + 1.1, z1 + 0.04), (x0 + 6.4, yt - 0.25, z1 + 0.07), "steel_light", faces=["south"])
    m.box((x0 + 0.3, yt + 0.25, Z - 0.6), (x0 + 1.1, yt + 0.85, Z + 0.6), "steel_dark")             # rear sight
    m.box((x0 + 0.3, yt + 0.85, Z - 0.6), (x0 + 1.1, yt + 1.05, Z - 0.2), "steel_dark")
    m.box((x0 + 0.3, yt + 0.85, Z + 0.2), (x0 + 1.1, yt + 1.05, Z + 0.6), "steel_dark")
    m.box((x0 + 0.29, yt + 0.55, Z - 0.5), (x0 + 0.31, yt + 0.75, Z - 0.3), "paint_white", faces=["west"])
    m.box((x0 + 0.29, yt + 0.55, Z + 0.3), (x0 + 0.31, yt + 0.75, Z + 0.5), "paint_white", faces=["west"])
    m.box((x1 - 1.0, yt + 0.25, Z - 0.18), (x1 - 0.5, yt + 0.95, Z + 0.18), "steel_dark")            # front post
    m.box((x1 - 1.01, yt + 0.6, Z - 0.1), (x1 - 0.99, yt + 0.8, Z + 0.1), "paint_white", faces=["west"])
    m.sight = (x1 - 0.75, yt + 0.92)
    # Muzzle end: barrel crown (and a bushing)
    m.box((x1 - 0.02, yb + 0.9, Z - 0.42), (x1, yb + 1.74, Z + 0.42), "bore", faces=["east"], full=True)
    if bushing:
        m.cyl("x", x1 - 0.1, x1 + 0.35, yb + 1.3, Z, 0.62, "steel", sides=16)
    # Frame: dust cover (with an accessory rail), trigger guard, grip, mag base
    fz0, fz1 = zs(w - 0.15)
    m.box((x0 + 1.4, yb - 1.4, fz0), (x1 - 0.6, yb, fz1), frame_tex)
    if rail:
        for k in range(3):
            x = x1 - 4.4 + k * 1.3
            m.box((x, yb - 1.75, Z - 0.6), (x + 0.6, yb - 1.4, Z + 0.6), frame_tex)
    m.trigger_guard(x0 + 4.2, x0 + 7.4, yb - 1.4, 1.7, frame_tex, t=0.4)
    m.box((x0 + 0.6, yb - 1.6, fz0), (x0 + 4.2, yb, fz1), frame_tex)
    m.grip(x0 + 0.4, yb - 1.2, 2.9, 5.2, Z - (w - 0.05) / 2, Z + (w - 0.05) / 2, grip_tex, angle=-22.5, check_tex=grip_check, base="steel_dark")
    m.box((x0 + 0.1, yb - 0.9, Z - 0.35), (x0 + 0.7, yb + 0.2, Z + 0.35), frame_tex if not beaver else "steel")   # beavertail
    # Slide stop on the left, mag release, takedown lever
    m.box((x0 + 4.6, yb - 0.55, z0 - 0.25), (x0 + 7.4, yb - 0.15, z0), "steel_dark")
    m.box((x0 + 3.5, yb - 1.4, z1), (x0 + 4.1, yb - 0.9, z1 + 0.2), "steel_dark")
    if hammer:
        m.box((x0 - 0.7, yt - 1.0, Z - 0.3), (x0 + 0.2, yt + 0.4, Z + 0.3), "steel_dark", rot=("z", 22.5, (x0, yt - 1.0, Z)))
        m.box((x0 - 1.1, yt + 0.1, Z - 0.3), (x0 - 0.3, yt + 0.5, Z + 0.3), "knurl", rot=("z", 22.5, (x0, yt - 1.0, Z)))
    m.stock_rear = x0 - 1.0
    if big:
        m.box((x0 + 2.0, yt + 0.25, Z - 0.6), (x1 - 1.4, yt + 0.6, Z + 0.6), "rail")       # top rail of a big pistol


def p17(m):
    pistol(m, slide_len=11.6)


def m1911(m):
    pistol(m, slide_len=12.6, slide_tex="blued", frame_tex="blued", grip_tex="walnut", grip_check="wood_check", slide_h=2.0, w=1.3,
           hammer=True, rail=False, bushing=True, beaver=True)


def hawk50(m):
    pistol(m, slide_len=14.2, slide_tex="chrome", frame_tex="chrome", grip_tex="rubber", grip_check="grip_check", slide_h=2.7, w=1.7,
           hammer=True, rail=False, big=True)
    # The Hawk's slab-sided barrel with its triangular front and the gas port
    m.box((10.5, 8.2, Z - 0.95), (15.4, 10.3, Z + 0.95), "chrome")
    m.box((15.0, 8.2, Z - 0.85), (15.6, 10.0, Z + 0.85), "chrome", rot=("z", -22.5, (15.0, 8.2, Z)))
    m.box((15.38, 8.7, Z - 0.5), (15.4, 9.7, Z + 0.5), "bore", faces=["east"], full=True)


def r357(m):
    z0, z1 = zs(1.5)
    # Frame with its top strap and the recoil shield, the cylinder (fluted), crane and latch
    m.box((1.0, 6.6, z0), (4.4, 10.4, z1), "blued")
    m.box((4.4, 10.0, Z - 0.6), (8.8, 10.5, Z + 0.6), "blued")                          # top strap
    m.box((8.6, 6.6, z0), (9.3, 10.5, z1), "blued")                                     # front of the frame
    m.box((4.4, 6.6, z0), (8.8, 7.0, z1), "blued")
    m.cyl("x", 4.5, 8.5, 8.55, Z, 1.42, "blued", sides=16)
    for k in range(6):                                                                   # flutes
        import math as _m
        a = k * _m.pi / 3
        cy, cz = 8.55 + 1.38 * _m.sin(a), Z + 1.38 * _m.cos(a)
        m.box((5.0, cy - 0.2, cz - 0.2), (8.0, cy + 0.2, cz + 0.2), "steel_dark")
    m.box((3.0, 8.0, z0 - 0.15), (3.8, 8.6, z0), "knurl")                              # cylinder latch
    m.cyl("x", 9.3, 18.0, 9.55, Z, 0.58, "blued", sides=16)                               # barrel
    m.box((9.3, 7.2, Z - 0.55), (18.0, 9.1, Z + 0.55), "blued")                          # full underlug
    m.box((9.3, 10.0, Z - 0.42), (18.0, 10.3, Z + 0.42), "blued")                         # vent rib
    for k in range(6):
        x = 10.0 + k * 1.3
        m.box((x, 10.3, Z - 0.35), (x + 0.6, 10.35, Z + 0.35), "bore", faces=["up"])
    m.box((17.99, 9.05, Z - 0.4), (18.01, 10.05, Z + 0.4), "bore", faces=["east"], full=True)
    m.box((16.8, 10.3, Z - 0.15), (17.7, 11.1, Z + 0.15), "blued", rot=("z", -22.5, (17.7, 10.3, Z)))   # front ramp
    m.box((17.0, 10.55, Z - 0.16), (17.3, 10.85, Z + 0.16), "paint_red", faces=["west", "south", "north"])
    m.box((4.0, 10.5, Z - 0.55), (4.8, 10.95, Z - 0.15), "blued")                         # rear notch
    m.box((4.0, 10.5, Z + 0.15), (4.8, 10.95, Z + 0.55), "blued")
    m.sight = (17.3, 10.95)
    m.cyl("x", 9.3, 14.0, 7.6, Z, 0.25, "steel_light", sides=8)                           # ejector rod
    # Hammer, trigger guard (round), grip
    m.box((0.2, 9.6, Z - 0.3), (1.4, 10.6, Z + 0.3), "blued", rot=("z", 22.5, (1.0, 9.6, Z)))
    m.box((-0.5, 10.4, Z - 0.35), (0.6, 10.9, Z + 0.35), "knurl", rot=("z", 22.5, (1.0, 9.6, Z)))
    m.trigger_guard(2.0, 5.4, 6.6, 1.9, "blued", t=0.4)
    m.profile([(-1.2, 1.2, 4.6), (0.0, 1.0, 6.2), (1.2, 1.2, 6.8), (2.6, 2.0, 6.7)], 1.7, "walnut", step=0.4)
    m.checkering(-0.6, 2.0, 2.0, 5.6, Z - 0.86, Z + 0.86, tex="wood_check")
    m.stock_rear = -1.4


# ---------------------------------------------------------------------------------------------------------------
# SMGs: MP5, MP7, UZ-9, KV .45
# ---------------------------------------------------------------------------------------------------------------
def mp5(m):
    z0, z1 = zs(1.8)
    # Round-topped stamped receiver, cocking tube over the barrel, the drum rear sight and the hooded front sight
    m.box((0.0, 7.4, z0), (14.0, 9.4, z1), "steel")
    m.cyl("x", 0.0, 14.0, 9.4, Z, 0.9, "steel", sides=16, ends=False)
    m.box((0.0, 8.0, z1), (14.0, 8.25, z1 + 0.05), "steel_dark", faces=["south"])          # stamping line
    m.box((6.6, 8.5, z1), (10.0, 9.6, z1 + 0.04), "bore", faces=["south"])                  # ejection port
    m.box((6.9, 8.7, z1 + 0.04), (9.7, 9.4, z1 + 0.06), "steel_light", faces=["south"])
    m.cyl("x", 14.0, 20.6, 10.15, Z, 0.5, "steel", sides=16)                               # cocking tube
    m.box((17.5, 9.9, z0 - 0.9), (18.1, 10.4, z0), "steel_dark", rot=("y", 22.5, (17.8, 10.1, z0)))   # cocking handle (left)
    m.cyl("z", z0 - 1.4, z0 - 0.8, 17.3, 10.15, 0.32, "steel_dark", sides=8)
    m.cyl("z", Z - 1.0, Z + 1.0, 1.8, 10.9, 0.7, "steel_dark", sides=16, end_tex="steel")   # drum rear sight
    m.box((1.4, 10.1, Z - 0.6), (2.2, 10.6, Z + 0.6), "steel_dark")
    m.cyl("x", 19.8, 21.2, 10.9, Z, 0.75, "steel_dark", sides=16, ends=False)                # front sight hood ring
    m.box((20.35, 10.15, Z - 0.1), (20.65, 11.0, Z + 0.1), "steel")
    m.box((19.8, 9.6, Z - 0.5), (21.2, 10.3, Z + 0.5), "steel_dark")
    m.sight = (20.5, 10.95)
    # Barrel with the three lugs, handguard, trigger group, grip, magazine
    m.tube(14.0, 22.6, 8.7, Z, 0.45, "steel_dark", sides=16)
    for a in range(3):
        m.box((21.6, 8.7 - 0.62, Z - 0.15 - (a - 1) * 0.4), (22.4, 8.7 + 0.62, Z + 0.15 - (a - 1) * 0.4), "steel_dark")
    m.profile([(13.6, 7.1, 9.7), (14.4, 6.8, 9.8), (19.0, 6.9, 9.8), (19.6, 7.5, 9.6)], 2.2, "polymer", step=0.5)
    for k in range(4):
        m.box((14.8 + k * 1.1, 7.2, Z + 1.1), (15.3 + k * 1.1, 9.4, Z + 1.14), "polymer_grey", faces=["south"])
    m.box((1.0, 5.8, Z - 0.85), (8.4, 7.4, Z + 0.85), "polymer")                            # trigger housing
    m.trigger_guard(3.4, 6.8, 5.8, 1.8, "polymer", t=0.45)
    m.grip(1.2, 6.0, 2.2, 4.4, Z - 0.8, Z + 0.8, "polymer", angle=-22.5, check_tex="grip_check")
    m.box((1.6, 6.4, z0 - 0.3), (2.6, 6.9, z0), "steel_dark")                             # selector
    m.cyl("z", z0 - 0.5, z0 - 0.3, 2.1, 6.65, 0.4, "steel_dark", sides=8)
    m.mag(8.4, 7.4, 1.9, 6.2, Z - 0.6, Z + 0.6, "steel_dark", curve=1, base="steel_dark", ribs=3)
    m.box((8.0, 6.8, Z - 0.75), (11.0, 7.4, Z + 0.75), "steel")                            # mag well
    # Retractable stock: two rods and the butt plate
    m.cyl("x", -10.2, 0.0, 9.6, Z - 0.6, 0.28, "steel_light", sides=8)
    m.cyl("x", -10.2, 0.0, 9.6, Z + 0.6, 0.28, "steel_light", sides=8)
    m.box((-11.0, 5.8, Z - 0.9), (-10.2, 10.3, Z + 0.9), "steel_dark")
    m.box((-11.4, 5.6, Z - 1.0), (-11.0, 10.4, Z + 1.0), "rubber")
    m.box((-10.2, 9.0, Z - 0.9), (-9.2, 10.3, Z + 0.9), "steel_dark")
    m.box((-0.6, 8.0, Z - 1.0), (0.2, 10.2, Z + 1.0), "steel_dark")                        # end cap
    m.stock_rear = -11.4


def mp7(m):
    z0, z1 = zs(1.9)
    m.box((0.0, 6.0, z0), (11.6, 9.6, z1), "polymer")
    m.box((0.6, 9.6, Z - 0.9), (11.2, 10.0, Z + 0.9), "polymer")
    m.rail(0.6, 11.2, 10.0, Z, w=1.5)
    m.box((11.6, 6.6, Z - 0.8), (13.0, 9.2, Z + 0.8), "polymer")                          # nose
    m.tube(13.0, 15.6, 8.5, Z, 0.42, "steel_dark", sides=16)
    m.cyl("x", 14.6, 16.8, 8.5, Z, 0.55, "steel_dark", sides=16)                           # flash hider
    m.box((16.79, 8.1, Z - 0.3), (16.81, 8.9, Z + 0.3), "bore", faces=["east"], full=True)
    m.side_rail(8.0, 11.0, 8.2, z1, "south", h=1.2)
    m.box((5.4, 8.6, z1), (8.4, 9.4, z1 + 0.04), "bore", faces=["south"])
    # Flip-up sights on the rail
    m.box((1.0, 10.85, Z - 0.7), (2.0, 11.9, Z + 0.7), "polymer")
    m.box((1.2, 11.9, Z - 0.7), (1.8, 12.3, Z - 0.35), "polymer")
    m.box((1.2, 11.9, Z + 0.35), (1.8, 12.3, Z + 0.7), "polymer")
    m.box((10.0, 10.85, Z - 0.6), (10.8, 11.9, Z + 0.6), "polymer")
    m.box((10.3, 11.9, Z - 0.12), (10.5, 12.25, Z + 0.12), "steel_light")
    m.sight = (10.4, 12.2)
    # Grip with the magazine inside it, trigger guard, folded foregrip
    m.grip(2.4, 6.2, 2.6, 4.8, Z - 0.85, Z + 0.85, "polymer", angle=-22.5, check_tex="grip_check", base="steel_dark")
    m.trigger_guard(4.6, 7.8, 6.0, 1.6, "polymer", t=0.45)
    m.box((8.6, 5.2, Z - 0.6), (11.2, 6.0, Z + 0.6), "polymer")                            # folded foregrip
    m.box((10.6, 4.2, Z - 0.55), (11.4, 6.0, Z + 0.55), "polymer")
    # Retracted stock: rods along the sides and a small butt plate
    m.cyl("x", -5.0, 0.2, 8.6, Z - 0.75, 0.28, "polymer", sides=8)
    m.cyl("x", -5.0, 0.2, 8.6, Z + 0.75, 0.28, "polymer", sides=8)
    m.box((-5.6, 6.2, Z - 1.0), (-5.0, 9.6, Z + 1.0), "polymer")
    m.box((-6.0, 6.0, Z - 1.05), (-5.6, 9.8, Z + 1.05), "rubber")
    m.stock_rear = -6.0


def uz9(m):
    z0, z1 = zs(2.0)
    m.box((0.0, 7.0, z0), (12.6, 10.0, z1), "steel")
    m.box((0.4, 10.0, Z - 0.9), (12.2, 10.4, Z + 0.9), "steel")                           # top cover
    m.box((8.0, 10.4, Z - 0.35), (9.2, 11.1, Z + 0.35), "steel_dark")                    # cocking knob
    for k in range(5):
        m.box((8.1, 10.5 + k * 0.12, Z - 0.36), (9.1, 10.56 + k * 0.12, Z + 0.36), "bore", faces=["south", "north"])
    m.box((0.6, 10.4, Z - 0.6), (1.4, 11.3, Z - 0.3), "steel_dark")                      # rear sight ears
    m.box((0.6, 10.4, Z + 0.3), (1.4, 11.3, Z + 0.6), "steel_dark")
    m.box((11.2, 10.4, Z - 0.5), (11.8, 11.4, Z - 0.35), "steel_dark")                   # front sight wings and post
    m.box((11.2, 10.4, Z + 0.35), (11.8, 11.4, Z + 0.5), "steel_dark")
    m.box((11.4, 10.4, Z - 0.1), (11.6, 11.2, Z + 0.1), "steel_light")
    m.sight = (11.5, 11.15)
    m.box((5.0, 8.2, z1), (8.0, 9.4, z1 + 0.04), "bore", faces=["south"])
    m.cyl("x", 12.6, 13.6, 8.6, Z, 0.95, "knurl", sides=16)                              # barrel nut
    m.tube(13.6, 16.4, 8.6, Z, 0.45, "steel_dark", sides=16)
    for k in range(6):
        m.box((0.8 + k * 1.8, 7.3, z1), (1.6 + k * 1.8, 9.6, z1 + 0.05), "steel_dark", faces=["south"])   # pressed ribs
    # Grip with the mag through it, the grip safety, trigger guard
    m.box((3.0, 1.2, Z - 0.95), (6.0, 7.0, Z + 0.95), "polymer")
    m.checkering(3.3, 5.7, 1.8, 6.6, Z - 0.95, Z + 0.95)
    m.box((2.6, 3.4, Z - 0.55), (3.0, 6.6, Z + 0.55), "steel_dark")
    m.box((3.4, -1.8, Z - 0.7), (5.6, 1.2, Z + 0.7), "steel_dark")                       # mag, sticking out below
    m.box((3.2, -2.3, Z - 0.8), (5.8, -1.8, Z + 0.8), "steel_dark")
    m.trigger_guard(6.0, 9.6, 7.0, 1.8, "steel_dark", t=0.4)
    # Folding metal stock, folded under
    m.box((-0.6, 6.4, Z - 0.9), (0.0, 9.6, Z + 0.9), "steel_dark")
    m.box((-0.4, 6.0, Z - 0.95), (10.6, 6.5, Z - 0.75), "steel_dark")
    m.box((-0.4, 6.0, Z + 0.75), (10.6, 6.5, Z + 0.95), "steel_dark")
    m.box((10.2, 5.4, Z - 0.95), (10.8, 6.5, Z + 0.95), "steel_dark")
    m.stock_rear = -0.6


def kv45(m):
    z0, z1 = zs(1.9)
    # Upper receiver with a full rail; the tall angled lower (the recoil mechanism) in front of the grip
    m.box((0.0, 8.0, z0), (13.4, 10.0, z1), "polymer")
    m.rail(0.3, 13.2, 10.0, Z, w=1.5)
    m.box((4.0, 4.4, z0), (10.8, 8.0, z1), "polymer")
    m.box((10.8, 5.4, z0), (13.4, 8.0, z1), "polymer")
    m.box((8.6, 4.4, z0 + 0.05), (12.6, 6.4, z1 - 0.05), "polymer", rot=("z", 45, (10.8, 4.4, Z)))
    m.box((4.6, 5.0, z1), (9.8, 7.4, z1 + 0.05), "polymer_grey", faces=["south"])
    m.screws_both(5.4, 6.8, z0, z1, r=0.22)
    m.screws_both(9.0, 6.8, z0, z1, r=0.22)
    m.box((7.0, 8.4, z1), (10.0, 9.4, z1 + 0.04), "bore", faces=["south"])
    # Barrel shroud with holes, the barrel
    m.box((13.4, 7.4, Z - 0.85), (17.4, 9.6, Z + 0.85), "polymer")
    for k in range(3):
        m.box((13.9 + k * 1.2, 8.0, Z + 0.85), (14.5 + k * 1.2, 9.0, Z + 0.9), "bore", faces=["south"])
    m.tube(17.4, 18.6, 8.5, Z, 0.45, "steel_dark", sides=16)
    # Flip sights
    m.box((0.8, 10.85, Z - 0.7), (1.8, 12.1, Z + 0.7), "polymer")
    m.box((12.2, 10.85, Z - 0.6), (13.0, 12.0, Z + 0.6), "polymer")
    m.box((12.5, 12.0, Z - 0.12), (12.7, 12.35, Z + 0.12), "steel_light")
    m.sight = (12.6, 12.3)
    # Grip, trigger guard, the long magazine in front of the trigger
    m.grip(1.4, 8.2, 2.3, 4.6, Z - 0.85, Z + 0.85, "polymer", angle=-22.5, check_tex="grip_check")
    m.trigger_guard(3.4, 6.2, 8.0, 1.6, "polymer", t=0.45)
    m.box((6.6, -2.6, Z - 0.7), (8.4, 4.4, Z + 0.7), "steel_dark")
    m.box((6.4, -3.0, Z - 0.8), (8.6, -2.6, Z + 0.8), "polymer")
    # Folding skeleton stock, open
    m.box((-10.0, 9.0, Z - 0.6), (0.0, 9.8, Z + 0.6), "polymer")
    m.box((-10.0, 5.6, Z - 0.6), (-9.0, 9.8, Z + 0.6), "polymer")
    m.box((-9.0, 5.6, Z - 0.5), (-3.0, 6.3, Z + 0.5), "polymer")
    m.box((-3.4, 5.9, Z - 0.5), (-2.6, 9.0, Z + 0.5), "polymer")
    m.box((-10.4, 5.4, Z - 0.8), (-10.0, 10.0, Z + 0.8), "rubber")
    m.stock_rear = -10.4


# ---------------------------------------------------------------------------------------------------------------
# MK17 battle rifle, AUG bullpup
# ---------------------------------------------------------------------------------------------------------------
def mk17(m):
    z0, z1 = zs(2.0)
    # The long aluminium upper with its full-length rail, the polymer lower
    m.box((0.0, 8.2, z0), (21.0, 10.6, z1), "alu_tan")
    m.rail(0.2, 20.8, 10.6, Z, w=1.6)
    m.side_rail(13.0, 20.6, 9.0, z1, "south", h=1.2, tex="rail_tan")
    m.side_rail(13.0, 20.6, 9.0, z0, "north", h=1.2, tex="rail_tan")
    m.box((13.2, 7.4, Z - 0.8), (20.6, 8.2, Z + 0.8), "rail_tan")
    m.box((0.6, 5.2, Z - 0.95), (12.0, 8.2, Z + 0.95), "polymer_tan")
    m.box((7.6, 3.6, Z - 1.0), (11.4, 5.2, Z + 1.0), "polymer_tan")                    # mag well
    m.box((4.0, 8.6, z1), (7.6, 9.9, z1 + 0.04), "bore", faces=["south"])
    m.box((10.6, 9.6, z0 - 0.9), (11.4, 10.2, z0), "steel_dark")                         # side charging handle
    m.box((10.6, 9.4, z0 - 1.3), (11.6, 10.4, z0 - 0.9), "polymer")
    m.screws_both(2.0, 7.0, Z - 0.95, Z + 0.95, r=0.24, tex="steel")
    m.screws_both(10.6, 7.0, Z - 0.95, Z + 0.95, r=0.24, tex="steel")
    m.box((2.4, 6.4, Z - 1.2), (3.6, 6.8, Z - 0.95), "steel_dark")                      # selector
    m.trigger_guard(3.2, 7.0, 5.2, 1.6, "polymer_tan", t=0.45)
    m.grip(3.0, 5.4, 2.2, 4.6, Z - 0.8, Z + 0.8, "polymer_tan", angle=-22.5, check_tex="grip_check_tan")
    # Barrel, gas block, flash hider; folding sights
    m.tube(21.0, 28.0, 9.2, Z, 0.6, "steel_dark", sides=16)
    m.box((21.0, 8.4, Z - 0.75), (22.4, 10.0, Z + 0.75), "steel_dark")
    m.cyl("x", 28.0, 30.2, 9.2, Z, 0.75, "steel_dark", sides=16, ends=False)
    m.box((30.19, 8.7, Z - 0.5), (30.21, 9.7, Z + 0.5), "bore", faces=["east"], full=True)
    for k in range(4):
        m.box((28.3 + k * 0.45, 9.8, Z - 0.2), (28.5 + k * 0.45, 9.96, Z + 0.2), "bore", faces=["up"])
    m.box((19.8, 11.05, Z - 0.7), (20.6, 12.2, Z - 0.5), "steel_dark")
    m.box((19.8, 11.05, Z + 0.5), (20.6, 12.2, Z + 0.7), "steel_dark")
    m.box((20.1, 11.05, Z - 0.1), (20.3, 12.0, Z + 0.1), "steel_light")
    m.box((0.8, 11.05, Z - 0.8), (2.0, 12.1, Z + 0.8), "steel_dark")
    m.sight = (20.2, 12.0)
    # Folding stock with a cheek riser
    m.box((-0.8, 7.2, Z - 0.9), (0.2, 10.4, Z + 0.9), "polymer")
    m.box((-11.6, 8.4, Z - 0.8), (-0.8, 9.9, Z + 0.8), "polymer_tan")
    m.box((-9.6, 9.9, Z - 0.7), (-3.4, 10.7, Z + 0.7), "polymer_tan")                  # cheek riser
    m.box((-11.6, 5.4, Z - 0.8), (-10.4, 8.4, Z + 0.8), "polymer_tan")
    m.profile([(-10.4, 5.4, 6.4), (-5.0, 6.2, 7.2), (-0.8, 7.2, 8.0)], 1.5, "polymer_tan")
    m.box((-12.1, 5.2, Z - 1.0), (-11.6, 10.0, Z + 1.0), "rubber")
    m.mag(7.9, 3.6, 3.2, 5.2, Z - 0.75, Z + 0.75, "steel_dark", curve=0, base="steel_dark", ribs=2)
    m.stock_rear = -12.1


def aug(m):
    z0, z1 = zs(2.3)
    # One long olive body: the receiver housing the action behind the grip, the butt
    m.profile([(-9.6, 5.2, 9.0), (-6.0, 4.6, 9.6), (2.0, 4.8, 9.8), (6.0, 6.6, 9.9), (12.6, 7.2, 9.9)], 2.4, "polymer_od", step=0.6)
    m.box((-10.0, 5.0, Z - 1.25), (-9.6, 9.2, Z + 1.25), "rubber")
    m.box((-2.0, 8.0, z1), (2.0, 9.2, z1 + 0.05), "bore", faces=["south"])              # ejection port
    m.box((-8.0, 6.0, z1), (0.0, 6.2, z1 + 0.05), "polymer", faces=["south"])           # parting line
    m.screws_both(-7.0, 7.2, z0, z1, r=0.22)
    # The magazine behind the grip (see-through smoke polymer)
    m.box((-3.8, 0.8, Z - 0.75), (-1.2, 4.8, Z + 0.75), "polymer_grey")
    m.box((-4.0, 0.4, Z - 0.85), (-1.0, 0.8, Z + 0.85), "polymer")
    # The big trigger guard closing over grip and hand, the grip, the folding foregrip
    m.box((1.6, 2.0, Z - 0.9), (5.0, 7.0, Z + 0.9), "polymer_od", rot=("z", -22.5, (3.3, 7.0, Z)))
    m.checkering(1.9, 4.7, 2.6, 6.4, Z - 0.9, Z + 0.9, tex="grip_check")
    m.box((5.0, 2.2, Z - 0.5), (11.0, 2.9, Z + 0.5), "polymer_od")
    m.box((10.4, 2.2, Z - 0.5), (11.0, 6.6, Z + 0.5), "polymer_od")
    m.box((11.2, 3.0, Z - 0.7), (12.8, 7.2, Z + 0.7), "polymer_od", rot=("z", 22.5, (12.0, 7.2, Z)))   # foregrip
    # The barrel with its gas cylinder and the forward grip collar, the three-prong flash hider
    m.tube(12.6, 26.5, 8.9, Z, 0.55, "steel_dark", sides=16)
    m.cyl("x", 12.6, 15.0, 9.9, Z, 0.4, "steel", sides=8)
    m.cyl("x", 15.0, 16.0, 8.9, Z, 0.9, "steel_dark", sides=16)
    m.cyl("x", 26.5, 28.2, 8.9, Z, 0.7, "steel_dark", sides=16, ends=False)
    m.box((28.19, 8.5, Z - 0.45), (28.21, 9.3, Z + 0.45), "bore", faces=["east"], full=True)
    # The built-in optic: a handle-shaped housing over the receiver with a short scope
    m.box((1.0, 9.9, Z - 0.7), (2.4, 11.2, Z + 0.7), "polymer_od")
    m.box((9.0, 9.9, Z - 0.7), (10.4, 11.2, Z + 0.7), "polymer_od")
    m.scope(0.6, 11.0, 11.8, Z, 0.75, tex="polymer_od", bell=1.25, turrets=False, mounts=())
    m.box((10.9, 12.6, Z - 0.15), (11.0, 12.9, Z + 0.15), "steel_dark")                # backup notch on top
    m.sight = (11.0, 11.8)
    m.stock_rear = -10.0


# ---------------------------------------------------------------------------------------------------------------
# Marksman and sniper rifles
# ---------------------------------------------------------------------------------------------------------------
def pso_scope(m):
    m.scope(-1.0, 9.4, 12.6, Z, 0.75, tex="scope_body", bell=1.35, base_y=10.4)
    m.box((0.6, 11.0, Z - 1.3), (2.0, 11.8, Z - 0.8), "rubber")                       # PSO's sun shade side
    m.sight = (9.4, 12.6)


def svd(m):
    z0, z1 = zs(1.7)
    m.box((0.0, 6.8, z0), (12.4, 9.4, z1), "steel")
    m.box((0.4, 9.4, z0 + 0.1), (11.6, 9.8, z1 - 0.1), "steel")
    m.cyl("x", 0.4, 11.6, 9.8, Z, 0.7, "steel", sides=16, ends=False)
    m.box((6.6, 8.6, z1), (10.6, 9.5, z1 + 0.04), "bore", faces=["south"])
    m.box((9.8, 8.7, z1), (10.4, 9.2, z1 + 1.2), "steel_light")
    m.box((3.0, 8.45, z1), (9.6, 8.8, z1 + 0.15), "steel_dark")
    m.trigger_guard(2.6, 6.0, 6.8, 1.8, "steel_dark", t=0.4)
    # Skeleton wooden stock with its cheek rest and the hole for the thumb
    m.box((-12.6, 8.4, Z - 0.9), (-0.2, 9.4, Z + 0.9), "birch")
    m.box((-9.6, 9.4, Z - 0.8), (-4.0, 10.2, Z + 0.8), "birch")                          # cheek rest
    m.box((-12.6, 3.8, Z - 0.95), (-11.0, 9.4, Z + 0.95), "birch")
    m.profile([(-11.0, 3.8, 5.0), (-6.0, 4.6, 5.8), (-3.4, 5.0, 6.4)], 1.8, "birch")
    m.box((-3.6, 3.6, Z - 0.85), (-0.2, 8.4, Z + 0.85), "birch", rot=("z", -22.5, (-1.9, 8.4, Z)))   # the grip in the stock
    m.box((-13.1, 3.6, Z - 1.0), (-12.6, 9.6, Z + 1.0), "rubber")
    # The long barrel, gas block, vented wooden handguard, the slotted flash hider, front sight
    m.tube(12.4, 29.6, 9.2, Z, 0.5, "steel_dark", sides=16)
    m.box((12.4, 7.6, Z - 1.1), (21.0, 10.8, Z + 1.1), "birch")
    for k in range(6):
        m.box((13.2 + k * 1.25, 8.6, Z + 1.1), (13.7 + k * 1.25, 10.2, Z + 1.14), "bore", faces=["south"])
        m.box((13.2 + k * 1.25, 8.6, Z - 1.14), (13.7 + k * 1.25, 10.2, Z - 1.1), "bore", faces=["north"])
    m.box((21.0, 8.4, Z - 0.7), (22.4, 10.6, Z + 0.7), "steel_dark")
    m.cyl("x", 29.6, 31.4, 9.2, Z, 0.62, "steel_dark", sides=16, ends=False)
    m.box((31.39, 8.8, Z - 0.42), (31.41, 9.6, Z + 0.42), "bore", faces=["east"], full=True)
    for k in range(4):
        m.box((29.9 + k * 0.4, 9.6, Z + 0.5), (30.1 + k * 0.4, 10.0, Z + 0.62), "bore", faces=["south"])
    m.box((27.4, 9.6, Z - 0.5), (28.2, 10.9, Z + 0.5), "steel_dark")
    m.grip(0.0, 6.8, 0.1, 0.1, Z - 0.05, Z + 0.05, "steel_dark")                        # (the grip is in the stock)
    m.mag(8.6, 6.8, 2.6, 4.2, Z - 0.75, Z + 0.75, "steel_dark", curve=0, base="steel_dark", ribs=2)
    pso_scope(m)
    m.stock_rear = -13.1


def sr25(m):
    def optic(m):
        m.scope(-0.6, 11.4, 12.6, Z, 0.8, base_y=11.25)
        m.sight = (11.4, 12.6)
    ar_platform(m, barrel_end=27.6, handguard_end=23.4, stock="a2", optic=optic, mag_depth=4.6, mag_curve=0, big=1.08, bipod=True)


def bolt_rifle(m, stock_tex="walnut", barrel_end=30.0, scope=True, long_stock=False, chassis=False):
    z0, z1 = zs(1.8)
    if chassis:
        # AX .338: an aluminium chassis with a skeleton stock, adjustable cheek and monopod
        m.box((-1.0, 6.6, z0), (18.0, 8.6, z1), "alu_od")
        m.box((10.0, 6.8, Z - 1.4), (22.0, 10.6, Z + 1.4), "alu_od")
        for k in range(4):
            m.box((11.0 + k * 2.6, 8.0, Z + 1.4), (12.6 + k * 2.6, 9.6, Z + 1.45), "bore", faces=["south"])
        m.rail(10.2, 21.8, 10.6, Z, w=1.6)
        m.box((-12.4, 8.4, Z - 0.8), (-1.0, 9.6, Z + 0.8), "alu_od")
        m.box((-10.4, 9.6, Z - 0.75), (-4.6, 10.8, Z + 0.75), "polymer")             # cheek piece
        m.cyl("y", 9.6, 10.6, -9.0, Z, 0.3, "steel_light", sides=8)
        m.cyl("y", 9.6, 10.6, -6.0, Z, 0.3, "steel_light", sides=8)
        m.box((-12.4, 4.6, Z - 0.85), (-11.2, 9.6, Z + 0.85), "alu_od")
        m.box((-11.2, 4.6, Z - 0.7), (-5.0, 5.4, Z + 0.7), "alu_od")
        m.cyl("y", 2.4, 4.6, -10.6, Z, 0.35, "steel_light", sides=8)                    # monopod
        m.box((-12.9, 4.4, Z - 1.0), (-12.4, 10.0, Z + 1.0), "rubber")
        m.grip(1.0, 6.8, 2.2, 4.6, Z - 0.8, Z + 0.8, "polymer", angle=-22.5, check_tex="grip_check")
        m.trigger_guard(2.8, 6.4, 6.6, 1.6, "alu_od", t=0.45)
        m.box((7.0, 3.0, Z - 0.8), (10.0, 6.6, Z + 0.8), "steel_dark")                # box magazine
        m.stock_rear = -12.9
    else:
        # A one-piece wooden stock: butt, comb, wrist with a pistol-grip swell, the forend under the barrel
        end = 25.6 if long_stock else 21.0
        m.profile([(-13.6, 4.0, 9.3), (-8.0, 5.2, 9.4), (-3.0, 6.2, 9.5), (-0.4, 6.6, 9.0), (2.0, 6.6, 8.7),
                   (9.0, 7.1, 9.0), (end - 2, 7.6, 9.0), (end, 8.1, 9.0)], 1.9, stock_tex,
                  taper=[(-13.6, 2.0), (-2.0, 1.7), (2.0, 1.75), (end, 1.4)])
        m.box((-1.2, 3.2, Z - 0.8), (1.0, 6.8, Z + 0.8), stock_tex, rot=("z", -22.5, (0, 6.8, Z)))   # grip swell
        m.checkering(-0.9, 0.8, 4.0, 6.2, Z - 0.8, Z + 0.8, tex="wood_check")
        m.checkering(10.0, 15.0, 7.4, 8.6, Z - 0.88, Z + 0.88, tex="wood_check")
        m.box((-14.1, 3.9, Z - 1.05), (-13.6, 9.5, Z + 1.05), "rubber" if not long_stock else "steel_dark")
        m.trigger_guard(2.6, 6.0, 7.0, 1.6, "steel_dark", t=0.4)
        m.box((-10.0, 4.3, Z - 0.3), (-9.0, 4.6, Z + 0.3), "steel_light")              # sling swivels
        m.box((end - 3, 7.2, Z - 0.3), (end - 2, 7.6, Z + 0.3), "steel_light")
        if long_stock:
            m.box((11.0, 9.0, Z - 0.8), (25.0, 10.1, Z + 0.8), stock_tex)                 # Mosin's upper handguard
            for x in (12.0, 22.0):
                m.box((x, 7.2, Z - 1.0), (x + 0.6, 10.2, Z + 1.0), "steel_dark")           # barrel bands
        m.stock_rear = -14.1
    # Round receiver and its bolt with the handle on the right
    m.cyl("x", -0.6, 10.0, 9.6, Z, 0.95, "blued" if not chassis else "steel_dark", sides=16)
    m.box((3.0, 9.8, Z + 0.6), (6.6, 10.4, Z + 0.97), "bore", faces=["south"])          # ejection port
    m.cyl("x", -1.8, -0.6, 9.6, Z, 0.6, "steel", sides=16)                               # bolt shroud
    m.box((-0.2, 9.3, Z + 0.9), (0.4, 9.9, Z + 2.6), "steel_light", rot=("x", -22.5, (0.1, 9.6, Z + 0.9)))
    m.cyl("x", -0.4, 0.6, 9.0, Z + 2.6, 0.48, "steel_light" if not chassis else "polymer", sides=16)   # the bolt knob
    # Barrel (heavier at the receiver)
    m.cyl("x", 10.0, 16.0, 9.6, Z, 0.7, "blued" if not chassis else "steel_dark", sides=16)
    m.tube(16.0, barrel_end, 9.6, Z, 0.5, "blued" if not chassis else "steel_dark", sides=16)
    if chassis:
        # The big muzzle brake and a bipod
        m.box((barrel_end, 8.8, Z - 0.8), (barrel_end + 1.8, 10.4, Z + 0.8), "steel_dark")
        for k in range(3):
            m.box((barrel_end + 0.3 + k * 0.5, 8.9, Z + 0.8), (barrel_end + 0.55 + k * 0.5, 10.3, Z + 0.85), "bore", faces=["south"])
            m.box((barrel_end + 0.3 + k * 0.5, 8.9, Z - 0.85), (barrel_end + 0.55 + k * 0.5, 10.3, Z - 0.8), "bore", faces=["north"])
        for s in (-1, 1):
            m.box((19.6, 1.4, Z + s * 1.0 - 0.25), (20.2, 6.8, Z + s * 1.0 + 0.25), "steel_dark", rot=("x", 22.5 * s, (19.9, 6.8, Z)))
    if scope:
        m.scope(-0.6, 12.4, 12.6, Z, 0.85 if chassis else 0.75, bell=1.45 if chassis else 1.3, base_y=10.5,
                mounts=(1.8, 8.0))
        m.sight = (12.4, 12.6)
    else:
        # Iron sights: a tangent leaf at the receiver, a hooded post at the muzzle
        m.box((10.4, 10.2, Z - 0.6), (13.4, 10.6, Z + 0.6), "steel_dark")
        m.box((10.6, 10.6, Z - 0.6), (11.0, 11.1, Z + 0.6), "steel_dark")
        m.cyl("x", barrel_end - 1.2, barrel_end - 0.2, 10.7, Z, 0.55, "steel_dark", sides=16, ends=False)
        m.box((barrel_end - 0.8, 10.1, Z - 0.1), (barrel_end - 0.6, 10.95, Z + 0.1), "steel")
        m.sight = (barrel_end - 0.7, 10.95)


def m700(m):
    bolt_rifle(m, stock_tex="walnut", barrel_end=30.0)


def mosin(m):
    bolt_rifle(m, stock_tex="beech", barrel_end=31.4, scope=False, long_stock=True)
    m.box((26.2, 8.6, Z - 0.3), (27.2, 9.2, Z + 0.3), "steel_dark")                      # bayonet lug


def ax338(m):
    bolt_rifle(m, barrel_end=29.4, chassis=True)


# ---------------------------------------------------------------------------------------------------------------
# Double barrel; machine guns
# ---------------------------------------------------------------------------------------------------------------
def double_barrel(m):
    # Two barrels side by side under a sighting rib, the forend, the engraved action, the top lever, two triggers
    for zc in (Z - 0.62, Z + 0.62):
        m.tube(6.6, 31.2, 9.3, zc, 0.62, "blued", sides=16, wall=0.12)
    m.box((6.6, 9.85, Z - 0.35), (31.2, 10.2, Z + 0.35), "blued")
    m.box((30.4, 10.2, Z - 0.12), (30.7, 10.5, Z + 0.12), "brass")
    m.sight = (30.55, 10.45)
    m.box((6.6, 8.6, Z - 0.3), (31.2, 9.0, Z + 0.3), "blued")                            # lower rib
    m.box((7.4, 7.4, Z - 1.25), (16.0, 8.9, Z + 1.25), "walnut")
    m.checkering(9.0, 14.6, 7.6, 8.6, Z - 1.25, Z + 1.25, tex="wood_check")
    m.box((1.8, 7.0, Z - 1.2), (6.8, 10.2, Z + 1.2), "steel_light")                     # action
    m.box((2.2, 7.4, Z + 1.2), (6.4, 9.8, Z + 1.24), "chrome", faces=["south"])          # engraved side plate
    m.box((2.2, 7.4, Z - 1.24), (6.4, 9.8, Z - 1.2), "chrome", faces=["north"])
    m.box((0.6, 10.0, Z - 0.3), (2.6, 10.4, Z + 0.3), "steel_light", rot=("y", 22.5, (2.0, 10.2, Z)))   # top lever
    m.box((2.6, 5.6, Z - 0.3), (6.0, 6.0, Z + 0.3), "steel_light")                      # trigger guard
    m.box((5.6, 5.6, Z - 0.3), (6.0, 7.0, Z + 0.3), "steel_light")
    m.box((3.6, 6.0, Z - 0.15), (3.9, 7.0, Z + 0.15), "brass", rot=("z", 22.5, (3.7, 6.5, Z)))
    m.box((4.5, 6.0, Z - 0.15), (4.8, 7.0, Z + 0.15), "brass", rot=("z", 22.5, (4.6, 6.5, Z)))
    m.profile([(-13.8, 3.8, 9.2), (-8.0, 5.0, 9.4), (-2.0, 6.0, 9.6), (0.0, 6.6, 9.6), (1.8, 7.0, 9.8)], 1.9, "walnut",
              taper=[(-13.8, 2.0), (0.0, 1.7), (1.8, 2.2)])
    m.box((-0.6, 3.8, Z - 0.8), (1.6, 7.0, Z + 0.8), "walnut", rot=("z", -22.5, (0.5, 7.0, Z)))
    m.checkering(-0.3, 1.3, 4.6, 6.4, Z - 0.8, Z + 0.8, tex="wood_check")
    m.box((-14.3, 3.7, Z - 1.05), (-13.8, 9.3, Z + 1.05), "rubber")
    m.stock_rear = -14.3


def machine_gun(m, kind="pkm"):
    z0, z1 = zs(2.0)
    tan = kind == "m249"
    rec = "alu" if tan else "steel"
    # Receiver, the feed cover on top with its hinge and latch, the feed tray and the belt going in on the left
    m.box((-1.0, 6.8, z0), (13.6, 10.0, z1), rec)
    m.box((1.2, 10.0, Z - 1.05), (11.6, 10.9, Z + 1.05), rec)
    m.cyl("z", Z - 1.1, Z + 1.1, 1.2, 10.4, 0.45, "steel_dark", sides=8)
    m.box((11.0, 10.2, Z - 0.6), (12.0, 11.1, Z + 0.6), "steel_dark")
    if tan:
        m.rail(2.0, 11.2, 10.9, Z, w=1.5)
    for k in range(7):                                                                   # the belt of rounds feeding in
        x = 4.4 + k * 0.9
        m.box((x, 8.4, z0 - 2.6), (x + 0.5, 9.0, z0 + 0.2), "brass")
        m.box((x - 0.05, 8.5, z0 - 2.4), (x + 0.55, 8.9, z0 - 2.0), "steel_dark")
    m.box((4.2, 8.8, z1), (9.0, 9.8, z1 + 0.04), "bore", faces=["south"])
    m.box((10.6, 8.4, z1), (11.6, 9.0, z1 + 1.4), "steel_light")                         # charging handle
    # Ammo box hanging under, on the left
    if tan:
        m.box((4.0, 1.2, Z - 2.2), (10.6, 6.8, Z + 1.0), "polymer_od")                  # soft pouch
        m.box((4.4, 2.0, Z + 1.0), (10.2, 6.0, Z + 1.06), "alu_od", faces=["south"])
    else:
        m.box((4.2, 1.6, Z - 2.4), (10.4, 6.8, Z + 0.8), "steel_dark")
        m.box((4.6, 6.8, Z - 2.0), (10.0, 7.2, Z + 0.4), "steel_dark")
        m.box((6.6, 1.0, Z - 0.6), (8.0, 1.6, Z + 0.2), "steel_light")                  # handle
    # Trigger, grip, stock
    m.trigger_guard(3.0, 6.6, 6.8, 1.7, "steel_dark", t=0.45)
    m.grip(1.2, 6.9, 2.2, 4.6, Z - 0.8, Z + 0.8, "polymer" if tan else "birch", angle=-22.5, check_tex="grip_check" if tan else "wood_check")
    if tan:
        m.box((-11.6, 8.4, Z - 0.8), (-1.0, 9.8, Z + 0.8), "polymer")
        m.box((-11.6, 4.8, Z - 0.8), (-10.4, 9.8, Z + 0.8), "polymer")
        m.profile([(-10.4, 4.8, 5.8), (-1.0, 6.6, 7.4)], 1.5, "polymer")
        m.box((-12.1, 4.6, Z - 1.0), (-11.6, 10.0, Z + 1.0), "rubber")
        m.stock_rear = -12.1
    else:
        # The PKM's skeleton stock with its big thumbhole
        m.box((-12.4, 8.6, Z - 0.85), (-1.0, 9.8, Z + 0.85), "polymer")
        m.box((-12.4, 3.8, Z - 0.9), (-10.8, 9.8, Z + 0.9), "polymer")
        m.profile([(-10.8, 3.8, 5.0), (-6.0, 4.6, 5.8), (-1.0, 6.4, 7.4)], 1.7, "polymer")
        m.box((-12.9, 3.6, Z - 1.0), (-12.4, 10.0, Z + 1.0), "steel_dark")
        m.stock_rear = -12.9
    # Barrel: heavy, fluted, with the carry handle; the gas tube under it, the bipod, the flash hider, sights
    m.tube(13.6, 29.2, 9.0, Z, 0.68, "steel_dark", sides=16)
    for k in range(8):
        m.box((14.4 + k * 1.2, 9.65, Z - 0.12), (15.2 + k * 1.2, 9.72, Z + 0.12), "bore", faces=["up"])
    m.cyl("x", 13.6, 22.6, 7.7, Z, 0.42, "steel", sides=8)
    m.box((14.4, 9.6, Z - 0.25), (14.9, 11.8, Z + 0.25), "steel_dark")                   # carry handle
    m.box((14.4, 11.6, Z - 0.3), (18.6, 12.1, Z + 0.3), "birch" if not tan else "polymer")
    m.box((18.1, 9.6, Z - 0.25), (18.6, 11.8, Z + 0.25), "steel_dark")
    if tan:
        m.box((13.6, 9.6, Z - 1.1), (19.0, 10.8, Z + 1.1), "steel", faces=["up", "north", "south"])   # heat shield
        m.box((13.6, 6.4, Z - 1.3), (17.6, 8.4, Z + 1.3), "polymer")                    # handguard
    m.box((22.0, 7.2, Z - 0.6), (23.2, 9.8, Z + 0.6), "steel_dark")
    for s in (-1, 1):
        m.box((22.4, 2.0, Z + s * 0.9 - 0.22), (22.9, 7.2, Z + s * 0.9 + 0.22), "steel_dark", rot=("x", 22.5 * s, (22.6, 7.2, Z)))
        m.box((22.2, 1.6, Z + s * 3.0 - 0.3), (23.1, 2.1, Z + s * 3.0 + 0.3), "steel_dark")
    m.cyl("x", 29.2, 31.2, 9.0, Z, 0.75, "steel_dark", sides=16, ends=False)
    m.box((31.19, 8.5, Z - 0.5), (31.21, 9.5, Z + 0.5), "bore", faces=["east"], full=True)
    for k in range(4):
        m.box((29.5 + k * 0.45, 9.55, Z - 0.2), (29.7 + k * 0.45, 9.76, Z + 0.2), "bore", faces=["up"])
    m.box((28.0, 9.6, Z - 0.6), (28.8, 11.6, Z - 0.42), "steel_dark")
    m.box((28.0, 9.6, Z + 0.42), (28.8, 11.6, Z + 0.6), "steel_dark")
    m.box((28.3, 9.6, Z - 0.1), (28.5, 11.3, Z + 0.1), "steel")
    m.box((9.0, 10.9, Z - 0.6), (10.6, 11.6, Z + 0.6), "steel_dark")                      # rear sight
    m.box((9.4, 11.6, Z - 0.6), (10.2, 12.1, Z - 0.2), "steel_dark")
    m.box((9.4, 11.6, Z + 0.2), (10.2, 12.1, Z + 0.6), "steel_dark")
    m.sight = (28.4, 11.25 if not tan else 11.6)


def pkm(m):
    machine_gun(m, "pkm")


def m249(m):
    machine_gun(m, "m249")


ALL = {
    "p17": (p17, 0.72, 0.30),
    "m1911": (m1911, 0.72, 0.30),
    "hawk50": (hawk50, 0.7, 0.30),
    "r357": (r357, 0.72, 0.30),
    "mp5": (mp5, 0.62, 0.10),
    "mp7": (mp7, 0.66, 0.10),
    "uz9": (uz9, 0.68, 0.24),
    "kv45": (kv45, 0.64, 0.10),
    "m4a1": (m4a1, 0.6, 0.10),
    "mk17": (mk17, 0.58, 0.10),
    "aug": (aug, 0.6, 0.10),
    "svd": (svd, 0.56, 0.10),
    "sr25": (sr25, 0.56, 0.10),
    "m700": (m700, 0.56, 0.10),
    "mosin": (mosin, 0.55, 0.10),
    "ax338": (ax338, 0.55, 0.10),
    "double_barrel": (double_barrel, 0.56, 0.10),
    "pkm": (pkm, 0.56, 0.10),
    "m249": (m249, 0.56, 0.10),
    "ak74": (ak74, 0.6, 0.10),
    "akm": (akm, 0.6, 0.10),
    "saiga12": (saiga12, 0.6, 0.10),
}
