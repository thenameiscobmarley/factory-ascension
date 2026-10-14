// Factory Ascension - levels, Essence and player stats.
// Progress is saved in the world's KubeJS data (server.persistentData), keyed by player UUID,
// so it survives death, relogs and restarts. Stat bonuses are transient attribute modifiers:
// they are re-applied on login/respawn and are never written into the player file, so
// removing these scripts leaves no leftover stats behind.

const $FAAttributes = Java.loadClass('net.minecraft.world.entity.ai.attributes.Attributes')
const $FAModifier = Java.loadClass('net.minecraft.world.entity.ai.attributes.AttributeModifier')
const $FAOperation = Java.loadClass('net.minecraft.world.entity.ai.attributes.AttributeModifier$Operation')

const FA_HEALTH_MOD = 'factory_ascension_health'
const FA_SPEED_MOD = 'factory_ascension_speed'

// ---- Saved data ----

function faGetLevel(server, uuid) {
  return server.persistentData.getInt('fa_level_' + uuid)
}

function faGetEssence(server, uuid) {
  return server.persistentData.getDouble('fa_essence_' + uuid)
}

function faSave(server, uuid, level, essence) {
  server.persistentData.putInt('fa_level_' + uuid, level)
  server.persistentData.putDouble('fa_essence_' + uuid, essence)
}

function faCost(level) {
  return Math.ceil(global.FA.costBase * Math.pow(global.FA.costGrowth, level))
}

// ---- Numbers ----

const FA_UNITS = ['K', 'M', 'B', 'T', 'Qa', 'Qi', 'Sx', 'Sp', 'Oc', 'No', 'Dc']

function faFmt(n) {
  if (!isFinite(n)) return 'Infinity'
  if (n < 10000) return String(Math.floor(n))
  let i = -1
  while (n >= 1000 && i < FA_UNITS.length - 1) {
    n /= 1000
    i++
  }
  if (n >= 1000) return (n * Math.pow(1000, i + 1)).toExponential(2)
  return n.toFixed(n < 10 ? 2 : n < 100 ? 1 : 0) + FA_UNITS[i]
}

function faPct(x) {
  return Math.round(x * 100) + '%'
}

// ---- Stats ----

// Replaces our named modifier on one attribute (removing first so it never stacks)
function faSetModifier(player, attribute, name, amount, operation) {
  let instance = player.getAttribute(attribute)
  if (!instance) return
  let old = []
  let it = instance.getModifiers().iterator()
  while (it.hasNext()) {
    let mod = it.next()
    if (mod.getName() == name) old.push(mod)
  }
  old.forEach(mod => instance.removeModifier(mod))
  if (amount != 0) instance.addTransientModifier(new $FAModifier(name, amount, operation))
}

function faApplyStats(player, healToFull) {
  let cfg = global.FA
  let uuid = player.getStringUUID()
  let level = faGetLevel(player.getServer(), uuid)
  global.FA_LEVELS[uuid] = level

  faSetModifier(player, $FAAttributes.MAX_HEALTH, FA_HEALTH_MOD, cfg.healthPerLevel * level, $FAOperation.ADDITION)
  faSetModifier(player, $FAAttributes.MOVEMENT_SPEED, FA_SPEED_MOD,
    Math.min(cfg.speedCap, cfg.speedPerLevel * level), $FAOperation.MULTIPLY_BASE)

  if (healToFull || player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth())
}

function faSafeApply(player, healToFull) {
  try {
    faApplyStats(player, healToFull)
  } catch (err) {
    console.error('[Factory Ascension] could not apply stats to ' + player.getScoreboardName() + ': ' + err)
  }
}

// ---- Levelling ----

function faAddEssence(player, amount) {
  let cfg = global.FA
  let server = player.getServer()
  let uuid = player.getStringUUID()
  let start = faGetLevel(server, uuid)
  let level = start
  let essence = faGetEssence(server, uuid) + amount

  while (level < cfg.maxLevel && essence >= faCost(level)) {
    essence -= faCost(level)
    level++
  }
  faSave(server, uuid, level, essence)

  if (level > start) {
    faApplyStats(player, cfg.healOnLevelUp)
    faCelebrate(player, start, level)
  }
  player.displayClientMessage(
    Text.lightPurple('+' + faFmt(amount) + ' Essence  ')
      .append(Text.gray(faFmt(essence) + ' / ' + faFmt(faCost(level)) + ' to Ascension ' + (level + 1))),
    true)
}

function faCelebrate(player, from, to) {
  let server = player.getServer()
  let name = player.getScoreboardName()
  let gained = to - from

  server.runCommandSilent('title ' + name + ' times 5 40 15')
  server.runCommandSilent('title ' + name + ' subtitle {"text":"Ascension ' + to +
    (gained > 1 ? '  (+' + gained + ')' : '') + '","color":"light_purple"}')
  server.runCommandSilent('title ' + name + ' title {"text":"ASCENDED","color":"gold","bold":true}')
  server.runCommandSilent('execute at ' + name + ' run playsound minecraft:ui.toast.challenge_complete player ' + name + ' ~ ~ ~ 1 1')
  server.runCommandSilent('execute at ' + name + ' run particle minecraft:totem_of_undying ~ ~1 ~ 0.6 1 0.6 0.4 120 normal')

  // Announce each time a multiple of announceEvery is crossed
  let every = global.FA.announceEvery
  if (every > 0 && Math.floor(to / every) > Math.floor(from / every)) {
    server.tell(Text.gold(name).append(Text.yellow(' has reached Ascension ' + to + '!')))
  }
}

function faShowStatus(player) {
  let cfg = global.FA
  let server = player.getServer()
  let uuid = player.getStringUUID()
  let level = faGetLevel(server, uuid)
  let essence = faGetEssence(server, uuid)
  let taken = 1 - 1 / (1 + cfg.damageTakenPerLevel * level)

  player.tell(Text.gold('Ascension ' + level).bold(true))
  player.tell(Text.gray('Essence: ').append(Text.lightPurple(faFmt(essence) + ' / ' + faFmt(faCost(level))))
    .append(Text.gray(' to the next level')))
  player.tell(Text.gray('Damage dealt: ').append(Text.red('x' + (1 + cfg.damageDealtPerLevel * level).toFixed(2)))
    .append(Text.gray('   Damage taken: ')).append(Text.green('-' + faPct(taken))))
  player.tell(Text.gray('Bonus health: ').append(Text.red('+' + faFmt(cfg.healthPerLevel * level / 2) + ' hearts'))
    .append(Text.gray('   Speed: ')).append(Text.aqua('+' + faPct(Math.min(cfg.speedCap, cfg.speedPerLevel * level)))))
  player.tell(Text.gray('Mob loot: ').append(Text.yellow('x' + (1 + cfg.lootPerLevel * level).toFixed(2))))
  return 1
}

function faSetLevel(player, level) {
  faSave(player.getServer(), player.getStringUUID(), level, 0)
  faApplyStats(player, true)
  return faShowStatus(player)
}

// ---- Events ----

Object.keys(global.FA.essenceValues).forEach(id => {
  ItemEvents.rightClicked(id, event => {
    let player = event.player
    let stack = event.item
    let count = player.isShiftKeyDown() ? stack.getCount() : 1
    if (count <= 0) return

    stack.shrink(count)
    try {
      faAddEssence(player, count * global.FA.essenceValues[id])
    } catch (err) {
      // Hand the items back rather than lose them
      player.give(Item.of(id, count))
      console.error('[Factory Ascension] absorbing ' + id + ' failed, items returned: ' + err)
    }
    event.cancel()
  })
})

PlayerEvents.loggedIn(event => faSafeApply(event.player, false))
PlayerEvents.respawned(event => faSafeApply(event.player, true))
PlayerEvents.loggedOut(event => {
  delete global.FA_LEVELS[event.player.getStringUUID()]
})

ServerEvents.commandRegistry(event => {
  let { commands: Commands, arguments: Arguments } = event

  event.register(Commands.literal('ascension')
    .executes(ctx => faShowStatus(ctx.source.getPlayerOrException()))
    .then(Commands.literal('set')
      .requires(src => src.hasPermission(2))
      .then(Commands.argument('level', Arguments.INTEGER.create(event))
        .executes(ctx => faSetLevel(ctx.source.getPlayerOrException(),
          Math.max(0, Math.min(global.FA.maxLevel, Arguments.INTEGER.getResult(ctx, 'level')))))))
    .then(Commands.literal('reset')
      .requires(src => src.hasPermission(2))
      .executes(ctx => faSetLevel(ctx.source.getPlayerOrException(), 0))))
})

// After /reload, re-apply stats to everyone already online
try {
  if (Utils.server) Utils.server.getPlayerList().getPlayers().forEach(p => faSafeApply(p, false))
} catch (err) {
  console.error('[Factory Ascension] reload re-apply failed: ' + err)
}
