// Factory Ascension - Essence bank, tomes and the /ascension upgrade menu.
//
//   1. Absorb Ascension Cores -> Essence bank (saved per player in the world's KubeJS data)
//   2. Tome in one hand + gear in the other, right-click the tome -> unlocks that enchant on the gear (uses the tome)
//   3. Hold the gear, type /ascension -> click [+1] [+10] [+100] [Max] to spend Essence on levels
//
// Enchant levels are stored in the item's NBT ("FactoryAscension"), so they stay on the item
// when it's dropped, stored or traded. Effects are in startup_scripts/20_fa_enchant_effects.js.

const FA_ITEM_TAG = 'FactoryAscension'
const FA_MENU_STEPS = [1, 10, 100, 1000]

const FA_MAX_ESSENCE = 1e300
const FA_MAX_LEVEL = 2147483647

// ---- Essence bank ----
// The bank lives in the world's KubeJS data. A backup copy rides in the player's own file
// (Forge's PlayerPersisted tag, which survives death), and is only used if the world copy is missing,
// e.g. when the KubeJS data file was lost or damaged. Bad values (NaN, negative, infinite) read as 0.

function faCleanEssence(value) {
  return typeof value == 'number' && isFinite(value) && value > 0 ? Math.min(value, FA_MAX_ESSENCE) : 0
}

function faBackup(player) {
  let data = player.getPersistentData()
  if (!data.contains('PlayerPersisted')) data.put('PlayerPersisted', data.getCompound('PlayerPersisted'))
  return data.getCompound('PlayerPersisted')
}

function faGetEssence(player) {
  let key = 'fa_essence_' + player.getStringUUID()
  let world = player.getServer().persistentData
  if (!world.contains(key)) {
    let backup = faBackup(player)
    if (!backup.contains('fa_essence')) return 0
    let restored = faCleanEssence(backup.getDouble('fa_essence'))
    world.putDouble(key, restored)
    console.warn('[Factory Ascension] restored ' + player.getScoreboardName() + "'s Essence (" + restored + ') from their player file')
    return restored
  }
  let raw = world.getDouble(key)
  let clean = faCleanEssence(raw)
  if (clean !== raw && raw !== 0) console.warn('[Factory Ascension] ' + player.getScoreboardName() + ' had a bad Essence value (' + raw + '), reset to ' + clean)
  return clean
}

function faSetEssence(player, value) {
  let clean = faCleanEssence(value)
  player.getServer().persistentData.putDouble('fa_essence_' + player.getStringUUID(), clean)
  let backup = faBackup(player)
  backup.putDouble('fa_essence', clean)
  player.getPersistentData().put('PlayerPersisted', backup)
}

// ---- Gear ----

// Gear = any single, unstackable item (tools, weapons, armor, shields, Tinkers tools, ...)
function faIsGear(stack) {
  return !stack.isEmpty() && stack.getMaxStackSize() == 1 && stack.getCount() == 1
}

// The item being enchanted: the main hand if it holds gear, otherwise the off hand
function faTarget(player) {
  let main = player.getMainHandItem()
  if (faIsGear(main)) return main
  let off = player.getOffhandItem()
  if (faIsGear(off)) return off
  return null
}

// Levels outside 0..max (only possible from hand-edited NBT) read as 0
function faGetLevel(stack, id) {
  let tag = stack.getTag()
  let level = tag && tag.contains(FA_ITEM_TAG) ? tag.getCompound(FA_ITEM_TAG).getInt(id) : 0
  return level > 0 && level <= FA_MAX_LEVEL ? level : 0
}

function faSetLevel(stack, id, level) {
  if (!(level >= 0 && level <= FA_MAX_LEVEL && level % 1 == 0)) throw 'bad level ' + level
  let tag = stack.getOrCreateTag()
  let data = tag.getCompound(FA_ITEM_TAG)
  data.putInt(id, level)
  tag.put(FA_ITEM_TAG, data)
  if (id == 'eternal') tag.putBoolean('Unbreakable', true)
}

function faMaxLevel(id) {
  let max = global.FA.enchants[id].maxLevel
  return max ? Math.min(max, FA_MAX_LEVEL) : FA_MAX_LEVEL
}

// All-or-nothing: write the new level, then the new Essence. If either write fails, put the
// item and the bank back exactly as they were, so a failure can never give free levels or eat Essence.
function faCommit(player, gear, id, oldLevel, newLevel, oldEssence, newEssence) {
  try {
    faSetLevel(gear, id, newLevel)
    faSetEssence(player, newEssence)
    return true
  } catch (err) {
    try {
      faSetLevel(gear, id, oldLevel)
      faSetEssence(player, oldEssence)
    } catch (err2) {
      console.error('[Factory Ascension] rollback failed: ' + err2)
    }
    console.error('[Factory Ascension] ' + id + ' change on ' + player.getScoreboardName() + "'s item failed and was undone: " + err)
    player.tell(Text.red('Something went wrong, nothing was changed. (See the server log.)'))
    return false
  }
}

// ---- Rolled (Chaos Tome) enchants ----
// Stored on the item as FactoryAscension.rolled: {"0": {seed, level}, "1": ..., "2": ...}

const FA_RARITY_TEXT = { gray: 'gray', aqua: 'aqua', light_purple: 'lightPurple', gold: 'gold', red: 'red' }

function faRolledSlots(stack) {
  let tag = stack.getTag()
  if (!tag || !tag.contains(FA_ITEM_TAG)) return []
  let data = tag.getCompound(FA_ITEM_TAG)
  if (!data.contains('rolled')) return []
  let rolled = data.getCompound('rolled')
  let out = []
  for (let i = 0; i < global.FA.chaos.maxPerItem; i++) {
    if (!rolled.contains(String(i))) continue
    let slot = rolled.getCompound(String(i))
    let level = slot.getInt('level')
    if (level > 0 && level <= FA_MAX_LEVEL) out.push({ slot: i, seed: slot.getInt('seed'), level: level })
  }
  return out
}

// Writes one rolled slot (seed + level). level 0 removes the slot.
function faSetRolled(stack, slotIndex, seed, level) {
  if (!(level >= 0 && level <= FA_MAX_LEVEL && level % 1 == 0)) throw 'bad level ' + level
  if (!(slotIndex >= 0 && slotIndex < global.FA.chaos.maxPerItem)) throw 'bad slot ' + slotIndex
  let tag = stack.getOrCreateTag()
  let data = tag.getCompound(FA_ITEM_TAG)
  let rolled = data.getCompound('rolled')
  if (level == 0) {
    rolled.remove(String(slotIndex))
  } else {
    let slot = rolled.getCompound(String(slotIndex))
    slot.putInt('seed', seed)
    slot.putInt('level', level)
    rolled.put(String(slotIndex), slot)
  }
  data.put('rolled', rolled)
  tag.put(FA_ITEM_TAG, data)
}

// All-or-nothing, like faCommit: the slot and the Essence change together or not at all
function faCommitRolled(player, gear, slot, oldSeed, oldLevel, newSeed, newLevel, oldEssence, newEssence) {
  try {
    faSetRolled(gear, slot, newSeed, newLevel)
    faSetEssence(player, newEssence)
    return true
  } catch (err) {
    try {
      faSetRolled(gear, slot, oldSeed, oldLevel)
      faSetEssence(player, oldEssence)
    } catch (err2) {
      console.error('[Factory Ascension] rollback failed: ' + err2)
    }
    console.error('[Factory Ascension] rolled enchant change on ' + player.getScoreboardName() + "'s item failed and was undone: " + err)
    player.tell(Text.red('Something went wrong, nothing was changed. (See the server log.)'))
    return false
  }
}

function faRollText(roll, level) {
  let R = global.FA.chaos.rarities[roll.rarity]
  return Text[FA_RARITY_TEXT[R.color] || 'white']('\u2742 ' + roll.name + ' ' + global.FA_num(level))
}

// ---- Feedback ----

function faSound(player, sound, pitch) {
  player.getServer().runCommandSilent('execute at ' + player.getScoreboardName() +
    ' run playsound ' + sound + ' player ' + player.getScoreboardName() + ' ~ ~ ~ 1 ' + pitch)
}

function faButton(label, command, hover) {
  return Text.green('[' + label + ']').clickRunCommand(command).hover(Text.gray(hover))
}

// The /ascension menu: Essence bank, and the target item's enchants with upgrade buttons
function faShowMenu(player) {
  let essence = faGetEssence(player)
  player.tell(Text.lightPurple('Essence: ').append(Text.white(global.FA_num(essence))).bold(true))

  let gear = faTarget(player)
  if (!gear) {
    player.tell(Text.gray('Hold a weapon, tool or armor piece to see its Ascended enchants.'))
    return 1
  }

  player.tell(Text.gold('').append(gear.getHoverName()).append(Text.gray(':')))
  let any = false
  Object.keys(global.FA.enchants).forEach(id => {
    let level = faGetLevel(gear, id)
    if (level <= 0) return
    any = true

    let e = global.FA.enchants[id]
    let line = (e.mythic ? Text.lightPurple(' \u2726 ') : Text.gold(' ')).append(Text.gold(e.name + ' ' + global.FA_num(level) + ' '))
      .append(Text.gray(global.FA_describe(id, level) + ' '))
    if (level < faMaxLevel(id)) {
      let cost = global.FA_cost(level, id)
      FA_MENU_STEPS.forEach(step => {
        line = line.append(faButton('+' + step, '/ascension upgrade ' + id + ' ' + step,
          'Add ' + step + ' level' + (step > 1 ? 's' : '') + ' (next costs ' + global.FA_num(cost) + ' Essence)')).append(Text.gray(' '))
      })
      line = line.append(faButton('Max', '/ascension upgrade ' + id + ' ' + global.FA.maxUpgradesPerCommand,
        'Spend as much Essence as possible'))
    } else {
      line = line.append(Text.darkGray('(max)'))
    }
    player.tell(line)
  })

  faRolledSlots(gear).forEach(r => {
    any = true
    let roll = global.FA_rollEnchant(r.seed)
    let R = global.FA.chaos.rarities[roll.rarity]
    let cost = global.FA_rollCost(r.level, roll.rarity)
    let line = Text.gray(' ').append(faRollText(roll, r.level)).append(Text.darkGray(' ' + R.name + ' '))
    FA_MENU_STEPS.forEach(step => {
      line = line.append(faButton('+' + step, '/ascension upgrade rolled ' + r.slot + ' ' + step,
        'Add ' + step + ' level' + (step > 1 ? 's' : '') + ' (next costs ' + global.FA_num(cost) + ' Essence)')).append(Text.gray(' '))
    })
    line = line.append(faButton('Max', '/ascension upgrade rolled ' + r.slot + ' ' + global.FA.maxUpgradesPerCommand, 'Spend as much Essence as possible'))
    player.tell(line)
    roll.affixes.forEach(def => player.tell(Text.darkGray('     ' + global.FA_describeDef(def, r.level))))
  })

  let souls = gear.getTag() ? gear.getTag().getCompound(FA_ITEM_TAG).getInt('souls') : 0
  if (souls > 0) player.tell(Text.lightPurple(' Souls: ' + global.FA_num(souls)))
  if (!any) player.tell(Text.gray(' No Ascended enchants yet. Hold a Tome in your other hand and right-click it.'))
  return 1
}

// /ascension danger: how dangerous it is right where you stand
function faShowDanger(player) {
  let d = global.FA.danger
  if (!d.enabled) {
    player.tell(Text.gray('Danger scaling is turned off.'))
    return 1
  }
  let danger = global.FA_dangerLevel(player.level(), player.getX(), player.getZ())
  let zone = global.FA_zone(danger)
  let m = global.FA_dangerMult(danger, false)
  let elite = Math.min(d.eliteChanceCap, d.eliteChancePerLevel * danger)
  player.tell(Text.gold(zone.name + '  ').append(Text.gray('Danger ' + danger)).bold(true))
  if (danger == 0) {
    player.tell(Text.gray(' Mobs are normal here. Danger starts ' + d.safeRadius + ' blocks from world spawn.'))
    return 1
  }
  player.tell(Text.gray(' Mobs: ').append(Text.red('x' + m.health.toFixed(1) + ' health, x' + m.damage.toFixed(1) + ' damage')))
  player.tell(Text.gray(' Rewards: ').append(Text.yellow('x' + m.loot.toFixed(1) + ' loot, x' + m.xp.toFixed(1) + ' XP')))
  player.tell(Text.gray(' Elites: ').append(Text.lightPurple(Math.round(elite * 100) + '% of mobs')))
  return 1
}

// ---- Actions ----

function faAbsorb(player, stack, id) {
  let count = player.isShiftKeyDown() ? stack.getCount() : 1
  if (stack.isEmpty() || count <= 0) return
  stack.shrink(count)
  try {
    let gained = count * global.FA.essenceValues[id]
    let total = faGetEssence(player) + gained
    faSetEssence(player, total)
    player.displayClientMessage(Text.lightPurple('+' + global.FA_num(gained) + ' Essence  ')
      .append(Text.gray('(bank: ' + global.FA_num(total) + ')')), true)
    faSound(player, 'minecraft:entity.experience_orb.pickup', 0.6)
  } catch (err) {
    // Hand the items back rather than lose them
    player.give(Item.of(id, count))
    console.error('[Factory Ascension] absorbing ' + id + ' failed, items returned: ' + err)
  }
}

function faUnlock(player, tome, id, gear) {
  let name = global.FA.enchants[id].name
  if (!faIsGear(gear)) {
    player.displayClientMessage(Text.red('Hold the gear you want to enchant in your other hand.'), true)
    return
  }
  if (faGetLevel(gear, id) > 0) {
    player.displayClientMessage(Text.yellow('That already has ' + name + '. Hold it and type /ascension to upgrade it.'), true)
    return
  }
  let cost = global.FA_cost(0, id)
  let essence = faGetEssence(player)
  if (essence < cost) {
    player.displayClientMessage(Text.red('Unlocking needs ' + global.FA_num(cost) + ' Essence. Absorb some Ascension Cores first.'), true)
    return
  }

  if (!faCommit(player, gear, id, 0, 1, essence, essence - cost)) return
  // The tome is only used up once the unlock has fully gone through
  tome.shrink(1)
  faSound(player, 'minecraft:block.enchantment_table.use', 1)
  player.tell(Text.gold(name + ' unlocked on ').append(gear.getHoverName()).append(Text.gold('! '))
    .append(faButton('Upgrade', '/ascension', 'Open the upgrade menu')))
}

function faUpgrade(player, id, amount) {
  let gear = faTarget(player)
  let name = global.FA.enchants[id].name
  if (!gear) {
    player.tell(Text.red('Hold the item you want to upgrade.'))
    return 0
  }
  let level = faGetLevel(gear, id)
  if (level <= 0) {
    player.tell(Text.red('This item has no ' + name + ' yet. Unlock it with a Tome of ' + name + ' first.'))
    return 0
  }

  let essence = faGetEssence(player)
  let max = faMaxLevel(id)
  let added = 0
  let spent = 0
  while (added < amount && level < max) {
    let cost = global.FA_cost(level, id)
    if (essence - spent < cost) break
    spent += cost
    level++
    added++
  }

  if (added == 0) {
    player.tell(level >= max
      ? Text.yellow(name + ' is already at its max level.')
      : Text.red('Not enough Essence: the next level costs ' + global.FA_num(global.FA_cost(level, id)) + '.'))
    return 0
  }

  if (!faCommit(player, gear, id, level - added, level, essence, essence - spent)) return 0
  faSound(player, 'minecraft:block.enchantment_table.use', 1.4)
  player.tell(Text.green('+' + global.FA_num(added) + ' ' + name + ' -> ' + global.FA_num(level) + '  ')
    .append(Text.gray('(' + global.FA_describe(id, level) + ', spent ' + global.FA_num(spent) + ' Essence)')))
  return 1
}

// Chaos Tome: roll a random enchant onto the gear in your other hand.
// On a full item, sneak-use rerolls its newest rolled enchant (back to level 1).
function faChaosRoll(player, tome, gear) {
  if (!faIsGear(gear)) {
    player.displayClientMessage(Text.red('Hold the gear you want to roll onto in your other hand.'), true)
    return
  }
  let slots = faRolledSlots(gear)
  let max = global.FA.chaos.maxPerItem
  let slot
  let old = { seed: 0, level: 0 }
  if (slots.length < max) {
    for (slot = 0; slot < max; slot++) if (!slots.some(r => r.slot == slot)) break
  } else if (player.isShiftKeyDown()) {
    old = slots[slots.length - 1]
    slot = old.slot
  } else {
    player.displayClientMessage(Text.yellow('This item already holds ' + max + ' rolled enchants. Sneak + right-click to reroll the newest one.'), true)
    return
  }
  let cost = global.FA_rollCost(0, 0)
  let essence = faGetEssence(player)
  if (essence < cost) {
    player.displayClientMessage(Text.red('Rolling needs ' + global.FA_num(cost) + ' Essence. Absorb some Ascension Cores first.'), true)
    return
  }

  let seed = Math.floor(Math.random() * 2147483646) + 1
  if (!faCommitRolled(player, gear, slot, old.seed, old.level, seed, 1, essence, essence - cost)) return
  tome.shrink(1)

  let roll = global.FA_rollEnchant(seed)
  let R = global.FA.chaos.rarities[roll.rarity]
  let name = player.getScoreboardName()
  let server = player.getServer()
  server.runCommandSilent('title ' + name + ' times 5 50 15')
  server.runCommandSilent('title ' + name + ' subtitle ' + JSON.stringify({ text: roll.name, color: R.color }))
  server.runCommandSilent('title ' + name + ' title ' + JSON.stringify({ text: R.name.toUpperCase(), color: R.color, bold: true }))
  faSound(player, roll.rarity >= 3 ? 'minecraft:ui.toast.challenge_complete' : roll.rarity >= 1 ? 'minecraft:entity.player.levelup' : 'minecraft:block.enchantment_table.use', 1)
  let line = Text.gray(old.level ? 'Rerolled into ' : 'Rolled ').append(faRollText(roll, 1)).append(Text.gray(' (' + R.name + ')'))
  player.tell(line)
  roll.affixes.forEach(def => player.tell(Text.darkGray('   ' + global.FA_describeDef(def, 1))))
  if (roll.rarity >= 3) server.tell(Text.gold(name + ' rolled a ' + R.name + ' enchant: ').append(faRollText(roll, 1)))
}

function faUpgradeRolled(player, slotIndex, amount) {
  let gear = faTarget(player)
  if (!gear) {
    player.tell(Text.red('Hold the item you want to upgrade.'))
    return 0
  }
  let r = faRolledSlots(gear).filter(x => x.slot == slotIndex)[0]
  if (!r) {
    player.tell(Text.red('This item has no rolled enchant in slot ' + slotIndex + '.'))
    return 0
  }
  let roll = global.FA_rollEnchant(r.seed)
  let essence = faGetEssence(player)
  let level = r.level
  let added = 0
  let spent = 0
  while (added < amount && level < FA_MAX_LEVEL) {
    let cost = global.FA_rollCost(level, roll.rarity)
    if (essence - spent < cost) break
    spent += cost
    level++
    added++
  }
  if (added == 0) {
    player.tell(Text.red('Not enough Essence: the next level costs ' + global.FA_num(global.FA_rollCost(level, roll.rarity)) + '.'))
    return 0
  }
  if (!faCommitRolled(player, gear, slotIndex, r.seed, r.level, r.seed, level, essence, essence - spent)) return 0
  faSound(player, 'minecraft:block.enchantment_table.use', 1.4)
  player.tell(Text.green('+' + global.FA_num(added) + ' ').append(faRollText(roll, level)).append(Text.gray('  (spent ' + global.FA_num(spent) + ' Essence)')))
  return 1
}

// ---- Events ----

Object.keys(global.FA.essenceValues).forEach(id => {
  ItemEvents.rightClicked(id, event => {
    faAbsorb(event.player, event.item, id)
    event.cancel()
  })
})

Object.keys(global.FA.enchants).forEach(id => {
  ItemEvents.rightClicked('kubejs:tome_' + id, event => {
    // The tome enchants whatever is in the other hand
    let player = event.player
    let gear = String(event.hand) == 'MAIN_HAND' ? player.getOffhandItem() : player.getMainHandItem()
    faUnlock(player, event.item, id, gear)
    event.cancel()
  })
})

ItemEvents.rightClicked('kubejs:chaos_tome', event => {
  let player = event.player
  let gear = String(event.hand) == 'MAIN_HAND' ? player.getOffhandItem() : player.getMainHandItem()
  faChaosRoll(player, event.item, gear)
  event.cancel()
})

ServerEvents.commandRegistry(event => {
  let { commands: Commands, arguments: Arguments } = event

  let upgrade = Commands.literal('upgrade')
  Object.keys(global.FA.enchants).forEach(id => {
    upgrade = upgrade.then(Commands.literal(id)
      .then(Commands.argument('amount', Arguments.INTEGER.create(event))
        .executes(ctx => faUpgrade(ctx.source.getPlayerOrException(), id,
          Math.max(1, Math.min(global.FA.maxUpgradesPerCommand, Arguments.INTEGER.getResult(ctx, 'amount')))))))
  })

  upgrade = upgrade.then(Commands.literal('rolled')
    .then(Commands.argument('slot', Arguments.INTEGER.create(event))
      .then(Commands.argument('amount', Arguments.INTEGER.create(event))
        .executes(ctx => faUpgradeRolled(ctx.source.getPlayerOrException(), Arguments.INTEGER.getResult(ctx, 'slot'),
          Math.max(1, Math.min(global.FA.maxUpgradesPerCommand, Arguments.INTEGER.getResult(ctx, 'amount'))))))))

  let essenceCmd = (name, apply) => Commands.literal(name)
    .then(Commands.argument('amount', Arguments.INTEGER.create(event))
      .executes(ctx => {
        let player = ctx.source.getPlayerOrException()
        faSetEssence(player, apply(faGetEssence(player), Arguments.INTEGER.getResult(ctx, 'amount')))
        return faShowMenu(player)
      }))

  event.register(Commands.literal('ascension')
    .executes(ctx => faShowMenu(ctx.source.getPlayerOrException()))
    .then(upgrade)
    .then(Commands.literal('danger').executes(ctx => faShowDanger(ctx.source.getPlayerOrException())))
    // Op-only, for testing and server admins
    .then(Commands.literal('essence')
      .requires(src => src.hasPermission(2))
      .then(essenceCmd('add', (old, n) => old + n))
      .then(essenceCmd('set', (old, n) => n))))
})
