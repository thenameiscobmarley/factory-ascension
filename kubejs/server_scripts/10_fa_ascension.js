// priority: 900
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
  let key = 'fa_essence_' + player.stringUuid
  let world = player.getServer().persistentData
  if (!world.contains(key)) {
    let backup = faBackup(player)
    if (!backup.contains('fa_essence')) return 0
    let restored = faCleanEssence(backup.getDouble('fa_essence'))
    world.putDouble(key, restored)
    console.warn('[Factory Ascension] restored ' + player.username + "'s Essence (" + restored + ') from their player file')
    return restored
  }
  let raw = world.getDouble(key)
  let clean = faCleanEssence(raw)
  if (clean !== raw && raw !== 0) console.warn('[Factory Ascension] ' + player.username + ' had a bad Essence value (' + raw + '), reset to ' + clean)
  return clean
}

function faSetEssence(player, value) {
  let clean = faCleanEssence(value)
  player.getServer().persistentData.putDouble('fa_essence_' + player.stringUuid, clean)
  let backup = faBackup(player)
  backup.putDouble('fa_essence', clean)
  player.getPersistentData().put('PlayerPersisted', backup)
}

// ---- Gear ----

// Gear = any single, unstackable item (tools, weapons, armor, shields, Tinkers tools, ...)
// (Empty slots come back as undefined in KubeJS, so check that first)
const $AscArmorItem = Java.loadClass('net.minecraft.world.item.ArmorItem')
const $AscDiggerItem = Java.loadClass('net.minecraft.world.item.DiggerItem')
const $AscAxeItem = Java.loadClass('net.minecraft.world.item.AxeItem')

// 'armor', 'tool', 'axe' (weapon and tool) or 'weapon' (anything else: swords, bows, tridents...)
function faGearKind(stack) {
  let item = stack.getItem()
  if (item instanceof $AscArmorItem) return 'armor'
  if (item instanceof $AscAxeItem) return 'axe'
  if (item instanceof $AscDiggerItem) return 'tool'
  return 'weapon'
}
const FA_KIND_NAMES = { armor: 'armor', axe: 'an axe', tool: 'a tool', weapon: 'a weapon' }

function faIsGear(stack) {
  return !!stack && !stack.isEmpty() && stack.getMaxStackSize() == 1 && stack.getCount() == 1
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
  let tag = stack.nbt
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

// ---- Tomes carry a level ----
// A tome stores its level the same way gear does (FactoryAscension.<enchant>); a plain tome is level 1.
// Level a tome on its own with /ascension, then apply it to gear; or extract an enchant from gear into a tome.

// The enchant id of a tome stack, or null
function faTomeId(stack) {
  if (!stack || stack.isEmpty()) return null
  let item = String(stack.getId())
  if (item.indexOf('kubejs:tome_') != 0) return null
  let id = item.slice('kubejs:tome_'.length)
  return global.FA.enchants[id] ? id : null
}

function faTomeLevel(tome, id) {
  return Math.max(1, faGetLevel(tome, id))
}

// The level a tome puts on gear: its own level x its foil (if any), within the enchant's max
function faAppliedLevel(tome, id) {
  let level = faTomeLevel(tome, id)
  let foil = global.FA_foilOf(tome)
  if (foil >= 0) level = Math.floor(level * global.FA.foils.types[foil].mult)
  return Math.max(1, Math.min(faMaxLevel(id), level))
}

// A tome item holding one enchant at a level
function faMakeTome(id, level) {
  let tome = Item.of('kubejs:tome_' + id)
  if (level > 1) faSetLevel(tome, id, level)
  return tome
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
    console.error('[Factory Ascension] ' + id + ' change on ' + player.username + "'s item failed and was undone: " + err)
    player.tell(Text.red('Something went wrong, nothing was changed. (See the server log.)'))
    return false
  }
}

// ---- Rolled (Chaos Tome) enchants ----
// Stored on the item as FactoryAscension.rolled: {"0": {seed, level}, "1": ..., "2": ...}

const FA_RARITY_TEXT = { gray: 'gray', aqua: 'aqua', light_purple: 'lightPurple', gold: 'gold', red: 'red' }

function faRolledSlots(stack) {
  let tag = stack.nbt
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
    console.error('[Factory Ascension] rolled enchant change on ' + player.username + "'s item failed and was undone: " + err)
    player.tell(Text.red('Something went wrong, nothing was changed. (See the server log.)'))
    return false
  }
}

function faRollText(roll, level) {
  let R = global.FA.chaos.rarities[roll.rarity]
  return Text[FA_RARITY_TEXT[R.color] || 'white']('\u2742 ' + roll.name + ' ' + global.FA_num(level))
}

// Stars and gems are read in 40_fa_progress.js; these wrappers keep the menu working if that file is missing
function faStarsOf(stack) { return typeof faStars == 'function' ? faStars(stack) : 0 }
function faGemsOf(stack) { return typeof faGems == 'function' ? faGems(stack) : [] }

// ---- Feedback ----

function faSound(player, sound, pitch) {
  player.getServer().runCommandSilent('execute at ' + player.username +
    ' run playsound ' + sound + ' player ' + player.username + ' ~ ~ ~ 1 ' + pitch)
}

function faButton(label, command, hover) {
  return Text.green('[' + label + ']').clickRunCommand(command).hover(Text.gray(hover))
}

// The /ascension menu: Essence bank, and the target item's enchants with upgrade buttons
function faShowMenu(player) {
  let essence = faGetEssence(player)
  player.tell(Text.lightPurple('Essence: ').append(Text.white(global.FA_num(essence))).bold(true)
    .append(Text.darkGray('   ')).append(Text.gold('[Sell held item]').clickRunCommand('/ascension sell').hover(Text.gray('See what the item in your hand is worth'))))

  // A tome in your main hand: level the tome itself
  let held = player.getMainHandItem()
  let tomeId = faTomeId(held)
  if (tomeId) {
    let e = global.FA.enchants[tomeId]
    let tl = faTomeLevel(held, tomeId)
    let line = Text.gold('Tome of ' + e.name + ' ' + global.FA_num(tl) + ' ').append(Text.gray(global.FA_describe(tomeId, tl) + ' '))
    if (held.getCount() != 1) {
      line = line.append(Text.red('(hold just one to level it)'))
    } else if (tl < faMaxLevel(tomeId)) {
      let cost = global.FA_cost(tl, tomeId)
      FA_MENU_STEPS.forEach(step => {
        line = line.append(faButton('+' + step, '/ascension tome ' + step, 'Add ' + step + ' level' + (step > 1 ? 's' : '') + ' to the tome (next costs ' + global.FA_num(cost) + ' Essence)')).append(Text.gray(' '))
      })
      line = line.append(faButton('Max', '/ascension tome ' + global.FA.maxUpgradesPerCommand, 'Spend as much Essence as possible on the tome'))
    } else {
      line = line.append(Text.darkGray('(max)'))
    }
    player.tell(line)
    player.tell(Text.darkGray(' Apply it: hold gear in your other hand and right-click the tome. Gear with a lower level swaps it out.'))
  }

  let gear = faTarget(player)
  if (!gear) {
    if (!tomeId) player.tell(Text.gray('Hold a weapon, tool or armor piece to see its Ascended enchants, or a tome to level it.'))
    return 1
  }

  let starCount = faStarsOf(gear)
  player.tell(Text.gold('').append(gear.getHoverName()).append(Text.yellow(starCount ? ' ' + '\u2605'.repeat(Math.min(starCount, 10)) + (starCount > 10 ? ' x' + starCount : '') : ''))
    .append(Text.gray(':')))
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
    line = line.append(Text.gray(' ')).append(Text.gold('[Extract]').clickRunCommand('/ascension extract ' + id)
      .hover(Text.gray('Pull ' + e.name + ' ' + global.FA_num(level) + ' off into a tome (needs a Blank Ascension Tome in your other hand)')))
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

  let souls = gear.nbt ? gear.nbt.getCompound(FA_ITEM_TAG).getInt('souls') : 0
  if (souls > 0) player.tell(Text.lightPurple(' Souls: ' + global.FA_num(souls)))
  faGemsOf(gear).forEach(g => {
    let G = global.FA.gems
    player.tell(Text.aqua(' \u25c6 ' + G.tiers[g.tier - 1].name + ' ' + G.types[g.type].name).append(Text.darkGray('  +' + global.FA_gemLevels(g.tier) + ' ' + global.FA.enchants[G.types[g.type].enchant].name)))
  })
  // Attachments, with a button to take each one off
  let attach = gear.nbt ? gear.nbt.getCompound(FA_ITEM_TAG).getCompound('attach') : null
  if (attach) attach.getAllKeys().forEach(k => {
    let id = String(attach.getString(k))
    player.tell(Text.gray(' \u2699 ' + global.FA_attachmentText(id) + ' ').append(Text.gold('[Detach]').clickRunCommand('/ascension detach ' + k)
      .hover(Text.gray('Take it off and get the attachment back'))))
  })
  if (any) player.tell(Text.darkGray(' Rebirth at ' + global.FA_num(global.FA_rebirthNeed(starCount)) + ' total levels: ').append(faButton('Rebirth', '/ascension rebirth', 'Reset to level 1 for a permanent star')))
  let AW = global.FA.artifacts
  if (any && AW.enabled && !global.FA_isArtifact(gear)) {
    let total = faTotalLevels(gear)
    player.tell(total >= AW.awakenLevels
      ? Text.lightPurple(' \u2736 It can be Awakened into a one-of-one Artifact: ').append(Text.gold('[Awaken]').clickRunCommand('/ascension awaken')
        .hover(Text.gray(global.FA_num(AW.awakenEssence) + ' Essence + ' + AW.awakenSouls + ' Ascended Soul')))
      : Text.darkGray(' Awakening into an Artifact needs ' + global.FA_num(AW.awakenLevels) + ' total levels (' + global.FA_num(total) + ' now)'))
  }
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
  let danger = global.FA_dangerLevel(player.level, player.getX(), player.getZ(), player)
  let zone = global.FA_zone(danger)
  let m = global.FA_dangerMult(danger, false)
  let elite = Math.min(d.eliteChanceCap, d.eliteChancePerLevel * danger)
  player.tell(Text.gold(zone.name + '  ').append(Text.gray('Danger ' + danger)).bold(true))
  if (danger == 0) {
    if (d.centerOn == 'home' && !global.FA_STATE.homes[String(player.stringUuid)]) {
      player.tell(Text.gray(' Mobs are normal everywhere until you set a home: explore, find a spot, then ').append(Text.white(global.FA.home.command ? '/home set' : '/ascension home set')).append(Text.gray('.')))
    } else {
      player.tell(Text.gray(' Mobs are normal here. Danger starts ' + global.FA_num(d.safeRadius) + ' blocks from ' + (d.centerOn == 'home' ? 'your home.' : 'world spawn.')))
    }
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
    if (global.FA_onStat) global.FA_onStat(player, 'essence_absorbed', gained)
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
  if (!global.FA_enchantFits(id, faGearKind(gear))) {
    player.displayClientMessage(Text.red(name + ' doesn\'t suit ' + FA_KIND_NAMES[faGearKind(gear)] + ': it goes on ' + global.FA_fitsText(id) + '.'), true)
    return
  }
  let tomeLevel = faAppliedLevel(tome, id)
  let had = faGetLevel(gear, id)
  if (had >= tomeLevel) {
    player.displayClientMessage(Text.yellow('That already has ' + name + ' ' + global.FA_num(had) + '. Level this tome past it first (hold it, /ascension).'), true)
    return
  }
  let cost = global.FA_cost(0, id)
  let essence = faGetEssence(player)
  if (essence < cost) {
    player.displayClientMessage(Text.red('Unlocking needs ' + global.FA_num(cost) + ' Essence. Absorb some Ascension Cores first.'), true)
    return
  }

  if (!faCommit(player, gear, id, had, tomeLevel, essence, essence - cost)) return
  // The tome is only used up once the unlock has fully gone through
  tome.shrink(1)
  // Swapping a higher tome onto gear that had a lower level: the old level comes back as a tome, nothing is lost
  if (had > 0) player.give(faMakeTome(id, had))
  if (global.FA_onStat) global.FA_onStat(player, 'unlocks', 1)
  faSound(player, 'minecraft:block.enchantment_table.use', tomeLevel > 1 ? 0.7 : 1)
  player.tell(Text.gold(name + (tomeLevel > 1 ? ' ' + global.FA_num(tomeLevel) : '') + (had > 0 ? ' swapped onto ' : ' unlocked on ')).append(gear.getHoverName())
    .append(Text.gold('! ')).append(had > 0 ? Text.gray('(your old level ' + global.FA_num(had) + ' came back as a tome) ') : Text.gray(''))
    .append(faButton('Upgrade', '/ascension', 'Open the upgrade menu')))
}

// Level the tome in your main hand (one tome at a time, so a stack never levels all at once)
function faUpgradeTome(player, amount) {
  let tome = player.getMainHandItem()
  let id = faTomeId(tome)
  if (!id) {
    player.tell(Text.red('Hold a tome in your main hand to level it.'))
    return 0
  }
  let name = global.FA.enchants[id].name
  if (tome.getCount() != 1) {
    player.tell(Text.red('Hold just one tome to level it (that\'s a stack of ' + tome.getCount() + ').'))
    return 0
  }
  let start = faTomeLevel(tome, id)
  let level = start
  let essence = faGetEssence(player)
  let max = faMaxLevel(id)
  let spent = 0
  while (level - start < amount && level < max) {
    let cost = global.FA_cost(level, id)
    if (essence - spent < cost) break
    spent += cost
    level++
  }
  if (level == start) {
    player.tell(level >= max ? Text.yellow('This tome is already at the max level.')
      : Text.red('Not enough Essence: the next level costs ' + global.FA_num(global.FA_cost(level, id)) + '.'))
    return 0
  }
  if (!faCommit(player, tome, id, faGetLevel(tome, id), level, essence, essence - spent)) return 0
  if (global.FA_onStat) global.FA_onStat(player, 'levels_bought', level - start)
  faSound(player, 'minecraft:block.enchantment_table.use', 1.2)
  player.tell(Text.green('+' + global.FA_num(level - start) + ' -> a level ' + global.FA_num(level) + ' Tome of ' + name + '  ')
    .append(Text.gray('(' + global.FA_describe(id, level) + ', spent ' + global.FA_num(spent) + ' Essence)')))
  return 1
}

// ---- Awakening: gear with enough enchant levels becomes a one-of-one Artifact ----

function faTotalLevels(gear) {
  let total = 0
  Object.keys(global.FA.enchants).forEach(id => { total += faGetLevel(gear, id) })
  return total
}

// Ascended Souls in the player's inventory
function faCountItem(player, id) {
  let inv = player.getInventory()
  let n = 0
  for (let i = 0; i < inv.getContainerSize(); i++) {
    let st = inv.getItem(i)
    if (st && !st.isEmpty() && String(st.getId()) == id) n += st.getCount()
  }
  return n
}

function faTakeItem(player, id, count) {
  let inv = player.getInventory()
  for (let i = 0; i < inv.getContainerSize() && count > 0; i++) {
    let st = inv.getItem(i)
    if (!st || st.isEmpty() || String(st.getId()) != id) continue
    let n = Math.min(count, st.getCount())
    st.shrink(n)
    count -= n
  }
}

function faAwaken(player) {
  let A = global.FA.artifacts
  let gear = faTarget(player)
  if (!A.enabled || !gear) {
    player.tell(Text.red('Hold the gear you want to Awaken.'))
    return 0
  }
  if (global.FA_isArtifact(gear)) {
    player.tell(Text.yellow('That is already an Artifact.'))
    return 0
  }
  let total = faTotalLevels(gear)
  if (total < A.awakenLevels) {
    player.tell(Text.red('Awakening needs ' + global.FA_num(A.awakenLevels) + ' total enchant levels on it (it has ' + global.FA_num(total) + ').'))
    return 0
  }
  let essence = faGetEssence(player)
  if (essence < A.awakenEssence || faCountItem(player, 'kubejs:ascended_soul') < A.awakenSouls) {
    player.tell(Text.red('Awakening costs ' + global.FA_num(A.awakenEssence) + ' Essence and ' + A.awakenSouls + ' Ascended Soul' + (A.awakenSouls > 1 ? 's' : '') + '.'))
    return 0
  }
  let art = global.FA_makeArtifact(gear, Math.random, player.getServer())
  if (!art) {
    player.tell(Text.red('That can\'t be Awakened.'))
    return 0
  }
  faSetEssence(player, essence - A.awakenEssence)
  faTakeItem(player, 'kubejs:ascended_soul', A.awakenSouls)
  let server = player.getServer()
  let name = player.username
  server.runCommandSilent('execute at ' + name + ' run playsound minecraft:ui.toast.challenge_complete player ' + name + ' ~ ~ ~ 1 0.6')
  server.runCommandSilent('execute at ' + name + ' run particle minecraft:end_rod ~ ~1 ~ 0.6 1 0.6 0.15 120 normal')
  server.runCommandSilent('title ' + name + ' times 5 60 20')
  server.runCommandSilent('title ' + name + ' subtitle ' + JSON.stringify({ text: art.title + '  \u00b7  1 of 1  \u00b7  #' + art.serial, color: 'gray' }))
  server.runCommandSilent('title ' + name + ' title ' + JSON.stringify({ text: art.name, color: art.color, bold: true }))
  server.tell(Text.lightPurple('\u2736 ' + name + ' Awakened ').append(Text.gold(art.name + ', ' + art.title)).append(Text.lightPurple(' (#' + art.serial + ')')))
  art.quirks.forEach(q => player.tell(Text.aqua('  \u25c8 ' + global.FA_quirkText(q.id, q.power))))
  return 1
}

// Pull an enchant off gear into a tome: gear in one hand, a Blank Ascension Tome in the other
function faExtract(player, id) {
  let main = player.getMainHandItem(), off = player.getOffhandItem()
  let blankId = 'kubejs:blank_ascension_tome'
  let gear = null, blank = null
  if (faIsGear(main) && off && String(off.getId()) == blankId) { gear = main; blank = off }
  else if (faIsGear(off) && main && String(main.getId()) == blankId) { gear = off; blank = main }
  let name = global.FA.enchants[id].name
  if (!gear) {
    player.tell(Text.red('Hold the gear in one hand and a Blank Ascension Tome in the other to extract an enchant.'))
    return 0
  }
  let level = faGetLevel(gear, id)
  if (level <= 0) {
    player.tell(Text.red('That has no ' + name + ' to extract.'))
    return 0
  }
  let tome = faMakeTome(id, level)
  try {
    faSetLevel(gear, id, 0)
    if (id == 'eternal' && gear.nbt) gear.nbt.remove('Unbreakable')
  } catch (err) {
    console.error('[Factory Ascension] extracting ' + id + ' failed: ' + err)
    player.tell(Text.red('Something went wrong, nothing was changed.'))
    return 0
  }
  blank.shrink(1)
  player.give(tome)
  faSound(player, 'minecraft:item.book.page_turn', 0.8)
  player.tell(Text.gold('Extracted ').append(Text.white(name + ' ' + global.FA_num(level))).append(Text.gold(' into a tome. ')).append(Text.gray('Right-click it with gear in your other hand to apply it.')))
  return 1
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
  if (global.FA_onStat) global.FA_onStat(player, 'levels_bought', added)
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

  // Only effects that suit this kind of gear (no Sharpness rolls on a helmet)
  let seed = global.FA_fittingSeed(Math.floor(Math.random() * 2147483646) + 1, faGearKind(gear))
  if (!faCommitRolled(player, gear, slot, old.seed, old.level, seed, 1, essence, essence - cost)) return
  tome.shrink(1)
  if (global.FA_onStat) global.FA_onStat(player, 'chaos_rolls', 1)

  let roll = global.FA_rollEnchant(seed)
  let R = global.FA.chaos.rarities[roll.rarity]
  let name = player.username
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
  if (global.FA_onStat) global.FA_onStat(player, 'levels_bought', added)
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

// ---- The safe zone's center ----
// Normally world spawn. /ascension center set moves it to you (Overworld); it's saved with the world.

function faSetCenter(server, center) {
  let data = server.persistentData
  if (center) {
    let c = data.getCompound('fa_danger_center')
    c.putInt('x', center.x)
    c.putInt('z', center.z)
    data.put('fa_danger_center', c)
  } else {
    data.remove('fa_danger_center')
  }
  global.FA_STATE.center = center || false   // never null on global
}

function faLoadCenter(server) {
  let c = server.persistentData.getCompound('fa_danger_center')
  global.FA_STATE.center = c.contains('x') ? { x: c.getInt('x'), z: c.getInt('z') } : false
}

ServerEvents.loaded(event => faLoadCenter(event.server))

function faCenterCommand(player, mode) {
  let D = global.FA.danger
  if (D.centerOn == 'home') {
    player.tell(Text.yellow('Danger is measured from each player\'s own home (/home set), so there\'s no shared center to move. ')
      .append(Text.gray('Set danger.centerOn to \'spawn\' in the config to use one.')))
    return 0
  }
  if (mode == 'set') {
    if (String(player.level.dimension) != 'minecraft:overworld') {
      player.tell(Text.red('Stand in the Overworld to move the safe zone.'))
      return 0
    }
    faSetCenter(player.getServer(), { x: Math.floor(player.getX()), z: Math.floor(player.getZ()) })
  } else if (mode == 'reset') {
    faSetCenter(player.getServer(), false)
  }
  let c = global.FA_STATE.center
  let spawn = player.getServer().overworld().getSharedSpawnPos()
  let where = c ? 'X ' + c.x + ', Z ' + c.z : 'world spawn (X ' + spawn.getX() + ', Z ' + spawn.getZ() + ')'
  player.tell(Text.green(mode ? 'Safe zone ' + (mode == 'set' ? 'moved' : 'reset') + ': ' : 'Safe zone: ').append(Text.white('centered on ' + where)))
  player.tell(Text.gray('No extra danger within ' + global.FA_num(D.safeRadius) + ' blocks of it, then +1 danger every ' + D.blocksPerLevel + ' blocks. ')
    .append(Text.darkGray('Mobs that already spawned keep their old level.')))
  return 1
}

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

  let tomeCmd = Commands.literal('tome').then(Commands.argument('amount', Arguments.INTEGER.create(event))
    .executes(ctx => faUpgradeTome(ctx.source.getPlayerOrException(),
      Math.max(1, Math.min(global.FA.maxUpgradesPerCommand, Arguments.INTEGER.getResult(ctx, 'amount'))))))
  let extractCmd = Commands.literal('extract')
  Object.keys(global.FA.enchants).forEach(id => {
    extractCmd = extractCmd.then(Commands.literal(id).executes(ctx => faExtract(ctx.source.getPlayerOrException(), id)))
  })

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
    .then(tomeCmd)
    .then(Commands.literal('awaken').executes(ctx => faAwaken(ctx.source.getPlayerOrException())))
    .then(extractCmd)
    .then(Commands.literal('danger').executes(ctx => faShowDanger(ctx.source.getPlayerOrException())))
    // Op-only: move the safe zone (no extra difficulty) to where you stand, or back to world spawn
    .then(Commands.literal('center').requires(src => src.hasPermission(2))
      .executes(ctx => faCenterCommand(ctx.source.getPlayerOrException(), ''))
      .then(Commands.literal('set').executes(ctx => faCenterCommand(ctx.source.getPlayerOrException(), 'set')))
      .then(Commands.literal('reset').executes(ctx => faCenterCommand(ctx.source.getPlayerOrException(), 'reset'))))
    .then(Commands.literal('milestones').executes(ctx => faShowMilestones(ctx.source.getPlayerOrException())))
    .then(Commands.literal('rebirth')
      .executes(ctx => faRebirth(ctx.source.getPlayerOrException(), false))
      .then(Commands.literal('confirm').executes(ctx => faRebirth(ctx.source.getPlayerOrException(), true))))
    .then(Commands.literal('event')
      .executes(ctx => {
        let e = global.FA_STATE.event
        ctx.source.getPlayerOrException().tell(e ? Text.gold('Current event: ' + e.type + ' (' + Math.ceil((e.until - Date.now()) / 60000) + ' min left)') : Text.gray('No world event right now.'))
        return 1
      })
      // Op-only: start or stop an event, e.g. /ascension event start bloodMoon
      .then(['bloodMoon', 'meteorShower', 'goldenHour'].reduce((cmd, type) => cmd.then(Commands.literal(type)
        .executes(ctx => global.FA_startEvent(ctx.source.getServer(), type) ? 1 : 0)), Commands.literal('start').requires(src => src.hasPermission(2))))
      .then(Commands.literal('stop').requires(src => src.hasPermission(2))
        .executes(ctx => { global.FA_endEvent(ctx.source.getServer()); return 1 })))
    // Op-only, for testing and server admins
    .then(Commands.literal('essence')
      .requires(src => src.hasPermission(2))
      .then(essenceCmd('add', (old, n) => old + n))
      .then(essenceCmd('set', (old, n) => n))))
})
