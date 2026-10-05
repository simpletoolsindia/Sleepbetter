# Dino art

Source for the illustrated dinos. `gen.py` holds every layer's path (body, belly, thighs, arms, tail, head) for the sitting and walking poses and writes:

- `app/src/main/java/com/sleepbetter/app/ui/components/DinoPaths.kt` (copy `DinoPaths.kt` there after running), and
- SVG/HTML previews via `front.py` and `side.py`, with CSS animations that match the app's.

Eyes, mouth, accessories and colours are drawn in `DinoArt.kt`, so they can react to mood, sleep and species. Original artwork in a common cute-dino style; no reference images are included.
