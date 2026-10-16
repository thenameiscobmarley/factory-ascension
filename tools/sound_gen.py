#!/usr/bin/env python3
"""Synthesizes the mod's sounds (stdlib DSP, then ffmpeg to Ogg Vorbis): gunshots for each kind of gun, suppressed
shots, far-away gunfire that rolls off the hills, reload and action noises, brass on the ground, and the dread sounds
(heartbeat, breathing, ear ringing, a rescue helicopter).

Every sound is made from noise and damped sines shaped by envelopes and filters, with a seeded random so a build is
always the same. Mono, 44.1 kHz (Minecraft only places mono sounds in the world).

  python3 tools/sound_gen.py      writes mod/src/main/resources/assets/factoryascension/sounds/*.ogg and sounds.json
"""
import json
import math
import os
import sys
import random
import struct
import subprocess
import tempfile
import wave

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import sound_real as R  # noqa: E402

SR = 44100
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "mod", "src", "main", "resources", "assets", "factoryascension")
OUT = os.path.join(ASSETS, "sounds")


# ---- Building blocks ----
def silence(sec):
    return [0.0] * int(sec * SR)


def mix_into(dst, src, at=0.0, gain=1.0):
    o = int(at * SR)
    if o + len(src) > len(dst):
        dst.extend([0.0] * (o + len(src) - len(dst)))
    for i, v in enumerate(src):
        dst[o + i] += v * gain
    return dst


def noise(sec, rnd):
    return [rnd.uniform(-1, 1) for _ in range(int(sec * SR))]


def lowpass(x, fc):
    a = math.exp(-2 * math.pi * fc / SR)
    y, out = 0.0, []
    for v in x:
        y = (1 - a) * v + a * y
        out.append(y)
    return out


def highpass(x, fc):
    lp = lowpass(x, fc)
    return [a - b for a, b in zip(x, lp)]


def bandpass(x, f0, q=1.0):
    """A biquad band-pass (constant peak gain)."""
    w = 2 * math.pi * f0 / SR
    alpha = math.sin(w) / (2 * q)
    b0, b1, b2 = alpha, 0, -alpha
    a0, a1, a2 = 1 + alpha, -2 * math.cos(w), 1 - alpha
    b0, b1, b2, a1, a2 = b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0
    x1 = x2 = y1 = y2 = 0.0
    out = []
    for v in x:
        y = b0 * v + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2, x1, y2, y1 = x1, v, y1, y
        out.append(y)
    return out


def env(x, attack, decay, hold=0.0, curve=1.0):
    """Attack (linear), hold, then exponential decay with time constant `decay`."""
    out = []
    for i, v in enumerate(x):
        t = i / SR
        if t < attack:
            g = t / max(1e-6, attack)
        elif t < attack + hold:
            g = 1.0
        else:
            g = math.exp(-(t - attack - hold) / max(1e-6, decay)) ** curve
        out.append(v * g)
    return out


def sine_sweep(sec, f0, f1, decay, phase=0.0):
    out, ph = [], phase
    n = int(sec * SR)
    for i in range(n):
        t = i / SR
        f = f1 + (f0 - f1) * math.exp(-t / max(1e-6, sec * 0.25))
        ph += 2 * math.pi * f / SR
        out.append(math.sin(ph) * math.exp(-t / decay))
    return out


def ring(sec, freqs, rnd, strike=0.002):
    """A struck piece of metal: damped inharmonic sines, set off by a tiny noise tick."""
    n = int(sec * SR)
    out = [0.0] * n
    for (f, decay, amp) in freqs:
        ph = rnd.uniform(0, 6.28)
        for i in range(n):
            t = i / SR
            out[i] += math.sin(ph + 2 * math.pi * f * t) * math.exp(-t / decay) * amp
    tick = env(highpass(noise(strike * 4, rnd), 2000), 0.0002, strike)
    mix_into(out, tick, 0, 0.8)
    return out


def reverb(x, size=1.0, decay=0.5, wet=0.4, damp=3000):
    """A small Schroeder reverb: four combs into two all-passes."""
    out = x + [0.0] * int(SR * decay * 3)
    combs = [int(SR * d * size) for d in (0.0297, 0.0371, 0.0411, 0.0437)]
    wetbuf = [0.0] * len(out)
    for delay in combs:
        g = 10 ** (-3 * delay / SR / max(0.05, decay))
        buf = [0.0] * delay
        k = 0
        lp = 0.0
        a = math.exp(-2 * math.pi * damp / SR)
        for i in range(len(out)):
            y = buf[k]
            lp = (1 - a) * y + a * lp
            buf[k] = out[i] + lp * g
            k = (k + 1) % delay
            wetbuf[i] += y * 0.25
    for delay, g in ((int(SR * 0.005), 0.7), (int(SR * 0.0017), 0.7)):
        buf = [0.0] * delay
        k = 0
        for i in range(len(wetbuf)):
            bv = buf[k]
            y = -g * wetbuf[i] + bv
            buf[k] = wetbuf[i] + g * y
            k = (k + 1) % delay
            wetbuf[i] = y
    return [o * (1 - wet) + w * wet for o, w in zip(out, wetbuf)]


def echo(x, delays, rnd, cutoff=900):
    """Slap-back from hills and buildings: a few delayed, darker, quieter copies."""
    out = list(x)
    for d, g in delays:
        dark = lowpass(x, cutoff * rnd.uniform(0.8, 1.2))
        mix_into(out, dark, d, g)
    return out


def soft_clip(x, drive=1.5):
    return [math.tanh(v * drive) / math.tanh(drive) for v in x]


def normalize(x, peak=0.92):
    m = max(1e-9, max(abs(v) for v in x))
    return [v * peak / m for v in x]


def fade_tail(x, sec=0.05):
    n = min(len(x), int(sec * SR))
    for i in range(n):
        x[len(x) - n + i] *= 1 - i / n
    return x


def trim(x, thresh=0.0008):
    end = len(x)
    while end > 1 and abs(x[end - 1]) < thresh:
        end -= 1
    return x[:max(end, int(0.05 * SR))]


# ---- Gunshots ----
def gunshot(rnd, crack=1.0, blast_tau=0.05, body=2600, thump=(140, 45, 0.09), tail=1.2, echoes=True, mech=0.25, bright=6000, roomy=0.3):
    out = silence(0.02)
    if crack > 0:
        c = env(highpass(noise(0.01, rnd), 3500), 0.0001, 0.0018)
        mix_into(out, c, 0.0, crack)
    blast = env(lowpass(noise(0.6, rnd), body), 0.0008, blast_tau, curve=1.0)
    blast = [v * 0.8 + w * 0.2 for v, w in zip(blast, env(lowpass(highpass(noise(0.6, rnd), 900), bright), 0.0005, blast_tau * 0.5))]
    mix_into(out, blast, 0.0005, 1.2)
    f0, f1, decay = thump
    mix_into(out, sine_sweep(0.35, f0, f1, decay), 0.0, 0.9)
    if mech > 0:
        clack = ring(0.12, [(rnd.uniform(1800, 2600), 0.02, 0.5), (rnd.uniform(3800, 5200), 0.012, 0.35), (rnd.uniform(6500, 8200), 0.008, 0.2)], rnd)
        mix_into(out, clack, rnd.uniform(0.035, 0.06), mech)
    out = soft_clip(normalize(out), 1.8)
    if echoes:
        out = echo(out, [(rnd.uniform(0.11, 0.18), 0.32), (rnd.uniform(0.26, 0.4), 0.2), (rnd.uniform(0.5, 0.8), 0.1)], rnd, 1100)
    out = reverb(out, size=1.4, decay=tail, wet=roomy, damp=2400)
    return fade_tail(trim(normalize(out)))


def suppressed(rnd):
    out = silence(0.02)
    pop = env(lowpass(noise(0.3, rnd), 900), 0.001, 0.022)
    mix_into(out, pop, 0, 1.0)
    mix_into(out, sine_sweep(0.2, 110, 50, 0.04), 0, 0.6)
    clack = ring(0.12, [(rnd.uniform(1900, 2500), 0.025, 0.6), (rnd.uniform(4200, 5400), 0.015, 0.4), (rnd.uniform(7000, 8500), 0.008, 0.25)], rnd)
    mix_into(out, clack, 0.004, 0.9)
    mix_into(out, ring(0.08, [(rnd.uniform(2600, 3200), 0.02, 0.5)], rnd), rnd.uniform(0.05, 0.07), 0.5)
    out = reverb(normalize(out), size=0.8, decay=0.25, wet=0.18, damp=3000)
    return fade_tail(trim(normalize(out, 0.8)))


def distant(rnd, burst=1):
    """Gunfire far off: only the low boom survives, rolling and echoing."""
    out = silence(0.05)
    t = 0.0
    for k in range(burst):
        shot = env(lowpass(noise(0.8, rnd), rnd.uniform(380, 650)), 0.004, rnd.uniform(0.07, 0.12))
        mix_into(out, shot, t, rnd.uniform(0.7, 1.0))
        mix_into(out, sine_sweep(0.4, 70, 35, 0.12), t, 0.6)
        t += rnd.uniform(0.07, 0.11) if burst > 1 else 0
    out = echo(normalize(out), [(rnd.uniform(0.25, 0.45), 0.5), (rnd.uniform(0.6, 0.9), 0.35), (rnd.uniform(1.0, 1.5), 0.22), (rnd.uniform(1.6, 2.2), 0.12)], rnd, 420)
    out = reverb(out, size=2.2, decay=2.2, wet=0.5, damp=900)
    return fade_tail(trim(normalize(out, 0.85)), 0.3)


# ---- Mechanics ----
def click(rnd, freqs=(3000, 5200, 8000), level=1.0, decay=0.01):
    return ring(0.08, [(f * rnd.uniform(0.93, 1.07), decay * rnd.uniform(0.8, 1.3), a) for f, a in zip(freqs, (0.6, 0.4, 0.25))], rnd, 0.001)


def slide(rnd, sec, f=1800):
    s = env(bandpass(noise(sec, rnd), f * rnd.uniform(0.85, 1.15), 2.5), sec * 0.3, sec * 0.5)
    return s


def mag_out(rnd):
    out = silence(0.4)
    mix_into(out, click(rnd, (2200, 4100, 6900)), 0.0, 0.8)
    mix_into(out, slide(rnd, 0.18, 1200), 0.03, 0.7)
    mix_into(out, click(rnd, (900, 1700, 3300), decay=0.02), 0.2, 0.6)
    return fade_tail(normalize(out, 0.8))


def mag_in(rnd):
    out = silence(0.45)
    mix_into(out, slide(rnd, 0.12, 1500), 0.0, 0.6)
    mix_into(out, click(rnd, (1500, 3200, 5600), decay=0.018), 0.12, 1.0)
    mix_into(out, env(lowpass(noise(0.1, rnd), 400), 0.001, 0.02), 0.12, 0.8)
    return fade_tail(normalize(out, 0.85))


def rack(rnd):
    out = silence(0.45)
    mix_into(out, click(rnd, (2600, 4800, 7300)), 0.0, 0.8)
    mix_into(out, slide(rnd, 0.1, 2400), 0.01, 0.5)
    mix_into(out, click(rnd, (2000, 3900, 6400), decay=0.014), 0.14, 1.0)
    mix_into(out, slide(rnd, 0.08, 2600), 0.15, 0.4)
    return fade_tail(normalize(out, 0.85))


def bolt(rnd):
    out = silence(0.8)
    mix_into(out, click(rnd, (1600, 3300, 5900), decay=0.02), 0.0, 0.8)          # up
    mix_into(out, slide(rnd, 0.16, 1700), 0.06, 0.6)                              # back
    mix_into(out, click(rnd, (2400, 4500, 7000)), 0.22, 0.7)
    mix_into(out, slide(rnd, 0.14, 1900), 0.32, 0.5)                              # forward
    mix_into(out, click(rnd, (1800, 3600, 6100), decay=0.02), 0.47, 1.0)          # down, locked
    return fade_tail(normalize(out, 0.85))


def pump(rnd):
    out = silence(0.6)
    mix_into(out, slide(rnd, 0.14, 900), 0.0, 0.8)
    mix_into(out, click(rnd, (1100, 2400, 4300), decay=0.03), 0.13, 1.0)
    mix_into(out, slide(rnd, 0.12, 1000), 0.2, 0.7)
    mix_into(out, click(rnd, (1300, 2700, 4800), decay=0.03), 0.32, 1.0)
    return fade_tail(normalize(out, 0.9))


def shell_in(rnd):
    out = silence(0.3)
    mix_into(out, slide(rnd, 0.06, 1300), 0.0, 0.5)
    mix_into(out, click(rnd, (900, 2100, 3900), decay=0.02), 0.06, 0.9)
    return fade_tail(normalize(out, 0.75))


def dry(rnd):
    out = silence(0.15)
    mix_into(out, click(rnd, (3200, 6100, 9000), decay=0.006), 0, 1.0)
    return fade_tail(normalize(out, 0.7))


def casing(rnd):
    """Brass bouncing on hard ground: a few bright tinks, each quieter."""
    out = silence(0.6)
    t, g = 0.0, 1.0
    for k in range(rnd.randint(3, 5)):
        tink = ring(0.15, [(rnd.uniform(5200, 6800), 0.05, 0.6), (rnd.uniform(8500, 11000), 0.03, 0.4), (rnd.uniform(3200, 4000), 0.04, 0.3)], rnd)
        mix_into(out, tink, t, g)
        t += rnd.uniform(0.05, 0.14) * (1 - k * 0.15)
        g *= rnd.uniform(0.45, 0.65)
    return fade_tail(normalize(out, 0.55))


# ---- Dread ----
def heartbeat(rnd):
    out = silence(0.7)
    lub = env(sine_sweep(0.2, 75, 42, 0.05), 0.005, 0.06)
    dub = env(sine_sweep(0.2, 65, 38, 0.04), 0.005, 0.05)
    mix_into(out, lowpass(lub, 180), 0.0, 1.0)
    mix_into(out, lowpass(dub, 160), 0.24, 0.75)
    return fade_tail(normalize(out, 0.95))


def breath(rnd, rate=1.0):
    inhale = 0.7 / rate
    exhale = 0.9 / rate
    n = noise(inhale + exhale + 0.3, rnd)
    a = bandpass(n, rnd.uniform(700, 1000), 0.8)
    b = bandpass(n, rnd.uniform(1800, 2400), 1.2)
    x = [p * 0.7 + q * 0.3 for p, q in zip(a, b)]
    out = []
    for i, v in enumerate(x):
        t = i / SR
        if t < inhale:
            g = math.sin(math.pi * t / inhale) ** 1.5 * 0.7
        elif t < inhale + 0.08:
            g = 0
        elif t < inhale + 0.08 + exhale:
            u = (t - inhale - 0.08) / exhale
            g = math.sin(math.pi * u) ** 1.2
        else:
            g = 0
        out.append(v * g)
    return fade_tail(normalize(out, 0.7))


def ringing(rnd):
    sec = 5.0
    out = []
    f1, f2 = rnd.uniform(6200, 6800), rnd.uniform(3900, 4300)
    for i in range(int(sec * SR)):
        t = i / SR
        g = min(1, t / 0.08) * math.exp(-t / 1.8)
        out.append((math.sin(2 * math.pi * f1 * t) * 0.7 + math.sin(2 * math.pi * f2 * t) * 0.15 * (1 + math.sin(t * 3))) * g)
    return fade_tail(normalize(out, 0.5), 0.5)


def helicopter(rnd):
    """Rotor blades beating the air, a turbine whine under them; loops."""
    sec = 4.0
    rate = 4.6
    out = [0.0] * int(sec * SR)
    period = 1 / rate
    t = 0.0
    while t < sec:
        slap = env(lowpass(noise(0.25, rnd), 240), 0.004, 0.06)
        mix_into(out, slap, t, 1.0)
        mix_into(out, sine_sweep(0.2, 60, 40, 0.06), t, 0.7)
        t += period
    whine = []
    for i in range(int(sec * SR)):
        tt = i / SR
        whine.append(math.sin(2 * math.pi * 1650 * tt + 0.3 * math.sin(tt * 9)) * 0.05 + math.sin(2 * math.pi * 3300 * tt) * 0.015)
    mix_into(out, whine, 0, 1.0)
    out = out[:int(sec * SR)]
    return normalize(out, 0.9)


def grenade_explosion(rnd):
    out = silence(0.02)
    blast = env(lowpass(noise(1.2, rnd), 1800), 0.002, 0.18)
    mix_into(out, blast, 0, 1.0)
    mix_into(out, sine_sweep(0.8, 90, 30, 0.3), 0, 1.0)
    debris = env(highpass(noise(1.0, rnd), 2500), 0.05, 0.25)
    mix_into(out, debris, 0.08, 0.15)
    out = soft_clip(normalize(out), 2.2)
    out = echo(out, [(0.2, 0.4), (0.45, 0.25), (0.9, 0.15)], rnd, 600)
    out = reverb(out, size=2.0, decay=1.8, wet=0.4, damp=1500)
    return fade_tail(trim(normalize(out)), 0.3)


# ---- Writing ----
def bullet_crack(rnd):
    """A supersonic round going past: the N-wave snap of its shock wave, then a short ringing hiss."""
    x = silence(0.25)
    n = int(SR * 0.0012)
    for i in range(n):
        x[i] = 1.0 - 2.0 * i / n                          # the N-wave: up at once, straight down through zero
    tail = env(highpass(noise(0.08, rnd), 2500), 0.0005, 0.018)
    mix_into(x, tail, 0.001, 0.45)
    x = reverb(x, size=0.6, decay=0.35, wet=0.25, damp=5000)
    return normalize(fade_tail(trim(x)), 0.95)


def bullet_whiz(rnd):
    """A slower round going past: a buzzing whine rising then falling as it passes (Doppler)."""
    sec = 0.32
    n = int(SR * sec)
    x = silence(sec)
    base = 900 + rnd.random() * 500
    ph = 0.0
    nz = noise(sec, rnd)
    for i in range(n):
        t = i / n
        f = base * (1.25 - 0.5 * t)                         # coming, then going
        ph += 2 * math.pi * f / SR
        a = math.exp(-((t - 0.45) / 0.18) ** 2)
        x[i] = (math.sin(ph) * 0.6 + math.sin(ph * 2.01) * 0.2 + nz[i] * 0.35) * a
    return normalize(fade_tail(bandpass(x, base, 0.9)), 0.8)


def engine_loop(rnd, cyl=1):
    """A running engine, one second that loops seamlessly (a whole number of firing cycles): a single's thumping
    pulses with a rasp of exhaust, or (cyl=12) a V12's smooth, dense howl. The game pitches it with the revs."""
    sec = 1.0
    n = int(SR * sec)
    x = silence(sec)
    fire = 50 if cyl == 1 else 120                       # firing pulses a second at idle-ish revs
    crank = 25 if cyl == 1 else 20                       # what repeats (one crank turn / one firing order)
    per = SR / fire
    for k in range(fire):
        start = int(k * per)
        # each pulse a little different, repeating with the crank so the loop stays seamless
        var = 1.0 + 0.25 * math.sin(2 * math.pi * (k % (fire // crank)) / max(1, fire // crank))
        tau = 0.006 if cyl == 1 else 0.0025
        f1 = 95 if cyl == 1 else 240
        for i in range(int(per * 1.6)):
            j = (start + i) % n
            t = i / SR
            e = math.exp(-t / tau)
            x[j] += var * e * (math.sin(2 * math.pi * f1 * t) + 0.5 * math.sin(2 * math.pi * f1 * 2.7 * t))
    # Exhaust rasp: noise shaped by the same pulses
    nz = noise(sec, rnd)
    env_pulse = [0.0] * n
    for k in range(fire):
        start = int(k * per)
        for i in range(int(per)):
            env_pulse[(start + i) % n] = math.exp(-i / SR / 0.004)
    rasp = [nz[i] * env_pulse[i] * (0.35 if cyl == 1 else 0.15) for i in range(n)]
    rasp = bandpass(rasp, 1800 if cyl == 1 else 2600, 0.8)
    x = [x[i] + rasp[i] for i in range(n)]
    x = lowpass(x, 3200 if cyl == 1 else 4200)
    # Remove any DC so the loop point doesn't click
    m = sum(x) / n
    x = [v - m for v in x]
    return normalize(x, 0.85)


def write_ogg(name, x):
    os.makedirs(os.path.dirname(os.path.join(OUT, name)), exist_ok=True)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tf:
        path = tf.name
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, v)) * 32000)) for v in x))
    subprocess.run(["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-i", path, "-c:a", "libvorbis", "-q:a", "3", os.path.join(OUT, name + ".ogg")], check=True)
    os.unlink(path)


SOUNDS = {
    # id: (variants, maker(rnd, k), subtitle)
    # Guns: tools/sound_real.py (blast waves, gas, action, a clipping mic, the world's reflections)
    "gun.pistol": (3, lambda r, k: R.gunshot(r, cal=0.6, supersonic=False, auto=True, rumble=0.8, room="close"), "Pistol shot"),
    "gun.magnum": (2, lambda r, k: R.gunshot(r, cal=0.95, supersonic=False, auto=False, rumble=1.2), "Magnum shot"),
    "gun.smg": (3, lambda r, k: R.gunshot(r, cal=0.55, supersonic=False, auto=True, rumble=0.7, room="close", bright=1.1), "SMG shot"),
    "gun.rifle": (3, lambda r, k: R.gunshot(r, cal=1.0, supersonic=True, auto=True), "Rifle shot"),
    "gun.battle": (2, lambda r, k: R.gunshot(r, cal=1.25, supersonic=True, auto=True, rumble=1.15), "Battle rifle shot"),
    "gun.sniper": (2, lambda r, k: R.gunshot(r, cal=1.4, supersonic=True, auto=False, rumble=1.25, room="valley"), "Sniper shot"),
    "gun.fifty": (2, lambda r, k: R.gunshot(r, cal=1.8, supersonic=True, auto=False, rumble=1.5, room="valley", bright=0.85), ".50 cal shot"),
    "gun.shotgun": (2, lambda r, k: R.shotgun_blast(r), "Shotgun blast"),
    "gun.lmg": (2, lambda r, k: R.gunshot(r, cal=1.1, supersonic=True, auto=True, rumble=1.1), "Machine gun fire"),
    "gun.suppressed": (3, lambda r, k: R.suppressed_shot(r), "Suppressed shot"),
    "gun.distant": (4, lambda r, k: R.distant_fire(r, 1), "Distant gunfire"),
    "gun.distant_burst": (3, lambda r, k: R.distant_fire(r, r.randint(3, 6)), "Distant gunfire"),
    "gun.dry": (2, lambda r, k: R.dry_fire(r), "Dry fire"),
    "gun.mag_out": (2, lambda r, k: R.mag_out(r), "Magazine out"),
    "gun.mag_in": (2, lambda r, k: R.mag_in(r), "Magazine in"),
    "gun.rack": (2, lambda r, k: R.rack(r), "Gun racked"),
    "gun.bolt": (2, lambda r, k: R.bolt(r), "Bolt worked"),
    "gun.pump": (2, lambda r, k: R.pump(r), "Shotgun pumped"),
    "gun.shell_in": (3, lambda r, k: R.shell_in(r), "Round loaded"),
    "gun.casing": (4, lambda r, k: R.brass(r), "Brass falls"),
    "dread.heartbeat": (1, lambda r, k: R.heartbeat(r), "Heartbeat"),
    "dread.breath": (3, lambda r, k: breath(r, 1.0 + k * 0.35), "Heavy breathing"),
    "dread.ringing": (1, lambda r, k: R.ringing(r), "Ears ringing"),
    "dread.helicopter": (1, lambda r, k: helicopter(r), "Helicopter"),
    "grenade.explode": (2, lambda r, k: R.explosion(r), "Grenade explodes"),
    "gun.crack": (3, lambda r, k: bullet_crack(r), "Round cracks past"),
    "gun.whiz": (3, lambda r, k: bullet_whiz(r), "Round whizzes past"),
    "vehicle.engine": (1, lambda r, k: engine_loop(r, 1), "Dirt bike engine"),
    "vehicle.engine_v12": (1, lambda r, k: engine_loop(r, 12), "V12 engine"),
}


def main():
    os.makedirs(OUT, exist_ok=True)
    sounds_json = {}
    only = set(sys.argv[1:])     # sound ids to (re)make; none = all of them (the .ogg files are kept in the repo)
    for sid, (variants, maker, subtitle) in SOUNDS.items():
        files = []
        for k in range(variants):
            name = sid.replace(".", "/") + ("_%d" % (k + 1) if variants > 1 else "")
            if not only or sid in only:
                rnd = random.Random(hash_str(sid) * 31 + k)
                write_ogg(name, maker(rnd, k))
            files.append({"name": "factoryascension:" + name, "stream": False} if sid not in ("dread.helicopter",) else {"name": "factoryascension:" + name})
        sounds_json[sid] = {"sounds": files, "subtitle": "subtitles.factoryascension." + sid}
        print("%-22s %d variant(s)" % (sid, variants))
    with open(os.path.join(ASSETS, "sounds.json"), "w") as f:
        json.dump(sounds_json, f, indent=1)
    lang_path = os.path.join(ASSETS, "lang", "en_us.json")
    lang = json.load(open(lang_path))
    for sid, (_, _, subtitle) in SOUNDS.items():
        lang["subtitles.factoryascension." + sid] = subtitle
    with open(lang_path, "w") as f:
        json.dump(lang, f, indent=1)
        f.write("\n")


def hash_str(s):
    h = 7
    for ch in s:
        h = (h * 131 + ord(ch)) & 0xFFFFFF
    return h


if __name__ == "__main__":
    main()
