// Factory Ascension - the Ascended Realm (Full download: the dimension and its blocks come with the mod jar).
// Right-click a Realm Gate anywhere outside the Realm: you arrive at the same x, z in the Realm on a small Realm Stone
// platform, with a gate of its own. Right-click that one (or any gate in the Realm) to go back where you left.
// Config: realm (platform height and size, cooldown) and danger.realmBonus.

const FA_REALM = 'factoryascension:ascended_realm'

function faRealmGo(entity, dim, x, y, z) {
  entity.getServer().runCommandSilent('execute in ' + dim + ' run tp ' + entity.getUuid() + ' ' + x + ' ' + y + ' ' + z)
}

// A platform at x, z in the Realm (built once): a floor of Realm Stone, room to stand, and a gate back.
// Returns where to stand.
function faRealmPlatform(server, x, z) {
  let cfg = global.FA.realm
  let level = server.getLevel(FA_REALM)
  let y = cfg.platformY
  let r = cfg.platformRadius
  let gate = level.getBlock(x, y, z - r)
  if (String(gate.id) != 'factoryascension:realm_gate') {
    for (let dx = -r; dx <= r; dx++) for (let dz = -r; dz <= r; dz++) {
      level.getBlock(x + dx, y - 1, z + dz).set('factoryascension:realm_stone')
      for (let dy = 0; dy < 4; dy++) level.getBlock(x + dx, y + dy, z + dz).set('minecraft:air')
    }
    gate.set('factoryascension:realm_gate')
  }
  return { x: x + 0.5, y: y, z: z + 0.5 }
}

// Through a gate at pos: into the Realm, or back from it. Works for any entity (the self-test sends an armor stand).
function faRealmUse(player, pos) {
  let data = player.persistentData
  let isPlayer = player.isPlayer()
  let now = player.getServer().getTickCount()
  if (now - data.getLong('fa_realm_last') < global.FA.realm.cooldownTicks) return
  data.putLong('fa_realm_last', now)
  let here = String(player.level.dimension)
  if (here == FA_REALM) {
    // Back to where you came from (or the Overworld's spawn if that's not known)
    let back = String(data.getString('fa_realm_return') || '')
    let p = back.split('|')
    if (p.length == 4) faRealmGo(player, p[0], p[1], p[2], p[3])
    else {
      let sp = player.getServer().overworld().getSharedSpawnPos()
      faRealmGo(player, 'minecraft:overworld', sp.getX() + 0.5, sp.getY() + 1, sp.getZ() + 0.5)
    }
    if (isPlayer) player.tell(Text.aqua('You leave the Ascended Realm.'))
    return
  }
  data.putString('fa_realm_return', here + '|' + player.getX().toFixed(2) + '|' + player.getY().toFixed(2) + '|' + player.getZ().toFixed(2))
  let at = faRealmPlatform(player.getServer(), Math.floor(pos.x), Math.floor(pos.z))
  faRealmGo(player, FA_REALM, at.x, at.y, at.z)
  if (!isPlayer) return
  let danger = global.FA_dangerLevel(player.getServer().getLevel(FA_REALM), at.x, at.z, player)
  player.tell(Text.lightPurple('✦ The Ascended Realm. ').append(Text.gray('Danger ' + danger + ' here. The gate behind you leads back.')))
}

if (Platform.isLoaded('factoryascension')) {
  BlockEvents.rightClicked('factoryascension:realm_gate', event => {
    try {
      if (String(event.hand) != 'MAIN_HAND' || event.player.isFake()) return
      faRealmUse(event.player, event.block.pos)
      event.cancel()
    } catch (err) {
      console.error('[Factory Ascension] realm gate: ' + err)
    }
  })
}

// For /ascension selftest
global.FA_realmUse = (player, pos) => faRealmUse(player, pos)
global.FA_realmPlatform = (server, x, z) => faRealmPlatform(server, x, z)
