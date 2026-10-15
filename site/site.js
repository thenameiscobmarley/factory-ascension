/* Factory Ascension site logic. Every number comes from the pack's real config (fa/00_fa_config.js), which this page runs
   directly, so the site always matches the game. The page is a book: one chapter (section.ch) is open at a time. */
(function () {
  "use strict";
  const FA = global.FA;
  const E = FA.enchants;
  const ids = Object.keys(E);
  const $ = s => document.querySelector(s);
  const reduce = matchMedia("(prefers-reduced-motion: reduce)").matches;
  const esc = s => String(s).replace(/[&<>"]/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[c]);
  const tex = id => "fa/items/" + id + ".png";
  const PACK = window.FA_PACK || { version: "1.1.0", checks: 461 };
  const MC_HEX = { white: "#e9e4de", gray: "#a8a8a8", dark_gray: "#8a8a8a", green: "#5fd47a", aqua: "#4fe3e8", blue: "#6b9cff", light_purple: "#d48cff",
    dark_purple: "#b06cf0", gold: "#ffb84d", yellow: "#f3e15a", red: "#ff5a5a", dark_red: "#e04040" };
  const hexOf = c => MC_HEX[c] || "#e9e4de";
  const oneIn = p => p >= 1 ? "always" : "1 in " + global.FA_num(Math.round(1 / p));
  const pct = p => (p * 100 >= 10 ? Math.round(p * 100) : Math.round(p * 1000) / 10) + "%";

  // ---------- pixel confetti: square bits that fall in steps ----------
  function confetti(el, count, colors) {
    if (reduce || !el) return;
    const r = el.getBoundingClientRect();
    const c = document.createElement("canvas");
    c.className = "confetti";
    c.width = innerWidth; c.height = innerHeight;
    document.body.appendChild(c);
    const g = c.getContext("2d"), px = parseInt(getComputedStyle(document.documentElement).getPropertyValue("--px")) || 3;
    const bits = Array.from({ length: count }, () => ({ x: r.left + r.width / 2, y: r.top + 20, vx: (Math.random() - .5) * 14, vy: -6 - Math.random() * 10,
      s: px * (1 + (Math.random() * 2 | 0)), c: colors[Math.random() * colors.length | 0] }));
    let frame = 0;
    (function tick() {
      g.clearRect(0, 0, c.width, c.height);
      bits.forEach(b => { b.vy += .55; b.x += b.vx; b.y += b.vy; b.vx *= .98; g.fillStyle = b.c; g.fillRect(Math.round(b.x / px) * px, Math.round(b.y / px) * px, b.s, b.s); });
      if (++frame < 90) requestAnimationFrame(tick); else c.remove();
    })();
  }
  const copy = text => {
    if (navigator.clipboard) navigator.clipboard.writeText(text);
  };

  // ---------- chapters: one open at a time, chosen by the address (#luck, #enchants/chaos...) ----------
  const CHAPTERS = [...document.querySelectorAll("section.ch")].map(s => s.id);
  // Old links from the previous site still land in the right place
  const ALIAS = { loop: "home", top: "home", install: "download", safety: "download", danger: "world", kit: "world", blocks: "machines",
    mythic: "enchants/mythic", ultimates: "enchants/ultimates", chaos: "enchants/chaos" };
  function route() {
    let hash = decodeURIComponent(location.hash.slice(1)) || "home";
    hash = ALIAS[hash] || hash;
    const [ch, sub] = hash.split("/");
    const open = CHAPTERS.includes(ch) ? ch : "home";
    document.querySelectorAll("section.ch").forEach(s => s.classList.toggle("is-open", s.id == open));
    document.querySelectorAll("#shelf a[data-ch]").forEach(a => a.toggleAttribute("aria-current", a.dataset.ch == open));
    if (open == "enchants") showSub(sub || "browse");
    window.scrollTo({ top: 0, behavior: "instant" });
  }
  addEventListener("hashchange", route);

  // ---------- hero stats (straight from the config: no counting animation, so the numbers never disagree) ----------
  $("#heroStats").innerHTML = [[ids.length, "enchants"], [FA.foils.types.length, "foils"], [4, "new metals"], [11, "machines"], [(window.FA_MOBS || []).length || 34, "new mobs"], [FA.mobFood.foods.length, "mob parts"], ["∞", "level cap"], [PACK.checks, "safety checks"]]
    .map(h => '<div class="slot-card"><b>' + (typeof h[0] == "number" ? global.FA_num(h[0]) : h[0]) + "</b><small>" + h[1] + "</small></div>").join("");
  $("#enchCount").textContent = global.FA_num(ids.length);
  $("#newVersion").textContent = "v" + PACK.version.replace(/\.0$/, "");

  // ---------- Liquid Essence color + the density ladder ----------
  document.documentElement.style.setProperty("--essence", "#" + ("000000" + FA.altar.color.toString(16)).slice(-6));
  const tiers = global.FA_condensedTiers();
  $("#ladderTop").textContent = global.FA_condensedName(tiers);
  $("#ladder").innerHTML = Array.from({ length: tiers + 1 }, (_, t) => '<img src="' + tex("condensed_essence_" + t) + '" alt="' + esc(global.FA_condensedName(t)) +
    '" title="' + esc(global.FA_condensedName(t)) + '" tabindex="0" data-t="' + t + '" loading="lazy" />').join("");
  const ladderInfo = t => {
    const buckets = Math.pow(FA.condensed.ratio, t) * FA.condensed.mbPerBlock / 1000;
    $("#ladderRead").innerHTML = "<b>" + esc(global.FA_condensedName(t)) + "</b> · " + global.FA_num(buckets) + " buckets · " + global.FA_num(global.FA_condensedValue(t)) + " Essence";
  };
  ["mouseover", "focusin"].forEach(ev => $("#ladder").addEventListener(ev, e => { const i = e.target.closest("img"); if (i) ladderInfo(+i.dataset.t); }));
  ladderInfo(tiers);

  // ---------- shared: tome icons with foil overlays, odds bars ----------
  const FOILS = FA.foils.types;
  const tomeImg = (id, foil, size) => '<span class="tome" style="--s:' + (size || 64) + 'px"><img src="' + tex("tome_" + id) + '" alt="" />' +
    (foil >= 0 ? '<img src="' + tex("foil_overlay_" + FOILS[foil].id) + '" alt="" />' : "") + "</span>";
  const oddsBar = (el, weights, colors, names) => {
    const total = weights.reduce((a, b) => a + b, 0);
    el.innerHTML = weights.map((w, i) => '<i style="flex-grow:' + (w / total * 1000).toFixed(2) + ";background:" + colors[i] + '" title="' + esc(names[i]) + " " + pct(w / total) + '"></i>').join("");
  };

  // ---------- LUCK: card packs (global.FA_openPack: the game's own odds) ----------
  const P = FA.packs;
  const rarHex = i => hexOf(P.rarities[i].color);
  const pTotal = P.rarities.reduce((a, R) => a + R.weight, 0);
  oddsBar($("#packOdds"), P.rarities.map(R => R.weight), P.rarities.map(R => hexOf(R.color)), P.rarities.map(R => R.name));
  $("#packKey").innerHTML = P.rarities.map(R => '<span><b style="color:' + hexOf(R.color) + '">' + R.name + "</b> " + pct(R.weight / pTotal) + "</span>").join("");
  let packsOpened = 0, bestPull = -1;
  const cardHtml = (c, i) => {
    const R = P.rarities[c.rarity], e = E[c.id], F = c.foil >= 0 ? FOILS[c.foil] : null;
    const applied = Math.round(c.level * (F ? F.mult : 1));
    return '<article class="card' + (R.holo ? " holo" : "") + (F ? " foiled" : "") + '" style="--rc:' + hexOf(R.color) + ";--d:" + (i * 140) + 'ms">' +
      '<small class="rar">' + (R.holo ? "✦ " : "") + R.name + "</small>" + tomeImg(c.id, c.foil, 56) + "<b>" + esc(e.name) + "</b>" +
      '<span class="lv">' + (e.maxLevel == 1 ? "&nbsp;" : "Level " + global.FA_num(c.level)) + "</span>" +
      (F ? '<span class="foil-tag" style="--fc:' + hexOf(F.color) + '">' + F.name + " x" + F.mult + (e.maxLevel == 1 ? "" : " → " + global.FA_num(applied)) + "</span>"
        : '<span class="foil-tag none">no foil</span>') + "</article>";
  };
  const packStats = () => { $("#packStats").textContent = packsOpened + " pack" + (packsOpened == 1 ? "" : "s") + " opened · best: " + P.rarities[bestPull].name; };
  const shake = el => { el.classList.remove("shake"); void el.offsetWidth; el.classList.add("shake"); };
  $("#openPack").addEventListener("click", () => {
    const cards = global.FA_openPack(Math.random);
    packsOpened++;
    cards.forEach(c => { if (c.rarity > bestPull) bestPull = c.rarity; });
    shake($("#packArt"));
    $("#cards").innerHTML = cards.map(cardHtml).join("");
    $("#boxSum").innerHTML = "";
    packStats();
    const top = cards.reduce((a, c) => c.rarity > a.rarity ? c : a, cards[0]);
    if (top.rarity >= 3) setTimeout(() => confetti($("#cards"), top.rarity >= 5 ? 160 : 70, [rarHex(top.rarity), "#ffffff", "#ffe68c"]), 700);
  });
  $("#openBox").addEventListener("click", () => {
    const all = [];
    for (let i = 0; i < P.boxPacks; i++) all.push.apply(all, global.FA_openPack(Math.random));
    packsOpened += P.boxPacks;
    all.forEach(c => { if (c.rarity > bestPull) bestPull = c.rarity; });
    shake($("#packArt"));
    const score = c => c.rarity * 1e9 + (c.foil + 1) * 1e7 + c.level;
    const best = all.slice().sort((a, b) => score(b) - score(a)).slice(0, P.cards);
    $("#cards").innerHTML = best.map(cardHtml).join("");
    const counts = P.rarities.map((_, i) => all.filter(c => c.rarity == i).length);
    const top = all.reduce((a, c) => c.level > a.level ? c : a, all[0]);
    $("#boxSum").innerHTML = "<b>" + P.boxPacks + " packs, " + all.length + " cards:</b> " + counts.map((n, i) => n ? '<span style="color:' + rarHex(i) + '">' + n + " " + P.rarities[i].name + "</span>" : "")
      .filter(Boolean).join(" · ") + " · " + all.filter(c => c.foil >= 0).length + " foiled · highest: " + esc(E[top.id].name) + " " + global.FA_num(top.level) + "<small>Showing the five best.</small>";
    packStats();
    if (best[0].rarity >= 3) setTimeout(() => confetti($("#cards"), 120, [rarHex(best[0].rarity), "#ffffff"]), 700);
  });

  // ---------- LUCK: foils ----------
  const fTotal = FOILS.reduce((a, f) => a + f.weight, 0);
  $("#foils").innerHTML = FOILS.map((f, i) => '<div class="foil slot-card" style="--fc:' + hexOf(f.color) + '">' + tomeImg("sharpness", i, 48) +
    '<img class="sheet" src="' + tex("foil_sheet_" + f.id) + '" alt="" />' + "<b>" + f.name + '</b><span class="mult">x' + f.mult + "</span><small>" + pct(f.weight / fTotal) + " of foils</small></div>").join("");

  // ---------- LUCK: Sealed Gear Caches (FA_cacheQuality + FA_makeRelic) ----------
  const CQ = FA.caches.qualities;
  const QHEX = { rare: "#4fe3e8", epic: "#d48cff", legendary: "#ffb84d", mythic: "#ff5a5a" };
  let cacheLvl = 0;
  const cacheOdds = () => {
    $("#cacheLvlOut").textContent = cacheLvl;
    oddsBar($("#cacheOdds"), CQ.map(q => q.weight * Math.pow(1 + q.growth, cacheLvl)), CQ.map(q => QHEX[q.quality]), CQ.map(q => q.quality));
  };
  $("#cacheLvl").addEventListener("input", e => { cacheLvl = +e.target.value; cacheOdds(); });
  cacheOdds();
  const relicHtml = (relic, art) => {
    const ench = Object.keys(relic.enchants).map(id => "<li>" + esc(E[id].name) + " <b>" + global.FA_num(relic.enchants[id]) + "</b></li>").join("");
    const gems = relic.gems.map(g => '<img src="' + tex(global.FA_gemId(g[0], g[1]).replace("kubejs:", "")) + '" alt="" title="' + esc(FA.gems.tiers[g[1] - 1].name + " " + FA.gems.types[g[0]].name) + '" width="20" height="20" />').join("");
    return '<div class="relic-card" style="--qc:' + QHEX[relic.quality] + '">' + (art ? '<p class="art-name" style="color:' + hexOf(art.color) + '">' + esc(art.name) + ", " + esc(art.title) + "</p>" : "") +
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
    if (q == "legendary" || q == "mythic") confetti($("#relic"), q == "mythic" ? 120 : 50, [QHEX[q], "#ffffff"]);
  });

  // ---------- LUCK: jackpots and rare mobs ----------
  const J = FA.jackpot;
  $("#tail").innerHTML = [10, 100, 1000, 10000].map(k => "<li><b>x" + global.FA_num(k) + "</b> the level <span>" + oneIn(Math.pow(k, -J.levelAlpha)) + "</span></li>").join("") +
    "<li><b>" + global.FA_num(J.levelCap) + "</b> the cap <span>it happens</span></li>";
  $("#chestJackpot").textContent = pct(J.chestChance);
  const V = FA.variants;
  const HIT = { fire: "hits set you on fire", frost: "hits slow you", storm: "hits call lightning (+" + Math.round(V.stormBonus * 100) + "%)" };
  $("#variants").innerHTML = V.types.map(t => '<div class="variant" style="--vc:' + hexOf(t.color) + '"><b>' + (t.symbol ? t.symbol + " " : "") + t.name + "</b><span>" + oneIn(t.chance) +
    (t.elemental ? " hostile mobs" : " mobs") + "</span><small>" + (t.onHit ? HIT[t.onHit] + " · " : t.glow ? "glows through walls · " : "sparkles · ") + "x" + t.xp + " XP · " +
    t.lootRolls + " loot roll" + (t.lootRolls > 1 ? "s" : "") + "</small></div>").join("");

  // ---------- GEAR: tiers (config ascendite + gearTiers; diamond and netherite for comparison) ----------
  const armorSum = a => a.protection.reduce((x, y) => x + y, 0);
  const TIERS = [
    { id: "ascendite", name: "Ascendite", t: FA.ascendite.tools, a: FA.ascendite.armor, note: "Between iron and diamond. Mines diamonds." },
    { name: "Diamond", ref: true, t: { uses: 1561, attackDamageBonus: 3 }, a: { protection: [3, 6, 8, 3], toughness: 2 } },
    { id: "voidsteel", note: "Between diamond and netherite." },
    { name: "Netherite", ref: true, t: { uses: 2031, attackDamageBonus: 4 }, a: { protection: [3, 6, 8, 3], toughness: 3 } },
    { id: "celestium", note: "Past netherite." },
    { id: "ascendium", note: "The endgame metal." }
  ].map(T => T.id && FA.gearTiers[T.id] ? Object.assign({ name: FA.gearTiers[T.id].name, t: FA.gearTiers[T.id].tools, a: FA.gearTiers[T.id].armor }, T) : T);
  const STATS = [["Durability", T => T.t.uses, v => global.FA_num(v)], ["Damage", T => T.t.attackDamageBonus, v => "+" + v],
    ["Armor", T => armorSum(T.a), v => v], ["Toughness", T => T.a.toughness, v => v]];
  const maxes = STATS.map(s => Math.max.apply(null, TIERS.map(s[1])));
  $("#tiers").innerHTML = TIERS.map(T => '<div class="tier slot-card' + (T.ref ? " ref" : " t-" + T.id) + '">' +
    (T.ref ? '<div class="tier-icons"><span class="ref-dot"></span></div>' : '<div class="tier-icons"><img src="' + tex(T.id + "_sword") + '" alt="" /><img src="' + tex(T.id + "_chestplate") + '" alt="" /><img src="' + tex(T.id + "_ingot") + '" alt="" /></div>') +
    "<b>" + T.name + "</b><small>" + (T.ref ? "vanilla, for comparison" : esc(T.note)) + "</small>" +
    STATS.map((s, i) => { const v = s[1](T); return '<div class="bar"><span>' + s[0] + '</span><i style="--w:' + (v / maxes[i] * 100).toFixed(1) + '%"></i><em>' + s[2](v) + "</em></div>"; }).join("") + "</div>").join("");

  // ---------- GEAR: condition, attachments, artifacts ----------
  const G = FA.condition.grades;
  oddsBar($("#grades"), G.map(g => g.weight), G.map(g => hexOf(g.color)), G.map(g => g.name));
  $("#rollGrade").addEventListener("click", () => {
    const i = global.FA_rollCondition(Math.random), g = G[i];
    $("#gradeOut").innerHTML = '<b style="color:' + hexOf(g.color) + '">' + g.name + "</b> · " + (g.damage > 1 ? "+" : g.damage == 1 ? "±" : "") + Math.round((g.damage - 1) * 100) + "% damage · " +
      (g.defense < 1 ? "-" : g.defense > 1 ? "+" : "±") + Math.abs(Math.round((g.defense - 1) * 1000) / 10) + "% damage taken per piece" + ' <span class="dim">(' + pct(g.weight / G.reduce((a, x) => a + x.weight, 0)) + " of gear)</span>";
    if (i == G.length - 1) confetti($("#gradeOut"), 50, ["#ffb84d", "#ffffff"]);
  });
  $("#attach").innerHTML = FA.attachments.list.map(a => '<button class="att" data-id="' + a.id + '" title="' + esc(a.name) + '" type="button"><img src="' + tex("attachment_" + a.id) + '" alt="' + esc(a.name) + '" /></button>').join("");
  const showAtt = id => { const a = global.FA_attachmentDef(id); $("#attachOut").innerHTML = "<b>" + esc(a.name) + '</b> <span class="dim">(' + a.fits + ")</span> " + esc(global.FA_attachmentEffect(a)); };
  ["mouseover", "focusin", "click"].forEach(ev => $("#attach").addEventListener(ev, e => { const b = e.target.closest(".att"); if (b) showAtt(b.dataset.id); }));
  showAtt("jetpack");
  const AR = FA.artifacts;
  $("#awakenLvls").textContent = global.FA_num(AR.awakenLevels);
  $("#relicArt").textContent = "Or get lucky: " + Math.round(AR.relicChance * 100) + "% of Mythic relics drop already awakened.";
  let artKind = "weapon", serial = 0;
  const KIND_ICON = { weapon: "ascendium_sword", armor: "ascendium_chestplate", tool: "ascendium_pickaxe" };
  const awaken = () => {
    const a = global.FA_rollArtifact(Math.random, artKind), el = $("#artCard");
    serial++;
    el.style.setProperty("--ac", hexOf(a.color));
    el.innerHTML = '<img src="' + tex(KIND_ICON[artKind]) + '" alt="" /><small class="rar">Artifact · No. ' + serial + "</small><b>" + esc(a.name) + '</b><span class="title">' + esc(a.title) +
      "</span><i>“" + esc(a.lore) + "”</i><ul>" + a.quirks.map(q => "<li>" + esc(global.FA_quirkText(q.id, q.power)) + "</li>").join("") + "</ul>";
    el.classList.remove("flip"); void el.offsetWidth; el.classList.add("flip");
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

  // ---------- ENCHANTS: sub-pages ----------
  function showSub(sub) {
    if (!document.querySelector('.sub-page[data-sub="' + sub + '"]')) sub = "browse";
    document.querySelectorAll(".sub-page").forEach(p => { p.hidden = p.dataset.sub != sub; });
    document.querySelectorAll("#enchTabs .chip").forEach(b => b.classList.toggle("is-active", b.dataset.sub == sub));
    if (sub == "ultimates") replay();
  }
  $("#enchTabs").addEventListener("click", e => {
    const b = e.target.closest(".chip");
    if (!b) return;
    history.replaceState(null, "", "#enchants" + (b.dataset.sub == "browse" ? "" : "/" + b.dataset.sub));
    showSub(b.dataset.sub);
  });

  // ---------- ENCHANTS: browse (24 a page, so it's never a long scroll) ----------
  const GROUPS = [
    ["Damage", ["damage", "critical", "reaper", "cleave"]], ["On hit", ["lifesteal", "hit_effect", "ignite", "hit_heal", "self_effect"]],
    ["On kill", ["plunder", "kill_heal", "xp", "drop"]], ["Defense", ["resist", "vitality", "retribution", "dodge", "undying"]],
    ["Passive", ["regen", "passive", "magnet", "repair"]], ["Utility", ["attribute", "mining", "eternal"]]
  ];
  const groupOf = id => E[id].ultimate ? "Ultimate" : E[id].mythic ? "Mythic" : E[id].triad ? "Triad" : E[id].type == "fusion" ? "Fusion" : (GROUPS.find(g => g[1].includes(E[id].type)) || ["Other"])[0];
  const nice = item => item.indexOf("kubejs:tome_") === 0 && E[item.slice(12)] ? "Tome of " + E[item.slice(12)].name : item.replace(/^[a-z_]+:/, "").replace(/_/g, " ");
  const catalysts = id => {
    const r = FA_RECIPES[id];
    if (!r) return "";
    const counts = {};
    r.items.forEach(i => { counts[i] = (counts[i] || 0) + 1; });
    return Object.keys(counts).map(i => (counts[i] > 1 ? counts[i] + "× " : "") + nice(i)).join(", ");
  };
  // Total Essence to reach a level: one running total per cost multiplier, extended as needed
  const cum = {};
  const totalCost = (id, level) => {
    const key = global.FA_costMult(id);
    const arr = cum[key] || (cum[key] = [0]);
    for (let k = arr.length - 1; k < level; k++) arr.push(arr[k] + global.FA_cost(k, id));
    return arr[level];
  };
  const PRESETS = [1, 10, 100, 1000, 3000, 100000];
  const PAGE = 24;
  const toLevel = v => Math.max(1, Math.round(Math.pow(10, (v / 1000) * 5)));
  const toSlider = l => Math.round(Math.log10(l) / 5 * 1000);
  let level = 1, group = "All", query = "", foilMult = 1, page = 0;
  const chipNames = ["All"].concat(GROUPS.map(g => g[0]), ["Mythic", "Ultimate", "Fusion", "Triad"]);
  $("#chips").innerHTML = chipNames.map(g => '<button class="chip' + (g == "All" ? " is-active" : "") + '" data-g="' + g + '" type="button">' + g + " <small>" +
    (g == "All" ? ids.length : ids.filter(id => groupOf(id) == g).length) + "</small></button>").join("");
  $("#presets").innerHTML = PRESETS.map(p => '<button class="chip" data-l="' + p + '" type="button">' + global.FA_num(p) + "</button>").join("");
  $("#foilPick").innerHTML = '<button class="chip is-active" data-m="1" type="button">No foil</button>' + FOILS.map(f => '<button class="chip" data-m="' + f.mult + '" type="button">' + f.name + " x" + f.mult + "</button>").join("");
  const searchText = {};
  ids.forEach(id => { searchText[id] = (E[id].name + " " + E[id].type + " " + groupOf(id) + " " + catalysts(id) + " " + global.FA_describe(id, 100)).toLowerCase(); });
  const cardOf = id => {
    const e = E[id];
    const want = Math.max(1, Math.round(level * foilMult)), l = e.maxLevel ? Math.min(e.maxLevel, want) : want;
    return '<article class="ench slot-card' + (e.ultimate ? " is-ultimate" : e.mythic ? " is-mythic" : e.triad ? " is-triad" : e.type == "fusion" ? " is-fusion" : "") + '">' +
      '<img src="' + tex("tome_" + id) + '" alt="" loading="lazy" />' + "<h3>" + (e.ultimate ? "☄ " : e.mythic ? "✦ " : "") + esc(e.name) +
      '<span class="lv">' + (e.maxLevel == 1 ? "" : global.FA_num(l)) + "</span></h3>" + '<p class="what">' + esc(global.FA_describe(id, l)) + "</p>" +
      '<span class="cost">' + global.FA_num(totalCost(id, l)) + " Essence to reach</span>" +
      '<div class="meta"><span class="tag">' + groupOf(id) + '</span><span class="tag">' + (e.maxLevel ? "max " + e.maxLevel : "no cap") + "</span>" +
      (catalysts(id) ? '<span class="tag">' + esc(catalysts(id)) + "</span>" : "") + "</div></article>";
  };
  function render() {
    $("#lvlOut").textContent = global.FA_num(level) + (foilMult > 1 ? " → " + global.FA_num(Math.round(level * foilMult)) : "");
    const shown = ids.filter(id => (group == "All" || groupOf(id) == group) && (!query || searchText[id].includes(query)));
    const pages = Math.max(1, Math.ceil(shown.length / PAGE));
    page = Math.min(page, pages - 1);
    $("#grid").innerHTML = shown.length ? shown.slice(page * PAGE, page * PAGE + PAGE).map(cardOf).join("") : '<p class="empty">No enchants match that. Try "fire", "heal" or "diamond".</p>';
    $("#count").textContent = shown.length + " of " + ids.length + " enchants";
    let nums = [];
    for (let p = 0; p < pages; p++) if (p < 2 || p > pages - 3 || Math.abs(p - page) <= 1) nums.push(p); else if (nums[nums.length - 1] !== "…") nums.push("…");
    $("#pager").innerHTML = pages < 2 ? "" : '<button class="chip" data-p="' + (page - 1) + '"' + (page == 0 ? " disabled" : "") + ' type="button">‹ Prev</button>' +
      nums.map(p => p === "…" ? "<span>…</span>" : '<button class="chip' + (p == page ? " is-active" : "") + '" data-p="' + p + '" type="button">' + (p + 1) + "</button>").join("") +
      '<button class="chip" data-p="' + (page + 1) + '"' + (page >= pages - 1 ? " disabled" : "") + ' type="button">Next ›</button>';
  }
  $("#pager").addEventListener("click", e => {
    const b = e.target.closest("button[data-p]");
    if (!b || b.disabled) return;
    page = +b.dataset.p;
    render();
    $("#enchTabs").scrollIntoView({ block: "start" });
  });
  const slider = $("#lvl");
  const setLevel = l => { level = l; slider.value = toSlider(l); render(); };
  let pending = null;
  slider.addEventListener("input", () => {
    if (pending === null) requestAnimationFrame(() => { const l = pending; pending = null; setLevel(l); });
    pending = toLevel(+slider.value);
  });
  $("#presets").addEventListener("click", e => { const b = e.target.closest("button"); if (b) setLevel(+b.dataset.l); });
  $("#foilPick").addEventListener("click", e => {
    const b = e.target.closest("button");
    if (!b) return;
    foilMult = +b.dataset.m;
    $("#foilPick").querySelectorAll("button").forEach(x => x.classList.toggle("is-active", x === b));
    render();
  });
  $("#chips").addEventListener("click", e => {
    const b = e.target.closest("button");
    if (!b) return;
    group = b.dataset.g; page = 0;
    $("#chips").querySelectorAll(".chip").forEach(c => c.classList.toggle("is-active", c === b));
    render();
  });
  let qTimer;
  $("#search").addEventListener("input", e => { clearTimeout(qTimer); qTimer = setTimeout(() => { query = e.target.value.trim().toLowerCase(); page = 0; render(); }, 80); });
  setLevel(1);

  // ---------- ENCHANTS: mythic ----------
  const F = FA.fragments;
  $("#fragRates").textContent = "Elites drop one " + Math.round(global.FA_fragmentChance(1, true) * 100) + "% of the time at 1,000 blocks out, " + Math.round(global.FA_fragmentChance(5, true) * 100) + "% at 2,000, climbing on the same curve as the mobs (up to " + Math.round(F.eliteChanceCap * 100) + "%).";
  $("#mythicGrid").innerHTML = ids.filter(id => E[id].mythic).map(id => '<div class="myth slot-card"><img src="' + tex("tome_" + id) + '" alt="" /><b>' + esc(E[id].name) + "</b><span>" +
    esc(global.FA_describe(id, E[id].maxLevel || 10)) + "</span></div>").join("");

  // ---------- ENCHANTS: ultimates, with a top-down replay of Thunder God's Waltz ----------
  const ults = ids.filter(id => E[id].ultimate);
  const showUlt = id => {
    const e = E[id];
    $("#ultName").textContent = "☄ " + e.name;
    $("#ultDesc").textContent = e.desc + ". Level 1: " + e.power + "x your hit per strike, every " + global.FA_ultCooldown(e, 1) + "s.";
    document.querySelectorAll(".ult").forEach(b => b.classList.toggle("is-active", b.dataset.id == id));
  };
  $("#ultGrid").innerHTML = ults.map(id => '<button class="ult slot-card" data-id="' + id + '" type="button"><b><img src="' + tex("tome_" + id) + '" alt="" />' + esc(E[id].name) +
    "</b><span>" + esc(E[id].desc) + "</span><small>" + E[id].power + "x per strike · " + E[id].cooldown + "s cooldown</small></button>").join("");
  $("#ultGrid").addEventListener("click", e => { const b = e.target.closest(".ult"); if (b) showUlt(b.dataset.id); });
  showUlt("thunder_waltz");
  const dot = $("#heroDot"), slashes = $("#slashes"), bolts = $("#bolts"), boom = $("#boom"), stepText = $("#replayStep");
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
      bolts.insertAdjacentHTML("beforeend", '<polyline class="bolt" points="' + (ox - 6) + ",-56 " + (ox + 4) + ",-36 " + (ox - 4) + ",-28 " + (ox + 6) + ",-8 " + (ox - 1) + ',0" />');
    });
    at(2950, () => { boom.classList.remove("go"); void boom.getBoundingClientRect(); boom.classList.add("go"); stepText.textContent = "Boom"; });
    at(3600, () => { dot.style.opacity = 1; move(-48, 30, 3.2); stepText.textContent = "Back where you started"; });
  }
  $("#replayBtn").addEventListener("click", replay);

  // ---------- ENCHANTS: chaos forge (the game's generator: global.FA_rollEnchant) ----------
  const C = FA.chaos;
  const cTotal = C.rarities.reduce((a, R) => a + R.weight, 0);
  oddsBar($("#odds"), C.rarities.map(R => R.weight), C.rarities.map(R => hexOf(R.color)), C.rarities.map(R => R.name));
  $("#oddsKey").innerHTML = C.rarities.map(R => '<span><b style="color:' + hexOf(R.color) + '">' + R.name + "</b> " + pct(R.weight / cTotal) + "</span>").join("");
  let rollLevel = 1, current = null, rolled = 0, best = -1;
  $("#rollLevels").innerHTML = [1, 10, 100, 1000].map(l => '<button class="chip' + (l == 1 ? " is-active" : "") + '" data-l="' + l + '" type="button">' + global.FA_num(l) + "</button>").join("");
  const randomSeed = () => 1 + Math.floor(Math.random() * 2147483645);
  const lines = (roll, l) => roll.affixes.map(a => global.FA_describeDef(a, l));
  function showRoll(seed, animate) {
    const roll = global.FA_rollEnchant(seed), R = C.rarities[roll.rarity], card = $("#rollCard");
    current = seed;
    card.className = "roll-card";
    card.style.setProperty("--rc", hexOf(R.color));
    card.innerHTML = '<span class="rar">' + R.name + "</span><h3>" + esc(roll.name) + '</h3><span class="lvl">Level ' + global.FA_num(rollLevel) + " · upgrades cost x" + R.costMult +
      "</span><ul>" + lines(roll, rollLevel).map(t => "<li>" + esc(t) + "</li>").join("") + '</ul><span class="seed">seed ' + roll.seed + "</span>";
    if (animate) { void card.offsetWidth; card.classList.add("flip"); }
    return roll;
  }
  const rollStats = roll => {
    rolled++;
    if (roll.rarity > best) best = roll.rarity;
    $("#rollStats").textContent = rolled + " rolled this visit · best: " + C.rarities[best].name;
  };
  $("#rollOne").addEventListener("click", () => {
    shake($(".chaos-tome"));
    const roll = showRoll(randomSeed(), true);
    rollStats(roll);
    if (roll.rarity >= 3) confetti($("#rollCard"), roll.rarity == 4 ? 140 : 60, [hexOf(C.rarities[roll.rarity].color), "#ffffff"]);
  });
  $("#rollMany").addEventListener("click", () => {
    let top = null;
    $("#rollGrid").innerHTML = Array.from({ length: 12 }, (_, i) => {
      const seed = randomSeed(), roll = global.FA_rollEnchant(seed), R = C.rarities[roll.rarity];
      rollStats(roll);
      if (!top || roll.rarity > top.rarity) top = roll;
      return '<button class="roll-mini slot-card" style="--rc:' + hexOf(R.color) + ";--d:" + i * 45 + 'ms" data-seed="' + seed + '" type="button"><small>' + R.name + "</small><b>" + esc(roll.name) + "</b>" +
        lines(roll, 1).map(t => "<span>" + esc(t) + "</span>").join("") + "</button>";
    }).join("");
    if (top) showRoll(top.seed, true);
  });
  $("#rollGrid").addEventListener("click", e => { const b = e.target.closest(".roll-mini"); if (b) showRoll(+b.dataset.seed, true); });
  $("#rollLevels").addEventListener("click", e => {
    const b = e.target.closest("button");
    if (!b) return;
    rollLevel = +b.dataset.l;
    $("#rollLevels").querySelectorAll("button").forEach(x => x.classList.toggle("is-active", x === b));
    if (current) showRoll(current, false);
  });
  $("#seedForm").addEventListener("submit", e => { e.preventDefault(); const n = parseInt($("#seedIn").value, 10); if (n > 0) showRoll(n, true); });
  showRoll(1337, false);

  // ---------- MACHINES: the Modular Engine builder (same math as the mod, numbers from config machines.engine) ----------
  const M = FA.machines, EN = M.engine;
  $("#cpb").textContent = EN.coilsPerBurner;
  const engine = { coils: 4, gears: 1, burners: 1 };
  const MODS = [["coils", "engine_coil_side", "Coil", "+" + global.FA_num(EN.suPerCoil) + " SU each"], ["gears", "engine_gearbox_side", "Gearbox", "next speed: " + EN.rpm.join(" / ") + " RPM"],
    ["burners", "engine_burner_side_lit", "Burner", "burns fuel, powers " + EN.coilsPerBurner + " Coils"]];
  $("#engineCtl").innerHTML = MODS.map(m => '<div class="mod-ctl slot-card"><img src="fa/blocks/' + m[1] + '.png" alt="" /><div><b>' + m[2] + "</b><small>" + m[3] + '</small></div>' +
    '<div class="stepper"><button class="btn sm" data-k="' + m[0] + '" data-d="-1" type="button">−</button><output id="eng_' + m[0] + '"></output><button class="btn sm" data-k="' + m[0] + '" data-d="1" type="button">+</button></div></div>').join("");
  const engineDraw = () => {
    const modules = engine.coils + engine.gears + engine.burners;
    const powered = Math.min(engine.coils, engine.burners * EN.coilsPerBurner);
    const rpm = powered > 0 ? EN.rpm[Math.min(engine.gears, EN.rpm.length - 1)] : 0;
    const su = powered * EN.suPerCoil;
    const needBurners = Math.ceil(engine.coils / EN.coilsPerBurner);
    const lit = Math.min(engine.burners, needBurners);
    MODS.forEach(m => { $("#eng_" + m[0]).textContent = engine[m[0]]; });
    const img = (f, cls) => '<img src="fa/blocks/' + f + '.png" alt="" class="' + (cls || "") + '" />';
    let html = img("engine_core_front");
    for (let i = 0; i < engine.coils; i++) html += img("engine_coil_side", i < powered ? "" : "off");
    for (let i = 0; i < engine.gears; i++) html += img("engine_gearbox_side");
    for (let i = 0; i < engine.burners; i++) html += img(i < lit ? "engine_burner_side_lit" : "engine_burner_side", i < lit ? "lit" : "");
    $("#engineBuild").innerHTML = html;
    $("#engineOut").innerHTML = [[rpm, "RPM"], [global.FA_num(su), "SU"], [lit, "burning"], [Math.round(lit * 60 / 80 / EN.fuelMultiplier * 10) / 10, "coal / min"]]
      .map(o => '<div class="slot-card"><b>' + o[0] + "</b><small>" + o[1] + "</small></div>").join("");
    $("#engineNote").textContent = modules > EN.maxModules ? "Too many modules: an engine takes up to " + EN.maxModules + "."
      : engine.coils == 0 ? "No coils, no power." : engine.burners == 0 ? "Add a Burner and some fuel." : powered < engine.coils ? (engine.coils - powered) + " coils have no Burner: add " + (needBurners - engine.burners) + " more."
      : engine.burners > needBurners ? (engine.burners - needBurners) + " spare Burner" + (engine.burners - needBurners > 1 ? "s" : "") + " waiting (they only burn when needed)."
      : "A water wheel makes 256 SU. This makes " + global.FA_num(su) + ".";
  };
  $("#engineCtl").addEventListener("click", e => {
    const b = e.target.closest("button[data-k]");
    if (!b) return;
    engine[b.dataset.k] = Math.max(0, Math.min(EN.maxModules, engine[b.dataset.k] + +b.dataset.d));
    engineDraw();
  });
  engineDraw();

  // ---------- MACHINES: every block (recipes and numbers from config.machines) ----------
  const itemName = id => {
    const t = id.indexOf("kubejs:tome_") === 0 && E[id.slice(12)];
    return t ? "Tome of " + t.name : id.replace(/^[a-z_]+:/, "").replace(/_/g, " ").replace(/\b\w/g, c => c.toUpperCase());
  };
  const vaultSlots = 108 * (M.vaultUpgrades.length + 1), SM = M.smelter, FT = M.fusionTable;
  const MACHINES = [
    ["super_smelter", "Super Smelter", "A Create machine: shaft in the back. Smelts " + SM.tiers.map(t => t[0]).join(" / ") + " items at once by tier (upgrade with " +
      SM.upgrades.map(u => u[1] + " " + itemName(u[0])).join(", then ") + "), and the faster it spins, the faster it goes: at 64 RPM a batch takes " + SM.cookTicksAt64Rpm / 20 + "s.",
      { f: "front_lit", s: "side", t: "top", b: "back" }],
    ["salvager", "Salvager", "Loot in, Ascension Cores out, at the same prices as /ascension sell. Anything it can't sell passes straight through. It never grinds up an Artifact."],
    ["tome_fusion_table", "Tome Fusion Table", "Merge copies of a tome: their levels add up, foils baked in. Or put in a Fusion's parent tomes and " + FT.fusionCores + " core (" + FT.triadCores +
      " for a Triad) and press Fuse. It shows the result before you commit.", { f: "side", s: "side", t: "top" }],
    ["ascension_vault", "Ascension Vault", "A chest with search, sort and a scroll bar. 108 slots, +108 per upgrade (" + M.vaultUpgrades.map(u => u[1] + " " + itemName(u[0])).join(", ") + "), " + vaultSlots + " max."],
    ["card_binder", "Card Binder", "Holds 54 tome cards, counts your collection, and floats your best card above the block.", { f: "side", s: "side", t: "top" }],
    ["essence_forge", "Essence Forge", "Gear in the slot, Liquid Essence piped into its 16-bucket tank: it levels the gear like the Ascension Altar, once a second."],
    ["pack_opener", "Pack Opener", "Hopper in Enchantment Packs: it opens one every " + (M.openerSeconds == 1 ? "second" : M.openerSeconds + " seconds") + " into 27 slots and flips the cards on screen."],
    ["ascension_workbench", "Ascension Workbench", "Two blocks wide. Gear on top, a tome, gem, attachment or Chaos Tome below, Apply. Star Up adds Legendary stars, up to " + FA.legendary.maxStars + ": each one a legendary effect and more power."],
    ["mini_crusher", "Mini Crusher", "Create's crushing recipes with no crushing wheels and no rotation: a motor (x" + M.miniCrusher.motors.map(m => m.speed).join(", x") + " speed) and a battery for power."],
    ["storage_terminal", "Universal Storage Terminal", "Link up to " + M.storage.maxLinks + " chests to it with a Storage Linker" + (M.storage.range > 0 ? " (within " + M.storage.range + " blocks)" : "") +
      " and use them as one big chest: search, Sort across every chest, scroll, shift-click in and out. Hoppers can feed it."],
    ["engine_core", "Engine Core", "The heart of a Modular Engine. Its front face is the shaft output. Right-click it for the engine's screen: speed, power, modules and what to add next.", { f: "front", s: "side", t: "top" }],
    ["engine_coil", "Coil Module", "+" + EN.suPerCoil + " SU of stress capacity, while a Burner powers it. Its screen shows how many coils are powered.", { f: "side", s: "side", t: "top" }],
    ["engine_gearbox", "Gearbox Module", "Each one takes the engine to its next speed: " + EN.rpm.join(", ") + " RPM. Same power, faster shaft. Its screen shows the speed ladder.", { f: "side", s: "side", t: "top" }],
    ["engine_burner", "Burner Module", "Holds any furnace fuel (hoppers and chutes can fill it). Burns only while the engine needs it. Its screen has a fuel slot, a flame gauge and the time left.", { f: "side_lit", s: "side", t: "top" }]
  ];
  const recipeHtml = r => {
    const cell = ch => {
      if (ch == " ") return "<i></i>";
      const id = r[1][ch];
      return id.indexOf("kubejs:") === 0 ? '<i title="' + esc(itemName(id)) + '"><img src="' + tex(id.slice(7)) + '" alt="" /></i>' : '<i title="' + esc(itemName(id)) + '"><b>' + ch + "</b></i>";
    };
    const legend = Object.keys(r[1]).filter(ch => r[1][ch].indexOf("kubejs:") !== 0).map(ch => "<b>" + ch + "</b> " + esc(itemName(r[1][ch]))).join(" · ");
    return '<div class="recipe"><div class="craft">' + r[0].join("").split("").map(cell).join("") + "</div><small>" + legend + "</small></div>";
  };
  $("#machinesGrid").innerHTML = MACHINES.map(m => {
    const faces = Object.assign({ f: "front", s: "side", t: "top" }, m[3] || {}), base = "fa/blocks/" + m[0] + "_";
    const face = (cls, k) => '<i class="' + cls + '" style="background-image:url(' + base + k + '.png)"></i>';
    return '<article class="machine slot-card"><div class="cube-wrap"><div class="cube">' + face("f", faces.f) + face("r", faces.s) + face("l", faces.s) +
      face("b", faces.b || faces.s) + face("t", faces.t) + "</div></div><h3>" + m[1] + "</h3><p>" + esc(m[2]) + "</p>" + (M.recipes[m[0]] ? recipeHtml(M.recipes[m[0]]) : "") + "</article>";
  }).join("");
  const ES = FA.easyStart;
  $("#easy").innerHTML = [
    [ES.casingsPerLog > 0, "Andesite Casing by hand", "any log + Andesite Alloy = " + ES.casingsPerLog + " casings, no Deployer needed"],
    [ES.beltFromLeather, "Belts from leather", "3 leather = a Mechanical Belt, no kelp farm"],
    [ES.cogwheelsPerAlloy > 0, "Cogwheels in bulk", "8 planks around an Andesite Alloy = " + ES.cogwheelsPerAlloy + " cogwheels"],
    [ES.shaftsPerAlloy > 0, "Shafts in bulk", "Andesite Alloy + a stick = " + ES.shaftsPerAlloy + " shafts"],
    [true, "Andesite Alloy, brass, a Blaze Burner", "Ascendite makes them early: no Nether trip, no sequenced assembly"],
    [true, "Ascension Cores by hand", "from day one, from amethyst, or from Ascension Shards"],
    [true, "Automated cores, no heat", "cobblestone, press, Millstone, a fan through water, then " + FA.ascendite.mixerNuggets + " nuggets + redstone in a Mechanical Mixer"]
  ].filter(x => x[0]).map(x => '<li class="slot-card"><b>' + x[1] + "</b><span>" + esc(x[2]) + "</span></li>").join("");

  // ---------- MACHINES: Factory Room TNT (shape from global.FA_roomPlan / FA_roomKind, like the game) ----------
  const RC = FA.roomCharges;
  const rgb = c => "rgb(" + c.join(",") + ")";
  let roomPick = [RC.styles[0].id, RC.sizes[RC.sizes.length - 1].id];
  $("#roomPick").innerHTML = RC.styles.map(st => RC.sizes.map(sz => '<button class="att" type="button" data-st="' + st.id + '" data-sz="' + sz.id + '" title="' +
    sz.size[0] + "x" + sz.size[1] + " " + esc(st.name) + ' Light Factory TNT"><img src="' + tex("room_tnt_" + st.id + "_" + sz.id) + '" alt="" /></button>').join("")).join("");
  const drawRoom = () => {
    const st = RC.styles.find(x => x.id == roomPick[0]), sz = RC.sizes.find(x => x.id == roomPick[1]);
    const plan = global.FA_roomPlan(sz.id, 0, 0, 0, 0, -1), c = $("#roomMap"), g = c.getContext("2d");
    c.width = plan.cols; c.height = plan.rows + 1;
    g.fillStyle = "#3a3a3a"; g.fillRect(0, 0, c.width, c.height);
    for (let v = 0; v < plan.rows; v++) for (let col = 0; col < plan.cols; col++) {
      const u = plan.umin - 1 + col, y = plan.rows - 1 - v;   // far wall at the top, you at the bottom
      const floor = global.FA_roomKind(plan, u, v, 1), roof = global.FA_roomKind(plan, u, v, plan.H + 1);
      g.fillStyle = floor == "air" && v == 0 ? "#1d140d" : roof == "light" ? "#ffe680" : floor == "air" ? "#4a3a2a" : rgb(st.color);
      g.fillRect(col, y, 1, 1);
    }
    g.fillStyle = "#ff5a5a"; g.fillRect(-plan.umin + 1, plan.rows, 1, 1);
    c.style.setProperty("--shell", rgb(st.color));
    document.querySelector(".k-shell").style.background = rgb(st.color);
    let lights = 0;
    for (let i = 0; i < plan.total; i++) if (global.FA_roomSpot(plan, i).kind == "light") lights++;
    const idx = RC.sizes.indexOf(sz);
    $("#roomInfo").innerHTML = "<b>" + sz.size[0] + "x" + sz.size[1] + " " + esc(st.name) + " Light Factory TNT</b><br>" + sz.size[0] + " x " + sz.size[1] + " inside, " + sz.size[2] +
      " high · walls of " + esc(itemName(st.shell)) + " · " + lights + " " + esc(itemName(st.light)) + "s in the roof<br>Made from " +
      (idx == 0 ? "TNT, 4 " + esc(itemName(st.shell)) + " and 2 " + esc(itemName(st.light)) : sz.craft + " of the " + RC.sizes[idx - 1].size[0] + "x" + RC.sizes[idx - 1].size[1]);
    $("#roomPick").querySelectorAll(".att").forEach(b => b.classList.toggle("is-active", b.dataset.st == st.id && b.dataset.sz == sz.id));
  };
  $("#roomPick").addEventListener("click", e => { const b = e.target.closest(".att"); if (b) { roomPick = [b.dataset.st, b.dataset.sz]; drawRoom(); } });
  drawRoom();

  // ---------- MACHINES: connected glass (a 3x3 wall: each pane's frame only where it has no neighbor) ----------
  const GLASS = [["factory_glass", "Factory"], ["ascendite_glass", "Ascendite"], ["gilded_glass", "Gilded"], ["smoked_glass", "Smoked"]];
  $("#glassRow").innerHTML = GLASS.map(g => {
    let tiles = "";
    for (let y = 0; y < 3; y++) for (let x = 0; x < 3; x++) {
      const code = (y > 0 ? 1 : 0) | (x < 2 ? 2 : 0) | (y < 2 ? 4 : 0) | (x > 0 ? 8 : 0);   // top, right, bottom, left joined
      tiles += '<img src="fa/blocks/' + g[0] + "_" + code + '.png" alt="" />';
    }
    return '<figure><div class="glass-wall">' + tiles + "</div>" + g[1] + " Glass</figure>";
  }).join("");

  // ---------- WORLD: skinwalkers ----------
  const SKW = FA.skinwalkers;
  $("#skinOdds").textContent = "About " + oneIn(SKW.chance) + " animals spawning at danger " + SKW.minDanger + " or deeper. It watches you at least " +
    SKW.minStalkSeconds + " seconds first. Its true form is " + SKW.dangerBonus + " danger levels nastier than where it turned, and drops " + SKW.lootRolls +
    " extra loot rolls a tier better (and maybe a Mythic Fragment).";

  // ---------- WORLD: Bestiary (fa/mobs.js from tools/mob_art.py; parts from the config) ----------
  const MOBS = window.FA_MOBS || [];
  const partOf = id => FA.mobFood.foods.find(f => f.mobs.indexOf("factoryascension:" + id) >= 0);
  $("#bestiary").innerHTML = MOBS.map(m => {
    const p = partOf(m.id);
    const boss = /^BOSS/.test(m.where);
    return '<article class="beast slot-card' + (boss ? " is-boss" : "") + '"><span class="egg" style="--e1:' + m.egg[0] + ";--e2:" + m.egg[1] + '"></span><b>' + esc(m.name) + '</b><span class="where">' +
      esc(m.where) + "</span><p>" + esc(m.what) + "</p>" + (p ? '<span class="part-line"><img src="' + tex(p.raw) + '" alt="" /><img src="' + tex(p.cooked) + '" alt="" />' +
      esc(p.rawName) + " → " + esc(p.cookedName) + '<span class="dim">(' + esc(p.effect[0].replace("minecraft:", "").replace(/_/g, " ")) + ")</span></span>" : "") + "</article>";
  }).join("");

  // ---------- WORLD: danger zones (drag to travel) ----------
  const D = FA.danger;
  const ZONE_HEX = { green: "#5fd47a", yellow: "#f3d34a", gold: "#ffb84d", red: "#ff5a3d", dark_red: "#e04040", dark_purple: "#b06cf0", dark_gray: "#a0a0a0" };
  const MAX_DIST = 30000;
  const distAt = p => MAX_DIST * Math.pow(p, 3);   // slow start, so every zone gets room on the slider
  // Labels sit where those distances really are on the (cube-root) slider
  $("#safeR").textContent = global.FA_num(D.safeRadius);
  $("#perLvl").textContent = global.FA_num(D.blocksPerLevel);
  $("#railZones").innerHTML = [[0, "home"], [1000, "1k"], [2000, "2k"], [5000, "5k"], [10000, "10k"], [30000, "30k"]]
    .map(d => '<span style="left:' + (Math.cbrt(d[0] / MAX_DIST) * 100).toFixed(1) + '%">' + d[1] + "</span>").join("");
  const TIER_HEX = { white: "#e9e4de", green: "#5fd47a", aqua: "#4fe3e8", light_purple: "#d48cff", gold: "#ffb84d", red: "#ff5a5a" };
  $("#lootBar").innerHTML = FA.loot.tiers.map(t => '<i style="background:' + TIER_HEX[t.color] + '" title="' + t.name + '"></i>').join("");
  let lootLvl = 0;
  function journey(p) {
    const dist = distAt(Math.max(0, Math.min(1, p)));
    const lvl = global.FA_dangerAtDistance(dist, 0), zone = global.FA_zone(lvl), m = global.FA_dangerMult(lvl, false);
    $("#dist").textContent = Math.round(dist).toLocaleString("en-US");
    $("#dLevel").textContent = lvl;
    $("#dHealth").textContent = "x" + m.health.toFixed(1);
    $("#dDamage").textContent = "x" + m.damage.toFixed(1);
    $("#dLoot").textContent = "x" + m.loot.toFixed(1);
    $("#dElite").textContent = Math.round(Math.min(D.eliteChanceCap, D.eliteChancePerLevel * lvl) * 100) + "%";
    $("#zoneName").textContent = zone.name;
    $("#danger").style.setProperty("--zt", ZONE_HEX[zone.color] || "#ffb84d");
    lootLvl = lvl;
    const odds = global.FA_lootOdds(lvl);
    [...$("#lootBar").children].forEach((seg, i) => { seg.style.flexGrow = (odds[i] * 1000).toFixed(1); });
    const top = odds[4] + odds[5];
    $("#lootRead").textContent = lvl <= 0 ? "No loot drops in the Safe Lands" : Math.round(global.FA_lootChance(lvl) * 100) + "% of kills drop loot · Legendary or better: " + (top * 100).toFixed(top < 0.1 ? 1 : 0) + "% of drops";
  }
  $("#travel").addEventListener("input", e => journey(e.target.value / 1000));
  journey(0);
  const lootLabel = drop => {
    if (drop.kind == "relic") return drop.relic.name;
    const id = drop.id, te = id.indexOf("kubejs:tome_") === 0 && E[id.slice(12)];
    if (te) return (te.ultimate ? "Ultimate Tome: " : te.mythic ? "Mythic Tome of " : te.triad ? "Triad Tome of " : te.type == "fusion" ? "Fusion Tome of " : "Tome of ") + te.name;
    const cm = /^kubejs:condensed_essence_(\d+)$/.exec(id);
    if (cm) return global.FA_condensedName(+cm[1]);
    const gm = /^kubejs:gem_([a-z]+)_(\d)$/.exec(id);
    if (gm && FA.gems.types[gm[1]]) return FA.gems.tiers[+gm[2] - 1].name + " " + FA.gems.types[gm[1]].name;
    return itemName(id);
  };
  $("#lootRoll").addEventListener("click", () => {
    const roll = global.FA_rollLoot(Math.random, Math.max(1, lootLvl), 0), T = FA.loot.tiers[roll.tier], d = roll.drop;
    let html = '<b style="color:' + TIER_HEX[T.color] + '">' + T.name + "</b> " + (d.kind == "relic" ? "" : d.count + "x ") + esc(lootLabel(d));
    if (d.kind == "relic") html += '<span class="relic-ench">' + Object.keys(d.relic.enchants).map(id => esc(E[id].name) + " " + global.FA_num(d.relic.enchants[id])).join(" · ") + "</span>";
    $("#lootDrop").innerHTML = html;
    if (roll.tier >= 4) confetti($("#lootDrop"), roll.tier == 5 ? 120 : 60, [TIER_HEX[T.color], "#ffffff"]);
  });

  // ---------- WORLD: survival kit and mob food ----------
  const H = FA.hammer, S = FA.sell;
  $("#kitGrid").innerHTML = [
    ["ascendite_ore", "Glowing ores", "Ascendite and every valuable ore give off light, so veins show up in dark caves."],
    ["ascendite_rock_hammer", "Rock Hammers", "Smash cobblestone into gravel, nuggets and the odd Raw Ascendite. The Ascendite one is " + H.tools["kubejs:ascendite_rock_hammer"].luck + "x as lucky."],
    ["ascension_core", "/ascension sell", "Turn spare loot into cores or Liquid Essence. A Booster Box is worth " + S.items["kubejs:booster_box"] + " Essence. (The Salvager does it automatically.)"],
    ["ascendium_boots", "/sethome and /home", "One home, any dimension. Locked for " + FA.home.combatSeconds + "s after a fight, " + FA.home.cooldownSeconds + "s cooldown."],
    ["gem_sapphire_3", "Gems", "Up to " + FA.gems.sockets + " per item, for free levels: Chipped +" + FA.gems.tiers[0].levels + ", Flawless +" + FA.gems.tiers[1].levels + ", Perfect +" + FA.gems.tiers[2].levels + "."],
    ["ascension_singularity", "Zone titles", "Cross into a new zone and its name fills the screen, with a rumble that gets deeper the further out you go."]
  ].map(k => '<article class="kit-card slot-card"><img src="' + tex(k[0]) + '" alt="" width="40" height="40" /><b>' + k[1] + "</b><p>" + esc(k[2]) + "</p></article>").join("");
  const MF = FA.mobFood.foods;
  $("#food").innerHTML = MF.map((f, i) => '<button class="att" data-i="' + i + '" type="button"><img src="' + tex(f.cooked) + '" alt="' + esc(f.cookedName) + '" /></button>').join("");
  const showFood = i => {
    const f = MF[i], e = f.effect;
    $("#foodOut").innerHTML = "<b>" + esc(f.cookedName) + '</b> <span class="dim">from ' + esc(f.rawName) + " (" + f.mobs.map(m => m.replace("minecraft:", "").replace(/_/g, " ")).join(", ") + ", " +
      Math.round(f.chance * 100) + "%)</span> · " + f.hunger + " hunger · " + esc(e[0].replace("minecraft:", "").replace(/_/g, " ") + (e[2] ? " " + ["", "II", "III", "IV"][e[2]] : "") + " " + e[1] + "s");
  };
  ["mouseover", "focusin", "click"].forEach(ev => $("#food").addEventListener(ev, e => { const b = e.target.closest(".att"); if (b) showFood(+b.dataset.i); }));
  showFood(0);

  // ---------- DOWNLOAD ----------
  const REL = "https://github.com/thenameiscobmarley/factory-ascension/releases/download/v" + PACK.version + "/FactoryAscension-";
  document.querySelectorAll("[data-dl]").forEach(a => {
    // From v19 the files carry the Minecraft version too: FactoryAscension-Full-v19.0.0-MC1.20.1-CurseForge.zip
    a.href = REL + a.dataset.dl + "-v" + PACK.version + "-MC" + (PACK.mc || "1.20.1") + (a.dataset.cf ? "-CurseForge" : "") + ".zip";
    a.textContent += " · v" + PACK.version + " for Minecraft " + (PACK.mc || "1.20.1");
  });
  $("#mods").innerHTML = [["Create", "create", 1], ["KubeJS", "kubejs", 1], ["Rhino", "rhino", 1], ["Architectury API", "architectury-api", 1],
    ["Create: The Factory Must Grow", "create-industry"], ["Mekanism", "mekanism"], ["JEI", "jei"], ["Waystones", "waystones"], ["Balm", "balm"],
    ["Sophisticated Backpacks", "sophisticated-backpacks"], ["Sophisticated Core", "sophisticated-core"], ["Jade", "jade"], ["Embeddium", "embeddium"],
    ["Oculus", "oculus"], ["Iris & Oculus Flywheel Compat", "iris-flywheel-compat"],
    ["ModernFix", "modernfix"], ["FerriteCore", "ferritecore"]]
    .map(m => '<a class="chip' + (m[2] ? " req" : "") + '" href="https://www.curseforge.com/minecraft/mc-mods/' + m[1] + '">' + esc(m[0]) + "</a>").join("");
  document.querySelectorAll("[data-copy]").forEach(b => b.addEventListener("click", () => { copy(b.dataset.copy); b.classList.add("is-active"); setTimeout(() => b.classList.remove("is-active"), 600); }));
  $("#term").innerHTML = '<span class="dim">$ java -jar rhino.jar tools/test/harness.js</span>\n  <span class="ok">ok</span>   Essence is conserved\n  <span class="ok">ok</span>   a failed upgrade is rolled back\n' +
    '  <span class="ok">ok</span>   gear drops are never duplicated\n<b class="ok">all checks passed</b> <span class="dim">(' + PACK.checks + ')</span>\n\n<span class="dim">&gt; /ascension selftest</span>\n' +
    '<b class="ok">' + (PACK.selftest || 68) + " / " + (PACK.selftest || 68) + " passed</b> <span class=\"dim\">(with the Full jar)</span>";

  route();
})();

// ---- Theme switch: Tome (default) or Classic, remembered in this browser ----
(() => {
  const btn = document.getElementById("themeSwitch");
  if (!btn) return;
  const label = () => { btn.textContent = document.documentElement.dataset.theme == "classic" ? "Tome look" : "Classic look"; };
  label();
  btn.addEventListener("click", () => {
    const next = document.documentElement.dataset.theme == "classic" ? "tome" : "classic";
    document.documentElement.dataset.theme = next;
    try { localStorage.setItem("fa-theme", next); } catch (e) { /* private mode: it just won't be remembered */ }
    label();
  });
})();

// ---- What's new / everything before: books that slide down, open, and read themselves in ----
(() => {
  const NEWS = window.FA_NEWS || [];
  if (!NEWS.length) return;
  const $ = s => document.querySelector(s);
  const esc = t => String(t).replace(/[&<>"]/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]));
  const cmp = (a, b) => { const x = a.split(".").map(Number), y = b.split(".").map(Number); for (let i = 0; i < 3; i++) if (x[i] != y[i]) return x[i] - y[i]; return 0; };
  const latest = NEWS.map(n => n.v).sort(cmp).pop();
  const label = v => "v" + v.replace(/\.0$/, "");
  const AREAS = { home: "the pack", luck: "Luck", gear: "Gear", enchants: "Enchants", machines: "Machines", world: "the World" };
  const COVER = { all: "purple", home: "purple", luck: "red", gear: "steel", enchants: "teal", machines: "brown", world: "green" };
  const reduced = matchMedia("(prefers-reduced-motion: reduce)").matches;

  // The home card: only the newest release
  if ($("#newVersion")) $("#newVersion").textContent = label(latest);
  const box = $("#newsLatest");
  if (box) box.innerHTML = NEWS.filter(n => n.v == latest).map(n =>
    '<a href="#' + (n.area == "home" ? "home" : n.area) + '"><img src="fa/' + esc(n.icon) + '" alt="" /><b>' + esc(n.title) + "</b><span>" + esc(n.text) + "</span></a>").join("");

  // A pair of books on every chapter: what's new there, and everything before
  ["luck", "gear", "enchants", "machines", "world"].forEach(id => {
    const head = document.querySelector("#" + id + " > .ch-head");
    if (head && !head.nextElementSibling?.classList.contains("shelf-books")) head.insertAdjacentHTML("afterend", '<div class="shelf-books" data-area="' + id + '"></div>');
  });
  const pick = (area, isNew) => NEWS.filter(n => (area == "all" || n.area == area) && (isNew ? n.v == latest : n.v != latest));
  document.querySelectorAll(".shelf-books").forEach(shelf => {
    const area = shelf.dataset.area, where = area == "all" ? "" : " in " + AREAS[area];
    const fresh = pick(area, true), old = pick(area, false);
    shelf.innerHTML = [[true, fresh, "New in " + label(latest), fresh.length + " new" + where], [false, old, "Everything before", old.length + " things" + where]]
      .map(([isNew, list, title, sub]) => '<button type="button" class="shelf-book' + (isNew ? " is-new" : "") + '" data-new="' + (isNew ? 1 : 0) + '" style="--c:url(art/cover_' + COVER[area] + '.png)"' +
        (list.length ? "" : " disabled") + '><span class="sb-spine"></span><b>' + esc(title) + "</b><small>" + esc(sub) + "</small></button>").join("");
    shelf.addEventListener("click", e => {
      const b = e.target.closest(".shelf-book");
      if (!b || b.disabled) return;
      const isNew = b.dataset.new == "1";
      openBook(pick(area, isNew), isNew ? "New in " + label(latest) : "Everything before", (isNew ? "What " + label(latest) + " added" : "Every release before " + label(latest)) + where, b, COVER[area]);
    });
  });

  // ---- The book ----
  const stage = $("#bookStage"), book = $("#book3d"), cover = $("#bookCover"), L = $("#bookLeft"), R = $("#bookRight");
  if (!stage) return;
  let pages = [], spread = 0, from = null, busy = false;
  const perPage = () => (innerWidth < 760 ? 3 : 3);
  const twoPages = () => innerWidth >= 760;

  // Every word gets its own delay, so the page reads itself in left to right, top to bottom
  let wordIndex = 0;
  const words = text => esc(text).split(/\s+/).map(w => '<span class="w" style="--d:' + (wordIndex++ * 28) + 'ms">' + w + "</span>").join(" ");
  const pageHtml = list => list.map(n => '<div class="bk-entry"><img src="fa/' + esc(n.icon) + '" alt="" /><div><b>' + words(n.title) + '</b><p>' + words(n.text) +
    '</p><small class="w" style="--d:' + (wordIndex++ * 28) + 'ms">' + label(n.v) + " · " + esc(AREAS[n.area] || n.area) + "</small></div></div>").join("") || '<p class="bk-empty">' + words("Nothing here yet.") + "</p>";

  const render = () => {
    wordIndex = 0;
    const per = twoPages() ? 2 : 1;
    L.innerHTML = pageHtml(pages[spread * per] || []);
    R.innerHTML = per == 2 ? pageHtml(pages[spread * per + 1] || []) : "";
    const total = Math.max(1, Math.ceil(pages.length / per));
    $("#bookPageNo").textContent = (spread + 1) + " / " + total;
    $("#bookPrev").disabled = spread == 0;
    $("#bookNext").disabled = spread >= total - 1;
    book.classList.remove("reading");
    void book.offsetWidth;            // restart the word animations
    book.classList.add("reading");
  };

  const anim = (el, frames, opts) => reduced ? Promise.resolve() : el.animate(frames, Object.assign({ fill: "forwards" }, opts)).finished.catch(() => {});

  async function openBook(list, title, sub, origin, color) {
    if (busy) return;
    busy = true;
    from = origin;
    pages = [];
    for (let i = 0; i < list.length; i += perPage()) pages.push(list.slice(i, i + perPage()));
    spread = 0;
    $("#bookTitle").textContent = title;
    $("#bookSub").textContent = sub;
    book.style.setProperty("--c", "url(art/cover_" + color + ".png)");
    book.classList.remove("open", "opened", "reading");
    L.innerHTML = R.innerHTML = "";
    stage.hidden = false;
    document.body.classList.add("book-lock");
    // Slide down from the shelf book that was clicked
    const a = origin.getBoundingClientRect(), b = book.getBoundingClientRect();
    const dx = a.left + a.width / 2 - (b.left + b.width / 2), dy = a.top + a.height / 2 - (b.top + b.height / 2), s = Math.max(0.15, a.width / b.width);
    anim(stage.querySelector(".book-dim"), [{ opacity: 0 }, { opacity: 1 }], { duration: 400 });
    await anim(book, [{ transform: `translate(${dx}px, ${dy}px) scale(${s})`, opacity: 0.6 }, { transform: "translate(0, 0) scale(1)", opacity: 1 }], { duration: 650, easing: "cubic-bezier(.2,.8,.2,1)" });
    // Open the cover, then read
    book.classList.add("open");
    await anim(cover, [{ transform: "rotateY(0deg)" }, { transform: "rotateY(-180deg)" }], { duration: 800, easing: "cubic-bezier(.5,0,.2,1)" });
    book.classList.add("opened");
    render();
    busy = false;
    $("#bookClose").focus();
  }

  async function closeBook() {
    if (busy || stage.hidden) return;
    busy = true;
    book.classList.remove("reading");
    await anim(book.querySelector(".book-pages"), [{ opacity: 1 }, { opacity: 0.4 }], { duration: 200 });
    book.classList.remove("open", "opened");
    await anim(cover, [{ transform: "rotateY(-180deg)" }, { transform: "rotateY(0deg)" }], { duration: 550, easing: "cubic-bezier(.5,0,.2,1)" });
    if (from) {
      const a = from.getBoundingClientRect(), b = book.getBoundingClientRect();
      anim(stage.querySelector(".book-dim"), [{ opacity: 1 }, { opacity: 0 }], { duration: 450 });
      await anim(book, [{ transform: "translate(0,0) scale(1)", opacity: 1 }, { transform: `translate(${a.left + a.width / 2 - (b.left + b.width / 2)}px, ${a.top + a.height / 2 - (b.top + b.height / 2)}px) scale(${Math.max(0.15, a.width / b.width)})`, opacity: 0 }], { duration: 500, easing: "cubic-bezier(.6,0,.8,.4)" });
    }
    book.getAnimations().forEach(x => x.cancel());
    cover.getAnimations().forEach(x => x.cancel());
    book.querySelector(".book-pages").getAnimations().forEach(x => x.cancel());
    stage.querySelectorAll(".book-dim").forEach(d => d.getAnimations().forEach(x => x.cancel()));
    stage.hidden = true;
    document.body.classList.remove("book-lock");
    busy = false;
    if (from) from.focus();
  }

  const turn = async step => {
    if (busy) return;
    busy = true;
    const page = step > 0 ? R : L;
    await anim(page, [{ transform: "rotateY(0)", opacity: 1 }, { transform: step > 0 ? "rotateY(-90deg)" : "rotateY(90deg)", opacity: 0.3 }], { duration: 220, easing: "ease-in" });
    spread += step;
    render();
    await anim(page, [{ transform: step > 0 ? "rotateY(90deg)" : "rotateY(-90deg)", opacity: 0.3 }, { transform: "rotateY(0)", opacity: 1 }], { duration: 220, easing: "ease-out" });
    page.getAnimations().forEach(x => x.cancel());
    busy = false;
  };
  // Test hook for screenshots: ?booktest=area,new|old,ms shows that book as it looks ms into opening (slide 0-650 ms,
  // cover 650-1450 ms, then the words), built synchronously so a plain page screenshot catches it
  const bt = new URLSearchParams(location.search).get("booktest");
  if (bt && !reduced) {
    const [area, kind, ms] = bt.split(","), t = +ms;
    const btn = document.querySelector('.shelf-books[data-area="' + area + '"] .shelf-book[data-new="' + (kind == "new" ? 1 : 0) + '"]');
    if (btn) {
      const isNew = kind == "new", list = pick(area, isNew);
      pages = [];
      for (let i = 0; i < list.length; i += perPage()) pages.push(list.slice(i, i + perPage()));
      $("#bookTitle").textContent = isNew ? "New in " + label(latest) : "Everything before";
      $("#bookSub").textContent = isNew ? "What " + label(latest) + " added" : "Every release before " + label(latest);
      book.style.setProperty("--c", "url(art/cover_" + COVER[area] + ".png)");
      stage.hidden = false;
      const a = btn.getBoundingClientRect(), b = book.getBoundingClientRect();
      const slide = book.animate([{ transform: `translate(${a.left + a.width / 2 - (b.left + b.width / 2)}px, ${a.top + a.height / 2 - (b.top + b.height / 2)}px) scale(${Math.max(0.15, a.width / b.width)})`, opacity: 0.6 },
        { transform: "none", opacity: 1 }], { duration: 650, easing: "cubic-bezier(.2,.8,.2,1)", fill: "forwards" });
      slide.currentTime = Math.min(650, t); slide.pause();
      if (t > 650) {
        book.classList.add("open");
        const c = cover.animate([{ transform: "rotateY(0deg)" }, { transform: "rotateY(-180deg)" }], { duration: 800, easing: "cubic-bezier(.5,0,.2,1)", fill: "forwards" });
        c.currentTime = Math.min(800, t - 650); c.pause();
      }
      if (t > 1450) {
        book.classList.add("opened");
        render();
        book.getAnimations({ subtree: true }).forEach(x => { if (x instanceof CSSAnimation) { x.currentTime = t - 1450; x.pause(); } });
      }
    }
  }
  $("#bookNext").addEventListener("click", () => turn(1));
  $("#bookPrev").addEventListener("click", () => turn(-1));
  $("#bookClose").addEventListener("click", closeBook);
  stage.querySelector(".book-dim").addEventListener("click", closeBook);
  addEventListener("keydown", e => {
    if (stage.hidden) return;
    if (e.key == "Escape") closeBook();
    if (e.key == "ArrowRight" && !$("#bookNext").disabled) turn(1);
    if (e.key == "ArrowLeft" && !$("#bookPrev").disabled) turn(-1);
  });
})();
