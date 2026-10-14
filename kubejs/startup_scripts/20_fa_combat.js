// Factory Ascension - combat and loot hooks.
// Forge events can only be hooked from startup scripts. They read global.FA_LEVELS,
// which the server script keeps up to date, so a hit costs one map lookup.

const $FAPlayer = Java.loadClass('net.minecraft.world.entity.player.Player')
const $FAItemEntity = Java.loadClass('net.minecraft.world.entity.item.ItemEntity')
const $FADamageTypeTags = Java.loadClass('net.minecraft.tags.DamageTypeTags')

// A broken hook logs once and stays out of the way instead of spamming the log
let faWarned = {}
function faWarnOnce(where, err) {
  if (faWarned[where]) return
  faWarned[where] = true
  console.error('[Factory Ascension] ' + where + ' hook failed, skipping it: ' + err)
}

function faLevelOf(entity) {
  if (!entity || !(entity instanceof $FAPlayer)) return 0
  let level = global.FA_LEVELS[entity.getStringUUID()]
  return level > 0 ? level : 0
}

ForgeEvents.onEvent('net.minecraftforge.event.entity.living.LivingHurtEvent', event => {
  try {
    let cfg = global.FA
    let source = event.getSource()
    let amount = event.getAmount()
    let scaled = amount

    // getEntity() is the owner for arrows and other projectiles, so bows count too
    let attackerLevel = faLevelOf(source.getEntity())
    if (attackerLevel > 0) scaled *= 1 + cfg.damageDealtPerLevel * attackerLevel

    // /kill and the void bypass this, so nobody can get stuck immortal
    let victimLevel = faLevelOf(event.getEntity())
    if (victimLevel > 0 && !source.is($FADamageTypeTags.BYPASSES_INVULNERABILITY)) {
      scaled /= 1 + cfg.damageTakenPerLevel * victimLevel
    }

    if (scaled != amount) event.setAmount(Math.min(scaled, 3.0e38))
  } catch (err) {
    faWarnOnce('damage scaling', err)
  }
})

ForgeEvents.onEvent('net.minecraftforge.event.entity.living.LivingDropsEvent', event => {
  try {
    // Never touch a player's death drops
    if (event.getEntity() instanceof $FAPlayer) return

    let level = faLevelOf(event.getSource().getEntity())
    if (level <= 0) return

    let cfg = global.FA
    let mult = 1 + cfg.lootPerLevel * level
    let budget = cfg.maxExtraDropStacks
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
    faWarnOnce('loot scaling', err)
  }
})
