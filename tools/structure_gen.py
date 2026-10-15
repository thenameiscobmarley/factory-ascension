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
    "factory_ruin": (40, "surface", "overworld"),
    "ascension_crypt": (55, ("uniform", -40, 30), "overworld"),
    "realm_vault": (4, ("uniform", 40, 200), "realm"),
    "sky_shrine": (9, "surface", "realm"),
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
    print("wrote %d structures" % len(STRUCTURES))


if __name__ == "__main__":
    main()
