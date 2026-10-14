// Factory Ascension - JEI.
// Every item, block and the Liquid Essence fluid this pack adds shows up in JEI on its own, and every recipe
// shows under its machine's tab (Create's mixer, press, crusher, deployer, assembly line, crafting table).
// This adds information pages for things that come from machines or the world instead of a recipe,
// and hides the assembly line's half-made item. Only runs when JEI is installed.

if (typeof JEIEvents != 'undefined') {
  JEIEvents.hideItems(event => {
    event.hide('kubejs:incomplete_ascension_core')
  })

  JEIEvents.information(event => {
    let FA = global.FA
    let info = (id, lines) => event.addItem(id, lines)
    let essence = n => global.FA_num(n) + ' Essence'

    // Machines
    info('kubejs:essence_condenser', [
      'Packs Liquid Essence into Condensed Essence Blocks.',
      'Put a tank or basin of Liquid Essence under it and a chest, barrel or Item Vault on top.',
      'Makes up to ' + FA.condensed.blocksPerSecond + ' blocks a second. 1 block = ' + FA.condensed.mbPerBlock + ' mB.'
    ])
    info('kubejs:ascension_altar', [
      'Levels up gear on its own, paid in Liquid Essence.',
      'Put a Create Depot on top (with the gear on it) and a tank or basin of Liquid Essence under it.',
      'Up to ' + FA.altar.maxLevelsPerSecond + ' levels a second, lowest enchant first, at the normal Essence prices (1 Essence = ' + FA.altar.mbPerEssence + ' mB).'
    ])

    // Where things come from when it isn't a recipe
    info(global.FA_condensedId(0), [
      'Made by an Essence Condenser from ' + FA.condensed.mbPerBlock + ' mB of Liquid Essence.',
      'Compact ' + FA.condensed.ratio + ' into the next tier with a Mechanical Press over a Basin, up to ' + global.FA_condensedName(global.FA_condensedTiers()) + '.',
      'Right-click in the air to absorb: ' + essence(global.FA_condensedValue(0)) + '. Sneak to absorb the whole stack.'
    ])
    info('kubejs:mythic_fragment', [
      'Drops from hostile mobs out in the danger zones (more than ' + FA.danger.safeRadius + ' blocks from spawn), when a player gets the kill.',
      'Elites drop them often, other mobs rarely. Ascended bosses always drop several. Blood Moons double the odds.',
      'Mythic tomes need 4 of them.'
    ])
    info('kubejs:ascended_soul', [
      'Only Ascended bosses drop these: rare giant mobs deep in the danger zones (danger ' + FA.bosses.minDanger + '+). Each drops 1 or 2.',
      'Ultimate tomes need 2, plus 4 Mythic Fragments and a catalyst, in a superheated mixer.'
    ])
    info('kubejs:chaos_tome', [
      'Hold gear in your other hand and right-click: rolls a brand-new random enchant (Common to Mythic).',
      'Up to ' + FA.chaos.maxPerItem + ' rolled enchants per item. Sneak on a full item to reroll its newest one.'
    ])

    // Absorbing Essence
    ;['kubejs:ascension_core', 'kubejs:dense_ascension_core', 'kubejs:ascension_singularity'].forEach(id => info(id, [
      'Right-click in the air to absorb: ' + essence(FA.essenceValues[id]) + '. Sneak to absorb the whole stack.',
      'Or melt it into Liquid Essence in a heated mixer (with a splash of water) for Altars and Condensers.'
    ]))

    // Gems
    Object.keys(FA.gems.types).forEach(type => {
      for (let tier = 1; tier <= 3; tier++) {
        info(global.FA_gemId(type, tier), [
          'Hold gear in your other hand and right-click to socket it: +' + global.FA_gemLevels(tier) + ' ' +
            FA.enchants[FA.gems.types[type].enchant].name + ' on that item.',
          'Up to ' + FA.gems.sockets + ' gems per item. Sneak on a full item to replace its newest gem (the old one is destroyed).'
        ])
      }
    })

    // Enchant tomes
    Object.keys(FA.enchants).forEach(id => {
      let e = FA.enchants[id]
      info('kubejs:tome_' + id, [
        'Hold gear in your other hand and right-click to unlock ' + e.name + ' on it.',
        'Level 1: ' + global.FA_describe(id, 1) + '.',
        e.maxLevel ? 'Max level ' + e.maxLevel + '.' : 'No level cap: upgrade it with Essence in /ascension, or with an Ascension Altar.'
      ])
    })
  })
}
