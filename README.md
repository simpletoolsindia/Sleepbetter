# SleepBetter

Android app (Kotlin + Jetpack Compose) for focus, reading and sleep. It mixes natural sounds that never loop, plays them on a timer, tracks sleep, sends bedtime reminders, shows a sleep-health level with tips, and has a cast of sleepy animals that move onto your island as your bedtimes get steadier.

The design mixes two concepts (see the [UI concept](https://claude.ai/artifact/L4TygS6m3ERHeBY1VPrKTh)):

- **Night Island:** the mix shown as a living scene. Each sound appears on a floating island: rain clouds, lightning timed to the real thunder, a tent, a car, a campfire, an owl and fireflies, a pond, a waterfall.
- **Sound Stage:** the same mix as glowing bubbles you drag closer (louder) or to the left or right (stereo position).

## Screens

| Screen | What it does |
|---|---|
| Tonight | Switch between the Island and Stage views of one mix. Sound chips, ready-made mixes, a timer (30 m to all night) and a morphing play button |
| Focus | 25-minute sessions with generated focus music. A rolling-digit timer, rings and Dozy the dinosaur pulse on the music's real beat |
| Wind down | 4-7-8 breathing blob, a "Lights down" dimmer, bedtime and reminder settings, and "Start sleep mode" |
| Sleep mode | Near-black countdown, a dimmed island and estimated 90-minute cycles; dims further after 30 s. "I'm awake" logs the night |
| Last night | Story cards in the style of Spotify Wrapped: hours slept, bedtime, the week as moon phases, a morning check-in, and your level (Good / Medium risk / At risk) with a tip |
| Visitors | The collection: Mochi the panda and Toffee the capybara (from day one), Ember the fox, Pebble the elephant, Hoot, Dozy and more, unlocked by steady bedtimes |

## Colour themes

Tap the palette button on Home to switch the whole app, including the dusk scene, between five themes:

| Theme | Idea |
|---|---|
| Moon milk | The original soft lavender |
| Jade mist | Pinterest 2026 "Jade" and calm mint-to-moss greens; greens and blues are linked with relaxation |
| Cool blue | Pinterest 2026 "Cool Blue" on Pantone 2026 "Cloud Dancer" white |
| Plum noir | Pinterest 2026 "Plum Noir" with a persimmon glow at dusk |
| Amber hour | Warm candlelight tones; warm, low-blue light is the gentlest on melatonin at night |

The status colours (green good, yellow medium, peach at risk) never change, so levels read the same in every theme.

## Mixes and sharing

- **20 ready-made mixes** in five groups (Rain, Storms, Nature, Cozy, Focus): Light rain, Heavy rain, Rain drops only, Heavy rain and thunder, Thunderstorm, Forest rain and thunder, Forest and thunder, Distant storm, Rain on the tent, Rain on the car, Campfire in the rain, Deep focus and more. Tap one to play it.
- **Your own mixes:** pick sounds, drag them closer or further away on the stage, then "Save this mix". Saved mixes can be played, shared or deleted.
- **Share with a friend:** Quick Share (Wi-Fi Direct, nearby phones), Bluetooth, any other app, or copy a link. A mix travels as a tiny text file holding a `sleepbetter://mix/...` link (name, sounds and their places; about 60 characters). No account or internet needed.
- **Receive:** open the file or link with SleepBetter, share text to it, or paste a link under "Add from a friend". Incoming mixes are checked and cleaned before you add them.

## Project layout

```
core/   Pure Kotlin, no Android: the sound engine and the sleep logic (unit-tested)
  audio/  DSP, procedural nature sounds, spatial mixer, sleep timer
  sleep/  sessions, sleep score and risk level, tips, visitor unlocks
  mix/    mixes, the 20 templates, the share code (MixCodec)
app/    The Android app
  audio/      AudioEngine (AudioTrack thread), PlaybackService (foreground, mediaPlayback)
  data/       SleepRepository (on-device only)
  reminders/  bedtime reminders (inexact alarms, no special permission)
  share/      sending mixes (Quick Share, Bluetooth, any app) and reading received ones
  ui/         Compose screens, characters, island scene, sound stage
```

## The sounds

Every sound is generated in code (`core/.../NatureSources.kt`): rain on leaves, a tent and a car, thunder, campfire, crickets and an owl, birds, cave drips, a stream, brown noise and generative focus music.

- **No loop points.** The sounds never repeat, so there is no seam to hear.
- **No licences or app size.** Nothing is downloaded or bundled.
- **Matched loudness.** Each sound is calibrated and checked by `SoundCalibrationTest`.

Real field recordings will sound more natural. The research plan lists licence-safe sources (`docs/RESEARCH_AND_PLAN.md`). Any recording can replace a generator by implementing `SoundSource`. Record every file in `sounds/LICENSES.csv`.

## Build and run

1. Open the folder in Android Studio (Narwhal or newer) and let it sync. It downloads the Android SDK 36 and the libraries from Google Maven.
2. Run the `app` configuration on a device or emulator (Android 8.0 / API 26+).

If Android Studio suggests newer versions for the libraries in `gradle/libs.versions.toml`, they are safe to accept.

The `core` module also builds on any machine with a JDK, without the Android SDK:

```
cd core && gradle test
```

## Fonts

Outfit is bundled under the SIL Open Font License; see `licenses/`.

## Privacy

Sleep data stays on the device in app storage. There is no account and no network access.

## Docs

- Research and product plan: [docs/RESEARCH_AND_PLAN.md](docs/RESEARCH_AND_PLAN.md)
- Sound licence register: [sounds/LICENSES.csv](sounds/LICENSES.csv)
