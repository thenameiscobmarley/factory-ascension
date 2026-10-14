// priority: 900
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
  projectile_teleport: 'gold', souls: 'gold', overkill: 'gold', rampage: 'gold', vein: 'gold', aura: 'gold', heads: 'gold', slam: 'gold',
  fusion: 'darkAqua', ultimate: 'red'
}

let FA_TIP_RARITY = { gray: 'gray', aqua: 'aqua', light_purple: 'lightPurple', gold: 'gold', red: 'red' }

function faColor(id) {
  return Text[FA_TYPE_COLORS[global.FA.enchants[id].type] || 'gold']
}

ItemEvents.tooltip(event => {
  // Ascended enchants on any item, listed right under its name like vanilla enchants
  event.addAdvanced(Ingredient.all, (item, advanced, text) => {
    let tag = item.nbt
    if (!tag || !tag.contains('FactoryAscension')) return
    let data = tag.getCompound('FactoryAscension')
    let line = 1
    let relic = data.getInt('relic')
    if (relic > 0) text.add(line++, relic == 2 ? Text.red('\u2739 MYTHIC RELIC').bold(true) : Text.gold('\u2605 LEGENDARY RELIC').bold(true))
    let stars = data.getInt('stars')
    if (stars > 0) text.add(line++, Text.yellow('\u2605'.repeat(Math.min(stars, 10)) + (stars > 10 ? ' x' + stars : '') + '  ')
      .append(Text.darkGray('levels count x' + global.FA_starMult(stars).toFixed(2))))
    Object.keys(global.FA.enchants).forEach(id => {
      let level = data.getInt(id)
      if (level <= 0) return
      let e = global.FA.enchants[id]
      let name = e.name + (e.maxLevel == 1 ? '' : ' ' + global.FA_num(level))
      let label = e.ultimate ? Text.red('\u2604 ').append(Text.gold(name).bold(true)) : e.mythic ? Text.lightPurple('\u2726 ').append(Text.gold(name)) : faColor(id)(name)
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
    // Gems
    if (data.contains('gems')) {
      let gems = data.getCompound('gems')
      let G = global.FA.gems
      for (let i = 0; i < G.sockets; i++) {
        if (!gems.contains(String(i))) continue
        let g = gems.getCompound(String(i))
        let type = G.types[String(g.getString('type'))]
        let tier = g.getInt('tier')
        if (!type || tier < 1 || tier > 3) continue
        text.add(line++, Text.aqua('\u25c6 ' + G.tiers[tier - 1].name + ' ' + type.name).append(Text.darkGray('  +' + global.FA_gemLevels(tier) + ' ' + global.FA.enchants[type.enchant].name)))
      }
    }
    // Gear sets this item belongs to
    global.FA.sets.forEach(set => {
      if (data.getInt(set.key) > 0) text.add(line++, Text.darkAqua('\u29bf ' + set.name + ' set ').append(Text.darkGray('(2 pieces: x' + set[2].mult + ' ' + global.FA.enchants[set.key].name + ', 4 pieces: more)')))
    })
    let souls = data.getInt('souls')
    if (souls > 0) text.add(line++, Text.lightPurple('  Souls: ' + global.FA_num(souls)))
  })

  Object.keys(global.FA.essenceValues).forEach(id => {
    event.add(id, [
      Text.lightPurple('Right-click in the air: +' + global.FA_num(global.FA.essenceValues[id]) + ' Essence'),
      Text.darkGray('Sneak + right-click absorbs the whole stack'),
      Text.darkGray('/ascension shows your Essence')
    ])
  })

  event.add('kubejs:blank_ascension_tome', Text.gray('Mix with a catalyst in a heated basin to make an enchant tome'))
  Object.keys(global.FA.enchants).forEach(id => {
    let e = global.FA.enchants[id]
    event.add('kubejs:tome_' + id, [
      e.ultimate ? Text.red('\u2604 ULTIMATE').bold(true) : e.mythic ? Text.lightPurple('\u2726 MYTHIC').bold(true) : e.type == 'fusion'
        ? Text.darkAqua('\u2697 FUSION of ' + global.FA.enchants[e.parts[0]].name + ' + ' + global.FA.enchants[e.parts[1]].name) : Text.darkGray('Ascended'),
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
  let G = global.FA.gems
  Object.keys(G.types).forEach(type => {
    for (let tier = 1; tier <= 3; tier++) {
      event.add(global.FA_gemId(type, tier), [
        Text.aqua('+' + global.FA_gemLevels(tier) + ' ' + global.FA.enchants[G.types[type].enchant].name + ' when socketed'),
        Text.gray('Right-click with gear in your other hand (up to ' + G.sockets + ' gems per item)'),
        Text.darkGray(tier < 3 ? 'Compact 4 into one ' + G.tiers[tier].name + ' ' + G.types[type].name : 'The best gem there is')
      ])
    }
  })
  // Condensed Essence: how much is packed in, shown in buckets too
  for (let tier = 0; tier <= global.FA_condensedTiers(); tier++) {
    let buckets = Math.pow(global.FA.condensed.ratio, tier) * global.FA.condensed.mbPerBlock / 1000
    event.add(global.FA_condensedId(tier), [
      Text.darkGray('Holds ' + global.FA_num(buckets) + ' buckets of Liquid Essence'),
      Text.darkGray(tier < global.FA_condensedTiers() ? global.FA.condensed.ratio + ' of these compact into ' + global.FA_condensedName(tier + 1) : 'The densest block there is')
    ])
  }
  event.add('kubejs:essence_condenser', [
    Text.lightPurple('Packs Liquid Essence into Condensed Essence Blocks'),
    Text.gray('Liquid Essence tank or basin below, chest or vault on top'),
    Text.darkGray('Up to ' + global.FA.condensed.blocksPerSecond + ' blocks a second')
  ])
  event.add('kubejs:ascension_altar', [
    Text.lightPurple('Levels up gear on its own'),
    Text.gray('Create Depot on top, Liquid Essence tank or basin below'),
    Text.darkGray('Up to ' + global.FA.altar.maxLevelsPerSecond + ' levels a second, lowest enchant first')
  ])
  event.add('kubejs:liquid_essence_bucket', [
    Text.lightPurple('Sneak + right-click to drink: +' + (1000 / global.FA.altar.mbPerEssence) + ' Essence'),
    Text.darkGray('Or pipe it into an Ascension Altar')
  ])
  event.add('kubejs:ascended_soul', [
    Text.red('Torn from an Ascended boss'),
    Text.gray('2 of them + 4 Mythic Fragments + a catalyst make an Ultimate tome (superheated mixer)')
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
