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
const $FAEntityType = Java.loadClass('net.minecraft.world.entity.EntityType')
const $FABlockTags = Java.loadClass('net.minecraft.tags.BlockTags')
const $FAForgeBlockTags = Java.loadClass('net.minecraftforge.common.Tags$Blocks')

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
// Rolled (Chaos Tome) enchants become "virtual" enchants: one per effect, with ids like r12345_0.
// They're registered when an item carrying them is read, and from then on every hook treats them
// exactly like config enchants, so they use the same tested effect code.
let FA_VIRTUAL = {}
let FA_VIRTUAL_BY_TYPE = {}
let FA_VIRTUAL_COUNT = 0
function faRegisterVirtual(id, def) {
  if (FA_VIRTUAL[id]) return
  // Bounded: if it ever gets big, start over; items re-register themselves on the next read
  if (FA_VIRTUAL_COUNT >= 3000) { FA_VIRTUAL = {}; FA_VIRTUAL_BY_TYPE = {}; FA_VIRTUAL_COUNT = 0 }
  FA_VIRTUAL[id] = def
  if (!FA_VIRTUAL_BY_TYPE[def.type]) FA_VIRTUAL_BY_TYPE[def.type] = []
  FA_VIRTUAL_BY_TYPE[def.type].push(id)
  FA_VIRTUAL_COUNT++
}
function faIds(type) {
  let base = FA_BY_TYPE[type] || []
  let rolled = FA_VIRTUAL_BY_TYPE[type]
  return rolled ? base.concat(rolled) : base
}

const FA_MAX_TOTAL = 2000000000
// Keys inside FactoryAscension that aren't enchant levels
const FA_RESERVED = { stars: true, souls: true, gems: true, rolled: true, relic: true }

// Everything one item adds: its enchant levels, gems and rolled enchants, all x its Prestige stars.
// Souls are raw counts and never multiplied.
function faItemLevels(data) {
  let own = {}
  let add = (id, value) => { if (value > 0) own[id] = Math.min(FA_MAX_TOTAL, (own[id] || 0) + value) }
  let stars = data.getInt('stars')
  let mult = global.FA_starMult(stars > 0 ? stars : 0)
  let keys = data.getAllKeys().iterator()
  while (keys.hasNext()) {
    let id = String(keys.next())
    let value = data.getInt(id)
    // Ignore broken values (only possible from hand-edited NBT) and cap totals, so nothing overflows
    if (value <= 0 || FA_RESERVED[id] && id != 'souls') continue
    let e = global.FA.enchants[id]
    if (e && e.type == 'fusion') {
      // A fusion is its two parent effects, each a virtual enchant "<id>~0" / "<id>~1"
      for (let i = 0; i < 2; i++) {
        faRegisterVirtual(id + '~' + i, global.FA_fusionPart(id, i))
        add(id + '~' + i, Math.floor(value * mult))
      }
      continue
    }
    add(id, id == 'souls' ? value : Math.floor(value * mult))
  }
  // Gems: {"0": {type, tier}, ...}
  if (data.contains('gems')) {
    let gems = data.getCompound('gems')
    let slots = gems.getAllKeys().iterator()
    while (slots.hasNext()) {
      let gem = gems.getCompound(String(slots.next()))
      let type = global.FA.gems.types[String(gem.getString('type'))]
      let tier = gem.getInt('tier')
      if (type && tier >= 1 && tier <= 3) add(type.enchant, Math.floor(global.FA_gemLevels(tier) * mult))
    }
  }
  faRolledLevels(data, own, mult)
  return own
}

// All enchant levels on a player's hands + armor, summed: {sharpness: 120, protection: 3000},
// with gear-set bonuses applied. Returns an empty object for anything that isn't a player.
function faLevels(entity) {
  let levels = {}
  if (!entity || !(entity instanceof $FAPlayer)) return levels
  let pieces = {}
  let it = entity.getAllSlots().iterator()
  while (it.hasNext()) {
    let tag = it.next().getTag()
    if (!tag || !tag.contains(FA_TAG)) continue
    let own = faItemLevels(tag.getCompound(FA_TAG))
    Object.keys(own).forEach(id => {
      levels[id] = Math.min(FA_MAX_TOTAL, (levels[id] || 0) + own[id])
      pieces[id] = (pieces[id] || 0) + 1
    })
  }
  faApplySets(levels, pieces)
  return levels
}

// Gear sets: 2 or 4 equipped pieces with the set's enchant boost it and can add other enchants
function faApplySets(levels, pieces) {
  global.FA.sets.forEach(set => {
    let n = pieces[set.key] || 0
    let bonus = n >= 4 ? set[4] : n >= 2 ? set[2] : null
    if (!bonus) return
    levels[set.key] = Math.min(FA_MAX_TOTAL, Math.floor(levels[set.key] * bonus.mult))
    Object.keys(bonus.add || {}).forEach(id => { levels[id] = Math.min(FA_MAX_TOTAL, (levels[id] || 0) + bonus.add[id]) })
  })
}

// Rolled enchants live under FactoryAscension.rolled: {"0": {seed, level}, "1": ...}
function faRolledLevels(data, levels, mult) {
  mult = mult || 1
  if (!data.contains('rolled')) return
  let rolled = data.getCompound('rolled')
  let slots = rolled.getAllKeys().iterator()
  while (slots.hasNext()) {
    let slot = rolled.getCompound(String(slots.next()))
    let level = slot.getInt('level')
    if (level <= 0) continue
    let roll = global.FA_rollEnchant(slot.getInt('seed'))
    roll.affixes.forEach((def, i) => {
      let id = 'r' + roll.seed + '_' + i
      faRegisterVirtual(id, def)
      levels[id] = Math.min(FA_MAX_TOTAL, (levels[id] || 0) + Math.floor(level * mult))
    })
  }
}

function faE(id) {
  return global.FA.enchants[id] || FA_VIRTUAL[id]
}

// Danger data saved on a scaled mob: {level, elite}, or null for normal mobs and players
function faDanger(entity) {
  if (!entity || !(entity instanceof $FALiving) || entity instanceof $FAPlayer) return null
  let data = entity.getPersistentData()
  if (!data.contains('fa_danger')) return null
  let level = data.getInt('fa_danger')
  let elite = data.getBoolean('fa_elite')
  let mult = global.FA_dangerMult(level, elite)
  mult.level = level
  mult.elite = elite
  mult.boss = data.getBoolean('fa_boss')
  if (mult.boss) {
    mult.health *= global.FA.bosses.healthMult
    mult.damage *= global.FA.bosses.damageMult
  }
  return mult
}

// The current world event ({type, until}) or null; set by startup_scripts/30_fa_world.js
function faEvent(type) {
  let e = global.FA_EVENT
  return e && e.type == type ? e : null
}

// Stats for milestones live in the server script; startup hooks report through this
function faStat(player, stat, amount, mode) {
  try {
    if (global.FA_onStat && player instanceof $FAPlayer) global.FA_onStat(player, stat, amount, mode || 'add')
  } catch (err) {
    faWarnOnce('stats', err)
  }
}

// Living hostile mobs around an entity, up to max. Never players, pets or villagers.
function faEnemiesNear(center, radius, exclude, max) {
  let found = center.level().getEntitiesOfClass($FALiving, center.getBoundingBox().inflate(radius))
  let out = []
  for (let i = 0; i < found.size() && out.length < max; i++) {
    let e = found.get(i)
    if (e === center || (exclude && exclude.indexOf(e) >= 0) || !(e instanceof $FAEnemy) || !e.isAlive()) continue
    out.push(e)
  }
  return out
}

// Particles + sound at an entity, for everyone nearby to see
function faFx(entity, particle, count, sound, pitch) {
  let server = entity.getServer()
  if (!server) return
  let pos = entity.getX().toFixed(2) + ' ' + (entity.getY() + 1).toFixed(2) + ' ' + entity.getZ().toFixed(2)
  let dim = 'execute in ' + entity.level().dimension().location() + ' run '
  server.runCommandSilent(dim + 'particle ' + particle + ' ' + pos + ' 0.6 0.6 0.6 0.2 ' + count + ' normal')
  if (sound) server.runCommandSilent(dim + 'playsound ' + sound + ' hostile @a ' + pos + ' 1 ' + (pitch || 1))
}

// Lightning that only looks and sounds like lightning: no fire, no damage, no charged creepers
function faBolt(target) {
  let bolt = $FAEntityType.LIGHTNING_BOLT.create(target.level())
  if (!bolt) return
  bolt.moveTo(target.getX(), target.getY(), target.getZ())
  bolt.setVisualOnly(true)
  target.level().addFreshEntity(bolt)
}

// Rampage kill-streak stacks, per player: {stacks, until (game time)}
let faRampage = {}
function faRampageStacks(player) {
  let r = faRampage[player.getStringUUID()]
  return r && player.level().getGameTime() <= r.until ? r.stacks : 0
}

// Heads for Headhunter, by mob id
let FA_HEADS = {
  'minecraft:zombie': 'minecraft:zombie_head', 'minecraft:husk': 'minecraft:zombie_head', 'minecraft:drowned': 'minecraft:zombie_head',
  'minecraft:skeleton': 'minecraft:skeleton_skull', 'minecraft:stray': 'minecraft:skeleton_skull',
  'minecraft:creeper': 'minecraft:creeper_head', 'minecraft:wither_skeleton': 'minecraft:wither_skeleton_skull',
  'minecraft:piglin': 'minecraft:piglin_head', 'minecraft:piglin_brute': 'minecraft:piglin_head'
}

// ---- Conditions ----

function faIsBoss(entity) {
  // Vanilla and most modded bosses can't change dimensions; huge health pools count too; so do Ascended bosses
  if (!entity.canChangeDimensions() || entity.getMaxHealth() >= 200) return true
  let data = entity.getPersistentData ? entity.getPersistentData() : null
  return !!(data && data.getBoolean('fa_boss'))
}

// Bosses shrug off crowd control (freezing, levitation, being pulled), or fights would be trivial
const FA_CC = { MOVEMENT_SLOWDOWN: true, LEVITATION: true, WEAKNESS: true }

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
let faPhoenixReady = {}
let faChronosReady = {}

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
    // Soul Harvest: souls are stored on the item and summed with the rest of the levels
    faIds('souls').forEach(id => {
      let level = A[id] || 0
      if (level > 0 && A.souls > 0) scaled *= 1 + faE(id).perLevel * level * A.souls
    })
    faIds('rampage').forEach(id => {
      let level = A[id] || 0
      let stacks = level > 0 ? faRampageStacks(attacker) : 0
      if (stacks > 0) scaled *= 1 + faE(id).perLevel * level * stacks
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

      // Phoenix: if it's still a killing blow, revive at full health in a firestorm
      faIds('phoenix').forEach(id => {
        let level = V[id] || 0
        if (level <= 0 || scaled < victim.getHealth()) return
        let uuid = victim.getStringUUID()
        let now = victim.level().getGameTime()
        if (faPhoenixReady[uuid] && now < faPhoenixReady[uuid]) return
        faPhoenixReady[uuid] = now + Math.round(global.FA_phoenixCooldown(faE(id), level) * 20)
        scaled = 0
        victim.setHealth(victim.getMaxHealth())
        victim.clearFire()
        faEnemiesNear(victim, 6, null, 32).forEach(m => {
          m.setSecondsOnFire(10)
          faSecondaryHit(m, victim.damageSources().playerAttack(victim), 10 * level)
        })
        faFx(victim, 'minecraft:flame', 200, 'minecraft:item.totem.use', 1)
        victim.displayClientMessage(Text.gold('Rise from the ashes!'), true)
      })
    }

    scaled = Math.min(scaled, FA_MAX_FLOAT)
    if (scaled != amount) event.setAmount(scaled)

    // ---- Chronos: getting hit freezes every enemy around you ----
    if (!bypass && attacker instanceof $FALiving && attacker !== victim) {
      faIds('time_stop').forEach(id => {
        let level = V[id] || 0
        if (level <= 0) return
        let uuid = victim.getStringUUID()
        let now = victim.level().getGameTime()
        if (faChronosReady[uuid] && now < faChronosReady[uuid]) return
        let c = global.FA_chronos(level)
        faChronosReady[uuid] = now + c.cooldown * 20
        let ticks = Math.round(c.seconds * 20)
        faEnemiesNear(victim, c.radius, null, 64).filter(m => !faIsBoss(m)).forEach(m => {
          m.addEffect(new $FAEffectInstance($FAEffects.MOVEMENT_SLOWDOWN, ticks, 127, false, false))
          m.addEffect(new $FAEffectInstance($FAEffects.WEAKNESS, ticks, 127, false, false))
        })
        faFx(victim, 'minecraft:reverse_portal', 150, 'minecraft:block.beacon.deactivate', 0.5)
        victim.displayClientMessage(Text.aqua('Time stops.'), true)
      })
    }

    // ---- Attacker's on-hit effects ----
    if (attacker instanceof $FAPlayer && attacker !== victim && attacker.isAlive()) {
      let heal = 0
      faIds('lifesteal').forEach(id => { heal += scaled * faE(id).perLevel * (A[id] || 0) })
      faIds('hit_heal').forEach(id => { heal += faE(id).perLevel * (A[id] || 0) })
      if (heal > 0) attacker.heal(Math.min(heal, FA_MAX_FLOAT))

      faIds('hit_effect').forEach(id => {
        let level = A[id] || 0
        if (level > 0 && !(FA_CC[faE(id).effect] && faIsBoss(victim))) victim.addEffect(faEffect(faE(id), faE(id).seconds * 20, global.FA_amp(faE(id), level)))
      })
      faIds('self_effect').forEach(id => {
        let level = A[id] || 0
        if (level > 0) attacker.addEffect(faEffect(faE(id), faE(id).seconds * 20, global.FA_amp(faE(id), level)))
      })
      faIds('ignite').forEach(id => {
        let level = A[id] || 0
        if (level > 0) victim.setSecondsOnFire(Math.min(1000000, Math.ceil(faE(id).perLevel * level)))
      })

      let playerHit = attacker.damageSources().playerAttack(attacker)
      faIds('cleave').forEach(id => {
        let level = A[id] || 0
        if (level <= 0) return
        faEnemiesNear(victim, faE(id).radius, [attacker], global.FA.maxCleaveTargets)
          .forEach(m => faSecondaryHit(m, playerHit, scaled * faE(id).perLevel * level))
      })

      // ---- Mythic on-hit ----
      faIds('chain_lightning').forEach(id => {
        let level = A[id] || 0
        if (level <= 0 || Math.random() >= global.FA_chainChance(level)) return
        let chained = faEnemiesNear(victim, faE(id).radius, [attacker], global.FA_chainTargets(level))
        let damage = scaled * (0.5 + faE(id).perLevel * level)
        faBolt(victim)
        chained.forEach((m, i) => {
          if (i < 3) faBolt(m)
          faSecondaryHit(m, playerHit, damage)
        })
      })
      faIds('gravity_well').forEach(id => {
        let level = A[id] || 0
        if (level <= 0) return
        let e = faE(id)
        let pull = Math.min(2, 0.4 + 0.05 * level)
        faEnemiesNear(victim, Math.min(e.cap, e.base + e.perLevel * level), [attacker], 64).filter(m => !faIsBoss(m)).forEach(m => {
          let dx = victim.getX() - m.getX()
          let dy = victim.getY() - m.getY()
          let dz = victim.getZ() - m.getZ()
          let len = Math.sqrt(dx * dx + dy * dy + dz * dz) || 1
          m.setDeltaMovement(dx / len * pull, Math.max(0.1, dy / len * pull), dz / len * pull)
          m.hurtMarked = true
        })
      })
      faIds('overkill').forEach(id => {
        let level = A[id] || 0
        let excess = scaled - victim.getHealth()
        if (level <= 0 || excess <= 0) return
        let next = faEnemiesNear(victim, faE(id).radius, [attacker], 1)
        if (next.length) faSecondaryHit(next[0], playerHit, excess * Math.min(1, 0.5 + faE(id).perLevel * level))
      })
      faIds('projectile_teleport').forEach(id => {
        if ((A[id] || 0) <= 0 || !source.is($FADamageTypeTags.IS_PROJECTILE)) return
        attacker.teleportTo(victim.getX(), victim.getY(), victim.getZ())
        attacker.fallDistance = 0
        faFx(attacker, 'minecraft:portal', 60, 'minecraft:entity.enderman.teleport', 1)
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
    let victim = event.getEntity()
    let heal = 0
    faIds('kill_heal').forEach(id => { heal += faE(id).perLevel * (levels[id] || 0) })
    if (heal > 0) killer.heal(Math.min(heal, FA_MAX_FLOAT))

    // Rampage: each kill within the window adds a stack
    faIds('rampage').forEach(id => {
      if ((levels[id] || 0) <= 0) return
      let stacks = Math.min(faE(id).maxStacks, faRampageStacks(killer) + 1)
      faRampage[killer.getStringUUID()] = { stacks: stacks, until: killer.level().getGameTime() + faE(id).seconds * 20 }
      killer.displayClientMessage(Text.red('Rampage x' + stacks), true)
    })

    // Soul Harvest: every equipped item with it stores one more soul
    faIds('souls').forEach(id => {
      let it = killer.getAllSlots().iterator()
      while (it.hasNext()) {
        let tag = it.next().getTag()
        if (!tag || !tag.contains(FA_TAG)) continue
        let data = tag.getCompound(FA_TAG)
        if (data.getInt(id) <= 0) continue
        data.putInt('souls', Math.min(2000000000, data.getInt('souls') + 1))
        tag.put(FA_TAG, data)
      }
    })

    // Meteor: the victim explodes into everything around it (but explosions don't chain)
    if (!faSecondary) {
      faIds('death_nova').forEach(id => {
        let level = levels[id] || 0
        if (level <= 0) return
        let damage = victim.getMaxHealth() * (0.5 + faE(id).perLevel * level)
        faFx(victim, 'minecraft:explosion_emitter', 1, 'minecraft:entity.generic.explode', 0.8)
        faEnemiesNear(victim, Math.min(12, 3 + level / 10), [killer], 32)
          .forEach(m => faSecondaryHit(m, killer.damageSources().playerAttack(killer), damage))
      })
    }
  } catch (err) {
    faWarnOnce('kills', err)
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
    if (Object.keys(levels).length == 0 && !danger && !faEvent('goldenHour')) return

    let budget = global.FA.maxExtraDropStacks
    let drops = event.getDrops()
    let extras = []
    let spawnAt = event.getEntity()
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

    // Mythic Fragments: only from danger-zone mobs killed by a player, and never multiplied
    let killer = event.getSource().getEntity()
    let byPlayer = killer instanceof $FAPlayer
    if (danger && byPlayer) {
      let chance = global.FA_fragmentChance(danger.level, danger.elite)
      if (faEvent('bloodMoon')) chance = Math.min(1, chance * global.FA.events.bloodMoon.fragmentMult)
      if (Math.random() < chance) addStacks(Item.of('kubejs:mythic_fragment'), 1)
      if (danger.elite) faStat(killer, 'elites_killed', 1)
    }
    // Ascended boss loot (never multiplied), and tell everyone
    if (danger && danger.boss && byPlayer) {
      global.FA.bosses.drops.forEach(d => addStacks(Item.of(d[0]), d[1] + Math.floor(Math.random() * (d[2] - d[1] + 1))))
      faStat(killer, 'bosses_killed', 1)
      let server = killer.getServer()
      if (server) server.tell(Text.gold(killer.getScoreboardName()).append(Text.yellow(' slew ').append(event.getEntity().getDisplayName())).append(Text.yellow('!')))
    }
    faIds('heads').forEach(id => {
      let level = levels[id] || 0
      let head = FA_HEADS[String($FAEntityType.getKey(event.getEntity().getType()))]
      if (level > 0 && head && Math.random() < Math.min(1, faE(id).perLevel * level)) addStacks(Item.of(head), 1)
    })

    // Plunder and danger: multiply what dropped
    let mult = 1
    faIds('plunder').forEach(id => { mult += faE(id).perLevel * (levels[id] || 0) })
    if (danger) mult *= danger.boss ? 1 : danger.loot
    if (byPlayer && faEvent('goldenHour')) mult *= global.FA.events.goldenHour.lootMult
    let it = drops.iterator()
    while (it.hasNext()) {
      let itemEntity = it.next()
      let stack = itemEntity.getItem()
      // Only plain stackable drops are multiplied. Gear and anything with NBT (named, enchanted, or an
      // item the mob picked up) is never copied, so loot bonuses can't duplicate a player's items.
      if (mult <= 1 || stack.isEmpty() || stack.getMaxStackSize() <= 1 || stack.hasTag()) continue
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
    // Blood Moon: everything spawns more dangerous
    if (danger > 0 && faEvent('bloodMoon')) danger += global.FA.events.bloodMoon.dangerBonus
    if (danger <= 0) return
    data.putInt('fa_danger', danger)

    // Ascended boss: rare, only deep out, never from a boss's own minions
    let B = global.FA.bosses
    if (B.enabled && danger >= B.minDanger && !data.getBoolean('fa_minion') && Math.random() < B.chancePerSpawn) {
      if (global.FA_makeBoss) global.FA_makeBoss(mob, danger)
      return
    }

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
  if (danger > 0) faStat(player, 'max_danger', danger, 'max')
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

// Wings: creative-style flight while equipped. The grant is remembered on the player,
// so flight is taken away again when the gear comes off, even after a restart.
function faFlight(player, levels) {
  let level = 0
  faIds('flight').forEach(id => { level = Math.max(level, Math.min(faE(id).maxLevel, levels[id] || 0)) })
  let data = player.getPersistentData()
  let abilities = player.getAbilities()
  if (level > 0) {
    let speed = 0.05 * (1 + 0.5 * (level - 1))
    if (!abilities.mayfly || Math.abs(abilities.getFlyingSpeed() - speed) > 0.0001) {
      abilities.mayfly = true
      abilities.setFlyingSpeed(speed)
      player.onUpdateAbilities()
    }
    data.putBoolean('fa_wings', true)
  } else if (data.getBoolean('fa_wings')) {
    data.remove('fa_wings')
    if (!player.isCreative() && !player.isSpectator()) {
      abilities.mayfly = false
      abilities.flying = false
    }
    abilities.setFlyingSpeed(0.05)
    player.onUpdateAbilities()
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
    try {
      faFlight(player, levels)
    } catch (err) {
      faWarnOnce('wings', err)
    }
    if (Object.keys(levels).length == 0) return

    // Radiance: hurts every enemy around you each second
    faIds('aura').forEach(id => {
      let level = levels[id] || 0
      if (level <= 0) return
      let hit = player.damageSources().playerAttack(player)
      faEnemiesNear(player, global.FA_auraRadius(level), null, 32).forEach(m => faSecondaryHit(m, hit, faE(id).perLevel * level))
    })

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

// ---- Gravity Slam: landing hurts everything around you instead of you ----

ForgeEvents.onEvent('net.minecraftforge.event.entity.living.LivingFallEvent', event => {
  try {
    let player = event.getEntity()
    if (!(player instanceof $FAPlayer) || player.level().isClientSide()) return
    let levels = faLevels(player)
    faIds('slam').forEach(id => {
      let level = levels[id] || 0
      if (level <= 0) return
      let dist = event.getDistance()
      event.setDamageMultiplier(0)
      if (dist < 3) return
      faFx(player, 'minecraft:explosion', 20, 'minecraft:entity.generic.explode', 1.4)
      let hit = player.damageSources().playerAttack(player)
      faEnemiesNear(player, Math.min(16, 3 + dist / 2), null, 32).forEach(m => faSecondaryHit(m, hit, dist * faE(id).perLevel * level))
    })
  } catch (err) {
    faWarnOnce('gravity slam', err)
  }
})

// ---- Vein Miner / Timber: break a whole vein or tree at once (sneak to break just one) ----

let faVeinBusy = false
ForgeEvents.onEvent('net.minecraftforge.event.level.BlockEvent$BreakEvent', event => {
  if (faVeinBusy) return
  try {
    let player = event.getPlayer()
    if (!(player instanceof $FAPlayer) || player.isShiftKeyDown()) return
    let state = event.getState()
    let levels = faLevels(player)
    faIds('vein').forEach(id => {
      let level = levels[id] || 0
      if (level <= 0) return
      let e = faE(id)
      let ores = e.blocks == 'ores'
      if (!state.is(ores ? $FAForgeBlockTags.ORES : $FABlockTags.LOGS)) return
      // Ores: only the same block (so deepslate and stone ores stay separate). Logs: any log.
      let matches = other => ores ? other.getBlock() === state.getBlock() : other.is($FABlockTags.LOGS)

      let world = event.getLevel()
      let start = event.getPos()
      let limit = Math.min(e.cap, e.perLevel * level)
      let key = p => p.getX() + ',' + p.getY() + ',' + p.getZ()
      let seen = {}
      seen[key(start)] = true
      let queue = [start]
      let broken = 0
      faVeinBusy = true
      try {
        while (queue.length && broken < limit) {
          let pos = queue.shift()
          for (let dx = -1; dx <= 1 && broken < limit; dx++) {
            for (let dy = -1; dy <= 1 && broken < limit; dy++) {
              for (let dz = -1; dz <= 1 && broken < limit; dz++) {
                let next = pos.offset(dx, dy, dz)
                if (seen[key(next)]) continue
                seen[key(next)] = true
                if (!matches(world.getBlockState(next))) continue
                // Stop if the tool broke
                if (player.getMainHandItem().isEmpty()) return
                // Breaks like the player did: uses the tool, Fortune, durability and drops
                player.gameMode.destroyBlock(next)
                broken++
                queue.push(next)
              }
            }
          }
        }
      } finally {
        faVeinBusy = false
      }
    })
  } catch (err) {
    faWarnOnce('vein miner', err)
  }
})
