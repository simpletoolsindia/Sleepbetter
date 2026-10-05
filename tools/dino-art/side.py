# Side-view (walking) dino rig, facing left.
from front import PALETTES, INK, spike
def side(p, tag=""):
    s = f'stroke="{INK}" stroke-width="4" stroke-linejoin="round" stroke-linecap="round"'
    spine = [(122,26,130,8,10),(135,38,150,26,10),(140,58,158,52,10),(146,96,164,88,10),(152,118,170,114,10),(162,150,170,132,9),(180,142,190,126,8)]
    plates = "".join(f'<path d="{spike(*a)}" fill="{p["plate"]}" {s}/>' for a in spine)
    toes = lambda xs, y: "".join(f'<circle cx="{x}" cy="{y}" r="3.6" fill="#fff" stroke="{INK}" stroke-width="2.2"/>' for x in xs)
    return f'''
<g class="side {tag}">
  <ellipse class="sh" cx="104" cy="186" rx="62" ry="7" fill="{INK}" opacity="0.15"/>
  <g class="bob">
    <g class="leg-back"><path d="M118 146 C114 134 136 130 140 146 L141 176 C141 184 121 184 120 176 Z" fill="{p["shade"]}" {s}/>{toes([124,132,139],182)}</g>
    <g class="tail">{plates}
      <path d="M136 146 C164 154 186 146 198 120 C196 146 176 172 134 172 Z" fill="{p["body"]}" {s}/>
    </g>
    <path d="M86 82 C64 100 64 150 80 172 L130 172 C154 168 160 136 150 110 C144 94 130 84 116 82 Z" fill="{p["body"]}" {s}/>
    <path d="M80 100 C68 120 70 156 84 168 L106 168 C96 150 94 120 100 96 Z" fill="{p["belly"]}"/>
    <path d="M78 116 Q88 118 97 114 M75 132 Q86 135 96 131 M77 148 Q88 151 98 147" fill="none" stroke="{p["stripe"]}" stroke-width="3" stroke-linecap="round"/>
    <circle cx="132" cy="120" r="4.5" fill="{p["spot"]}"/><circle cx="140" cy="138" r="3" fill="{p["spot"]}"/>
    <g class="leg-front"><path d="M88 146 C84 132 108 128 112 146 L112 178 C112 186 90 186 89 178 Z" fill="{p["body"]}" {s}/>{toes([94,102,109],184)}</g>
    <g class="arm"><path d="M90 108 C76 108 66 118 68 126 C70 132 78 130 80 124 C84 118 92 116 96 114 Z" fill="{p["body"]}" {s}/>
      <circle cx="68" cy="128" r="2.6" fill="#fff" stroke="{INK}" stroke-width="1.6"/></g>
    <g class="head">
      <path d="M62 38 C74 18 114 16 130 34 C142 48 140 74 126 84 C116 92 100 94 86 92 C70 94 42 92 34 76 C28 62 42 50 62 50 Z" fill="{p["body"]}"/>
      <path d="M36 80 C48 92 72 94 88 92 C104 94 118 90 128 82 C116 88 100 88 88 86 C70 88 48 88 36 80 Z" fill="{p["shade"]}"/>
      <path d="M62 38 C74 18 114 16 130 34 C142 48 140 74 126 84 C116 92 100 94 86 92 C70 94 42 92 34 76 C28 62 42 50 62 50 Z" fill="none" {s}/>
      <ellipse cx="80" cy="30" rx="11" ry="5" transform="rotate(-18 80 30)" fill="#fff" opacity="0.55"/>
      <circle cx="118" cy="40" r="4" fill="{p["spot"]}"/><circle cx="126" cy="56" r="3" fill="{p["spot"]}"/>
      <g class="eye"><ellipse cx="96" cy="52" rx="10" ry="12" fill="{INK}"/><circle cx="99.5" cy="47" r="4.2" fill="#fff"/><circle cx="93" cy="57" r="2" fill="#fff"/></g>
      <circle cx="46" cy="60" r="2.2" fill="{INK}"/>
      <circle cx="104" cy="74" r="7" fill="{p["cheek"]}" opacity="0.85"/>
      <path d="M44 74 Q66 86 88 74" fill="none" stroke="{INK}" stroke-width="4" stroke-linecap="round"/>
    </g>
  </g>
</g>'''
CSS = """
.side g { transform-box: view-box; }
.bob { animation: bob 0.6s ease-in-out infinite; }
.leg-front { transform-origin: 100px 146px; animation: step 1.2s ease-in-out infinite; }
.leg-back { transform-origin: 128px 146px; animation: step 1.2s ease-in-out infinite reverse; }
.arm { transform-origin: 92px 110px; animation: swing 1.2s ease-in-out infinite; }
.tail { transform-origin: 138px 158px; animation: wag 1.2s ease-in-out infinite; }
.head { transform-origin: 100px 90px; animation: nod 0.6s ease-in-out infinite; }
.eye { transform-box: fill-box; transform-origin: center; animation: blink 3.6s infinite; }
@keyframes bob { 50% { transform: translateY(-4px); } }
@keyframes step { 0%,100% { transform: rotate(-16deg); } 50% { transform: rotate(16deg); } }
@keyframes swing { 50% { transform: rotate(-22deg); } }
@keyframes wag { 50% { transform: rotate(-8deg); } }
@keyframes nod { 50% { transform: rotate(3deg); } }
@keyframes blink { 0%, 92%, 100% { transform: scaleY(1); } 95% { transform: scaleY(0.1); } }
"""
if __name__ == "__main__":
    svgs = "".join(f'<svg viewBox="0 0 210 200" width="300" height="286">{side(p, n)}</svg>' for n, p in PALETTES.items())
    open("side_static.html", "w").write(f'<html><body style="margin:0;background:#BFE3A0;display:flex;gap:10px;padding:10px">{svgs}</body></html>')
    open("side.html", "w").write(f'<html><body style="margin:0;background:#BFE3A0;display:flex;gap:10px;padding:10px"><style>{CSS}</style>{svgs}</body></html>')
