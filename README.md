# Factory Ascension

**Website: https://thenameiscobmarley.github.io/factory-ascension/**

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

## Chaos Tomes: random enchants

A **Chaos Tome** rolls a brand-new, randomly generated enchant onto the gear in your other hand:
a rarity, a generated name (like *Sanguine Rune of the Storm*) and 1 to 3 random effects.
Try rolling some on the [website](https://thenameiscobmarley.github.io/factory-ascension/#chaos). It runs the same generator,
so every seed there is a real enchant.

| Rarity | Odds | Effects | Strength | Upgrade cost |
|---|---|---|---|---|
| Common | 50% | 1 | x1 | x1 |
| Rare | 28% | 2 | x1.5 | x1.5 |
| Epic | 15% | 2 | x2.2 | x2.5 |
| Legendary | 6% | 3 | x3.2, can roll lightning, black holes, meteors and auras | x4 |
| Mythic | 1% | 3 | x5 | x8 |

- Recipe: blank tome + 4 Ascension Cores + an eye of ender in a superheated mixer.
- Up to 3 rolled enchants per item. Sneak + right-click on a full item rerolls its newest one, back to level 1.
- Rolled enchants level up with Essence in `/ascension`, with no level cap, like the others.
- Legendary and Mythic rolls are announced to the whole server.
- Rolls only use effects that clean up after themselves (no rolled flight, attributes or passive effects),
  and every effect a roll can make passes the same config check as the hand-made enchants (tested on 5,000 rolls).
- Rolls are made from a seed, so the same seed always gives the same enchant, in game and on the website.

## Liquid Essence and the Ascension Altar

Cores melt into **Liquid Essence** in a heated mixer (plus a splash of water): 1 core = 100 mB = 1 Essence, lossless.
Pipe it, store it in tanks, sneak-drink a bucket for 10 Essence, or feed an **Ascension Altar**:

```
[ Create Depot ]      <- gear goes here (a belt or Mechanical Arm can deliver it)
[ Ascension Altar ]
[ tank or basin ]     <- Liquid Essence
```

Once a second the altar buys levels for the gear on the depot, lowest enchant first (rolled ones too),
up to 20 levels a second, at the normal Essence prices. Your factory levels your gear while you're away.

## Condensed Essence (up to x50)

An **Essence Condenser** packs Liquid Essence into blocks you can carry around by the stack:

```
[ chest / Item Vault ]   <- Condensed Essence Blocks come out here
[ Essence Condenser ]
[ tank or basin ]        <- Liquid Essence
```

Up to 64 blocks a second (1 block = 1 bucket = 10 Essence). Each denser tier is 9 of the one below, compacted by a
Mechanical Press + Basin or crafted 3x3, and unpacked 1 → 9 in a crafting grid. There are 51 tiers, each with its own name:
*Condensed Essence Block*, then *Dense (x1)*, *Compact (x2)*, ... *Neutronium (x34)*, *Event Horizon (x39)*, *Black Hole (x41)*,
up to ***Absolute Essence Block (x50)***, which holds about 5 × 10⁴⁸ Essence. They're real placeable glowing blocks and stack to 64.
Right-click one in the air to absorb it; sneak to absorb the whole stack. Tier 0 melts back into its bucket in a heated mixer.

## Milestones

16 factory goals with rewards, like *Assembly Line* (1,000 Essence absorbed), *Megafactory* (100,000),
*Planet Eater* (10 million), *Gambler*, *Nightmare Walker* and *Godslayer*. Each pays out Chaos Tomes,
fragments or gems once. `/ascension milestones` shows your progress. Altar levels count even while you're offline.

## Prestige (Rebirth)

Once an item has 1,000 total levels, `/ascension rebirth` resets its enchants to level 1 for a permanent
**star**: every level on it then counts x1.25 (x1.5 at 2 stars, and so on). Each star needs 1,000 more levels than the last.

## Gems and gear sets

- **Gems** (Bloodstone, Sapphire, Garnet, Topaz, Opal, Onyx) are made in a superheated mixer, then compacted 4 → 1
  into Flawless and Perfect. Right-click one with gear in your other hand to socket it (3 sockets per item):
  +3 / +15 / +75 levels of their enchant. Sneak on a full item to replace the newest gem.
- **Sets**: wear 2 or 4 pieces with the same enchant for a bonus. *Juggernaut* (Protection), *Berserker* (Sharpness),
  *Plunderer* (Plunder), *Windwalker* (Swiftness), *Lifebinder* (Vitality). 2 pieces: x1.25 that enchant.
  4 pieces: x1.5 plus extra enchants (e.g. Juggernaut adds Vitality 100 and Bulwark 50).

## World events

About once every 20 minutes something happens to the whole server:
- **Blood Moon** (nights only): everything spawns 10 danger levels higher, and Mythic Fragments drop twice as often.
- **Meteor Shower**: glowing rare loot (diamonds, ancient debris, gem clusters, even Chaos Tomes) falls from the sky near everyone.
- **Golden Hour**: mob loot is doubled.

`/ascension event` shows the current one. Ops can `/ascension event start <bloodMoon|meteorShower|goldenHour>` and `/ascension event stop`.

## Ascended bosses

Deep in the danger zones (danger 30+), a rare spawn becomes an **Ascended boss**: a glowing, named, giant version of the
mob with a boss bar, x60 the zone's health and x3 its damage. At 66% and 33% health it summons minions and speeds up,
and every few seconds it slams nearby players away. Bosses ignore Slowness, Levitation, Weakness, Chronos and Black Hole,
and Reaper can't execute them. Killing one drops Chaos Tomes, Mythic Fragments and Dense Cores, and the whole server hears about it.

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
- A fuzz test runs 3,000 random absorbs, unlocks, upgrades and Chaos rolls and checks that every bit of Essence is
  accounted for: absorbed = bank + exactly what the levels on items cost (+ what rerolls threw away).

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
| `/ascension milestones` | Everyone | Milestone progress |
| `/ascension rebirth` | Everyone | Rebirth the held item (asks to confirm) |
| `/ascension event` | Everyone | The current world event |
| `/ascension event start <name>` / `stop` | Ops | Start or stop a world event |
| `/ascension essence add/set <n>` | Ops | Testing and server admin |

## Testing

`tools/test/harness.js` runs every script in plain Rhino against small fakes of the
KubeJS API and runs 256 checks, including a duplication fuzz test: recipes, Essence, tomes, upgrades, every enchant type, every Mythic, loot, danger zones and tooltips:

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

## JEI

Every item, block and the Liquid Essence fluid show up in JEI, and every recipe shows under its machine's tab.
`client_scripts/20_fa_jei.js` adds information pages for things that don't come from a recipe (the Condenser, the Altar,
Condensed Essence, Mythic Fragments, Chaos Tomes, absorbing cores, socketing gems, every tome) and hides the assembly line's half-made core.

## Website

`site/` is the project website, built with [UI Vault](https://github.com/thenameiscobmarley/ui-vault):
the home page (enchant browser, Chaos Forge, danger journey), a **[Gear Builder](https://thenameiscobmarley.github.io/factory-ascension/builder.html)**
(theorycraft all six slots with stars, gems, rolls and sets, then share the link) and a
**[Recipes](https://thenameiscobmarley.github.io/factory-ascension/recipes.html)** page (every item's full factory chain).
The builder's math (`site/calc.js`) is checked against the game engine by the test harness on 300 random builds.
It runs the pack's real config in the browser, so every number on it matches the game.
After changing the config or textures, rebuild it with `tools/build_site.sh [path/to/ui-vault]` and commit.
Pushing to `main` deploys it to GitHub Pages, and the deploy fails if the site's copy of the config is out of date.
