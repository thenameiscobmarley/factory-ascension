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
const $STHit = Java.loadClass('net.minecraft.world.phys.BlockHitResult')
const $STCompoundTag = Java.loadClass('net.minecraft.nbt.CompoundTag')
const $STSlot = Java.loadClass('net.minecraft.world.entity.EquipmentSlot')
const $STVec = Java.loadClass('net.minecraft.world.phys.Vec3')

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
    let recipeIds = ['core_by_hand', 'core_from_shards', 'core_mixing', 'core_compacting', 'starter_brass', 'starter_precision_mechanism', 'ascendite_pickaxe', 'press_cobblestone_ascendite']
    let missing = recipeIds.filter(r => !server.getRecipeManager().byKey('kubejs:factory_ascension/' + r).isPresent())
    check('the cheaper core recipes and starter recipes are loaded', missing.length == 0, missing.join(', '))
    // The ore feature is in the world's worldgen registry: place one vein into a block of stone and look for ore
    let sx = X + 10, sy = Y + 2, sz = Z - 14
    for (let dx = -3; dx <= 3; dx++) for (let dy = -3; dy <= 3; dy++) for (let dz = -3; dz <= 3; dz++) place(sx + dx, sy + dy, sz + dz, 'minecraft:stone')
    server.runCommandSilent('execute in minecraft:overworld run place feature kubejs:ore_ascendite ' + sx + ' ' + sy + ' ' + sz)
    let ores = 0
    for (let dx = -3; dx <= 3; dx++) for (let dy = -3; dy <= 3; dy++) for (let dz = -3; dz <= 3; dz++) {
      if (String(level.getBlock(new $STBlockPos(sx + dx, sy + dy, sz + dz)).id) == 'kubejs:ascendite_ore') ores++
    }
    check('Ascendite ore worldgen is loaded (a vein placed ' + ores + ' ore)', ores > 0, ores)
  })

  // ---- Mob food: a real pig drops parts, raw food makes you sick, cooking recipes are loaded ----
  attempt('mob food', () => {
    let foods = global.FA.mobFood.foods
    let saved = foods.map(fd => fd.chance)
    foods.forEach(fd => { fd.chance = 1 })
    let pig = spawn('minecraft:pig', X - 6, Y, Z + 8)
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
