#!/usr/bin/env python3
"""
Synthesizes every sound effect in android/assets/ from scratch (no samples, no copyrighted material)
and encodes it as .ogg (needs ffmpeg with libvorbis; falls back to .wav when ffmpeg is missing).

Usage: python3 tools/generate_sounds.py
Requires: numpy, scipy
"""
import os
import shutil
import subprocess
import wave

import numpy as np
from scipy import signal

SR = 44100
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "android", "assets")
os.makedirs(OUT, exist_ok=True)
rng = np.random.default_rng(2024)


# ------------------------------------------------------------------ building blocks
def t_axis(dur):
    return np.arange(int(SR * dur)) / SR


def noise(dur):
    return rng.standard_normal(int(SR * dur)).astype(np.float64)


def bandpass(x, lo, hi, order=2):
    sos = signal.butter(order, [lo, hi], btype="band", fs=SR, output="sos")
    return signal.sosfilt(sos, x)


def lowpass(x, fc, order=2):
    return signal.sosfilt(signal.butter(order, fc, btype="low", fs=SR, output="sos"), x)


def highpass(x, fc, order=2):
    return signal.sosfilt(signal.butter(order, fc, btype="high", fs=SR, output="sos"), x)


def decay_env(n, tau, attack=0.002):
    t = np.arange(n) / SR
    e = np.exp(-t / tau)
    a = int(attack * SR)
    if a > 0:
        e[:a] *= np.linspace(0, 1, a)
    return e


def sine_sweep(f0, f1, dur, curve=1.0):
    """Sine whose frequency glides from f0 to f1 (curve > 1 = fast at first)."""
    n = int(SR * dur)
    u = (np.arange(n) / max(n - 1, 1)) ** (1.0 / curve)
    f = f0 + (f1 - f0) * u
    return np.sin(2 * np.pi * np.cumsum(f) / SR)


def modal(freqs, taus, amps, dur):
    t = t_axis(dur)
    out = np.zeros_like(t)
    for f, tau, a in zip(freqs, taus, amps):
        out += a * np.sin(2 * np.pi * f * t + rng.uniform(0, 6.28)) * np.exp(-t / tau)
    return out


def place(buf, sound, at):
    i = int(at * SR)
    end = min(len(buf), i + len(sound))
    if i < len(buf):
        buf[i:end] += sound[: end - i]


def reverb(x, tail=0.35, wet=0.25):
    n = int(SR * tail)
    ir = rng.standard_normal(n) * np.exp(-np.arange(n) / (SR * tail / 4.0))
    ir[0] = 0
    wetsig = signal.fftconvolve(x, ir)[: len(x)]
    wetsig *= np.max(np.abs(x)) / (np.max(np.abs(wetsig)) + 1e-9)
    return x + wet * wetsig


def finish(x, peak=0.85, fade=0.012):
    x = x - np.mean(x)
    x = x / (np.max(np.abs(x)) + 1e-9) * peak
    f = int(fade * SR)
    if f > 0:
        x[-f:] *= np.linspace(1, 0, f)
        x[:8] *= np.linspace(0, 1, 8)
    return x


def tone_adsr(freq, dur, harmonics=((1, 1.0), (2, 0.35), (3, 0.18)), a=0.01, tau=0.25, vib=0.0, vib_rate=5.5):
    t = t_axis(dur)
    f = freq * (1 + vib * np.sin(2 * np.pi * vib_rate * t))
    phase = 2 * np.pi * np.cumsum(f) / SR
    x = sum(amp * np.sin(h * phase) for h, amp in harmonics)
    return x * decay_env(len(t), tau, a)


# ------------------------------------------------------------------ sounds
def launch():
    dur = 0.5
    n = int(SR * dur)
    sweep = np.linspace(500, 4200, n)
    wh = noise(dur)
    # time-varying band pass (cheap: crossfade between a few fixed filters)
    bands = [(300, 700), (700, 1500), (1500, 3000), (3000, 6000)]
    out = np.zeros(n)
    for i, (lo, hi) in enumerate(bands):
        centre = i / (len(bands) - 1)
        w = np.exp(-(((np.arange(n) / n) - (0.15 + 0.6 * centre)) / 0.18) ** 2)
        out += bandpass(wh, lo, hi) * w
    out *= decay_env(n, 0.22, 0.01)
    twang = sine_sweep(560, 170, 0.35, 2.5) * np.exp(-t_axis(0.35) / 0.12)
    twang += 0.5 * sine_sweep(1120, 340, 0.35, 2.5) * np.exp(-t_axis(0.35) / 0.07)
    wob = 1 + 0.15 * np.sin(2 * np.pi * 34 * t_axis(0.35))
    buf = out * 0.9
    place(buf, twang * wob * 0.9, 0.0)
    return finish(buf)


def stretch():
    dur = 0.22
    n = int(SR * dur)
    t = t_axis(dur)
    f = 180 + 160 * (t / dur) ** 0.8
    saw = signal.sawtooth(2 * np.pi * np.cumsum(f) / SR)
    x = lowpass(saw, 900) * (0.6 + 0.4 * np.sin(2 * np.pi * 46 * t))
    x += 0.35 * lowpass(noise(dur), 1800) * np.sin(np.pi * t / dur)
    x *= np.sin(np.pi * np.clip(t / dur, 0, 1)) ** 0.6
    return finish(x, 0.55)


def hit_wood():
    dur = 0.22
    click = highpass(noise(0.02), 1500) * decay_env(int(SR * 0.02), 0.004)
    body = modal([170, 310, 520, 880], [0.045, 0.035, 0.025, 0.018], [1.0, 0.7, 0.45, 0.25], dur)
    thump = lowpass(noise(dur), 700) * decay_env(int(SR * dur), 0.03)
    buf = body + 0.8 * thump
    place(buf, click * 0.9, 0.0)
    return finish(buf)


def hit_stone():
    dur = 0.3
    click = highpass(noise(0.015), 2500) * decay_env(int(SR * 0.015), 0.003)
    body = modal([120, 245, 410, 690, 1250], [0.12, 0.09, 0.06, 0.04, 0.025], [1.0, 0.6, 0.5, 0.35, 0.2], dur)
    grit = bandpass(noise(dur), 400, 2200) * decay_env(int(SR * dur), 0.035)
    buf = body + 0.6 * grit
    place(buf, click, 0.0)
    return finish(buf)


def hit_glass():
    dur = 0.35
    freqs = [2150, 3020, 4180, 5330, 6900]
    body = modal(freqs, [0.11, 0.09, 0.07, 0.05, 0.04], [1.0, 0.8, 0.6, 0.45, 0.3], dur)
    tick = highpass(noise(0.01), 4000) * decay_env(int(SR * 0.01), 0.002)
    buf = body
    place(buf, tick * 1.2, 0.0)
    return finish(reverb(buf, 0.2, 0.15))


def hit_pig():
    dur = 0.32
    t = t_axis(dur)
    f0 = 340 + 260 * np.sin(np.pi * np.clip(t / dur, 0, 1) ** 0.7)
    mod = np.sin(2 * np.pi * np.cumsum(f0 * 1.5) / SR) * 2.2
    car = np.sin(2 * np.pi * np.cumsum(f0) / SR + mod)
    x = bandpass(car, 500, 2600) + 0.3 * car
    x *= decay_env(len(t), 0.16, 0.008)
    x *= 1 + 0.25 * np.sin(2 * np.pi * 22 * t)
    snort = bandpass(noise(0.08), 500, 2500) * decay_env(int(SR * 0.08), 0.03)
    place(x, snort * 0.6, 0.0)
    return finish(x, 0.8)


def thud():
    dur = 0.2
    x = sine_sweep(150, 55, dur, 3.0) * decay_env(int(SR * dur), 0.07)
    x += 0.7 * lowpass(noise(dur), 500) * decay_env(int(SR * dur), 0.03)
    return finish(x)


def break_wood():
    dur = 0.55
    buf = np.zeros(int(SR * dur))
    for k in range(7):
        at = rng.uniform(0, 0.3) if k else 0.0
        sc = rng.uniform(0.6, 1.0)
        crack = bandpass(noise(0.06), rng.uniform(800, 1600), rng.uniform(2400, 4200)) * decay_env(int(SR * 0.06), 0.012)
        knock = modal([rng.uniform(150, 260), rng.uniform(380, 620)], [0.05, 0.04], [1.0, 0.6], 0.15)
        place(buf, crack * 1.3 * sc, at)
        place(buf, knock * sc, at)
    place(buf, lowpass(noise(0.35), 600) * decay_env(int(SR * 0.35), 0.08) * 0.8, 0.0)
    return finish(buf)


def break_stone():
    dur = 0.7
    buf = np.zeros(int(SR * dur))
    place(buf, lowpass(noise(0.6), 500, 3) * decay_env(int(SR * 0.6), 0.16) * 1.4, 0.0)
    place(buf, sine_sweep(110, 50, 0.4, 2.5) * decay_env(int(SR * 0.4), 0.12), 0.0)
    for k in range(9):
        at = rng.uniform(0.02, 0.45)
        f = rng.uniform(250, 900)
        gain = rng.uniform(0.3, 0.7)
        place(buf, modal([f, f * 1.7], [0.03, 0.02], [1.0, 0.6], 0.1) * gain, at)
        place(buf, highpass(noise(0.02), 2000) * decay_env(int(SR * 0.02), 0.004) * gain, at)
    return finish(buf)


def break_glass():
    dur = 0.9
    buf = np.zeros(int(SR * dur))
    place(buf, highpass(noise(0.35), 3000) * decay_env(int(SR * 0.35), 0.07) * 0.9, 0.0)
    for k in range(34):
        at = rng.exponential(0.12)
        if at > 0.7:
            continue
        f = rng.uniform(1800, 9500)
        ping = modal([f, f * 1.41], [rng.uniform(0.03, 0.14), 0.05], [1.0, 0.4], 0.35)
        place(buf, ping * rng.uniform(0.15, 0.55), at)
    return finish(reverb(buf, 0.3, 0.2))


def pig_die():
    dur = 0.7
    t = t_axis(dur)
    pop = (lowpass(noise(0.05), 2500) * decay_env(int(SR * 0.05), 0.012))
    f = 720 * np.exp(-t * 2.2) + 120
    mod = np.sin(2 * np.pi * np.cumsum(f * 2.0) / SR) * 3.0
    squeal = np.sin(2 * np.pi * np.cumsum(f) / SR + mod)
    squeal = bandpass(squeal, 350, 3000) * decay_env(len(t), 0.2, 0.01)
    squeal *= 1 + 0.3 * np.sin(2 * np.pi * 14 * t)
    puff = bandpass(noise(0.3), 300, 3500) * decay_env(int(SR * 0.3), 0.08)
    buf = squeal * 0.9
    place(buf, pop * 1.6, 0.0)
    place(buf, puff * 0.7, 0.02)
    return finish(buf)


def win():
    notes = [(523.25, 0.00), (659.25, 0.16), (783.99, 0.32), (1046.5, 0.50)]
    dur = 2.1
    buf = np.zeros(int(SR * dur))
    for f, at in notes:
        place(buf, tone_adsr(f, 0.55, ((1, 1.0), (2, 0.4), (3, 0.2), (4, 0.1)), tau=0.22) * 0.8, at)
    for f in (523.25, 659.25, 783.99, 1046.5, 1318.5):
        place(buf, tone_adsr(f, 1.5, ((1, 1.0), (2, 0.3), (3, 0.15)), a=0.015, tau=0.55) * 0.45, 0.75)
    for f, at in ((2093.0, 0.8), (2637.0, 0.95), (3136.0, 1.1)):
        place(buf, tone_adsr(f, 0.6, ((1, 1.0),), a=0.003, tau=0.18) * 0.18, at)
    return finish(reverb(buf, 0.45, 0.22))


def lose():
    notes = [(196.0, 0.0, 0.42), (185.0, 0.45, 0.42), (174.6, 0.90, 0.42), (164.8, 1.35, 1.0)]
    buf = np.zeros(int(SR * 2.5))
    for f, at, d in notes:
        t = t_axis(d)
        vib = 0.0 if d < 0.5 else 0.018
        fr = f * (1 + vib * np.sin(2 * np.pi * 5.5 * t))
        if d < 0.5:
            fr = fr * (1 - 0.05 * (t / d))
        saw = signal.sawtooth(2 * np.pi * np.cumsum(fr) / SR)
        # "wah": a low-pass that opens and closes
        wah = lowpass(saw, 1100)
        gate = 0.5 + 0.5 * np.sin(np.pi * np.clip(t / d, 0, 1)) ** 0.7
        env = np.minimum(1, t / 0.03) * np.exp(-np.maximum(t - 0.2, 0) / (0.25 if d < 0.5 else 0.6))
        place(buf, wah * gate * env, at)
    return finish(reverb(buf, 0.3, 0.12), 0.75)


def star():
    dur = 0.8
    x = tone_adsr(1318.5, dur, ((1, 1.0), (2, 0.35), (3.01, 0.2), (5.4, 0.12)), a=0.004, tau=0.28)
    sparkle = tone_adsr(2637.0, 0.4, ((1, 1.0),), a=0.002, tau=0.07) * 0.4
    place(x, sparkle, 0.03)
    return finish(reverb(x, 0.3, 0.2))


def click():
    dur = 0.07
    t = t_axis(dur)
    x = np.sin(2 * np.pi * 1700 * t) * decay_env(len(t), 0.012, 0.0005)
    x += 0.4 * np.sin(2 * np.pi * 850 * t) * decay_env(len(t), 0.02, 0.0005)
    return finish(x, 0.6)


def hop():
    dur = 0.2
    x = sine_sweep(260, 640, dur, 1.6) * decay_env(int(SR * dur), 0.08, 0.004)
    x += 0.35 * sine_sweep(520, 1280, dur, 1.6) * decay_env(int(SR * dur), 0.05, 0.004)
    return finish(x, 0.6)


# ---- bird voices: cartoon "voices" built from glides, vibrato and vowel-like formants
def vowel(x, f1, f2, q=0.8):
    return bandpass(x, f1 * (1 - 0.18), f1 * (1 + 0.18)) + q * bandpass(x, f2 * (1 - 0.15), f2 * (1 + 0.15))


def glide_voice(freqs, dur, rich=True, vib=0.0, vib_rate=8.0, tau=None):
    """freqs: list of (time_fraction, hz) breakpoints."""
    t = t_axis(dur)
    f = np.interp(t / dur, [p for p, _ in freqs], [h for _, h in freqs])
    f = f * (1 + vib * np.sin(2 * np.pi * vib_rate * t))
    ph = 2 * np.pi * np.cumsum(f) / SR
    x = signal.sawtooth(ph) if rich else np.sin(ph)
    env = np.minimum(1, t / 0.015) * np.minimum(1, (dur - t) / 0.05)
    if tau:
        env = env * np.exp(-t / tau)
    return x, env


def bird_red_launch():
    x, env = glide_voice([(0, 380), (0.35, 640), (1, 470)], 0.46, vib=0.02, vib_rate=7)
    x = vowel(x, 850, 1350) * env
    return finish(reverb(x, 0.15, 0.1), 0.85)


def bird_red_idle():
    buf = np.zeros(int(SR * 0.7))
    for at, hz in ((0.0, 620), (0.26, 700)):
        x, env = glide_voice([(0, hz * 0.9), (0.5, hz * 1.25), (1, hz)], 0.2, vib=0.015)
        place(buf, vowel(x, 800, 1300) * env, at)
    return finish(buf, 0.6)


def bird_yellow_launch():
    x, env = glide_voice([(0, 760), (0.7, 1900), (1, 1650)], 0.36, vib=0.03, vib_rate=11)
    x = vowel(x, 1700, 2600, 0.6) * env
    return finish(reverb(x, 0.15, 0.1), 0.85)


def bird_yellow_idle():
    buf = np.zeros(int(SR * 0.6))
    for k in range(3):
        x, env = glide_voice([(0, 1300), (1, 1750)], 0.07, rich=False)
        place(buf, x * env * (1 - 0.15 * k), 0.02 + k * 0.13)
    return finish(buf, 0.55)


def bird_blue_launch():
    x, env = glide_voice([(0, 1500), (0.4, 2400), (1, 2000)], 0.2, rich=False, vib=0.02, vib_rate=14)
    x = (x + 0.35 * np.sin(2 * np.pi * np.cumsum(np.interp(t_axis(0.2) / 0.2, [0, 0.4, 1], [3000, 4800, 4000])) / SR)) * env
    return finish(x, 0.8)


def bird_blue_idle():
    buf = np.zeros(int(SR * 0.8))
    for at in (0.0, 0.3):
        x, env = glide_voice([(0, 1500), (0.3, 2100), (0.6, 1700), (1, 2300)], 0.22, rich=False, vib=0.04, vib_rate=18)
        place(buf, x * env, at)
    return finish(buf, 0.55)


def yellow_dash():
    dur = 0.32
    t = t_axis(dur)
    sweep = bandpass(noise(dur), 900, 6500) * np.minimum(1, t / 0.01) * np.exp(-t / 0.14)
    tone = np.sin(2 * np.pi * np.cumsum(np.interp(t / dur, [0, 1], [500, 2600])) / SR) * np.exp(-t / 0.12)
    return finish(sweep * 0.8 + tone * 0.6, 0.85)


def blue_split():
    buf = np.zeros(int(SR * 0.4))
    for k, at in enumerate((0.0, 0.07, 0.14)):
        x, env = glide_voice([(0, 500 + 140 * k), (1, 1200 + 200 * k)], 0.09, rich=False, tau=0.03)
        place(buf, x * env, at)
    return finish(reverb(buf, 0.15, 0.12), 0.85)


def explosion():
    dur = 1.1
    n = int(SR * dur)
    t = t_axis(dur)
    boom = lowpass(noise(dur), 420, 3) * np.exp(-t / 0.28) * 1.6
    sub = np.sin(2 * np.pi * np.cumsum(np.interp(t / dur, [0, 0.3, 1], [110, 52, 36])) / SR) * np.exp(-t / 0.22)
    crack = highpass(noise(0.12), 1500) * decay_env(int(SR * 0.12), 0.03)
    debris = bandpass(noise(dur), 500, 4200) * np.exp(-t / 0.35) * 0.35
    buf = boom + sub * 1.1 + debris
    place(buf, crack * 1.2, 0.0)
    return finish(reverb(buf, 0.45, 0.2), 0.9)


def locked():
    buf = np.zeros(int(SR * 0.3))
    for at, hz in ((0.0, 130), (0.12, 98)):
        t = t_axis(0.12)
        x = lowpass(signal.square(2 * np.pi * hz * t), 700) * np.exp(-t / 0.08)
        place(buf, x, at)
    return finish(buf, 0.6)


# ------------------------------------------------------------------ music (original compositions)
NOTE_INDEX = {"C": 0, "D": 2, "E": 4, "F": 5, "G": 7, "A": 9, "B": 11}


def midi_hz(n):
    return 440.0 * 2 ** ((n - 69) / 12.0)


def note_number(tok):
    return 12 * (int(tok[-1]) + 1) + NOTE_INDEX[tok[0]] + (1 if "#" in tok else 0)


def marimba(f, dur, tau=0.22):
    t = t_axis(dur)
    x = np.sin(2 * np.pi * f * t) + 0.4 * np.sin(2 * np.pi * 3.97 * f * t) * np.exp(-t / (tau * 0.25)) + 0.12 * np.sin(2 * np.pi * 2 * f * t)
    return x * np.exp(-t / tau) * np.minimum(1, t / 0.004)


def string_pad(f, dur):
    t = t_axis(dur)
    x = np.zeros_like(t)
    for det in (-0.004, 0.0, 0.004):
        x += signal.sawtooth(2 * np.pi * f * (1 + det) * t + rng.uniform(0, 6.28))
    x = lowpass(x, 1400)
    env = np.minimum(1, t / 0.35) * np.minimum(1, np.maximum(0, (dur - t) / 0.4))
    return x * env


def bass_note(f, dur):
    t = t_axis(dur)
    x = np.sin(2 * np.pi * f * t) + 0.3 * np.sin(2 * np.pi * 2 * f * t)
    return x * np.exp(-t / 0.5) * np.minimum(1, t / 0.01)


def loop_fold(buf, n):
    """Folds anything past n (reverb tails) onto the start, so the loop is seamless."""
    out = buf[:n].copy()
    tail = buf[n:]
    out[: len(tail)] += tail[: n]
    return out


def music_menu():
    bpm = 104
    beat = 60.0 / bpm
    eighth = beat / 2
    bars = 16
    n = int(SR * beat * 4 * bars)
    buf = np.zeros(n + int(SR * 2.0))
    chords = {
        "C": (36, (60, 64, 67)), "G": (43, (59, 62, 67)), "Am": (45, (57, 60, 64)), "F": (41, (57, 60, 65)),
    }
    prog = ["C", "G", "Am", "F", "C", "G", "F", "G", "Am", "F", "C", "G", "F", "G", "C", "C"]
    melody = [
        "E5 - G5 - A5 G5 E5 -", "D5 - G5 - A5 - G5 -", "C5 - E5 - G5 E5 D5 C5", "D5 - E5 - C5 - A4 -",
        "E5 - G5 - A5 G5 E5 G5", "A5 - G5 - E5 - D5 -", "C5 - D5 - E5 - G5 E5", "D5 - . - D5 E5 D5 -",
        "A5 - C6 - A5 G5 E5 -", "A5 - G5 - E5 - C5 -", "G5 - E5 - G5 - C6 -", "A5 - G5 - D5 - G5 -",
        "C6 - A5 - G5 - E5 D5", "D5 - G5 - A5 - G5 -", "E5 - G5 - C6 - G5 E5", "C5 - - - . . . .",
    ]
    arp_pattern = [0, 2, 1, 2, 0, 2, 1, 2]
    for bar in range(bars):
        t0 = bar * beat * 4
        root, triad = chords[prog[bar]]
        place(buf, bass_note(midi_hz(root), beat * 2.0) * 0.55, t0)
        place(buf, bass_note(midi_hz(root + (7 if bar % 2 else 0)), beat * 2.0) * 0.4, t0 + beat * 2)
        for m in triad:
            place(buf, string_pad(midi_hz(m), beat * 4.1) * 0.10, t0)
        # soft arpeggio
        for k in range(8):
            m = triad[arp_pattern[k]] + 12
            place(buf, marimba(midi_hz(m), 0.3, 0.12) * 0.16, t0 + k * eighth)
        # melody
        toks = melody[bar].split()
        k = 0
        while k < len(toks):
            tok = toks[k]
            if tok in ("-", "."):
                k += 1
                continue
            length = 1
            while k + length < len(toks) and toks[k + length] == "-":
                length += 1
            place(buf, marimba(midi_hz(note_number(tok)), 0.2 + length * eighth, 0.3) * 0.8, t0 + k * eighth)
            k += length
        # percussion: soft kick on 1 and 3, shaker on the eighths
        for kick in (0, 2):
            kt = t_axis(0.18)
            place(buf, np.sin(2 * np.pi * np.cumsum(np.interp(kt / 0.18, [0, 1], [130, 48])) / SR) * np.exp(-kt / 0.07) * 0.45, t0 + kick * beat)
        for k in range(8):
            sh = highpass(noise(0.05), 6000) * decay_env(int(SR * 0.05), 0.012) * (0.10 if k % 2 == 0 else 0.06)
            place(buf, sh, t0 + k * eighth)
    buf = reverb(buf, 0.5, 0.2)
    out = loop_fold(buf, n)
    return finish(out, 0.8, 0.0)


def music_game():
    """Calm outdoor ambience for the game: wind, birdsong, a soft pad."""
    dur = 32.0
    n = int(SR * dur)
    fade = int(SR * 2.0)
    total = n + fade
    t = np.arange(total) / SR
    wind = bandpass(rng.standard_normal(total), 180, 900)
    wind *= 0.5 + 0.5 * np.sin(2 * np.pi * 0.11 * t + 1.0) ** 2
    wind += 0.5 * bandpass(rng.standard_normal(total), 900, 2400) * (0.3 + 0.7 * np.sin(2 * np.pi * 0.07 * t) ** 2)
    buf = wind * 0.55
    # soft pad drone with slow movement
    for hz, g in ((130.81, 0.05), (196.0, 0.04), (329.63, 0.03), (261.63, 0.03)):
        buf += g * np.sin(2 * np.pi * hz * t + 0.6 * np.sin(2 * np.pi * 0.05 * t)) * (0.7 + 0.3 * np.sin(2 * np.pi * 0.08 * t + hz))
    # birdsong
    for _ in range(26):
        at = rng.uniform(0.3, dur - 1.0)
        kind = rng.integers(0, 3)
        base = rng.uniform(2200, 3600)
        if kind == 0:
            x, env = glide_voice([(0, base), (0.5, base * 1.3), (1, base * 0.95)], 0.12, rich=False, vib=0.03, vib_rate=20)
            place(buf, x * env * 0.12, at)
            place(buf, x * env * 0.10, at + 0.16)
        elif kind == 1:
            for k in range(4):
                x, env = glide_voice([(0, base * 0.8), (1, base * 1.1)], 0.05, rich=False)
                place(buf, x * env * 0.10, at + k * 0.08)
        else:
            x, env = glide_voice([(0, base * 0.7), (0.3, base * 1.2), (0.6, base * 0.9), (1, base * 1.4)], 0.35, rich=False, vib=0.04, vib_rate=16)
            place(buf, x * env * 0.10, at)
    # seamless loop: crossfade the tail into the start
    ramp = np.linspace(0, 1, fade)
    out = buf[:n].copy()
    out[:fade] = buf[:fade] * ramp + buf[n:n + fade] * (1 - ramp)
    return finish(reverb(out, 0.4, 0.15), 0.7, 0.0)


SOUNDS = {
    "hit_wood": hit_wood, "hit_stone": hit_stone,
    "hit_glass": hit_glass, "hit_pig": hit_pig, "thud": thud, "break_wood": break_wood,
    "break_stone": break_stone, "break_glass": break_glass, "pig_die": pig_die,
    "star": star, "click": click, "hop": hop,
    "bird_red_idle": bird_red_idle,
    "bird_yellow_idle": bird_yellow_idle,
    "bird_blue_idle": bird_blue_idle,
    "yellow_dash": yellow_dash, "blue_split": blue_split, "locked": locked,
    "explosion": explosion,
}

MUSIC = {"music_game": music_game}  # music_menu.ogg is the supplied theme song, not generated


def write_wav(path, x):
    pcm = (np.clip(x, -1, 1) * 32767).astype(np.int16)
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())


def main():
    have_ffmpeg = shutil.which("ffmpeg") is not None
    for name, fn in list(SOUNDS.items()) + list(MUSIC.items()):
        x = fn()
        wav_path = os.path.join(OUT, name + ".wav")
        write_wav(wav_path, x)
        if have_ffmpeg:
            ogg = os.path.join(OUT, name + ".ogg")
            subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav_path, "-c:a", "libvorbis", "-q:a", "4" if name not in MUSIC else "3", ogg], check=True)
            os.remove(wav_path)
            print(f"  wrote {name}.ogg  {len(x) / SR:.2f}s  peak={np.max(np.abs(x)):.2f}  {os.path.getsize(ogg) // 1024} KB")
        else:
            print(f"  wrote {name}.wav  {len(x) / SR:.2f}s")


if __name__ == "__main__":
    main()
