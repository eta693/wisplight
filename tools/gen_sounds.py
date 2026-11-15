"""Synthétise les sons du mod (mono, 44,1 kHz) et les encode en Ogg Vorbis via ffmpeg."""
import os
import subprocess
import tempfile
import wave

import numpy as np

SR = 44100
HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "wisplight", "sounds")
rng = np.random.default_rng(42)


def t(dur):
    return np.arange(int(SR * dur)) / SR


def env(n, attack=0.005, decay=1.0):
    x = np.arange(n) / SR
    a = np.clip(x / attack, 0, 1)
    return a * np.exp(-x / decay)


def bell(freq, dur, decay=0.6, partials=((1, 1.0), (2.76, 0.45), (5.4, 0.25), (8.93, 0.12))):
    x = t(dur)
    out = np.zeros_like(x)
    for ratio, amp in partials:
        out += amp * np.sin(2 * np.pi * freq * ratio * x) * np.exp(-x * ratio / (decay * 3))
    return out * env(len(x), 0.003, decay)


def place(buf, sig, start):
    i = int(start * SR)
    end = min(len(buf), i + len(sig))
    buf[i:end] += sig[: end - i]
    return buf


def lowpass(sig, alpha):
    out = np.zeros_like(sig)
    acc = 0.0
    for i, v in enumerate(sig):
        acc += alpha * (v - acc)
        out[i] = acc
    return out


def normalize(sig, peak=0.8):
    m = np.max(np.abs(sig)) or 1.0
    fade = np.ones_like(sig)
    n = min(len(sig), int(0.02 * SR))
    fade[-n:] = np.linspace(1, 0, n)
    return sig / m * peak * fade


def ambient(seed_notes):
    buf = np.zeros(int(SR * 1.8))
    for i, f in enumerate(seed_notes):
        sig = bell(f, 1.4, decay=0.7) * (0.9 - 0.2 * i)
        x = t(1.4)
        sig *= 1 + 0.15 * np.sin(2 * np.pi * 6 * x)  # léger trémolo
        place(buf, sig, 0.12 * i)
    return normalize(buf, 0.55)


def hurt():
    x = t(0.3)
    f = np.linspace(1400, 500, len(x))
    sig = np.sin(2 * np.pi * np.cumsum(f) / SR) * env(len(x), 0.002, 0.12)
    sig += 0.25 * lowpass(rng.standard_normal(len(x)), 0.2) * env(len(x), 0.001, 0.05)
    return normalize(sig, 0.7)


def death():
    buf = np.zeros(int(SR * 1.2))
    for i, f in enumerate([1568, 1319, 1047, 784]):
        place(buf, bell(f, 0.9, decay=0.35) * (1 - 0.15 * i), 0.13 * i)
    x = t(1.2)
    buf *= np.linspace(1, 0.2, len(x))
    return normalize(buf, 0.7)


def capture():
    buf = np.zeros(int(SR * 0.9))
    for i, f in enumerate([1047, 1319, 1568, 2093]):
        place(buf, bell(f, 0.6, decay=0.3), 0.07 * i)
    place(buf, bell(3520, 0.2, decay=0.05, partials=((1, 1.0), (1.5, 0.5))) * 0.5, 0.32)  # tintement du verre
    return normalize(buf, 0.75)


def release():
    x = t(0.8)
    noise = lowpass(rng.standard_normal(len(x)), 0.08)
    sweep = noise * np.sin(np.pi * np.clip(x / 0.8, 0, 1)) * 0.6
    f = np.linspace(700, 1900, len(x))
    tone = np.sin(2 * np.pi * np.cumsum(f) / SR) * env(len(x), 0.05, 0.4) * 0.5
    return normalize(sweep + tone, 0.7)


def essence_ready():
    buf = np.zeros(int(SR * 1.0))
    place(buf, bell(1568, 0.9, decay=0.5), 0.0)
    place(buf, bell(2349, 0.8, decay=0.5) * 0.8, 0.14)
    return normalize(buf, 0.6)


def harvest():
    buf = np.zeros(int(SR * 0.5))
    place(buf, bell(2637, 0.25, decay=0.06, partials=((1, 1.0), (2.2, 0.4))), 0.0)
    x = t(0.18)
    f = 300 + 1600 * (x / 0.18) ** 2
    blip = np.sin(2 * np.pi * np.cumsum(f) / SR) * env(len(x), 0.005, 0.08)
    place(buf, blip * 0.8, 0.08)
    return normalize(buf, 0.7)


def altar_working():
    x = t(1.6)
    f = 110 + 40 * x
    hum = 0.5 * np.sin(2 * np.pi * np.cumsum(f) / SR) + 0.25 * np.sin(2 * np.pi * np.cumsum(f * 2) / SR)
    hum *= np.clip(x / 0.4, 0, 1) * np.clip((1.6 - x) / 0.4, 0, 1)
    buf = hum.copy()
    for i, fr in enumerate([1319, 1568, 1976, 2637]):
        place(buf, bell(fr, 0.5, decay=0.25) * 0.3, 0.25 + 0.3 * i)
    return normalize(buf, 0.6)


def altar_done():
    buf = np.zeros(int(SR * 1.6))
    for i, fr in enumerate([1047, 1319, 1568, 2093]):
        place(buf, bell(fr, 1.4, decay=0.8) * 0.8, 0.02 * i)
    return normalize(buf, 0.75)


def staff_shoot():
    x = t(0.45)
    noise = lowpass(rng.standard_normal(len(x)), 0.25) * env(len(x), 0.005, 0.12)
    f = np.linspace(400, 1800, len(x))
    zap = np.sin(2 * np.pi * np.cumsum(f) / SR) * env(len(x), 0.002, 0.15)
    return normalize(noise * 0.6 + zap * 0.7, 0.7)


def orb_impact():
    buf = np.zeros(int(SR * 0.6))
    x = t(0.15)
    pop = lowpass(rng.standard_normal(len(x)), 0.35) * env(len(x), 0.001, 0.04)
    place(buf, pop, 0.0)
    place(buf, bell(2093, 0.5, decay=0.2) * 0.5, 0.01)
    place(buf, bell(3136, 0.4, decay=0.15) * 0.35, 0.05)
    return normalize(buf, 0.7)


def shade_ambient(seed):
    r = np.random.default_rng(seed)
    x = t(1.4)
    breath = lowpass(r.standard_normal(len(x)), 0.05) * np.sin(np.pi * np.clip(x / 1.4, 0, 1)) ** 2
    drone = 0.3 * np.sin(2 * np.pi * (70 + 8 * np.sin(2 * np.pi * 0.7 * x)) * x)
    return normalize(breath + drone * np.sin(np.pi * x / 1.4), 0.5)


def shade_hurt():
    x = t(0.35)
    hiss = (rng.standard_normal(len(x)) - lowpass(rng.standard_normal(len(x)), 0.1)) * env(len(x), 0.005, 0.12)
    growl = np.sin(2 * np.pi * np.cumsum(np.linspace(160, 90, len(x))) / SR) * env(len(x), 0.005, 0.15)
    return normalize(hiss * 0.5 + growl * 0.6, 0.7)


def shade_death():
    x = t(1.3)
    whoosh = lowpass(rng.standard_normal(len(x)), np.linspace(0.3, 0.02, len(x)).mean()) * np.exp(-x / 0.5)
    tone = np.sin(2 * np.pi * np.cumsum(np.linspace(220, 55, len(x))) / SR) * np.exp(-x / 0.6)
    return normalize(whoosh * 0.6 + tone * 0.6, 0.7)


def shade_steal():
    buf = np.zeros(int(SR * 0.9))
    for i, fr in enumerate([2093, 1568, 1319, 1047]):
        place(buf, bell(fr, 0.5, decay=0.2) * (0.8 - 0.1 * i), 0.08 * i)
    x = t(0.9)
    buf *= np.linspace(1.0, 0.3, len(x))
    return normalize(buf, 0.6)


def rune_charge():
    buf = np.zeros(int(SR * 1.2))
    for i, fr in enumerate([784, 1175, 1568]):
        place(buf, bell(fr, 1.0, decay=0.5) * (1 - 0.2 * i), 0.06 * i)
    return normalize(buf, 0.65)


def ritual():
    x = t(3.0)
    drone = (np.sin(2 * np.pi * 55 * x) + 0.5 * np.sin(2 * np.pi * 82.5 * x)) * np.clip(x / 1.0, 0, 1) * np.clip((3 - x) / 0.8, 0, 1)
    buf = drone * 0.5
    for i, fr in enumerate([523, 659, 784, 1047]):
        place(buf, bell(fr, 1.6, decay=0.9) * 0.4, 0.5 + 0.45 * i)
    return normalize(buf, 0.7)


def compass():
    buf = np.zeros(int(SR * 0.8))
    for i in range(3):
        place(buf, bell(3000, 0.05, decay=0.01, partials=((1, 1.0),)) * 0.6, 0.08 * i)
    place(buf, bell(1760, 0.6, decay=0.3), 0.3)
    return normalize(buf, 0.6)


def metal_hit(freq, dur, decay):
    return bell(freq, dur, decay=decay, partials=((1, 1.0), (2.41, 0.6), (3.93, 0.4), (5.67, 0.3), (7.12, 0.2)))


def warden_ambient(seed):
    r = np.random.default_rng(seed)
    x = t(2.0)
    drone = np.sin(2 * np.pi * (48 + 3 * np.sin(2 * np.pi * 0.5 * x)) * x) * 0.6
    creak = lowpass(r.standard_normal(len(x)), 0.02) * np.sin(np.pi * x / 2.0) * 2.0
    buf = (drone + creak) * np.sin(np.pi * np.clip(x / 2.0, 0, 1))
    place(buf, metal_hit(330 + 40 * seed, 1.2, 0.5) * 0.4, 0.6)
    return normalize(buf, 0.6)


def warden_hurt():
    buf = metal_hit(260, 0.8, 0.25)
    x = t(0.8)
    buf = buf + lowpass(rng.standard_normal(len(x)), 0.3) * env(len(x), 0.001, 0.05) * 0.5
    return normalize(buf, 0.8)


def warden_death():
    x = t(3.0)
    tone = np.sin(2 * np.pi * np.cumsum(np.linspace(220, 40, len(x))) / SR) * np.exp(-x / 1.4)
    buf = tone * 0.7
    for i in range(6):
        place(buf, metal_hit(400 + 90 * i, 0.6, 0.15) * 0.3, 1.0 + 0.18 * i)
    for i, fr in enumerate([1047, 1319, 1568, 2093]):
        place(buf, bell(fr, 1.0, decay=0.6) * 0.25, 2.0 + 0.05 * i)
    return normalize(buf, 0.8)


def warden_phase():
    buf = metal_hit(98, 3.0, 1.2) + metal_hit(147, 3.0, 1.0) * 0.6
    return normalize(buf, 0.85)


def warden_slam():
    x = t(1.0)
    thud = np.sin(2 * np.pi * np.cumsum(np.linspace(90, 35, len(x))) / SR) * env(len(x), 0.002, 0.25)
    noise = lowpass(rng.standard_normal(len(x)), 0.15) * env(len(x), 0.001, 0.12)
    buf = thud + noise * 0.6
    for i in range(5):
        place(buf, metal_hit(900 + 150 * i, 0.2, 0.04) * 0.2, 0.1 + 0.06 * i)
    return normalize(buf, 0.85)


def warden_charge():
    x = t(1.5)
    f = 200 + 1600 * (x / 1.5) ** 2
    whine = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.clip(x / 1.5, 0, 1)
    shimmer = rng.standard_normal(len(x)) * 0.1 * np.clip(x / 1.5, 0, 1)
    return normalize(whine * 0.6 + shimmer, 0.7)


def warden_burst():
    x = t(1.8)
    boom = lowpass(rng.standard_normal(len(x)), 0.06) * np.exp(-x / 0.35) * 2.5
    buf = boom
    for i, fr in enumerate([1047, 1568, 2093, 3136]):
        place(buf, bell(fr, 1.4, decay=0.7) * 0.5, 0.02 * i)
    return normalize(buf, 0.9)


def write_ogg(name, sig):
    path = os.path.join(OUT, name + ".ogg")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    pcm = (np.clip(sig, -1, 1) * 32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        with wave.open(tmp.name, "wb") as wf:
            wf.setnchannels(1)
            wf.setsampwidth(2)
            wf.setframerate(SR)
            wf.writeframes(pcm.tobytes())
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", tmp.name, "-ac", "1",
                        "-c:a", "libvorbis", "-q:a", "4", path], check=True)
    os.unlink(tmp.name)


if __name__ == "__main__":
    sounds = {
        "wisp/ambient1": ambient([1319, 1760]),
        "wisp/ambient2": ambient([1175, 1568, 2349]),
        "wisp/ambient3": ambient([1568, 1976]),
        "wisp/hurt": hurt(),
        "wisp/death": death(),
        "wisp/capture": capture(),
        "wisp/release": release(),
        "jar/essence_ready": essence_ready(),
        "jar/harvest": harvest(),
        "altar/working": altar_working(),
        "altar/done": altar_done(),
        "staff/shoot": staff_shoot(),
        "staff/impact": orb_impact(),
        "shade/ambient1": shade_ambient(1),
        "shade/ambient2": shade_ambient(2),
        "shade/hurt": shade_hurt(),
        "shade/death": shade_death(),
        "shade/steal": shade_steal(),
        "sanctuary/rune_charge": rune_charge(),
        "sanctuary/ritual": ritual(),
        "sanctuary/compass": compass(),
        "warden/ambient1": warden_ambient(1),
        "warden/ambient2": warden_ambient(2),
        "warden/hurt": warden_hurt(),
        "warden/death": warden_death(),
        "warden/phase": warden_phase(),
        "warden/slam": warden_slam(),
        "warden/charge": warden_charge(),
        "warden/burst": warden_burst(),
    }
    for n, s in sounds.items():
        write_ogg(n, s)
    print(len(sounds), "sons écrits")
