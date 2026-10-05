"""
Fetches free, high-quality field recordings for the sounds that have no
bundled recording yet, at build time (the build machine has internet).

For each sound it searches Freesound for Creative Commons 0 recordings that
are long enough, most-downloaded first, checks the licence on the sound's own
page, downloads the high-quality preview and processes it exactly like the
bundled rain: the steadiest stretch, made seamless with an equal-power
crossfade, normalised to -20 dBFS RMS, 48 kHz stereo Ogg Vorbis.

Anything that fails is skipped: that sound keeps its generated version.
A report (fetched-sounds.json) says what was used and why others were not.

Usage: python3 fetch.py <app assets/sounds dir> <report path>
"""
import sys, os, re, json, html, tempfile, urllib.request, urllib.parse
import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from process import SR, load_file, highpass, low_shelf, rms_db, seamless, save

UA = "Mozilla/5.0 (X11; Linux x86_64) SleepBetter-build/1.0"

# code -> (search terms, extra words a title must not contain, filter)
WANTED = {
    "SW": (["ocean waves beach", "sea waves", "waves crashing shore"], ["underwater", "boat", "ship", "music"], "warm"),
    "CF": (["campfire crackling", "fireplace crackle", "fire crackling"], ["music", "whoosh", "torch"], "warm"),
    "NF": (["night crickets forest", "summer night crickets", "night ambience crickets"], ["city", "traffic", "music", "dog"], "plain"),
    "BI": (["birds singing forest morning", "birdsong forest", "dawn chorus"], ["city", "traffic", "music", "parrot", "crow"], "plain"),
    "ST": (["mountain stream", "creek water flowing", "brook stream"], ["rain", "music", "tap", "toilet"], "plain"),
    "WD": (["water drops cave", "water dripping", "dripping water cave"], ["tap", "sink", "music", "faucet"], "plain"),
}

DEBUG = {}
LICENCE_OK = ("creativecommons.org/publicdomain/zero", "Creative Commons 0", "CC0")


def get(url, binary=False):
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=60) as r:
        data = r.read()
    return data if binary else data.decode("utf-8", "replace")


def search(query):
    """Sound page URLs for a query, filtered to CC0 and 1-15 minutes, most downloaded first."""
    params = {
        "q": query,
        "f": 'license:"Creative Commons 0" duration:[60 TO 900]',
        "s": "num_downloads desc",
    }
    page = get("https://freesound.org/search/?" + urllib.parse.urlencode(params))
    seen, out = set(), []
    for user, sid in re.findall(r'href="/people/([^/"]+)/sounds/(\d+)/"', page):
        if sid not in seen:
            seen.add(sid)
            out.append(f"https://freesound.org/people/{user}/sounds/{sid}/")
    return out


def inspect(url):
    """Title, author, licence check and preview URL for one sound page."""
    page = get(url)
    title = html.unescape((re.search(r'<meta property="og:title" content="([^"]*)"', page) or re.search(r"<title>([^<]*)", page)).group(1))
    audio = re.search(r'<meta property="og:audio" content="([^"]+)"', page)
    preview = audio.group(1) if audio else None
    if preview is None:
        m = re.search(r'(https://cdn\.freesound\.org/previews/[^"\']+?-(?:hq|lq)\.(?:mp3|ogg))', page)
        preview = m.group(1) if m else None
    if preview:
        preview = html.unescape(preview).strip()
        if preview.startswith("//"):
            preview = "https:" + preview
        elif preview.startswith("/"):
            preview = "https://freesound.org" + preview
        preview = re.sub(r"-lq\.(mp3|ogg)$", r"-hq.\1", preview)
    DEBUG.setdefault("pages", []).append({"url": url, "preview": preview,
        "previews_on_page": sorted(set(re.findall(r'[^"\'\s]*previews/[^"\'\s]+', page)))[:6]})
    licence_block = page[page.find("icense"):][:4000] if "icense" in page else page
    cc0 = any(k in page for k in LICENCE_OK) and "noncommercial" not in licence_block.lower()
    author = url.rstrip("/").split("/")[-3]
    return title, author, cc0, preview


def process(path, flavour):
    x = load_file(path)
    if len(x) < 45 * SR:
        raise ValueError(f"too short ({len(x) / SR:.0f} s)")
    x = highpass(x, 30)
    if flavour == "warm":
        x = low_shelf(x, 200, 2)  # a little body for fire and surf
    loop = seamless(x, min(60, len(x) / SR - 4))
    loop *= 10 ** ((-20 - rms_db(loop)) / 20)
    # Round off the rare loud crackle or crash instead of turning everything down.
    knee = 0.7
    over = np.abs(loop) > knee
    loop[over] = np.sign(loop[over]) * (knee + 0.28 * np.tanh((np.abs(loop[over]) - knee) / 0.28))
    return loop


def main(out, report_path):
    report, credits = {}, []
    for code, (queries, banned, flavour) in WANTED.items():
        if os.path.exists(f"{out}/{code}.ogg"):
            report[code] = {"status": "already bundled"}
            continue
        tried = []
        done = False
        for q in queries:
            try:
                pages = search(q)
            except Exception as e:  # noqa: BLE001
                tried.append({"query": q, "error": str(e)[:200]})
                continue
            for url in pages[:6]:
                try:
                    title, author, cc0, preview = inspect(url)
                    if not cc0:
                        tried.append({"url": url, "skip": "not CC0"}); continue
                    if any(b in title.lower() for b in banned):
                        tried.append({"url": url, "skip": f"title: {title}"}); continue
                    if not preview:
                        tried.append({"url": url, "skip": "no preview"}); continue
                    ext = preview.rsplit(".", 1)[-1]
                    try:
                        data = get(preview, binary=True)
                    except Exception as e:  # noqa: BLE001
                        raise RuntimeError(f"preview {preview}: {e}")
                    with tempfile.NamedTemporaryFile(suffix="." + ext, delete=False) as fh:
                        fh.write(data)
                    loop = process(fh.name, flavour)
                    os.unlink(fh.name)
                    save(loop, f"{out}/{code}.ogg")
                    report[code] = {"status": "fetched", "title": title, "author": author, "url": url,
                                    "seconds": round(len(loop) / SR, 1), "rms_db": round(rms_db(loop), 1)}
                    credits.append(f"- {title} by {author} (Freesound, CC0): {url}")
                    done = True
                    break
                except Exception as e:  # noqa: BLE001
                    tried.append({"url": url, "error": str(e)[:200]})
            if done:
                break
        if not done:
            report[code] = {"status": "kept generated", "tried": tried}
        print(code, report[code].get("status"), report[code].get("title", ""), flush=True)

    if credits:
        with open(f"{out}/CREDITS.txt", "a") as fh:
            fh.write("\nRecordings dedicated to the public domain (CC0) on Freesound, processed the same way:\n\n")
            fh.write("\n".join(credits) + "\n")
    report["debug"] = {"pages": DEBUG.get("pages", [])[:5]}
    with open(report_path, "w") as fh:
        json.dump(report, fh, indent=1)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
