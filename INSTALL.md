# Installing by hand

About 15 minutes. These steps use **Prism Launcher** on Fedora KDE. The CurseForge app works
the same way: make a Forge 1.20.1 profile, add the mods, then copy the `kubejs` folder in.

## 1. Get Prism Launcher

Open **Discover**, search **Prism Launcher**, install it. Or in a terminal:

```sh
flatpak install flathub org.prismlauncher.PrismLauncher
```

Log in with your Microsoft account when it asks.

## 2. Make the instance

1. **Add Instance** -> pick **1.20.1** -> name it `Factory Ascension`.
2. In the same window, under **Mod loader**, pick **Forge** and the version marked recommended.
3. **OK**.

## 3. Give it enough memory

Right-click the instance -> **Edit** -> **Settings** -> **Java**:
tick **Memory**, set **Maximum memory** to **6144 MiB** (6 GB). Lower it to 4096 if your PC struggles.

## 4. Add the mods

Edit the instance -> **Mods** -> **Download mods** -> **CurseForge** tab.
Search each mod in [MODLIST.md](MODLIST.md), pick the **Forge 1.20.1** version, and click **Select**.
When you've picked them all, click **Review and confirm**. Prism adds most dependencies by itself.

The required ones are **Create, KubeJS, Rhino and Architectury API**. The rest are
recommended, so add whichever you want. **Sophisticated Backpacks** (with Sophisticated Core) is the one for hauling
horde loot home: give it a pickup upgrade and it scoops drops straight into the backpack.

## 5. Launch once, then close

Launch the instance, wait for the main menu, then quit. This creates the `kubejs` folder.

## 6. Copy in the scripts

1. Get this repo: on GitHub click **Code** -> **Download ZIP**, then extract it.
   Or: `git clone https://github.com/thenameiscobmarley/factory-ascension.git`
2. Right-click the instance in Prism -> **Folder**. That opens the instance's `minecraft` folder.
   (On the Flatpak it's under `~/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/instances/`.)
3. Copy the **contents** of the repo's `kubejs` folder into the instance's `kubejs` folder.
   Say **yes** to merging folders. KubeJS's own example files can stay.

You should end up with:

```
minecraft/kubejs/
  startup_scripts/00_fa_config.js, 10_fa_items.js, 20_fa_enchant_effects.js, 30_fa_world.js, 40_fa_ultimates.js, 50_fa_loot.js
  server_scripts/10_fa_ascension.js, 20_fa_core_recipes.js, 25_fa_tome_recipes.js, 30_fa_material_recipes.js,
                 40_fa_progress.js, 45_fa_altar.js, 90_fa_selftest.js
  client_scripts/10_fa_tooltips.js, 20_fa_jei.js
  assets/kubejs/textures/item/   (314 .png files)
  assets/kubejs/textures/block/  (53 .png files: altar, condenser, 51 Condensed Essence tiers)
```

## 7. Check it works

1. Launch and make a **Creative** world with cheats on.
2. Press **E** and type `ascension` in the JEI search bar. Cores, tomes and clusters should show up with textures.
3. Try the whole loop:
   ```
   /give @s diamond_sword
   /give @s kubejs:tome_sharpness
   /ascension essence add 1000
   ```
   Hold the tome in your main hand and the sword in your off hand (press **F** to swap), then right-click.
   All 286 tomes are listed in [docs/ENCHANTS.md](docs/ENCHANTS.md); `/give @s kubejs:tome_<name>` works for any of them.
   Hold the sword and type `/ascension`. Click **[+100]**. The sword's tooltip should now say
   **Ascended Sharpness 101**.
4. Run `/ascension selftest` (needs op / cheats). It builds a test spot far away (x = z = 100000), runs 19 checks
   with a fake player (Essence, enchants, danger zones, gems, rebirth, altar, condenser, loot, hordes, events,
   bosses, an Ultimate), cleans up, and prints **19 / 19 passed**. Any FAIL line says which part is broken.
5. If something's missing, open the instance folder -> `logs/kubejs/` and look in
   `startup.log`, `server.log` and `client.log` for lines with `[Factory Ascension]` or `ERROR`.

## Updating

Copy the new files over the old ones (the whole `kubejs` folder), then restart the game and run `/ascension selftest`.
- Changes to `server_scripts` load with `/reload` in game.
- Changes to `startup_scripts` or `client_scripts` need a full game restart.

## Removing

Delete the `fa_` files and the textures listed above. Enchant data left on items is harmless and does nothing without the scripts.
