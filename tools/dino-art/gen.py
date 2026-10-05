# Single source for the dino artwork: emits SVG previews and the Kotlin rig.
import math
from front import spike

def circle(cx, cy, r): return f"M{cx-r} {cy} a{r} {r} 0 1 0 {2*r} 0 a{r} {r} 0 1 0 {-2*r} 0 Z"
def ellipse(cx, cy, rx, ry): return f"M{cx-rx} {cy} a{rx} {ry} 0 1 0 {2*rx} 0 a{rx} {ry} 0 1 0 {-2*rx} 0 Z"
def rot_ellipse(cx, cy, rx, ry, deg):
    a = math.radians(deg); pts = []
    for i in range(24):
        t = 2 * math.pi * i / 24
        x, y = rx * math.cos(t), ry * math.sin(t)
        pts.append((cx + x * math.cos(a) - y * math.sin(a), cy + x * math.sin(a) + y * math.cos(a)))
    return "M" + " L".join(f"{x:.1f} {y:.1f}" for x, y in pts) + " Z"

# part = (group, path, fill, stroke, width, alpha)
L = "LINE"
def P(g, d, fill=None, stroke=None, w=0, a=1.0): return (g, d, fill, stroke, w, a)

HEAD = "M100 28 C146 28 166 58 162 86 C158 112 132 126 100 126 C68 126 42 112 38 86 C34 58 54 28 100 28 Z"
FRONT = [
    P("STATIC", ellipse(100, 190, 60, 7), "SHADOW"),
    *[P("TAIL", spike(*a), "PLATE", L, 4) for a in [(158,152,163,134,9),(172,145,181,126,8),(183,134,195,119,7)]],
    P("TAIL", "M128 170 C160 178 184 166 192 134 C194 126 186 124 182 132 C174 150 158 154 134 150 Z", "BODY", L, 4),
    *[P("STATIC", spike(*a), "PLATE", L, 4) for a in [(138,122,154,110,10),(146,140,162,132,9)]],
    P("TORSO", "M72 116 C50 134 48 182 78 188 L124 188 C154 182 150 134 128 116 Z", "BODY", L, 4),
    P("TORSO", "M128 120 C146 136 150 170 132 186 C142 166 140 138 124 124 Z", "SHADE"),
    P("TORSO", "M86 124 C74 140 74 178 90 184 L110 184 C126 178 126 140 114 124 Z", "BELLY", L, 3),
    *[P("TORSO", f"M{100-w} {y} Q100 {y+3} {100+w} {y}", None, "STRIPE", 3) for y, w in [(140,11),(152,15),(164,15),(175,11)]],
    P("TORSO", circle(64,146,4.5), "SPOT"), P("TORSO", circle(70,160,3), "SPOT"), P("TORSO", circle(140,152,4), "SPOT"),
    P("STATIC", "M54 170 C50 152 74 146 86 160 C92 172 88 186 76 188 L60 188 C52 188 50 180 54 170 Z", "BODY", L, 4),
    P("STATIC", "M146 170 C150 152 126 146 114 160 C108 172 112 186 124 188 L140 188 C148 188 150 180 146 170 Z", "BODY", L, 4),
    *[P("STATIC", circle(x, y, 4), "WHITE", L, 2.5) for x, y in [(62,186),(71,188),(80,186),(120,186),(129,188),(138,186)]],
    P("ARM_L", "M82 124 C70 128 68 146 76 152 C82 156 88 150 86 142 C86 134 90 128 82 124 Z", "BODY", L, 4),
    P("ARM_L", circle(75,151,2.6), "WHITE", L, 1.6), P("ARM_L", circle(81,154,2.6), "WHITE", L, 1.6),
    P("ARM_R", "M118 124 C130 128 132 146 124 152 C118 156 112 150 114 142 C114 134 110 128 118 124 Z", "BODY", L, 4),
    P("ARM_R", circle(125,151,2.6), "WHITE", L, 1.6), P("ARM_R", circle(119,154,2.6), "WHITE", L, 1.6),
    *[P("HEAD", spike(*a), "PLATE", L, 4) for a in [(106,33,112,12,11),(127,38,140,20,11),(145,52,162,40,10)]],
    P("HEAD", HEAD, "BODY"),
    P("HEAD", "M42 96 C54 118 78 126 100 126 C122 126 146 118 158 96 C144 112 124 119 100 119 C76 119 56 112 42 96 Z", "SHADE"),
    P("HEAD", HEAD, None, L, 4),
    P("HEAD", rot_ellipse(66, 52, 13, 6, -28), "WHITE", None, 0, 0.55),
    P("HEAD", circle(84,40,3), "WHITE", None, 0, 0.55),
    P("HEAD", circle(134,50,4.5), "SPOT"), P("HEAD", circle(145,64,3), "SPOT"), P("HEAD", circle(124,40,2.5), "SPOT"),
    P("HEAD", circle(58,100,8), "CHEEK", None, 0, 0.85), P("HEAD", circle(142,100,8), "CHEEK", None, 0, 0.85),
]

SHEAD = "M62 38 C74 18 114 16 130 34 C142 48 140 74 126 84 C116 92 100 94 86 92 C70 94 42 92 34 76 C28 62 42 50 62 50 Z"
SIDE = [
    P("STATIC", ellipse(104, 186, 62, 7), "SHADOW"),
    P("LEG_BACK", "M118 146 C114 134 136 130 140 146 L141 176 C141 184 121 184 120 176 Z", "SHADE", L, 4),
    *[P("LEG_BACK", circle(x, 182, 3.6), "WHITE", L, 2.2) for x in [124, 132, 139]],
    *[P("TAIL", spike(*a), "PLATE", L, 4) for a in [(122,26,130,8,10),(135,38,154,22,10),(140,58,162,48,10),(146,96,168,84,10),(152,118,174,112,10),(162,150,170,132,9),(180,142,190,126,8)]],
    P("TAIL", "M136 146 C164 154 186 146 198 120 C196 146 176 172 134 172 Z", "BODY", L, 4),
    P("BODY", "M86 82 C64 100 64 150 80 172 L130 172 C154 168 160 136 150 110 C144 94 130 84 116 82 Z", "BODY", L, 4),
    P("BODY", "M80 100 C68 120 70 156 84 168 L106 168 C96 150 94 120 100 96 Z", "BELLY"),
    P("BODY", "M78 116 Q88 118 97 114 M75 132 Q86 135 96 131 M77 148 Q88 151 98 147", None, "STRIPE", 3),
    P("BODY", circle(132,120,4.5), "SPOT"), P("BODY", circle(140,138,3), "SPOT"),
    P("LEG_FRONT", "M88 146 C84 132 108 128 112 146 L112 178 C112 186 90 186 89 178 Z", "BODY", L, 4),
    *[P("LEG_FRONT", circle(x, 184, 3.6), "WHITE", L, 2.2) for x in [94, 102, 109]],
    P("ARM", "M90 108 C76 108 66 118 68 126 C70 132 78 130 80 124 C84 118 92 116 96 114 Z", "BODY", L, 4),
    P("ARM", circle(68,128,2.6), "WHITE", L, 1.6),
    P("HEAD", SHEAD, "BODY"),
    P("HEAD", "M36 80 C48 92 72 94 88 92 C104 94 118 90 128 82 C116 88 100 88 88 86 C70 88 48 88 36 80 Z", "SHADE"),
    P("HEAD", SHEAD, None, L, 4),
    P("HEAD", rot_ellipse(80, 30, 11, 5, -18), "WHITE", None, 0, 0.55),
    P("HEAD", circle(118,40,4), "SPOT"), P("HEAD", circle(126,56,3), "SPOT"),
    P("HEAD", circle(46,60,2.2), "LINE"),
    P("HEAD", circle(104,74,7), "CHEEK", None, 0, 0.85),
    P("HEAD", "M44 74 Q66 86 88 74", None, L, 4),
]

def kotlin():
    def part(p):
        g, d, f, s, w, a = p
        fs = f"DinoInk.{f}" if f else "null"
        ss = f"DinoInk.{s}" if s else "null"
        return f'        DinoPart(DinoGroup.{g}, "{d}", {fs}, {ss}, {w}f, {a}f),'
    front = "\n".join(part(p) for p in FRONT)
    side = "\n".join(part(p) for p in SIDE)
    return f'''// Generated by the dino art script from the same paths as the SVG previews. Edit the script, not this file.
package com.sleepbetter.app.ui.components

internal object DinoPaths {{
    /** Sitting, facing the viewer. 200 x 200 artboard. */
    val front = listOf(
{front}
    )

    /** Walking, facing left. 210 x 200 artboard. */
    val side = listOf(
{side}
    )
}}
'''
open("DinoPaths.kt", "w").write(kotlin())
print(len(FRONT), len(SIDE))
