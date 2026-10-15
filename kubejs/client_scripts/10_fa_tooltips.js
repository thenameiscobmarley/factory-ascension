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
      if (event.isShift()) lines().forEach(l => text.add(l))
      else text.add(Text.darkGray('Hold ').append(Text.yellow('Shift')).append(Text.darkGray(' for details')))
    })
  }
  let made = s => Text.gray('Made: ').append(Text.white(s))
  let from = s => Text.gray('Found: ').append(Text.white(s))
  let use = s => Text.gray('Used for: ').append(Text.white(s))
  let tip = s => Text.darkGray(s)
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
    tip('  or 2 Ascendite Ingots + Redstone + 250 mB Lava (heated mixer)')
  ]
  details('kubejs:ascension_core', 'Hums with the power of a whole factory.', () => coreWays.concat([
    use('Essence, Blank Tomes, Fusion and Chaos Tomes, Liquid Essence (' + mb + ' mB each)')]))
  details('kubejs:dense_ascension_core', "Nine cores crushed into one. It's warm to the touch.", () => [
    made('9 Ascension Cores pressed in a heated Basin'), tip('Unpack back into 9 in a crafting grid'),
    use('the Ascension Altar, the Essence Condenser, ' + 9 * mb + ' mB of Liquid Essence')])
  details('kubejs:ascension_singularity', 'A pinpoint of pure progress. Do not drop.', () => [
    made('9 Dense Cores pressed in a superheated Basin'), tip('Unpack into 9 Dense Cores in a crafting grid'),
    use(ess(81) + ' in one click, or a stack of them for ' + ess(81 * 64))])
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
    from('danger-zone kills. Elites: ' + pct(F.eliteChance) + ' + ' + pct(F.eliteChancePerLevel) + ' per danger level (max ' + pct(F.eliteChanceCap) + ')'),
    tip('  Other mobs: ' + (F.normalChancePerLevel * 100).toFixed(1) + '% per danger level (max ' + pct(F.normalChanceCap) + '). Bosses drop several.'),
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
        tip(tier < 3 ? '4 of these press into one ' + FA.gems.tiers[tier].name + ' ' + T.name : 'Worth ' + global.FA_gemLevels(3) / global.FA_gemLevels(1) + ' Chipped gems in one socket')])
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
      tip(tier == 0 ? 'Melts back into ' + FA.condensed.mbPerBlock + ' mB in a heated mixer' : 'Unpacks into ' + FA.condensed.ratio + ' of the tier below')])
  }

  // Ascendite
  let O = A.ore
  ;['kubejs:ascendite_ore', 'kubejs:deepslate_ascendite_ore'].forEach(id => details(id, 'Teal veins that glimmer in torchlight.', () => [
    from('Overworld, Y ' + O.minY + ' to ' + O.maxY + ' (most common around Y ' + Math.round((O.minY + O.maxY) / 2) + '). Stone pickaxe or better.'),
    tip('Drops Raw Ascendite (more with Fortune) and a ' + pct(O.shardChance) + ' chance of an Ascension Shard (+' + pct(O.shardFortune) + ' per Fortune level)'),
    tip('Silk Touch keeps the ore. Crushing it gives extra ore, XP and sometimes a shard.')]))
  details('kubejs:raw_ascendite', 'Heavy, cold, and faintly humming.', () => [
    from('mining Ascendite Ore, or pressing ' + A.pressCobble + ' Cobblestone in a Basin (Mechanical Press)'),
    use('smelting into an Ingot, or crushing (Crushed Raw Ascendite + XP, sometimes a shard)')])
  details('kubejs:crushed_raw_ascendite', 'Ground fine, ready for the furnace or the wash.', () => [
    made('crushing Raw Ascendite or its ore, or blowing Crushed Raw Iron through soul fire (' + pct(A.hauntChance) + ')'),
    use('smelting into an Ingot, or washing (fan through water) into 9 Nuggets + Redstone (50%)')])
  details('kubejs:ascendite_ingot', 'Lighter than iron, tougher than it looks.', () => [
    made('smelting Raw or Crushed Ascendite, or 9 Nuggets'),
    use('Ascendite tools and armor, Ascension Cores, Precision Mechanisms, Blocks of Ascendite')])
  details('kubejs:ascendite_nugget', 'A little goes a long way.', () => [
    made('washing Crushed Raw Ascendite (9 each), or splitting an Ingot'),
    use('2 Brass from 1 Copper + 1 Zinc, double Andesite Alloy' + (Platform.isLoaded('tfmg') ? ', early TFMG Steel (heated mixer)' : ''))])
  details('kubejs:ascension_shard', 'A sliver of crystallized ascension.', () => [
    from('mining Ascendite Ore (' + pct(O.shardChance) + ', more with Fortune), and sometimes from crushing it'),
    use(A.shardsPerCore + ' make an Ascension Core (crafting grid, or pressed in a Basin)')])
  details('kubejs:ascendite_block', 'A solid block of teal metal.', () => [
    made('9 Ascendite Ingots'), use('a Blaze Burner without catching a blaze (+ Empty Blaze Burner + Magma Block), beacon bases')])
  let T = A.tools
  ;['sword', 'pickaxe', 'axe', 'shovel', 'hoe'].forEach(g => details('kubejs:ascendite_' + g, 'Forged from Ascendite: light, sharp, and ready to ascend.', () => [
    Text.gray('Tier: ').append(Text.aqua('between iron and diamond')).append(Text.darkGray('  ' + T.uses + ' durability, ' + (T.level >= 2 ? 'mines diamonds' : 'mines iron'))),
    made('Ascendite Ingots + Sticks, like iron tools. Repair with Ascendite Ingots in an anvil.'),
    tip('Takes Ascended enchants like any gear: hold a tome in your other hand and right-click')]))
  // Attachments
  let fitsText = { helmet: 'Helmets', chest: 'Chestplates', legs: 'Leggings', boots: 'Boots', weapon: 'Weapons (swords, axes...)', bow: 'Bows and crossbows', tool: 'Pickaxes, shovels and hoes' }
  FA.attachments.list.forEach(at => details('kubejs:attachment_' + at.id, 'Some assembly required.', () => [
    Text.gray('Fits: ').append(Text.white(fitsText[at.fits])).append(Text.gray('  \u2192  ')).append(Text.gold(at.text.replace('{p}', String(at.power)))),
    Text.gray('Use: ').append(Text.white('hold the gear in your other hand and right-click this')),
    tip('Up to ' + FA.attachments.slots + ' attachments per item. Take one off with [Detach] in /ascension.'),
    made(itemList(at.recipe) + ' (crafting grid)')]))

  // Foil Sheets
  FA.foils.types.forEach((f, i) => details('kubejs:foil_sheet_' + f.id, ['Cheap shine, honest work.', 'Polished to a mirror.', 'Every collector wants one.', 'It shifts color as you turn it.',
    'Splits light into every color.', 'A whole night sky, pressed thin.', 'It drinks the light around it.', 'Forged from the light of a dying star.'][i] || 'Shiny.', () => [
    Text.gray('Foil: ').append(Text.gold(f.name + ', x' + f.mult + ' level')).append(Text.darkGray('  (#' + (i + 1) + ' of ' + FA.foils.types.length + ')')),
    Text.gray('Use: ').append(Text.white('hold an enchant tome in your other hand and right-click this')),
    tip('A tome with a foil goes on gear at its level x ' + f.mult + ', and its cover shows the foil. A better foil replaces a worse one.'),
    made(itemList(f.recipe) + ' (crafting grid)')]))

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
      made(hid.indexOf('ascendite') >= 0 ? '5 Ascendite Ingots + 2 Sticks' : '5 Iron Ingots + 2 Sticks')])
  })

  // Higher gear tiers
  let tierMade = {
    voidsteel: 'Diamond + 2 Ascendite Ingots + Obsidian in a heated mixer (makes 2)',
    celestium: 'Netherite Ingot + 2 Voidsteel Ingots + a Dense Ascension Core, superheated (makes 2)',
    ascendium: '2 Celestium Ingots + an Ascension Singularity + 4 Mythic Fragments + 1,000 mB Liquid Essence, superheated'
  }
  let tierPlace = { voidsteel: 'between diamond and netherite', celestium: 'stronger than netherite', ascendium: 'far beyond netherite (endgame)' }
  let tierLore = { voidsteel: 'Darker than night, lighter than it looks.', celestium: 'It catches starlight even underground.', ascendium: 'Still warm from the forge that made it.' }
  Object.keys(FA.gearTiers).forEach(t => {
    let G = FA.gearTiers[t], GT = G.tools, GA = G.armor
    details('kubejs:' + t + '_ingot', tierLore[t], () => [made(tierMade[t]), use(G.name + ' tools and armor (' + tierPlace[t] + ')')])
    ;['sword', 'pickaxe', 'axe', 'shovel', 'hoe'].forEach(g => details('kubejs:' + t + '_' + g, tierLore[t], () => [
      Text.gray('Tier: ').append(Text.aqua(tierPlace[t])).append(Text.darkGray('  ' + GT.uses + ' durability, +' + GT.attackDamageBonus + ' damage bonus')),
      made(G.name + ' Ingots + Sticks, like iron tools. Repair with ' + G.name + ' Ingots in an anvil.'),
      tip('Takes Ascended enchants like any gear')]))
    let GP = GA.protection, slotIx = { boots: 0, leggings: 1, chestplate: 2, helmet: 3 }
    Object.keys(slotIx).forEach(g => details('kubejs:' + t + '_' + g, tierLore[t], () => [
      Text.gray('This piece: ').append(Text.aqua(GP[slotIx[g]] + ' armor, ' + GA.toughness + ' toughness'))
        .append(Text.darkGray('  (all 4 pieces: ' + (GP[0] + GP[1] + GP[2] + GP[3]) + ' armor, ' + 4 * GA.toughness + ' toughness)')),
      made(G.name + ' Ingots, like iron armor. Repair with ' + G.name + ' Ingots in an anvil.'),
      tip('Takes Ascended enchants like any gear')]))
  })

  // Mob food
  let MF = FA.mobFood
  let pretty = id => String(id).replace(/^[a-z0-9_]+:/, '').split('_').map(w => w.charAt(0).toUpperCase() + w.slice(1)).join(' ')
  let roman = n => ['I', 'II', 'III', 'IV', 'V'][n] || String(n + 1)
  let tomesNeeding = item => Object.keys(global.FA_TOME_CATALYSTS).filter(t => global.FA_TOME_CATALYSTS[t].indexOf(item) >= 0).map(t => FA.enchants[t].name)
  let rawLore = ['Smells awful. Definitely cook this.', 'Still twitching a little.', 'Who would eat this raw?']
  let cookedLore = ['Crispy outside, surprisingly good inside.', 'Somehow it tastes like adventure.', 'Grandma would be proud.']
  MF.foods.forEach((fd, i) => {
    let mobs = fd.mobs.map(pretty).join(', ')
    let needs = item => {
      let t = tomesNeeding(item)
      return t.length ? [use('the ' + t.join(', ') + ' tome' + (t.length > 1 ? 's' : ''))] : []
    }
    details('kubejs:' + fd.raw, rawLore[i % rawLore.length], () => [
      from(mobs + ' (' + pct(fd.chance) + ', +' + pct(MF.lootingBonus) + ' per Looting level)'),
      Text.red('Raw: ' + MF.raw.sickness.map(e => pretty(e[0]) + ' ' + roman(e[2]) + (e[3] < 1 ? ' (' + pct(e[3]) + ')' : '')).join(', ')),
      tip('Cook it in a furnace, smoker or campfire: ' + fd.cookedName)].concat(needs('kubejs:' + fd.raw)))
    details('kubejs:' + fd.cooked, cookedLore[i % cookedLore.length], () => [
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
    tip('Takes Ascended enchants like any gear: hold a tome in your other hand and right-click')]))
})

// Display name of any item, for recipe lines
function faItemName(id) {
  try {
    return String(Item.of(id).getHoverName().getString())
  } catch (err) {
    return String(id).replace(/^[a-z0-9_]+:/, '').replace(/_/g, ' ')
  }
}
