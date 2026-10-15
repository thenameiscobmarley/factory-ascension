// priority: 1500
// Factory Ascension - missing-mod guard. Each world remembers the mods that added its blocks and items. If one of them
// is missing when the world loads (forgot to install it, an update dropped it...), Minecraft drops that mod's blocks
// and items as they load, and nothing inside the game can stop that. So chat says so straight away:
//   [Save last files]     what to do: quit, install the mod, and the last backup (made by the Factory Ascension mod
//                         whenever a world closes, in backups/factoryascension/) has anything that's gone
//   [Keep playing anyway] the world stops expecting that mod
// For the Factory Ascension jar itself, the scripts also register placeholders under its ids (10_fa_items.js), so its
// blocks and items survive; only what's inside its machines needs the backup.
// Works without the mod jar (it's often the jar that's missing), so it lives in the scripts.

const $FAGuardRegistries = Java.loadClass('net.minecraftforge.registries.ForgeRegistries')
const FA_GUARD_SKIP = { minecraft: true, forge: true, kubejs: true }   // always there (KubeJS's own items come from these scripts)

let faMissingMods = []   // [[id, name]] missing this session

// Installed mods that have blocks or items in the game right now: { id: name }
function faContentMods() {
  let ids = {}
  ;[$FAGuardRegistries.ITEMS, $FAGuardRegistries.BLOCKS].forEach(reg => {
    let it = reg.getKeys().iterator()
    while (it.hasNext()) ids[String(it.next().getNamespace())] = true
  })
  let out = {}
  Object.keys(ids).forEach(id => {
    // Only mods that are really installed (the placeholders use the Factory Ascension mod's ids without it)
    if (FA_GUARD_SKIP[id] || !Platform.isLoaded(id)) return
    let info = Platform.getInfo(id)
    out[id] = info ? String(info.getName()) : id
  })
  return out
}

function faSeenMods(server) {
  let data = server.persistentData
  if (!data.contains('fa_mods_seen')) data.put('fa_mods_seen', data.getCompound('fa_mods_seen'))
  return data.getCompound('fa_mods_seen')
}

ServerEvents.loaded(event => {
  try {
    let server = event.server
    let now = faContentMods()
    let seen = faSeenMods(server)
    faMissingMods = []
    seen.getAllKeys().forEach(id => {
      id = String(id)
      if (!now[id]) faMissingMods.push([id, String(seen.getString(id))])
    })
    if (faMissingMods.length) console.warn('[Factory Ascension] This world was played with mods that are missing now: ' + faMissingMods.map(m => m[1]).join(', '))
    // New mods join the list; missing ones stay on it until a player says to stop expecting them
    Object.keys(now).forEach(id => seen.putString(id, now[id]))
  } catch (err) {
    console.error('[Factory Ascension] mod guard: ' + err)
  }
})

function faGuardNames() {
  let names = faMissingMods.map(m => m[1])
  return names.length <= 1 ? names.join('') : names.slice(0, -1).join(', ') + ' and ' + names[names.length - 1]
}

function faGuardWarn(player) {
  let fa = faMissingMods.some(m => m[0] == 'factoryascension')
  player.tell(Text.gold('⚠ Oops, it looks like you\'ve forgotten to install ').append(Text.yellow(faGuardNames()).bold(true))
    .append(Text.gold('. Save last files so you don\'t lose progress?')))
  player.tell(Text.gray(fa ? '  Its blocks and items are kept as placeholders until it\'s back, but what\'s inside its machines isn\'t. '
      : '  Its blocks and items in this world can\'t load without it. ')
    .append(faButton('Save last files', '/ascension modguard save', 'How to get everything back'))
    .append(Text.gray(' '))
    .append(faButton('Keep playing anyway', '/ascension modguard ignore', 'Stop asking: this world won\'t expect that mod any more')))
}

PlayerEvents.loggedIn(event => {
  if (faMissingMods.length) faGuardWarn(event.player)
})

ServerEvents.commandRegistry(event => {
  let { commands: Commands } = event
  event.register(Commands.literal('ascension').then(Commands.literal('modguard')
    .then(Commands.literal('save').executes(ctx => {
      let player = ctx.source.getPlayerOrException()
      if (!faMissingMods.length) { player.tell(Text.gray('Nothing is missing: this world has all its mods.')); return 0 }
      player.tell(Text.green('✔ Here\'s how to keep everything:'))
      player.tell(Text.gray('  1. Quit this world now (the less you play, the less changes). 2. Install ').append(Text.white(faGuardNames()))
        .append(Text.gray('. 3. Open the world again.')))
      player.tell(Text.gray('  Anything missing after that? The Factory Ascension mod backs each world up whenever it closes, in ')
        .append(Text.white('backups/factoryascension/')).append(Text.gray(' in your game folder: unzip the newest one from before this into saves/.')))
      return 1
    }))
    .then(Commands.literal('ignore').executes(ctx => {
      let player = ctx.source.getPlayerOrException()
      if (!faMissingMods.length) { player.tell(Text.gray('Nothing is missing: this world has all its mods.')); return 0 }
      let seen = faSeenMods(player.getServer())
      faMissingMods.forEach(m => seen.remove(m[0]))
      let names = faGuardNames()
      faMissingMods = []
      player.tell(Text.yellow('Okay. ').append(Text.gray('This world no longer expects ' + names + '.')))
      return 1
    }))))
})

// ---- Safety net (the mod jar has its own, EntitySanity): an entity whose position or motion became NaN or infinite
// makes mobs looking for it try to load a chunk that can never load, freezing the server. Remove it first.
if (!Platform.isLoaded('factoryascension') && typeof LevelEvents != 'undefined') {
  let faBad = v => !isFinite(v) || Math.abs(v) > 3.0e7
  LevelEvents.tick(event => {
    try {
      let level = event.level
      if (level.isClientSide()) return
      let it = level.getAllEntities().iterator()
      let fix = []
      while (it.hasNext()) {
        let e = it.next()
        if (!e) continue
        let m = e.getDeltaMovement()
        if (faBad(e.getX()) || faBad(e.getZ()) || !isFinite(e.getY()) || !isFinite(m.x()) || !isFinite(m.y()) || !isFinite(m.z())) fix.push(e)
      }
      fix.forEach(e => {
        console.warn('[Factory Ascension] Removed ' + e.type + ' with a broken position (it would freeze the server)')
        if (e.isPlayer && e.isPlayer()) {
          let sp = level.getSharedSpawnPos()
          e.setMotion(0, 0, 0)
          e.setPositionAndRotation(sp.getX() + 0.5, sp.getY() + 1, sp.getZ() + 0.5, 0, 0)
        } else e.discard()
      })
    } catch (err) {
      console.error('[Factory Ascension] safety net: ' + err)
    }
  })
}

// For /ascension selftest
global.FA_contentMods = () => faContentMods()
global.FA_missingMods = () => faMissingMods.map(m => m[0])
