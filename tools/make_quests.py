#!/usr/bin/env python3
"""FTB Quests quest book for Factory Ascension: six chapters that walk through the pack, from the first Ascendite
to 10-star Legendary gear. Writes two versions (the Half download has no mod blocks, so its book skips them):

  ftbquests/full/quests/...   (packaged as config/ftbquests/quests in the Full download)
  ftbquests/half/quests/...   (the Half download)

Run from the repo root:  python3 tools/make_quests.py
"""
import hashlib
import os
import shutil

ROOT = os.path.join(os.path.dirname(__file__), "..")
OUT = os.path.join(ROOT, "ftbquests")


def qid(*parts):
    """A stable 16-hex-digit FTB id from a name."""
    return hashlib.sha1("|".join(parts).encode()).hexdigest()[:16].upper()


# Each quest: (key, title, icon, tasks, rewards, depends on keys, description lines, x, y)
# tasks: ("item", id, count) | ("kill", entity, count) | ("check",)
# rewards: ("item", id, count) | ("xp", levels)
M = "factoryascension:"
CHAPTERS = [
    ("getting_started", "Getting Started", "kubejs:ascendite_ingot", [
        ("rock", "Smash Some Rock", "kubejs:rock_hammer", [("item", "kubejs:rock_hammer", 1)], [("item", "kubejs:ascendite_nugget", 9)], [],
         ["Craft a Rock Hammer (5 iron + 2 sticks).", "Hammer in one hand, cobblestone in the other: right-click to smash it into nuggets, flint and sometimes Raw Ascendite."], 0, 0),
        ("ascendite", "Ascendite", "kubejs:ascendite_ingot", [("item", "kubejs:ascendite_ingot", 8)], [("item", "kubejs:ascension_shard", 4)], ["rock"],
         ["Teal ore from Y -48 to 80. Smelt Raw Ascendite into ingots.", "No ore yet? Iron + copper + 2 lapis + an amethyst shard makes one by hand."], 2, 0),
        ("gear", "Teal Steel", "kubejs:ascendite_pickaxe", [("item", "kubejs:ascendite_pickaxe", 1)], [("xp", 5)], ["ascendite"],
         ["Ascendite tools and armor sit between iron and diamond, and take Ascended enchants like any gear."], 4, -1),
        ("first_core", "Your First Ascension Core", "kubejs:ascension_core", [("item", "kubejs:ascension_core", 1)], [("item", "kubejs:ascension_core", 2)], ["ascendite"],
         ["4 Ascendite Ingots + 4 Redstone + Lapis, or 4 Ascension Shards, in a crafting grid.", "Right-click a core in the air to absorb it: +1 Essence. Essence levels your enchants (/ascension)."], 4, 1),
        ("home", "Home Sweet Home", "minecraft:red_bed", [("check",)], [("item", "kubejs:polishing_cloth", 2)], ["first_core"],
         ["Stand where you'll build your base and type /sethome.", "Danger zones are measured from your home: within 1,000 blocks mobs are normal. Further out they get stronger, and so does the loot."], 6, 1),
    ]),
    ("factory_power", "Factory Power", M + "engine_core", [
        ("engine", "The Modular Engine", M + "engine_core", [("item", M + "engine_core", 1), ("item", M + "engine_coil", 1), ("item", M + "engine_burner", 1)],
         [("item", "minecraft:coal", 32)], [],
         ["Power from day one: an Engine Core, a Coil and a Burner touching it. Put a shaft on the core's front face.", "Right-click any part for its screen."], 0, 0),
        ("gearbox", "Faster!", M + "engine_gearbox", [("item", M + "engine_gearbox", 2)], [("item", "create:shaft", 16)], ["engine"],
         ["Each Gearbox takes the engine to its next speed: 16, 32, 64, 128, 256 RPM. Same power, faster shaft."], 2, -1),
        ("press", "Cobblestone Into Ore", "create:mechanical_press", [("item", "create:mechanical_press", 1), ("item", "create:basin", 1)], [("item", "kubejs:raw_ascendite", 8)], ["engine"],
         ["A Mechanical Press over a Basin squeezes 9 cobblestone into Raw Ascendite."], 2, 1),
        ("early_cores", "Cores on Autopilot", "create:mechanical_mixer", [("item", "create:millstone", 1), ("item", "create:mechanical_mixer", 1)], [("item", "kubejs:ascension_core", 8)], ["press"],
         ["Raw Ascendite -> Millstone -> fan through water -> 8 nuggets + 1 redstone in a Mechanical Mixer (no heat) = an Ascension Core.", "All of it runs on the Modular Engine."], 4, 1),
        ("mini_crusher", "Crushing, Small", M + "mini_crusher", [("item", M + "mini_crusher", 1), ("item", "kubejs:electric_motor", 1)], [("item", "kubejs:copper_battery", 1)], ["engine"],
         ["No crushing wheels needed: put a motor and a charged battery in a Mini Crusher and it runs Create's crushing recipes."], 0, 3),
        ("smelter", "The Super Smelter", M + "super_smelter", [("item", M + "super_smelter", 1)], [("item", "kubejs:ascendite_ingot", 16)], ["engine"],
         ["Smelts a whole batch at once, faster the faster it spins. Upgrade it with Voidsteel, Celestium and Ascendium."], 2, 3),
        ("terminal", "One Big Chest", M + "storage_terminal", [("item", M + "storage_terminal", 1), ("item", M + "storage_linker", 1)], [("item", "minecraft:chest", 8)], ["engine"],
         ["Right-click the terminal with the Storage Linker, then right-click chests to link them. The terminal opens them all as one searchable chest."], 4, -1),
    ]),
    ("enchanting", "Gear and Enchanting", "kubejs:blank_ascension_tome", [
        ("blank", "A Blank Tome", "kubejs:blank_ascension_tome", [("item", "kubejs:blank_ascension_tome", 1)], [("item", "kubejs:ascension_core", 4)], [],
         ["A Deployer presses an Ascension Core into a Book."], 0, 0),
        ("tome", "Knowledge Without Limit", "kubejs:tome_sharpness", [("item", "kubejs:tome_sharpness", 1)], [("xp", 10)], ["blank"],
         ["Mix a Blank Tome with a catalyst in a heated Basin. Hover a tome (hold Shift) for its recipe.", "Enchants have no level cap: level them with Essence in /ascension."], 2, 0),
        ("workbench", "The Ascension Workbench", M + "ascension_workbench", [("item", M + "ascension_workbench", 1)], [("item", "kubejs:polishing_cloth", 4)], ["tome"],
         ["Two blocks wide. Gear on top, a tome, gem, attachment or Chaos Tome below, press Apply."], 4, 0),
        ("gem", "Socket It", "kubejs:gem_garnet_1", [("item", "kubejs:gem_garnet_1", 1)], [("item", "kubejs:ascension_core", 8)], ["tome"],
         ["Gems add levels on top of what you bought. 4 Chipped press into 1 Flawless, 4 Flawless into 1 Perfect."], 4, 2),
        ("attachment", "Bolt-On Power", "kubejs:attachment_electric_spike_wrap", [("item", "kubejs:attachment_electric_spike_wrap", 1), ("item", "kubejs:copper_battery", 1)],
         [("item", "kubejs:copper_battery", 1)], ["workbench"],
         ["Attachments add effects to gear. The powered ones run on batteries you carry: recharge them in a crafting grid or a Mechanical Mixer."], 6, 1),
        ("foil", "Shiny!", "kubejs:foil_sheet_gold", [("item", "kubejs:foil_sheet_gold", 1)], [("item", "kubejs:foil_sheet_silver", 1)], ["tome"],
         ["A Foil Sheet multiplies the level a tome puts on gear. Right-click one with the tome in your other hand."], 2, 2),
        ("cased_foil", "Collector's Case", "kubejs:foil_sheet_ruby", [("item", "kubejs:foil_sheet_ruby", 1)], [("item", "kubejs:mythic_fragment", 4)], ["foil"],
         ["Ten cased foils go past Celestial: Ruby (x15) all the way to Eclipse (x100). Each is made from the one before it."], 2, 4),
        ("legend_1", "Legendary", "minecraft:nether_star", [("check",)], [("item", "kubejs:dense_ascension_core", 4)], ["workbench"],
         ["Put gear in the Ascension Workbench and press Star Up: the first Legendary star adds a legendary effect.", "Tick this when your gear has its first star."], 6, -1),
        ("legend_5", "Five Stars", "minecraft:nether_star", [("check",)], [("item", "kubejs:ascension_singularity", 1)], ["legend_1"],
         ["Every star adds another legendary effect and makes every level on the gear count for more."], 8, -1),
        ("legend_10", "Ascended Legendary", "minecraft:nether_star", [("check",)], [("item", "kubejs:ascended_soul", 2)], ["legend_5"],
         ["Ten stars. The whole server hears about it."], 10, -1),
    ]),
    ("danger", "Danger Zones", "minecraft:iron_sword", [
        ("frontier", "The Frontier", "minecraft:iron_sword", [("kill", "minecraft:zombie", 10)], [("item", "kubejs:enchantment_pack", 1)], [],
         ["Travel past 1,000 blocks from your home and fight. Mobs out there are tougher, and loot drops grow exponentially with distance."], 0, 0),
        ("fragment", "Mythic Fragments", "kubejs:mythic_fragment", [("item", "kubejs:mythic_fragment", 4)], [("item", "kubejs:gear_cache", 1)], ["frontier"],
         ["Danger-zone kills drop them, Elites most of all. 4 of them make a Mythic tome."], 2, 0),
        ("mod_mobs", "Strange Wildlife", M + "rust_walker_spawn_egg", [("kill", M + "rust_walker", 3), ("kill", M + "ascendite_husk", 3)], [("item", "kubejs:ascension_core", 16)], ["frontier"],
         ["The pack's own mobs live out in the danger zones. Each one drops its own part: cook it for a buff."], 2, 2),
        ("colossus", "The Rust Colossus", M + "rust_colossus_spawn_egg", [("kill", M + "rust_colossus", 1)], [("item", "kubejs:ascended_soul", 1)], ["mod_mobs"],
         ["A boss, deep out (danger 8+). It slams, calls minions and enrages at a quarter health."], 4, 1),
        ("broodmother", "The Broodmother", M + "broodmother_spawn_egg", [("kill", M + "broodmother", 1)], [("item", "kubejs:ascended_soul", 1)], ["mod_mobs"],
         ["A spider the size of a house, in the forests (danger 8+)."], 4, 3),
        ("hollow_king", "The Hollow King", M + "hollow_king_spawn_egg", [("kill", M + "hollow_king", 1)], [("item", "kubejs:ascended_soul", 2)], ["colossus"],
         ["Volleys of wither arrows (danger 12+)."], 6, 1),
        ("wisp_tyrant", "The Wisp Tyrant", M + "wisp_tyrant_spawn_egg", [("kill", M + "wisp_tyrant", 1)], [("item", "kubejs:ascended_soul", 2)], ["broodmother"],
         ["Rings of fireballs in the Nether (danger 10+)."], 6, 3),
    ]),
    ("collecting", "Collecting and Storage", "kubejs:enchantment_pack", [
        ("pack", "Rip It Open", "kubejs:enchantment_pack", [("item", "kubejs:enchantment_pack", 1)], [("item", "kubejs:enchantment_pack", 1)], [],
         ["Packs of tome cards turn up in chests and danger-zone loot. Right-click to open: the last card is Rare or better."], 0, 0),
        ("box", "A Whole Box", "kubejs:booster_box", [("item", "kubejs:booster_box", 1)], [("item", "kubejs:foil_sheet_holo", 1)], ["pack"],
         ["12 packs in one box. Only in the best chests and Epic+ loot."], 2, -1),
        ("binder", "The Card Binder", M + "card_binder", [("item", M + "card_binder", 1)], [("item", "kubejs:enchantment_pack", 2)], ["pack"],
         ["Holds 54 tome cards and floats your best one above it."], 2, 1),
        ("opener", "Pack Opener", M + "pack_opener", [("item", M + "pack_opener", 1)], [("item", "kubejs:enchantment_pack", 4)], ["binder"],
         ["Hopper packs in: it opens one a second."], 4, 1),
        ("vault", "The Ascension Vault", M + "ascension_vault", [("item", M + "ascension_vault", 1)], [("item", "kubejs:dense_ascension_core", 2)], ["pack"],
         ["A chest with search, sort and 108 slots, upgradeable to 432."], 2, 3),
        ("room", "Instant Basement", "kubejs:room_tnt_quartz_16", [("item", "kubejs:room_tnt_quartz_16", 1)], [("item", "minecraft:tnt", 4)], [],
         ["Factory Room TNT blasts out a lit, lined room in front of you."], 0, 3),
    ]),
    ("endgame", "Endgame", "kubejs:ascendium_ingot", [
        ("voidsteel", "Voidsteel", "kubejs:voidsteel_ingot", [("item", "kubejs:voidsteel_ingot", 4)], [("item", "kubejs:dense_ascension_core", 2)], [],
         ["Diamond + 2 Ascendite Ingots + Obsidian in a heated mixer."], 0, 0),
        ("dense", "Dense Cores", "kubejs:dense_ascension_core", [("item", "kubejs:dense_ascension_core", 9)], [("item", "kubejs:ascension_singularity", 1)], [],
         ["9 cores pressed in a heated Basin."], 0, 2),
        ("celestium", "Celestium", "kubejs:celestium_ingot", [("item", "kubejs:celestium_ingot", 4)], [("item", "kubejs:mythic_fragment", 4)], ["voidsteel", "dense"],
         ["Netherite + 2 Voidsteel + a Dense Core, superheated."], 2, 1),
        ("altar", "The Ascension Altar", "kubejs:ascension_altar", [("item", "kubejs:ascension_altar", 1)], [("item", "kubejs:liquid_essence_bucket", 4)], ["dense"],
         ["A Depot on top, Liquid Essence below: it levels gear by itself."], 2, 3),
        ("singularity", "Singularity", "kubejs:ascension_singularity", [("item", "kubejs:ascension_singularity", 1)], [("item", "kubejs:ascended_soul", 1)], ["celestium"],
         ["81 Essence in one item."], 4, 1),
        ("ascendium", "Ascendium", "kubejs:ascendium_ingot", [("item", "kubejs:ascendium_ingot", 1)], [("item", "kubejs:foil_sheet_celestial", 1)], ["singularity"],
         ["The endgame metal: Celestium, a Singularity, Mythic Fragments and Liquid Essence, superheated."], 6, 1),
        ("eclipse", "Eclipse", "kubejs:foil_sheet_eclipse", [("item", "kubejs:foil_sheet_eclipse", 1)], [("item", "minecraft:nether_star", 2)], ["ascendium"],
         ["The best foil there is: x100."], 8, 1),
    ]),
]


def snbt(v, ind=0):
    pad = "\t" * ind
    if isinstance(v, dict):
        if not v:
            return "{ }"
        return "{\n" + "\n".join("%s\t%s: %s" % (pad, k, snbt(v[k], ind + 1)) for k in sorted(v)) + "\n" + pad + "}"
    if isinstance(v, list):
        if not v:
            return "[ ]"
        if all(isinstance(x, str) for x in v):
            return "[\n" + "\n".join(pad + "\t" + snbt(x) for x in v) + "\n" + pad + "]"
        return "[\n" + "\n".join(pad + "\t" + snbt(x, ind + 1) for x in v) + "\n" + pad + "]"
    if isinstance(v, bool):
        return "true" if v else "false"
    if isinstance(v, float):
        return repr(v) + "d"
    if isinstance(v, Long):
        return str(int(v)) + "L"
    if isinstance(v, int):
        return str(v)
    return '"' + str(v).replace("\\", "\\\\").replace('"', '\\"') + '"'


class Long(int):
    pass


def task(chapter, key, i, t):
    tid = qid(chapter, key, "task", str(i))
    if t[0] == "item":
        return {"id": tid, "type": "item", "item": t[1], "count": Long(t[2])}
    if t[0] == "kill":
        return {"id": tid, "type": "kill", "entity": t[1], "value": Long(t[2])}
    return {"id": tid, "type": "checkmark"}


def reward(chapter, key, i, r):
    rid = qid(chapter, key, "reward", str(i))
    if r[0] == "item":
        return {"id": rid, "type": "item", "item": r[1], "count": r[2]}
    return {"id": rid, "type": "xp_levels", "xp_levels": r[1]}


def uses_mod(q):
    items = [q[2]] + [t[1] for t in q[3] if len(t) > 1] + [r[1] for r in q[4] if r[0] == "item"]
    return any(str(i).startswith(M) for i in items)


def build(variant):
    base = os.path.join(OUT, variant, "quests")
    if os.path.exists(base):
        shutil.rmtree(base)
    os.makedirs(os.path.join(base, "chapters"))
    with open(os.path.join(base, "data.snbt"), "w") as f:
        f.write(snbt({"version": 13, "default_reward_team": False, "default_consume_items": False, "default_autoclaim_rewards": "disabled",
                      "default_quest_shape": "circle", "default_quest_disable_jei": False, "emergency_items_cooldown": 300, "drop_loot_crates": False,
                      "disable_gui": False, "grid_scale": 0.5, "pause_game": False, "lock_message": "", "progression_mode": "default",
                      "detection_delay": 20, "title": "Factory Ascension", "icon": "kubejs:ascension_core"}) + "\n")
    with open(os.path.join(base, "chapter_groups.snbt"), "w") as f:
        f.write(snbt({"chapter_groups": []}) + "\n")
    total = 0
    for order, (cid, title, icon, quests) in enumerate(CHAPTERS):
        keep = [q for q in quests if variant == "full" or not uses_mod(q)]
        if not keep:
            continue
        keys = set(q[0] for q in keep)
        out = []
        for q in keep:
            key, qt, qicon, tasks, rewards, deps, desc, x, y = q
            out.append({"id": qid(cid, key), "title": qt, "icon": qicon, "x": float(x), "y": float(y), "description": desc,
                        "dependencies": [qid(cid, d) for d in deps if d in keys],
                        "tasks": [task(cid, key, i, t) for i, t in enumerate(tasks)],
                        "rewards": [reward(cid, key, i, r) for i, r in enumerate(rewards)]})
        total += len(out)
        icon = icon if variant == "full" or not icon.startswith(M) else "kubejs:ascension_core"
        with open(os.path.join(base, "chapters", cid + ".snbt"), "w") as f:
            f.write(snbt({"id": qid(cid), "filename": cid, "title": title, "icon": icon, "order_index": order, "group": "",
                          "default_quest_shape": "", "default_hide_dependency_lines": False, "quest_links": [], "quests": out}) + "\n")
    return total


def check_items():
    """Every kubejs: and factoryascension: item a quest names must exist (a texture or a lang entry)."""
    import json
    tex = os.path.join(ROOT, "kubejs", "assets", "kubejs", "textures")
    lang = json.load(open(os.path.join(ROOT, "mod", "src", "main", "resources", "assets", "factoryascension", "lang", "en_us.json")))
    bad = []
    for cid, title, icon, quests in CHAPTERS:
        for q in quests:
            ids = [q[2]] + [t[1] for t in q[3] if t[0] == "item"] + [r[1] for r in q[4] if r[0] == "item"]
            ents = [t[1] for t in q[3] if t[0] == "kill"]
            for i in ids:
                ns, path = i.split(":")
                if ns == "kubejs" and not path.endswith("_bucket") and not (os.path.exists(os.path.join(tex, "item", path + ".png")) or os.path.exists(os.path.join(tex, "block", path + ".png"))):
                    bad.append(i)
                if ns == "factoryascension" and "item.%s" % i.replace(":", ".") not in lang and "block.%s" % i.replace(":", ".") not in lang:
                    bad.append(i)
            for e in ents:
                if e.startswith(M) and "entity.%s" % e.replace(":", ".") not in lang:
                    bad.append(e)
    if bad:
        raise SystemExit("quests name items that don't exist: " + ", ".join(sorted(set(bad))))


def main():
    check_items()
    for v in ("full", "half"):
        print("%s: %d quests" % (v, build(v)))


if __name__ == "__main__":
    main()
