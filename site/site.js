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

  // ---------- ticker ----------
  const tickerHtml = ids.map(id => '<span><img src="' + tex("tome_" + id) + '" alt="" />' + esc(E[id].name) + (E[id].mythic ? " ✦" : "") + "</span>").join("");
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
  const groupOf = id => E[id].mythic ? "Mythic" : (GROUPS.find(g => g[1].includes(E[id].type)) || ["Other"])[0];
  const nice = item => item.replace(/^(minecraft|create|kubejs):/, "").replace(/_/g, " ");
  const catalysts = id => {
    const r = FA_RECIPES[id];
    if (!r) return "";
    const counts = {};
    r.items.forEach(i => { counts[i] = (counts[i] || 0) + 1; });
    return Object.keys(counts).map(i => (counts[i] > 1 ? counts[i] + "× " : "") + nice(i)).join(", ");
  };
  // Total Essence to take an enchant from 0 to `level`
  const totalCost = (id, level) => { let t = 0; for (let k = 0; k < level; k++) t += global.FA_cost(k, id); return t; };

  const PRESETS = [1, 10, 100, 1000, 3000, 100000];
  const toLevel = v => Math.max(1, Math.round(Math.pow(10, (v / 1000) * 5)));      // slider 0..1000 -> level 1..100,000 (log)
  const toSlider = l => Math.round(Math.log10(l) / 5 * 1000);
  let level = 1, group = "All", query = "";

  const chips = ["All"].concat(GROUPS.map(g => g[0]), ["Mythic"]);
  $("#chips").innerHTML = chips.map(g => '<button class="uv-chip' + (g == "All" ? " is-active" : "") + '" role="tab" data-g="' + g + '">' + g +
    ' <small>' + (g == "All" ? ids.length : ids.filter(id => groupOf(id) == g).length) + "</small></button>").join("");
  $("#presets").innerHTML = PRESETS.map(p => '<button class="uv-btn uv-btn--sm uv-btn--ghost" data-l="' + p + '">' + global.FA_num(p) + "</button>").join("");

  // Build every card once; filtering and level changes only update text, so it stays fast
  const grid = $("#grid");
  grid.innerHTML = ids.map(id => {
    const e = E[id];
    return '<article class="ench' + (e.mythic ? " is-mythic" : "") + '" data-id="' + id + '">' +
      '<img src="' + tex("tome_" + id) + '" alt="" loading="lazy" width="48" height="48" />' +
      "<h3>" + (e.mythic ? "✦ " : "") + esc(e.name) + '<span class="lv"></span></h3>' +
      '<p class="what"></p><span class="cost"></span>' +
      '<div class="meta"><span class="tag">' + groupOf(id) + "</span>" +
      (e.maxLevel ? '<span class="tag">max ' + e.maxLevel + "</span>" : '<span class="tag">no cap</span>') +
      '<span class="tag" title="Tome catalysts">' + esc(catalysts(id)) + "</span></div></article>";
  }).join("");
  const cards = [...grid.children];
  const searchText = {};
  ids.forEach(id => { searchText[id] = (E[id].name + " " + E[id].type + " " + groupOf(id) + " " + catalysts(id) + " " + global.FA_describe(id, 100)).toLowerCase(); });

  function render() {
    let shown = 0;
    $("#lvlOut").textContent = global.FA_num(level);
    cards.forEach(card => {
      const id = card.dataset.id, e = E[id];
      const ok = (group == "All" || groupOf(id) == group) && (!query || searchText[id].includes(query));
      card.hidden = !ok;
      if (!ok) return;
      shown++;
      const l = e.maxLevel ? Math.min(e.maxLevel, level) : level;
      card.querySelector(".lv").textContent = e.maxLevel == 1 ? "" : global.FA_num(l);
      card.querySelector(".what").textContent = global.FA_describe(id, l);
      card.querySelector(".cost").textContent = global.FA_num(totalCost(id, l)) + " Essence to reach";
    });
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
  slider.addEventListener("input", () => setLevel(toLevel(+slider.value)));
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

  // ---------- danger journey ----------
  const D = FA.danger;
  const overworld = { dimension: () => ({ location: () => "minecraft:overworld" }), getSharedSpawnPos: () => ({ getX: () => 0, getZ: () => 0 }) };
  const ZONE_HEX = { green: "#5fd47a", yellow: "#f3d34a", gold: "#ffb84d", red: "#ff5a3d", dark_red: "#d42a2a", dark_purple: "#a64df0", dark_gray: "#9a9a9a" };
  const MAX_DIST = 100000;
  const distAt = p => MAX_DIST * Math.pow(p, 3);          // slow start, so every zone gets screen time
  const pAt = dist => Math.cbrt(dist / MAX_DIST);
  const zoneStart = z => z.from == 0 ? 0 : D.safeRadius + (z.from - 1) * D.blocksPerLevel;
  $("#railZones").innerHTML = D.zones.map(z => '<span style="left:' + (pAt(zoneStart(z)) * 100).toFixed(1) + '%">' + esc(z.name) + "</span>").join("");
  const stage = $("#danger .uv-pin__stage");
  let lastZone = null;
  function journey(p) {
    p = Math.max(0, Math.min(1, p || 0));
    const dist = distAt(p);
    const lvl = global.FA_dangerLevel(overworld, dist, 0);
    const zone = global.FA_zone(lvl);
    const m = global.FA_dangerMult(lvl, false);
    const hex = ZONE_HEX[zone.color] || "#ffb84d";
    $("#dist").textContent = Math.round(dist).toLocaleString("en-US");
    $("#dLevel").textContent = lvl;
    $("#dHealth").textContent = "x" + m.health.toFixed(1);
    $("#dDamage").textContent = "x" + m.damage.toFixed(1);
    $("#dLoot").textContent = "x" + m.loot.toFixed(1);
    $("#dElite").textContent = Math.round(Math.min(D.eliteChanceCap, D.eliteChancePerLevel * lvl) * 100) + "%";
    $("#railDot").style.left = (p * 100).toFixed(2) + "%";
    stage.style.setProperty("--zt", hex);
    stage.style.setProperty("--zc", hex + "70");
    stage.style.setProperty("--dark", Math.min(0.95, 0.2 + p).toFixed(2));
    if (zone !== lastZone) {
      lastZone = zone;
      const name = $("#zoneName");
      name.textContent = zone.name;
      if (!reduce && window.UV && UV.scramble) UV.scramble(name, zone.name);
    }
    $("#eliteLv").textContent = lvl;
    $("#elite").classList.toggle("is-on", lvl >= 6);
  }
  if (window.UV && UV.scrub) UV.scrub($("#danger"), journey, "pin");
  else addEventListener("scroll", () => { const r = $("#danger").getBoundingClientRect(); journey(-r.top / Math.max(1, r.height - innerHeight)); }, { passive: true });
  journey(0);

  // ---------- install ----------
  const MODS = [
    ["Create", "create", 1], ["KubeJS", "kubejs", 1], ["Rhino", "rhino", 1], ["Architectury API", "architectury-api", 1],
    ["Create: The Factory Must Grow", "create-industry"], ["Mekanism", "mekanism"], ["Tinkers' Construct", "tinkers-construct"],
    ["Create Enchantment Industry", "create-enchantment-industry"], ["Twilight Forest", "the-twilight-forest"],
    ["Ice and Fire", "ice-and-fire-dragons"], ["When Dungeons Arise", "when-dungeons-arise"], ["JEI", "jei"], ["Jade", "jade"],
    ["Embeddium", "embeddium"], ["ModernFix", "modernfix"], ["FerriteCore", "ferritecore"]
  ];
  $("#mods").innerHTML = MODS.map(m => '<a class="uv-chip' + (m[2] ? " req" : "") + '" href="https://www.curseforge.com/minecraft/mc-mods/' + m[1] + '">' + esc(m[0]) + "</a>").join("");
  document.querySelectorAll("[data-copy]").forEach(b => b.addEventListener("click", () => {
    if (window.UV && UV.copy) UV.copy(b.dataset.copy, "Copied");
    else navigator.clipboard && navigator.clipboard.writeText(b.dataset.copy);
  }));

  // ---------- UI Vault touches ----------
  if (window.UV) {
    if (UV.scrollbar) UV.scrollbar();
    if (!reduce && matchMedia("(pointer: fine)").matches) {
      document.querySelectorAll(".magnet").forEach(el => UV.magnetic && UV.magnetic(el, { strength: 0.25 }));
      document.querySelectorAll(".myth, .safety .uv-card").forEach(el => UV.tilt && UV.tilt(el, { max: 6 }));
    }
  }
})();
