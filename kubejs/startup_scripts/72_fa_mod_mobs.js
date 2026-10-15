// priority: 280
// Factory Ascension - spawning rules for the Factory Ascension mod's own mobs (config: modMobs). Their biomes and
// light rules live in the mod; this cancels natural spawns that are too close to home for them (danger zones).
// Uses faWarnOnce from 20_fa_enchant_effects.js (same startup scope).

if (Platform.isLoaded('factoryascension')) {
  ForgeEvents.onEvent('net.minecraftforge.event.entity.living.MobSpawnEvent$FinalizeSpawn', event => {
    try {
      let mob = event.getEntity()
      let type = String(mob.type)
      if (type.indexOf('factoryascension:') != 0 || !global.FA.danger.enabled) return
      let reason = String(event.getSpawnType())
      if (reason != 'NATURAL' && reason != 'CHUNK_GENERATION') return
      let min = global.FA.modMobs.minDanger[type.slice('factoryascension:'.length)] || 0
      if (min > 0 && global.FA_dangerLevel(event.getLevel().getLevel(), event.getX(), event.getZ()) < min) event.setSpawnCancelled(true)
    } catch (err) {
      faWarnOnce('mod mob spawns', err)
    }
  })
}
