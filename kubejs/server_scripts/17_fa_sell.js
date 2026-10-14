// priority: 830
// Factory Ascension - selling (config: sell). /ascension sell quotes the stack in your main hand;
// the buttons sell it for Ascension Cores (paid in Singularities, Dense Cores and Cores) or Liquid Essence buckets.
// Uses helpers from 10_fa_ascension.js and 16_fa_packs.js (same server scope).

// Essence it cost to take an enchant from level 1 to `level` (exact up to 20,000 levels, Simpson's rule past that)
function faLevelsCost(id, level) {
  let total = 0
  let exact = Math.min(level, 20000)
  for (let L = 1; L < exact; L++) total += global.FA_cost(L, id)
  if (level > 20000) {
    let a = 20000, b = level, m = (a + b) / 2
    total += (b - a) * (global.FA_cost(a, id) + 4 * global.FA_cost(Math.floor(m), id) + global.FA_cost(b, id)) / 6
  }
  return total
}

// { value (Essence, for the whole stack), what (a short description) }, value 0 = can't be sold
function faSellValue(stack) {
  let S = global.FA.sell
  if (!S.enabled || !stack || stack.isEmpty()) return { value: 0, what: '' }
  let id = String(stack.getId())
  let n = stack.getCount()
  if (S.items[id]) return { value: S.items[id] * n, what: n + 'x ' + stack.getHoverName().getString() }
  let gem = /^kubejs:gem_[a-z_]+_(\d)$/.exec(id)
  if (gem) return { value: (S.gems[parseInt(gem[1]) - 1] || 0) * n, what: n + 'x ' + stack.getHoverName().getString() }
  if (global.FA.mobFood.foods.some(fd => 'kubejs:' + fd.cooked == id)) return { value: S.foodCooked * n, what: n + 'x ' + stack.getHoverName().getString() }
  let tomeId = faTomeId(stack)
  if (tomeId) {
    let e = global.FA.enchants[tomeId]
    let kind = e.ultimate ? 'ultimate' : e.mythic ? 'mythic' : e.type == 'fusion' ? 'fusion' : 'base'
    let level = faTomeLevel(stack, tomeId)
    let each = S.tomes[kind] + S.levelRefund * faLevelsCost(tomeId, level)
    let tag = stack.nbt
    if (tag && tag.getCompound(FA_ITEM_TAG).contains('card')) each *= S.holoBonus
    let foil = global.FA_foilOf(stack)
    if (foil >= 0) each *= global.FA.foils.types[foil].mult
    return { value: each * n, what: n + 'x Tome of ' + e.name + (level > 1 ? ' ' + global.FA_num(level) : '') }
  }
  if (faIsGear(stack)) {
    let value = 0
    Object.keys(global.FA.enchants).forEach(eid => {
      let level = faGetLevel(stack, eid)
      if (level > 0) value += S.levelRefund * faLevelsCost(eid, level + 1)
    })
    let artifact = global.FA_isArtifact(stack)
    if (artifact) value += S.artifact
    return { value: value, what: stack.getHoverName().getString(), artifact: artifact }
  }
  return { value: 0, what: '' }
}

// Cores, compressed: Singularities (81), Dense Cores (9), Cores (1); anything under 1 goes into the Essence bank
function faPayCores(player, value) {
  let whole = Math.floor(value)
  let sing = Math.floor(whole / 81), dense = Math.floor((whole % 81) / 9), cores = whole % 9
  if (sing > 0) player.give(Item.of('kubejs:ascension_singularity', sing))
  if (dense > 0) player.give(Item.of('kubejs:dense_ascension_core', dense))
  if (cores > 0) player.give(Item.of('kubejs:ascension_core', cores))
  let frac = value - whole
  if (frac > 0) faSetEssence(player, faGetEssence(player) + frac)
  return { sing: sing, dense: dense, cores: cores }
}

function faSellQuote(player) {
  let held = player.getMainHandItem()
  let q = faSellValue(held)
  if (q.value <= 0) {
    player.tell(Text.red('That can\'t be sold. ').append(Text.gray('Tomes, enchanted gear, gems, cards, packs, ingots and boss drops can.')))
    return 0
  }
  let bucket = 1000 / global.FA.altar.mbPerEssence
  player.tell(Text.gold('Sell ').append(Text.white(q.what)).append(Text.gold(' for ')).append(Text.lightPurple(global.FA_num(q.value) + ' Essence')).append(Text.gold('?')))
  if (q.artifact) player.tell(Text.red('⚠ This is a one-of-one Artifact. There will never be another.'))
  let line = Text.green('[Sell for Cores]').clickRunCommand('/ascension sell cores confirm')
    .hover(Text.gray('Paid in Singularities, Dense Cores and Cores (' + global.FA_num(Math.floor(q.value)) + ' Essence worth)'))
  if (q.value >= bucket) {
    line = line.append(Text.gray('  ')).append(Text.aqua('[Sell for Liquid Essence]').clickRunCommand('/ascension sell buckets confirm')
      .hover(Text.gray(Math.floor(q.value / bucket) + ' buckets of Liquid Essence (' + bucket + ' Essence each), the rest in cores')))
  }
  player.tell(line)
  return 1
}

function faSell(player, mode) {
  let held = player.getMainHandItem()
  let q = faSellValue(held)
  if (q.value <= 0) return faSellQuote(player)
  held.shrink(held.getCount())
  let bucket = 1000 / global.FA.altar.mbPerEssence
  let rest = q.value
  let buckets = 0
  if (mode == 'buckets') {
    buckets = Math.floor(q.value / bucket)
    if (buckets > 0) player.give(Item.of('kubejs:liquid_essence_bucket', buckets))
    rest = q.value - buckets * bucket
  }
  let paid = faPayCores(player, rest)
  faSound(player, 'minecraft:entity.experience_orb.pickup', 0.7)
  let parts = []
  if (buckets) parts.push(buckets + ' Liquid Essence bucket' + (buckets > 1 ? 's' : ''))
  if (paid.sing) parts.push(paid.sing + ' Singularit' + (paid.sing > 1 ? 'ies' : 'y'))
  if (paid.dense) parts.push(paid.dense + ' Dense Core' + (paid.dense > 1 ? 's' : ''))
  if (paid.cores) parts.push(paid.cores + ' Core' + (paid.cores > 1 ? 's' : ''))
  player.tell(Text.green('Sold ' + q.what + ' for ').append(Text.white(parts.join(', ') || 'a little Essence')).append(Text.gray(' (' + global.FA_num(q.value) + ' Essence)')))
  if (global.FA_onStat) global.FA_onStat(player, 'items_sold', 1)
  return 1
}

ServerEvents.commandRegistry(event => {
  let { commands: Commands } = event
  event.register(Commands.literal('ascension').then(Commands.literal('sell')
    .executes(ctx => faSellQuote(ctx.source.getPlayerOrException()))
    .then(Commands.literal('cores').executes(ctx => faSellQuote(ctx.source.getPlayerOrException()))
      .then(Commands.literal('confirm').executes(ctx => faSell(ctx.source.getPlayerOrException(), 'cores'))))
    .then(Commands.literal('buckets').executes(ctx => faSellQuote(ctx.source.getPlayerOrException()))
      .then(Commands.literal('confirm').executes(ctx => faSell(ctx.source.getPlayerOrException(), 'buckets'))))))
})
