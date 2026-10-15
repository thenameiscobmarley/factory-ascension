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

  // ---------- hero stats (counted from the config) ----------
  const PACK = window.FA_PACK || { version: "1.0.0", checks: 457, selftest: 60 };
  const HERO = [[ids.length, "enchants"], [FA.foils.types.length, "foils"], [4, "new metals"], [4, "machines"], ["&infin;", "level cap"], [PACK.checks, "safety checks"]];
  $("#heroStats").innerHTML = HERO.map(h => typeof h[0] == "number" ? '<div><b data-uv-count="' + h[0] + '">0</b><small>' + h[1] + "</small></div>"
    : "<div><b>" + h[0] + "</b><small>" + h[1] + "</small></div>").join("");
  $("#enchCount").textContent = ids.length;

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
  let level = 1, group = "All", query = "", foilMult = 1;

  const chips = ["All"].concat(GROUPS.map(g => g[0]), ["Mythic", "Ultimate", "Fusion", "Triad"]);
  $("#chips").innerHTML = chips.map(g => '<button class="uv-chip' + (g == "All" ? " is-active" : "") + '" role="tab" data-g="' + g + '">' + g +
    ' <small>' + (g == "All" ? ids.length : ids.filter(id => groupOf(id) == g).length) + "</small></button>").join("");
  $("#presets").innerHTML = PRESETS.map(p => '<button class="uv-btn uv-btn--sm uv-btn--ghost" data-l="' + p + '">' + global.FA_num(p) + "</button>").join("");

  // Build every card once; filtering and level changes only update text, so it stays fast
  const grid = $("#grid");
  grid.innerHTML = ids.map(id => {
    const e = E[id];
    return '<article class="ench' + (e.ultimate ? " is-ultimate" : e.mythic ? " is-mythic" : e.triad ? " is-fusion is-triad" : e.type == "fusion" ? " is-fusion" : "") + '" data-id="' + id + '">' +
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
    const key = level + "x" + foilMult;
    if (card._lv === key) return;
    card._lv = key;
    const id = card.dataset.id, e = E[id], p = parts.get(card);
    const want = Math.max(1, Math.round(level * foilMult));
    const l = e.maxLevel ? Math.min(e.maxLevel, want) : want;
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
    $("#lvlOut").textContent = global.FA_num(level) + (foilMult > 1 ? " → " + global.FA_num(Math.round(level * foilMult)) : "");
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
  // Foil: what the same tome goes on as with each foil
  $("#foilPick").innerHTML = '<button class="uv-btn uv-btn--sm uv-btn--ghost is-active" data-m="1">No foil</button>' +
    FA.foils.types.map(f => '<button class="uv-btn uv-btn--sm uv-btn--ghost" data-m="' + f.mult + '">' + f.name + " x" + f.mult + "</button>").join("");
  $("#foilPick").addEventListener("click", e => {
    const b = e.target.closest("button");
    if (!b) return;
    foilMult = +b.dataset.m;
    $("#foilPick").querySelectorAll("button").forEach(x => x.classList.toggle("is-active", x === b));
    render();
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

  // ---------- shared bits for the luck and gear sections ----------
  const MC_HEX = { white: "#e9e4de", gray: "#a8a8a8", dark_gray: "#7a7a7a", green: "#5fd47a", aqua: "#4fe3e8", blue: "#5b8cff", light_purple: "#d48cff",
    dark_purple: "#a64df0", gold: "#ffb84d", yellow: "#f3e15a", red: "#ff4d4d", dark_red: "#d42a2a" };
  const hexOf = c => MC_HEX[c] || "#e9e4de";
  const oneIn = p => p >= 1 ? "always" : "1 in " + global.FA_num(Math.round(1 / p));
  const pct = p => (p * 100 >= 10 ? Math.round(p * 100) : Math.round(p * 1000) / 10) + "%";
  const FOILS = FA.foils.types;
  // A tome icon, with its foil's overlay on top when it has one (the same overlay the game draws)
  const tomeImg = (id, foil, size) => '<span class="tome" style="--s:' + (size || 64) + 'px"><img src="' + tex("tome_" + id) + '" alt="" />' +
    (foil >= 0 ? '<img class="ov f-' + FOILS[foil].id + '" src="' + tex("foil_overlay_" + FOILS[foil].id) + '" alt="" />' : "") + "</span>";
  const oddsBar = (el, weights, colors, names) => {
    const total = weights.reduce((a, b) => a + b, 0);
    el.innerHTML = weights.map((w, i) => '<i style="flex-grow:' + (w / total * 1000).toFixed(2) + ";background:" + colors[i] + '" title="' + esc(names[i]) + " " + pct(w / total) + '"></i>').join("");
  };
  const confetti = (el, count, colors) => {
    if (reduce || !(window.UV && UV.confetti) || document.documentElement.classList.contains("lite")) return;
    const r = el.getBoundingClientRect();
    UV.confetti({ x: r.left + r.width / 2, y: r.top + 30, count: count, colors: colors });
  };

  // ---------- card packs (global.FA_openPack: the game's own odds) ----------
  const P = FA.packs;
  const rarHex = i => hexOf(P.rarities[i].color);
  const pTotal = P.rarities.reduce((a, R) => a + R.weight, 0);
  oddsBar($("#packOdds"), P.rarities.map(R => R.weight), P.rarities.map(R => hexOf(R.color)), P.rarities.map(R => R.name));
  $("#packKey").innerHTML = P.rarities.map(R => '<span><b style="color:' + hexOf(R.color) + '">' + R.name + "</b> " + pct(R.weight / pTotal) + "</span>").join("");
  let packsOpened = 0, bestPull = -1;
  const cardHtml = (c, i) => {
    const R = P.rarities[c.rarity], e = E[c.id], F = c.foil >= 0 ? FOILS[c.foil] : null;
    const applied = Math.round(c.level * (F ? F.mult : 1));
    return '<article class="card r' + c.rarity + (R.holo ? " holo" : "") + (F ? " foiled" : "") + '" style="--rc:' + hexOf(R.color) + ";--d:" + (i * 140) + 'ms">' +
      '<small class="rar">' + (R.holo ? "✦ " : "") + R.name + "</small>" + tomeImg(c.id, c.foil, 64) +
      "<b>" + esc(e.name) + "</b>" +
      '<span class="lv">' + (e.maxLevel == 1 ? "&nbsp;" : "Level " + global.FA_num(c.level)) + "</span>" +
      (F ? '<span class="foil-tag" style="--fc:' + hexOf(F.color) + '">' + F.name + " foil x" + F.mult + (e.maxLevel == 1 ? "" : " &rarr; " + global.FA_num(applied)) + "</span>" : '<span class="foil-tag none">no foil</span>') +
      "</article>";
  };
  const packStats = () => { $("#packStats").textContent = packsOpened + " pack" + (packsOpened == 1 ? "" : "s") + " opened · best: " + P.rarities[bestPull].name; };
  const shakeArt = () => { const a = $("#packArt"); a.classList.remove("shake"); void a.offsetWidth; a.classList.add("shake"); };
  $("#openPack").addEventListener("click", () => {
    const cards = global.FA_openPack(Math.random);
    packsOpened++;
    cards.forEach(c => { if (c.rarity > bestPull) bestPull = c.rarity; });
    shakeArt();
    $("#cards").innerHTML = cards.map(cardHtml).join("");
    $("#boxSum").innerHTML = "";
    packStats();
    const top = cards.reduce((a, c) => c.rarity > a.rarity ? c : a, cards[0]);
    if (top.rarity >= 3) setTimeout(() => confetti($("#cards"), top.rarity >= 5 ? 200 : 90, [rarHex(top.rarity), "#fff"]), 600);
  });
  $("#openBox").addEventListener("click", () => {
    const all = [];
    for (let i = 0; i < P.boxPacks; i++) all.push.apply(all, global.FA_openPack(Math.random));
    packsOpened += P.boxPacks;
    all.forEach(c => { if (c.rarity > bestPull) bestPull = c.rarity; });
    shakeArt();
    // The five best pulls (rarity, then foil, then level)
    const score = c => c.rarity * 1e9 + (c.foil + 1) * 1e7 + c.level;
    const best = all.slice().sort((a, b) => score(b) - score(a)).slice(0, P.cards);
    $("#cards").innerHTML = best.map(cardHtml).join("");
    const counts = P.rarities.map((_, i) => all.filter(c => c.rarity == i).length);
    const foiled = all.filter(c => c.foil >= 0).length;
    const top = all.reduce((a, c) => c.level > a.level ? c : a, all[0]);
    $("#boxSum").innerHTML = "<b>" + P.boxPacks + " packs, " + all.length + " cards:</b> " + counts.map((n, i) => n ? '<span style="color:' + rarHex(i) + '">' + n + " " + P.rarities[i].name + "</span>" : "").filter(Boolean).join(" · ") +
      " · " + foiled + " foiled · highest level: " + esc(E[top.id].name) + " " + global.FA_num(top.level) + "<small>Showing the five best.</small>";
    packStats();
    if (best[0].rarity >= 3) setTimeout(() => confetti($("#cards"), 140, [rarHex(best[0].rarity), "#fff"]), 600);
  });

  // ---------- foils ----------
  const fTotal = FOILS.reduce((a, f) => a + f.weight, 0);
  $("#foils").innerHTML = FOILS.map((f, i) => '<div class="foil" style="--fc:' + hexOf(f.color) + '">' + tomeImg("sharpness", i, 56) +
    '<img class="sheet" src="' + tex("foil_sheet_" + f.id) + '" alt="" width="28" height="28" />' +
    "<b>" + f.name + "</b><span class=\"mult\">x" + f.mult + "</span><small>" + pct(f.weight / fTotal) + " of foils</small></div>").join("");

  // ---------- Sealed Gear Caches (FA_cacheQuality + FA_makeRelic) ----------
  const CQ = FA.caches.qualities;
  const QHEX = { rare: "#4fe3e8", epic: "#d48cff", legendary: "#ffb84d", mythic: "#ff4d4d" };
  let cacheLvl = 0;
  const cacheOdds = () => {
    $("#cacheLvlOut").textContent = cacheLvl;
    oddsBar($("#cacheOdds"), CQ.map(q => q.weight * Math.pow(1 + q.growth, cacheLvl)), CQ.map(q => QHEX[q.quality]), CQ.map(q => q.quality));
  };
  $("#cacheLvl").addEventListener("input", e => { cacheLvl = +e.target.value; e.target.style.setProperty("--v", cacheLvl / 40 * 100 + "%"); cacheOdds(); });
  cacheOdds();
  const relicHtml = (relic, art) => {
    const ench = Object.keys(relic.enchants).map(id => "<li>" + esc(E[id].name) + " <b>" + global.FA_num(relic.enchants[id]) + "</b></li>").join("");
    const gems = relic.gems.map(g => '<img src="' + tex(global.FA_gemId(g[0], g[1]).replace("kubejs:", "")) + '" alt="" title="' + esc(FA.gems.tiers[g[1] - 1].name + " " + FA.gems.types[g[0]].name) + '" width="22" height="22" />').join("");
    const head = art ? '<p class="art-name" style="color:' + hexOf(art.color) + '">' + esc(art.name) + ", " + esc(art.title) + "</p>" : "";
    return '<div class="relic-card q-' + relic.quality + '" style="--qc:' + QHEX[relic.quality] + '">' + head +
      '<small class="rar">' + relic.quality + (relic.stars ? " ★" : "") + (art ? " · Artifact" : "") + "</small><b>" + esc(relic.name) + "</b>" +
      '<span class="dim">' + esc(relic.item.replace("minecraft:", "").replace(/_/g, " ")) + "</span><ul>" + ench + "</ul>" + (gems ? '<div class="gems">' + gems + "</div>" : "") +
      (art ? '<ul class="quirks">' + art.quirks.map(q => "<li>" + esc(global.FA_quirkText(q.id, q.power)) + "</li>").join("") + "</ul>" : "") + "</div>";
  };
  $("#openCache").addEventListener("click", () => {
    const q = global.FA_cacheQuality(Math.random, cacheLvl);
    const relic = global.FA_makeRelic(Math.random, q, cacheLvl);
    const AR = FA.artifacts;
    const art = q == "mythic" && AR.enabled && Math.random() < AR.relicChance ? global.FA_rollArtifact(Math.random, relic.kind == "weapon" ? "weapon" : relic.kind == "tool" ? "tool" : "armor") : null;
    $("#relic").innerHTML = relicHtml(relic, art);
    if (q == "legendary" || q == "mythic") confetti($("#relic"), q == "mythic" ? 160 : 70, [QHEX[q], "#fff"]);
  });

  // ---------- jackpot tail (P(mult >= k) = k^-alpha) ----------
  const J = FA.jackpot;
  $("#tail").innerHTML = [10, 100, 1000, 10000].map(k => "<li><b>x" + global.FA_num(k) + "</b> the normal level <span>" + oneIn(Math.pow(k, -J.levelAlpha)) + "</span></li>").join("") +
    "<li><b>" + global.FA_num(J.levelCap) + "</b> the hard cap <span>it happens</span></li>";
  $("#chestJackpot").textContent = pct(J.chestChance);

  // ---------- rare mob variants ----------
  const V = FA.variants;
  const HIT = { fire: "hits set you on fire", frost: "hits slow you", storm: "hits call lightning (+" + Math.round(V.stormBonus * 100) + "%)" };
  $("#variants").innerHTML = V.types.map(t => '<div class="variant" style="--vc:' + hexOf(t.color) + '"><b>' + (t.symbol ? t.symbol + " " : "") + t.name + "</b><span>" + oneIn(t.chance) +
    (t.elemental ? " hostile mobs" : " mobs") + "</span><small>" + (t.onHit ? HIT[t.onHit] + " · " : t.glow ? "glows through walls · " : "sparkles · ") + "x" + t.xp + " XP · " + t.lootRolls +
    " loot roll" + (t.lootRolls > 1 ? "s" : "") + (t.lootBump ? " (better)" : "") + "</small></div>").join("");

  // ---------- gear tiers (config: ascendite, gearTiers; diamond and netherite for comparison) ----------
  const armorSum = a => a.protection.reduce((x, y) => x + y, 0);
  const TIERS = [
    { id: "ascendite", name: "Ascendite", t: FA.ascendite.tools, a: FA.ascendite.armor, note: "Between iron and diamond. Mines diamonds." },
    { name: "Diamond", ref: true, t: { uses: 1561, attackDamageBonus: 3, speed: 8 }, a: { protection: [3, 6, 8, 3], toughness: 2, knockbackResistance: 0 } },
    { id: "voidsteel", note: "Between diamond and netherite." },
    { name: "Netherite", ref: true, t: { uses: 2031, attackDamageBonus: 4, speed: 9 }, a: { protection: [3, 6, 8, 3], toughness: 3, knockbackResistance: 0.1 } },
    { id: "celestium", note: "Past netherite." },
    { id: "ascendium", note: "The endgame metal." }
  ].map(T => T.id && FA.gearTiers[T.id] ? Object.assign({ name: FA.gearTiers[T.id].name, t: FA.gearTiers[T.id].tools, a: FA.gearTiers[T.id].armor }, T) : T);
  const maxOf = f => Math.max.apply(null, TIERS.map(f));
  const STATS = [["Durability", T => T.t.uses, v => global.FA_num(v)], ["Damage", T => T.t.attackDamageBonus, v => "+" + v],
    ["Armor", T => armorSum(T.a), v => v], ["Toughness", T => T.a.toughness, v => v]];
  const maxes = STATS.map(s => maxOf(s[1]));
  $("#tiers").innerHTML = TIERS.map(T => '<div class="tier' + (T.ref ? " ref" : " t-" + T.id) + '">' +
    (T.ref ? '<div class="tier-icons"><span class="ref-dot"></span></div>' : '<div class="tier-icons"><img src="' + tex(T.id + "_sword") + '" alt="" /><img src="' + tex(T.id + "_chestplate") + '" alt="" /><img src="' + tex(T.id + "_ingot") + '" alt="" /></div>') +
    "<b>" + T.name + "</b>" + (T.ref ? "<small>vanilla, for comparison</small>" : "<small>" + esc(T.note) + "</small>") +
    STATS.map((s, i) => { const v = s[1](T); return '<div class="bar"><span>' + s[0] + "</span><i style=\"--w:" + (v / maxes[i] * 100).toFixed(1) + '%"></i><em>' + s[2](v) + "</em></div>"; }).join("") +
    "</div>").join("");

  // ---------- condition grades ----------
  const G = FA.condition.grades;
  oddsBar($("#grades"), G.map(g => g.weight), G.map(g => hexOf(g.color)), G.map(g => g.name));
  const gradeLine = g => "<b style=\"color:" + hexOf(g.color) + "\">" + g.name + "</b> · " + (g.damage >= 1 ? "+" : "") + Math.round((g.damage - 1) * 100) + "% damage · " +
    (g.defense <= 1 ? "-" : "+") + Math.abs(Math.round((g.defense - 1) * 1000) / 10) + "% damage taken per piece" + (g.wear ? " · " + Math.round(g.wear * 100) + "% worn" : "");
  $("#rollGrade").addEventListener("click", () => {
    const i = global.FA_rollCondition(Math.random);
    $("#gradeOut").innerHTML = gradeLine(G[i]) + " <span class=\"dim\">(" + pct(G[i].weight / G.reduce((a, g) => a + g.weight, 0)) + " of gear)</span>";
    if (i == G.length - 1) confetti($("#gradeOut"), 60, ["#ffb84d", "#fff"]);
  });

  // ---------- attachments ----------
  const AT = FA.attachments.list;
  $("#attach").innerHTML = AT.map(a => '<button class="att" data-id="' + a.id + '" title="' + esc(a.name) + '"><img src="' + tex("attachment_" + a.id) + '" alt="' + esc(a.name) + '" /></button>').join("");
  const showAtt = id => { const a = global.FA_attachmentDef(id); $("#attachOut").innerHTML = "<b>" + esc(a.name) + "</b> <span class=\"dim\">(" + a.fits + ")</span> " + esc(a.text.replace("{p}", a.power)); };
  ["mouseover", "focusin", "click"].forEach(ev => $("#attach").addEventListener(ev, e => { const b = e.target.closest(".att"); if (b) showAtt(b.dataset.id); }));
  showAtt("jetpack");

  // ---------- artifacts (FA_rollArtifact) ----------
  const AR = FA.artifacts;
  $("#awakenLvls").textContent = global.FA_num(AR.awakenLevels);
  $("#relicArt").textContent = "Or get lucky: " + Math.round(AR.relicChance * 100) + "% of Mythic relics drop already awakened.";
  let artKind = "weapon", serial = 0;
  const KIND_ICON = { weapon: "ascendium_sword", armor: "ascendium_chestplate", tool: "ascendium_pickaxe" };
  const awaken = () => {
    const a = global.FA_rollArtifact(Math.random, artKind);
    serial++;
    const c = hexOf(a.color);
    $("#artCard").style.setProperty("--ac", c);
    $("#artCard").innerHTML = '<img src="' + tex(KIND_ICON[artKind]) + '" alt="" width="72" height="72" /><small class="rar">Artifact · No. ' + serial + "</small>" +
      "<b>" + esc(a.name) + "</b><span class=\"title\">" + esc(a.title) + "</span><i>“" + esc(a.lore) + "”</i><ul>" +
      a.quirks.map(q => "<li>" + esc(global.FA_quirkText(q.id, q.power)) + "</li>").join("") + "</ul>";
    if (!reduce) { const el = $("#artCard"); el.classList.remove("flip"); void el.offsetWidth; el.classList.add("flip"); }
  };
  $("#artKind").addEventListener("click", e => {
    const b = e.target.closest("button");
    if (!b) return;
    artKind = b.dataset.k;
    $("#artKind").querySelectorAll("button").forEach(x => x.classList.toggle("is-active", x === b));
    awaken();
  });
  $("#awaken").addEventListener("click", awaken);
  awaken();

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

  // ---------- machines (the Full download's blocks; recipes and numbers from config.machines) ----------
  const M = FA.machines;
  const vaultSlots = 108 * (M.vaultUpgrades.length + 1);
  const itemName = id => {
    const t = id.indexOf("kubejs:tome_") === 0 && E[id.slice(12)];
    return t ? "Tome of " + t.name : id.replace(/^[a-z_]+:/, "").replace(/_/g, " ").replace(/\b\w/g, c => c.toUpperCase());
  };
  const MACHINES = [
    ["ascension_vault", "Ascension Vault", "A storage chest with a search bar, a Sort button and a scroll bar. Starts at 108 slots; right-click it with " +
      M.vaultUpgrades.map(u => u[1] + " " + itemName(u[0])).join(", then ") + " for +108 each (" + vaultSlots + " max). Hoppers and funnels work."],
    ["card_binder", "Card Binder", "Holds 54 tome cards and nothing else. Shows how much of the " + ids.length + "-card collection you own, and floats your best card above the block."],
    ["essence_forge", "Essence Forge", "Put gear in the slot and pipe Liquid Essence into its 16-bucket tank. It levels the gear by the same rules as the Ascension Altar, once a second."],
    ["pack_opener", "Pack Opener", "Feed it Enchantment Packs with a hopper. It opens one every " + (M.openerSeconds == 1 ? "second" : M.openerSeconds + " seconds") + " into 27 output slots and flips the cards on screen."]
  ];
  const recipeHtml = r => {
    const rows = r[0], keys = r[1];
    const cell = ch => {
      if (ch == " ") return "<i></i>";
      const id = keys[ch], k = id.indexOf("kubejs:") === 0;
      return k ? '<i title="' + esc(itemName(id)) + '"><img src="' + tex(id.slice(7)) + '" alt="" /></i>' : '<i title="' + esc(itemName(id)) + '"><b>' + ch + "</b></i>";
    };
    const legend = Object.keys(keys).filter(ch => keys[ch].indexOf("kubejs:") !== 0).map(ch => "<b>" + ch + "</b> " + esc(itemName(keys[ch]))).join(" · ");
    return '<div class="recipe"><div class="craft">' + rows.join("").split("").map(cell).join("") + '</div><small>' + legend + "</small></div>";
  };
  $("#machines").innerHTML = MACHINES.map(m => {
    const f = "fa/blocks/" + m[0];
    return '<article class="machine"><div class="cube-wrap"><div class="cube">' +
      '<i class="f" style="background-image:url(' + f + '_front.png)"></i><i class="r" style="background-image:url(' + f + '_side.png)"></i>' +
      '<i class="l" style="background-image:url(' + f + '_side.png)"></i><i class="b" style="background-image:url(' + f + '_side.png)"></i>' +
      '<i class="t" style="background-image:url(' + f + '_top.png)"></i></div></div>' +
      "<h3>" + m[1] + "</h3><p>" + esc(m[2]) + "</p>" + recipeHtml(M.recipes[m[0]]) + "</article>";
  }).join("");

  // ---------- survival kit ----------
  const H = FA.hammer, S = FA.sell;
  const KIT = [
    ["ascendite_ore", "Glowing ores", "Ascendite and every valuable ore give off light, so veins show up in dark caves. Diamonds glow at " + FA.glowingOres.light["minecraft:diamond_ore"] + "."],
    ["ascendite_rock_hammer", "Rock Hammers", "Hammer in one hand, rocks in the other: smash cobblestone into gravel, nuggets and the odd Raw Ascendite. The Ascendite hammer is " +
      H.tools["kubejs:ascendite_rock_hammer"].luck + "x as lucky. A Deployer can swing it for you."],
    ["ascension_core", "/ascension sell", "Turn loot you don't need into Ascension Cores or Liquid Essence. A Booster Box sells for " + S.items["kubejs:booster_box"] +
      " Essence, an Artifact for " + S.artifact + " more. Foiled tomes sell for their foil's multiplier."],
    ["ascendium_boots", "/sethome and /home", "One home, any dimension. Locked for " + FA.home.combatSeconds + "s after a fight, with a " + FA.home.cooldownSeconds + "s cooldown."],
    ["gem_sapphire_3", "Gems", "Socket up to " + FA.gems.sockets + " gems into any gear for free enchant levels: Chipped +" + FA.gems.tiers[0].levels + ", Flawless +" + FA.gems.tiers[1].levels + ", Perfect +" + FA.gems.tiers[2].levels + "."],
    ["ascension_singularity", "Zone titles", "Cross into a new danger zone and its name fills the screen, with a rumble that gets deeper the further out you go."]
  ];
  $("#kitGrid").innerHTML = KIT.map(k => '<article class="kit-card"><img src="' + tex(k[0]) + '" alt="" width="40" height="40" /><b>' + k[1] + "</b><p>" + esc(k[2]) + "</p></article>").join("");
  const MF = FA.mobFood.foods;
  const effName = e => e[0].replace("minecraft:", "").replace(/_/g, " ") + (e[2] ? " " + ["", "II", "III", "IV"][e[2]] : "") + " " + e[1] + "s";
  $("#food").innerHTML = MF.map((f, i) => '<button class="att" data-i="' + i + '"><img src="' + tex(f.cooked) + '" alt="' + esc(f.cookedName) + '" /></button>').join("");
  const showFood = i => {
    const f = MF[i];
    $("#foodOut").innerHTML = "<b>" + esc(f.cookedName) + "</b> <span class=\"dim\">from " + esc(f.rawName) + " (" + f.mobs.map(m => m.replace("minecraft:", "").replace(/_/g, " ")).join(", ") + ", " +
      Math.round(f.chance * 100) + "%)</span> · " + f.hunger + " hunger · " + esc(effName(f.effect));
  };
  ["mouseover", "focusin", "click"].forEach(ev => $("#food").addEventListener(ev, e => { const b = e.target.closest(".att"); if (b) showFood(+b.dataset.i); }));
  showFood(0);

  // ---------- downloads (version from fa/pack.js, written by tools/build_site.sh) ----------
  const REL = "https://github.com/thenameiscobmarley/factory-ascension/releases/download/v" + PACK.version + "/FactoryAscension-";
  document.querySelectorAll("[data-dl]").forEach(a => {
    a.href = REL + a.dataset.dl + "-" + PACK.version + (a.dataset.cf ? "-CurseForge" : "") + ".zip";
    a.textContent += " · v" + PACK.version;
  });
  const MODS = [
    ["Create", "create", 1], ["KubeJS", "kubejs", 1], ["Rhino", "rhino", 1], ["Architectury API", "architectury-api", 1],
    ["Create: The Factory Must Grow", "create-industry"], ["Mekanism", "mekanism"], ["JEI", "jei"], ["Waystones", "waystones"], ["Balm", "balm"],
    ["Sophisticated Backpacks", "sophisticated-backpacks"], ["Sophisticated Core", "sophisticated-core"], ["Tinkers' Construct", "tinkers-construct"],
    ["Create Enchantment Industry", "create-enchantment-industry"], ["Twilight Forest", "the-twilight-forest"], ["Ice and Fire", "ice-and-fire-dragons"],
    ["When Dungeons Arise", "when-dungeons-arise"], ["Jade", "jade"], ["Embeddium", "embeddium"], ["ModernFix", "modernfix"], ["FerriteCore", "ferritecore"]
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
      document.querySelectorAll(".myth, .safety .uv-card, .machine, .tier:not(.ref)").forEach(el => UV.tilt && UV.tilt(el, { max: 6 }));
    }
  }
})();
