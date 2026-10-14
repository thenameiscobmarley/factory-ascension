// priority: 200
// Factory Ascension - combat tracking, for /home's "not in combat" rule (config: home).
// Hitting, or being hit by, a mob or another player marks you as in combat (by server tick).

function faMarkCombat(player) {
  let server = player.getServer()
  if (server) player.getPersistentData().putInt('fa_combat', server.getTickCount())
}

ForgeEvents.onEvent('net.minecraftforge.event.entity.living.LivingHurtEvent', event => {
  try {
    let victim = event.getEntity()
    let attacker = event.getSource().actual
    if (!(attacker instanceof $FALiving) || attacker === victim) return
    if (victim instanceof $FAPlayer) faMarkCombat(victim)
    if (attacker instanceof $FAPlayer) faMarkCombat(attacker)
  } catch (err) {
    faWarnOnce('combat tracking', err)
  }
})

// Seconds of combat left (0 = free to /home). Used by the server scripts.
global.FA_combatLeft = player => {
  let data = player.getPersistentData()
  if (!data.contains('fa_combat')) return 0
  let now = player.getServer().getTickCount()
  let at = data.getInt('fa_combat')
  if (at > now) return 0   // marked before a server restart (the tick count starts over)
  let ticks = at + global.FA.home.combatSeconds * 20 - now
  return ticks > 0 ? Math.ceil(ticks / 20) : 0
}
