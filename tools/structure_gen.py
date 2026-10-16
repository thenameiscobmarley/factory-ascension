#!/usr/bin/env python3
"""Where the pack's structures generate (the blocks are placed by mod/.../worldgen/FAStructures.java) and what their
chests hold. The scripts add Enchantment Packs, Booster Boxes and Gear Caches on top (config: packs.chests,
caches.chests). Run from the repo root (make_mod_assets.py runs it):  python3 tools/structure_gen.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import make_mod_assets as A  # noqa: E402

NS = "factoryascension"
DATA = A.DATA

# id: (rarity: 1 in N chunks, placement, where)
STRUCTURES = {
    "factory_ruin": (400, "surface", "overworld"),        # about one in every 20 x 20 chunks (and only on flat ground)
    "ascension_crypt": (300, ("uniform", -40, 30), "overworld"),
    "realm_vault": (25, ("uniform", 40, 200), "realm"),   # most tries miss the islands, so this is rarer than it looks
    "sky_shrine": (60, "surface", "realm"),
}


def entry(item, weight, lo=1, hi=1):
    e = {"type": "minecraft:item", "name": item, "weight": weight}
    if hi > 1:
        e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return e


def pool(lo, hi, entries):
    return {"rolls": {"type": "minecraft:uniform", "min": lo, "max": hi}, "entries": entries}


K = "kubejs:"
LOOT = {
    "factory_ruin": [pool(4, 7, [entry("create:andesite_alloy", 20, 4, 12), entry("create:shaft", 15, 2, 8), entry("create:cogwheel", 15, 2, 6),
                                 entry("create:large_cogwheel", 8, 1, 3), entry("create:brass_ingot", 10, 2, 6), entry("create:zinc_ingot", 10, 2, 8),
                                 entry("create:belt_connector", 8, 2, 6), entry("create:precision_mechanism", 3), entry("create:electron_tube", 6, 1, 4),
                                 entry("minecraft:iron_ingot", 12, 2, 8), entry("minecraft:copper_ingot", 12, 3, 10), entry("minecraft:redstone", 10, 4, 12)]),
                     pool(1, 3, [entry(K + "raw_ascendite", 20, 2, 8), entry(K + "ascendite_ingot", 12, 1, 4), entry(K + "ascension_core", 6, 1, 3),
                                 entry(K + "ascension_shard", 10, 2, 8)])],
    "ascension_crypt": [pool(3, 6, [entry("minecraft:bone", 15, 2, 8), entry("minecraft:gold_ingot", 10, 2, 6), entry("minecraft:diamond", 4, 1, 3),
                                    entry("minecraft:emerald", 6, 1, 4), entry("minecraft:golden_apple", 3), entry("minecraft:experience_bottle", 8, 2, 8),
                                    entry("minecraft:name_tag", 3)]),
                        pool(2, 4, [entry(K + "ascendite_ingot", 15, 2, 6), entry(K + "ascension_core", 12, 1, 4), entry(K + "dense_ascension_core", 3),
                                    entry(K + "chaos_tome", 4), entry(K + "polishing_cloth", 6, 1, 2), entry(K + "mythic_fragment", 2)])],
    "realm_vault": [pool(3, 5, [entry(K + "ascension_core", 15, 4, 12), entry(K + "dense_ascension_core", 12, 1, 4), entry(K + "voidsteel_ingot", 12, 2, 6),
                                entry(K + "celestium_ingot", 8, 1, 3), entry(K + "mythic_fragment", 10, 2, 6), entry(K + "chaos_tome", 6, 1, 2),
                                entry("minecraft:netherite_ingot", 4), entry("minecraft:enchanted_golden_apple", 2)]),
                    pool(1, 2, [entry(K + "ascension_singularity", 6), entry(K + "ascended_soul", 3), entry(K + "ascendium_ingot", 2),
                                entry(K + "booster_box", 6)])],
    "sky_shrine": [pool(2, 4, [entry(K + "mythic_fragment", 12, 1, 4), entry(K + "ascension_core", 15, 2, 8), entry(K + "voidsteel_ingot", 8, 1, 3),
                               entry(K + "polishing_cloth", 8, 1, 3), entry(K + "chaos_tome", 6), entry("minecraft:amethyst_shard", 10, 4, 16),
                               entry("minecraft:ender_pearl", 8, 2, 6)]),
                   pool(0, 1, [entry(K + "dense_ascension_core", 6), entry(K + "ascension_singularity", 1), entry(K + "enchantment_pack", 8, 1, 3)])],
}


# The armed operators' places (mod/.../worldgen/Outposts.java): each builds itself from a random roll, and only so far
# from world spawn (Outposts.Kind.minDistance), so near home most of these tries come to nothing on purpose.
OUTPOSTS = {
    "scav_camp": 220, "ruins": 260, "gas_station": 340, "checkpoint": 300, "trench": 360, "sniper_tower": 320,
    "military_outpost": 520, "bunker": 420, "crash_site": 480, "radio_tower": 460, "warlord_fort": 900, "cult_shrine": 700,
}

M = NS + ":"
GUNS_BY_TIER = [
    ["makarov", "tt33", "m9", "p17", "m1911", "mp40", "sawnoff", "double_barrel", "m870", "uz9", "aks74u", "mosin"],
    ["glock18", "usp45", "p226", "cz75", "python", "mp5", "ump45", "bizon", "ppsh", "thompson", "ak74", "akm", "galil", "fal", "m1_garand", "kar98k",
     "saiga12", "m1014", "spas12", "svd", "winchester", "rpk", "dp28"],
    ["five7", "hawk50", "m29", "judge", "vector", "p90", "mp7", "kv45", "mac10", "hk416", "m4a1", "scar_l", "ak12", "mcx", "g36", "famas", "l85",
     "tavor", "aug", "mk17", "as_val", "vss", "sr25", "mk14", "m700", "ksg", "aa12", "pkm", "m249", "m60", "mg42"],
    ["awm", "ax338", "m82", "minigun"],
]
CALIBERS = ["P9", "P45", "R545", "R556", "R762S", "R762N", "R762R", "G12", "P57", "P762T", "R44", "R9X39", "R300", "R357", "P46", "P50", "R338", "B50"]


def rounds(cal, weight, lo, hi, kind="REGULAR"):
    e = entry(M + "cartridge", weight, lo, hi)
    e.setdefault("functions", []).append({"function": "minecraft:set_nbt", "tag": '{Cal:"%s",Type:"%s",Prop:"minecraft:gunpowder",Case:"minecraft:copper_ingot",Color:13140028,Power:1.0f,Rel:1.0f}' % (cal.lower(), kind)})
    return e


def guns(tier, weight):
    return [entry(M + g, weight) for g in GUNS_BY_TIER[tier]]


def outpost_loot():
    out = {}
    meds = lambda w: [entry(M + "bandage", w, 1, 3), entry(M + "medkit", max(1, w // 3))]
    for t in range(3):
        cals = CALIBERS[:8 + t * 5]
        out["scav_%d" % t] = [
            pool(2, 4, [entry("minecraft:bread", 10, 1, 4), entry("minecraft:cooked_beef", 6, 1, 3), entry("minecraft:potato", 8, 2, 6), entry("minecraft:gunpowder", 8, 2, 6),
                        entry("minecraft:iron_nugget", 10, 3, 12), entry("minecraft:copper_ingot", 8, 2, 6), entry("minecraft:string", 6, 2, 5), entry("minecraft:paper", 6, 2, 6),
                        entry(M + "flare_red", 3), entry(M + "flare_green", 1), entry(M + "cannabis_seeds", 3, 1, 3), entry(M + "coca_seeds", 2, 1, 2),
                        entry(M + "joint", 3, 1, 3), entry(M + "cocaine", 1), entry(M + "polyethylene_plate", 2), entry(M + "flashbang", 1)] + meds(6)),
            pool(1, 2, [rounds(c, 6, 4, 14) for c in cals[:8]]),
            pool(0, 1, guns(0, 4) + (guns(1, 1) if t > 0 else []) + [{"type": "minecraft:empty", "weight": 30 - t * 8}]),
        ]
        out["military_%d" % t] = [
            pool(2, 4, [rounds(c, 6, 6, 20) for c in cals] + [rounds(c, 1, 4, 10, k) for c in cals[:6] for k in ("HOLLOW_POINT", "ARMOR_PIERCING", "TRACER")]),
            pool(1, 3, meds(8) + [entry(M + "frag_grenade", 6, 1, 2), entry(M + "flare_red", 3), entry(M + "flare_green", 2), entry("minecraft:iron_ingot", 6, 2, 6),
                                  entry("kubejs:ascendite_ingot", 3, 1, 3), entry("minecraft:spyglass", 2), entry(M + "flashbang", 5, 1, 2),
                                  entry(M + "plate_carrier", 2), entry(M + "steel_plate", 2), entry(M + "ceramic_plate", 2), entry(M + "nods_green", 1),
                                  entry(M + "adrenaline_shot", 3), entry(M + "morphine", 3)]),
            pool(0, 1, guns(min(1 + t, 2), 3) + guns(t, 2) + [{"type": "minecraft:empty", "weight": 22 - t * 6}]),
        ]
        out["bunker_%d" % t] = [
            pool(3, 5, [rounds(c, 6, 8, 24) for c in cals] + [rounds(c, 2, 6, 14, k) for c in cals[:8] for k in ("ARMOR_PIERCING", "RIP", "INCENDIARY", "EXPLOSIVE")]),
            pool(2, 3, meds(10) + [entry(M + "frag_grenade", 8, 1, 3), entry("minecraft:golden_apple", 2), entry("kubejs:ascendite_ingot", 5, 1, 4),
                                  entry("kubejs:ascension_core", 3, 1, 2), entry("minecraft:diamond", 2, 1, 2), entry(M + "flare_green", 3),
                                  entry(M + "hybrid_plate", 3), entry(M + "titanium_plate", 2), entry(M + "nods_green", 2), entry(M + "nods_white", 1),
                                  entry(M + "flashbang", 5, 1, 3), entry(M + "methamphetamine", 1), entry(M + "adrenaline_shot", 3, 1, 2)]),
            pool(1, 1, guns(2, 3) + guns(min(3, t + 1), 1) + [{"type": "minecraft:empty", "weight": 10 - t * 3}]),
        ]
    out["fort_treasure"] = [
        pool(3, 6, [entry("minecraft:gold_ingot", 12, 3, 10), entry("minecraft:emerald", 8, 2, 8), entry("minecraft:diamond", 6, 1, 4), entry("minecraft:netherite_scrap", 2),
                    entry("kubejs:ascension_core", 6, 1, 4), entry("kubejs:dense_ascension_core", 2), entry("minecraft:enchanted_golden_apple", 1), entry(M + "medkit", 6, 1, 2)]),
        pool(1, 2, guns(2, 3) + guns(3, 2)),
        pool(2, 3, [rounds(c, 4, 8, 24) for c in CALIBERS]),
    ]
    out["cult"] = [
        pool(2, 4, [entry("minecraft:candle", 8, 1, 4), entry("minecraft:bone", 10, 2, 6), entry("minecraft:rotten_flesh", 6, 2, 8), entry("minecraft:ender_pearl", 5, 1, 3),
                    entry("minecraft:experience_bottle", 6, 2, 6), entry("minecraft:crying_obsidian", 3, 1, 2), entry("kubejs:mythic_fragment", 2), entry("kubejs:chaos_tome", 2)]),
        pool(1, 2, [rounds("R9X39", 8, 6, 18), rounds("R9X39", 3, 6, 12, "SUBSONIC"), entry(M + "vss", 2), entry(M + "as_val", 2), entry(M + "medkit", 3),
                    entry(M + "magic_mushrooms", 4, 1, 3), entry(M + "methamphetamine", 2), entry(M + "nods_white", 1)]),
    ]
    return out


def placed_id(s):
    return NS + ":" + s


def main():
    for s, (rarity, how, where) in STRUCTURES.items():
        A.write_json(os.path.join(DATA, "worldgen", "configured_feature", s + ".json"), {"type": NS + ":" + s, "config": {}})
        place = [{"type": "minecraft:rarity_filter", "chance": rarity}, {"type": "minecraft:in_square"}]
        if how == "surface":
            place.append({"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"})
        else:
            place.append({"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": how[1]},
                                                                      "max_inclusive": {"absolute": how[2]}}})
        place.append({"type": "minecraft:biome"})
        A.write_json(os.path.join(DATA, "worldgen", "placed_feature", s + ".json"), {"feature": NS + ":" + s, "placement": place})
        A.write_json(os.path.join(DATA, "forge", "biome_modifier", "structure_" + s + ".json"), {
            "type": "forge:add_features", "biomes": "#minecraft:is_overworld" if where == "overworld" else "#factoryascension:is_realm",
            "features": placed_id(s), "step": "surface_structures" if how == "surface" else "underground_structures"})
        A.write_json(os.path.join(DATA, "loot_tables", "chests", s + ".json"), {"type": "minecraft:chest", "pools": LOOT[s]})
    for s, rarity in OUTPOSTS.items():
        A.write_json(os.path.join(DATA, "worldgen", "configured_feature", s + ".json"), {"type": NS + ":" + s, "config": {}})
        place = [{"type": "minecraft:rarity_filter", "chance": rarity}, {"type": "minecraft:in_square"},
                 {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]
        A.write_json(os.path.join(DATA, "worldgen", "placed_feature", s + ".json"), {"feature": NS + ":" + s, "placement": place})
        A.write_json(os.path.join(DATA, "forge", "biome_modifier", "structure_" + s + ".json"), {
            "type": "forge:add_features", "biomes": "#minecraft:is_overworld", "features": placed_id(s), "step": "surface_structures"})
    for name, pools in outpost_loot().items():
        A.write_json(os.path.join(DATA, "loot_tables", "chests", name + ".json"), {"type": "minecraft:chest", "pools": pools})
    print("wrote %d structures and %d outposts" % (len(STRUCTURES), len(OUTPOSTS)))


if __name__ == "__main__":
    main()
