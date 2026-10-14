// Factory Ascension - Ultimates: cinematic combo moves.
//
// Sneak + hit an enemy (a direct melee hit) with an Ultimate equipped and it plays out over the next second or
// three: teleports, launches, lightning, explosions. Built from a few shared steps on a tick scheduler.
//
// Safety rules, for every move:
//   - you can't be hurt during a combo (except by /kill and the void), and take no fall damage from it
//   - combo damage is "secondary" damage: it never triggers more enchants or another Ultimate
//   - if the target dies, the combo finishes its visuals and skips its hits
//   - if you log out, die or anything goes wrong, you're put back where you started
//   - a failsafe returns you home even if a step crashes
// Uses helpers from 20_fa_enchant_effects.js (same startup script scope).

let faTick = 0
let faQueue = []
let faCombos = {}      // player uuid -> combo
let faUltReady = {}    // player uuid + ult id -> tick it's ready again

// ---- Tick scheduler ----

function faLater(ticks, fn) {
  if (faQueue.length > 5000) return   // something is very wrong; don't grow forever
  faQueue.push({ at: faTick + Math.max(1, Math.round(ticks)), fn: fn })
}

ForgeEvents.onEvent('net.minecraftforge.event.TickEvent$ServerTickEvent', event => {
  if (String(event.phase) != 'END') return
  faTick++
  if (!faQueue.length) return
  let due = []
  let later = []
  faQueue.forEach(q => (q.at <= faTick ? due : later).push(q))
  faQueue = later
  due.forEach(q => {
    try {
      q.fn()
    } catch (err) {
      faWarnOnce('ultimate step', err)
    }
  })
})

// ---- Shared steps ----

function faInCombo(entity) {
  return entity instanceof $FAPlayer && !!faCombos[entity.getStringUUID()]
}

// Yaw/pitch that look from one point to another (Minecraft: yaw 0 = south, pitch up is negative)
function faLook(fx, fy, fz, tx, ty, tz) {
  let dx = tx - fx
  let dy = ty - fy
  let dz = tz - fz
  return { yaw: Math.atan2(dz, dx) * 180 / Math.PI - 90, pitch: -Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 180 / Math.PI }
}

// Where the target is (or was, if it already died)
function faAim(c) {
  if (c.target && c.target.isAlive()) { c.tx = c.target.getX(); c.ty = c.target.getY(); c.tz = c.target.getZ() }
  return { x: c.tx, y: c.ty, z: c.tz }
}

// Teleport the player to a point, facing the target
function faBlink(c, x, y, z) {
  let t = faAim(c)
  let look = faLook(x, y + 1.6, z, t.x, t.y + 1, t.z)
  c.player.teleportTo(c.level, x, y, z, look.yaw, look.pitch)
  c.player.fallDistance = 0
  c.moved = true
}

function faFxAt(c, x, y, z, particle, count, spread, speed, sound, pitch) {
  let server = c.player.getServer()
  if (!server) return
  let pos = x.toFixed(2) + ' ' + y.toFixed(2) + ' ' + z.toFixed(2)
  let dim = 'execute in ' + c.level.dimension().location() + ' run '
  if (particle) server.runCommandSilent(dim + 'particle ' + particle + ' ' + pos + ' ' + spread + ' ' + spread + ' ' + spread + ' ' + speed + ' ' + count + ' force')
  if (sound) server.runCommandSilent(dim + 'playsound ' + sound + ' player @a ' + pos + ' 1.5 ' + (pitch || 1))
}

function faBoltAt(c, x, y, z) {
  let bolt = $FAEntityType.LIGHTNING_BOLT.create(c.level)
  if (!bolt) return
  bolt.moveTo(x, y, z)
  bolt.setVisualOnly(true)   // looks and sounds real, but no fire, no damage, no charged creepers
  c.level.addFreshEntity(bolt)
}

// One combo strike: the triggering hit's damage x the Ultimate's power x mult
function faUltHit(c, mob, mult) {
  if (!mob || !mob.isAlive()) return
  mob.invulnerableTime = 0   // combo hits land in quick succession
  faSecondaryHit(mob, c.player.damageSources().playerAttack(c.player), c.dmg * mult)
}

function faUltAoe(c, x, y, z, radius, mult) {
  faEnemiesNear(c.target && c.target.isAlive() ? c.target : c.player, radius, [c.player], 32).forEach(m => {
    let dx = m.getX() - x
    let dz = m.getZ() - z
    if (dx * dx + dz * dz <= radius * radius) faUltHit(c, m, mult)
  })
}

function faFreeze(mob, ticks) {
  if (faIsBoss(mob)) return   // bosses shrug off crowd control
  mob.addEffect(new $FAEffectInstance($FAEffects.MOVEMENT_SLOWDOWN, ticks, 127, false, false))
  mob.addEffect(new $FAEffectInstance($FAEffects.WEAKNESS, ticks, 127, false, false))
}

// ---- The moves ----
// Each returns a list of [ticks to wait, step]. Steps get the combo `c`.

let FA_MOVES = {
  // The one from the request: circle-blink slashes, into the sky, three lightning strikes, boom, back home
  thunder_waltz: c => {
    let steps = []
    for (let i = 0; i < 8; i++) {
      steps.push([3, c => {
        let t = faAim(c)
        let a = i * Math.PI / 4
        faBlink(c, t.x + Math.cos(a) * 3, t.y, t.z + Math.sin(a) * 3)
        faFxAt(c, t.x, t.y + 1, t.z, 'minecraft:sweep_attack', 3, 0.6, 0, 'minecraft:entity.player.attack.sweep', 1 + i * 0.05)
        faUltHit(c, c.target, 1)
      }])
    }
    steps.push([4, c => {
      let t = faAim(c)
      faBlink(c, t.x, t.y + 9, t.z + 0.01)
      faFxAt(c, t.x, t.y + 9, t.z, 'minecraft:cloud', 30, 0.8, 0.05, 'minecraft:entity.phantom.flap', 0.6)
    }])
    for (let i = 0; i < 3; i++) {
      steps.push([6, c => {
        let t = faAim(c)
        faBoltAt(c, t.x, t.y, t.z)
        faUltHit(c, c.target, 1.5)
      }])
    }
    steps.push([8, c => {
      let t = faAim(c)
      faFxAt(c, t.x, t.y + 1, t.z, 'minecraft:explosion_emitter', 2, 0.5, 0, 'minecraft:entity.generic.explode', 0.7)
      faUltHit(c, c.target, 3)
      faUltAoe(c, t.x, t.y, t.z, 4, 1)
    }])
    return steps
  },

  thousand_cuts: c => {
    let steps = []
    for (let i = 0; i < 16; i++) {
      steps.push([1, c => {
        let t = faAim(c)
        let a = Math.random() * Math.PI * 2
        faBlink(c, t.x + Math.cos(a) * 2.5, t.y + Math.random(), t.z + Math.sin(a) * 2.5)
        faFxAt(c, t.x, t.y + 1, t.z, 'minecraft:sweep_attack', 2, 0.8, 0, i % 3 == 0 ? 'minecraft:entity.player.attack.sweep' : null, 1.4)
        faUltHit(c, c.target, 1)
      }])
    }
    steps.push([4, c => {
      let t = faAim(c)
      faFxAt(c, t.x, t.y + 1, t.z, 'minecraft:sweep_attack', 24, 1.6, 0, 'minecraft:entity.player.attack.crit', 0.6)
      faUltHit(c, c.target, 4)
    }])
    return steps
  },

  heaven_splitter: c => [
    [1, c => {
      if (!faIsBoss(c.target)) { c.target.setDeltaMovement(0, 2.2, 0); c.target.hurtMarked = true }
      let t = faAim(c)
      faFxAt(c, t.x, t.y, t.z, 'minecraft:cloud', 40, 0.6, 0.1, 'minecraft:entity.player.attack.knockback', 0.6)
    }],
    [12, c => {
      let t = faAim(c)
      faBlink(c, t.x, t.y + 3, t.z + 0.01)
      faFxAt(c, t.x, t.y + 2, t.z, 'minecraft:crit', 40, 1, 0.3, 'minecraft:entity.player.attack.strong', 0.8)
    }],
    [4, c => {
      if (!faIsBoss(c.target)) { c.target.setDeltaMovement(0, -3.5, 0); c.target.hurtMarked = true }
      faUltHit(c, c.target, 1)
    }],
    [6, c => {
      let t = faAim(c)
      faFxAt(c, t.x, t.y, t.z, 'minecraft:explosion', 30, 3, 0, 'minecraft:entity.generic.explode', 0.6)
      faUltAoe(c, t.x, t.y, t.z, 6, 0.4)
    }]
  ],

  dragon_spiral: c => {
    let steps = []
    for (let i = 0; i < 12; i++) {
      steps.push([2, c => {
        let t = faAim(c)
        let a = i * Math.PI / 3
        faBlink(c, t.x + Math.cos(a) * 2.5, t.y + i * 0.5, t.z + Math.sin(a) * 2.5)
        faFxAt(c, t.x, t.y + i * 0.5, t.z, 'minecraft:flame', 30, 1.2, 0.05, i % 4 == 0 ? 'minecraft:item.firecharge.use' : null, 1)
        faUltHit(c, c.target, 1)
        if (c.target.isAlive()) c.target.setSecondsOnFire(5)
      }])
    }
    steps.push([4, c => {
      let t = faAim(c)
      faFxAt(c, t.x, t.y + 1, t.z, 'minecraft:flame', 300, 4, 0.2, 'minecraft:entity.blaze.shoot', 0.5)
      faEnemiesNear(c.target.isAlive() ? c.target : c.player, 6, [c.player], 32).forEach(m => m.setSecondsOnFire(8))
      faUltAoe(c, t.x, t.y, t.z, 6, 1.5)
    }])
    return steps
  },

  void_collapse: c => {
    let steps = []
    c.caught = faEnemiesNear(c.target, 12, [c.player], 24).concat([c.target])
    for (let i = 0; i < 5; i++) {
      steps.push([2, c => {
        let t = faAim(c)
        c.caught.forEach(m => {
          if (!m.isAlive() || faIsBoss(m)) return
          let dx = t.x - m.getX()
          let dz = t.z - m.getZ()
          let d = Math.sqrt(dx * dx + dz * dz) || 1
          m.setDeltaMovement(dx / d * 1.2, 0.1, dz / d * 1.2)
          m.hurtMarked = true
        })
        faFxAt(c, t.x, t.y + 1, t.z, 'minecraft:reverse_portal', 80, 3, 0.4, i == 0 ? 'minecraft:entity.enderman.teleport' : null, 0.5)
      }])
    }
    steps.push([2, c => c.caught.forEach(m => faFreeze(m, 30))])
    steps.push([10, c => {
      let t = faAim(c)
      faFxAt(c, t.x, t.y + 1, t.z, 'minecraft:explosion_emitter', 1, 0.2, 0, 'minecraft:entity.generic.explode', 0.4)
      faFxAt(c, t.x, t.y + 1, t.z, 'minecraft:portal', 200, 2, 1, null)
      c.caught.forEach(m => faUltHit(c, m, 1))
    }])
    return steps
  },

  starfall: c => {
    let steps = [[1, c => {
      let t = faAim(c)
      faBlink(c, t.x, t.y + 10, t.z + 0.01)
      faFxAt(c, t.x, t.y + 10, t.z, 'minecraft:end_rod', 40, 1, 0.1, 'minecraft:block.beacon.activate', 1.5)
    }]]
    for (let i = 0; i < 10; i++) {
      steps.push([3, c => {
        let t = faAim(c)
        let pool = faEnemiesNear(c.target.isAlive() ? c.target : c.player, 8, [c.player], 16)
        if (c.target.isAlive()) pool.push(c.target)
        let hit = pool.length ? pool[Math.floor(Math.random() * pool.length)] : null
        let x = hit ? hit.getX() : t.x + (Math.random() * 8 - 4)
        let z = hit ? hit.getZ() : t.z + (Math.random() * 8 - 4)
        let y = hit ? hit.getY() : t.y
        faFxAt(c, x, y + 8, z, 'minecraft:flame', 40, 0.2, 0.3, null)
        faFxAt(c, x, y + 0.5, z, 'minecraft:explosion', 6, 0.8, 0, 'minecraft:entity.generic.explode', 1.2)
        if (hit) faUltHit(c, hit, 1)
        faUltAoe(c, x, y, z, 2, 0.5)
      }])
    }
    return steps
  },

  shadow_barrage: c => {
    let steps = []
    let foes = faEnemiesNear(c.player, 16, [c.player], 8)
    if (foes.indexOf(c.target) < 0) foes.unshift(c.target)
    foes.slice(0, 8).forEach(m => {
      steps.push([3, c => {
        if (!m.isAlive()) return
        let dx = m.getX() - c.origin.x
        let dz = m.getZ() - c.origin.z
        let d = Math.sqrt(dx * dx + dz * dz) || 1
        let saved = c.target
        c.target = m
        faBlink(c, m.getX() + dx / d * 1.5, m.getY(), m.getZ() + dz / d * 1.5)
        faFxAt(c, m.getX(), m.getY() + 1, m.getZ(), 'minecraft:large_smoke', 20, 0.5, 0.02, 'minecraft:entity.player.attack.crit', 1.3)
        faUltHit(c, m, 1)
        c.target = saved
      }])
    })
    return steps
  },

  frozen_eternity: c => {
    let steps = []
    c.caught = faEnemiesNear(c.player, 10, [c.player], 16)
    if (c.caught.indexOf(c.target) < 0) c.caught.push(c.target)
    steps.push([1, c => {
      c.caught.forEach(m => faFreeze(m, 100))
      faFxAt(c, c.origin.x, c.origin.y + 1, c.origin.z, 'minecraft:snowflake', 200, 6, 0.05, 'minecraft:block.glass.break', 0.5)
    }])
    c.caught.forEach(m => {
      steps.push([3, c => {
        if (!m.isAlive()) return
        let saved = c.target
        c.target = m
        faBlink(c, m.getX() + 1.2, m.getY(), m.getZ())
        faFxAt(c, m.getX(), m.getY() + 1, m.getZ(), 'minecraft:snowflake', 30, 0.5, 0.05, 'minecraft:block.amethyst_block.hit', 1.6)
        faUltHit(c, m, 0.5)
        c.target = saved
      }])
    })
    steps.push([6, c => {
      faFxAt(c, c.origin.x, c.origin.y + 1, c.origin.z, 'minecraft:item_snowball', 200, 6, 0.2, 'minecraft:block.glass.break', 0.8)
      c.caught.forEach(m => faUltHit(c, m, 1))
    }])
    return steps
  },

  spirit_bomb: c => {
    let steps = []
    for (let i = 0; i < 10; i++) {
      steps.push([4, c => {
        let p = c.player
        faFxAt(c, p.getX(), p.getY() + 4, p.getZ(), 'minecraft:end_rod', 10 + i * 8, 0.3 + i * 0.12, 0.02, i % 3 == 0 ? 'minecraft:block.beacon.power_select' : null, 0.6 + i * 0.1)
      }])
    }
    for (let i = 1; i <= 6; i++) {
      steps.push([2, c => {
        let p = c.player
        let t = faAim(c)
        let f = i / 6
        faFxAt(c, p.getX() + (t.x - p.getX()) * f, p.getY() + 4 + (t.y - p.getY() - 3) * f, p.getZ() + (t.z - p.getZ()) * f, 'minecraft:end_rod', 40, 0.8, 0.02, null)
      }])
    }
    steps.push([1, c => {
      let t = faAim(c)
      faFxAt(c, t.x, t.y + 1, t.z, 'minecraft:explosion_emitter', 3, 1.5, 0, 'minecraft:entity.generic.explode', 0.5)
      faFxAt(c, t.x, t.y + 1, t.z, 'minecraft:end_rod', 300, 5, 0.3, null)
      faUltHit(c, c.target, 1)
      faUltAoe(c, t.x, t.y, t.z, 6, 0.5)
    }])
    return steps
  },

  blade_storm: c => {
    let steps = []
    for (let i = 0; i < 12; i++) {
      steps.push([5, c => {
        let p = c.player
        faFxAt(c, p.getX(), p.getY() + 1, p.getZ(), 'minecraft:sweep_attack', 14, 2.5, 0, 'minecraft:entity.player.attack.sweep', 0.8 + (i % 4) * 0.1)
        faEnemiesNear(p, 5, [p], 24).forEach(m => faUltHit(c, m, 1))
      }])
    }
    return steps
  },

  eclipse: c => {
    let steps = [[1, c => {
      faEnemiesNear(c.player, 16, [c.player], 32).forEach(m => m.addEffect(new $FAEffectInstance($FAEffects.BLINDNESS, 100, 0, false, false)))
      faFxAt(c, c.origin.x, c.origin.y + 1, c.origin.z, 'minecraft:squid_ink', 150, 5, 0.05, 'minecraft:entity.wither.ambient', 0.5)
    }]]
    for (let i = 0; i < 4; i++) {
      steps.push([3, c => {
        let t = faAim(c)
        faFxAt(c, t.x, t.y + 16 - i * 4, t.z, 'minecraft:end_rod', 60, 0.4, 0, i == 0 ? 'minecraft:block.beacon.activate' : null, 0.5)
      }])
    }
    steps.push([3, c => {
      let t = faAim(c)
      faFxAt(c, t.x, t.y + 1, t.z, 'minecraft:flash', 1, 0, 0, 'minecraft:entity.lightning_bolt.thunder', 1.4)
      faUltHit(c, c.target, 1)
      faUltAoe(c, t.x, t.y, t.z, 4, 0.5)
      c.player.heal(c.player.getMaxHealth() * 0.25)
    }])
    return steps
  },

  phoenix_dive: c => {
    let steps = []
    let sky = () => { let t = faAim(c); return { x: t.x, y: t.y + 14, z: t.z } }
    for (let i = 1; i <= 4; i++) {
      steps.push([2, c => {
        let top = sky()
        let f = i / 4
        let x = c.origin.x + (top.x - c.origin.x) * f
        let y = c.origin.y + (top.y - c.origin.y) * f
        let z = c.origin.z + (top.z - c.origin.z) * f
        faBlink(c, x, y, z + 0.01)
        faFxAt(c, x, y, z, 'minecraft:flame', 40, 0.4, 0.05, i == 1 ? 'minecraft:entity.blaze.shoot' : null, 0.8)
      }])
    }
    steps.push([6, c => { let top = sky(); faFxAt(c, top.x, top.y, top.z, 'minecraft:flame', 120, 1.5, 0.1, 'minecraft:entity.ender_dragon.flap', 1.2) }])
    for (let i = 1; i <= 5; i++) {
      steps.push([1, c => {
        let t = faAim(c)
        let top = sky()
        let f = i / 5
        let x = top.x + (t.x - top.x) * f
        let y = top.y + (t.y + 1 - top.y) * f
        let z = top.z + (t.z - top.z) * f + 0.01
        faBlink(c, x, y, z)
        faFxAt(c, x, y, z, 'minecraft:flame', 40, 0.4, 0.05, null)
      }])
    }
    steps.push([1, c => {
      let t = faAim(c)
      faFxAt(c, t.x, t.y + 1, t.z, 'minecraft:explosion_emitter', 2, 1, 0, 'minecraft:entity.generic.explode', 0.8)
      faFxAt(c, t.x, t.y + 1, t.z, 'minecraft:flame', 200, 3, 0.2, null)
      faUltHit(c, c.target, 1)
      faUltAoe(c, t.x, t.y, t.z, 4, 0.6)
      faEnemiesNear(c.target.isAlive() ? c.target : c.player, 4, [c.player], 16).forEach(m => m.setSecondsOnFire(6))
    }])
    return steps
  }
}

// ---- Running a combo ----

function faEndCombo(c) {
  let uuid = c.uuid
  if (faCombos[uuid] !== c) return
  delete faCombos[uuid]
  try {
    // Moves that teleported you put you back exactly where you started (even across dimensions)
    if (c.moved && c.player.isAlive() && !c.player.isRemoved()) {
      c.player.teleportTo(c.originLevel, c.origin.x, c.origin.y, c.origin.z, c.origin.yaw, c.origin.pitch)
      c.player.fallDistance = 0
      faFxAt(c, c.origin.x, c.origin.y + 1, c.origin.z, 'minecraft:portal', 30, 0.5, 0.2, 'minecraft:entity.enderman.teleport', 1.4)
    }
  } catch (err) {
    faWarnOnce('ultimate return', err)
  }
}

function faStartCombo(player, target, id, level, baseDamage) {
  let e = faE(id)
  let uuid = player.getStringUUID()
  let c = {
    uuid: uuid, id: id, player: player, target: target, level: player.level(), originLevel: player.level(), moved: false,
    origin: { x: player.getX(), y: player.getY(), z: player.getZ(), yaw: player.getYRot(), pitch: player.getXRot() },
    dmg: Math.min(baseDamage * global.FA_ultPower(e, level), 3.0e38),
    tx: target.getX(), ty: target.getY(), tz: target.getZ()
  }
  faCombos[uuid] = c
  faUltReady[uuid + '|' + id] = faTick + global.FA_ultCooldown(e, level) * 20

  let server = player.getServer()
  if (server) {
    let name = player.getScoreboardName()
    server.runCommandSilent('title ' + name + ' times 2 25 8')
    server.runCommandSilent('title ' + name + ' title ""')
    server.runCommandSilent('title ' + name + ' subtitle ' + JSON.stringify({ text: e.name.toUpperCase(), color: 'gold', bold: true }))
  }

  let steps = FA_MOVES[e.move](c)
  let at = 0
  steps.forEach(s => {
    at += s[0]
    faLater(at, () => {
      if (faCombos[uuid] !== c) return
      // Abort cleanly if the player is gone, dead, or somewhere else
      if (!c.player.isAlive() || c.player.isRemoved() || c.player.level() !== c.level) return faEndCombo(c)
      s[1](c)
    })
  })
  faLater(at + 4, () => faEndCombo(c))
  faLater(at + 60, () => faEndCombo(c))   // failsafe: always home, even if a step crashed
}

// Trigger: a sneaking, direct melee hit on a mob, with an Ultimate ready
ForgeEvents.onEvent('net.minecraftforge.event.entity.living.LivingHurtEvent', event => {
  if (faSecondary) return
  try {
    let source = event.getSource()
    let player = source.getEntity()
    let target = event.getEntity()
    if (!(player instanceof $FAPlayer) || source.getDirectEntity() !== player || !player.isShiftKeyDown()) return
    if (!(target instanceof $FALiving) || target instanceof $FAPlayer || faCombos[player.getStringUUID()]) return
    let levels = faLevels(player)
    let best = null
    faIds('ultimate').forEach(id => {
      let level = levels[id] || 0
      if (level <= 0 || (faUltReady[player.getStringUUID() + '|' + id] || 0) > faTick) return
      if (!best || level > best.level) best = { id: id, level: level }
    })
    if (best) faStartCombo(player, target, best.id, best.level, event.getAmount())
  } catch (err) {
    faWarnOnce('ultimate trigger', err)
  }
})

// Untouchable mid-combo (except /kill and the void), and no fall damage from it
ForgeEvents.onEvent('net.minecraftforge.event.entity.living.LivingAttackEvent', event => {
  try {
    if (faInCombo(event.getEntity()) && !event.getSource().is($FADamageTypeTags.BYPASSES_INVULNERABILITY)) event.setCanceled(true)
  } catch (err) {
    faWarnOnce('ultimate shield', err)
  }
})
ForgeEvents.onEvent('net.minecraftforge.event.entity.living.LivingFallEvent', event => {
  try {
    if (faInCombo(event.getEntity())) event.setDamageMultiplier(0)
  } catch (err) {
    faWarnOnce('ultimate landing', err)
  }
})

// Logging out mid-combo: put the player back first, so they don't log in floating over a monster
ForgeEvents.onEvent('net.minecraftforge.event.entity.player.PlayerEvent$PlayerLoggedOutEvent', event => {
  try {
    let c = faCombos[event.getEntity().getStringUUID()]
    if (c) faEndCombo(c)
  } catch (err) {
    faWarnOnce('ultimate logout', err)
  }
})
