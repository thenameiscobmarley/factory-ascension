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
var handlers = { registry: [], recipes: [], rightClicked: {}, commands: [], tooltip: [], forge: {}, tags: {}, data: [], toolTiers: [], armorTiers: [] }
var global = {}
var logged = []
var console = { info: function (m) { print('  [info] ' + m) }, warn: function (m) { logged.push(m) }, error: function (m) { logged.push(m); if (!quiet) print('  [error] ' + m) } }
var quiet = false

function fakeText(s) {
  return {
    s: String(s), click: null,
    append: function (o) { var t = fakeText(this.s + (o.s !== undefined ? o.s : o)); t.clicks = (this.clicks || []).concat(o.clicks || []); return t },
    bold: function () { return this },
    italic: function () { return this },
    clickRunCommand: function (c) { this.clicks = (this.clicks || []).concat([c]); return this },
    hover: function () { return this }
  }
}
var Text = {}
;['gold', 'gray', 'darkGray', 'lightPurple', 'red', 'darkRed', 'green', 'aqua', 'yellow', 'white', 'blue', 'of', 'darkAqua', 'darkPurple', 'darkGreen', 'darkBlue'].forEach(function (c) { Text[c] = fakeText })

var KNOWN_ITEMS = { 'mekanism:raw_osmium': 1, 'mekanism:raw_lead': 1, 'mekanism:raw_tin': 1, 'mekanism:raw_uranium': 1, 'tfmg:steel_ingot': 1, 'tfmg:raw_nickel': 1 }
var Item = { of: function (id, n) { return { id: id, count: n || 1, isEmpty: function () { return !KNOWN_ITEMS[id] },
  getHoverName: function () { return { getString: function () { return String(id).replace(/^[a-z0-9_]+:/, '') } } } } } }
var Ingredient = { all: 'ALL' }
// KubeJS's Fluid.of: what scripts must pass to fluid handlers (a Forge FluidStack arrives empty in KubeJS)
var Fluid = { of: function (id, amount) { return { fluid: id, amount: amount, getAmount: function () { return this.amount } } } }
var Platform = { isLoaded: function (m) { return m == 'tfmg' || m == 'mekanism' } }

// The mods that have items and blocks "in the game" for the missing-mod guard (tests change this list)
var installedContentMods = ['minecraft', 'kubejs', 'create', 'factoryascension']
var fakeModRegistry = { getKeys: function () { return javaList(installedContentMods.map(function (m) { return { getNamespace: function () { return m } } })) } }
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
  getAllKeys: function () { var l = javaList(Object.keys(this.d)); l.forEach = function (f) { this.arr.forEach(f) }; return l },
  putFloat: function (k, v) { this.d[k] = v },
  getFloat: function (k) { return this.d[k] || 0 }
}

function PlayerClass() {}
function LivingClass() {}
function EnemyClass() {}
function RaiderClass() {}
function OrbClass() {}
var MobType = { UNDEAD: 'undead', ARTHROPOD: 'arthropod', WATER: 'water', UNDEFINED: 'undefined' }
function ItemEntityClass(level, x, y, z, stack) { this.s = stack; this.setDefaultPickUpDelay = function () {} }
ItemEntityClass.prototype.getItem = function () { return this.s }
var modIds = 0
var fakeBlocks = {}, lastBlockProps = null
function FakeArmorItem(slot) { this.slot = slot || 'CHEST' }
FakeArmorItem.prototype.getEquipmentSlot = function () { return this.slot }
function FakeDiggerItem() {}
function FakeAxeItem() {}
function FakeBowItem() {}
var Java = {
  loadClass: function (n) {
    var classes = {
      'net.minecraft.world.entity.player.Player': PlayerClass,
      'net.minecraft.world.entity.item.ItemEntity': ItemEntityClass,
      'net.minecraft.tags.DamageTypeTags': { BYPASSES_INVULNERABILITY: 'bypass', IS_PROJECTILE: 'projectile', IS_FIRE: 'fire', IS_FALL: 'fall', IS_EXPLOSION: 'explosion' },
      'net.minecraft.world.entity.ai.attributes.Attributes': { ARMOR: 'armor', MAX_HEALTH: 'max_health', MOVEMENT_SPEED: 'movement_speed', ATTACK_SPEED: 'attack_speed', ATTACK_KNOCKBACK: 'attack_knockback', LUCK: 'luck' },
      'net.minecraft.world.entity.LivingEntity': LivingClass,
      'net.minecraft.world.entity.monster.Enemy': EnemyClass,
      'net.minecraft.world.entity.raid.Raider': RaiderClass,
      'net.minecraft.world.entity.MobType': MobType,
      'net.minecraft.world.entity.ExperienceOrb': OrbClass,
      'net.minecraft.world.effect.MobEffects': { SLOW_FALLING: 'slow_falling', GLOWING: 'glowing', MOVEMENT_SLOWDOWN: 'slowness', WEAKNESS: 'weakness', WITHER: 'wither', POISON: 'poison', LEVITATION: 'levitation', MOVEMENT_SPEED: 'speed', BLINDNESS: 'blindness', NIGHT_VISION: 'night_vision', WATER_BREATHING: 'water_breathing', FIRE_RESISTANCE: 'fire_resistance', DIG_SPEED: 'haste', JUMP: 'jump', SATURATION: 'saturation' },
      'net.minecraft.world.effect.MobEffectInstance': function (effect, ticks, amp) { this.effect = effect; this.ticks = ticks; this.amp = amp },
      'net.minecraft.world.entity.EntityType': {
        LIGHTNING_BOLT: { create: function () { return { setPosition: function () {}, setVisualOnly: function (v) { this.visual = v } } } },
        getKey: function (type) { return type.id }
      },
      'net.minecraft.tags.BlockTags': { LOGS: 'logs' },
      'net.minecraftforge.common.Tags$Blocks': { ORES: 'ores' },
      'net.minecraft.server.level.ServerBossEvent': FakeBossBar,
      'net.minecraftforge.registries.ForgeRegistries': { ITEMS: fakeModRegistry, BLOCKS: fakeModRegistry },
      'net.minecraft.world.BossEvent$BossBarColor': { RED: 'red' },
      'net.minecraft.world.BossEvent$BossBarOverlay': { NOTCHED_10: 'n10' },
      'net.minecraft.world.level.levelgen.Heightmap$Types': { MOTION_BLOCKING: 'mb' },
      'net.minecraft.core.BlockPos': FakePos,
      'net.minecraft.world.item.ArmorItem': FakeArmorItem,
      'net.minecraft.world.item.DiggerItem': FakeDiggerItem,
      'net.minecraft.world.item.ProjectileWeaponItem': FakeBowItem,
      'net.minecraft.world.item.AxeItem': FakeAxeItem,
      'net.minecraft.nbt.CompoundTag': Compound,
      'net.minecraft.world.item.ItemStack': { of: function (t) { var st = stack(1, 1); st.id = t.d.id; st.tag = t.d.tag || null; st.restored = true; return st } },
      'net.minecraftforge.common.capabilities.ForgeCapabilities': { ITEM_HANDLER: 'items', FLUID_HANDLER: 'fluids' },
      'net.minecraft.core.Direction': { UP: 'up', DOWN: 'down' },
      'net.minecraftforge.fluids.capability.IFluidHandler$FluidAction': { SIMULATE: 'sim', EXECUTE: 'exec' },
      'net.minecraft.resources.ResourceLocation': function (ns, path) { this.s = ns + ':' + path },
      'net.minecraftforge.common.ForgeMod': { BLOCK_REACH: { get: function () { return 'block_reach' } }, ENTITY_REACH: { get: function () { return 'entity_reach' } }, STEP_HEIGHT_ADDITION: { get: function () { return 'step_height' } } },
      'net.minecraft.world.entity.ai.attributes.AttributeModifier': function (name, amount, op) {
        this.name = name; this.amount = amount; this.op = op; this.id = 'uuid-' + (++modIds)
        this.getName = function () { return this.name }; this.getAmount = function () { return this.amount }; this.getId = function () { return this.id }
      },
      'net.minecraft.world.entity.ai.attributes.AttributeModifier$Operation': { ADDITION: 'add', MULTIPLY_BASE: 'mul_base' },
      'net.minecraft.world.phys.AABB': function FakeAABB(x0, y0, z0, x1, y1, z1) { this.box = [x0, y0, z0, x1, y1, z1] }
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
var jei = { hidden: [], info: {} }
var JEIEvents = {
  hideItems: function (fn) { fn({ hide: function (id) { jei.hidden.push(id) } }) },
  information: function (fn) { fn({ addItem: function (id, lines) { jei.info[id] = lines } }) }
}
var PlayerEvents = { loggedIn: function (fn) { (handlers.loggedIn = handlers.loggedIn || []).push(fn) } }
var BlockEvents = { placed: function (id, fn) { (handlers.placed = handlers.placed || {})[id] = fn }, broken: function (id, fn) { (handlers.broken = handlers.broken || {})[id] = fn },
  // Every generic block right-click handler runs, like in KubeJS (the hammer's and the room charge's)
  rightClicked: function (a, b) {
    var fn = typeof a == 'function' ? a : b
    if (typeof a != 'function') return
    var all = handlers.blockClicks = handlers.blockClicks || []
    all.push(fn)
    handlers.blockClick = function (e) { all.forEach(function (f) { f(e) }) }
  } }
// Like KubeJS, every handler registered for an event runs
function allOf(name, fn) { var list = handlers[name + 'List'] = handlers[name + 'List'] || []; list.push(fn); handlers[name] = function (e) { list.forEach(function (f) { f(e) }) } }
var ServerEvents = { loaded: function (fn) { allOf('loaded', fn) }, chestLootTables: function (fn) { handlers.chestLoot = fn }, recipes: function (fn) { handlers.recipes.push(fn) }, commandRegistry: function (fn) { handlers.commands.push(fn) }, tick: function (fn) { allOf('tick', fn) },
  tags: function (type, fn) { (handlers.tags[type] = handlers.tags[type] || []).push(fn) }, highPriorityData: function (fn) { handlers.data.push(fn) } }
var ItemEvents = { rightClicked: function (id, fn) { handlers.rightClicked[id] = fn }, tooltip: function (fn) { handlers.tooltip.push(fn) },
  crafted: function (fn) { handlers.crafted = fn },
  toolTierRegistry: function (fn) { handlers.toolTiers.push(fn) }, armorTierRegistry: function (fn) { handlers.armorTiers.push(fn) } }
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
    get: function (i) { return this.arr[i] },
    remove: function (i) { return this.arr.splice(i, 1)[0] },
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
    get nbt() { return this.tag },
    set nbt(t) { this.tag = t },
    getOrCreateTag: function () { if (!this.tag) this.tag = new Compound(); return this.tag },
    getHoverName: function () { var t = fakeText(this.name); t.getString = function () { return this.s }; return t },
    getId: function () { return this.id }, getItem: function () { return this.id },
    damage: 0, getDamageValue: function () { return this.damage },
    save: function (t) { t.d.id = this.id; t.d.tag = this.tag; return t }, setDamageValue: function (d) { this.damage = d }, getMaxDamage: function () { return this.maxDamage || 0 }
  }
}
// Fake gear knows what it is from its name, like a real item knows its class (armor, axe, tool or weapon)
function gear(name) {
  var g = stack(1, 1, name), n = String(name || '')
  if (/Helmet|Chestplate|Leggings|Boots/.test(n)) g.getItem = function () { return new FakeArmorItem(/Helmet/.test(n) ? 'HEAD' : /Leggings/.test(n) ? 'LEGS' : /Boots/.test(n) ? 'FEET' : 'CHEST') }
  else if (/ Axe$/.test(n)) g.getItem = function () { return new FakeAxeItem() }
  else if (/Pickaxe|Shovel|Hoe/.test(n)) g.getItem = function () { return new FakeDiggerItem() }
  else if (!g.getItem) g.getItem = function () { return {} }
  return g
}

function makeServer() {
  var data = new Compound()
  return { persistentData: data, commands: [], told: [], runCommandSilent: function (c) { this.commands.push(c) }, tell: function (t) { this.told.push(t.s) },
    getTickCount: function () { return gameTime }, scheduleInTicks: function (t, f) { f() },
    getPlayerList: function () { return { getPlayers: function () { var a = []; a.size = function () { return 0 }; return a } } } }
}

function makePlayer(server, uuid) {
  var p = new PlayerClass()
  var attrs = {}
  p.main = stack(0); p.off = stack(0); p.armor = [stack(0), stack(0), stack(0), stack(0)]
  p.health = 20; p.healed = 0; p.messages = []; p.bar = []; p.given = []; p.sneaking = false; p.attrs = attrs; p.effects = []
  p.getServer = function () { return server }
  p.pdata = new Compound()
  p.getPersistentData = function () { return this.pdata }
  p.stringUuid = uuid
  p.username = 'Tester'
  p.isShiftKeyDown = function () { return this.sneaking }
  p.getMainHandItem = function () { return this.main }
  p.getOffhandItem = function () { return this.off }
  p.getAllSlots = function () { return javaList([this.main, this.off].concat(this.armor)) }
  p.getAttribute = function (a) {
    var list = attrs[a] = attrs[a] || []
    return {
      getModifiers: function () { return javaList(list.slice()) },
      removeModifier: function (id) { for (var i = list.length - 1; i >= 0; i--) if (list[i].id === id) list.splice(i, 1) },
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
  p.fallDistance = 5
  p.displayClientMessage = function (t) { this.bar.push(t.s) }
  p.tell = function (t) { this.messages.push(t) }
  p.give = function (i) { this.given.push(i) }
  p.inv = []
  p.getInventory = function () { var self = this; return { getContainerSize: function () { return self.inv.length }, getItem: function (i) { return self.inv[i] } } }
  p.cooldowns = {}
  p.getCooldowns = function () { var self = this; return { isOnCooldown: function (it) { return !!self.cooldowns[it] }, addCooldown: function (it, t) { self.cooldowns[it] = t } } }
  return p
}
// Shared living-entity behaviour for fake players and mobs
var gameTime = 1000
var mobIds = 0
var bolts = []
var spawnedMinions = []
function livingBits(o, maxHealth) {
  o.hp = maxHealth; o.maxHp = maxHealth; o.hurtBy = []; o.effects = o.effects || []; o.fire = 0
  o.pdata = new Compound(); o.x = 0; o.z = 0
  o.getPersistentData = function () { return this.pdata }
  o.setCustomName = function (n) { this.customName = n.s }
  o.hasCustomName = function () { return !!this.customName }
  o.getCustomName = function () { return this.customName ? fakeText(this.customName) : null }
  o.setCustomNameVisible = function (v) { this.nameVisible = v }
  // KubeJS: e.type is the id string
  o.type = o.type || 'minecraft:zombie'
  o.getServer = o.getServer || function () { return server }
  o.extinguish = function () { this.fire = 0 }
  o.isBaby = function () { return !!this.baby }
  o.isOnFire = function () { return this.fire > 0 }
  o.setMotion = function (x, y, z) { this.motion = [x, y, z] }
  o.isRemoved = function () { return !!this.removed }
  o.getDisplayName = function () { return fakeText(this.customName || 'Zombie') }
  o.setPersistenceRequired = function () { this.persistent = true }
  o.mobType = MobType.UNDEFINED; o.ground = true; o.wet = false; o.dim = 'minecraft:overworld'; o.boss = false; o.nearby = []
  o.isAlive = function () { return true }
  o.isPassenger = function () { return !!this.riding }
  if (!o.blockPosition) o.blockPosition = function () { var self = this; return { x: Math.floor(self.x || 0), y: 64, z: Math.floor(self.z || 0), getX: function () { return this.x }, getY: function () { return 64 }, getZ: function () { return this.z } } }
  o.getHealth = function () { return this.hp }
  o.setHealth = function (h) { this.hp = h }
  o.getMaxHealth = function () { return this.maxHp }
  o.attack = function (src, amount) { this.hurtBy.push(amount); return true }
  o.addEffect = function (e) { this.effects.push(e) }
  o.setSecondsOnFire = function (s) { this.fire = s }
  o.getMobType = function () { return this.mobType }
  o.canChangeDimensions = function () { return !this.boss }
  o.isInWaterOrRain = function () { return this.wet }
  o.onGround = function () { return this.ground }
  // Every entity has its own id, like the game
  if (o.stringUuid === undefined) o.stringUuid = 'mob-' + (++mobIds)
  o.displayClientMessage = o.displayClientMessage || function () {}
  var self = o
  // KubeJS: entity.level and level.dimension are properties; time comes from the server's tick count
  o.level = {
      get dimension() { return self.dim },
      getServer: function () { return { getTickCount: function () { return gameTime }, runCommandSilent: function (c) { (self.levelCommands = self.levelCommands || []).push(c) },
        getPlayerList: function () { return { getPlayers: function () { var a = []; a.size = function () { return 0 }; return a } } },
        tell: function (t) { (self.levelTells = self.levelTells || []).push(t.s) } } },
      getBlockState: function () { return { isAir: function () { return true } } },
      isLoaded: function (pos) { return !self.unloaded }, isPositionEntityTicking: function () { return !self.unloaded },
      createEntity: function (t) { var m = makeMob(); m.type = t; spawnedMinions.push(m); return m },
      getSharedSpawnPos: function () { return { getX: function () { return 0 }, getZ: function () { return 0 } } },
      getDayTime: function () { return self.dayTime || 1000 },
      getBlock: function (pos) { var k = pos.x + ',' + pos.y + ',' + pos.z; var w = fakeBlocks; return { get id() { return w[k] || 'minecraft:air' }, set: function (id, props) { if (id == 'minecraft:air') delete w[k]; else w[k] = id; lastBlockProps = props } } },
      isClientSide: function () { return false },
      addFreshEntity: function (e) { bolts.push(e) },
      getEntitiesOfClass: function (cls, box) { var l = self.nearby.filter(function (e) { return e instanceof cls }); return { size: function () { return l.length }, get: function (i) { return l[i] } } }
  }
  o.getBoundingBox = function () { return { inflate: function () { return 'box' } } }
  o.getX = function () { return this.x }; o.getZ = function () { return this.z }; o.getY = function () { return this.y !== undefined ? this.y : 64 }
  if (o.yaw === undefined) o.yaw = 0
  if (o.pitch === undefined) o.pitch = 0
  o.age = o.age || 0
  // KubeJS position setters (moveTo/teleportTo are ambiguous in KubeJS, so scripts never use them)
  o.setPosition = function (x, y, z) { this.x = x; this.y = y; this.z = z; this.teleported = [x, y, z] }
  o.setPositionAndRotation = function (x, y, z, yaw, pitch) { this.x = x; this.y = y; this.z = z; this.yaw = yaw; this.pitch = pitch; this.teleports = (this.teleports || 0) + 1; this.teleported = [x, y, z] }
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
;['startup_scripts/00_fa_config.js', 'startup_scripts/01_fa_conflicts.js', 'startup_scripts/03_fa_mod_ids.js', 'startup_scripts/10_fa_items.js', 'startup_scripts/20_fa_enchant_effects.js',
  'startup_scripts/30_fa_world.js', 'startup_scripts/40_fa_ultimates.js', 'startup_scripts/50_fa_loot.js', 'startup_scripts/60_fa_mob_food.js', 'startup_scripts/70_fa_variants.js', 'startup_scripts/72_fa_mod_mobs.js', 'startup_scripts/76_fa_skinwalkers.js', 'startup_scripts/80_fa_combat.js', 'startup_scripts/90_fa_artifacts.js', 'startup_scripts/95_fa_attachments.js', 'startup_scripts/97_fa_mod.js',
  'server_scripts/05_fa_mod_guard.js', 'server_scripts/10_fa_ascension.js', 'server_scripts/15_fa_home.js', 'server_scripts/16_fa_packs.js', 'server_scripts/17_fa_sell.js', 'server_scripts/18_fa_attachments.js', 'server_scripts/19_fa_foils.js', 'server_scripts/20_fa_core_recipes.js', 'server_scripts/25_fa_tome_recipes.js',
  'server_scripts/30_fa_material_recipes.js', 'server_scripts/32_fa_ascendite_data.js', 'server_scripts/35_fa_starter_recipes.js', 'server_scripts/37_fa_food_recipes.js', 'server_scripts/38_fa_hammer.js', 'server_scripts/39_fa_condition.js',
  'server_scripts/40_fa_progress.js', 'server_scripts/45_fa_altar.js', 'server_scripts/46_fa_machines.js', 'server_scripts/47_fa_legendary.js', 'server_scripts/48_fa_rooms.js',
  'client_scripts/05_fa_mob_info.js', 'client_scripts/10_fa_tooltips.js', 'client_scripts/20_fa_jei.js'].forEach(function (f) {
  load('kubejs/' + f)
  print('loaded ' + f)
})
var E = global.FA.enchants

// ---------------- Items & textures ----------------
print('\nitems')
var created = [], createdBlocks = [], createdFluids = [], placeholders = []
var builder = {}
;['displayName', 'rarity', 'glow', 'maxStackSize', 'thinTexture', 'bucketColor', 'soundType', 'hardness', 'resistance', 'requiresTool',
  'tagBlock', 'lightLevel', 'textureAll', 'maxDamage', 'model', 'notSolid', 'renderType', 'texture'].forEach(function (m) { builder[m] = function () { return builder } })
var gearTiers = {}, foodDefs = {}
handlers.registry.forEach(function (r) {
  var list = r.type == 'item' ? created : r.type == 'block' ? createdBlocks : createdFluids
  r.fn({ create: function (id, type) {
    // Another namespace = a placeholder for the mod's own blocks and items (registered when its jar is missing)
    if (id.indexOf(':') >= 0) placeholders.push(id); else list.push(id)
    var b = {}
    Object.keys(builder).forEach(function (m) { b[m] = function () { return b } })
    b.tier = function (t) { gearTiers[id] = { type: type, tier: t }; return b }
    b.food = function (fn) {
      var f = foodDefs[id] = { effects: [] }
      var fb = { hunger: function (n) { f.hunger = n; return fb }, saturation: function (n) { f.saturation = n; return fb }, meat: function () { return fb },
        alwaysEdible: function () { f.always = true; return fb }, effect: function (e, t, a, p) { f.effects.push([e, t, a, p]); return fb } }
      fn(fb)
      return b
    }
    return b
  } })
})
var toolTiers = {}, armorTiers = {}
handlers.toolTiers.forEach(function (fn) { fn({ add: function (id, f) { f(toolTiers[id] = {}) } }) })
handlers.armorTiers.forEach(function (fn) { fn({ add: function (id, f) { f(armorTiers[id] = {}) } }) })
var AS = global.FA.ascendite
check('Ascendite tool and armor tiers come from the config', toolTiers.ascendite.uses == AS.tools.uses && toolTiers.ascendite.level == AS.tools.level &&
  armorTiers.ascendite.slotProtections === AS.armor.protection && armorTiers.ascendite.repairIngredient == 'kubejs:ascendite_ingot')
var gearIds = Object.keys(gearTiers)
var tierNames = ['ascendite'].concat(Object.keys(global.FA.gearTiers))
check('Ascendite, Voidsteel, Celestium and Ascendium each have 9 tools and armor pieces on their own tier', gearIds.length == 36 &&
  gearIds.every(function (id) { return id == gearTiers[id].tier + '_' + gearTiers[id].type && tierNames.indexOf(gearTiers[id].tier) >= 0 }), gearIds.length)
check('every tier is registered for tools and armor, repaired with its own ingot', tierNames.every(function (t) {
  return toolTiers[t] && armorTiers[t] && toolTiers[t].repairIngredient == 'kubejs:' + t + '_ingot' && armorTiers[t].repairIngredient == 'kubejs:' + t + '_ingot' }))
check('Voidsteel sits between diamond and netherite, Celestium and Ascendium past netherite', toolTiers.voidsteel.uses > 1561 && toolTiers.voidsteel.uses < 2031 &&
  toolTiers.celestium.uses > 2031 && toolTiers.ascendium.uses > toolTiers.celestium.uses && armorTiers.ascendium.toughness > armorTiers.celestium.toughness &&
  armorTiers.celestium.toughness > 3)
check('every tier\'s worn armor has both texture layers', tierNames.every(function (t) { return [1, 2].every(function (l) { return new java.io.File('kubejs/assets/kubejs/textures/models/armor/' + t + '_layer_' + l + '.png').exists() }) }))
var condTiers = global.FA_condensedTiers()
check('altar, condenser, 51 Condensed Essence blocks and 3 Ascendite blocks registered', createdBlocks.length == 2 + condTiers + 1 + 3 && createdBlocks.indexOf('essence_condenser') >= 0 &&
  createdBlocks.indexOf('condensed_essence_50') >= 0 && createdFluids.join() == 'liquid_essence', createdBlocks.length)
var missingBlockTex = createdBlocks.filter(function (b) { return !new java.io.File('kubejs/assets/kubejs/textures/block/' + b + '.png').exists() })
check('every block has a texture', missingBlockTex.length == 0, missingBlockTex.join(','))
var enchantCount = Object.keys(global.FA.enchants).length
var mythics = Object.keys(global.FA.enchants).filter(function (id) { return global.FA.enchants[id].mythic })
var fusionIds = Object.keys(global.FA.enchants).filter(function (id) { return global.FA.enchants[id].type == 'fusion' })
var ultIds = Object.keys(global.FA.enchants).filter(function (id) { return global.FA.enchants[id].ultimate })
check('487 enchants (60 + 15 Mythic + 200 Fusion + 200 Triad + 12 Ultimate)', enchantCount == 487 && mythics.length == 15 && fusionIds.length == 400 &&
  fusionIds.filter(function (id) { return global.FA.enchants[id].triad }).length == 200 && ultIds.length == 12,
  enchantCount + ' / ' + mythics.length + ' / ' + fusionIds.length + ' / ' + ultIds.length)
var gemCount = Object.keys(global.FA.gems.types).length * 3
var MF = global.FA.mobFood
check('items registered (4 core + blank + tomes + chaos + fragment + soul + gems + 3 clusters + 5 Ascendite + 9 gear + 26 foods + 2 hammers + polishing cloth + pack + box + cache + 3 tiers x (ingot + 9 gear) + attachments + foil sheets + room TNT + collars + whistle + charms + unbinding scroll)', created.length == 11 + enchantCount + gemCount + 14 + 2 * MF.foods.length + 6 + 30 + global.FA.attachments.list.length + global.FA.attachments.batteries.length + global.FA.machines.miniCrusher.motors.length + global.FA.foils.types.length +
  global.FA.roomCharges.styles.length * global.FA.roomCharges.sizes.length + global.FA.pets.collars.length + 1 + global.FA.charms.list.length + 1, created.length)
var vanillaMobs = {}
MF.foods.forEach(function (fd) { fd.mobs.forEach(function (m) { vanillaMobs[m] = true }) })
check(MF.foods.length + ' mob parts for ' + Object.keys(vanillaMobs).length + ' mobs: raw ones make you sick, cooked ones feed you and give their buff, both edible on a full stomach',
  MF.foods.length >= 45 && Object.keys(vanillaMobs).length >= 60 && MF.foods.every(function (fd) {
  var r = foodDefs[fd.raw], c = foodDefs[fd.cooked]
  return r && c && r.effects.some(function (e) { return e[0] == 'minecraft:hunger' }) && r.effects.length == MF.raw.sickness.length && r.always &&
    c.hunger == fd.hunger && c.effects.length == 1 && c.effects[0][0] == fd.effect[0] && c.effects[0][3] == 1 && c.always
}), MF.foods.length + ' / ' + Object.keys(vanillaMobs).length)
check('parts for the mod\'s own mobs only exist with the mod (Half download)', MF.foods.every(function (fd) { return !fd.mod }))
check('every mob part has a sprite', MF.foods.every(function (fd) {
  return new java.io.File('kubejs/assets/kubejs/textures/item/' + fd.raw + '.png').exists() && new java.io.File('kubejs/assets/kubejs/textures/item/' + fd.cooked + '.png').exists() }))
check('no conflicting mods here, so nothing was turned off', global.FA_CONFLICTS.length == 0 && global.FA.home.command && global.FA.danger.enabled)
// Off for the other tests (it adds random drops); its own tests switch it back on
MF.enabled = false
var File = java.io.File
var missing = created.filter(function (id) { return !new File('kubejs/assets/kubejs/textures/item/' + id + '.png').exists() })
check('every item has a texture', missing.length == 0, missing.join(','))
// Every texture is regenerated at 32x32; a 16x16 one means the generator skipped it (stale file)
function pngSize(path) { var d = java.nio.file.Files.readAllBytes(new File(path).toPath()); var n = function (o) { return ((d[o] & 255) << 24) | ((d[o + 1] & 255) << 16) | ((d[o + 2] & 255) << 8) | (d[o + 3] & 255) }; return n(16) + 'x' + n(20) }
var staleTex = created.map(function (id) { return 'kubejs/assets/kubejs/textures/item/' + id + '.png' }).concat(createdBlocks.map(function (b) { return 'kubejs/assets/kubejs/textures/block/' + b + '.png' }))
  .filter(function (f) { return new File(f).exists() && pngSize(f) != '32x32' })
check('every item and block texture is 32x32 (none skipped by tools/make_textures.py)', staleTex.length == 0, staleTex.slice(0, 5).join(', '))
// Every tome has its own design (tools/tome_art.py): no two tome textures may be the same picture
var tomeHashes = {}, sameTomes = []
created.filter(function (id) { return id.indexOf('tome_') == 0 }).forEach(function (id) {
  var md = java.security.MessageDigest.getInstance('MD5')
  var h = String(new java.math.BigInteger(1, md.digest(java.nio.file.Files.readAllBytes(new File('kubejs/assets/kubejs/textures/item/' + id + '.png').toPath()))).toString(16))
  if (tomeHashes[h]) sameTomes.push(tomeHashes[h] + ' = ' + id); else tomeHashes[h] = id
})
check('all ' + enchantCount + ' enchant tomes have a unique design', sameTomes.length == 0 && Object.keys(tomeHashes).length == enchantCount, sameTomes.slice(0, 3).join(', '))

// ---------------- Recipes ----------------
print('\nrecipes')
var recipes = []
var ev = {
  custom: function (j) { var r = { json: j, id: function (i) { r.rid = i; return r } }; recipes.push(r); return r },
  shapeless: function (o, i) { var r = { json: { type: 'shapeless', out: o, ins: i }, id: function (x) { r.rid = x; return r } }; recipes.push(r); return r },
  shaped: function (o, p, k) { var r = { json: { type: 'shaped', out: o, key: k, pattern: p }, id: function (x) { r.rid = x; return r } }; recipes.push(r); return r }
}
;['smelting', 'blasting', 'smoking', 'campfireCooking'].forEach(function (t) {
  ev[t] = function (o, i) { var r = { json: { type: t, out: o, input: i }, xp: function () { return r }, cookingTime: function (n) { r.json.time = n; return r }, id: function (x) { r.rid = x; return r } }; recipes.push(r); return r }
})
handlers.recipes.forEach(function (fn) { fn(ev) })
function byId(id) { return recipes.filter(function (r) { return r.rid == 'kubejs:factory_ascension/' + id })[0] }
check('core recipe has 4 steps with TFMG steel', byId('ascension_core').json.sequence.length == 4)
check('every recipe has an id', recipes.every(function (r) { return !!r.rid }))
// Minecraft refuses a shaped recipe whose key has a symbol the pattern doesn't use (found on a real server)
var badKeys = recipes.filter(function (r) { return r.json.type == 'shaped' && Object.keys(r.json.key).some(function (k) { return r.json.pattern.join('').indexOf(k) < 0 }) })
check('every shaped recipe uses every key symbol', badKeys.length == 0, badKeys.map(function (r) { return r.rid }).join(', '))
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
// More ways to make Ascension Cores, none of them sequenced assembly
var coreWays = recipes.filter(function (r) { return r.rid != 'kubejs:factory_ascension/ascension_core' &&
  (r.json.out == 'kubejs:ascension_core' || (r.json.results && r.json.results.length == 1 && r.json.results[0].item == 'kubejs:ascension_core')) })
check('Ascension Cores have 6 more recipes without sequenced assembly, one with no heat at all', coreWays.length == 6 && coreWays.every(function (r) { return r.json.type != 'create:sequenced_assembly' }) && coreWays.some(function (r) { return r.json.type == 'create:mixing' && !r.json.heatRequirement }),
  coreWays.map(function (r) { return r.rid }).join(','))
var mill = byId('mill_raw_ascendite')
check('a Millstone grinds Raw Ascendite, with a chance of a shard (early, no heat)', mill && mill.json.type == 'create:milling' &&
  mill.json.results.some(function (r) { return r.item == 'kubejs:ascension_shard' && r.chance == global.FA.ascendite.millShardChance }))
check('Ascendite ores and raw ore smelt into ingots', ['raw_ascendite', 'crushed_raw_ascendite', 'ascendite_ore', 'deepslate_ascendite_ore'].every(function (id) {
  return recipes.some(function (r) { return r.json.type == 'smelting' && r.json.input == 'kubejs:' + id && r.json.out == 'kubejs:ascendite_ingot' }) }))
check('every Ascendite tool and armor piece has a recipe', gearIds.every(function (id) { return !!byId(id) }))
check('every raw mob food cooks in a furnace, smoker and campfire', global.FA.mobFood.foods.every(function (fd) {
  return ['smelting', 'smoking', 'campfireCooking'].every(function (t) { return recipes.some(function (r) { return r.json.type == t && r.json.input == 'kubejs:' + fd.raw && r.json.out == 'kubejs:' + fd.cooked }) }) }))
var partTomes = Object.keys(global.FA_TOME_CATALYSTS).filter(function (t) { return global.FA_TOME_CATALYSTS[t].some(function (i) { return global.FA.mobFood.foods.some(function (fd) { return i == 'kubejs:' + fd.raw || i == 'kubejs:' + fd.cooked }) }) })
check('13 tomes use a mob part tied to their effect (Night Sight: Fried Pig Eyes, Vitality: Seared Cow Heart...)', partTomes.length == 13 &&
  global.FA_TOME_CATALYSTS.night_sight.indexOf('kubejs:fried_pig_eyes') >= 0 && global.FA_TOME_CATALYSTS.vitality.indexOf('kubejs:seared_cow_heart') >= 0, partTomes.join(','))
check('starter recipes: brass, precision mechanism, blaze burner, TFMG steel', ['starter_brass', 'starter_precision_mechanism', 'starter_blaze_burner', 'starter_steel'].every(function (id) { return !!byId(id) }))
// ---- Factory Room TNT: the room's shape ----
var roomPlan = global.FA_roomPlan('50', 100, 10, 200, 0, -1)   // facing north
var rc = { air: 0, shell: 0, light: 0 }, roomSpots = []
for (var ri = 0; ri < roomPlan.total; ri++) { var sp = global.FA_roomSpot(roomPlan, ri); if (sp.kind) { rc[sp.kind]++; roomSpots.push(sp) } }
var door = global.FA.roomCharges.doorway
check('a 50x50 room: 50 x 50 x 16 hollowed out plus a ' + door[0] + 'x' + door[1] + ' doorway (' + rc.air + ' air)', rc.air == 50 * 50 * 16 + door[0] * door[1], rc.air)
check('it has a full shell: floor, roof, four walls (' + rc.shell + ' blocks) with ' + rc.light + ' lights in the roof',
  rc.shell + rc.light + rc.air == 52 * 52 * 18 && rc.light >= 64 && roomSpots.filter(function (x) { return x.kind == 'light' }).every(function (x) { return x.y == 10 + 17 }), rc.shell + ' / ' + rc.light)
check('the room opens away from you (facing north: all of it is north of the clicked block) and the floor is the layer under your feet',
  roomSpots.every(function (x) { return x.z <= 200 && x.y >= 10 && x.y <= 27 }) && roomSpots.some(function (x) { return x.z == 200 - 51 }) &&
  global.FA_roomSpot(roomPlan, 0).y == 27, '')
check('every direction works the same (east: room east of you)', [[1, 0], [-1, 0], [0, 1]].every(function (f) {
  var pl = global.FA_roomPlan('16', 0, 0, 0, f[0], f[1]), ok = true, n = 0
  for (var k = 0; k < pl.total; k++) { var q = global.FA_roomSpot(pl, k); if (q.kind == 'air') { n++; if ((q.x * f[0] + q.z * f[1]) < 0) ok = false } }
  return ok && n == 16 * 16 * 8 + door[0] * door[1] }))
check('the roof goes in first (top layer comes before anything below it)', global.FA_roomSpot(roomPlan, 0).y > global.FA_roomSpot(roomPlan, roomPlan.total - 1).y)
check('Factory Room TNT: TNT + 4 wall blocks + 2 lights, and each size up takes ' + global.FA.roomCharges.sizes[1].craft + ' then ' + global.FA.roomCharges.sizes[2].craft + ' of the last',
  global.FA.roomCharges.styles.every(function (st) {
    var small = byId('room_tnt_' + st.id + '_16'), mid = byId('room_tnt_' + st.id + '_32'), big = byId('room_tnt_' + st.id + '_50')
    return small && mid && big && JSON.stringify(small.json).indexOf('minecraft:tnt') >= 0 && JSON.stringify(mid.json).split('room_tnt_' + st.id + '_16').length - 1 == 4
  }))
check('easy start: casings by hand, belts from leather, bulk cogwheels and shafts', ['easy_andesite_casing', 'easy_belt', 'easy_cogwheels', 'easy_shafts'].every(function (id) { return !!byId(id) }))
check('the mod machine recipes and rules stay off without the mod (Half download)', !byId('machine_salvager') && !byId('machine_engine_core') && !global.FA_onMachine)
check('every mod machine has a config recipe, and the engine needs nothing past iron and copper', ['salvager', 'tome_fusion_table', 'super_smelter', 'engine_core', 'engine_coil',
  'engine_gearbox', 'engine_burner'].every(function (b) { return !!global.FA.machines.recipes[b] }) &&
  ['engine_core', 'engine_coil', 'engine_gearbox', 'engine_burner'].every(function (b) { var k = global.FA.machines.recipes[b][1]; return Object.keys(k).every(function (c) { return k[c].indexOf('kubejs:') != 0 }) }))
check('smelter tiers line up with its upgrades', global.FA.machines.smelter.tiers.length == global.FA.machines.smelter.upgrades.length + 1)
// Tags and the ore's loot table
var tagged = {}
;['item', 'block'].forEach(function (t) { (handlers.tags[t] || []).forEach(function (fn) { fn({ add: function (tag, id) { (tagged[t + ' ' + tag] = tagged[t + ' ' + tag] || []).push(id) } }) }) })
check('Ascendite ores are forge:ores (Vein Miner and ore finders see them)', (tagged['block forge:ores'] || []).length == 2 && (tagged['item forge:ingots/ascendite'] || []).length == 1)
var dataJson = {}
handlers.data.forEach(function (fn) { fn({ addJson: function (id, j) { dataJson[id] = j } }) })
var oreLoot = dataJson['kubejs:loot_tables/blocks/ascendite_ore']
check('ore loot: raw ore with Fortune, Silk Touch keeps the ore, shard chance from the config', !!oreLoot && JSON.stringify(oreLoot).indexOf('minecraft:ore_drops') >= 0 &&
  JSON.stringify(oreLoot).indexOf('silk_touch') >= 0 && oreLoot.pools[1].conditions[1].chances[0] == AS.ore.shardChance && !!dataJson['kubejs:loot_tables/blocks/deepslate_ascendite_ore'])
// Ore worldgen is a datapack (KubeJS worldgen doesn't work on 1.20.1); it must match the config
var FA_WORLDGEN_CHECK = true
load('tools/make_worldgen.js')
var wg = FA_worldgenFiles(AS.ore), stale = []
Object.keys(wg).forEach(function (path) {
  var f = new java.io.File(path)
  var onDisk = f.exists() ? JSON.parse(String(new java.lang.String(java.nio.file.Files.readAllBytes(f.toPath()), 'UTF-8'))) : null
  if (JSON.stringify(onDisk) != JSON.stringify(wg[path])) stale.push(path)
})
check('ore worldgen files match the config (else run tools/make_worldgen.js)', stale.length == 0, stale.join(', '))

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
check('commands registered (/ascension, /home, /sethome, /ascension sell, /ascension detach, /ascension legendary, /ascension modguard)', handlers.commands.length == 6)

// ---------------- Effects ----------------
print('\ndamage')
var hurt = handlers.forge['LivingHurtEvent']
function src(attacker, opts) {
  opts = opts || {}
  return {
    actual: attacker,
    immediate: opts.direct !== undefined ? opts.direct : attacker,
    is: function (tag) { return tag == 'bypass' ? !!opts.bypass : opts.tags ? opts.tags.indexOf(tag) >= 0 : false },
    getType: function () { return opts.msg || 'player' }
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
  return { list: list, looting: 0, getEntity: function () { return victim2 }, getSource: function () { return src(attacker) }, getDrops: function () { return list },
    getLootingLevel: function () { return this.looting } }
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
var loose = new ItemEntityClass(); loose.hasPickUpDelay = function () { return false }; loose.setPosition = function () { this.moved = true }
var thrown = new ItemEntityClass(); thrown.hasPickUpDelay = function () { return true }; thrown.setPosition = function () { this.moved = true }
camper.nearby = [loose, thrown]
camper.age = 40
tick({ phase: 'END', player: camper })
check('Renewal 8 heals 2 per second', camper.healed == 2)
check('Night Sight keeps night vision (20s, no flicker)', camper.effects.some(function (e) { return e.effect == 'night_vision' && e.ticks == 400 }))
check('Industrious capped at its max level', camper.effects.some(function (e) { return e.effect == 'haste' && e.amp == 2 }))
check('Restoration 5 repairs 5 durability', worn.damage == 15)
check('Magnetism pulls loose items, not ones you just threw', loose.moved && !thrown.moved)
camper.age = 41; camper.healed = 0
tick({ phase: 'END', player: camper })
check('only runs once a second', camper.healed == 0)

var digger = hero({ excavation: 4 })
var bs = { speed: 8, getEntity: function () { return digger }, getNewSpeed: function () { return this.speed }, setNewSpeed: function (s) { this.speed = s } }
handlers.forge['BreakSpeed'](bs)
check('Excavation 4 = x2 mining speed', near(bs.speed, 16), bs.speed)

// ---------------- Tooltips ----------------
print('\ntooltips')
var advanced = null, simple = 0, detailTips = {}, shiftDown = false, detailLore = {}
// Register the tooltips as the Full download would (the mod's blocks and eggs too)
var tipIsLoaded = Platform.isLoaded
Platform.isLoaded = function (m) { return m == 'factoryascension' || tipIsLoaded(m) }
handlers.tooltip.forEach(function (fn) { fn({ add: function () { simple++ }, isShift: function () { return shiftDown },
  addAdvanced: function (ing, f) { if (ing === Ingredient.all) advanced = f; else detailTips[ing] = f } }) })
Platform.isLoaded = tipIsLoaded
// Every item and block of the mod (from its lang file) has details too
var modLang = JSON.parse(readFile('mod/src/main/resources/assets/factoryascension/lang/en_us.json'))
var modItems = Object.keys(modLang).filter(function (k) { return /^(item|block)\.factoryascension\.[a-z_]+$/.test(k) }).map(function (k) { return 'factoryascension:' + k.split('.')[2] })
var modNoDetails = modItems.filter(function (id) { return !detailTips[id] })
check('every mod block and spawn egg has a detail tooltip (' + modItems.length + ')', modItems.length >= 49 && modNoDetails.length == 0, modNoDetails.slice(0, 8).join(', '))
// Every pack item gets a flavor line plus Shift details, and none of them print undefined or NaN
var allPackItems = created.concat(createdBlocks).map(function (x) { return 'kubejs:' + x }).concat(['kubejs:liquid_essence_bucket'])
var noDetails = allPackItems.filter(function (id) { return !detailTips[id] })
check('every pack item and block has a detail tooltip', noDetails.length == 0, noDetails.slice(0, 8).join(', '))
var badTips = []
function renderTip(id, shift) {
  shiftDown = shift
  var out = { arr: [], add: function (t) { this.arr.push(t.s) } }
  detailTips[id](stack(1), false, out)
  return out.arr
}
Object.keys(detailTips).forEach(function (id) {
  var short = renderTip(id, false), long = renderTip(id, true)
  if (short.length != 2 || !/Shift/.test(short[1]) || long.length < 2 || long.concat(short).some(function (l) { return /undefined|NaN|null|\[object/.test(l) })) badTips.push(id + ': ' + long.join(' / '))
})
shiftDown = false
// Every food has its own flavor line, raw and cooked
var foodLines = []
global.FA.mobFood.foods.forEach(function (fd) { foodLines.push(renderTip('kubejs:' + fd.raw, false)[0], renderTip('kubejs:' + fd.cooked, false)[0]) })
var foodDupes = foodLines.filter(function (l, i) { return foodLines.indexOf(l) != i })
check('every food has its own description (' + foodLines.length + ' lines, none repeated)', foodDupes.length == 0, foodDupes.slice(0, 3).join(' | '))
var upgradeTips = ['kubejs:ascension_core', 'kubejs:ascendite_ingot', 'kubejs:ascendite_sword', 'kubejs:voidsteel_chestplate', 'kubejs:rock_hammer',
  'factoryascension:ascension_vault', 'factoryascension:super_smelter', 'factoryascension:engine_core'].filter(function (id) { return !/Upgrade/.test(renderTip(id, true).join(' ')) })
check('tiered items say what their next tier needs', upgradeTips.length == 0, upgradeTips.join(', '))
print('  sample Shift details: ' + renderTip('factoryascension:super_smelter', true).join(' | '))
shiftDown = false
check('detail tooltips: a hint without Shift, real details with it, no undefined/NaN', badTips.length == 0, badTips.slice(0, 3).join(' || '))
print('  sample Shift details: ' + renderTip('kubejs:tome_sharpness', true).join(' | '))
print('  sample Shift details: ' + renderTip('kubejs:ascendite_chestplate', true).join(' | '))
check('tooltips for cores, blank tome, every tome, fragment and clusters', simple == Object.keys(global.FA.essenceValues).length + 1 + enchantCount + 1 + 1 + 1 + 4 + gemCount + 2 + (global.FA_condensedTiers() + 1) + 1, simple)
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
// These tests measure from world spawn (danger.centerOn 'spawn'); 'danger from home' below covers the default
var defaultCenterOn = global.FA.danger.centerOn
global.FA.danger.centerOn = 'spawn'
var join = handlers.forge['EntityJoinLevelEvent']
function spawn(mob2, x, opts) {
  opts = opts || {}
  mob2.x = x
  if (opts.dim) mob2.dim = opts.dim
  join({ loadedFromDisk: function () { return !!opts.fromDisk }, getEntity: function () { return mob2 }, getLevel: function () { return mob2.level } })
  return mob2
}
Math.random = function () { return 0.5 }
check('mobs near spawn stay normal', !spawn(makeMob(), 500).pdata.contains('fa_danger'))
var far = spawn(makeMob(), 6000)
check('6000 blocks out = danger 21 (a level every 250 blocks past 1000)', far.pdata.getInt('fa_danger') == 21, far.pdata.getInt('fa_danger'))
check('mobs loaded from a save are not re-rolled', !spawn(makeMob(), 6000, { fromDisk: true }).pdata.contains('fa_danger'))
var farBoss = makeMob(); farBoss.boss = true
check('bosses never scale', !spawn(farBoss, 6000).pdata.contains('fa_danger'))
check('passive mobs never scale', !spawn(makeMob(LivingClass), 6000).pdata.contains('fa_danger'))
check('Nether: 1000 blocks counts as 8000, +4 bonus = 33', spawn(makeMob(), 1000, { dim: 'minecraft:the_nether' }).pdata.getInt('fa_danger') == 33)
check('the End is always at least danger 7', spawn(makeMob(), 0, { dim: 'minecraft:the_end' }).pdata.getInt('fa_danger') == 7)
Math.random = function () { return 0 }
var elite = spawn(makeMob(), 6000)
check('elites get a visible name with their level', elite.pdata.getBoolean('fa_elite') && elite.nameVisible && /Elite Zombie .* Lv 21/.test(elite.customName), elite.customName)
Math.random = realRandom

var plainHero = makePlayer(server, 'plain')
// The curve: x(1 + 0.5 * (blocks / 1000) ^ 2.807), by where the zone starts. Danger 21 starts at 6000 blocks
var curve = 1 + 0.5 * Math.pow(6, 2.807)
check('danger 21 mob deals x' + curve.toFixed(1) + ' damage', near(hit(far, plainHero, 10), 10 * curve), hit(far, plainHero, 10))
check('danger 21 mob takes damage / ' + curve.toFixed(1), near(hit(plainHero, far, 10 * curve), 10))
check('elite takes another /3', near(hit(plainHero, elite, 30 * curve), 10))
var dm = function (l) { return global.FA_dangerMult(l, false).health }
check('x1.5 at 1000 blocks, x4.5 at 2000, with 3 steps in between (' + [1, 2, 3, 4, 5].map(function (l) { return dm(l).toFixed(2) }).join(', ') + ')',
  near(dm(1), 1.5) && Math.abs(dm(5) - 4.5) < 0.02 && dm(1) < dm(2) && dm(2) < dm(3) && dm(3) < dm(4) && dm(4) < dm(5) && dm(0) == 1)
check('then x11.9 at 3000 and x47 at 5000', Math.abs(dm(9) - 11.9) < 0.1 && Math.abs(dm(17) - 46.8) < 0.3, dm(9) + ' / ' + dm(17))
check('/kill still works on scaled mobs', hit(null, far, 50, { bypass: true }) == 50)
var tenMob = makeMob(); tenMob.pdata.putInt('fa_danger', 10)
// No loot roll or fragment this time (they're random), so only the drop multiplier is measured
Math.random = function () { return 0.999 }
var d6 = dropEvent(plainHero, tenMob, [stack(4)]); dropsHook(d6)
Math.random = realRandom
var m10 = global.FA_dangerMult(10, false)
check('danger 10 (3,250 blocks) multiplies drops on the same curve as mob health: x' + m10.loot.toFixed(1), Math.abs(totalCount(d6) - 4 * m10.loot) <= 1 && near(m10.loot, m10.health), totalCount(d6))

// ---- Mob food ----
MF.enabled = true
var dropIds = function (ev) { return ev.list.arr.map(function (ie) { return ie.getItem().id }).filter(function (x) { return !!x }) }
var pig = makeMob(); pig.type = 'minecraft:pig'
Math.random = function () { return 0 }
var f1 = dropEvent(plainHero, pig, []); dropsHook(f1)
check('a pig drops Pig Eyes and a Pig Brain (raw)', dropIds(f1).join() == 'kubejs:pig_eyes,kubejs:pig_brain', dropIds(f1).join())
pig.fire = 5
var f2 = dropEvent(plainHero, pig, []); dropsHook(f2)
check('a pig that dies burning drops them cooked', dropIds(f2).join() == 'kubejs:fried_pig_eyes,kubejs:roasted_pig_brain', dropIds(f2).join())
var piglet = makeMob(); piglet.type = 'minecraft:pig'; piglet.baby = true
var f3 = dropEvent(plainHero, piglet, []); dropsHook(f3)
check('baby animals drop no parts', dropIds(f3).length == 0)
Math.random = function () { return 0.32 }
var f4 = dropEvent(plainHero, pig, []); pig.fire = 0; dropsHook(f4)
var f5 = dropEvent(plainHero, pig, []); f5.looting = 3; dropsHook(f5)
check('Looting raises the drop chance (a 32% roll: nothing at Looting 0; Eyes 45% and Brain 35% at Looting III)', dropIds(f4).length == 0 && dropIds(f5).join() == 'kubejs:pig_eyes,kubejs:pig_brain', dropIds(f4).join() + ' / ' + dropIds(f5).join())
Math.random = realRandom
MF.enabled = false
var xpEv2 = { xp: 10, getEntity: function () { return tenMob }, getAttackingPlayer: function () { return plainHero }, getDroppedExperience: function () { return this.xp }, setDroppedExperience: function (n) { this.xp = n } }
xpHook(xpEv2)
check('danger 10 multiplies XP on the curve too', Math.abs(xpEv2.xp - 10 * m10.xp) <= 1, xpEv2.xp)

var explorer = makePlayer(server, 'explorer')
explorer.age = 20
server.commands = []
tick({ phase: 'END', player: explorer })
explorer.x = 6000
tick({ phase: 'END', player: explorer })
check('crossing into a new zone shows its title', server.commands.some(function (c) { return /The Abyss/.test(c) }), server.commands.join(' | '))
check('going deeper plays a cave sound', server.commands.some(function (c) { return /ambient.cave/.test(c) }))
server.commands = []; explorer.bar = []
explorer.x = 6600
tick({ phase: 'END', player: explorer })
check('a new level inside the same zone shows on the action bar only', server.commands.length == 0 && /Danger 23/.test(explorer.bar[0]), explorer.bar[0])
explorer.x = 0
tick({ phase: 'END', player: explorer })
check('heading home shows Safe Lands', server.commands.some(function (c) { return /Safe Lands/.test(c) }))
// Logging in out in a danger zone announces it; logging in at home stays quiet
var returning = makePlayer(server, 'returning'); returning.age = 20; returning.x = 6000
server.commands = []
check('logging in inside a danger zone shows its title', global.FA_dangerNotice(returning) == 'zone' && server.commands.some(function (c) { return /The Abyss/.test(c) }))
var homebody = makePlayer(server, 'homebody'); homebody.age = 20
check('logging in at spawn shows nothing', global.FA_dangerNotice(homebody) == '')
// KubeJS hands back a fresh copy of config objects on every read of global: zones must compare by name
var realZone = global.FA_zone
global.FA_zone = function (d) { var z = realZone(d); return { from: z.from, name: z.name, color: z.color } }
returning.x = 6600
check('a new level in the same zone is not a new zone, even with copied zone objects', global.FA_dangerNotice(returning) == 'level')
global.FA_zone = realZone
handlers.forge['PlayerLoggedOutEvent']({ getEntity: function () { return returning } })
server.commands = []
check('after logging out and back in, the zone is announced again', global.FA_dangerNotice(returning) == 'zone')
explorer.messages = []
explorer.x = 6000
faShowDanger(explorer)
check('/ascension danger explains the area', explorer.messages.length == 4 && /The Abyss/.test(explorer.messages[0].s), explorer.messages.map(function (t) { return t.s }).join(' | '))
print('  sample: ' + explorer.messages.map(function (t) { return t.s }).join(' |'))
var zones = [0, 1000, 3500, 6000, 11000, 21000, 41000, 100000].map(function (x) {
  var lvl = global.FA_dangerLevel(explorer.level, x, 0)
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
flyer2.age = 20
tick({ phase: 'END', player: flyer2 })
check('Wings grants flight at x1.5 speed', flyer2.abilities.mayfly && near(flyer2.abilities.speed, 0.075))
flyer2.armor[1] = stack(0)
tick({ phase: 'END', player: flyer2 })
check('taking Wings off takes flight away', !flyer2.abilities.mayfly && flyer2.abilities.speed == 0.05)
var builder2 = hero({ wings: 1 }, 'builder'); builder2.age = 20; builder2.creative = true
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
// KubeJS block container: level.getBlock(pos) with .id and .hasTag(tagId)
function state(block) { return { id: 'minecraft:' + block.name, hasTag: function (tag) { return tag == 'forge:ores' ? block === oreBlock : tag == 'minecraft:logs' ? !!block.log : false } } }
function bpos(x, y, z) { return { getX: function () { return x }, getY: function () { return y }, getZ: function () { return z }, offset: function (a, b, c) { return bpos(x + a, y + b, z + c) } } }
var world = { isLoaded: function () { return true }, getBlock: function (p2) { var inCube = Math.abs(p2.getX()) <= 1 && Math.abs(p2.getY()) <= 1 && Math.abs(p2.getZ()) <= 1; return state(inCube ? oreBlock : stone) } }
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
shiny.nearby = [r1, r2]; shiny.age = 20
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
check('fragment odds climb on the loot curve: Elite 15.5% at danger 1, capped 75% by danger 20; normal 0.2% at danger 1, capped 5%',
  near(global.FA_fragmentChance(1, true), 0.155) && near(global.FA_fragmentChance(20, true), 0.75) && near(global.FA_fragmentChance(1, false), 0.002) &&
  near(global.FA_fragmentChance(20, false), 0.05) && global.FA_fragmentChance(5, true) > global.FA_fragmentChance(3, true))

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
handlers.rightClicked['kubejs:gem_topaz_3']({ player: jew, item: perfect, hand: 'MAIN_HAND', cancel: function () {} })
jew.sneaking = false
check('sneak replaces the newest gem', faGems(jewGear).filter(function (g) { return g.type == 'topaz' && g.tier == 3 }).length == 1 && faGems(jewGear).length == 3)
jew.main = jewGear; jew.off = stack(0)
check('gems stack with stars and sets like any level', faLevels(jew).plunder == 75 && faLevels(jew).sharpness == 6)
// Gear match: a Sapphire (Protection) won't go in a sword, and tomes and Chaos rolls follow the same rule
var sap = stack(1); jew.main = sap; jew.off = gear('Plain Sword')
handlers.rightClicked['kubejs:gem_sapphire_1']({ player: jew, item: sap, hand: 'MAIN_HAND', cancel: function () {} })
check('a Sapphire (Protection) won\'t go in a sword, and is kept', sap.count == 1 && faGems(jew.off).length == 0)
var helm = gear('Iron Helmet'), stome = stack(1); jew.main = stome; jew.off = helm
handlers.rightClicked['kubejs:tome_sharpness']({ player: jew, item: stome, hand: 'MAIN_HAND', cancel: function () {} })
check('Sharpness won\'t go on a helmet (it says it goes on weapons), the tome is kept', faGetLevel(helm, 'sharpness') == 0 && stome.count == 1)
check('enchants know what they suit: Sharpness weapons, Protection armor, Excavation tools, Mending-style ones anything',
  global.FA_fitsText('sharpness') == 'weapons' && global.FA_fitsText('protection') == 'armor' && global.FA_enchantFits('excavation', 'tool') && global.FA_enchantFits('excavation', 'axe') &&
  ['weapon', 'armor', 'tool'].every(function (k) { return global.FA_enchantFits('eternal', k) }), global.FA_fitsText('excavation'))
check('every enchant suits at least one kind of gear', Object.keys(global.FA.enchants).every(function (id) { return global.FA_fitsText(id) != 'nothing' }),
  Object.keys(global.FA.enchants).filter(function (id) { return global.FA_fitsText(id) == 'nothing' }).join(', '))
var helmSeeds = 0, helmBad = 0
for (var hs = 1; hs < 60; hs++) {
  var fs = global.FA_fittingSeed(hs * 104729, 'armor')
  helmSeeds++
  if (!global.FA_rollEnchant(fs).affixes.every(function (a) { return global.FA_typeFits(a.type, 'armor') })) helmBad++
}
check('Chaos Tomes on a helmet only roll armor effects (' + helmSeeds + ' rolls, ' + helmBad + ' wrong)', helmBad == 0)

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
function makeTank(mb, fluid) { return { mb: mb, fluid: fluid || 'kubejs:liquid_essence', drained: 0,
  drain: function (want, action) { var n = want.fluid == this.fluid ? Math.min(want.amount, this.mb) : 0; if (action == 'exec') { this.mb -= n; this.drained += n } return { getAmount: function () { return n } } } } }
function capOf(h) { return { resolve: function () { return { orElse: function () { return h } } } } }
var altarGear = gear('Altar Sword'); faSetLevel(altarGear, 'sharpness', 5); faSetLevel(altarGear, 'protection', 50)
var depot = makeDepot(altarGear), tank = makeTank(100000)
var altarPos = new FakePos(10, 64, 10), blocks = { '10,64,10': 'kubejs:ascension_altar' }
var world2 = {
  dimension: 'minecraft:overworld',
  isLoaded: function () { return true },
  getBlock: function (p2) { return { id: blocks[p2.x + ',' + p2.y + ',' + p2.z] || 'minecraft:air' } },
  getBlockEntity: function (p2) { return p2.y == 65 ? { getCapability: function () { return capOf(depot) } } : p2.y == 63 ? { getCapability: function () { return capOf(tank) } } : null },
  addFreshEntity: function () {}, getServer: function () { return server }
}
server.getTickCount = function () { return 40 }
server.getAllLevels = function () { return [world2] }
server.getPlayerList = function () { return { getPlayers: function () { var a = []; a.size = function () { return 0 }; return a }, getPlayerCount: function () { return 0 } } }
var placer = makePlayer(server, 'placer')
handlers.placed['kubejs:ascension_altar']({ server: server, level: world2, block: { pos: altarPos }, player: placer, cancel: function () {} })
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

print('\nfusions')
var kb = global.FA.enchants.keen_bunker
check('Keen Bunker fuses Ascended Sharpness + Blast Ward', kb && kb.type == 'fusion' && kb.parts.join() == 'sharpness,blast_ward')
check('Fusion tomes are Tome A + Tome B + an Ascension Core, heated', byId('tome_keen_bunker').json.ingredients.map(function (i) { return i.item }).join() ==
  'kubejs:tome_sharpness,kubejs:tome_blast_ward,kubejs:ascension_core' && byId('tome_keen_bunker').json.heatRequirement == 'heated')
var fuser = makePlayer(server, 'fuser'), fuseSword = gear('Fused'); faSetLevel(fuseSword, 'keen_bunker', 100); fuser.main = fuseSword
check('a fusion does each parent at 60%: Sharpness part at level 100 = x7 damage', near(hit(fuser, makeMob(), 10), 10 * (1 + 0.1 * 0.6 * 100)), hit(fuser, makeMob(), 10))
check('...and its Blast Ward part works too', near(hit(null, fuser, 10, { tags: ['explosion'] }), 10 / (1 + 0.1 * 0.6 * 100)))
check('Fusions cost 2x Essence per level', global.FA_cost(100, 'keen_bunker') == Math.ceil(0.05 * 101 * 2))
var frosty2 = Object.keys(global.FA.enchants).filter(function (id) { var e = global.FA.enchants[id]; return e.type == 'fusion' && e.parts[0] == 'frostbite' })[0]
check('scaled-down effects step up slower (Frostbite every ' + global.FA_fusionPart(frosty2, 0).ampEvery + ' levels instead of 5)', global.FA_fusionPart(frosty2, 0).ampEvery == 9)
check('every fusion describes both halves', fusionIds.every(function (id) { var d = global.FA_describe(id, 10); return d.indexOf(' + ') > 0 && !/undefined|NaN/.test(d) }))
var badFuse = JSON.parse(JSON.stringify(global.FA))
badFuse.enchants.bad1 = { name: 'Bad One', type: 'fusion', parts: ['sharpness', 'wings'] }
badFuse.enchants.bad2 = { name: 'Bad Two', type: 'fusion', parts: ['sharpness', 'sharpness'] }
badFuse.enchants.bad3 = { name: 'Bad Three', type: 'fusion', parts: ['sharpness', 'swiftness'] }
global.FA_validate(badFuse)
check('fusions of Mythics, of the same enchant twice, or of attribute enchants are refused', !badFuse.enchants.bad1 && !badFuse.enchants.bad2 && !badFuse.enchants.bad3)
var fusedTip = { arr: ['Fused'], add: function (i, t) { this.arr.splice(i, 0, t.s) } }
advanced(fuseSword, false, fusedTip)
print('  sample: ' + fusedTip.arr[1])

print('\nultimates')
var serverTick = handlers.forge['ServerTickEvent']
function ticks(n) { for (var i = 0; i < n; i++) serverTick({ phase: 'END', getServer: function () { return server } }) }
// A small arena: the hero, a target 4 blocks away and three more enemies around it
function arena(ultId, lv) {
  var hero2 = makePlayer(server, 'ult_' + ultId), blade2 = gear('Ult Blade')
  faSetLevel(blade2, ultId, lv || 1); hero2.main = blade2
  hero2.x = 100; hero2.y = 70; hero2.z = 100; hero2.yaw = 33; hero2.pitch = -5; hero2.sneaking = true
  var target3 = makeMob(); target3.x = 104; target3.y = 70; target3.z = 100; target3.hp = target3.maxHp = 1e9
  var others = [makeMob(), makeMob(), makeMob()]
  others.forEach(function (m, i) { m.x = 104 + i; m.y = 70; m.z = 101; m.hp = m.maxHp = 1e9 })
  var all = [target3].concat(others)
  target3.nearby = all; hero2.nearby = all; others.forEach(function (m) { m.nearby = all })
  return { hero: hero2, target: target3, others: others }
}
faWarned = {}
var moveReports = []
ultIds.forEach(function (ultId) {
  var A = arena(ultId, 5)
  var dealt = hit(A.hero, A.target, 10)   // the triggering hit
  var started = !!faCombos[A.hero.stringUuid]
  ticks(200)
  var dmg = A.target.hurtBy.length + A.others.reduce(function (n, m) { return n + m.hurtBy.length }, 0)
  var home = A.hero.x == 100 && A.hero.y == 70 && A.hero.z == 100 && A.hero.yaw == 33
  moveReports.push(global.FA.enchants[ultId].name + ': ' + dmg + ' hits' + (A.hero.teleports ? ', ' + A.hero.teleports + ' teleports' : ''))
  check(global.FA.enchants[ultId].name + ' plays out, lands hits and leaves you back home', started && dmg > 0 && home && !faCombos[A.hero.stringUuid],
    'started ' + started + ' hits ' + dmg + ' at ' + [A.hero.x, A.hero.y, A.hero.z, A.hero.yaw].join(','))
})
print('  ' + moveReports.join(' | '))
check('no combo step ever crashed', Object.keys(faWarned).length == 0, Object.keys(faWarned).join(','))
check('the scheduler is empty again afterwards', faQueue.length == 0, faQueue.length)

var W = arena('thunder_waltz', 1)
hit(W.hero, W.target, 10)
ticks(10)
check("Thunder God's Waltz circles the target facing it", W.hero.teleports >= 2 && Math.abs(Math.sqrt(Math.pow(W.hero.x - 104, 2) + Math.pow(W.hero.z - 100, 2)) - 3) < 0.01)
var face = faLook(W.hero.x, W.hero.y + 1.6, W.hero.z, 104, 71, 100)
check('...always looking straight at it', Math.abs(W.hero.yaw - face.yaw) < 0.01)
var duringHit = hit(makeMob(), W.hero, 50)
var attackEv = { c: false, getSource: function () { return src(makeMob()) }, getEntity: function () { return W.hero }, isCanceled: function () { return this.c }, setCanceled: function (v) { this.c = v } }
handlers.forge['LivingAttackEvent'](attackEv)
check('you are untouchable mid-combo', attackEv.c)
var killEv = { c: false, getSource: function () { return src(null, { bypass: true }) }, getEntity: function () { return W.hero }, isCanceled: function () { return this.c }, setCanceled: function (v) { this.c = v } }
handlers.forge['LivingAttackEvent'](killEv)
check('...except /kill and the void', !killEv.c)
var fallEv = { mult: 1, getEntity: function () { return W.hero }, getDistance: function () { return 20 }, setDamageMultiplier: function (m) { this.mult = m } }
handlers.forge['LivingFallEvent'](fallEv)
check('...and take no fall damage from the combo', fallEv.mult == 0)
W.target.hp = 0; W.target.isAlive = function () { return false }
ticks(200)
check('if the target dies mid-combo, it still finishes and brings you home', W.hero.x == 100 && W.hero.z == 100 && !faCombos[W.hero.stringUuid])

var L2 = arena('thunder_waltz', 1)
hit(L2.hero, L2.target, 10); ticks(6)
handlers.forge['PlayerLoggedOutEvent']({ getEntity: function () { return L2.hero } })
check('logging out mid-combo puts you back first', L2.hero.x == 100 && L2.hero.z == 100 && !faCombos[L2.hero.stringUuid])
ticks(200)

var C2 = arena('blade_storm', 1)
hit(C2.hero, C2.target, 10); ticks(200)
var before2 = C2.target.hurtBy.length
hit(C2.hero, C2.target, 10)
check('Ultimates have a cooldown', !faCombos[C2.hero.stringUuid])
var N2 = arena('blade_storm', 1); N2.hero.sneaking = false
hit(N2.hero, N2.target, 10)
check('only a sneaking hit unleashes it', !faCombos[N2.hero.stringUuid])
var P2 = arena('blade_storm', 1)
hit(P2.hero, P2.target, 10, { direct: makeMob(), tags: ['projectile'] })
check('arrows never trigger it (melee only)', !faCombos[P2.hero.stringUuid])
check('Ultimate power: level 5 = 2x, cooldown drops a second per level (min 5s)', near(global.FA_ultPower({ power: 1, perLevel: 0.25 }, 5), 2) &&
  global.FA_ultCooldown({ cooldown: 40 }, 100) == 5)
check('Ultimate tomes need 2 Ascended Souls + 4 Mythic Fragments', byId('tome_thunder_waltz').json.ingredients.filter(function (i) { return i.item == 'kubejs:ascended_soul' }).length == 2 &&
  byId('tome_thunder_waltz').json.heatRequirement == 'superheated')
check('Ascended bosses drop Ascended Souls', global.FA.bosses.drops.some(function (d) { return d[0] == 'kubejs:ascended_soul' }))
check('Ultimates cost 10x Essence per level', global.FA_cost(10, 'eclipse') == Math.ceil(0.05 * 11 * 10))

print('\nloot')
var o0 = global.FA_lootOdds(0), o80 = global.FA_lootOdds(80)
check('loot odds add up to 100% at every depth', near(o0.reduce(function (a, b) { return a + b }, 0), 1) && near(o80.reduce(function (a, b) { return a + b }, 0), 1))
check('deeper zones shift the odds toward the top tiers', o80[4] + o80[5] > 4 * (o0[4] + o0[5]) && o80[0] < o0[0], (o0[4] + o0[5]).toFixed(4) + ' -> ' + (o80[4] + o80[5]).toFixed(4))
print('  odds at danger 0: ' + o0.map(function (x, i) { return global.FA.loot.tiers[i].name + ' ' + (x * 100).toFixed(1) + '%' }).join(', '))
print('  odds at danger 80: ' + o80.map(function (x, i) { return global.FA.loot.tiers[i].name + ' ' + (x * 100).toFixed(1) + '%' }).join(', '))
check('a kill drops a loot roll more often deeper, on the curve: 12% at 1,000 blocks, ' + (global.FA_lootChance(5) * 100).toFixed(0) + '% at 2,000, capped at 75%',
  near(global.FA_lootChance(1), 0.12) && near(global.FA_lootChance(5), 0.1 + 0.02 * Math.pow(2, 2.807)) && near(global.FA_lootChance(80), 0.75))
check('loot follows the mob curve: x1.5 at 1,000 blocks, x4.5 at 2,000, bigger stacks and better tiers deeper',
  near(global.FA_dangerMult(1).loot, 1.5) && Math.abs(global.FA_dangerMult(5).loot - 4.5) < 0.05 && global.FA_lootBulk(17) > 10 * (global.FA_lootBulk(1) - 1) &&
  global.FA_lootOdds(17)[5] > 30 * global.FA_lootOdds(1)[5], global.FA_dangerMult(5).loot.toFixed(2) + ', mythic ' + (global.FA_lootOdds(1)[5] * 100).toFixed(2) + '% -> ' + (global.FA_lootOdds(17)[5] * 100).toFixed(2) + '%')
// Meteor Shower: anything from the pack can fall, better things rarer, better odds deeper
var mIsLoaded = Platform.isLoaded
Platform.isLoaded = function (m) { return m == 'factoryascension' || m == 'sophisticatedbackpacks' || mIsLoaded(m) }
var mPool = global.FA_meteorPool()
Platform.isLoaded = mIsLoaded
var mAll = [].concat.apply([], mPool).filter(function (x) { return typeof x == 'string' })
var mMissing = created.concat(createdBlocks).map(function (x) { return 'kubejs:' + x }).concat(modItems.filter(function (id) { return !/_spawn_egg$/.test(id) }))
  .filter(function (id) { return id != 'kubejs:incomplete_ascension_core' && id != 'factoryascension:duffel_bag' && id != 'factoryascension:dog_tag' && mAll.indexOf(id) < 0 })
check('meteors can drop every item and block the pack makes (' + mAll.length + ', plus Relics)', mMissing.length == 0, mMissing.slice(0, 8).join(', '))
var mSeed = 7, mRand = function () { mSeed = (mSeed * 16807) % 2147483647; return mSeed / 2147483647 }
var mean = function (danger) { var t = 0; for (var i = 0; i < 3000; i++) t += global.FA_meteorRoll(mRand, danger, mPool).tier; return t / 3000 }
var mHome = mean(0), mFar = mean(17), mTop = [0, 0, 0, 0, 0, 0]
for (var mi = 0; mi < 3000; mi++) mTop[global.FA_meteorRoll(mRand, 0, mPool).tier]++
check('meteor loot: the better the tier the rarer, and better farther out (mean tier ' + mHome.toFixed(2) + ' at home, ' + mFar.toFixed(2) + ' at 5,000 blocks)',
  mTop[0] > mTop[1] && mTop[1] > mTop[2] && mTop[2] > mTop[3] && mTop[3] >= mTop[4] && mFar > mHome + 1, mTop.join(' / '))
var known = {}; created.concat(createdBlocks).forEach(function (id) { known['kubejs:' + id] = true })
var badLoot = [], tiersSeen = [0, 0, 0, 0, 0, 0], relicCount = 0
for (var lr = 0; lr < 3000; lr++) {
  var lroll = global.FA_rollLoot(Math.random, lr % 100, lr % 3)
  tiersSeen[lroll.tier]++
  var ld = lroll.drop
  if (ld.kind == 'relic') { relicCount++; continue }
  if (!(ld.count >= 1) || (ld.id.indexOf('kubejs:') === 0 && !known[ld.id])) badLoot.push(JSON.stringify(ld))
}
check('3,000 loot rolls only ever drop items that exist, in sane amounts', badLoot.length == 0, badLoot.slice(0, 3).join(' | '))
print('  3,000 rolls (danger 0-99, mixed elites/bosses): ' + tiersSeen.map(function (n, i) { return global.FA.loot.tiers[i].name + ' ' + n }).join(', ') + '; relics ' + relicCount)
var badRelics = []
for (var rr = 0; rr < 500; rr++) {
  var q = rr % 2 ? 'mythic' : 'legendary', rel = global.FA_makeRelic(Math.random, q, rr % 100)
  var eids = Object.keys(rel.enchants)
  if (!/^minecraft:(diamond|netherite)_/.test(rel.item)) badRelics.push('item ' + rel.item)
  if (eids.length < (q == 'mythic' ? 5 : 2)) badRelics.push('few enchants ' + eids.length)
  eids.forEach(function (id) {
    var e2 = E[id]
    if (!e2 || e2.ultimate || !(rel.enchants[id] >= 1) || (e2.maxLevel && rel.enchants[id] > e2.maxLevel)) badRelics.push(id + '=' + rel.enchants[id])
  })
  if (q == 'mythic' && !eids.some(function (id) { return E[id].mythic })) badRelics.push('mythic relic without a Mythic enchant')
  if (!/^[A-Z][a-z]+ [A-Z][a-z]+ of ([A-Z]|the [A-Z])/.test(rel.name)) badRelics.push('name ' + rel.name)
}
check('500 Relics: real gear, suitable enchants within their caps, Mythic ones carry a Mythic enchant', badRelics.length == 0, badRelics.slice(0, 4).join(' | '))
var sampleRelic = global.FA_makeRelic(Math.random, 'mythic', 60)
print('  sample Mythic Relic at danger 60: ' + sampleRelic.name + ' (' + sampleRelic.item + '): ' + Object.keys(sampleRelic.enchants).map(function (id) { return E[id].name + ' ' + sampleRelic.enchants[id] }).join(', ') +
  (sampleRelic.gems.length ? '; gems ' + sampleRelic.gems.map(function (g) { return g[0] }).join(',') : '') + (sampleRelic.stars ? '; 1 star' : ''))
Item.of = function (id, n) { var st = stack(n || 1); st.id = id; st.name = id; return st }
var relicItem = faRelicStack({ item: 'minecraft:netherite_sword', name: 'Test Edge of Ruin', quality: 'mythic', color: 'red', kind: 'weapon',
  enchants: { sharpness: 400, overkill: 50 }, gems: [['bloodstone', 3]], rolled: [[1337, 20]], stars: 1 })
var relicHero = makePlayer(server, 'relic'); relicHero.main = relicItem
var rlv = faLevels(relicHero)
check('a Relic reads like any gear: its stars, gems and rolls all count', rlv.sharpness == Math.floor(400 * 1.25) + Math.floor(75 * 1.25) && rlv.overkill == Math.floor(50 * 1.25) &&
  Object.keys(rlv).some(function (k) { return /^r1337_/.test(k) }), JSON.stringify(rlv))
check('Relics carry their name and a Relic tag', JSON.parse(relicItem.tag.d.display.d.Name).text == 'Test Edge of Ruin' && relicItem.tag.d.FactoryAscension.d.relic == 2)
var relicTip = { arr: ['Edge'], add: function (i, t) { this.arr.splice(i, 0, t.s) } }
advanced(relicItem, false, relicTip)
check('Relic tooltips say what they are', /MYTHIC RELIC/.test(relicTip.arr[1]), relicTip.arr[1])
Math.random = function () { return 0.01 }
var deep = makeMob(); deep.pdata.putInt('fa_danger', 40)
var dl = dropEvent(plainHero, deep, []); dropsHook(dl)
check('a lucky danger-zone kill drops loot', dl.list.arr.length >= 1)
var bossLoot = makeMob(); bossLoot.pdata.putInt('fa_danger', 40); bossLoot.pdata.putBoolean('fa_boss', true)
var dbl = dropEvent(plainHero, bossLoot, []); dropsHook(dbl)
check('Ascended bosses always roll loot, four times, never more than 12 stacks of it', dbl.list.arr.length >= 4 && dbl.list.arr.length <= 12 + 6, dbl.list.arr.length)
var dnone = dropEvent(makeMob(), deep, []); dropsHook(dnone)
check('no loot unless a player made the kill', dnone.list.arr.length == 0)
var dsafe2 = dropEvent(plainHero, makeMob(), []); dropsHook(dsafe2)
check('no loot from safe-zone mobs', dsafe2.list.arr.length == 0)
Math.random = realRandom

print('\nhordes')
var hordeBase = makeMob(); hordeBase.x = 30000; hordeBase.nearby = []
var addedBefore = bolts.length
Math.random = function () { return 0.4 }
spawn(hordeBase, 30000)
Math.random = realRandom
var hdanger = hordeBase.pdata.getInt('fa_danger')
ticks(2)
var extras = bolts.slice(addedBefore).filter(function (e) { return e.pdata && e.pdata.getBoolean('fa_horde') })
var expectExtras = Math.min(global.FA.horde.maxExtra, Math.floor(global.FA.horde.extraPerLevel * hdanger) + (0.4 < (global.FA.horde.extraPerLevel * hdanger) % 1 ? 1 : 0))
check('danger ' + hdanger + ' spawns come with a horde of ' + expectExtras, extras.length == expectExtras, extras.length + ' vs ' + expectExtras)
var chainBefore = bolts.length
extras.forEach(function (e) { join({ loadedFromDisk: function () { return false }, getEntity: function () { return e }, getLevel: function () { return e.level } }) })
ticks(2)
check('horde mobs never bring hordes of their own', bolts.length == chainBefore)
var crowded = makeMob(); crowded.x = 30000
var crowd = []; for (var cr = 0; cr < global.FA.horde.nearbyCap; cr++) crowd.push(makeMob())
crowded.nearby = crowd
var crowdBefore = bolts.length
spawn(crowded, 30000); ticks(2)
check('no extra mobs while the area is already crowded', bolts.length == crowdBefore)
// Deep out, at the edge of the loaded world: no horde (extras would make the game generate new chunks on the spot)
var edgeMob = makeMob(); edgeMob.x = 30000; edgeMob.unloaded = true; var edgeBefore = bolts.length
spawn(edgeMob, 30000); ticks(2)
check('no hordes in unloaded chunks (no chunk-generation chain reaction deep in the danger zones)', bolts.length == edgeBefore)
var rider = makeMob(); rider.x = 30000; rider.riding = true; var riderBefore = bolts.length
spawn(rider, 30000); ticks(2)
check('jockey riders never bring hordes', bolts.length == riderBefore)
var nearSpawn = makeMob(); var nsBefore = bolts.length
spawn(nearSpawn, 100); ticks(2)
check('no hordes in the safe zone', bolts.length == nsBefore)

print('\njei')
check('JEI hides only the half-made assembly item', jei.hidden.join() == 'kubejs:incomplete_ascension_core')
var needInfo = ['kubejs:essence_condenser', 'kubejs:ascension_altar', 'kubejs:condensed_essence_0', 'kubejs:mythic_fragment', 'kubejs:chaos_tome', 'kubejs:ascension_core', 'kubejs:gem_onyx_3', 'kubejs:tome_sharpness']
check('JEI explains everything that comes from a machine or the world', needInfo.every(function (id) { return jei.info[id] && jei.info[id].length > 0 }),
  needInfo.filter(function (id) { return !jei.info[id] }).join(','))
check('JEI pages have no broken text', Object.keys(jei.info).every(function (id) { return jei.info[id].every(function (l) { return typeof l == 'string' && !/undefined|NaN/.test(l) }) }))
check('every tome has a JEI page', Object.keys(E).every(function (id) { return !!jei.info['kubejs:tome_' + id] }))

print('\ncondensed essence')
var names = []
for (var ct = 0; ct <= condTiers; ct++) names.push(global.FA_condensedName(ct))
check('51 tiers, each with its own name ending in (xN)', names.length == 51 && names[0] == 'Condensed Essence Block' && /^Absolute Essence Block \(x50\)$/.test(names[50]) &&
  names.slice(1).every(function (n, i) { return n.indexOf('(x' + (i + 1) + ')') > 0 }) && names.filter(function (n, i) { return names.indexOf(n) != i }).length == 0, names[34])
print('  sample: ' + [names[1], names[10], names[34], names[39], names[50]].join(' | '))
check('one block = 1 bucket = 10 Essence, each tier = 9 of the last', global.FA_condensedValue(0) == 10 && global.FA_condensedValue(1) == 90 &&
  near(global.FA_condensedValue(50), 10 * Math.pow(9, 50)))
var condRecipesOk = true
for (var ct2 = 1; ct2 <= condTiers; ct2++) {
  var pack = byId('condense_' + ct2), unpack = recipes.filter(function (r) { return r.rid == 'kubejs:factory_ascension/uncondense_' + ct2 })[0]
  if (!pack || pack.json.ingredients.length != 9 || pack.json.ingredients[0].item != global.FA_condensedId(ct2 - 1) || !unpack) condRecipesOk = false
}
check('every tier compacts 9 -> 1 and unpacks 1 -> 9, so nothing is gained either way', condRecipesOk)
check('melting a block gives exactly its bucket back', byId('melt_condensed_essence').json.results[0].amount == 1000)
check('Condensed Essence is worth exactly the fluid it was made from', global.FA_condensedValue(0) == global.FA.condensed.mbPerBlock / global.FA.altar.mbPerEssence)
var hoarder = makePlayer(server, 'hoarder'); hoarder.sneaking = true
handlers.rightClicked[global.FA_condensedId(50)]({ player: hoarder, item: stack(64), cancel: function () {} })
check('absorbing a stack of 64 (x50) blocks: ~3.3e49 Essence, stored safely', near(faGetEssence(hoarder), 64 * global.FA_condensedValue(50)) && isFinite(faGetEssence(hoarder)), faGetEssence(hoarder))
var richPick = gear('Rich Pick'); faSetLevel(richPick, 'sharpness', 1); hoarder.main = richPick
faUpgrade(hoarder, 'sharpness', 100000)
check('...and it spends like any Essence', faGetLevel(richPick, 'sharpness') == 100001)

print('\ncondenser')
function makeChest(slots) { var arr = []; for (var i = 0; i < slots; i++) arr.push(stack(0))
  return { slots: arr, getSlots: function () { return arr.length }, getStackInSlot: function (i) { return arr[i] },
    insertItem: function (i, it, sim) {
      var cur = arr[i]
      if (!cur.isEmpty() && cur.id != it.id) return it
      var room = 64 - (cur.isEmpty() ? 0 : cur.count), n = Math.min(room, it.count)
      if (!sim && n > 0) { if (cur.isEmpty()) { arr[i] = it.copyWithCount(n) } else cur.count += n }
      return n == it.count ? stack(0) : it.copyWithCount(it.count - n)
    } } }
var chest = makeChest(2), ctank = makeTank(70500)
var condPos = new FakePos(20, 64, 20), cblocks = { '20,64,20': 'kubejs:essence_condenser' }
var world3 = {
  dimension: 'minecraft:overworld',
  isLoaded: function () { return true },
  getBlock: function (p2) { return { id: cblocks[p2.x + ',' + p2.y + ',' + p2.z] || 'minecraft:air' } },
  getBlockEntity: function (p2) { return p2.y == 65 ? { getCapability: function () { return capOf(chest) } } : p2.y == 63 ? { getCapability: function () { return capOf(ctank) } } : null },
  addFreshEntity: function (e) { condDrops.push(e) }, getServer: function () { return server }
}
var condDrops = []
server.getAllLevels = function () { return [world3] }
handlers.placed['kubejs:essence_condenser']({ server: server, level: world3, block: { pos: condPos }, player: placer, cancel: function () {} })
handlers.tick({ server: server })
check('the condenser packs 64 buckets a second into blocks in the chest above', chest.slots[0].count == 64 && chest.slots[0].id == global.FA_condensedId(0) && ctank.drained == 64000, chest.slots[0].count + ' / ' + ctank.drained)
handlers.tick({ server: server })
check('it never drains part of a block (6,500 mB left -> 6 blocks, 500 mB stays)', chest.slots[1].count == 6 && ctank.mb == 500, chest.slots[1].count + ' / ' + ctank.mb)
var fullChest = makeChest(1); fullChest.slots[0] = stack(64); fullChest.slots[0].id = 'minecraft:stone'; chest = fullChest; ctank = makeTank(5000)
handlers.tick({ server: server })
check('a full chest means no fluid is drained', ctank.drained == 0)
chest = makeChest(1); ctank = makeTank(5000, 'water')
handlers.tick({ server: server })
check('a tank of the wrong fluid is ignored', ctank.drained == 0 && chest.slots[0].isEmpty())
check('nothing was ever dropped on the ground', condDrops.length == 0)
server.getAllLevels = function () { return [world2] }

print('\nworld events')
var evServer = makeServer()
evServer.overworld = function () { return { isNight: function () { return false } } }
check('Blood Moon refuses to start in daytime', global.FA_startEvent(evServer, 'bloodMoon') === false && !global.FA_STATE.event)
check('Golden Hour starts and is announced', global.FA_startEvent(evServer, 'goldenHour') && global.FA_STATE.event.type == 'goldenHour' && evServer.commands.some(function (c) { return /GOLDEN HOUR/.test(c) }))
var gh = dropEvent(plainHero, makeMob(), [stack(4)]); dropsHook(gh)
check('Golden Hour doubles loot', totalCount(gh) == 8, totalCount(gh))
global.FA_endEvent(evServer)
evServer.overworld = function () { return { isNight: function () { return true } } }
global.FA_startEvent(evServer, 'bloodMoon')
Math.random = function () { return 0.99 }
var bmMob = spawn(makeMob(), 6000)
check('Blood Moon: everything spawns +10 danger', bmMob.pdata.getInt('fa_danger') == 31, bmMob.pdata.getInt('fa_danger'))
Math.random = realRandom
global.FA_endEvent(evServer)
check('events end cleanly', !global.FA_STATE.event)

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
var bossServer = makeServer(); bossServer.getPlayerList = function () { return { getPlayers: function () { var a = []; a.size = function () { return 0 }; return a }, getPlayerCount: function () { return 0 } } }
faLastSecond = 0; tickBoss({ phase: 'END', getServer: function () { return bossServer } })
var tracked = faBosses[bossMob.stringUuid]
check('a boss bar shows to nearby players', tracked && tracked.bar.players.indexOf(fan) >= 0)
check('the boss slams nearby players away', fan.motion && fan.motion[0] > 0)
bossMob.hp = 12; spawnedMinions = []
faLastSecond = 0; tickBoss({ phase: 'END', getServer: function () { return bossServer } })
check('at 66% health it summons minions', spawnedMinions.length == global.FA.bosses.minionsPerPhase && spawnedMinions.every(function (m) { return m.pdata.getBoolean('fa_minion') }))
var bossDrops = dropEvent(plainHero, bossMob, []); dropsHook(bossDrops)
check('killing it drops Chaos Tomes, fragments and cores', ['kubejs:chaos_tome', 'kubejs:mythic_fragment', 'kubejs:dense_ascension_core'].every(function (id) { return bossDrops.list.arr.some(function (ie) { return ie.s.id == id }) }))
bossMob.removed = true
faLastSecond = 0; tickBoss({ phase: 'END', getServer: function () { return bossServer } })
check('dead bosses take their bar with them', !faBosses[bossMob.stringUuid] && tracked.bar.players.length == 0)

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
    // A battery recharge takes one battery and gives back one (charged): not a duplication
    var recharge = /recharge_/.test(r.rid) && ins.filter(function (i) { return i == out }).length == 1 && (o.count || 1) == 1
    if (ins.indexOf(out) >= 0 && out != 'kubejs:incomplete_ascension_core' && !recharge) outputs[r.rid] = out
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

// ---------------- Rock Hammers ----------------
print('\nrock hammers')
var HM = global.FA.hammer
var allYes = function () { return 0 }
var smashed = global.FA_smash(allYes, 'minecraft:cobblestone', 'kubejs:rock_hammer')
check('a lucky smash of cobblestone can give every drop on its list', smashed.length == HM.rocks['minecraft:cobblestone'].length, smashed.map(function (r) { return r[0] }).join(','))
var luckyRoll = function () { return 0.04 }
check('the Ascendite hammer is luckier (a 4% roll beats Raw Ascendite 3% only with x1.6 luck)',
  !global.FA_smash(luckyRoll, 'minecraft:cobblestone', 'kubejs:rock_hammer').some(function (r) { return r[0] == 'kubejs:raw_ascendite' }) &&
  global.FA_smash(luckyRoll, 'minecraft:cobblestone', 'kubejs:ascendite_rock_hammer').some(function (r) { return r[0] == 'kubejs:raw_ascendite' }))
check('things that aren\'t rocks smash into nothing', global.FA_smash(allYes, 'minecraft:dirt', 'kubejs:rock_hammer').length == 0)
var miner2 = makePlayer(server, 'smasher')
var hammerStack = stack(1, 1); hammerStack.id = 'kubejs:rock_hammer'; hammerStack.maxDamage = 250
var rocks = stack(20); rocks.id = 'minecraft:cobblestone'
miner2.main = hammerStack; miner2.off = rocks
Math.random = function () { return 0 }
var hamCancel = false
handlers.rightClicked['kubejs:rock_hammer']({ player: miner2, item: hammerStack, hand: 'MAIN_HAND', cancel: function () { hamCancel = true } })
check('hand smash: one cobblestone in, its drops out, 1 durability used, short cooldown', rocks.count == 19 && miner2.given.length == HM.rocks['minecraft:cobblestone'].length &&
  hammerStack.damage == 1 && miner2.cooldowns['kubejs:rock_hammer'] == HM.tools['kubejs:rock_hammer'].cooldown && hamCancel, rocks.count + ' / ' + miner2.given.length + ' / ' + hammerStack.damage)
miner2.cooldowns = {}; miner2.sneaking = true
handlers.rightClicked['kubejs:rock_hammer']({ player: miner2, item: hammerStack, hand: 'MAIN_HAND', cancel: function () {} })
check('sneaking smashes ' + HM.sneakCount + ' at once', rocks.count == 19 - HM.sneakCount && hammerStack.damage == 1 + HM.sneakCount, rocks.count)
miner2.sneaking = false; miner2.cooldowns = {}
hammerStack.damage = 249
handlers.rightClicked['kubejs:rock_hammer']({ player: miner2, item: hammerStack, hand: 'MAIN_HAND', cancel: function () {} })
check('a worn-out hammer breaks', hammerStack.count == 0)
var popped = [], blockSet = null
var rockBlock = { id: 'minecraft:cobbled_deepslate', x: 1, y: 2, z: 3, set: function (b) { blockSet = b }, popItem: function (it) { popped.push(it.id) } }
var hammer2 = stack(1, 1); hammer2.id = 'kubejs:ascendite_rock_hammer'; hammer2.maxDamage = 1200
var bc = false
handlers.blockClick({ player: miner2, item: hammer2, block: rockBlock, level: miner2.level, cancel: function () { bc = true } })
check('block smash: the rock turns to air and pops its drops in place', blockSet == 'minecraft:air' && popped.length == HM.rocks['minecraft:cobbled_deepslate'].length && bc && hammer2.damage == 1, popped.join(','))
popped = []; blockSet = null
handlers.blockClick({ player: miner2, item: hammer2, block: { id: 'minecraft:stone', set: function (b) { blockSet = b }, popItem: function () {} }, level: miner2.level, cancel: function () {} })
check('non-rock blocks are left alone', blockSet === null)
Math.random = realRandom

// ---------------- /home ----------------
print('\nhome')
server.getTickCount = function () { return gameTime }   // an earlier test froze it at 40
var homer = makePlayer(server, 'homer')
homer.x = 120; homer.y = 70; homer.z = -40
server.commands = []
check('/home before /sethome tells you to set one', faGoHome(homer) == 0)
faSetHome(homer)
homer.x = 5000
gameTime = 100000
check('/home teleports you back (any dimension, via tp)', faGoHome(homer) == 1 && server.commands.some(function (c) { return /execute in minecraft:overworld run tp homer 120\.00 70\.00 -40\.00/.test(c) }),
  server.commands.join(' | '))
gameTime += 20
check('...then it cools down', faGoHome(homer) == 0)
gameTime += global.FA.home.cooldownSeconds * 20
var foe = makeMob()
hit(foe, homer, 2)
check('getting hit puts you in combat: no /home for ' + global.FA.home.combatSeconds + 's', global.FA_combatLeft(homer) == global.FA.home.combatSeconds && faGoHome(homer) == 0)
gameTime += global.FA.home.combatSeconds * 20 + 1
homer.messages = []
var backHome = faGoHome(homer)
check('...and it works again once combat is over', backHome == 1, homer.messages.map(function (t) { return t.s }).join(' | ') + ' combat=' + global.FA_combatLeft(homer))
homer.pdata.putInt('fa_combat', gameTime + 999999)
check('a combat mark from before a server restart doesn\'t count', global.FA_combatLeft(homer) == 0)

// ---------------- Enchantment card packs ----------------
print('\ncard packs')
var PR = global.FA.packs.rarities, pr = new java.util.Random(11), tallyR = PR.map(function () { return 0 }), lastOk = true
for (var pk = 0; pk < 4000; pk++) {
  var pack = global.FA_openPack(function () { return pr.nextDouble() })
  pack.forEach(function (c) { tallyR[c.rarity]++ })
  if (pack[pack.length - 1].rarity < 2) lastOk = false
}
check('a pack has ' + global.FA.packs.cards + ' cards and the last is always Rare or better', lastOk)
check('rarer cards are rarer (4,000 packs)', tallyR[0] > tallyR[1] && tallyR[1] > tallyR[3] && tallyR[3] > tallyR[4] && tallyR[4] > tallyR[5] && tallyR[5] > 0, tallyR.join(' / '))
var secret = global.FA_rollCard(function () { return 0.9999 }, 0)
check('Secret Rare cards are Ultimate tomes', !!global.FA.enchants[secret.id].ultimate && secret.rarity == PR.length - 1, secret.id)
var opener = makePlayer(server, 'opener'), packSt = stack(2); packSt.id = 'kubejs:enchantment_pack'
handlers.rightClicked['kubejs:enchantment_pack']({ player: opener, item: packSt, cancel: function () {} })
check('opening a pack gives 5 tomes (leveled to the card) and uses one pack', packSt.count == 1 && opener.given.length == 5 &&
  opener.given.every(function (t) { return /^kubejs:tome_/.test(t.id) }), opener.given.map(function (t) { return t.id }).join(','))
var boxSt = stack(1); boxSt.id = 'kubejs:booster_box'; opener.given = []
handlers.rightClicked['kubejs:booster_box']({ player: opener, item: boxSt, cancel: function () {} })
check('a booster box holds ' + global.FA.packs.boxPacks + ' packs', boxSt.count == 0 && opener.given[0].id == 'kubejs:enchantment_pack' && opener.given[0].count == global.FA.packs.boxPacks)
var chestMods = {}
handlers.chestLoot({ modify: function (id, fn) { var pools = []; fn({ addPool: function (f) { var pool = { items: [], empty: 0, addItem: function (i) { var e = { id: i, weight: function (w) { e.w = w; return e }, addFunction: function (f) { e.fn = f; return e } }; pool.items.push(e); return e }, addEmpty: function (w) { pool.empty = w } }; f(pool); pools.push(pool) } }); chestMods[id] = pools } })
// KubeJS's chest event adds the chests/ folder itself (found on a real server: full ids matched nothing)
var dung = chestMods['minecraft:simple_dungeon'], endc = chestMods['minecraft:end_city_treasure']
check('dungeon chests get a 30% pack roll and gear cache rolls; end cities a box roll too', dung.length == 4 && dung[0].items[0].w == 300 && dung[0].empty == 700 &&
  dung[1].items[0].id == 'kubejs:gear_cache' && endc.length == 5 && endc[1].items[0].id == 'kubejs:booster_box')
var jp = dung[3], packJ = jp.items.filter(function (e) { return e.id == 'kubejs:enchantment_pack' })[0]
var jpTotal = jp.items.reduce(function (a, e) { return a + e.w }, 0)
check('jackpot: a ' + (global.FA.jackpot.chestChance * 100) + '% roll per chest for a huge stack (10-40 packs, caches, boxes, cores)', near(jpTotal / (jpTotal + jp.empty), global.FA.jackpot.chestChance) &&
  packJ.fn.function == 'minecraft:set_count' && packJ.fn.count.min == 10 && packJ.fn.count.max == 40)
// Lucky levels: most rolls normal, a few huge, the cap reachable only by insane luck
var lr = new java.util.Random(9), lm = [], bigs = 0
for (var li = 0; li < 200000; li++) { var m = global.FA_luckyMult(function () { return lr.nextDouble() }); lm.push(m); if (m >= 100) bigs++ }
lm.sort(function (a, b) { return a - b })
check('lucky levels: half of rolls x1.55 or less, about 1 in 1,600 are x100+ (' + bigs + ' in 200,000), never past the cap', lm[100000] < 1.6 && bigs > 80 && bigs < 180 &&
  lm[lm.length - 1] <= global.FA.jackpot.levelCap, lm[100000].toFixed(2) + ' / ' + bigs)
check('even a level-1 card can roll into the thousands with insane luck', global.FA_rollCard(function () { return 0.9999999 }, 0).level > 1000)
check('village weaponsmiths get gear caches too (KubeJS-style id)', !!chestMods['minecraft:village/village_weaponsmith'])
// Sealed Gear Caches: the deeper you open one, the better the gear
var cq = function (danger) { var t = {}, g = new java.util.Random(3); for (var i = 0; i < 5000; i++) { var q = global.FA_cacheQuality(function () { return g.nextDouble() }, danger); t[q] = (t[q] || 0) + 1 } return t }
var safeQ = cq(0), deepQ = cq(40)
check('gear caches: mostly Rare/Epic at spawn, far more Legendary/Mythic deep in the danger zones', (safeQ.rare || 0) > 3000 && (deepQ.legendary || 0) + (deepQ.mythic || 0) > 10 * ((safeQ.legendary || 0) + (safeQ.mythic || 0) + 1) / 10,
  JSON.stringify(safeQ) + ' vs ' + JSON.stringify(deepQ))
var cacher = makePlayer(server, 'cacher'), cacheSt = stack(1); cacheSt.id = 'kubejs:gear_cache'
var opened = global.FA_openCacheFor(cacher, cacheSt)
check('opening a cache gives one piece of relic gear and uses the cache', cacheSt.count == 0 && cacher.given.length == 1 && /^minecraft:(iron|diamond|netherite)_/.test(cacher.given[0].id), cacher.given.map(function (g) { return g.id }).join(','))
var rareRelic = global.FA_makeRelic(Math.random, 'rare', 0)
check('Rare relics are iron gear with 1-2 enchants', /^minecraft:iron_/.test(rareRelic.item) && Object.keys(rareRelic.enchants).length >= 1 && Object.keys(rareRelic.enchants).length <= 2, rareRelic.item)

// ---------------- Artifacts ----------------
print('\nartifacts')
var ar = new java.util.Random(5), seen = {}, dupes = 0
for (var ai = 0; ai < 2000; ai++) {
  var art = global.FA_rollArtifact(function () { return ar.nextDouble() }, 'weapon')
  var key = art.name + '|' + art.title + '|' + art.quirks.map(function (q) { return q.id + q.power }).join(',')
  if (seen[key]) dupes++; seen[key] = 1
}
check('2,000 rolled Artifacts are all different', dupes == 0, dupes)
var armorArt = global.FA_rollArtifact(Math.random, 'armor')
check('Artifacts only get quirks that fit the gear (armor never gets Echo Strike)', armorArt.quirks.every(function (q) { var d = global.FA.artifacts.list.filter(function (x) { return x.id == q.id })[0]; return d.for == 'any' || d.for == 'armor' }))
var artServer = makeServer()
var blade2 = stack(1, 1, 'Netherite Sword'); blade2.id = 'minecraft:netherite_sword'; blade2.maxDamage = 2031
var made2 = global.FA_makeArtifact(blade2, function () { return 0.5 }, artServer)
check('making an Artifact names it and numbers it #1 of 1', made2 && made2.serial == 1 && /,/.test(JSON.parse(blade2.tag.getCompound('display').getString('Name')).text) && global.FA_isArtifact(blade2))
var blade3 = stack(1, 1); blade3.id = 'minecraft:iron_sword'; blade3.maxDamage = 250
check('the next one is #2', global.FA_makeArtifact(blade3, Math.random, artServer).serial == 2)
check('an Artifact can\'t be made twice', global.FA_makeArtifact(blade2, Math.random, artServer) === false)
// Quirks in combat
function artWith(id, power, isArmor) {
  var st = stack(1, 1); st.id = 'test:gear'; st.maxDamage = 500
  if (isArmor) st.getItem = function () { return new FakeArmorItem() }
  var tag = st.getOrCreateTag(), data = tag.getCompound('FactoryAscension'), a = new Compound(), qs = new Compound(), one = new Compound()
  one.putString('id', id); one.putDouble('power', power); qs.put('0', one); a.put('quirks', qs); a.putString('aura', '1 1 1')
  data.put('artifact', a); tag.put('FactoryAscension', data)
  return st
}
var wielder = makePlayer(server, 'wielder')
wielder.main = artWith('nightstalker', 50)
wielder.dayTime = 18000
check('Nightstalker 50: +50% damage at night', near(hit(wielder, makeMob(), 10), 15), hit(wielder, makeMob(), 10))
wielder.dayTime = 6000
check('...and nothing by day', hit(wielder, makeMob(), 10) == 10)
var faller = makePlayer(server, 'faller'); faller.armor = [artWith('featherfall', 1, true)]
check('Featherfall armor: no fall damage', hit(null, faller, 20, { tags: ['fall'] }) == 0)
var lucky = makePlayer(server, 'lucky'); lucky.armor = [artWith('second_life', 300, true)]; lucky.hp = 5
check('Second Life: a killing blow leaves you at half health instead', hit(makeMob(), lucky, 50) == 0 && lucky.hp == 10)
check('...and it needs to recharge', hit(makeMob(), lucky, 50) == 50)
var held = makePlayer(server, 'held'); held.off = artWith('nightstalker', 50); held.dayTime = 18000
check('an Artifact weapon in the off hand does nothing', hit(held, makeMob(), 10) == 10)
// Soulbound survives death
var dier = makePlayer(server, 'dier'), sb = artWith('soulbound', 1)
var deathDrops = javaList([{ getItem: function () { return sb } }, { getItem: function () { return stack(5) } }])
global.FA_keepSoulbound(dier, deathDrops)
check('Soulbound: the Artifact is taken out of your death drops', deathDrops.size() == 1)
check('...and given back when you respawn', global.FA_returnSoulbound(dier) == 1 && dier.given.length == 1 && dier.given[0].restored)
// Awakening
var awakener = makePlayer(server, 'awakener'), big2 = gear('Diamond Sword')
big2.id = 'minecraft:diamond_sword'; big2.maxDamage = 1561
faSetLevel(big2, 'sharpness', 3000)
awakener.main = big2
server.persistentData.putDouble('fa_essence_awakener', 1e6)
var soul = stack(1); soul.id = 'kubejs:ascended_soul'; awakener.inv = [soul]
check('Awakening turns 2,500+ levels of gear into an Artifact (costs Essence and an Ascended Soul)', faAwaken(awakener) == 1 && global.FA_isArtifact(big2) &&
  soul.count == 0 && faGetEssence(awakener) == 1e6 - global.FA.artifacts.awakenEssence)
var small2 = gear('Stick'); small2.maxDamage = 50; faSetLevel(small2, 'sharpness', 10); awakener.main = small2
check('...but not gear with too few levels', faAwaken(awakener) == 0 && !global.FA_isArtifact(small2))
var artTip = { arr: ['Sword'], add: function (i, t) { this.arr.splice(i, 0, t.s) } }
advanced(big2, false, artTip)
check('the tooltip shows ARTIFACT, 1 of 1, its serial, quirks and lore', /ARTIFACT\s+1 of 1/.test(artTip.arr[1]) && artTip.arr.some(function (l) { return /^\u25c8 /.test(l) }), artTip.arr.slice(0, 4).join(' | '))

// ---------------- Attachments ----------------
print('\nattachments')
function gearOf(kind) {
  var st = stack(1, 1, kind); st.id = 'test:' + kind; st.maxDamage = 500
  if (kind == 'helmet') st.getItem = function () { return new FakeArmorItem('HEAD') }
  if (kind == 'chest') st.getItem = function () { return new FakeArmorItem('CHEST') }
  if (kind == 'legs') st.getItem = function () { return new FakeArmorItem('LEGS') }
  if (kind == 'bow') st.getItem = function () { return new FakeBowItem() }
  return st
}
function attachItem(id) { var st = stack(3); st.id = 'kubejs:attachment_' + id; return st }
var tinker = makePlayer(server, 'tinker'), sword9 = gearOf('weapon'), saw = attachItem('chainsaw')
tinker.main = saw; tinker.off = sword9
handlers.rightClicked['kubejs:attachment_chainsaw']({ player: tinker, item: saw, hand: 'MAIN_HAND', cancel: function () {} })
check('a Chainsaw attaches to a sword (right-click, sword in the other hand)', global.FA_attachIds(sword9).join() == 'chainsaw' && saw.count == 2)
var lamp = attachItem('headlamp'); tinker.main = lamp
handlers.rightClicked['kubejs:attachment_headlamp']({ player: tinker, item: lamp, hand: 'MAIN_HAND', cancel: function () {} })
check('a Headlamp won\'t go on a sword', global.FA_attachIds(sword9).length == 1 && lamp.count == 3)
tinker.main = saw
handlers.rightClicked['kubejs:attachment_chainsaw']({ player: tinker, item: saw, hand: 'MAIN_HAND', cancel: function () {} })
check('no doubling up the same attachment', global.FA_attachIds(sword9).length == 1 && saw.count == 2)
var tz = attachItem('taser'), fl = attachItem('flamethrower'); tinker.main = tz
handlers.rightClicked['kubejs:attachment_taser']({ player: tinker, item: tz, hand: 'MAIN_HAND', cancel: function () {} })
tinker.main = fl
handlers.rightClicked['kubejs:attachment_flamethrower']({ player: tinker, item: fl, hand: 'MAIN_HAND', cancel: function () {} })
check(global.FA.attachments.slots + ' slots per item: a third attachment is refused', global.FA_attachIds(sword9).length == 2 && fl.count == 3)
// Chainsaw in combat
var sawer = makePlayer(server, 'sawer'), cs = gearOf('weapon'); faSetLevel(cs, 'sharpness', 0)
var tg = cs.getOrCreateTag(), dd = tg.getCompound('FactoryAscension'), at1 = new Compound(); at1.putString('0', 'chainsaw'); dd.put('attach', at1); tg.put('FactoryAscension', dd)
sawer.main = cs
var near1 = makeMob(), victimA = makeMob(); victimA.nearby = [near1]
check('Chainsaw: +30% damage and it rips into enemies next to the target', near(hit(sawer, victimA, 10), 13) && near1.hurtBy.length == 1, near1.hurtBy.join())
sawer.off = cs; sawer.main = stack(0)
check('a weapon attachment only works from the main hand', hit(sawer, makeMob(), 10) == 10)
// Sword attachments and batteries
function swordWith(id) { var g = gearOf('weapon'); faSetLevel(g, 'sharpness', 0); var t = g.getOrCreateTag(), d = t.getCompound('FactoryAscension'), a = new Compound(); a.putString('0', id); d.put('attach', a); t.put('FactoryAscension', d); return g }
function battery(kind, used) { var b = stack(1, 1); b.id = 'kubejs:' + kind; b.maxDamage = global.FA.attachments.batteries.filter(function (x) { return x.id == kind })[0].charge; b.damage = used || 0; return b }
var zapper = makePlayer(server, 'zapper'), cell = battery('copper_battery'); zapper.main = swordWith('electric_spike_wrap'); zapper.inv = [cell]
var arcA = makeMob(), zapped = makeMob(); zapped.nearby = [arcA]
var W = global.FA_attachmentDef('electric_spike_wrap')
check('Electric Spike Wrap: +' + W.spikes + ' spikes +' + W.power + ' shock, arcs to a nearby enemy, uses ' + W.charge + ' battery charge',
  near(hit(zapper, zapped, 10), 10 + W.spikes + W.power) && arcA.hurtBy.length == 1 && cell.damage == W.charge, cell.damage + ' / ' + arcA.hurtBy.join())
var flat = makePlayer(server, 'flat'); flat.main = swordWith('electric_spike_wrap'); flat.inv = [battery('copper_battery', 200)]
check('...with every battery flat only the spikes cut, and it says why', near(hit(flat, makeMob(), 10), 10 + W.spikes) && flat.bar.some(function (m) { return /No battery charge/.test(m) }), flat.bar.join(' | '))
var picky = makePlayer(server, 'picky'), fresh = battery('ascendite_battery', 10), old = battery('copper_battery', 150); picky.main = swordWith('plasma_edge'); picky.inv = [fresh, old]
var burnt = makeMob()
check('Plasma Edge: +40% and fire, drawing from the emptiest battery first', near(hit(picky, burnt, 10), 14) && burnt.fire > 0 && old.damage == 151 && fresh.damage == 10, old.damage + ' ' + fresh.damage)
var builder = makePlayer(server, 'builder'), spare = battery('copper_battery'); builder.creative = true; builder.main = swordWith('plasma_edge'); builder.inv = [spare]
check('Creative players need no charge', near(hit(builder, makeMob(), 10), 14) && spare.damage == 0)
var biter = makePlayer(server, 'biter'); biter.main = swordWith('vampire_fangs')
hit(biter, makeMob(), 10)
check('Vampire Fangs: heal 10% of the damage dealt', near(biter.healed, 1), biter.healed)
var guard = makePlayer(server, 'guard'), thug = makeMob(); guard.main = swordWith('tesla_guard'); guard.inv = [battery('copper_battery')]
hit(thug, guard, 5)
check('Tesla Guard: whatever hits you is zapped for 8', thug.hurtBy[0] == 8 && guard.inv[0].damage == 2, thug.hurtBy.join() + ' / ' + guard.inv[0].damage)
var charger = makePlayer(server, 'charger'), novaA = makeMob(), dummy = makeMob(); charger.main = swordWith('overcharger'); charger.inv = [battery('essence_cell')]; dummy.nearby = [novaA]
for (var oc = 0; oc < 4; oc++) hit(charger, dummy, 10)
var beforeNova = novaA.hurtBy.length
hit(charger, dummy, 10)
check('Overcharger: the 5th hit releases a shock nova', beforeNova == 0 && novaA.hurtBy.length == 1 && charger.inv[0].damage == 5, beforeNova + ' ' + novaA.hurtBy.length)
var lensed = makePlayer(server, 'lensed'); lensed.main = swordWith('targeting_lens'); Math.random = function () { return 0.1 }
check('Targeting Lens: a lucky hit crits for x1.5', near(hit(lensed, makeMob(), 10), 15))
Math.random = realRandom
check('batteries: crafted, recharged by hand and in a Mixer, never a duplication', global.FA.attachments.batteries.every(function (b) {
  return ['battery_', 'recharge_', 'mixer_recharge_'].every(function (k) { return recipes.some(function (r) { return r.rid == 'kubejs:factory_ascension/' + k + b.id }) })
}))
// Armor attachments
function wearWith(kind, id) { var g = gearOf(kind); var t = g.getOrCreateTag(), d = t.getCompound('FactoryAscension'), a = new Compound(); a.putString('0', id); d.put('attach', a); t.put('FactoryAscension', d); return g }
var padded = makePlayer(server, 'padded'); padded.armor = [wearWith('legs', 'knee_pads')]
check('Knee Pads: half fall damage', hit(null, padded, 10, { tags: ['fall'] }) == 5)
var spiky = makePlayer(server, 'spiky'), brute = makeMob(); spiky.armor = [wearWith('chest', 'spike_plating')]
hit(brute, spiky, 20)
check('Spike Plating: attackers take 25% back', brute.hurtBy[0] == 5, brute.hurtBy.join())
var sniper = makePlayer(server, 'sniper'), bowG = gearOf('bow'); var bt = bowG.getOrCreateTag(), bd = bt.getCompound('FactoryAscension'), ba = new Compound(); ba.putString('0', 'scope'); bd.put('attach', ba); bt.put('FactoryAscension', bd)
sniper.main = bowG
check('Scope: +25% arrow damage, but not on melee hits', near(hit(sniper, makeMob(), 10, { tags: ['projectile'], direct: makeMob() }), 12.5) && hit(sniper, makeMob(), 10) == 10)
// Headlamp places a light block that follows you, and cleans up
var miner3 = makePlayer(server, 'miner3'); miner3.armor = [wearWith('helmet', 'headlamp')]
miner3.x = 5.5; miner3.y = 64; miner3.z = 5.5; miner3.age = 4; fakeBlocks = {}
tick({ phase: 'END', player: miner3 })
check('Headlamp: a light block appears at your head', fakeBlocks['5,65,5'] == 'minecraft:light' && lastBlockProps.level == '15', JSON.stringify(fakeBlocks))
miner3.x = 9.5; miner3.age = 8
tick({ phase: 'END', player: miner3 })
check('...moves with you and leaves no light behind', fakeBlocks['9,65,5'] == 'minecraft:light' && !fakeBlocks['5,65,5'], JSON.stringify(fakeBlocks))
fakeBlocks['9,66,5'] = 'minecraft:stone'; miner3.y = 65; miner3.age = 12
tick({ phase: 'END', player: miner3 })
check('...and never replaces a real block', fakeBlocks['9,66,5'] == 'minecraft:stone')
miner3.armor = []; miner3.y = 64; miner3.age = 16
tick({ phase: 'END', player: miner3 })
check('taking the helmet off removes the light', Object.keys(fakeBlocks).filter(function (k) { return fakeBlocks[k] == 'minecraft:light' }).length == 0, JSON.stringify(fakeBlocks))
// Jetpack
var flyer3 = makePlayer(server, 'flyer3'); flyer3.armor = [wearWith('chest', 'jetpack')]; flyer3.ground = false; flyer3.sneaking = true; flyer3.age = 1
tick({ phase: 'END', player: flyer3 })
check('Jetpack: sneak in mid-air to rise (Levitation)', flyer3.effects.some(function (e) { return e.effect == $FAEffects.LEVITATION }))
// Menu
var fixer = makePlayer(server, 'fixer'); fixer.main = sword9; fixer.messages = []
faShowMenu(fixer)
var dclicks = []
fixer.messages.forEach(function (t) { dclicks = dclicks.concat(t.clicks || []) })
check('/ascension lists attachments with [Detach]', dclicks.indexOf('/ascension detach 0') >= 0 && dclicks.indexOf('/ascension detach 1') >= 0, dclicks.join(','))
fixer.given = []
faDetach(fixer, 0)
check('[Detach] takes it off and gives the attachment back', global.FA_attachIds(sword9).join() == 'taser' && fixer.given[0].id == 'kubejs:attachment_chainsaw')
check('every attachment has a recipe', global.FA.attachments.list.every(function (a) { return recipes.some(function (r) { return r.rid == 'kubejs:factory_ascension/attachment_' + a.id }) }))

check('mob loot tables only use drops that always exist (no forge:mod_loaded, which loot tables can\'t read)',
  Object.keys(global.FA.modMobs.minDanger).every(function (id) { return readFile('mod/src/main/resources/data/factoryascension/loot_tables/entities/' + id + '.json').indexOf('mod_loaded') < 0 }))
check('Mini Crusher: motors (with recipes and speeds) and a battery charge per item', global.FA.machines.miniCrusher.motors.every(function (m) { return m.speed > 0 && m.recipe[0].length == 3 }) &&
  global.FA.machines.miniCrusher.chargePerItem > 0)

check('the scripts\' block models never use the mod jar\'s textures (the Half download has no jar)',
  ['ascension_altar', 'essence_condenser'].every(function (b) { return readFile('kubejs/assets/kubejs/models/block/' + b + '.json').indexOf('factoryascension:') < 0 }))
check('the Enchantment Pack model only tilts by angles Minecraft accepts (0, 22.5, 45)',
  JSON.parse(readFile('kubejs/assets/kubejs/models/item/enchantment_pack.json')).elements.every(function (e) { return !e.rotation || [0, 22.5, -22.5, 45, -45].indexOf(e.rotation.angle) >= 0 }))

// ---------------- Missing-mod guard ----------------
print('\nmod guard')
var phIds = global.FA_MOD_CONTENT.blocks.concat(global.FA_MOD_CONTENT.items).map(function (i) { return 'factoryascension:' + i })
var phSeen = {}, phDupes = placeholders.filter(function (i) { var d = phSeen[i]; phSeen[i] = true; return d })
check('no placeholder id is registered twice (a block brings its own item; twice stops the game without the jar)', phDupes.length == 0, phDupes.join(', '))
check('without the jar, every mod block and item (' + phIds.length + ') gets a placeholder, so worlds keep them',
  phIds.length >= 50 && phIds.every(function (i) { return placeholders.indexOf(i) >= 0 }), phIds.filter(function (i) { return placeholders.indexOf(i) < 0 }).slice(0, 5).join())
Platform.getInfo = function (id) { return { getName: function () { return id == 'factoryascension' ? 'Factory Ascension' : id } } }
var guardIsLoaded0 = Platform.isLoaded
Platform.isLoaded = function (m) { return installedContentMods.indexOf(m) >= 0 }
var guardServer = makeServer()
handlers.loaded({ server: guardServer })
var seenMods = guardServer.persistentData.getCompound('fa_mods_seen')
check('a world remembers the mods that added its blocks and items (not Minecraft or KubeJS)',
  seenMods.getString('factoryascension') == 'Factory Ascension' && seenMods.contains('create') && !seenMods.contains('minecraft') && global.FA_missingMods().length == 0)
installedContentMods = ['minecraft', 'kubejs', 'create']
handlers.loaded({ server: guardServer })
check('opened without the mod jar: it notices (even with the placeholders in its namespace)', global.FA_missingMods().join() == 'factoryascension')
var forgetful = makePlayer(guardServer, 'forgetful')
handlers.loggedIn.forEach(function (fn) { fn({ player: forgetful }) })
var guardText = forgetful.messages.map(function (t) { return t.s }).join(' ')
var guardClicks = []
forgetful.messages.forEach(function (t) { guardClicks = guardClicks.concat(t.clicks || []) })
check('chat says "Oops, it looks like you\'ve forgotten to install Factory Ascension. Save last files..." with both buttons',
  /forgotten to install/.test(guardText) && /Factory Ascension/.test(guardText) && /Save last files/.test(guardText) &&
  guardClicks.indexOf('/ascension modguard save') >= 0 && guardClicks.indexOf('/ascension modguard ignore') >= 0, guardText)
installedContentMods = ['minecraft', 'kubejs', 'create', 'factoryascension']
Platform.isLoaded = guardIsLoaded0

// ---------------- Legendary stars ----------------
print('\nlegendary')
var LG = global.FA.legendary
function itemsOf(id, n) { var st = stack(n); st.id = id; return st }
var smith = makePlayer(server, 'smith'), lsword = gearOf('weapon'); faSetLevel(lsword, 'sharpness', 100)
smith.main = lsword; smith.inv = LG.costs[0].map(function (c) { return itemsOf(c[0], c[1] + 3) })
var star1 = global.FA_legendUp(smith, lsword)
var ldata = lsword.nbt.getCompound('FactoryAscension'), lfx = ldata.getCompound('legendfx').getCompound('0')
check('Star Up: costs come out of the inventory, the gear gets star 1 and a level-' + LG.effectLevels[0] + ' legendary effect that suits it (' + lfx.getString('id') + ')',
  star1 == 1 && ldata.getInt('legend') == 1 && smith.inv.every(function (st) { return st.count == 3 }) && lfx.getInt('level') == LG.effectLevels[0] &&
  global.FA_enchantFits(String(lfx.getString('id')), 'weapon'), smith.inv.map(function (st) { return st.count }).join())
check('...and with nothing left to pay, star 2 is refused', global.FA_legendUp(smith, lsword) == 0 && ldata.getInt('legend') == 1)
var five = gearOf('weapon'); faSetLevel(five, 'sharpness', 100); var fd = five.getOrCreateTag().getCompound('FactoryAscension'); fd.putInt('legend', 5); five.getOrCreateTag().put('FactoryAscension', fd)
var fiver = makePlayer(server, 'fiver'); fiver.main = five
check('5 Legendary stars: every level counts x1.5 (Sharpness 100 -> 150 = x16 damage)', near(hit(fiver, makeMob(), 1), 16), hit(fiver, makeMob(), 1))
check('legendary settings: ' + LG.maxStars + ' stars, a cost, an effect level and a grade for each', LG.costs.length == LG.maxStars && LG.effectLevels.length == LG.maxStars && LG.grades.length == LG.maxStars + 1)

// ---------------- Foils ----------------
print('\nfoils')
var FT = global.FA.foils.types, fr = new java.util.Random(4)
var tallyF = function (rarity) { var t = FT.map(function () { return 0 }), none = 0; for (var i = 0; i < 20000; i++) { var f = global.FA_rollFoil(function () { return fr.nextDouble() }, rarity); if (f < 0) none++; else t[f]++ } return { t: t, none: none } }
var commonF = tallyF(0), secretF = tallyF(5)
check('Common cards are rarely foiled; Secret Rares always are, with better foils', commonF.none > 17000 && secretF.none == 0 &&
  secretF.t[7] / 20000 > commonF.t[7] / (20000 - commonF.none) * 2, commonF.none + ' / ' + secretF.t.join(','))
var ft = stack(1); ft.id = 'kubejs:tome_sharpness'; faSetLevel(ft, 'sharpness', 100)
global.FA_setFoil(ft, 2)
check('a Gold Foil tome goes on gear at x1.5 (level 100 -> 150) and sets its look (CustomModelData 3)', faAppliedLevel(ft, 'sharpness') == 150 && ft.tag.d.CustomModelData == 3 && global.FA_foilOf(ft) == 2)
var foiler = makePlayer(server, 'foiler'), fgear = gear('Sword'); foiler.main = ft; foiler.off = fgear
server.persistentData.putDouble('fa_essence_foiler', 1000)
handlers.rightClicked['kubejs:tome_sharpness']({ player: foiler, item: ft, hand: 'MAIN_HAND', cancel: function () {} })
check('applying the foiled tome puts Sharpness 150 on the gear', faGetLevel(fgear, 'sharpness') == 150, faGetLevel(fgear, 'sharpness'))
var plainT = stack(1); plainT.id = 'kubejs:tome_smite'; var vs = stack(2); vs.id = 'kubejs:foil_sheet_void'
foiler.main = vs; foiler.off = plainT
handlers.rightClicked['kubejs:foil_sheet_void']({ player: foiler, item: vs, hand: 'MAIN_HAND', cancel: function () {} })
check('a Foil Sheet foils the tome in your other hand', global.FA_foilOf(plainT) == 6 && vs.count == 1)
var bs = stack(1); bs.id = 'kubejs:foil_sheet_bronze'; foiler.main = bs
handlers.rightClicked['kubejs:foil_sheet_bronze']({ player: foiler, item: bs, hand: 'MAIN_HAND', cancel: function () {} })
check('a worse foil never replaces a better one (sheet kept)', global.FA_foilOf(plainT) == 6 && bs.count == 1)
var foiledVal = faSellValue(plainT).value, plainT2 = stack(1); plainT2.id = 'kubejs:tome_smite'
check('a foiled tome sells for x its foil', near(foiledVal, faSellValue(plainT2).value * FT[6].mult))
var noModel = Object.keys(global.FA.enchants).filter(function (id) {
  var f = new File('kubejs/assets/kubejs/models/item/tome_' + id + '.json')
  if (!f.exists()) return true
  var m = JSON.parse(String(new java.lang.String(java.nio.file.Files.readAllBytes(f.toPath()), 'UTF-8')))
  return m.overrides.length != FT.length || !FT.every(function (t, i) { return new File('kubejs/assets/kubejs/models/item/foil/' + t.id + '/tome_' + id + '.json').exists() && m.overrides[i].predicate.custom_model_data == i + 1 })
})
check('every tome has a model with all ' + FT.length + ' foil looks, and every foil has its overlay', noModel.length == 0 &&
  FT.every(function (t) { return new File('kubejs/assets/kubejs/textures/item/foil_overlay_' + t.id + '.png').exists() }), noModel.slice(0, 3).join(','))

// ---------------- Selling ----------------
print('\nselling')
var seller = makePlayer(server, 'seller')
server.persistentData.putDouble('fa_essence_seller', 0)
var souls = stack(3); souls.id = 'kubejs:ascended_soul'; seller.main = souls
check('a sell quote prices the whole stack (3 Ascended Souls = 180 Essence)', faSellValue(souls).value == 180)
seller.given = []
faSell(seller, 'cores')
var paid = {}
seller.given.forEach(function (g) { paid[g.id] = (paid[g.id] || 0) + g.count })
check('selling for cores pays compressed: 180 = 2 Singularities + 2 Dense Cores', souls.count == 0 && paid['kubejs:ascension_singularity'] == 2 && paid['kubejs:dense_ascension_core'] == 2 && !paid['kubejs:ascension_core'],
  JSON.stringify(paid))
var gemS = stack(2); gemS.id = 'kubejs:gem_sapphire_3'; seller.main = gemS; seller.given = []
faSell(seller, 'buckets')
check('selling for Liquid Essence pays buckets (2 Perfect gems = 150 Essence = 15 buckets)', seller.given[0].id == 'kubejs:liquid_essence_bucket' && seller.given[0].count == 15)
var lvTome = stack(1); lvTome.id = 'kubejs:tome_sharpness'; faSetLevel(lvTome, 'sharpness', 100)
var plainTome = stack(1); plainTome.id = 'kubejs:tome_sharpness'
check('a leveled tome is worth more than a plain one (half its levels\' cost is refunded)', faSellValue(lvTome).value > faSellValue(plainTome).value + 100 &&
  faSellValue(plainTome).value == global.FA.sell.tomes.base, faSellValue(lvTome).value + ' / ' + faSellValue(plainTome).value)
var junk = stack(64); junk.id = 'minecraft:dirt'
check('things with no value can\'t be sold', faSellValue(junk).value == 0)
var hugeGear = gear('Axe'); faSetLevel(hugeGear, 'cleave', 2000000000)
check('even absurd levels price instantly (no endless loop)', isFinite(faSellValue(hugeGear).value) && faSellValue(hugeGear).value > 0)
seller.main = gear('Stick'); seller.given = []
check('selling an unsellable item just shows the quote', faSell(seller, 'cores') == 0 && seller.given.length == 0)
check('blaze rods without the Nether, and Waystones recipes only when Waystones is installed', !!byId('starter_blaze_rod') && !byId('starter_waystone'))
check('Voidsteel, Celestium and Ascendium ingots come from the factory', ['voidsteel_ingot', 'celestium_ingot', 'ascendium_ingot'].every(function (r) { return byId(r) && byId(r).json.type == 'create:mixing' }))

// ---------------- Moving the safe zone ----------------
print('\nsafe zone center')
var mover = makePlayer(server, 'mover')
server.overworld = function () { return { getSharedSpawnPos: function () { return { getX: function () { return 0 }, getZ: function () { return 0 } } } } }
mover.x = 6000; mover.z = 0
check('6000 blocks from spawn is danger 21', global.FA_dangerLevel(mover.level, 6000, 0) == 21)
faCenterCommand(mover, 'set')
check('/ascension center set: where you stand becomes the middle of the safe zone', global.FA_dangerLevel(mover.level, 6000, 0) == 0 &&
  global.FA_dangerLevel(mover.level, 0, 0) == 21 && global.FA_STATE.center.x == 6000)
var netherLvl = { dimension: 'minecraft:the_nether' }
check('the Nether\'s safe zone follows it (Overworld 6000 = Nether 750)', global.FA_dangerLevel(netherLvl, 750, 0) == global.FA.danger.netherBonus)
global.FA_STATE.center = false
faLoadCenter(server)
check('the center is saved with the world and loads back', global.FA_STATE.center && global.FA_STATE.center.x == 6000)
faCenterCommand(mover, 'reset')
check('/ascension center reset: back to world spawn (and never null on global)', global.FA_STATE.center === false && global.FA_dangerLevel(mover.level, 6000, 0) == 21)

// ---------------- Danger from your home (the default) ----------------
print('\ndanger from home')
global.FA.danger.centerOn = 'home'
check('the config measures danger from homes by default', defaultCenterOn == 'home', defaultCenterOn)
var settler = makePlayer(server, 'settler'), stranger = makePlayer(server, 'stranger')
settler.x = 50000; settler.z = 0; stranger.x = 3000; stranger.z = 0
check('no home yet: nothing scales anywhere, so you can explore for a base', global.FA_dangerLevel(settler.level, 50000, 0, settler) == 0 && global.FA_dangerLevel(settler.level, 99000, 0, settler) == 0)
settler.level.dimension = 'minecraft:overworld'
faSetHome(settler)
check('/home set makes your home the middle (and says what it means)', global.FA_STATE.homes[settler.stringUuid].x == 50000 &&
  global.FA_dangerLevel(settler.level, 50000, 0, settler) == 0 && global.FA_dangerLevel(settler.level, 50999, 0, settler) == 0 &&
  global.FA_dangerLevel(settler.level, 51000, 0, settler) == 1 && global.FA_dangerLevel(settler.level, 50000, 2000, settler) == 5 &&
  settler.messages.some(function (t) { return /danger zones start here/i.test(t.s) }), settler.messages.map(function (t) { return t.s }).join(' | '))
check('someone else\'s home doesn\'t count for you', global.FA_dangerLevel(stranger.level, 52000, 0, stranger) == 0)
// Mobs use the nearest player's home
var worldLvl = { dimension: 'minecraft:overworld', getServer: function () { return { getPlayerList: function () { return { getPlayers: function () { return [settler, stranger] } } } } } }
check('a mob near you uses your home; a mob near someone without a home doesn\'t scale', global.FA_dangerLevel(worldLvl, 50000, 3000) == 9 && global.FA_dangerLevel(worldLvl, 3100, 0) == 0)
var netherWorld = { dimension: 'minecraft:the_nether', getServer: worldLvl.getServer }
settler.level = { dimension: 'minecraft:the_nether' }
check('in the Nether your home counts at 1/8 (Overworld 50000 = Nether 6250)', global.FA_dangerLevel(netherWorld, 6250, 0, settler) == global.FA.danger.netherBonus)
settler.level = { dimension: 'minecraft:overworld' }
var homes0 = global.FA_STATE.homes
global.FA_STATE.homes = {}
faLoadHomes(server)
check('homes load back with the world', global.FA_STATE.homes[settler.stringUuid] && global.FA_STATE.homes[settler.stringUuid].x == 50000)
check('/ascension center does nothing while danger follows homes', faCenterCommand(mover, 'set') == 0 && global.FA_STATE.center === false)
check('two more starter recipes: an amethyst Ascension Core and an Ascendite alloy ingot', !!byId('core_from_amethyst') && !!byId('ascendite_alloy'))
var pressAsc = byId('press_cobblestone_ascendite')
check('a Mechanical Press over a Basin turns ' + global.FA.ascendite.pressCobble + ' cobblestone into Raw Ascendite', !!pressAsc && pressAsc.json.type == 'create:compacting' &&
  pressAsc.json.ingredients.length == global.FA.ascendite.pressCobble && pressAsc.json.ingredients.every(function (i) { return i.item == 'minecraft:cobblestone' }) &&
  pressAsc.json.results[0].item == 'kubejs:raw_ascendite')

// ---------------- Rare mob variants ----------------
print('\nvariants')
var VT = global.FA.variants.types
var counts = {}, N = 400000, rr = new java.util.Random(7)
for (var vi = 0; vi < N; vi++) { var vid = global.FA_rollVariant(function () { return rr.nextDouble() }, true); if (vid) counts[vid] = (counts[vid] || 0) + 1 }
check('variant odds: Golden ~1 in 4000, Shiny ~1 in 1000, each elemental ~1 in 1000 (400,000 hostile spawns)',
  Math.abs((counts.golden || 0) - N * 0.00025) < 45 && Math.abs((counts.shiny || 0) - N * 0.001) < 90 && Math.abs((counts.storm || 0) - N * 0.001) < 90, JSON.stringify(counts))
var peaceful = 0
for (var vj = 0; vj < 200000; vj++) { var pid = global.FA_rollVariant(function () { return rr.nextDouble() }, false); if (pid && global.FA_variantType(pid).elemental) peaceful++ }
check('animals can be Shiny or Golden but never elemental', peaceful == 0)
var goldPig = makeMob(); goldPig.type = 'minecraft:pig'
global.FA_makeVariant(goldPig, 'golden')
check('a Golden mob gets its name, glows and never despawns', /Golden Zombie/.test(goldPig.customName) && goldPig.nameVisible && goldPig.persistent &&
  goldPig.effects.some(function (e) { return e.effect == 'glowing' }) && global.FA_variantOf(goldPig) == 'golden', goldPig.customName)
var hurtHooks = handlers.forge['LivingHurtEvent']
var blazer = makeMob(); global.FA_makeVariant(blazer, 'blazing')
var burnt = makePlayer(server, 'burnt')
hit(blazer, burnt, 4)
check('a Blazing mob\'s hit sets you on fire', burnt.fire == global.FA.variants.fireSeconds, burnt.fire)
var froster = makeMob(); global.FA_makeVariant(froster, 'frozen')
var chilled = makePlayer(server, 'chilled')
hit(froster, chilled, 4)
check('a Frozen mob\'s hit slows you', chilled.effects.some(function (e) { return e.effect == 'slowness' && e.amp == 1 }))
var stormer = makeMob(); global.FA_makeVariant(stormer, 'storm')
var zapped = makePlayer(server, 'zapped')
check('a Storm mob\'s hit calls lightning and hits 20% harder', near(hit(stormer, zapped, 10), 12), hit(stormer, zapped, 10))
var gdrops = dropEvent(plainHero, goldPig, []); dropsHook(gdrops)
var gids = gdrops.list.arr.map(function (ie) { return ie.getItem().id })
check('a Golden mob drops gold, cores, a Flawless gem and 3 loot rolls', gids.indexOf('minecraft:gold_block') >= 0 && gids.indexOf('kubejs:ascension_core') >= 0 &&
  gids.some(function (x) { return /^kubejs:gem_\w+_2$/.test(x) }) && gids.length >= 6, gids.join(','))
var vxp = { xp: 10, getEntity: function () { return goldPig }, getAttackingPlayer: function () { return plainHero }, getDroppedExperience: function () { return this.xp }, setDroppedExperience: function (n) { this.xp = n } }
xpHook(vxp)
check('a Golden mob drops x10 XP', vxp.xp == 100, vxp.xp)
var named = makeMob(); named.customName = 'Bob'
check('named mobs (pets, bosses, elites) never turn into variants', global.FA_variantOf(named) == '')

// ---------------- Leveling tomes, applying them, extracting enchants ----------------
print('\ntome levels')
var scribe = makePlayer(server, 'scribe')
server.persistentData.putDouble('fa_essence_scribe', 1e9)
var stome = stack(1); stome.id = 'kubejs:tome_sharpness'
scribe.main = stome; scribe.off = stack(0)
faUpgradeTome(scribe, 249)
check('a held tome levels up on its own (/ascension tome): Sharpness tome 1 -> 250', faGetLevel(stome, 'sharpness') == 250, faGetLevel(stome, 'sharpness'))
var spentTome = 1e9 - faGetEssence(scribe), expectTome = 0
for (var tl = 1; tl < 250; tl++) expectTome += global.FA_cost(tl, 'sharpness')
check('...at the same Essence price as leveling gear', near(spentTome, expectTome), spentTome + ' vs ' + expectTome)
check('holding a leveled tome gives none of its power', !(faLevels(scribe).sharpness > 0))
var pile = stack(3); pile.id = 'kubejs:tome_sharpness'; scribe.main = pile
check('a stack of tomes can\'t be leveled all at once', faUpgradeTome(scribe, 5) == 0 && !pile.tag)
var carnage = gear('Netherite Sword')
scribe.main = stome; scribe.off = carnage
handlers.rightClicked['kubejs:tome_sharpness']({ player: scribe, item: stome, hand: 'MAIN_HAND', cancel: function () {} })
check('applying a level 250 tome puts Sharpness 250 straight on the gear', faGetLevel(carnage, 'sharpness') == 250 && stome.count == 0, faGetLevel(carnage, 'sharpness'))
var big = stack(1); big.id = 'kubejs:tome_sharpness'; faSetLevel(big, 'sharpness', 900)
scribe.main = big; scribe.given = []
handlers.rightClicked['kubejs:tome_sharpness']({ player: scribe, item: big, hand: 'MAIN_HAND', cancel: function () {} })
check('a higher tome swaps in, and the old level comes back as a tome (nothing lost)', faGetLevel(carnage, 'sharpness') == 900 &&
  scribe.given.length == 1 && scribe.given[0].id == 'kubejs:tome_sharpness' && faGetLevel(scribe.given[0], 'sharpness') == 250)
var small = stack(1); small.id = 'kubejs:tome_sharpness'
scribe.main = small
handlers.rightClicked['kubejs:tome_sharpness']({ player: scribe, item: small, hand: 'MAIN_HAND', cancel: function () {} })
check('a lower tome is refused and kept', small.count == 1 && faGetLevel(carnage, 'sharpness') == 900)
var blankT = stack(2); blankT.id = 'kubejs:blank_ascension_tome'
scribe.main = carnage; scribe.off = blankT; scribe.given = []
faExtract(scribe, 'sharpness')
check('extracting: Sharpness 900 comes off the gear into a level 900 tome, using a Blank Tome', faGetLevel(carnage, 'sharpness') == 0 && blankT.count == 1 &&
  scribe.given.length == 1 && faGetLevel(scribe.given[0], 'sharpness') == 900)
check('extracting needs the enchant to be there', faExtract(scribe, 'sharpness') == 0 && blankT.count == 1)
var eternalGear = gear('Bow'); faSetLevel(eternalGear, 'eternal', 1)
scribe.main = eternalGear
faExtract(scribe, 'eternal')
check('extracting Eternal makes the gear breakable again', eternalGear.tag.d.Unbreakable === undefined, JSON.stringify(Object.keys(eternalGear.tag.d)))
var menuTome = stack(1); menuTome.id = 'kubejs:tome_protection'; faSetLevel(menuTome, 'protection', 40)
scribe.main = menuTome; scribe.off = stack(0); scribe.messages = []
faShowMenu(scribe)
var tclicks = []
scribe.messages.forEach(function (t) { tclicks = tclicks.concat(t.clicks || []) })
check('/ascension with a tome in hand shows the tome and its level buttons', /Tome of Ascended Protection 40/.test(scribe.messages[1].s) && tclicks.indexOf('/ascension tome 100') >= 0, scribe.messages.map(function (t) { return t.s }).join(' | '))
scribe.main = gear('Axe'); faSetLevel(scribe.main, 'cleave', 5); scribe.messages = []
faShowMenu(scribe)
tclicks = []
scribe.messages.forEach(function (t) { tclicks = tclicks.concat(t.clicks || []) })
check('gear enchants in the menu have an [Extract] button', tclicks.indexOf('/ascension extract cleave') >= 0)

// ---------------- Gear condition ----------------
print('\ncondition')
var CG = global.FA.condition.grades
var tally = CG.map(function () { return 0 })
for (var r = 0; r < 20000; r++) tally[global.FA_rollCondition(Math.random)]++
check('grades roll at their weights (20,000 rolls)', CG.every(function (g, i) { return Math.abs(tally[i] / 200 - g.weight) < 1.5 }), tally.join(' / '))
function blade(max) { var b = stack(1, 1, 'Diamond Sword'); b.id = 'minecraft:diamond_sword'; b.maxDamage = max; return b }
var scarred = blade(1000)
global.FA_assignCondition(scarred, function () { return 0 })
check('a Battle-Scarred roll comes 30% worn', global.FA_conditionOf(scarred) == 0 && scarred.damage == 300, scarred.damage)
var again = global.FA_assignCondition(scarred, function () { return 0.999 })
check('a grade is rolled only once', again == 0 && global.FA_conditionOf(scarred) == 0)
var stackable = stack(16); stackable.id = 'minecraft:cobblestone'
check('stackable items never get a grade', global.FA_assignCondition(stackable, Math.random) == -1 && !stackable.tag)
var fighter = makePlayer(server, 'fighter'), dummy = makeMob()
var mint = blade(1000); global.FA_setCondition(mint, 4)
fighter.main = mint
check('Mint Condition: x1.12 damage', near(hit(fighter, dummy, 10), 11.2), hit(fighter, dummy, 10))
fighter.main = scarred
check('Battle-Scarred: x0.85 damage', near(hit(fighter, dummy, 10), 8.5), hit(fighter, dummy, 10))
var tank2 = makePlayer(server, 'armored'), plate = blade(500), plate2 = blade(500)
global.FA_setCondition(plate, 5); global.FA_setCondition(plate2, 5)
tank2.armor = [plate, plate2]
check('armor grades stack: two Pristine pieces take x0.94 x0.94 damage', near(hit(dummy, tank2, 10), 10 * 0.94 * 0.94), hit(dummy, tank2, 10))
tank2.armor = []; tank2.main = plate
check('a graded item in your hand is not armor', hit(dummy, tank2, 10) == 10)
var crafted2 = blade(800)
handlers.crafted({ item: crafted2, player: fighter })
check('crafting gear rolls its grade right away', global.FA_conditionOf(crafted2) >= 0)
var holder = makePlayer(server, 'holder'), held = blade(300)
holder.main = held
global.FA_conditionCheck(holder)
check('gear gets a grade the first time it\'s held (loot, trades, Create-crafted)', global.FA_conditionOf(held) >= 0)
// Polishing keeps the better grade
var polisher = makePlayer(server, 'polisher'), dull = blade(500), cloth = stack(4); cloth.id = 'kubejs:polishing_cloth'
global.FA_setCondition(dull, 1)
polisher.main = cloth; polisher.off = dull
Math.random = function () { return 0.01 }   // rolls Battle-Scarred: worse, so it's kept
handlers.rightClicked['kubejs:polishing_cloth']({ player: polisher, item: cloth, hand: 'MAIN_HAND', cancel: function () {} })
var keptWorse = global.FA_conditionOf(dull) == 1 && cloth.count == 3
Math.random = function () { return 0.995 }  // rolls Pristine
handlers.rightClicked['kubejs:polishing_cloth']({ player: polisher, item: cloth, hand: 'MAIN_HAND', cancel: function () {} })
check('a Polishing Cloth rerolls and keeps the better grade (a worse roll changes nothing)', keptWorse && global.FA_conditionOf(dull) == 5 && cloth.count == 2, global.FA_conditionOf(dull))
handlers.rightClicked['kubejs:polishing_cloth']({ player: polisher, item: cloth, hand: 'MAIN_HAND', cancel: function () {} })
check('Pristine can\'t be polished further (the cloth is kept)', cloth.count == 2)
Math.random = realRandom
var gradeTip = { arr: ['Diamond Sword'], add: function (i, t) { this.arr.splice(i, 0, t.s) } }
advanced(mint, false, gradeTip)
check('the grade shows right under the name with its effect', /Mint Condition\s+\+12% damage/.test(gradeTip.arr[1]), gradeTip.arr.join(' | '))

// ---------------- KubeJS's `global` ----------------
// In KubeJS `global` is a Java map, and reading a key that holds null crashes the game (a real crash, from the
// server tick). These two checks make sure no script ever leaves null (or undefined) on it.
print('\nkubejs global')
var nullAssigns = []
;['startup_scripts', 'server_scripts', 'client_scripts'].forEach(function (dir) {
  var files = new java.io.File('kubejs/' + dir).listFiles()
  for (var i = 0; i < files.length; i++) {
    var text = String(new java.lang.String(java.nio.file.Files.readAllBytes(files[i].toPath()), 'UTF-8'))
    var lines = text.split('\n')
    lines.forEach(function (line, n) {
      if (/\bglobal\.[\w.]+\s*=\s*(null|undefined)\b/.test(line) || /\bdelete\s+global\./.test(line)) nullAssigns.push(files[i].getName() + ':' + (n + 1))
    })
  }
})
check('no script ever sets a global key to null or deletes one', nullAssigns.length == 0, nullAssigns.join(', '))
// Deep scan: nothing reachable from global (config, state, caches) may hold null or undefined
var nullPaths = [], seenObjs = []
function scanNull(v, path, depth) {
  if (v === null || v === undefined) { nullPaths.push(path); return }
  if (typeof v != 'object' || depth > 8 || seenObjs.indexOf(v) >= 0) return
  seenObjs.push(v)
  Object.keys(v).forEach(function (k) { scanNull(v[k], path + '.' + k, depth + 1) })
}
Object.keys(global).forEach(function (k) { scanNull(global[k], 'global.' + k, 0) })
check('after every test, nothing reachable from global holds null', nullPaths.length == 0, nullPaths.slice(0, 5).join(', '))

// ---------------- KubeJS-real method names ----------------
// KubeJS hides or renames many vanilla methods (found on a real server). A script using one of these only fails
// in game, never here, so they're banned outright. Comments are skipped.
// ---------------- Skinwalkers ----------------
print('\nskinwalkers')
var SW = global.FA.skinwalkers
var formSeen = {}
for (var fi = 0; fi < 400; fi++) formSeen[global.FA_skinwalkerForm(Math.random, function (id) { return id.indexOf('minecraft:') == 0 })] = true
check('a skinwalker turns into one of its strong forms, never a mob from a mod that isn\'t installed (' + Object.keys(formSeen).length + ' seen)',
  Object.keys(formSeen).every(function (f) { return f.indexOf('minecraft:') == 0 }) && Object.keys(formSeen).length >= 4, Object.keys(formSeen).join(', '))
check('skinwalkers hide among ordinary animals, only out in the wilderness, and always come back Elite', SW.animals.indexOf('minecraft:cow') >= 0 &&
  SW.minDanger >= 1 && SW.chance > 0 && SW.chance < 0.1 && SW.forms.every(function (f) { return f[1] > 0 }))
var swText = String(new java.lang.String(java.nio.file.Files.readAllBytes(new java.io.File('kubejs/startup_scripts/76_fa_skinwalkers.js').toPath()), 'UTF-8'))
check('only a creature\'s hit makes it turn: falls, the void and /kill still work on it', /if \(!\(who instanceof \$FALiving\)\) return\s+event.setCanceled\(true\)/.test(swText))

// ---------------- The mod's mobs and bosses (config side) ----------------
print('\nmod mobs and bosses')
var cfgText = String(new java.lang.String(java.nio.file.Files.readAllBytes(new java.io.File('kubejs/startup_scripts/00_fa_config.js').toPath()), 'UTF-8'))
var modParts = cfgText.match(/mobs: \['factoryascension:(\w+)'\]/g) || []
var MM = global.FA.modMobs
check('every one of the mod\'s ' + Object.keys(MM.minDanger).length + ' mobs drops its own part (' + modParts.length + ')',
  Object.keys(MM.minDanger).length >= 34 && Object.keys(MM.minDanger).every(function (id) { return cfgText.indexOf("mobs: ['factoryascension:" + id + "']") >= 0 }))
check('bosses are deep, rare and spread out, and drop an Ascended Soul', Object.keys(MM.bosses).length >= 6 && Object.keys(MM.bosses).every(function (id) {
  var lootText = String(new java.lang.String(java.nio.file.Files.readAllBytes(new java.io.File('mod/src/main/resources/data/factoryascension/loot_tables/entities/' + id + '.json').toPath()), 'UTF-8'))
  return MM.minDanger[id] >= 8 && MM.bosses[id].chance < 1 && MM.bosses[id].spacing >= 200 && lootText.indexOf('kubejs:ascended_soul') >= 0 }))

// ---------------- The jar must load without the KubeJS data (the Realm's biomes and dimension live there) ----------------
print('\njar without the scripts')
var jarRefs = []
;(function walk(dir) {
  var files = new java.io.File(dir).listFiles()
  if (!files) return
  for (var i = 0; i < files.length; i++) {
    var f = files[i]
    if (f.isDirectory()) { walk(f.getPath()); continue }
    if (!/\.json$/.test(String(f.getName()))) continue
    var text = String(new java.lang.String(java.nio.file.Files.readAllBytes(f.toPath()), 'UTF-8'))
    if (/"factoryascension:(ascended_shelf|void_garden|astral_peaks|ascended_realm)"/.test(text)) jarRefs.push(String(f.getName()))
  }
})('mod/src/main/resources/data')
check('the mod jar never names a Realm biome or the Realm itself directly (only by tag), so it starts without the scripts', jarRefs.length == 0, jarRefs.join(', '))

// ---------------- Leaks in the mod's code: whatever opens a file, stream, image or thread must close it ----------------
print('\nresource leaks (static)')
var leakSpots = []
;(function walk(dir) {
  var files = new java.io.File(dir).listFiles()
  if (!files) return
  for (var i = 0; i < files.length; i++) {
    var f = files[i]
    if (f.isDirectory()) { walk(f.getPath()); continue }
    if (!/\.java$/.test(String(f.getName()))) continue
    var lines = String(new java.lang.String(java.nio.file.Files.readAllBytes(f.toPath()), 'UTF-8')).split('\n')
    for (var n = 0; n < lines.length; n++) {
      var line = lines[n]
      if (!/new FileInputStream|new FileOutputStream|Files\.new(In|Out)putStream|new ZipOutputStream|new ZipFile|NativeImage\.read\(|new NativeImage\(|Files\.walk\(|Files\.list\(/.test(line)) continue
      if (/try \(/.test(line) || /try \(\w+\)/.test(lines.slice(n, n + 4).join(' '))) continue   // try-with-resources closes it
      // Otherwise its variable must be closed in a finally block within the next 80 lines
      var m = line.match(/(\w+)\s*=\s*(new |NativeImage\.read|Files\.)/)
      var name = m ? m[1] : null
      var rest = lines.slice(n, n + 80).join('\n')
      if (name && new RegExp('finally[\\s\\S]*' + name + '\\.close\\(\\)').test(rest)) continue
      leakSpots.push(String(f.getName()) + ':' + (n + 1))
    }
  }
})('mod/src/main/java')
check('every file, stream, zip and image the mod opens is closed (try-with-resources, or close() in finally)', leakSpots.length == 0, leakSpots.join(', '))
var threadSpots = []
;(function walk(dir) {
  var files = new java.io.File(dir).listFiles()
  if (!files) return
  for (var i = 0; i < files.length; i++) {
    var f = files[i]
    if (f.isDirectory()) { walk(f.getPath()); continue }
    if (!/\.java$/.test(String(f.getName()))) continue
    var text = String(new java.lang.String(java.nio.file.Files.readAllBytes(f.toPath()), 'UTF-8'))
    if (/new Thread\(/.test(text) && !/setDaemon\((true|false)\)/.test(text)) threadSpots.push(String(f.getName()))
  }
})('mod/src/main/java')
check('every thread the mod starts says whether it may outlive the game (daemon, or on purpose not, like the backup that must finish)', threadSpots.length == 0, threadSpots.join(', '))

// ---------------- Conflicts: a mod that does the same job turns the pack's version off ----------------
print('\nconflicts')
var realIsLoaded = Platform.isLoaded
Platform.isLoaded = function (id) { return id == 'scalinghealth' || id == 'ftbessentials' }
load('kubejs/startup_scripts/01_fa_conflicts.js')
check('Scaling Health turns Danger Zones off, FTB Essentials turns /home off, and each says so',
  !global.FA.danger.enabled && !global.FA.home.command && global.FA.variants.enabled && global.FA_CONFLICTS.length == 2 &&
  global.FA_CONFLICTS[0] == "The Scaling Health mod conflicts with this mod's Danger Zones feature: Turned Danger Zones off.", global.FA_CONFLICTS.join(' | '))
print('  sample: ' + global.FA_CONFLICTS.join(' | '))
Platform.isLoaded = realIsLoaded
global.FA.danger.enabled = true
global.FA.home.command = true
global.FA_CONFLICTS = []

print('\nkubejs names')
var banned = [
  [/getStringUUID\(/, 'stringUuid'], [/getScoreboardName\(/, 'username'], [/\.(getTag|setTag)\(|\.hasTag\(\)/, 'stack.nbt'],
  [/\.level\(\)/, '.level'], [/getGameTime\(/, 'faNow()'], [/\.dimension\(\)/, 'String(level.dimension)'],
  [/get[XY]Rot\(/, 'yaw / pitch'], [/clearFire\(/, 'extinguish()'], [/\.hurt\(/, 'attack(src, amount)'],
  [/getMsgId\(/, 'source.getType()'], [/getDirectEntity\(/, 'source.immediate'], [/setDeltaMovement\(/, 'setMotion'],
  [/^(?!.*getNavigation\(\)\.moveTo).*\.moveTo\(/, 'setPosition (getNavigation().moveTo, the pathfinder, is fine)'], [/\.teleportTo\(/, 'setPositionAndRotation'], [/setGlowingTag\(/, 'glowing = true'], [/isCurrentlyGlowing\(/, 'the glowing property'],
  [/tickCount/, 'age'], [/noCollision\(/, 'faClear()'], [/new \$?\w*FluidStack\(/, "Fluid.of(id, mb)"],
  [/getType\(\)\.(getDescription|create)\(/, 'getDisplayName / level.createEntity(e.type)'], [/BLOCK\.getKey\(/, 'level.getBlock(pos).id'],
  [/state\.is\(/, 'getBlock(pos).hasTag(id)'], [/Array\.from\(/, 'a for loop (KubeJS drops its mapping function)'], [/source\.getEntity\(\)|getSource\(\)\.getEntity\(\)/, 'source.actual']
]
var badNames = []
;['startup_scripts', 'server_scripts', 'client_scripts'].forEach(function (dir) {
  var files = new java.io.File('kubejs/' + dir).listFiles()
  for (var i = 0; i < files.length; i++) {
    var text = String(new java.lang.String(java.nio.file.Files.readAllBytes(files[i].toPath()), 'UTF-8'))
    text.split('\n').forEach(function (line, n) {
      var code = line.replace(/\/\/.*$/, '')
      banned.forEach(function (b) { if (b[0].test(code)) badNames.push(files[i].getName() + ':' + (n + 1) + ' (use ' + b[1] + ')') })
      // KubeJS's Rhino rejects a const inside a block ("redeclaration of var"), and in a startup script that crashes the game
      if (/^\s+const\s/.test(code)) badNames.push(files[i].getName() + ':' + (n + 1) + ' (const inside a block: use let)')
    })
  }
})
check('no script uses a method KubeJS hides or renames, or a const inside a block', badNames.length == 0, badNames.slice(0, 6).join(', '))

print('\n' + (failures ? failures + ' FAILED' : 'all checks passed'))
if (failures) java.lang.System.exit(1)
