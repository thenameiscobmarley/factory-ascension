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
    let e = global.FA.enchants[id]
    event.create('tome_' + id).displayName((e.ultimate ? 'Ultimate Tome: ' : e.mythic ? 'Mythic Tome of ' : e.type == 'fusion' ? 'Fusion Tome of ' : 'Tome of ') + e.name).rarity('epic').glow(true)
  })

  // Rolls a random enchant (rarity, name and effects) onto gear
  event.create('chaos_tome').displayName('Chaos Tome').rarity('epic').glow(true)

  // Dropped only by Ascended bosses, needed for Ultimate tomes
  event.create('ascended_soul').displayName('Ascended Soul').rarity('epic').glow(true)

  // Dropped by mobs out in the danger zones, needed for Mythic tomes
  event.create('mythic_fragment').displayName('Mythic Fragment').rarity('epic').glow(true)

  // Gems: socket into gear for extra enchant levels (3 tiers per type)
  let G = global.FA.gems
  Object.keys(G.types).forEach(type => {
    for (let tier = 1; tier <= 3; tier++) {
      event.create(global.FA_gemId(type, tier).replace('kubejs:', ''))
        .displayName(G.tiers[tier - 1].name + ' ' + G.types[type].name)
        .rarity(tier == 3 ? 'epic' : tier == 2 ? 'rare' : 'uncommon').glow(tier == 3)
    }
  })

  // Renewable ore line: made from cobble-gen blocks, crushed into ores
  event.create('common_mineral_cluster').displayName('Common Mineral Cluster')
  event.create('gem_mineral_cluster').displayName('Gem Mineral Cluster').rarity('uncommon')
  event.create('exotic_mineral_cluster').displayName('Exotic Mineral Cluster').rarity('rare')
})

// Liquid Essence: melted cores. Pipe it, tank it, feed it to an Ascension Altar, or drink a bucket for Essence.
StartupEvents.registry('fluid', event => {
  let color = global.FA.altar.color
  event.create('liquid_essence').displayName('Liquid Essence').thinTexture(color).bucketColor(color)
})

// Ascension Altar: Depot on top, Liquid Essence tank or basin below = gear levels itself up
StartupEvents.registry('block', event => {
  event.create('ascension_altar').displayName('Ascension Altar').soundType('amethyst')
    .hardness(4).resistance(1200).requiresTool(true).tagBlock('minecraft:mineable/pickaxe').lightLevel(0.6)
    .textureAll('kubejs:block/ascension_altar')

  // Essence Condenser: Liquid Essence tank below -> Condensed Essence Blocks into the chest above
  event.create('essence_condenser').displayName('Essence Condenser').soundType('metal')
    .hardness(4).resistance(1200).requiresTool(true).tagBlock('minecraft:mineable/pickaxe').lightLevel(0.4)
    .textureAll('kubejs:block/essence_condenser')

  // Condensed Essence Blocks: tier 0 plus (x1) .. (x50), each glowing a little brighter
  let tiers = global.FA_condensedTiers()
  for (let tier = 0; tier <= tiers; tier++) {
    event.create('condensed_essence_' + tier).displayName(global.FA_condensedName(tier)).soundType('amethyst')
      .hardness(1.5).resistance(6).tagBlock('minecraft:mineable/pickaxe').lightLevel(Math.min(1, 0.3 + tier / tiers * 0.7))
      .textureAll('kubejs:block/condensed_essence_' + tier)
  }
})
