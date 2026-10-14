/* Factory recipes page: runs the pack's own recipe scripts (with stand-ins for KubeJS) and shows each item's
   full factory chain. Toggling TFMG / Mekanism re-runs the scripts, exactly like the pack does when they're missing. */
(function () {
  "use strict";
  const $ = s => document.querySelector(s);
  const esc = s => String(s).replace(/[&<>"]/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[c]);
  const pretty = id => id.replace(/^fluid:/, "").replace(/^[a-z0-9_]+:/, "").replace(/_/g, " ").replace(/\b\w/g, c => c.toUpperCase());
  const nameOf = id => FA_NAMES[id] || pretty(id) + (id.indexOf("fluid:") === 0 ? " (fluid)" : "");
  const MACHINES = {
    "create:mixing": "Mechanical Mixer + Basin", "create:compacting": "Mechanical Press + Basin (compacting)", "create:crushing": "Crushing Wheels",
    "create:deploying": "Deployer", "create:sequenced_assembly": "Sequenced assembly line", "create:filling": "Spout", "create:pressing": "Mechanical Press",
    shaped: "Crafting table", shapeless: "Crafting table"
  };
  const HEAT = { heated: "Blaze Burner: heated", superheated: "Blaze Burner: superheated (blaze cake)" };
  // Where the raw materials come from, for the ends of every chain
  const BASE = {
    "minecraft:cobblestone": "Cobblestone generator + Mechanical Drill", "minecraft:gravel": "Crush cobblestone", "minecraft:blackstone": "Lava over soul soil next to blue ice, drilled",
    "minecraft:netherrack": "Drill contraptions in the Nether", "minecraft:quartz": "Wash soul sand with an Encased Fan through water",
    "fluid:minecraft:lava": "Pump a lava lake, or a dripstone lava farm", "fluid:minecraft:water": "Any infinite water source + Mechanical Pump",
    "minecraft:book": "Paper (sugar cane) + leather", "create:polished_rose_quartz": "Mix redstone + quartz into rose quartz, polish with sandpaper on a Deployer",
    "tfmg:steel_ingot": "TFMG's steel line (blast furnace)", "minecraft:redstone": "Gem Mineral Cluster, crushed", "minecraft:lapis_lazuli": "Gem Mineral Cluster, crushed"
  };
  const tex = id => id.indexOf("kubejs:") === 0 && id.indexOf("fluid:") !== 0 ? "fa/items/" + id.slice(7) + ".png" : null;

  let index = {};

  function count(list) {
    const out = [], seen = {};
    list.forEach(x => { if (seen[x.id]) seen[x.id].count += x.count; else { seen[x.id] = { id: x.id, count: x.count, amount: x.amount }; out.push(seen[x.id]); } });
    return out;
  }
  const ing = i => i.fluid ? { id: "fluid:" + i.fluid, count: 1, amount: i.amount } : { id: i.item, count: i.count || 1 };
  const res = r => r.fluid ? { id: "fluid:" + r.fluid, count: 1, amount: r.amount, chance: r.chance } : { id: r.item, count: r.count || 1, chance: r.chance };

  function normalize(j) {
    if (j.type == "create:sequenced_assembly") {
      const trans = j.transitionalItem.item;
      return { machine: j.type, inputs: [ing(j.ingredient)].concat(count([].concat.apply([], j.sequence.map(st => st.ingredients.filter(x => x.item != trans).map(ing))).map(x => ({ id: x.id, count: x.count * j.loops, amount: x.amount && x.amount * j.loops })))),
        steps: j.sequence.map(st => ({ machine: st.type, extra: st.ingredients.filter(x => x.item != trans).map(ing)[0] })), loops: j.loops, outputs: j.results.map(res) };
    }
    return { machine: j.type, heat: j.heatRequirement, inputs: count((j.ingredients || []).map(ing)), outputs: (j.results || []).map(res) };
  }

  function run() {
    index = {};
    const add = r => r.outputs.forEach(o => { (index[o.id] = index[o.id] || []).push(r); });
    const ev = {
      custom: j => { add(normalize(j)); return { id: () => {} }; },
      shapeless: (out, ins) => {
        const o = typeof out == "string" ? { id: out, count: 1 } : { id: out.id, count: out.count };
        add({ machine: "shapeless", inputs: count(ins.map(i => ({ id: i, count: 1 }))), outputs: [o] });
        return { id: () => {} };
      },
      shaped: (out, pattern, key) => {
        const o = typeof out == "string" ? { id: out, count: 1 } : { id: out.id, count: out.count };
        const ins = [];
        pattern.join("").split("").forEach(ch => { if (key[ch]) ins.push({ id: key[ch], count: 1 }); });
        add({ machine: "shaped", inputs: count(ins), outputs: [o], pattern: pattern, key: key });
        return { id: () => {} };
      }
    };
    FA_RECIPE_FNS.forEach(fn => { try { fn(ev); } catch (e) { console.error(e); } });
  }

  // ---------- list ----------
  let selected = "kubejs:ascension_core", query = "";
  function renderList() {
    const all = Object.keys(index).sort((a, b) => (b.indexOf("kubejs:") === 0) - (a.indexOf("kubejs:") === 0) || nameOf(a).localeCompare(nameOf(b)));
    const shown = all.filter(id => !query || nameOf(id).toLowerCase().includes(query) || id.includes(query));
    $("#items").innerHTML = shown.map(id => '<button role="option" class="it' + (id == selected ? " is-active" : "") + '" data-id="' + esc(id) + '">' + icon(id) +
      "<span>" + esc(nameOf(id)) + "</span></button>").join("") || '<p class="dim">Nothing matches.</p>';
  }
  function icon(id) {
    const t = tex(id);
    return t ? '<img src="' + t + '" alt="" width="24" height="24" />' : '<i class="dot' + (id.indexOf("fluid:") === 0 ? " fl" : "") + '">' + esc(pretty(id).charAt(0)) + "</i>";
  }

  // ---------- chain ----------
  const qty = x => x.amount ? x.amount + " mB" : (x.count > 1 ? x.count + "×" : "");
  function chip(x, depth) {
    const has = !!index[x.id];
    return '<button class="ing' + (has ? " has" : "") + '" data-id="' + esc(x.id) + '"' + (has ? "" : " disabled") + ">" + icon(x.id) +
      "<span>" + (qty(x) ? "<b>" + qty(x) + "</b> " : "") + esc(nameOf(x.id)) + (x.chance && x.chance < 1 ? " <em>" + Math.round(x.chance * 100) + "%</em>" : "") + "</span></button>";
  }
  function recipeCard(r) {
    let html = '<div class="rc"><div class="mach"><b>' + (MACHINES[r.machine] || r.machine) + "</b>" + (r.heat ? '<span class="heat ' + r.heat + '">' + HEAT[r.heat] + "</span>" : "") + "</div>";
    if (r.steps) html += '<ol class="steps-mini">' + r.steps.map(st => "<li>" + (MACHINES[st.machine] || st.machine) + (st.extra ? ": " + (qty(st.extra) ? qty(st.extra) + " " : "") + esc(nameOf(st.extra.id)) : "") + "</li>").join("") + "</ol><p class=\"dim\">Repeat " + r.loops + " times, then it pops out finished.</p>";
    html += '<div class="io"><div class="ins">' + r.inputs.map(x => chip(x)).join("") + '</div><span class="arrow">→</span><div class="outs">' + r.outputs.map(x => chip(x)).join("") + "</div></div></div>";
    return html;
  }
  // The recipe to show first: a factory recipe rather than crafting-table unpacking
  const primary = recipes => recipes.find(r => r.machine != "shapeless") || recipes[0];
  function tree(id, depth, seen) {
    const recipes = index[id];
    if (!recipes) return '<p class="base">' + icon(id) + "<span><b>" + esc(nameOf(id)) + "</b> " + esc(BASE[id] || "Raw material") + "</span></p>";
    if (seen[id] || depth > 7) return '<p class="base dim">' + esc(nameOf(id)) + " (shown above)</p>";
    const next = Object.assign({}, seen); next[id] = true;
    const r = primary(recipes);
    let html = recipeCard(r);
    const subs = r.inputs.filter(x => x.id != id);
    if (subs.length) html += '<div class="sub">' + subs.map(x =>
      "<details" + (depth < 1 ? " open" : "") + "><summary>" + icon(x.id) + " " + esc(nameOf(x.id)) + "</summary>" + tree(x.id, depth + 1, next) + "</details>").join("") + "</div>";
    return html;
  }
  function renderChain() {
    const recipes = index[selected] || [];
    let html = '<div class="chain-head">' + icon(selected) + "<div><h2>" + esc(nameOf(selected)) + '</h2><p class="dim">' + esc(selected) + "</p></div></div>";
    html += tree(selected, 0, {});
    const others = recipes.filter(r => r !== primary(recipes));
    if (others.length) html += '<h3 class="mono" style="margin-top:26px">Other ways to make it</h3>' + others.map(recipeCard).join("");
    $("#chain").innerHTML = html;
  }

  // ---------- events ----------
  function select(id) { if (!index[id]) return; selected = id; renderList(); renderChain(); history.replaceState(null, "", "#" + encodeURIComponent(id)); }
  $("#items").addEventListener("click", e => { const b = e.target.closest("[data-id]"); if (b) select(b.dataset.id); });
  $("#chain").addEventListener("click", e => { const b = e.target.closest("button.ing.has"); if (b) { select(b.dataset.id); $("#chain").scrollIntoView({ behavior: "smooth", block: "start" }); } });
  $("#find").addEventListener("input", e => { query = e.target.value.trim().toLowerCase(); renderList(); });
  const remods = () => { FA_MODS.tfmg = $("#tfmg").checked; FA_MODS.mekanism = $("#mek").checked; run(); if (!index[selected]) selected = "kubejs:ascension_core"; renderList(); renderChain(); };
  $("#tfmg").addEventListener("change", remods);
  $("#mek").addEventListener("change", remods);

  run();
  const fromHash = decodeURIComponent(location.hash.slice(1));
  if (index[fromHash]) selected = fromHash;
  renderList();
  renderChain();
})();
