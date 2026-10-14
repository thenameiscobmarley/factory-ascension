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

// ---- Essence bank ----

function faGetEssence(player) {
  return player.getServer().persistentData.getDouble('fa_essence_' + player.getStringUUID())
}

function faSetEssence(player, value) {
  player.getServer().persistentData.putDouble('fa_essence_' + player.getStringUUID(), Math.max(0, value))
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

function faGetLevel(stack, id) {
  let tag = stack.getTag()
  return tag && tag.contains(FA_ITEM_TAG) ? tag.getCompound(FA_ITEM_TAG).getInt(id) : 0
}

function faSetLevel(stack, id, level) {
  let tag = stack.getOrCreateTag()
  let data = tag.getCompound(FA_ITEM_TAG)
  data.putInt(id, level)
  tag.put(FA_ITEM_TAG, data)
  if (id == 'eternal') tag.putBoolean('Unbreakable', true)
}

function faMaxLevel(id) {
  let max = global.FA.enchants[id].maxLevel
  return max ? max : 2147483647
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

    let line = Text.gold(' ' + global.FA.enchants[id].name + ' ' + global.FA_num(level) + ' ')
      .append(Text.gray(global.FA_describe(id, level) + ' '))
    if (level < faMaxLevel(id)) {
      let cost = global.FA_cost(level)
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

  if (!any) player.tell(Text.gray(' No Ascended enchants yet. Hold a Tome in your other hand and right-click it.'))
  return 1
}

// ---- Actions ----

function faAbsorb(player, stack, id) {
  let count = player.isShiftKeyDown() ? stack.getCount() : 1
  if (count <= 0) return
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
  let cost = global.FA_cost(0)
  let essence = faGetEssence(player)
  if (essence < cost) {
    player.displayClientMessage(Text.red('Unlocking needs ' + global.FA_num(cost) + ' Essence. Absorb some Ascension Cores first.'), true)
    return
  }

  faSetEssence(player, essence - cost)
  faSetLevel(gear, id, 1)
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
    let cost = global.FA_cost(level)
    if (essence - spent < cost) break
    spent += cost
    level++
    added++
  }

  if (added == 0) {
    player.tell(level >= max
      ? Text.yellow(name + ' is already at its max level.')
      : Text.red('Not enough Essence: the next level costs ' + global.FA_num(global.FA_cost(level)) + '.'))
    return 0
  }

  faSetEssence(player, essence - spent)
  faSetLevel(gear, id, level)
  faSound(player, 'minecraft:block.enchantment_table.use', 1.4)
  player.tell(Text.green('+' + global.FA_num(added) + ' ' + name + ' -> ' + global.FA_num(level) + '  ')
    .append(Text.gray('(' + global.FA_describe(id, level) + ', spent ' + global.FA_num(spent) + ' Essence)')))
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

ServerEvents.commandRegistry(event => {
  let { commands: Commands, arguments: Arguments } = event

  let upgrade = Commands.literal('upgrade')
  Object.keys(global.FA.enchants).forEach(id => {
    upgrade = upgrade.then(Commands.literal(id)
      .then(Commands.argument('amount', Arguments.INTEGER.create(event))
        .executes(ctx => faUpgrade(ctx.source.getPlayerOrException(), id,
          Math.max(1, Math.min(global.FA.maxUpgradesPerCommand, Arguments.INTEGER.getResult(ctx, 'amount')))))))
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
    // Op-only, for testing and server admins
    .then(Commands.literal('essence')
      .requires(src => src.hasPermission(2))
      .then(essenceCmd('add', (old, n) => old + n))
      .then(essenceCmd('set', (old, n) => n))))
})
