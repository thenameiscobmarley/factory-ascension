// priority: 490
// Factory Ascension - rules for the blocks in the Factory Ascension mod (the "Full" download's jar; config: machines).
// Without the mod installed this file does nothing, so the "Half" download (KubeJS only) works on its own.
//   Ascension Vault: upgraded by right-clicking it with the next upgrade's items (sizes are in the mod)
//   Essence Forge:   levels the gear in its slot from its Liquid Essence tank, the same rules as the Ascension Altar
//   Pack Opener:     opens one Enchantment Pack a second into its outputs (the same card rules as opening by hand)
// Uses helpers from 10_fa_ascension.js, 16_fa_packs.js and 45_fa_altar.js (same server scope).

const FA_MOD = 'factoryascension'
const FA_FORGE_ID = FA_MOD + ':essence_forge'
const FA_OPENER_ID = FA_MOD + ':pack_opener'
const FA_VAULT_ID = FA_MOD + ':ascension_vault'

if (Platform.isLoaded(FA_MOD)) {
  ServerEvents.recipes(event => {
    let R = global.FA.machines.recipes
    Object.keys(R).forEach(b => event.shaped(FA_MOD + ':' + b, R[b][0], R[b][1]).id('kubejs:factory_ascension/machine_' + b))
  })

  // Placed machines are remembered (per world) like Altars, so they run while their chunk is loaded
  let faMachineSet = (server, name) => {
    let data = server.persistentData
    if (!data.contains(name)) data.put(name, data.getCompound(name))
    return data.getCompound(name)
  }
  ;[[FA_FORGE_ID, 'fa_forges'], [FA_OPENER_ID, 'fa_openers']].forEach(m => {
    BlockEvents.placed(m[0], event => {
      faMachineSet(event.server, m[1]).putString(faAltarKey(String(event.level.dimension), event.block.pos), event.player ? event.player.stringUuid : '')
    })
    BlockEvents.broken(m[0], event => {
      faMachineSet(event.server, m[1]).remove(faAltarKey(String(event.level.dimension), event.block.pos))
    })
  })

  // ---- Ascension Vault upgrades ----
  BlockEvents.rightClicked(FA_VAULT_ID, event => {
    let player = event.player
    let held = player.getMainHandItem()
    if (!held || held.isEmpty()) return
    let vault = event.level.getBlockEntity(event.block.pos)
    if (!vault || !vault.faGetTier) return
    let ups = global.FA.machines.vaultUpgrades
    let tier = vault.faGetTier()
    let up = ups[tier]
    let isUpgrade = ups.some(u => u[0] == String(held.getId()))
    if (!isUpgrade) return
    if (!up) {
      player.displayClientMessage(Text.yellow('This vault is fully upgraded (' + vault.faGetCapacity() + ' slots).'), true)
    } else if (String(held.getId()) != up[0] || held.getCount() < up[1]) {
      player.displayClientMessage(Text.red('The next upgrade needs ' + up[1] + ' ' + Item.of(up[0]).getHoverName().getString() + '.'), true)
    } else {
      held.shrink(up[1])
      vault.faSetTier(tier + 1)
      faSound(player, 'minecraft:block.smithing_table.use', 0.8)
      player.tell(Text.gold('Vault upgraded: ').append(Text.white(vault.faGetCapacity() + ' slots')))
    }
    event.cancel()
  })

  // ---- Essence Forge and Pack Opener, once a second ----
  let faRunForge = (level, pos) => {
    let forge = level.getBlockEntity(pos)
    if (!forge || !forge.faReport) return 0
    let given = faLevelGear(level, pos, forge.items(), forge.tank())
    forge.faReport(given)
    return given
  }

  let faRunOpener = (level, pos) => {
    let opener = level.getBlockEntity(pos)
    if (!opener || !opener.faInput) return 0
    let input = opener.faInput(), output = opener.faOutput()
    let stack = input.getStackInSlot(0)
    if (!stack || stack.isEmpty()) return 0
    // A Booster Box turns into its packs once the slot is free for them
    if (String(stack.getId()) == 'kubejs:booster_box') {
      if (stack.getCount() == 1) input.setStackInSlot(0, Item.of('kubejs:enchantment_pack', global.FA.packs.boxPacks))
      return 0
    }
    let cards = global.FA_openPack(Math.random).map(c => {
      let tome = faMakeTome(c.id, c.level)
      if (c.foil >= 0) global.FA_setFoil(tome, c.foil)
      return tome
    })
    // Only open it if every card fits in the outputs
    let fits = cards.every(card => {
      let left = card.copy()
      for (let i = 0; i < output.getSlots() && !left.isEmpty(); i++) left = output.insertItem(i, left, true)
      return left.isEmpty()
    })
    if (!fits) return 0
    input.extractItem(0, 1, false)
    opener.faNewPack()
    cards.forEach(card => {
      let left = card
      for (let i = 0; i < output.getSlots() && !left.isEmpty(); i++) left = output.insertItem(i, left, false)
      opener.faShowCard(card)
    })
    opener.faFlush()
    let at = (pos.getX() + 0.5) + ' ' + (pos.getY() + 1.1) + ' ' + (pos.getZ() + 0.5)
    level.getServer().runCommandSilent('execute in ' + level.dimension + ' run particle minecraft:end_rod ' + at + ' 0.3 0.2 0.3 0.02 8 normal')
    return 1
  }
  global.FA_runOpener = (level, pos) => faRunOpener(level, pos)
  global.FA_runForge = (level, pos) => faRunForge(level, pos)

  ServerEvents.tick(event => {
    let server = event.server
    if (server.getTickCount() % 20 != 10) return
    try {
      if (server.persistentData.contains('fa_forges')) faTickMachines(server, faMachineSet(server, 'fa_forges'), FA_FORGE_ID, faRunForge, null)
      let every = Math.max(1, Math.round(global.FA.machines.openerSeconds))
      if (server.persistentData.contains('fa_openers') && (server.getTickCount() / 20) % every < 1) {
        faTickMachines(server, faMachineSet(server, 'fa_openers'), FA_OPENER_ID, faRunOpener, null)
      }
    } catch (err) {
      console.error('[Factory Ascension] mod machine tick failed: ' + err)
    }
  })
}
