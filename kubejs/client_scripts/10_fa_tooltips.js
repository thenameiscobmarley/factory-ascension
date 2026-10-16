// priority: 900
// Factory Ascension - tooltips.

// Tooltip color for each kind of enchant
let FA_TYPE_COLORS = {
  damage: 'red', critical: 'red', reaper: 'darkRed', cleave: 'red',
  lifesteal: 'darkRed', hit_effect: 'lightPurple', self_effect: 'lightPurple', ignite: 'gold', hit_heal: 'green',
  plunder: 'yellow', kill_heal: 'green', xp: 'yellow', drop: 'yellow',
  resist: 'aqua', vitality: 'green', retribution: 'gold', dodge: 'aqua', undying: 'gold',
  regen: 'green', passive: 'green', magnet: 'blue', repair: 'blue',
  attribute: 'white', mining: 'blue', eternal: 'lightPurple',
  flight: 'gold', chain_lightning: 'gold', gravity_well: 'gold', death_nova: 'gold', time_stop: 'gold', phoenix: 'gold',
  projectile_teleport: 'gold', souls: 'gold', overkill: 'gold', rampage: 'gold', vein: 'gold', aura: 'gold', heads: 'gold', slam: 'gold',
  fusion: 'darkAqua', ultimate: 'red'
}

let FA_TIP_RARITY = { gray: 'gray', aqua: 'aqua', light_purple: 'lightPurple', gold: 'gold', red: 'red' }
let FA_GRADE_COLORS = { dark_gray: 'darkGray', gray: 'gray', white: 'white', green: 'green', aqua: 'aqua', gold: 'gold' }
const $FATipArmor = Java.loadClass('net.minecraft.world.item.ArmorItem')

// One flavor line for every mob part, raw and cooked (raw id: [raw, cooked]). The harness checks they're all here and all different.
let FA_FOOD_LORE = {
  pig_eyes: ['They are still looking at you.', 'Crunchy, and you can see in the dark now.'],
  pig_brain: ['Not much going on in there.', 'Smart food for a busy miner.'],
  cow_tongue: ['It moos, faintly, when you squeeze it.', 'Fills you up like a whole steak dinner.'],
  cow_heart: ['Big, red and still warm.', 'Seared on the outside, pure strength inside.'],
  sheep_liver: ['Smells like wet wool.', 'A shepherd\'s secret for healing up.'],
  chicken_feet: ['Scratchy little toes.', 'Fried crisp. You feel lighter on your feet.'],
  rabbit_leg: ['Lucky, but not for the rabbit.', 'Hop to it.'],
  spider_legs: ['Eight of them. Some still kicking.', 'Like crab legs, if crabs lived in caves.'],
  zombie_brain: ['Grey, soft and full of grudges.', 'Cooking it gets the rot out. Mostly.'],
  marrow_bone: ['Rattles when you shake it.', 'A thick broth that makes your own bones feel tougher.'],
  creeper_heart: ['It hisses if you hold it too long.', 'Smoked slowly so it doesn\'t go off.'],
  squid_tentacle: ['Slimy and still squirming.', 'Golden rings with a squeeze of seawater.'],
  hoglin_ribs: ['A whole rack, bristles and all.', 'Slow-cooked over nether fire. Messy and worth it.'],
  ender_gland: ['It blinks out of your hand sometimes.', 'Seared until it stops teleporting.'],
  blaze_core: ['Too hot to hold for long.', 'Charred black, still glowing in the middle.'],
  ghast_membrane: ['Thin as paper, and it cries.', 'Crispy like a chip. Light as air.'],
  slime_core: ['It wobbles. A lot.', 'Fried until it stops bouncing.'],
  magma_core: ['Drips little blobs of lava.', 'Seared dark, it keeps you cool in the Nether.'],
  witch_wart: ['Warty, green and whispering.', 'Roasted, it tastes of luck and herbs.'],
  guardian_spine: ['Spiky, and it stares at you.', 'Grilled with the spikes off. Ocean flavor.'],
  phantom_wing: ['Smells like a night without sleep.', 'Crispy, and you drift down like a feather.'],
  withered_marrow: ['Black and dry as ash.', 'Blackened even more. Tough food for tough fights.'],
  piglin_snout: ['Snorts at you when you look away.', 'Roasted crisp. Piglins would not approve.'],
  mite_carapace: ['A tiny shell that skitters.', 'Toasted like a nut. Good for busy hands.'],
  shulker_tongue: ['It hides in its shell when poked.', 'Seared tender. Your skin feels like armor.'],
  illager_heart: ['Cold, and it hums a raid horn.', 'Charred over a campfire, like a captain\'s feast.'],
  vex_wisp: ['Buzzing, flickering, annoying.', 'Sizzled until it stops whining.'],
  ravager_flank: ['Enough meat for a whole village.', 'A feast that makes you hit like a ravager.'],
  warden_heart: ['It beats in the dark, loud as thunder.', 'Seared slowly. You feel twice as alive.'],
  strider_leg: ['Long, wobbly and still warm from the lava.', 'Grilled on the hot side. Never feel the heat again.'],
  dragon_heart: ['It still beats, and the End trembles.', 'Seared with dragon fire. The greatest meal there is.'],
  wither_spine: ['Three skulls\' worth of spite.', 'Charred so hard nothing can hurt you.'],
  bat_wing: ['Leathery and squeaky.', 'Fried crisp. The dark doesn\'t seem so dark.'],
  horse_haunch: ['A proper haunch. Smells of hay.', 'Roasted and juicy. You feel like galloping.'],
  llama_shank: ['It tried to spit at you.', 'Spiced like a trader\'s campfire dinner.'],
  goat_shank: ['Tough as a mountain rock.', 'Roasted on the peak. Jump like a goat.'],
  fox_haunch: ['Sly, even now.', 'Roasted with berries. Quick on your feet.'],
  wolf_haunch: ['Lean, and it still growls.', 'Grilled rare. You feel the pack\'s strength.'],
  bear_paw: ['Huge, furry and cold.', 'Roasted in the snow. Nothing knocks you over.'],
  panda_paw: ['Smells faintly of bamboo.', 'Braised in bamboo. Restful and healing.'],
  turtle_fin: ['Salty and leathery.', 'A slow soup. You can breathe the sea.'],
  frog_legs: ['Croaks when you pick it up.', 'Fried golden. Spring in your step.'],
  camel_hump: ['It sloshes. Mostly water and fat.', 'Roasted slowly. Keeps you full across a desert.'],
  sniffer_snout: ['Still sniffing for seeds.', 'Roasted. It smells luck for you now.'],
  bee_stinger: ['Careful. Pointy end out.', 'Dipped in honey. Sweet and soothing.'],
  rusty_gizzard: ['Full of grit, gears and rust.', 'Grilled clean. Your hands feel quicker.'],
  glowing_brain: ['Glows teal and hums a little.', 'Seared until the glow sinks into your eyes.'],
  ember_heart: ['Smoulders in your palm.', 'Smouldering still. Fire won\'t touch you.'],
  frozen_heart: ['Frost creeps up your arm.', 'Thawed by the fire. Cold resolve, warm belly.'],
  miners_lung: ['Coughs up coal dust.', 'Smoked dark. You swing a pickaxe twice as fast.'],
  void_marrow: ['Floats a little off your palm.', 'Blackened until it stops floating. You won\'t fall hard.'],
  glowing_marrow: ['Bones with a teal shine.', 'Roasted. You feel solid as Ascendite.'],
  crystal_fang: ['Sharp as amethyst, because it is.', 'Toasted. It rings when you bite it.'],
  charged_gland: ['Crackles and makes your hair stand up.', 'Sizzled. You feel quick as lightning.'],
  cinder_gland: ['Leaves ash on your fingers.', 'Charred twice. Flames slide right off you.'],
  glowing_creeper_heart: ['Ticks quietly. Teal sparks.', 'Smoked slowly. A shield of extra hearts.'],
  frost_core: ['A snowball that never melts.', 'Thawed out. You feel like a glacier.'],
  essence_gel: ['Purple, wobbly and full of power.', 'Candied like a jelly sweet. Heals fast.'],
  copper_gel: ['Turns your fingers green.', 'Fried. It tastes like a penny, in a good way.'],
  ore_mite_shell: ['Little flecks of ore in the shell.', 'Toasted. Your luck goes up.'],
  wisp_core: ['A tiny teal flame that won\'t go out.', 'Seared. Your feet feel light and fast.'],
  void_eye: ['It stares back. From very far away.', 'Seared. Now you can see in the dark.'],
  raider_heart: ['Beats like a war drum.', 'Charred. Hit as hard as a raider.'],
  ember_wings: ['Warm, and they smell like a campfire.', 'Hot wings. Actually hot. Fireproof hot.'],
  copper_tongue: ['Green and metallic. Moo.', 'Grilled copper-brown. Work twice as fast.'],
  bog_heart: ['Squelches. Smells like a swamp.', 'Smoked over peat. Breathe underwater.'],
  wraith_dust_gland: ['Leaks sand everywhere.', 'Roasted in desert heat. Fire can\'t touch you.'],
  thunder_core: ['Rumbles like a storm far away.', 'Charged and crackling. Run like lightning.'],
  brass_marrow: ['Heavy, shiny and clanks.', 'Roasted. You feel armored in brass.'],
  shadow_marrow: ['It pulls the light out of the room.', 'Blackened. The dark is your friend now.'],
  weaver_spinneret: ['Still spinning thread.', 'Toasted. You bounce like on a web.'],
  moss_gel: ['Green, squishy, smells like a lush cave.', 'Fried like a fritter. Fresh and earthy.'],
  glow_mite_shell: ['Glows in your pocket.', 'Toasted. You glow a little afterwards.'],
  storm_core: ['Static jumps from it to your hand.', 'Seared in a lightning strike. Electric taste.'],
  treasure_heart: ['It jingles. There\'s gold in it.', 'Gilded and rich. Tastes like a treasure hoard.'],
  colossus_heart: ['A rusted engine of a heart. Huge.', 'Forged, not cooked. Food fit for a giant.'],
  brood_gland: ['Venom drips from it.', 'Seared until the venom cooks off. Mostly.'],
  kings_marrow: ['Bone from a crowned king. Cold as a tomb.', 'Roasted for a royal feast.'],
  tyrant_core: ['A furnace of teal fire in your hand.', 'Quenched in water. The fire is yours now.']
}

// "+12% damage" / "-3% damage taken" from a multiplier
function faPct(mult, what) {
  let p = Math.round((mult - 1) * 1000) / 10
  return (p > 0 ? '+' : '') + p + '% ' + what
}

function faColor(id) {
  return Text[FA_TYPE_COLORS[global.FA.enchants[id].type] || 'gold']
}

ItemEvents.tooltip(event => {
  // Ascended enchants on any item, listed right under its name like vanilla enchants
  event.addAdvanced(Ingredient.all, (item, advanced, text) => {
    let tag = item.nbt
    if (!tag || !tag.contains('FactoryAscension')) return
    let data = tag.getCompound('FactoryAscension')
    let line = 1
    let relic = data.getInt('relic')
    if (relic > 0) text.add(line++, relic == 2 ? Text.red('\u2739 MYTHIC RELIC').bold(true) : Text.gold('\u2605 LEGENDARY RELIC').bold(true))
    let stars = data.getInt('stars')
    // Artifact: serial, quirks, lore
    if (data.contains('artifact')) {
      let a = data.getCompound('artifact')
      let tc = { gold: 'gold', light_purple: 'lightPurple', aqua: 'aqua', red: 'red', green: 'green', yellow: 'yellow' }[String(a.getString('color'))] || 'gold'
      text.add(line++, Text[tc]('\u2736 ARTIFACT').bold(true).append(Text.gray('  1 of 1  \u00b7  #' + ('000' + a.getInt('serial')).slice(-4))))
      let quirks = a.getCompound('quirks')
      quirks.getAllKeys().forEach(k => {
        let q = quirks.getCompound(k)
        text.add(line++, Text.aqua('\u25c8 ' + global.FA_quirkText(String(q.getString('id')), q.getDouble('power'))))
      })
      if (a.getInt('kills') > 0) text.add(line++, Text.darkGray('  Kills: ' + global.FA_num(a.getInt('kills'))))
      text.add(line++, Text.darkPurple(String(a.getString('lore'))).italic(true))
    }
    // Attachments
    if (data.contains('attach')) {
      let at = data.getCompound('attach')
      at.getAllKeys().forEach(k => text.add(line++, Text.gold('\u2699 ' + global.FA_attachmentText(String(at.getString(k))))))
    }
    // Foil on a tome: what it does to the level it puts on gear
    if (data.contains('foil')) {
      let F = global.FA.foils.types.filter(f => f.id == String(data.getString('foil')))[0]
      let tid = String(item.getId()).replace('kubejs:tome_', '')
      let te = global.FA.enchants[tid]
      if (F && te) {
        let base = Math.max(1, data.getInt(tid))
        let applied = Math.floor(base * F.mult)
        if (te.maxLevel) applied = Math.min(applied, te.maxLevel)
        let fc = { gold: 'gold', white: 'white', yellow: 'yellow', aqua: 'aqua', light_purple: 'lightPurple', dark_purple: 'darkPurple', dark_gray: 'darkGray' }[F.color] || 'gold'
        text.add(line++, Text[fc]('\u2756 ' + F.name.toUpperCase() + ' FOIL').bold(true).append(Text.gray('  x' + F.mult + ': goes on gear at level ' + global.FA_num(applied))))
      }
    }
    // A holo card pulled from a pack
    if (data.contains('card')) text.add(line++, Text.lightPurple('\u2726 HOLO \u00b7 ' + data.getString('card')).bold(true))
    // Condition grade, right under the name
    if (data.contains('condition') && global.FA.condition.enabled) {
      let gi = data.getInt('condition') - 1
      let G = global.FA.condition.grades
      if (gi >= 0 && gi < G.length) {
        let g = G[gi]
        let armor = item.getItem() instanceof $FATipArmor
        let effect = armor ? (g.defense == 1 ? 'no bonus' : faPct(g.defense, 'damage taken')) : (g.damage == 1 ? 'no bonus' : faPct(g.damage, 'damage'))
        text.add(line++, Text[FA_GRADE_COLORS[g.color] || 'white']((gi == G.length - 1 ? '\u2726 ' : '\u25c6 ') + g.name)
          .append(Text.darkGray('  ' + effect)))
      }
    }
    // Legendary stars (Ascension Workbench): the grade, its ten stars, and each star's legendary effect
    let legend = data.getInt('legend')
    if (legend > 0 && global.FA.legendary.enabled) {
      let LG = global.FA.legendary, grade = LG.grades[Math.min(legend, LG.grades.length - 1)]
      let gc = { gold: 'gold', light_purple: 'lightPurple', red: 'red', aqua: 'aqua', gray: 'gray' }[grade[1]] || 'gold'
      text.add(line++, Text[gc](global.FA_legendStars(legend) + ' ' + grade[0].toUpperCase()).bold(true)
        .append(Text.darkGray('  levels x' + global.FA_legendMult(legend).toFixed(1))))
      if (data.contains('legendfx')) {
        let fx = data.getCompound('legendfx')
        for (let i = 0; i < LG.maxStars; i++) {
          if (!fx.contains(String(i))) continue
          let e = fx.getCompound(String(i)), eid = String(e.getString('id'))
          if (global.FA.enchants[eid]) text.add(line++, Text[gc]('  \u2726 Legendary ' + global.FA.enchants[eid].name + ' ' + global.FA_num(e.getInt('level')))
            .append(Text.darkGray('  ' + global.FA_describe(eid, e.getInt('level')))))
        }
      }
    }
    if (stars > 0) text.add(line++, Text.yellow('\u2605'.repeat(Math.min(stars, 10)) + (stars > 10 ? ' x' + stars : '') + '  ')
      .append(Text.darkGray('levels count x' + global.FA_starMult(stars).toFixed(2))))
    Object.keys(global.FA.enchants).forEach(id => {
      let level = data.getInt(id)
      if (level <= 0) return
      let e = global.FA.enchants[id]
      let name = e.name + (e.maxLevel == 1 ? '' : ' ' + global.FA_num(level))
      let label = e.ultimate ? Text.red('\u2604 ').append(Text.gold(name).bold(true)) : e.mythic ? Text.lightPurple('\u2726 ').append(Text.gold(name)) : faColor(id)(name)
      text.add(line++, label.append(Text.darkGray('  ' + global.FA_describe(id, level))))
    })
    // Rolled (Chaos Tome) enchants: name in its rarity color, then each effect
    if (data.contains('rolled')) {
      let rolled = data.getCompound('rolled')
      for (let i = 0; i < global.FA.chaos.maxPerItem; i++) {
        if (!rolled.contains(String(i))) continue
        let slot = rolled.getCompound(String(i))
        let lvl = slot.getInt('level')
        if (lvl <= 0) continue
        let roll = global.FA_rollEnchant(slot.getInt('seed'))
        let R = global.FA.chaos.rarities[roll.rarity]
        text.add(line++, Text[FA_TIP_RARITY[R.color] || 'white']('\u2742 ' + roll.name + ' ' + global.FA_num(lvl)).append(Text.darkGray('  ' + R.name)))
        roll.affixes.forEach(def => text.add(line++, Text.darkGray('    ' + global.FA_describeDef(def, lvl))))
      }
    }
    // Gems
    if (data.contains('gems')) {
      let gems = data.getCompound('gems')
      let G = global.FA.gems
      for (let i = 0; i < G.sockets; i++) {
        if (!gems.contains(String(i))) continue
        let g = gems.getCompound(String(i))
        let type = G.types[String(g.getString('type'))]
        let tier = g.getInt('tier')
        if (!type || tier < 1 || tier > 3) continue
        text.add(line++, Text.aqua('\u25c6 ' + G.tiers[tier - 1].name + ' ' + type.name).append(Text.darkGray('  +' + global.FA_gemLevels(tier) + ' ' + global.FA.enchants[type.enchant].name)))
      }
    }
    // Gear sets this item belongs to
    global.FA.sets.forEach(set => {
      if (data.getInt(set.key) > 0) text.add(line++, Text.darkAqua('\u29bf ' + set.name + ' set ').append(Text.darkGray('(2 pieces: x' + set[2].mult + ' ' + global.FA.enchants[set.key].name + ', 4 pieces: more)')))
    })
    let souls = data.getInt('souls')
    if (souls > 0) text.add(line++, Text.lightPurple('  Souls: ' + global.FA_num(souls)))
  })

  Object.keys(global.FA.essenceValues).forEach(id => {
    event.add(id, [
      Text.lightPurple('Right-click in the air: +' + global.FA_num(global.FA.essenceValues[id]) + ' Essence'),
      Text.darkGray('Sneak + right-click absorbs the whole stack'),
      Text.darkGray('/ascension shows your Essence')
    ])
  })

  event.add('kubejs:blank_ascension_tome', Text.gray('Mix with a catalyst in a heated basin to make an enchant tome'))
  Object.keys(global.FA.enchants).forEach(id => {
    let e = global.FA.enchants[id]
    event.add('kubejs:tome_' + id, [
      e.ultimate ? Text.red('\u2604 ULTIMATE').bold(true) : e.mythic ? Text.lightPurple('\u2726 MYTHIC').bold(true) : e.type == 'fusion'
        ? Text.darkAqua((e.triad ? '\u2756 TRIAD of ' : '\u2697 FUSION of ') + e.parts.map(pid => global.FA.enchants[pid].name).join(' + ')) : Text.darkGray('Ascended'),
      faColor(id)('Level 1: ' + global.FA_describe(id, 1)),
      Text.gray('Hold gear in your other hand and right-click to apply it'),
      Text.darkGray(e.maxLevel == 1 ? 'One-time enchant'
        : 'Level the tome itself first if you like: hold it and type /ascension' + (e.maxLevel ? ' (max level ' + e.maxLevel + ')' : ''))
    ])
  })

  let odds = global.FA.chaos.rarities.map(R => R.name + ' ' + R.weight + '%').join(', ')
  event.add('kubejs:chaos_tome', [
    Text.lightPurple('Rolls a random enchant onto the gear in your other hand'),
    Text.gray('Rarity odds: ' + odds),
    Text.darkGray('Up to ' + global.FA.chaos.maxPerItem + ' per item. Sneak-use on a full item to reroll its newest.'),
    Text.darkGray('Upgrade rolled enchants with Essence: /ascension')
  ])
  let G = global.FA.gems
  Object.keys(G.types).forEach(type => {
    for (let tier = 1; tier <= 3; tier++) {
      event.add(global.FA_gemId(type, tier), [
        Text.aqua('+' + global.FA_gemLevels(tier) + ' ' + global.FA.enchants[G.types[type].enchant].name + ' when socketed'),
        Text.gray('Right-click with gear in your other hand (up to ' + G.sockets + ' gems per item)'),
        Text.darkGray(tier < 3 ? 'Compact 4 into one ' + G.tiers[tier].name + ' ' + G.types[type].name : 'The best gem there is')
      ])
    }
  })
  // Condensed Essence: how much is packed in, shown in buckets too
  for (let tier = 0; tier <= global.FA_condensedTiers(); tier++) {
    let buckets = Math.pow(global.FA.condensed.ratio, tier) * global.FA.condensed.mbPerBlock / 1000
    event.add(global.FA_condensedId(tier), [
      Text.darkGray('Holds ' + global.FA_num(buckets) + ' buckets of Liquid Essence'),
      Text.darkGray(tier < global.FA_condensedTiers() ? global.FA.condensed.ratio + ' of these compact into ' + global.FA_condensedName(tier + 1) : 'The densest block there is')
    ])
  }
  event.add('kubejs:essence_condenser', [
    Text.lightPurple('Packs Liquid Essence into Condensed Essence Blocks'),
    Text.gray('Liquid Essence tank or basin below, chest or vault on top'),
    Text.darkGray('Up to ' + global.FA.condensed.blocksPerSecond + ' blocks a second')
  ])
  event.add('kubejs:ascension_altar', [
    Text.lightPurple('Levels up gear on its own'),
    Text.gray('Create Depot on top, Liquid Essence tank or basin below'),
    Text.darkGray('Up to ' + global.FA.altar.maxLevelsPerSecond + ' levels a second, lowest enchant first')
  ])
  event.add('kubejs:liquid_essence_bucket', [
    Text.lightPurple('Sneak + right-click to drink: +' + (1000 / global.FA.altar.mbPerEssence) + ' Essence'),
    Text.darkGray('Or pipe it into an Ascension Altar')
  ])
  event.add('kubejs:ascended_soul', [
    Text.red('Torn from an Ascended boss'),
    Text.gray('2 of them + 4 Mythic Fragments + a catalyst make an Ultimate tome (superheated mixer)')
  ])
  event.add('kubejs:mythic_fragment', [
    Text.lightPurple('Drops from mobs out in the danger zones, mostly Elites'),
    Text.gray('4 of them + a catalyst make a Mythic tome (superheated mixer)')
  ])
  event.add('kubejs:incomplete_ascension_core', Text.darkGray('Still on the assembly line'))
  event.add('kubejs:common_mineral_cluster', Text.gray('Crush for iron, copper, zinc and gold'))
  event.add('kubejs:gem_mineral_cluster', Text.gray('Crush for redstone, lapis, diamonds and emeralds'))
  event.add('kubejs:exotic_mineral_cluster', Text.gray('Crush for modded metals (Mekanism, TFMG)'))

  // ---- Details on every pack item: a flavor line, then Shift for where it comes from, how it's made and what it's for ----
  let FA = global.FA
  let details = (id, lore, lines) => {
    event.addAdvanced(id, (item, advanced, text) => {
      text.add(Text.darkPurple(lore).italic(true))
      if (event.isShift()) lines(item).forEach(l => text.add(l))
      else text.add(Text.darkGray('Hold ').append(Text.yellow('Shift')).append(Text.darkGray(' for details')))
    })
  }
  let made = s => Text.gray('Made: ').append(Text.white(s))
  let from = s => Text.gray('Found: ').append(Text.white(s))
  let use = s => Text.gray('Used for: ').append(Text.white(s))
  let tip = s => Text.darkGray(s)
  // The next tier up, and what it takes
  let up = s => Text.gold('\u2b06 Upgrade: ').append(Text.white(s))
  let ess = n => global.FA_num(n) + ' Essence'
  let mb = FA.altar.mbPerEssence
  let A = FA.ascendite
  let pct = x => Math.round(x * 100) + '%'
  // "Diamond, 2x Flint" from a list of item ids
  let itemList = ids => {
    let counts = {}, order = []
    ids.forEach(i => { if (!counts[i]) { counts[i] = 0; order.push(i) } counts[i]++ })
    return order.map(i => (counts[i] > 1 ? counts[i] + 'x ' : '') + faItemName(i)).join(', ')
  }

  // Cores
  let coreWays = [
    made('a diamond on a sequenced assembly line (rose quartz' + (Platform.isLoaded('tfmg') ? ', steel' : '') + ', 250 mB lava, press) x3'),
    tip('  or 4 Ascendite Ingots + 4 Redstone + Lapis (crafting grid)'),
    tip('  or ' + A.shardsPerCore + ' Ascension Shards (crafting grid, or pressed in a Basin)'),
    tip('  or 2 Ascendite Ingots + Redstone + 250 mB Lava (heated mixer)'),
    tip('  or ' + A.mixerNuggets + ' Ascendite Nuggets + Redstone (Mechanical Mixer, no heat: the early automated way)'),
    tip('  Early line: Cobblestone -> Press -> Raw Ascendite -> Millstone -> fan through water -> Mixer')
  ]
  details('kubejs:ascension_core', 'Hums with the power of a whole factory.', () => coreWays.concat([
    use('Essence, Blank Tomes, Fusion and Chaos Tomes, Liquid Essence (' + mb + ' mB each)'),
    up('9 of them -> a Dense Ascension Core (Basin under a Mechanical Press, heated)')]))
  details('kubejs:dense_ascension_core', "Nine cores crushed into one. It's warm to the touch.", () => [
    made('9 Ascension Cores pressed in a heated Basin'), tip('Unpack back into 9 in a crafting grid'),
    use('the Ascension Altar, the Essence Condenser, ' + 9 * mb + ' mB of Liquid Essence'),
    up('9 of them -> an Ascension Singularity (Basin under a Mechanical Press, superheated)')])
  details('kubejs:ascension_singularity', 'A pinpoint of pure progress. Do not drop.', () => [
    made('9 Dense Cores pressed in a superheated Basin'), tip('Unpack into 9 Dense Cores in a crafting grid'),
    use(ess(81) + ' in one click, or a stack of them for ' + ess(81 * 64)),
    tip('The top core tier. For more, melt them into Liquid Essence and condense it into blocks.')])
  details('kubejs:incomplete_ascension_core', 'Not done yet. Keep it moving.', () => [
    tip('Half-made by the sequenced assembly line. Feed it back to the start until it pops out finished.')])

  // Tomes
  details('kubejs:blank_ascension_tome', 'Empty pages, waiting for a purpose.', () => [
    made('a Deployer pressing an Ascension Core into a Book'),
    use('every enchant tome: mix it with that tome\'s catalyst in a heated Basin (see each tome)'),
    tip('Also pulls an enchant off gear into a tome: gear in one hand, this in the other, /ascension -> [Extract]')])
  let costTo = {}
  let essenceTo = (id, target) => {
    let key = id + '|' + target
    if (costTo[key] === undefined) {
      let total = 0
      for (let L = 1; L < target; L++) total += global.FA_cost(L, id)
      costTo[key] = total
    }
    return costTo[key]
  }
  Object.keys(FA.enchants).forEach(id => {
    let e = FA.enchants[id]
    let lore = e.ultimate ? 'A legendary technique, written in blood and lightning.' : e.mythic ? 'Its pages glow with something not of this world.'
      : e.triad ? 'Three powers, one will.' : e.type == 'fusion' ? 'Two powers bound into one spine.' : 'Knowledge that grows without limit.'
    details('kubejs:tome_' + id, lore, () => {
      let lines = []
      if (e.type == 'fusion') {
        lines.push(made(e.parts.map(pid => 'Tome of ' + FA.enchants[pid].name).join(' + ') + (e.triad ? ' + 2 Ascension Cores' : ' + Ascension Core') + ', heated mixer'))
        lines.push(tip('Each part works at ' + pct(e.strength || FA.fusionStrength || 0.6) + ' strength, all in one enchant slot'))
      } else {
        let cat = global.FA_TOME_CATALYSTS[id] || []
        let extra = e.ultimate ? ' + 4 Mythic Fragments + 2 Ascended Souls' : e.mythic ? ' + 4 Mythic Fragments' : ''
        lines.push(made('Blank Tome + ' + itemList(cat) + extra + ((e.ultimate || e.mythic) ? ', superheated mixer' : ', heated mixer')))
      }
      if (e.ultimate) {
        lines.push(tip('Sneak and hit an enemy with a melee attack to unleash it. You can\'t be hurt mid-combo,'))
        lines.push(tip('and it always brings you back where you started. Cooldown ' + global.FA_ultCooldown(e, 1) + 's at level 1, shorter as it levels.'))
      }
      if (e.maxLevel != 1) {
        ;[10, 100, 1000].filter(L => !e.maxLevel || L <= e.maxLevel).forEach(L =>
          lines.push(Text.gray('Level ' + global.FA_num(L) + ': ').append(faColor(id)(global.FA_describe(id, L)))
            .append(Text.darkGray('  (' + ess(essenceTo(id, L)) + ' from level 1)'))))
      }
      return lines
    })
  })
  details('kubejs:chaos_tome', 'No two pages read the same.', () => [
    made('Blank Tome + 4 Ascension Cores + Eye of Ender, superheated mixer'),
    from('Ascended bosses drop 1-3'),
    tip('Every roll makes a brand-new enchant with its own name, rarity and 1-3 effects')])

  // Boss and danger-zone drops
  let F = FA.fragments
  details('kubejs:mythic_fragment', 'A shard of something that should not exist.', () => [
    from('danger-zone kills. Elites: ' + pct(global.FA_fragmentChance(1, true)) + ' at 1,000 blocks out, ' + pct(global.FA_fragmentChance(5, true)) + ' at 2,000 (max ' + pct(F.eliteChanceCap) + ')'),
    tip('  Other mobs: ' + (global.FA_fragmentChance(5, false) * 100).toFixed(1) + '% at 2,000 blocks, climbing fast after (max ' + pct(F.normalChanceCap) + '). Bosses drop several.'),
    use('Mythic tomes (4 each) and Ultimate tomes (4 each)')])
  details('kubejs:ascended_soul', 'Still screaming, faintly.', () => [
    from('Ascended bosses only: rare giant mobs at danger ' + FA.bosses.minDanger + '+ (' + global.FA_num(global.FA_dangerBlocks(FA.bosses.minDanger)) + '+ blocks from ' + (FA.danger.centerOn == 'home' ? 'your home)' : 'spawn)')),
    use('Ultimate tomes (2 each)')])

  // Gems
  Object.keys(FA.gems.types).forEach(type => {
    let T = FA.gems.types[type]
    for (let tier = 1; tier <= 3; tier++) {
      let lore = ['Rough, but the power is there.', 'Cut without a single flaw.', 'Perfect. It sings when it catches the light.'][tier - 1]
      details(global.FA_gemId(type, tier), lore, () => [
        tier == 1 ? made('Diamond + 250 mB Lava + ' + T.catalyst[1] + 'x ' + faItemName(T.catalyst[0]) + ', superheated mixer')
          : made('4 ' + FA.gems.tiers[tier - 2].name + ' ' + T.name + ' pressed in a ' + (tier == 3 ? 'superheated' : 'heated') + ' Basin'),
        tip('Each socketed gem adds its levels on top of the ' + FA.enchants[T.enchant].name + ' levels you bought'),
        tier < 3 ? up('4 of these -> one ' + FA.gems.tiers[tier].name + ' ' + T.name + ' (Basin under a Press, ' + (tier == 2 ? 'superheated' : 'heated') + '): +' + global.FA_gemLevels(tier + 1) + ' levels')
          : tip('The top gem tier: worth ' + global.FA_gemLevels(3) / global.FA_gemLevels(1) + ' Chipped gems in one socket')])
    }
  })

  // Ore line
  details('kubejs:common_mineral_cluster', 'Gravel, pressed by heat into something worth crushing.', () => [
    made('4 Gravel + 100 mB Lava, heated mixer'), use('crushing: crushed iron, copper (75%), zinc (60%), gold (35%)'),
    tip('Wash the crushed ore with a fan through water for nuggets, or blow crushed iron through soul fire for Ascendite')])
  details('kubejs:gem_mineral_cluster', 'Stone that remembers being a gem.', () => [
    made('4 Cobblestone + Quartz + 250 mB Lava, superheated mixer'), use('crushing: 2 Redstone, Lapis (75%), Diamond (12%), Emerald (8%)')])
  details('kubejs:exotic_mineral_cluster', 'Blackstone laced with metals from other worlds.', () => [
    made('4 Blackstone + Redstone + 250 mB Lava, superheated mixer'), use('crushing: modded raw metals (osmium, lead, nickel, tin...)')])

  // Liquid Essence machines
  details('kubejs:ascension_altar', 'Place gear upon it and watch it grow.', () => [
    made('2 Amethyst Blocks + Dense Core + Brass Casing + 5 Obsidian'),
    tip('Build: Create Depot on top (put gear on it), tank or basin of Liquid Essence below'),
    tip('Normal Essence prices: 1 Essence = ' + mb + ' mB. Counts toward your milestones even offline.')])
  details('kubejs:essence_condenser', 'Turns a lake of essence into a stack of blocks.', () => [
    made('4 Brass Ingots + 3 Tinted Glass + Dense Core + Fluid Tank'),
    tip('Build: tank or basin of Liquid Essence below, chest, barrel or Item Vault on top'),
    tip('1 block = ' + FA.condensed.mbPerBlock + ' mB. Never takes part of a block.')])
  details('kubejs:liquid_essence_bucket', 'Smells like ozone and ambition.', () => [
    made('melt Ascension Cores with a splash of water in a heated mixer (' + mb + ' mB per core)'),
    use('Ascension Altars, Essence Condensers, or drinking (sneak + right-click)')])
  for (let tier = 0; tier <= global.FA_condensedTiers(); tier++) {
    details(global.FA_condensedId(tier), tier == 0 ? 'An ocean of essence in one block.' : 'Heavier than it has any right to be.', () => [
      made(tier == 0 ? 'an Essence Condenser' : FA.condensed.ratio + ' of the tier below, pressed in a Basin or in a crafting grid'),
      Text.gray('Worth: ').append(Text.lightPurple(ess(global.FA_condensedValue(tier)))).append(Text.darkGray(' (right-click to absorb)')),
      tip(tier == 0 ? 'Melts back into ' + FA.condensed.mbPerBlock + ' mB in a heated mixer' : 'Unpacks into ' + FA.condensed.ratio + ' of the tier below'),
      tier < global.FA_condensedTiers() ? up(FA.condensed.ratio + ' of them -> ' + global.FA_condensedName(tier + 1) + ' (Basin under a Press, or a crafting grid)')
        : tip('The top tier there is')])
  }

  // Gear tiers in order, and how each tier's ingot is made
  let tierMade = {
    voidsteel: 'Diamond + 2 Ascendite Ingots + Obsidian in a heated mixer (makes 2)',
    celestium: 'Netherite Ingot + 2 Voidsteel Ingots + a Dense Ascension Core, superheated (makes 2)',
    ascendium: '2 Celestium Ingots + an Ascension Singularity + 4 Mythic Fragments + 1,000 mB Liquid Essence, superheated'
  }
  let tierOrder = ['ascendite'].concat(Object.keys(FA.gearTiers))
  let nextTier = t => {
    let n = tierOrder[tierOrder.indexOf(t) + 1]
    return n ? up(FA.gearTiers[n].name + ' gear, from ' + FA.gearTiers[n].name + ' Ingots: ' + tierMade[n]) : tip('The top gear tier')
  }

  // Ascendite
  let O = A.ore
  ;['kubejs:ascendite_ore', 'kubejs:deepslate_ascendite_ore'].forEach(id => details(id, 'Teal veins that glimmer in torchlight.', () => [
    from('Overworld, Y ' + O.minY + ' to ' + O.maxY + ' (most common around Y ' + Math.round((O.minY + O.maxY) / 2) + '). Stone pickaxe or better.'),
    tip('Drops Raw Ascendite (more with Fortune) and a ' + pct(O.shardChance) + ' chance of an Ascension Shard (+' + pct(O.shardFortune) + ' per Fortune level)'),
    tip('Silk Touch keeps the ore. Crushing it gives extra ore, XP and sometimes a shard.')]))
  details('kubejs:raw_ascendite', 'Heavy, cold, and faintly humming.', () => [
    from('mining Ascendite Ore, or pressing ' + A.pressCobble + ' Cobblestone in a Basin (Mechanical Press)'),
    use('smelting into an Ingot, crushing (Crushed Raw Ascendite + XP, sometimes a shard), or a Millstone (' + pct(A.millShardChance) + ' shard chance)')])
  details('kubejs:crushed_raw_ascendite', 'Ground fine, ready for the furnace or the wash.', () => [
    made('crushing or milling Raw Ascendite or its ore, or blowing Crushed Raw Iron through soul fire (' + pct(A.hauntChance) + ')'),
    use('smelting into an Ingot, or washing (fan through water) into 9 Nuggets + Redstone (50%)')])
  details('kubejs:ascendite_ingot', 'Lighter than iron, tougher than it looks.', () => [
    made('smelting Raw or Crushed Ascendite, or 9 Nuggets'),
    use('Ascendite tools and armor, Ascension Cores, Precision Mechanisms, Blocks of Ascendite'), nextTier('ascendite')])
  details('kubejs:ascendite_nugget', 'A little goes a long way.', () => [
    made('washing Crushed Raw Ascendite (9 each), or splitting an Ingot'),
    use('Ascension Cores (' + A.mixerNuggets + ' + Redstone in a Mechanical Mixer, no heat), 2 Brass from 1 Copper + 1 Zinc, double Andesite Alloy' + (Platform.isLoaded('tfmg') ? ', early TFMG Steel (heated mixer)' : ''))])
  details('kubejs:ascension_shard', 'A sliver of crystallized ascension.', () => [
    from('mining Ascendite Ore (' + pct(O.shardChance) + ', more with Fortune), crushing it, or milling Raw Ascendite (' + pct(A.millShardChance) + ')'),
    use(A.shardsPerCore + ' make an Ascension Core (crafting grid, or pressed in a Basin)')])
  details('kubejs:ascendite_block', 'A solid block of teal metal.', () => [
    made('9 Ascendite Ingots'), use('a Blaze Burner without catching a blaze (+ Empty Blaze Burner + Magma Block), beacon bases')])
  let T = A.tools
  ;['sword', 'pickaxe', 'axe', 'shovel', 'hoe'].forEach(g => details('kubejs:ascendite_' + g, 'Forged from Ascendite: light, sharp, and ready to ascend.', () => [
    Text.gray('Tier: ').append(Text.aqua('between iron and diamond')).append(Text.darkGray('  ' + T.uses + ' durability, ' + (T.level >= 2 ? 'mines diamonds' : 'mines iron'))),
    made('Ascendite Ingots + Sticks, like iron tools. Repair with Ascendite Ingots in an anvil.'),
    tip('Takes Ascended enchants like any gear: hold a tome in your other hand and right-click'), nextTier('ascendite')]))
  // Attachments
  let fitsText = { helmet: 'Helmets', chest: 'Chestplates', legs: 'Leggings', boots: 'Boots', weapon: 'Weapons (swords, axes...)', bow: 'Bows and crossbows', tool: 'Pickaxes, shovels and hoes' }
  FA.attachments.list.forEach(at => details('kubejs:attachment_' + at.id, 'Some assembly required.', () => [
    Text.gray('Fits: ').append(Text.white(fitsText[at.fits])).append(Text.gray('  \u2192  ')).append(Text.gold(global.FA_attachmentEffect(at))),
    Text.gray('Use: ').append(Text.white('hold the gear in your other hand and right-click this')),
    tip('Up to ' + FA.attachments.slots + ' attachments per item. Take one off with [Detach] in /ascension.'),
    made(itemList(at.recipe) + ' (crafting grid)')].concat(at.charge ? [Text.yellow('\u26a1 Powered: ').append(Text.white('uses ' + at.charge + ' charge each time, from a battery you carry (' +
      FA.attachments.batteries.map(b => b.name).join(', ') + ')'))] : [])))

  // Mini Crusher motors
  let MCm = FA.machines.miniCrusher
  let motorLore = ['It hums. It spins. It crushes.', 'Teal sparks fly when it spins up.', 'It spins so fast it sings.']
  MCm.motors.forEach((m, i) => details('kubejs:' + m.id, motorLore[i] || 'Spins.', () => [
    Text.gray('Use: ').append(Text.white('put it in a Mini Crusher\'s motor slot: crushes at x' + m.speed + ' speed')),
    made(itemList(m.recipe[0].join('').split('').filter(c => c != ' ').map(c => m.recipe[1][c])) + ' (crafting grid)'),
    MCm.motors[i + 1] ? up('the ' + MCm.motors[i + 1].name + ' (x' + MCm.motors[i + 1].speed + ') is made from this one') : tip('The fastest motor')]))

  // Batteries
  let batteryLore = ['Two copper plates and a lot of hope.', 'Hums quietly with teal light.', 'A whole storm in a glass jar.']
  FA.attachments.batteries.forEach((b, i) => details('kubejs:' + b.id, batteryLore[i] || 'Fully charged.', item => [
    Text.gray('Charge: ').append(Text.aqua(global.FA_num(Math.max(0, b.charge - (item && item.getDamageValue ? item.getDamageValue() : 0))) + ' / ' + global.FA_num(b.charge)))
      .append(Text.darkGray('  (the bar shows what is left)')),
    Text.gray('Use: ').append(Text.white('carry it: powered attachments draw charge from it')),
    tip('Powered: ' + FA.attachments.list.filter(a => a.charge).map(a => a.name + ' (' + a.charge + ')').join(', ')),
    made(itemList(b.recipe) + ' (crafting grid)'),
    tip('Recharge: the battery + ' + itemList(b.recharge) + ' in a crafting grid or a Mechanical Mixer (no heat)'),
    FA.attachments.batteries[i + 1] ? up('the ' + FA.attachments.batteries[i + 1].name + ' holds ' + global.FA_num(FA.attachments.batteries[i + 1].charge) + ': ' + itemList(FA.attachments.batteries[i + 1].recipe))
      : tip('The biggest battery')]))

  // Foil Sheets
  FA.foils.types.forEach((f, i) => details('kubejs:foil_sheet_' + f.id, ['Cheap shine, honest work.', 'Polished to a mirror.', 'Every collector wants one.', 'It shifts color as you turn it.',
    'Splits light into every color.', 'A whole night sky, pressed thin.', 'It drinks the light around it.', 'Forged from the light of a dying star.',
    'Sealed in red glass, never touched by hand.', 'Deep blue, like looking into the sea.', 'Green fire frozen behind glass.', 'Honey-gold light, trapped forever.',
    'The northern lights, in a box.', 'Black glass that hums when you hold it.', 'Still warm. The case is fireproof.', 'Frost creeps across the case.',
    'A galaxy being born behind the glass.', 'The sun, swallowed. Only the crown remains.'][i] || 'Shiny.', () => [
    Text.gray('Foil: ').append(Text.gold(f.name + ', x' + f.mult + ' level')).append(Text.darkGray('  (#' + (i + 1) + ' of ' + FA.foils.types.length + (f.cased ? ', collector case' : '') + ')')),
    Text.gray('Use: ').append(Text.white('hold an enchant tome in your other hand and right-click this')),
    tip('A tome with a foil goes on gear at its level x ' + f.mult + ', and its cover shows the foil. A better foil replaces a worse one.'),
    made(itemList(f.recipe) + ' (crafting grid)'),
    FA.foils.types[i + 1] ? up('the ' + FA.foils.types[i + 1].name + ' Foil (x' + FA.foils.types[i + 1].mult + '): ' + itemList(FA.foils.types[i + 1].recipe)) : tip('The best foil there is')]))

  // Card packs
  let PK = FA.packs
  details('kubejs:enchantment_pack', 'Sealed in shimmering foil. What\'s inside?', () => [
    Text.gray('Right-click to open: ').append(Text.white(PK.cards + ' enchant tome cards')).append(Text.darkGray(PK.lastCardRare ? ' (the last one is Rare or better)' : '')),
    tip(PK.rarities.map(r => r.name + ' ' + r.weight + '%').join(', ')),
    tip('Holo cards and up come pre-leveled. Found in dungeon, temple, stronghold and end city chests, and danger-zone loot.')])
  details('kubejs:booster_box', 'The whole box. Every collector\'s dream.', () => [
    Text.gray('Right-click to open: ').append(Text.white(PK.boxPacks + ' Enchantment Packs')),
    tip('Found in the best chests (end cities, ancient cities, bastions, mansions) and Epic+ danger-zone loot')])

  // Sealed Gear Cache
  details('kubejs:gear_cache', 'Something heavy shifts inside when you shake it.', () => [
    Text.gray('Right-click to break the seal: ').append(Text.white('one piece of relic gear (weapon, armor or a pickaxe)')),
    tip('Rare (iron) and Epic (diamond) gear is common; Legendary and Mythic get likelier the more dangerous the place you open it'),
    tip('Found in dungeon, temple, stronghold, village smithy, nether, bastion, mansion, ancient city and end city chests')])

  // Factory Room TNT
  let RC = FA.roomCharges
  RC.styles.forEach(st => RC.sizes.forEach((sz, i) => {
    details('kubejs:room_tnt_' + st.id + '_' + sz.id, 'Some assembly required. Not much, though.', () => [
      Text.gray('Right-click a block: after ' + RC.fuseSeconds + 's it blasts out a ').append(Text.white(sz.size[0] + ' x ' + sz.size[1] + ' room, ' + sz.size[2] + ' high inside')),
      tip('In front of you, floor level with your feet, with a ' + RC.doorway[0] + '-wide doorway where you stand'),
      tip('Floor, walls and roof lined with ' + faItemName(st.shell) + ', ' + faItemName(st.light) + ' set into the roof every ' + RC.lightSpacing + ' blocks'),
      tip('Never breaks bedrock, chests or machines' + (RC.collectOres ? '. Ores it blasts through drop in the middle of the room' : '')),
      made(i == 0 ? 'TNT, 4 ' + faItemName(st.shell) + ', 2 ' + faItemName(st.light) + ' (crafting grid)'
        : sz.craft + ' of the ' + RC.sizes[i - 1].size[0] + 'x' + RC.sizes[i - 1].size[1] + ' ' + st.name + ' TNT'),
      RC.sizes[i + 1] ? up(RC.sizes[i + 1].craft + ' of these -> a ' + RC.sizes[i + 1].size[0] + 'x' + RC.sizes[i + 1].size[1] + ' room (crafting grid)') : tip('The biggest room size')])
  }))

  // Polishing Cloth
  details('kubejs:polishing_cloth', 'A little elbow grease goes a long way.', () => [
    Text.gray('Use: ').append(Text.white('hold gear in your other hand and right-click the cloth')),
    tip('Rerolls the gear\'s condition and keeps whichever grade is better'),
    tip('Grades: ' + FA.condition.grades.map(g => g.name + ' ' + g.weight + '%').join(', ')),
    made('White Wool + 2 Ascendite Nuggets + Slime Ball (makes 2)')])

  // Rock Hammers
  let HM = FA.hammer
  let rockNames = Object.keys(HM.rocks).map(faItemName).join(', ')
  Object.keys(HM.tools).forEach(hid => {
    let T = HM.tools[hid]
    details(hid, hid.indexOf('ascendite') >= 0 ? 'It hums when it hits something valuable.' : 'Every miner starts somewhere.', () => [
      Text.gray('Hand: ').append(Text.white('hammer in your main hand, rocks in the other, right-click')).append(Text.darkGray(' (sneak: ' + HM.sneakCount + ' at once)')),
      Text.gray('Block: ').append(Text.white('right-click a placed rock to smash it in place. A Create Deployer can do it too.')),
      tip('Smashes: ' + rockNames),
      tip('Cobblestone: gravel, flint, iron, copper, zinc and gold nuggets, coal, sometimes Raw Ascendite'),
      tip(T.uses + ' uses' + (T.luck != 1 ? ', x' + T.luck + ' luck on every drop' : '') + ', ' + (T.cooldown / 20).toFixed(2) + 's between smashes'),
      made(hid.indexOf('ascendite') >= 0 ? '5 Ascendite Ingots + 2 Sticks' : '5 Iron Ingots + 2 Sticks'),
      hid.indexOf('ascendite') >= 0 ? tip('The best hammer') : up('the Ascendite Rock Hammer: 5 Ascendite Ingots + 2 Sticks (more uses, more luck, faster)')])
  })

  // Higher gear tiers
  let tierPlace = { voidsteel: 'between diamond and netherite', celestium: 'stronger than netherite', ascendium: 'far beyond netherite (endgame)' }
  let tierLore = { voidsteel: 'Darker than night, lighter than it looks.', celestium: 'It catches starlight even underground.', ascendium: 'Still warm from the forge that made it.' }
  Object.keys(FA.gearTiers).forEach(t => {
    let G = FA.gearTiers[t], GT = G.tools, GA = G.armor
    details('kubejs:' + t + '_ingot', tierLore[t], () => [made(tierMade[t]), use(G.name + ' tools and armor (' + tierPlace[t] + ')'), nextTier(t)])
    ;['sword', 'pickaxe', 'axe', 'shovel', 'hoe'].forEach(g => details('kubejs:' + t + '_' + g, tierLore[t], () => [
      Text.gray('Tier: ').append(Text.aqua(tierPlace[t])).append(Text.darkGray('  ' + GT.uses + ' durability, +' + GT.attackDamageBonus + ' damage bonus')),
      made(G.name + ' Ingots + Sticks, like iron tools. Repair with ' + G.name + ' Ingots in an anvil.'),
      tip('Takes Ascended enchants like any gear'), nextTier(t)]))
    let GP = GA.protection, slotIx = { boots: 0, leggings: 1, chestplate: 2, helmet: 3 }
    Object.keys(slotIx).forEach(g => details('kubejs:' + t + '_' + g, tierLore[t], () => [
      Text.gray('This piece: ').append(Text.aqua(GP[slotIx[g]] + ' armor, ' + GA.toughness + ' toughness'))
        .append(Text.darkGray('  (all 4 pieces: ' + (GP[0] + GP[1] + GP[2] + GP[3]) + ' armor, ' + 4 * GA.toughness + ' toughness)')),
      made(G.name + ' Ingots, like iron armor. Repair with ' + G.name + ' Ingots in an anvil.'),
      tip('Takes Ascended enchants like any gear'), nextTier(t)]))
  })

  // Mob food
  let MF = FA.mobFood
  let pretty = id => String(id).replace(/^[a-z0-9_]+:/, '').split('_').map(w => w.charAt(0).toUpperCase() + w.slice(1)).join(' ')
  let roman = n => ['I', 'II', 'III', 'IV', 'V'][n] || String(n + 1)
  let tomesNeeding = item => Object.keys(global.FA_TOME_CATALYSTS).filter(t => global.FA_TOME_CATALYSTS[t].indexOf(item) >= 0).map(t => FA.enchants[t].name)
  MF.foods.forEach((fd, i) => {
    let mobs = fd.mobs.map(pretty).join(', ')
    let needs = item => {
      let t = tomesNeeding(item)
      return t.length ? [use('the ' + t.join(', ') + ' tome' + (t.length > 1 ? 's' : ''))] : []
    }
    let L = FA_FOOD_LORE[fd.raw] || ['Smells awful. Definitely cook this.', 'Crispy outside, surprisingly good inside.']
    details('kubejs:' + fd.raw, L[0], () => [
      from(mobs + ' (' + pct(fd.chance) + ', +' + pct(MF.lootingBonus) + ' per Looting level)'),
      Text.red('Raw: ' + MF.raw.sickness.map(e => pretty(e[0]) + ' ' + roman(e[2]) + (e[3] < 1 ? ' (' + pct(e[3]) + ')' : '')).join(', ')),
      tip('Cook it in a furnace, smoker or campfire: ' + fd.cookedName)].concat(needs('kubejs:' + fd.raw)))
    details('kubejs:' + fd.cooked, L[1], () => [
      Text.green('+' + fd.hunger + ' hunger, ' + pretty(fd.effect[0]) + (fd.effect[2] > 0 ? ' ' + roman(fd.effect[2]) : '') + ' for ' + fd.effect[1] + 's'),
      made('cooking ' + fd.rawName + ', or kill the ' + fd.mobs.map(pretty)[0] + ' while it burns'),
      tip('You can eat it even when full')].concat(needs('kubejs:' + fd.cooked)))
  })

  let P = A.armor.protection, slots = { boots: 0, leggings: 1, chestplate: 2, helmet: 3 }
  let total = P[0] + P[1] + P[2] + P[3]
  Object.keys(slots).forEach(g => details('kubejs:ascendite_' + g, 'Forged from Ascendite: light, sharp, and ready to ascend.', () => [
    Text.gray('This piece: ').append(Text.aqua(P[slots[g]] + ' armor, ' + A.armor.toughness + ' toughness'))
      .append(Text.darkGray('  (all 4 pieces: ' + total + ' armor, ' + 4 * A.armor.toughness + ' toughness)')),
    made('Ascendite Ingots, like iron armor. Repair with Ascendite Ingots in an anvil.'),
    tip('Takes Ascended enchants like any gear: hold a tome in your other hand and right-click'), nextTier('ascendite')]))
  // ---- The Factory Ascension mod's blocks and spawn eggs (Full download) ----
  // Pets and charms (kubejs items, both downloads)
  FA.pets.collars.forEach((c, i) => details('kubejs:' + c.id, 'A good pet deserves good gear.', () => [
    use('right-clicking your own tamed pet: ' + c.health + ' health, ' + c.attack + ' attack, ' + c.armor + ' armor, and no fire or fall damage'),
    tip(i == 0 ? 'Wolves, cats, parrots, horses... any pet you tamed' : 'It must already wear a ' + FA.pets.collars[i - 1].name)]))
  details('kubejs:unbinding_scroll', 'Let it go.', () => [use('taking Soulbound off: hold the gear in your other hand and right-click'),
    made('Paper, Ender Pearls and an Ascension Shard (crafting grid)')])
  details('kubejs:pet_whistle', 'Here, boy!', () => [use('calling your pets within ' + FA.pets.whistleRange + ' blocks to you (sitting ones stay put)')])
  FA.charms.list.forEach(c => details('kubejs:' + c.id, c.id == 'ascended_charm' ? 'Every charm, at once, stronger.' : 'Keep it in your pocket.', () => [
    use(c.effects.length ? 'carrying it anywhere in your inventory: ' + c.effects.map(fx => faItemName(fx[0]).replace(/^minecraft:/, '').replace(/_/g, ' ') + ' ' + (fx[1] + 1)).join(', ')
      : 'carrying it anywhere in your inventory'),
    c.magnet ? tip('Pulls items within ' + c.magnet + ' blocks to you') : tip('The strongest charm of each kind counts'),
    tip(c.id == 'ascended_charm' ? 'Made from eight charms and an Ascended Soul' : 'Gold around ' + faItemName(c.extra) + ' and a core')]))

  if (Platform.isLoaded('factoryascension')) {
    let M = FA.machines, EN = M.engine
    // "4 Iron Ingot, 2 Shaft" from a shaped recipe in the config
    let shapedList = r => itemList(r[0].join('').split('').filter(c => c != ' ').map(c => r[1][c]))
    let block = (id, lore, lines) => details('factoryascension:' + id, lore, () => lines().concat([made(shapedList(M.recipes[id]) + ' (crafting grid)')]))
    let tierSteps = (ups, what) => ups.map((u, i) => up('tier ' + (i + 2) + ' (' + what(i + 1) + '): right-click it with ' + u[1] + ' ' + faItemName(u[0])))

    block('ascension_vault', 'Room for everything you will ever loot.', () => [
      use('a storage chest with a search bar, a Sort button and a scroll bar. Hoppers and Create funnels work.'),
      tip('Starts at 108 slots. Breaking it drops what is inside.')].concat(tierSteps(M.vaultUpgrades, t => 108 * (t + 1) + ' slots')))
    block('super_smelter', 'A furnace that never learned to wait.', () => [
      use('smelting a whole batch at once. Shaft in the back: the faster it spins, the faster it goes'),
      tip('Tier 1: ' + M.smelter.tiers[0][0] + ' items at once. A batch takes ' + (M.smelter.cookTicksAt64Rpm / 20) + 's at 64 RPM. Needs at least ' + M.smelter.minRpm + ' RPM.')]
      .concat(tierSteps(M.smelter.upgrades, t => M.smelter.tiers[t][0] + ' items at once')))
    block('salvager', 'One adventurer\'s junk is another\'s cores.', () => [
      use('/ascension sell as a machine: loot in the top row, Ascension Cores out of the bottom row'),
      tip(M.salvager.itemsPerRun + ' items every ' + M.salvager.seconds + 's at the same prices. What it can\'t sell passes straight through.'),
      tip(M.salvager.artifacts ? 'It will grind up Artifacts too.' : 'It never grinds up an Artifact.')])
    block('tome_fusion_table', 'Two books go in. One better book comes out.', () => [
      use('merging copies of one tome (their levels add up), or making a Fusion or Triad from its parent tomes'),
      tip('A Fusion costs ' + M.fusionTable.fusionCores + ' core, a Triad ' + M.fusionTable.triadCores + '. It shows the result before you commit.')])
    block('card_binder', 'Every collector needs a binder.', () => [
      use('holding 54 tome cards. Shows how much of the collection you have'), tip('Floats your best card above the block')])
    block('essence_forge', 'Gear goes in plain and comes out legendary.', () => [
      use('levelling the gear in its slot from its Liquid Essence tank (16 buckets), once a second'),
      tip('Same rules as the Ascension Altar. Pipe or pour the Liquid Essence in.')])
    block('pack_opener', 'Somebody has to open all those packs.', () => [
      use('opening Enchantment Packs (and Booster Boxes), one every ' + M.openerSeconds + 's, into 27 output slots'),
      tip('Hoppers work. It flips the cards on its screen.')])

    // Modular Engine
    let speeds = EN.rpm.join(', ')
    block('engine_core', 'The beating heart of your first factory.', () => [
      use('Create power from day one: put a shaft on its front face'),
      tip('Coils, Gearboxes and Burners touching it (or each other) make the engine. Up to ' + EN.maxModules + ' modules.'),
      tip('Right-click it for the engine screen: speed, power, modules and what to add next'),
      up('more power: +1 Coil (+' + EN.suPerCoil + ' SU) and a Burner for every ' + EN.coilsPerBurner + ' Coils'),
      up('more speed: +1 Gearbox (' + speeds + ' RPM)')])
    block('engine_coil', 'Copper wound tight around a spark.', () => [
      use('+' + EN.suPerCoil + ' SU of stress capacity, while a lit Burner powers it'),
      tip('Each lit Burner powers ' + EN.coilsPerBurner + ' Coils. Its screen shows how many are powered.'),
      up('add more Coils: every one is +' + EN.suPerCoil + ' SU (up to ' + EN.maxModules + ' modules in all)')])
    block('engine_gearbox', 'Same power, faster shaft.', () => [
      use('speed: each Gearbox takes the engine to its next speed (' + speeds + ' RPM)'),
      tip('The SU stays the same at any speed. Its screen shows the speed ladder.'),
      up('add another Gearbox for the next speed, up to ' + EN.rpm[EN.rpm.length - 1] + ' RPM with ' + (EN.rpm.length - 1))])
    block('engine_burner', 'Feed it coal and it will feed your factory.', () => [
      use('burning any furnace fuel to power ' + EN.coilsPerBurner + ' Coils' + (EN.fuelMultiplier != 1 ? ' (fuel lasts x' + EN.fuelMultiplier + ')' : '')),
      tip('Right-click with fuel to fill it, or open its screen. Hoppers and chutes fill it. Spare Burners wait.'),
      up('one more Burner for every ' + EN.coilsPerBurner + ' more Coils')])

    // Ascension backpacks (with Sophisticated Backpacks)
    let BPT = FA.backpacks.tiers
    let bpLore = ['Teal-trimmed and roomy.', 'Bigger on the inside than the void.', 'Stars stitched into the lining.', 'Forged straps, endless pockets.',
      'A whole factory fits in here.', 'You will never run out of room again.']
    BPT.forEach((t, i) => details('factoryascension:' + t.id + '_backpack', bpLore[i] || 'Huge.', () => [
      Text.gray('Holds: ').append(Text.white(global.FA_num(t.slots) + ' slots')).append(Text.gray(', ')).append(Text.white(t.upgrades + ' upgrade slots'))
        .append(Text.darkGray('  (a Netherite Backpack: 120 and 7)')),
      tip('A real Sophisticated backpack: every Sophisticated upgrade works in it. Wear it, place it, dye it.'),
      made(shapedList([t.recipe[0], Object.assign({ B: i == 0 ? 'sophisticatedbackpacks:netherite_backpack' : 'factoryascension:' + BPT[i - 1].id + '_backpack' }, t.recipe[1])]) +
        ' (keeps everything inside)'),
      BPT[i + 1] ? up('the ' + global.FA_num(BPT[i + 1].slots) + '-slot tier: ' + shapedList([BPT[i + 1].recipe[0], Object.assign({ B: 'this backpack' }, BPT[i + 1].recipe[1])]))
        : tip('The biggest backpack there is')]))

    // Quarry, Mob Grinder, Warp Hopper
    let QY = M.quarry, GR = M.grinder
    block('ascension_quarry', 'It digs so you don\'t have to.', () => [
      use('mining out the ' + (2 * QY.radius + 1) + ' x ' + (2 * QY.radius + 1) + ' square under it, layer by layer, to the bottom of the world'),
      tip('Turn the shaft in its back (at least ' + QY.minRpm + ' RPM, ' + QY.stress + ' SU per RPM). Faster spins dig faster.'),
      tip('Mines like a diamond pickaxe; skips bedrock, fluids, chests and machines. Put a chest on top for the drops.'),
      tip('Right-click: which layer it\'s on')])
    block('mob_grinder', 'The factory fights back.', () => [
      use('killing mobs in the ' + GR.reach + ' x 5 x 3 box in front of it: ' + GR.damagePerRpm + ' damage per RPM a hit'),
      tip('Counts as your kill: rare drops and loot rolls. Never hits players, villagers, pets or named mobs.'),
      tip('Turn the shaft in its back (at least ' + GR.minRpm + ' RPM, ' + GR.stress + ' SU per RPM). Put a chest under it for the drops.')])
    block('warp_hopper', 'Hoppers, but everywhere.', () => [
      use('sending items anywhere: what goes in comes out in the chest under every other Warp Hopper with the same key'),
      tip('Right-click with any item to key it to that item. Any distance, any dimension (both ends loaded).'),
      tip('Several receivers take turns. Feed it with a hopper, funnel or chute.')])

    // Shotgun and shells
    let SG = FA.guns.shotgun
    details('factoryascension:shotgun', 'Pump action. Factory made.', () => [
      use('hold right-click to aim down the sights, left-click to fire (hold it to keep firing), R to reload'),
      tip(SG.pellets + ' pellets x ' + SG.pelletDamage + ' damage, ' + SG.capacity + ' shells, up to ' + SG.range + ' blocks; aiming tightens the spread'),
      tip('Your attack: Ascended tomes go on it like any weapon, and your gear counts. Shatters glass.'),
      made(shapedList(SG.recipe) + ' (crafting grid)')])
    details('factoryascension:shotgun_shell', 'Mind the recoil.', () => [
      use('loading the Shotgun (R reloads one shell at a time from your inventory)'),
      made(FA.guns.shellsPerCraft + ' from ' + shapedList(FA.guns.shells) + ' (crafting grid)')])

    // The other guns and their rounds
    FA.guns.list.forEach(g => details('factoryascension:' + g.id, g.lore, () => [
      use('hold right-click to aim, left-click to fire, R to reload (stressed hands fumble reloads)'),
      tip('Skins, attachments, Akimbo and Gangster grips: the Weapons Workbench'),
      made((g.cost || []).map(c => c[1] + ' ' + faItemName(c[0])).join(', ') + ' (Weapons Workbench, GUNSMITH tab)')]))
    details('factoryascension:weapons_workbench', 'Tape, tools and a very steady hand.', () => [
      use('building guns (GUNSMITH: all ' + FA.guns.list.length + ' of them) and putting attachments on them: thousands, in twelve slots (muzzle, barrel, handguard, foregrip, optic, stock, magazine, underbarrel, tactical, grip, trigger, charm), Akimbo kits and Gangster grips included'),
      tip('Skins (SKINS tab): animated finishes like Obsidian, Snow, Black Water and Torn, some with 3D parts'),
      tip('Ammo Press: any item as the case and any item as the powder, any caliber, type and colour'),
      tip('A Jury-Rigged gun (enchanted book, at an anvil) takes any attachment anywhere, up to four a slot'),
      made(shapedList(FA.guns.workbench.recipe) + ' (crafting grid)')])
    details('factoryascension:frag_grenade', 'Pull pin. Count. Throw.', () => [
      use('hold right-click to pull the pin (it cooks while you hold it), let go to throw'),
      tip('Bounces and rolls; goes off about 3 seconds after the pin. Hold it too long and it goes off in your hand.'),
      made(FA.guns.fragGrenadesPerCraft + ' from ' + shapedList(FA.guns.fragGrenade) + ' (crafting grid)')])
    details('factoryascension:bandage', 'Press hard. Wrap tight.', () => [
      use('hold right-click for 2 seconds: stops Bleeding, heals a heart'),
      made(FA.guns.bandagesPerCraft + ' from ' + shapedList(FA.guns.bandage) + ' (crafting grid)')])
    details('factoryascension:medkit', 'Everything you need, if you have time.', () => [
      use('hold right-click for 4 seconds: stops Bleeding and poison, heals 3 hearts and more over time'),
      made(shapedList(FA.guns.medkit) + ' (crafting grid)')])
    details('factoryascension:dog_tag', 'Someone, once.', () => [
      tip('Dropped by every armed operator you put down: their name, what they were, who killed them and when')])
    // The armed operators' spawn eggs
    Object.keys(FA.npcs.kinds).forEach(id => details('factoryascension:' + id + '_spawn_egg', FA.npcs.kinds[id], () => [
      from('patrols out in the danger zones, the structures out there, or (hunters) a red flare'),
      tip('Hears gunshots and comes to look; takes cover to reload; drops its gun, rounds, kit and dog tag'),
      tip('Right-click a block to spawn one (Creative)')]))
    details('factoryascension:attachment', 'Bolts on. Probably.', () => [
      use('a gun, at the Weapons Workbench (take it off again with a right-click on its slot)')])
    details('factoryascension:cartridge', 'One round. Any caliber, any type, any making.', () => [
      use('loading a gun of its caliber (R with the gun in hand)'),
      tip('Plain rounds: a nugget, gunpowder and a copper ingot, plus the caliber\'s marker item (crafting grid)'),
      tip('Custom rounds (any item as propellant and case, any type and colour): the Weapons Workbench')])

    // Flare gun, flares, and the duffel bag of the Tarkov inventory
    let FG = FA.guns.flareGun
    details('factoryascension:flare_gun', 'Somebody, somewhere, is watching the sky.', () => [
      use('left-click fires the loaded flare, R loads one (R again swaps red for green)'),
      tip('Red: calls for rescue. ' + Math.round(FG.rescueChance * 100) + '% of the time a helicopter comes ' + FG.rescueSeconds + 's later and takes you home with everything you carry, if you stay within ' + FG.rescueRadius + ' blocks. Everyone armed out there sees it too.'),
      tip('Green: calls an airdrop, a supply crate that falls ' + FG.airdropSeconds + 's later, 8 blocks ahead, under green smoke'),
      made(shapedList(FG.recipe) + ' (crafting grid)')])
    details('factoryascension:flare_red', 'Hopefully someone sees it.', () => [
      use('the Flare Gun: a call for rescue (a helicopter, maybe)'),
      made(FA.guns.flares.perCraft + ' from ' + shapedList(FA.guns.flares.red) + ' (crafting grid)')])
    details('factoryascension:flare_green', 'Supplies inbound.', () => [
      use('the Flare Gun: calls an airdrop crate full of loot'),
      made(FA.guns.flares.perCraft + ' from ' + shapedList(FA.guns.flares.green) + ' (crafting grid)')])
    details('factoryascension:duffel_bag', 'Everything you own, zipped up.', () => [
      tip('The bag your inventory comes out of when you open it (not an item you can get)')])

    // The Ascended Realm
    block('realm_gate', 'A door with a sky on the other side.', () => [
      use('right-click: go to the Ascended Realm, floating islands where danger starts at ' + FA.danger.realmBonus),
      tip('You arrive at the same x, z on a Realm Stone platform, with a gate there that brings you back here'),
      tip('Its ores: Essence Crystal (Ascension Cores), Voidsteel (ingots) and Astral (Mythic Fragments)')])
    let realmOre = (id, lore, drop) => details('factoryascension:' + id, lore, () => [
      tip('Found in the Ascended Realm (through a Realm Gate)'), tip('Drops ' + drop + ' (more with Fortune; Silk Touch keeps the ore)')])
    realmOre('essence_crystal_ore', 'The Realm grows cores like crystals.', '1-3 Ascension Cores')
    realmOre('voidsteel_ore', 'Darker than the void it came from.', 'a Voidsteel Ingot')
    realmOre('astral_ore', 'Starlight, set in stone.', '1-2 Mythic Fragments')
    ;['realm_stone', 'realm_moss', 'realm_crystal'].forEach(id => details('factoryascension:' + id, 'From the Ascended Realm.', () => [
      tip('The floating islands of the Ascended Realm are made of it (reached through a Realm Gate)')]))

    // Mini Crusher
    let MCR = M.miniCrusher
    block('mini_crusher', 'All the crushing, none of the wheels.', () => [
      use('crushing like Create\'s Crushing Wheels (or a Millstone), in one block with no rotation'),
      tip('Needs a motor in its motor slot and a battery in its battery slot: ' + MCR.chargePerItem + ' charge per item'),
      tip('Hoppers and funnels feed it from any side (batteries go in the battery slot) and take results out'),
      up('a faster motor: ' + MCR.motors.map(m => m.name + ' x' + m.speed).join(', '))])

    // Ascension Workbench
    let LG = FA.legendary
    block('ascension_workbench', 'Where good gear becomes legendary.', () => [
      use('putting tomes, gems, attachments, Chaos Tomes and Polishing Cloths on gear: gear on top, the item below, Apply'),
      tip('Two blocks wide. It says what Apply will do before you press it.'),
      Text.gold('\u2605 Legendary stars: ').append(Text.white('Star Up adds a star, up to ' + LG.maxStars + '. Each star adds a legendary effect')),
      tip('  (a random enchant that suits the gear, level ' + LG.effectLevels[0] + ' at star 1 up to ' + global.FA_num(LG.effectLevels[LG.maxStars - 1]) + ' at star ' + LG.maxStars + ')'),
      tip('  and makes every level on the item count x' + (1 + LG.bonusPerStar).toFixed(1) + ' per star (x' + global.FA_legendMult(LG.maxStars).toFixed(1) + ' at ' + LG.maxStars + ')'),
      up('star 1: ' + LG.costs[0].map(c => c[1] + ' ' + faItemName(c[0])).join(', ') + ' ... star ' + LG.maxStars + ': ' + LG.costs[LG.maxStars - 1].map(c => c[1] + ' ' + faItemName(c[0])).join(', '))])

    // Universal Storage Terminal and its linker
    let ST = M.storage
    block('storage_terminal', 'Every chest you own, one click away.', () => [
      use('seeing every chest linked to it as one big chest: search, Sort, scroll, take and put'),
      tip('Link chests with a Storage Linker: up to ' + ST.maxLinks + ' chests, barrels, vaults or Create vaults' + (ST.range > 0 ? ' within ' + ST.range + ' blocks' : '')),
      tip('Hoppers and funnels can feed it: items go into the first linked chest with room. Double chests count once.'),
      up('more room: link more chests (or link Ascension Vaults and upgrade those)')])
    block('storage_linker', 'One end in your hand, the other in every chest.', () => [
      Text.gray('Use: ').append(Text.white('right-click a Universal Storage Terminal to set it, then right-click chests to link or unlink them')),
      tip('While you hold it, the terminal sparkles gold and its chests teal'),
      tip('Sneak + right-click the air to clear it')])

    // Connected glass
    let glassLore = { factory_glass: 'Clean steel-framed panes for any factory.', ascendite_glass: 'Teal-tinted, with an Ascendite frame.',
      gilded_glass: 'Warm glass trimmed in gold.', smoked_glass: 'Dark glass: see out, not in.' }
    Object.keys(M.glass).forEach(g => details('factoryascension:' + g, glassLore[g] || 'Clear as day.', () => [
      tip('Connects to the same glass next to it: one big pane, framed only round the edge'),
      made('8 Glass around ' + faItemName(M.glass[g]) + ' (crafting grid, makes 8)')]))

    // Spawn eggs: where the mob lives, what it does, what it drops
    Object.keys(global.FA_MOB_INFO).forEach(id => {
      let info = global.FA_MOB_INFO[id], boss = FA.modMobs.bosses[id], min = FA.modMobs.minDanger[id] || 0
      let parts = MF.foods.filter(fd => fd.mobs.indexOf('factoryascension:' + id) >= 0)
      details('factoryascension:' + id + '_spawn_egg', info.what, () => {
        let lines = [from(info.where + (min > 0 && info.where.indexOf('danger') < 0 ? ', danger ' + min + '+' : ''))]
        parts.forEach(fd => lines.push(Text.gray('Part: ').append(Text.white(fd.rawName + ' (' + pct(fd.chance) + ')')).append(Text.darkGray(', cooks into ' + fd.cookedName))))
        if (boss) lines.push(Text.red('Boss: ').append(Text.white('boss bar, minions at 2/3 and 1/3 health, enrages at 1/4')),
          tip(pct(boss.chance) + ' of its spawns go through, never within ' + boss.spacing + ' blocks of another'))
        lines.push(tip('Right-click a block to spawn one (Creative, or on a spawner)'))
        return lines
      })
    })
  }
})

// Display name of any item, for recipe lines
function faItemName(id) {
  try {
    return String(Item.of(id).getHoverName().getString())
  } catch (err) {
    return String(id).replace(/^[a-z0-9_]+:/, '').replace(/_/g, ' ')
  }
}
