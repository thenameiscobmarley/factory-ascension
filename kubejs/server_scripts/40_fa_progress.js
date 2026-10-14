// priority: 600
// Factory Ascension - milestones, Prestige (Rebirth), gems and Liquid Essence buckets.
// Uses the helpers from 10_fa_ascension.js (same server script scope).

// ---- Stats + milestones ----
// Stats are saved per player in the world's KubeJS data: fa_stat_<stat>_<uuid>.
// Milestones are checked whenever a stat goes up, and again at login (stats can grow while you're
// offline, e.g. from your altars). Each milestone pays out once: fa_ms_<id>_<uuid> marks it done.

function faStatGet(server, uuid, stat) {
  let v = server.persistentData.getDouble('fa_stat_' + stat + '_' + uuid)
  return isFinite(v) && v > 0 ? v : 0
}

// mode 'add' adds amount, 'max' keeps the highest value seen
function faStatBump(server, uuid, stat, amount, mode) {
  if (!(amount > 0) || !isFinite(amount)) return
  let old = faStatGet(server, uuid, stat)
  let next = mode == 'max' ? Math.max(old, amount) : Math.min(1e300, old + amount)
  if (next != old) server.persistentData.putDouble('fa_stat_' + stat + '_' + uuid, next)
}

function faCheckMilestones(player) {
  let server = player.getServer()
  let uuid = player.stringUuid
  global.FA.milestones.forEach(m => {
    let key = 'fa_ms_' + m.id + '_' + uuid
    if (server.persistentData.getBoolean(key) || faStatGet(server, uuid, m.stat) < m.goal) return
    // Mark it done first, so a crash while handing out rewards can never pay it twice
    server.persistentData.putBoolean(key, true)
    m.reward.forEach(r => player.give(Item.of(r[0], r[1])))
    let name = player.username
    server.runCommandSilent('title ' + name + ' times 5 50 15')
    server.runCommandSilent('title ' + name + ' subtitle ' + JSON.stringify({ text: m.name, color: 'gold' }))
    server.runCommandSilent('title ' + name + ' title ' + JSON.stringify({ text: 'MILESTONE', color: 'yellow', bold: true }))
    faSound(player, 'minecraft:ui.toast.challenge_complete', 1)
    player.tell(Text.gold('★ Milestone: ' + m.name + ' ').append(Text.gray('(' + m.reward.map(r => r[1] + 'x ' + r[0].replace(/^\w+:/, '').replace(/_/g, ' ')).join(', ') + ')')))
  })
}

// One entry point for every stat change (startup hooks call it through global.FA_onStat)
function faStatRecord(player, stat, amount, mode) {
  try {
    faStatBump(player.getServer(), player.stringUuid, stat, amount, mode || 'add')
    faCheckMilestones(player)
  } catch (err) {
    console.error('[Factory Ascension] stat ' + stat + ' failed: ' + err)
  }
}
global.FA_onStat = faStatRecord

function faShowMilestones(player) {
  let server = player.getServer()
  let uuid = player.stringUuid
  let done = 0
  player.tell(Text.gold('Milestones').bold(true))
  global.FA.milestones.forEach(m => {
    let have = faStatGet(server, uuid, m.stat)
    let finished = server.persistentData.getBoolean('fa_ms_' + m.id + '_' + uuid)
    if (finished) done++
    let pct = Math.min(1, have / m.goal)
    let bar = '█'.repeat(Math.round(pct * 10)) + '░'.repeat(10 - Math.round(pct * 10))
    player.tell((finished ? Text.green(' ✔ ') : Text.gray(' • ')).append(Text.white(m.name + ' '))
      .append(Text.darkGray(bar + ' ' + global.FA_num(Math.min(have, m.goal)) + ' / ' + global.FA_num(m.goal) + ' ' + m.stat.replace(/_/g, ' '))))
  })
  player.tell(Text.gray(done + ' of ' + global.FA.milestones.length + ' done'))
  return 1
}

PlayerEvents.loggedIn(event => {
  try {
    faCheckMilestones(event.player)
  } catch (err) {
    console.error('[Factory Ascension] milestone check failed: ' + err)
  }
})

// ---- Prestige (Rebirth) ----

// Sum of every enchant + rolled level on the item (gems don't count: they aren't reset)
function faTotalLevels(stack) {
  let total = 0
  Object.keys(global.FA.enchants).forEach(id => { total += faGetLevel(stack, id) })
  faRolledSlots(stack).forEach(r => { total += r.level })
  return total
}

function faStars(stack) {
  let tag = stack.nbt
  let stars = tag && tag.contains(FA_ITEM_TAG) ? tag.getCompound(FA_ITEM_TAG).getInt('stars') : 0
  return stars > 0 && stars <= 1000000 ? stars : 0
}

function faRebirth(player, confirmed) {
  let gear = faTarget(player)
  if (!gear) {
    player.tell(Text.red('Hold the item you want to Rebirth.'))
    return 0
  }
  let P = global.FA.prestige
  let stars = faStars(gear)
  let need = global.FA_rebirthNeed(stars)
  let have = faTotalLevels(gear)
  if (P.maxStars > 0 && stars >= P.maxStars) {
    player.tell(Text.yellow('This item already has the most stars allowed.'))
    return 0
  }
  if (have < need) {
    player.tell(Text.red('Rebirth needs ' + global.FA_num(need) + ' total levels on this item (it has ' + global.FA_num(have) + ').'))
    return 0
  }
  if (!confirmed) {
    player.tell(Text.gold('Rebirth this item? ').append(Text.gray('Every enchant goes back to level 1, and the item gains a star: all its levels then count x' +
      global.FA_starMult(stars + 1).toFixed(2) + ' forever. Spent Essence is not refunded. ')).append(faButton('Confirm', '/ascension rebirth confirm', 'Reset to level 1 for a permanent star')))
    return 1
  }

  // Snapshot so it can be undone exactly if anything fails
  let before = gear.nbt.copy()
  try {
    Object.keys(global.FA.enchants).forEach(id => { if (faGetLevel(gear, id) > 0) faSetLevel(gear, id, 1) })
    faRolledSlots(gear).forEach(r => faSetRolled(gear, r.slot, r.seed, 1))
    let tag = gear.getOrCreateTag()
    let data = tag.getCompound(FA_ITEM_TAG)
    data.putInt('stars', stars + 1)
    tag.put(FA_ITEM_TAG, data)
  } catch (err) {
    gear.nbt = before
    console.error('[Factory Ascension] Rebirth failed and was undone: ' + err)
    player.tell(Text.red('Something went wrong, nothing was changed. (See the server log.)'))
    return 0
  }
  faSound(player, 'minecraft:item.totem.use', 1)
  player.tell(Text.gold('★'.repeat(Math.min(stars + 1, 10)) + ' Reborn! ').append(Text.gray('Every level on this item now counts x' + global.FA_starMult(stars + 1).toFixed(2) + '.')))
  faStatRecord(player, 'rebirths', 1)
  return 1
}

// ---- Gems ----

function faGems(stack) {
  let tag = stack.nbt
  if (!tag || !tag.contains(FA_ITEM_TAG)) return []
  let data = tag.getCompound(FA_ITEM_TAG)
  if (!data.contains('gems')) return []
  let gems = data.getCompound('gems')
  let out = []
  for (let i = 0; i < global.FA.gems.sockets; i++) {
    if (!gems.contains(String(i))) continue
    let g = gems.getCompound(String(i))
    let type = String(g.getString('type'))
    let tier = g.getInt('tier')
    if (global.FA.gems.types[type] && tier >= 1 && tier <= 3) out.push({ slot: i, type: type, tier: tier })
  }
  return out
}

function faSocket(player, gemStack, type, tier, gear) {
  if (!faIsGear(gear)) {
    player.displayClientMessage(Text.red('Hold the gear you want to socket in your other hand.'), true)
    return
  }
  let G = global.FA.gems
  let gems = faGems(gear)
  let slot
  if (gems.length < G.sockets) {
    for (slot = 0; slot < G.sockets; slot++) if (!gems.some(g => g.slot == slot)) break
  } else if (player.isShiftKeyDown()) {
    slot = gems[gems.length - 1].slot
  } else {
    player.displayClientMessage(Text.yellow('All ' + G.sockets + ' sockets are full. Sneak + right-click to replace the newest gem (it will be destroyed).'), true)
    return
  }
  let before = gear.nbt ? gear.nbt.copy() : null
  try {
    let tag = gear.getOrCreateTag()
    let data = tag.getCompound(FA_ITEM_TAG)
    let all = data.getCompound('gems')
    let g = all.getCompound(String(slot))
    g.putString('type', type)
    g.putInt('tier', tier)
    all.put(String(slot), g)
    data.put('gems', all)
    tag.put(FA_ITEM_TAG, data)
  } catch (err) {
    gear.nbt = before
    console.error('[Factory Ascension] socketing failed and was undone: ' + err)
    return
  }
  // The gem is only used up once it's safely in the socket
  gemStack.shrink(1)
  faSound(player, 'minecraft:block.amethyst_block.place', 1.2)
  let e = global.FA.enchants[G.types[type].enchant]
  player.displayClientMessage(Text.aqua(G.tiers[tier - 1].name + ' ' + G.types[type].name + ' socketed: +' + global.FA_gemLevels(tier) + ' ' + e.name), true)
  faStatRecord(player, 'gems_socketed', 1)
}

Object.keys(global.FA.gems.types).forEach(type => {
  for (let tier = 1; tier <= 3; tier++) {
    let t = tier
    ItemEvents.rightClicked(global.FA_gemId(type, tier), event => {
      let player = event.player
      let gear = String(event.hand) == 'MAIN_HAND' ? player.getOffhandItem() : player.getMainHandItem()
      faSocket(player, event.item, type, t, gear)
      event.cancel()
    })
  }
})

// ---- Liquid Essence bucket: drink it for Essence (1 bucket = 1000 mB = 10 Essence by default) ----

ItemEvents.rightClicked('kubejs:liquid_essence_bucket', event => {
  let player = event.player
  if (!player.isShiftKeyDown()) return   // sneak to drink; a plain right-click places it like any bucket
  let gain = 1000 / global.FA.altar.mbPerEssence
  let stack = event.item
  stack.shrink(1)
  try {
    faSetEssence(player, faGetEssence(player) + gain)
    player.give(Item.of('minecraft:bucket'))
    faStatRecord(player, 'essence_absorbed', gain)
    player.displayClientMessage(Text.lightPurple('+' + global.FA_num(gain) + ' Essence  ').append(Text.gray('(bank: ' + global.FA_num(faGetEssence(player)) + ')')), true)
    faSound(player, 'minecraft:item.honey_bottle.drink', 0.8)
  } catch (err) {
    player.give(Item.of('kubejs:liquid_essence_bucket'))
    console.error('[Factory Ascension] drinking Liquid Essence failed, bucket returned: ' + err)
  }
  event.cancel()
})
