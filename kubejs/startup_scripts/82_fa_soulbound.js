// Factory Ascension - Soulbound (enchant 'soulbound'): the item stays with its owner until an Unbinding Scroll takes the
// enchant off (server_scripts/51_fa_pets_charms.js). Kept through death, can't be thrown out, and if it ever lands on the
// ground anyway (a broken chest...) it can't burn, blow up or despawn.

const $SBPlayer = Java.loadClass('net.minecraft.world.entity.player.Player')
const $SBItemEntity = Java.loadClass('net.minecraft.world.entity.item.ItemEntity')
const $SBItemStack = Java.loadClass('net.minecraft.world.item.ItemStack')
const $SBCompound = Java.loadClass('net.minecraft.nbt.CompoundTag')
const $SBList = Java.loadClass('net.minecraft.nbt.ListTag')
const FA_SB_KEPT = 'fa_soulbound_ench'   // inside Forge's PlayerPersisted tag, which survives death ('fa_soulbound' is the Artifact quirk's)

function faSoulbound(stack) {
  if (!stack || stack.isEmpty()) return false
  let tag = stack.nbt
  return !!tag && tag.contains('FactoryAscension') && tag.getCompound('FactoryAscension').getInt('soulbound') > 0
}
global.FA_isSoulbound = stack => faSoulbound(stack)

function faSBPersisted(player) {
  let data = player.getPersistentData()
  if (!data.contains('PlayerPersisted')) data.put('PlayerPersisted', new $SBCompound())
  return data.getCompound('PlayerPersisted')
}

// Death: soulbound items leave the drops and wait for the respawn
function faSBKeep(player, drops) {
  let keep = faSBPersisted(player)
  let list = keep.getList(FA_SB_KEPT, 10)
  let n = 0
  let it = drops.iterator()
  while (it.hasNext()) {
    let drop = it.next()
    if (faSoulbound(drop.getItem())) {
      list.add(drop.getItem().save(new $SBCompound()))
      drop.discard()
      it.remove()
      n++
    }
  }
  if (n) keep.put(FA_SB_KEPT, list)
  return n
}

function faSBReturn(player) {
  let keep = faSBPersisted(player)
  if (!keep.contains(FA_SB_KEPT)) return 0
  let list = keep.getList(FA_SB_KEPT, 10)
  for (let i = 0; i < list.size(); i++) {
    let stack = $SBItemStack.of(list.getCompound(i))
    if (!player.getInventory().add(stack)) player.drop(stack, false)   // a full inventory: at your feet, still protected
  }
  keep.remove(FA_SB_KEPT)
  return list.size()
}
global.FA_keepSoulboundGear = (player, drops) => faSBKeep(player, drops)
global.FA_returnSoulboundGear = player => faSBReturn(player)

ForgeEvents.onEvent('net.minecraftforge.event.entity.living.LivingDropsEvent', event => {
  try {
    let player = event.getEntity()
    if (player instanceof $SBPlayer && !player.level.isClientSide()) faSBKeep(player, event.getDrops())
  } catch (err) {
    console.error('[Factory Ascension] soulbound (death): ' + err)
  }
})

ForgeEvents.onEvent('net.minecraftforge.event.entity.player.PlayerEvent$PlayerRespawnEvent', event => {
  try {
    faSBReturn(event.getEntity())
  } catch (err) {
    console.error('[Factory Ascension] soulbound (respawn): ' + err)
  }
})

// Throwing it out (Q, or dragging it out of a screen): it goes straight back
ForgeEvents.onEvent('net.minecraftforge.event.entity.item.ItemTossEvent', event => {
  try {
    let stack = event.getEntity().getItem()
    if (!faSoulbound(stack)) return
    let player = event.getPlayer()
    if (player.getInventory().add(stack.copy())) {
      event.setCanceled(true)
      player.displayClientMessage(Text.lightPurple('Soulbound: it stays with you.'), true)
    }
  } catch (err) {
    console.error('[Factory Ascension] soulbound (toss): ' + err)
  }
})

// On the ground anyway: lava, fire, cactus and explosions can't hurt it, and it never despawns
ForgeEvents.onEvent('net.minecraftforge.event.entity.EntityJoinLevelEvent', event => {
  try {
    let e = event.getEntity()
    if (!(e instanceof $SBItemEntity) || !faSoulbound(e.getItem())) return
    e.setInvulnerable(true)
    e.setUnlimitedLifetime()
  } catch (err) {
    console.error('[Factory Ascension] soulbound (item): ' + err)
  }
})
