// Factory Ascension - tooltips.

let FA_COLORS = {
  sharpness: 'red', protection: 'aqua', vitality: 'green', lifesteal: 'darkRed', plunder: 'yellow',
  swiftness: 'white', retribution: 'gold', excavation: 'blue', eternal: 'lightPurple'
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
      let name = global.FA.enchants[id].name + (global.FA.enchants[id].maxLevel == 1 ? '' : ' ' + global.FA_num(level))
      text.add(line++, Text[FA_COLORS[id] || 'gold'](name).append(Text.darkGray('  ' + global.FA_describe(id, level))))
    })
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
      Text[FA_COLORS[id] || 'gold']('Level 1: ' + global.FA_describe(id, 1)),
      Text.gray('Hold gear in your other hand and right-click to unlock'),
      Text.darkGray(e.maxLevel == 1 ? 'One-time enchant' : 'Then upgrade it with Essence: /ascension (no level cap)')
    ])
  })

  event.add('kubejs:incomplete_ascension_core', Text.darkGray('Still on the assembly line'))
  event.add('kubejs:common_mineral_cluster', Text.gray('Crush for iron, copper, zinc and gold'))
  event.add('kubejs:gem_mineral_cluster', Text.gray('Crush for redstone, lapis, diamonds and emeralds'))
  event.add('kubejs:exotic_mineral_cluster', Text.gray('Crush for modded metals (Mekanism, TFMG)'))
})
