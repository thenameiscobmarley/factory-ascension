// priority: 500
// Factory Ascension - hordes and loot drops in the danger zones.
// Uses helpers from 20_fa_enchant_effects.js and the tick scheduler from 40_fa_ultimates.js (same startup scope).
// What can drop, and how often, is all in the config (loot, horde); the shared generator is global.FA_rollLoot.

// ---- Hordes ----
// A hostile mob spawning in a danger zone brings extra copies of itself, a tick later (adding entities while one
// is joining the world isn't safe), in clear space beside it. Extras never bring their own extras or become bosses.

function faHostilesNear(mob, radius) {
  let found = mob.level.getEntitiesOfClass($FALiving, mob.getBoundingBox().inflate(radius))
  let n = 0
  for (let i = 0; i < found.size(); i++) if (found.get(i) instanceof $FAEnemy) n++
  return n
}

function faSpawnHorde(mob, count) {
  let H = global.FA.horde
  let level = mob.level
  for (let i = 0; i < count; i++) {
    // A few tries for a clear spot; give up on this one rather than spawn it inside a wall
    for (let attempt = 0; attempt < 4; attempt++) {
      let x = mob.getX() + (Math.random() * 2 - 1) * H.radius
      let z = mob.getZ() + (Math.random() * 2 - 1) * H.radius
      if (!faClear(level, x, mob.getY(), z)) continue
      let extra = level.createEntity(mob.type)
      if (!extra) return
      extra.getPersistentData().putBoolean('fa_horde', true)
      extra.setPositionAndRotation(x, mob.getY(), z, Math.random() * 360, 0)
      level.addFreshEntity(extra)
      break
    }
  }
}

ForgeEvents.onEvent('net.minecraftforge.event.entity.EntityJoinLevelEvent', event => {
  try {
    let H = global.FA.horde
    if (!H.enabled || event.loadedFromDisk() || event.getLevel().isClientSide()) return
    let mob = event.getEntity()
    if (!(mob instanceof $FAEnemy) || !(mob instanceof $FALiving)) return
    let data = mob.getPersistentData()
    if (!data.contains('fa_danger') || data.getBoolean('fa_horde') || data.getBoolean('fa_boss') || data.getBoolean('fa_minion') || faIsBoss(mob)) return
    let count = Math.min(H.maxExtra, faRoll(H.extraPerLevel * data.getInt('fa_danger')))
    if (count <= 0) return
    faLater(1, () => {
      if (!mob.isAlive() || mob.isRemoved() || faHostilesNear(mob, 48) >= H.nearbyCap) return
      faSpawnHorde(mob, count)
    })
  } catch (err) {
    faWarnOnce('hordes', err)
  }
})

// ---- Loot drops ----

const FA_LOOT_TEXT = { white: 'white', green: 'green', aqua: 'aqua', light_purple: 'lightPurple', gold: 'gold', red: 'red' }

// Writes a Relic (plain data from global.FA_makeRelic) onto a real item
function faRelicStack(relic) {
  let stack = Item.of(relic.item)
  let tag = stack.getOrCreateTag()
  let data = tag.getCompound(FA_TAG)
  Object.keys(relic.enchants).forEach(id => data.putInt(id, relic.enchants[id]))
  if (relic.gems.length) {
    let gems = data.getCompound('gems')
    relic.gems.forEach((g, i) => {
      let one = gems.getCompound(String(i))
      one.putString('type', g[0])
      one.putInt('tier', g[1])
      gems.put(String(i), one)
    })
    data.put('gems', gems)
  }
  if (relic.rolled.length) {
    let rolled = data.getCompound('rolled')
    relic.rolled.forEach((r, i) => {
      let one = rolled.getCompound(String(i))
      one.putInt('seed', r[0])
      one.putInt('level', r[1])
      rolled.put(String(i), one)
    })
    data.put('rolled', rolled)
  }
  if (relic.stars) data.putInt('stars', relic.stars)
  data.putInt('relic', relic.quality == 'mythic' ? 2 : 1)
  tag.put(FA_TAG, data)
  if (relic.enchants.eternal) tag.putBoolean('Unbreakable', true)
  let display = tag.getCompound('display')
  display.putString('Name', JSON.stringify({ text: relic.name, color: relic.color, italic: false }))
  tag.put('display', display)
  return stack
}

// For server scripts (Sealed Gear Caches)
global.FA_relicStack = relic => faRelicStack(relic)

// Big drops announce themselves: a glowing item, a beam of light, a sound, and for the best ones a title
function faLootFx(killer, victim, tier, label) {
  let T = global.FA.loot.tiers[tier]
  let server = killer.getServer()
  if (!server || tier < 3) return
  let x = victim.getX().toFixed(1)
  let y = victim.getY()
  let z = victim.getZ().toFixed(1)
  let dim = 'execute in ' + victim.level.dimension + ' run '
  for (let h = 0; h < 6; h++) server.runCommandSilent(dim + 'particle minecraft:end_rod ' + x + ' ' + (y + h * 1.5).toFixed(1) + ' ' + z + ' 0.05 0.6 0.05 0 12 force')
  server.runCommandSilent(dim + 'playsound ' + (tier >= 4 ? 'minecraft:ui.toast.challenge_complete' : 'minecraft:entity.player.levelup') + ' player @a ' + x + ' ' + y + ' ' + z + ' 1.5 ' + (tier >= 5 ? 0.6 : 1))
  if (tier >= 4) {
    let name = killer.username
    server.runCommandSilent('title ' + name + ' times 5 50 15')
    server.runCommandSilent('title ' + name + ' subtitle ' + JSON.stringify({ text: label, color: T.color }))
    server.runCommandSilent('title ' + name + ' title ' + JSON.stringify({ text: T.name.toUpperCase() + ' DROP', color: T.color, bold: true }))
  }
  if (tier >= 5) server.tell(Text.red(killer.username + ' found a Mythic drop: ').append(Text.gold(label)))
}

ForgeEvents.onEvent('net.minecraftforge.event.entity.living.LivingDropsEvent', event => {
  try {
    let L = global.FA.loot
    if (!L.enabled) return
    let victim = event.getEntity()
    let killer = event.getSource().actual
    if (!(killer instanceof $FAPlayer) || victim instanceof $FAPlayer) return
    let danger = faDanger(victim)
    if (!danger) return

    let rolls = danger.boss ? L.bossRolls : danger.elite ? L.eliteRolls : 1
    let bump = danger.boss ? 2 : danger.elite ? 1 : 0
    let chance = danger.boss ? 1 : global.FA_lootChance(danger.level)
    let drops = event.getDrops()
    let stacks = 0
    for (let i = 0; i < rolls; i++) {
      if (Math.random() >= chance) continue
      let roll = global.FA_rollLoot(Math.random, danger.level, bump)
      let made = []
      let label
      if (roll.drop.kind == 'relic') {
        let relic = faRelicStack(roll.drop.relic)
        label = roll.drop.relic.name
        // The luckiest Mythic Relics come out as one-of-one Artifacts
        let AR = global.FA.artifacts
        if (roll.drop.relic.quality == 'mythic' && AR.enabled && Math.random() < AR.relicChance && global.FA_makeArtifact) {
          let art = global.FA_makeArtifact(relic, Math.random, killer.getServer())
          if (art) {
            label = art.name + ', ' + art.title
            let server = killer.getServer()
            if (server) server.tell(Text.lightPurple('\u2736 ' + killer.username + ' found an Artifact: ').append(Text.gold(label + ' (#' + art.serial + ')')))
          }
        }
        made.push(relic)
        faStat(killer, 'relics_found', 1)
      } else {
        let proto = Item.of(roll.drop.id)
        let left = roll.drop.count
        while (left > 0 && made.length < 8) {
          let n = Math.min(left, proto.getMaxStackSize())
          let st = Item.of(roll.drop.id, n)
          // Tomes from loot can come foiled, better foils from better tiers
          if (String(roll.drop.id).indexOf('kubejs:tome_') == 0 && global.FA.foils.enabled && Math.random() < global.FA.foils.lootChance) {
            global.FA_setFoil(st, global.FA_foilQuality(Math.random, Math.min(5, roll.tier)))
          }
          made.push(st)
          left -= n
        }
        label = roll.drop.count + 'x ' + proto.getHoverName().getString()
      }
      made.forEach(stack => {
        if (stacks++ >= 12) return   // never flood the ground
        let e = new $FAItemEntity(victim.level, victim.getX(), victim.getY() + 0.5, victim.getZ(), stack)
        e.setDefaultPickUpDelay()
        if (roll.tier >= 2) e.glowing = true
        drops.add(e)
      })
      faLootFx(killer, victim, roll.tier, label)
    }
  } catch (err) {
    faWarnOnce('loot drops', err)
  }
})
