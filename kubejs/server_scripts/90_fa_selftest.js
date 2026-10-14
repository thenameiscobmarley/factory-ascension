// priority: 100
// Factory Ascension - in-game self test: /ascension selftest (ops only, also works from the server console).
//
// Runs the pack's systems against the real game, the way the out-of-game harness can't: a fake player swings at
// real zombies, real Create blocks run the Altar and Condenser, real drops and hordes spawn. Every check is
// reported as PASS or FAIL, and everything it spawns or places is removed again afterwards.
// It works far out at x = 100000 (forceloaded while it runs), well away from anything you've built.

const $STFakePlayers = Java.loadClass('net.minecraftforge.common.util.FakePlayerFactory')
const $STBlockPos = Java.loadClass('net.minecraft.core.BlockPos')
const $STCaps = Java.loadClass('net.minecraftforge.common.capabilities.ForgeCapabilities')
const $STDirection = Java.loadClass('net.minecraft.core.Direction')
const $STFluidAction = Java.loadClass('net.minecraftforge.fluids.capability.IFluidHandler$FluidAction')
const $STItemEntity = Java.loadClass('net.minecraft.world.entity.item.ItemEntity')
const $STLiving = Java.loadClass('net.minecraft.world.entity.LivingEntity')
const $STArmorStand = Java.loadClass('net.minecraft.world.entity.decoration.ArmorStand')

let faSelftestBusy = false

function faSelftest(server, report, done) {
  let results = []
  let level = server.overworld()
  let X = 100000, Y = 120, Z = 100000
  let spawned = []
  let placed = []
  let check = (name, ok, detail) => {
    results.push({ name: name, ok: !!ok })
    report((ok ? '§aPASS§r ' : '§cFAIL§r ') + name + (ok || detail === undefined ? '' : '  (' + detail + ')'))
  }
  let attempt = (name, fn) => {
    try {
      fn()
    } catch (err) {
      check(name, false, String(err).slice(0, 160))
    }
  }
  let spawn = (type, x, y, z) => {
    let e = level.createEntity(type)
    e.setPositionAndRotation(x, y, z, 0, 0)
    level.addFreshEntity(e)
    spawned.push(e)
    return e
  }
  // Runs fn once the server has really ticked `ticks` more times. Each stage waits on the one before it, so
  // a lagging server (or a first run that has to generate the chunks out here) can't run them out of order.
  let after = (ticks, fn) => {
    let target = server.getTickCount() + ticks
    let poll = () => {
      if (server.getTickCount() >= target) attempt('stage', fn)
      else server.scheduleInTicks(1, poll)
    }
    server.scheduleInTicks(1, poll)
  }
  let place = (x, y, z, id) => {
    let pos = new $STBlockPos(x, y, z)
    level.getBlock(pos).set(id)
    placed.push(pos)
    return pos
  }

  server.runCommandSilent('forceload add ' + (X - 32) + ' ' + (Z - 32) + ' ' + (X + 32) + ' ' + (Z + 32))
  // A floor to stand things on
  for (let dx = -24; dx <= 24; dx++) for (let dz = -24; dz <= 24; dz++) place(X + dx, Y - 1, Z + dz, 'minecraft:stone')

  let player = $STFakePlayers.getMinecraft(level)
  player.setPositionAndRotation(X, Y, Z, 0, 0)
  player.setShiftKeyDown(false)
  let savedLoot = global.FA.loot.chance
  let savedCap = global.FA.loot.chanceCap
  let essenceKey = 'fa_essence_' + player.stringUuid

  // Wait until the forceloaded area is live (entities in it tick and can be found), up to 30 seconds
  let probe = level.createEntity('minecraft:armor_stand')
  probe.setPositionAndRotation(X, Y, Z, 0, 0)
  level.addFreshEntity(probe)
  let waited = 0
  let ready = () => {
    let live = level.getEntitiesOfClass($STArmorStand, player.getBoundingBox().inflate(4)).size() > 0
    if (!live && waited < 600) { waited += 10; after(10, ready); return }
    probe.discard()
    if (!live) report('§eThe test area never finished loading; results may be off')
    faSelftestRun(server, report, done, level, X, Y, Z, player, spawned, placed, check, attempt, spawn, place, after,
      results, savedLoot, savedCap, essenceKey)
  }
  after(20, ready)
}

function faSelftestRun(server, report, done, level, X, Y, Z, player, spawned, placed, check, attempt, spawn, place, after,
  results, savedLoot, savedCap, essenceKey) {

  // ---- 1. Config and scripts ----
  attempt('config', () => {
    check('config loaded with ' + Object.keys(global.FA.enchants).length + ' enchants', Object.keys(global.FA.enchants).length >= 74)
    check('config check found no problems', global.FA_validate(JSON.parse(JSON.stringify(global.FA))).length == 0)
  })

  // ---- 2. Essence, unlock, upgrade (server scripts on a real player object) ----
  let sword = Item.of('minecraft:diamond_sword')
  attempt('essence', () => {
    faSetEssence(player, 0)
    let cores = Item.of('kubejs:ascension_core', 10)
    player.setItemInHand('main_hand', cores)
    faAbsorb(player, cores, 'kubejs:ascension_core')
    check('absorbing a core adds 1 Essence', faGetEssence(player) == 1, faGetEssence(player))
    faSetEssence(player, 1e6)
    let tome = Item.of('kubejs:tome_sharpness', 2)
    faUnlock(player, tome, 'sharpness', sword)
    check('a tome unlocks Ascended Sharpness 1 on a real sword', faGetLevel(sword, 'sharpness') == 1 && tome.getCount() == 1, faGetLevel(sword, 'sharpness'))
    player.setItemInHand('main_hand', sword)
    faUpgrade(player, 'sharpness', 99)
    check('upgrading to Sharpness 100 spends Essence', faGetLevel(sword, 'sharpness') == 100 && faGetEssence(player) < 1e6, faGetLevel(sword, 'sharpness'))
  })

  // ---- 3. Combat hooks (startup scripts, fired by the real damage event) ----
  attempt('combat', () => {
    let z = spawn('minecraft:zombie', X + 2, Y, Z)
    // This far out the zombie is danger-scaled; clear that so only the enchant is measured
    ;['fa_danger', 'fa_elite', 'fa_boss'].forEach(k => z.getPersistentData().remove(k))
    z.setHealth(20)
    z.invulnerableTime = 0
    z.attack(player.damageSources().playerAttack(player), 1)
    // 1 x (1 + 0.1 x 100) = 11, then the zombie's own 2 armor points take ~8% off: about 10 damage
    check('Sharpness 100 turns a 1-damage hit into about 10 after armor', z.getHealth() < 11 && z.getHealth() > 8.5, z.getHealth())
  })

  // ---- 4. Danger tagging ----
  attempt('danger', () => {
    let danger = global.FA_dangerLevel(level, X, Z)
    let z = spawn('minecraft:zombie', X - 2, Y, Z)
    let tagged = z.getPersistentData().getInt('fa_danger')
    check('a zombie spawned ' + X + ' blocks out is tagged danger ' + danger, danger > 100 && tagged == danger, tagged + ' vs ' + danger)
  })

  // ---- 5. Chaos, gems, Rebirth ----
  attempt('chaos', () => {
    let gear = Item.of('minecraft:iron_sword')
    let chaos = Item.of('kubejs:chaos_tome', 1)
    faChaosRoll(player, chaos, gear)
    check('a Chaos Tome rolls an enchant onto real gear', faRolledSlots(gear).length == 1 && chaos.getCount() == 0)
    let gem = Item.of('kubejs:gem_bloodstone_3', 1)
    faSocket(player, gem, 'bloodstone', 3, gear)
    check('a gem sockets into real gear', faGems(gear).length == 1 && gem.getCount() == 0)
    faSetLevel(gear, 'sharpness', 1200)
    player.setItemInHand('main_hand', gear)
    faRebirth(player, true)
    check('Rebirth resets levels and adds a star', faStars(gear) == 1 && faGetLevel(gear, 'sharpness') == 1, faStars(gear))
  })

  // ---- 6. Altar + Condenser on real Create blocks (checked a second later) ----
  let altarPos, condPos, altarTank, condTank, depot, chestPos, altarGear, condFilled = 0
  attempt('machines setup', () => {
    altarTank = place(X + 4, Y, Z + 4, 'create:basin')
    altarPos = place(X + 4, Y + 1, Z + 4, 'kubejs:ascension_altar')
    depot = place(X + 4, Y + 2, Z + 4, 'create:depot')
    condTank = place(X - 4, Y, Z + 4, 'create:basin')
    condPos = place(X - 4, Y + 1, Z + 4, 'kubejs:essence_condenser')
    chestPos = place(X - 4, Y + 2, Z + 4, 'minecraft:chest')
    let dim = String(level.dimension)
    faAltars(server).putString(faAltarKey(dim, altarPos), '')
    faCondensers(server).putString(faAltarKey(dim, condPos), '')
  })

  // Give the new block entities a few ticks, then fill tanks and load the depot
  after(10, () => {
    attempt('machines fill', () => {
      let fill = pos => level.getBlockEntity(pos).getCapability($STCaps.FLUID_HANDLER, $STDirection.UP).resolve().orElse(null)
        .fill(Fluid.of('kubejs:liquid_essence', 8000), $STFluidAction.EXECUTE)
      let a = fill(altarTank), b = fill(condTank)
      check('basins accept Liquid Essence (' + a + ' / ' + b + ' mB)', a > 0 && b > 0, a + ' / ' + b)
      condFilled = b
      altarGear = Item.of('minecraft:diamond_pickaxe')
      faSetLevel(altarGear, 'excavation', 1)
      let items = level.getBlockEntity(depot).getCapability($STCaps.ITEM_HANDLER, $STDirection.DOWN).resolve().orElse(null)
      items.insertItem(0, altarGear, false)
    })
    after(10, faStage7)
  })

  // ---- 7. Loot, hordes, events, bosses, an Ultimate ----
  let faStage7 = () => {
    attempt('loot', () => {
      global.FA.loot.chance = 1
      global.FA.loot.chanceCap = 1
      let before = level.getEntitiesOfClass($STItemEntity, player.getBoundingBox().inflate(12)).size()
      let z = spawn('minecraft:zombie', X + 1, Y, Z + 1)
      z.attack(player.damageSources().playerAttack(player), 100000)
      let after = level.getEntitiesOfClass($STItemEntity, player.getBoundingBox().inflate(12)).size()
      check('a danger-zone kill by a player drops loot', after > before, before + ' -> ' + after)
    })
    global.FA.loot.chance = savedLoot
    global.FA.loot.chanceCap = savedCap
    attempt('horde', () => {
      let z = spawn('minecraft:zombie', X - 16, Y, Z - 16)
      after(3, () => attempt('horde check', () => {
        let near = level.getEntitiesOfClass($STLiving, z.getBoundingBox().inflate(6))
        let extras = 0
        for (let i = 0; i < near.size(); i++) {
          let m = near.get(i)
          if (m.getPersistentData().getBoolean('fa_horde')) { extras++; spawned.push(m) }
        }
        check('a deep spawn brings a horde (' + extras + ' extra)', extras > 0, extras)
      }))
    })
    attempt('events', () => {
      global.FA_startEvent(server, 'goldenHour')
      let on = global.FA_STATE.event && global.FA_STATE.event.type == 'goldenHour'
      global.FA_endEvent(server)
      check('world events start and stop', on && !global.FA_STATE.event)
    })
    attempt('boss', () => {
      let z = spawn('minecraft:zombie', X + 3, Y, Z - 3)
      global.FA_makeBoss(z, 40)
      check('a mob becomes an Ascended boss', z.getPersistentData().getBoolean('fa_boss') && !!z.getCustomName())
    })
    after(20, faUltimateStage)
  }
  let faUltimateStage = () => {
    attempt('ultimate', () => {
      // On its own patch, so the whirlwind doesn't hit the other tests' mobs
      player.setPositionAndRotation(X + 16, Y, Z + 16, 0, 0)
      let blade = Item.of('minecraft:netherite_sword')
      faSetLevel(blade, 'blade_storm', 1)
      global.FA_clearUltCooldowns(player.stringUuid)
      player.setItemInHand('main_hand', blade)
      player.setShiftKeyDown(true)
      let z = spawn('minecraft:zombie', X + 18, Y, Z + 16)
      ;['fa_danger', 'fa_elite', 'fa_boss'].forEach(k => z.getPersistentData().remove(k))
      z.setHealth(20)
      z.invulnerableTime = 0
      let hp = z.getHealth()
      z.attack(player.damageSources().playerAttack(player), 1)
      after(80, () => attempt('ultimate check', () => {
        check('a sneaking hit unleashes Blade Storm Dance (target took combo hits)', z.getHealth() < hp - 1 || !z.isAlive(), hp + ' -> ' + z.getHealth())
        check('the player is back where they started', Math.abs(player.getX() - (X + 16)) < 0.01 && Math.abs(player.getZ() - (Z + 16)) < 0.01, player.getX() + ',' + player.getZ())
      }))
    })
    // Outside the attempt, so a failed stage still reaches the summary
    after(120, () => {
      player.setShiftKeyDown(false)
      faFinish()
    })
  }

  // ---- 8. Machine results, then clean up and summarize ----
  let faFinish = () => {
    attempt('machines check', () => {
      let items = level.getBlockEntity(depot).getCapability($STCaps.ITEM_HANDLER, $STDirection.DOWN).resolve().orElse(null)
      let leveled = items.getStackInSlot(0)
      check('the Altar levelled the gear on its depot', !!leveled && faGetLevel(leveled, 'excavation') > 1, leveled ? faGetLevel(leveled, 'excavation') : 'no item')
      let chest = level.getBlockEntity(chestPos).getCapability($STCaps.ITEM_HANDLER, $STDirection.DOWN).resolve().orElse(null)
      let made = 0
      for (let i = 0; i < chest.getSlots(); i++) {
        let s = chest.getStackInSlot(i)
        if (s && !s.isEmpty() && String(s.getId()) == 'kubejs:condensed_essence_0') made += s.getCount()
      }
      // A basin holds 1,000 mB per tank, so expect one block per full bucket it took
      check('the Condenser packed Liquid Essence into blocks (' + made + ')', made >= 1 && made == Math.floor(condFilled / global.FA.condensed.mbPerBlock), made + ' from ' + condFilled + ' mB')
    })

    // Clean up: entities, blocks, machine registrations, forceload, test Essence
    spawned.forEach(e => { try { if (!e.isRemoved()) e.discard() } catch (err) {} })
    // Hordes and minions the test caused aren't in `spawned`: sweep up every danger-tagged mob at the test site
    let leftovers = level.getEntitiesOfClass($STLiving, player.getBoundingBox().inflate(60))
    for (let i = 0; i < leftovers.size(); i++) {
      let m = leftovers.get(i)
      if (m !== player && m.getPersistentData().contains('fa_danger')) m.discard()
    }
    let dim = String(level.dimension)
    try { faAltars(server).remove(faAltarKey(dim, altarPos)) } catch (err) {}
    try { faCondensers(server).remove(faAltarKey(dim, condPos)) } catch (err) {}
    placed.reverse().forEach(pos => { try { level.getBlock(pos).set('minecraft:air') } catch (err) {} })
    level.getEntitiesOfClass($STItemEntity, player.getBoundingBox().inflate(60)).forEach(e => e.discard())
    server.persistentData.remove(essenceKey)
    server.runCommandSilent('forceload remove ' + (X - 32) + ' ' + (Z - 32) + ' ' + (X + 32) + ' ' + (Z + 32))

    let failed = results.filter(r => !r.ok)
    report((failed.length ? '§c' : '§a') + 'Self test: ' + (results.length - failed.length) + ' / ' + results.length + ' passed' +
      (failed.length ? ' - failed: ' + failed.map(r => r.name).join('; ') : ''))
    done()
  }
}

ServerEvents.commandRegistry(event => {
  let { commands: Commands } = event
  event.register(Commands.literal('ascension')
    .then(Commands.literal('selftest').requires(src => src.hasPermission(2)).executes(ctx => {
      let src = ctx.source
      let server = src.getServer()
      let report = line => {
        console.info('[FA selftest] ' + line.replace(/§./g, ''))
        if (src.player) src.player.tell(Text.of(line))
      }
      if (faSelftestBusy) {
        report('§eThe self test is already running')
        return 0
      }
      faSelftestBusy = true
      report('Running the Factory Ascension self test (takes about 10 seconds)...')
      try {
        faSelftest(server, report, () => { faSelftestBusy = false })
      } catch (err) {
        faSelftestBusy = false
        report('§cThe self test could not start: ' + err)
      }
      return 1
    })))
})
