/* Gear Builder page. All the math is in calc.js (shared with the tests); this file is just the page. */
(function () {
  "use strict";
  const FA = global.FA, E = FA.enchants, ids = Object.keys(E);
  const $ = s => document.querySelector(s);
  const esc = s => String(s).replace(/[&<>"]/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[c]);
  const num = n => global.FA_num(n);
  const SLOT_NAMES = { main: "Main hand", off: "Off hand", head: "Helmet", chest: "Chestplate", legs: "Leggings", feet: "Boots" };
  const GROUPS = [
    ["Damage", ["damage", "critical", "reaper", "cleave"]], ["On hit", ["lifesteal", "hit_effect", "ignite", "hit_heal", "self_effect"]],
    ["On kill", ["plunder", "kill_heal", "xp", "drop"]], ["Defense", ["resist", "vitality", "retribution", "dodge", "undying"]],
    ["Passive", ["regen", "passive", "magnet", "repair"]], ["Utility", ["attribute", "mining", "eternal"]]
  ];
  const WHEN = { undead: "vs undead", arthropod: "vs arthropods", boss: "vs bosses", nether: "in the Nether", raider: "vs raiders", wet: "vs wet targets",
    airborne: "vs airborne", full_health: "vs full-health targets", low_health: "vs targets under 30%", berserk: "while under 50% health",
    projectile: "with projectiles", sneaking: "while sneaking" };
  const RAR = { gray: "#b9b3ad", aqua: "#4fe3e8", light_purple: "#d48cff", gold: "#ffb84d", red: "#ff4d4d" };
  const MAX_LEVEL = 2147483647;

  const blank = () => ({ slots: {} });
  let build = blank();
  const item = s => build.slots[s] || (build.slots[s] = { e: {}, stars: 0, gems: [], rolled: [] });

  const PRESETS = {
    "Starter": { slots: { main: { e: { sharpness: 25, lifesteal: 5 } }, chest: { e: { protection: 25, vitality: 10 } } } },
    "Protection 3000": { slots: { head: { e: { protection: 750 } }, chest: { e: { protection: 750, vitality: 200 } }, legs: { e: { protection: 750 } }, feet: { e: { protection: 750, swiftness: 50 } }, main: { e: { sharpness: 300 } } } },
    "Glass cannon": { slots: { main: { e: { sharpness: 2000, critical: 300, executioner: 500 }, gems: [["bloodstone", 3], ["bloodstone", 3], ["bloodstone", 3]], stars: 2 }, off: { e: { berserk: 800, rampage: 50 } } } },
    "Mythic chaos": { slots: { main: { e: { thunderlord: 40, black_hole: 20, overkill: 30 }, rolled: [[88930115, 120], [1337, 400]] }, chest: { e: { phoenix: 5, chronos: 10, protection: 200 } }, feet: { e: { wings: 3, gravity_slam: 50 } } } }
  };

  // ---------- share links: the build lives in the URL hash ----------
  const encode = b => btoa(unescape(encodeURIComponent(JSON.stringify(b)))).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
  function decode(h) {
    try {
      const b = JSON.parse(decodeURIComponent(escape(atob(h.replace(/-/g, "+").replace(/_/g, "/")))));
      return b && typeof b.slots == "object" ? clean(b) : null;
    } catch (e) { return null; }
  }
  // Only keep known slots/enchants/gems and sane numbers (links can be edited by hand)
  function clean(b) {
    const out = blank(), int = (v, max) => Math.max(0, Math.min(max, Math.floor(+v) || 0));
    FA_CALC.SLOTS.forEach(s => {
      const it = b.slots[s];
      if (!it) return;
      const c = { e: {}, stars: int(it.stars, 1000), gems: [], rolled: [] };
      Object.keys(it.e || {}).forEach(id => {
        if (!E[id]) return;
        const v = int(it.e[id], E[id].maxLevel || MAX_LEVEL);
        if (v > 0) c.e[id] = v;
      });
      (it.gems || []).slice(0, FA.gems.sockets).forEach(g => { if (FA.gems.types[g[0]] && g[1] >= 1 && g[1] <= 3) c.gems.push([g[0], int(g[1], 3)]); });
      (it.rolled || []).slice(0, FA.chaos.maxPerItem).forEach(r => { const sd = int(r[0], 2147483646), lv = int(r[1], MAX_LEVEL); if (sd > 0 && lv > 0) c.rolled.push([sd, lv]); });
      out.slots[s] = c;
    });
    return out;
  }
  const save = () => history.replaceState(null, "", "#b=" + encode(build));

  // ---------- slots ----------
  const options = (() => {
    let html = '<option value="">+ Add enchant…</option>';
    GROUPS.forEach(g => {
      html += '<optgroup label="' + g[0] + '">' + ids.filter(id => !E[id].mythic && !E[id].ultimate && g[1].includes(E[id].type)).map(id => '<option value="' + id + '">' + esc(E[id].name) + "</option>").join("") + "</optgroup>";
    });
    html += '<optgroup label="Mythic">' + ids.filter(id => E[id].mythic).map(id => '<option value="' + id + '">✦ ' + esc(E[id].name) + "</option>").join("") + "</optgroup>";
    html += '<optgroup label="Ultimate">' + ids.filter(id => E[id].ultimate).map(id => '<option value="' + id + '">☄ ' + esc(E[id].name) + "</option>").join("") + "</optgroup>";
    html += '<optgroup label="Fusion">' + ids.filter(id => E[id].type == "fusion").map(id => '<option value="' + id + '">⚗ ' + esc(E[id].name) + " (" +
      esc(E[E[id].parts[0]].name) + " + " + esc(E[E[id].parts[1]].name) + ")</option>").join("") + "</optgroup>";
    return html;
  })();
  const gemOptions = sel => '<option value="">empty socket</option>' + Object.keys(FA.gems.types).map(t =>
    [1, 2, 3].map(tier => '<option value="' + t + ":" + tier + '"' + (sel == t + ":" + tier ? " selected" : "") + ">" + FA.gems.tiers[tier - 1].name + " " + FA.gems.types[t].name + "</option>").join("")).join("");

  function renderSlots() {
    $("#slots").innerHTML = FA_CALC.SLOTS.map(s => {
      const it = build.slots[s] || { e: {}, stars: 0, gems: [], rolled: [] };
      const rows = Object.keys(it.e).map(id =>
        '<div class="row"><img src="fa/items/tome_' + id + '.png" alt="" /><span class="nm' + (E[id].mythic ? " myth" : "") + '">' + esc(E[id].name) + '</span>' +
        '<input class="uv-input lv" type="number" min="1" max="' + (E[id].maxLevel || MAX_LEVEL) + '" value="' + it.e[id] + '" data-s="' + s + '" data-e="' + id + '" aria-label="' + esc(E[id].name) + ' level" />' +
        '<button class="x" data-s="' + s + '" data-rm="' + id + '" aria-label="Remove">×</button></div>').join("");
      const gems = [0, 1, 2].slice(0, FA.gems.sockets).map(i => {
        const g = it.gems[i];
        return '<select class="uv-select gem" data-s="' + s + '" data-g="' + i + '" aria-label="Gem socket ' + (i + 1) + '">' + gemOptions(g ? g[0] + ":" + g[1] : "") + "</select>";
      }).join("");
      const rolled = it.rolled.map((r, i) => {
        const roll = global.FA_rollEnchant(r[0]), R = FA.chaos.rarities[roll.rarity];
        return '<div class="row roll" style="--rc:' + RAR[R.color] + '"><span class="nm" title="seed ' + r[0] + '">❂ ' + esc(roll.name) + " <small>" + R.name + "</small></span>" +
          '<input class="uv-input lv" type="number" min="1" value="' + r[1] + '" data-s="' + s + '" data-r="' + i + '" aria-label="Rolled enchant level" />' +
          '<button class="x" data-s="' + s + '" data-rmr="' + i + '" aria-label="Remove">×</button></div>';
      }).join("");
      return '<article class="slot uv-card" data-slot="' + s + '"><header><h2>' + SLOT_NAMES[s] + '</h2>' +
        '<span class="stars"><button data-s="' + s + '" data-star="-1" aria-label="Remove star">−</button><b>' + (it.stars ? "★".repeat(Math.min(it.stars, 5)) + (it.stars > 5 ? "×" + it.stars : "") : "no stars") + '</b><button data-s="' + s + '" data-star="1" aria-label="Add star">+</button></span></header>' +
        rows + '<select class="uv-select add" data-s="' + s + '" aria-label="Add enchant">' + options + "</select>" +
        '<div class="gems">' + gems + "</div>" + rolled +
        (it.rolled.length < FA.chaos.maxPerItem ? '<button class="uv-btn uv-btn--sm uv-btn--ghost dice" data-s="' + s + '">🎲 Add a random Chaos roll</button>' : "") +
        "</article>";
    }).join("");
  }

  // ---------- totals ----------
  function renderTotals() {
    const lv = FA_CALC.levels(build), sm = FA_CALC.summary(lv), cost = FA_CALC.cost(build);
    const x = n => "x" + (n < 100 ? n.toFixed(2) : num(n));
    // Near 100% keeps decimals, so -99.99% doesn't read as an impossible -100%
    const pct = n => { const v = n * 100; return (v < 10 || v >= 99 ? (v >= 99.995 ? Math.floor(v * 1000) / 1000 : v.toFixed(2)) : num(v)) + "%"; };
    const big = (label, value, note) => '<div class="big"><small>' + label + "</small><b>" + value + "</b>" + (note ? "<em>" + note + "</em>" : "") + "</div>";
    let html = '<p class="mono">Totals</p><div class="bigs">' +
      big("Damage", x(sm.damage), sm.critFactor > 1.001 ? "x" + sm.critFactor.toFixed(2) + " more on average from crits" : "on every hit") +
      big("Damage taken", sm.taken < 1 ? "-" + pct(1 - sm.taken) : "normal", "from everything except /kill and the void") +
      big("Effective health", num(Math.round(sm.effectiveHealth)), "your heart bar still shows 10") +
      big("Mob loot", x(sm.loot)) + big("Mob XP", x(sm.xp)) + big("Mining", x(sm.mining)) +
      "</div>" + '<div class="cost"><small>Essence to build all of this</small><b>' + num(cost) + "</b></div>";
    if (lv.__sets.length) html += '<div class="sets">' + lv.__sets.map(st => '<span class="uv-chip is-active">⦿ ' + st.name + " (" + st.pieces + " pieces)</span>").join("") + "</div>";
    if (sm.conditional.length) html += '<h3>Situational damage</h3><ul class="kv">' + sm.conditional.map(c => "<li><span>" + WHEN[c.when] + "</span><b>" + x(c.mult) + "</b></li>").join("") + "</ul>";
    const wards = Object.keys(sm.wards);
    if (wards.length) html += '<h3>Extra protection</h3><ul class="kv">' + wards.map(w => "<li><span>" + w + " damage</span><b>-" + pct(1 - sm.wards[w] * sm.taken) + "</b></li>").join("") + "</ul>";
    html += '<h3>Everything on you</h3><ul class="all">' + (sm.lines.length ? sm.lines.sort((a, b) => (a.rolled - b.rolled) || (b.mythic - a.mythic)).map(l =>
      '<li class="' + (l.mythic ? "myth" : l.rolled ? "rolled" : "") + '"><b>' + esc(l.name || "Rolled effect") + " " + num(l.level) + "</b><span>" + esc(l.text) + "</span></li>").join("") :
      '<li class="dim">Nothing yet. Add an enchant to a slot, or start from a preset.</li>') + "</ul>";
    $("#totals").innerHTML = html;
  }

  const update = () => { renderTotals(); save(); };
  const full = () => { renderSlots(); update(); };

  // ---------- events ----------
  $("#slots").addEventListener("change", e => {
    const t = e.target, s = t.dataset.s;
    if (t.classList.contains("add") && t.value) {
      if (!item(s).e[t.value]) item(s).e[t.value] = 1;
      full();
    } else if (t.classList.contains("gem")) {
      const g = item(s).gems, i = +t.dataset.g;
      if (t.value) { const p = t.value.split(":"); g[i] = [p[0], +p[1]]; } else g[i] = null;
      item(s).gems = g.filter(Boolean);
      full();
    }
  });
  $("#slots").addEventListener("input", e => {
    const t = e.target, s = t.dataset.s;
    if (!t.classList.contains("lv")) return;
    const max = +t.max || MAX_LEVEL, v = Math.max(1, Math.min(max, Math.floor(+t.value) || 1));
    if (t.dataset.e) item(s).e[t.dataset.e] = v;
    else if (t.dataset.r != null) item(s).rolled[+t.dataset.r][1] = v;
    update();
  });
  $("#slots").addEventListener("click", e => {
    const b = e.target.closest("button");
    if (!b) return;
    const s = b.dataset.s;
    if (b.dataset.rm) delete item(s).e[b.dataset.rm];
    else if (b.dataset.rmr != null) item(s).rolled.splice(+b.dataset.rmr, 1);
    else if (b.dataset.star) item(s).stars = Math.max(0, Math.min(1000, item(s).stars + +b.dataset.star));
    else if (b.classList.contains("dice")) item(s).rolled.push([1 + Math.floor(Math.random() * 2147483645), 1]);
    else return;
    full();
  });
  $("#presets").innerHTML = Object.keys(PRESETS).map(p => '<button class="uv-btn uv-btn--sm uv-btn--ghost" data-p="' + p + '">' + p + "</button>").join("");
  $("#presets").addEventListener("click", e => { const b = e.target.closest("button"); if (b) { build = clean(JSON.parse(JSON.stringify(PRESETS[b.dataset.p]))); full(); } });
  $("#reset").addEventListener("click", () => { build = blank(); full(); });
  $("#share").addEventListener("click", () => {
    save();
    if (window.UV && UV.copy) UV.copy(location.href, "Share link copied");
    else navigator.clipboard && navigator.clipboard.writeText(location.href);
  });

  // A share link pasted into this same tab only changes the hash, so load it from there too
  addEventListener("hashchange", () => {
    const m = /^#b=(.+)$/.exec(location.hash), b = m && decode(m[1]);
    if (b) { build = b; full(); }
  });
  const fromHash = /^#b=(.+)$/.exec(location.hash);
  build = fromHash && decode(fromHash[1]) || clean(JSON.parse(JSON.stringify(PRESETS["Starter"])));
  full();
})();
