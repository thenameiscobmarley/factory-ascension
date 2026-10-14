// priority: 750
// Factory Ascension - tome recipes.
// Blank tome: a deployer presses an Ascension Core into a book.
// Enchant tomes: blank tome + a themed catalyst in a heated mixer.
// Mythic tomes: also 4 Mythic Fragments, superheated.
// Fusion tomes: the two parent tomes + an Ascension Core, heated.
// Every tome has at least one catalyst no other tome uses, so the mixer never mixes them up.

ServerEvents.recipes(event => {
  event.custom({
    type: 'create:deploying',
    ingredients: [{ item: 'minecraft:book' }, { item: 'kubejs:ascension_core' }],
    results: [{ item: 'kubejs:blank_ascension_tome' }]
  }).id('kubejs:factory_ascension/blank_ascension_tome')

  // The catalyst list lives in 00_fa_config.js (global.FA_TOME_CATALYSTS), so tooltips can show it too
  let catalysts = global.FA_TOME_CATALYSTS


  // Chaos Tome: blank tome + 4 cores + an eye of ender, superheated
  event.custom({
    type: 'create:mixing',
    ingredients: [{ item: 'kubejs:blank_ascension_tome' }, { item: 'kubejs:ascension_core' }, { item: 'kubejs:ascension_core' },
      { item: 'kubejs:ascension_core' }, { item: 'kubejs:ascension_core' }, { item: 'minecraft:ender_eye' }],
    results: [{ item: 'kubejs:chaos_tome' }],
    heatRequirement: 'superheated'
  }).id('kubejs:factory_ascension/chaos_tome')

  Object.keys(global.FA.enchants).forEach(id => {
    let e = global.FA.enchants[id]
    if (e.type == 'fusion') {
      event.custom({
        type: 'create:mixing',
        ingredients: [{ item: 'kubejs:tome_' + e.parts[0] }, { item: 'kubejs:tome_' + e.parts[1] }, { item: 'kubejs:ascension_core' }],
        results: [{ item: 'kubejs:tome_' + id }],
        heatRequirement: 'heated'
      }).id('kubejs:factory_ascension/tome_' + id)
      return
    }
    let items = catalysts[id]
    if (!items) {
      console.error('[Factory Ascension] no tome recipe for enchant "' + id + '"')
      return
    }
    let mythic = global.FA.enchants[id].mythic || global.FA.enchants[id].ultimate
    if (mythic) items = items.concat(['kubejs:mythic_fragment', 'kubejs:mythic_fragment', 'kubejs:mythic_fragment', 'kubejs:mythic_fragment'])
    if (global.FA.enchants[id].ultimate) items = items.concat(['kubejs:ascended_soul', 'kubejs:ascended_soul'])
    event.custom({
      type: 'create:mixing',
      ingredients: [{ item: 'kubejs:blank_ascension_tome' }].concat(items.map(i => ({ item: i }))),
      results: [{ item: 'kubejs:tome_' + id }],
      heatRequirement: mythic ? 'superheated' : 'heated'
    }).id('kubejs:factory_ascension/tome_' + id)
  })
})
