// priority: 280
// Factory Ascension - spawning rules for the Factory Ascension mod's own mobs (config: modMobs). Their biomes and
// light rules live in the mod; this cancels natural spawns that are too close to home for them (danger zones),
// and keeps the bosses rare and apart (config modMobs.bosses).
// Uses faWarnOnce from 20_fa_enchant_effects.js (same startup scope).

let $FAModAABB = Java.loadClass('net.minecraft.world.phys.AABB')

if (Platform.isLoaded('factoryascension')) {
  ForgeEvents.onEvent('net.minecraftforge.event.entity.living.MobSpawnEvent$FinalizeSpawn', event => {
    try {
      let mob = event.getEntity()
      let type = String(mob.type)
      if (type.indexOf('factoryascension:') != 0 || !global.FA.danger.enabled) return
      let reason = String(event.getSpawnType())
      if (reason != 'NATURAL' && reason != 'CHUNK_GENERATION') return
      let id = type.slice('factoryascension:'.length)
      let level = event.getLevel().getLevel()
      let min = global.FA.modMobs.minDanger[id] || 0
      if (min > 0 && global.FA_dangerLevel(level, event.getX(), event.getZ()) < min) { event.setSpawnCancelled(true); return }
      // Bosses: only some of the time, and never close to another of the same boss
      let B = global.FA.modMobs.bosses[id]
      if (!B) return
      if (Math.random() >= B.chance) { event.setSpawnCancelled(true); return }
      let r = B.spacing
      let near = level.getEntitiesOfClass(mob.getClass(), new $FAModAABB(event.getX() - r, -64, event.getZ() - r, event.getX() + r, 320, event.getZ() + r))
      for (let i = 0; i < near.size(); i++) if (near.get(i) !== mob && String(near.get(i).type) == type) { event.setSpawnCancelled(true); return }
    } catch (err) {
      faWarnOnce('mod mob spawns', err)
    }
  })
}
