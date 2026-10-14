// Factory Ascension - tome recipes.
// Blank tome: a deployer presses an Ascension Core into a book.
// Enchant tomes: blank tome + a themed catalyst in a heated mixer.
// Mythic tomes: also 4 Mythic Fragments, superheated.
// Every tome has at least one catalyst no other tome uses, so the mixer never mixes them up.

ServerEvents.recipes(event => {
  event.custom({
    type: 'create:deploying',
    ingredients: [{ item: 'minecraft:book' }, { item: 'kubejs:ascension_core' }],
    results: [{ item: 'kubejs:blank_ascension_tome' }]
  }).id('kubejs:factory_ascension/blank_ascension_tome')

  let catalysts = {
    // Damage
    sharpness: ['minecraft:diamond', 'minecraft:flint', 'minecraft:flint'],
    smite: ['minecraft:rotten_flesh', 'minecraft:rotten_flesh', 'minecraft:rotten_flesh'],
    bane: ['minecraft:spider_eye', 'minecraft:spider_eye'],
    giant_slayer: ['minecraft:wither_skeleton_skull'],
    hellbane: ['minecraft:blaze_rod', 'minecraft:blaze_rod'],
    raidbreaker: ['minecraft:crossbow'],
    impaling: ['minecraft:prismarine_shard', 'minecraft:prismarine_shard'],
    skyhunter: ['minecraft:phantom_membrane', 'minecraft:phantom_membrane'],
    first_strike: ['minecraft:spectral_arrow', 'minecraft:spectral_arrow'],
    executioner: ['minecraft:iron_axe'],
    berserk: ['minecraft:blaze_powder', 'minecraft:blaze_powder'],
    power: ['minecraft:bow'],
    assassin: ['minecraft:ink_sac', 'minecraft:ink_sac'],
    critical: ['minecraft:amethyst_shard', 'minecraft:amethyst_shard'],
    reaper: ['minecraft:wither_rose'],
    cleave: ['minecraft:diamond_axe'],
    // On hit
    lifesteal: ['minecraft:fermented_spider_eye', 'minecraft:redstone', 'minecraft:redstone'],
    frostbite: ['minecraft:blue_ice'],
    enfeeble: ['minecraft:poisonous_potato'],
    withering: ['minecraft:soul_sand', 'minecraft:soul_sand', 'minecraft:coal'],
    venom: ['minecraft:pufferfish'],
    skyward: ['minecraft:shulker_shell'],
    fire_aspect: ['minecraft:fire_charge', 'minecraft:fire_charge'],
    knockback: ['minecraft:piston'],
    second_wind: ['minecraft:honeycomb', 'minecraft:honeycomb'],
    adrenaline: ['minecraft:cocoa_beans', 'minecraft:cocoa_beans'],
    // On kill
    plunder: ['minecraft:emerald', 'minecraft:gold_ingot', 'minecraft:gold_ingot'],
    soul_eater: ['minecraft:soul_lantern'],
    wisdom: ['minecraft:experience_bottle', 'minecraft:experience_bottle'],
    midas: ['minecraft:gold_block'],
    gem_hunter: ['minecraft:emerald_block'],
    // Defense
    protection: ['minecraft:iron_block', 'minecraft:obsidian'],
    fire_ward: ['minecraft:magma_block'],
    featherweight: ['minecraft:slime_block'],
    blast_ward: ['minecraft:tnt'],
    arrow_ward: ['minecraft:shield'],
    spellward: ['minecraft:lapis_block'],
    wither_ward: ['minecraft:nether_star'],
    bulwark: ['minecraft:chain', 'minecraft:chain'],
    vitality: ['minecraft:golden_apple', 'minecraft:glistering_melon_slice'],
    retribution: ['minecraft:cactus', 'minecraft:quartz', 'minecraft:quartz'],
    evasion: ['minecraft:ender_pearl', 'minecraft:ender_pearl'],
    undying: ['minecraft:totem_of_undying'],
    // Passive
    renewal: ['minecraft:ghast_tear'],
    night_sight: ['minecraft:golden_carrot', 'minecraft:golden_carrot'],
    gills: ['minecraft:scute'],
    fireproof: ['minecraft:magma_cream', 'minecraft:magma_cream'],
    industrious: ['minecraft:golden_pickaxe'],
    leaping: ['minecraft:slime_ball', 'minecraft:slime_ball'],
    sustenance: ['minecraft:cake'],
    magnetism: ['minecraft:lodestone'],
    restoration: ['minecraft:anvil'],
    // Movement & utility
    swiftness: ['minecraft:sugar', 'minecraft:sugar', 'minecraft:feather'],
    reach: ['minecraft:spyglass'],
    stride: ['minecraft:scaffolding', 'minecraft:scaffolding'],
    frenzy: ['minecraft:clock'],
    lucky: ['minecraft:rabbit_foot'],
    excavation: ['create:mechanical_drill', 'minecraft:redstone_block'],
    eternal: ['minecraft:netherite_ingot', 'create:precision_mechanism'],
    // Mythic (plus 4 Mythic Fragments each, superheated)
    wings: ['minecraft:elytra'],
    thunderlord: ['minecraft:lightning_rod'],
    black_hole: ['minecraft:end_crystal'],
    meteor: ['create:blaze_cake'],
    chronos: ['minecraft:amethyst_block'],
    phoenix: ['minecraft:respawn_anchor'],
    voidwalker: ['minecraft:popped_chorus_fruit', 'minecraft:popped_chorus_fruit'],
    soul_harvest: ['minecraft:soul_campfire'],
    overkill: ['minecraft:netherite_sword'],
    rampage: ['minecraft:nether_wart_block'],
    vein_miner: ['minecraft:diamond_pickaxe'],
    timber: ['create:mechanical_saw'],
    radiance: ['minecraft:beacon'],
    headhunter: ['minecraft:skeleton_skull'],
    gravity_slam: ['minecraft:crying_obsidian']
  }

  Object.keys(global.FA.enchants).forEach(id => {
    let items = catalysts[id]
    if (!items) {
      console.error('[Factory Ascension] no tome recipe for enchant "' + id + '"')
      return
    }
    let mythic = global.FA.enchants[id].mythic
    if (mythic) items = items.concat(['kubejs:mythic_fragment', 'kubejs:mythic_fragment', 'kubejs:mythic_fragment', 'kubejs:mythic_fragment'])
    event.custom({
      type: 'create:mixing',
      ingredients: [{ item: 'kubejs:blank_ascension_tome' }].concat(items.map(i => ({ item: i }))),
      results: [{ item: 'kubejs:tome_' + id }],
      heatRequirement: mythic ? 'superheated' : 'heated'
    }).id('kubejs:factory_ascension/tome_' + id)
  })
})
