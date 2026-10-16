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
const $STHand = Java.loadClass('net.minecraft.world.InteractionHand')
const $STEffects = Java.loadClass('net.minecraft.world.effect.MobEffects')
const $STAttributes = Java.loadClass('net.minecraft.world.entity.ai.attributes.Attributes')
const $STArrayList = Java.loadClass('java.util.ArrayList')
const $STHit = Java.loadClass('net.minecraft.world.phys.BlockHitResult')
const $STCompoundTag = Java.loadClass('net.minecraft.nbt.CompoundTag')
const $STSlot = Java.loadClass('net.minecraft.world.entity.EquipmentSlot')
const $STVec = Java.loadClass('net.minecraft.world.phys.Vec3')
const $STBoom = Java.loadClass('net.minecraft.world.level.Level$ExplosionInteraction')
const $STAABB = Java.loadClass('net.minecraft.world.phys.AABB')

let faSelftestBusy = false
let faSelftestCenterOn = 'home'   // the real danger.centerOn, put back when the test ends
let armedRoom = false             // the Room TNT armed in the room test, checked at the end
let skinwalkerAt = false          // where the test skinwalker turned, checked at the end
let bossAt = false                // the boss hurt in the mob test, checked at the end

function faSelftest(server, report, done) {
  let results = []
  let level = server.overworld()
  let X = 100000, Y = 120, Z = 100000
  let spawned = []
  let placed = []
  // The test's fake player isn't in the player list, so mobs at the test site measure danger from spawn while it runs
  faSelftestCenterOn = global.FA.danger.centerOn
  global.FA.danger.centerOn = 'spawn'
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
  if (server.getLevel('factoryascension:ascended_realm')) server.runCommandSilent('execute in factoryascension:ascended_realm run forceload add ' + X + ' ' + Z)
  // A clean start: whatever an earlier run left behind (a crash skips the clean-up) is cleared, blocks and mobs, so
  // stale machines can't poison this run. Fill works on at most 32,768 blocks at once: 16 x 16 columns at a time.
  for (let cx = X - 32; cx < X + 32; cx += 16) for (let cz = Z - 32; cz < Z + 32; cz += 16)
    server.runCommandSilent('fill ' + cx + ' ' + (Y - 14) + ' ' + cz + ' ' + (cx + 15) + ' ' + (Y + 12) + ' ' + (cz + 15) + ' minecraft:air')
  server.runCommandSilent('kill @e[type=!minecraft:player,x=' + (X - 34) + ',y=' + (Y - 20) + ',z=' + (Z - 34) + ',dx=68,dy=40,dz=68]')
  // A floor to stand things on
  for (let dx = -24; dx <= 24; dx++) for (let dz = -24; dz <= 24; dz++) place(X + dx, Y - 1, Z + dz, 'minecraft:stone')

  let player = $STFakePlayers.getMinecraft(level)
  // Open files and threads at the start, for the leak check at the end
  global.FA_stLeak0 = { files: -1, threads: 0 }
  if (Platform.isLoaded('factoryascension')) {
    let $Health0 = Java.loadClass('dev.factoryascension.HealthMonitor')
    global.FA_stLeak0 = { files: $Health0.openFiles(), threads: $Health0.threads() }
  }
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

  // Leveled tomes: level one in hand, apply it with a real click, then extract it back into a tome
  attempt('tome levels', () => {
    let tome = Item.of('kubejs:tome_cleave')
    player.setItemInHand('main_hand', tome)
    player.setItemInHand('off_hand', Item.of('minecraft:air'))
    faUpgradeTome(player, 49)
    let tl = faGetLevel(player.getMainHandItem(), 'cleave')
    let axe = Item.of('minecraft:iron_axe')
    player.setItemInHand('off_hand', axe)
    player.gameMode.useItem(player, level, player.getMainHandItem(), $STHand.MAIN_HAND)
    let onAxe = faGetLevel(player.getOffhandItem(), 'cleave')
    check('a tome leveled to 50 in hand puts Cleave 50 on an axe (real click)', tl == 50 && onAxe == 50, tl + ' / ' + onAxe)
    player.setItemInHand('main_hand', player.getOffhandItem())
    player.setItemInHand('off_hand', Item.of('kubejs:blank_ascension_tome'))
    let before = player.getInventory().countItem(Item.of('kubejs:tome_cleave').getItem())
    faExtract(player, 'cleave')
    let gotBack = player.getInventory().countItem(Item.of('kubejs:tome_cleave').getItem()) - before
    check('[Extract] pulls Cleave 50 off the axe into a tome', faGetLevel(player.getMainHandItem(), 'cleave') == 0 && gotBack == 1, gotBack)
    player.getInventory().clearContent()
    player.setItemInHand('main_hand', sword)
  })

  // A real right-click, through Minecraft's own item-use code (the way a player's click reaches the scripts)
  attempt('tome right-click', () => {
    let useWith = (tomeHand, tome, gear) => {
      player.setItemInHand(tomeHand == 'main' ? 'main_hand' : 'off_hand', tome)
      player.setItemInHand(tomeHand == 'main' ? 'off_hand' : 'main_hand', gear)
      player.gameMode.useItem(player, level, tome, tomeHand == 'main' ? $STHand.MAIN_HAND : $STHand.OFF_HAND)
      return faGetLevel(tomeHand == 'main' ? player.getOffhandItem() : player.getMainHandItem(), 'plunder')
    }
    let a = useWith('main', Item.of('kubejs:tome_plunder'), Item.of('minecraft:iron_sword'))
    let b = useWith('off', Item.of('kubejs:tome_plunder'), Item.of('minecraft:iron_sword'))
    check('right-clicking a tome enchants the gear in the other hand (tome in either hand)', a == 1 && b == 1, a + ' / ' + b)
    player.setItemInHand('off_hand', Item.of('minecraft:air'))
    player.setItemInHand('main_hand', sword)   // the combat check below swings the Sharpness 100 sword
  })

  // The title you get when you walk into a new danger zone
  attempt('zone notice', () => {
    player.setPositionAndRotation(0.5, 100, 0.5, 0, 0)
    global.FA_dangerNotice(player)
    player.setPositionAndRotation(X, Y, Z, 0, 0)
    let shown = global.FA_dangerNotice(player)
    check('walking out into a danger zone shows its title', shown == 'zone', shown)
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

  // ---- Gear condition: graded when held, Mint hits harder, a Polishing Cloth rerolls (real clicks) ----
  attempt('condition', () => {
    let blade = Item.of('minecraft:iron_sword')
    player.setItemInHand('main_hand', blade)
    global.FA_conditionCheck(player)
    let grade = global.FA_conditionOf(player.getMainHandItem())
    check('gear gets a condition grade when held (' + (grade >= 0 ? global.FA.condition.grades[grade].name : 'none') + ')', grade >= 0, grade)
    // Same swing, two zombies: one with a plain sword, one with a Mint Condition sword
    let swing = held => {
      player.setItemInHand('main_hand', held)
      let z = spawn('minecraft:zombie', X + 2, Y, Z + 3)
      ;['fa_danger', 'fa_elite', 'fa_boss'].forEach(k => z.getPersistentData().remove(k))
      z.setHealth(20)
      z.invulnerableTime = 0
      z.attack(player.damageSources().playerAttack(player), 4)
      return 20 - z.getHealth()
    }
    let plain = swing(Item.of('minecraft:stick'))
    let minted = Item.of('minecraft:iron_sword')
    global.FA_setCondition(minted, 4)
    let mintHit = swing(minted)
    check('a Mint Condition weapon hits 12% harder', Math.abs(mintHit / plain - global.FA.condition.grades[4].damage) < 0.02, plain.toFixed(2) + ' vs ' + mintHit.toFixed(2))
    let worn = Item.of('minecraft:iron_sword')
    global.FA_setCondition(worn, 0)
    player.setItemInHand('main_hand', Item.of('kubejs:polishing_cloth', 64))
    player.setItemInHand('off_hand', worn)
    for (let i = 0; i < 40 && global.FA_conditionOf(player.getOffhandItem()) == 0; i++) {
      player.gameMode.useItem(player, level, player.getMainHandItem(), $STHand.MAIN_HAND)
    }
    let after = global.FA_conditionOf(player.getOffhandItem())
    check('a Polishing Cloth (right-click) improves a Battle-Scarred sword', after > 0 && player.getMainHandItem().getCount() < 64, after)
    player.setItemInHand('off_hand', Item.of('minecraft:air'))
    player.setItemInHand('main_hand', sword)
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
  // ---- Ascendite: gear stats, ore drops, recipes, worldgen ----
  attempt('ascendite', () => {
    let AS = global.FA.ascendite
    let pick = Item.of('kubejs:ascendite_pickaxe')
    let chest = Item.of('kubejs:ascendite_chestplate')
    let defense = chest.item.getDefense()
    check('Ascendite gear has its own tier (pickaxe ' + pick.getMaxDamage() + ' uses, chestplate ' + defense + ' armor)',
      pick.getMaxDamage() == AS.tools.uses && defense == AS.armor.protection[2], pick.getMaxDamage() + ' / ' + defense)
    // Mine a real ore block with a real pickaxe and see what drops
    let orePos = place(X - 8, Y, Z - 8, 'kubejs:ascendite_ore')
    player.setItemInHand('main_hand', Item.of('minecraft:iron_pickaxe'))
    player.gameMode.destroyBlock(orePos)
    let raw = 0
    let drops = level.getEntitiesOfClass($STItemEntity, player.getBoundingBox().move(-8, 0, -8).inflate(3))
    for (let i = 0; i < drops.size(); i++) {
      let it = drops.get(i).getItem()
      if (String(it.getId()) == 'kubejs:raw_ascendite') raw += it.getCount()
      drops.get(i).discard()
    }
    check('mining Ascendite ore drops Raw Ascendite', raw >= 1, raw)
    let recipeIds = ['core_by_hand', 'core_from_shards', 'core_mixing', 'core_compacting', 'core_mixing_unheated', 'mill_raw_ascendite', 'starter_brass', 'starter_precision_mechanism', 'ascendite_pickaxe', 'press_cobblestone_ascendite']
    let missing = recipeIds.filter(r => !server.getRecipeManager().byKey('kubejs:factory_ascension/' + r).isPresent())
    check('the cheaper core recipes and starter recipes are loaded', missing.length == 0, missing.join(', '))
    // The ore feature is in the world's worldgen registry: place one vein into a block of stone and look for ore
    let sx = X + 10, sy = Y + 2, sz = Z - 14
    for (let dx = -3; dx <= 3; dx++) for (let dy = -3; dy <= 3; dy++) for (let dz = -3; dz <= 3; dz++) place(sx + dx, sy + dy, sz + dz, 'minecraft:stone')
    // A vein is random and can miss the stone, so give it a few tries
    let ores = 0
    for (let tries = 0; tries < 5 && ores == 0; tries++) {
      server.runCommandSilent('execute in minecraft:overworld run place feature kubejs:ore_ascendite ' + sx + ' ' + sy + ' ' + sz)
      for (let dx = -3; dx <= 3; dx++) for (let dy = -3; dy <= 3; dy++) for (let dz = -3; dz <= 3; dz++) {
        if (String(level.getBlock(new $STBlockPos(sx + dx, sy + dy, sz + dz)).id) == 'kubejs:ascendite_ore') ores++
      }
    }
    check('Ascendite ore worldgen is loaded (a vein placed ' + ores + ' ore)', ores > 0, ores)
  })

  // ---- Mob food: a real pig drops parts, raw food makes you sick, cooking recipes are loaded ----
  attempt('mob food', () => {
    let foods = global.FA.mobFood.foods
    let saved = foods.map(fd => fd.chance)
    foods.forEach(fd => { fd.chance = 1 })
    let pig = spawn('minecraft:pig', X - 6, Y, Z + 8)
    // A plain pig (out here it can spawn as an elite or boss that shrugs off the hit)
    ;['fa_danger', 'fa_elite', 'fa_boss', 'fa_skinwalker', 'fa_variant'].forEach(k => pig.getPersistentData().remove(k))
    pig.setHealth(2)
    pig.attack(player.damageSources().playerAttack(player), 1000)
    foods.forEach((fd, i) => { fd.chance = saved[i] })
    let parts = []
    let near = level.getEntitiesOfClass($STItemEntity, pig.getBoundingBox().inflate(3))
    for (let i = 0; i < near.size(); i++) {
      let id = String(near.get(i).getItem().getId())
      if (id.indexOf('kubejs:') == 0) parts.push(id)
      near.get(i).discard()
    }
    check('a pig drops Pig Eyes and a Pig Brain', parts.indexOf('kubejs:pig_eyes') >= 0 && parts.indexOf('kubejs:pig_brain') >= 0, parts.join(', ') || 'nothing')
    player.removeAllEffects()
    let raw = Item.of('kubejs:pig_eyes')
    player.setItemInHand('main_hand', raw)
    raw.finishUsingItem(level, player)
    check('eating raw Pig Eyes makes you sick (Hunger)', player.hasEffect($STEffects.HUNGER))
    player.removeAllEffects()
    let cooks = ['cook_', 'smoke_', 'campfire_'].filter(k => server.getRecipeManager().byKey('kubejs:factory_ascension/' + k + 'pig_eyes').isPresent())
    check('raw food cooks in a furnace, smoker and campfire', cooks.length == 3, cooks.join(', '))
  })

  // ---- Hordes hit harder and take less; the mob loot switch turns Plunder off ----
  attempt('horde strength and loot switch', () => {
    let plain = spawn('minecraft:zombie', X - 10, Y, Z + 14), horde = spawn('minecraft:zombie', X - 12, Y, Z + 14)
    ;[plain, horde].forEach(z => {
      z.getPersistentData().putInt('fa_danger', 1)
      ;['fa_elite', 'fa_boss'].forEach(k => z.getPersistentData().remove(k))
      z.setNoAi(true)
      z.getAttribute($STAttributes.MAX_HEALTH).setBaseValue(1000)
      z.setHealth(1000)
    })
    horde.getPersistentData().putBoolean('fa_horde', true)
    plain.attack(player.damageSources().playerAttack(player), 300)
    horde.invulnerableTime = 0
    horde.attack(player.damageSources().playerAttack(player), 300)
    let lostPlain = 1000 - plain.getHealth(), lostHorde = 1000 - horde.getHealth()
    let ratio = lostHorde > 0 ? lostPlain / lostHorde : 0
    plain.discard()
    horde.discard()
    check('horde mobs are ' + global.FA.horde.healthMult + 'x tougher: the same hit takes ' + lostPlain.toFixed(1) + ' from a normal zombie, ' + lostHorde.toFixed(2) + ' from a horde one',
      Math.abs(ratio - global.FA.horde.healthMult) < 0.5, ratio)
    // Two cows killed with a Plunder sword, the switch on then off
    let sword = Item.of('minecraft:netherite_sword')
    faSetLevel(sword, 'plunder', 2000)
    player.setItemInHand('main_hand', sword)
    let kill = () => {
      let cow = spawn('minecraft:cow', X - 14, Y, Z + 18)
      ;['fa_danger', 'fa_elite', 'fa_boss'].forEach(k => cow.getPersistentData().remove(k))
      cow.setHealth(1)
      cow.attack(player.damageSources().playerAttack(player), 100)
      let n = 0
      let near = level.getEntitiesOfClass($STItemEntity, cow.getBoundingBox().inflate(3))
      for (let i = 0; i < near.size(); i++) { n += near.get(i).getItem().getCount(); near.get(i).discard() }
      return n
    }
    if (global.FA_lootOff(player)) global.FA_toggleLoot(player)
    let on = kill()
    global.FA_toggleLoot(player)
    let offFlag = global.FA_lootOff(player)
    let off = kill()
    global.FA_toggleLoot(player)
    player.setItemInHand('main_hand', Item.of('minecraft:air'))
    check('mob loot switch: a Plunder 2000 kill drops ' + on + ' items, with the switch off ' + off, offFlag && !global.FA_lootOff(player) && on > 20 && off <= 8, on + ' / ' + off)
  })

  // ---- Pets and charms: collars in order, the whistle, charm effects and the magnet ----
  attempt('pets and charms', () => {
    let wolf = spawn('minecraft:wolf', X - 8, Y, Z - 8)
    ;['fa_danger', 'fa_elite', 'fa_boss'].forEach(k => wolf.getPersistentData().remove(k))
    wolf.tame(player)
    let refused = global.FA_applyCollar(wolf, 1)
    let ok = global.FA_applyCollar(wolf, 0)
    let C0 = global.FA.pets.collars[0]
    let hp = wolf.getMaxHealth()
    check('Ascension Collar: a tamed wolf refuses the Voidsteel Collar first ("' + refused + '"), then takes the Ascendite one (' + hp + ' max health)',
      refused.length > 0 && ok == '' && hp >= C0.health && wolf.getPersistentData().getInt('fa_collar') == 1, refused + ' / ' + ok + ' / ' + hp)
    wolf.setPositionAndRotation(X - 20, Y, Z - 20, 0, 0)
    let called = global.FA_whistle(player)
    let dist = Math.sqrt(Math.pow(wolf.getX() - player.getX(), 2) + Math.pow(wolf.getZ() - player.getZ(), 2))
    check('Pet Whistle: calls the wolf back from 17 blocks away (' + called + ' called, now ' + dist.toFixed(1) + ' blocks away)', called >= 1 && dist < 2, called + ' / ' + dist)
    player.removeAllEffects()
    player.getInventory().setItem(20, Item.of('kubejs:iron_charm'))
    player.getInventory().setItem(21, Item.of('kubejs:magnet_charm'))
    let stick = level.createEntity('minecraft:item')
    stick.setItem(Item.of('minecraft:stick'))
    stick.setPosition(player.getX() + 5, player.getY(), player.getZ())
    stick.spawn()
    let got = global.FA_charmTick(player)
    let pulled = Math.abs(stick.getX() - player.getX()) < 1
    check('charms: an Iron Charm anywhere in the inventory gives Resistance, a Magnet Charm pulls a stick 5 blocks away (' + JSON.stringify(got) + ')',
      player.hasEffect($STEffects.DAMAGE_RESISTANCE) && pulled, player.hasEffect($STEffects.DAMAGE_RESISTANCE) + ' / ' + pulled)
    stick.discard()
    player.getInventory().setItem(20, Item.of('minecraft:air'))
    player.getInventory().setItem(21, Item.of('minecraft:air'))
    player.removeAllEffects()
    // Soulbound: thrown out, it comes straight back; lying on the ground it can't burn or despawn
    let bound = Item.of('minecraft:iron_sword')
    faSetLevel(bound, 'soulbound', 1)
    player.getInventory().setItem(23, bound.copy())
    let before = player.getInventory().getItem(23).getCount()
    player.getInventory().setItem(23, Item.of('minecraft:air'))
    player.drop(bound.copy(), false)
    let back = 0
    for (let i = 0; i < player.getInventory().getContainerSize(); i++) if (global.FA_isSoulbound(player.getInventory().getItem(i))) { back++; player.getInventory().setItem(i, Item.of('minecraft:air')) }
    let ground = level.createEntity('minecraft:item')
    ground.setItem(bound.copy())
    ground.setPosition(X - 6, Y + 1, Z - 6)
    ground.spawn()
    let lavaProof = ground.isInvulnerable() && !ground.attack(level.damageSources().lava(), 100) && ground.isAlive()
    ground.discard()
    // Death round trip: the drops lose it, the respawn gives it back
    let deathDrops = new $STArrayList()
    let dying = level.createEntity('minecraft:item')
    dying.setItem(bound.copy())
    deathDrops.add(dying)
    let keptN = global.FA_keepSoulboundGear(player, deathDrops)
    let returnedN = global.FA_returnSoulboundGear(player)
    let backAfterDeath = 0
    for (let i = 0; i < player.getInventory().getContainerSize(); i++) if (global.FA_isSoulbound(player.getInventory().getItem(i))) { backAfterDeath++; player.getInventory().setItem(i, Item.of('minecraft:air')) }
    check('Soulbound gear leaves the death drops (' + keptN + ', ' + deathDrops.size() + ' left) and comes back on respawn (' + returnedN + ')',
      keptN == 1 && deathDrops.size() == 0 && returnedN == 1 && backAfterDeath == 1, [keptN, deathDrops.size(), returnedN, backAfterDeath].join(' / '))
    check('Soulbound: thrown out it comes back to the inventory (' + back + '), on the ground it can\'t burn or despawn (' + lavaProof + ')', before == 1 && back == 1 && lavaProof, back + ' / ' + lavaProof)
    let recipesMissing = global.FA.pets.collars.map(c => 'collar_' + c.id).concat(['pet_whistle', 'charm_ascended', 'unbinding_scroll', 'tome_soulbound'])
      .concat(global.FA.charms.list.filter(c => c.id != 'ascended_charm').map(c => 'charm_' + c.id))
      .filter(r => !server.getRecipeManager().byKey('kubejs:factory_ascension/' + r).isPresent())
    check('every collar, the whistle and every charm has a recipe', recipesMissing.length == 0, recipesMissing.join(', '))
  })

  // ---- Rock Hammer: hand mode and block mode, through Minecraft's own click handling ----
  attempt('rock hammer', () => {
    player.getCooldowns().removeCooldown(Item.of('kubejs:rock_hammer').getItem())
    let hammer = Item.of('kubejs:rock_hammer')
    let rocks = Item.of('minecraft:cobblestone', 16)
    player.setItemInHand('main_hand', hammer)
    player.setItemInHand('off_hand', rocks)
    player.gameMode.useItem(player, level, hammer, $STHand.MAIN_HAND)
    let left = player.getOffhandItem().getCount()
    check('Rock Hammer: right-click smashes one cobblestone from the other hand (and wears the hammer)',
      left == 15 && player.getMainHandItem().getDamageValue() == 1, left + ' rocks left, damage ' + player.getMainHandItem().getDamageValue())
    player.setItemInHand('off_hand', Item.of('minecraft:air'))
    player.getCooldowns().removeCooldown(hammer.getItem())
    let rockPos = place(X - 10, Y, Z - 4, 'minecraft:cobblestone')
    player.setPositionAndRotation(X - 10, Y, Z - 2, 180, 0)
    let hit = new $STHit(new $STVec(X - 9.5, Y + 0.5, Z - 3.0), $STDirection.SOUTH, rockPos, false)
    player.gameMode.useItemOn(player, level, player.getMainHandItem(), $STHand.MAIN_HAND, hit)
    let after = String(level.getBlock(rockPos).id)
    let pops = level.getEntitiesOfClass($STItemEntity, player.getBoundingBox().inflate(4)).size()
    check('Rock Hammer: right-clicking a placed cobblestone smashes it in place', after == 'minecraft:air', after + ', ' + pops + ' drops')
    level.getEntitiesOfClass($STItemEntity, player.getBoundingBox().inflate(4)).forEach(e => e.discard())
    player.setPositionAndRotation(X, Y, Z, 0, 0)
  })

  // ---- /home, card packs, Artifacts ----
  attempt('home', () => {
    player.getPersistentData().remove('fa_combat')
    player.getPersistentData().remove('fa_home_used')
    player.setPositionAndRotation(X + 3, Y, Z - 3, 0, 0)
    faSetHome(player)
    // The teleport itself is vanilla /tp, which can't target Forge's fake player; check that /home goes through
    // out of combat and is refused in combat
    player.setPositionAndRotation(X + 20, Y, Z + 20, 0, 0)
    let went = faGoHome(player) == 1
    player.getPersistentData().remove('fa_home_used')
    player.getPersistentData().putInt('fa_combat', server.getTickCount())
    let refused = faGoHome(player) == 0
    check('/home goes through out of combat and is refused in combat', went && refused, went + ' / ' + refused)
    player.getPersistentData().remove('fa_combat')
    server.persistentData.getCompound('fa_homes').remove(player.stringUuid)
    player.setPositionAndRotation(X, Y, Z, 0, 0)
  })
  attempt('card packs', () => {
    player.getInventory().clearContent()
    let pack = Item.of('kubejs:enchantment_pack', 2)
    player.setItemInHand('main_hand', pack)
    player.gameMode.useItem(player, level, pack, $STHand.MAIN_HAND)
    let tomes = 0
    let inv = player.getInventory()
    for (let i = 0; i < inv.getContainerSize(); i++) {
      let st = inv.getItem(i)
      if (st && !st.isEmpty() && String(st.getId()).indexOf('kubejs:tome_') == 0) tomes += st.getCount()
    }
    check('opening an Enchantment Pack (real click) gives 5 tome cards', tomes == global.FA.packs.cards && player.getMainHandItem().getCount() == 1, tomes)
    player.getInventory().clearContent()
    let found = 0, caches = 0
    for (let i = 0; i < 40; i++) server.runCommandSilent('loot spawn ' + (X - 12) + ' ' + (Y + 2) + ' ' + (Z + 12) + ' loot minecraft:chests/simple_dungeon')
    let near = level.getEntitiesOfClass($STItemEntity, player.getBoundingBox().move(-12, 0, 12).inflate(6))
    for (let i = 0; i < near.size(); i++) {
      let id = String(near.get(i).getItem().getId())
      if (id == 'kubejs:enchantment_pack') found++
      if (id == 'kubejs:gear_cache') caches += near.get(i).getItem().getCount()
      near.get(i).discard()
    }
    check('Enchantment Packs and Gear Caches turn up in dungeon chests (' + found + ' packs, ' + caches + ' caches in 40 chests)', found > 0 && caches > 0, found + ' / ' + caches)
    // A cache opened with a real click gives a piece of relic gear
    player.getInventory().clearContent()
    let cache = Item.of('kubejs:gear_cache')
    player.setItemInHand('main_hand', cache)
    player.gameMode.useItem(player, level, cache, $STHand.MAIN_HAND)
    let gearGot = ''
    let inv2 = player.getInventory()
    for (let i = 0; i < inv2.getContainerSize(); i++) {
      let st = inv2.getItem(i)
      if (st && !st.isEmpty() && /^minecraft:(iron|diamond|netherite)_/.test(String(st.getId()))) gearGot = String(st.getId()) + ' "' + st.getHoverName().getString() + '"'
    }
    check('a Sealed Gear Cache (real click) gives relic gear: ' + gearGot, gearGot != '', gearGot)
    player.getInventory().clearContent()
  })
  attempt('artifacts', () => {
    let blade = Item.of('minecraft:netherite_sword')
    faSetLevel(blade, 'sharpness', 3000)
    player.setItemInHand('main_hand', blade)
    faSetEssence(player, 1e6)
    player.getInventory().add(Item.of('kubejs:ascended_soul'))
    let ok = faAwaken(player) == 1
    let held = player.getMainHandItem()
    let name = held.getHoverName().getString()
    check('Awakening makes a one-of-one Artifact: ' + name, ok && global.FA_isArtifact(held), name)
    // Soulbound round trip through Minecraft's own item save and load
    let persisted = player.getPersistentData().getCompound('PlayerPersisted')
    let box = new $STCompoundTag()
    box.put('0', held.save(new $STCompoundTag()))
    persisted.put('fa_soulbound', box)
    player.getPersistentData().put('PlayerPersisted', persisted)
    player.getInventory().clearContent()
    let n = global.FA_returnSoulbound(player)
    let back = false
    let inv = player.getInventory()
    for (let i = 0; i < inv.getContainerSize(); i++) if (global.FA_isArtifact(inv.getItem(i))) back = true
    check('a Soulbound Artifact comes back intact after death', n == 1 && back)
    player.getInventory().clearContent()
    player.setItemInHand('main_hand', sword)
  })

  // ---- Attachments: attach with a real click, and a Headlamp really lights the world ----
  attempt('attachments', () => {
    let blade = Item.of('minecraft:iron_sword')
    player.setItemInHand('main_hand', Item.of('kubejs:attachment_chainsaw'))
    player.setItemInHand('off_hand', blade)
    player.gameMode.useItem(player, level, player.getMainHandItem(), $STHand.MAIN_HAND)
    let on = global.FA_attachIds(player.getOffhandItem())
    check('a Chainsaw attaches to a sword (real click)', on.length == 1 && on[0] == 'chainsaw', on.join(','))
    let helm = Item.of('minecraft:iron_helmet')
    player.setItemInHand('main_hand', Item.of('kubejs:attachment_headlamp'))
    player.setItemInHand('off_hand', helm)
    player.gameMode.useItem(player, level, player.getMainHandItem(), $STHand.MAIN_HAND)
    player.setItemSlot($STSlot.HEAD, player.getOffhandItem())
    player.setItemInHand('off_hand', Item.of('minecraft:air'))
    player.setItemInHand('main_hand', Item.of('minecraft:air'))
    player.setPositionAndRotation(X - 6, Y, Z - 14, 0, 0)
    global.FA_attachmentTick(player, 4)
    let lightPos = new $STBlockPos(X - 6, Y + 1, Z - 14)
    let lit = String(level.getBlock(lightPos).id) == 'minecraft:light' ? level.getBlockState(lightPos).getLightEmission() : 0
    global.FA_clearLamp(player)
    let cleared = String(level.getBlock(lightPos).id) == 'minecraft:air'
    check('a Headlamp places a real light block at your head (light ' + lit + ') and cleans it up', lit == 15 && cleared, lit + ' / ' + cleared)
    player.setItemSlot($STSlot.HEAD, Item.of('minecraft:air'))
    player.setPositionAndRotation(X, Y, Z, 0, 0)
    // Electric Spike Wrap on a real sword, powered by a real battery in the inventory
    let zapSword = Item.of('minecraft:iron_sword')
    player.setItemInHand('main_hand', Item.of('kubejs:attachment_electric_spike_wrap'))
    player.setItemInHand('off_hand', zapSword)
    player.gameMode.useItem(player, level, player.getMainHandItem(), $STHand.MAIN_HAND)
    player.setItemInHand('main_hand', player.getOffhandItem())
    player.setItemInHand('off_hand', Item.of('minecraft:air'))
    let cellSlot = 20
    player.getInventory().setItem(cellSlot, Item.of('kubejs:copper_battery'))
    let target = spawn('minecraft:husk', X - 3, Y, Z - 3)
    ;['fa_danger', 'fa_elite', 'fa_boss'].forEach(k => target.getPersistentData().remove(k))
    target.setHealth(20)
    target.invulnerableTime = 0
    let hp0 = target.getHealth()
    target.attack(player.damageSources().playerAttack(player), 1)
    let used = player.getInventory().getItem(cellSlot).getDamageValue()
    let W = global.FA_attachmentDef('electric_spike_wrap')
    check('Electric Spike Wrap + Copper Battery: a 1-damage hit does ' + (hp0 - target.getHealth()).toFixed(1) + ' and uses ' + used + ' charge',
      used == W.charge && hp0 - target.getHealth() >= (1 + W.spikes + W.power) * 0.85, used + ' / ' + (hp0 - target.getHealth()))
    player.getInventory().setItem(cellSlot, Item.of('minecraft:air'))
    target.discard()
    player.setItemInHand('main_hand', sword)
  })

  // ---- The Factory Ascension mod's blocks (Full download only) ----
  if (Platform.isLoaded('factoryascension')) attempt('mod blocks', () => {
    // Ascension Vault: 108 slots, upgrades, sorting
    let vpos = place(X + 14, Y, Z + 10, 'factoryascension:ascension_vault')
    let vault = level.getBlockEntity(vpos)
    let ok1 = vault && vault.faGetCapacity() == 108
    vault.faSetTier(2)
    let items = vault.items()
    items.setStackInSlot(5, Item.of('minecraft:stone', 10))
    items.setStackInSlot(0, Item.of('minecraft:dirt', 3))
    items.setStackInSlot(9, Item.of('minecraft:stone', 20))
    vault.sortContents()
    let sorted = String(items.getStackInSlot(0).getId()) == 'minecraft:dirt' && items.getStackInSlot(1).getCount() == 30 && items.getStackInSlot(2).isEmpty()
    check('Ascension Vault: 108 slots, upgrades to ' + vault.faGetCapacity() + ', sorting merges and orders stacks', ok1 && vault.faGetCapacity() == 324 && sorted)
    // Card Binder: only cards, counts the collection, shows the best
    let bpos = place(X + 14, Y, Z + 12, 'factoryascension:card_binder')
    let binder = level.getBlockEntity(bpos)
    let best = Item.of('kubejs:tome_cleave')
    global.FA_setFoil(best, 6)
    let refused = binder.items().insertItem(0, Item.of('minecraft:dirt'), false).getCount() == 1
    binder.items().insertItem(0, Item.of('kubejs:tome_sharpness'), false)
    binder.items().insertItem(1, best, false)
    binder.items().insertItem(2, Item.of('kubejs:tome_sharpness'), false)
    check('Card Binder: takes only cards, counts ' + binder.uniqueCards() + ' different, shows the Void-foiled one on top', refused && binder.uniqueCards() == 2 &&
      String(binder.showcase().getId()) == 'kubejs:tome_cleave')
    // Essence Forge: levels the gear in its slot from its tank
    let fpos = place(X + 14, Y, Z + 14, 'factoryascension:essence_forge')
    let forge = level.getBlockEntity(fpos)
    let pick = Item.of('minecraft:iron_pickaxe')
    faSetLevel(pick, 'excavation', 1)
    forge.items().setStackInSlot(0, pick)
    let filled = forge.tank().fill(Fluid.of('kubejs:liquid_essence', 8000), $STFluidAction.EXECUTE)
    let given = global.FA_runForge(level, fpos)
    let after = faGetLevel(forge.items().getStackInSlot(0), 'excavation')
    check('Essence Forge: ' + given + ' levels from its own tank (Excavation 1 -> ' + after + ')', filled == 8000 && given > 0 && after == 1 + given && forge.levelsLastSecond() == given, filled + ' / ' + given)
    // Pack Opener: opens a pack into its outputs and shows the cards
    let opos = place(X + 14, Y, Z + 16, 'factoryascension:pack_opener')
    let opener = level.getBlockEntity(opos)
    opener.faInput().setStackInSlot(0, Item.of('kubejs:enchantment_pack', 2))
    global.FA_runOpener(level, opos)
    let out = 0
    // Count cards, not slots: two identical cards stack into one slot
    for (let i = 0; i < opener.faOutput().getSlots(); i++) out += opener.faOutput().getStackInSlot(i).getCount()
    check('Pack Opener: opens a pack into ' + out + ' cards and shows them', out == global.FA.packs.cards && opener.recent().size() == global.FA.packs.cards &&
      opener.faInput().getStackInSlot(0).getCount() == 1, out)
    // Salvager: a pack goes in, its value comes out as cores (or waits in the bank)
    let spos = place(X + 16, Y, Z + 10, 'factoryascension:salvager')
    let sal = level.getBlockEntity(spos)
    sal.faInput().setStackInSlot(0, Item.of('kubejs:booster_box', 1))
    sal.faInput().setStackInSlot(1, Item.of('minecraft:dirt', 5))
    global.FA_runSalvager(level, spos)
    let paid = 0
    for (let i = 0; i < sal.faOutput().getSlots(); i++) {
      let o = sal.faOutput().getStackInSlot(i)
      if (String(o.getId()) == 'kubejs:ascension_core') paid += o.getCount()
      if (String(o.getId()) == 'kubejs:dense_ascension_core') paid += 9 * o.getCount()
    }
    let passed = false
    for (let i = 0; i < sal.faOutput().getSlots(); i++) if (String(sal.faOutput().getStackInSlot(i).getId()) == 'minecraft:dirt') passed = true
    let boxValue = global.FA.sell.items['kubejs:booster_box']
    check('Salvager: a Booster Box pays ' + paid + ' cores (+' + sal.faGetBank().toFixed(2) + ' banked), dirt passes through', Math.abs(paid + sal.faGetBank() - boxValue) < 0.01 && passed,
      paid + ' / ' + boxValue)
    // Tome Fusion Table: merge two leveled tomes (one foiled), then make a Fusion from its parents
    let tpos = place(X + 16, Y, Z + 12, 'factoryascension:tome_fusion_table')
    let table = level.getBlockEntity(tpos)
    let t1 = Item.of('kubejs:tome_sharpness'), t2 = Item.of('kubejs:tome_sharpness')
    faSetLevel(t1, 'sharpness', 10)
    faSetLevel(t2, 'sharpness', 20)
    global.FA_setFoil(t2, 2)   // Gold x1.5: counts as 30
    table.faItems().setStackInSlot(0, t1)
    table.faItems().setStackInSlot(1, t2)
    global.FA_fuseTable(table, false)
    let merged = table.faItems().getStackInSlot(4)
    let mergedOk = String(merged.getId()) == 'kubejs:tome_sharpness' && faTomeLevel(merged, 'sharpness') == 40 && global.FA_foilOf(merged) < 0 &&
      table.faItems().getStackInSlot(0).isEmpty() && table.faItems().getStackInSlot(1).isEmpty()
    table.faItems().setStackInSlot(4, Item.empty)
    let fusion = Object.keys(global.FA.enchants).filter(k => global.FA.enchants[k].type == 'fusion' && !global.FA.enchants[k].triad)[0]
    let parts = global.FA.enchants[fusion].parts
    table.faItems().setStackInSlot(0, Item.of('kubejs:tome_' + parts[0]))
    table.faItems().setStackInSlot(1, Item.of('kubejs:tome_' + parts[1]))
    let noCore = global.FA_fuseTable(table, false) == 0
    table.faItems().setStackInSlot(3, Item.of('kubejs:ascension_core', 1))
    global.FA_fuseTable(table, false)
    let made = String(table.faItems().getStackInSlot(4).getId()) == 'kubejs:tome_' + fusion && table.faItems().getStackInSlot(3).isEmpty()
    check('Tome Fusion Table: merges Sharpness 10 + 20 (Gold foil) into 40, and makes ' + global.FA.enchants[fusion].name + ' from its parents and a core',
      mergedOk && noCore && made, mergedOk + ' ' + noCore + ' ' + made)
    // Super Smelter: knows smelting recipes, upgrades add lanes and stress
    let mpos = place(X + 16, Y, Z + 14, 'factoryascension:super_smelter')
    let sm = level.getBlockEntity(mpos)
    let ingot = sm.result(Item.of('minecraft:raw_iron'))
    let lanes0 = sm.lanes()
    sm.faSetTier(2)
    check('Super Smelter: raw iron -> ' + ingot.getId() + ', ' + lanes0 + ' -> ' + sm.lanes() + ' items at once after two upgrades',
      String(ingot.getId()) == 'minecraft:iron_ingot' && lanes0 == global.FA.machines.smelter.tiers[0][0] && sm.lanes() == global.FA.machines.smelter.tiers[2][0])
    // Modular Engine: a core, one coil and one burner with coal runs at the first speed with one coil's capacity
    let epos = place(X + 16, Y, Z + 16, 'factoryascension:engine_core')
    place(X + 17, Y, Z + 16, 'factoryascension:engine_coil')
    let bpos2 = place(X + 18, Y, Z + 16, 'factoryascension:engine_burner')
    level.getBlockEntity(bpos2).fuel().setStackInSlot(0, Item.of('minecraft:coal', 2))
    let engine = level.getBlockEntity(epos)
    for (let i = 0; i < 3; i++) engine.tick()
    let G = global.FA.machines.engine
    check('Modular Engine: core + coil + burning coal = ' + engine.rpm() + ' RPM, ' + engine.su() + ' SU', engine.coils() == 1 && engine.litBurners() == 1 &&
      engine.rpm() == G.rpm[0] && Math.abs(engine.su() - G.suPerCoil) < 0.01, engine.status().getString())
    // Engine screens: the menu behind a burner finds the core and reports the same numbers
    let $EngineMenu = Java.loadClass('dev.factoryascension.menu.EngineMenu')
    let em = new $EngineMenu(0, player.getInventory(), bpos2, $EngineMenu.BURNER)
    let lone = new $EngineMenu(0, player.getInventory(), place(X + 16, Y, Z + 18, 'factoryascension:engine_gearbox'), $EngineMenu.GEARBOX)
    check('engine screens: burner sees ' + em.rpm() + ' RPM, ' + em.su() + ' SU, lit ' + em.burnerLit() + '; a lone gearbox says not connected',
      em.connected() && em.rpm() == engine.rpm() && Math.abs(em.su() - engine.su()) <= 1 && em.burnerLit() && em.coils() == 1 && !lone.connected())
    // Universal Storage Terminal: link a chest and a barrel, read and write them as one big chest, sort across both
    let termPos = place(X + 20, Y, Z + 16, 'factoryascension:storage_terminal')
    let cpos = place(X + 22, Y, Z + 16, 'minecraft:chest')
    let rpos = place(X + 24, Y, Z + 16, 'minecraft:barrel')
    level.getBlockEntity(cpos).setItem(5, Item.of('minecraft:iron_ingot', 20))
    level.getBlockEntity(rpos).setItem(0, Item.of('minecraft:iron_ingot', 30))
    let term = level.getBlockEntity(termPos)
    let l1 = String(term.toggle(cpos)), l2 = String(term.toggle(rpos)), l3 = String(term.toggle(place(X + 20, Y, Z + 18, 'minecraft:stone')))
    let inv = term.storage()
    let n0 = inv.getSlots()
    inv.setStackInSlot(30, Item.of('minecraft:gold_ingot', 3))      // slot 30 = the barrel's slot 3
    let viaTerminal = String(level.getBlockEntity(rpos).getItem(3).getId())
    term.sortContents()
    let first = term.storage().getStackInSlot(0), second = term.storage().getStackInSlot(1)
    let tm = term.createMenu(0, player.getInventory(), player)
    check('Storage Terminal: a chest + a barrel = ' + n0 + ' slots, writes land in the right chest, Sort merges across both (' + first.getCount() + ' ' + first.getId() + ')',
      l1 == 'LINKED' && l2 == 'LINKED' && l3 == 'NOT_STORAGE' && n0 == 54 && viaTerminal == 'minecraft:gold_ingot' &&
      String(first.getId()) == 'minecraft:gold_ingot' && String(second.getId()) == 'minecraft:iron_ingot' && second.getCount() == 50 && tm.capacity() == 54 && tm.links() == 2,
      [l1, l2, l3, n0, viaTerminal, first.getId(), second.getId(), second.getCount(), tm.capacity(), tm.links()].join(' '))
    // Mini Crusher: Raw Ascendite + an Electric Motor + a Copper Battery -> Create's crushing results, using charge
    let crushPos = place(X + 28, Y, Z + 16, 'factoryascension:mini_crusher')
    let crusher = level.getBlockEntity(crushPos)
    crusher.items().setStackInSlot(0, Item.of('kubejs:raw_ascendite', 2))
    crusher.items().setStackInSlot(5, Item.of('kubejs:electric_motor'))
    crusher.items().setStackInSlot(6, Item.of('kubejs:copper_battery'))
    for (let t = 0; t < 260; t++) crusher.serverTick()
    let crushedOut = []
    for (let o = 1; o <= 4; o++) { let st = crusher.items().getStackInSlot(o); if (!st.isEmpty()) crushedOut.push(st.getCount() + ' ' + st.getId()) }
    let crushCharge = crusher.items().getStackInSlot(6).getDamageValue()
    check('Mini Crusher: a motor and a battery crush Raw Ascendite (' + crushedOut.join(', ') + '), using ' + crushCharge + ' charge',
      crushedOut.some(x => x.indexOf('kubejs:crushed_raw_ascendite') >= 0) && crushCharge >= global.FA.machines.miniCrusher.chargePerItem, crushedOut.join(', ') + ' / ' + crushCharge)
    // Ascension backpacks (with Sophisticated Backpacks): every tier exists, has its recipe, and a placed one keeps a
    // storage block entity (so placing a backpack never loses what's inside)
    if (Platform.isLoaded('sophisticatedbackpacks')) {
      let bpMissing = global.FA.backpacks.tiers.filter(t => Item.of('factoryascension:' + t.id + '_backpack').isEmpty() ||
        !server.getRecipeManager().byKey('kubejs:factory_ascension/backpack_' + t.id).isPresent()).map(t => t.id)
      let bpPos = place(X + 30, Y, Z + 16, 'factoryascension:ascended_backpack')
      let bpBe = level.getBlockEntity(bpPos)
      check('Ascension backpacks: all ' + global.FA.backpacks.tiers.length + ' tiers with recipes, and a placed one keeps its storage (' + (bpBe ? bpBe.getClass().getSimpleName() : 'none') + ')',
        bpMissing.length == 0 && !!bpBe && String(bpBe.getClass().getSimpleName()) == 'BackpackBlockEntity', bpMissing.join(', '))
    }
    // The Ascended Realm: the dimension loads, its islands are Realm Stone with Realm ores in them, the gate builds a
    // platform with a gate back, danger starts at realmBonus, and a round trip through the gates lands where it started
    let realmLevel = server.getLevel('factoryascension:ascended_realm')
    let realmCounts = {}
    if (realmLevel) {
      // Fresh spots each run, so this checks newly generated terrain; islands are scattered, so the best of three
      for (let attemptSpot = 0; attemptSpot < 3 && (realmCounts.realm_stone || 0) < 200; attemptSpot++) {
        realmCounts = {}
        let rbx = 16 * Math.floor(20000 + Math.random() * 40000), rbz = 16 * Math.floor(20000 + Math.random() * 40000)
        for (let rx = 0; rx < 48; rx += 2) for (let rz = 0; rz < 48; rz += 2) for (let ry = 0; ry < 256; ry += 2) {
          let rid = String(realmLevel.getBlock(rbx + rx, ry, rbz + rz).id)
          if (rid.indexOf('factoryascension:') == 0) realmCounts[rid.split(':')[1]] = (realmCounts[rid.split(':')[1]] || 0) + 1
        }
      }
    }
    let realmAt = realmLevel ? global.FA_realmPlatform(server, 4000, 4000) : null
    let realmGate = realmLevel && String(realmLevel.getBlock(4000, global.FA.realm.platformY, 4000 - global.FA.realm.platformRadius).id) == 'factoryascension:realm_gate'
    let realmUuid0 = String(player.stringUuid), realmSavedHome = global.FA_STATE.homes[realmUuid0] || false
    faPutHomeCenter(realmUuid0, faHomeCenter('minecraft:overworld', X, Z))
    let realmDanger = realmLevel ? global.FA_dangerLevel(realmLevel, 0, 0, player) : -1
    faPutHomeCenter(realmUuid0, realmSavedHome)
    // (the Realm end of the trip was forceloaded with the test area, so the stand can be found there)
    let realmStand = level.createEntity('minecraft:armor_stand')
    realmStand.setPosition(X + 0.5, Y + 3, Z + 0.5)
    realmStand.spawn()
    let realmUuid = realmStand.getUuid()
    let realmFind = () => { let a = server.overworld().getEntity(realmUuid); return a ? a : (realmLevel ? realmLevel.getEntity(realmUuid) : null) }
    let realmStart = ['minecraft:overworld', X, Z]
    let realmBefore = !!realmFind()
    global.FA_realmUse(realmStand, realmStand.blockPosition())
    let realmTraveller = realmFind()
    let realmIn = realmTraveller ? String(realmTraveller.level.dimension) : 'lost'
    if (realmTraveller) { realmTraveller.persistentData.putLong('fa_realm_last', -100000); global.FA_realmUse(realmTraveller, realmTraveller.blockPosition()) }
    realmTraveller = realmFind()
    let realmBack = realmTraveller ? [String(realmTraveller.level.dimension), Math.floor(realmTraveller.getX()), Math.floor(realmTraveller.getZ())] : ['lost']
    if (realmTraveller) realmTraveller.discard()
    server.runCommandSilent('execute in factoryascension:ascended_realm run forceload remove ' + X + ' ' + Z)
    let realmOres = ['essence_crystal_ore', 'voidsteel_ore', 'astral_ore'].filter(o => realmCounts[o] > 0)
    check('Ascended Realm: islands (' + Object.keys(realmCounts).map(k => realmCounts[k] + ' ' + k).join(', ') + '), a platform with a gate back, danger ' + realmDanger +
      ' at its center, and a round trip ' + realmStart.join(' ') + ' -> ' + realmIn + ' -> ' + realmBack.join(' '),
      !!realmLevel && (realmCounts.realm_stone || 0) > 50 && realmOres.length >= 1 && realmGate && realmDanger >= global.FA.danger.realmBonus &&
      realmIn == 'factoryascension:ascended_realm' && realmBack.join(' ') == realmStart.join(' '),
      [!!realmLevel, realmBefore, JSON.stringify(realmCounts), realmGate, realmDanger, realmIn, realmBack.join(' ')].join(' / '))
    // Structures: each one builds, its chests fill from their loot tables and its spawner holds the right mob
    let stChest = (lvl, x, y, z) => {
      let be = lvl.getBlockEntity(new $STBlockPos(x, y, z))
      if (!be || !be.unpackLootTable) return -1
      be.unpackLootTable(null)
      let n = 0
      for (let i = 0; i < be.getContainerSize(); i++) if (!be.getItem(i).isEmpty()) n++
      return n
    }
    let stSpawner = (lvl, x, y, z) => { let be = lvl.getBlockEntity(new $STBlockPos(x, y, z)); return be ? String(be.saveWithoutMetadata()) : '' }
    let stOut = {}
    // Factory Ruin on a stone pad in the middle of a chunk (structures stay inside their chunk; its chest is near the middle)
    let rx0 = X - 24, rz0 = Z + 24
    for (let dx = -6; dx <= 6; dx++) for (let dz = -6; dz <= 6; dz++) place(rx0 + dx, Y - 1, rz0 + dz, 'minecraft:stone')
    for (let dx = -5; dx <= 5; dx++) for (let dz = -5; dz <= 5; dz++) for (let dy = 0; dy <= 6; dy++) placed.push(new $STBlockPos(rx0 + dx, Y + dy, rz0 + dz))
    server.runCommandSilent('place feature factoryascension:factory_ruin ' + rx0 + ' ' + Y + ' ' + rz0)
    stOut.ruin = -1
    for (let dx = -3; dx <= 3; dx++) for (let dz = -3; dz <= 3; dz++)
      if (String(level.getBlock(rx0 + dx, Y, rz0 + dz).id) == 'minecraft:chest') stOut.ruin = stChest(level, rx0 + dx, Y, rz0 + dz)
    // Ascension Crypt below the floor, also mid-chunk
    let cx0 = X - 24, cz0 = Z - 24
    server.runCommandSilent('place feature factoryascension:ascension_crypt ' + cx0 + ' ' + (Y - 12) + ' ' + cz0)
    stOut.crypt = stChest(level, cx0 - 3, Y - 12, cz0) + stChest(level, cx0 + 3, Y - 12, cz0)
    stOut.cryptMob = /ascendite_(skeleton|husk)/.test(stSpawner(level, cx0, Y - 12, cz0))
    for (let dx = -4; dx <= 4; dx++) for (let dz = -4; dz <= 4; dz++) for (let dy = -1; dy <= 4; dy++) placed.push(new $STBlockPos(cx0 + dx, Y - 12 + dy, cz0 + dz))
    // Realm Vault (inside Realm Stone) and Sky Shrine (on Realm Moss), in the Realm's forceloaded chunk
    if (realmLevel) {
      let vx = X + 8, vz = Z + 8
      server.runCommandSilent('execute in factoryascension:ascended_realm run fill ' + (vx - 5) + ' 59 ' + (vz - 5) + ' ' + (vx + 5) + ' 66 ' + (vz + 5) + ' factoryascension:realm_stone')
      server.runCommandSilent('execute in factoryascension:ascended_realm run place feature factoryascension:realm_vault ' + vx + ' 60 ' + vz)
      stOut.vault = stChest(realmLevel, vx, 60, vz - 3) + stChest(realmLevel, vx, 60, vz + 3)
      stOut.vaultMob = /void_(stalker|archer)/.test(stSpawner(realmLevel, vx, 60, vz))
      server.runCommandSilent('execute in factoryascension:ascended_realm run fill ' + (vx - 4) + ' 89 ' + (vz - 4) + ' ' + (vx + 4) + ' 89 ' + (vz + 4) + ' factoryascension:realm_moss')
      server.runCommandSilent('execute in factoryascension:ascended_realm run place feature factoryascension:sky_shrine ' + vx + ' 90 ' + vz)
      stOut.shrine = stChest(realmLevel, vx, 90, vz)
    }
    check('Structures: Factory Ruin, Ascension Crypt, Realm Vault and Sky Shrine build, fill their chests and set their spawners (' + JSON.stringify(stOut) + ')',
      stOut.ruin > 0 && stOut.crypt > 1 && stOut.cryptMob && stOut.vault > 1 && stOut.vaultMob && stOut.shrine > 0, JSON.stringify(stOut))
    // The Realm's own mobs and bosses: they exist and the bosses get a boss bar
    let realmMobs = ['astral_wisp', 'void_knight', 'astral_sovereign', 'void_behemoth'].map(m => {
      let e = spawn('factoryascension:' + m, X + 6, Y, Z - 26)
      let ok = !!e && e.isAlive()
      if (e) e.discard()   // gone straight away, so they don't count toward the horde cap later
      return m + (ok ? '' : ' (missing)')
    })
    check('the Realm mobs spawn: ' + realmMobs.join(', '), realmMobs.every(m => m.indexOf('missing') < 0), realmMobs.join(', '))
    // Shotgun: a shot from the hip hits a zombie 4 blocks ahead and uses a shell; reloading takes shells from the inventory
    let $Shotgun = Java.loadClass('dev.factoryascension.item.ShotgunItem')
    let gunAt = [player.getX(), player.getY(), player.getZ(), player.yaw, player.pitch]
    player.setPositionAndRotation(X + 0.5, Y, Z - 30 + 0.5, -90, 0)
    player.setRotation(-90, 0)
    // The target 2.5 blocks along wherever the player looks, and a stone wall behind it for stray pellets
    let aimDir = player.getViewVector(1)
    let tx = X + 0.5 + aimDir.x() * 2.5, tz = Z - 30 + 0.5 + aimDir.z() * 2.5
    for (let w = -2; w <= 2; w++) for (let h = 0; h <= 3; h++) {
      let wx = Math.floor(X + 0.5 + aimDir.x() * 5 + (Math.abs(aimDir.z()) > 0.5 ? w : 0)), wz = Math.floor(Z - 30 + 0.5 + aimDir.z() * 5 + (Math.abs(aimDir.x()) > 0.5 ? w : 0))
      place(wx, Y + h, wz, 'minecraft:stone')
    }
    let target = spawn('minecraft:zombie', tx, Y, tz)
    ;['fa_danger', 'fa_elite', 'fa_boss'].forEach(k => target.getPersistentData().remove(k))
    target.setCustomName(null)
    target.setNoAi(true)
    target.setHealth(20)
    let gun = Item.of('factoryascension:shotgun')
    gun.getOrCreateTag().putInt('Shells', global.FA.guns.shotgun.capacity)
    player.setItemInHand('main_hand', gun)
    player.getCooldowns().removeCooldown(gun.getItem())
    let gunLook = player.getViewVector(1), gunEye = player.getEyePosition()
    let gunDiag = 'look ' + gunLook.x().toFixed(2) + ',' + gunLook.y().toFixed(2) + ',' + gunLook.z().toFixed(2) + ' eye ' + gunEye.x().toFixed(1) + ',' + gunEye.y().toFixed(1) + ',' + gunEye.z().toFixed(1) +
      ' zombie ' + target.getX().toFixed(1) + ',' + target.getY().toFixed(1) + ',' + target.getZ().toFixed(1) + ' ' + target.type
    $Shotgun.fire(player, false)
    // A hip shot's pellets spread at random: if a volley happens to miss, take up to two more
    for (let extra = 0; extra < 2 && $Shotgun.lastHits == 0 && target.isAlive(); extra++) {
      player.getCooldowns().removeCooldown(gun.getItem())
      $Shotgun.fire(player, false)
    }
    let gunHits = $Shotgun.lastHits
    let held = player.getMainHandItem()
    let shellsLeft = $Shotgun.shells(held)
    let hurt = !target.isAlive() || target.getHealth() < 20
    held.getOrCreateTag().putInt('Shells', 0)
    player.getInventory().setItem(22, Item.of('factoryascension:shotgun_shell', 3))
    for (let i = 0; i < 4; i++) $Shotgun.loadOne(player, held)
    let reloaded = $Shotgun.shells(held)
    let shellsInBag = player.getInventory().getItem(22).getCount()
    check('Shotgun: a hip shot hits the zombie ahead (' + gunHits + ' hit, ' + (target.isAlive() ? target.getHealth().toFixed(1) + ' health left' : 'dead') + '), uses a shell (' +
      shellsLeft + ' left), and reloading loads the 3 shells in the inventory (' + reloaded + ')',
      gunHits >= 1 && hurt && shellsLeft >= global.FA.guns.shotgun.capacity - 3 && shellsLeft <= global.FA.guns.shotgun.capacity - 1 && reloaded == 3 && shellsInBag == 0,
      [gunHits, hurt, shellsLeft, reloaded, shellsInBag, $Shotgun.lastDamage, $Shotgun.lastLanded, target.getHealth(), gunDiag].join(' / '))
    target.discard()
    player.getCooldowns().removeCooldown(gun.getItem())
    player.setItemInHand('main_hand', Item.of('minecraft:air'))
    player.getInventory().setItem(22, Item.of('minecraft:air'))
    player.setPositionAndRotation(gunAt[0], gunAt[1], gunAt[2], gunAt[3], gunAt[4])
    // Ascension Workbench: two halves; Apply puts an attachment on a sword, Star Up adds a Legendary star
    let benchPos = place(X + 26, Y, Z + 16, 'minecraft:air')
    level.getBlock(benchPos).set('factoryascension:ascension_workbench', { facing: 'north', main: 'true' })
    let benchSidePos = place(X + 25, Y, Z + 16, 'minecraft:air')
    level.getBlock(benchSidePos).set('factoryascension:ascension_workbench', { facing: 'north', main: 'false' })
    let bench = level.getBlockEntity(benchPos)
    bench.faItems().setStackInSlot(0, Item.of('minecraft:iron_sword'))
    bench.faItems().setStackInSlot(1, Item.of('kubejs:attachment_vampire_fangs'))
    bench.request('bench_apply', player)
    let benchSword = bench.faItems().getStackInSlot(0)
    let benchFanged = global.FA_attachIds(benchSword).indexOf('vampire_fangs') >= 0 && bench.faItems().getStackInSlot(1).isEmpty()
    global.FA.legendary.costs[0].forEach((c, i) => player.getInventory().setItem(30 + i, Item.of(c[0], c[1])))
    bench.request('bench_star', player)
    let benchStars = global.FA_legendOf(bench.faItems().getStackInSlot(0))
    bench.faItems().setStackInSlot(1, Item.of('kubejs:attachment_headlamp'))
    bench.request('bench_preview', null)
    let benchRefused = !bench.canApply() && String(bench.status()).length > 0
    level.getBlock(benchPos).set('minecraft:air')
    let benchSideGone = String(level.getBlock(benchSidePos).id) == 'minecraft:air'
    check('Ascension Workbench: Apply attaches Vampire Fangs, Star Up adds Legendary star ' + benchStars + ', a Headlamp is refused ("' + bench.status() + '"), and both halves go together',
      benchFanged && benchStars == 1 && benchRefused && benchSideGone, [benchFanged, benchStars, benchRefused, benchSideGone].join(' '))
    global.FA.legendary.costs[0].forEach((c, i) => player.getInventory().setItem(30 + i, Item.of('minecraft:air')))
    let recipes = ['ascension_vault', 'card_binder', 'essence_forge', 'pack_opener', 'salvager', 'tome_fusion_table', 'super_smelter', 'engine_core', 'engine_coil',
      'engine_gearbox', 'engine_burner', 'storage_terminal', 'storage_linker', 'ascension_workbench', 'mini_crusher', 'realm_gate', 'ascension_quarry', 'mob_grinder',
      'warp_hopper'].filter(b => !server.getRecipeManager().byKey('kubejs:factory_ascension/machine_' + b).isPresent())
      .concat(Object.keys(global.FA.machines.glass).map(g => 'glass_' + g).filter(r => !server.getRecipeManager().byKey('kubejs:factory_ascension/' + r).isPresent()))
    check('the mod blocks and its 4 glasses have recipes', recipes.length == 0, recipes.join(', '))
  })

  // ---- The mod's mobs, connected glass and creeper blasts (Full download only) ----
  if (Platform.isLoaded('factoryascension')) attempt('mod mobs', () => {
    let ids = Object.keys(global.FA.modMobs.minDanger), ok = [], bad = []
    ids.forEach((id, i) => {
      let e = spawn('factoryascension:' + id, X - 22 + (i % 8) * 2, Y, Z + 12 + Math.floor(i / 8) * 2)
      if (e && String(e.type) == 'factoryascension:' + id && e.getMaxHealth() > 0) ok.push(id)
      else bad.push(id)
    })
    check('all ' + ids.length + ' of the mod\'s mobs spawn with their stats (' + ok.length + ')', ok.length == ids.length, bad.join(', '))
    // Gone again before their hordes arrive (a crowd here would stop the horde test later on)
    spawned.forEach(e => { if (String(e.type).indexOf('factoryascension:') == 0 && !e.isRemoved()) e.discard() })
    // A boss: zone scaling only (no Elite, no Ascended boss on top), no horde of itself, and minions when it's hurt
    let boss = spawn('factoryascension:rust_colossus', X - 18, Y, Z - 20)
    let bd = boss.getPersistentData()
    check('a boss spawns huge (' + boss.getMaxHealth() + ' health), scaled by its zone but not made an Elite or Ascended boss',
      boss.getMaxHealth() >= 300 && bd.getInt('fa_danger') > 0 && !bd.getBoolean('fa_elite') && !bd.getBoolean('fa_boss'), bd.getInt('fa_danger'))
    boss.setHealth(boss.getMaxHealth() * 0.6)
    bossAt = { boss: boss }
    // A kill drops the mob's own part (forced chance)
    let cowFood = global.FA.mobFood.foods.filter(f => f.raw == 'copper_tongue')[0]
    let saved = cowFood.chance
    cowFood.chance = 1
    let cow = spawn('factoryascension:copper_cow', X - 22, Y, Z + 8)
    cow.attack(player.damageSources().playerAttack(player), 1e6)
    cowFood.chance = saved
    let near = level.getEntitiesOfClass($STItemEntity, cow.getBoundingBox().inflate(3)), got = []
    for (let i = 0; i < near.size(); i++) got.push(String(near.get(i).getItem().getId()))
    check('a Copper Cow drops its own part (' + got.join(', ') + ')', got.indexOf('kubejs:copper_tongue') >= 0 || got.indexOf('kubejs:grilled_copper_tongue') >= 0, got.join(', '))
    // A Frost Creeper's blast breaks nothing and leaves snow
    let fpos = place(X - 14, Y, Z + 8, 'minecraft:stone')
    let frost = spawn('factoryascension:frost_creeper', X - 14, Y + 1, Z + 10)
    level.explode(frost, X - 14 + 0.5, Y + 1, Z + 10.5, 3, $STBoom.MOB)
    let snow = 0
    for (let dx = -3; dx <= 3; dx++) for (let dz = -3; dz <= 3; dz++) if (String(level.getBlock(new $STBlockPos(X - 14 + dx, Y, Z + 10 + dz)).id) == 'minecraft:snow') snow++
    check('a Frost Creeper blast breaks no blocks and leaves snow (' + snow + ' patches)', String(level.getBlock(fpos).id) == 'minecraft:stone' && snow > 0, snow)
    for (let dx = -3; dx <= 3; dx++) for (let dz = -3; dz <= 3; dz++) {
      let b = level.getBlock(new $STBlockPos(X - 14 + dx, Y, Z + 10 + dz))
      if (String(b.id) == 'minecraft:snow') b.set('minecraft:air')
    }
    // Connected glass joins up with its neighbours
    let g1 = place(X - 20, Y + 3, Z + 4, 'factoryascension:ascendite_glass')
    place(X - 19, Y + 3, Z + 4, 'factoryascension:ascendite_glass')
    let props = level.getBlock(g1).getProperties()
    check('connected glass joins the glass next to it (east ' + props.get('east') + ', west ' + props.get('west') + ')', String(props.get('east')) == 'true' && String(props.get('west')) == 'false')
  })

  // ---- Gear match: Sharpness won't go on a helmet; a Chaos Tome on a helmet only rolls armor effects (real clicks) ----
  // Meteor Shower: every item in its pool is real, and a roll makes a real stack
  attempt('meteor shower', () => {
    let pool = global.FA_meteorPool()
    let bad = []
    pool.forEach(t => t.forEach(id => { if (typeof id == 'string' && Item.of(id).isEmpty()) bad.push(id) }))
    let rolls = 0
    for (let i = 0; i < 200; i++) { let r = global.FA_meteorRoll(Math.random, 17, pool); if (r.relic || !Item.of(r.id).isEmpty()) rolls++ }
    check('Meteor Shower: all ' + pool.reduce((a, t) => a + t.length, 0) + ' things it can drop exist, and 200 rolls far out all make real items',
      bad.length == 0 && rolls == 200, bad.slice(0, 6).join(', '))
  })

  attempt('gear match', () => {
    player.getInventory().clearContent()
    let helmet = Item.of('minecraft:iron_helmet')
    player.setItemInHand('main_hand', Item.of('kubejs:tome_sharpness'))
    player.setItemInHand('off_hand', helmet)
    faSetEssence(player, 100000)
    player.gameMode.useItem(player, level, player.getMainHandItem(), $STHand.MAIN_HAND)
    let kept = String(player.getMainHandItem().getId()) == 'kubejs:tome_sharpness' && faGetLevel(player.getOffhandItem(), 'sharpness') == 0
    check('Sharpness won\'t go on a helmet (real click; the tome is kept)', kept)
    let wrong = 0, rolls = 0
    for (let i = 0; i < 6; i++) {
      let h = Item.of('minecraft:diamond_helmet')
      player.setItemInHand('main_hand', Item.of('kubejs:chaos_tome'))
      player.setItemInHand('off_hand', h)
      player.gameMode.useItem(player, level, player.getMainHandItem(), $STHand.MAIN_HAND)
      faRolledSlots(player.getOffhandItem()).forEach(r => {
        rolls++
        global.FA_rollEnchant(r.seed).affixes.forEach(a => { if (!global.FA_typeFits(a.type, 'armor')) wrong++ })
      })
    }
    check('Chaos Tomes on helmets only roll armor effects (' + rolls + ' rolls, ' + wrong + ' wrong)', rolls >= 5 && wrong == 0, rolls + ' / ' + wrong)
    player.getInventory().clearContent()
  })

  // ---- Foils: a sheet applied with a real click, then the foiled tome applied with a real click ----
  attempt('foils', () => {
    player.getInventory().clearContent()
    let tome = Item.of('kubejs:tome_cleave')
    faSetLevel(tome, 'cleave', 40)
    player.setItemInHand('main_hand', Item.of('kubejs:foil_sheet_gold'))
    player.setItemInHand('off_hand', tome)
    player.gameMode.useItem(player, level, player.getMainHandItem(), $STHand.MAIN_HAND)
    let foiled = player.getOffhandItem()
    let cmd = foiled.nbt ? foiled.nbt.getInt('CustomModelData') : 0
    check('a Gold Foil Sheet foils a tome (real click; look number ' + cmd + ')', global.FA_foilOf(foiled) == 2 && cmd == 3, cmd)
    player.setItemInHand('main_hand', foiled)
    player.setItemInHand('off_hand', Item.of('minecraft:iron_axe'))
    faSetEssence(player, 1000)
    player.gameMode.useItem(player, level, player.getMainHandItem(), $STHand.MAIN_HAND)
    let got = faGetLevel(player.getOffhandItem(), 'cleave')
    check('the Gold-foiled level 40 tome puts Cleave ' + got + ' on an axe (x1.5)', got == 60, got)
    player.getInventory().clearContent()
    player.setItemInHand('main_hand', sword)
  })

  // ---- Jackpot chests and Triads ----
  attempt('jackpots', () => {
    let jackpots = 0, biggest = ''
    for (let i = 0; i < 600; i++) server.runCommandSilent('loot spawn ' + (X - 14) + ' ' + (Y + 2) + ' ' + (Z + 14) + ' loot minecraft:chests/simple_dungeon')
    let near = level.getEntitiesOfClass($STItemEntity, player.getBoundingBox().move(-14, 0, 14).inflate(8))
    for (let i = 0; i < near.size(); i++) {
      let st = near.get(i).getItem()
      let id = String(st.getId())
      if ((id == 'kubejs:enchantment_pack' && st.getCount() >= 10) || (id == 'kubejs:gear_cache' && st.getCount() >= 4) ||
        (id == 'kubejs:booster_box' && st.getCount() >= 2) || (id == 'kubejs:ascension_core' && st.getCount() >= 32)) {
        jackpots++
        biggest = st.getCount() + 'x ' + id.replace('kubejs:', '')
      }
      near.get(i).discard()
    }
    check('jackpot chests: ' + jackpots + ' huge stacks in 600 dungeon chests (e.g. ' + biggest + ')', jackpots > 0, jackpots)
    let triads = Object.keys(global.FA.enchants).filter(id => global.FA.enchants[id].triad)
    let missing = triads.filter(id => !server.getRecipeManager().byKey('kubejs:factory_ascension/tome_' + id).isPresent())
    check(triads.length + ' Triad enchants, each with a tome recipe', triads.length == 200 && missing.length == 0, missing.slice(0, 3).join(', '))
  })

  // ---- Selling, gear tiers, Waystones and blaze rod recipes ----
  attempt('selling', () => {
    player.getInventory().clearContent()
    player.setItemInHand('main_hand', Item.of('kubejs:ascended_soul', 3))
    faSell(player, 'cores')
    let inv = player.getInventory(), sing = 0, dense = 0, souls = 0
    for (let i = 0; i < inv.getContainerSize(); i++) {
      let st = inv.getItem(i)
      if (!st || st.isEmpty()) continue
      if (String(st.getId()) == 'kubejs:ascension_singularity') sing += st.getCount()
      if (String(st.getId()) == 'kubejs:dense_ascension_core') dense += st.getCount()
      if (String(st.getId()) == 'kubejs:ascended_soul') souls += st.getCount()
    }
    check('selling 3 Ascended Souls takes them and pays 2 Singularities + 2 Dense Cores', sing == 2 && dense == 2 && souls == 0, sing + ' / ' + dense + ' / ' + souls)
    player.getInventory().clearContent()
    player.setItemInHand('main_hand', sword)
  })
  attempt('gear tiers', () => {
    let d = t => Item.of('kubejs:' + t + '_chestplate').item.getDefense()
    let uses = t => Item.of('kubejs:' + t + '_pickaxe').getMaxDamage()
    check('new tiers: Voidsteel ' + uses('voidsteel') + ' / Celestium ' + uses('celestium') + ' / Ascendium ' + uses('ascendium') + ' uses (diamond 1561, netherite 2031)',
      uses('voidsteel') > 1561 && uses('voidsteel') < 2031 && uses('celestium') > 2031 && uses('ascendium') > uses('celestium') &&
      d('celestium') == global.FA.gearTiers.celestium.armor.protection[2], d('voidsteel') + ' / ' + d('celestium') + ' / ' + d('ascendium'))
    let r = ['starter_blaze_rod', 'voidsteel_ingot', 'celestium_ingot', 'ascendium_ingot', 'voidsteel_sword', 'ascendium_chestplate']
    if (Platform.isLoaded('waystones')) r = r.concat(['starter_warp_stone', 'starter_waystone', 'starter_warp_dust', 'starter_warp_scroll'])
    let missing = r.filter(x => !server.getRecipeManager().byKey('kubejs:factory_ascension/' + x).isPresent())
    check('tier, blaze rod' + (Platform.isLoaded('waystones') ? ' and Waystones' : '') + ' recipes are loaded', missing.length == 0, missing.join(', '))
  })

  // ---- Glowing ores and the movable safe zone ----
  attempt('glowing ores', () => {
    let G = global.FA.glowingOres.light
    let dpos = place(X + 12, Y, Z + 12, 'minecraft:diamond_ore')
    let apos = place(X + 12, Y, Z + 13, 'kubejs:ascendite_ore')
    let dl = level.getBlockState(dpos).getLightEmission(), al = level.getBlockState(apos).getLightEmission()
    check('ores glow in the dark (diamond ore ' + dl + ', Ascendite ore ' + al + ')', dl == G['minecraft:diamond_ore'] && al == G['kubejs:ascendite_ore'], dl + ' / ' + al)
  })
  attempt('room TNT', () => {
    // A solid block of stone with a chest and an iron ore inside, then a 16x16 Sandstone room carved into it (facing north)
    let cx = X, fy = Y + 20, cz = Z - 5
    server.runCommandSilent('fill ' + (cx - 9) + ' ' + fy + ' ' + (cz - 17) + ' ' + (cx + 8) + ' ' + (fy + 9) + ' ' + cz + ' minecraft:stone')
    let chestPos = new $STBlockPos(cx, fy + 2, cz - 8)
    level.getBlock(chestPos).set('minecraft:chest')
    level.getBlock(new $STBlockPos(cx + 3, fy + 3, cz - 5)).set('minecraft:iron_ore')
    let job = global.FA_carveRoomNow(level, '16', 'sandstone', cx, fy, cz, 0, -1)
    let id = (x, y, z) => String(level.getBlock(new $STBlockPos(x, y, z)).id)
    let inside = id(cx - 4, fy + 4, cz - 10), floor = id(cx - 4, fy, cz - 10), wall = id(cx - 9, fy + 4, cz - 10), door = id(cx, fy + 1, cz)
    let lights = 0
    for (let x = cx - 8; x <= cx + 7; x++) for (let z = cz - 16; z <= cz - 1; z++) if (id(x, fy + 9, z) == 'minecraft:glowstone') lights++
    let chestKept = id(cx, fy + 2, cz - 8) == 'minecraft:chest'
    check('Room TNT: 16x16 room carved (inside ' + inside + ', floor ' + floor + ', wall ' + wall + ', doorway ' + door + ', ' + lights + ' lights), the chest kept, the ore collected',
      inside == 'minecraft:air' && floor == 'minecraft:smooth_sandstone' && wall == 'minecraft:smooth_sandstone' && door == 'minecraft:air' && lights >= 4 &&
      chestKept && job.ores['minecraft:iron_ore'] == 1 && job.kept == 1, job.changed + ' changed, kept ' + job.kept)
    // Arming by hand, 40 blocks up (clear of the test site), with no fuse: the server builds it over the next ticks;
    // the last stage checks it and clears it away
    let jobs = global.FA_roomJobCount()
    let tnt = Item.of('kubejs:room_tnt_quartz_16', 2)
    let fuse = global.FA.roomCharges.fuseSeconds
    let px = player.getX(), py = player.getY(), pz = player.getZ()
    player.setPosition(X + 0.5, Y + 41, Z + 0.5)
    global.FA.roomCharges.fuseSeconds = 0
    let armed = global.FA_armRoom(player, level, new $STBlockPos(X, Y + 40, Z), tnt)
    global.FA.roomCharges.fuseSeconds = fuse
    let f = faFacing(player.yaw)
    armedRoom = global.FA_roomPlan('16', X, Y + 40, Z, f[0], f[1])
    player.setPosition(px, py, pz)
    check('arming a Room TNT uses one and lights the fuse', armed == 1 && global.FA_roomJobCount() == jobs + 1 && (player.isCreative() || tnt.getCount() == 1), armed + ' / ' + tnt.getCount())
    level.getBlock(chestPos).set('minecraft:air')
    server.runCommandSilent('fill ' + (cx - 9) + ' ' + fy + ' ' + (cz - 17) + ' ' + (cx + 8) + ' ' + (fy + 9) + ' ' + cz + ' minecraft:air')
  })
  attempt('skinwalker', () => {
    // A cow that isn't one: it looks normal, then (hit by the player) twists into its true form, checked at the end
    let cow = spawn('minecraft:cow', X + 18, Y, Z - 18)
    global.FA_makeSkinwalker(cow)
    let plain = !cow.hasCustomName() && !cow.glowing
    cow.attack(player.damageSources().playerAttack(player), 5)
    check('a skinwalker looks like a plain cow, and a hit makes it turn instead of hurting it', plain && cow.getHealth() == cow.getMaxHealth() && cow.isNoAi(),
      plain + ' ' + cow.getHealth())
    cow.discard()   // (its slow shapeshift stops when it's gone)
    // A second one turns at test speed, checked at the end. In its own walled pen, well away from the quarry's hole, the
    // Ultimate's whirlwind and the boss check (which clears every mob within 24 blocks of its boss)
    let swx = X + 2, swz = Z + 26
    for (let dx = -4; dx <= 4; dx++) for (let dz = -4; dz <= 4; dz++) for (let dy = -1; dy <= 4; dy++)
      place(swx + dx, Y + dy, swz + dz, (Math.abs(dx) == 4 || Math.abs(dz) == 4 || dy == 4 || dy == -1) ? 'minecraft:stone' : 'minecraft:air')
    let cow2 = spawn('minecraft:cow', swx + 0.5, Y, swz + 0.5)
    global.FA_makeSkinwalker(cow2)
    global.FA_shapeshift(cow2, player, true)
    skinwalkerAt = { x: swx, z: swz, cow: cow2 }
    // Stalking uses the mob's pathfinder: a pig can be sent walking toward the player
    let pig = spawn('minecraft:pig', X + 14, Y, Z - 14)
    let path = pig.getNavigation().moveTo(player, 1.0)
    check('a skinwalker can walk after a player (its pathfinder answers: ' + path + ')', path === true || path === false)
    pig.discard()
  })
  attempt('danger from home', () => {
    let D = global.FA.danger, uuid = String(player.stringUuid)
    let saved = global.FA_STATE.homes[uuid] || false
    D.centerOn = 'home'
    faPutHomeCenter(uuid, false)
    let none = global.FA_dangerLevel(level, X + 3000, Z, player)
    faPutHomeCenter(uuid, faHomeCenter('minecraft:overworld', X, Z))
    let near = global.FA_dangerLevel(level, X + 500, Z, player)
    let at1 = global.FA_dangerLevel(level, X + 1000, Z, player), at2 = global.FA_dangerLevel(level, X, Z + 2000, player)
    let m1 = global.FA_dangerMult(at1, false).health, m2 = global.FA_dangerMult(at2, false).health
    faPutHomeCenter(uuid, saved)
    D.centerOn = 'spawn'
    check('danger counts from your home: none before /home set, 500 blocks -> ' + near + ', 1,000 -> ' + at1 + ' (x' + m1.toFixed(1) + '), 2,000 -> ' + at2 + ' (x' + m2.toFixed(1) + ')',
      none == 0 && near == 0 && at1 == 1 && at2 == 5 && Math.abs(m1 - 1.5) < 0.01 && Math.abs(m2 - 4.5) < 0.05, none + ' ' + near + ' ' + at1 + ' ' + at2)
  })
  attempt('safe zone center', () => {
    let saved = global.FA_STATE.center
    faCenterCommand(player, 'set')
    let here = global.FA_dangerLevel(level, X, Z)
    faSetCenter(server, saved || false)
    check('/ascension center set makes where you stand safe (danger ' + here + ')', here == 0, here)
  })

  // ---- Rare variants: a spawn rolls one, Golden drops its loot, Blazing burns what it hits ----
  attempt('variants', () => {
    let golden = global.FA_variantType('golden')
    let saved = golden.chance
    golden.chance = 1
    let g = spawn('minecraft:pig', X + 8, Y, Z - 8)
    golden.chance = saved
    check('a new spawn can roll a variant (forced Golden): ' + (g.getCustomName() ? g.getCustomName().getString() : 'no name'),
      global.FA_variantOf(g) == 'golden' && g.isCustomNameVisible(), global.FA_variantOf(g))
    g.attack(player.damageSources().playerAttack(player), 1000)
    let got = []
    let near = level.getEntitiesOfClass($STItemEntity, g.getBoundingBox().inflate(3))
    for (let i = 0; i < near.size(); i++) {
      got.push(String(near.get(i).getItem().getId()))
      near.get(i).discard()
    }
    check('a Golden mob drops gold blocks, cores and more', got.indexOf('minecraft:gold_block') >= 0 && got.indexOf('kubejs:ascension_core') >= 0, got.join(', '))
    let blazer = spawn('minecraft:zombie', X + 8, Y, Z - 10)
    ;['fa_danger', 'fa_elite', 'fa_boss'].forEach(k => blazer.getPersistentData().remove(k))
    global.FA_makeVariant(blazer, 'blazing')
    let target = spawn('minecraft:pig', X + 8, Y, Z - 11)
    target.attack(blazer.damageSources().mobAttack(blazer), 1)
    check('a Blazing mob sets what it hits on fire', target.isOnFire())
  })

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
      // 100,000 blocks out a zombie has x200,000 health: hit it hard enough
      z.attack(player.damageSources().playerAttack(player), 1e12)
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
    // Quarry, Mob Grinder and Warp Hoppers (Full download): real rotation from Creative Motors, checked 100 ticks later
    if (Platform.isLoaded('factoryascension')) attempt('new machines', () => {
      let $Cfg = Java.loadClass('dev.factoryascension.FAConfig')
      let Q = global.FA.machines.quarry
      $Cfg.setQuarry(1, Q.work, Q.maxPerTick, Q.minRpm, Q.stress)    // a 3 x 3 hole, so it stays out of the other tests' way
      let qx = X + 20, qy = Y + 4, qz = Z - 20
      let quarryPos = place(qx, qy, qz, 'factoryascension:ascension_quarry')
      place(qx, qy + 1, qz, 'minecraft:chest')
      let motor1 = place(qx, qy, qz + 1, 'minecraft:air')
      level.getBlock(motor1).set('create:creative_motor', { facing: 'north' })
      let gx = X + 24, gz = Z - 20
      let grinderPos = place(gx, Y, gz, 'factoryascension:mob_grinder')
      place(gx, Y - 1, gz, 'minecraft:chest')
      let motor2 = place(gx, Y, gz + 1, 'minecraft:air')
      level.getBlock(motor2).set('create:creative_motor', { facing: 'north' })
      let pig = spawn('minecraft:pig', gx + 0.5, Y, gz - 2.5)
      ;['fa_danger', 'fa_elite', 'fa_boss'].forEach(k => pig.getPersistentData().remove(k))
      pig.setCustomName(null)
      pig.setHealth(2)
      let wA = place(X + 27, Y, Z - 24, 'factoryascension:warp_hopper')
      let wB = place(X + 29, Y + 1, Z - 24, 'factoryascension:warp_hopper')
      let wChest = place(X + 29, Y, Z - 24, 'minecraft:chest')
      after(5, () => attempt('warp hoppers', () => {
        let diamond = Item.of('minecraft:diamond').getItem()
        level.getBlockEntity(wA).setKey(diamond)
        level.getBlockEntity(wB).setKey(diamond)
        let left = level.getBlockEntity(wA).faInput().insertItem(0, Item.of('minecraft:diamond', 5), false)
        let got = level.getBlockEntity(wChest).getItem(0)
        check('Warp Hoppers: 5 diamonds into one come out in the chest under the other (' + got.getCount() + ' ' + got.getId() + ')',
          left.isEmpty() && String(got.getId()) == 'minecraft:diamond' && got.getCount() == 5, left.getCount() + ' left, ' + got.getCount() + ' ' + got.getId())
      }))
      after(100, () => attempt('quarry and grinder', () => {
        let q = level.getBlockEntity(quarryPos)
        let qChest = level.getBlockEntity(new $STBlockPos(qx, qy + 1, qz))
        let dug = []
        for (let i = 0; i < 27; i++) { let st = qChest.getItem(i); if (!st.isEmpty()) dug.push(st.getCount() + ' ' + st.getId()) }
        check('Ascension Quarry: a Creative Motor turns it and it digs (' + q.faMined() + ' blocks, now at y=' + q.faLayer() + '; chest: ' + dug.join(', ') + ')',
          q.faMined() > 0 && dug.length > 0, q.status().getString())
        let g = level.getBlockEntity(grinderPos)
        let gChest = level.getBlockEntity(new $STBlockPos(gx, Y - 1, gz))
        let loot = []
        for (let i = 0; i < 27; i++) { let st = gChest.getItem(i); if (!st.isEmpty()) loot.push(st.getCount() + ' ' + st.getId()) }
        for (let i = 0; i < 27; i++) { let st = g.faOutput().getStackInSlot(i); if (!st.isEmpty()) loot.push(st.getCount() + ' ' + st.getId() + ' (inside)') }
        check('Mob Grinder: a Creative Motor turns it and it grinds the pig in front (' + g.faKills() + ' kills; drops: ' + loot.join(', ') + ')',
          g.faKills() >= 1 && !pig.isAlive() && loot.length > 0, g.status().getString())
        $Cfg.setQuarry(Q.radius, Q.work, Q.maxPerTick, Q.minRpm, Q.stress)
        ;[motor1, motor2].forEach(m => level.getBlock(m).set('minecraft:air'))
      }))
    })
    // Tarkov inventory: items take their size in the grid, a full grid refuses what can't fit, weight slows you;
    // the flare gun loads and fires a green flare (an airdrop on its way), and a dropped crate lands as a loot chest
    if (Platform.isLoaded('factoryascension')) attempt('tarkov inventory', () => {
      let $Tarkov = Java.loadClass('dev.factoryascension.tarkov.TarkovServer')
      let $Layout = Java.loadClass('dev.factoryascension.tarkov.TarkovLayout')
      let $Cfg = Java.loadClass('dev.factoryascension.FAConfig')
      let inv = player.getInventory()
      let clear = () => { for (let i = 0; i < 36; i++) inv.setItem(i, Item.of('minecraft:air')) }
      let gridW = $Cfg.tarkovWidth, gridH = $Cfg.tarkovHeight
      $Cfg.setTarkov(true, 10, 6, true)
      clear()
      let layout = $Tarkov.layout(player)
      for (let s = 0; s < 27; s++) layout.pos[s] = -1
      inv.setItem(9, Item.of('minecraft:iron_chestplate'))
      inv.setItem(10, Item.of('minecraft:diamond_sword'))
      inv.setItem(11, Item.of('minecraft:cobblestone', 20))
      inv.setItem(12, Item.of('factoryascension:shotgun'))
      $Tarkov.update(player)
      let occ = layout.occupancy(inv.items), cells = 0
      for (let x = 0; x < 10; x++) for (let y = 0; y < 6; y++) if (occ[x][y] >= 0) cells++
      let placed = [0, 1, 2, 3].map(s => layout.pos[s] >= 0)
      check('Tarkov inventory: a chestplate (3x3), a sword (1x3), cobblestone (1x1) and the shotgun (5x2) all get their own spot in the grid (' + cells + ' cells taken)',
        placed.every(p => p) && cells == 9 + 3 + 1 + 10, placed.join(',') + ' / ' + cells)
      // Six chestplates fill 9 of the 10 columns: a sword still fits down the last one, another chestplate doesn't
      clear()
      for (let s = 0; s < 27; s++) layout.pos[s] = -1
      for (let i = 0; i < 9; i++) inv.setItem(i, Item.of('minecraft:stone', 64))
      for (let i = 0; i < 6; i++) inv.setItem(9 + i, Item.of('minecraft:iron_chestplate'))
      $Tarkov.update(player)
      let swordOk = $Tarkov.canTake(player, Item.of('minecraft:diamond_sword'))
      let chestOk = $Tarkov.canTake(player, Item.of('minecraft:diamond_chestplate'))
      check('Tarkov inventory: with 6 chestplates in, a sword still fits but a 7th chestplate is refused (21 empty slots, no 3x3 space)',
        swordOk && !chestOk, swordOk + ' / ' + chestOk)
      let WT = global.FA.inventory.weight
      $Cfg.setWeight(true, 1, 60, 0.01, 0.5)   // a 60 kg limit for the test, whatever the config says
      let speedAttr = player.getAttribute($STAttributes.MOVEMENT_SPEED)
      let kg = $Tarkov.weight(player)
      $Tarkov.applyWeight(player)
      let heavy = speedAttr.getValue(), base = speedAttr.getBaseValue()
      clear()
      $Tarkov.applyWeight(player)
      let light = speedAttr.getValue()
      check('Weight: 6 chestplates and 9 stacks of stone (' + kg.toFixed(1) + ' kg) slow you down (' + (100 * (1 - heavy / base)).toFixed(0) + '% slower), and back to normal when you drop them',
        kg > 60 && heavy < base && Math.abs(light - base) < 1e-6, [kg, heavy, base, light].join(' / '))
      $Cfg.setWeight(WT.enabled, WT.kgPerCell, WT.carryKg, WT.slowPerKg, WT.maxSlow)
      $Cfg.setTarkov(true, gridW, gridH, true)
      // Flare gun: R loads a green flare from the inventory; firing it calls an airdrop
      let $Flare = Java.loadClass('dev.factoryascension.item.FlareGunItem')
      let fgun = Item.of('factoryascension:flare_gun')
      player.setItemInHand('main_hand', fgun)
      inv.setItem(22, Item.of('factoryascension:flare_green', 1))
      let fheld = player.getMainHandItem()
      $Flare.reload(player, fheld)
      let loadedGreen = $Flare.loaded(fheld) == 2 && inv.getItem(22).isEmpty()
      let before = $Flare.pending()
      player.getCooldowns().removeCooldown(fheld.getItem())
      $Flare.fire(player)
      let called = $Flare.pending() == before + 1 && $Flare.loaded(player.getMainHandItem()) == 0
      $Flare.cancelAirdrops()
      player.setItemInHand('main_hand', Item.of('minecraft:air'))
      check('Flare gun: R loads the green flare from the inventory, firing it uses it and calls an airdrop', loadedGreen && called, loadedGreen + ' / ' + called)
      // The crate: dropped over a clear spot, it falls and lands as a chest with the airdrop loot
      let cx = X - 26, cz = Z + 26
      let crate = $Flare.dropCrate(level, cx, cz)
      after(120, () => attempt('airdrop check', () => {
        let gy = level.getHeight(Java.loadClass('net.minecraft.world.level.levelgen.Heightmap$Types').MOTION_BLOCKING_NO_LEAVES, cx, cz) - 1
        let pos = new $STBlockPos(cx, gy, cz)
        let be = level.getBlockEntity(pos)
        let table = be ? String(be.saveWithoutMetadata().getString('LootTable')) : ''
        let n = 0
        if (be && table) { be.unpackLootTable(null); for (let i = 0; i < 27; i++) if (!be.getItem(i).isEmpty()) n++ }
        check('Airdrop: the crate falls and lands as a chest filled from the airdrop loot (' + n + ' stacks)',
          !!crate && String(level.getBlock(pos).id) == 'minecraft:chest' && table == global.FA.guns.flareGun.airdropLoot && n > 0,
          (crate ? 'dropped' : 'no crate') + ' / ' + level.getBlock(pos).id + ' at y=' + gy + ' / ' + table + ' / ' + n)
        if (be) be.clearContent()
        level.getBlock(pos).set('minecraft:air')
      }))
    })
    // Guns: every kind of reload fills the gun from rounds in the inventory; fumbles cost time but the reload still
    // finishes; a shot hits and uses a round; round types do their thing (RIP bleeds, incendiary burns, snakeshot spreads)
    if (Platform.isLoaded('factoryascension')) attempt('guns', () => {
      let $Gun = Java.loadClass('dev.factoryascension.gun.GunItem')
      let $Cart = Java.loadClass('dev.factoryascension.gun.CartridgeItem')
      let $Cal = Java.loadClass('dev.factoryascension.gun.Caliber')
      let $Type = Java.loadClass('dev.factoryascension.gun.AmmoType')
      let inv = player.getInventory()
      let clearInv = () => { for (let i = 0; i < 36; i++) inv.setItem(i, Item.of('minecraft:air')) }
      let reloadFully = (gun) => {
        $Gun.startReload(player, gun)
        let n = 0
        while ($Gun.reloading(gun) && n < 2000) { $Gun.tickReload(player, gun); n++ }
        return n
      }
      let results = []
      $Gun.forceFumbleChance = 0
      ;[['m4a1', 'R556'], ['mosin', 'R762R'], ['r357', 'R357'], ['double_barrel', 'G12'], ['pkm', 'R762R'], ['saiga12', 'G12']].forEach(pair => {
        clearInv()
        let gun = Item.of('factoryascension:' + pair[0])
        player.setItemInHand('main_hand', gun)
        let held = player.getMainHandItem()
        inv.setItem(20, $Cart.standard($Cal[pair[1]], $Type.REGULAR, 64))
        inv.setItem(21, $Cart.standard($Cal[pair[1]], $Type.REGULAR, 64))
        let ticks = reloadFully(held)
        let mag = held.getItem().spec.mag
        let left = inv.getItem(20).getCount() + inv.getItem(21).getCount()
        results.push(pair[0] + ' ' + $Gun.ammo(held) + '/' + mag + ' in ' + ticks + 't, ' + (128 - left) + ' used')
        if ($Gun.ammo(held) != mag || 128 - left != mag) results.push('BAD')
      })
      check('Guns reload: magazine, bolt with clips, cylinder, break-action, belt and shotgun box mag all fill from the rounds you carry (' + results.join('; ') + ')',
        results.indexOf('BAD') < 0, results.join('; '))
      // Fumbles forced on: the reload still finishes (or leaves the round unchambered), and says what went wrong
      clearInv()
      player.setItemInHand('main_hand', Item.of('factoryascension:m4a1'))
      let fheld = player.getMainHandItem()
      inv.setItem(20, $Cart.standard($Cal.R556, $Type.REGULAR, 64))
      $Gun.forceFumbleChance = 1
      let fticks = reloadFully(fheld)
      $Gun.forceFumbleChance = 0
      let fumbles = []
      $Gun.lastFumbles.forEach(f => fumbles.push(String(f.name())))
      let noCh = fheld.nbt.getBoolean('NoCh')
      check('Reload fumbles: with every step going wrong, the M4 still reloads (' + fticks + ' ticks; ' + fumbles.join(', ') + (noCh ? '; not chambered' : '') + ')',
        fumbles.length >= 1 && $Gun.ammo(fheld) == 30 && !$Gun.reloading(fheld) && fticks < 600, fumbles.join(',') + ' ' + $Gun.ammo(fheld) + ' ' + fticks)
      // Firing at a zombie with special rounds
      let shootAt = (id, cal, type) => {
        clearInv()
        player.setPositionAndRotation(X + 0.5, Y, Z - 30 + 0.5, -90, 0)
        player.setRotation(-90, 0)
        let dir = player.getViewVector(1)
        let zx = X + 0.5 + dir.x() * 3, zz = Z - 30 + 0.5 + dir.z() * 3
        let z = spawn('minecraft:zombie', zx, Y, zz)
        ;['fa_danger', 'fa_elite', 'fa_boss'].forEach(k => z.getPersistentData().remove(k))
        z.setCustomName(null)
        z.setNoAi(true)
        z.getAttribute($STAttributes.MAX_HEALTH).setBaseValue(400)
        z.setHealth(400)
        player.setItemInHand('main_hand', Item.of('factoryascension:' + id))
        let g = player.getMainHandItem()
        inv.setItem(20, $Cart.standard($Cal[cal], $Type[type], 64))
        reloadFully(g)
        let before = $Gun.ammo(g)
        let hits = 0
        for (let i = 0; i < 4 && hits == 0; i++) {
          player.getCooldowns().removeCooldown(g.getItem())
          g.nbt.putInt('Bolt', 0)
          $Gun.fire(player, true)
          hits += $Gun.lastHits
        }
        let out = { hits: hits, used: before - $Gun.ammo(g), pellets: $Gun.lastPellets, wither: z.hasEffect($STEffects.WITHER), fire: z.getRemainingFireTicks() > 0,
          hp: z.getHealth(), result: String($Gun.lastResult) }
        z.discard()
        return out
      }
      let wall = place(X + 4, Y, Z - 30, 'minecraft:stone')
      for (let w = -1; w <= 1; w++) for (let h = 0; h <= 2; h++) place(X + 6, Y + h, Z - 30 + w, 'minecraft:stone')
      let fmj = shootAt('m4a1', 'R556', 'REGULAR')
      let rip = shootAt('ak74', 'R545', 'RIP')
      let inc = shootAt('akm', 'R762S', 'INCENDIARY')
      let snake = shootAt('p17', 'P9', 'SNAKESHOT')
      level.getBlock(wall).set('minecraft:air')
      check('Shooting: an M4 hits the zombie and uses a round (' + fmj.hits + ' hit, ' + fmj.used + ' used, ' + fmj.hp.toFixed(0) + ' health left)',
        fmj.hits >= 1 && fmj.used >= 1 && fmj.hp < 400, JSON.stringify(fmj))
      check('Round types: RIP makes it bleed, incendiary sets it on fire, snakeshot fires a cloud of ' + snake.pellets + ' pellets from a pistol',
        rip.wither && inc.fire && snake.pellets == 9, JSON.stringify([rip, inc, snake]))
      // Weapons Workbench: an attachment that fits goes on (and costs its parts), one that doesn't is refused,
      // a Jury-Rigged gun takes anything (three suppressors, a drum on a revolver), and the numbers change
      let $Bench = Java.loadClass('dev.factoryascension.gun.Bench')
      let $Atts = Java.loadClass('dev.factoryascension.gun.Attachments')
      let $Stats = Java.loadClass('dev.factoryascension.gun.GunStats')
      let pick = (family, slot) => { let found = null; $Atts.ALL.forEach(a => { if (!found && String(a.family) == family) found = a }); return found }
      clearInv()
      inv.setItem(20, Item.of('minecraft:iron_ingot', 64))
      inv.setItem(21, Item.of('kubejs:ascendite_ingot', 64))
      let m4 = Item.of('factoryascension:m4a1')
      let drum = pick('mag_drum'), choke = pick('choke'), supp = pick('suppressor_long')
      $Bench.install(player, m4, drum.id)
      let magAfter = $Stats.of(m4).mag
      let ironLeft = inv.getItem(20).getCount()
      $Bench.install(player, m4, choke.id)
      let afterChoke = $Stats.ids(m4).size()
      check('Weapons Workbench: a drum magazine goes on an M4 (' + magAfter + ' rounds, cost ' + (64 - ironLeft) + ' iron) and a shotgun choke is refused (' + afterChoke + ' on it)',
        magAfter == 75 && ironLeft < 64 && afterChoke == 1, magAfter + ' / ' + ironLeft + ' / ' + afterChoke)
      let rev = Item.of('factoryascension:r357').enchant('factoryascension:jury_rigged', 1)
      ;[supp, supp, supp, drum, pick('sniper_scope'), pick('auto_sear')].forEach(a => $Bench.install(player, rev, a.id))
      let rs = $Stats.of(rev)
      check('Jury-Rigged: a revolver takes three suppressors, a drum, a sniper scope and an auto sear (' + $Stats.ids(rev).size() + ' on, ' + rs.mag + ' rounds, full auto ' + rs.auto + ', loudness ' + rs.sound.toFixed(3) + ')',
        $Stats.ids(rev).size() == 6 && rs.auto && rs.mag == 15 && rs.sound < 0.05, $Stats.ids(rev).size() + ' ' + rs.mag + ' ' + rs.auto + ' ' + rs.sound)
      // Ammo Press: rounds from a slime ball case and sugar for powder, RIP type, pink
      let $Press = Java.loadClass('dev.factoryascension.gun.AmmoPress')
      let made = $Press.press($Cal.R556, $Type.RIP, Item.of('minecraft:sugar'), Item.of('minecraft:slime_ball'), 0xE060A0)
      let mnbt = made.nbt
      check('Ammo Press: sugar in a slime ball case makes ' + made.getCount() + ' pink RIP 5.56 rounds (power ' + mnbt.getFloat('Power').toFixed(2) + ', reliability ' + mnbt.getFloat('Rel').toFixed(2) + ')',
        made.getCount() == global.FA.guns.ammo.perCraft.r556 && String(mnbt.getString('Type')) == 'RIP' && mnbt.getInt('Color') == 0xE060A0 && mnbt.getFloat('Power') < 0.5 && mnbt.getFloat('Rel') < 0.7,
        String(mnbt))
      clearInv()
      player.setItemInHand('main_hand', Item.of('minecraft:air'))
    })
    // Safety net: an item flung at 200 blocks a tick is slowed before it can stall the server
    attempt('speed cap', () => {
      let fast = level.createEntity('minecraft:item')
      fast.setItem(Item.of('minecraft:stick'))
      fast.setPosition(X + 3, Y + 5, Z + 3)
      fast.spawn()
      fast.setMotion(200, 0, 0)
      after(3, () => attempt('speed cap check', () => {
        let m = fast.getDeltaMovement()
        check('safety net: an item flung at 200 blocks a tick is slowed (now ' + m.length().toFixed(2) + ')', !fast.isAlive() || m.length() <= 4.5, m.length())
        fast.discard()
      }))
    })
    // Leaks: saving the world over and over leaves no files or threads open, and Warp Hoppers leave the registry
    // of loaded hoppers when they're removed (a static list that only grew would be a memory leak)
    if (Platform.isLoaded('factoryascension')) attempt('leaks', () => {
      let $Health = Java.loadClass('dev.factoryascension.HealthMonitor')
      let $Warp = Java.loadClass('dev.factoryascension.machine.WarpHopperBlockEntity')
      let warp0 = $Warp.faLoadedCount()
      let wp = place(X - 2, Y + 6, Z - 2, 'factoryascension:warp_hopper')
      after(4, () => attempt('leaks check', () => {
        let warp1 = $Warp.faLoadedCount()
        level.getBlock(wp).set('minecraft:air')
        after(4, () => attempt('leaks check 2', () => {
          let warp2 = $Warp.faLoadedCount()
          check('no leaks: a Warp Hopper joins and leaves the loaded list (' + warp0 + ' -> ' + warp1 + ' -> ' + warp2 + ')',
            warp1 == warp0 + 1 && warp2 == warp0, [warp0, warp1, warp2].join(' / '))
          let rep = $Health.report()
          check('health monitor reports memory, files and threads (' + String(rep.get(1)) + ')', rep.size() == 3 && /threads: \d+/.test(String(rep.get(1))), String(rep.get(0)))
        }))
      }))
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
    // Leaks over the whole self-test (every system in the pack ran): open files and threads end where they started
    if (Platform.isLoaded('factoryascension')) attempt('leaks over the run', () => {
      let $Health = Java.loadClass('dev.factoryascension.HealthMonitor')
      let files1 = $Health.openFiles(), threads1 = $Health.threads()
      // Compared with where the last run ended: the first run after a restart opens world region files that the game
      // then keeps cached (not a leak); a real leak grows again on every run
      let prev = global.FA_stLeakPrev
      global.FA_stLeakPrev = { files: files1, threads: threads1 }
      if (!prev) check('leak baseline recorded (open files ' + files1 + ', threads ' + threads1 + '); run the self-test again to compare', true)
      else check('no leaks from one self-test run to the next: open files ' + prev.files + ' -> ' + files1 + ', threads ' + prev.threads + ' -> ' + threads1,
        (files1 < 0 || files1 - prev.files <= 6) && threads1 - prev.threads <= 4, [prev.files, files1, prev.threads, threads1].join(' / '))
    })
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

    // The hurt boss called its minions, and brought no horde of copies
    if (bossAt) attempt('boss minions', () => {
      let near = level.getEntitiesOfClass($STLiving, bossAt.boss.getBoundingBox().inflate(24)), minions = 0, copies = 0, extra = 0
      for (let i = 0; i < near.size(); i++) {
        let m = near.get(i)
        if (m.getPersistentData().getBoolean('fa_minion')) minions++
        else if (String(m.type) == 'factoryascension:rust_walker' || String(m.type) == 'factoryascension:deep_miner') extra++
        if (m !== bossAt.boss && String(m.type) == 'factoryascension:rust_colossus') copies++
        if (m !== player) m.discard()
      }
      check('a boss below 2/3 health calls its minions (' + minions + '), and neither it nor they bring hordes (' + copies + ' copies, ' + extra + ' extras)',
        minions >= 2 && minions <= 7 && copies == 0 && extra == 0)
      bossAt = false
    })

    // The skinwalker cow turned into a named Elite
    if (skinwalkerAt) attempt('skinwalker turned', () => {
      let near = level.getEntitiesOfClass($STLiving, new $STAABB(skinwalkerAt.x - 48, Y - 4, skinwalkerAt.z - 48, skinwalkerAt.x + 48, Y + 8, skinwalkerAt.z + 48))
      let form = null
      for (let i = 0; i < near.size(); i++) if (near.get(i).getPersistentData().getBoolean('fa_skinwalker_form')) form = near.get(i)
      let name = form && form.getCustomName() ? form.getCustomName().getString() : ''
      check('the skinwalker shapeshifted into ' + (form ? String(form.type) : 'nothing') + ': "' + name + '", an Elite', skinwalkerAt.cow.isRemoved() && form &&
        /Skinwalker/.test(name) && form.getPersistentData().getBoolean('fa_elite'), name)
      if (form) form.discard()
      skinwalkerAt = false
    })

    // The armed Room TNT built itself over the last ticks: quartz shell, lights in the roof, hollow inside
    if (armedRoom) attempt('room TNT built', () => {
      let kinds = { air: 0, shell: 0, light: 0 }, right = { air: 0, shell: 0, light: 0 }
      let want = { air: 'minecraft:air', shell: 'minecraft:smooth_quartz', light: 'minecraft:sea_lantern' }
      for (let i = 0; i < armedRoom.total; i++) {
        let sp = global.FA_roomSpot(armedRoom, i)
        if (!sp.kind) continue
        kinds[sp.kind]++
        if (String(level.getBlock(new $STBlockPos(sp.x, sp.y, sp.z)).id) == want[sp.kind]) right[sp.kind]++
      }
      check('an armed 16x16 Quartz Room TNT built itself: ' + right.air + '/' + kinds.air + ' hollow, ' + right.shell + '/' + kinds.shell + ' wall, ' + right.light + '/' + kinds.light + ' lights',
        right.air == kinds.air && right.shell == kinds.shell && right.light == kinds.light && global.FA_roomJobCount() == 0)
      for (let i = 0; i < armedRoom.total; i++) {
        let sp = global.FA_roomSpot(armedRoom, i)
        if (sp.kind) level.getBlock(new $STBlockPos(sp.x, sp.y, sp.z)).set('minecraft:air')
      }
      armedRoom = false
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

    global.FA.danger.centerOn = faSelftestCenterOn
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
