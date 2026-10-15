// Factory Ascension - pets and charms (config: pets, charms).
// Ascension Collars: right-click your own tamed pet with one; it gets the collar's health, attack and armor, heals
// fully, and stops taking fire and fall damage. Each collar needs the one before it. The Pet Whistle calls your pets.
// Charms: carried anywhere in the inventory, they keep their effects on you (the strongest of each kind counts).

const $FAAttributes = Java.loadClass('net.minecraft.world.entity.ai.attributes.Attributes')
const $FATamable = Java.loadClass('net.minecraft.world.entity.TamableAnimal')
const $FAHorse = Java.loadClass('net.minecraft.world.entity.animal.horse.AbstractHorse')
const $FAItemEntity = Java.loadClass('net.minecraft.world.entity.item.ItemEntity')

// Is this tamed animal the player's?
function faOwnsPet(player, e) {
  if (!e || !e.getOwnerUUID) return false
  let tame = e instanceof $FATamable ? e.isTame() : e instanceof $FAHorse ? e.isTamed() : false
  let owner = e.getOwnerUUID()
  return tame && owner != null && String(owner) == String(player.getUuid())
}

// Put collar `tier` (0-based) on a pet. Returns '' or why not.
function faApplyCollar(pet, tier) {
  let C = global.FA.pets.collars[tier]
  let has = pet.persistentData.getInt('fa_collar')   // 0 = none, else tier + 1
  if (has >= tier + 1) return 'It already wears a ' + global.FA.pets.collars[has - 1].name + '.'
  if (has < tier) return 'It needs a ' + global.FA.pets.collars[tier - 1].name + ' first.'
  let set = (attr, v) => { let a = pet.getAttribute(attr); if (a) a.setBaseValue(Math.max(a.getBaseValue(), v)) }
  set($FAAttributes.MAX_HEALTH, C.health)
  set($FAAttributes.ATTACK_DAMAGE, C.attack)
  set($FAAttributes.ARMOR, C.armor)
  pet.setHealth(pet.getMaxHealth())
  pet.persistentData.putInt('fa_collar', tier + 1)
  return ''
}

ItemEvents.entityInteracted(event => {
  try {
    let id = String(event.item.id)
    let tier = global.FA.pets.collars.findIndex(c => 'kubejs:' + c.id == id)
    if (tier < 0) return
    let player = event.player, pet = event.target
    if (!faOwnsPet(player, pet)) { player.displayClientMessage(Text.red('Collars go on your own tamed pets.'), true); event.cancel(); return }
    let why = faApplyCollar(pet, tier)
    if (why) player.displayClientMessage(Text.red(why), true)
    else {
      if (!player.isCreative()) event.item.shrink(1)
      let C = global.FA.pets.collars[tier]
      player.displayClientMessage(Text.gold('✦ ' + C.name + ': ').append(Text.white(C.health + ' health, ' + C.attack + ' attack, ' + C.armor + ' armor, no fire or fall damage')), true)
      pet.level.playSound(null, pet.blockPosition(), 'minecraft:entity.player.levelup', 'neutral', 0.6, 1.4)
    }
    event.cancel()
  } catch (err) {
    console.error('[Factory Ascension] collar: ' + err)
  }
})

// (Collared pets shrugging off fire and falls: startup_scripts/81_fa_pets.js)

// The Pet Whistle: your standing pets within range come to you
function faWhistle(player) {
  let range = global.FA.pets.whistleRange
  let box = player.getBoundingBox().inflate(range)
  let n = 0
  ;[$FATamable, $FAHorse].forEach(cls => {
    let list = player.level.getEntitiesOfClass(cls, box)
    for (let i = 0; i < list.size(); i++) {
      let pet = list.get(i)
      if (!faOwnsPet(player, pet) || (pet.isOrderedToSit && pet.isOrderedToSit()) || pet.isLeashed() || pet.isVehicle()) continue
      pet.setPositionAndRotation(player.getX(), player.getY(), player.getZ(), pet.yaw, 0)
      if (pet.getNavigation) pet.getNavigation().stop()
      n++
    }
  })
  return n
}

ItemEvents.rightClicked('kubejs:pet_whistle', event => {
  let n = faWhistle(event.player)
  event.player.displayClientMessage(n ? Text.aqua('Your pets come running (' + n + ').') : Text.gray('No pets of yours within ' + global.FA.pets.whistleRange + ' blocks (sitting ones stay put).'), true)
  event.player.level.playSound(null, event.player.blockPosition(), 'minecraft:block.note_block.flute', 'players', 1, 1.6)
  event.cancel()
})

// ---- Charms ----
// The best amplifier of each effect from the charms a player carries, plus the strongest magnet: { effects: {id: amp}, magnet }
function faCharmsOf(player) {
  let byId = {}
  global.FA.charms.list.forEach(c => { byId['kubejs:' + c.id] = c })
  let out = { effects: {}, magnet: 0 }
  let inv = player.getInventory()
  for (let i = 0; i < inv.getContainerSize(); i++) {
    let c = byId[String(inv.getItem(i).getId())]
    if (!c) continue
    c.effects.forEach(fx => { if (!(fx[0] in out.effects) || out.effects[fx[0]] < fx[1]) out.effects[fx[0]] = fx[1] })
    if (c.magnet) out.magnet = Math.max(out.magnet, c.magnet)
  }
  return out
}

function faCharmTick(player) {
  let got = faCharmsOf(player)
  let every = global.FA.charms.everyTicks
  Object.keys(got.effects).forEach(fx => {
    // Night vision flickers under 10 seconds left, so it gets a longer top-up
    player.potionEffects.add(fx, every + (fx == 'minecraft:night_vision' ? 260 : 40), got.effects[fx], true, false)
  })
  if (got.magnet > 0) {
    let items = player.level.getEntitiesOfClass($FAItemEntity, player.getBoundingBox().inflate(got.magnet))
    for (let i = 0; i < items.size(); i++) {
      let it = items.get(i)
      if (it.isAlive() && !it.hasPickUpDelay()) it.setPos(player.getX(), player.getY() + 0.5, player.getZ())
    }
  }
  return got
}

ServerEvents.tick(event => {
  let server = event.server
  if (server.getTickCount() % global.FA.charms.everyTicks != 0) return
  server.players.forEach(p => { try { faCharmTick(p) } catch (err) { console.error('[Factory Ascension] charms: ' + err) } })
})

ServerEvents.recipes(event => {
  let P = global.FA.pets
  P.collars.forEach(c => event.shaped('kubejs:' + c.id, c.recipe[0], c.recipe[1]).id('kubejs:factory_ascension/collar_' + c.id))
  event.shaped('kubejs:pet_whistle', P.whistle[0], P.whistle[1]).id('kubejs:factory_ascension/pet_whistle')
  let CH = global.FA.charms
  CH.list.forEach(c => {
    if (c.id == 'ascended_charm') {
      let others = CH.list.filter(o => o.id != 'ascended_charm').map(o => 'kubejs:' + o.id)
      event.shapeless('kubejs:ascended_charm', others.slice(0, 8).concat([c.extra])).id('kubejs:factory_ascension/charm_ascended')
    } else {
      event.shaped('kubejs:' + c.id, CH.recipe[0], Object.assign({ X: c.extra }, CH.recipe[1])).id('kubejs:factory_ascension/charm_' + c.id)
    }
  })
})

// For /ascension selftest
global.FA_applyCollar = (pet, tier) => faApplyCollar(pet, tier)
global.FA_charmTick = player => faCharmTick(player)
global.FA_whistle = player => faWhistle(player)

// /ascension loot: the same switch as sneak + swap hands (startup_scripts/20_fa_enchant_effects.js)
ServerEvents.commandRegistry(event => {
  let { commands: Commands } = event
  event.register(Commands.literal('ascension').then(Commands.literal('loot').executes(ctx => {
    global.FA_toggleLoot(ctx.source.getPlayerOrException())
    return 1
  })))
})

// ---- Unbinding Scroll: takes Soulbound off the gear in your other hand ----
ItemEvents.rightClicked('kubejs:unbinding_scroll', event => {
  let player = event.player
  let gear = event.hand == 'MAIN_HAND' || String(event.hand) == 'MAIN_HAND' ? player.getOffhandItem() : player.getMainHandItem()
  if (!global.FA_isSoulbound(gear)) {
    player.displayClientMessage(Text.gray('Hold the Soulbound gear in your other hand.'), true)
    event.cancel()
    return
  }
  let gtag = gear.nbt
  let fa = gtag.getCompound('FactoryAscension')
  fa.remove('soulbound')
  gtag.put('FactoryAscension', fa)
  gear.nbt = gtag
  if (!player.isCreative()) event.item.shrink(1)
  player.displayClientMessage(Text.lightPurple('Unbound: ').append(Text.white(gear.getHoverName().getString() + ' is no longer Soulbound.')), true)
  player.level.playSound(null, player.blockPosition(), 'minecraft:item.book.page_turn', 'players', 1, 0.7)
  event.cancel()
})

ServerEvents.recipes(event => {
  event.shaped('kubejs:unbinding_scroll', [' P ', 'ESE', ' P '], { P: 'minecraft:paper', E: 'minecraft:ender_pearl', S: 'kubejs:ascension_shard' })
    .id('kubejs:factory_ascension/unbinding_scroll')
})
