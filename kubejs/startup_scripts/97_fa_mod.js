// priority: 10
// Factory Ascension - the bridge to the Factory Ascension mod (the "Full" download's jar). Without the mod this does nothing.
//   - pushes the config's machine numbers (machines.smelter, machines.engine, machines.salvager, machines.storage) into the mod
//   - passes the mod's machine events (Salvager timer, Fusion Table slots and button) to server_scripts/46_fa_machines.js

if (Platform.isLoaded('factoryascension')) {
  let $FAModConfig = Java.loadClass('dev.factoryascension.FAConfig')
  let M = global.FA.machines
  M.smelter.tiers.forEach((t, i) => $FAModConfig.setSmelterTier(i, t[0], t[1]))
  $FAModConfig.setSmelter(M.smelter.cookTicksAt64Rpm, M.smelter.minRpm)
  $FAModConfig.setEngine(M.engine.suPerCoil, M.engine.coilsPerBurner, M.engine.maxModules, M.engine.fuelMultiplier)
  M.engine.rpm.forEach((r, i) => $FAModConfig.setEngineRpm(i, r))
  $FAModConfig.setStorage(M.storage.range, M.storage.maxLinks)
  $FAModConfig.setQuarry(M.quarry.radius, M.quarry.work, M.quarry.maxPerTick, M.quarry.minRpm, M.quarry.stress)
  $FAModConfig.setGrinder(M.grinder.reach, M.grinder.damagePerRpm, M.grinder.interval, M.grinder.minRpm, M.grinder.stress)
  global.FA.backpacks.tiers.forEach((t, i) => $FAModConfig.setBackpack(i, t.slots, t.upgrades))
  let MC = M.miniCrusher
  $FAModConfig.setMiniCrusher(MC.ticksPerItem, MC.chargePerItem)
  MC.motors.forEach(m => $FAModConfig.addMotor('kubejs:' + m.id, m.speed))
  global.FA.attachments.batteries.forEach(b => $FAModConfig.addBattery('kubejs:' + b.id))
  let LG = global.FA.legendary
  $FAModConfig.setLegendary(LG.maxStars)
  LG.costs.forEach((cost, star) => cost.forEach(c => $FAModConfig.addLegendCost(star, c[0], c[1])))
  LG.grades.forEach(g => $FAModConfig.addLegendGrade(g[0]))
  $FAModConfig.setSalvager(Math.max(1, Math.round(M.salvager.seconds * 20)))

  ForgeEvents.onEvent('dev.factoryascension.event.MachineEvent', event => {
    try {
      if (global.FA_onMachine) global.FA_onMachine(event)
    } catch (err) {
      console.error('[Factory Ascension] machine event ' + event.getKind() + ' failed: ' + err)
    }
  })
}
