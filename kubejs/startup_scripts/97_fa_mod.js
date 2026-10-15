// priority: 10
// Factory Ascension - the bridge to the Factory Ascension mod (the "Full" download's jar). Without the mod this does nothing.
//   - pushes the config's machine numbers (machines.smelter, machines.engine, machines.salvager) into the mod
//   - passes the mod's machine events (Salvager timer, Fusion Table slots and button) to server_scripts/46_fa_machines.js

if (Platform.isLoaded('factoryascension')) {
  let $FAModConfig = Java.loadClass('dev.factoryascension.FAConfig')
  let M = global.FA.machines
  M.smelter.tiers.forEach((t, i) => $FAModConfig.setSmelterTier(i, t[0], t[1]))
  $FAModConfig.setSmelter(M.smelter.cookTicksAt64Rpm, M.smelter.minRpm)
  $FAModConfig.setEngine(M.engine.suPerCoil, M.engine.coilsPerBurner, M.engine.maxModules, M.engine.fuelMultiplier)
  M.engine.rpm.forEach((r, i) => $FAModConfig.setEngineRpm(i, r))
  $FAModConfig.setSalvager(Math.max(1, Math.round(M.salvager.seconds * 20)))

  ForgeEvents.onEvent('dev.factoryascension.event.MachineEvent', event => {
    try {
      if (global.FA_onMachine) global.FA_onMachine(event)
    } catch (err) {
      console.error('[Factory Ascension] machine event ' + event.getKind() + ' failed: ' + err)
    }
  })
}
