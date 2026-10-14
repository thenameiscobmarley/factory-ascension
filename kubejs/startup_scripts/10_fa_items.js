// Factory Ascension - items. Textures live in kubejs/assets/kubejs/textures/item/.

StartupEvents.registry('item', event => {
  // Essence items: absorbed into your Essence bank
  event.create('ascension_core').displayName('Ascension Core').rarity('rare').glow(true)
  event.create('dense_ascension_core').displayName('Dense Ascension Core').rarity('epic').glow(true)
  event.create('ascension_singularity').displayName('Ascension Singularity').rarity('epic').glow(true)

  // Create sequenced-assembly transitional item (stacks to 1 like Create's own)
  event.create('incomplete_ascension_core').displayName('Incomplete Ascension Core').maxStackSize(1)

  // Tomes: each one unlocks an Ascended enchant on one piece of gear
  event.create('blank_ascension_tome').displayName('Blank Ascension Tome').rarity('uncommon')
  Object.keys(global.FA.enchants).forEach(id => {
    event.create('tome_' + id).displayName('Tome of ' + global.FA.enchants[id].name).rarity('epic').glow(true)
  })

  // Renewable ore line: made from cobble-gen blocks, crushed into ores
  event.create('common_mineral_cluster').displayName('Common Mineral Cluster')
  event.create('gem_mineral_cluster').displayName('Gem Mineral Cluster').rarity('uncommon')
  event.create('exotic_mineral_cluster').displayName('Exotic Mineral Cluster').rarity('rare')
})
