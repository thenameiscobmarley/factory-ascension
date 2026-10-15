// priority: 990
// Factory Ascension - conflicts (config: conflicts). If a mod that does the same job is installed, turn the pack's
// version off before anything uses it, and remember the message for players (shown when they log in) and the log.

global.FA_CONFLICTS = []
global.FA.conflicts.forEach(c => {
  let hit = Object.keys(c.mods).filter(id => Platform.isLoaded(id))[0]
  if (!hit) return
  if (c.off == 'danger') global.FA.danger.enabled = false
  else if (c.off == 'variants') global.FA.variants.enabled = false
  else if (c.off == 'homeCommand') global.FA.home.command = false
  let msg = 'The ' + c.mods[hit] + ' mod conflicts with this mod\'s ' + c.feature + ' feature: Turned ' + c.feature + ' off.'
  global.FA_CONFLICTS.push(msg)
  console.warn('[Factory Ascension] ' + msg)
})
