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

// Vanilla chests: a pack (and in the best chests, a box) as an extra roll
ServerEvents.chestLootTables(event => {
  let C = global.FA.packs.chests
  Object.keys(C).forEach(table => {
    let chances = C[table]
    // KubeJS adds the chests/ folder itself: 'minecraft:chests/simple_dungeon' -> 'minecraft:simple_dungeon'
    event.modify(table.replace(':chests/', ':'), loot => {
      ;[['kubejs:enchantment_pack', chances[0]], ['kubejs:booster_box', chances[1]]].forEach(e => {
        if (!(e[1] > 0)) return
        loot.addPool(pool => {
          pool.rolls = 1
          pool.addItem(e[0]).weight(Math.max(1, Math.round(e[1] * 1000)))
          if (e[1] < 1) pool.addEmpty(Math.round((1 - e[1]) * 1000))
        })
      })
    })
  })
})
