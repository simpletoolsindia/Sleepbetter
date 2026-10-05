# Generates the front-facing (sitting) dino rig as SVG, one palette per species.
def spike(x, y, tx, ty, w=7):
    # A fat, rounded plate: wide base, soft sides, blunt tip.
    mx, my = (x + tx) / 2, (y + ty) / 2
    return (f"M{x-w} {y} C{x-w} {my} {tx-w*0.45} {ty+1} {tx} {ty} "
            f"C{tx+w*0.45} {ty+1} {x+w} {my} {x+w} {y} Z")

PALETTES = {
    "pico": dict(body="#8BD46E", shade="#6DBB55", spot="#5DA847", belly="#FFE9A8", stripe="#EFC970", plate="#FFC94D", cheek="#FF9F6B"),
    "lulu": dict(body="#FFB8CF", shade="#F296B4", spot="#E77FA2", belly="#FFF1F6", stripe="#F7C9D9", plate="#C7B2FA", cheek="#FF7FA0"),
}
INK = "#2A2440"

def front(p, wave=True, tag=""):
    s = lambda: f'stroke="{INK}" stroke-width="4" stroke-linejoin="round" stroke-linecap="round"'
    tail_spikes = "".join(f'<path d="{spike(*a)}" fill="{p["plate"]}" {s()}/>' for a in [(158,152,163,134,9),(172,145,181,126,8),(183,134,195,119,7)])
    back_spikes = "".join(f'<path d="{spike(*a)}" fill="{p["plate"]}" {s()}/>' for a in [(138,122,154,110,10),(146,140,162,132,9)])
    head_spikes = "".join(f'<path d="{spike(*a)}" fill="{p["plate"]}" {s()}/>' for a in [(106,33,112,12,11),(127,38,140,20,11),(145,52,162,40,10)])
    toes = "".join(f'<circle cx="{x}" cy="{y}" r="4" fill="#FFFFFF" {s().replace("4","2.5",1)}/>' for x,y in [(62,186),(71,188),(80,186),(120,186),(129,188),(138,186)])
    stripes = "".join(f'<path d="M{100-w} {y} Q100 {y+3} {100+w} {y}" fill="none" stroke="{p["stripe"]}" stroke-width="3" stroke-linecap="round"/>' for y,w in [(140,11),(152,15),(164,15),(175,11)])
    return f'''
<g class="dino {tag}">
  <ellipse cx="100" cy="190" rx="60" ry="7" fill="{INK}" opacity="0.15"/>
  <g class="tail">{tail_spikes}
    <path d="M128 170 C160 178 184 166 192 134 C194 126 186 124 182 132 C174 150 158 154 134 150 Z" fill="{p["body"]}" {s()}/>
  </g>
  {back_spikes}
  <g class="torso">
    <path d="M72 116 C50 134 48 182 78 188 L124 188 C154 182 150 134 128 116 Z" fill="{p["body"]}" {s()}/>
    <path d="M128 120 C146 136 150 170 132 186 C142 166 140 138 124 124 Z" fill="{p["shade"]}"/>
    <path d="M86 124 C74 140 74 178 90 184 L110 184 C126 178 126 140 114 124 Z" fill="{p["belly"]}" {s().replace('width="4"','width="3"')}/>
    {stripes}
    <circle cx="64" cy="146" r="4.5" fill="{p["spot"]}"/><circle cx="70" cy="160" r="3" fill="{p["spot"]}"/><circle cx="140" cy="152" r="4" fill="{p["spot"]}"/>
  </g>
  <path d="M54 170 C50 152 74 146 86 160 C92 172 88 186 76 188 L60 188 C52 188 50 180 54 170 Z" fill="{p["body"]}" {s()}/>
  <path d="M146 170 C150 152 126 146 114 160 C108 172 112 186 124 188 L140 188 C148 188 150 180 146 170 Z" fill="{p["body"]}" {s()}/>
  {toes}
  <g class="arm-l"><path d="M82 124 C70 128 68 146 76 152 C82 156 88 150 86 142 C86 134 90 128 82 124 Z" fill="{p["body"]}" {s()}/>
    <circle cx="75" cy="151" r="2.6" fill="#fff" stroke="{INK}" stroke-width="1.6"/><circle cx="81" cy="154" r="2.6" fill="#fff" stroke="{INK}" stroke-width="1.6"/></g>
  <g class="head">
    {head_spikes}
    <path d="M100 28 C146 28 166 58 162 86 C158 112 132 126 100 126 C68 126 42 112 38 86 C34 58 54 28 100 28 Z" fill="{p["body"]}"/>
    <path d="M42 96 C54 118 78 126 100 126 C122 126 146 118 158 96 C144 112 124 119 100 119 C76 119 56 112 42 96 Z" fill="{p["shade"]}"/>
    <path d="M100 28 C146 28 166 58 162 86 C158 112 132 126 100 126 C68 126 42 112 38 86 C34 58 54 28 100 28 Z" fill="none" {s()}/>
    <ellipse cx="66" cy="52" rx="13" ry="6" transform="rotate(-28 66 52)" fill="#fff" opacity="0.55"/>
    <circle cx="84" cy="40" r="3" fill="#fff" opacity="0.55"/>
    <circle cx="134" cy="50" r="4.5" fill="{p["spot"]}"/><circle cx="145" cy="64" r="3" fill="{p["spot"]}"/><circle cx="124" cy="40" r="2.5" fill="{p["spot"]}"/>
    <g class="eye"><ellipse cx="78" cy="80" rx="13" ry="15" fill="{INK}"/><circle cx="83" cy="73" r="5.5" fill="#fff"/><circle cx="73" cy="87" r="2.5" fill="#fff"/></g>
    <g class="eye"><ellipse cx="122" cy="80" rx="13" ry="15" fill="{INK}"/><circle cx="127" cy="73" r="5.5" fill="#fff"/><circle cx="117" cy="87" r="2.5" fill="#fff"/></g>
    <circle cx="58" cy="100" r="8" fill="{p["cheek"]}" opacity="0.85"/><circle cx="142" cy="100" r="8" fill="{p["cheek"]}" opacity="0.85"/>
    <path d="M88 101 Q100 113 112 101" fill="none" stroke="{INK}" stroke-width="4" stroke-linecap="round"/>
  </g>
  <g class="arm-r"><path d="M118 124 C130 128 132 146 124 152 C118 156 112 150 114 142 C114 134 110 128 118 124 Z" fill="{p["body"]}" {s()}/>
    <circle cx="125" cy="151" r="2.6" fill="#fff" stroke="{INK}" stroke-width="1.6"/><circle cx="119" cy="154" r="2.6" fill="#fff" stroke="{INK}" stroke-width="1.6"/></g>
</g>'''

CSS = """
.dino .torso, .dino .head, .dino .arm-l, .dino .arm-r { transform-box: view-box; }
.torso { transform-origin: 100px 188px; animation: breathe 3s ease-in-out infinite; }
.head { transform-origin: 100px 122px; animation: tilt 4.2s ease-in-out infinite; }
.tail { transform-origin: 134px 160px; transform-box: view-box; animation: wag 1.6s ease-in-out infinite; }
.eye { transform-box: fill-box; transform-origin: center; animation: blink 4s infinite; }
.arm-r { transform-origin: 118px 128px; animation: wave 5s ease-in-out infinite; }
@keyframes breathe { 50% { transform: scale(1.02, 0.975); } }
@keyframes tilt { 25% { transform: rotate(-4deg); } 75% { transform: rotate(4deg); } }
@keyframes wag { 50% { transform: rotate(7deg); } }
@keyframes blink { 0%, 92%, 100% { transform: scaleY(1); } 95% { transform: scaleY(0.1); } }
@keyframes wave { 0%, 55%, 100% { transform: rotate(0deg); } 62% { transform: rotate(-135deg); } 68% { transform: rotate(-110deg); } 74% { transform: rotate(-135deg); } 80% { transform: rotate(-110deg); } 88% { transform: rotate(-130deg); } }
"""

def page(static=False):
    svgs = "".join(f'<svg viewBox="0 0 200 200" width="300" height="300"><style>{"" if static else CSS}</style>{front(p, tag=n)}</svg>' for n, p in PALETTES.items())
    return f'<html><body style="margin:0;background:#BFE3E6;display:flex;gap:10px;padding:10px">{svgs}</body></html>'

open("front.html", "w").write(page())
open("front_static.html", "w").write(page(static=True))
