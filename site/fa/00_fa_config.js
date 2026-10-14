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
    gravity_slam: { name: 'Gravity Slam', type: 'slam', mythic: true, perLevel: 1 }
  },

  // ---- Mythic Fragments: drop from danger-zone mobs, needed for Mythic tomes ----
  fragments: {
    eliteChance: 0.15,          // Elites: 15% + 0.5% per danger level, max 75%
    eliteChancePerLevel: 0.005,
    eliteChanceCap: 0.75,
    normalChancePerLevel: 0.002, // other danger-zone mobs: 0.2% per danger level, max 5%
    normalChanceCap: 0.05
  },

  // ---- Danger: the farther from spawn, the nastier the mobs (and the better the loot) ----
  // Danger level = (distance from world spawn - safeRadius) / blocksPerLevel, no cap.
  // Only hostile, non-boss mobs scale. Being AFK changes nothing: it's all about where you are.
  danger: {
    enabled: true,
    safeRadius: 1000,          // normal mobs within this many blocks of world spawn
    blocksPerLevel: 500,       // +1 danger level every 500 blocks past that
    netherScale: 8,            // Nether distance counts x8, like Nether travel
    netherBonus: 3,            // the Nether is always at least this dangerous
    endBonus: 6,               // the End too (the main island is inside safeRadius of 0,0)
    healthPerLevel: 0.25,      // mobs take damage / (1 + 0.25 * level), like having more health
    damagePerLevel: 0.15,      // mobs deal x(1 + 0.15 * level) damage
    lootPerLevel: 0.1,         // mob drops x(1 + 0.1 * level)
    xpPerLevel: 0.2,           // mob XP x(1 + 0.2 * level)
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
      { from: 1, name: 'The Frontier', color: 'yellow' },
      { from: 5, name: 'The Wilds', color: 'gold' },
      { from: 10, name: 'Dread Reaches', color: 'red' },
      { from: 20, name: 'The Abyss', color: 'dark_red' },
      { from: 40, name: 'Nightmare', color: 'dark_purple' },
      { from: 80, name: 'The Void Beyond', color: 'dark_gray' }
    ]
  },

  // ---- Chaos Tomes: randomly generated enchants ----
  // A Chaos Tome rolls a brand-new enchant onto gear: a rarity, a generated name and 1-3 random effects.
  // Rolls are made from a seed, so the same seed always gives the same enchant (in game and on the website).
  chaos: {
    maxPerItem: 3,             // rolled enchants one item can hold (sneak-use a Chaos Tome on a full item to reroll its newest)
    rarities: [
      { name: 'Common', weight: 50, power: 1, affixes: 1, costMult: 1, color: 'gray' },
      { name: 'Rare', weight: 28, power: 1.5, affixes: 2, costMult: 1.5, color: 'aqua' },
      { name: 'Epic', weight: 15, power: 2.2, affixes: 2, costMult: 2.5, color: 'light_purple' },
      { name: 'Legendary', weight: 6, power: 3.2, affixes: 3, costMult: 4, color: 'gold' },
      { name: 'Mythic', weight: 1, power: 5, affixes: 3, costMult: 8, color: 'red' }
    ]
  },

  // ---- Safety limits (not power caps) ----
  maxExtraDropStacks: 8,       // most extra item stacks one kill can spawn, so loot enchants can't lag the server
  maxCleaveTargets: 16,
  maxUpgradesPerCommand: 100000
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
    case 'slam': return 'landing smashes nearby enemies for ' + num(e.perLevel * level) + ' damage per block fallen, no fall damage'
  }
  return ''
}

// ---- Danger helpers ----

// Danger level at a position in a level (world). Overworld: distance from world spawn.
// Nether: distance from 0,0 x netherScale. Everything else: distance from 0,0.
global.FA_dangerLevel = (level, x, z) => {
  let d = global.FA.danger
  if (!d.enabled) return 0
  let dim = String(level.dimension().location())
  let dist
  let bonus = 0
  if (dim == 'minecraft:overworld') {
    let spawn = level.getSharedSpawnPos()
    dist = Math.sqrt(Math.pow(x - spawn.getX(), 2) + Math.pow(z - spawn.getZ(), 2))
  } else {
    dist = Math.sqrt(x * x + z * z)
    if (dim == 'minecraft:the_nether') {
      dist *= d.netherScale
      bonus = d.netherBonus
    } else if (dim == 'minecraft:the_end') {
      bonus = d.endBonus
    }
  }
  let danger = Math.max(0, Math.floor((dist - d.safeRadius) / d.blocksPerLevel) + 1) + bonus
  if (dist < d.safeRadius) danger = bonus
  return d.maxLevel > 0 ? Math.min(d.maxLevel, danger) : danger
}

global.FA_zone = danger => {
  let zones = global.FA.danger.zones
  let zone = zones[0]
  zones.forEach(z => { if (danger >= z.from) zone = z })
  return zone
}

// Multipliers for a mob at a danger level (elite = the rare named kind)
global.FA_dangerMult = (danger, elite) => {
  let d = global.FA.danger
  return {
    health: (1 + d.healthPerLevel * danger) * (elite ? d.eliteHealth : 1),
    damage: (1 + d.damagePerLevel * danger) * (elite ? d.eliteDamage : 1),
    loot: (1 + d.lootPerLevel * danger) * (elite ? d.eliteLoot : 1),
    xp: (1 + d.xpPerLevel * danger) * (elite ? d.eliteLoot : 1)
  }
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
  return elite ? Math.min(f.eliteChanceCap, f.eliteChance + f.eliteChancePerLevel * danger)
    : Math.min(f.normalChanceCap, f.normalChancePerLevel * danger)
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

// Essence cost for a rolled enchant from level L to L+1
global.FA_rollCost = (level, rarity) => Math.ceil(global.FA_cost(level) * global.FA.chaos.rarities[rarity].costMult)

// Essence cost to go from level L to L+1 (Mythic enchants cost more)
global.FA_cost = (level, id) => {
  let e = id ? global.FA.enchants[id] : null
  let mult = e && e.mythic ? global.FA.mythicCostMultiplier : 1
  return Math.ceil(global.FA.costBase * Math.pow(level + 1, global.FA.costPower) * mult)
}

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
  mining: ['perLevel'], eternal: [], flight: [], chain_lightning: ['perLevel', 'radius'], gravity_well: ['perLevel', 'base', 'cap'],
  death_nova: ['perLevel'], time_stop: [], phoenix: ['cooldown', 'perLevel', 'minCooldown'], projectile_teleport: [],
  souls: ['perLevel'], overkill: ['perLevel', 'radius'], rampage: ['perLevel', 'maxStacks', 'seconds'], vein: ['perLevel', 'cap'],
  aura: ['perLevel'], heads: ['perLevel'], slam: ['perLevel']
}
let FA_ATTRS = ['movement_speed', 'attack_speed', 'attack_knockback', 'luck', 'block_reach', 'entity_reach', 'step_height']
let FA_VS = ['all', 'fire', 'fall', 'explosion', 'projectile', 'magic', 'wither', 'melee']

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
  if (!okNum(cfg.mythicCostMultiplier, 1)) { problems.push('mythicCostMultiplier reset to 5'); cfg.mythicCostMultiplier = 5 }
  ;['maxExtraDropStacks', 'maxCleaveTargets', 'maxUpgradesPerCommand'].forEach(f => {
    if (!okNum(cfg[f], 1)) { problems.push(f + ' reset to 8'); cfg[f] = 8 }
  })

  let d = cfg.danger
  let dangerOk = ['safeRadius', 'netherScale', 'netherBonus', 'endBonus', 'healthPerLevel', 'damagePerLevel', 'lootPerLevel',
    'xpPerLevel', 'eliteChancePerLevel', 'eliteChanceCap', 'maxLevel'].every(f => need(d[f], 'danger.' + f, 0)) &&
    need(d.blocksPerLevel, 'danger.blocksPerLevel', 1) && need(d.eliteHealth, 'danger.eliteHealth', 1) &&
    need(d.eliteDamage, 'danger.eliteDamage', 0) && need(d.eliteLoot, 'danger.eliteLoot', 0) &&
    d.zones && d.zones.length > 0 && d.zones[0].from == 0
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
  let f = cfg.fragments
  if (!['eliteChance', 'eliteChancePerLevel', 'eliteChanceCap', 'normalChancePerLevel', 'normalChanceCap'].every(k => okNum(f[k], 0) && f[k] <= 1)) {
    problems.push('fragment chances must be between 0 and 1, reset to defaults')
    cfg.fragments = { eliteChance: 0.15, eliteChancePerLevel: 0.005, eliteChanceCap: 0.75, normalChancePerLevel: 0.002, normalChanceCap: 0.05 }
  }
  return problems
}

global.FA_validate(global.FA).forEach(p => console.error('[Factory Ascension] config: ' + p))
