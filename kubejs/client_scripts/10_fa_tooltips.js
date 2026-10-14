// Factory Ascension - tooltips.

// Tooltip color for each kind of enchant
let FA_TYPE_COLORS = {
  damage: 'red', critical: 'red', reaper: 'darkRed', cleave: 'red',
  lifesteal: 'darkRed', hit_effect: 'lightPurple', self_effect: 'lightPurple', ignite: 'gold', hit_heal: 'green',
  plunder: 'yellow', kill_heal: 'green', xp: 'yellow', drop: 'yellow',
  resist: 'aqua', vitality: 'green', retribution: 'gold', dodge: 'aqua', undying: 'gold',
  regen: 'green', passive: 'green', magnet: 'blue', repair: 'blue',
  attribute: 'white', mining: 'blue', eternal: 'lightPurple',
  flight: 'gold', chain_lightning: 'gold', gravity_well: 'gold', death_nova: 'gold', time_stop: 'gold', phoenix: 'gold',
  projectile_teleport: 'gold', souls: 'gold', overkill: 'gold', rampage: 'gold', vein: 'gold', aura: 'gold', heads: 'gold', slam: 'gold'
}

let FA_TIP_RARITY = { gray: 'gray', aqua: 'aqua', light_purple: 'lightPurple', gold: 'gold', red: 'red' }

function faColor(id) {
  return Text[FA_TYPE_COLORS[global.FA.enchants[id].type] || 'gold']
}

ItemEvents.tooltip(event => {
  // Ascended enchants on any item, listed right under its name like vanilla enchants
  event.addAdvanced(Ingredient.all, (item, advanced, text) => {
    let tag = item.getTag()
    if (!tag || !tag.contains('FactoryAscension')) return
    let data = tag.getCompound('FactoryAscension')
    let line = 1
    Object.keys(global.FA.enchants).forEach(id => {
      let level = data.getInt(id)
      if (level <= 0) return
      let e = global.FA.enchants[id]
      let name = e.name + (e.maxLevel == 1 ? '' : ' ' + global.FA_num(level))
      let label = e.mythic ? Text.lightPurple('\u2726 ').append(Text.gold(name)) : faColor(id)(name)
      text.add(line++, label.append(Text.darkGray('  ' + global.FA_describe(id, level))))
    })
    // Rolled (Chaos Tome) enchants: name in its rarity color, then each effect
    if (data.contains('rolled')) {
      let rolled = data.getCompound('rolled')
      for (let i = 0; i < global.FA.chaos.maxPerItem; i++) {
        if (!rolled.contains(String(i))) continue
        let slot = rolled.getCompound(String(i))
        let lvl = slot.getInt('level')
        if (lvl <= 0) continue
        let roll = global.FA_rollEnchant(slot.getInt('seed'))
        let R = global.FA.chaos.rarities[roll.rarity]
        text.add(line++, Text[FA_TIP_RARITY[R.color] || 'white']('\u2742 ' + roll.name + ' ' + global.FA_num(lvl)).append(Text.darkGray('  ' + R.name)))
        roll.affixes.forEach(def => text.add(line++, Text.darkGray('    ' + global.FA_describeDef(def, lvl))))
      }
    }
    let souls = data.getInt('souls')
    if (souls > 0) text.add(line++, Text.lightPurple('  Souls: ' + global.FA_num(souls)))
  })

  Object.keys(global.FA.essenceValues).forEach(id => {
    event.add(id, [
      Text.lightPurple('Right-click: +' + global.FA.essenceValues[id] + ' Essence'),
      Text.darkGray('Sneak + right-click absorbs the whole stack'),
      Text.darkGray('/ascension shows your Essence')
    ])
  })

  event.add('kubejs:blank_ascension_tome', Text.gray('Mix with a catalyst in a heated basin to make an enchant tome'))
  Object.keys(global.FA.enchants).forEach(id => {
    let e = global.FA.enchants[id]
    event.add('kubejs:tome_' + id, [
      e.mythic ? Text.lightPurple('\u2726 MYTHIC').bold(true) : Text.darkGray('Ascended'),
      faColor(id)('Level 1: ' + global.FA_describe(id, 1)),
      Text.gray('Hold gear in your other hand and right-click to unlock'),
      Text.darkGray(e.maxLevel == 1 ? 'One-time enchant'
        : e.maxLevel ? 'Then upgrade it with Essence: /ascension (max level ' + e.maxLevel + ')'
          : 'Then upgrade it with Essence: /ascension (no level cap)')
    ])
  })

  let odds = global.FA.chaos.rarities.map(R => R.name + ' ' + R.weight + '%').join(', ')
  event.add('kubejs:chaos_tome', [
    Text.lightPurple('Rolls a random enchant onto the gear in your other hand'),
    Text.gray('Rarity odds: ' + odds),
    Text.darkGray('Up to ' + global.FA.chaos.maxPerItem + ' per item. Sneak-use on a full item to reroll its newest.'),
    Text.darkGray('Upgrade rolled enchants with Essence: /ascension')
  ])
  event.add('kubejs:mythic_fragment', [
    Text.lightPurple('Drops from mobs out in the danger zones, mostly Elites'),
    Text.gray('4 of them + a catalyst make a Mythic tome (superheated mixer)')
  ])
  event.add('kubejs:incomplete_ascension_core', Text.darkGray('Still on the assembly line'))
  event.add('kubejs:common_mineral_cluster', Text.gray('Crush for iron, copper, zinc and gold'))
  event.add('kubejs:gem_mineral_cluster', Text.gray('Crush for redstone, lapis, diamonds and emeralds'))
  event.add('kubejs:exotic_mineral_cluster', Text.gray('Crush for modded metals (Mekanism, TFMG)'))
})
