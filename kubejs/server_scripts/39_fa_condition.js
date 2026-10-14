// priority: 620
// Factory Ascension - gear condition (config: condition).
// Crafted gear rolls its grade right away; anything else rolls the first time it's held or worn (startup scripts).
// A Polishing Cloth rerolls a grade and keeps the better one.

const FA_GRADE_TEXT = { dark_gray: 'darkGray', gray: 'gray', white: 'white', green: 'green', aqua: 'aqua', gold: 'gold' }

function faGradeText(grade) {
  let g = global.FA.condition.grades[grade]
  return Text[FA_GRADE_TEXT[g.color] || 'white']((grade == global.FA.condition.grades.length - 1 ? '✦ ' : '') + g.name)
}

ItemEvents.crafted(event => {
  try {
    global.FA_assignCondition(event.item, Math.random)
  } catch (err) {
    console.warn('[Factory Ascension] gear condition on craft failed: ' + err)
  }
})

// Polishing: hold the cloth in one hand and the gear in the other, right-click the cloth
function faPolish(player, cloth, gear) {
  let C = global.FA.condition
  if (!C.enabled) return
  if (!gear || gear.isEmpty() || gear.getMaxStackSize() != 1 || gear.getMaxDamage() <= 0) {
    player.displayClientMessage(Text.gray('Hold the gear you want to polish in your other hand'), true)
    return
  }
  let best = C.grades.length - 1
  let old = global.FA_conditionOf(gear)
  if (old < 0) old = global.FA_assignCondition(gear, Math.random)
  if (old == best) {
    player.displayClientMessage(Text.gold('That\'s already Pristine. It can\'t shine any brighter.'), true)
    return
  }
  let roll = global.FA_rollCondition(Math.random)
  cloth.shrink(1)
  if (roll > old) {
    global.FA_setCondition(gear, roll)
    player.tell(Text.gray('Polished: ').append(faGradeText(old)).append(Text.gray(' → ')).append(faGradeText(roll)).append(Text.gray('!')))
    player.level.getServer().runCommandSilent('execute at ' + player.username + ' run playsound minecraft:block.amethyst_block.chime player ' +
      player.username + ' ~ ~ ~ 1 ' + (roll == best ? 0.6 : 1.4))
  } else {
    player.displayClientMessage(Text.gray('Still ').append(faGradeText(old)).append(Text.darkGray(' (rolled ')).append(faGradeText(roll)).append(Text.darkGray(')')), true)
  }
}

ItemEvents.rightClicked('kubejs:polishing_cloth', event => {
  let player = event.player
  let gear = String(event.hand) == 'MAIN_HAND' ? player.getOffhandItem() : player.getMainHandItem()
  faPolish(player, event.item, gear)
  event.cancel()
})
