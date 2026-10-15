// priority: 840
// Factory Ascension - Enchantment card packs (config: packs).
// Right-click a foil Enchantment Pack: 5 tome "cards" are revealed one by one (the last is Rare or better).
// Right-click a Booster Box for a stack of packs. Packs and boxes turn up in vanilla chests and danger-zone loot.

const FA_CARD_TEXT = { white: 'white', green: 'green', aqua: 'aqua', light_purple: 'lightPurple', gold: 'gold', red: 'red' }

function faCardText(rarity, s) {
  return Text[FA_CARD_TEXT[global.FA.packs.rarities[rarity].color] || 'white'](s)
}

// The cards go straight into your inventory (so nothing is lost if you log out); the reveal plays over a couple of seconds
function faOpenPack(player, pack) {
  let P = global.FA.packs
  pack.shrink(1)
  let cards = global.FA_openPack(Math.random)
  let server = player.getServer()
  let name = player.username
  cards.forEach(c => {
    let tome = faMakeTome(c.id, c.level)
    if (c.foil >= 0) global.FA_setFoil(tome, c.foil)
    let R = P.rarities[c.rarity]
    if (R.holo) {
      let tag = tome.getOrCreateTag()
      let data = tag.getCompound(FA_ITEM_TAG)
      data.putString('card', R.name)
      tag.put(FA_ITEM_TAG, data)
    }
    player.give(tome)
  })
  faSound(player, 'minecraft:item.book.page_turn', 1.6)
  cards.forEach((c, i) => {
    server.scheduleInTicks(8 + i * 10, () => {
      let R = P.rarities[c.rarity]
      let e = global.FA.enchants[c.id]
      let line = faCardText(c.rarity, (R.holo ? '✦ ' : '') + R.name + ': ').append(Text.white(e.name + (c.level > 1 ? ' ' + global.FA_num(c.level) : '')))
      if (c.foil >= 0) {
        let F = global.FA.foils.types[c.foil]
        line = line.append(Text.gold('  [' + F.name + ' Foil x' + F.mult + ']'))
        if (c.foil >= 5) faSound(player, 'minecraft:block.amethyst_block.resonate', 0.6)
      }
      player.tell(Text.darkGray('  Card ' + (i + 1) + '/' + cards.length + '  ').append(line))
      faSound(player, R.holo ? 'minecraft:ui.toast.challenge_complete' : 'minecraft:entity.experience_orb.pickup', R.holo ? 1 : 0.8 + c.rarity * 0.15)
      if (R.holo) {
        server.runCommandSilent('title ' + name + ' times 3 30 10')
        server.runCommandSilent('title ' + name + ' subtitle ' + JSON.stringify({ text: e.name + (c.level > 1 ? ' ' + c.level : ''), color: 'white' }))
        server.runCommandSilent('title ' + name + ' title ' + JSON.stringify({ text: '✦ ' + R.name.toUpperCase() + ' ✦', color: R.color, bold: true }))
        server.runCommandSilent('execute at ' + name + ' run particle minecraft:totem_of_undying ~ ~1 ~ 0.5 0.8 0.5 0.4 60 normal')
      }
      if (c.rarity == P.rarities.length - 1) server.tell(Text.red('✦ ' + name + ' pulled a Secret Rare: ').append(Text.gold(e.name + '!')))
    })
  })
  return cards
}

ItemEvents.rightClicked('kubejs:enchantment_pack', event => {
  faOpenPack(event.player, event.item)
  event.cancel()
})

ItemEvents.rightClicked('kubejs:booster_box', event => {
  let n = global.FA.packs.boxPacks
  event.item.shrink(1)
  event.player.give(Item.of('kubejs:enchantment_pack', n))
  faSound(event.player, 'minecraft:block.barrel.open', 1.2)
  event.player.tell(Text.lightPurple('You crack open the box: ').append(Text.white(n + ' Enchantment Packs')).append(Text.lightPurple('!')))
  event.cancel()
})

// Sealed Gear Cache: one piece of relic gear, better the more dangerous the place you open it
const FA_RELIC_TEXT = { aqua: 'aqua', light_purple: 'lightPurple', gold: 'gold', red: 'red' }

function faOpenCache(player, cache) {
  let danger = global.FA_dangerLevel(player.level, player.getX(), player.getZ(), player)
  let quality = global.FA_cacheQuality(Math.random, danger)
  let relic = global.FA_makeRelic(Math.random, quality, danger)
  let stack = global.FA_relicStack(relic)
  let AR = global.FA.artifacts
  let art = quality == 'mythic' && AR.enabled && Math.random() < AR.relicChance ? global.FA_makeArtifact(stack, Math.random, player.getServer()) : false
  cache.shrink(1)
  player.give(stack)
  let label = art ? art.name + ', ' + art.title : relic.name
  let color = FA_RELIC_TEXT[relic.color] || 'white'
  player.tell(Text.gray('The seal breaks: ').append(Text[color](label)).append(Text.darkGray('  (' + quality + (danger > 0 ? ', danger ' + danger : '') + ')')))
  let big = quality == 'legendary' || quality == 'mythic'
  faSound(player, big ? 'minecraft:ui.toast.challenge_complete' : 'minecraft:block.chest.open', big ? 1 : 1.2)
  if (big) {
    let server = player.getServer()
    let name = player.username
    server.runCommandSilent('title ' + name + ' times 3 40 10')
    server.runCommandSilent('title ' + name + ' subtitle ' + JSON.stringify({ text: label, color: relic.color }))
    server.runCommandSilent('title ' + name + ' title ' + JSON.stringify({ text: quality.toUpperCase() + ' GEAR', color: relic.color, bold: true }))
    if (quality == 'mythic') server.tell(Text.red(name + ' broke open a Mythic cache: ').append(Text.gold(label)))
  }
  return { quality: quality, stack: stack }
}
global.FA_openCacheFor = (player, cache) => faOpenCache(player, cache)

ItemEvents.rightClicked('kubejs:gear_cache', event => {
  faOpenCache(event.player, event.item)
  event.cancel()
})

// Vanilla chests: packs, boxes and gear caches as extra rolls (each table modified once)
ServerEvents.chestLootTables(event => {
  let pools = {}
  let add = (table, item, chance) => {
    if (table.indexOf('factoryascension:') == 0 && !Platform.isLoaded('factoryascension')) return   // its structures need the mod
    if (chance > 0) (pools[table] = pools[table] || []).push([item, chance])
  }
  let P = global.FA.packs.chests
  Object.keys(P).forEach(t => { add(t, 'kubejs:enchantment_pack', P[t][0]); add(t, 'kubejs:booster_box', P[t][1]) })
  let C = global.FA.caches.chests
  Object.keys(C).forEach(t => { add(t, 'kubejs:gear_cache', C[t][0]); add(t, 'kubejs:gear_cache', C[t][1]) })
  let J = global.FA.jackpot
  Object.keys(pools).forEach(table => {
    // KubeJS adds the chests/ folder itself: 'minecraft:chests/simple_dungeon' -> 'minecraft:simple_dungeon'
    event.modify(table.replace(':chests/', ':'), loot => {
      pools[table].forEach(e => {
        loot.addPool(pool => {
          pool.rolls = 1
          pool.addItem(e[0]).weight(Math.max(1, Math.round(e[1] * 1000)))
          if (e[1] < 1) pool.addEmpty(Math.round((1 - e[1]) * 1000))
        })
      })
      // Jackpot: a rare huge stack (40 packs, a pile of caches...)
      if (J.chestChance > 0) {
        loot.addPool(pool => {
          pool.rolls = 1
          let total = J.chestStacks.reduce((a, j) => a + j[3], 0)
          J.chestStacks.forEach(j => {
            pool.addItem(j[0]).weight(Math.max(1, Math.round(J.chestChance * 100000 * j[3] / total)))
              .addFunction({ function: 'minecraft:set_count', count: { type: 'minecraft:uniform', min: j[1], max: j[2] } })
          })
          pool.addEmpty(Math.round((1 - J.chestChance) * 100000))
        })
      }
    })
  })
})
