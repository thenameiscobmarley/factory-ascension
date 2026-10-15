// Factory Ascension - collared pets (server_scripts/51_fa_pets_charms.js puts the collars on) take no fire or fall damage.
const FA_PET_IMMUNE = { inFire: true, onFire: true, lava: true, hotFloor: true, fall: true, flyIntoWall: true }

ForgeEvents.onEvent('net.minecraftforge.event.entity.living.LivingAttackEvent', event => {
  try {
    let e = event.getEntity()
    if (e.getPersistentData().getInt('fa_collar') > 0 && FA_PET_IMMUNE[String(event.getSource().getType())]) event.setCanceled(true)
  } catch (err) { /* never break damage */ }
})
