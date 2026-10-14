// priority: 680
// Factory Ascension - Ascendite tags and the ore's loot table, built from the config.

ServerEvents.tags('item', event => {
  event.add('forge:ingots/ascendite', 'kubejs:ascendite_ingot')
  event.add('forge:ingots', 'kubejs:ascendite_ingot')
  event.add('forge:nuggets/ascendite', 'kubejs:ascendite_nugget')
  event.add('forge:nuggets', 'kubejs:ascendite_nugget')
  event.add('forge:raw_materials/ascendite', 'kubejs:raw_ascendite')
  event.add('forge:raw_materials', 'kubejs:raw_ascendite')
  event.add('forge:storage_blocks/ascendite', 'kubejs:ascendite_block')
  event.add('forge:storage_blocks', 'kubejs:ascendite_block')
  event.add('create:crushed_raw_materials', 'kubejs:crushed_raw_ascendite')
  ;['ascendite_ore', 'deepslate_ascendite_ore'].forEach(ore => {
    event.add('forge:ores/ascendite', 'kubejs:' + ore)
    event.add('forge:ores', 'kubejs:' + ore)
  })
})

ServerEvents.tags('block', event => {
  ;['ascendite_ore', 'deepslate_ascendite_ore'].forEach(ore => {
    event.add('forge:ores/ascendite', 'kubejs:' + ore)
    event.add('forge:ores', 'kubejs:' + ore)   // so Vein Miner and ore-finding mods know it
  })
  event.add('forge:storage_blocks/ascendite', 'kubejs:ascendite_block')
  event.add('minecraft:beacon_base_blocks', 'kubejs:ascendite_block')
})

// Ore drops: Silk Touch gives the ore; otherwise Raw Ascendite (Fortune adds more, like iron) and a chance of an
// Ascension Shard (Fortune raises it)
ServerEvents.highPriorityData(event => {
  let O = global.FA.ascendite.ore
  let silk = { condition: 'minecraft:match_tool', predicate: { enchantments: [{ enchantment: 'minecraft:silk_touch', levels: { min: 1 } }] } }
  let noSilk = { condition: 'minecraft:inverted', term: silk }
  let fortuneChances = []
  for (let lvl = 0; lvl <= 4; lvl++) fortuneChances.push(Math.min(1, O.shardChance + O.shardFortune * lvl))
  ;['ascendite_ore', 'deepslate_ascendite_ore'].forEach(ore => {
    event.addJson('kubejs:loot_tables/blocks/' + ore, {
      type: 'minecraft:block',
      pools: [
        {
          rolls: 1,
          entries: [{
            type: 'minecraft:alternatives',
            children: [
              { type: 'minecraft:item', name: 'kubejs:' + ore, conditions: [silk] },
              {
                type: 'minecraft:item', name: 'kubejs:raw_ascendite',
                functions: [
                  { function: 'minecraft:apply_bonus', enchantment: 'minecraft:fortune', formula: 'minecraft:ore_drops' },
                  { function: 'minecraft:explosion_decay' }
                ]
              }
            ]
          }]
        },
        {
          rolls: 1,
          conditions: [noSilk, { condition: 'minecraft:table_bonus', enchantment: 'minecraft:fortune', chances: fortuneChances }],
          entries: [{ type: 'minecraft:item', name: 'kubejs:ascension_shard' }]
        }
      ]
    })
  })
})
