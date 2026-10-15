#!/usr/bin/env python3
"""Skins, loot tables, spawns, spawn-egg models and names for the Factory Ascension mod's 20 mobs.

Each mob reuses a vanilla model (tools/mob_models.py) with its own skin, painted here box by box: a base color per
part, a surface pattern (rust, crystal, ember cracks, frost, glowing veins...), lit from above like the pack's other
textures, and a face on the head. The mobs' behavior lives in the mod (dev.factoryascension.mob.FAMobs).
Run from the repo root:  python3 tools/mob_art.py
"""
import json
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))
import make_textures  # noqa: E402
from mob_models import MODELS, faces  # noqa: E402

RES = os.path.join(os.path.dirname(__file__), "..", "mod", "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", "factoryascension")
DATA = os.path.join(RES, "data", "factoryascension")

# id: model, name, colors per part (anything unlisted uses 'base'), pattern, eyes, egg colors, spawns, loot.
# spawn: [biome tag, weight, min, max]; loot: [item, min, max] rolls plus optional chance.
MOBS = {
    'rust_walker': dict(model='humanoid', name='Rust Walker', base=(118, 96, 74), head=(126, 140, 96), body=(150, 82, 40), pattern='rust', eyes=(255, 120, 40),
                        egg=(0x7A5230, 0xC86A28), spawn=['#minecraft:is_overworld', 40, 1, 3],
                        loot=[['minecraft:rotten_flesh', 0, 2], ['minecraft:iron_nugget', 1, 4], ['create:copper_nugget', 0, 3]]),
    'ascendite_husk': dict(model='humanoid', name='Ascendite Husk', base=(60, 74, 86), head=(70, 96, 104), body=(40, 52, 64), pattern='glow', glow=(64, 232, 212),
                           eyes=(120, 255, 240), egg=(0x2E3A44, 0x40E8D4), spawn=['#minecraft:is_overworld', 20, 1, 2],
                           loot=[['minecraft:rotten_flesh', 0, 2], ['kubejs:ascension_shard', 1, 2], ['kubejs:ascendite_nugget', 0, 4]]),
    'ember_ghoul': dict(model='humanoid', name='Ember Ghoul', base=(70, 40, 34), head=(84, 46, 36), body=(54, 30, 26), pattern='ember', glow=(255, 150, 40),
                        eyes=(255, 220, 80), egg=(0x3A2018, 0xFF9628), spawn=['#minecraft:is_nether', 30, 1, 3],
                        loot=[['minecraft:rotten_flesh', 0, 2], ['minecraft:blaze_powder', 1, 2], ['minecraft:magma_cream', 0, 1]]),
    'frost_revenant': dict(model='humanoid', name='Frost Revenant', base=(150, 186, 210), head=(176, 206, 226), body=(90, 120, 160), pattern='frost',
                           eyes=(200, 250, 255), egg=(0x96BAD2, 0xEAF6FF), spawn=['#forge:is_snowy', 50, 1, 3],
                           loot=[['minecraft:rotten_flesh', 0, 2], ['minecraft:packed_ice', 0, 2], ['minecraft:snowball', 1, 4]]),
    'deep_miner': dict(model='humanoid', name='Deep Miner', base=(96, 110, 80), head=(104, 126, 88), body=(70, 70, 90), pattern='dirt', eyes=(250, 230, 120),
                       helmet=(230, 190, 60), egg=(0x464658, 0xE6BE3C), spawn=['#minecraft:is_overworld', 45, 1, 2],
                       loot=[['minecraft:rotten_flesh', 0, 1], ['minecraft:raw_iron', 0, 2], ['minecraft:raw_copper', 0, 3], ['minecraft:coal', 1, 3]]),
    'void_archer': dict(model='skeleton', name='Void Archer', base=(52, 40, 70), pattern='veins', glow=(190, 100, 255), eyes=(220, 140, 255),
                        egg=(0x342846, 0xBE64FF), spawn=['#minecraft:is_end', 20, 1, 2],
                        loot=[['minecraft:bone', 0, 2], ['minecraft:arrow', 0, 3], ['minecraft:ender_pearl', 0, 1, 0.3]]),
    'ascendite_skeleton': dict(model='skeleton', name='Ascendite Skeleton', base=(200, 214, 210), pattern='glow', glow=(64, 232, 212), eyes=(64, 232, 212),
                               egg=(0xC8D6D2, 0x40E8D4), spawn=['#minecraft:is_overworld', 25, 1, 2],
                               loot=[['minecraft:bone', 0, 2], ['minecraft:arrow', 0, 2], ['kubejs:ascendite_nugget', 1, 4]]),
    'crystal_spider': dict(model='spider', name='Crystal Spider', base=(110, 70, 160), body=(130, 86, 190), pattern='crystal', eyes=(255, 190, 255),
                           egg=(0x6E46A0, 0xFFBEFF), spawn=['#minecraft:is_overworld', 30, 1, 2],
                           loot=[['minecraft:string', 0, 2], ['minecraft:amethyst_shard', 1, 3]]),
    'spark_spider': dict(model='spider', name='Spark Spider', base=(56, 56, 64), body=(70, 70, 80), pattern='veins', glow=(255, 230, 80), eyes=(255, 240, 120),
                         egg=(0x383840, 0xFFE650), spawn=['#minecraft:is_overworld', 30, 1, 2],
                         loot=[['minecraft:string', 0, 2], ['minecraft:redstone', 1, 4], ['minecraft:glowstone_dust', 0, 2]]),
    'cinder_spider': dict(model='spider', name='Cinder Spider', base=(60, 34, 30), body=(80, 40, 30), pattern='ember', glow=(255, 120, 30), eyes=(255, 200, 60),
                          egg=(0x3C221E, 0xFF781E), spawn=['#minecraft:is_nether', 30, 1, 2],
                          loot=[['minecraft:string', 0, 2], ['minecraft:blaze_powder', 0, 2], ['minecraft:fire_charge', 0, 1, 0.4]]),
    'ascendite_creeper': dict(model='creeper', name='Ascendite Creeper', base=(46, 104, 104), pattern='glow', glow=(64, 232, 212), eyes=(10, 30, 34),
                              egg=(0x2E6868, 0x40E8D4), spawn=['#minecraft:is_overworld', 25, 1, 1],
                              loot=[['minecraft:gunpowder', 0, 2], ['kubejs:ascension_shard', 0, 2]]),
    'frost_creeper': dict(model='creeper', name='Frost Creeper', base=(170, 210, 230), pattern='frost', eyes=(30, 60, 90),
                          egg=(0xAAD2E6, 0x1E3C5A), spawn=['#forge:is_snowy', 40, 1, 2],
                          loot=[['minecraft:gunpowder', 0, 2], ['minecraft:snowball', 1, 4], ['minecraft:blue_ice', 0, 1, 0.2]]),
    'essence_slime': dict(model='slime', name='Essence Slime', base=(170, 96, 255), inner=(120, 50, 200), pattern='plain', eyes=(250, 230, 255),
                          egg=(0xAA60FF, 0xF0DCFF), spawn=['#minecraft:is_overworld', 15, 1, 2],
                          loot=[['minecraft:slime_ball', 0, 2], ['kubejs:ascension_shard', 0, 1, 0.5]]),
    'rust_slime': dict(model='slime', name='Rust Slime', base=(90, 170, 130), inner=(170, 100, 50), pattern='rust', eyes=(40, 30, 20),
                       egg=(0x5AAA82, 0xAA6432), spawn=['#minecraft:is_overworld', 20, 1, 2],
                       loot=[['minecraft:slime_ball', 0, 2], ['create:copper_nugget', 1, 3]]),
    'ore_mite': dict(model='silverfish', name='Ore Mite', base=(120, 120, 126), pattern='ore', glow=(230, 190, 60), eyes=(40, 40, 40),
                     egg=(0x78787E, 0xE6BE3C), spawn=['#minecraft:is_overworld', 30, 2, 4],
                     loot=[['minecraft:iron_nugget', 0, 3], ['minecraft:gold_nugget', 0, 2], ['kubejs:ascendite_nugget', 0, 1, 0.5]]),
    'teal_wisp': dict(model='blaze', name='Teal Wisp', base=(40, 160, 160), rod=(120, 240, 230), pattern='glow', glow=(200, 255, 250), eyes=(255, 255, 255),
                      egg=(0x28A0A0, 0xC8FFFA), spawn=['#minecraft:is_nether', 15, 1, 2],
                      loot=[['kubejs:ascension_shard', 0, 2], ['minecraft:glowstone_dust', 0, 3]]),
    'void_stalker': dict(model='enderman', name='Void Stalker', base=(26, 18, 36), pattern='veins', glow=(150, 60, 220), eyes=(230, 120, 255),
                         egg=(0x1A1224, 0x963CDC), spawn=['#minecraft:is_end', 15, 1, 1],
                         loot=[['minecraft:ender_pearl', 0, 2], ['minecraft:chorus_fruit', 0, 2]]),
    'factory_raider': dict(model='illager', name='Factory Raider', base=(140, 140, 136), head=(150, 150, 146), body=(150, 110, 50), robe=(120, 90, 40),
                           pattern='stripes', eyes=(40, 120, 60), egg=(0x8C8C88, 0x966E32), spawn=['#minecraft:is_forest', 8, 1, 2],
                           loot=[['minecraft:emerald', 0, 1], ['create:brass_nugget', 1, 4], ['create:andesite_alloy', 0, 2]]),
    'ember_hen': dict(model='chicken', name='Ember Hen', base=(230, 120, 40), head=(240, 140, 50), beak=(250, 210, 80), wattle=(255, 60, 30), leg=(250, 200, 60),
                      pattern='ember', glow=(255, 220, 90), eyes=(20, 10, 10), egg=(0xE67828, 0xFFDC5A), spawn=['#minecraft:is_savanna', 10, 2, 4],
                      loot=[['minecraft:feather', 0, 2], ['minecraft:blaze_powder', 0, 1, 0.5]]),
    'copper_cow': dict(model='cow', name='Copper Cow', base=(180, 100, 60), head=(190, 110, 70), horn=(230, 220, 200), udder=(240, 170, 160), pattern='patina',
                       glow=(80, 170, 130), eyes=(20, 20, 20), egg=(0xB4643C, 0x50AA82), spawn=['#minecraft:is_overworld', 6, 2, 3],
                       loot=[['minecraft:leather', 0, 2], ['minecraft:copper_ingot', 0, 2]]),
    # ---- 1.6: more mobs ----
    'bog_lurker': dict(model='humanoid', name='Bog Lurker', base=(70, 86, 50), head=(84, 104, 60), body=(56, 64, 40), pattern='dirt', eyes=(200, 230, 90),
                       egg=(0x3E4A2A, 0x7C9A3C), spawn=['#forge:is_swamp', 40, 1, 3],
                       loot=[['minecraft:rotten_flesh', 0, 2], ['minecraft:slime_ball', 0, 2], ['minecraft:lily_pad', 0, 1]]),
    'sand_wraith': dict(model='humanoid', name='Sand Wraith', base=(200, 176, 120), head=(214, 190, 134), body=(150, 120, 70), pattern='dirt', eyes=(40, 30, 20),
                        egg=(0xC8B078, 0x6E5A32), spawn=['#forge:is_desert', 40, 1, 2],
                        loot=[['minecraft:rotten_flesh', 0, 2], ['minecraft:sand', 1, 4], ['minecraft:gold_nugget', 0, 3]]),
    'thunder_creeper': dict(model='creeper', name='Thunder Creeper', base=(56, 56, 72), pattern='veins', glow=(240, 224, 80), eyes=(240, 224, 80),
                            egg=(0x383848, 0xF0E050), spawn=['#minecraft:is_overworld', 15, 1, 1],
                            loot=[['minecraft:gunpowder', 1, 3], ['minecraft:redstone', 1, 4]]),
    'brass_sentinel': dict(model='skeleton', name='Brass Sentinel', base=(180, 140, 60), pattern='stripes', eyes=(255, 120, 40),
                           egg=(0xB48C3C, 0x5A4628), spawn=['#minecraft:is_overworld', 15, 1, 2],
                           loot=[['minecraft:bone', 0, 2], ['minecraft:arrow', 1, 4], ['create:brass_nugget', 1, 4]]),
    'shadow_skeleton': dict(model='skeleton', name='Shadow Skeleton', base=(30, 30, 38), pattern='veins', glow=(140, 80, 220), eyes=(200, 140, 255),
                            egg=(0x1C1C24, 0x8C50DC), spawn=['#minecraft:is_forest', 25, 1, 2],
                            loot=[['minecraft:bone', 0, 2], ['minecraft:coal', 0, 2], ['minecraft:echo_shard', 0, 1, 0.1]]),
    'cave_weaver': dict(model='spider', name='Cave Weaver', base=(140, 140, 150), body=(170, 170, 180), pattern='stripes', eyes=(255, 255, 255),
                        egg=(0x8C8C96, 0xE6E6F0), spawn=['#minecraft:is_overworld', 25, 1, 2],
                        loot=[['minecraft:string', 2, 5], ['minecraft:cobweb', 0, 1, 0.3]]),
    'moss_slime': dict(model='slime', name='Moss Slime', base=(78, 140, 50), inner=(120, 90, 50), pattern='dirt', eyes=(250, 230, 120),
                       egg=(0x4E8C32, 0xA0D25A), spawn=['minecraft:lush_caves', 40, 1, 2],
                       loot=[['minecraft:slime_ball', 0, 2], ['minecraft:moss_block', 0, 1], ['minecraft:glow_berries', 0, 2]]),
    'glow_mite': dict(model='silverfish', name='Glow Mite', base=(110, 100, 80), pattern='ore', glow=(255, 220, 90), eyes=(255, 240, 160),
                      egg=(0x6E6450, 0xFFDC5A), spawn=['#minecraft:is_overworld', 25, 2, 4],
                      loot=[['minecraft:glowstone_dust', 1, 3]]),
    'storm_wisp': dict(model='blaze', name='Storm Wisp', base=(200, 200, 220), rod=(255, 240, 100), pattern='veins', glow=(255, 250, 160), eyes=(255, 255, 255),
                       egg=(0xD2D2E6, 0xFFF064), spawn=['#minecraft:is_mountain', 15, 1, 1],
                       loot=[['minecraft:glowstone_dust', 1, 3], ['minecraft:redstone', 1, 3], ['kubejs:ascension_shard', 0, 1, 0.5]]),
    'treasure_raider': dict(model='illager', name='Treasure Raider', base=(140, 140, 136), head=(150, 150, 146), body=(200, 160, 50), robe=(60, 140, 80),
                            pattern='stripes', eyes=(255, 210, 60), egg=(0xC8A032, 0x32A050), spawn=['#minecraft:is_overworld', 3, 1, 1],
                            loot=[['minecraft:emerald', 1, 4], ['minecraft:gold_ingot', 1, 3], ['kubejs:enchantment_pack', 0, 1, 0.5], ['kubejs:gear_cache', 0, 1, 0.3]]),
    # ---- Bosses (scaled up in game; a crown on the ones with heads) ----
    'rust_colossus': dict(model='humanoid', name='Rust Colossus', base=(110, 76, 50), head=(126, 92, 60), body=(150, 82, 40), pattern='rust', eyes=(255, 110, 30),
                          crown=(232, 176, 58), egg=(0x5A3C28, 0xFF6A1E), spawn=['#minecraft:is_overworld', 1, 1, 1],
                          loot=[['kubejs:ascended_soul', 1, 1], ['kubejs:mythic_fragment', 2, 4], ['kubejs:dense_ascension_core', 2, 5], ['minecraft:iron_block', 2, 4],
                                ['kubejs:booster_box', 1, 1], ['kubejs:gear_cache', 1, 3]]),
    'broodmother': dict(model='spider', name='Broodmother', base=(70, 40, 90), body=(96, 56, 120), pattern='crystal', eyes=(255, 120, 255),
                        egg=(0x46285A, 0xDC64FF), spawn=['#minecraft:is_forest', 1, 1, 1],
                        loot=[['kubejs:ascended_soul', 1, 1], ['kubejs:mythic_fragment', 2, 4], ['kubejs:dense_ascension_core', 2, 5], ['minecraft:string', 8, 16],
                              ['minecraft:amethyst_block', 1, 3], ['kubejs:booster_box', 1, 1], ['kubejs:gear_cache', 1, 3]]),
    'hollow_king': dict(model='skeleton', name='Hollow King', base=(220, 220, 206), pattern='veins', glow=(140, 80, 220), eyes=(200, 140, 255),
                        crown=(232, 176, 58), egg=(0xE6E6D2, 0x8C50DC), spawn=['#minecraft:is_overworld', 1, 1, 1],
                        loot=[['kubejs:ascended_soul', 1, 1], ['kubejs:mythic_fragment', 2, 4], ['kubejs:dense_ascension_core', 2, 5], ['minecraft:bone_block', 2, 4],
                              ['minecraft:wither_skeleton_skull', 0, 1, 0.5], ['kubejs:booster_box', 1, 1], ['kubejs:gear_cache', 1, 3]]),
    'wisp_tyrant': dict(model='blaze', name='Wisp Tyrant', base=(20, 120, 120), rod=(64, 232, 212), pattern='glow', glow=(220, 255, 250), eyes=(255, 255, 255),
                        egg=(0x147878, 0x40E8D4), spawn=['#minecraft:is_nether', 1, 1, 1],
                        loot=[['kubejs:ascended_soul', 1, 1], ['kubejs:mythic_fragment', 2, 4], ['kubejs:dense_ascension_core', 2, 5], ['minecraft:blaze_rod', 4, 8],
                              ['kubejs:ascension_shard', 4, 8], ['kubejs:booster_box', 1, 1], ['kubejs:gear_cache', 1, 3]]),
}


def grain(seed, x, y):
    h = (x * 374761393 + y * 668265263 + seed * 2246822519) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFFFF) / 32767.5 - 1


def mul(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


SHADE = {'top': 1.15, 'bottom': 0.7, 'front': 1.0, 'back': 0.84, 'left': 0.9, 'right': 0.92}


def surface(spec, base, face, x, y, fx, fy, fw, fh, seed):
    """One pixel of a face: base color, the mob's pattern, lighting, a darker rim."""
    p = spec['pattern']
    glow = spec.get('glow', (255, 255, 255))
    c = mul(base, 1 + 0.07 * grain(seed, x, y))
    n = grain(seed + 17, x // 2, y // 2)
    if p == 'rust' and n > 0.35:
        c = mix(c, (170, 80, 30), 0.6)
    elif p == 'crystal' and (x + y) % 5 == 0:
        c = mix(c, (255, 230, 255), 0.45)
    elif p == 'ember' and abs(grain(seed + 3, x, y // 2)) < 0.12:
        c = glow
    elif p == 'frost' and grain(seed + 5, x, y) > 0.55:
        c = mix(c, (255, 255, 255), 0.7)
    elif p == 'glow' and (fx == fw // 2 or abs(grain(seed + 9, x, y)) < 0.06):
        c = glow
    elif p == 'veins' and abs(grain(seed + 11, x // 1, y // 3)) < 0.09:
        c = glow
    elif p == 'dirt' and n > 0.5:
        c = mul(c, 0.75)
    elif p == 'stripes' and fy % 4 == 0:
        c = mul(c, 0.8)
    elif p == 'ore' and grain(seed + 13, x, y) > 0.7:
        c = glow
    elif p == 'patina' and n > 0.3:
        c = mix(c, glow, 0.7)
    c = mul(c, SHADE[face])
    if fx == 0 or fy == 0 or fx == fw - 1 or fy == fh - 1:
        c = mul(c, 0.86)
    return c + (255,)


def face_features(px, spec, model, part, fx0, fy0, fw, fh):
    """Eyes and mouths on the head's front face (and the slime's eye boxes)."""
    eye = spec['eyes']

    def put(x, y, c):
        px[fy0 + y][fx0 + x] = tuple(c) + (255,)
    if model in ('humanoid', 'skeleton') and part == 'head':
        for x in (1, 2, 5, 6):
            put(x, 4, eye)
        put(2, 4, mix(eye, (255, 255, 255), 0.5))
        put(5, 4, mix(eye, (255, 255, 255), 0.5))
        for x in range(2, 6):
            put(x, 6, mul(spec.get('head', spec['base']), 0.45))
        if spec.get('helmet'):
            for y in range(0, 3):
                for x in range(fw):
                    put(x, y, mul(spec['helmet'], 0.9 + 0.1 * (y == 0)))
            put(3, 1, (255, 255, 200))
            put(4, 1, (255, 255, 200))
    elif model == 'creeper' and part == 'head':
        for x, y in ((1, 2), (2, 2), (1, 3), (2, 3), (5, 2), (6, 2), (5, 3), (6, 3), (3, 4), (4, 4), (2, 5), (3, 5), (4, 5), (5, 5), (2, 6), (5, 6)):
            put(x, y, eye)
    elif model == 'spider' and part == 'head':
        for x, y in ((1, 3), (2, 3), (5, 3), (6, 3), (0, 4), (3, 4), (4, 4), (7, 4)):
            put(x, y, eye)
    elif model == 'blaze' and part == 'head':
        for x in (1, 2, 5, 6):
            put(x, 4, eye)
    elif model == 'enderman' and part == 'head':
        for x in (0, 1, 2, 5, 6, 7):
            put(x, 4, eye if x in (1, 6) else mix(eye, spec['base'], 0.4))
    elif model == 'illager' and part == 'head':
        for x in (1, 2, 5, 6):
            put(x, 5, (240, 240, 240) if x in (1, 6) else eye)
        for x in range(1, 7):
            put(x, 4, mul(spec['head'], 0.5))
    elif model == 'chicken' and part == 'head':
        put(0, 1, eye)
        put(3, 1, eye)
    elif model == 'cow' and part == 'head':
        put(1, 3, eye)
        put(6, 3, eye)
        for y in range(5, 8):
            for x in range(2, 6):
                put(x, y, mix(spec['head'], (240, 200, 180), 0.45))
        put(2, 6, (40, 30, 30))
        put(5, 6, (40, 30, 30))
    elif model == 'slime' and part in ('reye', 'leye'):
        for y in range(fh):
            for x in range(fw):
                put(x, y, eye)
    elif model == 'silverfish' and part == 's0':
        put(0, 0, eye)
        put(fw - 1, 0, eye)


def skin(mob_id, spec):
    W, H, boxes = MODELS[spec['model']]
    px = [[(0, 0, 0, 0)] * W for _ in range(H)]
    seed = sum(ord(ch) * (i + 1) for i, ch in enumerate(mob_id))
    for part, u, v, w, h, d in boxes:
        if part in ('hat', 'jaw'):
            continue
        key = {'rarm': 'arm', 'larm': 'arm', 'rleg': 'leg', 'lleg': 'leg', 'reye': 'eye', 'leye': 'eye', 'neck': 'head'}.get(part, part)
        base = spec.get(part, spec.get(key, spec['base']))
        if spec['model'] == 'humanoid' and part in ('arm', 'leg'):
            base = spec.get('body', spec['base']) if part == 'leg' else spec.get('head', spec['base'])
        if spec['model'] == 'slime' and part == 'outer':
            base = spec['base']
        for face, (fx0, fy0, fw, fh) in faces(u, v, w, h, d).items():
            for yy in range(fh):
                for xx in range(fw):
                    x, y = fx0 + xx, fy0 + yy
                    if 0 <= x < W and 0 <= y < H:
                        c = surface(spec, base, face, x, y, xx, yy, fw, fh, seed)
                        if spec['model'] == 'slime' and part == 'outer':
                            c = c[:3] + (170,)        # the slime's see-through jelly
                        px[y][x] = c
            if face == 'front' or (spec['model'] == 'slime' and part in ('reye', 'leye')):
                face_features(px, spec, spec['model'], part, fx0, fy0, fw, fh)
        # Bosses with a head wear a gold crown: the top of the head and a band around its top two rows, with gems
        if spec.get('crown') and part == 'head':
            for face, (fx0, fy0, fw, fh) in faces(u, v, w, h, d).items():
                rows = range(fh) if face == 'top' else range(2) if face != 'bottom' else range(0)
                for yy in rows:
                    for xx in range(fw):
                        if face == 'top' and 1 < xx < fw - 2 and 1 < yy < fh - 2:
                            continue
                        c = mul(spec['crown'], 1.15 if yy == 0 else 0.85)
                        if face != 'top' and yy == 0 and xx % 2:
                            c = (0, 0, 0, 0)[:3]    # points of the crown
                            px[fy0 + yy][fx0 + xx] = (0, 0, 0, 0)
                            continue
                        px[fy0 + yy][fx0 + xx] = c + (255,)
                if face == 'front':
                    px[fy0 + 1][fx0 + fw // 2] = (220, 40, 60, 255)
                    px[fy0 + 1][fx0 + fw // 2 - 1] = (255, 120, 140, 255)
        # Humanoid arms: sleeves on the top third
        if spec['model'] == 'humanoid' and part == 'arm':
            for face, (fx0, fy0, fw, fh) in faces(u, v, w, h, d).items():
                if face in ('top', 'bottom'):
                    continue
                for yy in range(4):
                    for xx in range(fw):
                        px[fy0 + yy][fx0 + xx] = surface(spec, spec.get('body', spec['base']), face, fx0 + xx, fy0 + yy, xx, yy, fw, fh, seed)
    return px


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def loot(spec):
    pools = []
    for entry in spec['loot']:
        item, lo, hi = entry[0], entry[1], entry[2]
        conditions = [{"condition": "minecraft:killed_by_player"}] if len(entry) > 3 else []
        if len(entry) > 3:
            conditions.append({"condition": "minecraft:random_chance_with_looting", "chance": entry[3], "looting_multiplier": 0.05})
        e = {"type": "minecraft:item", "name": item, "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}},
            {"function": "minecraft:looting_enchant", "count": {"type": "minecraft:uniform", "min": 0, "max": 1}}]}
        # Items from mods that aren't installed are skipped instead of breaking the table
        mod = item.split(':')[0]
        if mod not in ('minecraft', 'kubejs', 'factoryascension'):
            e["conditions"] = [{"condition": "forge:mod_loaded", "modid": mod}]
        pool = {"rolls": 1, "entries": [e]}
        if conditions:
            pool["conditions"] = conditions
        pools.append(pool)
    return {"type": "minecraft:entity", "pools": pools}


DESC = {'bog_lurker': ('Swamps', 'Drags itself out of the mud; poisons what it touches. Drops slime balls and lily pads.'), 'sand_wraith': ('Deserts', "Doesn't burn in the sun; blinds you. Drops sand and gold nuggets."), 'thunder_creeper': ('Overworld', 'Its blast breaks no blocks: it calls lightning and jolts everything nearby.'), 'brass_sentinel': ('Overworld, danger 4+', 'Armored in brass; its arrows slow you. Drops brass nuggets.'), 'shadow_skeleton': ('Forests, danger 3+', 'Arrows that bring Darkness. Sometimes drops an echo shard.'), 'cave_weaver': ('Caves below Y 30', 'Leaves cobwebs at your feet when it bites. Drops lots of string.'), 'moss_slime': ('Lush caves', 'A mossy slime. Drops moss and glow berries.'), 'glow_mite': ('Below Y 40', 'A glowing silverfish; makes you glow. Drops glowstone dust.'), 'storm_wisp': ('Mountains, danger 6+', 'A pale blaze that strikes with lightning. Drops redstone, glowstone and shards.'), 'treasure_raider': ('Overworld, danger 3+ (rare)', 'A raider loaded with loot: emeralds, gold, packs and gear caches.'), 'rust_colossus': ('BOSS · Overworld, danger 8+', 'A towering rusted giant. Slams the ground, calls Rust Walkers and Deep Miners, enrages at 1/4 health.'), 'broodmother': ('BOSS · Forests, danger 8+', 'A spider the size of a house. Poisons, webs, slams, and calls her brood.'), 'hollow_king': ('BOSS · Overworld, danger 12+', 'A crowned skeleton king. Wither arrows in volleys of five; calls Shadow and Ascendite Skeletons.'), 'wisp_tyrant': ('BOSS · The Nether, danger 10+', 'A giant teal blaze. Rings of fireballs; calls Teal and Storm Wisps.'), 'rust_walker': ('Overworld, past the safe zone', 'A corroded factory hand. Its hits leave you weak; drops iron and copper nuggets.'), 'ascendite_husk': ('Overworld, danger 3+', "Glows teal, doesn't burn in daylight, makes you glow. Drops Ascension Shards."), 'ember_ghoul': ('The Nether', 'Fireproof, sets you alight. Drops blaze powder and magma cream.'), 'frost_revenant': ('Snowy biomes', 'Slows you to a crawl. Drops packed ice and snowballs.'), 'deep_miner': ('Below Y 0', 'Swings an iron pickaxe and gives you Mining Fatigue. Drops raw ore and coal.'), 'void_archer': ('The End', 'Its arrows make you float. Drops ender pearls.'), 'ascendite_skeleton': ('Overworld, danger 3+', 'Arrows that make you glow. Drops Ascendite nuggets.'), 'crystal_spider': ('Caves below Y 40', 'Armored amethyst; slows you. Drops amethyst shards.'), 'spark_spider': ('Overworld, danger 2+', 'Crackles with lightning. Drops redstone and glowstone.'), 'cinder_spider': ('The Nether', 'Fireproof, sets you alight. Drops blaze powder and fire charges.'), 'ascendite_creeper': ('Overworld, danger 3+', 'Its blast breaks no blocks and scatters Ascension Shards.'), 'frost_creeper': ('Snowy biomes', 'Its blast breaks no blocks: it leaves snow and slows everything nearby.'), 'essence_slime': ('Below Y 40, danger 5+', 'A purple slime that splits like any other. Drops Ascension Shards.'), 'rust_slime': ('Below Y 40', 'A copper-green slime. Drops copper nuggets.'), 'ore_mite': ('Below Y 20', 'A silverfish full of metal. Drops iron, gold and Ascendite nuggets.'), 'teal_wisp': ('The Nether', 'A teal blaze. Drops Ascension Shards and glowstone.'), 'void_stalker': ('The End', 'A huge, dark enderman. Blinds you. Drops ender pearls.'), 'factory_raider': ('Forests, danger 4+', 'A raider in work clothes; weakens you. Drops brass and andesite alloy.'), 'ember_hen': ('Savannas', 'Friendly and fireproof. Lays blaze powder now and then, as well as eggs.'), 'copper_cow': ('Overworld', 'Friendly. Moos in patina. Drops leather and copper ingots.')}


def site_data(path):
    """The mobs for the website's Bestiary (tools/build_site.sh)."""
    out = [{"id": k, "name": v["name"], "base": v["model"], "egg": ["#%06x" % v["egg"][0], "#%06x" % v["egg"][1]],
            "where": DESC[k][0], "what": DESC[k][1]} for k, v in MOBS.items()]
    with open(path, "w") as f:
        f.write("window.FA_MOBS = " + json.dumps(out) + ";\n")


def main():
    os.makedirs(os.path.join(ASSETS, "textures", "entity"), exist_ok=True)
    for mob_id, spec in MOBS.items():
        make_textures.png(os.path.join(ASSETS, "textures", "entity", mob_id + ".png"), skin(mob_id, spec))
        write_json(os.path.join(DATA, "loot_tables", "entities", mob_id + ".json"), loot(spec))
        tag, weight, lo, hi = spec['spawn']
        write_json(os.path.join(DATA, "forge", "biome_modifier", "spawn_" + mob_id + ".json"), {
            "type": "forge:add_spawns", "biomes": tag,
            "spawners": {"type": "factoryascension:" + mob_id, "weight": weight, "minCount": lo, "maxCount": hi}})
        write_json(os.path.join(ASSETS, "models", "item", mob_id + "_spawn_egg.json"), {"parent": "minecraft:item/template_spawn_egg"})
    lang_path = os.path.join(ASSETS, "lang", "en_us.json")
    lang = json.load(open(lang_path))
    for mob_id, spec in MOBS.items():
        lang["entity.factoryascension." + mob_id] = spec['name']
        lang["item.factoryascension." + mob_id + "_spawn_egg"] = spec['name'] + " Spawn Egg"
    write_json(lang_path, lang)
    # Egg colors for the Java side, so they're defined once
    with open(os.path.join(os.path.dirname(__file__), "..", "mod", "src", "main", "resources", "assets", "factoryascension", "mob_eggs.txt"), "w") as f:
        for mob_id, spec in MOBS.items():
            f.write("%s %06x %06x\n" % (mob_id, spec['egg'][0], spec['egg'][1]))
    print("wrote skins, loot tables, spawns, egg models and names for %d mobs" % len(MOBS))


if __name__ == "__main__":
    if len(sys.argv) > 2 and sys.argv[1] == "site":
        site_data(sys.argv[2])
    else:
        main()
