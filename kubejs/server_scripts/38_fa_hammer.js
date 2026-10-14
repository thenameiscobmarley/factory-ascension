// priority: 630
// Factory Ascension - Rock Hammers (config: hammer).
// Hand: hammer in your main hand, rocks in your other hand, right-click to smash one (sneak: a handful).
// Block: right-click a placed rock with the hammer to smash it where it is. A Create Deployer holding a hammer
// does this too, so a cobblestone generator + deployer is an automatic ore smasher.

ServerEvents.recipes(event => {
  let hammer = (id, metal) => event.shaped(id, ['MMM', 'MSM', ' S '], { M: metal, S: 'minecraft:stick' }).id('kubejs:factory_ascension/' + id.replace('kubejs:', ''))
  hammer('kubejs:rock_hammer', 'minecraft:iron_ingot')
  hammer('kubejs:ascendite_rock_hammer', 'kubejs:ascendite_ingot')
})

// Items from mods that aren't installed are skipped (checked once per id)
let faItemOk = {}
function faRealItem(id) {
  if (faItemOk[id] === undefined) {
    try {
      faItemOk[id] = !Item.of(id).isEmpty()
    } catch (err) {
      faItemOk[id] = false
    }
  }
  return faItemOk[id]
}

// Wears the hammer down; returns false once it breaks
function faWearHammer(player, hammer, uses) {
  if (player.isCreative()) return true
  let damage = hammer.getDamageValue() + uses
  if (damage >= hammer.getMaxDamage()) {
    hammer.shrink(1)
    faHammerSound(player.level, 'minecraft:entity.item.break', player.getX(), player.getY(), player.getZ(), 1)
    return false
  }
  hammer.setDamageValue(damage)
  return true
}

function faHammerSound(level, sound, x, y, z, pitch) {
  level.getServer().runCommandSilent('execute in ' + level.dimension + ' run playsound ' + sound + ' block @a ' +
    x.toFixed(1) + ' ' + y.toFixed(1) + ' ' + z.toFixed(1) + ' 1 ' + pitch.toFixed(2))
}

function faSmashSound(level, x, y, z) {
  faHammerSound(level, 'minecraft:block.stone.break', x, y, z, 0.8 + Math.random() * 0.3)
}

Object.keys(global.FA.hammer.tools).forEach(hammerId => {
  // Hand mode: rocks in the other hand
  ItemEvents.rightClicked(hammerId, event => {
    let player = event.player
    let H = global.FA.hammer
    if (String(event.hand) != 'MAIN_HAND') return
    let rocks = player.getOffhandItem()
    if (!rocks || rocks.isEmpty() || !H.rocks[String(rocks.getId())]) {
      player.displayClientMessage(Text.gray('Hold rocks in your other hand to smash them (cobblestone, deepslate, gravel, netherrack, blackstone, end stone)'), true)
      return
    }
    let hammer = event.item
    if (player.getCooldowns().isOnCooldown(hammer.getItem())) return
    let rock = String(rocks.getId())
    let n = Math.min(player.isShiftKeyDown() ? H.sneakCount : 1, rocks.getCount())
    for (let i = 0; i < n; i++) {
      rocks.shrink(1)
      global.FA_smash(Math.random, rock, hammerId).forEach(r => { if (faRealItem(r[0])) player.give(Item.of(r[0], r[1])) })
      if (!faWearHammer(player, hammer, 1)) break
    }
    faSmashSound(player.level, player.getX(), player.getY(), player.getZ())
    player.getCooldowns().addCooldown(hammer.getItem(), H.tools[hammerId].cooldown)
    event.cancel()
  })
})

// Block mode: smash a placed rock where it is (players and Deployers)
BlockEvents.rightClicked(event => {
  let H = global.FA.hammer
  let hammer = event.item
  if (!hammer || hammer.isEmpty()) return
  let hammerId = String(hammer.getId())
  let rock = String(event.block.id)
  if (!H.tools[hammerId] || !H.rocks[rock]) return
  let player = event.player
  if (player && player.getCooldowns().isOnCooldown(hammer.getItem())) return
  let block = event.block
  block.set('minecraft:air')
  global.FA_smash(Math.random, rock, hammerId).forEach(r => { if (faRealItem(r[0])) block.popItem(Item.of(r[0], r[1])) })
  faSmashSound(event.level, block.x + 0.5, block.y + 0.5, block.z + 0.5)
  if (player) {
    faWearHammer(player, hammer, 1)
    player.getCooldowns().addCooldown(hammer.getItem(), H.tools[hammerId].cooldown)
  }
  event.cancel()
})
