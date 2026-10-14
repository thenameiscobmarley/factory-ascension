// priority: 810
// Factory Ascension - Foil Sheets (config: foils): crafted, then applied to a tome by right-clicking with the tome in
// your other hand. A better foil replaces a worse one; a tome's foil multiplies the level it puts on gear.

ServerEvents.recipes(event => {
  global.FA.foils.types.forEach(f => {
    event.shapeless('kubejs:foil_sheet_' + f.id, f.recipe).id('kubejs:factory_ascension/foil_sheet_' + f.id)
  })
})

function faApplyFoil(player, sheet, index, tome) {
  let F = global.FA.foils
  let id = faTomeId(tome)
  if (!F.enabled || !id) {
    player.displayClientMessage(Text.red('Hold an enchant tome in your other hand to foil it.'), true)
    return 0
  }
  if (tome.getCount() != 1) {
    player.displayClientMessage(Text.red('Foil one tome at a time (that\'s a stack of ' + tome.getCount() + ').'), true)
    return 0
  }
  let have = global.FA_foilOf(tome)
  if (have >= index) {
    player.displayClientMessage(Text.yellow('That tome already has ' + F.types[have].name + ' Foil' + (have > index ? ', which is better.' : '.')), true)
    return 0
  }
  global.FA_setFoil(tome, index)
  sheet.shrink(1)
  let f = F.types[index]
  faSound(player, 'minecraft:block.amethyst_block.chime', 0.8 + index * 0.1)
  player.tell(Text.gold(f.name + ' Foil applied: ').append(Text.white(global.FA.enchants[id].name)).append(Text.gray(' now goes on gear at ' +
    global.FA_num(faAppliedLevel(tome, id)) + ' (x' + f.mult + ')')))
  return 1
}

global.FA.foils.types.forEach((f, index) => {
  ItemEvents.rightClicked('kubejs:foil_sheet_' + f.id, event => {
    let player = event.player
    let tome = String(event.hand) == 'MAIN_HAND' ? player.getOffhandItem() : player.getMainHandItem()
    faApplyFoil(player, event.item, index, tome)
    event.cancel()
  })
})
