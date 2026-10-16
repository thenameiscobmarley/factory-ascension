// priority: 490
// Factory Ascension - rules for the blocks in the Factory Ascension mod (the "Full" download's jar; config: machines).
// The Super Smelter and Modular Engine run in the mod itself (numbers pushed in by startup_scripts/97_fa_mod.js).
// Without the mod installed this file does nothing, so the "Half" download (KubeJS only) works on its own.
//   Ascension Vault: upgraded by right-clicking it with the next upgrade's items (sizes are in the mod)
//   Essence Forge:   levels the gear in its slot from its Liquid Essence tank, the same rules as the Ascension Altar
//   Pack Opener:     opens one Enchantment Pack a second into its outputs (the same card rules as opening by hand)
//   Salvager:        sells loot put in it for cores (the same prices as /ascension sell)
//   Fusion Table:    merges copies of a tome, or makes a Fusion / Triad from its parents
//   Super Smelter:   upgraded by right-clicking it, like the vault
// Uses helpers from 10_fa_ascension.js, 16_fa_packs.js and 45_fa_altar.js (same server scope).

const FA_MOD = 'factoryascension'
const FA_FORGE_ID = FA_MOD + ':essence_forge'
const FA_OPENER_ID = FA_MOD + ':pack_opener'
const FA_VAULT_ID = FA_MOD + ':ascension_vault'

if (Platform.isLoaded(FA_MOD)) {
  ServerEvents.recipes(event => {
    let R = global.FA.machines.recipes
    Object.keys(R).forEach(b => event.shaped(FA_MOD + ':' + b, R[b][0], R[b][1]).id('kubejs:factory_ascension/machine_' + b))
    global.FA.machines.miniCrusher.motors.forEach(m => event.shaped('kubejs:' + m.id, m.recipe[0], m.recipe[1]).id('kubejs:factory_ascension/motor_' + m.id))
    // Ascension backpacks: Sophisticated's upgrade recipe keeps everything inside when you move up a tier
    if (Platform.isLoaded('sophisticatedbackpacks')) global.FA.backpacks.tiers.forEach((t, i) => {
      let prev = i == 0 ? 'sophisticatedbackpacks:netherite_backpack' : 'factoryascension:' + global.FA.backpacks.tiers[i - 1].id + '_backpack'
      let key = {}
      Object.keys(t.recipe[1]).forEach(k => { key[k] = { item: t.recipe[1][k] } })
      key.B = { item: prev }
      event.custom({ type: 'sophisticatedbackpacks:backpack_upgrade', pattern: t.recipe[0], key: key, result: { item: 'factoryascension:' + t.id + '_backpack' } })
        .id('kubejs:factory_ascension/backpack_' + t.id)
    })
    let G = global.FA.guns
    event.shaped(FA_MOD + ':shotgun', G.shotgun.recipe[0], G.shotgun.recipe[1]).id('kubejs:factory_ascension/shotgun')
    event.shaped(Item.of(FA_MOD + ':shotgun_shell', G.shellsPerCraft), G.shells[0], G.shells[1]).id('kubejs:factory_ascension/shotgun_shell')
    event.shaped(FA_MOD + ':flare_gun', G.flareGun.recipe[0], G.flareGun.recipe[1]).id('kubejs:factory_ascension/flare_gun')
    event.shaped(FA_MOD + ':weapons_workbench', G.workbench.recipe[0], G.workbench.recipe[1]).id('kubejs:factory_ascension/weapons_workbench')
    event.shaped(Item.of('minecraft:enchanted_book').enchant(FA_MOD + ':jury_rigged', 1), G.workbench.juryRiggedBook[0], G.workbench.juryRiggedBook[1])
      .id('kubejs:factory_ascension/jury_rigged_book')
    // The guns themselves are built at the Weapons Workbench (GUNSMITH tab), from each one's `cost`
    event.shaped(Item.of(FA_MOD + ':frag_grenade', G.fragGrenadesPerCraft), G.fragGrenade[0], G.fragGrenade[1]).id('kubejs:factory_ascension/frag_grenade')
    event.shaped(Item.of(FA_MOD + ':bandage', G.bandagesPerCraft), G.bandage[0], G.bandage[1]).id('kubejs:factory_ascension/bandage')
    event.shaped(FA_MOD + ':medkit', G.medkit[0], G.medkit[1]).id('kubejs:factory_ascension/medkit')
    event.shaped(Item.of(FA_MOD + ':flashbang', G.flashbangsPerCraft), G.flashbang[0], G.flashbang[1]).id('kubejs:factory_ascension/flashbang')
    event.shaped(FA_MOD + ':plate_carrier', G.plateCarrier[0], G.plateCarrier[1]).id('kubejs:factory_ascension/plate_carrier')
    Object.keys(G.plates).forEach(p => event.shaped(FA_MOD + ':' + p, G.plates[p][0], G.plates[p][1]).id('kubejs:factory_ascension/' + p))
    event.shaped(FA_MOD + ':nods_green', G.nodsGreen[0], G.nodsGreen[1]).id('kubejs:factory_ascension/nods_green')
    event.shaped(FA_MOD + ':nods_white', G.nodsWhite[0], G.nodsWhite[1]).id('kubejs:factory_ascension/nods_white')
    let RD = global.FA.raids
    event.shaped(Item.of(FA_MOD + ':ballistic_concrete', RD.ballisticConcretePerCraft), RD.ballisticConcrete[0], RD.ballisticConcrete[1]).id('kubejs:factory_ascension/ballistic_concrete')
    event.shaped(Item.of(FA_MOD + ':ballistic_glass', RD.ballisticGlassPerCraft), RD.ballisticGlass[0], RD.ballisticGlass[1]).id('kubejs:factory_ascension/ballistic_glass')
    event.shaped(Item.of(FA_MOD + ':ballistic_glass_pane', 16), ['GGG', 'GGG'], { G: FA_MOD + ':ballistic_glass' }).id('kubejs:factory_ascension/ballistic_glass_pane')
    event.shaped(FA_MOD + ':steel_door', RD.steelDoor[0], RD.steelDoor[1]).id('kubejs:factory_ascension/steel_door')
    event.shaped(FA_MOD + ':steel_trapdoor', RD.steelTrapdoor[0], RD.steelTrapdoor[1]).id('kubejs:factory_ascension/steel_trapdoor')
    let VR = global.FA.vehicles.recipes
    Object.keys(VR).forEach(id => {
      if (!Array.isArray(VR[id])) return
      let n = id == 'fuel_can' ? VR.fuelCansPerCraft : 1
      event.shaped(Item.of(FA_MOD + ':' + id, n), VR[id][0], VR[id][1]).id('kubejs:factory_ascension/' + id)
    })
    let DR = global.FA.drugs
    event.shaped(FA_MOD + ':joint', DR.jointRecipe[0], DR.jointRecipe[1]).id('kubejs:factory_ascension/joint')
    event.shaped(FA_MOD + ':chem_lab', DR.chemLab[0], DR.chemLab[1]).id('kubejs:factory_ascension/chem_lab')
    // Plain rounds of every caliber: a nugget bullet, gunpowder, a copper case
    Object.keys(G.ammo.perCraft).forEach(cal => {
      let round = Item.of(FA_MOD + ':cartridge', G.ammo.perCraft[cal],
        '{Cal:"' + cal + '",Type:"REGULAR",Prop:"minecraft:gunpowder",Case:"minecraft:copper_ingot",Color:13140028,Power:1.0f,Rel:1.0f}')
      let mk = G.ammo.markers[cal]
      let keys = Object.assign({}, G.ammo.recipe)
      if (mk) keys.M = mk
      event.shaped(round, mk ? ['NM', 'G ', 'C '] : ['N', 'G', 'C'], keys).id('kubejs:factory_ascension/round_' + cal)
    })
    event.shaped(Item.of(FA_MOD + ':flare_red', G.flares.perCraft), G.flares.red[0], G.flares.red[1]).id('kubejs:factory_ascension/flare_red')
    event.shaped(Item.of(FA_MOD + ':flare_green', G.flares.perCraft), G.flares.green[0], G.flares.green[1]).id('kubejs:factory_ascension/flare_green')
    let GL = global.FA.machines.glass
    Object.keys(GL).forEach(g => event.shaped(Item.of(FA_MOD + ':' + g, 8), ['GGG', 'GCG', 'GGG'], { G: 'minecraft:glass', C: GL[g] })
      .id('kubejs:factory_ascension/glass_' + g))
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

  // ---- Upgrades: Ascension Vault (more slots) and Super Smelter (more lanes) ----
  // Right-click the block holding the next upgrade's items. faGetTier / faSetTier live in the mod.
  let faUpgrades = (blockId, ups, label) => BlockEvents.rightClicked(blockId, event => {
    let player = event.player
    let held = player.getMainHandItem()
    if (!held || held.isEmpty()) return
    let be = event.level.getBlockEntity(event.block.pos)
    if (!be || !be.faGetTier) return
    let tier = be.faGetTier()
    let up = ups[tier]
    if (!ups.some(u => u[0] == String(held.getId()))) return
    if (!up) {
      player.displayClientMessage(Text.yellow('Fully upgraded (' + label(be) + ').'), true)
    } else if (String(held.getId()) != up[0] || held.getCount() < up[1]) {
      player.displayClientMessage(Text.red('The next upgrade needs ' + up[1] + ' ' + Item.of(up[0]).getHoverName().getString() + '.'), true)
    } else {
      held.shrink(up[1])
      be.faSetTier(tier + 1)
      faSound(player, 'minecraft:block.smithing_table.use', 0.8)
      player.tell(Text.gold('Upgraded: ').append(Text.white(label(be))))
    }
    event.cancel()
  })
  faUpgrades(FA_VAULT_ID, global.FA.machines.vaultUpgrades, be => be.faGetCapacity() + ' slots')
  faUpgrades(FA_MOD + ':super_smelter', global.FA.machines.smelter.upgrades, be => global.FA.machines.smelter.tiers[be.faGetTier()][0] + ' items at once')

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

  // ---- Salvager: /ascension sell as a machine ----
  // Each run: up to itemsPerRun items are priced one at a time (faSellValue, 17_fa_sell.js) and their value goes into the
  // salvager's Essence bank; whole cores are paid into the outputs as Singularities, Dense Cores and Cores.
  let faMoveOut = (output, stack, simulate) => {
    let left = stack
    for (let i = 0; i < output.getSlots() && !left.isEmpty(); i++) left = output.insertItem(i, left, simulate)
    return left
  }
  let faRunSalvager = (level, pos) => {
    let sal = level.getBlockEntity(pos)
    if (!sal || !sal.faInput) return 0
    let S = global.FA.machines.salvager
    let input = sal.faInput(), output = sal.faOutput()
    let bank = sal.faGetBank(), count = 0, value = 0
    for (let n = 0; n < S.itemsPerRun; n++) {
      let slot = -1
      for (let i = 0; i < input.getSlots(); i++) if (!input.getStackInSlot(i).isEmpty()) { slot = i; break }
      if (slot < 0) break
      let stack = input.getStackInSlot(slot)
      let one = stack.copyWithCount(1)
      let q = (!S.artifacts && global.FA_isArtifact(one)) ? { value: 0 } : faSellValue(one)
      if (q.value <= 0) {
        // Can't (or won't) sell it: pass the whole stack through, if there's room
        let left = faMoveOut(output, stack.copy(), true)
        if (left.getCount() == stack.getCount()) break
        faMoveOut(output, input.extractItem(slot, stack.getCount() - left.getCount(), false), false)
        continue
      }
      input.extractItem(slot, 1, false)
      bank += q.value
      value += q.value
      count++
    }
    // Pay whole cores, biggest first, as far as the outputs have room
    let whole = Math.floor(bank)
    ;[['kubejs:ascension_singularity', 81], ['kubejs:dense_ascension_core', 9], ['kubejs:ascension_core', 1]].forEach(u => {
      let n = Math.floor(whole / u[1])
      while (n > 0) {
        let batch = Math.min(n, 64)
        let left = faMoveOut(output, Item.of(u[0], batch), false)
        let paid = batch - left.getCount()
        whole -= paid * u[1]
        bank -= paid * u[1]
        n -= batch
        if (paid < batch) break
      }
    })
    sal.faSetBank(bank)
    sal.faReport(count, value)
    if (count > 0) {
      let at = (pos.getX() + 0.5) + ' ' + (pos.getY() + 1.05) + ' ' + (pos.getZ() + 0.5)
      level.getServer().runCommandSilent('execute in ' + level.dimension + ' run particle minecraft:crit ' + at + ' 0.25 0.1 0.25 0.05 6 normal')
    }
    return count
  }
  global.FA_runSalvager = (level, pos) => faRunSalvager(level, pos)

  // ---- Tome Fusion Table ----
  // What the table's slots make: { result, text, ok, use: [[slot, count]], cores }
  let faFusionPlan = items => {
    let F = global.FA.machines.fusionTable
    let E = global.FA.enchants
    let tomes = []
    for (let i = 0; i < 3; i++) {
      let st = items.getStackInSlot(i)
      let id = faTomeId(st)
      if (id) tomes.push({ slot: i, stack: st, id: id, count: st.getCount(), level: faAppliedLevel(st, id) })
    }
    let cores = items.getStackInSlot(3).isEmpty() ? 0 : items.getStackInSlot(3).getCount()
    let none = text => ({ result: Item.empty, text: text, ok: false })
    if (tomes.length == 0) return none('')
    let ids = tomes.map(t => t.id).filter((id, k, a) => a.indexOf(id) == k)
    if (ids.length == 1) {
      // Merge: every tome in the slots adds its applied level (foil included)
      let id = ids[0]
      let total = tomes.reduce((a, t) => a + t.count, 0)
      if (total < 2) return none('Add another ' + E[id].name + ' tome to merge, or a Fusion partner')
      let max = faMaxLevel(id)
      let level = Math.min(max, tomes.reduce((a, t) => a + t.count * t.level, 0))
      if (tomes.every(t => t.level >= max)) return none(E[id].name + ' is already at its max level')
      let plan = { result: faMakeTome(id, level), use: tomes.map(t => [t.slot, t.count]), cores: F.mergeCores }
      plan.text = 'Merge ' + total + ' tomes: ' + E[id].name + ' ' + global.FA_num(level)
      plan.ok = cores >= F.mergeCores
      if (!plan.ok) plan.text += ' (needs ' + F.mergeCores + ' cores)'
      return plan
    }
    // A Fusion / Triad from its parents: one of each
    let key = ids.slice().sort().join('+')
    let fid = Object.keys(E).filter(k => E[k].type == 'fusion' && E[k].parts && E[k].parts.slice().sort().join('+') == key)[0]
    if (!fid) return none('No Fusion uses ' + ids.map(id => E[id].name).join(' + '))
    let need = E[fid].triad ? F.triadCores : F.fusionCores
    let level = F.keepLevel ? Math.min.apply(null, tomes.map(t => t.level)) : 1
    level = Math.min(faMaxLevel(fid), Math.max(1, level))
    let plan = { result: faMakeTome(fid, level), use: ids.map(id => [tomes.filter(t => t.id == id)[0].slot, 1]), cores: need }
    plan.text = (E[fid].triad ? 'Triad: ' : 'Fusion: ') + E[fid].name + (level > 1 ? ' ' + global.FA_num(level) : '')
    plan.ok = cores >= need
    if (!plan.ok) plan.text += ' (needs ' + need + ' core' + (need > 1 ? 's' : '') + ')'
    return plan
  }

  let faFusionPreview = table => {
    let plan = faFusionPlan(table.faItems())
    table.faSetPreview(plan.result, plan.text, plan.ok && table.faItems().getStackInSlot(4).isEmpty())
    return plan
  }

  let faFuse = (table, player) => {
    let items = table.faItems()
    let plan = faFusionPlan(items)
    if (!plan.ok || !items.getStackInSlot(4).isEmpty()) {
      faFusionPreview(table)
      return 0
    }
    plan.use.forEach(u => items.extractItem(u[0], u[1], false))
    if (plan.cores > 0) items.extractItem(3, plan.cores, false)
    items.setStackInSlot(4, plan.result.copy())
    if (player) {
      faSound(player, 'minecraft:block.enchantment_table.use', 1.2)
      player.displayClientMessage(Text.lightPurple(plan.text.replace(/ \(needs.*$/, '')), true)
    }
    faFusionPreview(table)
    return 1
  }

  // ---- Ascension Workbench: gear in the top slot, a tome / gem / attachment / Chaos Tome / Polishing Cloth below ----
  // The preview says what Apply will do (or why it can't); Apply and Star Up run the same rules as right-clicking.
  let faBenchPlan = (gear, mod) => {
    if (!gear || gear.isEmpty()) return { ok: false, text: 'Put a weapon, tool or piece of armor in the top slot.' }
    if (!mod || mod.isEmpty()) return { ok: false, text: 'Add a tome, gem, attachment, Chaos Tome or Polishing Cloth below it.' }
    let id = String(mod.getId())
    let kind = faIsGear(gear) ? faGearKind(gear) : ''
    let tomeId = faTomeId(mod)
    if (tomeId) {
      let e = global.FA.enchants[tomeId]
      if (!kind) return { ok: false, text: 'Tomes only go on gear.' }
      if (!global.FA_enchantFits(tomeId, kind)) return { ok: false, text: e.name + ' goes on ' + global.FA_fitsText(tomeId) + '.' }
      let level = faAppliedLevel(mod, tomeId), had = faGetLevel(gear, tomeId)
      if (had >= level) return { ok: false, text: 'It already has ' + e.name + ' ' + global.FA_num(had) + '.' }
      return { ok: true, text: 'Apply ' + e.name + ' ' + global.FA_num(level) + (had ? ' (swaps out level ' + global.FA_num(had) + ')' : '') + ' for ' + global.FA_num(global.FA_cost(0, tomeId)) + ' Essence', act: 'tome', id: tomeId }
    }
    let gem = /^kubejs:gem_([a-z_]+)_(\d)$/.exec(id)
    if (gem) {
      let G = global.FA.gems, T = G.types[gem[1]]
      if (!kind || !T) return { ok: false, text: 'Gems only go in gear.' }
      if (faGemsOf(gear).length >= G.sockets) return { ok: false, text: 'All ' + G.sockets + ' sockets are full.' }
      return { ok: true, text: 'Socket it: +' + global.FA_gemLevels(parseInt(gem[2])) + ' ' + global.FA.enchants[T.enchant].name, act: 'gem', type: gem[1], tier: parseInt(gem[2]) }
    }
    if (id.indexOf('kubejs:attachment_') == 0) {
      let def = global.FA_attachmentDef(id.slice(18))
      if (!def) return { ok: false, text: 'Unknown attachment.' }
      if (global.FA_gearSlot(gear) != def.fits) return { ok: false, text: 'A ' + def.name + ' doesn\'t fit this.' }
      let have = global.FA_attachIds(gear)
      if (have.indexOf(def.id) >= 0) return { ok: false, text: 'It already has a ' + def.name + '.' }
      if (have.length >= global.FA.attachments.slots) return { ok: false, text: 'No free attachment slots.' }
      return { ok: true, text: 'Attach: ' + global.FA_attachmentEffect(def), act: 'attach', id: def.id }
    }
    if (id == 'kubejs:chaos_tome') return kind ? { ok: true, text: 'Roll a random enchant onto it for ' + global.FA_num(global.FA_rollCost(0, 0)) + ' Essence', act: 'chaos' }
      : { ok: false, text: 'Chaos Tomes only roll onto gear.' }
    if (id == 'kubejs:polishing_cloth') return gear.getMaxDamage() > 0 ? { ok: true, text: 'Polish it: reroll its condition, keeping the better grade', act: 'polish' }
      : { ok: false, text: 'Only gear with durability can be polished.' }
    return { ok: false, text: 'That doesn\'t go on gear.' }
  }

  let faBenchPreview = be => {
    let items = be.faItems()
    let plan = faBenchPlan(items.getStackInSlot(0), items.getStackInSlot(1))
    be.faSetPreview(plan.text, plan.ok)
  }

  let faBenchApply = (be, player) => {
    if (!player) return
    let items = be.faItems()
    let gear = items.getStackInSlot(0), mod = items.getStackInSlot(1)
    let plan = faBenchPlan(gear, mod)
    if (!plan.ok) { be.faSetPreview(plan.text, false); return }
    if (plan.act == 'tome') faUnlock(player, mod, plan.id, gear)
    else if (plan.act == 'gem') faSocket(player, mod, plan.type, plan.tier, gear)
    else if (plan.act == 'attach') faAttach(player, mod, plan.id, gear)
    else if (plan.act == 'chaos') faChaosRoll(player, mod, gear)
    else if (plan.act == 'polish') faPolish(player, mod, gear)
    be.faChanged()
  }

  let faBenchStar = (be, player) => {
    if (!player) return
    let gear = be.faItems().getStackInSlot(0)
    let check = global.FA_legendCheck(player, gear)
    if (check.ok && global.FA_legendUp(player, gear)) be.faChanged()
    else be.faSetPreview(check.text, false)
  }

  global.FA_onMachine = event => {
    let kind = String(event.getKind())
    let be = event.getBlockEntity()
    if (!be) return
    if (kind == 'salvage') faRunSalvager(event.getLevel(), event.getPos())
    else if (kind == 'preview' && be.faItems && !be.faIsBench) faFusionPreview(be)
    else if (kind == 'fuse' && be.faItems) faFuse(be, event.hasPlayer() ? event.getPlayer() : false)
    else if (kind == 'bench_preview') faBenchPreview(be)
    else if (kind == 'bench_apply') faBenchApply(be, event.hasPlayer() ? event.getPlayer() : false)
    else if (kind == 'bench_star') faBenchStar(be, event.hasPlayer() ? event.getPlayer() : false)
  }
  global.FA_benchPlanText = (gear, mod) => faBenchPlan(gear, mod).text
  global.FA_fusionPlanText = table => faFusionPlan(table.faItems()).text
  global.FA_fuseTable = (table, player) => faFuse(table, player)
}
