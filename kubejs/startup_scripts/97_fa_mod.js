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
  $FAModConfig.setStructureSpacing(global.FA.structures.spacing, global.FA.structures.cellChance)
  let RD = global.FA.raids
  $FAModConfig.setRaids(RD.enabled, RD.markTicks, RD.killsPerLevel, RD.firstRaidTicks, RD.raidEveryTicks, RD.partySize, RD.breakDoors, RD.bulletsPenetrate)
  let VH = global.FA.vehicles, $FABike = Java.loadClass('dev.factoryascension.vehicle.BikeParts')
  Object.keys(VH.engines).forEach(id => { let e = VH.engines[id]; $FABike.engine(id, e.name, e.power, e.top, e.weight, e.size, e.fuelUse, e.pitch, id == 'engine_v12') })
  Object.keys(VH.frames).forEach(id => { let f = VH.frames[id]; $FABike.frameStats(id, f.name, f.weight, f.maxEngine, f.grip, f.turn) })
  Object.keys(VH.gearboxes).forEach(id => { let g = VH.gearboxes[id]; $FABike.gearbox(id, g.name, g.gears, g.topMul, g.accelMul) })
  $FABike.clearFuels()
  VH.fuels.forEach(f => $FABike.fuel(f.item, f.name, f.amount, f.power, f.use, f.leftover))
  Object.keys(VH.cars).forEach(m => { let c = VH.cars[m]; $FAModConfig.setCar(m, c.accel, c.top, c.turn, c.mass, c.health, c.fuelUse) })
  $FAModConfig.setTurret(VH.turret.cooldownTicks, VH.turret.spread, VH.turret.damage, VH.turret.wrenchRepair)
  let BD = global.FA.body, GW = global.FA.gunWear, WE = global.FA.weather, NH = global.FA.nightHunter, EX = global.FA.extraction
  $FAModConfig.setBody(BD.enabled, BD.damageScale, BD.fractureFall, BD.fractureHit, BD.heavyBleedHit, BD.heavyBleedChance, BD.waterHours, BD.mendPerSecond)
  $FAModConfig.setGunWear(GW.enabled, GW.wearPerShot, GW.heatPerShot, GW.heatCooling, GW.jamBase, GW.jamWorn, GW.jamHot, GW.cleaningKitRestore, GW.cleaningKitUses)
  $FAModConfig.setWeather(WE.fogBanks, WE.fogChancePerMinute, WE.fogMinutes, WE.nightDarkness)
  $FAModConfig.setNightHunter(NH.enabled, NH.chancePerMinute, NH.minDanger)
  $FAModConfig.setExtraction(EX.minutes, EX.minDist, EX.maxDist, EX.radius, EX.seconds, EX.xp, EX.bodyBags)
  let HL = global.FA.heldLights, MV = global.FA.movement
  $FAModConfig.setHeldLights(HL.enabled, HL.dim)
  $FAModConfig.setMovement(MV.airStrafe, MV.bhop, MV.airCap, MV.airAccel, MV.maxSpeed, MV.bhopKeep)
  let TR = global.FA.traders, $FATraders = Java.loadClass('dev.factoryascension.raid.Traders')
  $FAModConfig.setCash(TR.cashBundle, TR.operatorCashChance)
  $FATraders.clear()
  TR.offers.forEach(o => $FATraders.offer(o[0], o[1], o[2], o[3], o[4]))
  TR.tasks.forEach(t => $FATraders.task(t[0], t[1], t[2], t[3], t[4], t[5], t[6], t[7]))
  Object.keys(TR.values).forEach(k => $FATraders.value(k, TR.values[k]))
  $FATraders.setLoyalty(TR.loyalty[0], TR.loyalty[1], TR.loyalty[2])
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
  $FAModConfig.setTarkovGrid(!!INV.grid)
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
