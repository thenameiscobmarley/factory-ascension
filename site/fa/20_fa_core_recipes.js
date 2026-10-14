// Factory Ascension - Ascension Core recipes.
// Every step runs through Create machines, so the factory (not a crafting table) makes your power.

ServerEvents.recipes(event => {
  let INC = 'kubejs:incomplete_ascension_core'
  let step = (type, extra) => ({
    type: type,
    ingredients: [{ item: INC }].concat(extra ? [extra] : []),
    results: [{ item: INC }]
  })
  let nine = id => {
    let list = []
    for (let i = 0; i < 9; i++) list.push({ item: id })
    return list
  }

  // Core: diamond -> rose quartz -> lava -> press, three loops
  let sequence = [
    step('create:deploying', { item: 'create:polished_rose_quartz' }),
    step('create:filling', { fluid: 'minecraft:lava', amount: 250 }),
    step('create:pressing')
  ]
  // With TFMG installed, each loop also needs a steel ingot
  if (Platform.isLoaded('tfmg') && !Item.of('tfmg:steel_ingot').isEmpty()) {
    sequence.splice(1, 0, step('create:deploying', { item: 'tfmg:steel_ingot' }))
  }

  event.custom({
    type: 'create:sequenced_assembly',
    ingredient: { item: 'minecraft:diamond' },
    transitionalItem: { item: INC },
    sequence: sequence,
    results: [{ item: 'kubejs:ascension_core' }],
    loops: 3
  }).id('kubejs:factory_ascension/ascension_core')

  // Compression happens in a heated basin under a press, so it automates too
  event.custom({
    type: 'create:compacting',
    ingredients: nine('kubejs:ascension_core'),
    results: [{ item: 'kubejs:dense_ascension_core' }],
    heatRequirement: 'heated'
  }).id('kubejs:factory_ascension/dense_ascension_core')

  event.custom({
    type: 'create:compacting',
    ingredients: nine('kubejs:dense_ascension_core'),
    results: [{ item: 'kubejs:ascension_singularity' }],
    heatRequirement: 'superheated'
  }).id('kubejs:factory_ascension/ascension_singularity')

  // Melting: cores + a splash of water -> Liquid Essence (100 mB per Essence by default), for pipes, tanks and
  // Ascension Altars. The water keeps this recipe from matching inside other core recipes (like the Chaos Tome's).
  let mb = global.FA.altar.mbPerEssence
  event.custom({
    type: 'create:mixing',
    ingredients: [{ item: 'kubejs:ascension_core' }, { fluid: 'minecraft:water', amount: 50 }],
    results: [{ fluid: 'kubejs:liquid_essence', amount: mb }],
    heatRequirement: 'heated'
  }).id('kubejs:factory_ascension/melt_ascension_core')
  event.custom({
    type: 'create:mixing',
    ingredients: [{ item: 'kubejs:dense_ascension_core' }, { fluid: 'minecraft:water', amount: 450 }],
    results: [{ fluid: 'kubejs:liquid_essence', amount: 9 * mb }],
    heatRequirement: 'heated'
  }).id('kubejs:factory_ascension/melt_dense_ascension_core')

  // The Ascension Altar
  event.shaped('kubejs:ascension_altar', ['ADA', 'OBO', 'OOO'], {
    A: 'minecraft:amethyst_block', D: 'kubejs:dense_ascension_core', O: 'minecraft:obsidian', B: 'create:brass_casing'
  }).id('kubejs:factory_ascension/ascension_altar')

  // Gems: a chipped gem from a superheated mixer, then compact 4 -> 1 for each tier up
  let gemCatalysts = {
    bloodstone: ['minecraft:redstone', 4], sapphire: ['minecraft:lapis_lazuli', 4], garnet: ['minecraft:nether_wart', 4],
    topaz: ['minecraft:gold_ingot', 4], opal: ['minecraft:quartz', 4], onyx: ['minecraft:coal_block', 4]
  }
  Object.keys(global.FA.gems.types).forEach(type => {
    let c = gemCatalysts[type]
    if (!c) return
    let ingredients = [{ item: 'minecraft:diamond' }, { fluid: 'minecraft:lava', amount: 250 }]
    for (let i = 0; i < c[1]; i++) ingredients.push({ item: c[0] })
    event.custom({
      type: 'create:mixing', ingredients: ingredients,
      results: [{ item: global.FA_gemId(type, 1) }], heatRequirement: 'superheated'
    }).id('kubejs:factory_ascension/gem_' + type + '_1')
    for (let tier = 2; tier <= 3; tier++) {
      let four = []
      for (let i = 0; i < 4; i++) four.push({ item: global.FA_gemId(type, tier - 1) })
      event.custom({
        type: 'create:compacting', ingredients: four,
        results: [{ item: global.FA_gemId(type, tier) }], heatRequirement: tier == 3 ? 'superheated' : 'heated'
      }).id('kubejs:factory_ascension/gem_' + type + '_' + tier)
    }
  })

  // Unpacking, in case you want to spend them in smaller amounts
  event.shapeless(Item.of('kubejs:ascension_core', 9), ['kubejs:dense_ascension_core'])
    .id('kubejs:factory_ascension/unpack_dense_core')
  event.shapeless(Item.of('kubejs:dense_ascension_core', 9), ['kubejs:ascension_singularity'])
    .id('kubejs:factory_ascension/unpack_singularity')
})
