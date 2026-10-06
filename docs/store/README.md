# Google Play listing assets

| File | Play Console field | Spec |
|---|---|---|
| `icon-512.png` | App icon | 512 × 512 PNG, full bleed (Play rounds the corners) |
| `feature-graphic.png` | Feature graphic | 1024 × 500 PNG |
| `phone-1.png` … `phone-8.png` | Phone screenshots | 1080 × 1920 (9:16), in this order |

Regenerate after UI changes from the CI screenshots zip:

```
unzip SleepBetter-screenshots.zip -d /tmp/shots
node tools/store/make.js /tmp/shots docs/store <dir with outfit-400..800.ttf>
```
