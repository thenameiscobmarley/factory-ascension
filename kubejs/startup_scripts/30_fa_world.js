// priority: 700
// Factory Ascension - world events and Ascended bosses.
// Both run from one server tick hook, once a second. State lives in global (events end on restart,
// bosses re-register themselves when their chunk loads, since their flags are saved on the mob).

const $FABossEvent = Java.loadClass('net.minecraft.server.level.ServerBossEvent')
const $FABarColor = Java.loadClass('net.minecraft.world.BossEvent$BossBarColor')
const $FABarOverlay = Java.loadClass('net.minecraft.world.BossEvent$BossBarOverlay')
const $FAHeightmap = Java.loadClass('net.minecraft.world.level.levelgen.Heightmap$Types')

// The current event lives in global.FA_STATE.event: { type, until (ms), next (ms, meteor timer) }, or false for none
let faBosses = {}           // uuid -> { mob, bar, phase, nextSlam }

// ---- World events ----

const FA_EVENT_INFO = {
  bloodMoon: { title: 'BLOOD MOON', color: 'dark_red', sub: 'Mobs are stronger and drop more Mythic Fragments' },
  meteorShower: { title: 'METEOR SHOWER', color: 'gold', sub: 'Rare loot is falling from the sky. Look up!' },
  goldenHour: { title: 'GOLDEN HOUR', color: 'yellow', sub: 'Mob loot is doubled' }
}

function faTitleAll(server, title, color, sub) {
  server.runCommandSilent('title @a times 10 70 20')
  server.runCommandSilent('title @a subtitle ' + JSON.stringify({ text: sub, color: 'gray' }))
  server.runCommandSilent('title @a title ' + JSON.stringify({ text: title, color: color, bold: true }))
}

// Starts an event by name. Returns false if it can't run right now (e.g. Blood Moon in daytime).
global.FA_startEvent = (server, type) => {
  let E = global.FA.events
  let cfg = E[type]
  if (!cfg || !FA_EVENT_INFO[type]) return false
  if (type == 'bloodMoon' && cfg.nightOnly && !server.overworld().isNight()) return false
  global.FA_STATE.event = { type: type, until: Date.now() + cfg.minutes * 60000, next: 0 }
  let info = FA_EVENT_INFO[type]
  faTitleAll(server, info.title, info.color, info.sub)
  server.tell(Text.gold('★ ' + info.title + ' ★ ').append(Text.gray(info.sub + ' (' + cfg.minutes + ' min)')))
  server.runCommandSilent('execute as @a at @s run playsound ' +
    (type == 'bloodMoon' ? 'minecraft:entity.wither.ambient' : 'minecraft:ui.toast.challenge_complete') + ' master @s ~ ~ ~ 0.8 0.7')
  return true
}

global.FA_endEvent = server => {
  let e = global.FA_STATE.event
  global.FA_STATE.event = false   // never null: see FA_STATE in 00_fa_config.js
  if (e && server) server.tell(Text.gray('The ' + FA_EVENT_INFO[e.type].title.toLowerCase().replace(/\b\w/g, c => c.toUpperCase()) + ' has ended.'))
}

function faPickWeighted(list) {
  let total = 0
  list.forEach(x => { total += x[1] })
  let r = Math.random() * total
  for (let i = 0; i < list.length; i++) {
    r -= list[i][1]
    if (r < 0) return list[i][0]
  }
  return list[list.length - 1][0]
}

// Meteor Shower: drop a glowing rare item from the sky somewhere near each player
function faMeteor(server, player) {
  let M = global.FA.events.meteorShower
  let level = player.level
  let x = Math.floor(player.getX() + (Math.random() * 2 - 1) * M.radius)
  let z = Math.floor(player.getZ() + (Math.random() * 2 - 1) * M.radius)
  let y = level.getHeight($FAHeightmap.MOTION_BLOCKING, x, z) + 30
  let drop = new $FAItemEntity(level, x + 0.5, y, z + 0.5, Item.of(faPickWeighted(M.drops)))
  drop.glowing = true
  drop.setMotion(0, -1.5, 0)
  level.addFreshEntity(drop)
  let dim = 'execute in ' + level.dimension + ' run '
  server.runCommandSilent(dim + 'particle minecraft:flame ' + x + ' ' + (y - 10) + ' ' + z + ' 0.3 8 0.3 0.05 60 force')
  server.runCommandSilent(dim + 'playsound minecraft:entity.firework_rocket.large_blast ambient @a ' + x + ' ' + (y - 20) + ' ' + z + ' 3 0.6')
}

// ---- Ascended bosses ----

// Turns a freshly spawned mob into an Ascended boss (called from the danger spawn hook)
global.FA_makeBoss = (mob, danger) => {
  let B = global.FA.bosses
  let data = mob.getPersistentData()
  data.putBoolean('fa_boss', true)
  let title = B.titles[Math.floor(Math.random() * B.titles.length)]
  mob.setCustomName(Text.darkRed('☠ ').append(mob.getDisplayName()).append(Text.darkRed(' ' + title)).append(Text.gray(' · Lv ' + danger)))
  mob.setCustomNameVisible(true)
  if (mob.setPersistenceRequired) mob.setPersistenceRequired()
  mob.addEffect(new $FAEffectInstance($FAEffects.GLOWING, 1000000, 0, true, false))
  faTrackBoss(mob)
}

function faTrackBoss(mob) {
  let uuid = mob.stringUuid
  if (faBosses[uuid]) return
  let bar = new $FABossEvent(mob.getDisplayName(), $FABarColor.RED, $FABarOverlay.NOTCHED_10)
  faBosses[uuid] = { mob: mob, bar: bar, phase: 0, nextSlam: 0 }
}

// Bosses re-register when their chunk loads again
ForgeEvents.onEvent('net.minecraftforge.event.entity.EntityJoinLevelEvent', event => {
  try {
    let mob = event.getEntity()
    if (!event.getLevel().isClientSide() && event.loadedFromDisk() && mob instanceof $FALiving && !(mob instanceof $FAPlayer) &&
      mob.getPersistentData().getBoolean('fa_boss')) faTrackBoss(mob)
  } catch (err) {
    faWarnOnce('boss reload', err)
  }
})

function faBossTick(server, now) {
  let B = global.FA.bosses
  Object.keys(faBosses).forEach(uuid => {
    let b = faBosses[uuid]
    let mob = b.mob
    if (!mob.isAlive() || mob.isRemoved()) {
      b.bar.removeAllPlayers()
      delete faBosses[uuid]
      return
    }
    let ratio = Math.max(0, Math.min(1, mob.getHealth() / mob.getMaxHealth()))
    b.bar.setProgress(ratio)
    // Show the bar to players within 64 blocks, hide it from everyone else
    let near = mob.level.getEntitiesOfClass($FAPlayer, mob.getBoundingBox().inflate(64))
    let nearSet = {}
    for (let i = 0; i < near.size(); i++) {
      let p = near.get(i)
      nearSet[p.stringUuid] = true
      b.bar.addPlayer(p)
    }
    let shown = b.bar.getPlayers().toArray()
    for (let i = 0; i < shown.length; i++) if (!nearSet[shown[i].stringUuid]) b.bar.removePlayer(shown[i])

    // Phases at 66% and 33%: summon minions and get angrier
    let phase = ratio <= 0.33 ? 2 : ratio <= 0.66 ? 1 : 0
    if (phase > b.phase) {
      b.phase = phase
      for (let i = 0; i < B.minionsPerPhase; i++) {
        let minion = mob.level.createEntity(mob.type)
        if (!minion) break
        minion.getPersistentData().putBoolean('fa_minion', true)
        minion.setPositionAndRotation(mob.getX() + (Math.random() * 6 - 3), mob.getY(), mob.getZ() + (Math.random() * 6 - 3), Math.random() * 360, 0)
        mob.level.addFreshEntity(minion)
      }
      mob.addEffect(new $FAEffectInstance($FAEffects.MOVEMENT_SPEED, 1000000, phase, true, false))
      faFx(mob, 'minecraft:soul_fire_flame', 120, 'minecraft:entity.wither.spawn', 0.8)
      for (let i = 0; i < near.size(); i++) near.get(i).displayClientMessage(Text.darkRed(phase == 1 ? 'It calls for help!' : 'It is enraged!'), true)
    }

    // Slam every 8 seconds: throws nearby players back
    if (now >= b.nextSlam && near.size() > 0) {
      b.nextSlam = now + 8000
      let hit = false
      for (let i = 0; i < near.size(); i++) {
        let p = near.get(i)
        let dx = p.getX() - mob.getX()
        let dz = p.getZ() - mob.getZ()
        let dist = Math.sqrt(dx * dx + dz * dz)
        if (dist > 6 || p.isCreative() || p.isSpectator()) continue
        p.setMotion(dx / (dist || 1) * 1.6, 0.7, dz / (dist || 1) * 1.6)
        p.hurtMarked = true
        hit = true
      }
      if (hit) faFx(mob, 'minecraft:explosion', 12, 'minecraft:entity.generic.explode', 0.6)
    }
  })
}

// ---- The tick ----

let faLastSecond = 0
let faLastEventCheck = Date.now()

ForgeEvents.onEvent('net.minecraftforge.event.TickEvent$ServerTickEvent', event => {
  if (String(event.phase) != 'END') return
  let now = Date.now()
  if (now - faLastSecond < 1000) return
  faLastSecond = now
  let server = event.getServer()
  if (!server) return

  try {
    let E = global.FA.events
    let current = global.FA_STATE.event
    if (current && now >= current.until) {
      global.FA_endEvent(server)
    } else if (current && current.type == 'meteorShower' && now >= current.next) {
      current.next = now + E.meteorShower.everySeconds * 1000
      server.getPlayerList().getPlayers().forEach(p => { if (!p.isSpectator()) faMeteor(server, p) })
    } else if (!current && E.enabled && now - faLastEventCheck >= E.checkEveryMinutes * 60000) {
      faLastEventCheck = now
      if (server.getPlayerList().getPlayers().size() > 0 && Math.random() < E.chancePerCheck) {
        let types = Object.keys(FA_EVENT_INFO)
        global.FA_startEvent(server, types[Math.floor(Math.random() * types.length)])
      }
    }
  } catch (err) {
    faWarnOnce('world events', err)
  }

  try {
    faBossTick(server, now)
  } catch (err) {
    faWarnOnce('bosses', err)
  }
})
