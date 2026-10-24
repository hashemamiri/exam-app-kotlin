(function () {
  var TYPES = [
    { id: 'col', name: 'ستونی' },
    { id: 'cmp', name: 'ستونی خوشه‌ای' },
    { id: 'lchr', name: 'خطی' },
    { id: 'pie', name: 'دایره‌ای' },
    { id: 'hbar', name: 'میله‌ای' },
    { id: 'hcmp', name: 'میله‌ای خوشه‌ای' },
    { id: 'area', name: 'ناحیه‌ای' },
    { id: 'scat', name: 'پراکندگی' },
    { id: 'map', name: 'نقشه‌ای' },
    { id: 'ohlc', name: 'سهام' },
    { id: 'surf', name: 'سطحی' },
    { id: 'radar', name: 'راداری' },
    { id: 'tree', name: 'نقشه درختی' },
    { id: 'sun', name: 'خورشیدی' },
    { id: 'hist', name: 'هیستوگرام' },
    { id: 'pareto', name: 'پارتو' },
    { id: 'box', name: 'جعبه‌ای' },
    { id: 'fall', name: 'آبشاری' },
    { id: 'funn', name: 'قیفی' },
    { id: 'combo', name: 'ترکیبی' },
    { id: 'donut', name: 'دوناتی' },
    { id: 'stack', name: 'پشته‌ای' },
    { id: 'sarea', name: 'مساحت انباشته' },
    { id: 'bub', name: 'حبابی' },
    { id: 'heat', name: 'کانتور' },
    { id: 'gauge', name: 'عقربه‌ای' },
    { id: 'bull', name: 'گلوله‌ای' },
    { id: 'pyra', name: 'هرم جمعیت' },
    { id: 'mekko', name: 'مکّو' },
    { id: 'venn', name: 'ون' },
    { id: 'pict', name: 'پیکتوگرام' },
    { id: 'flow', name: 'فلوچارت' },
    { id: 'gantt', name: 'گانت' },
    { id: 'ctrl', name: 'کنترلی' },
    { id: 'time', name: 'تایملاین' },
    { id: 'line', name: 'y=mx+b' },
    { id: 'quad', name: 'سهمی' },
    { id: 'sine', name: 'سینوسی' },
    { id: 'exp', name: 'نمایی' },
    { id: 'plot', name: 'محور مختصات' },
    { id: 'st100', name: 'انباشته ۱۰۰٪' },
    { id: 'lolli', name: 'لولی‌پاپ' },
    { id: 'dumb', name: 'دمبل' },
    { id: 'step', name: 'پله‌ای' },
    { id: 'spark', name: 'اسپارک‌لاین' },
    { id: 'slope', name: 'شیب' },
    { id: 'stream', name: 'جریانی' },
    { id: 'viol', name: 'ویولن' },
    { id: 'strip', name: 'نوار نقطه‌ای' },
    { id: 'stem', name: 'ساقه و برگ' },
    { id: 'waff', name: 'وافل' },
    { id: 'smat', name: 'ماتریس پراکندگی' },
    { id: 'dend', name: 'دندروگرام' },
    { id: 'sank', name: 'سنکی' },
    { id: 'chrd', name: 'کورد' },
    { id: 'netw', name: 'شبکه‌ای' },
    { id: 'bmap', name: 'نقشه حبابی' },
    { id: 'hmap', name: 'حرارتی' },
    { id: 'calh', name: 'تقویم حرارتی' },
    { id: 'rose', name: 'گل رز / قطبی' },
    { id: 'word', name: 'ابر واژه' }
  ];

  var COLORS = ['#6c63f5', '#27c4a8', '#f0a202', '#e4572e', '#4c9be8', '#9b5de5', '#00bbf9', '#f15bb5'];

  function esc(s) {
    return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\"/g, '&quot;');
  }
  function faEn(v) {
    if (v == null) return v;
    return String(v).replace(/[۰-۹]/g, function (c) { return String.fromCharCode(c.charCodeAt(0) - 1728); })
      .replace(/[٠-٩]/g, function (c) { return String.fromCharCode(c.charCodeAt(0) - 1584); });
  }
  function num(v, fb) {
    var n = parseFloat(faEn(String(v == null ? '' : v)).replace('،', '.'));
    return isFinite(n) ? n : fb;
  }
  function clamp(v, a, b) {
    return Math.max(a, Math.min(b, v));
  }
  function wrap(inner, vh, vw) {
    vh = vh || 280;
    vw = vw || 360;
    return '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ' + vw + ' ' + vh + '" width="' + Math.round(320 * vw / 360) + '" height="' + Math.round(250 * vh / 280) + '" direction="ltr" overflow="hidden">' +
      '<style>text{font:700 12px Vazirmatn,Tahoma,sans-serif;fill:#243044;direction:ltr;unicode-bidi:embed;paint-order:stroke;stroke:#fff;stroke-width:2.8px;stroke-linejoin:round}.tk{font:600 11px Vazirmatn,Tahoma,sans-serif;fill:#4a5870}.ttl{font:800 13px Vazirmatn,Tahoma,sans-serif;fill:#1a2433}.fa{font:700 11px Vazirmatn,Tahoma,sans-serif;fill:#1a2433}.lg{font:700 11px Vazirmatn,Tahoma,sans-serif;fill:#1a2433}.onf{font:800 11px Vazirmatn,Tahoma,sans-serif;fill:#fff;paint-order:stroke;stroke:#1a2433;stroke-width:2.6;stroke-linejoin:round}</style>' +
      inner + '</svg>';
  }
  function splitList(s) {
    return String(s == null ? '' : s).split(/[,،;|\n]+/).map(function (x) { return x.trim(); }).filter(Boolean);
  }
  function pad(a, n, fb) {
    a = a.slice();
    while (a.length < n) a.push(fb);
    return a;
  }

  function axes(xmin, xmax, ymin, ymax) {
    var L = 42, T = 22, R = 338, B = 248;
    function xof(x) { return L + (x - xmin) / (xmax - xmin) * (R - L); }
    function yof(y) { return B - (y - ymin) / (ymax - ymin) * (B - T); }
    var ox = xof(0), oy = yof(0);
    if (ox < L || ox > R) ox = L;
    if (oy < T || oy > B) oy = B;
    var h = '';
    h += '<rect class="gf-bg" x="' + L + '" y="' + T + '" width="' + (R - L) + '" height="' + (B - T) + '" fill="#fbfcfe" stroke="#d5dce6"/>';
    var gx;
    for (gx = Math.ceil(xmin); gx <= xmax; gx++) {
      var xx = xof(gx);
      h += '<line x1="' + xx + '" y1="' + T + '" x2="' + xx + '" y2="' + B + '" stroke="#eef1f6"/>';
      if (gx !== 0) h += '<text class="tk" x="' + xx + '" y="' + Math.min(B + 14, 272) + '" text-anchor="middle">' + gx + '</text>';
    }
    var gy;
    for (gy = Math.ceil(ymin); gy <= ymax; gy++) {
      var yy = yof(gy);
      h += '<line x1="' + L + '" y1="' + yy + '" x2="' + R + '" y2="' + yy + '" stroke="#eef1f6"/>';
      if (gy !== 0) h += '<text class="tk" x="' + (L - 6) + '" y="' + (yy + 3) + '" text-anchor="end">' + gy + '</text>';
    }
    h += '<line x1="' + L + '" y1="' + oy + '" x2="' + R + '" y2="' + oy + '" stroke="#2c3a50" stroke-width="1.6"/>';
    h += '<line x1="' + ox + '" y1="' + T + '" x2="' + ox + '" y2="' + B + '" stroke="#2c3a50" stroke-width="1.6"/>';
    h += '<polygon points="' + R + ',' + oy + ' ' + (R - 7) + ',' + (oy - 4) + ' ' + (R - 7) + ',' + (oy + 4) + '" fill="#2c3a50"/>';
    h += '<polygon points="' + ox + ',' + T + ' ' + (ox - 4) + ',' + (T + 7) + ' ' + (ox + 4) + ',' + (T + 7) + '" fill="#2c3a50"/>';
    h += '<text class="tk" x="' + (R - 4) + '" y="' + (oy - 8) + '">x</text>';
    h += '<text class="tk" x="' + (ox + 8) + '" y="' + (T + 12) + '">y</text>';
    return { html: h, xof: xof, yof: yof, L: L, T: T, R: R, B: B };
  }

  function plotFn(ax, fn, xmin, xmax) {
    var pts = [], i, n = 80;
    for (i = 0; i <= n; i++) {
      var x = xmin + (xmax - xmin) * i / n;
      var y = fn(x);
      if (!isFinite(y)) continue;
      pts.push(ax.xof(x).toFixed(1) + ',' + ax.yof(y).toFixed(1));
    }
    if (pts.length < 2) return '';
    return '<polyline points="' + pts.join(' ') + '" fill="none" stroke="#6c63f5" stroke-width="2.2" stroke-linejoin="round"/>';
  }

  function frame(L, T, R, B) {
    return '<rect class="gf-bg" x="' + L + '" y="' + T + '" width="' + (R - L) + '" height="' + (B - T) + '" fill="#fbfcfe" stroke="#d5dce6"/>';
  }
  function itemW(n) {
    return 11 + 6 + Math.min(88, String(n).length * 7.2 + 8);
  }
  function legendBar(items, y) {
    items = (items || []).filter(function (it) { return it && String(it.n || '').trim(); });
    if (!items.length) return { html: '', h: 0 };
    var sw = 11, gap = 14, maxW = 340, rows = [], cur = [], w = 0, i;
    for (i = 0; i < items.length; i++) {
      var wi = itemW(items[i].n) + gap;
      if (cur.length && w + wi > maxW) { rows.push(cur); cur = []; w = 0; }
      cur.push(items[i]); w += wi;
    }
    if (cur.length) rows.push(cur);
    var parts = [], ri;
    for (ri = 0; ri < rows.length; ri++) {
      var row = rows[ri], tot = 0, k;
      for (k = 0; k < row.length; k++) tot += itemW(row[k].n) + (k ? gap : 0);
      var x = Math.max(10, (360 - tot) / 2);
      var yy = y + ri * 16;
      for (k = 0; k < row.length; k++) {
        parts.push('<rect x="' + x.toFixed(1) + '" y="' + (yy - 9) + '" width="' + sw + '" height="' + sw + '" rx="2" fill="' + row[k].c + '"/>');
        parts.push('<text class="lg" x="' + (x + sw + 6).toFixed(1) + '" y="' + yy + '">' + esc(row[k].n) + '</text>');
        x += itemW(row[k].n) + gap;
      }
    }
    return { html: parts.join(''), h: rows.length * 16 };
  }
  function chrome(title, items) {
    var h = '', y = 14;
    if (title) {
      h += '<text class="ttl" x="180" y="14" text-anchor="middle">' + esc(title) + '</text>';
      y = 30;
    }
    var leg = legendBar(items, y);
    h += leg.html;
    if (leg.h) y += leg.h;
    return { html: h, T: Math.max(title || (items && items.length) ? y + 8 : 28, 28) };
  }

  function ringSlice(cx, cy, r0, r1, a0, a1, fill) {
    var large = (a1 - a0) > Math.PI ? 1 : 0;
    function pt(r, a) { return (cx + r * Math.cos(a)).toFixed(1) + ',' + (cy + r * Math.sin(a)).toFixed(1); }
    return '<path d="M' + pt(r1, a0) + ' A' + r1 + ',' + r1 + ' 0 ' + large + ' 1 ' + pt(r1, a1) +
      ' L' + pt(r0, a1) + ' A' + r0 + ',' + r0 + ' 0 ' + large + ' 0 ' + pt(r0, a0) +
      ' Z" fill="' + fill + '" stroke="#fff" stroke-width="1.2"/>';
  }

  function personIcon(x, y, s, fill) {
    var h = s * 0.28;
    return '<g fill="' + fill + '">' +
      '<circle cx="' + x + '" cy="' + (y - s * 0.62) + '" r="' + (s * 0.16) + '"/>' +
      '<path d="M' + (x - s * 0.22) + ',' + (y - s * 0.42) + ' h' + (s * 0.44) + ' v' + (s * 0.38) +
      ' h' + (-s * 0.12) + ' v' + (s * 0.32) + ' h' + (-s * 0.2) + ' v' + (-s * 0.32) +
      ' h' + (-s * 0.12) + ' z"/>' +
      '</g>';
  }

  function parsePairs(s) {
    return splitList(s).map(function (item) {
      var m = String(item).match(/^\s*([^:\-–—]+)\s*[-–—]\s*([^:]+?)(?:\s*:\s*(.+))?\s*$/);
      if (!m) return null;
      return { a: m[1].trim(), b: m[2].trim(), v: num(m[3], 1) };
    }).filter(Boolean);
  }
  function lerpC(a, b, f) {
    function hx(c) {
      c = String(c || '#888').replace('#', '');
      if (c.length === 3) c = c[0] + c[0] + c[1] + c[1] + c[2] + c[2];
      return [parseInt(c.slice(0, 2), 16), parseInt(c.slice(2, 4), 16), parseInt(c.slice(4, 6), 16)];
    }
    var A = hx(a), B = hx(b);
    return 'rgb(' + Math.round(A[0] + (B[0] - A[0]) * f) + ',' + Math.round(A[1] + (B[1] - A[1]) * f) + ',' + Math.round(A[2] + (B[2] - A[2]) * f) + ')';
  }

  // V137.2 — شناسه‌های محورهای تازه (هم‌نام با AxisSvgRenderer.SUPPORTED).
  var AXIS_EXT = ['axnumpts', 'axnumint', 'axnumineq', 'axnumfrac', 'axnumdec', 'axnumblank', 'axnumlog', 'axnumtwo',
    'axxypts', 'axxyline', 'axxyvec', 'axxycirc', 'axxynogrid', 'axxyquads', 'axxyblank', 'axdual',
    'axlog', 'axloglog', 'axtime', 'axpolpts', 'ax3dpt',
    // V304 — ابزار ریاضی ابتدایی (PrimaryMathSvgRenderer.kt)
    'axtally', 'axbase10', 'axplace', 'axclock', 'axruler', 'axfrac', 'axmoney',
    'axabacus', 'axjump', 'axbalance', 'axthermo', 'axsaw', 'axpattern',
    'axtenframe', 'axsym', 'axspin', 'axbeaker', 'axgeoboard', 'axdice', 'axmularea', 'axvenn',
    'axnumgrid', 'axfacttree', 'axfnmachine', 'axfracwall', 'axtangram', 'axangle', 'axrod', 'axtower', 'axmulwheel', 'axmultgrid', 'axnet', 'axcubes', 'axcalendar', 'axlever', 'axcycle', 'axwordtrain',
    // V327 — رنگ‌آمیزی، فارسی و علوم دبستان
    'axpaintgrid', 'axletterforms', 'axsyllablechart', 'axmagnet', 'axsenses', 'axseedgrow', 'axbodyparts', 'axtiles', 'axkenken', 'axdicefold', 'axtransform', 'axcaliper', 'axsigntable', 'axtrigcircle', 'axmapping', 'axprobtree', 'axconic', 'axorbital', 'axpunnett', 'axecg', 'axprosody', 'axtruthtable', 'axledger', 'axstaff', 'axkmap', 'axgate', 'axdfa', 'axbst', 'axbode', 'axbeam', 'axmohr', 'axpv', 'axpert', 'axbenzene', 'axnormal', 'axsupply', 'axgame',
    // V319 — بستهٔ سوم ابزار آموزشی (دبستان تا دانشگاه)
    'axunitconv', 'axperimeter', 'axpictograph', 'axrounding', 'axdivision', 'axshapeid', 'axcircleparts', 'axcylinder', 'axproportion', 'axlinear', 'axbarchart', 'axparabola', 'axinthales', 'axpendulum', 'axincline', 'axcircuit', 'axoptics', 'axheatcurve', 'axdna', 'axcell', 'axtimeline', 'axmapgeo', 'axsentence', 'axvectors', 'axmatrix', 'axintegral', 'axflowchart', 'axstack', 'axsort', 'axregression'];
  var PRIMARY_MATH_FIRST_ID = 'axtally';
  // V206 — حالت «درج محور» در ویرایشگر وب (سایت): همان ۶ محور پایهٔ AXIS_FIGURES اپ (FigureGallery.kt)
  // با پیش‌فرض‌های یکسان. در اپ، انتخاب محور بومی است و این فهرست فقط وقتی open(…,'axis') یا
  // توکنی با نوع محور باز شود نمایش داده می‌شود؛ فهرست «نوع نمودار» دست‌نخورده می‌ماند.
  var AXIS_TYPES = [
    { id: 'axnum', name: 'محور اعداد', X: { xmin: '-5', xmax: '5', step: '1' } },
    { id: 'axxy', name: 'محور مختصات (۴ ربع)', X: { xmin: '-5', xmax: '5', ymin: '-4', ymax: '4', step: '1' } },
    { id: 'axq1', name: 'ربع اول', X: { xmax: '10', ymax: '8', step: '1' } },
    { id: 'axgrid', name: 'شبکهٔ شطرنجی', X: { xmax: '10', ymax: '8' } },
    { id: 'axpol', name: 'محور قطبی', X: { xmax: '4' } },
    { id: 'ax3d', name: 'سه‌بعدی (x,y,z)', X: { xmax: '4', step: '1' } },
    // V247 — ۲۱ محور V137.2 (پیش‌فرض‌ها عیناً FigureGallery.kt)
    { id: 'axnumpts', name: 'محور اعداد با نقطه', X: { xmin: '-5', xmax: '5', step: '1', pts: '-2,1.5,4' } },
    { id: 'axnumint', name: 'بازه روی محور', X: { xmin: '-5', xmax: '5', step: '1', lo: '-2', hi: '3', lc: '1', hc: '0' } },
    { id: 'axnumineq', name: 'نامعادله روی محور', X: { xmin: '-5', xmax: '5', step: '1', x0: '1', dir: '1', cl: '0' } },
    { id: 'axnumfrac', name: 'محور اعداد کسری', X: { xmin: '0', xmax: '3', step: '1', den: '4' } },
    { id: 'axnumdec', name: 'محور اعداد اعشاری', X: { xmin: '0', xmax: '1', step: '0.1' } },
    { id: 'axnumblank', name: 'محور اعداد بی‌عدد', X: { xmin: '-5', xmax: '5', step: '1' } },
    { id: 'axnumlog', name: 'محور لگاریتمی', X: { xmax: '4' } },
    { id: 'axnumtwo', name: 'دو محور موازی (تبدیل)', X: { xmin: '0', xmax: '10', step: '1', m: '2', b: '0', s1: 'A', s2: 'B' } },
    { id: 'axxypts', name: 'مختصات با نقطه‌ها', X: { xmin: '-5', xmax: '5', ymin: '-4', ymax: '4', step: '1', pts: '1,2;-3,1;2,-2' } },
    { id: 'axxyline', name: 'مختصات با خط y=mx+b', X: { xmin: '-5', xmax: '5', ymin: '-4', ymax: '4', step: '1', m: '1', b: '1' } },
    { id: 'axxyvec', name: 'مختصات با بردار', X: { xmin: '-5', xmax: '5', ymin: '-4', ymax: '4', step: '1', x1: '0', y1: '0', x2: '3', y2: '2' } },
    { id: 'axxycirc', name: 'مختصات با دایره', X: { xmin: '-5', xmax: '5', ymin: '-4', ymax: '4', step: '1', cx: '1', cy: '1', r: '2' } },
    { id: 'axxynogrid', name: 'مختصات بدون شبکه', X: { xmin: '-5', xmax: '5', ymin: '-4', ymax: '4', step: '1' } },
    { id: 'axxyquads', name: 'مختصات با نام ربع‌ها', X: { xmin: '-5', xmax: '5', ymin: '-4', ymax: '4', step: '1' } },
    { id: 'axxyblank', name: 'مختصات بی‌عدد', X: { xmin: '-5', xmax: '5', ymin: '-4', ymax: '4', step: '1' } },
    { id: 'axdual', name: 'دو محور y (چپ/راست)', X: { xmin: '0', xmax: '10', ymin: '0', ymax: '100', lo: '0', hi: '10', s1: 'y₁', s2: 'y₂' } },
    { id: 'axlog', name: 'نیم‌لگاریتمی', X: { xmax: '10', ymax: '3' } },
    { id: 'axloglog', name: 'تمام‌لگاریتمی', X: { xmax: '3', ymax: '3' } },
    { id: 'axtime', name: 'محور زمان', X: { labs: '۸:۰۰,۹:۰۰,۱۰:۰۰,۱۱:۰۰,۱۲:۰۰', s1: 't' } },
    { id: 'axpolpts', name: 'قطبی با نقطه‌ها', X: { xmax: '4', pts: '2,30;3,120' } },
    { id: 'ax3dpt', name: 'سه‌بعدی با نقطه', X: { xmax: '4', step: '1', x1: '3', y1: '2', z1: '2' } },
    // V304 — ابزار ریاضی ابتدایی (همان FigureGallery.kt؛ از axtally به بعد با سرتیتر جدا)
    { id: 'axtally', name: 'چوب‌خط', X: { n: '7' } },
    { id: 'axbase10', name: 'دسته‌های ده‌تایی', X: { n: '134' } },
    { id: 'axplace', name: 'جدول ارزش مکانی', X: { n: '2457', blank: '0' } },
    { id: 'axclock', name: 'ساعت عقربه‌ای', X: { h: '3', m: '0', nums: '1' } },
    { id: 'axruler', name: 'خط‌کش', X: { len: '10' } },
    { id: 'axfrac', name: 'کسر شکلی', X: { num: '1', den: '4', shape: 'circle' } },
    { id: 'axmoney', name: 'سکه و اسکناس', X: { notes: '1000,2000,5000,10000', coins: '500,1000', unit: 'تومان' } },
    { id: 'axabacus', name: 'چرتکهٔ ستونی', X: { n: '142536', cols: '6' } },
    { id: 'axjump', name: 'پرش روی محور', X: { start: '0', step: '3', jumps: '4', max: '12', op: '+' } },
    { id: 'axbalance', name: 'ترازوی دوکفه‌ای', X: { left: '5', right: '3', unit: 'کیلوگرم' } },
    { id: 'axthermo', name: 'دماسنج', X: { val: '25', min: '-10', max: '50' } },
    { id: 'axsaw', name: 'جدول شگفت‌انگیز و سودوکو', X: { size: '4', vals: '1,2,3,4;3,4,?,2;2,?,4,3;4,3,2,1', mode: 'shape' } },
    { id: 'axpattern', name: 'الگویابی رنگی/شکلی/عددی', X: { seq: '●,▲,●,▲,●,?', mode: 'shape' } },
    { id: 'axtenframe', name: 'قاب ده‌تایی', X: { a: '7', b: '5', frames: '2' } },
    { id: 'axsym', name: 'تقارن شطرنجی', X: { cells: '1,1;2,1;2,2;3,2', axis: 'v', mirror: '1' } },
    { id: 'axbeaker', name: 'استوانهٔ مدرج (حجم)', X: { val: '350', max: '500', unit: 'mL', count: '1' } },
    { id: 'axgeoboard', name: 'ژئوبرد (تختهٔ میخی)', X: { pts: '1,1;4,1;4,3;1,3', grid: '5' } },
    { id: 'axdice', name: 'تاس و مکعب شمارش', X: { dice: '4,6', sum: '1' } },
    { id: 'axmularea', name: 'ضرب مساحتی (مستطیلی)', X: { a: '24', b: '13', blank: '0' } },
    { id: 'axvenn', name: 'نمودار ون (مجموعه‌ها)', X: { left: 'پرنده: کبوتر، گنجشک', right: 'آبزی: ماهی، میگو', mid: 'اردک' } },
    { id: 'axnumgrid', name: 'جدول صدتایی و الگوهای عددی', X: { start: '1', cols: '10', rows: '10', rule: '5', blanks: '15,35,60,85' } },
    { id: 'axfacttree', name: 'درخت تجزیهٔ عوامل اول', X: { n: '36', blank: '0' } },
    { id: 'axfnmachine', name: 'ماشین ورودی و خروجی', X: { 'in': '2,4,6,8', op1: '+ 3', op2: '× 2', blank: '1' } },
    { id: 'axfracwall', name: 'دیوار کسرها (مقایسه و تساوی)', X: { dens: '1,2,3,4,6,8', fills: '1,1,2,2,3,4', mode: 'label' } },
    { id: 'axtangram', name: 'تانگرام (پازل ۷ تکه)', X: { shape: 'square', mode: 'color' } },
    { id: 'axangle', name: 'نقاله و اندازه‌گیری زاویه', X: { deg: '65', pro: '1', label: '1' } },
    { id: 'axrod', name: 'میله‌های کوئیزنر (۱ تا ۱۰)', X: { nums: '1,2,3,4,5,6,7,8,9,10', blank: '0' } },
    { id: 'axtower', name: 'هرم اعداد (جمع و تفریق)', X: { base: '1,2,3,4', op: '+', blank: '0' } },
    { id: 'axmulwheel', name: 'چرخ ضرب و جمع', X: { c: '3', n: '10', op: '×', hide: '0' } },
    { id: 'axmultgrid', name: 'جدول ضرب پازل', X: { rows: '2,4,7', cols: '3,5,8', blank: '0' } },
    { id: 'axnet', name: 'گستردهٔ احجام (نت)', X: { shape: 'cube', mode: 'color' } },
    { id: 'axcubes', name: 'شمارش مکعب‌های روی‌هم‌چیده', X: { map: '2,1,3;1,0,1;2,2,1', view: 'iso', hide: '0' } },
    { id: 'axcalendar', name: 'تقویم ماهانه', X: { month: 'مهر', start: '3', days: '30', mark: '12,23', blank: '0' } },
    { id: 'axlever', name: 'اهرم و ماشین‌های ساده', X: { kind: 'lever1', load: '60', effort: '20', l1: '2', l2: '1' } },
    { id: 'axcycle', name: 'چرخه‌های علوم (آب، گیاه، جانور)', X: { kind: 'water' } },
    { id: 'axwordtrain', name: 'قطار کلمه‌سازی', X: { syll: 'مد,ر,سه', word: 'مدرسه', mode: 'join' } },
    // V327 / V335.8 — ابزارهای دبستان
    { id: 'axpaintgrid', name: 'شبکهٔ رنگ‌آمیزی', X: { rows: '4', cols: '4', goal: '4', cells: '' } },
    { id: 'axletterforms', name: 'نویسه‌های حرف (فارسی)', X: { ch: 'ب' } },
    { id: 'axsyllablechart', name: 'هجاهای کلمه', X: { word: 'مدرسه', mode: 'ask' } },
    { id: 'axmagnet', name: 'آهنربا و قطب‌ها', X: { mode: 'attract' } },
    { id: 'axsenses', name: 'حواس پنج‌گانه', X: {} },
    { id: 'axtiles', name: 'کاشی‌های جبر', X: { x2: '1', x: '2', k: '3', neg: '0' } },
    { id: 'axkenken', name: 'کن‌کن و کاکورو (هوش ریاضی)', X: { size: '4', cages: '11,12|6|+;13,14|7|+;21,22|3|×;23,24,31,32|10|+;33,34|4|×;41,42,43,44|10|+' } },
    { id: 'axdicefold', name: 'گستردهٔ تاس و وجه مقابل', X: { mode: 'dots', top: '1', ask: '0' } },
    { id: 'axtransform', name: 'تبدیلات هندسی روی شبکه', X: { shape: 'tri', op: 'translate', dx: '3', dy: '2', ang: '90', axis: 'y', grid: '8' } },
    { id: 'axcaliper', name: 'کولیس و ریزسنج', X: { kind: 'vernier', val: '23.4', max: '50' } },
    { id: 'axsigntable', name: 'جدول تعیین علامت', X: { roots: '-2,1,3', mult: '1,1,1', lead: '1' } },
    { id: 'axtrigcircle', name: 'دایرهٔ مثلثاتی', X: { deg: '135', show: '1' } },
    { id: 'axmapping', name: 'تناظر و تابع (دامنه و برد)', X: { from: '1,2,3', to: 'a,b,c', arrows: '1-a;2-a;3-b' } },
    { id: 'axprobtree', name: 'درخت احتمال', X: { first: 'شیر,خط', second: 'شیر,خط', mode: 'prob' } },
    { id: 'axconic', name: 'مقاطع مخروطی (بیضی، هذلولی، سهمی)', X: { kind: 'ellipse', a: '4', b: '2' } },
    { id: 'axorbital', name: 'آرایش الکترونی و اوربیتال', X: { z: '26', mode: 'box', sym: 'Na', val: '1' } },
    { id: 'axpunnett', name: 'مربع پانت و شجره‌نامه', X: { kind: 'punnett', p1: 'Aa', p2: 'Aa', mode: 'genotype' } },
    { id: 'axecg', name: 'نوار قلب و نوسان', X: { kind: 'ecg', n: '2', label: '1', A: '1', T: '2' } },
    { id: 'axprosody', name: 'تقطیع عروضی و وزن شعر', X: { line: 'فعولن فعولن فعولن فعل', mode: 'arkan' } },
    { id: 'axtruthtable', name: 'جدول ارزش و مربع تقابل', X: { kind: 'truth', expr: 'p و q' } },
    { id: 'axledger', name: 'سند حسابداری T و دفتر روزنامه', X: { title: 'صندوق', debit: '12000,8500', credit: '6000,3000' } },
    { id: 'axstaff', name: 'پنج‌خط حامل و چرخهٔ رنگ', X: { kind: 'staff', clef: 'treble', notes: '60,62,64,65,67' } },
    { id: 'axkmap', name: 'جدول کارنو (K-Map)', X: { vars: '3', ones: '0,1,3,5', groups: '' } },
    { id: 'axgate', name: 'گیت‌های منطقی', X: { kind: 'and', a: '1', b: '0' } },
    { id: 'axdfa', name: 'ماشین حالت متناهی (DFA)', X: { states: 'q0,q1,q2', start: 'q0', accept: 'q2', edges: 'q0-q1:a;q1-q2:b;q1-q1:a' } },
    { id: 'axbst', name: 'درخت دودویی جستجو (BST)', X: { vals: '50,30,70,20,40,60,80', mode: 'bst' } },
    { id: 'axbode', name: 'نمودار بود و نایکوئیست', X: { kind: 'bode', gain: '20', poles: '1,10' } },
    { id: 'axbeam', name: 'تیر و نمودار برش و گشتاور', X: { len: '6', load: 'point', p: '10', a: '2', q: '2' } },
    { id: 'axmohr', name: 'دایرهٔ مور (تنش و کرنش)', X: { sx: '80', sy: '20', txy: '30' } },
    { id: 'axpv', name: 'چرخهٔ ترمودینامیکی (P-V)', X: { kind: 'carnot' } },
    { id: 'axpert', name: 'شبکهٔ مدیریت پروژه (CPM/PERT)', X: { acts: 'A:1-2:3;B:2-3:2;C:2-4:4;D:3-4:1' } },
    { id: 'axbenzene', name: 'بنزن و سیکلوهگزان (شیمی آلی)', X: { kind: 'kekule', mode: 'label' } },
    { id: 'axnormal', name: 'منحنی نرمال و امتیاز Z', X: { lo: '-1', hi: '1', mu: '0', sd: '1' } },
    { id: 'axsupply', name: 'عرضه و تقاضا', X: { kind: 'equilibrium', shift: '2', ceiling: '0' } },
    { id: 'axgame', name: 'ماتریس بازی و مدل IS-LM', X: { kind: 'game', mat: '3,0;5,1' } },
    // V319
    { id: 'axunitconv', name: 'نردبان تبدیل واحد', X: { kind: 'len', val: '1' } },
    { id: 'axperimeter', name: 'محیط و مساحت اشکال', X: { shape: 'rect', w: '6', h: '4', n: '5' } },
    { id: 'axrounding', name: 'گرد کردن و تخمین', X: { n: '147', mode: 'ten' } },
    { id: 'axcircleparts', name: 'اجزای دایره (شعاع، قطر، وتر)', X: { show: 'all' } },
    { id: 'axcylinder', name: 'استوانه و مخروط (حجم و سطح)', X: { kind: 'cylinder', r: '3', h: '6' } },
    { id: 'axproportion', name: 'تناسب و جدول نسبت', X: { a: '3', b: '4', c: '6' } },
    { id: 'axlinear', name: 'دستگاه دو معادلهٔ خطی', X: { m1: '1', b1: '2', m2: '-1', b2: '4' } },
    { id: 'axbarchart', name: 'نمودار میله‌ای و داده‌های آماری', X: { data: '4,7,3,6,5', labs: 'الف,ب,ج,د,ه', show: 'mean' } },
    { id: 'axparabola', name: 'سهمی (رأس، محور تقارن، ریشه)', X: { a: '1', b: '-2', c: '-3' } },
    { id: 'axinthales', name: 'زاویهٔ مرکزی و محاطی', X: { ang: '60' } },
    { id: 'axpendulum', name: 'آونگ ساده (دورهٔ نوسان)', X: { len: '1', amp: '20' } },
    { id: 'axincline', name: 'سطح شیب‌دار و تحلیل نیروها', X: { ang: '30', mass: '5' } },
    { id: 'axcircuit', name: 'مدار سری و موازی (قانون اهم)', X: { mode: 'series', r1: '4', r2: '6', v: '12' } },
    { id: 'axoptics', name: 'عدسی و مسیر پرتوهای نور', X: { kind: 'convex', f: '10', d: '30' } },
    { id: 'axheatcurve', name: 'منحنی گرمایش آب (گرما و دما)', X: {} },
    { id: 'axdna', name: 'ساختار DNA و جفت‌بازها', X: { mode: 'helix', seq: 'ATGC' } },
    { id: 'axtimeline', name: 'خط زمان تاریخی', X: { events: '۵۵۹:سقوط ساسانیان,۶۲۲:هجرت پیامبر,۱۲۵۸:ایلخانان,۱۵۰۱:صفویان' } },
    { id: 'axmapgeo', name: 'نقشه و جهت‌های جغرافیایی', X: { kind: 'compass' } },
    { id: 'axsentence', name: 'اجزای جمله و نقش دستوری', X: { subj: 'کودکان', verb: 'بازی می‌کنند', obj: '', mode: 'verbal' } },
    { id: 'axvectors', name: 'جمع و تفاضل بردارها', X: { ax1: '3', ay1: '1', bx: '1', by: '2', mode: 'sum' } },
    { id: 'axmatrix', name: 'ضرب ماتریس ۲×۲', X: { a: '1,2;3,4', b: '5,6;7,8' } },
    { id: 'axintegral', name: 'انتگرال معین و مساحت زیر نمودار', X: { f: 'quad', lo: '0', hi: '2' } },
    { id: 'axflowchart', name: 'فلوچارت الگوریتم', X: { steps: 'شروع,ورودی: n,n > 0 ؟,چاپ n,پایان' } },
    { id: 'axstack', name: 'پشته و صف (ساختمان داده)', X: { mode: 'stack', items: '3,5,8', op: 'push' } },
    { id: 'axsort', name: 'مرتب‌سازی حبابی مرحله‌ای', X: { vals: '5,3,8,1', step: '0' } },
    { id: 'axregression', name: 'رگرسیون خطی و همبستگی', X: { pts: '1,2;2,3;3,5;4,4;5,6' } }
  ];
  function isAxisType(t) { return /^ax/.test(String(t || '')); }
  // V247 — پورت یک‌به‌یک ۲۱ محور تازهٔ AxisSvgRenderer.kt (V137.2) برای سایت (بدون پل بومی).
  function axisExSvg(t, X, title) {
    var AL = 40, AT = 26, AR = 336, AB = 246, ST = '#2c3a50', GR = '#d5dce6', MU = '#4a5870', PT = '#d6336c';
    function f2(v) { return String(Math.round(v * 100) / 100); }
    function ln(x1, y1, x2, y2, c, w, dash) { var ds = dash === true ? '4 3' : (typeof dash === 'string' && dash ? dash : ''); return '<line x1="' + f2(x1) + '" y1="' + f2(y1) + '" x2="' + f2(x2) + '" y2="' + f2(y2) + '" stroke="' + c + '" stroke-width="' + w + '"' + (ds ? ' stroke-dasharray="' + ds + '"' : '') + '/>'; }
    // V322 / V335.11 — جاگیری خودکار برچسب با پشتیبانی عرض بوم پویا (cw)
    function fitTxt(x, s, an, sz) { return fitTxtW(x, s, an, sz, 360); }
    function fitTxtW(x, s, an, sz, cw) {
      var W = cw || 360, maxW = W - 16, txt = String(s), fs = sz || 11, est = txt.length * fs * 0.55, keep;
      if (est > maxW) { fs = Math.max(7, Math.floor(maxW / (txt.length * 0.55))); est = txt.length * fs * 0.55; }
      if (est > maxW) { keep = Math.max(2, Math.floor(maxW / (fs * 0.55)) - 1); txt = txt.slice(0, keep) + '…'; est = txt.length * fs * 0.55; }
      var half = est / 2, lo = an === 'middle' ? half + 3 : an === 'end' ? est + 3 : 3, hi = an === 'middle' ? (W - 3) - half : an === 'end' ? (W - 3) : (W - 3) - est;
      var xx = lo <= hi ? Math.min(Math.max(x, lo), hi) : Math.min(Math.max(x, 3), W - 3);
      return [xx, txt, fs];
    }
    function tx(x, y, s, c, an, bold, sz, cw) { if (s == null || String(s).trim() === '') return ''; var ft = fitTxtW(x, s, an, sz, cw); return '<text x="' + f2(ft[0]) + '" y="' + f2(y) + '" direction="ltr" font-family="sans-serif" font-size="' + ft[2] + '"' + (bold ? ' font-weight="700"' : '') + ' fill="' + c + '" text-anchor="' + an + '" style="font:' + (bold ? '700 ' : '400 ') + ft[2] + 'px sans-serif;direction:ltr;unicode-bidi:embed">' + esc(ft[1]) + '</text>'; }
    function poly(pts, c) { return '<polygon points="' + pts + '" fill="' + c + '"/>'; }
    function arR(x, y, c) { return poly(f2(x) + ',' + f2(y) + ' ' + f2(x - 8) + ',' + f2(y - 4.5) + ' ' + f2(x - 8) + ',' + f2(y + 4.5), c || ST); }
    function arL(x, y) { return poly(f2(x) + ',' + f2(y) + ' ' + f2(x + 8) + ',' + f2(y - 4.5) + ' ' + f2(x + 8) + ',' + f2(y + 4.5), ST); }
    function arU(x, y, c) { return poly(f2(x) + ',' + f2(y) + ' ' + f2(x - 4.5) + ',' + f2(y + 8) + ' ' + f2(x + 4.5) + ',' + f2(y + 8), c || ST); }
    function nm(v) { return (v === Math.round(v)) ? String(v) : v.toFixed(1); }
    function stepOf(span) { var st = num(X.step, 0); if (st > 0) return st; return span <= 12 ? 1 : span <= 30 ? 2 : span <= 60 ? 5 : 10; }
    function clamp(v, a, b) { return Math.max(a, Math.min(b, v)); }
    function xs(k, fb) { var v = faEn(X[k]); return (v == null || String(v).trim() === '') ? (fb || '') : String(v); }
    function xl(k) { return xs(k, '').split(/[,،;؛\n]+/).map(function (s) { return s.trim(); }).filter(Boolean); }
    function pairs(s) { var out = [], re = /(-?\d+(?:\.\d+)?)\s*[,،]\s*(-?\d+(?:\.\d+)?)/g, m; while ((m = re.exec(s))) out.push([parseFloat(m[1]), parseFloat(m[2])]); return out; }
    function dot(x, y, c, r) { return '<circle cx="' + f2(x) + '" cy="' + f2(y) + '" r="' + f2(r || 4) + '" fill="' + (c || PT) + '" stroke="#fff" stroke-width="1.2"/>'; }
    function oDot(x, y) { return '<circle cx="' + f2(x) + '" cy="' + f2(y) + '" r="4.5" fill="#fff" stroke="' + PT + '" stroke-width="1.8"/>'; }
    function gcd(a, b) { return b === 0 ? Math.abs(a) : gcd(b, a % b); }
    var s = title ? tx(180, 14, title, '#1a2433', 'middle', true, 13) : '';
    var i, k, d, v, x, y;
    function numberBase(xmin, xmax, step, yy, labels, den, decimals) {
      var px = function (q) { return AL + (q - xmin) / (xmax - xmin) * (AR - AL); };
      s += ln(AL - 12, yy, AR + 12, yy, ST, 2) + arR(AR + 12, yy) + arL(AL - 12, yy);
      for (v = Math.ceil(xmin / step - 1e-4) * step; v <= xmax + 1e-4; v += step) {
        x = px(v); var z = Math.abs(v) < 1e-4;
        s += ln(x, yy - (z ? 10 : 7), x, yy + (z ? 10 : 7), ST, z ? 2 : 1.4);
        if (labels) s += tx(x, yy + 24, decimals >= 0 ? v.toFixed(decimals) : nm(v), MU, 'middle', false, 11);
        if (den > 1) for (k = 1; k < den; k++) { var fx = v + step * k / den; if (fx > xmax + 1e-4) break; var xx = px(fx); s += ln(xx, yy - 4, xx, yy + 4, ST, 1); if (labels) { var g = gcd(k, den), base = Math.round(v); s += tx(xx, yy + 21, (base === 0 ? '' : base + ' ') + (k / g) + '/' + (den / g), MU, 'middle', false, 8); } }
      }
      return px;
    }
    function cartBase(xmin, xmax, ymin, ymax, sx, sy, grid, labels, quads) {
      var px = function (q) { return AL + (q - xmin) / (xmax - xmin) * (AR - AL); }, py = function (q) { return AB - (q - ymin) / (ymax - ymin) * (AB - AT); };
      var ox = clamp(px(0), AL, AR), oy = clamp(py(0), AT, AB), g;
      if (grid) { for (g = Math.ceil(xmin / sx) * sx; g <= xmax + 1e-4; g += sx) s += ln(px(g), AT, px(g), AB, GR, 0.8); for (g = Math.ceil(ymin / sy) * sy; g <= ymax + 1e-4; g += sy) s += ln(AL, py(g), AR, py(g), GR, 0.8); }
      s += ln(AL - 8, oy, AR + 8, oy, ST, 1.8) + arR(AR + 8, oy) + ln(ox, AB + 8, ox, AT - 8, ST, 1.8) + arU(ox, AT - 8);
      s += tx(AR + 4, oy - 8, 'x', ST, 'end', true, 12) + tx(ox + 8, AT - 2, 'y', ST, 'start', true, 12);
      for (g = Math.ceil(xmin / sx) * sx; g <= xmax + 1e-4; g += sx) if (Math.abs(g) > 1e-4) { s += ln(px(g), oy - 4, px(g), oy + 4, ST, 1.2); if (labels) s += tx(px(g), oy + 15, nm(g), MU, 'middle', false, 10); }
      for (g = Math.ceil(ymin / sy) * sy; g <= ymax + 1e-4; g += sy) if (Math.abs(g) > 1e-4) { s += ln(ox - 4, py(g), ox + 4, py(g), ST, 1.2); if (labels) s += tx(ox - 7, py(g) + 4, nm(g), MU, 'end', false, 10); }
      if (labels) s += tx(ox - 6, oy + 14, '0', MU, 'end', false, 10);
      if (quads) { s += tx((ox + AR) / 2, (AT + oy) / 2, 'I', '#8a94a6', 'middle', true, 22) + tx((AL + ox) / 2, (AT + oy) / 2, 'II', '#8a94a6', 'middle', true, 22) + tx((AL + ox) / 2, (oy + AB) / 2, 'III', '#8a94a6', 'middle', true, 22) + tx((ox + AR) / 2, (oy + AB) / 2, 'IV', '#8a94a6', 'middle', true, 22); }
      return [px, py];
    }
    if (t === 'axnumpts' || t === 'axnumint' || t === 'axnumineq' || t === 'axnumfrac' || t === 'axnumdec' || t === 'axnumblank') {
      var dec = t === 'axnumdec', xmin = num(X.xmin, dec ? 0 : -5), xmax = num(X.xmax, dec ? 1 : 5); if (xmax <= xmin) xmax = xmin + (dec ? 0.5 : 2);
      var step = dec ? (num(X.step, 0) > 0 ? num(X.step, 0) : 0.1) : stepOf(xmax - xmin), yy = 140;
      var px = numberBase(xmin, xmax, step, yy, t !== 'axnumblank', t === 'axnumfrac' ? clamp(Math.floor(num(X.den, 4)), 2, 12) : 0, dec ? 1 : -1);
      var labs = xl('labs');
      if (t === 'axnumpts') { var pts = xl('pts'); if (!pts.length) pts = ['-2', '1.5', '4']; pts.forEach(function (p, j) { var q = parseFloat(p); if (isFinite(q) && q >= xmin && q <= xmax) s += dot(px(q), yy) + tx(px(q), yy - 14, labs[j] || nm(q), PT, 'middle', true, 11); }); }
      else if (t === 'axnumint') { var lo = clamp(num(X.lo, -2), xmin, xmax), hi = clamp(num(X.hi, 3), xmin, xmax), lc = num(X.lc, 1) > 0, hc = num(X.hc, 0) > 0; s += ln(px(lo), yy, px(hi), yy, PT, 4) + (lc ? dot(px(lo), yy) : oDot(px(lo), yy)) + (hc ? dot(px(hi), yy) : oDot(px(hi), yy)); s += tx((px(lo) + px(hi)) / 2, yy - 16, xs('labs') || ((lc ? '[' : '(') + nm(lo) + ' , ' + nm(hi) + (hc ? ']' : ')')), PT, 'middle', true, 11); }
      else if (t === 'axnumineq') { var x0 = clamp(num(X.x0, 1), xmin, xmax), right = num(X.dir, 1) >= 0, closed = num(X.cl, 0) > 0; s += ln(px(x0), yy, right ? AR + 12 : AL - 12, yy, PT, 4) + (closed ? dot(px(x0), yy) : oDot(px(x0), yy)); s += tx(px(x0), yy - 16, xs('labs') || ((right ? (closed ? 'x ≥ ' : 'x > ') : (closed ? 'x ≤ ' : 'x < ')) + nm(x0)), PT, 'middle', true, 11); }
      return wrap(s);
    }
    if (t === 'axnumlog') {
      var nDec = clamp(Math.floor(num(X.xmax, 4)), 1, 8), y1 = 140, decW = (AR - AL) / nDec;
      s += ln(AL - 12, y1, AR + 12, y1, ST, 2) + arR(AR + 12, y1);
      for (d = 0; d <= nDec; d++) { x = AL + d * decW; s += ln(x, y1 - 9, x, y1 + 9, ST, 1.8) + tx(x, y1 + 26, '10', MU, 'middle', false, 11) + tx(x + 8, y1 + 20, String(d), MU, 'start', false, 8); if (d < nDec) for (k = 2; k <= 9; k++) { var lx = x + decW * Math.log10(k); s += ln(lx, y1 - 4, lx, y1 + 4, ST, 0.9); } }
      return wrap(s);
    }
    if (t === 'axnumtwo') {
      var txmin = num(X.xmin, 0), txmax = num(X.xmax, 10); if (txmax <= txmin) txmax = txmin + 2;
      var tst = stepOf(txmax - txmin), m = num(X.m, 2), b = num(X.b, 0);
      var tpx = numberBase(txmin, txmax, tst, 105, true, 0, -1);
      s += tx(AL - 14, 92, xs('s1', 'A'), ST, 'end', true, 11);
      var y2 = 185; s += ln(AL - 12, y2, AR + 12, y2, ST, 2) + arR(AR + 12, y2) + arL(AL - 12, y2);
      for (v = Math.ceil(txmin / tst - 1e-4) * tst; v <= txmax + 1e-4; v += tst) { x = tpx(v); s += ln(x, y2 - 7, x, y2 + 7, ST, 1.4) + tx(x, y2 + 24, nm(m * v + b), MU, 'middle', false, 11) + ln(x, 117, x, y2 - 12, GR, 0.8); }
      s += tx(AL - 14, y2 - 13, xs('s2', 'B'), ST, 'end', true, 11);
      return wrap(s);
    }
    if (t === 'axxypts' || t === 'axxyline' || t === 'axxyvec' || t === 'axxycirc' || t === 'axxynogrid' || t === 'axxyquads' || t === 'axxyblank') {
      var cxmin = num(X.xmin, -5), cxmax = num(X.xmax, 5); if (cxmax <= cxmin) cxmax = cxmin + 2;
      var cymin = num(X.ymin, -4), cymax = num(X.ymax, 4); if (cymax <= cymin) cymax = cymin + 2;
      // V322 — اگر نقطه/بردار/دایره بیرون از بازهٔ محور باشد، بازه خودکار گسترده می‌شود تا شکل ناپدید نشود.
      if (t === 'axxyvec') {
        [num(X.x1, 0), num(X.x2, 3)].forEach(function (vxv) { cxmin = Math.min(cxmin, vxv - 1); cxmax = Math.max(cxmax, vxv + 1); });
        [num(X.y1, 0), num(X.y2, 2)].forEach(function (vyv) { cymin = Math.min(cymin, vyv - 1); cymax = Math.max(cymax, vyv + 1); });
      } else if (t === 'axxycirc') {
        var ecx = num(X.cx, 1), ecy = num(X.cy, 1), ecr = Math.abs(num(X.r, 2));
        cxmin = Math.min(cxmin, ecx - ecr - 1); cxmax = Math.max(cxmax, ecx + ecr + 1);
        cymin = Math.min(cymin, ecy - ecr - 1); cymax = Math.max(cymax, ecy + ecr + 1);
      }
      var csx = stepOf(cxmax - cxmin), csy = stepOf(cymax - cymin);
      var pp = cartBase(cxmin, cxmax, cymin, cymax, csx, csy, t !== 'axxynogrid', t !== 'axxyblank', t === 'axxyquads'), cpx = pp[0], cpy = pp[1];
      if (t === 'axxypts') { var pl = xl('labs'); pairs(xs('pts', '1,2;-3,1;2,-2')).forEach(function (p, j) { if (p[0] >= cxmin && p[0] <= cxmax && p[1] >= cymin && p[1] <= cymax) s += dot(cpx(p[0]), cpy(p[1])) + tx(cpx(p[0]) + 6, cpy(p[1]) - 6, pl[j] || ('(' + nm(p[0]) + ', ' + nm(p[1]) + ')'), PT, 'start', true, 10); }); }
      else if (t === 'axxyline') { var lm = num(X.m, 1), lb = num(X.b, 1), ptsS = ''; for (i = 0; i <= 120; i++) { x = cxmin + (cxmax - cxmin) * i / 120; var yv = lm * x + lb; if (yv >= cymin && yv <= cymax) ptsS += f2(cpx(x)) + ',' + f2(cpy(yv)) + ' '; } s += '<polyline points="' + ptsS.trim() + '" fill="none" stroke="' + PT + '" stroke-width="2"/>'; s += tx(AR - 4, AT + 12, xs('labs') || ('y = ' + nm(lm) + 'x ' + (lb < 0 ? '− ' + nm(-lb) : '+ ' + nm(lb))), PT, 'end', true, 11); }
      else if (t === 'axxyvec') { var vx1 = num(X.x1, 0), vy1 = num(X.y1, 0), vx2 = num(X.x2, 3), vy2 = num(X.y2, 2), ax = cpx(vx1), ay = cpy(vy1), bx = cpx(vx2), by = cpy(vy2); s += ln(ax, ay, bx, by, PT, 2.2); var ang = Math.atan2(by - ay, bx - ax); s += poly(f2(bx) + ',' + f2(by) + ' ' + f2(bx - 10 * Math.cos(ang - 0.45)) + ',' + f2(by - 10 * Math.sin(ang - 0.45)) + ' ' + f2(bx - 10 * Math.cos(ang + 0.45)) + ',' + f2(by - 10 * Math.sin(ang + 0.45)), PT) + dot(ax, ay, PT, 3); s += tx(bx + 8, by - 4, xs('labs') || ('v = (' + nm(vx2 - vx1) + ', ' + nm(vy2 - vy1) + ')'), PT, 'start', true, 11); }
      else if (t === 'axxycirc') { var ccx = num(X.cx, 1), ccy = num(X.cy, 1), cr = Math.max(0.1, num(X.r, 2)), rx = cr / (cxmax - cxmin) * (AR - AL), ry = cr / (cymax - cymin) * (AB - AT); s += '<ellipse cx="' + f2(cpx(ccx)) + '" cy="' + f2(cpy(ccy)) + '" rx="' + f2(rx) + '" ry="' + f2(ry) + '" fill="rgba(214,51,108,.08)" stroke="' + PT + '" stroke-width="2"/>' + dot(cpx(ccx), cpy(ccy), PT, 3) + ln(cpx(ccx), cpy(ccy), cpx(ccx) + rx, cpy(ccy), PT, 1.2, true) + tx(cpx(ccx) + rx / 2, cpy(ccy) - 6, 'r = ' + nm(cr), PT, 'middle', true, 10); }
      return wrap(s);
    }
    if (t === 'axdual') {
      var dxmin = num(X.xmin, 0), dxmax = num(X.xmax, 10); if (dxmax <= dxmin) dxmax = dxmin + 2;
      var dymin = num(X.ymin, 0), dymax = num(X.ymax, 100); if (dymax <= dymin) dymax = dymin + 2;
      var dlo = num(X.lo, 0), dhi = num(X.hi, 10); if (dhi <= dlo) dhi = dlo + 2;
      var dsx = stepOf(dxmax - dxmin), ny = 5, l = AL + 10, r = AR - 10, dpx = function (q) { return l + (q - dxmin) / (dxmax - dxmin) * (r - l); };
      for (i = 0; i <= ny; i++) { y = AB - (AB - AT) * i / ny; s += ln(l, y, r, y, GR, 0.8) + tx(l - 6, y + 4, nm(dymin + (dymax - dymin) * i / ny), '#2563eb', 'end', false, 10) + tx(r + 6, y + 4, nm(dlo + (dhi - dlo) * i / ny), '#dc2626', 'start', false, 10); }
      s += ln(l, AB, r, AB, ST, 1.8) + ln(r, AB, r + 8, AB, ST, 1.8) + arR(r + 8, AB);
      s += ln(l, AB, l, AT - 8, '#2563eb', 1.8) + arU(l, AT - 8, '#2563eb') + ln(r, AB, r, AT - 8, '#dc2626', 1.8) + arU(r, AT - 8, '#dc2626');
      for (v = Math.ceil(dxmin / dsx) * dsx; v <= dxmax + 1e-4; v += dsx) s += ln(dpx(v), AB - 4, dpx(v), AB + 4, ST, 1.2) + tx(dpx(v), AB + 15, nm(v), MU, 'middle', false, 10);
      s += tx(l + 6, AT - 2, xs('s1', 'y₁'), '#2563eb', 'start', true, 11) + tx(r - 6, AT - 2, xs('s2', 'y₂'), '#dc2626', 'end', true, 11) + tx(r + 8, AB - 8, 'x', ST, 'end', true, 12);
      return wrap(s);
    }
    if (t === 'axlog' || t === 'axloglog') {
      var both = t === 'axloglog', lny = clamp(Math.floor(num(X.ymax, 3)), 1, 6), lnx = both ? clamp(Math.floor(num(X.xmax, 3)), 1, 6) : Math.max(1, num(X.xmax, 10));
      s += '<rect x="' + AL + '" y="' + AT + '" width="' + (AR - AL) + '" height="' + (AB - AT) + '" fill="none" stroke="' + ST + '" stroke-width="1.4"/>';
      var decH = (AB - AT) / lny;
      for (d = 0; d <= lny; d++) { y = AB - d * decH; s += ln(AL, y, AR, y, ST, 1) + tx(AL - 6, y + 4, '10', MU, 'end', false, 10) + tx(AL - 5, y - 3, String(d), MU, 'start', false, 7); if (d < lny) for (k = 2; k <= 9; k++) { var ly = y - decH * Math.log10(k); s += ln(AL, ly, AR, ly, GR, 0.7); } }
      if (both) { var dW = (AR - AL) / lnx; for (d = 0; d <= lnx; d++) { x = AL + d * dW; s += ln(x, AT, x, AB, ST, 1) + tx(x, AB + 16, '10', MU, 'middle', false, 10) + tx(x + 7, AB + 11, String(d), MU, 'start', false, 7); if (d < lnx) for (k = 2; k <= 9; k++) { var lxx = x + dW * Math.log10(k); s += ln(lxx, AT, lxx, AB, GR, 0.7); } } }
      else { var lsx = stepOf(lnx); for (v = 0; v <= lnx + 1e-4; v += lsx) { x = AL + v / lnx * (AR - AL); s += ln(x, AT, x, AB, GR, 0.8) + tx(x, AB + 16, nm(v), MU, 'middle', false, 10); } }
      s += tx(AR, AB + 30, 'x', ST, 'end', true, 12) + tx(AL - 24, AT - 8, 'y', ST, 'start', true, 12);
      return wrap(s);
    }
    if (t === 'axtime') {
      var tl = xl('labs'); if (!tl.length) tl = ['۸:۰۰', '۹:۰۰', '۱۰:۰۰', '۱۱:۰۰', '۱۲:۰۰'];
      var ty = 140, n = tl.length; s += ln(AL - 6, ty, AR + 12, ty, ST, 2) + arR(AR + 12, ty);
      tl.forEach(function (lab, j) { var xx = n === 1 ? AL : AL + (AR - AL) * j / (n - 1); s += ln(xx, ty - 8, xx, ty + 8, ST, 1.5) + tx(xx, ty + 24, lab, MU, 'middle', false, 10); if (j < n - 1) for (k = 1; k <= 3; k++) { var mx = xx + (AR - AL) / (n - 1) * k / 4; s += ln(mx, ty - 3, mx, ty + 3, ST, 0.9); } });
      s += tx(AR + 12, ty - 10, xs('s1', 't'), ST, 'end', true, 12);
      return wrap(s);
    }
    if (t === 'axpolpts') {
      var rings = clamp(Math.floor(num(X.xmax, 4)), 1, 12), pcx = 180, pcy = (AT + AB) / 2, rMax = Math.min(AR - AL, AB - AT) / 2 - 6;
      for (i = 1; i <= rings; i++) { var rr = rMax * i / rings; s += '<circle cx="' + pcx + '" cy="' + f2(pcy) + '" r="' + f2(rr) + '" fill="none" stroke="' + GR + '" stroke-width="0.9"/>' + tx(pcx + rr + 2, pcy - 3, String(i), MU, 'start', false, 9); }
      var labels = ['0°', '30°', '60°', '90°', '120°', '150°', '180°', '210°', '240°', '270°', '300°', '330°'];
      for (k = 0; k < 12; k++) { var a = k * Math.PI / 6; s += ln(pcx, pcy, pcx + rMax * Math.cos(a), pcy - rMax * Math.sin(a), k % 3 === 0 ? ST : GR, k % 3 === 0 ? 1.4 : 0.8) + tx(pcx + (rMax + 13) * Math.cos(a), pcy - (rMax + 13) * Math.sin(a) + 3, labels[k], MU, 'middle', false, 9); }
      s += arR(pcx + rMax, pcy);
      var plabs = xl('labs'); pairs(xs('pts', '2,30;3,120')).forEach(function (p, j) { var prr = clamp(p[0] / rings * rMax, 0, rMax + 20), pa = p[1] * Math.PI / 180, qx = pcx + prr * Math.cos(pa), qy = pcy - prr * Math.sin(pa); s += ln(pcx, pcy, qx, qy, PT, 1.2) + dot(qx, qy) + tx(qx + 6, qy - 6, plabs[j] || ('(' + nm(p[0]) + ', ' + nm(p[1]) + '°)'), PT, 'start', true, 10); });
      return wrap(s);
    }
    if (t === 'ax3dpt') {
      var cx = 170, cy = 150, len = 100, zx = cx - len * 0.62, zy = cy + len * 0.62;
      s += ln(cx - 30, cy, cx + len, cy, ST, 1.8) + arR(cx + len, cy) + ln(cx, cy + 30, cx, cy - len, ST, 1.8) + arU(cx, cy - len) + ln(cx + 20, cy - 20, zx, zy, ST, 1.8) + poly(f2(zx) + ',' + f2(zy) + ' ' + f2(zx + 9) + ',' + f2(zy - 1) + ' ' + f2(zx + 2) + ',' + f2(zy - 9), ST);
      s += tx(cx + len + 4, cy + 4, 'x', ST, 'start', true, 12) + tx(cx + 6, cy - len - 2, 'y', ST, 'start', true, 12) + tx(zx - 4, zy + 12, 'z', ST, 'end', true, 12) + tx(cx - 6, cy + 14, 'O', MU, 'end', false, 10);
      var st3 = Math.max(0.5, num(X.step, 1)), n3 = clamp(Math.floor(num(X.xmax, 4)), 1, 10);
      for (i = 1; i <= n3; i++) { d = len * i / (n3 + 0.6); s += ln(cx + d, cy - 3, cx + d, cy + 3, ST, 1.1) + tx(cx + d, cy + 13, nm(i * st3), MU, 'middle', false, 9) + ln(cx - 3, cy - d, cx + 3, cy - d, ST, 1.1) + tx(cx - 6, cy - d + 3, nm(i * st3), MU, 'end', false, 9); var t3x = cx - d * 0.62, t3y = cy + d * 0.62; s += ln(t3x - 2.5, t3y - 2.5, t3x + 2.5, t3y + 2.5, ST, 1.1); }
      // V322 — نقطهٔ بیرون از بازهٔ محورها بیرون از بوم رسم می‌شد.
      var axMax3 = n3 * st3;
      var unit = len / (n3 + 0.6) / st3, p3x = clamp(num(X.x1, 3), -axMax3, axMax3) * unit, p3y = clamp(num(X.y1, 2), -axMax3, axMax3) * unit, p3z = clamp(num(X.z1, 2), -axMax3, axMax3) * unit, b3x = cx + p3x - p3z * 0.62, b3y = cy + p3z * 0.62, q3x = b3x, q3y = b3y - p3y;
      s += ln(cx + p3x, cy, b3x, b3y, PT, 1, true) + ln(cx - p3z * 0.62, cy + p3z * 0.62, b3x, b3y, PT, 1, true) + ln(b3x, b3y, q3x, q3y, PT, 1, true) + ln(cx, cy - p3y, q3x, q3y, PT, 1, true) + ln(cx, cy, q3x, q3y, PT, 1.6) + dot(q3x, q3y);
      s += tx(q3x + 7, q3y - 6, xs('labs') || ('P(' + nm(num(X.x1, 3)) + ', ' + nm(num(X.y1, 2)) + ', ' + nm(num(X.z1, 2)) + ')'), PT, 'start', true, 10);
      return wrap(s);
    }
    // ---------------------------------------------------------------- V304 — ابزار ریاضی ابتدایی
    // پورت یک‌به‌یک PrimaryMathSvgRenderer.kt (همان مختصات/رنگ‌ها).
    var ACC = '#6c63f5', ACCS = 'rgba(108,99,245,.18)', GRN = '#27c4a8', GLD = '#f0a202';
    function rc(x, y, w, h, fill, st, sw, rx) { return '<rect x="' + f2(x) + '" y="' + f2(y) + '" width="' + f2(w) + '" height="' + f2(h) + '"' + (rx ? ' rx="' + f2(rx) + '"' : '') + ' fill="' + fill + '" stroke="' + st + '" stroke-width="' + f2(sw) + '"/>'; }
    function lnr(x1, y1, x2, y2, c, w) { return '<line x1="' + f2(x1) + '" y1="' + f2(y1) + '" x2="' + f2(x2) + '" y2="' + f2(y2) + '" stroke="' + c + '" stroke-width="' + f2(w) + '" stroke-linecap="round"/>'; }
    function grp(v) { var str = String(v), out = '', j; for (j = 0; j < str.length; j++) { var idx = str.length - j; out += str[j]; if (idx > 1 && (idx - 1) % 3 === 0) out += '،'; } return out; }
    function ilist(k, fb) { return xs(k, fb).split(/[,،;؛\n]+/).map(function (q) { return parseInt(q.trim(), 10); }).filter(function (q) { return !isNaN(q); }).slice(0, 8); }
    if (t === 'axtally') {
      var n = clamp(Math.floor(num(X.n, 7)), 0, 75), groups = Math.floor((n + 4) / 5), g;
      for (g = 0; g < groups; g++) {
        var gx = 32 + (g % 5) * 64, gy = 44 + Math.floor(g / 5) * 74, cnt = Math.min(5, n - g * 5);
        for (i = 0; i < Math.min(cnt, 4); i++) s += lnr(gx + i * 11, gy, gx + i * 11, gy + 40, ST, 3);
        if (cnt === 5) s += lnr(gx - 6, gy + 34, gx + 40, gy + 6, PT, 3);
      }
      var tlMode = xs('label', '1');
      if (tlMode === '2') s += tx(180, 262, 'تعداد: ؟', PT, 'middle', true, 14);
      else if (tlMode !== '0') s += tx(180, 262, 'تعداد: ' + n, MU, 'middle', false, 13);
      return wrap(s);
    }
    if (t === 'axbase10') {
      var nbClean = faEn(String(X.n == null ? '134' : X.n)).replace(/\D/g, '');
      var nbParsed = nbClean ? parseInt(nbClean.slice(0, 12), 10) : 134;
      var nb = clamp(isFinite(nbParsed) ? nbParsed : 134, 0, 999999999), bd = [], bq = nb, bk, bLbMode = xs('label', '1');
      for (bk = 0; bk < 9; bk++) { bd.push(bq % 10); bq = Math.floor(bq / 10); }
      // (a,b,h) واحد؛ نمایش سه‌بعدی؛ مقیاس رتبه‌های بالاتر نمایشی است
      var BSHP = [[1, 1, 1, '#ffd54f', 'یکان', 6], [1, 10, 1, '#66bb6a', 'دهگان', 3], [10, 10, 1, '#42a5f5', 'صدگان', 2.4], [10, 10, 10, '#ab47bc', 'یکان هزار', 1.8], [1, 10, 10, '#ef5350', 'دهگان هزار', 2], [10, 10, 10, '#26a69a', 'صدگان هزار', 1.8], [10, 10, 10, '#ff7043', 'یکان میلیون', 1.8], [10, 10, 10, '#8d6e63', 'دهگان میلیون', 1.8], [10, 10, 10, '#5c6bc0', 'صدگان میلیون', 1.8]];
      var BAVAIL = 262, bp, bn, bj; /* V310.9 — جای برچسب‌های رتبه (تا x≈90) */
      function bshade(hex, f) { var r = parseInt(hex.substr(1, 2), 16) * f, g = parseInt(hex.substr(3, 2), 16) * f, b2 = parseInt(hex.substr(5, 2), 16) * f; return 'rgb(' + Math.round(Math.min(255, r)) + ',' + Math.round(Math.min(255, g)) + ',' + Math.round(Math.min(255, b2)) + ')'; }
      function isoBox(L, bot, a, b, h, k, col) {
        var ox = L + b * 0.866 * k, oy = bot - (a + b) * 0.5 * k;
        function P(x, y, z) { return f2(ox + (x - y) * 0.866 * k) + ',' + f2(oy + (x + y) * 0.5 * k - z * k); }
        var out = '';
        out += poly(P(a, 0, 0) + ' ' + P(a, b, 0) + ' ' + P(a, b, h) + ' ' + P(a, 0, h), bshade(col, 0.72));
        out += poly(P(0, b, 0) + ' ' + P(a, b, 0) + ' ' + P(a, b, h) + ' ' + P(0, b, h), bshade(col, 0.86));
        out += poly(P(0, 0, h) + ' ' + P(a, 0, h) + ' ' + P(a, b, h) + ' ' + P(0, b, h), col);
        return out;
      }
      // V310.9 — برچسب‌های رتبه و تعداد: جهت ltr تا در بیدی RTL از کادر بیرون نزنند
      function ltx(x, y, s, c, sz) { return '<text x="' + f2(x) + '" y="' + f2(y) + '" direction="ltr" font-family="sans-serif" font-size="' + sz + '" font-weight="700" fill="' + c + '" text-anchor="start">' + esc(s) + '</text>'; }
      for (bp = 0; bp < 9; bp++) {
        var ptop = 4 + (8 - bp) * 36, pbot = ptop + 33, shp = BSHP[bp], pa = shp[0], pb = shp[1], ph = shp[2], pcol = shp[3];
        bn = bd[bp];
        s += ltx(4, ptop + 14, shp[4], ST, 10) + ((bn === 0 || bLbMode === '2' || bLbMode === '0') ? '' : ltx(4, ptop + 28, '× ' + bn, MU, 11));
        if (bn === 0) continue;
        var kh = 32 / ((pa + pb) * 0.5 + ph), kw = (BAVAIL - 4 * bn) / (bn * (pa + pb) * 0.866), kk = Math.min(shp[5], kh, kw), slot = 92;
        for (bj = 0; bj < bn; bj++) {
          s += isoBox(slot, pbot, pa, pb, ph, kk, pcol);
          slot += (pa + pb) * 0.866 * kk + 4;
        }
      }
      if (bLbMode === '2') s += tx(180, 362, 'عدد: ؟', PT, 'middle', true, 14);
      else if (bLbMode !== '0') s += tx(180, 362, 'عدد: ' + grp(nb), ST, 'middle', true, 13);
      return wrap(s, 370);
    }
    if (t === 'axplace') {
      var pClean = function (v) { return String(xs(v, '')).replace(/\D/g, '').replace(/^0+(?=\d)/, ''); };
      var pRowsN = clamp(Math.floor(num(X.rows, 3)), 1, 6), pBlank = xs('blank', '0') === '1';
      var pVals = [pClean('n') || '0', pClean('n2'), pClean('n3')], pOps = [xs('op2', ''), xs('op3', '')];
      var pPer = [['میلیارد', '#d62828'], ['میلیون', '#e0a800'], ['هزار', '#2e9e44'], ['یکی', '#7b3fbf']];
      var pCw = 326 / 12, pX0 = 30, pPw = 3 * pCw, pRh = 40, pTop = 80, pBot = pTop + pRh * pRowsN, pq, pj, pr;
      var pH = Math.max(280, pBot + 60);
      s += rc(pX0, 22, 12 * pCw, pBot - 22 + 4, '#f7f9fc', ST, 1.2);
      for (pq = 0; pq < 4; pq++) {
        var ppx = pX0 + pq * pPw, pgrp = 3 - pq;
        s += tx(ppx + pPw / 2, 40, pPer[pq][0], pPer[pq][1], 'middle', true, 13);
        s += rc(ppx, 48, pPw, 26, ACCS, pPer[pq][1], 1.6);
        for (pj = 0; pj < 3; pj++) {
          var pcx = ppx + pj * pCw;
          s += tx(pcx + pCw / 2, 66, ['ص', 'د', 'ی'][pj], ST, 'middle', true, 11);
          if (pj > 0) s += lnr(pcx, 48, pcx, pBot, '#8fa3c7', 0.8);
          for (pr = 0; pr < pRowsN; pr++) {
            s += rc(pcx, pTop + pr * pRh, pCw, pRh, '#fff', '#8fa3c7', 0.8);
            var pStr = pr < 3 ? pVals[pr] : '', pIdx = pStr.length - 1 - (pgrp * 3 + (2 - pj)), pDg = pIdx >= 0 ? pStr[pIdx] : null;
            if (!(pr === 0 && pBlank) && pDg != null) s += tx(pcx + pCw / 2, pTop + pr * pRh + 27, pDg, PT, 'middle', true, 22);
          }
        }
        s += rc(ppx, 48, pPw, pBot - 48, 'none', pPer[pq][1], 2);
      }
      for (pr = 1; pr < pRowsN && pr < 3; pr++) {
        var pOp = pOps[pr - 1] === '-' ? '−' : pOps[pr - 1];
        if (pOp === '+' || pOp === '−') s += tx(16, pTop + pr * pRh + 27, pOp, ST, 'middle', true, 22);
      }
      if (pVals[0].length > 12 || pVals[1].length > 12 || pVals[2].length > 12) s += tx(180, pBot + 22, 'بیشتر از ۱۲ رقم: فقط ۱۲ رقم آخر نمایش داده می‌شود', MU, 'middle', false, 11);
      return wrap(s, pH);
    }
    if (t === 'axclock') {
      var ch0 = ((Math.floor(num(X.h, 3)) % 12) + 12) % 12, cm = clamp(Math.floor(num(X.m, 0)), 0, 59), showN = xs('nums', '1') !== '0', ccx = 180, ccy = 150, cr = 104, a;
      s += '<circle cx="180" cy="150" r="104" fill="#fff" stroke="' + ST + '" stroke-width="3"/>';
      for (i = 0; i < 60; i++) { a = i * 6 * Math.PI / 180; var big = i % 5 === 0, r1 = big ? cr - 12 : cr - 6; s += lnr(ccx + r1 * Math.sin(a), ccy - r1 * Math.cos(a), ccx + (cr - 2) * Math.sin(a), ccy - (cr - 2) * Math.cos(a), ST, big ? 2.2 : 1); }
      if (showN) for (i = 1; i <= 12; i++) { a = i * 30 * Math.PI / 180; s += tx(ccx + (cr - 26) * Math.sin(a), ccy - (cr - 26) * Math.cos(a) + 5, String(i), ST, 'middle', true, 14); }
      var ah = ((ch0 % 12) * 30 + cm * 0.5) * Math.PI / 180, am = cm * 6 * Math.PI / 180;
      s += lnr(ccx, ccy, ccx + cr * 0.5 * Math.sin(ah), ccy - cr * 0.5 * Math.cos(ah), ST, 6) + lnr(ccx, ccy, ccx + cr * 0.78 * Math.sin(am), ccy - cr * 0.78 * Math.cos(am), PT, 3.5);
      s += '<circle cx="180" cy="150" r="5" fill="' + ST + '"/>';
      var clkLb = xs('label', '0');
      if (clkLb === '1') s += tx(ccx, 272, (ch0 === 0 ? 12 : ch0) + ':' + (cm < 10 ? '0' : '') + cm, MU, 'middle', false, 13);
      else if (clkLb === '2') s += tx(ccx, 272, 'ساعت = ؟ : ؟', PT, 'middle', true, 14);
      return wrap(s);
    }
    if (t === 'axruler') {
      var len = clamp(Math.floor(num(X.len, 10)), 2, 50), ra = parseFloat(xs('a')), rb = parseFloat(xs('b')), rx0 = 24, rx1 = 336, ry0 = 110, unit = (rx1 - rx0) / len, showMm = len <= 15;
      var lstep = len <= 15 ? 1 : (len <= 30 ? 2 : 5);
      s += rc(rx0 - 10, ry0, rx1 - rx0 + 20, 70, 'rgba(240,162,2,.16)', GLD, 1.6);
      for (i = 0; i <= len; i++) {
        x = rx0 + i * unit; s += lnr(x, ry0, x, ry0 + (i % lstep === 0 ? 26 : 14), ST, 1.8);
        if (i % lstep === 0 || i === len) s += tx(x, ry0 + 46, String(i), ST, 'middle', false, len > 30 ? 10 : 12);
        if (showMm && i < len) for (k = 1; k < 10; k++) s += lnr(x + k * unit / 10, ry0, x + k * unit / 10, ry0 + (k === 5 ? 17 : 10), ST, k === 5 ? 1.2 : 0.8);
      }
      s += tx(rx1, ry0 + 64, 'cm', MU, 'middle', true, 11);
      if (!isNaN(ra) && !isNaN(rb)) {
        var xa = rx0 + clamp(ra, 0, len) * unit, xb = rx0 + clamp(rb, 0, len) * unit;
        s += lnr(xa, ry0 - 22, xb, ry0 - 22, PT, 4) + lnr(xa, ry0 - 32, xa, ry0 - 12, PT, 2) + lnr(xb, ry0 - 32, xb, ry0 - 12, PT, 2);
        var rSpan = Math.abs(clamp(rb, 0, len) - clamp(ra, 0, len)), rLb = xs('label', '1');
        var rSpanTxt = Math.abs(rSpan - Math.round(rSpan)) < 1e-4 ? String(Math.round(rSpan)) : rSpan.toFixed(1);
        if (rLb === '2') s += tx((xa + xb) / 2, ry0 - 30, '؟ cm', PT, 'middle', true, 13);
        else if (rLb !== '0') s += tx((xa + xb) / 2, ry0 - 30, rSpanTxt + ' cm', PT, 'middle', true, 12);
      }
      return wrap(s);
    }
    if (t === 'axfrac') {
      var den = clamp(Math.floor(num(X.den, 4)), 1, 25), nu = clamp(Math.floor(num(X.num, 1)), 0, den), shape = xs('shape', 'circle');
      var fillF = function (q) { return q < nu ? ACC : '#fff'; };
      if (shape === 'bar') { var bw = 300 / den; for (i = 0; i < den; i++) s += rc(30 + i * bw, 82, bw, 68, fillF(i), ST, 1.6); }
      else if (shape === 'square') {
        var scols = Math.ceil(Math.sqrt(den)), srows = Math.ceil(den / scols), scw = 166 / scols, sch = 166 / srows;
        for (i = 0; i < den; i++) s += rc(97 + (i % scols) * scw, 24 + Math.floor(i / scols) * sch, scw, sch, fillF(i), ST, 1.6);
      }
      else if (shape === 'ring') {
        if (den === 1) s += '<circle cx="180" cy="110" r="80" fill="' + fillF(0) + '" stroke="' + ST + '" stroke-width="1.8"/><circle cx="180" cy="110" r="40" fill="#fff" stroke="' + ST + '" stroke-width="1.8"/>';
        else for (i = 0; i < den; i++) {
          var ra0 = (i * 360 / den - 90) * Math.PI / 180, ra1 = ((i + 1) * 360 / den - 90) * Math.PI / 180, rlg = 360 / den > 180 ? 1 : 0;
          s += '<path d="M ' + f2(180 + 80 * Math.cos(ra0)) + ' ' + f2(110 + 80 * Math.sin(ra0)) + ' A 80 80 0 ' + rlg + ' 1 ' + f2(180 + 80 * Math.cos(ra1)) + ' ' + f2(110 + 80 * Math.sin(ra1)) + ' L ' + f2(180 + 40 * Math.cos(ra1)) + ' ' + f2(110 + 40 * Math.sin(ra1)) + ' A 40 40 0 ' + rlg + ' 0 ' + f2(180 + 40 * Math.cos(ra0)) + ' ' + f2(110 + 40 * Math.sin(ra0)) + ' Z" fill="' + fillF(i) + '" stroke="' + ST + '" stroke-width="1.8"/>';
        }
      }
      else if (den === 1) s += '<circle cx="180" cy="110" r="80" fill="' + (nu === 1 ? ACC : '#fff') + '" stroke="' + ST + '" stroke-width="1.8"/>';
      else for (i = 0; i < den; i++) {
        var a0 = (i * 360 / den - 90) * Math.PI / 180, a1 = ((i + 1) * 360 / den - 90) * Math.PI / 180, lg = 360 / den > 180 ? 1 : 0;
        s += '<path d="M 180 110 L ' + f2(180 + 80 * Math.cos(a0)) + ' ' + f2(110 + 80 * Math.sin(a0)) + ' A 80 80 0 ' + lg + ' 1 ' + f2(180 + 80 * Math.cos(a1)) + ' ' + f2(110 + 80 * Math.sin(a1)) + ' Z" fill="' + (i < nu ? ACC : '#fff') + '" stroke="' + ST + '" stroke-width="1.8"/>';
      }
      var frLb = xs('label', '1');
      if (frLb === '2') s += tx(180, 228, '؟', PT, 'middle', true, 18) + lnr(164, 236, 196, 236, ST, 2) + tx(180, 258, '؟', PT, 'middle', true, 18);
      else if (frLb !== '0') s += tx(180, 228, String(nu), ST, 'middle', true, 18) + lnr(164, 236, 196, 236, ST, 2) + tx(180, 258, String(den), ST, 'middle', true, 18);
      return wrap(s);
    }
    if (t === 'axmoney') {
      var notes = ilist('notes', '1000,2000,5000,10000'), coins = ilist('coins', '500,1000'), un = xs('unit', 'تومان'), mx = 18, my = 36, total = 0;
      for (i = 0; i < notes.length; i++) {
        if (i > 0 && i % 3 === 0) { mx = 18; my += 64; }
        s += rc(mx, my, 100, 50, 'rgba(39,196,168,.18)', GRN, 1.6, 8) + rc(mx + 6, my + 6, 88, 38, 'none', GRN, 0.8, 5) + tx(mx + 50, my + 24, grp(notes[i]), ST, 'middle', true, 14) + tx(mx + 50, my + 40, un, MU, 'middle', false, 10);
        mx += 112; total += notes[i];
      }
      my = notes.length ? my + 74 : 50; mx = 18;
      for (i = 0; i < coins.length; i++) {
        if (mx + 50 > 350) { mx = 18; my += 60; }
        s += '<circle cx="' + f2(mx + 24) + '" cy="' + f2(my + 24) + '" r="24" fill="rgba(240,162,2,.28)" stroke="' + GLD + '" stroke-width="2"/><circle cx="' + f2(mx + 24) + '" cy="' + f2(my + 24) + '" r="18" fill="none" stroke="' + GLD + '" stroke-width="0.8"/>' + tx(mx + 24, my + 28, grp(coins[i]), ST, 'middle', true, 11);
        mx += 60; total += coins[i];
      }
      var mnSum = xs('sum', '1');
      if (mnSum === '2') s += tx(180, 272, 'جمع: ؟ ' + un, PT, 'middle', true, 13);
      else if (mnSum === '1') s += tx(180, 272, 'جمع: ' + grp(total) + ' ' + un, PT, 'middle', true, 13);
      return wrap(s);
    }
    if (t === 'axabacus') {
      var cols = clamp(Math.floor(num(X.cols, 6)), 2, 6), an = clamp(Math.floor(num(X.n, 142536)), 0, 999999);
      var alabs = ['یکان', 'دهگان', 'صدگان', 'یکان هزار', 'دهگان هزار', 'صدگان هزار'];
      var acols = ['#ef5350', '#42a5f5', '#66bb6a', '#ab47bc', '#ffa726', '#26a69a'];
      var astep = 408 / cols, digs = [], arem = an, abBeadW = Math.min(44, astep - 16);
      for (i = 0; i < cols; i++) { digs.push(arem % 10); arem = Math.floor(arem / 10); }
      s += rc(16, 216, 428, 40, '#f6e7d2', '#6d4c41', 2.2, 8) + lnr(20, 220, 440, 220, '#d7b894', 1.2);
      for (c = 0; c < cols; c++) {
        var aidx = cols - 1 - c, acx = 26 + c * astep + astep / 2, ad = digs[aidx], acol = acols[aidx % acols.length], alb = alabs[aidx];
        if (c > 0) s += lnr(26 + c * astep, 220, 26 + c * astep, 252, '#d7b894', 1);
        s += lnr(acx, 30, acx, 216, ST, 3.4) + '<circle cx="' + f2(acx) + '" cy="29" r="4" fill="#6d4c41" stroke="' + ST + '" stroke-width="1.2"/>';
        for (k = 0; k < ad; k++) s += rc(acx - abBeadW / 2, 205 - k * 18 - 7.5, abBeadW, 15, acol, ST, 1.3, 7.5);
        s += '<text x="' + f2(acx) + '" y="241" font-family="Vazirmatn,Tahoma,sans-serif" font-size="' + (cols >= 6 ? '10.2' : '11.5') + '" font-weight="800" fill="#3e2723" text-anchor="middle">' + esc(alb) + '</text>';
        if (xs('blank', '0') === '1') {
          s += rc(acx - 17, 264, 34, 28, '#fff8e1', PT, 1.4, 6) + '<text x="' + f2(acx) + '" y="284" font-family="Vazirmatn,Tahoma,sans-serif" font-size="15" font-weight="800" fill="' + PT + '" text-anchor="middle">؟</text>';
        } else {
          s += rc(acx - 17, 264, 34, 28, '#f8fafc', '#cbd5e1', 1.2, 6) + '<text x="' + f2(acx) + '" y="284" font-family="Vazirmatn,Tahoma,sans-serif" font-size="16" font-weight="800" fill="' + ST + '" text-anchor="middle">' + esc(String(ad)) + '</text>';
        }
      }
      return wrap(s, 310, 460);
    }
    if (t === 'axjump') {
      var jopRaw = xs('op', '+').trim(), jop = (jopRaw === '-' || jopRaw === '−') ? '-' : (jopRaw === '×' || jopRaw === '*') ? '×' : (jopRaw === '÷' || jopRaw === '/') ? '÷' : '+';
      var jsp = clamp(Math.floor(num(X.step, 3)), 1, 20), jcnt = clamp(Math.floor(num(X.jumps, 4)), 1, 10);
      var jstIn = clamp(Math.floor(num(X.start, 0)), 0, 100), jspan = jsp * jcnt;
      var jfwd = (jop === '+' || jop === '×');
      var jfrom = jfwd ? (jop === '×' ? 0 : jstIn) : (jop === '÷' ? (jstIn >= jspan && jstIn > 0 ? jstIn : jspan) : Math.max(jstIn, jspan));
      var jend = jfwd ? (jfrom + jspan) : Math.max(0, jfrom - jspan);
      var jneed = Math.max(jfrom, jend);
      var jmax = clamp(Math.max(jneed, Math.floor(num(X.max, jneed))), 2, 200), jy = 186;
      s += lnr(20, jy, 342, jy, ST, 2.2);
      var jtk = jmax <= 15 ? 1 : jmax <= 30 ? 2 : jmax <= 60 ? 5 : 10;
      for (i = 0; i <= jmax; i++) {
        var jx = 28 + 304 * (i / jmax), jmaj = (i % jtk === 0 || i === jfrom || i === jend);
        s += lnr(jx, jy - (jmaj ? 7 : 3.5), jx, jy + (jmaj ? 7 : 3.5), ST, jmaj ? 1.8 : 0.9);
        if (i % jtk === 0) s += tx(jx, jy + 22, String(i), ST, 'middle', true, jmax > 50 ? 9.5 : 11);
      }
      s += '<circle cx="' + f2(28 + 304 * (jfrom / jmax)) + '" cy="' + f2(jy) + '" r="4.8" fill="' + ACC + '"/>';
      for (i = 0; i < jcnt; i++) {
        var ja = jfwd ? (jfrom + i * jsp) : (jfrom - i * jsp);
        var jb = jfwd ? (ja + jsp) : (ja - jsp);
        if (jb < 0 || jb > jmax) break;
        var jxa = 28 + 304 * (ja / jmax), jxb = 28 + 304 * (jb / jmax), jxm = (jxa + jxb) / 2;
        var jh = Math.min(86, Math.max(34, Math.abs(jxb - jxa) * 0.65));
        s += '<path d="M ' + f2(jxa) + ' ' + f2(jy - 4) + ' Q ' + f2(jxm) + ' ' + f2(jy - jh) + ' ' + f2(jxb) + ' ' + f2(jy - 4) + '" fill="none" stroke="' + ACC + '" stroke-width="2.4"/>';
        var jdir = jfwd ? 1 : -1;
        s += '<polygon points="' + f2(jxb) + ',' + f2(jy - 3) + ' ' + f2(jxb - jdir * 6) + ',' + f2(jy - 11) + ' ' + f2(jxb - jdir * 1.5) + ',' + f2(jy - 12) + '" fill="' + ACC + '"/>';
        s += '<circle cx="' + f2(jxb) + '" cy="' + f2(jy) + '" r="4" fill="' + PT + '"/>';
        var jarcLbl = jop === '-' ? ('−' + jsp) : jop === '÷' ? ('−' + jsp) : ('+' + jsp);
        s += tx(jxm, jy - jh * 0.58, jarcLbl, PT, 'middle', true, 11);
      }
      var jlbMode = xs('label', '1');
      if (jlbMode !== '0') {
        var jans = jop === '÷' ? String(jcnt) : String(jend);
        var rhs = jlbMode === '2' ? '؟' : jans;
        var jeq = jop === '+'
          ? (jcnt === 1 ? (jfrom + ' + ' + jsp + ' = ' + rhs) : (jfrom + ' + (' + jcnt + ' × ' + jsp + ') = ' + rhs))
          : jop === '-'
            ? (jcnt === 1 ? (jfrom + ' − ' + jsp + ' = ' + rhs) : (jfrom + ' − (' + jcnt + ' × ' + jsp + ') = ' + rhs))
            : jop === '×'
              ? (jcnt + ' × ' + jsp + ' = ' + rhs)
              : (jfrom + ' ÷ ' + jsp + ' = ' + rhs);
        s += tx(180, 252, jeq, jlbMode === '2' ? PT : ST, 'middle', true, 14);
      }
      return wrap(s);
    }
    if (t === 'axbalance') {
      var bl = num(X.left, 5), br = num(X.right, 3), bun = xs('unit', 'کیلوگرم'), bdiff = clamp(br - bl, -10, 10), bang = (bdiff / 10) * 14 * Math.PI / 180;
      var bcx = 180, bcy = 116, barm = 104, blx = bcx - barm * Math.cos(bang), bly = bcy - barm * Math.sin(bang), brx = bcx + barm * Math.cos(bang), bry = bcy + barm * Math.sin(bang);
      s += '<polygon points="' + f2(bcx) + ',' + f2(bcy) + ' ' + f2(bcx - 26) + ',236 ' + f2(bcx + 26) + ',236" fill="#90a4ae" stroke="' + ST + '" stroke-width="2"/>' + rc(bcx - 56, 236, 112, 14, ST, ST, 1, 5) + lnr(blx, bly, brx, bry, ST, 4) + '<circle cx="' + f2(bcx) + '" cy="' + f2(bcy) + '" r="7" fill="' + GLD + '" stroke="' + ST + '" stroke-width="2"/>';
      [[blx, bly, bl, ACC], [brx, bry, br, GRN]].forEach(function (p) {
        var px = p[0], py = p[1], pv = p[2], pcol = p[3], plb = String(pv);
        s += lnr(px, py, px - 28, py + 44, MU, 1.5) + lnr(px, py, px + 28, py + 44, MU, 1.5) + rc(px - 36, py + 44, 72, 10, GLD, ST, 1.6, 4) + rc(px - 22, py + 14, 44, 30, pcol, ST, 1.6, 5) + tx(px, py + 33, plb, '#fff', 'middle', true, 13) + tx(px, py + 68, plb + ' ' + bun, ST, 'middle', true, 11);
      });
      var balLb = xs('label', '1');
      if (balLb === '2') s += tx(180, 268, '؟', PT, 'middle', true, 20);
      else if (balLb !== '0') s += tx(180, 268, bl > br ? '>' : bl < br ? '<' : '=', PT, 'middle', true, 20);
      return wrap(s);
    }
    if (t === 'axthermo') {
      var tmin = num(X.min, -10), tmax = Math.max(tmin + 5, num(X.max, 50)), tval = clamp(num(X.val, 25), tmin, tmax);
      var tcx = 136, ttop = 34, tbot = 204, tvy = tbot - (tbot - ttop) * ((tval - tmin) / (tmax - tmin)), tcol = tval <= 0 ? '#1e88e5' : '#e53935';
      s += rc(tcx - 12, ttop - 8, 24, tbot - ttop + 16, '#f8fafc', ST, 2, 12) + rc(tcx - 7, tvy, 14, tbot - tvy + 12, tcol, 'none', 0, 6) + '<circle cx="' + f2(tcx) + '" cy="' + f2(tbot + 22) + '" r="22" fill="' + tcol + '" stroke="' + ST + '" stroke-width="2"/>';
      var tstep = (tmax - tmin <= 20) ? 2 : (tmax - tmin <= 60) ? 10 : 20;
      for (var ttk = Math.ceil(tmin / tstep) * tstep; ttk <= tmax + 0.01; ttk += tstep) {
        var tty = tbot - (tbot - ttop) * ((ttk - tmin) / (tmax - tmin));
        s += lnr(tcx + 12, tty, tcx + 22, tty, ST, 1.6) + tx(tcx + 42, tty + 4, Math.round(ttk) + '°', ST, 'middle', true, 11);
      }
      s += rc(224, 102, 94, 44, '#fff8e1', tcol, 1.8, 10) + tx(271, 131, Math.round(tval) + '°C', tcol, 'middle', true, 20);
      return wrap(s);
    }
    var SAW_PAL = {
      '1': ['●', '#e53935', '#ffebee', 'قرمز'],
      '2': ['▲', '#1e88e5', '#e3f2fd', 'آبی'],
      '3': ['■', '#43a047', '#e8f5e9', 'سبز'],
      '4': ['★', '#fbc02d', '#fffde7', 'زرد'],
      '5': ['◆', '#8e24aa', '#f3e5f5', 'بنفش'],
      '6': ['♥', '#fb8c00', '#fff3e0', 'نارنجی'],
      '7': ['⬢', '#00acc1', '#e0f7fa', 'فیروزه‌ای'],
      '8': ['✿', '#ec407a', '#fce4ec', 'صورتی'],
      '9': ['▼', '#6d4c41', '#efebe9', 'قهوه‌ای']
    };
    function normSaw(v0) {
      var c0 = faEn(String(v0 == null ? '' : v0).trim());
      if (c0 === '_' || c0 === '-' || c0 === 'خالی') return '_';
      if (!c0 || c0 === '?' || c0 === '؟') return '?';
      if (SAW_PAL[c0]) return c0;
      if (c0.indexOf('قرمز') >= 0 || c0.indexOf('سرخ') >= 0 || c0 === '●') return '1';
      if (c0.indexOf('آبی') >= 0 || c0 === '▲') return '2';
      if (c0.indexOf('سبز') >= 0 || c0 === '■') return '3';
      if (c0.indexOf('زرد') >= 0 || c0 === '★') return '4';
      if (c0.indexOf('بنفش') >= 0 || c0 === '◆') return '5';
      if (c0.indexOf('نارنجی') >= 0 || c0 === '♥') return '6';
      if (c0.indexOf('فیروزه') >= 0 || c0 === '⬢') return '7';
      if (c0.indexOf('صورتی') >= 0 || c0 === '✿') return '8';
      if (c0.indexOf('قهوه') >= 0 || c0 === '▼') return '9';
      return c0;
    }
    function defSudokuRows(n0) {
      var sz = clamp(Math.floor(n0 || 4), 3, 9);
      if (sz === 4) return '1,2,3,4;3,4,?,2;2,?,4,3;4,3,2,1'.split(';').map(function (r0) { return r0.split(','); });
      var out = [], rr, cc;
      for (rr = 0; rr < sz; rr++) {
        var rowArr = [];
        for (cc = 0; cc < sz; cc++) {
          var v2 = sz === 9 ? (((rr * 3 + Math.floor(rr / 3) + cc) % 9) + 1)
            : sz === 6 ? (((rr * 3 + Math.floor(rr / 2) + cc) % 6) + 1)
            : (((rr + cc) % sz) + 1);
          rowArr.push(((rr * 2 + cc + 1) % (sz + 1) === 0) ? '?' : String(v2));
        }
        out.push(rowArr);
      }
      return out;
    }
    if (t === 'axsaw') {
      var sraw = xs('vals', '1,2,3,4;3,4,?,2;2,?,4,3;4,3,2,1'), smode = xs('mode', 'shape').toLowerCase();
      var sparsed = sraw.split(/[;؛]/).map(function (r0) { return r0.split(/[,،]/).map(normSaw); }).filter(function (r0) { return r0.length > 0; });
      var sexp = Math.floor(num(X.size, 0));
      var sn2 = clamp((sexp >= 3 && sexp <= 9) ? sexp : clamp(sparsed.length, 3, 9), 3, 9);
      var srows = (sparsed.length === sn2 && sparsed.every(function (r0) { return r0.length >= sn2; })) ? sparsed : defSudokuRows(sn2);
      var sleg = smode === 'color' || smode === 'color_shape';
      var smaxG = sleg ? 196 : 216, scs = Math.min(52, smaxG / sn2), sgw2 = scs * sn2;
      var sx0 = (360 - sgw2) / 2, sy0 = (sleg ? 24 : 28) + (smaxG - sgw2) / 2;
      var sfsz = sn2 <= 4 ? 21 : sn2 <= 6 ? 16 : 12;
      for (r = 0; r < sn2; r++) for (c = 0; c < sn2; c++) {
        var scell = (srows[r] && srows[r][c]) ? srows[r][c] : '?', sx = sx0 + c * scs, sy = sy0 + r * scs;
        var sisQ = scell === '?' || scell === '؟', sisEmpty = scell === '_';
        var stok = SAW_PAL[scell];
        var sbg = sisQ ? '#fff8e1' : sisEmpty ? '#ffffff' : ((smode === 'color' || smode === 'color_shape') && stok ? stok[2] : '#ffffff');
        s += rc(sx, sy, scs, scs, sbg, '#cbd5e1', 1);
        if (sisEmpty) { /* خانهٔ کاملاً خالی بدون علامت سؤال */ }
        else if (sisQ) s += tx(sx + scs / 2, sy + scs * 0.66, '؟', PT, 'middle', true, sfsz);
        else if (smode === 'color' && stok) {
          var spad = Math.max(3.5, scs * 0.16);
          s += rc(sx + spad, sy + spad, scs - spad * 2, scs - spad * 2, stok[1], ST, 1.2, Math.max(3, scs * 0.16));
        } else if ((smode === 'shape' || smode === 'color_shape') && stok) {
          s += tx(sx + scs / 2, sy + scs * 0.68, stok[0], stok[1], 'middle', true, sfsz + 2);
        } else {
          s += tx(sx + scs / 2, sy + scs * 0.66, scell, ST, 'middle', true, sfsz);
        }
      }
      var sboxR = sn2 === 9 ? 3 : (sn2 === 6 || sn2 === 4) ? 2 : sn2;
      var sboxC = (sn2 === 9 || sn2 === 6) ? 3 : sn2 === 4 ? 2 : sn2;
      for (r = 1; r < sn2; r++) s += lnr(sx0, sy0 + r * scs, sx0 + sgw2, sy0 + r * scs, ST, (r % sboxR === 0) ? 2.5 : 0.9);
      for (c = 1; c < sn2; c++) s += lnr(sx0 + c * scs, sy0, sx0 + c * scs, sy0 + sgw2, ST, (c % sboxC === 0) ? 2.5 : 0.9);
      s += rc(sx0, sy0, sgw2, sgw2, 'none', ST, 2.8);
      if (sleg) {
        var sitW = Math.min(36, 320 / sn2), stLW = sitW * sn2, slx0 = (360 - stLW) / 2, sly = 246;
        for (i = 1; i <= sn2; i++) {
          var ltok = SAW_PAL[String(i)]; if (!ltok) continue;
          var lcx = slx0 + (i - 0.5) * sitW;
          s += '<circle cx="' + f2(lcx) + '" cy="' + f2(sly) + '" r="7.5" fill="' + ltok[1] + '" stroke="' + ST + '" stroke-width="1.2"/>';
          s += tx(lcx, sly + (sn2 <= 6 ? 19 : 18), sn2 <= 6 ? ltok[3] : String(i), MU, 'middle', true, 9);
        }
      }
      return wrap(s);
    }
    if (t === 'axpattern') {
      var pitems = xs('seq', '●,▲,●,▲,●,?').split(/[,،]/).map(function (z) { return z.trim(); }).filter(Boolean).slice(0, 12);
      if (!pitems.length) return wrap('');
      var pmode = xs('mode', 'shape').toLowerCase();
      var pn = pitems.length, pcw = Math.min(42, 300 / pn), pgap = pn <= 8 ? 6 : 4, ptw = pn * pcw + (pn - 1) * pgap, px0 = (360 - ptw) / 2, py0 = 112;
      var ppal = ['#e53935', '#1e88e5', '#43a047', '#fbc02d', '#8e24aa', '#fb8c00'];
      if (pmode === 'bead') s += lnr(px0 - 14, py0 + 27, px0 + ptw + 14, py0 + 27, ST, 3);
      for (i = 0; i < pn; i++) {
        var px2 = px0 + i * (pcw + pgap), pisQ = pitems[i] === '?' || pitems[i] === '؟', pnorm = normSaw(pitems[i]), ptok = SAW_PAL[pnorm];
        var pcol2 = pisQ ? PT : (ptok ? ptok[1] : ppal[i % ppal.length]);
        if (pmode === 'bead') {
          var pr2 = Math.min(20, pcw * 0.45), pcx = px2 + pcw / 2, pcy = py0 + 27;
          s += '<circle cx="' + f2(pcx) + '" cy="' + f2(pcy) + '" r="' + f2(pr2) + '" fill="' + (pisQ ? '#fff8e1' : pcol2) + '" stroke="' + ST + '" stroke-width="2"/>';
          if (pisQ) s += tx(pcx, pcy + 6, '؟', PT, 'middle', true, 16);
        } else if (pmode === 'color') {
          s += rc(px2, py0, pcw, 54, pisQ ? '#fff8e1' : pcol2, ST, 2, 10);
          if (pisQ) s += tx(px2 + pcw / 2, py0 + 34, '؟', PT, 'middle', true, 20);
        } else if (pmode === 'growing') {
          var gCnt = pisQ ? 0 : clamp(parseInt(faEn(pitems[i]), 10) || (i + 1), 1, 25);
          s += rc(px2, py0 - 16, pcw, 86, pisQ ? '#fff8e1' : '#f8fafc', pisQ ? PT : '#cbd5e1', 1.6, 8);
          if (pisQ) {
            s += tx(px2 + pcw / 2, py0 + 34, '؟', PT, 'middle', true, 20);
          } else {
            var gCols = Math.max(1, Math.ceil(Math.sqrt(gCnt))), gRows = Math.max(1, Math.ceil(gCnt / gCols));
            var gStep = Math.min((pcw - 10) / gCols, 52 / gRows, 13);
            var gDr = Math.max(2.3, gStep * 0.38);
            for (var gi = 0; gi < gCnt; gi++) {
              var gc2 = gi % gCols, gr2 = Math.floor(gi / gCols);
              var gdx = px2 + pcw / 2 + (gc2 - (gCols - 1) / 2) * gStep;
              var gdy = py0 + 16 + (gr2 - (gRows - 1) / 2) * gStep;
              s += '<circle cx="' + f2(gdx) + '" cy="' + f2(gdy) + '" r="' + f2(gDr) + '" fill="' + ACC + '"/>';
            }
            s += tx(px2 + pcw / 2, py0 + 62, String(gCnt), ST, 'middle', true, 11);
          }
        } else if (pmode === 'number') {
          s += rc(px2, py0, pcw, 54, pisQ ? '#fff8e1' : '#eff6ff', pisQ ? PT : '#2563eb', 2, 10);
          s += tx(px2 + pcw / 2, py0 + 34, pisQ ? '؟' : pitems[i], pisQ ? PT : '#1e40af', 'middle', true, pn > 8 ? 14 : 18);
          if (i < pn - 1) s += tx(px2 + pcw + pgap / 2, py0 + 31, '،', MU, 'middle', true, 12);
        } else if (pmode === 'mixed') {
          var mFill = pisQ ? '#fff8e1' : (ptok ? ptok[2] : '#f5f3ff');
          var mStroke = pisQ ? PT : (ptok ? ptok[1] : ACC);
          s += rc(px2, py0, pcw, 54, mFill, mStroke, 2, 10);
          var mlbl = pisQ ? '؟' : (ptok && SAW_PAL[pitems[i]] ? ptok[0] : pitems[i]);
          s += tx(px2 + pcw / 2, py0 + 34, mlbl, mStroke, 'middle', true, pn > 8 ? 14 : 19);
        } else {
          s += rc(px2, py0, pcw, 54, pisQ ? '#fff8e1' : ACCS, pcol2, 2, 9);
          var plbl = pisQ ? '؟' : (ptok && SAW_PAL[pitems[i]] ? ptok[0] : pitems[i]);
          s += tx(px2 + pcw / 2, py0 + 34, plbl, pcol2, 'middle', true, pn > 8 ? 15 : 20);
        }
      }
      return wrap(s);
    }
    if (t === 'axtenframe') {
      var tfr = clamp(Math.floor(num(X.frames, 2)), 1, 2), tcap = tfr * 10;
      var ta = clamp(Math.floor(num(X.a, 7)), 0, tcap), tb = clamp(Math.floor(num(X.b, 5)), 0, tcap - ta);
      var tcw = 44, tch = 44, tfw = tcw * 5, tfh = tch * 2, tx0 = (360 - tfw) / 2;
      for (var fi = 0; fi < tfr; fi++) {
        var tfy = tfr === 1 ? 88 : 32 + fi * (tfh + 18);
        s += rc(tx0, tfy, tfw, tfh, '#ffffff', ST, 2.2, 6);
        for (r = 0; r < 2; r++) for (c = 0; c < 5; c++) {
          var tcx2 = tx0 + c * tcw, tcy2 = tfy + r * tch, tidx = fi * 10 + r * 5 + c;
          s += rc(tcx2, tcy2, tcw, tch, 'none', ST, 1.2);
          if (tidx < ta + tb) s += '<circle cx="' + f2(tcx2 + tcw / 2) + '" cy="' + f2(tcy2 + tch / 2) + '" r="14" fill="' + (tidx < ta ? '#e53935' : '#1e88e5') + '" stroke="' + ST + '" stroke-width="1.4"/>';
        }
      }
      var tfLb = xs('label', '1');
      if (tfLb === '2') s += tx(180, 262, ta + ' + ' + tb + ' = ؟', PT, 'middle', true, 15);
      else if (tfLb !== '0') s += tx(180, 262, ta + ' + ' + tb + ' = ' + (ta + tb), ST, 'middle', true, 15);
      return wrap(s);
    }
    if (t === 'axsym') {
      var saxis = xs('axis', 'v').toLowerCase(), smir = Math.floor(num(X.mirror, 1)) !== 0;
      var scellsRaw = Object.prototype.hasOwnProperty.call(X, 'cells') ? String(X.cells == null ? '' : X.cells) : '1,1;2,1;2,2;3,2';
      var scells = scellsRaw.split(/[;؛]/).map(function (tk) {
        var pp = tk.split(/[,،]/).map(function (z) { return parseInt(faEn(z.trim()), 10); });
        return (pp.length >= 2 && isFinite(pp[0]) && isFinite(pp[1])) ? [clamp(pp[0], 1, 8), clamp(pp[1], 1, 8)] : null;
      }).filter(Boolean);
      var sansCells = String(xs('ansCells', '')).split(/[;؛]/).map(function (tk) {
        var pp = tk.split(/[,،]/).map(function (z) { return parseInt(faEn(z.trim()), 10); });
        return (pp.length >= 2 && isFinite(pp[0]) && isFinite(pp[1])) ? [clamp(pp[0], 1, 8), clamp(pp[1], 1, 8)] : null;
      }).filter(Boolean);
      var sc = 26, sn = 8, sgw = sc * sn, sx00 = (360 - sgw) / 2, sy00 = 32;
      s += rc(sx00, sy00, sgw, sgw, '#f8fafc', ST, 1.8);
      for (r = 0; r < sn; r++) for (c = 0; c < sn; c++) s += rc(sx00 + c * sc, sy00 + r * sc, sc, sc, 'none', '#cbd5e1', 1);
      scells.forEach(function (pr) {
        var r1 = pr[0], d1 = pr[1];
        if (saxis === 'h') {
          var cPos = clamp(r1 - 1, 0, 7);
          var rOrig = d1 <= 4 ? clamp(4 - d1, 0, 3) : clamp(d1 - 1, 4, 7);
          var rRef = 7 - rOrig;
          s += rc(sx00 + cPos * sc + 2, sy00 + rOrig * sc + 2, sc - 4, sc - 4, ACC, ST, 1.2, 3);
          if (smir) s += rc(sx00 + cPos * sc + 2, sy00 + rRef * sc + 2, sc - 4, sc - 4, GRN, ST, 1.2, 3);
        } else {
          var rPos = clamp(r1 - 1, 0, 7);
          var cOrig = d1 <= 4 ? clamp(4 - d1, 0, 3) : clamp(d1 - 1, 4, 7);
          var cRef = 7 - cOrig;
          s += rc(sx00 + cOrig * sc + 2, sy00 + rPos * sc + 2, sc - 4, sc - 4, ACC, ST, 1.2, 3);
          if (smir) s += rc(sx00 + cRef * sc + 2, sy00 + rPos * sc + 2, sc - 4, sc - 4, GRN, ST, 1.2, 3);
        }
      });
      sansCells.forEach(function (apr) {
        var ar = apr[0] - 1, ac = apr[1] - 1;
        s += rc(sx00 + ac * sc + 2, sy00 + ar * sc + 2, sc - 4, sc - 4, GRN, ST, 1.4, 3);
      });
      if (saxis === 'h') s += ln(sx00 - 12, sy00 + 4 * sc, sx00 + sgw + 12, sy00 + 4 * sc, PT, 3.2, true);
      else s += ln(sx00 + 4 * sc, sy00 - 10, sx00 + 4 * sc, sy00 + sgw + 10, PT, 3.2, true);
      return wrap(s);
    }
    if (t === 'axspin') {
      var sparts = xs('parts', 'قرمز,آبی,سبز,زرد,قرمز,آبی').split(/[,،]/).map(function (z) { return z.trim(); }).filter(Boolean).slice(0, 12);
      if (sparts.length < 2) sparts = ['قرمز', 'آبی', 'سبز', 'زرد'];
      var spn = sparts.length, sarrow = ((Math.floor(num(X.arrow, 1)) - 1) % spn + spn) % spn;
      var spcx = 180, spcy = 142, spr = 98, sppal = ['#ef5350', '#42a5f5', '#66bb6a', '#ffca28', '#ab47bc', '#26a69a', '#ffa726', '#ec407a'];
      var spcol = function (lb, ix) {
        if (lb.indexOf('قرمز') >= 0 || lb.indexOf('سرخ') >= 0) return '#ef5350';
        if (lb.indexOf('آبی') >= 0) return '#42a5f5';
        if (lb.indexOf('سبز') >= 0) return '#66bb6a';
        if (lb.indexOf('زرد') >= 0) return '#ffca28';
        if (lb.indexOf('بنفش') >= 0) return '#ab47bc';
        if (lb.indexOf('نارنجی') >= 0) return '#ffa726';
        return sppal[ix % sppal.length];
      };
      for (i = 0; i < spn; i++) {
        var spa0 = (i * 360 / spn - 90) * Math.PI / 180, spa1 = ((i + 1) * 360 / spn - 90) * Math.PI / 180;
        var spx1 = spcx + spr * Math.cos(spa0), spy1 = spcy + spr * Math.sin(spa0), spx2 = spcx + spr * Math.cos(spa1), spy2 = spcy + spr * Math.sin(spa1), splg = 360 / spn > 180 ? 1 : 0;
        s += '<path d="M ' + f2(spcx) + ' ' + f2(spcy) + ' L ' + f2(spx1) + ' ' + f2(spy1) + ' A ' + f2(spr) + ' ' + f2(spr) + ' 0 ' + splg + ' 1 ' + f2(spx2) + ' ' + f2(spy2) + ' Z" fill="' + spcol(sparts[i], i) + '" stroke="' + ST + '" stroke-width="1.8"/>';
        var spam = (spa0 + spa1) / 2;
        s += tx(spcx + spr * 0.62 * Math.cos(spam), spcy + spr * 0.62 * Math.sin(spam) + 4, sparts[i], '#fff', 'middle', true, 11);
      }
      var spang = ((sarrow + 0.5) * 360 / spn - 90) * Math.PI / 180, spax = spcx + spr * 0.48 * Math.cos(spang), spay = spcy + spr * 0.48 * Math.sin(spang);
      s += lnr(spcx, spcy, spax, spay, ST, 4.2) + '<circle cx="' + f2(spax) + '" cy="' + f2(spay) + '" r="5.5" fill="' + GLD + '" stroke="' + ST + '" stroke-width="1.8"/><circle cx="' + f2(spcx) + '" cy="' + f2(spcy) + '" r="9" fill="#fff" stroke="' + ST + '" stroke-width="2.4"/>';
      return wrap(s);
    }
    if (t === 'axbeaker') {
      var bmx = Math.max(10, num(X.max, 500)), bcnt = clamp(Math.floor(num(X.count, 1)), 1, 4), bunit = xs('unit', 'mL');
      var bvals = [
        clamp(num(X.val, 350), 0, bmx),
        clamp(num(X.val2, Math.round(bmx * 0.4)), 0, bmx),
        clamp(num(X.val3, Math.round(bmx * 0.8)), 0, bmx),
        clamp(num(X.val4, Math.round(bmx * 0.2)), 0, bmx)
      ];
      var bcols = [
        ['rgba(38,166,154,.44)', '#00897b', '#e0f2f1', '#00695c'],
        ['rgba(66,165,245,.42)', '#1e88e5', '#e3f2fd', '#1565c0'],
        ['rgba(171,71,188,.38)', '#8e24aa', '#f3e5f5', '#6a1b9a'],
        ['rgba(255,167,38,.44)', '#fb8c00', '#fff3e0', '#ef6c00']
      ];
      if (bcnt === 1) {
        var bval = bvals[0], bx0 = 96, bw2 = 112, btop = 36, bbot = 216, bh2 = bbot - btop, bvh = bh2 * (bval / bmx), bvy = bbot - bvh;
        s += rc(bx0, btop, bw2, bh2, '#f8fafc', 'none', 0);
        if (bval > 0) s += rc(bx0 + 3, bvy, bw2 - 6, bvh, bcols[0][0], bcols[0][1], 1.5);
        s += '<path d="M ' + f2(bx0 - 8) + ' ' + f2(btop) + ' L ' + f2(bx0) + ' ' + f2(btop) + ' L ' + f2(bx0) + ' ' + f2(bbot) + ' L ' + f2(bx0 + bw2) + ' ' + f2(bbot) + ' L ' + f2(bx0 + bw2) + ' ' + f2(btop) + ' L ' + f2(bx0 + bw2 + 8) + ' ' + f2(btop) + '" fill="none" stroke="' + ST + '" stroke-width="3"/>';
        s += tx(bx0 + bw2 + 34, btop - 8, '(' + bunit + ')', MU, 'middle', true, 11);
        for (i = 0; i <= 10; i++) {
          var bty = bbot - bh2 * (i / 10), bmaj = (i % 2 === 0);
          s += lnr(bx0 + bw2 - (bmaj ? 18 : 10), bty, bx0 + bw2, bty, ST, bmaj ? 1.8 : 1.1);
          if (bmaj && i > 0) s += tx(bx0 + bw2 + 34, bty + 4, String(Math.round(bmx * i / 10)), ST, 'middle', true, 12);
        }
        s += rc(bx0 - 4, 230, bw2 + 8, 30, bcols[0][2], bcols[0][1], 1.5, 8) + tx(bx0 + bw2 / 2, 250, Math.round(bval) + ' ' + bunit, bcols[0][3], 'middle', true, 15);
      } else {
        var bslot = 336 / bcnt, mbw = Math.min(58, bslot - (bcnt >= 4 ? 26 : 32)), mbtop = 38, mbbot = 208, mbh = mbbot - mbtop;
        s += tx(180, 22, 'ظرفیت هر استوانه: ' + Math.round(bmx) + ' ' + bunit, MU, 'middle', true, 11);
        for (var bi = 0; bi < bcnt; bi++) {
          var mval = bvals[bi], mc = bcols[bi % bcols.length];
          var msx = 12 + bi * bslot + (bslot - mbw) / 2 - 6, mvh = mbh * (mval / bmx), mvy = mbbot - mvh;
          s += rc(msx, mbtop, mbw, mbh, '#f8fafc', 'none', 0);
          if (mval > 0) s += rc(msx + 2, mvy, mbw - 4, mvh, mc[0], mc[1], 1.3);
          s += '<path d="M ' + f2(msx - 5) + ' ' + f2(mbtop) + ' L ' + f2(msx) + ' ' + f2(mbtop) + ' L ' + f2(msx) + ' ' + f2(mbbot) + ' L ' + f2(msx + mbw) + ' ' + f2(mbbot) + ' L ' + f2(msx + mbw) + ' ' + f2(mbtop) + ' L ' + f2(msx + mbw + 5) + ' ' + f2(mbtop) + '" fill="none" stroke="' + ST + '" stroke-width="2.4"/>';
          for (i = 0; i <= 10; i++) {
            var mty = mbbot - mbh * (i / 10), mmaj = (i % 5 === 0);
            s += lnr(msx + mbw - (mmaj ? 12 : 6), mty, msx + mbw, mty, ST, mmaj ? 1.5 : 0.9);
            if (mmaj && i > 0) s += tx(msx + mbw + 15, mty + 3.5, String(Math.round(bmx * i / 10)), MU, 'middle', true, 8.5);
          }
          var badgeW = Math.min(bslot - 6, mbw + 18);
          s += rc(msx + mbw / 2 - badgeW / 2, 222, badgeW, 26, mc[2], mc[1], 1.3, 6);
          s += tx(msx + mbw / 2, 239, Math.round(mval) + (bcnt <= 3 ? (' ' + bunit) : ''), mc[3], 'middle', true, bcnt >= 4 ? 11 : 12);
          s += tx(msx + mbw / 2, 264, '(' + (bi + 1) + ')', ST, 'middle', true, 11);
        }
      }
      return wrap(s);
    }
    if (t === 'axgeoboard') {
      var gg = clamp(Math.floor(num(X.grid, 5)), 3, 7);
      var gpts = xs('pts', '1,1;4,1;4,3;1,3').split(/[;؛]/).map(function (tk) {
        var pp = tk.split(/[,،]/).map(function (z) { return parseFloat(faEn(z.trim())); });
        return (pp.length >= 2 && isFinite(pp[0]) && isFinite(pp[1])) ? [clamp(pp[0], 1, gg), clamp(pp[1], 1, gg)] : null;
      }).filter(Boolean);
      var gbw = 200, gx0 = (360 - gbw) / 2, gy0 = 36, gstep = gbw / Math.max(1, gg - 1);
      s += rc(gx0 - 18, gy0 - 18, gbw + 36, gbw + 36, '#fff8e1', ST, 2, 12);
      if (gpts.length >= 2) {
        var gpoly = gpts.map(function (pt) { return f2(gx0 + (pt[0] - 1) * gstep) + ',' + f2(gy0 + (gg - pt[1]) * gstep); }).join(' ');
        s += '<polygon points="' + gpoly + '" fill="' + ACCS + '" stroke="' + PT + '" stroke-width="3"/>';
      }
      for (r = 0; r < gg; r++) for (c = 0; c < gg; c++) s += '<circle cx="' + f2(gx0 + c * gstep) + '" cy="' + f2(gy0 + r * gstep) + '" r="4.2" fill="' + ST + '"/>';
      return wrap(s);
    }
    if (t === 'axdice') {
      var dl = xs('dice', '4,6').split(/[,،]/).map(function (z) { return parseInt(faEn(z.trim()), 10); }).filter(function (v2) { return isFinite(v2) && v2 >= 1 && v2 <= 6; }).slice(0, 4);
      if (!dl.length) dl = [4, 6];
      var dsum = Math.floor(num(X.sum, 1)), dsz = 66, dgap = 22, dtw = dl.length * dsz + (dl.length - 1) * dgap, dx0 = (360 - dtw) / 2, dy0 = 92;
      var dpips = {
        1: [[0.5, 0.5]], 2: [[0.26, 0.26], [0.74, 0.74]], 3: [[0.24, 0.24], [0.5, 0.5], [0.76, 0.76]],
        4: [[0.26, 0.26], [0.74, 0.26], [0.26, 0.74], [0.74, 0.74]],
        5: [[0.24, 0.24], [0.76, 0.24], [0.5, 0.5], [0.24, 0.76], [0.76, 0.76]],
        6: [[0.26, 0.22], [0.74, 0.22], [0.26, 0.5], [0.74, 0.5], [0.26, 0.78], [0.74, 0.78]]
      };
      var dtot = 0;
      dl.forEach(function (dv, idx) {
        dtot += dv;
        var ddx = dx0 + idx * (dsz + dgap);
        s += rc(ddx, dy0, dsz, dsz, '#ffffff', ST, 2.2, 12);
        (dpips[dv] || []).forEach(function (pp) { s += '<circle cx="' + f2(ddx + pp[0] * dsz) + '" cy="' + f2(dy0 + pp[1] * dsz) + '" r="5.6" fill="' + PT + '"/>'; });
        if (idx < dl.length - 1 && dsum !== 0) s += tx(ddx + dsz + dgap / 2, dy0 + dsz * 0.58, '+', ST, 'middle', true, 18);
      });
      if (dsum === 1) s += tx(180, 214, dl.join(' + ') + ' = ' + dtot, ST, 'middle', true, 16);
      else if (dsum === 2) s += tx(180, 214, dl.join(' + ') + ' = ؟', PT, 'middle', true, 16);
      return wrap(s);
    }
    if (t === 'axmularea') {
      var ma = clamp(Math.floor(num(X.a, 24)), 10, 99), mb = clamp(Math.floor(num(X.b, 13)), 10, 99), mblank = Math.floor(num(X.blank, 0)) !== 0;
      var ma1 = Math.floor(ma / 10) * 10, ma2 = ma % 10, mb1 = Math.floor(mb / 10) * 10, mb2 = mb % 10;
      var mx0 = 76, my0 = 56, mw1 = 136, mw2 = 76, mh1 = 96, mh2 = 58;
      s += rc(mx0, my0, mw1, mh1, 'rgba(108,99,245,.16)', ST, 1.8) + rc(mx0 + mw1, my0, mw2, mh1, 'rgba(39,196,168,.18)', ST, 1.8) +
        rc(mx0, my0 + mh1, mw1, mh2, 'rgba(240,162,2,.18)', ST, 1.8) + rc(mx0 + mw1, my0 + mh1, mw2, mh2, 'rgba(214,51,108,.15)', ST, 1.8);
      s += tx(mx0 + mw1 / 2, my0 - 10, String(ma1), ST, 'middle', true, 14) + tx(mx0 + mw1 + mw2 / 2, my0 - 10, String(ma2), ST, 'middle', true, 14) +
        tx(mx0 - 26, my0 + mh1 / 2 + 5, String(mb1), ST, 'middle', true, 14) + tx(mx0 - 26, my0 + mh1 + mh2 / 2 + 5, String(mb2), ST, 'middle', true, 14);
      s += tx(mx0 + mw1 / 2, my0 + mh1 / 2 + 6, mblank ? '؟' : String(ma1 * mb1), ST, 'middle', true, 15) +
        tx(mx0 + mw1 + mw2 / 2, my0 + mh1 / 2 + 6, mblank ? '؟' : String(ma2 * mb1), ST, 'middle', true, 15) +
        tx(mx0 + mw1 / 2, my0 + mh1 + mh2 / 2 + 6, mblank ? '؟' : String(ma1 * mb2), ST, 'middle', true, 15) +
        tx(mx0 + mw1 + mw2 / 2, my0 + mh1 + mh2 / 2 + 6, mblank ? '؟' : String(ma2 * mb2), ST, 'middle', true, 15);
      s += tx(180, 248, mblank ? (ma + ' × ' + mb + ' = ؟') : (ma + ' × ' + mb + ' = ' + (ma * mb)), ST, 'middle', true, 15);
      return wrap(s);
    }
    if (t === 'axvenn') {
      var vleft = xs('left', 'پرنده: کبوتر، گنجشک'), vright = xs('right', 'آبزی: ماهی، میگو'), vmid = xs('mid', 'اردک');
      s += '<circle cx="128" cy="150" r="84" fill="rgba(66,165,245,.20)" stroke="#1e88e5" stroke-width="2.2"/><circle cx="232" cy="150" r="84" fill="rgba(102,187,106,.20)" stroke="#43a047" stroke-width="2.2"/>';
      var vSide = function (raw, titleX, itemX, col, bg) {
        var str0 = String(raw || ''), cIdx = str0.indexOf(':');
        var titlePart = cIdx >= 0 ? str0.slice(0, cIdx).trim() : '';
        var bodyPart = cIdx >= 0 ? str0.slice(cIdx + 1) : str0;
        if (titlePart) s += rc(titleX - 44, 32, 88, 24, bg, col, 1.5, 12) + tx(titleX, 48, titlePart, col, 'middle', true, 12);
        var items = bodyPart.split(/[،,]/).map(function (z) { return z.trim(); }).filter(Boolean).slice(0, 4);
        var sy = 154 - (items.length - 1) * 11;
        items.forEach(function (ln2, ix2) { s += tx(itemX, sy + ix2 * 22, ln2, ST, 'middle', true, 12); });
      };
      vSide(vleft, 114, 82, '#1565c0', '#e3f2fd');
      vSide(vright, 246, 278, '#2e7d32', '#e8f5e9');
      var mItems = String(vmid || '').split(/[:،,]/).map(function (z) { return z.trim(); }).filter(Boolean).slice(0, 4);
      var mSy = 154 - (mItems.length - 1) * 11;
      mItems.forEach(function (ln2, ix2) { s += tx(180, mSy + ix2 * 22, ln2, PT, 'middle', true, 12); });
      return wrap(s);
    }
    function isPrimeNum(v0) {
      if (v0 < 2) return false;
      for (var d0 = 2; d0 * d0 <= v0; d0++) if (v0 % d0 === 0) return false;
      return true;
    }
    if (t === 'axnumgrid') {
      var gstart = clamp(Math.floor(num(X.start, 1)), 0, 900);
      var gcols = clamp(parseInt(faEn(xs('cols', '10')), 10) || 10, 4, 10);
      var grows = clamp(parseInt(faEn(xs('rows', '10')), 10) || 10, 3, 10);
      var grule = faEn(xs('rule', '5')).trim().toLowerCase(), gstepV = parseInt(grule, 10) || 0;
      var gblanks = {};
      xs('blanks', '15,35,60,85').split(/[,،;\s]+/).forEach(function (z) { var v2 = parseInt(faEn(z.trim()), 10); if (isFinite(v2)) gblanks[v2] = 1; });
      var gcs = Math.min(280 / gcols, 218 / grows), ggw = gcs * gcols, ggh = gcs * grows;
      var gx00 = (360 - ggw) / 2, gy00 = 26 + (222 - ggh) / 2, gfs = Math.max(gcols, grows) >= 9 ? 10 : 13;
      for (r = 0; r < grows; r++) for (c = 0; c < gcols; c++) {
        var gnum = gstart + r * gcols + c, gx2 = gx00 + c * gcs, gy2 = gy00 + r * gcs, gisB = !!gblanks[gnum];
        var ghi = grule === 'prime' ? isPrimeNum(gnum) : grule === 'even' ? (gnum % 2 === 0) : grule === 'odd' ? (gnum % 2 !== 0) : (gstepV > 0 && gnum > 0 && gnum % gstepV === 0);
        s += rc(gx2, gy2, gcs, gcs, gisB ? '#fff8e1' : (ghi ? 'rgba(39,196,168,.26)' : '#ffffff'), '#cbd5e1', 0.9);
        if (gisB) s += tx(gx2 + gcs / 2, gy2 + gcs * 0.68, '؟', PT, 'middle', true, gfs + 1);
        else s += tx(gx2 + gcs / 2, gy2 + gcs * 0.68, String(gnum), ghi ? '#00796b' : ST, 'middle', ghi, gfs);
      }
      s += rc(gx00, gy00, ggw, ggh, 'none', ST, 2.2);
      return wrap(s);
    }
    if (t === 'axfacttree') {
      var fn = clamp(Math.floor(num(X.n, 36)), 4, 999), fblank = clamp(Math.floor(num(X.blank, 0)), 0, 2);
      var fSplit = function (v0) {
        if (isPrimeNum(v0) || v0 < 4) return false;
        var best = 2, d0;
        for (d0 = 2; d0 * d0 <= v0; d0++) if (v0 % d0 === 0) best = d0;
        return [best, Math.floor(v0 / best)];
      };
      var fDraw = function (v0, cx0, cy0, spr0, dep0) {
        var sp = dep0 < 3 ? fSplit(v0) : null;
        if (sp) {
          var lx = cx0 - spr0, rx = cx0 + spr0, ny = cy0 + 62;
          s += lnr(cx0, cy0 + 16, lx, ny - 16, ST, 2) + lnr(cx0, cy0 + 16, rx, ny - 16, ST, 2);
          fDraw(sp[0], lx, ny, spr0 * 0.52, dep0 + 1);
          fDraw(sp[1], rx, ny, spr0 * 0.52, dep0 + 1);
        }
        var leaf = !sp, hide = (fblank === 1 && leaf) || (fblank === 2 && dep0 > 0 && !leaf);
        var fl = hide ? '#fff8e1' : dep0 === 0 ? 'rgba(108,99,245,.18)' : leaf ? 'rgba(39,196,168,.24)' : '#ffffff';
        var stk = hide ? PT : leaf ? '#00897b' : ST;
        if (leaf) s += '<circle cx="' + f2(cx0) + '" cy="' + f2(cy0) + '" r="17" fill="' + fl + '" stroke="' + stk + '" stroke-width="2"/>';
        else s += rc(cx0 - 21, cy0 - 15, 42, 30, fl, stk, 2, 8);
        s += tx(cx0, cy0 + 5, hide ? '؟' : String(v0), hide ? PT : ST, 'middle', true, 13);
      };
      fDraw(fn, 180, 44, 78, 0);
      return wrap(s);
    }
    if (t === 'axfnmachine') {
      var finps = (xs('in', '') || xs('ins', '2,4,6,8')).split(/[,،;]/).map(function (z) { return parseInt(faEn(z.trim()), 10); }).filter(isFinite).slice(0, 8);
      if (!finps.length) finps = [2, 4, 6, 8];
      var fop1 = xs('op1', '+ 3').trim(), fop2 = xs('op2', '').trim(), fmblank = clamp(Math.floor(num(X.blank, 1)), 0, 2);
      var fApply = function (v0, opR) {
        var s0 = faEn(String(opR || '')).trim(); if (!s0) return v0;
        var m0 = /-?\d+/.exec(s0), n0 = m0 ? parseInt(m0[0], 10) : 0;
        if (s0.indexOf('×') === 0 || s0.indexOf('*') === 0) return v0 * n0;
        if (s0.indexOf('÷') === 0 || s0.indexOf('/') === 0) return n0 !== 0 ? Math.floor(v0 / n0) : v0;
        if (s0.indexOf('-') === 0 || s0.indexOf('−') === 0) return v0 - Math.abs(n0);
        return v0 + n0;
      };
      s += tx(40, 64, 'ورودی', MU, 'middle', true, 11);
      if (fop2) {
        s += lnr(62, 60, 92, 60, ST, 2.2) + rc(94, 38, 74, 44, 'rgba(108,99,245,.15)', ACC, 2.2, 10) +
          tx(131, 65, fmblank === 2 ? '؟' : fop1, fmblank === 2 ? PT : ACC, 'middle', true, 15) +
          lnr(170, 60, 192, 60, ST, 2.2) + rc(194, 38, 74, 44, 'rgba(39,196,168,.18)', '#00897b', 2.2, 10) +
          tx(231, 65, fmblank === 2 ? '؟' : fop2, fmblank === 2 ? PT : '#00897b', 'middle', true, 15) + lnr(270, 60, 298, 60, ST, 2.2);
      } else {
        s += rc(112, 36, 136, 48, 'rgba(108,99,245,.15)', ACC, 2.2, 12) +
          tx(180, 66, fmblank === 2 ? '؟' : fop1, fmblank === 2 ? PT : ACC, 'middle', true, 17) +
          lnr(62, 60, 110, 60, ST, 2.2) + lnr(250, 60, 298, 60, ST, 2.2);
      }
      s += tx(322, 64, 'خروجی', MU, 'middle', true, 11);
      var flw = 52, fcw = Math.min(50, 286 / finps.length), ftw = flw + fcw * finps.length, fx0 = (360 - ftw) / 2, fy0 = 116, frh = 44;
      var fnumSz = finps.length >= 7 ? 11.5 : 14;
      s += rc(fx0, fy0, ftw, frh * 2, '#ffffff', ST, 2, 8) + lnr(fx0, fy0 + frh, fx0 + ftw, fy0 + frh, ST, 1.6);
      s += tx(fx0 + flw / 2, fy0 + 27, 'ورودی', ACC, 'middle', true, 11.5) + tx(fx0 + flw / 2, fy0 + frh + 27, 'خروجی', '#00897b', 'middle', true, 11.5);
      finps.forEach(function (inp, idx) {
        var fcx = fx0 + flw + idx * fcw, fout = fApply(fApply(inp, fop1), fop2), fhide = fmblank === 1 && idx >= Math.floor(finps.length / 2);
        s += lnr(fcx, fy0, fcx, fy0 + frh * 2, ST, 1.2) + tx(fcx + fcw / 2, fy0 + 28, String(inp), ST, 'middle', true, fnumSz);
        if (fhide) s += rc(fcx + 2.5, fy0 + frh + 3, fcw - 5, frh - 6, '#fff8e1', 'none', 0, 5) + tx(fcx + fcw / 2, fy0 + frh + 28, '؟', PT, 'middle', true, fnumSz + 1);
        else s += tx(fcx + fcw / 2, fy0 + frh + 28, String(fout), ST, 'middle', true, fnumSz);
      });
      return wrap(s);
    }
    if (t === 'axfracwall') {
      var wdens = xs('dens', '1,2,3,4,6,8').split(/[,،;]/).map(function (z) { return clamp(parseInt(faEn(z.trim()), 10) || 1, 1, 12); }).slice(0, 7);
      if (!wdens.length) wdens = [1, 2, 3, 4, 6, 8];
      var wfills = xs('fills', '1,1,2,2,3,4').split(/[,،;]/).map(function (z) { return parseInt(faEn(z.trim()), 10); });
      var wmode = xs('mode', 'label').toLowerCase(), wx0 = 30, wtw = 300, wrh = Math.min(34, 210 / wdens.length), wy0 = 30 + (214 - wrh * wdens.length) / 2;
      var wpal = ['#ef5350', '#42a5f5', '#66bb6a', '#ffa726', '#ab47bc', '#26a69a', '#ec407a'];
      wdens.forEach(function (den, rIdx) {
        var fcnt = clamp(isFinite(wfills[rIdx]) ? wfills[rIdx] : 1, 0, den), pw = wtw / den, ry = wy0 + rIdx * wrh, col = wpal[rIdx % wpal.length];
        for (c = 0; c < den; c++) {
          var px2 = wx0 + c * pw, act = c < fcnt;
          s += rc(px2, ry, pw, wrh, act ? col : '#ffffff', ST, 1.3);
          if (wmode !== 'color') {
            var lbl = (wmode === 'blank' && act && rIdx > 0) ? '؟' : (den === 1 ? '۱' : ('1/' + den));
            s += tx(px2 + pw / 2, ry + wrh * 0.66, lbl, act ? '#ffffff' : ST, 'middle', true, den >= 8 ? 9 : 11);
          }
        }
      });
      s += rc(wx0, wy0, wtw, wrh * wdens.length, 'none', ST, 2.2);
      return wrap(s);
    }
    if (t === 'axtangram') {
      var tshape = xs('shape', 'square').toLowerCase(), tmode = xs('mode', 'color').toLowerCase();
      var tcols = ['#ef5350', '#42a5f5', '#66bb6a', '#fdd835', '#ab47bc', '#fb8c00', '#26a69a'];
      var tmap = {
        house: [['110,120 180,50 250,120', 180, 96], ['110,120 180,120 110,190', 132, 144], ['180,120 250,120 250,190', 226, 144], ['110,190 180,120 250,190', 180, 168], ['110,190 155,190 155,235 110,235', 132, 216], ['155,190 250,190 225,235 155,235', 198, 216], ['205,65 230,65 230,100 205,75', 218, 78]],
        boat: [['175,45 175,155 105,155', 152, 122], ['183,65 245,155 183,155', 204, 128], ['90,165 270,165 235,210 125,210', 180, 192], ['125,210 235,210 210,235 150,235', 180, 226], ['175,45 198,45 175,68', 183, 54], ['105,155 135,130 135,155', 125, 148], ['245,155 268,140 258,165', 255, 154]],
        cat: [['145,55 165,85 145,85', 152, 76], ['195,55 195,85 175,85', 188, 76], ['145,85 195,85 195,125 145,125', 170, 108], ['170,125 225,175 145,175', 180, 158], ['145,175 225,175 185,225', 185, 196], ['185,225 235,195 235,235', 218, 218], ['235,215 278,185 268,225 225,235', 252, 215]],
        square: [['90,46 270,46 180,136', 180, 82], ['90,46 180,136 90,226', 124, 136], ['270,46 270,136 225,91', 252, 91], ['180,136 225,91 270,136 225,181', 225, 140], ['135,181 180,136 225,181', 180, 166], ['90,226 135,181 225,181 180,226', 158, 206], ['180,226 270,136 270,226', 238, 198]]
      };
      (tmap[tshape] || tmap.square).forEach(function (pl, idx) {
        var fl = tmode === 'outline' ? '#ffffff' : tcols[idx % tcols.length];
        s += '<polygon points="' + pl[0] + '" fill="' + fl + '" stroke="' + ST + '" stroke-width="2" stroke-linejoin="round"/>';
        if (tmode === 'num' || tmode === 'blank') {
          var lb = (tmode === 'blank' && idx % 2 === 1) ? '؟' : String(idx + 1);
          s += tx(pl[1], pl[2], lb, tmode === 'outline' ? ST : '#ffffff', 'middle', true, 13);
        }
      });
      return wrap(s);
    }
    if (t === 'axangle') {
      var adeg = clamp(num(X.deg, 65), 0, 180), apro = Math.floor(num(X.pro, 1)) !== 0, albl = xs('label', '1');
      var acx = 180, acy = 212, ar = 132;
      if (apro) {
        s += '<path d="M ' + f2(acx - ar) + ' ' + f2(acy) + ' A ' + f2(ar) + ' ' + f2(ar) + ' 0 0 1 ' + f2(acx + ar) + ' ' + f2(acy) + ' Z" fill="rgba(227,242,253,.55)" stroke="#1e88e5" stroke-width="1.8"/>';
        s += '<path d="M ' + f2(acx - ar * 0.55) + ' ' + f2(acy) + ' A ' + f2(ar * 0.55) + ' ' + f2(ar * 0.55) + ' 0 0 1 ' + f2(acx + ar * 0.55) + ' ' + f2(acy) + ' Z" fill="#ffffff" stroke="#90caf9" stroke-width="1.2"/>';
        for (d = 0; d <= 180; d += 10) {
          var arad0 = d * Math.PI / 180, maj = (d % 30 === 0);
          s += lnr(acx + (ar - (maj ? 12 : 6)) * Math.cos(arad0), acy - (ar - (maj ? 12 : 6)) * Math.sin(arad0), acx + ar * Math.cos(arad0), acy - ar * Math.sin(arad0), '#1565c0', maj ? 1.6 : 0.9);
          if (maj) s += tx(acx + (ar - 24) * Math.cos(arad0), acy - (ar - 24) * Math.sin(arad0) + 4, String(d), '#1565c0', 'middle', true, 10);
        }
      }
      var arad = adeg * Math.PI / 180, arx = acx + (ar + 10) * Math.cos(arad), ary = acy - (ar + 10) * Math.sin(arad), aar = 38;
      if (Math.abs(adeg - 90) < 0.5) s += rc(acx, acy - 22, 22, 22, 'rgba(214,51,108,.16)', PT, 1.8);
      else if (adeg > 0.5) s += '<path d="M ' + f2(acx + aar) + ' ' + f2(acy) + ' A ' + f2(aar) + ' ' + f2(aar) + ' 0 0 0 ' + f2(acx + aar * Math.cos(arad)) + ' ' + f2(acy - aar * Math.sin(arad)) + ' L ' + f2(acx) + ' ' + f2(acy) + ' Z" fill="rgba(214,51,108,.18)" stroke="' + PT + '" stroke-width="1.8"/>';
      s += lnr(acx, acy, acx + ar + 10, acy, ST, 3) + lnr(acx, acy, arx, ary, PT, 3) + '<circle cx="' + f2(acx) + '" cy="' + f2(acy) + '" r="4.5" fill="' + ST + '"/>';
      var akind = Math.abs(adeg - 90) < 0.5 ? 'زاویهٔ راست (قائمه)' : Math.abs(adeg - 180) < 0.5 ? 'زاویهٔ نیم‌صفحه' : adeg < 90 ? 'زاویهٔ تند' : 'زاویهٔ باز';
      if (albl === '2') s += tx(180, 254, 'اندازهٔ زاویه = ؟°', PT, 'middle', true, 14);
      else if (albl !== '0') s += tx(180, 254, Math.round(adeg) + '° (' + akind + ')', ST, 'middle', true, 14);
      return wrap(s);
    }
    // ============================== V318 — بستهٔ جامع ابزارهای آموزشی (۱) دبستان
    function pfxPoly(pts, fill, stroke, sw) {
      return '<polygon points="' + pts + '" fill="' + fill + '"' + (stroke && stroke !== 'none' ? ' stroke="' + stroke + '" stroke-width="' + f2(sw || 1.4) + '" stroke-linejoin="round"' : '') + '/>';
    }
    function pfxPath(d, fill, stroke, sw, dash) {
      return '<path d="' + d + '" fill="' + fill + '" stroke="' + stroke + '" stroke-width="' + f2(sw || 1.4) + '" stroke-linejoin="round" stroke-linecap="round"' + (dash ? ' stroke-dasharray="' + dash + '"' : '') + '/>';
    }
    function pfxCls(cx, cy, r, fill, stroke, sw) {
      return '<circle cx="' + f2(cx) + '" cy="' + f2(cy) + '" r="' + f2(r) + '" fill="' + fill + '"' + (stroke && stroke !== 'none' ? ' stroke="' + stroke + '" stroke-width="' + f2(sw || 1.4) + '"' : '') + '/>';
    }
    function pfxArrow(x1, y1, x2, y2, c, w, head) {
      var an = Math.atan2(y2 - y1, x2 - x1), hd = head || 8;
      return ln(x1, y1, x2, y2, c, w || 1.8) + pfxPoly(f2(x2) + ',' + f2(y2) + ' ' + f2(x2 - hd * Math.cos(an - 0.42)) + ',' + f2(y2 - hd * Math.sin(an - 0.42)) + ' ' + f2(x2 - hd * Math.cos(an + 0.42)) + ',' + f2(y2 - hd * Math.sin(an + 0.42)), c);
    }
    if (t === 'axrod') {
      // V322 — ilist به ۸ عدد محدود است و دو میلهٔ آخر (۹ و ۱۰) هرگز رسم نمی‌شدند.
      var rdList = xs('nums', '1,2,3,4,5,6,7,8,9,10').split(/[,،;؛\n]+/).map(function (qrd) { return parseInt(faEn(qrd.trim()), 10); }).filter(function (qrd) { return !isNaN(qrd); }).slice(0, 10);
      if (!rdList.length) rdList = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
      var rdBlank = xs('blank', '0') === '1';
      var rdPal = ['#ffffff', '#e53935', '#8bc34a', '#8e24aa', '#fdd835', '#1b5e20', '#37474f', '#6d4c41', '#1e88e5', '#fb8c00'];
      rdList.slice(0, 10).forEach(function (v, ri) {
        var u = clamp(Math.floor(v), 1, 10), ry = 16 + ri * 18.5;
        s += rc(44, ry, u * 21, 14, rdPal[u - 1], ST, 1.2, 3) + tx(36, ry + 11, rdBlank ? '؟' : String(u), MU, 'end', true, 10);
      });
      s += tx(180, 268, rdBlank ? 'میله‌ها را بشمار: ؟' : 'میله‌های کوئیزنر (۱ تا ۱۰)', ST, 'middle', true, 12);
      return wrap(s);
    }
    function pfxCellMap(raw) {
      var m = {};
      String(raw == null ? '' : raw).split(/[;؛]/).forEach(function (tk) {
        var i2 = tk.indexOf(':');
        if (i2 > 0) { var k0 = faEn(tk.slice(0, i2).trim()), v0 = tk.slice(i2 + 1).trim(); if (k0) m[k0] = v0; }
      });
      return m;
    }
    function pfxEmptySet(raw) {
      var m = {};
      String(raw == null ? '' : raw).split(/[,،;؛\s]+/).forEach(function (tk) {
        var k0 = faEn(tk.trim()); if (k0) m[k0] = 1;
      });
      return m;
    }
    if (t === 'axtower') {
      var twBase = ilist('base', '1,2,3,4'), twOp = xs('op', '+') === '-' ? '−' : '+', twBlank = xs('blank', '0') === '1';
      var twOv = pfxCellMap(xs('ov', '')), twEmpty = pfxEmptySet(xs('empty', '')), twAns = pfxCellMap(xs('ans', ''));
      if (twBase.length < 2) twBase = [1, 2, 3, 4];
      twBase = twBase.slice(0, 6);
      var twLv = [twBase], twCur = twBase, twK;
      while (twCur.length > 1) {
        var twNext = [];
        for (twK = 0; twK < twCur.length - 1; twK++) twNext.push(twOp === '+' ? twCur[twK] + twCur[twK + 1] : Math.abs(twCur[twK] - twCur[twK + 1]));
        twLv.push(twNext); twCur = twNext;
      }
      var twBh = Math.min(34, 216 / twLv.length), twBw = Math.min(58, 300 / twBase.length), twRow = 0;
      for (twRow = 0; twRow < twLv.length; twRow++) {
        var lvIdx = twLv.length - 1 - twRow, lvArr = twLv[lvIdx], ly = 24 + twRow * twBh;
        for (twK = 0; twK < lvArr.length; twK++) {
          var tKey = lvIdx + '_' + twK, bx = 180 + (twK - (lvArr.length - 1) / 2) * twBw - twBw / 2;
          var isExpEmp = !!twEmpty[tKey], stuVal = twAns[tKey] || '', ovVal = twOv[tKey];
          var autoHide = !isExpEmp && ovVal == null && twBlank && ((lvIdx + twK) % 3 === 1);
          var bg = isExpEmp ? (stuVal ? 'rgba(39,196,168,.16)' : '#ffffff') : autoHide ? '#fff8e1' : lvIdx === 0 ? 'rgba(108,99,245,.16)' : '#ffffff';
          var stk = isExpEmp ? (stuVal ? '#1f8a78' : '#94a3b8') : autoHide ? PT : ST;
          var txt = isExpEmp ? stuVal : (ovVal != null ? ovVal : (autoHide ? '؟' : String(lvArr[twK])));
          var tCol = isExpEmp ? '#1f8a78' : autoHide ? PT : ST;
          s += '<rect x="' + f2(bx) + '" y="' + f2(ly) + '" width="' + f2(twBw - 3) + '" height="' + f2(twBh - 3) + '" rx="4" fill="' + bg + '" stroke="' + stk + '" stroke-width="1.6"' + (isExpEmp && !stuVal ? ' stroke-dasharray="4 3"' : '') + '/>';
          if (txt) s += tx(bx + (twBw - 3) / 2, ly + twBh * 0.66, txt, tCol, 'middle', true, 13);
        }
      }
      s += tx(180, 268, twOp === '+' ? 'هر آجر = جمع دو آجر زیرین' : 'هر آجر = تفاضل دو آجر زیرین', MU, 'middle', false, 11);
      return wrap(s);
    }
    if (t === 'axmulwheel') {
      var mwC = clamp(Math.floor(num(X.c, 3)), 1, 12), mwN = clamp(Math.floor(num(X.n, 10)), 3, 12);
      var mwOp = xs('op', '×') === '+' ? '+' : '×', mwHide = xs('hide', '0') === '1', mwJ;
      var mwOv = pfxCellMap(xs('ov', '')), mwEmpty = pfxEmptySet(xs('empty', '')), mwAns = pfxCellMap(xs('ans', ''));
      var cEmp = !!mwEmpty.center, cStu = mwAns.center || '', cTxt = cEmp ? cStu : (mwOv.center != null ? mwOv.center : String(mwC));
      s += pfxCls(180, 142, 96, 'none', ACC, 1.6) + pfxCls(180, 142, 34, cEmp ? '#ffffff' : 'rgba(108,99,245,.16)', cEmp && !cStu ? '#94a3b8' : ACC, 2.2);
      if (cTxt) s += tx(180, 140, cTxt, cEmp ? '#1f8a78' : ACC, 'middle', true, 18);
      s += tx(180, 158, mwOp, ACC, 'middle', true, 12);
      for (mwJ = 0; mwJ < mwN; mwJ++) {
        var mKey = 's_' + mwJ, ma = -Math.PI / 2 + mwJ * 2 * Math.PI / mwN, ox = 180 + 96 * Math.cos(ma), oy = 142 + 96 * Math.sin(ma);
        var ix = 180 + 56 * Math.cos(ma), iy = 142 + 56 * Math.sin(ma);
        var mv = mwOp === '+' ? mwC + (mwJ + 1) : mwC * (mwJ + 1);
        var isEmp = !!mwEmpty[mKey], stuV = mwAns[mKey] || '', ovV = mwOv[mKey];
        var oh = !isEmp && ovV == null && mwHide && mwJ % 3 === 2;
        s += lnr(180 + 34 * Math.cos(ma), 142 + 34 * Math.sin(ma), ox, oy, GR, 1.6);
        s += pfxCls(ix, iy, 11, '#f8fafc', '#cbd5e1', 1.1) + tx(ix, iy + 4, String(mwJ + 1), MU, 'middle', true, 9.5);
        s += '<circle cx="' + f2(ox) + '" cy="' + f2(oy) + '" r="17" fill="' + (isEmp ? (stuV ? 'rgba(39,196,168,.16)' : '#ffffff') : oh ? '#fff8e1' : '#ffffff') + '" stroke="' + (isEmp ? (stuV ? '#1f8a78' : '#94a3b8') : oh ? PT : ST) + '" stroke-width="1.8"' + (isEmp && !stuV ? ' stroke-dasharray="4 3"' : '') + '/>';
        var oTxt = isEmp ? stuV : (ovV != null ? ovV : (oh ? '؟' : String(mv)));
        if (oTxt) s += tx(ox, oy + 5, oTxt, isEmp ? '#1f8a78' : oh ? PT : ST, 'middle', true, 12);
      }
      return wrap(s);
    }
    if (t === 'axmultgrid') {
      var mgRows = ilist('rows', '2,4,7'), mgCols = ilist('cols', '3,5,8'), mgBlank = Math.floor(num(X.blank, 0));
      var mgOv = pfxCellMap(xs('ov', '')), mgEmpty = pfxEmptySet(xs('empty', '')), mgAns = pfxCellMap(xs('ans', ''));
      if (!mgRows.length) mgRows = [2, 4, 7];
      if (!mgCols.length) mgCols = [3, 5, 8];
      mgRows = mgRows.slice(0, 4); mgCols = mgCols.slice(0, 4);
      var mgCw = Math.min(56, 268 / (mgCols.length + 1)), mgCh = Math.min(42, 216 / (mgRows.length + 1));
      var mgX0 = 180 - mgCw * (mgCols.length + 1) / 2, mgY0 = 26 + (218 - mgCh * (mgRows.length + 1)) / 2, mgR, mgC;
      var drawMgCell = function (key, x2, y2, defTxt, autoQ, defBg, defCol) {
        var isE = !!mgEmpty[key], stV = mgAns[key] || '', ovV = mgOv[key];
        var shQ = !isE && ovV == null && autoQ;
        var bg = isE ? (stV ? 'rgba(39,196,168,.16)' : '#ffffff') : shQ ? '#fff8e1' : defBg;
        var stk = isE && stV ? '#1f8a78' : ST;
        var txt = isE ? stV : (ovV != null ? ovV : (shQ ? '؟' : defTxt));
        var col = isE ? '#1f8a78' : shQ ? PT : defCol;
        s += rc(x2, y2, mgCw, mgCh, bg, stk, 1.4);
        if (txt) s += tx(x2 + mgCw / 2, y2 + mgCh * 0.66, txt, col, 'middle', true, 14);
      };
      for (mgC = 0; mgC < mgCols.length; mgC++) {
        drawMgCell('c' + mgC, mgX0 + (mgC + 1) * mgCw, mgY0, String(mgCols[mgC]), mgBlank === 2, 'rgba(39,196,168,.18)', ST);
      }
      for (mgR = 0; mgR < mgRows.length; mgR++) {
        drawMgCell('r' + mgR, mgX0, mgY0 + (mgR + 1) * mgCh, String(mgRows[mgR]), mgBlank === 2, 'rgba(39,196,168,.18)', ST);
        for (mgC = 0; mgC < mgCols.length; mgC++) {
          drawMgCell(mgR + '_' + mgC, mgX0 + (mgC + 1) * mgCw, mgY0 + (mgR + 1) * mgCh, String(mgRows[mgR] * mgCols[mgC]), mgBlank === 1 && (mgR + mgC) % 2 === 1, '#ffffff', ACC);
        }
      }
      s += rc(mgX0, mgY0, mgCw, mgCh, 'rgba(108,99,245,.16)', ST, 1.8) + tx(mgX0 + mgCw / 2, mgY0 + mgCh * 0.66, '×', ACC, 'middle', true, 15);
      return wrap(s);
    }
    if (t === 'axnet') {
      var ntShape = xs('shape', 'cube').toLowerCase(), ntMode = xs('mode', 'color').toLowerCase();
      var ntPal = ['#ef5350', '#42a5f5', '#66bb6a', '#fdd835', '#ab47bc', '#fb8c00'];
      var ntFill = function (i2) { return ntMode === 'outline' ? '#ffffff' : ntPal[i2 % ntPal.length]; };
      var ntFace = function (x2, y2, w2, h2, i2, lbl2) {
        var fl2 = ntFill(i2);
        return rc(x2, y2, w2, h2, fl2, ST, 1.6) + tx(x2 + w2 / 2, y2 + h2 / 2 + 5, ntMode === 'blank' ? '؟' : String(lbl2), ntMode === 'outline' ? ST : '#ffffff', 'middle', true, 13);
      };
      var ntU = ntShape === 'cuboid' ? 44 : 52;
      if (ntShape === 'cube' || ntShape === 'cuboid') {
        var nW = ntShape === 'cuboid' ? 70 : ntU, nH = ntU, nX0 = 180 - nW * 1.5, nY0 = 60;
        s += ntFace(nX0, nY0, nW, nH, 0, 1) + ntFace(nX0 + nW, nY0, nW, nH, 1, 2) + ntFace(nX0 + 2 * nW, nY0, nW, nH, 2, 3) + ntFace(nX0 + 3 * nW, nY0, nW, nH, 3, 4);
        s += ntFace(nX0 + nW, nY0 - nH, nW, nH, 4, 5) + ntFace(nX0 + nW, nY0 + nH, nW, nH, 5, 6);
      } else if (ntShape === 'pyramid') {
        s += ntFace(180 - ntU, 96, ntU * 2, ntU * 2, 0, 1);
        s += pfxPoly(f2(180 - ntU) + ',' + 96 + ' ' + f2(180) + ',' + 26 + ' ' + f2(180 + ntU) + ',' + 96, ntFill(1), ST, 1.6) + tx(180, 76, '۲', ntMode === 'outline' ? ST : '#fff', 'middle', true, 13);
        s += pfxPoly(f2(180 - ntU) + ',' + f2(96 + ntU * 2) + ' ' + f2(180) + ',' + f2(96 + ntU * 3) + ' ' + f2(180 + ntU) + ',' + f2(96 + ntU * 2), ntFill(2), ST, 1.6);
        s += pfxPoly(f2(180 - ntU) + ',' + 96 + ' ' + f2(180 - ntU) + ',' + f2(96 + ntU * 2) + ' ' + f2(180 - ntU * 2) + ',' + f2(96 + ntU), ntFill(3), ST, 1.6) + tx(180 - ntU * 1.4, 96 + ntU, '۴', ntMode === 'outline' ? ST : '#fff', 'middle', true, 13);
        s += pfxPoly(f2(180 + ntU) + ',' + 96 + ' ' + f2(180 + ntU) + ',' + f2(96 + ntU * 2) + ' ' + f2(180 + ntU * 2) + ',' + f2(96 + ntU), ntFill(4), ST, 1.6) + tx(180 + ntU * 1.4, 96 + ntU, '۵', ntMode === 'outline' ? ST : '#fff', 'middle', true, 13);
      } else {
        var pT = 36, pRw = 52, pRh = 74, pRx = 180 - pRw * 1.5, pRy = 92;
        s += ntFace(pRx, pRy, pRw, pRh, 0, 1) + ntFace(pRx + pRw, pRy, pRw, pRh, 1, 2) + ntFace(pRx + 2 * pRw, pRy, pRw, pRh, 2, 3);
        s += pfxPoly(f2(pRx) + ',' + f2(pRy) + ' ' + f2(pRx + pRw / 2) + ',' + f2(pRy - pT) + ' ' + f2(pRx + pRw) + ',' + f2(pRy), ntFill(3), ST, 1.6);
        s += pfxPoly(f2(pRx) + ',' + f2(pRy + pRh) + ' ' + f2(pRx + pRw / 2) + ',' + f2(pRy + pRh + pT) + ' ' + f2(pRx + pRw) + ',' + f2(pRy + pRh), ntFill(4), ST, 1.6);
      }
      s += tx(180, 268, ntShape === 'pyramid' ? 'گستردهٔ هرم — ۵ وجه' : ntShape === 'cuboid' ? 'گستردهٔ مکعب‌مستطیل — ۶ وجه' : ntShape === 'cube' ? 'گستردهٔ مکعب — ۶ وجه' : 'گستردهٔ منشور — ۵ وجه', MU, 'middle', true, 12);
      return wrap(s);
    }
    if (t === 'axcubes') {
      var cbView = xs('view', 'iso').toLowerCase(), cbRowsRaw = xs('map', '2,1,3;1,0,1;2,2,1').split(/[;؛]/);
      var cbParsed = cbRowsRaw.map(function (rw) { return rw.split(/[,،]/).map(function (q2) { var iv = parseInt(faEn(q2.trim()), 10); return clamp(isFinite(iv) ? iv : 0, 0, 5); }); }).filter(function (rw) { return rw.length; }).slice(0, 4);
      if (!cbParsed.length) cbParsed = [[2, 1, 3], [1, 0, 1], [2, 2, 1]];
      var cgN = cbParsed.length, cgM = 1;
      cbParsed.forEach(function (rw) { if (rw.length > cgM) cgM = Math.min(4, rw.length); });
      var cbMap = cbParsed.map(function (rw) { var a = []; for (var k0 = 0; k0 < cgM; k0++) a.push(rw[k0] || 0); return a; });
      var cbTotal = 0, cbMaxH = 1;
      cbMap.forEach(function (rw) { rw.forEach(function (h2) { cbTotal += h2; if (h2 > cbMaxH) cbMaxH = h2; }); });
      if (cbView === 'top') {
        var cgW = Math.min(54, 220 / cgM), cgH = Math.min(50, 184 / cgN);
        var cgX0 = 180 - (cgM * cgW) / 2, cgY0 = 126 - (cgN * cgH) / 2;
        s += tx(180, 26, 'نمای از بالا (تعداد مکعب در هر ستون)', ST, 'middle', true, 12);
        for (var cr = 0; cr < cgN; cr++) for (var cc2 = 0; cc2 < cgM; cc2++) {
          var chh = cbMap[cr][cc2], cx2 = cgX0 + cc2 * cgW, cy2 = cgY0 + cr * cgH;
          var topFill = chh === 0 ? '#f8fafc' : chh === 1 ? 'rgba(108,99,245,.16)' : chh === 2 ? 'rgba(108,99,245,.30)' : 'rgba(108,99,245,.46)';
          s += rc(cx2 + 1.5, cy2 + 1.5, cgW - 3, cgH - 3, topFill, ST, 1.5, 4) +
            tx(cx2 + cgW / 2, cy2 + cgH * 0.64, xs('hide', '0') === '1' && chh > 0 ? '' : String(chh), chh === 0 ? MU : ST, 'middle', true, 15);
        }
        s += tx(180, cgY0 + cgN * cgH + 22, '▼ روبه‌رو', ACC, 'middle', true, 11);
      } else if (cbView === 'front' || cbView === 'side') {
        var prof = [];
        if (cbView === 'front') {
          for (var fc = 0; fc < cgM; fc++) { var mxF = 0; for (var fr = 0; fr < cgN; fr++) if (cbMap[fr][fc] > mxF) mxF = cbMap[fr][fc]; prof.push(mxF); }
        } else {
          for (var sr = 0; sr < cgN; sr++) { var mxS = 0; for (var sc2 = 0; sc2 < cgM; sc2++) if (cbMap[sr][sc2] > mxS) mxS = cbMap[sr][sc2]; prof.push(mxS); }
        }
        var pCols = Math.max(1, prof.length), uSz = Math.min(44, 220 / pCols, 172 / Math.max(3, cbMaxH));
        var pTw = pCols * uSz, pX0 = 180 - pTw / 2, pBaseY = 222;
        s += tx(180, 26, cbView === 'front' ? 'نمای روبه‌رو' : 'نمای از پهلو (چپ به راست: پشت به جلو)', ST, 'middle', true, 12);
        s += lnr(pX0 - 18, pBaseY, pX0 + pTw + 18, pBaseY, ST, 2.5);
        prof.forEach(function (colH, ci2) {
          var ux = pX0 + ci2 * uSz;
          for (var lv = 0; lv < colH; lv++) {
            var uy = pBaseY - (lv + 1) * uSz;
            s += rc(ux + 1.5, uy + 1.5, uSz - 3, uSz - 3, cbView === 'front' ? 'rgba(108,99,245,.28)' : 'rgba(39,196,168,.28)', ST, 1.6, 3);
          }
          if (xs('hide', '0') !== '1') s += tx(ux + uSz / 2, pBaseY + 18, String(colH), MU, 'middle', true, 11);
        });
      } else {
        var cbS = Math.min(26, 175 / Math.max(3, cgN + cgM)), dx = cbS * 0.88, dy = cbS * 0.48, dz = cbS * 0.94;
        var midDiff = (cgM - cgN) / 2, originX = 180 - midDiff * dx, originY = 216;
        for (var ir = 0; ir < cgN; ir++) {
          for (var ic = 0; ic < cgM; ic++) {
            var hh = cbMap[ir][ic];
            var baseX = originX + (ic - ir) * dx, baseY = originY - (cgN - 1 - ir + ic) * dy * 0.65;
            for (var z = 0; z < hh; z++) {
              var ty = baseY - (z + 1) * dz, by = baseY - z * dz;
              s += pfxPoly(f2(baseX) + ',' + f2(ty - dy) + ' ' + f2(baseX + dx) + ',' + f2(ty) + ' ' + f2(baseX) + ',' + f2(ty + dy) + ' ' + f2(baseX - dx) + ',' + f2(ty), '#eef2ff', ST, 1.2);
              s += pfxPoly(f2(baseX - dx) + ',' + f2(ty) + ' ' + f2(baseX) + ',' + f2(ty + dy) + ' ' + f2(baseX) + ',' + f2(by + dy) + ' ' + f2(baseX - dx) + ',' + f2(by), '#a5b4fc', ST, 1.2);
              s += pfxPoly(f2(baseX) + ',' + f2(ty + dy) + ' ' + f2(baseX + dx) + ',' + f2(ty) + ' ' + f2(baseX + dx) + ',' + f2(by) + ' ' + f2(baseX) + ',' + f2(by + dy), '#6ee7b7', ST, 1.2);
            }
          }
        }
      }
      var cbFooter = xs('hide', '0') === '1' ? 'تعداد کل مکعب‌ها: ؟'
        : cbView === 'top' ? ('نمای از بالا — مجموع مکعب‌ها: ' + cbTotal)
        : cbView === 'front' ? 'نمای روبه‌رو — بلندترین ستون هر ردیف عمودی'
        : cbView === 'side' ? 'نمای از پهلو — بلندترین ستون هر ردیف افقی'
        : ('تعداد مکعب‌ها: ' + cbTotal);
      s += tx(180, 266, cbFooter, xs('hide', '0') === '1' ? PT : MU, 'middle', true, 12);
      return wrap(s);
    }
    if (t === 'axcalendar') {
      var clMonth = xs('month', 'مهر'), clStart = clamp(Math.floor(num(X.start, 3)), 0, 6), clDays = clamp(Math.floor(num(X.days, 30)), 20, 31);
      var clMark = String(xs('mark', '12,23')).split(/[,،;]/).map(function (q3) { return parseInt(faEn(q3.trim()), 10); }).filter(function (q3) { return isFinite(q3); });
      var clBlank = xs('blank', '0') === '1', clWd = ['ش', 'ی', 'د', 'س', 'چ', 'پ', 'ج'], clCw = 44, clCh = 30, clX0 = 180 - clCw * 3.5, clY0 = 46;
      var clOv = pfxCellMap(xs('ov', '')), clEmpty = pfxEmptySet(xs('empty', '')), clAns = pfxCellMap(xs('ans', ''));
      s += tx(180, 30, 'تقویم ' + clMonth, ST, 'middle', true, 14);
      for (var cd = 0; cd < 7; cd++) s += rc(clX0 + cd * clCw, clY0, clCw, clCh, 'rgba(108,99,245,.16)', ST, 1.2) + tx(clX0 + cd * clCw + clCw / 2, clY0 + clCh * 0.68, clWd[6 - cd], ACC, 'middle', true, 12);
      for (var ci = 0; ci < clDays; ci++) {
        var cellIdx = ci + clStart, col2 = 6 - (cellIdx % 7), row2 = Math.floor(cellIdx / 7), dayN = ci + 1, dKey = String(dayN);
        var bx2 = clX0 + col2 * clCw, by2 = clY0 + clCh + row2 * clCh, isMark = clMark.indexOf(dayN) >= 0;
        var isExpE = !!clEmpty[dKey], stVal = clAns[dKey] || '', ovVal2 = clOv[dKey];
        var autoB = !isExpE && ovVal2 == null && clBlank && dayN % 5 === 0;
        var cBg = isExpE ? (stVal ? 'rgba(39,196,168,.18)' : '#ffffff') : autoB ? '#fff8e1' : isMark ? 'rgba(39,196,168,.26)' : '#ffffff';
        var cTxt2 = isExpE ? stVal : (ovVal2 != null ? ovVal2 : (autoB ? '؟' : dKey));
        var cCol = isExpE ? '#1f8a78' : autoB ? PT : isMark ? '#00796b' : ST;
        s += rc(bx2, by2, clCw, clCh, cBg, isExpE && stVal ? '#1f8a78' : ST, 1.1);
        if (cTxt2) s += tx(bx2 + clCw / 2, by2 + clCh * 0.66, cTxt2, cCol, 'middle', isMark || autoB || isExpE, 12);
      }
      return wrap(s);
    }
    if (t === 'axlever') {
      var lvKind = xs('kind', 'lever1').toLowerCase(), lvLoad = num(X.load, 60), lvEff = num(X.effort, 20);
      var lvL1 = Math.max(0.5, num(X.l1, 2)), lvL2 = Math.max(0.5, num(X.l2, 1)), lvMa = lvL2 / lvL1;
      if (lvKind === 'pulley') {
        s += rc(96, 18, 168, 8, '#cbd5e1', ST, 1.6, 3);
        s += lnr(142, 26, 142, 118, ST, 2.2) + lnr(182, 118, 182, 60, ST, 2.2) + lnr(226, 60, 226, 152, ACC, 2.4);
        s += lnr(204, 26, 204, 60, ST, 3) + pfxCls(204, 60, 22, 'rgba(108,99,245,.16)', ACC, 2.2) + pfxCls(204, 60, 5, ST, 'none', 0);
        s += pfxCls(162, 118, 20, 'rgba(39,196,168,.22)', GRN, 2.2) + pfxCls(162, 118, 5, ST, 'none', 0);
        s += lnr(162, 138, 162, 162, ST, 2.4) + rc(134, 162, 56, 38, 'rgba(214,51,108,.16)', PT, 1.8, 6);
        s += tx(162, 186, 'بار: ' + nm(lvLoad) + ' N', PT, 'middle', true, 11.5);
        s += pfxArrow(226, 136, 226, 182, ACC, 2.6, 9) + tx(274, 168, 'نیرو: ' + nm(lvEff) + ' N', ACC, 'middle', true, 11.5);
        s += tx(180, 252, 'قرقرهٔ مرکب (ثابت و متحرک) — مزیت مکانیکی ≈ ۲', MU, 'middle', true, 11.5);
        return wrap(s);
      }
      if (lvKind === 'gears' || lvKind === 'gear') {
        var grAx = 126, grBx = 198, grCy = 128, grA = 44, grB = 26;
        for (var gj = 0; gj < 16; gj++) { var ga = gj * Math.PI / 8; s += lnr(grAx + (grA - 5) * Math.cos(ga), grCy + (grA - 5) * Math.sin(ga), grAx + (grA + 6) * Math.cos(ga), grCy + (grA + 6) * Math.sin(ga), ACC, 3.6); }
        for (var gk = 0; gk < 10; gk++) { var gb2 = gk * Math.PI / 5 + Math.PI / 10; s += lnr(grBx + (grB - 4) * Math.cos(gb2), grCy + (grB - 4) * Math.sin(gb2), grBx + (grB + 5.5) * Math.cos(gb2), grCy + (grB + 5.5) * Math.sin(gb2), GLD, 3.4); }
        s += pfxCls(grAx, grCy, grA, '#eef2ff', ACC, 2.2) + pfxCls(grAx, grCy, 8, '#ffffff', ACC, 1.8);
        s += pfxCls(grBx, grCy, grB, '#fef9c3', GLD, 2.2) + pfxCls(grBx, grCy, 6, '#ffffff', GLD, 1.8);
        s += tx(grAx, grCy + 26, '۱۶ دندانه', ACC, 'middle', true, 10.5) + tx(grBx, grCy + 20, '۱۰ دندانه', '#a06a00', 'middle', true, 10);
        s += tx(180, 244, 'چرخ‌دنده‌های درگیر — نسبت دور = ۱٫۶ (خلاف جهت هم)', MU, 'middle', true, 11.5);
        return wrap(s);
      }
      var lvX1 = 46, lvX2 = 314, lvY = 158;
      var lvFulc = lvKind === 'lever1' ? 180 : 68;
      var ldX = lvKind === 'lever1' ? 76 : lvKind === 'lever2' ? 168 : 286;
      var efX = lvKind === 'lever1' ? 284 : lvKind === 'lever2' ? 286 : 168;
      s += lnr(lvX1, lvY, lvX2, lvY, ST, 6.5);
      s += pfxPoly(f2(lvFulc) + ',' + f2(lvY + 3) + ' ' + f2(lvFulc - 18) + ',' + f2(lvY + 40) + ' ' + f2(lvFulc + 18) + ',' + f2(lvY + 40), 'rgba(108,99,245,.25)', ST, 2);
      s += tx(lvFulc, lvY + 56, 'تکیه‌گاه', MU, 'middle', true, 10.5);
      s += rc(ldX - 28, lvY - 46, 56, 36, 'rgba(214,51,108,.18)', PT, 1.8, 6);
      s += tx(ldX, lvY - 30, 'بار', PT, 'middle', true, 10) + tx(ldX, lvY - 16, nm(lvLoad) + ' N', PT, 'middle', true, 11);
      s += pfxArrow(ldX, lvY - 8, ldX, lvY + 30, PT, 2.6, 8);
      if (lvKind === 'lever1') {
        s += pfxArrow(efX, lvY - 56, efX, lvY - 8, ACC, 2.8, 9);
        s += rc(efX - 30, lvY - 90, 60, 30, 'rgba(108,99,245,.16)', ACC, 1.6, 6) + tx(efX, lvY - 71, 'نیرو ' + nm(lvEff) + ' N', ACC, 'middle', true, 10.5);
      } else {
        s += pfxArrow(efX, lvY + 42, efX, lvY + 6, ACC, 2.8, 9);
        s += rc(efX - 32, lvY - 86, 64, 30, 'rgba(108,99,245,.16)', ACC, 1.6, 6) + tx(efX, lvY - 67, 'نیرو ' + nm(lvEff) + ' N', ACC, 'middle', true, 10.5);
        s += ln(efX, lvY - 56, efX, lvY - 4, ACC, 1.2, true);
      }
      var kName = lvKind === 'lever1' ? 'اول (تکیه‌گاه وسط)' : lvKind === 'lever2' ? 'دوم (بار وسط)' : 'سوم (نیرو وسط)';
      s += tx(180, 254, 'اهرم نوع ' + kName + ' — مزیت مکانیکی = ' + nm(lvL2) + ' ÷ ' + nm(lvL1) + ' = ' + nm(Math.round(lvMa * 100) / 100), MU, 'middle', true, 11);
      return wrap(s);
    }
    if (t === 'axcycle') {
      var cyKind = xs('kind', 'water').toLowerCase();
      var cySets = {
        water: ['تبخیر', 'میعان', 'بارش', 'جمع‌آوری'],
        frog: ['تخم', 'نوزاد (قورباغه)', 'قورباغهٔ جوان', 'قورباغهٔ بالغ'],
        plant: ['دانه', 'جوانه', 'گیاه کوچک', 'گیاه گل‌دار'],
        food: ['خورشید', 'گیاه', 'گیاه‌خوار', 'گوشت‌خوار']
      };
      var cyLabs = (cySets[cyKind] || cySets.water), cyCx = 180, cyCy = 138, cyR = 86;
      for (var cyi = 0; cyi < 4; cyi++) {
        var cyA = -Math.PI / 2 + cyi * Math.PI / 2, nx = cyCx + cyR * Math.cos(cyA), ny = cyCy + cyR * 0.86 * Math.sin(cyA);
        var na2 = cyA + Math.PI / 4;
        s += pfxPath('M ' + f2(cyCx + cyR * 1.02 * Math.cos(na2 - 0.22)) + ' ' + f2(cyCy + cyR * 0.88 * Math.sin(na2 - 0.22)) + ' A ' + f2(cyR * 1.02) + ' ' + f2(cyR * 0.88) + ' 0 0 1 ' + f2(cyCx + cyR * 1.02 * Math.cos(na2 + 0.22)) + ' ' + f2(cyCy + cyR * 0.88 * Math.sin(na2 + 0.22)), 'none', ACC, 2.2);
        var tipX = cyCx + cyR * 1.02 * Math.cos(na2 + 0.22), tipY = cyCy + cyR * 0.88 * Math.sin(na2 + 0.22), ta = na2 + Math.PI / 2 + 0.5;
        s += pfxPoly(f2(tipX) + ',' + f2(tipY) + ' ' + f2(tipX - 9 * Math.cos(ta - 0.4)) + ',' + f2(tipY - 9 * Math.sin(ta - 0.4)) + ' ' + f2(tipX - 9 * Math.cos(ta + 0.4)) + ',' + f2(tipY - 9 * Math.sin(ta + 0.4)), ACC);
        s += pfxCls(nx, ny, 30, '#ffffff', ST, 1.8) + tx(nx, ny + 5, cyLabs[cyi], ST, 'middle', true, 11);
      }
      s += tx(180, 262, cyKind === 'water' ? 'چرخهٔ آب در طبیعت' : cyKind === 'frog' ? 'دگردیسی قورباغه' : cyKind === 'plant' ? 'مراحل رشد گیاه' : 'زنجیرهٔ غذایی', MU, 'middle', true, 12);
      return wrap(s);
    }

    // ===== V327 — ابزارهای تازهٔ دبستان: رنگ‌آمیزی، فارسی و علوم =====
    var pFa = function (v) { return String(v).replace(/[0-9]/g, function (d) { return '۰۱۲۳۴۵۶۷۸۹'[+d]; }); };

    if (t === 'axpaintgrid') {
      /* شبکهٔ رنگ‌آمیزی: با کلیک روی هر خانه در ویرایشگر معلم یا آزمون دانش‌آموز رنگ می‌شود */
      var pgRows = clamp(Math.round(num(xs('rows', '4'), 4)), 1, 8);
      var pgCols = clamp(Math.round(num(xs('cols', '4'), 4)), 1, 8);
      var pgGoal = clamp(Math.round(num(xs('goal', '0'), 0)), 0, pgRows * pgCols);
      var parsePgCells = function (raw) {
        return String(raw || '').split(/[;؛]/).map(function (q) {
          var a2 = faEn(q).split(/[,،]/);
          return a2.length >= 2 ? [Math.round(+a2[0]), Math.round(+a2[1])] : null;
        }).filter(function (c3) { return c3 && c3[0] >= 1 && c3[0] <= pgRows && c3[1] >= 1 && c3[1] <= pgCols; });
      };
      var pgCells = parsePgCells(xs('cells', ''));
      var pgAns = parsePgCells(xs('ansCells', ''));
      var pgW = Math.min(300, pgCols * 42), pgH = Math.min(150, pgRows * 34);
      var pgCw = pgW / pgCols, pgCh = pgH / pgRows, pgX0 = (360 - pgW) / 2, pgY0 = 62 + (150 - pgH) / 2;
      s += tx(180, 38, 'خانه‌ها را رنگ کن', ST, 'middle', true, 13);
      for (var pr = 0; pr < pgRows; pr++) {
        for (var pc = 0; pc < pgCols; pc++) {
          var onTeacher = pgCells.some(function (c4) { return c4[0] === pr + 1 && c4[1] === pc + 1; });
          var onStudent = pgAns.some(function (c5) { return c5[0] === pr + 1 && c5[1] === pc + 1; });
          var fillCol = onStudent ? 'rgba(39,196,168,.65)' : onTeacher ? 'rgba(108,99,245,.62)' : '#ffffff';
          s += rc(pgX0 + pc * pgCw, pgY0 + pr * pgCh, pgCw, pgCh, fillCol, ST, 1.5, 4);
        }
      }
      if (pgGoal > 0) s += tx(180, pgY0 + pgH + 26, 'تعداد خانه‌های هدف برای رنگ‌آمیزی: ' + pFa(pgGoal) + ' خانه', ACC, 'middle', true, 12);
      s += tx(180, 260, 'با کلیک یا لمس هر خانه، آن خانه رنگ یا پاک می‌شود', MU, 'middle', false, 10.5);
      return wrap(s);
    }

    if (t === 'axletterforms') {
      /* نویسه‌های حرف: تنها، آغازین، میانی، پایانی */
      var lfCh = String(xs('ch', 'ب')).trim().charAt(0) || 'ب';
      var LF = {
        'ب': ['ب', 'بـ', 'ـبـ', 'ـب'], 'پ': ['پ', 'پـ', 'ـپـ', 'ـپ'], 'ت': ['ت', 'تـ', 'ـتـ', 'ـت'],
        'ث': ['ث', 'ثـ', 'ـثـ', 'ـث'], 'ج': ['ج', 'جـ', 'ـجـ', 'ـج'], 'چ': ['چ', 'چـ', 'ـچـ', 'ـچ'],
        'ح': ['ح', 'حـ', 'ـحـ', 'ـح'], 'خ': ['خ', 'خـ', 'ـخـ', 'ـخ'], 'س': ['س', 'سـ', 'ـسـ', 'ـس'],
        'ش': ['ش', 'شـ', 'ـشـ', 'ـش'], 'ص': ['ص', 'صـ', 'ـصـ', 'ـص'], 'ض': ['ض', 'ضـ', 'ـضـ', 'ـض'],
        'ع': ['ع', 'عـ', 'ـعـ', 'ـع'], 'غ': ['غ', 'غـ', 'ـغـ', 'ـغ'], 'ف': ['ف', 'فـ', 'ـفـ', 'ـف'],
        'ق': ['ق', 'قـ', 'ـقـ', 'ـق'], 'ک': ['ک', 'کـ', 'ـکـ', 'ـک'], 'گ': ['گ', 'گـ', 'ـگـ', 'ـگ'],
        'ل': ['ل', 'لـ', 'ـلـ', 'ـل'], 'م': ['م', 'مـ', 'ـمـ', 'ـم'], 'ن': ['ن', 'نـ', 'ـنـ', 'ـن'],
        'ه': ['ه', 'هـ', 'ـهـ', 'ـه'], 'ی': ['ی', 'یـ', 'ـیـ', 'ـی']
      };
      var lfForms = LF[lfCh] || [lfCh, lfCh + 'ـ', 'ـ' + lfCh + 'ـ', 'ـ' + lfCh];
      var lfCap = ['تنها', 'آغازین', 'میانی', 'پایانی'];
      for (var li = 0; li < 4; li++) {
        var lfx = 306 - li * 72;
        s += rc(lfx - 30, 84, 60, 60, 'rgba(108,99,245,.10)', ACC, 1.6, 10);
        s += tx(lfx, 124, lfForms[li], ST, 'middle', true, 26);
        s += tx(lfx, 164, lfCap[li], MU, 'middle', false, 11);
      }
      s += tx(180, 34, 'نویسه‌های حرف «' + lfCh + '»', ST, 'middle', true, 13);
      s += tx(180, 206, 'حرف در اول، میان و آخر کلمه شکلش عوض می‌شود', MU, 'middle', false, 10);
      return wrap(s);
    }

    if (t === 'axsyllablechart') {
      /* هجاهای کلمه: در حالت پرسش (ask) کلمه یکجا نمایش داده می‌شود تا دانش‌آموز بخش کند؛ در حالت آموزش (show) بخش‌ها نمایش داده می‌شوند */
      var syRaw = String(xs('word', 'مدرسه')).trim() || 'مدرسه';
      var syMode = xs('mode', 'ask').toLowerCase();
      var syClean = syRaw.replace(/[-–—]/g, '');
      s += tx(180, 38, 'بخش‌کردن کلمه (هجاها)', ST, 'middle', true, 13.5);
      if (syMode === 'show') {
        var syList = syRaw.split(/[-–—]/).map(function (q5) { return q5.trim(); }).filter(Boolean).slice(0, 6);
        if (!syList.length) syList = [syClean];
        var syN = syList.length, syW = Math.min(72, 292 / syN), syX0 = 180 + (syN * (syW + 8) - 8) / 2;
        syList.forEach(function (sy2, si2) {
          var bx = syX0 - syW - si2 * (syW + 8);
          s += rc(bx, 82, syW, 52, 'rgba(39,196,168,.16)', '#1f8a78', 1.8, 10);
          s += tx(bx + syW / 2, 114, sy2, ST, 'middle', true, 18);
          s += pfxCls(bx + syW / 2, 150, 4.2, '#1f8a78', 'none', 0);
        });
        s += tx(180, 184, 'کلمهٔ «' + syClean + '» ← ' + pFa(syN) + ' بخش', ACC, 'middle', true, 13);
      } else {
        s += rc(84, 72, 192, 62, 'rgba(108,99,245,.12)', ACC, 2.2, 14);
        s += tx(180, 111, syClean, ST, 'middle', true, 24);
        s += tx(180, 168, 'این کلمه را بخش کن و هر بخش را در یک کادر بنویس', ACC, 'middle', true, 12);
        for (var sb = 0; sb < 3; sb++) {
          var sbx = 228 - sb * 72;
          s += '<rect x="' + f2(sbx) + '" y="186" width="56" height="36" rx="8" fill="#ffffff" stroke="#94a3b8" stroke-width="1.5" stroke-dasharray="4 3"/>';
        }
        s += tx(72, 209, '+', '#1f8a78', 'middle', true, 18);
      }
      s += tx(180, 254, 'دانش‌آموز در آزمون با دکمهٔ + کادر اضافه می‌کند و بخش‌ها را می‌نویسد', MU, 'middle', false, 10);
      return wrap(s);
    }

    if (t === 'axmagnet') {
      /* آهنربا و قطب‌ها: دو آهنربای کامل میله‌ای دو رنگ (N قرمز، S آبی) */
      var mgMode = xs('mode', 'attract').toLowerCase();
      if (mgMode !== 'repel' && mgMode !== 'repel_s') mgMode = 'attract';
      var isAttr = mgMode === 'attract';
      s += tx(180, 36, isAttr ? 'قطب‌های ناهم‌نام — جذب یکدیگر' : 'قطب‌های هم‌نام — دفع یکدیگر', ST, 'middle', true, 13.5);
      var mgY = 142, mgH = 46;
      var drawBarMag = function (mx0, leftPole, rightPole) {
        var lCol = leftPole === 'N' ? '#e53935' : '#1e88e5', rCol = rightPole === 'N' ? '#e53935' : '#1e88e5';
        s += rc(mx0, mgY - mgH / 2, 52, mgH, lCol, ST, 1.8, 4) + rc(mx0 + 52, mgY - mgH / 2, 52, mgH, rCol, ST, 1.8, 4);
        s += tx(mx0 + 26, mgY + 6, leftPole, '#ffffff', 'middle', true, 16) + tx(mx0 + 78, mgY + 6, rightPole, '#ffffff', 'middle', true, 16);
      };
      if (mgMode === 'attract') {
        drawBarMag(34, 'S', 'N');
        drawBarMag(222, 'S', 'N');
        s += pfxArrow(144, mgY, 174, mgY, '#1f8a78', 2.6, 8) + pfxArrow(216, mgY, 186, mgY, '#1f8a78', 2.6, 8);
        s += tx(180, mgY - 36, 'جذب (N و S)', '#1f8a78', 'middle', true, 12.5);
      } else if (mgMode === 'repel_s') {
        drawBarMag(34, 'N', 'S');
        drawBarMag(222, 'S', 'N');
        s += pfxArrow(170, mgY, 142, mgY, PT, 2.6, 8) + pfxArrow(190, mgY, 218, mgY, PT, 2.6, 8);
        s += tx(180, mgY - 36, 'دفع (S و S)', PT, 'middle', true, 12.5);
      } else {
        drawBarMag(34, 'S', 'N');
        drawBarMag(222, 'N', 'S');
        s += pfxArrow(170, mgY, 142, mgY, PT, 2.6, 8) + pfxArrow(190, mgY, 218, mgY, PT, 2.6, 8);
        s += tx(180, mgY - 36, 'دفع (N و N)', PT, 'middle', true, 12.5);
      }
      s += tx(180, 236, 'قطب‌های هم‌نام همدیگر را دفع و قطب‌های ناهم‌نام همدیگر را جذب می‌کنند', MU, 'middle', false, 10.5);
      return wrap(s);
    }

    if (t === 'axsenses') {
      /* حواس پنج‌گانه: بدون عبور خط از روی متن + آیکن برداری واضح برای هر اندام */
      var snCx = 180, snCy = 136, snR0 = 32;
      var snNodes = [
        [180, 52, 'بینایی', 'چشم', 180, 18],
        [78, 102, 'شنوایی', 'گوش', 78, 142],
        [282, 102, 'بویایی', 'بینی', 282, 142],
        [106, 200, 'چشایی', 'زبان', 106, 240],
        [254, 200, 'لامسه', 'پوست (دست)', 254, 240]
      ];
      snNodes.forEach(function (nd) {
        var dx = nd[0] - snCx, dy = nd[1] - snCy, dist = Math.hypot(dx, dy) || 1;
        s += lnr(snCx + dx / dist * snR0, snCy + dy / dist * snR0, nd[0] - dx / dist * 21, nd[1] - dy / dist * 21, '#94a3b8', 1.8);
      });
      s += pfxCls(snCx, snCy, snR0, '#eef2ff', ACC, 2.2);
      s += tx(snCx, snCy - 2, 'حواس', ACC, 'middle', true, 12.5) + tx(snCx, snCy + 14, 'پنج‌گانه', ST, 'middle', true, 10.5);
      snNodes.forEach(function (nd, idx) {
        var nx = nd[0], ny = nd[1];
        s += rc(nx - 21, ny - 21, 42, 42, '#ffffff', ACC, 1.9, 14);
        if (idx === 0) {
          s += '<path d="M ' + f2(nx - 13) + ' ' + f2(ny) + ' Q ' + f2(nx) + ' ' + f2(ny - 10) + ' ' + f2(nx + 13) + ' ' + f2(ny) + ' Q ' + f2(nx) + ' ' + f2(ny + 10) + ' ' + f2(nx - 13) + ' ' + f2(ny) + ' Z" fill="#e0f2fe" stroke="' + ST + '" stroke-width="1.4"/>' + pfxCls(nx, ny, 4.2, '#1d4ed8', 'none', 0);
        } else if (idx === 1) {
          s += '<path d="M ' + f2(nx - 4) + ' ' + f2(ny - 10) + ' C ' + f2(nx + 10) + ' ' + f2(ny - 12) + ' ' + f2(nx + 10) + ' ' + f2(ny + 6) + ' ' + f2(nx) + ' ' + f2(ny + 11) + ' C ' + f2(nx - 5) + ' ' + f2(ny + 12) + ' ' + f2(nx - 6) + ' ' + f2(ny + 6) + ' ' + f2(nx - 2) + ' ' + f2(ny + 4) + '" fill="none" stroke="#b45309" stroke-width="2" stroke-linecap="round"/>';
        } else if (idx === 2) {
          s += '<path d="M ' + f2(nx) + ' ' + f2(ny - 10) + ' L ' + f2(nx - 8) + ' ' + f2(ny + 6) + ' Q ' + f2(nx) + ' ' + f2(ny + 11) + ' ' + f2(nx + 8) + ' ' + f2(ny + 6) + ' Z" fill="#fde68a" stroke="#92400e" stroke-width="1.4"/>';
        } else if (idx === 3) {
          s += '<path d="M ' + f2(nx - 11) + ' ' + f2(ny - 3) + ' H ' + f2(nx + 11) + '" stroke="' + ST + '" stroke-width="1.6"/>' +
            '<path d="M ' + f2(nx - 7) + ' ' + f2(ny - 3) + ' V ' + f2(ny + 5) + ' A 7 7 0 0 0 ' + f2(nx + 7) + ' ' + f2(ny + 5) + ' V ' + f2(ny - 3) + ' Z" fill="#fda4af" stroke="#be123c" stroke-width="1.4"/>';
        } else {
          s += rc(nx - 7, ny - 1, 14, 11, '#fed7aa', '#9a3412', 1.3, 4);
          [-5, -2, 1, 4, 7].forEach(function (ox, fi) { s += lnr(nx + ox, ny - 1, nx + ox, ny - (fi === 2 ? 11 : 9), '#9a3412', 2); });
        }
        s += rc(nd[4] - 44, nd[5] - 12, 88, 19, '#f8fafc', '#cbd5e1', 1, 6);
        s += tx(nd[4], nd[5] + 2, nd[2] + ' (' + nd[3] + ')', ST, 'middle', true, 9.8);
      });
      s += tx(180, 268, 'پنج حس بدن انسان و اندام مربوط به هر حس', MU, 'middle', false, 10.5);
      return wrap(s);
    }

    if (t === 'axseedgrow') {
      /* مراحل رشد دانه */
      var sgStages = [['دانه', 0], ['جوانه', 1], ['گیاه کوچک', 2], ['گیاه گلدار', 3]];
      s += lnr(40, 190, 320, 190, '#8d6e63', 3);
      sgStages.forEach(function (st2, si4) {
        var sx4 = 300 - si4 * 76;
        if (st2[1] === 0) {
          s += '<ellipse cx="' + sx4 + '" cy="184" rx="9" ry="6" fill="#a1887f" stroke="' + ST + '" stroke-width="1.2"/>';
        } else {
          var hgt = 14 + st2[1] * 26;
          s += lnr(sx4, 190, sx4, 190 - hgt, '#2e7d32', 2.6);
          for (var lv = 0; lv < st2[1]; lv++) {
            var ly = 190 - 14 - lv * 20;
            s += '<path d="M' + sx4 + ',' + ly + ' q-16,-9 -22,2 q12,7 22,-2" fill="rgba(46,125,50,.35)" stroke="#2e7d32" stroke-width="1.2"/>';
            s += '<path d="M' + sx4 + ',' + ly + ' q16,-9 22,2 q-12,7 -22,-2" fill="rgba(46,125,50,.25)" stroke="#2e7d32" stroke-width="1.2"/>';
          }
          if (st2[1] === 3) s += pfxCls(sx4, 190 - hgt - 10, 7, '#e91e63', '#e91e63', 1.2);
        }
        s += tx(sx4, 216, st2[0], ST, 'middle', true, 10);
      });
      s += tx(180, 40, 'مراحل رشد دانه', ST, 'middle', true, 13);
      s += tx(180, 250, 'از دانه تا گیاه گل‌دار — ترتیب مراحل', MU, 'middle', false, 10);
      return wrap(s);
    }

    if (t === 'axbodyparts') {
      /* اندام‌های بدن (نمای ساده) */
      s += pfxCls(180, 74, 24, '#ffe0b2', ST, 1.8);
      s += '<path d="M180,98 L180,168" stroke="' + ST + '" stroke-width="16" stroke-linecap="round"/>';
      s += '<path d="M146,142 L180,120 L214,142" fill="none" stroke="' + ST + '" stroke-width="8" stroke-linecap="round"/>';
      s += '<path d="M180,168 L158,214 M180,168 L202,214" stroke="' + ST + '" stroke-width="9" stroke-linecap="round"/>';
      s += pfxCls(180, 70, 3, ST, 'none', 0);
      var bpLabs = [['سر', 236, 74], ['دست', 252, 146], ['شکم', 236, 160], ['پا', 250, 214]];
      bpLabs.forEach(function (b2) {
        var lx = b2[1], ly2 = b2[2];
        s += lnr(180, ly2, lx - 10, ly2, '#b9c2d0', 1.1);
        s += pfxCls(lx, ly2, 3, ACC, 'none', 0);
        s += tx(lx + 6, ly2 + 4, b2[0], ST, 'start', true, 11);
      });
      s += tx(180, 40, 'اندام‌های بدن', ST, 'middle', true, 13);
      s += tx(180, 252, 'هر اندام را نام‌گذاری کنید', MU, 'middle', false, 10);
      return wrap(s);
    }

    if (t === 'axwordtrain') {
      /* قطار کلمه‌سازی فارسی (راست به چپ: لوکوموتیو در راست، واگن‌های هجا از راست به چپ) */
      var wtSyl = String(xs('syll', 'مد,ر,سه')).split(/[,،-]/).map(function (q4) { return q4.trim(); }).filter(Boolean).slice(0, 5);
      if (!wtSyl.length) wtSyl = ['مد', 'ر', 'سه'];
      var wtWord = String(xs('word', '')).trim() || wtSyl.join('');
      var wtMode = xs('mode', 'join').toLowerCase();
      var wagN = wtSyl.length, locoW = 48, gap = 8;
      var wagW = Math.min(60, (292 - locoW - wagN * gap) / wagN);
      var totW = locoW + wagN * (wagW + gap), leftX = (360 - totW) / 2, locoX = leftX + totW - locoW, railY = 152;
      s += lnr(18, railY, 342, railY, '#475569', 3);
      for (var sl = 24; sl <= 336; sl += 20) s += lnr(sl, railY, sl - 4, railY + 6, '#64748b', 2);
      /* دودکش و بدنهٔ لوکوموتیو */
      s += pfxCls(locoX + 34, 58, 5, '#cbd5e1', 'none', 0) + pfxCls(locoX + 26, 48, 7, '#e2e8f0', 'none', 0);
      s += rc(locoX + 28, 68, 10, 20, '#334155', ST, 1.5, 2);
      s += rc(locoX, 88, locoW, 50, '#ef5350', ST, 1.8, 6) + rc(locoX + 5, 76, 22, 22, '#fee2e2', ST, 1.5, 4);
      s += pfxPoly(f2(locoX + locoW) + ',124 ' + f2(locoX + locoW + 10) + ',138 ' + f2(locoX + locoW) + ',138', '#475569', ST, 1.2);
      s += pfxCls(locoX + 13, 143, 8.5, '#334155', ST, 1.6) + pfxCls(locoX + 36, 143, 8.5, '#334155', ST, 1.6);
      var wagCols = ['#ede9fe', '#e0f2fe', '#dcfce7', '#fef3c7', '#fce7f3'];
      wtSyl.forEach(function (sy, si) {
        var wx = locoX - (si + 1) * (wagW + gap);
        s += lnr(wx + wagW, 122, wx + wagW + gap, 122, ST, 3.2);
        s += rc(wx, 90, wagW, 48, wagCols[si % wagCols.length], ST, 1.8, 7);
        s += pfxCls(wx + wagW * 0.26, 143, 7.5, '#475569', ST, 1.5) + pfxCls(wx + wagW * 0.74, 143, 7.5, '#475569', ST, 1.5);
        s += tx(wx + wagW / 2, 120, sy, ST, 'middle', true, wagW < 48 ? 14 : 16);
      });
      s += rc(76, 184, 208, 44, wtMode === 'blank' ? '#fff8e1' : 'rgba(39,196,168,.16)', wtMode === 'blank' ? PT : '#1f8a78', 2, 10);
      s += tx(180, 212, wtMode === 'blank' ? 'کلمهٔ ساخته‌شده: ؟' : 'کلمهٔ ساخته‌شده: ' + wtWord, wtMode === 'blank' ? PT : '#0f766e', 'middle', true, 14.5);
      return wrap(s);
    }
    // ============================== V318 — بستهٔ جامع ابزارهای آموزشی (۲) متوسطهٔ اول
    function rcDash(x, y, w, h, fill, st, sw, rx, dash) {
      return '<rect x="' + f2(x) + '" y="' + f2(y) + '" width="' + f2(w) + '" height="' + f2(h) + '"' + (rx ? ' rx="' + f2(rx) + '"' : '') + ' fill="' + fill + '" stroke="' + st + '" stroke-width="' + f2(sw || 1.4) + '" stroke-dasharray="' + (dash || '4 2') + '"/>';
    }
    function icWrap(key, label, defVal, isEmpty, innerSvg) {
      return '<g class="gf-ic" data-cell-key="' + esc(String(key)) + '" data-cell-label="' + esc(String(label)) + '" data-cell-def="' + esc(String(defVal)) + '"' + (isEmpty ? ' data-cell-empty="1"' : '') + ' style="cursor:pointer">' + innerSvg + '</g>';
    }
    if (t === 'axtiles') {
      var tlX2 = clamp(Math.floor(num(X.x2, 1)), 0, 9);
      var tlX = clamp(Math.floor(num(X.x, 2)), 0, 16);
      var tlK = clamp(Math.floor(num(X.k, 3)), 0, 24);
      var tlNeg = xs('neg', '0') === '1';
      var tlShow = xs('show', '1');
      s += rc(16, 16, 328, 28, '#f8fafc', '#cbd5e1', 1.1, 7);
      s += tx(334, 34, 'مدل کاشی:', '#1e293b', 'end', true, 10);
      s += rc(208, 21, 16, 16, tlNeg ? 'rgba(214,51,108,.22)' : 'rgba(108,99,245,.22)', tlNeg ? PT : ACC, 1.4, 3);
      s += tx(202, 34, (tlNeg ? '−' : '+') + 'x² (' + tlX2 + ')', tlNeg ? PT : ACC, 'end', true, 9.5);
      s += rc(126, 24, 22, 10, tlNeg ? 'rgba(249,115,22,.22)' : 'rgba(39,196,168,.24)', tlNeg ? '#ea580c' : '#0f766e', 1.3, 2);
      s += tx(120, 34, (tlNeg ? '−' : '+') + 'x (' + tlX + ')', tlNeg ? '#c2410c' : '#0f766e', 'end', true, 9.5);
      s += rc(58, 24, 10, 10, tlNeg ? 'rgba(225,29,72,.20)' : 'rgba(240,162,2,.28)', tlNeg ? '#be123c' : '#b45309', 1.3, 2);
      s += tx(52, 34, (tlNeg ? '−' : '+') + '1 (' + tlK + ')', tlNeg ? '#be123c' : '#b45309', 'end', true, 9.5);

      var yTop = 52, availH = 166;
      var x2Cols = tlX2 <= 0 ? 0 : (tlX2 <= 2 ? 1 : (tlX2 <= 6 ? 2 : 3));
      var x2Rows = x2Cols === 0 ? 0 : Math.ceil(tlX2 / x2Cols);
      var xCols = tlX <= 0 ? 0 : (tlX <= 5 ? 1 : (tlX <= 12 ? 2 : 3));
      var xRows = xCols === 0 ? 0 : Math.ceil(tlX / xCols);
      var kCols = tlK <= 0 ? 0 : (tlK <= 6 ? 2 : (tlK <= 15 ? 3 : 4));
      var kRows = kCols === 0 ? 0 : Math.ceil(tlK / kCols);

      var aSide = x2Rows <= 2 ? 46 : 38, gapBig = 5;
      var xW = xRows <= 6 ? 38 : 34, xH = xRows <= 5 ? 18 : (xRows <= 7 ? 15 : 13), gapMid = 4;
      var uSide = kRows <= 4 ? 18 : 15, gapUnit = 4;

      var w1 = x2Cols > 0 ? x2Cols * aSide + (x2Cols - 1) * gapBig : 0;
      var w2 = xCols > 0 ? xCols * xW + (xCols - 1) * gapMid : 0;
      var w3 = kCols > 0 ? kCols * uSide + (kCols - 1) * gapUnit : 0;
      var activeGroups = [w1, w2, w3].filter(function (v0) { return v0 > 0; }).length;
      var groupGap = activeGroups >= 3 ? 14 : 18;
      var rawTotalW = w1 + w2 + w3 + Math.max(0, activeGroups - 1) * groupGap;
      var scT = rawTotalW > 328 ? 328 / rawTotalW : 1;
      aSide *= scT; gapBig *= scT; xW *= scT; xH *= scT; gapMid *= scT; uSide *= scT; gapUnit *= scT; groupGap *= scT;
      var fw1 = x2Cols > 0 ? x2Cols * aSide + (x2Cols - 1) * gapBig : 0;
      var fw2 = xCols > 0 ? xCols * xW + (xCols - 1) * gapMid : 0;
      var fw3 = kCols > 0 ? kCols * uSide + (kCols - 1) * gapUnit : 0;
      var curX = (360 - (fw1 + fw2 + fw3 + Math.max(0, activeGroups - 1) * groupGap)) / 2;

      if (tlX2 > 0) {
        var gh1 = x2Rows * aSide + (x2Rows - 1) * gapBig, gy01 = yTop + (availH - gh1) / 2;
        for (var ti2 = 0; ti2 < tlX2; ti2++) {
          var bx1 = curX + (ti2 % x2Cols) * (aSide + gapBig), by1 = gy01 + Math.floor(ti2 / x2Cols) * (aSide + gapBig);
          s += rc(bx1, by1, aSide, aSide, tlNeg ? 'rgba(214,51,108,.18)' : 'rgba(108,99,245,.18)', tlNeg ? PT : ACC, 1.7, 4);
          s += tx(bx1 + aSide / 2, by1 + aSide / 2 + 5, (tlNeg ? '−' : '') + 'x²', tlNeg ? PT : ACC, 'middle', true, aSide >= 40 ? 14 : 11);
        }
        curX += fw1 + groupGap;
      }
      if (tlX > 0) {
        var gh2 = xRows * xH + (xRows - 1) * gapMid, gy02 = yTop + (availH - gh2) / 2;
        for (var tj = 0; tj < tlX; tj++) {
          var bx2 = curX + (tj % xCols) * (xW + gapMid), by2 = gy02 + Math.floor(tj / xCols) * (xH + gapMid);
          s += rc(bx2, by2, xW, xH, tlNeg ? 'rgba(249,115,22,.20)' : 'rgba(39,196,168,.22)', tlNeg ? '#ea580c' : '#0f766e', 1.5, 3);
          s += tx(bx2 + xW / 2, by2 + xH * 0.76, (tlNeg ? '−' : '') + 'x', tlNeg ? '#c2410c' : '#0f766e', 'middle', true, xH >= 15 ? 11 : 9);
        }
        curX += fw2 + groupGap;
      }
      if (tlK > 0) {
        var gh3 = kRows * uSide + (kRows - 1) * gapUnit, gy03 = yTop + (availH - gh3) / 2;
        for (var tk0 = 0; tk0 < tlK; tk0++) {
          var bx3 = curX + (tk0 % kCols) * (uSide + gapUnit), by3 = gy03 + Math.floor(tk0 / kCols) * (uSide + gapUnit);
          s += rc(bx3, by3, uSide, uSide, tlNeg ? 'rgba(225,29,72,.18)' : 'rgba(240,162,2,.25)', tlNeg ? '#be123c' : '#b45309', 1.4, 2.5);
          s += tx(bx3 + uSide / 2, by3 + uSide * 0.76, tlNeg ? '−1' : '1', tlNeg ? '#be123c' : '#92400e', 'middle', true, uSide >= 16 ? 10 : 8.5);
        }
      }
      if (tlX2 === 0 && tlX === 0 && tlK === 0) s += tx(180, 138, '۰ (بدون کاشی)', MU, 'middle', true, 13);

      if (tlShow !== '0') {
        var sg = tlNeg ? '−' : '+', tlTerms = [];
        if (tlX2 > 0) tlTerms.push((tlNeg ? '−' : '') + (tlX2 === 1 ? 'x²' : tlX2 + 'x²'));
        if (tlX > 0) { var t1s = tlX === 1 ? 'x' : tlX + 'x'; tlTerms.push(!tlTerms.length ? (tlNeg ? '−' + t1s : t1s) : sg + ' ' + t1s); }
        if (tlK > 0) { tlTerms.push(!tlTerms.length ? (tlNeg ? '−' + tlK : String(tlK)) : sg + ' ' + tlK); }
        var expr = tlTerms.length ? tlTerms.join(' ') : '0';
        var askExpr = tlShow === '2';
        s += rc(22, 230, 316, 34, askExpr ? '#fffbeb' : '#eef2ff', askExpr ? GLD : ACC, 1.5, 8);
        s += tx(180, 252, askExpr ? 'عبارت جبری:  ؟' : 'عبارت جبری:  ' + expr, askExpr ? '#b45309' : '#1e1b4b', 'middle', true, 13);
      }
      return wrap(s);
    }
    if (t === 'axkenken') {
      var kkKind = xs('kind', 'kenken').toLowerCase();
      var kkN = clamp(Math.floor(num(X.size, 4)), 3, 6);
      var kkEmp = pfxEmptySet(xs('empty', '')), kkOv = pfxCellMap(xs('ov', '')), kkAns = pfxCellMap(xs('ans', ''));
      var hasExplicitEmp = Object.keys(kkEmp).length > 0;
      var kkCs = Math.min(48, 206 / kkN), kkW = kkCs * kkN, kkX0 = (360 - kkW) / 2, kkY0 = 32;

      if (kkKind === 'kakuro') {
        var rawClues = xs('cages', '0,1|-|10;0,2|-|7;0,3|-|8;1,0|9|-;2,0|16|-;1,3|0|0;2,1|4|-');
        var clueM = {};
        rawClues.split(/[;؛]/).forEach(function (part) {
          var ps = part.trim().split('|');
          if (ps.length >= 3) {
            var rcA = ps[0].split(/[,،]/).map(function (z) { return parseInt(faEn(z.trim()), 10); });
            if (rcA.length >= 2 && !isNaN(rcA[0]) && !isNaN(rcA[1])) clueM[rcA[0] + ',' + rcA[1]] = [ps[1].trim(), ps[2].trim()];
          }
        });
        for (var kr = 0; kr < kkN; kr++) for (var kc = 0; kc < kkN; kc++) {
          var kbx = kkX0 + kc * kkCs, kby = kkY0 + kr * kkCs, cKey = kr + ',' + kc;
          var cl = clueM[cKey] || ((kr === 0 && kc === 0) ? ['-', '-'] : (kr === 0 ? ['-', String(kc * 4 + 6)] : (kc === 0 ? [String(kr * 5 + 4), '-'] : null)));
          if (cl) {
            s += rc(kbx, kby, kkCs, kkCs, '#1e293b', '#0f172a', 1.4);
            s += ln(kbx, kby, kbx + kkCs, kby + kkCs, '#64748b', 1.1);
            if (cl[0] !== '-' && cl[0] !== '0' && cl[0] !== '') s += tx(kbx + kkCs * 0.74, kby + kkCs * 0.40, cl[0], '#f8fafc', 'middle', true, 9);
            if (cl[1] !== '-' && cl[1] !== '0' && cl[1] !== '') s += tx(kbx + kkCs * 0.28, kby + kkCs * 0.84, cl[1], '#cbd5e1', 'middle', true, 9);
          } else {
            var cellId = (kr + 1) + ',' + (kc + 1);
            var isEmpK = hasExplicitEmp ? !!kkEmp[cellId] : (kkOv[cellId] == null);
            var innerK = rc(kbx, kby, kkCs, kkCs, '#ffffff', '#64748b', 1.1);
            if (isEmpK) {
              innerK += rcDash(kbx + 4, kby + 4, kkCs - 8, kkCs - 8, '#f8fafc', '#6366f1', 1.1, 4, '3 2');
              var stvK = (kkAns[cellId] || '').trim();
              if (stvK) innerK += tx(kbx + kkCs / 2, kby + kkCs * 0.66, stvK, '#1d4ed8', 'middle', true, 13);
            } else {
              var dvK = (kkOv[cellId] || '').trim();
              if (dvK) innerK += tx(kbx + kkCs / 2, kby + kkCs * 0.66, dvK, ST, 'middle', true, 13);
            }
            s += icWrap(cellId, 'خانهٔ (' + (kr + 1) + '،' + (kc + 1) + ') کاکورو', '', isEmpK, innerK);
          }
        }
        s += rc(kkX0, kkY0, kkW, kkW, 'none', ST, 2.4);
        s += tx(180, kkY0 + kkW + 22, 'پازل کاکورو (جمع اعداد ۱ تا ۹ بدون تکرار در هر رشته)', MU, 'middle', true, 10.5);
        return wrap(s);
      }

      var kkDef = kkN === 3 ? '11,12|5|+;13,23|6|×;21,31|3|−;22,32,33|7|+' : '11,12|6|+;13,14|7|+;21,22|3|×;23,24,31,32|10|+;33,34|4|×;41,42,43,44|10|+';
      var cageMap = {}, cageHeads = {};
      var cageTints = ['rgba(108,99,245,.09)', 'rgba(39,196,168,.10)', 'rgba(240,162,2,.10)', 'rgba(214,51,108,.08)', 'rgba(14,165,233,.10)', 'rgba(168,85,247,.09)'];
      xs('cages', kkDef).split(/[;؛]/).filter(Boolean).forEach(function (cg, idx) {
        var ps = cg.trim().split('|'), cells = [];
        (ps[0] || '').split(/[,،]/).forEach(function (tok) {
          var d2 = faEn(tok.trim());
          if (d2.length >= 2) {
            var r0 = parseInt(d2[0], 10) - 1, c0 = parseInt(d2[1], 10) - 1;
            if (r0 >= 0 && r0 < kkN && c0 >= 0 && c0 < kkN) { cageMap[r0 + '_' + c0] = idx; cells.push([r0, c0]); }
          }
        });
        cells.sort(function (a, b) { return a[0] !== b[0] ? a[0] - b[0] : a[1] - b[1]; });
        if (cells.length) cageHeads[idx] = [cells[0][0], cells[0][1], (ps[1] || '').trim() + (ps[2] || '').trim()];
      });

      for (var r2 = 0; r2 < kkN; r2++) for (var c2 = 0; c2 < kkN; c2++) {
        var bx = kkX0 + c2 * kkCs, by = kkY0 + r2 * kkCs, cid = (r2 + 1) + ',' + (c2 + 1);
        var cgIdx = cageMap[r2 + '_' + c2], fillC = cgIdx != null ? cageTints[cgIdx % cageTints.length] : '#ffffff';
        var isEmp = hasExplicitEmp ? !!kkEmp[cid] : (kkOv[cid] == null);
        var cellInner = rc(bx, by, kkCs, kkCs, fillC, '#cbd5e1', 0.9);
        if (isEmp) {
          cellInner += rcDash(bx + 5, by + 15, kkCs - 10, kkCs - 19, 'rgba(255,255,255,.72)', '#818cf8', 1, 3, '3 2');
          var stv = (kkAns[cid] || '').trim();
          if (stv) cellInner += tx(bx + kkCs / 2, by + kkCs * 0.73, stv, '#1d4ed8', 'middle', true, kkCs >= 40 ? 14 : 11);
        } else {
          var dv = (kkOv[cid] || '').trim();
          if (dv) cellInner += tx(bx + kkCs / 2, by + kkCs * 0.73, dv, ST, 'middle', true, kkCs >= 40 ? 14 : 11);
        }
        s += icWrap(cid, 'خانهٔ سطر ' + (r2 + 1) + ' ستون ' + (c2 + 1), '', isEmp, cellInner);
      }
      for (var r3 = 0; r3 <= kkN; r3++) for (var c3 = 0; c3 < kkN; c3++) {
        var thickH = r3 === 0 || r3 === kkN || cageMap[(r3 - 1) + '_' + c3] !== cageMap[r3 + '_' + c3];
        if (thickH) s += ln(kkX0 + c3 * kkCs, kkY0 + r3 * kkCs, kkX0 + (c3 + 1) * kkCs, kkY0 + r3 * kkCs, ST, (r3 === 0 || r3 === kkN) ? 2.6 : 2.2);
      }
      for (var c4 = 0; c4 <= kkN; c4++) for (var r4 = 0; r4 < kkN; r4++) {
        var thickV = c4 === 0 || c4 === kkN || cageMap[r4 + '_' + (c4 - 1)] !== cageMap[r4 + '_' + c4];
        if (thickV) s += ln(kkX0 + c4 * kkCs, kkY0 + r4 * kkCs, kkX0 + c4 * kkCs, kkY0 + (r4 + 1) * kkCs, ST, (c4 === 0 || c4 === kkN) ? 2.6 : 2.2);
      }
      Object.keys(cageHeads).forEach(function (hk) {
        var hd = cageHeads[hk];
        if (hd[2]) s += tx(kkX0 + hd[1] * kkCs + 4, kkY0 + hd[0] * kkCs + 12, hd[2], '#312e81', 'start', true, kkCs >= 40 ? 10 : 8.5);
      });
      s += tx(180, kkY0 + kkW + 22, 'کن‌کن ' + kkN + '×' + kkN + ' — اعداد ۱ تا ' + kkN + ' بدون تکرار در سطر و ستون', MU, 'middle', true, 10.5);
      return wrap(s);
    }
    if (t === 'axdicefold') {
      var dfDots = xs('mode', 'dots') !== 'num', dfNet = xs('net', 'cross').toLowerCase();
      var dfTop = clamp(Math.floor(num(X.top, 1)), 1, 6), dfAsk = xs('ask', '0') === '1';
      var dfEmp = pfxEmptySet(xs('empty', '')), dfOv = pfxCellMap(xs('ov', '')), dfAns = pfxCellMap(xs('ans', ''));
      var dfOpp = 7 - dfTop, dfFront = (dfTop === 1 || dfTop === 6) ? 2 : 1, dfBack = 7 - dfFront;
      var dfRight = (dfTop !== 3 && dfOpp !== 3 && dfFront !== 3 && dfBack !== 3) ? 3 : 4, dfLeft = 7 - dfRight;
      var dfValues = [dfTop, dfFront, dfOpp, dfBack, dfLeft, dfRight];
      var dfNames = ['وجه بالا', 'وجه جلو', 'وجه پایین (مقابل بالا)', 'وجه عقب (مقابل جلو)', 'وجه چپ', 'وجه راست (مقابل چپ)'];
      var dfCoords = dfNet === 't' ? [[1, 0], [1, 1], [1, 2], [1, 3], [0, 0], [2, 0]]
        : dfNet === 'stairs' ? [[0, 0], [1, 0], [1, 1], [2, 1], [2, 2], [3, 2]]
        : dfNet === 'z' ? [[0, 0], [0, 1], [1, 1], [1, 2], [2, 2], [2, 3]]
        : [[1, 0], [1, 1], [1, 2], [1, 3], [0, 1], [2, 1]];
      var maxCol = Math.max.apply(null, dfCoords.map(function (p) { return p[0]; })) + 1;
      var maxRow = Math.max.apply(null, dfCoords.map(function (p) { return p[1]; })) + 1;
      var dfS = Math.min(48, Math.min(200 / maxCol, 196 / maxRow));
      var dfX0 = 32 + (208 - maxCol * dfS) / 2, dfY0 = 28 + (198 - maxRow * dfS) / 2;
      var pairTint = ['#eef2ff', '#ecfdf5', '#eef2ff', '#ecfdf5', '#fffbeb', '#fffbeb'];
      var pairBrd = [ACC, '#0d9488', ACC, '#0d9488', '#d97706', '#d97706'];
      function drawPips(bx, by, pipN, col) {
        var r = dfS * 0.082, cx = bx + dfS / 2, cy = by + dfS / 2, d2 = dfS * 0.25, o = '';
        var m = { 1: [[0,0]], 2: [[-1,-1],[1,1]], 3: [[-1,-1],[0,0],[1,1]], 4: [[-1,-1],[1,-1],[-1,1],[1,1]], 5: [[-1,-1],[1,-1],[0,0],[-1,1],[1,1]], 6: [[-1,-1],[1,-1],[-1,0],[1,0],[-1,1],[1,1]] }[pipN] || [];
        m.forEach(function (pt) { o += pfxCls(cx + pt[0] * d2, cy + pt[1] * d2, r, col, 'none', 0); });
        return o;
      }
      dfCoords.forEach(function (pt, idx) {
        var bx = dfX0 + pt[0] * dfS, by = dfY0 + pt[1] * dfS, key = 'f' + idx;
        var autoEmp = dfAsk && (idx === 2 || idx === 5), isEmp = !!dfEmp[key] || autoEmp;
        var defV = String(dfValues[idx]);
        var gIn = rc(bx, by, dfS, dfS, isEmp ? '#ffffff' : pairTint[idx], isEmp ? '#4f46e5' : pairBrd[idx], isEmp ? 2 : 1.7, 5);
        if (isEmp) {
          gIn += rcDash(bx + 5, by + 5, dfS - 10, dfS - 10, '#f8fafc', '#6366f1', 1.3, 4, '4 2.5');
          var stv = (dfAns[key] || '').trim(), stNum = parseInt(faEn(stv), 10);
          if (stv) {
            if (dfDots && stNum >= 1 && stNum <= 6) gIn += drawPips(bx, by, stNum, '#1d4ed8');
            else gIn += tx(bx + dfS / 2, by + dfS * 0.64, stv, '#1d4ed8', 'middle', true, 16);
          }
        } else {
          var disp = (dfOv[key] != null ? dfOv[key] : defV).trim(), dNum = parseInt(faEn(disp), 10);
          if (dfDots && dNum >= 1 && dNum <= 6) gIn += drawPips(bx, by, dNum, ST);
          else gIn += tx(bx + dfS / 2, by + dfS * 0.64, disp, ST, 'middle', true, 16);
        }
        s += icWrap(key, dfNames[idx], defV, isEmp, gIn);
      });
      var ix = 294, iy = 112, ir = 34, c30 = ir * 0.866, s30 = ir * 0.5;
      s += pfxPoly(f2(ix) + ',' + f2(iy - ir) + ' ' + f2(ix + c30) + ',' + f2(iy - s30) + ' ' + f2(ix) + ',' + f2(iy) + ' ' + f2(ix - c30) + ',' + f2(iy - s30), '#e0e7ff', ACC, 1.5);
      s += pfxPoly(f2(ix - c30) + ',' + f2(iy - s30) + ' ' + f2(ix) + ',' + f2(iy) + ' ' + f2(ix) + ',' + f2(iy + ir) + ' ' + f2(ix - c30) + ',' + f2(iy + s30), '#ccfbf1', '#0d9488', 1.5);
      s += pfxPoly(f2(ix) + ',' + f2(iy) + ' ' + f2(ix + c30) + ',' + f2(iy - s30) + ' ' + f2(ix + c30) + ',' + f2(iy + s30) + ' ' + f2(ix) + ',' + f2(iy + ir), '#fef3c7', '#d97706', 1.5);
      s += tx(ix, iy - s30 + 3, String(dfTop), '#312e81', 'middle', true, 11);
      s += tx(ix - c30 * 0.5, iy + s30 * 0.6, String(dfFront), '#0f766e', 'middle', true, 11);
      s += tx(ix + c30 * 0.5, iy + s30 * 0.6, String(dfRight), '#92400e', 'middle', true, 11);
      s += tx(ix, iy + ir + 18, 'تاس سه‌بعدی', MU, 'middle', true, 9.5);
      s += rc(20, 236, 320, 28, '#f8fafc', '#cbd5e1', 1.1, 7);
      s += tx(180, 254, 'مجموع هر دو وجه مقابل در تاس استاندارد برابر ۷ است', '#1e293b', 'middle', true, 10.5);
      return wrap(s);
    }
    if (t === 'axtransform') {
      var tfOp = xs('op', 'translate').toLowerCase(), tfShape = xs('shape', 'tri').toLowerCase(), tfGrid = clamp(Math.floor(num(X.grid, 8)), 4, 10);
      // V322 — جابه‌جایی بزرگ شکل را بیرون از بوم می‌برد؛ به اندازهٔ شبکه مهار می‌شود.
      var tfDx = clamp(num(X.dx, 3), -4, 4), tfDy = clamp(num(X.dy, 2), -4, 4), tfAng = num(X.ang, 90), tfAxis = xs('axis', 'y').toLowerCase();
      var tfU = Math.min(26, 240 / tfGrid), tfOx = 180 - tfU * tfGrid / 2, tfOy = 26 + (220 - tfU * tfGrid) / 2 + tfU * tfGrid;
      for (var gi = 0; gi <= tfGrid; gi++) {
        s += lnr(tfOx + gi * tfU, tfOy - tfGrid * tfU, tfOx + gi * tfU, tfOy, '#e3e9f2', 1) + lnr(tfOx, tfOy - gi * tfU, tfOx + tfGrid * tfU, tfOy - gi * tfU, '#e3e9f2', 1);
      }
      s += lnr(tfOx, tfOy, tfOx + tfGrid * tfU, tfOy, ST, 1.8) + lnr(tfOx, tfOy, tfOx, tfOy - tfGrid * tfU, ST, 1.8);
      var tfBase = tfShape === 'sq' ? [[2, 2], [4, 2], [4, 4], [2, 4]] : tfShape === 'rect' ? [[2, 2], [5, 2], [5, 4], [2, 4]] : [[2, 2], [5, 2], [3, 4]];
      var tfPt = function (p2) { return [tfOx + p2[0] * tfU, tfOy - p2[1] * tfU]; };
      var tfSvg = function (pts2, fill2, col2, dash2) {
        var str2 = pts2.map(function (p2) { var q5 = tfPt(p2); return f2(q5[0]) + ',' + f2(q5[1]); }).join(' ');
        return pfxPoly(str2, fill2, col2, 2) + (dash2 ? '' : '');
      };
      var tfMap = tfBase.map(function (p2) {
        if (tfOp === 'rotate') {
          var rad2 = tfAng * Math.PI / 180, cx4 = 3, cy4 = 3, ddx = p2[0] - cx4, ddy = p2[1] - cy4;
          return [cx4 + ddx * Math.cos(rad2) - ddy * Math.sin(rad2), cy4 + ddx * Math.sin(rad2) + ddy * Math.cos(rad2)];
        }
        if (tfOp === 'reflect') return tfAxis === 'x' ? [p2[0], 2 * 3 - p2[1]] : [2 * 3 - p2[0], p2[1]];
        return [p2[0] + tfDx, p2[1] + tfDy];
      });
      s += tfSvg(tfBase, 'rgba(108,99,245,.12)', ACC, true) + tfSvg(tfMap, 'rgba(39,196,168,.22)', GRN, false);
      if (tfOp === 'reflect') s += lnr(tfOx, tfOy - 3 * tfU, tfOx + tfGrid * tfU, tfOy - 3 * tfU, PT, 1.6) + (tfAxis === 'x' ? '' : '');
      if (tfOp === 'reflect' && tfAxis !== 'x') s += lnr(tfOx + 3 * tfU, tfOy, tfOx + 3 * tfU, tfOy - tfGrid * tfU, PT, 1.6);
      if (tfOp === 'translate') s += pfxArrow(tfPt(tfBase[0])[0], tfPt(tfBase[0])[1], tfPt(tfMap[0])[0], tfPt(tfMap[0])[1], PT, 1.6, 8);
      var tfName = tfOp === 'rotate' ? ('دوران ' + nm(tfAng) + '° حول (۳,۳)') : tfOp === 'reflect' ? ('بازتاب نسبت به محور ' + (tfAxis === 'x' ? 'x' : 'y')) : ('انتقال با بردار (' + nm(tfDx) + ', ' + nm(tfDy) + ')');
      s += tx(180, 258, tfName, MU, 'middle', true, 12);
      return wrap(s);
    }
    if (t === 'axcaliper') {
      var cpMicro = xs('kind', 'vernier').toLowerCase() === 'micrometer';
      var cpLbl = xs('label', '1');
      if (!cpMicro) {
        var cpMax = clamp(num(X.max, 50), 30, 80);
        // V322 — ورنیه فراتر از انتهای خط‌کش نرود
        var cpVal = Math.min(cpMax, Math.max(0, num(X.val, 23.4)));
        var cpWhole = Math.floor(cpVal + 1e-4), cpTenth = Math.round((cpVal - cpWhole) * 10) % 10;
        var rx0 = 56, rw = 268, pxPerMm = rw / (cpMax + 10);
        s += pfxPoly(f2(rx0 - 16) + ',40 ' + f2(rx0) + ',40 ' + f2(rx0) + ',182 ' + f2(rx0 - 8) + ',182 ' + f2(rx0 - 16) + ',154', '#cbd5e1', ST, 1.7);
        s += rc(rx0, 62, rw, 44, '#f8fafc', ST, 1.8, 2);
        for (var mm = 0; mm <= Math.floor(cpMax) + 9; mm++) {
          var mx = rx0 + mm * pxPerMm; if (mx > rx0 + rw - 2) break;
          var th = mm % 10 === 0 ? 18 : (mm % 5 === 0 ? 12 : 7);
          s += ln(mx, 106 - th, mx, 106, ST, mm % 10 === 0 ? 1.5 : 0.9);
          if (mm % 10 === 0) s += tx(mx, 80, String(mm / 10), ST, 'middle', true, 10);
        }
        s += tx(rx0 + rw - 6, 74, 'cm', MU, 'end', true, 9);
        var vx0 = rx0 + cpVal * pxPerMm;
        if (cpVal > 1.5) s += rc(rx0 + 0.8, 140, Math.max(3, vx0 - rx0 - 1.6), 24, 'rgba(39,196,168,.24)', '#0d9488', 1.4, 3);
        var vw = Math.max(46, 9 * pxPerMm + 16);
        s += pfxPoly(f2(vx0) + ',106 ' + f2(vx0 + 16) + ',106 ' + f2(vx0 + 16) + ',182 ' + f2(vx0 + 7) + ',182 ' + f2(vx0) + ',156', '#e0e7ff', ACC, 1.7);
        s += rc(vx0, 106, Math.min(vw, 352 - vx0), 34, '#eef2ff', ACC, 1.8, 2);
        var vStep = 0.9 * pxPerMm;
        for (var vi = 0; vi <= 10; vi++) {
          var vtx = vx0 + 4 + vi * vStep;
          if (vtx <= 350) {
            var isMatch = vi === cpTenth;
            s += ln(vtx, 106, vtx, 106 + (isMatch ? 16 : (vi % 5 === 0 ? 13 : 9)), isMatch ? PT : ACC, isMatch ? 2.1 : 1.1);
            if (isMatch) s += pfxCls(vtx, 126, 2.6, PT, 'none', 0);
            if (vi === 0 || vi === 5 || vi === 10) s += tx(vtx, 136, String(vi), ACC, 'middle', true, 8.5);
          }
        }
        if (cpLbl !== '0') {
          var ask = cpLbl === '2';
          s += rc(20, 224, 320, 36, ask ? '#fffbeb' : '#f8fafc', ask ? GLD : '#cbd5e1', 1.4, 8);
          var msg = ask ? 'عدد کولیس (دقت 0.1 mm):  ؟ mm' : ('کولیس: ' + cpWhole + ' + 0.' + cpTenth + ' = ' + cpVal.toFixed(1) + ' mm');
          s += tx(180, 247, msg, ask ? '#92400e' : '#1e293b', 'middle', true, 12);
        }
      } else {
        var mVal = clamp(num(X.val, 7.38), 0, 25);
        var sleeveHalf = Math.floor(mVal * 2 + 1e-4) * 0.5;
        var thimbleDiv = Math.round((mVal - sleeveHalf) * 100) % 50;
        s += pfxPath('M 36 74 L 62 74 L 62 166 L 36 166 Q 16 120 36 74 Z', '#cbd5e1', ST, 1.8);
        s += rc(62, 104, 10, 32, '#94a3b8', ST, 1.4);
        var slX0 = 118, mmPx = 7.5, spindleEnd = slX0 - Math.max(8, (25 - mVal) * 1.6);
        s += rc(72, 108, Math.max(6, spindleEnd - 72), 24, 'rgba(39,196,168,.24)', '#0d9488', 1.4, 2);
        s += rc(spindleEnd, 111, slX0 - spindleEnd, 18, '#e2e8f0', ST, 1.4);
        var visW = Math.max(28, (sleeveHalf + 1.2) * mmPx);
        s += rc(slX0, 88, visW, 64, '#f8fafc', ST, 1.8, 2);
        s += ln(slX0, 120, slX0 + visW, 120, ST, 1.6);
        var maxWhole = Math.floor(sleeveHalf);
        for (var m = 0; m <= maxWhole; m++) {
          var txM = slX0 + 8 + m * mmPx;
          if (txM < slX0 + visW - 2) {
            s += ln(txM, 106, txM, 120, ST, m % 5 === 0 ? 1.6 : 1.1);
            if (m % 5 === 0) s += tx(txM, 102, String(m), ST, 'middle', true, 9);
          }
          if (m + 0.5 <= sleeveHalf + 1e-4) {
            var txH = txM + 0.5 * mmPx;
            if (txH < slX0 + visW - 1) s += ln(txH, 120, txH, 131, '#0284c7', 1.2);
          }
        }
        var thX = slX0 + visW;
        s += pfxPoly(f2(thX) + ',88 ' + f2(thX + 24) + ',76 ' + f2(thX + 24) + ',164 ' + f2(thX) + ',152', '#e0e7ff', ACC, 1.8);
        s += rc(thX + 24, 76, 76, 88, '#eef2ff', ACC, 1.8, 3);
        for (var d0 = -4; d0 <= 4; d0++) {
          var mark = (thimbleDiv + d0 + 50) % 50, my = 120 - d0 * 8.5, isRef = d0 === 0;
          s += ln(thX + 2, my, thX + (isRef ? 18 : (mark % 5 === 0 ? 15 : 10)), my, isRef ? PT : ACC, isRef ? 2 : 1.1);
          if (mark % 5 === 0 || isRef) s += tx(thX + 34, my + 3.5, String(mark), isRef ? PT : '#1e1b4b', 'start', isRef, isRef ? 10 : 8.5);
        }
        if (cpLbl !== '0') {
          var askM = cpLbl === '2';
          s += rc(20, 224, 320, 36, askM ? '#fffbeb' : '#f8fafc', askM ? GLD : '#cbd5e1', 1.4, 8);
          var msgM = askM ? 'عدد ریزسنج (دقت 0.01 mm):  ؟ mm' : ('ریزسنج: ' + sleeveHalf.toFixed(1) + ' + ' + (thimbleDiv / 100).toFixed(2) + ' = ' + mVal.toFixed(2) + ' mm');
          s += tx(180, 247, msgM, askM ? '#92400e' : '#1e293b', 'middle', true, 12);
        }
      }
      return wrap(s);
    }
    if (t === 'axsigntable') {
      var sgRoots = ilist('roots', '-2,1,3').slice(0, 4);
      if (!sgRoots.length) sgRoots = [-2, 1, 3];
      sgRoots.sort(function (a2, b2) { return a2 - b2; });
      var sgMult = ilist('mult', '').slice(0, sgRoots.length);
      while (sgMult.length < sgRoots.length) sgMult.push(1);
      var sgLead = num(X.lead, 1) >= 0 ? 1 : -1, sgCols = sgRoots.length + 1;
      var sgW = Math.min(300, 62 * sgCols + 20 * sgRoots.length), sgCw = (sgW - 26) / sgCols, sgX0 = 180 - sgW / 2 + 13, sgY0 = 34, sgRh = Math.min(30, 150 / (sgRoots.length + 2));
      var sgSgn = function (v) { return v >= 0 ? '+' : '−'; };
      var sgSample = [];
      for (var sci = 0; sci < sgCols; sci++) {
        if (sci === 0) sgSample.push(sgRoots[0] - 1);
        else if (sci === sgCols - 1) sgSample.push(sgRoots[sgRoots.length - 1] + 1);
        else sgSample.push((sgRoots[sci - 1] + sgRoots[sci]) / 2);
      }
      s += rc(sgX0, sgY0, sgCw * sgCols, sgRh, 'rgba(108,99,245,.16)', ST, 1.4) + tx(sgX0 - 6, sgY0 + sgRh * 0.68, 'x', ST, 'end', true, 12);
      for (var scj = 0; scj < sgCols; scj++) {
        var lo2 = scj === 0 ? '−∞' : nm(sgRoots[scj - 1]), hi2 = scj === sgCols - 1 ? '+∞' : nm(sgRoots[scj]);
        s += tx(sgX0 + scj * sgCw + sgCw / 2, sgY0 + sgRh * 0.68, '(' + lo2 + ', ' + hi2 + ')', MU, 'middle', true, 10);
        if (scj < sgCols - 1) s += lnr(sgX0 + (scj + 1) * sgCw, sgY0, sgX0 + (scj + 1) * sgCw, sgY0 + sgRh, ST, 1.2);
      }
      for (var sri = 0; sri < sgRoots.length; sri++) {
        var ry3 = sgY0 + sgRh * (sri + 1), rt2 = sgRoots[sri], rm = Math.abs(sgMult[sri]) % 2 === 0 ? 2 : 1;
        s += lnr(sgX0, ry3, sgX0 + sgCw * sgCols, ry3, ST, 1.2) + tx(sgX0 - 6, ry3 + sgRh * 0.68, '(x ' + (rt2 < 0 ? '+ ' + nm(-rt2) : '− ' + nm(rt2)) + ')' + (rm === 2 ? '²' : ''), ST, 'end', true, 10);
        for (var sc2 = 0; sc2 < sgCols; sc2++) {
          var sgv = rm === 2 ? 1 : (sgSample[sc2] > rt2 ? 1 : -1);
          s += tx(sgX0 + sc2 * sgCw + sgCw / 2, ry3 + sgRh * 0.68, sgSgn(sgv), sgv > 0 ? '#1b7f4c' : PT, 'middle', true, 14);
          if (sc2 < sgCols - 1) s += lnr(sgX0 + (sc2 + 1) * sgCw, ry3, sgX0 + (sc2 + 1) * sgCw, ry3 + sgRh, '#c8d2e0', 1);
        }
        s += tx(sgX0 + sgCw * sgCols + 4, ry3 + sgRh * 0.68, '۰', PT, 'start', true, 12);
      }
      var fy2 = sgY0 + sgRh * (sgRoots.length + 1);
      s += lnr(sgX0, fy2, sgX0 + sgCw * sgCols, fy2, ST, 1.6) + tx(sgX0 - 6, fy2 + sgRh * 0.68, 'f(x)', ACC, 'end', true, 11);
      for (var sc3 = 0; sc3 < sgCols; sc3++) {
        var fs = sgLead;
        for (var sfi = 0; sfi < sgRoots.length; sfi++) fs *= (Math.abs(sgMult[sfi]) % 2 === 0 ? 1 : (sgSample[sc3] > sgRoots[sfi] ? 1 : -1));
        s += tx(sgX0 + sc3 * sgCw + sgCw / 2, fy2 + sgRh * 0.68, sgSgn(fs), fs > 0 ? '#1b7f4c' : PT, 'middle', true, 15);
        if (sc3 < sgCols - 1) s += lnr(sgX0 + (sc3 + 1) * sgCw, fy2, sgX0 + (sc3 + 1) * sgCw, fy2 + sgRh, ST, 1.2);
      }
      s += rc(sgX0, fy2, sgCw * sgCols, sgRh, 'rgba(39,196,168,.12)', ST, 1.6);
      s += tx(180, fy2 + sgRh + 28, 'جدول تعیین علامت — ریشه‌ها: ' + sgRoots.map(function (v2) { return nm(v2); }).join(' , '), MU, 'middle', true, 11);
      return wrap(s);
    }
    if (t === 'axtrigcircle') {
      var rawDeg = num(X.deg, 55), showMode = xs('show', '1').toLowerCase();
      var cx = 176, cy = 128, r = 82;
      s += ln(cx - r - 22, cy, cx + r + 24, cy, ST, 1.5) + arR(cx + r + 24, cy);
      s += ln(cx, cy + r + 18, cx, cy - r - 20, ST, 1.5) + arU(cx, cy - r - 20);
      s += tx(cx + r + 18, cy + 14, '1', MU, 'middle', false, 9.5);
      s += tx(cx - r - 15, cy + 14, '−1', MU, 'middle', false, 9.5);
      s += tx(cx + 12, cy - r - 8, '1', MU, 'start', false, 9.5);
      s += pfxCls(cx, cy, r, 'rgba(108,99,245,.05)', ST, 1.8);

      var normDeg = ((rawDeg % 360) + 360) % 360;
      var rad = rawDeg * Math.PI / 180, cVal = Math.cos(rad), sVal = Math.sin(rad);
      var px = cx + r * cVal, py = cy - r * sVal;

      if (showMode !== '0') {
        if (Math.abs(sVal) > 0.03) s += ln(px, cy, px, py, PT, 2, '4 3');
        if (Math.abs(cVal) > 0.03) s += ln(cx, cy, px, cy, '#0d9488', 2.6);
        if (showMode === 'tan' && Math.abs(cVal) > 0.12) {
          var tanVal = sVal / cVal, ty = clamp(cy - r * tanVal, 24, 224), txTan = cx + r;
          s += ln(txTan, 26, txTan, 224, '#94a3b8', 1.1, '3 3');
          s += ln(cx, cy, txTan, ty, '#f59e0b', 1.4, '4 3');
          s += ln(txTan, cy, txTan, ty, '#d97706', 2.6);
        }
      }

      var rArc = 25, absSpan = Math.min(Math.abs(rawDeg), 360);
      if (absSpan >= 359.5) {
        s += pfxCls(cx, cy, rArc, 'rgba(240,162,2,.16)', '#d97706', 2);
      } else if (absSpan >= 1) {
        var sweep = rawDeg >= 0 ? 0 : 1, largeArc = absSpan > 180 ? 1 : 0;
        var endRad = (rawDeg >= 0 ? absSpan : -absSpan) * Math.PI / 180;
        var ax2 = cx + rArc * Math.cos(endRad), ay2 = cy - rArc * Math.sin(endRad);
        s += pfxPath('M ' + f2(cx + rArc) + ' ' + f2(cy) + ' A ' + f2(rArc) + ' ' + f2(rArc) + ' 0 ' + largeArc + ' ' + sweep + ' ' + f2(ax2) + ' ' + f2(ay2), 'none', '#d97706', 2.2);
      }
      var midRad = ((Math.abs(rawDeg) > 360 ? normDeg : rawDeg) / 2) * Math.PI / 180;
      s += tx(cx + (rArc + 15) * Math.cos(midRad), cy - (rArc + 15) * Math.sin(midRad) + 4, nm(rawDeg) + '°', '#b45309', 'middle', true, 10);

      s += ln(cx, cy, px, py, ACC, 2.4);
      s += pfxCls(px, py, 4.5, PT, '#ffffff', 1.5);

      var exactTrig = function (nDeg, isSin) {
        var d0 = Math.round(((nDeg % 360) + 360) % 360);
        var sM = {0:'0',30:'1/2',45:'√2/2',60:'√3/2',90:'1',120:'√3/2',135:'√2/2',150:'1/2',180:'0',210:'−1/2',225:'−√2/2',240:'−√3/2',270:'−1',300:'−√3/2',315:'−√2/2',330:'−1/2',360:'0'};
        var cM = {0:'1',30:'√3/2',45:'√2/2',60:'1/2',90:'0',120:'−1/2',135:'−√2/2',150:'−√3/2',180:'−1',210:'−√3/2',225:'−√2/2',240:'−1/2',270:'0',300:'1/2',315:'√2/2',330:'√3/2',360:'1'};
        if (Math.abs(nDeg - d0) < 0.05 && sM[d0]) return isSin ? sM[d0] : cM[d0];
        return (isSin ? Math.sin(nDeg * Math.PI / 180) : Math.cos(nDeg * Math.PI / 180)).toFixed(2);
      };
      if (showMode !== '0') {
        var sinS = exactTrig(normDeg, true), cosS = exactTrig(normDeg, false);
        var qNum = normDeg <= 90 ? 'اول' : (normDeg <= 180 ? 'دوم' : (normDeg <= 270 ? 'سوم' : 'چهارم'));
        s += rc(16, 240, 328, 28, '#f8fafc', '#cbd5e1', 1.2, 7);
        s += tx(180, 258, 'θ = ' + nm(rawDeg) + '° (ربع ' + qNum + ')   |   cos = ' + cosS + '   ،   sin = ' + sinS, '#1e293b', 'middle', true, 10.5);
      } else {
        s += tx(180, 258, 'θ = ' + nm(rawDeg) + '°', ACC, 'middle', true, 12);
      }
      return wrap(s);
    }
    if (t === 'axmapping') {
      var mpD = xs('from', '1,2,3').split(/[,،;]/).map(function (z) { return z.trim(); }).filter(Boolean).slice(0, 6);
      var mpC = xs('to', 'a,b,c').split(/[,،;]/).map(function (z) { return z.trim(); }).filter(Boolean).slice(0, 6);
      if (!mpD.length) mpD = ['1', '2', '3'];
      if (!mpC.length) mpC = ['a', 'b', 'c'];
      var mpShow = xs('show', '1');
      var mpPairs = xs('arrows', '1-a;2-a;3-b').split(/[;؛,،]/).map(function (pr) {
        var sp = pr.trim().split(/[-→:>]/); return sp.length >= 2 ? [sp[0].trim(), sp[1].trim()] : null;
      }).filter(Boolean);

      var lx = 92, rx = 268, cy = 126, ry = 84;
      s += '<ellipse cx="' + lx + '" cy="' + cy + '" rx="44" ry="' + ry + '" fill="rgba(108,99,245,.10)" stroke="' + ACC + '" stroke-width="2"/>';
      s += '<ellipse cx="' + rx + '" cy="' + cy + '" rx="44" ry="' + ry + '" fill="rgba(39,196,168,.12)" stroke="#0d9488" stroke-width="2"/>';
      s += tx(lx, 30, 'دامنه (A)', ACC, 'middle', true, 11);
      s += tx(rx, 30, 'هم‌دامنه (B)', '#0f766e', 'middle', true, 11);

      var yOf = function (i, total) { return total === 1 ? cy : (cy - ry + 26 + i * ((2 * ry - 52) / (total - 1))); };
      var outDeg = {}, inDeg = {};
      mpD.forEach(function (d0) { outDeg[d0] = 0; });
      mpC.forEach(function (c0) { inDeg[c0] = 0; });

      mpPairs.forEach(function (pr) {
        var di = mpD.indexOf(pr[0]), ci = mpC.indexOf(pr[1]);
        if (di >= 0 && ci >= 0) {
          outDeg[pr[0]] = (outDeg[pr[0]] || 0) + 1;
          inDeg[pr[1]] = (inDeg[pr[1]] || 0) + 1;
          var y1 = yOf(di, mpD.length), y2 = yOf(ci, mpC.length);
          var sx0 = lx + 24, sy0 = y1, ex0 = rx - 24, ey0 = y2, midY = (sy0 + ey0) / 2 - 8;
          s += pfxPath('M ' + f2(sx0) + ' ' + f2(sy0) + ' Q 180 ' + f2(midY) + ' ' + f2(ex0) + ' ' + f2(ey0), 'none', PT, 1.8);
          var ang = Math.atan2(ey0 - midY, ex0 - 180);
          s += pfxPoly(f2(ex0) + ',' + f2(ey0) + ' ' + f2(ex0 - 8 * Math.cos(ang - 0.42)) + ',' + f2(ey0 - 8 * Math.sin(ang - 0.42)) + ' ' + f2(ex0 - 8 * Math.cos(ang + 0.42)) + ',' + f2(ey0 - 8 * Math.sin(ang + 0.42)), PT);
        }
      });
      mpD.forEach(function (v, idx) {
        var yy = yOf(idx, mpD.length);
        s += rc(lx - 22, yy - 10, 40, 20, '#ffffff', ACC, 1.2, 6) + tx(lx - 2, yy + 4, v, ST, 'middle', true, 11) + pfxCls(lx + 22, yy, 3.2, ACC, 'none', 0);
      });
      mpC.forEach(function (v, idx) {
        var yy = yOf(idx, mpC.length), hit = (inDeg[v] || 0) > 0;
        s += rc(rx - 18, yy - 10, 40, 20, hit ? '#ecfdf5' : '#ffffff', '#0d9488', 1.2, 6) + tx(rx + 2, yy + 4, v, ST, 'middle', true, 11) + pfxCls(rx - 22, yy, 3.2, hit ? PT : '#94a3b8', 'none', 0);
      });

      if (mpShow !== '0') {
        var isFunc = mpD.every(function (k) { return outDeg[k] === 1; });
        var isInj = isFunc && mpC.every(function (k) { return (inDeg[k] || 0) <= 1; });
        var isSurj = isFunc && mpC.every(function (k) { return (inDeg[k] || 0) >= 1; });
        var verdict = mpShow === '2' ? 'آیا این رابطه تابع است؟ نوع آن: ؟'
          : !isFunc ? 'رابطه تابع نیست (خروجی دامنه دقیقاً یکی نیست)'
          : (isInj && isSurj) ? 'تابع دوسویه (یک‌به‌یک و پوشا)'
          : isInj ? 'تابع یک‌به‌یک (تزریقی)' : isSurj ? 'تابع پوشا' : 'تابع است (غیر یک‌به‌یک و غیر پوشا)';
        s += rc(18, 234, 324, 30, isFunc ? '#eef2ff' : '#fff1f2', isFunc ? ACC : PT, 1.4, 8);
        s += tx(180, 254, verdict, isFunc ? '#1e1b4b' : PT, 'middle', true, 10.5);
      }
      return wrap(s);
    }
    if (t === 'axprobtree') {
      var parseBr = function (raw, fb) {
        var src2 = String(raw || '').trim() || fb;
        return src2.split(/[,،;]/).map(function (tk) {
          var t0 = tk.trim(); if (!t0) return null;
          var ix = t0.indexOf(':') > 0 ? t0.indexOf(':') : t0.indexOf('=');
          if (ix > 0) { var nm2 = t0.slice(0, ix).trim(), wv = parseFloat(faEn(t0.slice(ix + 1).trim())); return [nm2 || '؟', (isFinite(wv) && wv > 0) ? wv : 1]; }
          return [t0, 1];
        }).filter(Boolean);
      };
      var s1 = parseBr(xs('first', 'شیر,خط'), 'شیر,خط').slice(0, 4);
      var s2 = parseBr(xs('second', 'شیر,خط'), '').slice(0, 3);
      var s3 = parseBr(xs('third', ''), '').slice(0, 2);
      var s4 = parseBr(xs('fourth', ''), '').slice(0, 2);
      var hasExplicitSt = xs('stages', '').trim() !== '';
      var reqSt = clamp(Math.floor(num(X.stages, 2)), 1, 4);
      var stageLists = [s1];
      if ((hasExplicitSt ? reqSt >= 2 : s2.length > 0)) stageLists.push((s2.length ? s2 : [['الف', 1], ['ب', 1]]).slice(0, reqSt >= 3 ? 2 : 3));
      if ((hasExplicitSt ? reqSt >= 3 : s3.length > 0)) stageLists.push((s3.length ? s3 : [['۱', 1], ['۲', 1]]).slice(0, 2));
      if ((hasExplicitSt ? reqSt >= 4 : s4.length > 0)) stageLists.push((s4.length ? s4 : [['الف', 1], ['ب', 1]]).slice(0, 2));
      while (stageLists.length > 1) {
        var prod = stageLists.reduce(function (a, b) { return a * b.length; }, 1);
        if (prod <= 16) break;
        stageLists.pop();
      }
      var nSt = stageLists.length;
      var W = nSt >= 4 ? 520 : (nSt === 3 ? 440 : 360);
      var H = nSt >= 4 ? 440 : (nSt === 3 ? 340 : 280);
      if (title) s = tx(W / 2, 14, title, '#1a2433', 'middle', true, 13, W);

      var pbMode = xs('mode', 'prob').toLowerCase();
      var pbEmp = pfxEmptySet(xs('empty', '')), pbOv = pfxCellMap(xs('ov', '')), pbAns = pfxCellMap(xs('ans', ''));
      var xRoot = 24, xOutEnd = W - 12, xLeafEnd = W - (nSt >= 3 ? 98 : 88);
      var stageX = function (stIdx) { return xRoot + stIdx * ((xLeafEnd - xRoot) / nSt); };
      var yTop = 36, yBot = H - 24;
      for (var st0 = 1; st0 <= nSt; st0++) s += tx((stageX(st0 - 1) + stageX(st0)) / 2, 20, 'مرحلهٔ ' + st0, MU, 'middle', true, 9.5, W);

      var gcd = function (a, b) { a = Math.abs(a); b = Math.abs(b); while (b) { var t2 = a % b; a = b; b = t2; } return a || 1; };
      var totalLeaves = stageLists.reduce(function (a, b) { return a * b.length; }, 1);
      var leafCounter = 0;

      function buildStage(stIdx, pathIdxs, probNum, probDen, pathNames) {
        if (stIdx === nSt) {
          var ly = totalLeaves === 1 ? (yTop + yBot) / 2 : (yTop + (leafCounter + 0.5) * ((yBot - yTop) / totalLeaves));
          leafCounter++;
          var outKey = 'out_' + pathIdxs.join('_'), g2 = gcd(probNum, probDen);
          var simpFrac = (probNum / g2) + '/' + (probDen / g2), pct = Math.round(probNum * 100 / probDen) + '%';
          var defOut = pbMode === 'count' ? pathNames.join('،') : (pbMode === 'pct' ? pct : simpFrac);
          var autoE = pbMode === 'blank', isE = !!pbEmp[outKey] || autoE;
          var bx = xLeafEnd + 8, bw = Math.max(46, xOutEnd - bx), bh = totalLeaves >= 12 ? 20 : (totalLeaves >= 8 ? 22 : 25), by = ly - bh / 2;
          var leafIn = '';
          if (isE) {
            leafIn += rcDash(bx, by, bw, bh, '#ffffff', '#4f46e5', 1.4, 5, '4 2');
            var stv = (pbAns[outKey] || '').trim();
            if (stv) leafIn += tx(bx + bw / 2, ly + 4, stv, '#1d4ed8', 'middle', true, bh <= 20 ? 9 : 10, W);
          } else {
            var disp = (pbOv[outKey] != null ? pbOv[outKey] : defOut).trim();
            leafIn += rc(bx, by, bw, bh, '#ecfdf5', '#10b981', 1.2, 5);
            leafIn += tx(bx + bw / 2, ly + 4, disp, '#065f46', 'middle', true, bh <= 20 ? 9 : 10, W);
          }
          s += ln(xLeafEnd, ly, bx - 2, ly, '#94a3b8', 1.1, '2 2');
          s += icWrap(outKey, 'نتیجهٔ پایانی (' + pathNames.join('←') + ')', defOut, isE, leafIn);
          return ly;
        }
        var opts = stageLists[stIdx], sumW = opts.reduce(function (a, b) { return a + Math.max(1, Math.round(b[1] * 10)); }, 0);
        var xParent = stageX(stIdx), xChild = stageX(stIdx + 1), childYs = [];
        opts.forEach(function (opt, idx) {
          var wInt = Math.max(1, Math.round(opt[1] * 10));
          childYs.push(buildStage(stIdx + 1, pathIdxs.concat([idx]), probNum * wInt, probDen * sumW, pathNames.concat([opt[0]])));
        });
        var py = childYs.reduce(function (a, b) { return a + b; }, 0) / childYs.length;
        s += pfxCls(xParent, py, 3.8, ACC, '#ffffff', 1.2);
        opts.forEach(function (opt, idx) {
          var cy2 = childYs[idx], nKey = 'n_' + stIdx + '_' + pathIdxs.concat([idx]).join('_'), defLbl = opt[0];
          var isNE = !!pbEmp[nKey];
          var pillW = nSt >= 4 ? 40 : (nSt === 3 ? 46 : 54), pillH = totalLeaves >= 12 ? 18 : 21;
          var pillX = xChild - pillW / 2, pillY = cy2 - pillH / 2;
          var xLineEnd = pillX - 2;
          if (xLineEnd > xParent + 5) {
            s += ln(xParent + 4, py, xLineEnd, cy2, ACC, 1.5);
            var ang = Math.atan2(cy2 - py, xLineEnd - (xParent + 4));
            s += pfxPoly(f2(xLineEnd) + ',' + f2(cy2) + ' ' + f2(xLineEnd - 6 * Math.cos(ang - 0.45)) + ',' + f2(cy2 - 6 * Math.sin(ang - 0.45)) + ' ' + f2(xLineEnd - 6 * Math.cos(ang + 0.45)) + ',' + f2(cy2 - 6 * Math.sin(ang + 0.45)), ACC);
          }
          var nodeIn = '';
          if (isNE) {
            nodeIn += rcDash(pillX, pillY, pillW, pillH, '#ffffff', '#4f46e5', 1.4, 5, '4 2');
            var stv2 = (pbAns[nKey] || '').trim();
            if (stv2) nodeIn += tx(xChild, cy2 + 4, stv2, '#1d4ed8', 'middle', true, pillH <= 18 ? 8.5 : 9.5, W);
          } else {
            var disp2 = (pbOv[nKey] != null ? pbOv[nKey] : defLbl).trim();
            nodeIn += rc(pillX, pillY, pillW, pillH, '#eef2ff', ACC, 1.3, 5);
            nodeIn += tx(xChild, cy2 + 4, disp2, '#1e1b4b', 'middle', true, pillH <= 18 ? 8.5 : 9.5, W);
          }
          s += icWrap(nKey, 'شاخهٔ مرحلهٔ ' + (stIdx + 1) + ' (' + defLbl + ')', defLbl, isNE, nodeIn);
        });
        return py;
      }
      buildStage(0, [], 1, 1, []);
      return wrap(s, H, W);
    }
    if (t === 'axconic') {
      var cnKind = xs('kind', 'ellipse').toLowerCase(), cnA = Math.max(0.5, num(X.a, 4)), cnB = Math.max(0.5, num(X.b, 2));
      var cnCx = 180, cnCy = 136, cnS = Math.min(240 / (2 * Math.max(cnA, cnB) + 2), 92 / cnB, 26);
      s += lnr(cnCx - 150, cnCy, cnCx + 158, cnCy, ST, 1.4) + pfxArrow(cnCx + 146, cnCy, cnCx + 164, cnCy, ST, 1.4, 7);
      s += lnr(cnCx, cnCy + 110, cnCx, cnCy - 112, ST, 1.4) + pfxArrow(cnCx, cnCy - 100, cnCx, cnCy - 118, ST, 1.4, 7);
      s += tx(cnCx + 164, cnCy + 16, 'x', ST, 'start', true, 12) + tx(cnCx - 16, cnCy - 106, 'y', ST, 'start', true, 12);
      if (cnKind === 'hyperbola') {
        s += lnr(cnCx - 130, cnCy - 88, cnCx + 130, cnCy + 88, '#b0bec5', 1, '5 4') + lnr(cnCx - 130, cnCy + 88, cnCx + 130, cnCy - 88, '#b0bec5', 1, '5 4');
        s += pfxPath('M ' + f2(cnCx - cnA * cnS) + ' ' + f2(cnCy - 104) + ' Q ' + f2(cnCx - cnA * cnS * 0.35) + ' ' + f2(cnCy) + ' ' + f2(cnCx - cnA * cnS) + ' ' + f2(cnCy + 104), 'none', PT, 2.4);
        s += pfxPath('M ' + f2(cnCx + cnA * cnS) + ' ' + f2(cnCy - 104) + ' Q ' + f2(cnCx + cnA * cnS * 0.35) + ' ' + f2(cnCy) + ' ' + f2(cnCx + cnA * cnS) + ' ' + f2(cnCy + 104), 'none', PT, 2.4);
      } else if (cnKind === 'parabola') {
        s += pfxPath('M ' + f2(cnCx - 120) + ' ' + f2(cnCy - 96) + ' Q ' + f2(cnCx - 120) + ' ' + f2(cnCy) + ' ' + f2(cnCx) + ' ' + f2(cnCy) + ' Q ' + f2(cnCx + 120) + ' ' + f2(cnCy) + ' ' + f2(cnCx + 120) + ' ' + f2(cnCy - 96), 'none', PT, 2.4);
        s += lnr(cnCx, cnCy, cnCx, cnCy - 96, '#b0bec5', 1, '5 4') + lnr(cnCx - 132, cnCy - 96, cnCx + 132, cnCy - 96, GRN, 1.6, '6 4');
        s += pfxCls(cnCx, cnCy - 40, 4.4, PT, 'none', 0) + tx(cnCx + 8, cnCy - 38, 'F', PT, 'start', true, 12);
      } else {
        s += '<ellipse cx="' + cnCx + '" cy="' + cnCy + '" rx="' + f2(cnA * cnS) + '" ry="' + f2(cnB * cnS) + '" fill="rgba(108,99,245,.10)" stroke="' + ACC + '" stroke-width="2.2"/>';
        var cnF = Math.sqrt(Math.max(0, cnA * cnA - cnB * cnB)) * cnS;
        s += pfxCls(cnCx + cnF, cnCy, 4.2, PT, 'none', 0) + pfxCls(cnCx - cnF, cnCy, 4.2, PT, 'none', 0);
        s += tx(cnCx + cnF + 6, cnCy - 8, "F'", PT, 'start', true, 11) + tx(cnCx - cnF - 16, cnCy - 8, 'F', PT, 'start', true, 11);
        s += lnr(cnCx, cnCy, cnCx + cnA * cnS, cnCy, ACC, 1.4) + tx(cnCx + cnA * cnS / 2, cnCy + 15, 'a', ACC, 'middle', true, 11);
        s += lnr(cnCx, cnCy, cnCx, cnCy - cnB * cnS, GRN, 1.4) + tx(cnCx + 8, cnCy - cnB * cnS / 2, 'b', '#00796b', 'start', true, 11);
      }
      s += tx(180, 262, cnKind === 'hyperbola' ? 'هذلولی' : cnKind === 'parabola' ? 'سهمی' : 'بیضی', MU, 'middle', true, 12);
      return wrap(s);
    }
    if (t === 'axorbital') {
      var obMode = xs('mode', 'box').toLowerCase();
      var obEmp = pfxEmptySet(xs('empty', '')), obOv = pfxCellMap(xs('ov', '')), obAns = pfxCellMap(xs('ans', ''));
      if (obMode === 'lewis') {
        var sym = xs('sym', 'O').trim() || 'O', valE = clamp(Math.floor(num(X.val, 6)), 1, 8);
        var cx = 180, cy = 124;
        var symEmpty = !!obEmp.sym, symDisp = (obOv.sym != null ? obOv.sym : sym).trim();
        var symIn = symEmpty
          ? (rcDash(cx - 28, cy - 28, 56, 56, '#f8fafc', '#4f46e5', 1.8, 12, '5 3') + ((obAns.sym || '').trim() ? tx(cx, cy + 8, obAns.sym.trim(), '#1d4ed8', 'middle', true, 22) : ''))
          : (pfxCls(cx, cy, 30, 'rgba(108,99,245,.14)', ACC, 2) + tx(cx, cy + 8, symDisp, ST, 'middle', true, 22));
        s += icWrap('sym', 'نماد شیمیایی عنصر', sym, symEmpty, symIn);
        var sides = [[cx - 16, cy - 54, 32, 20, true], [cx + 34, cy - 16, 20, 32, false], [cx - 16, cy + 34, 32, 20, true], [cx - 54, cy - 16, 20, 32, false]];
        var sideNames = ['بالا', 'راست', 'پایین', 'چپ'];
        sides.forEach(function (bx, si) {
          var cnt = (valE > si ? 1 : 0) + (valE > si + 4 ? 1 : 0), sk = 'side_' + si, isSE = !!obEmp[sk];
          var gSide = rc(bx[0], bx[1], bx[2], bx[3], 'rgba(255,255,255,.01)', 'none', 0);
          if (isSE) {
            gSide += rcDash(bx[0], bx[1], bx[2], bx[3], '#ffffff', '#4f46e5', 1.3, 4, '3 2');
            var stC = parseInt(faEn((obAns[sk] || '').trim()), 10);
            for (var d2 = 0; d2 < (isNaN(stC) ? 0 : Math.min(2, stC)); d2++) {
              var dx = bx[4] ? (stC === 1 ? bx[0] + bx[2] / 2 : bx[0] + 9 + d2 * 14) : bx[0] + bx[2] / 2;
              var dy = bx[4] ? bx[1] + bx[3] / 2 : (stC === 1 ? bx[1] + bx[3] / 2 : bx[1] + 9 + d2 * 14);
              gSide += pfxCls(dx, dy, 4.2, '#1d4ed8', 'none', 0);
            }
          } else {
            var ovC = obOv[sk] != null ? parseInt(faEn(obOv[sk].trim()), 10) : cnt;
            var useC = clamp(isNaN(ovC) ? cnt : ovC, 0, 2);
            for (var d3 = 0; d3 < useC; d3++) {
              var dx2 = bx[4] ? (useC === 1 ? bx[0] + bx[2] / 2 : bx[0] + 9 + d3 * 14) : bx[0] + bx[2] / 2;
              var dy2 = bx[4] ? bx[1] + bx[3] / 2 : (useC === 1 ? bx[1] + bx[3] / 2 : bx[1] + 9 + d3 * 14);
              gSide += pfxCls(dx2, dy2, 4.5, PT, 'none', 0);
            }
          }
          s += icWrap(sk, 'الکترون‌های سمت ' + sideNames[si], String(cnt), isSE, gSide);
        });
        var cfgE = !!obEmp.cfg, defCfg = 'الکترون ظرفیت: ' + valE;
        var cfgIn = cfgE
          ? (rcDash(80, 232, 200, 28, '#ffffff', '#4f46e5', 1.4, 7, '4 2') + ((obAns.cfg || '').trim() ? tx(180, 250, obAns.cfg.trim(), '#1d4ed8', 'middle', true, 11) : ''))
          : (rc(80, 232, 200, 28, '#f8fafc', '#cbd5e1', 1.2, 7) + tx(180, 250, (obOv.cfg || defCfg).trim(), '#1e293b', 'middle', true, 11));
        s += icWrap('cfg', 'توضیح الکترون ظرفیت', defCfg, cfgE, cfgIn);
        return wrap(s);
      }

      var z = clamp(Math.floor(num(X.z, 26)), 1, 54);
      var elems = ['','H','He','Li','Be','B','C','N','O','F','Ne','Na','Mg','Al','Si','P','S','Cl','Ar','K','Ca','Sc','Ti','V','Cr','Mn','Fe','Co','Ni','Cu','Zn','Ga','Ge','As','Se','Br','Kr','Rb','Sr','Y','Zr','Nb','Mo','Tc','Ru','Rh','Pd','Ag','Cd','In','Sn','Sb','Te','I','Xe'];
      var subshells = [['1s',1,0],['2s',1,0],['2p',3,0],['3s',1,0],['3p',3,0],['4s',1,1],['3d',5,1],['4p',3,1],['5s',1,2],['4d',5,2],['5p',3,2]];
      var rem = z, counts = {};
      subshells.forEach(function (sb2) { var tk = Math.min(rem, sb2[1] * 2); counts[sb2[0]] = tk; rem -= tk; });
      if (z === 24) { counts['4s'] = 1; counts['3d'] = 5; }
      else if (z === 29) { counts['4s'] = 1; counts['3d'] = 10; }
      else if (z === 42) { counts['5s'] = 1; counts['4d'] = 5; }
      else if (z === 47) { counts['5s'] = 1; counts['4d'] = 10; }
      else if (z === 46) { counts['5s'] = 0; counts['4d'] = 10; }

      var usedSubs = subshells.filter(function (sb2) { return (counts[sb2[0]] || 0) > 0 || sb2[0] === '1s'; });
      var activeRows = [];
      usedSubs.forEach(function (sb2) { if (activeRows.indexOf(sb2[2]) < 0) activeRows.push(sb2[2]); });
      s += tx(180, 22, 'آرایش اوربیتالی ' + elems[z] + ' (Z = ' + z + ')', '#1e293b', 'middle', true, 12);

      var nR = activeRows.length, bw = nR === 1 ? 26 : (nR === 2 ? 23 : 21), bh = nR === 1 ? 30 : (nR === 2 ? 26 : 22);
      var subGap = nR === 1 ? 10 : 8, rowStep = nR === 1 ? 0 : (nR === 2 ? 64 : 52);
      var baseY = nR === 1 ? 106 : (nR === 2 ? 64 : 44);
      var supMap = {'0':'⁰','1':'¹','2':'²','3':'³','4':'⁴','5':'⁵','6':'⁶','7':'⁷','8':'⁸','9':'⁹'};
      var toSup = function (v0) { return String(v0).split('').map(function (c0) { return supMap[c0] || c0; }).join(''); };

      var drawSpinInBox = function (bx, by, code) {
        var t0 = String(code || '').trim(), midY = by + bh / 2, topY = by + 4, botY = by + bh - 4, o = '';
        var upArrow = function (ax) { return ln(ax, botY, ax, topY, ACC, 1.7) + pfxPoly(f2(ax) + ',' + f2(topY - 1) + ' ' + f2(ax - 3) + ',' + f2(topY + 5) + ' ' + f2(ax + 3) + ',' + f2(topY + 5), ACC); };
        var dnArrow = function (ax) { return ln(ax, topY, ax, botY, PT, 1.7) + pfxPoly(f2(ax) + ',' + f2(botY + 1) + ' ' + f2(ax - 3) + ',' + f2(botY - 5) + ' ' + f2(ax + 3) + ',' + f2(botY - 5), PT); };
        if (t0 === '↑↓' || t0 === '2' || t0.toLowerCase() === 'ud') o += upArrow(bx + bw * 0.34) + dnArrow(bx + bw * 0.68);
        else if (t0 === '↑' || t0 === '1' || t0.toLowerCase() === 'u') o += upArrow(bx + bw * 0.5);
        else if (t0 === '↓' || t0.toLowerCase() === 'd') o += dnArrow(bx + bw * 0.5);
        else if (t0) o += tx(bx + bw / 2, midY + 4, t0, '#1d4ed8', 'middle', true, 9);
        return o;
      };

      activeRows.forEach(function (rVal, rIdx) {
        var rowSubs = usedSubs.filter(function (sb2) { return sb2[2] === rVal; });
        var rowW = rowSubs.reduce(function (a, b) { return a + b[1] * (bw + 2); }, 0) + Math.max(0, rowSubs.length - 1) * subGap;
        var curX = (360 - rowW) / 2, ry = baseY + rIdx * rowStep;
        rowSubs.forEach(function (sb2) {
          var sName = sb2[0], nBoxes = sb2[1], eCnt = counts[sName] || 0, subStartX = curX;
          for (var bi = 0; bi < nBoxes; bi++) {
            var up = eCnt > bi, down = eCnt > bi + nBoxes;
            var defSpin = (up && down) ? '↑↓' : (up ? '↑' : '');
            var key = sName + '_' + bi, isE = !!obEmp[key];
            var boxIn = rc(curX, ry, bw, bh, isE ? '#ffffff' : 'rgba(108,99,245,.08)', isE ? '#4f46e5' : ST, isE ? 1.7 : 1.4, 3);
            if (isE) {
              boxIn += rcDash(curX + 2.5, ry + 2.5, bw - 5, bh - 5, '#f8fafc', '#6366f1', 1.1, 2, '3 2');
              var stv = (obAns[key] || '').trim(); if (stv) boxIn += drawSpinInBox(curX, ry, stv);
            } else {
              var disp = obOv[key] != null ? obOv[key] : defSpin;
              boxIn += drawSpinInBox(curX, ry, disp);
            }
            s += icWrap(key, 'اوربیتال ' + sName + ' (خانهٔ ' + (bi + 1) + ')', defSpin, isE, boxIn);
            curX += bw + 2;
          }
          var subCenter = (subStartX + curX - 2) / 2;
          s += tx(subCenter, ry + bh + 13, sName + toSup(eCnt), MU, 'middle', true, nR === 3 ? 9 : 10);
          curX += subGap;
        });
      });

      var cfgFull = usedSubs.filter(function (sb2) { return (counts[sb2[0]] || 0) > 0; }).map(function (sb2) { return sb2[0] + toSup(counts[sb2[0]]); }).join(' ');
      var cfgGas = obMode === 'gas' ? (z > 36 ? '[Kr] ' + usedSubs.filter(function (q2) { return ['5s','4d','5p'].indexOf(q2[0]) >= 0 && counts[q2[0]] > 0; }).map(function (q2) { return q2[0] + toSup(counts[q2[0]]); }).join(' ')
        : z > 18 ? '[Ar] ' + usedSubs.filter(function (q2) { return ['4s','3d','4p'].indexOf(q2[0]) >= 0 && counts[q2[0]] > 0; }).map(function (q2) { return q2[0] + toSup(counts[q2[0]]); }).join(' ')
        : z > 10 ? '[Ne] ' + usedSubs.filter(function (q2) { return ['3s','3p'].indexOf(q2[0]) >= 0 && counts[q2[0]] > 0; }).map(function (q2) { return q2[0] + toSup(counts[q2[0]]); }).join(' ') : cfgFull) : cfgFull;
      var cfgKey = 'cfg', cfgEmpty = !!obEmp[cfgKey];
      var cfgSvg = cfgEmpty
        ? (rcDash(18, 240, 324, 28, '#ffffff', '#4f46e5', 1.4, 7, '4 2.5') + ((obAns[cfgKey] || '').trim() ? tx(180, 258, obAns[cfgKey].trim(), '#1d4ed8', 'middle', true, 10.5) : ''))
        : (rc(18, 240, 324, 28, '#f8fafc', '#cbd5e1', 1.2, 7) + tx(180, 258, (obOv[cfgKey] || cfgGas).trim(), '#1e293b', 'middle', true, 10.5));
      s += icWrap(cfgKey, 'آرایش الکترونی نوشتاری', cfgGas, cfgEmpty, cfgSvg);
      return wrap(s);
    }
    if (t === 'axpunnett') {
      var pnKind = xs('kind', 'punnett').toLowerCase(), pnMode = xs('mode', 'genotype').toLowerCase();
      var pnEmp = pfxEmptySet(xs('empty', '')), pnOv = pfxCellMap(xs('ov', '')), pnAns = pfxCellMap(xs('ans', ''));
      if (pnKind === 'pedigree') {
        var gens = clamp(Math.floor(num(X.gens, 3)), 2, 4);
        var yForGen = function (g) { return gens === 2 ? (74 + g * 92) : (gens === 3 ? (48 + g * 74) : (38 + g * 55)); };
        var rNode = gens >= 4 ? 11.5 : 14;
        var drawPed = function (key, label, cx, cy, defState) {
          var isE = !!pnEmp[key] || (pnMode === 'blank' && key.indexOf('g' + (gens - 1)) === 0);
          var rawSt = isE ? (pnAns[key] || '').trim() : (pnOv[key] != null ? pnOv[key] : defState).trim();
          var up = rawSt.toUpperCase(), isMale = up.indexOf('M') === 0, isFem = up.indexOf('F') === 0;
          var aff = up.indexOf('*') >= 0 || up.indexOf('A') >= 0, car = up.indexOf('C') >= 0;
          var gIn = '';
          if (isE && !rawSt) {
            gIn += rcDash(cx - rNode, cy - rNode, 2 * rNode, 2 * rNode, '#ffffff', '#4f46e5', 1.5, 4, '3 2');
          } else if (isMale || (!isFem && defState.indexOf('M') === 0)) {
            gIn += rc(cx - rNode, cy - rNode, 2 * rNode, 2 * rNode, aff ? PT : '#ffffff', isE ? '#1d4ed8' : ST, 1.8, 2);
            if (car && !aff) gIn += rc(cx - rNode, cy - rNode, rNode, 2 * rNode, 'rgba(214,51,108,.45)', 'none', 0);
          } else {
            gIn += pfxCls(cx, cy, rNode, aff ? PT : '#ffffff', isE ? '#1d4ed8' : ST, 1.8);
            if (car && !aff) gIn += pfxPath('M ' + f2(cx) + ' ' + f2(cy - rNode) + ' A ' + f2(rNode) + ' ' + f2(rNode) + ' 0 0 0 ' + f2(cx) + ' ' + f2(cy + rNode) + ' Z', 'rgba(214,51,108,.45)', 'none', 0);
          }
          if (rawSt && !isMale && !isFem) gIn += tx(cx, cy + 4, rawSt, '#1d4ed8', 'middle', true, 9.5);
          s += icWrap(key, label, defState, isE, gIn);
        };
        var rom = ['I', 'II', 'III', 'IV'];
        for (var g0 = 0; g0 < gens; g0++) s += tx(24, yForGen(g0) + 4, rom[g0], MU, 'middle', true, 11);
        var y0 = yForGen(0), y1 = yForGen(1);
        s += ln(124 + rNode, y0, 224 - rNode, y0, ST, 1.7) + ln(174, y0, 174, y1 - 24, ST, 1.7);
        drawPed('g0_1', 'نسل I فرد ۱', 124, y0, 'Mc');
        drawPed('g0_2', 'نسل I فرد ۲', 224, y0, 'Fc');
        s += ln(96, y1 - 24, 232, y1 - 24, ST, 1.7);
        [96, 164, 232].forEach(function (x10) { s += ln(x10, y1 - 24, x10, y1 - rNode, ST, 1.7); });
        drawPed('g1_1', 'نسل II فرد ۱', 96, y1, 'M*');
        drawPed('g1_2', 'نسل II فرد ۲', 164, y1, 'F');
        drawPed('g1_3', 'نسل II فرد ۳', 232, y1, 'Mc');
        if (gens >= 3) {
          var y2 = yForGen(2);
          drawPed('g1_4', 'نسل II همسر (۴)', 296, y1, 'Fc');
          s += ln(232 + rNode, y1, 296 - rNode, y1, ST, 1.7) + ln(264, y1, 264, y2 - 22, ST, 1.7);
          s += ln(196, y2 - 22, 312, y2 - 22, ST, 1.7);
          [196, 254, 312].forEach(function (x20) { s += ln(x20, y2 - 22, x20, y2 - rNode, ST, 1.7); });
          drawPed('g2_1', 'نسل III فرد ۱', 196, y2, 'F*');
          drawPed('g2_2', 'نسل III فرد ۲', 254, y2, 'M');
          drawPed('g2_3', 'نسل III فرد ۳', 312, y2, 'Fc');
        }
        if (gens >= 4) {
          var y2b = yForGen(2), y3 = yForGen(3);
          drawPed('g2_0', 'نسل III همسر', 128, y2b, 'M');
          s += ln(128 + rNode, y2b, 196 - rNode, y2b, ST, 1.7) + ln(162, y2b, 162, y3 - 20, ST, 1.7);
          s += ln(122, y3 - 20, 202, y3 - 20, ST, 1.7);
          s += ln(122, y3 - 20, 122, y3 - rNode, ST, 1.7) + ln(202, y3 - 20, 202, y3 - rNode, ST, 1.7);
          drawPed('g3_1', 'نسل IV فرد ۱', 122, y3, 'Mc');
          drawPed('g3_2', 'نسل IV فرد ۲', 202, y3, 'Fc');
        }
        s += rc(16, 244, 328, 24, '#f8fafc', '#cbd5e1', 1, 6);
        s += tx(180, 260, '□ مرد   ○ زن   ■/● مبتلا   نیمه‌پر: ناقل (روی هر فرد بزنید)', MU, 'middle', true, 9.5);
        return wrap(s);
      }

      var rawP1 = xs('p1', 'Aa').replace(/\s+/g, ''), rawP2 = xs('p2', 'Aa').replace(/\s+/g, '');
      var isDihybrid = rawP1.length >= 4 && rawP2.length >= 4;
      var gametesOf = function (p, di) {
        if (!di) { var a = p[0] || 'A', b = p[1] || a; return [a, b]; }
        var a1 = p[0] || 'A', a2 = p[1] || 'a', b1 = p[2] || 'B', b2 = p[3] || 'b';
        return [a1 + b1, a1 + b2, a2 + b1, a2 + b2];
      };
      var combineGam = function (g1, g2) {
        if (g1.length >= 2 && g2.length >= 2) {
          var loc1 = [g1[0], g2[0]].sort().join(''), loc2 = [g1[1], g2[1]].sort().join('');
          return loc1 + loc2;
        }
        return [g1, g2].sort().join('');
      };
      var gCols = gametesOf(rawP1, isDihybrid), gRows = gametesOf(rawP2, isDihybrid);
      var gridN = gCols.length, cs = isDihybrid ? 40 : 66, hdr = isDihybrid ? 24 : 30;
      var x0 = (360 - (hdr + gridN * cs)) / 2 + hdr, y0 = isDihybrid ? 42 : 52;
      s += rc(x0 - hdr, y0 - hdr, hdr, hdr, '#f1f5f9', '#94a3b8', 1.2, 3) + ln(x0 - hdr, y0 - hdr, x0, y0, '#94a3b8', 1);

      gCols.forEach(function (gc, c) {
        var hk = 'hc_' + c, isHE = !!pnEmp[hk], bx = x0 + c * cs, by = y0 - hdr;
        var hIn = isHE
          ? (rcDash(bx + 1, by + 1, cs - 2, hdr - 2, '#ffffff', '#4f46e5', 1.3, 3, '3 2') + ((pnAns[hk] || '').trim() ? tx(bx + cs / 2, by + hdr * 0.72, pnAns[hk].trim(), '#1d4ed8', 'middle', true, isDihybrid ? 10 : 13) : ''))
          : (rc(bx, by, cs, hdr, '#eef2ff', ACC, 1.3, 2) + tx(bx + cs / 2, by + hdr * 0.72, (pnOv[hk] != null ? pnOv[hk] : gc).trim(), ACC, 'middle', true, isDihybrid ? 10 : 13));
        s += icWrap(hk, 'گامت والد اول (' + (c + 1) + ')', gc, isHE, hIn);
      });
      gRows.forEach(function (gr, r) {
        var hk = 'hr_' + r, isHE = !!pnEmp[hk], bx = x0 - hdr, by = y0 + r * cs;
        var hIn = isHE
          ? (rcDash(bx + 1, by + 1, hdr - 2, cs - 2, '#ffffff', '#4f46e5', 1.3, 3, '3 2') + ((pnAns[hk] || '').trim() ? tx(bx + hdr / 2, by + cs * 0.58, pnAns[hk].trim(), '#1d4ed8', 'middle', true, isDihybrid ? 10 : 13) : ''))
          : (rc(bx, by, hdr, cs, '#fdf2f8', PT, 1.3, 2) + tx(bx + hdr / 2, by + cs * 0.58, (pnOv[hk] != null ? pnOv[hk] : gr).trim(), PT, 'middle', true, isDihybrid ? 10 : 13));
        s += icWrap(hk, 'گامت والد دوم (' + (r + 1) + ')', gr, isHE, hIn);
      });

      var genoCounts = {};
      for (var r2 = 0; r2 < gridN; r2++) for (var c2 = 0; c2 < gridN; c2++) {
        var bx2 = x0 + c2 * cs, by2 = y0 + r2 * cs, defG = combineGam(gCols[c2], gRows[r2]);
        genoCounts[defG] = (genoCounts[defG] || 0) + 1;
        var ck = 'c_' + r2 + '_' + c2, autoE = pnMode === 'blank' && (r2 + c2) % 2 === 1, isCE = !!pnEmp[ck] || autoE;
        var dom = defG[0] >= 'A' && defG[0] <= 'Z';
        var cIn = rc(bx2, by2, cs, cs, isCE ? '#ffffff' : (dom ? 'rgba(108,99,245,.12)' : 'rgba(39,196,168,.15)'), isCE ? '#4f46e5' : ST, 1.5, 2);
        if (isCE) {
          cIn += rcDash(bx2 + 4, by2 + 4, cs - 8, cs - 8, '#f8fafc', '#6366f1', 1.2, 4, '3 2');
          var stv = (pnAns[ck] || '').trim();
          if (stv) cIn += tx(bx2 + cs / 2, by2 + cs * 0.58, stv, '#1d4ed8', 'middle', true, isDihybrid ? 10 : 15);
        } else {
          var disp = (pnOv[ck] != null ? pnOv[ck] : defG).trim();
          cIn += tx(bx2 + cs / 2, by2 + cs * 0.58, disp, ST, 'middle', true, isDihybrid ? 10 : 15);
        }
        s += icWrap(ck, 'خانهٔ (' + (r2 + 1) + '،' + (c2 + 1) + ') پانت', defG, isCE, cIn);
      }

      var ratioTxt = isDihybrid ? ('تلاقی دورگه (' + rawP1 + ' × ' + rawP2 + ') — نسبت فنوتیپی کلاسیک 9:3:3:1')
        : ('ژنوتیپ‌ها: ' + Object.keys(genoCounts).map(function (gk) { return genoCounts[gk] + gk; }).join(' : '));
      var rEmpty = !!pnEmp.ratio;
      var rIn = rEmpty
        ? (rcDash(18, 236, 324, 28, '#ffffff', '#4f46e5', 1.4, 7, '4 2') + ((pnAns.ratio || '').trim() ? tx(180, 254, pnAns.ratio.trim(), '#1d4ed8', 'middle', true, 10.5) : ''))
        : (rc(18, 236, 324, 28, '#f8fafc', '#cbd5e1', 1.2, 7) + tx(180, 254, (pnOv.ratio || ratioTxt).trim(), '#1e293b', 'middle', true, 10.5));
      s += icWrap('ratio', 'نسبت ژنوتیپی و فنوتیپی', ratioTxt, rEmpty, rIn);
      return wrap(s);
    }
    if (t === 'axecg') {
      var ecKind = xs('kind', 'ecg').toLowerCase(), ecN = clamp(Math.floor(num(X.n, 2)), 1, 3), ecLbl = xs('label', '1') !== '0';
      if (ecKind === 'shm') {
        var shA = Math.max(0.2, num(X.A, 1)), shT = Math.max(0.5, num(X.T, 2)), shX0 = 40, shX1 = 330, shY = 140, shAmp = 62 * shA;
        s += lnr(shX0, shY, shX1 + 8, shY, ST, 1.6) + pfxArrow(shX1, shY, shX1 + 12, shY, ST, 1.6, 7);
        s += lnr(shX0 + 0, shY + 80, shX0, shY - 84, ST, 1.6) + pfxArrow(shX0, shY - 74, shX0, shY - 90, ST, 1.6, 7);
        var shPts = '';
        for (var si2 = 0; si2 <= 140; si2++) { var xx5 = shX0 + (shX1 - shX0) * si2 / 140, yy5 = shY - shAmp * Math.sin(2 * Math.PI * (si2 / 140) * 2); shPts += f2(xx5) + ',' + f2(yy5) + ' '; }
        s += '<polyline points="' + shPts.trim() + '" fill="none" stroke="' + PT + '" stroke-width="2.2"/>';
        s += lnr(shX0, shY - shAmp, shX0 + 6, shY - shAmp, GRN, 1.4) + tx(shX0 + 10, shY - shAmp + 4, 'A', '#00796b', 'start', true, 11);
        s += tx(182, 258, 'نوسان هماهنگ ساده — دامنه ' + nm(shA) + ' ، دوره ' + nm(shT), MU, 'middle', true, 12);
        return wrap(s);
      }
      var ecX0 = 34, ecW = 300 / ecN, ecY = 168, ecS = 0.2;
      for (var gj2 = 0; gj2 < 40; gj2++) for (var gi2 = 0; gi2 < 26; gi2++) {
        s += '<circle cx="' + f2(36 + gj2 * 7.6) + '" cy="' + f2(36 + gi2 * 7.6) + '" r="0.9" fill="#f3b6c2"/>';
      }
      var ecPath = '', ecLab = [];
      for (var eb = 0; eb < ecN; eb++) {
        var ex0 = ecX0 + eb * ecW, ey = ecY, u7 = ecW;
        var pts4 = [
          [0, 0], [0.10, 0], [0.16, -12], [0.22, 0], [0.30, 0],
          [0.34, 8], [0.38, -74], [0.42, 22], [0.46, 0], [0.60, 0], [0.68, -22], [0.76, 0], [1, 0]
        ];
        ecPath += (eb === 0 ? 'M ' : 'L ') + f2(ex0) + ' ' + f2(ey) + ' ';
        pts4.forEach(function (pp2) { ecPath += 'L ' + f2(ex0 + pp2[0] * u7) + ' ' + f2(ey + pp2[1]) + ' '; });
        ecLab.push([ex0 + 0.16 * u7, ey - 26, 'P'], [ex0 + 0.38 * u7, ey - 86, 'QRS'], [ex0 + 0.68 * u7, ey - 36, 'T']);
      }
      s += lnr(ecX0 - 8, ecY, ecX0 + ecW * ecN + 8, ecY, '#9aa7ba', 1);
      s += pfxPath(ecPath, 'none', PT, 2.2);
      if (ecLbl) ecLab.forEach(function (lp) { s += tx(lp[0], lp[1], lp[2], '#0d47a1', 'middle', true, 10); });
      s += tx(180, 258, ecKind === 'ecg' ? 'نوار قلب (ECG) — موج‌های P و QRS و T' : 'نوار نوسان‌نگار', MU, 'middle', true, 12);
      return wrap(s);
    }
    // ============================== V318 — بستهٔ جامع ابزارهای آموزشی (۴) انسانی، هنر و فنی
    if (t === 'axprosody') {
      var prFeet = xs('line', 'فعولن فعولن فعولن فعل').trim().split(/\s+/).filter(Boolean).slice(0, 4);
      var prVerse = xs('verse', '').trim(), prMode = xs('mode', 'arkan').toLowerCase();
      var footPat = {'فعولن':'U--','فاعلن':'-U-','مفاعیلن':'U---','مستفعلن':'--U-','فاعلاتن':'-U--','مفاعلن':'U-U-','فعلاتن':'UU--','مفتعلن':'-UU-','مفعولن':'---','فع لن':'--','فعلن':'UU-','فعل':'U-','فع':'-'};
      var nF = Math.max(1, prFeet.length), fw = (324 - (nF - 1) * 8) / nF;
      var yVerse = 38, yBox = prVerse ? 64 : 46;
      if (prVerse) {
        s += rc(20, yVerse - 18, 320, 28, '#fffbeb', '#f59e0b', 1.2, 7);
        s += tx(180, yVerse, prVerse, '#78350f', 'middle', true, 12);
      }
      var pats = [];
      for (var fi = 0; fi < nF; fi++) {
        var foot = prFeet[nF - 1 - fi], pat = footPat[foot] || 'U--', bx = 18 + fi * (fw + 8);
        pats.unshift(pat);
        s += rc(bx, yBox, fw, 92, 'rgba(108,99,245,.08)', ACC, 1.6, 8);
        if (prMode === 'arkan') s += tx(bx + fw / 2, yBox + 24, foot, ST, 'middle', true, nF >= 4 ? 12 : 13);
        else if (prMode === 'blank') s += rcDash(bx + 8, yBox + 8, fw - 16, 22, '#ffffff', '#6366f1', 1.2, 4, '3 2');
        var step = (fw - 16) / Math.max(1, pat.length), sy = yBox + (prMode === 'taqti' ? 48 : 64);
        for (var si = 0; si < pat.length; si++) {
          var ch = pat[pat.length - 1 - si], sx = bx + 8 + (si + 0.5) * step;
          if (ch === 'U') s += pfxPath('M ' + f2(sx - 6) + ' ' + f2(sy - 5) + ' Q ' + f2(sx) + ' ' + f2(sy + 7) + ' ' + f2(sx + 6) + ' ' + f2(sy - 5), 'none', PT, 2.3);
          else if (ch === '~') s += ln(sx - 7, sy - 2, sx + 7, sy - 2, '#0d9488', 2.4) + pfxCls(sx, sy + 5, 1.8, '#0d9488', 'none', 0);
          else s += ln(sx - 7, sy, sx + 7, sy, '#1e1b4b', 2.6);
        }
        s += tx(bx + fw / 2, yBox + 84, pat.split('').map(function (z) { return z === 'U' ? '∪' : '−'; }).join(' '), MU, 'middle', false, 9.5);
      }
      var joined = prFeet.join(' ');
      var bahr = joined.indexOf('فعولن فعولن فعولن فعل') === 0 ? 'بحر متقارب مثمن محذوف (شاهنامه / بوستان)'
        : joined.indexOf('فعولن فعولن فعولن فعولن') === 0 ? 'بحر متقارب مثمن سالم'
        : joined.indexOf('فاعلاتن فاعلاتن فاعلاتن فاعلن') === 0 ? 'بحر رمل مثمن محذوف (مثنوی معنوی)'
        : joined.indexOf('مفاعیلن مفاعیلن مفاعیلن مفاعیلن') === 0 ? 'بحر هزج مثمن سالم'
        : joined.indexOf('مستفعلن مستفعلن مستفعلن مستفعلن') === 0 ? 'بحر رجز مثمن سالم'
        : ('وزن عروضی: ' + joined);
      s += rc(18, 224, 324, 34, '#f8fafc', '#cbd5e1', 1.2, 8);
      s += tx(180, 240, prMode === 'blank' ? 'نام وزن و بحر عروضی: ؟' : bahr, '#1e293b', 'middle', true, 10.5);
      s += tx(180, 253, '∪ هجای کوتاه   |   − هجای بلند   (' + pats.reduce(function (a, b) { return a + b.length; }, 0) + ' هجا)', MU, 'middle', false, 9);
      return wrap(s);
    }
    if (t === 'axtruthtable') {
      var ttKind = xs('kind', 'truth').toLowerCase();
      if (ttKind === 'square') {
        s += rc(126, 34, 108, 44, 'rgba(108,99,245,.14)', ACC, 1.8, 6) + tx(180, 62, 'موجبهٔ کلی', ACC, 'middle', true, 11);
        s += rc(126, 182, 108, 44, 'rgba(108,99,245,.14)', ACC, 1.8, 6) + tx(180, 210, 'موجبهٔ جزئی', ACC, 'middle', true, 11);
        s += rc(24, 108, 104, 44, 'rgba(39,196,168,.14)', GRN, 1.8, 6) + tx(76, 136, 'سالبهٔ کلی', '#00796b', 'middle', true, 11);
        s += rc(232, 108, 104, 44, 'rgba(39,196,168,.14)', GRN, 1.8, 6) + tx(284, 136, 'سالبهٔ جزئی', '#00796b', 'middle', true, 11);
        s += lnr(150, 78, 120, 108, PT, 1.4) + tx(124, 96, 'تضاد', PT, 'middle', true, 9);
        s += lnr(210, 78, 240, 108, PT, 1.4) + tx(238, 96, 'تضاد', PT, 'middle', true, 9);
        s += lnr(150, 182, 120, 152, PT, 1.4) + tx(124, 174, 'تداخل', PT, 'middle', true, 9);
        s += lnr(210, 182, 240, 152, PT, 1.4) + tx(238, 174, 'تداخل', PT, 'middle', true, 9);
        s += lnr(180, 78, 180, 182, '#9aa7ba', 1.4, '5 4') + tx(180, 130, 'تناقض', '#607d8b', 'middle', true, 9);
        s += tx(180, 254, 'مربع تقابل ارسطویی', MU, 'middle', true, 12);
        return wrap(s);
      }
      var ttRows = [[0, 0], [0, 1], [1, 0], [1, 1]], ttExpr = xs('expr', 'p و q');
      var ttCols = ['p', 'q', 'p ∧ q', 'p ∨ q', 'p → q'], ttWidths = [34, 34, 60, 60, 60];
      var ttW = ttWidths.reduce(function (a4, b4) { return a4 + b4; }, 0), ttX = 180 - ttW / 2, ttY = 52, ttRh = 30;
      var ttx = ttX;
      ttCols.forEach(function (c4, ci3) { s += rc(ttx, ttY, ttWidths[ci3], ttRh, 'rgba(108,99,245,.16)', ST, 1.4) + tx(ttx + ttWidths[ci3] / 2, ttY + 20, c4, ST, 'middle', true, 11); ttx += ttWidths[ci3]; });
      ttRows.forEach(function (rw, ri3) {
        var ry4 = ttY + (ri3 + 1) * ttRh, vals = [rw[0], rw[1], rw[0] && rw[1], rw[0] || rw[1], (!rw[0] || rw[1]) ? 1 : 0];
        var cx5 = ttX;
        vals.forEach(function (v4, vi2) {
          s += rc(cx5, ry4, ttWidths[vi2], ttRh, '#ffffff', ST, 1) + tx(cx5 + ttWidths[vi2] / 2, ry4 + 20, v4 ? 'د' : 'ن', vi2 > 1 ? (v4 ? '#1b7f4c' : PT) : ST, 'middle', true, 12);
          cx5 += ttWidths[vi2];
        });
      });
      s += tx(180, ttY + 5 * ttRh + 26, 'جدول ارزش — ' + ttExpr + '   (د = درست ، ن = نادرست)', MU, 'middle', true, 11);
      return wrap(s);
    }
    if (t === 'axledger') {
      var lgTitle = xs('title', 'صندوق'), lgDebit = String(xs('debit', '12000,8500')).split(/[,،]/).map(function (q8) { return q8.trim(); }).filter(Boolean).slice(0, 4);
      var lgCredit = String(xs('credit', '6000,3000')).split(/[,،]/).map(function (q8) { return q8.trim(); }).filter(Boolean).slice(0, 4);
      var lgW = 300, lgX = 30, lgY = 52, lgColW = lgW / 2;
      s += rc(lgX, lgY, lgW, 26, 'rgba(108,99,245,.14)', ST, 1.6) + rc(lgX, lgY, lgColW, 26, 'rgba(108,99,245,.20)', ST, 1.6);
      s += tx(lgX + lgColW / 2, lgY + 18, 'بدهکار', ACC, 'middle', true, 12) + tx(lgX + lgColW + lgColW / 2, lgY + 18, 'بستانکار', '#00796b', 'middle', true, 12);
      var lgRows = Math.max(lgDebit.length, lgCredit.length), lgRh2 = 26, lgSumD = 0, lgSumC = 0;
      for (var lri = 0; lri < lgRows; lri++) {
        var ly2 = lgY + 26 + lri * lgRh2;
        s += rc(lgX, ly2, lgW, lgRh2, '#ffffff', '#c8d2e0', 1);
        if (lgDebit[lri]) { lgSumD += parseFloat(lgDebit[lri]) || 0; s += tx(lgX + lgColW - 8, ly2 + 18, lgDebit[lri], ST, 'end', false, 11); }
        if (lgCredit[lri]) { lgSumC += parseFloat(lgCredit[lri]) || 0; s += tx(lgX + lgW - 8, ly2 + 18, lgCredit[lri], ST, 'end', false, 11); }
      }
      var lgTy = lgY + 26 + lgRows * lgRh2;
      s += lnr(lgX + lgColW, lgY, lgX + lgColW, lgTy + lgRh2, ST, 1.8) + lnr(lgX, lgTy, lgX + lgW, lgTy, ST, 1.8) + lnr(lgX, lgTy + lgRh2, lgX + lgW, lgTy + lgRh2, ST, 1.8) + lnr(lgX, lgY, lgX, lgTy + lgRh2, ST, 1.8) + lnr(lgX + lgW, lgY, lgX + lgW, lgTy + lgRh2, ST, 1.8);
      s += tx(lgX + lgColW - 8, lgTy + 18, grp(lgSumD), ACC, 'end', true, 12) + tx(lgX + lgW - 8, lgTy + 18, grp(lgSumC), '#00796b', 'end', true, 12) + tx(lgX + 8, lgTy + 18, 'جمع', MU, 'start', true, 11);
      s += tx(180, lgTy + lgRh2 + 28, lgSumD === lgSumC ? 'تراز است ✓' : 'تراز نیست ✗', lgSumD === lgSumC ? '#1b7f4c' : PT, 'middle', true, 12);
      return wrap(s);
    }
    if (t === 'axstaff') {
      var stKind = xs('kind', 'staff').toLowerCase();
      if (stKind === 'color') {
        var harmony = xs('harmony', 'triad').toLowerCase(), cx = 180, cy = 124, rOut = 84, rIn = 40;
        var wheel = [['زرد','#facc15'],['زرد-نارنجی','#fb923c'],['نارنجی','#f97316'],['قرمز-نارنجی','#ef4444'],['قرمز','#dc2626'],['قرمز-بنفش','#a855f7'],['بنفش','#7e22ce'],['آبی-بنفش','#4f46e5'],['آبی','#2563eb'],['آبی-سبز','#0d9488'],['سبز','#16a34a'],['زرد-سبز','#84cc16']];
        wheel.forEach(function (w, i) {
          var a0 = (-90 + i * 30 - 15) * Math.PI / 180, a1 = (-90 + i * 30 + 15) * Math.PI / 180;
          s += pfxPath('M ' + f2(cx + rIn * Math.cos(a0)) + ' ' + f2(cy + rIn * Math.sin(a0)) + ' L ' + f2(cx + rOut * Math.cos(a0)) + ' ' + f2(cy + rOut * Math.sin(a0)) + ' A ' + f2(rOut) + ' ' + f2(rOut) + ' 0 0 1 ' + f2(cx + rOut * Math.cos(a1)) + ' ' + f2(cy + rOut * Math.sin(a1)) + ' L ' + f2(cx + rIn * Math.cos(a1)) + ' ' + f2(cy + rIn * Math.sin(a1)) + ' A ' + f2(rIn) + ' ' + f2(rIn) + ' 0 0 0 ' + f2(cx + rIn * Math.cos(a0)) + ' ' + f2(cy + rIn * Math.sin(a0)) + ' Z', w[1], '#ffffff', 1.6);
          if (i % 2 === 0) { var am = (-90 + i * 30) * Math.PI / 180; s += tx(cx + (rOut + 16) * Math.cos(am), cy + (rOut + 16) * Math.sin(am) + 4, w[0], ST, 'middle', true, 9); }
        });
        s += pfxCls(cx, cy, rIn, '#ffffff', ST, 1.4);
        var ptAt = function (idx) { var a = (-90 + idx * 30) * Math.PI / 180; return [cx + (rIn - 3) * Math.cos(a), cy + (rIn - 3) * Math.sin(a)]; };
        if (harmony === 'comp') { var pA = ptAt(0), pB = ptAt(6); s += ln(pA[0], pA[1], pB[0], pB[1], ST, 2.2, '3 2'); }
        else if (harmony === 'analog') { [11, 0, 1].forEach(function (k) { var p = ptAt(k); s += pfxCls(p[0], p[1], 3.5, ST, 'none', 0); }); }
        else if (harmony === 'warmcool') s += ln(cx - rOut - 10, cy - 28, cx + rOut + 10, cy + 28, '#334155', 1.8, '5 3');
        else if (harmony !== 'none') { var p0 = ptAt(0), p4 = ptAt(4), p8 = ptAt(8); s += pfxPoly(f2(p0[0]) + ',' + f2(p0[1]) + ' ' + f2(p4[0]) + ',' + f2(p4[1]) + ' ' + f2(p8[0]) + ',' + f2(p8[1]), 'rgba(15,23,42,.08)', ST, 1.8); }
        var hTitle = harmony === 'comp' ? 'رنگ‌های مکمل (روبه‌رو در چرخهٔ ایتن)' : harmony === 'analog' ? 'رنگ‌های هم‌جوار (هماهنگ)' : harmony === 'warmcool' ? 'تفکیک رنگ‌های گرم و سرد' : 'چرخهٔ ۱۲ رنگ ایتن — مثلث رنگ‌های اصلی (زرد، قرمز، آبی)';
        s += rc(18, 238, 324, 28, '#f8fafc', '#cbd5e1', 1.2, 7) + tx(180, 256, hTitle, '#1e293b', 'middle', true, 10);
        return wrap(s);
      }

      var bass = xs('clef', 'treble').toLowerCase() === 'bass', timeSig = xs('time', '4/4'), showNames = xs('label', '1') !== '0';
      var durs = xs('dur', '').split(/[,،;\s]+/).map(function (z) { return z.trim().toLowerCase(); }).filter(Boolean);
      var rawToks = xs('notes', bass ? '48,50,52,53,55' : '60,62,64,65,67').split(/[,،;\s]+/).map(function (z) { return z.trim(); }).filter(Boolean).slice(0, 8);
      // V322 — نت خارج از محدوده مهار می‌شود
      var stfNotes = rawToks.map(function (tk) { var m = parseInt(faEn(tk.replace(/[#b♯♭]/g, '')), 10); return isNaN(m) ? 60 : m; });
      stfNotes = stfNotes.map(function (nst) { return clamp(nst, 53, 74); });
      if (!rawToks.length) { rawToks = ['60', '62', '64', '65', '67']; stfNotes = [60, 62, 64, 65, 67]; }

      var yTop = 82, gap = 14, yBot = yTop + 4 * gap;
      for (var li = 0; li < 5; li++) s += ln(28, yTop + li * gap, 336, yTop + li * gap, ST, 1.4);
      s += ln(28, yTop, 28, yBot, ST, 2.2) + ln(332, yTop, 332, yBot, ST, 1.2) + ln(336, yTop, 336, yBot, ST, 2.6);
      s += tx(46, yBot - 6, bass ? '𝄢' : '𝄞', ACC, 'middle', true, bass ? 34 : 42);
      if (timeSig && timeSig.indexOf('/') > 0) {
        var ts = timeSig.split('/');
        s += tx(74, yTop + 22, ts[0].trim(), ST, 'middle', true, 16) + tx(74, yBot - 4, ts[1].trim(), ST, 'middle', true, 16);
      }

      var degMap = {0:[0,'دو',''],1:[0,'دو','♯'],2:[1,'ر',''],3:[2,'می','♭'],4:[2,'می',''],5:[3,'فا',''],6:[3,'فا','♯'],7:[4,'سل',''],8:[5,'لا','♭'],9:[5,'لا',''],10:[6,'سی','♭'],11:[6,'سی','']};
      var baseStep = bass ? 30 : 38, xStart = 104, stepX = (320 - xStart) / Math.max(1, rawToks.length);
      var faLabels = [];
      rawToks.forEach(function (tok, idx) {
        var midi = bass ? clamp(parseInt(faEn(tok.replace(/[#b♯♭]/g, '')), 10) || 48, 41, 62) : stfNotes[idx];
        var info = degMap[((midi % 12) + 12) % 12] || [0, 'دو', ''];
        var acc = (tok.indexOf('#') >= 0 || tok.indexOf('♯') >= 0) ? '♯' : ((tok.indexOf('b') >= 0 || tok.indexOf('♭') >= 0) ? '♭' : info[2]);
        var diat = Math.floor(midi / 12) * 7 + info[0], halfSteps = diat - baseStep;
        var ny = yBot - halfSteps * (gap / 2), nx = xStart + (idx + 0.5) * stepX;
        if (halfSteps <= -2) for (var hs = -2; hs >= halfSteps; hs -= 2) s += ln(nx - 12, yBot - hs * (gap / 2), nx + 12, yBot - hs * (gap / 2), ST, 1.4);
        if (halfSteps >= 10) for (var hs2 = 10; hs2 <= halfSteps; hs2 += 2) s += ln(nx - 12, yBot - hs2 * (gap / 2), nx + 12, yBot - hs2 * (gap / 2), ST, 1.4);
        var dur = durs[idx] || 'q', hollow = dur === 'w' || dur === 'h';
        s += '<ellipse cx="' + f2(nx) + '" cy="' + f2(ny) + '" rx="7.2" ry="5.2" fill="' + (hollow ? '#ffffff' : ST) + '" stroke="' + ST + '" stroke-width="1.8"/>';
        if (dur !== 'w') {
          var stemUp = ny >= yTop + 2 * gap, sx = stemUp ? nx + 6.5 : nx - 6.5, sy2 = stemUp ? ny - 34 : ny + 34;
          s += ln(sx, ny, sx, sy2, ST, 1.8);
          if (dur === 'e') s += pfxPath('M ' + f2(sx) + ' ' + f2(sy2) + ' Q ' + f2(sx + 10) + ' ' + f2(sy2 + (stemUp ? 8 : -8)) + ' ' + f2(sx + 6) + ' ' + f2(sy2 + (stemUp ? 16 : -16)), 'none', ST, 1.8);
        }
        if (acc) s += tx(nx - 15, ny + 4, acc, PT, 'middle', true, 12);
        faLabels.push(info[1] + acc);
        if (showNames) s += tx(nx, 194, info[1] + acc, ACC, 'middle', true, 10);
      });
      if (showNames) {
        s += rc(20, 232, 320, 30, '#f8fafc', '#cbd5e1', 1.2, 7);
        s += tx(180, 251, 'کلید ' + (bass ? 'فا' : 'سل') + ' — نت‌ها: ' + faLabels.join(' ، '), '#1e293b', 'middle', true, 10.5);
      }
      return wrap(s);
    }
    if (t === 'axkmap') {
      var kmVars = clamp(Math.floor(num(X.vars, 3)), 2, 4), kmOnes = String(xs('ones', '0,1,3,5')).split(/[,،;]/).map(function (q10) { return parseInt(faEn(q10.trim()), 10); }).filter(function (q10) { return isFinite(q10); });
      var kmGroups = String(xs('groups', '')).split(/[;؛]/).filter(Boolean);
      var kmRowsN = kmVars >= 4 ? 4 : 2, kmColsN = kmVars === 2 ? 2 : 4;
      var kmRowLbl = kmVars === 2 ? ['0', '1'] : kmVars === 3 ? ['0', '1'] : ['00', '01', '11', '10'];
      var kmColLbl = kmVars === 2 ? ['0', '1'] : ['00', '01', '11', '10'];
      var kmCell = Math.min(56, 220 / Math.max(kmRowsN, kmColsN)), kmX0 = 180 - kmCell * kmColsN / 2 + 14, kmY0 = 56;
      var kmVal = function (r5, c5) {
        if (kmVars === 2) return r5 * 2 + c5;
        if (kmVars === 3) return (r5) * 4 + [0, 1, 3, 2][c5];
        return [0, 1, 3, 2][r5] * 4 + [0, 1, 3, 2][c5];
      };
      for (var kc2 = 0; kc2 < kmColsN; kc2++) s += rc(kmX0 + kc2 * kmCell, kmY0 - 26, kmCell, 26, 'rgba(108,99,245,.14)', ST, 1.2) + tx(kmX0 + kc2 * kmCell + kmCell / 2, kmY0 - 8, kmColLbl[kc2], ACC, 'middle', true, 11);
      for (var kr2 = 0; kr2 < kmRowsN; kr2++) {
        s += rc(kmX0 - 34, kmY0 + kr2 * kmCell, 34, kmCell, 'rgba(108,99,245,.14)', ST, 1.2) + tx(kmX0 - 17, kmY0 + kr2 * kmCell + kmCell / 2 + 4, kmRowLbl[kr2], ACC, 'middle', true, 11);
        for (var kc3 = 0; kc3 < kmColsN; kc3++) {
          var idx3 = kmVal(kr2, kc3), on = kmOnes.indexOf(idx3) >= 0;
          s += rc(kmX0 + kc3 * kmCell, kmY0 + kr2 * kmCell, kmCell, kmCell, on ? 'rgba(39,196,168,.26)' : '#ffffff', ST, 1.4) + tx(kmX0 + kc3 * kmCell + kmCell / 2, kmY0 + kr2 * kmCell + kmCell / 2 + 6, on ? '۱' : '۰', on ? '#00796b' : '#9aa7ba', 'middle', true, 14);
        }
      }
      kmGroups.forEach(function (grp2, gi3) {
        var cl2 = grp2.split(/[,،]/).map(function (q11) { return parseInt(faEn(q11.trim()), 10); }).filter(function (q11) { return isFinite(q11); });
        if (!cl2.length) return;
        var pos2 = cl2.map(function (v5) {
          for (var r6 = 0; r6 < kmRowsN; r6++) for (var c6 = 0; c6 < kmColsN; c6++) if (kmVal(r6, c6) === v5) return [r6, c6];
          return null;
        }).filter(Boolean);
        if (!pos2.length) return;
        var minR = Math.min.apply(null, pos2.map(function (p3) { return p3[0]; })), maxR = Math.max.apply(null, pos2.map(function (p3) { return p3[0]; }));
        var minC = Math.min.apply(null, pos2.map(function (p3) { return p3[1]; })), maxC = Math.max.apply(null, pos2.map(function (p3) { return p3[1]; }));
        var gx2 = kmX0 + minC * kmCell + 3 + gi3 * 2, gy2 = kmY0 + minR * kmCell + 3 + gi3 * 2;
        var gw2 = (maxC - minC + 1) * kmCell - 6, gh2 = (maxR - minR + 1) * kmCell - 6;
        s += '<rect x="' + f2(gx2) + '" y="' + f2(gy2) + '" width="' + f2(gw2) + '" height="' + f2(gh2) + '" rx="10" fill="none" stroke="' + (gi3 % 2 ? PT : ACC) + '" stroke-width="2" stroke-dasharray="6 4"/>';
      });
      s += tx(180, kmY0 + kmRowsN * kmCell + 26, 'جدول کارنو — ' + nm(kmVars) + ' متغیره', MU, 'middle', true, 12);
      return wrap(s);
    }
    if (t === 'axgate') {
      var gtKind = xs('kind', 'and').toLowerCase(), gtA = xs('a', '1') === '1' ? 1 : 0, gtB = xs('b', '0') === '1' ? 1 : 0;
      var gtOut = gtKind === 'and' || gtKind === 'nand' ? (gtA && gtB) : gtKind === 'or' || gtKind === 'nor' ? (gtA || gtB) : gtKind === 'xor' ? (gtA !== gtB) : (1 - gtA);
      if (gtKind === 'nand') gtOut = 1 - gtOut;
      if (gtKind === 'nor') gtOut = 1 - gtOut;
      var gtX = 118, gtY = 92, gtW = 96, gtH = 84;
      if (gtKind === 'not') {
        s += pfxPoly(f2(gtX) + ',' + f2(gtY) + ' ' + f2(gtX) + ',' + f2(gtY + gtH) + ' ' + f2(gtX + gtW) + ',' + f2(gtY + gtH / 2), 'rgba(108,99,245,.14)', ST, 2) + pfxCls(gtX + gtW + 8, gtY + gtH / 2, 8, '#ffffff', ST, 2);
        s += lnr(gtX - 52, gtY + gtH / 2, gtX, gtY + gtH / 2, ST, 2) + tx(gtX - 30, gtY + gtH / 2 - 10, 'A = ' + nm(gtA), ACC, 'middle', true, 12);
        s += lnr(gtX + gtW + 16, gtY + gtH / 2, gtX + gtW + 56, gtY + gtH / 2, PT, 2) + tx(gtX + gtW + 40, gtY + gtH / 2 - 10, 'Y = ' + nm(gtOut), PT, 'middle', true, 12);
      } else {
        var gShield = 'M ' + f2(gtX) + ',' + f2(gtY) + ' L ' + f2(gtX + gtW * 0.55) + ',' + f2(gtY) + ' Q ' + f2(gtX + gtW) + ',' + f2(gtY + gtH / 2) + ' ' + f2(gtX + gtW * 0.55) + ',' + f2(gtY + gtH) + ' L ' + f2(gtX) + ',' + f2(gtY + gtH) + ' Z';
        if (gtKind === 'and' || gtKind === 'nand') s += pfxPath('M ' + f2(gtX) + ',' + f2(gtY) + ' L ' + f2(gtX + gtW * 0.5) + ',' + f2(gtY) + ' A ' + f2(gtH / 2) + ',' + f2(gtH / 2) + ' 0 0 1 ' + f2(gtX + gtW * 0.5) + ',' + f2(gtY + gtH) + ' L ' + f2(gtX) + ',' + f2(gtY + gtH) + ' Z', 'rgba(108,99,245,.14)', ST, 2);
        else s += pfxPath(gShield, 'rgba(108,99,245,.14)', ST, 2);
        if (gtKind === 'xor') s += pfxPath('M ' + f2(gtX - 12) + ',' + f2(gtY) + ' Q ' + f2(gtX + 22) + ',' + f2(gtY + gtH / 2) + ' ' + f2(gtX - 12) + ',' + f2(gtY + gtH), 'none', ST, 2);
        if (gtKind === 'nand' || gtKind === 'nor') s += pfxCls(gtX + gtW + 8, gtY + gtH / 2, 8, '#ffffff', ST, 2);
        s += lnr(gtX - 56, gtY + 22, gtX + 4, gtY + 22, ST, 2) + tx(gtX - 40, gtY + 12, 'A = ' + nm(gtA), ACC, 'middle', true, 12);
        s += lnr(gtX - 56, gtY + 62, gtX + 4, gtY + 62, ST, 2) + tx(gtX - 40, gtY + 52, 'B = ' + nm(gtB), ACC, 'middle', true, 12);
        var gOutX = gtX + gtW + ((gtKind === 'nand' || gtKind === 'nor') ? 16 : 6);
        s += lnr(gOutX, gtY + gtH / 2, gOutX + 52, gtY + gtH / 2, PT, 2) + tx(gOutX + 26, gtY + gtH / 2 - 10, 'Y = ' + nm(gtOut), PT, 'middle', true, 12);
      }
      var gtNames = { and: 'AND — و', or: 'OR — یا', not: 'NOT — نقیض', nand: 'NAND — نقیض و', nor: 'NOR — نقیض یا', xor: 'XOR — یا انحصاری' };
      s += tx(180, 224, gtNames[gtKind] || 'گیت منطقی', ST, 'middle', true, 14) + tx(180, 250, 'Y = ' + nm(gtOut), gtOut ? '#1b7f4c' : PT, 'middle', true, 13);
      return wrap(s);
    }
    if (t === 'axdfa') {
      var dfStates = String(xs('states', 'q0,q1,q2')).split(/[,،]/).map(function (q12) { return q12.trim(); }).filter(Boolean).slice(0, 4);
      if (!dfStates.length) dfStates = ['q0', 'q1', 'q2'];
      var dfStart = xs('start', 'q0'), dfAcc = String(xs('accept', 'q2')).split(/[,،]/).map(function (q12) { return q12.trim(); });
      var dfEdges = String(xs('edges', 'q0-q1:a;q1-q2:b;q1-q1:a')).split(/[;؛]/).map(function (q12) { return q12.trim(); }).filter(Boolean);
      var dfCy = 132, dfR = 30, dfStep = Math.min(96, 280 / Math.max(1, dfStates.length));
      var dfPos = {};
      dfStates.forEach(function (st2, i6) { dfPos[st2] = [180 + (i6 - (dfStates.length - 1) / 2) * dfStep, dfCy]; });
      s += pfxArrow(dfPos[dfStart] ? dfPos[dfStart][0] - dfR - 34 : 30, dfCy, dfPos[dfStart] ? dfPos[dfStart][0] - dfR : 60, dfCy, ST, 1.6, 8);
      dfStates.forEach(function (st2) {
        var pp3 = dfPos[st2];
        s += pfxCls(pp3[0], pp3[1], dfR, dfAcc.indexOf(st2) >= 0 ? 'rgba(39,196,168,.18)' : '#ffffff', ST, 2);
        if (dfAcc.indexOf(st2) >= 0) s += pfxCls(pp3[0], pp3[1], dfR - 6, 'none', ST, 1.4);
        s += tx(pp3[0], pp3[1] + 5, st2, ST, 'middle', true, 12);
      });
      dfEdges.forEach(function (ed) {
        var mm = ed.split(/[:\-—]/).map(function (z3) { return z3.trim(); }).filter(Boolean);
        if (mm.length < 3) return;
        var from = mm[0], to = mm[1], lab3 = mm.slice(2).join(':');
        if (!dfPos[from] || !dfPos[to]) return;
        var p1 = dfPos[from], p2 = dfPos[to];
        if (from === to) {
          /* V326 — دو سر حلقه دقیقاً روی محیط: ±۳۰ درجه از بالای گره */
          var dfa = -Math.PI / 2, dfo = 0.52, dfr2 = 20;
          var dfl1 = [p1[0] + dfR * Math.cos(dfa - dfo), p1[1] + dfR * Math.sin(dfa - dfo)];
          var dfl2 = [p1[0] + dfR * Math.cos(dfa + dfo), p1[1] + dfR * Math.sin(dfa + dfo)];
          s += pfxPath('M ' + f2(dfl1[0]) + ',' + f2(dfl1[1]) + ' A ' + dfr2 + ' ' + dfr2 + ' 0 1 1 ' + f2(dfl2[0]) + ',' + f2(dfl2[1]), 'none', ACC, 1.6);
          s += tx(p1[0], p1[1] - dfR - 20, lab3, ACC, 'middle', true, 10);
          return;
        }
        var dxx = p2[0] - p1[0], dyy = p2[1] - p1[1], dl = Math.sqrt(dxx * dxx + dyy * dyy) || 1;
        var sx3 = p1[0] + dxx / dl * dfR, sy3 = p1[1] + dyy / dl * dfR, ex2 = p2[0] - dxx / dl * dfR, ey2 = p2[1] - dyy / dl * dfR;
        s += pfxArrow(sx3, sy3, ex2, ey2, ACC, 1.6, 8) + tx((sx3 + ex2) / 2, (sy3 + ey2) / 2 - 8, lab3, '#0d47a1', 'middle', true, 10);
      });
      s += tx(180, 238, 'ماشین حالت متناهی (DFA)', ST, 'middle', true, 13) + tx(180, 260, 'حالت شروع: ' + dfStart + ' • حالت پایانی: ' + dfAcc.join(' , '), MU, 'middle', false, 11);
      return wrap(s);
    }
    if (t === 'axbst') {
      var bsVals = ilist('vals', '50,30,70,20,40,60,80').slice(0, 12), bsMode = xs('mode', 'bst').toLowerCase();
      if (!bsVals.length) bsVals = [50, 30, 70, 20, 40, 60, 80];
      var bsTree = null;
      var bsInsert = function (node, v6) {
        if (!node) return { v: v6, l: null, r: null };
        if (v6 < node.v) node.l = bsInsert(node.l, v6); else node.r = bsInsert(node.r, v6);
        return node;
      };
      if (bsMode === 'heap') {
        var bsBuild = function (i7) { if (i7 >= bsVals.length) return null; return { v: bsVals[i7], l: bsBuild(i7 * 2 + 1), r: bsBuild(i7 * 2 + 2) }; };
        bsTree = bsBuild(0);
      } else bsVals.forEach(function (v6) { bsTree = bsInsert(bsTree, v6); });
      var bsMaxD = 0, bsWalk = function (node, d2) { if (!node) return; bsMaxD = Math.max(bsMaxD, d2); bsWalk(node.l, d2 + 1); bsWalk(node.r, d2 + 1); };
      bsWalk(bsTree, 0);
      var bsRowH = Math.min(46, 190 / Math.max(1, bsMaxD + 1));
      var bsDraw = function (node, x6, d3, span) {
        if (!node) return;
        var y6 = 40 + d3 * bsRowH;
        var bsR = 15;
        var bsSeg = function (ax, ay, bx, by) {
          var dx = bx - ax, dy = by - ay, len = Math.hypot(dx, dy) || 1;
          return [ax + dx / len * (bsR + 1), ay + dy / len * (bsR + 1),
                  bx - dx / len * (bsR + 1), by - dy / len * (bsR + 1)];
        };
        if (node.l) { var sl = bsSeg(x6, y6, x6 - span, y6 + bsRowH); s += lnr(sl[0], sl[1], sl[2], sl[3], ST, 1.4); bsDraw(node.l, x6 - span, d3 + 1, span / 2); }
        if (node.r) { var sr = bsSeg(x6, y6, x6 + span, y6 + bsRowH); s += lnr(sr[0], sr[1], sr[2], sr[3], ST, 1.4); bsDraw(node.r, x6 + span, d3 + 1, span / 2); }
        s += pfxCls(x6, y6, bsR, 'rgba(108,99,245,.16)', ACC, 1.8) + tx(x6, y6 + 4, String(node.v), ST, 'middle', true, 10.5);
      };
      bsDraw(bsTree, 180, 0, Math.min(84, 210 / Math.pow(2, Math.max(0, bsMaxD - 1))));
      s += tx(180, 258, bsMode === 'heap' ? 'درخت هرم (آرایه‌ای)' : 'درخت دودویی جستجو (BST)', MU, 'middle', true, 12);
      return wrap(s);
    }
    if (t === 'axbode') {
      var bdKind = xs('kind', 'bode').toLowerCase();
      if (bdKind === 'nyquist') {
        var nyP = String(xs('poles', '1,10')).split(/[,،]/).map(function (q13) { return parseFloat(faEn(q13.trim())) || 1; }).slice(0, 2);
        var nyPts = '';
        for (var wi = 0; wi <= 120; wi++) {
          var w0 = Math.pow(10, -2 + 4 * wi / 120), re2 = 1, im2 = 0;
          nyP.forEach(function (pl2) { var dr = 1, di2 = w0 / pl2; var nr = re2 * dr + im2 * di2, ni2 = im2 * dr - re2 * di2, d3 = dr * dr + di2 * di2; re2 = nr / d3; im2 = ni2 / d3; });
          nyPts += f2(180 + re2 * 110) + ',' + f2(140 - im2 * 110) + ' ';
        }
        s += lnr(30, 140, 330, 140, ST, 1.4) + lnr(180, 20, 180, 260, ST, 1.4);
        s += '<polyline points="' + nyPts.trim() + '" fill="none" stroke="' + PT + '" stroke-width="2"/>';
        s += tx(316, 156, 'Re', MU, 'middle', true, 11) + tx(196, 30, 'Im', MU, 'middle', true, 11);
        s += tx(180, 272, 'نمودار نایکوئیست', MU, 'middle', true, 12);
        return wrap(s);
      }
      var bdGain = num(X.gain, 20), bdPoles = String(xs('poles', '1,10')).split(/[,،]/).map(function (q14) { return Math.max(0.1, parseFloat(faEn(q14.trim())) || 1); }).slice(0, 3);
      var bdMag = function (w3) {
        var mdb = bdGain;
        bdPoles.forEach(function (pl3) { mdb -= 10 * Math.log(1 + Math.pow(w3 / pl3, 2)) / Math.LN10; });
        return mdb;
      };
      var bdPh = function (w3) {
        var ph2 = 0;
        bdPoles.forEach(function (pl3) { ph2 -= Math.atan(w3 / pl3) * 180 / Math.PI; });
        return ph2;
      };
      var bPlot = function (y0, h2, fn3, lab4, col4) {
        var out2 = rc(40, y0, 292, h2, '#ffffff', ST, 1.2, 3);
        for (var mi3 = 0; mi3 <= 3; mi3++) { var yy7 = y0 + (h2 * mi3 / 3); out2 += lnr(40, yy7, 332, yy7, '#e8eef6', 1); }
        var pts5 = '';
        for (var wi2 = 0; wi2 <= 120; wi2++) {
          var ww = Math.pow(10, -1 + 3 * wi2 / 120);
          pts5 += f2(40 + 292 * wi2 / 120) + ',' + f2(y0 + h2 / 2 - fn3(ww)) + ' ';
        }
        return out2 + '<polyline points="' + pts5.trim() + '" fill="none" stroke="' + col4 + '" stroke-width="2"/>' + tx(44, y0 + 13, lab4, col4, 'start', true, 10);
      };
      s += bPlot(34, 92, function (w4) { return clamp(bdMag(w4) * 0.9, -46, 46); }, 'اندازه (dB)', ACC);
      s += bPlot(142, 92, function (w4) { return clamp(bdPh(w4) * 0.5, -46, 46); }, 'فاز (°)', '#00796b');
      s += tx(180, 250, 'نمودار بود — بهره ' + nm(bdGain) + ' dB ، قطب‌ها: ' + bdPoles.join(' , '), MU, 'middle', true, 11);
      return wrap(s);
    }
    if (t === 'axbeam') {
      var bmLen = Math.max(2, num(X.len, 6)), bmP = num(X.p, 10), bmA = clamp(num(X.a, 2), 0.2, bmLen - 0.2), bmQ = num(X.q, 2), bmKind = xs('load', 'point').toLowerCase();
      var bmX0 = 46, bmX1 = 320, bmSX = function (v7) { return bmX0 + (v7 / bmLen) * (bmX1 - bmX0); }, bmY = 74;
      s += lnr(bmX0, bmY, bmX1, bmY, ST, 6) + pfxPoly(f2(bmX0) + ',' + f2(bmY) + ' ' + f2(bmX0 + 12) + ',' + f2(bmY + 16) + ' ' + f2(bmX0 - 12) + ',' + f2(bmY + 16), '#ffffff', ST, 1.6);
      s += pfxCls(bmX1, bmY + 9, 6, '#ffffff', ST, 1.6) + lnr(bmX1 - 10, bmY + 20, bmX1 + 10, bmY + 20, ST, 1.6);
      var bmR1 = bmKind === 'udl' ? bmQ * bmLen / 2 : bmP * (bmLen - bmA) / bmLen, bmR2 = bmKind === 'udl' ? bmQ * bmLen / 2 : bmP * bmA / bmLen;
      if (bmKind === 'udl') {
        for (var ui2 = 0; ui2 <= bmLen * 4; ui2++) { var ux2 = bmSX(ui2 / 4); s += pfxArrow(ux2, bmY - 40, ux2, bmY - 6, ACC, 1.2, 5); }
        s += lnr(bmX0, bmY - 40, bmX1, bmY - 40, ACC, 1.6) + tx(180, bmY - 48, 'بار گسترده ' + nm(bmQ) + ' N/m', ACC, 'middle', true, 11);
      } else {
        s += pfxArrow(bmSX(bmA), bmY - 46, bmSX(bmA), bmY - 6, PT, 2.4, 9) + tx(bmSX(bmA), bmY - 54, nm(bmP) + ' N', PT, 'middle', true, 11);
        s += tx(bmSX(bmA), bmY + 34, nm(bmA) + ' m', MU, 'middle', false, 10);
      }
      s += pfxArrow(bmX0, bmY + 34, bmX0, bmY + 8, '#00796b', 2, 7) + tx(bmX0 + 2, bmY + 46, 'R₁=' + nm(Math.round(bmR1 * 10) / 10), '#00796b', 'start', true, 10);
      s += pfxArrow(bmX1, bmY + 34, bmX1, bmY + 8, '#00796b', 2, 7) + tx(bmX1 - 2, bmY + 46, 'R₂=' + nm(Math.round(bmR2 * 10) / 10), '#00796b', 'end', true, 10);
      var bmSf = function (v7) { return bmKind === 'udl' ? bmR1 - bmQ * v7 : bmR1 - (v7 >= bmA ? bmP : 0); };
      var bmMaxS = Math.max(Math.abs(bmR1), Math.abs(bmR1 - (bmKind === 'udl' ? bmQ * bmLen : bmP))) || 1;
      var bmSfY = 152, bmSfH = 44;
      s += lnr(bmX0, bmSfY, bmX1, bmSfY, ST, 1.4) + tx(44, bmSfY - 6, 'V', MU, 'end', true, 11);
      var sfPts = '';
      for (var bi3 = 0; bi3 <= 100; bi3++) { var bv = bmLen * bi3 / 100; sfPts += f2(bmSX(bv)) + ',' + f2(bmSfY - bmSf(bv) / bmMaxS * bmSfH) + ' '; }
      s += '<polyline points="' + sfPts.trim() + '" fill="none" stroke="' + ACC + '" stroke-width="2"/>';
      var bmBmY = 232, bmBmH = 34;
      var bmBm = function (v7) {
        if (bmKind === 'udl') return bmR1 * v7 - bmQ * v7 * v7 / 2;
        return v7 <= bmA ? bmR1 * v7 : bmR2 * (bmLen - v7);
      };
      var bmMaxB = Math.max(1, bmKind === 'udl' ? bmQ * bmLen * bmLen / 8 : bmP * bmA * (bmLen - bmA) / bmLen);
      s += lnr(bmX0, bmBmY, bmX1, bmBmY, ST, 1.4) + tx(44, bmBmY - 6, 'M', MU, 'end', true, 11);
      var bmPts2 = '';
      for (var bi4 = 0; bi4 <= 100; bi4++) { var bv2 = bmLen * bi4 / 100; bmPts2 += f2(bmSX(bv2)) + ',' + f2(bmBmY + bmBm(bv2) / bmMaxB * bmBmH) + ' '; }
      s += '<polyline points="' + bmPts2.trim() + '" fill="none" stroke="' + PT + '" stroke-width="2"/>';
      s += tx(180, 274, 'تیر دو سر مفصل — نمودار برش (V) و گشتاور (M)', MU, 'middle', true, 11);
      return wrap(s);
    }
    if (t === 'axmohr') {
      var mhSx = num(X.sx, 80), mhSy = num(X.sy, 20), mhT = num(X.txy, 30);
      var mhC = (mhSx + mhSy) / 2, mhR = Math.sqrt(Math.pow((mhSx - mhSy) / 2, 2) + mhT * mhT);
      var mhMax = Math.max(mhC + mhR, Math.abs(mhC - mhR), Math.abs(mhT) * 1.3, 1), mhSc = 108 / mhMax;
      var mhCx = 180, mhCy = 138, mhPx = function (v8) { return mhCx + v8 * mhSc; }, mhPy = function (v8) { return mhCy - v8 * mhSc; };
      s += lnr(24, mhCy, 336, mhCy, ST, 1.4) + pfxArrow(324, mhCy, 340, mhCy, ST, 1.4, 7);
      s += lnr(mhCx, 258, mhCx, 20, ST, 1.4) + pfxArrow(mhCx, 32, mhCx, 16, ST, 1.4, 7);
      s += tx(334, mhCy + 16, 'σ', MU, 'middle', true, 12) + tx(mhCx + 14, 26, 'τ', MU, 'middle', true, 12);
      s += pfxCls(mhCx, mhCy, 4, ST, 'none', 0);
      s += pfxCls(mhPx(mhC), mhCy, mhR * mhSc, 'rgba(108,99,245,.10)', ACC, 2);
      s += pfxCls(mhPx(mhSx), mhPy(mhT), 5, PT, 'none', 0) + pfxCls(mhPx(mhSy), mhPy(-mhT), 5, GRN, 'none', 0);
      s += lnr(mhPx(mhSx), mhCy, mhPx(mhSx), mhPy(mhT), '#c8d2e0', 1, '4 3') + lnr(mhPx(mhSy), mhCy, mhPx(mhSy), mhPy(-mhT), '#c8d2e0', 1, '4 3');
      s += lnr(mhPx(mhC), mhCy, mhPx(mhSx), mhCy, ACC, 1.4) + tx(mhPx((mhC + mhSx) / 2), mhCy + 16, 'R', ACC, 'middle', true, 11);
      var mhSgn = function (v0) { v0 = Math.round(v0 * 10) / 10; return (v0 < 0 ? '−' : '') + Math.abs(v0); };
      s += tx(mhPx(mhC - mhR) - 6, mhCy - 10, 'σ₂ = ' + mhSgn(mhC - mhR), '#00796b', 'end', true, 10);
      s += tx(mhPx(mhC + mhR) + 6, mhCy - 10, 'σ₁ = ' + mhSgn(mhC + mhR), '#00796b', 'start', true, 10);
      s += tx(180, 272, 'دایرهٔ مور — σx = ' + mhSgn(mhSx) + ' ، σy = ' + mhSgn(mhSy) + ' ، τxy = ' + mhSgn(mhT), MU, 'middle', true, 11);
      return wrap(s);
    }
    if (t === 'axpv') {
      var pvKind = xs('kind', 'carnot').toLowerCase();
      var pvPath = '';
      if (pvKind === 'otto' || pvKind === 'diesel') {
        pvPath = 'M 96 96 L 252 96 L 150 214 L 96 214 Z';
        s += pfxPath(pvPath, 'rgba(108,99,245,.10)', ACC, 2.2);
        s += tx(174, 84, 'احتراق (حجم ثابت)', PT, 'middle', true, 10) + tx(150, 232, 'انبساط', MU, 'middle', false, 10);
      } else if (pvKind === 'rankine') {
        s += pfxPath('M 96 96 L 252 96 L 252 216 L 96 216 Z', 'rgba(108,99,245,.10)', ACC, 2.2);
        s += tx(174, 84, 'گرم شدن', PT, 'middle', true, 10);
      } else {
        s += pfxPath('M 104 78 C 176 54 250 92 262 150 C 238 206 160 226 104 196 C 66 170 70 110 104 78 Z', 'rgba(108,99,245,.10)', ACC, 2.2);
        s += tx(180, 52, 'هم‌دما', PT, 'middle', true, 10);
      }
      s += lnr(40, 226, 328, 226, ST, 1.6) + pfxArrow(316, 226, 332, 226, ST, 1.6, 7);
      s += lnr(66, 226, 66, 30, ST, 1.6) + pfxArrow(66, 42, 66, 24, ST, 1.6, 7);
      s += tx(320, 242, 'V', MU, 'middle', true, 12) + tx(54, 34, 'P', MU, 'middle', true, 12);
      var pvNames = { carnot: 'چرخهٔ کارنو', otto: 'چرخهٔ اتو', diesel: 'چرخهٔ دیزل', rankine: 'چرخهٔ رانکین' };
      s += tx(180, 258, pvNames[pvKind] || 'چرخهٔ ترمودینامیکی', ST, 'middle', true, 13);
      return wrap(s);
    }
    if (t === 'axpert') {
      var ptActs = String(xs('acts', 'A:1-2:3;B:2-3:2;C:2-4:4;D:3-4:1')).split(/[;؛]/).map(function (q15) { return q15.trim(); }).filter(Boolean);
      var ptList = [];
      ptActs.forEach(function (ac) {
        var mm2 = ac.split(':').map(function (z4) { return z4.trim(); });
        if (mm2.length < 3) return;
        var nd = mm2[1].split(/[-—]/).map(function (z4) { return z4.trim(); });
        ptList.push({ name: mm2[0], from: nd[0], to: nd[1] || nd[0], dur: Math.max(0, parseFloat(faEn(mm2[2])) || 0) });
      });
      if (!ptList.length) ptList = [{ name: 'A', from: '1', to: '2', dur: 3 }];
      var ptNodes = {};
      ptList.forEach(function (a5) { ptNodes[a5.from] = 1; ptNodes[a5.to] = 1; });
      var ptOrder = Object.keys(ptNodes);
      var ptDepth = {}; ptOrder.forEach(function (k2) { ptDepth[k2] = 0; });
      for (var pd = 0; pd < 6; pd++) ptList.forEach(function (a5) { ptDepth[a5.to] = Math.max(ptDepth[a5.to] || 0, (ptDepth[a5.from] || 0) + 1); });
      var ptMaxD = Math.max.apply(null, ptOrder.map(function (k2) { return ptDepth[k2]; }));
      var ptPos = {};
      ptOrder.forEach(function (k2) {
        var same = ptOrder.filter(function (o2) { return ptDepth[o2] === ptDepth[k2]; }), idx5 = same.indexOf(k2);
        ptPos[k2] = [48 + (ptMaxD ? ptDepth[k2] / Math.max(1, ptMaxD) : 0) * 260, 54 + idx5 * Math.min(58, 160 / Math.max(1, same.length))];
      });
      ptList.forEach(function (a5) {
        var p1b = ptPos[a5.from], p2b = ptPos[a5.to];
        if (!p1b || !p2b) return;
        var d5 = Math.sqrt(Math.pow(p2b[0] - p1b[0], 2) + Math.pow(p2b[1] - p1b[1], 2)) || 1;
        var sxx = p1b[0] + (p2b[0] - p1b[0]) / d5 * 15, syy = p1b[1] + (p2b[1] - p1b[1]) / d5 * 15;
        var exx = p2b[0] - (p2b[0] - p1b[0]) / d5 * 15, eyy = p2b[1] - (p2b[1] - p1b[1]) / d5 * 15;
        var wdx = exx - sxx, wdy = eyy - syy, wl = Math.hypot(wdx, wdy) || 1;
        var wnx = -wdy / wl, wny = wdx / wl;
        if (wny > 0) { wnx = -wnx; wny = -wny; }   /* همیشه بالای خط */
        /* V326 — فعالیتی که از روی یک گره می‌پرد (مثل C: ۲→۴) با کمان بالای ردیف کشیده می‌شود
           تا خط و برچسبش روی گرهٔ میانی نیفتد */
        var jump = null;
        if (Math.abs(wdy) < 6) {
          ptOrder.forEach(function (k3) {
            var pp5 = ptPos[k3];
            if (!pp5 || Math.abs(pp5[1] - p1b[1]) > 12) return;
            if (pp5[0] > Math.min(p1b[0], p2b[0]) + 14 && pp5[0] < Math.max(p1b[0], p2b[0]) - 14) jump = pp5;
          });
        }
        if (jump) {
          var jmx = (sxx + exx) / 2, jmy = Math.max(syy, eyy) + 44;
          var apy = (syy + 2 * jmy + eyy) / 4;   /* پایین‌ترین نقطهٔ کمان */
          var tdx = exx - jmx, tdy = eyy - jmy, tl = Math.hypot(tdx, tdy) || 1;
          var tux = tdx / tl, tuy = tdy / tl, tbx = exx - tux * 8, tby = eyy - tuy * 8;
          s += v319path('M ' + f2(sxx) + ' ' + f2(syy) + ' Q ' + f2(jmx) + ' ' + f2(jmy) + ' ' + f2(tbx) + ' ' + f2(tby), 'none', ACC, 1.8) +
            v319path('M ' + f2(exx) + ' ' + f2(eyy) + ' L ' + f2(tbx - tuy * 4) + ' ' + f2(tby + tux * 4) + ' L ' + f2(tbx + tuy * 4) + ' ' + f2(tby - tux * 4) + ' Z', ACC, ACC, 1) +
            tx(jmx, apy + 14, a5.name + ' (' + nm(a5.dur) + ')', '#0d47a1', 'middle', true, 10);
        } else {
          s += pfxArrow(sxx, syy, exx, eyy, ACC, 1.8, 8) +
            tx((sxx + exx) / 2 + wnx * 13, (syy + eyy) / 2 + wny * 13 + 4, a5.name + ' (' + nm(a5.dur) + ')', '#0d47a1', 'middle', true, 10);
        }
      });
      ptOrder.forEach(function (k2) { var pp4 = ptPos[k2]; s += pfxCls(pp4[0], pp4[1], 14, '#ffffff', ST, 2); s += tx(pp4[0], pp4[1] + 4, k2, ST, 'middle', true, 10); });
      s += tx(180, 262, 'شبکهٔ فعالیت‌ها (CPM/PERT) — ' + nm(ptList.length) + ' فعالیت', MU, 'middle', true, 11);
      return wrap(s);
    }
    if (t === 'axbenzene') {
      var bzKind = xs('kind', 'kekule').toLowerCase(), bzMode = xs('mode', 'label').toLowerCase();
      if (bzKind === 'chair') {
        s += pfxPath('M 70 118 L 130 94 L 190 118 L 250 94 L 250 138 L 190 162 L 130 138 L 70 162 Z', 'none', ST, 2.2);
        s += lnr(70, 118, 70, 162, ST, 2.2) + lnr(130, 94, 130, 138, ST, 2.2) + lnr(190, 118, 190, 162, ST, 2.2) + lnr(250, 94, 250, 138, ST, 2.2);
        s += tx(180, 214, 'صندلی سیکلوهگزان', MU, 'middle', true, 12);
        return wrap(s);
      }
      if (bzKind === 'newman') {
        s += pfxCls(180, 132, 66, 'none', ST, 2.2) + pfxCls(180, 132, 12, '#ffffff', ST, 1.8);
        for (var ni3 = 0; ni3 < 3; ni3++) {
          var na3 = ni3 * 2 * Math.PI / 3;
          s += lnr(180 + 12 * Math.cos(na3), 132 + 12 * Math.sin(na3), 180 + 66 * Math.cos(na3), 132 + 66 * Math.sin(na3), ST, 2.2);
          s += pfxCls(180 + 74 * Math.cos(na3), 132 + 74 * Math.sin(na3), 7, '#ffffff', ST, 1.6);
          var nb3 = na3 + Math.PI / 3;
          s += lnr(180 + 12 * Math.cos(nb3 + 0.6), 132 + 12 * Math.sin(nb3 + 0.6), 180 + 56 * Math.cos(nb3 + 0.6), 132 + 56 * Math.sin(nb3 + 0.6), PT, 1.8, '4 3');
        }
        s += tx(180, 244, 'تصویر نیومن — اتان', MU, 'middle', true, 12);
        return wrap(s);
      }
      if (bzKind === 'fischer') {
        s += lnr(120, 60, 240, 60, ST, 2) + lnr(120, 200, 240, 200, ST, 2) + lnr(180, 60, 180, 200, ST, 2);
        s += tx(150, 50, 'CH₃', ACC, 'middle', true, 12) + tx(210, 50, 'H', '#00796b', 'middle', true, 12);
        s += tx(150, 222, 'H', '#00796b', 'middle', true, 12) + tx(210, 222, 'OH', PT, 'middle', true, 12);
        s += tx(180, 132, 'C', ST, 'middle', true, 13) + tx(180, 268, 'تصویر فیشر', MU, 'middle', true, 12);
        return wrap(s);
      }
      var bzCx = 180, bzCy = 130, bzR = 74, bzPts = [];
      for (var bi5 = 0; bi5 < 6; bi5++) { var ba = -Math.PI / 2 + bi5 * Math.PI / 3; bzPts.push([bzCx + bzR * Math.cos(ba), bzCy + bzR * Math.sin(ba)]); }
      s += pfxPoly(bzPts.map(function (p4) { return f2(p4[0]) + ',' + f2(p4[1]); }).join(' '), 'rgba(108,99,245,.08)', ST, 2.2);
      if (bzMode === 'circle') s += pfxCls(bzCx, bzCy, bzR * 0.62, 'none', ACC, 2.4);
      else for (var bi6 = 0; bi6 < 6; bi6 += 2) {
        var q1 = bzPts[bi6], q2 = bzPts[(bi6 + 1) % 6];
        var md = [(q1[0] + q2[0]) / 2, (q1[1] + q2[1]) / 2], inx = bzCx - md[0], iny = bzCy - md[1], il = Math.sqrt(inx * inx + iny * iny) || 1;
        var ox3 = inx / il * 9, oy3 = iny / il * 9;
        s += lnr(md[0] - (q2[0] - q1[0]) / 2 * 0.55 + ox3, md[1] - (q2[1] - q1[1]) / 2 * 0.55 + oy3, md[0] + (q2[0] - q1[0]) / 2 * 0.55 + ox3, md[1] + (q2[1] - q1[1]) / 2 * 0.55 + oy3, ACC, 2.6);
      }
      bzPts.forEach(function (p4, i8) { if (bzMode !== 'circle') s += tx(p4[0] + (p4[0] - bzCx) * 0.22, p4[1] + (p4[1] - bzCy) * 0.22 + 4, 'C', MU, 'middle', false, 10); });
      s += tx(180, 250, bzMode === 'circle' ? 'بنزن — نمایش حلقهٔ آروماتیک' : 'بنزن — ساختار ککوله', ST, 'middle', true, 13);
      return wrap(s);
    }
    if (t === 'axnormal') {
      var nrMu = num(X.mu, 0), nrSd = Math.max(0.1, num(X.sd, 1)), nrLo = num(X.lo, -1), nrHi = num(X.hi, 1);
      var nrX0 = 34, nrX1 = 326, nrY = 206, nrH = 140, nrMin = nrMu - 3.4 * nrSd, nrMax = nrMu + 3.4 * nrSd;
      // V322 — lo/hi بیرون از ناحیهٔ رسم، خط و نقطهٔ بیرون از بوم می‌ساخت.
      nrLo = clamp(nrLo, nrMin, nrMax); nrHi = clamp(nrHi, nrMin, nrMax);
      var nrPx = function (v9) { return nrX0 + (v9 - nrMin) / (nrMax - nrMin) * (nrX1 - nrX0); };
      var nrPdf = function (v9) { var z5 = (v9 - nrMu) / nrSd; return Math.exp(-z5 * z5 / 2); };
      var shade = '';
      for (var bi7 = 0; bi7 <= 120; bi7++) {
        var vv1 = nrMin + (nrMax - nrMin) * bi7 / 120;
        if (vv1 >= nrLo && vv1 <= nrHi) shade += f2(nrPx(vv1)) + ',' + f2(nrY - nrPdf(vv1) * nrH) + ' ';
      }
      if (shade) s += '<polygon points="' + f2(nrPx(Math.max(nrLo, nrMin))) + ',' + nrY + ' ' + shade + f2(nrPx(Math.min(nrHi, nrMax))) + ',' + nrY + '" fill="rgba(39,196,168,.26)"/>';
      var curve = '';
      for (var bi8 = 0; bi8 <= 140; bi8++) { var vv2 = nrMin + (nrMax - nrMin) * bi8 / 140; curve += f2(nrPx(vv2)) + ',' + f2(nrY - nrPdf(vv2) * nrH) + ' '; }
      s += '<polyline points="' + curve.trim() + '" fill="none" stroke="' + ACC + '" stroke-width="2.4"/>';
      s += lnr(nrX0, nrY, nrX1 + 8, nrY, ST, 1.4);
      for (var si4 = -3; si4 <= 3; si4++) {
        var zx3 = nrMu + si4 * nrSd;
        s += lnr(nrPx(zx3), nrY, nrPx(zx3), nrY + 6, ST, 1.2) + tx(nrPx(zx3), nrY + 20, nm(si4), MU, 'middle', false, 10);
      }
      s += tx(nrPx(nrMu), nrY + 38, 'μ = ' + nm(nrMu), ST, 'middle', true, 11);
      s += lnr(nrPx(nrLo), nrY, nrPx(nrLo), nrY - nrPdf(nrLo) * nrH, PT, 1.4, '4 3') + lnr(nrPx(nrHi), nrY, nrPx(nrHi), nrY - nrPdf(nrHi) * nrH, PT, 1.4, '4 3');
      s += tx(180, 262, 'منحنی نرمال — احتمال بین ' + nm(nrLo) + ' و ' + nm(nrHi), MU, 'middle', true, 12);
      return wrap(s);
    }
    if (t === 'axsupply') {
      var spKind = xs('kind', 'equilibrium').toLowerCase(), spShift = num(X.shift, 2), spCeil = num(X.ceiling, 0);
      var spX0 = 54, spX1 = 320, spY0 = 206, spY1 = 44;
      s += lnr(spX0, spY0, spX1, spY0, ST, 1.6) + pfxArrow(spX1 - 12, spY0, spX1 + 6, spY0, ST, 1.6, 7);
      s += lnr(spX0, spY0, spX0, spY1 + 18, ST, 1.6) + pfxArrow(spX0, spY1 + 30, spX0, spY1 + 12, ST, 1.6, 7);
      s += tx(spX1 + 2, spY0 + 16, 'Q', MU, 'middle', true, 12) + tx(spX0 - 14, spY1 + 22, 'P', MU, 'middle', true, 12);
      var spDem = function (x7) { return spY0 - 30 - (x7 / 1) * 0.55; };
      var spSup = function (x7) { return spY0 - 26 - (1 - x7 / 1) * 0.15; };
      s += pfxArrow(spX0 + 16, spDem(0.06), spX1 - 22, spY0 - 150, PT, 2.4, 9);
      s += pfxArrow(spX0 + 16, spY0 - 32, spX1 - 22, spY1 + 40, '#00796b', 2.4, 9);
      s += tx(spX0 + 34, spY0 - 62, 'تقاضا', PT, 'start', true, 11) + tx(spX1 - 34, spY1 + 74, 'عرضه', '#00796b', 'end', true, 11);
      if (spKind === 'shift') {
        s += pfxArrow(spX0 + 60, spDem(0.2), spX1 - 60, spY0 - 162, 'rgba(214,51,108,.45)', 2, 8) + tx(240, 72, 'تقاضای جدید', PT, 'middle', false, 10);
      }
      if (spCeil > 0) {
        var cy4 = clamp(spY0 - 40 - spCeil * 12, spY1 + 40, spY0 - 30);
        s += lnr(spX0, cy4, spX1 - 10, cy4, ACC, 1.8, '6 4') + tx(spX1 - 14, cy4 - 6, 'سقف قیمت', ACC, 'end', true, 10);
      }
      s += pfxCls(spX0 + 118, spY0 - 118, 5, ST, 'none', 0) + tx(spX0 + 130, spY0 - 132, 'تعادل', ST, 'start', true, 11);
      var spName = spKind === 'shift' ? 'افزایش تقاضا — قیمت و مقدار تعادلی بالا می‌رود' : spKind === 'ceiling' ? 'سقف قیمت — کمبود عرضه' : 'تعادل بازار — تلاقی عرضه و تقاضا';
      s += tx(180, 258, spName, ST, 'middle', true, 11);
      return wrap(s);
    }
    if (t === 'axgame') {
      var gmKind = xs('kind', 'game').toLowerCase();
      if (gmKind === 'islm') {
        var isX0 = 54, isX1 = 320, isY0 = 210, isY1 = 44;
        s += lnr(isX0, isY0, isX1, isY0, ST, 1.6) + pfxArrow(isX1 - 12, isY0, isX1 + 6, isY0, ST, 1.6, 7) + lnr(isX0, isY0, isX0, isY1, ST, 1.6) + pfxArrow(isX0, isY1 + 14, isX0, isY1 - 4, ST, 1.6, 7);
        s += tx(isX1 + 2, isY0 + 16, 'Y', MU, 'middle', true, 12) + tx(isX0 - 14, isY1 + 10, 'i', MU, 'middle', true, 12);
        s += pfxArrow(isX0 + 20, isY0 - 24, isX1 - 26, isY1 + 46, ACC, 2.4, 9) + tx(66, 66, 'IS', ACC, 'start', true, 12);
        s += pfxArrow(isX0 + 20, isY1 + 52, isX1 - 26, isY0 - 30, '#00796b', 2.4, 9) + tx(isX1 - 30, isY0 - 34, 'LM', '#00796b', 'end', true, 12);
        s += pfxCls((isX0 + isX1) / 2, (isY0 + isY1) / 2 + 14, 5, ST, 'none', 0) + tx((isX0 + isX1) / 2 + 10, (isY0 + isY1) / 2 + 10, 'E', ST, 'start', true, 12);
        s += tx(180, 258, 'مدل IS-LM — تعادل هم‌زمان بازار کالا و پول', MU, 'middle', true, 12);
        return wrap(s);
      }
      var gmM = String(xs('mat', '3,0;5,1')).split(/[;؛]/).map(function (rw2) { return rw2.split(/[,،]/).map(function (z6) { return (z6.trim()); }); }).slice(0, 2);
      if (gmM.length < 2) gmM = [['3', '0'], ['5', '1']];
      var gmCell2 = Math.min(62, 240 / 3), gmX0 = 180 - gmCell2 * 1.5, gmY0 = 56;
      s += tx(gmX0 + gmCell2 * 1.5, 40, 'بازیکن ستون', MU, 'middle', true, 11);
      s += tx(gmX0 - 26, gmY0 + gmCell2 * 1.5, 'بازیکن', MU, 'middle', true, 11);
      for (var gr = 0; gr < 2; gr++) for (var gc = 0; gc < 2; gc++) {
        var cellV = (gmM[gr] && gmM[gr][gc]) || '0';
        s += rc(gmX0 + gc * gmCell2, gmY0 + gr * gmCell2, gmCell2, gmCell2, '#ffffff', ST, 1.6) + tx(gmX0 + gc * gmCell2 + gmCell2 / 2, gmY0 + gr * gmCell2 + gmCell2 / 2 + 6, cellV, ST, 'middle', true, 14);
      }
      s += rc(gmX0, gmY0, gmCell2 * 2, gmCell2 * 2, 'none', ST, 2);
      s += tx(180, gmY0 + gmCell2 * 2 + 30, 'ماتریس بازی ۲×۲ — سود بازیکن سطر', MU, 'middle', true, 11);
      return wrap(s);
    }
    // ================================================================ V319 — بستهٔ سوم ابزار آموزشی
    function v319poly(pts, fill, stroke, w) { return '<polygon points="' + pts + '" fill="' + fill + '" stroke="' + stroke + '" stroke-width="' + w + '" stroke-linejoin="round"/>'; }
    function v319circ(cx, cy, r, fill, stroke, w) { return '<circle cx="' + f2(cx) + '" cy="' + f2(cy) + '" r="' + f2(r) + '" fill="' + fill + '" stroke="' + stroke + '" stroke-width="' + (w || 1.2) + '"/>'; }
    function v319path(d, fill, stroke, w, dash) { return '<path d="' + d + '" fill="' + fill + '" stroke="' + stroke + '" stroke-width="' + w + '" stroke-linejoin="round" stroke-linecap="round"' + (dash ? ' stroke-dasharray="' + dash + '"' : '') + '/>'; }
    function v319txt(x, y, t2, c, an, b, sz) { return tx(x, y, t2, c || MU, an || 'middle', !!b, sz || 11); }
    /* V326 — نقطه روی محیط دایره (درجهٔ چرخشی، ۰ = راست، ۹۰ = پایین چون y به سمت پایین است) */
    function circPt(cx, cy, r, deg) { var a = deg * Math.PI / 180; return [cx + r * Math.cos(a), cy + r * Math.sin(a)]; }
    /* V326 — برچسب بیرون دایره: کادر متن هرگز روی محیط یا داخل خط نمی‌افتد */
    function circLabel(cx, cy, r, deg, t2, c, gap) {
      var a = deg * Math.PI / 180, ux = Math.cos(a), uy = Math.sin(a);
      var rr = r + (gap == null ? 15 : gap);
      var an = ux > 0.3 ? 'start' : (ux < -0.3 ? 'end' : 'middle');
      var dy = uy > 0.4 ? 11 : (uy < -0.4 ? -5 : 4);
      return tx(cx + ux * rr, cy + uy * rr + dy, t2, c, an, true, 10.5);
    }
    /* V326 — برچسب داخل دایره در فاصلهٔ امن از محیط و از خطوط عبوری از مرکز */
    function innerLabel(cx, cy, r, deg, t2, c, frac) {
      var a = deg * Math.PI / 180, f = (frac == null ? 0.55 : frac) * r;
      return tx(cx + Math.cos(a) * f, cy + Math.sin(a) * f + 4, t2, c, 'middle', true, 10.5);
    }
    /* V326 — کمانِ روی محیط (بدون جابه‌جایی) با فلش جهت */
    function arcOnCircle(cx, cy, r, d0, d1, c, w) {
      var p0 = circPt(cx, cy, r, d0), p1 = circPt(cx, cy, r, d1);
      var span = ((d1 - d0) % 360 + 360) % 360, large = span > 180 ? 1 : 0;
      return v319path('M ' + f2(p0[0]) + ' ' + f2(p0[1]) + ' A ' + f2(r) + ' ' + f2(r) + ' 0 ' + large + ' 1 ' + f2(p1[0]) + ' ' + f2(p1[1]),
        'none', c, w || 3.2);
    }

    function fmtClean(v0) {
      var av = Math.abs(v0);
      if (av >= 1e-6 && Math.abs(v0 - Math.round(v0)) < 1e-6) return String(Math.round(v0));
      if (av === 0) return '0';
      return v0.toFixed(4).replace(/0+$/, '').replace(/\.$/, '');
    }
    function vecArrow(x1, y1, x2, y2, c, w, hd, dash) {
      var an = Math.atan2(y2 - y1, x2 - x1), h0 = hd || 7.5;
      return ln(x1, y1, x2, y2, c, w || 2.2, dash || '') +
        pfxPoly(f2(x2) + ',' + f2(y2) + ' ' + f2(x2 - h0 * Math.cos(an - 0.44)) + ',' + f2(y2 - h0 * Math.sin(an - 0.44)) + ' ' + f2(x2 - h0 * Math.cos(an + 0.44)) + ',' + f2(y2 - h0 * Math.sin(an + 0.44)), c);
    }

    if (t === 'axunitconv') {
      var uck = xs('kind', 'len').toLowerCase(), ucv = num(X.val, 1), ucBlank = xs('blank', '0') === '1';
      var ucEmp = pfxEmptySet(xs('empty', '')), ucOv = pfxCellMap(xs('ov', '')), ucAns = pfxCellMap(xs('ans', ''));
      var chain = uck === 'mass' ? ['جرم', ['t', 'kg', 'g', 'mg'], [1000, 1000, 1000]]
        : uck === 'area' ? ['مساحت', ['m²', 'dm²', 'cm²', 'mm²'], [100, 100, 100]]
        : uck === 'vol' ? ['حجم', ['m³', 'L (dm³)', 'mL (cm³)'], [1000, 1000]]
        : uck === 'time' ? ['زمان', ['ساعت', 'دقیقه', 'ثانیه'], [60, 60]]
        : ['طول', ['km', 'm', 'cm', 'mm'], [1000, 100, 10]];
      var units = chain[1], facs = chain[2], nU = units.length, vals = [ucv];
      for (var fi = 0; fi < facs.length; fi++) vals.push(vals[vals.length - 1] * facs[fi]);
      s += tx(180, 22, 'نردبان تبدیل واحد ' + chain[0], '#1e293b', 'middle', true, 12);

      var cardW = nU === 4 ? 74 : 94, cardH = 62, gapU = nU === 4 ? 11 : 20;
      var x0U = (360 - (nU * cardW + (nU - 1) * gapU)) / 2, yCard = 104;
      var tints = ['#eef2ff', '#ecfdf5', '#fffbeb', '#fdf2f8'], brds = [ACC, '#0d9488', '#d97706', PT];

      for (var ui = 0; ui < nU - 1; ui++) {
        var xA = x0U + ui * (cardW + gapU) + cardW * 0.72, xB = x0U + (ui + 1) * (cardW + gapU) + cardW * 0.28, xm = (xA + xB) / 2, fac = facs[ui];
        s += pfxPath('M ' + f2(xA) + ' ' + f2(yCard - 4) + ' Q ' + f2(xm) + ' ' + f2(yCard - 38) + ' ' + f2(xB) + ' ' + f2(yCard - 4), 'none', ACC, 1.7);
        s += pfxPoly(f2(xB) + ',' + f2(yCard - 4) + ' ' + f2(xB - 6) + ',' + f2(yCard - 10) + ' ' + f2(xB - 1.5) + ',' + f2(yCard - 12), ACC);
        s += rc(xm - 21, yCard - 42, 42, 16, '#ffffff', ACC, 1, 5) + tx(xm, yCard - 31, '×' + fac, ACC, 'middle', true, 9);

        var yBotU = yCard + cardH;
        s += pfxPath('M ' + f2(xB) + ' ' + f2(yBotU + 4) + ' Q ' + f2(xm) + ' ' + f2(yBotU + 38) + ' ' + f2(xA) + ' ' + f2(yBotU + 4), 'none', '#0f766e', 1.6, '4 2.5');
        s += pfxPoly(f2(xA) + ',' + f2(yBotU + 4) + ' ' + f2(xA + 6) + ',' + f2(yBotU + 10) + ' ' + f2(xA + 1.5) + ',' + f2(yBotU + 12), '#0f766e');
        s += rc(xm - 21, yBotU + 26, 42, 16, '#ffffff', '#0f766e', 1, 5) + tx(xm, yBotU + 37, '÷' + fac, '#0f766e', 'middle', true, 9);
      }

      for (var uj = 0; uj < nU; uj++) {
        var cxU = x0U + uj * (cardW + gapU), keyU = 'u' + uj, autoEU = ucBlank && uj > 0, isEU = !!ucEmp[keyU] || autoEU;
        var defVU = fmtClean(vals[uj]);
        var cardIn = rc(cxU, yCard, cardW, cardH, isEU ? '#ffffff' : tints[uj % tints.length], isEU ? '#4f46e5' : brds[uj % brds.length], isEU ? 1.9 : 1.7, 8);
        cardIn += rc(cxU + 3, yCard + cardH - 20, cardW - 6, 17, 'rgba(15,23,42,.06)', 'none', 0, 5);
        cardIn += tx(cxU + cardW / 2, yCard + cardH - 8, units[uj], MU, 'middle', true, 10);
        if (isEU) {
          cardIn += rcDash(cxU + 5, yCard + 5, cardW - 10, 32, '#f8fafc', '#4f46e5', 1.4, 5, '4 2.5');
          var stvU = (ucAns[keyU] || '').trim();
          if (stvU) cardIn += tx(cxU + cardW / 2, yCard + 26, stvU, '#1d4ed8', 'middle', true, stvU.length > 6 ? 10 : 12);
        } else {
          var dispU = (ucOv[keyU] != null ? ucOv[keyU] : defVU).trim();
          cardIn += tx(cxU + cardW / 2, yCard + 26, dispU, ST, 'middle', true, dispU.length > 6 ? 10 : 12);
        }
        s += icWrap(keyU, 'مقدار بر حسب ' + units[uj], defVU, isEU, cardIn);
      }
      s += tx(180, 256, 'ضرب در حرکت به واحد کوچک‌تر، تقسیم در حرکت به واحد بزرگ‌تر', MU, 'middle', false, 10);
      return wrap(s);
    }

    if (t === 'axperimeter') {
      var psk = xs('shape', 'rect').toLowerCase(), pShow = xs('show', 'both').toLowerCase();
      var pw = Math.max(1, num(X.w, 6)), ph = Math.max(1, num(X.h, 4)), paTop = Math.max(1, num(X.a, Math.max(1, pw - 2)));
      var perStr = '', areaStr = '';
      if (psk === 'square') {
        var sPx = 124, rxS = 180 - sPx / 2, ryS = 42;
        s += rc(rxS, ryS, sPx, sPx, 'rgba(108,99,245,.13)', ACC, 2.2, 3) + rc(rxS, ryS + sPx - 11, 11, 11, 'none', ACC, 1.3);
        s += ln(180, ryS - 4, 180, ryS + 4, PT, 1.8) + ln(180, ryS + sPx - 4, 180, ryS + sPx + 4, PT, 1.8);
        s += ln(rxS - 4, ryS + sPx / 2, rxS + 4, ryS + sPx / 2, PT, 1.8) + ln(rxS + sPx - 4, ryS + sPx / 2, rxS + sPx + 4, ryS + sPx / 2, PT, 1.8);
        s += tx(180, ryS - 10, 'a = ' + nm(pw), ST, 'middle', true, 12);
        perStr = 'P = 4a = ' + nm(4 * pw); areaStr = 'S = a² = ' + nm(pw * pw);
      } else if (psk === 'circle') {
        var rPx = clamp(pw * 11, 36, 72), cxC = 180, cyC = 112;
        s += pfxCls(cxC, cyC, rPx, 'rgba(39,196,168,.15)', GRN, 2.2) + pfxCls(cxC, cyC, 3.2, ST, 'none', 0);
        s += ln(cxC, cyC, cxC + rPx, cyC, PT, 2) + tx(cxC + rPx / 2, cyC - 8, 'r = ' + nm(pw), PT, 'middle', true, 11);
        perStr = 'P = 2πr ≈ ' + nm(Math.round(2 * Math.PI * pw * 10) / 10);
        areaStr = 'S = πr² ≈ ' + nm(Math.round(Math.PI * pw * pw * 10) / 10);
      } else if (psk === 'tri') {
        var scT2 = Math.min(180 / pw, 120 / ph), bwT = Math.max(60, pw * scT2), bhT = Math.max(50, ph * scT2), x0T = 180 - bwT / 2, y0T = 176;
        s += pfxPoly(f2(x0T) + ',' + f2(y0T) + ' ' + f2(x0T + bwT) + ',' + f2(y0T) + ' ' + f2(x0T) + ',' + f2(y0T - bhT), 'rgba(240,162,2,.15)', GLD, 2.2);
        s += rc(x0T, y0T - 11, 11, 11, 'none', ST, 1.3);
        var hyp = Math.sqrt(pw * pw + ph * ph);
        s += tx(x0T + bwT / 2, y0T + 18, 'a = ' + nm(pw), ST, 'middle', true, 11) + tx(x0T - 10, y0T - bhT / 2, 'b = ' + nm(ph), ST, 'end', true, 11) + tx(x0T + bwT / 2 + 14, y0T - bhT / 2 - 4, 'c ≈ ' + nm(Math.round(hyp * 10) / 10), PT, 'start', true, 11);
        perStr = 'P = ' + nm(Math.round((pw + ph + hyp) * 10) / 10); areaStr = 'S = ½ab = ' + nm(pw * ph / 2);
      } else if (psk === 'para') {
        var bwP = 156, bhP = 96, skw = 34, x0P = 96, y0P = 172;
        s += pfxPoly(f2(x0P) + ',' + f2(y0P) + ' ' + f2(x0P + bwP) + ',' + f2(y0P) + ' ' + f2(x0P + bwP + skw) + ',' + f2(y0P - bhP) + ' ' + f2(x0P + skw) + ',' + f2(y0P - bhP), 'rgba(108,99,245,.13)', ACC, 2.2);
        s += ln(x0P + skw, y0P - bhP, x0P + skw, y0P, PT, 1.6, '4 3') + rc(x0P + skw, y0P - 10, 10, 10, 'none', PT, 1.2);
        var sideP = Math.round(Math.hypot(ph, Math.max(1, pw * 0.35)) * 10) / 10;
        s += tx(x0P + bwP / 2, y0P + 18, 'قاعده b = ' + nm(pw), ST, 'middle', true, 11) + tx(x0P + skw + 6, y0P - bhP / 2, 'h = ' + nm(ph), PT, 'start', true, 11);
        perStr = 'P ≈ ' + nm(2 * (pw + sideP)); areaStr = 'S = b×h = ' + nm(pw * ph);
      } else if (psk === 'trap') {
        var bBig = Math.max(pw, paTop + 1), bSm = Math.min(pw, paTop), wPx = 176, topPx = clamp(bSm / bBig * wPx, 64, 148), hPx = 98;
        var x0Tr = 180 - wPx / 2, xTopTr = 180 - topPx / 2, y0Tr = 172;
        s += pfxPoly(f2(x0Tr) + ',' + f2(y0Tr) + ' ' + f2(x0Tr + wPx) + ',' + f2(y0Tr) + ' ' + f2(xTopTr + topPx) + ',' + f2(y0Tr - hPx) + ' ' + f2(xTopTr) + ',' + f2(y0Tr - hPx), 'rgba(39,196,168,.15)', '#0d9488', 2.2);
        s += ln(xTopTr, y0Tr - hPx, xTopTr, y0Tr, PT, 1.6, '4 3') + rc(xTopTr, y0Tr - 10, 10, 10, 'none', PT, 1.2);
        var leg = Math.sqrt(Math.pow((bBig - bSm) / 2, 2) + ph * ph);
        s += tx(180, y0Tr - hPx - 8, 'a = ' + nm(bSm), ST, 'middle', true, 11) + tx(180, y0Tr + 18, 'b = ' + nm(bBig), ST, 'middle', true, 11) + tx(xTopTr + 6, y0Tr - hPx / 2, 'h = ' + nm(ph), PT, 'start', true, 11);
        perStr = 'P ≈ ' + nm(Math.round((bBig + bSm + 2 * leg) * 10) / 10); areaStr = 'S = ½(a+b)h = ' + nm((bBig + bSm) * ph / 2);
      } else if (psk === 'rhomb') {
        var d1P = 164, d2P = 106, cxR = 180, cyR = 112;
        s += pfxPoly(f2(cxR) + ',' + f2(cyR - d2P / 2) + ' ' + f2(cxR + d1P / 2) + ',' + f2(cyR) + ' ' + f2(cxR) + ',' + f2(cyR + d2P / 2) + ' ' + f2(cxR - d1P / 2) + ',' + f2(cyR), 'rgba(214,51,108,.12)', PT, 2.2);
        s += ln(cxR - d1P / 2, cyR, cxR + d1P / 2, cyR, ACC, 1.5, '4 3') + ln(cxR, cyR - d2P / 2, cxR, cyR + d2P / 2, '#0d9488', 1.5, '4 3');
        var sideR = Math.hypot(pw / 2, ph / 2);
        s += tx(cxR + d1P / 4, cyR - 6, 'd₁=' + nm(pw), ACC, 'middle', true, 10) + tx(cxR + 6, cyR + d2P / 4 + 4, 'd₂=' + nm(ph), '#0d9488', 'start', true, 10);
        perStr = 'P = 4a ≈ ' + nm(Math.round(4 * sideR * 10) / 10); areaStr = 'S = ½d₁d₂ = ' + nm(pw * ph / 2);
      } else if (psk === 'poly') {
        var nPl = clamp(Math.round(num(X.n, 5)), 3, 10), rPl = 68, cxPl = 180, cyPl = 110, ptsPl = [];
        for (var pi0 = 0; pi0 < nPl; pi0++) { var aPl = -Math.PI / 2 + 2 * Math.PI * pi0 / nPl; ptsPl.push(f2(cxPl + rPl * Math.cos(aPl)) + ',' + f2(cyPl + rPl * Math.sin(aPl))); }
        s += pfxPoly(ptsPl.join(' '), 'rgba(108,99,245,.13)', ACC, 2.2);
        var apo = pw / (2 * Math.tan(Math.PI / nPl));
        s += tx(cxPl, cyPl + rPl + 18, nPl + ' ضلعی منتظم — ضلع a = ' + nm(pw), ST, 'middle', true, 11);
        perStr = 'P = ' + nPl + 'a = ' + nm(nPl * pw); areaStr = 'S ≈ ' + nm(Math.round(nPl * pw * apo / 2 * 10) / 10);
      } else {
        var scR = Math.min(204 / pw, 122 / ph), rw = Math.max(72, pw * scR), rh = Math.max(48, ph * scR), rx0 = 180 - rw / 2, ry0 = 108 - rh / 2;
        s += rc(rx0, ry0, rw, rh, 'rgba(108,99,245,.13)', ACC, 2.2, 3) + rc(rx0, ry0 + rh - 11, 11, 11, 'none', ACC, 1.3);
        s += tx(180, ry0 - 9, 'طول = ' + nm(pw), ST, 'middle', true, 12) + tx(rx0 + rw + 10, ry0 + rh / 2 + 4, 'عرض = ' + nm(ph), ST, 'start', true, 12);
        perStr = 'محیط P = ' + nm(2 * (pw + ph)); areaStr = 'مساحت S = ' + nm(pw * ph);
      }
      var footerP = (pShow === 'none' || pShow === '0') ? '' : (pShow === 'perim' || pShow === 'p') ? perStr : (pShow === 'area' || pShow === 's') ? areaStr : (perStr + '   |   ' + areaStr);
      if (footerP) {
        s += rc(22, 232, 316, 32, '#eef2ff', ACC, 1.5, 8);
        s += tx(180, 253, footerP, '#1e1b4b', 'middle', true, 12);
      }
      return wrap(s);
    }
    if (t === 'axpictograph') {
      var pgMode = xs('mode', 'picto'), pgData = ilist('data', '4,7,3,6'), pgLabs = xl('labs');
      if (!pgData.length) pgData = [4, 7, 3, 6];
      if (!pgLabs.length) pgLabs = ['سیب', 'موز', 'پرتقال', 'انگور'];
      var pgMax = 1, pgI;
      for (pgI = 0; pgI < pgData.length; pgI++) pgMax = Math.max(pgMax, pgData[pgI]);
      var pgRows = Math.min(5, pgData.length), pgTop = 46, pgGap = Math.min(38, 168 / pgRows);
      s += lnr(96, pgTop, 96, pgTop + pgRows * pgGap + 6, ST, 1.6) + lnr(96, pgTop + pgRows * pgGap + 6, 330, pgTop + pgRows * pgGap + 6, ST, 1.6);
      for (pgI = 0; pgI < pgRows; pgI++) {
        var pgy = pgTop + pgI * pgGap + pgGap / 2;
        s += v319txt(88, pgy + 4, pgLabs[pgI] || ('دستهٔ ' + (pgI + 1)), MU, 'end', true, 10);
        if (pgMode === 'bar') {
          var pgw = (pgData[pgI] / pgMax) * 214;
          s += rc(100, pgy - 9, Math.max(2, pgw), 18, 'rgba(108,99,245,.55)', ACC, 1.2, 4);
          s += v319txt(104 + pgw, pgy + 4, nm(pgData[pgI]), ST, 'start', true, 10);
        } else {
          for (var pgJ = 0; pgJ < Math.min(10, pgData[pgI]); pgJ++) s += v319circ(112 + pgJ * 21, pgy, 7.5, 'rgba(39,196,168,.85)', '#ffffff', 1);
          s += v319txt(112 + Math.min(10, pgData[pgI]) * 21 + 4, pgy + 4, String(pgData[pgI]), ST, 'start', true, 10);
        }
      }
      s += v319txt(180, 254, pgMode === 'bar' ? 'نمودار میله‌ای — مقایسهٔ تعداد' : 'نمودار تصویری — هر شکل نمایندهٔ ۱ عدد', MU, 'middle', true, 11);
      return wrap(s);
    }

    if (t === 'axrounding') {
      var rn = num(X.n, 147), rm = xs('mode', 'ten').toLowerCase(), rLbl = xs('label', '1');
      var rStep = rm === 'thousand' ? 1000 : rm === 'hundred' ? 100 : rm === 'one' ? 1 : rm === 'tenth' ? 0.1 : rm === 'hundredth' ? 0.01 : 10;
      var rmFa = rm === 'thousand' ? 'هزارگان' : rm === 'hundred' ? 'صدگان' : rm === 'one' ? 'یکان' : rm === 'tenth' ? 'دهم' : rm === 'hundredth' ? 'صدم' : 'دهگان';
      var rlo = Math.floor(rn / rStep + 1e-9) * rStep, rhi = rlo + rStep, rmid = rlo + rStep / 2;
      var rUp = rn >= rmid - 1e-9, rans = rUp ? rhi : rlo;
      var rxL = 44, rxR = 316, ryL = 136, rxMid = (rxL + rxR) / 2;
      s += tx(180, 26, 'گرد کردن عدد ' + fmtClean(rn) + ' به تقریب ' + rmFa, '#1e293b', 'middle', true, 12);
      s += rc(rxL, ryL - 16, rxMid - rxL, 16, 'rgba(148,163,184,.16)', 'none', 0, 3);
      s += rc(rxMid, ryL - 16, rxR - rxMid, 16, 'rgba(39,196,168,.18)', 'none', 0, 3);
      s += ln(rxL - 12, ryL, rxR + 12, ryL, ST, 2.2);
      for (var ri = 0; ri <= 10; ri++) {
        var rtx = rxL + ri * (rxR - rxL) / 10, isE = ri === 0 || ri === 10, isM = ri === 5;
        var hTk = isE ? 12 : (isM ? 9 : 5);
        s += ln(rtx, ryL - hTk, rtx, ryL + hTk, isM ? '#d97706' : ST, (isE || isM) ? 2 : 1.1);
      }
      s += tx(rxL, ryL + 28, fmtClean(rlo), ST, 'middle', true, 12) + tx(rxMid, ryL + 26, 'وسط (' + fmtClean(rmid) + ')', '#b45309', 'middle', true, 10) + tx(rxR, ryL + 28, fmtClean(rhi), ST, 'middle', true, 12);
      var rnx = rxL + clamp((rn - rlo) / (rhi - rlo), 0, 1) * (rxR - rxL), txAns = rUp ? rxR : rxL;
      if (rLbl === '1' && Math.abs(rnx - txAns) > 6) {
        s += pfxPath('M ' + f2(rnx) + ' ' + f2(ryL - 12) + ' Q ' + f2((rnx + txAns) / 2) + ' ' + f2(ryL - 58) + ' ' + f2(txAns) + ' ' + f2(ryL - 14), 'none', '#0d9488', 2, '5 3');
        s += pfxCls(txAns, ryL, 6.5, '#10b981', '#ffffff', 1.6);
      }
      s += pfxCls(rnx, ryL, 6.5, PT, '#ffffff', 1.8) + rc(rnx - 24, ryL - 44, 48, 20, '#fff1f2', PT, 1.3, 5) + tx(rnx, ryL - 30, fmtClean(rn), PT, 'middle', true, 11);
      if (rLbl !== '0') {
        var askR = rLbl === '2';
        s += rc(28, 224, 304, 34, askR ? '#fffbeb' : '#ecfdf5', askR ? GLD : '#10b981', 1.6, 8);
        s += tx(180, 246, askR ? (fmtClean(rn) + '  ≈  ؟   (گرد به ' + rmFa + ')') : (fmtClean(rn) + '  ≈  ' + fmtClean(rans) + '   (گرد به ' + rmFa + ')'), askR ? '#92400e' : '#065f46', 'middle', true, 13);
      }
      return wrap(s);
    }
    if (t === 'axdivision') {
      var dvA = Math.max(0, Math.min(99, num(X.a, 17))), dvB = Math.max(1, Math.min(12, num(X.b, 5)));
      var dvQ = Math.floor(dvA / dvB), dvR = dvA - dvQ * dvB, dvi, dvRow;
      for (dvi = 0; dvi < Math.min(dvQ, 6); dvi++) {
        dvRow = Math.floor(dvi / 3);
        var dcx = 74 + (dvi % 3) * 78, dcy = 74 + dvRow * 70;
        s += v319circ(dcx, dcy, 30, 'rgba(108,99,245,.10)', ACC, 1.6);
        for (var dvj = 0; dvj < dvB; dvj++) {
          var da = -Math.PI / 2 + dvj * 2 * Math.PI / dvB;
          s += dot(dcx + 17 * Math.cos(da), dcy + 17 * Math.sin(da), ACC, 4.2);
        }
      }
      if (dvQ > 6) s += v319txt(180, 210, 'و ' + (dvQ - 6) + ' گروه دیگر…', MU, 'middle', false, 10);
      for (var dvk = 0; dvk < dvR; dvk++) s += dot(300 + (dvk % 2) * 14, 60 + dvk * 14, PT, 5);
      if (dvR > 0) s += v319txt(320, 30, 'باقی‌مانده', PT, 'middle', true, 10);
      s += v319txt(180, 236, nm(dvA) + ' ÷ ' + nm(dvB) + ' = ' + nm(dvQ) + ' باقی‌ماندهٔ ' + nm(dvR), ST, 'middle', true, 13);
      s += v319txt(180, 258, nm(dvB) + ' × ' + nm(dvQ) + ' + ' + nm(dvR) + ' = ' + nm(dvA), ACC, 'middle', true, 12);
      return wrap(s);
    }

    if (t === 'axshapeid') {
      var shk = xs('kind', 'tri'), shn = Math.max(3, Math.min(8, num(X.n, 5))), shcx = 180, shcy = 128, shr = 80, shv = '', shi;
      if (shk === 'circle') {
        s += v319circ(shcx, shcy, shr, 'rgba(39,196,168,.16)', ACC, 2.2) + dot(shcx, shcy, PT, 3.4);
        var shR = shr;
        s += v319txt(shcx, shcy + (shR > 0 ? -6 : 0), 'مرکز', PT, 'middle', true, 10);
        s += v319txt(180, 232, 'دایره — بی‌ضلع و بی‌زاویه', ST, 'middle', true, 13);
        s += v319txt(180, 254, 'هر نقطه روی دایره به مرکز فاصلهٔ یکسان دارد', MU, 'middle', false, 10);
        return wrap(s);
      }
      var shN = shk === 'tri' ? 3 : shk === 'square' ? 4 : shk === 'rect' ? 4 : shn;
      for (shi = 0; shi < shN; shi++) {
        var sa = -Math.PI / 2 + shi * 2 * Math.PI / shN;
        var sx2 = shcx + shr * Math.cos(sa), sy2 = shcy + shr * Math.sin(sa);
        if (shk === 'rect' && shN === 4) sy2 = shcy + (shr * 0.62) * Math.sin(sa);
        shv += f2(sx2) + ',' + f2(sy2) + ' ';
      }
      s += v319poly(shv.trim(), 'rgba(108,99,245,.16)', ACC, 2.2);
      s += dot(shcx, shcy, PT, 3.4);
      var shName = shk === 'tri' ? 'مثلث' : shk === 'square' ? 'مربع' : shk === 'rect' ? 'مستطیل' : shN + 'ضلعی (چندضلعی)';
      s += v319txt(180, 232, shName + ' — ' + shN + ' ضلع و ' + shN + ' زاویه', ST, 'middle', true, 13);
      s += v319txt(180, 254, shk === 'square' ? 'مربع: چهار ضلع برابر و چهار زاویهٔ ۹۰°' : shk === 'rect' ? 'مستطیل: ضلع‌های روبه‌رو برابر و چهار زاویهٔ ۹۰°' : 'مجموع زاویه‌های داخلی = (' + shN + ' − ۲) × ۱۸۰°', MU, 'middle', false, 10);
      return wrap(s);
    }

    if (t === 'axcircleparts') {
      /* V326 — بازنویسی: همهٔ اجزا دقیقاً روی محیط می‌نشینند و برچسب‌ها بیرون/داخلِ امن می‌روند */
      var cpShow = xs('show', 'all'), cpOn = function (k2) { return cpShow === 'all' || cpShow === k2; };
      var cpx = 180, cpy = 118, cpr = 74;
      s += v319circ(cpx, cpy, cpr, 'rgba(108,99,245,.05)', ST, 2.2);
      s += rc(12, 10, 336, 260, 'none', '#e2e8f0', 1, 10);
      if (cpOn('radius')) {
        var rp = circPt(cpx, cpy, cpr, -42);
        s += lnr(cpx, cpy, rp[0], rp[1], PT, 2.2) + dot(rp[0], rp[1], PT, 3.6);
        s += circLabel(cpx, cpy, cpr, -42, 'شعاع r', PT);
      }
      if (cpOn('diameter')) {
        var dp = circPt(cpx, cpy, cpr, 180), dp2 = circPt(cpx, cpy, cpr, 0);
        s += lnr(dp[0], dp[1], dp2[0], dp2[1], '#1f8f7a', 2.2);
        s += v319txt(cpx - 34, cpy - 9, 'قطر 2r', '#1f8f7a', 'middle', true, 10.5);
      }
      if (cpOn('chord')) {
        var c1 = circPt(cpx, cpy, cpr, 32), c2 = circPt(cpx, cpy, cpr, 148);
        s += lnr(c1[0], c1[1], c2[0], c2[1], GLD, 2.2) + dot(c1[0], c1[1], GLD, 3.4) + dot(c2[0], c2[1], GLD, 3.4);
        s += v319txt(cpx + 6, (c1[1] + c2[1]) / 2 - 8, 'وتر', GLD, 'start', true, 10.5);
      }
      if (cpOn('tangent')) {
        s += lnr(cpx - 96, cpy + cpr, cpx + 96, cpy + cpr, '#8e44ad', 2.2, '6 4');
        s += dot(cpx, cpy + cpr, '#8e44ad', 3.4);
        s += v319txt(cpx + 100, cpy + cpr + 5, 'مماس', '#8e44ad', 'start', true, 10.5);
      }
      if (cpOn('arc')) {
        s += arcOnCircle(cpx, cpy, cpr, 196, 250, PT, 4);
        s += circLabel(cpx, cpy, cpr, 223, 'کمان', PT);
      }
      s += dot(cpx, cpy, ST, 3.6) + v319txt(cpx - 9, cpy + 15, 'O', ST, 'middle', true, 10.5);
      s += v319txt(180, 258, 'اجزای دایره — مرکز، شعاع، قطر، وتر، مماس و کمان', MU, 'middle', true, 10.5);
      return wrap(s);
    }

    if (t === 'axcylinder') {
      var cyk = xs('kind', 'cylinder'), cyr = Math.max(1, num(X.r, 3)), cyh = Math.max(1, num(X.h, 6));
      var cyR = Math.min(66, 12 * cyr), cyH = Math.min(120, 14 * cyh), cycx = 168, cyTop = 168 - cyH, cyBot = 168, cyRy = cyR * 0.34;
      if (cyk === 'cone') {
        s += v319path('M ' + f2(cycx - cyR) + ' ' + f2(cyBot) + ' L ' + cycx + ' ' + f2(cyTop) + ' L ' + f2(cycx + cyR) + ' ' + f2(cyBot), 'rgba(240,162,2,.16)', GLD, 2.2);
        s += v319path('M ' + f2(cycx - cyR) + ' ' + f2(cyBot) + ' A ' + cyR + ' ' + f2(cyRy) + ' 0 0 0 ' + f2(cycx + cyR) + ' ' + f2(cyBot), 'rgba(240,162,2,.10)', GLD, 2.2);
        s += v319path('M ' + f2(cycx - cyR) + ' ' + f2(cyBot) + ' A ' + cyR + ' ' + f2(cyRy) + ' 0 0 1 ' + f2(cycx + cyR) + ' ' + f2(cyBot), 'none', GLD, 1.4, '5 4');
        s += lnr(cycx, cyBot, cycx + cyR, cyBot, PT, 1.6) + v319txt(cycx + cyR / 2, cyBot + 16, 'r = ' + nm(cyr), PT, 'middle', true, 10);
        s += lnr(cycx, cyBot, cycx, cyTop, PT, 1.6, '4 3') + v319txt(cycx - 8, cyTop + cyH / 2, 'h = ' + nm(cyh), PT, 'end', true, 10);
        var cyl = Math.sqrt(cyr * cyr + cyh * cyh);
        s += v319txt(180, 218, 'حجم = ⅓πr²h ≈ ' + nm(Math.round(Math.PI * cyr * cyr * cyh / 3 * 100) / 100), ST, 'middle', true, 12);
        s += v319txt(180, 240, 'سطح کل = πr(r + l)، l = √(r² + h²) ≈ ' + nm(Math.round(cyl * 100) / 100), ACC, 'middle', true, 11);
        s += v319txt(180, 260, 'مخروط — قاعدهٔ دایره‌ای و یک رأس', MU, 'middle', false, 10);
        return wrap(s);
      }
      s += v319path('M ' + f2(cycx - cyR) + ' ' + f2(cyTop) + ' L ' + f2(cycx - cyR) + ' ' + f2(cyBot) + ' M ' + f2(cycx + cyR) + ' ' + f2(cyTop) + ' L ' + f2(cycx + cyR) + ' ' + f2(cyBot), 'none', ACC, 2.2);
      s += v319path('M ' + f2(cycx - cyR) + ' ' + f2(cyBot) + ' A ' + cyR + ' ' + f2(cyRy) + ' 0 0 0 ' + f2(cycx + cyR) + ' ' + f2(cyBot), 'none', ACC, 2.2);
      s += v319path('M ' + f2(cycx - cyR) + ' ' + f2(cyBot) + ' A ' + cyR + ' ' + f2(cyRy) + ' 0 0 1 ' + f2(cycx + cyR) + ' ' + f2(cyBot), 'none', ACC, 1.4, '5 4');
      s += '<ellipse cx="' + f2(cycx) + '" cy="' + f2(cyTop) + '" rx="' + f2(cyR) + '" ry="' + f2(cyRy) + '" fill="rgba(108,99,245,.18)" stroke="' + ACC + '" stroke-width="2"/>';
      s += lnr(cycx, cyTop, cycx + cyR, cyTop, PT, 1.6) + v319txt(cycx + cyR / 2, cyTop - 14, 'r = ' + nm(cyr), PT, 'middle', true, 10);
      s += lnr(cycx + cyR + 18, cyTop, cycx + cyR + 18, cyBot, PT, 1.6, '4 3') + v319txt(cycx + cyR + 24, cyTop + cyH / 2, 'h = ' + nm(cyh), PT, 'start', true, 10);
      s += v319txt(180, 214, 'حجم = πr²h ≈ ' + nm(Math.round(Math.PI * cyr * cyr * cyh * 100) / 100), ST, 'middle', true, 12);
      s += v319txt(180, 236, 'سطح کل = ۲πr(r + h) ≈ ' + nm(Math.round(2 * Math.PI * cyr * (cyr + cyh) * 100) / 100), ACC, 'middle', true, 11);
      s += v319txt(180, 258, 'استوانه — دو قاعدهٔ دایره‌ای موازی', MU, 'middle', false, 10);
      return wrap(s);
    }

    if (t === 'axproportion') {
      var pra = Math.max(0.1, num(X.a, 3)), prb = Math.max(0.1, num(X.b, 4)), prc = Math.max(0.1, num(X.c, 6));
      var prx = prb * prc / pra, prY0 = 96;
      // کسر اول: a / b
      s += v319txt(96, prY0 - 8, nm(pra), ACC, 'middle', true, 20) + lnr(66, prY0, 126, prY0, ST, 2) + v319txt(96, prY0 + 30, nm(prb), ACC, 'middle', true, 20);
      s += v319txt(180, prY0 + 12, '=', ST, 'middle', true, 20);
      // کسر دوم: c / x
      s += v319txt(264, prY0 - 8, nm(prc), '#1f8f7a', 'middle', true, 20) + lnr(234, prY0, 294, prY0, ST, 2) + v319txt(264, prY0 + 30, 'x', PT, 'middle', true, 20);
      s += v319path('M ' + f2(120) + ' ' + f2(prY0 - 22) + ' L ' + f2(250) + ' ' + f2(prY0 + 44), 'none', GLD, 1.3, '4 4');
      s += v319path('M ' + f2(250) + ' ' + f2(prY0 - 22) + ' L ' + f2(120) + ' ' + f2(prY0 + 44), 'none', GLD, 1.3, '4 4');
      s += v319txt(180, 224, 'a × x = b × c  →  x = (b × c) ÷ a', ST, 'middle', true, 12);
      s += v319txt(180, 246, nm(pra) + ' × x = ' + nm(prb) + ' × ' + nm(prc) + '  →  x = ' + nm(Math.round(prx * 100) / 100), ACC, 'middle', true, 12);
      s += v319txt(180, 266, 'تناسب مستقیم — جدول نسبت', MU, 'middle', false, 10);
      return wrap(s);
    }

    if (t === 'axlinear') {
      var lm1 = num(X.m1, 1), lb1 = num(X.b1, 2), lm2 = num(X.m2, -1), lb2 = num(X.b2, 4), lShow = xs('show', 'all').toLowerCase();
      var fmtEq = function (m, b) {
        if (Math.abs(m) < 1e-4) return 'y = ' + nm(b);
        var mT = Math.abs(m - 1) < 1e-4 ? 'x' : (Math.abs(m + 1) < 1e-4 ? '−x' : (m < 0 ? '−' + nm(Math.abs(m)) + 'x' : nm(m) + 'x'));
        if (Math.abs(b) < 1e-4) return 'y = ' + mT;
        return 'y = ' + mT + (b > 0 ? ' + ' + nm(b) : ' − ' + nm(Math.abs(b)));
      };
      // ۱) معادله‌ها کاملاً بیرون از صفحهٔ مختصات (بالای شبکه: y = 6..36)
      if (lShow !== 'none') {
        s += rc(18, 6, 324, 30, '#f8fafc', '#cbd5e1', 1.2, 7);
        s += ln(28, 21, 44, 21, ACC, 2.5) + tx(50, 25, fmtEq(lm1, lb1), ACC, 'start', true, 11);
        s += ln(188, 21, 204, 21, '#0d9488', 2.5) + tx(210, 25, fmtEq(lm2, lb2), '#0d9488', 'start', true, 11);
      }
      // ۲) شبکهٔ مختصات تمیز (y = 44..244)
      var lox = 186, loy = 144, lsc = 16.5;
      for (var li = -7; li <= 7; li++) { var gx = lox + li * lsc; if (gx >= 52 && gx <= 320) s += ln(gx, 44, gx, 244, '#eef1f6', 1); }
      for (var lj = -6; lj <= 6; lj++) { var gy = loy - lj * lsc; if (gy >= 44 && gy <= 244) s += ln(52, gy, 320, gy, '#eef1f6', 1); }
      s += rc(52, 44, 268, 200, 'none', '#cbd5e1', 1.1) + ln(52, loy, 320, loy, ST, 1.5) + ln(lox, 44, lox, 244, ST, 1.5) + arR(320, loy) + arU(lox, 44);
      var lpx = function (v3) { return lox + v3 * lsc; }, lpy = function (v3) { return loy - v3 * lsc; };
      // V322 — شیب/عرض از مبدأ بزرگ به کادر مهار می‌شود
      var lpcx = function (v3) { return clamp(lpx(v3), 46, 326); }, lpcy = function (v3) { return clamp(lpy(v3), 44, 254); };
      var drawClip = function (m, b, col, dash) {
        var pts = [], xMin = -8.12, xMax = 8.12, yMin = -6.06, yMax = 6.06;
        var y1 = m * xMin + b; if (y1 >= yMin && y1 <= yMax) pts.push([xMin, y1]);
        var y2 = m * xMax + b; if (y2 >= yMin && y2 <= yMax) pts.push([xMax, y2]);
        if (Math.abs(m) > 1e-4) {
          var xb = (yMin - b) / m; if (xb >= xMin && xb <= xMax) pts.push([xb, yMin]);
          var xt = (yMax - b) / m; if (xt >= xMin && xt <= xMax) pts.push([xt, yMax]);
        }
        if (pts.length >= 2) s += ln(lpcx(pts[0][0]), lpcy(pts[0][1]), lpcx(pts[pts.length - 1][0]), lpcy(pts[pts.length - 1][1]), col, 2.3, dash || '');
        else s += ln(lpcx(-7), lpcy(m * -7 + b), lpcx(7), lpcy(m * 7 + b), col, 2.3, dash || '');
      };
      var sameM = Math.abs(lm1 - lm2) < 1e-3, sameB = Math.abs(lb1 - lb2) < 1e-3;
      drawClip(lm1, lb1, ACC, '');
      drawClip(lm2, lb2, '#0d9488', (sameM && sameB) ? '6 4' : '');

      var stTxt = '', stCol = '#1e293b';
      if (!sameM) {
        var lix = (lb2 - lb1) / (lm1 - lm2), liy = lm1 * lix + lb1;
        if (lpx(lix) >= 52 && lpx(lix) <= 320 && lpy(liy) >= 44 && lpy(liy) <= 244) s += pfxCls(lpx(lix), lpy(liy), 5, PT, '#ffffff', 1.6);
        stTxt = 'جواب دستگاه (نقطهٔ برخورد):  (' + nm(Math.round(lix * 100) / 100) + ', ' + nm(Math.round(liy * 100) / 100) + ')';
        stCol = PT;
      } else if (sameB) { stTxt = 'دو خط منطبق‌اند — دستگاه بی‌شمار جواب دارد'; stCol = '#0f766e'; }
      else { stTxt = 'دو خط موازی‌اند — دستگاه جواب ندارد'; stCol = '#b45309'; }

      if (lShow === 'all') {
        s += rc(24, 250, 312, 24, '#f8fafc', '#cbd5e1', 1.1, 6) + tx(180, 266, stTxt, stCol, 'middle', true, 11);
      }
      return wrap(s);
    }

    if (t === 'axbarchart') {
      var bcData = xs('data', '4,7,3,6,5').split(/[,،;؛\n]+/).map(function (q2) { return parseInt(faEn(q2.trim()), 10); }).filter(function (q2) { return !isNaN(q2); }).slice(0, 10);
      if (!bcData.length) bcData = [4, 7, 3, 6, 5];
      // V322 — دادهٔ منفی مهار می‌شود
      bcData = bcData.map(function (vbc) { return clamp(vbc, 0, 999); });
      var bcLabs = xl('labs'); if (!bcLabs.length) bcLabs = ['الف', 'ب', 'ج', 'د', 'ه'];
      var bcShow = xs('show', 'mean').toLowerCase();
      var rawMx = Math.max.apply(null, bcData.concat([1]));
      var niceMx = rawMx <= 5 ? 5 : (rawMx <= 10 ? 10 : Math.ceil(rawMx / 5) * 5);
      var bx0 = 50, by0 = 216, bw2 = 282, bh2 = 166, bbar = bw2 / bcData.length * 0.62;
      for (var bt = 0; bt <= 4; bt++) {
        var btv = niceMx * bt / 4, bty = by0 - (btv / niceMx) * bh2;
        s += ln(bx0, bty, bx0 + bw2, bty, bt === 0 ? ST : '#e2e8f0', bt === 0 ? 1.7 : 1) + tx(bx0 - 6, bty + 4, nm(btv), MU, 'end', false, 9);
      }
      s += ln(bx0, by0 - bh2 - 8, bx0, by0, ST, 1.7);
      var bCols = [ACC, GRN, GLD, PT, '#4c9be8', '#9b5de5', '#0d9488', '#ea580c', '#2563eb', '#be185d'];
      bcData.forEach(function (v0, bi) {
        var hh = v0 / niceMx * bh2, xx = bx0 + (bi + 0.5) * (bw2 / bcData.length) - bbar / 2;
        s += rc(xx, by0 - hh, bbar, hh, bCols[bi % bCols.length], ST, 1.2, 3);
        s += tx(xx + bbar / 2, by0 - hh - 5, String(v0), ST, 'middle', true, 10) + tx(xx + bbar / 2, by0 + 16, bcLabs[bi] || String(bi + 1), MU, 'middle', true, 10);
      });
      var bMean = bcData.reduce(function (a, b) { return a + b; }, 0) / bcData.length;
      var bSort = bcData.slice().sort(function (a, b) { return a - b; });
      var bMed = bSort.length % 2 ? bSort[(bSort.length - 1) / 2] : (bSort[bSort.length / 2 - 1] + bSort[bSort.length / 2]) / 2;
      var bRange = bSort[bSort.length - 1] - bSort[0];
      var fMap = {}, maxFq = 1;
      bcData.forEach(function (v0) { fMap[v0] = (fMap[v0] || 0) + 1; if (fMap[v0] > maxFq) maxFq = fMap[v0]; });
      var modes = Object.keys(fMap).filter(function (k0) { return fMap[k0] === maxFq; }).sort(function (a, b) { return Number(a) - Number(b); });
      var modeStr = (maxFq <= 1 && bcData.length > 1) ? 'ندارد' : modes.join('، ');
      var bSd = Math.sqrt(bcData.reduce(function (a, b) { return a + (b - bMean) * (b - bMean); }, 0) / bcData.length);

      if (bcShow === 'mean' || bcShow === 'all') s += ln(bx0, by0 - bMean / niceMx * bh2, bx0 + bw2, by0 - bMean / niceMx * bh2, PT, 1.9, '6 4');
      if (bcShow === 'median' || bcShow === 'all') s += ln(bx0, by0 - bMed / niceMx * bh2, bx0 + bw2, by0 - bMed / niceMx * bh2, '#0d9488', 1.8, '3 3');
      if (bcShow !== 'none') {
        var bSum = bcShow === 'median' ? ('میانه = ' + nm(Math.round(bMed * 100) / 100))
          : bcShow === 'mode' ? ('مد (نما) = ' + modeStr + '   |   دامنهٔ تغییرات R = ' + bRange)
          : bcShow === 'all' ? ('میانگین=' + nm(Math.round(bMean * 10) / 10) + ' ، میانه=' + nm(Math.round(bMed * 10) / 10) + ' ، مد=' + modeStr + ' ، σ=' + nm(Math.round(bSd * 10) / 10))
          : ('میانگین (x̄) = ' + nm(Math.round(bMean * 100) / 100));
        s += rc(18, 240, 324, 28, '#f8fafc', '#cbd5e1', 1.2, 7) + tx(180, 258, bSum, '#1e293b', 'middle', true, 11);
      }
      return wrap(s);
    }
    if (t === 'axparabola') {
      var pqa = num(X.a, 1) || 1, pqb = num(X.b, -2), pqc = num(X.c, -3);
      var pox = 180, poy = 150, psc = 19, pg;
      for (pg = -6; pg <= 6; pg++) s += lnr(pox + pg * psc, 34, pox + pg * psc, 258, GR, 0.6) + lnr(48, poy - pg * psc, 322, poy - pg * psc, GR, 0.6);
      s += lnr(42, poy, 322, poy, ST, 1.8) + arR(322, poy) + lnr(pox, 246, pox, 30, ST, 1.8) + arU(pox, 30);
      s += v319txt(326, poy + 14, 'x', ST, 'middle', true, 12) + v319txt(pox - 12, 28, 'y', ST, 'middle', true, 12);
      var pFX = function (v4) { return pox + v4 * psc; }, pFY = function (v4) { return poy - v4 * psc; }, pqd = '';
      for (pg = -70; pg <= 70; pg++) {
        var pxv = pg / 10, pyv = pqa * pxv * pxv + pqb * pxv + pqc;
        if (Math.abs(pxv) > 6.2 || Math.abs(pyv) > 8) { pqd = ''; continue; }
        pqd += (pqd ? ' L ' : 'M ') + f2(pFX(pxv)) + ' ' + f2(pFY(pyv));
      }
      if (pqd) s += v319path(pqd, 'none', ACC, 2.6);
      var pvx = -pqb / (2 * pqa), pvy = pqa * pvx * pvx + pqb * pvx + pqc;
      if (Math.abs(pvx) <= 6 && Math.abs(pvy) <= 8) {
        s += dot(pFX(pvx), pFY(pvy), PT, 5);
        s += v319txt(clamp(pFX(pvx) + 8, 60, 280), clamp(pFY(pvy) - 10, 40, 250), 'رأس (' + nm(Math.round(pvx * 100) / 100) + ', ' + nm(Math.round(pvy * 100) / 100) + ')', PT, 'start', true, 10);
      }
      if (Math.abs(pvx) <= 6.2) s += lnr(pFX(pvx), 258, pFX(pvx), 34, GLD, 1.4, '5 4');
      var pdel = pqb * pqb - 4 * pqa * pqc, proot = '';
      if (pdel > 0) {
        var pr1 = (-pqb + Math.sqrt(pdel)) / (2 * pqa), pr2 = (-pqb - Math.sqrt(pdel)) / (2 * pqa);
        if (Math.abs(pr1) <= 6) s += dot(pFX(pr1), pFY(0), '#1f8f7a', 4.5);
        if (Math.abs(pr2) <= 6) s += dot(pFX(pr2), pFY(0), '#1f8f7a', 4.5);
        proot = 'ریشه‌ها: ' + nm(Math.round(pr2 * 100) / 100) + ' و ' + nm(Math.round(pr1 * 100) / 100);
      } else if (pdel === 0) proot = 'یک ریشهٔ مضاعف: ' + nm(Math.round(pvx * 100) / 100);
      else proot = 'ریشهٔ حقیقی ندارد (Δ < 0)';
      s += v319txt(180, 262, proot + '  ·  Δ = ' + nm(pdel), MU, 'middle', false, 10);
      var pqSign = function (v5) { v5 = Math.round(v5 * 100) / 100; return (v5 < 0 ? ' − ' + Math.abs(v5) : ' + ' + v5); };
      s += v319txt(60, 40, 'y = ' + (pqa === 1 ? '' : nm(pqa)) + 'x²' + (pqb === 0 ? '' : pqSign(pqb) + 'x') + (pqc === 0 ? '' : pqSign(pqc)), ACC, 'start', true, 12);
      return wrap(s);
    }

    if (t === 'axinthales') {
      var itDeg = Math.max(20, Math.min(160, num(X.ang, 60))), itcx = 180, itcy = 138, itr = 74;
      var ita0 = -Math.PI / 2 - itDeg * Math.PI / 360, ita1 = -Math.PI / 2 + itDeg * Math.PI / 360;
      var itAx = itcx + itr * Math.cos(ita0), itAy = itcy + itr * Math.sin(ita0);
      var itBx = itcx + itr * Math.cos(ita1), itBy = itcy + itr * Math.sin(ita1);
      var itCx = itcx + itr * Math.cos(ita1 + (2 * Math.PI - itDeg * Math.PI / 180) / 2), itCy = itcy + itr * Math.sin(ita1 + (2 * Math.PI - itDeg * Math.PI / 180) / 2);
      s += v319circ(itcx, itcy, itr, 'rgba(108,99,245,.05)', ACC, 2);
      s += v319path('M ' + f2(itAx) + ' ' + f2(itAy) + ' A ' + itr + ' ' + itr + ' 0 0 1 ' + f2(itBx) + ' ' + f2(itBy), 'none', PT, 3.6);
      s += lnr(itcx, itcy, itAx, itAy, ST, 1.8) + lnr(itcx, itcy, itBx, itBy, ST, 1.8);
      s += lnr(itCx, itCy, itAx, itAy, GLD, 1.8) + lnr(itCx, itCy, itBx, itBy, GLD, 1.8);
      s += dot(itcx, itcy, ST, 4) + dot(itAx, itAy, PT, 4.5) + dot(itBx, itBy, PT, 4.5) + dot(itCx, itCy, GLD, 4.5);
      s += v319txt(itcx + 6, itcy + 28, 'O', ST, 'start', true, 11);
      s += v319txt(itAx - 10, itAy - 6, 'A', PT, 'end', true, 11) + v319txt(itBx + 10, itBy - 6, 'B', PT, 'start', true, 11);
      var itcu = [itCx - itcx, itCy - itcy], itcl = Math.hypot(itcu[0], itcu[1]) || 1;
      s += v319txt(itCx + itcu[0] / itcl * 15, itCy + itcu[1] / itcl * 15 + 4, 'C', GLD, 'middle', true, 11);
      s += v319txt(180, 34, 'زاویهٔ مرکزی = ' + nm(itDeg) + '°', PT, 'middle', true, 12);
      s += v319txt(180, 244, 'زاویهٔ محاطی = نصف زاویهٔ مرکزی = ' + nm(Math.round(itDeg / 2 * 10) / 10) + '°', GLD, 'middle', true, 12);
      s += v319txt(180, 264, 'زاویه‌های محاطی روی یک کمان با هم برابرند', MU, 'middle', false, 10);
      return wrap(s);
    }

    if (t === 'axpendulum') {
      var pLenM = Math.max(0.1, num(X.len, 1)), pAmp = clamp(num(X.amp, 24), 5, 65), pG = Math.max(1, num(X.g, 9.8));
      var pMode = xs('mode', 'period').toLowerCase();
      var ppx = 180, ppy = 36, prPx = 148, prad = pAmp * Math.PI / 180;
      var pbx = ppx + prPx * Math.sin(prad), pby = ppy + prPx * Math.cos(prad), plx = ppx - prPx * Math.sin(prad), eqY = ppy + prPx;
      s += ln(116, ppy, 244, ppy, ST, 3);
      for (var hx = 122; hx <= 238; hx += 14) s += ln(hx, ppy, hx - 7, ppy - 9, '#64748b', 1.3);
      s += pfxCls(ppx, ppy, 4.5, ST, '#ffffff', 1.3);
      // مسیر کمان واقعی حرکت گلوله حول نقطهٔ تعادل پایین
      s += ln(ppx, ppy, ppx, eqY + 14, '#94a3b8', 1.4, '5 4');
      s += pfxPath('M ' + f2(plx) + ' ' + f2(pby) + ' A ' + f2(prPx) + ' ' + f2(prPx) + ' 0 0 0 ' + f2(pbx) + ' ' + f2(pby), 'none', ACC, 1.7, '5 3');
      s += pfxCls(plx, pby, 10, 'rgba(148,163,184,.18)', '#94a3b8', 1.2) + pfxCls(ppx, eqY, 10, 'rgba(39,196,168,.15)', GRN, 1.2);
      var rAng = 42;
      s += pfxPath('M ' + f2(ppx) + ' ' + f2(ppy + rAng) + ' A ' + f2(rAng) + ' ' + f2(rAng) + ' 0 0 0 ' + f2(ppx + rAng * Math.sin(prad)) + ' ' + f2(ppy + rAng * Math.cos(prad)), 'none', '#d97706', 2);
      s += tx(ppx + rAng * Math.sin(prad / 2) + 8, ppy + rAng * Math.cos(prad / 2) + 12, 'θ=' + nm(pAmp) + '°', '#b45309', 'start', true, 10);
      s += ln(ppx, ppy, pbx, pby, ST, 2.2) + tx((ppx + pbx) / 2 + 10, (ppy + pby) / 2 - 4, 'L = ' + nm(pLenM) + ' m', ST, 'start', true, 10);

      if (pMode === 'forces') {
        s += vecArrow(pbx, pby, pbx, pby + 42, ACC, 2.1, 7) + tx(pbx + 6, pby + 44, 'mg', ACC, 'start', true, 10);
        var ux = (ppx - pbx) / prPx, uy = (ppy - pby) / prPx;
        s += vecArrow(pbx, pby, pbx + ux * 40, pby + uy * 40, '#0d9488', 2.1, 7) + tx(pbx + ux * 40 - 8, pby + uy * 40, 'T', '#0d9488', 'end', true, 10);
        var txTan = -Math.cos(prad), tyTan = Math.sin(prad);
        s += vecArrow(pbx, pby, pbx + txTan * 34, pby + tyTan * 34, PT, 2, 6.5) + tx(pbx + txTan * 36 - 4, pby + tyTan * 36 + 10, 'mg·sinθ', PT, 'end', true, 9);
      } else if (pMode === 'energy') {
        s += ln(ppx - 10, pby, pbx, pby, '#0d9488', 1.3, '3 3') + tx(ppx - 14, (pby + eqY) / 2 + 4, 'h', '#0d9488', 'end', true, 11);
      }
      s += pfxCls(pbx, pby, 13, PT, ST, 1.9);
      var pT = Math.round(2 * Math.PI * Math.sqrt(pLenM / pG) * 100) / 100, hRise = Math.round(pLenM * (1 - Math.cos(prad)) * 100) / 100;
      if (pMode !== 'quiz') {
        var btmP = pMode === 'forces' ? ('نیروی بازگرداننده: F = mg·sinθ   |   دوره: T ≈ ' + pT + ' s')
          : pMode === 'energy' ? ('اوج ارتفاع: h = L(1−cosθ) ≈ ' + hRise + ' m   |   T ≈ ' + pT + ' s')
          : ('دورهٔ نوسان:  T = 2π√(L/g) ≈ ' + pT + ' s');
        s += rc(20, 240, 320, 28, '#f8fafc', '#cbd5e1', 1.2, 7) + tx(180, 258, btmP, '#1e293b', 'middle', true, 11);
      }
      return wrap(s);
    }

    if (t === 'axincline') {
      var ia = clamp(num(X.ang, 30), 10, 70), im = Math.max(0.5, num(X.mass, 5)), imu = clamp(num(X.mu, 0), 0, 1.2), ig = Math.max(1, num(X.g, 9.8));
      var iShow = xs('show', 'all').toLowerCase(), irad = ia * Math.PI / 180, cosA = Math.cos(irad), sinA = Math.sin(irad);
      var axI = 48, yBase = 216, hypI = Math.min(236, Math.min(228 / cosA, 146 / sinA));
      var bxI = axI + hypI * cosA, cyPeak = yBase - hypI * sinA;
      s += ln(30, yBase, 332, yBase, '#94a3b8', 1.5);
      s += pfxPoly(f2(axI) + ',' + f2(yBase) + ' ' + f2(bxI) + ',' + f2(yBase) + ' ' + f2(bxI) + ',' + f2(cyPeak), '#f1f5f9', ST, 2.2);
      s += rc(bxI - 11, yBase - 11, 11, 11, 'none', ST, 1.3);
      var rArcI = 34;
      s += pfxPath('M ' + f2(axI + rArcI) + ' ' + f2(yBase) + ' A ' + f2(rArcI) + ' ' + f2(rArcI) + ' 0 0 0 ' + f2(axI + rArcI * cosA) + ' ' + f2(yBase - rArcI * sinA), 'none', '#d97706', 2);
      s += tx(axI + (rArcI + 14) * Math.cos(irad / 2), yBase - (rArcI + 12) * Math.sin(irad / 2) + 3, nm(ia) + '°', '#b45309', 'middle', true, 10);

      var uUpX = cosA, uUpY = -sinA, uNormX = -sinA, uNormY = -cosA;
      var pxI = axI + 0.54 * (bxI - axI), pyI = yBase + 0.54 * (cyPeak - yBase), bwI = 42, bhI = 26;
      var p1x = pxI - (bwI / 2) * uUpX, p1y = pyI - (bwI / 2) * uUpY, p2x = pxI + (bwI / 2) * uUpX, p2y = pyI + (bwI / 2) * uUpY;
      s += pfxPoly(f2(p1x) + ',' + f2(p1y) + ' ' + f2(p2x) + ',' + f2(p2y) + ' ' + f2(p2x + bhI * uNormX) + ',' + f2(p2y + bhI * uNormY) + ' ' + f2(p1x + bhI * uNormX) + ',' + f2(p1y + bhI * uNormY), 'rgba(108,99,245,.20)', ACC, 2);
      var cmX = pxI + (bhI / 2) * uNormX, cmY = pyI + (bhI / 2) * uNormY;
      s += pfxCls(cmX, cmY, 3, ST, 'none', 0);

      if (iShow !== 'plain') {
        var vMg = 52, vN = vMg * cosA, vPar = Math.max(22, vMg * sinA);
        s += vecArrow(cmX, cmY, cmX, cmY + vMg, ACC, 2.2, 7) + tx(cmX + 6, cmY + vMg + 4, 'mg', ACC, 'start', true, 10);
        s += vecArrow(cmX, cmY, cmX + vN * uNormX, cmY + vN * uNormY, '#0d9488', 2.2, 7) + tx(cmX + (vN + 11) * uNormX, cmY + (vN + 11) * uNormY + 3, 'N', '#0d9488', 'middle', true, 11);
        s += vecArrow(cmX, cmY, cmX - vN * uNormX, cmY - vN * uNormY, '#64748b', 1.5, 6, '4 3');
        s += vecArrow(cmX, cmY, cmX - vPar * uUpX, cmY - vPar * uUpY, PT, 2.3, 7) + tx(cmX - (vPar + 8) * uUpX - 4, cmY - (vPar + 8) * uUpY + 4, 'mg·sinθ', PT, 'end', true, 10);
        if (imu > 0.001) {
          var vF = Math.min(vPar, Math.max(20, imu * vN));
          s += vecArrow(cmX, cmY, cmX + vF * uUpX, cmY + vF * uUpY, '#dc2626', 2.1, 6.5) + tx(cmX + (vF + 8) * uUpX + 4, cmY + (vF + 8) * uUpY, 'f', '#dc2626', 'start', true, 11);
        }
      } else s += tx(cmX, cmY + 4, 'm', ST, 'middle', true, 11);

      if (iShow === 'all') {
        var fPar = im * ig * sinA, nForce = im * ig * cosA, acc = Math.max(0, (fPar - imu * nForce) / im);
        var sumI = imu > 0.001 ? ('m=' + nm(im) + 'kg ، μ=' + nm(imu) + '  |  N≈' + nm(Math.round(nForce * 10) / 10) + 'N ، a≈' + nm(Math.round(acc * 10) / 10) + ' m/s²')
          : ('m=' + nm(im) + 'kg  |  mg·sinθ≈' + nm(Math.round(fPar * 10) / 10) + 'N ، N≈' + nm(Math.round(nForce * 10) / 10) + 'N ، a≈' + nm(Math.round(acc * 10) / 10) + ' m/s²');
        s += rc(16, 240, 328, 28, '#f8fafc', '#cbd5e1', 1.2, 7) + tx(180, 258, sumI, '#1e293b', 'middle', true, 10);
      }
      return wrap(s);
    }

    if (t === 'axcircuit') {
      var cm = xs('mode', 'series').toLowerCase(), cr1 = Math.max(0.5, num(X.r1, 4)), cr2 = Math.max(0.5, num(X.r2, 6)), cr3 = Math.max(0, num(X.r3, 0));
      var cv = Math.max(0.5, num(X.v, 12)), cShow = xs('show', 'all').toLowerCase(), creq = 0;
      var drawBat = function (bx, cy) { return ln(bx, cy - 16, bx, cy + 16, PT, 3.2) + ln(bx - 7, cy - 9, bx - 7, cy + 9, ST, 2.2) + tx(bx + 6, cy - 18, '+', PT, 'start', true, 11) + tx(bx - 16, cy + 24, nm(cv) + 'V', PT, 'middle', true, 11); };
      var drawAmp = function (ax, ay) { return pfxCls(ax, ay, 12, '#ffffff', '#0d9488', 1.8) + tx(ax, ay + 4, 'A', '#0d9488', 'middle', true, 11); };
      if (cm === 'parallel') {
        var has3P = cr3 >= 0.5;
        creq = has3P ? (1 / (1 / cr1 + 1 / cr2 + 1 / cr3)) : ((cr1 * cr2) / (cr1 + cr2));
        s += ln(54, 44, 298, 44, ST, 2) + ln(54, 206, 298, 206, ST, 2) + ln(54, 44, 54, 112, ST, 2) + ln(54, 138, 54, 206, ST, 2);
        s += drawBat(54, 125) + drawAmp(96, 44);
        var bYs = has3P ? [76, 126, 176] : [92, 160], bRs = has3P ? [cr1, cr2, cr3] : [cr1, cr2];
        s += ln(136, 44, 136, 206, ST, 2) + ln(298, 44, 298, 206, ST, 2);
        bYs.forEach(function (by, idx) {
          s += ln(136, by, 182, by, ST, 2) + ln(252, by, 298, by, ST, 2);
          s += rc(182, by - 12, 70, 24, 'rgba(108,99,245,.15)', ACC, 1.9, 4) + tx(217, by + 4, 'R' + (idx + 1) + '=' + nm(bRs[idx]) + 'Ω', ST, 'middle', true, 10);
        });
      } else if (cm === 'mixed') {
        var r3E = cr3 >= 0.5 ? cr3 : 6;
        creq = cr1 + (cr2 * r3E) / (cr2 + r3E);
        s += ln(52, 54, 104, 54, ST, 2) + rc(104, 42, 64, 24, 'rgba(108,99,245,.15)', ACC, 1.9, 4) + tx(136, 58, 'R1=' + nm(cr1) + 'Ω', ST, 'middle', true, 10) + ln(168, 54, 196, 54, ST, 2);
        s += ln(196, 34, 196, 78, ST, 2) + ln(298, 34, 298, 202, ST, 2);
        [[34, 'R2', cr2], [78, 'R3', r3E]].forEach(function (br) {
          s += ln(196, br[0], 214, br[0], ST, 2) + rc(214, br[0] - 11, 66, 22, 'rgba(39,196,168,.16)', '#0d9488', 1.8, 4) + tx(247, br[0] + 4, br[1] + '=' + nm(br[2]) + 'Ω', ST, 'middle', true, 9.5) + ln(280, br[0], 298, br[0], ST, 2);
        });
        s += ln(52, 202, 298, 202, ST, 2) + ln(52, 54, 52, 202, ST, 2) + drawBat(156, 202) + drawAmp(246, 202);
      } else {
        var has3S = cr3 >= 0.5;
        creq = has3S ? (cr1 + cr2 + cr3) : (cr1 + cr2);
        if (has3S) {
          s += ln(50, 64, 78, 64, ST, 2) + rc(78, 51, 60, 26, 'rgba(108,99,245,.15)', ACC, 1.9, 4) + tx(108, 68, 'R1=' + nm(cr1) + 'Ω', ST, 'middle', true, 9.5);
          s += ln(138, 64, 156, 64, ST, 2) + rc(156, 51, 60, 26, 'rgba(108,99,245,.15)', ACC, 1.9, 4) + tx(186, 68, 'R2=' + nm(cr2) + 'Ω', ST, 'middle', true, 9.5);
          s += ln(216, 64, 234, 64, ST, 2) + rc(234, 51, 60, 26, 'rgba(108,99,245,.15)', ACC, 1.9, 4) + tx(264, 68, 'R3=' + nm(cr3) + 'Ω', ST, 'middle', true, 9.5) + ln(294, 64, 312, 64, ST, 2);
        } else {
          s += ln(50, 64, 94, 64, ST, 2) + rc(94, 50, 72, 28, 'rgba(108,99,245,.15)', ACC, 1.9, 4) + tx(130, 68, 'R1 = ' + nm(cr1) + 'Ω', ST, 'middle', true, 10);
          s += ln(166, 64, 200, 64, ST, 2) + rc(200, 50, 72, 28, 'rgba(108,99,245,.15)', ACC, 1.9, 4) + tx(236, 68, 'R2 = ' + nm(cr2) + 'Ω', ST, 'middle', true, 10) + ln(272, 64, 312, 64, ST, 2);
        }
        s += ln(50, 64, 50, 198, ST, 2) + ln(312, 64, 312, 198, ST, 2) + ln(50, 198, 152, 198, ST, 2) + ln(166, 198, 240, 198, ST, 2) + ln(264, 198, 312, 198, ST, 2);
        s += ln(152, 182, 152, 214, ST, 2.2) + ln(166, 174, 166, 222, PT, 3.2) + tx(159, 168, nm(cv) + 'V', PT, 'middle', true, 11) + drawAmp(252, 198);
      }
      var curI = cv / creq, powW = cv * curI;
      if (cShow !== 'given') {
        s += rc(16, 238, 328, 30, '#f8fafc', '#cbd5e1', 1.2, 7) + tx(180, 258, 'R = ' + nm(Math.round(creq * 100) / 100) + ' Ω   |   I = ' + nm(Math.round(curI * 100) / 100) + ' A   |   P = ' + nm(Math.round(powW * 10) / 10) + ' W', '#1e293b', 'middle', true, 11);
      }
      return wrap(s);
    }

    if (t === 'axoptics') {
      var opKind = xs('kind', 'convex').toLowerCase(), opShow = xs('show', 'all').toLowerCase();
      // V322 — حفاظت تکینگی جسم روی کانون و مقیاس پویا
      var opF = clamp(num(X.f, 10), 2, 25), opD = clamp(num(X.d, 30), 2, 60);
      var opAtFocus = opKind !== 'concave' && Math.abs(opD - opF) < 0.05;
      var olx = 180, ooy = 128, osc = clamp(130 / Math.max(opD, 2 * opF, 20), 2.0, 5.2);
      var fPx = opF * osc, dPx = opD * osc, topY = 48, botY = 208;
      s += ln(16, ooy, 344, ooy, '#64748b', 1.4);
      if (opKind !== 'concave') {
        s += pfxPath('M ' + olx + ' ' + topY + ' Q ' + (olx + 16) + ' ' + ooy + ' ' + olx + ' ' + botY + ' Q ' + (olx - 16) + ' ' + ooy + ' ' + olx + ' ' + topY + ' Z', 'rgba(56,189,248,.18)', '#0284c7', 2);
        s += pfxPoly(olx + ',' + (topY - 4) + ' ' + (olx - 5) + ',' + (topY + 5) + ' ' + (olx + 5) + ',' + (topY + 5), '#0284c7');
        s += pfxPoly(olx + ',' + (botY + 4) + ' ' + (olx - 5) + ',' + (botY - 5) + ' ' + (olx + 5) + ',' + (botY - 5), '#0284c7');
      } else {
        s += pfxPath('M ' + (olx - 11) + ' ' + topY + ' L ' + (olx + 11) + ' ' + topY + ' Q ' + (olx + 3) + ' ' + ooy + ' ' + (olx + 11) + ' ' + botY + ' L ' + (olx - 11) + ' ' + botY + ' Q ' + (olx - 3) + ' ' + ooy + ' ' + (olx - 11) + ' ' + topY + ' Z', 'rgba(56,189,248,.18)', '#0284c7', 2);
      }
      s += ln(olx, topY, olx, botY, '#0284c7', 1, '3 3') + tx(olx + 5, ooy + 14, 'O', MU, 'start', true, 9);
      [[olx - fPx, 'F'], [olx + fPx, "F'"], [olx - 2 * fPx, '2F'], [olx + 2 * fPx, "2F'"]].forEach(function (fp) {
        if (fp[0] >= 22 && fp[0] <= 338) s += pfxCls(fp[0], ooy, 3, '#d97706', 'none', 0) + tx(fp[0], ooy + 15, fp[1], '#b45309', 'middle', true, 9);
      });
      var objX = clamp(olx - dPx, 24, olx - 14), objH = 44, objTop = ooy - objH;
      s += vecArrow(objX, ooy, objX, objTop, '#0d9488', 2.6, 7.5) + tx(objX, ooy + 26, 'جسم', '#0f766e', 'middle', true, 9);

      var fSigned = opKind === 'concave' ? -opF : opF;
      var odi = opAtFocus ? 9999 : (1 / (1 / fSigned - 1 / opD)), mag = opAtFocus ? 0 : (-odi / opD);
      if (opShow !== 'blank') {
        s += ln(objX, objTop, olx, objTop, '#ea580c', 1.8);
        if (opAtFocus) {
          var slp = objH / fPx;
          s += ln(olx, objTop, 340, objTop + slp * (340 - olx), '#ea580c', 1.8) + ln(objX, objTop, 340, ooy + slp * (340 - olx), PT, 1.7);
        } else if (opKind !== 'concave' && odi > 0) {
          var imgX = clamp(olx + odi * osc, olx + 14, 336), imgTipY = ooy + clamp(objH * Math.abs(mag), 10, 74);
          s += ln(olx, objTop, imgX, imgTipY, '#ea580c', 1.8) + ln(objX, objTop, imgX, imgTipY, PT, 1.7);
          if (opShow === 'all') s += vecArrow(imgX, ooy, imgX, imgTipY, ACC, 2.5, 7);
        } else if (opKind !== 'concave' && odi < 0) {
          var imgXV = clamp(olx + odi * osc, 20, olx - 10), imgTipV = ooy - clamp(objH * Math.abs(mag), 18, 76);
          var sl1 = objH / fPx, sl2 = objH / (olx - objX);
          s += ln(olx, objTop, 338, clamp(objTop + sl1 * (338 - olx), 36, 224), '#ea580c', 1.8) + ln(olx, objTop, imgXV, imgTipV, '#ea580c', 1.5, '4 3');
          s += ln(objX, objTop, 338, clamp(ooy + sl2 * (338 - olx), 36, 224), PT, 1.7) + ln(objX, objTop, imgXV, imgTipV, PT, 1.5, '4 3');
          if (opShow === 'all') s += vecArrow(imgXV, ooy, imgXV, imgTipV, ACC, 2.3, 6.5, '4 3');
        } else {
          var fLeftX = olx - fPx, slDiv = (objTop - ooy) / (olx - fLeftX), slO = (ooy - objTop) / (olx - objX);
          s += ln(olx, objTop, 338, clamp(objTop + slDiv * (338 - olx), 34, 220), '#ea580c', 1.8) + ln(olx, objTop, Math.max(20, fLeftX), ooy, '#ea580c', 1.5, '4 3');
          s += ln(objX, objTop, 338, clamp(ooy + slO * (338 - olx), 34, 224), PT, 1.7);
          var imgXC = clamp(olx + odi * osc, fLeftX + 4, olx - 6), imgHC = clamp(objH * Math.abs(mag), 10, objH - 4);
          if (opShow === 'all') s += vecArrow(imgXC, ooy, imgXC, ooy - imgHC, ACC, 2.3, 6, '4 3');
        }
      }
      var opDiTxt = opAtFocus ? 'بی‌نهایت' : (nm(Math.round(odi * 10) / 10) + ' cm');
      var kInfo = opAtFocus ? 'پرتوهای خروجی موازی' : (odi > 0 ? 'حقیقی و وارونه' : 'مجازی و مستقیم');
      if (opShow === 'all') {
        s += rc(14, 240, 332, 28, '#f8fafc', '#cbd5e1', 1.2, 7) + tx(180, 258, 'f=' + nm(opF) + 'cm ، p=' + nm(opD) + 'cm  →  q = ' + opDiTxt + ' (' + kInfo + ')', '#1e293b', 'middle', true, 10);
      } else {
        s += tx(180, 258, 'عدسی ' + (opKind === 'concave' ? 'مقعر (واگرا)' : 'محدب (همگرا)') + ':  f = ' + nm(opF) + ' cm ، p = ' + nm(opD) + ' cm', MU, 'middle', true, 11);
      }
      return wrap(s);
    }

    if (t === 'axheatcurve') {
      var ht0 = clamp(num(X.t0, -20), -60, -2), ht1 = clamp(num(X.t1, 120), 105, 180), hm = Math.max(1, num(X.m, 100));
      var hMode = xs('mode', 'full').toLowerCase(), hx0 = 52, hy0 = 212, hw = 278, hh = 166;
      var hpx = function (u) { return hx0 + u / 100 * hw; };
      var hpy = function (v) { return hy0 - clamp((v - (ht0 - 5)) / ((ht1 + 10) - (ht0 - 5)), 0, 1) * hh; };
      s += ln(hx0, hpy(0), hx0 + hw, hpy(0), '#cbd5e1', 1.2, '4 4') + ln(hx0, hpy(100), hx0 + hw, hpy(100), '#cbd5e1', 1.2, '4 4');
      s += ln(hx0, hy0, hx0 + hw + 8, hy0, ST, 1.8) + arR(hx0 + hw + 8, hy0) + ln(hx0, hy0 - hh - 10, hx0, hy0, ST, 1.8) + arU(hx0, hy0 - hh - 10);
      s += tx(hx0 - 6, hpy(ht0) + 4, nm(ht0) + '°', MU, 'end', true, 9) + tx(hx0 - 6, hpy(0) + 4, '0°C', '#0284c7', 'end', true, 10) + tx(hx0 - 6, hpy(100) + 4, '100°C', PT, 'end', true, 10) + tx(hx0 + 8, hy0 - hh - 2, 'دما (°C)', MU, 'start', true, 10);
      var hpts = [[0, ht0], [14, 0], [36, 0], [60, 100], [88, 100], [100, ht1]];
      for (var hi = 0; hi < hpts.length - 1; hi++) {
        var phC = hi === 1 || hi === 3;
        s += ln(hpx(hpts[hi][0]), hpy(hpts[hi][1]), hpx(hpts[hi + 1][0]), hpy(hpts[hi + 1][1]), phC ? PT : ACC, 2.8);
        s += pfxCls(hpx(hpts[hi][0]), hpy(hpts[hi][1]), 3.2, phC ? PT : ACC, '#ffffff', 1);
      }
      s += pfxCls(hpx(100), hpy(ht1), 3.2, ACC, '#ffffff', 1);
      if (hMode === 'quiz') {
        s += tx(hpx(7), hpy(ht0 / 2) - 8, '(۱)', ST, 'middle', true, 10) + tx(hpx(25), hpy(0) - 8, '(۲)', PT, 'middle', true, 10) + tx(hpx(48), hpy(50) - 8, '(۳)', ST, 'middle', true, 10) + tx(hpx(74), hpy(100) - 8, '(۴)', PT, 'middle', true, 10) + tx(hpx(94), hpy((100 + ht1) / 2) - 8, '(۵)', ST, 'middle', true, 10);
        s += tx(180, 254, 'گرمای کل Q — مراحل (۱) تا (۵) منحنی گرمایش (' + nm(hm) + 'g آب) را مشخص کنید', MU, 'middle', true, 10);
      } else {
        s += tx(hpx(8), hpy(ht0 / 2) - 8, 'یخ', '#0284c7', 'middle', true, 10) + tx(hpx(25), hpy(0) - 8, hMode === 'full' ? 'ذوب (mLf)' : 'ذوب', PT, 'middle', true, 9) + tx(hpx(47), hpy(52) - 8, 'آب', ACC, 'middle', true, 10) + tx(hpx(74), hpy(100) - 8, hMode === 'full' ? 'جوش (mLv)' : 'تبخیر', PT, 'middle', true, 9) + tx(hpx(94), hpy((100 + ht1) / 2) - 8, 'بخار', '#d97706', 'middle', true, 10);
        var qTot = (hm * 2.1 * Math.abs(ht0) + hm * 334 + hm * 4.18 * 100 + hm * 2260 + hm * 2.0 * (ht1 - 100)) / 1000;
        s += rc(20, 236, 320, 28, '#f8fafc', '#cbd5e1', 1.2, 7) + tx(180, 254, 'جرم m = ' + nm(hm) + 'g  |  گرمای کل ذوب و تبخیر: Q ≈ ' + nm(Math.round(qTot * 10) / 10) + ' kJ', '#1e293b', 'middle', true, 10);
      }
      return wrap(s);
    }

    if (t === 'axdna') {
      var dm = xs('mode', 'helix').toLowerCase(), dLbl = xs('label', '1') !== '0';
      var dseq = xs('seq', 'ATGCGATC').toUpperCase().replace(/[^ATGC]/g, '').slice(0, 10) || 'ATGC';
      var dpair = function (b) { return b === 'A' ? 'T' : b === 'T' ? 'A' : b === 'G' ? 'C' : 'G'; };
      var rpair = function (b) { return b === 'A' ? 'U' : b === 'T' ? 'A' : b === 'G' ? 'C' : 'G'; };
      var bCol = function (b) { return b === 'A' ? ACC : b === 'T' ? PT : b === 'G' ? '#0d9488' : b === 'C' ? '#d97706' : '#ea580c'; };
      var hbCnt = function (b) { return (b === 'G' || b === 'C') ? 3 : 2; };
      var totHb = dseq.split('').reduce(function (a, b) { return a + hbCnt(b); }, 0);
      var gcPct = Math.round(dseq.split('').filter(function (b) { return b === 'G' || b === 'C'; }).length * 100 / dseq.length);

      if (dm === 'flat' || dm === 'mrna' || dm === 'quiz') {
        var isRna = dm === 'mrna', isQz = dm === 'quiz', nD = dseq.length, xLD = 38, xRD = 322, stD = (xRD - xLD) / (nD + 0.4), yTD = 66, yBD = 178;
        s += ln(xLD, yTD, xRD, yTD, ST, 3.2) + ln(xLD, yBD, xRD, yBD, isRna ? '#ea580c' : ST, 3.2);
        s += tx(22, yTD + 4, "5'", ACC, 'middle', true, 11) + tx(340, yTD + 4, "3'", ACC, 'middle', true, 11);
        s += tx(22, yBD + 4, "3'", PT, 'middle', true, 11) + tx(340, yBD + 4, "5'", PT, 'middle', true, 11);
        for (var di = 0; di < nD; di++) {
          var dx = xLD + (di + 0.7) * stD, b1 = dseq[di], b2 = isRna ? rpair(b1) : dpair(b1);
          var offs = hbCnt(b1) === 3 ? [-3.5, 0, 3.5] : [-2.2, 2.2];
          offs.forEach(function (ddx) { s += ln(dx + ddx, yTD + 26, dx + ddx, yBD - 26, '#94a3b8', 1.3, '3 2'); });
          s += rc(dx - 12, yTD + 4, 24, 22, '#ffffff', bCol(b1), 1.8, 5) + tx(dx, yTD + 19, b1, bCol(b1), 'middle', true, 12);
          if (isQz) s += rcDash(dx - 12, yBD - 26, 24, 22, '#f8fafc', '#4f46e5', 1.5, 5, '4 2');
          else s += rc(dx - 12, yBD - 26, 24, 22, '#ffffff', bCol(b2), 1.8, 5) + tx(dx, yBD - 11, b2, bCol(b2), 'middle', true, 12);
        }
        if (dLbl) {
          var ftD = isQz ? 'رشتهٔ مکمل را بنویسید (A=T با ۲ پیوند و G≡C با ۳ پیوند هیدروژنی)'
            : isRna ? ('رونویسی DNA به mRNA (جایگزینی U به‌جای T) — GC = ' + gcPct + '%')
            : ('جفت‌بازهای مکمل (5\'→3\' / 3\'→5\'):  مجموع پیوند هیدروژنی = ' + totHb + '  |  GC = ' + gcPct + '%');
          s += rc(14, 228, 332, 30, '#f8fafc', '#cbd5e1', 1.2, 7) + tx(180, 248, ftD, '#1e293b', 'middle', true, 10);
        }
        return wrap(s);
      }

      s += tx(34, 34, "5'", ACC, 'middle', true, 11) + tx(326, 34, "3'", PT, 'middle', true, 11);
      var dp1 = '', dp2 = '';
      for (var dpt = 0; dpt <= 54; dpt++) {
        var dt = dpt / 54, dxx = 36 + dt * 288, dy1 = 122 + 46 * Math.sin(dt * 2.2 * Math.PI), dy2 = 122 - 46 * Math.sin(dt * 2.2 * Math.PI);
        dp1 += (dpt === 0 ? 'M ' : ' L ') + f2(dxx) + ' ' + f2(dy1);
        dp2 += (dpt === 0 ? 'M ' : ' L ') + f2(dxx) + ' ' + f2(dy2);
      }
      var rungs = clamp(dseq.length, 4, 10);
      for (var dk = 0; dk < rungs; dk++) {
        var dtk = (dk + 0.5) / rungs, dxk = 36 + dtk * 288, dsk = Math.sin(dtk * 2.2 * Math.PI), dy1k = 122 + 46 * dsk, dy2k = 122 - 46 * dsk;
        var b1k = dseq[dk % dseq.length], b2k = dpair(b1k), ym = (dy1k + dy2k) / 2;
        s += ln(dxk, dy1k, dxk, ym - 2, bCol(b1k), 2.6) + ln(dxk, ym + 2, dxk, dy2k, bCol(b2k), 2.6);
        if (Math.abs(dy1k - dy2k) > 26) {
          s += rc(dxk - 8, dy1k + (dy2k > dy1k ? 4 : -18), 16, 14, '#ffffff', bCol(b1k), 1, 3) + tx(dxk, dy1k + (dy2k > dy1k ? 14 : -8), b1k, bCol(b1k), 'middle', true, 9);
          s += rc(dxk - 8, dy2k + (dy1k > dy2k ? 4 : -18), 16, 14, '#ffffff', bCol(b2k), 1, 3) + tx(dxk, dy2k + (dy1k > dy2k ? 14 : -8), b2k, bCol(b2k), 'middle', true, 9);
        }
      }
      s += pfxPath(dp1, 'none', ACC, 3.2) + pfxPath(dp2, 'none', PT, 3.2);
      if (dLbl) {
        s += rc(16, 232, 328, 30, '#f8fafc', '#cbd5e1', 1.2, 7) + tx(180, 252, 'مارپیچ دوگانهٔ DNA (' + dseq + ') — پیوند هیدروژنی: ' + totHb + '  |  GC: ' + gcPct + '%', '#1e293b', 'middle', true, 10);
      }
      return wrap(s);
    }
    if (t === 'axcell') {
      var celk = xs('kind', 'animal'), celPlant = celk === 'plant';
      s += v319path('M 72 152 Q 76 66 180 62 Q 284 66 288 152 Q 284 236 180 240 Q 76 236 72 152 Z', celPlant ? 'rgba(39,196,168,.10)' : 'rgba(108,99,245,.08)', celPlant ? '#1f8f7a' : ACC, 2.6);
      s += v319circ(150, 138, 34, 'rgba(108,99,245,.22)', ACC, 2);
      s += v319txt(150, 142, 'هسته', ST, 'middle', true, 10);
      s += v319txt(150, 156, 'DNA', MU, 'middle', false, 8);
      s += '<ellipse cx="236" cy="112" rx="26" ry="13" fill="rgba(240,162,2,.35)" stroke="' + GLD + '" stroke-width="1.6" transform="rotate(-18 236 112)"/>';
      s += v319txt(236, 116, 'میتوکندری', ST, 'middle', true, 8);
      if (celPlant) {
        s += '<ellipse cx="214" cy="196" rx="34" ry="22" fill="rgba(39,196,168,.25)" stroke="#1f8f7a" stroke-width="1.6"/>';
        s += v319txt(214, 200, 'واکوئل', ST, 'middle', true, 9);
        s += '<ellipse cx="112" cy="200" rx="18" ry="10" fill="rgba(39,196,168,.4)" stroke="#1f8f7a" stroke-width="1.4" transform="rotate(24 112 200)"/>';
        s += v319txt(112, 204, 'کلروپلاست', ST, 'middle', true, 8);
        s += rc(70, 60, 220, 12, 'rgba(240,162,2,.25)', '#8a5b00', 1.4, 4) + v319txt(180, 70, 'دیوارهٔ سلولی', ST, 'middle', true, 9);
      } else {
        s += '<ellipse cx="232" cy="200" rx="26" ry="18" fill="rgba(214,51,108,.18)" stroke="' + PT + '" stroke-width="1.6"/>';
        s += v319txt(232, 204, 'واکوئل کوچک', ST, 'middle', true, 8);
      }
      s += v319txt(180, 258, celPlant ? 'سلول گیاهی — دیوارهٔ سلولی، کلروپلاست و واکوئل بزرگ' : 'سلول جانوری — غشا، هسته و میتوکندری', ST, 'middle', true, 11);
      s += v319txt(180, 274, 'هر اندامک کار مشخصی دارد', MU, 'middle', false, 9);
      return wrap(s);
    }

    if (t === 'axtimeline') {
      var tlItems = xl('events'), tli;
      if (!tlItems.length) tlItems = ['۵۵۹:سقوط ساسانیان', '۶۲۲:هجرت پیامبر', '۱۰۴۰:سلجوقیان', '۱۲۸۰:ایلخانان', '۱۵۰۱:صفویان'];
      tlItems = tlItems.slice(0, 6);
      var tlN = tlItems.length, tlX0 = 56, tlX1 = 320, tlY = 148;
      s += lnr(tlX0 - 12, tlY, tlX1 + 12, tlY, ST, 2.4) + arR(tlX1 + 12, tlY);
      for (tli = 0; tli < tlN; tli++) {
        var tip = tlItems[tli].split(/[:：]/), tlYear = (tip[0] || '').trim(), tlLab = (tip[1] || '').trim();
        var tlpx = tlN === 1 ? (tlX0 + tlX1) / 2 : tlX0 + (tlX1 - tlX0) * tli / (tlN - 1);
        var tlUp = tli % 2 === 0;
        s += dot(tlpx, tlY, tli % 2 ? GLD : ACC, 5.5);
        s += lnr(tlpx, tlY, tlpx, tlUp ? tlY - 34 : tlY + 34, GR, 1.4, '4 4');
        s += v319txt(tlpx, tlUp ? tlY - 44 : tlY + 50, tlYear, ST, 'middle', true, 11);
        s += v319txt(tlpx, tlUp ? tlY - 62 : tlY + 68, tlLab, MU, 'middle', false, 9);
      }
      s += v319txt(180, 258, 'خط زمان تاریخی — ترتیب رویدادها از چپ به راست', ST, 'middle', true, 11);
      return wrap(s);
    }

    if (t === 'axmapgeo') {
      var mgKind = xs('kind', 'compass');
      if (mgKind === 'map') {
        for (i = 0; i <= 6; i++) {
          s += lnr(66 + i * 38, 54, 66 + i * 38, 220, GR, 0.8) + lnr(66, 54 + i * 27.7, 294, 54 + i * 27.7, GR, 0.8);
        }
        s += rc(60, 48, 240, 178, 'none', ST, 1.8);
        s += v319poly('176,84 214,96 232,140 208,184 168,196 146,152 152,106', 'rgba(39,196,168,.28)', '#1f8f7a', 1.8);
        s += dot(196, 140, PT, 5) + v319txt(206, 136, 'تهران', ST, 'start', true, 10);
        s += v319txt(180, 244, 'نقشه — موقعیت و مختصات جغرافیایی', ST, 'middle', true, 11);
        s += lnr(60, 268, 140, 268, ST, 2.4) + lnr(60, 264, 60, 272, ST, 1.6) + lnr(140, 264, 140, 272, ST, 1.6);
        s += v319txt(100, 262, 'مقیاس ۱۰۰ کیلومتر', MU, 'middle', false, 9);
        return wrap(s);
      }
      var mgcx = 180, mgcy = 146, mgr = 62;
      s += v319circ(mgcx, mgcy, mgr, 'rgba(108,99,245,.06)', ACC, 2);
      var mgDirs = [['شمال', -Math.PI / 2], ['شمال‌شرق', -Math.PI / 4], ['شرق', 0], ['جنوب‌شرق', Math.PI / 4], ['جنوب', Math.PI / 2], ['جنوب‌غرب', 3 * Math.PI / 4], ['غرب', Math.PI], ['شمال‌غرب', 5 * Math.PI / 4]];
      for (i = 0; i < 8; i++) {
        var mda = mgDirs[i][1], mdx = mgcx + mgr * Math.cos(mda), mdy = mgcy + mgr * Math.sin(mda);
        s += lnr(mgcx, mgcy, mdx, mdy, i % 2 ? GR : ST, i % 2 ? 1 : 1.8);
        s += v319txt(mgcx + (mgr + 22) * Math.cos(mda), mgcy + (mgr + 22) * Math.sin(mda) + 3, mgDirs[i][0], i === 0 ? PT : MU, 'middle', i === 0, 9);
      }
      s += v319poly('180,86 187,152 180,144 173,152', PT, PT, 1) + v319poly('180,206 173,140 180,148 187,140', ACC, ACC, 1);
      s += v319txt(180, 246, 'جهت‌های جغرافیایی — شمال بالا، جنوب پایین', ST, 'middle', true, 11);
      s += v319txt(180, 266, 'شمال ← شرق ← جنوب ← غرب (در جهت عقربه‌های ساعت)', MU, 'middle', false, 10);
      return wrap(s);
    }

    if (t === 'axsentence') {
      var snSubj = xs('subj', 'کودکان'), snVerb = xs('verb', 'بازی می‌کنند'), snObj = xs('obj', ''), snMode = xs('mode', 'verbal');
      var snX = 180, snY = 96;
      s += rc(40, 66, 120, 52, 'rgba(108,99,245,.16)', ACC, 1.8, 10) + v319txt(100, 88, snSubj, ST, 'middle', true, 13) + v319txt(100, 106, 'نهاد', MU, 'middle', false, 9);
      s += rc(200, 66, 132, 52, 'rgba(39,196,168,.18)', '#1f8f7a', 1.8, 10);
      s += v319txt(266, 88, snVerb, ST, 'middle', true, 13) + v319txt(266, 106, snMode === 'nominal' ? 'گزاره (مسند)' : 'گزاره (فعل)', MU, 'middle', false, 9);
      s += lnr(162, 92, 198, 92, ST, 1.8) + arR(198, 92);
      if (snObj) {
        s += rc(110, 150, 140, 46, 'rgba(240,162,2,.18)', GLD, 1.8, 10) + v319txt(180, 170, snObj, ST, 'middle', true, 12) + v319txt(180, 187, 'مفعول', MU, 'middle', false, 9);
        s += lnr(180, 120, 180, 148, GLD, 1.6, '5 4');
      }
      s += v319txt(180, 218, 'نهاد + گزاره = جملهٔ کامل', ST, 'middle', true, 12);
      s += v319txt(180, 240, snMode === 'nominal' ? 'جملهٔ اسمیه: نهاد + مسند (بودن حذف می‌شود)' : 'جملهٔ فعلیه: نهاد + فعل (شناسه با نهاد مطابق است)', MU, 'middle', false, 10);
      s += v319txt(180, 262, snSubj + ' ' + (snObj ? snObj + ' ' : '') + snVerb, ACC, 'middle', true, 12);
      return wrap(s);
    }

    if (t === 'axvectors') {
      var v1x = num(X.ax1, 3), v1y = num(X.ay1, 1), v2x = num(X.bx, 1), v2y = num(X.by, 2), vmode = xs('mode', 'sum');
      var vox = 170, voy = 176, vsc = 26;
      s += lnr(56, voy, 320, voy, ST, 1.6) + arR(320, voy) + lnr(vox, 264, vox, 46, ST, 1.6) + arU(vox, 46);
      s += v319txt(324, voy + 14, 'x', ST, 'middle', true, 11) + v319txt(vox - 12, 44, 'y', ST, 'middle', true, 11);
      for (i = -4; i <= 5; i++) {
        if (i === 0) continue;
        s += v319txt(vox + i * vsc, voy + 13, String(i), MU, 'middle', false, 8) + v319txt(vox - 10, voy - i * vsc + 3, String(i), MU, 'end', false, 8);
      }
      // V322 — مؤلفهٔ بزرگ، نوک پیکان را با مختصات ده‌ها هزار بیرون از بوم می‌کشید؛ حالا در کادر مهار می‌شود.
      var vAx = clamp(vox + v1x * vsc, 56, 318), vAy = clamp(voy - v1y * vsc, 48, 258), vBx2 = clamp(vox + v2x * vsc, 56, 318), vBy2 = clamp(voy - v2y * vsc, 48, 258);
      var vRx = clamp(vmode === 'diff' ? vox + (v1x - v2x) * vsc : vox + (v1x + v2x) * vsc, 56, 318);
      var vRy = clamp(vmode === 'diff' ? voy - (v1y - v2y) * vsc : voy - (v1y + v2y) * vsc, 48, 258);
      s += lnr(vox, voy, vRx, vRy, 'rgba(39,196,168,.35)', 8);
      s += lnr(vox, voy, vAx, vAy, ACC, 2.4) + poly(f2(vAx) + ',' + f2(vAy) + ' ' + f2(vAx - 9) + ',' + f2(vAy + 4) + ' ' + f2(vAx - 5) + ',' + f2(vAy + 10), ACC);
      s += lnr(vox, voy, vBx2, vBy2, GLD, 2.4) + poly(f2(vBx2) + ',' + f2(vBy2) + ' ' + f2(vBx2 - 9) + ',' + f2(vBy2 + 4) + ' ' + f2(vBx2 - 5) + ',' + f2(vBy2 + 10), GLD);
      s += lnr(vox, voy, vRx, vRy, PT, 2.6) + poly(f2(vRx) + ',' + f2(vRy) + ' ' + f2(vRx - 10) + ',' + f2(vRy + 4) + ' ' + f2(vRx - 6) + ',' + f2(vRy + 11), PT);
      s += v319txt(vAx + 8, vAy - 8, 'a', ACC, 'start', true, 12) + v319txt(vBx2 + 8, vBy2 - 8, 'b', GLD, 'start', true, 12);
      s += v319txt(vRx + 8, vRy - 14, vmode === 'diff' ? 'a − b' : 'a + b', PT, 'start', true, 12);
      s += v319txt(180, 252, (vmode === 'diff' ? 'تفاضل' : 'جمع') + ' بردارها = (' + nm(vmode === 'diff' ? v1x - v2x : v1x + v2x) + ', ' + nm(vmode === 'diff' ? v1y - v2y : v1y + v2y) + ')', ST, 'middle', true, 12);
      s += v319txt(180, 272, 'اندازهٔ بردار = √(x² + y²)', MU, 'middle', false, 10);
      return wrap(s);
    }

    if (t === 'axmatrix') {
      var mxA = xs('a', '1,2;3,4').split(';'), mxB = xs('b', '5,6;7,8').split(';');
      var mxGet = function (rows, r, c) { var rr = (rows[r] || '').split(/[,،]/); return parseFloat((rr[c] || '0').trim()) || 0; };
      var mxc1 = mxGet(mxA, 0, 0) * mxGet(mxB, 0, 0) + mxGet(mxA, 0, 1) * mxGet(mxB, 1, 0);
      var mxc2 = mxGet(mxA, 0, 0) * mxGet(mxB, 0, 1) + mxGet(mxA, 0, 1) * mxGet(mxB, 1, 1);
      var mxd1 = mxGet(mxA, 1, 0) * mxGet(mxB, 0, 0) + mxGet(mxA, 1, 1) * mxGet(mxB, 1, 0);
      var mxd2 = mxGet(mxA, 1, 0) * mxGet(mxB, 0, 1) + mxGet(mxA, 1, 1) * mxGet(mxB, 1, 1);
      var mdrawMat = function (x0, y0, vals, col, tag) {
        var out2 = '', rr2, cc2;
        for (rr2 = 0; rr2 < 2; rr2++) for (cc2 = 0; cc2 < 2; cc2++) out2 += v319txt(x0 + 22 + cc2 * 32, y0 + 22 + rr2 * 26, nm(Math.round(vals[rr2][cc2] * 100) / 100), ST, 'middle', true, 12);
        out2 += v319txt(x0 + 5, y0 + 22, '[', col, 'middle', true, 26) + v319txt(x0 + 71, y0 + 22, ']', col, 'middle', true, 26);
        out2 += v319txt(x0 + 38, y0 + 66, tag, col, 'middle', false, 9);
        return out2;
      };
      s += mdrawMat(28, 92, [[mxGet(mxA, 0, 0), mxGet(mxA, 0, 1)], [mxGet(mxA, 1, 0), mxGet(mxA, 1, 1)]], ACC, 'A');
      s += v319txt(118, 122, '×', ST, 'middle', true, 16);
      s += mdrawMat(132, 92, [[mxGet(mxB, 0, 0), mxGet(mxB, 0, 1)], [mxGet(mxB, 1, 0), mxGet(mxB, 1, 1)]], GLD, 'B');
      s += v319txt(222, 122, '=', ST, 'middle', true, 16);
      s += mdrawMat(238, 92, [[mxc1, mxc2], [mxd1, mxd2]], PT, 'A×B');
      s += v319txt(180, 202, 'درایهٔ (۱،۱): ' + nm(mxGet(mxA, 0, 0)) + '×' + nm(mxGet(mxB, 0, 0)) + ' + ' + nm(mxGet(mxA, 0, 1)) + '×' + nm(mxGet(mxB, 1, 0)) + ' = ' + nm(mxc1), ST, 'middle', true, 11);
      s += v319txt(180, 224, 'ضرب ماتریس: سطر × ستون و جمع حاصل‌ضرب‌ها', MU, 'middle', false, 10);
      s += v319txt(180, 246, 'مرتبهٔ حاصل = ۲×۲  ·  A دارای ۲ سطر و B دارای ۲ ستون', MU, 'middle', false, 10);
      s += v319txt(180, 268, 'det(A×B) = det(A) × det(B)', ACC, 'middle', true, 10);
      return wrap(s);
    }

    if (t === 'axintegral') {
      // V322 — بازهٔ منفی/بزرگ نقاط سایه‌زنی را بیرون از بوم می‌گذاشت.
      var inf = xs('f', 'quad'), inA = clamp(num(X.lo, 0), 0, 20), inB = clamp(num(X.hi, 2), 0, 20);
      if (inB <= inA) inB = inA + 1;
      var inFn = function (v7) { return inf === 'sin' ? Math.sin(v7) : inf === 'line' ? 0.6 * v7 + 0.4 : 0.25 * v7 * v7; };
      var inX0 = 62, inY0 = 212, inW = 250, inH = 140, inMaxV = Math.max(inB, 1);
      var inX = function (v7) { return inX0 + v7 / inMaxV * inW; }, inY = function (v7) { return inY0 - v7 / (inFn(inMaxV) || 1) * inH; };
      s += lnr(inX0 - 8, inY0, inX0 + inW + 8, inY0, ST, 1.6) + lnr(inX0 - 8, inY0, inX0 - 8, 56, ST, 1.6);
      var inShade = '', inn = 40, ink;
      for (ink = 0; ink <= inn; ink++) {
        var ixv = inA + (inB - inA) * ink / inn;
        inShade += f2(inX(ixv)) + ',' + f2(inY0) + ' ' + f2(inX(ixv)) + ',' + f2(inY(inFn(ixv))) + ' ';
      }
      s += '<polygon points="' + inShade + f2(inX(inB)) + ',' + f2(inY0) + '" fill="rgba(39,196,168,.28)"/>';
      var inCurve = '';
      for (ink = 0; ink <= 80; ink++) { var ixv2 = inMaxV * ink / 80; inCurve += (ink ? ' L ' : 'M ') + f2(inX(ixv2)) + ' ' + f2(inY(inFn(ixv2))); }
      s += v319path(inCurve, 'none', ACC, 2.4);
      s += lnr(inX(inA), inY0, inX(inA), inY(inFn(inA)), PT, 1.6, '5 4') + lnr(inX(inB), inY0, inX(inB), inY(inFn(inB)), PT, 1.6, '5 4');
      s += v319txt(inX(inA), inY0 + 16, 'a = ' + nm(inA), PT, 'middle', true, 10) + v319txt(inX(inB), inY0 + 16, 'b = ' + nm(inB), PT, 'middle', true, 10);
      var inSum = 0, inN2 = 200, inj;
      for (inj = 0; inj < inN2; inj++) inSum += inFn(inA + (inB - inA) * (inj + 0.5) / inN2) * (inB - inA) / inN2;
      s += v319txt(180, 238, '∫ f(x) dx از ' + nm(inA) + ' تا ' + nm(inB) + ' ≈ ' + nm(Math.round(inSum * 1000) / 1000), ST, 'middle', true, 12);
      s += v319txt(180, 258, 'انتگرال معین = مساحت زیر نمودار (با علامت)', MU, 'middle', false, 10);
      s += v319txt(180, 274, 'روش: تقسیم به مستطیل‌های باریک و جمع مساحت‌ها', MU, 'middle', false, 9);
      return wrap(s);
    }

    if (t === 'axflowchart') {
      var fcSteps = xl('steps');
      if (!fcSteps.length) fcSteps = ['شروع', 'ورودی: n', 'n > 0 ؟', 'چاپ n', 'پایان'];
      fcSteps = fcSteps.slice(0, 5);
      var fcy = 30;
      for (i = 0; i < fcSteps.length; i++) {
        var fcLab = fcSteps[i], fcKind = i === 0 ? 'start' : i === fcSteps.length - 1 ? 'end' : /[؟?]/.test(fcLab) ? 'dec' : 'proc';
        var fcH = fcKind === 'dec' ? 42 : 24;
        if (fcKind === 'start' || fcKind === 'end') {
          s += '<rect x="112" y="' + fcy + '" width="136" height="' + fcH + '" rx="13" fill="' + (fcKind === 'start' ? 'rgba(39,196,168,.25)' : 'rgba(214,51,108,.18)') + '" stroke="' + (fcKind === 'start' ? '#1f8f7a' : PT) + '" stroke-width="1.8"/>';
        } else if (fcKind === 'dec') {
          s += v319poly('180,' + fcy + ' 248,' + (fcy + fcH / 2) + ' 180,' + (fcy + fcH) + ' 112,' + (fcy + fcH / 2), 'rgba(240,162,2,.22)', GLD, 1.8);
        } else {
          s += rc(112, fcy, 136, fcH, 'rgba(108,99,245,.16)', ACC, 1.8, 5);
        }
        s += v319txt(180, fcy + fcH / 2 + 4, fcLab, ST, 'middle', true, 10);
        fcy += fcH;
        if (i < fcSteps.length - 1) {
          s += lnr(180, fcy, 180, fcy + 14, ST, 1.6) + poly(f2(175.5) + ',' + f2(fcy + 8) + ' ' + f2(184.5) + ',' + f2(fcy + 8) + ' ' + f2(180) + ',' + f2(fcy + 15), ST);
          fcy += 15;
        }
      }
      s += v319txt(180, 258, 'بیضی = شروع/پایان · مستطیل = پردازش · لوزی = شرط', MU, 'middle', true, 10);
      return wrap(s);
    }

    if (t === 'axstack') {
      var stMode = xs('mode', 'stack'), stItems = ilist('items', '3,5,8'), stOp = xs('op', 'push');
      if (!stItems.length) stItems = [3, 5, 8];
      if (stMode === 'queue') {
        s += rc(64, 88, 232, 52, 'none', ST, 2) + lnr(64, 88, 64, 140, ST, 2) + lnr(296, 88, 296, 140, ST, 2);
        for (i = 0; i < stItems.length; i++) {
          s += rc(74 + i * 56, 96, 48, 36, 'rgba(39,196,168,.20)', '#1f8f7a', 1.6, 5) + v319txt(98 + i * 56, 120, String(stItems[i]), ST, 'middle', true, 13);
        }
        s += v319txt(76, 168, 'خروج (front)', PT, 'start', true, 10) + lnr(88, 156, 88, 140, PT, 1.6);
        s += v319txt(284, 168, 'ورود (rear)', GRN, 'end', true, 10) + lnr(284, 156, 284, 140, GRN, 1.6);
        s += v319txt(180, 210, 'صف (Queue) — اولین ورودی، اولین خروجی (FIFO)', ST, 'middle', true, 12);
        s += v319txt(180, 232, 'افزودن از انتها و حذف از ابتدا', MU, 'middle', false, 10);
      } else {
        s += lnr(120, 76, 120, 216, ST, 2) + lnr(240, 76, 240, 216, ST, 2) + lnr(120, 216, 240, 216, ST, 2);
        for (i = 0; i < stItems.length; i++) {
          var sy2 = 216 - (i + 1) * 34;
          s += rc(122, sy2, 116, 32, i === stItems.length - 1 ? 'rgba(214,51,108,.22)' : 'rgba(108,99,245,.16)', i === stItems.length - 1 ? PT : ACC, 1.6, 4);
          s += v319txt(180, sy2 + 21, String(stItems[i]), ST, 'middle', true, 13);
        }
        s += v319txt(272, 96, stOp === 'pop' ? 'pop → ' + stItems[stItems.length - 1] : 'push', PT, 'start', true, 11);
        s += v319txt(180, 244, 'پشته (Stack) — آخرین ورودی، اولین خروجی (LIFO)', ST, 'middle', true, 12);
        s += v319txt(180, 266, stOp === 'pop' ? 'حذف فقط از بالای پشته ممکن است' : 'افزودن فقط به بالای پشته ممکن است', MU, 'middle', false, 10);
      }
      return wrap(s);
    }

    if (t === 'axsort') {
      var soVals = ilist('vals', '5,3,8,1'), soStep = clamp(Math.floor(num(X.step, 0)), 0, 4), soI, soJ;
      if (!soVals.length) soVals = [5, 3, 8, 1];
      for (soI = 0; soI < soStep; soI++) {
        for (soJ = 0; soJ < soVals.length - 1 - soI; soJ++) {
          if (soVals[soJ] > soVals[soJ + 1]) { var soT = soVals[soJ]; soVals[soJ] = soVals[soJ + 1]; soVals[soJ + 1] = soT; }
        }
      }
      var soBw = Math.min(54, 220 / Math.max(1, soVals.length)), soX0 = 180 - (soVals.length * soBw) / 2;
      // V322 — پیش‌تر ارتفاع = ۲۶ + |v|×۶ بود و مقدار ۹۹۹ میله را هزاران پیکسل بالا می‌برد.
      var soMaxAbs = 1; soVals.forEach(function (vso) { soMaxAbs = Math.max(soMaxAbs, Math.abs(vso)); });
      for (soI = 0; soI < soVals.length; soI++) {
        var soh = 26 + Math.abs(soVals[soI]) / soMaxAbs * 110, soC = soStep === 0 && soI < 2 ? 'rgba(214,51,108,.22)' : 'rgba(108,99,245,.18)';
        s += rc(soX0 + soI * soBw + 3, 200 - soh, soBw - 6, soh, soC, soStep === 0 && soI < 2 ? PT : ACC, 1.8, 5);
        s += v319txt(soX0 + soI * soBw + soBw / 2, 200 - soh / 2 + 5, String(soVals[soI]), ST, 'middle', true, 14);
      }
      s += lnr(soX0, 200, soX0 + soVals.length * soBw, 200, ST, 2);
      s += v319txt(180, 226, 'مرتب‌سازی حبابی — دور ' + nm(soStep) + ' از ' + nm(Math.max(1, soVals.length - 1)), ST, 'middle', true, 12);
      s += v319txt(180, 248, 'در هر دور بزرگ‌ترین عدد به انتها می‌رود', MU, 'middle', false, 10);
      s += v319txt(180, 268, 'تعداد مقایسه‌ها ≈ n(n−1)/2 = ' + nm(Math.round(soVals.length * (soVals.length - 1) / 2)), MU, 'middle', false, 10);
      return wrap(s);
    }

    if (t === 'axregression') {
      var rgPts = pairs(xs('pts', '1,2;2,3;3,5;4,4;5,6')); if (!rgPts.length) rgPts = [[1, 2], [2, 3], [3, 5], [4, 4], [5, 6]];
      var rgN = rgPts.length, rgsx = 0, rgsy = 0, rgI;
      for (rgI = 0; rgI < rgN; rgI++) { rgsx += rgPts[rgI][0]; rgsy += rgPts[rgI][1]; }
      var rgmx = rgsx / rgN, rgmy = rgsy / rgN, rgsxy = 0, rgsxx = 0, rgsyy = 0;
      for (rgI = 0; rgI < rgN; rgI++) { rgsxy += (rgPts[rgI][0] - rgmx) * (rgPts[rgI][1] - rgmy); rgsxx += Math.pow(rgPts[rgI][0] - rgmx, 2); rgsyy += Math.pow(rgPts[rgI][1] - rgmy, 2); }
      var rgb = rgsxx ? rgsxy / rgsxx : 0, rga = rgmy - rgb * rgmx, rgr = (rgsxx && rgsyy) ? rgsxy / Math.sqrt(rgsxx * rgsyy) : 0;
      var rgX0 = 62, rgY0 = 210, rgW = 250, rgH = 148, rgMaxX = 1, rgMaxY = 1;
      for (rgI = 0; rgI < rgN; rgI++) { rgMaxX = Math.max(rgMaxX, rgPts[rgI][0]); rgMaxY = Math.max(rgMaxY, rgPts[rgI][1]); }
      var rgX = function (v8) { return rgX0 + v8 / (rgMaxX * 1.15) * rgW; }, rgY = function (v8) { return rgY0 - v8 / (rgMaxY * 1.15) * rgH; };
      s += lnr(rgX0 - 8, rgY0, rgX0 + rgW + 8, rgY0, ST, 1.6) + lnr(rgX0 - 8, rgY0, rgX0 - 8, 54, ST, 1.6);
      s += lnr(rgX(0), rgY(rga), rgX(rgMaxX * 1.15), rgY(rga + rgb * rgMaxX * 1.15), ACC, 2.4);
      for (rgI = 0; rgI < rgN; rgI++) {
        var rpx = rgX(rgPts[rgI][0]), rpy = rgY(rgPts[rgI][1]);
        s += dot(rpx, rpy, PT, 4.5) + lnr(rpx, rpy, rpx, rgY(rga + rgb * rgPts[rgI][0]), GR, 1, '3 3');
      }
      s += v319txt(180, 232, 'y = ' + nm(Math.round(rgb * 100) / 100) + 'x + ' + nm(Math.round(rga * 100) / 100) + '   ·   r = ' + nm(Math.round(rgr * 100) / 100), ST, 'middle', true, 12);
      s += v319txt(180, 252, 'رگرسیون کمترین مربعات — خط بهترین برازش', MU, 'middle', false, 10);
      s += v319txt(180, 272, 'r² = ' + nm(Math.round(rgr * rgr * 100) / 100) + '  (نزدیک به ۱ یعنی برازش خوب)', MU, 'middle', false, 10);
      return wrap(s);
    }

    return null;
  }
  function svgOf(spec) {
    spec = spec || {};
    var t = spec.t || 'line';
    var X = spec.X || {};
    var title = X.title || '';
    var head = title ? '<text class="ttl" x="180" y="14" text-anchor="middle">' + esc(title) + '</text>' : '';


    // V137.2 — ۲۱ نوع تازهٔ محور: منبع واحد رندر، AxisSvgRenderer.kt است؛ پیش‌نمایش چاپ از پل
    // بومی (ExamPrintBridge.renderFigure → data:image/svg+xml) همان SVG را می‌گیرد تا خروجی
    // سازنده/چاپ یکسان بماند. بدون پل (تست مرورگر) به نزدیک‌ترین محور پایه برمی‌گردد.
    if (AXIS_EXT.indexOf(t) >= 0) {
      try {
        if (window.ExamPrintBridge && typeof ExamPrintBridge.renderFigure === 'function') {
          var url = ExamPrintBridge.renderFigure(JSON.stringify(spec));
          // V137.3 — SVG بومی به‌جای <image> مستقیم درج می‌شود تا کادر ۸دستگیره بتواند به اندازهٔ
          // خودِ محتوا تنگ شود (tightenFigSvgs با getBBox) و زمینه/حاشیهٔ خالی نداشته باشد.
          if (url && String(url).indexOf('data:image/svg+xml;base64,') === 0) {
            try {
              var xml = decodeURIComponent(escape(atob(String(url).slice('data:image/svg+xml;base64,'.length))));
              if (xml.indexOf('<svg') >= 0) return xml.slice(xml.indexOf('<svg'));
            } catch (_d) {}
          }
          if (url && String(url).indexOf('data:') === 0) {
            return '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 360 280" width="320" height="250" overflow="hidden">' +
              '<image href="' + esc(url) + '" x="0" y="0" width="360" height="280" preserveAspectRatio="xMidYMid meet"/></svg>';
          }
        }
      } catch (_e) {}
      // V247 — بدون پل (سایت): همان رندر، پورت‌شده به JS
      try { var exSvg = axisExSvg(t, X, title); if (exSvg) return exSvg; } catch (_x) {}
      t = t.indexOf('axnum') === 0 ? 'axnum' : t === 'axpolpts' ? 'axpol' : t === 'ax3dpt' ? 'ax3d' : t === 'axtime' ? 'axnum' : 'axxy';
    }

    // V136 — محورها (axnum/axxy/axq1/axgrid/axpol/ax3d)؛ پورت یک‌به‌یک AxisSvgRenderer.kt
    if (t === 'axnum' || t === 'axxy' || t === 'axq1' || t === 'axgrid' || t === 'axpol' || t === 'ax3d') {
      var AL = 40, AT = 26, AR = 336, AB = 246, ST = '#2c3a50', GR = '#d5dce6', MU = '#4a5870';
      function f2(v) { return String(Math.round(v * 100) / 100); }
      function ln(x1, y1, x2, y2, c, w) { return '<line x1="' + f2(x1) + '" y1="' + f2(y1) + '" x2="' + f2(x2) + '" y2="' + f2(y2) + '" stroke="' + c + '" stroke-width="' + w + '"/>'; }
      function tx(x, y, s, c, an, bold, sz) { return '<text x="' + f2(x) + '" y="' + f2(y) + '" font-family="sans-serif" font-size="' + (sz || 11) + '"' + (bold ? ' font-weight="700"' : '') + ' fill="' + c + '" text-anchor="' + an + '" style="font:' + (bold ? '700 ' : '400 ') + (sz || 11) + 'px sans-serif">' + esc(s) + '</text>'; }
      function arR(x, y) { return '<polygon points="' + f2(x) + ',' + f2(y) + ' ' + f2(x - 8) + ',' + f2(y - 4.5) + ' ' + f2(x - 8) + ',' + f2(y + 4.5) + '" fill="' + ST + '"/>'; }
      function arL(x, y) { return '<polygon points="' + f2(x) + ',' + f2(y) + ' ' + f2(x + 8) + ',' + f2(y - 4.5) + ' ' + f2(x + 8) + ',' + f2(y + 4.5) + '" fill="' + ST + '"/>'; }
      function arU(x, y) { return '<polygon points="' + f2(x) + ',' + f2(y) + ' ' + f2(x - 4.5) + ',' + f2(y + 8) + ' ' + f2(x + 4.5) + ',' + f2(y + 8) + '" fill="' + ST + '"/>'; }
      function nm(v) { return (v === Math.round(v)) ? String(v) : v.toFixed(1); }
      function stepOf(span) { var st = num(X.step, 0); if (st > 0) return st; return span <= 12 ? 1 : span <= 30 ? 2 : span <= 60 ? 5 : 10; }
      var hAx = title ? tx(180, 14, title, '#1a2433', 'middle', true, 13) : '';
      var i, d;
      if (t === 'axnum') {
        var nmin = num(X.xmin, -5), nmax = num(X.xmax, 5); if (nmax <= nmin) nmax = nmin + 2;
        var nst = stepOf(nmax - nmin), ny = 140;
        var npx = function (x) { return AL + (x - nmin) / (nmax - nmin) * (AR - AL); };
        hAx += ln(AL - 12, ny, AR + 12, ny, ST, 2) + arR(AR + 12, ny) + arL(AL - 12, ny);
        for (var nv = Math.ceil(nmin / nst) * nst; nv <= nmax + 1e-4; nv += nst) {
          var nz = Math.abs(nv) < 1e-4;
          hAx += ln(npx(nv), ny - (nz ? 10 : 7), npx(nv), ny + (nz ? 10 : 7), ST, nz ? 2 : 1.4);
          hAx += tx(npx(nv), ny + 24, nm(nv), MU, 'middle', false, 11);
        }
        return wrap(hAx);
      }
      if (t === 'axxy' || t === 'axq1') {
        var q1 = t === 'axq1';
        var cxmin = q1 ? 0 : num(X.xmin, -5), cxmax = num(X.xmax, q1 ? 10 : 5); if (cxmax <= cxmin) cxmax = cxmin + 2;
        var cymin = q1 ? 0 : num(X.ymin, -4), cymax = num(X.ymax, q1 ? 8 : 4); if (cymax <= cymin) cymax = cymin + 2;
        var sx = stepOf(cxmax - cxmin), sy = stepOf(cymax - cymin);
        var cpx = function (x) { return AL + (x - cxmin) / (cxmax - cxmin) * (AR - AL); };
        var cpy = function (y) { return AB - (y - cymin) / (cymax - cymin) * (AB - AT); };
        var ox = Math.min(AR, Math.max(AL, cpx(0))), oy = Math.min(AB, Math.max(AT, cpy(0)));
        var g;
        for (g = Math.ceil(cxmin / sx) * sx; g <= cxmax + 1e-4; g += sx) hAx += ln(cpx(g), AT, cpx(g), AB, GR, 0.8);
        for (g = Math.ceil(cymin / sy) * sy; g <= cymax + 1e-4; g += sy) hAx += ln(AL, cpy(g), AR, cpy(g), GR, 0.8);
        hAx += ln(AL - 8, oy, AR + 8, oy, ST, 1.8) + arR(AR + 8, oy);
        hAx += ln(ox, AB + 8, ox, AT - 8, ST, 1.8) + arU(ox, AT - 8);
        hAx += tx(AR + 4, oy - 8, 'x', ST, 'end', true, 12) + tx(ox + 8, AT - 2, 'y', ST, 'start', true, 12);
        for (g = Math.ceil(cxmin / sx) * sx; g <= cxmax + 1e-4; g += sx) {
          if (Math.abs(g) > 1e-4) hAx += ln(cpx(g), oy - 4, cpx(g), oy + 4, ST, 1.2) + tx(cpx(g), oy + 15, nm(g), MU, 'middle', false, 10);
        }
        for (g = Math.ceil(cymin / sy) * sy; g <= cymax + 1e-4; g += sy) {
          if (Math.abs(g) > 1e-4) hAx += ln(ox - 4, cpy(g), ox + 4, cpy(g), ST, 1.2) + tx(ox - 7, cpy(g) + 4, nm(g), MU, 'end', false, 10);
        }
        hAx += tx(ox - 6, oy + 14, '0', MU, 'end', false, 10);
        return wrap(hAx);
      }
      if (t === 'axgrid') {
        var cols = Math.min(40, Math.max(2, Math.floor(num(X.xmax, 10)))), rows = Math.min(40, Math.max(2, Math.floor(num(X.ymax, 8))));
        var cell = Math.min((AR - AL) / cols, (AB - AT) / rows), gw = cell * cols, gh = cell * rows;
        var x0 = (360 - gw) / 2, y0 = AT + ((AB - AT) - gh) / 2;
        hAx += '<rect x="' + f2(x0) + '" y="' + f2(y0) + '" width="' + f2(gw) + '" height="' + f2(gh) + '" fill="#fbfcfe" stroke="' + ST + '" stroke-width="1.4"/>';
        for (i = 1; i < cols; i++) hAx += ln(x0 + i * cell, y0, x0 + i * cell, y0 + gh, GR, 0.9);
        for (i = 1; i < rows; i++) hAx += ln(x0, y0 + i * cell, x0 + gw, y0 + i * cell, GR, 0.9);
        return wrap(hAx);
      }
      if (t === 'axpol') {
        var rings = Math.min(12, Math.max(1, Math.floor(num(X.xmax, 4)))), pcx = 180, pcy = (AT + AB) / 2;
        var rMax = Math.min(AR - AL, AB - AT) / 2 - 6;
        for (i = 1; i <= rings; i++) {
          var rr = rMax * i / rings;
          hAx += '<circle cx="' + pcx + '" cy="' + pcy + '" r="' + f2(rr) + '" fill="none" stroke="' + GR + '" stroke-width="0.9"/>';
          hAx += tx(pcx + rr + 2, pcy - 3, String(i), MU, 'start', false, 9);
        }
        for (i = 0; i < 12; i++) {
          var a = i * Math.PI / 6;
          hAx += ln(pcx, pcy, pcx + rMax * Math.cos(a), pcy - rMax * Math.sin(a), i % 3 === 0 ? ST : GR, i % 3 === 0 ? 1.4 : 0.8);
          hAx += tx(pcx + (rMax + 13) * Math.cos(a), pcy - (rMax + 13) * Math.sin(a) + 3, (i * 30) + '°', MU, 'middle', false, 9);
        }
        hAx += arR(pcx + rMax, pcy);
        return wrap(hAx);
      }
      // ax3d
      var dcx = 170, dcy = 150, len = 100, zx = dcx - len * 0.62, zy = dcy + len * 0.62;
      hAx += ln(dcx - 30, dcy, dcx + len, dcy, ST, 1.8) + arR(dcx + len, dcy);
      hAx += ln(dcx, dcy + 30, dcx, dcy - len, ST, 1.8) + arU(dcx, dcy - len);
      hAx += ln(dcx + 20, dcy - 20, zx, zy, ST, 1.8);
      hAx += '<polygon points="' + f2(zx) + ',' + f2(zy) + ' ' + f2(zx + 9) + ',' + f2(zy - 1) + ' ' + f2(zx + 2) + ',' + f2(zy - 9) + '" fill="' + ST + '"/>';
      hAx += tx(dcx + len + 4, dcy + 4, 'x', ST, 'start', true, 12) + tx(dcx + 6, dcy - len - 2, 'y', ST, 'start', true, 12) + tx(zx - 4, zy + 12, 'z', ST, 'end', true, 12) + tx(dcx - 6, dcy + 14, 'O', MU, 'end', false, 10);
      var dst = Math.max(0.5, num(X.step, 1)), dn = Math.min(10, Math.max(1, Math.floor(num(X.xmax, 4))));
      for (i = 1; i <= dn; i++) {
        d = len * i / (dn + 0.6);
        hAx += ln(dcx + d, dcy - 3, dcx + d, dcy + 3, ST, 1.1) + tx(dcx + d, dcy + 13, nm(i * dst), MU, 'middle', false, 9);
        hAx += ln(dcx - 3, dcy - d, dcx + 3, dcy - d, ST, 1.1) + tx(dcx - 6, dcy - d + 3, nm(i * dst), MU, 'end', false, 9);
        hAx += ln(dcx - d * 0.62 - 2.5, dcy + d * 0.62 - 2.5, dcx - d * 0.62 + 2.5, dcy + d * 0.62 + 2.5, ST, 1.1);
      }
      return wrap(hAx);
    }

    if (t === 'line' || t === 'quad' || t === 'sine' || t === 'plot' || t === 'exp') {
      var xmin = num(X.xmin, -5), xmax = num(X.xmax, 5);
      var ymin = num(X.ymin, -4), ymax = num(X.ymax, 4);
      if (xmax <= xmin) xmax = xmin + 2;
      if (ymax <= ymin) ymax = ymin + 2;
      var ax = axes(xmin, xmax, ymin, ymax);
      var yof = ax.yof;
      ax.yof = function (y) { return yof(Math.max(ymin, Math.min(ymax, y))); };
      var body = '';
      if (t === 'line') {
        var m = num(X.m, 1), b = num(X.b, 0);
        body = plotFn(ax, function (x) { return m * x + b; }, xmin, xmax);
      } else if (t === 'quad') {
        var a = num(X.a, 1), bb = num(X.b, 0), c = num(X.c, 0);
        body = plotFn(ax, function (x) { return a * x * x + bb * x + c; }, xmin, xmax);
      } else if (t === 'sine') {
        var A = num(X.A, 1), w = num(X.w, 1), ph = num(X.ph, 0);
        body = plotFn(ax, function (x) { return A * Math.sin(w * x + ph); }, xmin, xmax);
      } else if (t === 'exp') {
        var ae = num(X.a, 1), be = num(X.b, 0.5);
        body = plotFn(ax, function (x) { return ae * Math.exp(be * x); }, xmin, xmax);
      }
      return wrap(head + ax.html + body);
    }

    var labels = splitList(X.labs || 'A,B,C,D');
    var values = splitList(X.vals || '4,7,3,6').map(function (v) { return num(v, 0); });
    while (values.length < labels.length) values.push(0);
    labels = labels.slice(0, Math.max(labels.length, values.length));
    var colors = COLORS;

    if (t === 'bar' || t === 'col') {
      var maxv = Math.max.apply(null, values.concat([1]));
      var n = labels.length, gap = 12;
      var L = 48, T = 28, R = 340, B = 230;
      var bw = Math.max(12, (R - L - gap * (n + 1)) / n);
      var h = frame(L, T, R, B);
      h += '<line x1="' + L + '" y1="' + B + '" x2="' + R + '" y2="' + B + '" stroke="#2c3a50" stroke-width="1.5"/>';
      h += '<line x1="' + L + '" y1="' + T + '" x2="' + L + '" y2="' + B + '" stroke="#2c3a50" stroke-width="1.5"/>';
      for (var i = 0; i < n; i++) {
        var bh = (Math.max(0, values[i]) / maxv) * (B - T - 12);
        var x = L + gap + i * (bw + gap);
        var y = B - bh;
        h += '<rect x="' + x + '" y="' + y + '" width="' + bw + '" height="' + bh + '" rx="5" fill="' + colors[i % colors.length] + '"/>';
        h += '<text class="fa" x="' + (x + bw / 2) + '" y="' + (B + 16) + '" text-anchor="middle">' + esc(labels[i]) + '</text>';
        h += '<text class="tk" x="' + (x + bw / 2) + '" y="' + (y - 5) + '" text-anchor="middle">' + values[i] + '</text>';
      }
      return wrap(head + h);
    }

    if (t === 'hbar') {
      var maxh = Math.max.apply(null, values.concat([1]));
      var nh = labels.length, Lh = 86, Th = 28, Rh = 340, Bh = 240;
      var bh2 = Math.max(10, (Bh - Th - 8 * (nh + 1)) / nh);
      var hh = frame(36, Th, Rh, Bh);
      hh += '<line x1="' + Lh + '" y1="' + Th + '" x2="' + Lh + '" y2="' + Bh + '" stroke="#2c3a50" stroke-width="1.5"/>';
      for (var ih = 0; ih < nh; ih++) {
        var bw2 = (Math.max(0, values[ih]) / maxh) * (Rh - Lh - 16);
        var yb = Th + 8 + ih * (bh2 + 8);
        hh += '<rect x="' + Lh + '" y="' + yb + '" width="' + bw2 + '" height="' + bh2 + '" rx="5" fill="' + colors[ih % colors.length] + '"/>';
        hh += '<text class="fa" x="' + (Lh - 6) + '" y="' + (yb + bh2 / 2 + 4) + '" text-anchor="end">' + esc(labels[ih]) + '</text>';
        hh += '<text class="tk" x="' + (Lh + bw2 + 6) + '" y="' + (yb + bh2 / 2 + 4) + '">' + values[ih] + '</text>';
      }
      return wrap(head + hh);
    }

    if (t === 'stack') {
      var sA = values;
      var sB = pad(splitList(X.vals2 || '2,3,1,2').map(function (v) { return num(v, 0); }), sA.length, 0);
      var sC = pad(splitList(X.vals3 || '1,1,2,1').map(function (v) { return num(v, 0); }), sA.length, 0);
      var ns = labels.length;
      var totals = [];
      var si;
      for (si = 0; si < ns; si++) totals.push(Math.max(0, sA[si]) + Math.max(0, sB[si]) + Math.max(0, sC[si]));
      var maxs = Math.max.apply(null, totals.concat([1]));
      var chs = chrome(title, [
        { c: '#6c63f5', n: X.s1 || 'سری ۱' },
        { c: '#27c4a8', n: X.s2 || 'سری ۲' },
        { c: '#f0a202', n: X.s3 || 'سری ۳' }
      ]);
      var Ls = 48, Ts = chs.T, Rs = 340, Bs = 222, gaps = 14;
      var bws = Math.max(14, (Rs - Ls - gaps * (ns + 1)) / ns);
      var hs = chs.html + frame(Ls, Ts, Rs, Bs);
      hs += '<line x1="' + Ls + '" y1="' + Bs + '" x2="' + Rs + '" y2="' + Bs + '" stroke="#2c3a50" stroke-width="1.5"/>';
      for (si = 0; si < ns; si++) {
        var xs = Ls + gaps + si * (bws + gaps);
        var y0 = Bs;
        var parts = [sA[si], sB[si], sC[si]];
        var cols = ['#6c63f5', '#27c4a8', '#f0a202'];
        var pi;
        for (pi = 0; pi < 3; pi++) {
          var ph = (Math.max(0, parts[pi]) / maxs) * (Bs - Ts - 10);
          y0 -= ph;
          hs += '<rect x="' + xs + '" y="' + y0 + '" width="' + bws + '" height="' + ph + '" fill="' + cols[pi] + '"/>';
        }
        hs += '<text class="fa" x="' + (xs + bws / 2) + '" y="' + (Bs + 16) + '" text-anchor="middle">' + esc(labels[si]) + '</text>';
      }
      return wrap(hs);
    }

    if (t === 'pie' || t === 'donut') {
      var sum = values.reduce(function (a, b) { return a + Math.max(0, b); }, 0) || 1;
      var chp = chrome(title, null);
      var cx = 124, cy = 158, rr = 72, ang = -Math.PI / 2, h2 = chp.html;
      var legY = Math.max(40, chp.T);
      for (var j = 0; j < values.length; j++) {
        var slice = Math.max(0, values[j]) / sum * Math.PI * 2;
        var a1 = ang, a2 = ang + slice;
        var large = slice > Math.PI ? 1 : 0;
        var x1 = cx + rr * Math.cos(a1), y1 = cy + rr * Math.sin(a1);
        var x2 = cx + rr * Math.cos(a2), y2 = cy + rr * Math.sin(a2);
        if (slice > 0.0001) {
          h2 += '<path d="M' + cx + ',' + cy + ' L' + x1.toFixed(1) + ',' + y1.toFixed(1) +
            ' A' + rr + ',' + rr + ' 0 ' + large + ' 1 ' + x2.toFixed(1) + ',' + y2.toFixed(1) +
            ' Z" fill="' + colors[j % colors.length] + '" stroke="#fff" stroke-width="1.5"/>';
        }
        ang = a2;
        var pct = Math.round(Math.max(0, values[j]) / sum * 100);
        h2 += '<rect x="228" y="' + (legY + j * 24) + '" width="12" height="12" rx="3" fill="' + colors[j % colors.length] + '"/>';
        h2 += '<text class="lg" x="246" y="' + (legY + 11 + j * 24) + '">' + esc(labels[j]) + ' — ' + pct + '٪</text>';
      }
      if (t === 'donut') h2 += '<circle cx="' + cx + '" cy="' + cy + '" r="36" fill="#fff"/>';
      return wrap(h2);
    }

    if (t === 'lchr' || t === 'area') {
      var maxv2 = Math.max.apply(null, values.concat([1]));
      var n2 = labels.length;
      var L2 = 48, T2 = 28, R2 = 340, B2 = 230;
      var h3 = frame(L2, T2, R2, B2);
      h3 += '<line x1="' + L2 + '" y1="' + B2 + '" x2="' + R2 + '" y2="' + B2 + '" stroke="#2c3a50" stroke-width="1.5"/>';
      var pts2 = [];
      for (var k = 0; k < n2; k++) {
        var xx = L2 + (n2 === 1 ? (R2 - L2) / 2 : k * (R2 - L2) / (n2 - 1));
        var yy = B2 - (Math.max(0, values[k]) / maxv2) * (B2 - T2 - 16);
        pts2.push(xx.toFixed(1) + ',' + yy.toFixed(1));
        h3 += '<text class="fa" x="' + xx + '" y="' + (B2 + 16) + '" text-anchor="middle">' + esc(labels[k]) + '</text>';
        h3 += '<text class="tk" x="' + xx + '" y="' + (yy - 8) + '" text-anchor="middle">' + values[k] + '</text>';
      }
      if (t === 'area' && pts2.length) {
        var firstX = pts2[0].split(',')[0];
        var lastX = pts2[pts2.length - 1].split(',')[0];
        h3 += '<polygon points="' + firstX + ',' + B2 + ' ' + pts2.join(' ') + ' ' + lastX + ',' + B2 +
          '" fill="rgba(108,99,245,.28)" stroke="none"/>';
      }
      if (pts2.length) h3 += '<polyline points="' + pts2.join(' ') + '" fill="none" stroke="#6c63f5" stroke-width="2.2"/>';
      for (k = 0; k < n2; k++) {
        var p = pts2[k].split(',');
        h3 += '<circle cx="' + p[0] + '" cy="' + p[1] + '" r="4" fill="#6c63f5"/>';
      }
      return wrap(head + h3);
    }

    if (t === 'hist') {
      var maxv3 = Math.max.apply(null, values.concat([1]));
      var n3 = labels.length, L3 = 48, T3 = 28, R3 = 340, B3 = 230;
      var bw3 = (R3 - L3) / n3;
      var hih = frame(L3, T3, R3, B3);
      hih += '<line x1="' + L3 + '" y1="' + B3 + '" x2="' + R3 + '" y2="' + B3 + '" stroke="#2c3a50" stroke-width="1.5"/>';
      for (var hi = 0; hi < n3; hi++) {
        var bh3 = (Math.max(0, values[hi]) / maxv3) * (B3 - T3 - 10);
        var x3 = L3 + hi * bw3;
        hih += '<rect x="' + x3 + '" y="' + (B3 - bh3) + '" width="' + (bw3 - 1) + '" height="' + bh3 + '" fill="#6c63f5" opacity=".85"/>';
        hih += '<text class="fa" x="' + (x3 + bw3 / 2) + '" y="' + (B3 + 16) + '" text-anchor="middle">' + esc(labels[hi]) + '</text>';
      }
      return wrap(head + hih);
    }

    if (t === 'scat' || t === 'bub') {
      var xs = splitList(X.xs || '1,2,3,4,5').map(function (v) { return num(v, 0); });
      var ys = splitList(X.ys || '2,3,1,5,4').map(function (v) { return num(v, 0); });
      var zs = splitList(X.zs || '8,14,6,18,10').map(function (v) { return num(v, 8); });
      var n4 = Math.min(xs.length, ys.length) || 1;
      var xmin2 = Math.min.apply(null, xs.concat([0])) - 1;
      var xmax2 = Math.max.apply(null, xs.concat([1])) + 1;
      var ymin2 = Math.min.apply(null, ys.concat([0])) - 1;
      var ymax2 = Math.max.apply(null, ys.concat([1])) + 1;
      var ax2 = axes(xmin2, xmax2, ymin2, ymax2);
      var dots = '';
      var zmax = Math.max.apply(null, zs.concat([1]));
      for (var si2 = 0; si2 < n4; si2++) {
        var rad = t === 'bub' ? (6 + 16 * Math.max(0, zs[si2] || 8) / zmax) : 5;
        dots += '<circle cx="' + ax2.xof(xs[si2]) + '" cy="' + ax2.yof(ys[si2]) + '" r="' + rad.toFixed(1) +
          '" fill="' + colors[si2 % colors.length] + '" fill-opacity="' + (t === 'bub' ? '.45' : '1') +
          '" stroke="' + colors[si2 % colors.length] + '" stroke-width="1.4"/>';
      }
      return wrap(head + ax2.html + dots);
    }

    if (t === 'radar') {
      var nr = Math.max(3, labels.length);
      labels = pad(labels, nr, 'P');
      values = pad(values, nr, 0);
      var maxr = Math.max.apply(null, values.concat([1]));
      var cxr = 180, cyr = 152, rr2 = 68;
      var hr = '';
      var ring;
      for (ring = 1; ring <= 4; ring++) {
        var ptsR = [];
        var ir;
        for (ir = 0; ir < nr; ir++) {
          var angR = -Math.PI / 2 + ir * 2 * Math.PI / nr;
          var radR = rr2 * ring / 4;
          ptsR.push((cxr + radR * Math.cos(angR)).toFixed(1) + ',' + (cyr + radR * Math.sin(angR)).toFixed(1));
        }
        hr += '<polygon points="' + ptsR.join(' ') + '" fill="none" stroke="#e0e5ee" stroke-width="1"/>';
      }
      var polyV = [];
      for (var jr = 0; jr < nr; jr++) {
        var ang2 = -Math.PI / 2 + jr * 2 * Math.PI / nr;
        var axp = cxr + rr2 * Math.cos(ang2), ayp = cyr + rr2 * Math.sin(ang2);
        hr += '<line x1="' + cxr + '" y1="' + cyr + '" x2="' + axp.toFixed(1) + '" y2="' + ayp.toFixed(1) + '" stroke="#d5dce6"/>';
        var rv = rr2 * Math.max(0, values[jr]) / maxr;
        polyV.push((cxr + rv * Math.cos(ang2)).toFixed(1) + ',' + (cyr + rv * Math.sin(ang2)).toFixed(1));
        var lx = cxr + (rr2 + 20) * Math.cos(ang2);
        var ly = cyr + (rr2 + 20) * Math.sin(ang2);
        var anchor = Math.abs(Math.cos(ang2)) < 0.25 ? 'middle' : (Math.cos(ang2) > 0 ? 'start' : 'end');
        hr += '<text class="fa" x="' + lx.toFixed(1) + '" y="' + (ly + 4).toFixed(1) + '" text-anchor="' + anchor + '">' + esc(labels[jr]) + '</text>';
      }
      hr += '<polygon points="' + polyV.join(' ') + '" fill="rgba(108,99,245,.30)" stroke="#6c63f5" stroke-width="2"/>';
      return wrap(head + hr);
    }

    if (t === 'gauge') {
      var vmin = num(X.vmin, 0), vmax = num(X.vmax, 100);
      if (vmax <= vmin) vmax = vmin + 100;
      var gval = num(X.val, 65);
      var frac = Math.max(0, Math.min(1, (gval - vmin) / (vmax - vmin)));
      var chg = chrome(title, null);
      var top = Math.max(chg.T + 4, title ? 38 : 30);
      var cgx = 180, cgy = 200, rg = 70;
      if (cgy - rg - 14 < top) rg = Math.max(46, cgy - top - 14);
      var th = Math.PI * (1 - frac);
      var nx = cgx + (rg - 16) * Math.cos(th);
      var ny = cgy - (rg - 16) * Math.sin(th);
      var ex = cgx + rg * Math.cos(th);
      var ey = cgy - rg * Math.sin(th);
      var hg = chg.html;
      hg += '<rect class="gf-bg" x="22" y="' + top + '" width="316" height="' + (266 - top) + '" rx="12" fill="#fbfcfe" stroke="#d5dce6"/>';
      hg += '<path d="M' + (cgx - rg) + ',' + cgy + ' A' + rg + ',' + rg + ' 0 0 1 ' + (cgx + rg) + ',' + cgy + '" fill="none" stroke="#e6eaf0" stroke-width="14" stroke-linecap="round"/>';
      if (frac >= 0.992) {
        hg += '<path d="M' + (cgx - rg) + ',' + cgy + ' A' + rg + ',' + rg + ' 0 0 1 ' + (cgx + rg) + ',' + cgy + '" fill="none" stroke="#6c63f5" stroke-width="14" stroke-linecap="round"/>';
      } else if (frac > 0.015) {
        hg += '<path d="M' + (cgx - rg) + ',' + cgy + ' A' + rg + ',' + rg + ' 0 0 1 ' + ex.toFixed(1) + ',' + ey.toFixed(1) + '" fill="none" stroke="#6c63f5" stroke-width="14" stroke-linecap="round"/>';
      }
      hg += '<line x1="' + cgx + '" y1="' + cgy + '" x2="' + nx.toFixed(1) + '" y2="' + ny.toFixed(1) + '" stroke="#2c3a50" stroke-width="3" stroke-linecap="round"/>';
      hg += '<circle cx="' + cgx + '" cy="' + cgy + '" r="6" fill="#2c3a50"/>';
      hg += '<text class="ttl" x="' + cgx + '" y="' + (cgy + 24) + '" text-anchor="middle">' + gval + '</text>';
      hg += '<text class="tk" x="' + (cgx - rg + 2) + '" y="' + (cgy + 16) + '" text-anchor="middle">' + vmin + '</text>';
      hg += '<text class="tk" x="' + (cgx + rg - 2) + '" y="' + (cgy + 16) + '" text-anchor="middle">' + vmax + '</text>';
      return wrap(hg);
    }
    if (t === 'cmp') {
      var vals2 = splitList(X.vals2 || '5,4,6,2').map(function (v) { return num(v, 0); });
      var nc = Math.max(labels.length, values.length, vals2.length, 1);
      labels = pad(labels, nc, '');
      values = pad(values, nc, 0);
      vals2 = pad(vals2, nc, 0);
      var maxc = Math.max.apply(null, values.concat(vals2).concat([1]));
      var chc = chrome(title, [
        { c: '#6c63f5', n: X.s1 || 'سری ۱' },
        { c: '#27c4a8', n: X.s2 || 'سری ۲' }
      ]);
      var Lc = 48, Tc = chc.T, Rc = 340, Bc = 218, gapc = 12;
      var group = Math.max(28, (Rc - Lc - gapc * (nc + 1)) / nc);
      var w1 = Math.max(8, group * 0.38);
      var hc = chc.html + frame(Lc, Tc, Rc, Bc);
      hc += '<line x1="' + Lc + '" y1="' + Bc + '" x2="' + Rc + '" y2="' + Bc + '" stroke="#2c3a50" stroke-width="1.5"/>';
      for (var ic = 0; ic < nc; ic++) {
        var xg = Lc + gapc + ic * (group + gapc);
        var h1 = (Math.max(0, values[ic]) / maxc) * (Bc - Tc - 16);
        var h2c = (Math.max(0, vals2[ic]) / maxc) * (Bc - Tc - 16);
        hc += '<rect x="' + xg + '" y="' + (Bc - h1) + '" width="' + w1 + '" height="' + h1 + '" rx="3" fill="#6c63f5"/>';
        hc += '<rect x="' + (xg + w1 + 4) + '" y="' + (Bc - h2c) + '" width="' + w1 + '" height="' + h2c + '" rx="3" fill="#27c4a8"/>';
        hc += '<text class="fa" x="' + (xg + w1 + 2) + '" y="' + (Bc + 16) + '" text-anchor="middle">' + esc(labels[ic]) + '</text>';
      }
      return wrap(hc);
    }

    if (t === 'pareto') {
      var items = labels.map(function (lb, ix) { return { l: lb, v: Math.max(0, values[ix] || 0) }; });
      items.sort(function (a, b) { return b.v - a.v; });
      var sumP = items.reduce(function (a, b) { return a + b.v; }, 0) || 1;
      var Lp = 48, Tp = 28, Rp = 318, Bp = 230;
      var nP = items.length;
      var bwP = Math.max(12, (Rp - Lp) / nP - 6);
      var hp = frame(Lp, Tp, Rp + 22, Bp);
      hp += '<line x1="' + Lp + '" y1="' + Bp + '" x2="' + Rp + '" y2="' + Bp + '" stroke="#2c3a50" stroke-width="1.5"/>';
      var acc = 0, cpts = [];
      for (var ip = 0; ip < nP; ip++) {
        var xp = Lp + 6 + ip * ((Rp - Lp) / nP);
        var barH = (items[ip].v / items[0].v) * (Bp - Tp - 18);
        hp += '<rect x="' + xp + '" y="' + (Bp - barH) + '" width="' + bwP + '" height="' + barH + '" fill="#6c63f5"/>';
        hp += '<text class="fa" x="' + (xp + bwP / 2) + '" y="' + (Bp + 16) + '" text-anchor="middle">' + esc(items[ip].l) + '</text>';
        acc += items[ip].v;
        var cyP = Bp - (acc / sumP) * (Bp - Tp - 18);
        cpts.push((xp + bwP / 2).toFixed(1) + ',' + cyP.toFixed(1));
        hp += '<circle cx="' + (xp + bwP / 2) + '" cy="' + cyP + '" r="3.2" fill="#e4572e"/>';
      }
      if (cpts.length) hp += '<polyline points="' + cpts.join(' ') + '" fill="none" stroke="#e4572e" stroke-width="2"/>';
      hp += '<text class="tk" x="338" y="40" text-anchor="end">%</text>';
      return wrap(head + hp);
    }

    if (t === 'fall') {
      var nF = labels.length;
      var Lf = 48, Tf = 28, Rf = 340, Bf = 230;
      var run = 0, maxF = 0, minF = 0, runs = [];
      for (var iF = 0; iF < nF; iF++) {
        var dv = values[iF];
        var from = run;
        run += dv;
        runs.push({ from: from, to: run, dv: dv });
        maxF = Math.max(maxF, from, run);
        minF = Math.min(minF, from, run);
      }
      var spanF = Math.max(1, maxF - minF);
      var bwF = Math.max(14, (Rf - Lf - 10 * (nF + 1)) / nF);
      var hf = frame(Lf, Tf, Rf, Bf);
      hf += '<line x1="' + Lf + '" y1="' + Bf + '" x2="' + Rf + '" y2="' + Bf + '" stroke="#2c3a50" stroke-width="1.5"/>';
      function yF(v) { return Bf - 8 - ((v - minF) / spanF) * (Bf - Tf - 20); }
      for (iF = 0; iF < nF; iF++) {
        var xf = Lf + 10 + iF * (bwF + 10);
        var yTop = yF(Math.max(runs[iF].from, runs[iF].to));
        var yBot = yF(Math.min(runs[iF].from, runs[iF].to));
        var colF = iF === 0 || iF === nF - 1 ? '#4c9be8' : (runs[iF].dv >= 0 ? '#27c4a8' : '#e4572e');
        hf += '<rect x="' + xf + '" y="' + yTop + '" width="' + bwF + '" height="' + Math.max(3, yBot - yTop) + '" rx="3" fill="' + colF + '"/>';
        hf += '<text class="tk" x="' + (xf + bwF / 2) + '" y="' + (yTop - 5) + '" text-anchor="middle">' + values[iF] + '</text>';
        hf += '<text class="fa" x="' + (xf + bwF / 2) + '" y="' + (Bf + 16) + '" text-anchor="middle">' + esc(labels[iF]) + '</text>';
        if (iF < nF - 1) {
          hf += '<line x1="' + (xf + bwF) + '" y1="' + yF(runs[iF].to) + '" x2="' + (xf + bwF + 10) + '" y2="' + yF(runs[iF].to) + '" stroke="#9aa6b8" stroke-dasharray="3 3"/>';
        }
      }
      return wrap(head + hf);
    }

    if (t === 'gantt') {
      var starts = values;
      var durs = pad(splitList(X.vals2 || '3,4,3,2').map(function (v) { return num(v, 1); }), labels.length, 1);
      var nG = labels.length;
      var maxG = 1;
      var iG;
      for (iG = 0; iG < nG; iG++) maxG = Math.max(maxG, (starts[iG] || 0) + Math.max(0.4, durs[iG]));
      var Lg = 92, Tg = 36, Rg = 340, Bg = 240;
      var rowH = Math.max(16, (Bg - Tg) / nG);
      var hg2 = frame(Lg, Tg, Rg, Bg);
      hg2 += '<line x1="' + Lg + '" y1="' + Tg + '" x2="' + Lg + '" y2="' + Bg + '" stroke="#2c3a50" stroke-width="1.4"/>';
      var tick;
      for (tick = 0; tick <= maxG; tick++) {
        var xt = Lg + tick / maxG * (Rg - Lg);
        hg2 += '<line x1="' + xt + '" y1="' + Tg + '" x2="' + xt + '" y2="' + Bg + '" stroke="#eef1f6"/>';
        hg2 += '<text class="tk" x="' + xt + '" y="' + (Tg - 6) + '" text-anchor="middle">' + tick + '</text>';
      }
      for (iG = 0; iG < nG; iG++) {
        var yg = Tg + 4 + iG * rowH;
        var xg0 = Lg + (Math.max(0, starts[iG] || 0) / maxG) * (Rg - Lg);
        var wg = (Math.max(0.4, durs[iG]) / maxG) * (Rg - Lg);
        hg2 += '<rect x="' + xg0 + '" y="' + yg + '" width="' + wg + '" height="' + Math.max(10, rowH - 8) + '" rx="5" fill="' + colors[iG % colors.length] + '"/>';
        hg2 += '<text class="fa" x="' + (Lg - 6) + '" y="' + (yg + rowH / 2) + '" text-anchor="end">' + esc(labels[iG]) + '</text>';
      }
      return wrap(head + hg2);
    }

    if (t === 'ctrl') {
      var nC = values.length || 1;
      var mean = X.mean === '' || X.mean == null ? values.reduce(function (a, b) { return a + b; }, 0) / nC : num(X.mean, 0);
      var sd = 0;
      var iC;
      for (iC = 0; iC < nC; iC++) sd += (values[iC] - mean) * (values[iC] - mean);
      sd = Math.sqrt(sd / Math.max(1, nC)) || 1;
      var ucl = X.ucl === '' || X.ucl == null ? mean + 3 * sd : num(X.ucl, mean + 3);
      var lcl = X.lcl === '' || X.lcl == null ? mean - 3 * sd : num(X.lcl, mean - 3);
      var vminC = Math.min.apply(null, values.concat([lcl])) - 1;
      var vmaxC = Math.max.apply(null, values.concat([ucl])) + 1;
      var Lc2 = 48, Tc2 = 28, Rc2 = 340, Bc2 = 230;
      function yC(v) { return Bc2 - ((v - vminC) / (vmaxC - vminC)) * (Bc2 - Tc2); }
      var hct = frame(Lc2, Tc2, Rc2, Bc2);
      hct += '<line x1="' + Lc2 + '" y1="' + yC(ucl) + '" x2="' + Rc2 + '" y2="' + yC(ucl) + '" stroke="#e4572e" stroke-dasharray="5 4"/>';
      hct += '<line x1="' + Lc2 + '" y1="' + yC(lcl) + '" x2="' + Rc2 + '" y2="' + yC(lcl) + '" stroke="#e4572e" stroke-dasharray="5 4"/>';
      hct += '<line x1="' + Lc2 + '" y1="' + yC(mean) + '" x2="' + Rc2 + '" y2="' + yC(mean) + '" stroke="#27c4a8" stroke-width="1.6"/>';
      hct += '<text class="tk" x="' + (Rc2 - 2) + '" y="' + (yC(ucl) - 4) + '" text-anchor="end">UCL</text>';
      hct += '<text class="tk" x="' + (Rc2 - 2) + '" y="' + (yC(lcl) + 12) + '" text-anchor="end">LCL</text>';
      var ptsC = [];
      for (iC = 0; iC < nC; iC++) {
        var xc = Lc2 + (nC === 1 ? (Rc2 - Lc2) / 2 : iC * (Rc2 - Lc2) / (nC - 1));
        var yc = yC(values[iC]);
        ptsC.push(xc.toFixed(1) + ',' + yc.toFixed(1));
        hct += '<circle cx="' + xc + '" cy="' + yc + '" r="3.5" fill="#6c63f5"/>';
        if (labels[iC]) hct += '<text class="fa" x="' + xc + '" y="' + (Bc2 + 16) + '" text-anchor="middle">' + esc(labels[iC]) + '</text>';
      }
      hct += '<polyline points="' + ptsC.join(' ') + '" fill="none" stroke="#6c63f5" stroke-width="2"/>';
      return wrap(head + hct);
    }

    if (t === 'heat') {
      var rows = splitList(X.rows || 'A,B,C');
      var cols = splitList(X.cols || '۱,۲,۳,۴');
      var cells = splitList(X.vals || '2,5,8,3,6,1,9,4,7,2,5,8').map(function (v) { return num(v, 0); });
      var nrH = rows.length, ncH = cols.length;
      var Lh2 = 58, Th2 = 40, Rh2 = 340, Bh2 = 240;
      var cw = (Rh2 - Lh2) / ncH, rh = (Bh2 - Th2) / nrH;
      var minH = Math.min.apply(null, cells.concat([0]));
      var maxH = Math.max.apply(null, cells.concat([1]));
      var hh2 = '';
      var irH, icH;
      for (icH = 0; icH < ncH; icH++) {
        hh2 += '<text class="fa" x="' + (Lh2 + cw * (icH + 0.5)) + '" y="' + (title ? 36 : 28) + '" text-anchor="middle">' + esc(cols[icH]) + '</text>';
      }
      for (irH = 0; irH < nrH; irH++) {
        hh2 += '<text class="fa" x="' + (Lh2 - 6) + '" y="' + (Th2 + rh * (irH + 0.55)) + '" text-anchor="end">' + esc(rows[irH]) + '</text>';
        for (icH = 0; icH < ncH; icH++) {
          var valH = cells[irH * ncH + icH];
          if (valH == null) valH = 0;
          var fH = maxH === minH ? 0.5 : (valH - minH) / (maxH - minH);
          var rC = Math.round(232 - fH * 140);
          var gC = Math.round(236 - fH * 80);
          var bC = Math.round(255 - fH * 40);
          var fillH = 'rgb(' + rC + ',' + gC + ',' + bC + ')';
          if (fH > 0.55) fillH = 'rgb(' + Math.round(108 + (1 - fH) * 80) + ',' + Math.round(99 + (1 - fH) * 90) + ',' + Math.round(245 - fH * 40) + ')';
          hh2 += '<rect x="' + (Lh2 + icH * cw) + '" y="' + (Th2 + irH * rh) + '" width="' + (cw - 2) + '" height="' + (rh - 2) + '" rx="4" fill="' + fillH + '"/>';
          hh2 += '<text class="tk" x="' + (Lh2 + cw * (icH + 0.5) - 1) + '" y="' + (Th2 + rh * (irH + 0.58)) + '" text-anchor="middle">' + valH + '</text>';
        }
      }
      return wrap(head + hh2);
    }

    if (t === 'pict') {
      var maxP = Math.max.apply(null, values.concat([1]));
      var unit = num(X.unit, 0) || Math.max(1, Math.ceil(maxP / 8));
      var nPi = labels.length;
      var hp2 = '';
      var iP;
      for (iP = 0; iP < nPi; iP++) {
        var yP = 40 + iP * Math.min(48, 220 / nPi);
        hp2 += '<text class="fa" x="8" y="' + (yP + 8) + '">' + esc(labels[iP]) + '</text>';
        var count = Math.max(0, Math.round(values[iP] / unit));
        var kP;
        for (kP = 0; kP < count && kP < 12; kP++) {
          hp2 += personIcon(90 + kP * 20, yP + 18, 22, colors[iP % colors.length]);
        }
        hp2 += '<text class="tk" x="340" y="' + (yP + 8) + '" text-anchor="end">' + values[iP] + '</text>';
      }
      hp2 += '<text class="fa" x="180" y="270" text-anchor="middle">هر نماد ≈ ' + unit + '</text>';
      return wrap(head + hp2);
    }

    if (t === 'time') {
      var nT = labels.length;
      var yT = 140;
      var ht = '<line x1="28" y1="' + yT + '" x2="332" y2="' + yT + '" stroke="#2c3a50" stroke-width="2"/>';
      ht += '<polygon points="332,' + yT + ' 322,' + (yT - 5) + ' 322,' + (yT + 5) + '" fill="#2c3a50"/>';
      for (var iT = 0; iT < nT; iT++) {
        var xT = 40 + (nT === 1 ? 140 : iT * 260 / (nT - 1));
        var up = iT % 2 === 0;
        ht += '<circle cx="' + xT + '" cy="' + yT + '" r="6" fill="' + colors[iT % colors.length] + '" stroke="#fff" stroke-width="2"/>';
        ht += '<line x1="' + xT + '" y1="' + yT + '" x2="' + xT + '" y2="' + (up ? yT - 36 : yT + 36) + '" stroke="#c5cedb"/>';
        ht += '<text class="fa" x="' + xT + '" y="' + (up ? yT - 44 : yT + 52) + '" text-anchor="middle">' + esc(labels[iT]) + '</text>';
        if (values[iT] != null) ht += '<text class="tk" x="' + xT + '" y="' + (up ? yT - 58 : yT + 66) + '" text-anchor="middle">' + values[iT] + '</text>';
      }
      return wrap(head + ht);
    }

    if (t === 'venn') {
      var nV = Math.max(2, Math.min(3, Math.round(num(X.n, 3))));
      var s1 = X.s1 || 'A', s2 = X.s2 || 'B', s3 = X.s3 || 'C';
      var hv = '';
      if (nV === 2) {
        hv += '<circle cx="140" cy="150" r="78" fill="rgba(108,99,245,.35)" stroke="#6c63f5"/>';
        hv += '<circle cx="220" cy="150" r="78" fill="rgba(39,196,168,.35)" stroke="#27c4a8"/>';
        hv += '<text class="ttl" x="108" y="150" text-anchor="middle">' + esc(s1) + '</text>';
        hv += '<text class="ttl" x="252" y="150" text-anchor="middle">' + esc(s2) + '</text>';
        hv += '<text class="tk" x="180" y="154" text-anchor="middle">' + esc(X.ab || 'A∩B') + '</text>';
      } else {
        hv += '<circle cx="155" cy="128" r="70" fill="rgba(108,99,245,.32)" stroke="#6c63f5"/>';
        hv += '<circle cx="205" cy="128" r="70" fill="rgba(39,196,168,.32)" stroke="#27c4a8"/>';
        hv += '<circle cx="180" cy="178" r="70" fill="rgba(240,162,2,.32)" stroke="#f0a202"/>';
        hv += '<text class="fa" x="120" y="110" text-anchor="middle">' + esc(s1) + '</text>';
        hv += '<text class="fa" x="240" y="110" text-anchor="middle">' + esc(s2) + '</text>';
        hv += '<text class="fa" x="180" y="232" text-anchor="middle">' + esc(s3) + '</text>';
        hv += '<text class="tk" x="180" y="124" text-anchor="middle">' + esc(X.ab || '') + '</text>';
        hv += '<text class="tk" x="148" y="172" text-anchor="middle">' + esc(X.ac || '') + '</text>';
        hv += '<text class="tk" x="212" y="172" text-anchor="middle">' + esc(X.bc || '') + '</text>';
        hv += '<text class="tk" x="180" y="158" text-anchor="middle">' + esc(X.abc || '') + '</text>';
      }
      return wrap(head + hv);
    }

    if (t === 'flow') {
      var steps = labels.length ? labels : splitList('شروع,پردازش,تصمیم؟,پایان');
      var nFl = Math.min(6, Math.max(2, steps.length));
      steps = steps.slice(0, nFl);
      var hf2 = '';
      var boxW = Math.min(78, 300 / nFl);
      var iFl;
      for (iFl = 0; iFl < nFl; iFl++) {
        var xf2 = 20 + iFl * (320 / nFl);
        var yf2 = 120;
        var isDec = /[؟?]|تصمیم/.test(steps[iFl]);
        var isTerm = iFl === 0 || iFl === nFl - 1;
        if (isDec) {
          hf2 += '<polygon points="' + (xf2 + boxW / 2) + ',' + (yf2 - 36) + ' ' + (xf2 + boxW + 6) + ',' + yf2 +
            ' ' + (xf2 + boxW / 2) + ',' + (yf2 + 36) + ' ' + (xf2 - 6) + ',' + yf2 + '" fill="#fff4d6" stroke="#f0a202" stroke-width="1.6"/>';
        } else if (isTerm) {
          hf2 += '<rect x="' + xf2 + '" y="' + (yf2 - 22) + '" width="' + boxW + '" height="44" rx="20" fill="#e8e6ff" stroke="#6c63f5" stroke-width="1.6"/>';
        } else {
          hf2 += '<rect x="' + xf2 + '" y="' + (yf2 - 22) + '" width="' + boxW + '" height="44" rx="8" fill="#e6f8f4" stroke="#27c4a8" stroke-width="1.6"/>';
        }
        hf2 += '<text class="fa" x="' + (xf2 + boxW / 2) + '" y="' + (yf2 + 4) + '" text-anchor="middle">' + esc(steps[iFl]) + '</text>';
        if (iFl < nFl - 1) {
          var ax1 = xf2 + boxW + 4, ax2 = 20 + (iFl + 1) * (320 / nFl) - 6;
          hf2 += '<line x1="' + ax1 + '" y1="120" x2="' + ax2 + '" y2="120" stroke="#667386" stroke-width="1.6"/>';
          hf2 += '<polygon points="' + ax2 + ',120 ' + (ax2 - 7) + ',116 ' + (ax2 - 7) + ',124" fill="#667386"/>';
        }
      }
      return wrap(head + hf2);
    }

    if (t === 'sun') {
      var labs2 = splitList(X.labs2 || 'A1,A2,B1,B2,C1,C2');
      var vOut = splitList(X.vals2 || '2,2,2,1,1,1').map(function (v) { return num(v, 1); });
      var nIn = Math.max(1, labels.length);
      var nOut = Math.max(nIn, labs2.length);
      labs2 = pad(labs2, nOut, '');
      vOut = pad(vOut, nOut, 1);
      var sumIn = values.reduce(function (a, b) { return a + Math.max(0, b); }, 0) || 1;
      var cxS = 168, cyS = 150;
      var hs2 = '';
      var angS = -Math.PI / 2;
      var counts = [];
      var base = Math.floor(nOut / nIn), rem = nOut % nIn, ci;
      for (ci = 0; ci < nIn; ci++) counts.push(base + (ci < rem ? 1 : 0));
      var oix = 0;
      for (ci = 0; ci < nIn; ci++) {
        var sliceIn = Math.max(0, values[ci]) / sumIn * Math.PI * 2;
        var a0 = angS, a1 = angS + sliceIn;
        if (sliceIn > 0.0001) hs2 += ringSlice(cxS, cyS, 22, 58, a0, a1, colors[ci % colors.length]);
        var mid = (a0 + a1) / 2;
        if (sliceIn > 0.25) hs2 += '<text class="onf" x="' + (cxS + 38 * Math.cos(mid)).toFixed(1) + '" y="' + (cyS + 38 * Math.sin(mid) + 3).toFixed(1) + '" text-anchor="middle">' + esc(labels[ci]) + '</text>';
        var chunk = vOut.slice(oix, oix + counts[ci]);
        var sumCh = chunk.reduce(function (a, b) { return a + Math.max(0, b); }, 0) || 1;
        var aC = a0, kC;
        for (kC = 0; kC < chunk.length; kC++) {
          var sl = sliceIn * Math.max(0, chunk[kC]) / sumCh;
          var a2 = aC + sl;
          if (sl > 0.0001) hs2 += ringSlice(cxS, cyS, 58, 96, aC, a2, colors[(ci + kC + 1) % colors.length]);
          var m2 = (aC + a2) / 2;
          if (sl > 0.18) hs2 += '<text class="onf" x="' + (cxS + 78 * Math.cos(m2)).toFixed(1) + '" y="' + (cyS + 78 * Math.sin(m2) + 3).toFixed(1) + '" text-anchor="middle">' + esc(labs2[oix + kC]) + '</text>';
          aC = a2;
        }
        oix += counts[ci];
        angS = a1;
      }
      hs2 += '<circle cx="' + cxS + '" cy="' + cyS + '" r="20" fill="#fff"/>';
      return wrap(head + hs2);
    }

    if (t === 'tree') {
      var itemsT = labels.map(function (lb, ix) { return { l: lb, v: Math.max(0.01, values[ix] || 0) }; });
      itemsT.sort(function (a, b) { return b.v - a.v; });
      var boxes = [];
      function squarify(arr, x, y, w, h) {
        if (!arr.length || w < 2 || h < 2) return;
        if (arr.length === 1) { boxes.push({ x: x, y: y, w: w, h: h, l: arr[0].l, v: arr[0].v }); return; }
        var sum = arr.reduce(function (a, b) { return a + b.v; }, 0) || 1;
        var first = arr[0], rest = arr.slice(1);
        var f = first.v / sum;
        if (w >= h) {
          boxes.push({ x: x, y: y, w: w * f, h: h, l: first.l, v: first.v });
          squarify(rest, x + w * f, y, w * (1 - f), h);
        } else {
          boxes.push({ x: x, y: y, w: w, h: h * f, l: first.l, v: first.v });
          squarify(rest, x, y + h * f, w, h * (1 - f));
        }
      }
      squarify(itemsT, 20, 28, 320, 230);
      var ht2 = '';
      boxes.forEach(function (b, ix) {
        ht2 += '<rect x="' + b.x.toFixed(1) + '" y="' + b.y.toFixed(1) + '" width="' + Math.max(1, b.w - 2).toFixed(1) + '" height="' + Math.max(1, b.h - 2).toFixed(1) + '" rx="4" fill="' + colors[ix % colors.length] + '"/>';
        if (b.w > 36 && b.h > 22) {
          ht2 += '<text class="onf" x="' + (b.x + b.w / 2).toFixed(1) + '" y="' + (b.y + b.h / 2).toFixed(1) + '" text-anchor="middle">' + esc(b.l) + '</text>';
          ht2 += '<text class="onf" x="' + (b.x + b.w / 2).toFixed(1) + '" y="' + (b.y + b.h / 2 + 13).toFixed(1) + '" text-anchor="middle">' + b.v + '</text>';
        }
      });
      return wrap(head + ht2);
    }

    if (t === 'funn') {
      var nFn = labels.length;
      var maxFn = Math.max.apply(null, values.concat([1]));
      var hf3 = '';
      var iFn;
      var rowH = Math.min(44, 200 / nFn);
      for (iFn = 0; iFn < nFn; iFn++) {
        var yFn = 36 + iFn * (rowH + 6);
        var wTop = 40 + (Math.max(0, values[iFn]) / maxFn) * 240;
        var nextV = iFn < nFn - 1 ? values[iFn + 1] : values[iFn] * 0.72;
        var wBot = 40 + (Math.max(0, nextV) / maxFn) * 240;
        var xT = 180 - wTop / 2, xB = 180 - wBot / 2;
        hf3 += '<polygon points="' + xT.toFixed(1) + ',' + yFn + ' ' + (xT + wTop).toFixed(1) + ',' + yFn +
          ' ' + (xB + wBot).toFixed(1) + ',' + (yFn + rowH) + ' ' + xB.toFixed(1) + ',' + (yFn + rowH) +
          '" fill="' + colors[iFn % colors.length] + '"/>';
        hf3 += '<text class="onf" x="180" y="' + (yFn + rowH / 2 + 4) + '" text-anchor="middle">' + esc(labels[iFn]) + ' · ' + values[iFn] + '</text>';
      }
      return wrap(head + hf3);
    }

    if (t === 'box') {
      var mins = pad(splitList(X.mins || '1,2,1,0').map(function (v) { return num(v, 0); }), labels.length, 0);
      var q1s = pad(splitList(X.q1s || '3,4,2,2').map(function (v) { return num(v, 1); }), labels.length, 1);
      var meds = pad(splitList(X.meds || '5,6,4,3').map(function (v) { return num(v, 2); }), labels.length, 2);
      var q3s = pad(splitList(X.q3s || '7,8,6,5').map(function (v) { return num(v, 3); }), labels.length, 3);
      var maxs = pad(splitList(X.maxs || '9,10,8,7').map(function (v) { return num(v, 4); }), labels.length, 4);
      var allB = mins.concat(q1s, meds, q3s, maxs);
      var loB = Math.min.apply(null, allB) - 1, hiB = Math.max.apply(null, allB) + 1;
      var Lb = 48, Tb = 28, Rb = 340, Bb = 230;
      function yB(v) { return Bb - ((v - loB) / (hiB - loB)) * (Bb - Tb); }
      var hb = frame(Lb, Tb, Rb, Bb);
      hb += '<line x1="' + Lb + '" y1="' + Bb + '" x2="' + Rb + '" y2="' + Bb + '" stroke="#2c3a50" stroke-width="1.4"/>';
      var nB = labels.length, gapB = (Rb - Lb) / nB;
      var iB;
      for (iB = 0; iB < nB; iB++) {
        var xcB = Lb + gapB * (iB + 0.5);
        var wB = Math.min(28, gapB * 0.45);
        hb += '<line x1="' + xcB + '" y1="' + yB(mins[iB]) + '" x2="' + xcB + '" y2="' + yB(maxs[iB]) + '" stroke="#2c3a50"/>';
        hb += '<line x1="' + (xcB - 8) + '" y1="' + yB(mins[iB]) + '" x2="' + (xcB + 8) + '" y2="' + yB(mins[iB]) + '" stroke="#2c3a50"/>';
        hb += '<line x1="' + (xcB - 8) + '" y1="' + yB(maxs[iB]) + '" x2="' + (xcB + 8) + '" y2="' + yB(maxs[iB]) + '" stroke="#2c3a50"/>';
        var yQ1 = yB(q1s[iB]), yQ3 = yB(q3s[iB]);
        hb += '<rect x="' + (xcB - wB / 2) + '" y="' + Math.min(yQ1, yQ3) + '" width="' + wB + '" height="' + Math.max(4, Math.abs(yQ1 - yQ3)) + '" fill="#c9c4ff" stroke="#6c63f5"/>';
        hb += '<line x1="' + (xcB - wB / 2) + '" y1="' + yB(meds[iB]) + '" x2="' + (xcB + wB / 2) + '" y2="' + yB(meds[iB]) + '" stroke="#2c3a50" stroke-width="2"/>';
        hb += '<text class="fa" x="' + xcB + '" y="' + (Bb + 16) + '" text-anchor="middle">' + esc(labels[iB]) + '</text>';
      }
      return wrap(head + hb);
    }

    if (t === 'ohlc') {
      var opens = pad(splitList(X.opens || '10,12,11,13').map(function (v) { return num(v, 10); }), labels.length, 10);
      var highs = pad(splitList(X.highs || '14,15,13,16').map(function (v) { return num(v, 12); }), labels.length, 12);
      var lows = pad(splitList(X.lows || '8,10,9,11').map(function (v) { return num(v, 8); }), labels.length, 8);
      var closes = pad(splitList(X.closes || '12,11,12,15').map(function (v) { return num(v, 11); }), labels.length, 11);
      var allO = opens.concat(highs, lows, closes);
      var loO = Math.min.apply(null, allO) - 1, hiO = Math.max.apply(null, allO) + 1;
      var Lo = 48, To = 28, Ro = 340, Bo = 230;
      function yO(v) { return Bo - ((v - loO) / (hiO - loO)) * (Bo - To); }
      var ho = frame(Lo, To, Ro, Bo);
      ho += '<line x1="' + Lo + '" y1="' + Bo + '" x2="' + Ro + '" y2="' + Bo + '" stroke="#2c3a50" stroke-width="1.4"/>';
      var nO = labels.length, gapO = (Ro - Lo) / nO;
      var iO;
      for (iO = 0; iO < nO; iO++) {
        var xcO = Lo + gapO * (iO + 0.5);
        var up = closes[iO] >= opens[iO];
        var colO = up ? '#27c4a8' : '#e4572e';
        ho += '<line x1="' + xcO + '" y1="' + yO(highs[iO]) + '" x2="' + xcO + '" y2="' + yO(lows[iO]) + '" stroke="' + colO + '" stroke-width="1.6"/>';
        var yOp = yO(opens[iO]), yCl = yO(closes[iO]);
        ho += '<rect x="' + (xcO - 7) + '" y="' + Math.min(yOp, yCl) + '" width="14" height="' + Math.max(3, Math.abs(yOp - yCl)) + '" fill="' + colO + '"/>';
        ho += '<text class="fa" x="' + xcO + '" y="' + (Bo + 16) + '" text-anchor="middle">' + esc(labels[iO]) + '</text>';
      }
      return wrap(head + ho);
    }

    if (t === 'combo') {
      var lineV = pad(splitList(X.vals2 || '20,35,28,40').map(function (v) { return num(v, 0); }), labels.length, 0);
      var maxCol = Math.max.apply(null, values.concat([1]));
      var maxLn = Math.max.apply(null, lineV.concat([1]));
      var chcm = chrome(title, [
        { c: '#6c63f5', n: X.s1 || 'ستون' },
        { c: '#e4572e', n: X.s2 || 'خط' }
      ]);
      var Lcm = 48, Tcm = chcm.T, Rcm = 330, Bcm = 222;
      var nCm = labels.length, gapCm = 12;
      var bwCm = Math.max(12, (Rcm - Lcm - gapCm * (nCm + 1)) / nCm);
      var hcm = chcm.html + frame(Lcm, Tcm, Rcm, Bcm);
      hcm += '<line x1="' + Lcm + '" y1="' + Bcm + '" x2="' + Rcm + '" y2="' + Bcm + '" stroke="#2c3a50" stroke-width="1.4"/>';
      var ptsCm = [];
      var iCm;
      for (iCm = 0; iCm < nCm; iCm++) {
        var xCm = Lcm + gapCm + iCm * (bwCm + gapCm);
        var hCol = (Math.max(0, values[iCm]) / maxCol) * (Bcm - Tcm - 16);
        hcm += '<rect x="' + xCm + '" y="' + (Bcm - hCol) + '" width="' + bwCm + '" height="' + hCol + '" rx="3" fill="#6c63f5"/>';
        var xLn = xCm + bwCm / 2;
        var yLn = Bcm - (Math.max(0, lineV[iCm]) / maxLn) * (Bcm - Tcm - 16);
        ptsCm.push(xLn.toFixed(1) + ',' + yLn.toFixed(1));
        hcm += '<text class="fa" x="' + xLn + '" y="' + (Bcm + 16) + '" text-anchor="middle">' + esc(labels[iCm]) + '</text>';
      }
      hcm += '<polyline points="' + ptsCm.join(' ') + '" fill="none" stroke="#e4572e" stroke-width="2.2"/>';
      ptsCm.forEach(function (p) {
        var xy = p.split(',');
        hcm += '<circle cx="' + xy[0] + '" cy="' + xy[1] + '" r="3.4" fill="#e4572e"/>';
      });
      return wrap(hcm);
    }

    if (t === 'bull') {
      var tars = pad(splitList(X.vals2 || '8,7,9,6').map(function (v) { return num(v, 8); }), labels.length, 8);
      var nBu = labels.length;
      var maxBu = Math.max.apply(null, values.concat(tars).concat([1]));
      var hbu = '';
      var iBu;
      var rowBu = Math.min(46, 210 / nBu);
      for (iBu = 0; iBu < nBu; iBu++) {
        var yBu = 36 + iBu * (rowBu + 8);
        var trackW = 230;
        hbu += '<text class="fa" x="8" y="' + (yBu + 16) + '">' + esc(labels[iBu]) + '</text>';
        hbu += '<rect x="80" y="' + (yBu + 4) + '" width="' + trackW + '" height="22" rx="3" fill="#e8edf4"/>';
        hbu += '<rect x="80" y="' + (yBu + 4) + '" width="' + (trackW * 0.6) + '" height="22" fill="#d5dce8"/>';
        hbu += '<rect x="80" y="' + (yBu + 4) + '" width="' + (trackW * 0.35) + '" height="22" fill="#c3cbd8"/>';
        var wAct = (Math.max(0, values[iBu]) / maxBu) * trackW;
        hbu += '<rect x="80" y="' + (yBu + 10) + '" width="' + wAct + '" height="10" rx="2" fill="#6c63f5"/>';
        var xTar = 80 + (Math.max(0, tars[iBu]) / maxBu) * trackW;
        hbu += '<line x1="' + xTar + '" y1="' + (yBu + 2) + '" x2="' + xTar + '" y2="' + (yBu + 28) + '" stroke="#2c3a50" stroke-width="2.4"/>';
        hbu += '<text class="tk" x="322" y="' + (yBu + 18) + '">' + values[iBu] + '</text>';
      }
      return wrap(head + hbu);
    }

    if (t === 'pyra') {
      var leftP = values;
      var rightP = pad(splitList(X.vals2 || '5,6,4,3').map(function (v) { return num(v, 0); }), labels.length, 0);
      var maxPy = Math.max.apply(null, leftP.concat(rightP).concat([1]));
      var chpy = chrome(title, [
        { c: '#6c63f5', n: X.s1 || 'چپ' },
        { c: '#27c4a8', n: X.s2 || 'راست' }
      ]);
      var Lp2 = 40, Rp2 = 320, midP = 180, Tp2 = chpy.T + 4, Bp2 = 236;
      var nPy = labels.length, rhP = (Bp2 - Tp2) / nPy;
      var hpy = chpy.html + '<line x1="' + midP + '" y1="' + Tp2 + '" x2="' + midP + '" y2="' + Bp2 + '" stroke="#2c3a50"/>';
      var iPy;
      for (iPy = 0; iPy < nPy; iPy++) {
        var yPy = Tp2 + iPy * rhP + 3;
        var wL = (Math.max(0, leftP[iPy]) / maxPy) * (midP - Lp2 - 8);
        var wR = (Math.max(0, rightP[iPy]) / maxPy) * (Rp2 - midP - 8);
        hpy += '<rect x="' + (midP - 4 - wL) + '" y="' + yPy + '" width="' + wL + '" height="' + (rhP - 6) + '" fill="#6c63f5"/>';
        hpy += '<rect x="' + (midP + 4) + '" y="' + yPy + '" width="' + wR + '" height="' + (rhP - 6) + '" fill="#27c4a8"/>';
        hpy += '<text class="fa" x="' + midP + '" y="' + (yPy + rhP / 2 + 2) + '" text-anchor="middle">' + esc(labels[iPy]) + '</text>';
      }
      return wrap(hpy);
    }

    if (t === 'mekko') {
      var wts = values.map(function (v) { return Math.max(0.05, v); });
      var sB2 = pad(splitList(X.vals2 || '40,30,50,20').map(function (v) { return num(v, 0); }), labels.length, 0);
      var sC2 = pad(splitList(X.vals3 || '30,40,20,50').map(function (v) { return num(v, 0); }), labels.length, 0);
      var sumW = wts.reduce(function (a, b) { return a + b; }, 0) || 1;
      var chm = chrome(title, [
        { c: '#6c63f5', n: X.s1 || 'سری ۱' },
        { c: '#27c4a8', n: X.s2 || 'سری ۲' },
        { c: '#f0a202', n: X.s3 || 'سری ۳' }
      ]);
      var Lm = 40, Tm = chm.T, Rm = 340, Bm = 222;
      var hm = chm.html + frame(Lm, Tm, Rm, Bm);
      var xM = Lm;
      var iM;
      for (iM = 0; iM < labels.length; iM++) {
        var wM = (wts[iM] / sumW) * (Rm - Lm);
        var totM = Math.max(0, values[iM]) + Math.max(0, sB2[iM]) + Math.max(0, sC2[iM]) || 1;
        var partsM = [values[iM], sB2[iM], sC2[iM]];
        var colsM = ['#6c63f5', '#27c4a8', '#f0a202'];
        var yM = Bm, pM;
        for (pM = 0; pM < 3; pM++) {
          var hM = (Math.max(0, partsM[pM]) / totM) * (Bm - Tm);
          yM -= hM;
          hm += '<rect x="' + xM + '" y="' + yM + '" width="' + Math.max(1, wM - 1.5) + '" height="' + hM + '" fill="' + colsM[pM] + '"/>';
        }
        hm += '<text class="fa" x="' + (xM + wM / 2) + '" y="' + (Bm + 16) + '" text-anchor="middle">' + esc(labels[iM]) + '</text>';
        xM += wM;
      }
      return wrap(hm);
    }

    if (t === 'sarea') {
      var a1 = values;
      var a2 = pad(splitList(X.vals2 || '2,3,2,4').map(function (v) { return num(v, 0); }), labels.length, 0);
      var a3 = pad(splitList(X.vals3 || '1,1,2,1').map(function (v) { return num(v, 0); }), labels.length, 0);
      var nSa = labels.length;
      var stacks = [];
      var maxSa = 1, iSa;
      for (iSa = 0; iSa < nSa; iSa++) {
        var t1 = Math.max(0, a1[iSa]), t2 = t1 + Math.max(0, a2[iSa]), t3 = t2 + Math.max(0, a3[iSa]);
        stacks.push([t1, t2, t3]);
        maxSa = Math.max(maxSa, t3);
      }
      var chsa = chrome(title, [
        { c: '#6c63f5', n: X.s1 || 'سری ۱' },
        { c: '#27c4a8', n: X.s2 || 'سری ۲' },
        { c: '#f0a202', n: X.s3 || 'سری ۳' }
      ]);
      var Lsa = 48, Tsa = chsa.T, Rsa = 340, Bsa = 222;
      function xSa(i) { return Lsa + (nSa === 1 ? (Rsa - Lsa) / 2 : i * (Rsa - Lsa) / (nSa - 1)); }
      function ySa(v) { return Bsa - (v / maxSa) * (Bsa - Tsa - 8); }
      var hsa = chsa.html + frame(Lsa, Tsa, Rsa, Bsa);
      hsa += '<line x1="' + Lsa + '" y1="' + Bsa + '" x2="' + Rsa + '" y2="' + Bsa + '" stroke="#2c3a50" stroke-width="1.4"/>';
      var fills = ['rgba(108,99,245,.45)', 'rgba(39,196,168,.45)', 'rgba(240,162,2,.45)'];
      var strokes = ['#6c63f5', '#27c4a8', '#f0a202'];
      var layer;
      for (layer = 2; layer >= 0; layer--) {
        var top = [], iL;
        for (iL = 0; iL < nSa; iL++) top.push(xSa(iL).toFixed(1) + ',' + ySa(stacks[iL][layer]).toFixed(1));
        var bot = [];
        for (iL = nSa - 1; iL >= 0; iL--) {
          var base = layer === 0 ? 0 : stacks[iL][layer - 1];
          bot.push(xSa(iL).toFixed(1) + ',' + ySa(base).toFixed(1));
        }
        hsa += '<polygon points="' + top.concat(bot).join(' ') + '" fill="' + fills[layer] + '" stroke="none"/>';
        hsa += '<polyline points="' + top.join(' ') + '" fill="none" stroke="' + strokes[layer] + '" stroke-width="1.8"/>';
      }
      for (iSa = 0; iSa < nSa; iSa++) {
        hsa += '<text class="fa" x="' + xSa(iSa) + '" y="' + (Bsa + 16) + '" text-anchor="middle">' + esc(labels[iSa]) + '</text>';
      }
      return wrap(hsa);
    }

    if (t === 'hcmp') {
      var valsH = pad(splitList(X.vals2 || '5,4,6,2').map(function (v) { return num(v, 0); }), labels.length, 0);
      var nHc = labels.length;
      var maxHc = Math.max.apply(null, values.concat(valsH).concat([1]));
      var chh = chrome(title, [
        { c: '#6c63f5', n: X.s1 || 'سری ۱' },
        { c: '#27c4a8', n: X.s2 || 'سری ۲' }
      ]);
      var Lhc = 86, Thc = chh.T, Rhc = 340, Bhc = 236;
      var rowHc = Math.max(18, (Bhc - Thc) / nHc);
      var hhc = chh.html + frame(36, Thc, Rhc, Bhc);
      hhc += '<line x1="' + Lhc + '" y1="' + Thc + '" x2="' + Lhc + '" y2="' + Bhc + '" stroke="#2c3a50" stroke-width="1.4"/>';
      var iHc;
      for (iHc = 0; iHc < nHc; iHc++) {
        var yHc = Thc + 6 + iHc * rowHc;
        var bhHc = Math.max(6, rowHc * 0.32);
        var wA = (Math.max(0, values[iHc]) / maxHc) * (Rhc - Lhc - 16);
        var wB = (Math.max(0, valsH[iHc]) / maxHc) * (Rhc - Lhc - 16);
        hhc += '<rect x="' + Lhc + '" y="' + yHc + '" width="' + wA + '" height="' + bhHc + '" rx="3" fill="#6c63f5"/>';
        hhc += '<rect x="' + Lhc + '" y="' + (yHc + bhHc + 3) + '" width="' + wB + '" height="' + bhHc + '" rx="3" fill="#27c4a8"/>';
        hhc += '<text class="fa" x="' + (Lhc - 6) + '" y="' + (yHc + bhHc + 4) + '" text-anchor="end">' + esc(labels[iHc]) + '</text>';
      }
      return wrap(hhc);
    }

    if (t === 'map') {
      if (!X.labs || X.labs === 'A,B,C,D') labels = [];
      if (!X.vals || X.vals === '4,7,3,6') values = [12, 8, 15, 10, 22, 9];
      var regs = [
        { name: 'شمال‌غرب', d: 'M24,16 L72,10 L77,53 L32,58 L14,38 Z', lx: 44, ly: 40 },
        { name: 'شمال', d: 'M72,10 L137,16 L137,53 L77,53 Z', lx: 104, ly: 36 },
        { name: 'شمال‌شرق', d: 'M137,16 L167,30 L187,53 L197,88 L147,93 L137,53 Z', lx: 164, ly: 56 },
        { name: 'غرب', d: 'M14,38 L32,58 L50,113 L10,133 L0,103 L4,68 Z', lx: 22, ly: 86 },
        { name: 'مرکز', d: 'M77,53 L137,53 L147,93 L107,138 L50,113 L32,58 Z', lx: 92, ly: 90 },
        { name: 'جنوب', d: 'M50,113 L107,138 L147,93 L197,88 L194,123 L180,153 L152,173 L112,186 L67,180 L32,166 L10,133 Z', lx: 102, ly: 152 }
      ];
      var mapLabs = labels.length ? labels : regs.map(function (r) { return r.name; });
      var mapVals = values.slice();
      while (mapVals.length < regs.length) mapVals.push(0);
      var minM = Math.min.apply(null, mapVals), maxM = Math.max.apply(null, mapVals.concat([minM + 1]));
      function heat(v) {
        var f = (v - minM) / (maxM - minM || 1);
        var r = Math.round(198 - f * 90), g = Math.round(220 - f * 70), b = Math.round(255 - f * 30);
        if (f > 0.45) { r = Math.round(80 + (1 - f) * 70); g = Math.round(140 - f * 40); b = Math.round(200 - f * 20); }
        return 'rgb(' + r + ',' + g + ',' + b + ')';
      }
      var mapItems = regs.map(function (r, ix) { return { c: heat(mapVals[ix]), n: mapLabs[ix] || r.name }; });
      var chmp = chrome(title, mapItems);
      var Tm = chmp.T, Bm = 268, Lm = 22, Rm = 338;
      var boxH = Bm - Tm, boxW = Rm - Lm;
      var hm2 = chmp.html;
      hm2 += '<rect x="' + Lm + '" y="' + Tm + '" width="' + boxW + '" height="' + boxH + '" rx="10" fill="#f4f7fb" stroke="#d5dce6"/>';
      var sc = Math.min(boxW / 210, boxH / 196);
      var ox = Lm + (boxW - 200 * sc) / 2;
      var oy = Tm + (boxH - 190 * sc) / 2;
      hm2 += '<g transform="translate(' + ox.toFixed(1) + ',' + oy.toFixed(1) + ') scale(' + sc.toFixed(3) + ')">';
      regs.forEach(function (r, ix) {
        hm2 += '<path d="' + r.d + '" fill="' + heat(mapVals[ix]) + '" stroke="#fff" stroke-width="1.8"/>';
        hm2 += '<text class="onf" x="' + r.lx + '" y="' + r.ly + '" text-anchor="middle">' + esc(mapLabs[ix] || r.name) + '</text>';
      });
      hm2 += '</g>';
      return wrap(hm2);
    }

    if (t === 'surf') {
      var nrS = Math.max(2, Math.min(6, Math.round(num(X.nrows, 4))));
      var ncS = Math.max(2, Math.min(6, Math.round(num(X.ncols, 4))));
      var zraw = splitList(X.vals || '1,2,3,2,2,4,5,3,3,5,6,4,2,3,4,3').map(function (v) { return num(v, 0); });
      var grid = [], irS, icS;
      for (irS = 0; irS < nrS; irS++) {
        grid[irS] = [];
        for (icS = 0; icS < ncS; icS++) grid[irS][icS] = zraw[irS * ncS + icS] != null ? zraw[irS * ncS + icS] : 0;
      }
      var zmin = Infinity, zmax = -Infinity;
      for (irS = 0; irS < nrS; irS++) for (icS = 0; icS < ncS; icS++) {
        zmin = Math.min(zmin, grid[irS][icS]); zmax = Math.max(zmax, grid[irS][icS]);
      }
      if (zmax <= zmin) zmax = zmin + 1;
      function colZ(z) {
        var f = (z - zmin) / (zmax - zmin);
        if (f < 0.33) return 'rgb(' + Math.round(80 + f * 80) + ',' + Math.round(160 + f * 40) + ',' + Math.round(220 - f * 40) + ')';
        if (f < 0.66) return 'rgb(' + Math.round(90 + f * 80) + ',' + Math.round(190 - f * 40) + ',' + Math.round(120) + ')';
        return 'rgb(' + Math.round(200 + f * 40) + ',' + Math.round(160 - f * 80) + ',' + Math.round(70) + ')';
      }
      var chs = chrome(title, [
        { c: 'rgb(80,160,220)', n: 'کم' },
        { c: 'rgb(140,170,120)', n: 'متوسط' },
        { c: 'rgb(230,90,70)', n: 'زیاد' }
      ]);
      var Ts = chs.T, Bs = 266, Ls = 22, Rs = 338;
      var boxH = Bs - Ts, boxW = Rs - Ls;
      var steps = Math.max(1, nrS + ncS - 2);
      var sz = Math.min(42, boxH * 0.22);
      var sy = Math.min(11, (boxH - 28 - sz) / steps);
      var sx = Math.min(24, (boxW * 0.40) / Math.max(1, Math.max(nrS, ncS) - 1));
      var baseY = Ts + 16 + sz;
      function iso(i, j, z) {
        return {
          x: 180 + (i - j) * sx,
          y: baseY + (i + j) * sy - ((z - zmin) / (zmax - zmin)) * sz
        };
      }
      var faces = [];
      for (irS = 0; irS < nrS - 1; irS++) {
        for (icS = 0; icS < ncS - 1; icS++) {
          var p00 = iso(irS, icS, grid[irS][icS]);
          var p10 = iso(irS + 1, icS, grid[irS + 1][icS]);
          var p11 = iso(irS + 1, icS + 1, grid[irS + 1][icS + 1]);
          var p01 = iso(irS, icS + 1, grid[irS][icS + 1]);
          var avg = (grid[irS][icS] + grid[irS + 1][icS] + grid[irS + 1][icS + 1] + grid[irS][icS + 1]) / 4;
          faces.push({ z: irS + icS, avg: avg, pts: [p00, p10, p11, p01] });
        }
      }
      faces.sort(function (a, b) { return a.z - b.z; });
      var hsu = chs.html;
      hsu += '<rect x="' + Ls + '" y="' + Ts + '" width="' + boxW + '" height="' + boxH + '" rx="10" fill="#f7f9fc" stroke="#d5dce6"/>';
      hsu += '<clipPath id="sfClip0"><rect x="' + (Ls + 2) + '" y="' + (Ts + 2) + '" width="' + (boxW - 4) + '" height="' + (boxH - 4) + '" rx="8"/></clipPath>';
      hsu += '<g clip-path="url(#sfClip0)">';
      faces.forEach(function (f) {
        hsu += '<polygon points="' + f.pts.map(function (p) { return p.x.toFixed(1) + ',' + p.y.toFixed(1); }).join(' ') +
          '" fill="' + colZ(f.avg) + '" fill-opacity=".9" stroke="#2c3a50" stroke-width=".6"/>';
      });
      hsu += '</g>';
      hsu += '<text class="tk" x="' + (Ls + 10) + '" y="' + (Bs - 8) + '">x</text>';
      hsu += '<text class="tk" x="' + (Rs - 16) + '" y="' + (Bs - 8) + '">y</text>';
      hsu += '<text class="tk" x="180" y="' + (Ts + 14) + '" text-anchor="middle">z</text>';
      return wrap(hsu);
    }

    if (t === 'st100') {
      var a100 = values.map(function (v) { return Math.max(0, v); });
      var b100 = pad(splitList(X.vals2 || '2,3,1,2').map(function (v) { return num(v, 0); }), a100.length, 0);
      var c100 = pad(splitList(X.vals3 || '1,1,2,1').map(function (v) { return num(v, 0); }), a100.length, 0);
      var n100 = labels.length, i100;
      var ch100 = chrome(title, [
        { c: '#6c63f5', n: X.s1 || 'سری ۱' }, { c: '#27c4a8', n: X.s2 || 'سری ۲' }, { c: '#f0a202', n: X.s3 || 'سری ۳' }
      ]);
      var L100 = 48, T100 = ch100.T, R100 = 340, B100 = 222, g100 = 14;
      var w100 = Math.max(14, (R100 - L100 - g100 * (n100 + 1)) / n100);
      var h100 = ch100.html + frame(L100, T100, R100, B100);
      h100 += '<line x1="' + L100 + '" y1="' + B100 + '" x2="' + R100 + '" y2="' + B100 + '" stroke="#2c3a50" stroke-width="1.5"/>';
      for (i100 = 0; i100 < n100; i100++) {
        var tot100 = Math.max(0.0001, Math.max(0, a100[i100]) + Math.max(0, b100[i100]) + Math.max(0, c100[i100]));
        var x100 = L100 + g100 + i100 * (w100 + g100), y100 = B100;
        var parts100 = [a100[i100], b100[i100], c100[i100]], cols100 = ['#6c63f5', '#27c4a8', '#f0a202'], p100;
        for (p100 = 0; p100 < 3; p100++) {
          var ph100 = (Math.max(0, parts100[p100]) / tot100) * (B100 - T100 - 10);
          y100 -= ph100;
          h100 += '<rect x="' + x100 + '" y="' + y100 + '" width="' + w100 + '" height="' + ph100 + '" fill="' + cols100[p100] + '"/>';
        }
        h100 += '<text class="fa" x="' + (x100 + w100 / 2) + '" y="' + (B100 + 16) + '" text-anchor="middle">' + esc(labels[i100]) + '</text>';
      }
      return wrap(h100);
    }

    if (t === 'lolli') {
      var maxL = Math.max.apply(null, values.concat([1]));
      var nL = labels.length, gL = 16, LL = 48, TL = 32, RL = 340, BL = 228;
      var hL = frame(LL, TL, RL, BL);
      hL += '<line x1="' + LL + '" y1="' + BL + '" x2="' + RL + '" y2="' + BL + '" stroke="#2c3a50" stroke-width="1.5"/>';
      for (var iL = 0; iL < nL; iL++) {
        var xL = LL + gL + (iL + 0.5) * ((RL - LL - 2 * gL) / nL);
        var yL = BL - (Math.max(0, values[iL]) / maxL) * (BL - TL - 16);
        hL += '<line x1="' + xL + '" y1="' + BL + '" x2="' + xL + '" y2="' + yL + '" stroke="' + colors[iL % colors.length] + '" stroke-width="3"/>';
        hL += '<circle cx="' + xL + '" cy="' + yL + '" r="7" fill="' + colors[iL % colors.length] + '" stroke="#fff" stroke-width="1.5"/>';
        hL += '<text class="tk" x="' + xL + '" y="' + (yL - 12) + '" text-anchor="middle">' + values[iL] + '</text>';
        hL += '<text class="fa" x="' + xL + '" y="' + (BL + 16) + '" text-anchor="middle">' + esc(labels[iL]) + '</text>';
      }
      return wrap(head + hL);
    }

    if (t === 'dumb') {
      var dA = values;
      var dB = pad(splitList(X.vals2 || '6,4,8,5').map(function (v) { return num(v, 0); }), dA.length, 0);
      var maxD = Math.max.apply(null, dA.concat(dB).concat([1]));
      var chD = chrome(title, [{ c: '#6c63f5', n: X.s1 || 'شروع' }, { c: '#e4572e', n: X.s2 || 'پایان' }]);
      var LD = 86, TD = chD.T, RD = 336, BD = 248, nD = labels.length;
      var rowD = (BD - TD) / Math.max(1, nD);
      var hD = chD.html + frame(LD, TD, RD, BD);
      for (var iD = 0; iD < nD; iD++) {
        var yD = TD + rowD * (iD + 0.5);
        var x1D = LD + 10 + (Math.max(0, dA[iD]) / maxD) * (RD - LD - 24);
        var x2D = LD + 10 + (Math.max(0, dB[iD]) / maxD) * (RD - LD - 24);
        hD += '<text class="fa" x="' + (LD - 8) + '" y="' + (yD + 4) + '" text-anchor="end">' + esc(labels[iD]) + '</text>';
        hD += '<line x1="' + x1D + '" y1="' + yD + '" x2="' + x2D + '" y2="' + yD + '" stroke="#94a3b8" stroke-width="3"/>';
        hD += '<circle cx="' + x1D + '" cy="' + yD + '" r="6" fill="#6c63f5"/>';
        hD += '<circle cx="' + x2D + '" cy="' + yD + '" r="6" fill="#e4572e"/>';
      }
      return wrap(hD);
    }

    if (t === 'step' || t === 'spark') {
      var nS = Math.max(labels.length, values.length, 2);
      var Ls2 = t === 'spark' ? 18 : 48, Ts2 = t === 'spark' ? 36 : 36, Rs2 = t === 'spark' ? 342 : 340, Bs2 = t === 'spark' ? 210 : 228;
      var minS = Math.min.apply(null, values), maxS = Math.max.apply(null, values);
      if (maxS === minS) { maxS = minS + 1; minS = minS - 1; }
      function xS(i) { return Ls2 + (nS === 1 ? 0 : i * (Rs2 - Ls2) / (nS - 1)); }
      function yS(v) { return Bs2 - ((v - minS) / (maxS - minS)) * (Bs2 - Ts2 - 8); }
      var hS = t === 'spark' ? '' : frame(Ls2, Ts2, Rs2, Bs2);
      if (t !== 'spark') hS += '<line x1="' + Ls2 + '" y1="' + Bs2 + '" x2="' + Rs2 + '" y2="' + Bs2 + '" stroke="#2c3a50" stroke-width="1.4"/>';
      var ptsS = [], iS;
      if (t === 'step') {
        for (iS = 0; iS < nS; iS++) {
          var xv = xS(iS), yv = yS(values[iS] != null ? values[iS] : 0);
          if (iS) ptsS.push(xv.toFixed(1) + ',' + yS(values[iS - 1] != null ? values[iS - 1] : 0).toFixed(1));
          ptsS.push(xv.toFixed(1) + ',' + yv.toFixed(1));
        }
      } else {
        for (iS = 0; iS < nS; iS++) ptsS.push(xS(iS).toFixed(1) + ',' + yS(values[iS] != null ? values[iS] : 0).toFixed(1));
      }
      hS += '<polyline points="' + ptsS.join(' ') + '" fill="none" stroke="#6c63f5" stroke-width="' + (t === 'spark' ? '2.6' : '2.2') + '" stroke-linejoin="round"/>';
      if (t === 'spark') {
        var lastV = values[nS - 1] != null ? values[nS - 1] : 0;
        hS += '<circle cx="' + xS(nS - 1) + '" cy="' + yS(lastV) + '" r="5" fill="#e4572e"/>';
        hS += '<text class="ttl" x="180" y="22" text-anchor="middle">' + esc(title || 'اسپارک‌لاین') + '  ' + lastV + '</text>';
      } else {
        for (iS = 0; iS < nS; iS++) {
          hS += '<circle cx="' + xS(iS) + '" cy="' + yS(values[iS] != null ? values[iS] : 0) + '" r="3.2" fill="#6c63f5"/>';
          if (labels[iS]) hS += '<text class="fa" x="' + xS(iS) + '" y="' + (Bs2 + 16) + '" text-anchor="middle">' + esc(labels[iS]) + '</text>';
        }
      }
      return wrap(head + hS);
    }

    if (t === 'slope') {
      var slA = values;
      var slB = pad(splitList(X.vals2 || '6,3,8,4').map(function (v) { return num(v, 0); }), slA.length, 0);
      var maxSl = Math.max.apply(null, slA.concat(slB).concat([1]));
      var minSl = Math.min.apply(null, slA.concat(slB).concat([0]));
      if (maxSl === minSl) maxSl = minSl + 1;
      var chSl = chrome(title, [{ c: '#6c63f5', n: X.s1 || 'قبل' }, { c: '#27c4a8', n: X.s2 || 'بعد' }]);
      var Tsl = chSl.T + 8, Bsl = 246, xLsl = 90, xRsl = 270;
      var hSl = chSl.html;
      hSl += '<line x1="' + xLsl + '" y1="' + Tsl + '" x2="' + xLsl + '" y2="' + Bsl + '" stroke="#94a3b8"/>';
      hSl += '<line x1="' + xRsl + '" y1="' + Tsl + '" x2="' + xRsl + '" y2="' + Bsl + '" stroke="#94a3b8"/>';
      hSl += '<text class="fa" x="' + xLsl + '" y="' + (Tsl - 8) + '" text-anchor="middle">' + esc(X.s1 || 'قبل') + '</text>';
      hSl += '<text class="fa" x="' + xRsl + '" y="' + (Tsl - 8) + '" text-anchor="middle">' + esc(X.s2 || 'بعد') + '</text>';
      function ySl(v) { return Bsl - ((v - minSl) / (maxSl - minSl)) * (Bsl - Tsl - 10); }
      for (var iSl = 0; iSl < labels.length; iSl++) {
        var y1s = ySl(slA[iSl] || 0), y2s = ySl(slB[iSl] || 0);
        var colSl = slB[iSl] >= slA[iSl] ? '#27c4a8' : '#e4572e';
        hSl += '<line x1="' + xLsl + '" y1="' + y1s + '" x2="' + xRsl + '" y2="' + y2s + '" stroke="' + colSl + '" stroke-width="2.2"/>';
        hSl += '<circle cx="' + xLsl + '" cy="' + y1s + '" r="4.5" fill="' + colSl + '"/>';
        hSl += '<circle cx="' + xRsl + '" cy="' + y2s + '" r="4.5" fill="' + colSl + '"/>';
        hSl += '<text class="fa" x="' + (xLsl - 8) + '" y="' + (y1s + 4) + '" text-anchor="end">' + esc(labels[iSl]) + '</text>';
      }
      return wrap(hSl);
    }

    if (t === 'stream') {
      var stA = values.map(function (v) { return Math.max(0, v); });
      var stB = pad(splitList(X.vals2 || '3,4,5,3,4').map(function (v) { return num(v, 0); }), stA.length, 0);
      var stC = pad(splitList(X.vals3 || '2,1,3,2,2').map(function (v) { return num(v, 0); }), stA.length, 0);
      var nSt = Math.max(stA.length, 2);
      var chSt = chrome(title, [
        { c: '#6c63f5', n: X.s1 || 'سری ۱' }, { c: '#27c4a8', n: X.s2 || 'سری ۲' }, { c: '#f0a202', n: X.s3 || 'سری ۳' }
      ]);
      var Lst = 28, Tst = chSt.T, Rst = 340, Bst = 246, mid = (Tst + Bst) / 2;
      var maxSt = 1, iSt;
      for (iSt = 0; iSt < nSt; iSt++) maxSt = Math.max(maxSt, stA[iSt] + stB[iSt] + stC[iSt]);
      function xSt(i) { return Lst + i * (Rst - Lst) / Math.max(1, nSt - 1); }
      function band(arr, off) {
        var top = [], bot = [], k;
        for (k = 0; k < nSt; k++) {
          var half = ((arr[k] || 0) / maxSt) * (Bst - Tst - 20) / 2;
          var base = ((off[k] || 0) / maxSt) * (Bst - Tst - 20) / 2;
          top.push(xSt(k).toFixed(1) + ',' + (mid - base - half).toFixed(1));
          bot.unshift(xSt(k).toFixed(1) + ',' + (mid - base).toFixed(1));
        }
        return top.concat(bot).join(' ');
      }
      var off0 = stA.map(function () { return 0; });
      var off1 = stA.slice();
      var off2 = stA.map(function (v, i) { return v + stB[i]; });
      var hSt = chSt.html + frame(Lst, Tst, Rst, Bst);
      hSt += '<polygon points="' + band(stC, off2) + '" fill="#f0a202" fill-opacity=".85"/>';
      hSt += '<polygon points="' + band(stB, off1) + '" fill="#27c4a8" fill-opacity=".85"/>';
      hSt += '<polygon points="' + band(stA, off0) + '" fill="#6c63f5" fill-opacity=".85"/>';
      return wrap(hSt);
    }

    if (t === 'viol' || t === 'strip') {
      var minsV = pad(splitList(X.mins || '').map(function (v) { return num(v, NaN); }), labels.length, NaN);
      var q1V = pad(splitList(X.q1s || '').map(function (v) { return num(v, NaN); }), labels.length, NaN);
      var medV = pad(splitList(X.meds || '').map(function (v) { return num(v, NaN); }), labels.length, NaN);
      var q3V = pad(splitList(X.q3s || '').map(function (v) { return num(v, NaN); }), labels.length, NaN);
      var maxsV = pad(splitList(X.maxs || '').map(function (v) { return num(v, NaN); }), labels.length, NaN);
      var nV = labels.length, iV;
      for (iV = 0; iV < nV; iV++) {
        if (!isFinite(minsV[iV])) minsV[iV] = Math.max(0, (values[iV] || 4) - 2);
        if (!isFinite(q1V[iV])) q1V[iV] = Math.max(0, (values[iV] || 4) - 1);
        if (!isFinite(medV[iV])) medV[iV] = values[iV] || 4;
        if (!isFinite(q3V[iV])) q3V[iV] = (values[iV] || 4) + 1;
        if (!isFinite(maxsV[iV])) maxsV[iV] = (values[iV] || 4) + 2;
      }
      var loV = Math.min.apply(null, minsV), hiV = Math.max.apply(null, maxsV);
      if (hiV === loV) hiV = loV + 1;
      var LV = 48, TV = 32, RV = 340, BV = 226;
      var hV = frame(LV, TV, RV, BV);
      hV += '<line x1="' + LV + '" y1="' + BV + '" x2="' + RV + '" y2="' + BV + '" stroke="#2c3a50" stroke-width="1.4"/>';
      var slotV = (RV - LV) / nV;
      function yVV(v) { return BV - ((v - loV) / (hiV - loV)) * (BV - TV - 12); }
      for (iV = 0; iV < nV; iV++) {
        var cxV = LV + slotV * (iV + 0.5);
        if (t === 'viol') {
          var yMin = yVV(minsV[iV]), yQ1 = yVV(q1V[iV]), yMed = yVV(medV[iV]), yQ3 = yVV(q3V[iV]), yMax = yVV(maxsV[iV]);
          var wMid = Math.min(28, slotV * 0.38), wEnd = Math.min(10, slotV * 0.16);
          hV += '<path d="M' + cxV + ',' + yMax + ' C' + (cxV + wEnd) + ',' + yMax + ' ' + (cxV + wMid) + ',' + yQ3 + ' ' + (cxV + wMid) + ',' + yMed +
            ' C' + (cxV + wMid) + ',' + yQ1 + ' ' + (cxV + wEnd) + ',' + yMin + ' ' + cxV + ',' + yMin +
            ' C' + (cxV - wEnd) + ',' + yMin + ' ' + (cxV - wMid) + ',' + yQ1 + ' ' + (cxV - wMid) + ',' + yMed +
            ' C' + (cxV - wMid) + ',' + yQ3 + ' ' + (cxV - wEnd) + ',' + yMax + ' ' + cxV + ',' + yMax +
            ' Z" fill="' + colors[iV % colors.length] + '" fill-opacity=".35" stroke="' + colors[iV % colors.length] + '"/>';
          hV += '<line x1="' + (cxV - 10) + '" y1="' + yMed + '" x2="' + (cxV + 10) + '" y2="' + yMed + '" stroke="#243044" stroke-width="2"/>';
        } else {
          var dots = [minsV[iV], q1V[iV], medV[iV], q3V[iV], maxsV[iV], values[iV]];
          dots.forEach(function (dv, di) {
            var jx = cxV + ((di % 3) - 1) * 6;
            hV += '<circle cx="' + jx + '" cy="' + yVV(dv) + '" r="3.4" fill="' + colors[iV % colors.length] + '" fill-opacity=".85"/>';
          });
        }
        hV += '<text class="fa" x="' + cxV + '" y="' + (BV + 16) + '" text-anchor="middle">' + esc(labels[iV]) + '</text>';
      }
      return wrap(head + hV);
    }

    if (t === 'stem') {
      var nums = values.slice();
      if (!X.vals) nums = [12, 15, 18, 21, 22, 27, 31, 33, 34, 41];
      var buckets = {};
      nums.forEach(function (v) {
        var n = Math.round(v);
        var st = Math.floor(n / 10), lf = Math.abs(n % 10);
        if (!buckets[st]) buckets[st] = [];
        buckets[st].push(lf);
      });
      var keys = Object.keys(buckets).map(Number).sort(function (a, b) { return a - b; });
      var hStem = '<text class="ttl" x="180" y="18" text-anchor="middle">' + esc(title || 'ساقه و برگ') + '</text>';
      hStem += '<text class="tk" x="70" y="40">ساقه</text><text class="tk" x="120" y="40">برگ</text>';
      keys.forEach(function (k, ix) {
        var yk = 62 + ix * 22;
        buckets[k].sort(function (a, b) { return a - b; });
        hStem += '<text class="ttl" x="70" y="' + yk + '" text-anchor="middle">' + k + '</text>';
        hStem += '<text class="fa" x="100" y="' + yk + '">|  ' + buckets[k].join('  ') + '</text>';
      });
      return wrap(hStem);
    }

    if (t === 'waff') {
      var totW = values.reduce(function (s, v) { return s + Math.max(0, v); }, 0) || 1;
      var cells = [];
      labels.forEach(function (lb, ix) {
        var n = Math.round(100 * Math.max(0, values[ix] || 0) / totW);
        for (var k = 0; k < n; k++) cells.push(ix);
      });
      while (cells.length < 100) cells.push(-1);
      cells = cells.slice(0, 100);
      var chW = chrome(title, labels.map(function (lb, ix) { return { c: colors[ix % colors.length], n: lb }; }));
      var Lw = 40, Tw = chW.T, size = 22, gapw = 3;
      var hw = chW.html;
      for (var rW = 0; rW < 10; rW++) {
        for (var cW = 0; cW < 10; cW++) {
          var idw = rW * 10 + cW, colw = cells[idw] < 0 ? '#e2e8f0' : colors[cells[idw] % colors.length];
          hw += '<rect x="' + (Lw + cW * (size + gapw)) + '" y="' + (Tw + rW * (size + gapw)) + '" width="' + size + '" height="' + size + '" rx="4" fill="' + colw + '"/>';
        }
      }
      return wrap(hw);
    }

    if (t === 'smat') {
      var xsM = splitList(X.xs || '1,2,3,4,5,6').map(function (v) { return num(v, 0); });
      var ysM = splitList(X.ys || '2,3,1,5,4,3').map(function (v) { return num(v, 0); });
      var zsM = splitList(X.zs || '3,1,4,2,5,3').map(function (v) { return num(v, 0); });
      var nM = Math.min(xsM.length, ysM.length, zsM.length) || 1;
      var series = [xsM.slice(0, nM), ysM.slice(0, nM), zsM.slice(0, nM)];
      var namesM = [X.s1 || 'X', X.s2 || 'Y', X.s3 || 'Z'];
      var hM = head;
      var cell = 78, gapm = 8, oxm = 48, oym = 40;
      function mm(arr) { return { a: Math.min.apply(null, arr), b: Math.max.apply(null, arr) }; }
      var iR, iC, iP;
      for (iR = 0; iR < 3; iR++) {
        for (iC = 0; iC < 3; iC++) {
          var x0 = oxm + iC * (cell + gapm), y0 = oym + iR * (cell + gapm);
          hM += '<rect x="' + x0 + '" y="' + y0 + '" width="' + cell + '" height="' + cell + '" fill="#fbfcfe" stroke="#d5dce6"/>';
          if (iR === iC) {
            hM += '<text class="fa" x="' + (x0 + cell / 2) + '" y="' + (y0 + cell / 2 + 4) + '" text-anchor="middle">' + esc(namesM[iR]) + '</text>';
          } else {
            var xr = mm(series[iC]), yr = mm(series[iR]);
            if (xr.b === xr.a) xr.b = xr.a + 1;
            if (yr.b === yr.a) yr.b = yr.a + 1;
            for (iP = 0; iP < nM; iP++) {
              var px = x0 + 6 + ((series[iC][iP] - xr.a) / (xr.b - xr.a)) * (cell - 12);
              var py = y0 + cell - 6 - ((series[iR][iP] - yr.a) / (yr.b - yr.a)) * (cell - 12);
              hM += '<circle cx="' + px.toFixed(1) + '" cy="' + py.toFixed(1) + '" r="3" fill="#6c63f5" fill-opacity=".8"/>';
            }
          }
        }
      }
      return wrap(hM);
    }

    if (t === 'dend') {
      var labsD = labels.length ? labels : ['A', 'B', 'C', 'D', 'E', 'F'];
      var nDen = labsD.length;
      var Ldn = 24, Rdn = 336, Bdn = 250, Tdn = 36;
      var hDen = head;
      hDen += '<line x1="' + Ldn + '" y1="' + Tdn + '" x2="' + Ldn + '" y2="' + Bdn + '" stroke="#cbd5e1"/>';
      var xsD = [], iDen;
      for (iDen = 0; iDen < nDen; iDen++) {
        xsD.push(Ldn + 28 + iDen * ((Rdn - Ldn - 36) / Math.max(1, nDen - 1)));
        hDen += '<text class="fa" x="' + xsD[iDen] + '" y="' + (Bdn + 14) + '" text-anchor="middle">' + esc(labsD[iDen]) + '</text>';
        hDen += '<line x1="' + xsD[iDen] + '" y1="' + Bdn + '" x2="' + xsD[iDen] + '" y2="' + (Bdn - 18) + '" stroke="#6c63f5"/>';
      }
      var level = xsD.map(function (x, i) { return { x: x, y: Bdn - 18, lab: labsD[i] }; });
      var stepY = Math.max(22, (Bdn - Tdn - 40) / Math.max(1, nDen - 1));
      while (level.length > 1) {
        var nxt = [], j;
        for (j = 0; j < level.length; j += 2) {
          if (j + 1 >= level.length) { nxt.push(level[j]); break; }
          var a = level[j], b = level[j + 1];
          var yJoin = Math.min(a.y, b.y) - stepY;
          var xm = (a.x + b.x) / 2;
          hDen += '<path d="M' + a.x + ',' + a.y + ' V' + yJoin + ' H' + b.x + ' V' + b.y + '" fill="none" stroke="#6c63f5" stroke-width="1.6"/>';
          nxt.push({ x: xm, y: yJoin });
        }
        level = nxt;
      }
      return wrap(hDen);
    }

    if (t === 'sank') {
      var edges = parsePairs(X.vals || 'A-C:8,A-D:4,B-C:3,B-D:7');
      if (!edges.length) edges = [{ a: 'A', b: 'C', v: 8 }, { a: 'B', b: 'D', v: 5 }];
      var leftN = [], rightN = [], mapL = {}, mapR = {};
      edges.forEach(function (e) {
        if (mapL[e.a] == null) { mapL[e.a] = leftN.length; leftN.push({ n: e.a, v: 0 }); }
        if (mapR[e.b] == null) { mapR[e.b] = rightN.length; rightN.push({ n: e.b, v: 0 }); }
        leftN[mapL[e.a]].v += e.v; rightN[mapR[e.b]].v += e.v;
      });
      var totL = leftN.reduce(function (s, n) { return s + n.v; }, 0) || 1;
      var totR = rightN.reduce(function (s, n) { return s + n.v; }, 0) || 1;
      var Tsk = 36, Bsk = 250, Hsk = Bsk - Tsk;
      function layout(nodes, tot, x) {
        var y = Tsk, gap = 8, usable = Hsk - gap * Math.max(0, nodes.length - 1);
        return nodes.map(function (nd) {
          var h = Math.max(10, nd.v / tot * usable);
          var o = { n: nd.n, v: nd.v, x: x, y: y, h: h };
          y += h + gap;
          return o;
        });
      }
      var Lnodes = layout(leftN, totL, 36), Rnodes = layout(rightN, totR, 300);
      var usedL = {}, usedR = {}, hSk = head;
      Lnodes.forEach(function (nd, ix) {
        hSk += '<rect x="' + nd.x + '" y="' + nd.y + '" width="18" height="' + nd.h + '" rx="3" fill="' + colors[ix % colors.length] + '"/>';
        hSk += '<text class="fa" x="' + (nd.x - 4) + '" y="' + (nd.y + nd.h / 2 + 4) + '" text-anchor="end">' + esc(nd.n) + '</text>';
        usedL[nd.n] = nd.y;
      });
      Rnodes.forEach(function (nd, ix) {
        hSk += '<rect x="' + nd.x + '" y="' + nd.y + '" width="18" height="' + nd.h + '" rx="3" fill="' + colors[(ix + 3) % colors.length] + '"/>';
        hSk += '<text class="fa" x="' + (nd.x + 24) + '" y="' + (nd.y + nd.h / 2 + 4) + '">' + esc(nd.n) + '</text>';
        usedR[nd.n] = nd.y;
      });
      edges.forEach(function (e, ix) {
        var a = Lnodes[mapL[e.a]], b = Rnodes[mapR[e.b]];
        var ha = Math.max(4, e.v / totL * (Hsk - 8 * Math.max(0, leftN.length - 1)));
        var hb = Math.max(4, e.v / totR * (Hsk - 8 * Math.max(0, rightN.length - 1)));
        var y1 = usedL[e.a], y2 = usedR[e.b];
        usedL[e.a] += ha; usedR[e.b] += hb;
        var x1 = a.x + 18, x2 = b.x;
        hSk += '<path d="M' + x1 + ',' + y1 + ' C' + ((x1 + x2) / 2) + ',' + y1 + ' ' + ((x1 + x2) / 2) + ',' + y2 + ' ' + x2 + ',' + y2 +
          ' L' + x2 + ',' + (y2 + hb) + ' C' + ((x1 + x2) / 2) + ',' + (y2 + hb) + ' ' + ((x1 + x2) / 2) + ',' + (y1 + ha) + ' ' + x1 + ',' + (y1 + ha) +
          ' Z" fill="' + colors[ix % colors.length] + '" fill-opacity=".35"/>';
      });
      return wrap(hSk);
    }

    if (t === 'chrd') {
      var nCh = Math.max(3, labels.length);
      var labsCh = labels.slice(0, nCh);
      while (labsCh.length < nCh) labsCh.push(String(labsCh.length + 1));
      var mat = splitList(X.vals || '').map(function (v) { return num(v, 0); });
      if (mat.length < nCh * nCh) {
        mat = [];
        var ia, ib;
        for (ia = 0; ia < nCh; ia++) for (ib = 0; ib < nCh; ib++) mat.push(ia === ib ? 0 : 2 + ((ia * 3 + ib) % 5));
      }
      var cxC = 180, cyC = 150, rC = 92;
      var sums = [], iC, jC, totC = 0;
      for (iC = 0; iC < nCh; iC++) {
        var s = 0;
        for (jC = 0; jC < nCh; jC++) s += Math.max(0, mat[iC * nCh + jC] || 0);
        sums.push(s); totC += s;
      }
      if (!totC) totC = 1;
      var ang0 = -Math.PI / 2, arcs = [];
      for (iC = 0; iC < nCh; iC++) {
        var span = (sums[iC] / totC) * Math.PI * 2 * 0.92;
        arcs.push({ a0: ang0, a1: ang0 + span, c: colors[iC % colors.length], n: labsCh[iC] });
        ang0 += span + 0.06;
      }
      var hCh = head;
      for (iC = 0; iC < nCh; iC++) {
        hCh += ringSlice(cxC, cyC, rC - 2, rC + 10, arcs[iC].a0, arcs[iC].a1, arcs[iC].c);
        var am = (arcs[iC].a0 + arcs[iC].a1) / 2;
        hCh += '<text class="fa" x="' + (cxC + (rC + 24) * Math.cos(am)).toFixed(1) + '" y="' + (cyC + (rC + 24) * Math.sin(am) + 4).toFixed(1) + '" text-anchor="middle">' + esc(arcs[iC].n) + '</text>';
      }
      for (iC = 0; iC < nCh; iC++) {
        for (jC = iC + 1; jC < nCh; jC++) {
          var vv = Math.max(0, mat[iC * nCh + jC] || 0) + Math.max(0, mat[jC * nCh + iC] || 0);
          if (!vv) continue;
          var a1 = (arcs[iC].a0 + arcs[iC].a1) / 2, a2 = (arcs[jC].a0 + arcs[jC].a1) / 2;
          var p1 = (cxC + (rC - 8) * Math.cos(a1)).toFixed(1) + ',' + (cyC + (rC - 8) * Math.sin(a1)).toFixed(1);
          var p2 = (cxC + (rC - 8) * Math.cos(a2)).toFixed(1) + ',' + (cyC + (rC - 8) * Math.sin(a2)).toFixed(1);
          hCh += '<path d="M' + p1 + ' Q' + cxC + ',' + cyC + ' ' + p2 + '" fill="none" stroke="' + arcs[iC].c + '" stroke-width="' + Math.max(1.2, Math.min(8, vv)) + '" stroke-opacity=".45"/>';
        }
      }
      return wrap(hCh);
    }

    if (t === 'netw') {
      var nodesN = labels.length ? labels : ['A', 'B', 'C', 'D', 'E'];
      var eds = parsePairs(X.vals || 'A-B,A-C,B-D,C-D,C-E');
      var nN = nodesN.length, cxN = 180, cyN = 148, rN = 88;
      var pos = {};
      var hN = head;
      nodesN.forEach(function (nm, ix) {
        var an = -Math.PI / 2 + ix * 2 * Math.PI / nN;
        pos[nm] = { x: cxN + rN * Math.cos(an), y: cyN + rN * Math.sin(an), c: colors[ix % colors.length] };
      });
      eds.forEach(function (e) {
        if (!pos[e.a] || !pos[e.b]) return;
        hN += '<line x1="' + pos[e.a].x.toFixed(1) + '" y1="' + pos[e.a].y.toFixed(1) + '" x2="' + pos[e.b].x.toFixed(1) + '" y2="' + pos[e.b].y.toFixed(1) + '" stroke="#94a3b8" stroke-width="1.8"/>';
      });
      nodesN.forEach(function (nm) {
        var p = pos[nm];
        hN += '<circle cx="' + p.x.toFixed(1) + '" cy="' + p.y.toFixed(1) + '" r="14" fill="' + p.c + '" stroke="#fff" stroke-width="2"/>';
        hN += '<text class="onf" x="' + p.x.toFixed(1) + '" y="' + (p.y + 4).toFixed(1) + '" text-anchor="middle" fill="#fff">' + esc(nm) + '</text>';
      });
      return wrap(hN);
    }

    if (t === 'bmap') {
      var regsB = [
        { name: 'شمال‌غرب', lx: 70, ly: 70 }, { name: 'شمال', lx: 150, ly: 64 }, { name: 'شمال‌شرق', lx: 230, ly: 78 },
        { name: 'غرب', lx: 68, ly: 140 }, { name: 'مرکز', lx: 160, ly: 138 }, { name: 'جنوب', lx: 168, ly: 200 }
      ];
      var labsB = labels.length && X.labs !== 'A,B,C,D' ? labels : regsB.map(function (r) { return r.name; });
      var valsB = values.slice();
      if (!X.vals || X.vals === '4,7,3,6') valsB = [10, 16, 8, 12, 20, 14];
      while (valsB.length < regsB.length) valsB.push(4);
      var maxB = Math.max.apply(null, valsB.concat([1]));
      var chB = chrome(title, labsB.map(function (n, i) { return { c: colors[i % colors.length], n: n }; }));
      var hB = chB.html + '<rect x="20" y="' + chB.T + '" width="320" height="' + (268 - chB.T) + '" rx="10" fill="#eef4fb" stroke="#d5dce6"/>';
      hB += '<path d="M40,90 C80,40 200,36 300,80 C330,130 300,210 200,230 C120,246 50,200 40,140 Z" fill="#dbe7f5" stroke="#c5d4e6"/>';
      regsB.forEach(function (r, ix) {
        var rr = 8 + 22 * (valsB[ix] / maxB);
        hB += '<circle cx="' + r.lx + '" cy="' + (r.ly + chB.T * 0.15) + '" r="' + rr + '" fill="' + colors[ix % colors.length] + '" fill-opacity=".45" stroke="' + colors[ix % colors.length] + '"/>';
        hB += '<text class="fa" x="' + r.lx + '" y="' + (r.ly + chB.T * 0.15 + 4) + '" text-anchor="middle">' + esc(labsB[ix] || r.name) + '</text>';
      });
      return wrap(hB);
    }

    if (t === 'hmap') {
      var rowsH = splitList(X.rows || 'A,B,C,D');
      var colsH = splitList(X.cols || '۱,۲,۳,۴,۵');
      var cellsH = splitList(X.vals || '1,3,5,2,4,6,2,8,3,1,4,7,2,5,9,3,1,6,4,2').map(function (v) { return num(v, 0); });
      var nrX = rowsH.length, ncX = colsH.length;
      var Lx = 58, Tx = 42, Rx = 340, Bx = 242;
      var cwX = (Rx - Lx) / ncX, rhX = (Bx - Tx) / nrX;
      var minX = Math.min.apply(null, cellsH.concat([0])), maxX = Math.max.apply(null, cellsH.concat([1]));
      var hhX = head;
      var irX, icX;
      for (icX = 0; icX < ncX; icX++) hhX += '<text class="fa" x="' + (Lx + cwX * (icX + 0.5)) + '" y="28" text-anchor="middle">' + esc(colsH[icX]) + '</text>';
      for (irX = 0; irX < nrX; irX++) {
        hhX += '<text class="fa" x="' + (Lx - 6) + '" y="' + (Tx + rhX * (irX + 0.55)) + '" text-anchor="end">' + esc(rowsH[irX]) + '</text>';
        for (icX = 0; icX < ncX; icX++) {
          var vX = cellsH[irX * ncX + icX]; if (vX == null) vX = 0;
          var fX = maxX === minX ? 0.5 : (vX - minX) / (maxX - minX);
          hhX += '<rect x="' + (Lx + icX * cwX) + '" y="' + (Tx + irX * rhX) + '" width="' + (cwX - 2) + '" height="' + (rhX - 2) + '" rx="3" fill="' + lerpC('#dcfce7', '#15803d', fX) + '"/>';
          hhX += '<text class="tk" x="' + (Lx + cwX * (icX + 0.5)) + '" y="' + (Tx + rhX * (irX + 0.58)) + '" text-anchor="middle" fill="' + (fX > 0.62 ? '#fff' : '#14532d') + '">' + vX + '</text>';
        }
      }
      return wrap(hhX);
    }

    if (t === 'calh') {
      var cellsC = splitList(X.vals || '0,1,2,3,1,0,4,2,5,1,0,2,3,4,1,2,0,5,3,1,2,4,0,1,3,2,5,1,0,2,4,3,1,0,2').map(function (v) { return num(v, 0); });
      while (cellsC.length < 35) cellsC.push(0);
      cellsC = cellsC.slice(0, 56);
      var weeks = Math.ceil(cellsC.length / 7);
      var maxC = Math.max.apply(null, cellsC.concat([1]));
      var days = ['ش', 'ی', 'د', 'س', 'چ', 'پ', 'ج'];
      var hC = '<text class="ttl" x="180" y="18" text-anchor="middle">' + esc(title || 'تقویم حرارتی') + '</text>';
      var sizeC = Math.min(22, 280 / weeks), gapC = 3, oxC = 50, oyC = 44;
      days.forEach(function (d, i) {
        hC += '<text class="tk" x="34" y="' + (oyC + i * (sizeC + gapC) + 14) + '">' + d + '</text>';
      });
      cellsC.forEach(function (v, i) {
        var week = Math.floor(i / 7), day = i % 7;
        var f = v / maxC;
        hC += '<rect x="' + (oxC + week * (sizeC + gapC)) + '" y="' + (oyC + day * (sizeC + gapC)) + '" width="' + sizeC + '" height="' + sizeC + '" rx="3" fill="' + lerpC('#f1f5f9', '#15803d', f) + '"/>';
      });
      return wrap(hC);
    }

    if (t === 'rose') {
      var nR = Math.max(labels.length, 3);
      var labsR = labels.slice();
      while (labsR.length < nR) labsR.push(String(labsR.length + 1));
      var valsR = values.slice();
      while (valsR.length < nR) valsR.push(2);
      var maxR = Math.max.apply(null, valsR.concat([1]));
      var cxR = 180, cyR = 150, rMax = 96, aStep = 2 * Math.PI / nR;
      var hR = head;
      [0.33, 0.66, 1].forEach(function (f) {
        hR += '<circle cx="' + cxR + '" cy="' + cyR + '" r="' + (rMax * f) + '" fill="none" stroke="#e2e8f0"/>';
      });
      for (var iR2 = 0; iR2 < nR; iR2++) {
        var a0r = -Math.PI / 2 + iR2 * aStep, a1r = a0r + aStep * 0.92;
        var rr = 16 + (rMax - 16) * (Math.max(0, valsR[iR2]) / maxR);
        hR += ringSlice(cxR, cyR, 10, rr, a0r, a1r, colors[iR2 % colors.length]);
        var amr = (a0r + a1r) / 2;
        hR += '<text class="fa" x="' + (cxR + (rMax + 18) * Math.cos(amr)).toFixed(1) + '" y="' + (cyR + (rMax + 18) * Math.sin(amr) + 4).toFixed(1) + '" text-anchor="middle">' + esc(labsR[iR2]) + '</text>';
      }
      return wrap(hR);
    }

    if (t === 'word') {
      var words = labels.length && X.labs !== 'A,B,C,D' ? labels : ['داده', 'نمودار', 'تحلیل', 'فروش', 'روند', 'داشبورد', 'گزارش', 'KPI', 'میانگین', 'سهم'];
      var wvals = values.slice();
      if (!X.vals || X.vals === '4,7,3,6') wvals = [9, 7, 6, 5, 5, 4, 3, 3, 2, 2];
      while (wvals.length < words.length) wvals.push(2);
      var maxWrd = Math.max.apply(null, wvals.concat([1]));
      var places = [
        [180, 120], [90, 88], [270, 92], [120, 170], [250, 168], [180, 70], [70, 150], [300, 150], [150, 210], [230, 48]
      ];
      var hWrd = head;
      words.forEach(function (w, ix) {
        var p = places[ix % places.length];
        var fs = 11 + 18 * (wvals[ix] / maxWrd);
        var jitter = ((ix * 17) % 11) - 5;
        hWrd += '<text x="' + (p[0] + jitter) + '" y="' + p[1] + '" text-anchor="middle" fill="' + colors[ix % colors.length] + '" font-size="' + fs.toFixed(1) + '" font-weight="800">' + esc(w) + '</text>';
      });
      return wrap(hWrd);
    }

    return wrap(head);
  }

  function iconOf(id) {
    var m = {
      line: '<path d="M4,22 L10,16 L16,18 L32,6" fill="none" stroke="currentColor" stroke-width="1.8"/><path d="M4,26 H32 M6,4 V26" fill="none" stroke="currentColor" stroke-width="1.3"/>',
      quad: '<path d="M4,22 Q18,2 32,22" fill="none" stroke="currentColor" stroke-width="1.8"/><path d="M4,26 H32 M6,4 V26" fill="none" stroke="currentColor" stroke-width="1.3"/>',
      sine: '<path d="M4,15 C10,4 14,26 20,15 C26,4 30,26 34,15" fill="none" stroke="currentColor" stroke-width="1.8"/>',
      plot: '<path d="M6,4 V26 H32" fill="none" stroke="currentColor" stroke-width="1.6"/><path d="M10,20 L16,12 L22,16 L30,8" fill="none" stroke="currentColor" stroke-width="1.7"/>',
      col: '<path d="M8,24 V12 M15,24 V7 M22,24 V15 M29,24 V10" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round"/>',
      bar: '<path d="M8,24 V12 M15,24 V7 M22,24 V15 M29,24 V10" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round"/>',
      hbar: '<path d="M6,8 H22 M6,15 H28 M6,22 H18" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round"/>',
      pie: '<circle cx="18" cy="15" r="10" fill="none" stroke="currentColor" stroke-width="1.6"/><path d="M18,15 L18,5 A10,10 0 0 1 27,20 Z" fill="currentColor" opacity=".35"/>',
      donut: '<circle cx="18" cy="15" r="10" fill="none" stroke="currentColor" stroke-width="3.4"/><circle cx="18" cy="15" r="4" fill="none" stroke="currentColor" stroke-width="1.4"/>',
      lchr: '<path d="M6,22 L12,14 L18,17 L28,7" fill="none" stroke="currentColor" stroke-width="1.8"/><circle cx="6" cy="22" r="1.6" fill="currentColor"/><circle cx="12" cy="14" r="1.6" fill="currentColor"/><circle cx="18" cy="17" r="1.6" fill="currentColor"/><circle cx="28" cy="7" r="1.6" fill="currentColor"/>',
      area: '<path d="M6,24 L6,16 L14,10 L22,14 L30,6 L30,24 Z" fill="currentColor" opacity=".3" stroke="currentColor" stroke-width="1.3"/>',
      scat: '<circle cx="8" cy="20" r="2.2" fill="currentColor"/><circle cx="14" cy="12" r="2.2" fill="currentColor"/><circle cx="22" cy="16" r="2.2" fill="currentColor"/><circle cx="28" cy="7" r="2.2" fill="currentColor"/>',
      radar: '<polygon points="18,4 30,12 26,26 10,26 6,12" fill="none" stroke="currentColor" stroke-width="1.5"/><polygon points="18,10 24,14 22,22 14,22 12,14" fill="currentColor" opacity=".3" stroke="currentColor"/>',
      gauge: '<path d="M6,22 A13,13 0 0 1 30,22" fill="none" stroke="currentColor" stroke-width="2"/><path d="M18,22 L24,10" stroke="currentColor" stroke-width="1.7"/>',
      cmp: '<path d="M9,24 V12 M13,24 V16 M21,24 V8 M25,24 V14" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round"/>',
      hist: '<path d="M7,24 V14 H13 V24 M13,24 V8 H19 V24 M19,24 V16 H25 V24 M25,24 V11 H31 V24" fill="currentColor" opacity=".75"/>',
      exp: '<path d="M6,24 C12,24 16,20 20,12 C24,6 28,4 32,4" fill="none" stroke="currentColor" stroke-width="1.8"/>',
      stack: '<path d="M8,24 V18 H14 V24 Z M8,18 V12 H14 V18 Z M8,12 V7 H14 V12 Z M18,24 V16 H24 V24 Z M18,16 V11 H24 V16 Z" fill="currentColor"/>',
      pict: '<circle cx="10" cy="8" r="3" fill="currentColor"/><path d="M6,13 h8 v7 h-2.5 v6 h-3 v-6 h-2.5 z" fill="currentColor"/><circle cx="24" cy="8" r="3" fill="currentColor" opacity=".45"/><path d="M20,13 h8 v7 h-2.5 v6 h-3 v-6 h-2.5 z" fill="currentColor" opacity=".45"/>',
      flow: '<rect x="4" y="11" width="8" height="8" rx="4" fill="none" stroke="currentColor"/><rect x="16" y="11" width="8" height="8" rx="1.5" fill="none" stroke="currentColor"/><path d="M28,15 l4,-5 4,5 -4,5 z" fill="none" stroke="currentColor"/>',
      gantt: '<path d="M6,8 H20 M10,15 H30 M8,22 H18" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round"/>',
      ctrl: '<path d="M4,8 H32 M4,15 H32 M4,22 H32" stroke="currentColor" stroke-dasharray="2 2"/><path d="M6,20 L12,12 L18,16 L28,9" fill="none" stroke="currentColor"/>',
      fall: '<path d="M7,8 H14 V16 H21 V10 H28 V22 H7 Z" fill="currentColor" opacity=".8"/>',
      pareto: '<path d="M7,24 V14 H13 V24 M13,24 V9 H19 V24 M19,24 V16 H25 V24" fill="currentColor" opacity=".7"/><path d="M10,12 L16,8 L22,14 L30,6" fill="none" stroke="currentColor" stroke-width="1.5"/>',
      heat: '<rect x="6" y="6" width="10" height="8" fill="currentColor" opacity=".3"/><rect x="18" y="6" width="10" height="8" fill="currentColor" opacity=".7"/><rect x="6" y="16" width="10" height="8" fill="currentColor" opacity=".9"/><rect x="18" y="16" width="10" height="8" fill="currentColor" opacity=".45"/>',
      time: '<path d="M4,15 H32" stroke="currentColor" stroke-width="1.6"/><circle cx="10" cy="15" r="2.4" fill="currentColor"/><circle cx="20" cy="15" r="2.4" fill="currentColor"/><circle cx="30" cy="15" r="2.4" fill="currentColor"/>',
      bub: '<circle cx="12" cy="18" r="5" fill="currentColor" opacity=".4" stroke="currentColor"/><circle cx="24" cy="10" r="8" fill="currentColor" opacity=".35" stroke="currentColor"/>',
      venn: '<circle cx="14" cy="15" r="8" fill="currentColor" opacity=".3" stroke="currentColor"/><circle cx="22" cy="15" r="8" fill="currentColor" opacity=".3" stroke="currentColor"/>',
      sun: '<circle cx="18" cy="15" r="4" fill="none" stroke="currentColor"/><circle cx="18" cy="15" r="9" fill="none" stroke="currentColor"/><path d="M18,6 L18,24 M9,15 H27 M11,8 L25,22 M25,8 L11,22" stroke="currentColor" stroke-width="1"/>',
      tree: '<rect x="5" y="6" width="14" height="18" fill="currentColor" opacity=".85"/><rect x="21" y="6" width="10" height="8" fill="currentColor" opacity=".55"/><rect x="21" y="16" width="10" height="8" fill="currentColor" opacity=".35"/>',
      funn: '<path d="M6,6 H30 L24,14 H12 Z M12,16 H24 L20,24 H16 Z" fill="currentColor"/>',
      box: '<path d="M18,5 V10 M12,10 H24 V20 H12 Z M18,20 V25 M12,5 H24 M12,25 H24 M12,15 H24" fill="none" stroke="currentColor"/>',
      ohlc: '<path d="M10,6 V24 M7,10 H10 M10,18 H13 M22,5 V24 M19,14 H22 M22,9 H25" stroke="currentColor" stroke-width="1.6"/>',
      combo: '<path d="M8,24 V14 M15,24 V10 M22,24 V16" stroke="currentColor" stroke-width="3"/><path d="M6,18 L14,8 L22,12 L30,6" fill="none" stroke="currentColor"/>',
      bull: '<path d="M6,14 H28" stroke="currentColor" stroke-width="6" opacity=".25"/><path d="M6,14 H18" stroke="currentColor" stroke-width="3"/><path d="M24,8 V20" stroke="currentColor" stroke-width="2"/>',
      pyra: '<path d="M16,8 H6 V12 H16 M16,13 H8 V17 H16 M16,18 H10 V22 H16" fill="currentColor"/><path d="M20,8 H30 V12 H20 M20,13 H28 V17 H20 M20,18 H26 V22 H20" fill="currentColor" opacity=".55"/>',
      mekko: '<path d="M6,24 V10 H16 V24 Z M16,24 V6 H24 V24 Z M24,24 V14 H32 V24 Z" fill="currentColor" opacity=".7"/>',
      sarea: '<path d="M4,24 L4,16 L14,10 L24,14 L32,8 L32,24 Z" fill="currentColor" opacity=".35"/><path d="M4,24 L4,20 L14,16 L24,18 L32,14 L32,24 Z" fill="currentColor" opacity=".55"/>',
      hcmp: '<path d="M6,8 H18 M6,12 H26 M6,18 H16 M6,22 H24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/>',
      map: '<path d="M10,8 L18,6 L26,10 L28,18 L22,24 L12,22 L8,14 Z" fill="currentColor" opacity=".35" stroke="currentColor"/><path d="M10,8 L18,14 L12,22 M18,6 L18,14 L28,18" fill="none" stroke="currentColor"/>',
      surf: '<path d="M6,20 L14,16 L22,18 L30,12 L30,22 L6,26 Z" fill="currentColor" opacity=".3"/><path d="M6,20 L14,10 L22,14 L30,8" fill="none" stroke="currentColor"/>',
      st100: '<path d="M8,6 H14 V26 H8 Z M18,6 H24 V14 H18 Z M18,14 H24 V26 H18 Z" fill="currentColor"/>',
      lolli: '<path d="M10,24 V12 M18,24 V8 M26,24 V15" stroke="currentColor" stroke-width="2"/><circle cx="10" cy="12" r="3" fill="currentColor"/><circle cx="18" cy="8" r="3" fill="currentColor"/><circle cx="26" cy="15" r="3" fill="currentColor"/>',
      dumb: '<path d="M8,10 H28 M8,20 H22" stroke="currentColor" stroke-width="2"/><circle cx="8" cy="10" r="2.6" fill="currentColor"/><circle cx="28" cy="10" r="2.6" fill="currentColor"/><circle cx="8" cy="20" r="2.6" fill="currentColor"/><circle cx="22" cy="20" r="2.6" fill="currentColor"/>',
      step: '<path d="M4,22 H12 V14 H20 V8 H32" fill="none" stroke="currentColor" stroke-width="1.8"/>',
      spark: '<path d="M4,20 L10,12 L16,16 L24,7 L32,11" fill="none" stroke="currentColor" stroke-width="1.8"/><circle cx="32" cy="11" r="2.2" fill="currentColor"/>',
      slope: '<path d="M8,6 V24 M28,6 V24" stroke="currentColor"/><path d="M8,18 L28,8 M8,12 L28,20" stroke="currentColor" stroke-width="1.6"/>',
      stream: '<path d="M4,16 C10,8 16,10 22,14 C28,18 32,12 34,16 C32,22 26,24 20,20 C14,16 8,24 4,16 Z" fill="currentColor" opacity=".7"/>',
      viol: '<path d="M18,5 C24,8 24,14 18,16 C12,18 12,24 18,26 C24,24 24,18 18,16 C12,14 12,8 18,5 Z" fill="currentColor" opacity=".7"/>',
      strip: '<circle cx="10" cy="8" r="2" fill="currentColor"/><circle cx="14" cy="16" r="2" fill="currentColor"/><circle cx="10" cy="22" r="2" fill="currentColor"/><circle cx="24" cy="10" r="2" fill="currentColor"/><circle cx="26" cy="18" r="2" fill="currentColor"/>',
      stem: '<path d="M8,6 V24 M8,10 H28 M8,16 H22 M8,22 H18" stroke="currentColor" stroke-width="1.6"/>',
      waff: '<rect x="6" y="6" width="6" height="6" fill="currentColor"/><rect x="14" y="6" width="6" height="6" fill="currentColor" opacity=".55"/><rect x="22" y="6" width="6" height="6" fill="currentColor"/><rect x="6" y="14" width="6" height="6" fill="currentColor" opacity=".55"/><rect x="14" y="14" width="6" height="6" fill="currentColor"/><rect x="22" y="14" width="6" height="6" fill="currentColor" opacity=".3"/>',
      smat: '<rect x="5" y="5" width="11" height="11" fill="none" stroke="currentColor"/><rect x="18" y="5" width="11" height="11" fill="none" stroke="currentColor"/><rect x="5" y="18" width="11" height="11" fill="none" stroke="currentColor"/><rect x="18" y="18" width="11" height="11" fill="none" stroke="currentColor"/>',
      dend: '<path d="M8,24 V16 H18 V24 M18,16 V10 H28 V24" fill="none" stroke="currentColor"/>',
      sank: '<path d="M4,8 H10 V20 H4 Z M26,6 H32 V14 H26 Z M26,16 H32 V24 H26 Z" fill="currentColor"/><path d="M10,10 C18,10 18,8 26,8" fill="none" stroke="currentColor"/><path d="M10,18 C18,18 18,20 26,20" fill="none" stroke="currentColor"/>',
      chrd: '<circle cx="18" cy="15" r="10" fill="none" stroke="currentColor"/><path d="M10,10 Q18,15 26,10 M12,22 Q18,15 24,20" fill="none" stroke="currentColor"/>',
      netw: '<circle cx="10" cy="10" r="3" fill="currentColor"/><circle cx="26" cy="10" r="3" fill="currentColor"/><circle cx="18" cy="22" r="3" fill="currentColor"/><path d="M10,10 L26,10 L18,22 Z" fill="none" stroke="currentColor"/>',
      bmap: '<path d="M8,20 C10,8 26,8 28,20 C26,26 10,26 8,20 Z" fill="currentColor" opacity=".3" stroke="currentColor"/><circle cx="14" cy="16" r="3" fill="currentColor"/><circle cx="22" cy="14" r="4.5" fill="currentColor" opacity=".7"/>',
      hmap: '<rect x="6" y="6" width="7" height="7" fill="currentColor" opacity=".3"/><rect x="15" y="6" width="7" height="7" fill="currentColor" opacity=".7"/><rect x="24" y="6" width="7" height="7" fill="currentColor"/><rect x="6" y="15" width="7" height="7" fill="currentColor" opacity=".8"/><rect x="15" y="15" width="7" height="7" fill="currentColor" opacity=".4"/><rect x="24" y="15" width="7" height="7" fill="currentColor" opacity=".55"/>',
      calh: '<rect x="6" y="8" width="4" height="4" fill="currentColor" opacity=".3"/><rect x="12" y="8" width="4" height="4" fill="currentColor"/><rect x="18" y="8" width="4" height="4" fill="currentColor" opacity=".6"/><rect x="24" y="8" width="4" height="4" fill="currentColor" opacity=".2"/><rect x="6" y="14" width="4" height="4" fill="currentColor" opacity=".8"/><rect x="12" y="14" width="4" height="4" fill="currentColor" opacity=".4"/><rect x="18" y="14" width="4" height="4" fill="currentColor"/><rect x="24" y="14" width="4" height="4" fill="currentColor" opacity=".5"/>',
      rose: '<path d="M18,15 L18,5 A10,10 0 0 1 26,12 Z M18,15 L26,18 A10,10 0 0 1 16,25 Z M18,15 L10,18 A10,10 0 0 1 12,7 Z" fill="currentColor" opacity=".75"/>',
      word: '<text x="4" y="14" font-size="9" fill="currentColor">Aa</text><text x="16" y="22" font-size="7" fill="currentColor">کلمه</text>',
      // V206 — محورها
      axnum: '<path d="M3,15 L33,15 M30,12 L33,15 L30,18 M8,12 L8,18 M14,12 L14,18 M20,12 L20,18 M26,12 L26,18" stroke="currentColor" stroke-width="1.6" fill="none"/>',
      axxy: '<path d="M3,15 L33,15 M30,12 L33,15 L30,18 M18,28 L18,3 M15,6 L18,3 L21,6" stroke="currentColor" stroke-width="1.6" fill="none"/>',
      axq1: '<path d="M6,26 L33,26 M30,23 L33,26 L30,29 M6,26 L6,3 M3,6 L6,3 L9,6" stroke="currentColor" stroke-width="1.6" fill="none"/>',
      axgrid: '<path d="M4,4 H32 V26 H4 Z M11,4 V26 M18,4 V26 M25,4 V26 M4,11 H32 M4,19 H32" stroke="currentColor" stroke-width="1.2" fill="none"/>',
      axpol: '<circle cx="18" cy="15" r="11" stroke="currentColor" stroke-width="1.4" fill="none"/><circle cx="18" cy="15" r="6" stroke="currentColor" stroke-width="1.2" fill="none"/><path d="M7,15 H29 M18,4 V26 M10,7 L26,23 M26,7 L10,23" stroke="currentColor" stroke-width="1" fill="none"/>',
      ax3d: '<path d="M18,27 L18,5 M15,8 L18,5 L21,8 M18,27 L33,27 M30,24 L33,27 L30,30 M18,27 L5,17 M9,17 L5,17 L6,21" stroke="currentColor" stroke-width="1.6" fill="none"/>'
    };
    // V247 — آیکن ۲۱ محور تازه: آیکن پایه + نشانهٔ کوچک
    var DOT = '<circle cx="23" cy="15" r="2.6" fill="currentColor"/>';
    var AXI = {
      axnumpts: m.axnum + DOT, axnumint: m.axnum + '<path d="M11,15 H23" stroke="currentColor" stroke-width="3.5"/>', axnumineq: m.axnum + '<path d="M14,15 H31" stroke="currentColor" stroke-width="3.5"/><circle cx="14" cy="15" r="2.6" fill="#fff" stroke="currentColor" stroke-width="1.4"/>',
      axnumfrac: m.axnum + '<path d="M11,19 V21 M17,19 V21 M23,19 V21" stroke="currentColor" stroke-width="1"/>', axnumdec: m.axnum + '<circle cx="18" cy="22" r="1.3" fill="currentColor"/>', axnumblank: m.axnum, axnumlog: m.axnum + '<path d="M22,12 V18 M24,12 V18 M25.5,12 V18" stroke="currentColor" stroke-width="1"/>',
      axnumtwo: '<path d="M3,10 L33,10 M30,7 L33,10 L30,13 M3,21 L33,21 M30,18 L33,21 L30,24 M10,7 V13 M20,7 V13 M10,18 V24 M20,18 V24" stroke="currentColor" stroke-width="1.5" fill="none"/>',
      axxypts: m.axxy + DOT + '<circle cx="12" cy="20" r="2.6" fill="currentColor"/>', axxyline: m.axxy + '<path d="M7,24 L29,6" stroke="currentColor" stroke-width="1.6"/>', axxyvec: m.axxy + '<path d="M18,15 L28,8 M25,8 L28,8 L28,11" stroke="currentColor" stroke-width="1.6" fill="none"/>',
      axxycirc: m.axxy + '<circle cx="22" cy="11" r="5" fill="none" stroke="currentColor" stroke-width="1.4"/>', axxynogrid: m.axxy, axxyquads: m.axxy + '<text x="25" y="10" font-size="7" fill="currentColor" style="font:700 7px sans-serif">I</text>', axxyblank: m.axxy,
      axdual: '<path d="M6,26 H30 M6,26 V3 M3,6 L6,3 L9,6 M30,26 V3 M27,6 L30,3 L33,6" stroke="currentColor" stroke-width="1.6" fill="none"/>',
      axlog: m.axgrid + '<path d="M4,22 H32 M4,24 H32" stroke="currentColor" stroke-width="0.8"/>', axloglog: m.axgrid + '<path d="M4,22 H32 M4,24 H32 M28,4 V26 M30,4 V26" stroke="currentColor" stroke-width="0.8"/>',
      axtime: m.axnum + '<circle cx="8" cy="8" r="3" fill="none" stroke="currentColor" stroke-width="1.2"/><path d="M8,6 V8 H10" stroke="currentColor" stroke-width="1" fill="none"/>',
      axpolpts: m.axpol + DOT, ax3dpt: m.ax3d + DOT,
      // V304 — ابزار ریاضی ابتدایی
      axtally: '<path d="M8,6 V24 M13,6 V24 M18,6 V24 M23,6 V24 M5,21 L27,8" stroke="currentColor" stroke-width="1.8" fill="none" stroke-linecap="round"/>',
      axbase10: '<rect x="3" y="5" width="14" height="20" fill="none" stroke="currentColor" stroke-width="1.4"/><path d="M3,10 H17 M3,15 H17 M3,20 H17 M8,5 V25 M12,5 V25" stroke="currentColor" stroke-width="0.8"/><rect x="21" y="5" width="4" height="20" fill="none" stroke="currentColor" stroke-width="1.4"/><rect x="28" y="21" width="4" height="4" fill="currentColor"/><rect x="28" y="15" width="4" height="4" fill="currentColor"/>',
      axplace: '<rect x="3" y="6" width="30" height="18" fill="none" stroke="currentColor" stroke-width="1.4"/><path d="M3,13 H33 M13,6 V24 M23,6 V24" stroke="currentColor" stroke-width="1.2"/><text x="8" y="21" font-size="7" fill="currentColor" style="font:700 7px sans-serif" text-anchor="middle">2</text><text x="18" y="21" font-size="7" fill="currentColor" style="font:700 7px sans-serif" text-anchor="middle">4</text><text x="28" y="21" font-size="7" fill="currentColor" style="font:700 7px sans-serif" text-anchor="middle">5</text>',
      axclock: '<circle cx="18" cy="15" r="11" fill="none" stroke="currentColor" stroke-width="1.6"/><path d="M18,15 V8 M18,15 L24,17" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/><circle cx="18" cy="15" r="1.4" fill="currentColor"/>',
      axruler: '<rect x="3" y="10" width="30" height="10" fill="none" stroke="currentColor" stroke-width="1.4"/><path d="M8,10 V15 M13,10 V13 M18,10 V15 M23,10 V13 M28,10 V15" stroke="currentColor" stroke-width="1.2"/>',
      axfrac: '<circle cx="18" cy="15" r="11" fill="none" stroke="currentColor" stroke-width="1.6"/><path d="M18,15 V4 A11,11 0 0 1 29,15 Z" fill="currentColor"/><path d="M18,15 H29 M18,15 V26 M18,15 L7,15" stroke="currentColor" stroke-width="1"/>',
      axmoney: '<rect x="2" y="8" width="20" height="12" rx="2" fill="none" stroke="currentColor" stroke-width="1.4"/><circle cx="12" cy="14" r="2.5" fill="none" stroke="currentColor" stroke-width="1"/><circle cx="27" cy="19" r="6" fill="none" stroke="currentColor" stroke-width="1.6"/><circle cx="27" cy="19" r="3" fill="none" stroke="currentColor" stroke-width="0.8"/>',
      axabacus: '<rect x="4" y="23" width="28" height="4" rx="1" fill="currentColor"/><path d="M10,6 V23 M18,6 V23 M26,6 V23" stroke="currentColor" stroke-width="1.6"/><circle cx="10" cy="20" r="2.5" fill="currentColor"/><circle cx="18" cy="20" r="2.5" fill="currentColor"/><circle cx="18" cy="14" r="2.5" fill="currentColor"/><circle cx="26" cy="20" r="2.5" fill="currentColor"/>',
      axjump: '<path d="M4,22 H32" stroke="currentColor" stroke-width="1.6"/><path d="M6,22 Q12,10 18,22 Q24,10 30,22" fill="none" stroke="currentColor" stroke-width="1.6"/>',
      axbalance: '<path d="M18,10 L14,25 H22 Z M6,10 H30 M8,10 L5,18 H11 Z M28,10 L25,18 H31 Z" fill="none" stroke="currentColor" stroke-width="1.4"/>',
      axthermo: '<rect x="15" y="4" width="6" height="16" rx="3" fill="none" stroke="currentColor" stroke-width="1.5"/><circle cx="18" cy="23" r="4.5" fill="currentColor"/>',
      axsaw: '<rect x="6" y="4" width="24" height="22" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M18,4 V26 M6,15 H30" stroke="currentColor" stroke-width="1.4"/>',
      axpattern: '<rect x="3" y="10" width="8" height="10" rx="2" fill="none" stroke="currentColor" stroke-width="1.4"/><rect x="14" y="10" width="8" height="10" rx="2" fill="none" stroke="currentColor" stroke-width="1.4"/><rect x="25" y="10" width="8" height="10" rx="2" fill="none" stroke="currentColor" stroke-width="1.4"/>',
      axtenframe: '<rect x="3" y="7" width="30" height="16" rx="2" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M3,15 H33 M9,7 V23 M15,7 V23 M21,7 V23 M27,7 V23" stroke="currentColor" stroke-width="1"/><circle cx="6" cy="11" r="2" fill="currentColor"/><circle cx="12" cy="11" r="2" fill="currentColor"/>',
      axsym: '<path d="M18,3 V27" stroke="currentColor" stroke-width="1.8" stroke-dasharray="2 2"/><rect x="8" y="8" width="8" height="14" fill="currentColor" opacity=".4"/><rect x="20" y="8" width="8" height="14" fill="none" stroke="currentColor" stroke-width="1.4"/>',
      axbeaker: '<path d="M9,5 H11 V25 H25 V5 H27" fill="none" stroke="currentColor" stroke-width="1.6"/><rect x="11" y="13" width="14" height="12" fill="currentColor" opacity=".35"/>',
      axgeoboard: '<rect x="5" y="5" width="26" height="20" rx="2" fill="none" stroke="currentColor" stroke-width="1.4"/><polygon points="10,9 26,9 22,21 10,21" fill="currentColor" opacity=".25" stroke="currentColor" stroke-width="1.4"/>',
      axdice: '<rect x="4" y="7" width="12" height="14" rx="2" fill="none" stroke="currentColor" stroke-width="1.5"/><rect x="20" y="7" width="12" height="14" rx="2" fill="none" stroke="currentColor" stroke-width="1.5"/><circle cx="10" cy="14" r="1.6" fill="currentColor"/><circle cx="24" cy="11" r="1.4" fill="currentColor"/><circle cx="28" cy="17" r="1.4" fill="currentColor"/>',
      axmularea: '<rect x="5" y="6" width="26" height="18" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M21,6 V24 M5,16 H31" stroke="currentColor" stroke-width="1.3"/>',
      axvenn: '<circle cx="14" cy="15" r="9" fill="none" stroke="currentColor" stroke-width="1.5"/><circle cx="22" cy="15" r="9" fill="none" stroke="currentColor" stroke-width="1.5"/>',
      axnumgrid: '<rect x="5" y="4" width="26" height="22" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M13,4 V26 M22,4 V26 M5,11 H31 M5,19 H31" stroke="currentColor" stroke-width="1"/><rect x="13" y="11" width="9" height="8" fill="currentColor" opacity=".35"/>',
      axfacttree: '<circle cx="18" cy="6" r="3" fill="currentColor"/><circle cx="10" cy="16" r="2.8" fill="none" stroke="currentColor" stroke-width="1.4"/><circle cx="26" cy="16" r="2.8" fill="currentColor"/><circle cx="6" cy="25" r="2.5" fill="currentColor"/><circle cx="14" cy="25" r="2.5" fill="currentColor"/><path d="M16,8 L11,13 M20,8 L25,13 M9,19 L7,22 M11,19 L13,22" stroke="currentColor" stroke-width="1.4"/>',
      axfnmachine: '<rect x="10" y="7" width="16" height="16" rx="3" fill="none" stroke="currentColor" stroke-width="1.6"/><path d="M3,15 H10 M26,15 H33" stroke="currentColor" stroke-width="1.6"/>',
      axfracwall: '<rect x="4" y="5" width="28" height="20" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M4,12 H32 M4,19 H32 M18,5 V12 M13,12 V19 M23,12 V19 M11,19 V25 M18,19 V25 M25,19 V25" stroke="currentColor" stroke-width="1.1"/>',
      axtangram: '<rect x="6" y="4" width="24" height="22" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M6,4 L30,26 M6,26 L18,15 M18,15 L30,4 M12,20 L18,26" stroke="currentColor" stroke-width="1.2"/>',
      axangle: '<path d="M5,24 H31 A13,13 0 0 0 5,24 Z" fill="none" stroke="currentColor" stroke-width="1.4"/><path d="M18,24 L27,11" stroke="currentColor" stroke-width="2"/>',
      axrod: '<rect x="4" y="5" width="28" height="4" rx="1" fill="currentColor"/><rect x="4" y="12" width="20" height="4" fill="currentColor" opacity=".7"/><rect x="4" y="19" width="12" height="4" fill="currentColor" opacity=".5"/>',
      axtower: '<path d="M18,3 L30,27 H6 Z" fill="none" stroke="currentColor" stroke-width="1.6"/><path d="M12,17 H24 M18,17 V27" stroke="currentColor" stroke-width="1.2"/>',
      axmulwheel: '<circle cx="18" cy="15" r="12" fill="none" stroke="currentColor" stroke-width="1.5"/><circle cx="18" cy="15" r="4" fill="currentColor"/><path d="M18,3 V9 M18,21 V27 M6,15 H12 M24,15 H30" stroke="currentColor" stroke-width="1.2"/>',
      axmultgrid: '<rect x="4" y="5" width="28" height="22" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M13,5 V27 M24,5 V27 M4,12 H32 M4,20 H32" stroke="currentColor" stroke-width="1"/>',
      axnet: '<path d="M6,6 H30 V24 H6 Z" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M6,15 H30 M18,6 V24" stroke="currentColor" stroke-width="1"/>',
      axcubes: '<path d="M18,4 L30,10 L18,16 L6,10 Z" fill="currentColor" opacity=".45"/><path d="M6,10 V22 L18,28 V16 Z" fill="currentColor" opacity=".7"/><path d="M30,10 V22 L18,28 V16 Z" fill="currentColor" opacity=".3"/>',
      axcalendar: '<rect x="4" y="7" width="28" height="20" rx="2" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M4,13 H32 M11,7 V10 M25,7 V10" stroke="currentColor" stroke-width="1.2"/><circle cx="11" cy="19" r="1.6" fill="currentColor"/><circle cx="18" cy="19" r="1.6" fill="currentColor"/><circle cx="25" cy="24" r="1.6" fill="currentColor"/>',
      axlever: '<path d="M4,22 H32" stroke="currentColor" stroke-width="2.5"/><path d="M18,22 L14,28 H22 Z" fill="currentColor"/><path d="M6,12 H30" stroke="currentColor" stroke-width="1.4"/>',
      axcycle: '<path d="M10,15 A8,8 0 1 1 15,24" fill="none" stroke="currentColor" stroke-width="1.6"/><path d="M15,24 L12,20 L17,21 Z" fill="currentColor"/>',
      axpaintgrid: '<rect x="4" y="4" width="28" height="24" rx="2" fill="none" stroke="currentColor" stroke-width="1.4"/><path d="M13,4 V28 M22,4 V28 M4,12 H32 M4,20 H32" stroke="currentColor" stroke-width="1"/><rect x="13" y="12" width="9" height="8" fill="currentColor" opacity=".55"/>',
      axletterforms: '<path d="M5,25 V11 q0,-3 3,-3 h5" fill="none" stroke="currentColor" stroke-width="1.8"/><path d="M20,8 h6 v6 q0,3 -3,3" fill="none" stroke="currentColor" stroke-width="1.8"/><circle cx="27" cy="25" r="1.6" fill="currentColor"/>',
      axsyllablechart: '<rect x="3" y="10" width="12" height="9" rx="2" fill="none" stroke="currentColor" stroke-width="1.4"/><rect x="18" y="10" width="12" height="9" rx="2" fill="none" stroke="currentColor" stroke-width="1.4"/><circle cx="9" cy="25" r="2" fill="currentColor"/><circle cx="24" cy="25" r="2" fill="currentColor"/>',
      axmagnet: '<path d="M8,6 v12 a8,8 0 0 0 16,0 V6" fill="none" stroke="currentColor" stroke-width="3"/><path d="M8,6 h5 M23,6 h5" stroke="currentColor" stroke-width="3"/>',
      axsenses: '<circle cx="18" cy="15" r="8" fill="none" stroke="currentColor" stroke-width="1.6"/><circle cx="15" cy="13" r="1.4" fill="currentColor"/><circle cx="21" cy="13" r="1.4" fill="currentColor"/><path d="M15,19 q3,2 6,0" fill="none" stroke="currentColor" stroke-width="1.3"/>',
      axwordtrain: '<rect x="3" y="10" width="12" height="10" rx="2" fill="none" stroke="currentColor" stroke-width="1.4"/><rect x="17" y="10" width="12" height="10" rx="2" fill="none" stroke="currentColor" stroke-width="1.4"/><circle cx="8" cy="23" r="2" fill="currentColor"/><circle cx="24" cy="23" r="2" fill="currentColor"/>',
      axtiles: '<rect x="3" y="4" width="16" height="16" rx="2" fill="none" stroke="currentColor" stroke-width="1.4"/><rect x="21" y="4" width="12" height="5" fill="currentColor" opacity=".6"/><rect x="21" y="12" width="5" height="5" fill="currentColor" opacity=".4"/><rect x="28" y="12" width="5" height="5" fill="currentColor" opacity=".4"/>',
      axkenken: '<rect x="4" y="4" width="28" height="24" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M4,12 H22 M4,20 H22 M12,4 V28 M22,4 V28 M22,12 H32" stroke="currentColor" stroke-width="1.6"/>',
      axdicefold: '<rect x="12" y="3" width="12" height="12" fill="none" stroke="currentColor" stroke-width="1.4"/><path d="M6,9 H18 M18,9 H30 M12,15 V27 M18,15 V27" stroke="currentColor" stroke-width="1.2"/><circle cx="18" cy="9" r="1.6" fill="currentColor"/>',
      axtransform: '<path d="M6,24 L14,10 L22,24 Z" fill="none" stroke="currentColor" stroke-width="1.4"/><path d="M20,20 L28,8 L32,20 Z" fill="currentColor" opacity=".4"/><path d="M16,22 L22,16 M19,16 H22 V19" stroke="currentColor" stroke-width="1.2" fill="none"/>',
      axcaliper: '<path d="M4,18 H32 M4,24 H32" stroke="currentColor" stroke-width="1.6"/><path d="M4,12 H12 M4,14 V24" stroke="currentColor" stroke-width="2"/><path d="M24,12 H32" stroke="currentColor" stroke-width="2"/>',
      axsigntable: '<rect x="4" y="6" width="28" height="18" fill="none" stroke="currentColor" stroke-width="1.4"/><path d="M4,13 H32 M14,6 V24 M23,6 V24" stroke="currentColor" stroke-width="1"/><text x="9" y="21" font-size="8" fill="currentColor" style="font:700 8px sans-serif" text-anchor="middle">+</text><text x="18" y="21" font-size="8" fill="currentColor" style="font:700 8px sans-serif" text-anchor="middle">−</text>',
      axtrigcircle: '<circle cx="18" cy="15" r="12" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M18,15 L27,7" stroke="currentColor" stroke-width="2"/><path d="M27,7 V15 H18" stroke="currentColor" stroke-width="1" stroke-dasharray="2 2"/>',
      axmapping: '<ellipse cx="10" cy="15" rx="7" ry="12" fill="none" stroke="currentColor" stroke-width="1.4"/><ellipse cx="26" cy="15" rx="7" ry="12" fill="none" stroke="currentColor" stroke-width="1.4"/><path d="M14,10 L22,14 M14,15 L22,15" stroke="currentColor" stroke-width="1.2"/>',
      axprobtree: '<circle cx="6" cy="15" r="2.5" fill="currentColor"/><circle cx="20" cy="7" r="2.5" fill="currentColor"/><circle cx="20" cy="23" r="2.5" fill="currentColor"/><path d="M8,14 L18,8 M8,16 L18,22 M22,7 H31 M22,23 H31" stroke="currentColor" stroke-width="1.3"/>',
      axconic: '<ellipse cx="18" cy="15" rx="14" ry="8" fill="none" stroke="currentColor" stroke-width="1.6"/><circle cx="12" cy="15" r="1.6" fill="currentColor"/><circle cx="24" cy="15" r="1.6" fill="currentColor"/>',
      axorbital: '<rect x="4" y="8" width="8" height="6" fill="none" stroke="currentColor" stroke-width="1.2"/><rect x="14" y="8" width="8" height="6" fill="none" stroke="currentColor" stroke-width="1.2"/><path d="M6,10 V12 M6,12 V10" stroke="currentColor" stroke-width="1"/><path d="M8,11 L12,11" stroke="currentColor" stroke-width="1"/><rect x="4" y="18" width="8" height="6" fill="none" stroke="currentColor" stroke-width="1.2"/><rect x="14" y="18" width="8" height="6" fill="none" stroke="currentColor" stroke-width="1.2"/>',
      axpunnett: '<rect x="6" y="8" width="10" height="10" fill="none" stroke="currentColor" stroke-width="1.4"/><rect x="19" y="8" width="10" height="10" fill="none" stroke="currentColor" stroke-width="1.4"/><rect x="6" y="21" width="10" height="6" fill="none" stroke="currentColor" stroke-width="1.4"/><rect x="19" y="21" width="10" height="6" fill="none" stroke="currentColor" stroke-width="1.4"/>',
      axecg: '<path d="M3,16 H10 L13,7 L16,24 L19,16 H25 L28,13 L31,16" fill="none" stroke="currentColor" stroke-width="1.6"/>',
      axprosody: '<path d="M4,20 H10 M14,20 H20 M24,20 H30" stroke="currentColor" stroke-width="2"/><circle cx="4" cy="10" r="1.6" fill="currentColor"/><circle cx="14" cy="10" r="1.6" fill="currentColor"/><circle cx="24" cy="10" r="1.6" fill="currentColor"/>',
      axtruthtable: '<rect x="4" y="6" width="28" height="18" fill="none" stroke="currentColor" stroke-width="1.4"/><path d="M4,12 H32 M4,18 H32 M16,6 V24" stroke="currentColor" stroke-width="1"/><text x="10" y="17" font-size="7" fill="currentColor" style="font:700 7px sans-serif" text-anchor="middle">د</text>',
      axledger: '<path d="M18,3 V27 M4,6 H32 M4,6 V24 H32 V6" stroke="currentColor" stroke-width="1.6" fill="none"/><path d="M8,11 H14 M22,11 H28 M8,16 H14 M22,16 H28" stroke="currentColor" stroke-width="1.1"/>',
      axstaff: '<path d="M4,7 H32 M4,11 H32 M4,15 H32 M4,19 H32 M4,23 H32" stroke="currentColor" stroke-width="1"/><circle cx="17" cy="19" r="3" fill="currentColor"/><path d="M20,19 V6" stroke="currentColor" stroke-width="1.3"/>',
      axkmap: '<rect x="4" y="6" width="28" height="18" fill="none" stroke="currentColor" stroke-width="1.4"/><path d="M4,12 H32 M4,18 H32 M16,6 V24 M25,6 V24" stroke="currentColor" stroke-width="1"/><text x="10" y="17" font-size="8" fill="currentColor" style="font:700 8px sans-serif" text-anchor="middle">1</text>',
      axgate: '<path d="M6,7 V23 H17 A8,8 0 0 0 17,7 Z" fill="none" stroke="currentColor" stroke-width="1.6"/><path d="M2,12 H6 M2,18 H6 M25,15 H32" stroke="currentColor" stroke-width="1.4"/>',
      axdfa: '<circle cx="9" cy="15" r="5" fill="none" stroke="currentColor" stroke-width="1.4"/><circle cx="25" cy="15" r="5" fill="none" stroke="currentColor" stroke-width="1.4"/><circle cx="25" cy="15" r="3.4" fill="none" stroke="currentColor" stroke-width="1"/><path d="M3,15 H4 M14,15 H20 M18,11 L22,13" stroke="currentColor" stroke-width="1.3"/>',
      axbst: '<circle cx="18" cy="7" r="4" fill="none" stroke="currentColor" stroke-width="1.4"/><circle cx="9" cy="17" r="4" fill="none" stroke="currentColor" stroke-width="1.4"/><circle cx="27" cy="17" r="4" fill="none" stroke="currentColor" stroke-width="1.4"/><path d="M16,10 L11,14 M20,10 L25,14" stroke="currentColor" stroke-width="1.3"/>',
      axbode: '<path d="M3,8 H32 M3,16 H32 M3,24 H32" stroke="currentColor" stroke-width=".8"/><path d="M4,10 C14,10 18,22 32,24" fill="none" stroke="currentColor" stroke-width="1.6"/>',
      axbeam: '<path d="M3,13 H33" stroke="currentColor" stroke-width="3"/><path d="M16,4 V11 M14,8 L16,11 L18,8" stroke="currentColor" stroke-width="1.6" fill="none"/><path d="M6,18 L9,22 H3 Z M30,18 L33,22 H27 Z" fill="currentColor"/>',
      axmohr: '<circle cx="18" cy="15" r="11" fill="none" stroke="currentColor" stroke-width="1.6"/><path d="M3,15 H33 M18,3 V27" stroke="currentColor" stroke-width="1"/><circle cx="26" cy="9" r="2" fill="currentColor"/><circle cx="10" cy="21" r="2" fill="currentColor"/>',
      axpv: '<path d="M6,24 V4 M6,24 H32" stroke="currentColor" stroke-width="1.4"/><path d="M8,10 Q16,4 26,12 Q16,26 8,20 Q4,15 8,10 Z" fill="none" stroke="currentColor" stroke-width="1.6"/>',
      axpert: '<circle cx="7" cy="15" r="4" fill="none" stroke="currentColor" stroke-width="1.4"/><circle cx="19" cy="9" r="4" fill="none" stroke="currentColor" stroke-width="1.4"/><circle cx="19" cy="22" r="4" fill="none" stroke="currentColor" stroke-width="1.4"/><circle cx="30" cy="15" r="4" fill="none" stroke="currentColor" stroke-width="1.4"/><path d="M10,13 L15,10 M10,17 L15,20 M23,10 L27,13 M23,20 L27,17" stroke="currentColor" stroke-width="1.2"/>',
      axbenzene: '<path d="M18,4 L28,10 V22 L18,28 L8,22 V10 Z" fill="none" stroke="currentColor" stroke-width="1.6"/><circle cx="18" cy="16" r="5" fill="none" stroke="currentColor" stroke-width="1.2"/>',
      axnormal: '<path d="M3,26 C9,26 10,6 18,6 C26,6 27,26 33,26" fill="none" stroke="currentColor" stroke-width="1.6"/><path d="M3,26 H33" stroke="currentColor" stroke-width="1"/><path d="M14,26 V14 H22 V26" fill="currentColor" opacity=".3"/>',
      axsupply: '<path d="M4,6 V26 H32" stroke="currentColor" stroke-width="1.4" fill="none"/><path d="M8,24 L28,8" stroke="currentColor" stroke-width="1.6"/><path d="M8,8 L28,24" stroke="currentColor" stroke-width="1.6"/><circle cx="18" cy="16" r="2" fill="currentColor"/>',
      axgame: '<rect x="4" y="6" width="28" height="20" fill="none" stroke="currentColor" stroke-width="1.4"/><path d="M18,6 V26 M4,16 H32" stroke="currentColor" stroke-width="1.2"/><text x="10" y="14" font-size="8" fill="currentColor" style="font:700 8px sans-serif" text-anchor="middle">3</text><text x="26" y="24" font-size="8" fill="currentColor" style="font:700 8px sans-serif" text-anchor="middle">1</text>',
      // V319
      axunitconv: '<path d="M4,9 H32 M4,15 H32 M4,21 H32" stroke="currentColor" stroke-width="1.4" fill="none"/><path d="M16,6 V12 M16,12 L13,10 M16,12 L19,10 M16,24 V18 M16,18 L13,20 M16,18 L19,20" stroke="currentColor" stroke-width="1.4" fill="none"/>',
      axperimeter: '<rect x="5" y="8" width="18" height="13" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M23,8 H31 V21 H13" stroke="currentColor" stroke-width="1.1" stroke-dasharray="3 2" fill="none"/><circle cx="27" cy="16" r="4" fill="none" stroke="currentColor" stroke-width="1.2"/>',
      axpictograph: '<path d="M5,25 H31 M8,25 V6" stroke="currentColor" stroke-width="1.4" fill="none"/><rect x="11" y="16" width="4" height="9" fill="currentColor"/><rect x="18" y="10" width="4" height="15" fill="currentColor"/><rect x="25" y="19" width="4" height="6" fill="currentColor"/>',
      axrounding: '<path d="M4,20 H32" stroke="currentColor" stroke-width="1.4" fill="none"/><path d="M10,16 V24 M20,16 V24 M28,16 V24" stroke="currentColor" stroke-width="1.3"/><circle cx="20" cy="20" r="3" fill="currentColor"/><path d="M20,12 Q24,6 28,12" stroke="currentColor" stroke-width="1.2" fill="none" stroke-dasharray="3 2"/>',
      axdivision: '<circle cx="10" cy="13" r="6" fill="none" stroke="currentColor" stroke-width="1.3"/><circle cx="24" cy="13" r="6" fill="none" stroke="currentColor" stroke-width="1.3"/><circle cx="17" cy="24" r="2" fill="currentColor"/><circle cx="23" cy="24" r="2" fill="currentColor"/>',
      axshapeid: '<path d="M9,24 L18,6 L27,24 Z" fill="none" stroke="currentColor" stroke-width="1.5"/><rect x="24" y="16" width="8" height="8" fill="none" stroke="currentColor" stroke-width="1.2"/>',
      axcircleparts: '<circle cx="18" cy="15" r="11" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M18,15 L18,4 M7,15 H29" stroke="currentColor" stroke-width="1.2"/><circle cx="18" cy="15" r="1.6" fill="currentColor"/>',
      axcylinder: '<ellipse cx="18" cy="8" rx="10" ry="4" fill="none" stroke="currentColor" stroke-width="1.4"/><path d="M8,8 V22 M28,8 V22" stroke="currentColor" stroke-width="1.4"/><path d="M8,22 A10,4 0 0 0 28,22" fill="none" stroke="currentColor" stroke-width="1.4"/>',
      axproportion: '<rect x="4" y="7" width="28" height="16" fill="none" stroke="currentColor" stroke-width="1.3"/><path d="M4,15 H32 M18,7 V23" stroke="currentColor" stroke-width="1.2"/><path d="M9,19 L14,11 M24,19 L27,11" stroke="currentColor" stroke-width="1.1" stroke-dasharray="3 2"/>',
      axlinear: '<path d="M5,24 H31 M8,6 V26" stroke="currentColor" stroke-width="1.4" fill="none"/><path d="M7,6 L29,22 M7,22 L29,6" stroke="currentColor" stroke-width="1.5"/><circle cx="18" cy="14" r="2.4" fill="currentColor"/>',
      axbarchart: '<path d="M5,25 H31 M7,26 V6" stroke="currentColor" stroke-width="1.4" fill="none"/><rect x="10" y="15" width="4" height="10" fill="currentColor"/><rect x="17" y="8" width="4" height="17" fill="currentColor"/><rect x="24" y="18" width="4" height="7" fill="currentColor"/><path d="M6,12 H31" stroke="currentColor" stroke-width="1" stroke-dasharray="3 2"/>',
      axparabola: '<path d="M4,24 H32 M18,28 V4" stroke="currentColor" stroke-width="1.3" fill="none"/><path d="M8,7 Q18,31 28,7" fill="none" stroke="currentColor" stroke-width="1.7"/><circle cx="18" cy="19" r="2" fill="currentColor"/>',
      axinthales: '<circle cx="18" cy="15" r="11" fill="none" stroke="currentColor" stroke-width="1.4"/><path d="M18,15 L11,6 M18,15 L26,8 M18,15 L22,25 M18,15 L22,25" stroke="currentColor" stroke-width="1.2"/><path d="M11,6 L22,25 M26,8 L22,25" stroke="currentColor" stroke-width="1" stroke-dasharray="2 2"/>',
      axpendulum: '<path d="M14,5 H22 M18,5 V18" stroke="currentColor" stroke-width="1.4" fill="none"/><circle cx="18" cy="21" r="4" fill="none" stroke="currentColor" stroke-width="1.4"/><path d="M9,7 Q18,14 27,7" fill="none" stroke="currentColor" stroke-width="1" stroke-dasharray="3 2"/>',
      axincline: '<path d="M5,24 H31 L5,8 Z" fill="none" stroke="currentColor" stroke-width="1.5"/><rect x="14" y="12" width="6" height="6" fill="currentColor" transform="rotate(-33 17 15)"/>',
      axcircuit: '<path d="M8,24 V10 H16 M22,10 H30 V24 H8" stroke="currentColor" stroke-width="1.4" fill="none"/><rect x="16" y="6" width="6" height="8" fill="none" stroke="currentColor" stroke-width="1.3"/><path d="M10,18 H14 M10,21 H14" stroke="currentColor" stroke-width="1.2"/>',
      axoptics: '<path d="M4,15 H32" stroke="currentColor" stroke-width="1.2"/><path d="M18,4 Q24,15 18,26 Q12,15 18,4 Z" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M6,7 L18,15 L30,23" fill="none" stroke="currentColor" stroke-width="1.1" stroke-dasharray="3 2"/>',
      axheatcurve: '<path d="M5,26 H31 M7,27 V4" stroke="currentColor" stroke-width="1.3" fill="none"/><path d="M8,24 L14,16 H20 L26,6 H30" fill="none" stroke="currentColor" stroke-width="1.6"/>',
      axdna: '<path d="M10,4 Q22,10 10,16 Q-2,22 10,28" fill="none" stroke="currentColor" stroke-width="1.5" transform="translate(8,0)"/><path d="M22,4 Q10,10 22,16 Q34,22 22,28" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M10,10 H22 M10,22 H22" stroke="currentColor" stroke-width="1.1"/>',
      axcell: '<ellipse cx="18" cy="15" rx="13" ry="10" fill="none" stroke="currentColor" stroke-width="1.5"/><circle cx="15" cy="14" r="4" fill="none" stroke="currentColor" stroke-width="1.3"/><ellipse cx="24" cy="19" rx="4" ry="2" fill="currentColor"/>',
      axtimeline: '<path d="M3,15 H33" stroke="currentColor" stroke-width="1.6"/><path d="M9,11 V19 M18,11 V19 M27,11 V19" stroke="currentColor" stroke-width="1.4"/><circle cx="9" cy="15" r="2.2" fill="currentColor"/><circle cx="18" cy="15" r="2.2" fill="currentColor"/><circle cx="27" cy="15" r="2.2" fill="currentColor"/>',
      axmapgeo: '<circle cx="18" cy="15" r="11" fill="none" stroke="currentColor" stroke-width="1.4"/><path d="M18,4 L21,15 L18,13 L15,15 Z" fill="currentColor"/><path d="M18,26 L15,15" stroke="currentColor" stroke-width="1.2"/>',
      axsentence: '<rect x="3" y="8" width="14" height="8" rx="2" fill="none" stroke="currentColor" stroke-width="1.3"/><rect x="20" y="8" width="13" height="8" rx="2" fill="none" stroke="currentColor" stroke-width="1.3"/><path d="M17,12 H20" stroke="currentColor" stroke-width="1.3"/><rect x="10" y="19" width="16" height="8" rx="2" fill="none" stroke="currentColor" stroke-width="1.3"/>',
      axvectors: '<path d="M6,26 L18,24" stroke="currentColor" stroke-width="1.6"/><path d="M18,24 L26,8" stroke="currentColor" stroke-width="1.6"/><path d="M6,26 L26,8" stroke="currentColor" stroke-width="1.2" stroke-dasharray="3 2"/><path d="M6,26 L11,22" stroke="currentColor" stroke-width="1.6"/>',
      axmatrix: '<path d="M8,6 H4 V24 H8 M28,6 H32 V24 H28" stroke="currentColor" stroke-width="1.5" fill="none"/><path d="M12,11 H24 M12,16 H24 M12,21 H24" stroke="currentColor" stroke-width="1.1"/><path d="M16,7 V25 M21,7 V25" stroke="currentColor" stroke-width="1.1"/>',
      axintegral: '<path d="M4,24 H32 M6,26 V5" stroke="currentColor" stroke-width="1.3" fill="none"/><path d="M8,20 Q20,20 30,6" fill="none" stroke="currentColor" stroke-width="1.7"/><path d="M8,20 V24 H30 V20" fill="currentColor" opacity="0.28"/><path d="M8,24 V20" stroke="currentColor" stroke-width="1.2"/>',
      axflowchart: '<rect x="12" y="4" width="12" height="6" rx="3" fill="none" stroke="currentColor" stroke-width="1.4"/><rect x="12" y="14" width="12" height="6" fill="none" stroke="currentColor" stroke-width="1.4"/><path d="M18,10 V14 M18,20 V24" stroke="currentColor" stroke-width="1.2"/><path d="M18,20 L24,24 L18,28 L12,24 Z" fill="none" stroke="currentColor" stroke-width="1.3"/>',
      axstack: '<path d="M12,6 H26 V26 H12 Z M12,12 H26 M12,19 H26" fill="none" stroke="currentColor" stroke-width="1.4"/><path d="M5,10 H9 M5,16 H9" stroke="currentColor" stroke-width="1.2"/><path d="M7,22 H11" stroke="currentColor" stroke-width="1.2"/>',
      axsort: '<rect x="4" y="16" width="6" height="9" fill="currentColor"/><rect x="12" y="10" width="6" height="15" fill="currentColor"/><rect x="20" y="13" width="6" height="12" fill="currentColor"/><rect x="28" y="7" width="5" height="18" fill="currentColor" opacity="0.45"/>',
      axregression: '<path d="M5,25 H31 M7,26 V5" stroke="currentColor" stroke-width="1.3" fill="none"/><path d="M8,22 L30,6" stroke="currentColor" stroke-width="1.6"/><circle cx="11" cy="19" r="2" fill="currentColor"/><circle cx="17" cy="15" r="2" fill="currentColor"/><circle cx="23" cy="13" r="2" fill="currentColor"/><circle cx="28" cy="8" r="2" fill="currentColor"/>',
    };
    return '<svg viewBox="0 0 36 30" aria-hidden="true">' + (m[id] || AXI[id] || m.plot) + '</svg>';
  }

  function fieldsFor(t) {
    var common = [['X.title', 'عنوان نمودار']];
    if (t === 'axnum') return [['X.title', 'عنوان'], ['X.xmin', 'کمترین'], ['X.xmax', 'بیشترین'], ['X.step', 'گام']];
    if (t === 'axxy') return [['X.title', 'عنوان'], ['X.xmin', 'x min'], ['X.xmax', 'x max'], ['X.ymin', 'y min'], ['X.ymax', 'y max'], ['X.step', 'گام']];
    if (t === 'axq1') return [['X.title', 'عنوان'], ['X.xmax', 'x max'], ['X.ymax', 'y max'], ['X.step', 'گام']];
    if (t === 'axgrid') return [['X.title', 'عنوان'], ['X.xmax', 'تعداد ستون'], ['X.ymax', 'تعداد ردیف']];
    if (t === 'axpol') return [['X.title', 'عنوان'], ['X.xmax', 'بیشترین شعاع']];
    if (t === 'ax3d') return [['X.title', 'عنوان'], ['X.xmax', 'بیشترین مقدار'], ['X.step', 'گام']];
    // V247 — فیلدهای ۲۱ محور V137.2 (عیناً FigurePickerDialog.kt)
    var AXF = {
      axnumpts: [['xmin', 'کمینه'], ['xmax', 'بیشینه'], ['step', 'گام'], ['pts', 'نقطه‌ها (با ویرگول)'], ['labs', 'نام نقطه‌ها']],
      axnumint: [['xmin', 'کمینه'], ['xmax', 'بیشینه'], ['step', 'گام'], ['lo', 'ابتدای بازه'], ['hi', 'انتهای بازه'], ['lc', 'ابتدا بسته؟ (۱/۰)'], ['hc', 'انتها بسته؟ (۱/۰)'], ['labs', 'برچسب']],
      axnumineq: [['xmin', 'کمینه'], ['xmax', 'بیشینه'], ['step', 'گام'], ['x0', 'نقطهٔ مرز'], ['dir', 'جهت (۱ راست، -۱ چپ)'], ['cl', 'بسته؟ (۱/۰)'], ['labs', 'برچسب']],
      axnumfrac: [['xmin', 'کمینه'], ['xmax', 'بیشینه'], ['step', 'گام'], ['den', 'مخرج']],
      axnumdec: [['xmin', 'کمینه'], ['xmax', 'بیشینه'], ['step', 'گام اعشاری']],
      axnumblank: [['xmin', 'کمینه'], ['xmax', 'بیشینه'], ['step', 'گام']],
      axnumlog: [['xmax', 'تعداد دهه']],
      axnumtwo: [['xmin', 'کمینه'], ['xmax', 'بیشینه'], ['step', 'گام'], ['m', 'ضریب'], ['b', 'عرض از مبدأ'], ['s1', 'نام محور بالا'], ['s2', 'نام محور پایین']],
      axxypts: [['xmin', 'x min'], ['xmax', 'x max'], ['ymin', 'y min'], ['ymax', 'y max'], ['step', 'گام'], ['pts', 'نقطه‌ها (x,y;x,y)'], ['labs', 'نام نقطه‌ها']],
      axxyline: [['xmin', 'x min'], ['xmax', 'x max'], ['ymin', 'y min'], ['ymax', 'y max'], ['step', 'گام'], ['m', 'شیب m'], ['b', 'عرض از مبدأ b'], ['labs', 'برچسب']],
      axxyvec: [['xmin', 'x min'], ['xmax', 'x max'], ['ymin', 'y min'], ['ymax', 'y max'], ['step', 'گام'], ['x1', 'x ابتدا'], ['y1', 'y ابتدا'], ['x2', 'x انتها'], ['y2', 'y انتها'], ['labs', 'برچسب']],
      axxycirc: [['xmin', 'x min'], ['xmax', 'x max'], ['ymin', 'y min'], ['ymax', 'y max'], ['step', 'گام'], ['cx', 'x مرکز'], ['cy', 'y مرکز'], ['r', 'شعاع']],
      axxynogrid: [['xmin', 'x min'], ['xmax', 'x max'], ['ymin', 'y min'], ['ymax', 'y max'], ['step', 'گام']],
      axxyquads: [['xmin', 'x min'], ['xmax', 'x max'], ['ymin', 'y min'], ['ymax', 'y max'], ['step', 'گام']],
      axxyblank: [['xmin', 'x min'], ['xmax', 'x max'], ['ymin', 'y min'], ['ymax', 'y max'], ['step', 'گام']],
      axdual: [['xmin', 'x min'], ['xmax', 'x max'], ['ymin', 'y چپ min'], ['ymax', 'y چپ max'], ['lo', 'y راست min'], ['hi', 'y راست max'], ['s1', 'نام محور چپ'], ['s2', 'نام محور راست']],
      axlog: [['xmax', 'x max'], ['ymax', 'تعداد دههٔ y']],
      axloglog: [['xmax', 'تعداد دههٔ x'], ['ymax', 'تعداد دههٔ y']],
      axtime: [['labs', 'زمان‌ها (با ویرگول)'], ['s1', 'نام محور']],
      axpolpts: [['xmax', 'تعداد حلقه'], ['pts', 'نقطه‌ها (r,θ;r,θ)'], ['labs', 'نام نقطه‌ها']],
      ax3dpt: [['xmax', 'تعداد تیک'], ['step', 'گام'], ['x1', 'x نقطه'], ['y1', 'y نقطه'], ['z1', 'z نقطه'], ['labs', 'نام نقطه']],
      // V304 / V335.8 — ابزار ریاضی ابتدایی (عیناً FigurePickerDialog.kt)
      axtally: [['n', 'تعداد (۰ تا ۷۵)'], ['label', 'نمایش تعداد زیر شکل']],
      axbase10: [['n', 'عدد (۰ تا ۹۹۹٬۹۹۹٬۹۹۹)'], ['label', 'نمایش عدد زیر شکل']],
      axplace: [['n', 'ردیف ۱ (عدد)'], ['n2', 'ردیف ۲ (خالی = ندارد)'], ['op2', 'علامت ردیف ۲ (+ یا −)'], ['n3', 'ردیف ۳ (خالی = ندارد)'], ['op3', 'علامت ردیف ۳ (+ یا −)'], ['rows', 'تعداد ردیف‌ها (۱ تا ۶)'], ['blank', 'خانه‌ها خالی؟ (۱=بله)']],
      axclock: [['h', 'ساعت (۰ تا ۱۲)'], ['m', 'دقیقه (۰ تا ۵۹)'], ['nums', 'نمایش عددها (۱/۰)'], ['label', 'نوشتن ساعت زیر شکل (۱/۰)']],
      axruler: [['len', 'طول خط‌کش (۲ تا ۵۰ سانتی‌متر)'], ['a', 'ابتدای اندازه‌گیری (اختیاری)'], ['b', 'انتهای اندازه‌گیری (اختیاری)'], ['label', 'نمایش طول بازه']],
      axfrac: [['num', 'صورت'], ['den', 'مخرج (۱ تا ۲۵)'], ['shape', 'شکل'], ['label', 'نوشتن کسر زیر شکل (۱/۰)']],
      axmoney: [['notes', 'اسکناس‌ها (با ویرگول)'], ['coins', 'سکه‌ها (با ویرگول)'], ['unit', 'واحد'], ['sum', 'نوشتن جمع (۱/۰)']],
      axabacus: [['n', 'عدد (۰ تا ۹۹۹٬۹۹۹)'], ['cols', 'تعداد ستون‌ها (۲ تا ۶)'], ['blank', 'مخفی کردن ارقام زیر چرتکه (۱=بله)']],
      axjump: [['op', 'نوع عملیات روی محور'], ['start', 'نقطهٔ شروع'], ['step', 'طول هر پرش'], ['jumps', 'تعداد پرش'], ['max', 'انتهای محور'], ['label', 'نمایش عبارت ریاضی']],
      axbalance: [['left', 'وزنهٔ کفهٔ چپ'], ['right', 'وزنهٔ کفهٔ راست'], ['unit', 'واحد جرم'], ['label', 'نمایش علامت مقایسه']],
      axthermo: [['val', 'دما (°C)'], ['min', 'کمینهٔ دما'], ['max', 'بیشینهٔ دما']],
      axsaw: [['size', 'اندازهٔ جدول (۳×۳ تا ۹×۹)'], ['mode', 'نمایش (رنگ / شکل / عدد)']],
      axpattern: [['mode', 'نوع نمایش الگو'], ['seq', 'الگوی آماده']],
      axtenframe: [['a', 'مهره‌های قرمز'], ['b', 'مهره‌های آبی'], ['frames', 'تعداد قاب (۱ یا ۲)'], ['label', 'نمایش عبارت جمع']],
      axsym: [['axis', 'خط تقارن'], ['mirror', 'نمایش قرینه']],
      axbeaker: [['count', 'تعداد استوانه‌های مدرج'], ['val', num((state && state.X && state.X.count) || 1, 1) >= 2 ? 'حجم استوانهٔ ۱' : 'حجم مایع']]
        .concat(num((state && state.X && state.X.count) || 1, 1) >= 2 ? [['val2', 'حجم استوانهٔ ۲']] : [])
        .concat(num((state && state.X && state.X.count) || 1, 1) >= 3 ? [['val3', 'حجم استوانهٔ ۳']] : [])
        .concat(num((state && state.X && state.X.count) || 1, 1) >= 4 ? [['val4', 'حجم استوانهٔ ۴']] : [])
        .concat([['max', 'ظرفیت کل هر استوانه'], ['unit', 'واحد (mL یا لیتر)']]),
      axgeoboard: [['pts', 'مختصات میخ‌ها (x,y با ;)'], ['grid', 'اندازهٔ شبکه (۳ تا ۷)']],
      axdice: [['dice', 'اعداد تاس‌ها (با کاما)'], ['sum', 'نمایش جمع (۱=جمع، ۲=؟، ۰=خیر)']],
      axmularea: [['a', 'عدد اول (افقی)'], ['b', 'عدد دوم (عمودی)'], ['blank', 'مخفی کردن حاصل‌ها (۰ یا ۱)']],
      axvenn: [['left', 'دایرهٔ چپ'], ['right', 'دایرهٔ راست'], ['mid', 'بخش مشترک (وسط)']],
      axnumgrid: [['start', 'عدد شروع'], ['cols', 'تعداد ستون'], ['rows', 'تعداد سطر'], ['rule', 'رنگ‌آمیزی مضرب/الگو'], ['blanks', 'خانه‌های جای خالی (با ویرگول)']],
      axfacttree: [['n', 'عدد مورد تجزیه (۴ تا ۹۹۹)'], ['blank', 'حالت جای خالی (؟)']],
      axfnmachine: [['in', 'ورودی‌ها با ویرگول (تا ۸ عدد)'], ['op1', 'عملگر مرحلهٔ اول'], ['op2', 'عملگر مرحلهٔ دوم (اختیاری)'], ['blank', 'حالت جای خالی (؟)']],
      axfracwall: [['dens', 'مخرج ردیف‌ها (با ویرگول)'], ['fills', 'تعداد رنگ‌شدهٔ هر ردیف'], ['mode', 'نحوهٔ نمایش کسرها']],
      axtangram: [['shape', 'طرح تانگرام (۷ تکه)'], ['mode', 'حالت نمایش قطعات']],
      axangle: [['deg', 'زاویه (۰ تا ۱۸۰ درجه)'], ['pro', 'نمایش نقاله'], ['label', 'نمایش اندازهٔ زاویه']],
      axrod: [['nums', 'میله‌ها (۱ تا ۱۰ با ویرگول)'], ['blank', 'جای خالی (؟)']],
      axtower: [['base', 'آجرهای ردیف پایین'], ['op', 'عمل (+ یا −)'], ['blank', 'جای خالی خودکار']],
      axmulwheel: [['c', 'عدد مرکز'], ['n', 'تعداد پره (۳ تا ۱۲)'], ['op', 'عمل (× یا +)'], ['hide', 'جای خالی خودکار']],
      axmultgrid: [['rows', 'عددهای سطر'], ['cols', 'عددهای ستون'], ['blank', 'جای خالی خودکار']],
      axnet: [['shape', 'نوع حجم'], ['mode', 'حالت نمایش']],
      axcubes: [['view', 'نما'], ['hide', 'نمایش تعداد کل مکعب‌ها']],
      axcalendar: [['month', 'نام ماه'], ['start', 'روز شروع هفته'], ['days', 'تعداد روزها'], ['mark', 'روزهای نشان‌دار (با ویرگول)'], ['blank', 'جای خالی خودکار']],
      axlever: [['kind', 'نوع ماشین'], ['load', 'نیروی بار (N)'], ['effort', 'نیروی محرک (N)'], ['l1', 'بازوی بار'], ['l2', 'بازوی نیرو']],
      axcycle: [['kind', 'نوع چرخه']],
      axwordtrain: [['syll', 'هجاهای واگن‌ها (با ویرگول)'], ['word', 'کلمهٔ ساخته‌شده'], ['mode', 'حالت نمایش']],
      axpaintgrid: [['rows', 'تعداد سطر'], ['cols', 'تعداد ستون'], ['goal', 'تعداد خانه‌های هدف (۰ = آزاد)']],
      axletterforms: [['ch', 'حرف']],
      axsyllablechart: [['word', 'کلمه'], ['mode', 'نحوهٔ نمایش در سؤال']],
      axmagnet: [['mode', 'حالت قطب‌های آهنربا']],
      axsenses: [],
      axtiles: [['x2', 'تعداد کاشی x² (۰ تا ۹)'], ['x', 'تعداد کاشی x (۰ تا ۱۶)'], ['k', 'تعداد کاشی ۱ (۰ تا ۲۴)'], ['neg', 'علامت کاشی‌ها'], ['label', 'نمایش مدل و عبارت جبری']],
      axkenken: [['kind', 'نوع پازل'], ['size', 'ابعاد جدول (۳ تا ۶)'], ['cages', 'قفس‌ها (خانه‌ها|هدف|عمل با ;)']],
      axdicefold: [['net', 'الگوی گستردهٔ مکعب'], ['mode', 'نمایش وجه‌ها'], ['top', 'شمارهٔ وجه مرجع'], ['ask', 'جای خالی خودکار']],
      axtransform: [['shape', 'شکل'], ['op', 'نوع تبدیل'], ['dx', 'انتقال افقی'], ['dy', 'انتقال عمودی'], ['ang', 'زاویهٔ دوران'], ['axis', 'محور بازتاب'], ['grid', 'اندازهٔ شبکه']],
      axcaliper: [['kind', 'نوع ابزار'], ['val', 'مقدار اندازه‌گیری (mm)'], ['max', 'بیشینهٔ مقیاس'], ['label', 'نمایش خوانش']],
      axsigntable: [['roots', 'ریشه‌ها (با ویرگول)'], ['mult', 'تکرار ریشه‌ها (۱ فرد / ۲ زوج)'], ['lead', 'ضریب پیشرو (۱ یا -۱)']],
      axtrigcircle: [['deg', 'زاویه (درجه — مثبت یا منفی)'], ['show', 'نمایش مثلثاتی']],
      axmapping: [['from', 'اعضای دامنه A (با ویرگول)'], ['to', 'اعضای هم‌دامنه B (با ویرگول)'], ['arrows', 'پیکان‌ها (مبدأ-مقصد با ;)'], ['label', 'بررسی نوع رابطه و تابع']],
      axprobtree: [['stages', 'تعداد مرحله‌ها (۲ تا ۴)'], ['first', 'گزینه‌های مرحلهٔ ۱ (نام:احتمال)'], ['second', 'گزینه‌های مرحلهٔ ۲'], ['third', 'گزینه‌های مرحلهٔ ۳'], ['fourth', 'گزینه‌های مرحلهٔ ۴'], ['mode', 'نمایش انتهایی']],
      axconic: [['kind', 'نوع مقطع'], ['a', 'نیم‌محور a'], ['b', 'نیم‌محور b']],
      axorbital: [['z', 'عدد اتمی Z (۱ تا ۵۴ — تا زنون)'], ['mode', 'حالت نمایش'], ['sym', 'نماد اتم (لوئیس)'], ['val', 'الکترون ظرفیت (لوئیس)']],
      axpunnett: [['kind', 'نوع نمودار ژنتیک'], ['p1', 'ژنوتیپ والد اول (Aa یا AaBb)'], ['p2', 'ژنوتیپ والد دوم (Aa یا AaBb)'], ['gens', 'تعداد نسل‌های شجره‌نامه (۲ تا ۴)'], ['mode', 'حالت نمایش']],
      axecg: [['kind', 'نوع نوار'], ['n', 'تعداد ضربان'], ['label', 'برچسب موج‌ها (۱/۰)'], ['A', 'دامنه (نوسان)'], ['T', 'دوره (نوسان)']],
      axprosody: [['verse', 'متن مصراع (اختیاری)'], ['line', 'ارکان عروضی (با فاصله)'], ['mode', 'حالت نمایش']],
      axtruthtable: [['kind', 'نوع'], ['expr', 'عبارت (عنوان ستون)']],
      axledger: [['title', 'عنوان حساب'], ['debit', 'اقلام بدهکار (با ویرگول)'], ['credit', 'اقلام بستانکار (با ویرگول)']],
      axstaff: [['kind', 'نوع ابزار'], ['clef', 'کلید موسیقی'], ['time', 'کسر میزان'], ['notes', 'نت‌ها (MIDI با # یا b)'], ['dur', 'کشش نت‌ها (w,h,q,e اختیاری)'], ['harmony', 'هماهنگی چرخهٔ رنگ'], ['label', 'نمایش نام نت‌ها']],
      axkmap: [['vars', 'تعداد متغیر'], ['ones', 'خانه‌های ۱ (شماره)'], ['groups', 'گروه‌ها (خانه‌ها با ;)']],
      axgate: [['kind', 'نوع گیت'], ['a', 'ورودی A (۱/۰)'], ['b', 'ورودی B (۱/۰)']],
      axdfa: [['states', 'حالت‌ها'], ['start', 'حالت شروع'], ['accept', 'حالت پایانی'], ['edges', 'یال‌ها (مبدأ-مقصد:برچسب با ;)']],
      axbst: [['vals', 'مقدارها (با ویرگول)'], ['mode', 'نوع درخت']],
      axbode: [['kind', 'نوع نمودار'], ['gain', 'بهره (dB)'], ['poles', 'قطب‌ها (با ویرگول)']],
      axbeam: [['len', 'طول تیر (m)'], ['load', 'نوع بار'], ['p', 'بار متمرکز (N)'], ['a', 'فاصلهٔ بار (m)'], ['q', 'بار گسترده (N/m)']],
      axmohr: [['sx', 'تنش σx'], ['sy', 'تنش σy'], ['txy', 'تنش برشی τxy']],
      axpv: [['kind', 'نوع چرخه']],
      axpert: [['acts', 'فعالیت‌ها (نام:شروع-پایان:مدت با ;)']],
      axbenzene: [['kind', 'نوع ساختار'], ['mode', 'نمایش حلقه']],
      axnormal: [['lo', 'کران پایین'], ['hi', 'کران بالا'], ['mu', 'میانگین μ'], ['sd', 'انحراف معیار σ']],
      axsupply: [['kind', 'نوع نمودار'], ['shift', 'مقدار جابه‌جایی'], ['ceiling', 'سقف قیمت (۰ = ندارد)']],
      axgame: [['kind', 'نوع'], ['mat', 'ماتریس سود (سطرها با ;)']],
      axunitconv: [['kind', 'کمیت'], ['val', 'مقدار واحد اول']],
      axperimeter: [['shape', 'شکل هندسی'], ['w', 'طول / قاعده / شعاع / قطر اول'], ['h', 'عرض / ارتفاع / قطر دوم'], ['a', 'قاعدهٔ کوچک ذوزنقه'], ['n', 'تعداد ضلع چندضلعی'], ['show', 'نمایش زیر شکل']],
      axpictograph: [['mode', 'نوع نمودار'], ['data', 'داده‌ها (با ویرگول)'], ['labs', 'نام دسته‌ها']],
      axrounding: [['n', 'عدد مورد نظر'], ['mode', 'مرتبهٔ گرد کردن'], ['label', 'نمایش پاسخ زیر محور']],
      axdivision: [['a', 'عدد کل'], ['b', 'تعداد هر گروه']],
      axshapeid: [['kind', 'شکل'], ['n', 'تعداد ضلع (چندضلعی)']],
      axcircleparts: [['show', 'بخش نمایش‌داده‌شده']],
      axcylinder: [['kind', 'نوع جسم'], ['r', 'شعاع قاعده'], ['h', 'ارتفاع']],
      axproportion: [['a', 'مخرج معلوم'], ['b', 'صورت معلوم'], ['c', 'مقدار متناظر']],
      axlinear: [['m1', 'شیب خط اول (m₁)'], ['b1', 'عرض از مبدأ اول (b₁)'], ['m2', 'شیب خط دوم (m₂)'], ['b2', 'عرض از مبدأ دوم (b₂)'], ['show', 'نمایش معادلات و جواب']],
      axbarchart: [['data', 'داده‌ها (با ویرگول)'], ['labs', 'برچسب ستون‌ها'], ['show', 'نمایش شاخص آماری']],
      axparabola: [['a', 'ضریب a'], ['b', 'ضریب b'], ['c', 'جملهٔ ثابت c']],
      axinthales: [['ang', 'زاویهٔ مرکزی (درجه)']],
      axpendulum: [['len', 'طول نخ L (متر)'], ['amp', 'زاویهٔ انحراف θ (درجه)'], ['g', 'شتاب گرانش g'], ['mode', 'حالت نمایش']],
      axincline: [['ang', 'زاویهٔ شیب θ (درجه)'], ['mass', 'جرم جسم m (kg)'], ['mu', 'ضریب اصطکاک جنبشی μ'], ['show', 'نمایش نیروها']],
      axcircuit: [['mode', 'نوع بستن مدار'], ['r1', 'مقاومت R₁ (Ω)'], ['r2', 'مقاومت R₂ (Ω)'], ['r3', 'مقاومت R₃ (۰ = ندارد)'], ['v', 'ولتاژ باتری (V)'], ['show', 'نمایش محاسبات']],
      axoptics: [['kind', 'نوع عدسی'], ['f', 'فاصلهٔ کانونی f (cm)'], ['d', 'فاصلهٔ جسم p (cm)'], ['show', 'نمایش پرتوها و تصویر']],
      axheatcurve: [['t0', 'دمای اولیهٔ یخ (°C)'], ['t1', 'دمای نهایی بخار (°C)'], ['m', 'جرم نمونه (g)'], ['mode', 'حالت نمایش']],
      axdna: [['mode', 'حالت نمایش'], ['seq', 'توالی بازها (A/T/G/C)'], ['label', 'نمایش توضیحات و پیوندها']],
      axcell: [['kind', 'نوع سلول']],
      axtimeline: [['events', 'رویدادها (سال:عنوان)']],
      axmapgeo: [['kind', 'نوع نمایش']],
      axsentence: [['subj', 'نهاد'], ['verb', 'گزاره'], ['obj', 'مفعول (اختیاری)'], ['mode', 'نوع جمله']],
      axvectors: [['ax1', 'مؤلفهٔ x بردار a'], ['ay1', 'مؤلفهٔ y بردار a'], ['bx', 'مؤلفهٔ x بردار b'], ['by', 'مؤلفهٔ y بردار b'], ['mode', 'عمل']],
      axmatrix: [['a', 'ماتریس A (سطرها با ;)'], ['b', 'ماتریس B (سطرها با ;)']],
      axintegral: [['f', 'تابع'], ['lo', 'کران پایین a'], ['hi', 'کران بالا b']],
      axflowchart: [['steps', 'مراحل (با ویرگول)']],
      axstack: [['mode', 'ساختار'], ['items', 'عناصر'], ['op', 'عملیات']],
      axsort: [['vals', 'اعداد آرایه'], ['step', 'شمارهٔ دور']],
      axregression: [['pts', 'نقطه‌ها (x,y;x,y)']]
    };
    if (AXF[t]) return [['X.title', 'عنوان']].concat(AXF[t].map(function (f) { return ['X.' + f[0], f[1]]; }));
    if (isAxisType(t)) return [['X.title', 'عنوان']];
    if (t === 'line') return common.concat([['X.m', 'شیب m'], ['X.b', 'عرض از مبدأ b'], ['X.xmin', 'x min'], ['X.xmax', 'x max'], ['X.ymin', 'y min'], ['X.ymax', 'y max']]);
    if (t === 'quad') return common.concat([['X.a', 'a'], ['X.b', 'b'], ['X.c', 'c'], ['X.xmin', 'x min'], ['X.xmax', 'x max'], ['X.ymin', 'y min'], ['X.ymax', 'y max']]);
    if (t === 'sine') return common.concat([['X.A', 'دامنه A'], ['X.w', 'ω'], ['X.ph', 'فاز'], ['X.xmin', 'x min'], ['X.xmax', 'x max'], ['X.ymin', 'y min'], ['X.ymax', 'y max']]);
    if (t === 'plot') return common.concat([['X.xmin', 'x min'], ['X.xmax', 'x max'], ['X.ymin', 'y min'], ['X.ymax', 'y max']]);
    if (t === 'exp') return common.concat([['X.a', 'ضریب a'], ['X.b', 'نرخ b'], ['X.xmin', 'x min'], ['X.xmax', 'x max'], ['X.ymin', 'y min'], ['X.ymax', 'y max']]);
    if (t === 'scat') return common.concat([['X.xs', 'مقدارهای x'], ['X.ys', 'مقدارهای y']]);
    if (t === 'bub') return common.concat([['X.xs', 'مقدارهای x'], ['X.ys', 'مقدارهای y'], ['X.zs', 'اندازه حباب‌ها']]);
    if (t === 'gauge') return common.concat([['X.val', 'مقدار عقربه'], ['X.vmin', 'حداقل'], ['X.vmax', 'حداکثر']]);
    if (t === 'cmp') return common.concat([['X.labs', 'برچسب‌ها'], ['X.vals', 'سری ۱'], ['X.vals2', 'سری ۲'], ['X.s1', 'نام سری ۱'], ['X.s2', 'نام سری ۲']]);
    if (t === 'stack') return common.concat([['X.labs', 'برچسب‌ها'], ['X.vals', 'سری ۱'], ['X.vals2', 'سری ۲'], ['X.vals3', 'سری ۳'], ['X.s1', 'نام سری ۱'], ['X.s2', 'نام سری ۲'], ['X.s3', 'نام سری ۳']]);
    if (t === 'gantt') return common.concat([['X.labs', 'فعالیت‌ها'], ['X.vals', 'شروع'], ['X.vals2', 'مدت']]);
    if (t === 'fall') return common.concat([['X.labs', 'برچسب‌ها'], ['X.vals', 'مقدارها (منفی مجاز)']]);
    if (t === 'ctrl') return common.concat([['X.labs', 'نمونه‌ها'], ['X.vals', 'مقدارها'], ['X.mean', 'میانگین (خالی=خودکار)'], ['X.ucl', 'UCL (خالی=خودکار)'], ['X.lcl', 'LCL (خالی=خودکار)']]);
    if (t === 'heat') return common.concat([['X.rows', 'ردیف‌ها'], ['X.cols', 'ستون‌ها'], ['X.vals', 'مقدارها سطری']]);
    if (t === 'pict') return common.concat([['X.labs', 'برچسب‌ها'], ['X.vals', 'مقدارها'], ['X.unit', 'هر نماد برابر است با']]);
    if (t === 'time') return common.concat([['X.labs', 'رویدادها'], ['X.vals', 'تاریخ / مقدار']]);
    if (t === 'venn') return common.concat([['X.n', 'تعداد مجموعه (۲ یا ۳)'], ['X.s1', 'مجموعه A'], ['X.s2', 'مجموعه B'], ['X.s3', 'مجموعه C'], ['X.ab', 'A∩B'], ['X.ac', 'A∩C'], ['X.bc', 'B∩C'], ['X.abc', 'A∩B∩C']]);
    if (t === 'flow') return common.concat([['X.labs', 'مراحل (با ویرگول)']]);
    if (t === 'pareto') return common.concat([['X.labs', 'برچسب‌ها'], ['X.vals', 'مقدارها']]);
    if (t === 'sun') return common.concat([['X.labs', 'حلقهٔ داخلی'], ['X.vals', 'مقدار داخلی'], ['X.labs2', 'حلقهٔ بیرونی'], ['X.vals2', 'مقدار بیرونی']]);
    if (t === 'tree') return common.concat([['X.labs', 'بخش‌ها'], ['X.vals', 'مقدارها']]);
    if (t === 'funn') return common.concat([['X.labs', 'مراحل قیف'], ['X.vals', 'مقدارها']]);
    if (t === 'box') return common.concat([['X.labs', 'گروه‌ها'], ['X.mins', 'حداقل'], ['X.q1s', 'چارک ۱'], ['X.meds', 'میانه'], ['X.q3s', 'چارک ۳'], ['X.maxs', 'حداکثر']]);
    if (t === 'ohlc') return common.concat([['X.labs', 'دوره‌ها'], ['X.opens', 'باز'], ['X.highs', 'بیشینه'], ['X.lows', 'کمینه'], ['X.closes', 'بسته']]);
    if (t === 'combo') return common.concat([['X.labs', 'برچسب‌ها'], ['X.vals', 'ستون‌ها'], ['X.vals2', 'خط'], ['X.s1', 'نام ستون'], ['X.s2', 'نام خط']]);
    if (t === 'bull') return common.concat([['X.labs', 'شاخص‌ها'], ['X.vals', 'مقدار واقعی'], ['X.vals2', 'هدف']]);
    if (t === 'pyra') return common.concat([['X.labs', 'گروه‌های سنی'], ['X.vals', 'سمت چپ'], ['X.vals2', 'سمت راست'], ['X.s1', 'نام چپ'], ['X.s2', 'نام راست']]);
    if (t === 'mekko') return common.concat([['X.labs', 'دسته‌ها (پهنای ستون)'], ['X.vals', 'سری ۱ + پهنا'], ['X.vals2', 'سری ۲'], ['X.vals3', 'سری ۳'], ['X.s1', 'نام سری ۱'], ['X.s2', 'نام سری ۲'], ['X.s3', 'نام سری ۳']]);
    if (t === 'sarea') return common.concat([['X.labs', 'برچسب‌ها'], ['X.vals', 'سری ۱'], ['X.vals2', 'سری ۲'], ['X.vals3', 'سری ۳']]);
    if (t === 'hcmp') return common.concat([['X.labs', 'برچسب‌ها'], ['X.vals', 'سری ۱'], ['X.vals2', 'سری ۲'], ['X.s1', 'نام سری ۱'], ['X.s2', 'نام سری ۲']]);
    if (t === 'map') return common.concat([['X.labs', 'نام مناطق'], ['X.vals', 'مقدار هر منطقه']]);
    if (t === 'surf') return common.concat([['X.nrows', 'تعداد ردیف'], ['X.ncols', 'تعداد ستون'], ['X.vals', 'ارتفاع‌ها (سطری)']]);
    if (t === 'st100' || t === 'stream') return common.concat([['X.labs', 'برچسب‌ها'], ['X.vals', 'سری ۱'], ['X.vals2', 'سری ۲'], ['X.vals3', 'سری ۳'], ['X.s1', 'نام سری ۱'], ['X.s2', 'نام سری ۲'], ['X.s3', 'نام سری ۳']]);
    if (t === 'lolli' || t === 'step' || t === 'spark' || t === 'rose' || t === 'waff') return common.concat([['X.labs', 'برچسب‌ها'], ['X.vals', 'مقدارها']]);
    if (t === 'dumb' || t === 'slope') return common.concat([['X.labs', 'برچسب‌ها'], ['X.vals', 'مقدار شروع / قبل'], ['X.vals2', 'مقدار پایان / بعد'], ['X.s1', 'نام سری ۱'], ['X.s2', 'نام سری ۲']]);
    if (t === 'viol' || t === 'strip') return common.concat([['X.labs', 'گروه‌ها'], ['X.vals', 'نماینده'], ['X.mins', 'حداقل'], ['X.q1s', 'چارک ۱'], ['X.meds', 'میانه'], ['X.q3s', 'چارک ۳'], ['X.maxs', 'حداکثر']]);
    if (t === 'stem') return common.concat([['X.vals', 'عددها (با ویرگول)']]);
    if (t === 'smat') return common.concat([['X.xs', 'متغیر X'], ['X.ys', 'متغیر Y'], ['X.zs', 'متغیر Z'], ['X.s1', 'نام X'], ['X.s2', 'نام Y'], ['X.s3', 'نام Z']]);
    if (t === 'dend') return common.concat([['X.labs', 'برگ‌ها / نام‌ها']]);
    if (t === 'sank') return common.concat([['X.vals', 'جریان‌ها (مثل A-C:8,B-D:5)']]);
    if (t === 'chrd') return common.concat([['X.labs', 'گره‌ها'], ['X.vals', 'ماتریس سطری']]);
    if (t === 'netw') return common.concat([['X.labs', 'گره‌ها'], ['X.vals', 'یال‌ها (مثل A-B,B-C)']]);
    if (t === 'bmap') return common.concat([['X.labs', 'نام مناطق'], ['X.vals', 'اندازه حباب']]);
    if (t === 'hmap') return common.concat([['X.rows', 'ردیف‌ها'], ['X.cols', 'ستون‌ها'], ['X.vals', 'مقدارها سطری']]);
    if (t === 'calh') return common.concat([['X.vals', 'مقدار روزها (از شنبه، سطری)']]);
    if (t === 'word') return common.concat([['X.labs', 'واژه‌ها'], ['X.vals', 'وزن / فراوانی']]);
    return common.concat([['X.labs', 'برچسب‌ها (با ویرگول)'], ['X.vals', 'مقدارها (با ویرگول)']]);
  }

  function def(t) {
    return {
      k: 'g', t: t || 'col',
      X: {
        title: '', m: '1', b: '0', a: '1', c: '0', A: '2', w: '1', ph: '0',
        xmin: '-5', xmax: '5', ymin: '-4', ymax: '4',
        labs: 'A,B,C,D', vals: '4,7,3,6', vals2: '5,4,6,2', vals3: '1,2,2,1',
        xs: '1,2,3,4,5', ys: '2,3,1,5,4', zs: '8,14,6,18,10',
        val: '65', vmin: '0', vmax: '100',
        s1: 'سری ۱', s2: 'سری ۲', s3: 'سری ۳',
        n: '3', ab: '۳', ac: '۲', bc: '۲', abc: '۱',
        rows: 'A,B,C', cols: '۱,۲,۳,۴', unit: '1',
        mean: '', ucl: '', lcl: '', nrows: '4', ncols: '4'
      }
    };
  }
  // V206 — پیش‌فرض محورها (هم‌ارز JSON قالب‌های FigureGallery.kt).
  function defAxis(t) {
    var d = { k: 'g', t: t, X: { title: '' } }, i;
    for (i = 0; i < AXIS_TYPES.length; i++) if (AXIS_TYPES[i].id === t) { Object.keys(AXIS_TYPES[i].X).forEach(function (k) { d.X[k] = AXIS_TYPES[i].X[k]; }); break; }
    return d;
  }

  var state = def('col');
  var replaceEl = null;
  function $(id) { return document.getElementById(id); }

  // V310.8 / V310.9 — منوی کشویی شبیه دسکتاپ سایت (دکمهٔ گرد + فهرست گرد). این صفحه در iframe است و site.css ندارد، پس استایل آن این‌جا تزریق می‌شود.
  // نکتهٔ حیاتی: .gf-overlay دارای z-index:120100 است؛ بنابراین .gf-dd-menu باید z-index بالاتر (200000) داشته باشد تا پشت پنجره پنهان نشود.
  function gfDdCss() {
    if (document.getElementById('gfDdCss')) return;
    var st = document.createElement('style');
    st.id = 'gfDdCss';
    st.textContent = '.gf-fields .gf-field{display:grid;gap:4px;font-size:11px;font-weight:700;color:#4a5870}' +
      '.gf-dd-wrap{display:block;position:relative;width:100%}' +
      '.gf-dd-native,.gf-shape.gf-dd-native{display:none!important;position:absolute!important;opacity:0!important;pointer-events:none!important;width:0!important;height:0!important;min-height:0!important;left:0;top:0;overflow:hidden}' +
      '.gf-dd-btn{font:inherit;color:#1d2935;width:100%;min-width:120px;box-sizing:border-box;text-align:right;border-radius:999px;border:1px solid #d9e1e6;background-color:#fff;padding:9px 16px 9px 38px;min-height:40px;line-height:1.4;cursor:pointer;box-shadow:0 2px 8px rgba(60,120,170,.08);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;background-image:url("data:image/svg+xml;utf8,%3Csvg xmlns=%27http://www.w3.org/2000/svg%27 width=%2716%27 height=%2716%27 viewBox=%270 0 24 24%27 fill=%27none%27 stroke=%27%2339758a%27 stroke-width=%272.2%27 stroke-linecap=%27round%27 stroke-linejoin=%27round%27%3E%3Cpath d=%27M6 9l6 6 6-6%27/%3E%3C/svg%3E");background-repeat:no-repeat;background-position:left 14px center;background-size:16px}' +
      '.gf-dd-btn{background-repeat:no-repeat;background-position:left 14px center}' +
      '.gf-dd-btn:hover,.gf-dd-btn.open{border-color:#39758a}.gf-dd-btn.open{box-shadow:0 0 0 3px #e6f0f3}' +
      '.gf-dd-menu{position:fixed;z-index:200000;direction:rtl;display:flex;flex-direction:column;gap:4px;padding:8px;box-sizing:border-box;border-radius:18px;background:#fff;border:1px solid #d9e1e6;box-shadow:0 16px 40px rgba(60,120,170,.18);max-height:360px;overflow-y:auto}' +
      '.gf-dd-item{font:inherit;color:#1d2935;direction:rtl;text-align:right;border:1px solid transparent;border-radius:999px;padding:8px 14px;background:transparent;cursor:pointer;white-space:nowrap}' +
      '.gf-dd-item:hover{background:#eef1f6}.gf-dd-item.on{background:#e6f0f3;color:#39758a;border-color:#39758a;font-weight:700}';
    document.head.appendChild(st);
  }
  function gfDdClose() {
    Array.prototype.forEach.call(document.querySelectorAll('.gf-dd-menu'), function (m) { m.parentNode.removeChild(m); });
    Array.prototype.forEach.call(document.querySelectorAll('.gf-dd-btn.open'), function (b) { b.classList.remove('open'); });
  }
  function gfDdAttach(sel) {
    if (sel.dataset.gfdd) return;
    sel.dataset.gfdd = '1';
    gfDdCss();
    sel.classList.add('gf-dd-native'); /* V310.9 — منوی بومی پنهان؛ فقط دکمهٔ هم‌شکل منوی تعداد دفعات مجاز دیده شود */
    var wrapEl = document.createElement('span');
    wrapEl.className = 'gf-dd-wrap';
    sel.parentNode.insertBefore(wrapEl, sel);
    wrapEl.appendChild(sel);
    var btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'gf-dd-btn';
    wrapEl.appendChild(btn);
    function label() { var o = sel.options[sel.selectedIndex]; btn.textContent = o ? o.text : ''; }
    label();
    sel.addEventListener('change', label);
    btn.addEventListener('click', function (e) {
      e.preventDefault();
      e.stopPropagation();
      if (btn.classList.contains('open')) { gfDdClose(); return; }
      gfDdClose();
      var menu = document.createElement('div');
      menu.className = 'gf-dd-menu';
      menu.addEventListener('click', function (ev) { ev.stopPropagation(); });
      menu.addEventListener('wheel', function (ev) { ev.stopPropagation(); }, {passive: true});
      Array.prototype.forEach.call(sel.options, function (o, i) {
        var it = document.createElement('button');
        it.type = 'button';
        it.className = 'gf-dd-item' + (i === sel.selectedIndex ? ' on' : '');
        it.textContent = o.text;
        it.addEventListener('click', function (ev) {
          ev.preventDefault();
          ev.stopPropagation();
          sel.selectedIndex = i;
          sel.dispatchEvent(new Event('change', {bubbles: true}));
          label();
          gfDdClose();
        });
        menu.appendChild(it);
      });
      document.body.appendChild(menu);
      /* V310.9 — منو بالا باز می‌شود اگر پایین جا نیست؛ ارتفاع با جای واقعی تنظیم می‌شود (مثل منوی سایت) */
      var r = btn.getBoundingClientRect();
      var below = window.innerHeight - r.bottom - 12, above = r.top - 12;
      var maxH = Math.max(120, Math.min(360, Math.max(below, above)));
      menu.style.maxHeight = maxH + 'px';
      menu.style.minWidth = Math.max(r.width, 140) + 'px';
      menu.style.left = Math.max(8, Math.min(r.left, window.innerWidth - Math.max(r.width, 160) - 8)) + 'px';
      var mh = Math.min(menu.scrollHeight, maxH);
      if (below < mh && above > below) menu.style.top = Math.max(8, r.top - mh - 6) + 'px';
      else menu.style.top = (r.bottom + 6) + 'px';
      btn.classList.add('open');
    });
  }
  document.addEventListener('click', gfDdClose);
  document.addEventListener('keydown', function (e) { if (e.key === 'Escape') gfDdClose(); });
  window.addEventListener('resize', gfDdClose);
  window.addEventListener('scroll', function (e) {
    var m = document.querySelector('.gf-dd-menu');
    if (m && e.target && m.contains && m.contains(e.target)) return;
    gfDdClose();
  }, true);
  var clockDrag = null;
  function clockPoint(box, e) {
    var svg = box.querySelector('svg');
    if (!svg || !svg.getScreenCTM || !svg.createSVGPoint) return null;
    var pt = svg.createSVGPoint();
    pt.x = e.clientX;
    pt.y = e.clientY;
    return pt.matrixTransform(svg.getScreenCTM().inverse());
  }
  function clockSet(box, e) {
    var p = clockPoint(box, e);
    if (!p) return;
    var dx = p.x - 180, dy = p.y - 150;
    var deg = (Math.atan2(dx, -dy) * 180 / Math.PI + 360) % 360;
    if (clockDrag === 'hour') state.X.h = String(Math.round(deg / 30) % 12);
    else state.X.m = String(Math.round(deg / 6) % 60);
  }
  function gfCellPopClose() {
    var old = document.getElementById('gfCellPop');
    if (old && old.parentNode) old.parentNode.removeChild(old);
  }
  function openGfCellPopup(cfg) {
    gfCellPopClose();
    var bd = document.createElement('div');
    bd.id = 'gfCellPop';
    bd.className = 'gf-cell-pop-bd';
    var card = document.createElement('div');
    card.className = 'gf-cell-pop';
    card.setAttribute('dir', 'rtl');
    var h = '<div class="gf-cp-hd"><b>' + esc(cfg.title || 'تنظیم خانه') + '</b><button type="button" class="gf-cp-x" data-act="close">×</button></div>';
    if (cfg.sub) h += '<div class="gf-cp-sub">' + esc(cfg.sub) + '</div>';
    if (cfg.chips && cfg.chips.length) {
      h += '<div class="gf-cp-chips">' + cfg.chips.map(function (ch) {
        var on = String(cfg.current || '') === String(ch[0]) ? ' on' : '';
        var st = ch[2] ? ' style="' + ch[2] + '"' : '';
        return '<button type="button" class="gf-cp-chip' + on + '" data-v="' + esc(ch[0]) + '"' + st + '>' + esc(ch[1]) + '</button>';
      }).join('') + '</div>';
    }
    if (cfg.allowCustom) {
      h += '<div class="gf-cp-row"><input type="text" class="gf-cp-inp" placeholder="' + esc(cfg.placeholder || 'عدد یا متن دلخواه') + '" value="' + esc(cfg.inputVal == null ? '' : cfg.inputVal) + '">' +
        '<button type="button" class="gf-btn ok gf-cp-save">ثبت مقدار</button></div>';
    }
    h += '<div class="gf-cp-acts">';
    if (cfg.onEmpty) h += '<button type="button" class="gf-cp-act emp" data-act="empty">' + esc(cfg.emptyLabel || 'خانهٔ خالی (بدون علامت سؤال)') + '</button>';
    if (cfg.onQuestion) h += '<button type="button" class="gf-cp-act qst" data-act="question">علامت سؤال (؟)</button>';
    if (cfg.onReset) h += '<button type="button" class="gf-cp-act rst" data-act="reset">برگشت به مقدار پیش‌فرض</button>';
    h += '</div>';
    card.innerHTML = h;
    bd.appendChild(card);
    document.body.appendChild(bd);
    bd.addEventListener('click', function (ev) { if (ev.target === bd) gfCellPopClose(); });
    card.querySelectorAll('[data-act="close"]').forEach(function (b) { b.onclick = gfCellPopClose; });
    card.querySelectorAll('.gf-cp-chip').forEach(function (b) {
      b.onclick = function () { cfg.onPick(b.getAttribute('data-v')); gfCellPopClose(); };
    });
    var inp = card.querySelector('.gf-cp-inp');
    var saveBtn = card.querySelector('.gf-cp-save');
    if (inp && saveBtn) {
      var commit = function () {
        var v = inp.value.trim();
        if (v === '' && cfg.onEmpty) cfg.onEmpty();
        else if (cfg.onPick) cfg.onPick(v);
        gfCellPopClose();
      };
      saveBtn.onclick = commit;
      inp.addEventListener('keydown', function (ev) { if (ev.key === 'Enter') { ev.preventDefault(); commit(); } });
      setTimeout(function () { try { inp.focus(); inp.select(); } catch (_e) {} }, 20);
    }
    var empB = card.querySelector('[data-act="empty"]');
    if (empB && cfg.onEmpty) empB.onclick = function () { cfg.onEmpty(); gfCellPopClose(); };
    var qstB = card.querySelector('[data-act="question"]');
    if (qstB && cfg.onQuestion) qstB.onclick = function () { cfg.onQuestion(); gfCellPopClose(); };
    var rstB = card.querySelector('[data-act="reset"]');
    if (rstB && cfg.onReset) rstB.onclick = function () { cfg.onReset(); gfCellPopClose(); };
  }
  function gfParseMap(raw) {
    var m = {};
    String(raw == null ? '' : raw).split(/[;؛]/).forEach(function (tk) {
      var i2 = tk.indexOf(':');
      if (i2 > 0) { var k0 = faEn(tk.slice(0, i2).trim()), v0 = tk.slice(i2 + 1).trim(); if (k0) m[k0] = v0; }
    });
    return m;
  }
  function gfEncodeMap(m) {
    return Object.keys(m).filter(function (k0) { return m[k0] != null && String(m[k0]) !== ''; }).map(function (k0) { return k0 + ':' + m[k0]; }).join(';');
  }
  function gfParseEmpty(raw) {
    var m = {};
    String(raw == null ? '' : raw).split(/[,،;؛\s]+/).forEach(function (tk) { var k0 = faEn(tk.trim()); if (k0) m[k0] = 1; });
    return m;
  }
  function gfEncodeEmpty(m) {
    return Object.keys(m).filter(function (k0) { return !!m[k0]; }).join(',');
  }
  function gfQuickChipsFor(t, cellKey) {
    if (t === 'axorbital' && cellKey !== 'cfg') {
      return [['↑↓', '↑↓ (جفت الکترون)'], ['↑', '↑ (تک‌الکترون بالا)'], ['↓', '↓ (تک‌الکترون پایین)']];
    }
    if (t === 'axdicefold') {
      return [['1', '۱'], ['2', '۲'], ['3', '۳'], ['4', '۴'], ['5', '۵'], ['6', '۶']];
    }
    if (t === 'axpunnett') {
      if (String(state.X.kind || '').toLowerCase() === 'pedigree') {
        return [['M', '□ مرد سالم'], ['M*', '■ مرد مبتلا'], ['Mc', '◧ مرد ناقل'], ['F', '○ زن سالم'], ['F*', '● زن مبتلا'], ['Fc', '◐ زن ناقل']];
      }
      if (cellKey.indexOf('c_') === 0) return [['AA', 'AA'], ['Aa', 'Aa'], ['aa', 'aa'], ['AABB', 'AABB'], ['AaBb', 'AaBb'], ['aabb', 'aabb']];
      if (cellKey.indexOf('h') === 0) return [['A', 'A'], ['a', 'a'], ['AB', 'AB'], ['Ab', 'Ab'], ['aB', 'aB'], ['ab', 'ab']];
    }
    if (t === 'axkenken') {
      var sz = Math.max(3, Math.min(6, Math.floor(num(state.X.size, 4)))), arr = [];
      for (var d = 1; d <= sz; d++) arr.push([String(d), String(d)]);
      return arr;
    }
    return null;
  }
  function gfClearAutoBlankFlags() {
    if (state.t === 'axdicefold') state.X.ask = '0';
    else if (state.t === 'axmulwheel') state.X.hide = '0';
    else if (state.t === 'axpunnett') { if (state.X.mode === 'blank') state.X.mode = 'genotype'; }
    else if (state.X.blank != null) state.X.blank = '0';
  }
  function gfOpenKeyedCellEditor(cellKey, cellLabel, defaultVal) {
    var ovMap = gfParseMap(state.X.ov), empSet = gfParseEmpty(state.X.empty);
    var hasEmpKeys = Object.keys(empSet).length > 0;
    var isEmp = state.t === 'axkenken' ? (hasEmpKeys ? !!empSet[cellKey] : (ovMap[cellKey] == null)) : !!empSet[cellKey];
    var curVal = isEmp ? '' : (ovMap[cellKey] != null ? ovMap[cellKey] : String(defaultVal || ''));
    openGfCellPopup({
      title: 'تنظیم ' + cellLabel,
      sub: 'مقدار دلخواه بنویسید یا این خانه را برای پاسخ دانش‌آموز کاملاً خالی (بدون علامت سؤال) بگذارید.',
      chips: gfQuickChipsFor(state.t, cellKey),
      current: curVal,
      allowCustom: true,
      placeholder: defaultVal ? ('مقدار (مثلاً ' + defaultVal + ')') : 'مقدار خانه',
      inputVal: curVal,
      onPick: function (v) {
        var clean = faEn(String(v).replace(/[;؛]/g, ',').trim());
        delete empSet[cellKey];
        ovMap[cellKey] = clean;
        gfClearAutoBlankFlags();
        state.X.empty = gfEncodeEmpty(empSet);
        state.X.ov = gfEncodeMap(ovMap);
        paint(); renderFields();
      },
      onEmpty: function () {
        delete ovMap[cellKey];
        if (state.t !== 'axkenken' || hasEmpKeys) empSet[cellKey] = 1;
        gfClearAutoBlankFlags();
        state.X.empty = gfEncodeEmpty(empSet);
        state.X.ov = gfEncodeMap(ovMap);
        paint(); renderFields();
      },
      onReset: function () {
        delete ovMap[cellKey];
        delete empSet[cellKey];
        state.X.empty = gfEncodeEmpty(empSet);
        state.X.ov = gfEncodeMap(ovMap);
        paint(); renderFields();
      }
    });
  }
  // V310.7 / V314.0 / V315.1 / V315.2 / V335.8 — همهٔ ابزارهای ریاضی/علوم ابتدایی و محورها با ماوس و لمس در پیش‌نمایش قابل تنظیم‌اند
  function bindClock(box) {
    var INTERACTIVE_PRIMARY = {
      axclock: 1, axfrac: 1, axabacus: 1, axthermo: 1, axtenframe: 1, axbeaker: 1,
      axtally: 1, axbase10: 1, axplace: 1, axruler: 1, axmoney: 1, axjump: 1, axbalance: 1,
      axsaw: 1, axpattern: 1, axsym: 1, axgeoboard: 1, axdice: 1, axmularea: 1, axvenn: 1,
      axnumgrid: 1, axfacttree: 1, axfnmachine: 1, axfracwall: 1, axtangram: 1, axangle: 1,
      axrod: 1, axtower: 1, axmulwheel: 1, axmultgrid: 1, axnet: 1, axcubes: 1, axcalendar: 1, axlever: 1, axcycle: 1, axwordtrain: 1, axpaintgrid: 1, axmagnet: 1,
      axtiles: 1, axkenken: 1, axdicefold: 1, axtransform: 1, axcaliper: 1, axsigntable: 1, axtrigcircle: 1, axmapping: 1, axprobtree: 1, axconic: 1, axorbital: 1, axpunnett: 1, axecg: 1, axprosody: 1, axtruthtable: 1, axledger: 1, axstaff: 1, axkmap: 1, axgate: 1, axdfa: 1, axbst: 1, axbode: 1, axbeam: 1, axmohr: 1, axpv: 1, axpert: 1, axbenzene: 1, axnormal: 1, axsupply: 1, axgame: 1,
      // V319
      axunitconv: 1, axperimeter: 1, axpictograph: 1, axrounding: 1, axdivision: 1, axshapeid: 1, axcircleparts: 1, axcylinder: 1, axproportion: 1, axlinear: 1, axbarchart: 1, axparabola: 1, axinthales: 1, axpendulum: 1, axincline: 1, axcircuit: 1, axoptics: 1, axheatcurve: 1, axdna: 1, axcell: 1, axtimeline: 1, axmapgeo: 1, axsentence: 1, axvectors: 1, axmatrix: 1, axintegral: 1, axflowchart: 1, axstack: 1, axsort: 1, axregression: 1,
      axnumpts: 1, axnumint: 1, axnumineq: 1, axxypts: 1, axxyline: 1, axxyvec: 1, axxycirc: 1, axpolpts: 1,
      gauge: 1
    };
    if (!INTERACTIVE_PRIMARY[state.t]) {
      box.onpointerdown = box.onpointermove = box.onpointerup = box.onpointercancel = null;
      box.style.touchAction = '';
      box.style.cursor = '';
      return;
    }
    box.style.touchAction = 'none';
    box.style.cursor = 'pointer';
    if (state.t === 'axdicefold' || state.t === 'axunitconv' || state.t === 'axprobtree' || state.t === 'axorbital' || state.t === 'axpunnett' || state.t === 'axkenken') {
      box.onpointerdown = function (e) {
        var el = e.target && e.target.closest ? e.target.closest('[data-cell-key]') : null;
        if (!el) return;
        gfOpenKeyedCellEditor(
          el.getAttribute('data-cell-key') || '',
          el.getAttribute('data-cell-label') || 'این بخش',
          el.getAttribute('data-cell-def') || ''
        );
      };
      box.onpointermove = box.onpointerup = box.onpointercancel = null;
      return;
    }
    if (state.t === 'axtiles') {
      box.onpointerdown = function (e) {
        var p = clockPoint(box, e); if (!p) return;
        if (p.x < 128) state.X.x2 = String((Math.max(0, Math.min(9, Math.floor(num(state.X.x2, 1)))) + 1) % 10);
        else if (p.x < 248) state.X.x = String((Math.max(0, Math.min(16, Math.floor(num(state.X.x, 3)))) + 1) % 17);
        else state.X.k = String((Math.max(0, Math.min(24, Math.floor(num(state.X.k, 2)))) + 1) % 25);
        paint(); renderFields();
      };
      box.onpointermove = box.onpointerup = box.onpointercancel = null;
      return;
    }
    if (state.t === 'axtenframe') {
      box.onpointerdown = function (e) {
        var p = clockPoint(box, e); if (!p) return;
        var fr = Math.max(1, Math.min(2, Math.floor(num(state.X.frames, 2)))), cap = fr * 10;
        var a = Math.max(0, Math.min(cap, Math.floor(num(state.X.a, 7)))), b = Math.max(0, Math.min(cap - a, Math.floor(num(state.X.b, 5))));
        if (p.x <= 180) state.X.a = String((a + 1) % (cap - b + 1));
        else state.X.b = String((b + 1) % (cap - a + 1));
        paint(); renderFields();
      };
      box.onpointermove = box.onpointerup = box.onpointercancel = null;
      return;
    }
    if (state.t === 'axvenn') {
      box.onpointerdown = function (e) {
        var p = clockPoint(box, e); if (!p) return;
        var key = p.x < 135 ? 'left' : (p.x > 225 ? 'right' : 'mid');
        var fb = key === 'left' ? 'پرنده: کبوتر، گنجشک' : (key === 'right' ? 'آبزی: ماهی، میگو' : 'اردک');
        var cur = String(state.X[key] || fb);
        state.X[key] = (cur.trim() === '؟' || /:\s*؟$/.test(cur)) ? fb : (cur.indexOf(':') >= 0 ? cur.split(':')[0] + ': ؟' : '؟');
        paint(); renderFields();
      };
      box.onpointermove = box.onpointerup = box.onpointercancel = null;
      return;
    }
    if (state.t === 'axnumpts') {
      box.onpointerdown = function (e) {
        var p = clockPoint(box, e); if (!p) return;
        var xmin = num(state.X.xmin, -5), xmax = Math.max(xmin + 2, num(state.X.xmax, 5));
        var v = Math.round((xmin + Math.max(0, Math.min(1, (p.x - 40) / (336 - 40))) * (xmax - xmin)) * 2) / 2;
        var vs = String(v);
        var list = String(state.X.pts || '-2,1.5,4').split(/[,،]/).map(function (z) { return faEn(z.trim()); }).filter(Boolean);
        var ix = list.indexOf(vs);
        if (ix >= 0 && list.length > 1) list.splice(ix, 1); else if (ix < 0) list.push(vs);
        state.X.pts = list.join(',');
        paint(); renderFields();
      };
      box.onpointermove = box.onpointerup = box.onpointercancel = null;
      return;
    }
    if (state.t === 'axnumint' || state.t === 'axnumineq') {
      var setNumAxis = function (e) {
        var p = clockPoint(box, e); if (!p) return;
        var xmin = num(state.X.xmin, -5), xmax = Math.max(xmin + 2, num(state.X.xmax, 5));
        var v = Math.round((xmin + Math.max(0, Math.min(1, (p.x - 40) / (336 - 40))) * (xmax - xmin)) * 2) / 2;
        if (state.t === 'axnumineq') state.X.x0 = String(v);
        else if (p.x < 188) state.X.lo = String(v);
        else state.X.hi = String(v);
      };
      box.onpointerdown = function (e) { clockDrag = 'numax'; setNumAxis(e); paint(); try { box.setPointerCapture(e.pointerId); } catch (_c) {} };
      box.onpointermove = function (e) { if (!clockDrag) return; setNumAxis(e); paint(); };
      box.onpointerup = box.onpointercancel = function () { if (!clockDrag) return; clockDrag = null; renderFields(); };
      return;
    }
    if (state.t === 'axxypts') {
      box.onpointerdown = function (e) {
        var p = clockPoint(box, e); if (!p) return;
        var xmin = num(state.X.xmin, -5), xmax = Math.max(xmin + 2, num(state.X.xmax, 5));
        var ymin = num(state.X.ymin, -4), ymax = Math.max(ymin + 2, num(state.X.ymax, 4));
        var xv = Math.round(xmin + Math.max(0, Math.min(1, (p.x - 40) / (336 - 40))) * (xmax - xmin));
        var yv = Math.round(ymin + Math.max(0, Math.min(1, (246 - p.y) / (246 - 26))) * (ymax - ymin));
        var key = xv + ',' + yv;
        var list = String(state.X.pts || '1,2;-3,1;2,-2').split(/[;؛]/).map(function (z) { return faEn(z.trim()); }).filter(Boolean);
        var ix = list.indexOf(key);
        if (ix >= 0 && list.length > 1) list.splice(ix, 1); else if (ix < 0) list.push(key);
        state.X.pts = list.join(';');
        paint(); renderFields();
      };
      box.onpointermove = box.onpointerup = box.onpointercancel = null;
      return;
    }
    if (state.t === 'axxyline' || state.t === 'axxyvec' || state.t === 'axxycirc') {
      var setXy = function (e) {
        var p = clockPoint(box, e); if (!p) return;
        var xmin = num(state.X.xmin, -5), xmax = Math.max(xmin + 2, num(state.X.xmax, 5));
        var ymin = num(state.X.ymin, -4), ymax = Math.max(ymin + 2, num(state.X.ymax, 4));
        var xv = Math.round((xmin + Math.max(0, Math.min(1, (p.x - 40) / (336 - 40))) * (xmax - xmin)) * 2) / 2;
        var yv = Math.round((ymin + Math.max(0, Math.min(1, (246 - p.y) / (246 - 26))) * (ymax - ymin)) * 2) / 2;
        if (state.t === 'axxyline') state.X.b = String(yv);
        else if (state.t === 'axxyvec') { state.X.x2 = String(xv); state.X.y2 = String(yv); }
        else { state.X.cx = String(xv); state.X.cy = String(yv); }
      };
      box.onpointerdown = function (e) { clockDrag = 'xyax'; setXy(e); paint(); try { box.setPointerCapture(e.pointerId); } catch (_c) {} };
      box.onpointermove = function (e) { if (!clockDrag) return; setXy(e); paint(); };
      box.onpointerup = box.onpointercancel = function () { if (!clockDrag) return; clockDrag = null; renderFields(); };
      return;
    }
    if (state.t === 'axpolpts') {
      box.onpointerdown = function (e) {
        var p = clockPoint(box, e); if (!p) return;
        var rings = Math.max(1, Math.min(12, Math.floor(num(state.X.xmax, 4))));
        var dx = p.x - 180, dy = 136 - p.y;
        var rr = Math.max(1, Math.min(rings, Math.round(Math.hypot(dx, dy) / 104 * rings)));
        var deg = (Math.round(((Math.atan2(dy, dx) * 180 / Math.PI + 360) % 360) / 30) * 30) % 360;
        state.X.pts = rr + ',' + deg;
        paint(); renderFields();
      };
      box.onpointermove = box.onpointerup = box.onpointercancel = null;
      return;
    }
    if (state.t === 'gauge') {
      var setGauge = function (e) {
        var p = clockPoint(box, e); if (!p) return;
        var vmin = num(state.X.vmin, 0), vmax = Math.max(vmin + 1, num(state.X.vmax, 100));
        var ratio = Math.max(0, Math.min(1, (p.x - 60) / 240));
        state.X.val = String(Math.round(vmin + ratio * (vmax - vmin)));
      };
      box.onpointerdown = function (e) { clockDrag = 'gauge'; setGauge(e); paint(); try { box.setPointerCapture(e.pointerId); } catch (_c) {} };
      box.onpointermove = function (e) { if (!clockDrag) return; setGauge(e); paint(); };
      box.onpointerup = box.onpointercancel = function () { if (!clockDrag) return; clockDrag = null; renderFields(); };
      return;
    }
    if (state.t === 'axbeaker') {
      var setBeaker = function (e) {
        var p = clockPoint(box, e); if (!p) return;
        var mx = Math.max(10, num(state.X.max, 500));
        var cnt = Math.max(1, Math.min(4, Math.floor(num(state.X.count, 1))));
        var bot = cnt === 1 ? 216 : 208, topY = cnt === 1 ? 36 : 38;
        var ratio = Math.max(0, Math.min(1, (bot - p.y) / (bot - topY)));
        var step = mx >= 100 ? 10 : 1;
        var vNew = String(Math.min(mx, Math.max(0, Math.round((ratio * mx) / step) * step)));
        var bIdx = cnt === 1 ? 0 : Math.max(0, Math.min(cnt - 1, Math.floor((p.x - 12) / (336 / cnt))));
        var bKey = bIdx === 0 ? 'val' : ('val' + (bIdx + 1));
        state.X[bKey] = vNew;
      };
      box.onpointerdown = function (e) { clockDrag = 'beaker'; setBeaker(e); paint(); try { box.setPointerCapture(e.pointerId); } catch (_c) {} };
      box.onpointermove = function (e) { if (!clockDrag) return; setBeaker(e); paint(); };
      box.onpointerup = box.onpointercancel = function () { if (!clockDrag) return; clockDrag = null; renderFields(); };
      return;
    }
    if (state.t === 'axruler') {
      var setRuler = function (e) {
        var p = clockPoint(box, e); if (!p) return;
        var len = Math.max(2, Math.min(50, Math.floor(num(state.X.len, 10))));
        var ratio = Math.max(0, Math.min(1, (p.x - 24) / (336 - 24)));
        state.X.a = '0';
        state.X.b = String(Math.max(1, Math.min(len, Math.round(ratio * len))));
      };
      box.onpointerdown = function (e) { clockDrag = 'ruler'; setRuler(e); paint(); try { box.setPointerCapture(e.pointerId); } catch (_c) {} };
      box.onpointermove = function (e) { if (!clockDrag) return; setRuler(e); paint(); };
      box.onpointerup = box.onpointercancel = function () { if (!clockDrag) return; clockDrag = null; renderFields(); };
      return;
    }
    if (state.t === 'axtally') {
      box.onpointerdown = function (e) {
        var p = clockPoint(box, e); if (!p) return;
        var cur = Math.max(0, Math.min(75, Math.floor(num(state.X.n, 7))));
        state.X.n = String(p.x >= 180 ? Math.min(75, cur + 1) : Math.max(1, cur - 1));
        paint(); renderFields();
      };
      box.onpointermove = box.onpointerup = box.onpointercancel = null;
      return;
    }
    if (state.t === 'axbase10') {
      box.onpointerdown = function (e) {
        var p = clockPoint(box, e); if (!p) return;
        var rawClean = faEn(String(state.X.n == null ? '134' : state.X.n)).replace(/\D/g, '');
        var cur = Math.max(0, Math.min(999999999, rawClean ? parseInt(rawClean.slice(0, 9), 10) : 134));
        if (p.y >= 340) {
          var lb = String(state.X.label || '1');
          state.X.label = lb === '1' ? '2' : (lb === '2' ? '0' : '1');
        } else {
          var bp = Math.max(0, Math.min(8, 8 - Math.floor((p.y - 4) / 36)));
          var pow = Math.pow(10, bp);
          var d = Math.floor(cur / pow) % 10;
          state.X.n = String(cur + (((d + 1) % 10) - d) * pow);
        }
        paint(); renderFields();
      };
      box.onpointermove = box.onpointerup = box.onpointercancel = null;
      return;
    }
    if (state.t === 'axplace') {
      box.onpointerdown = function (e) {
        var p = clockPoint(box, e); if (!p) return;
        var cur = Math.max(0, parseInt(faEn(String(state.X.n || '2457')).replace(/\D/g, '') || '0', 10));
        var col = Math.max(0, Math.min(11, Math.floor((p.x - 30) / (326 / 12))));
        var pow = Math.pow(10, 11 - col);
        var d = Math.floor(cur / pow) % 10;
        state.X.n = String(cur + (((d + 1) % 10) - d) * pow);
        paint(); renderFields();
      };
      box.onpointermove = box.onpointerup = box.onpointercancel = null;
      return;
    }
    if (state.t === 'axmoney') {
      box.onpointerdown = function (e) {
        var p = clockPoint(box, e);
        if (p && p.y < 140) {
          var nts = String(state.X.notes || '1000,500').split(/[,،]/).map(function (z) { return z.trim(); }).filter(Boolean);
          if (nts.length >= 5) nts.pop(); else nts.push('1000');
          state.X.notes = nts.join(',');
        } else if (p && p.y < 230) {
          var cns = String(state.X.coins || '200,100,100').split(/[,،]/).map(function (z) { return z.trim(); }).filter(Boolean);
          if (cns.length >= 6) cns.pop(); else cns.push('100');
          state.X.coins = cns.join(',');
        } else {
          var sm = String(state.X.sum || '1');
          state.X.sum = sm === '1' ? '2' : (sm === '2' ? '0' : '1');
        }
        paint(); renderFields();
      };
      box.onpointermove = box.onpointerup = box.onpointercancel = null;
      return;
    }
    if (state.t === 'axjump') {
      box.onpointerdown = function (e) {
        var p = clockPoint(box, e); if (!p) return;
        var s0 = Math.max(0, Math.floor(num(state.X.start, 0)));
        if (p.x >= 180) {
          var j = (Math.max(1, Math.floor(num(state.X.jumps, 4))) % 8) + 1;
          var st = Math.max(1, Math.floor(num(state.X.step, 3)));
          state.X.jumps = String(j); state.X.max = String(s0 + j * st);
        } else {
          var st2 = (Math.max(1, Math.floor(num(state.X.step, 3))) % 6) + 1;
          var j2 = Math.max(1, Math.floor(num(state.X.jumps, 4)));
          state.X.step = String(st2); state.X.max = String(s0 + j2 * st2);
        }
        paint(); renderFields();
      };
      box.onpointermove = box.onpointerup = box.onpointercancel = null;
      return;
    }
    if (state.t === 'axbalance') {
      box.onpointerdown = function (e) {
        var p = clockPoint(box, e); if (!p) return;
        if (p.x < 180) state.X.left = String((Math.max(1, Math.floor(num(state.X.left, 5))) % 10) + 1);
        else state.X.right = String((Math.max(1, Math.floor(num(state.X.right, 3))) % 10) + 1);
        paint(); renderFields();
      };
      box.onpointermove = box.onpointerup = box.onpointercancel = null;
      return;
    }
    if (state.t === 'axsaw') {
      box.onpointerdown = function (e) {
        var p = clockPoint(box, e); if (!p) return;
        var raw = String(state.X.vals || '1,2,3,4;3,4,?,2;2,?,4,3;4,3,2,1').split(/[;؛]/).map(function (r0) { return r0.split(/[,،]/).map(function (z) { return faEn(z.trim()); }); }).filter(function (r0) { return r0.length > 0; });
        var exp = Math.floor(num(state.X.size, 0));
        var n2 = Math.max(3, Math.min(9, (exp >= 3 && exp <= 9) ? exp : raw.length));
        var mode = String(state.X.mode || 'shape').toLowerCase();
        var sleg = mode === 'color' || mode === 'color_shape';
        var maxG = sleg ? 196 : 216, cs = Math.min(52, maxG / n2), gw2 = cs * n2;
        var x0 = (360 - gw2) / 2, y0 = (sleg ? 24 : 28) + (maxG - gw2) / 2;
        if (p.x < x0 || p.x > x0 + gw2 || p.y < y0 || p.y > y0 + gw2) return;
        var c = Math.max(0, Math.min(n2 - 1, Math.floor((p.x - x0) / cs)));
        var r = Math.max(0, Math.min(n2 - 1, Math.floor((p.y - y0) / cs)));
        var grid = [], ri, ci;
        for (ri = 0; ri < n2; ri++) {
          var rowArr = [];
          for (ci = 0; ci < n2; ci++) {
            var defV = n2 === 9 ? (((ri * 3 + Math.floor(ri / 3) + ci) % 9) + 1) : n2 === 6 ? (((ri * 3 + Math.floor(ri / 2) + ci) % 6) + 1) : (((ri + ci) % n2) + 1);
            var cellV = (raw.length === n2 && raw[ri] && raw[ri][ci]) ? raw[ri][ci] : (((ri * 2 + ci + 1) % (n2 + 1) === 0) ? '?' : String(defV));
            rowArr.push(cellV);
          }
          grid.push(rowArr);
        }
        var sawPalMeta = [
          ['1', '●', '#e53935', 'قرمز'], ['2', '▲', '#1e88e5', 'آبی'], ['3', '■', '#43a047', 'سبز'],
          ['4', '★', '#fbc02d', 'زرد'], ['5', '◆', '#8e24aa', 'بنفش'], ['6', '♥', '#fb8c00', 'نارنجی'],
          ['7', '⬢', '#00acc1', 'فیروزه‌ای'], ['8', '✿', '#ec407a', 'صورتی'], ['9', '▼', '#6d4c41', 'قهوه‌ای']
        ];
        var chips = [];
        for (var k2 = 0; k2 < n2; k2++) {
          var pm = sawPalMeta[k2];
          var lbl = mode === 'num' ? pm[0] : mode === 'color' ? pm[3] : mode === 'color_shape' ? (pm[1] + ' ' + pm[3]) : (pm[1] + ' (' + pm[0] + ')');
          var st = (mode === 'color' || mode === 'color_shape') ? ('border-color:' + pm[2] + ';color:' + pm[2]) : '';
          chips.push([pm[0], lbl, st]);
        }
        var setCell = function (val) {
          grid[r][c] = val;
          state.X.size = String(n2);
          state.X.vals = grid.map(function (row) { return row.join(','); }).join(';');
          paint(); renderFields();
        };
        openGfCellPopup({
          title: 'مقدار خانه (سطر ' + (r + 1) + '، ستون ' + (c + 1) + ')',
          sub: 'یکی از گزینه‌های زیر را برای این خانه انتخاب کنید:',
          chips: chips,
          current: grid[r][c],
          onPick: setCell,
          onQuestion: function () { setCell('?'); },
          onEmpty: function () { setCell('_'); },
          emptyLabel: 'خانهٔ خالی (بدون ؟)'
        });
      };
      box.onpointermove = box.onpointerup = box.onpointercancel = null;
      return;
    }
    if (state.t === 'axpattern') {
      box.onpointerdown = function (e) {
        var p = clockPoint(box, e); if (!p) return;
        var items = String(state.X.seq || '●,▲,●,▲,●,?').split(/[,،]/).map(function (z) { return z.trim(); }).filter(Boolean);
        if (!items.length) return;
        var n = items.length, cw = Math.min(42, 300 / n), gap = n <= 8 ? 6 : 4;
        var x0 = (360 - (n * cw + (n - 1) * gap)) / 2;
        if (p.x < x0 - 8 || p.x > x0 + n * (cw + gap) || p.y < 92 || p.y > 190) return;
        var idx = Math.max(0, Math.min(n - 1, Math.floor((p.x - x0) / (cw + gap))));
        var mode = String(state.X.mode || 'shape').toLowerCase();
        var chips = (mode === 'color' || mode === 'bead')
          ? [['قرمز', '🔴 قرمز'], ['آبی', '🔵 آبی'], ['سبز', '🟢 سبز'], ['زرد', '🟡 زرد'], ['بنفش', '🟣 بنفش'], ['نارنجی', '🟠 نارنجی']]
          : (mode === 'number' || mode === 'growing')
            ? [['1', '1'], ['2', '2'], ['3', '3'], ['4', '4'], ['5', '5'], ['6', '6'], ['8', '8'], ['10', '10'], ['12', '12'], ['16', '16']]
            : [['●', '● دایره'], ['▲', '▲ مثلث'], ['■', '■ مربع'], ['★', '★ ستاره'], ['◆', '◆ لوزی'], ['♥', '♥ قلب']];
        var setSlot = function (val) {
          items[idx] = val;
          state.X.seq = items.join(',');
          paint(); renderFields();
        };
        openGfCellPopup({
          title: 'ویرایش عضو شمارهٔ ' + (idx + 1) + ' الگو',
          sub: 'از شکل‌ها/رنگ‌ها انتخاب کنید یا عدد/متن دلخواه بنویسید:',
          chips: chips,
          current: items[idx],
          allowCustom: true,
          placeholder: 'عدد یا نماد دلخواه',
          inputVal: items[idx] === '?' ? '' : items[idx],
          onPick: setSlot,
          onQuestion: function () { setSlot('?'); }
        });
      };
      box.onpointermove = box.onpointerup = box.onpointercancel = null;
      return;
    }
    if (state.t === 'axnumgrid') {
      box.onpointerdown = function (e) {
        var p = clockPoint(box, e); if (!p) return;
        var start = Math.max(0, Math.min(900, Math.floor(num(state.X.start, 1))));
        var cols = Math.max(4, Math.min(10, parseInt(faEn(String(state.X.cols || '10')), 10) || 10));
        var rows = Math.max(3, Math.min(10, parseInt(faEn(String(state.X.rows || '10')), 10) || 10));
        var cs = Math.min(280 / cols, 218 / rows), gw = cs * cols, gh = cs * rows;
        var x0 = (360 - gw) / 2, y0 = 26 + (222 - gh) / 2;
        var c = Math.max(0, Math.min(cols - 1, Math.floor((p.x - x0) / cs)));
        var r = Math.max(0, Math.min(rows - 1, Math.floor((p.y - y0) / cs)));
        var target = String(start + r * cols + c);
        var list = String(state.X.blanks || '15,35,60,85').split(/[,،;\s]+/).map(function (z) { return faEn(z.trim()); }).filter(Boolean);
        var ix = list.indexOf(target);
        if (ix >= 0) list.splice(ix, 1); else list.push(target);
        state.X.blanks = list.join(',');
        paint(); renderFields();
      };
      box.onpointermove = box.onpointerup = box.onpointercancel = null;
      return;
    }
    if (state.t === 'axfacttree' || state.t === 'axfnmachine') {
      box.onpointerdown = function () {
        state.X.blank = String((Math.floor(num(state.X.blank, 1)) + 1) % 3);
        paint(); renderFields();
      };
      box.onpointermove = box.onpointerup = box.onpointercancel = null;
      return;
    }
    if (state.t === 'axfracwall') {
      box.onpointerdown = function (e) {
        var p = clockPoint(box, e); if (!p) return;
        var dens = String(state.X.dens || '1,2,3,4,6,8').split(/[,،;]/).map(function (z) { return Math.max(1, Math.min(12, parseInt(faEn(z.trim()), 10) || 1)); }).slice(0, 7);
        if (!dens.length) dens = [1, 2, 3, 4, 6, 8];
        var fills = String(state.X.fills || '1,1,2,2,3,4').split(/[,،;]/).map(function (z) { return parseInt(faEn(z.trim()), 10); });
        while (fills.length < dens.length) fills.push(1);
        var rh = Math.min(34, 210 / dens.length), y0 = 30 + (214 - rh * dens.length) / 2;
        var rIdx = Math.max(0, Math.min(dens.length - 1, Math.floor((p.y - y0) / rh)));
        fills[rIdx] = ((isFinite(fills[rIdx]) ? fills[rIdx] : 1) + 1) % (dens[rIdx] + 1);
        state.X.fills = fills.slice(0, dens.length).join(',');
        paint(); renderFields();
      };
      box.onpointermove = box.onpointerup = box.onpointercancel = null;
      return;
    }
    if (state.t === 'axtangram') {
      box.onpointerdown = function () {
        var shapes = ['square', 'house', 'boat', 'cat'];
        var cur = String(state.X.shape || 'square').toLowerCase();
        state.X.shape = shapes[(shapes.indexOf(cur) + 1) % shapes.length];
        paint(); renderFields();
      };
      box.onpointermove = box.onpointerup = box.onpointercancel = null;
      return;
    }
    if (state.t === 'axangle') {
      var setAng = function (e) {
        var p = clockPoint(box, e); if (!p) return;
        var deg = Math.round(Math.atan2(212 - p.y, p.x - 180) * 180 / Math.PI);
        state.X.deg = String(Math.max(5, Math.min(175, deg)));
      };
      box.onpointerdown = function (e) { clockDrag = 'angle'; setAng(e); paint(); try { box.setPointerCapture(e.pointerId); } catch (_c) {} };
      box.onpointermove = function (e) { if (!clockDrag) return; setAng(e); paint(); };
      box.onpointerup = box.onpointercancel = function () { if (!clockDrag) return; clockDrag = null; renderFields(); };
      return;
    }
    if (state.t === 'axsym') {
      box.onpointerdown = function (e) {
        var p = clockPoint(box, e); if (!p) return;
        if (p.x < 76 || p.x > 284 || p.y < 32 || p.y > 240) return;
        var c = Math.max(0, Math.min(7, Math.floor((p.x - 76) / 26)));
        var r = Math.max(0, Math.min(7, Math.floor((p.y - 32) / 26)));
        var axis = String(state.X.axis || 'v').toLowerCase();
        var mirrorOn = Math.floor(num(state.X.mirror, 1)) !== 0;
        var pair;
        if (axis === 'h') {
          var dH = mirrorOn ? (r < 4 ? 4 - r : r - 3) : (r < 4 ? 4 - r : r + 1);
          pair = (c + 1) + ',' + dH;
        } else {
          var dV = mirrorOn ? (c < 4 ? 4 - c : c - 3) : (c < 4 ? 4 - c : c + 1);
          pair = (r + 1) + ',' + dV;
        }
        var rawCells = Object.prototype.hasOwnProperty.call(state.X, 'cells') ? String(state.X.cells == null ? '' : state.X.cells) : '1,1;2,1;2,2;3,2';
        var list = rawCells.split(/[;؛]/).map(function (z) { return faEn(z.trim()); }).filter(Boolean);
        var ix = list.indexOf(pair);
        if (ix >= 0) list.splice(ix, 1);
        else list.push(pair);
        state.X.cells = list.join(';');
        paint(); renderFields();
      };
      box.onpointermove = box.onpointerup = box.onpointercancel = null;
      return;
    }
    if (state.t === 'axpaintgrid') {
      box.onpointerdown = function (e) {
        var p = clockPoint(box, e); if (!p) return;
        var rows = Math.max(1, Math.min(8, Math.round(num(state.X.rows, 4))));
        var cols = Math.max(1, Math.min(8, Math.round(num(state.X.cols, 4))));
        var w = Math.min(300, cols * 42), h = Math.min(150, rows * 34);
        var x0 = (360 - w) / 2, y0 = 62 + (150 - h) / 2;
        if (p.x < x0 || p.x > x0 + w || p.y < y0 || p.y > y0 + h) return;
        var cc = Math.max(1, Math.min(cols, Math.floor((p.x - x0) / (w / cols)) + 1));
        var rr = Math.max(1, Math.min(rows, Math.floor((p.y - y0) / (h / rows)) + 1));
        var key = rr + ',' + cc;
        var list = String(state.X.cells || '').split(/[;؛]/).map(function (z) { return faEn(z.trim()); }).filter(Boolean);
        var ix = list.indexOf(key);
        if (ix >= 0) list.splice(ix, 1); else list.push(key);
        state.X.cells = list.join(';');
        paint(); renderFields();
      };
      box.onpointermove = box.onpointerup = box.onpointercancel = null;
      return;
    }
    if (state.t === 'axcalendar') {
      box.onpointerdown = function (e) {
        var p = clockPoint(box, e); if (!p) return;
        var clStart = Math.max(0, Math.min(6, Math.floor(num(state.X.start, 3))));
        var clDays = Math.max(20, Math.min(31, Math.floor(num(state.X.days, 30))));
        var clCw = 44, clCh = 30, clX0 = 180 - clCw * 3.5, clY0 = 46 + clCh;
        if (p.x < clX0 || p.x > clX0 + 7 * clCw || p.y < clY0) return;
        var col2 = Math.floor((p.x - clX0) / clCw), row2 = Math.floor((p.y - clY0) / clCh);
        var cellIdx = row2 * 7 + (6 - col2), dayN = cellIdx - clStart + 1;
        if (dayN < 1 || dayN > clDays) return;
        gfOpenKeyedCellEditor(String(dayN), 'روز ' + dayN + ' تقویم', String(dayN));
      };
      box.onpointermove = box.onpointerup = box.onpointercancel = null;
      return;
    }
    if (state.t === 'axmultgrid') {
      box.onpointerdown = function (e) {
        var p = clockPoint(box, e); if (!p) return;
        var rNums = String(state.X.rows || '2,4,7').split(/[,،;]/).map(function (z) { return parseInt(faEn(z.trim()), 10); }).filter(isFinite).slice(0, 4);
        var cNums = String(state.X.cols || '3,5,8').split(/[,،;]/).map(function (z) { return parseInt(faEn(z.trim()), 10); }).filter(isFinite).slice(0, 4);
        if (!rNums.length) rNums = [2, 4, 7];
        if (!cNums.length) cNums = [3, 5, 8];
        var cw = Math.min(56, 268 / (cNums.length + 1)), ch = Math.min(42, 216 / (rNums.length + 1));
        var x0 = 180 - cw * (cNums.length + 1) / 2, y0 = 26 + (218 - ch * (rNums.length + 1)) / 2;
        if (p.x < x0 || p.x > x0 + (cNums.length + 1) * cw || p.y < y0 || p.y > y0 + (rNums.length + 1) * ch) return;
        var gc = Math.floor((p.x - x0) / cw), gr = Math.floor((p.y - y0) / ch);
        if (gr === 0 && gc === 0) return;
        if (gr === 0) gfOpenKeyedCellEditor('c' + (gc - 1), 'سرستون ' + gc, String(cNums[gc - 1]));
        else if (gc === 0) gfOpenKeyedCellEditor('r' + (gr - 1), 'سرسطر ' + gr, String(rNums[gr - 1]));
        else gfOpenKeyedCellEditor((gr - 1) + '_' + (gc - 1), 'خانهٔ (' + rNums[gr - 1] + ' × ' + cNums[gc - 1] + ')', String(rNums[gr - 1] * cNums[gc - 1]));
      };
      box.onpointermove = box.onpointerup = box.onpointercancel = null;
      return;
    }
    if (state.t === 'axmulwheel') {
      box.onpointerdown = function (e) {
        var p = clockPoint(box, e); if (!p) return;
        var mwC = Math.max(1, Math.min(12, Math.floor(num(state.X.c, 3))));
        var mwN = Math.max(3, Math.min(12, Math.floor(num(state.X.n, 10))));
        var mwOp = String(state.X.op || '×') === '+' ? '+' : '×';
        if (Math.hypot(p.x - 180, p.y - 142) <= 34) {
          gfOpenKeyedCellEditor('center', 'عدد مرکز چرخ', String(mwC));
          return;
        }
        for (var j = 0; j < mwN; j++) {
          var a = -Math.PI / 2 + j * 2 * Math.PI / mwN, ox = 180 + 96 * Math.cos(a), oy = 142 + 96 * Math.sin(a);
          if (Math.hypot(p.x - ox, p.y - oy) <= 22) {
            var defV = mwOp === '+' ? (mwC + j + 1) : (mwC * (j + 1));
            gfOpenKeyedCellEditor('s_' + j, 'خانهٔ بیرونی پرهٔ ' + (j + 1), String(defV));
            return;
          }
        }
      };
      box.onpointermove = box.onpointerup = box.onpointercancel = null;
      return;
    }
    if (state.t === 'axtower') {
      box.onpointerdown = function (e) {
        var p = clockPoint(box, e); if (!p) return;
        var base = String(state.X.base || '1,2,3,4').split(/[,،;]/).map(function (z) { return parseInt(faEn(z.trim()), 10); }).filter(isFinite).slice(0, 6);
        if (base.length < 2) base = [1, 2, 3, 4];
        var op = String(state.X.op || '+') === '-' ? '−' : '+';
        var lv = [base], cur = base, k0;
        while (cur.length > 1) {
          var nxt = [];
          for (k0 = 0; k0 < cur.length - 1; k0++) nxt.push(op === '+' ? cur[k0] + cur[k0 + 1] : Math.abs(cur[k0] - cur[k0 + 1]));
          lv.push(nxt); cur = nxt;
        }
        var bh = Math.min(34, 216 / lv.length), bw = Math.min(58, 300 / base.length);
        for (var row = 0; row < lv.length; row++) {
          var lvIdx = lv.length - 1 - row, arr = lv[lvIdx], ly = 24 + row * bh;
          if (p.y >= ly && p.y <= ly + bh) {
            for (k0 = 0; k0 < arr.length; k0++) {
              var bx = 180 + (k0 - (arr.length - 1) / 2) * bw - bw / 2;
              if (p.x >= bx && p.x <= bx + bw) {
                gfOpenKeyedCellEditor(lvIdx + '_' + k0, 'آجر ردیف ' + (lvIdx + 1) + '، شمارهٔ ' + (k0 + 1), String(arr[k0]));
                return;
              }
            }
          }
        }
      };
      box.onpointermove = box.onpointerup = box.onpointercancel = null;
      return;
    }
    if (state.t === 'axcubes') {
      box.onpointerdown = function (e) {
        var p = clockPoint(box, e);
        var vw = String(state.X.view || 'iso').toLowerCase();
        if (vw === 'top' && p) {
          var rowsRaw = String(state.X.map || '2,1,3;1,0,1;2,2,1').split(/[;؛]/).map(function (rw) { return rw.split(/[,،]/).map(function (q2) { var iv = parseInt(faEn(q2.trim()), 10); return Math.max(0, Math.min(5, isFinite(iv) ? iv : 0)); }); }).filter(function (rw) { return rw.length; }).slice(0, 4);
          if (!rowsRaw.length) rowsRaw = [[2, 1, 3], [1, 0, 1], [2, 2, 1]];
          var cgN = rowsRaw.length, cgM = Math.max(1, rowsRaw[0].length);
          var cgW = Math.min(54, 220 / cgM), cgH = Math.min(50, 184 / cgN);
          var cgX0 = 180 - (cgM * cgW) / 2, cgY0 = 126 - (cgN * cgH) / 2;
          if (p.x >= cgX0 && p.x <= cgX0 + cgM * cgW && p.y >= cgY0 && p.y <= cgY0 + cgN * cgH) {
            var cc2 = Math.max(0, Math.min(cgM - 1, Math.floor((p.x - cgX0) / cgW)));
            var rr2 = Math.max(0, Math.min(cgN - 1, Math.floor((p.y - cgY0) / cgH)));
            while (rowsRaw[rr2].length < cgM) rowsRaw[rr2].push(0);
            rowsRaw[rr2][cc2] = (rowsRaw[rr2][cc2] + 1) % 6;
            state.X.map = rowsRaw.map(function (rw) { return rw.slice(0, cgM).join(','); }).join(';');
            paint(); renderFields();
            return;
          }
        }
        var views = ['iso', 'top', 'front', 'side'];
        state.X.view = views[(views.indexOf(vw) + 1) % views.length];
        paint(); renderFields();
      };
      box.onpointermove = box.onpointerup = box.onpointercancel = null;
      return;
    }
    if (state.t === 'axgeoboard') {
      box.onpointerdown = function (e) {
        var p = clockPoint(box, e); if (!p) return;
        var g = Math.max(3, Math.min(7, Math.floor(num(state.X.grid, 5))));
        var step = 200 / Math.max(1, g - 1);
        var px = Math.max(0, Math.min(g - 1, Math.round((p.x - 80) / step))) + 1;
        var py = g - Math.max(0, Math.min(g - 1, Math.round((p.y - 36) / step)));
        var key = px + ',' + py;
        var list = String(state.X.pts || '1,1;4,1;4,3;1,3').split(/[;؛]/).map(function (z) { return faEn(z.trim()); }).filter(Boolean);
        var ix = list.indexOf(key);
        if (ix >= 0 && list.length > 3) list.splice(ix, 1);
        else if (ix < 0) list.push(key);
        state.X.pts = list.join(';');
        paint(); renderFields();
      };
      box.onpointermove = box.onpointerup = box.onpointercancel = null;
      return;
    }
    if (state.t === 'axdice') {
      box.onpointerdown = function (e) {
        var p = clockPoint(box, e); if (!p) return;
        var dl = String(state.X.dice || '4,6').split(/[,،]/).map(function (z) { return parseInt(faEn(z.trim()), 10); }).filter(function (v2) { return v2 >= 1 && v2 <= 6; });
        if (!dl.length) dl = [4, 6];
        var x0 = (360 - (dl.length * 66 + (dl.length - 1) * 22)) / 2;
        var idx = Math.max(0, Math.min(dl.length - 1, Math.floor((p.x - x0) / 88)));
        dl[idx] = (dl[idx] % 6) + 1;
        state.X.dice = dl.join(',');
        paint(); renderFields();
      };
      box.onpointermove = box.onpointerup = box.onpointercancel = null;
      return;
    }
    if (state.t === 'axmularea') {
      box.onpointerdown = function () {
        state.X.blank = Math.floor(num(state.X.blank, 0)) === 0 ? '1' : '0';
        paint(); renderFields();
      };
      box.onpointermove = box.onpointerup = box.onpointercancel = null;
      return;
    }
    if (state.t === 'axfrac') {
      box.onpointerdown = function () {
        var den = Math.max(1, Math.min(25, Math.floor(num(state.X.den, 4))));
        var cur = Math.max(0, Math.min(den, Math.floor(num(state.X.num, 1))));
        state.X.num = String((cur + 1) % (den + 1));
        paint(); renderFields();
      };
      box.onpointermove = box.onpointerup = box.onpointercancel = null;
      return;
    }
    if (state.t === 'axabacus') {
      box.onpointerdown = function (e) {
        var p = clockPoint(box, e); if (!p) return;
        var cols = Math.max(2, Math.min(6, Math.floor(num(state.X.cols, 6))));
        var cidx = Math.max(0, Math.min(cols - 1, Math.floor((p.x - 24) / (412 / cols))));
        var place = cols - 1 - cidx, pow = Math.pow(10, place);
        var curN = Math.max(0, Math.min(999999, Math.floor(num(state.X.n, 142536))));
        var curD = Math.floor(curN / pow) % 10, nextD = (curD + 1) % 10;
        state.X.n = String(curN + (nextD - curD) * pow);
        paint(); renderFields();
      };
      box.onpointermove = box.onpointerup = box.onpointercancel = null;
      return;
    }
    if (state.t === 'axthermo') {
      var setThermo = function (e) {
        var p = clockPoint(box, e); if (!p) return;
        var mn = num(state.X.min, -10), mx = Math.max(mn + 5, num(state.X.max, 50));
        var ratio = Math.max(0, Math.min(1, (204 - p.y) / (204 - 34)));
        state.X.val = String(Math.round(mn + ratio * (mx - mn)));
      };
      box.onpointerdown = function (e) { clockDrag = 'thermo'; setThermo(e); paint(); try { box.setPointerCapture(e.pointerId); } catch (_c) {} };
      box.onpointermove = function (e) { if (!clockDrag) return; setThermo(e); paint(); };
      box.onpointerup = box.onpointercancel = function () { if (!clockDrag) return; clockDrag = null; renderFields(); };
      return;
    }
    var CYCLE = {
      axrod: ['blank', ['0', '1']],
      axnet: ['shape', ['cube', 'cuboid', 'pyramid', 'prism']],
      axlever: ['kind', ['lever1', 'lever2', 'lever3', 'pulley', 'gears']],
      axcycle: ['kind', ['water', 'plant', 'frog', 'food']],
      axwordtrain: ['mode', ['join', 'blank']],
      axmagnet: ['mode', ['attract', 'repel', 'repel_s']],
      axtransform: ['op', ['translate', 'rotate', 'reflect']],
      axsigntable: ['lead', ['1', '-1']],
      axconic: ['kind', ['ellipse', 'hyperbola', 'parabola']],
      axecg: ['kind', ['ecg', 'shm']],
      axprosody: ['mode', ['arkan', 'taqti', 'blank']],
      axtruthtable: ['kind', ['truth', 'square']],
      axstaff: ['kind', ['staff', 'color']],
      axkmap: ['vars', ['2', '3', '4']],
      axbst: ['mode', ['bst', 'heap']],
      axbode: ['kind', ['bode', 'nyquist']],
      axbeam: ['load', ['point', 'udl']],
      axpv: ['kind', ['carnot', 'otto', 'diesel', 'rankine']],
      axbenzene: ['kind', ['kekule', 'chair', 'newman', 'fischer']],
      axsupply: ['kind', ['equilibrium', 'shift', 'ceiling']],
      axgame: ['kind', ['game', 'islm']],
      axperimeter: ['shape', ['rect', 'square', 'tri', 'para', 'trap', 'rhomb', 'circle', 'poly']],
      axpictograph: ['mode', ['picto', 'bar']],
      axrounding: ['mode', ['ten', 'hundred', 'thousand', 'one', 'tenth']],
      axdivision: ['b', ['2', '3', '4', '5', '6', '7', '8', '9', '10']],
      axshapeid: ['kind', ['tri', 'square', 'rect', 'circle', 'poly']],
      axcircleparts: ['show', ['all', 'radius', 'diameter', 'chord', 'tangent', 'arc']],
      axcylinder: ['kind', ['cylinder', 'cone']],
      axproportion: ['a', ['1', '2', '3', '4', '5', '6', '8', '10']],
      axbarchart: ['show', ['mean', 'median', 'mode', 'all', 'none']],
      axinthales: ['ang', ['30', '45', '60', '90', '120', '150']],
      axpendulum: ['mode', ['period', 'forces', 'energy', 'quiz']],
      axincline: ['show', ['all', 'forces', 'plain']],
      axcircuit: ['mode', ['series', 'parallel', 'mixed']],
      axoptics: ['kind', ['convex', 'concave']],
      axheatcurve: ['mode', ['full', 'phases', 'quiz']],
      axdna: ['mode', ['helix', 'flat', 'mrna', 'quiz']],
      axcell: ['kind', ['animal', 'plant']],
      axmapgeo: ['kind', ['compass', 'map']],
      axsentence: ['mode', ['verbal', 'nominal']],
      axvectors: ['mode', ['sum', 'diff']],
      axintegral: ['f', ['quad', 'sin', 'line']],
      axstack: ['mode', ['stack', 'queue']],
      axsort: ['step', ['0', '1', '2', '3']]
    };
    var NUMSTEP = {
      axcaliper: ['val', 0.5, 0, 50],
      axmohr: ['txy', 5, 0, 120],
      axperimeter: ['w', 1, 1, 12],
      axrounding: ['n', 10, 10, 990],
      axdivision: ['a', 1, 0, 60],
      axshapeid: ['n', 1, 3, 8],
      axcylinder: ['r', 1, 1, 8],
      axproportion: ['c', 1, 1, 20],
      axlinear: ['b1', 1, -5, 5],
      axparabola: ['c', 1, -6, 6],
      axinthales: ['ang', 10, 20, 160],
      axpendulum: ['len', 1, 1, 3],
      axincline: ['ang', 5, 10, 60],
      axcircuit: ['r1', 1, 1, 20],
      axoptics: ['d', 5, 10, 60],
      axsort: ['step', 1, 0, 4]
    };
    if (CYCLE[state.t]) {
      box.onpointerdown = function () {
        var cspec = CYCLE[state.t], vals = cspec[1], key = cspec[0];
        var cur = state.X[key] == null || String(state.X[key]) === '' ? vals[0] : String(state.X[key]);
        var ix = vals.indexOf(cur);
        state.X[key] = vals[(ix + 1) % vals.length];
        paint(); renderFields();
      };
      box.onpointermove = box.onpointerup = box.onpointercancel = null;
      return;
    }
    if (NUMSTEP[state.t]) {
      box.onpointerdown = function () {
        var nspec = NUMSTEP[state.t], key = nspec[0], step = nspec[1], lo = nspec[2], hi = nspec[3];
        var cur = num(state.X[key], lo), nx = Math.round((cur + step) * 100) / 100;
        if (nx > hi) nx = lo;
        state.X[key] = String(nx);
        paint(); renderFields();
      };
      box.onpointermove = box.onpointerup = box.onpointercancel = null;
      return;
    }
    if (state.t === 'axgate') {
      box.onpointerdown = function (e) {
        var p = clockPoint(box, e);
        var key = (p && p.y > 132) ? 'b' : 'a';
        state.X[key] = String(state.X[key] === '1' ? '0' : (state.X[key] === '0' ? '1' : (key === 'a' ? '1' : '0')));
        paint(); renderFields();
      };
      box.onpointermove = box.onpointerup = box.onpointercancel = null;
      return;
    }
    if (state.t === 'axtrigcircle') {
      var setTrig = function (e) {
        var p = clockPoint(box, e); if (!p) return;
        var deg = Math.atan2(134 - p.y, p.x - 152) * 180 / Math.PI;
        state.X.deg = String(Math.round((deg + 360) % 360));
      };
      box.onpointerdown = function (e) { clockDrag = 'trig'; setTrig(e); paint(); try { box.setPointerCapture(e.pointerId); } catch (_c) {} };
      box.onpointermove = function (e) { if (!clockDrag) return; setTrig(e); paint(); };
      box.onpointerup = box.onpointercancel = function () { if (!clockDrag) return; clockDrag = null; renderFields(); };
      return;
    }
    box.onpointerdown = function (e) {
      var p = clockPoint(box, e);
      if (!p) return;
      clockDrag = Math.hypot(p.x - 180, p.y - 150) <= 62 ? 'hour' : 'minute';
      clockSet(box, e);
      paint();
      try { box.setPointerCapture(e.pointerId); } catch (_c) {}
    };
    box.onpointermove = function (e) {
      if (!clockDrag) return;
      clockSet(box, e);
      paint();
    };
    box.onpointerup = box.onpointercancel = function () {
      if (!clockDrag) return;
      clockDrag = null;
      renderFields();
    };
  }
  function paint() {
    var box = $('grPreview');
    if (!box) return;
    box.innerHTML = svgOf(state);
    bindClock(box);
  }
  function getPath(obj, path) {
    return path.split('.').reduce(function (o, k) { return o && o[k]; }, obj);
  }
  function setPath(obj, path, val) {
    var ks = path.split('.'), o = obj, i;
    for (i = 0; i < ks.length - 1; i++) {
      if (!o[ks[i]] || typeof o[ks[i]] !== 'object') o[ks[i]] = {};
      o = o[ks[i]];
    }
    o[ks[ks.length - 1]] = val;
  }
  var GF_DROPDOWNS = {
    // V319
    'axunitconv:X.kind': [['len', 'طول (km → m → cm → mm)'], ['mass', 'جرم (t → kg → g → mg)'], ['area', 'مساحت (m² → dm² → cm² → mm²)'], ['vol', 'حجم (m³ → L → mL)'], ['time', 'زمان (ساعت → دقیقه → ثانیه)']],
    'axperimeter:X.shape': [['rect', 'مستطیل'], ['square', 'مربع'], ['tri', 'مثلث قائم‌الزاویه'], ['para', 'متوازی‌الاضلاع'], ['trap', 'ذوزنقه'], ['rhomb', 'لوزی'], ['circle', 'دایره'], ['poly', 'چندضلعی منتظم']],
    'axperimeter:X.show': [['both', 'هم محیط و هم مساحت'], ['perim', 'فقط محیط (P)'], ['area', 'فقط مساحت (S)'], ['none', 'هیچ‌کدام (بدون فرمول زیر شکل)']],
    'axpictograph:X.mode': [['picto', 'تصویری (شکل‌ها)'], ['bar', 'میله‌ای']],
    'axrounding:X.mode': [['ten', 'دهگان'], ['hundred', 'صدگان'], ['thousand', 'هزارگان'], ['one', 'یکان'], ['tenth', 'دهم (۰٫۱)'], ['hundredth', 'صدم (۰٫۰۱)']],
    'axrounding:X.label': [['1', 'نمایش پاسخ گردشده'], ['2', 'جای خالی (≈ ؟)'], ['0', 'بدون متن زیر محور']],
    'axshapeid:X.kind': [['tri', 'مثلث'], ['square', 'مربع'], ['rect', 'مستطیل'], ['circle', 'دایره'], ['poly', 'چندضلعی منتظم']],
    'axcircleparts:X.show': [['all', 'همهٔ اجزا'], ['radius', 'شعاع'], ['diameter', 'قطر'], ['chord', 'وتر'], ['tangent', 'مماس'], ['arc', 'کمان']],
    'axcylinder:X.kind': [['cylinder', 'استوانه'], ['cone', 'مخروط']],
    'axlinear:X.show': [['all', 'معادله‌ها (بالای شبکه) + نقطهٔ برخورد'], ['eq', 'فقط معادله‌ها بالای شبکه'], ['none', 'فقط خطوط روی صفحهٔ مختصات']],
    'axbarchart:X.show': [['mean', 'میانگین (x̄)'], ['median', 'میانه'], ['mode', 'مد (نما) و دامنهٔ تغییرات'], ['all', 'همهٔ شاخص‌ها (میانگین، میانه، مد، σ)'], ['none', 'بدون شاخص زیر نمودار']],
    'axpendulum:X.mode': [['period', 'دورهٔ نوسان و مسیر حرکت'], ['forces', 'تجزیهٔ نیروها (mg ، T ، mg·sinθ)'], ['energy', 'تبدیل انرژی و اوج ارتفاع h'], ['quiz', 'پرسشی (بدون فرمول زیر شکل)']],
    'axincline:X.show': [['all', 'بردار نیروها + محاسبات شتاب'], ['forces', 'فقط بردار نیروها (بدون عدد)'], ['plain', 'فقط شکل جسم روی سطح شیب‌دار']],
    'axcircuit:X.mode': [['series', 'متوالی (سری)'], ['parallel', 'موازی'], ['mixed', 'ترکیبی (R₁ سری با R₂∥R₃)']],
    'axcircuit:X.show': [['all', 'نمایش مقاومت معادل، جریان و توان'], ['given', 'فقط مقادیر داده‌شده روی مدار']],
    'axoptics:X.kind': [['convex', 'عدسی محدب (همگرا)'], ['concave', 'عدسی مقعر (واگرا)']],
    'axoptics:X.show': [['all', 'پرتوها + تصویر + فرمول'], ['rays', 'فقط مسیر پرتوها'], ['blank', 'فقط جسم و عدسی (برای رسم دانش‌آموز)']],
    'axheatcurve:X.mode': [['full', 'کامل (نام مراحل + گرمای کل Q)'], ['phases', 'فقط نام فازها'], ['quiz', 'پرسشی — مراحل (۱) تا (۵)']],
    'axdna:X.mode': [['helix', 'مارپیچ دوگانهٔ DNA'], ['flat', 'نردبانی جفت‌بازها (پیوند هیدروژنی)'], ['mrna', 'رونویسی DNA به mRNA'], ['quiz', 'پرسش رشتهٔ مکمل (خانه‌های خالی)']],
    'axdna:X.label': [['1', 'نمایش پیوندها و درصد GC'], ['0', 'بدون توضیح زیر شکل']],
    'axcell:X.kind': [['animal', 'سلول جانوری'], ['plant', 'سلول گیاهی']],
    'axmapgeo:X.kind': [['compass', 'گل‌آویز جهت‌ها'], ['map', 'نقشه و مقیاس']],
    'axsentence:X.mode': [['verbal', 'جملهٔ فعلیه'], ['nominal', 'جملهٔ اسمیه']],
    'axvectors:X.mode': [['sum', 'جمع a + b'], ['diff', 'تفاضل a − b']],
    'axintegral:X.f': [['quad', 'f(x) = ¼x²'], ['sin', 'f(x) = sin x'], ['line', 'f(x) = ۰٫۶x + ۰٫۴']],
    'axstack:X.mode': [['stack', 'پشته (LIFO)'], ['queue', 'صف (FIFO)']],
    'axstack:X.op': [['push', 'افزودن'], ['pop', 'حذف']],
    'axfrac:X.shape': [['circle', 'دایره‌ای (قطاع)'], ['ring', 'حلقه‌ای'], ['bar', 'نواری'], ['square', 'مربعی (شبکه)']],
    'axsaw:X.size': [
      ['3', '۳×۳ (پایهٔ اول — ۳ خانه)'],
      ['4', '۴×۴ (استاندارد ابتدایی — بلوک ۲×۲)'],
      ['5', '۵×۵ (پنج‌تایی)'],
      ['6', '۶×۶ (پیشرفته — بلوک ۲×۳)'],
      ['7', '۷×۷ (هفت‌تایی)'],
      ['8', '۸×۸ (هشت‌تایی)'],
      ['9', '۹×۹ (سودوکوی کامل — بلوک ۳×۳)']
    ],
    'axsaw:X.mode': [
      ['color', 'رنگی — ویژهٔ اول ابتدایی (خانه‌های رنگی)'],
      ['shape', 'شکلی (● ▲ ■ ★ ◆ ♥ ⬢ ✿ ▼)'],
      ['color_shape', 'رنگ و شکل با هم (خانهٔ رنگی + شکل)'],
      ['num', 'عددی (۱ تا ۹)']
    ],
    'axpattern:X.mode': [
      ['shape', 'الگوی شکلی (● ▲ ■ ★ ◆ ♥)'],
      ['color', 'الگوی رنگ‌آمیزی (خانه‌های رنگی)'],
      ['bead', 'الگوی نخ و مهره‌های رنگی'],
      ['number', 'الگوی عددی (حسابی / هندسی / فیبوناچی)'],
      ['growing', 'الگوی رشدشونده (افزایشی نقطه و شکل)'],
      ['mixed', 'الگوی ترکیبی (رنگ، شکل و عدد)']
    ],
    'axpattern:X.seq': [
      ['●,▲,●,▲,●,?', 'تکراری دوتایی شکلی (● ، ▲ ، ● ، ▲ ، ؟)'],
      ['●,▲,■,●,▲,?', 'تکراری سه‌تایی شکلی (● ، ▲ ، ■ ، ● ، ▲ ، ؟)'],
      ['●,●,▲,●,●,?', 'تکراری دوبه‌یک (● ، ● ، ▲ ، ● ، ● ، ؟)'],
      ['●,▲,▲,●,▲,▲,?', 'تکراری یک‌به‌دو (● ، ▲ ، ▲ ، ● ، ▲ ، ▲ ، ؟)'],
      ['★,◆,♥,★,◆,?', 'ستاره، لوزی و قلب (★ ، ◆ ، ♥ ، ★ ، ◆ ، ؟)'],
      ['قرمز,آبی,قرمز,آبی,قرمز,?', 'تکراری رنگی دوتایی (قرمز ، آبی ، ...)'],
      ['قرمز,آبی,سبز,قرمز,آبی,?', 'تکراری رنگی سه‌تایی (قرمز ، آبی ، سبز ، ...)'],
      ['قرمز,زرد,زرد,قرمز,زرد,?', 'تکراری رنگی یک‌به‌دو (قرمز ، زرد ، زرد ، ...)'],
      ['2,4,6,8,10,?', 'عددی زوج (+۲): ۲ ، ۴ ، ۶ ، ۸ ، ۱۰ ، ؟'],
      ['1,3,5,7,9,?', 'عددی فرد (+۲): ۱ ، ۳ ، ۵ ، ۷ ، ۹ ، ؟'],
      ['5,10,15,20,25,?', 'شمارش پنج‌تاپنج‌تا: ۵ ، ۱۰ ، ۱۵ ، ۲۰ ، ۲۵ ، ؟'],
      ['10,20,30,40,?', 'شمارش ده‌تاده‌تا: ۱۰ ، ۲۰ ، ۳۰ ، ۴۰ ، ؟'],
      ['3,6,9,12,15,?', 'مضرب‌های ۳: ۳ ، ۶ ، ۹ ، ۱۲ ، ۱۵ ، ؟'],
      ['20,17,14,11,?', 'عددی کاهشی (−۳): ۲۰ ، ۱۷ ، ۱۴ ، ۱۱ ، ؟'],
      ['1,2,4,8,16,?', 'دو برابر شونده (×۲): ۱ ، ۲ ، ۴ ، ۸ ، ۱۶ ، ؟'],
      ['1,1,2,3,5,8,?', 'الگوی فیبوناچی: ۱ ، ۱ ، ۲ ، ۳ ، ۵ ، ۸ ، ؟'],
      ['1,3,6,10,?', 'الگوی مثلثی (رشدشونده): ۱ ، ۳ ، ۶ ، ۱۰ ، ؟'],
      ['1,4,9,16,?', 'الگوی مربعی (رشدشونده): ۱ ، ۴ ، ۹ ، ۱۶ ، ؟']
    ],
    'axjump:X.op': [
      ['+', 'جمع روی محور (+)'],
      ['-', 'تفریق روی محور (−)'],
      ['×', 'ضرب روی محور (×)'],
      ['÷', 'تقسیم روی محور (÷)']
    ],
    'axbeaker:X.count': [
      ['1', '۱ استوانهٔ مدرج'],
      ['2', '۲ استوانهٔ مدرج'],
      ['3', '۳ استوانهٔ مدرج'],
      ['4', '۴ استوانهٔ مدرج']
    ],
    'axsyllablechart:X.mode': [
      ['ask', 'پرسش از دانش‌آموز (نمایش کلمه بدون تفکیک هجا)'],
      ['show', 'نمایش آموزشی بخش‌ها (جداشده با خط تیره -)']
    ],
    'axmagnet:X.mode': [
      ['attract', 'جذب — قطب‌های ناهم‌نام روبه‌روی هم (N و S)'],
      ['repel', 'دفع — دو قطب هم‌نام شمال روبه‌روی هم (N و N)'],
      ['repel_s', 'دفع — دو قطب هم‌نام جنوب روبه‌روی هم (S و S)']
    ],
    'axnumgrid:X.cols': [['10', '۱۰ ستون (جدول صدتایی)'], ['5', '۵ ستون'], ['6', '۶ ستون'], ['8', '۸ ستون']],
    'axnumgrid:X.rows': [['10', '۱۰ سطر (جدول صدتایی)'], ['5', '۵ سطر (۵۰تایی)'], ['6', '۶ سطر'], ['8', '۸ سطر']],
    'axnumgrid:X.rule': [
      ['0', 'بدون رنگ‌آمیزی الگو'],
      ['2', 'مضرب‌های ۲ (شمارش دو تا دو تا)'],
      ['3', 'مضرب‌های ۳ (شمارش سه تا سه تا)'],
      ['5', 'مضرب‌های ۵ (شمارش پنج تا پنج تا)'],
      ['10', 'مضرب‌های ۱۰ (شمارش ده تا ده تا)'],
      ['prime', 'اعداد اول (غربال اراتستن)'],
      ['odd', 'اعداد فرد']
    ],
    'axfacttree:X.blank': [['0', 'نمایش کامل همهٔ عوامل'], ['1', 'جای خالی عوامل اول انتهایی (؟)'], ['2', 'جای خالی شاخه‌های میانی (؟)']],
    'axfnmachine:X.op1': [['+ 3', '+ 3 (جمع با ۳)'], ['+ 5', '+ 5 (جمع با ۵)'], ['- 2', '- 2 (تفریق ۲)'], ['× 2', '× 2 (ضرب در ۲)'], ['× 3', '× 3 (ضرب در ۳)'], ['÷ 2', '÷ 2 (تقسیم بر ۲)']],
    'axfnmachine:X.op2': [['', 'ندارد (ماشین یک‌مرحله‌ای)'], ['+ 1', '+ 1'], ['- 1', '- 1'], ['× 2', '× 2'], ['+ 5', '+ 5']],
    'axfnmachine:X.blank': [['1', 'جای خالی در خروجی‌ها (؟)'], ['2', 'جای خالی قانون ماشین (؟)'], ['0', 'نمایش کامل ورودی و خروجی']],
    'axfracwall:X.mode': [['label', 'نمایش کسر و رنگ'], ['color', 'فقط رنگ‌آمیزی (بدون نوشته)'], ['blank', 'جای خالی کسرهای مساوی (؟)']],
    'axtangram:X.shape': [['square', 'مربع ۷ تکه (کلاسیک)'], ['house', 'خانه'], ['boat', 'قایق'], ['cat', 'گربه']],
    'axtangram:X.mode': [['color', 'رنگی کامل'], ['num', 'رنگی با شمارهٔ قطعات (۱ تا ۷)'], ['outline', 'سفید خط‌چین (برای رنگ‌آمیزی)'], ['blank', 'جای خالی شمارهٔ قطعات (؟)']],
    'axangle:X.pro': [['1', 'همراه با نقالهٔ مدرج'], ['0', 'فقط دو ضلع و کمان زاویه']],
    'axangle:X.label': [['1', 'نمایش درجه و نوع زاویه'], ['2', 'نمایش با جای خالی (؟°)'], ['0', 'بدون برچسب']],
    'axrod:X.blank': [['0', 'نمایش طول میله‌ها'], ['1', 'طول با جای خالی (؟)']],
    'axtower:X.op': [['+', 'جمع (+)'], ['-', 'تفریق (−)']],
    'axtower:X.blank': [['0', 'کامل'], ['1', 'با جای خالی (؟)']],
    'axmulwheel:X.op': [['×', 'ضرب (×)'], ['+', 'جمع (+)']],
    'axmulwheel:X.hide': [['0', 'کامل'], ['1', 'با جای خالی (؟)']],
    'axmultgrid:X.blank': [['0', 'کامل'], ['1', 'خانه‌های خالی (؟)'], ['2', 'سرستون‌های خالی (؟)']],
    'axnet:X.shape': [['cube', 'مکعب (۶ وجه)'], ['cuboid', 'مکعب‌مستطیل'], ['pyramid', 'هرم (۵ وجه)'], ['prism', 'منشور (۵ وجه)']],
    'axnet:X.mode': [['color', 'رنگی کامل'], ['num', 'شمارهٔ وجه‌ها'], ['outline', 'سفید برای رنگ‌آمیزی'], ['blank', 'جای خالی (؟)']],
    'axcubes:X.view': [['iso', 'نمای سه‌بعدی (ایزومتریک)'], ['top', 'نمای از بالا'], ['front', 'نمای روبه‌رو'], ['side', 'نمای از پهلو']],
    'axcubes:X.hide': [['0', 'نمایش تعداد'], ['1', 'تعداد با جای خالی (؟)']],
    'axcalendar:X.start': [['0', 'شنبه'], ['1', 'یکشنبه'], ['2', 'دوشنبه'], ['3', 'سه‌شنبه'], ['4', 'چهارشنبه'], ['5', 'پنجشنبه'], ['6', 'جمعه']],
    'axcalendar:X.blank': [['0', 'همهٔ روزها'], ['1', 'چند خانه جای خالی (؟)']],
    'axlever:X.kind': [['lever1', 'اهرم نوع اول'], ['lever2', 'اهرم نوع دوم'], ['lever3', 'اهرم نوع سوم'], ['pulley', 'قرقرهٔ متحرک'], ['gears', 'چرخ‌دنده']],
    'axcycle:X.kind': [['water', 'چرخهٔ آب'], ['plant', 'مراحل رشد گیاه'], ['frog', 'دگردیسی قورباغه'], ['food', 'زنجیرهٔ غذایی']],
    'axwordtrain:X.mode': [['join', 'نمایش کلمه'], ['blank', 'کلمه با جای خالی (؟)']],
    'axtiles:X.neg': [['0', 'مثبت (+ آبی/سبز/نارنجی)'], ['1', 'منفی (− قرمز)']],
    'axtiles:X.label': [['1', 'مدل کاشی بالا + عبارت جبری پایین'], ['2', 'عبارت جبری با جای خالی (؟)'], ['0', 'بدون نوشته']],
    'axkenken:X.kind': [['kenken', 'کن‌کن (قفس‌های عملیاتی)'], ['kakuro', 'کاکورو (جمع سطری و ستونی)']],
    'axkenken:X.size': [['3', '۳×۳'], ['4', '۴×۴'], ['5', '۵×۵'], ['6', '۶×۶']],
    'axdicefold:X.net': [['cross', 'صلیبی استاندارد'], ['t', 'الگوی T'], ['z', 'الگوی پلکانی (زیگزاگ)'], ['l', 'الگوی L']],
    'axdicefold:X.mode': [['dots', 'نقطه‌های تاس'], ['num', 'اعداد ۱ تا ۶']],
    'axdicefold:X.top': [['1', '1'], ['2', '2'], ['3', '3'], ['4', '4'], ['5', '5'], ['6', '6']],
    'axdicefold:X.ask': [['0', 'نمایش همهٔ وجه‌ها'], ['1', 'خالی کردن دو وجه برای دانش‌آموز']],
    'axtransform:X.shape': [['tri', 'مثلث'], ['sq', 'مربع'], ['rect', 'مستطیل']],
    'axtransform:X.op': [['translate', 'انتقال (بردار)'], ['rotate', 'دوران'], ['reflect', 'بازتاب']],
    'axtransform:X.axis': [['x', 'نسبت به محور x'], ['y', 'نسبت به محور y']],
    'axcaliper:X.kind': [['vernier', 'کولیس ورنیه (دقت ۰٫۱ mm)'], ['vernier02', 'کولیس ورنیه (دقت ۰٫۰۲ mm)'], ['micrometer', 'ریزسنج / میکرومتر (دقت ۰٫۰۱ mm)']],
    'axcaliper:X.label': [['1', 'نمایش خوانش دقیق زیر شکل'], ['2', 'پرسش خوانش ابزار (؟ mm)'], ['0', 'بدون برچسب زیر شکل']],
    'axsigntable:X.lead': [['1', 'مثبت (۱)'], ['-1', 'منفی (-۱)']],
    'axtrigcircle:X.show': [['all', 'تصویر sin/cos + خط مماس tan + رادیان'], ['1', 'تصویر sin و cos و مقادیر'], ['0', 'فقط زاویه روی دایرهٔ مثلثاتی']],
    'axmapping:X.label': [['1', 'تشخیص خودکار تابع، یک‌به‌یک و پوشا'], ['2', 'پرسش نوع رابطه از دانش‌آموز (؟)'], ['0', 'بدون برچسب زیر نمودار']],
    'axprobtree:X.stages': [['2', '۲ مرحله‌ای'], ['3', '۳ مرحله‌ای'], ['4', '۴ مرحله‌ای']],
    'axprobtree:X.mode': [['prob', 'نمایش احتمال هر پیامد'], ['frac', 'نمایش پیشامد انتهایی'], ['none', 'بدون ستون انتهایی']],
    'axconic:X.kind': [['ellipse', 'بیضی'], ['hyperbola', 'هذلولی'], ['parabola', 'سهمی']],
    'axorbital:X.mode': [['box', 'آرایش اوربیتالی (هوند و آفبا تا 5p)'], ['gas', 'آرایش فشرده با گاز نجیب ([Ar] / [Kr])'], ['lewis', 'ساختار نقطه‌ای لوئیس']],
    'axpunnett:X.kind': [['punnett', 'مربع پانت (تک‌رگه ۲×۲ یا دورگه ۴×۴)'], ['pedigree', 'شجره‌نامهٔ ژنتیکی']],
    'axpunnett:X.gens': [['2', '۲ نسل (I تا II)'], ['3', '۳ نسل (I تا III)'], ['4', '۴ نسل (I تا IV)']],
    'axpunnett:X.mode': [['genotype', 'نمایش کامل ژنوتیپ‌ها'], ['blank', 'چند خانه خالی برای پاسخ دانش‌آموز']],
    'axecg:X.kind': [['ecg', 'نوار قلب (ECG)'], ['shm', 'نوسان هماهنگ ساده']],
    'axecg:X.label': [['1', 'با برچسب P و QRS و T'], ['0', 'بدون برچسب']],
    'axecg:X.n': [['1', '۱ ضربان'], ['2', '۲ ضربان'], ['3', '۳ ضربان']],
    'axprosody:X.mode': [['arkan', 'نام ارکان + تقطیع هجاها + بحر'], ['taqti', 'فقط خط و نیم‌دایره (∪ و −)'], ['blank', 'جای خالی نام ارکان و بحر (پرسشی)']],
    'axtruthtable:X.kind': [['truth', 'جدول ارزش گزاره‌ها'], ['square', 'مربع تقابل ارسطویی']],
    'axstaff:X.kind': [['staff', 'پنج‌خط حامل موسیقی'], ['color', 'چرخهٔ ۱۲ رنگ ایتن']],
    'axstaff:X.clef': [['treble', 'کلید سل (𝄞)'], ['bass', 'کلید فا (𝄢)']],
    'axstaff:X.time': [['4/4', '۴/۴'], ['3/4', '۳/۴'], ['2/4', '۲/۴'], ['6/8', '۶/۸'], ['none', 'بدون کسر میزان']],
    'axstaff:X.harmony': [['triad', 'مثلث رنگ‌های اصلی'], ['comp', 'قطر رنگ‌های مکمل'], ['analog', 'رنگ‌های هم‌جوار'], ['warmcool', 'مرز رنگ‌های گرم و سرد']],
    'axstaff:X.label': [['1', 'نمایش نام نت‌ها'], ['0', 'بدون نام نت (پرسش نت‌خوانی)']],
    'axkmap:X.vars': [['2', '۲ متغیره'], ['3', '۳ متغیره'], ['4', '۴ متغیره']],
    'axgate:X.kind': [['and', 'AND (و)'], ['or', 'OR (یا)'], ['not', 'NOT (نقیض)'], ['nand', 'NAND'], ['nor', 'NOR'], ['xor', 'XOR']],
    'axgate:X.a': [['0', '۰'], ['1', '۱']],
    'axgate:X.b': [['0', '۰'], ['1', '۱']],
    'axbst:X.mode': [['bst', 'درخت جستجوی دودویی'], ['heap', 'درخت هرم (آرایه‌ای)']],
    'axbode:X.kind': [['bode', 'نمودار بود (اندازه و فاز)'], ['nyquist', 'نمودار نایکوئیست']],
    'axbeam:X.load': [['point', 'بار متمرکز'], ['udl', 'بار گسترده']],
    'axpv:X.kind': [['carnot', 'چرخهٔ کارنو'], ['otto', 'چرخهٔ اتو'], ['diesel', 'چرخهٔ دیزل'], ['rankine', 'چرخهٔ رانکین']],
    'axbenzene:X.kind': [['kekule', 'بنزن (ککوله)'], ['chair', 'صندلی سیکلوهگزان'], ['newman', 'تصویر نیومن'], ['fischer', 'تصویر فیشر']],
    'axbenzene:X.mode': [['label', 'پیوندهای دوگانه'], ['circle', 'حلقهٔ آروماتیک']],
    'axsupply:X.kind': [['equilibrium', 'تعادل بازار'], ['shift', 'افزایش تقاضا'], ['ceiling', 'سقف قیمت']],
    'axgame:X.kind': [['game', 'ماتریس بازی ۲×۲'], ['islm', 'مدل IS-LM']],
    'axsym:X.axis': [['v', 'عمودی (چپ و راست)'], ['h', 'افقی (بالا و پایین)']],
    'axsym:X.mirror': [['1', 'نمایش کامل شکل قرینه'], ['0', 'عدم نمایش کامل شکل قرینه']],
    'axplace:X.op2': [['', 'ندارد'], ['+', 'جمع (+)'], ['-', 'تفریق (−)']],
    'axplace:X.op3': [['', 'ندارد'], ['+', 'جمع (+)'], ['-', 'تفریق (−)']],
    'axplace:X.rows': [['1', '۱ ردیف'], ['2', '۲ ردیف'], ['3', '۳ ردیف'], ['4', '۴ ردیف']],
    'axplace:X.blank': [['0', 'نمایش عددها'], ['1', 'جای خالی (برای طرح سؤال)']],
    'axabacus:X.blank': [['0', 'نمایش عددها'], ['1', 'جای خالی (برای طرح سؤال)']],
    'axmularea:X.blank': [['0', 'نمایش عددها'], ['1', 'جای خالی (برای طرح سؤال)']],
    'axabacus:X.cols': [['2', '۲ ستون (یکان تا دهگان)'], ['3', '۳ ستون (تا صدگان)'], ['4', '۴ ستون (تا یکان هزار)'], ['5', '۵ ستون (تا دهگان هزار)'], ['6', '۶ ستون کامل (یکان تا صدگان هزار)']],
    'axclock:X.nums': [['1', 'نمایش عددهای ۱ تا ۱۲'], ['0', 'بدون عدد (تمرین ساعت)']],
    'axclock:X.label': [['0', 'ندارد'], ['1', 'نمایش ساعت زیر شکل'], ['2', 'نمایش با جای خالی (ساعت = ؟ : ؟)']],
    'axfrac:X.label': [['1', 'نمایش کسر زیر شکل'], ['2', 'نمایش با جای خالی (؟ / ؟)'], ['0', 'ندارد']],
    'axtally:X.label': [['1', 'نمایش عدد زیر شکل'], ['2', 'نمایش با جای خالی (؟) برای سؤال'], ['0', 'ندارد']],
    'axbase10:X.label': [['1', 'نمایش عدد زیر شکل'], ['2', 'نمایش با جای خالی (؟) برای سؤال'], ['0', 'ندارد']],
    'axruler:X.label': [['1', 'نمایش طول بازه (cm)'], ['2', 'نمایش با جای خالی (؟ cm)'], ['0', 'ندارد']],
    'axbalance:X.label': [['1', 'نمایش علامت مقایسه (> / < / =)'], ['2', 'نمایش با جای خالی (؟) برای سؤال'], ['0', 'ندارد']],
    'axtenframe:X.label': [['1', 'نمایش عبارت کامل'], ['2', 'نمایش با جای خالی (= ؟)'], ['0', 'ندارد']],
    'axjump:X.label': [['1', 'نمایش عبارت کامل'], ['2', 'نمایش با جای خالی (= ؟)'], ['0', 'ندارد']],
    'axmoney:X.unit': [['تومان', 'تومان'], ['ریال', 'ریال']],
    'axmoney:X.sum': [['1', 'نمایش جمع کل'], ['2', 'نمایش با جای خالی (جمع: ؟)'], ['0', 'مخفی (بدون جمع)']],
    'axbalance:X.unit': [['کیلوگرم', 'کیلوگرم'], ['گرم', 'گرم']],
    'axbeaker:X.unit': [['mL', 'میلی‌لیتر (mL)'], ['لیتر', 'لیتر'], ['سی‌سی', 'سی‌سی (cc)']],
    'axtenframe:X.frames': [['2', '۲ قاب (۲۰تایی)'], ['1', '۱ قاب (۱۰تایی)']],
    'axgeoboard:X.grid': [['3', '۳×۳'], ['4', '۴×۴'], ['5', '۵×۵ (استاندارد)'], ['6', '۶×۶'], ['7', '۷×۷']],
    'axdice:X.sum': [['1', 'نمایش جمع کامل (۴ + ۶ = ۱۰)'], ['2', 'نمایش با جای خالی (۴ + ۶ = ؟)'], ['0', 'فقط نمایش تاس‌ها']],
    'axnumint:X.lc': [['1', 'بسته [ (نقطهٔ توپر)'], ['0', 'باز ( (نقطهٔ توخالی)']],
    'axnumint:X.hc': [['0', 'باز ) (نقطهٔ توخالی)'], ['1', 'بسته ] (نقطهٔ توپر)']],
    'axnumineq:X.dir': [['1', 'به سمت راست (x > یا x ≥)'], ['-1', 'به سمت چپ (x < یا x ≤)']],
    'axnumineq:X.cl': [['0', 'باز (توخالی: > یا <)'], ['1', 'بسته (توپر: ≥ یا ≤)']],
    'axnumfrac:X.den': [['2', '۲ (نیم)'], ['3', '۳ (ثلث)'], ['4', '۴ (ربع)'], ['5', '۵ (خمس)'], ['6', '۶ (سدس)'], ['8', '۸'], ['10', '۱۰ (دهم)']],
    'venn:X.n': [['2', '۲ مجموعه (A و B)'], ['3', '۳ مجموعه (A، B و C)']]
  };
  function renderFields() {
    gfDdClose();
    var host = $('grFields');
    if (!host) return;
    var extraHtml = '';
    if (state.t === 'axsaw') {
      extraHtml += '<div class="gf-hint-box">💡 روی هر خانهٔ جدول در پیش‌نمایش کلیک کنید تا مقدار، علامت سؤال (؟) یا خانهٔ خالی را از پنجره انتخاب کنید.</div>';
    } else if (state.t === 'axpattern') {
      extraHtml += '<div class="gf-hint-box">💡 از منوی «الگوی آماده» انتخاب کنید یا روی هر عضو شکل کلیک کنید.<div class="gf-mini-btns">' +
        '<button type="button" class="gf-mini-btn" data-pact="add">+ افزودن خانه به الگو</button>' +
        '<button type="button" class="gf-mini-btn" data-pact="rem">− حذف آخرین خانه</button></div></div>';
    } else if (state.t === 'axsym') {
      extraHtml += '<div class="gf-hint-box">💡 روی هر خانهٔ شطرنجی کلیک کنید تا رنگ یا پاک شود.<div class="gf-mini-btns">' +
        '<button type="button" class="gf-mini-btn" data-pact="sym-clear">پاک‌کردن همهٔ خانه‌ها</button></div></div>';
    } else if (state.t === 'axpaintgrid') {
      extraHtml += '<div class="gf-hint-box">💡 روی هر خانهٔ شبکه کلیک کنید تا رنگ یا پاک شود.<div class="gf-mini-btns">' +
        '<button type="button" class="gf-mini-btn" data-pact="pg-clear">پاک‌کردن خانه‌های رنگ‌شده</button></div></div>';
    } else if (state.t === 'axtiles') {
      extraHtml += '<div class="gf-hint-box">💡 روی بخش چپ (x²)، وسط (x) یا راست (۱) در پیش‌نمایش کلیک کنید تا تعداد کاشی‌ها تغییر کند.</div>';
    } else if (state.t === 'axcalendar' || state.t === 'axmultgrid' || state.t === 'axmulwheel' || state.t === 'axtower' || state.t === 'axdicefold' || state.t === 'axunitconv' || state.t === 'axprobtree' || state.t === 'axorbital' || state.t === 'axpunnett' || state.t === 'axkenken') {
      extraHtml += '<div class="gf-hint-box">💡 روی هر خانه در شکل کلیک کنید تا عدد دلخواه بنویسید یا آن را بدون علامت سؤال خالی بگذارید.' +
        ((state.X.ov || state.X.empty) ? '<div class="gf-mini-btns"><button type="button" class="gf-mini-btn" data-pact="ov-clear">پاک‌کردن تنظیمات دستی خانه‌ها</button></div>' : '') + '</div>';
    } else if (state.t === 'axcubes') {
      var cbRows = String(state.X.map || '2,1,3;1,0,1;2,2,1').split(/[;؛]/).map(function (rw) {
        return rw.split(/[,،]/).map(function (q2) { var iv = parseInt(faEn(q2.trim()), 10); return Math.max(0, Math.min(5, isFinite(iv) ? iv : 0)); });
      }).filter(function (rw) { return rw.length; }).slice(0, 4);
      if (!cbRows.length) cbRows = [[2, 1, 3], [1, 0, 1], [2, 2, 1]];
      var crN = cbRows.length, ccM = Math.max(1, cbRows[0].length), curDim = crN + 'x' + ccM;
      var dims = [['2x2', '۲×۲'], ['3x3', '۳×۳'], ['4x4', '۴×۴'], ['2x3', '۲×۳'], ['3x2', '۳×۲']];
      extraHtml += '<div class="gf-cubes-ed"><div class="gf-cubes-hd"><span>تعداد مکعب در هر ستون (کلیک = تغییر ۰ تا ۵):</span>' +
        '<div class="gf-mini-btns">' + dims.map(function (dm) {
          return '<button type="button" class="gf-mini-btn' + (curDim === dm[0] ? ' on' : '') + '" data-cdim="' + dm[0] + '">' + dm[1] + '</button>';
        }).join('') + '</div></div><div class="gf-cubes-grid" style="grid-template-columns:repeat(' + ccM + ',1fr)">';
      for (var rci = 0; rci < crN; rci++) {
        for (var cci = 0; cci < ccM; cci++) {
          var hv = (cbRows[rci] && cbRows[rci][cci] != null) ? cbRows[rci][cci] : 0;
          extraHtml += '<button type="button" class="gf-cube-cell' + (hv > 0 ? ' on' : '') + '" data-cr="' + rci + '" data-cc="' + cci + '">' + hv + '</button>';
        }
      }
      extraHtml += '</div><div class="gf-mini-btns"><button type="button" class="gf-mini-btn" data-pact="cubes-zero">صفر کردن همهٔ ستون‌ها</button></div></div>';
    }
    host.innerHTML = '<h4>داده‌ها و بازه</h4><div class="gf-grid">' + fieldsFor(state.t).map(function (r) {
      var v = getPath(state, r[0]);
      var ddOpts = GF_DROPDOWNS[state.t + ':' + r[0]];
      if (r[0] === 'X.shape' || ddOpts) {
        var baseOpts = ddOpts || [['circle', 'دایره‌ای (قطاع)'], ['ring', 'حلقه‌ای'], ['bar', 'نواری'], ['square', 'مربعی (شبکه)']];
        var cur = (v != null && v !== '') ? String(v) : baseOpts[0][0];
        var opts = baseOpts.slice();
        if (cur && !opts.some(function (o) { return o[0] === cur; })) {
          opts.unshift([cur, 'الگوی سفارشی (' + cur + ')']);
        }
        var selTag = r[0] === 'X.shape' ? '<select data-k="X.shape" class="gf-sel">' : '<select data-k="' + r[0] + '" class="gf-sel">';
        return '<div class="gf-field gf-span"><span class="gf-lbl">' + esc(r[1]) + '</span>' + selTag + opts.map(function (o) {
          return '<option value="' + esc(o[0]) + '"' + (cur === o[0] ? ' selected' : '') + '>' + esc(o[1]) + '</option>';
        }).join('') + '</select></div>';
      }
      var wide = /labs|vals|title|xs|ys|zs|rows|cols|mins|q1s|meds|q3s|maxs|opens|highs|lows|closes|cells|pts|seq|parts|dice|left|right|mid|in|syll/.test(r[0]);
      return '<label class="' + (wide ? 'gf-span' : '') + '">' + esc(r[1]) +
        '<input data-k="' + r[0] + '" value="' + esc(v == null ? '' : v) + '"></label>';
    }).join('') + '</div>' + extraHtml;
    host.querySelectorAll('input,select').forEach(function (inp) {
      inp.addEventListener(inp.tagName === 'SELECT' ? 'change' : 'input', function () {
        var dk = inp.getAttribute('data-k');
        setPath(state, dk, inp.value);
        paint();
        if (dk === 'X.count') renderFields();
      });
    });
    host.querySelectorAll('[data-pact]').forEach(function (btn) {
      btn.onclick = function () {
        var act = btn.getAttribute('data-pact');
        if (act === 'add') {
          var items = String(state.X.seq || '●,▲,●,▲,●,?').split(/[,،]/).map(function (z) { return z.trim(); }).filter(Boolean);
          if (items.length < 12) { items.push('?'); state.X.seq = items.join(','); paint(); renderFields(); }
        } else if (act === 'rem') {
          var items2 = String(state.X.seq || '●,▲,●,▲,●,?').split(/[,،]/).map(function (z) { return z.trim(); }).filter(Boolean);
          if (items2.length > 2) { items2.pop(); state.X.seq = items2.join(','); paint(); renderFields(); }
        } else if (act === 'sym-clear') {
          state.X.cells = ''; paint(); renderFields();
        } else if (act === 'pg-clear') {
          state.X.cells = ''; paint(); renderFields();
        } else if (act === 'ov-clear') {
          delete state.X.ov; delete state.X.empty; paint(); renderFields();
        } else if (act === 'cubes-zero') {
          var curM = String(state.X.map || '2,1,3;1,0,1;2,2,1').split(/[;؛]/);
          var rn = Math.max(2, Math.min(4, curM.length)), cn = Math.max(2, Math.min(4, (curM[0] || '').split(/[,،]/).length));
          var zRows = [];
          for (var zr = 0; zr < rn; zr++) { var zc = []; for (var zk = 0; zk < cn; zk++) zc.push('0'); zRows.push(zc.join(',')); }
          state.X.map = zRows.join(';'); paint(); renderFields();
        }
      };
    });
    host.querySelectorAll('[data-cdim]').forEach(function (btn) {
      btn.onclick = function () {
        var sp = btn.getAttribute('data-cdim').split('x'), nr = +sp[0] || 3, nc = +sp[1] || 3;
        var oldR = String(state.X.map || '2,1,3;1,0,1;2,2,1').split(/[;؛]/).map(function (rw) { return rw.split(/[,،]/).map(function (q2) { return parseInt(faEn(q2.trim()), 10) || 0; }); });
        var nextR = [];
        for (var r0 = 0; r0 < nr; r0++) {
          var rowA = [];
          for (var c0 = 0; c0 < nc; c0++) rowA.push((oldR[r0] && oldR[r0][c0] != null) ? oldR[r0][c0] : 1);
          nextR.push(rowA.join(','));
        }
        state.X.map = nextR.join(';'); paint(); renderFields();
      };
    });
    host.querySelectorAll('.gf-cube-cell').forEach(function (btn) {
      btn.onclick = function () {
        var rr = +btn.getAttribute('data-cr'), cc = +btn.getAttribute('data-cc');
        var rowsA = String(state.X.map || '2,1,3;1,0,1;2,2,1').split(/[;؛]/).map(function (rw) { return rw.split(/[,،]/).map(function (q2) { var iv = parseInt(faEn(q2.trim()), 10); return Math.max(0, Math.min(5, isFinite(iv) ? iv : 0)); }); });
        if (rowsA[rr] && rowsA[rr][cc] != null) {
          rowsA[rr][cc] = (rowsA[rr][cc] + 1) % 6;
          state.X.map = rowsA.map(function (rw) { return rw.join(','); }).join(';');
          paint(); renderFields();
        }
      };
    });
    host.querySelectorAll('select.gf-sel,select.gf-shape').forEach(gfDdAttach);
  }
  function setType(id) {
    var prev = state;
    if (isAxisType(id)) {
      // V206 — محور: فقط کلیدهای همان محور از حالت قبلی می‌ماند (ویرایش توکن مقدارها را حفظ می‌کند).
      state = defAxis(id);
      Object.keys(state.X).forEach(function (k) {
        if (prev.X && prev.X[k] != null && prev.X[k] !== '') state.X[k] = prev.X[k];
      });
    } else {
      state = def(id);
      Object.keys(prev.X || {}).forEach(function (k) {
        if (prev.X[k] != null && prev.X[k] !== '') state.X[k] = prev.X[k];
      });
    }
    state.t = id; state.k = 'g';
    document.querySelectorAll('#grShapes .gf-shape').forEach(function (b) {
      b.classList.toggle('on', b.getAttribute('data-t') === id);
    });
    renderFields(); paint();
    try {
      var onG = document.querySelector('#grShapes .gf-shape.on');
      if (onG && onG.scrollIntoView) onG.scrollIntoView({ block: 'nearest' });
    } catch (e) {}
  }
  // V206 — فهرست نوع‌ها: نمودارها یا (در حالت محور) ۶ محور پایه.
  // V310 — «ابزار ریاضی ابتدایی» آیکن جدا دارد (حالت math)؛ فهرست محور فقط محورهای پایه را نشان می‌دهد.
  function mathSplit() { for (var i = 0; i < AXIS_TYPES.length; i++) if (AXIS_TYPES[i].id === PRIMARY_MATH_FIRST_ID) return i; return AXIS_TYPES.length; }
  function isMathType(t) { var sp = mathSplit(); for (var i = 0; i < AXIS_TYPES.length; i++) if (AXIS_TYPES[i].id === t) return i >= sp; return false; }
  // V320 — دسته‌بندی ابزارهای ریاضی (هم‌تراز با MathToolCatalog.kt)
  var MATH_CATS = [
    { id: 'elementary', name: '🧮 دبستان', hint: 'پایهٔ اول تا ششم — شمارش، جمع و تفریق، ضرب، شکل و اندازه', ids: ['axtally', 'axbase10', 'axplace', 'axclock', 'axruler', 'axfrac', 'axmoney', 'axabacus', 'axjump', 'axbalance', 'axthermo', 'axsaw', 'axpattern', 'axtenframe', 'axsym', 'axbeaker', 'axgeoboard', 'axdice', 'axmularea', 'axvenn', 'axnumgrid', 'axfacttree', 'axfnmachine', 'axfracwall', 'axtangram', 'axangle', 'axrod', 'axtower', 'axmulwheel', 'axmultgrid', 'axnet', 'axcubes', 'axcalendar', 'axlever', 'axcycle', 'axwordtrain', 'axpaintgrid', 'axletterforms', 'axsyllablechart', 'axmagnet', 'axsenses'] },
    { id: 'middle', name: '📐 متوسطهٔ اول', hint: 'پایهٔ هفتم تا نهم — جبر، هندسه، اندازه‌گیری و آمار مقدماتی', ids: ['axtiles', 'axkenken', 'axdicefold', 'axtransform', 'axcaliper', 'axunitconv', 'axperimeter', 'axrounding', 'axcircleparts', 'axcylinder', 'axproportion', 'axlinear'] },
    { id: 'high', name: '🧪 متوسطهٔ دوم و کنکور', hint: 'ریاضی، فیزیک، شیمی و زیست — از تابع و مثلثات تا مدار و ژنتیک', ids: ['axsigntable', 'axtrigcircle', 'axmapping', 'axprobtree', 'axconic', 'axorbital', 'axpunnett', 'axecg', 'axprosody', 'axtruthtable', 'axledger', 'axstaff', 'axbarchart', 'axparabola', 'axinthales', 'axpendulum', 'axincline', 'axcircuit', 'axoptics', 'axheatcurve', 'axdna'] },
    { id: 'university', name: '🎓 انسانی و دانشگاه', hint: 'انسانی، هنر، فنی و دانشگاه — از عروض و حسابداری تا منطق دیجیتال و الگوریتم', ids: ['axkmap', 'axgate', 'axdfa', 'axbst', 'axbode', 'axbeam', 'axmohr', 'axpv', 'axpert', 'axbenzene', 'axnormal', 'axsupply', 'axgame', 'axtimeline', 'axmapgeo', 'axsentence', 'axvectors', 'axmatrix', 'axintegral', 'axflowchart', 'axstack', 'axsort', 'axregression'] }
  ];
  var mathCat = null;
  var shapesMode = null;
  function mathCatById(id) { for (var i = 0; i < MATH_CATS.length; i++) if (MATH_CATS[i].id === id) return MATH_CATS[i]; return null; }
  function mathToolById(id) { for (var i = 0; i < AXIS_TYPES.length; i++) if (AXIS_TYPES[i].id === id) return AXIS_TYPES[i]; return null; }
  function mathCatOfType(t) { for (var i = 0; i < MATH_CATS.length; i++) if (MATH_CATS[i].ids.indexOf(t) >= 0) return MATH_CATS[i]; return null; }
  /**
   * V320 — پنجرهٔ «ابزار ریاضی» دو مرحله‌ای شد:
   *   مرحلهٔ ۱: کارت چهار دسته (دبستان، متوسطهٔ اول، متوسطهٔ دوم و کنکور، انسانی و دانشگاه)
   *   مرحلهٔ ۲: ابزارهای همان دسته + دکمهٔ «↩ دسته‌بندی‌ها»
   */
  function fillShapes(mode, forceCat) {
    /* mode: 'graph' | 'axis' | 'math' */
    if (mode !== 'axis' && mode !== 'math') mode = 'graph';
    var math = mode === 'math';
    if (!math) mathCat = null;
    var key = mode + ':' + (math ? (mathCat || '-') : '');
    if (shapesMode === key) return;
    shapesMode = key;
    var host = $('grShapes'); if (!host) return;
    host.innerHTML = '';
    var axis = mode !== 'graph', sp = mathSplit();
    var title = math ? (mathCat ? ('ابزار ریاضی — ' + mathCatById(mathCat).name) : 'ابزار ریاضی — انتخاب دسته')
      : axis ? 'درج محور' : 'درج نمودار';
    if (math && !mathCat) {
      var wrapEl = document.createElement('div');
      wrapEl.className = 'gf-cats';
      MATH_CATS.forEach(function (c) {
        var card = document.createElement('button');
        card.type = 'button'; card.className = 'gf-cat'; card.setAttribute('data-cat', c.id);
        card.innerHTML = '<b>' + c.name + '</b><span>' + c.ids.length + ' ابزار</span><small>' + c.hint + '</small>';
        card.addEventListener('click', function () { mathCat = c.id; fillShapes('math'); });
        wrapEl.appendChild(card);
      });
      host.appendChild(wrapEl);
    } else {
      if (math && mathCat) {
        var back = document.createElement('button');
        back.type = 'button'; back.className = 'gf-back'; back.textContent = '↩ دسته‌بندی‌ها';
        back.addEventListener('click', function () { mathCat = null; fillShapes('math'); });
        host.appendChild(back);
      }
      var list;
      if (!axis) list = TYPES;
      else if (math) {
        var cur = mathCatById(mathCat);
        list = (cur ? cur.ids : AXIS_TYPES.slice(sp).map(function (x) { return x.id; })).map(mathToolById).filter(Boolean);
      } else list = AXIS_TYPES.slice(0, sp);
      list.forEach(function (s) {
        var b = document.createElement('button');
        b.type = 'button'; b.className = 'gf-shape'; b.setAttribute('data-t', s.id);
        b.innerHTML = iconOf(s.id) + '<span>' + s.name + '</span>';
        b.addEventListener('click', function () { setType(s.id); });
        host.appendChild(b);
      });
    }
    var h3 = document.querySelector('#grOverlay .gf-head h3'); if (h3) h3.textContent = title;
    var th = document.querySelector('#grOverlay .gf-types-h');
    if (th) th.textContent = math ? (mathCat ? 'ابزارهای این دسته' : 'دستهٔ آموزشی') : axis ? 'نوع محور' : 'نوع نمودار';
    var md = document.querySelector('#grOverlay .gf-modal');
    if (md) {
      md.setAttribute('aria-label', title);
      /* V320 — در مرحلهٔ انتخاب دسته، ستون فهرست پهن‌تر و کارت‌ها دو ستونه می‌شوند */
      md.classList.toggle('gf-cats-mode', !!(math && !mathCat));
    }
  }
  function ensureModal() {
    if ($('grOverlay')) return;
    var ov = document.createElement('div');
    ov.id = 'grOverlay';
    ov.className = 'gf-overlay';
    ov.innerHTML =
      '<div class="gf-modal gf-3" role="dialog" aria-label="درج نمودار">' +
        '<div class="gf-head"><h3>درج نمودار</h3><button type="button" class="gf-x" id="grClose">×</button></div>' +
        '<div class="gf-split an-split">' +
          '<aside class="gf-types"><div class="gf-types-h">نوع نمودار</div><div class="gf-shapes" id="grShapes"></div></aside>' +
          '<section class="an-mid"><div class="gf-preview" id="grPreview"></div></section>' +
          '<aside class="an-side"><div class="an-side-h">تنظیمات نمودار</div><div class="gf-fields" id="grFields"></div></aside>' +
        '</div>' +
        '<div class="gf-foot"><button type="button" class="gf-btn ghost" id="grCancel">انصراف</button>' +
        '<button type="button" class="gf-btn ok" id="grApply">درج در سؤال</button></div>' +
      '</div>';
    document.body.appendChild(ov);
    fillShapes('graph');
    ov.addEventListener('click', function (e) { if (e.target === ov) close(); });
    $('grClose').onclick = close;
    $('grCancel').onclick = close;
    $('grApply').onclick = apply;
  }
  function tokenOf(spec) { return '%%FIG:' + JSON.stringify(spec) + '%%'; }
  function targetTextarea() {
    var t = null;
    try { if (window.__qmfActiveField && document.body.contains(window.__qmfActiveField)) t = window.__qmfActiveField; } catch (e) {}
    try { if (!t && typeof activeMathTextarea !== 'undefined' && activeMathTextarea && document.body.contains(activeMathTextarea)) t = activeMathTextarea; } catch (e) {}
    try { if (!t && typeof extractedActiveTextarea !== 'undefined' && extractedActiveTextarea && document.body.contains(extractedActiveTextarea)) t = extractedActiveTextarea; } catch (e) {}
    return t || document.getElementById('qTxt_main') || document.querySelector('.screen[data-view="builder"] textarea');
  }
  function insertToken(token) {
    var t = targetTextarea();
    if (!t) return;
    try { if (window.QMF) QMF.upgrade(t); } catch (e) {}
    var start = t.selectionStart != null ? t.selectionStart : (t.value || '').length;
    var end = t.selectionEnd != null ? t.selectionEnd : start;
    var v = t.value || '';
    var left = v.slice(0, start);
    var next = (left.length && !/[\n]$/.test(left) ? '\n' : '') + token + '\n';
    t.value = left + next + v.slice(end);
    t.dispatchEvent(new Event('input', { bubbles: true }));
    try { if (window.__examBuilderUpdateQuestionText) window.__examBuilderUpdateQuestionText(t); } catch(e) {}
    if (typeof qMathSync === 'function') qMathSync(t.id);
  }
  function apply() {
    state.k = 'g';
    if (replaceEl && replaceEl.classList && replaceEl.classList.contains('qmf-fig')) {
      replaceEl.dataset.fig = JSON.stringify(state);
      replaceEl.setAttribute('data-fig', JSON.stringify(state));
      replaceEl.innerHTML = svgOf(state);
      var t = targetTextarea();
      try { if (t && window.QMF) QMF.syncFromSurface(t); } catch (e) {}
      try { if (t && window.__examBuilderUpdateQuestionText) window.__examBuilderUpdateQuestionText(t); } catch (e) {}
      try { if (typeof renderPreview === 'function') renderPreview(); } catch (e) {}
    } else insertToken(tokenOf(state));
    close();
  }
  function open(spec, el, mode) {
    replaceEl = el || null;
    var math = mode === 'math' || !!(spec && isMathType(spec.t)); /* V310 */
    var axis = mode === 'axis' || math || !!(spec && isAxisType(spec.t));
    /* V323 — منوی دسکتاپ دستهٔ خواسته‌شده را در window.__mathCatReq می‌گذارد تا پنجره مستقیم همان دسته را باز کند. */
    var catReq = (mode === 'math' && window.__mathCatReq) ? mathCatById(String(window.__mathCatReq)) : null;
    var t0 = math ? PRIMARY_MATH_FIRST_ID : 'axnum';
    try { state = spec ? JSON.parse(JSON.stringify(spec)) : (axis ? defAxis(t0) : def('col')); }
    catch (e) { state = axis ? defAxis(t0) : def('col'); }
    state.k = 'g';
    if (!state.t) state.t = axis ? t0 : 'col';
    if (!state.X) state.X = (isAxisType(state.t) ? defAxis(state.t) : def(state.t)).X;
    ensureModal();
    shapesMode = null;
    /* V320 — بازکردن تازه: از مرحلهٔ انتخاب دسته؛ ویرایش شکل موجودِ ریاضی: همان دسته از پیش انتخاب */
    var presel = (spec && spec.t) ? mathCatOfType(spec.t) : (catReq || null);
    mathCat = presel ? presel.id : null;
    fillShapes(math ? 'math' : axis ? 'axis' : 'graph');
    $('grOverlay').classList.add('open');
    var ok = $('grApply');
    if (ok) ok.textContent = replaceEl ? 'اعمال تغییرات' : 'درج در سؤال';
    setType(state.t);
  }
  function openFromEl(fig) {
    if (!fig) return false;
    var spec = {};
    try { spec = JSON.parse(fig.getAttribute('data-fig') || fig.dataset.fig || '{}'); }
    catch (e) { spec = def('col'); }
    open(spec, fig);
    return true;
  }
  function close() {
    gfDdClose();
    gfCellPopClose();
    var ov = $('grOverlay');
    if (ov) ov.classList.remove('open');
    replaceEl = null;
  }
  function make(raw) {
    var el = document.createElement('span');
    el.className = 'qmf-fig qmf-gra';
    el.contentEditable = 'false';
    el.setAttribute('data-fig', raw);
    el.dataset.fig = raw;
    el.setAttribute('role', 'img');
    el.setAttribute('aria-label', 'نمودار');
    el.title = 'برای ویرایش دوبار کلیک کنید';
    try { el.innerHTML = svgOf(JSON.parse(raw)); }
    catch (e) { el.textContent = '[نمودار]'; }
    return el;
  }

  function bind() {
    var btn = $('openGraphEditor');
    if (btn && !btn._grBound) {
      btn._grBound = true;
      btn.addEventListener('click', function (e) { e.preventDefault(); open(null, null); });
    }
  }

  window.GraphFig = { svg: svgOf, make: make, open: open, openFromEl: openFromEl, close: close };

  if (window.GeoFig) {
    var oldMake = window.GeoFig.make;
    var oldOpen = window.GeoFig.openFromEl;
    window.GeoFig.make = function (raw) {
      try {
        var s = JSON.parse(raw);
        if (s && s.k === 'g') return make(raw);
      } catch (e) {}
      return oldMake ? oldMake(raw) : make(raw);
    };
    window.GeoFig.openFromEl = function (fig) {
      try {
        var s = JSON.parse(fig.getAttribute('data-fig') || fig.dataset.fig || '{}');
        if (s && s.k === 'g') return openFromEl(fig);
      } catch (e) {}
      return oldOpen ? oldOpen(fig) : openFromEl(fig);
    };
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', bind);
  else bind();
})();
