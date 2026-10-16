// priority: 10
// Factory Ascension - the bridge to the Factory Ascension mod (the "Full" download's jar). Without the mod this does nothing.
//   - pushes the config's machine numbers (machines.smelter, machines.engine, machines.salvager, machines.storage), guns and
//     the Tarkov inventory (inventory) into the mod
//   - passes the mod's machine events (Salvager timer, Fusion Table slots and button) to server_scripts/46_fa_machines.js

if (Platform.isLoaded('factoryascension')) {
  let $FAModConfig = Java.loadClass('dev.factoryascension.FAConfig')
  let M = global.FA.machines
  M.smelter.tiers.forEach((t, i) => $FAModConfig.setSmelterTier(i, t[0], t[1]))
  $FAModConfig.setSmelter(M.smelter.cookTicksAt64Rpm, M.smelter.minRpm)
  $FAModConfig.setEngine(M.engine.suPerCoil, M.engine.coilsPerBurner, M.engine.maxModules, M.engine.fuelMultiplier)
  M.engine.rpm.forEach((r, i) => $FAModConfig.setEngineRpm(i, r))
  $FAModConfig.setStorage(M.storage.range, M.storage.maxLinks)
  let SG = global.FA.guns.shotgun
  $FAModConfig.setShotgun(SG.pellets, SG.pelletDamage, SG.spreadHip, SG.spreadAim, SG.range, SG.fullDamageRange, SG.falloff,
    SG.cooldownTicks, SG.capacity, SG.reloadTicks, SG.kickback, SG.breaksGlass)
  global.FA.guns.list.forEach(g => {
    $FAModConfig.setGun(g.id, g.damage, g.rpmScale, g.magBonus, g.spreadScale, g.rangeScale, g.recoilScale)
    $FAModConfig.clearGunCost(g.id)
    ;(g.cost || []).forEach(c => $FAModConfig.addGunCost(g.id, c[0], c[1]))
  })
  $FAModConfig.setSkins(global.FA.guns.workbench.skinItem, global.FA.guns.workbench.skinCost)
  let NP = global.FA.npcs
  $FAModConfig.setNpcs(NP.healthScale, NP.damageScale, NP.spreadScale, NP.bleedChance, NP.gunDropChance, NP.patrols, NP.patrolChance, NP.patrolMinDanger,
    NP.maxNearby, NP.flareHunters)
  $FAModConfig.setMobHearing(NP.mobsHearShots, NP.mobHearingScale)
  let RD = global.FA.raids
  $FAModConfig.setRaids(RD.enabled, RD.markTicks, RD.killsPerLevel, RD.firstRaidTicks, RD.raidEveryTicks, RD.partySize, RD.breakDoors, RD.bulletsPenetrate)
  let DR = global.FA.drugs
  $FAModConfig.setDrugSeeds(DR.seedChance)
  $FAModConfig.clearChem()
  DR.lab.forEach(r => {
    r.in.forEach(i => $FAModConfig.chemInput(i[0], i[1]))
    $FAModConfig.chemOutput(r.out, r.count)
  })
  let $FAStress = Java.loadClass('dev.factoryascension.gun.Stress')
  global.FA_MOD_DANGER = (player, danger) => $FAStress.setDanger(player, danger)
  Object.keys(global.FA.guns.ammo.perCraft).forEach(c => $FAModConfig.setAmmoPerCraft(c, global.FA.guns.ammo.perCraft[c]))
  $FAModConfig.setBench(global.FA.guns.workbench.partItem, global.FA.guns.workbench.tierItem)
  let FU = global.FA.guns.fumbles
  $FAModConfig.setFumbles(FU.enabled, FU.base, FU.stress)
  $FAModConfig.explosiveBreaksBlocks = global.FA.guns.explosiveRoundsBreakBlocks
  let FG = global.FA.guns.flareGun
  $FAModConfig.setFlareGun(FG.cooldownTicks, FG.speed, Math.round(FG.airdropSeconds * 20), FG.airdropHeight, FG.airdropLoot)
  $FAModConfig.setRescue(FG.rescue, FG.rescueChance, Math.round(FG.rescueSeconds * 20), FG.rescueRadius)
  let INV = global.FA.inventory
  $FAModConfig.setTarkov(INV.enabled, INV.width, INV.height, INV.blockPickup)
  INV.sizes.forEach(s => $FAModConfig.setTarkovSize(s[0], s[1], s[2]))
  $FAModConfig.setWeight(INV.weight.enabled, INV.weight.kgPerCell, INV.weight.carryKg, INV.weight.slowPerKg, INV.weight.maxSlow)
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
