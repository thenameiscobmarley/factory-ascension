// Offline test harness: runs the KubeJS scripts in plain Rhino against small fakes of the
// KubeJS / Minecraft API, so syntax errors and logic bugs show up without launching the game.
// It cannot prove the real API calls exist - that still needs an in-game test.
//
// Usage (from repo root):  java -jar rhino-1.7.15.jar -version 200 tools/test/harness.js

var failures = 0
function check(name, ok, detail) {
  if (ok) print('  ok   ' + name)
  else { failures++; print('  FAIL ' + name + (detail !== undefined ? '  -> ' + detail : '')) }
}
function near(a, b) { return Math.abs(a - b) < 1e-6 * Math.max(1, Math.abs(b)) }

// ---------------- Fakes ----------------
var handlers = { registry: [], recipes: [], rightClicked: {}, commands: [], tooltip: [], forge: {} }
var global = {}
var logged = []
var console = { info: function (m) { print('  [info] ' + m) }, warn: function (m) { logged.push(m) }, error: function (m) { logged.push(m); if (!quiet) print('  [error] ' + m) } }
var quiet = false

function fakeText(s) {
  return {
    s: String(s), click: null,
    append: function (o) { var t = fakeText(this.s + (o.s !== undefined ? o.s : o)); t.clicks = (this.clicks || []).concat(o.clicks || []); return t },
    bold: function () { return this },
    clickRunCommand: function (c) { this.clicks = (this.clicks || []).concat([c]); return this },
    hover: function () { return this }
  }
}
var Text = {}
;['gold', 'gray', 'darkGray', 'lightPurple', 'red', 'darkRed', 'green', 'aqua', 'yellow', 'white', 'blue', 'of', 'darkAqua', 'darkPurple', 'darkGreen', 'darkBlue'].forEach(function (c) { Text[c] = fakeText })

var KNOWN_ITEMS = { 'mekanism:raw_osmium': 1, 'mekanism:raw_lead': 1, 'mekanism:raw_tin': 1, 'mekanism:raw_uranium': 1, 'tfmg:steel_ingot': 1, 'tfmg:raw_nickel': 1 }
var Item = { of: function (id, n) { return { id: id, count: n || 1, isEmpty: function () { return !KNOWN_ITEMS[id] } } } }
var Ingredient = { all: 'ALL' }
var Platform = { isLoaded: function (m) { return m == 'tfmg' || m == 'mekanism' } }

function Compound() { this.d = {} }
Compound.prototype = {
  contains: function (k) { return k in this.d },
  getCompound: function (k) { return this.d[k] instanceof Compound ? this.d[k] : new Compound() },
  getInt: function (k) { return this.d[k] || 0 },
  getDouble: function (k) { return this.d[k] || 0 },
  putInt: function (k, v) { this.d[k] = v | 0 },
  putDouble: function (k, v) { this.d[k] = v },
  putBoolean: function (k, v) { this.d[k] = v },
  putString: function (k, v) { this.d[k] = String(v) },
  getString: function (k) { return typeof this.d[k] == 'string' ? this.d[k] : '' },
  copy: function () { var c = new Compound(); for (var k in this.d) c.d[k] = this.d[k] instanceof Compound ? this.d[k].copy() : this.d[k]; return c },
  remove: function (k) { delete this.d[k] },
  getBoolean: function (k) { return !!this.d[k] },
  put: function (k, v) { this.d[k] = v },
  getAllKeys: function () { return javaList(Object.keys(this.d)) }
}

function PlayerClass() {}
function LivingClass() {}
function EnemyClass() {}
function RaiderClass() {}
function OrbClass() {}
var MobType = { UNDEAD: 'undead', ARTHROPOD: 'arthropod', WATER: 'water', UNDEFINED: 'undefined' }
function ItemEntityClass(level, x, y, z, stack) { this.s = stack; this.setDefaultPickUpDelay = function () {} }
ItemEntityClass.prototype.getItem = function () { return this.s }
var Java = {
  loadClass: function (n) {
    var classes = {
      'net.minecraft.world.entity.player.Player': PlayerClass,
      'net.minecraft.world.entity.item.ItemEntity': ItemEntityClass,
      'net.minecraft.tags.DamageTypeTags': { BYPASSES_INVULNERABILITY: 'bypass', IS_PROJECTILE: 'projectile', IS_FIRE: 'fire', IS_FALL: 'fall', IS_EXPLOSION: 'explosion' },
      'net.minecraft.world.entity.ai.attributes.Attributes': { MOVEMENT_SPEED: 'movement_speed', ATTACK_SPEED: 'attack_speed', ATTACK_KNOCKBACK: 'attack_knockback', LUCK: 'luck' },
      'net.minecraft.world.entity.LivingEntity': LivingClass,
      'net.minecraft.world.entity.monster.Enemy': EnemyClass,
      'net.minecraft.world.entity.raid.Raider': RaiderClass,
      'net.minecraft.world.entity.MobType': MobType,
      'net.minecraft.world.entity.ExperienceOrb': OrbClass,
      'net.minecraft.world.effect.MobEffects': { MOVEMENT_SLOWDOWN: 'slowness', WEAKNESS: 'weakness', WITHER: 'wither', POISON: 'poison', LEVITATION: 'levitation', MOVEMENT_SPEED: 'speed', NIGHT_VISION: 'night_vision', WATER_BREATHING: 'water_breathing', FIRE_RESISTANCE: 'fire_resistance', DIG_SPEED: 'haste', JUMP: 'jump', SATURATION: 'saturation' },
      'net.minecraft.world.effect.MobEffectInstance': function (effect, ticks, amp) { this.effect = effect; this.ticks = ticks; this.amp = amp },
      'net.minecraft.world.entity.EntityType': {
        LIGHTNING_BOLT: { create: function () { return { moveTo: function () {}, setVisualOnly: function (v) { this.visual = v } } } },
        getKey: function (type) { return type.id }
      },
      'net.minecraft.tags.BlockTags': { LOGS: 'logs' },
      'net.minecraftforge.common.Tags$Blocks': { ORES: 'ores' },
      'net.minecraft.server.level.ServerBossEvent': FakeBossBar,
      'net.minecraft.world.BossEvent$BossBarColor': { RED: 'red' },
      'net.minecraft.world.BossEvent$BossBarOverlay': { NOTCHED_10: 'n10' },
      'net.minecraft.world.level.levelgen.Heightmap$Types': { MOTION_BLOCKING: 'mb' },
      'net.minecraft.core.BlockPos': FakePos,
      'net.minecraftforge.common.capabilities.ForgeCapabilities': { ITEM_HANDLER: 'items', FLUID_HANDLER: 'fluids' },
      'net.minecraft.core.Direction': { UP: 'up', DOWN: 'down' },
      'net.minecraftforge.fluids.FluidStack': function (fluid, amount) { this.fluid = fluid; this.amount = amount; this.getAmount = function () { return this.amount } },
      'net.minecraftforge.fluids.capability.IFluidHandler$FluidAction': { SIMULATE: 'sim', EXECUTE: 'exec' },
      'net.minecraft.core.registries.BuiltInRegistries': { FLUID: { get: function () { return 'liquid_essence' } }, BLOCK: { getKey: function (b) { return b } } },
      'net.minecraft.resources.ResourceLocation': function (ns, path) { this.s = ns + ':' + path },
      'net.minecraftforge.common.ForgeMod': { BLOCK_REACH: { get: function () { return 'block_reach' } }, ENTITY_REACH: { get: function () { return 'entity_reach' } }, STEP_HEIGHT_ADDITION: { get: function () { return 'step_height' } } },
      'net.minecraft.world.entity.ai.attributes.AttributeModifier': function (name, amount, op) {
        this.name = name; this.amount = amount; this.op = op
        this.getName = function () { return this.name }; this.getAmount = function () { return this.amount }
      },
      'net.minecraft.world.entity.ai.attributes.AttributeModifier$Operation': { ADDITION: 'add', MULTIPLY_BASE: 'mul_base' }
    }
    if (!(n in classes)) throw 'unknown class ' + n
    return classes[n]
  }
}

function FakeBossBar(name) {
  this.players = []; this.progress = 1; this.name = name
  this.setProgress = function (p) { this.progress = p }
  this.addPlayer = function (p) { if (this.players.indexOf(p) < 0) this.players.push(p) }
  this.removePlayer = function (p) { this.players = this.players.filter(function (x) { return x !== p }) }
  this.removeAllPlayers = function () { this.players = [] }
  var self = this
  this.getPlayers = function () { return { toArray: function () { return self.players.slice() } } }
}
function FakePos(x, y, z) { this.x = x; this.y = y; this.z = z }
FakePos.prototype = {
  getX: function () { return this.x }, getY: function () { return this.y }, getZ: function () { return this.z },
  above: function () { return new FakePos(this.x, this.y + 1, this.z) }, below: function () { return new FakePos(this.x, this.y - 1, this.z) },
  offset: function (a, b, c) { return new FakePos(this.x + a, this.y + b, this.z + c) }
}
var StartupEvents = { registry: function (type, fn) { handlers.registry.push({ type: type, fn: fn }) } }
var PlayerEvents = { loggedIn: function (fn) { (handlers.loggedIn = handlers.loggedIn || []).push(fn) } }
var BlockEvents = { placed: function (id, fn) { handlers.placed = fn }, broken: function (id, fn) { handlers.broken = fn } }
var ServerEvents = { recipes: function (fn) { handlers.recipes.push(fn) }, commandRegistry: function (fn) { handlers.commands.push(fn) }, tick: function (fn) { handlers.tick = fn } }
var ItemEvents = { rightClicked: function (id, fn) { handlers.rightClicked[id] = fn }, tooltip: function (fn) { handlers.tooltip.push(fn) } }
// Several scripts can hook the same Forge event: keep them all and call each in turn
var ForgeEvents = { onEvent: function (cls, fn) {
  var k = cls.split(/[.$]/).pop(), prev = handlers.forge[k]
  handlers.forge[k] = prev ? function (e) { prev(e); fn(e) } : fn
} }

function javaList(arr) {
  return {
    arr: arr,
    iterator: function () { var i = 0, a = this.arr; return { hasNext: function () { return i < a.length }, next: function () { return a[i++] } } },
    add: function (x) { this.arr.push(x) },
    size: function () { return this.arr.length },
    toArray: function () { return this.arr.slice() }
  }
}

function stack(count, max, name) {
  return {
    count: count, max: max || 64, tag: null, name: name || 'Thing',
    isEmpty: function () { return this.count <= 0 },
    getCount: function () { return this.count },
    getMaxStackSize: function () { return this.max },
    shrink: function (n) { this.count -= n },
    copyWithCount: function (n) { var c = stack(n, this.max, this.name); c.tag = this.tag; c.id = this.id; return c },
    getTag: function () { return this.tag },
    setTag: function (t) { this.tag = t },
    hasTag: function () { return !!this.tag },
    getOrCreateTag: function () { if (!this.tag) this.tag = new Compound(); return this.tag },
    getHoverName: function () { return fakeText(this.name) }
  }
}
function gear(name) { return stack(1, 1, name) }

function makeServer() {
  var data = new Compound()
  return { persistentData: data, commands: [], told: [], runCommandSilent: function (c) { this.commands.push(c) }, tell: function (t) { this.told.push(t.s) } }
}

function makePlayer(server, uuid) {
  var p = new PlayerClass()
  var attrs = {}
  p.main = stack(0); p.off = stack(0); p.armor = [stack(0), stack(0), stack(0), stack(0)]
  p.health = 20; p.healed = 0; p.messages = []; p.bar = []; p.given = []; p.sneaking = false; p.attrs = attrs; p.effects = []
  p.getServer = function () { return server }
  p.pdata = new Compound()
  p.getPersistentData = function () { return this.pdata }
  p.getStringUUID = function () { return uuid }
  p.getScoreboardName = function () { return 'Tester' }
  p.isShiftKeyDown = function () { return this.sneaking }
  p.getMainHandItem = function () { return this.main }
  p.getOffhandItem = function () { return this.off }
  p.getAllSlots = function () { return javaList([this.main, this.off].concat(this.armor)) }
  p.getAttribute = function (a) {
    var list = attrs[a] = attrs[a] || []
    return {
      getModifiers: function () { return javaList(list.slice()) },
      removeModifier: function (m) { var i = list.indexOf(m); if (i >= 0) list.splice(i, 1) },
      addTransientModifier: function (m) { list.push(m) }
    }
  }
  livingBits(p, 20)
  p.isShiftKeyDown = function () { return this.sneaking }
  p.heal = function (h) { this.healed += h }
  p.abilities = { mayfly: false, flying: false, speed: 0.05, getFlyingSpeed: function () { return this.speed }, setFlyingSpeed: function (v) { this.speed = v } }
  p.abilityUpdates = 0
  p.getAbilities = function () { return this.abilities }
  p.onUpdateAbilities = function () { this.abilityUpdates++ }
  p.creative = false
  p.isCreative = function () { return this.creative }
  p.isSpectator = function () { return false }
  p.teleportTo = function (x, y, z) { this.teleported = [x, y, z] }
  p.fallDistance = 5
  p.displayClientMessage = function (t) { this.bar.push(t.s) }
  p.tell = function (t) { this.messages.push(t) }
  p.give = function (i) { this.given.push(i) }
  return p
}
// Shared living-entity behaviour for fake players and mobs
var gameTime = 1000
var bolts = []
var spawnedMinions = []
function livingBits(o, maxHealth) {
  o.hp = maxHealth; o.maxHp = maxHealth; o.hurtBy = []; o.effects = o.effects || []; o.fire = 0
  o.pdata = new Compound(); o.x = 0; o.z = 0
  o.getPersistentData = function () { return this.pdata }
  o.setCustomName = function (n) { this.customName = n.s }
  o.setCustomNameVisible = function (v) { this.nameVisible = v }
  o.typeId = 'minecraft:zombie'
  o.getType = function () { var self2 = this; return { id: self2.typeId, getDescription: function () { return fakeText('Zombie') }, create: function () { var m = makeMob(); spawnedMinions.push(m); return m } } }
  o.getServer = o.getServer || function () { return server }
  o.clearFire = function () { this.fire = 0 }
  o.setDeltaMovement = function (x, y, z) { this.motion = [x, y, z] }
  o.isRemoved = function () { return !!this.removed }
  o.moveTo = function (x, y, z) { this.x = x; this.z = z }
  o.getDisplayName = function () { return fakeText(this.customName || 'Zombie') }
  o.setPersistenceRequired = function () { this.persistent = true }
  o.mobType = MobType.UNDEFINED; o.ground = true; o.wet = false; o.dim = 'minecraft:overworld'; o.boss = false; o.nearby = []
  o.isAlive = function () { return true }
  o.getHealth = function () { return this.hp }
  o.setHealth = function (h) { this.hp = h }
  o.getMaxHealth = function () { return this.maxHp }
  o.hurt = function (src, amount) { this.hurtBy.push(amount); return true }
  o.addEffect = function (e) { this.effects.push(e) }
  o.setSecondsOnFire = function (s) { this.fire = s }
  o.getMobType = function () { return this.mobType }
  o.canChangeDimensions = function () { return !this.boss }
  o.isInWaterOrRain = function () { return this.wet }
  o.onGround = function () { return this.ground }
  o.getStringUUID = o.getStringUUID || function () { return 'mob' }
  o.displayClientMessage = o.displayClientMessage || function () {}
  var self = o
  o.level = function () {
    return {
      dimension: function () { return { location: function () { return self.dim } } },
      getGameTime: function () { return gameTime },
      getSharedSpawnPos: function () { return { getX: function () { return 0 }, getZ: function () { return 0 } } },
      isClientSide: function () { return false },
      addFreshEntity: function (e) { bolts.push(e) },
      getEntitiesOfClass: function (cls, box) { var l = self.nearby.filter(function (e) { return e instanceof cls }); return { size: function () { return l.length }, get: function (i) { return l[i] } } }
    }
  }
  o.getBoundingBox = function () { return { inflate: function () { return 'box' } } }
  o.getX = function () { return this.x }; o.getZ = function () { return this.z }; o.getY = function () { return 64 }
  o.damageSources = function () { return { thorns: function () { return 'thorns' }, playerAttack: function () { return 'playerAttack' } } }
  o.getAllSlots = o.getAllSlots || function () { return javaList([]) }
  return o
}
function makeMob(type) {
  var m = new (type || EnemyClass)()
  return livingBits(m, 20)
}
EnemyClass.prototype = Object.create(LivingClass.prototype)
RaiderClass.prototype = Object.create(EnemyClass.prototype)
PlayerClass.prototype = Object.create(LivingClass.prototype)

// ---------------- Load scripts in KubeJS order ----------------
;['startup_scripts/00_fa_config.js', 'startup_scripts/10_fa_items.js', 'startup_scripts/20_fa_enchant_effects.js',
  'startup_scripts/30_fa_world.js',
  'server_scripts/10_fa_ascension.js', 'server_scripts/20_fa_core_recipes.js', 'server_scripts/25_fa_tome_recipes.js',
  'server_scripts/30_fa_material_recipes.js', 'server_scripts/40_fa_progress.js', 'server_scripts/45_fa_altar.js',
  'client_scripts/10_fa_tooltips.js'].forEach(function (f) {
  load('kubejs/' + f)
  print('loaded ' + f)
})
var E = global.FA.enchants

// ---------------- Items & textures ----------------
print('\nitems')
var created = [], createdBlocks = [], createdFluids = []
var builder = {}
;['displayName', 'rarity', 'glow', 'maxStackSize', 'thinTexture', 'bucketColor', 'soundType', 'hardness', 'resistance', 'requiresTool',
  'tagBlock', 'lightLevel', 'textureAll'].forEach(function (m) { builder[m] = function () { return builder } })
handlers.registry.forEach(function (r) {
  var list = r.type == 'item' ? created : r.type == 'block' ? createdBlocks : createdFluids
  r.fn({ create: function (id) { list.push(id); return builder } })
})
check('Ascension Altar block and Liquid Essence fluid registered', createdBlocks.join() == 'ascension_altar' && createdFluids.join() == 'liquid_essence')
check('the altar has a block texture', new java.io.File('kubejs/assets/kubejs/textures/block/ascension_altar.png').exists())
var enchantCount = Object.keys(global.FA.enchants).length
var mythics = Object.keys(global.FA.enchants).filter(function (id) { return global.FA.enchants[id].mythic })
check('74 enchants (59 + 15 Mythic)', enchantCount == 74 && mythics.length == 15, enchantCount + ' / ' + mythics.length)
var gemCount = Object.keys(global.FA.gems.types).length * 3
check('items registered (4 core + blank + tomes + chaos + fragment + gems + 3 clusters)', created.length == 10 + enchantCount + gemCount, created.length)
var File = java.io.File
var missing = created.filter(function (id) { return !new File('kubejs/assets/kubejs/textures/item/' + id + '.png').exists() })
check('every item has a texture', missing.length == 0, missing.join(','))

// ---------------- Recipes ----------------
print('\nrecipes')
var recipes = []
var ev = {
  custom: function (j) { var r = { json: j, id: function (i) { r.rid = i; return r } }; recipes.push(r); return r },
  shapeless: function (o, i) { var r = { json: { type: 'shapeless' }, id: function (x) { r.rid = x; return r } }; recipes.push(r); return r },
  shaped: function (o, p, k) { var r = { json: { type: 'shaped', out: o, key: k }, id: function (x) { r.rid = x; return r } }; recipes.push(r); return r }
}
handlers.recipes.forEach(function (fn) { fn(ev) })
function byId(id) { return recipes.filter(function (r) { return r.rid == 'kubejs:factory_ascension/' + id })[0] }
check('core recipe has 4 steps with TFMG steel', byId('ascension_core').json.sequence.length == 4)
check('every recipe has an id', recipes.every(function (r) { return !!r.rid }))
var ids = recipes.map(function (r) { return r.rid })
check('recipe ids are unique', ids.filter(function (x, i) { return ids.indexOf(x) != i }).length == 0)
check('a tome recipe for every enchant', Object.keys(E).every(function (id) { return !!byId('tome_' + id) }))
check('crushing recipes have 1-4 results', recipes.filter(function (r) { return r.json.type == 'create:crushing' })
  .every(function (r) { return r.json.results.length >= 1 && r.json.results.length <= 4 }))
var mixes = recipes.filter(function (r) { return r.json.type == 'create:mixing' })
check('mixing recipes have <= 9 ingredients', mixes.every(function (r) { return r.json.ingredients.length <= 9 }))
// No mixing recipe's items may be a subset of another's, or the basin could pick the wrong one
// Fluids count too (as fluid:<id>), since the basin matches on both
function itemBag(r) { return r.json.ingredients.map(function (i) { return i.item || 'fluid:' + i.fluid }).sort() }
var basinRecipes = recipes.filter(function (r) { return r.json.type == 'create:mixing' || r.json.type == 'create:compacting' })
function isSubset(a, b) { var c = b.slice(); return a.every(function (x) { var i = c.indexOf(x); if (i < 0) return false; c.splice(i, 1); return true }) }
var clash = []
basinRecipes.forEach(function (a) { basinRecipes.forEach(function (b) { if (a !== b && a.json.type == b.json.type && isSubset(itemBag(a), itemBag(b))) clash.push(a.rid + ' in ' + b.rid) }) })
check('no basin recipe (mixing or compacting) is a subset of another', clash.length == 0, clash.join('; '))

// ---------------- Essence ----------------
print('\nessence')
var server = makeServer()
var p = makePlayer(server, 'uuid-1')
var cores = stack(64)
var cancelled = false
handlers.rightClicked['kubejs:ascension_core']({ player: p, item: cores, cancel: function () { cancelled = true } })
check('click absorbs 1 core', cores.count == 63 && faGetEssence(p) == 1 && cancelled)
p.sneaking = true
handlers.rightClicked['kubejs:ascension_singularity']({ player: p, item: stack(10), cancel: function () {} })
check('sneak absorbs whole stack (1 + 810)', faGetEssence(p) == 811, faGetEssence(p))
p.sneaking = false
var broken = makePlayer({ persistentData: { getDouble: function () { throw 'disk on fire' } } }, 'uuid-2')
handlers.rightClicked['kubejs:ascension_core']({ player: broken, item: stack(5), cancel: function () {} })
check('items are returned when absorbing fails', broken.given.length == 1)

// ---------------- Tomes ----------------
print('\ntomes')
var sword = gear('Diamond Sword')
var tome = stack(3)
p.main = tome; p.off = stack(0)
handlers.rightClicked['kubejs:tome_sharpness']({ player: p, item: tome, hand: 'MAIN_HAND', cancel: function () {} })
check('no gear in other hand: tome kept', tome.count == 3 && /other hand/.test(p.bar[p.bar.length - 1]))
p.off = stack(64)
handlers.rightClicked['kubejs:tome_sharpness']({ player: p, item: tome, hand: 'MAIN_HAND', cancel: function () {} })
check('stackable items cannot be enchanted', tome.count == 3)
p.off = sword
handlers.rightClicked['kubejs:tome_sharpness']({ player: p, item: tome, hand: 'MAIN_HAND', cancel: function () {} })
check('tome unlocks Sharpness 1 on off-hand gear', faGetLevel(sword, 'sharpness') == 1 && tome.count == 2)
check('unlock costs 1 Essence', faGetEssence(p) == 810, faGetEssence(p))
handlers.rightClicked['kubejs:tome_sharpness']({ player: p, item: tome, hand: 'MAIN_HAND', cancel: function () {} })
check('second unlock refused, tome kept', tome.count == 2 && faGetLevel(sword, 'sharpness') == 1)
// Tome in off hand, gear in main hand works too
var chest = gear('Netherite Chestplate')
var ptome = stack(1)
p.main = chest; p.off = ptome
handlers.rightClicked['kubejs:tome_protection']({ player: p, item: ptome, hand: 'OFF_HAND', cancel: function () {} })
check('tome in off hand enchants main-hand gear', faGetLevel(chest, 'protection') == 1 && ptome.count == 0)
var etome = stack(1)
p.main = etome; p.off = sword
handlers.rightClicked['kubejs:tome_eternal']({ player: p, item: etome, hand: 'MAIN_HAND', cancel: function () {} })
check('Eternal sets Unbreakable', sword.tag.d.Unbreakable === true)

// ---------------- Upgrades ----------------
print('\nupgrades')
p.main = sword; p.off = stack(0)
var before = faGetEssence(p)
faUpgrade(p, 'sharpness', 10)
check('+10 Sharpness -> 11', faGetLevel(sword, 'sharpness') == 11)
var expectSpent = 0
for (var L = 1; L < 11; L++) expectSpent += global.FA_cost(L)
check('spent the right Essence', near(before - faGetEssence(p), expectSpent), (before - faGetEssence(p)) + ' vs ' + expectSpent)
faUpgrade(p, 'sharpness', global.FA.maxUpgradesPerCommand)
var lvl = faGetLevel(sword, 'sharpness')
check('Max spends Essence until the next level is unaffordable', faGetEssence(p) < global.FA_cost(lvl) && lvl > 11, lvl + ' left ' + faGetEssence(p))
check('upgrade with no Essence refused', faUpgrade(p, 'sharpness', 1) == 0)
check('Eternal cannot go past 1', faUpgrade(p, 'eternal', 5) == 0 && faGetLevel(sword, 'eternal') == 1)
p.main = gear('Stick of Truth')
check('upgrade needs the enchant unlocked first', faUpgrade(p, 'sharpness', 1) == 0)
server.persistentData.putDouble('fa_essence_uuid-1', 1e12)
p.main = chest
faUpgrade(p, 'protection', 2999)
check('Protection 3000 is reachable', faGetLevel(chest, 'protection') == 3000)
check('cost curve: level 3000 -> 3001 costs 151', global.FA_cost(3000) == 151, global.FA_cost(3000))
var total = 0
for (var L2 = 0; L2 < 3000; L2++) total += global.FA_cost(L2)
print('  info total Essence for one enchant 0 -> 3000: ' + global.FA_num(total))

// ---------------- Menu & command ----------------
print('\nmenu')
p.messages = []
faShowMenu(p)
var menu = p.messages.map(function (t) { return t.s }).join('\n')
check('menu lists enchant with level', /Ascended Protection 3,000/.test(menu), menu)
var clicks = []
p.messages.forEach(function (t) { clicks = clicks.concat(t.clicks || []) })
check('menu has clickable upgrade buttons', clicks.indexOf('/ascension upgrade protection 100') >= 0, clicks.join(','))
print('  sample: ' + p.messages[2].s)
check('command registered', handlers.commands.length == 1)

// ---------------- Effects ----------------
print('\ndamage')
var hurt = handlers.forge['LivingHurtEvent']
function src(attacker, opts) {
  opts = opts || {}
  return {
    getEntity: function () { return attacker },
    is: function (tag) { return tag == 'bypass' ? !!opts.bypass : opts.tags ? opts.tags.indexOf(tag) >= 0 : false },
    getMsgId: function () { return opts.msg || 'player' }
  }
}
function hit(attacker, victim, amount, opts) {
  var ev = {
    amount: amount,
    getSource: function () { return src(attacker, opts) },
    getEntity: function () { return victim },
    getAmount: function () { return this.amount },
    setAmount: function (a) { this.amount = a }
  }
  hurt(ev)
  return ev.amount
}
function hero(levels, uuid) {
  var h = makePlayer(server, uuid || 'hero')
  var g = gear('Hero Gear')
  Object.keys(levels).forEach(function (id) { faSetLevel(g, id, levels[id]) })
  h.armor[1] = g
  return h
}
var mob = makeMob()

check('Sharpness 100 = x11', near(hit(hero({ sharpness: 100 }), mob, 10), 110))
var zombie = makeMob(); zombie.mobType = MobType.UNDEAD
check('Smite 4 = x2 vs undead only', near(hit(hero({ smite: 4 }), zombie, 10), 20) && hit(hero({ smite: 4 }), mob, 10) == 10)
var spider = makeMob(); spider.mobType = MobType.ARTHROPOD
check('Bane 4 = x2 vs arthropods', near(hit(hero({ bane: 4 }), spider, 10), 20))
var boss = makeMob(); boss.boss = true; boss.maxHp = boss.hp = 300
check('Giant Slayer 10 = x2.5 vs bosses', near(hit(hero({ giant_slayer: 10 }), boss, 10), 25))
var piglin = makeMob(); piglin.dim = 'minecraft:the_nether'
check('Hellbane in the Nether', near(hit(hero({ hellbane: 5 }), piglin, 10), 20))
check('Raidbreaker vs raiders', near(hit(hero({ raidbreaker: 4 }), makeMob(RaiderClass), 10), 20))
var drowned = makeMob(); drowned.wet = true
check('Impaling vs wet targets', near(hit(hero({ impaling: 4 }), drowned, 10), 20))
var flyer = makeMob(); flyer.ground = false
check('Skyhunter vs airborne', near(hit(hero({ skyhunter: 5 }), flyer, 10), 20))
check('First Strike vs full health', near(hit(hero({ first_strike: 10 }), makeMob(), 10), 40))
var hurtMob = makeMob(); hurtMob.hp = 5
check('Executioner vs low health', near(hit(hero({ executioner: 10 }), hurtMob, 1), 4))
var lowHero = hero({ berserk: 10 }); lowHero.hp = 5
check('Berserk while under 50%', near(hit(lowHero, makeMob(), 10), 40) && hit(hero({ berserk: 10 }), makeMob(), 10) == 10)
check('Power with projectiles only', near(hit(hero({ power: 10 }), mob, 10, { tags: ['projectile'] }), 25) && hit(hero({ power: 10 }), mob, 10) == 10)
var sneaky = hero({ assassin: 10 }); sneaky.sneaking = true
check('Assassin while sneaking', near(hit(sneaky, mob, 10), 40))
var realRandom = Math.random
Math.random = function () { return 0 }
check('Critical Edge crits for x3', near(hit(hero({ critical: 1 }), mob, 10), 30))
Math.random = function () { return 0.999 }
check('Critical Edge can miss', hit(hero({ critical: 1 }), mob, 10) == 10)
Math.random = realRandom
var weak = makeMob(); weak.hp = 10; weak.maxHp = 100
check('Reaper 20 kills a non-boss left under 20%', hit(hero({ reaper: 20 }), weak, 1) >= 11)
var weakBoss = makeMob(); weakBoss.boss = true; weakBoss.hp = 10; weakBoss.maxHp = 100
check('Reaper never executes bosses', hit(hero({ reaper: 20 }), weakBoss, 1) == 1)
var target = makeMob(), side1 = makeMob(), side2 = makeMob(), cow = makeMob(LivingClass)
target.nearby = [target, side1, side2, cow]
hit(hero({ cleave: 10 }), target, 10)
check('Cleave hits nearby hostiles for 50%', near(side1.hurtBy[0], 5) && near(side2.hurtBy[0], 5))
check('Cleave spares non-hostile mobs', cow.hurtBy.length == 0)

print('\non hit')
var vamp = hero({ lifesteal: 10, second_wind: 4 })
hit(vamp, makeMob(), 10)
check('Lifesteal 10% + Second Wind 2 heal together', near(vamp.healed, 3), vamp.healed)
var victim = makeMob()
hit(hero({ frostbite: 11, venom: 21, fire_aspect: 2 }), victim, 1)
var slow = victim.effects.filter(function (e) { return e.effect == 'slowness' })[0]
check('Frostbite 11 = Slowness 3 for 3s', slow && slow.amp == 2 && slow.ticks == 60, slow && slow.amp)
check('Venom 21 = Poison 3', victim.effects.some(function (e) { return e.effect == 'poison' && e.amp == 2 }))
check('Fire Aspect 2 = 8s on fire', victim.fire == 8)
var cap = makeMob()
hit(hero({ skyward: 100000 }), cap, 1)
check('effect amplifiers cap at 127', cap.effects[0].amp == 127)
var runner2 = hero({ adrenaline: 1000 })
hit(runner2, makeMob(), 1)
check('Adrenaline gives Speed, max amplifier 4', runner2.effects[0].effect == 'speed' && runner2.effects[0].amp == 4)

print('\ndefense')
var tank = hero({ protection: 3000, vitality: 50 })
check('Protection 3000 + Vitality 50 stack', near(hit(mob, tank, 100), 100 / 121 * 20 / 120))
check('/kill and void ignore defenses', hit(mob, tank, 100, { bypass: true }) == 100)
check('Fire Ward only vs fire', near(hit(mob, hero({ fire_ward: 10 }), 10, { tags: ['fire'] }), 5) && hit(mob, hero({ fire_ward: 10 }), 10) == 10)
check('Featherweight vs falls', near(hit(null, hero({ featherweight: 10 }), 10, { tags: ['fall'] }), 5))
check('Blast Ward vs explosions', near(hit(null, hero({ blast_ward: 10 }), 10, { tags: ['explosion'] }), 5))
check('Arrow Ward vs projectiles', near(hit(mob, hero({ arrow_ward: 10 }), 10, { tags: ['projectile'] }), 5))
check('Spellward vs magic', near(hit(null, hero({ spellward: 10 }), 10, { msg: 'indirectMagic' }), 5))
check('Wither Ward vs wither', near(hit(null, hero({ wither_ward: 10 }), 10, { msg: 'wither' }), 5))
check('Bulwark vs melee', near(hit(mob, hero({ bulwark: 20 }), 10, { msg: 'mob' }), 5))
var spiky = hero({ retribution: 4 }), brute = makeMob()
hit(brute, spiky, 100)
check('Retribution reflects 20% of the raw hit', near(brute.hurtBy[0], 20))
var twinA = hero({ retribution: 4 }, 'a'), twinB = hero({ retribution: 4 }, 'b')
twinA.hurt = function (s, a) { hit(twinB, twinA, a); return true }
twinB.hurt = function (s, a) { hit(twinA, twinB, a); return true }
hit(twinA, twinB, 10)
check('Retribution between two players does not loop forever', true)
var lucky1 = hero({ undying: 1 }, 'u1')
lucky1.hp = 5
check('Undying turns a killing blow into 1 health left', hit(mob, lucky1, 50) == 4)
check('Undying then needs to recharge', hit(mob, lucky1, 50) == 50)
gameTime += 300 * 20
check('Undying recharges after 300s at level 1', hit(mob, lucky1, 50) == 4)
var dodge = handlers.forge['LivingAttackEvent']
function attack(victim2, bypass) {
  var ev = { c: false, getSource: function () { return src(mob, { bypass: bypass }) }, getEntity: function () { return victim2 },
    isCanceled: function () { return this.c }, setCanceled: function (v) { this.c = v } }
  dodge(ev)
  return ev.c
}
Math.random = function () { return 0.5 }
check('Evasion 1000 dodges (chance capped at 75%)', attack(hero({ evasion: 1000 })) && global.FA_describe('evasion', 1000000) == '75% dodge chance')
check('Evasion never dodges /kill', !attack(hero({ evasion: 1000 }), true))
Math.random = function () { return 0.8 }
check('Evasion 75% can still be hit', !attack(hero({ evasion: 1000000 })))
Math.random = realRandom

print('\nkills')
var death = handlers.forge['LivingDeathEvent']
var eater = hero({ soul_eater: 5 })
death({ getSource: function () { return src(eater) }, getEntity: function () { return makeMob() } })
check('Soul Eater heals 5 per kill', eater.healed == 5)
var xpHook = handlers.forge['LivingExperienceDropEvent']
var xpEv = { xp: 10, getEntity: function () { return makeMob() }, getAttackingPlayer: function () { return hero({ wisdom: 10 }) }, getDroppedExperience: function () { return this.xp }, setDroppedExperience: function (n) { this.xp = n } }
xpHook(xpEv)
check('Wisdom 10 doubles XP', xpEv.xp == 20)
var dropsHook = handlers.forge['LivingDropsEvent']
function dropEvent(attacker, victim2, stacks) {
  var ents = stacks.map(function (s) {
    return { s: s, getItem: function () { return this.s }, setItem: function (n) { this.s = n },
      level: function () { return null }, getX: function () { return 0 }, getY: function () { return 0 }, getZ: function () { return 0 } }
  })
  var list = javaList(ents)
  return { list: list, getEntity: function () { return victim2 }, getSource: function () { return src(attacker) }, getDrops: function () { return list } }
}
function totalCount(ev) { var t = 0; ev.list.arr.forEach(function (ie) { t += ie.getItem().count }); return t }
Item.of = function (id, n) { var st = stack(n || 1); st.id = id; return st }
var d1 = dropEvent(hero({ plunder: 20 }), makeMob(), [stack(3)]); dropsHook(d1)
check('Plunder 20 doubles drops', totalCount(d1) == 6, totalCount(d1))
var d2 = dropEvent(hero({ plunder: 20 }), makeMob(), [stack(1, 1)]); dropsHook(d2)
check('gear drops are never duplicated', d2.list.arr.length == 1 && d2.list.arr[0].s.count == 1)
var tagged = stack(5); tagged.tag = new Compound()
var dt = dropEvent(hero({ plunder: 20 }), makeMob(), [tagged]); dropsHook(dt)
check('items with NBT are never duplicated', totalCount(dt) == 5 && dt.list.arr.length == 1)
var d3 = dropEvent(hero({ plunder: 100000 }), makeMob(), [stack(64)]); dropsHook(d3)
check('extra stacks capped at 8', d3.list.arr.length == 9, d3.list.arr.length)
var d4 = dropEvent(hero({ plunder: 20 }), makePlayer(server, 'v'), [stack(64)]); dropsHook(d4)
check('player death drops untouched', totalCount(d4) == 64 && d4.list.arr.length == 1)
var d5 = dropEvent(hero({ midas: 50 }), makeMob(), []); dropsHook(d5)
check('Midas 50 drops 10 gold nuggets even with no other drops', d5.list.arr.length == 1 && d5.list.arr[0].s.count == 10 && d5.list.arr[0].s.id == 'minecraft:gold_nugget')

print('\nattributes')
var equip = handlers.forge['LivingEquipmentChangeEvent']
var walker = hero({ swiftness: 30, reach: 4, stride: 100, frenzy: 5, lucky: 7, knockback: 2 })
equip({ getEntity: function () { return walker } })
function attr(pl, a) { var l = pl.attrs[a] || []; return l.length == 1 ? l[0].amount : l.length }
check('Swiftness 30 = +30% speed', near(attr(walker, 'movement_speed'), 0.3))
check('Reach 4 = +2 block and entity reach', attr(walker, 'block_reach') == 2 && attr(walker, 'entity_reach') == 2)
check('Stride capped at 3', attr(walker, 'step_height') == 3)
check('Frenzy 5 = +50% attack speed', near(attr(walker, 'attack_speed'), 0.5))
check('Lucky 7 = +7 luck', attr(walker, 'luck') == 7)
check('Knockback 2 = +1 knockback', attr(walker, 'attack_knockback') == 1)
equip({ getEntity: function () { return walker } })
check('no duplicate modifiers', walker.attrs.movement_speed.length == 1)
walker.armor[1] = stack(0)
equip({ getEntity: function () { return walker } })
check('removing the gear removes every bonus', ['movement_speed', 'block_reach', 'step_height', 'luck'].every(function (a) { return walker.attrs[a].length == 0 }))

print('\nevery second')
var tick = handlers.forge['PlayerTickEvent']
var camper = hero({ renewal: 8, night_sight: 1, industrious: 3, restoration: 5, magnetism: 3 })
camper.hp = 10
var worn = gear('Worn Pick'); worn.damage = 20
worn.isDamageableItem = function () { return true }; worn.isDamaged = function () { return this.damage > 0 }
worn.getDamageValue = function () { return this.damage }; worn.setDamageValue = function (d) { this.damage = d }
camper.main = worn
;[stack(0), stack(0), stack(0)].concat([camper.off]).forEach(function (s) { s.isDamageableItem = function () { return false } })
camper.armor.forEach(function (s) { s.isDamageableItem = s.isDamageableItem || function () { return false } })
var loose = new ItemEntityClass(); loose.hasPickUpDelay = function () { return false }; loose.teleportTo = function () { this.moved = true }
var thrown = new ItemEntityClass(); thrown.hasPickUpDelay = function () { return true }; thrown.teleportTo = function () { this.moved = true }
camper.nearby = [loose, thrown]
camper.tickCount = 40
tick({ phase: 'END', player: camper })
check('Renewal 8 heals 2 per second', camper.healed == 2)
check('Night Sight keeps night vision (20s, no flicker)', camper.effects.some(function (e) { return e.effect == 'night_vision' && e.ticks == 400 }))
check('Industrious capped at its max level', camper.effects.some(function (e) { return e.effect == 'haste' && e.amp == 2 }))
check('Restoration 5 repairs 5 durability', worn.damage == 15)
check('Magnetism pulls loose items, not ones you just threw', loose.moved && !thrown.moved)
camper.tickCount = 41; camper.healed = 0
tick({ phase: 'END', player: camper })
check('only runs once a second', camper.healed == 0)

var digger = hero({ excavation: 4 })
var bs = { speed: 8, getEntity: function () { return digger }, getNewSpeed: function () { return this.speed }, setNewSpeed: function (s) { this.speed = s } }
handlers.forge['BreakSpeed'](bs)
check('Excavation 4 = x2 mining speed', near(bs.speed, 16), bs.speed)

// ---------------- Tooltips ----------------
print('\ntooltips')
var advanced = null, simple = 0
handlers.tooltip.forEach(function (fn) { fn({ add: function () { simple++ }, addAdvanced: function (ing, f) { advanced = f } }) })
check('tooltips for cores, blank tome, every tome, fragment and clusters', simple == 3 + 1 + enchantCount + 1 + 1 + 4 + gemCount + 2, simple)
var lines = { arr: ['Netherite Chestplate', 'When on body:'], add: function (i, t) { this.arr.splice(i, 0, t.s) } }
advanced(chest, false, lines)
check('enchants show right under the item name', /Ascended Protection 3,000/.test(lines.arr[1]), lines.arr.join(' | '))
var plain = { arr: ['Stick'], add: function () { throw 'should not add' } }
advanced(stack(1), false, plain)
check('items without enchants are untouched', plain.arr.length == 1)

print('\ndescriptions')
var blank = Object.keys(E).filter(function (id) { var d = global.FA_describe(id, 1); return !d || /undefined|NaN/.test(d + global.FA_describe(id, 3000)) })
check('every enchant has a description at levels 1 and 3000', blank.length == 0, blank.join(','))
check('numbers', global.FA_num(1234) == '1,234' && global.FA_num(2.5e9) == '2.50B' && global.FA_num(1e40).indexOf('e+') > 0 &&
  global.FA_num(0.25) == '0.25' && global.FA_num(1.5) == '1.5' && global.FA_num(42.37) == '42.4' && global.FA_num(7) == '7',
  [global.FA_num(0.25), global.FA_num(1.5), global.FA_num(42.37)].join(' '))
Object.keys(E).forEach(function (id) {
  var top = E[id].maxLevel || 3000
  print('  ' + E[id].name + ' ' + top + ': ' + global.FA_describe(id, top))
})

// ---------------- Danger ----------------
print('\ndanger')
var join = handlers.forge['EntityJoinLevelEvent']
function spawn(mob2, x, opts) {
  opts = opts || {}
  mob2.x = x
  if (opts.dim) mob2.dim = opts.dim
  join({ loadedFromDisk: function () { return !!opts.fromDisk }, getEntity: function () { return mob2 }, getLevel: function () { return mob2.level() } })
  return mob2
}
Math.random = function () { return 0.5 }
check('mobs near spawn stay normal', !spawn(makeMob(), 500).pdata.contains('fa_danger'))
var far = spawn(makeMob(), 6000)
check('6000 blocks out = danger 11', far.pdata.getInt('fa_danger') == 11, far.pdata.getInt('fa_danger'))
check('mobs loaded from a save are not re-rolled', !spawn(makeMob(), 6000, { fromDisk: true }).pdata.contains('fa_danger'))
var farBoss = makeMob(); farBoss.boss = true
check('bosses never scale', !spawn(farBoss, 6000).pdata.contains('fa_danger'))
check('passive mobs never scale', !spawn(makeMob(LivingClass), 6000).pdata.contains('fa_danger'))
check('Nether: 1000 blocks counts as 8000, +3 bonus = 18', spawn(makeMob(), 1000, { dim: 'minecraft:the_nether' }).pdata.getInt('fa_danger') == 18)
check('the End is always at least danger 6', spawn(makeMob(), 0, { dim: 'minecraft:the_end' }).pdata.getInt('fa_danger') == 6)
Math.random = function () { return 0 }
var elite = spawn(makeMob(), 6000)
check('elites get a visible name with their level', elite.pdata.getBoolean('fa_elite') && elite.nameVisible && /Elite Zombie .* Lv 11/.test(elite.customName), elite.customName)
Math.random = realRandom

var plainHero = makePlayer(server, 'plain')
check('danger 11 mob deals x2.65 damage', near(hit(far, plainHero, 10), 26.5), hit(far, plainHero, 10))
check('danger 11 mob takes damage / 3.75', near(hit(plainHero, far, 37.5), 10))
check('elite takes another /3', near(hit(plainHero, elite, 112.5), 10))
check('/kill still works on scaled mobs', hit(null, far, 50, { bypass: true }) == 50)
var tenMob = makeMob(); tenMob.pdata.putInt('fa_danger', 10)
var d6 = dropEvent(plainHero, tenMob, [stack(4)]); dropsHook(d6)
check('danger 10 doubles drops even without Plunder', totalCount(d6) == 8, totalCount(d6))
var xpEv2 = { xp: 10, getEntity: function () { return tenMob }, getAttackingPlayer: function () { return plainHero }, getDroppedExperience: function () { return this.xp }, setDroppedExperience: function (n) { this.xp = n } }
xpHook(xpEv2)
check('danger 10 triples XP', xpEv2.xp == 30)

var explorer = makePlayer(server, 'explorer')
explorer.tickCount = 20
server.commands = []
tick({ phase: 'END', player: explorer })
explorer.x = 6000
tick({ phase: 'END', player: explorer })
check('crossing into a new zone shows its title', server.commands.some(function (c) { return /Dread Reaches/.test(c) }), server.commands.join(' | '))
check('going deeper plays a cave sound', server.commands.some(function (c) { return /ambient.cave/.test(c) }))
server.commands = []; explorer.bar = []
explorer.x = 6600
tick({ phase: 'END', player: explorer })
check('a new level inside the same zone shows on the action bar only', server.commands.length == 0 && /Danger 12/.test(explorer.bar[0]), explorer.bar[0])
explorer.x = 0
tick({ phase: 'END', player: explorer })
check('heading home shows Safe Lands', server.commands.some(function (c) { return /Safe Lands/.test(c) }))
explorer.messages = []
explorer.x = 6000
faShowDanger(explorer)
check('/ascension danger explains the area', explorer.messages.length == 4 && /Dread Reaches/.test(explorer.messages[0].s), explorer.messages.map(function (t) { return t.s }).join(' | '))
print('  sample: ' + explorer.messages.map(function (t) { return t.s }).join(' |'))
var zones = [0, 1000, 3500, 6000, 11000, 21000, 41000, 100000].map(function (x) {
  var lvl = global.FA_dangerLevel(explorer.level(), x, 0)
  return x + 'b: ' + global.FA_zone(lvl).name + ' (' + lvl + ', mobs x' + global.FA_dangerMult(lvl, false).health.toFixed(1) + ')'
})
print('  ' + zones.join('\n  '))

// ---------------- Mythic ----------------
print('\nmythic')
var wingsTome = byId('tome_wings').json
check('Mythic tomes need 4 fragments, superheated', wingsTome.heatRequirement == 'superheated' &&
  wingsTome.ingredients.filter(function (i) { return i.item == 'kubejs:mythic_fragment' }).length == 4)
check('Mythic levels cost 5x', global.FA_cost(100, 'thunderlord') == Math.ceil(0.05 * 101 * 5) && global.FA_cost(100, 'sharpness') == Math.ceil(0.05 * 101))

var flyer2 = hero({ wings: 2 }, 'flyer')
flyer2.tickCount = 20
tick({ phase: 'END', player: flyer2 })
check('Wings grants flight at x1.5 speed', flyer2.abilities.mayfly && near(flyer2.abilities.speed, 0.075))
flyer2.armor[1] = stack(0)
tick({ phase: 'END', player: flyer2 })
check('taking Wings off takes flight away', !flyer2.abilities.mayfly && flyer2.abilities.speed == 0.05)
var builder2 = hero({ wings: 1 }, 'builder'); builder2.tickCount = 20; builder2.creative = true
tick({ phase: 'END', player: builder2 })
builder2.armor[1] = stack(0)
tick({ phase: 'END', player: builder2 })
check('creative players keep their own flight', builder2.abilities.mayfly)

Math.random = function () { return 0 }
var storm = makeMob(), s1 = makeMob(), s2 = makeMob()
storm.nearby = [storm, s1, s2]
bolts = []
hit(hero({ thunderlord: 10 }), storm, 10)
check('Thunderlord chains to nearby enemies', near(s1.hurtBy[0], 10 * 0.7) && near(s2.hurtBy[0], 7))
check('lightning is visual only', bolts.length == 3 && bolts.every(function (b) { return b.visual }))
Math.random = realRandom

var well = makeMob(), pulled = makeMob()
pulled.x = 10; well.nearby = [pulled]
hit(hero({ black_hole: 10 }), well, 1)
check('Black Hole pulls enemies toward the target', pulled.motion && pulled.motion[0] < 0 && pulled.hurtMarked)

var boomer = makeMob(), b1 = makeMob(); boomer.nearby = [b1]; boomer.maxHp = 40
death({ getSource: function () { return src(hero({ meteor: 5 })) }, getEntity: function () { return boomer } })
check('Meteor: kills explode for 100% of max health at level 5', near(b1.hurtBy[0], 40), b1.hurtBy[0])

var timeLord = hero({ chronos: 2 }, 'chronos'), frozen = makeMob()
timeLord.nearby = [frozen]
hit(makeMob(), timeLord, 1)
check('Chronos freezes enemies when you get hit', frozen.effects.some(function (e) { return e.effect == 'slowness' && e.amp == 127 }))
frozen.effects = []
hit(makeMob(), timeLord, 1)
check('Chronos has a cooldown', frozen.effects.length == 0)

var bird = hero({ phoenix: 1 }, 'phoenix'); bird.hp = 5
check('Phoenix cancels a killing blow', hit(makeMob(), bird, 100) == 0 && bird.hp == bird.maxHp)
bird.hp = 5
check('Phoenix then needs 600s to recharge', hit(makeMob(), bird, 100) == 100)
var both = hero({ undying: 1, phoenix: 1 }, 'both'); both.hp = 5
hit(makeMob(), both, 100)
check('Undying goes first and saves Phoenix for later', both.hp == 5)

var archer = hero({ voidwalker: 1 }), mark = makeMob(); mark.x = 40
hit(archer, mark, 1, { tags: ['projectile'] })
check('Voidwalker arrows teleport you to the target', archer.teleported && archer.teleported[0] == 40 && archer.fallDistance == 0)
var melee = hero({ voidwalker: 1 }); hit(melee, mark, 1)
check('Voidwalker ignores melee', !melee.teleported)

var reaperBlade = gear('Soul Blade'); faSetLevel(reaperBlade, 'soul_harvest', 10)
var harvester = makePlayer(server, 'harvester'); harvester.main = reaperBlade
for (var k = 0; k < 100; k++) death({ getSource: function () { return src(harvester) }, getEntity: function () { return makeMob() } })
check('Soul Harvest stores a soul per kill on the item', reaperBlade.tag.d.FactoryAscension.d.souls == 100)
check('100 souls at level 10 = x2 damage', near(hit(harvester, makeMob(), 10), 20))

var over = makeMob(), nextUp = makeMob(); over.hp = 5; over.nearby = [nextUp]
hit(hero({ overkill: 10 }), over, 25)
check('Overkill: 100% of the excess jumps to the next enemy at level 10', near(nextUp.hurtBy[0], 20), nextUp.hurtBy[0])

var berserker = hero({ rampage: 10 }, 'rampage')
for (var r = 0; r < 3; r++) death({ getSource: function () { return src(berserker) }, getEntity: function () { return makeMob() } })
check('Rampage: 3 kills = x4 damage at level 10', near(hit(berserker, makeMob(), 10), 40))
gameTime += 200
check('Rampage stacks wear off after 5 seconds', hit(berserker, makeMob(), 10) == 10)

// Vein Miner: a 3x3x3 cube of iron ore
var oreBlock = { name: 'iron_ore' }, stone = { name: 'stone' }
function state(block) { return { getBlock: function () { return block }, is: function (tag) { return tag == 'ores' ? block === oreBlock : tag == 'logs' ? block.log : false } } }
function bpos(x, y, z) { return { getX: function () { return x }, getY: function () { return y }, getZ: function () { return z }, offset: function (a, b, c) { return bpos(x + a, y + b, z + c) } } }
var world = { getBlockState: function (p2) { var inCube = Math.abs(p2.getX()) <= 1 && Math.abs(p2.getY()) <= 1 && Math.abs(p2.getZ()) <= 1; return state(inCube ? oreBlock : stone) } }
var minerPick = gear('Pick'); faSetLevel(minerPick, 'vein_miner', 2)
var miner = makePlayer(server, 'miner'); miner.main = minerPick
miner.gameMode = { broken: 0, destroyBlock: function () { this.broken++; return true } }
var brk = handlers.forge['BreakEvent']
function breakEv(pl) { return { getPlayer: function () { return pl }, getState: function () { return state(oreBlock) }, getLevel: function () { return world }, getPos: function () { return bpos(0, 0, 0) } } }
brk(breakEv(miner))
check('Vein Miner 2 breaks up to 16 connected ores', miner.gameMode.broken == 16, miner.gameMode.broken)
faSetLevel(minerPick, 'vein_miner', 100); miner.gameMode.broken = 0
brk(breakEv(miner))
check('...and stops at the edge of the vein', miner.gameMode.broken == 26, miner.gameMode.broken)
miner.gameMode.broken = 0; miner.sneaking = true
brk(breakEv(miner))
check('sneaking mines just one block', miner.gameMode.broken == 0)

var shiny = hero({ radiance: 5 }, 'radiance'), r1 = makeMob(), r2 = makeMob()
shiny.nearby = [r1, r2]; shiny.tickCount = 20
tick({ phase: 'END', player: shiny })
check('Radiance burns nearby enemies each second', r1.hurtBy[0] == 10 && r2.hurtBy[0] == 10)

Math.random = function () { return 0 }
var dh = dropEvent(hero({ headhunter: 1 }), makeMob(), []); dropsHook(dh)
check('Headhunter drops zombie heads', dh.list.arr.some(function (ie) { return ie.s.id == 'minecraft:zombie_head' }))
var eliteMob = makeMob(); eliteMob.pdata.putInt('fa_danger', 20); eliteMob.pdata.putBoolean('fa_elite', true)
var df = dropEvent(plainHero, eliteMob, []); dropsHook(df)
check('Elites drop Mythic Fragments', df.list.arr.some(function (ie) { return ie.s.id == 'kubejs:mythic_fragment' }))
var dn = dropEvent(makeMob(), eliteMob, []); dropsHook(dn)
check('no fragments unless a player made the kill', !dn.list.arr.some(function (ie) { return ie.s.id == 'kubejs:mythic_fragment' }))
var dsafe = dropEvent(plainHero, makeMob(), []); dropsHook(dsafe)
check('no fragments from safe-zone mobs', dsafe.list.arr.length == 0)
Math.random = realRandom
check('fragment odds: Elite at danger 20 = 25%, normal mob at danger 20 = 4%',
  near(global.FA_fragmentChance(20, true), 0.25) && near(global.FA_fragmentChance(20, false), 0.04))

var slammer = hero({ gravity_slam: 2 }, 'slam'), sl1 = makeMob(); slammer.nearby = [sl1]
var fall = { mult: 1, getEntity: function () { return slammer }, getDistance: function () { return 10 }, setDamageMultiplier: function (m) { this.mult = m } }
handlers.forge['LivingFallEvent'](fall)
check('Gravity Slam: no fall damage, 10 blocks x level 2 = 20 damage around you', fall.mult == 0 && sl1.hurtBy[0] == 20)

// ---------------- Safety ----------------
print('\nsafety: config')
check('shipped config has no problems', global.FA_validate(JSON.parse(JSON.stringify(global.FA))).length == 0,
  global.FA_validate(JSON.parse(JSON.stringify(global.FA))).join('; '))
var bad = JSON.parse(JSON.stringify(global.FA))
bad.enchants.typo = { name: 'Typo', type: 'damgae', perLevel: 1 }
bad.enchants.nan = { name: 'NaN', type: 'damage', when: 'always', perLevel: NaN }
bad.enchants.neg = { name: 'Neg', type: 'resist', vs: 'all', perLevel: -1 }
bad.enchants.badwhen = { name: 'W', type: 'damage', when: 'tuesdays', perLevel: 1 }
bad.enchants.noeffect = { name: 'E', type: 'passive' }
bad.enchants.badattr = { name: 'A', type: 'attribute', attrs: ['jump'], perLevel: 1 }
bad.enchants.frac = { name: 'F', type: 'eternal', maxLevel: 1.5 }
bad.essenceValues['kubejs:dense_ascension_core'] = 10
bad.danger.blocksPerLevel = 0
bad.fragments.eliteChance = 7
var problems = global.FA_validate(bad)
check('broken enchants are switched off, good ones kept', ['typo', 'nan', 'neg', 'badwhen', 'noeffect', 'badattr', 'frac'].every(function (id) { return !bad.enchants[id] }) &&
  Object.keys(bad.enchants).length == enchantCount, Object.keys(bad.enchants).length)
check('Essence values that break 1:9:81 are reset (no Essence from unpacking)', bad.essenceValues['kubejs:dense_ascension_core'] == 9)
check('broken danger settings switch danger off instead of crashing', bad.danger.enabled === false)
check('fragment chances over 100% are reset', bad.fragments.eliteChance == 0.15)
check('every problem is reported', problems.length >= 11, problems.length)

print('\nsafety: Essence storage')
var saver = makePlayer(server, 'saver')
faSetEssence(saver, 500)
server.persistentData.remove('fa_essence_saver')
check('lost world data is restored from the player file backup', faGetEssence(saver) == 500 && server.persistentData.getDouble('fa_essence_saver') == 500)
server.persistentData.putDouble('fa_essence_saver', NaN)
check('NaN Essence reads as 0', faGetEssence(saver) === 0)
server.persistentData.putDouble('fa_essence_saver', -50)
check('negative Essence reads as 0', faGetEssence(saver) === 0)
server.persistentData.putDouble('fa_essence_saver', Infinity)
check('infinite Essence reads as 0', faGetEssence(saver) === 0)
faSetEssence(saver, 1e400)
check('Essence never stores Infinity or NaN', faGetEssence(saver) === 0 && isFinite(server.persistentData.getDouble('fa_essence_saver')))
var hacked = gear('Hacked'); hacked.getOrCreateTag().put('FactoryAscension', new Compound()); hacked.tag.d.FactoryAscension.putInt('sharpness', -500)
check('negative levels from edited NBT read as 0', faGetLevel(hacked, 'sharpness') == 0)
var hackedHero = makePlayer(server, 'hh'); hackedHero.main = hacked
check('...and never weaken or break combat', hit(hackedHero, makeMob(), 10) == 10)

print('\nsafety: transactions')
var tx = makePlayer(server, 'tx')
faSetEssence(tx, 100)
var cursed = gear('Cursed'); var txTome = stack(1)
cursed.getOrCreateTag = function () { throw 'item vanished' }
tx.main = txTome; tx.off = cursed
quiet = true
handlers.rightClicked['kubejs:tome_sharpness']({ player: tx, item: txTome, hand: 'MAIN_HAND', cancel: function () {} })
quiet = false
check('a failed unlock keeps the tome and the Essence', txTome.count == 1 && faGetEssence(tx) == 100)
var good = gear('Good'); faSetLevel(good, 'sharpness', 5); tx.main = good
var realPut = server.persistentData.putDouble
server.persistentData.putDouble = function (k, v) { if (k == 'fa_essence_tx' && v < 100) throw 'disk full'; realPut.call(this, k, v) }
quiet = true
faUpgrade(tx, 'sharpness', 10)
quiet = false
server.persistentData.putDouble = realPut
check('a failed upgrade is rolled back (no free levels)', faGetLevel(good, 'sharpness') == 5 && faGetEssence(tx) == 100, faGetLevel(good, 'sharpness') + ' / ' + faGetEssence(tx))
var maxed = gear('Maxed'); faSetLevel(maxed, 'sharpness', 2147483646); tx.main = maxed
faSetEssence(tx, 1e300)
faUpgrade(tx, 'sharpness', 100000)
check('levels stop at the int limit instead of overflowing', faGetLevel(maxed, 'sharpness') == 2147483647)
var threw = false
try { faSetLevel(maxed, 'sharpness', 2147483648) } catch (e) { threw = true }
check('writing an out-of-range level is refused', threw && faGetLevel(maxed, 'sharpness') == 2147483647)
var stackOf2 = stack(2, 1); tx.main = stackOf2
check('only single items can be upgraded', faUpgrade(tx, 'sharpness', 1) == 0)
var onlyTags = gear('Clean'); tx.main = onlyTags; faSetEssence(tx, 1000)
var t2 = stack(1); tx.off = t2; tx.main = onlyTags
handlers.rightClicked['kubejs:tome_eternal']({ player: tx, item: t2, hand: 'OFF_HAND', cancel: function () {} })
faUpgrade(tx, 'eternal', 1)
check('only the FactoryAscension and Unbreakable tags are ever written to items',
  Object.keys(onlyTags.tag.d).every(function (k) { return k == 'FactoryAscension' || k == 'Unbreakable' }), Object.keys(onlyTags.tag.d).join(','))

print('\nsafety: no duplication (fuzz)')
// A seeded random mix of absorbing, unlocking, upgrading and failed attempts. Afterwards every bit of
// Essence must be accounted for: absorbed = still in the bank + exactly what the levels on items cost.
var seed = 12345
function rnd(n) { seed = (seed * 1103515245 + 12345) % 2147483648; return Math.floor(seed / 2147483648 * n) }
var fz = makePlayer(server, 'fuzz')
var fzGear = [gear('A'), gear('B'), gear('C'), gear('D')]
var ids = Object.keys(E)
var absorbed = 0, tomesUsed = 0, tomesMade = 0, destroyed = 0
// What a rolled slot cost: 1 to roll, then each level at its rarity's price
function rolledValue(r) {
  var roll = global.FA_rollEnchant(r.seed), v = global.FA_rollCost(0, 0)
  for (var k = 1; k < r.level; k++) v += global.FA_rollCost(k, roll.rarity)
  return v
}
for (var step = 0; step < 3000; step++) {
  var op = rnd(10)
  if (op < 3) {
    var cid = Object.keys(global.FA.essenceValues)[rnd(3)]
    var cstack = stack(1 + rnd(64))
    fz.sneaking = rnd(2) == 1
    var before2 = cstack.count
    handlers.rightClicked[cid]({ player: fz, item: cstack, cancel: function () {} })
    absorbed += (before2 - cstack.count) * global.FA.essenceValues[cid]
  } else if (op < 6) {
    var eid = ids[rnd(ids.length)]
    var tm = stack(1 + rnd(3)); tomesMade += tm.count
    fz.main = tm; fz.off = fzGear[rnd(4)]
    var tb = tm.count
    handlers.rightClicked['kubejs:tome_' + eid]({ player: fz, item: tm, hand: 'MAIN_HAND', cancel: function () {} })
    tomesUsed += tb - tm.count
  } else if (op < 8) {
    fz.main = fzGear[rnd(4)]; fz.off = stack(0)
    faUpgrade(fz, ids[rnd(ids.length)], [1, 10, 100, 1000, 100000][rnd(5)])
  } else if (op < 9) {
    // Chaos Tome roll (sometimes a sneak reroll that throws away the newest rolled enchant)
    var ct = stack(1); tomesMade++
    var target2 = fzGear[rnd(4)]
    fz.main = ct; fz.off = target2; fz.sneaking = rnd(2) == 1
    var slotsBefore = faRolledSlots(target2)
    faChaosRoll(fz, ct, target2)
    tomesUsed += 1 - ct.count
    if (ct.count == 0 && slotsBefore.length == global.FA.chaos.maxPerItem) destroyed += rolledValue(slotsBefore[slotsBefore.length - 1])
    fz.sneaking = false
  } else {
    var g3 = fzGear[rnd(4)]; fz.main = g3; fz.off = stack(0)
    var rs = faRolledSlots(g3)
    if (rs.length) faUpgradeRolled(fz, rs[rnd(rs.length)].slot, [1, 10, 100, 100000][rnd(4)])
  }
  if (faGetEssence(fz) < 0) break
}
var spentOnItems = 0, unlocked = 0, rolledCount = 0
fzGear.forEach(function (g) {
  faRolledSlots(g).forEach(function (r) { spentOnItems += rolledValue(r); rolledCount++ })
  ids.forEach(function (id) {
    var lv = faGetLevel(g, id)
    if (lv > 0) unlocked++
    for (var k2 = 0; k2 < lv; k2++) spentOnItems += global.FA_cost(k2, id)
  })
})
print('  info absorbed ' + absorbed + ', bank ' + faGetEssence(fz) + ', on items ' + spentOnItems + ', rerolled away ' + destroyed + ', unlocks ' + unlocked + ', rolled ' + rolledCount)
check('Essence is conserved: absorbed = bank + cost of every level on items (+ rerolled away)', near(absorbed, faGetEssence(fz) + spentOnItems + destroyed), absorbed + ' vs ' + (faGetEssence(fz) + spentOnItems + destroyed))
check('rolled enchants were part of the fuzz', rolledCount > 0)
check('no item ever holds more rolled enchants than allowed', fzGear.every(function (g) { return faRolledSlots(g).length <= global.FA.chaos.maxPerItem }))
check('Essence never went negative', faGetEssence(fz) >= 0)

print('\nchaos tomes')
var r1 = global.FA_rollEnchant(424242), r2 = global.FA_rollEnchant(424242)
check('the same seed always rolls the same enchant', JSON.stringify(r1) == JSON.stringify(r2))
var counts = [0, 0, 0, 0, 0], badRolls = [], affixTotal = 0
for (var sd = 1; sd <= 5000; sd++) {
  var rr = global.FA_rollEnchant(sd * 7919)
  counts[rr.rarity]++
  var R0 = global.FA.chaos.rarities[rr.rarity]
  if (rr.affixes.length != R0.affixes) badRolls.push(sd + ': affix count')
  var cfgCopy = JSON.parse(JSON.stringify(global.FA)); cfgCopy.enchants = {}; cfgCopy.sets = []; cfgCopy.gems.types = {}
  rr.affixes.forEach(function (a, i) { cfgCopy.enchants['a' + i] = JSON.parse(JSON.stringify(a)) })
  var probs = global.FA_validate(cfgCopy)
  if (probs.length) badRolls.push(sd + ': ' + probs[0])
  rr.affixes.forEach(function (a) { affixTotal++; if (/undefined|NaN/.test(global.FA_describeDef(a, 1) + global.FA_describeDef(a, 1000))) badRolls.push(sd + ': bad text') })
  if (!/^[A-Z][a-z]+ [A-Z]/.test(rr.name)) badRolls.push(sd + ': name ' + rr.name)
}
check('5,000 rolls: every effect passes the config check, text and names are clean', badRolls.length == 0, badRolls.slice(0, 3).join(' | '))
print('  info rarity spread over 5,000 rolls: ' + global.FA.chaos.rarities.map(function (R, i) { return R.name + ' ' + counts[i] }).join(', '))
check('rarity odds roughly match the weights', Math.abs(counts[0] / 5000 - 0.5) < 0.04 && counts[4] > 10 && counts[4] < 120, counts.join(','))
check('rolls never include effects that need cleanup', !/flight|attribute|passive|eternal|vein|undying|phoenix|time_stop/.test(JSON.stringify([global.FA_rollEnchant(1), global.FA_rollEnchant(99)])))
print('  sample: ' + [11, 222, 3333].map(function (sd) { var x = global.FA_rollEnchant(sd); return global.FA.chaos.rarities[x.rarity].name + ' "' + x.name + '"' }).join(', '))

var roller = makePlayer(server, 'roller'); faSetEssence(roller, 1000)
var rGear = gear('Roll Sword'), cTome = stack(5)
roller.main = cTome; roller.off = rGear
for (var t = 0; t < 3; t++) handlers.rightClicked['kubejs:chaos_tome']({ player: roller, item: cTome, hand: 'MAIN_HAND', cancel: function () {} })
check('Chaos Tomes fill up to 3 rolled slots', faRolledSlots(rGear).length == 3 && cTome.count == 2 && faGetEssence(roller) == 997)
handlers.rightClicked['kubejs:chaos_tome']({ player: roller, item: cTome, hand: 'MAIN_HAND', cancel: function () {} })
check('a full item refuses a 4th roll and keeps the tome', faRolledSlots(rGear).length == 3 && cTome.count == 2)
var newestBefore = faRolledSlots(rGear)[2].seed
roller.sneaking = true
handlers.rightClicked['kubejs:chaos_tome']({ player: roller, item: cTome, hand: 'MAIN_HAND', cancel: function () {} })
roller.sneaking = false
check('sneak-use rerolls only the newest slot', faRolledSlots(rGear).length == 3 && faRolledSlots(rGear)[2].seed != newestBefore && cTome.count == 1)
roller.main = rGear; roller.off = stack(0)
faUpgradeRolled(roller, 0, 9)
check('rolled enchants level up with Essence', faRolledSlots(rGear)[0].level == 10)
check('upgrading an empty slot is refused', faUpgradeRolled(roller, 5, 1) == 0)
roller.messages = []; faShowMenu(roller)
var menuClicks = []; roller.messages.forEach(function (t2) { menuClicks = menuClicks.concat(t2.clicks || []) })
check('the /ascension menu lists rolled enchants with upgrade buttons', menuClicks.indexOf('/ascension upgrade rolled 0 10') >= 0)
// A rolled enchant actually works in combat
var dmgSeed = 0
for (var sd2 = 1; sd2 < 20000 && !dmgSeed; sd2++) { var x2 = global.FA_rollEnchant(sd2); if (x2.affixes.length == 1 && x2.affixes[0].type == 'damage' && x2.affixes[0].when == 'always') dmgSeed = sd2 }
var rolledBlade = gear('Rolled Blade'); faSetRolled(rolledBlade, 0, dmgSeed, 10)
var rolledHero = makePlayer(server, 'rh'); rolledHero.main = rolledBlade
var expectDmg = 10 * (1 + global.FA_rollEnchant(dmgSeed).affixes[0].perLevel * 10)
check('a rolled damage enchant boosts damage like a normal one', near(hit(rolledHero, makeMob(), 10), expectDmg), hit(rolledHero, makeMob(), 10) + ' vs ' + expectDmg)
var tipLines = { arr: ['Rolled Blade'], add: function (i, t3) { this.arr.splice(i, 0, t3.s) } }
advanced(rolledBlade, false, tipLines)
check('tooltips show rolled enchants with their rarity and effects', tipLines.arr.length >= 3 && /Common/.test(tipLines.arr[1]), tipLines.arr.join(' | '))
print('  sample: ' + tipLines.arr.slice(1).join(' | '))
var hackedRoll = gear('Hacked Roll'); faSetRolled(hackedRoll, 0, 77, 5); hackedRoll.tag.d.FactoryAscension.d.rolled.d['0'].d.level = -9
check('broken rolled levels from edited NBT are ignored', faRolledSlots(hackedRoll).length == 0)
var badThrow = false
try { faSetRolled(rGear, 7, 1, 1) } catch (e) { badThrow = true }
check('rolled slots outside 0-2 are refused', badThrow)

// ---------------- New systems ----------------
print('\nmilestones')
var ms = makePlayer(server, 'ms')
faStatRecord(ms, 'essence_absorbed', 1)
check('first core pays the First Spark milestone', ms.given.length == 1 && ms.given[0].id == 'kubejs:blank_ascension_tome' && ms.given[0].count == 2)
faStatRecord(ms, 'essence_absorbed', 1)
check('a milestone never pays twice', ms.given.length == 1)
faStatRecord(ms, 'max_danger', 12, 'max'); faStatRecord(ms, 'max_danger', 5, 'max')
check("'max' stats keep the highest value", faStatGet(server, 'ms', 'max_danger') == 12 && ms.given.some(function (g) { return g.id == 'kubejs:mythic_fragment' }))
faStatBump(server, 'ms', 'altar_levels', 2000, 'add')
handlers.loggedIn.forEach(function (fn) { fn({ player: ms }) })
check('stats earned while offline pay out at login', ms.given.some(function (g) { return g.id == 'kubejs:gem_onyx_3' }))
ms.messages = []; faShowMilestones(ms)
check('/ascension milestones lists every milestone', ms.messages.length == global.FA.milestones.length + 2)
quiet = true
faStatRecord(ms, 'essence_absorbed', NaN); faStatRecord(ms, 'essence_absorbed', -5)
quiet = false
check('bad stat amounts are ignored', faStatGet(server, 'ms', 'essence_absorbed') == 2)

print('\nprestige')
var reb = makePlayer(server, 'reb'), rebGear = gear('Old Sword')
faSetLevel(rebGear, 'sharpness', 600); faSetLevel(rebGear, 'protection', 300); reb.main = rebGear
check('Rebirth needs 1,000 total levels', faRebirth(reb, true) == 0 && faStars(rebGear) == 0)
faSetLevel(rebGear, 'protection', 400)
reb.messages = []
faRebirth(reb, false)
check('Rebirth asks for confirmation first', faStars(rebGear) == 0 && reb.messages.some(function (t) { return (t.clicks || []).indexOf('/ascension rebirth confirm') >= 0 }))
faRebirth(reb, true)
check('Rebirth resets levels to 1 and adds a star', faStars(rebGear) == 1 && faGetLevel(rebGear, 'sharpness') == 1 && faGetLevel(rebGear, 'protection') == 1)
faSetLevel(rebGear, 'sharpness', 100)
check('a 1-star item counts its levels x1.25', faLevels(reb).sharpness == 125)
check('2 stars need 2,000 levels', global.FA_rebirthNeed(1) == 2000)

print('\ngems')
var jew = makePlayer(server, 'jew'), jewGear = gear('Gem Sword'), chip = stack(5)
jew.main = chip; jew.off = jewGear
handlers.rightClicked['kubejs:gem_bloodstone_1']({ player: jew, item: chip, hand: 'MAIN_HAND', cancel: function () {} })
check('socketing a gem uses it up', chip.count == 4 && faGems(jewGear).length == 1)
jew.main = jewGear; jew.off = stack(0)
check('Chipped Bloodstone adds 3 Sharpness levels', faLevels(jew).sharpness == 3)
jew.main = chip; jew.off = jewGear
handlers.rightClicked['kubejs:gem_bloodstone_1']({ player: jew, item: chip, hand: 'MAIN_HAND', cancel: function () {} })
handlers.rightClicked['kubejs:gem_bloodstone_1']({ player: jew, item: chip, hand: 'MAIN_HAND', cancel: function () {} })
handlers.rightClicked['kubejs:gem_bloodstone_1']({ player: jew, item: chip, hand: 'MAIN_HAND', cancel: function () {} })
check('only 3 sockets; a 4th gem is refused and kept', faGems(jewGear).length == 3 && chip.count == 2)
var perfect = stack(1); jew.main = perfect; jew.sneaking = true
handlers.rightClicked['kubejs:gem_sapphire_3']({ player: jew, item: perfect, hand: 'MAIN_HAND', cancel: function () {} })
jew.sneaking = false
check('sneak replaces the newest gem', faGems(jewGear).filter(function (g) { return g.type == 'sapphire' && g.tier == 3 }).length == 1 && faGems(jewGear).length == 3)
jew.main = jewGear; jew.off = stack(0)
check('gems stack with stars and sets like any level', faLevels(jew).protection == 75 && faLevels(jew).sharpness == 6)

print('\nsets')
var setHero = makePlayer(server, 'set')
var p1 = gear('Helm'), p2 = gear('Chest'), p3 = gear('Legs'), p4 = gear('Boots')
;[p1, p2, p3, p4].forEach(function (g) { faSetLevel(g, 'protection', 100) })
setHero.armor = [p1, p2, stack(0), stack(0)]
check('2-piece Juggernaut: Protection x1.25', faLevels(setHero).protection == 250)
setHero.armor = [p1, p2, p3, p4]
var four = faLevels(setHero)
check('4-piece Juggernaut: x1.5 plus Vitality and Bulwark', four.protection == 600 && four.vitality == 100 && four.bulwark == 50)

print('\nliquid essence')
var drinker = makePlayer(server, 'drinker'), bucket = stack(1)
handlers.rightClicked['kubejs:liquid_essence_bucket']({ player: drinker, item: bucket, cancel: function () {} })
check('a plain right-click leaves the bucket alone (it places like a bucket)', bucket.count == 1 && faGetEssence(drinker) == 0)
drinker.sneaking = true
handlers.rightClicked['kubejs:liquid_essence_bucket']({ player: drinker, item: bucket, cancel: function () {} })
check('sneak-drinking a bucket gives 10 Essence and the empty bucket back', bucket.count == 0 && faGetEssence(drinker) == 10 && drinker.given.some(function (g) { return g.id == 'minecraft:bucket' }))
var melt = byId('melt_ascension_core').json
check('melting is lossless: 1 core = 100 mB = 1 Essence = the bucket rate', melt.results[0].amount == global.FA.altar.mbPerEssence && 1000 / global.FA.altar.mbPerEssence == 10)

print('\naltar')
// A fake world: a depot above the altar, a tank below
function makeDepot(item) { var slots = [item]; return { slots: slots, getSlots: function () { return slots.length }, getStackInSlot: function (i) { return slots[i] },
  extractItem: function (i, n, sim) { var it = slots[i]; if (!sim) slots[i] = stack(0); return it },
  insertItem: function (i, it, sim) { if (!slots[i].isEmpty()) return it; if (!sim) slots[i] = it; return stack(0) } } }
function makeTank(mb, fluid) { return { mb: mb, fluid: fluid || 'liquid_essence', drained: 0,
  drain: function (want, action) { var n = want.fluid == this.fluid ? Math.min(want.amount, this.mb) : 0; if (action == 'exec') { this.mb -= n; this.drained += n } return { getAmount: function () { return n } } } } }
function capOf(h) { return { resolve: function () { return { orElse: function () { return h } } } } }
var altarGear = gear('Altar Sword'); faSetLevel(altarGear, 'sharpness', 5); faSetLevel(altarGear, 'protection', 50)
var depot = makeDepot(altarGear), tank = makeTank(100000)
var altarPos = new FakePos(10, 64, 10), blocks = { '10,64,10': 'kubejs:ascension_altar' }
var world2 = {
  dimension: function () { return { location: function () { return 'minecraft:overworld' } } },
  isLoaded: function () { return true },
  getBlockState: function (p2) { var b = blocks[p2.x + ',' + p2.y + ',' + p2.z] || 'minecraft:air'; return { getBlock: function () { return b } } },
  getBlockEntity: function (p2) { return p2.y == 65 ? { getCapability: function () { return capOf(depot) } } : p2.y == 63 ? { getCapability: function () { return capOf(tank) } } : null },
  addFreshEntity: function () {}, getServer: function () { return server }
}
server.getTickCount = function () { return 40 }
server.getAllLevels = function () { return [world2] }
server.getPlayerList = function () { return { getPlayers: function () { return [] }, getPlayerCount: function () { return 0 } } }
var placer = makePlayer(server, 'placer')
handlers.placed({ server: server, level: world2, block: { pos: altarPos }, player: placer, cancel: function () {} })
check('placing an altar registers it with its owner', server.persistentData.getCompound('fa_altars').getString('minecraft:overworld|10|64|10') == 'placer')
handlers.tick({ server: server })
var spentMb = 0
for (var L3 = 5; L3 < 25; L3++) spentMb += global.FA_cost(L3, 'sharpness') * 100
check('the altar levels the lowest enchant first, up to 20 levels a second', faGetLevel(depot.slots[0], 'sharpness') == 25 && faGetLevel(depot.slots[0], 'protection') == 50, faGetLevel(depot.slots[0], 'sharpness'))
check('...and drains exactly what those levels cost', tank.drained == spentMb, tank.drained + ' vs ' + spentMb)
check('the gear goes back on the depot', depot.slots[0] === altarGear)
check('altar levels count toward the owner\'s milestones', faStatGet(server, 'placer', 'altar_levels') == 20)
var water = makeTank(100000, 'water'); var realTank = tank; tank = water
handlers.tick({ server: server })
check('an altar over the wrong fluid does nothing', water.drained == 0 && faGetLevel(depot.slots[0], 'sharpness') == 25)
tank = makeTank(250)
handlers.tick({ server: server })
check('a nearly empty tank pays for what it can and stops (level 25 costs 200 mB)', tank.drained == 200 && tank.mb == 50 && faGetLevel(depot.slots[0], 'sharpness') == 26, tank.drained)
blocks = {}
handlers.tick({ server: server })
check('altars removed some other way are forgotten', !server.persistentData.getCompound('fa_altars').contains('minecraft:overworld|10|64|10'))

print('\nworld events')
var evServer = makeServer()
evServer.overworld = function () { return { isNight: function () { return false } } }
check('Blood Moon refuses to start in daytime', global.FA_startEvent(evServer, 'bloodMoon') === false && global.FA_EVENT === null)
check('Golden Hour starts and is announced', global.FA_startEvent(evServer, 'goldenHour') && global.FA_EVENT.type == 'goldenHour' && evServer.commands.some(function (c) { return /GOLDEN HOUR/.test(c) }))
var gh = dropEvent(plainHero, makeMob(), [stack(4)]); dropsHook(gh)
check('Golden Hour doubles loot', totalCount(gh) == 8, totalCount(gh))
global.FA_endEvent(evServer)
evServer.overworld = function () { return { isNight: function () { return true } } }
global.FA_startEvent(evServer, 'bloodMoon')
Math.random = function () { return 0.99 }
var bmMob = spawn(makeMob(), 6000)
check('Blood Moon: everything spawns +10 danger', bmMob.pdata.getInt('fa_danger') == 21, bmMob.pdata.getInt('fa_danger'))
Math.random = realRandom
global.FA_endEvent(evServer)
check('events end cleanly', global.FA_EVENT === null)

print('\nAscended bosses')
var bossMob = makeMob(); bossMob.x = 30000
Math.random = function () { return 0 }
spawn(bossMob, 30000)
Math.random = realRandom
check('deep spawns can become Ascended bosses', bossMob.pdata.getBoolean('fa_boss') && bossMob.persistent && /Lv/.test(bossMob.customName), bossMob.customName)
var lvl2 = bossMob.pdata.getInt('fa_danger')
var bossTaken = hit(plainHero, bossMob, 1000)
check('bosses take damage / (zone health x 60)', near(bossTaken, 1000 / (global.FA_dangerMult(lvl2, false).health * 60)), bossTaken)
check('Reaper can never execute a boss', faIsBoss(bossMob))
var frosty = hero({ frostbite: 50 }); hit(frosty, bossMob, 1)
check('bosses shrug off Slowness', !bossMob.effects.some(function (e) { return e.effect == 'slowness' }))
var fan = makePlayer(server, 'fan'); fan.x = 30002; bossMob.nearby = [fan]
fan.isCreative = function () { return false }
var tickBoss = handlers.forge['ServerTickEvent']
var bossServer = makeServer(); bossServer.getPlayerList = function () { return { getPlayers: function () { return [] }, getPlayerCount: function () { return 0 } } }
faLastSecond = 0; tickBoss({ phase: 'END', getServer: function () { return bossServer } })
var tracked = faBosses[bossMob.getStringUUID()] || faBosses['mob']
check('a boss bar shows to nearby players', tracked && tracked.bar.players.indexOf(fan) >= 0)
check('the boss slams nearby players away', fan.motion && fan.motion[0] > 0)
bossMob.hp = 12; spawnedMinions = []
faLastSecond = 0; tickBoss({ phase: 'END', getServer: function () { return bossServer } })
check('at 66% health it summons minions', spawnedMinions.length == global.FA.bosses.minionsPerPhase && spawnedMinions.every(function (m) { return m.pdata.getBoolean('fa_minion') }))
var bossDrops = dropEvent(plainHero, bossMob, []); dropsHook(bossDrops)
check('killing it drops Chaos Tomes, fragments and cores', ['kubejs:chaos_tome', 'kubejs:mythic_fragment', 'kubejs:dense_ascension_core'].every(function (id) { return bossDrops.list.arr.some(function (ie) { return ie.s.id == id }) }))
bossMob.removed = true
faLastSecond = 0; tickBoss({ phase: 'END', getServer: function () { return bossServer } })
check('dead bosses take their bar with them', !faBosses[bossMob.getStringUUID()] && tracked.bar.players.length == 0)

print('\nwebsite gear math vs the game')
load('site/calc.js')
// Random builds: write them onto fake gear, read them back with the real engine, compare with the site's math
var seed2 = 777
function rnd2(n) { seed2 = (seed2 * 16807) % 2147483647; return seed2 % n }
var mismatches = []
var slotKeys = ['main', 'off', 'head', 'chest', 'legs', 'feet']
for (var b = 0; b < 300; b++) {
  var build = { slots: {} }, pl = makePlayer(server, 'b' + b), items = {}
  slotKeys.forEach(function (sk) {
    if (rnd2(4) == 0) return
    var item = { e: {}, stars: rnd2(3), gems: [], rolled: [] }
    var g = gear(sk)
    for (var k = 0; k < 1 + rnd2(4); k++) { var id = ids[rnd2(ids.length)]; var lv = 1 + rnd2(5000); item.e[id] = lv; faSetLevel(g, id, lv) }
    if (rnd2(2)) { item.e.protection = 1 + rnd2(300); faSetLevel(g, 'protection', item.e.protection) }
    var gtypes = Object.keys(global.FA.gems.types)
    for (var k2 = 0; k2 < rnd2(4); k2++) {
      var gt = gtypes[rnd2(gtypes.length)], tier = 1 + rnd2(3)
      item.gems.push([gt, tier])
      var all = g.getOrCreateTag().getCompound('FactoryAscension'); var gemsTag = all.getCompound('gems'); var one = new Compound()
      one.putString('type', gt); one.putInt('tier', tier); gemsTag.put(String(k2), one); all.put('gems', gemsTag); g.tag.put('FactoryAscension', all)
    }
    for (var k3 = 0; k3 < rnd2(3); k3++) { var sd3 = 1 + rnd2(2000000000), rl = 1 + rnd2(800); item.rolled.push([sd3, rl]); faSetRolled(g, k3, sd3, rl) }
    if (item.stars) { var all2 = g.getOrCreateTag().getCompound('FactoryAscension'); all2.putInt('stars', item.stars); g.tag.put('FactoryAscension', all2) }
    build.slots[sk] = item; items[sk] = g
  })
  pl.main = items.main || stack(0); pl.off = items.off || stack(0)
  pl.armor = [items.head || stack(0), items.chest || stack(0), items.legs || stack(0), items.feet || stack(0)]
  var game = faLevels(pl), site = FA_CALC.levels(build)
  Object.keys(game).concat(Object.keys(site)).forEach(function (k4) { if (k4 != '__sets' && game[k4] !== site[k4] && mismatches.length < 5) mismatches.push('build ' + b + ' ' + k4 + ': game ' + game[k4] + ' site ' + site[k4]) })
}
check('300 random builds: the website gear builder reads gear exactly like the game', mismatches.length == 0, mismatches.join(' | '))
var tank2 = FA_CALC.summary(FA_CALC.levels({ slots: { chest: { e: { protection: 3000, vitality: 50 } } } }))
check('builder summary: Protection 3000 + Vitality 50 = same damage taken as in combat', near(tank2.taken, 1 / 121 * 20 / 120))
var costBuild = { slots: { main: { e: { sharpness: 100 } } } }
var expectCost = 0; for (var k5 = 0; k5 < 100; k5++) expectCost += global.FA_cost(k5, 'sharpness')
check('builder Essence cost matches the upgrade prices', FA_CALC.cost(costBuild) == expectCost)

print('\nsafety: recipes and hooks')
var outputs = {}
recipes.forEach(function (r) {
  var ins = (r.json.ingredients || []).map(function (i) { return i.item || 'fluid:' + i.fluid }).concat(r.json.ingredient ? [r.json.ingredient.item] : [])
  ;(r.json.results || []).forEach(function (o) {
    var out = o.item || 'fluid:' + o.fluid
    if (ins.indexOf(out) >= 0 && out != 'kubejs:incomplete_ascension_core') outputs[r.rid] = out
  })
})
check('no recipe gives back its own input (no duplication loops)', Object.keys(outputs).length == 0, JSON.stringify(outputs))
var core9 = byId('dense_ascension_core').json.ingredients.length, dense9 = byId('ascension_singularity').json.ingredients.length
check('compression recipes match Essence values (9 cores = 1 dense, 9 dense = 1 singularity)', core9 == 9 && dense9 == 9)
var boom = makeMob(); boom.hurt = function () { throw 'mob exploded' }
var victim3 = makeMob(); victim3.nearby = [boom]
quiet = true
hit(hero({ cleave: 5 }), victim3, 10)
quiet = false
var nextHit = makeMob(), victim4 = makeMob(); victim4.nearby = [nextHit]
hit(hero({ cleave: 5 }), victim4, 10)
check('a crash inside a secondary hit never leaves enchants switched off', nextHit.hurtBy.length == 1)
var reload = makeMob(); reload.pdata.putInt('fa_danger', 7)
spawn(reload, 60000, { fromDisk: true })
check('reloaded mobs keep their saved danger level', reload.pdata.getInt('fa_danger') == 7)

print('\n' + (failures ? failures + ' FAILED' : 'all checks passed'))
if (failures) java.lang.System.exit(1)
