// priority: 850
// Factory Ascension - /sethome and /home (config: home). One home per player, in any dimension.
// /home is blocked while you're in combat (startup script 80_fa_combat.js) and has a short cooldown.

function faHomes(server) {
  let data = server.persistentData
  if (!data.contains('fa_homes')) data.put('fa_homes', data.getCompound('fa_homes'))
  return data.getCompound('fa_homes')
}

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
  player.tell(Text.green('Home set: ').append(Text.white(Math.floor(player.getX()) + ', ' + Math.floor(player.getY()) + ', ' + Math.floor(player.getZ())))
    .append(Text.gray(' (' + String(player.level.dimension).replace('minecraft:', '') + '). Type /home to come back.')))
  return 1
}

function faGoHome(player) {
  let H = global.FA.home
  if (!H.enabled) return 0
  let server = player.getServer()
  let homes = faHomes(server)
  if (!homes.contains(player.stringUuid)) {
    player.tell(Text.red('You have no home yet. Stand where you want it and type /sethome.'))
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
