// Factory Ascension - tome recipes.
// Blank tome: a deployer presses an Ascension Core into a book.
// Enchant tomes: blank tome + a themed catalyst in a heated mixer.

ServerEvents.recipes(event => {
  event.custom({
    type: 'create:deploying',
    ingredients: [{ item: 'minecraft:book' }, { item: 'kubejs:ascension_core' }],
    results: [{ item: 'kubejs:blank_ascension_tome' }]
  }).id('kubejs:factory_ascension/blank_ascension_tome')

  let catalysts = {
    sharpness: ['minecraft:diamond', 'minecraft:flint', 'minecraft:flint'],
    protection: ['minecraft:iron_block', 'minecraft:obsidian'],
    vitality: ['minecraft:golden_apple', 'minecraft:glistering_melon_slice'],
    lifesteal: ['minecraft:fermented_spider_eye', 'minecraft:redstone', 'minecraft:redstone'],
    plunder: ['minecraft:emerald', 'minecraft:gold_ingot', 'minecraft:gold_ingot'],
    swiftness: ['minecraft:sugar', 'minecraft:sugar', 'minecraft:feather'],
    retribution: ['minecraft:cactus', 'minecraft:quartz', 'minecraft:quartz'],
    excavation: ['create:mechanical_drill', 'minecraft:redstone_block'],
    eternal: ['minecraft:netherite_ingot', 'create:precision_mechanism']
  }

  Object.keys(global.FA.enchants).forEach(id => {
    let items = catalysts[id]
    if (!items) {
      console.error('[Factory Ascension] no tome recipe for enchant "' + id + '"')
      return
    }
    event.custom({
      type: 'create:mixing',
      ingredients: [{ item: 'kubejs:blank_ascension_tome' }].concat(items.map(i => ({ item: i }))),
      results: [{ item: 'kubejs:tome_' + id }],
      heatRequirement: 'heated'
    }).id('kubejs:factory_ascension/tome_' + id)
  })
})
