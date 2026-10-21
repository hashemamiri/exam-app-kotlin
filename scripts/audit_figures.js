#!/usr/bin/env node
/**
 * V326 — بازرس چشمی/هندسی شکل‌ها با مرورگر واقعی.
 *
 * ابزارها (geo_fig.js + graph_fig.js) در کروم headless رندر می‌شوند و با
 * getBBox واقعی، کادر هر متن و مسیر اندازه‌گیری می‌شود؛ سپس:
 *   • سرِ خطوط/کمان‌هایی که باید روی دایره بنشینند، با شعاع مقایسه می‌شود
 *     (اختلاف بین ۱ و ۱۲ پیکسل = «روی دایره نمی‌نشیند»),
 *   • کادر متن‌ها با محیط دایره، خطوط ساختاری، کادر و هم‌نوع‌هایشان برخورد می‌کند.
 *
 * اجرا:  node scripts/audit_figures.js [--shots] [--only=id,id] [--tol=2.5]
 * خروجی: فهرست ایرادها + (با --shots) تصویر .fig-audit/sheet.png برای بازبینی چشمی
 */
'use strict';
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const WEB = path.join(ROOT, 'app/src/main/assets/print/web');
const OUT = path.join(ROOT, '.fig-audit');

const GEO_IDS = ['tri', 'rtri', 'iso', 'eq', 'scal', 'sq', 'rect', 'para', 'rhomb', 'trap', 'itrap', 'rtrap',
  'circ', 'ang', 'parll', 'cube', 'box', 'cyl', 'cone', 'sph', 'pyr', 'pris', 'hex', 'kite', 'semi', 'sec',
  'ell', 'hemi', 'frust', 'pent', 'hept', 'oct', 'deca', 'ngon', 'ring', 'segm', 'arc', 'pseg', 'ray', 'ln',
  'tet', 'hpris', 'netc', 'thales', 'chord', 'tang', 'acut', 'obt', 'star', 'torus', 'elps', 'octa', 'dodec', 'icosa'];

const GRAPH_IDS = [
  'axcaliper', 'axsigntable', 'axtrigcircle', 'axmapping', 'axprobtree', 'axconic', 'axorbital', 'axpunnett',
  'axecg', 'axprosody', 'axtruthtable', 'axledger', 'axstaff', 'axkmap', 'axgate', 'axdfa', 'axbst', 'axbode',
  'axbeam', 'axmohr', 'axpv', 'axpert', 'axbenzene', 'axnormal', 'axsupply', 'axgame', 'axunitconv',
  'axperimeter', 'axpictograph', 'axrounding', 'axdivision', 'axshapeid', 'axcircleparts', 'axcylinder',
  'axproportion', 'axlinear', 'axbarchart', 'axparabola', 'axinthales', 'axpendulum', 'axincline', 'axcircuit',
  'axoptics', 'axheatcurve', 'axdna'
];

const args = process.argv.slice(2);
const only = (args.find(a => a.startsWith('--only=')) || '').slice(7);
const tol = parseFloat((args.find(a => a.startsWith('--tol=')) || '--tol=2.5').slice(6));
const shots = args.includes('--shots');

async function main() {
  const { chromium } = require('/tmp/pw/node_modules/playwright-core');
  const geo = fs.readFileSync(path.join(WEB, 'geo_fig.js'), 'utf8');
  const graph = fs.readFileSync(path.join(WEB, 'graph_fig.js'), 'utf8');

  const list = [
    ...GEO_IDS.filter(id => !only || only.split(',').includes(id)).map(id => ({ id, kind: 'geo' })),
    ...GRAPH_IDS.filter(id => !only || only.split(',').includes(id)).map(id => ({ id, kind: 'graph' }))
  ];

  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1500, height: 900 } });
  page.on('pageerror', e => console.error('PAGEERROR', e.message));
  await page.setContent('<!DOCTYPE html><html><head><meta charset="utf-8"></head><body></body></html>');
  await page.addScriptTag({ content: geo });
  await page.addScriptTag({ content: graph });

  const report = await page.evaluate(({ list, tol }) => {
    const segDist = (p, a, b) => {
      const vx = b[0] - a[0], vy = b[1] - a[1];
      const L2 = vx * vx + vy * vy || 1;
      let t = ((p[0] - a[0]) * vx + (p[1] - a[1]) * vy) / L2;
      t = Math.max(0, Math.min(1, t));
      return Math.hypot(p[0] - (a[0] + t * vx), p[1] - (a[1] + t * vy));
    };
    const issues = [];
    const add = (id, m) => issues.push(id + ': ' + m);
    const box = document.createElement('div');
    box.style.cssText = 'position:absolute;left:0;top:0;width:360px;height:280px;';
    document.body.appendChild(box);

    for (const { id, kind } of list) {
      let svgText;
      try {
        svgText = kind === 'geo' ? window.GeoFig.svg({ t: id }) : window.GraphFig.svg({ t: id, X: {} });
      } catch (e) { add(kind + ':' + id, 'خطای رندر — ' + e.message); continue; }
      if (!svgText || svgText.length < 40) { add(kind + ':' + id, 'خروجی خالی'); continue; }
      box.innerHTML = svgText;
      const svg = box.querySelector('svg');
      if (!svg) { add(kind + ':' + id, 'SVG ساخته نشد'); continue; }
      svg.setAttribute('width', '360');
      svg.setAttribute('height', '280');
      svg.style.width = '360px';
      svg.style.height = '280px';
      const vb = (svg.getAttribute('viewBox') || '0 0 360 280').split(/\s+/).map(Number);
      const sx = vb[2] ? 360 / vb[2] : 1, sy = vb[3] ? 280 / vb[3] : 1;
      const name = kind + ':' + id;
      /* V326 — خطی که کاملاً زیر یک مستطیل مات (مقاومت، جعبه…) پوشیده شده، دیده نمی‌شود */
      const coveredByFill = (l) => [...svg.querySelectorAll('rect')].some(r => {
        const f = (r.getAttribute('fill') || '').trim();
        if (!f || f === 'none' || /^rgba\([^)]*,\s*0?\.\d+\)$/.test(f)) return false;
        const x = +r.getAttribute('x'), y = +r.getAttribute('y'),
              w = +r.getAttribute('width'), h = +r.getAttribute('height');
        return [l.a, l.b].every(p => p[0] >= x - 1 && p[0] <= x + w + 1 && p[1] >= y - 1 && p[1] <= y + h + 1);
      });

      // --- هندسهٔ دایره‌ها
      const circles = [...svg.querySelectorAll('circle')].map(c => ({
        cx: +c.getAttribute('cx'), cy: +c.getAttribute('cy'), r: +c.getAttribute('r'),
        stroke: getComputedStyle(c).stroke, fill: c.getAttribute('fill') || ''
      })).filter(c => c.r >= 8);

      // --- خطوط
      const lines = [...svg.querySelectorAll('line')].map(l => {
        const st = getComputedStyle(l);
        return {
          a: [+l.getAttribute('x1'), +l.getAttribute('y1')],
          b: [+l.getAttribute('x2'), +l.getAttribute('y2')],
          w: parseFloat(st.strokeWidth) || 1, stroke: st.stroke, dash: l.getAttribute('stroke-dasharray')
        };
      });

      // --- سرِ مسیرها (فقط نقاط M/L)
      const pathPts = [];
      for (const p of [...svg.querySelectorAll('path')]) {
        const d = p.getAttribute('d') || '';
        const st = getComputedStyle(p);
        const seg = d.replace(/[Aa][^A-Za-z]*/g, ' ');
        const re = /(-?\d+(?:\.\d+)?)[ ,](-?\d+(?:\.\d+)?)/g;
        let m; const pts = [];
        while ((m = re.exec(seg))) pts.push([+m[1], +m[2]]);
        pathPts.push({ pts, w: parseFloat(st.strokeWidth) || 1, stroke: st.stroke, d });
      }

      // ۱) سرهای نزدیک محیط که دقیقاً روی آن نیستند (حلقهٔ تزئینی هم‌مرکز نادیده گرفته می‌شود)
      const outerR = (c) => Math.max(...circles.filter(k => Math.hypot(k.cx - c.cx, k.cy - c.cy) < 0.5).map(k => k.r));
      for (const c of circles) {
        if (c.r < outerR(c) - 0.5) continue;
        if (c.r < 20) continue;
        const ends = [];
        for (const l of lines) { ends.push(['خط', l.a, l]); ends.push(['خط', l.b, l]); }
        for (const p of pathPts) for (const pt of p.pts) ends.push(['مسیر', pt, p]);
        for (const [what, pt, obj] of ends) {
          if (!isFinite(pt[0]) || !isFinite(pt[1])) continue;
          const dd = Math.hypot(pt[0] - c.cx, pt[1] - c.cy);
          const err = Math.abs(dd - c.r);
          if (err > tol && err < 14) {
            add(name, `${what} با سر (${pt[0].toFixed(1)}, ${pt[1].toFixed(1)}) — فاصله از مرکز ${dd.toFixed(1)} ولی شعاع ${c.r} (${err.toFixed(1)}px بیرون/درون)`);
          }
        }
      }

      // ۲) متن‌ها: کادر واقعی
      const texts = [...svg.querySelectorAll('text')].map(t => {
        let b;
        try { b = t.getBBox(); } catch (e) { return null; }
        return {
          txt: (t.textContent || '').trim(), x: +t.getAttribute('x') || 0, y: +t.getAttribute('y') || 0,
          box: [b.x * sx, b.y * sy, (b.x + b.width) * sx, (b.y + b.height) * sy]
        };
      }).filter(Boolean);

      const inside = (b, x, y) => x >= b[0] && x <= b[2] && y >= b[1] && y <= b[3];
      /* V326 — برچسبی که وسط یک شکل مات (گرهٔ سفید، جعبه) نشسته، روی خط حساب نمی‌شود */
      const opaqueFill = (f) => f && f !== 'none' && !/^rgba\([^)]*,\s*0?(\.\d+)?\)$/.test(f) && !/^transparent$/.test(f);
      const insideShape = (x, y) => {
        for (const el2 of [...svg.querySelectorAll('circle')]) {
          if (!opaqueFill(el2.getAttribute('fill'))) continue;
          const cxx = +el2.getAttribute('cx'), cyy = +el2.getAttribute('cy'), crr = +el2.getAttribute('r');
          if (Math.hypot(x - cxx, y - cyy) < crr - 1) return true;
        }
        for (const r of [...svg.querySelectorAll('rect')]) {
          if (!opaqueFill(r.getAttribute('fill'))) continue;
          const rx = +r.getAttribute('x'), ry = +r.getAttribute('y'),
                rw = +r.getAttribute('width'), rh = +r.getAttribute('height');
          if (x >= rx && x <= rx + rw && y >= ry && y <= ry + rh) return true;
        }
        return false;
      };
      for (const t of texts) {
        if (!t.txt) continue;
        const b = t.box;
        const cxT = (b[0] + b[2]) / 2, cyT = (b[1] + b[3]) / 2;
        // ۲-الف) روی محیط دایره
        for (const c of circles) {
          if (c.r < 20) continue;
          const samples = [[b[0], b[1]], [b[2], b[1]], [b[0], b[3]], [b[2], b[3]], [cxT, cyT],
            [(b[0] + b[2]) / 2, b[1]], [(b[0] + b[2]) / 2, b[3]], [b[0], cyT], [b[2], cyT]];
          const straddle = samples.some(p => Math.hypot(p[0] - c.cx, p[1] - c.cy) < c.r) &&
                           samples.some(p => Math.hypot(p[0] - c.cx, p[1] - c.cy) > c.r);
          const near = samples.some(p => Math.abs(Math.hypot(p[0] - c.cx, p[1] - c.cy) - c.r) < 3);
          const corners = samples.slice(0, 4).map(p => inside(b, p[0], p[1]));
          if (straddle || near) add(name, `برچسب «${t.txt}» روی محیط دایره (r=${c.r}) می‌افتد`);
        }
        // ۲-ب) روی خط ساختاری (ضخیم و پررنگ، بدون خط‌چین)
        for (const l of lines) {
          if (l.w < 1.4 || l.dash) continue;
          if (/rgb\(2[0-4]\d,\s*2[0-4]\d,\s*2[0-4]\d\)/.test(l.stroke)) continue; // خاکستری روشن = راهنما
          if (coveredByFill(l)) continue;
          // فقط اگر «مرکز» برچسب روی خط باشد ایراد است؛ عبور لبه‌ای با هالهٔ سفید خوانا می‌ماند
          if (t.txt.length > 2 && segDist([cxT, cyT], l.a, l.b) < 1.8 && !insideShape(cxT, cyT)) {
            add(name, `برچسب «${t.txt}» روی خط (${l.a.map(Math.round)}→${l.b.map(Math.round)}) افتاده`);
          }
        }
        // ۲-ج) بیرون کادر
        if (b[0] < 1 || b[2] > 359 || b[1] < 1 || b[3] > 279) {
          add(name, `برچسب «${t.txt}» بیرون کادر است (${b.map(v => Math.round(v)).join(', ')})`);
        }
      }

      // ۳) هم‌پوشانی متن‌ها
      for (let i = 0; i < texts.length; i++) {
        for (let j = i + 1; j < texts.length; j++) {
          const A = texts[i].box, B = texts[j].box;
          const ox = Math.min(A[2], B[2]) - Math.max(A[0], B[0]);
          const oy = Math.min(A[3], B[3]) - Math.max(A[1], B[1]);
          if (ox > 1 && oy > 1 && ox * oy > 6) add(name, `برچسب‌های «${texts[i].txt}» و «${texts[j].txt}» روی هم می‌افتند`);
        }
      }
    }
    return issues;
  }, { list, tol });

  const byTool = {};
  for (const i of report) {
    const k = i.split(': ')[0];
    (byTool[k] = byTool[k] || []).push(i);
  }
  for (const k of Object.keys(byTool)) {
    console.log('\n### ' + k + '  (' + byTool[k].length + ')');
    for (const i of byTool[k]) console.log('  - ' + i.slice(k.length + 2));
  }
  console.log(`\nابزارها: ${list.length} | ایراد: ${report.length} در ${Object.keys(byTool).length} ابزار`);

  /* V326 — گزارش ماشین‌خوان برای تست‌های JVM (CI بدون مرورگر هم همین فایل را می‌سنجد) */
  const fullRun = !only && list.length >= 95;
  const jsonFile = path.join(ROOT, 'site', 'figure_audit.json');
  let prevTools = 0;
  try { prevTools = (JSON.parse(fs.readFileSync(jsonFile, 'utf8')).tools || 0); } catch (e) {}
  const summary = {
    tools: Math.max(list.length, prevTools),
    issues: report.length,
    byTool,
    note: 'خروجی node scripts/audit_figures.js — قاعده: هر سرِ خط/کمانی که باید روی محیط دایره بنشیند باید دقیقاً همان شعاع را داشته باشد و برچسب‌ها روی محیط/خط نیفتند.'
  };
  if (fullRun) fs.writeFileSync(jsonFile, JSON.stringify(summary, null, 2) + '\n');

  if (shots) {
    fs.mkdirSync(OUT, { recursive: true });
    const cols = 4, cellW = 370, cellH = 300;
    const html = `<!DOCTYPE html><html dir="rtl"><head><meta charset="utf-8"><style>
      body{margin:0;background:#fff;font:12px Tahoma}
      .g{display:grid;grid-template-columns:repeat(${cols},${cellW}px)}
      .c{border:1px solid #dde;position:relative;width:${cellW}px;height:${cellH}px}
      .h{position:absolute;top:0;left:4px;font:10px monospace;color:#c33;direction:ltr;z-index:2}
      svg{width:360px;height:280px}
    </style></head><body><div class="g" id="g"></div><script>window.__geo=${JSON.stringify(geo)};window.__graph=${JSON.stringify(graph)};</script></body></html>`;
    fs.writeFileSync(path.join(OUT, 'sheet.html'), html);
    const p2 = await browser.newPage({ viewport: { width: cols * cellW + 10, height: 900 } });
    await p2.goto('file://' + path.join(OUT, 'sheet.html'));
    await p2.addScriptTag({ content: geo });
    await p2.addScriptTag({ content: graph });
    await p2.evaluate((list) => {
      const g = document.getElementById('g');
      for (const { id, kind } of list) {
        const d = document.createElement('div');
        d.className = 'c';
        d.innerHTML = '<div class="h">' + kind + ':' + id + '</div>';
        const s = document.createElement('div');
        d.appendChild(s);
        g.appendChild(d);
        try {
          s.innerHTML = kind === 'geo' ? GeoFig.svg({ t: id }) : GraphFig.svg({ t: id, X: {} });
        } catch (e) { s.textContent = e.message; }
      }
    }, list);
    await p2.waitForTimeout(400);
    const file = path.join(OUT, 'sheet.png');
    await p2.screenshot({ path: file, fullPage: true });
    console.log('تصویر بازبینی: ' + file);
  }

  await browser.close();
  process.exit(report.length ? 1 : 0);
}

function segDist(p, a, b) {
  const vx = b[0] - a[0], vy = b[1] - a[1];
  const L2 = vx * vx + vy * vy || 1;
  let t = ((p[0] - a[0]) * vx + (p[1] - a[1]) * vy) / L2;
  t = Math.max(0, Math.min(1, t));
  return Math.hypot(p[0] - (a[0] + t * vx), p[1] - (a[1] + t * vy));
}

main().catch(e => { console.error(e); process.exit(2); });
