// Google Play store assets for SleepBetter, built from the app's own
// screenshots (the CI "SleepBetter-screenshots.zip") and icon.
//
//   node tools/store/make.js <screenshots dir> <out dir> <outfit fonts dir>
//
// Writes:
//   icon-512.png            512 x 512, full bleed (Play rounds the corners itself)
//   feature-graphic.png     1024 x 500
//   phone-1..8.png          1080 x 1920 marketing screenshots (9:16)
// Needs Playwright with Chromium (preinstalled on the build machines).
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const [shots, out, fonts] = process.argv.slice(2);
fs.mkdirSync(out, { recursive: true });
const repo = path.resolve(__dirname, '..', '..');
const icon = fs.readFileSync(path.join(repo, 'docs/icon/icon.svg'), 'utf8');
// Full-bleed square: drop the rounded preview mask.
const iconSquare = icon.replace('clip-path="url(#m)"', '');
const img = (name) => 'data:image/png;base64,' + fs.readFileSync(path.join(shots, name)).toString('base64');
const font = (w) => `@font-face{font-family:Outfit;font-weight:${w};src:url(data:font/ttf;base64,${fs.readFileSync(path.join(fonts, `outfit-${w}.ttf`)).toString('base64')})}`;
const FONTS = [400, 500, 600, 700, 800].map(font).join('');

const sized = (svg, n) => svg.replace('width="512" height="512"', `width="${n}" height="${n}"`);

const SLIDES = [
  { shot: '01-home.png', title: 'Fall asleep<br>faster 🌙', sub: 'Calming sounds, a cozy dino friend and your bedtime, all in one place', theme: 'lav' },
  { shot: '02-sounds.png', title: 'Mix your<br>perfect night', sub: 'Real rain, thunder, ocean waves, campfire and forest, layered your way', theme: 'night' },
  { shot: '24-auto-setup.png', title: 'No buttons<br>at bedtime', sub: 'Put your phone down and we track your sleep for you. No watch needed', theme: 'lav' },
  { shot: '03-insights.png', title: 'Watch your<br>sleep improve', sub: 'Sleep score, weekly trends and gentle, personal tips', theme: 'mint' },
  { shot: '06-wind-down.png', title: 'Breathe and<br>wind down', sub: 'Guided 4-7-8 breathing and a calm routine before bed', theme: 'peach' },
  { shot: '05-focus.png', title: 'Focus with<br>calm music', sub: '25-minute sessions with a live timer in your notifications', theme: 'sky' },
  { shot: '04-friends.png', title: 'Collect sleepy<br>dino friends', sub: 'Steady bedtimes unlock cute new friends', theme: 'lav' },
  { shot: '17-dark-home.png', title: 'Gentle on your<br>eyes at night', sub: 'Light and dark modes with five calming colour themes', theme: 'night' },
];

const THEMES = {
  lav: { bg: 'linear-gradient(165deg,#EDE7FF 0%,#D9CCFF 55%,#BBA6FF 100%)', ink: '#241B3D', soft: '#5B4F7A' },
  night: { bg: 'radial-gradient(120% 70% at 50% 0%,#4B37B8 0%,#231A5C 45%,#120E2E 100%)', ink: '#FFFFFF', soft: '#CFC6FF' },
  mint: { bg: 'linear-gradient(165deg,#E6F7EC 0%,#C9EFD6 55%,#A6E1BC 100%)', ink: '#173326', soft: '#3F6B54' },
  peach: { bg: 'linear-gradient(165deg,#FFF0E6 0%,#FFD9C4 55%,#FFBF9E 100%)', ink: '#3A2216', soft: '#7A4E38' },
  sky: { bg: 'linear-gradient(165deg,#E7F2FF 0%,#CBE2FF 55%,#A9CCFF 100%)', ink: '#14294A', soft: '#41608F' },
};

function stars(n, seed, color, minX = 0, width = 1080) {
  let s = seed, out = '';
  const r = () => (s = (s * 9301 + 49297) % 233280) / 233280;
  for (let i = 0; i < n; i++) {
    const size = 4 + r() * 8;
    out += `<div style="position:absolute;left:${minX + r() * (width - minX)}px;top:${r() * 560}px;width:${size}px;height:${size}px;border-radius:50%;background:${color};opacity:${0.25 + r() * 0.6}"></div>`;
  }
  return out;
}

function slide(s, i) {
  const t = THEMES[s.theme];
  const dark = s.theme === 'night';
  return `<!doctype html><html><head><style>${FONTS}
  *{margin:0;box-sizing:border-box}
  body{width:1080px;height:1920px;overflow:hidden;background:${t.bg};font-family:Outfit,sans-serif;position:relative}
  h1{position:absolute;left:84px;right:84px;top:120px;font-size:104px;line-height:1.02;font-weight:800;letter-spacing:-3px;color:${t.ink}}
  p{position:absolute;left:84px;right:120px;top:356px;font-size:40px;line-height:1.3;font-weight:500;color:${t.soft}}
  .phone{position:absolute;left:150px;top:560px;width:780px;height:1600px;border-radius:96px;background:#0E0B1F;padding:22px;
    box-shadow:0 60px 120px rgba(20,10,60,${dark ? 0.6 : 0.28}),0 0 0 3px rgba(255,255,255,${dark ? 0.12 : 0.6}) inset}
  .screen{width:100%;height:100%;border-radius:76px;overflow:hidden;background:#000}
  .screen img{width:100%;display:block}
  .pill{position:absolute;left:84px;top:64px;font-size:30px;font-weight:600;color:${t.soft};letter-spacing:1px}
  .glow{position:absolute;left:140px;top:600px;width:800px;height:800px;border-radius:50%;background:radial-gradient(closest-side,rgba(255,255,255,${dark ? 0.12 : 0.55}),transparent)}
  </style></head><body>
  ${dark ? stars(40, 7 + i, '#fff') : stars(14, 3 + i, '#fff')}
  <div class="pill">${String(i + 1).padStart(2, '0')} · SleepBetter</div>
  <h1>${s.title}</h1><p>${s.sub}</p>
  <div class="glow"></div>
  <div class="phone"><div class="screen"><img src="${img(s.shot)}"></div></div>
  </body></html>`;
}

function feature() {
  return `<!doctype html><html><head><style>${FONTS}
  *{margin:0;box-sizing:border-box}
  body{width:1024px;height:500px;overflow:hidden;font-family:Outfit,sans-serif;position:relative;
    background:radial-gradient(90% 120% at 25% 100%,#6B4FE0 0%,#3A2A93 40%,#1A1446 100%)}
  .moon{position:absolute;left:-120px;bottom:-260px;width:620px;height:620px;border-radius:50%;background:radial-gradient(closest-side,rgba(255,230,163,.35),transparent)}
  .icon{position:absolute;left:64px;top:70px;width:136px;height:136px;border-radius:34px;overflow:hidden;box-shadow:0 18px 40px rgba(0,0,0,.35)}
  h1{position:absolute;left:64px;top:228px;font-size:84px;font-weight:800;letter-spacing:-2.5px;color:#fff}
  p{position:absolute;left:66px;top:330px;width:500px;font-size:27px;line-height:1.3;font-weight:500;color:#D9D0FF}
  .chips{position:absolute;left:64px;top:420px;display:flex;gap:10px}
  .chip{font-size:19px;font-weight:600;color:#fff;background:rgba(255,255,255,.14);border:1px solid rgba(255,255,255,.22);padding:7px 14px;border-radius:20px}
  .phone{position:absolute;left:640px;top:40px;width:300px;height:640px;border-radius:44px;background:#0E0B1F;padding:10px;transform:rotate(8deg);
    box-shadow:0 40px 80px rgba(0,0,0,.45),0 0 0 2px rgba(255,255,255,.14) inset}
  .screen{width:100%;height:100%;border-radius:36px;overflow:hidden}
  .screen img{width:100%;display:block}
  </style></head><body>
  ${stars(24, 11, '#fff', 560, 1024)}
  <div class="moon"></div>
  <div class="icon">${sized(iconSquare, 136)}</div>
  <h1>SleepBetter</h1>
  <p>Calming sounds, gentle routines and automatic sleep tracking</p>
  <div class="chips"><span class="chip">🌧️ Real rain</span><span class="chip">🌊 Ocean</span><span class="chip">📱 Auto tracking</span></div>
  <div class="phone"><div class="screen"><img src="${img('01-home.png')}"></div></div>
  </body></html>`;
}

(async () => {
  const browser = await chromium.launch();
  const shoot = async (html, w, h, file) => {
    const page = await browser.newPage({ viewport: { width: w, height: h } });
    await page.setContent(html, { waitUntil: 'load' });
    await page.evaluate(() => document.fonts.ready);
    await page.screenshot({ path: path.join(out, file), clip: { x: 0, y: 0, width: w, height: h } });
    await page.close();
    console.log('saved', file);
  };
  await shoot(`<body style="margin:0">${sized(iconSquare, 512)}</body>`, 512, 512, 'icon-512.png');
  await shoot(feature(), 1024, 500, 'feature-graphic.png');
  for (let i = 0; i < SLIDES.length; i++) await shoot(slide(SLIDES[i], i), 1080, 1920, `phone-${i + 1}.png`);
  await browser.close();
})();
