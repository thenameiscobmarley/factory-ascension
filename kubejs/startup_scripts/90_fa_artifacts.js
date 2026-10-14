// priority: 100
// Factory Ascension - Artifacts: one-of-one gear (config: artifacts).
// An Artifact keeps its normal enchants and adds a generated name, a world serial number, a signature color and
// aura, a lore line, and rolled "quirks": mechanics that aren't enchants (Echo Strike, Evolving, Soulbound...).
// Stored on the item as FactoryAscension.artifact. Uses helpers from 20_fa_enchant_effects.js (same startup scope).

const $FAArmorItem = Java.loadClass('net.minecraft.world.item.ArmorItem')
const $FADiggerItem = Java.loadClass('net.minecraft.world.item.DiggerItem')
const $FAAxeItem = Java.loadClass('net.minecraft.world.item.AxeItem')
const $FACompoundTag = Java.loadClass('net.minecraft.nbt.CompoundTag')
const $FAItemStack = Java.loadClass('net.minecraft.world.item.ItemStack')

// weapon, armor or tool (axes count as weapons)
function faGearKind(stack) {
  let item = stack.getItem()
  if (item instanceof $FAArmorItem) return 'armor'
  if (item instanceof $FADiggerItem && !(item instanceof $FAAxeItem)) return 'tool'
  return 'weapon'
}

function faArtifactData(stack) {
  if (faEmpty(stack)) return null
  let tag = stack.nbt
  if (!tag || !tag.contains(FA_TAG)) return null
  let data = tag.getCompound(FA_TAG)
  return data.contains('artifact') ? data.getCompound('artifact') : null
}

// Turns gear into a brand-new Artifact. server: for the world serial number. Returns the rolled data, or false.
global.FA_makeArtifact = (stack, rnd, server) => {
  if (!global.FA.artifacts.enabled || !faWearable(stack) || faArtifactData(stack)) return false
  let art = global.FA_rollArtifact(rnd || Math.random, faGearKind(stack))
  let world = server.persistentData
  let serial = world.getInt('fa_artifact_serial') + 1
  world.putInt('fa_artifact_serial', serial)
  let tag = stack.getOrCreateTag()
  let data = tag.getCompound(FA_TAG)
  let a = new $FACompoundTag()
  a.putString('name', art.name)
  a.putString('title', art.title)
  a.putString('lore', art.lore)
  a.putString('color', art.color)
  a.putString('aura', art.aura)
  a.putInt('seed', art.seed)
  a.putInt('serial', serial)
  let quirks = new $FACompoundTag()
  art.quirks.forEach((q, i) => {
    let one = new $FACompoundTag()
    one.putString('id', q.id)
    one.putDouble('power', q.power)
    quirks.put(String(i), one)
  })
  a.put('quirks', quirks)
  data.put('artifact', a)
  tag.put(FA_TAG, data)
  let display = tag.getCompound('display')
  display.putString('Name', JSON.stringify({ text: art.name + ', ' + art.title, color: art.color, italic: false, bold: true }))
  tag.put('display', display)
  art.serial = serial
  return art
}
global.FA_isArtifact = stack => !!faArtifactData(stack)

// Quirks in effect for a player: weapon/tool quirks from the main hand, armor quirks from worn armor, 'any' from either.
// { q: { id: power }, main: the main-hand Artifact's data or null }
function faQuirks(player) {
  let out = { q: {}, main: null }
  if (!global.FA.artifacts.enabled || !(player instanceof $FAPlayer)) return out
  let defs = {}
  global.FA.artifacts.list.forEach(d => { defs[d.id] = d })
  let it = player.getAllSlots().iterator()
  let i = 0
  while (it.hasNext()) {
    let slot = it.next()
    let index = i++
    if (index == 1) continue   // the off hand
    let a = faArtifactData(slot)
    if (!a) continue
    if (index == 0) out.main = a
    let quirks = a.getCompound('quirks')
    quirks.getAllKeys().forEach(k => {
      let q = quirks.getCompound(k)
      let id = String(q.getString('id'))
      let d = defs[id]
      if (!d) return
      let ok = d.for == 'any' || (d.for == 'armor' ? index >= 2 : index == 0)
      if (!ok) return
      let p = q.getDouble('power')
      out.q[id] = d.lowerIsBetter ? Math.min(out.q[id] || 1e9, p) : (out.q[id] || 0) + p
    })
  }
  return out
}
global.FA_quirksOf = player => faQuirks(player).q

function faIsNight(level) {
  let t = level.getDayTime() % 24000
  return t > 13000 && t < 23000
}

let faSecondLifeReady = {}

ForgeEvents.onEvent('net.minecraftforge.event.entity.living.LivingHurtEvent', event => {
  if (faSecondary) return
  try {
    let source = event.getSource()
    let attacker = source.actual
    let victim = event.getEntity()
    let amount = event.getAmount()

    // ---- Attacker's Artifact ----
    let A = faQuirks(attacker)
    if (Object.keys(A.q).length) {
      let q = A.q
      if (q.nightstalker && faIsNight(attacker.level)) amount *= 1 + q.nightstalker / 100
      if (q.evolving && A.main) amount *= 1 + q.evolving / 100 * Math.floor(A.main.getInt('kills') / 100)
      let hits = 0
      if (A.main) {
        hits = A.main.getInt('hits') + 1
        A.main.putInt('hits', hits % 1000000)
      }
      event.setAmount(Math.min(amount, FA_MAX_FLOAT))
      let hit = attacker.damageSources().playerAttack(attacker)
      if (q.echo && Math.random() < q.echo / 100) faSecondaryHit(victim, hit, amount)
      if (q.vortex && hits % 10 == 0) {
        faEnemiesNear(victim, q.vortex, [attacker], 16).forEach(m => {
          let dx = victim.getX() - m.getX(), dz = victim.getZ() - m.getZ()
          m.setMotion(dx * 0.25, 0.2, dz * 0.25)
        })
        faFx(victim, 'minecraft:portal', 60, 'minecraft:entity.enderman.teleport', 0.6)
      }
      if (q.overcharge && hits % 8 == 0) {
        faEnemiesNear(victim, 4, [attacker], 16).forEach(m => faSecondaryHit(m, hit, amount * q.overcharge / 100))
        faFx(victim, 'minecraft:electric_spark', 80, 'minecraft:entity.lightning_bolt.thunder', 1.6)
      }
    }

    // ---- Victim's Artifact ----
    let V = faQuirks(victim)
    if (V.q.featherfall && source.is($FADamageTypeTags.IS_FALL)) {
      event.setAmount(0)
      return
    }
    if (V.q.second_life && event.getAmount() >= victim.getHealth()) {
      let uuid = victim.stringUuid
      let now = faNow(victim)
      if (!faSecondLifeReady[uuid] || now >= faSecondLifeReady[uuid]) {
        faSecondLifeReady[uuid] = now + Math.round(V.q.second_life * 20)
        event.setAmount(0)
        victim.setHealth(victim.getMaxHealth() / 2)
        faFx(victim, 'minecraft:totem_of_undying', 60, 'minecraft:item.totem.use', 1.3)
        victim.displayClientMessage(Text.gold('Second Life!'), true)
      }
    }
  } catch (err) {
    faWarnOnce('artifact hits', err)
  }
})

// Kills: Evolving counts them; Gilded pays out. Soulbound gear stays with a player who dies.
ForgeEvents.onEvent('net.minecraftforge.event.entity.living.LivingDropsEvent', event => {
  try {
    let victim = event.getEntity()
    let drops = event.getDrops()
    if (victim instanceof $FAPlayer) {
      faKeepSoulbound(victim, drops)
      return
    }
    let killer = event.getSource().actual
    let K = faQuirks(killer)
    if (K.main) K.main.putInt('kills', Math.min(2000000000, K.main.getInt('kills') + 1))
    if (K.q.gilded) {
      let add = stack => {
        let e = new $FAItemEntity(victim.level, victim.getX(), victim.getY() + 0.5, victim.getZ(), stack)
        e.setDefaultPickUpDelay()
        drops.add(e)
      }
      add(Item.of('minecraft:gold_nugget', 1 + Math.floor(Math.random() * 4)))
      if (Math.random() < K.q.gilded / 100) add(Item.of('kubejs:ascension_core'))
    }
  } catch (err) {
    faWarnOnce('artifact kills', err)
  }
})

// Soulbound: pulled out of the death drops and kept in the player's persisted data until they respawn
function faKeepSoulbound(player, drops) {
  let kept = []
  for (let i = drops.size() - 1; i >= 0; i--) {
    let stack = drops.get(i).getItem()
    let a = faArtifactData(stack)
    if (!a) continue
    let quirks = a.getCompound('quirks')
    let soulbound = false
    quirks.getAllKeys().forEach(k => { if (String(quirks.getCompound(k).getString('id')) == 'soulbound') soulbound = true })
    if (!soulbound) continue
    kept.push(stack.save(new $FACompoundTag()))
    drops.remove(i)
  }
  if (!kept.length) return
  let pdata = player.getPersistentData()
  let persisted = pdata.getCompound('PlayerPersisted')
  let box = persisted.getCompound('fa_soulbound')
  let n = box.getAllKeys().size()
  kept.forEach(t => box.put(String(n++), t))
  persisted.put('fa_soulbound', box)
  pdata.put('PlayerPersisted', persisted)
}
global.FA_keepSoulbound = (player, drops) => faKeepSoulbound(player, drops)

function faReturnSoulbound(player) {
  let pdata = player.getPersistentData()
  let persisted = pdata.getCompound('PlayerPersisted')
  if (!persisted.contains('fa_soulbound')) return 0
  let box = persisted.getCompound('fa_soulbound')
  let n = 0
  box.getAllKeys().forEach(k => {
    player.give($FAItemStack.of(box.getCompound(k)))
    n++
  })
  persisted.remove('fa_soulbound')
  pdata.put('PlayerPersisted', persisted)
  if (n) player.displayClientMessage(Text.lightPurple('Your Soulbound Artifact' + (n > 1 ? 's return' : ' returns') + ' to you.'), true)
  return n
}
global.FA_returnSoulbound = player => faReturnSoulbound(player)

ForgeEvents.onEvent('net.minecraftforge.event.entity.player.PlayerEvent$PlayerRespawnEvent', event => {
  try {
    faReturnSoulbound(event.getEntity())
  } catch (err) {
    faWarnOnce('soulbound', err)
  }
})

// Every second: Sunblessed healing, Colossus and Quickdraw attributes, the aura of a held Artifact
ForgeEvents.onEvent('net.minecraftforge.event.TickEvent$PlayerTickEvent', event => {
  try {
    let player = event.player
    if (String(event.phase) != 'END' || player.level.isClientSide() || player.age % 20 != 7 || !player.isAlive()) return
    let Q = faQuirks(player)
    let q = Q.q
    if (q.sunblessed && !faIsNight(player.level) && String(player.level.dimension) == 'minecraft:overworld' && player.getHealth() < player.getMaxHealth()) {
      player.heal(q.sunblessed)
    }
    faSetModifier(player, $FAAttributes.MAX_HEALTH, 'fa_artifact_colossus', q.colossus || 0, $FAOperation.ADDITION)
    faSetModifier(player, $FAAttributes.ATTACK_SPEED, 'fa_artifact_quickdraw', (q.quickdraw || 0) / 100, $FAOperation.MULTIPLY_BASE)
    if (Q.main) {
      let server = player.getServer()
      if (server) server.runCommandSilent('execute in ' + player.level.dimension + ' run particle minecraft:dust ' + Q.main.getString('aura') + ' 1 ' +
        player.getX().toFixed(1) + ' ' + (player.getY() + 1).toFixed(1) + ' ' + player.getZ().toFixed(1) + ' 0.4 0.6 0.4 0 6 normal')
    }
  } catch (err) {
    faWarnOnce('artifact passives', err)
  }
})

// Prospector: stone you mine can drop Raw Ascendite
ForgeEvents.onEvent('net.minecraftforge.event.level.BlockEvent$BreakEvent', event => {
  try {
    let player = event.getPlayer()
    let q = faQuirks(player).q
    if (!q.prospector || Math.random() >= q.prospector / 100) return
    let level = event.getLevel()
    let block = level.getBlock(event.getPos())
    if (!block.hasTag('forge:stone') && String(block.id) != 'minecraft:stone' && String(block.id) != 'minecraft:deepslate') return
    let pos = event.getPos()
    let e = new $FAItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, Item.of('kubejs:raw_ascendite'))
    e.setDefaultPickUpDelay()
    level.addFreshEntity(e)
  } catch (err) {
    faWarnOnce('prospector', err)
  }
})
