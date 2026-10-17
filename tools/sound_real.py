#!/usr/bin/env python3
"""Realistic synthesized sounds (no recordings: everything here is made from maths): used by tools/sound_gen.py.

What makes a gunshot sound real rather than like a toy, and how it's built here:
  - No tones. A shot is pressure and noise: a Friedlander blast wave (a near-instant overpressure that decays, then a
    suction phase) from the muzzle, a burst of turbulent gas, a short low "punch" of filtered noise, and for supersonic
    rounds the N-wave crack of the bullet itself.
  - The action: the bolt carrier slamming back and forward, as clusters of metal resonances (modal synthesis).
  - Microphones clip on gunshots: a hard saturation stage gives the dense, loud character.
  - Most of what you hear is the world answering: the shot is convolved (ffmpeg afir) with an impulse response built for
    it: the ground bounce a few milliseconds later, scattered early reflections off terrain and buildings, and a long
    diffuse tail whose highs die faster than its lows (air and foliage soak up treble), so it rolls away.
Foley (magazines, bolts, pumps, brass) gets the same treatment at close range: friction noise with grit for sliding
metal, many-mode resonances for impacts, a small close room.
"""
import math
import os
import struct
import subprocess
import tempfile
import wave

SR = 44100


# ---- Basics ----
def zeros(sec):
    return [0.0] * int(sec * SR)


def add(dst, src, at=0.0, gain=1.0):
    o = int(at * SR)
    if o + len(src) > len(dst):
        dst.extend([0.0] * (o + len(src) - len(dst)))
    for i, v in enumerate(src):
        dst[o + i] += v * gain
    return dst


def white(sec, rnd):
    return [rnd.gauss(0, 0.5) for _ in range(int(sec * SR))]


def onepole_lp(x, fc):
    a = math.exp(-2 * math.pi * fc / SR)
    y, out = 0.0, []
    for v in x:
        y = (1 - a) * v + a * y
        out.append(y)
    return out


def lp(x, fc, order=2):
    for _ in range(order):
        x = onepole_lp(x, fc)
    return x


def hp(x, fc, order=1):
    for _ in range(order):
        low = onepole_lp(x, fc)
        x = [a - b for a, b in zip(x, low)]
    return x


def bp(x, f0, q=1.0):
    w = 2 * math.pi * f0 / SR
    alpha = math.sin(w) / (2 * q)
    b0, b2 = alpha, -alpha
    a0, a1, a2 = 1 + alpha, -2 * math.cos(w), 1 - alpha
    b0, b2, a1, a2 = b0 / a0, b2 / a0, a1 / a0, a2 / a0
    x1 = x2 = y1 = y2 = 0.0
    out = []
    for v in x:
        y = b0 * v + b2 * x2 - a1 * y1 - a2 * y2
        x2, x1, y2, y1 = x1, v, y1, y
        out.append(y)
    return out


def decay_env(x, attack, tau, hold=0.0):
    out = []
    for i, v in enumerate(x):
        t = i / SR
        if t < attack:
            g = t / attack
        elif t < attack + hold:
            g = 1.0
        else:
            g = math.exp(-(t - attack - hold) / tau)
        out.append(v * g)
    return out


def peak(x):
    return max(1e-9, max(abs(v) for v in x))


def norm(x, p=0.95):
    m = peak(x)
    return [v * p / m for v in x]


def sat(x, drive):
    """A clipping microphone preamp: soft, then hard."""
    t = math.tanh(drive)
    return [max(-1.0, min(1.0, math.tanh(v * drive) / t * 1.02)) for v in x]


def trim_tail(x, thresh=0.0006, fade=0.06):
    end = len(x)
    while end > int(0.05 * SR) and abs(x[end - 1]) < thresh:
        end -= 1
    x = x[:end]
    n = min(len(x), int(fade * SR))
    for i in range(n):
        x[len(x) - n + i] *= 1 - i / n
    return x


# ---- Physics pieces ----
def friedlander(t_pos, amp=1.0, b=1.4, sec=None):
    """The blast wave: overpressure rising instantly, decaying to a suction phase about as long again."""
    sec = sec or t_pos * 6
    out = []
    for i in range(int(sec * SR)):
        t = i / SR
        out.append(amp * (1 - t / t_pos) * math.exp(-b * t / t_pos))
    # A real microphone's attack isn't a perfect step: a hair of rise time
    return onepole_lp(out, 14000)


def n_wave(length=0.0005, amp=1.0):
    n = max(4, int(length * SR))
    return [amp * (1 - 2 * i / n) for i in range(n)] + [0.0] * 4


def modes(rnd, sec, partials, strike=0.0008):
    """Struck metal: a cluster of damped resonances (freq, decay, amp), set off by a sharp noise tick."""
    n = int(sec * SR)
    out = [0.0] * n
    for f, d, a in partials:
        ph = rnd.uniform(0, 6.283)
        w = 2 * math.pi * f / SR
        for i in range(n):
            out[i] += a * math.sin(ph + w * i) * math.exp(-i / SR / d)
    tick = decay_env(hp(white(0.01, rnd), 1500), 0.00005, strike)
    add(out, tick, 0, 1.2)
    return out


def metal_hit(rnd, size=1.0, bright=1.0, sec=0.12):
    """A piece of steel hitting steel: lots of inharmonic modes (bigger = lower and longer)."""
    base = rnd.uniform(1400, 2200) / size
    parts = []
    for k in range(14):
        ratio = 1 + k * rnd.uniform(0.6, 1.3) + rnd.uniform(-0.1, 0.1)
        f = base * ratio
        if f > 15000:
            break
        parts.append((f, rnd.uniform(0.004, 0.02) * size / (1 + k * 0.15), rnd.uniform(0.2, 1.0) / (1 + k * 0.3 / bright)))
    return modes(rnd, sec, parts)


def friction(rnd, sec, centre, q=1.5, grit=0.6):
    """Metal sliding on metal: band-limited noise, roughened by tiny stick-slip bursts."""
    n = white(sec, rnd)
    rough = []
    g = 1.0
    for i in range(len(n)):
        if rnd.random() < 0.004:
            g = 1 + rnd.uniform(0, 3) * grit
        g = 1 + (g - 1) * 0.995
        rough.append(n[i] * g)
    x = bp(rough, centre, q)
    # shape: a swell and a fall over the stroke
    return [v * math.sin(math.pi * min(1, i / max(1, len(x)))) ** 0.7 for i, v in enumerate(x)]


# ---- Environments (impulse responses) ----
def ir_outdoor(rnd, length=1.8, rt_low=1.1, rt_high=0.25, ground=0.004, early=8, spread=0.25, wet=0.45):
    """Open air: the direct sound, a ground bounce, scattered early reflections (terrain, trees, buildings) and a long
    diffuse tail whose treble dies faster than its bass."""
    n = int(length * SR)
    ir = [0.0] * n
    ir[0] = 1.0
    gi = int(ground * SR)
    if gi < n:
        ir[gi] += 0.55
    for _ in range(early):
        t = rnd.uniform(0.02, spread)
        k = int(t * SR)
        if k < n:
            ir[k] += rnd.uniform(0.15, 0.45) * math.exp(-t / 0.35) * (1 if rnd.random() < 0.7 else -1)
    # The diffuse tail: highs die over rt_high, lows over rt_low (the RT60s), scaled by *energy* so the whole tail
    # carries `wet` x the direct sound's energy (sample by sample it's tiny; there are tens of thousands of them)
    bright = [rnd.gauss(0, 1) * math.exp(-i / SR / rt_high * 6.9) for i in range(n)]
    dark = lp([rnd.gauss(0, 1) * math.exp(-i / SR / rt_low * 6.9) for i in range(n)], 700, 2)
    onset = int(0.012 * SR)
    tail = [0.0] * n
    for i in range(onset, n):
        ramp = min(1.0, (i - onset) / (0.03 * SR))
        tail[i] = (bright[i] * 0.35 + dark[i] * 1.0) * ramp
    e = sum(v * v for v in tail) or 1.0
    k = math.sqrt(wet * wet * 0.5 / e)
    for i in range(n):
        ir[i] += tail[i] * k
    return ir


def ir_room(rnd, length=0.25, rt=0.12, wet=0.3):
    """A small close space (foley): a few quick reflections and a short tail."""
    n = int(length * SR)
    ir = [0.0] * n
    ir[0] = 1.0
    for _ in range(6):
        k = int(rnd.uniform(0.002, 0.02) * SR)
        ir[k] += rnd.uniform(0.1, 0.3) * (1 if rnd.random() < 0.6 else -1)
    tail = [0.0] * n
    for i in range(int(0.003 * SR), n):
        tail[i] = rnd.gauss(0, 1) * math.exp(-i / SR / rt * 6.9)
    e = sum(v * v for v in tail) or 1.0
    k = math.sqrt(wet * wet * 0.3 / e)
    return lp([a + b * k for a, b in zip(ir, tail)], 9000, 1)


def write_wav(path, x):
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, v)) * 32000)) for v in x))


def read_wav(path):
    with wave.open(path, "rb") as w:
        raw = w.readframes(w.getnframes())
    return [v / 32768.0 for v in struct.unpack("<%dh" % (len(raw) // 2), raw)]


def convolve(x, ir):
    """x * ir, by ffmpeg's partitioned FFT convolution (no gain games: plain maths), in 32-bit float end to end so
    nothing clips on the way (clipping there shows up as clicks)."""
    d = tempfile.mkdtemp()
    a, b, o = os.path.join(d, "x.f32"), os.path.join(d, "ir.f32"), os.path.join(d, "o.f32")
    sx, si = peak(x), peak(ir)
    with open(a, "wb") as f:
        f.write(struct.pack("<%df" % (len(x) + len(ir)), *([v / sx * 0.5 for v in x] + [0.0] * len(ir))))
    with open(b, "wb") as f:
        f.write(struct.pack("<%df" % len(ir), *[v / si * 0.5 for v in ir]))
    raw = ["-f", "f32le", "-ar", str(SR), "-ac", "1"]
    subprocess.run(["ffmpeg", "-hide_banner", "-loglevel", "error", "-y"] + raw + ["-i", a] + raw + ["-i", b] + ["-filter_complex",
                    "[0][1]afir=gtype=-1:irnorm=-1:dry=1:wet=1", "-f", "f32le", "-ac", "1", "-ar", str(SR), o], check=True)
    with open(o, "rb") as f:
        data = f.read()
    y = list(struct.unpack("<%df" % (len(data) // 4), data))
    for f in (a, b, o):
        os.unlink(f)
    os.rmdir(d)
    return y


# ---- Gunshots ----
def gunshot(rnd, cal=1.0, supersonic=True, auto=True, rumble=1.0, room="field", bright=1.0):
    """cal: how big (0.6 pistol .. 1.0 rifle .. 1.8 .50 BMG); auto: the action cycles; rumble: the low body."""
    x = zeros(0.25)
    # The bullet's crack arrives first for supersonic rounds (it outruns the blast)
    if supersonic:
        add(x, n_wave(0.0004 * cal, 0.9), 0.0)
    t0 = 0.0012 if supersonic else 0.0
    # Muzzle blast: Friedlander wave (longer for bigger charges) + turbulent gas
    add(x, friedlander(0.0006 + 0.0011 * cal, 1.6), t0)
    gas = decay_env(hp(white(0.25, rnd), 220), 0.0002, 0.006 + 0.012 * cal)
    add(x, lp(gas, 7000 * bright, 1), t0, 0.9)
    # The body: a short, low push of filtered noise (no tones)
    body = decay_env(lp(white(0.4, rnd), 160 + 60 / cal, 3), 0.001, 0.03 + 0.05 * cal)
    add(x, body, t0, 2.6 * rumble)
    # The action cycling: carrier back, buffer, forward and lock
    if auto:
        add(x, metal_hit(rnd, 0.8, 1.0, 0.06), t0 + rnd.uniform(0.018, 0.03), 0.08)
        add(x, metal_hit(rnd, 0.9, 0.8, 0.06), t0 + rnd.uniform(0.045, 0.07), 0.06)
    # The microphone clips
    x = sat(norm(x), 3.2)
    if room == "field":
        ir = ir_outdoor(rnd, 2.2, 1.3 * (0.8 + 0.3 * cal), 0.3, 0.004, 10, 0.32, 0.5)
    elif room == "close":
        ir = ir_outdoor(rnd, 1.2, 0.7, 0.18, 0.003, 6, 0.15, 0.35)
    else:
        ir = ir_outdoor(rnd, 3.0, 2.0, 0.45, 0.005, 14, 0.5, 0.65)
    y = convolve(x, ir)
    return trim_tail(norm(y, 0.97))


def shotgun_blast(rnd):
    x = zeros(0.25)
    add(x, friedlander(0.0022, 1.7), 0.0)
    gas = decay_env(hp(white(0.25, rnd), 160), 0.0003, 0.024)
    add(x, lp(gas, 5200, 1), 0.0, 1.1)
    add(x, decay_env(lp(white(0.5, rnd), 150, 3), 0.001, 0.11), 0.0, 3.2)
    x = sat(norm(x), 3.6)
    y = convolve(x, ir_outdoor(rnd, 2.4, 1.6, 0.32, 0.004, 10, 0.32, 0.55))
    return trim_tail(norm(y, 0.97))


def suppressed_shot(rnd):
    """Suppressed: the blast is mostly trapped, so you hear a dull thump, the gas venting, and the action."""
    x = zeros(0.3)
    add(x, friedlander(0.0016, 0.5), 0.0)
    add(x, decay_env(bp(white(0.2, rnd), 900, 0.8), 0.0005, 0.012), 0.0, 0.9)
    add(x, decay_env(lp(white(0.3, rnd), 300, 2), 0.001, 0.035), 0.0, 1.4)
    add(x, metal_hit(rnd, 0.8, 1.2, 0.08), rnd.uniform(0.006, 0.012), 0.5)
    add(x, metal_hit(rnd, 0.9, 1.0, 0.08), rnd.uniform(0.04, 0.06), 0.35)
    x = sat(norm(x), 2.0)
    y = convolve(x, ir_outdoor(rnd, 0.9, 0.5, 0.15, 0.003, 5, 0.12, 0.25))
    return trim_tail(norm(y, 0.8))


def distant_fire(rnd, burst=1):
    """Gunfire far off: the highs are gone, the reports roll off hills and come back again and again."""
    x = zeros(0.1)
    t = 0.0
    for _ in range(burst):
        shot = decay_env(lp(white(0.5, rnd), rnd.uniform(400, 700), 3), 0.003, rnd.uniform(0.04, 0.08))
        add(x, shot, t, rnd.uniform(0.7, 1.0))
        t += rnd.uniform(0.07, 0.11) if burst > 1 else 0
    ir = ir_outdoor(rnd, 4.0, 2.6, 0.5, 0.012, 18, 1.8, 0.9)
    y = convolve(norm(x), ir)
    y = lp(y, 1400, 1)
    return trim_tail(norm(y, 0.85), fade=0.3)


def explosion(rnd):
    """A grenade: a big blast wave, a long rolling rumble, and debris pattering down."""
    x = zeros(1.6)
    add(x, friedlander(0.006, 1.8, sec=0.08), 0.0)
    add(x, decay_env(hp(white(0.3, rnd), 120), 0.0005, 0.03), 0.0, 1.2)
    add(x, decay_env(lp(white(1.6, rnd), 90, 3), 0.004, 0.45), 0.0, 4.0)
    for _ in range(rnd.randint(18, 30)):
        t = rnd.uniform(0.25, 1.4)
        grain = decay_env(hp(white(0.03, rnd), rnd.uniform(1500, 4000)), 0.0002, rnd.uniform(0.002, 0.008))
        add(x, grain, t, rnd.uniform(0.03, 0.12) * math.exp(-(t - 0.25) / 0.6))
    x = sat(norm(x), 3.0)
    y = convolve(x, ir_outdoor(rnd, 3.2, 2.2, 0.4, 0.005, 14, 0.6, 0.6))
    return trim_tail(norm(y, 0.97), fade=0.2)


# ---- Foley: the gun in your hands ----
def foley(rnd, x):
    return trim_tail(norm(convolve(norm(x), ir_room(rnd)), 0.85))


def mag_out(rnd):
    x = zeros(0.45)
    add(x, metal_hit(rnd, 0.7, 1.3, 0.05), 0.0, 0.7)                       # the release button
    add(x, friction(rnd, 0.16, 1500, 1.2), 0.02, 0.5)                       # the mag sliding down the well
    add(x, metal_hit(rnd, 1.3, 0.7, 0.12), 0.19, 0.4)                       # free of the well
    return foley(rnd, x)


def mag_in(rnd):
    x = zeros(0.45)
    add(x, friction(rnd, 0.11, 1300, 1.4), 0.0, 0.5)
    add(x, metal_hit(rnd, 1.0, 1.0, 0.1), 0.11, 1.0)                        # seated
    add(x, decay_env(lp(white(0.08, rnd), 500, 2), 0.0005, 0.012), 0.11, 1.4)   # the thump of the hand
    add(x, metal_hit(rnd, 0.6, 1.4, 0.05), 0.125, 0.5)                      # the catch clicks
    return foley(rnd, x)


def rack(rnd):
    x = zeros(0.45)
    add(x, metal_hit(rnd, 0.8, 1.1, 0.06), 0.0, 0.6)
    add(x, friction(rnd, 0.09, 2400, 2.0, 0.9), 0.01, 0.5)                 # back
    add(x, metal_hit(rnd, 0.9, 1.0, 0.08), 0.1, 0.5)
    add(x, friction(rnd, 0.06, 2600, 2.0, 0.9), 0.12, 0.4)                 # spring slams it home
    add(x, metal_hit(rnd, 1.0, 0.9, 0.12), 0.18, 1.0)
    return foley(rnd, x)


def bolt(rnd):
    x = zeros(0.8)
    add(x, metal_hit(rnd, 0.9, 1.0, 0.08), 0.0, 0.8)                        # up
    add(x, friction(rnd, 0.15, 1800, 1.4), 0.06, 0.6)                       # back
    add(x, metal_hit(rnd, 0.8, 1.1, 0.08), 0.22, 0.6)
    add(x, friction(rnd, 0.13, 2000, 1.4), 0.31, 0.5)                       # forward
    add(x, metal_hit(rnd, 1.0, 0.9, 0.12), 0.46, 1.0)                       # down, locked
    return foley(rnd, x)


def pump(rnd):
    x = zeros(0.6)
    add(x, friction(rnd, 0.13, 900, 1.0, 0.8), 0.0, 0.8)
    add(x, metal_hit(rnd, 1.3, 0.8, 0.12), 0.13, 1.0)
    add(x, friction(rnd, 0.11, 1000, 1.0, 0.8), 0.2, 0.7)
    add(x, metal_hit(rnd, 1.2, 0.9, 0.12), 0.32, 1.0)
    return foley(rnd, x)


def shell_in(rnd):
    x = zeros(0.3)
    add(x, friction(rnd, 0.05, 1200, 1.2), 0.0, 0.5)
    add(x, metal_hit(rnd, 1.1, 0.8, 0.08), 0.05, 0.9)
    add(x, decay_env(lp(white(0.05, rnd), 600, 2), 0.0005, 0.01), 0.05, 0.8)
    return foley(rnd, x)


def dry_fire(rnd):
    x = zeros(0.15)
    add(x, metal_hit(rnd, 0.6, 1.5, 0.04), 0, 1.0)
    return foley(rnd, x)


def brass(rnd):
    """Empty cases on hard ground: bright, hollow, bouncing a few times, each bounce sooner and quieter."""
    x = zeros(0.7)
    t, g, gap = 0.0, 1.0, rnd.uniform(0.07, 0.12)
    for _ in range(rnd.randint(3, 6)):
        base = rnd.uniform(4800, 6400)
        parts = [(base * r * rnd.uniform(0.98, 1.02), rnd.uniform(0.03, 0.08), a) for r, a in ((1, 1.0), (1.58, 0.6), (2.21, 0.45), (2.9, 0.25), (0.62, 0.3))]
        add(x, modes(rnd, 0.18, parts, 0.0004), t, g)
        t += gap
        gap *= rnd.uniform(0.55, 0.75)
        g *= rnd.uniform(0.45, 0.65)
    y = convolve(norm(x), ir_outdoor(rnd, 0.5, 0.3, 0.12, 0.002, 4, 0.06, 0.15))
    return trim_tail(norm(y, 0.5))


def heartbeat(rnd):
    """Felt more than heard: two low thuds of filtered noise, no tone."""
    x = zeros(0.75)
    add(x, decay_env(lp(white(0.25, rnd), 70, 3), 0.008, 0.045), 0.0, 1.0)
    add(x, decay_env(lp(white(0.25, rnd), 60, 3), 0.008, 0.04), 0.24, 0.75)
    return trim_tail(norm(x, 0.95))


def ringing(rnd):
    """After a blast: a thin high whine with a little wavering, fading out."""
    n = int(2.6 * SR)
    f = rnd.uniform(6200, 7600)
    out = []
    ph = 0.0
    for i in range(n):
        t = i / SR
        ph += 2 * math.pi * f * (1 + 0.002 * math.sin(2 * math.pi * 0.7 * t)) / SR
        out.append(math.sin(ph) * math.exp(-t / 0.9) * min(1, t / 0.05) * 0.4)
    return trim_tail(out)


# ---- The world around you (ambience) ----
def wind(rnd, sec=7.0):
    """Wind: broad noise whose brightness and loudness swell and fall in gusts, with a soft whistle at the peaks."""
    n = int(sec * SR)
    x = white(sec, rnd)
    # gust shape: a few overlapping slow swells
    g = [0.0] * n
    for _ in range(3):
        c, w, a = rnd.uniform(0.2, 0.8) * sec, rnd.uniform(1.2, 2.6), rnd.uniform(0.5, 1.0)
        for i in range(n):
            t = i / SR
            g[i] += a * math.exp(-((t - c) / w) ** 2)
    out, y1, y2 = [], 0.0, 0.0
    for i, v in enumerate(x):
        fc = 250 + 900 * g[i]
        a = math.exp(-2 * math.pi * fc / SR)
        y1 = (1 - a) * v + a * y1
        y2 = (1 - a) * y1 + a * y2
        out.append(y2 * (0.25 + g[i]))
    whistle = bp(white(sec, rnd), rnd.uniform(700, 1100), 12)
    out = [o + w * gg * 0.4 for o, w, gg in zip(out, whistle, g)]
    for i in range(n):
        f = min(1, i / (0.8 * SR), (n - i) / (0.8 * SR))
        out[i] *= f
    return norm(out, 0.7)


def bird(rnd):
    """A songbird's phrase: a run of quick whistled chirps sweeping in pitch, then the woods answering faintly."""
    x = zeros(1.6)
    t = 0.0
    style = rnd.randint(0, 2)
    base = rnd.uniform(2600, 4200)
    for k in range(rnd.randint(4, 9)):
        dur = rnd.uniform(0.03, 0.11)
        f0 = base * rnd.uniform(0.85, 1.3)
        f1 = f0 * (rnd.uniform(1.2, 1.6) if style == 0 else rnd.uniform(0.55, 0.8) if style == 1 else rnd.uniform(0.9, 1.1))
        n = int(dur * SR)
        ph, chirp = 0.0, []
        for i in range(n):
            u = i / n
            f = f0 + (f1 - f0) * u + 180 * math.sin(2 * math.pi * 38 * i / SR) * (style == 2)
            ph += 2 * math.pi * f / SR
            env = math.sin(math.pi * u) ** 1.5
            chirp.append((math.sin(ph) + 0.12 * math.sin(2 * ph)) * env)
        add(x, chirp, t, rnd.uniform(0.6, 1.0))
        t += dur + rnd.uniform(0.02, 0.09)
        if t > 1.3:
            break
    y = convolve(norm(x), ir_outdoor(rnd, 0.9, 0.6, 0.3, 0.003, 6, 0.2, 0.35))
    return trim_tail(norm(y, 0.6))


def crickets(rnd, sec=4.0):
    """Crickets at night: a high carrier, pulsed in quick trains, a few insects slightly out of step."""
    n = int(sec * SR)
    out = [0.0] * n
    for _ in range(rnd.randint(2, 4)):
        f = rnd.uniform(4200, 5200)
        rate = rnd.uniform(25, 40)
        train = rnd.uniform(0.25, 0.45)
        gap = rnd.uniform(0.2, 0.5)
        off = rnd.uniform(0, 0.4)
        amp = rnd.uniform(0.4, 1.0)
        for i in range(n):
            t = i / SR + off
            ph = t % (train + gap)
            if ph < train:
                edge = min(1.0, ph / 0.03, (train - ph) / 0.03)          # each train fades in and out: no clicks
                pulse = max(0.0, math.sin(math.pi * ((t * rate) % 1.0))) ** 3
                out[i] += amp * edge * pulse * math.sin(2 * math.pi * f * t)
    for i in range(n):
        out[i] *= min(1, i / (0.3 * SR), (n - i) / (0.3 * SR))
    return norm(out, 0.35)


def cave_drip(rnd):
    """A water drop in a cave: a tiny falling 'plip', then the cave's long, dark echo."""
    x = zeros(0.1)
    f0 = rnd.uniform(1400, 2400)
    n = int(0.05 * SR)
    ph = 0.0
    plip = []
    for i in range(n):
        u = i / n
        f = f0 * (1.5 - 0.6 * u)
        ph += 2 * math.pi * f / SR
        plip.append(math.sin(ph) * math.exp(-u * 5))
    add(x, plip, 0, 1.0)
    y = convolve(norm(x), ir_outdoor(rnd, 2.6, 2.2, 0.9, 0.006, 16, 0.4, 1.4))
    return trim_tail(norm(y, 0.5), fade=0.3)


def cave_rumble(rnd, sec=5.0):
    """Deep underground: a very low, slow rumble, as if the stone itself were settling."""
    x = lp(white(sec, rnd), 60, 3)
    n = len(x)
    for i in range(n):
        x[i] *= min(1, i / (1.2 * SR), (n - i) / (1.2 * SR)) * (0.6 + 0.4 * math.sin(2 * math.pi * 0.15 * i / SR))
    return norm(x, 0.6)


# ---- Footsteps ----
def grains(rnd, sec, count, lo, hi, size=(0.0005, 0.003)):
    """Lots of tiny noise bursts: crunching (grass, gravel, snow, sand)."""
    x = zeros(sec)
    for _ in range(count):
        t = rnd.uniform(0, sec * 0.85) ** 1.3 / (sec * 0.85) ** 0.3
        g = decay_env(bp(white(0.012, rnd), rnd.uniform(lo, hi), 1.5), 0.0002, rnd.uniform(*size))
        add(x, g, t, rnd.uniform(0.3, 1.0))
    return x


def footstep(rnd, surface):
    """One step: a heel strike and a softer toe roll, each made of what the ground's made of."""
    x = zeros(0.4)
    toe = rnd.uniform(0.07, 0.11)
    for at, k in ((0.0, 1.0), (toe, rnd.uniform(0.4, 0.6))):
        thump = decay_env(lp(white(0.08, rnd), 140, 3), 0.002, 0.018)
        add(x, thump, at, 1.6 * k)
        if surface == "stone":
            add(x, decay_env(hp(white(0.02, rnd), 1800), 0.0002, 0.0025), at, 0.9 * k)
            add(x, grains(rnd, 0.04, 6, 2500, 6000), at, 0.25 * k)
        elif surface == "grass":
            add(x, grains(rnd, 0.09, 40, 1500, 5000, (0.0008, 0.004)), at, 0.5 * k)
        elif surface == "gravel":
            add(x, grains(rnd, 0.14, 90, 900, 5500, (0.001, 0.005)), at, 0.9 * k)
        elif surface == "wood":
            base = rnd.uniform(160, 230)
            add(x, modes(rnd, 0.06, [(base, 0.014, 1.0), (base * 2.3, 0.01, 0.6), (base * 4.1, 0.007, 0.35), (base * 6.6, 0.005, 0.2)], 0.001), at, 0.55 * k)
            add(x, decay_env(bp(white(0.03, rnd), 1200, 1.2), 0.0003, 0.004), at, 0.5 * k)
        elif surface == "sand":
            add(x, lp(grains(rnd, 0.1, 50, 800, 3500, (0.002, 0.008)), 3000, 1), at, 0.45 * k)
        elif surface == "snow":
            add(x, grains(rnd, 0.12, 60, 700, 3000, (0.002, 0.006)), at, 0.6 * k)
            add(x, decay_env(bp(white(0.06, rnd), rnd.uniform(1000, 1500), 6), 0.01, 0.03), at + 0.02, 0.3 * k)
        elif surface == "metal":
            add(x, metal_hit(rnd, 1.6, 0.7, 0.15), at, 0.5 * k)
    y = convolve(norm(x), ir_room(rnd, 0.15, 0.05, 0.15))
    return trim_tail(norm(y, 0.6))


# ---- The night hunter ----
def hunter_click(rnd):
    """Rapid, resonant clicking: a run of hard little ticks through a hollow body, then a pause, then a few more."""
    x = zeros(1.3)
    t = 0.0
    for burst in range(rnd.randint(2, 3)):
        for k in range(rnd.randint(6, 14)):
            tick = decay_env(hp(white(0.01, rnd), 1200), 0.0001, 0.0012)
            body = modes(rnd, 0.03, [(rnd.uniform(700, 900), 0.008, 1.0), (rnd.uniform(1700, 2100), 0.005, 0.5)], 0.0003)
            add(x, tick, t, 1.0)
            add(x, body, t, 0.6)
            t += rnd.uniform(0.018, 0.034)
        t += rnd.uniform(0.15, 0.3)
    y = convolve(norm(x), ir_outdoor(rnd, 1.0, 0.8, 0.3, 0.004, 6, 0.2, 0.4))
    return trim_tail(norm(y, 0.7))


def hunter_whisper(rnd):
    """Something breathing words that aren't words: noise through shifting mouth shapes (formants), very close."""
    sec = 1.6
    n = int(sec * SR)
    out = [0.0] * n
    base = white(sec, rnd)
    sylls = rnd.randint(3, 6)
    for s in range(sylls):
        t0 = s * sec / sylls + rnd.uniform(0, 0.05)
        dur = sec / sylls * rnd.uniform(0.5, 0.85)
        f1, f2 = rnd.uniform(400, 900), rnd.uniform(1100, 2600)
        seg = base[int(t0 * SR):int((t0 + dur) * SR)]
        if not seg:
            continue
        v = [a + b for a, b in zip(bp(seg, f1, 4), bp(seg, f2, 5))]
        v = [x * math.sin(math.pi * i / len(v)) ** 1.5 for i, x in enumerate(v)]
        add(out, v, t0, 1.0)
    return trim_tail(norm(hp(out, 300), 0.6))


def hunter_shriek(rnd):
    """A screech: a rasping cry falling in pitch, breaking up, with the dark answering."""
    sec = 1.1
    n = int(sec * SR)
    out = []
    ph = 0.0
    f0 = rnd.uniform(1600, 2200)
    for i in range(n):
        t = i / n
        f = f0 * (1 - 0.55 * t) * (1 + 0.04 * math.sin(2 * math.pi * 23 * i / SR))
        ph += 2 * math.pi * f / SR
        rasp = math.sin(ph) + 0.6 * math.sin(2.01 * ph) + 0.35 * math.sin(3.02 * ph)
        out.append(math.tanh(rasp * 2.5) * math.sin(math.pi * min(1, t * 6)) * (1 - t) ** 0.6)
    noise_part = decay_env(bp(white(sec, rnd), 2400, 1.2), 0.01, 0.35)
    out = [a + b * 0.5 for a, b in zip(out, noise_part)]
    y = convolve(norm(out), ir_outdoor(rnd, 2.0, 1.5, 0.4, 0.005, 10, 0.4, 0.6))
    return trim_tail(norm(y, 0.9), fade=0.2)


def ricochet(rnd):
    """A round glancing off steel: a sharp ping, then the whine of it tumbling away."""
    x = zeros(0.6)
    add(x, metal_hit(rnd, 0.5, 1.5, 0.08), 0, 0.9)
    n = int(0.45 * SR)
    ph = 0.0
    f0 = rnd.uniform(2600, 3600)
    whine = []
    for i in range(n):
        u = i / n
        ph += 2 * math.pi * f0 * (1 - 0.5 * u) / SR
        whine.append(math.sin(ph) * math.sin(math.pi * min(1, u * 8)) * (1 - u) ** 1.2)
    add(x, whine, 0.01, 0.5)
    y = convolve(norm(x), ir_outdoor(rnd, 1.0, 0.7, 0.2, 0.003, 6, 0.2, 0.35))
    return trim_tail(norm(y, 0.8))
