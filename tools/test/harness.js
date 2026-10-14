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

// ---- Fakes ----
var handlers = { registry: [], recipes: [], rightClicked: {}, loggedIn: [], respawned: [], loggedOut: [], commands: [], tooltip: [], forge: {} }
var global = {}
var console = { info: function (m) { print('  [info] ' + m) }, error: function (m) { print('  [error] ' + m) } }

function fakeText(s) {
  var t = { s: s, append: function (o) { return fakeText(this.s + o.s) }, bold: function () { return this } }
  return t
}
var Text = {}
;['gold', 'gray', 'darkGray', 'lightPurple', 'red', 'green', 'aqua', 'yellow', 'white', 'of'].forEach(function (c) { Text[c] = fakeText })

var KNOWN_ITEMS = { 'mekanism:raw_osmium': 1, 'mekanism:raw_lead': 1, 'mekanism:raw_tin': 1, 'mekanism:raw_uranium': 1, 'tfmg:steel_ingot': 1, 'tfmg:raw_nickel': 1 }
var Item = { of: function (id, n) { return { id: id, count: n || 1, isEmpty: function () { return !KNOWN_ITEMS[id] } } } }
var Platform = { isLoaded: function (m) { return m == 'tfmg' || m == 'mekanism' } }
var Utils = { server: null }

function FakeClass(name) { this.name = name }
var PlayerClass = function () {}
var ItemEntityClass = function (level, x, y, z, stack) { this.stack = stack; this.setDefaultPickUpDelay = function () {} }
var Java = {
  loadClass: function (n) {
    if (n == 'net.minecraft.world.entity.player.Player') return PlayerClass
    if (n == 'net.minecraft.world.entity.item.ItemEntity') return ItemEntityClass
    if (n == 'net.minecraft.tags.DamageTypeTags') return { BYPASSES_INVULNERABILITY: 'bypass' }
    if (n == 'net.minecraft.world.entity.ai.attributes.Attributes') return { MAX_HEALTH: 'max_health', MOVEMENT_SPEED: 'movement_speed' }
    if (n == 'net.minecraft.world.entity.ai.attributes.AttributeModifier') return function (name, amount, op) { this.name = name; this.amount = amount; this.op = op; this.getName = function () { return this.name } }
    if (n == 'net.minecraft.world.entity.ai.attributes.AttributeModifier$Operation') return { ADDITION: 'add', MULTIPLY_BASE: 'mul_base' }
    throw 'unknown class ' + n
  }
}

var StartupEvents = { registry: function (type, fn) { handlers.registry.push(fn) } }
var ServerEvents = {
  recipes: function (fn) { handlers.recipes.push(fn) },
  commandRegistry: function (fn) { handlers.commands.push(fn) }
}
var ItemEvents = {
  rightClicked: function (id, fn) { handlers.rightClicked[id] = fn },
  tooltip: function (fn) { handlers.tooltip.push(fn) }
}
var PlayerEvents = {
  loggedIn: function (fn) { handlers.loggedIn.push(fn) },
  respawned: function (fn) { handlers.respawned.push(fn) },
  loggedOut: function (fn) { handlers.loggedOut.push(fn) }
}
var ForgeEvents = { onEvent: function (cls, fn) { handlers.forge[cls.split('.').pop()] = fn } }

function javaList(arr) {
  return {
    arr: arr,
    iterator: function () { var i = 0, a = this.arr; return { hasNext: function () { return i < a.length }, next: function () { return a[i++] } } },
    add: function (x) { this.arr.push(x) }
  }
}

function makeServer() {
  var data = {}
  var server = {
    commands: [], told: [],
    persistentData: {
      getInt: function (k) { return data[k] || 0 }, putInt: function (k, v) { data[k] = v },
      getDouble: function (k) { return data[k] || 0 }, putDouble: function (k, v) { data[k] = v }
    },
    runCommandSilent: function (c) { this.commands.push(c) },
    tell: function (t) { this.told.push(t.s) }
  }
  return server
}

function makePlayer(server, uuid) {
  var p = new PlayerClass()
  var mods = { max_health: [], movement_speed: [] }
  p.health = 20
  p.mods = mods
  p.messages = []
  p.given = []
  p.sneaking = false
  p.getServer = function () { return server }
  p.getStringUUID = function () { return uuid }
  p.getScoreboardName = function () { return 'Tester' }
  p.isShiftKeyDown = function () { return this.sneaking }
  p.getAttribute = function (a) {
    return {
      getModifiers: function () { return javaList(mods[a].slice()) },
      removeModifier: function (m) { mods[a] = mods[a].filter(function (x) { return x !== m }) },
      addTransientModifier: function (m) { mods[a].push(m) }
    }
  }
  p.getMaxHealth = function () { var h = 20; mods.max_health.forEach(function (m) { h += m.amount }); return h }
  p.getHealth = function () { return this.health }
  p.setHealth = function (h) { this.health = h }
  p.displayClientMessage = function (t) { this.messages.push(t.s) }
  p.tell = function (t) { this.messages.push(t.s) }
  p.give = function (i) { this.given.push(i) }
  return p
}

function stack(count, max) {
  return {
    count: count, max: max || 64,
    isEmpty: function () { return this.count <= 0 },
    getCount: function () { return this.count },
    getMaxStackSize: function () { return this.max },
    shrink: function (n) { this.count -= n },
    copyWithCount: function (n) { return stack(n, this.max) }
  }
}

// ---- Load the scripts in KubeJS order ----
var ROOT = 'kubejs/'
;['startup_scripts/00_fa_config.js', 'startup_scripts/10_fa_items.js', 'startup_scripts/20_fa_combat.js',
  'server_scripts/10_fa_ascension.js', 'server_scripts/20_fa_core_recipes.js', 'server_scripts/30_fa_material_recipes.js',
  'client_scripts/10_fa_tooltips.js'].forEach(function (f) {
  load(ROOT + f)
  print('loaded ' + f)
})

// ---- Items ----
print('\nitems')
var created = []
var builder = {}
;['displayName', 'rarity', 'glow', 'maxStackSize'].forEach(function (m) { builder[m] = function () { return builder } })
handlers.registry.forEach(function (fn) { fn({ create: function (id) { created.push(id); return builder } }) })
check('7 items registered', created.length == 7, created.join(','))

// ---- Recipes ----
print('\nrecipes')
var recipes = []
var ev = {
  custom: function (j) { var r = { json: j, id: function (i) { r.rid = i; return r } }; recipes.push(r); return r },
  shapeless: function (o, i) { var r = { json: { type: 'shapeless', out: o, in: i }, id: function (x) { r.rid = x; return r } }; recipes.push(r); return r }
}
handlers.recipes.forEach(function (fn) { fn(ev) })
function byId(id) { return recipes.filter(function (r) { return r.rid == 'kubejs:factory_ascension/' + id })[0] }
var core = byId('ascension_core')
check('core recipe exists', !!core)
check('core has 4 steps with TFMG steel', core && core.json.sequence.length == 4, core && core.json.sequence.length)
check('every recipe has an id', recipes.every(function (r) { return !!r.rid }))
var ids = recipes.map(function (r) { return r.rid })
check('recipe ids are unique', ids.filter(function (x, i) { return ids.indexOf(x) != i }).length == 0)
recipes.filter(function (r) { return r.json.type == 'create:crushing' }).forEach(function (r) {
  check(r.rid + ' has 1-4 results', r.json.results.length >= 1 && r.json.results.length <= 4, r.json.results.length)
})
var ex = byId('crush_exotic_mineral_cluster')
check('exotic crush picks mekanism lead (first match) and caps at 4',
  ex && ex.json.results.length == 4 && ex.json.results[1].item == 'mekanism:raw_lead', ex && JSON.stringify(ex.json.results))
var mixes = recipes.filter(function (r) { return r.json.type == 'create:mixing' })
check('mixing recipes have <= 9 ingredients', mixes.every(function (r) { return r.json.ingredients.length <= 9 }))

// ---- Levelling ----
print('\nascension')
var server = makeServer()
var p = makePlayer(server, 'uuid-1')
handlers.loggedIn[0]({ player: p })
check('fresh player is level 0', global.FA_LEVELS['uuid-1'] === 0)

var held = stack(64)
var cancelled = false
handlers.rightClicked['kubejs:ascension_core']({ player: p, item: held, cancel: function () { cancelled = true } })
check('single click uses 1 core', held.count == 63)
check('1 essence = level 1 (cost 1)', global.FA_LEVELS['uuid-1'] == 1)
check('event cancelled', cancelled)
check('health modifier applied (+2)', p.getMaxHealth() == 22, p.getMaxHealth())
check('healed on level up', p.health == 22)
check('title command sent', server.commands.some(function (c) { return c.indexOf('ASCENDED') > 0 }))

p.sneaking = true
handlers.rightClicked['kubejs:ascension_singularity']({ player: p, item: stack(10), cancel: function () {} })
var lvl = global.FA_LEVELS['uuid-1']
// Total essence 1 + 810; check level matches the cost curve
var spent = 0, expect = 0
while (spent + Math.ceil(Math.pow(1.12, expect)) <= 811) { spent += Math.ceil(Math.pow(1.12, expect)); expect++ }
check('sneak absorbs the whole stack and levels follow the cost curve', lvl == expect, lvl + ' vs ' + expect)
check('only one health modifier (no stacking)', p.mods.max_health.length == 1, p.mods.max_health.length)
check('speed capped at +50%', p.mods.movement_speed[0].amount <= 0.5, p.mods.movement_speed[0].amount)
check('server announcement every 10 levels', server.told.length == 1, server.told.length)

handlers.respawned[0]({ player: p })
check('respawn re-applies stats', p.mods.max_health.length == 1 && p.health == p.getMaxHealth())
handlers.loggedOut[0]({ player: p })
check('logout clears cached level', global.FA_LEVELS['uuid-1'] === undefined)
handlers.loggedIn[0]({ player: p })
check('level persists across relog', global.FA_LEVELS['uuid-1'] == lvl)

// Failure path: items are returned if absorbing throws
var broken = makePlayer({ persistentData: { getInt: function () { throw 'disk on fire' } } }, 'uuid-2')
handlers.rightClicked['kubejs:ascension_core']({ player: broken, item: stack(5), cancel: function () {} })
check('items are returned when absorbing fails', broken.given.length == 1 && broken.given[0].count == 1)

// ---- Combat ----
print('\ncombat')
var hurt = handlers.forge['LivingHurtEvent']
function hurtEvent(attacker, victim, amount, bypass) {
  return {
    amount: amount,
    getSource: function () { return { getEntity: function () { return attacker }, is: function () { return !!bypass } } },
    getEntity: function () { return victim },
    getAmount: function () { return this.amount },
    setAmount: function (a) { this.amount = a }
  }
}
var mob = {}
var e1 = hurtEvent(p, mob, 10)
hurt(e1)
check('player damage scaled by level', Math.abs(e1.amount - 10 * (1 + 0.15 * lvl)) < 1e-6, e1.amount)
var e2 = hurtEvent(mob, p, 10)
hurt(e2)
check('damage taken reduced', Math.abs(e2.amount - 10 / (1 + 0.05 * lvl)) < 1e-6, e2.amount)
var e3 = hurtEvent(mob, p, 10, true)
hurt(e3)
check('/kill and void are not reduced', e3.amount == 10)
var e4 = hurtEvent(mob, mob, 10)
hurt(e4)
check('mob vs mob untouched', e4.amount == 10)

print('\nloot')
var dropsHook = handlers.forge['LivingDropsEvent']
function dropEvent(attacker, victim, stacks) {
  var ents = stacks.map(function (s) {
    var ie = { s: s, getItem: function () { return this.s }, setItem: function (n) { this.s = n },
      level: function () { return null }, getX: function () { return 0 }, getY: function () { return 0 }, getZ: function () { return 0 } }
    return ie
  })
  var list = javaList(ents)
  return { list: list, getEntity: function () { return victim }, getSource: function () { return { getEntity: function () { return attacker } } }, getDrops: function () { return list } }
}
function total(ev) { var t = 0; ev.list.arr.forEach(function (ie) { t += ie.stack ? ie.stack.count : ie.s.count }); return t }
var d1 = dropEvent(p, mob, [stack(3)])
dropsHook(d1)
var mult = 1 + 0.05 * lvl
check('drops multiplied', total(d1) >= Math.floor(3 * mult) && total(d1) <= Math.ceil(3 * mult), total(d1) + ' for x' + mult)
var d2 = dropEvent(p, mob, [stack(1, 1)])
dropsHook(d2)
check('unstackables become extra entities', d2.list.arr.length > 1, d2.list.arr.length)
check('extra stacks capped at 8', d2.list.arr.length <= 9)
var d3 = dropEvent(p, makePlayer(server, 'victim'), [stack(64)])
dropsHook(d3)
check('player death drops untouched', total(d3) == 64 && d3.list.arr.length == 1)

// ---- Tooltips & command ----
print('\nclient + command')
var tips = 0
handlers.tooltip.forEach(function (fn) { fn({ add: function () { tips++ } }) })
check('tooltips registered', tips == 7, tips)
check('command registered', handlers.commands.length == 1)
p.messages = []
var r = faShowStatus(p)
check('/ascension prints status', r == 1 && p.messages.length == 5, p.messages.length)
print('  sample: ' + p.messages[0] + ' | ' + p.messages[1])
check('number formatting', faFmt(1234) == '1234' && faFmt(12345) == '12.3K' && faFmt(2.5e9) == '2.50B' && faFmt(1e40).indexOf('e+') > 0,
  [faFmt(12345), faFmt(2.5e9), faFmt(1e40)].join(' '))

print('\n' + (failures ? failures + ' FAILED' : 'all checks passed'))
if (failures) java.lang.System.exit(1)
