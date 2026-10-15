// priority: 850
// Factory Ascension - /sethome and /home (config: home). One home per player, in any dimension.
// /home is blocked while you're in combat (startup script 80_fa_combat.js) and has a short cooldown.
// With danger.centerOn 'home' (the default), your home is also the middle of your danger zones: nothing scales until
// you set one, and it grows the farther you go from it. global.FA_STATE.homes keeps every home's center for that.

function faHomes(server) {
  let data = server.persistentData
  if (!data.contains('fa_homes')) data.put('fa_homes', data.getCompound('fa_homes'))
  return data.getCompound('fa_homes')
}

// A home's center in Overworld coordinates (a Nether home counts x8; an End home measures from the End's middle)
function faHomeCenter(dim, x, z) {
  let D = global.FA.danger
  if (dim == 'minecraft:the_nether') return { x: Math.round(x * D.netherScale), z: Math.round(z * D.netherScale), end: false }
  if (dim == 'minecraft:overworld') return { x: Math.round(x), z: Math.round(z), end: false }
  return { x: 0, z: 0, end: true }
}

// Replace the whole object (never mutate through global, never store null)
function faPutHomeCenter(uuid, center) {
  let homes = Object.assign({}, global.FA_STATE.homes || {})
  if (center) homes[uuid] = center
  else delete homes[uuid]
  global.FA_STATE.homes = homes
}

function faLoadHomes(server) {
  let homes = faHomes(server)
  let out = {}
  let keys = homes.getAllKeys().toArray()
  for (let i = 0; i < keys.length; i++) {
    let h = homes.getCompound(keys[i])
    out[String(keys[i])] = faHomeCenter(String(h.getString('dim')), h.getDouble('x'), h.getDouble('z'))
  }
  global.FA_STATE.homes = out
}

ServerEvents.loaded(event => faLoadHomes(event.server))

function faSetHome(player) {
  let H = global.FA.home
  if (!H.enabled) return 0
  let homes = faHomes(player.getServer())
  let h = homes.getCompound(player.stringUuid)
  h.putString('dim', String(player.level.dimension))
  h.putDouble('x', player.getX())
  h.putDouble('y', player.getY())
  h.putDouble('z', player.getZ())
  h.putFloat('yaw', player.yaw)
  homes.put(player.stringUuid, h)
  player.getServer().persistentData.put('fa_homes', homes)
  let first = !global.FA_STATE.homes[String(player.stringUuid)]
  faPutHomeCenter(String(player.stringUuid), faHomeCenter(String(player.level.dimension), player.getX(), player.getZ()))
  player.tell(Text.green('Home set: ').append(Text.white(Math.floor(player.getX()) + ', ' + Math.floor(player.getY()) + ', ' + Math.floor(player.getZ())))
    .append(Text.gray(' (' + String(player.level.dimension).replace('minecraft:', '') + '). Type /home to come back.')))
  let D = global.FA.danger
  if (D.enabled && D.centerOn == 'home') {
    let m = k => global.FA_dangerMult(global.FA_dangerAtDistance(k * 1000, 0), false).health.toFixed(1)
    player.tell(Text.gold(first ? 'Your danger zones start here. ' : 'Your danger zones moved here. ')
      .append(Text.gray('Mobs stay normal within ' + global.FA_num(D.safeRadius) + ' blocks of home, then get tougher: x' + m(1) + ' at 1,000 blocks, x' + m(2) +
        ' at 2,000, x' + m(3) + ' at 3,000... with better loot to match. Mobs that already spawned keep their level.')))
  }
  return 1
}

function faGoHome(player) {
  let H = global.FA.home
  if (!H.enabled) return 0
  let server = player.getServer()
  let homes = faHomes(server)
  if (!homes.contains(player.stringUuid)) {
    player.tell(Text.red('You have no home yet. Stand where you want it and type /home set.'))
    return 0
  }
  let combat = global.FA_combatLeft(player)
  if (combat > 0) {
    player.tell(Text.red('You\'re in combat! ').append(Text.gray('/home works again in ' + combat + 's.')))
    return 0
  }
  let data = player.getPersistentData()
  let now = server.getTickCount()
  let used = data.contains('fa_home_used') ? data.getInt('fa_home_used') : -1e9
  if (used > now) used = -1e9   // from before a server restart
  let wait = Math.ceil((used + H.cooldownSeconds * 20 - now) / 20)
  if (wait > 0) {
    player.tell(Text.yellow('/home is cooling down: ' + wait + 's.'))
    return 0
  }
  let h = homes.getCompound(player.stringUuid)
  // By UUID: works for every player name (and Forge's fake players)
  server.runCommandSilent('execute in ' + h.getString('dim') + ' run tp ' + player.stringUuid + ' ' +
    h.getDouble('x').toFixed(2) + ' ' + h.getDouble('y').toFixed(2) + ' ' + h.getDouble('z').toFixed(2) + ' ' + h.getFloat('yaw').toFixed(1) + ' 0')
  data.putInt('fa_home_used', now)
  server.runCommandSilent('execute at ' + player.stringUuid + ' run playsound minecraft:entity.enderman.teleport player ' + player.stringUuid + ' ~ ~ ~ 1 1.2')
  player.displayClientMessage(Text.green('Welcome home.'), true)
  return 1
}

ServerEvents.commandRegistry(event => {
  let { commands: Commands } = event
  event.register(Commands.literal('sethome').executes(ctx => faSetHome(ctx.source.getPlayerOrException())))
  event.register(Commands.literal('home')
    .executes(ctx => faGoHome(ctx.source.getPlayerOrException()))
    .then(Commands.literal('set').executes(ctx => faSetHome(ctx.source.getPlayerOrException()))))
})

// Until you set a home, nothing out here scales: say so when you join
PlayerEvents.loggedIn(event => {
  let D = global.FA.danger
  let player = event.player
  if (!D.enabled || D.centerOn != 'home' || global.FA_STATE.homes[String(player.stringUuid)]) return
  player.tell(Text.gold('Explore freely: ').append(Text.gray('mobs stay normal everywhere until you pick a base and type '))
    .append(Text.white('/home set')).append(Text.gray('. After that, the farther you go from home, the tougher they get (and the better the loot).')))
})
