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
  // Defaults: levels 1-20 cost 1 each, level 3000 costs 150, all the way to 3000 is about 226K Essence.
  costBase: 0.05,
  costPower: 1,

  // ---- Ascended enchants ----
  // Levels have no cap (except Eternal). Enchants count from every equipped item:
  // both hands and all four armor slots, so you can mix and match onto anything.
  enchants: {
    sharpness:   { name: 'Ascended Sharpness',  perLevel: 0.10 }, // damage dealt x(1 + 0.10 * L)
    protection:  { name: 'Ascended Protection', perLevel: 0.04 }, // damage taken / (1 + 0.04 * L), never zero
    vitality:    { name: 'Vitality',            perLevel: 2 },    // +2 hidden health per level (no extra hearts on screen)
    lifesteal:   { name: 'Lifesteal',           perLevel: 0.01 }, // heal 1% of damage dealt per level
    plunder:     { name: 'Plunder',             perLevel: 0.05 }, // mob drops x(1 + 0.05 * L)
    swiftness:   { name: 'Swiftness',           perLevel: 0.01 }, // +1% move speed per level, up to speedCap
    retribution: { name: 'Retribution',         perLevel: 0.05 }, // reflect 5% of damage taken per level
    excavation:  { name: 'Excavation',          perLevel: 0.25 }, // mining speed x(1 + 0.25 * L)
    eternal:     { name: 'Eternal',             maxLevel: 1 }     // item never breaks
  },

  // ---- Safety limits (not power caps) ----
  speedCap: 0.5,               // +50% move speed max, faster outruns chunk loading
  maxExtraDropStacks: 8,       // most extra item stacks one kill can spawn, so Plunder can't lag the server
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

// Short effect text for an enchant at a level, shared by tooltips and the /ascension menu.
global.FA_describe = (id, level) => {
  let e = global.FA.enchants[id]
  let x = n => 'x' + (n < 100 ? n.toFixed(2) : global.FA_num(n))
  if (id == 'sharpness') return x(1 + e.perLevel * level) + ' damage'
  if (id == 'protection') return '-' + (100 - 100 / (1 + e.perLevel * level)).toFixed(1) + '% damage taken'
  if (id == 'vitality') return '+' + global.FA_num(e.perLevel * level) + ' hidden health'
  if (id == 'lifesteal') return 'heal ' + Math.round(e.perLevel * level * 100) + '% of damage dealt'
  if (id == 'plunder') return x(1 + e.perLevel * level) + ' mob loot'
  if (id == 'swiftness') return '+' + Math.round(Math.min(global.FA.speedCap, e.perLevel * level) * 100) + '% speed'
  if (id == 'retribution') return 'reflect ' + Math.round(e.perLevel * level * 100) + '% of damage taken'
  if (id == 'excavation') return x(1 + e.perLevel * level) + ' mining speed'
  if (id == 'eternal') return 'never breaks'
  return ''
}

// Essence cost to go from level L to L+1
global.FA_cost = level => Math.ceil(global.FA.costBase * Math.pow(level + 1, global.FA.costPower))
