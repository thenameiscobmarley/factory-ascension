// priority: 400
// Factory Ascension - mob food drops. Animals and some monsters drop body parts when they die (config: mobFood).
// Raw parts make you sick when eaten; cook them first. A mob that dies burning drops the cooked version.
// Uses $FAItemEntity and faWarnOnce from 20_fa_enchant_effects.js (same startup scope).

// mob id -> the foods it can drop, built once from the config
let faFoodByMob = null

function faFoodTable() {
  if (!faFoodByMob) {
    faFoodByMob = {}
    global.FA.mobFood.foods.forEach(fd => fd.mobs.forEach(m => { (faFoodByMob[m] = faFoodByMob[m] || []).push(fd) }))
  }
  return faFoodByMob
}

ForgeEvents.onEvent('net.minecraftforge.event.entity.living.LivingDropsEvent', event => {
  try {
    let MF = global.FA.mobFood
    if (!MF.enabled) return
    let mob = event.getEntity()
    if (mob.level.isClientSide()) return
    let foods = faFoodTable()[String(mob.type)]
    if (!foods || mob.isBaby()) return
    let bonus = MF.lootingBonus * event.getLootingLevel()
    let burning = mob.isOnFire()
    foods.forEach(fd => {
      if (Math.random() >= Math.min(1, fd.chance + bonus)) return
      let item = new $FAItemEntity(mob.level, mob.getX(), mob.getY() + 0.5, mob.getZ(), Item.of('kubejs:' + (burning ? fd.cooked : fd.raw)))
      item.setDefaultPickUpDelay()
      event.getDrops().add(item)
    })
  } catch (err) {
    faWarnOnce('mob food drops', err)
  }
})
