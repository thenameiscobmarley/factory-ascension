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
var console = { info: function (m) { print('  [info] ' + m) }, error: function (m) { print('  [error] ' + m) } }

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
;['gold', 'gray', 'darkGray', 'lightPurple', 'red', 'darkRed', 'green', 'aqua', 'yellow', 'white', 'blue', 'of'].forEach(function (c) { Text[c] = fakeText })

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

var StartupEvents = { registry: function (type, fn) { handlers.registry.push(fn) } }
var ServerEvents = { recipes: function (fn) { handlers.recipes.push(fn) }, commandRegistry: function (fn) { handlers.commands.push(fn) } }
var ItemEvents = { rightClicked: function (id, fn) { handlers.rightClicked[id] = fn }, tooltip: function (fn) { handlers.tooltip.push(fn) } }
var ForgeEvents = { onEvent: function (cls, fn) { handlers.forge[cls.split(/[.$]/).pop()] = fn } }

function javaList(arr) {
  return {
    arr: arr,
    iterator: function () { var i = 0, a = this.arr; return { hasNext: function () { return i < a.length }, next: function () { return a[i++] } } },
    add: function (x) { this.arr.push(x) }
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
    getOrCreateTag: function () { if (!this.tag) this.tag = new Compound(); return this.tag },
    getHoverName: function () { return fakeText(this.name) }
  }
}
function gear(name) { return stack(1, 1, name) }

function makeServer() {
  var data = new Compound()
  return { persistentData: data, commands: [], runCommandSilent: function (c) { this.commands.push(c) } }
}

function makePlayer(server, uuid) {
  var p = new PlayerClass()
  var attrs = {}
  p.main = stack(0); p.off = stack(0); p.armor = [stack(0), stack(0), stack(0), stack(0)]
  p.health = 20; p.healed = 0; p.messages = []; p.bar = []; p.given = []; p.sneaking = false; p.attrs = attrs; p.effects = []
  p.getServer = function () { return server }
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
  p.displayClientMessage = function (t) { this.bar.push(t.s) }
  p.tell = function (t) { this.messages.push(t) }
  p.give = function (i) { this.given.push(i) }
  return p
}
// Shared living-entity behaviour for fake players and mobs
var gameTime = 1000
function livingBits(o, maxHealth) {
  o.hp = maxHealth; o.maxHp = maxHealth; o.hurtBy = []; o.effects = o.effects || []; o.fire = 0
  o.pdata = new Compound(); o.x = 0; o.z = 0
  o.getPersistentData = function () { return this.pdata }
  o.setCustomName = function (n) { this.customName = n.s }
  o.setCustomNameVisible = function (v) { this.nameVisible = v }
  o.getType = function () { return { getDescription: function () { return fakeText('Zombie') } } }
  o.mobType = MobType.UNDEFINED; o.ground = true; o.wet = false; o.dim = 'minecraft:overworld'; o.boss = false; o.nearby = []
  o.isAlive = function () { return true }
  o.getHealth = function () { return this.hp }
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
  'server_scripts/10_fa_ascension.js', 'server_scripts/20_fa_core_recipes.js', 'server_scripts/25_fa_tome_recipes.js',
  'server_scripts/30_fa_material_recipes.js', 'client_scripts/10_fa_tooltips.js'].forEach(function (f) {
  load('kubejs/' + f)
  print('loaded ' + f)
})
var E = global.FA.enchants

// ---------------- Items & textures ----------------
print('\nitems')
var created = []
var builder = {}
;['displayName', 'rarity', 'glow', 'maxStackSize'].forEach(function (m) { builder[m] = function () { return builder } })
handlers.registry.forEach(function (fn) { fn({ create: function (id) { created.push(id); return builder } }) })
var enchantCount = Object.keys(global.FA.enchants).length
check('59 enchants (9 original + 50 new)', enchantCount == 59, enchantCount)
check('items registered (4 core + blank + tomes + 3 clusters)', created.length == 8 + enchantCount, created.length)
var File = java.io.File
var missing = created.filter(function (id) { return !new File('kubejs/assets/kubejs/textures/item/' + id + '.png').exists() })
check('every item has a texture', missing.length == 0, missing.join(','))

// ---------------- Recipes ----------------
print('\nrecipes')
var recipes = []
var ev = {
  custom: function (j) { var r = { json: j, id: function (i) { r.rid = i; return r } }; recipes.push(r); return r },
  shapeless: function (o, i) { var r = { json: { type: 'shapeless' }, id: function (x) { r.rid = x; return r } }; recipes.push(r); return r }
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
function itemBag(r) { return r.json.ingredients.filter(function (i) { return i.item }).map(function (i) { return i.item }).sort() }
function isSubset(a, b) { var c = b.slice(); return a.every(function (x) { var i = c.indexOf(x); if (i < 0) return false; c.splice(i, 1); return true }) }
var clash = []
mixes.forEach(function (a) { mixes.forEach(function (b) { if (a !== b && isSubset(itemBag(a), itemBag(b))) clash.push(a.rid + ' in ' + b.rid) }) })
check('no mixing recipe is a subset of another', clash.length == 0, clash.join('; '))

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
death({ getSource: function () { return src(eater) } })
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
check('unstackables become extra entities', d2.list.arr.length == 2)
var d3 = dropEvent(hero({ plunder: 100000 }), makeMob(), [stack(1, 1)]); dropsHook(d3)
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
check('tooltips for cores, blank tome, every tome and clusters', simple == 3 + 1 + enchantCount + 4, simple)
var lines = { arr: ['Netherite Chestplate', 'When on body:'], add: function (i, t) { this.arr.splice(i, 0, t.s) } }
advanced(chest, false, lines)
check('enchants show right under the item name', /Ascended Protection 3,000/.test(lines.arr[1]), lines.arr.join(' | '))
var plain = { arr: ['Stick'], add: function () { throw 'should not add' } }
advanced(stack(1), false, plain)
check('items without enchants are untouched', plain.arr.length == 1)

print('\ndescriptions')
var blank = Object.keys(E).filter(function (id) { var d = global.FA_describe(id, 1); return !d || /undefined|NaN/.test(d + global.FA_describe(id, 3000)) })
check('every enchant has a description at levels 1 and 3000', blank.length == 0, blank.join(','))
check('numbers', global.FA_num(1234) == '1,234' && global.FA_num(2.5e9) == '2.50B' && global.FA_num(1e40).indexOf('e+') > 0)
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

print('\n' + (failures ? failures + ' FAILED' : 'all checks passed'))
if (failures) java.lang.System.exit(1)
