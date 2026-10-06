"""
The SleepBetter app icon: Pico the baby dino tucked into a crescent-moon
cradle, asleep under the stars. One list of shapes is written out as the
adaptive icon's vector drawables (background, foreground, monochrome) and as
an SVG preview, so they can never drift apart.

Usage: python3 tools/icon/gen.py   (from the repo root)
Viewport is 108 x 108 (adaptive icon). Launchers mask the outer 18 units on
each side, so the foreground keeps inside the 66-unit safe circle.
"""
import math, os

OUT_RES = "app/src/main/res/drawable"
OUT_SVG = "docs/icon/icon.svg"

INK = "#FF3A2622"

# The foreground is drawn a little larger around the cradle's centre, to fill the safe zone.
FG_SCALE, FG_PIVOT = 1.12, (54, 54)

# Moon cradle: a bowl-shaped crescent (outer circle minus an inner one above it).
CX, CY, R_OUT = 54, 56, 27
CY_IN, R_IN = 47.5, 25


def horns():
    # Intersection of the two circles (same x centre).
    y = ((CY ** 2 - CY_IN ** 2) - (R_OUT ** 2 - R_IN ** 2)) / (2 * (CY - CY_IN))
    dx = math.sqrt(R_OUT ** 2 - (y - CY) ** 2)
    return (CX - dx, y), (CX + dx, y)


(LX, LY), (RX, RY) = horns()
CRADLE = (f"M{LX:.2f},{LY:.2f} A{R_OUT},{R_OUT} 0 1,0 {RX:.2f},{RY:.2f} "
          f"A{R_IN},{R_IN} 0 1,1 {LX:.2f},{LY:.2f} Z")


def star(x, y, r):
    """A soft four-point sparkle."""
    k = r * 0.18
    return (f"M{x},{y - r} Q{x + k},{y - k} {x + r},{y} Q{x + k},{y + k} {x},{y + r} "
            f"Q{x - k},{y + k} {x - r},{y} Q{x - k},{y - k} {x},{y - r} Z")


def ellipse(cx, cy, rx, ry):
    return f"M{cx - rx},{cy} a{rx},{ry} 0 1,0 {2 * rx},0 a{rx},{ry} 0 1,0 {-2 * rx},0 Z"


def z(x, y, s):
    """A hand-drawn 'z' as a stroke."""
    return f"M{x},{y} L{x + s},{y} L{x},{y + s} L{x + s},{y + s}"


# Each shape: (layer, path, fill, stroke, stroke width, extra)
# fill may be ("linear", x1, y1, x2, y2, [(offset, color)...]) or ("radial", cx, cy, r, [...]).
SHAPES = [
    # Night sky.
    ("bg", "M0,0h108v108h-108z", ("linear", 20, 0, 88, 108, [(0, "#FF1E1850"), (0.55, "#FF3B2A93"), (1, "#FF7A5BEA")]), None, 0),
    # Moonglow behind the cradle.
    ("bg", ellipse(54, 58, 40, 40), ("radial", 54, 58, 40, [(0, "#66FFE6A3"), (0.55, "#22FFE6A3"), (1, "#00FFE6A3")]), None, 0),
    # Far stars (may be cropped by the mask; that's fine).
    ("bg", star(22, 24, 2.6), "#CCFFFFFF", None, 0),
    ("bg", star(86, 80, 2.2), "#99FFFFFF", None, 0),
    ("bg", star(16, 70, 1.8), "#80FFFFFF", None, 0),
    ("bg", star(90, 20, 1.8), "#80FFFFFF", None, 0),

    # Pico's back plates, peeking over the head.
    ("fg", "M40,40 Q41,30.5 45.5,30.5 Q50,30.5 51,40 Z", "#FFFFD98A", INK, 1.8),
    ("fg", "M48.5,37 Q49.5,25 54,25 Q58.5,25 59.5,37 Z", "#FFFFD98A", INK, 1.8),
    ("fg", "M57,40 Q58,30.5 62.5,30.5 Q67,30.5 68,40 Z", "#FFFFD98A", INK, 1.8),
    # Head, tucked into the cradle: shade, face, outline.
    ("fg", "M33,53 C33,40 42.5,34.5 54,34.5 C65.5,34.5 75,40 75,53 C75,62 66,66 54,66 C42,66 33,62 33,53 Z", "#FF6DBB55", None, 0),
    ("fg", "M34,51.8 C34,40.5 43,35.3 54,35.3 C65,35.3 74,40.5 74,51.8 C74,58.5 66,62 54,62 C42,62 34,58.5 34,51.8 Z", "#FF8BD46E", None, 0),
    ("fg", "M33,53 C33,40 42.5,34.5 54,34.5 C65.5,34.5 75,40 75,53 C75,62 66,66 54,66 C42,66 33,62 33,53 Z", None, INK, 2.0),
    # Shine on the head.
    ("fg", ellipse(45, 41.5, 4.2, 2.2), "#66FFFFFF", None, 0),
    # Sleeping eyes, blush, tiny smile.
    ("fg", "M42.5,49.5 Q46,52.6 49.5,49.5 M58.5,49.5 Q62,52.6 65.5,49.5", None, INK, 2.2),
    ("fg", ellipse(40.5, 55, 3, 2.1), "#B3FF9AA8", None, 0),
    ("fg", ellipse(67.5, 55, 3, 2.1), "#B3FF9AA8", None, 0),
    ("fg", "M51.5,55.5 Q54,57.6 56.5,55.5", None, INK, 1.8),

    # The moon cradle in front: body, a lighter rim, outline.
    ("fg", CRADLE, ("linear", 30, 50, 80, 86, [(0, "#FFFFF1C4"), (1, "#FFFFCF6B")]), None, 0),
    ("fg", CRADLE, None, "#FF8A5A1E", 1.6),
    # Craters.
    ("fg", ellipse(44, 74, 2.6, 1.8), "#33B9772A", None, 0),
    ("fg", ellipse(62.5, 77.5, 3.4, 2.2), "#33B9772A", None, 0),
    ("fg", ellipse(54, 70.5, 1.6, 1.1), "#33B9772A", None, 0),
    # Little hands resting on the rim.
    ("fg", ellipse(43, 64.6, 4, 2.9), "#FF8BD46E", INK, 1.6),
    ("fg", ellipse(65, 64.6, 4, 2.9), "#FF8BD46E", INK, 1.6),

    # Zzz drifting up.
    ("fg", z(71, 29, 4.6), None, "#FFFFFFFF", 1.7),
    ("fg", z(77.5, 21.5, 3.4), None, "#CCFFFFFF", 1.4),
    # A near sparkle.
    ("fg", star(30, 31, 2.8), "#FFFFF1C4", None, 0),
]

# Themed (Android 13+) icon: one colour, the head and cradle as solid shapes with the eyes cut out.
MONO = [
    (CRADLE, "#FFFFFFFF"),
    ("M34,51.8 C34,40.5 43,35.3 54,35.3 C65,35.3 74,40.5 74,51.8 C74,58.5 66,62 54,62 C42,62 34,58.5 34,51.8 Z "
     "M42.5,49 Q46,52.4 49.5,49 L49.5,50.6 Q46,54.4 42.5,50.6 Z M58.5,49 Q62,52.4 65.5,49 L65.5,50.6 Q62,54.4 58.5,50.6 Z", "#FFFFFFFF"),
    ("M48.5,37 Q49.5,25 54,25 Q58.5,25 59.5,37 Z", "#FFFFFFFF"),
]


def argb_to_svg(c):
    a = int(c[1:3], 16) / 255
    return f"#{c[3:]}", a


def xml_fill(fill):
    if isinstance(fill, tuple):
        kind = fill[0]
        stops = "".join(f'\n                <item android:offset="{o}" android:color="{c}" />' for o, c in fill[-1])
        if kind == "linear":
            _, x1, y1, x2, y2, _ = fill
            g = (f'<gradient android:type="linear" android:startX="{x1}" android:startY="{y1}" '
                 f'android:endX="{x2}" android:endY="{y2}">{stops}\n            </gradient>')
        else:
            _, cx, cy, r, _ = fill
            g = (f'<gradient android:type="radial" android:centerX="{cx}" android:centerY="{cy}" '
                 f'android:gradientRadius="{r}">{stops}\n            </gradient>')
        return f'\n        <aapt:attr name="android:fillColor">\n            {g}\n        </aapt:attr>'
    return None


def vector(shapes, comment, scale=1.0):
    out = ['<?xml version="1.0" encoding="utf-8"?>', f"<!-- {comment} Generated by tools/icon/gen.py; edit that, not this. -->",
           '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
           '    xmlns:aapt="http://schemas.android.com/aapt"',
           '    android:width="108dp"', '    android:height="108dp"',
           '    android:viewportWidth="108"', '    android:viewportHeight="108">']
    for path, fill, stroke, width in shapes:
        attrs = [f'android:pathData="{path}"']
        child = None
        if fill is not None:
            child = xml_fill(fill)
            if child is None:
                attrs.append(f'android:fillColor="{fill}"')
        if stroke:
            attrs += [f'android:strokeColor="{stroke}"', f'android:strokeWidth="{width}"',
                      'android:strokeLineCap="round"', 'android:strokeLineJoin="round"']
        a = "\n        ".join(attrs)
        out.append(f"    <path\n        {a}>{child}\n    </path>" if child else f"    <path\n        {a} />")
    if scale != 1.0:
        head, paths = out[:8], out[8:]
        indented = ["\n".join("    " + line for line in p.split("\n")) for p in paths]
        out = head + [f'    <group android:scaleX="{scale}" android:scaleY="{scale}" android:pivotX="{FG_PIVOT[0]}" android:pivotY="{FG_PIVOT[1]}">'] + indented + ["    </group>"]
    out.append("</vector>\n")
    return "\n".join(out)


def svg(layers, size=512, mask=True):
    defs, body = [], []
    shapes = [(p, f, st, w, sc) for sc, group in layers for p, f, st, w in group]
    for i, (path, fill, stroke, width, scale) in enumerate(shapes):
        style = []
        if isinstance(fill, tuple):
            gid = f"g{i}"
            stops = "".join(f'<stop offset="{o}" stop-color="{argb_to_svg(c)[0]}" stop-opacity="{argb_to_svg(c)[1]:.2f}"/>' for o, c in fill[-1])
            if fill[0] == "linear":
                _, x1, y1, x2, y2, _ = fill
                defs.append(f'<linearGradient id="{gid}" gradientUnits="userSpaceOnUse" x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}">{stops}</linearGradient>')
            else:
                _, cx, cy, r, _ = fill
                defs.append(f'<radialGradient id="{gid}" gradientUnits="userSpaceOnUse" cx="{cx}" cy="{cy}" r="{r}">{stops}</radialGradient>')
            style.append(f'fill="url(#{gid})"')
        elif fill:
            c, a = argb_to_svg(fill)
            style.append(f'fill="{c}" fill-opacity="{a:.2f}"')
        else:
            style.append('fill="none"')
        if stroke:
            c, a = argb_to_svg(stroke)
            style.append(f'stroke="{c}" stroke-opacity="{a:.2f}" stroke-width="{width}" stroke-linecap="round" stroke-linejoin="round"')
        px, py = FG_PIVOT
        tf = f' transform="translate({px} {py}) scale({scale}) translate({-px} {-py})"' if scale != 1.0 else ""
        body.append(f'<path d="{path}" {" ".join(style)}{tf}/>')
    clip = '<clipPath id="m"><rect x="0" y="0" width="108" height="108" rx="30"/></clipPath>' if mask else ""
    group = 'clip-path="url(#m)"' if mask else ""
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" viewBox="0 0 108 108">'
            f'<defs>{clip}{"".join(defs)}</defs><g {group}>{"".join(body)}</g></svg>\n')


def main():
    bg = [(p, f, s, w) for layer, p, f, s, w in SHAPES if layer == "bg"]
    fg = [(p, f, s, w) for layer, p, f, s, w in SHAPES if layer == "fg"]
    os.makedirs(OUT_RES, exist_ok=True)
    open(f"{OUT_RES}/ic_launcher_background.xml", "w").write(vector(bg, "Night sky with moonglow and stars."))
    open(f"{OUT_RES}/ic_launcher_foreground.xml", "w").write(vector(fg, "Pico asleep in a crescent-moon cradle, inside the 66 dp safe zone.", FG_SCALE))
    open(f"{OUT_RES}/ic_launcher_monochrome.xml", "w").write(
        vector([(p, c, None, 0) for p, c in MONO], "Single-colour version for themed icons (Android 13+).", FG_SCALE))
    os.makedirs(os.path.dirname(OUT_SVG), exist_ok=True)
    open(OUT_SVG, "w").write(svg([(1.0, bg), (FG_SCALE, fg)]))


if __name__ == "__main__":
    main()
