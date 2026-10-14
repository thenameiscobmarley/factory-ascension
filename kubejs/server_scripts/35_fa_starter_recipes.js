// priority: 650
// Factory Ascension - Ascendite and starter recipes.
// Ascendite is an early ore (y -48 to 80). It gives:
//   - its own tools and armor (between iron and diamond)
//   - cheap ways into the factory: brass without a Blaze Burner, Precision Mechanisms without sequenced assembly,
//     a Blaze Burner without a Nether trip, early TFMG steel
//   - Ascension Cores without the diamond line: by hand, from shards, or in a heated mixer (no sequencing)
// Later it's renewable: blow Crushed Raw Iron through soul fire (Encased Fan) for Crushed Raw Ascendite.

ServerEvents.recipes(event => {
  let A = global.FA.ascendite
  let id = name => 'kubejs:factory_ascension/' + name
  let many = (item, n) => {
    let list = []
    for (let i = 0; i < n; i++) list.push(item)
    return list
  }

  // ---- Processing ----
  event.smelting('kubejs:ascendite_ingot', 'kubejs:raw_ascendite').xp(0.7).id(id('smelt_raw_ascendite'))
  event.blasting('kubejs:ascendite_ingot', 'kubejs:raw_ascendite').xp(0.7).id(id('blast_raw_ascendite'))
  event.smelting('kubejs:ascendite_ingot', 'kubejs:crushed_raw_ascendite').xp(0.1).id(id('smelt_crushed_ascendite'))
  event.blasting('kubejs:ascendite_ingot', 'kubejs:crushed_raw_ascendite').xp(0.1).id(id('blast_crushed_ascendite'))
  ;['ascendite_ore', 'deepslate_ascendite_ore'].forEach(ore => {
    event.smelting('kubejs:ascendite_ingot', 'kubejs:' + ore).xp(0.7).id(id('smelt_' + ore))
    event.custom({
      type: 'create:crushing',
      ingredients: [{ item: 'kubejs:' + ore }],
      results: [{ item: 'kubejs:crushed_raw_ascendite' }, { item: 'kubejs:crushed_raw_ascendite', chance: 0.75 },
        { item: 'kubejs:ascension_shard', chance: 0.15 }, { item: 'create:experience_nugget', chance: 0.75 }],
      processingTime: 250
    }).id(id('crush_' + ore))
  })
  event.custom({
    type: 'create:crushing',
    ingredients: [{ item: 'kubejs:raw_ascendite' }],
    results: [{ item: 'kubejs:crushed_raw_ascendite' }, { item: 'create:experience_nugget', chance: 0.75 },
      { item: 'kubejs:ascension_shard', chance: 0.1 }],
    processingTime: 200
  }).id(id('crush_raw_ascendite'))
  // Washing (fan through water): nuggets, like Create's own ores
  event.custom({
    type: 'create:splashing',
    ingredients: [{ item: 'kubejs:crushed_raw_ascendite' }],
    results: [{ item: 'kubejs:ascendite_nugget', count: 9 }, { item: 'minecraft:redstone', chance: 0.5 }]
  }).id(id('wash_crushed_ascendite'))
  // Renewable: Crushed Raw Iron (from Common Mineral Clusters) blown through soul fire
  event.custom({
    type: 'create:haunting',
    ingredients: [{ item: 'create:crushed_raw_iron' }],
    results: [{ item: 'kubejs:crushed_raw_ascendite', chance: A.hauntChance }]
  }).id(id('haunt_crushed_iron'))

  // Before you find the ore: alloy an ingot from early Overworld materials
  event.shapeless('kubejs:ascendite_ingot', ['minecraft:iron_ingot', 'minecraft:copper_ingot', 'minecraft:lapis_lazuli', 'minecraft:lapis_lazuli',
    'minecraft:amethyst_shard']).id(id('ascendite_alloy'))

  // Mass production: a Mechanical Press over a Basin squeezes cobblestone into Raw Ascendite
  event.custom({
    type: 'create:compacting',
    ingredients: many({ item: 'minecraft:cobblestone' }, Math.min(9, A.pressCobble)),
    results: [{ item: 'kubejs:raw_ascendite' }]
  }).id(id('press_cobblestone_ascendite'))

  // Storage
  event.shapeless('kubejs:ascendite_block', many('kubejs:ascendite_ingot', 9)).id(id('ascendite_block'))
  event.shapeless(Item.of('kubejs:ascendite_ingot', 9), ['kubejs:ascendite_block']).id(id('unpack_ascendite_block'))
  event.shapeless('kubejs:ascendite_ingot', many('kubejs:ascendite_nugget', 9)).id(id('ascendite_ingot_from_nuggets'))
  event.shapeless(Item.of('kubejs:ascendite_nugget', 9), ['kubejs:ascendite_ingot']).id(id('ascendite_nuggets'))

  // ---- Tools and armor ----
  let I = 'kubejs:ascendite_ingot', S = 'minecraft:stick'
  let gear = {
    sword: [[' I ', ' I ', ' S ']], pickaxe: [['III', ' S ', ' S ']], axe: [['II ', 'IS ', ' S ']], shovel: [[' I ', ' S ', ' S ']],
    hoe: [['II ', ' S ', ' S ']], helmet: [['III', 'I I']], chestplate: [['I I', 'III', 'III']], leggings: [['III', 'I I', 'I I']],
    boots: [['I I', 'I I']]
  }
  // Ascendite and every higher tier (config: gearTiers) craft their gear from their own ingot
  ;['ascendite'].concat(Object.keys(global.FA.gearTiers)).forEach(tier => {
    let ingot = 'kubejs:' + tier + '_ingot'
    Object.keys(gear).forEach(g => {
      // Minecraft rejects a recipe whose key has a symbol the pattern doesn't use, so armor gets no stick
      let key = gear[g][0].join('').indexOf('S') >= 0 ? { I: ingot, S: S } : { I: ingot }
      event.shaped('kubejs:' + tier + '_' + g, gear[g][0], key).id(id(tier + '_' + g))
    })
  })

  // ---- Higher tier ingots, made in the factory ----
  // Voidsteel (between diamond and netherite): heated mixer
  event.custom({
    type: 'create:mixing',
    ingredients: [{ item: 'minecraft:diamond' }, { item: I }, { item: I }, { item: 'minecraft:obsidian' }],
    results: [{ item: 'kubejs:voidsteel_ingot', count: 2 }],
    heatRequirement: 'heated'
  }).id(id('voidsteel_ingot'))
  // Celestium (past netherite): superheated
  event.custom({
    type: 'create:mixing',
    ingredients: [{ item: 'minecraft:netherite_ingot' }, { item: 'kubejs:voidsteel_ingot' }, { item: 'kubejs:voidsteel_ingot' }, { item: 'kubejs:dense_ascension_core' }],
    results: [{ item: 'kubejs:celestium_ingot', count: 2 }],
    heatRequirement: 'superheated'
  }).id(id('celestium_ingot'))
  // Ascendium (endgame): superheated, with Liquid Essence
  event.custom({
    type: 'create:mixing',
    ingredients: [{ item: 'kubejs:celestium_ingot' }, { item: 'kubejs:celestium_ingot' }, { item: 'kubejs:ascension_singularity' },
      { item: 'kubejs:mythic_fragment' }, { item: 'kubejs:mythic_fragment' }, { item: 'kubejs:mythic_fragment' }, { item: 'kubejs:mythic_fragment' },
      { fluid: 'kubejs:liquid_essence', amount: 1000 }],
    results: [{ item: 'kubejs:ascendium_ingot' }],
    heatRequirement: 'superheated'
  }).id(id('ascendium_ingot'))

  // ---- Starting the factory ----
  // Brass by hand, before you have a Blaze Burner
  event.shapeless(Item.of('create:brass_ingot', 2), ['minecraft:copper_ingot', 'create:zinc_ingot', 'kubejs:ascendite_nugget'])
    .id(id('starter_brass'))
  // Andesite Alloy: Ascendite nuggets bind twice as much
  event.shaped(Item.of('create:andesite_alloy', 2), ['AN', 'NA'], { A: 'minecraft:andesite', N: 'kubejs:ascendite_nugget' })
    .id(id('starter_andesite_alloy'))
  // Precision Mechanism without a sequenced-assembly line
  event.shaped('create:precision_mechanism', [' C ', 'GIG', ' C '], { C: 'create:cogwheel', G: 'create:golden_sheet', I: I })
    .id(id('starter_precision_mechanism'))
  // A Blaze Burner without catching a blaze
  event.shapeless('create:blaze_burner', ['create:empty_blaze_burner', 'kubejs:ascendite_block', 'minecraft:magma_block'])
    .id(id('starter_blaze_burner'))
  // TFMG steel in a heated mixer, before you've built its blast furnace (the core recipe needs steel with TFMG)
  if (Platform.isLoaded('tfmg') && !Item.of('tfmg:steel_ingot').isEmpty()) {
    event.custom({
      type: 'create:mixing',
      ingredients: [{ item: 'minecraft:iron_ingot' }, { item: 'minecraft:coal' }, { item: 'kubejs:ascendite_nugget' }],
      results: [{ item: 'tfmg:steel_ingot' }],
      heatRequirement: 'heated'
    }).id(id('starter_steel'))
  }

  // Polishing Cloth: rerolls a piece of gear's condition, keeping the better grade
  event.shapeless(Item.of('kubejs:polishing_cloth', 2), ['minecraft:white_wool', 'kubejs:ascendite_nugget', 'kubejs:ascendite_nugget', 'minecraft:slime_ball'])
    .id(id('polishing_cloth'))

  // A blaze rod without the Nether: lava, a stick, redstone and gold (the bucket comes back)
  event.shapeless('minecraft:blaze_rod', ['minecraft:lava_bucket', 'minecraft:stick', 'minecraft:redstone', 'minecraft:redstone', 'minecraft:gold_ingot'])
    .id(id('starter_blaze_rod'))

  // Waystones (if installed): no ender pearls, emeralds or obsidian needed
  if (Platform.isLoaded('waystones')) {
    event.shaped('waystones:warp_stone', ['DAD', 'LRL', 'DAD'], { D: 'minecraft:amethyst_shard', A: I, L: 'minecraft:lapis_lazuli', R: 'minecraft:redstone' })
      .id(id('starter_warp_stone'))
    event.shaped('waystones:waystone', [' S ', 'SWS', 'CCC'], { S: 'minecraft:stone_bricks', W: 'waystones:warp_stone', C: 'minecraft:cobbled_deepslate' })
      .id(id('starter_waystone'))
    event.shapeless(Item.of('waystones:warp_dust', 4), ['minecraft:amethyst_shard', 'kubejs:ascendite_nugget']).id(id('starter_warp_dust'))
    event.shaped(Item.of('waystones:warp_scroll', 3), ['GDG', 'GNG', 'PPP'], { G: 'minecraft:gold_nugget', D: 'minecraft:purple_dye', N: 'kubejs:ascendite_nugget', P: 'minecraft:paper' })
      .id(id('starter_warp_scroll'))
  }

  // ---- More ways to make Ascension Cores (none need sequenced assembly) ----
  // By hand, from day one
  event.shaped('kubejs:ascension_core', ['RIR', 'IQI', 'RIR'], { R: 'minecraft:redstone', I: I, Q: 'minecraft:lapis_lazuli' })
    .id(id('core_by_hand'))
  // Day-one core from things you find in the first caves (no Ascendite needed)
  event.shaped('kubejs:ascension_core', ['ARA', 'RGR', 'ARA'], { A: 'minecraft:amethyst_shard', R: 'minecraft:redstone', G: 'minecraft:gold_ingot' })
    .id(id('core_from_amethyst'))
  // From Ascension Shards (mined from Ascendite ore, or crushed out of it)
  event.shapeless('kubejs:ascension_core', many('kubejs:ascension_shard', A.shardsPerCore)).id(id('core_from_shards'))
  // Automated, in a heated mixer: cheaper than the hand recipe once the factory runs
  event.custom({
    type: 'create:mixing',
    ingredients: [{ item: I }, { item: I }, { item: 'minecraft:redstone' }, { fluid: 'minecraft:lava', amount: 250 }],
    results: [{ item: 'kubejs:ascension_core' }],
    heatRequirement: 'heated'
  }).id(id('core_mixing'))
  // Shards press into a core too (Mechanical Press + Basin, no heat), so a crushing line can feed it
  event.custom({
    type: 'create:compacting',
    ingredients: many({ item: 'kubejs:ascension_shard' }, A.shardsPerCore),
    results: [{ item: 'kubejs:ascension_core' }]
  }).id(id('core_compacting'))
})
