/*! UI Vault 3.1.0 — full bundle (core, motion, feedback, forms, fx, waterfall, layers). MIT */
/*! UI Vault 3 · core.js — the UV namespace, module registry, theming, auto-init. Load first. MIT
 *  Every other module calls UV.use(name, initFn). initFn(root) runs on DOMContentLoaded and on UV.init(root).
 */
(function (global) {
  "use strict";
  if (global.UV && global.UV._core) return;
  const UV = global.UV = Object.assign(global.UV || {}, { version: "3.0.0", _core: true, modules: [] });
  const hooks = [];
  let ready = false;

  UV.reduce = matchMedia("(prefers-reduced-motion: reduce)").matches;
  UV.raf = requestAnimationFrame.bind(global);
  UV.$ = (s, r = document) => r.querySelector(s);
  UV.$$ = (s, r = document) => [...r.querySelectorAll(s)];
  UV.local = (e, el) => { const r = el.getBoundingClientRect(); return { x: e.clientX - r.left, y: e.clientY - r.top, w: r.width, h: r.height }; };
  UV.once = (el, key) => { if (el["_uv_" + key]) return false; el["_uv_" + key] = true; return true; };
  UV.css = (id, text) => { if (document.getElementById(id)) return; const s = document.createElement("style"); s.id = id; s.textContent = text; document.head.appendChild(s); };

  /** Register a module. fn(root) wires up everything the module owns inside root. */
  UV.use = (name, fn) => {
    UV.modules.push(name);
    if (fn) { hooks.push(fn); if (ready) fn(document); }
  };

  /** Wire every module inside root (call again after injecting HTML). */
  UV.init = (root = document) => {
    root.querySelectorAll("[data-uv]").forEach(el => {
      if (!UV.once(el, "behaviors")) return;
      el.dataset.uv.split(/\s+/).forEach(b => typeof UV[b] === "function" && UV[b](el));
    });
    hooks.forEach(fn => { try { fn(root); } catch (e) { console.warn("[UV]", e); } });
    return UV;
  };

  /** Light/dark mode for the default theme: "dark" | "light" | "toggle". */
  UV.theme = mode => {
    const r = document.documentElement;
    const next = mode === "toggle" ? (r.dataset.theme === "dark" ? "light" : "dark") : mode;
    r.dataset.theme = next;
    try { localStorage.setItem("uv-theme", next); } catch {}
    return next;
  };

  /** Design theme (skin) for any element: UV.skin("neon") for the page, UV.skin("brutal", el) for one section. */
  UV.skin = (name, el = document.documentElement) => {
    if (name) el.dataset.uvTheme = name; else el.removeAttribute("data-uv-theme");
    if (el === document.documentElement) { try { localStorage.setItem("uv-skin", name || ""); } catch {} }
    return name;
  };
  UV.skins = ["editorial", "brutal", "neon", "glass", "terminal", "soft", "retro", "swiss", "y2k", "mono", "sketch", "luxe"];

  try { const t = localStorage.getItem("uv-theme"); if (t && !document.documentElement.dataset.theme) document.documentElement.dataset.theme = t; } catch {}

  const boot = () => { ready = true; UV.init(document); };
  if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", boot); else setTimeout(boot);
})(window);

/*! UI Vault 3 · motion.js — countUp, typewriter, scramble, reveal, tilt, magnetic, spotlight, ripple, parallax, ring. Needs core.js. MIT */
(function (UV) {
  "use strict";
  const { raf } = UV;

  UV.countUp = (el, to, { duration = 1400, decimals = 0, prefix = "", suffix = "", from = 0 } = {}) => {
    const fmt = v => prefix + v.toLocaleString(undefined, { minimumFractionDigits: decimals, maximumFractionDigits: decimals }) + suffix;
    if (UV.reduce) { el.textContent = fmt(to); return; }
    const t0 = performance.now();
    (function step(now) { const p = Math.min(1, (now - t0) / duration), e = 1 - Math.pow(1 - p, 4); el.textContent = fmt(from + (to - from) * e); if (p < 1) raf(step); })(t0);
  };

  UV.typewriter = (el, words, { speed = 70, pause = 1400, loop = true } = {}) => {
    let w = 0, i = 0, del = false, stopped = false;
    UV.css("uv-caret-css", ".uv-caret::after{content:'';display:inline-block;width:2px;height:1em;margin-left:3px;vertical-align:-.12em;background:currentColor;animation:uv-blink .9s step-end infinite}@keyframes uv-blink{50%{opacity:0}}");
    el.classList.add("uv-caret");
    (function tick() {
      if (stopped) return;
      const word = words[w]; el.textContent = word.slice(0, i);
      if (!del && i < word.length) { i++; setTimeout(tick, speed); }
      else if (!del) { if (!loop && w === words.length - 1) return; del = true; setTimeout(tick, pause); }
      else if (i > 0) { i--; setTimeout(tick, speed / 2); }
      else { del = false; w = (w + 1) % words.length; setTimeout(tick, speed * 3); }
    })();
    return () => { stopped = true; };
  };

  UV.scramble = (el, text = el.textContent, { duration = 900, chars = "!<>-_\\/[]{}—=+*^?#ABCDEFX01" } = {}) => {
    if (UV.reduce) { el.textContent = text; return; }
    const t0 = performance.now();
    (function step(now) {
      const p = Math.min(1, (now - t0) / duration);
      el.textContent = [...text].map((ch, k) => ch === " " || k / text.length < p ? ch : chars[(Math.random() * chars.length) | 0]).join("");
      if (p < 1) raf(step); else el.textContent = text;
    })(t0);
  };

  UV.spotlight = el => el.addEventListener("pointermove", e => { const p = UV.local(e, el); el.style.setProperty("--x", p.x + "px"); el.style.setProperty("--y", p.y + "px"); });
  UV.tilt = (el, { max = 12 } = {}) => {
    if (UV.reduce) return;
    el.classList.add("uv-tilt");
    if (!el.querySelector(":scope > .uv-glare")) { const g = document.createElement("i"); g.className = "uv-glare"; el.appendChild(g); }
    el.addEventListener("pointermove", e => {
      const p = UV.local(e, el), px = p.x / p.w, py = p.y / p.h;
      el.style.setProperty("--ry", (px - .5) * max * 2 + "deg"); el.style.setProperty("--rx", (.5 - py) * max * 2 + "deg");
      el.style.setProperty("--gx", px * 100 + "%"); el.style.setProperty("--gy", py * 100 + "%");
    });
    el.addEventListener("pointerleave", () => { el.style.setProperty("--rx", "0deg"); el.style.setProperty("--ry", "0deg"); });
  };
  UV.magnetic = (el, { strength = .35 } = {}) => {
    if (UV.reduce) return;
    el.style.transition = "transform .3s cubic-bezier(.2,.8,.2,1)";
    el.addEventListener("pointermove", e => { const p = UV.local(e, el); el.style.transform = `translate(${(p.x - p.w / 2) * strength}px,${(p.y - p.h / 2) * strength}px)`; });
    el.addEventListener("pointerleave", () => { el.style.transform = ""; });
  };
  UV.ripple = el => el.addEventListener("pointerdown", e => {
    const p = UV.local(e, el), d = Math.max(p.w, p.h), i = document.createElement("span");
    i.className = "uv-ripple"; i.style.cssText = `width:${d}px;height:${d}px;left:${p.x - d / 2}px;top:${p.y - d / 2}px`;
    if (getComputedStyle(el).position === "static") el.style.position = "relative";
    el.style.overflow = "hidden"; el.appendChild(i); setTimeout(() => i.remove(), 650);
  });
  UV.parallax = el => {
    if (UV.reduce) return;
    const layers = el.querySelectorAll("[data-depth]");
    layers.forEach(l => { l.style.transition = "transform .25s ease-out"; });
    el.addEventListener("pointermove", e => { const p = UV.local(e, el), dx = p.x / p.w - .5, dy = p.y / p.h - .5; layers.forEach(l => { const d = +l.dataset.depth; l.style.transform = `translate(${dx * d * 40}px,${dy * d * 40}px)`; }); });
    el.addEventListener("pointerleave", () => layers.forEach(l => { l.style.transform = ""; }));
  };
  UV.ring = (el, value, { duration = 1200 } = {}) => {
    const from = +getComputedStyle(el).getPropertyValue("--v") || 0, t0 = performance.now();
    (function step(now) { const p = UV.reduce ? 1 : Math.min(1, (now - t0) / duration), e = 1 - Math.pow(1 - p, 3); el.style.setProperty("--v", from + (value - from) * e); if (p < 1) raf(step); })(t0);
  };

  let io;
  UV.reveal = (root = document) => {
    io = io || new IntersectionObserver(es => es.forEach(e => {
      if (!e.isIntersecting) return;
      const el = e.target; io.unobserve(el); el.classList.add("is-in");
      if (el.hasAttribute("data-uv-count")) UV.countUp(el, +el.dataset.uvCount, { decimals: +el.dataset.uvDecimals || 0, prefix: el.dataset.uvPrefix || "", suffix: el.dataset.uvSuffix || "" });
      if (el.hasAttribute("data-uv-scramble")) UV.scramble(el, el.dataset.uvText);
      if (el.classList.contains("uv-ring") && el.dataset.value) UV.ring(el, +el.dataset.value);
      if (el.classList.contains("uv-progress") && el.dataset.value) el.firstElementChild.style.setProperty("--v", el.dataset.value + "%");
    }), { threshold: 0, rootMargin: "0px 0px 18% 0px" });   // start revealing just BEFORE elements scroll in
    root.querySelectorAll("[data-uv-stagger]").forEach(p => [...p.children].forEach((c, k) => { if (!c.hasAttribute("data-uv-reveal")) c.setAttribute("data-uv-reveal", p.dataset.uvStagger || ""); c.style.setProperty("--d", (k % 6) * 40 + "ms"); }));
    root.querySelectorAll("[data-uv-reveal],[data-uv-count],[data-uv-scramble],.uv-ring[data-value],.uv-progress[data-value],.uv-fx-highlight").forEach(el => {
      if (!UV.once(el, "reveal")) return;
      if (el.hasAttribute("data-uv-scramble")) el.dataset.uvText = el.dataset.uvText || el.textContent;
      io.observe(el);
    });
  };

  // ── Scroll-linked motion ─────────────────────────────────────
  // data-uv-scrub        → sets --p from 0 (element enters bottom) to 1 (leaves top); style anything with var(--p)
  // data-uv-scrub="center" → 0 at entering, 1 when its centre reaches the viewport centre
  // data-uv-scrub="pin"  → for a tall .uv-pin section: 0 when its top hits the viewport top, 1 when its bottom leaves
  //                         (its .uv-pin__stage stays stuck on screen meanwhile — scroll-scrubbed scenes)
  // data-uv-scrub="page" → whole-page progress 0…1 (put it on a small fixed element, e.g. a backdrop or a gauge)
  // data-uv-velocity     → sets --v (-1…1) from how fast the page is scrolling, eased back to 0 (skews, stretches)
  // data-uv-speed="0.3"  → scroll parallax (translateY); negative values move against the scroll
  // .uv-scrollbar        → page progress bar (uses --uv-page on <html>)
  const de_smooth = () => document.documentElement.hasAttribute("data-uv-smooth");
  const bars = document.getElementsByClassName("uv-scrollbar");   // live collection
  const live = new Set();
  let ticking = false;
  const vis = new IntersectionObserver(es => es.forEach(e => e.isIntersecting ? live.add(e.target) : live.delete(e.target)), { rootMargin: "20% 0px" });
  function frame() {
    ticking = false;
    const vh = innerHeight, de = document.documentElement;
    // write the page progress ONLY on progress bars — setting an inherited var on <html> restyles the whole page every frame
    if (bars.length) { const pg = (scrollY / Math.max(1, de.scrollHeight - vh)).toFixed(4); for (const b of bars) b.style.setProperty("--uv-page", pg); }
    // read every rect first, then write — interleaving reads and writes forces a layout per element
    const items = [...live].map(el => [el, el.getBoundingClientRect()]);
    const now = performance.now(), dy = scrollY - lastY, dt = Math.max(8, now - lastT); lastY = scrollY; lastT = now;
    vel += (Math.max(-1, Math.min(1, dy / dt / 4)) - vel) * .35;
    for (const [el, r] of items) {
      if (el.hasAttribute("data-uv-scrub")) {
        const m = el.dataset.uvScrub;
        const p = m === "page" ? scrollY / Math.max(1, de.scrollHeight - vh) : m === "pin" ? -r.top / Math.max(1, r.height - vh) : m === "center" ? (vh - r.top) / (vh / 2 + r.height / 2) : (vh - r.top) / (vh + r.height);
        const v = Math.min(1, Math.max(0, p)), t = v.toFixed(4);
        if (el._uvP !== t) { el._uvP = t; el.style.setProperty("--p", t); if (el._uvScrub) el._uvScrub.forEach(f => f(v, el)); }
      }
      if (el.hasAttribute("data-uv-velocity")) el.style.setProperty("--v", vel.toFixed(3));
      if (el.dataset.uvSpeed) el.style.transform = `translate3d(0, ${((r.top + r.height / 2 - vh / 2) * -parseFloat(el.dataset.uvSpeed)).toFixed(1)}px, 0)`;
    }
  }
  let lastY = scrollY, lastT = performance.now(), vel = 0;
  const onScroll = () => { if (!ticking) { ticking = true; raf(frame); } };
  // keep easing --v back to rest for a moment after the scrolling stops
  const settle = () => { if (Math.abs(vel) > .002) { onScroll(); setTimeout(settle, 16); } else if (vel) { vel = 0; onScroll(); } };
  addEventListener("scroll", () => { clearTimeout(UV._velT); UV._velT = setTimeout(settle, 40); }, { passive: true });
  UV.scroll = (root = document) => {
    if (UV.reduce) return;
    root.querySelectorAll("[data-uv-scrub],[data-uv-speed],[data-uv-velocity]").forEach(el => UV.once(el, "scroll") && vis.observe(el));
    if (!UV._scrollBound) { UV._scrollBound = true; addEventListener("scroll", onScroll, { passive: true }); addEventListener("resize", onScroll); }
    onScroll();
  };
  /** Scroll progress in JS: UV.scrub(el, p => …, "pin"|"center"|"page"|"") — same 0…1 value the CSS gets as --p.
   *  Use it for numbers, canvas frames or anything CSS can't do. Returns an off() function. */
  UV.scrub = (el, cb, mode) => {
    if (mode != null || !el.hasAttribute("data-uv-scrub")) el.setAttribute("data-uv-scrub", mode || "");
    (el._uvScrub = el._uvScrub || new Set()).add(cb); el._uvP = null;
    if (UV.reduce) { cb(1, el); return () => {}; }
    UV.scroll(el.parentNode || document);
    return () => el._uvScrub.delete(cb);
  };
  UV.scrollbar = () => { if (document.querySelector(".uv-scrollbar")) return; const b = document.createElement("div"); b.className = "uv-scrollbar"; document.body.prepend(b); UV.scroll(); };

  /** Scrollytelling: children with [data-step] fire as they cross the viewport centre.
   *  UV.steps(container, (stepEl, index) => …); also sets container.dataset.step and dispatches "uv-step". */
  UV.steps = (el, cb) => {
    const steps = [...el.querySelectorAll("[data-step]")];
    const io2 = new IntersectionObserver(es => es.forEach(e => {
      if (!e.isIntersecting) return;
      const i = steps.indexOf(e.target);
      steps.forEach((s, k) => s.classList.toggle("is-active", k === i));
      el.dataset.step = e.target.dataset.step;
      cb && cb(e.target, i);
      el.dispatchEvent(new CustomEvent("uv-step", { detail: { index: i, step: e.target.dataset.step } }));
    }), { rootMargin: "-50% 0px -50% 0px" });
    steps.forEach(s => io2.observe(s));
    return () => io2.disconnect();
  };

  /** Inertial smooth scrolling for mouse wheels. Leaves inner scrollable areas, dialogs, zoom and touch alone.
   *  Turn on with <html data-uv-smooth> or UV.smoothScroll({ ease }). Off with prefers-reduced-motion. */
  UV.smoothScroll = ({ ease = .11, multiplier = 1 } = {}) => {
    if (UV.reduce || UV._smooth) return; UV._smooth = true;
    const de = document.documentElement;
    let target = scrollY, current = scrollY, running = false, last = 0;
    const max = () => de.scrollHeight - innerHeight;
    const innerScroll = (el, dy) => {
      for (; el && el !== document.body && el !== de; el = el.parentElement) {
        if (el.scrollHeight > el.clientHeight + 1 && /(auto|scroll)/.test(getComputedStyle(el).overflowY))
          if (dy > 0 ? el.scrollTop + el.clientHeight < el.scrollHeight - 1 : el.scrollTop > 0) return true;
      }
      return false;
    };
    const step = now => {
      const dt = Math.min(64, now - (last || now - 16.7)); last = now;
      current += (target - current) * (1 - Math.pow(1 - ease, dt / 16.7));   // same feel at 60, 120 or 144 Hz
      if (Math.abs(target - current) < .5) { current = target; running = false; last = 0; }
      scrollTo({ top: current, behavior: "instant" });
      if (running) raf(step);
    };
    addEventListener("wheel", e => {
      if (e.ctrlKey || e.defaultPrevented || Math.abs(e.deltaX) > Math.abs(e.deltaY) || document.querySelector("dialog[open]") || innerScroll(e.target, e.deltaY)) return;
      e.preventDefault();
      const d = e.deltaMode === 1 ? e.deltaY * 36 : e.deltaMode === 2 ? e.deltaY * innerHeight : e.deltaY;
      if (!running) target = current = scrollY;
      target = Math.max(0, Math.min(max(), target + d * multiplier));
      if (!running) { running = true; raf(step); }
    }, { passive: false });
    addEventListener("scroll", () => { if (!running) target = current = scrollY; }, { passive: true });
  };

  UV.use("motion", root => {
    if (root === document && de_smooth()) UV.smoothScroll();
    UV.scroll(root);
    root.querySelectorAll("[data-uv-steps]").forEach(el => UV.once(el, "steps") && UV.steps(el));
    root.querySelectorAll(".uv-card--spot").forEach(el => UV.once(el, "spot") && UV.spotlight(el));
    root.querySelectorAll("[data-uv-type]").forEach(el => UV.once(el, "type") && UV.typewriter(el, el.dataset.uvType.split("|")));
    UV.reveal(root);
  });
})(window.UV);

/*! UI Vault 3 · feedback.js — toast, modal, cmdk palette, confetti, sound, copy. Needs core.js. MIT */
(function (UV) {
  "use strict";
  let host, actx;

  UV.toast = (msg, { type = "info", duration = 3000, icon } = {}) => {
    if (!host) { host = document.createElement("div"); host.className = "uv-toasts uv"; host.setAttribute("aria-live", "polite"); document.body.appendChild(host); }
    const t = document.createElement("div");
    t.className = "uv-toast uv-toast--" + type;
    t.innerHTML = '<span class="uv-toast__icon"></span><span></span>';
    t.firstChild.textContent = icon || { success: "✓", error: "✕", warn: "!", info: "i" }[type] || "i";
    t.lastChild.textContent = msg;
    host.appendChild(t);
    const close = () => { t.classList.add("is-out"); setTimeout(() => t.remove(), 300); };
    t.addEventListener("click", close); setTimeout(close, duration);
    return close;
  };

  UV.copy = async (text, msg = "Copied to clipboard") => {
    try { await navigator.clipboard.writeText(text); }
    catch { const ta = document.createElement("textarea"); ta.value = text; ta.style.cssText = "position:fixed;opacity:0"; document.body.appendChild(ta); ta.select(); document.execCommand("copy"); ta.remove(); }
    if (msg) UV.toast(msg, { type: "success", duration: 1800 });
  };

  UV.sound = (name = "click", vol = .15) => {
    try {
      actx = actx || new (window.AudioContext || window.webkitAudioContext)();
      const now = actx.currentTime;
      const tone = (f, t, d, type = "sine", v = vol, f2) => {
        const o = actx.createOscillator(), g = actx.createGain();
        o.type = type; o.frequency.setValueAtTime(f, now + t); if (f2) o.frequency.exponentialRampToValueAtTime(f2, now + t + d * .8);
        g.gain.setValueAtTime(0, now + t); g.gain.linearRampToValueAtTime(v, now + t + .005); g.gain.exponentialRampToValueAtTime(.0001, now + t + d);
        o.connect(g).connect(actx.destination); o.start(now + t); o.stop(now + t + d + .02);
      };
      ({ click: () => tone(1800, 0, .04, "triangle", vol * .6),
         pop: () => tone(420, 0, .12, "sine", vol, 920),
         success: () => { tone(660, 0, .25); tone(880, .09, .3); tone(1320, .18, .4); },
         error: () => { tone(220, 0, .18, "square", vol * .4); tone(180, .12, .25, "square", vol * .4); },
         hover: () => tone(2400, 0, .02, "sine", vol * .3),
         whoosh: () => tone(200, 0, .3, "sawtooth", vol * .15, 1200),
         coin: () => { tone(988, 0, .08, "square", vol * .4); tone(1319, .08, .3, "square", vol * .4); },
         laser: () => tone(1600, 0, .25, "sawtooth", vol * .3, 120) }[name] || (() => {}))();
    } catch {}
  };

  UV.confetti = ({ x = innerWidth / 2, y = innerHeight / 2, count = 120, colors, shapes = "rect", spread = 1 } = {}) => {
    if (UV.reduce) return;
    const cs = getComputedStyle(document.documentElement);
    colors = colors || [cs.getPropertyValue("--uv-accent").trim() || "#ff4f00", "#1a1917", "#8f8a82", "#e2ded6", "#fbfaf8"];
    const c = document.createElement("canvas"), dpr = Math.min(devicePixelRatio || 1, 2);
    c.style.cssText = "position:fixed;inset:0;width:100%;height:100%;pointer-events:none;z-index:10000";
    c.width = innerWidth * dpr; c.height = innerHeight * dpr; document.body.appendChild(c);
    const g = c.getContext("2d"); g.scale(dpr, dpr);
    const P = Array.from({ length: count }, () => { const a = Math.random() * Math.PI * 2, s = (4 + Math.random() * 9) * spread;
      return { x, y, vx: Math.cos(a) * s, vy: Math.sin(a) * s - 6, r: Math.random() * 6, vr: (Math.random() - .5) * .4, w: 6 + Math.random() * 6, h: 4 + Math.random() * 4, c: colors[(Math.random() * colors.length) | 0], life: 0 }; });
    let frame = 0;
    (function tick() {
      g.clearRect(0, 0, innerWidth, innerHeight); let alive = 0;
      for (const p of P) {
        p.vy += .28; p.vx *= .985; p.vy *= .985; p.x += p.vx; p.y += p.vy; p.r += p.vr; p.life++;
        if (p.y < innerHeight + 20) alive++;
        g.save(); g.translate(p.x, p.y); g.rotate(p.r); g.globalAlpha = Math.max(0, 1 - p.life / 160); g.fillStyle = p.c;
        if (shapes === "circle") { g.beginPath(); g.arc(0, 0, p.w / 2, 0, 7); g.fill(); } else g.fillRect(-p.w / 2, -p.h / 2, p.w, p.h * Math.abs(Math.cos(p.life * .15)));
        g.restore();
      }
      if (alive && ++frame < 200) UV.raf(tick); else c.remove();
    })();
  };

  UV.modal = ({ title = "", body = "", actions = [{ label: "OK", value: true, primary: true }] } = {}) => new Promise(res => {
    const d = document.createElement("dialog");
    d.className = "uv-modal uv";
    d.innerHTML = `<div><h3 class="uv-h3" style="margin-bottom:8px"></h3><div class="uv-muted" style="margin-bottom:22px"></div><div class="uv-row" style="justify-content:flex-end"></div></div>`;
    d.querySelector("h3").textContent = title;
    const b = d.querySelector(".uv-muted"); typeof body === "string" ? (b.textContent = body) : b.appendChild(body);
    actions.forEach(a => { const btn = document.createElement("button"); btn.className = "uv-btn " + (a.primary ? "uv-btn--primary" : a.danger ? "uv-btn--danger" : "uv-btn--ghost"); btn.textContent = a.label; btn.onclick = () => d.close(JSON.stringify(a.value ?? a.label)); d.querySelector(".uv-row").appendChild(btn); });
    d.addEventListener("click", e => { if (e.target === d) d.close("null"); });
    d.addEventListener("close", () => { let v = null; try { v = JSON.parse(d.returnValue || "null"); } catch {} d.remove(); res(v); });
    document.body.appendChild(d); d.showModal();
  });

  UV.cmdk = (items, { hotkey = "k", placeholder = "Type a command or search…" } = {}) => {
    const d = document.createElement("dialog");
    d.className = "uv-cmdk uv"; d.innerHTML = `<input placeholder="${placeholder}" aria-label="Command"><ul role="listbox"></ul>`;
    document.body.appendChild(d);
    const input = d.querySelector("input"), ul = d.querySelector("ul");
    let shown = [], active = 0;
    const run = it => { d.close(); it.action && it.action(); };
    const render = () => {
      const q = input.value.toLowerCase().trim();
      shown = items.filter(i => !q || (i.label + " " + (i.keywords || "")).toLowerCase().includes(q));
      active = Math.min(active, Math.max(0, shown.length - 1));
      ul.innerHTML = shown.length ? "" : `<div class="uv-cmdk__empty">No results</div>`;
      shown.forEach((it, k) => {
        const li = document.createElement("li"); li.className = k === active ? "is-active" : "";
        li.innerHTML = `<span>${it.icon || "›"}</span><span></span>${it.shortcut ? `<span class="uv-kbd">${it.shortcut}</span>` : ""}`;
        li.children[1].textContent = it.label;
        li.onmouseenter = () => { active = k; [...ul.children].forEach((c, j) => c.classList.toggle("is-active", j === k)); };
        li.onclick = () => run(it); ul.appendChild(li);
      });
    };
    input.addEventListener("input", () => { active = 0; render(); });
    input.addEventListener("keydown", e => {
      if (e.key === "ArrowDown" || e.key === "ArrowUp") { e.preventDefault(); active = (active + (e.key === "ArrowDown" ? 1 : -1) + shown.length) % (shown.length || 1); render(); ul.children[active]?.scrollIntoView({ block: "nearest" }); }
      if (e.key === "Enter" && shown[active]) run(shown[active]);
    });
    d.addEventListener("click", e => { if (e.target === d) d.close(); });
    const open = () => { input.value = ""; active = 0; render(); d.showModal(); input.focus(); };
    if (hotkey) document.addEventListener("keydown", e => { if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === hotkey) { e.preventDefault(); d.open ? d.close() : open(); } });
    return { open, close: () => d.close() };
  };

  UV.use("feedback", root => {
    root.querySelectorAll("[data-uv-confetti]").forEach(el => UV.once(el, "conf") && el.addEventListener("click", e => UV.confetti({ x: e.clientX, y: e.clientY })));
    root.querySelectorAll("[data-uv-copy]").forEach(el => UV.once(el, "copy") && el.addEventListener("click", () => UV.copy(el.dataset.uvCopy)));
    root.querySelectorAll("[data-uv-sound]").forEach(el => UV.once(el, "snd") && el.addEventListener("click", () => UV.sound(el.dataset.uvSound)));
    root.querySelectorAll("[data-uv-toast]").forEach(el => UV.once(el, "toast") && el.addEventListener("click", () => UV.toast(el.dataset.uvToast, { type: el.dataset.uvToastType || "info" })));
  });
})(window.UV);

/*! UI Vault 3 · forms.js — tabs, otp, range, file drop. Needs core.js. MIT */
(function (UV) {
  "use strict";
  UV.tabs = (el, onChange) => {
    if (!UV.once(el, "tabs")) return el._uvTabsApi;
    el.setAttribute("role", "tablist");
    let ind = el.querySelector(".uv-tabs__ind");
    if (!ind) { ind = document.createElement("span"); ind.className = "uv-tabs__ind"; el.prepend(ind); }
    const tabs = [...el.querySelectorAll(":scope > button")];
    const move = t => { ind.style.width = t.offsetWidth + "px"; ind.style.transform = `translateX(${t.offsetLeft}px)`; };
    const select = (t, silent) => {
      tabs.forEach(x => { const on = x === t; x.setAttribute("aria-selected", on); x.setAttribute("role", "tab"); x.tabIndex = on ? 0 : -1;
        const panel = x.getAttribute("aria-controls") && document.getElementById(x.getAttribute("aria-controls")); if (panel) panel.hidden = !on; });
      move(t); if (!silent && onChange) onChange(t, tabs.indexOf(t));
      if (!silent) el.dispatchEvent(new CustomEvent("uv-change", { detail: tabs.indexOf(t) }));
    };
    tabs.forEach((t, k) => {
      t.addEventListener("click", () => select(t));
      t.addEventListener("keydown", e => { const d = e.key === "ArrowRight" ? 1 : e.key === "ArrowLeft" ? -1 : 0; if (d) { const n = tabs[(k + d + tabs.length) % tabs.length]; n.focus(); select(n); } });
    });
    const first = tabs.find(t => t.getAttribute("aria-selected") === "true") || tabs[0];
    ind.style.transition = "none"; select(first, true);
    UV.raf(() => UV.raf(() => { ind.style.transition = ""; move(first); }));
    new ResizeObserver(() => { const s = tabs.find(t => t.getAttribute("aria-selected") === "true"); s && move(s); }).observe(el);
    return (el._uvTabsApi = { select: i => select(tabs[i]) });
  };

  UV.otp = el => {
    const inputs = [...el.querySelectorAll("input")];
    inputs.forEach((inp, k) => {
      inp.maxLength = 1; inp.inputMode = "numeric";
      inp.addEventListener("input", () => { inp.value = inp.value.replace(/\D/g, ""); if (inp.value && inputs[k + 1]) inputs[k + 1].focus(); if (inputs.every(i => i.value)) el.dispatchEvent(new CustomEvent("uv-complete", { detail: inputs.map(i => i.value).join("") })); });
      inp.addEventListener("keydown", e => { if (e.key === "Backspace" && !inp.value && inputs[k - 1]) inputs[k - 1].focus(); });
      inp.addEventListener("paste", e => { const t = (e.clipboardData.getData("text") || "").replace(/\D/g, ""); if (t) { e.preventDefault(); inputs.forEach((i, j) => { i.value = t[j] || ""; }); inputs[Math.min(t.length, inputs.length) - 1].focus(); inp.dispatchEvent(new Event("input")); } });
    });
  };
  UV.range = el => { const sync = () => el.style.setProperty("--v", ((el.value - (el.min || 0)) / ((el.max || 100) - (el.min || 0))) * 100 + "%"); el.addEventListener("input", sync); sync(); };
  UV.drop = (el, onFiles) => {
    const input = document.createElement("input"); input.type = "file"; input.multiple = true; input.hidden = true; el.appendChild(input);
    const emit = files => { if (files.length) { el.dispatchEvent(new CustomEvent("uv-files", { detail: [...files] })); onFiles && onFiles([...files]); } };
    el.addEventListener("click", e => { if (e.target !== input) input.click(); });
    input.addEventListener("change", () => emit(input.files));
    ["dragenter", "dragover"].forEach(ev => el.addEventListener(ev, e => { e.preventDefault(); el.classList.add("is-over"); }));
    ["dragleave", "drop"].forEach(ev => el.addEventListener(ev, e => { e.preventDefault(); el.classList.remove("is-over"); }));
    el.addEventListener("drop", e => emit(e.dataTransfer.files));
  };

  UV.use("forms", root => {
    root.querySelectorAll(".uv-tabs").forEach(el => UV.tabs(el));
    root.querySelectorAll(".uv-otp").forEach(el => UV.once(el, "otp") && UV.otp(el));
    root.querySelectorAll(".uv-range").forEach(el => UV.once(el, "range") && UV.range(el));
    root.querySelectorAll(".uv-drop").forEach(el => UV.once(el, "drop") && UV.drop(el));
  });
})(window.UV);

/*! UI Vault 3 · fx.js — interactive effects. Needs core.js (pairs with fx.css). MIT
 *  Declarative:  data-uv-fx="matrix starfield particles snow cursor-trail cursor-blob sparkles magnet scramble-hover fireworks gravity spotlight holo"
 *  Imperative:   UV.fx.matrix(el, opts) … every effect returns a stop() function.
 *  Canvas effects size to their element, pause when off-screen and respect prefers-reduced-motion.
 */
(function (UV) {
  "use strict";
  const fx = UV.fx = {};
  const accent = el => getComputedStyle(el).getPropertyValue("--uv-accent").trim() || "#ff4f00";
  const textCol = el => getComputedStyle(el).color;

  /** Mount a full-size canvas inside el and run draw(g, w, h, t) while visible. */
  function canvasFx(el, setup, draw) {
    if (UV.reduce) return () => {};
    if (getComputedStyle(el).position === "static") el.style.position = "relative";
    const c = document.createElement("canvas"), g = c.getContext("2d"), dpr = Math.min(devicePixelRatio || 1, 1.5);
    c.style.cssText = "position:absolute;inset:0;width:100%;height:100%;pointer-events:none;z-index:0;border-radius:inherit";
    el.prepend(c);
    let w = 0, h = 0, raf = 0, visible = false, state = {};
    const size = () => { w = el.clientWidth; h = el.clientHeight; c.width = w * dpr; c.height = h * dpr; g.setTransform(dpr, 0, 0, dpr, 0, 0); state = setup(w, h, state) || state; };
    const loop = t => { raf = 0; draw(g, w, h, t, state); if (visible) raf = UV.raf(loop); };
    const ro = new ResizeObserver(size); ro.observe(el); size();
    const io = new IntersectionObserver(([e]) => { visible = e.isIntersecting; if (visible && !raf) raf = UV.raf(loop); }); io.observe(el);
    return () => { visible = false; ro.disconnect(); io.disconnect(); c.remove(); };
  }

  fx.matrix = (el, { color, size = 14, chars = "アイウエオカキクケコｱｲｳｴｵ01ΣΔΞ#$%" } = {}) => canvasFx(el,
    (w, h) => ({ cols: Array.from({ length: Math.ceil(w / size) }, () => Math.random() * -h / size), col: color || accent(el), last: 0 }),
    (g, w, h, t, s) => {
      if (t - s.last < 50) return; s.last = t;
      g.fillStyle = "rgba(0,0,0,.08)"; g.globalCompositeOperation = "destination-out"; g.fillRect(0, 0, w, h); g.globalCompositeOperation = "source-over";
      g.fillStyle = s.col; g.font = size + "px monospace";
      s.cols.forEach((y, i) => { g.fillText(chars[(Math.random() * chars.length) | 0], i * size, y * size); s.cols[i] = y * size > h && Math.random() > .975 ? 0 : y + 1; });
    });

  fx.starfield = (el, { count = 160, speed = 1 } = {}) => canvasFx(el,
    (w, h) => ({ stars: Array.from({ length: count }, () => ({ x: Math.random() * w - w / 2, y: Math.random() * h - h / 2, z: Math.random() * w })), col: textCol(el) }),
    (g, w, h, t, s) => {
      g.clearRect(0, 0, w, h); g.fillStyle = s.col;
      for (const p of s.stars) {
        p.z -= 2 * speed; if (p.z <= 1) { p.z = w; p.x = Math.random() * w - w / 2; p.y = Math.random() * h - h / 2; }
        const k = 128 / p.z, x = p.x * k + w / 2, y = p.y * k + h / 2, r = Math.max(.3, (1 - p.z / w) * 2.2);
        g.globalAlpha = 1 - p.z / w; g.beginPath(); g.arc(x, y, r, 0, 7); g.fill();
      }
      g.globalAlpha = 1;
    });

  fx.particles = (el, { count = 60, link = 110 } = {}) => {
    let mouse = { x: -999, y: -999 };
    el.addEventListener("pointermove", e => { mouse = UV.local(e, el); });
    el.addEventListener("pointerleave", () => { mouse = { x: -999, y: -999 }; });
    return canvasFx(el,
      (w, h) => ({ p: Array.from({ length: count }, () => ({ x: Math.random() * w, y: Math.random() * h, vx: (Math.random() - .5) * .5, vy: (Math.random() - .5) * .5 })), col: textCol(el), acc: accent(el) }),
      (g, w, h, t, s) => {
        g.clearRect(0, 0, w, h);
        const pts = s.p.concat([{ x: mouse.x, y: mouse.y, m: 1 }]);
        for (const p of s.p) { p.x += p.vx; p.y += p.vy; if (p.x < 0 || p.x > w) p.vx *= -1; if (p.y < 0 || p.y > h) p.vy *= -1; }
        for (let i = 0; i < pts.length; i++) for (let j = i + 1; j < pts.length; j++) {
          const a = pts[i], b = pts[j], d = Math.hypot(a.x - b.x, a.y - b.y);
          if (d < link) { g.strokeStyle = b.m ? s.acc : s.col; g.globalAlpha = (1 - d / link) * (b.m ? .9 : .35); g.beginPath(); g.moveTo(a.x, a.y); g.lineTo(b.x, b.y); g.stroke(); }
        }
        g.globalAlpha = 1; g.fillStyle = s.col; for (const p of s.p) { g.beginPath(); g.arc(p.x, p.y, 1.6, 0, 7); g.fill(); }
      });
  };

  fx.snow = (el, { count = 90 } = {}) => canvasFx(el,
    (w, h) => ({ f: Array.from({ length: count }, () => ({ x: Math.random() * w, y: Math.random() * h, r: .6 + Math.random() * 2.2, s: .3 + Math.random() * .9, o: Math.random() * 6 })), col: textCol(el) }),
    (g, w, h, t, s) => {
      g.clearRect(0, 0, w, h); g.fillStyle = s.col;
      for (const p of s.f) { p.y += p.s; p.x += Math.sin(t / 900 + p.o) * .4; if (p.y > h + 4) { p.y = -4; p.x = Math.random() * w; } g.globalAlpha = .4 + p.r / 4; g.beginPath(); g.arc(p.x, p.y, p.r, 0, 7); g.fill(); }
      g.globalAlpha = 1;
    });

  fx["cursor-trail"] = (el, { length = 14 } = {}) => {
    if (UV.reduce) return () => {};
    const dots = Array.from({ length }, (_, i) => { const d = document.createElement("i"); d.style.cssText = `position:fixed;left:0;top:0;width:${10 - i * .5}px;height:${10 - i * .5}px;border-radius:50%;background:${accent(el)};pointer-events:none;z-index:9999;opacity:${1 - i / length};transform:translate(-50%,-50%)`; document.body.appendChild(d); return { d, x: 0, y: 0 }; });
    let mx = -100, my = -100, on = true;
    const mv = e => { mx = e.clientX; my = e.clientY; };
    el.addEventListener("pointermove", mv);
    el.addEventListener("pointerleave", () => { mx = my = -100; });
    (function tick() { if (!on) return; let x = mx, y = my; dots.forEach(p => { p.x += (x - p.x) * .45; p.y += (y - p.y) * .45; p.d.style.left = p.x + "px"; p.d.style.top = p.y + "px"; x = p.x; y = p.y; }); UV.raf(tick); })();
    return () => { on = false; dots.forEach(p => p.d.remove()); el.removeEventListener("pointermove", mv); };
  };

  fx["cursor-blob"] = (el, { size = 120 } = {}) => {
    if (UV.reduce) return () => {};
    const b = document.createElement("i");
    b.style.cssText = `position:fixed;left:0;top:0;width:${size}px;height:${size}px;margin:${-size / 2}px 0 0 ${-size / 2}px;border-radius:50%;background:#fff;mix-blend-mode:difference;pointer-events:none;z-index:9999;transform:scale(0);transition:transform .3s cubic-bezier(.3,1.4,.5,1)`;
    document.body.appendChild(b);
    let x = 0, y = 0, tx = 0, ty = 0, on = true;
    el.addEventListener("pointermove", e => { tx = e.clientX; ty = e.clientY; });
    el.addEventListener("pointerenter", () => { b.style.transform = "scale(1)"; });
    el.addEventListener("pointerleave", () => { b.style.transform = "scale(0)"; });
    (function tick() { if (!on) return; x += (tx - x) * .18; y += (ty - y) * .18; b.style.left = x + "px"; b.style.top = y + "px"; UV.raf(tick); })();
    return () => { on = false; b.remove(); };
  };

  fx.sparkles = (el, { rate = 60 } = {}) => {
    if (UV.reduce) return () => {};
    UV.css("uv-sparkle-css", "@keyframes uv-sparkle{0%{transform:translate(-50%,-50%) scale(0) rotate(0)}50%{transform:translate(-50%,-50%) scale(1) rotate(90deg)}100%{transform:translate(-50%,-50%) scale(0) rotate(180deg)}}");
    if (getComputedStyle(el).position === "static") el.style.position = "relative";
    let last = 0;
    const mv = e => {
      const now = performance.now(); if (now - last < rate) return; last = now;
      const p = UV.local(e, el), s = document.createElement("i"), z = 8 + Math.random() * 12;
      s.style.cssText = `position:absolute;left:${p.x + (Math.random() - .5) * 30}px;top:${p.y + (Math.random() - .5) * 30}px;width:${z}px;height:${z}px;pointer-events:none;z-index:5;background:${accent(el)};clip-path:polygon(50% 0,61% 39%,100% 50%,61% 61%,50% 100%,39% 61%,0 50%,39% 39%);animation:uv-sparkle .7s ease forwards`;
      el.appendChild(s); setTimeout(() => s.remove(), 700);
    };
    el.addEventListener("pointermove", mv);
    return () => el.removeEventListener("pointermove", mv);
  };

  /** Letters push away from the cursor. */
  fx.magnet = (el, { radius = 90, force = 26 } = {}) => {
    if (UV.reduce) return () => {};
    const spans = fx.split(el);
    spans.forEach(s => { s.style.transition = "transform .25s cubic-bezier(.2,.8,.2,1)"; s.style.display = "inline-block"; });
    const mv = e => spans.forEach(s => {
      const r = s.getBoundingClientRect(), dx = r.left + r.width / 2 - e.clientX, dy = r.top + r.height / 2 - e.clientY, d = Math.hypot(dx, dy);
      const k = Math.max(0, 1 - d / radius) * force; s.style.transform = k ? `translate(${dx / (d || 1) * k}px,${dy / (d || 1) * k}px)` : "";
    });
    el.addEventListener("pointermove", mv);
    el.addEventListener("pointerleave", () => spans.forEach(s => { s.style.transform = ""; }));
    return () => el.removeEventListener("pointermove", mv);
  };

  fx["scramble-hover"] = el => { const t = el.textContent; const h = () => UV.scramble ? UV.scramble(el, t, { duration: 500 }) : 0; el.addEventListener("mouseenter", h); return () => el.removeEventListener("mouseenter", h); };

  /** Click anywhere inside el for a firework burst. */
  fx.fireworks = el => {
    const h = e => { if (UV.confetti) UV.confetti({ x: e.clientX, y: e.clientY, count: 70, shapes: "circle", spread: .8 }); };
    el.addEventListener("click", h); return () => el.removeEventListener("click", h);
  };

  /** Click: the element's letters fall with gravity, then reassemble. */
  fx.gravity = el => {
    const spans = fx.split(el); spans.forEach(s => { s.style.display = "inline-block"; });
    const h = () => {
      if (UV.reduce) return;
      spans.forEach(s => { const vx = (Math.random() - .5) * 6; let vy = -Math.random() * 6, x = 0, y = 0, r = 0, vr = (Math.random() - .5) * 20, t = 0;
        (function f() { vy += .5; x += vx; y += vy; r += vr; s.style.transform = `translate(${x}px,${y}px) rotate(${r}deg)`; if (++t < 70) UV.raf(f); else { s.style.transition = "transform .6s cubic-bezier(.3,1.4,.5,1)"; s.style.transform = ""; setTimeout(() => s.style.transition = "", 600); } })(); });
    };
    el.addEventListener("click", h); return () => el.removeEventListener("click", h);
  };

  fx.spotlight = el => { const h = e => { const p = UV.local(e, el); el.style.setProperty("--x", p.x + "px"); el.style.setProperty("--y", p.y + "px"); }; el.addEventListener("pointermove", h); return () => el.removeEventListener("pointermove", h); };
  fx.holo = el => { const h = e => { const p = UV.local(e, el); el.style.setProperty("--gx", (p.x / p.w) * 100 + "%"); }; el.addEventListener("pointermove", h); return () => el.removeEventListener("pointermove", h); };

  /** Split text into <span style="--i:n"> per character (idempotent). */
  fx.split = el => {
    if (el._uvSplit) return el._uvSplit;
    const text = el.textContent; el.setAttribute("aria-label", text); el.textContent = "";
    return (el._uvSplit = [...text].map((ch, i) => { const s = document.createElement("span"); s.textContent = ch === " " ? " " : ch; s.style.setProperty("--i", i); s.setAttribute("aria-hidden", "true"); el.appendChild(s); return s; }));
  };

  fx.list = Object.keys(fx).filter(k => k !== "split" && k !== "list");

  UV.use("fx", root => {
    root.querySelectorAll(".uv-fx-glitch:not([data-text])").forEach(el => el.dataset.text = el.textContent);
    root.querySelectorAll(".uv-fx-wave, .uv-fx-bounce-in").forEach(el => fx.split(el));
    root.querySelectorAll(".uv-fx-spotlight").forEach(el => UV.once(el, "fxspot") && fx.spotlight(el));
    root.querySelectorAll(".uv-fx-holo-card").forEach(el => UV.once(el, "fxholo") && fx.holo(el));
    root.querySelectorAll("[data-uv-fx]").forEach(el => {
      if (!UV.once(el, "fx")) return;
      el.dataset.uvFx.split(/\s+/).forEach(name => fx[name] && fx[name](el));
    });
  });
})(window.UV);

/*! UI Vault 3 · waterfall.js — a soft, filmic waterfall background (WebGL, procedural, no video). MIT
 *  UVWaterfall.mount(el?, { scale, fps, tint })  → stop()        (also UV.fx.waterfall when UV is loaded)
 *  Renders at a fraction of screen resolution and is upscaled by the browser — that is what makes it look
 *  like a blurred, out-of-focus video while staying cheap on integrated GPUs. Pauses when hidden/off-screen.
 */
(function (global) {
  "use strict";
  const FS = `#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
uniform vec2 R; uniform float t;
float h(vec2 p){ p = fract(p * vec2(123.34, 456.21)); p += dot(p, p + 45.32); return fract(p.x * p.y); }
float n(vec2 p){ vec2 i = floor(p), f = fract(p); f = f * f * (3. - 2. * f);
  return mix(mix(h(i), h(i + vec2(1., 0.)), f.x), mix(h(i + vec2(0., 1.)), h(i + vec2(1., 1.)), f.x), f.y); }
float fbm(vec2 p){ float v = 0., a = .5; for (int i = 0; i < 5; i++) { v += a * n(p); p = p * 2.03 + vec2(1.7, 9.2); a *= .5; } return v; }
void main(){
  vec2 uv = gl_FragCoord.xy / R; float asp = R.x / R.y;
  vec2 p = vec2((uv.x - .5) * asp, uv.y);
  // wet rock + moss
  float rock = fbm(p * vec2(2.6, 3.4));
  vec3 col = mix(vec3(.04, .06, .055), vec3(.17, .21, .19), rock);
  col = mix(col, vec3(.11, .23, .12), smoothstep(.45, .78, fbm(p * 4.6 + 3.)) * .75);
  col += vec3(.03, .05, .06) * fbm(vec2(p.x * 7., p.y * 2. - t * .25));
  // the falls: a wobbling column, wider towards the pool
  float x = p.x + .035 * sin(uv.y * 3.1 + .4) + .015 * sin(uv.y * 9. + t * .6);
  float w = .22 + .08 * (1. - uv.y) + .02 * sin(uv.y * 5. + t * .45);
  float edge = (fbm(vec2(uv.y * 3.5 - t * .9, 4.)) - .5) * .12;
  float m = smoothstep(w + edge, w + edge - .09, abs(x));
  float s1 = fbm(vec2(x * 16., p.y * 1.1 + t * 1.5));
  float s2 = fbm(vec2(x * 31. + 5., p.y * 1.9 + t * 2.3));
  float streak = pow(clamp(s1 * .62 + s2 * .5, 0., 1.), 2.1);
  vec3 water = mix(vec3(.5, .68, .74), vec3(.97, .99, 1.), streak);
  col = mix(col, water, m * (.7 + .3 * streak));
  // spray at the edges of the falls
  col = mix(col, vec3(.85, .92, .94), smoothstep(.08, 0., abs(abs(x) - w)) * .25 * fbm(vec2(x * 10., p.y * 4. + t)));
  // plunge pool + foam
  float pool = smoothstep(.27, .04, uv.y);
  float foam = fbm(vec2(p.x * 5.5 + sin(t * .2), p.y * 7. - t * .7));
  vec3 poolc = mix(vec3(.12, .27, .29), vec3(.9, .95, .96), smoothstep(.42, .8, foam) * (.35 + .65 * smoothstep(.6, 0., abs(p.x))));
  col = mix(col, poolc, pool);
  // rising mist
  float mist = fbm(vec2(p.x * 1.8 + t * .04, p.y * 2.4 - t * .22));
  col = mix(col, vec3(.88, .94, .95), mist * smoothstep(.62, 0., uv.y) * .8);
  // canopy at the top corners
  float leaf = fbm(p * 6. + vec2(t * .02, 0.));
  col = mix(col, vec3(.04, .11, .05), smoothstep(.6, .95, uv.y) * smoothstep(.2, .7, abs(p.x)) * smoothstep(.38, .62, leaf) * .9);
  // daylight from above, vignette, grade
  col += vec3(.16, .19, .15) * smoothstep(.35, 1., uv.y) * max(0., 1. - abs(p.x) * .9) * .55;
  col *= .78 + .38 * (1. - length((uv - .5) * vec2(1., 1.25)));
  gl_FragColor = vec4(pow(col, vec3(.95)), 1.);
}`;

  function mount(host = document.body, { scale = .2, fps = (navigator.hardwareConcurrency || 4) >= 8 ? 60 : 30, maxW = 440, tint = true } = {}) {
    const fixed = host === document.body;
    const c = document.createElement("canvas");
    c.className = "uv-waterfall";
    c.setAttribute("aria-hidden", "true");
    c.style.cssText = (fixed ? "position:fixed;" : "position:absolute;") + "inset:0;width:100%;height:100%;z-index:-3;pointer-events:none;display:block;";
    host.prepend(c);
    let veil;
    if (tint) {
      veil = document.createElement("div");
      veil.className = "uv-waterfall-veil";
      veil.setAttribute("aria-hidden", "true");
      veil.style.cssText = (fixed ? "position:fixed;" : "position:absolute;") + "inset:0;z-index:-2;pointer-events:none;";
      c.after(veil);
    }
    const opts = { antialias: false, depth: false, alpha: false, powerPreference: "low-power" };
    const forced2d = /[?#&]wf=2d/.test(location.href);
    const gl = !forced2d && (c.getContext("webgl", opts) || c.getContext("experimental-webgl", opts));
    let pr = null;
    if (gl) {
      const sh = (type, src) => { const x = gl.createShader(type); gl.shaderSource(x, src); gl.compileShader(x); return gl.getShaderParameter(x, gl.COMPILE_STATUS) ? x : null; };
      const vs = sh(gl.VERTEX_SHADER, "attribute vec2 p; void main(){ gl_Position = vec4(p, 0., 1.); }"), fs = sh(gl.FRAGMENT_SHADER, FS);
      if (vs && fs) {
        pr = gl.createProgram(); gl.attachShader(pr, vs); gl.attachShader(pr, fs);
        gl.bindAttribLocation(pr, 0, "p"); gl.linkProgram(pr);
        if (!gl.getProgramParameter(pr, gl.LINK_STATUS)) pr = null;
      }
    }
    if (!gl || !pr) return mount2d(c, veil, fixed, host, { fps: 4 });   // no WebGL → light software fallback
    gl.useProgram(pr);
    gl.bindBuffer(gl.ARRAY_BUFFER, gl.createBuffer());
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 1, -1, -1, 1, 1, 1]), gl.STATIC_DRAW);
    gl.enableVertexAttribArray(0); gl.vertexAttribPointer(0, 2, gl.FLOAT, false, 0, 0);
    const uR = gl.getUniformLocation(pr, "R"), uT = gl.getUniformLocation(pr, "t");
    const size = () => {
      const w = Math.min(maxW, Math.max(160, Math.round(c.clientWidth * scale))), h = Math.max(90, Math.round(w * c.clientHeight / Math.max(1, c.clientWidth)));
      c.width = w; c.height = h; gl.viewport(0, 0, w, h); gl.uniform2f(uR, w, h);
    };
    size(); addEventListener("resize", size);
    const reduce = matchMedia("(prefers-reduced-motion: reduce)").matches;
    let raf = 0, last = 0, t0 = performance.now(), on = true, visible = true;
    const draw = now => { gl.uniform1f(uT, (now - t0) / 1000 + 20); gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4); };
    const loop = now => {
      raf = 0; if (!on) return;
      if (now - last >= 1000 / fps) { last = now; draw(now); }
      if (visible && !document.hidden && !reduce) raf = requestAnimationFrame(loop);
    };
    const kick = () => { if (!raf && on) raf = requestAnimationFrame(loop); };
    document.addEventListener("visibilitychange", kick);
    if (!fixed) new IntersectionObserver(([e]) => { visible = e.isIntersecting; kick(); }).observe(host);
    reduce ? draw(t0 + 4000) : kick();
    return () => { on = false; c.remove(); veil && veil.remove(); removeEventListener("resize", size); };
  }

  /** Canvas 2D fallback for browsers without WebGL: same scene, ~120px wide, upscaled (naturally soft). */
  function mount2d(c, veil, fixed, host, { fps = 12 } = {}) {
    const g = c.getContext("2d");
    const H = (x, y) => { const s = Math.sin(x * 127.1 + y * 311.7) * 43758.5453; return s - Math.floor(s); };
    const N = (x, y) => { const ix = Math.floor(x), iy = Math.floor(y); let fx = x - ix, fy = y - iy; fx = fx * fx * (3 - 2 * fx); fy = fy * fy * (3 - 2 * fy);
      const a = H(ix, iy), b = H(ix + 1, iy), cc = H(ix, iy + 1), d = H(ix + 1, iy + 1); return a + (b - a) * fx + (cc - a) * fy + (a - b - cc + d) * fx * fy; };
    const F = (x, y) => { let v = 0, a = .5; for (let i = 0; i < 4; i++) { v += a * N(x, y); x = x * 2.03 + 1.7; y = y * 2.03 + 9.2; a *= .5; } return v; };
    const mix = (a, b, t) => a + (b - a) * t, sm = (e0, e1, x) => { const t = Math.min(1, Math.max(0, (x - e0) / (e1 - e0))); return t * t * (3 - 2 * t); };
    let w = 0, h = 0, img, base = null;
    const size = () => { w = 96; h = Math.max(32, Math.round(w * c.clientHeight / Math.max(1, c.clientWidth))); c.width = w; c.height = h; img = g.createImageData(w, h); base = null; };
    size(); addEventListener("resize", size);
    // static scene (rock, moss, pool, mist, light, vignette) is computed ONCE; only the falling-water strip animates
    const pixel = (i, j, t, waterOnly) => {
      const u = i / w, v = 1 - j / h, asp = w / h, px = (u - .5) * asp;
      const x = px + .035 * Math.sin(v * 3.1 + .4), wd = .22 + .08 * (1 - v), m = sm(wd + .02, wd - .07, Math.abs(x));
      const o = (j * w + i) * 4;
      if (waterOnly && m <= 0) return;
      let r, gg, b;
      if (base && waterOnly) { r = base[o] / 255; gg = base[o + 1] / 255; b = base[o + 2] / 255; }
      else {
        const rock = F(px * 2.6, v * 3.4), moss = sm(.45, .78, F(px * 4.6 + 3, v * 4.6 + 3)) * .75;
        r = mix(mix(.04, .17, rock), .11, moss); gg = mix(mix(.06, .21, rock), .23, moss); b = mix(mix(.055, .19, rock), .12, moss);
        const pool = sm(.27, .04, v), foam = sm(.42, .8, F(px * 5.5, v * 7)) * (.35 + .65 * sm(.6, 0, Math.abs(px)));
        r = mix(r, mix(.12, .9, foam), pool); gg = mix(gg, mix(.27, .95, foam), pool); b = mix(b, mix(.29, .96, foam), pool);
        const mist = F(px * 1.8, v * 2.4) * sm(.62, 0, v) * .8; r = mix(r, .88, mist); gg = mix(gg, .94, mist); b = mix(b, .95, mist);
        const lit = .16 * sm(.35, 1, v) * Math.max(0, 1 - Math.abs(px) * .9) * .55, vig = .78 + .38 * (1 - Math.hypot(u - .5, (v - .5) * 1.25));
        r = (r + lit) * vig; gg = (gg + lit * 1.2) * vig; b = (b + lit * .9) * vig;
        if (!waterOnly) { base[o] = r * 255; base[o + 1] = gg * 255; base[o + 2] = b * 255; }
      }
      if (m > 0) {
        const st = Math.pow(Math.min(1, N(x * 16, v * 1.1 + t * 1.5) * .62 + N(x * 31 + 5, v * 1.9 + t * 2.3) * .5), 2.1), k = m * (.7 + .3 * st) * (1 - sm(.27, .04, v) * .6);
        r = mix(r, mix(.5, .97, st), k); gg = mix(gg, mix(.68, .99, st), k); b = mix(b, mix(.74, 1, st), k);
      }
      const d = img.data; d[o] = r * 255; d[o + 1] = gg * 255; d[o + 2] = b * 255; d[o + 3] = 255;
    };
    const draw = t => {
      const full = !base || base.length !== w * h * 4;
      if (full) base = new Uint8ClampedArray(w * h * 4);
      for (let j = 0; j < h; j++) for (let i = 0; i < w; i++) pixel(i, j, t, !full);
      g.putImageData(img, 0, 0);
    };
    const reduce = matchMedia("(prefers-reduced-motion: reduce)").matches;
    let on = true, last = 0, raf = 0, visible = true; const t0 = performance.now();
    const loop = now => { raf = 0; if (!on) return; if (now - last >= 1000 / fps) { last = now; draw((now - t0) / 1000 + 20); } if (visible && !document.hidden && !reduce) raf = requestAnimationFrame(loop); };
    const kick = () => { if (!raf && on) raf = requestAnimationFrame(loop); };
    document.addEventListener("visibilitychange", kick);
    if (!fixed) new IntersectionObserver(([e]) => { visible = e.isIntersecting; kick(); }).observe(host);
    reduce ? draw(24) : kick();
    c.dataset.renderer = "2d";
    return () => { on = false; c.remove(); veil && veil.remove(); removeEventListener("resize", size); };
  }

  global.UVWaterfall = { mount };
  if (global.UV) { global.UV.fx = global.UV.fx || {}; global.UV.fx.waterfall = (el, o) => mount(el, o); }
})(window);

/*! UI Vault 3.1.0 · layers.js — stackable effect layers (glow, shadow, border, backdrop, fill, shape, text, hover,
 *  press, entrance, idle, every uv-fx effect, page backgrounds/cursor/scroll) on any element type or a single element.
 *  UV.layers.apply({ btn: { glow: { o: "neon", c: "accent", s: 18 }, hover: { o: "jelly" } }, page: { canvas: { o: "starfield" } } })
 *  UV.layers.css(stack) → the static CSS (for exporting). Needs core.js; uses motion/feedback/fx when present. MIT */
(function (UV) {
  "use strict";
  const DEFS = {"kinds": [{"key": "glow", "label": "Glow", "params": ["c", "s"], "defaults": {"c": "accent", "s": 18}, "options": [{"name": "soft", "label": "Soft", "drop": [[0, 0, "s", "c@55"]]}, {"name": "neon", "label": "Neon", "drop": [[0, 0, ".25*s", "c"], [0, 0, "s", "c@80"], [0, 0, "2*s", "c@40"]]}, {"name": "halo", "label": "Halo", "drop": [[0, 0, ".5*s", "c@70"], [0, 0, "1.6*s", "c@30"]]}, {"name": "under", "label": "Under-glow", "drop": [[0, ".6*s", "s", "c@70"]]}, {"name": "duo", "label": "Two-tone", "drop": [["-.4*s", 0, ".8*s", "c@70"], [".4*s", 0, ".8*s", "c2@70"]]}, {"name": "rainbow", "label": "Rainbow (animated)", "drop": [[0, 0, ".8*s", "c"]], "anim": "uvl-hue"}, {"name": "pulse", "label": "Pulsing", "drop": [[0, 0, ".8*s", "c@70"]], "anim": "uvl-glowpulse"}, {"name": "hover", "label": "Only on hover", "hover": {"drop": [[0, 0, "s", "c"], [0, 0, "2*s", "c@40"]]}}, {"name": "aura", "label": "Aura", "drop": [[0, 0, ".4*s", "c@80"], [0, 0, "1.2*s", "c2@50"], [0, 0, "2.4*s", "c@25"]]}, {"name": "fire", "label": "Fire", "drop": [[0, "-.2*s", ".6*s", "#ffb300"], [0, 0, "1.4*s", "#ff3d00aa"]]}, {"name": "ice", "label": "Ice", "drop": [[0, 0, ".6*s", "#bfe6ff"], [0, 0, "1.6*s", "#2a8be0aa"]]}, {"name": "gold", "label": "Gold", "drop": [[0, 0, ".5*s", "#ffe08a"], [0, 0, "1.4*s", "#c9962eaa"]]}, {"name": "tight", "label": "Tight edge", "drop": [[0, 0, ".15*s", "c"], [0, 0, ".3*s", "c@60"]]}, {"name": "pink", "label": "Hot pink", "drop": [[0, 0, ".5*s", "#ff4fd8"], [0, 0, "1.5*s", "#ff4fd880"]]}]}, {"key": "shadow", "label": "Shadow", "params": ["c", "s"], "defaults": {"c": "black", "s": 16}, "options": [{"name": "soft", "label": "Soft", "drop": [[0, ".4*s", "s", "c@35"]]}, {"name": "float", "label": "Floating", "drop": [[0, "s", "1.4*s", "c@30"], [0, ".15*s", ".3*s", "c@20"]]}, {"name": "hard", "label": "Hard offset", "drop": [[".3*s", ".3*s", 0, "c"]]}, {"name": "hard-accent", "label": "Hard offset (colour 2)", "drop": [[".3*s", ".3*s", 0, "c2"]]}, {"name": "long", "label": "Long", "drop": [[".1*s", ".1*s", 0, "c@60"], [".2*s", ".2*s", 0, "c@45"], [".3*s", ".3*s", 0, "c@30"], [".4*s", ".4*s", 0, "c@18"]]}, {"name": "stacked", "label": "Stacked", "drop": [[".2*s", ".2*s", 0, "c2"], [".4*s", ".4*s", 0, "c"]]}, {"name": "lifted", "label": "Lifted on hover", "hover": {"y": "-.2*s", "drop": [[0, ".6*s", "1.2*s", "c@35"]]}}, {"name": "sharp", "label": "Sharp bottom", "drop": [[0, ".2*s", 0, "c@50"]]}, {"name": "deep", "label": "Deep", "drop": [[0, "1.4*s", "2*s", "c@45"]]}, {"name": "colored", "label": "Coloured (colour 2)", "drop": [[0, ".6*s", "1.2*s", "c2@60"]]}, {"name": "left", "label": "Cast left", "drop": [["-.5*s", ".4*s", ".6*s", "c@35"]]}, {"name": "right", "label": "Cast right", "drop": [[".5*s", ".4*s", ".6*s", "c@35"]]}, {"name": "outline-hard", "label": "Hard outline", "drop": [[".15*s", 0, 0, "c"], ["-.15*s", 0, 0, "c"], [0, ".15*s", 0, "c"], [0, "-.15*s", 0, "c"]]}, {"name": "retro", "label": "Retro double", "drop": [[".2*s", ".2*s", 0, "c2"], [".4*s", ".4*s", 0, "c@80"]]}]}, {"key": "border", "label": "Border", "params": ["c", "c2", "s"], "defaults": {"c": "accent", "c2": "accent-2", "s": 2}, "options": [{"name": "ring", "label": "Ring", "outline": ["s", "solid", "c", "s"]}, {"name": "tight", "label": "Tight line", "outline": ["s", "solid", "c", "-s"]}, {"name": "double", "label": "Double", "outline": ["2*s+1", "double", "c", "s"]}, {"name": "dashed", "label": "Dashed", "outline": ["s", "dashed", "c", "s"]}, {"name": "dotted", "label": "Dotted", "outline": ["s", "dotted", "c", "s"]}, {"name": "gradient", "label": "Gradient", "ring": "gradient"}, {"name": "spin", "label": "Spinning gradient", "ring": "spin"}, {"name": "holo", "label": "Holographic", "ring": "holo"}, {"name": "march", "label": "Marching ants", "ring": "dash"}, {"name": "focus", "label": "Ring on hover", "hover": {"outline": ["s", "solid", "c", "s"]}}, {"name": "thick", "label": "Thick ring", "outline": ["2*s", "solid", "c", 0]}, {"name": "inset", "label": "Inset line", "outline": ["s", "solid", "c", "-2*s"]}, {"name": "double-tight", "label": "Tight double", "outline": ["2*s+1", "double", "c", "-s"]}, {"name": "dash-hover", "label": "Dashed on hover", "hover": {"outline": ["s", "dashed", "c", "s"]}}, {"name": "glow-ring", "label": "Glowing ring", "outline": ["s", "solid", "c", "s"], "drop": [[0, 0, "4*s", "c@70"]]}]}, {"key": "backdrop", "label": "Backdrop", "params": ["s"], "defaults": {"s": 12}, "options": [{"name": "blur", "label": "Blur", "backdrop": ["s", 0.35]}, {"name": "frost", "label": "Frosted glass", "backdrop": ["s", 0.5], "extra": {"border-color": "#ffffff40"}}, {"name": "tint", "label": "Tinted glass", "backdrop": ["s", 0.25], "bg": "color-mix(in srgb, %A 22%, transparent)"}, {"name": "dark", "label": "Smoked glass", "backdrop": ["s", 0.0], "bg": "#0b0b10a6", "fg": "#f2f2f5"}, {"name": "clear", "label": "Clear (no fill)", "backdrop": [0, 0.0], "bg": "transparent"}]}, {"key": "fill", "label": "Fill", "params": ["c", "c2"], "defaults": {"c": "accent", "c2": "accent-2"}, "options": [{"name": "solid", "label": "Solid colour", "bg": "c", "fg": "on(c)"}, {"name": "soft", "label": "Soft tint", "bg": "c@16", "fg": "mix(c,%T,60)"}, {"name": "gradient", "label": "Gradient", "bg": "linear-gradient(135deg, c, c2)", "fg": "#ffffff"}, {"name": "sweep", "label": "Animated gradient", "bg": "linear-gradient(110deg, c, c2, c) 0 0 / 200% 100%", "fg": "#ffffff", "anim": "uvl-pan"}, {"name": "pearl", "label": "Pearlescent", "bg": "linear-gradient(115deg, #ffffff 0%, #f3e8ff 22%, #e0f4ff 42%, #fff4dc 62%, #ffe6f4 80%, #ffffff 100%) 0 0 / 220% 100%", "fg": "#3a2f5a", "hover": {"anim": "uvl-pan"}}, {"name": "holo", "label": "Holo foil", "bg": "linear-gradient(115deg, #ff9ae6, #9ad3ff, #b8ff9a, #fff59a, #ff9ae6, #9ad3ff) 0 0 / 300% 100%", "fg": "#2a1f3d", "anim": "uvl-pan"}, {"name": "chrome", "label": "Chrome", "bg": "linear-gradient(180deg, #ffffff 0%, #dfe3ea 40%, #8a919c 50%, #c9ced6 62%, #f4f6f9 100%)", "fg": "#1b1f26"}, {"name": "gold", "label": "Gold", "bg": "linear-gradient(180deg, #fff3c4 0%, #e6c25a 40%, #9a6f12 52%, #d8b04a 66%, #fff0b8 100%)", "fg": "#2a1a00"}, {"name": "deep-metal", "label": "Deep blue metal", "bg": "linear-gradient(180deg, #a9cbff 0%, #3f74dc 30%, #0a2560 50%, #1d4cad 64%, #86b4ff 100%)", "fg": "#f2f7ff"}, {"name": "metal", "label": "Anodised (colour)", "bg": "linear-gradient(180deg, mix(c,#ffffff,55) 0%, mix(c,#ffffff,12) 44%, mix(c,#000000,30) 54%, mix(c,#ffffff,8) 100%)", "fg": "#ffffff"}, {"name": "ink", "label": "Ink", "bg": "%T", "fg": "%BG"}, {"name": "paper", "label": "Paper", "bg": "%S", "fg": "%T"}, {"name": "stripes", "label": "Stripes", "bg": "repeating-linear-gradient(-45deg, c 0 10px, mix(c,#ffffff,25) 10px 20px)", "fg": "#ffffff"}, {"name": "carbon", "label": "Carbon", "bg": "repeating-linear-gradient(45deg, #1a1a1d 0 4px, #232327 4px 8px)", "fg": "#f2f2f5"}, {"name": "sunset", "label": "Sunset", "bg": "linear-gradient(180deg, #ffe66d, #ff6ec7 50%, #7b2ff7)", "fg": "#ffffff"}, {"name": "mesh", "label": "Mesh", "bg": "radial-gradient(at 15% 20%, c@70, transparent 55%), radial-gradient(at 85% 80%, c2@70, transparent 55%), %S", "fg": "%T"}, {"name": "p-sunset", "label": "Sunset", "bg": "linear-gradient(135deg, #ff512f, #f09819, #ffd86f)", "fg": "#ffffff"}, {"name": "p-ocean", "label": "Ocean", "bg": "linear-gradient(135deg, #2e3192, #1bffff)", "fg": "#ffffff"}, {"name": "p-aurora", "label": "Aurora", "bg": "linear-gradient(135deg, #00c9a7, #845ec2, #ff6f91)", "fg": "#ffffff"}, {"name": "p-peach", "label": "Peach", "bg": "linear-gradient(135deg, #ffecd2, #fcb69f)", "fg": "#3a2010"}, {"name": "p-lagoon", "label": "Lagoon", "bg": "linear-gradient(135deg, #43cea2, #185a9d)", "fg": "#ffffff"}, {"name": "p-berry", "label": "Berry", "bg": "linear-gradient(135deg, #8e2de2, #4a00e0)", "fg": "#ffffff"}, {"name": "p-citrus", "label": "Citrus", "bg": "linear-gradient(135deg, #f7971e, #ffd200)", "fg": "#2a1a00"}, {"name": "p-mint", "label": "Mint", "bg": "linear-gradient(135deg, #d4fc79, #96e6a1)", "fg": "#123018"}, {"name": "p-candy", "label": "Candy", "bg": "linear-gradient(135deg, #ff9a9e, #fecfef, #a18cd1)", "fg": "#3a1030"}, {"name": "p-ember", "label": "Ember", "bg": "linear-gradient(135deg, #ff416c, #ff4b2b)", "fg": "#ffffff"}, {"name": "p-deep-space", "label": "Deep space", "bg": "linear-gradient(135deg, #000428, #004e92)", "fg": "#e8f0ff"}, {"name": "p-lavender", "label": "Lavender", "bg": "linear-gradient(135deg, #e0c3fc, #8ec5fc)", "fg": "#2a2050"}, {"name": "p-vapor", "label": "Vaporwave", "bg": "linear-gradient(135deg, #ff6ad5, #c774e8, #ad8cff, #94d0ff)", "fg": "#ffffff"}, {"name": "p-cosmic", "label": "Cosmic", "bg": "linear-gradient(135deg, #ff00cc, #333399)", "fg": "#ffffff"}, {"name": "p-forest", "label": "Forest", "bg": "linear-gradient(135deg, #134e5e, #71b280)", "fg": "#ffffff"}, {"name": "p-rose-gold", "label": "Rose gold", "bg": "linear-gradient(135deg, #b76e79, #e8c4a0, #f6e0c8)", "fg": "#3a1a1a"}]}, {"key": "shape", "label": "Shape", "params": [], "defaults": {}, "options": [{"name": "square", "label": "Square", "radius": "0px"}, {"name": "crisp", "label": "Crisp 4px", "radius": "4px"}, {"name": "soft", "label": "Soft 10px", "radius": "10px"}, {"name": "round", "label": "Round 18px", "radius": "18px"}, {"name": "pill", "label": "Pill", "radius": "pill"}, {"name": "blob", "label": "Blob", "radius": "blob"}, {"name": "notch", "label": "Notched corners", "clip": "notch"}, {"name": "ticket", "label": "Cut corners", "clip": "cut"}]}, {"key": "text", "label": "Text", "params": ["c"], "defaults": {"c": "accent"}, "options": [{"name": "upper", "label": "Uppercase", "font": {"tt": "uppercase", "ls": ".08em"}}, {"name": "wide", "label": "Wide tracking", "font": {"ls": ".18em"}}, {"name": "mono", "label": "Monospace", "font": {"ff": "mono"}}, {"name": "serif", "label": "Serif italic", "font": {"ff": "serif", "fs": "italic"}}, {"name": "heavy", "label": "Heavy", "font": {"fw": 800}}, {"name": "glow", "label": "Glowing text", "font": {"glow": "c"}}, {"name": "gradient", "label": "Gradient text", "font": {"grad": "linear-gradient(90deg, c, c2)"}}, {"name": "outline", "label": "Outlined", "font": {"stroke": "c"}}, {"name": "emboss", "label": "Embossed", "font": {"emboss": true}}, {"name": "small-caps", "label": "Small caps", "font": {"sc": true}}, {"name": "chrome", "label": "Chrome text", "font": {"grad": "linear-gradient(180deg, #ffffff 0%, #c9ced6 38%, #3b4048 50%, #dfe3ea 62%, #ffffff 100%)"}}, {"name": "gold", "label": "Gold text", "font": {"grad": "linear-gradient(180deg, #fff3c4 0%, #e6c25a 38%, #8a5f0c 52%, #d8b04a 64%, #fff0b8 100%)"}}, {"name": "holo", "label": "Holo text", "font": {"grad": "linear-gradient(115deg, #ff9ae6, #9ad3ff, #b8ff9a, #fff59a, #ff9ae6)"}}, {"name": "deep-metal", "label": "Deep metal text", "font": {"grad": "linear-gradient(180deg, #3f74dc 22%, #d6e6ff 48%, #5b8ce8 54%, #23489a 76%)"}}, {"name": "sunset", "label": "Sunset text", "font": {"grad": "linear-gradient(90deg, #ff512f, #f09819, #ffd86f)"}}, {"name": "light", "label": "Light weight", "font": {"fw": 300, "ls": ".02em"}}]}, {"key": "hover", "label": "Hover", "params": ["s"], "defaults": {"s": 1}, "options": [{"name": "lift", "label": "Lift", "hover": {"y": "-3*s"}}, {"name": "sink", "label": "Sink", "hover": {"y": "2*s"}}, {"name": "pop", "label": "Pop", "hover": {"scale": "1+.06*s"}}, {"name": "shrink", "label": "Shrink", "hover": {"scale": "1-.04*s"}}, {"name": "tilt", "label": "Tilt", "hover": {"rot": "-2*s", "scale": "1+.03*s"}}, {"name": "jelly", "label": "Jelly", "hover": {"anim": "uvl-jelly"}}, {"name": "wobble", "label": "Wobble", "hover": {"anim": "uvl-wobble"}}, {"name": "bounce", "label": "Bounce", "hover": {"anim": "uvl-bounce"}}, {"name": "rubber", "label": "Rubber band", "hover": {"anim": "uvl-rubber"}}, {"name": "heartbeat", "label": "Heartbeat", "hover": {"anim": "uvl-heart"}}, {"name": "shake", "label": "Shake", "hover": {"anim": "uvl-shake"}}, {"name": "swing", "label": "Swing", "hover": {"anim": "uvl-swing"}}, {"name": "brighten", "label": "Brighten", "hover": {"bright": 1.15}}, {"name": "shine", "label": "Shine sweep", "hover": {"shine": true}}, {"name": "float", "label": "Float while hovered", "hover": {"anim": "uvl-float"}}, {"name": "magnetic", "label": "Magnetic (follows cursor)", "js": "magnetic"}, {"name": "tilt3d", "label": "3D tilt (follows cursor)", "js": "tilt"}, {"name": "spotlight", "label": "Spotlight", "js": "spotlight"}, {"name": "big-lift", "label": "Big lift", "hover": {"y": "-6*s", "drop": [[0, "4*s", "6*s", "#00000055"]]}}, {"name": "nudge", "label": "Nudge right", "hover": {"x": "4*s"}}, {"name": "nudge-left", "label": "Nudge left", "hover": {"x": "-4*s"}}, {"name": "twist", "label": "Twist", "hover": {"rot": "6*s"}}, {"name": "dim", "label": "Dim", "hover": {"bright": 0.85}}, {"name": "glow-up", "label": "Glow up", "hover": {"drop": [[0, 0, "14*s", "c"]], "y": "-2*s"}}, {"name": "grow", "label": "Grow", "hover": {"scale": "1+.12*s"}}, {"name": "rubber", "label": "Rubber band", "hover": {"anim": "uvl-rubber"}}]}, {"key": "press", "label": "Press", "params": ["s"], "defaults": {"s": 1}, "options": [{"name": "sink", "label": "Sink", "active": {"y": "2*s"}}, {"name": "squish", "label": "Squish", "active": {"scale": "1-.06*s"}}, {"name": "dim", "label": "Dim", "active": {"bright": 0.85}}, {"name": "ripple", "label": "Ripple", "js": "ripple"}, {"name": "confetti", "label": "Confetti", "js": "confetti"}, {"name": "click", "label": "Click sound", "js": "sound"}, {"name": "sparkle", "label": "Sparkle burst", "js": "sparkle"}, {"name": "pop", "label": "Pop", "active": {"anim": "uvl-pop"}}, {"name": "big-squish", "label": "Big squish", "active": {"scale": "1-.12*s"}}, {"name": "flash", "label": "Flash", "active": {"bright": 1.35}}, {"name": "push", "label": "Push in", "active": {"y": "3*s", "scale": "1-.03*s"}}]}, {"key": "enter", "label": "Entrance", "params": ["t"], "defaults": {"t": 0.6}, "options": [{"name": "fade", "label": "Fade in", "anim": "uvl-in-fade"}, {"name": "rise", "label": "Rise", "anim": "uvl-in-rise"}, {"name": "drop", "label": "Drop", "anim": "uvl-in-drop"}, {"name": "zoom", "label": "Zoom", "anim": "uvl-in-zoom"}, {"name": "blur", "label": "Blur in", "anim": "uvl-in-blur"}, {"name": "left", "label": "Slide from left", "anim": "uvl-in-left"}, {"name": "right", "label": "Slide from right", "anim": "uvl-in-right"}, {"name": "flip", "label": "Flip up", "anim": "uvl-in-flip"}, {"name": "pop", "label": "Pop in", "anim": "uvl-in-pop"}, {"name": "swing", "label": "Swing in", "anim": "uvl-in-swing"}]}, {"key": "idle", "label": "Idle motion", "params": ["t"], "defaults": {"t": 3}, "options": [{"name": "float", "label": "Float", "anim": "uvl-float"}, {"name": "pulse", "label": "Pulse", "anim": "uvl-pulse"}, {"name": "breathe", "label": "Breathe", "anim": "uvl-breathe"}, {"name": "sway", "label": "Sway", "anim": "uvl-sway"}, {"name": "bob", "label": "Bob", "anim": "uvl-bob"}, {"name": "hue", "label": "Colour cycle", "anim": "uvl-hue"}, {"name": "flicker", "label": "Neon flicker", "anim": "uvl-flicker"}, {"name": "wiggle", "label": "Wiggle now and then", "anim": "uvl-wiggle"}, {"name": "spin", "label": "Slow spin", "anim": "uvl-spin"}, {"name": "blink", "label": "Blink", "anim": "uvl-blink"}, {"name": "jelly", "label": "Jelly loop", "anim": "uvl-jelly"}, {"name": "heartbeat", "label": "Heartbeat", "anim": "uvl-heart"}, {"name": "swing", "label": "Swing loop", "anim": "uvl-swing"}, {"name": "bounce", "label": "Bounce loop", "anim": "uvl-bounce"}, {"name": "rubber", "label": "Rubber loop", "anim": "uvl-rubber"}, {"name": "glow-pulse", "label": "Glow pulse", "anim": "uvl-glowpulse"}]}, {"key": "fx", "label": "Effect (uv-fx)", "params": [], "defaults": {}, "options": [{"name": "3d", "label": "3d", "fx": "uv-fx-3d"}, {"name": "bevel", "label": "Bevel", "fx": "uv-fx-bevel"}, {"name": "blob", "label": "Blob", "fx": "uv-fx-blob"}, {"name": "blur-in", "label": "Blur in", "fx": "uv-fx-blur-in"}, {"name": "bounce-in", "label": "Bounce in", "fx": "uv-fx-bounce-in"}, {"name": "breathe", "label": "Breathe", "fx": "uv-fx-breathe"}, {"name": "checker", "label": "Checker", "fx": "uv-fx-checker"}, {"name": "chrome", "label": "Chrome", "fx": "uv-fx-chrome"}, {"name": "corners", "label": "Corners", "fx": "uv-fx-corners"}, {"name": "crt", "label": "Crt", "fx": "uv-fx-crt"}, {"name": "dashed-march", "label": "Dashed march", "fx": "uv-fx-dashed-march"}, {"name": "dither", "label": "Dither", "fx": "uv-fx-dither"}, {"name": "fill-hover", "label": "Fill hover", "fx": "uv-fx-fill-hover"}, {"name": "flip", "label": "Flip", "fx": "uv-fx-flip"}, {"name": "float", "label": "Float", "fx": "uv-fx-float"}, {"name": "frost", "label": "Frost", "fx": "uv-fx-frost"}, {"name": "glitch", "label": "Glitch", "fx": "uv-fx-glitch"}, {"name": "glow", "label": "Glow", "fx": "uv-fx-glow"}, {"name": "gradient-text", "label": "Gradient text", "fx": "uv-fx-gradient-text"}, {"name": "grid-floor", "label": "Grid floor", "fx": "uv-fx-grid-floor"}, {"name": "halftone", "label": "Halftone", "fx": "uv-fx-halftone"}, {"name": "hard-shadow", "label": "Hard shadow", "fx": "uv-fx-hard-shadow"}, {"name": "highlight", "label": "Highlight", "fx": "uv-fx-highlight"}, {"name": "holo", "label": "Holo", "fx": "uv-fx-holo"}, {"name": "holo-card", "label": "Holo card", "fx": "uv-fx-holo-card"}, {"name": "inset", "label": "Inset", "fx": "uv-fx-inset"}, {"name": "jelly", "label": "Jelly", "fx": "uv-fx-jelly"}, {"name": "lift", "label": "Lift", "fx": "uv-fx-lift"}, {"name": "long-shadow", "label": "Long shadow", "fx": "uv-fx-long-shadow"}, {"name": "mesh-anim", "label": "Mesh anim", "fx": "uv-fx-mesh-anim"}, {"name": "mirror", "label": "Mirror", "fx": "uv-fx-mirror"}, {"name": "neon", "label": "Neon", "fx": "uv-fx-neon"}, {"name": "neumorph", "label": "Neumorph", "fx": "uv-fx-neumorph"}, {"name": "noise", "label": "Noise", "fx": "uv-fx-noise"}, {"name": "paper", "label": "Paper", "fx": "uv-fx-paper"}, {"name": "pixel", "label": "Pixel", "fx": "uv-fx-pixel"}, {"name": "pop", "label": "Pop", "fx": "uv-fx-pop"}, {"name": "pulse-ring", "label": "Pulse ring", "fx": "uv-fx-pulse-ring"}, {"name": "rainbow-border", "label": "Rainbow border", "fx": "uv-fx-rainbow-border"}, {"name": "redacted", "label": "Redacted", "fx": "uv-fx-redacted"}, {"name": "rgb", "label": "Rgb", "fx": "uv-fx-rgb"}, {"name": "scanlines", "label": "Scanlines", "fx": "uv-fx-scanlines"}, {"name": "shake", "label": "Shake", "fx": "uv-fx-shake"}, {"name": "shine", "label": "Shine", "fx": "uv-fx-shine"}, {"name": "spin-hover", "label": "Spin hover", "fx": "uv-fx-spin-hover"}, {"name": "spotlight", "label": "Spotlight", "fx": "uv-fx-spotlight"}, {"name": "squiggle", "label": "Squiggle", "fx": "uv-fx-squiggle"}, {"name": "stamp", "label": "Stamp", "fx": "uv-fx-stamp"}, {"name": "sticker", "label": "Sticker", "fx": "uv-fx-sticker"}, {"name": "stripes", "label": "Stripes", "fx": "uv-fx-stripes"}, {"name": "stroke", "label": "Stroke", "fx": "uv-fx-stroke"}, {"name": "swing", "label": "Swing", "fx": "uv-fx-swing"}, {"name": "tape", "label": "Tape", "fx": "uv-fx-tape"}, {"name": "torn", "label": "Torn", "fx": "uv-fx-torn"}, {"name": "vhs", "label": "Vhs", "fx": "uv-fx-vhs"}, {"name": "wave", "label": "Wave", "fx": "uv-fx-wave"}, {"name": "wobble", "label": "Wobble", "fx": "uv-fx-wobble"}]}], "pageKinds": [{"key": "canvas", "label": "Animated background", "params": [], "defaults": {}, "options": [{"name": "waterfall", "label": "Waterfall", "page": {"canvas": "waterfall"}}, {"name": "matrix", "label": "Matrix rain", "page": {"canvas": "matrix"}}, {"name": "starfield", "label": "Starfield", "page": {"canvas": "starfield"}}, {"name": "particles", "label": "Particles", "page": {"canvas": "particles"}}, {"name": "snow", "label": "Snow", "page": {"canvas": "snow"}}]}, {"key": "cursor", "label": "Cursor", "params": [], "defaults": {}, "options": [{"name": "trail", "label": "Trail", "page": {"cursor": "cursor-trail"}}, {"name": "blob", "label": "Blob", "page": {"cursor": "cursor-blob"}}, {"name": "sparkles", "label": "Sparkles", "page": {"cursor": "sparkles"}}]}, {"key": "scroll", "label": "Scrolling", "params": [], "defaults": {}, "options": [{"name": "smooth", "label": "Smooth scrolling", "page": {"scroll": "smooth"}}, {"name": "progress", "label": "Progress bar", "page": {"scroll": "progress"}}, {"name": "both", "label": "Smooth + progress bar", "page": {"scroll": "both"}}]}], "types": [{"key": "btn", "label": "Buttons", "targets": ".uv-btn, .btn"}, {"key": "card", "label": "Cards", "targets": ".uv-card, .res"}, {"key": "input", "label": "Text fields", "targets": ".uv-input, .uv-select, .uv-textarea, .uv-otp input"}, {"key": "switch", "label": "Switches", "targets": ".uv-switch"}, {"key": "check", "label": "Checkboxes", "targets": ".uv-check"}, {"key": "range", "label": "Sliders", "targets": ".uv-range"}, {"key": "tabs", "label": "Tabs", "targets": ".uv-tabs"}, {"key": "nav", "label": "Navigation", "targets": ".uv-nav, .uv-sidenav, .uv-pages, .uv-menu"}, {"key": "badge", "label": "Badges", "targets": ".uv-badge, .uv-chip, .chip"}, {"key": "alert", "label": "Alerts & toasts", "targets": ".uv-alert, .uv-toast"}, {"key": "modal", "label": "Modals", "targets": ".uv-modal, .uv-cmdk"}, {"key": "spinner", "label": "Spinners", "targets": ".uv-spinner"}, {"key": "progress", "label": "Progress", "targets": ".uv-progress"}, {"key": "head", "label": "Headings", "targets": ".uv-h1, .uv-h2, h1, h2"}, {"key": "link", "label": "Links", "targets": "a:not(.uv-btn):not(.btn)"}, {"key": "img", "label": "Images", "targets": "img, .uv-avatar"}, {"key": "text", "label": "Paragraphs", "targets": "p"}, {"key": "page", "label": "Whole page", "targets": "html"}], "colors": ["accent", "accent-2", "text", "white", "black", "success", "warn", "danger", "info"], "keyframes": "@keyframes uvl-hue { to { filter: hue-rotate(360deg); } }\n@keyframes uvl-glowpulse { 50% { opacity: .85; scale: 1.015; } }\n@keyframes uvl-pan { to { background-position: 100% 0; } }\n@keyframes uvl-jelly { 30% { scale: 1.12 .88; } 50% { scale: .92 1.08; } 70% { scale: 1.04 .96; } }\n@keyframes uvl-wobble { 25% { rotate: -4deg; } 50% { rotate: 3deg; } 75% { rotate: -1.5deg; } }\n@keyframes uvl-bounce { 40% { translate: 0 -8px; } 60% { translate: 0 0; } 75% { translate: 0 -3px; } }\n@keyframes uvl-rubber { 30% { scale: 1.2 .8; } 40% { scale: .8 1.2; } 60% { scale: 1.1 .9; } 75% { scale: .97 1.03; } }\n@keyframes uvl-heart { 15%, 45% { scale: 1.1; } 30% { scale: 1; } }\n@keyframes uvl-shake { 20%, 60% { translate: -4px 0; } 40%, 80% { translate: 4px 0; } }\n@keyframes uvl-swing { 20% { rotate: 8deg; } 40% { rotate: -6deg; } 60% { rotate: 3deg; } 80% { rotate: -2deg; } }\n@keyframes uvl-float { 50% { translate: 0 -6px; } }\n@keyframes uvl-pop { 50% { scale: 1.12; } }\n@keyframes uvl-pulse { 50% { scale: 1.05; } }\n@keyframes uvl-breathe { 50% { opacity: .65; } }\n@keyframes uvl-sway { 50% { rotate: 2deg; } } @keyframes uvl-bob { 50% { translate: 0 3px; } }\n@keyframes uvl-flicker { 0%, 19%, 21%, 23%, 55%, 57%, 100% { opacity: 1; } 20%, 22%, 56% { opacity: .5; } }\n@keyframes uvl-wiggle { 0%, 88%, 100% { rotate: 0deg; } 90% { rotate: -5deg; } 93% { rotate: 5deg; } 96% { rotate: -3deg; } }\n@keyframes uvl-spin { to { rotate: 360deg; } } @keyframes uvl-blink { 50% { opacity: .35; } }\n@keyframes uvl-in-fade { from { opacity: 0; } } @keyframes uvl-in-rise { from { opacity: 0; translate: 0 24px; } }\n@keyframes uvl-in-drop { from { opacity: 0; translate: 0 -24px; } } @keyframes uvl-in-zoom { from { opacity: 0; scale: .85; } }\n@keyframes uvl-in-blur { from { opacity: 0; filter: blur(10px); } } @keyframes uvl-in-left { from { opacity: 0; translate: -40px 0; } }\n@keyframes uvl-in-right { from { opacity: 0; translate: 40px 0; } } @keyframes uvl-in-flip { from { opacity: 0; transform: perspective(600px) rotateX(-60deg); } }\n@keyframes uvl-in-pop { 0% { opacity: 0; scale: .6; } 70% { scale: 1.06; } } @keyframes uvl-in-swing { from { opacity: 0; rotate: -8deg; translate: -20px 0; } }\n@keyframes uvl-shine { from { translate: -120% 0; } to { translate: 120% 0; } }\n@keyframes uvl-spin-a { to { --uvl-a: 360deg; } } @keyframes uvl-march { to { background-position: 16px 0, -16px 100%, 0 -16px, 100% 16px; } }\n@property --uvl-a { syntax: \"<angle>\"; initial-value: 0deg; inherits: false; }\n"};
  const KIND = Object.fromEntries([...DEFS.kinds, ...DEFS.pageKinds].map(k => [k.key, k]));
  const TYPE = Object.fromEntries(DEFS.types.map(t => [t.key, t]));
  const UI = ".uvp, .uvp *, .uvp-btn, .uvi, .uvi *";            // the editors themselves never get layered
  const NO_PSEUDO = /^(input|img|range|switch|check|page)$/;     // replaced elements / composites: no ::before/::after rings
  const opt = (kind, name) => (KIND[kind] && KIND[kind].options.find(o => o.name === name)) || null;

  // ── value helpers ────────────────────────────────────────────────────────
  const TOK = { accent: "var(--uv-accent)", "accent-2": "var(--uv-accent-2)", text: "var(--uv-text)", white: "#ffffff", black: "#000000",
    success: "var(--uv-success)", warn: "var(--uv-warn)", danger: "var(--uv-danger)", info: "var(--uv-info)" };
  const col = v => TOK[v] || v || "var(--uv-accent)";
  const num = (expr, s) => typeof expr === "number" ? expr : Function("s", "return " + String(expr))(s);
  function val(str, p) {
    const c = col(p.c), c2 = col(p.c2 || "accent-2");
    return String(str)
      .replace(/%A2/g, "var(--uv-accent-2)").replace(/%A/g, "var(--uv-accent)").replace(/%T/g, "var(--uv-text)").replace(/%BG/g, "var(--uv-bg)").replace(/%S/g, "var(--uv-surface)")
      .replace(/on\(c\)/g, p.c === "white" ? "#111111" : (!p.c || p.c === "accent") ? "var(--uv-on-accent, #fff)" : "#ffffff")
      .replace(/mix\(([^,]+),([^,]+),(\d+)\)/g, (_, a, b, n) => `color-mix(in srgb, ${a.trim()}, ${b.trim()} ${n}%)`)
      .replace(/\bc2@(\d+)/g, (_, n) => `color-mix(in srgb, ${c2} ${n}%, transparent)`)
      .replace(/\bc@(\d+)/g, (_, n) => `color-mix(in srgb, ${c} ${n}%, transparent)`)
      .replace(/\bc2\b/g, c2).replace(/\bc\b/g, c);
  }
  const drops = (list, p) => (list || []).map(([x, y, b, cc]) => `drop-shadow(${num(x, p.s)}px ${num(y, p.s)}px ${Math.max(0, num(b, p.s))}px ${val(cc, p)})`);
  const RADIUS = { pill: "999px", blob: "30% 70% 70% 30% / 30% 30% 70% 70%" };
  const CLIP = { notch: "polygon(12px 0, calc(100% - 12px) 0, 100% 12px, 100% calc(100% - 12px), calc(100% - 12px) 100%, 12px 100%, 0 calc(100% - 12px), 0 12px)",
    cut: "polygon(0 0, calc(100% - 14px) 0, 100% 14px, 100% 100%, 14px 100%, 0 calc(100% - 14px))" };
  const FONT = { mono: "var(--uv-mono)", serif: "var(--uv-serif)", display: "var(--uv-font-head)" };
  const RING = (kind, p) => ({
    gradient: `linear-gradient(135deg, ${col(p.c)}, ${col(p.c2 || "accent-2")})`,
    spin: `conic-gradient(from var(--uvl-a), ${col(p.c)}, ${col(p.c2 || "accent-2")}, transparent 45%, ${col(p.c)})`,
    holo: "conic-gradient(from var(--uvl-a), #ff9ae6, #9ad3ff, #b8ff9a, #fff59a, #ff9ae6)",
    dash: `linear-gradient(90deg, ${col(p.c)} 50%, transparent 0) 0 0 / 16px ${p.s}px repeat-x, linear-gradient(90deg, ${col(p.c)} 50%, transparent 0) 0 100% / 16px ${p.s}px repeat-x, linear-gradient(0deg, ${col(p.c)} 50%, transparent 0) 0 0 / ${p.s}px 16px repeat-y, linear-gradient(0deg, ${col(p.c)} 50%, transparent 0) 100% 0 / ${p.s}px 16px repeat-y`,
  }[kind]);

  // ── CSS for one target (a type's selector or one element's attribute selector) ─
  function block(sel, type, kinds) {
    const S = `:is(${sel}):not(${UI}):not(#_):not(#_)`;
    const P = k => Object.assign({}, (KIND[k] || {}).defaults, kinds[k]);
    const base = [], hov = [], act = [], out = [], filt = [], hfilt = [], afilt = [], anims = [], hanims = [];
    let pseudoBefore = "", pseudoAfter = "", needPos = false, enter = null;
    for (const k of Object.keys(kinds)) {
      const p = P(k), o = opt(k, p.o); if (!o || KIND[k].key === "fx" || o.page) continue;
      filt.push(...drops(o.drop, p));
      if (o.anim && k !== "enter") anims.push(o.anim === "uvl-hue" || o.anim === "uvl-spin" ? `${o.anim} ${k === "idle" ? p.t * 2 : 6}s linear infinite` : `${o.anim} ${k === "idle" ? p.t : 2.4}s ease-in-out infinite`);
      if (o.anim && k === "enter") enter = `${o.anim} ${p.t}s var(--uv-ease, ease) both`;
      if (o.outline) { const [w, st, c, off] = o.outline; base.push(`outline: ${num(w, p.s)}px ${st} ${val(c, p)}; outline-offset: ${num(off, p.s)}px`); }
      if (o.ring && !NO_PSEUDO.test(type)) {
        needPos = true;
        pseudoAfter = `content: ""; position: absolute; inset: 0; z-index: 2; border-radius: inherit; pointer-events: none; padding: ${o.ring === "dash" ? 0 : p.s}px; background: ${RING(o.ring, p)};` +
          (o.ring === "dash" ? " animation: uvl-march 1s linear infinite;" : ` -webkit-mask: linear-gradient(#000 0 0) content-box, linear-gradient(#000 0 0); -webkit-mask-composite: xor; mask: linear-gradient(#000 0 0) content-box exclude, linear-gradient(#000 0 0);${o.ring !== "gradient" ? " animation: uvl-spin-a 3s linear infinite;" : ""}`);
      } else if (o.ring) base.push(`outline: ${p.s}px solid ${col(p.c)}; outline-offset: 1px`);
      if (o.bg) base.push(`background: ${val(o.bg, p)}`);
      if (o.fg) base.push(`color: ${val(o.fg, p)}`);
      if (o.backdrop) {
        const [b, a] = o.backdrop;
        base.push(`backdrop-filter: blur(${num(b, p.s)}px) saturate(1.4); -webkit-backdrop-filter: blur(${num(b, p.s)}px) saturate(1.4)`);
        if (!o.bg) base.push(`background: color-mix(in srgb, var(--uv-surface) ${Math.round(a * 100)}%, transparent)`);
      }
      if (o.extra) for (const [a, b] of Object.entries(o.extra)) base.push(`${a}: ${b}`);
      if (o.radius) base.push(`border-radius: ${RADIUS[o.radius] || o.radius}`);
      if (o.clip) base.push(`clip-path: ${CLIP[o.clip]}`);
      if (o.font) {
        const f = o.font;
        if (f.tt) base.push(`text-transform: ${f.tt}`); if (f.ls) base.push(`letter-spacing: ${f.ls}`);
        if (f.ff) base.push(`font-family: ${FONT[f.ff]}`); if (f.fw) base.push(`font-weight: ${f.fw}`); if (f.fs) base.push(`font-style: ${f.fs}`);
        if (f.sc) base.push("font-variant: small-caps");
        if (f.glow) base.push(`text-shadow: 0 0 6px ${val(f.glow, p)}, 0 0 18px ${val("c@60", p)}`);
        if (f.emboss) base.push("text-shadow: 0 1px 0 #ffffff70, 0 -1px 0 #00000040");
        if (f.stroke) base.push(`-webkit-text-stroke: 1.2px ${val(f.stroke, p)}; color: transparent; -webkit-text-fill-color: transparent`);
        if (f.grad) base.push(`background-image: ${val(f.grad, p)}; -webkit-background-clip: text; background-clip: text; color: transparent; -webkit-text-fill-color: transparent`);
      }
      for (const [state, list, fl, an] of [[o.hover, hov, hfilt, hanims], [o.active, act, afilt, null]]) {
        if (!state) continue;
        if (state.y != null || state.x != null) list.push(`translate: ${num(state.x || 0, p.s)}px ${num(state.y || 0, p.s)}px`);
        if (state.scale != null) list.push(`scale: ${num(state.scale, p.s)}`);
        if (state.rot != null) list.push(`rotate: ${num(state.rot, p.s)}deg`);
        if (state.bright) fl.push(`brightness(${state.bright})`);
        if (state.drop) fl.push(...drops(state.drop, p));
        if (state.outline) { const [w, st, c, off] = state.outline; list.push(`outline: ${num(w, p.s)}px ${st} ${val(c, p)}; outline-offset: ${num(off, p.s)}px`); }
        if (state.anim) (an || list).push(an ? `${state.anim} ${state.anim === "uvl-pan" ? "1.2s ease forwards" : state.anim === "uvl-float" ? "1.6s ease-in-out infinite" : ".55s ease"}` : `animation: ${state.anim} .4s ease`);
        if (state.shine && !NO_PSEUDO.test(type)) { needPos = true; base.push("overflow: hidden"); pseudoBefore = `content: ""; position: absolute; inset: 0; z-index: 1; border-radius: inherit; pointer-events: none; background: linear-gradient(110deg, transparent 35%, #ffffff70 50%, transparent 65%); translate: -120% 0;`; out.push(`${S}:hover::before { animation: uvl-shine .7s ease; }`); }
      }
      if (o.js === "tilt") base.push("transform: perspective(700px) rotateX(var(--uvl-rx, 0deg)) rotateY(var(--uvl-ry, 0deg))");
      if (o.js === "magnetic") base.push("translate: var(--uvl-mx, 0px) var(--uvl-my, 0px)");
      if (o.js === "spotlight" && !NO_PSEUDO.test(type)) { needPos = true; base.push("overflow: hidden"); pseudoBefore = `content: ""; position: absolute; inset: 0; z-index: 1; pointer-events: none; border-radius: inherit; background: radial-gradient(220px circle at var(--uvl-x, 50%) var(--uvl-y, 50%), ${val("c@25", p)}, transparent 65%);`; }
    }
    if (filt.length) base.push(`filter: ${filt.join(" ")}`);
    if (hfilt.length) hov.push(`filter: ${[...filt, ...hfilt].join(" ")}`);
    if (afilt.length) act.push(`filter: ${[...filt, ...afilt].join(" ")}`);
    if (anims.length) base.push(`animation: ${anims.join(", ")}`);
    if (hanims.length) hov.push(`animation: ${[...hanims, ...anims].join(", ")}`);
    if (needPos && type !== "page") base.push("position: relative; isolation: isolate");
    if (hov.length || act.length) base.push("transition: translate .25s var(--uv-ease, ease), scale .25s var(--uv-spring, ease), rotate .25s ease, filter .25s ease, outline-color .2s, background-color .2s, background-position .6s, color .2s, border-color .2s, box-shadow .25s");
    const css = [];
    if (base.length) css.push(`${S} { ${base.join("; ")}; }`);
    if (hov.length) css.push(`${S}:hover { ${hov.join("; ")}; }`);
    if (act.length) css.push(`${S}:active { ${act.join("; ")}; }`);
    if (pseudoBefore) css.push(`${S}::before { ${pseudoBefore} }`);
    if (pseudoAfter) css.push(`${S}::after { ${pseudoAfter} }`);
    if (enter) {
      css.push(`html.uvl-js ${S}:not(.uvl-in) { opacity: 0; }`);
      css.push(`${S}.uvl-in { animation: ${enter}${anims.length ? ", " + anims.map(a => a + "").join(", ") : ""}; }`);
    }
    return css.concat(out).join("\n");
  }

  /** The static CSS for a stack (everything except JS behaviours, uv-fx classes and page effects). */
  function css(stack = {}) {
    const out = [DEFS.keyframes.trim()];
    for (const [type, kinds] of Object.entries(stack)) {
      if (type === "@el" || type === "page" || !TYPE[type]) continue;
      const b = block(TYPE[type].targets, type, kinds); if (b) out.push(`/* ${TYPE[type].label} */\n` + b);
    }
    for (const [key, v] of Object.entries(stack["@el"] || {})) {
      if (!v || !v.kinds || (v.page && v.page !== location.pathname)) continue;
      const b = block(`[data-uvl="${v.id}"]`, v.type, v.kinds); if (b) out.push(`/* one ${v.type} on ${v.page} */\n` + b);
    }
    return out.length > 1 ? out.join("\n") : "";
  }

  // ── runtime: JS behaviours, uv-fx classes, entrance observer, page effects ─
  let style, current = {}, stops = [], io, mo, pending = 0;
  const allTargets = () => {
    const list = [];
    for (const [type, kinds] of Object.entries(current)) if (TYPE[type] && type !== "page" && type !== "@el") list.push([TYPE[type].targets, type, kinds]);
    for (const v of Object.values(current["@el"] || {})) if (v && v.kinds && (!v.page || v.page === location.pathname)) list.push([`[data-uvl="${v.id}"]`, v.type, v.kinds]);
    return list;
  };
  const isUI = el => !!el.closest(".uvp, .uvi, .uvp-btn");
  function tagElements() {
    // per-element picks are stored as a CSS path; give the element its id attribute
    for (const v of Object.values(current["@el"] || {})) {
      if (!v || (v.page && v.page !== location.pathname)) continue;
      try { const el = document.querySelector(v.path); if (el) { el.setAttribute("data-uvl", v.id); if (v.design) el.setAttribute("data-uv-" + v.type, v.design); } } catch {}
    }
    const fxWanted = new Map();
    for (const [sel, , kinds] of allTargets()) {
      const els = [...document.querySelectorAll(sel)].filter(e => !isUI(e));
      const fx = kinds.fx && opt("fx", kinds.fx.o);
      if (fx) els.forEach(e => { (fxWanted.get(e) || fxWanted.set(e, new Set()).get(e)).add(fx.fx); });
      if (kinds.enter && kinds.enter.o) els.forEach(e => { if (!e.classList.contains("uvl-in") && !e._uvlSeen) { e._uvlSeen = 1; io.observe(e); } });
    }
    // add/remove uv-fx classes we manage
    document.querySelectorAll("[data-uvl-fx]").forEach(e => { if (!fxWanted.has(e)) { e.dataset.uvlFx.split(" ").forEach(c => e.classList.remove(c)); e.removeAttribute("data-uvl-fx"); } });
    fxWanted.forEach((set, e) => {
      const want = [...set], had = (e.dataset.uvlFx || "").split(" ").filter(Boolean);
      had.filter(c => !set.has(c)).forEach(c => e.classList.remove(c));
      want.forEach(c => e.classList.add(c));
      if (!e.dataset.text) e.dataset.text = e.textContent.trim().slice(0, 80);
      e.dataset.uvlFx = want.join(" ");
    });
  }
  const kindsFor = el => {
    const res = [];
    for (const [sel, , kinds] of allTargets()) { try { if (el.matches(sel)) res.push(kinds); } catch {} }
    return res;
  };
  const jsFor = (el, name) => kindsFor(el).some(ks => Object.entries(ks).some(([k, v]) => { const o = opt(k, v && v.o); return o && o.js === name; }));
  const hit = e => { const t = e.target.closest && e.target.closest("*"); if (!t || isUI(t)) return null; for (let el = t; el && el !== document; el = el.parentElement) { if (kindsFor(el).length) return el; } return null; };
  function onDown(e) {
    const el = hit(e); if (!el) return;
    if (jsFor(el, "ripple")) {
      const r = el.getBoundingClientRect(), d = Math.max(r.width, r.height), i = document.createElement("span");
      if (getComputedStyle(el).position === "static") el.style.position = "relative";
      el.style.overflow = "hidden";
      i.className = "uv-ripple"; i.style.cssText = `width:${d}px;height:${d}px;left:${e.clientX - r.left - d / 2}px;top:${e.clientY - r.top - d / 2}px`;
      el.appendChild(i); setTimeout(() => i.remove(), 650);
    }
    if (jsFor(el, "sound") && UV.sound) UV.sound("click");
  }
  function onClick(e) {
    const el = hit(e); if (!el) return;
    if (jsFor(el, "confetti") && UV.confetti) UV.confetti({ x: e.clientX, y: e.clientY, count: 80 });
    if (jsFor(el, "sparkle") && UV.confetti) UV.confetti({ x: e.clientX, y: e.clientY, count: 26, spread: .45, shapes: "circle" });
  }
  function onMove(e) {
    const el = hit(e); if (!el) return;
    const r = el.getBoundingClientRect(), x = e.clientX - r.left, y = e.clientY - r.top;
    if (jsFor(el, "tilt")) { el.style.setProperty("--uvl-rx", ((.5 - y / r.height) * 12).toFixed(2) + "deg"); el.style.setProperty("--uvl-ry", ((x / r.width - .5) * 14).toFixed(2) + "deg"); }
    if (jsFor(el, "magnetic")) { el.style.setProperty("--uvl-mx", ((x - r.width / 2) * .25).toFixed(1) + "px"); el.style.setProperty("--uvl-my", ((y - r.height / 2) * .3).toFixed(1) + "px"); }
    if (jsFor(el, "spotlight")) { el.style.setProperty("--uvl-x", x + "px"); el.style.setProperty("--uvl-y", y + "px"); }
    el._uvlMoved = 1;
  }
  function onOut(e) {
    const el = e.target; if (!el || !el._uvlMoved || (e.relatedTarget && el.contains(e.relatedTarget))) return;
    ["--uvl-rx", "--uvl-ry", "--uvl-mx", "--uvl-my"].forEach(p => el.style.removeProperty(p));
  }
  function page(p = {}) {
    stops.forEach(s => { try { s && s(); } catch {} }); stops = [];
    const o = k => p[k] && opt(k, p[k].o);
    const canvas = o("canvas"), cursor = o("cursor"), scroll = o("scroll");
    if (canvas && UV.fx) {
      const host = document.createElement("div"); host.className = "uvl-page-bg";
      host.style.cssText = "position:fixed;inset:0;z-index:-1;pointer-events:none;overflow:hidden";
      document.body.prepend(host);
      const run = UV.fx[canvas.page.canvas] || (canvas.page.canvas === "waterfall" && window.UVWaterfall && (el => window.UVWaterfall.mount(el)));
      let stop = null; try { stop = run && run(host); } catch {}
      stops.push(() => { try { typeof stop === "function" ? stop() : stop && stop.stop && stop.stop(); } catch {} host.remove(); });
    }
    if (cursor && UV.fx && UV.fx[cursor.page.cursor]) { try { stops.push(UV.fx[cursor.page.cursor](document.body)); } catch {} }
    if (scroll) {
      const s = scroll.page.scroll;
      if ((s === "smooth" || s === "both") && UV.smoothScroll && !document.documentElement.hasAttribute("data-uv-smooth")) { try { stops.push(UV.smoothScroll()); } catch {} }
      if ((s === "progress" || s === "both") && UV.scrollbar) { UV.scrollbar(); stops.push(() => document.querySelectorAll(".uv-scrollbar").forEach(b => b.remove())); }
    }
  }
  let lastPage = "";
  function apply(stack = {}) {
    current = stack || {};
    if (!style) { style = document.createElement("style"); style.id = "uv-layers"; document.head.appendChild(style); }
    style.textContent = css(current);
    document.documentElement.classList.add("uvl-js");
    if (!io) io = new IntersectionObserver(es => es.forEach(en => { if (en.isIntersecting) { en.target.classList.add("uvl-in"); io.unobserve(en.target); } }), { rootMargin: "0px 0px -6% 0px" });
    if (!mo) {
      mo = new MutationObserver(() => { if (!pending) pending = setTimeout(() => { pending = 0; tagElements(); }, 250); });
      const start = () => { mo.observe(document.body, { childList: true, subtree: true }); document.addEventListener("pointerdown", onDown, true); document.addEventListener("click", onClick, true); document.addEventListener("pointermove", onMove, { passive: true }); document.addEventListener("pointerout", onOut, true); };
      document.body ? start() : document.addEventListener("DOMContentLoaded", start);
    }
    const go = () => {
      tagElements();
      const pj = JSON.stringify(current.page || {}); if (pj !== lastPage) { lastPage = pj; page(current.page); }
    };
    document.body ? go() : document.addEventListener("DOMContentLoaded", go);
    return current;
  }
  /** A CSS path that finds this element again on this page. */
  function pathOf(el) {
    if (el.id) return "#" + CSS.escape(el.id);
    const parts = [];
    for (let e = el; e && e.nodeType === 1 && e !== document.body; e = e.parentElement) {
      if (e.id) { parts.unshift("#" + CSS.escape(e.id)); break; }
      let i = 1; for (let s = e.previousElementSibling; s; s = s.previousElementSibling) if (s.tagName === e.tagName) i++;
      parts.unshift(e.tagName.toLowerCase() + ":nth-of-type(" + i + ")");
    }
    return parts.join(" > ");
  }
  /** Which element type (btn, card, …) an element belongs to, or null. */
  function typeOf(el) {
    for (let e = el; e && e !== document.documentElement; e = e.parentElement) {
      if (isUI(e)) return null;
      for (const t of DEFS.types) { if (t.key === "page") continue; try { if (e.matches(t.targets)) return [t.key, e]; } catch {} }
    }
    return ["page", document.documentElement];
  }
  /** CSS for one selector (previews, exports): UV.layers.cssFor('[data-x="1"]', "btn", { glow: { o: "neon" } }) */
  const cssFor = (sel, type, kinds) => block(sel, type, kinds);
  UV.layers = { defs: DEFS, kinds: KIND, types: TYPE, apply, css, cssFor, keyframes: DEFS.keyframes, get: () => current, pathOf, typeOf, option: opt };
})(window.UV);
