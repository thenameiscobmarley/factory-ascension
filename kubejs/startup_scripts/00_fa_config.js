// priority: 1000
// Factory Ascension - settings.
// This is a startup script: after editing, fully restart the game (/reload is not enough).

global.FA = {
  // ---- Essence ----
  // Absorbing cores fills your Essence bank (right-click; sneak + right-click for the whole stack).
  essenceValues: {
    'kubejs:ascension_core': 1,
    'kubejs:dense_ascension_core': 9,
    'kubejs:ascension_singularity': 81
  },

  // ---- Enchant upgrade cost ----
  // Essence to raise an Ascended enchant from level L to L+1 = ceil(costBase * (L + 1) ^ costPower).
  // Defaults: levels 1-20 cost 1 each, level 3000 costs 151, all the way to 3000 is about 226K Essence.
  costBase: 0.05,
  costPower: 1,
  mythicCostMultiplier: 5,     // Mythic enchants cost 5x as much per level
  ultimateCostMultiplier: 10,  // Ultimates cost 10x as much per level

  // ---- Ascended enchants ----
  // No level cap unless maxLevel is set. Enchants count from every equipped item (both hands
  // and all four armor slots), so any enchant can go on any gear. L = total level.
  //
  // Each enchant's "type" picks what it does (see startup_scripts/20_fa_enchant_effects.js):
  //   damage      damage dealt x(1 + perLevel * L) when `when` is true
  //   resist      damage taken / (1 + perLevel * L) for damage of kind `vs`
  //   hit_effect  gives the target `effect`, 1 amplifier step every `ampEvery` levels (max 128)
  //   self_effect gives you `effect` when you hit something
  //   passive     keeps `effect` on you, amplifier = L - 1
  //   attribute   +perLevel * L to each attribute in `attrs`, up to `cap`
  enchants: {
    // ---- Damage ----
    sharpness:    { name: 'Ascended Sharpness', type: 'damage', when: 'always', perLevel: 0.10 },
    smite:        { name: 'Ascended Smite', type: 'damage', when: 'undead', perLevel: 0.25 },
    bane:         { name: 'Ascended Bane', type: 'damage', when: 'arthropod', perLevel: 0.25 },
    giant_slayer: { name: 'Giant Slayer', type: 'damage', when: 'boss', perLevel: 0.15 },
    hellbane:     { name: 'Hellbane', type: 'damage', when: 'nether', perLevel: 0.2 },
    raidbreaker:  { name: 'Raidbreaker', type: 'damage', when: 'raider', perLevel: 0.25 },
    impaling:     { name: 'Ascended Impaling', type: 'damage', when: 'wet', perLevel: 0.25 },
    skyhunter:    { name: 'Skyhunter', type: 'damage', when: 'airborne', perLevel: 0.2 },
    first_strike: { name: 'First Strike', type: 'damage', when: 'full_health', perLevel: 0.3 },
    executioner:  { name: 'Executioner', type: 'damage', when: 'low_health', perLevel: 0.3 },
    berserk:      { name: 'Berserk', type: 'damage', when: 'berserk', perLevel: 0.3 },
    power:        { name: 'Ascended Power', type: 'damage', when: 'projectile', perLevel: 0.15 },
    assassin:     { name: 'Assassin', type: 'damage', when: 'sneaking', perLevel: 0.3 },
    critical:     { name: 'Critical Edge', type: 'critical', critMultiplier: 3 },   // chance L / (L + 100)
    reaper:       { name: 'Reaper', type: 'reaper', perLevel: 0.01, cap: 0.5 },     // kills non-bosses below this share of health
    cleave:       { name: 'Cleave', type: 'cleave', perLevel: 0.05, radius: 3 },    // share of each hit dealt to nearby hostiles

    // ---- On hit ----
    lifesteal:    { name: 'Lifesteal', type: 'lifesteal', perLevel: 0.01 },
    frostbite:    { name: 'Frostbite', type: 'hit_effect', effect: 'MOVEMENT_SLOWDOWN', label: 'Slowness', seconds: 3, ampEvery: 5 },
    enfeeble:     { name: 'Enfeeble', type: 'hit_effect', effect: 'WEAKNESS', label: 'Weakness', seconds: 5, ampEvery: 5 },
    withering:    { name: 'Withering', type: 'hit_effect', effect: 'WITHER', label: 'Wither', seconds: 4, ampEvery: 10 },
    venom:        { name: 'Venom', type: 'hit_effect', effect: 'POISON', label: 'Poison', seconds: 4, ampEvery: 10 },
    skyward:      { name: 'Skyward', type: 'hit_effect', effect: 'LEVITATION', label: 'Levitation', seconds: 2, ampEvery: 10 },
    fire_aspect:  { name: 'Ascended Fire Aspect', type: 'ignite', perLevel: 4 },     // seconds on fire
    knockback:    { name: 'Ascended Knockback', type: 'attribute', attrs: ['attack_knockback'], perLevel: 0.5, cap: 5, maxLevel: 10, label: 'knockback' },
    second_wind:  { name: 'Second Wind', type: 'hit_heal', perLevel: 0.5 },          // health per hit
    adrenaline:   { name: 'Adrenaline', type: 'self_effect', effect: 'MOVEMENT_SPEED', label: 'Speed', seconds: 3, ampEvery: 10, maxAmp: 4 },

    // ---- On kill ----
    plunder:      { name: 'Plunder', type: 'plunder', perLevel: 0.05 },
    soul_eater:   { name: 'Soul Eater', type: 'kill_heal', perLevel: 1 },            // health per kill
    wisdom:       { name: 'Wisdom', type: 'xp', perLevel: 0.1 },
    midas:        { name: 'Midas', type: 'drop', item: 'minecraft:gold_nugget', label: 'gold nuggets', perLevel: 0.2 },
    gem_hunter:   { name: 'Gem Hunter', type: 'drop', item: 'minecraft:emerald', label: 'emeralds', perLevel: 0.02 },

    // ---- Defense ----
    protection:   { name: 'Ascended Protection', type: 'resist', vs: 'all', perLevel: 0.04 },
    fire_ward:    { name: 'Fire Ward', type: 'resist', vs: 'fire', perLevel: 0.1 },
    featherweight:{ name: 'Featherweight', type: 'resist', vs: 'fall', perLevel: 0.1 },
    blast_ward:   { name: 'Blast Ward', type: 'resist', vs: 'explosion', perLevel: 0.1 },
    arrow_ward:   { name: 'Arrow Ward', type: 'resist', vs: 'projectile', perLevel: 0.1 },
    spellward:    { name: 'Spellward', type: 'resist', vs: 'magic', perLevel: 0.1 },
    wither_ward:  { name: 'Wither Ward', type: 'resist', vs: 'wither', perLevel: 0.1 },
    bulwark:      { name: 'Bulwark', type: 'resist', vs: 'melee', perLevel: 0.05 },
    vitality:     { name: 'Vitality', type: 'vitality', perLevel: 2 },               // hidden health, no extra hearts drawn
    retribution:  { name: 'Retribution', type: 'retribution', perLevel: 0.05 },
    evasion:      { name: 'Evasion', type: 'dodge', cap: 0.75 },                      // chance L / (L + 100), max 75%
    undying:      { name: 'Undying', type: 'undying', cooldown: 300, perLevel: 0.1, minCooldown: 3 },

    // ---- Passive ----
    renewal:      { name: 'Renewal', type: 'regen', perLevel: 0.25 },                // health per second
    night_sight:  { name: 'Night Sight', type: 'passive', effect: 'NIGHT_VISION', label: 'Night Vision', maxLevel: 1 },
    gills:        { name: 'Gills', type: 'passive', effect: 'WATER_BREATHING', label: 'Water Breathing', maxLevel: 1 },
    fireproof:    { name: 'Fireproof', type: 'passive', effect: 'FIRE_RESISTANCE', label: 'Fire Resistance', maxLevel: 1 },
    industrious:  { name: 'Industrious', type: 'passive', effect: 'DIG_SPEED', label: 'Haste', maxLevel: 5 },
    leaping:      { name: 'Leaping', type: 'passive', effect: 'JUMP', label: 'Jump Boost', maxLevel: 5 },
    sustenance:   { name: 'Sustenance', type: 'passive', effect: 'SATURATION', label: 'never hungry', maxLevel: 1 },
    magnetism:    { name: 'Magnetism', type: 'magnet', perLevel: 1, base: 2, cap: 32 }, // pull radius in blocks
    restoration:  { name: 'Restoration', type: 'repair', perLevel: 1 },              // durability per second, all gear
    soulbound:    { name: 'Soulbound', type: 'soulbound', maxLevel: 1 },             // kept on death, can't be dropped, lava-proof (an Unbinding Scroll takes it off)

    // ---- Movement & utility ----
    swiftness:    { name: 'Swiftness', type: 'attribute', attrs: ['movement_speed'], percent: true, perLevel: 0.01, cap: 0.5, label: 'speed' },
    reach:        { name: 'Reach', type: 'attribute', attrs: ['block_reach', 'entity_reach'], perLevel: 0.5, cap: 16, label: 'blocks reach' },
    stride:       { name: 'Stride', type: 'attribute', attrs: ['step_height'], perLevel: 0.5, cap: 3, maxLevel: 6, label: 'step height' },
    frenzy:       { name: 'Frenzy', type: 'attribute', attrs: ['attack_speed'], percent: true, perLevel: 0.1, cap: 10, label: 'attack speed' },
    lucky:        { name: 'Lucky', type: 'attribute', attrs: ['luck'], perLevel: 1, label: 'luck' },
    excavation:   { name: 'Excavation', type: 'mining', perLevel: 0.25 },
    eternal:      { name: 'Eternal', type: 'eternal', maxLevel: 1 },

    // ---- Mythic: rare and crazy ----
    // Mythic tomes need Mythic Fragments, which only drop from mobs out in the danger zones (mostly Elites).
    wings:        { name: 'Wings', type: 'flight', mythic: true, maxLevel: 3 },                           // creative-style flight
    thunderlord:  { name: 'Thunderlord', type: 'chain_lightning', mythic: true, perLevel: 0.02, radius: 8 },
    black_hole:   { name: 'Black Hole', type: 'gravity_well', mythic: true, perLevel: 1, base: 4, cap: 24 },
    meteor:       { name: 'Meteor', type: 'death_nova', mythic: true, perLevel: 0.1 },
    chronos:      { name: 'Chronos', type: 'time_stop', mythic: true },
    phoenix:      { name: 'Phoenix', type: 'phoenix', mythic: true, cooldown: 600, perLevel: 0.2, minCooldown: 30 },
    voidwalker:   { name: 'Voidwalker', type: 'projectile_teleport', mythic: true, maxLevel: 1 },
    soul_harvest: { name: 'Soul Harvest', type: 'souls', mythic: true, perLevel: 0.001 },               // per soul stored on the item
    overkill:     { name: 'Overkill', type: 'overkill', mythic: true, perLevel: 0.05, radius: 6 },
    rampage:      { name: 'Rampage', type: 'rampage', mythic: true, perLevel: 0.1, maxStacks: 10, seconds: 5 },
    vein_miner:   { name: 'Vein Miner', type: 'vein', mythic: true, blocks: 'ores', perLevel: 8, cap: 256 },
    timber:       { name: 'Timber', type: 'vein', mythic: true, blocks: 'logs', perLevel: 32, cap: 512 },
    radiance:     { name: 'Radiance', type: 'aura', mythic: true, perLevel: 2 },
    headhunter:   { name: 'Headhunter', type: 'heads', mythic: true, perLevel: 0.01 },
    gravity_slam: { name: 'Gravity Slam', type: 'slam', mythic: true, perLevel: 1 },

    // ---- Ultimates: cinematic combo moves ----
    // Sneak + hit an enemy to unleash one (then it needs `cooldown` seconds, minus 1 per level, at least 5).
    // Every hit in the combo deals the triggering hit's damage x `power`, x(1 + perLevel x (L - 1)), so your other
    // enchants make them stronger. You can't be hurt during a combo and always land back where you started.
    // Tomes need Ascended Souls, which only Ascended bosses drop.
    thunder_waltz:   { name: "Thunder God's Waltz", type: 'ultimate', ultimate: true, move: 'thunder_waltz', power: 0.6, perLevel: 0.25, cooldown: 40,
      desc: 'Blink in a circle around the target slashing it, leap into the sky, call down three lightning strikes, then it explodes and you land back home' },
    thousand_cuts:   { name: 'Phantom Thousand Cuts', type: 'ultimate', ultimate: true, move: 'thousand_cuts', power: 0.35, perLevel: 0.25, cooldown: 30,
      desc: 'Sixteen afterimage slashes from every direction in under a second, then one final cut' },
    heaven_splitter: { name: 'Heaven Splitter', type: 'ultimate', ultimate: true, move: 'heaven_splitter', power: 3, perLevel: 0.25, cooldown: 35,
      desc: 'Launch the target into the sky, appear above it, and slam it into the ground with a shockwave' },
    dragon_spiral:   { name: 'Dragon Flame Spiral', type: 'ultimate', ultimate: true, move: 'dragon_spiral', power: 0.5, perLevel: 0.25, cooldown: 35,
      desc: 'Spiral upward around the target in a pillar of fire, then a flame nova burns everything nearby' },
    void_collapse:   { name: 'Void Collapse', type: 'ultimate', ultimate: true, move: 'void_collapse', power: 2, perLevel: 0.25, cooldown: 45,
      desc: 'Drag every enemy around into one point, hold them there, then crush them all at once' },
    starfall:        { name: 'Starfall', type: 'ultimate', ultimate: true, move: 'starfall', power: 1.2, perLevel: 0.25, cooldown: 45,
      desc: 'Leap high and rain ten meteors onto the enemies around your target' },
    shadow_barrage:  { name: 'Shadow Clone Barrage', type: 'ultimate', ultimate: true, move: 'shadow_barrage', power: 1.5, perLevel: 0.25, cooldown: 30,
      desc: 'Blink to every enemy nearby and strike each one, then return in a puff of smoke' },
    frozen_eternity: { name: 'Frozen Eternity', type: 'ultimate', ultimate: true, move: 'frozen_eternity', power: 1.5, perLevel: 0.25, cooldown: 50,
      desc: 'Freeze time around you, strike every frozen enemy, then shatter them all' },
    spirit_bomb:     { name: 'Spirit Bomb', type: 'ultimate', ultimate: true, move: 'spirit_bomb', power: 4, perLevel: 0.25, cooldown: 60,
      desc: 'Gather a growing sphere of light overhead for two seconds, then hurl it at the target' },
    blade_storm:     { name: 'Blade Storm Dance', type: 'ultimate', ultimate: true, move: 'blade_storm', power: 0.6, perLevel: 0.25, cooldown: 35,
      desc: 'Become a whirlwind of blades for three seconds, cutting everything around you' },
    eclipse:         { name: 'Eclipse Judgement', type: 'ultimate', ultimate: true, move: 'eclipse', power: 3, perLevel: 0.25, cooldown: 55,
      desc: 'Blind every enemy, then a pillar of light falls on the target while you are healed' },
    phoenix_dive:    { name: 'Phoenix Dive', type: 'ultimate', ultimate: true, move: 'phoenix_dive', power: 2.5, perLevel: 0.25, cooldown: 40,
      desc: 'Soar into the sky, then dive onto the target in a trail of fire and explode on impact' }
  },

  // ---- Hordes: the farther out, the more mobs spawn together ----
  // Each hostile mob that spawns in a danger zone brings extraPerLevel x danger extra copies (fractions are a chance),
  // up to maxExtra, a tick later, in clear space next to it. No extras while nearbyCap hostiles are already within 48 blocks.
  horde: {
    enabled: true,
    extraPerLevel: 0.05,       // danger 20 -> +1 mob per spawn, danger 60 -> +3
    maxExtra: 6,
    nearbyCap: 60,
    radius: 3,
    // Horde mobs are much stronger than the mob that brought them (on top of the danger zone's scaling)
    healthMult: 15,            // they take damage / 15
    damageMult: 15             // and hit 15x as hard
  },

  // ---- Loot drops: danger-zone kills can drop a loot roll, Common to Mythic ----
  // The deeper the zone, the more often a kill drops one and the better the tier. Elites roll twice and lean a tier up,
  // Ascended bosses roll four times and lean two up. Bulk amounts grow with danger. Top tiers drop Relics:
  // pre-enchanted gear with big levels (upgradeable like any other gear).
  // Loot grows on the same exponential curve as the mobs (danger.exponent): curve = (blocks out / 1000) ^ 2.807,
  // so 1 at 1,000 blocks, 7 at 2,000, 22 at 3,000, 92 at 5,000, 640 at 10,000. Every "Scale" below multiplies it.
  loot: {
    enabled: true,
    chance: 0.1, chanceScale: 0.02, chanceCap: 0.75,   // chance a kill drops a roll: 12% at 1k, 24% at 2k, 54% at 3k, max 75%
    bulkScale: 0.2,            // stack sizes x(1 + 0.2 x curve): x1.2 at 1k, x2.4 at 2k, x19 at 5k
    relicLevelScale: 2,        // Relic enchant levels: (8 + 2 x curve) x the quality's levelMult
    condensedScale: 0.05,      // Condensed Essence drops reach one block tier higher per 20 curve
    // Tier weights: weight x (1 + curve) ^ (growth x tierCurve), so the rare tiers take over the deeper you go.
    // Mythic odds: 0.1% at home, ~1% at 2,000 blocks, ~10% at 5,000, ~36% at 10,000.
    tierCurve: 10,
    eliteRolls: 2, bossRolls: 4,
    tiers: [
      // weight at danger 0, and how fast it grows per danger level (higher tiers grow faster)
      { name: 'Common', color: 'white', weight: 60, growth: 0,
        drops: [['item', 'kubejs:ascension_core', 4, 12, 5], ['item', 'kubejs:gem_mineral_cluster', 8, 24, 3], ['item', 'kubejs:common_mineral_cluster', 16, 48, 3], ['gem', 1, 2]] },
      { name: 'Uncommon', color: 'green', weight: 25, growth: 0.01,
        drops: [['item', 'kubejs:dense_ascension_core', 2, 6, 4], ['condensed', 0, 0, 3], ['tome', 'base', 3], ['gem', 1, 2], ['item', 'kubejs:exotic_mineral_cluster', 8, 16, 2], ['item', 'kubejs:enchantment_pack', 1, 1, 2], ['relic', 'rare', 3]] },
      { name: 'Rare', color: 'aqua', weight: 10, growth: 0.025,
        drops: [['item', 'kubejs:chaos_tome', 1, 2, 4], ['gem', 2, 3], ['tome', 'fusion', 3], ['item', 'kubejs:mythic_fragment', 1, 3, 3], ['condensed', 1, 3, 2], ['item', 'kubejs:enchantment_pack', 1, 3, 3], ['relic', 'rare', 4], ['relic', 'epic', 2]] },
      { name: 'Epic', color: 'light_purple', weight: 4, growth: 0.05,
        drops: [['gem', 3, 3], ['tome', 'mythic', 3], ['item', 'kubejs:ascension_singularity', 1, 3, 2], ['condensed', 4, 8, 2], ['item', 'kubejs:chaos_tome', 3, 5, 2], ['item', 'kubejs:booster_box', 1, 1, 1], ['relic', 'epic', 5]] },
      { name: 'Legendary', color: 'gold', weight: 0.9, growth: 0.08,
        drops: [['relic', 'legendary', 5], ['item', 'kubejs:ascended_soul', 1, 1, 2], ['condensed', 10, 16, 2], ['tome', 'mythic', 2], ['item', 'kubejs:booster_box', 1, 2, 2]] },
      { name: 'Mythic', color: 'red', weight: 0.1, growth: 0.12,
        drops: [['relic', 'mythic', 5], ['tome', 'ultimate', 2], ['condensed', 20, 30, 2], ['item', 'kubejs:ascended_soul', 2, 3, 1]] }
    ],
    // Relics: gear rolled with enchants that match what it is (weapons get offense, armor defense, tools utility)
    // metal: iron, diamond or netherite gear
    relics: {
      rare: { metal: 'iron', enchants: [1, 2], levelMult: 0.5, mythicEnchants: 0, gems: [0, 0], rolled: 0, starChance: 0, color: 'aqua' },
      epic: { metal: 'diamond', enchants: [2, 3], levelMult: 0.75, mythicEnchants: 0, gems: [0, 1], rolled: 0, starChance: 0, color: 'light_purple' },
      legendary: { metal: 'diamond', enchants: [2, 4], levelMult: 1, mythicEnchants: 0, gems: [0, 1], rolled: 0, starChance: 0, color: 'gold' },
      mythic: { metal: 'netherite', enchants: [4, 6], levelMult: 3, mythicEnchants: 1, gems: [1, 3], rolled: 1, starChance: 0.35, color: 'red' }
    }
  },

  // ---- Mythic Fragments: drop from danger-zone mobs, needed for Mythic tomes ----
  fragments: {
    // On the loot curve (see loot): Elites 15% + 0.5% x curve (max 75%), other danger-zone mobs 0.2% x curve (max 5%)
    eliteChance: 0.15,
    eliteChanceScale: 0.005,
    eliteChanceCap: 0.75,
    normalChanceScale: 0.002,
    normalChanceCap: 0.05
  },

  // ---- Fusion enchants: two enchants fused into one tome ----
  // Each does both of its parents' effects at fusionStrength of their power. Tome: Tome A + Tome B + an Ascension Core
  // in a heated mixer. Costs fusionCostMultiplier x the Essence per level. [id, name, parent A, parent B]
  fusionStrength: 0.6,
  fusionCostMultiplier: 2,
  fusions: [
    ['hallowed_pyre', 'Hallowed Pyre', 'smite', 'fire_aspect'],
    ['crawling_vengeance', 'Crawling Vengeance', 'bane', 'retribution'],
    ['infernal_insight', 'Infernal Insight', 'hellbane', 'wisdom'],
    ['siegebreaking_anvil', 'Siegebreaking Anvil', 'raidbreaker', 'restoration'],
    ['skyborne_feather', 'Skyborne Feather', 'skyhunter', 'featherweight'],
    ['merciless_venom', 'Merciless Venom', 'executioner', 'venom'],
    ['raging_rampart', 'Raging Rampart', 'berserk', 'bulwark'],
    ['shadowed_hoard', 'Shadowed Hoard', 'assassin', 'plunder'],
    ['precise_bloom', 'Precise Bloom', 'critical', 'renewal'],
    ['sweeping_aegis', 'Sweeping Aegis', 'cleave', 'protection'],
    ['vampiric_purge', 'Vampiric Purge', 'lifesteal', 'smite'],
    ['enervating_ward', 'Enervating Ward', 'enfeeble', 'spellward'],
    ['blighted_trident', 'Blighted Trident', 'withering', 'impaling'],
    ['soaring_shadowstep', 'Soaring Shadowstep', 'skyward', 'evasion'],
    ['burning_volley', 'Burning Volley', 'fire_aspect', 'power'],
    ['rallying_fortune', 'Rallying Fortune', 'second_wind', 'midas'],
    ['frenzied_drill', 'Frenzied Drill', 'adrenaline', 'excavation'],
    ['greedy_thirst', 'Greedy Thirst', 'plunder', 'lifesteal'],
    ['soulbound_bunker', 'Soulbound Bunker', 'soul_eater', 'blast_ward'],
    ['gilded_updraft', 'Gilded Updraft', 'midas', 'skyward'],
    ['gleaming_heart', 'Gleaming Heart', 'gem_hunter', 'vitality'],
    ['warded_guillotine', 'Warded Guillotine', 'protection', 'executioner'],
    ['fireproof_harvest', 'Fireproof Harvest', 'fire_ward', 'soul_eater'],
    ['weightless_lodestar', 'Weightless Lodestar', 'featherweight', 'magnetism'],
    ['blastproof_scythe', 'Blastproof Scythe', 'blast_ward', 'reaper'],
    ['spellproof_bane', 'Spellproof Bane', 'spellward', 'bane'],
    ['unwithering_blight', 'Unwithering Blight', 'wither_ward', 'withering'],
    ['hearty_falcon', 'Hearty Falcon', 'vitality', 'skyhunter'],
    ['vengeful_rush', 'Vengeful Rush', 'retribution', 'adrenaline'],
    ['deathless_dagger', 'Deathless Dagger', 'undying', 'assassin'],
    ['verdant_gem', 'Verdant Gem', 'renewal', 'gem_hunter'],
    ['magnetic_edge', 'Magnetic Edge', 'magnetism', 'sharpness'],
    ['mending_frost', 'Mending Frost', 'restoration', 'frostbite'],
    ['delving_shield', 'Delving Shield', 'excavation', 'arrow_ward'],
    ['hallowed_gem', 'Hallowed Gem', 'smite', 'gem_hunter'],
    ['titanslaying_frost', 'Titanslaying Frost', 'giant_slayer', 'frostbite'],
    ['infernal_shield', 'Infernal Shield', 'hellbane', 'arrow_ward'],
    ['tidal_pyre', 'Tidal Pyre', 'impaling', 'fire_aspect'],
    ['skyborne_vengeance', 'Skyborne Vengeance', 'skyhunter', 'retribution'],
    ['merciless_insight', 'Merciless Insight', 'executioner', 'wisdom'],
    ['raging_anvil', 'Raging Anvil', 'berserk', 'restoration'],
    ['shadowed_feather', 'Shadowed Feather', 'assassin', 'featherweight'],
    ['grim_venom', 'Grim Venom', 'reaper', 'venom'],
    ['sweeping_rampart', 'Sweeping Rampart', 'cleave', 'bulwark'],
    ['vampiric_opening', 'Vampiric Opening', 'lifesteal', 'first_strike'],
    ['frozen_hoard', 'Frozen Hoard', 'frostbite', 'plunder'],
    ['enervating_bloom', 'Enervating Bloom', 'enfeeble', 'renewal'],
    ['blighted_precision', 'Blighted Precision', 'withering', 'critical'],
    ['venomous_aegis', 'Venomous Aegis', 'venom', 'protection'],
    ['soaring_purge', 'Soaring Purge', 'skyward', 'smite'],
    ['rallying_ward', 'Rallying Ward', 'second_wind', 'spellward'],
    ['frenzied_trident', 'Frenzied Trident', 'adrenaline', 'impaling'],
    ['greedy_rally', 'Greedy Rally', 'plunder', 'second_wind'],
    ['soulbound_shadowstep', 'Soulbound Shadowstep', 'soul_eater', 'evasion'],
    ['sage_volley', 'Sage Volley', 'wisdom', 'power'],
    ['gleaming_drill', 'Gleaming Drill', 'gem_hunter', 'excavation'],
    ['warded_thirst', 'Warded Thirst', 'protection', 'lifesteal'],
    ['weightless_hellfire', 'Weightless Hellfire', 'featherweight', 'hellbane'],
    ['blastproof_updraft', 'Blastproof Updraft', 'blast_ward', 'skyward'],
    ['spellproof_guillotine', 'Spellproof Guillotine', 'spellward', 'executioner'],
    ['unwithering_harvest', 'Unwithering Harvest', 'wither_ward', 'soul_eater'],
    ['stalwart_lodestar', 'Stalwart Lodestar', 'bulwark', 'magnetism'],
    ['hearty_scythe', 'Hearty Scythe', 'vitality', 'reaper'],
    ['elusive_bane', 'Elusive Bane', 'evasion', 'bane'],
    ['deathless_blight', 'Deathless Blight', 'undying', 'withering'],
    ['verdant_remedy', 'Verdant Remedy', 'renewal', 'wither_ward'],
    ['magnetic_falcon', 'Magnetic Falcon', 'magnetism', 'skyhunter'],
    ['mending_rush', 'Mending Rush', 'restoration', 'adrenaline'],
    ['delving_phylactery', 'Delving Phylactery', 'excavation', 'undying'],
    ['keen_blight', 'Keen Blight', 'sharpness', 'withering'],
    ['hallowed_remedy', 'Hallowed Remedy', 'smite', 'wither_ward'],
    ['titanslaying_rush', 'Titanslaying Rush', 'giant_slayer', 'adrenaline'],
    ['infernal_phylactery', 'Infernal Phylactery', 'hellbane', 'undying'],
    ['tidal_gem', 'Tidal Gem', 'impaling', 'gem_hunter'],
    ['swift_frost', 'Swift Frost', 'first_strike', 'frostbite'],
    ['merciless_shield', 'Merciless Shield', 'executioner', 'arrow_ward'],
    ['shadowed_vengeance', 'Shadowed Vengeance', 'assassin', 'retribution'],
    ['grim_insight', 'Grim Insight', 'reaper', 'wisdom'],
    ['sweeping_anvil', 'Sweeping Anvil', 'cleave', 'restoration'],
    ['vampiric_cleaver', 'Vampiric Cleaver', 'lifesteal', 'cleave'],
    ['frozen_feather', 'Frozen Feather', 'frostbite', 'featherweight'],
    ['enervating_slayer', 'Enervating Slayer', 'enfeeble', 'giant_slayer'],
    ['venomous_rampart', 'Venomous Rampart', 'venom', 'bulwark'],
    ['soaring_opening', 'Soaring Opening', 'skyward', 'first_strike'],
    ['burning_hoard', 'Burning Hoard', 'fire_aspect', 'plunder'],
    ['rallying_bloom', 'Rallying Bloom', 'second_wind', 'renewal'],
    ['frenzied_precision', 'Frenzied Precision', 'adrenaline', 'critical'],
    ['greedy_aegis', 'Greedy Aegis', 'plunder', 'protection'],
    ['soulbound_purge', 'Soulbound Purge', 'soul_eater', 'smite'],
    ['sage_malaise', 'Sage Malaise', 'wisdom', 'enfeeble'],
    ['gilded_ward', 'Gilded Ward', 'midas', 'spellward'],
    ['warded_rally', 'Warded Rally', 'protection', 'second_wind'],
    ['weightless_volley', 'Weightless Volley', 'featherweight', 'power'],
    ['blastproof_fortune', 'Blastproof Fortune', 'blast_ward', 'midas'],
    ['spellproof_thirst', 'Spellproof Thirst', 'spellward', 'lifesteal'],
    ['stalwart_hellfire', 'Stalwart Hellfire', 'bulwark', 'hellbane'],
    ['hearty_updraft', 'Hearty Updraft', 'vitality', 'skyward'],
    ['elusive_guillotine', 'Elusive Guillotine', 'evasion', 'executioner'],
    ['deathless_harvest', 'Deathless Harvest', 'undying', 'soul_eater'],
    ['magnetic_scythe', 'Magnetic Scythe', 'magnetism', 'reaper'],
    ['mending_flamewall', 'Mending Flamewall', 'restoration', 'fire_ward'],
    ['delving_bane', 'Delving Bane', 'excavation', 'bane'],
    ['keen_harvest', 'Keen Harvest', 'sharpness', 'soul_eater'],
    ['hallowed_lodestar', 'Hallowed Lodestar', 'smite', 'magnetism'],
    ['titanslaying_flamewall', 'Titanslaying Flamewall', 'giant_slayer', 'fire_ward'],
    ['siegebreaking_blight', 'Siegebreaking Blight', 'raidbreaker', 'withering'],
    ['tidal_remedy', 'Tidal Remedy', 'impaling', 'wither_ward'],
    ['swift_rush', 'Swift Rush', 'first_strike', 'adrenaline'],
    ['merciless_phylactery', 'Merciless Phylactery', 'executioner', 'undying'],
    ['piercing_gem', 'Piercing Gem', 'power', 'gem_hunter'],
    ['precise_frost', 'Precise Frost', 'critical', 'frostbite'],
    ['grim_shield', 'Grim Shield', 'reaper', 'arrow_ward'],
    ['frozen_vengeance', 'Frozen Vengeance', 'frostbite', 'retribution'],
    ['enervating_fury', 'Enervating Fury', 'enfeeble', 'berserk'],
    ['blighted_insight', 'Blighted Insight', 'withering', 'wisdom'],
    ['venomous_anvil', 'Venomous Anvil', 'venom', 'restoration'],
    ['soaring_cleaver', 'Soaring Cleaver', 'skyward', 'cleave'],
    ['burning_feather', 'Burning Feather', 'fire_aspect', 'featherweight'],
    ['rallying_slayer', 'Rallying Slayer', 'second_wind', 'giant_slayer'],
    ['greedy_rampart', 'Greedy Rampart', 'plunder', 'bulwark'],
    ['soulbound_opening', 'Soulbound Opening', 'soul_eater', 'first_strike'],
    ['gilded_bloom', 'Gilded Bloom', 'midas', 'renewal'],
    ['gleaming_precision', 'Gleaming Precision', 'gem_hunter', 'critical'],
    ['fireproof_purge', 'Fireproof Purge', 'fire_ward', 'smite'],
    ['weightless_malaise', 'Weightless Malaise', 'featherweight', 'enfeeble'],
    ['deflecting_trident', 'Deflecting Trident', 'arrow_ward', 'impaling'],
    ['stalwart_volley', 'Stalwart Volley', 'bulwark', 'power'],
    ['hearty_fortune', 'Hearty Fortune', 'vitality', 'midas'],
    ['vengeful_drill', 'Vengeful Drill', 'retribution', 'excavation'],
    ['elusive_thirst', 'Elusive Thirst', 'evasion', 'lifesteal'],
    ['verdant_hellfire', 'Verdant Hellfire', 'renewal', 'hellbane'],
    ['magnetic_updraft', 'Magnetic Updraft', 'magnetism', 'skyward'],
    ['mending_heart', 'Mending Heart', 'restoration', 'vitality'],
    ['delving_guillotine', 'Delving Guillotine', 'excavation', 'executioner'],
    ['keen_bunker', 'Keen Bunker', 'sharpness', 'blast_ward'],
    ['crawling_updraft', 'Crawling Updraft', 'bane', 'skyward'],
    ['titanslaying_heart', 'Titanslaying Heart', 'giant_slayer', 'vitality'],
    ['siegebreaking_harvest', 'Siegebreaking Harvest', 'raidbreaker', 'soul_eater'],
    ['tidal_lodestar', 'Tidal Lodestar', 'impaling', 'magnetism'],
    ['swift_flamewall', 'Swift Flamewall', 'first_strike', 'fire_ward'],
    ['raging_blight', 'Raging Blight', 'berserk', 'withering'],
    ['piercing_remedy', 'Piercing Remedy', 'power', 'wither_ward'],
    ['grim_phylactery', 'Grim Phylactery', 'reaper', 'undying'],
    ['vampiric_gem', 'Vampiric Gem', 'lifesteal', 'gem_hunter'],
    ['frozen_edge', 'Frozen Edge', 'frostbite', 'sharpness'],
    ['blighted_shield', 'Blighted Shield', 'withering', 'arrow_ward'],
    ['venomous_warbreaker', 'Venomous Warbreaker', 'venom', 'raidbreaker'],
    ['burning_vengeance', 'Burning Vengeance', 'fire_aspect', 'retribution'],
    ['rallying_fury', 'Rallying Fury', 'second_wind', 'berserk'],
    ['frenzied_insight', 'Frenzied Insight', 'adrenaline', 'wisdom'],
    ['greedy_anvil', 'Greedy Anvil', 'plunder', 'restoration'],
    ['soulbound_cleaver', 'Soulbound Cleaver', 'soul_eater', 'cleave'],
    ['sage_feather', 'Sage Feather', 'wisdom', 'featherweight'],
    ['gilded_slayer', 'Gilded Slayer', 'midas', 'giant_slayer'],
    ['gleaming_venom', 'Gleaming Venom', 'gem_hunter', 'venom'],
    ['weightless_hoard', 'Weightless Hoard', 'featherweight', 'plunder'],
    ['blastproof_bloom', 'Blastproof Bloom', 'blast_ward', 'renewal'],
    ['deflecting_precision', 'Deflecting Precision', 'arrow_ward', 'critical'],
    ['stalwart_malaise', 'Stalwart Malaise', 'bulwark', 'enfeeble'],
    ['vengeful_trident', 'Vengeful Trident', 'retribution', 'impaling'],
    ['elusive_rally', 'Elusive Rally', 'evasion', 'second_wind'],
    ['verdant_volley', 'Verdant Volley', 'renewal', 'power'],
    ['magnetic_fortune', 'Magnetic Fortune', 'magnetism', 'midas'],
    ['delving_thirst', 'Delving Thirst', 'excavation', 'lifesteal'],
    ['keen_shadowstep', 'Keen Shadowstep', 'sharpness', 'evasion'],
    ['crawling_fortune', 'Crawling Fortune', 'bane', 'midas'],
    ['titanslaying_drill', 'Titanslaying Drill', 'giant_slayer', 'excavation'],
    ['infernal_thirst', 'Infernal Thirst', 'hellbane', 'lifesteal'],
    ['siegebreaking_bunker', 'Siegebreaking Bunker', 'raidbreaker', 'blast_ward'],
    ['skyborne_updraft', 'Skyborne Updraft', 'skyhunter', 'skyward'],
    ['swift_heart', 'Swift Heart', 'first_strike', 'vitality'],
    ['raging_harvest', 'Raging Harvest', 'berserk', 'soul_eater'],
    ['piercing_lodestar', 'Piercing Lodestar', 'power', 'magnetism'],
    ['precise_flamewall', 'Precise Flamewall', 'critical', 'fire_ward'],
    ['sweeping_blight', 'Sweeping Blight', 'cleave', 'withering'],
    ['vampiric_remedy', 'Vampiric Remedy', 'lifesteal', 'wither_ward'],
    ['frozen_falcon', 'Frozen Falcon', 'frostbite', 'skyhunter'],
    ['venomous_dagger', 'Venomous Dagger', 'venom', 'assassin'],
    ['soaring_gem', 'Soaring Gem', 'skyward', 'gem_hunter'],
    ['burning_edge', 'Burning Edge', 'fire_aspect', 'sharpness'],
    ['rallying_frost', 'Rallying Frost', 'second_wind', 'frostbite'],
    ['frenzied_shield', 'Frenzied Shield', 'adrenaline', 'arrow_ward'],
    ['greedy_warbreaker', 'Greedy Warbreaker', 'plunder', 'raidbreaker'],
    ['soulbound_pyre', 'Soulbound Pyre', 'soul_eater', 'fire_aspect'],
    ['sage_vengeance', 'Sage Vengeance', 'wisdom', 'retribution'],
    ['gilded_fury', 'Gilded Fury', 'midas', 'berserk'],
    ['gleaming_insight', 'Gleaming Insight', 'gem_hunter', 'wisdom'],
    ['warded_anvil', 'Warded Anvil', 'protection', 'restoration'],
    ['fireproof_cleaver', 'Fireproof Cleaver', 'fire_ward', 'cleave'],
    ['blastproof_slayer', 'Blastproof Slayer', 'blast_ward', 'giant_slayer'],
    ['deflecting_venom', 'Deflecting Venom', 'arrow_ward', 'venom'],
    ['spellproof_rampart', 'Spellproof Rampart', 'spellward', 'bulwark'],
    ['unwithering_opening', 'Unwithering Opening', 'wither_ward', 'first_strike'],
    ['hearty_bloom', 'Hearty Bloom', 'vitality', 'renewal'],
    ['vengeful_precision', 'Vengeful Precision', 'retribution', 'critical'],
    ['elusive_aegis', 'Elusive Aegis', 'evasion', 'protection'],
    ['deathless_purge', 'Deathless Purge', 'undying', 'smite'],
    ['magnetic_ward', 'Magnetic Ward', 'magnetism', 'spellward'],
    ['mending_trident', 'Mending Trident', 'restoration', 'impaling'],
    ['delving_rally', 'Delving Rally', 'excavation', 'second_wind']
  ],
  // ---- Triad enchants: three base enchants fused into one tome ----
  // Each does all three parents' effects at triadStrength of their power. Tome: the three parent tomes + 2 Ascension
  // Cores in a heated mixer. Costs triadCostMultiplier x the Essence per level. [id, name, parent A, parent B, parent C]
  // (No two parents of a Triad are also a Fusion pair, so the basin never mixes the recipes up.)
  triadStrength: 0.45,
  triadCostMultiplier: 3,
  triads: [
    ['triad_stormforged_requiem', 'Stormforged Requiem', 'berserk', 'executioner', 'sharpness'],
    ['triad_spectral_bloodrite', 'Spectral Bloodrite', 'assassin', 'bane', 'berserk'],
    ['triad_venomous_rend', 'Venomous Rend', 'cleave', 'power', 'venom'],
    ['triad_cursed_bloodrite', 'Cursed Bloodrite', 'midas', 'raidbreaker', 'smite'],
    ['triad_frenzied_tempest', 'Frenzied Tempest', 'impaling', 'lifesteal', 'power'],
    ['triad_gilded_carnage', 'Gilded Carnage', 'cleave', 'impaling', 'plunder'],
    ['triad_frenzied_rampage_song', 'Frenzied Rampage Song', 'lifesteal', 'midas', 'withering'],
    ['triad_ashen_carnage', 'Ashen Carnage', 'enfeeble', 'gem_hunter', 'plunder'],
    ['triad_searing_tempest', 'Searing Tempest', 'hellbane', 'second_wind', 'sharpness'],
    ['triad_ironclad_ruin', 'Ironclad Ruin', 'cleave', 'giant_slayer', 'skyhunter'],
    ['triad_bloodbound_rend', 'Bloodbound Rend', 'critical', 'giant_slayer', 'power'],
    ['triad_feral_butchery', 'Feral Butchery', 'executioner', 'reaper', 'skyward'],
    ['triad_wicked_fury', 'Wicked Fury', 'hellbane', 'impaling', 'second_wind'],
    ['triad_howling_fury', 'Howling Fury', 'critical', 'lifesteal', 'skyhunter'],
    ['triad_ashen_bloodrite', 'Ashen Bloodrite', 'hellbane', 'impaling', 'power'],
    ['triad_venomous_execution', 'Venomous Execution', 'executioner', 'fire_aspect', 'withering'],
    ['triad_nightfall_vendetta', 'Nightfall Vendetta', 'enfeeble', 'reaper', 'skyhunter'],
    ['triad_crimson_ravage', 'Crimson Ravage', 'bane', 'enfeeble', 'gem_hunter'],
    ['triad_shattered_butchery', 'Shattered Butchery', 'fire_aspect', 'skyhunter', 'venom'],
    ['triad_nightfall_reckoning', 'Nightfall Reckoning', 'bane', 'enfeeble', 'fire_aspect'],
    ['triad_grim_maelstrom', 'Grim Maelstrom', 'executioner', 'power', 'second_wind'],
    ['triad_grim_tempest', 'Grim Tempest', 'frostbite', 'lifesteal', 'skyward'],
    ['triad_ironclad_bloodrite', 'Ironclad Bloodrite', 'adrenaline', 'bane', 'executioner'],
    ['triad_stormforged_calamity', 'Stormforged Calamity', 'berserk', 'sharpness', 'smite'],
    ['triad_bloodbound_bloodrite', 'Bloodbound Bloodrite', 'cleave', 'smite', 'venom'],
    ['triad_frenzied_requiem', 'Frenzied Requiem', 'assassin', 'fire_aspect', 'withering'],
    ['triad_hollow_execution', 'Hollow Execution', 'lifesteal', 'second_wind', 'soul_eater'],
    ['triad_searing_calamity', 'Searing Calamity', 'adrenaline', 'hellbane', 'plunder'],
    ['triad_cursed_maelstrom', 'Cursed Maelstrom', 'enfeeble', 'frostbite', 'soul_eater'],
    ['triad_moonlit_execution', 'Moonlit Execution', 'plunder', 'smite', 'withering'],
    ['triad_ironclad_rampage_song', 'Ironclad Rampage Song', 'enfeeble', 'impaling', 'soul_eater'],
    ['triad_spectral_warpath', 'Spectral Warpath', 'adrenaline', 'hellbane', 'withering'],
    ['triad_savage_butchery', 'Savage Butchery', 'first_strike', 'giant_slayer', 'raidbreaker'],
    ['triad_radiant_reckoning', 'Radiant Reckoning', 'berserk', 'fire_aspect', 'first_strike'],
    ['triad_howling_deathknell', 'Howling Deathknell', 'gem_hunter', 'hellbane', 'skyhunter'],
    ['triad_ashen_onslaught', 'Ashen Onslaught', 'cleave', 'second_wind', 'sharpness'],
    ['triad_spectral_butchery', 'Spectral Butchery', 'adrenaline', 'assassin', 'raidbreaker'],
    ['triad_thunderous_ravage', 'Thunderous Ravage', 'giant_slayer', 'power', 'venom'],
    ['triad_nightfall_butchery', 'Nightfall Butchery', 'assassin', 'critical', 'skyhunter'],
    ['triad_gilded_ruin', 'Gilded Ruin', 'assassin', 'giant_slayer', 'soul_eater'],
    ['triad_howling_ruin', 'Howling Ruin', 'cleave', 'first_strike', 'reaper'],
    ['triad_venomous_harrow', 'Venomous Harrow', 'enfeeble', 'impaling', 'power'],
    ['triad_stormforged_rampage_song', 'Stormforged Rampage Song', 'critical', 'power', 'skyward'],
    ['triad_shattered_bloodrite', 'Shattered Bloodrite', 'adrenaline', 'power', 'skyward'],
    ['triad_wrathful_butchery', 'Wrathful Butchery', 'midas', 'skyhunter', 'smite'],
    ['triad_wicked_verdict', 'Wicked Verdict', 'critical', 'power', 'raidbreaker'],
    ['triad_crimson_slaughter', 'Crimson Slaughter', 'bane', 'venom', 'wisdom'],
    ['triad_moonlit_verdict', 'Moonlit Verdict', 'adrenaline', 'skyhunter', 'withering'],
    ['triad_shattered_calamity', 'Shattered Calamity', 'giant_slayer', 'power', 'reaper'],
    ['triad_ashen_ravage', 'Ashen Ravage', 'bane', 'frostbite', 'wisdom'],
    ['triad_nightfall_fury', 'Nightfall Fury', 'adrenaline', 'enfeeble', 'sharpness'],
    ['triad_ironclad_vendetta', 'Ironclad Vendetta', 'first_strike', 'power', 'second_wind'],
    ['triad_ashen_slaughter', 'Ashen Slaughter', 'adrenaline', 'hellbane', 'venom'],
    ['triad_feral_harrow', 'Feral Harrow', 'enfeeble', 'midas', 'plunder'],
    ['triad_stormforged_maelstrom', 'Stormforged Maelstrom', 'critical', 'sharpness', 'venom'],
    ['triad_grim_harrow', 'Grim Harrow', 'adrenaline', 'assassin', 'enfeeble'],
    ['triad_gilded_maelstrom', 'Gilded Maelstrom', 'impaling', 'reaper', 'soul_eater'],
    ['triad_radiant_havoc', 'Radiant Havoc', 'executioner', 'frostbite', 'impaling'],
    ['triad_howling_tempest', 'Howling Tempest', 'fire_aspect', 'skyhunter', 'wisdom'],
    ['triad_feral_execution', 'Feral Execution', 'midas', 'reaper', 'sharpness'],
    ['triad_frenzied_vendetta', 'Frenzied Vendetta', 'bane', 'critical', 'executioner'],
    ['triad_ashen_calamity', 'Ashen Calamity', 'berserk', 'cleave', 'plunder'],
    ['triad_howling_verdict', 'Howling Verdict', 'first_strike', 'second_wind', 'wisdom'],
    ['triad_feral_warpath', 'Feral Warpath', 'adrenaline', 'lifesteal', 'sharpness'],
    ['triad_savage_warpath', 'Savage Warpath', 'lifesteal', 'midas', 'power'],
    ['triad_feral_deathknell', 'Feral Deathknell', 'bane', 'enfeeble', 'soul_eater'],
    ['triad_cursed_vendetta', 'Cursed Vendetta', 'executioner', 'impaling', 'lifesteal'],
    ['triad_venomous_slaughter', 'Venomous Slaughter', 'critical', 'second_wind', 'smite'],
    ['triad_thunderous_deathknell', 'Thunderous Deathknell', 'first_strike', 'hellbane', 'raidbreaker'],
    ['triad_searing_onslaught', 'Searing Onslaught', 'adrenaline', 'soul_eater', 'venom'],
    ['triad_ironclad_warpath', 'Ironclad Warpath', 'enfeeble', 'lifesteal', 'raidbreaker'],
    ['triad_searing_fury', 'Searing Fury', 'lifesteal', 'midas', 'reaper'],
    ['triad_sundered_warpath', 'Sundered Warpath', 'cleave', 'raidbreaker', 'smite'],
    ['triad_ironclad_slaughter', 'Ironclad Slaughter', 'giant_slayer', 'hellbane', 'soul_eater'],
    ['triad_spectral_onslaught', 'Spectral Onslaught', 'berserk', 'power', 'skyhunter'],
    ['triad_hollow_tempest', 'Hollow Tempest', 'berserk', 'skyhunter', 'venom'],
    ['triad_radiant_rampage_song', 'Radiant Rampage Song', 'bane', 'first_strike', 'withering'],
    ['triad_feral_rend', 'Feral Rend', 'adrenaline', 'fire_aspect', 'raidbreaker'],
    ['triad_searing_eviscerator', 'Searing Eviscerator', 'executioner', 'first_strike', 'smite'],
    ['triad_venomous_deathknell', 'Venomous Deathknell', 'bane', 'executioner', 'hellbane'],
    ['triad_grim_bloodrite', 'Grim Bloodrite', 'enfeeble', 'plunder', 'reaper'],
    ['triad_savage_carnage', 'Savage Carnage', 'adrenaline', 'assassin', 'skyward'],
    ['triad_thunderous_fury', 'Thunderous Fury', 'assassin', 'cleave', 'hellbane'],
    ['triad_ironclad_butchery', 'Ironclad Butchery', 'impaling', 'reaper', 'smite'],
    ['triad_feral_massacre', 'Feral Massacre', 'frostbite', 'power', 'withering'],
    ['triad_radiant_vendetta', 'Radiant Vendetta', 'power', 'reaper', 'skyward'],
    ['triad_hollow_fury', 'Hollow Fury', 'frostbite', 'gem_hunter', 'raidbreaker'],
    ['triad_wicked_massacre', 'Wicked Massacre', 'executioner', 'hellbane', 'skyward'],
    ['triad_shattered_slaughter', 'Shattered Slaughter', 'cleave', 'raidbreaker', 'wisdom'],
    ['triad_thunderous_massacre', 'Thunderous Massacre', 'power', 'reaper', 'soul_eater'],
    ['triad_nightfall_deathknell', 'Nightfall Deathknell', 'power', 'skyhunter', 'venom'],
    ['triad_howling_vendetta', 'Howling Vendetta', 'enfeeble', 'fire_aspect', 'skyward'],
    ['triad_ironclad_rend', 'Ironclad Rend', 'impaling', 'lifesteal', 'raidbreaker'],
    ['triad_stormforged_ravage', 'Stormforged Ravage', 'fire_aspect', 'frostbite', 'midas'],
    ['triad_shattered_verdict', 'Shattered Verdict', 'fire_aspect', 'first_strike', 'giant_slayer'],
    ['triad_cursed_deathknell', 'Cursed Deathknell', 'hellbane', 'reaper', 'sharpness'],
    ['triad_radiant_execution', 'Radiant Execution', 'adrenaline', 'reaper', 'smite'],
    ['triad_venomous_eviscerator', 'Venomous Eviscerator', 'cleave', 'critical', 'reaper'],
    ['triad_nightfall_havoc', 'Nightfall Havoc', 'bane', 'impaling', 'plunder'],
    ['triad_hollow_eviscerator', 'Hollow Eviscerator', 'bane', 'executioner', 'raidbreaker'],
    ['triad_grim_massacre', 'Grim Massacre', 'assassin', 'critical', 'sharpness'],
    ['triad_moonlit_reckoning', 'Moonlit Reckoning', 'critical', 'hellbane', 'skyward'],
    ['triad_moonlit_vendetta', 'Moonlit Vendetta', 'giant_slayer', 'lifesteal', 'power'],
    ['triad_crimson_carnage', 'Crimson Carnage', 'bane', 'lifesteal', 'withering'],
    ['triad_ashen_verdict', 'Ashen Verdict', 'enfeeble', 'reaper', 'soul_eater'],
    ['triad_radiant_slaughter', 'Radiant Slaughter', 'first_strike', 'power', 'sharpness'],
    ['triad_ashen_havoc', 'Ashen Havoc', 'assassin', 'berserk', 'gem_hunter'],
    ['triad_ashen_rampage_song', 'Ashen Rampage Song', 'enfeeble', 'lifesteal', 'second_wind'],
    ['triad_savage_verdict', 'Savage Verdict', 'assassin', 'berserk', 'hellbane'],
    ['triad_savage_onslaught', 'Savage Onslaught', 'bane', 'hellbane', 'smite'],
    ['triad_thunderous_execution', 'Thunderous Execution', 'giant_slayer', 'power', 'withering'],
    ['triad_hollow_calamity', 'Hollow Calamity', 'enfeeble', 'executioner', 'fire_aspect'],
    ['triad_moonlit_fury', 'Moonlit Fury', 'bane', 'gem_hunter', 'skyhunter'],
    ['triad_frenzied_massacre', 'Frenzied Massacre', 'fire_aspect', 'midas', 'reaper'],
    ['triad_ironclad_calamity', 'Ironclad Calamity', 'critical', 'enfeeble', 'executioner'],
    ['triad_wicked_butchery', 'Wicked Butchery', 'bane', 'lifesteal', 'power'],
    ['triad_bloodbound_deathknell', 'Bloodbound Deathknell', 'first_strike', 'midas', 'reaper'],
    ['triad_thunderous_verdict', 'Thunderous Verdict', 'cleave', 'enfeeble', 'smite'],
    ['triad_crimson_deathknell', 'Crimson Deathknell', 'smite', 'venom', 'wisdom'],
    ['triad_shattered_ravage', 'Shattered Ravage', 'adrenaline', 'berserk', 'plunder'],
    ['triad_shattered_carnage', 'Shattered Carnage', 'bane', 'plunder', 'wisdom'],
    ['triad_gilded_rend', 'Gilded Rend', 'adrenaline', 'fire_aspect', 'second_wind'],
    ['triad_searing_deathknell', 'Searing Deathknell', 'frostbite', 'impaling', 'lifesteal'],
    ['triad_grim_verdict', 'Grim Verdict', 'adrenaline', 'frostbite', 'reaper'],
    ['triad_spectral_ruin', 'Spectral Ruin', 'cleave', 'frostbite', 'wisdom'],
    ['triad_searing_requiem', 'Searing Requiem', 'critical', 'second_wind', 'skyhunter'],
    ['triad_moonlit_tempest', 'Moonlit Tempest', 'berserk', 'gem_hunter', 'sharpness'],
    ['triad_ironclad_eviscerator', 'Ironclad Eviscerator', 'berserk', 'power', 'venom'],
    ['triad_radiant_carnage', 'Radiant Carnage', 'executioner', 'impaling', 'plunder'],
    ['triad_bloodbound_calamity', 'Bloodbound Calamity', 'executioner', 'power', 'skyhunter'],
    ['triad_unyielding_rampart', 'Unyielding Rampart', 'blast_ward', 'evasion', 'undying'],
    ['triad_eternal_oath', 'Eternal Oath', 'blast_ward', 'fire_ward', 'wither_ward'],
    ['triad_sworn_rampart', 'Sworn Rampart', 'featherweight', 'spellward', 'wither_ward'],
    ['triad_granite_fortress', 'Granite Fortress', 'featherweight', 'renewal', 'retribution'],
    ['triad_sworn_oath', 'Sworn Oath', 'arrow_ward', 'undying', 'vitality'],
    ['triad_warded_oath', 'Warded Oath', 'blast_ward', 'retribution', 'spellward'],
    ['triad_verdant_shroud', 'Verdant Shroud', 'blast_ward', 'featherweight', 'fire_ward'],
    ['triad_silent_covenant', 'Silent Covenant', 'bulwark', 'protection', 'retribution'],
    ['triad_sworn_mantle', 'Sworn Mantle', 'arrow_ward', 'fire_ward', 'renewal'],
    ['triad_granite_citadel', 'Granite Citadel', 'blast_ward', 'featherweight', 'vitality'],
    ['triad_ancient_aegis', 'Ancient Aegis', 'blast_ward', 'retribution', 'undying'],
    ['triad_eternal_aegis', 'Eternal Aegis', 'featherweight', 'fire_ward', 'undying'],
    ['triad_verdant_ward_song', 'Verdant Ward-Song', 'spellward', 'undying', 'vitality'],
    ['triad_iron_shroud', 'Iron Shroud', 'evasion', 'spellward', 'wither_ward'],
    ['triad_eternal_bastion', 'Eternal Bastion', 'blast_ward', 'evasion', 'wither_ward'],
    ['triad_silent_bulwark_rite', 'Silent Bulwark Rite', 'retribution', 'spellward', 'vitality'],
    ['triad_living_fortress', 'Living Fortress', 'arrow_ward', 'bulwark', 'vitality'],
    ['triad_iron_fortress', 'Iron Fortress', 'fire_ward', 'undying', 'wither_ward'],
    ['triad_living_citadel', 'Living Citadel', 'fire_ward', 'spellward', 'vitality'],
    ['triad_verdant_rampart', 'Verdant Rampart', 'blast_ward', 'fire_ward', 'retribution'],
    ['triad_silent_carapace', 'Silent Carapace', 'arrow_ward', 'blast_ward', 'bulwark'],
    ['triad_warded_mantle', 'Warded Mantle', 'arrow_ward', 'blast_ward', 'retribution'],
    ['triad_silent_oath', 'Silent Oath', 'blast_ward', 'bulwark', 'fire_ward'],
    ['triad_silent_shroud', 'Silent Shroud', 'blast_ward', 'fire_ward', 'undying'],
    ['triad_sentinel_vigil', 'Sentinel Vigil', 'bulwark', 'featherweight', 'wither_ward'],
    ['triad_eternal_mantle', 'Eternal Mantle', 'blast_ward', 'protection', 'spellward'],
    ['triad_silent_citadel', 'Silent Citadel', 'retribution', 'spellward', 'undying'],
    ['triad_verdant_mantle', 'Verdant Mantle', 'evasion', 'retribution', 'undying'],
    ['triad_hallowed_oath', 'Hallowed Oath', 'evasion', 'fire_ward', 'retribution'],
    ['triad_sentinel_rampart', 'Sentinel Rampart', 'evasion', 'renewal', 'undying'],
    ['triad_celestial_rampart', 'Celestial Rampart', 'arrow_ward', 'renewal', 'retribution'],
    ['triad_unyielding_aegis', 'Unyielding Aegis', 'featherweight', 'fire_ward', 'protection'],
    ['triad_iron_bulwark_rite', 'Iron Bulwark Rite', 'evasion', 'spellward', 'undying'],
    ['triad_living_bastion', 'Living Bastion', 'renewal', 'retribution', 'undying'],
    ['triad_eternal_carapace', 'Eternal Carapace', 'arrow_ward', 'retribution', 'spellward'],
    ['triad_stalwart_bulwark_rite', 'Stalwart Bulwark Rite', 'blast_ward', 'featherweight', 'protection'],
    ['triad_living_oath', 'Living Oath', 'featherweight', 'fire_ward', 'retribution'],
    ['triad_unyielding_shroud', 'Unyielding Shroud', 'protection', 'retribution', 'spellward'],
    ['triad_eternal_vigil', 'Eternal Vigil', 'arrow_ward', 'evasion', 'vitality'],
    ['triad_unyielding_bastion', 'Unyielding Bastion', 'evasion', 'featherweight', 'wither_ward'],
    ['triad_sworn_vigil', 'Sworn Vigil', 'arrow_ward', 'evasion', 'spellward'],
    ['triad_celestial_mantle', 'Celestial Mantle', 'protection', 'undying', 'vitality'],
    ['triad_silent_bastion', 'Silent Bastion', 'arrow_ward', 'bulwark', 'protection'],
    ['triad_iron_sanctum', 'Iron Sanctum', 'protection', 'spellward', 'vitality'],
    ['triad_celestial_vigil', 'Celestial Vigil', 'protection', 'spellward', 'undying'],
    ['triad_sworn_citadel', 'Sworn Citadel', 'arrow_ward', 'retribution', 'vitality'],
    ['triad_granite_bulwark_rite', 'Granite Bulwark Rite', 'arrow_ward', 'blast_ward', 'protection'],
    ['triad_stalwart_vigil', 'Stalwart Vigil', 'renewal', 'retribution', 'spellward'],
    ['triad_sworn_sanctum', 'Sworn Sanctum', 'arrow_ward', 'featherweight', 'protection'],
    ['triad_granite_mantle', 'Granite Mantle', 'arrow_ward', 'protection', 'wither_ward'],
    ['triad_granite_rampart', 'Granite Rampart', 'arrow_ward', 'renewal', 'spellward'],
    ['triad_iron_bastion', 'Iron Bastion', 'featherweight', 'vitality', 'wither_ward'],
    ['triad_iron_carapace', 'Iron Carapace', 'blast_ward', 'evasion', 'retribution'],
    ['triad_celestial_carapace', 'Celestial Carapace', 'featherweight', 'spellward', 'undying'],
    ['triad_stalwart_citadel', 'Stalwart Citadel', 'bulwark', 'featherweight', 'retribution'],
    ['triad_unyielding_mantle', 'Unyielding Mantle', 'arrow_ward', 'protection', 'retribution'],
    ['triad_silent_sanctum', 'Silent Sanctum', 'arrow_ward', 'featherweight', 'undying'],
    ['triad_verdant_aegis', 'Verdant Aegis', 'fire_ward', 'retribution', 'vitality'],
    ['triad_warded_fortress', 'Warded Fortress', 'renewal', 'spellward', 'undying'],
    ['triad_verdant_oath', 'Verdant Oath', 'bulwark', 'evasion', 'renewal'],
    ['triad_celestial_shroud', 'Celestial Shroud', 'arrow_ward', 'evasion', 'retribution'],
    ['triad_lucky_windfall', 'Lucky Windfall', 'gem_hunter', 'midas', 'restoration'],
    ['triad_deepdelvers_bounty', "Deepdelver's Bounty", 'midas', 'restoration', 'wisdom'],
    ['triad_deepdelvers_toil', "Deepdelver's Toil", 'excavation', 'restoration', 'wisdom'],
    ['triad_delvers_toil', "Delver's Toil", 'excavation', 'midas', 'restoration'],
    ['triad_prospectors_windfall', "Prospector's Windfall", 'excavation', 'midas', 'wisdom'],
    ['triad_deepdelvers_haul', "Deepdelver's Haul", 'excavation', 'magnetism', 'wisdom'],
    ['triad_deepdelvers_windfall', "Deepdelver's Windfall", 'gem_hunter', 'magnetism', 'restoration'],
    ['triad_gilded_bounty', 'Gilded Bounty', 'excavation', 'magnetism', 'restoration'],
    ['triad_patient_vein', 'Patient Vein', 'magnetism', 'restoration', 'wisdom']
  ],

  // ---- Danger: the farther from home, the nastier the mobs (and the better the loot) ----
  // Danger level = (distance from the center - safeRadius) / blocksPerLevel + 1, no cap.
  // Only hostile, non-boss mobs scale. Being AFK changes nothing: it's all about where you are.
  danger: {
    enabled: true,
    // 'home': danger is measured from each player's home (/home set), and nothing scales until they set one, so you
    //         can explore for a base first. Mobs use the home of the player they spawn nearest to.
    // 'spawn': from world spawn, or wherever an op moved it (/ascension center set).
    centerOn: 'home',
    safeRadius: 1000,          // normal mobs within this many blocks of the center
    blocksPerLevel: 250,       // +1 danger level every 250 blocks past that: 1,000 / 1,250 / 1,500 / 1,750 / 2,000 ...
    netherScale: 8,            // Nether distance counts x8, like Nether travel
    netherBonus: 4,            // the Nether is always at least this dangerous
    endBonus: 7,               // the End too (the main island is inside safeRadius of 0,0)
    realmBonus: 12,            // the Ascended Realm (Full download) starts this dangerous, and climbs from its 0,0 like the End
    // Mobs get tougher on a curve by how far out their zone starts: x(1 + scale * (blocks / 1000) ^ exponent).
    // 0.5 and 2.807 give x1.5 at 1,000 blocks and x4.5 at 2,000, then x11.9 at 3,000, x26 at 4,000, x47 at 5,000.
    exponent: 2.807,
    healthScale: 0.5,          // mobs take damage / that, like having more health
    damageScale: 0.5,          // mobs deal x that damage
    // Mob drops and XP grow on the same curve as their health: x(1 + scale * (blocks / 1000) ^ exponent)
    lootScale: 0.5,            // mob drops x1.5 at 1,000 blocks, x4.5 at 2,000, x47 at 5,000
    xpScale: 0.5,              // mob XP the same
    // Elites: rare, named, much tougher, much better loot
    eliteChancePerLevel: 0.01, // 1% per level...
    eliteChanceCap: 0.25,      // ...up to 25% of mobs
    eliteHealth: 3,
    eliteDamage: 1.5,
    eliteLoot: 3,
    maxLevel: 0,               // 0 = no cap
    // Zone names shown when you cross into them, by minimum danger level (title colors are Minecraft color names)
    zones: [
      { from: 0, name: 'Safe Lands', color: 'green' },
      { from: 1, name: 'The Frontier', color: 'yellow' },       // 1,000 blocks out
      { from: 5, name: 'The Wilds', color: 'gold' },            // 2,000
      { from: 9, name: 'Dread Reaches', color: 'red' },         // 3,000
      { from: 17, name: 'The Abyss', color: 'dark_red' },       // 5,000
      { from: 37, name: 'Nightmare', color: 'dark_purple' },    // 10,000
      { from: 77, name: 'The Void Beyond', color: 'dark_gray' } // 20,000
    ]
  },

  // ---- Chaos Tomes: randomly generated enchants ----
  // A Chaos Tome rolls a brand-new enchant onto gear: a rarity, a generated name and 1-3 random effects.
  // Rolls are made from a seed, so the same seed always gives the same enchant (in game and on the website).
  chaos: {
    matchGear: true,           // a roll only gets effects that suit the item it's on (see gearMatch)
    maxPerItem: 3,             // rolled enchants one item can hold (sneak-use a Chaos Tome on a full item to reroll its newest)
    rarities: [
      { name: 'Common', weight: 50, power: 1, affixes: 1, costMult: 1, color: 'gray' },
      { name: 'Rare', weight: 28, power: 1.5, affixes: 2, costMult: 1.5, color: 'aqua' },
      { name: 'Epic', weight: 15, power: 2.2, affixes: 2, costMult: 2.5, color: 'light_purple' },
      { name: 'Legendary', weight: 6, power: 3.2, affixes: 3, costMult: 4, color: 'gold' },
      { name: 'Mythic', weight: 1, power: 5, affixes: 3, costMult: 8, color: 'red' }
    ]
  },

  // ---- Liquid Essence + Ascension Altar ----
  // Cores melt into Liquid Essence in a heated mixer (1 core = 100 mB = 1 Essence, so nothing is gained or lost).
  // An Ascension Altar with a Create Depot on top and a tank or basin of Liquid Essence below
  // levels up the gear on the depot by itself: one level at a time, always the lowest enchant first.
  altar: {
    color: 0xB05CFF,           // Liquid Essence color (fluid, bucket, website)
    mbPerEssence: 100,
    maxLevelsPerSecond: 20,    // per altar
    maxAltars: 256             // most altars the server tracks
  },

  // ---- Condensed Essence: fluid packed into blocks, for storing truly absurd amounts ----
  // An Essence Condenser takes Liquid Essence from a tank below it and puts Condensed Essence Blocks into
  // the chest or vault on top: 1 block = mbPerBlock mB. Each denser tier is `ratio` of the one below
  // (compact them with a Mechanical Press + Basin, or craft 3x3), up to (x50). Right-click one in the air to
  // absorb it; sneak to absorb the whole stack. Every tier stacks to 64.
  condensed: {
    mbPerBlock: 1000,          // 1 bucket per block = 10 Essence
    ratio: 9,
    blocksPerSecond: 64,       // per condenser
    maxCondensers: 256,
    // One name per dense tier, from (x1) to (x50): each one denser than the last
    names: ['Dense', 'Compact', 'Packed', 'Heavy', 'Thickened', 'Solidified', 'Compressed', 'Pressurized', 'Crushed', 'Leaden',
      'Ironclad', 'Weighted', 'Ponderous', 'Massive', 'Monolithic', 'Tectonic', 'Bedrock', 'Mantle', 'Core-Forged', 'Magmatic',
      'Crystalline', 'Diamond-Pressed', 'Hyperdense', 'Ultradense', 'Degenerate', 'Collapsed', 'Plasma-Bound', 'Fused', 'Fusion', 'Stellar',
      'Supergiant', 'Nova', 'White Dwarf', 'Neutronium', 'Pulsar', 'Magnetar', 'Quark', 'Strange Matter', 'Event Horizon', 'Singularity',
      'Black Hole', 'Quasar', 'Galactic', 'Cluster', 'Supercluster', 'Dark Matter', 'Big Crunch', 'Primordial', 'Cosmic', 'Absolute']
  },

  // ---- Prestige (Rebirth) ----
  // Reset an item's enchant levels back to 1 for a permanent star: every level on it then counts
  // x(1 + bonusPerStar * stars). Needs needLevels * (stars + 1) total levels on the item.
  prestige: {
    needLevels: 1000,
    bonusPerStar: 0.25,
    maxStars: 0                // 0 = no cap
  },

  // ---- Ascension backpacks (Full download + Sophisticated Backpacks): tiers past Netherite, as real Sophisticated ----
  // backpacks, so every Sophisticated upgrade works in them. Each tier is crafted around the one before it (the first
  // around a Netherite Backpack) and keeps everything inside. slots: storage slots, upgrades: upgrade slots.
  // (Sophisticated's own Netherite Backpack has 120 and 7.)
  backpacks: {
    tiers: [
      { id: 'ascendite', slots: 162, upgrades: 7, recipe: [['AAA', 'ABA', 'AAA'], { A: 'kubejs:ascendite_block' }] },
      { id: 'voidsteel', slots: 270, upgrades: 8, recipe: [['VVV', 'VBV', 'VVV'], { V: 'kubejs:voidsteel_ingot' }] },
      { id: 'celestium', slots: 432, upgrades: 9, recipe: [['CCC', 'CBC', 'CCC'], { C: 'kubejs:celestium_ingot' }] },
      { id: 'ascendium', slots: 648, upgrades: 10, recipe: [['ADA', 'DBD', 'ADA'], { A: 'kubejs:ascendium_ingot', D: 'kubejs:dense_ascension_core' }] },
      { id: 'singularity', slots: 1080, upgrades: 12, recipe: [['SSS', 'SBS', 'SSS'], { S: 'kubejs:ascension_singularity' }] },
      { id: 'ascended', slots: 1728, upgrades: 14, recipe: [['SDS', 'DBD', 'SNS'], { S: 'kubejs:ascended_soul', D: 'kubejs:ascension_singularity', N: 'minecraft:nether_star' }] }
    ]
  },

  // ---- Legendary stars: Fallout 76-style grades for gear, from no stars to 10, made at the Ascension Workbench ----
  // Each star adds one legendary effect (a random enchant that suits the gear, at effectLevels[star - 1]) and makes
  // every level on the item count x(1 + bonusPerStar x stars), on top of Rebirth stars. costs[star - 1]: what that star
  // takes from your inventory ([item, count]). The workbench is also where tomes, gems, attachments, Chaos Tomes and
  // Polishing Cloths go on gear.
  legendary: {
    enabled: true,
    maxStars: 10,
    bonusPerStar: 0.1,
    effectLevels: [10, 25, 50, 100, 200, 400, 800, 1500, 3000, 6000],
    // Grade names and colors by star count (index = stars)
    grades: [['Plain', 'gray'], ['Legendary', 'gold'], ['Legendary', 'gold'], ['Legendary', 'gold'], ['Epic Legendary', 'light_purple'],
      ['Epic Legendary', 'light_purple'], ['Epic Legendary', 'light_purple'], ['Mythic Legendary', 'red'], ['Mythic Legendary', 'red'],
      ['Mythic Legendary', 'red'], ['Ascended Legendary', 'aqua']],
    costs: [
      [['kubejs:ascension_core', 16], ['kubejs:ascension_shard', 8]],
      [['kubejs:dense_ascension_core', 4], ['kubejs:mythic_fragment', 1]],
      [['kubejs:dense_ascension_core', 8], ['kubejs:mythic_fragment', 2]],
      [['kubejs:dense_ascension_core', 16], ['kubejs:mythic_fragment', 4], ['kubejs:voidsteel_ingot', 4]],
      [['kubejs:ascension_singularity', 1], ['kubejs:mythic_fragment', 6], ['kubejs:voidsteel_ingot', 8]],
      [['kubejs:ascension_singularity', 2], ['kubejs:mythic_fragment', 8], ['kubejs:celestium_ingot', 4]],
      [['kubejs:ascension_singularity', 4], ['kubejs:ascended_soul', 1], ['kubejs:celestium_ingot', 8]],
      [['kubejs:ascension_singularity', 8], ['kubejs:ascended_soul', 2], ['kubejs:ascendium_ingot', 2]],
      [['kubejs:ascension_singularity', 16], ['kubejs:ascended_soul', 4], ['kubejs:ascendium_ingot', 4]],
      [['kubejs:ascension_singularity', 32], ['kubejs:ascended_soul', 8], ['kubejs:ascendium_ingot', 8], ['minecraft:nether_star', 1]]
    ]
  },

  // ---- Gear sets: wear several pieces with the same enchant for a bonus ----
  // pieces = equipped items (hands + armor) that have the set's enchant.
  sets: [
    { name: 'Juggernaut', key: 'protection', 2: { mult: 1.25 }, 4: { mult: 1.5, add: { vitality: 100, bulwark: 50 } } },
    { name: 'Berserker', key: 'sharpness', 2: { mult: 1.25 }, 4: { mult: 1.5, add: { critical: 50, lifesteal: 25 } } },
    { name: 'Plunderer', key: 'plunder', 2: { mult: 1.25 }, 4: { mult: 1.5, add: { midas: 25, wisdom: 50 } } },
    { name: 'Windwalker', key: 'swiftness', 2: { mult: 1.25 }, 4: { mult: 1.5, add: { evasion: 100, featherweight: 50 } } },
    { name: 'Lifebinder', key: 'vitality', 2: { mult: 1.25 }, 4: { mult: 1.5, add: { renewal: 40, second_wind: 40 } } }
  ],

  // ---- Gems: socket into gear for extra enchant levels ----
  // Made in the factory, then compacted 4 -> 1 into the next tier. Right-click a gem with gear in your
  // other hand to socket it. Sneak on a full item to replace its newest gem (the old gem is destroyed).
  gems: {
    sockets: 3,
    tiers: [{ name: 'Chipped', levels: 3 }, { name: 'Flawless', levels: 15 }, { name: 'Perfect', levels: 75 }],
    // catalyst: [item, count] mixed with a diamond + 250 mB lava (superheated) for a Chipped gem
    types: {
      bloodstone: { name: 'Bloodstone', enchant: 'sharpness', color: [200, 30, 40], catalyst: ['minecraft:redstone', 4] },
      sapphire: { name: 'Sapphire', enchant: 'protection', color: [40, 90, 230], catalyst: ['minecraft:lapis_lazuli', 4] },
      garnet: { name: 'Garnet', enchant: 'vitality', color: [150, 20, 70], catalyst: ['minecraft:nether_wart', 4] },
      topaz: { name: 'Topaz', enchant: 'plunder', color: [240, 180, 40], catalyst: ['minecraft:gold_ingot', 4] },
      opal: { name: 'Opal', enchant: 'swiftness', color: [190, 240, 255], catalyst: ['minecraft:quartz', 4] },
      onyx: { name: 'Onyx', enchant: 'excavation', color: [60, 60, 75], catalyst: ['minecraft:coal_block', 4] }
    }
  },

  // ---- Milestones: factory goals with rewards (checked whenever a stat goes up) ----
  milestones: [
    { id: 'first_core', name: 'First Spark', stat: 'essence_absorbed', goal: 1, reward: [['kubejs:blank_ascension_tome', 2]] },
    { id: 'essence_1k', name: 'Assembly Line', stat: 'essence_absorbed', goal: 1000, reward: [['kubejs:chaos_tome', 1]] },
    { id: 'essence_100k', name: 'Megafactory', stat: 'essence_absorbed', goal: 100000, reward: [['kubejs:chaos_tome', 3], ['kubejs:mythic_fragment', 4]] },
    { id: 'essence_10m', name: 'Planet Eater', stat: 'essence_absorbed', goal: 10000000, reward: [['kubejs:chaos_tome', 10], ['kubejs:mythic_fragment', 16]] },
    { id: 'levels_100', name: 'Tinkerer', stat: 'levels_bought', goal: 100, reward: [['kubejs:gem_bloodstone_1', 1]] },
    { id: 'levels_10k', name: 'Ascendant', stat: 'levels_bought', goal: 10000, reward: [['kubejs:gem_sapphire_2', 1]] },
    { id: 'levels_1m', name: 'Beyond Measure', stat: 'levels_bought', goal: 1000000, reward: [['kubejs:gem_bloodstone_3', 1], ['kubejs:gem_sapphire_3', 1]] },
    { id: 'unlocks_10', name: 'Collector', stat: 'unlocks', goal: 10, reward: [['kubejs:chaos_tome', 1]] },
    { id: 'rolls_25', name: 'Gambler', stat: 'chaos_rolls', goal: 25, reward: [['kubejs:chaos_tome', 5]] },
    { id: 'danger_10', name: 'Explorer', stat: 'max_danger', goal: 10, reward: [['kubejs:mythic_fragment', 2]] },
    { id: 'danger_40', name: 'Nightmare Walker', stat: 'max_danger', goal: 40, reward: [['kubejs:mythic_fragment', 8]] },
    { id: 'elites_50', name: 'Elite Hunter', stat: 'elites_killed', goal: 50, reward: [['kubejs:gem_topaz_2', 1]] },
    { id: 'boss_1', name: 'Godslayer', stat: 'bosses_killed', goal: 1, reward: [['kubejs:chaos_tome', 3]] },
    { id: 'rebirth_1', name: 'Born Again', stat: 'rebirths', goal: 1, reward: [['kubejs:gem_garnet_3', 1]] },
    { id: 'gems_10', name: 'Jeweler', stat: 'gems_socketed', goal: 10, reward: [['kubejs:gem_opal_2', 1]] },
    { id: 'altar_1k', name: 'Automation', stat: 'altar_levels', goal: 1000, reward: [['kubejs:gem_onyx_3', 1]] },
    { id: 'relic_1', name: 'Treasure Hunter', stat: 'relics_found', goal: 1, reward: [['kubejs:chaos_tome', 2]] },
    { id: 'relic_25', name: 'Hoard of Legends', stat: 'relics_found', goal: 25, reward: [['kubejs:ascended_soul', 1]] }
  ],

  // ---- World events: random server-wide events ----
  events: {
    enabled: true,
    checkEveryMinutes: 1,
    chancePerCheck: 0.05,      // about one event every 20 minutes
    bloodMoon: { minutes: 6, dangerBonus: 10, fragmentMult: 2, nightOnly: true },
    // Meteor Shower: anything the pack makes can fall from the sky. Each meteor picks a loot tier (Common 0 ... Mythic 5)
    // with the same odds as a loot drop where it lands, so the farther from home, the better (loot curve), then any item
    // of that tier. The better the item, the higher (rarer) its tier. counts: [min, max] per tier for stackable items,
    // times the loot curve's bulk. pool: the tier of each kind of item. extra: other items, [id, tier].
    meteorShower: { minutes: 3, everySeconds: 15, radius: 30, perWave: 2,
      counts: [[2, 8], [1, 4], [1, 2], [1, 1], [1, 1], [1, 1]],
      pool: {
        basics: 0,             // Ascension Cores and Shards, Ascendite, mineral clusters, Blank Tomes, Polishing Cloths, Rock Hammers
        food: 0,               // every mob part, raw and cooked
        engine: 0,             // Modular Engine parts, connected glass, Storage Linker (Full download)
        ascenditeGear: 1, attachments: 1, packs: 1, denseCore: 1, baseTomes: 1,
        voidsteel: 2, fusionTomes: 2, chaos: 2, machines: 2,   // machines: the mod's other blocks
        celestium: 3, triadTomes: 3, singularity: 3, bigMachines: 3,   // bigMachines: Ascension Vault, Essence Forge, Ascension Workbench
        ascendium: 4, mythicTomes: 4, souls: 4,
        ultimateTomes: 5,
        gems: [1, 2, 3],       // Chipped, Flawless, Perfect
        roomTnt: [0, 1, 2],    // 16, 32, 50
        foils: [0, 0, 1, 1, 2, 3, 4, 5, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5],   // the 10 cased foils are Legendary / Mythic
        condensedPerTier: 10,  // Condensed Essence: blocks 0-9 -> Uncommon, 10-19 -> Rare ... 40+ -> Mythic
        relics: { rare: 2, epic: 3, legendary: 4, mythic: 5 },
        backpacks: [3, 3, 4, 4, 5, 5],  // the Ascension backpacks, smallest first (with Sophisticated Backpacks)
        collars: [1, 2, 3, 4, 5], whistle: 1, charms: 2, ascendedCharm: 5
      },
      extra: [['minecraft:diamond', 1], ['minecraft:ancient_debris', 2], ['minecraft:netherite_ingot', 4]] },
    goldenHour: { minutes: 5, lootMult: 2 }
  },

  // ---- Ascended bosses: rare huge versions of normal mobs, deep in the danger zones ----
  bosses: {
    enabled: true,
    minDanger: 30,             // only this deep or deeper
    chancePerSpawn: 0.002,     // per hostile spawn there
    healthMult: 60,            // on top of the zone's own multiplier
    damageMult: 3,
    minionsPerPhase: 3,        // summoned at 66% and 33% health
    titles: ['the Unending', 'the Ashborn', 'of the Void', 'the Devourer', 'the Hollow King', 'Worldbreaker'],
    drops: [['kubejs:chaos_tome', 1, 3], ['kubejs:mythic_fragment', 3, 8], ['kubejs:dense_ascension_core', 2, 6], ['kubejs:ascended_soul', 1, 2]]
  },

  // ---- Ascendite: a new early-game ore, with its own tools and armor ----
  // Mined from the start, it opens cheap ways into the factory (brass without a Blaze Burner, Precision Mechanisms
  // without sequenced assembly) and Ascension Cores without the expensive diamond line.
  ascendite: {
    ore: {
      veinSize: 8,             // blocks per vein (like iron's 9)
      veinsPerChunk: 10,
      minY: -48, maxY: 80,     // most common halfway between
      shardChance: 0.12,       // chance a mined ore also drops an Ascension Shard (+0.04 per Fortune level)
      shardFortune: 0.04
    },
    shardsPerCore: 4,          // Ascension Shards -> 1 Ascension Core (crafting grid)
    hauntChance: 0.3,          // Encased Fan through soul fire: Crushed Raw Iron -> Crushed Raw Ascendite (renewable)
    pressCobble: 9,            // Mechanical Press over a Basin: this much cobblestone -> 1 Raw Ascendite (Create allows at most 9)
    // Early automated cores, no Blaze Burner needed (the Modular Engine runs all of it):
    // cobblestone -> press -> Raw Ascendite -> Millstone -> Crushed (+ a shard) -> fan through water -> nuggets + redstone
    // -> an unheated Mechanical Mixer: mixerNuggets Ascendite Nuggets + 1 Redstone -> 1 Ascension Core
    millShardChance: 0.25,     // Millstone: Raw Ascendite -> Crushed Raw Ascendite + this chance of an Ascension Shard
    mixerNuggets: 8,           // unheated mixer: this many Ascendite Nuggets + 1 Redstone -> 1 Ascension Core (at most 8: Create mixes up to 9 items)
    // Between iron and diamond. Mines diamonds; levels well with Ascended enchants like any gear.
    tools: { uses: 900, speed: 7.5, attackDamageBonus: 2.5, level: 2, enchantability: 18 },
    armor: {
      durabilityMultiplier: 22,
      protection: [2, 6, 7, 2],  // boots, leggings, chestplate, helmet
      toughness: 1,
      knockbackResistance: 0,
      enchantability: 18
    }
  },

  // ---- Mob food: animal parts that drop on death. Raw makes you sick; cook it (furnace, smoker, campfire) ----
  // Mobs that die burning drop the cooked version. Each cooked food gives a small buff tied to its animal,
  // and several enchant tomes need one of these parts (see global.FA_TOME_CATALYSTS).
  mobFood: {
    enabled: true,
    lootingBonus: 0.05,        // +5% drop chance per Looting level
    raw: {
      hunger: 2, saturation: 0.1,
      // [effect, seconds, amplifier, chance]: what eating it raw does to you
      sickness: [['minecraft:hunger', 30, 1, 1.0], ['minecraft:nausea', 12, 0, 0.6], ['minecraft:poison', 5, 0, 0.3]]
    },
    cookSeconds: 10,           // furnace time (smoker and blast are half, campfire x3)
    // mobs: who drops it; chance: per death; effect: [effect, seconds, amplifier] when eaten cooked. Both raw and cooked
    // can always be eaten, even on a full hunger bar. art: the sprite's shape and colors (tools/food_art.py).
    // mod: only with the Factory Ascension mod (its own mobs).
    foods: [
      { raw: 'pig_eyes', rawName: 'Pig Eyes', cooked: 'fried_pig_eyes', cookedName: 'Fried Pig Eyes', mobs: ['minecraft:pig'], chance: 0.3, hunger: 4, saturation: 0.5, effect: ['minecraft:night_vision', 45, 0] },
      { raw: 'pig_brain', rawName: 'Pig Brain', cooked: 'roasted_pig_brain', cookedName: 'Roasted Pig Brain', mobs: ['minecraft:pig'], chance: 0.2, hunger: 6, saturation: 0.6, effect: ['minecraft:haste', 30, 0] },
      { raw: 'cow_tongue', rawName: 'Cow Tongue', cooked: 'grilled_cow_tongue', cookedName: 'Grilled Cow Tongue', mobs: ['minecraft:cow', 'minecraft:mooshroom'], chance: 0.3, hunger: 8, saturation: 0.8, effect: ['minecraft:saturation', 2, 0] },
      { raw: 'cow_heart', rawName: 'Cow Heart', cooked: 'seared_cow_heart', cookedName: 'Seared Cow Heart', mobs: ['minecraft:cow', 'minecraft:mooshroom'], chance: 0.15, hunger: 7, saturation: 0.7, effect: ['minecraft:health_boost', 60, 0] },
      { raw: 'sheep_liver', rawName: 'Sheep Liver', cooked: 'cooked_sheep_liver', cookedName: 'Cooked Sheep Liver', mobs: ['minecraft:sheep'], chance: 0.25, hunger: 6, saturation: 0.6, effect: ['minecraft:regeneration', 6, 0] },
      { raw: 'chicken_feet', rawName: 'Chicken Feet', cooked: 'fried_chicken_feet', cookedName: 'Fried Chicken Feet', mobs: ['minecraft:chicken'], chance: 0.35, hunger: 4, saturation: 0.5, effect: ['minecraft:slow_falling', 20, 0] },
      { raw: 'rabbit_leg', rawName: 'Rabbit Leg', cooked: 'roasted_rabbit_leg', cookedName: 'Roasted Rabbit Leg', mobs: ['minecraft:rabbit'], chance: 0.35, hunger: 5, saturation: 0.6, effect: ['minecraft:jump_boost', 30, 1] },
      { raw: 'spider_legs', rawName: 'Spider Legs', cooked: 'grilled_spider_legs', cookedName: 'Grilled Spider Legs', mobs: ['minecraft:spider', 'minecraft:cave_spider'], chance: 0.3, hunger: 4, saturation: 0.4, effect: ['minecraft:speed', 30, 0] },
      { raw: 'zombie_brain', rawName: 'Zombie Brain', cooked: 'cooked_zombie_brain', cookedName: 'Cooked Zombie Brain', mobs: ['minecraft:zombie', 'minecraft:husk', 'minecraft:drowned', 'minecraft:zombie_villager'], chance: 0.15, hunger: 5, saturation: 0.4, effect: ['minecraft:strength', 20, 0] },
      { raw: 'marrow_bone', rawName: 'Marrow Bone', cooked: 'bone_marrow_broth', cookedName: 'Bone Marrow Broth', mobs: ['minecraft:skeleton', 'minecraft:stray'], chance: 0.2, hunger: 6, saturation: 0.7, effect: ['minecraft:resistance', 20, 0] },
      { raw: 'creeper_heart', rawName: 'Creeper Heart', cooked: 'smoked_creeper_heart', cookedName: 'Smoked Creeper Heart', mobs: ['minecraft:creeper'], chance: 0.15, hunger: 6, saturation: 0.6, effect: ['minecraft:absorption', 30, 1] },
      { raw: 'squid_tentacle', rawName: 'Squid Tentacle', cooked: 'fried_calamari', cookedName: 'Fried Calamari', mobs: ['minecraft:squid', 'minecraft:glow_squid'], chance: 0.4, hunger: 5, saturation: 0.6, effect: ['minecraft:water_breathing', 60, 0] },
      { raw: 'hoglin_ribs', rawName: 'Hoglin Ribs', cooked: 'bbq_hoglin_ribs', cookedName: 'BBQ Hoglin Ribs', mobs: ['minecraft:hoglin', 'minecraft:zoglin'], chance: 0.35, hunger: 10, saturation: 0.9, effect: ['minecraft:fire_resistance', 45, 0] },
      { raw: 'ender_gland', rawName: "Ender Gland", cooked: 'seared_ender_gland', cookedName: "Seared Ender Gland", mobs: ['minecraft:enderman'], chance: 0.3, hunger: 5, saturation: 0.5, effect: ['minecraft:speed', 30, 1], art: ['gland', [40, 20, 60], [180, 90, 230]] },
      { raw: 'blaze_core', rawName: "Blaze Core", cooked: 'charred_blaze_core', cookedName: "Charred Blaze Core", mobs: ['minecraft:blaze'], chance: 0.3, hunger: 4, saturation: 0.4, effect: ['minecraft:fire_resistance', 60, 0], art: ['core', [255, 170, 40], [255, 240, 140]] },
      { raw: 'ghast_membrane', rawName: "Ghast Membrane", cooked: 'crispy_ghast_membrane', cookedName: "Crispy Ghast Membrane", mobs: ['minecraft:ghast'], chance: 0.5, hunger: 4, saturation: 0.3, effect: ['minecraft:slow_falling', 40, 0], art: ['wing', [236, 236, 236], [190, 190, 200]] },
      { raw: 'slime_core', rawName: "Slime Core", cooked: 'fried_slime_core', cookedName: "Fried Slime Core", mobs: ['minecraft:slime'], chance: 0.2, hunger: 3, saturation: 0.3, effect: ['minecraft:jump_boost', 30, 1], art: ['core', [110, 190, 90], [190, 250, 160]] },
      { raw: 'magma_core', rawName: "Magma Core", cooked: 'seared_magma_core', cookedName: "Seared Magma Core", mobs: ['minecraft:magma_cube'], chance: 0.25, hunger: 4, saturation: 0.4, effect: ['minecraft:fire_resistance', 45, 0], art: ['core', [200, 60, 20], [255, 190, 60]] },
      { raw: 'witch_wart', rawName: "Witch Wart", cooked: 'roasted_witch_wart', cookedName: "Roasted Witch Wart", mobs: ['minecraft:witch'], chance: 0.4, hunger: 4, saturation: 0.4, effect: ['minecraft:luck', 90, 0], art: ['organ', [110, 150, 70], [70, 40, 90]] },
      { raw: 'guardian_spine', rawName: "Guardian Spine", cooked: 'grilled_guardian_spine', cookedName: "Grilled Guardian Spine", mobs: ['minecraft:guardian', 'minecraft:elder_guardian'], chance: 0.35, hunger: 6, saturation: 0.6, effect: ['minecraft:conduit_power', 30, 0], art: ['spine', [120, 170, 160], [230, 140, 80]] },
      { raw: 'phantom_wing', rawName: "Phantom Wing", cooked: 'crispy_phantom_wing', cookedName: "Crispy Phantom Wing", mobs: ['minecraft:phantom'], chance: 0.4, hunger: 4, saturation: 0.4, effect: ['minecraft:slow_falling', 30, 0], art: ['wing', [70, 80, 130], [150, 200, 230]] },
      { raw: 'withered_marrow', rawName: "Withered Marrow", cooked: 'blackened_marrow', cookedName: "Blackened Marrow", mobs: ['minecraft:wither_skeleton'], chance: 0.3, hunger: 6, saturation: 0.6, effect: ['minecraft:resistance', 15, 1], art: ['bone', [60, 60, 64], [150, 40, 50]] },
      { raw: 'piglin_snout', rawName: "Piglin Snout", cooked: 'roasted_piglin_snout', cookedName: "Roasted Piglin Snout", mobs: ['minecraft:piglin', 'minecraft:piglin_brute', 'minecraft:zombified_piglin'], chance: 0.3, hunger: 5, saturation: 0.5, effect: ['minecraft:absorption', 30, 0], art: ['snout', [230, 150, 140], [120, 60, 60]] },
      { raw: 'mite_carapace', rawName: "Mite Carapace", cooked: 'toasted_carapace', cookedName: "Toasted Carapace", mobs: ['minecraft:silverfish', 'minecraft:endermite'], chance: 0.25, hunger: 2, saturation: 0.2, effect: ['minecraft:haste', 30, 0], art: ['carapace', [140, 140, 150], [90, 60, 120]] },
      { raw: 'shulker_tongue', rawName: "Shulker Tongue", cooked: 'seared_shulker_tongue', cookedName: "Seared Shulker Tongue", mobs: ['minecraft:shulker'], chance: 0.35, hunger: 5, saturation: 0.5, effect: ['minecraft:resistance', 20, 0], art: ['tongue', [170, 110, 170], [240, 200, 120]] },
      { raw: 'illager_heart', rawName: "Illager Heart", cooked: 'charred_illager_heart', cookedName: "Charred Illager Heart", mobs: ['minecraft:pillager', 'minecraft:vindicator', 'minecraft:evoker', 'minecraft:illusioner'], chance: 0.25, hunger: 6, saturation: 0.6, effect: ['minecraft:strength', 20, 0], art: ['heart', [140, 40, 60], [90, 90, 100]] },
      { raw: 'vex_wisp', rawName: "Vex Wisp", cooked: 'sizzled_vex_wisp', cookedName: "Sizzled Vex Wisp", mobs: ['minecraft:vex'], chance: 0.3, hunger: 2, saturation: 0.2, effect: ['minecraft:speed', 20, 0], art: ['wisp', [200, 220, 255], [140, 160, 230]] },
      { raw: 'ravager_flank', rawName: "Ravager Flank", cooked: 'seared_ravager_flank', cookedName: "Seared Ravager Flank", mobs: ['minecraft:ravager'], chance: 0.8, hunger: 12, saturation: 1.0, effect: ['minecraft:strength', 30, 1], art: ['flank', [150, 80, 70], [90, 90, 90]] },
      { raw: 'warden_heart', rawName: "Warden Heart", cooked: 'seared_warden_heart', cookedName: "Seared Warden Heart", mobs: ['minecraft:warden'], chance: 1.0, hunger: 14, saturation: 1.2, effect: ['minecraft:health_boost', 120, 2], art: ['heart', [20, 60, 70], [60, 230, 230]] },
      { raw: 'strider_leg', rawName: "Strider Leg", cooked: 'grilled_strider_leg', cookedName: "Grilled Strider Leg", mobs: ['minecraft:strider'], chance: 0.35, hunger: 6, saturation: 0.6, effect: ['minecraft:fire_resistance', 60, 0], art: ['leg', [180, 60, 60], [120, 90, 90]] },
      { raw: 'dragon_heart', rawName: "Dragon Heart", cooked: 'seared_dragon_heart', cookedName: "Seared Dragon Heart", mobs: ['minecraft:ender_dragon'], chance: 1.0, hunger: 20, saturation: 1.2, effect: ['minecraft:regeneration', 60, 2], art: ['heart', [60, 20, 80], [230, 120, 255]] },
      { raw: 'wither_spine', rawName: "Wither Spine", cooked: 'charred_wither_spine', cookedName: "Charred Wither Spine", mobs: ['minecraft:wither'], chance: 1.0, hunger: 16, saturation: 1.2, effect: ['minecraft:resistance', 120, 2], art: ['spine', [40, 40, 44], [120, 120, 130]] },
      { raw: 'bat_wing', rawName: "Bat Wing", cooked: 'fried_bat_wing', cookedName: "Fried Bat Wing", mobs: ['minecraft:bat'], chance: 0.5, hunger: 2, saturation: 0.2, effect: ['minecraft:night_vision', 60, 0], art: ['wing', [80, 60, 50], [130, 100, 80]] },
      { raw: 'horse_haunch', rawName: "Horse Haunch", cooked: 'roasted_haunch', cookedName: "Roasted Haunch", mobs: ['minecraft:horse', 'minecraft:donkey', 'minecraft:mule', 'minecraft:zombie_horse', 'minecraft:skeleton_horse'], chance: 0.4, hunger: 8, saturation: 0.8, effect: ['minecraft:speed', 45, 0], art: ['haunch', [190, 90, 80], [236, 226, 196]] },
      { raw: 'llama_shank', rawName: "Llama Shank", cooked: 'spiced_llama_shank', cookedName: "Spiced Llama Shank", mobs: ['minecraft:llama', 'minecraft:trader_llama'], chance: 0.4, hunger: 8, saturation: 0.8, effect: ['minecraft:saturation', 1, 0], art: ['haunch', [200, 110, 100], [236, 226, 196]] },
      { raw: 'goat_shank', rawName: "Goat Shank", cooked: 'roasted_goat_shank', cookedName: "Roasted Goat Shank", mobs: ['minecraft:goat'], chance: 0.4, hunger: 7, saturation: 0.7, effect: ['minecraft:jump_boost', 40, 1], art: ['haunch', [210, 120, 110], [236, 226, 196]] },
      { raw: 'fox_haunch', rawName: "Fox Haunch", cooked: 'roasted_fox_haunch', cookedName: "Roasted Fox Haunch", mobs: ['minecraft:fox'], chance: 0.35, hunger: 6, saturation: 0.6, effect: ['minecraft:speed', 40, 0], art: ['haunch', [220, 120, 70], [236, 226, 196]] },
      { raw: 'wolf_haunch', rawName: "Wolf Haunch", cooked: 'grilled_wolf_haunch', cookedName: "Grilled Wolf Haunch", mobs: ['minecraft:wolf'], chance: 0.35, hunger: 6, saturation: 0.6, effect: ['minecraft:strength', 20, 0], art: ['haunch', [180, 100, 90], [236, 226, 196]] },
      { raw: 'bear_paw', rawName: "Bear Paw", cooked: 'roasted_bear_paw', cookedName: "Roasted Bear Paw", mobs: ['minecraft:polar_bear'], chance: 0.4, hunger: 9, saturation: 0.8, effect: ['minecraft:resistance', 30, 0], art: ['paw', [236, 236, 230], [60, 50, 50]] },
      { raw: 'panda_paw', rawName: "Panda Paw", cooked: 'braised_panda_paw', cookedName: "Bamboo-Braised Panda Paw", mobs: ['minecraft:panda'], chance: 0.4, hunger: 9, saturation: 0.8, effect: ['minecraft:regeneration', 10, 0], art: ['paw', [40, 40, 44], [230, 230, 226]] },
      { raw: 'turtle_fin', rawName: "Turtle Fin", cooked: 'turtle_fin_soup', cookedName: "Turtle Fin Soup", mobs: ['minecraft:turtle'], chance: 0.4, hunger: 6, saturation: 0.6, effect: ['minecraft:water_breathing', 90, 0], art: ['fin', [90, 150, 80], [200, 220, 150]] },
      { raw: 'frog_legs', rawName: "Frog Legs", cooked: 'fried_frog_legs', cookedName: "Fried Frog Legs", mobs: ['minecraft:frog'], chance: 0.5, hunger: 4, saturation: 0.4, effect: ['minecraft:jump_boost', 30, 1], art: ['leg', [120, 170, 90], [230, 210, 180]] },
      { raw: 'camel_hump', rawName: "Camel Hump", cooked: 'roasted_camel_hump', cookedName: "Roasted Camel Hump", mobs: ['minecraft:camel'], chance: 0.5, hunger: 10, saturation: 1.0, effect: ['minecraft:saturation', 2, 0], art: ['organ', [210, 170, 110], [250, 230, 200]] },
      { raw: 'sniffer_snout', rawName: "Sniffer Snout", cooked: 'roasted_sniffer_snout', cookedName: "Roasted Sniffer Snout", mobs: ['minecraft:sniffer'], chance: 0.5, hunger: 8, saturation: 0.7, effect: ['minecraft:luck', 120, 0], art: ['snout', [180, 70, 60], [80, 130, 80]] },
      { raw: 'bee_stinger', rawName: "Bee Stinger", cooked: 'candied_stinger', cookedName: "Candied Stinger", mobs: ['minecraft:bee'], chance: 0.3, hunger: 2, saturation: 0.3, effect: ['minecraft:regeneration', 4, 0], art: ['wisp', [240, 200, 60], [40, 30, 20]] },
      // The Factory Ascension mod's own mobs (Full download)
      { raw: 'rusty_gizzard', rawName: "Rusty Gizzard", cooked: 'grilled_rusty_gizzard', cookedName: "Grilled Rusty Gizzard", mobs: ['factoryascension:rust_walker'], chance: 0.35, hunger: 6, saturation: 0.6, effect: ['minecraft:haste', 30, 0], art: ['organ', [150, 90, 50], [90, 60, 40]], mod: true },
      { raw: 'glowing_brain', rawName: "Glowing Brain", cooked: 'seared_glowing_brain', cookedName: "Seared Glowing Brain", mobs: ['factoryascension:ascendite_husk'], chance: 0.35, hunger: 6, saturation: 0.6, effect: ['minecraft:night_vision', 90, 0], art: ['organ', [64, 200, 190], [30, 90, 100]], mod: true },
      { raw: 'ember_heart', rawName: "Ember Heart", cooked: 'smoldering_heart', cookedName: "Smoldering Heart", mobs: ['factoryascension:ember_ghoul'], chance: 0.35, hunger: 6, saturation: 0.6, effect: ['minecraft:fire_resistance', 90, 0], art: ['heart', [200, 70, 30], [255, 200, 80]], mod: true },
      { raw: 'frozen_heart', rawName: "Frozen Heart", cooked: 'thawed_heart', cookedName: "Thawed Heart", mobs: ['factoryascension:frost_revenant'], chance: 0.35, hunger: 6, saturation: 0.6, effect: ['minecraft:resistance', 30, 0], art: ['heart', [150, 200, 230], [240, 250, 255]], mod: true },
      { raw: 'miners_lung', rawName: "Miner's Lung", cooked: 'smoked_miners_lung', cookedName: "Smoked Miner's Lung", mobs: ['factoryascension:deep_miner'], chance: 0.35, hunger: 6, saturation: 0.6, effect: ['minecraft:haste', 45, 1], art: ['organ', [140, 130, 120], [60, 60, 60]], mod: true },
      { raw: 'void_marrow', rawName: "Void Marrow", cooked: 'blackened_void_marrow', cookedName: "Blackened Void Marrow", mobs: ['factoryascension:void_archer'], chance: 0.35, hunger: 6, saturation: 0.6, effect: ['minecraft:slow_falling', 60, 0], art: ['bone', [60, 40, 90], [190, 100, 255]], mod: true },
      { raw: 'glowing_marrow', rawName: "Glowing Marrow", cooked: 'roasted_glowing_marrow', cookedName: "Roasted Glowing Marrow", mobs: ['factoryascension:ascendite_skeleton'], chance: 0.35, hunger: 6, saturation: 0.6, effect: ['minecraft:resistance', 30, 0], art: ['bone', [200, 214, 210], [64, 232, 212]], mod: true },
      { raw: 'crystal_fang', rawName: "Crystal Fang", cooked: 'toasted_crystal_fang', cookedName: "Toasted Crystal Fang", mobs: ['factoryascension:crystal_spider'], chance: 0.35, hunger: 4, saturation: 0.4, effect: ['minecraft:haste', 30, 0], art: ['carapace', [130, 86, 190], [255, 200, 255]], mod: true },
      { raw: 'charged_gland', rawName: "Charged Gland", cooked: 'sizzled_charged_gland', cookedName: "Sizzled Charged Gland", mobs: ['factoryascension:spark_spider'], chance: 0.35, hunger: 4, saturation: 0.4, effect: ['minecraft:speed', 30, 1], art: ['gland', [70, 70, 80], [255, 230, 80]], mod: true },
      { raw: 'cinder_gland', rawName: "Cinder Gland", cooked: 'charred_cinder_gland', cookedName: "Charred Cinder Gland", mobs: ['factoryascension:cinder_spider'], chance: 0.35, hunger: 4, saturation: 0.4, effect: ['minecraft:fire_resistance', 60, 0], art: ['gland', [90, 40, 30], [255, 130, 40]], mod: true },
      { raw: 'glowing_creeper_heart', rawName: "Glowing Creeper Heart", cooked: 'smoked_glowing_heart', cookedName: "Smoked Glowing Heart", mobs: ['factoryascension:ascendite_creeper'], chance: 0.4, hunger: 6, saturation: 0.6, effect: ['minecraft:absorption', 30, 1], art: ['heart', [46, 140, 140], [64, 232, 212]], mod: true },
      { raw: 'frost_core', rawName: "Frost Core", cooked: 'thawed_frost_core', cookedName: "Thawed Frost Core", mobs: ['factoryascension:frost_creeper'], chance: 0.4, hunger: 5, saturation: 0.5, effect: ['minecraft:resistance', 20, 1], art: ['core', [170, 210, 230], [255, 255, 255]], mod: true },
      { raw: 'essence_gel', rawName: "Essence Gel", cooked: 'candied_essence_gel', cookedName: "Candied Essence Gel", mobs: ['factoryascension:essence_slime'], chance: 0.15, hunger: 4, saturation: 0.6, effect: ['minecraft:regeneration', 8, 1], art: ['core', [170, 96, 255], [240, 210, 255]], mod: true },
      { raw: 'copper_gel', rawName: "Copper Gel", cooked: 'fried_copper_gel', cookedName: "Fried Copper Gel", mobs: ['factoryascension:rust_slime'], chance: 0.15, hunger: 4, saturation: 0.4, effect: ['minecraft:haste', 30, 0], art: ['core', [90, 170, 130], [200, 120, 60]], mod: true },
      { raw: 'ore_mite_shell', rawName: "Ore Mite Shell", cooked: 'toasted_ore_shell', cookedName: "Toasted Ore Shell", mobs: ['factoryascension:ore_mite'], chance: 0.2, hunger: 2, saturation: 0.3, effect: ['minecraft:luck', 120, 0], art: ['carapace', [120, 120, 126], [230, 190, 60]], mod: true },
      { raw: 'wisp_core', rawName: "Wisp Core", cooked: 'seared_wisp_core', cookedName: "Seared Wisp Core", mobs: ['factoryascension:teal_wisp'], chance: 0.35, hunger: 5, saturation: 0.5, effect: ['minecraft:speed', 45, 0], art: ['wisp', [60, 200, 200], [220, 255, 250]], mod: true },
      { raw: 'void_eye', rawName: "Void Eye", cooked: 'seared_void_eye', cookedName: "Seared Void Eye", mobs: ['factoryascension:void_stalker'], chance: 0.35, hunger: 5, saturation: 0.5, effect: ['minecraft:night_vision', 120, 0], art: ['gland', [30, 20, 40], [200, 110, 255]], mod: true },
      { raw: 'raider_heart', rawName: "Raider Heart", cooked: 'charred_raider_heart', cookedName: "Charred Raider Heart", mobs: ['factoryascension:factory_raider'], chance: 0.35, hunger: 7, saturation: 0.7, effect: ['minecraft:strength', 30, 0], art: ['heart', [150, 60, 60], [150, 110, 50]], mod: true },
      { raw: 'ember_wings', rawName: "Ember Wings", cooked: 'hot_ember_wings', cookedName: "Hot Ember Wings", mobs: ['factoryascension:ember_hen'], chance: 0.4, hunger: 4, saturation: 0.4, effect: ['minecraft:fire_resistance', 60, 0], art: ['wing', [230, 120, 40], [255, 220, 90]], mod: true },
      { raw: 'copper_tongue', rawName: "Copper Tongue", cooked: 'grilled_copper_tongue', cookedName: "Grilled Copper Tongue", mobs: ['factoryascension:copper_cow'], chance: 0.35, hunger: 7, saturation: 0.7, effect: ['minecraft:haste', 45, 0], art: ['tongue', [190, 110, 70], [80, 170, 130]], mod: true },
      { raw: 'bog_heart', rawName: "Bog Heart", cooked: 'smoked_bog_heart', cookedName: "Smoked Bog Heart", mobs: ['factoryascension:bog_lurker'], chance: 0.35, hunger: 6, saturation: 0.6, effect: ['minecraft:water_breathing', 90, 0], art: ['heart', [70, 90, 50], [150, 180, 90]], mod: true },
      { raw: 'wraith_dust_gland', rawName: "Wraith Gland", cooked: 'roasted_wraith_gland', cookedName: "Roasted Wraith Gland", mobs: ['factoryascension:sand_wraith'], chance: 0.35, hunger: 5, saturation: 0.5, effect: ['minecraft:fire_resistance', 60, 0], art: ['gland', [200, 176, 120], [120, 90, 50]], mod: true },
      { raw: 'thunder_core', rawName: "Thunder Core", cooked: 'charged_thunder_core', cookedName: "Charged Thunder Core", mobs: ['factoryascension:thunder_creeper'], chance: 0.4, hunger: 5, saturation: 0.5, effect: ['minecraft:speed', 40, 1], art: ['core', [56, 56, 72], [240, 224, 80]], mod: true },
      { raw: 'brass_marrow', rawName: "Brass Marrow", cooked: 'roasted_brass_marrow', cookedName: "Roasted Brass Marrow", mobs: ['factoryascension:brass_sentinel'], chance: 0.35, hunger: 6, saturation: 0.6, effect: ['minecraft:resistance', 30, 0], art: ['bone', [180, 140, 60], [255, 210, 120]], mod: true },
      { raw: 'shadow_marrow', rawName: "Shadow Marrow", cooked: 'blackened_shadow_marrow', cookedName: "Blackened Shadow Marrow", mobs: ['factoryascension:shadow_skeleton'], chance: 0.35, hunger: 6, saturation: 0.6, effect: ['minecraft:night_vision', 120, 0], art: ['bone', [30, 30, 38], [140, 80, 220]], mod: true },
      { raw: 'weaver_spinneret', rawName: "Weaver Spinneret", cooked: 'toasted_spinneret', cookedName: "Toasted Spinneret", mobs: ['factoryascension:cave_weaver'], chance: 0.35, hunger: 4, saturation: 0.4, effect: ['minecraft:jump_boost', 30, 1], art: ['gland', [170, 170, 180], [255, 255, 255]], mod: true },
      { raw: 'moss_gel', rawName: "Moss Gel", cooked: 'fried_moss_gel', cookedName: "Fried Moss Gel", mobs: ['factoryascension:moss_slime'], chance: 0.15, hunger: 4, saturation: 0.5, effect: ['minecraft:regeneration', 6, 0], art: ['core', [78, 140, 50], [160, 210, 90]], mod: true },
      { raw: 'glow_mite_shell', rawName: "Glow Mite Shell", cooked: 'toasted_glow_shell', cookedName: "Toasted Glow Shell", mobs: ['factoryascension:glow_mite'], chance: 0.2, hunger: 2, saturation: 0.3, effect: ['minecraft:night_vision', 90, 0], art: ['carapace', [110, 100, 80], [255, 220, 90]], mod: true },
      { raw: 'storm_core', rawName: "Storm Core", cooked: 'seared_storm_core', cookedName: "Seared Storm Core", mobs: ['factoryascension:storm_wisp'], chance: 0.35, hunger: 5, saturation: 0.5, effect: ['minecraft:speed', 45, 1], art: ['wisp', [200, 200, 220], [255, 240, 100]], mod: true },
      { raw: 'treasure_heart', rawName: "Treasure Heart", cooked: 'gilded_heart', cookedName: "Gilded Heart", mobs: ['factoryascension:treasure_raider'], chance: 0.5, hunger: 8, saturation: 0.8, effect: ['minecraft:luck', 300, 1], art: ['heart', [200, 160, 50], [60, 140, 80]], mod: true },
      { raw: 'colossus_heart', rawName: "Colossus Heart", cooked: 'forged_colossus_heart', cookedName: "Forged Colossus Heart", mobs: ['factoryascension:rust_colossus'], chance: 1.0, hunger: 16, saturation: 1.2, effect: ['minecraft:health_boost', 300, 3], art: ['heart', [150, 82, 40], [255, 110, 30]], mod: true },
      { raw: 'brood_gland', rawName: "Brood Gland", cooked: 'seared_brood_gland', cookedName: "Seared Brood Gland", mobs: ['factoryascension:broodmother'], chance: 1.0, hunger: 14, saturation: 1.2, effect: ['minecraft:speed', 300, 2], art: ['gland', [96, 56, 120], [255, 120, 255]], mod: true },
      { raw: 'kings_marrow', rawName: "King's Marrow", cooked: 'roasted_kings_marrow', cookedName: "Roasted King's Marrow", mobs: ['factoryascension:hollow_king'], chance: 1.0, hunger: 14, saturation: 1.2, effect: ['minecraft:resistance', 300, 2], art: ['bone', [220, 220, 206], [232, 176, 58]], mod: true },
      { raw: 'tyrant_core', rawName: "Tyrant Core", cooked: 'quenched_tyrant_core', cookedName: "Quenched Tyrant Core", mobs: ['factoryascension:wisp_tyrant'], chance: 1.0, hunger: 14, saturation: 1.2, effect: ['minecraft:fire_resistance', 600, 0], art: ['core', [20, 120, 120], [220, 255, 250]], mod: true },
      { raw: 'astral_ember', rawName: "Astral Ember", cooked: 'starlit_ember', cookedName: "Starlit Ember", mobs: ['factoryascension:astral_wisp'], chance: 0.35, hunger: 5, saturation: 0.6, effect: ['minecraft:slow_falling', 60, 0], art: ['wisp', [220, 200, 120], [255, 250, 200]], mod: true },
      { raw: 'void_sinew', rawName: "Void Sinew", cooked: 'smoked_void_sinew', cookedName: "Smoked Void Sinew", mobs: ['factoryascension:void_knight'], chance: 0.35, hunger: 6, saturation: 0.6, effect: ['minecraft:strength', 60, 0], art: ['gland', [70, 50, 100], [220, 160, 255]], mod: true },
      { raw: 'sovereign_crown_shard', rawName: "Sovereign's Heart", cooked: 'gilded_sovereign_heart', cookedName: "Gilded Sovereign's Heart", mobs: ['factoryascension:astral_sovereign'], chance: 1.0, hunger: 16, saturation: 1.4, effect: ['minecraft:regeneration', 300, 1], art: ['heart', [240, 232, 200], [255, 230, 140]], mod: true },
      { raw: 'behemoth_core', rawName: "Behemoth Core", cooked: 'quenched_behemoth_core', cookedName: "Quenched Behemoth Core", mobs: ['factoryascension:void_behemoth'], chance: 1.0, hunger: 16, saturation: 1.4, effect: ['minecraft:strength', 300, 2], art: ['core', [60, 40, 90], [255, 120, 255]], mod: true }
    ]
  },

  // ---- Foils: shiny finishes on enchant tomes ----
  // A foil multiplies the tome's level when it's applied to gear (a level 100 tome with Gold Foil goes on as 150).
  // Each foil also changes how the tome looks, so you can tell its foil at a glance. Pack cards roll foils (better
  // cards, better odds); Foil Sheets are crafted and applied by right-clicking with the tome in your other hand.
  // weight: how often it rolls; recipe: shapeless ingredients for its Foil Sheet.
  foils: {
    enabled: true,
    types: [
      { id: 'bronze', name: 'Bronze', mult: 1.1, weight: 40, color: 'gold', recipe: ['minecraft:paper', 'minecraft:copper_ingot', 'minecraft:copper_ingot'] },
      { id: 'silver', name: 'Silver', mult: 1.25, weight: 25, color: 'white', recipe: ['minecraft:paper', 'minecraft:iron_ingot', 'minecraft:iron_ingot', 'minecraft:quartz'] },
      { id: 'gold', name: 'Gold', mult: 1.5, weight: 15, color: 'yellow', recipe: ['minecraft:paper', 'minecraft:gold_ingot', 'minecraft:gold_ingot', 'kubejs:ascendite_ingot'] },
      { id: 'holo', name: 'Holographic', mult: 2, weight: 10, color: 'aqua', recipe: ['minecraft:paper', 'minecraft:prismarine_crystals', 'minecraft:amethyst_shard', 'kubejs:ascension_shard', 'minecraft:glass_pane'] },
      { id: 'prismatic', name: 'Prismatic', mult: 3, weight: 6, color: 'light_purple', recipe: ['minecraft:paper', 'minecraft:diamond', 'minecraft:red_dye', 'minecraft:yellow_dye', 'minecraft:lime_dye', 'minecraft:light_blue_dye', 'minecraft:purple_dye'] },
      { id: 'galaxy', name: 'Galaxy', mult: 5, weight: 3, color: 'dark_purple', recipe: ['minecraft:paper', 'kubejs:voidsteel_ingot', 'minecraft:ender_eye', 'kubejs:mythic_fragment'] },
      { id: 'void', name: 'Void', mult: 8, weight: 0.8, color: 'dark_gray', recipe: ['minecraft:paper', 'kubejs:celestium_ingot', 'minecraft:crying_obsidian', 'kubejs:mythic_fragment', 'kubejs:mythic_fragment'] },
      { id: 'celestial', name: 'Celestial', mult: 12, weight: 0.2, color: 'gold', recipe: ['minecraft:paper', 'kubejs:ascendium_ingot', 'kubejs:ascension_singularity', 'minecraft:nether_star'] },
      // Cased foils: collector-grade foils sealed in a tinted clear case (cased: true). Each is made from the one before it.
      // art: [main rgb, deep rgb] for the case tint and the foil's pattern (tools/tome_art.py, tools/item_models.py).
      { id: 'ruby', name: 'Ruby', mult: 15, weight: 0.12, color: 'red', cased: true, art: [[255, 70, 90], [120, 10, 34]],
        recipe: ['kubejs:foil_sheet_celestial', 'minecraft:tinted_glass', 'minecraft:glass_pane', 'minecraft:glass_pane', 'minecraft:redstone_block', 'kubejs:mythic_fragment'] },
      { id: 'sapphire', name: 'Sapphire', mult: 18, weight: 0.09, color: 'blue', cased: true, art: [[90, 140, 255], [20, 30, 120]],
        recipe: ['kubejs:foil_sheet_ruby', 'minecraft:tinted_glass', 'minecraft:lapis_block', 'kubejs:mythic_fragment'] },
      { id: 'emerald', name: 'Emerald', mult: 22, weight: 0.07, color: 'green', cased: true, art: [[80, 240, 130], [10, 100, 50]],
        recipe: ['kubejs:foil_sheet_sapphire', 'minecraft:tinted_glass', 'minecraft:emerald_block', 'kubejs:mythic_fragment'] },
      { id: 'amber', name: 'Amber', mult: 27, weight: 0.05, color: 'gold', cased: true, art: [[255, 190, 60], [150, 70, 10]],
        recipe: ['kubejs:foil_sheet_emerald', 'minecraft:tinted_glass', 'minecraft:honey_block', 'minecraft:gold_block', 'kubejs:mythic_fragment'] },
      { id: 'aurora', name: 'Aurora', mult: 33, weight: 0.035, color: 'aqua', cased: true, art: [[110, 255, 220], [120, 60, 200]],
        recipe: ['kubejs:foil_sheet_amber', 'minecraft:tinted_glass', 'minecraft:prismarine_crystals', 'minecraft:amethyst_block', 'kubejs:ascended_soul'] },
      { id: 'obsidian', name: 'Obsidian', mult: 40, weight: 0.025, color: 'dark_purple', cased: true, art: [[150, 80, 230], [24, 10, 40]],
        recipe: ['kubejs:foil_sheet_aurora', 'minecraft:tinted_glass', 'minecraft:crying_obsidian', 'minecraft:crying_obsidian', 'kubejs:ascended_soul'] },
      { id: 'inferno', name: 'Inferno', mult: 50, weight: 0.016, color: 'red', cased: true, art: [[255, 130, 30], [150, 20, 10]],
        recipe: ['kubejs:foil_sheet_obsidian', 'minecraft:tinted_glass', 'minecraft:magma_block', 'minecraft:blaze_rod', 'kubejs:ascended_soul'] },
      { id: 'glacier', name: 'Glacier', mult: 62, weight: 0.01, color: 'aqua', cased: true, art: [[210, 245, 255], [60, 140, 200]],
        recipe: ['kubejs:foil_sheet_inferno', 'minecraft:tinted_glass', 'minecraft:blue_ice', 'minecraft:blue_ice', 'kubejs:ascension_singularity'] },
      { id: 'nebula', name: 'Nebula', mult: 80, weight: 0.006, color: 'light_purple', cased: true, art: [[255, 120, 230], [50, 20, 110]],
        recipe: ['kubejs:foil_sheet_glacier', 'minecraft:tinted_glass', 'minecraft:nether_star', 'kubejs:ascension_singularity'] },
      { id: 'eclipse', name: 'Eclipse', mult: 100, weight: 0.003, color: 'dark_gray', cased: true, art: [[255, 220, 120], [10, 8, 14]],
        recipe: ['kubejs:foil_sheet_nebula', 'minecraft:tinted_glass', 'minecraft:nether_star', 'minecraft:dragon_breath', 'kubejs:ascendium_ingot'] }
    ],
    // Chance a pack card comes foiled, by card rarity (Common .. Secret Rare); foil quality leans up with rarity
    cardChance: [0.08, 0.15, 0.3, 1, 1, 1],
    rarityBoost: 0.6,          // per rarity step, better foils' weights grow by (1 + this) ^ foil tier
    lootChance: 0.15           // chance a tome from danger-zone loot comes foiled
  },

  // ---- Jackpots: the rare insane find ----
  // Relic and card enchant levels have a long lucky tail: most rolls are normal, a few are many times higher, and a
  // sliver reach levelCap. levelAlpha sets how fat the tail is (lower = more huge rolls): with 1.6, a roll x10 higher
  // happens about 1 in 40 times, x100 about 1 in 1,600, x10,000 about 1 in 2.5 million.
  // Chests: chestChance per chest of a jackpot stack, one of chestStacks ([item, min, max, weight]).
  jackpot: {
    levelAlpha: 1.6,
    levelCap: 100000,
    chestChance: 0.015,
    chestStacks: [['kubejs:enchantment_pack', 10, 40, 5], ['kubejs:gear_cache', 4, 16, 4], ['kubejs:booster_box', 2, 6, 1], ['kubejs:ascension_core', 64, 192, 3]]
  },

  // ---- Enchantment card packs: foil packs of tome "cards", and booster boxes of packs ----
  packs: {
    cards: 5,                  // tomes per pack, revealed one by one
    lastCardRare: true,        // the last card is always Rare or better
    boxPacks: 12,              // packs in a Booster Box
    // kind: which tomes ('base', 'fusion', 'mythic', 'ultimate'); level: [min, max] the tome comes pre-leveled to
    rarities: [
      { name: 'Common', weight: 60, color: 'white', kind: 'base', level: [1, 1] },
      { name: 'Uncommon', weight: 25, color: 'green', kind: 'base', level: [5, 25] },
      { name: 'Rare', weight: 10, color: 'aqua', kind: 'base', level: [50, 150] },
      { name: 'Holo Rare', weight: 3.5, color: 'light_purple', kind: 'fusion', level: [10, 100], holo: true },
      { name: 'Mythic Rare', weight: 1.2, color: 'gold', kind: 'mythic', level: [1, 25], holo: true },
      { name: 'Secret Rare', weight: 0.3, color: 'red', kind: 'ultimate', level: [1, 5], holo: true }
    ],
    // Vanilla chests: [chance of a pack, chance of a booster box] per chest
    chests: {
      'minecraft:chests/simple_dungeon': [0.3, 0], 'minecraft:chests/abandoned_mineshaft': [0.25, 0],
      'minecraft:chests/desert_pyramid': [0.3, 0.02], 'minecraft:chests/jungle_temple': [0.3, 0.02],
      'minecraft:chests/shipwreck_treasure': [0.3, 0.02], 'minecraft:chests/buried_treasure': [0.5, 0.05],
      'minecraft:chests/pillager_outpost': [0.25, 0], 'minecraft:chests/ruined_portal': [0.15, 0],
      'minecraft:chests/stronghold_corridor': [0.3, 0.03], 'minecraft:chests/stronghold_library': [0.6, 0.08],
      'minecraft:chests/nether_bridge': [0.25, 0.02], 'minecraft:chests/bastion_treasure': [0.6, 0.15],
      'minecraft:chests/woodland_mansion': [0.5, 0.1], 'minecraft:chests/ancient_city': [0.6, 0.15],
      'minecraft:chests/end_city_treasure': [0.6, 0.2],
      // The pack's structures (Full download)
      'factoryascension:chests/factory_ruin': [0.5, 0.03], 'factoryascension:chests/ascension_crypt': [0.6, 0.08],
      'factoryascension:chests/sky_shrine': [0.8, 0.15], 'factoryascension:chests/realm_vault': [1, 0.35]
    }
  },

  // ---- Sealed Gear Caches: relic gear found in vanilla chests ----
  // Right-click a cache to break the seal: one piece of relic gear (weapon, armor or tool). Its quality depends on the
  // danger level where you open it, so chests out in the danger zones give much better gear.
  caches: {
    // weight at danger 0 and growth per danger level, like loot tiers
    qualities: [
      { quality: 'rare', weight: 70, growth: 0 },
      { quality: 'epic', weight: 25, growth: 0.04 },
      { quality: 'legendary', weight: 5, growth: 0.1 },
      { quality: 'mythic', weight: 0.5, growth: 0.15 }
    ],
    // [chance of a cache, chance of a second one] per vanilla chest
    chests: {
      'minecraft:chests/simple_dungeon': [0.45, 0.1], 'minecraft:chests/abandoned_mineshaft': [0.35, 0.05],
      'minecraft:chests/desert_pyramid': [0.5, 0.15], 'minecraft:chests/jungle_temple': [0.5, 0.15],
      'minecraft:chests/shipwreck_treasure': [0.4, 0.1], 'minecraft:chests/buried_treasure': [0.6, 0.2],
      'minecraft:chests/pillager_outpost': [0.5, 0.15], 'minecraft:chests/ruined_portal': [0.35, 0.05],
      'minecraft:chests/igloo_chest': [0.4, 0.05], 'minecraft:chests/village/village_weaponsmith': [0.4, 0.05],
      'minecraft:chests/village/village_armorer': [0.4, 0.05], 'minecraft:chests/village/village_toolsmith': [0.4, 0.05],
      'minecraft:chests/stronghold_corridor': [0.6, 0.2], 'minecraft:chests/stronghold_crossing': [0.5, 0.15],
      'minecraft:chests/nether_bridge': [0.55, 0.2], 'minecraft:chests/bastion_treasure': [0.9, 0.6],
      'minecraft:chests/bastion_other': [0.5, 0.15], 'minecraft:chests/bastion_bridge': [0.5, 0.15],
      'minecraft:chests/woodland_mansion': [0.8, 0.4], 'minecraft:chests/ancient_city': [0.9, 0.5],
      'minecraft:chests/end_city_treasure': [0.9, 0.6],
      'factoryascension:chests/factory_ruin': [0.4, 0.1], 'factoryascension:chests/ascension_crypt': [0.7, 0.3],
      'factoryascension:chests/sky_shrine': [0.8, 0.4], 'factoryascension:chests/realm_vault': [1, 0.8]
    }
  },

  // ---- Artifacts: true one-of-one gear ----
  // Awaken gear in /ascension once its enchants total awakenLevels (or find one: a Mythic Relic sometimes drops as one).
  // Each Artifact gets a generated name, a world serial number, a signature color and aura, a lore line, and
  // `quirks` mechanics from the list below with randomly rolled strength, so no two are ever the same.
  artifacts: {
    enabled: true,
    awakenLevels: 2500,        // total enchant levels the gear needs
    awakenEssence: 25000,
    awakenSouls: 1,            // Ascended Souls used up
    relicChance: 0.15,         // chance a Mythic Relic drops as an Artifact
    quirks: [3, 4],            // how many quirks [min, max]
    // id: what it does ({p} = its rolled power); for: weapon, armor, tool or any; power: [min, max]
    list: [
      { id: 'echo', name: 'Echo Strike', for: 'weapon', power: [10, 35], text: '{p}% chance a hit lands twice' },
      { id: 'evolving', name: 'Evolving', for: 'any', power: [1, 3], text: '+{p}% damage for every 100 kills it has made' },
      { id: 'nightstalker', name: 'Nightstalker', for: 'weapon', power: [30, 100], text: '+{p}% damage at night' },
      { id: 'sunblessed', name: 'Sunblessed', for: 'any', power: [1, 3], text: 'heals {p} health a second in daylight' },
      { id: 'vortex', name: 'Vortex', for: 'weapon', power: [4, 8], text: 'every 10 hits pulls enemies within {p} blocks into the target' },
      { id: 'overcharge', name: 'Overcharge', for: 'weapon', power: [50, 150], text: 'every 8 hits a shockwave deals {p}% of the hit around the target' },
      { id: 'gilded', name: 'Gilded', for: 'weapon', power: [5, 25], text: 'kills drop gold, {p}% chance of an Ascension Core' },
      { id: 'second_life', name: 'Second Life', for: 'armor', power: [240, 600], text: 'a killing blow leaves you at half health instead (every {p}s)', lowerIsBetter: true },
      { id: 'colossus', name: 'Colossus', for: 'armor', power: [4, 12], text: '+{p} max health' },
      { id: 'quickdraw', name: 'Quickdraw', for: 'weapon', power: [10, 40], text: '+{p}% attack speed' },
      { id: 'soulbound', name: 'Soulbound', for: 'any', power: [1, 1], text: 'stays with you when you die' },
      { id: 'featherfall', name: 'Featherfall', for: 'armor', power: [1, 1], text: 'no fall damage' },
      { id: 'prospector', name: 'Prospector', for: 'tool', power: [2, 8], text: '{p}% chance stone you mine drops Raw Ascendite' }
    ],
    names: {
      first: ['Vael', 'Kor', 'Ith', 'Zar', 'Mor', 'Sel', 'Drak', 'Aur', 'Nyx', 'Thal', 'Ves', 'Orr', 'Kael', 'Syl', 'Eld', 'Rhun'],
      second: ['thorn', 'gar', 'iel', 'eth', 'orin', 'ax', 'ys', 'umbra', 'ion', 'ash', 'wyn', 'mere', 'dris', 'kael'],
      titles: ['the Dawnshatter', 'Bane of Kings', 'the Last Light', 'Worldsong', 'the Hollow Star', 'Ember of Ages', 'the Unwritten',
        "Grief's Edge", 'the Silent Storm', 'Heart of the Forge', 'the Unbroken Oath', 'Starfall Remnant', 'the Ninth Ascension', 'Echo of the Void'],
      lore: ['Forged in a factory that never slept.', 'It remembers every hand that held it.', 'There will never be another.',
        'Whispers in a language older than stone.', 'Its edge was quenched in liquid Essence.', 'Some say it chose its owner.',
        'Light bends strangely around it.', 'It hums a little louder in the dark.']
    },
    colors: ['gold', 'light_purple', 'aqua', 'red', 'green', 'yellow'],
    auras: ['1 0.8 0.2', '0.8 0.4 1', '0.3 1 1', '1 0.3 0.3', '0.4 1 0.4', '1 1 0.4']
  },

  // ---- Machines from the Factory Ascension mod (the "Full" download's jar; ignored without it) ----
  machines: {
    // Ascension Vault: 108 slots, +108 per upgrade. Right-click the vault holding the next upgrade's items.
    vaultUpgrades: [['kubejs:voidsteel_ingot', 8], ['kubejs:celestium_ingot', 8], ['kubejs:ascendium_ingot', 4]],
    openerSeconds: 1,          // the Pack Opener opens one pack this often
    // Salvager: /ascension sell as a machine (same prices). Loot it can't sell passes straight through to its outputs.
    salvager: {
      seconds: 1,              // it salvages this often...
      itemsPerRun: 4,          // ...this many items each time
      artifacts: false         // never grinds up an Artifact (it passes through instead)
    },
    // Tome Fusion Table: merge copies of one tome (their levels add up; foils are baked into the level),
    // or build a Fusion / Triad from its parent tomes (the same tomes the mixer recipe uses)
    fusionTable: {
      mergeCores: 0,           // Ascension Cores used per merge
      fusionCores: 1,          // ...per Fusion (like the mixer recipe)
      triadCores: 2,           // ...per Triad
      keepLevel: true          // a Fusion made here starts at its weakest parent's level (the mixer always makes level 1)
    },
    // Super Smelter (Create machine; shaft in the back): smelts a batch at once, faster the faster it spins.
    // tiers: [items per batch, stress in SU per RPM]; upgrades: right-click it with these to reach the next tier.
    smelter: {
      tiers: [[4, 4], [8, 6], [16, 8], [32, 12], [64, 16], [128, 24]],
      upgrades: [['kubejs:voidsteel_ingot', 8], ['kubejs:celestium_ingot', 8], ['kubejs:ascendium_ingot', 4],
        ['kubejs:ascension_singularity', 4], ['kubejs:ascended_soul', 2]],
      cookTicksAt64Rpm: 40,    // ticks per batch at 64 RPM (half that at 128, a quarter at 256)
      minRpm: 8
    },
    // Ascension Quarry (Create machine; shaft in the back): mines out the square below it, layer by layer, down to the
    // bottom of the world, like a diamond pickaxe. Each block costs `work` (the shaft's RPM adds up every tick: 128 work
    // at 128 RPM = a block a tick). Skips bedrock, fluids and anything with an inventory. Drops go into a chest on top.
    quarry: { radius: 8, work: 128, maxPerTick: 8, minRpm: 8, stress: 16 },
    // Mob Grinder (Create machine; shaft in the back): hits every mob in a box in front of it (reach deep, 5 wide, 3 high)
    // for damagePerRpm x RPM, every interval ticks at 64 RPM (faster at higher speed). Kills count as a player's (rare
    // drops, loot rolls). Never hits players, villagers, pets or named mobs. Drops go into a chest under it.
    grinder: { reach: 5, damagePerRpm: 0.5, interval: 20, minRpm: 8, stress: 12 },

    // Modular Engine (Create generator): an Engine Core plus modules touching it or each other, in any shape.
    // Coil: +suPerCoil stress capacity. Gearbox: next speed in rpm. Burner: burns furnace fuel, powers coilsPerBurner Coils.
    engine: {
      suPerCoil: 256,          // a Coil is worth about a water wheel
      coilsPerBurner: 4,
      maxModules: 32,
      rpm: [16, 32, 64, 128, 256],   // speed with 0, 1, 2, 3, 4+ Gearboxes (same SU, faster shaft)
      fuelMultiplier: 1        // fuel lasts this many times its furnace burn time (1 coal = 80 s)
    },
    // Mini Crusher: Create's crushing (and milling) recipes without the big crushing wheels or any rotation. It needs a
    // motor in its motor slot (the motor sets the speed) and a battery (attachments.batteries) in its battery slot.
    miniCrusher: {
      ticksPerItem: 100,       // at speed x1 (Create's own processing time is used if a recipe sets one)
      chargePerItem: 2,        // battery charge per item crushed
      // Motors: crafted (shaped recipe), speed multiplies how fast it crushes
      motors: [
        { id: 'electric_motor', name: 'Electric Motor', speed: 1,
          recipe: [['ICI', 'CRC', 'ISI'], { I: 'minecraft:iron_ingot', C: 'minecraft:copper_ingot', R: 'minecraft:redstone', S: 'create:shaft' }] },
        { id: 'ascendite_motor', name: 'Ascendite Motor', speed: 2,
          recipe: [['ACA', 'CMC', 'AGA'], { A: 'kubejs:ascendite_ingot', C: 'minecraft:copper_ingot', M: 'kubejs:electric_motor', G: 'create:cogwheel' }] },
        { id: 'essence_motor', name: 'Essence Motor', speed: 4,
          recipe: [['VDV', 'DMD', 'VPV'], { V: 'kubejs:voidsteel_ingot', D: 'kubejs:dense_ascension_core', M: 'kubejs:ascendite_motor', P: 'create:precision_mechanism' }] }
      ]
    },
    // Universal Storage Terminal: link chests to it with a Storage Linker, then use it as one big chest.
    // range: how far a linked chest may be from the terminal (0 = any distance in the same dimension).
    storage: { range: 128, maxLinks: 128 },
    // Connected glass: 8 Glass around the center item -> 8 of that glass (crafting grid)
    glass: { factory_glass: 'create:andesite_alloy', ascendite_glass: 'kubejs:ascendite_nugget', gilded_glass: 'minecraft:gold_nugget', smoked_glass: 'minecraft:coal' },
    recipes: {
      ascension_vault: [['ABA', 'CDC', 'ABA'], { A: 'kubejs:ascendite_ingot', B: 'kubejs:ascendite_block', C: 'minecraft:chest', D: 'kubejs:dense_ascension_core' }],
      card_binder: [['LBL', 'BCB', 'AAA'], { L: 'minecraft:leather', B: 'minecraft:book', C: 'kubejs:ascension_core', A: 'kubejs:ascendite_ingot' }],
      essence_forge: [['IAI', 'BDB', 'OOO'], { I: 'kubejs:ascendite_ingot', A: 'minecraft:anvil', B: 'create:brass_casing', D: 'kubejs:dense_ascension_core', O: 'minecraft:obsidian' }],
      pack_opener: [['IPI', 'CDC', 'IRI'], { I: 'kubejs:ascendite_ingot', P: 'minecraft:piston', C: 'minecraft:chest', D: 'kubejs:dense_ascension_core', R: 'minecraft:redstone_block' }],
      salvager: [['IHI', 'GCG', 'IAI'], { I: 'kubejs:ascendite_ingot', H: 'minecraft:hopper', G: 'minecraft:grindstone', C: 'kubejs:ascension_core', A: 'create:andesite_casing' }],
      tome_fusion_table: [['BEB', 'LCL', 'AAA'], { B: 'minecraft:book', E: 'minecraft:enchanting_table', L: 'minecraft:lapis_block', C: 'kubejs:ascension_core', A: 'kubejs:ascendite_ingot' }],
      super_smelter: [['IBI', 'FCF', 'ISI'], { I: 'kubejs:ascendite_ingot', B: 'minecraft:blast_furnace', F: 'minecraft:furnace', C: 'kubejs:ascension_core', S: 'create:shaft' }],
      // The engine is for day one: iron, copper, Create's andesite parts and a furnace
      engine_core: [['ICI', 'SFS', 'IAI'], { I: 'minecraft:iron_ingot', C: 'create:cogwheel', S: 'create:shaft', F: 'minecraft:furnace', A: 'create:andesite_alloy' }],
      engine_coil: [['CAC', 'ARA', 'CAC'], { C: 'minecraft:copper_ingot', A: 'create:andesite_alloy', R: 'minecraft:redstone' }],
      engine_gearbox: [['ICI', 'CGC', 'ICI'], { I: 'minecraft:iron_ingot', C: 'create:cogwheel', G: 'create:large_cogwheel' }],
      engine_burner: [['III', 'IFI', 'BBB'], { I: 'minecraft:iron_ingot', F: 'minecraft:furnace', B: 'minecraft:bricks' }],
      // Storage: early too (Ascendite, chests and a core)
      storage_terminal: [['IGI', 'CEC', 'IRI'], { I: 'kubejs:ascendite_ingot', G: 'minecraft:glass_pane', C: 'minecraft:chest', E: 'kubejs:ascension_core', R: 'minecraft:comparator' }],
      // The Ascension Workbench (two blocks wide): where gear gets its tomes, gems, attachments and Legendary stars
      ascension_workbench: [['GTG', 'WCW', 'I I'], { G: 'kubejs:ascendite_ingot', T: 'minecraft:smithing_table', W: 'minecraft:crafting_table', C: 'kubejs:ascension_core', I: 'minecraft:iron_block' }],
      // The Mini Crusher: no crushing wheels, just iron, flint, a hopper and a casing
      mini_crusher: [['IHI', 'FCF', 'IBI'], { I: 'minecraft:iron_ingot', H: 'minecraft:hopper', F: 'minecraft:flint', C: 'create:andesite_casing', B: 'minecraft:iron_block' }],
      ascension_quarry: [['IDI', 'PCP', 'ISI'], { I: 'kubejs:ascendite_ingot', D: 'minecraft:diamond_pickaxe', P: 'minecraft:piston', C: 'kubejs:dense_ascension_core', S: 'create:shaft' }],
      mob_grinder: [['ISI', 'SCS', 'IHI'], { I: 'kubejs:ascendite_ingot', S: 'minecraft:iron_sword', C: 'kubejs:ascension_core', H: 'minecraft:hopper' }],
      warp_hopper: [['IEI', 'PHP', 'IEI'], { I: 'kubejs:ascendite_ingot', E: 'minecraft:ender_pearl', P: 'minecraft:amethyst_shard', H: 'minecraft:hopper' }],
      // The Realm Gate (Full download): the way into the Ascended Realm. Build it once; going through is free
      realm_gate: [['OMO', 'ESE', 'OMO'], { O: 'minecraft:crying_obsidian', M: 'kubejs:mythic_fragment', E: 'minecraft:ender_eye', S: 'kubejs:ascension_singularity' }],
      storage_linker: [['  A', ' N ', 'I  '], { A: 'minecraft:amethyst_shard', N: 'kubejs:ascendite_nugget', I: 'kubejs:ascendite_ingot' }]
    }
  },

  // ---- The Ascended Realm (Full download): floating islands of Realm Stone, reached through a Realm Gate ----
  // Right-click a Realm Gate: you arrive at the same x, z in the Realm, on a platform at platformY with a gate back.
  // That gate returns you to where you left. Ores there: Essence Crystal (Ascension Cores), Voidsteel (ingots) and
  // Astral (Mythic Fragments). Danger there starts at danger.realmBonus.
  realm: { platformY: 128, platformRadius: 3, cooldownTicks: 40 },

  // ---- Guns (Full download) ----
  // Shotgun: hold right-click to aim (zoom, tighter spread), left-click to fire, R to reload one shell at a time.
  // Pellets are your attack, so tomes on it and your gear count. Damage falls off past fullDamageRange to
  // `falloff` x at `range`. All pellets that hit one target land as one hit.
  guns: {
    shotgun: {
      pellets: 8, pelletDamage: 5, spreadHip: 7, spreadAim: 2.5, range: 40, fullDamageRange: 10, falloff: 0.35,
      cooldownTicks: 16, capacity: 6, reloadTicks: 10, kickback: 0.15, breaksGlass: true,
      recipe: [['  I', 'SBP', 'W  '], { I: 'minecraft:iron_ingot', S: 'kubejs:ascendite_ingot', B: 'create:fluid_pipe', P: 'minecraft:piston', W: 'minecraft:spruce_log' }]
    },
    // 8 shells: paper, gunpowder, iron nuggets and a gold nugget for the head
    shells: [[' P ', 'GNG', ' U '], { P: 'minecraft:paper', G: 'minecraft:gunpowder', N: 'minecraft:iron_nugget', U: 'minecraft:gold_nugget' }],
    shellsPerCraft: 8,
    // More guns (Full download): the controls are the shotgun's. Each one's base numbers are in the mod (gun/Guns.java);
    // these scale them: damage (x), rpmScale (fire rate x), magBonus (+ rounds), spreadScale, rangeScale, recoilScale.
    // Guns are built at the Weapons Workbench (its GUNSMITH tab) from `cost`: [item, count] pairs taken from your inventory.
    // Rounds: crafted plain (ammo below) or made at the Weapons Workbench from any items (type, propellant, case, colour).
    list: [
      { id: 'm1911', name: "M1911", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A hundred years old and still hits like a door.",
        cost: [['minecraft:iron_ingot', 6], ['kubejs:ascendite_ingot', 1], ['create:fluid_pipe', 1]] },
      { id: 'p17', name: "P17 Pistol", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "Polymer, boring, never jams. Mostly.",
        cost: [['minecraft:iron_ingot', 6], ['kubejs:ascendite_ingot', 1], ['create:fluid_pipe', 1]] },
      { id: 'hawk50', name: "Hawk .50", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A hand cannon. Mind your wrists.",
        cost: [['minecraft:iron_ingot', 6], ['kubejs:ascendite_ingot', 1], ['create:fluid_pipe', 1]] },
      { id: 'r357', name: "R357 Revolver", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "Six chances. Reloads one chamber at a time.",
        cost: [['minecraft:iron_ingot', 7], ['kubejs:ascendite_ingot', 1], ['create:fluid_pipe', 1]] },
      { id: 'mp5', name: "MP5 SMG", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "Smooth, accurate, and pleasant to hold. The classic.",
        cost: [['minecraft:iron_ingot', 10], ['kubejs:ascendite_ingot', 2], ['create:fluid_pipe', 1], ['minecraft:piston', 1]] },
      { id: 'mp7', name: "MP7 PDW", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "Tiny rounds, very fast, goes straight through armor.",
        cost: [['minecraft:iron_ingot', 10], ['kubejs:ascendite_ingot', 2], ['create:fluid_pipe', 1], ['minecraft:piston', 1]] },
      { id: 'uz9', name: "UZ-9 SMG", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A box that sprays. Aim is a suggestion.",
        cost: [['minecraft:iron_ingot', 10], ['kubejs:ascendite_ingot', 2], ['create:fluid_pipe', 1], ['minecraft:piston', 1]] },
      { id: 'kv45', name: "KV .45 SMG", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "The recoil goes down instead of up. Empties itself in a second.",
        cost: [['minecraft:iron_ingot', 10], ['kubejs:ascendite_ingot', 2], ['create:fluid_pipe', 1], ['minecraft:piston', 1]] },
      { id: 'ak74', name: "AK-74M", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "Light recoil, cheap rounds, works in mud.",
        cost: [['minecraft:iron_ingot', 14], ['kubejs:ascendite_ingot', 3], ['create:fluid_pipe', 2], ['minecraft:piston', 1], ['minecraft:spruce_log', 2]] },
      { id: 'akm', name: "AKM", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "The AK everyone knows. Hits hard, kicks harder.",
        cost: [['minecraft:iron_ingot', 14], ['kubejs:ascendite_ingot', 3], ['create:fluid_pipe', 2], ['minecraft:piston', 1], ['minecraft:spruce_log', 2]] },
      { id: 'm4a1', name: "M4A1", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "Modular, flat-shooting, everywhere.",
        cost: [['minecraft:iron_ingot', 14], ['kubejs:ascendite_ingot', 3], ['create:fluid_pipe', 2], ['minecraft:piston', 1]] },
      { id: 'mk17', name: "MK17 Battle Rifle", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A full-power rifle round on full auto. Brace yourself.",
        cost: [['minecraft:iron_ingot', 14], ['kubejs:ascendite_ingot', 3], ['create:fluid_pipe', 2], ['minecraft:piston', 1]] },
      { id: 'aug', name: "AUG A3", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A bullpup: the magazine sits behind the grip. Built-in optic.",
        cost: [['minecraft:iron_ingot', 14], ['kubejs:ascendite_ingot', 3], ['create:fluid_pipe', 2], ['minecraft:piston', 1], ['minecraft:spyglass', 1]] },
      { id: 'svd', name: "SVD Dragunov", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A long, thin, semi-automatic marksman rifle with a classic scope.",
        cost: [['minecraft:iron_ingot', 15], ['kubejs:ascendite_ingot', 3], ['create:fluid_pipe', 2], ['minecraft:piston', 1], ['kubejs:ascension_core', 1], ['minecraft:spyglass', 1], ['minecraft:spruce_log', 2]] },
      { id: 'sr25', name: "SR-25 DMR", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "An AR built for precision at range.",
        cost: [['minecraft:iron_ingot', 15], ['kubejs:ascendite_ingot', 3], ['create:fluid_pipe', 2], ['minecraft:piston', 1], ['kubejs:ascension_core', 1], ['minecraft:spyglass', 1]] },
      { id: 'm700', name: "M700 Bolt Rifle", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "Bolt action. Work the bolt after every shot. Loads round by round.",
        cost: [['minecraft:iron_ingot', 16], ['kubejs:ascendite_ingot', 4], ['create:fluid_pipe', 2], ['kubejs:ascension_core', 1], ['minecraft:spyglass', 1], ['minecraft:spruce_log', 2]] },
      { id: 'mosin', name: "Mosin-Nagant", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A century-old bolt rifle. Long, heavy, and it still works.",
        cost: [['minecraft:iron_ingot', 16], ['kubejs:ascendite_ingot', 4], ['create:fluid_pipe', 2], ['kubejs:ascension_core', 1], ['minecraft:spruce_log', 2]] },
      { id: 'ax338', name: "AX .338", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "The long-range sniper. One shot, one problem solved.",
        cost: [['minecraft:iron_ingot', 16], ['kubejs:ascendite_ingot', 4], ['create:fluid_pipe', 2], ['kubejs:ascension_core', 1], ['minecraft:spyglass', 1]] },
      { id: 'saiga12', name: "Saiga-12", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A semi-automatic shotgun with a box magazine.",
        cost: [['minecraft:iron_ingot', 12], ['kubejs:ascendite_ingot', 2], ['create:fluid_pipe', 2], ['minecraft:piston', 1]] },
      { id: 'double_barrel', name: "Double Barrel", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "Two barrels, both at once if you're quick.",
        cost: [['minecraft:iron_ingot', 12], ['kubejs:ascendite_ingot', 2], ['create:fluid_pipe', 2], ['minecraft:piston', 1], ['minecraft:spruce_log', 2]] },
      { id: 'pkm', name: "PKM", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A hundred-round belt of full-power rifle rounds.",
        cost: [['minecraft:iron_ingot', 22], ['kubejs:ascendite_ingot', 5], ['create:fluid_pipe', 3], ['minecraft:piston', 2], ['kubejs:ascension_core', 1]] },
      { id: 'm249', name: "M249 SAW", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A belt-fed 5.56 that never stops.",
        cost: [['minecraft:iron_ingot', 22], ['kubejs:ascendite_ingot', 5], ['create:fluid_pipe', 3], ['minecraft:piston', 2], ['kubejs:ascension_core', 1]] },
      { id: 'glock18', name: "G18 Auto Pistol", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A pistol that thinks it's an SMG. Empties in a second.",
        cost: [['minecraft:iron_ingot', 6], ['kubejs:ascendite_ingot', 1], ['create:fluid_pipe', 1]] },
      { id: 'm9', name: "M9", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A long-slide service pistol with an open top.",
        cost: [['minecraft:iron_ingot', 6], ['kubejs:ascendite_ingot', 1], ['create:fluid_pipe', 1]] },
      { id: 'usp45', name: "USP .45", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A heavy polymer .45. Takes a can well.",
        cost: [['minecraft:iron_ingot', 6], ['kubejs:ascendite_ingot', 1], ['create:fluid_pipe', 1]] },
      { id: 'five7', name: "Five-seveN", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "Twenty tiny, fast rounds that punch through armor.",
        cost: [['minecraft:iron_ingot', 6], ['kubejs:ascendite_ingot', 1], ['create:fluid_pipe', 1]] },
      { id: 'p226', name: "P226", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "An alloy-framed classic with a decocker.",
        cost: [['minecraft:iron_ingot', 6], ['kubejs:ascendite_ingot', 1], ['create:fluid_pipe', 1]] },
      { id: 'cz75', name: "CZ 75", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "All steel, sits low in the hand, shoots flat.",
        cost: [['minecraft:iron_ingot', 6], ['kubejs:ascendite_ingot', 1], ['create:fluid_pipe', 1], ['minecraft:spruce_log', 2]] },
      { id: 'makarov', name: "PM Makarov", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "Small, simple, everywhere.",
        cost: [['minecraft:iron_ingot', 6], ['kubejs:ascendite_ingot', 1], ['create:fluid_pipe', 1]] },
      { id: 'tt33', name: "TT-33", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A hot little bottleneck round from a thin old pistol.",
        cost: [['minecraft:iron_ingot', 6], ['kubejs:ascendite_ingot', 1], ['create:fluid_pipe', 1]] },
      { id: 'luger', name: "P08 Luger", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "The toggle lock folds up like a knee with every shot.",
        cost: [['minecraft:iron_ingot', 6], ['kubejs:ascendite_ingot', 1], ['create:fluid_pipe', 1], ['minecraft:spruce_log', 2]] },
      { id: 'c96', name: "C96 Broomhandle", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "The magazine's in front of the trigger. Loaded from stripper clips.",
        cost: [['minecraft:iron_ingot', 6], ['kubejs:ascendite_ingot', 1], ['create:fluid_pipe', 1], ['minecraft:spruce_log', 2]] },
      { id: 'python', name: "Python .357", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A vent-ribbed, full-lug .357. Smooth as glass.",
        cost: [['minecraft:iron_ingot', 7], ['kubejs:ascendite_ingot', 1], ['create:fluid_pipe', 1], ['minecraft:spruce_log', 2]] },
      { id: 'm29', name: "Model 29 .44", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "The most powerful handgun in the world, once.",
        cost: [['minecraft:iron_ingot', 7], ['kubejs:ascendite_ingot', 1], ['create:fluid_pipe', 1]] },
      { id: 'judge', name: "Judge", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A revolver that takes shotgun shells.",
        cost: [['minecraft:iron_ingot', 7], ['kubejs:ascendite_ingot', 1], ['create:fluid_pipe', 1]] },
      { id: 'vector', name: "Vector .45", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "The recoil is sent downward. Absurdly fast, absurdly flat.",
        cost: [['minecraft:iron_ingot', 10], ['kubejs:ascendite_ingot', 2], ['create:fluid_pipe', 1], ['minecraft:piston', 1]] },
      { id: 'p90', name: "P90", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "Fifty rounds lie across the top. Spits brass straight down.",
        cost: [['minecraft:iron_ingot', 10], ['kubejs:ascendite_ingot', 2], ['create:fluid_pipe', 1], ['minecraft:piston', 1]] },
      { id: 'ump45', name: "UMP-45", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A slow, heavy-hitting polymer SMG.",
        cost: [['minecraft:iron_ingot', 10], ['kubejs:ascendite_ingot', 2], ['create:fluid_pipe', 1], ['minecraft:piston', 1]] },
      { id: 'thompson', name: "Thompson", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "The Chicago typewriter. Fifty in the drum.",
        cost: [['minecraft:iron_ingot', 10], ['kubejs:ascendite_ingot', 2], ['create:fluid_pipe', 1], ['minecraft:piston', 1], ['minecraft:spruce_log', 2]] },
      { id: 'ppsh', name: "PPSh-41", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "Seventy-one rounds in the drum, and it fires them all fast.",
        cost: [['minecraft:iron_ingot', 10], ['kubejs:ascendite_ingot', 2], ['create:fluid_pipe', 1], ['minecraft:piston', 1], ['minecraft:spruce_log', 2]] },
      { id: 'mac10', name: "MAC-10", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A suppressed brick that empties in under two seconds.",
        cost: [['minecraft:iron_ingot', 10], ['kubejs:ascendite_ingot', 2], ['create:fluid_pipe', 1], ['minecraft:piston', 1]] },
      { id: 'bizon', name: "PP-19 Bizon", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A helical magazine under the barrel holds sixty-four.",
        cost: [['minecraft:iron_ingot', 10], ['kubejs:ascendite_ingot', 2], ['create:fluid_pipe', 1], ['minecraft:piston', 1]] },
      { id: 'mp40', name: "MP40", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "Slow, steady, controllable.",
        cost: [['minecraft:iron_ingot', 10], ['kubejs:ascendite_ingot', 2], ['create:fluid_pipe', 1], ['minecraft:piston', 1], ['minecraft:spruce_log', 2]] },
      { id: 'hk416', name: "HK416", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A piston AR. Runs dirty, runs hot, keeps running.",
        cost: [['minecraft:iron_ingot', 14], ['kubejs:ascendite_ingot', 3], ['create:fluid_pipe', 2], ['minecraft:piston', 1]] },
      { id: 'mcx', name: "MCX .300", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "Built around its suppressor. Heavy subsonic punch, quiet.",
        cost: [['minecraft:iron_ingot', 14], ['kubejs:ascendite_ingot', 3], ['create:fluid_pipe', 2], ['minecraft:piston', 1]] },
      { id: 'ak12', name: "AK-12", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "The modern AK: rails, a folding stock, the same mud-proof heart.",
        cost: [['minecraft:iron_ingot', 14], ['kubejs:ascendite_ingot', 3], ['create:fluid_pipe', 2], ['minecraft:piston', 1]] },
      { id: 'aks74u', name: "AKS-74U", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A rifle cut down to a carbine. Loud, short, and mean.",
        cost: [['minecraft:iron_ingot', 14], ['kubejs:ascendite_ingot', 3], ['create:fluid_pipe', 2], ['minecraft:piston', 1]] },
      { id: 'galil', name: "Galil ACE", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "An AK action dressed in rails and a red dot.",
        cost: [['minecraft:iron_ingot', 14], ['kubejs:ascendite_ingot', 3], ['create:fluid_pipe', 2], ['minecraft:piston', 1]] },
      { id: 'as_val', name: "AS VAL", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A rifle that's mostly suppressor. Heavy, slow, silent rounds.",
        cost: [['minecraft:iron_ingot', 14], ['kubejs:ascendite_ingot', 3], ['create:fluid_pipe', 2], ['minecraft:piston', 1]] },
      { id: 'scar_l', name: "SCAR-L", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "Light, modular, rails all round.",
        cost: [['minecraft:iron_ingot', 14], ['kubejs:ascendite_ingot', 3], ['create:fluid_pipe', 2], ['minecraft:piston', 1]] },
      { id: 'g36', name: "G36", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A carry handle with a built-in optic, see-through mags.",
        cost: [['minecraft:iron_ingot', 14], ['kubejs:ascendite_ingot', 3], ['create:fluid_pipe', 2], ['minecraft:piston', 1], ['minecraft:spyglass', 1]] },
      { id: 'famas', name: "FAMAS", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "The bugle: a fast bullpup under a long carry handle.",
        cost: [['minecraft:iron_ingot', 14], ['kubejs:ascendite_ingot', 3], ['create:fluid_pipe', 2], ['minecraft:piston', 1]] },
      { id: 'l85', name: "L85A2", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A bullpup with a 4x SUSAT scope. Accurate, finally.",
        cost: [['minecraft:iron_ingot', 14], ['kubejs:ascendite_ingot', 3], ['create:fluid_pipe', 2], ['minecraft:piston', 1], ['minecraft:spyglass', 1]] },
      { id: 'tavor', name: "Tavor X95", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A compact bullpup, rifle barrel in a carbine's length.",
        cost: [['minecraft:iron_ingot', 14], ['kubejs:ascendite_ingot', 3], ['create:fluid_pipe', 2], ['minecraft:piston', 1]] },
      { id: 'fal', name: "FN FAL", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "The right arm of the free world. Full-power, full auto.",
        cost: [['minecraft:iron_ingot', 14], ['kubejs:ascendite_ingot', 3], ['create:fluid_pipe', 2], ['minecraft:piston', 1]] },
      { id: 'm1_garand', name: "M1 Garand", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "Eight rounds in an en-bloc clip. Ping.",
        cost: [['minecraft:iron_ingot', 14], ['kubejs:ascendite_ingot', 3], ['create:fluid_pipe', 2], ['minecraft:piston', 1], ['minecraft:spruce_log', 2]] },
      { id: 'mk14', name: "Mk 14 EBR", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "An M14 in a chassis with a scope. Fast, heavy, precise.",
        cost: [['minecraft:iron_ingot', 15], ['kubejs:ascendite_ingot', 3], ['create:fluid_pipe', 2], ['minecraft:piston', 1], ['kubejs:ascension_core', 1], ['minecraft:spyglass', 1]] },
      { id: 'vss', name: "VSS Vintorez", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A silent scoped rifle that also goes full auto.",
        cost: [['minecraft:iron_ingot', 15], ['kubejs:ascendite_ingot', 3], ['create:fluid_pipe', 2], ['minecraft:piston', 1], ['kubejs:ascension_core', 1], ['minecraft:spyglass', 1], ['minecraft:spruce_log', 2]] },
      { id: 'awm', name: "AWM", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A green bolt-action magnum. Ends arguments at a kilometre.",
        cost: [['minecraft:iron_ingot', 16], ['kubejs:ascendite_ingot', 4], ['create:fluid_pipe', 2], ['kubejs:ascension_core', 1], ['minecraft:spyglass', 1]] },
      { id: 'm82', name: "M82 Barrett", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A semi-automatic .50 anti-materiel rifle. Shoots through things.",
        cost: [['minecraft:iron_ingot', 28], ['kubejs:ascendite_ingot', 6], ['create:fluid_pipe', 2], ['kubejs:ascension_core', 2], ['minecraft:spyglass', 1]] },
      { id: 'kar98k', name: "Kar98k", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A turned-down bolt, iron sights, and a long reach.",
        cost: [['minecraft:iron_ingot', 16], ['kubejs:ascendite_ingot', 4], ['create:fluid_pipe', 2], ['kubejs:ascension_core', 1], ['minecraft:spruce_log', 2]] },
      { id: 'winchester', name: "Winchester 1873", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "The gun that won the West. Work the lever, load through the gate.",
        cost: [['minecraft:iron_ingot', 14], ['kubejs:ascendite_ingot', 3], ['create:fluid_pipe', 2], ['minecraft:piston', 1], ['minecraft:spruce_log', 2]] },
      { id: 'm870', name: "M870", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "Pump action. Rack it after every shot.",
        cost: [['minecraft:iron_ingot', 12], ['kubejs:ascendite_ingot', 2], ['create:fluid_pipe', 2], ['minecraft:piston', 1], ['minecraft:spruce_log', 2]] },
      { id: 'm1014', name: "M1014", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A semi-automatic combat shotgun with ghost-ring sights.",
        cost: [['minecraft:iron_ingot', 12], ['kubejs:ascendite_ingot', 2], ['create:fluid_pipe', 2], ['minecraft:piston', 1]] },
      { id: 'spas12', name: "SPAS-12", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A pump with a heat shield and attitude.",
        cost: [['minecraft:iron_ingot', 12], ['kubejs:ascendite_ingot', 2], ['create:fluid_pipe', 2], ['minecraft:piston', 1]] },
      { id: 'aa12', name: "AA-12", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A full-auto shotgun with a twenty-shell drum. Barely kicks.",
        cost: [['minecraft:iron_ingot', 12], ['kubejs:ascendite_ingot', 2], ['create:fluid_pipe', 2], ['minecraft:piston', 1]] },
      { id: 'ksg', name: "KSG", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A bullpup pump with two tubes: fourteen shells.",
        cost: [['minecraft:iron_ingot', 12], ['kubejs:ascendite_ingot', 2], ['create:fluid_pipe', 2], ['minecraft:piston', 1]] },
      { id: 'sawnoff', name: "Sawn-Off", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "Two barrels, cut short. Devastating, up close.",
        cost: [['minecraft:iron_ingot', 12], ['kubejs:ascendite_ingot', 2], ['create:fluid_pipe', 2], ['minecraft:piston', 1], ['minecraft:spruce_log', 2]] },
      { id: 'rpk', name: "RPK", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A long, heavy AK with a 75-round drum and a bipod.",
        cost: [['minecraft:iron_ingot', 22], ['kubejs:ascendite_ingot', 5], ['create:fluid_pipe', 3], ['minecraft:piston', 2], ['kubejs:ascension_core', 1], ['minecraft:spruce_log', 2]] },
      { id: 'mg42', name: "MG 42", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "Hitler's buzzsaw: twelve hundred rounds a minute.",
        cost: [['minecraft:iron_ingot', 22], ['kubejs:ascendite_ingot', 5], ['create:fluid_pipe', 3], ['minecraft:piston', 2], ['kubejs:ascension_core', 1]] },
      { id: 'm60', name: "M60", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "The pig. A hundred-round belt.",
        cost: [['minecraft:iron_ingot', 22], ['kubejs:ascendite_ingot', 5], ['create:fluid_pipe', 3], ['minecraft:piston', 2], ['kubejs:ascension_core', 1]] },
      { id: 'dp28', name: "DP-28", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "A flat pan of forty-seven rounds on top, a bipod at the front.",
        cost: [['minecraft:iron_ingot', 22], ['kubejs:ascendite_ingot', 5], ['create:fluid_pipe', 3], ['minecraft:piston', 2], ['kubejs:ascension_core', 1], ['minecraft:spruce_log', 2]] },
      { id: 'minigun', name: "M134 Minigun", damage: 1, rpmScale: 1, magBonus: 0, spreadScale: 1, rangeScale: 1, recoilScale: 1, lore: "Six barrels. Spin it up, then nothing in front of you survives.",
        cost: [['minecraft:iron_ingot', 48], ['kubejs:ascendite_ingot', 12], ['create:fluid_pipe', 6], ['kubejs:ascension_core', 3]] }
    ],
    // Plain rounds: a nugget (the bullet) over gunpowder over the case's copper, with the caliber's marker item to the
    // right of the bullet (9x19mm has none). Makes `perCraft[caliber]`.
    ammo: {
      recipe: { N: 'minecraft:iron_nugget', G: 'minecraft:gunpowder', C: 'minecraft:copper_ingot' },
      markers: { p9: false, p45: 'minecraft:gold_nugget', p50: 'minecraft:gold_ingot', r357: 'minecraft:redstone', p46: 'minecraft:quartz',
        r545: 'minecraft:flint', r556: 'minecraft:iron_ingot', r762s: 'minecraft:coal', r762n: 'minecraft:charcoal', r762r: 'minecraft:clay_ball',
        r338: 'minecraft:diamond', g12: 'minecraft:paper', p57: 'minecraft:prismarine_crystals', p762t: 'minecraft:bone_meal',
        r44: 'minecraft:glowstone_dust', r9x39: 'minecraft:lapis_lazuli', r300: 'minecraft:amethyst_shard', b50: 'minecraft:netherite_scrap' },
      perCraft: { p9: 16, p45: 12, p50: 6, r357: 8, p46: 16, r545: 12, r556: 12, r762s: 10, r762n: 8, r762r: 8, r338: 4, g12: 8,
        p57: 16, p762t: 14, r44: 8, r9x39: 10, r300: 12, b50: 3 }
    },
    // Weapons Workbench: puts attachments on guns (1000+ of them, each `cost` x partItem, plus (grade - 1) x tierItem
    // from grade 2 up) and presses rounds from any items. The Jury-Rigged book lets a gun take any attachment, anywhere.
    workbench: {
      partItem: 'minecraft:iron_ingot', tierItem: 'kubejs:ascendite_ingot',
      recipe: [['SAS', 'IWI', 'I I'], { S: 'minecraft:smooth_stone_slab', A: 'kubejs:ascendite_ingot', I: 'minecraft:iron_ingot', W: 'minecraft:smithing_table' }],
      juryRiggedBook: [['TCT', 'SBS', 'TCT'], { T: 'minecraft:string', C: 'minecraft:copper_ingot', S: 'minecraft:slime_ball', B: 'minecraft:book' }],
      // Skins (the SKINS tab): animated finishes, some with 3D parts (snow, torn cloth, flames...). Each costs this.
      skinItem: 'minecraft:amethyst_shard', skinCost: 2
    },
    // Grenades, bandages and medkits (crafting grid). Hold right-click to cook a grenade, let go to throw; hold to use meds.
    fragGrenade: [[' I ', 'GTG', ' I '], { I: 'minecraft:iron_ingot', G: 'minecraft:gunpowder', T: 'minecraft:tnt' }],
    fragGrenadesPerCraft: 2,
    bandage: [['PSP'], { P: 'minecraft:paper', S: 'minecraft:string' }],
    bandagesPerCraft: 3,
    medkit: [['WBW', 'BHB', 'WBW'], { W: 'minecraft:white_wool', B: 'factoryascension:bandage', H: 'minecraft:glistering_melon_slice' }],
    // Reloading under stress: chance a reload step goes wrong = base + stress^1.5 x stress (stress 0..1)
    fumbles: { enabled: true, base: 0.02, stress: 0.55 },
    explosiveRoundsBreakBlocks: false,
    // Flare gun: the shotgun's controls, one flare at a time (R loads; R again swaps colour). A red flare calls for rescue;
    // a green one calls an airdrop: after airdropSeconds a supply crate falls from airdropHeight blocks up, 8 blocks
    // ahead of where you fired, under green smoke, and lands as a chest filled from airdropLoot.
    // A red flare calls for rescue: after rescueSeconds, with rescueChance, a helicopter comes, circles the flare, and takes
    // you home with everything you carry if you're within rescueRadius blocks of where you fired it. Or no one comes.
    flareGun: {
      cooldownTicks: 30, speed: 1.6, airdropSeconds: 20, airdropHeight: 60, airdropLoot: 'factoryascension:chests/airdrop',
      rescue: true, rescueChance: 0.7, rescueSeconds: 60, rescueRadius: 12,
      recipe: [['IIB', ' GR', '  P'], { I: 'minecraft:iron_ingot', B: 'minecraft:copper_ingot', G: 'kubejs:ascendite_ingot', R: 'minecraft:redstone', P: 'minecraft:orange_dye' }]
    },
    flares: {
      red: [[' P ', 'GRG', ' U '], { P: 'minecraft:paper', G: 'minecraft:gunpowder', R: 'minecraft:red_dye', U: 'minecraft:gold_nugget' }],
      green: [[' P ', 'GDG', ' C '], { P: 'minecraft:paper', G: 'minecraft:gunpowder', D: 'minecraft:green_dye', C: 'kubejs:ascension_core' }],
      perCraft: 2
    }
  },

  // ---- Armed operators (Full download): people out there with guns, who fight like players ----
  // They hear gunshots and come to look, call their squad onto you, take cover to reload and patch up, flank, throw
  // grenades and keep firing where you were. Patrols roam the danger zones (more and worse the deeper you go, and at
  // night); squads hold the structures out there (scav camps near home, bunkers and outposts further, warlords' forts far
  // out); a red flare sends hunters. Their hits can leave you Bleeding (bandage it). Kill one: its gun (with what's left in
  // it), spare rounds, kit and dog tag drop.
  npcs: {
    healthScale: 1, damageScale: 0.45, spreadScale: 1, bleedChance: 0.2, gunDropChance: 0.85,
    // Monsters (hordes too) hear gunshots and come: hearing x the shot's reach (about 100 blocks for a rifle, a few for a suppressed one)
    mobsHearShots: true, mobHearingScale: 0.8,
    patrols: true, patrolChance: 0.06, patrolMinDanger: 1, maxNearby: 14, flareHunters: true,
    kinds: {
      scav: 'A scavenger with whatever gun he found. Cheap, numerous, nervous.',
      scav_rifleman: 'A scav who found an AK and some confidence.',
      scav_sniper: 'Lies out on towers with an old bolt rifle. Patient.',
      looter: 'Sprays an SMG and runs right at you.',
      raider: 'A trained fighter in a plate carrier. Flanks, throws grenades, works with his squad.',
      raider_medic: 'Keeps the raiders alive: patches up anyone hurt near him.',
      breacher: 'A shotgun and a helmet visor. Comes through the door first.',
      grenadier: 'Throws grenades at whatever cover you\'re behind.',
      marksman: 'Accurate at long range with a scoped rifle.',
      machine_gunner: 'Keeps a belt-fed gun firing where you were last seen.',
      rogue: 'Ex-military. Heavy armor, night vision, very good.',
      rogue_sniper: 'Sees you before you see the glint.',
      cultist: 'Robed, masked, silent. Creeps up at night with a suppressed rifle.',
      ghost: 'Quiet, crouched, suppressed. You hear him when he\'s already beside you.',
      juggernaut: 'Armored head to toe with a machine gun. Slow, and nearly unstoppable.',
      hunter: 'Sent for whoever fired that red flare. Always seems to know where you are.',
      warlord: 'Rules a fort with his bodyguards. Tough, and carries a hand cannon.',
      bodyguard: 'Stays by the warlord, shotgun ready.',
      smuggler: 'Fast, lightly armed, runs when it goes bad.',
      deserter: 'Left the army with his rifle. Fights alone, fights well.'
    }
  },

  // ---- Tarkov inventory (Full download) ----
  // Your main inventory is a grid where items take their real size: a block 1x1, a sword 1x3, a chestplate 3x3, the
  // shotgun 5x2, a backpack 3x3. Pick an item up, press R to turn it, click a free spot to put it down. Items stay in
  // the game's own inventory slots (quests, charms, soulbound and other mods see them as usual); anything that can't
  // fit waits in a "No room" strip. With blockPickup, items you have no room for stay on the ground.
  // sizes: [item id, width, height] to change any item's size. Creative mode keeps the normal inventory.
  // weight: each cell weighs kgPerCell (a full stack of a 1x1 item = 1 cell; a lone block weighs 1/64 of that).
  // Past carryKg you slow down by slowPerKg (0.01 = 1% per kg), never more than maxSlow (0.5 = half speed).
  inventory: {
    enabled: true, width: 10, height: 6, blockPickup: true,
    sizes: [['minecraft:totem_of_undying', 1, 2], ['minecraft:spyglass', 1, 2], ['minecraft:anvil', 2, 2]],
    weight: { enabled: true, kgPerCell: 1, carryKg: 100, slowPerKg: 0.01, maxSlow: 0.5 }
  },

  // ---- Pets: Ascension Collars and the Pet Whistle ----
  // Right-click your own tamed pet (wolf, cat, parrot, horse, ...) with a collar: it gets the collar's health, attack
  // and armor, heals fully, stops taking fire and fall damage, and keeps it all. Each collar needs the one before.
  // The Pet Whistle calls your pets (not the sitting ones) within whistleRange blocks to you.
  pets: {
    collars: [
      { id: 'ascendite_collar', name: 'Ascendite Collar', health: 60, attack: 8, armor: 6, recipe: [[' L ', 'I I', ' C '], { L: 'minecraft:leather', I: 'kubejs:ascendite_ingot', C: 'kubejs:ascension_core' }] },
      { id: 'voidsteel_collar', name: 'Voidsteel Collar', health: 150, attack: 20, armor: 10, recipe: [[' L ', 'IPI', ' C '], { L: 'minecraft:leather', I: 'kubejs:voidsteel_ingot', P: 'kubejs:ascendite_collar', C: 'kubejs:dense_ascension_core' }] },
      { id: 'celestium_collar', name: 'Celestium Collar', health: 400, attack: 50, armor: 15, recipe: [[' L ', 'IPI', ' C '], { L: 'minecraft:leather', I: 'kubejs:celestium_ingot', P: 'kubejs:voidsteel_collar', C: 'kubejs:mythic_fragment' }] },
      { id: 'ascendium_collar', name: 'Ascendium Collar', health: 1000, attack: 150, armor: 20, recipe: [[' L ', 'IPI', ' C '], { L: 'minecraft:leather', I: 'kubejs:ascendium_ingot', P: 'kubejs:celestium_collar', C: 'kubejs:ascension_singularity' }] },
      { id: 'singularity_collar', name: 'Singularity Collar', health: 4000, attack: 500, armor: 30, recipe: [[' S ', 'SPS', ' A '], { S: 'kubejs:ascension_singularity', P: 'kubejs:ascendium_collar', A: 'kubejs:ascended_soul' }] }
    ],
    whistleRange: 256,
    whistle: [[' N ', 'IPI', ' I '], { N: 'minecraft:gold_nugget', I: 'minecraft:iron_ingot', P: 'minecraft:ender_pearl' }]
  },

  // ---- Charms: carry one anywhere in your inventory for its effect (the strongest of each kind counts) ----
  // effects: [effect, amplifier]. magnet: pulls items within that many blocks to you. Never adds hearts.
  charms: {
    list: [
      { id: 'haste_charm', name: 'Charm of Haste', effects: [['minecraft:haste', 1]], extra: 'minecraft:golden_pickaxe' },
      { id: 'swift_charm', name: 'Charm of Swiftness', effects: [['minecraft:speed', 1]], extra: 'minecraft:sugar' },
      { id: 'owl_charm', name: 'Owl Charm', effects: [['minecraft:night_vision', 0]], extra: 'minecraft:golden_carrot' },
      { id: 'ember_charm', name: 'Ember Charm', effects: [['minecraft:fire_resistance', 0]], extra: 'minecraft:magma_cream' },
      { id: 'tide_charm', name: 'Tide Charm', effects: [['minecraft:water_breathing', 0], ['minecraft:dolphins_grace', 0]], extra: 'minecraft:nautilus_shell' },
      { id: 'iron_charm', name: 'Iron Charm', effects: [['minecraft:resistance', 0]], extra: 'minecraft:iron_block' },
      { id: 'luck_charm', name: 'Lucky Charm', effects: [['minecraft:luck', 2]], extra: 'minecraft:rabbit_foot' },
      { id: 'leap_charm', name: 'Leaping Charm', effects: [['minecraft:jump_boost', 1]], extra: 'minecraft:slime_ball' },
      { id: 'magnet_charm', name: 'Magnet Charm', effects: [], magnet: 8, extra: 'minecraft:lodestone' },
      { id: 'ascended_charm', name: 'Ascended Charm', effects: [['minecraft:haste', 2], ['minecraft:speed', 2], ['minecraft:night_vision', 0], ['minecraft:fire_resistance', 0],
        ['minecraft:water_breathing', 0], ['minecraft:dolphins_grace', 0], ['minecraft:resistance', 1], ['minecraft:luck', 3], ['minecraft:jump_boost', 1]], magnet: 16, extra: 'kubejs:ascended_soul' }
    ],
    // Every charm: gold around its ingredient (`extra`) and a core. The Ascended Charm: every other charm around a Soul
    recipe: [['GNG', 'NXN', 'GCG'], { G: 'minecraft:gold_ingot', N: 'minecraft:gold_nugget', C: 'kubejs:ascension_core' }],
    everyTicks: 40
  },

  // ---- Factory Room TNT: blasts out a ready-made underground room ----
  // Right-click a block with one. After the fuse it hollows out a room in front of you (width x length x height inside),
  // lines the floor, walls and roof with the style's block, sets its lights into the roof every lightSpacing blocks and
  // leaves a doorway on your side, level with your feet. It never touches bedrock or anything with an inventory or a
  // machine inside (chests, Create machines, spawners), and ores it blasts through drop in the middle of the room.
  roomCharges: {
    enabled: true,
    fuseSeconds: 3,
    blocksPerTick: 6000,       // spread over a few ticks so the server never freezes
    lightSpacing: 6,
    doorway: [3, 3],           // width, height of the opening on your side
    collectOres: true,
    // size: [width, length, height inside]; craft: how many of the next size down make one
    sizes: [
      { id: '16', size: [16, 16, 8] },
      { id: '32', size: [32, 32, 12], craft: 4 },
      { id: '50', size: [50, 50, 16], craft: 3 }
    ],
    // The smallest size is TNT + 4 wall blocks + 2 lights; color / lightColor are only for the item icons
    styles: [
      { id: 'sandstone', name: 'Sandstone', shell: 'minecraft:smooth_sandstone', light: 'minecraft:glowstone', color: [226, 208, 152], lightColor: [255, 214, 120] },
      { id: 'stone_brick', name: 'Stone Brick', shell: 'minecraft:stone_bricks', light: 'minecraft:sea_lantern', color: [140, 140, 140], lightColor: [196, 236, 230] },
      { id: 'deepslate', name: 'Deepslate', shell: 'minecraft:deepslate_tiles', light: 'minecraft:ochre_froglight', color: [70, 70, 80], lightColor: [250, 220, 140] },
      { id: 'quartz', name: 'Quartz', shell: 'minecraft:smooth_quartz', light: 'minecraft:sea_lantern', color: [236, 230, 222], lightColor: [196, 236, 230] }
    ]
  },

  // ---- Skinwalkers: some animals out in the wilderness aren't animals ----
  // A passive animal is sometimes secretly a skinwalker: most often out in the danger zones, but even at home (safeChance). It looks and acts normal, until a player
  // comes near: then it follows them at a distance and stops to stare. After a while (sooner at night), or the moment
  // it's hit, it shapeshifts (twitching, cracking, rising) into a strong mob, and only then is it revealed:
  // always an Elite, sometimes a rare variant as well, with extra loot.
  skinwalkers: {
    enabled: true,
    chance: 0.05,              // per animal spawning at danger minDanger or deeper
    minDanger: 2,
    safeChance: 0.02,          // per animal spawning closer in, even in the safe zone: nowhere is safe
    mimicChancePerSecond: 0.04, // while it stalks you: footsteps behind you, a knock, a voice, someone hurt...
    animals: ['minecraft:cow', 'minecraft:pig', 'minecraft:sheep', 'minecraft:chicken', 'minecraft:horse', 'minecraft:donkey', 'minecraft:goat',
      'minecraft:llama', 'minecraft:rabbit', 'minecraft:fox', 'minecraft:mooshroom', 'factoryascension:copper_cow', 'factoryascension:ember_hen'],
    noticeRange: 48,           // it starts following a player this close
    stareRange: 12,            // ...and stops to stare from about this far
    minStalkSeconds: 20,       // it watches you at least this long before it can turn
    shiftChancePerSecond: 0.03,
    nightMult: 2.5,            // more likely at night
    // What it turns into: [mob, weight]. Mobs from mods that aren't installed are skipped.
    forms: [['minecraft:ravager', 3], ['minecraft:vindicator', 4], ['minecraft:evoker', 2], ['minecraft:zoglin', 4], ['minecraft:wither_skeleton', 3],
      ['minecraft:enderman', 2], ['factoryascension:void_stalker', 3], ['factoryascension:ascendite_husk', 3], ['factoryascension:frost_revenant', 2],
      ['factoryascension:factory_raider', 3]],
    dangerBonus: 5,            // the form is this much more dangerous than where it turned
    variantChance: 0.35,       // chance the form is also a rare variant (Blazing, Frozen, Storm, Shiny or Golden)
    lootRolls: 2,              // extra loot rolls when you kill it, one tier better
    fragmentChance: 0.5        // and maybe a Mythic Fragment
  },

  // ---- The Factory Ascension mod's own 20 mobs (Full download): how deep out each starts spawning ----
  // Natural spawns below this danger level are cancelled, so the strange ones live out in the danger zones.
  // (The Nether and End count their own danger bonus; no home set yet means danger 0 everywhere.)
  modMobs: {
    minDanger: {
      rust_walker: 1, ascendite_husk: 3, ember_ghoul: 0, frost_revenant: 1, deep_miner: 1, void_archer: 0, ascendite_skeleton: 3,
      crystal_spider: 1, spark_spider: 2, cinder_spider: 0, ascendite_creeper: 3, frost_creeper: 1, essence_slime: 5, rust_slime: 1,
      ore_mite: 1, teal_wisp: 0, void_stalker: 0, factory_raider: 4, ember_hen: 0, copper_cow: 0,
      bog_lurker: 1, sand_wraith: 1, thunder_creeper: 2, brass_sentinel: 4, shadow_skeleton: 3, cave_weaver: 1, moss_slime: 0, glow_mite: 1,
      storm_wisp: 6, treasure_raider: 3, rust_colossus: 8, broodmother: 8, hollow_king: 12, wisp_tyrant: 10,
      astral_wisp: 12, void_knight: 12, astral_sovereign: 12, void_behemoth: 12   // Realm only (its danger starts at realmBonus)
    },
    // Bosses: on top of minDanger, only `chance` of their natural spawns go through, and never within `spacing`
    // blocks of another of the same boss. Each has a boss bar, calls minions at 2/3 and 1/3 health and enrages at 1/4.
    bosses: {
      rust_colossus: { chance: 0.3, spacing: 300 },
      broodmother: { chance: 0.3, spacing: 300 },
      hollow_king: { chance: 0.25, spacing: 400 },
      wisp_tyrant: { chance: 0.3, spacing: 300 },
      astral_sovereign: { chance: 0.25, spacing: 400 },
      void_behemoth: { chance: 0.25, spacing: 400 }
    }
  },

  // ---- Gear match: enchants go on gear they suit (tomes, Chaos rolls and gems) ----
  // Every slot's levels add up, so a mismatched enchant still counts, but it's confusing: Sharpness on a helmet,
  // Protection on a pickaxe. With enabled: true a tome or gem only goes on gear it suits, and Chaos Tomes only roll
  // effects that suit the item. weapon: swords, axes, bows, tridents; armor; tool: pickaxes, shovels, hoes (axes too).
  gearMatch: {
    enabled: true,
    weapon: ['damage', 'critical', 'reaper', 'cleave', 'lifesteal', 'hit_effect', 'self_effect', 'ignite', 'hit_heal', 'plunder', 'kill_heal', 'xp', 'drop',
      'chain_lightning', 'gravity_well', 'death_nova', 'souls', 'overkill', 'rampage', 'heads', 'projectile_teleport', 'ultimate'],
    armor: ['resist', 'vitality', 'retribution', 'dodge', 'undying', 'regen', 'passive', 'phoenix', 'time_stop', 'aura', 'slam', 'flight'],
    tool: ['mining', 'vein', 'magnet', 'xp', 'drop'],
    any: ['repair', 'eternal', 'attribute', 'soulbound']    // these make sense on anything
  },

  // ---- Conflicts: when another installed mod already does a feature's job, the pack turns its own version off ----
  // and says so in chat: "The Scaling Health mod conflicts with this mod's Danger Zones feature: Turned Danger Zones off."
  // mods: { mod id: name }. off: what gets switched off. (Shiny Reflections, in the mod, checks for shader mods itself.)
  conflicts: [
    { feature: 'Danger Zones', off: 'danger', mods: { scalinghealth: 'Scaling Health', champions: 'Champions', improvedmobs: 'Improved Mobs' } },
    { feature: 'Rare Mob Variants', off: 'variants', mods: { champions: 'Champions', infernalmobs: 'Infernal Mobs' } },
    { feature: '/home', off: 'homeCommand', mods: { ftbessentials: 'FTB Essentials' } }
  ],

  // ---- Easy start: cheaper ways into Create's first machines (both downloads) ----
  easyStart: {
    enabled: true,
    casingsPerLog: 2,          // any log + Andesite Alloy in a crafting grid -> Andesite Casings (no deployer needed)
    beltFromLeather: true,     // 3 leather -> a Mechanical Belt (no kelp farm needed)
    cogwheelsPerAlloy: 4,      // 8 planks around an Andesite Alloy -> this many cogwheels
    shaftsPerAlloy: 8          // Andesite Alloy + a stick -> this many shafts
  },

  // ---- Home: /sethome saves where you stand, /home takes you back (any dimension) ----
  home: {
    enabled: true,
    command: true,             // /home and /sethome (always also /ascension home and /ascension sethome)
    combatSeconds: 10,         // no /home for this long after you hit, or get hit by, a mob or player
    cooldownSeconds: 30        // time between uses
  },

  // ---- More gear tiers: Voidsteel (between diamond and netherite), Celestium (past netherite), Ascendium (endgame) ----
  // Each has a full tool and armor set made from its ingot, like iron. For comparison: diamond tools 1561 uses /
  // +3 damage, armor 3/6/8/3 + 2 toughness; netherite 2031 / +4, 3/6/8/3 + 3 toughness + 0.1 knockback resistance.
  gearTiers: {
    voidsteel: { name: 'Voidsteel',
      tools: { uses: 1800, speed: 8.5, attackDamageBonus: 3.5, level: 3, enchantability: 12 },
      armor: { durabilityMultiplier: 35, protection: [3, 6, 8, 3], toughness: 2.5, knockbackResistance: 0.05, enchantability: 12 } },
    celestium: { name: 'Celestium',
      tools: { uses: 3200, speed: 10, attackDamageBonus: 5, level: 4, enchantability: 18 },
      armor: { durabilityMultiplier: 50, protection: [4, 7, 9, 4], toughness: 4, knockbackResistance: 0.15, enchantability: 18 } },
    ascendium: { name: 'Ascendium',
      tools: { uses: 6000, speed: 13, attackDamageBonus: 7, level: 4, enchantability: 25 },
      armor: { durabilityMultiplier: 80, protection: [5, 8, 10, 5], toughness: 6, knockbackResistance: 0.25, enchantability: 25 } }
  },

  // ---- Attachments: craftable add-ons for gear (a light on your helmet, a chainsaw on your sword...) ----
  // Hold an attachment with the gear in your other hand and right-click. Each piece of gear has `slots` slots;
  // /ascension shows them with a [Detach] button. fits: helmet, chest, legs, boots, weapon, bow or tool (pickaxe,
  // shovel, hoe). power: the number the effect uses. recipe: shapeless crafting ingredients.
  // charge: powered attachments use this much battery charge each time they fire, from a battery anywhere in your
  // inventory (the emptiest one first). With no charge left they don't fire (spikes and blades still cut).
  attachments: {
    enabled: true,
    slots: 2,
    list: [
      { id: 'headlamp', name: 'Headlamp', fits: 'helmet', power: 15, text: 'lights up the area around you (light {p})',
        recipe: ['minecraft:iron_ingot', 'minecraft:torch', 'minecraft:glass_pane', 'minecraft:copper_ingot'] },
      { id: 'rebreather', name: 'Rebreather', fits: 'helmet', power: 1, text: 'breathe underwater',
        recipe: ['minecraft:iron_ingot', 'minecraft:glass_pane', 'minecraft:kelp', 'minecraft:kelp'] },
      { id: 'thermal_visor', name: 'Thermal Visor', fits: 'helmet', power: 24, text: 'enemies within {p} blocks glow through walls',
        recipe: ['minecraft:glass_pane', 'minecraft:redstone', 'minecraft:spider_eye', 'kubejs:ascendite_nugget'] },
      { id: 'jetpack', name: 'Jetpack', fits: 'chest', power: 2, text: 'sneak in mid-air to fly up, glide down otherwise',
        recipe: ['kubejs:ascendite_ingot', 'kubejs:ascendite_ingot', 'minecraft:gunpowder', 'minecraft:gunpowder', 'minecraft:blaze_rod'] },
      { id: 'armor_plating', name: 'Armor Plating', fits: 'chest', power: 4, text: '+{p} armor',
        recipe: ['minecraft:iron_ingot', 'minecraft:iron_ingot', 'minecraft:iron_ingot', 'kubejs:ascendite_ingot', 'kubejs:ascendite_ingot'] },
      { id: 'spike_plating', name: 'Spike Plating', fits: 'chest', power: 25, text: 'attackers take {p}% of their hit back',
        recipe: ['minecraft:iron_ingot', 'minecraft:iron_ingot', 'minecraft:cactus', 'minecraft:cactus'] },
      { id: 'knee_pads', name: 'Knee Pads', fits: 'legs', power: 50, text: '-{p}% fall damage',
        recipe: ['minecraft:leather', 'minecraft:leather', 'minecraft:white_wool', 'minecraft:iron_ingot'] },
      { id: 'hydraulics', name: 'Hydraulics', fits: 'legs', power: 1, text: 'walk straight up full blocks',
        recipe: ['minecraft:piston', 'minecraft:piston', 'kubejs:ascendite_ingot'] },
      { id: 'rocket_boots', name: 'Rocket Boots', fits: 'boots', power: 25, text: '+{p}% movement speed',
        recipe: ['kubejs:ascendite_ingot', 'kubejs:ascendite_ingot', 'minecraft:gunpowder', 'minecraft:firework_rocket'] },
      { id: 'spring_coils', name: 'Spring Coils', fits: 'boots', power: 2, text: 'Jump Boost {p}',
        recipe: ['minecraft:iron_ingot', 'minecraft:iron_ingot', 'minecraft:slime_ball', 'minecraft:slime_ball'] },
      { id: 'chainsaw', name: 'Chainsaw', fits: 'weapon', power: 30, text: '+{p}% damage, and it rips through enemies next to the target',
        recipe: ['minecraft:iron_ingot', 'minecraft:iron_ingot', 'kubejs:ascendite_ingot', 'minecraft:chain', 'create:cogwheel'] },
      { id: 'taser', name: 'Taser', fits: 'weapon', power: 15, text: '{p}% chance to stun the target for a second',
        recipe: ['minecraft:copper_ingot', 'minecraft:copper_ingot', 'minecraft:redstone', 'minecraft:redstone', 'kubejs:ascendite_nugget'] },
      { id: 'flamethrower', name: 'Flamethrower Nozzle', fits: 'weapon', power: 5, text: 'sets targets on fire for {p}s',
        recipe: ['minecraft:flint_and_steel', 'minecraft:copper_ingot', 'minecraft:blaze_rod'] },
      { id: 'serrated_edge', name: 'Serrated Edge', fits: 'weapon', power: 3, text: 'targets bleed (Wither) for {p}s',
        recipe: ['minecraft:flint', 'minecraft:flint', 'minecraft:flint', 'minecraft:iron_ingot'] },
      { id: 'scope', name: 'Scope', fits: 'bow', power: 25, text: '+{p}% arrow damage',
        recipe: ['minecraft:spyglass', 'minecraft:glass_pane', 'minecraft:iron_ingot'] },
      { id: 'drill_bit', name: 'Drill Bit', fits: 'tool', power: 2, text: 'Haste {p} while you hold it',
        recipe: ['minecraft:iron_ingot', 'minecraft:iron_ingot', 'minecraft:iron_ingot', 'minecraft:diamond', 'create:cogwheel'] },
      { id: 'mining_lamp', name: 'Mining Lamp', fits: 'tool', power: 15, text: 'lights up the area around you while held (light {p})',
        recipe: ['minecraft:torch', 'minecraft:torch', 'minecraft:iron_ingot', 'minecraft:glass_pane'] },
      // Sword attachments (any weapon). Powered ones need battery charge.
      { id: 'electric_spike_wrap', name: 'Electric Spike Wrap', fits: 'weapon', power: 6, charge: 1, spikes: 2, arcs: 2,
        text: '+{s} spike damage; powered, every hit shocks for +{p} and arcs to {a} enemies nearby',
        recipe: ['minecraft:iron_ingot', 'minecraft:iron_ingot', 'minecraft:copper_ingot', 'minecraft:copper_ingot', 'minecraft:redstone', 'minecraft:lightning_rod'] },
      { id: 'plasma_edge', name: 'Plasma Edge', fits: 'weapon', power: 40, charge: 1,
        text: 'powered, +{p}% damage and sets targets on fire',
        recipe: ['kubejs:ascendite_ingot', 'kubejs:ascendite_ingot', 'minecraft:blaze_powder', 'minecraft:redstone', 'minecraft:glowstone_dust'] },
      { id: 'overcharger', name: 'Overcharger', fits: 'weapon', power: 5, charge: 5,
        text: 'powered, every {p}th hit releases a shock nova that hits everything within 4 blocks',
        recipe: ['kubejs:ascendite_ingot', 'minecraft:redstone_block', 'minecraft:lightning_rod', 'kubejs:ascension_core'] },
      { id: 'tesla_guard', name: 'Tesla Guard', fits: 'weapon', power: 8, charge: 2,
        text: 'powered, anything that hits you while you hold it is zapped for {p} and knocked back',
        recipe: ['minecraft:lightning_rod', 'minecraft:copper_ingot', 'minecraft:copper_ingot', 'minecraft:redstone_block'] },
      { id: 'vampire_fangs', name: 'Vampire Fangs', fits: 'weapon', power: 10,
        text: 'heal {p}% of the damage you deal',
        recipe: ['minecraft:bone', 'minecraft:bone', 'minecraft:fermented_spider_eye', 'minecraft:iron_ingot'] },
      { id: 'weighted_pommel', name: 'Weighted Pommel', fits: 'weapon', power: 2,
        text: 'knocks targets back much harder (+{p} knockback)',
        recipe: ['minecraft:iron_block', 'minecraft:chain'] },
      { id: 'targeting_lens', name: 'Targeting Lens', fits: 'weapon', power: 20,
        text: '{p}% chance a hit is a critical strike for x1.5 damage',
        recipe: ['minecraft:spyglass', 'minecraft:redstone', 'kubejs:ascendite_nugget'] }
    ],
    // Batteries: carry one to power attachments. charge: how much it holds (its durability bar shows what's left).
    // Recharge an empty one in a crafting grid or a Mechanical Mixer (no heat) with its `recharge` items; two part-used
    // batteries of the same kind also combine in a crafting grid like tools do.
    batteries: [
      { id: 'copper_battery', name: 'Copper Battery', charge: 200,
        recipe: ['minecraft:copper_ingot', 'minecraft:copper_ingot', 'minecraft:redstone', 'minecraft:redstone', 'minecraft:iron_nugget'],
        recharge: ['minecraft:redstone'] },
      { id: 'ascendite_battery', name: 'Ascendite Battery', charge: 1000,
        recipe: ['kubejs:ascendite_ingot', 'kubejs:ascendite_ingot', 'minecraft:redstone_block', 'minecraft:copper_ingot'],
        recharge: ['minecraft:redstone_block'] },
      { id: 'essence_cell', name: 'Essence Cell', charge: 6000,
        recipe: ['kubejs:dense_ascension_core', 'kubejs:ascendite_ingot', 'kubejs:ascendite_ingot', 'minecraft:glass', 'minecraft:redstone_block'],
        recharge: ['kubejs:ascension_core'] }
    ]
  },

  // ---- Selling: /ascension sell turns the item in your hand into Ascension Cores or Liquid Essence ----
  // Values are in Essence (1 Essence = 1 Ascension Core; a Liquid Essence bucket = 10).
  sell: {
    enabled: true,
    items: {
      'kubejs:mythic_fragment': 8, 'kubejs:ascended_soul': 60, 'kubejs:chaos_tome': 12, 'kubejs:blank_ascension_tome': 1.5,
      'kubejs:enchantment_pack': 6, 'kubejs:booster_box': 80, 'kubejs:gear_cache': 8, 'kubejs:polishing_cloth': 0.5,
      'kubejs:ascension_shard': 0.25, 'kubejs:raw_ascendite': 0.1, 'kubejs:ascendite_ingot': 0.15, 'kubejs:ascendite_block': 1.35,
      'kubejs:voidsteel_ingot': 2, 'kubejs:celestium_ingot': 10, 'kubejs:ascendium_ingot': 60,
      'minecraft:diamond': 1, 'minecraft:emerald': 0.5, 'minecraft:netherite_ingot': 8, 'minecraft:nether_star': 40
    },
    gems: [3, 15, 75],                 // Chipped / Flawless / Perfect
    foodCooked: 0.2,                   // any cooked mob food
    tomes: { base: 2, fusion: 6, mythic: 25, ultimate: 80 },
    holoBonus: 1.5,                    // x value for a holo card from a pack
    levelRefund: 0.5,                  // tomes and gear: plus this share of the Essence their enchant levels cost
    artifact: 500                      // extra for an Artifact
  },

  // ---- Glowing ores: light level 0-15 each ore gives off, so veins show up in dark caves ----
  // Ores from mods that aren't installed are skipped. Set enabled: false for vanilla lighting.
  glowingOres: {
    enabled: true,
    light: {
      'kubejs:ascendite_ore': 7, 'kubejs:deepslate_ascendite_ore': 7,
      'minecraft:diamond_ore': 6, 'minecraft:deepslate_diamond_ore': 6, 'minecraft:emerald_ore': 6, 'minecraft:deepslate_emerald_ore': 6,
      'minecraft:ancient_debris': 5,
      'minecraft:gold_ore': 4, 'minecraft:deepslate_gold_ore': 4, 'minecraft:nether_gold_ore': 4,
      'minecraft:lapis_ore': 4, 'minecraft:deepslate_lapis_ore': 4, 'minecraft:redstone_ore': 4, 'minecraft:deepslate_redstone_ore': 4,
      'minecraft:iron_ore': 3, 'minecraft:deepslate_iron_ore': 3, 'minecraft:copper_ore': 3, 'minecraft:deepslate_copper_ore': 3,
      'minecraft:nether_quartz_ore': 3, 'create:zinc_ore': 3, 'create:deepslate_zinc_ore': 3,
      'minecraft:coal_ore': 2, 'minecraft:deepslate_coal_ore': 2,
      'mekanism:osmium_ore': 3, 'mekanism:deepslate_osmium_ore': 3, 'mekanism:tin_ore': 3, 'mekanism:deepslate_tin_ore': 3,
      'mekanism:lead_ore': 3, 'mekanism:deepslate_lead_ore': 3, 'mekanism:uranium_ore': 4, 'mekanism:deepslate_uranium_ore': 4,
      'mekanism:fluorite_ore': 3, 'mekanism:deepslate_fluorite_ore': 3
    }
  },

  // ---- Rock Hammers: smash rock into gravel, nuggets and the odd ore ----
  // Hammer in your main hand + rocks in your other hand, right-click: smash one (sneak: sneakCount).
  // Or right-click a placed rock block to smash it where it is (a Create Deployer holding a hammer does this too).
  hammer: {
    sneakCount: 8,
    // uses: durability; luck: multiplies every chance; cooldown: ticks between smashes
    tools: {
      'kubejs:rock_hammer': { uses: 250, luck: 1, cooldown: 6 },
      'kubejs:ascendite_rock_hammer': { uses: 1200, luck: 1.6, cooldown: 3 }
    },
    // rock -> [[item, chance, max count]]; each line rolls on its own. Items from mods that aren't installed are skipped.
    rocks: {
      'minecraft:cobblestone': [['minecraft:gravel', 0.5, 1], ['minecraft:flint', 0.1, 1], ['minecraft:iron_nugget', 0.3, 3], ['create:copper_nugget', 0.25, 3],
        ['create:zinc_nugget', 0.15, 2], ['minecraft:gold_nugget', 0.08, 2], ['minecraft:coal', 0.1, 1], ['kubejs:raw_ascendite', 0.03, 1]],
      'minecraft:cobbled_deepslate': [['minecraft:gravel', 0.4, 1], ['minecraft:iron_nugget', 0.35, 4], ['minecraft:gold_nugget', 0.15, 3], ['minecraft:redstone', 0.2, 2],
        ['minecraft:lapis_lazuli', 0.12, 2], ['kubejs:raw_ascendite', 0.06, 1], ['minecraft:diamond', 0.005, 1]],
      'minecraft:gravel': [['minecraft:sand', 0.5, 1], ['minecraft:flint', 0.3, 1], ['minecraft:iron_nugget', 0.12, 2], ['minecraft:gold_nugget', 0.05, 1]],
      'minecraft:netherrack': [['minecraft:quartz', 0.2, 1], ['minecraft:gold_nugget', 0.2, 3], ['minecraft:glowstone_dust', 0.08, 2]],
      'minecraft:blackstone': [['minecraft:gold_nugget', 0.3, 4], ['minecraft:quartz', 0.1, 1], ['minecraft:netherite_scrap', 0.003, 1]],
      'minecraft:end_stone': [['minecraft:ender_pearl', 0.03, 1], ['minecraft:chorus_fruit', 0.05, 1]]
    }
  },

  // ---- Condition: every piece of gear rolls a wear grade, like weapon skins ----
  // Rolled once: when it's crafted, or the first time it's held or worn (so loot, trades and Create-crafted gear get one).
  // damage: x damage dealt with it in your main hand; defense: x damage taken, per armor piece wearing it;
  // wear: how much of its durability is already used when it's rolled. A Polishing Cloth rerolls it, keeping the better grade.
  condition: {
    enabled: true,
    grades: [
      { name: 'Battle-Scarred', weight: 10, damage: 0.85, defense: 1.04, wear: 0.3, color: 'dark_gray' },
      { name: 'Well-Worn', weight: 20, damage: 0.93, defense: 1.02, wear: 0.15, color: 'gray' },
      { name: 'Field-Tested', weight: 35, damage: 1.0, defense: 1.0, wear: 0.05, color: 'white' },
      { name: 'Minimal Wear', weight: 24, damage: 1.06, defense: 0.985, wear: 0, color: 'green' },
      { name: 'Mint Condition', weight: 10, damage: 1.12, defense: 0.97, wear: 0, color: 'aqua' },
      { name: 'Pristine', weight: 1, damage: 1.25, defense: 0.94, wear: 0, color: 'gold' }
    ]
  },

  // ---- Rare mob variants: the rarer, the better ----
  // Rolled once per mob spawn, rarest first. Elementals are hostile mobs only; Shiny and Golden can be any mob.
  // chance: per spawn; xp: x experience; lootRolls/lootBump: loot rolls like danger-zone loot, bumped up that many tiers;
  // drops: [item, min, max] or ['gem', tier] (a random gem of that tier).
  variants: {
    enabled: true,
    types: [
      { id: 'golden', name: 'Golden', symbol: '\u2605', chance: 0.00025, color: 'gold', particle: 'minecraft:wax_on', glow: true, announceSpawn: true,
        xp: 10, lootRolls: 3, lootBump: 2, drops: [['minecraft:gold_block', 1, 2], ['kubejs:ascension_core', 2, 5], ['gem', 2]] },
      { id: 'shiny', name: 'Shiny', symbol: '\u2726', chance: 0.001, color: 'aqua', particle: 'minecraft:end_rod', announceSpawn: true,
        xp: 5, lootRolls: 2, lootBump: 1, drops: [['gem', 1]] },
      { id: 'blazing', name: 'Blazing', chance: 0.001, elemental: true, onHit: 'fire', color: 'red', particle: 'minecraft:flame',
        xp: 2, lootRolls: 1, lootBump: 0, drops: [['minecraft:blaze_powder', 1, 3], ['minecraft:magma_cream', 0, 1]] },
      { id: 'frozen', name: 'Frozen', chance: 0.001, elemental: true, onHit: 'frost', color: 'blue', particle: 'minecraft:snowflake',
        xp: 2, lootRolls: 1, lootBump: 0, drops: [['minecraft:packed_ice', 1, 2], ['minecraft:snowball', 2, 4], ['minecraft:prismarine_crystals', 0, 2]] },
      { id: 'storm', name: 'Storm', chance: 0.001, elemental: true, onHit: 'storm', color: 'yellow', particle: 'minecraft:electric_spark',
        xp: 2, lootRolls: 1, lootBump: 0, drops: [['minecraft:glowstone_dust', 2, 4], ['minecraft:redstone', 2, 4]] }
    ],
    fireSeconds: 4,            // Blazing: sets you on fire
    frostSeconds: 3,           // Frozen: Slowness II
    stormBonus: 0.2            // Storm: a lightning strike, +20% damage on that hit
  },

  // ---- Safety limits (not power caps) ----
  maxExtraDropStacks: 8,       // most extra item stacks one kill can spawn, so loot enchants can't lag the server
  maxCleaveTargets: 16,
  maxUpgradesPerCommand: 100000
}

// Runtime state shared between scripts lives in this object. Nothing reachable from `global` may ever hold null:
// in KubeJS `global` is a Java map, and reading a key that holds null crashes the game (it did, in the server tick).
// Objects stored on it may be read the same way, so "no value" is written as false instead.
// Mob parts from the Factory Ascension mod's own mobs only exist when the mod does (the website keeps them all)
if (typeof Platform != 'undefined' && !Platform.isLoaded('factoryascension')) global.FA.mobFood.foods = global.FA.mobFood.foods.filter(f => !f.mod)

global.FA_STATE = { event: false, center: false, homes: {} }   // homes: uuid -> {x, z, end} (Overworld coordinates)

// Fusion enchants become ordinary enchant entries (type 'fusion'), so tomes, upgrades, the altar, tooltips,
// JEI and the website all handle them like any other enchant
global.FA.fusions.forEach(f => {
  if (!global.FA.enchants[f[0]]) global.FA.enchants[f[0]] = { name: f[1], type: 'fusion', parts: [f[2], f[3]], costMult: global.FA.fusionCostMultiplier }
})
// Triads are fusions of three, a little weaker per part
global.FA.triads.forEach(t => {
  if (!global.FA.enchants[t[0]]) global.FA.enchants[t[0]] = { name: t[1], type: 'fusion', triad: true, parts: [t[2], t[3], t[4]],
    strength: global.FA.triadStrength, costMult: global.FA.triadCostMultiplier }
})

// A parent enchant at a fusion's strength: same effect, scaled down
global.FA_scalePart = (def, strength) => {
  let p = {}
  Object.keys(def).forEach(k => { p[k] = def[k] })
  if (typeof p.perLevel == 'number') p.perLevel = p.perLevel * strength
  if (typeof p.critMultiplier == 'number') p.critMultiplier = Math.round((1 + (p.critMultiplier - 1) * strength) * 100) / 100
  if (p.type == 'dodge') p.cap = p.cap * strength
  if (typeof p.ampEvery == 'number') p.ampEvery = Math.max(1, Math.ceil(p.ampEvery / strength))
  return p
}
let faPartCache = {}
// The i-th effect of a fusion enchant (cached: they never change while the game runs)
global.FA_fusionPart = (id, i) => {
  let key = id + '~' + i
  let fe = global.FA.enchants[id]
  if (!faPartCache[key]) faPartCache[key] = global.FA_scalePart(global.FA.enchants[fe.parts[i]], fe.strength || global.FA.fusionStrength)
  return faPartCache[key]
}

// Readable numbers: 1234 -> "1,234", 12345678 -> "12.3M", 1e40 -> "1.00e+40"
global.FA_num = n => {
  if (!isFinite(n)) return 'Infinity'
  // Small fractions keep their decimals (0.25, 1.5) instead of rounding down to 0
  if (n < 100 && n % 1 != 0) return String(Math.round(n * (n < 10 ? 100 : 10)) / (n < 10 ? 100 : 10))
  if (n < 1e6) {
    let whole = String(Math.floor(n))
    return whole.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  }
  let units = ['K', 'M', 'B', 'T', 'Qa', 'Qi', 'Sx', 'Sp', 'Oc', 'No', 'Dc']
  let i = -1
  while (n >= 1000 && i < units.length - 1) {
    n /= 1000
    i++
  }
  if (n >= 1000) return (n * Math.pow(1000, i + 1)).toExponential(2)
  return n.toFixed(n < 10 ? 2 : n < 100 ? 1 : 0) + units[i]
}

// Shared helpers for effect strength, used by both the effects and the descriptions
global.FA_chance = level => level / (level + 100)
global.FA_amp = (e, level) => Math.min(e.maxAmp !== undefined ? e.maxAmp : 127, Math.floor((level - 1) / (e.ampEvery || 1)))
global.FA_undyingCooldown = (e, level) => Math.max(e.minCooldown, e.cooldown / (1 + e.perLevel * (level - 1)))

let FA_WHEN = {
  always: '', undead: ' vs undead', arthropod: ' vs arthropods', boss: ' vs bosses', nether: ' in the Nether',
  raider: ' vs raiders', wet: ' vs wet targets', airborne: ' vs airborne targets', full_health: ' vs full-health targets',
  low_health: ' vs targets under 30% health', berserk: ' while under 50% health', projectile: ' with projectiles',
  sneaking: ' while sneaking'
}

// Short effect text for an enchant at a level, shared by tooltips and the /ascension menu
global.FA_describe = (id, level) => global.FA_describeDef(global.FA.enchants[id], level)
global.FA_describeDef = (e, level) => {
  let num = global.FA_num
  let x = n => 'x' + (n < 100 ? n.toFixed(2) : num(n))
  let pct = n => (n < 10 ? Math.round(n * 1000) / 10 : num(Math.round(n * 100))) + '%'
  let amp = a => ' ' + (a + 1)
  switch (e.type) {
    case 'damage': return x(1 + e.perLevel * level) + ' damage' + FA_WHEN[e.when]
    case 'resist': return '-' + (100 - 100 / (1 + e.perLevel * level)).toFixed(1) + '%' + (e.vs == 'all' ? '' : ' ' + e.vs) + ' damage taken'
    case 'critical': return pct(global.FA_chance(level)) + ' chance for x' + e.critMultiplier + ' damage'
    case 'reaper': return 'kills non-bosses under ' + pct(Math.min(e.cap, e.perLevel * level)) + ' health'
    case 'cleave': return pct(e.perLevel * level) + ' of each hit to nearby enemies'
    case 'lifesteal': return 'heal ' + pct(e.perLevel * level) + ' of damage dealt'
    case 'hit_effect': return e.label + amp(global.FA_amp(e, level)) + ' on hit (' + e.seconds + 's)'
    case 'self_effect': return e.label + amp(global.FA_amp(e, level)) + ' when you hit (' + e.seconds + 's)'
    case 'ignite': return 'sets targets on fire for ' + num(e.perLevel * level) + 's'
    case 'hit_heal': return 'heal ' + num(e.perLevel * level) + ' health per hit'
    case 'plunder': return x(1 + e.perLevel * level) + ' mob loot'
    case 'kill_heal': return 'heal ' + num(e.perLevel * level) + ' health per kill'
    case 'xp': return x(1 + e.perLevel * level) + ' mob XP'
    case 'drop': return '+' + (e.perLevel * level < 10 ? (e.perLevel * level).toFixed(2) : num(e.perLevel * level)) + ' ' + e.label + ' per kill'
    case 'vitality': return '+' + num(e.perLevel * level) + ' hidden health'
    case 'retribution': return 'reflect ' + pct(e.perLevel * level) + ' of damage taken'
    case 'dodge': return pct(Math.min(e.cap, global.FA_chance(level))) + ' dodge chance'
    case 'undying': return 'survive a killing blow every ' + Math.round(global.FA_undyingCooldown(e, level)) + 's'
    case 'regen': return '+' + num(e.perLevel * level) + ' health per second'
    case 'passive': return e.maxLevel == 1 ? e.label : e.label + amp(level - 1)
    case 'magnet': return 'pulls items within ' + Math.min(e.cap, e.base + e.perLevel * level) + ' blocks'
    case 'repair': return 'repairs ' + num(e.perLevel * level) + ' durability/s on all gear'
    case 'soulbound': return 'stays with you: kept when you die, can\'t be dropped, never burns or despawns'
    case 'attribute': {
      let v = e.cap !== undefined ? Math.min(e.cap, e.perLevel * level) : e.perLevel * level
      return '+' + (e.percent ? pct(v) : v % 1 ? v.toFixed(1) : num(v)) + ' ' + e.label
    }
    case 'mining': return x(1 + e.perLevel * level) + ' mining speed'
    case 'eternal': return 'never breaks'
    case 'flight': return 'fly like in creative' + (level > 1 ? ', x' + (1 + 0.5 * (level - 1)) + ' flight speed' : '')
    case 'chain_lightning': return pct(global.FA_chainChance(level)) + ' chance: lightning chains to ' + global.FA_chainTargets(level) +
      (global.FA_chainTargets(level) == 1 ? ' enemy' : ' enemies') + ' for ' + pct(0.5 + e.perLevel * level) + ' of the hit'
    case 'gravity_well': return 'pulls enemies within ' + Math.min(e.cap, e.base + e.perLevel * level) + ' blocks into your target'
    case 'death_nova': return 'kills explode for ' + pct(0.5 + e.perLevel * level) + " of the victim's max health"
    case 'time_stop': {
      let c = global.FA_chronos(level)
      return 'when hit: freezes enemies within ' + c.radius + ' blocks for ' + c.seconds + 's (every ' + c.cooldown + 's)'
    }
    case 'phoenix': return 'on death: revive at full health in a firestorm (every ' + Math.round(global.FA_phoenixCooldown(e, level)) + 's)'
    case 'projectile_teleport': return 'your arrows teleport you to what they hit'
    case 'souls': return '+' + pct(e.perLevel * level) + ' damage per soul (kills grow the weapon forever)'
    case 'overkill': return pct(Math.min(1, 0.5 + e.perLevel * level)) + ' of excess damage jumps to the next enemy'
    case 'rampage': return '+' + pct(e.perLevel * level) + ' damage per kill in a streak (up to ' + e.maxStacks + ')'
    case 'vein': return 'mines up to ' + num(Math.min(e.cap, e.perLevel * level)) + ' connected ' + e.blocks + ' at once (sneak for one)'
    case 'aura': return 'burns enemies within ' + global.FA_auraRadius(level) + ' blocks for ' + num(e.perLevel * level) + ' damage/s'
    case 'heads': return pct(Math.min(1, e.perLevel * level)) + ' chance to drop mob heads'
    case 'fusion': {
      // The scaled-down parents are cached on the fusion (they never change while the game runs)
      if (!e._scaled) e._scaled = e.parts.map(pid => global.FA_scalePart(global.FA.enchants[pid], e.strength || global.FA.fusionStrength))
      return e._scaled.map(p => global.FA_describeDef(p, level)).join(' + ')
    }
    case 'ultimate': return 'sneak + hit: ' + e.desc.charAt(0).toLowerCase() + e.desc.slice(1) + ' (' + global.FA_ultPower(e, level).toFixed(2) +
      'x hit damage per strike, every ' + global.FA_ultCooldown(e, level) + 's)'
    case 'slam': return 'landing smashes nearby enemies for ' + num(e.perLevel * level) + ' damage per block fallen, no fall damage'
  }
  return ''
}

// ---- Danger helpers ----

// Danger level at a position: distance from the center (a player's home, or spawn) past safeRadius, a level every
// blocksPerLevel. Nether distances count x netherScale; the End measures from its own 0, 0.

// The player nearest to (x, z) in this world, or false
function faNearestPlayer(level, x, z) {
  let server = level.getServer ? level.getServer() : null
  if (!server) return false
  let dim = String(level.dimension), best = false, bestD = Infinity
  server.getPlayerList().getPlayers().forEach(p => {
    if (String(p.level.dimension) != dim) return
    let d = Math.pow(p.getX() - x, 2) + Math.pow(p.getZ() - z, 2)
    if (d < bestD) { bestD = d; best = p }
  })
  return best
}

// Danger level at `dist` blocks (Overworld-sized) from the center, plus a dimension's bonus
global.FA_dangerAtDistance = (dist, bonus) => {
  let d = global.FA.danger
  let danger = dist < d.safeRadius ? 0 : Math.floor((dist - d.safeRadius) / d.blocksPerLevel) + 1
  danger += bonus || 0
  return d.maxLevel > 0 ? Math.min(d.maxLevel, danger) : danger
}

// Where a danger level starts, in blocks from the center (level 1 = safeRadius)
global.FA_dangerBlocks = danger => danger <= 0 ? 0 : global.FA.danger.safeRadius + (danger - 1) * global.FA.danger.blocksPerLevel

// The center danger is measured from, in Overworld coordinates: { x, z, end } or false (no scaling at all).
// `who`: the player it's for (zone titles, /ascension); mobs pass nothing and use the nearest player.
global.FA_dangerCenter = (level, x, z, who) => {
  let d = global.FA.danger
  if (d.centerOn == 'home') {
    let p = who && who.stringUuid ? who : faNearestPlayer(level, x, z)
    let h = p ? global.FA_STATE.homes[String(p.stringUuid)] : false
    return h || false
  }
  let center = global.FA_STATE.center
  if (center) return { x: center.x, z: center.z }
  let spawn = level.getSharedSpawnPos ? level.getSharedSpawnPos() : null
  return spawn ? { x: spawn.getX(), z: spawn.getZ() } : { x: 0, z: 0 }
}

global.FA_dangerLevel = (level, x, z, who) => {
  let d = global.FA.danger
  if (!d.enabled) return 0
  let c = global.FA_dangerCenter(level, x, z, who)
  if (!c) return 0
  let dim = String(level.dimension)
  if (dim == 'minecraft:the_nether') {
    // Nether coordinates are 1/8 of the Overworld's: measure there, then scale up
    let dist = Math.sqrt(Math.pow(x - c.x / d.netherScale, 2) + Math.pow(z - c.z / d.netherScale, 2)) * d.netherScale
    return global.FA_dangerAtDistance(dist, d.netherBonus)
  }
  if (dim == 'minecraft:overworld' && !c.end) return global.FA_dangerAtDistance(Math.sqrt(Math.pow(x - c.x, 2) + Math.pow(z - c.z, 2)), 0)
  // The End, the Ascended Realm and other dimensions: from their 0, 0
  let bonus = dim == 'minecraft:the_end' ? d.endBonus : dim == 'factoryascension:ascended_realm' ? d.realmBonus : 0
  return global.FA_dangerAtDistance(Math.sqrt(x * x + z * z), bonus)
}

global.FA_zone = danger => {
  let zones = global.FA.danger.zones
  let zone = zones[0]
  zones.forEach(z => { if (danger >= z.from) zone = z })
  return zone
}

// Multipliers for a mob at a danger level (elite = the rare named kind)
// The loot curve: (blocks out / 1000) ^ exponent, the same curve the mobs get tougher on (0 in the safe lands)
global.FA_lootCurve = danger => danger > 0 ? Math.pow(global.FA_dangerBlocks(danger) / 1000, global.FA.danger.exponent) : 0

global.FA_dangerMult = (danger, elite) => {
  let d = global.FA.danger
  let curve = global.FA_lootCurve(danger)
  return {
    health: (danger > 0 ? 1 + d.healthScale * Math.pow(global.FA_dangerBlocks(danger) / 1000, d.exponent) : 1) * (elite ? d.eliteHealth : 1),
    damage: (danger > 0 ? 1 + d.damageScale * Math.pow(global.FA_dangerBlocks(danger) / 1000, d.exponent) : 1) * (elite ? d.eliteDamage : 1),
    loot: (1 + d.lootScale * curve) * (elite ? d.eliteLoot : 1),
    xp: (1 + d.xpScale * curve) * (elite ? d.eliteLoot : 1)
  }
}

// ---- Factory Room TNT: the room's shape (shared by the game, the tests and the website) ----
// A room is laid out in local coordinates: u across (right is +), v forward (away from you), h up from the floor layer.
// The inside is W x L x H; around it is a one-block shell (floor, walls, roof) with lights in the roof and a doorway
// in the near wall. (cx, cz): the block you clicked (the middle of the near wall); floorY: the floor layer, one below
// your feet; (fx, fz): the way you face (one of the four directions).
global.FA_roomPlan = (sizeId, cx, floorY, cz, fx, fz) => {
  let R = global.FA.roomCharges
  let S = R.sizes.filter(z => z.id == sizeId)[0]
  if (!S) return false
  let W = S.size[0], L = S.size[1], H = S.size[2]
  let umin = -Math.floor(W / 2)
  return { cx: cx, cz: cz, floorY: floorY, fx: fx, fz: fz, rx: -fz, rz: fx, W: W, L: L, H: H, umin: umin, umax: umin + W - 1,
    cols: W + 2, rows: L + 2, layers: H + 2, total: (W + 2) * (L + 2) * (H + 2), spacing: Math.max(2, R.lightSpacing), door: R.doorway }
}

// What goes at a local spot: 'air' (inside or the doorway), 'shell', 'light', or false (outside the room)
global.FA_roomKind = (plan, u, v, h) => {
  if (u < plan.umin - 1 || u > plan.umax + 1 || v < 0 || v > plan.L + 1 || h < 0 || h > plan.H + 1) return false
  let inside = u >= plan.umin && u <= plan.umax && v >= 1 && v <= plan.L
  if (inside && h >= 1 && h <= plan.H) return 'air'
  // The doorway: the middle of the near wall, from the floor up
  let half = Math.floor(plan.door[0] / 2)
  if (v == 0 && Math.abs(u) <= half && h >= 1 && h <= Math.min(plan.H, plan.door[1])) return 'air'
  if (inside && h == plan.H + 1) {
    let sp = plan.spacing, off = Math.floor(sp / 2)
    if ((u - plan.umin) % sp == off && (v - 1) % sp == off) return 'light'
  }
  return 'shell'
}

// The i-th spot of a room (0 .. total - 1), top layer first so the roof goes in before anything below it is hollowed out
global.FA_roomSpot = (plan, i) => {
  let per = plan.cols * plan.rows
  let h = plan.H + 1 - Math.floor(i / per)
  let rest = i % per
  let u = plan.umin - 1 + (rest % plan.cols)
  let v = Math.floor(rest / plan.cols)
  return { x: plan.cx + u * plan.rx + v * plan.fx, y: plan.floorY + h, z: plan.cz + u * plan.rz + v * plan.fz, kind: global.FA_roomKind(plan, u, v, h) }
}

// The middle of the room's floor (where the ores it blasted through land)
global.FA_roomMiddle = plan => {
  let u = Math.floor((plan.umin + plan.umax) / 2), v = Math.floor((plan.L + 1) / 2)
  return { x: plan.cx + u * plan.rx + v * plan.fx, y: plan.floorY + 1, z: plan.cz + u * plan.rz + v * plan.fz }
}

// Does this enchant type suit this kind of gear ('weapon', 'armor', 'tool' or 'axe', which is both weapon and tool)?
global.FA_typeFits = (type, kind) => {
  let M = global.FA.gearMatch
  if (!M.enabled || M.any.indexOf(type) >= 0) return true
  if (kind == 'axe') return M.weapon.indexOf(type) >= 0 || M.tool.indexOf(type) >= 0
  return (M[kind] || M.weapon).indexOf(type) >= 0
}

// Does this enchant suit the gear? A Fusion or Triad suits it if any of its parts does.
global.FA_enchantFits = (id, kind) => {
  let e = global.FA.enchants[id]
  if (!e) return false
  if (e.type == 'fusion' && e.parts) return e.parts.some(p => global.FA_enchantFits(p, kind))
  return global.FA_typeFits(e.type, kind)
}

// Which kinds of gear an enchant suits, for messages: "weapons", "armor", "tools"
global.FA_fitsText = id => {
  let names = { weapon: 'weapons', armor: 'armor', tool: 'tools' }
  return ['weapon', 'armor', 'tool'].filter(k => global.FA_enchantFits(id, k)).map(k => names[k]).join(' or ') || 'nothing'
}

// A Chaos roll whose effects all suit the gear: tries seeds from `seed` on (the seed is what's saved on the item,
// so the same roll reads back the same way everywhere). Returns the seed.
global.FA_fittingSeed = (seed, kind) => {
  if (!global.FA.chaos.matchGear || !global.FA.gearMatch.enabled) return seed
  for (let i = 0; i < 400; i++) {
    let s = ((seed + i * 7919) % 2147483646) + 1
    if (global.FA_rollEnchant(s).affixes.every(a => global.FA_typeFits(a.type, kind))) return s
  }
  return seed
}

// A skinwalker's true form: one of skinwalkers.forms by weight, skipping mods that aren't installed. loaded(id) -> bool
global.FA_skinwalkerForm = (rnd, loaded) => {
  let forms = global.FA.skinwalkers.forms.filter(f => loaded(f[0]))
  let total = forms.reduce((a, f) => a + f[1], 0)
  let pick = rnd() * total
  for (let i = 0; i < forms.length; i++) {
    pick -= forms[i][1]
    if (pick < 0) return forms[i][0]
  }
  return forms.length ? forms[forms.length - 1][0] : 'minecraft:vindicator'
}

// ---- Mythic helpers (shared by the effects and the descriptions) ----
global.FA_chainChance = level => Math.min(1, 0.1 + 0.01 * level)
global.FA_chainTargets = level => Math.min(16, 1 + Math.floor(level / 5))
global.FA_auraRadius = level => Math.min(16, 3 + Math.floor(level / 10))
global.FA_chronos = level => ({ radius: Math.min(32, 8 + level), seconds: Math.min(10, 2 + 0.5 * level), cooldown: Math.max(5, 30 - level) })
global.FA_phoenixCooldown = (e, level) => Math.max(e.minCooldown, e.cooldown / (1 + e.perLevel * (level - 1)))
global.FA_fragmentChance = (danger, elite) => {
  let f = global.FA.fragments
  if (danger <= 0) return 0
  let curve = global.FA_lootCurve(danger)
  return elite ? Math.min(f.eliteChanceCap, f.eliteChance + f.eliteChanceScale * curve)
    : Math.min(f.normalChanceCap, f.normalChanceScale * curve)
}

// ---- Chaos Tome generator ----
// Park-Miller random numbers: exact in plain JS doubles, so every engine (KubeJS's Rhino, browsers,
// the test harness) produces the very same enchant from the same seed.
global.FA_rng = seed => {
  let state = (Math.abs(Math.floor(seed)) % 2147483646) + 1
  return () => {
    state = (state * 16807) % 2147483647
    return (state - 1) / 2147483646
  }
}

let FA_ROLL_WORDS = {
  prefix: {
    damage: ['Savage', 'Brutal', 'Searing', 'Vicious', 'Keen'], resist: ['Warded', 'Stalwart', 'Ironclad', 'Bastion'],
    hit_effect: ['Cursed', 'Blighted', 'Venomous', 'Frostbitten'], self_effect: ['Surging', 'Fervent', 'Kindled'],
    lifesteal: ['Sanguine', 'Vampiric'], hit_heal: ['Mending', 'Hallowed'], kill_heal: ['Ravenous', 'Gorging'],
    xp: ['Learned', 'Sage'], plunder: ['Greedy', 'Gilded'], drop: ['Lucky', 'Prospecting'], regen: ['Verdant', 'Blooming'],
    ignite: ['Blazing', 'Smoldering'], critical: ['Precise', 'Deadly'], cleave: ['Sweeping', 'Rending'],
    retribution: ['Spiteful', 'Thorned'], vitality: ['Hearty', 'Titanic'], mining: ['Delving', 'Tunneling'],
    dodge: ['Elusive', 'Phantom'], chain_lightning: ['Thundering', 'Stormborn'], gravity_well: ['Collapsing', 'Abyssal'],
    death_nova: ['Cataclysmic', 'Volatile'], aura: ['Radiant', 'Sunforged']
  },
  noun: [['Spark', 'Trinket', 'Charm'], ['Sigil', 'Rune', 'Edge'], ['Relic', 'Oath', 'Brand'], ['Covenant', 'Crown', 'Requiem'], ['Apotheosis', 'Singularity', 'Godspark']],
  suffix: ['the Abyss', 'the Forge', 'the Storm', 'the Void', 'Embers', 'the Tide', 'the Wilds', 'Dread', 'Dawn', 'Ashes', 'the Deep', 'Kings']
}

// Every effect a roll can pick. `min` = lowest rarity index it can appear at. Each builder gets
// (random 0..1, rarity power) and returns a normal enchant definition, checked by FA_validate like any other.
// Effects that have to be cleaned up when gear comes off (attributes, flight, passives) are never rolled.
let FA_AFFIXES = [
  { w: 10, make: (r, p) => ({ type: 'damage', when: ['always', 'undead', 'arthropod', 'boss', 'nether', 'raider', 'airborne', 'full_health', 'low_health', 'berserk', 'projectile', 'sneaking'][Math.floor(r() * 12)], perLevel: (0.05 + r() * 0.1) * p }) },
  { w: 8, make: (r, p) => ({ type: 'resist', vs: ['all', 'fire', 'fall', 'explosion', 'projectile', 'magic', 'wither', 'melee'][Math.floor(r() * 8)], perLevel: (0.03 + r() * 0.05) * p }) },
  { w: 6, make: (r, p) => {
    let fx = [['MOVEMENT_SLOWDOWN', 'Slowness'], ['WEAKNESS', 'Weakness'], ['WITHER', 'Wither'], ['POISON', 'Poison'], ['LEVITATION', 'Levitation'], ['GLOWING', 'Glowing']][Math.floor(r() * 6)]
    return { type: 'hit_effect', effect: fx[0], label: fx[1], seconds: 2 + Math.floor(r() * 4), ampEvery: Math.max(1, Math.round((8 + r() * 8) / p)) }
  } },
  { w: 4, make: (r, p) => {
    // Resistance stops at amplifier 2 (level 3): level 5 would make you immune to everything
    let fx = [['MOVEMENT_SPEED', 'Speed', 4], ['DAMAGE_BOOST', 'Strength', 4], ['REGENERATION', 'Regeneration', 3], ['DAMAGE_RESISTANCE', 'Resistance', 2]][Math.floor(r() * 4)]
    return { type: 'self_effect', effect: fx[0], label: fx[1], seconds: 2 + Math.floor(r() * 3), ampEvery: Math.max(1, Math.round((10 + r() * 10) / p)), maxAmp: fx[2] }
  } },
  { w: 5, make: (r, p) => ({ type: 'lifesteal', perLevel: (0.003 + r() * 0.005) * p }) },
  { w: 4, make: (r, p) => ({ type: 'hit_heal', perLevel: (0.2 + r() * 0.3) * p }) },
  { w: 4, make: (r, p) => ({ type: 'kill_heal', perLevel: (0.5 + r() * 1) * p }) },
  { w: 4, make: (r, p) => ({ type: 'xp', perLevel: (0.05 + r() * 0.07) * p }) },
  { w: 4, make: (r, p) => ({ type: 'plunder', perLevel: (0.02 + r() * 0.03) * p }) },
  { w: 4, make: (r, p) => {
    let d = [['minecraft:gold_nugget', 'gold nuggets', 0.2], ['minecraft:iron_nugget', 'iron nuggets', 0.3], ['minecraft:emerald', 'emeralds', 0.02],
      ['minecraft:diamond', 'diamonds', 0.005], ['minecraft:lapis_lazuli', 'lapis', 0.1], ['minecraft:quartz', 'quartz', 0.1]][Math.floor(r() * 6)]
    return { type: 'drop', item: d[0], label: d[1], perLevel: d[2] * (0.7 + r() * 0.6) * p }
  } },
  { w: 4, make: (r, p) => ({ type: 'regen', perLevel: (0.08 + r() * 0.12) * p }) },
  { w: 4, make: (r, p) => ({ type: 'ignite', perLevel: (2 + r() * 2) * p }) },
  { w: 3, make: (r, p) => ({ type: 'critical', critMultiplier: Math.round((2 + r() * p) * 10) / 10 }) },
  { w: 3, make: (r, p) => ({ type: 'cleave', perLevel: (0.02 + r() * 0.03) * p, radius: 2 + Math.floor(r() * 3) }) },
  { w: 3, make: (r, p) => ({ type: 'retribution', perLevel: (0.02 + r() * 0.03) * p }) },
  { w: 4, make: (r, p) => ({ type: 'vitality', perLevel: (1 + r() * 1.5) * p }) },
  { w: 3, make: (r, p) => ({ type: 'mining', perLevel: (0.1 + r() * 0.15) * p }) },
  { w: 2, make: (r, p) => ({ type: 'dodge', cap: Math.min(0.5, 0.15 + 0.05 * p) }) },
  { w: 2, min: 3, make: (r, p) => ({ type: 'chain_lightning', perLevel: (0.01 + r() * 0.01) * p, radius: 6 + Math.floor(r() * 5) }) },
  { w: 2, min: 3, make: (r, p) => ({ type: 'gravity_well', perLevel: 0.5 * p, base: 3, cap: 16 }) },
  { w: 2, min: 3, make: (r, p) => ({ type: 'death_nova', perLevel: (0.05 + r() * 0.05) * p }) },
  { w: 2, min: 3, make: (r, p) => ({ type: 'aura', perLevel: (1 + r()) * p }) }
]

let faRollCache = {}
let faRollCacheSize = 0

// The enchant a seed makes: { seed, name, rarity (index), affixes: [definition, ...] }
global.FA_rollEnchant = seed => {
  seed = Math.abs(Math.floor(seed)) % 2147483647
  if (faRollCache[seed]) return faRollCache[seed]
  let r = global.FA_rng(seed)
  let rarities = global.FA.chaos.rarities
  let total = 0
  rarities.forEach(x => { total += x.weight })
  let pick = r() * total
  let rarity = 0
  for (let i = 0; i < rarities.length; i++) {
    pick -= rarities[i].weight
    if (pick < 0) { rarity = i; break }
  }
  let R = rarities[rarity]

  // Pick distinct effect types, weighted
  let pool = FA_AFFIXES.filter(a => !a.min || rarity >= a.min)
  let affixes = []
  let used = {}
  while (affixes.length < R.affixes && pool.length) {
    let weight = 0
    pool.forEach(a => { weight += a.w })
    let x = r() * weight
    let chosen = pool[pool.length - 1]
    for (let i = 0; i < pool.length; i++) {
      x -= pool[i].w
      if (x < 0) { chosen = pool[i]; break }
    }
    pool = pool.filter(a => a !== chosen)
    let def = chosen.make(r, R.power)
    if (used[def.type]) continue
    used[def.type] = true
    def.name = 'affix'
    affixes.push(def)
  }

  let W = FA_ROLL_WORDS
  let prefixes = W.prefix[affixes[0].type] || ['Strange']
  let nouns = W.noun[rarity]
  let name = prefixes[Math.floor(r() * prefixes.length)] + ' ' + nouns[Math.floor(r() * nouns.length)]
  if (rarity >= 1) name += ' of ' + W.suffix[Math.floor(r() * W.suffix.length)]

  let roll = { seed: seed, name: name, rarity: rarity, affixes: affixes }
  // Cache rolls (they never change); clear the cache now and then so it can't grow forever
  if (faRollCacheSize > 2000) { faRollCache = {}; faRollCacheSize = 0 }
  faRollCache[seed] = roll
  faRollCacheSize++
  return roll
}

// ---- Shared helpers for gems, sets and prestige ----

// Item id for a gem: kubejs:gem_<type>_<tier 1..3>
global.FA_gemId = (type, tier) => 'kubejs:gem_' + type + '_' + tier
global.FA_gemLevels = tier => global.FA.gems.tiers[tier - 1].levels
// Total levels an item needs before its next Rebirth
global.FA_rebirthNeed = stars => global.FA.prestige.needLevels * (stars + 1)
global.FA_starMult = stars => 1 + global.FA.prestige.bonusPerStar * stars
global.FA_legendMult = stars => 1 + global.FA.legendary.bonusPerStar * Math.max(0, Math.min(global.FA.legendary.maxStars, stars))
// "★★★☆☆☆☆☆☆☆" for a star count out of maxStars
global.FA_legendStars = stars => '\u2605'.repeat(stars) + '\u2606'.repeat(Math.max(0, global.FA.legendary.maxStars - stars))

// ---- Condensed Essence helpers ----
// Tier 0 is the plain Condensed Essence Block; tiers 1..50 are the dense ones, "(x1)" .. "(x50)"
global.FA_condensedTiers = () => global.FA.condensed.names.length
global.FA_condensedId = tier => 'kubejs:condensed_essence_' + tier
global.FA_condensedName = tier => tier == 0 ? 'Condensed Essence Block' : global.FA.condensed.names[tier - 1] + ' Essence Block (x' + tier + ')'
// Essence in one block of a tier: (mbPerBlock / mbPerEssence) x ratio^tier
global.FA_condensedValue = tier => global.FA.condensed.mbPerBlock / global.FA.altar.mbPerEssence * Math.pow(global.FA.condensed.ratio, tier)

// ---- Loot helpers (shared by the game and the website) ----

// Chance a danger-zone kill drops a loot roll
global.FA_lootChance = danger => danger <= 0 ? 0 : Math.min(global.FA.loot.chanceCap, global.FA.loot.chance + global.FA.loot.chanceScale * global.FA_lootCurve(danger))

// ---- Meteor Shower pool: every item the pack makes, sorted into the six loot tiers (shared by the game and the tests) ----
// Returns 6 lists (Common ... Mythic) of item ids, or { relic: quality } for a Relic.
global.FA_meteorPool = () => {
  let P = global.FA.events.meteorShower.pool, FA = global.FA, E = FA.enchants
  let mod = typeof Platform != 'undefined' && Platform.isLoaded('factoryascension')
  let tiers = [[], [], [], [], [], []]
  let put = (tier, ids) => { if (tier >= 0 && tier < 6) ids.forEach(id => tiers[tier].push(id)) }
  let k = id => 'kubejs:' + id
  let gear = t => ['ingot', 'sword', 'pickaxe', 'axe', 'shovel', 'hoe', 'helmet', 'chestplate', 'leggings', 'boots'].map(g => k(t + '_' + g))
  let tomes = test => Object.keys(E).filter(id => test(E[id])).map(id => k('tome_' + id))
  put(P.basics, ['ascension_core', 'ascension_shard', 'raw_ascendite', 'crushed_raw_ascendite', 'ascendite_ingot', 'ascendite_nugget', 'ascendite_block',
    'ascendite_ore', 'deepslate_ascendite_ore', 'common_mineral_cluster', 'gem_mineral_cluster', 'exotic_mineral_cluster', 'blank_ascension_tome',
    'polishing_cloth', 'rock_hammer', 'ascendite_rock_hammer', 'liquid_essence_bucket'].map(k))
  put(P.food, [].concat.apply([], FA.mobFood.foods.map(fd => [k(fd.raw), k(fd.cooked)])))
  put(P.ascenditeGear, gear('ascendite').filter(id => id != k('ascendite_ingot')))
  put(P.attachments, FA.attachments.list.map(a => k('attachment_' + a.id)).concat(FA.attachments.batteries.map(b => k(b.id)))
    .concat(FA.machines.miniCrusher.motors.map(m => k(m.id))))
  put(P.packs, [k('enchantment_pack'), k('gear_cache')])
  put(P.denseCore, [k('dense_ascension_core')])
  put(P.baseTomes, tomes(e => !e.mythic && !e.ultimate && e.type != 'fusion'))
  put(P.voidsteel, gear('voidsteel'))
  put(P.fusionTomes, tomes(e => e.type == 'fusion' && !e.triad))
  put(P.chaos, [k('chaos_tome'), k('mythic_fragment')])
  put(P.machines, [k('ascension_altar'), k('essence_condenser')])
  put(P.celestium, gear('celestium'))
  put(P.triadTomes, tomes(e => e.triad))
  put(P.singularity, [k('ascension_singularity'), k('booster_box')])
  put(P.ascendium, gear('ascendium'))
  put(P.mythicTomes, tomes(e => e.mythic))
  put(P.souls, [k('ascended_soul')])
  FA.pets.collars.forEach((c, i) => put(P.collars[Math.min(i, P.collars.length - 1)], [k(c.id)]))
  put(P.whistle, [k('pet_whistle'), k('unbinding_scroll')])
  FA.charms.list.forEach(c => put(c.id == 'ascended_charm' ? P.ascendedCharm : P.charms, [k(c.id)]))
  put(P.ultimateTomes, tomes(e => e.ultimate))
  Object.keys(FA.gems.types).forEach(type => P.gems.forEach((t, i) => put(t, [global.FA_gemId(type, i + 1)])))
  FA.roomCharges.styles.forEach(st => FA.roomCharges.sizes.forEach((sz, i) => put(P.roomTnt[i], [k('room_tnt_' + st.id + '_' + sz.id)])))
  FA.foils.types.forEach((f, i) => put(P.foils[Math.min(i, P.foils.length - 1)], [k('foil_sheet_' + f.id)]))
  for (let t = 0; t <= global.FA_condensedTiers(); t++) put(Math.min(5, 1 + Math.floor(t / P.condensedPerTier)), [global.FA_condensedId(t)])
  Object.keys(P.relics).forEach(q => put(P.relics[q], [{ relic: q }]))
  if (mod) {
    put(P.engine, ['engine_core', 'engine_coil', 'engine_gearbox', 'engine_burner', 'storage_linker', 'mini_crusher'].concat(Object.keys(FA.machines.glass)).map(b => 'factoryascension:' + b))
    put(P.machines, ['card_binder', 'pack_opener', 'salvager', 'tome_fusion_table', 'super_smelter', 'storage_terminal'].map(b => 'factoryascension:' + b))
    put(P.bigMachines, ['ascension_vault', 'essence_forge', 'ascension_workbench'].map(b => 'factoryascension:' + b))
    put(P.machines, ['ascension_quarry', 'mob_grinder', 'warp_hopper'].map(b => 'factoryascension:' + b))
    put(P.machines, ['factoryascension:shotgun', 'factoryascension:flare_gun'].concat(FA.guns.list.map(g => 'factoryascension:' + g.id)))
    put(P.basics, ['factoryascension:shotgun_shell', 'factoryascension:flare_red', 'factoryascension:flare_green', 'factoryascension:cartridge', 'factoryascension:attachment',
      'factoryascension:bandage', 'factoryascension:medkit', 'factoryascension:frag_grenade'])
    put(P.machines, ['factoryascension:weapons_workbench'])
    put(P.bigMachines, ['realm_gate', 'realm_stone', 'realm_moss', 'realm_crystal', 'essence_crystal_ore', 'voidsteel_ore', 'astral_ore'].map(b => 'factoryascension:' + b))
    if (Platform.isLoaded('sophisticatedbackpacks')) FA.backpacks.tiers.forEach((t, i) => put(P.backpacks[Math.min(i, P.backpacks.length - 1)], ['factoryascension:' + t.id + '_backpack']))
  }
  global.FA.events.meteorShower.extra.forEach(x => put(x[1], [x[0]]))
  return tiers
}

// One meteor: a tier with the loot odds at this danger, then any item of it. { tier, id, count } or { tier, relic }.
global.FA_meteorRoll = (r, danger, pool) => {
  let M = global.FA.events.meteorShower
  pool = pool || global.FA_meteorPool()
  let odds = global.FA_lootOdds(danger)
  let x = r(), tier = 0
  for (let i = 0; i < odds.length; i++) { x -= odds[i]; if (x < 0) { tier = i; break } }
  while (tier > 0 && pool[tier].length == 0) tier--
  let pick = pool[tier][Math.floor(r() * pool[tier].length)]
  if (typeof pick != 'string') return { tier: tier, relic: pick.relic }
  let c = M.counts[tier]
  return { tier: tier, id: pick, count: Math.max(1, Math.round((c[0] + Math.floor(r() * (c[1] - c[0] + 1))) * (c[1] > 1 ? global.FA_lootBulk(danger) : 1))) }
}

// Stack sizes of bulk drops grow on the loot curve
global.FA_lootBulk = danger => 1 + global.FA.loot.bulkScale * global.FA_lootCurve(danger)

// Odds of each tier at a danger level, as fractions that add up to 1
global.FA_lootOdds = danger => {
  let curve = global.FA_lootCurve(danger)
  let w = global.FA.loot.tiers.map(t => t.weight * Math.pow(1 + curve, t.growth * global.FA.loot.tierCurve))
  let total = w.reduce((a, b) => a + b, 0)
  return w.map(x => x / total)
}

let FA_RELIC_KINDS = {
  weapon: { slot: 'weapon', items: ['sword', 'axe'], nouns: ['Blade', 'Cleaver', 'Edge', 'Fang'] },
  tool: { slot: 'tool', items: ['pickaxe'], nouns: ['Pick', 'Breaker', 'Delver'] },
  head: { slot: 'armor', items: ['helmet'], nouns: ['Crown', 'Visage', 'Helm'] },
  chest: { slot: 'armor', items: ['chestplate'], nouns: ['Aegis', 'Heart', 'Bulwark'] },
  legs: { slot: 'armor', items: ['leggings'], nouns: ['Greaves', 'Legguards'] },
  feet: { slot: 'armor', items: ['boots'], nouns: ['Treads', 'Striders'] }
}
// Which enchant types suit which kind of gear
let FA_RELIC_TYPES = {
  weapon: ['damage', 'critical', 'reaper', 'cleave', 'lifesteal', 'hit_effect', 'self_effect', 'ignite', 'hit_heal', 'plunder', 'kill_heal', 'xp', 'drop',
    'chain_lightning', 'gravity_well', 'death_nova', 'souls', 'overkill', 'rampage', 'heads', 'fusion'],
  armor: ['resist', 'vitality', 'retribution', 'dodge', 'undying', 'regen', 'passive', 'attribute', 'phoenix', 'time_stop', 'aura', 'slam', 'flight', 'fusion'],
  tool: ['mining', 'vein', 'magnet', 'repair', 'eternal', 'xp', 'drop']
}
let FA_RELIC_WORDS = {
  prefix: ['Abyssal', 'Godforged', 'Starfallen', 'Dreadbound', 'Sunforged', 'Voidtouched', 'Stormborn', 'Bloodsworn', 'Ashen', 'Eternal', 'Runebound', 'Hollow'],
  suffix: ['the Storm', 'the Abyss', 'Ten Thousand Kills', 'the Last King', 'Embers', 'the Void', 'Dawn', 'the Deep', 'Ruin', 'the Forge']
}

// A Relic as plain data: { item, name, quality, color, enchants: {id: level}, gems: [[type, tier]], rolled: [[seed, level]], stars }.
// `r` is a random function (Math.random in game, seeded on the website). The game writes this onto a real item.
global.FA_makeRelic = (r, quality, danger) => {
  let Q = global.FA.loot.relics[quality]
  let pick = list => list[Math.floor(r() * list.length)]
  let kindName = pick(Object.keys(FA_RELIC_KINDS))
  let kind = FA_RELIC_KINDS[kindName]
  let metal = Q.metal || (quality == 'mythic' ? 'netherite' : 'diamond')
  let item = 'minecraft:' + metal + '_' + pick(kind.items)
  let E = global.FA.enchants
  let suits = id => FA_RELIC_TYPES[kind.slot].indexOf(E[id].type) >= 0 && !E[id].ultimate
  let normal = Object.keys(E).filter(id => suits(id) && !E[id].mythic)
  let mythics = Object.keys(E).filter(id => suits(id) && E[id].mythic)
  let base = (8 + global.FA.loot.relicLevelScale * global.FA_lootCurve(danger)) * Q.levelMult
  let level = id => E[id].maxLevel ? 1 + Math.floor(r() * E[id].maxLevel) : Math.max(1, Math.min(global.FA.jackpot.levelCap, Math.round(base * (0.5 + r()) * global.FA_luckyMult(r))))
  let enchants = {}
  let count = Q.enchants[0] + Math.floor(r() * (Q.enchants[1] - Q.enchants[0] + 1))
  for (let i = 0; i < Q.mythicEnchants && mythics.length; i++) { let id = pick(mythics); enchants[id] = level(id) }
  let guard = 0
  while (Object.keys(enchants).length < count + Q.mythicEnchants && normal.length && guard++ < 50) {
    let id = pick(normal)
    if (!enchants[id]) enchants[id] = level(id)
  }
  let gems = []
  let gemTypes = Object.keys(global.FA.gems.types)
  let gemCount = Math.min(global.FA.gems.sockets, Q.gems[0] + Math.floor(r() * (Q.gems[1] - Q.gems[0] + 1)))
  for (let i = 0; i < gemCount && gemTypes.length; i++) gems.push([pick(gemTypes), quality == 'mythic' ? 3 : 2])
  let rolled = []
  for (let i = 0; i < Q.rolled; i++) rolled.push([1 + Math.floor(r() * 2147483645), Math.max(1, Math.round(base / 3))])
  let stars = r() < Q.starChance ? 1 : 0
  let name = pick(FA_RELIC_WORDS.prefix) + ' ' + pick(kind.nouns) + ' of ' + pick(FA_RELIC_WORDS.suffix)
  return { item: item, name: name, quality: quality, color: Q.color, kind: kindName, enchants: enchants, gems: gems, rolled: rolled, stars: stars }
}

// One loot roll: picks a tier (bump = tiers to lean up, a chance each) and an entry from it.
// Returns { tier, drop: { kind, id, count } or { kind: 'relic', relic } }.
global.FA_rollLoot = (r, danger, bump) => {
  let L = global.FA.loot
  let odds = global.FA_lootOdds(danger)
  let x = r()
  let tier = 0
  for (let i = 0; i < odds.length; i++) { x -= odds[i]; if (x < 0) { tier = i; break } }
  for (let i = 0; i < (bump || 0); i++) if (r() < 0.5) tier = Math.min(L.tiers.length - 1, tier + 1)
  let drops = L.tiers[tier].drops
  let total = drops.reduce((a, d) => a + d[d.length - 1], 0)
  let y = r() * total
  let d = drops[drops.length - 1]
  for (let i = 0; i < drops.length; i++) { y -= drops[i][drops[i].length - 1]; if (y < 0) { d = drops[i]; break } }
  let scale = global.FA_lootBulk(danger)   // bulk amounts grow on the loot curve
  let E = global.FA.enchants
  let tomeOf = group => {
    let ids = Object.keys(E).filter(id => group == 'ultimate' ? E[id].ultimate : group == 'mythic' ? E[id].mythic : group == 'fusion' ? E[id].type == 'fusion'
      : !E[id].mythic && !E[id].ultimate && E[id].type != 'fusion')
    return ids.length ? 'kubejs:tome_' + ids[Math.floor(r() * ids.length)] : 'kubejs:blank_ascension_tome'
  }
  let drop
  switch (d[0]) {
    case 'item': drop = { kind: 'item', id: d[1], count: Math.max(1, Math.round((d[2] + r() * (d[3] - d[2])) * (d[2] > 1 ? scale : 1))) }; break
    case 'gem': {
      let types = Object.keys(global.FA.gems.types)
      drop = { kind: 'item', id: global.FA_gemId(types[Math.floor(r() * types.length)], d[1]), count: 1 }
      break
    }
    case 'tome': drop = { kind: 'item', id: tomeOf(d[1]), count: 1 }; break
    case 'condensed': {
      let top = Math.min(global.FA_condensedTiers(), d[2] + Math.floor(global.FA_lootCurve(danger) * L.condensedScale))
      drop = { kind: 'item', id: global.FA_condensedId(Math.min(top, d[1] + Math.floor(r() * (top - d[1] + 1)))), count: 1 + Math.floor(r() * 3) }
      break
    }
    case 'relic': drop = { kind: 'relic', relic: global.FA_makeRelic(r, d[1], danger) }; break
  }
  return { tier: tier, drop: drop }
}

// Essence cost for a rolled enchant from level L to L+1
global.FA_rollCost = (level, rarity) => Math.ceil(global.FA_cost(level) * global.FA.chaos.rarities[rarity].costMult)

// Essence cost to go from level L to L+1 (Mythic enchants cost more)
// The price multiplier of an enchant: Mythic ones use mythicCostMultiplier, others can set costMult
global.FA_costMult = id => {
  let e = id ? global.FA.enchants[id] : null
  return e ? (e.ultimate ? global.FA.ultimateCostMultiplier : e.mythic ? global.FA.mythicCostMultiplier : e.costMult || 1) : 1
}

// Ultimate helpers: the combo moves the engine knows, cooldown and power at a level
global.FA_ULTIMATE_MOVES = ['thunder_waltz', 'thousand_cuts', 'heaven_splitter', 'dragon_spiral', 'void_collapse', 'starfall',
  'shadow_barrage', 'frozen_eternity', 'spirit_bomb', 'blade_storm', 'eclipse', 'phoenix_dive']
global.FA_ultCooldown = (e, level) => Math.max(5, e.cooldown - (level - 1))
global.FA_ultPower = (e, level) => e.power * (1 + e.perLevel * (level - 1))
global.FA_cost = (level, id) => Math.ceil(global.FA.costBase * Math.pow(level + 1, global.FA.costPower) * global.FA_costMult(id))

// ---- Config check ----
// Runs at startup. A typo or bad number switches that one thing off (with a log line) instead of
// crashing the game or writing bad data into items and worlds.

global.FA_TYPES = {
  // type: fields that must be numbers (perLevel etc.)
  damage: ['perLevel'], resist: ['perLevel'], critical: ['critMultiplier'], reaper: ['perLevel', 'cap'], cleave: ['perLevel', 'radius'],
  lifesteal: ['perLevel'], hit_effect: ['seconds', 'ampEvery'], self_effect: ['seconds', 'ampEvery'], ignite: ['perLevel'],
  hit_heal: ['perLevel'], plunder: ['perLevel'], kill_heal: ['perLevel'], xp: ['perLevel'], drop: ['perLevel'],
  vitality: ['perLevel'], retribution: ['perLevel'], dodge: ['cap'], undying: ['cooldown', 'perLevel', 'minCooldown'],
  regen: ['perLevel'], passive: [], magnet: ['perLevel', 'base', 'cap'], repair: ['perLevel'], attribute: ['perLevel'],
  mining: ['perLevel'], eternal: [], soulbound: [], flight: [], chain_lightning: ['perLevel', 'radius'], gravity_well: ['perLevel', 'base', 'cap'],
  death_nova: ['perLevel'], time_stop: [], phoenix: ['cooldown', 'perLevel', 'minCooldown'], projectile_teleport: [],
  souls: ['perLevel'], overkill: ['perLevel', 'radius'], rampage: ['perLevel', 'maxStacks', 'seconds'], vein: ['perLevel', 'cap'],
  aura: ['perLevel'], heads: ['perLevel'], slam: ['perLevel'], fusion: [], ultimate: ['power', 'perLevel', 'cooldown']
}
// Fusions can't be made from these: they need cleanup when gear comes off, or they're one-offs
let FA_NO_FUSE = { attribute: true, passive: true, eternal: true, soulbound: true, flight: true, fusion: true, ultimate: true }
let FA_ATTRS = ['movement_speed', 'attack_speed', 'attack_knockback', 'luck', 'block_reach', 'entity_reach', 'step_height']
let FA_VS = ['all', 'fire', 'fall', 'explosion', 'projectile', 'magic', 'wither', 'melee']

// A lucky multiplier: >= 1, usually small, very rarely huge (a Pareto tail, see jackpot)
global.FA_luckyMult = rnd => {
  let J = global.FA.jackpot
  return Math.min(J.levelCap, Math.pow(Math.max(1e-12, 1 - rnd()), -1 / J.levelAlpha))
}

// A foil for a card or loot tome: index into foils.types (or -1 for none). rarity: 0 (Common) .. 5 (Secret Rare)
global.FA_rollFoil = (rnd, rarity) => {
  let F = global.FA.foils
  if (!F.enabled) return -1
  let chance = F.cardChance[Math.min(F.cardChance.length - 1, Math.max(0, rarity))]
  if (rnd() >= chance) return -1
  return global.FA_foilQuality(rnd, rarity)
}

// Which foil, leaning toward better ones the higher the rarity (0..5)
global.FA_foilQuality = (rnd, rarity) => {
  let F = global.FA.foils
  let w = F.types.map((t, i) => t.weight * Math.pow(1 + F.rarityBoost, i * rarity / 5))
  let total = w.reduce((a, b) => a + b, 0)
  let pick = rnd() * total
  for (let i = 0; i < w.length; i++) {
    pick -= w[i]
    if (pick < 0) return i
  }
  return w.length - 1
}

// One card from a pack: { rarity index, enchant id, level }. rnd: 0..1 random; minRarity: lowest allowed index.
global.FA_rollCard = (rnd, minRarity) => {
  let P = global.FA.packs
  let pool = P.rarities.map((r, i) => i >= (minRarity || 0) ? r.weight : 0)
  let total = pool.reduce((a, b) => a + b, 0)
  let pick = rnd() * total, ri = 0
  for (let i = 0; i < pool.length; i++) {
    pick -= pool[i]
    if (pick < 0) { ri = i; break }
    ri = i
  }
  let R = P.rarities[ri]
  let E = global.FA.enchants
  let ids = Object.keys(E).filter(id => R.kind == 'ultimate' ? E[id].ultimate : R.kind == 'mythic' ? E[id].mythic : R.kind == 'fusion' ? E[id].type == 'fusion'
    : !E[id].mythic && !E[id].ultimate && E[id].type != 'fusion')
  let id = ids[Math.floor(rnd() * ids.length)]
  let level = Math.round((R.level[0] + Math.floor(rnd() * (R.level[1] - R.level[0] + 1))) * global.FA_luckyMult(rnd))
  level = Math.min(level, global.FA.jackpot.levelCap)
  if (E[id].maxLevel) level = Math.min(level, E[id].maxLevel)
  return { rarity: ri, id: id, level: Math.max(1, level), foil: global.FA_rollFoil(rnd, ri) }
}

// A whole pack of cards; the last one is Rare or better when lastCardRare is on
global.FA_openPack = rnd => {
  let P = global.FA.packs
  let cards = []
  for (let i = 0; i < P.cards; i++) cards.push(global.FA_rollCard(rnd, i == P.cards - 1 && P.lastCardRare ? 2 : 0))
  return cards
}

// Which relic quality a Sealed Gear Cache opened at this danger level gives
global.FA_cacheQuality = (rnd, danger) => {
  let Q = global.FA.caches.qualities
  let w = Q.map(q => q.weight * Math.pow(1 + q.growth, Math.max(0, danger)))
  let total = w.reduce((a, b) => a + b, 0)
  let pick = rnd() * total
  for (let i = 0; i < Q.length; i++) {
    pick -= w[i]
    if (pick < 0) return Q[i].quality
  }
  return Q[Q.length - 1].quality
}

// A brand-new Artifact (plain data): name, title, color, aura, lore and rolled quirks fitting the gear kind
global.FA_rollArtifact = (rnd, kind) => {
  let A = global.FA.artifacts
  let pick = list => list[Math.floor(rnd() * list.length)]
  let n = A.names
  let fits = A.list.filter(q => q.for == 'any' || q.for == kind)
  let count = Math.min(fits.length, A.quirks[0] + Math.floor(rnd() * (A.quirks[1] - A.quirks[0] + 1)))
  // Shuffle the fitting quirks and take the first few (always finishes, even with a stuck random source)
  let order = fits.slice()
  for (let i = order.length - 1; i > 0; i--) {
    let j = Math.floor(rnd() * (i + 1))
    let t = order[i]
    order[i] = order[j]
    order[j] = t
  }
  let quirks = order.slice(0, count).map(q => {
    let power = q.power[0] + rnd() * (q.power[1] - q.power[0])
    return { id: q.id, power: Math.round(power * 10) / 10 }
  })
  let ci = Math.floor(rnd() * A.colors.length)
  return {
    name: pick(n.first) + "'" + pick(n.second), title: pick(n.titles), lore: pick(n.lore),
    color: A.colors[ci], aura: A.auras[ci % A.auras.length], quirks: quirks, seed: Math.floor(rnd() * 2147483647)
  }
}

// "Headlamp: lights up the area around you (light 15)"
global.FA_attachmentText = id => {
  let a = global.FA.attachments.list.filter(x => x.id == id)[0]
  return a ? a.name + ': ' + global.FA_attachmentEffect(a) : id
}
// What an attachment does, with its numbers filled in: {p} power, {s} spikes, {a} arcs; powered ones say so
global.FA_attachmentEffect = a => a.text.replace('{p}', String(a.power)).replace('{s}', String(a.spikes || 0)).replace('{a}', String(a.arcs || 0)) +
  (a.charge ? ' (' + a.charge + ' battery charge each)' : '')
global.FA_attachmentDef = id => global.FA.attachments.list.filter(x => x.id == id)[0]

// "Echo Strike: 23% chance a hit lands twice"
global.FA_quirkText = (id, power) => {
  let q = global.FA.artifacts.list.filter(x => x.id == id)[0]
  if (!q) return id
  let p = Math.round(power * 10) / 10
  return q.name + ': ' + q.text.replace('{p}', String(p))
}

// Which variant a new mob becomes (rarest checked first), or '' for none. Elementals only for hostile mobs.
global.FA_rollVariant = (rnd, hostile) => {
  let V = global.FA.variants
  if (!V.enabled) return ''
  for (let i = 0; i < V.types.length; i++) {
    let t = V.types[i]
    if (t.elemental && !hostile) continue
    if (rnd() < t.chance) return t.id
  }
  return ''
}
global.FA_variantType = id => global.FA.variants.types.filter(t => t.id == id)[0]

// A random condition grade (index into condition.grades), by weight
global.FA_rollCondition = rnd => {
  let G = global.FA.condition.grades
  let total = 0
  G.forEach(g => { total += g.weight })
  let r = rnd() * total
  for (let i = 0; i < G.length; i++) {
    r -= G[i].weight
    if (r < 0) return i
  }
  return G.length - 1
}

// One smash of a rock with a hammer: [[item, count]] (rnd: a 0..1 random function). Shared by the game and tests.
global.FA_smash = (rnd, rock, hammerId) => {
  let H = global.FA.hammer
  let table = H.rocks[rock], tool = H.tools[hammerId]
  if (!table || !tool) return []
  let out = []
  table.forEach(d => {
    if (rnd() < Math.min(1, d[1] * tool.luck)) out.push([d[0], 1 + Math.floor(rnd() * d[2])])
  })
  return out
}

global.FA_validate = cfg => {
  let problems = []
  let okNum = (v, min) => typeof v == 'number' && isFinite(v) && v >= min
  let need = (v, path, min) => {
    if (okNum(v, min || 0)) return true
    problems.push(path + ' must be a number >= ' + (min || 0) + ' (got ' + v + ')')
    return false
  }

  Object.keys(cfg.enchants).forEach(id => {
    let e = cfg.enchants[id]
    let ok = /^[a-z0-9_]+$/.test(id) && typeof e.name == 'string' && e.name.length > 0
    if (!ok) problems.push('enchant "' + id + '" needs a lowercase id and a name')
    let fields = global.FA_TYPES[e.type]
    if (!fields) {
      problems.push('enchant "' + id + '" has unknown type "' + e.type + '"')
      ok = false
    } else {
      fields.forEach(f => { if (!need(e[f], 'enchants.' + id + '.' + f, 0)) ok = false })
    }
    ;['cap', 'radius', 'seconds', 'maxAmp', 'cooldown', 'minCooldown', 'base', 'maxStacks'].forEach(f => {
      if (e[f] !== undefined && !need(e[f], 'enchants.' + id + '.' + f, 0)) ok = false
    })
    if (e.ampEvery !== undefined && !need(e.ampEvery, 'enchants.' + id + '.ampEvery', 1)) ok = false
    if (e.maxLevel !== undefined && (!need(e.maxLevel, 'enchants.' + id + '.maxLevel', 1) || e.maxLevel % 1 != 0)) ok = false
    if (e.type == 'damage' && FA_WHEN[e.when] === undefined) { problems.push('enchants.' + id + '.when "' + e.when + '" is unknown'); ok = false }
    if (e.type == 'resist' && FA_VS.indexOf(e.vs) < 0) { problems.push('enchants.' + id + '.vs "' + e.vs + '" is unknown'); ok = false }
    if (e.type == 'attribute' && !(e.attrs && e.attrs.length && e.attrs.every(a => FA_ATTRS.indexOf(a) >= 0))) {
      problems.push('enchants.' + id + '.attrs must list known attributes: ' + FA_ATTRS.join(', '))
      ok = false
    }
    if (e.type == 'vein' && e.blocks != 'ores' && e.blocks != 'logs') { problems.push('enchants.' + id + ".blocks must be 'ores' or 'logs'"); ok = false }
    if (e.type == 'fusion' && !(e.parts && (e.parts.length == 2 || (e.triad && e.parts.length == 3)) && e.parts.every((pid, i) => e.parts.indexOf(pid) == i) && e.parts.every(pid => {
      let parent = cfg.enchants[pid]
      return parent && !parent.mythic && !FA_NO_FUSE[parent.type]
    }))) { problems.push('fusion "' + id + '" needs ' + (e.triad ? 'three' : 'two') + ' different, non-Mythic parents that exist'); ok = false }
    if (e.type == 'ultimate' && (global.FA_ULTIMATE_MOVES.indexOf(e.move) < 0 || typeof e.desc != 'string')) {
      problems.push('ultimate "' + id + '" needs a known move (' + global.FA_ULTIMATE_MOVES.join(', ') + ') and a desc'); ok = false
    }
    if (e.costMult !== undefined && !need(e.costMult, 'enchants.' + id + '.costMult', 1)) ok = false
    if (e.type == 'drop' && !/^[a-z0-9_.-]+:[a-z0-9_/.-]+$/.test(String(e.item))) { problems.push('enchants.' + id + '.item is not an item id'); ok = false }
    if (['hit_effect', 'self_effect', 'passive'].indexOf(e.type) >= 0 && e.effect === undefined) { problems.push('enchants.' + id + '.effect is missing'); ok = false }
    if (e.effect !== undefined && !/^[A-Z_]+$/.test(String(e.effect))) { problems.push('enchants.' + id + '.effect must be a MobEffects field like NIGHT_VISION'); ok = false }
    if (!ok) {
      delete cfg.enchants[id]
      problems.push('enchant "' + id + '" was switched off')
    }
  })

  // Essence must follow the 9:1 compression, or unpacking cores would create Essence from nothing
  let ev = cfg.essenceValues
  let core = ev['kubejs:ascension_core']
  if (!okNum(core, 0.000001) || ev['kubejs:dense_ascension_core'] != 9 * core || ev['kubejs:ascension_singularity'] != 81 * core) {
    problems.push('essenceValues must be 1 : 9 : 81 (core : dense : singularity), reset to 1 / 9 / 81')
    cfg.essenceValues = { 'kubejs:ascension_core': 1, 'kubejs:dense_ascension_core': 9, 'kubejs:ascension_singularity': 81 }
  }
  if (!okNum(cfg.costBase, 0.000001)) { problems.push('costBase reset to 0.05'); cfg.costBase = 0.05 }
  if (!okNum(cfg.costPower, 0)) { problems.push('costPower reset to 1'); cfg.costPower = 1 }
  if (!okNum(cfg.fusionStrength, 0.01) || cfg.fusionStrength > 1) { problems.push('fusionStrength reset to 0.6'); cfg.fusionStrength = 0.6 }
  if (!okNum(cfg.ultimateCostMultiplier, 1)) { problems.push('ultimateCostMultiplier reset to 10'); cfg.ultimateCostMultiplier = 10 }
  if (!okNum(cfg.mythicCostMultiplier, 1)) { problems.push('mythicCostMultiplier reset to 5'); cfg.mythicCostMultiplier = 5 }
  ;['maxExtraDropStacks', 'maxCleaveTargets', 'maxUpgradesPerCommand'].forEach(f => {
    if (!okNum(cfg[f], 1)) { problems.push(f + ' reset to 8'); cfg[f] = 8 }
  })

  let d = cfg.danger
  let dangerOk = ['safeRadius', 'netherScale', 'netherBonus', 'endBonus', 'realmBonus', 'healthScale', 'damageScale', 'exponent', 'lootScale',
    'xpScale', 'eliteChancePerLevel', 'eliteChanceCap', 'maxLevel'].every(f => need(d[f], 'danger.' + f, 0)) &&
    need(d.blocksPerLevel, 'danger.blocksPerLevel', 1) && need(d.eliteHealth, 'danger.eliteHealth', 1) &&
    need(d.eliteDamage, 'danger.eliteDamage', 0) && need(d.eliteLoot, 'danger.eliteLoot', 0) &&
    d.zones && d.zones.length > 0 && d.zones[0].from == 0 && (d.centerOn == 'home' || d.centerOn == 'spawn')
  if (!dangerOk && d.enabled) {
    problems.push('danger settings are broken, so danger scaling was switched off')
    d.enabled = false
  }
  let c = cfg.chaos
  let chaosOk = c && okNum(c.maxPerItem, 1) && c.maxPerItem <= 10 && c.maxPerItem % 1 == 0 && c.rarities && c.rarities.length > 0 &&
    c.rarities.every(R => typeof R.name == 'string' && okNum(R.weight, 0) && okNum(R.power, 0.01) && okNum(R.costMult, 1) &&
      okNum(R.affixes, 1) && R.affixes <= 5 && R.affixes % 1 == 0 && ['gray', 'aqua', 'light_purple', 'gold', 'red'].indexOf(R.color) >= 0) &&
    c.rarities.some(R => R.weight > 0)
  if (!chaosOk) {
    problems.push('chaos settings are broken, reset to defaults')
    cfg.chaos = { maxPerItem: 3, rarities: [
      { name: 'Common', weight: 50, power: 1, affixes: 1, costMult: 1, color: 'gray' },
      { name: 'Rare', weight: 28, power: 1.5, affixes: 2, costMult: 1.5, color: 'aqua' },
      { name: 'Epic', weight: 15, power: 2.2, affixes: 2, costMult: 2.5, color: 'light_purple' },
      { name: 'Legendary', weight: 6, power: 3.2, affixes: 3, costMult: 4, color: 'gold' },
      { name: 'Mythic', weight: 1, power: 5, affixes: 3, costMult: 8, color: 'red' }
    ] }
  }
  // New systems: each one switches itself off if its settings are broken
  let a = cfg.altar
  if (!(a && okNum(a.mbPerEssence, 1) && okNum(a.maxLevelsPerSecond, 1) && okNum(a.maxAltars, 1))) {
    problems.push('altar settings are broken, reset to defaults')
    cfg.altar = { mbPerEssence: 100, maxLevelsPerSecond: 20, maxAltars: 256 }
  }
  let cd = cfg.condensed
  if (!(cd && okNum(cd.mbPerBlock, 1) && cd.mbPerBlock % 1 == 0 && okNum(cd.ratio, 2) && cd.ratio <= 9 && cd.ratio % 1 == 0 &&
    okNum(cd.blocksPerSecond, 1) && okNum(cd.maxCondensers, 1) && cd.names && cd.names.length >= 1 && cd.names.length <= 100 &&
    cd.names.every(n => typeof n == 'string' && n.length > 0) && cd.mbPerBlock % cfg.altar.mbPerEssence == 0)) {
    // ratio must be 2..9 (the press compacts at most 9 items), and a block must hold whole Essence
    problems.push('condensed settings are broken, reset to defaults')
    cfg.condensed = { mbPerBlock: 1000, ratio: 9, blocksPerSecond: 64, maxCondensers: 256, names: ['Dense'] }
  }
  let pr = cfg.prestige
  let lg = cfg.legendary
  if (lg && lg.enabled && !(okNum(lg.maxStars, 1) && okNum(lg.bonusPerStar, 0) && lg.effectLevels && lg.effectLevels.length >= lg.maxStars &&
    lg.costs && lg.costs.length >= lg.maxStars && lg.grades && lg.grades.length > lg.maxStars && lg.costs.every(c => c.every(x => typeof x[0] == 'string' && okNum(x[1], 1))))) {
    problems.push('legendary star settings are broken, Legendary stars were switched off')
    lg.enabled = false
  }
  if (!(pr && okNum(pr.needLevels, 1) && okNum(pr.bonusPerStar, 0) && okNum(pr.maxStars, 0))) {
    problems.push('prestige settings are broken, reset to defaults')
    cfg.prestige = { needLevels: 1000, bonusPerStar: 0.25, maxStars: 0 }
  }
  cfg.sets = (cfg.sets || []).filter(set => {
    let ok = set && typeof set.name == 'string' && cfg.enchants[set.key] && [2, 4].every(n => !set[n] || (okNum(set[n].mult, 1) &&
      Object.keys(set[n].add || {}).every(k => cfg.enchants[k] && okNum(set[n].add[k], 0))))
    if (!ok) problems.push('set "' + (set && set.name) + '" is broken and was switched off')
    return ok
  })
  let g = cfg.gems
  if (!(g && okNum(g.sockets, 1) && g.sockets <= 6 && g.tiers && g.tiers.length == 3 && g.tiers.every(t => okNum(t.levels, 1)))) {
    problems.push('gem settings are broken, gems were switched off')
    cfg.gems = { sockets: 0, tiers: [{ name: 'Chipped', levels: 1 }, { name: 'Flawless', levels: 1 }, { name: 'Perfect', levels: 1 }], types: {} }
  } else {
    Object.keys(g.types).forEach(t => {
      if (!/^[a-z]+$/.test(t) || !cfg.enchants[g.types[t].enchant]) { problems.push('gem "' + t + '" points at an unknown enchant and was removed'); delete g.types[t] }
    })
  }
  cfg.milestones = (cfg.milestones || []).filter(m => {
    let ok = m && /^[a-z0-9_]+$/.test(m.id) && typeof m.name == 'string' && typeof m.stat == 'string' && okNum(m.goal, 1) &&
      (m.reward || []).every(r => /^[a-z0-9_.-]+:[a-z0-9_/.-]+$/.test(String(r[0])) && okNum(r[1], 1) && r[1] <= 64)
    if (!ok) problems.push('milestone "' + (m && m.id) + '" is broken and was switched off')
    return ok
  })
  let wev = cfg.events
  if (wev && wev.enabled && !(okNum(wev.checkEveryMinutes, 1) && okNum(wev.chancePerCheck, 0) && wev.chancePerCheck <= 1 &&
    okNum(wev.bloodMoon.minutes, 1) && okNum(wev.bloodMoon.dangerBonus, 0) && okNum(wev.bloodMoon.fragmentMult, 1) &&
    okNum(wev.meteorShower.minutes, 1) && okNum(wev.meteorShower.everySeconds, 1) && okNum(wev.meteorShower.radius, 1) && okNum(wev.meteorShower.perWave, 1) &&
    wev.meteorShower.pool && wev.meteorShower.counts && wev.meteorShower.counts.length == 6 &&
    okNum(wev.goldenHour.minutes, 1) && okNum(wev.goldenHour.lootMult, 1))) {
    problems.push('world event settings are broken, events were switched off')
    wev.enabled = false
  }
  let b = cfg.bosses
  if (b && b.enabled && !(okNum(b.minDanger, 0) && okNum(b.chancePerSpawn, 0) && b.chancePerSpawn <= 1 && okNum(b.healthMult, 1) &&
    okNum(b.damageMult, 0) && okNum(b.minionsPerPhase, 0) && b.minionsPerPhase <= 10 && b.titles.length > 0 &&
    b.drops.every(d => okNum(d[1], 0) && okNum(d[2], d[1]) && d[2] <= 64))) {
    problems.push('boss settings are broken, Ascended bosses were switched off')
    b.enabled = false
  }
  let hz = cfg.horde
  if (hz && hz.enabled && !(okNum(hz.extraPerLevel, 0) && okNum(hz.maxExtra, 0) && hz.maxExtra <= 20 && okNum(hz.nearbyCap, 1) && okNum(hz.radius, 1) && okNum(hz.healthMult == null ? 1 : hz.healthMult, 0.01) && okNum(hz.damageMult == null ? 1 : hz.damageMult, 0))) {
    problems.push('horde settings are broken, hordes were switched off')
    hz.enabled = false
  }
  let lt = cfg.loot
  let kinds = { item: 5, gem: 3, tome: 3, condensed: 4, relic: 3 }
  if (lt && lt.enabled && !(okNum(lt.chance, 0) && okNum(lt.chanceScale, 0) && okNum(lt.chanceCap, 0) && lt.chanceCap <= 1 &&
    okNum(lt.bulkScale, 0) && okNum(lt.tierCurve, 0) && okNum(lt.relicLevelScale, 0) && okNum(lt.condensedScale, 0) &&
    okNum(lt.eliteRolls, 0) && okNum(lt.bossRolls, 0) && lt.tiers && lt.tiers.length > 0 &&
    lt.tiers.every(t => typeof t.name == 'string' && okNum(t.weight, 0) && okNum(t.growth, 0) && t.drops && t.drops.length > 0 &&
      t.drops.every(d => kinds[d[0]] == d.length && okNum(d[d.length - 1], 0.0001) &&
        (d[0] != 'item' || (/^[a-z0-9_.-]+:[a-z0-9_/.-]+$/.test(d[1]) && okNum(d[2], 1) && okNum(d[3], d[2]))) &&
        (d[0] != 'gem' || (d[1] >= 1 && d[1] <= 3)) && (d[0] != 'tome' || ['base', 'fusion', 'mythic', 'ultimate'].indexOf(d[1]) >= 0) &&
        (d[0] != 'condensed' || (okNum(d[1], 0) && okNum(d[2], d[1]))) && (d[0] != 'relic' || !!(lt.relics && lt.relics[d[1]])))))) {
    problems.push('loot settings are broken, loot drops were switched off')
    lt.enabled = false
  }
  let f = cfg.fragments
  if (!['eliteChance', 'eliteChanceScale', 'eliteChanceCap', 'normalChanceScale', 'normalChanceCap'].every(k => okNum(f[k], 0) && f[k] <= 1)) {
    problems.push('fragment chances must be between 0 and 1, reset to defaults')
    cfg.fragments = { eliteChance: 0.15, eliteChanceScale: 0.005, eliteChanceCap: 0.75, normalChanceScale: 0.002, normalChanceCap: 0.05 }
  }
  let mf = cfg.mobFood
  let effectOk = e => e && /^[a-z0-9_]+:[a-z0-9_]+$/.test(e[0]) && okNum(e[1], 0) && okNum(e[2], 0)
  if (mf && mf.enabled && !(okNum(mf.lootingBonus, 0) && okNum(mf.cookSeconds, 1) && mf.raw && okNum(mf.raw.hunger, 0) && okNum(mf.raw.saturation, 0) &&
    mf.raw.sickness.every(e => effectOk(e) && okNum(e[3], 0) && e[3] <= 1) && mf.foods && mf.foods.every(fd =>
      /^[a-z0-9_]+$/.test(fd.raw) && /^[a-z0-9_]+$/.test(fd.cooked) && fd.mobs && fd.mobs.length > 0 && okNum(fd.chance, 0) && fd.chance <= 1 &&
      okNum(fd.hunger, 0) && okNum(fd.saturation, 0) && effectOk(fd.effect)))) {
    problems.push('mobFood settings are broken, mob food drops were switched off')
    mf.enabled = false
  }
  let cond = cfg.condition
  if (cond && cond.enabled && !(cond.grades && cond.grades.length > 0 && cond.grades.every(g => typeof g.name == 'string' && okNum(g.weight, 0) &&
    okNum(g.damage, 0) && okNum(g.defense, 0) && okNum(g.wear, 0) && g.wear < 1))) {
    problems.push('condition settings are broken, gear conditions were switched off')
    cond.enabled = false
  }
  let vr = cfg.variants
  if (vr && vr.enabled && !(vr.types && vr.types.length > 0 && vr.types.every(t => /^[a-z_]+$/.test(t.id) && typeof t.name == 'string' &&
    okNum(t.chance, 0) && t.chance <= 1 && okNum(t.xp, 0) && okNum(t.lootRolls, 0) && okNum(t.lootBump, 0) && t.drops &&
    t.drops.every(d => d[0] == 'gem' ? d[1] >= 1 && d[1] <= 3 : okNum(d[1], 0) && okNum(d[2], d[1]))) &&
    okNum(vr.fireSeconds, 0) && okNum(vr.frostSeconds, 0) && okNum(vr.stormBonus, 0))) {
    problems.push('variants settings are broken, rare mob variants were switched off')
    vr.enabled = false
  }
  let ho = cfg.home
  if (ho && ho.enabled && !(okNum(ho.combatSeconds, 0) && okNum(ho.cooldownSeconds, 0))) {
    problems.push('home settings are broken, reset to 10s combat / 30s cooldown')
    cfg.home = { enabled: true, combatSeconds: 10, cooldownSeconds: 30 }
  }
  let hm = cfg.hammer
  let idOk = id => /^[a-z0-9_.-]+:[a-z0-9_/.-]+$/.test(id)
  if (!(hm && okNum(hm.sneakCount, 1) && hm.tools && Object.keys(hm.tools).every(t => idOk(t) && okNum(hm.tools[t].uses, 1) && okNum(hm.tools[t].luck, 0) && okNum(hm.tools[t].cooldown, 0)) &&
    hm.rocks && Object.keys(hm.rocks).every(r => idOk(r) && hm.rocks[r].every(d => idOk(d[0]) && okNum(d[1], 0) && d[1] <= 1 && okNum(d[2], 1))))) {
    problems.push('hammer settings are broken, Rock Hammers smash nothing')
    cfg.hammer = { sneakCount: 8, tools: hm && hm.tools ? hm.tools : {}, rocks: {} }
  }
  return problems
}

global.FA_validate(global.FA).forEach(p => console.error('[Factory Ascension] config: ' + p))

// Condensed Essence blocks can be absorbed like cores: add every tier to essenceValues
// (after the config check, which only guards the core 1 : 9 : 81 values)
for (let tier = 0; tier <= global.FA_condensedTiers(); tier++) global.FA.essenceValues[global.FA_condensedId(tier)] = global.FA_condensedValue(tier)

// ---- Tome catalysts: what goes in the heated mixer with a Blank Ascension Tome, per enchant ----
// Used by the tome recipes (25_fa_tome_recipes.js) and shown in tome tooltips. Every tome has at least one
// catalyst no other tome uses, so the mixer never mixes them up (the test harness checks this).
global.FA_TOME_CATALYSTS = {
  // Damage
  sharpness: ['minecraft:diamond', 'minecraft:flint', 'minecraft:flint'],
  smite: ['minecraft:rotten_flesh', 'minecraft:rotten_flesh', 'kubejs:zombie_brain'],
  bane: ['minecraft:spider_eye', 'kubejs:spider_legs'],
  giant_slayer: ['minecraft:wither_skeleton_skull'],
  hellbane: ['minecraft:blaze_rod', 'kubejs:hoglin_ribs'],
  raidbreaker: ['minecraft:crossbow'],
  impaling: ['minecraft:prismarine_shard', 'minecraft:prismarine_shard'],
  skyhunter: ['minecraft:phantom_membrane', 'minecraft:phantom_membrane'],
  first_strike: ['minecraft:spectral_arrow', 'minecraft:spectral_arrow'],
  executioner: ['minecraft:iron_axe'],
  berserk: ['minecraft:blaze_powder', 'minecraft:blaze_powder'],
  power: ['minecraft:bow'],
  assassin: ['minecraft:ink_sac', 'minecraft:ink_sac'],
  critical: ['minecraft:amethyst_shard', 'minecraft:amethyst_shard'],
  reaper: ['minecraft:wither_rose'],
  cleave: ['minecraft:diamond_axe'],
  // On hit
  lifesteal: ['minecraft:fermented_spider_eye', 'minecraft:redstone', 'minecraft:redstone'],
  frostbite: ['minecraft:blue_ice'],
  enfeeble: ['minecraft:poisonous_potato'],
  withering: ['minecraft:soul_sand', 'minecraft:soul_sand', 'minecraft:coal'],
  venom: ['minecraft:pufferfish'],
  skyward: ['minecraft:shulker_shell'],
  fire_aspect: ['minecraft:fire_charge', 'minecraft:fire_charge'],
  knockback: ['minecraft:piston'],
  second_wind: ['minecraft:honeycomb', 'minecraft:honeycomb'],
  adrenaline: ['minecraft:cocoa_beans', 'minecraft:cocoa_beans'],
  // On kill
  plunder: ['minecraft:emerald', 'minecraft:gold_ingot', 'minecraft:gold_ingot'],
  soul_eater: ['minecraft:soul_lantern'],
  wisdom: ['minecraft:experience_bottle', 'kubejs:roasted_pig_brain'],
  midas: ['minecraft:gold_block'],
  gem_hunter: ['minecraft:emerald_block'],
  // Defense
  protection: ['minecraft:iron_block', 'minecraft:obsidian'],
  fire_ward: ['minecraft:magma_block'],
  featherweight: ['minecraft:slime_block', 'kubejs:chicken_feet'],
  blast_ward: ['minecraft:tnt', 'kubejs:creeper_heart'],
  arrow_ward: ['minecraft:shield'],
  spellward: ['minecraft:lapis_block'],
  wither_ward: ['minecraft:nether_star'],
  bulwark: ['minecraft:chain', 'kubejs:marrow_bone'],
  vitality: ['minecraft:golden_apple', 'kubejs:seared_cow_heart'],
  retribution: ['minecraft:cactus', 'minecraft:quartz', 'minecraft:quartz'],
  evasion: ['minecraft:ender_pearl', 'minecraft:ender_pearl'],
  undying: ['minecraft:totem_of_undying'],
  // Passive
  renewal: ['minecraft:ghast_tear', 'kubejs:sheep_liver'],
  night_sight: ['minecraft:golden_carrot', 'kubejs:fried_pig_eyes'],
  gills: ['minecraft:scute', 'kubejs:squid_tentacle'],
  fireproof: ['minecraft:magma_cream', 'minecraft:magma_cream'],
  industrious: ['minecraft:golden_pickaxe'],
  leaping: ['minecraft:slime_ball', 'kubejs:roasted_rabbit_leg'],
  sustenance: ['minecraft:cake', 'kubejs:grilled_cow_tongue'],
  magnetism: ['minecraft:lodestone'],
  restoration: ['minecraft:anvil'],
  // Movement & utility
  swiftness: ['minecraft:sugar', 'minecraft:sugar', 'minecraft:feather'],
  reach: ['minecraft:spyglass'],
  stride: ['minecraft:scaffolding', 'minecraft:scaffolding'],
  frenzy: ['minecraft:clock'],
  lucky: ['minecraft:rabbit_foot'],
  excavation: ['create:mechanical_drill', 'minecraft:redstone_block'],
  eternal: ['minecraft:netherite_ingot', 'create:precision_mechanism'],
  soulbound: ['minecraft:ender_eye', 'minecraft:echo_shard'],   // remembers its owner
  // Mythic (plus 4 Mythic Fragments each, superheated)
  wings: ['minecraft:elytra'],
  thunderlord: ['minecraft:lightning_rod'],
  black_hole: ['minecraft:end_crystal'],
  meteor: ['create:blaze_cake'],
  chronos: ['minecraft:amethyst_block'],
  phoenix: ['minecraft:respawn_anchor'],
  voidwalker: ['minecraft:popped_chorus_fruit', 'minecraft:popped_chorus_fruit'],
  soul_harvest: ['minecraft:soul_campfire'],
  overkill: ['minecraft:netherite_sword'],
  rampage: ['minecraft:nether_wart_block'],
  vein_miner: ['minecraft:diamond_pickaxe'],
  timber: ['create:mechanical_saw'],
  radiance: ['minecraft:beacon'],
  headhunter: ['minecraft:skeleton_skull'],
  gravity_slam: ['minecraft:crying_obsidian'],
  // Ultimate (plus 2 Ascended Souls and 4 Mythic Fragments each, superheated)
  thunder_waltz: ['minecraft:trident'],
  thousand_cuts: ['minecraft:golden_sword'],
  heaven_splitter: ['minecraft:sticky_piston'],
  dragon_spiral: ['minecraft:dragon_head'],
  void_collapse: ['minecraft:ender_chest'],
  starfall: ['minecraft:firework_rocket'],
  shadow_barrage: ['minecraft:black_dye'],
  frozen_eternity: ['minecraft:packed_ice'],
  spirit_bomb: ['minecraft:glowstone'],
  blade_storm: ['minecraft:iron_sword'],
  eclipse: ['minecraft:daylight_detector'],
  phoenix_dive: ['minecraft:fire_charge']
}
