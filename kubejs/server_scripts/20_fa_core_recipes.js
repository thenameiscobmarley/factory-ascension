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

  // Unpacking, in case you want to spend them in smaller amounts
  event.shapeless(Item.of('kubejs:ascension_core', 9), ['kubejs:dense_ascension_core'])
    .id('kubejs:factory_ascension/unpack_dense_core')
  event.shapeless(Item.of('kubejs:dense_ascension_core', 9), ['kubejs:ascension_singularity'])
    .id('kubejs:factory_ascension/unpack_singularity')
})
