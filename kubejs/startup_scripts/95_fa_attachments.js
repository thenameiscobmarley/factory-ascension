// priority: 50
// Factory Ascension - attachments: craftable add-ons for gear (config: attachments).
// Stored on the gear as FactoryAscension.attach {"0": id, "1": id}. Armor attachments work while worn; weapon, bow and
// tool attachments while the item is in your main hand. Uses helpers from 20_fa_enchant_effects.js (same startup scope).

const $FAProjectileWeapon = Java.loadClass('net.minecraft.world.item.ProjectileWeaponItem')

// What kind of gear a stack is, for which attachments fit: helmet, chest, legs, boots, bow, tool, weapon, or ''
function faGearSlot(stack) {
  if (!faWearable(stack)) return ''
  let item = stack.getItem()
  if (item instanceof $FAArmorItem) return { HEAD: 'helmet', CHEST: 'chest', LEGS: 'legs', FEET: 'boots' }[String(item.getEquipmentSlot())] || ''
  if (item instanceof $FAProjectileWeapon) return 'bow'
  if (item instanceof $FADiggerItem && !(item instanceof $FAAxeItem)) return 'tool'
  return 'weapon'
}
global.FA_gearSlot = stack => faGearSlot(stack)

function faAttachIds(stack) {
  if (faEmpty(stack)) return []
  let tag = stack.nbt
  if (!tag || !tag.contains(FA_TAG)) return []
  let data = tag.getCompound(FA_TAG)
  if (!data.contains('attach')) return []
  let at = data.getCompound('attach')
  let out = []
  at.getAllKeys().forEach(k => out.push(String(at.getString(k))))
  return out
}
global.FA_attachIds = stack => faAttachIds(stack)

// Attachments in effect: armor ones from worn armor, the rest from the main hand
function faActiveAttachments(player) {
  let on = {}
  if (!global.FA.attachments.enabled || !(player instanceof $FAPlayer)) return on
  let it = player.getAllSlots().iterator()
  let i = 0
  while (it.hasNext()) {
    let slot = it.next()
    let index = i++
    if (index == 1) continue
    faAttachIds(slot).forEach(id => {
      let d = global.FA_attachmentDef(id)
      if (!d) return
      let armor = ['helmet', 'chest', 'legs', 'boots'].indexOf(d.fits) >= 0
      if (armor ? index >= 2 : index == 0) on[id] = d.power
    })
  }
  return on
}
global.FA_activeAttachments = player => faActiveAttachments(player)

// ---- Lamps: a real light block that follows you (Headlamp, Mining Lamp) ----
let faLamps = {}   // player uuid -> "dim|x|y|z" of the light block we placed

function faClearLamp(player) {
  let key = faLamps[player.stringUuid]
  if (!key) return
  delete faLamps[player.stringUuid]
  let p = key.split('|')
  if (String(player.level.dimension) != p[0]) return
  let block = player.level.getBlock(new $FABlockPos(parseInt(p[1]), parseInt(p[2]), parseInt(p[3])))
  if (String(block.id) == 'minecraft:light') block.set('minecraft:air')
}

function faUpdateLamp(player, level) {
  let x = Math.floor(player.getX()), y = Math.floor(player.getY() + 1), z = Math.floor(player.getZ())
  let key = String(player.level.dimension) + '|' + x + '|' + y + '|' + z
  if (faLamps[player.stringUuid] == key) return
  let block = player.level.getBlock(new $FABlockPos(x, y, z))
  let id = String(block.id)
  if (id != 'minecraft:air' && id != 'minecraft:cave_air' && id != 'minecraft:light') return   // never replace real blocks
  faClearLamp(player)
  block.set('minecraft:light', { level: String(Math.max(1, Math.min(15, level))) })
  faLamps[player.stringUuid] = key
}

function faHide(effect, ticks, amp) {
  return new $FAEffectInstance(effect, ticks, amp, true, false)
}

ForgeEvents.onEvent('net.minecraftforge.event.TickEvent$PlayerTickEvent', event => {
  try {
    let player = event.player
    if (String(event.phase) != 'END' || player.level.isClientSide() || !player.isAlive()) return
    faAttachmentTick(player, player.age)
  } catch (err) {
    faWarnOnce('attachments', err)
  }
})

// One tick of attachment effects (also called by /ascension selftest, whose fake player doesn't tick on its own)
function faAttachmentTick(player, age) {
  {
    let A = faActiveAttachments(player)
    // Jetpack, every tick: sneak in the air to rise, otherwise glide
    if (A.jetpack && !player.onGround()) {
      if (player.isShiftKeyDown()) player.addEffect(faHide($FAEffects.LEVITATION, 4, A.jetpack + 1))
      else player.addEffect(faHide($FAEffects.SLOW_FALLING, 10, 0))
    }
    if (age % 4 == 0) {
      let lamp = Math.max(A.headlamp || 0, A.mining_lamp || 0)
      if (lamp > 0) faUpdateLamp(player, lamp)
      else faClearLamp(player)
    }
    if (age % 20 != 3) return
    if (A.rebreather) player.addEffect(faHide($FAEffects.WATER_BREATHING, 60, 0))
    if (A.spring_coils) player.addEffect(faHide($FAEffects.JUMP, 60, A.spring_coils - 1))
    if (A.drill_bit) player.addEffect(faHide($FAEffects.DIG_SPEED, 60, A.drill_bit - 1))
    if (A.thermal_visor) faEnemiesNear(player, A.thermal_visor, null, 48).forEach(m => m.addEffect(faHide($FAEffects.GLOWING, 60, 0)))
    faSetModifier(player, $FAAttributes.ARMOR, 'fa_attach_plating', A.armor_plating || 0, $FAOperation.ADDITION)
    faSetModifier(player, $FAAttributes.MOVEMENT_SPEED, 'fa_attach_rocket', (A.rocket_boots || 0) / 100, $FAOperation.MULTIPLY_BASE)
    faSetModifier(player, $FAForgeMod.STEP_HEIGHT_ADDITION.get(), 'fa_attach_hydraulics', A.hydraulics || 0, $FAOperation.ADDITION)
  }
}
global.FA_attachmentTick = (player, age) => faAttachmentTick(player, age)
global.FA_clearLamp = player => faClearLamp(player)

ForgeEvents.onEvent('net.minecraftforge.event.entity.player.PlayerEvent$PlayerLoggedOutEvent', event => {
  try {
    faClearLamp(event.getEntity())
  } catch (err) {
    faWarnOnce('lamp logout', err)
  }
})

// ---- Combat attachments ----
ForgeEvents.onEvent('net.minecraftforge.event.entity.living.LivingHurtEvent', event => {
  if (faSecondary) return
  try {
    let source = event.getSource()
    let attacker = source.actual
    let victim = event.getEntity()
    let A = faActiveAttachments(attacker)
    if (Object.keys(A).length) {
      let amount = event.getAmount()
      let projectile = source.is($FADamageTypeTags.IS_PROJECTILE)
      if (A.scope && projectile) amount *= 1 + A.scope / 100
      if (!projectile) {
        if (A.chainsaw) {
          amount *= 1 + A.chainsaw / 100
          let hit = attacker.damageSources().playerAttack(attacker)
          faEnemiesNear(victim, 2.5, [attacker], 2).forEach(m => faSecondaryHit(m, hit, amount * 0.5))
        }
        if (A.taser && Math.random() < A.taser / 100) {
          victim.addEffect(new $FAEffectInstance($FAEffects.MOVEMENT_SLOWDOWN, 20, 4))
          victim.addEffect(new $FAEffectInstance($FAEffects.WEAKNESS, 20, 1))
          faFx(victim, 'minecraft:electric_spark', 20, 'minecraft:block.beehive.work', 2)
        }
        if (A.flamethrower) victim.setSecondsOnFire(A.flamethrower)
        if (A.serrated_edge) victim.addEffect(new $FAEffectInstance($FAEffects.WITHER, A.serrated_edge * 20, 0))
      }
      event.setAmount(Math.min(amount, FA_MAX_FLOAT))
    }
    let V = faActiveAttachments(victim)
    if (V.knee_pads && source.is($FADamageTypeTags.IS_FALL)) event.setAmount(event.getAmount() * (1 - V.knee_pads / 100))
    if (V.spike_plating && attacker instanceof $FALiving && attacker !== victim && !source.is($FADamageTypeTags.IS_PROJECTILE)) {
      faSecondaryHit(attacker, victim.damageSources().thorns(victim), event.getAmount() * V.spike_plating / 100)
    }
  } catch (err) {
    faWarnOnce('attachment hits', err)
  }
})
