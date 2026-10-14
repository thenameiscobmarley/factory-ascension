// Factory Ascension - what the Ascended enchants do.
// Enchant levels live in each item's NBT under "FactoryAscension", e.g. {sharpness: 3000}.
// A player's level in an enchant = the total across both hands and all armor.
// Every enchant has a "type" in 00_fa_config.js; each type is handled once here.
// Forge events can only be hooked from startup scripts.

const $FAPlayer = Java.loadClass('net.minecraft.world.entity.player.Player')
const $FALiving = Java.loadClass('net.minecraft.world.entity.LivingEntity')
const $FAEnemy = Java.loadClass('net.minecraft.world.entity.monster.Enemy')
const $FARaider = Java.loadClass('net.minecraft.world.entity.raid.Raider')
const $FAMobType = Java.loadClass('net.minecraft.world.entity.MobType')
const $FAItemEntity = Java.loadClass('net.minecraft.world.entity.item.ItemEntity')
const $FAOrb = Java.loadClass('net.minecraft.world.entity.ExperienceOrb')
const $FADamageTypeTags = Java.loadClass('net.minecraft.tags.DamageTypeTags')
const $FAEffects = Java.loadClass('net.minecraft.world.effect.MobEffects')
const $FAEffectInstance = Java.loadClass('net.minecraft.world.effect.MobEffectInstance')
const $FAAttributes = Java.loadClass('net.minecraft.world.entity.ai.attributes.Attributes')
const $FAModifier = Java.loadClass('net.minecraft.world.entity.ai.attributes.AttributeModifier')
const $FAOperation = Java.loadClass('net.minecraft.world.entity.ai.attributes.AttributeModifier$Operation')
const $FAForgeMod = Java.loadClass('net.minecraftforge.common.ForgeMod')

const FA_TAG = 'FactoryAscension'
const FA_MAX_FLOAT = 3.0e38

// A broken hook logs once and stays out of the way instead of spamming the log
let faWarned = {}
function faWarnOnce(where, err) {
  if (faWarned[where]) return
  faWarned[where] = true
  console.error('[Factory Ascension] ' + where + ' hook failed, skipping it: ' + err)
}

// Enchant ids grouped by type, e.g. FA_BY_TYPE.damage = ['sharpness', 'smite', ...]
let FA_BY_TYPE = {}
Object.keys(global.FA.enchants).forEach(id => {
  let type = global.FA.enchants[id].type
  if (!FA_BY_TYPE[type]) FA_BY_TYPE[type] = []
  FA_BY_TYPE[type].push(id)
})
function faIds(type) {
  return FA_BY_TYPE[type] || []
}

// All enchant levels on a player's hands + armor, summed: {sharpness: 120, protection: 3000}.
// Returns an empty object for anything that isn't a player.
function faLevels(entity) {
  let levels = {}
  if (!entity || !(entity instanceof $FAPlayer)) return levels
  let it = entity.getAllSlots().iterator()
  while (it.hasNext()) {
    let tag = it.next().getTag()
    if (!tag || !tag.contains(FA_TAG)) continue
    let data = tag.getCompound(FA_TAG)
    let keys = data.getAllKeys().iterator()
    while (keys.hasNext()) {
      let id = String(keys.next())
      levels[id] = (levels[id] || 0) + data.getInt(id)
    }
  }
  return levels
}

function faE(id) {
  return global.FA.enchants[id]
}

// Danger data saved on a scaled mob: {level, elite}, or null for normal mobs and players
function faDanger(entity) {
  if (!entity || !(entity instanceof $FALiving) || entity instanceof $FAPlayer) return null
  let data = entity.getPersistentData()
  if (!data.contains('fa_danger')) return null
  return global.FA_dangerMult(data.getInt('fa_danger'), data.getBoolean('fa_elite'))
}

// ---- Conditions ----

function faIsBoss(entity) {
  // Vanilla and most modded bosses can't change dimensions; huge health pools count too
  return !entity.canChangeDimensions() || entity.getMaxHealth() >= 200
}

function faWhen(when, source, attacker, target) {
  switch (when) {
    case 'always': return true
    case 'undead': return target.getMobType() == $FAMobType.UNDEAD
    case 'arthropod': return target.getMobType() == $FAMobType.ARTHROPOD
    case 'boss': return faIsBoss(target)
    case 'nether': return String(target.level().dimension().location()) == 'minecraft:the_nether'
    case 'raider': return target instanceof $FARaider
    case 'wet': return target.getMobType() == $FAMobType.WATER || target.isInWaterOrRain()
    case 'airborne': return !target.onGround()
    case 'full_health': return target.getHealth() >= target.getMaxHealth()
    case 'low_health': return target.getHealth() <= target.getMaxHealth() * 0.3
    case 'berserk': return attacker.getHealth() <= attacker.getMaxHealth() * 0.5
    case 'projectile': return source.is($FADamageTypeTags.IS_PROJECTILE)
    case 'sneaking': return attacker.isShiftKeyDown()
  }
  return false
}

function faResists(vs, source) {
  let id = String(source.getMsgId())
  switch (vs) {
    case 'all': return true
    case 'fire': return source.is($FADamageTypeTags.IS_FIRE)
    case 'fall': return source.is($FADamageTypeTags.IS_FALL)
    case 'explosion': return source.is($FADamageTypeTags.IS_EXPLOSION)
    case 'projectile': return source.is($FADamageTypeTags.IS_PROJECTILE)
    case 'magic': return id == 'magic' || id == 'indirectMagic'
    case 'wither': return id == 'wither' || id == 'witherSkull'
    case 'melee': return id == 'mob' || id == 'player'
  }
  return false
}

function faEffect(e, ticks, amp) {
  // ambient, no particles: keeps the screen clean
  return new $FAEffectInstance($FAEffects[e.effect], ticks, amp, true, false)
}

// ---- Combat ----

// Set while Cleave or Retribution deal their own damage, so those hits don't trigger
// more enchants (no chain reactions, no infinite reflect loops)
let faSecondary = false
let faUndyingReady = {}

function faSecondaryHit(target, source, amount) {
  faSecondary = true
  try {
    target.hurt(source, Math.min(amount, FA_MAX_FLOAT))
  } finally {
    faSecondary = false
  }
}

// Evasion: cancel the attack before it lands
ForgeEvents.onEvent('net.minecraftforge.event.entity.living.LivingAttackEvent', event => {
  try {
    if (faSecondary || event.getSource().is($FADamageTypeTags.BYPASSES_INVULNERABILITY)) return
    let levels = faLevels(event.getEntity())
    faIds('dodge').forEach(id => {
      let level = levels[id] || 0
      if (level > 0 && !event.isCanceled() && Math.random() < Math.min(faE(id).cap, global.FA_chance(level))) {
        event.setCanceled(true)
      }
    })
  } catch (err) {
    faWarnOnce('evasion', err)
  }
})

ForgeEvents.onEvent('net.minecraftforge.event.entity.living.LivingHurtEvent', event => {
  if (faSecondary) return
  try {
    let source = event.getSource()
    let attacker = source.getEntity() // the owner for arrows and other projectiles
    let victim = event.getEntity()
    let amount = event.getAmount()
    let A = faLevels(attacker)
    let V = faLevels(victim)
    let scaled = amount

    // ---- Attacker's damage ----
    faIds('damage').forEach(id => {
      let level = A[id] || 0
      if (level > 0 && faWhen(faE(id).when, source, attacker, victim)) scaled *= 1 + faE(id).perLevel * level
    })
    faIds('critical').forEach(id => {
      let level = A[id] || 0
      if (level > 0 && Math.random() < global.FA_chance(level)) scaled *= faE(id).critMultiplier
    })

    // ---- Danger: far-out mobs hit harder and take longer to kill ----
    let bypass = source.is($FADamageTypeTags.BYPASSES_INVULNERABILITY)
    let attackerDanger = faDanger(attacker)
    if (attackerDanger) scaled *= attackerDanger.damage
    let victimDanger = faDanger(victim)
    if (victimDanger && !bypass) scaled /= victimDanger.health

    // ---- Victim's defenses (/kill and the void ignore them, so nobody gets stuck immortal) ----
    if (!bypass) {
      faIds('resist').forEach(id => {
        let level = V[id] || 0
        if (level > 0 && faResists(faE(id).vs, source)) scaled /= 1 + faE(id).perLevel * level
      })
      // Vitality: 20 base health + V hidden health, so each hit does 20 / (20 + V) as much
      faIds('vitality').forEach(id => {
        let level = V[id] || 0
        if (level > 0) scaled *= 20 / (20 + faE(id).perLevel * level)
      })
    }

    // ---- Reaper: finish off weakened non-bosses ----
    faIds('reaper').forEach(id => {
      let level = A[id] || 0
      if (level > 0 && !faIsBoss(victim) &&
        victim.getHealth() - scaled <= victim.getMaxHealth() * Math.min(faE(id).cap, faE(id).perLevel * level)) {
        scaled = Math.max(scaled, victim.getHealth() + 1)
      }
    })

    // ---- Undying: a killing blow leaves you at 1 health, then it needs to recharge ----
    if (!bypass) {
      faIds('undying').forEach(id => {
        let level = V[id] || 0
        if (level <= 0 || scaled < victim.getHealth()) return
        let uuid = victim.getStringUUID()
        let now = victim.level().getGameTime()
        if (faUndyingReady[uuid] && now < faUndyingReady[uuid]) return
        faUndyingReady[uuid] = now + Math.round(global.FA_undyingCooldown(faE(id), level) * 20)
        scaled = Math.max(0, victim.getHealth() - 1)
        victim.displayClientMessage(Text.gold('Undying saved you!'), true)
      })
    }

    scaled = Math.min(scaled, FA_MAX_FLOAT)
    if (scaled != amount) event.setAmount(scaled)

    // ---- Attacker's on-hit effects ----
    if (attacker instanceof $FAPlayer && attacker !== victim && attacker.isAlive()) {
      let heal = 0
      faIds('lifesteal').forEach(id => { heal += scaled * faE(id).perLevel * (A[id] || 0) })
      faIds('hit_heal').forEach(id => { heal += faE(id).perLevel * (A[id] || 0) })
      if (heal > 0) attacker.heal(Math.min(heal, FA_MAX_FLOAT))

      faIds('hit_effect').forEach(id => {
        let level = A[id] || 0
        if (level > 0) victim.addEffect(faEffect(faE(id), faE(id).seconds * 20, global.FA_amp(faE(id), level)))
      })
      faIds('self_effect').forEach(id => {
        let level = A[id] || 0
        if (level > 0) attacker.addEffect(faEffect(faE(id), faE(id).seconds * 20, global.FA_amp(faE(id), level)))
      })
      faIds('ignite').forEach(id => {
        let level = A[id] || 0
        if (level > 0) victim.setSecondsOnFire(Math.min(1000000, Math.ceil(faE(id).perLevel * level)))
      })

      faIds('cleave').forEach(id => {
        let level = A[id] || 0
        if (level <= 0) return
        let e = faE(id)
        let nearby = victim.level().getEntitiesOfClass($FALiving, victim.getBoundingBox().inflate(e.radius))
        let hits = 0
        for (let i = 0; i < nearby.size() && hits < global.FA.maxCleaveTargets; i++) {
          let other = nearby.get(i)
          // Only hostile mobs: never players, pets or villagers
          if (other === victim || other === attacker || !(other instanceof $FAEnemy) || !other.isAlive()) continue
          faSecondaryHit(other, attacker.damageSources().playerAttack(attacker), scaled * e.perLevel * level)
          hits++
        }
      })
    }

    // ---- Victim's Retribution ----
    if (attacker && attacker !== victim && attacker instanceof $FALiving && attacker.isAlive()) {
      faIds('retribution').forEach(id => {
        let level = V[id] || 0
        if (level > 0) faSecondaryHit(attacker, victim.damageSources().thorns(victim), amount * faE(id).perLevel * level)
      })
    }
  } catch (err) {
    faWarnOnce('damage', err)
  }
})

// ---- Kills ----

ForgeEvents.onEvent('net.minecraftforge.event.entity.living.LivingDeathEvent', event => {
  try {
    let killer = event.getSource().getEntity()
    if (!(killer instanceof $FAPlayer) || !killer.isAlive()) return
    let levels = faLevels(killer)
    let heal = 0
    faIds('kill_heal').forEach(id => { heal += faE(id).perLevel * (levels[id] || 0) })
    if (heal > 0) killer.heal(Math.min(heal, FA_MAX_FLOAT))
  } catch (err) {
    faWarnOnce('soul eater', err)
  }
})

ForgeEvents.onEvent('net.minecraftforge.event.entity.living.LivingExperienceDropEvent', event => {
  try {
    let levels = faLevels(event.getAttackingPlayer())
    let mult = 1
    faIds('xp').forEach(id => { mult += faE(id).perLevel * (levels[id] || 0) })
    let danger = faDanger(event.getEntity())
    if (danger) mult *= danger.xp
    if (mult > 1) event.setDroppedExperience(Math.min(2000000000, Math.round(event.getDroppedExperience() * mult)))
  } catch (err) {
    faWarnOnce('wisdom', err)
  }
})

// Fractional amounts become a chance for one more: 2.3 -> 2, or 3 with 30% chance
function faRoll(exact) {
  let n = Math.floor(exact)
  return Math.random() < exact - n ? n + 1 : n
}

ForgeEvents.onEvent('net.minecraftforge.event.entity.living.LivingDropsEvent', event => {
  try {
    // Never touch a player's death drops
    if (event.getEntity() instanceof $FAPlayer) return
    let levels = faLevels(event.getSource().getEntity())
    let danger = faDanger(event.getEntity())
    if (Object.keys(levels).length == 0 && !danger) return

    let budget = global.FA.maxExtraDropStacks
    let drops = event.getDrops()
    let extras = []
    let spawnAt = null
    let addStacks = (stack, total) => {
      let max = stack.getMaxStackSize()
      while (total > 0 && budget > 0) {
        let n = Math.min(total, max)
        let extra = new $FAItemEntity(spawnAt.level(), spawnAt.getX(), spawnAt.getY(), spawnAt.getZ(), stack.copyWithCount(n))
        extra.setDefaultPickUpDelay()
        extras.push(extra)
        total -= n
        budget--
      }
    }

    // Plunder and danger: multiply what dropped
    let mult = 1
    faIds('plunder').forEach(id => { mult += faE(id).perLevel * (levels[id] || 0) })
    if (danger) mult *= danger.loot
    let it = drops.iterator()
    while (it.hasNext()) {
      let itemEntity = it.next()
      spawnAt = itemEntity
      let stack = itemEntity.getItem()
      if (mult <= 1 || stack.isEmpty()) continue
      let total = faRoll(stack.getCount() * mult)
      let max = stack.getMaxStackSize()
      itemEntity.setItem(stack.copyWithCount(Math.min(total, max)))
      addStacks(stack, total - max)
    }

    // Midas, Gem Hunter: bonus items
    faIds('drop').forEach(id => {
      let level = levels[id] || 0
      if (level <= 0) return
      let count = faRoll(faE(id).perLevel * level)
      if (count <= 0) return
      if (!spawnAt) spawnAt = event.getEntity()
      addStacks(Item.of(faE(id).item), count)
    })

    extras.forEach(e => drops.add(e))
  } catch (err) {
    faWarnOnce('loot', err)
  }
})

// ---- Attributes (Swiftness, Reach, Stride, Frenzy, Lucky, Knockback) ----

function faAttribute(key) {
  switch (key) {
    case 'movement_speed': return $FAAttributes.MOVEMENT_SPEED
    case 'attack_speed': return $FAAttributes.ATTACK_SPEED
    case 'attack_knockback': return $FAAttributes.ATTACK_KNOCKBACK
    case 'luck': return $FAAttributes.LUCK
    case 'block_reach': return $FAForgeMod.BLOCK_REACH.get()
    case 'entity_reach': return $FAForgeMod.ENTITY_REACH.get()
    case 'step_height': return $FAForgeMod.STEP_HEIGHT_ADDITION.get()
  }
  return null
}

function faSetModifier(player, attribute, name, amount, operation) {
  let instance = player.getAttribute(attribute)
  if (!instance) return
  let old = []
  let it = instance.getModifiers().iterator()
  while (it.hasNext()) {
    let mod = it.next()
    if (mod.getName() == name) old.push(mod)
  }
  // Skip the work when nothing changed (equipment events fire a lot)
  if (old.length == 1 && old[0].getAmount() == amount) return
  old.forEach(mod => instance.removeModifier(mod))
  if (amount > 0) instance.addTransientModifier(new $FAModifier(name, amount, operation))
}

// Fires whenever equipment changes, including login, respawn and enchant upgrades
ForgeEvents.onEvent('net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent', event => {
  try {
    let player = event.getEntity()
    if (!(player instanceof $FAPlayer)) return
    let levels = faLevels(player)
    faIds('attribute').forEach(id => {
      let e = faE(id)
      let amount = e.perLevel * (levels[id] || 0)
      if (e.cap !== undefined) amount = Math.min(e.cap, amount)
      let op = e.percent ? $FAOperation.MULTIPLY_BASE : $FAOperation.ADDITION
      e.attrs.forEach(key => {
        let attribute = faAttribute(key)
        if (attribute) faSetModifier(player, attribute, 'factory_ascension_' + id, amount, op)
      })
    })
  } catch (err) {
    faWarnOnce('attributes', err)
  }
})

// ---- Danger: tag hostile mobs when they first spawn ----
// The level is saved on the mob (persistent data), so it keeps it across saves and reloads.

ForgeEvents.onEvent('net.minecraftforge.event.entity.EntityJoinLevelEvent', event => {
  try {
    let d = global.FA.danger
    if (!d.enabled || event.loadedFromDisk()) return
    let mob = event.getEntity()
    let level = event.getLevel()
    if (level.isClientSide() || !(mob instanceof $FAEnemy) || !(mob instanceof $FALiving) || faIsBoss(mob)) return
    let data = mob.getPersistentData()
    if (data.contains('fa_danger')) return

    let danger = global.FA_dangerLevel(level, mob.getX(), mob.getZ())
    if (danger <= 0) return
    data.putInt('fa_danger', danger)

    if (Math.random() < Math.min(d.eliteChanceCap, d.eliteChancePerLevel * danger)) {
      data.putBoolean('fa_elite', true)
      mob.setCustomName(Text.red('\u2620 Elite ').append(mob.getType().getDescription()).append(Text.gray(' \u00b7 Lv ' + danger)))
      mob.setCustomNameVisible(true)
    }
  } catch (err) {
    faWarnOnce('danger spawn', err)
  }
})

// ---- Danger: tell players when they cross into a new zone ----

let faLastDanger = {}
// Minecraft color names -> KubeJS Text helpers, for the action bar
let FA_TEXT_COLORS = { green: 'green', yellow: 'yellow', gold: 'gold', red: 'red', dark_red: 'darkRed', dark_purple: 'darkPurple', dark_gray: 'gray' }

function faDangerNotice(player) {
  let uuid = player.getStringUUID()
  let danger = global.FA_dangerLevel(player.level(), player.getX(), player.getZ())
  let last = faLastDanger[uuid]
  faLastDanger[uuid] = danger
  if (last === undefined || last == danger) return

  let zone = global.FA_zone(danger)
  let m = global.FA_dangerMult(danger, false)
  let server = player.getServer()
  let name = player.getScoreboardName()

  if (zone !== global.FA_zone(last)) {
    let deeper = danger > last
    server.runCommandSilent('title ' + name + ' times 10 60 20')
    server.runCommandSilent('title ' + name + ' subtitle {"text":"Danger ' + danger +
      (danger > 0 ? '  \u00b7  mobs x' + m.health.toFixed(1) + ' tougher, x' + m.loot.toFixed(1) + ' loot' : '  \u00b7  mobs are normal here') +
      '","color":"gray"}')
    server.runCommandSilent('title ' + name + ' title {"text":"' + zone.name + '","color":"' + zone.color + '"}')
    server.runCommandSilent('execute at ' + name + ' run playsound ' +
      (deeper ? 'minecraft:ambient.cave' : 'minecraft:block.beacon.activate') + ' player ' + name + ' ~ ~ ~ 1 ' + (deeper ? 0.6 : 1.2))
  } else {
    let color = FA_TEXT_COLORS[zone.color] || 'gray'
    player.displayClientMessage(Text[color]('\u26a0 Danger ' + danger + '  \u00b7  mobs: x' + m.health.toFixed(1) + ' health, x' +
      m.damage.toFixed(1) + ' damage, x' + m.loot.toFixed(1) + ' loot'), true)
  }
}

// ---- Every second: danger zones, Renewal, passive effects, Magnetism, Restoration ----

ForgeEvents.onEvent('net.minecraftforge.event.TickEvent$PlayerTickEvent', event => {
  try {
    let player = event.player
    if (String(event.phase) != 'END' || player.level().isClientSide() || player.tickCount % 20 != 0 || !player.isAlive()) return
    try {
      faDangerNotice(player)
    } catch (err) {
      faWarnOnce('danger zones', err)
    }
    let levels = faLevels(player)
    if (Object.keys(levels).length == 0) return

    let regen = 0
    faIds('regen').forEach(id => { regen += faE(id).perLevel * (levels[id] || 0) })
    if (regen > 0 && player.getHealth() < player.getMaxHealth()) player.heal(Math.min(regen, FA_MAX_FLOAT))

    faIds('passive').forEach(id => {
      let level = levels[id] || 0
      if (level <= 0) return
      let e = faE(id)
      // Night vision flickers under 10 seconds left, so keep it topped up past that
      let ticks = e.effect == 'NIGHT_VISION' ? 400 : e.effect == 'SATURATION' ? 1 : 60
      let max = e.maxLevel ? e.maxLevel - 1 : 127
      player.addEffect(faEffect(e, ticks, Math.min(max, level - 1)))
    })

    faIds('magnet').forEach(id => {
      let level = levels[id] || 0
      if (level <= 0) return
      let e = faE(id)
      let box = player.getBoundingBox().inflate(Math.min(e.cap, e.base + e.perLevel * level))
      ;[$FAItemEntity, $FAOrb].forEach(cls => {
        let found = player.level().getEntitiesOfClass(cls, box)
        for (let i = 0; i < found.size() && i < 256; i++) {
          let ent = found.get(i)
          // Items you just threw still have a pickup delay, so they aren't sucked straight back
          if (ent instanceof $FAItemEntity && ent.hasPickUpDelay()) continue
          ent.teleportTo(player.getX(), player.getY(), player.getZ())
        }
      })
    })

    let repair = 0
    faIds('repair').forEach(id => { repair += faE(id).perLevel * (levels[id] || 0) })
    if (repair > 0) {
      let it = player.getAllSlots().iterator()
      while (it.hasNext()) {
        let stack = it.next()
        if (stack.isDamageableItem() && stack.isDamaged()) {
          stack.setDamageValue(Math.max(0, stack.getDamageValue() - Math.min(repair, 2000000000)))
        }
      }
    }
  } catch (err) {
    faWarnOnce('passives', err)
  }
})

// ---- Mining ----
// Runs on both client and server, which both see the item NBT, so mining stays in sync

ForgeEvents.onEvent('net.minecraftforge.event.entity.player.PlayerEvent$BreakSpeed', event => {
  try {
    let levels = faLevels(event.getEntity())
    let mult = 1
    faIds('mining').forEach(id => { mult *= 1 + faE(id).perLevel * (levels[id] || 0) })
    if (mult > 1) event.setNewSpeed(Math.min(event.getNewSpeed() * mult, FA_MAX_FLOAT))
  } catch (err) {
    faWarnOnce('mining', err)
  }
})
