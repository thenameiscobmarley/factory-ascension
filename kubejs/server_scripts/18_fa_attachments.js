// priority: 820
// Factory Ascension - attachments (config: attachments): recipes, attaching and detaching.
// Hold an attachment with the gear in your other hand and right-click. /ascension lists them with [Detach].

ServerEvents.recipes(event => {
  global.FA.attachments.list.forEach(at => {
    event.shapeless('kubejs:attachment_' + at.id, at.recipe).id('kubejs:factory_ascension/attachment_' + at.id)
  })
  // Batteries: craft one, then recharge it (any charge level) by hand or in a Mechanical Mixer, no heat needed
  global.FA.attachments.batteries.forEach(b => {
    let id = 'kubejs:' + b.id
    event.shapeless(id, b.recipe).id('kubejs:factory_ascension/battery_' + b.id)
    event.shapeless(id, [id].concat(b.recharge)).id('kubejs:factory_ascension/recharge_' + b.id)
    event.custom({ type: 'create:mixing', ingredients: [{ item: id }].concat(b.recharge.map(r => ({ item: r }))), results: [{ item: id }] })
      .id('kubejs:factory_ascension/mixer_recharge_' + b.id)
  })
})

const FA_FITS_TEXT = { helmet: 'a helmet', chest: 'a chestplate', legs: 'leggings', boots: 'boots', weapon: 'a weapon', bow: 'a bow or crossbow', tool: 'a pickaxe, shovel or hoe' }

function faAttach(player, item, id, gear) {
  let C = global.FA.attachments
  let def = global.FA_attachmentDef(id)
  if (!C.enabled || !def) return 0
  let kind = gear ? global.FA_gearSlot(gear) : ''
  if (kind != def.fits) {
    player.displayClientMessage(Text.red('A ' + def.name + ' goes on ' + FA_FITS_TEXT[def.fits] + '. Hold that in your other hand.'), true)
    return 0
  }
  let have = global.FA_attachIds(gear)
  if (have.indexOf(id) >= 0) {
    player.displayClientMessage(Text.yellow('That already has a ' + def.name + '.'), true)
    return 0
  }
  if (have.length >= C.slots) {
    player.displayClientMessage(Text.red('No free attachment slots (' + C.slots + '). Detach one in /ascension first.'), true)
    return 0
  }
  let tag = gear.getOrCreateTag()
  let data = tag.getCompound(FA_ITEM_TAG)
  let at = data.getCompound('attach')
  let slot = 0
  while (at.contains(String(slot))) slot++
  at.putString(String(slot), id)
  data.put('attach', at)
  tag.put(FA_ITEM_TAG, data)
  item.shrink(1)
  faSound(player, 'minecraft:block.smithing_table.use', 1.2)
  player.tell(Text.gold(def.name + ' attached to ').append(gear.getHoverName()).append(Text.gold('! ')).append(Text.gray(global.FA_attachmentEffect(def))))
  return 1
}

function faDetach(player, slot) {
  let gear = faTarget(player)
  if (!gear) {
    player.tell(Text.red('Hold the gear to detach from.'))
    return 0
  }
  let tag = gear.nbt
  let data = tag ? tag.getCompound(FA_ITEM_TAG) : null
  let at = data ? data.getCompound('attach') : null
  if (!at || !at.contains(String(slot))) {
    player.tell(Text.red('Nothing attached in that slot.'))
    return 0
  }
  let id = String(at.getString(String(slot)))
  at.remove(String(slot))
  data.put('attach', at)
  tag.put(FA_ITEM_TAG, data)
  player.give(Item.of('kubejs:attachment_' + id))
  faSound(player, 'minecraft:block.chain.break', 1)
  let def = global.FA_attachmentDef(id)
  player.tell(Text.gray('Detached the ' + (def ? def.name : id) + '.'))
  return 1
}

global.FA.attachments.list.forEach(at => {
  ItemEvents.rightClicked('kubejs:attachment_' + at.id, event => {
    let player = event.player
    let gear = String(event.hand) == 'MAIN_HAND' ? player.getOffhandItem() : player.getMainHandItem()
    faAttach(player, event.item, at.id, gear)
    event.cancel()
  })
})

ServerEvents.commandRegistry(event => {
  let { commands: Commands, arguments: Arguments } = event
  event.register(Commands.literal('ascension').then(Commands.literal('detach')
    .then(Commands.argument('slot', Arguments.INTEGER.create(event))
      .executes(ctx => faDetach(ctx.source.getPlayerOrException(), Arguments.INTEGER.getResult(ctx, 'slot'))))))
})
