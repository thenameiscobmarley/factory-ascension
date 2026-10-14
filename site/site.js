/* Factory Ascension site logic. Every number comes from the pack's real config (fa/00_fa_config.js),
   which this page runs directly, so the site always matches the game. Uses UI Vault (window.UV). */
(function () {
  "use strict";
  const FA = global.FA;
  const E = FA.enchants;
  const ids = Object.keys(E);
  const $ = s => document.querySelector(s);
  const reduce = matchMedia("(prefers-reduced-motion: reduce)").matches;
  const esc = s => String(s).replace(/[&<>"]/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[c]);
  const tex = id => "fa/items/" + id + ".png";

  // ---------- Lite mode ----------
  // Time the first second of frames; if this machine can't hold ~55 fps with the decorations on, turn them off.
  // The footer button overrides it either way, and the choice is remembered.
  const LITE_KEY = "fa-lite";
  const store = { get: () => { try { return localStorage.getItem(LITE_KEY); } catch (e) { return null; } },
    set: v => { try { localStorage.setItem(LITE_KEY, v); } catch (e) {} } };
  const setLite = on => {
    document.documentElement.classList.toggle("lite", on);
    const b = document.getElementById("fxToggle");
    if (b) b.textContent = on ? "Effects: lite (faster)" : "Effects: full";
  };
  const saved = store.get();
  setLite(saved === "1" || (saved === null && reduce));
  if (saved === null && !reduce) {
    // Measure while actually scrolling (an idle page is always fast): ~60 frames of the first scroll
    const times = [];
    let last = 0, scrolling = 0, running = false;
    const probe = now => {
      if (now - scrolling > 150) { running = false; last = 0; return; }   // paused scrolling: wait for the next scroll
      if (last) times.push(now - last);
      last = now;
      if (times.length < 60) return requestAnimationFrame(probe);
      removeEventListener("scroll", onFirstScrolls);
      times.sort((a, b) => a - b);
      if (times[30] > 20) setLite(true);
    };
    const onFirstScrolls = () => {
      scrolling = performance.now();
      if (!running) { running = true; requestAnimationFrame(probe); }
    };
    addEventListener("scroll", onFirstScrolls, { passive: true });
  }
  document.addEventListener("click", e => {
    if (e.target.id != "fxToggle") return;
    const on = !document.documentElement.classList.contains("lite");
    setLite(on);
    store.set(on ? "1" : "0");
  });

  // ---------- gear shape for the hero ----------
  (function gear() {
    const teeth = 12, r1 = 46, r2 = 38, hole = 15;
    let d = "";
    for (let i = 0; i < teeth * 4; i++) {
      const a = (i / (teeth * 4)) * Math.PI * 2, r = i % 4 < 2 ? r1 : r2;
      d += (i ? "L" : "M") + (Math.cos(a) * r).toFixed(2) + " " + (Math.sin(a) * r).toFixed(2);
    }
    d += "Z M" + hole + " 0 A" + hole + " " + hole + " 0 1 0 " + -hole + " 0 A" + hole + " " + hole + " 0 1 0 " + hole + " 0Z";
    document.querySelectorAll(".gear").forEach(g => { g.innerHTML = '<path fill-rule="evenodd" d="' + d + '"/>'; });
  })();

  // ---------- embers (small canvas, pauses when off screen) ----------
  (function embers() {
    const c = $(".embers");
    if (!c || reduce) return;
    const ctx = c.getContext("2d");
    let w = 0, h = 0, on = true, parts = [];
    const size = () => { w = c.width = c.clientWidth; h = c.height = c.clientHeight; };
    const spawn = y => ({ x: Math.random() * w, y: y == null ? h + 10 : y, v: 0.3 + Math.random() * 0.9, r: 0.6 + Math.random() * 1.8, s: Math.random() * 6, life: 0.5 + Math.random() * 0.5 });
    size();
    addEventListener("resize", size);
    for (let i = 0; i < 46; i++) parts.push(spawn(Math.random() * h));
    new IntersectionObserver(e => { on = e[0].isIntersecting; if (on) requestAnimationFrame(tick); }).observe(c);
    function tick(t) {
      if (!on || document.hidden) return;
      ctx.clearRect(0, 0, w, h);
      for (const p of parts) {
        p.y -= p.v; p.x += Math.sin(t / 900 + p.s) * 0.35;
        if (p.y < -10) Object.assign(p, spawn());
        const a = Math.min(1, p.y / h) * p.life;
        ctx.fillStyle = "rgba(255," + (120 + (p.s * 20 | 0)) + ",40," + a.toFixed(3) + ")";
        ctx.beginPath(); ctx.arc(p.x, p.y, p.r, 0, 7); ctx.fill();
      }
      requestAnimationFrame(tick);
    }
    document.addEventListener("visibilitychange", () => { if (!document.hidden && on) requestAnimationFrame(tick); });
    requestAnimationFrame(tick);
  })();

  // ---------- Liquid Essence color + the density ladder (all from the config) ----------
  const essenceHex = "#" + ("000000" + FA.altar.color.toString(16)).slice(-6);
  document.documentElement.style.setProperty("--essence", essenceHex);
  const tiers = global.FA_condensedTiers();
  $("#ladderTop").textContent = global.FA_condensedName(tiers);
  $("#ladder").innerHTML = Array.from({ length: tiers + 1 }, (_, t) => '<img src="' + tex("condensed_essence_" + t) + '" alt="' + esc(global.FA_condensedName(t)) +
    '" title="' + esc(global.FA_condensedName(t)) + '" tabindex="0" data-t="' + t + '" loading="lazy" />').join("");
  const ladderInfo = t => {
    const buckets = Math.pow(FA.condensed.ratio, t) * FA.condensed.mbPerBlock / 1000;
    $("#ladderRead").innerHTML = "<b>" + esc(global.FA_condensedName(t)) + "</b> · " + global.FA_num(buckets) + " buckets · " +
      global.FA_num(global.FA_condensedValue(t)) + " Essence · a stack of 64: " + global.FA_num(64 * global.FA_condensedValue(t)) + " Essence";
  };
  ["mouseover", "focusin"].forEach(ev => $("#ladder").addEventListener(ev, e => { const i = e.target.closest("img"); if (i) ladderInfo(+i.dataset.t); }));
  ladderInfo(tiers);

  // ---------- ticker ----------
  const tickerHtml = ids.filter(id => E[id].type != "fusion").map(id => '<span><img src="' + tex("tome_" + id) + '" alt="" />' + esc(E[id].name) + (E[id].mythic ? " ✦" : "") + "</span>").join("");
  $("#tickerA").innerHTML = tickerHtml;
  $("#tickerB").innerHTML = tickerHtml;

  // ---------- enchant browser ----------
  const GROUPS = [
    ["Damage", ["damage", "critical", "reaper", "cleave"]],
    ["On hit", ["lifesteal", "hit_effect", "ignite", "hit_heal", "self_effect"]],
    ["On kill", ["plunder", "kill_heal", "xp", "drop"]],
    ["Defense", ["resist", "vitality", "retribution", "dodge", "undying"]],
    ["Passive", ["regen", "passive", "magnet", "repair"]],
    ["Utility", ["attribute", "mining", "eternal"]]
  ];
  const groupOf = id => E[id].ultimate ? "Ultimate" : E[id].mythic ? "Mythic" : E[id].triad ? "Triad" : E[id].type == "fusion" ? "Fusion" : (GROUPS.find(g => g[1].includes(E[id].type)) || ["Other"])[0];
  const nice = item => item.indexOf("kubejs:tome_") === 0 && E[item.slice(12)] ? "Tome of " + E[item.slice(12)].name
    : item.replace(/^(minecraft|create|kubejs):/, "").replace(/_/g, " ");
  const catalysts = id => {
    const r = FA_RECIPES[id];
    if (!r) return "";
    const counts = {};
    r.items.forEach(i => { counts[i] = (counts[i] || 0) + 1; });
    return Object.keys(counts).map(i => (counts[i] > 1 ? counts[i] + "× " : "") + nice(i)).join(", ");
  };
  // Total Essence to take an enchant from 0 to `level`. Enchants only differ by their cost multiplier, so one running
  // total per multiplier is cached and extended as needed: every card is then an O(1) lookup instead of a loop to the level.
  const cum = {};
  const totalCost = (id, level) => {
    const key = global.FA_costMult(id);   // same multiplier -> same curve
    const arr = cum[key] || (cum[key] = [0]);
    for (let k = arr.length - 1; k < level; k++) arr.push(arr[k] + global.FA_cost(k, id));
    return arr[level];
  };

  const PRESETS = [1, 10, 100, 1000, 3000, 100000];
  const toLevel = v => Math.max(1, Math.round(Math.pow(10, (v / 1000) * 5)));      // slider 0..1000 -> level 1..100,000 (log)
  const toSlider = l => Math.round(Math.log10(l) / 5 * 1000);
  let level = 1, group = "All", query = "";

  const chips = ["All"].concat(GROUPS.map(g => g[0]), ["Mythic", "Ultimate", "Fusion", "Triad"]);
  $("#chips").innerHTML = chips.map(g => '<button class="uv-chip' + (g == "All" ? " is-active" : "") + '" role="tab" data-g="' + g + '">' + g +
    ' <small>' + (g == "All" ? ids.length : ids.filter(id => groupOf(id) == g).length) + "</small></button>").join("");
  $("#presets").innerHTML = PRESETS.map(p => '<button class="uv-btn uv-btn--sm uv-btn--ghost" data-l="' + p + '">' + global.FA_num(p) + "</button>").join("");

  // Build every card once; filtering and level changes only update text, so it stays fast
  const grid = $("#grid");
  grid.innerHTML = ids.map(id => {
    const e = E[id];
    return '<article class="ench' + (e.ultimate ? " is-ultimate" : e.mythic ? " is-mythic" : e.type == "fusion" ? " is-fusion" : "") + '" data-id="' + id + '">' +
      '<img src="' + tex("tome_" + id) + '" alt="" loading="lazy" width="48" height="48" />' +
      "<h3>" + (e.ultimate ? "☄ " : e.mythic ? "✦ " : "") + esc(e.name) + '<span class="lv"></span></h3>' +
      '<p class="what"></p><span class="cost"></span>' +
      '<div class="meta"><span class="tag">' + groupOf(id) + "</span>" +
      (e.maxLevel ? '<span class="tag">max ' + e.maxLevel + "</span>" : '<span class="tag">no cap</span>') +
      '<span class="tag" title="Tome catalysts">' + esc(catalysts(id)) + "</span></div></article>";
  }).join("");
  const cards = [...grid.children];
  const searchText = {};
  ids.forEach(id => { searchText[id] = (E[id].name + " " + E[id].type + " " + groupOf(id) + " " + catalysts(id) + " " + global.FA_describe(id, 100)).toLowerCase(); });

  // Only cards near the screen get their text rewritten; the rest catch up when they scroll into view.
  // With 274 cards that keeps dragging the level slider smooth.
  const near = new Set();
  const parts = new Map(cards.map(c => [c, { lv: c.querySelector(".lv"), what: c.querySelector(".what"), cost: c.querySelector(".cost") }]));
  function paint(card) {
    if (card._lv === level) return;
    card._lv = level;
    const id = card.dataset.id, e = E[id], p = parts.get(card);
    const l = e.maxLevel ? Math.min(e.maxLevel, level) : level;
    p.lv.textContent = e.maxLevel == 1 ? "" : global.FA_num(l);
    p.what.textContent = global.FA_describe(id, l);
    p.cost.textContent = global.FA_num(totalCost(id, l)) + " Essence to reach";
  }
  const cardIO = new IntersectionObserver(es => es.forEach(en => {
    if (en.isIntersecting) { near.add(en.target); paint(en.target); } else near.delete(en.target);
  }), { rootMargin: "600px 0px" });
  cards.forEach(c => cardIO.observe(c));
  let lastFilter = null;
  function render() {
    let shown = 0;
    $("#lvlOut").textContent = global.FA_num(level);
    const filter = group + "|" + query;
    cards.forEach(card => {
      const id = card.dataset.id;
      const ok = (group == "All" || groupOf(id) == group) && (!query || searchText[id].includes(query));
      if (filter !== lastFilter) card.hidden = !ok;
      if (!ok) return;
      shown++;
      if (near.has(card) || lastFilter === null) paint(card);
    });
    lastFilter = filter;
    $("#count").textContent = shown ? shown + " of " + ids.length + " enchants" : "";
    if (!shown && !grid.querySelector(".empty")) grid.insertAdjacentHTML("beforeend", '<p class="empty">No enchants match that. Try "fire", "heal" or "diamond".</p>');
    if (shown) grid.querySelectorAll(".empty").forEach(n => n.remove());
  }
  const slider = $("#lvl");
  const setLevel = l => {
    level = l;
    slider.value = toSlider(l);
    slider.style.setProperty("--v", slider.value / 10 + "%");
    render();
  };
  // Dragging fires many input events per frame; render once per frame with the latest value
  let pending = null;
  slider.addEventListener("input", () => {
    if (pending === null) requestAnimationFrame(() => { const l = pending; pending = null; setLevel(l); });
    pending = toLevel(+slider.value);
  });
  $("#presets").addEventListener("click", e => { const b = e.target.closest("button"); if (b) setLevel(+b.dataset.l); });
  $("#chips").addEventListener("click", e => {
    const b = e.target.closest("button");
    if (!b) return;
    group = b.dataset.g;
    $("#chips").querySelectorAll(".uv-chip").forEach(c => c.classList.toggle("is-active", c === b));
    render();
  });
  let qTimer;
  $("#search").addEventListener("input", e => { clearTimeout(qTimer); qTimer = setTimeout(() => { query = e.target.value.trim().toLowerCase(); render(); }, 80); });
  setLevel(1);

  // ---------- mythic ----------
  const F = FA.fragments;
  $("#fragRates").textContent = "Elites drop one " + Math.round(F.eliteChance * 100) + "% of the time, plus " + (F.eliteChancePerLevel * 100) +
    "% per danger level (up to " + Math.round(F.eliteChanceCap * 100) + "%).";
  $("#mythicGrid").innerHTML = ids.filter(id => E[id].mythic).map(id =>
    '<div class="myth"><img src="' + tex("tome_" + id) + '" alt="" /><b>' + esc(E[id].name) + "</b><span>" +
    esc(global.FA_describe(id, E[id].maxLevel || 10)) + "</span></div>").join("");

  // ---------- ultimates: cards + a top-down replay of Thunder God's Waltz ----------
  const ults = ids.filter(id => E[id].ultimate);
  const showUlt = id => {
    const e = E[id];
    $("#ultName").textContent = "☄ " + e.name;
    $("#ultDesc").textContent = e.desc + ". Level 1: " + e.power + "x your hit per strike, every " + global.FA_ultCooldown(e, 1) + "s.";
    document.querySelectorAll(".ult").forEach(b => b.classList.toggle("is-active", b.dataset.id == id));
  };
  $("#ultGrid").innerHTML = ults.map(id => '<button class="ult" data-id="' + id + '"><b><img src="' + tex("tome_" + id) + '" alt="" />' + esc(E[id].name) +
    "</b><span>" + esc(E[id].desc) + "</span><small>" + E[id].power + "x per strike · " + E[id].cooldown + "s cooldown</small></button>").join("");
  $("#ultGrid").addEventListener("click", e => { const b = e.target.closest(".ult"); if (b) showUlt(b.dataset.id); });
  showUlt("thunder_waltz");

  // The replay: the same beats as the in-game move (8 circling slashes, up, 3 bolts, boom, home)
  const svg = $("#replay"), dot = $("#heroDot"), slashes = $("#slashes"), bolts = $("#bolts"), boom = $("#boom"), stepText = $("#replayStep");
  let timers = [];
  const at = (ms, fn) => timers.push(setTimeout(fn, ms));
  const move = (x, y, r) => { dot.setAttribute("cx", x); dot.setAttribute("cy", y); if (r) dot.setAttribute("r", r); };
  function replay() {
    timers.forEach(clearTimeout); timers = [];
    slashes.innerHTML = ""; bolts.innerHTML = ""; boom.classList.remove("go"); dot.style.opacity = 1;
    move(-48, 30, 3.2); stepText.textContent = "Sneak + hit";
    for (let i = 0; i < 8; i++) at(300 + i * 150, () => {
      const a = i * Math.PI / 4, x = Math.cos(a) * 30, y = Math.sin(a) * 30;
      move(x.toFixed(1), y.toFixed(1));
      stepText.textContent = "Blink " + (i + 1) + " / 8";
      slashes.insertAdjacentHTML("beforeend", '<line class="slash" x1="' + (x * 0.25).toFixed(1) + '" y1="' + (y * 0.25 - 6).toFixed(1) + '" x2="' + (-x * 0.25).toFixed(1) + '" y2="' + (-y * 0.25 + 6).toFixed(1) + '" />');
    });
    at(1600, () => { move(0, -2, 5.5); dot.style.opacity = 0.6; stepText.textContent = "Into the sky"; });
    for (let i = 0; i < 3; i++) at(1950 + i * 300, () => {
      stepText.textContent = "Lightning " + (i + 1) + " / 3";
      const ox = (i - 1) * 4;
      bolts.insertAdjacentHTML("beforeend", '<polyline class="bolt" points="' + (ox - 6) + ',-56 ' + (ox + 4) + ',-36 ' + (ox - 4) + ',-28 ' + (ox + 6) + ',-8 ' + (ox - 1) + ',0" />');
    });
    at(2950, () => { boom.classList.remove("go"); void boom.getBoundingClientRect(); boom.classList.add("go"); stepText.textContent = "Boom"; });
    at(3600, () => { dot.style.opacity = 1; move(-48, 30, 3.2); stepText.textContent = "Back where you started"; });
  }
  $("#replayBtn").addEventListener("click", replay);
  new IntersectionObserver(es => { if (es[0].isIntersecting) replay(); }, { threshold: 0.5 }).observe($(".replay"));

  // ---------- chaos forge (same generator as the game: global.FA_rollEnchant) ----------
  const C = FA.chaos;
  const RAR_HEX = { gray: "#b9b3ad", aqua: "#4fe3e8", light_purple: "#d48cff", gold: "#ffb84d", red: "#ff4d4d" };
  const totalWeight = C.rarities.reduce((a, R) => a + R.weight, 0);
  $("#odds").innerHTML = C.rarities.map(R => '<i style="width:' + (R.weight / totalWeight * 100) + "%;background:" + RAR_HEX[R.color] + '"></i>').join("");
  $("#oddsKey").innerHTML = C.rarities.map(R => '<span><b style="color:' + RAR_HEX[R.color] + '">' + R.name + "</b> " + Math.round(R.weight / totalWeight * 1000) / 10 + "%</span>").join("");
  let rollLevel = 1, current = null, rolled = 0, best = -1;
  const LEVELS = [1, 10, 100, 1000];
  $("#rollLevels").innerHTML = LEVELS.map(l => '<button class="uv-btn uv-btn--sm uv-btn--ghost' + (l == 1 ? " is-active" : "") + '" data-l="' + l + '">' + global.FA_num(l) + "</button>").join("");
  const randomSeed = () => 1 + Math.floor(Math.random() * 2147483645);
  const lines = (roll, l) => roll.affixes.map(a => global.FA_describeDef(a, l));

  function showRoll(seed, animate) {
    const roll = global.FA_rollEnchant(seed);
    const R = C.rarities[roll.rarity];
    current = seed;
    const card = $("#rollCard");
    card.className = "roll-card r" + roll.rarity;
    card.style.setProperty("--rc", RAR_HEX[R.color]);
    card.innerHTML = '<span class="rar">' + R.name + "</span><h3>" + esc(roll.name) + '</h3><span class="lvl">Level ' + global.FA_num(rollLevel) +
      " · upgrades cost x" + R.costMult + "</span><ul>" + lines(roll, rollLevel).map(t => "<li>" + esc(t) + "</li>").join("") +
      '</ul><span class="seed">seed ' + roll.seed + "</span>";
    if (animate && !reduce) { void card.offsetWidth; card.classList.add("flip"); }
    return roll;
  }
  function celebrate(roll) {
    if (reduce || roll.rarity < 3 || !(window.UV && UV.confetti)) return;
    const r = $("#rollCard").getBoundingClientRect();
    UV.confetti({ x: r.left + r.width / 2, y: r.top + 40, count: roll.rarity == 4 ? 200 : 90, colors: roll.rarity == 4 ? ["#ff4d4d", "#ffb84d", "#fff"] : ["#ffb84d", "#fff3c4", "#ff8a3d"] });
  }
  function stats(roll) {
    rolled++;
    if (roll.rarity > best) best = roll.rarity;
    $("#rollStats").textContent = rolled + " rolled this visit · best: " + C.rarities[best].name;
  }
  $("#rollOne").addEventListener("click", () => {
    const tome = $(".chaos-tome");
    tome.classList.remove("shake"); void tome.offsetWidth; tome.classList.add("shake");
    const roll = showRoll(randomSeed(), true);
    stats(roll); celebrate(roll);
  });
  $("#rollMany").addEventListener("click", () => {
    let top = null;
    $("#rollGrid").innerHTML = Array.from({ length: 12 }, (_, i) => {
      const seed = randomSeed(), roll = global.FA_rollEnchant(seed), R = C.rarities[roll.rarity];
      stats(roll);
      if (!top || roll.rarity > top.rarity) top = roll;
      return '<button class="roll-mini" style="--rc:' + RAR_HEX[R.color] + ";--d:" + i * 45 + 'ms" data-seed="' + seed + '"><small>' + R.name + "</small><b>" + esc(roll.name) +
        "</b>" + lines(roll, 1).map(t => "<span>" + esc(t) + "</span>").join("") + "</button>";
    }).join("");
    if (top) { showRoll(top.seed, true); celebrate(top); }
  });
  $("#rollGrid").addEventListener("click", e => { const b = e.target.closest(".roll-mini"); if (b) showRoll(+b.dataset.seed, true); });
  $("#rollLevels").addEventListener("click", e => {
    const b = e.target.closest("button");
    if (!b) return;
    rollLevel = +b.dataset.l;
    $("#rollLevels").querySelectorAll("button").forEach(x => x.classList.toggle("is-active", x === b));
    if (current) showRoll(current, false);
  });
  $("#seedForm").addEventListener("submit", e => {
    e.preventDefault();
    const n = parseInt($("#seedIn").value, 10);
    if (n > 0) showRoll(n, true);
  });
  showRoll(1337, false);

  // ---------- danger journey ----------
  const D = FA.danger;
  const overworld = { dimension: "minecraft:overworld", getSharedSpawnPos: () => ({ getX: () => 0, getZ: () => 0 }) };
  const ZONE_HEX = { green: "#5fd47a", yellow: "#f3d34a", gold: "#ffb84d", red: "#ff5a3d", dark_red: "#d42a2a", dark_purple: "#a64df0", dark_gray: "#9a9a9a" };
  const MAX_DIST = 100000;
  const distAt = p => MAX_DIST * Math.pow(p, 3);          // slow start, so every zone gets screen time
  const pAt = dist => Math.cbrt(dist / MAX_DIST);
  const zoneStart = z => z.from == 0 ? 0 : D.safeRadius + (z.from - 1) * D.blocksPerLevel;
  $("#railZones").innerHTML = D.zones.map(z => '<span style="left:' + (pAt(zoneStart(z)) * 100).toFixed(1) + '%">' + esc(z.name) + "</span>").join("");
  const stage = $("#danger .uv-pin__stage");
  const el = { shade: $("#shade"), dist: $("#dist"), lvl: $("#dLevel"), hp: $("#dHealth"), dmg: $("#dDamage"), loot: $("#dLoot"), elite: $("#dElite"), dot: $("#railDot"), rail: $(".rail") };
  let railW = el.rail.clientWidth;
  addEventListener("resize", () => { railW = el.rail.clientWidth; });
  // Only touch the page when a value actually changes: scrolling stays cheap
  const shown = {};
  const put = (key, node, text) => { if (shown[key] !== text) { shown[key] = text; node.textContent = text; } };
  let lastZone = null, lastLvl = -1, lastDark = "";
  function journey(p) {
    p = Math.max(0, Math.min(1, p || 0));
    const dist = distAt(p);
    const lvl = global.FA_dangerLevel(overworld, dist, 0);
    const zone = global.FA_zone(lvl);
    put("dist", el.dist, Math.round(dist).toLocaleString("en-US"));
    el.dot.style.transform = "translateX(" + (p * railW).toFixed(1) + "px)";
    const dark = Math.min(0.95, 0.2 + p).toFixed(2);
    if (dark !== lastDark) { lastDark = dark; el.shade.style.opacity = dark; }
    if (lvl === lastLvl) return;
    lastLvl = lvl;
    const m = global.FA_dangerMult(lvl, false);
    const hex = ZONE_HEX[zone.color] || "#ffb84d";
    put("lvl", el.lvl, String(lvl));
    put("hp", el.hp, "x" + m.health.toFixed(1));
    put("dmg", el.dmg, "x" + m.damage.toFixed(1));
    put("loot", el.loot, "x" + m.loot.toFixed(1));
    put("elite", el.elite, Math.round(Math.min(D.eliteChanceCap, D.eliteChancePerLevel * lvl) * 100) + "%");
    lootOdds(lvl);
    if (zone !== lastZone) {
      stage.style.setProperty("--zt", hex);
      stage.style.setProperty("--zc", hex + "70");
      lastZone = zone;
      const name = $("#zoneName");
      name.textContent = zone.name;
      if (!reduce && window.UV && UV.scramble) UV.scramble(name, zone.name);
    }
    $("#eliteLv").textContent = lvl;
    $("#elite").classList.toggle("is-on", lvl >= 6);
  }
  // Loot odds at the current depth, and a sample drop on demand (same generator as the game)
  const TIER_HEX = { white: "#e9e4de", green: "#5fd47a", aqua: "#4fe3e8", light_purple: "#d48cff", gold: "#ffb84d", red: "#ff4d4d" };
  const segs = global.FA.loot.tiers.map((t, i) => '<i style="background:' + TIER_HEX[t.color] + '" title="' + t.name + '"></i>');
  $("#lootBar").innerHTML = segs.join("");
  let lootLvl = 0;
  function lootOdds(lvl) {
    lootLvl = lvl;
    const odds = global.FA_lootOdds(lvl);
    [...$("#lootBar").children].forEach((seg, i) => { seg.style.flexGrow = (odds[i] * 1000).toFixed(1); });
    const top = odds[4] + odds[5];
    $("#lootRead").textContent = lvl <= 0 ? "No loot drops in the Safe Lands" :
      Math.round(global.FA_lootChance(lvl) * 100) + "% of kills drop loot · Legendary or better: " + (top * 100).toFixed(top < 0.1 ? 1 : 0) + "% of drops";
  }
  const lootLabel = drop => {
    if (drop.kind == "relic") return drop.relic.name;
    const id = drop.id;
    const te = id.indexOf("kubejs:tome_") === 0 && E[id.slice(12)];
    if (te) return (te.ultimate ? "Ultimate Tome: " : te.mythic ? "Mythic Tome of " : te.triad ? "Triad Tome of " : te.type == "fusion" ? "Fusion Tome of " : "Tome of ") + te.name;
    const cm = /^kubejs:condensed_essence_(\d+)$/.exec(id);
    if (cm) return global.FA_condensedName(+cm[1]);
    const gm = /^kubejs:gem_([a-z]+)_(\d)$/.exec(id);
    if (gm && FA.gems.types[gm[1]]) return FA.gems.tiers[+gm[2] - 1].name + " " + FA.gems.types[gm[1]].name;
    return id.replace(/^[a-z]+:/, "").replace(/_/g, " ").replace(/\b\w/g, c => c.toUpperCase());
  };
  $("#lootRoll").addEventListener("click", () => {
    const lvl = Math.max(1, lootLvl);
    const roll = global.FA_rollLoot(Math.random, lvl, 0), T = global.FA.loot.tiers[roll.tier], d = roll.drop;
    let html = '<b style="color:' + TIER_HEX[T.color] + '">' + T.name + "</b> " + (d.kind == "relic" ? "" : d.count + "x ") + esc(lootLabel(d));
    if (d.kind == "relic") html += '<span class="relic-ench">' + Object.keys(d.relic.enchants).map(id => esc(E[id].name) + " " + global.FA_num(d.relic.enchants[id])).join(" · ") +
      (d.relic.gems.length ? " · " + d.relic.gems.length + " gems" : "") + (d.relic.stars ? " · ★" : "") + "</span>";
    $("#lootDrop").innerHTML = html;
    if (roll.tier >= 4 && !reduce && window.UV && UV.confetti) UV.confetti({ count: roll.tier == 5 ? 160 : 80, colors: [TIER_HEX[T.color], "#fff"] });
  });

  if (window.UV && UV.scrub) UV.scrub($("#danger"), journey, "pin");
  else addEventListener("scroll", () => { const r = $("#danger").getBoundingClientRect(); journey(-r.top / Math.max(1, r.height - innerHeight)); }, { passive: true });
  journey(0);

  // ---------- install ----------
  const MODS = [
    ["Create", "create", 1], ["KubeJS", "kubejs", 1], ["Rhino", "rhino", 1], ["Architectury API", "architectury-api", 1],
    ["Create: The Factory Must Grow", "create-industry"], ["Mekanism", "mekanism"], ["Tinkers' Construct", "tinkers-construct"],
    ["Create Enchantment Industry", "create-enchantment-industry"], ["Twilight Forest", "the-twilight-forest"],
    ["Ice and Fire", "ice-and-fire-dragons"], ["When Dungeons Arise", "when-dungeons-arise"], ["JEI", "jei"], ["Jade", "jade"],
    ["Sophisticated Backpacks", "sophisticated-backpacks"], ["Sophisticated Core", "sophisticated-core"],
    ["Embeddium", "embeddium"], ["ModernFix", "modernfix"], ["FerriteCore", "ferritecore"]
  ];
  $("#mods").innerHTML = MODS.map(m => '<a class="uv-chip' + (m[2] ? " req" : "") + '" href="https://www.curseforge.com/minecraft/mc-mods/' + m[1] + '">' + esc(m[0]) + "</a>").join("");
  document.querySelectorAll("[data-copy]").forEach(b => b.addEventListener("click", () => {
    if (window.UV && UV.copy) UV.copy(b.dataset.copy, "Copied");
    else navigator.clipboard && navigator.clipboard.writeText(b.dataset.copy);
  }));

  // ---------- pause looping animations that are off screen ----------
  const sleepers = document.querySelectorAll(".hero, .ticker, .sec");
  const io = new IntersectionObserver(es => es.forEach(e => e.target.classList.toggle("is-off", !e.isIntersecting)), { rootMargin: "100px 0px" });
  sleepers.forEach(el => io.observe(el));

  // ---------- UI Vault touches ----------
  if (window.UV) {
    if (UV.scrollbar) UV.scrollbar();
    if (!reduce && matchMedia("(pointer: fine)").matches) {
      document.querySelectorAll(".magnet").forEach(el => UV.magnetic && UV.magnetic(el, { strength: 0.25 }));
      document.querySelectorAll(".myth, .safety .uv-card").forEach(el => UV.tilt && UV.tilt(el, { max: 6 }));
    }
  }
})();
