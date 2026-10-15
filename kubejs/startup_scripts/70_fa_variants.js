// priority: 300
// Factory Ascension - rare mob variants (config: variants). The rarer, the better:
//   Blazing / Frozen / Storm (hostile mobs, 1 in 1000 each): an on-hit effect, themed drops and a loot roll
//   Shiny (1 in 1000): sparkles, a chime when it spawns near you, two better loot rolls and a gem
//   Golden (1 in 4000): glows through walls, gold sparkles, gold, cores, a gem and three top loot rolls
// Kills are announced in chat. Uses helpers from 20_fa_enchant_effects.js and 50_fa_loot.js (same startup scope).

let faVariants = {}   // mob uuid -> { mob, type }

const FA_VARIANT_TEXT = { gold: 'gold', aqua: 'aqua', red: 'red', blue: 'blue', yellow: 'yellow', light_purple: 'lightPurple', green: 'green' }

function faVariantOf(mob) {
  let data = mob && mob.getPersistentData ? mob.getPersistentData() : null
  return data && data.contains('fa_variant') ? global.FA_variantType(String(data.getString('fa_variant'))) : null
}
global.FA_variantOf = mob => {
  let t = faVariantOf(mob)
  return t ? t.id : ''
}

function faVariantName(t, mob) {
  return Text[FA_VARIANT_TEXT[t.color] || 'white']((t.symbol ? t.symbol + ' ' : '') + t.name + ' ').append(mob.getDisplayName())
}

// Players within this many blocks of a mob
function faPlayersNear(mob, radius) {
  let out = []
  let server = mob.level.getServer()
  if (!server) return out
  server.getPlayerList().getPlayers().forEach(p => {
    if (p.level !== mob.level && String(p.level.dimension) != String(mob.level.dimension)) return
    let dx = p.getX() - mob.getX(), dz = p.getZ() - mob.getZ()
    if (dx * dx + dz * dz <= radius * radius) out.push(p)
  })
  return out
}

// Turn a mob into a variant (also used by /ascension selftest)
global.FA_makeVariant = (mob, id) => {
  let t = global.FA_variantType(id)
  if (!t) return false
  let data = mob.getPersistentData()
  data.putString('fa_variant', t.id)
  let name = faVariantName(t, mob)   // built before the custom name replaces the mob's own name
  mob.setCustomName(name)
  mob.setCustomNameVisible(true)
  if (mob.setPersistenceRequired) mob.setPersistenceRequired()   // too rare to let it despawn
  if (t.glow) mob.addEffect(new $FAEffectInstance($FAEffects.GLOWING, 1000000, 0, true, false))
  faVariants[mob.stringUuid] = { mob: mob, type: t }
  if (t.announceSpawn) {
    let server = mob.level.getServer()
    faPlayersNear(mob, 64).forEach(p => {
      p.displayClientMessage(Text.of('').append(name).append(Text.gray(' appeared nearby!')), true)
      if (server) server.runCommandSilent('execute at ' + p.username + ' run playsound minecraft:block.amethyst_block.chime player ' +
        p.username + ' ~ ~ ~ 1 ' + (t.id == 'golden' ? 0.6 : 1.3))
    })
  }
  return true
}

// New spawns roll; variants loading back in from disk re-register for their sparkles
ForgeEvents.onEvent('net.minecraftforge.event.entity.EntityJoinLevelEvent', event => {
  try {
    if (!global.FA.variants.enabled || event.getLevel().isClientSide()) return
    let mob = event.getEntity()
    if (!(mob instanceof $FALiving) || mob instanceof $FAPlayer || String(mob.type) == 'minecraft:armor_stand') return
    if (event.loadedFromDisk()) {
      let t = faVariantOf(mob)
      if (t) faVariants[mob.stringUuid] = { mob: mob, type: t }
      return
    }
    let data = mob.getPersistentData()
    if (data.contains('fa_variant') || data.getBoolean('fa_skinwalker') || data.getBoolean('fa_boss') || data.getBoolean('fa_elite') || data.getBoolean('fa_minion') ||
      faIsBoss(mob) || mob.hasCustomName()) return
    let id = global.FA_rollVariant(Math.random, mob instanceof $FAEnemy)
    if (id) global.FA_makeVariant(mob, id)
  } catch (err) {
    faWarnOnce('mob variants', err)
  }
})

// Sparkles, twice a second; forget mobs that are gone
ForgeEvents.onEvent('net.minecraftforge.event.TickEvent$ServerTickEvent', event => {
  try {
    if (String(event.phase) != 'END') return
    let server = event.getServer()
    if (server.getTickCount() % 10 != 0) return
    Object.keys(faVariants).forEach(uuid => {
      let v = faVariants[uuid]
      if (!v.mob.isAlive() || v.mob.isRemoved()) {
        delete faVariants[uuid]
        return
      }
      let m = v.mob
      server.runCommandSilent('execute in ' + m.level.dimension + ' run particle ' + v.type.particle + ' ' + m.getX().toFixed(1) + ' ' +
        (m.getY() + 1).toFixed(1) + ' ' + m.getZ().toFixed(1) + ' 0.35 0.45 0.35 0.02 ' + (v.type.glow ? 6 : 3) + ' normal')
    })
  } catch (err) {
    faWarnOnce('variant sparkles', err)
  }
})

// Elementals: their hits burn, freeze or call lightning
ForgeEvents.onEvent('net.minecraftforge.event.entity.living.LivingHurtEvent', event => {
  try {
    let attacker = event.getSource().actual
    let t = faVariantOf(attacker)
    if (!t || !t.onHit) return
    let V = global.FA.variants
    let victim = event.getEntity()
    if (t.onHit == 'fire') victim.setSecondsOnFire(V.fireSeconds)
    if (t.onHit == 'frost') victim.addEffect(new $FAEffectInstance($FAEffects.MOVEMENT_SLOWDOWN, V.frostSeconds * 20, 1))
    if (t.onHit == 'storm') {
      faBolt(victim)
      event.setAmount(event.getAmount() * (1 + V.stormBonus))
    }
  } catch (err) {
    faWarnOnce('variant hits', err)
  }
})

// Loot: themed drops, gems, loot rolls; the kill is announced
function faVariantStacks(t, danger) {
  let G = global.FA.gems
  let gemTypes = Object.keys(G.types)
  let out = []
  t.drops.forEach(d => {
    if (d[0] == 'gem') out.push(Item.of(global.FA_gemId(gemTypes[Math.floor(Math.random() * gemTypes.length)], d[1])))
    else {
      let n = d[1] + Math.floor(Math.random() * (d[2] - d[1] + 1))
      if (n > 0) out.push(Item.of(d[0], n))
    }
  })
  for (let i = 0; i < t.lootRolls; i++) {
    let roll = global.FA_rollLoot(Math.random, danger, t.lootBump)
    if (roll.drop.kind == 'relic') out.push(faRelicStack(roll.drop.relic))
    else out.push(Item.of(roll.drop.id, Math.min(roll.drop.count, 64)))
  }
  return out
}

ForgeEvents.onEvent('net.minecraftforge.event.entity.living.LivingDropsEvent', event => {
  try {
    let mob = event.getEntity()
    let t = faVariantOf(mob)
    if (!t) return
    let danger = faDanger(mob)
    let drops = event.getDrops()
    faVariantStacks(t, danger ? danger.level : 0).forEach(stack => {
      let e = new $FAItemEntity(mob.level, mob.getX(), mob.getY() + 0.5, mob.getZ(), stack)
      e.setDefaultPickUpDelay()
      e.glowing = true
      drops.add(e)
    })
    let killer = event.getSource().actual
    let server = mob.level.getServer()
    if (server && killer instanceof $FAPlayer) {
      server.tell(Text.gold(killer.username).append(Text.gray(' slew a ')).append(mob.getCustomName() || faVariantName(t, mob)).append(Text.gray('!')))
      server.runCommandSilent('execute at ' + killer.username + ' run playsound minecraft:ui.toast.challenge_complete player ' + killer.username + ' ~ ~ ~ 1 1.2')
    }
    delete faVariants[mob.stringUuid]
  } catch (err) {
    faWarnOnce('variant drops', err)
  }
})

ForgeEvents.onEvent('net.minecraftforge.event.entity.living.LivingExperienceDropEvent', event => {
  try {
    let t = faVariantOf(event.getEntity())
    if (t && t.xp > 1) event.setDroppedExperience(Math.round(event.getDroppedExperience() * t.xp))
  } catch (err) {
    faWarnOnce('variant xp', err)
  }
})
