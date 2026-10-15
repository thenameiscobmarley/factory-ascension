#!/usr/bin/env python3
"""The Ascended Realm: a floating-islands dimension (Full download). Writes its blocks' textures, models, loot tables
and tags, and its worldgen: dimension type, generator (vanilla floating islands rebuilt from Realm Stone), three
biomes with their mobs, ores and crystal patches. The gate's rules (cost, platform, return trip) are in the scripts
(server_scripts/49_fa_realm.js, config: realm).
Run from the repo root:  python3 tools/realm_gen.py
"""
import json
import os
import sys
import zipfile

sys.path.insert(0, os.path.dirname(__file__))
import make_mod_assets as A  # noqa: E402
import make_textures  # noqa: E402

RES = A.RES
ASSETS, DATA = A.ASSETS, A.DATA
VANILLA = os.path.expanduser("~/fa-testserver/libraries/net/minecraft/server/1.20.1-20230612.114412/server-1.20.1-20230612.114412-extra.jar")
NS = "factoryascension"
S = A.S


def tex(name, px):
    make_textures.png(os.path.join(ASSETS, "textures", "block", name + ".png"), px)


def stone(seed=5):
    import random
    rnd = random.Random(seed)
    px = A.canvas()
    for y in range(S):
        for x in range(S):
            t = 0.35 + 0.25 * rnd.random() + 0.1 * ((x // 4 + y // 4) % 2)
            px[y][x] = A._mix(A._shade("steel", t), (150, 110, 200), 0.35) + (255,)
    return px


def ore(seed, mat, gem=False):
    import random
    rnd = random.Random(seed)
    px = stone(seed)
    for _ in range(9):
        cx, cy = rnd.randint(4, 27), rnd.randint(4, 27)
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1), (-1, 0), (0, -1)):
            px[cy + dy][cx + dx] = A._shade(mat, 0.2 + 0.4 * rnd.random()) + (255,)
        px[cy][cx] = (255, 255, 255, 255) if gem else A._shade(mat, 0.05) + (255,)
    return px


def moss_top():
    import random
    rnd = random.Random(9)
    px = A.canvas()
    for y in range(S):
        for x in range(S):
            px[y][x] = A._mix(A._shade("teal", 0.45 + 0.35 * rnd.random()), (60, 40, 110), 0.35) + (255,)
    for _ in range(14):
        x, y = rnd.randint(0, S - 1), rnd.randint(0, S - 1)
        px[y][x] = (220, 255, 250, 255)
    return px


def moss_side():
    px = stone(9)
    top = moss_top()
    import random
    rnd = random.Random(10)
    for x in range(S):
        h = 6 + rnd.randint(0, 5)
        for y in range(h):
            px[y][x] = top[y][x]
    return px


def crystal():
    import math
    px = [[(0, 0, 0, 0)] * S for _ in range(S)]
    for cx, h, w in ((10, 22, 4), (17, 28, 5), (24, 18, 3)):
        for y in range(S - h, S):
            half = max(1, int(w * (y - (S - h)) / h) + 1)
            for x in range(cx - half, cx + half):
                if 0 <= x < S:
                    edge = x in (cx - half, cx + half - 1)
                    px[y][x] = A._shade("teal" if cx != 17 else "gem", 0.6 if edge else 0.15 + 0.3 * (y / S)) + (255,)
    return px


def gate_side():
    px = A.side()
    A.plate(px, 4, 4, 24, 24, "steel_dk")
    A.frame(px, "gold", 4)
    import math
    for y in range(6, 26):
        for x in range(6, 26):
            a = math.atan2(y - 15.5, x - 15.5)
            d = math.hypot(x - 15.5, y - 15.5)
            t = (math.sin(a * 3 + d * 0.8) + 1) / 2
            px[y][x] = A._mix(A._shade("gem", 0.2 + 0.4 * t), A._shade("teal", 0.2), 0.3 * (1 - d / 14)) + (255,)
    A.gem(px, 16, 16, 2)
    return px


BLOCKS = {
    "realm_stone": ("Realm Stone", {"all": "realm_stone"}),
    "realm_moss": ("Realm Moss", {"top": "realm_moss_top", "side": "realm_moss_side", "bottom": "realm_stone"}),
    "realm_crystal": ("Realm Crystal", None),
    "essence_crystal_ore": ("Essence Crystal Ore", {"all": "essence_crystal_ore"}),
    "voidsteel_ore": ("Voidsteel Ore", {"all": "voidsteel_ore"}),
    "astral_ore": ("Astral Ore", {"all": "astral_ore"}),
    "realm_gate": ("Realm Gate", None),
}


def block_assets(lang):
    tex("realm_stone", stone())
    tex("realm_moss_top", moss_top())
    tex("realm_moss_side", moss_side())
    tex("realm_crystal", crystal())
    tex("essence_crystal_ore", ore(11, "gem", True))
    tex("voidsteel_ore", ore(12, "steel_dk"))
    tex("astral_ore", ore(13, "gold", True))
    tex("realm_gate_side", gate_side())
    t = NS + ":block/"
    for b, (name, faces) in BLOCKS.items():
        if b == "realm_crystal":
            model = {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout", "textures": {"cross": t + "realm_crystal"}}
            item = {"parent": "minecraft:item/generated", "textures": {"layer0": t + "realm_crystal"}}
        elif b == "realm_gate":
            mat = {"mat_" + m: t + "mat_" + m for m in A.MATERIALS}
            model = {"parent": "minecraft:block/block", "textures": dict(mat, swirl=t + "realm_gate_side", particle=t + "realm_gate_side"),
                     "elements": [A.E((0, 0, 0), (16, 2, 16), "mat_steel_dk"), A.E((0, 14, 0), (16, 16, 16), "mat_steel_dk"),
                                  A.E((1, 2, 7), (15, 14, 9), "swirl")] + A.corners(0, 16, 2, 14, 2, "mat_gold")}
            item = {"parent": t + b}
        elif "all" in faces:
            model = {"parent": "minecraft:block/cube_all", "textures": {"all": t + faces["all"]}}
            item = {"parent": t + b}
        else:
            model = {"parent": "minecraft:block/cube_bottom_top", "textures": {k: t + v for k, v in faces.items()}}
            item = {"parent": t + b}
        A.write_json(os.path.join(ASSETS, "models", "block", b + ".json"), model)
        A.write_json(os.path.join(ASSETS, "models", "item", b + ".json"), item)
        A.write_json(os.path.join(ASSETS, "blockstates", b + ".json"), {"variants": {"": {"model": t + b}}})
        lang["block." + NS + "." + b] = name
    # Loot: ores drop their treasure (more with Fortune, the block itself with Silk Touch); moss drops stone
    silk = {"condition": "minecraft:match_tool", "predicate": {"enchantments": [{"enchantment": "minecraft:silk_touch", "levels": {"min": 1}}]}}

    def drops(b, item, lo, hi):
        return {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": NS + ":" + b, "conditions": [silk]},
            {"type": "minecraft:item", "name": item, "functions": [
                {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}},
                {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                {"function": "minecraft:explosion_decay"}]}]}]}]}
    loot = {"essence_crystal_ore": drops("essence_crystal_ore", "kubejs:ascension_core", 1, 3),
            "voidsteel_ore": drops("voidsteel_ore", "kubejs:voidsteel_ingot", 1, 1),
            "astral_ore": drops("astral_ore", "kubejs:mythic_fragment", 1, 2),
            "realm_moss": drops("realm_moss", NS + ":realm_stone", 1, 1)}
    for b in BLOCKS:
        table = loot.get(b, {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": NS + ":" + b}],
                                                                   "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
        A.write_json(os.path.join(DATA, "loot_tables", "blocks", b + ".json"), table)


# ---- Worldgen ----
BIOMES = {
    # id: (name, sky, fog, extra monsters [(mob, weight)], crystal patches per chunk)
    "ascended_shelf": ("Ascended Shelf", 0x2a1a5e, 0x3b2a7a, [("ascendite_skeleton", 40), ("ascendite_husk", 40), ("ascendite_creeper", 25)], 4),
    "void_garden": ("Void Garden", 0x12081f, 0x1c0f30, [("void_stalker", 30), ("void_archer", 40), ("shadow_skeleton", 40)], 10),
    "astral_peaks": ("Astral Peaks", 0x4a3a1a, 0x5a4a2a, [("storm_wisp", 30), ("thunder_creeper", 25), ("brass_sentinel", 30)], 6),
}
COMMON_MOBS = [("teal_wisp", 30), ("spark_spider", 25), ("minecraft:enderman", 20), ("wisp_tyrant", 1), ("hollow_king", 1)]
ORES = [("essence_crystal_ore", 8, 9), ("voidsteel_ore", 5, 6), ("astral_ore", 3, 4)]   # (block, vein size, veins per chunk)


def feature(path, obj):
    A.write_json(os.path.join(DATA, "worldgen", path + ".json"), obj)


def worldgen(lang):
    with zipfile.ZipFile(VANILLA) as z:
        islands = json.loads(z.read("data/minecraft/worldgen/noise_settings/floating_islands.json"))
    islands["default_block"] = {"Name": NS + ":realm_stone"}
    islands["default_fluid"] = {"Name": "minecraft:air"}
    islands["ore_veins_enabled"] = False
    islands["aquifers_enabled"] = False
    islands["surface_rule"] = {"type": "minecraft:sequence", "sequence": [
        {"type": "minecraft:condition", "if_true": {"type": "minecraft:stone_depth", "offset": 0, "add_surface_depth": False,
                                                     "secondary_depth_range": 0, "surface_type": "floor"},
         "then_run": {"type": "minecraft:block", "result_state": {"Name": NS + ":realm_moss"}}},
        {"type": "minecraft:block", "result_state": {"Name": NS + ":realm_stone"}}]}
    feature("noise_settings/ascended_realm", islands)
    A.write_json(os.path.join(DATA, "dimension_type", "ascended_realm.json"), {
        "ambient_light": 0.15, "bed_works": False, "coordinate_scale": 1.0, "effects": "minecraft:overworld", "fixed_time": 18000,
        "has_ceiling": False, "has_raids": False, "has_skylight": True, "height": 256, "infiniburn": "#minecraft:infiniburn_end",
        "logical_height": 256, "min_y": 0, "monster_spawn_block_light_limit": 0,
        "monster_spawn_light_level": {"type": "minecraft:uniform", "value": {"min_inclusive": 0, "max_inclusive": 7}},
        "natural": False, "piglin_safe": False, "respawn_anchor_works": False, "ultrawarm": False})
    A.write_json(os.path.join(DATA, "dimension", "ascended_realm.json"), {
        "type": NS + ":ascended_realm",
        "generator": {"type": "minecraft:noise", "settings": NS + ":ascended_realm",
                      "biome_source": {"type": "minecraft:checkerboard", "scale": 4, "biomes": [NS + ":" + b for b in BIOMES]}}})
    ore_ids = []
    for b, size, count in ORES:
        feature("configured_feature/realm_" + b, {"type": "minecraft:ore", "config": {"size": size, "discard_chance_on_air_exposure": 0.0,
            "targets": [{"target": {"predicate_type": "minecraft:block_match", "block": NS + ":realm_stone"}, "state": {"Name": NS + ":" + b}}]}})
        feature("placed_feature/realm_" + b, {"feature": NS + ":realm_" + b, "placement": [
            {"type": "minecraft:count", "count": count}, {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": 0}, "max_inclusive": {"absolute": 255}}},
            {"type": "minecraft:biome"}]})
        ore_ids.append(NS + ":realm_" + b)
    feature("configured_feature/realm_crystal", {"type": "minecraft:simple_block", "config": {"to_place": {"type": "minecraft:simple_state_provider",
                                                                                                          "state": {"Name": NS + ":realm_crystal"}}}})
    for b, (name, sky, fog, mobs, crystals) in BIOMES.items():
        feature("placed_feature/realm_crystal_" + b, {"feature": NS + ":realm_crystal", "placement": [
            {"type": "minecraft:count", "count": crystals}, {"type": "minecraft:in_square"},
            {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"},
            {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
                {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"},
                {"type": "minecraft:matching_blocks", "offset": [0, -1, 0], "blocks": NS + ":realm_moss"}]}},
            {"type": "minecraft:biome"}]})
        steps = [[] for _ in range(11)]
        steps[6] = ore_ids
        steps[9] = [NS + ":realm_crystal_" + b]
        monsters = [{"type": (m if ":" in m else NS + ":" + m), "weight": w, "minCount": 1, "maxCount": 1 if w <= 1 else 2} for m, w in mobs + COMMON_MOBS]
        feature("biome/" + b, {"has_precipitation": False, "temperature": 0.5, "downfall": 0.0,
            "effects": {"sky_color": sky, "fog_color": fog, "water_color": 0x40e8d4, "water_fog_color": 0x1c0f30,
                        "particle": {"options": {"type": "minecraft:end_rod"}, "probability": 0.002},
                        "mood_sound": {"sound": "minecraft:ambient.cave", "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0}},
            "spawners": {"monster": monsters, "creature": [], "ambient": [], "axolotls": [], "underground_water_creature": [],
                         "water_creature": [], "water_ambient": [], "misc": []},
            "spawn_costs": {}, "carvers": {"air": []}, "features": steps})
        lang["biome." + NS + "." + b] = name


def main():
    lang_path = os.path.join(ASSETS, "lang", "en_us.json")
    lang = json.load(open(lang_path))
    block_assets(lang)
    worldgen(lang)
    A.write_json(lang_path, lang)
    tags = os.path.join(RES, "data", "minecraft", "tags", "blocks")
    for tag, ids in (("mineable/pickaxe", list(BLOCKS)), ("needs_diamond_tool", ["voidsteel_ore", "astral_ore"]), ("needs_iron_tool", ["essence_crystal_ore"])):
        path = os.path.join(tags, tag + ".json")
        cur = json.load(open(path)) if os.path.exists(path) else {"replace": False, "values": []}
        for b in ids:
            if NS + ":" + b not in cur["values"]:
                cur["values"].append(NS + ":" + b)
        A.write_json(path, cur)
    print("wrote the Ascended Realm: %d blocks, %d biomes, %d ores" % (len(BLOCKS), len(BIOMES), len(ORES)))


if __name__ == "__main__":
    main()
