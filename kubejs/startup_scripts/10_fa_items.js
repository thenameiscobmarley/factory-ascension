// priority: 900
// Factory Ascension - items. Textures live in kubejs/assets/kubejs/textures/item/.

// [type, display name] for every Ascendite tool and armor piece
const FA_GEAR = [['sword', 'Sword'], ['pickaxe', 'Pickaxe'], ['axe', 'Axe'], ['shovel', 'Shovel'], ['hoe', 'Hoe'],
  ['helmet', 'Helmet'], ['chestplate', 'Chestplate'], ['leggings', 'Leggings'], ['boots', 'Boots']]

// Light an ore gives off (config: glowingOres), as KubeJS's 0-1 light level
function faOreLight(id) {
  let G = global.FA.glowingOres
  return G.enabled && G.light[id] ? Math.min(15, G.light[id]) / 15 : 0
}

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

  // Ascendite: early-game ore. Raw -> smelt or crush, plus shards that make Ascension Cores by hand
  event.create('raw_ascendite').displayName('Raw Ascendite')
  event.create('crushed_raw_ascendite').displayName('Crushed Raw Ascendite')
  event.create('ascendite_ingot').displayName('Ascendite Ingot')
  event.create('ascendite_nugget').displayName('Ascendite Nugget')
  event.create('ascension_shard').displayName('Ascension Shard').rarity('uncommon').glow(true)
  // Mob food: raw parts make you sick, cooked ones are good food with a small buff (config: mobFood)
  let MF = global.FA.mobFood
  MF.foods.forEach(fd => {
    event.create(fd.raw).displayName(fd.rawName).food(food => {
      food.hunger(MF.raw.hunger).saturation(MF.raw.saturation).meat()
      MF.raw.sickness.forEach(e => food.effect(e[0], e[1] * 20, e[2], e[3]))
    })
    event.create(fd.cooked).displayName(fd.cookedName).food(food => {
      food.hunger(fd.hunger).saturation(fd.saturation).meat().alwaysEdible()
      food.effect(fd.effect[0], fd.effect[1] * 20, fd.effect[2], 1)
    })
  })

  // Enchantment card packs (foil) and booster boxes of them (config: packs)
  event.create('enchantment_pack').displayName('Enchantment Pack').rarity('rare').glow(true)
  event.create('booster_box').displayName('Enchantment Booster Box').rarity('epic').glow(true)
  // A piece of relic gear, sealed; its quality depends on the danger where it's opened (config: caches)
  event.create('gear_cache').displayName('Sealed Gear Cache').rarity('rare')

  // Rerolls a piece of gear's condition, keeping the better grade (config: condition)
  event.create('polishing_cloth').displayName('Polishing Cloth').rarity('uncommon')

  // Rock Hammers: smash rock into gravel, nuggets and ore (config: hammer)
  event.create('rock_hammer').displayName('Rock Hammer').maxDamage(global.FA.hammer.tools['kubejs:rock_hammer'].uses)
  event.create('ascendite_rock_hammer').displayName('Ascendite Rock Hammer').rarity('uncommon')
    .maxDamage(global.FA.hammer.tools['kubejs:ascendite_rock_hammer'].uses)

  // Tools and armor use the tiers registered below (numbers in the config)
  FA_GEAR.forEach(g => event.create('ascendite_' + g[0], g[0]).displayName('Ascendite ' + g[1]).tier('ascendite'))

  // Higher gear tiers (config: gearTiers): an ingot and a full set each
  Object.keys(global.FA.gearTiers).forEach(t => {
    let T = global.FA.gearTiers[t]
    let rarity = t == 'voidsteel' ? 'uncommon' : t == 'celestium' ? 'rare' : 'epic'
    event.create(t + '_ingot').displayName(T.name + ' Ingot').rarity(rarity)
    FA_GEAR.forEach(g => event.create(t + '_' + g[0], g[0]).displayName(T.name + ' ' + g[1]).tier(t).rarity(rarity))
  })
})

// Tool and armor tiers (the website runs this file without them, so they're optional here)
if (typeof ItemEvents != 'undefined' && ItemEvents.toolTierRegistry) {
  // Ascendite plus every tier in the config's gearTiers, each repaired with its own ingot
  let faAllTiers = () => {
    let all = { ascendite: { tools: global.FA.ascendite.tools, armor: global.FA.ascendite.armor } }
    Object.keys(global.FA.gearTiers).forEach(t => { all[t] = global.FA.gearTiers[t] })
    return all
  }
  ItemEvents.toolTierRegistry(event => {
    let all = faAllTiers()
    Object.keys(all).forEach(name => {
      let T = all[name].tools
      event.add(name, tier => {
        tier.uses = T.uses
        tier.speed = T.speed
        tier.attackDamageBonus = T.attackDamageBonus
        tier.level = T.level
        tier.enchantmentValue = T.enchantability
        tier.repairIngredient = 'kubejs:' + name + '_ingot'
      })
    })
  })
  ItemEvents.armorTierRegistry(event => {
    let all = faAllTiers()
    Object.keys(all).forEach(name => {
      let A = all[name].armor
      event.add(name, tier => {
        tier.durabilityMultiplier = A.durabilityMultiplier
        tier.slotProtections = A.protection
        tier.toughness = A.toughness
        tier.knockbackResistance = A.knockbackResistance
        tier.enchantmentValue = A.enchantability
        tier.equipSound = name == 'ascendite' ? 'minecraft:item.armor.equip_iron' : 'minecraft:item.armor.equip_netherite'
        tier.repairIngredient = 'kubejs:' + name + '_ingot'
      })
    })
  })
}

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

  // Ascendite ore (drops come from a loot table made in 32_fa_ascendite_data.js) and its storage block
  event.create('ascendite_ore').displayName('Ascendite Ore').soundType('stone')
    .hardness(3).resistance(3).requiresTool(true).tagBlock('minecraft:mineable/pickaxe').tagBlock('minecraft:needs_stone_tool')
    .lightLevel(faOreLight('kubejs:ascendite_ore')).textureAll('kubejs:block/ascendite_ore')
  event.create('deepslate_ascendite_ore').displayName('Deepslate Ascendite Ore').soundType('deepslate')
    .hardness(4.5).resistance(3).requiresTool(true).tagBlock('minecraft:mineable/pickaxe').tagBlock('minecraft:needs_stone_tool')
    .lightLevel(faOreLight('kubejs:deepslate_ascendite_ore')).textureAll('kubejs:block/deepslate_ascendite_ore')
  event.create('ascendite_block').displayName('Block of Ascendite').soundType('metal')
    .hardness(5).resistance(6).requiresTool(true).tagBlock('minecraft:mineable/pickaxe').tagBlock('minecraft:needs_stone_tool')
    .textureAll('kubejs:block/ascendite_block')
})

// Glowing ores: everything else in the config's list (vanilla and other mods' ores). Skipped on the website.
if (typeof BlockEvents != 'undefined' && BlockEvents.modification) {
  BlockEvents.modification(event => {
    let G = global.FA.glowingOres
    if (!G.enabled) return
    Object.keys(G.light).forEach(id => {
      if (id.indexOf('kubejs:') == 0 || !Platform.isLoaded(id.split(':')[0])) return
      try {
        event.modify(id, block => { block.lightEmission = Math.min(15, G.light[id]) })
      } catch (err) {
        console.warn('[Factory Ascension] could not make ' + id + ' glow: ' + err)
      }
    })
  })
}
