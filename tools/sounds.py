#!/usr/bin/env python3
"""Makes the app's own notification sounds from plain tones (no recordings, no rights to clear).

Writes 16-bit mono WAV files to androidApp/src/main/res/raw/. The same shapes as the approved
prototype (docs/design/prototype-src/ink-v7/v7.js): a short soft envelope per tone.
Run: python3 tools/sounds.py
"""
import math
import os
import struct
import wave

RATE = 22050
OUT = os.path.join(os.path.dirname(__file__), '..', 'androidApp', 'src', 'main', 'res', 'raw')


def tone(buf, t0, f, d, v=0.25, f2=None, kind='sine', attack=0.006):
    start = int(t0 * RATE)
    n = int(d * RATE)
    need = start + n + 1
    if len(buf) < need:
        buf.extend([0.0] * (need - len(buf)))
    phase = 0.0
    for i in range(n):
        t = i / RATE
        freq = f if f2 is None else f * (f2 / f) ** (t / d)
        phase += 2 * math.pi * freq / RATE
        if kind == 'triangle':
            s = 2 / math.pi * math.asin(math.sin(phase))
        else:
            s = math.sin(phase)
        # quick rise, then an exponential fall to near silence at the end
        env = min(1.0, t / attack) * math.exp(-5.5 * t / d)
        buf[start + i] += v * env * s


def write(name, buf, pad=0.05):
    buf = buf + [0.0] * int(pad * RATE)
    peak = max(1e-9, max(abs(x) for x in buf))
    scale = min(1.0, 0.89 / peak)
    path = os.path.join(OUT, name + '.wav')
    with wave.open(path, 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b''.join(struct.pack('<h', int(max(-1, min(1, x * scale)) * 32767)) for x in buf))
    return path


def main():
    os.makedirs(OUT, exist_ok=True)
    sounds = {}
    b = []; tone(b, 0, 1046, 0.5, 0.3, f2=620); tone(b, 0.02, 2093, 0.18, 0.05); sounds['yawm_drop'] = b
    b = []; tone(b, 0, 1568, 0.09, 0.2); tone(b, 0.15, 1568, 0.09, 0.2); sounds['yawm_tap'] = b
    b = []; tone(b, 0, 150, 0.28, 0.55, f2=70); tone(b, 0.03, 784, 0.7, 0.1); sounds['yawm_seal'] = b
    b = []
    for i, f in enumerate([659, 784, 988]):
        tone(b, i * 0.11, f, 0.32, 0.18)
    sounds['yawm_paper'] = b
    b = []; tone(b, 0, 587, 1.5, 0.2); tone(b, 0, 1174, 1.0, 0.05); tone(b, 0.65, 880, 1.8, 0.18); tone(b, 0.65, 1760, 1.1, 0.04); sounds['yawm_bell'] = b
    b = []
    for i, f in enumerate([880, 740, 587]):
        tone(b, i * 0.3, f, 0.8, 0.18)
    sounds['yawm_breath'] = b
    # The alarm: a three-note figure repeated for six seconds, rising from soft to full. Android
    # repeats it until the user answers (FLAG_INSISTENT), so it rings even if the screen cannot show.
    b = []
    for i in range(6):
        for j, f in enumerate([784, 988, 1175]):
            tone(b, i * 1.0 + j * 0.2, f, 0.22, 0.1 + 0.2 * i / 5, kind='triangle')
    sounds['yawm_wake'] = b
    for name, buf in sounds.items():
        p = write(name, buf)
        print(name, os.path.getsize(p), 'bytes')


if __name__ == '__main__':
    main()
