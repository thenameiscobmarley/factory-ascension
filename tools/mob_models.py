"""Texture layouts of the vanilla models the mod's mobs use (each mob reuses a vanilla model with its own skin).

A box is (name, u, v, width, height, depth) in texture pixels, unwrapped the standard Minecraft way:
top and bottom above, then right, front, left and back in a row. `optional` boxes are overlays a skin may leave empty.
"""

MODELS = {
    # Zombie-style humanoid (zombie, husk...), 64x64. The left arm and leg mirror the right ones' texture.
    'humanoid': (64, 64, [('head', 0, 0, 8, 8, 8), ('hat', 32, 0, 8, 8, 8), ('body', 16, 16, 8, 12, 4), ('arm', 40, 16, 4, 12, 4),
                          ('leg', 0, 16, 4, 12, 4)]),
    'skeleton': (64, 32, [('head', 0, 0, 8, 8, 8), ('hat', 32, 0, 8, 8, 8), ('body', 16, 16, 8, 12, 4), ('arm', 40, 16, 2, 12, 2), ('leg', 0, 16, 2, 12, 2)]),
    'spider': (64, 32, [('head', 32, 4, 8, 8, 8), ('neck', 0, 0, 6, 6, 6), ('body', 0, 12, 10, 8, 12), ('leg', 18, 0, 16, 2, 2)]),
    'creeper': (64, 32, [('head', 0, 0, 8, 8, 8), ('body', 16, 16, 8, 12, 4), ('leg', 0, 16, 4, 6, 4)]),
    'slime': (64, 32, [('outer', 0, 0, 8, 8, 8), ('inner', 0, 16, 6, 6, 6), ('reye', 32, 0, 2, 2, 2), ('leye', 32, 4, 2, 2, 2), ('mouth', 32, 8, 1, 1, 1)]),
    'blaze': (64, 32, [('head', 0, 0, 8, 8, 8), ('rod', 0, 16, 2, 8, 2)]),
    'enderman': (64, 32, [('head', 0, 0, 8, 8, 8), ('jaw', 0, 16, 8, 8, 8), ('body', 32, 16, 8, 12, 4), ('limb', 56, 0, 2, 30, 2)]),
    'illager': (64, 64, [('head', 0, 0, 8, 10, 8), ('nose', 24, 0, 2, 4, 2), ('body', 16, 20, 8, 12, 6), ('robe', 0, 38, 8, 20, 6),
                         ('arms', 44, 22, 4, 8, 4), ('armsmid', 40, 38, 8, 4, 4), ('leg', 0, 22, 4, 12, 4), ('arm', 40, 46, 4, 12, 4)]),
    'silverfish': (64, 32, [('s0', 0, 0, 3, 2, 2), ('s1', 0, 4, 4, 3, 2), ('s2', 0, 9, 6, 4, 3), ('s3', 0, 16, 3, 3, 3), ('s4', 0, 22, 2, 2, 3),
                            ('s5', 11, 0, 2, 1, 2), ('s6', 13, 4, 1, 1, 2), ('l0', 20, 0, 10, 8, 3), ('l1', 20, 11, 6, 4, 3), ('l2', 20, 18, 6, 5, 2)]),
    'chicken': (64, 32, [('head', 0, 0, 4, 6, 3), ('beak', 14, 0, 4, 2, 2), ('wattle', 14, 4, 2, 2, 2), ('body', 0, 9, 6, 8, 6),
                         ('leg', 26, 0, 3, 5, 3), ('wing', 24, 13, 1, 4, 6)]),
    'cow': (64, 32, [('head', 0, 0, 8, 8, 6), ('horn', 22, 0, 1, 3, 1), ('body', 18, 4, 12, 18, 10), ('udder', 52, 0, 4, 6, 1), ('leg', 0, 16, 4, 12, 4)]),
}
OPTIONAL = {'hat', 'jaw'}


def faces(u, v, w, h, d):
    """{face: (x, y, width, height)} of a box's unwrapped texture."""
    return {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, h), 'front': (u + d, v + d, w, h),
            'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}


def mask(model, skip_optional=True):
    W, H, boxes = MODELS[model]
    m = [[False] * W for _ in range(H)]
    for name, u, v, w, h, d in boxes:
        if skip_optional and name in OPTIONAL:
            continue
        for (x, y, fw, fh) in faces(u, v, w, h, d).values():
            for yy in range(y, min(H, y + fh)):
                for xx in range(x, min(W, x + fw)):
                    m[yy][xx] = True
    return m
