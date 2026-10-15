// priority: 485
// Factory Ascension - Legendary stars (config: legendary): Fallout 76-style grades for gear, from no stars to 10.
// Each star costs the config's items (from your inventory), adds one legendary effect (a random enchant that suits
// the gear) and makes every level on the item count for more. Made at the Ascension Workbench (Full download), or with
// /ascension legendary on the gear in your hand (both downloads).
// Uses helpers from 10_fa_ascension.js (same server scope).

function faLegend(stack) {
  if (!stack || stack.isEmpty() || !stack.nbt || !stack.nbt.contains(FA_ITEM_TAG)) return 0
  return Math.max(0, stack.nbt.getCompound(FA_ITEM_TAG).getInt('legend'))
}
global.FA_legendOf = stack => faLegend(stack)

// What the next star needs, as text: "4 Dense Ascension Core, 1 Mythic Fragment"
function faLegendCostText(star) {
  let cost = global.FA.legendary.costs[star - 1]
  return cost ? cost.map(c => c[1] + ' ' + Item.of(c[0]).getHoverName().getString()).join(', ') : ''
}

// The enchants a legendary effect can be: base enchants (no Mythic, Ultimate or Fusion) that suit this gear and
// aren't one of its legendary effects already
function faLegendChoices(gear, fx) {
  let E = global.FA.enchants, kind = faGearKind(gear)
  let taken = {}
  fx.getAllKeys().forEach(k => { taken[String(fx.getCompound(k).getString('id'))] = true })
  return Object.keys(E).filter(id => !E[id].mythic && !E[id].ultimate && E[id].type != 'fusion' && E[id].maxLevel != 1 &&
    !taken[id] && global.FA_enchantFits(id, kind))
}

// Can this gear take another star? { ok, text, missing: [[item, have, need]] }
function faLegendCheck(player, gear) {
  let L = global.FA.legendary
  if (!L.enabled) return { ok: false, text: 'Legendary stars are switched off.' }
  if (!faIsGear(gear)) return { ok: false, text: 'Put a weapon, tool or piece of armor in.' }
  let stars = faLegend(gear)
  if (stars >= L.maxStars) return { ok: false, text: 'Already ' + L.maxStars + ' stars: as legendary as it gets.' }
  let missing = []
  L.costs[stars].forEach(c => {
    let have = player ? faCountItem(player, c[0]) : 0
    if (have < c[1]) missing.push([c[0], have, c[1]])
  })
  let next = stars + 1
  return { ok: missing.length == 0, missing: missing,
    text: 'Star ' + next + ' needs ' + faLegendCostText(next) + (missing.length ? ' (missing ' + missing.map(m => (m[2] - m[1]) + ' ' + Item.of(m[0]).getHoverName().getString()).join(', ') + ')' : '') }
}
global.FA_legendCheck = (player, gear) => faLegendCheck(player, gear)

// Add a star: take the cost, roll the new legendary effect, tell the player. Returns 1 if it worked.
function faLegendUp(player, gear) {
  let L = global.FA.legendary
  let check = faLegendCheck(player, gear)
  if (!check.ok) {
    player.displayClientMessage(Text.red(check.text), true)
    return 0
  }
  let star = faLegend(gear) + 1
  let tag = gear.getOrCreateTag()
  let data = tag.getCompound(FA_ITEM_TAG)
  let fx = data.getCompound('legendfx')
  let choices = faLegendChoices(gear, fx)
  L.costs[star - 1].forEach(c => faTakeItem(player, c[0], c[1]))
  let id = choices.length ? choices[Math.floor(Math.random() * choices.length)] : ''
  if (id) {
    let e = new $FALegendCompound()
    e.putString('id', id)
    e.putInt('level', L.effectLevels[star - 1])
    fx.put(String(star - 1), e)
    data.put('legendfx', fx)
  }
  data.putInt('legend', star)
  tag.put(FA_ITEM_TAG, data)
  let grade = L.grades[star]
  let col = { gold: 'gold', light_purple: 'lightPurple', red: 'red', aqua: 'aqua', gray: 'gray' }[grade[1]] || 'gold'
  faSound(player, star >= 7 ? 'minecraft:ui.toast.challenge_complete' : 'minecraft:block.anvil.use', star >= 7 ? 1 : 1.4)
  player.tell(Text[col](global.FA_legendStars(star) + ' ' + grade[0].toUpperCase()).bold(true)
    .append(Text.gray(' ').append(gear.getHoverName()))
    .append(id ? Text.gold('  + Legendary ' + global.FA.enchants[id].name + ' ' + global.FA_num(L.effectLevels[star - 1])) : Text.gray('')))
  if (star == L.maxStars) player.getServer().tell(Text.aqua(player.username + ' made a ' + L.maxStars + '-star Ascended Legendary: ').append(gear.getHoverName()))
  if (global.FA_onStat) global.FA_onStat(player, 'legend_stars', 1)
  return 1
}
global.FA_legendUp = (player, gear) => faLegendUp(player, gear)

const $FALegendCompound = Java.loadClass('net.minecraft.nbt.CompoundTag')

ServerEvents.commandRegistry(event => {
  let { commands: Commands } = event
  event.register(Commands.literal('ascension').then(Commands.literal('legendary').executes(ctx => {
    let player = ctx.source.getPlayerOrException()
    let gear = faTarget(player)
    if (!gear) {
      player.tell(Text.red('Hold the gear to make legendary.'))
      return 0
    }
    return faLegendUp(player, gear)
  })))
})
