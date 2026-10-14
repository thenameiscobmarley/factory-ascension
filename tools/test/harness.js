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
  put: function (k, v) { this.d[k] = v },
  getAllKeys: function () { return Object.keys(this.d) }
}

function PlayerClass() {}
function ItemEntityClass(level, x, y, z, stack) { this.s = stack; this.setDefaultPickUpDelay = function () {} }
ItemEntityClass.prototype.getItem = function () { return this.s }
var Java = {
  loadClass: function (n) {
    var classes = {
      'net.minecraft.world.entity.player.Player': PlayerClass,
      'net.minecraft.world.entity.item.ItemEntity': ItemEntityClass,
      'net.minecraft.tags.DamageTypeTags': { BYPASSES_INVULNERABILITY: 'bypass' },
      'net.minecraft.world.entity.ai.attributes.Attributes': { MOVEMENT_SPEED: 'movement_speed' },
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
    copyWithCount: function (n) { var c = stack(n, this.max, this.name); c.tag = this.tag; return c },
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
  var speed = []
  p.main = stack(0); p.off = stack(0); p.armor = [stack(0), stack(0), stack(0), stack(0)]
  p.health = 20; p.healed = 0; p.messages = []; p.bar = []; p.given = []; p.sneaking = false; p.speed = speed
  p.getServer = function () { return server }
  p.getStringUUID = function () { return uuid }
  p.getScoreboardName = function () { return 'Tester' }
  p.isShiftKeyDown = function () { return this.sneaking }
  p.getMainHandItem = function () { return this.main }
  p.getOffhandItem = function () { return this.off }
  p.getAllSlots = function () { return javaList([this.main, this.off].concat(this.armor)) }
  p.getAttribute = function () {
    return {
      getModifiers: function () { return javaList(speed.slice()) },
      removeModifier: function (m) { var i = speed.indexOf(m); if (i >= 0) speed.splice(i, 1) },
      addTransientModifier: function (m) { speed.push(m) }
    }
  }
  p.isAlive = function () { return true }
  p.heal = function (h) { this.healed += h }
  p.hurtBy = []
  p.hurt = function (src, amount) { this.hurtBy.push(amount); return true }
  p.damageSources = function () { return { thorns: function (e) { return 'thorns' } } }
  p.displayClientMessage = function (t) { this.bar.push(t.s) }
  p.tell = function (t) { this.messages.push(t) }
  p.give = function (i) { this.given.push(i) }
  return p
}
function makeMob() {
  return { hurtBy: [], isAlive: function () { return true }, hurt: function (s, a) { this.hurtBy.push(a); return true }, getAllSlots: function () { return javaList([]) } }
}

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
check('items registered (4 core + blank + 9 tomes + 3 clusters)', created.length == 17, created.length)
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
print('\neffects')
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
var fighter = makePlayer(server, 'f')
var blade = gear('Blade'); faSetLevel(blade, 'sharpness', 100); faSetLevel(blade, 'lifesteal', 10)
var plate = gear('Plate'); faSetLevel(plate, 'protection', 3000); faSetLevel(plate, 'vitality', 50); faSetLevel(plate, 'retribution', 4)
fighter.main = blade; fighter.armor[1] = plate
var mob = makeMob()

var e1 = hurtEvent(fighter, mob, 10); hurt(e1)
check('Sharpness 100 = x11 damage', near(e1.amount, 110), e1.amount)
check('Lifesteal 10 heals 10% of damage dealt', near(fighter.healed, 11), fighter.healed)
var e2 = hurtEvent(mob, fighter, 100); hurt(e2)
var expected = 100 / (1 + 0.04 * 3000) * 20 / (20 + 100)
check('Protection 3000 + Vitality 50 stack', near(e2.amount, expected), e2.amount + ' vs ' + expected)
check('Retribution reflects 20% of the raw hit', near(mob.hurtBy[0], 20), mob.hurtBy[0])
var e3 = hurtEvent(mob, fighter, 100, true); hurt(e3)
check('/kill and void ignore defenses', e3.amount == 100)
var e4 = hurtEvent(mob, mob, 10); hurt(e4)
check('mob vs mob untouched', e4.amount == 10)
// Two players with Retribution must not loop forever
var twin = makePlayer(server, 't'); twin.armor[1] = plate
twin.hurt = function (src, a) { hurt(hurtEvent(fighter, twin, a)); return true }
fighter.hurt = function (src, a) { hurt(hurtEvent(twin, fighter, a)); return true }
hurt(hurtEvent(twin, fighter, 10))
check('Retribution between two players does not recurse forever', true)

print('\nplunder')
var dropsHook = handlers.forge['LivingDropsEvent']
function dropEvent(attacker, victim, stacks) {
  var ents = stacks.map(function (s) {
    return { s: s, getItem: function () { return this.s }, setItem: function (n) { this.s = n },
      level: function () { return null }, getX: function () { return 0 }, getY: function () { return 0 }, getZ: function () { return 0 } }
  })
  var list = javaList(ents)
  return { list: list, getEntity: function () { return victim }, getSource: function () { return { getEntity: function () { return attacker } } }, getDrops: function () { return list } }
}
function totalCount(ev) { var t = 0; ev.list.arr.forEach(function (ie) { t += ie.getItem().count }); return t }
var looter = makePlayer(server, 'l'); var charm = gear('Charm'); faSetLevel(charm, 'plunder', 20); looter.off = charm
var d1 = dropEvent(looter, mob, [stack(3)]); dropsHook(d1)
check('Plunder 20 doubles drops', totalCount(d1) == 6, totalCount(d1))
var d2 = dropEvent(looter, mob, [stack(1, 1)]); dropsHook(d2)
check('unstackables become extra entities', d2.list.arr.length == 2, d2.list.arr.length)
faSetLevel(charm, 'plunder', 100000)
var d3 = dropEvent(looter, mob, [stack(1, 1)]); dropsHook(d3)
check('extra stacks capped at 8', d3.list.arr.length == 9, d3.list.arr.length)
var d4 = dropEvent(looter, makePlayer(server, 'v'), [stack(64)]); dropsHook(d4)
check('player death drops untouched', totalCount(d4) == 64 && d4.list.arr.length == 1)

print('\nswiftness + excavation')
var equip = handlers.forge['LivingEquipmentChangeEvent']
var runner = makePlayer(server, 'r'); var boots = gear('Boots'); faSetLevel(boots, 'swiftness', 30); runner.armor[3] = boots
equip({ getEntity: function () { return runner } })
check('Swiftness 30 = +30% speed', runner.speed.length == 1 && near(runner.speed[0].amount, 0.3))
equip({ getEntity: function () { return runner } })
check('no duplicate speed modifiers', runner.speed.length == 1)
faSetLevel(boots, 'swiftness', 500)
equip({ getEntity: function () { return runner } })
check('speed capped at +50%', runner.speed.length == 1 && near(runner.speed[0].amount, 0.5))
runner.armor[3] = stack(0)
equip({ getEntity: function () { return runner } })
check('removing the boots removes the bonus', runner.speed.length == 0)
var digger = makePlayer(server, 'd'); var pick = gear('Pick'); faSetLevel(pick, 'excavation', 4); digger.main = pick
var bs = { speed: 8, getEntity: function () { return digger }, getNewSpeed: function () { return this.speed }, setNewSpeed: function (s) { this.speed = s } }
handlers.forge['BreakSpeed'](bs)
check('Excavation 4 = x2 mining speed', near(bs.speed, 16), bs.speed)

// ---------------- Tooltips ----------------
print('\ntooltips')
var advanced = null, simple = 0
handlers.tooltip.forEach(function (fn) { fn({ add: function () { simple++ }, addAdvanced: function (ing, f) { advanced = f } }) })
check('tooltips registered for cores, tomes and clusters', simple == 3 + 1 + 9 + 4, simple)
var lines = { arr: ['Netherite Chestplate', 'When on body:'], add: function (i, t) { this.arr.splice(i, 0, t.s) } }
advanced(chest, false, lines)
check('enchants show right under the item name', /Ascended Protection 3,000/.test(lines.arr[1]), lines.arr.join(' | '))
print('  sample: ' + lines.arr[1])
var plain = { arr: ['Stick'], add: function () { throw 'should not add' } }
advanced(stack(1), false, plain)
check('items without enchants are untouched', plain.arr.length == 1)

print('\nformat')
check('numbers', global.FA_num(1234) == '1,234' && global.FA_num(2.5e9) == '2.50B' && global.FA_num(1e40).indexOf('e+') > 0,
  [global.FA_num(1234), global.FA_num(2.5e9), global.FA_num(1e40)].join(' '))
Object.keys(E).forEach(function (id) { print('  ' + E[id].name + ' 3000: ' + global.FA_describe(id, 3000)) })

print('\n' + (failures ? failures + ' FAILED' : 'all checks passed'))
if (failures) java.lang.System.exit(1)
