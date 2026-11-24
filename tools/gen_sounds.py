"""Synthesizes every High Life sound (original, no samples) into assets/highlife/sounds + sounds.json.

Run: python tools/gen_sounds.py [preview_dir]
One-shots are mono (Minecraft only attenuates mono sounds by distance); the rave track is stereo.
"""
import json, os, sys
import numpy as np
import soundfile as sf

SR = 44100
ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "highlife")
OUT = os.path.join(ROOT, "sounds")
rng = np.random.default_rng(1337)
np.seterr(over="ignore")


def t_axis(sec):
    return np.arange(int(sec * SR)) / SR


def env_ad(n, attack, decay_tau):
    t = np.arange(n) / SR
    e = np.exp(-np.maximum(0, t - attack) / decay_tau)
    a = int(attack * SR)
    if a > 0:
        e[:a] = np.linspace(0, 1, a)
    return e


def noise(sec):
    return rng.uniform(-1, 1, int(sec * SR))


def fft_filter(x, lo=0.0, hi=None):
    """Brick-ish band-pass by FFT mask with soft edges."""
    hi = hi or SR / 2
    spec = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / SR)
    mask = 1 / (1 + np.exp(-(f - lo) / max(20, lo * 0.1))) * 1 / (1 + np.exp((f - hi) / max(20, hi * 0.1)))
    return np.fft.irfft(spec * mask, len(x))


def sweep_filter(x, lo_fn, hi_fn, frame=2048):
    """Time-varying band-pass with overlap-add; lo_fn/hi_fn map 0..1 progress to Hz."""
    hop = frame // 2
    win = np.hanning(frame)
    out = np.zeros(len(x) + frame)
    f = np.fft.rfftfreq(frame, 1 / SR)
    for start in range(0, len(x), hop):
        seg = np.zeros(frame)
        chunk = x[start:start + frame]
        seg[:len(chunk)] = chunk
        p = start / max(1, len(x))
        lo, hi = lo_fn(p), hi_fn(p)
        mask = 1 / (1 + np.exp(-(f - lo) / max(20, lo * 0.15))) * 1 / (1 + np.exp((f - hi) / max(20, hi * 0.15)))
        out[start:start + frame] += np.fft.irfft(np.fft.rfft(seg * win) * mask, frame)
    return out[:len(x)]


def saw(freq, t, harmonics=12, phase=0.0):
    out = np.zeros_like(t)
    for k in range(1, harmonics + 1):
        if freq * k > SR / 2:
            break
        out += np.sin(2 * np.pi * k * freq * t + phase * k) / k
    return out * 0.6


def square(freq, t, harmonics=9):
    out = np.zeros_like(t)
    for k in range(1, harmonics * 2, 2):
        if freq * k > SR / 2:
            break
        out += np.sin(2 * np.pi * k * freq * t) / k
    return out * 0.8


def norm(x, peak=0.9):
    m = np.max(np.abs(x))
    return x if m == 0 else x / m * peak


def fade(x, fin=0.005, fout=0.02):
    x = x.copy()
    a, b = int(fin * SR), int(fout * SR)
    if a: x[:a] *= np.linspace(0, 1, a) if x.ndim == 1 else np.linspace(0, 1, a)[:, None]
    if b: x[-b:] *= np.linspace(1, 0, b) if x.ndim == 1 else np.linspace(1, 0, b)[:, None]
    return x


SOUNDS = {}  # event -> list of (file, stream)


def save(event, name, x, stream=False, peak=0.9, do_fade=True):
    os.makedirs(os.path.dirname(os.path.join(OUT, name)), exist_ok=True)
    x = norm(x, peak)
    if do_fade:
        x = fade(x)
    x = x.astype(np.float32)
    channels = 1 if x.ndim == 1 else x.shape[1]
    # libsndfile's vorbis encoder overflows the stack on big single writes (Windows) -> write in blocks
    with sf.SoundFile(os.path.join(OUT, name + ".ogg"), "w", SR, channels, format="OGG", subtype="VORBIS") as f:
        for i in range(0, len(x), 4096):
            f.write(x[i:i + 4096])
    SOUNDS.setdefault(event, []).append({"name": f"highlife:{name}", "stream": stream} if stream else f"highlife:{name}")


# =====================================================================
# Rave track: 128 bpm, A minor, 16 bars = 30 s, loops seamlessly
# =====================================================================
BPM = 128
BEAT = 60 / BPM
BAR = BEAT * 4
BARS = 16
N = int(round(BARS * BAR * SR))
L = np.zeros(N)
R = np.zeros(N)


def add(buf, start_sec, sig, gain=1.0):
    s = int(round(start_sec * SR)) % N
    end = s + len(sig)
    if end <= N:
        buf[s:end] += sig * gain
    else:  # wrap tail to the start so the loop stays seamless
        cut = N - s
        buf[s:] += sig[:cut] * gain
        buf[:end - N] += sig[cut:] * gain


def kick():
    t = t_axis(0.42)
    f = 45 + 110 * np.exp(-t / 0.035)
    ph = 2 * np.pi * np.cumsum(f) / SR
    body = np.sin(ph) * np.exp(-t / 0.16)
    click = noise(0.42) * np.exp(-t / 0.003) * 0.4
    return np.tanh((body + click) * 1.6)


def clap():
    n = noise(0.25)
    e = np.zeros(len(n))
    for off in (0, 0.011, 0.022):
        e += env_ad(len(n), 0.0, 0.01) * 0.6 if off == 0 else np.roll(env_ad(len(n), 0, 0.012), int(off * SR)) * 0.6
    e += np.roll(env_ad(len(n), 0, 0.09), int(0.03 * SR)) * 0.8
    return fft_filter(n, 900, 5000) * e


def hat(open_=False):
    n = noise(0.25 if open_ else 0.06)
    return fft_filter(n, 7000, 16000) * env_ad(len(n), 0.0, 0.07 if open_ else 0.012)


NOTE = lambda midi: 440.0 * 2 ** ((midi - 69) / 12)
# Am - F - C - G   (root midi for bass in octave 2)
PROG = [(45, [57, 60, 64]), (41, [53, 57, 60]), (48, [55, 60, 64]), (43, [55, 59, 62])]

sidechain = np.ones(N)
for b in range(BARS * 4):
    s = int(round(b * BEAT * SR))
    tt = np.arange(int(BEAT * SR)) / SR
    duck = 1 - 0.65 * np.exp(-tt / 0.09)
    sidechain[s:s + len(duck)] = np.minimum(sidechain[s:s + len(duck)], duck[:N - s])

drums_L = np.zeros(N); drums_R = np.zeros(N)
music_L = np.zeros(N); music_R = np.zeros(N)

K, CL, CH, OH = kick(), clap(), hat(), hat(True)
for bar in range(BARS):
    breakdown = bar in (12, 13)
    build = bar in (14, 15)
    root, chord = PROG[bar % 4]
    bar_t = bar * BAR
    for beat in range(4):
        bt = bar_t + beat * BEAT
        if not breakdown and not (bar == 15 and beat >= 2):
            add(drums_L, bt, K, 1.0); add(drums_R, bt, K, 1.0)
        if not breakdown and beat in (1, 3):
            add(drums_L, bt, CL, 0.55); add(drums_R, bt, CL, 0.5)
        if not breakdown and not build:
            add(drums_L, bt + BEAT / 2, OH, 0.25); add(drums_R, bt + BEAT / 2, OH, 0.32)
            for s16 in range(4):
                add(drums_L if s16 % 2 else drums_R, bt + s16 * BEAT / 4, CH, 0.18 if s16 % 2 else 0.12)
    # snare/clap roll + riser in the build
    if build:
        steps = 8 if bar == 14 else 16
        for i in range(steps):
            add(drums_L, bar_t + i * BAR / steps, CL, 0.25 + 0.3 * i / steps)
            add(drums_R, bar_t + i * BAR / steps, CL, 0.25 + 0.3 * i / steps)
    # rolling off-beat bass
    if not breakdown:
        for e8 in range(8):
            if e8 % 2 == 1 or bar >= 4:
                tt = t_axis(BEAT / 2 * 0.9)
                f = NOTE(root - 12 if e8 % 2 == 0 else root)
                note = saw(f, tt, harmonics=10) * env_ad(len(tt), 0.004, 0.12)
                add(music_L, bar_t + e8 * BEAT / 2, note, 0.55); add(music_R, bar_t + e8 * BEAT / 2, note, 0.55)
    # supersaw chord stabs (off-beats) in the drops, long pad in breakdown
    if not breakdown and not build:
        for beat in range(4):
            tt = t_axis(BEAT * 0.45)
            st_l = sum(saw(NOTE(n) * 1.004, tt, 14) + saw(NOTE(n) * 0.996, tt, 14) for n in chord)
            st_r = sum(saw(NOTE(n) * 1.007, tt, 14) + saw(NOTE(n) * 0.993, tt, 14) for n in chord)
            e = env_ad(len(tt), 0.003, 0.09)
            add(music_L, bar_t + beat * BEAT + BEAT / 2, st_l * e, 0.18)
            add(music_R, bar_t + beat * BEAT + BEAT / 2, st_r * e, 0.18)
    if breakdown or build:
        tt = t_axis(BAR)
        swell = np.minimum(1, tt / 0.6) * (0.9 if breakdown else 0.6)
        pad_l = sum(saw(NOTE(n) * 1.005, tt, 6) for n in chord)
        pad_r = sum(saw(NOTE(n) * 0.995, tt, 6) for n in chord)
        add(music_L, bar_t, pad_l * swell, 0.12); add(music_R, bar_t, pad_r * swell, 0.12)
    # arpeggio lead with ping-pong delay (from bar 8 on, and filtered-ish in breakdown)
    if bar >= 8 and not build:
        pattern = [chord[0], chord[1], chord[2], chord[1] + 12, chord[2], chord[1], chord[0] + 12, chord[2]]
        for s16 in range(16):
            tt = t_axis(BEAT / 4 * 0.85)
            n = pattern[s16 % 8] + 12
            note = square(NOTE(n), tt, harmonics=4 if breakdown else 7) * env_ad(len(tt), 0.002, 0.06)
            st = bar_t + s16 * BEAT / 4
            add(music_L, st, note, 0.16); add(music_R, st, note, 0.12)
            add(music_R, st + BEAT * 0.75, note, 0.07); add(music_L, st + BEAT * 1.5, note, 0.04)
# white-noise riser over the build
rs = int(round(14 * BAR * SR)); re_ = N
riser = sweep_filter(noise((re_ - rs) / SR), lambda p: 300 + 6000 * p, lambda p: 1200 + 14000 * p)
riser *= np.linspace(0, 1, len(riser)) ** 2
riser[-int(0.03 * SR):] *= np.linspace(1, 0, int(0.03 * SR))  # no click at the loop point
music_L[rs:re_] += riser * 0.35; music_R[rs:re_] += riser * 0.35

L = drums_L + music_L * sidechain
R = drums_R + music_R * sidechain
mix = np.tanh(np.stack([L, R], axis=1) * 0.9)
save("music.rave", "music/rave", mix, stream=True, peak=0.85, do_fade=False)

# =====================================================================
# Body / action sounds (mono)
# =====================================================================
def heartbeat():
    t = t_axis(0.7)
    def thump(start, gain, f0):
        x = np.zeros(len(t))
        s = int(start * SR)
        tt = t[: len(t) - s]
        x[s:] = np.sin(2 * np.pi * (f0 + 30 * np.exp(-tt / 0.02)) * tt) * np.exp(-tt / 0.07) * gain
        return x
    return thump(0.0, 1.0, 48) + thump(0.24, 0.7, 42)

save("heartbeat", "heartbeat", heartbeat())

def inhale():
    n = noise(1.1)
    x = sweep_filter(n, lambda p: 500 + 400 * p, lambda p: 2200 + 1800 * p)
    e = np.sin(np.pi * np.linspace(0, 1, len(x))) ** 1.5
    return x * e
save("smoke.inhale", "smoke/inhale", inhale(), peak=0.6)

def exhale():
    n = noise(1.6)
    x = fft_filter(n, 120, 2600)
    return x * env_ad(len(x), 0.06, 0.45)
save("smoke.exhale", "smoke/exhale", exhale(), peak=0.6)

def cough(seed):
    r = np.random.default_rng(seed)
    out = np.zeros(int(0.9 * SR))
    t0 = 0.0
    for i in range(r.integers(2, 4)):
        d = 0.16 + r.random() * 0.06
        n = r.uniform(-1, 1, int(d * SR))
        tt = np.arange(len(n)) / SR
        body = fft_filter(n, 280, 2200) + 0.5 * np.sin(2 * np.pi * (150 + 40 * r.random()) * tt)
        burst = body * env_ad(len(n), 0.004, 0.05)
        s = int(t0 * SR)
        out[s:s + len(burst)] += burst[: len(out) - s] * (1 - i * 0.2)
        t0 += d + 0.05 + r.random() * 0.06
    return out
for i in range(3):
    save("smoke.cough", f"smoke/cough{i + 1}", cough(10 + i), peak=0.8)

def bubbles():
    out = np.zeros(int(1.7 * SR))
    times = np.sort(rng.random(55) ** 0.7 * 1.55)
    for st in times:
        d = 0.03 + rng.random() * 0.03
        tt = t_axis(d)
        f = (250 + rng.random() * 300) * (1 + 2.5 * tt / d)
        b = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-tt / (d * 0.4))
        s = int(st * SR)
        out[s:s + len(b)] += b[: len(out) - s] * (0.4 + rng.random() * 0.6)
    return out + fft_filter(noise(1.7), 100, 600) * 0.05
save("bong.bubble", "bong/bubble", bubbles(), peak=0.7)

def lighter():
    click = fft_filter(noise(0.05), 2000, 12000) * env_ad(int(0.05 * SR), 0, 0.004)
    ping = np.sin(2 * np.pi * 3100 * t_axis(0.12)) * np.exp(-t_axis(0.12) / 0.02) * 0.3
    flame = fft_filter(noise(0.6), 80, 1400) * env_ad(int(0.6 * SR), 0.05, 0.25) * 0.7
    out = np.zeros(int(0.75 * SR))
    out[:len(click)] += click; out[:len(ping)] += ping
    s = int(0.06 * SR); out[s:s + len(flame)] += flame
    return out
save("lighter.flick", "lighter/flick", lighter(), peak=0.7)

def snort():
    n = noise(0.6)
    x = sweep_filter(n, lambda p: 400 + 1500 * p, lambda p: 1500 + 5000 * p)
    e = np.minimum(1, np.linspace(0, 1, len(x)) * 3) * np.exp(-np.maximum(0, np.linspace(0, 0.6, len(x)) - 0.45) / 0.05)
    return x * e
save("dose.snort", "dose/snort", snort(), peak=0.7)

def pill():
    out = np.zeros(int(0.5 * SR))
    for st in (0.0, 0.07):
        c = fft_filter(noise(0.02), 3000, 12000) * env_ad(int(0.02 * SR), 0, 0.003)
        s = int(st * SR); out[s:s + len(c)] += c * 0.5
    tt = t_axis(0.16)
    gulp = np.sin(2 * np.pi * np.cumsum(320 - 900 * tt) / SR) * np.sin(np.pi * tt / 0.16)
    s = int(0.25 * SR); out[s:s + len(gulp)] += gulp
    return out
save("dose.pill", "dose/pill", pill(), peak=0.7)

def balloon():
    tt = t_axis(0.18)
    squeak = np.sin(2 * np.pi * np.cumsum(900 + 120 * np.sin(2 * np.pi * 37 * tt) + 300 * tt / 0.18) / SR) * np.sin(np.pi * tt / 0.18)
    hiss = fft_filter(noise(0.9), 1500, 9000) * env_ad(int(0.9 * SR), 0.1, 0.3)
    out = np.zeros(int(1.1 * SR))
    out[:len(squeak)] += squeak * 0.6
    s = int(0.15 * SR); out[s:s + len(hiss)] += hiss * 0.8
    return out
save("dose.balloon", "dose/balloon", balloon(), peak=0.7)

# =====================================================================
# Trip sounds (mono, played only to the tripping player)
# =====================================================================
def chime(seed):
    r = np.random.default_rng(seed)
    tt = t_axis(2.2)
    base = 300 * 2 ** (r.integers(0, 12) / 12)
    out = np.zeros(len(tt))
    for ratio, g, tau in ((1, 1, 0.9), (2.76, 0.5, 0.5), (5.4, 0.3, 0.25), (8.93, 0.15, 0.15)):
        out += np.sin(2 * np.pi * base * ratio * tt) * g * np.exp(-tt / tau)
    swell = np.minimum(1, tt / 0.4)  # backwards-ish swell
    return out * swell + np.roll(out * swell, int(0.33 * SR)) * 0.35
for i in range(3):
    save("trip.chime", f"trip/chime{i + 1}", chime(20 + i), peak=0.55)

def whisper(seed):
    r = np.random.default_rng(seed)
    n = r.uniform(-1, 1, int(1.8 * SR))
    x = sweep_filter(n, lambda p: 1500 + 800 * np.sin(p * 9 + seed), lambda p: 4500 + 1500 * np.sin(p * 13))
    t = np.arange(len(x)) / SR
    syll = np.zeros(len(x))
    st = 0.05
    while st < 1.6:
        d = 0.08 + r.random() * 0.18
        s, e = int(st * SR), int((st + d) * SR)
        syll[s:e] = np.sin(np.linspace(0, np.pi, e - s)) * (0.5 + r.random() * 0.5)
        st += d + r.random() * 0.12
    return x * syll * (0.6 + 0.4 * np.sin(2 * np.pi * 0.5 * t))
for i in range(3):
    save("trip.whisper", f"trip/whisper{i + 1}", whisper(30 + i), peak=0.5)

def whoosh():
    n = noise(1.6)
    x = sweep_filter(n, lambda p: 200 + 1200 * np.sin(np.pi * p), lambda p: 800 + 6000 * np.sin(np.pi * p))
    tt = t_axis(1.6)
    tone = np.sin(2 * np.pi * np.cumsum(120 + 500 * np.sin(np.pi * tt / 1.6)) / SR) * 0.15
    return (x + tone) * np.sin(np.pi * np.linspace(0, 1, len(x))) ** 2
save("trip.whoosh", "trip/whoosh", whoosh(), peak=0.6)

def machine_elf(seed):
    r = np.random.default_rng(seed)
    out = np.zeros(int(1.4 * SR))
    st = 0.0
    while st < 1.2:
        d = 0.05 + r.random() * 0.12
        tt = t_axis(d)
        f0, f1 = 700 + r.random() * 2500, 700 + r.random() * 2500
        f = f0 + (f1 - f0) * (tt / d) + 150 * np.sin(2 * np.pi * (30 + r.random() * 40) * tt)
        c = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.sin(np.pi * tt / d)
        s = int(st * SR); out[s:s + len(c)] += c[: len(out) - s]
        st += d + r.random() * 0.05
    return out
for i in range(3):
    save("trip.machine_elf", f"trip/machine_elf{i + 1}", machine_elf(40 + i), peak=0.45)

def rumble():
    n = noise(3.0)
    tt = t_axis(3.0)
    x = fft_filter(n, 20, 140) * 2 + np.sin(2 * np.pi * 38 * tt) * 0.5
    return x * np.sin(np.pi * tt / 3.0) ** 1.2
save("salvia.rumble", "trip/rumble", rumble(), peak=0.8)

# ---- loops (seamless: all frequencies are whole cycles over the loop length) ----
def loop_freq(f, T):
    return round(f * T) / T

def drone():
    T = 12.0
    tt = t_axis(T)
    out = np.zeros(len(tt))
    for f, g in ((55, 1.0), (82.5, 0.6), (110.4, 0.35), (164.5, 0.2)):
        f = loop_freq(f, T)
        out += saw(f, tt, harmonics=6) * g
        out += saw(f + 1 / T * 3, tt, harmonics=6) * g * 0.7  # slow beating
    lfo = 0.65 + 0.35 * np.sin(2 * np.pi * (2 / T) * tt)
    shimmer = np.sin(2 * np.pi * loop_freq(880, T) * tt) * (0.5 + 0.5 * np.sin(2 * np.pi * (3 / T) * tt)) * 0.05
    return out * lfo + shimmer
save("drone.k_hole", "trip/k_hole_drone", fade(drone(), 0.008, 0.008), stream=True, peak=0.6, do_fade=False)

def wah():
    T = 4.0
    tt = t_axis(T)
    pulse = 0.5 + 0.5 * np.sin(2 * np.pi * 4.0 * tt - np.pi / 2)  # "wah-wah-wah" throb, 16 per loop; silent at loop point
    f = loop_freq(150, T)
    out = np.zeros(len(tt))
    for k in range(1, 16):
        weight = np.exp(-((k - (1 + 9 * pulse)) ** 2) / 6) / k ** 0.3  # moving formant
        out += np.sin(2 * np.pi * k * f * tt) * weight
    return out * (0.03 + 0.97 * pulse)
save("nitrous.wah", "trip/nitrous_wah", wah(), peak=0.55, do_fade=False)

# ---------------- sounds.json ----------------
SUBTITLES = {"smoke.cough": "Someone coughs", "smoke.inhale": "Someone inhales", "smoke.exhale": "Smoke exhaled",
             "bong.bubble": "Bong bubbles", "lighter.flick": "Lighter flicks", "dose.snort": "Someone snorts",
             "dose.pill": "Pill swallowed", "dose.balloon": "Balloon hisses"}
js = {}
for ev, files in SOUNDS.items():
    entry = {"sounds": files}
    if ev in SUBTITLES:
        entry["subtitle"] = f"subtitles.highlife.{ev}"
    js[ev] = entry
with open(os.path.join(ROOT, "sounds.json"), "w") as f:
    json.dump(js, f, indent=2)
with open(os.path.join(os.path.dirname(__file__), "subtitles.json"), "w") as f:
    json.dump({f"subtitles.highlife.{k}": v for k, v in SUBTITLES.items()}, f, indent=2)
print("events:", len(js), "files:", sum(len(v) for v in SOUNDS.values()))

if len(sys.argv) > 1:  # WAV copies for listening
    os.makedirs(sys.argv[1], exist_ok=True)
    for ev, files in SOUNDS.items():
        for fobj in files:
            name = (fobj["name"] if isinstance(fobj, dict) else fobj).split(":")[1]
            data, sr = sf.read(os.path.join(OUT, name + ".ogg"))
            sf.write(os.path.join(sys.argv[1], name.replace("/", "_") + ".wav"), data, sr)
