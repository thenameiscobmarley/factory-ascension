// priority: 700
// Factory Ascension - renewable ore line.
// Cobblestone generators + Create drills give endless cobblestone, gravel and blackstone.
// These recipes turn them into every ore the pack needs, so materials come from
// factory size, not from hunting ore veins.
//
//   gravel x4     + 100mb lava, heated      -> Common Mineral Cluster -> crush -> iron, copper, zinc, gold
//   cobblestone x4 + quartz + 250mb lava, superheated -> Gem Mineral Cluster -> crush -> redstone, lapis, diamond, emerald
//   blackstone x4 + redstone + 250mb lava, superheated -> Exotic Mineral Cluster -> crush -> modded metals
//
// Crushed ores then go through Create's normal washing (fans + water) for nuggets and bonus output.

ServerEvents.recipes(event => {
  let lava = amount => ({ fluid: 'minecraft:lava', amount: amount })
  let many = (id, n) => {
    let list = []
    for (let i = 0; i < n; i++) list.push({ item: id })
    return list
  }
  let exists = id => {
    try {
      return !Item.of(id).isEmpty()
    } catch (err) {
      return false
    }
  }
  let mix = (name, ingredients, result, heat) => event.custom({
    type: 'create:mixing',
    ingredients: ingredients,
    results: [{ item: result }],
    heatRequirement: heat
  }).id('kubejs:factory_ascension/' + name)
  // Create allows at most 4 crushing outputs
  let crush = (name, input, results) => event.custom({
    type: 'create:crushing',
    ingredients: [{ item: input }],
    results: results.slice(0, 4),
    processingTime: 250
  }).id('kubejs:factory_ascension/' + name)

  // ---- Common metals ----
  mix('common_mineral_cluster', many('minecraft:gravel', 4).concat([lava(100)]),
    'kubejs:common_mineral_cluster', 'heated')
  crush('crush_common_mineral_cluster', 'kubejs:common_mineral_cluster', [
    { item: 'create:crushed_raw_iron' },
    { item: 'create:crushed_raw_copper', chance: 0.75 },
    { item: 'create:crushed_raw_zinc', chance: 0.6 },
    { item: 'create:crushed_raw_gold', chance: 0.35 }
  ])

  // ---- Gems ----
  mix('gem_mineral_cluster', many('minecraft:cobblestone', 4).concat([{ item: 'minecraft:quartz' }, lava(250)]),
    'kubejs:gem_mineral_cluster', 'superheated')
  crush('crush_gem_mineral_cluster', 'kubejs:gem_mineral_cluster', [
    { item: 'minecraft:redstone', count: 2 },
    { item: 'minecraft:lapis_lazuli', chance: 0.75 },
    { item: 'minecraft:diamond', chance: 0.12 },
    { item: 'minecraft:emerald', chance: 0.08 }
  ])

  // ---- Modded metals: uses whichever mods are installed ----
  // One entry per metal; the first item that exists is used. Missing mods are skipped.
  let exotic = [
    { ids: ['mekanism:raw_osmium'], chance: 0.6 },
    { ids: ['mekanism:raw_lead', 'tfmg:raw_lead'], chance: 0.5 },
    { ids: ['tfmg:raw_nickel'], chance: 0.5 },
    { ids: ['mekanism:raw_tin'], chance: 0.5 },
    { ids: ['mekanism:raw_uranium'], chance: 0.3 },
    { ids: ['tfmg:raw_lithium'], chance: 0.3 }
  ]
  let exoticResults = []
  exotic.forEach(metal => {
    for (let i = 0; i < metal.ids.length; i++) {
      if (exists(metal.ids[i])) {
        exoticResults.push({ item: metal.ids[i], chance: metal.chance })
        break
      }
    }
  })

  if (exoticResults.length > 0) {
    mix('exotic_mineral_cluster', many('minecraft:blackstone', 4).concat([{ item: 'minecraft:redstone' }, lava(250)]),
      'kubejs:exotic_mineral_cluster', 'superheated')
    crush('crush_exotic_mineral_cluster', 'kubejs:exotic_mineral_cluster', exoticResults)
    if (exoticResults.length > 4) {
      console.info('[Factory Ascension] more than 4 modded metals found; the Exotic cluster gives the first 4')
    }
  } else {
    console.info('[Factory Ascension] no modded metals found; Exotic Mineral Cluster has no recipe')
  }

  // ---- Netherite ----
  mix('ancient_debris',
    many('minecraft:netherrack', 4).concat(many('minecraft:gold_ingot', 2), [{ item: 'minecraft:diamond' }, lava(1000)]),
    'minecraft:ancient_debris', 'superheated')
})
