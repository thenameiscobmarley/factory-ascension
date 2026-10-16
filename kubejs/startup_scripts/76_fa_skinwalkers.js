// priority: 270
// Factory Ascension - skinwalkers (config: skinwalkers). A few animals out in the danger zones are something else.
// They look normal until a player comes near; then they follow and stare, and in the end shapeshift into a strong mob,
// and only that form is named. Uses faLater (40_fa_ultimates.js), faWarnOnce and the $FA classes (20_fa_enchant_effects.js),
// and the danger and variant helpers (same startup scope).

let faSkinwalkers = {}   // uuid -> { mob, stalk (seconds near a player), shifting }

function faIsSkinwalker(mob) {
  return mob && mob.getPersistentData && mob.getPersistentData().getBoolean('fa_skinwalker')
}

function faNearestSurvivor(mob, range) {
  let server = mob.level.getServer(), best = null, bestD = range * range
  if (!server) return null
  server.getPlayerList().getPlayers().forEach(p => {
    if (p.level !== mob.level && String(p.level.dimension) != String(mob.level.dimension)) return
    if (p.isCreative() || p.isSpectator()) return
    let d = Math.pow(p.getX() - mob.getX(), 2) + Math.pow(p.getY() - mob.getY(), 2) + Math.pow(p.getZ() - mob.getZ(), 2)
    if (d < bestD) { bestD = d; best = p }
  })
  return best
}

function faAt(mob) {
  return mob.getX().toFixed(2) + ' ' + (mob.getY() + 0.6).toFixed(2) + ' ' + mob.getZ().toFixed(2)
}

// The shapeshift: freeze and twitch, bones crack, it rises, then it bursts into its true form. fast: for the self test.
global.FA_shapeshift = (mob, target, fast) => {
  let entry = faSkinwalkers[mob.stringUuid] || (faSkinwalkers[mob.stringUuid] = { mob: mob, stalk: 0 })
  if (entry.shifting) return false
  entry.shifting = true
  let S = global.FA.skinwalkers
  let level = mob.level, server = level.getServer(), dim = String(level.dimension)
  let cmd = c => server.runCommandSilent('execute in ' + dim + ' run ' + c)
  let step = fast ? 0 : 1
  mob.setNoAi(true)
  mob.setInvulnerable(true)
  let x0 = mob.getX(), y0 = mob.getY(), z0 = mob.getZ()
  // 1. It stops, turns to you, and twitches
  for (let i = 0; i < 10; i++) faLater(1 + i * 4 * step, () => {
    if (mob.isRemoved()) return
    let yaw = target ? Math.atan2(target.getZ() - z0, target.getX() - x0) * 180 / Math.PI - 90 : 0
    mob.setPositionAndRotation(x0 + (Math.random() - 0.5) * 0.12, mob.getY(), z0 + (Math.random() - 0.5) * 0.12, yaw + (Math.random() - 0.5) * 70, (Math.random() - 0.5) * 60)
    cmd('particle minecraft:squid_ink ' + faAt(mob) + ' 0.3 0.4 0.3 0.02 6 normal')
    cmd('playsound minecraft:entity.skeleton.hurt hostile @a ' + faAt(mob) + ' 0.9 ' + (0.4 + Math.random() * 0.3).toFixed(2))
    if (i % 3 == 0) cmd('playsound minecraft:block.bone_block.break hostile @a ' + faAt(mob) + ' 1 0.5')
  })
  faLater(2, () => cmd('playsound minecraft:entity.warden.heartbeat hostile @a ' + faAt(mob) + ' 2 0.6'))
  // 2. It rises, and the light goes out of the world around it
  for (let i = 0; i < 6; i++) faLater(42 * step + 1 + i * 3 * step, () => {
    if (mob.isRemoved()) return
    mob.setPosition(mob.getX(), y0 + (i + 1) * 0.15, mob.getZ())
    cmd('particle minecraft:soul ' + faAt(mob) + ' 0.4 0.5 0.4 0.03 10 normal')
  })
  faLater(42 * step + 1, () => {
    level.getEntitiesOfClass($FAPlayer, mob.getBoundingBox().inflate(16)).forEach(p => {
      p.addEffect(new $FAEffectInstance($FAEffects.DARKNESS, 100, 0))
      p.addEffect(new $FAEffectInstance($FAEffects.CONFUSION, 70, 0))
    })
  })
  // 3. The burst, and what was inside
  faLater(64 * step + 2, () => {
    if (mob.isRemoved()) return
    let animal = mob.getDisplayName().getString()
    let at = faAt(mob)
    cmd('particle minecraft:large_smoke ' + at + ' 0.6 0.8 0.6 0.05 50 force')
    cmd('particle minecraft:soul_fire_flame ' + at + ' 0.5 0.8 0.5 0.08 40 force')
    cmd('playsound minecraft:entity.warden.roar hostile @a ' + at + ' 3 0.8')
    cmd('playsound minecraft:entity.generic.explode hostile @a ' + at + ' 1 0.6')
    let formId = global.FA_skinwalkerForm(Math.random, id => Platform.isLoaded(id.split(':')[0]))
    let form = level.createEntity(formId)
    if (!form) form = level.createEntity('minecraft:vindicator')
    let fd = form.getPersistentData()
    fd.putBoolean('fa_skinwalker_form', true)
    form.setPositionAndRotation(mob.getX(), y0, mob.getZ(), mob.yaw, 0)
    mob.discard()
    delete faSkinwalkers[mob.stringUuid]
    level.addFreshEntity(form)
    // Out here the danger tag is already on it (the join handlers ran); turn it up, and make it an Elite
    let danger = Math.max(1, fd.getInt('fa_danger')) + S.dangerBonus
    fd.putInt('fa_danger', danger)
    fd.putBoolean('fa_elite', true)
    if (Math.random() < S.variantChance) {
      let kinds = global.FA.variants.types.map(t => t.id)
      global.FA_makeVariant(form, kinds[Math.floor(Math.random() * kinds.length)])
    }
    let shown = form.hasCustomName() ? form.getCustomName() : form.getDisplayName()
    form.setCustomName(Text.darkRed('☠ Skinwalker ').bold(true).append(shown).append(Text.gray(' · Lv ' + danger)))
    form.setCustomNameVisible(true)
    if (form.setPersistenceRequired) form.setPersistenceRequired()
    if (target && form.setTarget) form.setTarget(target)
    if (target) {
      let name = target.username
      server.runCommandSilent('title ' + name + ' times 5 50 20')
      server.runCommandSilent('title ' + name + ' subtitle ' + JSON.stringify({ text: 'That was never a ' + animal.toLowerCase() + '.', color: 'gray', italic: true }))
      server.runCommandSilent('title ' + name + ' title ' + JSON.stringify({ text: 'SKINWALKER', color: 'dark_red', bold: true }))
    }
  })
  return true
}

// Mark a mob as a skinwalker (spawns, and the self test)
global.FA_makeSkinwalker = mob => {
  mob.getPersistentData().putBoolean('fa_skinwalker', true)
  faSkinwalkers[mob.stringUuid] = { mob: mob, stalk: 0 }
  return true
}

ForgeEvents.onEvent('net.minecraftforge.event.entity.EntityJoinLevelEvent', event => {
  try {
    let S = global.FA.skinwalkers
    if (!S.enabled || event.getLevel().isClientSide()) return
    let mob = event.getEntity()
    if (!(mob instanceof $FALiving) || mob instanceof $FAPlayer) return
    if (faIsSkinwalker(mob)) {
      faSkinwalkers[mob.stringUuid] = { mob: mob, stalk: 0 }   // back from disk, or marked before it joined
      return
    }
    if (event.loadedFromDisk() || S.animals.indexOf(String(mob.type)) < 0 || mob.isBaby()) return
    let data = mob.getPersistentData()
    if (data.contains('fa_variant') || mob.hasCustomName()) return
    let deep = global.FA_dangerLevel(mob.level, mob.getX(), mob.getZ()) >= S.minDanger
    if (Math.random() >= (deep ? S.chance : S.safeChance)) return
    global.FA_makeSkinwalker(mob)
  } catch (err) {
    faWarnOnce('skinwalker spawns', err)
  }
})

// Hit one and it stops pretending (the hit does nothing to it)
ForgeEvents.onEvent('net.minecraftforge.event.entity.living.LivingAttackEvent', event => {
  try {
    let mob = event.getEntity()
    if (!faIsSkinwalker(mob) || mob.level.isClientSide()) return
    // Only a creature's hit gives it away; falls, fire, the void and /kill hurt it like anything else
    let who = event.getSource().actual
    if (!(who instanceof $FALiving)) return
    event.setCanceled(true)
    global.FA_shapeshift(mob, who instanceof $FAPlayer ? who : faNearestSurvivor(mob, 32), false)
  } catch (err) {
    faWarnOnce('skinwalker hits', err)
  }
})

// Twice a second: follow the nearest player at a distance, stop and stare, and maybe turn
ForgeEvents.onEvent('net.minecraftforge.event.TickEvent$ServerTickEvent', event => {
  try {
    if (String(event.phase) != 'END') return
    let server = event.getServer()
    if (server.getTickCount() % 10 != 5) return
    let S = global.FA.skinwalkers
    Object.keys(faSkinwalkers).forEach(uuid => {
      let e = faSkinwalkers[uuid], mob = e.mob
      if (!mob.isAlive() || mob.isRemoved()) { delete faSkinwalkers[uuid]; return }
      if (e.shifting || !S.enabled) return
      let p = faNearestSurvivor(mob, S.noticeRange)
      if (!p) { e.stalk = 0; return }
      let d = Math.sqrt(Math.pow(p.getX() - mob.getX(), 2) + Math.pow(p.getZ() - mob.getZ(), 2))
      if (d > S.stareRange + 4) mob.getNavigation().moveTo(p, 1.0)
      else {
        mob.getNavigation().stop()
        mob.getLookControl().setLookAt(p, 30, 30)
      }
      // Mimicry: a sound from where it stands, or just behind you
      if (Math.random() < S.mimicChancePerSecond * 0.5) {
        let sounds = ['minecraft:entity.player.hurt', 'minecraft:block.wooden_door.open', 'minecraft:entity.villager.ambient', 'minecraft:block.grass.step',
          'minecraft:block.chest.open', 'minecraft:entity.player.breath', 'minecraft:entity.villager.no', 'minecraft:item.armor.equip_leather']
        let snd = sounds[Math.floor(Math.random() * sounds.length)]
        let behind = Math.random() < 0.5
        let yaw = p.yaw * Math.PI / 180
        let sx = behind ? p.getX() + Math.sin(yaw) * 4 : mob.getX(), sz = behind ? p.getZ() - Math.cos(yaw) * 4 : mob.getZ()
        server.runCommandSilent('execute in ' + String(mob.level.dimension) + ' run playsound ' + snd + ' hostile ' + p.username + ' ' +
          sx.toFixed(1) + ' ' + p.getY().toFixed(1) + ' ' + sz.toFixed(1) + ' 0.8 ' + (0.75 + Math.random() * 0.2).toFixed(2))
      }
      if (d > S.stareRange * 2) return
      e.stalk += 0.5
      let chance = S.shiftChancePerSecond * 0.5 * (mob.level.isNight() ? S.nightMult : 1)
      if (e.stalk >= S.minStalkSeconds && Math.random() < chance) global.FA_shapeshift(mob, p, false)
    })
  } catch (err) {
    faWarnOnce('skinwalker stalking', err)
  }
})

// Killing a revealed skinwalker: extra loot, a tier better, and maybe a Mythic Fragment
ForgeEvents.onEvent('net.minecraftforge.event.entity.living.LivingDropsEvent', event => {
  try {
    let mob = event.getEntity()
    if (!mob.getPersistentData().getBoolean('fa_skinwalker_form') || !(event.getSource().actual instanceof $FAPlayer)) return
    let S = global.FA.skinwalkers
    let danger = mob.getPersistentData().getInt('fa_danger')
    let drops = event.getDrops()
    let add = stack => {
      let it = new $FAItemEntity(mob.level, mob.getX(), mob.getY() + 0.5, mob.getZ(), stack)
      it.setDefaultPickUpDelay()
      drops.add(it)
    }
    for (let i = 0; i < S.lootRolls; i++) {
      let roll = global.FA_rollLoot(Math.random, danger, 1)
      add(roll.drop.kind == 'relic' ? faRelicStack(roll.drop.relic) : Item.of(roll.drop.id, Math.min(roll.drop.count, 64)))
    }
    if (Math.random() < S.fragmentChance) add(Item.of('kubejs:mythic_fragment'))
  } catch (err) {
    faWarnOnce('skinwalker drops', err)
  }
})

// For the self test
global.FA_skinwalkerCount = () => Object.keys(faSkinwalkers).length
