# Factory Ascension

**Website: https://thenameiscobmarley.github.io/factory-ascension/**

A Minecraft modpack setup (Forge 1.20.1) where your **Create factory powers your gear**.
The bigger the factory grows, the stronger your enchants get, with no level cap.
Want **Protection 3000**? Build a factory big enough to pay for it.
Mobs get scarier **the farther you go from spawn**, with better loot to match. Time doesn't matter,
so going AFK is always safe.

**Download:** [latest release](https://github.com/thenameiscobmarley/factory-ascension/releases/latest). Pick one:

| Download | What's in it |
|---|---|
| **Full** | The KubeJS scripts **plus** the Factory Ascension mod jar: the Modular Engine, Super Smelter, Salvager, Tome Fusion Table, Ascension Vault, Card Binder, Essence Forge, Pack Opener, Universal Storage Terminal, Ascension Workbench, Mini Crusher, connected glass, Shiny Reflections and 34 new mobs |
| **Half** | The KubeJS scripts only. Everything works except the mod's blocks |

Each comes as a plain zip and as a **CurseForge** zip you import straight into the CurseForge app (it downloads the mods for you).
**Install:** follow [INSTALL.md](INSTALL.md).

## The loop

```
day one: mine Ascendite -> Ascendite tools + armor, hand-made Ascension Cores, early brass
cobble generators -> mineral clusters -> ores -> diamonds, rose quartz, steel
   -> Ascension Cores (Create assembly line, or cheaper Ascendite recipes) -> absorb into your Essence bank
   -> Tomes unlock Ascended enchants on any gear -> spend Essence to level them up
   -> crush bosses and dungeons -> grow the factory -> repeat
```

## Ascended enchants

**487 enchants** (59 Ascended + 15 Mythic + 12 Ultimate + 200 Fusion + 200 Triad), separate from vanilla ones (vanilla caps levels at 255), so levels go as high as your
factory can pay for. They count from **every equipped item**, both hands and all armor, so any enchant
works on any gear. Full list with numbers and tome recipes: **[docs/ENCHANTS.md](docs/ENCHANTS.md)**.

| Group | Enchants |
|---|---|
| Damage | Ascended Sharpness, Smite, Bane, Power, Impaling, Giant Slayer, Hellbane, Raidbreaker, Skyhunter, First Strike, Executioner, Berserk, Assassin, Critical Edge, Reaper, Cleave |
| On hit | Lifesteal, Frostbite, Enfeeble, Withering, Venom, Skyward, Ascended Fire Aspect, Ascended Knockback, Second Wind, Adrenaline |
| On kill | Plunder, Soul Eater, Wisdom, Midas, Gem Hunter |
| Defense | Ascended Protection, Vitality (hidden health, no extra hearts), Fire / Blast / Arrow / Wither Ward, Featherweight, Spellward, Bulwark, Retribution, Evasion, Undying |
| Passive | Renewal, Night Sight, Gills, Fireproof, Industrious, Leaping, Sustenance, Magnetism, Restoration |
| Movement & utility | Swiftness, Reach, Stride, Frenzy, Lucky, Excavation, Eternal, Soulbound |
| **✦ Mythic** | Wings, Thunderlord, Black Hole, Meteor, Chronos, Phoenix, Voidwalker, Soul Harvest, Overkill, Rampage, Vein Miner, Timber, Radiance, Headhunter, Gravity Slam |

### ☄ Ultimates (12): cinematic combo moves

**Sneak + hit** an enemy and the game takes over. *Thunder God's Waltz*: you blink in a circle around the target slashing it
from 8 sides (always facing it), leap into the sky, call down three lightning strikes, it explodes, and you land exactly
where you started. The others:

| Ultimate | The move |
|---|---|
| Phantom Thousand Cuts | 16 afterimage slashes from every direction in under a second, then one final cut |
| Heaven Splitter | Launch the target into the sky, appear above it, slam it into the ground with a shockwave |
| Dragon Flame Spiral | Spiral up around the target in a pillar of fire, then a flame nova |
| Void Collapse | Drag every enemy around into one point, hold them, crush them all at once |
| Starfall | Leap high and rain ten meteors on the enemies around your target |
| Shadow Clone Barrage | Blink to every enemy nearby and strike each one, then return |
| Frozen Eternity | Freeze time around you, strike every frozen enemy, then shatter them all |
| Spirit Bomb | Gather a growing sphere of light overhead, then hurl it |
| Blade Storm Dance | Become a whirlwind of blades for three seconds |
| Eclipse Judgement | Blind every enemy, then a pillar of light falls on the target while you heal |
| Phoenix Dive | Soar up, then dive onto the target in a trail of fire and explode |

- Every strike deals the triggering hit's damage x the move's power, so all your other enchants make Ultimates stronger.
  Each level adds 25% power and takes a second off the cooldown (5 s minimum).
- You can't be hurt mid-combo (except by `/kill` and the void), take no fall damage from it, and always land back home.
  If the target dies the combo still finishes; if you log out you're put back first; a failsafe returns you even if a step fails.
- Combo hits never trigger other enchants or another Ultimate. Bosses still resist being frozen, launched or pulled.
- Tomes need **2 Ascended Souls** (a new drop only Ascended bosses have) + 4 Mythic Fragments, superheated. 10x Essence per level.

### ❖ Triad enchants (200)

Three base enchants fused into one tome, each at 45% strength, all in one enchant slot: *Stormforged Requiem*
(Berserk + Executioner + Sharpness), *Hallowed Aegis*, *Patient Vein*... 130 for weapons, 61 for armor, 9 for tools.
Tome: the three parent tomes + 2 Ascension Cores in a heated mixer. Levels cost 3x the Essence. Their covers split into
three bands with each parent's emblem. The full list is in [docs/ENCHANTS.md](docs/ENCHANTS.md).

### ⚗ Fusion enchants (200)

Two enchants fused into one tome: each Fusion does **both** parents' effects at 60% strength, in a single slot.
*Keen Bunker* is Ascended Sharpness + Blast Ward. Names come from the two parents
(an adjective from the first, a noun from the second): *Hallowed Pyre*, *Frozen Hoard*, *Grim Venom*, *Deathless Dagger*...
The tome is the two parent tomes + an Ascension Core in a heated mixer, and Fusion levels cost 2x the Essence.
Fusions are never made from Mythics or from enchants that need cleanup (attributes, passive effects, Eternal).
The full list is a plain array in the config (`fusions`), so you can add, rename or remove them.

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

## Legendary stars (Ascension Workbench)

Fallout 76-style grades for gear, from no stars to **10**. Put gear in the **Ascension Workbench** (two blocks wide) and
press **Star Up**: each star costs items from your inventory, adds a **legendary effect** (a random enchant that suits
the gear: level 10 at star 1, up to 6,000 at star 10) and makes every level on the item count x1.1 more per star (x2 at
10 stars, on top of Rebirth stars). The grade shows under the item's name: ★★★☆☆☆☆☆☆☆ LEGENDARY.

| Stars | Grade | Cost of that star |
|---|---|---|
| 1 | Legendary | 16 Ascension Cores, 8 Shards |
| 2-3 | Legendary | 4-8 Dense Cores, 1-2 Mythic Fragments |
| 4-6 | Epic Legendary | Dense Cores / Singularities, Mythic Fragments, Voidsteel then Celestium |
| 7-9 | Mythic Legendary | Singularities, Ascended Souls, Celestium then Ascendium |
| 10 | Ascended Legendary | 32 Singularities, 8 Ascended Souls, 8 Ascendium, a Nether Star (the server hears about it) |

The workbench is also where gear gets everything else: put gear on top and a tome, gem, attachment, Chaos Tome or
Polishing Cloth below, and it says what **Apply** will do before you press it. Half download: `/ascension legendary` on
the gear in your hand. Everything is in the `legendary` section of the config.

## Gems and gear sets

- **Gems** (Bloodstone, Sapphire, Garnet, Topaz, Opal, Onyx) are made in a superheated mixer, then compacted 4 → 1
  into Flawless and Perfect. Right-click one with gear in your other hand to socket it (3 sockets per item):
  +3 / +15 / +75 levels of their enchant. Sneak on a full item to replace the newest gem.
- **Sets**: wear 2 or 4 pieces with the same enchant for a bonus. *Juggernaut* (Protection), *Berserker* (Sharpness),
  *Plunderer* (Plunder), *Windwalker* (Swiftness), *Lifebinder* (Vitality). 2 pieces: x1.25 that enchant.
  4 pieces: x1.5 plus extra enchants (e.g. Juggernaut adds Vitality 100 and Bulwark 50).

## The Ascended Realm (Full download)

A dimension of floating islands made of Realm Stone and glowing Realm Moss, with three regions (the Ascended Shelf, the
Void Garden and the Astral Peaks) and the pack's toughest mobs. Build a **Realm Gate** (Crying Obsidian, Mythic Fragments,
Eyes of Ender and an Ascension Singularity) and right-click it: you arrive at the same x, z on a Realm Stone platform,
next to a gate that takes you back to where you left. Going through is free.

- Danger there starts at **12** (`danger.realmBonus`) and climbs from the Realm's 0, 0 like the End.
- Ores you won't find anywhere else: **Essence Crystal** (1-3 Ascension Cores), **Voidsteel** (Voidsteel Ingots) and
  **Astral** (1-2 Mythic Fragments). Fortune works on all of them; Silk Touch keeps the ore.
- Platform height and size are in `realm`. Two quests in the Endgame chapter lead there.

## The Ascension Shotgun (Full download)

A pump shotgun with a full 3D model. **Hold right-click** to aim down the sights (zoom, tighter spread), **left-click** to
fire (hold it to keep firing as fast as the pump racks), **R** to reload one shell at a time from your inventory. 8 pellets
x 5 damage, 6 shells, up to 40 blocks, less damage past 10. The pellets are your attack: Ascended tomes go on it like on
any weapon and your gear counts. It shatters glass. Shotgun Shells: 8 from paper, gunpowder, iron nuggets and a gold
nugget. Every number is in `guns`.

## Pets and charms

- **Ascension Collars** (5 tiers): right-click your own tamed pet (wolf, cat, parrot, horse...) with one. It gets the
  collar's health, attack and armor, heals fully, and stops taking fire and fall damage. Ascendite (60 health, 8 attack),
  Voidsteel (150 / 20), Celestium (400 / 50), Ascendium (1,000 / 150), Singularity (4,000 / 500). Each needs the one before.
- **Pet Whistle**: right-click to call your pets within 256 blocks (sitting ones stay put).
- **Charms**: carry one anywhere in your inventory: Haste, Swiftness, Owl (night vision), Ember (fire resistance), Tide
  (water breathing + dolphin's grace), Iron (resistance), Lucky, Leaping, Magnet (pulls items within 8 blocks). The
  **Ascended Charm** (eight charms + an Ascended Soul) does all of them, stronger, with a 16-block magnet. Charms never add hearts.

New in the Ascended Realm (Full download): **Astral Wisps** and **Void Knights**, and two bosses, the **Astral Sovereign**
and the **Void Behemoth**, each with a boss bar, minions and Ascended Souls (sometimes a Singularity) to drop.

## Quarry, Mob Grinder and Warp Hopper (Full download)

- **Ascension Quarry**: turn the shaft in its back and it mines out the 17 x 17 square under it (`machines.quarry.radius`),
  layer by layer, to the bottom of the world, like a diamond pickaxe. It skips bedrock, fluids, chests and machines.
  Faster spins dig faster. Put a chest on top for what it digs. Right-click it to see which layer it's on.
- **Mob Grinder**: turn its shaft and it hits every mob in the 5 x 5 x 3 box in front of it for 0.5 damage per RPM.
  Kills count as a player's, so you get rare drops and loot rolls. It never hits players, villagers, pets or named mobs.
  Put a chest under it for the drops.
- **Warp Hopper**: right-click it with any item to key it to that item. Items fed into it come out in the chest under every
  other Warp Hopper with the same key, at any distance and in any dimension.
- **Super Smelter**: two new tiers, 64 items at once (4 Singularities) and 128 at once (2 Ascended Souls).

## Structures (Full download)

Four new places to find, each with chests that join the lottery (packs, Booster Boxes and Gear Caches on top of their own loot):

| Structure | Where | Inside |
|---|---|---|
| **Factory Ruin** | Overworld surface | A collapsed Create workshop: Create parts, Ascendite and cores |
| **Ascension Crypt** | Overworld, underground | An Ascendite Skeleton or Husk spawner and two chests: gold, diamonds, cores, Chaos Tomes |
| **Sky Shrine** | On the Realm's islands | Crystal pillars around an offering chest: Mythic Fragments, cores, sometimes a Singularity |
| **Realm Vault** | Inside the Realm's islands | A Void Stalker or Void Archer spawner and two chests with the best loot: Singularities, Ascended Souls, Ascendium |

## World events

About once every 20 minutes something happens to the whole server:
- **Blood Moon** (nights only): everything spawns 10 danger levels higher, and Mythic Fragments drop twice as often.
- **Meteor Shower**: glowing loot falls from the sky near everyone, 2 meteors every 15 seconds. **Anything the pack makes can
  fall**: every item, block, tome, gem, gear tier, food, foil, Room TNT, the mod's machines (Full download), even Relics.
  Better things are rarer: each meteor picks a tier with the same odds as a loot drop where it lands (Common 60% ...
  Mythic 0.1% at home, much better far out, see the table below), then any item of that tier. Legendary and Mythic
  meteors announce themselves. The tiers of each kind of item are in `events.meteorShower.pool`.
- **Golden Hour**: mob loot is doubled.

`/ascension event` shows the current one. Ops can `/ascension event start <bloodMoon|meteorShower|goldenHour>` and `/ascension event stop`.

## Graphics (Full download)

**Pause > Factory Ascension...** has every graphics option, and each one switches off by itself if it ever fails (safe mode),
so the game keeps running:

- **Ascension Lighting**: screen-space traced lighting: rays marched through the depth of the frame give soft contact
  shadows in corners and under overhangs, light bounced off nearby surfaces in their colour, and light shafts from the sun
  and moon. Off / Low / Medium / High plus a strength slider. Standard OpenGL only (AMD and NVIDIA alike). It steps aside
  by itself when an Oculus shader pack is on.
- **Smoke**: soft, drifting smoke from explosions and gunfire.
- **Voxel debris**: broken and blown-up blocks shatter into small 3D pieces of their texture that tumble, bounce and settle.
- **Water**: foam where waterfalls land and where water flows, bubbles underwater, and a splash ring when something drops in.
- **Depth pack**: the **Make depth pack...** button builds a LabPBR resource pack on your PC from the textures your game has
  loaded (every block, item, mob and armor, from every mod), with a height and normal map for each, and turns it on.
  With **Oculus** and **Complementary Shaders** (both in the pack; Complementary starts in LabPBR mode) every surface gets
  real parallax depth. Nothing made from Mojang's textures is ever shipped: it's built locally.

Oculus also uses **R** to reload shaders, which is the shotgun's reload key; the mod unbinds Oculus's (once) so a reload
never freezes the game. Rebind it in Controls if you want it.

## Health checks

The mod watches memory (after garbage collection), open files and threads every 30 seconds and warns in the log and in
`logs/factoryascension-health.txt` if one keeps growing. `/ascension health` shows the numbers. The self-test and the
offline checks also look for leaks: saving the world repeatedly must not leave files or threads open, every file, zip and
image the mod opens is closed, and every thread says whether it may outlive the game.

## Soulbound

**Soulbound** (tome: Blank Tome + Eye of Ender + Echo Shard, heated) goes on any gear and keeps it with you: it stays in your
inventory when you die (it's back when you respawn), it can't be thrown out (it pops back into your inventory), and if it ever
ends up on the ground anyway it can't burn in lava or fire, blow up or despawn. You can still put it in a chest yourself.
To take it off, hold the gear in one hand and right-click an **Unbinding Scroll** (paper, Ender Pearls and an Ascension
Shard) in the other.

## Mob loot switch

Too many mob parts? **Sneak + swap hands (F)** (or `/ascension loot`) turns your mob loot enchants off: Plunder (and every
Fusion or Triad with it), Midas, Gem Hunter, the heads enchant and vanilla Looting stop adding drops. Do it again to turn
them back on. The action bar says which. Danger-zone loot rolls and Mythic Fragments still drop either way.

## Hordes and loot drops

**Hordes:** in the danger zones every hostile spawn brings extra copies of itself: +1 per 20 danger levels
(danger 60 = packs of 4), up to +6, in clear space beside it. Extras never bring their own extras or become bosses, and none
spawn while 60 hostiles are already within 48 blocks, so it stays playable. The extras are **15x stronger** than the mob that brought them
(they take damage / 15 and hit 15x as hard, on top of the zone's scaling: `horde.healthMult`, `horde.damageMult`).

**Loot drops:** danger-zone kills can drop a loot roll (12% at 1,000 blocks out, 24% at 2,000, 54% at 3,000, up to 75%).
Elites roll twice and lean a tier up, Ascended bosses roll four times and lean two up. The odds grow exponentially with
distance, on the same curve as the mobs, and bulk stacks grow with it (x2.4 at 2,000 blocks, x19 at 5,000):

| Tier | At home | 2,000 blocks | 5,000 blocks | 10,000 blocks | What drops |
|---|---|---|---|---|---|
| Common | 60% | 48% | 27% | 9% | Stacks of cores and mineral clusters (bigger stacks deeper out) |
| Uncommon | 25% | 25% | 17% | 7% | Dense Cores, Condensed Essence, base tomes, Chipped gems, **Rare Relics**, Enchantment Packs |
| Rare | 10% | 13.5% | 14% | 8% | Chaos Tomes, Flawless gems, Fusion tomes, Mythic Fragments, **Rare and Epic Relics**, packs |
| Epic | 4% | 9% | 17% | 15.5% | Perfect gems, Mythic tomes, Singularities, dense Condensed Essence, **Epic Relics**, Booster Boxes |
| Legendary | 0.9% | 3.8% | 15% | 24% | **Legendary Relics**, Ascended Souls, very dense Condensed Essence |
| Mythic | 0.1% | 1% | 10% | 36% | **Mythic Relics**, Ultimate tomes, Ascended Souls |

**Relics** are named, pre-enchanted gear, like *Dreadbound Greaves of Ruin*: Rare (iron, 1-2 enchants), Epic (diamond,
2-3 enchants), Legendary (diamond, 2-4) and Mythic (netherite, 4-6 plus a Mythic enchant). Their enchants suit what
they are (weapons roll offense, armor rolls defense, pickaxes roll utility), at levels that scale with the danger. Mythic Relics
also carry a Mythic enchant, a Chaos roll, Perfect gems and sometimes a star. Upgrade them like any other gear.
Epic and better drops glow, shoot a beam of light and play a sound; Legendary and Mythic ones put a title on your screen.
Try the odds on the [website](https://thenameiscobmarley.github.io/factory-ascension/#danger): the danger journey shows them at every
depth and can roll a sample drop.

## Ascended bosses

Deep in the danger zones (danger 30+), a rare spawn becomes an **Ascended boss**: a glowing, named, giant version of the
mob with a boss bar, x60 the zone's health and x3 its damage. At 66% and 33% health it summons minions and speeds up,
and every few seconds it slams nearby players away. Bosses ignore Slowness, Levitation, Weakness, Chronos and Black Hole,
and Reaper can't execute them. Killing one drops Chaos Tomes, Mythic Fragments and Dense Cores, and the whole server hears about it.

## Ascendite: the early-game ore

A new teal ore in the Overworld, Y -48 to 80 (most common around Y 16), in stone and deepslate. A stone pickaxe mines it.
It glows in the dark, and so do vanilla, Create and Mekanism ores (diamond and emerald brightest, coal faintest), so veins
stand out in caves. Light levels are in the `glowingOres` section of the config.

Haven't found any yet? Alloy an **Ascendite Ingot** by hand: iron ingot + copper ingot + 2 lapis + an amethyst shard.

- **Drops:** Raw Ascendite (more with Fortune), plus a 12% chance of an **Ascension Shard** (+4% per Fortune level).
  Silk Touch gives the ore block. It's tagged `forge:ores`, so Vein Miner works on it.
- **Processing:** smelt or blast Raw Ascendite into ingots, or crush it with Create (extra ore, XP, sometimes a shard),
  then wash Crushed Raw Ascendite for 9 nuggets. Ingot / nugget / block convert both ways.
- **Renewable later:** blow Crushed Raw Iron through soul fire with an Encased Fan: 30% becomes Crushed Raw Ascendite.
- **Mass production:** a Mechanical Press over a Basin presses **9 cobblestone into 1 Raw Ascendite** (Create's limit is 9
  items per basin recipe). Feed it from a cobblestone generator: 5,000 cobblestone is about 555 Raw Ascendite.
- **Tools and armor** (sword, pickaxe, axe, shovel, hoe, helmet, chestplate, leggings, boots): between iron and diamond.
  900 durability, mines diamonds, armor 2/6/7/2 + 1 toughness. They take Ascended enchants like any gear.

### Higher gear tiers

| Tier | Tools | Armor (full set) | Ingot (Create) |
|---|---|---|---|
| Voidsteel | 1,800 uses, +3.5 damage | 20 armor, 10 toughness | heated mixer: diamond + 2 Ascendite ingots + obsidian → 2 |
| Celestium | 3,200 uses, +5 damage | 24 armor, 16 toughness | superheated: netherite ingot + 2 Voidsteel + Dense Ascension Core → 2 |
| Ascendium | 6,000 uses, +7 damage | 28 armor, 24 toughness | superheated: 2 Celestium + Ascension Singularity + 4 Mythic Fragments + 1,000 mB Liquid Essence |

For comparison: diamond is 1,561 uses / +3 / 20 armor / 8 toughness, netherite 2,031 / +4 / 20 / 12. Each tier crafts like
iron gear from its ingot, takes Ascended enchants, and has its own look (Voidsteel violet, Celestium pearl and sky
blue, Ascendium crimson and molten orange). Numbers are in the `gearTiers` section of the config.

### Starting the factory (all need Ascendite)

| Makes | Recipe | Skips |
|---|---|---|
| 2 Brass Ingots | Copper ingot + zinc ingot + Ascendite nugget (crafting grid) | a Blaze Burner |
| 2 Andesite Alloy | 2 andesite + 2 Ascendite nuggets | half the nuggets |
| Precision Mechanism | 2 cogwheels + 2 golden sheets + Ascendite ingot (crafting grid) | sequenced assembly |
| Blaze Burner | Empty Blaze Burner + Block of Ascendite + magma block | catching a blaze |
| TFMG Steel Ingot | Iron ingot + coal + Ascendite nugget, heated mixer | TFMG's blast furnace |
| Blaze Rod | Lava bucket + stick + 2 redstone + gold ingot (bucket comes back) | the Nether |
| Waystone* | 3 stone bricks + Warp Stone + 3 cobbled deepslate | obsidian |
| Warp Stone* | 4 amethyst shards + 2 Ascendite ingots + 2 lapis + redstone | ender pearls and an emerald |
| 4 Warp Dust* | amethyst shard + Ascendite nugget | an ender pearl |
| 3 Warp Scrolls* | gold nuggets + purple dye + Ascendite nugget + paper | an ender pearl |

\* only when the [Waystones](https://www.curseforge.com/minecraft/mc-mods/waystones) mod is installed.

### More ways to make Ascension Cores (no sequenced assembly)

| Recipe | Where |
|---|---|
| 4 amethyst shards + 4 redstone + 1 gold ingot | crafting grid, from your first cave (no Ascendite needed) |
| 4 Ascendite ingots + 4 redstone + 1 lapis | crafting grid, from day one |
| 4 Ascension Shards | crafting grid, or a Mechanical Press over a basin |
| 2 Ascendite ingots + 1 redstone + 250 mB lava | heated mixer (automates) |
| 8 Ascendite nuggets + 1 redstone | Mechanical Mixer with **no heat**: the early automated way |

**Early automated cores (no Blaze Burner, runs on a Modular Engine):** cobblestone → Mechanical Press over a basin →
Raw Ascendite → Millstone (Crushed Raw Ascendite, 25% chance of a shard) → Encased Fan through water (9 nuggets +
50% redstone) → Mechanical Mixer (8 nuggets + 1 redstone → 1 core). Shards press into cores on the side.

The original diamond assembly line still works. All the numbers are in the `ascendite` section of `00_fa_config.js`.

### Ascendite gear

The tools and armor are gunmetal steel with glowing teal energy channels, gold guards, collars and trim, violet gems
and leather-wrapped grips with gold bands. Worn armor matches: a gold-trimmed helm with a glowing eye slit, gem and nose guard,
gold pauldrons, a teal V into the chest gem, a belt with a gem buckle, gold knee plates and capped boots with a glowing sole.

## Artifacts: one-of-one gear

An Artifact is more than enchants. It keeps everything it had, and gains:

- a generated name and title, like *Nyx'ash, Echo of the Void*, and a world serial number: **1 of 1 · #0007**
- a signature color and a particle aura while you hold it, and a lore line
- **3-4 quirks**, rolled for the kind of gear, each with a random strength, so no two Artifacts are the same:

| Quirk | Gear | Does |
|---|---|---|
| Echo Strike | weapon | 10-35% chance a hit lands twice |
| Evolving | any | +1-3% damage for every 100 kills it has made (it counts) |
| Nightstalker | weapon | +30-100% damage at night |
| Sunblessed | any | heals 1-3 health a second in daylight |
| Vortex | weapon | every 10 hits pulls enemies within 4-8 blocks into the target |
| Overcharge | weapon | every 8 hits a shockwave deals 50-150% of the hit around the target |
| Gilded | weapon | kills drop gold, 5-25% chance of an Ascension Core |
| Second Life | armor | a killing blow leaves you at half health instead (every 4-10 min) |
| Colossus | armor | +4-12 max health |
| Quickdraw | weapon | +10-40% attack speed |
| Soulbound | any | stays with you when you die |
| Featherfall | armor | no fall damage |
| Prospector | tool | 2-8% chance stone you mine drops Raw Ascendite |

**How to get one:** gear with 2,500 total enchant levels shows **[Awaken]** in `/ascension` (25,000 Essence + 1 Ascended
Soul). Or get lucky: 15% of Mythic Relic drops come out as Artifacts, announced to the server.

## Foils: shiny tomes that hit harder

A **foil** on an enchant tome multiplies the level it puts on gear, and changes how the tome looks so you can tell at a
glance:

| Foil | Power | Look |
|---|---|---|
| Bronze | x1.1 | a copper edge and sheen |
| Silver | x1.25 | a bright silver edge, two sheens |
| Gold | x1.5 | a thick gold frame |
| Holographic | x2 | rainbow stripes and a rainbow edge |
| Prismatic | x3 | a full rainbow wash |
| Galaxy | x5 | a purple night sky full of stars |
| Void | x8 | pitch black with a glowing violet edge |
| Celestial | x12 | white-gold rays and sparkles |
| **Cased:** Ruby, Sapphire, Emerald, Amber, Aurora | x15, x18, x22, x27, x33 | collector foils sealed in a tinted clear case, faceted gem patterns |
| **Cased:** Obsidian, Inferno, Glacier, Nebula, Eclipse | x40, x50, x62, x80, x100 | each made from the one before it |

- **Looted:** pack cards roll foils (8% of Commons, always on Holo cards and up, with better foils the rarer the card),
  and 15% of tomes from danger-zone loot come foiled.
- **Crafted:** a **Foil Sheet** for each foil (in a clear tinted wrapper you can see the foil through), from copper and
  paper up to Ascendium and a nether star, then the ten **Foil Cases** past it. Hold the sheet,
  put the tome in your other hand, right-click. A better foil replaces a worse one; a worse one is refused.
- A level 100 tome with Gold Foil goes on gear as level 150. Foiled tomes also sell for x their foil.
- Everything is in the `foils` section of the config.

## Jackpots: the rare insane find

- **Chests:** every chest the pack touches has a 1.5% jackpot roll for a huge stack: 10-40 Enchantment Packs, 4-16 Gear
  Caches, 2-6 Booster Boxes, or 64-192 Ascension Cores.
- **Enchant levels** on relics, gear caches and pack cards have a long lucky tail: most rolls are normal, about 1 in 40
  is 10x higher, 1 in 1,600 is 100x higher, and with truly insane luck a level can reach 100,000.
- Both are in the `jackpot` section of the config.

## Sealed Gear Caches: gear in chests

Vanilla chests now hold **Sealed Gear Caches**: dungeons, mineshafts, temples, shipwrecks, outposts, village weaponsmiths,
armorers and toolsmiths, strongholds, nether fortresses, bastions, mansions, ancient cities and end cities (the best
chests often have two). Right-click a cache to break the seal for one piece of relic gear. **Its quality depends on the
danger where you open it**: near spawn it's mostly Rare and Epic, but deep in the danger zones Legendary and Mythic gear
becomes common (and a Mythic one can come out as an Artifact). Odds are in the `caches` section of the config.

## Enchantment card packs

**Enchantment Packs** come in shimmering foil. Right-click one to open it: 5 tome "cards" are revealed one by one, and the
last is always Rare or better.

| Card | Odds | Tome |
|---|---|---|
| Common | 60% | an Ascended tome |
| Uncommon | 25% | an Ascended tome pre-leveled 5-25 |
| Rare | 10% | an Ascended tome pre-leveled 50-150 |
| ✦ Holo Rare | 3.5% | a Fusion tome, level 10-100 |
| ✦ Mythic Rare | 1.2% | a Mythic tome |
| ✦ Secret Rare | 0.3% | an Ultimate tome (announced to the server) |

A **Booster Box** holds 12 packs. Packs turn up in dungeon, mineshaft, temple, shipwreck, stronghold, nether fortress,
bastion, mansion, ancient city and end city chests, and in danger-zone loot. Boxes only in the best chests and Epic+ loot.

## Attachments: a light on your helmet, a chainsaw or an electric spike wrap on your sword

Craft an attachment, hold it with the gear in your other hand and right-click. Every piece of gear has **2 slots**, and
`/ascension` lists them with a **[Detach]** button that gives the attachment back. Armor attachments work while worn;
weapon, bow and tool ones while held.

| Gear | Attachment | Does | Recipe (crafting grid) |
|---|---|---|---|
| Helmet | Headlamp | a real light that follows you around (light 15) | iron, torch, glass pane, copper |
| Helmet | Rebreather | breathe underwater | iron, glass pane, 2 kelp |
| Helmet | Thermal Visor | enemies within 24 blocks glow through walls | glass pane, redstone, spider eye, Ascendite nugget |
| Chestplate | Jetpack | sneak in mid-air to fly up, glide down otherwise | 2 Ascendite ingots, 2 gunpowder, blaze rod |
| Chestplate | Armor Plating | +4 armor | 3 iron, 2 Ascendite ingots |
| Chestplate | Spike Plating | attackers take 25% of their hit back | 2 iron, 2 cactus |
| Leggings | Knee Pads | half fall damage | 2 leather, wool, iron |
| Leggings | Hydraulics | walk straight up full blocks | 2 pistons, Ascendite ingot |
| Boots | Rocket Boots | +25% speed | 2 Ascendite ingots, gunpowder, firework rocket |
| Boots | Spring Coils | Jump Boost II | 2 iron, 2 slime balls |
| Weapon | Chainsaw | +30% damage, rips into enemies next to the target | 2 iron, Ascendite ingot, chain, Create cogwheel |
| Weapon | Taser | 15% chance to stun for a second | 2 copper, 2 redstone, Ascendite nugget |
| Weapon | Flamethrower Nozzle | sets targets on fire for 5s | flint and steel, copper, blaze rod |
| Weapon | Serrated Edge | targets bleed (Wither) for 3s | 3 flint, iron |
| Weapon | ⚡ Electric Spike Wrap | +2 spike damage always; powered, every hit shocks for +6 and arcs to 2 enemies nearby (1 charge) | 2 iron, 2 copper, redstone, lightning rod |
| Weapon | ⚡ Plasma Edge | powered, +40% damage and sets targets on fire (1 charge) | 2 Ascendite ingots, blaze powder, redstone, glowstone dust |
| Weapon | ⚡ Overcharger | powered, every 5th hit releases a shock nova on everything within 4 blocks (5 charge) | Ascendite ingot, redstone block, lightning rod, Ascension Core |
| Weapon | ⚡ Tesla Guard | powered, anything that hits you while you hold it is zapped for 8 and knocked back (2 charge) | lightning rod, 2 copper, redstone block |
| Weapon | Vampire Fangs | heal 10% of the damage you deal | 2 bones, fermented spider eye, iron |
| Weapon | Weighted Pommel | +2 knockback | iron block, chain |
| Weapon | Targeting Lens | 20% chance a hit crits for x1.5 | spyglass, redstone, Ascendite nugget |
| Bow / crossbow | Scope | +25% arrow damage | spyglass, glass pane, iron |
| Pickaxe / shovel / hoe | Drill Bit | Haste II while held | 3 iron, diamond, Create cogwheel |
| Pickaxe / shovel / hoe | Mining Lamp | a light that follows you while held | 2 torches, iron, glass pane |

**⚡ Batteries:** powered attachments draw charge from a battery anywhere in your inventory, the emptiest one first. A
battery's durability bar is its charge. With every battery flat, powered effects stop (the Spike Wrap's spikes still cut)
and the action bar tells you. Creative players need no charge.

| Battery | Charge | Recipe | Recharge (crafting grid or Mechanical Mixer, no heat) |
|---|---|---|---|
| Copper Battery | 200 | 2 copper, 2 redstone, iron nugget | + 1 redstone |
| Ascendite Battery | 1,000 | 2 Ascendite ingots, redstone block, copper | + 1 redstone block |
| Essence Cell | 6,000 | Dense Ascension Core, 2 Ascendite ingots, glass, redstone block | + 1 Ascension Core |

Two part-used batteries of the same kind also combine in a crafting grid, like tools.

Everything is in the `attachments` section of the config (`list` and `batteries`).

## Selling: turn loot into Essence

Hold something and type `/ascension sell` (or click **[Sell held item]** in `/ascension`). You get a quote for the whole
stack and two buttons: **[Sell for Cores]** (paid in Singularities, Dense Cores and Cores, so it doesn't flood your
inventory) or **[Sell for Liquid Essence]** (buckets, 10 Essence each). Leftover fractions go into your Essence bank.

- **Tomes and cards:** 2 (base) / 6 (Fusion) / 25 (Mythic) / 80 (Ultimate) Essence, plus half of what their levels
  cost; holo cards are worth x1.5
- **Enchanted gear:** half of what its enchant levels cost (+500 for an Artifact, with a warning first)
- **Gems:** 3 / 15 / 75. **Boss and danger drops:** Mythic Fragment 8, Ascended Soul 60, Chaos Tome 12
- **Packs:** 6, Booster Box 80, Gear Cache 8. **Ingots:** Voidsteel 2, Celestium 10, Ascendium 60, netherite 8
- Every value is in the `sell` section of the config.

## Rare mob variants

Any mob can spawn as a rare variant. The rarer it is, the better it drops. Kills are announced in chat.

| Variant | Odds (per spawn) | What it does | Drops |
|---|---|---|---|
| Blazing / Frozen / Storm | 1 in 1,000 each (hostile mobs) | its hits set you on fire / slow you / call lightning for +20% damage | themed materials (blaze powder, packed ice, glowstone...) + 1 loot roll, x2 XP |
| ✦ Shiny | 1 in 1,000 | sparkles; a chime and a message when it spawns near you | a Chipped gem + 2 loot rolls one tier better, x5 XP |
| ★ Golden | 1 in 4,000 | gold sparkles, glows through walls, chime when it spawns | 1-2 gold blocks, 2-5 Ascension Cores, a Flawless gem, 3 loot rolls two tiers better, x10 XP |

Variants never despawn, so you can go back for one. Bosses, Elites and named mobs never become variants. Everything is in
the `variants` section of the config.

## Gear condition: Battle-Scarred to Pristine

Every tool, weapon and piece of armor (from any mod) rolls a **condition grade** once: when it's crafted, or the first
time you hold or wear it (so loot, villager trades and Create-crafted gear get one too). It shows in color right under
the item's name.

| Grade | Chance | Weapon damage | Armor (per piece) | Starts worn |
|---|---|---|---|---|
| Battle-Scarred | 10% | -15% | +4% damage taken | 30% of durability used |
| Well-Worn | 20% | -7% | +2% damage taken | 15% |
| Field-Tested | 35% | normal | normal | 5% |
| Minimal Wear | 24% | +6% | -1.5% damage taken | none |
| Mint Condition | 10% | +12% | -3% damage taken | none |
| ✦ Pristine | 1% | +25% | -6% damage taken | none |

A **Polishing Cloth** (white wool + 2 Ascendite nuggets + slime ball, makes 2) rerolls the grade of the gear in your
other hand and keeps whichever is better, so you can work your way up to Pristine. Everything is in the `condition`
section of the config.

## Rock Hammers: smash rock into ore

Craft a **Rock Hammer** (5 iron ingots + 2 sticks, 250 uses) or an **Ascendite Rock Hammer** (5 Ascendite ingots, 1,200 uses,
x1.6 luck on every drop, twice as fast).

- **By hand:** hammer in your main hand, rocks in the other, right-click to smash one. Sneak to smash 8.
- **In place:** right-click a placed rock block to smash it where it is. A Create **Deployer** holding a hammer does this
  too, so a cobblestone generator + a deployer is an automatic ore smasher.

| Rock | Can give |
|---|---|
| Cobblestone | gravel, flint, iron / copper / zinc / gold nuggets, coal, Raw Ascendite (3%) |
| Cobbled Deepslate | gravel, iron and gold nuggets, redstone, lapis, Raw Ascendite (6%), diamond (0.5%) |
| Gravel | sand, flint, iron and gold nuggets |
| Netherrack | quartz, gold nuggets, glowstone dust |
| Blackstone | gold nuggets, quartz, netherite scrap (0.3%) |
| End Stone | ender pearls, chorus fruit |

Every chance is in the `hammer` section of the config. JEI's info page on each hammer lists the exact odds.

## Mob food

**Every mob drops a body part** (45 parts across 63 vanilla mobs, plus one for each of the mod's 20 mobs; only pets,
villagers, golems and fish are spared). Raw or cooked, they can **always be eaten, even on a full hunger bar**. Eat one
**raw** and you get sick (Hunger II, often Nausea, sometimes Poison). **Cook** it in a furnace, smoker or campfire and
it's good food with a buff tied to the mob. A mob that dies burning drops it already cooked. Looting adds +5% per level.
Babies drop nothing. The table below is the original thirteen; every part is listed in the `mobFood` section of the
config (and on the website's Bestiary).

| Mob | Raw (chance) | Cooked | Cooked buff |
|---|---|---|---|
| Pig | Pig Eyes (30%) | Fried Pig Eyes | Night Vision 45s |
| Pig | Pig Brain (20%) | Roasted Pig Brain | Haste 30s |
| Cow, Mooshroom | Cow Tongue (30%) | Grilled Cow Tongue | 8 hunger + Saturation |
| Cow, Mooshroom | Cow Heart (15%) | Seared Cow Heart | Health Boost 60s |
| Sheep | Sheep Liver (25%) | Cooked Sheep Liver | Regeneration 6s |
| Chicken | Chicken Feet (35%) | Fried Chicken Feet | Slow Falling 20s |
| Rabbit | Rabbit Leg (35%) | Roasted Rabbit Leg | Jump Boost II 30s |
| Spider, Cave Spider | Spider Legs (30%) | Grilled Spider Legs | Speed 30s |
| Zombie, Husk, Drowned, Zombie Villager | Zombie Brain (15%) | Cooked Zombie Brain | Strength 20s |
| Skeleton, Stray | Marrow Bone (20%) | Bone Marrow Broth | Resistance 20s |
| Creeper | Creeper Heart (15%) | Smoked Creeper Heart | Absorption II 30s |
| Squid, Glow Squid | Squid Tentacle (40%) | Fried Calamari | Water Breathing 60s |
| Hoglin | Hoglin Ribs (35%) | BBQ Hoglin Ribs | 10 hunger + Fire Resistance 45s |

Some tomes need a matching part in their recipe: Night Sight (Fried Pig Eyes), Wisdom (Roasted Pig Brain),
Vitality (Seared Cow Heart), Sustenance (Grilled Cow Tongue), Renewal (Sheep Liver), Featherweight (Chicken Feet),
Leaping (Roasted Rabbit Leg), Bane (Spider Legs), Smite (Zombie Brain), Bulwark (Marrow Bone), Blast Ward
(Creeper Heart), Gills (Squid Tentacle) and Hellbane (Hoglin Ribs). Everything is in the `mobFood` section of the config.

## Factory Ascension blocks (Full download)

The Full download adds a Forge mod with twelve machine blocks, four kinds of connected glass and the Storage Linker, styled like the Ascendite gear. Find them in the
**Factory Ascension** creative tab or in JEI. Recipes and every number are in the `machines` section of the config.

### Modular Engine: power from day one

An **Engine Core** plus modules touching it, or touching each other. Coils, Gearboxes and Burners lie on their side
and join into one long engine rod (place them against the core's back, or facing along the rod). It's a real Create generator: put a
shaft on the core's front face.

| Module | What it does |
|---|---|
| **Coil** | +256 SU of stress capacity (about a water wheel), while a Burner powers it |
| **Gearbox** | Each one takes the engine to its next speed: 16, 32, 64, 128, 256 RPM. Same SU, faster shaft |
| **Burner** | Holds any furnace fuel (hoppers and chutes fill it). Each lit Burner powers 4 Coils; spare Burners wait |

Everything is made from iron, copper, a furnace and Create's andesite parts, so it's the first power you build.
Every part has its own screen (right-click it):

- **Core:** the whole engine: RPM and SU in big numbers, how many coils, gearboxes and burners (and how many are on),
  a modules-used bar, the speed ladder with the current speed lit, and what to do next.
- **Coil:** the SU each coil adds, how many coils are powered, and how many more Burners you need.
- **Gearbox:** the speed ladder, the next gearbox's speed, and a reminder that SU stays the same.
- **Burner:** a fuel slot, a flame gauge and the time left. Right-clicking with fuel in hand still puts it straight in.

A part that isn't touching an engine says "Not connected". Engineer's Goggles show the same numbers. One core per
engine: two cores touching the same modules jam. Up to 32 modules.

### Universal Storage Terminal (Full download)

One block that turns all your chests into one big chest. Craft a **Storage Linker**, right-click the **Universal Storage
Terminal** with it to set it, then right-click chests to link them (again to unlink). Chests, barrels, shulker boxes,
Ascension Vaults, Create vaults and any modded inventory with 9+ slots work; double chests count once. The terminal's
screen is the vault's: search box, Sort (merges and sorts across every linked chest), a scroll bar, shift-click in and out.
While you hold the linker, the terminal sparkles gold and its chests teal. Hoppers and funnels can feed the terminal too.
Up to 128 chests within 128 blocks, same dimension (`machines.storage`). Sneak + right-click the air clears the linker.

| Item | Recipe |
|---|---|
| Universal Storage Terminal | 4 Ascendite Ingots, 2 Chests, 1 Glass Pane, 1 Ascension Core, 1 Comparator |
| Storage Linker | Ascendite Ingot, Ascendite Nugget, Amethyst Shard (diagonal) |

### Ascension backpacks (Full download + Sophisticated Backpacks)

Six backpack tiers past Sophisticated Backpacks' Netherite one. They're real Sophisticated backpacks, so **every
Sophisticated upgrade works in them**, and you can wear, place and dye them. Each is crafted around the one before
(the first around a Netherite Backpack) and keeps everything inside.

| Tier | Storage slots | Upgrade slots | Crafted around the last one with |
|---|---|---|---|
| Ascendite | 162 | 7 | 8 Blocks of Ascendite |
| Voidsteel | 270 | 8 | 8 Voidsteel Ingots |
| Celestium | 432 | 9 | 8 Celestium Ingots |
| Ascendium | 648 | 10 | 4 Ascendium Ingots, 4 Dense Cores |
| Singularity | 1,080 | 12 | 8 Ascension Singularities |
| Ascended | 1,728 | 14 | 4 Ascended Souls, 3 Singularities, a Nether Star |

(Sophisticated's Netherite Backpack: 120 and 7.) Sizes are in the config (`backpacks`).

### Mini Crusher (Full download)

Create's crushing recipes (Millstone recipes if there's no crushing one) in one block, with **no crushing wheels and
no rotation**: put a **motor** in its motor slot and a **battery** in its battery slot (2 charge per item). Motors set the
speed: Electric Motor x1, Ascendite Motor x2, Essence Motor x4, each crafted from the one before. Hoppers and funnels
feed it from any side (batteries go into the battery slot) and take the results out. Config: `machines.miniCrusher`.

### 3D models

Every block the pack adds is a real 3D model built from parts: the vault's door and dial, the binder's slanted desk,
the forge's glass tank of essence, the terminal's tilted screen and keyboard, the workbench's tools and vise, the
Altar's pillar and crystals, and so on. Enchantment Packs are thick foil packs torn open at the top with cards peeking
out, Booster Boxes have a lid, Foil Sheets are cards in a clear wrapper, and Foil Cases are cards in a tinted case.

### The other machines

| Block | What it does |
|---|---|
| **Super Smelter** | A Create machine (shaft in the back). Smelts a batch at once: 4, 8, 16 or 32 items by tier, upgraded like the vault (8 Voidsteel, 8 Celestium, 4 Ascendium ingots). The faster it spins the faster it goes: at 64 RPM a batch takes 2 seconds. Hoppers and funnels work |
| **Salvager** | `/ascension sell` as a machine: loot in the top row, Ascension Cores out of the bottom row, at the same prices. Anything it can't sell passes straight through. It never grinds up an Artifact |
| **Tome Fusion Table** | Merge copies of one tome: their levels add up (foils are baked into the level). Or put in a Fusion's parent tomes and 1 core (2 for a Triad) and press Fuse: the Fusion starts at its weakest parent's level. It shows the result before you commit |

| Block | What it does |
|---|---|
| **Ascension Vault** | A storage chest with a search bar, a Sort button and a scroll bar. Starts at 108 slots; right-click it with 8 Voidsteel, then 8 Celestium, then 4 Ascendium ingots for +108 each (432 max). Hoppers and Create funnels work; breaking it drops what's inside |
| **Card Binder** | Holds 54 tomes (cards only). Shows how much of the collection you have, and floats your best card above the block |
| **Essence Forge** | Put gear in the slot and pipe or pour Liquid Essence in (16 buckets). It levels the gear by the same rules as the Ascension Altar, once a second |
| **Pack Opener** | Feed it Enchantment Packs (hoppers work). It opens one a second into 27 output slots and flips the cards on screen |

Without the jar (Half download) the pack still works; these blocks just don't exist.

### Easy start (both downloads)

Config `easyStart`: Andesite Casings by hand (any log + Andesite Alloy = 2), a Mechanical Belt from 3 leather,
4 cogwheels from 8 planks around an Andesite Alloy, and 8 shafts from an Andesite Alloy and a stick.

## Gear match: enchants go on gear they suit

Enchant levels add up from every slot, so a mismatched enchant still counted, but it made no sense: Sharpness on a helmet,
Protection on a pickaxe. Now a tome or a gem only goes on gear it suits ("Ascended Sharpness doesn't suit armor: it goes
on weapons."), and a Chaos Tome only rolls effects that suit the item you hold it against: weapons get damage and on-hit
effects, armor gets resistances and defenses, tools get mining effects, and an axe counts as a weapon and a tool.
Fusions and Triads suit gear if any of their parts does; Unbreakable-style effects suit anything. Turn it off with
`gearMatch.enabled: false` in the config.

## Skinwalkers

Out in the wilderness, not every animal is an animal. About 1 in 50 cows, pigs, sheep, chickens, horses, goats, llamas,
rabbits and foxes spawning at danger 2 or deeper (from your home) is a skinwalker. It looks and acts completely normal:
no name, no glow. When you come near it follows you, keeping its distance, and stops to stare. After at least 20 seconds
(and more likely at night), or the moment anything hits it, it shapeshifts: it freezes and twitches, its bones crack,
it rises off the ground, nearby players get Darkness and Nausea, and it bursts into its true form, only now named
**☠ Skinwalker**: a Ravager, Vindicator, Evoker, Zoglin, Wither Skeleton, Enderman (or, with the Full download, a Void
Stalker, Ascendite Husk, Frost Revenant or Factory Raider). It's always an Elite, 5 danger levels nastier than where it
turned, and sometimes a rare variant too. Killing it drops 2 extra loot rolls a tier better and maybe a Mythic Fragment.
Falls, lava, the void and `/kill` still hurt a disguised one normally; only a creature's hit makes it turn. Every number
is in the `skinwalkers` section of the config.

## Bestiary: 34 new mobs and 4 bosses (Full download)

Each is a vanilla mob's body and brain (so it moves and fights the way you'd expect) with its own skin, stats and
tricks. Hostile ones scale with the danger zones like any mob, and the strange ones only spawn out in them: natural
spawns closer to home than `modMobs.minDanger` in the config are cancelled. Every one drops its own body part.

**Bosses** turn up every now and then deep in the danger zones: drawn two to three times normal size, with a boss bar,
300-400 health (before danger scaling), knockback resistance and a signature attack (a ground slam, volleys of five
arrows, or rings of fireballs). At 2/3 and 1/3 health they call their minions; below 1/4 they enrage. Only some of their
natural spawns go through, and never near another of the same boss (`modMobs.bosses`). They drop an Ascended Soul,
Mythic Fragments, Dense Cores, a Booster Box and Gear Caches, and a part with a long, strong buff.

| Mob | Where | What it does |
|---|---|---|
| **Rust Walker** | Overworld, past the safe zone | A corroded factory hand. Its hits leave you weak; drops iron and copper nuggets. |
| **Ascendite Husk** | Overworld, danger 3+ | Glows teal, doesn't burn in daylight, makes you glow. Drops Ascension Shards. |
| **Ember Ghoul** | The Nether | Fireproof, sets you alight. Drops blaze powder and magma cream. |
| **Frost Revenant** | Snowy biomes | Slows you to a crawl. Drops packed ice and snowballs. |
| **Deep Miner** | Below Y 0 | Swings an iron pickaxe and gives you Mining Fatigue. Drops raw ore and coal. |
| **Void Archer** | The End | Its arrows make you float. Drops ender pearls. |
| **Ascendite Skeleton** | Overworld, danger 3+ | Arrows that make you glow. Drops Ascendite nuggets. |
| **Crystal Spider** | Caves below Y 40 | Armored amethyst; slows you. Drops amethyst shards. |
| **Spark Spider** | Overworld, danger 2+ | Crackles with lightning. Drops redstone and glowstone. |
| **Cinder Spider** | The Nether | Fireproof, sets you alight. Drops blaze powder and fire charges. |
| **Ascendite Creeper** | Overworld, danger 3+ | Its blast breaks no blocks and scatters Ascension Shards. |
| **Frost Creeper** | Snowy biomes | Its blast breaks no blocks: it leaves snow and slows everything nearby. |
| **Essence Slime** | Below Y 40, danger 5+ | A purple slime that splits like any other. Drops Ascension Shards. |
| **Rust Slime** | Below Y 40 | A copper-green slime. Drops copper nuggets. |
| **Ore Mite** | Below Y 20 | A silverfish full of metal. Drops iron, gold and Ascendite nuggets. |
| **Teal Wisp** | The Nether | A teal blaze. Drops Ascension Shards and glowstone. |
| **Void Stalker** | The End | A huge, dark enderman. Blinds you. Drops ender pearls. |
| **Factory Raider** | Forests, danger 4+ | A raider in work clothes; weakens you. Drops brass and andesite alloy. |
| **Ember Hen** | Savannas | Friendly and fireproof. Lays blaze powder now and then, as well as eggs. |
| **Copper Cow** | Overworld | Friendly. Moos in patina. Drops leather and copper ingots. |
| **Bog Lurker** | Swamps | Drags itself out of the mud; poisons what it touches. Drops slime balls and lily pads. |
| **Sand Wraith** | Deserts | Doesn't burn in the sun; blinds you. Drops sand and gold nuggets. |
| **Thunder Creeper** | Overworld | Its blast breaks no blocks: it calls lightning and jolts everything nearby. |
| **Brass Sentinel** | Overworld, danger 4+ | Armored in brass; its arrows slow you. Drops brass nuggets. |
| **Shadow Skeleton** | Forests, danger 3+ | Arrows that bring Darkness. Sometimes drops an echo shard. |
| **Cave Weaver** | Caves below Y 30 | Leaves cobwebs at your feet when it bites. Drops lots of string. |
| **Moss Slime** | Lush caves | A mossy slime. Drops moss and glow berries. |
| **Glow Mite** | Below Y 40 | A glowing silverfish; makes you glow. Drops glowstone dust. |
| **Storm Wisp** | Mountains, danger 6+ | A pale blaze that strikes with lightning. Drops redstone, glowstone and shards. |
| **Treasure Raider** | Overworld, danger 3+ (rare) | A raider loaded with loot: emeralds, gold, packs and gear caches. |
| **Rust Colossus** | BOSS · Overworld, danger 8+ | A towering rusted giant. Slams the ground, calls Rust Walkers and Deep Miners, enrages at 1/4 health. |
| **Broodmother** | BOSS · Forests, danger 8+ | A spider the size of a house. Poisons, webs, slams, and calls her brood. |
| **Hollow King** | BOSS · Overworld, danger 12+ | A crowned skeleton king. Wither arrows in volleys of five; calls Shadow and Ascendite Skeletons. |
| **Wisp Tyrant** | BOSS · The Nether, danger 10+ | A giant teal blaze. Rings of fireballs; calls Teal and Storm Wisps. |

## Connected glass and Shiny Reflections (Full download)

- **Connected glass** in four kinds (Factory, Ascendite, Gilded, Smoked): put panes of the same kind together and the
  frames between them disappear, so a wall of it reads as one big window. It's done with block models, not render code,
  so nothing can conflict with it. Recipe: 8 Glass around the center item, makes 8. Factory: Andesite Alloy,
  Ascendite: an Ascendite Nugget, Gilded: a Gold Nugget, Smoked: Coal (config `machines.glass`).
- **Shiny Reflections**: torches, lanterns, glowstone and the sun glint off gold, diamond, iron, copper, quartz, glass,
  ice, amethyst, polished stone, glazed terracotta and the mod's own glass and machines, and the glints slide across the
  surface as you move, the way a mirror image would. **Every block shines its own way:** metals tint their glints in
  their color and brushed metal stretches them into streaks, gems (diamond, emerald, amethyst, Ascendite, the Altar)
  twinkle with star sparkles, glass and ice glow more at grazing angles, and the sun's highlight is sharp on gems and
  polished stone, broad on metal. No shaders: twice a second it lists the shiny faces and lights near you, and each
  frame it draws a small glint where each light's mirror image lands. Shiny blocks are the `factoryascension:shiny`
  block tag (add your own with a datapack); radius, brightness and the glint cap are in
  `config/factoryascension-client.toml`.

## Conflicts

When another installed mod already does one of the pack's jobs, the pack turns its own version off and says so in chat
(and the log), for example: *"The Oculus mod conflicts with this mod's Shiny Reflections feature: Turned Shiny Reflections off."*

| Feature | Turned off when you have |
|---|---|
| Shiny Reflections | Oculus, Iris or OptiFine (shader mods) |
| Danger Zones | Scaling Health, Champions or Improved Mobs |
| Rare Mob Variants | Champions or Infernal Mobs |
| `/home` and `/sethome` | FTB Essentials (use `/ascension home` and `/ascension home set` instead) |

The list is the `conflicts` section of the config.

## Factory Room TNT: an underground room in one click

Right-click a block with one: after a 3 second fuse it blasts out a room **in front of you**, floor level with your
feet, then lines the floor, walls and roof with its block, sets lights into the roof every 6 blocks and leaves a
3x3 doorway where you're standing. It's built over a few ticks, roof first, so big rooms don't freeze the server or
drop sand on you.

| Size | Inside | Recipe |
|---|---|---|
| 16x16 | 16 x 16, 8 high | TNT + 4 wall blocks + 2 lights (crafting grid) |
| 32x32 | 32 x 32, 12 high | 4 of the 16x16 |
| 50x50 | 50 x 50, 16 high | 3 of the 32x32 |

| Style | Walls | Lights |
|---|---|---|
| Sandstone | Smooth Sandstone | Glowstone |
| Stone Brick | Stone Bricks | Sea Lanterns |
| Deepslate | Deepslate Tiles | Ochre Froglights |
| Quartz | Smooth Quartz | Sea Lanterns |

So a **50x50 Sandstone Light Factory TNT** gives you a 50 x 50 x 16 sandstone hall with glowstone in the ceiling.
It never breaks bedrock, chests, Create machines or anything else with an inventory or machinery inside, and ores it
blasts through drop in the middle of the room as ore blocks (ready for a crusher). Sizes, styles, spacing and speed are
in the `roomCharges` section of the config.

## Danger zones

**Danger is measured from your home.** Until you type `/home set`, nothing scales anywhere, so you can explore the map
and pick a base first. After that, mobs stay normal within 1,000 blocks of your home; past that, every 250 blocks adds a
**danger level**, with no cap. That's 3 in-between steps from one 1,000 blocks to the next, so it never jumps all at once.
Moving your home moves your zones (mobs that already spawned keep their level). A mob uses the home of the player it
spawns nearest to; near someone who hasn't set a home, mobs stay normal.

Crossing into a new zone puts its name and danger on screen as a big title, with a sound (a cave rumble going deeper).
A new danger level inside the same zone shows above the hotbar. Logging in out in a danger zone shows it too.

| Distance from home | Zone | Danger | Mob health, damage, drops and XP | Loot roll chance | Mythic loot odds |
|---|---|---|---|---|---|
| 0 - 1,000 | Safe Lands | 0 | normal | none | 0.1% |
| 1,000 | The Frontier | 1 | x1.5 | 12% | 0.2% |
| 1,250 / 1,500 / 1,750 | The Frontier | 2 / 3 / 4 | x1.9 / x2.6 / x3.4 | 13% - 19% | 0.3% - 0.7% |
| 2,000 | The Wilds | 5 | x4.5 | 24% | 1% |
| 3,000 | Dread Reaches | 9 | x11.9 | 54% | 2.8% |
| 5,000 | The Abyss | 17 | x47 | 75% | 10% |
| 10,000 | Nightmare | 37 | x322 | 75% | 36% |
| 20,000+ | The Void Beyond | 77+ | x2,245+ | 75% | 66%+ |

Mob toughness follows `x(1 + 0.5 * (blocks / 1000) ^ 2.807)`, measured where each level starts: x1.5 at 1,000 blocks and
x4.5 at 2,000 (`healthScale`, `damageScale` and `exponent` in the config). **Loot rides the same curve:** mob drops and
XP get the same multiplier (`lootScale`, `xpScale`), and loot rolls, their tiers, stack sizes, Relic levels and Mythic
Fragment chances all grow with `(blocks / 1000) ^ 2.807` too (the `loot` and `fragments` sections).

- **Elites:** 1% of mobs per danger level (max 25%) spawn as named Elites, like `☠ Elite Zombie · Lv 23`.
  They have x3 health, x1.5 damage and x3 loot on top of the zone's numbers.
- **The Nether** counts distance from your home x8 and is always at least danger 4. **The End** is always at least danger 7.
- Only hostile, non-boss mobs scale. Boss fights from other mods stay as their makers designed them.
- `/ascension danger` shows the danger where you're standing.
- Prefer one shared center? Set `centerOn: 'spawn'` in the config: then danger counts from world spawn, and ops can move
  it with `/ascension center set` / `reset` (saved with the world).
- Every number is in the `danger` section of `00_fa_config.js`. Set `enabled: false` to turn it off.

## How to use it

1. **Absorb cores:** right-click an Ascension Core for 1 Essence. Sneak + right-click absorbs the whole stack.
   Dense Cores give 9 and Singularities give 81.
2. **Unlock an enchant:** hold a Tome in one hand and gear in the other, then right-click the tome.
   It costs the tome and 1 Essence.
3. **Level it up:** hold the gear and type `/ascension`. Click **[+1] [+10] [+100] [+1000] [Max]**.

### Leveled tomes and extracting

Tomes level up on their own: hold a single tome and type `/ascension`, then click **[+1] [+10] [+100] [+1000] [Max]**,
same Essence prices as gear. A level 500 tome puts the enchant on gear at level 500 in one click. If the gear already
has that enchant at a lower level, the tome swaps in and the old level comes back to you as a tome, so nothing is lost.

To take an enchant off: hold the gear in one hand and a **Blank Ascension Tome** in the other, type `/ascension` and
click **[Extract]** next to the enchant. You get a tome at that exact level. Holding a leveled tome does nothing by
itself; it only counts once it's on gear.

The enchants show in the item's tooltip right under its name, with what they do, e.g.
`Ascended Sharpness 101  x11.10 damage`.

Every one of the 487 enchant tomes has its own cover: each base enchant has an emblem that shows what it does
(a sword for Sharpness, a spider for Bane, an hourglass for Chronos...), Mythic tomes get a gold frame and a glow,
Ultimates a black-and-crimson double frame with a ribbon, and Fusion tomes split the cover between both parents'
colors and emblems. The emblems are in `tools/tome_art.py`.

Every item the pack adds has a flavor line, and **Hold Shift** for details: where it comes from, its exact recipe,
and what it's for. Tomes also show what the enchant does at levels 10, 100 and 1,000 and the Essence it takes to get there.
Anything with tiers has a gold **⬆ Upgrade** line saying what the next tier needs: cores, gems, Condensed Essence, gear
(Ascendite → Voidsteel → Celestium → Ascendium), Rock Hammers, Room TNT, foils, the Vault, the Super Smelter and the
engine parts. Every mob part has its own line, raw and cooked. With the Full download the mod's blocks, glass and spawn
eggs have details too (eggs say where the mob lives, what it drops, and boss rules).

## Keeping your world safe

- **Backups (Full download):** every time a world closes, the mod zips it into `backups/factoryascension/` in your game
  folder, keeping the last 3 per world (`config/factoryascension-common.toml`). To restore one, unzip it into `saves/`.
- **Forgot a mod?** Each world remembers the mods that added its blocks and items. If one is missing when it loads,
  chat says *"Oops, it looks like you've forgotten to install (mod name). Save last files so you don't lose
  progress?"* with **[Save last files]** (quit, install it, and where the backups are) and **[Keep playing anyway]**.
- **The mod's own jar missing:** the scripts register placeholder crates under all of its block and item names, so
  Minecraft doesn't delete them from the world or your inventory. Install the jar again and they're back. What was
  stored inside its machines needs the backup.
- **Freeze safety net:** an entity whose position or speed becomes broken (NaN or infinite) is removed before mobs
  look for it, so it can't freeze the server. If a tick ever runs over 10 seconds anyway, the mod writes what it was
  doing to `logs/factoryascension-freeze.txt`.

## Quest book (FTB Quests)

The downloads include an FTB Quests quest book (FTB Quests, FTB Library and FTB Teams are in the CurseForge mod list):
six chapters, from your first Rock Hammer to 10-star Legendary gear and the Eclipse foil, with rewards. The Half
download's book skips the mod's blocks. It's generated by `tools/make_quests.py`.

## Recipes (all automatable with Create)

| Item | How |
|---|---|
| Ascension Core | Sequenced assembly on a diamond: deploy polished rose quartz, (deploy TFMG steel), fill 250 mB lava, press. 3 loops. Cheaper Ascendite recipes [above](#more-ways-to-make-ascension-cores-no-sequenced-assembly) |
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
| `/sethome` (or `/home set`) | Everyone | Save where you stand as your home (any dimension). It's also the middle of your danger zones |
| `/home` | Everyone | Teleport home. Not while in combat (10s after hitting or being hit), 30s cooldown |
| `/ascension awaken` | Everyone | Awaken the held gear into an Artifact (what the menu button runs) |
| `/ascension sell` | Everyone | Quote the held stack, then sell it for Cores or Liquid Essence |
| `/ascension detach <slot>` | Everyone | Take an attachment off the held gear (what the [Detach] button runs) |
| `/ascension milestones` | Everyone | Milestone progress |
| `/ascension rebirth` | Everyone | Rebirth the held item (asks to confirm) |
| `/ascension event` | Everyone | The current world event |
| `/ascension event start <name>` / `stop` | Ops | Start or stop a world event |
| `/ascension center` / `set` / `reset` | Ops | With `centerOn: 'spawn'`: show, move to you, or reset the shared safe-zone center |
| `/ascension essence add/set <n>` | Ops | Testing and server admin |
| `/ascension tome <levels>` | Everyone | What the tome menu buttons run (levels the tome in your main hand) |
| `/ascension extract <enchant>` | Everyone | Pull an enchant off gear into a tome (Blank Ascension Tome in the other hand) |
| `/ascension selftest` | Ops / console | Runs 66 checks (86 with the Full download's jar) of every system in the real game, far from players, then cleans up |

## Testing

`tools/test/harness.js` runs every script in plain Rhino against small fakes of the
KubeJS API and runs 520 checks, including a duplication fuzz test: recipes, Essence, tomes, upgrades, every enchant type, every Mythic, loot, danger zones and tooltips:

```sh
curl -LO https://repo1.maven.org/maven2/org/mozilla/rhino/1.7.15/rhino-1.7.15.jar
java -jar rhino-1.7.15.jar -version 200 tools/test/harness.js
```

It catches syntax and logic errors, but can't prove the real KubeJS / Minecraft calls behave
the same, so it also bans method names KubeJS hides or renames (found on a real server): use `stringUuid`,
`username`, `stack.nbt`, `entity.level`, `String(level.dimension)`, `attack(src, amount)`, `setPosition...`,
`source.actual` / `source.immediate`, `level.getBlock(pos).id` / `.hasTag(id)`, and `Fluid.of(id, mB)` for fluid
handlers (a Forge FluidStack from a script arrives empty). Scripts start with `// priority: N` so they load in order
on servers too. The real test is `/ascension selftest` in game (see [INSTALL.md](INSTALL.md)).

Textures are generated by `python3 tools/make_textures.py`: each is drawn at 16x16, then rendered at 32x32 with a detail
pass (smoothed edges, top-left lighting, carved inner detail, grain, a thin colored outline), and `docs/ENCHANTS.md` by
`java -jar rhino-1.7.15.jar -version 200 tools/make_enchant_docs.js`. Both read the config, so new enchants
added there get a tome texture and a docs entry automatically.

## License

MIT

## JEI

Every item, block and the Liquid Essence fluid show up in JEI, and every recipe shows under its machine's tab.
`client_scripts/20_fa_jei.js` adds information pages for things that don't come from a recipe (the Condenser, the Altar,
Condensed Essence, Mythic Fragments, Chaos Tomes, absorbing cores, socketing gems, every tome) and hides the assembly line's half-made core.

## Website

`site/` is the project website: a pixel-art book in the style of the pack's tome textures (covers and frames drawn by
`tools/site_art.py`, fonts Pixelify Sans and Silkscreen under the SIL Open Font License). One chapter is open at a time:
Home, Luck (a real pack opener), Gear, Enchants (browser, Mythic, Ultimates, Chaos Forge), Machines (with an engine
builder), World (danger zones) and Download. There's also a **[Gear Builder](https://thenameiscobmarley.github.io/factory-ascension/builder.html)**
(theorycraft all six slots with stars, gems, rolls and sets, then share the link) and a
**[Recipes](https://thenameiscobmarley.github.io/factory-ascension/recipes.html)** page (every item's full factory chain).
The builder's math (`site/calc.js`) is checked against the game engine by the test harness on 300 random builds.
It runs the pack's real config in the browser, so every number on it matches the game.
After changing the config or textures, rebuild it with `tools/build_site.sh [path/to/ui-vault]` and commit
(`FA_VERSION=x.y.z` points the download buttons at a release before it's tagged; `RHINO=path/to/rhino.jar` counts the checks).
Pushing to `main` deploys it to GitHub Pages, and the deploy fails if the site's copy of the config is out of date.

## Building the mod and the downloads

```sh
tools/mod_libs.sh                       # once: Create (and what it bundles) into mod/libs, to compile against
cd mod && ./gradlew build && cd ..      # mod/build/libs/factoryascension-<version>.jar (JDK 17)
python3 tools/make_mod_assets.py        # after changing block art, models or names
python3 tools/package.py 1.1.0          # dist/: Full and Half, plain and CurseForge-import zips
```
