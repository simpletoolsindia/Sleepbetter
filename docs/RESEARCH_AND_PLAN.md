# SleepBetter — Research & Product Plan

Native Android app (Kotlin + Jetpack Compose) for **focus, reading and sleep**:
a natural-sounding ambient **sound mixer** (rain, thunder, tent, car, campfire,
forest, birds, water drops, noise colours, focus music), **timed loop playback**,
**sleep tracking**, **bedtime reminders**, a **sleep-health risk score with
suggestions**, and **delightful character animations** (sleepy panda, dino, …).

> Status: research + plan. Nothing is built yet. Items marked **VERIFY** must be
> checked by a person before shipping (licences, Play policies).

---

## 1. Sound research

### 1.1 Where to get legally usable sounds

| Source | Licence | Commercial app OK? | Attribution | Notes |
|---|---|---|---|---|
| **Freesound.org** (filter: *Creative Commons 0*) | CC0 per file | ✅ | Not required | Best variety of real field recordings. Licence is **per file** — always check the sound page. Use the CC0 filter. |
| Freesound (CC BY files) | CC BY 4.0 | ✅ | **Required** | OK if we show credits in *Settings → Sound credits*. |
| Freesound (CC BY‑NC files) | Non-commercial | ❌ | — | **Never use** — the app may be monetised later. |
| **Pixabay Sound Effects / Music** | Pixabay Content License | ✅ inside the app | Not required | Cannot resell/redistribute files *standalone*. Mitigation: ship only inside our mixer, never offer a "download raw file" button. **VERIFY** the full licence's prohibited-uses list before release. |
| **Mixkit** | Mixkit Free License | ✅ | Not required | Has "night forest with insects", "summer night crickets loop", storm/thunder packs. **VERIFY** the SFX-specific licence terms. |
| **Sonniss GDC Game Audio Bundles** | Royalty-free, perpetual | ✅ | Not required | Professional-grade rain/thunder/nature libraries (200 GB+ archive). Only bans: claiming as your own, AI training. |
| **OpenGameArt.org** (CC0 entries) | CC0 | ✅ | Not required | Has ready-made CC0 *loopable* rain (4 loops, 25–45 s) and fireplace loops. Mono/short — use as secondary layers. |
| **Record our own** | We own it | ✅ | — | **Recommended for the signature sounds** (rain on tent, rain on car) — CC0 versions of these are rare and usually low quality. A Zoom H1n/H5 + windshield on a rainy weekend is enough. |
| BBC Sound Effects | RemArc licence | ❌ **non-commercial only** | — | **Do not use.** |
| YouTube "10 hours of rain" rips | Copyrighted | ❌ | — | **Do not use.** |

### 1.2 The 10 core sounds (+ extras)

Each sound is a *layer* in the mixer. "Bed" = continuous loop, "One-shots" =
short random events scattered over the bed (this is what makes it feel real —
see §2).

| # | Sound | Layer design | What to search / candidate recordings to evaluate |
|---|---|---|---|
| 1 | **Gentle rain** | Bed (3 intensity variants: light/medium/heavy, crossfaded by the "intensity" slider) | Freesound CC0: `rain`, `rain light`, `rain garden`; candidate *Rain_Attic_01.WAV* (CC0, Schoeps stereo pair); OpenGameArt *rain loopable* (CC0) |
| 2 | **Heavy rain / downpour** | Bed (heavy variant of #1) | Freesound CC0 *"Ambience, Rain, Heavy, A/B/C"* series (**VERIFY** licence on page — some of this author's work is CC BY) |
| 3 | **Thunder** | Bed (very low distant rumble) + **one-shots** (8–15 distinct claps/rolls, random every 40–180 s, random pan/volume) | Freesound CC0 *"rainstorm with distant thunder.wav"* (Southern England, 2015, CC0); *"two distant thunders with rain"*; Mixkit thunder; Sonniss bundles |
| 4 | **Rain on tent** | Bed + occasional heavier drips | Self-record (nylon tent, mic inside). Fallbacks: Pixabay rain-on-tent, Sonniss. |
| 5 | **Rain on car** | Bed (roof drumming, interior muffled) + one-shot "wiper swish" optional | Self-record (parked car, engine off, mic on the dashboard). Freesound has a 4-channel heavy-rain car-interior recording — **VERIFY** licence. |
| 6 | **Forest birds (morning)** | Bed (soft dawn chorus) + one-shot individual calls | Freesound CC0 `dawn chorus`, `birdsong forest`; Sonniss nature packs |
| 7 | **Night forest** | Bed (crickets, light wind) + one-shots (owl hoots, frogs, rustles) | Freesound CC0 *"Night Crickets Ambience on Rural Property"* (CC0, Zoom H4n stereo); Mixkit "Night forest with insects" |
| 8 | **Campfire** | Bed (low roar) + one-shots (pops, crackles, log shifts) | Freesound CC0 *"Fire Crackles.WAV"* (ID_23), crackling fireplace by *martats* (CC0); OpenGameArt *Fireplace Sound loop* (PagDev, CC0), *Fire Crackling* (AntumDeluge, CC0) |
| 9 | **Water drops (cave drip)** | Sparse one-shots with reverb tail + quiet bed | Freesound CC0 `water drop cave`, `drip`; Pixabay "water drops" |
| 10 | **Sleep noise (pink / brown / white)** | **Generated in code** — no licence, infinitely non-repeating, zero APK size | Procedural (see §2.4) |

**Extras (Phase 2):** rain on window/glass, rain on tin roof, river/stream,
ocean waves, wind in trees, fan, café murmur, distant train, purring cat,
soft focus music.

### 1.3 Focus music

* **Option A (recommended for MVP):** 6–10 licensed ambient/lo‑fi tracks from
  **Pixabay Music** (commercial use, no attribution). Pick tracks with **no
  vocals, slow tempo (60–80 BPM), no sudden drops** — lyrics compete with
  reading and verbal work.
* **Option B (Phase 3 differentiator):** a *generative* ambient pad engine
  (slow chords on a soft synth, randomised every few minutes). Never repeats,
  no licensing.
* **Avoid marketing claims** about binaural beats / "40 Hz" / "ADHD cure":
  evidence is weak or mixed.

### 1.4 What the science actually says (for in-app copy)

* **Pink noise & sleep:** studies showed more slow-wave (deep) sleep and better
  next-day memory, *but* those studies used short pulses timed to brain waves,
  not continuous playback, and later multi-night studies were mixed.
  App copy: *"Steady, natural sound can mask disruptive noises and help many
  people fall asleep."* — no stronger claim.
* **Noise & focus:** a 2024 meta-analysis (13 studies, youth with ADHD) found
  modest gains from white/pink noise, but roughly ⅓ did *worse*. Brown-noise
  evidence is mostly anecdotal. → Let users experiment; show a
  "Did this help you focus?" thumbs-up after sessions to personalise presets.

---

## 2. Making it sound like *real* rain (the core differentiator)

Most rain apps sound fake because of **audible looping** (you hear the same
drip every 30 s) and **clicks at the loop point**. Plan:

### 2.1 Asset preparation (offline, in Reaper/Audacity)
1. Choose **long clean sections (3–5 min)** — no cars, voices, planes, dogs.
2. Remove hum (high-pass ~40 Hz), gentle EQ; keep it *wide stereo*.
3. Make a **seamless loop**: overlap the tail and head with an equal-power
   crossfade of 2–5 s, cut at zero crossings. Verify by looping 10× and
   listening on headphones.
4. **Loudness-normalise every bed** to the same integrated loudness
   (e.g. −24 LUFS, true peak ≤ −2 dBTP) so sliders behave predictably.
5. Export **Ogg Opus 128 kbps stereo, 48 kHz** (Opus/Vorbis loop gaplessly;
   MP3 adds encoder padding → gap at the loop point).
6. Record every file's source/licence in `sounds/LICENSES.csv`.

### 2.2 Runtime tricks
* **Multiple bed variants with co-prime lengths** (e.g. 181 s and 233 s) layered
  at slightly different gains → the combined pattern only repeats after hours.
* **Randomised one-shots** (thunder, owl, fire pops, drips, bird calls):
  random interval, random choice (no immediate repeat), random pan
  (−0.6…0.6), random gain (−6…0 dB), occasional slight pitch shift (±3 %).
* **Slow "weather" modulation:** a very slow random LFO (period 2–6 min) moves
  rain intensity ±15 % so the storm *breathes* like real weather.
* **Thunder after a lightning flash:** the UI flashes when a thunder one-shot
  is scheduled and the sound follows 0.5–3 s later (distance illusion).
* **"Where are you?" filter:** a low-pass + muffling preset turns plain rain into
  "inside a cabin" / "under a blanket". It reuses the same assets.
* **Smooth everything:** 300 ms gain ramps on slider moves, 3–10 s fade-in at
  start, long fade-out (1–5 min, user-set) at timer end so sleepers are not
  woken by an abrupt stop.

### 2.3 Audio engine architecture

```
 ┌──────────── MixerService : MediaSessionService (foreground, type=mediaPlayback)
 │   MediaSession ── SimpleBasePlayer adapter (lock-screen / notification / BT controls)
 │        │
 │   MixerEngine (Kotlin, own thread)
 │     ├─ LayerBed[]      – streaming Opus decode (MediaExtractor+MediaCodec) → ring buffer,
 │     │                    sample-accurate crossfade looping
 │     ├─ OneShotScheduler – random events, preloaded short PCM clips
 │     ├─ NoiseGenerator  – white / pink (Voss-McCartney or Paul Kellet filter) / brown (integrated, leaky)
 │     ├─ per-layer gain ramps, pan, low-pass (biquad)
 │     ├─ master gain + limiter (avoid clipping when many layers are loud)
 │     └─ AudioTrack (float PCM, 48 kHz stereo, USAGE_MEDIA, CONTENT_TYPE_SONIFICATION→MUSIC)
 └─ SleepTimer (engine clock) → fade-out → stop → stopSelf()
```

* **Why a custom mixer instead of N × ExoPlayer?** Sample-accurate looping, one
  audio focus/session, cheap per-layer DSP, synchronised events for UI
  animations, and lower battery use over 8 h. Media3 is still used for the
  `MediaSession`/notification layer.
* **Fallback/MVP shortcut:** if schedule is tight, ship v0.1 with one
  Media3 ExoPlayer per layer (`REPEAT_MODE_ONE`, Ogg files) and migrate later
  — the `MixerEngine` interface stays the same.
* **Audio focus:** request `AUDIOFOCUS_GAIN`; on transient loss *duck* instead of
  pausing (users often get a notification while falling asleep); pause on
  becoming-noisy (headphones unplugged).
* **Play policy:** Android 14+ requires
  `android:foregroundServiceType="mediaPlayback"` +
  `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permission; the type is reviewed by
  Google Play — ours is a genuine media-playback use.

### 2.4 Sleep timer / loop duration
Choices: **15 m, 30 m, 45 m, 1 h, 2 h, 3 h, 4 h, 8 h, ∞, custom (h:m)**, plus
"Until alarm time". Fade-out length setting (off / 1 / 3 / 5 min). The timer
lives inside the engine clock (the service is already foreground), so no
`AlarmManager` is needed for it. Show remaining time in the media
notification (on Android 16+, consider a progress-style "Live Update"
notification — **VERIFY** API availability on target devices).

### 2.5 Mixer UX
* Grid of sound tiles (animated icons). Tap = on/off, long-press or drag = volume.
* Active layers show a vertical slider + "intensity"/"distance" sub-control
  where relevant (rain intensity, thunder frequency, fire size).
* **Presets** (editable, savable, shareable as a short code):

| Preset | Mix |
|---|---|
| Deep Focus | Medium rain 70 % · Brown noise 20 % |
| Cozy Tent | Rain on tent 80 % · Distant thunder 25 % · Night forest 15 % |
| Road-trip Nap | Rain on car 85 % · Brown noise 10 % |
| Campfire Reading | Campfire 60 % · Night forest 30 % · Light rain 20 % |
| Storm Sleeper | Heavy rain 70 % · Thunder 50 % (rolls every ~90 s) |
| Morning Study | Forest birds 50 % · Light rain 30 % · Focus music 25 % |
| Cave Calm | Water drops 60 % · Brown noise 30 % |

* Max ~6 simultaneous layers (keeps the mix clean and CPU low).

---

## 3. Sleep tracking research

### 3.1 Data sources (combine, from most to least accurate)

| Source | What it gives | Effort | Notes |
|---|---|---|---|
| **Health Connect** (`SleepSessionRecord`) | Sessions + **stages** (awake, light, deep, REM) from wearables (Pixel/Galaxy Watch, Fitbit, Oura, …) | Medium | Read-only for us. Needs Play Console *Health apps* declaration + privacy policy. Write our own manual sessions too so other apps see them. |
| **Google Play services Sleep API** | `SleepSegmentEvent` (start/end) + `SleepClassifyEvent` every ~10 min (confidence 0–100, motion, light) | Low | Passive, low battery, phone-only. Needs `ACTIVITY_RECOGNITION`. **No stages.** **VERIFY** API is still supported at implementation time. |
| **"Sleep now" button + wake check-in** | Bedtime / wake time, subjective quality (😴 1–5), awakenings | Low | Always works; also fills gaps. Starting the mixer at night with a sleep preset can prompt "Going to sleep?". |
| Phone accelerometer on mattress | Movement-based "light vs restless" | High | Battery-heavy, unreliable — **not planned** for MVP. |

**Honesty rule:** a phone alone **cannot** measure REM/deep sleep. Show real
stages only when they come from a wearable via Health Connect. Otherwise show
an **estimated cycle chart** (≈90 min cycles, real range 70–120 min, 4–6 per
night; deep sleep front-loaded, REM longer toward morning) clearly labelled
*"Estimate"*.

### 3.2 Metrics we compute
* Total sleep time (TST), time in bed, sleep efficiency (if in-bed known)
* Bedtime & wake time; **consistency** = std-dev of mid-sleep over 7/14 days
* **Sleep Regularity Index (SRI)**, 0–100, when we have minute-level asleep/awake
  data (Sleep API / Health Connect)
* **Sleep debt** = Σ(target − TST) over the last 7 days (floor at 0 per night)
* Social jet lag = mid-sleep (free days) − mid-sleep (work days)
* Weekly trends, best/worst nights, correlation with what was played

### 3.3 Sleep-health score and risk level

Evidence base:
* **AASM + Sleep Research Society consensus:** adults need **≥ 7 h** regularly;
  **≤ 6 h** is inadequate. Chronic short sleep is associated with weight gain,
  diabetes, hypertension, heart disease, stroke, depression, impaired immunity,
  more errors/accidents and higher mortality. ~35 % of US adults sleep < 7 h.
* **Regularity matters as much or more than duration:** UK Biobank (~61 000
  people, accelerometry) found the most regular sleepers had **20–48 % lower
  all-cause mortality** vs the least regular; regularity predicted mortality
  better than duration.

Score (0–100, rolling 7 nights, minimum 3 nights of data):

| Component | Weight | Full points | Zero points |
|---|---|---|---|
| Duration (avg TST) | 40 | 7–9 h | ≤ 5 h (or > 10.5 h) |
| Regularity (SRI, or mid-sleep SD) | 30 | SRI ≥ 85 / SD ≤ 30 min | SRI ≤ 60 / SD ≥ 120 min |
| Sleep debt (7-day) | 15 | ≤ 1 h | ≥ 7 h |
| Quality (subjective rating, efficiency, wakeups) | 15 | rating ≥ 4, eff. ≥ 85 % | rating 1, eff. ≤ 65 % |

| Level | Score | Plus hard rule | UI |
|---|---|---|---|
| 🟢 **Good** | ≥ 75 | — | Happy, stretching panda |
| 🟡 **Medium risk** | 50–74 | or avg 6–7 h | Yawning dino |
| 🔴 **At risk** | < 50 | or avg < 6 h for 5+ of 7 nights | Droopy sloth + "why it matters" card |

Rules for health copy (also Google Play *Health* policy):
* It is a **wellness indicator, not a diagnosis.** Always show a disclaimer.
* If a user logs poor sleep ≥ 3 nights/week for ≥ 3 months, or reports loud
  snoring/gasping, suggest **talking to a doctor** (chronic insomnia / sleep
  apnoea criteria) — never "you have X".
* Explain *why* in plain language and link the sources (AASM, CDC, the SRI study).

### 3.4 Suggestions engine (rule-based, local)
Personalised tips chosen from triggers, e.g.:

| Trigger | Suggestion |
|---|---|
| Bedtime SD > 60 min | "Pick one bedtime and keep it within 30 min, even on weekends." |
| Avg TST < 7 h | "You're ~X h short this week. Try going to bed 20 min earlier for 3 nights." |
| Late screen use (app open after target bedtime) | "Dim the screen / enable wind-down 45 min before bed." |
| Social jet lag > 1 h | "Weekend lie-ins shift your clock — wake within 1 h of your weekday time." |
| Low subjective quality + caffeine logged after 2 pm | "Caffeine lasts 5–6 h — try your last coffee before 2 pm." |
| Good streak | Celebrate 🎉 (animated character), streak badge |

Plus an "**Why sleep matters**" learn section: short animated cards (memory,
mood, immunity, heart, weight, focus, safety).

### 3.5 Reminders & alarms
* **Bedtime reminder** = wake time − sleep goal − wind-down (default 30 min).
  Inexact `AlarmManager` / WorkManager is fine (±10 min ok) → no special permission.
* **Wind-down reminder** with a one-tap "Start my sleep mix".
* **Morning check-in** ("How did you sleep?") after detected/expected wake.
* **Smart wake alarm (Phase 3):** wakes in the lightest estimated phase within a
  30-min window. Exact alarms need `USE_EXACT_ALARM` (allowed for genuine
  alarm-clock apps) or user-granted `SCHEDULE_EXACT_ALARM` (denied by default
  on new installs since Android 14). Full-screen alarm UI needs
  `USE_FULL_SCREEN_INTENT`, also limited to alarm/call apps — only claim it if
  we ship a real alarm.
* `POST_NOTIFICATIONS` runtime permission (Android 13+), asked *in context*
  ("Want a nudge at 22:30?"), not at first launch.

---

## 4. Animation & character plan

### 4.1 Tools
| Tool | Use for | Why |
|---|---|---|
| **Rive** (`app.rive:rive-android`) | Mascot characters with **state machines** (idle → yawn → sleep → wake → celebrate → sad), reacting to app state | Interactive, tiny files, one file holds every state |
| **Lottie** (`lottie-compose`) | One-off illustrations, onboarding, badges, success bursts | Huge ecosystem, easy hand-off from After Effects designers |
| **Compose Canvas / `AGSL` shaders** | Real-time weather backgrounds: rain streaks scaled to rain volume, lightning flash synced to thunder events, fireflies for night forest, ember sparks for campfire, star field | Reacts to the live mix; no asset weight |
| **AnimatedVectorDrawable** | Notification/launcher-adjacent small icons, splash screen icon | Supported by the system outside the app |

### 4.2 Cast (one consistent art style — commission one illustrator/animator)
* 🐼 **Pip the Panda** — main mascot: bedtime, good-score celebrations
* 🦕 **Dozy the Dino** — focus sessions (wears tiny headphones), medium-risk state
* 🦥 **Slo the Sloth** — "at risk" gentle nudges (never scary, never shaming)
* 🦉 **Hoot the Owl** — night forest, late-night "still awake?" nudges
* 🐨 Koala, 🐱 Cat, 🦊 Fox, 🐰 Bunny — unlockable via sleep streaks (gamified collection)

Each character needs: idle loop, yawn, fall asleep, sleeping (breathing + Zzz),
wake & stretch, celebrate, sad/tired. Keep loops ≤ 6 s, 30 fps, files < 150 KB.

### 4.3 Notifications — what is actually possible
System notifications **cannot play Lottie/Rive**: `RemoteViews` only supports a
limited set of views; small icons must be monochrome.
Plan:
1. **Rich static art:** big-picture / large-icon character art per notification
   type (sleepy panda at bedtime, sunrise dino in the morning) + friendly copy
   ("Pip is getting sleepy… your sleep mix is ready 🌧️").
2. **Tap → animated full-screen "Bedtime moment"** inside the app: the
   character yawns, the screen slowly dims to OLED black, rain starts fading in.
3. **Media notification** with mix name, character artwork and timer
   countdown.
4. **Home-screen widget** (Jetpack Glance): today's score, character mood, one-tap
   favourite mix.
5. Wake alarm (if shipped): full-screen animated alarm activity.

### 4.4 Animation rules
* Night UI: dark, warm, low-contrast colours; avoid bright white flashes after
  bedtime (lightning flash dimmed at night).
* Respect **Remove animations / reduce motion** system setting.
* Auto-dim and pause all UI animation after 30 s of a sleep session
  (battery + light). Audio keeps playing.

### 4.5 Visual direction: "Night Island" (v2, replaces Midnight Meadow)

Interactive concept: <https://claude.ai/artifact/L4TygS6m3ERHeBY1VPrKTh>
(page "Night Island": island mixer, morning summary, sleep mode with Live
Update, visitors; page "First pass" keeps the earlier screens).

**The one big idea:** the sound mixer *is* a living island. Every sound you
turn on appears on it: a cloud and rain for Rain, lightning for Thunder, a
glowing tent, a parked car, a campfire, an owl and fireflies for Night forest,
birds in the tree, a pond with ripples for Water drops, a waterfall for Stream,
drifting wind ribbons for Brown noise, a little radio with floating notes for
Focus music. Pip the panda sleeps in the middle. Good sleep brings new
**visitors** (animals) to live on the island, so building good habits and
building tonight's soundscape share one world.

Why this and not a tile grid: BetterSleep, Calm and most noise apps use the
same grid of sound tiles. A scene you build is ownable, shows the mix at a
glance, and gives the characters a home (see §8).

| Token | Value | Use |
|---|---|---|
| `night` | `#0F1133` | App background |
| `sleepBlack` | `#07081F` | Sleep mode |
| `sheet` | `#1F2350` | Mixer sheet, cards (also the chart surface) |
| `ink` / `inkSoft` / `inkMuted` | `#F3F1FF` / `#D6D4F5` / `#B3B1D9` | Text |
| `lantern` | `#F4B860` | Primary action, selected sounds, timer |
| `moss` / `mossLight` | `#4E8A64` / `#5E9C72` | Island grass |
| `clay` | `#3B2D63` | Island underside |
| `moonlight` | `#F2E3B8` | Moon, score moons |
| Status good / medium / at risk | `#45A87C` / `#BF8426` / `#D6548A` | Always with an icon and a text label. Passed the dataviz palette validator on `#1F2350` (lightness band, CVD ΔE ≥ 8.6, normal-vision ΔE ≥ 16.7, contrast ≥ 3:1). |

* **Type:** Fraunces with the SOFT axis at 100 (a soft, storybook serif) for
  headings and big numbers; Atkinson Hyperlegible for body text, chosen for
  legibility with tired, half-closed eyes at night.
* **Score as a moon:** the moon fills with the score; the week is shown as
  seven moon phases with the hours labelled under each.
* **Motion:** user-triggered and spring-based, following Material 3
  Expressive. Sound buttons morph from circle to rounded square, the timer
  button group stretches the selected option, and the play button morphs and
  turns when playing. Each island object springs into place when you turn its
  sound on. The single orchestrated moment is the new visitor arriving in the
  morning summary. Ambient motion (rain, fire, fireflies) only appears when the
  matching sound is actually playing, so motion always means something.
* **Compose mapping:** `spring(dampingRatio = 0.5f)` with `animateDpAsState`
  for corner morphs, `MaterialShapes` and `Morph` from
  `androidx.graphics.shapes` for expressive shapes, `ButtonGroup` for the
  timer, `AnimatedVisibility(enter = scaleIn(spring()))` for island objects,
  `Canvas` particles for rain, embers and fireflies, `rememberInfiniteTransition`
  for breathing, Rive for characters.

---

## 5. Technical architecture

* **Language/UI:** Kotlin 2.x, Jetpack Compose, Material 3 (dynamic colour off
  at night — custom night palette)
* **Min SDK 26, target latest stable SDK**
* **DI:** Hilt · **Async:** Coroutines/Flow · **Storage:** Room (sessions,
  mixes, scores), DataStore (settings)
* **Audio:** custom `MixerEngine` (AudioTrack + MediaCodec) + Media3
  `MediaSessionService`
* **Health:** Health Connect client, Play services Sleep API
* **Background:** WorkManager (daily score computation, Health Connect sync),
  AlarmManager (reminders)
* **Animation:** Rive, Lottie Compose, Compose Canvas/AGSL
* **Widgets:** Glance
* **Privacy:** all data on device by default; no account needed; export/delete
  in Settings; Health Connect permissions requested only when the user enables it.
* **Assets:** core 10 sounds in the APK (~25–35 MB as Opus);
  extra packs via **Play Asset Delivery** (on-demand).

### Modules
```
:app
:core:designsystem   (theme, night palette, characters, shared composables)
:core:audio          (MixerEngine, NoiseGenerator, OneShotScheduler, MixerService)
:core:data           (Room, DataStore, repositories)
:core:health         (Health Connect + Sleep API adapters)
:feature:mixer       (sound grid, presets, timer)
:feature:sleep       (sleep log, sessions, cycle chart)
:feature:insights    (score, risk level, suggestions, learn cards)
:feature:reminders   (bedtime / wind-down / morning)
:feature:onboarding
```

### Key screens
1. **Home** — character greeting, tonight's suggested mix, score ring, start buttons
   (Focus / Read / Sleep)
2. **Mixer** — sound tiles, sliders, presets, timer chip, live weather background
3. **Now playing (sleep mode)** — huge dim timer, character sleeping, swipe to stop
4. **Sleep log** — calendar + nightly detail (timeline, stages or estimated cycles)
5. **Insights** — score, risk level, trends, suggestions, "why sleep matters"
6. **Reminders** — sleep goal, wake time, wind-down
7. **Settings** — fade-out, Health Connect, sound credits, privacy, data export

---

## 6. Roadmap

| Phase | Scope | Exit criteria |
|---|---|---|
| **0. Sound curation** (1–2 wk, parallel) | Source/record 10 core sounds, master loops, fill `sounds/LICENSES.csv` | Every file CC0/owned/royalty-free; no audible loop on 10× listen |
| **1. MVP mixer** (3–4 wk) | Project scaffold, MixerEngine, 10 sounds + noise, presets, timer + fade, media notification | 8 h playback, screen off, no clicks/gaps, < 3 %/h battery on mid-range phone |
| **2. Sleep basics** (3 wk) | Manual sleep log, Sleep API, bedtime/morning reminders, score + risk level + rule-based tips | Score stable with ≥ 3 nights; disclaimers in place |
| **3. Delight** (3–4 wk) | Rive mascots, weather backgrounds synced to audio, bedtime moment, widgets, streaks & unlockable animals | Animations ≥ 55 fps on mid-range, reduce-motion honoured |
| **4. Depth** | Health Connect stages, smart wake alarm, extra sound packs, focus-music engine, Pomodoro focus mode, sharing mixes | Play health declaration approved |

### Testing checklist
* Loop-point click detector (unit test: max sample delta across loop boundary)
* 8-hour soak test with Doze, battery saver, Bluetooth headphones, phone calls
* Audio-focus scenarios: notification sound, call, another music app
* Score calculator unit tests against hand-made week fixtures
* Accessibility: TalkBack labels on tiles/sliders, contrast, font scaling

---

## 7. Open decisions for the owner
1. Monetisation (free + premium packs/characters? one-time unlock?) — affects
   which licences are acceptable (we already exclude non-commercial).
2. Budget for an illustrator/animator for the mascot set (strongly recommended
   for a coherent look) vs. LottieFiles/Rive community assets (check each
   file's licence).
3. Self-record tent/car rain? (best quality + full ownership).
4. Ship smart wake alarm (requires alarm-clock positioning for exact-alarm and
   full-screen-intent permissions)?

---

## Sources
* Freesound licences & CC0 filter — <https://en.wikipedia.org/wiki/Freesound>, <https://getsoundly.com/faq/how-can-i-use-the-freesound-library>
* Pixabay licence — <https://pixabay.com/service/license/>, <https://pixabay.com/service/terms/>
* Free SFX licence overview (Pixabay, Sonniss) — <https://app.cinevva.com/guides/free-sound-effects-music>
* Sonniss GDC bundle licence — <https://gamefromscratch.com/sonniss-27-5gb-sound-effect-giveaway-at-gdc-2024/>
* Mixkit — <https://mixkit.co/llm-info/>, <https://mixkit.co/free-sound-effects/night/>, <https://mixkit.co/free-sound-effects/thunder/>
* OpenGameArt CC0 loops — <https://opengameart.org/content/rain-loopable>, <https://opengameart.org/content/fireplace-sound-loop>
* BBC RemArc non-commercial restriction — <https://cdm.link/bbc-gives-away-16k-wav-sound-effects-disallows-using/>
* CC0 thunder/rain candidates — <https://market.cloud.edu.tw/resources/web/1780610>
* Pink noise research — <https://www.sleepfoundation.org/sleep-news/can-older-adults-improve-sleep-with-noise>, <https://acibademinternational.com/blog/pink-noise-the-gentler-sound-science-actually-studied-for-sleep/>
* Brown/white noise & ADHD — <https://www.healthline.com/health/adhd/brown-noise-adhd>, <https://superpower.com/guides/brown-noise-focus-sleep-evidence>
* AASM adult sleep duration — <https://aasm.org/advocacy/position-statements/adult-sleep-duration-health-advisory/>, <https://www.aasm.org/resources/pdf/adultsleepdurationconsensus.pdf>
* Sleep regularity & mortality — <https://www.ncbi.nlm.nih.gov/pmc/articles/PMC10782501/>, <https://elifesciences.org/articles/88359>
* Sleep cycles — <https://acibademinternational.com/blog/sleep-stages-the-90-minute-architecture-of-your-night/>
* Health Connect `SleepSessionRecord` — <https://developer.android.com/reference/kotlin/androidx/health/connect/client/records/SleepSessionRecord>
* Sleep API — <https://developers.google.com/location-context/sleep>
* Foreground service types — <https://developer.android.com/about/versions/14/changes/fgs-types-required>
* Exact alarms — <https://developer.android.com/about/versions/14/changes/schedule-exact-alarms>
* Media3 playback — <https://developer.android.com/media/implement/playback-app>
* Lottie vs Rive — <https://callstack.com/blog/lottie-vs-rive-optimizing-mobile-app-animation>, <https://lottiefiles.com/blog/working-with-lottie-animations/getting-started-with-lottie-animations-in-android-app>
* RemoteViews limits — <https://itnext.io/android-custom-notification-in-6-mins-c2e7e2ddadab>

---

## 8. UI/UX trend research (round 2)

| Finding | Source | What we take from it |
|---|---|---|
| Material 3 Expressive (Android 16): spring motion physics, 35 morphable shapes, button groups, emphasised type; used selectively at hero moments | Android Authority, 9to5Google, ProAndroidDev | Spring-morph controls, button-group timer, shape morph on play |
| Android 16 Live Updates: `Notification.ProgressStyle` with segments and points, promoted to lock screen and a status-bar chip | ProAndroidDev, Android Authority | Sleep timer as a Live Update; one segment per estimated 90-min cycle |
| Pokémon Sleep: creature collection tied to sleep; ~10 M downloads; a Japanese study reported users slept 26 min more per night | Sleep Foundation, Sleep Review | "Visitors" collection rewarded by steady bedtimes |
| Finch: a cute pet whose wellbeing mirrors yours drives retention well above meditation apps; living widget; onboarding hatches the pet | Deconstructor of Fun, Appbot, Pratt IxD critique | Pip lives on the island and sleeps when you sleep; home-screen widget shows the island; onboarding starts by naming your island |
| BetterSleep opens straight to audio; Sleep Cycle's morning summary and notes (caffeine, stress); RISE's sleep debt | Sleep Foundation, BetterSleep blog | Island first on open; "Anything different last night?" tags; sleep debt in tips |
| 2026 mobile trends: tactile/clay 3D, functional micro-interactions over decoration | daisyUI trends, vp0 | Soft clay-like island objects; every animation reflects state |

Design review notes (applied from the frontend-design guidance): removed
all-caps labels, middle-dot meta strings and fade-in on every card from the
first pass; one bold idea (the island) with quieter surroundings; sentence
case and action-named buttons; visible focus rings; reduced motion respected.

### Sources (round 2)
* <https://www.androidauthority.com/google-material-3-expressive-features-changes-availability-supported-devices-3556392/>
* <https://9to5google.com/2025/05/13/android-16-material-3-expressive-redesign/>
* <https://proandroiddev.com/live-updates-in-android-16-exploring-the-next-evolution-of-notifications-1a5cf5de2068>
* <https://androidauthority.com/android-16-live-notifications-3518375>
* <https://sleepfoundation.org/sleep-news/can-you-catch-all-your-zzzs-with-pokemon-sleep-app>
* <https://sleepreviewmag.com/curated/pokemon-sleep/>
* <https://www.deconstructoroffun.com/blog/x0hd2ssr80y5n7gv0w967pg7hwd7tl>
* <https://appbot.co/blog/finch-app-reviews-emotional-attachment-user-retention-product-loyalty/>
* <https://ixd.prattsi.org/2026/02/design-critique-finch-self-care-pet-ios-app/>
* <https://www.sleepfoundation.org/best-sleep-apps>
* <https://www.bettersleep.com/blog/sleep-cycle-vs-bettersleep-2026-honest-sleep-app-comparison>
* <https://trends.daisyui.com/ui-design-trends-2026/>
* <https://vp0.com/blogs/mobile-app-ui-design-inspiration-2026>

---

## 9. Round 3: music, animation and 2026 UI research ("Sound Stage", v3)

Concept page "Sound Stage (v3)": <https://claude.ai/artifact/L4TygS6m3ERHeBY1VPrKTh>

### What other apps do

| App / trend | What stands out | What we take |
|---|---|---|
| **Endel** | Every soundscape has its own generative visual; calm sound plays during onboarding so you feel the product before signing up | Background colours come from the sounds you are playing; sound starts on the first screen |
| **Portal** | Immersive 3D spatial soundscapes of real places; Webby-winning design, App Store Award finalist | **Sound Stage**: drag each sound around the listener to set volume (distance) and left/right position (pan) |
| **Spotify Wrapped 2025** | Built with Rive; bold kinetic type, "visual mixtape", shareable story cards | **Last night** recap as tap-through story cards with dropping numerals and a share button |
| **Duolingo** | Rive state-machine characters (idle, happy, sad, talking) driven by app inputs | Characters as Rive state machines with inputs such as `mood`, `beat` and `sleepiness` |
| **Headspace / Calm** | Slow easing and long transitions that physically calm the user | Wind down screen uses a slow 19-second breathing cycle; no fast motion after bedtime |
| **Tiimo** (2025 iPhone App of the Year) | Visual, accessible routines | Simple visual session dots for focus sessions |
| **Material 3 Expressive** | Spring physics, shape morphing, button groups, hero moments | Morphing play buttons, stretching timer group, morphing breathing blob |
| **Apple Liquid Glass** / glassmorphism 2.0 | Translucent layers that refract colour behind them | Frosted glass controls and tab bar floating over moving colour |
| **Aurora / mesh gradients** | Soft moving colour fields | Background blobs coloured by the active sounds; in Compose, `Brush` mesh gradients or AGSL shaders |
| **Kinetic typography** | Animated letterforms as the main visual | Headlines that blur and rise in word by word; rolling-digit timers |
| **Rich haptics** (`VibrationEffect.Composition`) | Precise ticks and slow rises on modern vibration motors | A soft tick when a sound snaps into place; a slow-rise buzz to end a focus session; check `arePrimitivesSupported()` first |
| **Nothing OS Glyph Matrix** | Dot-matrix visuals for notifications | Optional later: show the sleep timer on Glyph-capable phones |

### v3 screens
1. **Sound stage:** sounds are glowing bubbles that pulse at their own rhythm. Drag a bubble toward Pip (wearing headphones) to make it louder, or left and right to place it. Arrow keys move the focused bubble too. A live caption says what is close and what is far. The background colour changes with the sounds on stage.
2. **Focus flow:** a rolling-digit 25-minute timer, a ring of bars and rings pulsing at the track's tempo (72 BPM), Dozy nodding to the beat, a glass music card with track changes, and session dots (2 of 4).
3. **Wind down:** a colourful blob that morphs and grows with 4-7-8 breathing, with the phase and count in the centre, plus a "Lights down" slider that dims the screen and a button to start sleep mode.
4. **Last night:** four auto-advancing story cards: hours slept with dropping numerals; time to fall asleep with rain; a week of rising moons; and the new visitor reveal with Share.

### Build notes (Android)
* Spatial mix: map distance to gain (`1 − d/R`) and x to pan in the `MixerEngine`. Use `pointerInput { detectDragGestures }` and `Modifier.offset` with `animateOffsetAsState(spring())`. Expose bubble positions to TalkBack with custom accessibility actions ("Move closer", "Move left").
* Beat sync: the engine publishes the track's BPM and beat phase as a `StateFlow`, so the UI pulses on real beats, not a guessed CSS timer.
* Audio-reactive visuals: read the mixer's output levels (RMS per layer) every frame and feed them into the bubble glow and pulse size. Avoid `Visualizer` (it needs the microphone permission); the engine already has the samples.
* Shaders: AGSL `RuntimeShader` for aurora and the breathing blob on API 33+, with a static gradient fallback below that.
* Story cards: `HorizontalPager` with `graphicsLayer` transitions and an auto-advance timer. Share renders a 1080×1920 bitmap of the card.
* Motion safety: respect "Remove animations"; never flash bright colour after bedtime; stop ambient animation 30 seconds into sleep mode.

### Sources (round 3)
* Endel design: <https://blog.readymag.com/tune-in-drop-out-how-endel-helps-get-into-the-flow-via-immersive-soundscapes-5ab08d932687/>, <https://screensdesign.com/showcase/endel-focus-sleep-sounds>
* Portal: <https://apps.apple.com/us/app/portal-sleep-focus-escape/id1436994560>, <https://portal.app/campfire/mac-app-of-the-year-finalist>
* Spotify Wrapped 2025 with Rive: <https://rive.app/blog/spotify-used-rive-for-spotify-wrapped-2025>, <https://newsroom.spotify.com/?p=37865>
* Duolingo and Rive: <https://rive.app/blog/duolingo-s-ai-powered-video-call-brings-lily-to-life>, <https://blog.duolingo.com/world-character-visemes>
* Headspace motion: <https://blakecrosley.com/guides/design/headspace>
* App Store Awards 2025 (Tiimo): <https://www.apple.com/sg/newsroom/2025/12/apple-unveils-the-winners-of-the-2025-app-store-awards/>
* Liquid Glass: <https://en.wikipedia.org/wiki/Liquid_Glass>
* Mesh gradients and AGSL in Compose: <https://proandroiddev.com/mesh-gradients-in-jetpack-compose-a8a6795eb8ee>, <https://github.com/AndreFrelicot/paper-shaders-android>, <https://developer.android.com/guide/topics/graphics/agsl/using-agsl>
* 2026 UI trends: <https://trends.daisyui.com/>, <https://line25.com/articles/web-design-trends-2026/>
* Haptics: <https://developer.android.com/develop/ui/views/haptics/custom-haptic-effects>
* Nothing Glyph Matrix: <https://design-milk.com/the-nothing-phone-3s-glyph-matrix-turns-notifications-into-pixel-art/>
