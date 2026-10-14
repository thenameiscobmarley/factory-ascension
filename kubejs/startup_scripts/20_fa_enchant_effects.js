// Factory Ascension - what the Ascended enchants actually do.
// Enchant levels live in each item's NBT under "FactoryAscension", e.g. {sharpness: 3000}.
// They count from every equipped item (both hands + armor) of a player.
// Forge events can only be hooked from startup scripts.

const $FAPlayer = Java.loadClass('net.minecraft.world.entity.player.Player')
const $FAItemEntity = Java.loadClass('net.minecraft.world.entity.item.ItemEntity')
const $FADamageTypeTags = Java.loadClass('net.minecraft.tags.DamageTypeTags')
const $FAAttributes = Java.loadClass('net.minecraft.world.entity.ai.attributes.Attributes')
const $FAModifier = Java.loadClass('net.minecraft.world.entity.ai.attributes.AttributeModifier')
const $FAOperation = Java.loadClass('net.minecraft.world.entity.ai.attributes.AttributeModifier$Operation')

const FA_TAG = 'FactoryAscension'
const FA_SPEED_MOD = 'factory_ascension_swiftness'

// A broken hook logs once and stays out of the way instead of spamming the log
let faWarned = {}
function faWarnOnce(where, err) {
  if (faWarned[where]) return
  faWarned[where] = true
  console.error('[Factory Ascension] ' + where + ' hook failed, skipping it: ' + err)
}

// Total level of one enchant across a player's hands and armor (0 for anything that isn't a player)
function faSum(entity, id) {
  if (!entity || !(entity instanceof $FAPlayer)) return 0
  let total = 0
  let it = entity.getAllSlots().iterator()
  while (it.hasNext()) {
    let tag = it.next().getTag()
    if (tag && tag.contains(FA_TAG)) total += tag.getCompound(FA_TAG).getInt(id)
  }
  return total
}

function faPer(id) {
  return global.FA.enchants[id].perLevel
}

// Set while Retribution is reflecting damage, so two players can't bounce it back and forth forever
let faReflecting = false

ForgeEvents.onEvent('net.minecraftforge.event.entity.living.LivingHurtEvent', event => {
  try {
    let source = event.getSource()
    let attacker = source.getEntity() // the owner for arrows and other projectiles
    let victim = event.getEntity()
    let amount = event.getAmount()
    let scaled = amount

    let sharp = faSum(attacker, 'sharpness')
    if (sharp > 0) scaled *= 1 + faPer('sharpness') * sharp

    // /kill and the void ignore defenses, so nobody can get stuck immortal
    if (!source.is($FADamageTypeTags.BYPASSES_INVULNERABILITY)) {
      let prot = faSum(victim, 'protection')
      if (prot > 0) scaled /= 1 + faPer('protection') * prot

      // Vitality acts like extra max health without drawing extra hearts:
      // 20 base health + V hidden health means each hit takes 20 / (20 + V) as much
      let vit = faSum(victim, 'vitality')
      if (vit > 0) scaled *= 20 / (20 + faPer('vitality') * vit)
    }

    scaled = Math.min(scaled, 3.0e38)
    if (scaled != amount) event.setAmount(scaled)

    let steal = faSum(attacker, 'lifesteal')
    if (steal > 0 && attacker.isAlive()) attacker.heal(Math.min(scaled * faPer('lifesteal') * steal, 3.0e38))

    let retri = faSum(victim, 'retribution')
    if (retri > 0 && !faReflecting && attacker && attacker !== victim && attacker.isAlive() && attacker.hurt) {
      faReflecting = true
      try {
        attacker.hurt(victim.damageSources().thorns(victim), Math.min(amount * faPer('retribution') * retri, 3.0e38))
      } finally {
        faReflecting = false
      }
    }
  } catch (err) {
    faWarnOnce('damage', err)
  }
})

ForgeEvents.onEvent('net.minecraftforge.event.entity.living.LivingDropsEvent', event => {
  try {
    // Never touch a player's death drops
    if (event.getEntity() instanceof $FAPlayer) return

    let plunder = faSum(event.getSource().getEntity(), 'plunder')
    if (plunder <= 0) return

    let mult = 1 + faPer('plunder') * plunder
    let budget = global.FA.maxExtraDropStacks
    let drops = event.getDrops()
    let extras = []

    let it = drops.iterator()
    while (it.hasNext()) {
      let itemEntity = it.next()
      let stack = itemEntity.getItem()
      if (stack.isEmpty()) continue

      // Fractional part becomes a chance for one more item
      let exact = stack.getCount() * mult
      let total = Math.floor(exact)
      if (Math.random() < exact - total) total++

      let max = stack.getMaxStackSize()
      itemEntity.setItem(stack.copyWithCount(Math.min(total, max)))

      let left = total - max
      while (left > 0 && budget > 0) {
        let n = Math.min(left, max)
        let extra = new $FAItemEntity(itemEntity.level(), itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), stack.copyWithCount(n))
        extra.setDefaultPickUpDelay()
        extras.push(extra)
        left -= n
        budget--
      }
    }

    extras.forEach(e => drops.add(e))
  } catch (err) {
    faWarnOnce('plunder', err)
  }
})

// Swiftness: re-applied whenever equipment changes (this also fires on login, respawn and enchant upgrades)
ForgeEvents.onEvent('net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent', event => {
  try {
    let player = event.getEntity()
    if (!(player instanceof $FAPlayer)) return

    let instance = player.getAttribute($FAAttributes.MOVEMENT_SPEED)
    if (!instance) return
    let amount = Math.min(global.FA.speedCap, faPer('swiftness') * faSum(player, 'swiftness'))

    let old = []
    let it = instance.getModifiers().iterator()
    while (it.hasNext()) {
      let mod = it.next()
      if (mod.getName() == FA_SPEED_MOD) old.push(mod)
    }
    // Skip the work when nothing changed (this event fires a lot)
    if (old.length == 1 && old[0].getAmount() == amount) return
    old.forEach(mod => instance.removeModifier(mod))
    if (amount > 0) instance.addTransientModifier(new $FAModifier(FA_SPEED_MOD, amount, $FAOperation.MULTIPLY_BASE))
  } catch (err) {
    faWarnOnce('swiftness', err)
  }
})

// Excavation: runs on both client and server, which both see the item NBT, so mining stays in sync
ForgeEvents.onEvent('net.minecraftforge.event.entity.player.PlayerEvent$BreakSpeed', event => {
  try {
    let level = faSum(event.getEntity(), 'excavation')
    if (level > 0) event.setNewSpeed(Math.min(event.getNewSpeed() * (1 + faPer('excavation') * level), 3.0e38))
  } catch (err) {
    faWarnOnce('excavation', err)
  }
})
