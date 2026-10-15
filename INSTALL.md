# Installing

Get a download from the [release page](https://github.com/thenameiscobmarley/factory-ascension/releases/latest):

| File | Use it when |
|---|---|
| `FactoryAscension-Full-<version>-CurseForge.zip` | You use the CurseForge app (easiest). Scripts + the mod blocks |
| `FactoryAscension-Half-<version>-CurseForge.zip` | Same, without the mod blocks (KubeJS only) |
| `FactoryAscension-Full-<version>.zip` | You already have an instance and want to copy the files in by hand |
| `FactoryAscension-Half-<version>.zip` | Same, scripts only |

**Full** = the KubeJS scripts plus `factoryascension-<version>.jar` (Modular Engine, Super Smelter, Salvager, Tome Fusion Table, Ascension Vault, Card Binder, Essence Forge, Pack Opener, connected glass, Shiny Reflections, 34 mobs and 4 bosses).
**Half** = the scripts only. Both work on their own; Half just doesn't have the mod's blocks.
Servers and everyone joining them need the same one.

## Easy way: import into the CurseForge app

1. Open the CurseForge app -> **Minecraft** -> **Create Custom Profile** (top right) -> **import** (the small link next to the
   Create button) -> pick the `-CurseForge.zip` you downloaded.
2. It makes a Forge 1.20.1 profile and downloads Create, KubeJS, Rhino, Architectury, TFMG, Mekanism, JEI, Waystones and Balm
   (the versions the pack is tested with). The scripts and, for Full, the mod jar are copied in for you.
3. Profile **...** -> **Profile Options**: give it **6 GB** of memory (4 GB if your PC struggles).
4. Play, then do **Check it works** below. Add anything else from [MODLIST.md](MODLIST.md) you want.

## By hand (any launcher)

1. Make a **Forge 1.20.1** instance (Forge 47.4.10 or newer) with at least **Create, KubeJS, Rhino and Architectury API**
   from [MODLIST.md](MODLIST.md), pick the **Forge 1.20.1** file of each. Give it 6 GB of memory.
2. Launch once to the main menu and quit. This creates the `kubejs` folder.
3. Open the instance folder (the one with `mods` in it). Extract the plain zip into it and say **yes** to merging folders.
   Full puts `factoryascension-<version>.jar` into `mods/`; both put the scripts into `kubejs/`. KubeJS's own example files can stay.
   On Linux, `./install.sh "/path/to/instance"` from a clone of this repo does the `kubejs` part and backs up anything it overwrites.

You should end up with:

```
mods/factoryascension-1.1.0.jar   (Full only)
kubejs/
  startup_scripts/00_fa_config.js, 01_fa_conflicts.js, 10_fa_items.js, 20_fa_enchant_effects.js, 30_fa_world.js, 40_fa_ultimates.js, 50_fa_loot.js,
                  60_fa_mob_food.js, 70_fa_variants.js, 72_fa_mod_mobs.js, 76_fa_skinwalkers.js, 80_fa_combat.js, 90_fa_artifacts.js, 95_fa_attachments.js, 97_fa_mod.js
  server_scripts/10_fa_ascension.js, 15_fa_home.js, 16_fa_packs.js, 17_fa_sell.js, 18_fa_attachments.js, 19_fa_foils.js, 20_fa_core_recipes.js, 25_fa_tome_recipes.js, 30_fa_material_recipes.js,
                 32_fa_ascendite_data.js, 35_fa_starter_recipes.js, 37_fa_food_recipes.js, 38_fa_hammer.js, 39_fa_condition.js, 40_fa_progress.js, 45_fa_altar.js,
                 46_fa_machines.js, 48_fa_rooms.js, 90_fa_selftest.js
  client_scripts/10_fa_tooltips.js, 20_fa_jei.js
  data/kubejs/   (Ascendite ore worldgen: worldgen/ and forge/biome_modifier/)
  assets/kubejs/textures/item/   (item textures)
  assets/kubejs/textures/block/  (altar, condenser, Condensed Essence tiers, Ascendite ores + block)
  assets/kubejs/textures/models/armor/  (worn Ascendite, Voidsteel, Celestium and Ascendium armor)
  assets/kubejs/models/item/     (tome models: each picks its foil look)
```

## Check it works

1. Launch and make a **Creative** world with cheats on.
2. Press **E** and type `ascension` in the JEI search bar. Cores, tomes and clusters should show up with textures.
3. Try the whole loop:
   ```
   /give @s diamond_sword
   /give @s kubejs:tome_sharpness
   /ascension essence add 1000
   ```
   Hold the tome in your main hand and the sword in your off hand (press **F** to swap), then right-click.
   All 486 tomes are listed in [docs/ENCHANTS.md](docs/ENCHANTS.md); `/give @s kubejs:tome_<name>` works for any of them.
   Hold the sword and type `/ascension`. Click **[+100]**. The sword's tooltip should now say
   **Ascended Sharpness 101**.
4. Run `/ascension selftest` (needs op / cheats). It builds a test spot far away (x = z = 100000), runs 64 checks (79 with the Full jar)
   with a fake player (Essence, a real tome right-click, leveled tomes and extracting, enchants, danger zones, Ascendite, mob food, Rock Hammers, gear condition, rare variants, glowing ores, the safe-zone center, /home, card packs, Artifacts, gems, rebirth,
   altar, condenser, loot, hordes, events, bosses, an Ultimate), cleans up, and prints **64 / 64 passed** (**79 / 79** with Full).
   Any FAIL line says which part is broken.
5. Full only: open the **Factory Ascension** creative tab, place each block and open it. The Vault should scroll, search and sort;
   drop an Enchantment Pack into the Pack Opener and watch the cards flip. Build an engine: an Engine Core, a Coil and a Burner
   with coal, a shaft on the core's front into a Super Smelter's back; raw iron in the smelter should come out as ingots.
6. If something's missing, open the instance folder -> `logs/kubejs/` and look in
   `startup.log`, `server.log` and `client.log` for lines with `[Factory Ascension]` or `ERROR`.

## Updating

CurseForge app: import the new `-CurseForge.zip` as a new profile, or extract the plain zip over your profile's folder
(Profile **...** -> **Open Folder**). By hand: extract the new zip over the old files, and delete the old
`mods/factoryascension-*.jar` if the version changed. Then restart the game and run `/ascension selftest`.
If you're updating from a version with 3D item models, delete the instance's `kubejs/assets/kubejs/models` folder first
(copying over the top doesn't remove old files).
- Changes to `server_scripts` load with `/reload` in game.
- Changes to `startup_scripts` or `client_scripts` need a full game restart.

## Removing

Delete the `fa_` files, the textures listed above and `mods/factoryascension-*.jar` (the mod's blocks and anything stored in them go with it). Enchant data left on items is harmless and does nothing without the scripts.
