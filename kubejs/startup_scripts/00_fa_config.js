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
    eternal:      { name: 'Eternal', type: 'eternal', maxLevel: 1 }
  },

  // ---- Safety limits (not power caps) ----
  maxExtraDropStacks: 8,       // most extra item stacks one kill can spawn, so loot enchants can't lag the server
  maxCleaveTargets: 16,
  maxUpgradesPerCommand: 100000
}

// Readable numbers: 1234 -> "1,234", 12345678 -> "12.3M", 1e40 -> "1.00e+40"
global.FA_num = n => {
  if (!isFinite(n)) return 'Infinity'
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
global.FA_describe = (id, level) => {
  let e = global.FA.enchants[id]
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
  }
  return ''
}

// Essence cost to go from level L to L+1
global.FA_cost = level => Math.ceil(global.FA.costBase * Math.pow(level + 1, global.FA.costPower))
