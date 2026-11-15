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
    }
    for n, s in sounds.items():
        write_ogg(n, s)
    print(len(sounds), "sons écrits")
