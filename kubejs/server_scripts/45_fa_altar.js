// priority: 500
// Factory Ascension - Ascension Altar and Essence Condenser: machines that run on Liquid Essence.
//
// Ascension Altar: the factory levels your gear while you're away.
//
//   [ Create Depot ]   <- put gear here (or let a belt/arm deliver it)
//   [ Ascension Altar ]
//   [ tank / basin ]   <- Liquid Essence, piped in from your melting line
//
// Once a second each altar pays for one level at a time (lowest enchant first) with Liquid Essence,
// up to altar.maxLevelsPerSecond. Costs are the same as /ascension: 1 Essence = altar.mbPerEssence mB.
// Altars are remembered in the world's KubeJS data, with whoever placed them (for the Automation milestone).

const $FAAltarPos = Java.loadClass('net.minecraft.core.BlockPos')
const $FACaps = Java.loadClass('net.minecraftforge.common.capabilities.ForgeCapabilities')
const $FADirection = Java.loadClass('net.minecraft.core.Direction')
const $FAFluidAction = Java.loadClass('net.minecraftforge.fluids.capability.IFluidHandler$FluidAction')
const $FAItemEntityS = Java.loadClass('net.minecraft.world.entity.item.ItemEntity')

const FA_ALTAR_ID = 'kubejs:ascension_altar'
const FA_CONDENSER_ID = 'kubejs:essence_condenser'

// Liquid Essence for fluid handlers. Use KubeJS's Fluid.of: a Forge FluidStack passed from a script reaches the
// tank empty (checked on a live server), so every fill/drain would silently move nothing.
function faEssence(mb) {
  return Fluid.of('kubejs:liquid_essence', mb)
}

function faAltarKey(dim, pos) {
  return dim + '|' + pos.getX() + '|' + pos.getY() + '|' + pos.getZ()
}

function faAltars(server) {
  let data = server.persistentData
  if (!data.contains('fa_altars')) data.put('fa_altars', data.getCompound('fa_altars'))
  return data.getCompound('fa_altars')
}

BlockEvents.placed(FA_ALTAR_ID, event => {
  try {
    let altars = faAltars(event.server)
    if (altars.getAllKeys().size() >= global.FA.altar.maxAltars) {
      if (event.player) event.player.displayClientMessage(Text.red('This world already has the most altars allowed (' + global.FA.altar.maxAltars + ').'), true)
      event.cancel()
      return
    }
    let owner = event.player ? event.player.stringUuid : ''
    altars.putString(faAltarKey(String(event.level.dimension), event.block.pos), owner)
    if (event.player) event.player.displayClientMessage(Text.lightPurple('Altar ready: Depot on top, Liquid Essence tank or basin below.'), true)
  } catch (err) {
    console.error('[Factory Ascension] registering an altar failed: ' + err)
  }
})

BlockEvents.broken(FA_ALTAR_ID, event => {
  try {
    faAltars(event.server).remove(faAltarKey(String(event.level.dimension), event.block.pos))
  } catch (err) {
    console.error('[Factory Ascension] removing an altar failed: ' + err)
  }
})

// The next upgrade for an item: its lowest enchant (static or rolled) that can still go up
function faNextAltarUpgrade(gear) {
  let best = null
  Object.keys(global.FA.enchants).forEach(id => {
    let level = faGetLevel(gear, id)
    if (level <= 0 || level >= faMaxLevel(id)) return
    if (!best || level < best.level) best = { kind: 'enchant', id: id, level: level, cost: global.FA_cost(level, id) }
  })
  faRolledSlots(gear).forEach(r => {
    if (r.level >= FA_MAX_LEVEL) return
    if (!best || r.level < best.level) best = { kind: 'rolled', slot: r.slot, seed: r.seed, level: r.level, cost: global.FA_rollCost(r.level, global.FA_rollEnchant(r.seed).rarity) }
  })
  return best
}

function faApplyAltarUpgrade(gear, up, level) {
  if (up.kind == 'enchant') faSetLevel(gear, up.id, level)
  else faSetRolled(gear, up.slot, up.seed, level)
}

// Runs one altar for one second. Returns how many levels it gave.
function faRunAltar(level, pos) {
  let above = level.getBlockEntity(pos.above())
  let below = level.getBlockEntity(pos.below())
  if (!above || !below) return 0
  let items = above.getCapability($FACaps.ITEM_HANDLER, $FADirection.DOWN).resolve().orElse(null)
  let fluids = below.getCapability($FACaps.FLUID_HANDLER, $FADirection.UP).resolve().orElse(null)
  if (!items || !fluids) return 0
  return faLevelGear(level, pos, items, fluids)
}

// Levels the first upgradable gear in `items` from the Liquid Essence in `fluids`, for one second (the Ascension Altar,
// and the mod's Essence Forge in the Full download). Returns how many levels it gave.
function faLevelGear(level, pos, items, fluids) {
  let slot = -1
  for (let i = 0; i < items.getSlots(); i++) {
    let s = items.getStackInSlot(i)
    if (faIsGear(s) && faNextAltarUpgrade(s)) { slot = i; break }
  }
  if (slot < 0) return 0

  // Take the item out, upgrade it, put it back. Each level is paid with a real drain first;
  // if the drain comes up short the level is undone, so fluid and levels always match.
  let gear = items.extractItem(slot, 1, false)
  if (!gear || gear.isEmpty()) return 0
  let given = 0
  try {
    while (given < global.FA.altar.maxLevelsPerSecond) {
      let up = faNextAltarUpgrade(gear)
      if (!up) break
      let mb = Math.ceil(up.cost * global.FA.altar.mbPerEssence)
      if (!(mb > 0) || mb > 2000000000) break
      if (fluids.drain(faEssence(mb), $FAFluidAction.SIMULATE).getAmount() < mb) break
      faApplyAltarUpgrade(gear, up, up.level + 1)
      let got = fluids.drain(faEssence(mb), $FAFluidAction.EXECUTE).getAmount()
      if (got < mb) {
        faApplyAltarUpgrade(gear, up, up.level)
        break
      }
      given++
    }
  } finally {
    // Always give the item back; if the depot won't take it, drop it on top instead of losing it
    let rest = items.insertItem(slot, gear, false)
    if (rest && !rest.isEmpty()) {
      let drop = new $FAItemEntityS(level, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, rest)
      level.addFreshEntity(drop)
    }
  }
  if (given > 0) {
    let at = (pos.getX() + 0.5) + ' ' + (pos.getY() + 1.2) + ' ' + (pos.getZ() + 0.5)
    level.getServer().runCommandSilent('execute in ' + level.dimension + ' run particle minecraft:enchant ' + at + ' 0.3 0.4 0.3 0.6 ' + Math.min(80, 10 + given * 4) + ' normal')
  }
  return given
}

// ---- Essence Condenser ----
//
//   [ chest / Item Vault / any inventory ]   <- Condensed Essence Blocks come out here
//   [ Essence Condenser ]
//   [ tank / basin ]                         <- Liquid Essence
//
// Once a second it packs up to condensed.blocksPerSecond buckets into blocks. It checks how many blocks fit
// before draining, and only drains whole blocks' worth, so fluid is never lost.

function faCondensers(server) {
  let data = server.persistentData
  if (!data.contains('fa_condensers')) data.put('fa_condensers', data.getCompound('fa_condensers'))
  return data.getCompound('fa_condensers')
}

BlockEvents.placed(FA_CONDENSER_ID, event => {
  try {
    let all = faCondensers(event.server)
    if (all.getAllKeys().size() >= global.FA.condensed.maxCondensers) {
      if (event.player) event.player.displayClientMessage(Text.red('This world already has the most condensers allowed (' + global.FA.condensed.maxCondensers + ').'), true)
      event.cancel()
      return
    }
    all.putString(faAltarKey(String(event.level.dimension), event.block.pos), event.player ? event.player.stringUuid : '')
    if (event.player) event.player.displayClientMessage(Text.lightPurple('Condenser ready: chest or vault on top, Liquid Essence tank or basin below.'), true)
  } catch (err) {
    console.error('[Factory Ascension] registering a condenser failed: ' + err)
  }
})

BlockEvents.broken(FA_CONDENSER_ID, event => {
  try {
    faCondensers(event.server).remove(faAltarKey(String(event.level.dimension), event.block.pos))
  } catch (err) {
    console.error('[Factory Ascension] removing a condenser failed: ' + err)
  }
})

// Puts a stack into any slots that take it; returns what didn't fit
function faInsertAll(items, stack, simulate) {
  let rest = stack
  for (let i = 0; i < items.getSlots() && !faRestEmpty(rest); i++) rest = items.insertItem(i, rest, simulate)
  return faRestEmpty(rest) ? null : rest
}
// What's left over after inserting: undefined or an empty stack both mean "all of it went in"
function faRestEmpty(rest) {
  return !rest || rest.isEmpty()
}

// Runs one condenser for one second. Returns how many blocks it made.
function faRunCondenser(level, pos) {
  let above = level.getBlockEntity(pos.above())
  let below = level.getBlockEntity(pos.below())
  if (!above || !below) return 0
  let items = above.getCapability($FACaps.ITEM_HANDLER, $FADirection.DOWN).resolve().orElse(null)
  let fluids = below.getCapability($FACaps.FLUID_HANDLER, $FADirection.UP).resolve().orElse(null)
  if (!items || !fluids) return 0

  let C = global.FA.condensed
  let blockId = global.FA_condensedId(0)
  let made = 0
  // In stacks of at most 64, until the second's budget, the fluid or the space runs out
  while (made < C.blocksPerSecond) {
    let want = Math.min(64, C.blocksPerSecond - made)
    let available = Math.floor(fluids.drain(faEssence(want * C.mbPerBlock), $FAFluidAction.SIMULATE).getAmount() / C.mbPerBlock)
    let n = Math.min(want, available)
    if (n <= 0) break
    let leftover = faInsertAll(items, Item.of(blockId, n), true)
    n -= leftover ? leftover.getCount() : 0
    if (n <= 0) break
    let drained = fluids.drain(faEssence(n * C.mbPerBlock), $FAFluidAction.EXECUTE).getAmount()
    let blocks = Math.floor(drained / C.mbPerBlock)
    if (blocks <= 0) break
    // If the inventory changed in between, drop what doesn't fit on top instead of losing it
    let rest = faInsertAll(items, Item.of(blockId, blocks), false)
    if (rest) level.addFreshEntity(new $FAItemEntityS(level, pos.getX() + 0.5, pos.getY() + 2.2, pos.getZ() + 0.5, rest))
    made += blocks
    if (blocks < n) break
  }
  if (made > 0) {
    let at = (pos.getX() + 0.5) + ' ' + (pos.getY() + 0.6) + ' ' + (pos.getZ() + 0.5)
    level.getServer().runCommandSilent('execute in ' + level.dimension + ' run particle minecraft:reverse_portal ' + at + ' 0.4 0.4 0.4 0.02 ' + Math.min(60, 8 + made) + ' normal')
  }
  return made
}

// ---- One tick for every machine ----

// Runs `run` for every registered machine of one kind, forgetting ones that were removed some other way
// (explosions, pistons, other mods). `after(server, owner, result)` gets each machine's result.
function faTickMachines(server, all, blockId, run, after) {
  let keys = all.getAllKeys().toArray()
  if (keys.length == 0) return
  let levels = {}
  server.getAllLevels().forEach(l => { levels[String(l.dimension)] = l })
  for (let k = 0; k < keys.length; k++) {
    let key = String(keys[k])
    let parts = key.split('|')
    let level = levels[parts[0]]
    if (!level || parts.length != 4) continue
    let pos = new $FAAltarPos(parseInt(parts[1]), parseInt(parts[2]), parseInt(parts[3]))
    if (!level.isLoaded(pos)) continue
    if (String(level.getBlock(pos).id) != blockId) {
      all.remove(key)
      continue
    }
    try {
      let result = run(level, pos)
      if (after && result > 0) after(server, String(all.getString(key)), result)
    } catch (err) {
      console.error('[Factory Ascension] ' + blockId + ' at ' + key + ' failed: ' + err)
    }
  }
}

ServerEvents.tick(event => {
  let server = event.server
  if (server.getTickCount() % 20 != 0) return
  try {
    if (server.persistentData.contains('fa_altars')) {
      faTickMachines(server, faAltars(server), FA_ALTAR_ID, faRunAltar, (srv, owner, given) => {
        if (!owner) return
        let player = null
        srv.getPlayerList().getPlayers().forEach(p => { if (p.stringUuid == owner) player = p })
        if (player) faStatRecord(player, 'altar_levels', given)
        else faStatBump(srv, owner, 'altar_levels', given, 'add')
      })
    }
    if (server.persistentData.contains('fa_condensers')) faTickMachines(server, faCondensers(server), FA_CONDENSER_ID, faRunCondenser, null)
  } catch (err) {
    console.error('[Factory Ascension] machine tick failed: ' + err)
  }
})
