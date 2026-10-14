/* Factory Ascension - gear math for the website (no page code in here).
   It mirrors how the game reads gear (faItemLevels / faLevels / sets in 20_fa_enchant_effects.js) using the
   real config, and the test harness checks it against the game engine on random builds, so they can't drift.

   A build: { slots: { main: { e: { sharpness: 100 }, stars: 0, gems: [['bloodstone', 3]], rolled: [[seed, level]] }, ... } } */
var FA_CALC = (function () {
  "use strict";
  var SLOTS = ["main", "off", "head", "chest", "legs", "feet"];
  var MAX = 2000000000;

  function cfg() { return global.FA; }

  // The definition behind an id: a config enchant, or one effect of a rolled enchant ("r<seed>_<i>")
  function def(id) {
    var e = cfg().enchants[id];
    if (e) return e;
    var m = /^r(\d+)_(\d+)$/.exec(id);
    if (!m) return null;
    return global.FA_rollEnchant(+m[1]).affixes[+m[2]] || null;
  }

  function itemLevels(item) {
    var own = {};
    var add = function (id, v) { if (v > 0) own[id] = Math.min(MAX, (own[id] || 0) + v); };
    var stars = item.stars > 0 ? item.stars : 0;
    var mult = global.FA_starMult(stars);
    var e = item.e || {};
    Object.keys(e).forEach(function (id) { if (cfg().enchants[id] && e[id] > 0) add(id, Math.floor(e[id] * mult)); });
    (item.gems || []).slice(0, cfg().gems.sockets).forEach(function (g) {
      var type = cfg().gems.types[g[0]];
      if (type && g[1] >= 1 && g[1] <= 3) add(type.enchant, Math.floor(global.FA_gemLevels(g[1]) * mult));
    });
    (item.rolled || []).slice(0, cfg().chaos.maxPerItem).forEach(function (r) {
      if (!(r[1] > 0)) return;
      var roll = global.FA_rollEnchant(r[0]);
      roll.affixes.forEach(function (a, i) { add("r" + roll.seed + "_" + i, Math.floor(r[1] * mult)); });
    });
    return own;
  }

  // Total levels for a build, with gear-set bonuses (same rules as the game)
  function levels(build) {
    var total = {}, pieces = {};
    SLOTS.forEach(function (s) {
      var item = build.slots && build.slots[s];
      if (!item) return;
      var own = itemLevels(item);
      Object.keys(own).forEach(function (id) {
        total[id] = Math.min(MAX, (total[id] || 0) + own[id]);
        pieces[id] = (pieces[id] || 0) + 1;
      });
    });
    var sets = [];
    cfg().sets.forEach(function (set) {
      var n = pieces[set.key] || 0;
      var bonus = n >= 4 ? set[4] : n >= 2 ? set[2] : null;
      if (!bonus) return;
      total[set.key] = Math.min(MAX, Math.floor(total[set.key] * bonus.mult));
      Object.keys(bonus.add || {}).forEach(function (id) { total[id] = Math.min(MAX, (total[id] || 0) + bonus.add[id]); });
      sets.push({ name: set.name, pieces: n >= 4 ? 4 : 2 });
    });
    total.__sets = sets;
    return total;
  }

  // Headline numbers, computed the way the combat hooks do it
  function summary(lv) {
    var out = { damage: 1, critFactor: 1, taken: 1, loot: 1, xp: 1, mining: 1, conditional: [], wards: {}, lines: [] };
    Object.keys(lv).forEach(function (id) {
      if (id.indexOf("__") === 0) return;
      var d = def(id), L = lv[id];
      if (!d || !(L > 0)) return;
      if (d.type == "damage") {
        if (d.when == "always") out.damage *= 1 + d.perLevel * L;
        else out.conditional.push({ when: d.when, mult: 1 + d.perLevel * L });
      } else if (d.type == "critical") {
        out.critFactor *= 1 + global.FA_chance(L) * (d.critMultiplier - 1);
      } else if (d.type == "resist") {
        if (d.vs == "all") out.taken /= 1 + d.perLevel * L;
        else out.wards[d.vs] = (out.wards[d.vs] || 1) / (1 + d.perLevel * L);
      } else if (d.type == "vitality") {
        out.taken *= 20 / (20 + d.perLevel * L);
      } else if (d.type == "plunder") {
        out.loot += d.perLevel * L;
      } else if (d.type == "xp") {
        out.xp += d.perLevel * L;
      } else if (d.type == "mining") {
        out.mining *= 1 + d.perLevel * L;
      }
      out.lines.push({ id: id, name: cfg().enchants[id] ? cfg().enchants[id].name : null, level: L, text: global.FA_describeDef(d, L), mythic: !!d.mythic, rolled: !cfg().enchants[id] });
    });
    out.effectiveHealth = 20 / out.taken;
    return out;
  }

  // Essence it costs to build every item (Rebirth stars and gems aren't paid in Essence, so they're not included)
  function cost(build) {
    var total = 0;
    SLOTS.forEach(function (s) {
      var item = build.slots && build.slots[s];
      if (!item) return;
      Object.keys(item.e || {}).forEach(function (id) {
        if (!cfg().enchants[id]) return;
        var L = Math.min(item.e[id], 1000000);
        for (var k = 0; k < L; k++) total += global.FA_cost(k, id);
      });
      (item.rolled || []).forEach(function (r) {
        var rarity = global.FA_rollEnchant(r[0]).rarity, L = Math.min(r[1], 1000000);
        if (L > 0) total += global.FA_rollCost(0, 0);
        for (var k = 1; k < L; k++) total += global.FA_rollCost(k, rarity);
      });
    });
    return total;
  }

  return { SLOTS: SLOTS, def: def, itemLevels: itemLevels, levels: levels, summary: summary, cost: cost };
})();
