# Factory Ascension

A Minecraft modpack setup (Forge 1.20.1) where your **Create factory powers your gear**.
The bigger the factory grows, the stronger your enchants get, with no level cap.
Want **Protection 3000**? Build a factory big enough to pay for it.
Mobs don't scale with time or distance, so you can explore freely and AFK safely.

**Install:** follow [INSTALL.md](INSTALL.md).

## The loop

```
cobble generators -> mineral clusters -> ores -> diamonds, rose quartz, steel
   -> Ascension Cores (Create assembly line) -> absorb into your Essence bank
   -> Tomes unlock Ascended enchants on any gear -> spend Essence to level them up
   -> crush bosses and dungeons -> grow the factory -> repeat
```

## Ascended enchants

**59 enchants**, separate from vanilla ones (vanilla caps levels at 255), so levels go as high as your
factory can pay for. They count from **every equipped item**, both hands and all armor, so any enchant
works on any gear. Full list with numbers and tome recipes: **[docs/ENCHANTS.md](docs/ENCHANTS.md)**.

| Group | Enchants |
|---|---|
| Damage | Ascended Sharpness, Smite, Bane, Power, Impaling, Giant Slayer, Hellbane, Raidbreaker, Skyhunter, First Strike, Executioner, Berserk, Assassin, Critical Edge, Reaper, Cleave |
| On hit | Lifesteal, Frostbite, Enfeeble, Withering, Venom, Skyward, Ascended Fire Aspect, Ascended Knockback, Second Wind, Adrenaline |
| On kill | Plunder, Soul Eater, Wisdom, Midas, Gem Hunter |
| Defense | Ascended Protection, Vitality (hidden health, no extra hearts), Fire / Blast / Arrow / Wither Ward, Featherweight, Spellward, Bulwark, Retribution, Evasion, Undying |
| Passive | Renewal, Night Sight, Gills, Fireproof, Industrious, Leaping, Sustenance, Magnetism, Restoration |
| Movement & utility | Swiftness, Reach, Stride, Frenzy, Lucky, Excavation, Eternal |

A few at level 3000: Ascended Sharpness x301 damage, Ascended Protection -99.2% damage taken,
Vitality +6,000 hidden health, Plunder x151 loot, Excavation x751 mining speed.

Each level costs a bit more than the last: levels 1-20 cost 1 Essence each, level 3000 costs 151,
and getting one enchant from 0 to 3000 takes about 226K Essence.

## How to use it

1. **Absorb cores:** right-click an Ascension Core for 1 Essence. Sneak + right-click absorbs the whole stack.
   Dense Cores give 9 and Singularities give 81.
2. **Unlock an enchant:** hold a Tome in one hand and gear in the other, then right-click the tome.
   It costs the tome and 1 Essence.
3. **Level it up:** hold the gear and type `/ascension`. Click **[+1] [+10] [+100] [+1000] [Max]**.

The enchants show in the item's tooltip right under its name, with what they do, e.g.
`Ascended Sharpness 101  x11.10 damage`.

## Recipes (all automatable with Create)

| Item | How |
|---|---|
| Ascension Core | Sequenced assembly on a diamond: deploy polished rose quartz, (deploy TFMG steel), fill 250 mB lava, press. 3 loops |
| Dense Core / Singularity | Compact 9 (heated) / 9 Dense (superheated). Unpack in a crafting grid |
| Blank Ascension Tome | Deployer: Ascension Core onto a book |
| Enchant Tomes | Heated mixer: blank tome + catalyst (see JEI) |
| Ores, gems, netherite | Mineral clusters from cobblestone, gravel, blackstone. See [docs/FACTORY.md](docs/FACTORY.md) |

## Settings

Everything is in `kubejs/startup_scripts/00_fa_config.js`: Essence values, the cost curve, what each
enchant does per level, and safety limits. Edit it, then restart the game.

## Safety

- Essence is saved per player in the world's KubeJS data. Enchant levels live on the item.
- `/kill` and the void still kill you, whatever your Protection.
- Player death drops are never multiplied. Extra loot per kill is capped so Plunder can't lag the server.
- Cleave and Retribution damage can't trigger more enchants, so there are no chain reactions or infinite loops.
- Cleave only hits hostile mobs, never players, pets or villagers. Reaper never executes bosses.
- Evasion maxes out at 75%, and effect amplifiers stop at 128, so nothing breaks vanilla limits.
- If absorbing cores fails, the items are given back. A tome is only used up when the unlock succeeds.
- Only single unstackable items (tools, weapons, armor) can be enchanted, never a stack.
- Recipes that use other mods only load when those mods are installed.
- A broken hook logs one error and switches itself off instead of spamming the log.

## Commands

| Command | Who | What |
|---|---|---|
| `/ascension` | Everyone | Essence bank + upgrade menu for the held item |
| `/ascension upgrade <enchant> <levels>` | Everyone | What the menu buttons run |
| `/ascension essence add/set <n>` | Ops | Testing and server admin |

## Testing

`tools/test/harness.js` runs every script in plain Rhino against small fakes of the
KubeJS API and runs 100 checks: recipes, Essence, tomes, upgrades, every enchant type, loot and tooltips:

```sh
curl -LO https://repo1.maven.org/maven2/org/mozilla/rhino/1.7.15/rhino-1.7.15.jar
java -jar rhino-1.7.15.jar -version 200 tools/test/harness.js
```

It catches syntax and logic errors, but can't prove the real KubeJS / Minecraft calls behave
the same. The in-game check in [INSTALL.md](INSTALL.md) is the real test.

Textures are generated by `python3 tools/make_textures.py`, and `docs/ENCHANTS.md` by
`java -jar rhino-1.7.15.jar -version 200 tools/make_enchant_docs.js`. Both read the config, so new enchants
added there get a tome texture and a docs entry automatically.

## License

MIT
