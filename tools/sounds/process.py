"""
Turns the owner's chosen recordings into the app's sound assets.

Loops: pick the steadiest stretch, make it seamless with an equal-power
crossfade of its end into its start, normalise to -20 dBFS RMS, 48 kHz
stereo Ogg Vorbis. Thunder: trim, add weight below 120 Hz, split long
recordings into separate strikes, normalise peaks to -1 dBFS.

Usage: python3 process.py <dir with the source mp3s> <app assets dir>
"""
import sys, os, json
import numpy as np, soundfile as sf
from scipy.signal import resample_poly, butter, sosfilt

SR = 48000

LOOPS = {  # sound code -> (source file name fragment, loop seconds)
    "RN": ("gentle-rain-07", 60),
    "DP": ("calming-rain", 60),
    "CA": ("copyright-free-rain-sounds", 60),
    "TE": ("relaxing-rain", 60),
}
THUNDER = ["loud-thunder", "thunder-strike", "thunder-clap", "thunder-sound"]
CREDITS = {
    "gentle-rain-07": "Gentle Rain 07 by Dragon Studio (Pixabay)",
    "calming-rain": "Calming Rain by liecio (Pixabay)",
    "copyright-free-rain-sounds": "Copyright Free Rain Sounds by Dragon Studio (Pixabay)",
    "relaxing-rain": "Relaxing Rain by Dragon Studio (Pixabay)",
    "loud-thunder": "Loud Thunder by Universfield (Pixabay)",
    "thunder-strike": "Thunder Strike by Universfield (Pixabay)",
    "thunder-clap": "Thunder Clap by u_q2hb2391vb (Pixabay)",
    "thunder-sound": "Thunder Sound by SoundReality (Pixabay)",
}

def load(fragment, src):
    path = next(os.path.join(src, f) for f in os.listdir(src) if fragment in f)
    return load_file(path)

def load_file(path):
    x, sr = sf.read(path, always_2d=True, dtype="float64")
    if x.shape[1] == 1:
        x = np.repeat(x, 2, axis=1)
    if sr != SR:
        g = np.gcd(sr, SR)
        x = resample_poly(x, SR // g, sr // g, axis=0)
    return x[:, :2]

def highpass(x, hz):
    return sosfilt(butter(2, hz, "highpass", fs=SR, output="sos"), x, axis=0)

def low_shelf(x, hz, db):
    # Weight: add a low-passed copy (simple, artefact-free shelf).
    low = sosfilt(butter(2, hz, "lowpass", fs=SR, output="sos"), x, axis=0)
    return x + low * (10 ** (db / 20) - 1)

def rms_db(x):
    return 20 * np.log10(np.sqrt((x ** 2).mean()) + 1e-12)

def steadiest(x, seconds, fade):
    """Start of the window whose loudness varies least (no stray bangs or gaps)."""
    n = int((seconds + fade) * SR)
    if len(x) <= n:
        return 0
    hop = SR
    env = np.sqrt(np.convolve((x ** 2).mean(1), np.ones(SR) / SR, mode="valid")[::hop])
    win = n // hop
    best, best_score = 0, 1e9
    skip = 3  # recordings often fade in
    for s in range(skip, len(env) - win):
        seg = env[s:s + win]
        score = seg.std() / (seg.mean() + 1e-9) + 0.5 * (seg.max() / (seg.mean() + 1e-9))
        if score < best_score:
            best, best_score = s, score
    return best * hop

def seamless(x, seconds, fade=3.0):
    start = steadiest(x, seconds, fade)
    n, f = int(seconds * SR), int(fade * SR)
    seg = x[start:start + n + f]
    if len(seg) < n + f:  # short source: use what there is
        n = len(seg) - f
    out = seg[:n].copy()
    t = np.linspace(0, np.pi / 2, f)[:, None]
    out[:f] = seg[:f] * np.sin(t) + seg[n:n + f] * np.cos(t)
    return out

def strikes(x, name):
    """Splits a recording into strikes at quiet points; short ones stay whole."""
    env = np.sqrt(np.convolve((x ** 2).mean(1), np.ones(SR // 10) / (SR // 10), mode="same"))
    if len(x) < 20 * SR:
        parts = [x]
    else:
        quiet = env < env.max() * 0.08
        parts, start = [], 0
        i = 4 * SR
        while i < len(x):
            if quiet[i] and i - start > 6 * SR:
                parts.append(x[start:i]); start = i
                i += 3 * SR
            i += SR // 10
        parts.append(x[start:])
        parts = [p for p in parts if len(p) > 3 * SR and np.abs(p).max() > 0.2 * np.abs(x).max()]
    out = []
    for p in parts:
        lead = np.argmax(np.sqrt((p ** 2).mean(1)) > np.abs(p).max() * 0.02)
        p = p[max(0, lead - SR // 50):]
        p = p[: 22 * SR]
        tail = min(len(p), int(1.5 * SR))
        p[-tail:] *= np.linspace(1, 0, tail)[:, None] ** 2
        out.append(p)
    return out

def save(x, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    data = np.clip(x, -1, 1).astype("float32")
    # libsndfile's Vorbis encoder can crash on large writes; feed it small blocks.
    with sf.SoundFile(path, "w", SR, 2, format="OGG", subtype="VORBIS") as fh:
        for i in range(0, len(data), 4096):
            fh.write(data[i:i + 4096])

def main(SRC, OUT):
    report = {}
    os.makedirs(OUT, exist_ok=True)
    for code, (frag, secs) in LOOPS.items():
        x = highpass(load(frag, SRC), 25)
        loop = seamless(x, secs)
        loop *= 10 ** ((-20 - rms_db(loop)) / 20)
        save(loop, f"{OUT}/{code}.ogg")
        report[code] = {"source": CREDITS[frag], "seconds": round(len(loop) / SR, 1), "rms_db": round(rms_db(loop), 1)}

    clips = []
    for frag in THUNDER:
        x = low_shelf(highpass(load(frag, SRC), 22), 120, 5)  # heavier, deeper rumble
        for p in strikes(x, frag):
            clips.append((frag, p * (10 ** (-1 / 20) / np.abs(p).max())))
    for i, (frag, p) in enumerate(clips, 1):
        save(p, f"{OUT}/thunder/{i}.ogg")
        report[f"thunder/{i}"] = {"source": CREDITS[frag], "seconds": round(len(p) / SR, 1)}

    with open(f"{OUT}/CREDITS.txt", "w") as fh:
        fh.write("Recordings used under the Pixabay Content License (https://pixabay.com/service/license-summary/),\n")
        fh.write("processed (trimmed, looped, equalised) and mixed inside SleepBetter.\n\n")
        for v in sorted({CREDITS[k] for k in CREDITS}):
            fh.write(f"- {v}\n")
    print(json.dumps(report, indent=1))

if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
