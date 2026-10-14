# Factory Ascension

A Minecraft modpack setup (Forge 1.20.1) where your **Create factory powers your gear**.
The bigger the factory grows, the stronger your enchants get, with no level cap.
Want **Protection 3000**? Build a factory big enough to pay for it.
Mobs get scarier **the farther you go from spawn**, with better loot to match. Time doesn't matter,
so going AFK is always safe.

**Install:** follow [INSTALL.md](INSTALL.md).

## The loop

```
cobble generators -> mineral clusters -> ores -> diamonds, rose quartz, steel
   -> Ascension Cores (Create assembly line) -> absorb into your Essence bank
   -> Tomes unlock Ascended enchants on any gear -> spend Essence to level them up
   -> crush bosses and dungeons -> grow the factory -> repeat
```

## Ascended enchants

**74 enchants** (59 Ascended + 15 Mythic), separate from vanilla ones (vanilla caps levels at 255), so levels go as high as your
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
| **✦ Mythic** | Wings, Thunderlord, Black Hole, Meteor, Chronos, Phoenix, Voidwalker, Soul Harvest, Overkill, Rampage, Vein Miner, Timber, Radiance, Headhunter, Gravity Slam |

### ✦ Mythic enchants

The rare, crazy tier. Mythic tomes need **Mythic Fragments**, which only drop from mobs out in the
danger zones, mostly from Elites. So the factory makes you strong, and exploring makes you strange.

| Mythic | What it does |
|---|---|
| Wings | Creative-style flight while equipped (up to x2 flight speed) |
| Thunderlord | Lightning chains from your target to up to 16 nearby enemies |
| Black Hole | Hits pull every enemy around into your target |
| Meteor | Kills explode, hurting everything nearby for a share of the victim's max health |
| Chronos | Getting hit freezes every enemy around you for a few seconds |
| Phoenix | A killing blow revives you at full health in a firestorm instead (with a cooldown) |
| Voidwalker | Your arrows teleport you to whatever they hit |
| Soul Harvest | Every kill stores a soul on the weapon, and each soul adds damage. It grows forever |
| Overkill | Damage past what kills a mob jumps to the next enemy |
| Rampage | Each kill in a streak adds a big damage boost (up to 10 stacks) |
| Vein Miner / Timber | Break a whole ore vein or tree at once (sneak to break one block) |
| Radiance | Every enemy around you burns each second |
| Headhunter | Mobs drop their heads |
| Gravity Slam | No fall damage. Landing smashes everything nearby, harder the farther you fell |

Mythic levels cost 5x the Essence. Fragment drop rates are in [docs/ENCHANTS.md](docs/ENCHANTS.md#mythic).

A few at level 3000: Ascended Sharpness x301 damage, Ascended Protection -99.2% damage taken,
Vitality +6,000 hidden health, Plunder x151 loot, Excavation x751 mining speed.

Each level costs a bit more than the last: levels 1-20 cost 1 Essence each, level 3000 costs 151,
and getting one enchant from 0 to 3000 takes about 226K Essence.

## Danger zones

Near spawn, mobs are normal. Past 1000 blocks, every 500 blocks adds a **danger level**, with no cap.
Crossing into a new zone shows its name on screen. Far-out mobs are tougher, but drop more.

| Distance from spawn | Zone | Danger | Mobs | Loot |
|---|---|---|---|---|
| 0 - 1000 | Safe Lands | 0 | normal | normal |
| 1000+ | The Frontier | 1+ | x1.3 health | x1.1 |
| 3500 | The Wilds | 6 | x2.5 health, x1.9 damage | x1.6 |
| 6000 | Dread Reaches | 11 | x3.8 health, x2.7 damage | x2.1 |
| 11000 | The Abyss | 21 | x6.3 health, x4.2 damage | x3.1 |
| 21000 | Nightmare | 41 | x11 health, x7 damage | x5.1 |
| 41000+ | The Void Beyond | 81+ | x21+ health, x13+ damage | x9+ |

- **Elites:** 1% of mobs per danger level (max 25%) spawn as named Elites, like `☠ Elite Zombie · Lv 23`.
  They have x3 health, x1.5 damage and x3 loot on top of the zone's numbers.
- **The Nether** counts distance x8 and is always at least danger 3. **The End** is always at least danger 6.
- Only hostile, non-boss mobs scale. Boss fights from other mods stay as their makers designed them.
- `/ascension danger` shows the danger where you're standing.
- Every number is in the `danger` section of `00_fa_config.js`. Set `enabled: false` to turn it off.

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

**No duplication**
- Loot bonuses (Plunder, danger zones) only multiply plain stackable drops. Gear and anything with NBT,
  like an enchanted sword a zombie picked up, is never copied.
- Upgrades are all-or-nothing: the item and the Essence bank change together, or neither changes.
  A tome is only used up after its unlock fully succeeds. If absorbing cores fails, the cores come back.
- Core values must follow the 9 : 1 compression (1 : 9 : 81 Essence), so unpacking can never create Essence.
- A fuzz test runs 3,000 random absorbs, unlocks and upgrades and checks that every bit of Essence is
  accounted for: absorbed = bank + exactly what the levels on items cost.

**No corruption**
- Essence is saved in the world's KubeJS data, with a backup in the player's own file that survives death.
  If the world copy is ever lost, the backup restores it.
- Bad values (NaN, negative, infinite, out-of-range levels from hand-edited NBT) read as 0 and are never written.
- Only two tags are ever written to items: `FactoryAscension` and `Unbreakable` (from Eternal).
- Stat bonuses are temporary modifiers that are re-applied on login, so removing the scripts leaves no broken attributes.
- A config check runs at startup. A typo or bad number switches that one enchant or feature off,
  with a log line, instead of crashing or writing bad data.

**No runaway effects**
- `/kill` and the void still kill you, whatever your Protection, and still kill scaled mobs too.
- Cleave, Retribution, Meteor, Radiance, Gravity Slam and Phoenix damage can't trigger more enchants,
  so there are no chain reactions or infinite loops. Area effects only hit hostile mobs.
- Reaper never executes bosses. Evasion maxes out at 75%. Effect amplifiers stop at 128.
- Extra loot per kill is capped so high levels can't lag the server.
- Thunderlord's lightning is visual only: no fires, no charged creepers.
- Wings remembers it gave you flight, so taking it off removes flight again, even after a restart. Creative flight is never touched.
- Vein Miner and Timber break blocks the way you do (tool, Fortune, durability, claim protection), stop if the tool breaks, and have a block limit.
- A broken hook logs one error and steps aside instead of spamming the log.

## Commands

| Command | Who | What |
|---|---|---|
| `/ascension` | Everyone | Essence bank + upgrade menu for the held item |
| `/ascension upgrade <enchant> <levels>` | Everyone | What the menu buttons run |
| `/ascension danger` | Everyone | Danger level, zone and mob multipliers where you stand |
| `/ascension essence add/set <n>` | Ops | Testing and server admin |

## Testing

`tools/test/harness.js` runs every script in plain Rhino against small fakes of the
KubeJS API and runs 177 checks, including a duplication fuzz test: recipes, Essence, tomes, upgrades, every enchant type, every Mythic, loot, danger zones and tooltips:

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
