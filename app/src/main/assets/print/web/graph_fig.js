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
  function wrap(inner, vh) {
    vh = vh || 280;
    return '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 360 ' + vh + '" width="320" height="' + Math.round(250 * vh / 280) + '" direction="ltr" overflow="hidden">' +
      '<style>text{font:700 12px Vazirmatn,Tahoma,sans-serif;fill:#243044;direction:ltr;unicode-bidi:embed}.tk{font:600 11px Vazirmatn,Tahoma,sans-serif;fill:#4a5870}.ttl{font:800 13px Vazirmatn,Tahoma,sans-serif;fill:#1a2433}.fa{font:700 11px Vazirmatn,Tahoma,sans-serif;fill:#1a2433}.lg{font:700 11px Vazirmatn,Tahoma,sans-serif;fill:#1a2433}.onf{font:800 11px Vazirmatn,Tahoma,sans-serif;fill:#fff;paint-order:stroke;stroke:#1a2433;stroke-width:2.6;stroke-linejoin:round}</style>' +
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
    'axnumgrid', 'axfacttree', 'axfnmachine', 'axfracwall', 'axtangram', 'axangle', 'axrod', 'axtower', 'axmulwheel', 'axmultgrid', 'axnet', 'axcubes', 'axcalendar', 'axlever', 'axcycle', 'axwordtrain', 'axtiles', 'axkenken', 'axdicefold', 'axtransform', 'axcaliper', 'axsigntable', 'axtrigcircle', 'axmapping', 'axprobtree', 'axconic', 'axorbital', 'axpunnett', 'axecg', 'axprosody', 'axtruthtable', 'axledger', 'axstaff', 'axkmap', 'axgate', 'axdfa', 'axbst', 'axbode', 'axbeam', 'axmohr', 'axpv', 'axpert', 'axbenzene', 'axnormal', 'axsupply', 'axgame',
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
    { id: 'axabacus', name: 'چرتکهٔ ستونی', X: { n: '2435', cols: '4' } },
    { id: 'axjump', name: 'پرش روی محور', X: { start: '0', step: '3', jumps: '4', max: '12' } },
    { id: 'axbalance', name: 'ترازوی دوکفه‌ای', X: { left: '5', right: '3', unit: 'کیلوگرم' } },
    { id: 'axthermo', name: 'دماسنج', X: { val: '25', min: '-10', max: '50' } },
    { id: 'axsaw', name: 'جدول شگفت‌انگیز و سودوکو', X: { size: '4', vals: '1,2,3,4;3,4,?,2;2,?,4,3;4,3,2,1', mode: 'shape' } },
    { id: 'axpattern', name: 'الگویابی رنگی/شکلی/عددی', X: { seq: '●,▲,●,▲,●,?', mode: 'shape' } },
    { id: 'axtenframe', name: 'قاب ده‌تایی', X: { a: '7', b: '5', frames: '2' } },
    { id: 'axsym', name: 'تقارن شطرنجی', X: { cells: '1,1;2,1;2,2;3,2', axis: 'v', mirror: '1' } },
    { id: 'axspin', name: 'چرخندهٔ احتمال', X: { parts: 'قرمز,آبی,سبز,زرد,قرمز,آبی', arrow: '1' } },
    { id: 'axbeaker', name: 'استوانهٔ مدرج (حجم)', X: { val: '350', max: '500', unit: 'mL' } },
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
    { id: 'axwordtrain', name: 'قطار کلمه‌سازی', X: { syll: 'با,با,با', word: 'بابا', mode: 'join' } },
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
    { id: 'axpictograph', name: 'نمودار تصویری و میله‌ای', X: { mode: 'picto', data: '4,7,3,6', labs: 'سیب,موز,پرتقال,انگور' } },
    { id: 'axrounding', name: 'گرد کردن و تخمین', X: { n: '147', mode: 'ten' } },
    { id: 'axdivision', name: 'تقسیم با باقی‌مانده', X: { a: '17', b: '5' } },
    { id: 'axshapeid', name: 'شناسایی و دسته‌بندی اشکال', X: { kind: 'tri', n: '5' } },
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
    { id: 'axcell', name: 'سلول جانوری و گیاهی', X: { kind: 'animal' } },
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
    function ln(x1, y1, x2, y2, c, w, dash) { return '<line x1="' + f2(x1) + '" y1="' + f2(y1) + '" x2="' + f2(x2) + '" y2="' + f2(y2) + '" stroke="' + c + '" stroke-width="' + w + '"' + (dash ? ' stroke-dasharray="4 3"' : '') + '/>'; }
    // V322 — جاگیری خودکار برچسب: متنِ بلندتر از بوم کوچک و در نهایت با «…» کوتاه می‌شود و مختصات داخل بوم مهار می‌شود.
    function fitTxt(x, s, an, sz) {
      var maxW = 344, txt = String(s), fs = sz || 11, est = txt.length * fs * 0.55, keep;
      if (est > maxW) { fs = Math.max(7, Math.floor(maxW / (txt.length * 0.55) * fs)); est = txt.length * fs * 0.55; }
      if (est > maxW) { keep = Math.max(2, Math.floor(maxW / (fs * 0.55)) - 1); txt = txt.slice(0, keep) + '…'; est = txt.length * fs * 0.55; }
      var half = est / 2, lo = an === 'middle' ? half + 3 : an === 'end' ? est + 3 : 3, hi = an === 'middle' ? 357 - half : an === 'end' ? 357 : 357 - est;
      var xx = lo <= hi ? Math.min(Math.max(x, lo), hi) : Math.min(Math.max(x, 3), 357);
      return [xx, txt, fs];
    }
    function tx(x, y, s, c, an, bold, sz) { if (s == null || String(s).trim() === '') return ''; var ft = fitTxt(x, s, an, sz); return '<text x="' + f2(ft[0]) + '" y="' + f2(y) + '" direction="ltr" font-family="sans-serif" font-size="' + ft[2] + '"' + (bold ? ' font-weight="700"' : '') + ' fill="' + c + '" text-anchor="' + an + '" style="font:' + (bold ? '700 ' : '400 ') + ft[2] + 'px sans-serif;direction:ltr;unicode-bidi:embed">' + esc(ft[1]) + '</text>'; }
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
      var nb = clamp(Math.floor(num(X.n, 134)), 0, 999999999), bd = [], bq = nb, bk, bLbMode = xs('label', '1');
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
      var den = clamp(Math.floor(num(X.den, 4)), 1, 24), nu = clamp(Math.floor(num(X.num, 1)), 0, den), shape = xs('shape', 'circle');
      var fillF = function (q) { return q < nu ? ACC : '#fff'; };
      if (shape === 'bar') { var bw = 300 / den; for (i = 0; i < den; i++) s += rc(30 + i * bw, 100, bw, 70, fillF(i), ST, 1.6); }
      else if (shape === 'square') {
        var scols = Math.ceil(Math.sqrt(den)), srows = Math.ceil(den / scols), scw = 180 / scols, sch = 180 / srows;
        for (i = 0; i < den; i++) s += rc(90 + (i % scols) * scw, 50 + Math.floor(i / scols) * sch, scw, sch, fillF(i), ST, 1.6);
      }
      else if (shape === 'ring') {
        if (den === 1) s += '<circle cx="180" cy="124" r="88" fill="' + fillF(0) + '" stroke="' + ST + '" stroke-width="1.8"/><circle cx="180" cy="124" r="44" fill="#fff" stroke="' + ST + '" stroke-width="1.8"/>';
        else for (i = 0; i < den; i++) {
          var ra0 = (i * 360 / den - 90) * Math.PI / 180, ra1 = ((i + 1) * 360 / den - 90) * Math.PI / 180, rlg = 360 / den > 180 ? 1 : 0;
          s += '<path d="M ' + f2(180 + 88 * Math.cos(ra0)) + ' ' + f2(124 + 88 * Math.sin(ra0)) + ' A 88 88 0 ' + rlg + ' 1 ' + f2(180 + 88 * Math.cos(ra1)) + ' ' + f2(124 + 88 * Math.sin(ra1)) + ' L ' + f2(180 + 44 * Math.cos(ra1)) + ' ' + f2(124 + 44 * Math.sin(ra1)) + ' A 44 44 0 ' + rlg + ' 0 ' + f2(180 + 44 * Math.cos(ra0)) + ' ' + f2(124 + 44 * Math.sin(ra0)) + ' Z" fill="' + fillF(i) + '" stroke="' + ST + '" stroke-width="1.8"/>';
        }
      }
      else if (den === 1) s += '<circle cx="180" cy="124" r="88" fill="' + (nu === 1 ? ACC : '#fff') + '" stroke="' + ST + '" stroke-width="1.8"/>';
      else for (i = 0; i < den; i++) {
        var a0 = (i * 360 / den - 90) * Math.PI / 180, a1 = ((i + 1) * 360 / den - 90) * Math.PI / 180, lg = 360 / den > 180 ? 1 : 0;
        s += '<path d="M 180 124 L ' + f2(180 + 88 * Math.cos(a0)) + ' ' + f2(124 + 88 * Math.sin(a0)) + ' A 88 88 0 ' + lg + ' 1 ' + f2(180 + 88 * Math.cos(a1)) + ' ' + f2(124 + 88 * Math.sin(a1)) + ' Z" fill="' + (i < nu ? ACC : '#fff') + '" stroke="' + ST + '" stroke-width="1.8"/>';
      }
      var frLb = xs('label', '1');
      if (frLb === '2') s += tx(180, 242, '؟', PT, 'middle', true, 19) + lnr(166, 250, 194, 250, ST, 2) + tx(180, 272, '؟', PT, 'middle', true, 19);
      else if (frLb !== '0') s += tx(180, 242, String(nu), ST, 'middle', true, 19) + lnr(166, 250, 194, 250, ST, 2) + tx(180, 272, String(den), ST, 'middle', true, 19);
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
      var cols = clamp(Math.floor(num(X.cols, 4)), 2, 6), an = clamp(Math.floor(num(X.n, 2435)), 0, 999999);
      var alabs = ['یکان', 'دهگان', 'صدگان', 'هزار', 'ده‌هزار', 'صدهزار'], acols = ['#ef5350', '#42a5f5', '#66bb6a', '#ab47bc', '#ffa726', '#26a69a'];
      var astep = 288 / cols, digs = [], arem = an;
      for (i = 0; i < cols; i++) { digs.push(arem % 10); arem = Math.floor(arem / 10); }
      s += rc(24, 214, 312, 22, '#8d6e63', ST, 1.8, 6);
      for (c = 0; c < cols; c++) {
        var aidx = cols - 1 - c, acx = 36 + c * astep + astep / 2, ad = digs[aidx], acol = acols[aidx % acols.length];
        s += lnr(acx, 44, acx, 214, ST, 3.2);
        for (k = 0; k < ad; k++) s += rc(acx - 16, 204 - k * 16 - 7, 32, 14, acol, ST, 1.2, 7);
        s += tx(acx, 230, alabs[aidx], '#fff', 'middle', true, 10);
        if (xs('blank', '0') !== '1') s += tx(acx, 258, String(ad), ST, 'middle', true, 15);
      }
      return wrap(s);
    }
    if (t === 'axjump') {
      var jst = clamp(Math.floor(num(X.start, 0)), 0, 50), jsp = clamp(Math.floor(num(X.step, 3)), 1, 20), jcnt = clamp(Math.floor(num(X.jumps, 4)), 1, 10);
      var jmax = clamp(Math.max(jst + jsp * jcnt, Math.floor(num(X.max, jst + jsp * jcnt))), 2, 100), jy = 186;
      s += lnr(20, jy, 342, jy, ST, 2.2);
      var jtk = jmax <= 15 ? 1 : jmax <= 30 ? 2 : 5;
      for (i = 0; i <= jmax; i++) {
        var jx = 28 + 304 * (i / jmax), jmaj = i % jtk === 0;
        s += lnr(jx, jy - (jmaj ? 7 : 4), jx, jy + (jmaj ? 7 : 4), ST, jmaj ? 1.8 : 1);
        if (jmaj) s += tx(jx, jy + 22, String(i), ST, 'middle', true, 11);
      }
      for (i = 0; i < jcnt; i++) {
        var ja = jst + i * jsp, jb = ja + jsp; if (jb > jmax) break;
        var jxa = 28 + 304 * (ja / jmax), jxb = 28 + 304 * (jb / jmax), jxm = (jxa + jxb) / 2, jh = Math.min(85, Math.max(34, (jxb - jxa) * 0.65));
        s += '<path d="M ' + f2(jxa) + ' ' + f2(jy - 4) + ' Q ' + f2(jxm) + ' ' + f2(jy - jh) + ' ' + f2(jxb) + ' ' + f2(jy - 4) + '" fill="none" stroke="' + ACC + '" stroke-width="2.4"/>';
        s += '<circle cx="' + f2(jxb) + '" cy="' + f2(jy) + '" r="4" fill="' + PT + '"/>' + tx(jxm, jy - jh * 0.58, '+' + jsp, PT, 'middle', true, 11);
      }
      if (xs('label', '1') === '2') s += tx(180, 252, jst + ' + (' + jcnt + ' × ' + jsp + ') = ؟', PT, 'middle', true, 14);
      else if (xs('label', '1') !== '0') s += tx(180, 252, jst + ' + (' + jcnt + ' × ' + jsp + ') = ' + (jst + jcnt * jsp), ST, 'middle', true, 14);
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
        var scell = (srows[r] && srows[r][c]) ? srows[r][c] : '?', sx = sx0 + c * scs, sy = sy0 + r * scs, sisQ = scell === '?' || scell === '؟';
        var stok = SAW_PAL[scell];
        var sbg = sisQ ? '#fff8e1' : ((smode === 'color' || smode === 'color_shape') && stok ? stok[2] : '#ffffff');
        s += rc(sx, sy, scs, scs, sbg, '#cbd5e1', 1);
        if (sisQ) s += tx(sx + scs / 2, sy + scs * 0.66, '؟', PT, 'middle', true, sfsz);
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
      var scells = xs('cells', '1,1;2,1;2,2;3,2').split(/[;؛]/).map(function (tk) {
        var pp = tk.split(/[,،]/).map(function (z) { return parseInt(faEn(z.trim()), 10); });
        return (pp.length >= 2 && isFinite(pp[0]) && isFinite(pp[1])) ? [clamp(pp[0], 1, 4), clamp(pp[1], 1, 4)] : null;
      }).filter(Boolean);
      var sc = 26, sn = 8, sgw = sc * sn, sx00 = (360 - sgw) / 2, sy00 = 32;
      s += rc(sx00, sy00, sgw, sgw, '#f8fafc', ST, 1.8);
      for (r = 0; r < sn; r++) for (c = 0; c < sn; c++) s += rc(sx00 + c * sc, sy00 + r * sc, sc, sc, 'none', '#cbd5e1', 1);
      scells.forEach(function (pr) {
        var r1 = pr[0], d1 = pr[1];
        if (saxis === 'h') {
          var rTop = clamp(4 - d1, 0, 3), cPos = clamp(r1 - 1, 0, 7), rBot = clamp(3 + d1, 4, 7);
          s += rc(sx00 + cPos * sc + 2, sy00 + rTop * sc + 2, sc - 4, sc - 4, ACC, ST, 1.2, 3);
          if (smir) s += rc(sx00 + cPos * sc + 2, sy00 + rBot * sc + 2, sc - 4, sc - 4, GRN, ST, 1.2, 3);
          else s += tx(sx00 + cPos * sc + sc / 2, sy00 + rBot * sc + sc * 0.68, '؟', PT, 'middle', true, 14);
        } else {
          var rPos = clamp(r1 - 1, 0, 7), cLeft = clamp(4 - d1, 0, 3), cRight = clamp(3 + d1, 4, 7);
          s += rc(sx00 + cLeft * sc + 2, sy00 + rPos * sc + 2, sc - 4, sc - 4, ACC, ST, 1.2, 3);
          if (smir) s += rc(sx00 + cRight * sc + 2, sy00 + rPos * sc + 2, sc - 4, sc - 4, GRN, ST, 1.2, 3);
          else s += tx(sx00 + cRight * sc + sc / 2, sy00 + rPos * sc + sc * 0.68, '؟', PT, 'middle', true, 14);
        }
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
      var bmx = Math.max(10, num(X.max, 500)), bval = clamp(num(X.val, 350), 0, bmx), bunit = xs('unit', 'mL');
      var bx0 = 96, bw2 = 112, btop = 36, bbot = 216, bh2 = bbot - btop, bvh = bh2 * (bval / bmx), bvy = bbot - bvh;
      s += rc(bx0, btop, bw2, bh2, '#f8fafc', 'none', 0);
      if (bval > 0) s += rc(bx0 + 3, bvy, bw2 - 6, bvh, 'rgba(38,166,154,.42)', '#00897b', 1.5);
      s += '<path d="M ' + f2(bx0 - 8) + ' ' + f2(btop) + ' L ' + f2(bx0) + ' ' + f2(btop) + ' L ' + f2(bx0) + ' ' + f2(bbot) + ' L ' + f2(bx0 + bw2) + ' ' + f2(bbot) + ' L ' + f2(bx0 + bw2) + ' ' + f2(btop) + ' L ' + f2(bx0 + bw2 + 8) + ' ' + f2(btop) + '" fill="none" stroke="' + ST + '" stroke-width="3"/>';
      s += tx(bx0 + bw2 + 34, btop - 8, '(' + bunit + ')', MU, 'middle', true, 11);
      for (i = 0; i <= 10; i++) {
        var bty = bbot - bh2 * (i / 10), bmaj = (i % 2 === 0);
        s += lnr(bx0 + bw2 - (bmaj ? 18 : 10), bty, bx0 + bw2, bty, ST, bmaj ? 1.8 : 1.1);
        if (bmaj && i > 0) s += tx(bx0 + bw2 + 34, bty + 4, String(Math.round(bmx * i / 10)), ST, 'middle', true, 12);
      }
      s += rc(bx0 - 4, 230, bw2 + 8, 30, '#e0f2f1', '#00897b', 1.5, 8) + tx(bx0 + bw2 / 2, 250, Math.round(bval) + ' ' + bunit, '#00695c', 'middle', true, 15);
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
      var finps = xs('in', '2,4,6,8').split(/[,،;]/).map(function (z) { return parseInt(faEn(z.trim()), 10); }).filter(isFinite).slice(0, 5);
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
      var fcols = finps.length + 1, fcw = Math.min(52, 290 / fcols), ftw = fcw * fcols, fx0 = (360 - ftw) / 2, fy0 = 116, frh = 44;
      s += rc(fx0, fy0, ftw, frh * 2, '#ffffff', ST, 2, 8) + lnr(fx0, fy0 + frh, fx0 + ftw, fy0 + frh, ST, 1.6);
      s += tx(fx0 + fcw / 2, fy0 + 27, 'ورودی', ACC, 'middle', true, 12) + tx(fx0 + fcw / 2, fy0 + frh + 27, 'خروجی', '#00897b', 'middle', true, 12);
      finps.forEach(function (inp, idx) {
        var fcx = fx0 + (idx + 1) * fcw, fout = fApply(fApply(inp, fop1), fop2), fhide = fmblank === 1 && idx >= Math.floor(finps.length / 2);
        s += lnr(fcx, fy0, fcx, fy0 + frh * 2, ST, 1.2) + tx(fcx + fcw / 2, fy0 + 28, String(inp), ST, 'middle', true, 14);
        if (fhide) s += rc(fcx + 3, fy0 + frh + 3, fcw - 6, frh - 6, '#fff8e1', 'none', 0, 5) + tx(fcx + fcw / 2, fy0 + frh + 28, '؟', PT, 'middle', true, 16);
        else s += tx(fcx + fcw / 2, fy0 + frh + 28, String(fout), ST, 'middle', true, 14);
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
    if (t === 'axtower') {
      var twBase = ilist('base', '1,2,3,4'), twOp = xs('op', '+') === '-' ? '−' : '+', twBlank = xs('blank', '0') === '1';
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
          var bx = 180 + (twK - (lvArr.length - 1) / 2) * twBw - twBw / 2, hide = twBlank && ((lvIdx + twK) % 3 === 1);
          s += rc(bx, ly, twBw - 3, twBh - 3, hide ? '#fff8e1' : lvIdx === 0 ? 'rgba(108,99,245,.16)' : '#ffffff', hide ? PT : ST, 1.6, 4) +
            tx(bx + (twBw - 3) / 2, ly + twBh * 0.66, hide ? '؟' : String(lvArr[twK]), hide ? PT : ST, 'middle', true, 13);
        }
      }
      s += tx(180, 268, twOp === '+' ? 'هر آجر = جمع دو آجر زیرین' : 'هر آجر = تفاضل دو آجر زیرین', MU, 'middle', false, 11);
      return wrap(s);
    }
    if (t === 'axmulwheel') {
      var mwC = clamp(Math.floor(num(X.c, 3)), 1, 12), mwN = clamp(Math.floor(num(X.n, 10)), 3, 12);
      var mwOp = xs('op', '×') === '+' ? '+' : '×', mwHide = xs('hide', '0') === '1', mwJ;
      s += pfxCls(180, 142, 96, 'none', ACC, 1.6) + pfxCls(180, 142, 34, 'rgba(108,99,245,.16)', ACC, 2.2);
      s += tx(180, 140, String(mwC), ACC, 'middle', true, 18) + tx(180, 158, mwOp, ACC, 'middle', true, 12);
      for (mwJ = 0; mwJ < mwN; mwJ++) {
        var ma = -Math.PI / 2 + mwJ * 2 * Math.PI / mwN, ox = 180 + 96 * Math.cos(ma), oy = 142 + 96 * Math.sin(ma);
        var mv = mwOp === '+' ? mwC + (mwJ + 1) : mwC * (mwJ + 1), oh = mwHide && mwJ % 3 === 2;
        s += lnr(180 + 34 * Math.cos(ma), 142 + 34 * Math.sin(ma), ox, oy, GR, 1.6);
        s += pfxCls(ox, oy, 17, oh ? '#fff8e1' : '#ffffff', oh ? PT : ST, 1.8);
        s += tx(ox, oy + 5, oh ? '؟' : String(mv), oh ? PT : ST, 'middle', true, 12);
      }
      return wrap(s);
    }
    if (t === 'axmultgrid') {
      var mgRows = ilist('rows', '2,4,7'), mgCols = ilist('cols', '3,5,8'), mgBlank = Math.floor(num(X.blank, 0));
      if (!mgRows.length) mgRows = [2, 4, 7];
      if (!mgCols.length) mgCols = [3, 5, 8];
      mgRows = mgRows.slice(0, 4); mgCols = mgCols.slice(0, 4);
      var mgCw = Math.min(56, 268 / (mgCols.length + 1)), mgCh = Math.min(42, 216 / (mgRows.length + 1));
      var mgX0 = 180 - mgCw * (mgCols.length + 1) / 2, mgY0 = 26 + (218 - mgCh * (mgRows.length + 1)) / 2, mgR, mgC;
      for (mgC = 0; mgC < mgCols.length; mgC++) {
        s += rc(mgX0 + (mgC + 1) * mgCw, mgY0, mgCw, mgCh, 'rgba(39,196,168,.18)', ST, 1.4) + tx(mgX0 + (mgC + 1.5) * mgCw, mgY0 + mgCh * 0.66, mgBlank === 2 ? '؟' : String(mgCols[mgC]), mgBlank === 2 ? PT : ST, 'middle', true, 14);
      }
      for (mgR = 0; mgR < mgRows.length; mgR++) {
        s += rc(mgX0, mgY0 + (mgR + 1) * mgCh, mgCw, mgCh, 'rgba(39,196,168,.18)', ST, 1.4) + tx(mgX0 + mgCw / 2, mgY0 + (mgR + 1.5) * mgCh, mgBlank === 2 ? '؟' : String(mgRows[mgR]), mgBlank === 2 ? PT : ST, 'middle', true, 14);
        for (mgC = 0; mgC < mgCols.length; mgC++) {
          var mv2 = mgRows[mgR] * mgCols[mgC], mh2 = mgBlank === 1 && (mgR + mgC) % 2 === 1;
          s += rc(mgX0 + (mgC + 1) * mgCw, mgY0 + (mgR + 1) * mgCh, mgCw, mgCh, mh2 ? '#fff8e1' : '#ffffff', ST, 1.4) + tx(mgX0 + (mgC + 1.5) * mgCw, mgY0 + (mgR + 1.5) * mgCh, mh2 ? '؟' : String(mv2), mh2 ? PT : ACC, 'middle', true, 14);
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
      var cbMap = cbRowsRaw.map(function (rw) { return rw.split(/[,،]/).map(function (q2) { return clamp(Math.floor(parseInt(faEn(q2.trim()), 10)) || 0, 0, 4); }); }).filter(function (rw) { return rw.length; }).slice(0, 4);
      if (!cbMap.length) cbMap = [[2, 1, 3], [1, 0, 1], [2, 2, 1]];
      var cbS = 30, cbTotal = 0;
      cbMap.forEach(function (rw) { rw.forEach(function (h2) { cbTotal += h2; }); });
      if (cbView === 'top' || cbView === 'front' || cbView === 'side') {
        var cgN = cbMap.length, cgM = cbMap[0].length || 1, cgW = Math.min(52, 260 / cgN), cgH = Math.min(46, 200 / cgM);
        for (var cr = 0; cr < cgN; cr++) for (var cc2 = 0; cc2 < (cbMap[cr].length || 0); cc2++) {
          var chh = cbMap[cr][cc2], cx2 = 180 + (cr - (cgN - 1) / 2) * cgW - cgW / 2, cy2 = 34 + (cc2 - (cgM - 1) / 2) * cgH;
          var val = cbView === 'top' ? chh : chh;
          s += rc(cx2, cy2, cgW - 3, cgH - 3, chh ? 'rgba(108,99,245,.22)' : '#ffffff', ST, 1.4, 3) + tx(cx2 + (cgW - 3) / 2, cy2 + cgH * 0.62, chh ? String(val) : '۰', ST, 'middle', true, 12);
        }
      } else {
        for (var ir = cbMap.length - 1; ir >= 0; ir--) {
          for (var ic = 0; ic < cbMap[ir].length; ic++) {
            var hh = cbMap[ir][ic];
            var ox2 = 92 + (ic - ir) * cbS * 0.9, oy2 = 196 - (ic + ir) * cbS * 0.5;
            if (hh > 0) {
              var topY = oy2 - (hh - 1) * cbS * 0.98 - cbS;
              s += pfxPoly(f2(ox2) + ',' + f2(topY) + ' ' + f2(ox2 + cbS * 0.9) + ',' + f2(topY + cbS * 0.5) + ' ' + f2(ox2) + ',' + f2(topY + cbS) + ' ' + f2(ox2 - cbS * 0.9) + ',' + f2(topY + cbS * 0.5), '#f7f9fd', '#9fb0c6', 1);
              s += pfxPoly(f2(ox2 - cbS * 0.9) + ',' + f2(topY + cbS * 0.5) + ' ' + f2(ox2) + ',' + f2(topY + cbS) + ' ' + f2(ox2) + ',' + f2(oy2) + ' ' + f2(ox2 - cbS * 0.9) + ',' + f2(oy2 - cbS * 0.5), 'rgba(108,99,245,.14)', '#9fb0c6', 1);
              s += pfxPoly(f2(ox2) + ',' + f2(topY + cbS) + ' ' + f2(ox2 + cbS * 0.9) + ',' + f2(topY + cbS * 0.5) + ' ' + f2(ox2 + cbS * 0.9) + ',' + f2(oy2 - cbS * 0.5) + ' ' + f2(ox2) + ',' + f2(oy2), 'rgba(39,196,168,.14)', '#9fb0c6', 1);
            }
          }
        }
      }
      s += tx(180, 268, cbView === 'top' ? 'نمای از بالا' : cbView === 'front' ? 'نمای روبه‌رو' : cbView === 'side' ? 'نمای از پهلو' : ('تعداد مکعب‌ها: ' + (xs('hide', '0') === '1' ? '؟' : cbTotal)), MU, 'middle', true, 12);
      return wrap(s);
    }
    if (t === 'axcalendar') {
      var clMonth = xs('month', 'مهر'), clStart = clamp(Math.floor(num(X.start, 3)), 0, 6), clDays = clamp(Math.floor(num(X.days, 30)), 20, 31);
      var clMark = String(xs('mark', '12,23')).split(/[,،;]/).map(function (q3) { return parseInt(faEn(q3.trim()), 10); }).filter(function (q3) { return isFinite(q3); });
      var clBlank = xs('blank', '0') === '1', clWd = ['ش', 'ی', 'د', 'س', 'چ', 'پ', 'ج'], clCw = 44, clCh = 30, clX0 = 180 - clCw * 3.5, clY0 = 46;
      s += tx(180, 30, 'تقویم ' + clMonth, ST, 'middle', true, 14);
      for (var cd = 0; cd < 7; cd++) s += rc(clX0 + cd * clCw, clY0, clCw, clCh, 'rgba(108,99,245,.16)', ST, 1.2) + tx(clX0 + cd * clCw + clCw / 2, clY0 + clCh * 0.68, clWd[6 - cd], ACC, 'middle', true, 12);
      for (var ci = 0; ci < clDays; ci++) {
        var cellIdx = ci + clStart, col2 = 6 - (cellIdx % 7), row2 = Math.floor(cellIdx / 7), dayN = ci + 1;
        var bx2 = clX0 + col2 * clCw, by2 = clY0 + clCh + row2 * clCh, isMark = clMark.indexOf(dayN) >= 0, isBlank = clBlank && dayN % 5 === 0;
        s += rc(bx2, by2, clCw, clCh, isMark ? 'rgba(39,196,168,.26)' : '#ffffff', ST, 1.1) + tx(bx2 + clCw / 2, by2 + clCh * 0.66, isBlank ? '؟' : String(dayN), isBlank ? PT : isMark ? '#00796b' : ST, 'middle', isMark || isBlank, 12);
      }
      return wrap(s);
    }
    if (t === 'axlever') {
      var lvKind = xs('kind', 'lever1').toLowerCase(), lvLoad = num(X.load, 60), lvEff = num(X.effort, 20);
      var lvL1 = Math.max(0.5, num(X.l1, 2)), lvL2 = Math.max(0.5, num(X.l2, 1)), lvMa = (lvL1 / lvL2);
      if (lvKind === 'pulley') {
        s += pfxCls(180, 60, 26, 'rgba(108,99,245,.16)', ACC, 2.2) + pfxCls(180, 60, 6, ST, 'none', 0);
        s += pfxCls(236, 110, 20, 'rgba(39,196,168,.18)', GRN, 2.2) + pfxCls(236, 110, 5, ST, 'none', 0);
        s += lnr(180, 86, 180, 130, ST, 2) + pfxArrow(180, 130, 180, 168, PT, 2, 9);
        s += lnr(180, 34, 300, 34, ST, 2) + lnr(256, 110, 300, 110, ST, 2) + pfxArrow(300, 110, 300, 168, ACC, 2, 9);
        s += tx(180, 188, 'بار: ' + nm(lvLoad) + ' N', PT, 'middle', true, 12) + tx(300, 188, 'نیرو: ' + nm(lvEff) + ' N', ACC, 'middle', true, 12);
        s += tx(180, 252, 'قرقرهٔ متحرک — مزیت مکانیکی ≈ ۲', MU, 'middle', true, 12);
        return wrap(s);
      }
      if (lvKind === 'gears') {
        var grA = 40, grB = 22;
        s += pfxCls(140, 130, grA, 'rgba(108,99,245,.18)', ACC, 2.2) + pfxCls(140 + grA + grB, 130, grB, 'rgba(240,162,2,.22)', GLD, 2.2);
        for (var gj = 0; gj < 16; gj++) { var ga = gj * Math.PI / 8; s += lnr(140 + (grA - 5) * Math.cos(ga), 130 + (grA - 5) * Math.sin(ga), 140 + (grA + 5) * Math.cos(ga), 130 + (grA + 5) * Math.sin(ga), ACC, 3); }
        for (var gk = 0; gk < 10; gk++) { var gb2 = gk * Math.PI / 5; s += lnr(140 + grA + (grB - 4) * Math.cos(gb2), 130 + (grB - 4) * Math.sin(gb2), 140 + grA + (grB + 4) * Math.cos(gb2), 130 + (grB + 4) * Math.sin(gb2), GLD, 3); }
        s += tx(140, 136, nm(grA) + 'T', ACC, 'middle', true, 12) + tx(140 + grA + grB, 136, nm(grB) + 'T', '#a06a00', 'middle', true, 11);
        s += tx(180, 240, 'چرخ‌دنده — نسبت انتقال = ' + nm(Math.round(grA / grB * 10) / 10), MU, 'middle', true, 12);
        return wrap(s);
      }
      var lvFulc = 180, lvY = 150, lvSpan = 250, lvX1 = lvFulc - lvSpan / 2, lvX2 = lvFulc + lvSpan / 2;
      if (lvKind === 'lever2') lvFulc = lvX1 + 20;
      if (lvKind === 'lever3') lvFulc = lvX1 + 20;
      lvX1 = lvFulc - (lvKind === 'lever1' ? lvSpan / 2 : 20);
      lvX2 = lvX1 + lvSpan;
      s += lnr(lvX1, lvY, lvX2, lvY, ST, 7);
      s += pfxPoly(f2(lvFulc) + ',' + f2(lvY + 4) + ' ' + f2(lvFulc - 20) + ',' + f2(lvY + 46) + ' ' + f2(lvFulc + 20) + ',' + f2(lvY + 46), 'rgba(108,99,245,.25)', ST, 2);
      var ldX = lvKind === 'lever1' ? lvX1 + 40 : lvX2, efX = lvKind === 'lever1' ? lvX2 - 60 : (lvKind === 'lever2' ? lvX2 : lvX2 - 60);
      if (lvKind === 'lever1') { ldX = lvX1 + 45; efX = lvX2 - 45; }
      if (lvKind === 'lever2') { ldX = 180 + 40; efX = lvX2 - 30; }
      if (lvKind === 'lever3') { ldX = lvX2 - 30; efX = 180 + 40; }
      s += pfxArrow(ldX, lvY - 8, ldX, lvY - 58, PT, 3, 10) + rc(ldX - 26, lvY - 96, 52, 34, 'rgba(214,51,108,.16)', PT, 1.8, 6) + tx(ldX, lvY - 74, nm(lvLoad) + ' N', PT, 'middle', true, 11);
      s += pfxArrow(efX, lvY - 8, efX, lvY - 58, ACC, 3, 10) + rc(efX - 26, lvY - 96, 52, 34, 'rgba(108,99,245,.16)', ACC, 1.8, 6) + tx(efX, lvY - 74, nm(lvEff) + ' N', ACC, 'middle', true, 11);
      s += tx(180, lvY + 70, 'اهرم نوع ' + (lvKind === 'lever1' ? 'اول' : lvKind === 'lever2' ? 'دوم' : 'سوم') + ' — مزیت مکانیکی = ' + nm(Math.round(lvMa * 100) / 100), MU, 'middle', true, 12);
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
    if (t === 'axwordtrain') {
      var wtSyl = String(xs('syll', 'با,با,با')).split(/[,،]/).map(function (q4) { return q4.trim(); }).filter(Boolean).slice(0, 5);
      if (!wtSyl.length) wtSyl = ['با', 'با', 'با'];
      var wtWord = xs('word', 'بابا'), wtMode = xs('mode', 'join').toLowerCase(), wtX = 180 - (wtSyl.length * 74) / 2;
      s += lnr(wtX - 20, 120, wtX + wtSyl.length * 74 + 6, 120, ST, 3);
      wtSyl.forEach(function (sy, si) {
        var sx2 = wtX + si * 74;
        s += pfxPoly(f2(sx2) + ',92 ' + f2(sx2 + 62) + ',92 ' + f2(sx2 + 62) + ',142 ' + f2(sx2) + ',142', 'rgba(108,99,245,.16)', ST, 1.8);
        s += pfxCls(sx2 + 12, 152, 7, '#ffffff', ST, 1.6) + pfxCls(sx2 + 50, 152, 7, '#ffffff', ST, 1.6);
        s += tx(sx2 + 31, 128, sy, ST, 'middle', true, 16);
      });
      s += rc(180 - 92, 186, 184, 44, wtMode === 'blank' ? '#fff8e1' : 'rgba(39,196,168,.18)', wtMode === 'blank' ? PT : GRN, 2, 8);
      s += tx(180, 214, wtMode === 'blank' ? 'کلمهٔ ساخته‌شده: ؟' : 'کلمه: ' + wtWord, wtMode === 'blank' ? PT : '#00796b', 'middle', true, 15);
      return wrap(s);
    }
    // ============================== V318 — بستهٔ جامع ابزارهای آموزشی (۲) متوسطهٔ اول
    if (t === 'axtiles') {
      var tlX2 = clamp(Math.floor(num(X.x2, 1)), 0, 4), tlX = clamp(Math.floor(num(X.x, 2)), 0, 8), tlK = clamp(Math.floor(num(X.k, 3)), 0, 12), tlNeg = xs('neg', '0') === '1';
      var tlA = 54, tlB = 54, tlS = 18, tlX0 = 34, tlY0 = 26;
      if (tlNeg) { tlX = -tlX; tlK = -tlK; }
      var tlFill = function (on) { return tlNeg ? 'rgba(214,51,108,.18)' : 'rgba(108,99,245,.18)'; };
      for (var ti2 = 0; ti2 < tlX2; ti2++) {
        var tx2 = tlX0 + (ti2 % 2) * (tlA + 8), ty2 = tlY0 + Math.floor(ti2 / 2) * (tlA + 8);
        s += rc(tx2, ty2, tlA, tlA, tlFill(), ACC, 1.8, 4) + tx(tx2 + tlA / 2, ty2 + tlA / 2 + 6, 'x²', ACC, 'middle', true, 17);
      }
      var tlColX = tlX0 + ((tlX2 > 0) ? 2 * (tlA + 8) + 6 : 0);
      for (var tj = 0; tj < Math.abs(tlX); tj++) {
        var tx3 = tlColX + (tj % 2) * (tlA + 6), ty3 = tlY0 + Math.floor(tj / 2) * (tlS + 6);
        s += rc(tx3, ty3, tlA, tlS, tlFill(), ACC, 1.5, 3) + tx(tx3 + tlA / 2, ty3 + tlS - 4, 'x', ACC, 'middle', true, 12);
      }
      var tlColK = tlColX + ((tlX !== 0) ? 2 * (tlA + 6) + 6 : 0);
      for (var tk = 0; tk < Math.abs(tlK); tk++) {
        var tx4 = tlColK + (tk % 3) * (tlS + 6), ty4 = tlY0 + Math.floor(tk / 3) * (tlS + 6);
        s += rc(tx4, ty4, tlS, tlS, tlFill(), ST, 1.4, 3) + tx(tx4 + tlS / 2, ty4 + tlS - 4, '۱', ST, 'middle', true, 11);
      }
      var tlExpr = (tlNeg ? '−(' : '') + (tlX2 ? nm(tlX2) + 'x² ' : '') + (tlX ? '+ ' + nm(Math.abs(tlX)) + 'x ' : '') + (tlK ? '+ ' + nm(Math.abs(tlK)) : '') + (tlNeg ? ')' : '');
      s += tx(180, 246, 'مدل کاشی‌ها:  ' + tlExpr, ST, 'middle', true, 13);
      s += tx(180, 266, 'آبی = مثبت • قرمز = منفی', MU, 'middle', false, 11);
      return wrap(s);
    }
    if (t === 'axkenken') {
      var kkSize = clamp(Math.floor(num(X.size, 4)), 3, 6);
      var kkCages = String(xs('cages', '11,12|6|+;13,14|7|+;21,22|3|×;23,24,31,32|10|+;33,34|4|×;41,42,43,44|10|+')).split(/[;؛]/).filter(Boolean);
      var kkCell = Math.min(52, 240 / kkSize), kkX0 = 180 - kkCell * kkSize / 2, kkY0 = 34;
      var kkOwner = {}, kkHead = {};
      kkCages.forEach(function (cg, ci2) {
        var parts = cg.split('|'); if (parts.length < 2) return;
        var cells = parts[0].split(/[,،]/).map(function (c3) { return c3.trim(); }).filter(Boolean);
        cells.forEach(function (c3, j2) { kkOwner[c3] = ci2; if (j2 === 0) kkHead[c3] = (parts[1] || '') + (parts[2] || ''); });
      });
      for (var kr = 1; kr <= kkSize; kr++) for (var kc = 1; kc <= kkSize; kc++) {
        var key = String(kr) + String(kc), kx2 = kkX0 + (kc - 1) * kkCell, ky2 = kkY0 + (kr - 1) * kkCell;
        s += rc(kx2, ky2, kkCell, kkCell, '#ffffff', '#b9c4d4', 1);
        if (kkHead[key]) s += tx(kx2 + 4, ky2 + 13, kkHead[key], ACC, 'start', true, 10);
      }
      kkCages.forEach(function (cg) {
        var parts = cg.split('|'); if (parts.length < 2) return;
        var cells = parts[0].split(/[,،]/).map(function (c3) { return c3.trim(); }).filter(Boolean);
        cells.forEach(function (c3) {
          var kr2 = parseInt(c3[0], 10), kc2 = parseInt(c3[1], 10);
          if (!isFinite(kr2) || !isFinite(kc2)) return;
          var cx3 = kkX0 + (kc2 - 1) * kkCell, cy3 = kkY0 + (kr2 - 1) * kkCell;
          var up = cells.indexOf(String(kr2 - 1) + String(kc2)) < 0, dn = cells.indexOf(String(kr2 + 1) + String(kc2)) < 0;
          var lf = cells.indexOf(String(kr2) + String(kc2 - 1)) < 0, rt = cells.indexOf(String(kr2) + String(kc2 + 1)) < 0;
          if (up) s += lnr(cx3, cy3, cx3 + kkCell, cy3, ST, 2.6);
          if (dn) s += lnr(cx3, cy3 + kkCell, cx3 + kkCell, cy3 + kkCell, ST, 2.6);
          if (lf) s += lnr(cx3, cy3, cx3, cy3 + kkCell, ST, 2.6);
          if (rt) s += lnr(cx3 + kkCell, cy3, cx3 + kkCell, cy3 + kkCell, ST, 2.6);
        });
      });
      s += tx(180, kkY0 + kkCell * kkSize + 26, 'کن‌کن — عدد هر سطر و ستون بدون تکرار', MU, 'middle', true, 12);
      return wrap(s);
    }
    if (t === 'axdicefold') {
      var dfMode = xs('mode', 'dots').toLowerCase(), dfTop = clamp(Math.floor(num(X.top, 1)), 1, 6);
      var dfU = 56, dfX0 = 180 - dfU * 1.5, dfY0 = 62;
      var dfFaces = [[0, 1, dfTop], [1, 1, (dfTop % 6) + 1], [2, 1, (dfTop + 1) % 6 + 1], [3, 1, (dfTop + 2) % 6 + 1], [1, 0, 7 - dfTop], [1, 2, 7 - ((dfTop % 6) + 1)]];
      var dfPips = { 1: [[0, 0]], 2: [[-1, -1], [1, 1]], 3: [[-1, -1], [0, 0], [1, 1]], 4: [[-1, -1], [1, -1], [-1, 1], [1, 1]], 5: [[-1, -1], [1, -1], [0, 0], [-1, 1], [1, 1]], 6: [[-1, -1], [1, -1], [-1, 0], [1, 0], [-1, 1], [1, 1]] };
      dfFaces.forEach(function (fc, fi2) {
        var fx = dfX0 + fc[0] * dfU, fy = dfY0 + fc[1] * dfU, val = fc[2], hide = xs('ask', '0') === '1' && fi2 > 2;
        s += rc(fx, fy, dfU, dfU, hide ? '#fff8e1' : '#ffffff', ST, 1.6);
        if (dfMode === 'num' || hide) s += tx(fx + dfU / 2, fy + dfU / 2 + 6, hide ? '؟' : String(val), hide ? PT : ST, 'middle', true, 18);
        else (dfPips[val] || []).forEach(function (pp) { s += pfxCls(fx + dfU / 2 + pp[0] * 15, fy + dfU / 2 + pp[1] * 15, 4.6, ST, 'none', 0); });
      });
      s += tx(180, 250, 'گستردهٔ تاس — مجموع وجه‌های روبه‌رو = ۷', MU, 'middle', true, 12);
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
      // V322 — مقدار خوانده‌شده بزرگ‌تر از حداکثر، عقربه/درجه را بیرون از خط‌کش می‌برد.
      var cpKind = xs('kind', 'vernier').toLowerCase(), cpMax = Math.min(999, Math.max(10, num(X.max, 50))), cpVal = Math.min(cpMax, Math.max(0, num(X.val, 23.4)));
      if (cpKind === 'micrometer') {
        s += rc(40, 96, 200, 30, 'rgba(108,99,245,.14)', ST, 2, 4) + rc(240, 86, 46, 50, 'rgba(39,196,168,.18)', ST, 2, 4);
        s += lnr(40, 126, 40, 126, ST, 3) + rc(28, 86, 14, 50, ST, 'none', 0, 3) + lnr(286, 96, 312, 96, ST, 6) + lnr(286, 116, 312, 116, ST, 6);
        for (var mi = 0; mi <= cpMax; mi += 5) { var mx2 = 40 + mi / cpMax * 200; s += lnr(mx2, 96, mx2, mi % 10 === 0 ? 112 : 104, ST, 1.2); if (mi % 10 === 0) s += tx(mx2, 90, String(mi), MU, 'middle', false, 9); }
        var mv3 = cpVal - Math.floor(cpVal);
        for (var mj = 0; mj < 50; mj++) { var ma2 = mj * Math.PI / 25; s += lnr(263 + 22 * Math.cos(ma2), 111 + 22 * Math.sin(ma2), 263 + 22 * Math.cos(ma2 + 0.02), 111 + 22 * Math.sin(ma2 + 0.02), ST, 1); }
        s += lnr(263 + 30 * Math.cos(mv3 * 2 * Math.PI), 111 + 30 * Math.sin(mv3 * 2 * Math.PI), 263, 111, PT, 2.4);
        s += tx(180, 210, 'ریزسنج: ' + nm(Math.round(cpVal * 100) / 100) + ' mm', ST, 'middle', true, 15) + tx(180, 236, 'خواندن: ' + nm(Math.floor(cpVal)) + ' mm + ' + nm(Math.round(mv3 * 100)) + ' صدم', MU, 'middle', false, 12);
        return wrap(s);
      }
      s += rc(26, 110, 308, 26, '#ffffff', ST, 2, 3) + rc(26, 136, 308, 24, 'rgba(108,99,245,.10)', ST, 1.6, 3);
      s += rc(18, 96, 16, 54, ST, 'none', 0, 3) + rc(326, 96, 16, 54, ST, 'none', 0, 3) + lnr(34, 80, 34, 96, ST, 4) + lnr(326, 80, 326, 96, ST, 4);
      for (var vi = 0; vi <= cpMax; vi += 1) {
        var vx4 = 34 + vi / cpMax * 292, maj2 = vi % 10 === 0, mid2 = vi % 5 === 0;
        s += lnr(vx4, 110, vx4, 110 + (maj2 ? 14 : mid2 ? 9 : 5), '#0d47a1', maj2 ? 1.4 : 0.8);
        if (maj2) s += tx(vx4, 104, String(vi), '#0d47a1', 'middle', true, 9);
      }
      var cpOff = 34 + cpVal / cpMax * 292;
      for (var vj = 0; vj <= 10; vj++) { var vx2 = cpOff + vj * 4; s += lnr(vx2, 136, vx2, 136 + (vj % 5 === 0 ? 14 : 8), '#00695c', 1.2); if (vj % 5 === 0) s += tx(vx2, 162, String(vj / 10).slice(0, 1), '#00695c', 'middle', false, 8); }
      s += pfxCls(cpOff, 148, 3.4, PT, 'none', 0);
      s += tx(180, 206, 'کولیس: ' + nm(Math.round(cpVal * 10) / 10) + ' میلی‌متر', ST, 'middle', true, 15) + tx(180, 232, 'کولیس ورنیه — دقت ۰٫۱ میلی‌متر', MU, 'middle', false, 12);
      return wrap(s);
    }
    // ============================== V318 — بستهٔ جامع ابزارهای آموزشی (۳) متوسطهٔ دوم و کنکور
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
      var tcDeg = num(X.deg, 135), tcShow = xs('show', '1') !== '0', tcCx = 176, tcCy = 140, tcR = 96;
      var tcRad = tcDeg * Math.PI / 180, tcPx = tcCx + tcR * Math.cos(tcRad), tcPy = tcCy - tcR * Math.sin(tcRad);
      s += pfxCls(tcCx, tcCy, tcR, 'rgba(227,242,253,.5)', '#1565c0', 1.8);
      s += lnr(tcCx - tcR - 22, tcCy, tcCx + tcR + 22, tcCy, ST, 1.4) + pfxArrow(tcCx + tcR + 14, tcCy, tcCx + tcR + 30, tcCy, ST, 1.4, 7);
      s += lnr(tcCx, tcCy + tcR + 22, tcCx, tcCy - tcR - 22, ST, 1.4) + pfxArrow(tcCx, tcCy - tcR - 14, tcCx, tcCy - tcR - 30, ST, 1.4, 7);
      s += tx(tcCx + tcR + 32, tcCy + 4, 'x', ST, 'start', true, 12) + tx(tcCx + 6, tcCy - tcR - 26, 'y', ST, 'start', true, 12);
      s += lnr(tcCx - tcR, tcCy, tcCx + tcR, tcCy, '#b0bec5', 1);
      s += lnr(tcCx, tcCy, tcPx, tcPy, PT, 2.4) + pfxCls(tcPx, tcPy, 4.6, PT, 'none', 0);
      if (tcShow) {
        s += pfxPath('M ' + f2(tcPx) + ' ' + f2(tcPy) + ' L ' + f2(tcPx) + ' ' + f2(tcCy) + ' L ' + f2(tcCx) + ' ' + f2(tcCy), 'none', GRN, 1.6, '4 3');
        s += lnr(tcPx, tcCy, tcPx, tcPy, 'rgba(39,196,168,.35)', 6);
        s += lnr(tcCx, tcCy, tcPx, tcCy, 'rgba(108,99,245,.35)', 6);
        s += tx(tcPx + (tcPx >= tcCx ? 7 : -7), (tcPy + tcCy) / 2 + 4, 'sin', GRN, tcPx >= tcCx ? 'start' : 'end', true, 11);
        s += tx((tcCx + tcPx) / 2, tcCy - 7, 'cos', ACC, 'middle', true, 11);
      }
      s += pfxPath('M ' + f2(tcCx + 34) + ' ' + f2(tcCy) + ' A 34 34 0 0 0 ' + f2(tcCx + 34 * Math.cos(tcRad)) + ' ' + f2(tcCy - 34 * Math.sin(tcRad)), 'none', PT, 1.8);
      s += tx(tcCx + 48 * Math.cos(tcRad / 2), tcCy - 48 * Math.sin(tcRad / 2) + 4, Math.round(tcDeg) + '°', PT, 'middle', true, 11);
      s += tx(tcCx - 12, tcCy + 16, 'O', MU, 'middle', true, 11) + tx(tcCx + tcR + 4, tcCy + 16, '(1,0)', MU, 'middle', false, 9);
      var tcSin = Math.round(Math.sin(tcRad) * 1000) / 1000, tcCos = Math.round(Math.cos(tcRad) * 1000) / 1000;
      s += tx(180, 254, 'sin ' + Math.round(tcDeg) + '° = ' + nm(tcSin) + '   •   cos ' + Math.round(tcDeg) + '° = ' + nm(tcCos), ST, 'middle', true, 12);
      return wrap(s);
    }
    if (t === 'axmapping') {
      var mpFrom = String(xs('from', '1,2,3')).split(/[,،]/).map(function (q6) { return q6.trim(); }).filter(Boolean).slice(0, 5);
      var mpTo = String(xs('to', 'a,b,c')).split(/[,،]/).map(function (q6) { return q6.trim(); }).filter(Boolean).slice(0, 5);
      var mpArrows = String(xs('arrows', '1-a;2-a;3-b')).split(/[;؛]/).map(function (q6) { return q6.trim().split(/[-:→]/).map(function (z2) { return z2.trim(); }); }).filter(function (q6) { return q6.length >= 2; });
      if (!mpFrom.length) mpFrom = ['1', '2', '3'];
      if (!mpTo.length) mpTo = ['a', 'b', 'c'];
      var mpLx = 96, mpRx = 264, mpCx2 = 180, mpTop = 44, mpStep = Math.min(44, 190 / Math.max(mpFrom.length, mpTo.length));
      s += '<ellipse cx="' + mpLx + '" cy="' + (mpTop + 90) + '" rx="52" ry="104" fill="rgba(108,99,245,.10)" stroke="' + ACC + '" stroke-width="2"/>';
      s += '<ellipse cx="' + mpRx + '" cy="' + (mpTop + 90) + '" rx="52" ry="104" fill="rgba(39,196,168,.10)" stroke="' + GRN + '" stroke-width="2"/>';
      s += tx(mpLx, 30, 'دامنه', ACC, 'middle', true, 12) + tx(mpRx, 30, 'برد', '#00796b', 'middle', true, 12);
      var mpPos = {};
      mpFrom.forEach(function (v3, i3) { var yy3 = mpTop + 30 + i3 * mpStep; mpPos[v3] = yy3; s += pfxCls(mpLx, yy3, 4, ACC, 'none', 0) + tx(mpLx + 10, yy3 + 5, v3, ST, 'start', true, 13); });
      mpTo.forEach(function (v3, i3) { var yy3 = mpTop + 30 + i3 * mpStep; mpPos[v3] = yy3; s += pfxCls(mpRx, yy3, 4, GRN, 'none', 0) + tx(mpRx + 10, yy3 + 5, v3, ST, 'start', true, 13); });
      var mpFn = true, mpSeen = {};
      mpArrows.forEach(function (pr) {
        var y1 = mpPos[pr[0]], y2 = mpPos[pr[1]];
        if (y1 == null || y2 == null) return;
        if (mpSeen[pr[0]]) mpFn = false; mpSeen[pr[0]] = 1;
        s += pfxArrow(mpLx + 12, y1, mpRx - 12, y2, 'rgba(214,51,108,.85)', 1.6, 7);
      });
      s += tx(180, 258, (mpFn ? 'این تناظر یک تابع است' : 'این تناظر تابع نیست') + ' — ' + mpArrows.length + ' پیکان', mpFn ? '#1b7f4c' : PT, 'middle', true, 12);
      return wrap(s);
    }
    if (t === 'axprobtree') {
      var pbA = String(xs('first', 'شیر,خط')).split(/[,،]/).map(function (q7) { return q7.trim(); }).filter(Boolean).slice(0, 3);
      var pbB = String(xs('second', 'شیر,خط')).split(/[,،]/).map(function (q7) { return q7.trim(); }).filter(Boolean).slice(0, 3);
      if (!pbA.length) pbA = ['شیر', 'خط'];
      if (!pbB.length) pbB = ['شیر', 'خط'];
      var pbRoot = [56, 140], pbLv1X = 152, pbLv2X = 286, pbShow = xs('mode', 'prob') !== 'count';
      s += pfxCls(pbRoot[0], pbRoot[1], 5, ST, 'none', 0);
      var pbY1 = pbA.map(function (a3, i4) { return 60 + i4 * (170 / Math.max(1, pbA.length - 1 || 1)); });
      if (pbA.length === 1) pbY1 = [140];
      pbA.forEach(function (a3, i5) {
        var y1b = pbY1[i5];
        s += pfxArrow(pbRoot[0], pbRoot[1], pbLv1X - 10, y1b, ST, 1.6, 7) + tx((pbRoot[0] + pbLv1X) / 2 - 6, (pbRoot[1] + y1b) / 2 - 6, a3, ST, 'middle', true, 11);
        var y2b = pbB.map(function (b3, j3) { return y1b + (j3 - (pbB.length - 1) / 2) * 40; });
        pbB.forEach(function (b3, j4) {
          s += pfxArrow(pbLv1X, y1b, pbLv2X - 12, y2b[j4], 'rgba(108,99,245,.8)', 1.4, 6);
          s += tx((pbLv1X + pbLv2X) / 2 - 4, (y1b + y2b[j4]) / 2 - 5, b3, MU, 'middle', false, 10);
          var lab2 = pbShow ? nm(Math.round(100 / (pbA.length * pbB.length))) + '٪' : (a3 + ' , ' + b3);
          s += tx(pbLv2X + 4, y2b[j4] + 4, lab2, '#00796b', 'start', true, 10);
        });
        s += tx(pbLv1X + 2, y1b - 8, pbShow ? nm(Math.round(100 / pbA.length)) + '٪' : '', ACC, 'start', true, 10);
      });
      s += tx(180, 262, 'درخت احتمال دو مرحله‌ای', MU, 'middle', true, 12);
      return wrap(s);
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
      var orZ = clamp(Math.floor(num(X.z, 26)), 1, 36), orMode = xs('mode', 'box').toLowerCase();
      var orSubs = [['1s', 2], ['2s', 2], ['2p', 6], ['3s', 2], ['3p', 6], ['4s', 2], ['3d', 10], ['4p', 6]];
      if (orMode === 'lewis') {
        var orSym = xs('sym', 'Na'), orVal = clamp(Math.floor(num(X.val, 1)), 1, 8);
        s += pfxCls(178, 120, 52, 'rgba(108,99,245,.10)', ACC, 2.2) + tx(178, 136, orSym, ST, 'middle', true, 34);
        for (var li2 = 0; li2 < orVal; li2++) {
          var la2 = -Math.PI / 2 + li2 * Math.PI / 4;
          s += pfxCls(178 + 68 * Math.cos(la2), 120 + 68 * Math.sin(la2), 5, PT, 'none', 0);
        }
        s += tx(180, 226, 'ساختار لوئیس — ' + nm(orVal) + ' الکترون ظرفیت', MU, 'middle', true, 12);
        return wrap(s);
      }
      var orLeft = orZ, orBoxes = [];
      orSubs.forEach(function (sp) {
        if (orLeft <= 0) return;
        var take = Math.min(sp[1], orLeft); orLeft -= take;
        orBoxes.push([sp[0], sp[1], take]);
      });
      var orN = orBoxes.length, orPerRow = 3, orBW = 36, orAx = 34, orAy = 44;
      orBoxes.forEach(function (bx3, bi2) {
        var orRow = Math.floor(bi2 / orPerRow), orCol = bi2 % orPerRow, bxx = orAx + orCol * 100, byy = orAy + orRow * 66;
        var orbs = bx3[1] / 2, filled = Math.min(orbs, Math.ceil(bx3[2] / 2));
        s += tx(bxx, byy - 8, bx3[0], ST, 'start', true, 12);
        for (var oj = 0; oj < orbs; oj++) {
          var bx4 = bxx + oj * (orBW + 2), occ = bx3[2] - oj * 2;
          s += rc(bx4, byy, orBW, 24, occ > 0 ? 'rgba(108,99,245,.10)' : '#ffffff', ST, 1.4, 3);
          if (occ === 1) s += tx(bx4 + orBW / 2, byy + 17, '↑', ACC, 'middle', true, 12);
          else if (occ >= 2) { s += tx(bx4 + orBW / 2 - 7, byy + 17, '↑', ACC, 'middle', true, 12); s += tx(bx4 + orBW / 2 + 7, byy + 17, '↓', PT, 'middle', true, 12); }
        }
      });
      s += tx(180, 262, 'آرایش الکترونی اتم شمارهٔ ' + orZ, MU, 'middle', true, 12);
      return wrap(s);
    }
    if (t === 'axpunnett') {
      var puKind = xs('kind', 'punnett').toLowerCase();
      if (puKind === 'pedigree') {
        s += pfxPoly(f2(180) + ',34 ' + f2(196) + ',62 ' + f2(164) + ',62', ST === '' ? '#fff' : '#ffffff', ST, 1.8);
        s += pfxCls(180, 88, 14, 'rgba(108,99,245,.18)', ST, 1.8) + pfxCls(244, 88, 14, '#ffffff', ST, 1.8);
        s += lnr(180, 62, 180, 74, ST, 1.6) + lnr(180, 74, 244, 74, ST, 1.6) + lnr(244, 74, 244, 74, ST, 1.6) + lnr(180, 74, 180, 74, ST, 1.6);
        s += lnr(212, 74, 212, 102, ST, 1.6);
        s += lnr(196, 130, 236, 130, ST, 1.6) + lnr(216, 130, 216, 146, ST, 1.6);
        s += rc(216 - 13, 146, 26, 26, 'rgba(214,51,108,.25)', ST, 1.8) + pfxCls(180, 171, 13, '#ffffff', ST, 1.8) + pfxCls(252, 171, 13, 'rgba(214,51,108,.25)', ST, 1.8);
        s += lnr(196, 130, 196, 158, ST, 1.6) + lnr(236, 130, 236, 158, ST, 1.6);
        s += lnr(180, 102, 180, 108, ST, 1.6) + lnr(244, 102, 244, 108, ST, 1.6);
        s += tx(180, 230, 'مربع = پسر   ●  دایره = دختر   •  رنگی = بیمار', MU, 'middle', true, 11);
        s += tx(180, 254, 'شجره‌نامهٔ وراثت', ST, 'middle', true, 13);
        return wrap(s);
      }
      var puP1 = xs('p1', 'Aa'), puP2 = xs('p2', 'Aa'), puMode = xs('mode', 'genotype').toLowerCase();
      s += rc(42, 84, 60, 34, 'rgba(108,99,245,.16)', ACC, 1.8, 6) + tx(72, 106, puP1, ACC, 'middle', true, 15);
      s += rc(42, 126, 60, 34, 'rgba(39,196,168,.16)', GRN, 1.8, 6) + tx(72, 148, puP2, GRN, 'middle', true, 15);
      if (puP1.length !== 2) puP1 = 'Aa';
      if (puP2.length !== 2) puP2 = 'Aa';
      var puCells = [];
      for (var pa = 0; pa < 2; pa++) for (var pb = 0; pb < 2; pb++) {
        var g1 = puP1[pa], g2 = puP2[pb];
        puCells.push((g1 === g1.toUpperCase() ? g1 : g2) + (g1 === g1.toUpperCase() ? g2 : g1));
      }
      var puU = 54, puX0 = 148, puY0 = 84;
      for (var pg = 0; pg < 2; pg++) {
        s += tx(puX0 + pg * puU + puU / 2, puY0 - 8, puP2[pg], GRN, 'middle', true, 13);
        s += tx(puX0 - 14, puY0 + pg * puU + puU / 2 + 5, puP1[pg], ACC, 'middle', true, 13);
        for (var ph = 0; ph < 2; ph++) {
          var cell = puCells[pg * 2 + ph], filled = cell === (cell.toUpperCase());
          s += rc(puX0 + ph * puU, puY0 + pg * puU, puU, puU, filled ? 'rgba(214,51,108,.16)' : '#ffffff', ST, 1.8) + tx(puX0 + ph * puU + puU / 2, puY0 + pg * puU + puU / 2 + 6, cell, ST, 'middle', true, 16);
        }
      }
      s += tx(180, 238, puMode === 'ratio' ? 'نسبت فنوتیپی: ۳ : ۱' : ('تلاقی ' + puP1 + ' × ' + puP2), MU, 'middle', true, 12);
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
      var prLine = xs('line', 'فعولن فعولن فعولن فعل'), prMode = xs('mode', 'arkan').toLowerCase();
      var prDict = {
        'فعولن': '∪ − −', 'مفاعیلن': '∪ − − −', 'فاعلاتن': '− ∪ − −', 'مستفعلن': '− − ∪ −',
        'متفاعلن': '∪ − ∪ −', 'مفتعلن': '− ∪ ∪ −', 'مفاعلن': '∪ − ∪ −', 'فاعلن': '− ∪ −',
        'فع': '− ∪', 'فعل': '− −', 'فعول': '∪ − −', 'مفاعیل': '∪ − − −', 'فعولات': '∪ − − −'
      };
      var prArkan = prLine.split(/\s+/).filter(Boolean).slice(0, 8);
      var prY = 96, prX = 30, prStep = Math.min(96, 300 / Math.max(1, prArkan.length)), prLine2 = '';
      if (!prArkan.length) prArkan = ['فعولن'];
      prArkan.forEach(function (ar, ai2) {
        var sym = prDict[ar] || '∪ − −';
        var bw = prStep - 10, bx5 = prX + ai2 * prStep;
        s += rc(bx5, 44, bw, 40, 'rgba(108,99,245,.10)', ACC, 1.4, 5) + tx(bx5 + bw / 2, 70, ar, ST, 'middle', true, 13);
        s += rc(bx5, prY, bw, 34, '#ffffff', ST, 1.2, 5) + tx(bx5 + bw / 2, prY + 23, prMode === 'taqti' ? sym : sym, '#0d47a1', 'middle', true, 13);
        s += tx(bx5 + bw / 2, prY + 54, prMode === 'taqti' ? '' : 'رکن ' + nm(ai2 + 1), MU, 'middle', false, 9);
        prLine2 += sym + '   ';
      });
      s += tx(180, 226, prMode === 'taqti' ? 'تقطیع: ' + prLine2.trim() : 'هجاهای کوتاه (∪) و بلند (−)', MU, 'middle', true, 11);
      s += tx(180, 250, 'وزن شعر: ' + prArkan.join(' '), ST, 'middle', true, 12);
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
      var stfKind = xs('kind', 'staff').toLowerCase();
      if (stfKind === 'color') {
        var scCols = ['#e53935', '#fb8c00', '#fdd835', '#7cb342', '#43a047', '#00897b', '#00acc1', '#1e88e5', '#3949ab', '#8e24aa', '#d81b60', '#6d4c41'];
        var scCx = 180, scCy = 136, scR = 96, scIn = 46;
        for (var si3 = 0; si3 < 12; si3++) {
          var a0 = -Math.PI / 2 + si3 * Math.PI / 6, a1 = a0 + Math.PI / 6;
          s += pfxPath('M ' + f2(scCx + scIn * Math.cos(a0)) + ' ' + f2(scCy + scIn * Math.sin(a0)) + ' L ' + f2(scCx + scR * Math.cos(a0)) + ' ' + f2(scCy + scR * Math.sin(a0)) + ' A ' + scR + ' ' + scR + ' 0 0 1 ' + f2(scCx + scR * Math.cos(a1)) + ' ' + f2(scCy + scR * Math.sin(a1)) + ' L ' + f2(scCx + scIn * Math.cos(a1)) + ' ' + f2(scCy + scIn * Math.sin(a1)) + ' A ' + scIn + ' ' + scIn + ' 0 0 0 ' + f2(scCx + scIn * Math.cos(a0)) + ' ' + f2(scCy + scIn * Math.sin(a0)) + ' Z', scCols[si3], '#ffffff', 1.4);
        }
        s += pfxCls(scCx, scCy, scIn, '#ffffff', ST, 1.6);
        s += tx(180, 254, 'چرخهٔ ۱۲ رنگ ایتن', MU, 'middle', true, 12);
        return wrap(s);
      }
      var stfClef = xs('clef', 'treble').toLowerCase();
      var stfNotes = String(xs('notes', '60,62,64,65,67')).split(/[,،]/).map(function (q9) { return parseInt(faEn(q9.trim()), 10); }).filter(function (q9) { return isFinite(q9); }).slice(0, 8);
      if (!stfNotes.length) stfNotes = [60, 62, 64, 65, 67];
      // V322 — شمارهٔ نت نامعتبر نُت را بیرون از حامل می‌کشید.
      stfNotes = stfNotes.map(function (nst) { return clamp(nst, 53, 74); });
      var stX0 = 40, stX1 = 330, stTop = 84, stGap = 14;
      for (var sl = 0; sl < 5; sl++) s += lnr(stX0, stTop + sl * stGap, stX1, stTop + sl * stGap, ST, 1.4);
      s += pfxPath('M ' + stX0 + ' ' + (stTop + 4 * stGap) + ' V ' + stTop, ST, 'none', 2);
      if (stfClef === 'bass') {
        s += tx(52, 148, '𝄢', ST, 'start', true, 40);
      } else {
        s += tx(50, 150, '𝄞', ST, 'start', true, 42);
      }
      var stfNames = { 60: 'دو', 62: 'ر', 64: 'می', 65: 'فا', 67: 'سل', 69: 'لا', 71: 'سی', 72: 'دوٴ', 59: 'سی' };
      stfNotes.forEach(function (nn, ni2) {
        var nx2 = 96 + ni2 * Math.min(32, 210 / Math.max(1, stfNotes.length - 1) || 30);
        var step2 = (nn - 60), yy6 = stTop + 4 * stGap - step2 * (stGap / 2);
        s += pfxCls(nx2, yy6, 6.2, ST, 'none', 0) + lnr(nx2 + 5, yy6, nx2 + 5, yy6 - 30, ST, 1.4);
        s += tx(nx2, 200, stfNames[nn] || nm(nn), MU, 'middle', false, 10);
      });
      s += tx(180, 240, stfClef === 'bass' ? 'خطوط حامل — کلید فا' : 'خطوط حامل — کلید سل', ST, 'middle', true, 13);
      return wrap(s);
    }
    // ============================== V318 — بستهٔ جامع ابزارهای آموزشی (۵) دانشگاه
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
      s += pfxArrow(dfPos[dfStart] ? dfPos[dfStart][0] - dfR - 34 : 30, dfCy, dfPos[dfStart] ? dfPos[dfStart][0] - dfR - 4 : 60, dfCy, ST, 1.6, 8);
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
          s += pfxPath('M ' + f2(p1[0] - 12) + ',' + f2(p1[1] - dfR) + ' A 18 18 0 1 1 ' + f2(p1[0] + 12) + ',' + f2(p1[1] - dfR), 'none', ACC, 1.6);
          s += tx(p1[0], p1[1] - dfR - 18, lab3, ACC, 'middle', true, 10);
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
        if (node.l) { s += lnr(x6, y6, x6 - span, y6 + bsRowH, ST, 1.4); bsDraw(node.l, x6 - span, d3 + 1, span / 2); }
        if (node.r) { s += lnr(x6, y6, x6 + span, y6 + bsRowH, ST, 1.4); bsDraw(node.r, x6 + span, d3 + 1, span / 2); }
        s += pfxCls(x6, y6, 15, 'rgba(108,99,245,.16)', ACC, 1.8) + tx(x6, y6 + 5, String(node.v), ST, 'middle', true, 11);
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
      s += tx(mhPx(mhC - mhR) - 3, mhCy - 8, 'σ₂ = ' + mhSgn(mhC - mhR), '#00796b', 'end', true, 10);
      s += tx(mhPx(mhC + mhR) + 3, mhCy - 8, 'σ₁ = ' + mhSgn(mhC + mhR), '#00796b', 'start', true, 10);
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
        s += pfxArrow(sxx, syy, exx, eyy, ACC, 1.8, 8) + tx((sxx + exx) / 2, (syy + eyy) / 2 - 7, a5.name + ' (' + nm(a5.dur) + ')', '#0d47a1', 'middle', true, 10);
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
      s += tx(spX0 + 22, spY0 - 34, 'تقاضا', PT, 'start', true, 11) + tx(spX1 - 30, spY1 + 60, 'عرضه', '#00796b', 'end', true, 11);
      if (spKind === 'shift') {
        s += pfxArrow(spX0 + 60, spDem(0.2), spX1 - 60, spY0 - 162, 'rgba(214,51,108,.45)', 2, 8) + tx(240, 72, 'تقاضای جدید', PT, 'middle', false, 10);
      }
      if (spCeil > 0) {
        var cy4 = clamp(spY0 - 40 - spCeil * 12, spY1 + 40, spY0 - 30);
        s += lnr(spX0, cy4, spX1 - 10, cy4, ACC, 1.8, '6 4') + tx(spX1 - 14, cy4 - 6, 'سقف قیمت', ACC, 'end', true, 10);
      }
      s += pfxCls(spX0 + 118, spY0 - 118, 5, ST, 'none', 0) + tx(spX0 + 130, spY0 - 122, 'تعادل', ST, 'start', true, 11);
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

    if (t === 'axunitconv') {
      var uck = xs('kind', 'len'), ucv = num(X.val, 1);
      var ucn = uck === 'mass' ? ['تن', 'کیلوگرم', 'گرم'] : uck === 'area' ? ['متر مربع', 'دسی‌متر مربع', 'سانتی‌متر مربع'] : ['کیلومتر', 'متر', 'میلی‌متر'];
      var ucf = uck === 'area' ? 100 : 1000, uci, ucy;
      for (uci = 0; uci < 3; uci++) {
        ucy = 44 + uci * 66;
        s += rc(56, ucy, 176, 46, uci === 1 ? ACCS : '#ffffff', ST, 1.6, 10);
        s += v319txt(144, ucy + 20, ucn[uci], ST, 'middle', true, 13) + v319txt(144, ucy + 39, nm(Math.round(ucv * Math.pow(ucf, uci) * 1000) / 1000), ACC, 'middle', true, 13);
        s += v319txt(240, ucy + 27, '×' + ucf, MU, 'start', false, 10);
        if (uci < 2) {
          s += lnr(144, ucy + 46, 144, ucy + 62, PT, 1.6) + poly(f2(139.5) + ',' + f2(ucy + 54) + ' ' + f2(148.5) + ',' + f2(ucy + 54) + ' ' + f2(144) + ',' + f2(ucy + 64), PT);
          s += v319txt(154, ucy + 60, '× ' + ucf, PT, 'start', true, 11);
        }
      }
      s += v319txt(180, 262, 'هر پله ×' + ucf + ' — برگشت ÷' + ucf, MU, 'middle', true, 12);
      return wrap(s);
    }

    if (t === 'axperimeter') {
      var psk = xs('shape', 'rect'), pw = Math.max(1, num(X.w, 6)), ph = Math.max(1, num(X.h, 4));
      var pp = 0, pa = 0, pform = '';
      if (psk === 'circle') {
        var prad = Math.min(84, 15 * pw);
        s += v319circ(180, 130, prad, 'rgba(108,99,245,.16)', ACC, 2.2);
        s += lnr(180, 130, 180 + prad, 130, PT, 1.6) + dot(180, 130, PT, 3.2) + v319txt(180 + prad / 2, 122, 'r = ' + nm(pw), PT, 'middle', true, 11);
        pp = 2 * Math.PI * pw; pa = Math.PI * pw * pw; pform = 'محیط = ۲πr  ·  مساحت = πr²';
      } else if (psk === 'tri') {
        var tx0 = 100, ty0 = 206, tw = Math.min(180, 20 * pw), th = Math.min(120, 20 * ph);
        s += v319poly(f2(tx0) + ',' + f2(ty0) + ' ' + f2(tx0 + tw) + ',' + f2(ty0) + ' ' + f2(tx0) + ',' + f2(ty0 - th), 'rgba(39,196,168,.16)', ACC, 2.2);
        s += rc(tx0, ty0 - 12, 12, 12, 'none', ST, 1.2);
        s += v319txt(tx0 + tw / 2, ty0 + 16, nm(pw), MU, 'middle', true, 11) + v319txt(tx0 - 8, ty0 - th / 2, nm(ph), MU, 'end', true, 11);
        pp = pw + ph + Math.sqrt(pw * pw + ph * ph); pa = pw * ph / 2; pform = 'مساحت مثلث = ½ × قاعده × ارتفاع';
      } else if (psk === 'poly') {
        var pn = Math.max(3, Math.min(8, num(X.n, 5))), pcx = 180, pcy = 126, pr2 = 76, ppv = '';
        for (i = 0; i < pn; i++) { var pa2 = -Math.PI / 2 + i * 2 * Math.PI / pn; ppv += f2(pcx + pr2 * Math.cos(pa2)) + ',' + f2(pcy + pr2 * Math.sin(pa2)) + ' '; }
        s += v319poly(ppv.trim(), 'rgba(240,162,2,.16)', GLD, 2.2);
        var pside = 2 * pr2 * Math.sin(Math.PI / pn) / 14;
        s += v319txt(180, 222, pn + ' ضلع', ST, 'middle', true, 12);
        pp = pn * pside; pa = 0.5 * pn * pside * pside / (4 * Math.tan(Math.PI / pn)); pform = 'چندضلعی منتظم — محیط = n × ضلع';
      } else {
        var rx0 = 180 - Math.min(110, 15 * pw), ry0 = 120 - Math.min(60, 15 * ph), rw = Math.min(220, 30 * pw), rh = Math.min(120, 30 * ph);
        rx0 = 180 - rw / 2; ry0 = 126 - rh / 2;
        s += rc(rx0, ry0, rw, rh, 'rgba(108,99,245,.16)', ACC, 2.2);
        s += v319txt(180, ry0 + rh + 18, 'عرض = ' + nm(pw), MU, 'middle', true, 11) + v319txt(rx0 - 8, ry0 + rh / 2 + 4, 'ارتفاع = ' + nm(ph), MU, 'end', true, 11);
        pp = 2 * (pw + ph); pa = pw * ph; pform = 'محیط = ۲(طول + عرض)  ·  مساحت = طول × عرض';
      }
      s += v319txt(180, 30, pform, MU, 'middle', false, 10);
      s += v319txt(180, 244, 'محیط ≈ ' + nm(Math.round(pp * 100) / 100), ST, 'middle', true, 13);
      s += v319txt(180, 262, 'مساحت ≈ ' + nm(Math.round(pa * 100) / 100), ACC, 'middle', true, 13);
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
      var rdN = num(X.n, 147), rdTen = xs('mode', 'ten') !== 'hundred';
      var rdStep = rdTen ? 10 : 100;
      var rdLo = Math.floor(rdN / rdStep) * rdStep, rdHi = rdLo + rdStep;
      var rdL2 = rdLo - rdStep, rdH2 = rdHi + rdStep;
      var rdX = function (v) { return 60 + (v - rdL2) / (rdH2 - rdL2) * 240; };
      var rdy = 150;
      s += lnr(52, rdy, 320, rdy, ST, 2);
      [rdL2, rdLo, rdHi, rdH2].forEach(function (v) { s += lnr(rdX(v), rdy - 7, rdX(v), rdy + 7, ST, 1.5) + v319txt(rdX(v), rdy + 22, nm(v), MU, 'middle', false, 10); });
      s += oDot(rdX(rdLo), rdy) + oDot(rdX(rdHi), rdy);
      s += dot(rdX(rdN), rdy - 26, PT, 5) + v319txt(rdX(rdN), rdy - 36, nm(rdN), PT, 'middle', true, 12);
      var rdNear = Math.abs(rdN - rdLo) <= Math.abs(rdHi - rdN) ? rdLo : rdHi;
      s += v319path('M ' + f2(rdX(rdN)) + ' ' + f2(rdy - 20) + ' Q ' + f2((rdX(rdN) + rdX(rdNear)) / 2) + ' ' + f2(rdy - 52) + ' ' + f2(rdX(rdNear)) + ' ' + f2(rdy - 8), 'none', ACC, 1.8, '5 4');
      s += v319txt(180, 218, nm(rdN) + ' ≈ ' + nm(rdNear) + '   (گرد به ' + (rdTen ? 'دهگان' : 'صدگان') + ')', ST, 'middle', true, 13);
      s += v319txt(180, 240, 'اختلاف تا ' + nm(rdLo) + ' = ' + nm(Math.abs(rdN - rdLo)) + '  ·  تا ' + nm(rdHi) + ' = ' + nm(Math.abs(rdHi - rdN)), MU, 'middle', false, 10);
      s += v319txt(180, 262, 'رقم گردکردن: ' + nm(rdStep), MU, 'middle', false, 10);
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
      var cpShow = xs('show', 'all'), cpOn = function (k2) { return cpShow === 'all' || cpShow === k2; };
      var cpc = { x: 180, y: 138, r: 86 };
      s += v319circ(cpc.x, cpc.y, cpc.r, 'rgba(108,99,245,.06)', ACC, 2.2) + dot(cpc.x, cpc.y, ST, 4);
      s += v319txt(cpc.x, cpc.y + 16, 'O', ST, 'middle', true, 11);
      if (cpOn('radius')) { s += lnr(cpc.x, cpc.y, cpc.x + cpc.r * Math.cos(-Math.PI / 4), cpc.y + cpc.r * Math.sin(-Math.PI / 4), PT, 2); s += v319txt(cpc.x + 36, cpc.y - 30, 'شعاع r', PT, 'start', true, 11); }
      if (cpOn('diameter')) { s += lnr(cpc.x - cpc.r, cpc.y, cpc.x + cpc.r, cpc.y, '#1f8f7a', 2); s += v319txt(cpc.x - 62, cpc.y - 8, 'قطر 2r', '#1f8f7a', 'middle', true, 11); }
      if (cpOn('chord')) { s += lnr(cpc.x - 66, cpc.y + 46, cpc.x + 30, cpc.y + 74, GLD, 2); s += v319txt(cpc.x - 34, cpc.y + 92, 'وتر', GLD, 'middle', true, 11); }
      if (cpOn('tangent')) { s += lnr(cpc.x - 96, cpc.y + cpc.r, cpc.x + 96, cpc.y + cpc.r, '#8e44ad', 2, '5 4'); s += v319txt(cpc.x + 92, cpc.y + cpc.r + 14, 'خط مماس', '#8e44ad', 'end', true, 11); }
      if (cpOn('arc')) { s += v319path('M ' + f2(cpc.x + cpc.r * Math.cos(Math.PI * 1.15)) + ' ' + f2(cpc.y + cpc.r * Math.sin(Math.PI * 1.15)) + ' A ' + cpc.r + ' ' + cpc.r + ' 0 0 1 ' + f2(cpc.x + cpc.r * Math.cos(Math.PI * 1.75)) + ' ' + f2(cpc.y + cpc.r * Math.sin(Math.PI * 1.75)), 'none', PT, 3.4); s += v319txt(cpc.x - 66, cpc.y - 40, 'کمان', PT, 'end', true, 11); }
      s += v319txt(180, 250, 'اجزای دایره — مرکز، شعاع، قطر، وتر، مماس و کمان', MU, 'middle', true, 11);
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
      var lm1 = num(X.m1, 1), lb1 = num(X.b1, 2), lm2 = num(X.m2, -1), lb2 = num(X.b2, 4);
      var lox = 180, loy = 148, lsc = 20, lgx = 0;
      for (lgx = -6; lgx <= 6; lgx++) {
        s += lnr(lox + lgx * lsc, 44, lox + lgx * lsc, 250, GR, 0.7);
        if (lgx !== 0) s += v319txt(lox + lgx * lsc, loy + 14, String(lgx), MU, 'middle', false, 9);
      }
      for (lgx = -5; lgx <= 5; lgx++) {
        s += lnr(52, loy - lgx * lsc, 320, loy - lgx * lsc, GR, 0.7);
        if (lgx !== 0) s += v319txt(lox - 12, loy - lgx * lsc + 3, String(lgx), MU, 'end', false, 9);
      }
      s += lnr(46, loy, 322, loy, ST, 1.8) + arR(322, loy) + lnr(lox, 254, lox, 42, ST, 1.8) + arU(lox, 42);
      s += v319txt(326, loy + 14, 'x', ST, 'middle', true, 12) + v319txt(lox - 12, 40, 'y', ST, 'middle', true, 12);
      var lpx = function (v3) { return lox + v3 * lsc; }, lpy = function (v3) { return loy - v3 * lsc; };
      // V322 — با شیب/عرض از مبدأ بزرگ، سرِ خطوط بیرون از بوم می‌افتاد؛ حالا در کادر برش می‌خورند.
      var lpcx = function (v3) { return clamp(lpx(v3), 46, 326); }, lpcy = function (v3) { return clamp(lpy(v3), 44, 254); };
      s += lnr(lpcx(-6), lpcy(lm1 * -6 + lb1), lpcx(6), lpcy(lm1 * 6 + lb1), ACC, 2.2);
      s += lnr(lpcx(-6), lpcy(lm2 * -6 + lb2), lpcx(6), lpcy(lm2 * 6 + lb2), PT, 2.2);
      var lden = lm1 - lm2, lix = Math.abs(lden) < 1e-6 ? 0 : (lb2 - lb1) / lden, liy = lm1 * lix + lb1;
      if (Math.abs(lden) > 1e-6) {
        s += dot(clamp(lpx(lix), 50, 322), clamp(lpy(liy), 48, 250), '#1f8f7a', 5.5);
        s += v319txt(clamp(lpx(lix) + 8, 60, 300), clamp(lpy(liy) - 10, 56, 240), '(' + nm(Math.round(lix * 100) / 100) + ', ' + nm(Math.round(liy * 100) / 100) + ')', '#1f8f7a', 'start', true, 11);
        s += v319txt(180, 268, 'نقطهٔ تقاطع — جواب دستگاه', '#1f8f7a', 'middle', true, 11);
      } else {
        s += v319txt(180, 268, 'دو خط موازی‌اند — دستگاه جواب ندارد', PT, 'middle', true, 11);
      }
      s += v319txt(70, 56, 'y = ' + nm(lm1) + 'x + ' + nm(lb1), ACC, 'start', true, 11);
      s += v319txt(70, 74, 'y = ' + nm(lm2) + 'x + ' + nm(lb2), PT, 'start', true, 11);
      return wrap(s);
    }

    if (t === 'axbarchart') {
      var bcData = ilist('data', '4,7,3,6,5'), bcLabs = xl('labs');
      if (!bcData.length) bcData = [4, 7, 3, 6, 5];
      // V322 — دادهٔ منفی ارتفاع منفی می‌ساخت؛ به بازهٔ مجاز محدود می‌شود.
      bcData = bcData.map(function (vbc) { return clamp(vbc, 0, 999); });
      var bcShow = xs('show', 'mean'), bcN = bcData.length, bcMax = 1, bci;
      for (bci = 0; bci < bcN; bci++) bcMax = Math.max(bcMax, bcData[bci]);
      var bcX0 = 66, bcY0 = 216, bcW = 260, bcH = 150, bcBw = Math.min(34, bcW / (bcN * 1.6));
      s += lnr(bcX0 - 8, bcY0, bcX0 + bcW, bcY0, ST, 1.8) + lnr(bcX0 - 8, bcY0, bcX0 - 8, bcY0 - bcH - 8, ST, 1.8);
      for (bci = 0; bci < bcN; bci++) {
        var bcbx = bcX0 + 16 + bci * (bcW - 20) / bcN, bcbh = bcData[bci] / bcMax * bcH;
        s += rc(bcbx, bcY0 - bcbh, bcBw, bcbh, bci % 2 ? 'rgba(39,196,168,.75)' : 'rgba(108,99,245,.75)', '#ffffff', 1.2, 4);
        s += v319txt(bcbx + bcBw / 2, bcY0 - bcbh - 6, String(bcData[bci]), ST, 'middle', true, 10);
        s += v319txt(bcbx + bcBw / 2, bcY0 + 16, bcLabs[bci] || ('مورد ' + (bci + 1)), MU, 'middle', false, 10);
      }
      var bcSum = 0, bcSorted = bcData.slice().sort(function (p1, p2) { return p1 - p2; });
      for (bci = 0; bci < bcN; bci++) bcSum += bcData[bci];
      var bcMean = bcSum / bcN, bcMed = bcN % 2 ? bcSorted[(bcN - 1) / 2] : (bcSorted[bcN / 2 - 1] + bcSorted[bcN / 2]) / 2;
      if (bcShow === 'mean') {
        var bcy = bcY0 - bcMean / bcMax * bcH;
        s += lnr(bcX0 - 4, bcy, bcX0 + bcW, bcy, PT, 1.8, '6 4') + v319txt(bcX0 + bcW, bcy - 6, 'میانگین ' + nm(Math.round(bcMean * 100) / 100), PT, 'end', true, 10);
      }
      s += v319txt(180, 248, 'میانگین = ' + nm(Math.round(bcMean * 100) / 100) + '  ·  میانه = ' + nm(bcMed) + '  ·  بیشترین = ' + nm(bcMax), ST, 'middle', true, 11);
      s += v319txt(180, 268, 'نمودار میله‌ای داده‌های آماری', MU, 'middle', false, 10);
      return wrap(s);
    }

    if (t === 'axparabola') {
      var pqa = num(X.a, 1) || 1, pqb = num(X.b, -2), pqc = num(X.c, -3);
      var pox = 180, poy = 150, psc = 19, pg;
      for (pg = -6; pg <= 6; pg++) s += lnr(pox + pg * psc, 34, pox + pg * psc, 258, GR, 0.6) + lnr(48, poy - pg * psc, 322, poy - pg * psc, GR, 0.6);
      s += lnr(42, poy, 322, poy, ST, 1.8) + arR(322, poy) + lnr(pox, 262, pox, 30, ST, 1.8) + arU(pox, 30);
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
      s += v319txt(180, 268, proot + '  ·  Δ = ' + nm(pdel), MU, 'middle', false, 10);
      s += v319txt(66, 44, 'y = ' + nm(pqa) + 'x² + ' + nm(pqb) + 'x + ' + nm(pqc), ACC, 'start', true, 12);
      return wrap(s);
    }

    if (t === 'axinthales') {
      var itDeg = Math.max(20, Math.min(160, num(X.ang, 60))), itcx = 180, itcy = 150, itr = 88;
      var ita0 = -Math.PI / 2 - itDeg * Math.PI / 360, ita1 = -Math.PI / 2 + itDeg * Math.PI / 360;
      var itAx = itcx + itr * Math.cos(ita0), itAy = itcy + itr * Math.sin(ita0);
      var itBx = itcx + itr * Math.cos(ita1), itBy = itcy + itr * Math.sin(ita1);
      var itCx = itcx + itr * Math.cos(ita1 + (2 * Math.PI - itDeg * Math.PI / 180) / 2), itCy = itcy + itr * Math.sin(ita1 + (2 * Math.PI - itDeg * Math.PI / 180) / 2);
      s += v319circ(itcx, itcy, itr, 'rgba(108,99,245,.05)', ACC, 2);
      s += v319path('M ' + f2(itAx) + ' ' + f2(itAy) + ' A ' + itr + ' ' + itr + ' 0 0 1 ' + f2(itBx) + ' ' + f2(itBy), 'none', PT, 3.6);
      s += lnr(itcx, itcy, itAx, itAy, ST, 1.8) + lnr(itcx, itcy, itBx, itBy, ST, 1.8);
      s += lnr(itCx, itCy, itAx, itAy, GLD, 1.8) + lnr(itCx, itCy, itBx, itBy, GLD, 1.8);
      s += dot(itcx, itcy, ST, 4) + dot(itAx, itAy, PT, 4.5) + dot(itBx, itBy, PT, 4.5) + dot(itCx, itCy, GLD, 4.5);
      s += v319txt(itcx, itcy + 18, 'O', ST, 'middle', true, 11);
      s += v319txt(itAx - 10, itAy - 6, 'A', PT, 'end', true, 11) + v319txt(itBx + 10, itBy - 6, 'B', PT, 'start', true, 11);
      s += v319txt(itCx, itCy + (itCy < itcy ? -10 : 18), 'C', GLD, 'middle', true, 11);
      s += v319txt(180, 40, 'زاویهٔ مرکزی = ' + nm(itDeg) + '°', PT, 'middle', true, 12);
      s += v319txt(180, 250, 'زاویهٔ محاطی = نصف زاویهٔ مرکزی = ' + nm(Math.round(itDeg / 2 * 10) / 10) + '°', GLD, 'middle', true, 12);
      s += v319txt(180, 268, 'زاویه‌های محاطی روی یک کمان با هم برابرند', MU, 'middle', false, 10);
      return wrap(s);
    }

    if (t === 'axpendulum') {
      var pdL = Math.max(0.1, num(X.len, 1)), pdAmp = Math.max(5, Math.min(60, num(X.amp, 20)));
      var pdT = 2 * Math.PI * Math.sqrt(pdL / 9.8), pdx0 = 180, pdy0 = 46, pdLen = 150;
      var pdTh = pdAmp * Math.PI / 180, pdbx = pdx0 + pdLen * Math.sin(pdTh), pdby = pdy0 + pdLen * Math.cos(pdTh);
      s += rc(150, 34, 60, 12, ST, ST, 1, 5) + v319txt(180, 30, 'تکیه‌گاه', MU, 'middle', false, 10);
      s += v319path('M ' + pdx0 + ' ' + pdy0 + ' A ' + pdLen + ' ' + pdLen + ' 0 0 1 ' + f2(pdx0 + pdLen) + ' ' + pdy0, 'none', GR, 1.4, '5 4');
      s += lnr(pdx0, pdy0, pdbx, pdby, ST, 2);
      s += v319circ(pdbx, pdby, 12, 'rgba(214,51,108,.85)', PT, 1.4) + dot(pdx0, pdy0, ST, 3.4);
      s += v319path('M ' + f2(pdbx) + ' ' + f2(pdby) + ' L ' + f2(pdbx) + ' ' + f2(pdy0 + pdLen), 'none', MU, 1.2, '4 4');
      s += v319path('M ' + f2(pdbx) + ' ' + f2(pdy0 + pdLen) + ' A ' + f2(pdLen) + ' ' + f2(pdLen) + ' 0 0 1 ' + f2(pdx0 + pdLen) + ' ' + f2(pdy0 + pdLen - 0.1), 'none', MU, 1, '3 4');
      s += v319txt(pdx0 + 8, pdy0 + pdLen / 2, 'L = ' + nm(pdL) + ' m', PT, 'start', true, 11);
      s += v319txt(250, pdy0 + 40, 'θ = ' + nm(pdAmp) + '°', GLD, 'start', true, 11);
      s += v319txt(180, 236, 'دورهٔ آونگ T = ۲π√(L / g) ≈ ' + nm(Math.round(pdT * 100) / 100) + ' s', ST, 'middle', true, 13);
      s += v319txt(180, 258, 'g ≈ ۹٫۸ m/s²   ·   مستقل از جرم گلوله', MU, 'middle', false, 10);
      return wrap(s);
    }

    if (t === 'axincline') {
      var icAng = Math.max(10, Math.min(60, num(X.ang, 30))), icM = Math.max(1, num(X.mass, 5));
      var icAx = 74, icAy = 210, icLen = 220, icRad = icAng * Math.PI / 180;
      var icBx = icAx + icLen, icBy = icAy, icCx = icAx + icLen * Math.cos(icRad), icCy = icAy - icLen * Math.sin(icRad);
      s += v319poly(f2(icAx) + ',' + f2(icAy) + ' ' + f2(icBx) + ',' + f2(icBy) + ' ' + f2(icCx) + ',' + f2(icCy), 'rgba(108,99,245,.10)', ACC, 2);
      var icT = 0.55, icpx = icAx + icLen * icT * Math.cos(icRad), icpy = icAy - icLen * icT * Math.sin(icRad);
      s += v319poly(f2(icpx - 9) + ',' + f2(icpy - 20) + ' ' + f2(icpx + 11) + ',' + f2(icpy - 6) + ' ' + f2(icpx + 2) + ',' + f2(icpy + 3) + ' ' + f2(icpx - 18) + ',' + f2(icpy - 11), 'rgba(240,162,2,.9)', '#8a5b00', 1.2);
      var icW = icM * 9.8, icSin = Math.sin(icRad), icCos = Math.cos(icRad);
      s += lnr(icpx, icpy, icpx, icpy + 34, PT, 1.8) + poly(f2(icpx - 4.5) + ',' + f2(icpy + 26) + ' ' + f2(icpx + 4.5) + ',' + f2(icpy + 26) + ' ' + f2(icpx) + ',' + f2(icpy + 34), PT);
      s += v319txt(icpx, icpy + 48, 'mg = ' + nm(Math.round(icW * 10) / 10) + ' N', PT, 'middle', true, 10);
      var icDx = -icSin * 40, icDy = icCos * 40;
      s += lnr(icpx, icpy, icpx - icSin * 44, icpy + icCos * 44, GLD, 1.8) + poly(f2(icpx - icSin * 44 - 4) + ',' + f2(icpy + icCos * 44 - 3) + ' ' + f2(icpx - icSin * 44 + 4) + ',' + f2(icpy + icCos * 44 + 3) + ' ' + f2(icpx - icSin * 44 + 1) + ',' + f2(icpy + icCos * 44 + 9), GLD);
      s += v319txt(icpx - icSin * 62 - 14, icpy + icCos * 62 + 24, 'mg·sinθ = ' + nm(Math.round(icW * icSin * 10) / 10) + ' N', GLD, 'middle', true, 10);
      s += lnr(icpx, icpy, icpx - icSin * 40, icpy - icCos * 40, '#1f8f7a', 1.8);
      s += v319txt(icpx - icSin * 54 - 6, icpy - icCos * 54 - 8, 'N = ' + nm(Math.round(icW * icCos * 10) / 10) + ' N', '#1f8f7a', 'middle', true, 10);
      s += v319path('M ' + f2(icAx + 34) + ' ' + f2(icAy) + ' A 34 34 0 0 0 ' + f2(icAx + 34 * Math.cos(icRad)) + ' ' + f2(icAy - 34 * Math.sin(icRad)), 'none', ST, 1.4);
      s += v319txt(icAx + 46, icAy - 8, 'θ = ' + nm(icAng) + '°', ST, 'start', true, 11);
      s += v319txt(180, 236, 'نیروی موازی با سطح: F = mg·sinθ', ST, 'middle', true, 12);
      s += v319txt(180, 258, 'نیروی عمود بر سطح: N = mg·cosθ   ·   m = ' + nm(icM) + ' kg', MU, 'middle', false, 10);
      return wrap(s);
    }

    if (t === 'axcircuit') {
      var cmMode = xs('mode', 'series'), cr1 = Math.max(0.5, num(X.r1, 4)), cr2 = Math.max(0.5, num(X.r2, 6)), cv = Math.max(0.1, num(X.v, 12));
      var creq = cmMode === 'parallel' ? (cr1 * cr2) / (cr1 + cr2) : cr1 + cr2, ci = cv / creq;
      s += rc(46, 96, 16, 84, '#ffffff', ST, 1.8, 3);
      s += lnr(46, 110, 62, 110, ST, 1.6) + lnr(46, 166, 62, 166, ST, 1.6);
      s += lnr(54, 120, 54, 156, ST, 1.4) + lnr(48, 124, 60, 124, ST, 1.4) + lnr(54, 134, 54, 142, ST, 1.4);
      s += v319txt(38, 142, 'V=' + nm(cv) + 'V', MU, 'middle', true, 10);
      if (cmMode === 'parallel') {
        s += lnr(62, 110, 320, 110, ST, 1.8) + lnr(62, 166, 320, 166, ST, 1.8);
        s += lnr(320, 110, 320, 166, ST, 1.8);
        s += rc(128, 96, 64, 28, 'rgba(108,99,245,.16)', ACC, 1.8, 4) + v319txt(160, 114, 'R₁=' + nm(cr1) + 'Ω', ST, 'middle', true, 11);
        s += rc(128, 152, 64, 28, 'rgba(39,196,168,.18)', '#1f8f7a', 1.8, 4) + v319txt(160, 170, 'R₂=' + nm(cr2) + 'Ω', ST, 'middle', true, 11);
        s += v319txt(240, 102, 'I₁=' + nm(Math.round(cv / cr1 * 100) / 100) + 'A', ACC, 'start', true, 10);
        s += v319txt(240, 186, 'I₂=' + nm(Math.round(cv / cr2 * 100) / 100) + 'A', '#1f8f7a', 'start', true, 10);
      } else {
        s += lnr(62, 110, 120, 110, ST, 1.8) + lnr(120, 96, 188, 96, ST, 1.8) + lnr(188, 110, 320, 110, ST, 1.8);
        s += lnr(62, 166, 320, 166, ST, 1.8) + lnr(320, 110, 320, 166, ST, 1.8);
        s += rc(120, 82, 68, 28, 'rgba(108,99,245,.16)', ACC, 1.8, 4) + v319txt(154, 100, 'R₁=' + nm(cr1) + 'Ω', ST, 'middle', true, 11);
        s += rc(212, 82, 68, 28, 'rgba(39,196,168,.18)', '#1f8f7a', 1.8, 4) + v319txt(246, 100, 'R₂=' + nm(cr2) + 'Ω', ST, 'middle', true, 11);
        s += lnr(188, 96, 212, 96, ST, 1.8);
        s += v319txt(262, 138, 'I = ' + nm(Math.round(ci * 100) / 100) + ' A', ST, 'middle', true, 11);
      }
      s += v319txt(180, 212, (cmMode === 'parallel' ? 'مدار موازی' : 'مدار سری') + '  ·  R کل = ' + nm(Math.round(creq * 100) / 100) + ' Ω', ST, 'middle', true, 13);
      s += v319txt(180, 234, 'جریان کل I = V ÷ R ≈ ' + nm(Math.round(ci * 100) / 100) + ' A', ACC, 'middle', true, 12);
      s += v319txt(180, 256, cmMode === 'parallel' ? 'در موازی: ۱/R = ۱/R₁ + ۱/R₂ و ولتاژها برابر' : 'در سری: R = R₁ + R₂ و جریان‌ها برابر', MU, 'middle', false, 10);
      return wrap(s);
    }

    if (t === 'axoptics') {
      var opF = Math.max(2, num(X.f, 10)), opD = Math.max(1, num(X.d, 30)), opKind = xs('kind', 'convex');
      var opCx = 190, opCy = 152, opS = 2.4;
      // V322 — d = f مخرج را صفر می‌کرد و «Infinity» در SVG می‌نشست؛ حالا حالت «تصویر در بی‌نهایت» صریح است.
      var opAtFocus = opKind !== 'concave' && Math.abs(opD - opF) < 0.05;
      var opDi = opAtFocus ? Infinity : ((opKind === 'concave') ? (opF * opD) / (opD + opF) : (opF * opD) / (opD - opF));
      var opFp = clamp(opF * opS, 12, 120);
      s += lnr(36, opCy, 330, opCy, ST, 1.6) + arR(330, opCy);
      s += v319path('M ' + opCx + ' ' + (opCy - 74) + ' Q ' + (opCx + (opKind === 'concave' ? 22 : 22)) + ' ' + opCy + ' ' + opCx + ' ' + (opCy + 74), 'none', ACC, 2.4);
      if (opKind === 'concave') s += v319path('M ' + opCx + ' ' + (opCy - 74) + ' Q ' + (opCx + 10) + ' ' + opCy + ' ' + opCx + ' ' + (opCy + 74), 'none', ACC, 2.4);
      s += v319txt(opCx + 26, opCy - 82, opKind === 'concave' ? 'عدسی مقعر' : 'عدسی محدب', ACC, 'middle', true, 11);
      var opFx = opCx + opFp;
      s += dot(opCx - opFp, opCy, PT, 3.4) + dot(opFx, opCy, PT, 3.4);
      s += v319txt(opCx - opFp, opCy + 16, 'F', PT, 'middle', true, 11) + v319txt(opFx, opCy + 16, "F'", PT, 'middle', true, 11);
      var opOx = clamp(opCx - opD * opS, 24, opCx - 12), opOh = 46;
      s += lnr(opOx, opCy, opOx, opCy - opOh, '#1f8f7a', 2.4) + poly(f2(opOx - 4) + ',' + f2(opCy - opOh + 8) + ' ' + f2(opOx + 4) + ',' + f2(opCy - opOh + 8) + ' ' + f2(opOx) + ',' + f2(opCy - opOh), '#1f8f7a');
      s += v319txt(opOx, opCy + 18, 'جسم', '#1f8f7a', 'middle', true, 10);
      if (!opAtFocus) {
        var opIx = clamp(opCx + opDi * opS, 28, 332), opIh = clamp(opOh * (opDi / opD), -120, 120);
        s += lnr(opIx, opCy, opIx, opCy + opIh, GLD, 2.4) + poly(f2(opIx - 4) + ',' + f2(opCy + opIh - 8) + ' ' + f2(opIx + 4) + ',' + f2(opCy + opIh - 8) + ' ' + f2(opIx) + ',' + f2(opCy + opIh), GLD);
        s += v319txt(opIx, opCy - 8, 'تصویر', GLD, 'middle', true, 10);
      } else {
        s += v319txt(180, 126, 'جسم روی کانون — تصویر در بی‌نهایت', PT, 'middle', true, 11);
      }
      s += lnr(opOx, opCy - opOh, opCx, opCy - opOh, MU, 1.1, '4 4') + lnr(opCx, opCy - opOh, opFx, opCy, MU, 1.1, '4 4');
      s += lnr(opOx, opCy - opOh, opCx, opCy, MU, 1.2);
      s += v319txt(180, 234, '۱/f = ۱/فاصلهٔ جسم + ۱/فاصلهٔ تصویر', ST, 'middle', true, 12);
      var opDiTxt = opAtFocus ? 'بی‌نهایت' : nm(Math.round(opDi * 100) / 100) + ' cm';
      var opMag = opAtFocus ? '—' : nm(Math.round(Math.abs(opDi / opD) * 100) / 100) + ' برابر';
      s += v319txt(180, 254, 'f = ' + nm(opF) + ' cm · d = ' + nm(opD) + ' cm → تصویر در ' + opDiTxt, ACC, 'middle', true, 11);
      s += v319txt(180, 272, 'بزرگنمایی = ' + opMag, MU, 'middle', false, 10);
      return wrap(s);
    }

    if (t === 'axheatcurve') {
      var hc = [[0, -10], [20, 0], [40, 0], [60, 100], [80, 100], [100, 110]];
      var hcX0 = 62, hcY0 = 214, hcW = 260, hcH = 160;
      var hcpx = function (u) { return hcX0 + u / 100 * hcW; }, hcpy = function (v6) { return hcY0 - (v6 + 15) / 130 * hcH; };
      s += lnr(hcX0 - 8, hcY0, hcX0 + hcW + 6, hcY0, ST, 1.6) + lnr(hcX0 - 8, hcY0, hcX0 - 8, hcY0 - hcH - 8, ST, 1.6);
      s += v319txt(hcX0 + hcW / 2, hcY0 + 20, 'گرما (زمان)', MU, 'middle', true, 10) + v319txt(hcX0 - 20, hcY0 - hcH - 4, 'دما', MU, 'middle', true, 10);
      s += v319path('M ' + hcpx(0) + ' ' + hcpy(-10) + ' L ' + hcpx(20) + ' ' + hcpy(0) + ' L ' + hcpx(40) + ' ' + hcpy(0) + ' L ' + hcpx(60) + ' ' + hcpy(100) + ' L ' + hcpx(80) + ' ' + hcpy(100) + ' L ' + hcpx(100) + ' ' + hcpy(110), 'none', ACC, 2.6);
      s += lnr(hcX0 - 12, hcpy(0), hcX0 + hcW, hcpy(0), GR, 1, '4 4') + v319txt(hcX0 - 18, hcpy(0) + 4, '۰°C', PT, 'end', true, 10);
      s += lnr(hcX0 - 12, hcpy(100), hcX0 + hcW, hcpy(100), GR, 1, '4 4') + v319txt(hcX0 - 18, hcpy(100) + 4, '۱۰۰°C', PT, 'end', true, 10);
      s += v319txt(hcpx(10), hcpy(-4) - 10, 'جامد', ST, 'middle', true, 10);
      s += v319txt(hcpx(30), hcpy(0) - 12, 'ذوب (دما ثابت)', PT, 'middle', true, 10);
      s += v319txt(hcpx(50), hcpy(50), 'مایع', ST, 'middle', true, 10);
      s += v319txt(hcpx(70), hcpy(100) - 12, 'جوش (دما ثابت)', PT, 'middle', true, 10);
      s += v319txt(hcpx(92), hcpy(108) + 12, 'بخار', ST, 'middle', true, 10);
      s += v319txt(180, 250, 'منحنی گرمایش آب — در نقطهٔ ذوب و جوش دما تغییر نمی‌کند', MU, 'middle', true, 11);
      s += v319txt(180, 270, 'گرمای نهان: Q = mL  ·  گرمای ویژه: Q = mcΔT', MU, 'middle', false, 10);
      return wrap(s);
    }

    if (t === 'axdna') {
      var dnaMode = xs('mode', 'helix'), dnaSeq = (xs('seq', 'ATGC')).toUpperCase().slice(0, 6), dnai;
      var dnPair = function (b) { return b === 'A' ? 'T' : b === 'T' ? 'A' : b === 'G' ? 'C' : b === 'C' ? 'G' : '?'; };
      if (dnaMode === 'flat') {
        s += lnr(96, 52, 96, 224, ACC, 2.6) + lnr(264, 52, 264, 224, ACC, 2.6);
        for (dnai = 0; dnai < dnaSeq.length; dnai++) {
          var dny = 68 + dnai * 26;
          s += lnr(96, dny, 264, dny, GR, 1.8);
          s += v319txt(86, dny + 5, dnaSeq[dnai], PT, 'end', true, 13) + v319txt(274, dny + 5, dnPair(dnaSeq[dnai]), '#1f8f7a', 'start', true, 13);
          s += v319txt(180, dny + 5, (dnaSeq[dnai] === 'A' || dnaSeq[dnai] === 'T') ? '۲ پیوند هیدروژنی' : '۳ پیوند هیدروژنی', MU, 'middle', false, 9);
        }
        s += v319txt(180, 244, 'DNA — دو رشتهٔ مکمل و جفت‌بازهای A–T و G–C', ST, 'middle', true, 12);
        return wrap(s);
      }
      var dn1 = '', dn2 = '';
      for (dnai = 0; dnai <= 60; dnai++) {
        var dtx = 70 + dnai * 3.6, dph = dnai / 60 * Math.PI * 4;
        dn1 += (dnai ? ' L ' : 'M ') + f2(dtx) + ' ' + f2(132 + 34 * Math.sin(dph));
        dn2 += (dnai ? ' L ' : 'M ') + f2(dtx) + ' ' + f2(132 - 34 * Math.sin(dph));
      }
      s += v319path(dn1, 'none', ACC, 2.6) + v319path(dn2, 'none', PT, 2.6);
      for (dnai = 0; dnai <= 8; dnai++) {
        var dtx2 = 74 + dnai * 27, dph2 = (dtx2 - 70) / 3.6 / 60 * Math.PI * 4;
        s += lnr(dtx2, 132 + 34 * Math.sin(dph2), dtx2, 132 - 34 * Math.sin(dph2), GR, 1.4);
        if (dnai < dnaSeq.length) {
          s += v319txt(dtx2 - 5, 132 + 34 * Math.sin(dph2) + 4, dnaSeq[dnai], ST, 'end', true, 10);
          s += v319txt(dtx2 + 5, 132 - 34 * Math.sin(dph2) + 4, dnPair(dnaSeq[dnai]), ST, 'start', true, 10);
        }
      }
      s += v319txt(180, 214, 'مارپیچ دوگانهٔ DNA — پیوند فسفودی‌استر و جفت‌بازهای مکمل', ST, 'middle', true, 11);
      s += v319txt(180, 236, 'A با T و G با C جفت می‌شوند', MU, 'middle', false, 10);
      s += v319txt(180, 256, 'توالی نمونه: ' + dnaSeq, ACC, 'middle', true, 11);
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
      axspin: '<circle cx="18" cy="15" r="11" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M18,4 V26 M7,15 H29" stroke="currentColor" stroke-width="1"/><path d="M18,15 L25,9" stroke="currentColor" stroke-width="2.2"/>',
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
      // V304 — ابزار ریاضی ابتدایی (عیناً FigurePickerDialog.kt)
      axtally: [['n', 'تعداد (۰ تا ۷۵)'], ['label', 'نمایش تعداد زیر شکل']],
      axbase10: [['n', 'عدد (۰ تا ۱٬۰۰۰٬۰۰۰)'], ['label', 'نمایش عدد زیر شکل']],
      axplace: [['n', 'ردیف ۱ (عدد)'], ['n2', 'ردیف ۲ (خالی = ندارد)'], ['op2', 'علامت ردیف ۲ (+ یا −)'], ['n3', 'ردیف ۳ (خالی = ندارد)'], ['op3', 'علامت ردیف ۳ (+ یا −)'], ['rows', 'تعداد ردیف‌ها (۱ تا ۶)'], ['blank', 'خانه‌ها خالی؟ (۱=بله)']],
      axclock: [['h', 'ساعت (۰ تا ۱۲)'], ['m', 'دقیقه (۰ تا ۵۹)'], ['nums', 'نمایش عددها (۱/۰)'], ['label', 'نوشتن ساعت زیر شکل (۱/۰)']],
      axruler: [['len', 'طول خط‌کش (۲ تا ۵۰ سانتی‌متر)'], ['a', 'ابتدای اندازه‌گیری (اختیاری)'], ['b', 'انتهای اندازه‌گیری (اختیاری)'], ['label', 'نمایش طول بازه']],
      axfrac: [['num', 'صورت'], ['den', 'مخرج'], ['shape', 'شکل'], ['label', 'نوشتن کسر زیر شکل (۱/۰)']],
      axmoney: [['notes', 'اسکناس‌ها (با ویرگول)'], ['coins', 'سکه‌ها (با ویرگول)'], ['unit', 'واحد'], ['sum', 'نوشتن جمع (۱/۰)']],
      axabacus: [['n', 'عدد (۰ تا ۹۹۹٬۹۹۹)'], ['cols', 'تعداد ستون‌ها (۲ تا ۶)'], ['blank', 'مخفی کردن ارقام زیر چرتکه (۱=بله)']],
      axjump: [['start', 'نقطهٔ شروع'], ['step', 'طول هر پرش'], ['jumps', 'تعداد پرش'], ['max', 'انتهای محور'], ['label', 'نمایش عبارت جمع/ضرب (۱/۰)']],
      axbalance: [['left', 'وزنهٔ کفهٔ چپ'], ['right', 'وزنهٔ کفهٔ راست'], ['unit', 'واحد جرم'], ['label', 'نمایش علامت مقایسه']],
      axthermo: [['val', 'دما (°C)'], ['min', 'کمینهٔ دما'], ['max', 'بیشینهٔ دما']],
      axsaw: [['size', 'اندازهٔ جدول (۳×۳ تا ۹×۹)'], ['mode', 'نمایش (رنگ / شکل / عدد)'], ['vals', 'سطرها با ; و خانه‌ها با , (جای خالی = ?)']],
      axpattern: [['seq', 'دنباله با ویرگول (مثلاً ●,▲,●,▲,?)'], ['mode', 'نوع نمایش الگو (شکل / رنگ / مهره)']],
      axtenframe: [['a', 'مهره‌های قرمز'], ['b', 'مهره‌های آبی'], ['frames', 'تعداد قاب (۱ یا ۲)'], ['label', 'نمایش عبارت جمع']],
      axsym: [['cells', 'خانه‌ها (ردیف,ستون با ;)'], ['axis', 'خط تقارن (v عمودی / h افقی)'], ['mirror', 'نمایش قرینه (۱=بله، ۰=؟)']],
      axspin: [['parts', 'بخش‌های چرخنده (با کاما)'], ['arrow', 'شمارهٔ بخش عقربه']],
      axbeaker: [['val', 'حجم مایع'], ['max', 'ظرفیت کل'], ['unit', 'واحد (mL یا لیتر)']],
      axgeoboard: [['pts', 'مختصات میخ‌ها (x,y با ;)'], ['grid', 'اندازهٔ شبکه (۳ تا ۷)']],
      axdice: [['dice', 'اعداد تاس‌ها (با کاما)'], ['sum', 'نمایش جمع (۱=جمع، ۲=؟، ۰=خیر)']],
      axmularea: [['a', 'عدد اول (افقی)'], ['b', 'عدد دوم (عمودی)'], ['blank', 'مخفی کردن حاصل‌ها (۰ یا ۱)']],
      axvenn: [['left', 'دایرهٔ چپ'], ['right', 'دایرهٔ راست'], ['mid', 'بخش مشترک (وسط)']],
      axnumgrid: [['start', 'عدد شروع'], ['cols', 'تعداد ستون'], ['rows', 'تعداد سطر'], ['rule', 'رنگ‌آمیزی مضرب/الگو'], ['blanks', 'خانه‌های جای خالی (با ویرگول)']],
      axfacttree: [['n', 'عدد مورد تجزیه (۴ تا ۹۹۹)'], ['blank', 'حالت جای خالی (؟)']],
      axfnmachine: [['in', 'ورودی‌ها (با ویرگول)'], ['op1', 'عملگر مرحلهٔ اول'], ['op2', 'عملگر مرحلهٔ دوم (اختیاری)'], ['blank', 'حالت جای خالی (؟)']],
      axfracwall: [['dens', 'مخرج ردیف‌ها (با ویرگول)'], ['fills', 'تعداد رنگ‌شدهٔ هر ردیف'], ['mode', 'نحوهٔ نمایش کسرها']],
      axtangram: [['shape', 'طرح تانگرام (۷ تکه)'], ['mode', 'حالت نمایش قطعات']],
      axangle: [['deg', 'زاویه (۰ تا ۱۸۰ درجه)'], ['pro', 'نمایش نقاله'], ['label', 'نمایش اندازهٔ زاویه']],
      axrod: [['nums', 'میله‌ها (۱ تا ۱۰ با ویرگول)'], ['blank', 'جای خالی (؟)']],
      axtower: [['base', 'آجرهای ردیف پایین'], ['op', 'عمل (+ یا −)'], ['blank', 'جای خالی (؟)']],
      axmulwheel: [['c', 'عدد مرکز'], ['n', 'تعداد پره (۳ تا ۱۲)'], ['op', 'عمل (× یا +)'], ['hide', 'جای خالی (؟)']],
      axmultgrid: [['rows', 'عددهای سطر'], ['cols', 'عددهای ستون'], ['blank', 'جای خالی (۰ کامل / ۱ خانه‌ها / ۲ سرستون‌ها)']],
      axnet: [['shape', 'نوع حجم'], ['mode', 'حالت نمایش']],
      axcubes: [['map', 'ارتفاع ستون‌ها (سطرها با ;)'], ['view', 'نما'], ['hide', 'مخفی کردن تعداد (؟)']],
      axcalendar: [['month', 'نام ماه'], ['start', 'روز شروع (۰ = شنبه)'], ['days', 'تعداد روزها'], ['mark', 'روزهای نشان‌دار (با ویرگول)'], ['blank', 'جای خالی (؟)']],
      axlever: [['kind', 'نوع ماشین'], ['load', 'نیروی بار (N)'], ['effort', 'نیروی محرک (N)'], ['l1', 'بازوی بار'], ['l2', 'بازوی نیرو']],
      axcycle: [['kind', 'نوع چرخه']],
      axwordtrain: [['syll', 'هجاها (با ویرگول)'], ['word', 'کلمهٔ ساخته‌شده'], ['mode', 'حالت']],
      axtiles: [['x2', 'تعداد کاشی x²'], ['x', 'تعداد کاشی x'], ['k', 'تعداد کاشی ۱'], ['neg', 'منفی (۱/۰)']],
      axkenken: [['size', 'ابعاد جدول (۳ تا ۶)'], ['cages', 'قفس‌ها (خانه‌ها|هدف|عمل با ;)']],
      axdicefold: [['mode', 'نمایش وجه‌ها'], ['top', 'شمارهٔ وجه بالا'], ['ask', 'پرسش وجه مقابل (۱/۰)']],
      axtransform: [['shape', 'شکل'], ['op', 'نوع تبدیل'], ['dx', 'انتقال افقی'], ['dy', 'انتقال عمودی'], ['ang', 'زاویهٔ دوران'], ['axis', 'محور بازتاب'], ['grid', 'اندازهٔ شبکه']],
      axcaliper: [['kind', 'نوع ابزار'], ['val', 'مقدار اندازه‌گیری (mm)'], ['max', 'بیشینهٔ مقیاس']],
      axsigntable: [['roots', 'ریشه‌ها (با ویرگول)'], ['mult', 'تکرار ریشه‌ها (۱ فرد / ۲ زوج)'], ['lead', 'ضریب پیشرو (۱ یا -۱)']],
      axtrigcircle: [['deg', 'زاویه (درجه)'], ['show', 'نمایش تصویر sin و cos (۱/۰)']],
      axmapping: [['from', 'اعضای دامنه'], ['to', 'اعضای برد'], ['arrows', 'پیکان‌ها (مبدأ-مقصد با ;)']],
      axprobtree: [['first', 'گزینه‌های مرحلهٔ ۱'], ['second', 'گزینه‌های مرحلهٔ ۲'], ['mode', 'نمایش']],
      axconic: [['kind', 'نوع مقطع'], ['a', 'نیم‌محور a'], ['b', 'نیم‌محور b']],
      axorbital: [['z', 'عدد اتمی'], ['mode', 'حالت (جعبهٔ اوربیتال / ساختار لوئیس)'], ['sym', 'نماد اتم (لوئیس)'], ['val', 'الکترون ظرفیت (لوئیس)']],
      axpunnett: [['kind', 'نوع'], ['p1', 'والد ۱'], ['p2', 'والد ۲'], ['mode', 'نمایش']],
      axecg: [['kind', 'نوع نوار'], ['n', 'تعداد ضربان'], ['label', 'برچسب موج‌ها (۱/۰)'], ['A', 'دامنه (نوسان)'], ['T', 'دوره (نوسان)']],
      axprosody: [['line', 'مصراع (ارکان با فاصله)'], ['mode', 'حالت نمایش']],
      axtruthtable: [['kind', 'نوع'], ['expr', 'عبارت (عنوان ستون)']],
      axledger: [['title', 'عنوان حساب'], ['debit', 'اقلام بدهکار (با ویرگول)'], ['credit', 'اقلام بستانکار (با ویرگول)']],
      axstaff: [['kind', 'نوع'], ['clef', 'کلید'], ['notes', 'نت‌ها (شمارهٔ MIDI)']],
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
      axunitconv: [['kind', 'نوع واحد'], ['val', 'مقدار']],
      axperimeter: [['shape', 'شکل'], ['w', 'عرض / شعاع'], ['h', 'ارتفاع'], ['n', 'تعداد ضلع چندضلعی']],
      axpictograph: [['mode', 'نوع نمودار'], ['data', 'داده‌ها (با ویرگول)'], ['labs', 'نام دسته‌ها']],
      axrounding: [['n', 'عدد'], ['mode', 'گرد به']],
      axdivision: [['a', 'عدد کل'], ['b', 'تعداد هر گروه']],
      axshapeid: [['kind', 'شکل'], ['n', 'تعداد ضلع (چندضلعی)']],
      axcircleparts: [['show', 'بخش نمایش‌داده‌شده']],
      axcylinder: [['kind', 'نوع جسم'], ['r', 'شعاع قاعده'], ['h', 'ارتفاع']],
      axproportion: [['a', 'مخرج معلوم'], ['b', 'صورت معلوم'], ['c', 'مقدار متناظر']],
      axlinear: [['m1', 'شیب خط اول'], ['b1', 'عرض از مبدأ اول'], ['m2', 'شیب خط دوم'], ['b2', 'عرض از مبدأ دوم']],
      axbarchart: [['data', 'داده‌ها'], ['labs', 'برچسب‌ها'], ['show', 'نمایش شاخص']],
      axparabola: [['a', 'ضریب a'], ['b', 'ضریب b'], ['c', 'جملهٔ ثابت c']],
      axinthales: [['ang', 'زاویهٔ مرکزی (درجه)']],
      axpendulum: [['len', 'طول نخ (متر)'], ['amp', 'دامنه (درجه)']],
      axincline: [['ang', 'زاویهٔ شیب (درجه)'], ['mass', 'جرم (kg)']],
      axcircuit: [['mode', 'نوع مدار'], ['r1', 'مقاومت ۱ (Ω)'], ['r2', 'مقاومت ۲ (Ω)'], ['v', 'ولتاژ باتری']],
      axoptics: [['kind', 'نوع عدسی'], ['f', 'فاصلهٔ کانونی (cm)'], ['d', 'فاصلهٔ جسم (cm)']],
      axheatcurve: [],
      axdna: [['mode', 'نمایش'], ['seq', 'توالی بازها (A/T/G/C)']],
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
  // V310.7 / V314.0 / V315.1 / V315.2 — همهٔ ابزارهای ریاضی/علوم ابتدایی و محورها و نمودار عقربه‌ای با ماوس و لمس در پیش‌نمایش قابل تنظیم‌اند
  function bindClock(box) {
    var INTERACTIVE_PRIMARY = {
      axclock: 1, axfrac: 1, axabacus: 1, axthermo: 1, axtenframe: 1, axspin: 1, axbeaker: 1,
      axtally: 1, axbase10: 1, axplace: 1, axruler: 1, axmoney: 1, axjump: 1, axbalance: 1,
      axsaw: 1, axpattern: 1, axsym: 1, axgeoboard: 1, axdice: 1, axmularea: 1, axvenn: 1,
      axnumgrid: 1, axfacttree: 1, axfnmachine: 1, axfracwall: 1, axtangram: 1, axangle: 1,
      axrod: 1, axtower: 1, axmulwheel: 1, axmultgrid: 1, axnet: 1, axcubes: 1, axcalendar: 1, axlever: 1, axcycle: 1, axwordtrain: 1, axtiles: 1, axkenken: 1, axdicefold: 1, axtransform: 1, axcaliper: 1, axsigntable: 1, axtrigcircle: 1, axmapping: 1, axprobtree: 1, axconic: 1, axorbital: 1, axpunnett: 1, axecg: 1, axprosody: 1, axtruthtable: 1, axledger: 1, axstaff: 1, axkmap: 1, axgate: 1, axdfa: 1, axbst: 1, axbode: 1, axbeam: 1, axmohr: 1, axpv: 1, axpert: 1, axbenzene: 1, axnormal: 1, axsupply: 1, axgame: 1,
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
    if (state.t === 'axspin') {
      box.onpointerdown = function (e) {
        var p = clockPoint(box, e);
        var sp = String(state.X.parts || 'قرمز,آبی,سبز,زرد,قرمز,آبی').split(/[,،]/).map(function (x) { return x.trim(); }).filter(Boolean);
        var n = Math.max(2, sp.length);
        if (p) {
          var deg = (Math.atan2(p.y - 142, p.x - 180) * 180 / Math.PI + 90 + 360) % 360;
          state.X.arrow = String(Math.min(n, Math.max(1, Math.floor(deg / (360 / n)) + 1)));
        } else {
          var cur = Math.max(1, Math.min(n, Math.floor(num(state.X.arrow, 1))));
          state.X.arrow = String((cur % n) + 1);
        }
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
        var ratio = Math.max(0, Math.min(1, (216 - p.y) / (216 - 36)));
        var step = mx >= 100 ? 10 : 1;
        state.X.val = String(Math.min(mx, Math.max(0, Math.round((ratio * mx) / step) * step)));
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
        var cur = Math.max(0, Math.min(999999999, Math.floor(num(state.X.n, 134))));
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
        var cyc = [];
        for (var k2 = 1; k2 <= n2; k2++) cyc.push(String(k2));
        cyc.push('?');
        grid[r][c] = cyc[(cyc.indexOf(grid[r][c]) + 1) % cyc.length];
        state.X.size = String(n2);
        state.X.vals = grid.map(function (row) { return row.join(','); }).join(';');
        paint(); renderFields();
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
        var idx = Math.max(0, Math.min(n - 1, Math.floor((p.x - x0) / (cw + gap))));
        var mode = String(state.X.mode || 'shape').toLowerCase();
        var cyc = (mode === 'color' || mode === 'bead') ? ['قرمز', 'آبی', 'سبز', 'زرد', 'بنفش', '?'] : ['●', '▲', '■', '★', '?'];
        items[idx] = cyc[(cyc.indexOf(items[idx]) + 1) % cyc.length];
        state.X.seq = items.join(',');
        paint(); renderFields();
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
        var c = Math.max(0, Math.min(7, Math.floor((p.x - 76) / 26)));
        var r = Math.max(0, Math.min(7, Math.floor((p.y - 32) / 26)));
        var axis = String(state.X.axis || 'v').toLowerCase();
        var pair = axis === 'h'
          ? ((c + 1) + ',' + (r < 4 ? 4 - r : r - 3))
          : ((r + 1) + ',' + (c < 4 ? 4 - c : c - 3));
        var list = String(state.X.cells || '1,1;2,1;2,2;3,2').split(/[;؛]/).map(function (z) { return faEn(z.trim()); }).filter(Boolean);
        var ix = list.indexOf(pair);
        if (ix >= 0 && list.length > 1) list.splice(ix, 1);
        else if (ix < 0) list.push(pair);
        state.X.cells = list.join(';');
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
        var den = Math.max(1, Math.min(24, Math.floor(num(state.X.den, 4))));
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
        var cols = Math.max(2, Math.min(6, Math.floor(num(state.X.cols, 4))));
        var cidx = Math.max(0, Math.min(cols - 1, Math.floor((p.x - 36) / (288 / cols))));
        var place = cols - 1 - cidx, pow = Math.pow(10, place);
        var curN = Math.max(0, Math.min(999999, Math.floor(num(state.X.n, 2435))));
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
      axtower: ['op', ['+', '-']],
      axmulwheel: ['c', ['1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12']],
      axmultgrid: ['blank', ['0', '1', '2']],
      axnet: ['shape', ['cube', 'cuboid', 'pyramid', 'prism']],
      axcubes: ['view', ['iso', 'top', 'front', 'side']],
      axcalendar: ['start', ['0', '1', '2', '3', '4', '5', '6']],
      axlever: ['kind', ['lever1', 'lever2', 'lever3', 'pulley', 'gears']],
      axcycle: ['kind', ['water', 'plant', 'frog', 'food']],
      axwordtrain: ['mode', ['join', 'blank']],
      axtiles: ['x', ['0', '1', '2', '3', '4', '5', '6', '7', '8']],
      axkenken: ['size', ['3', '4', '5', '6']],
      axdicefold: ['top', ['1', '2', '3', '4', '5', '6']],
      axtransform: ['op', ['translate', 'rotate', 'reflect']],
      axsigntable: ['lead', ['1', '-1']],
      axprobtree: ['mode', ['prob', 'count']],
      axconic: ['kind', ['ellipse', 'hyperbola', 'parabola']],
      axpunnett: ['mode', ['genotype', 'ratio']],
      axecg: ['kind', ['ecg', 'shm']],
      axprosody: ['mode', ['arkan', 'taqti']],
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
      axunitconv: ['kind', ['len', 'mass', 'area']],
      axperimeter: ['shape', ['rect', 'tri', 'circle', 'poly']],
      axpictograph: ['mode', ['picto', 'bar']],
      axrounding: ['mode', ['ten', 'hundred']],
      axdivision: ['b', ['2', '3', '4', '5', '6', '7', '8', '9', '10']],
      axshapeid: ['kind', ['tri', 'square', 'rect', 'circle', 'poly']],
      axcircleparts: ['show', ['all', 'radius', 'diameter', 'chord', 'tangent', 'arc']],
      axcylinder: ['kind', ['cylinder', 'cone']],
      axproportion: ['a', ['1', '2', '3', '4', '5', '6', '8', '10']],
      axbarchart: ['show', ['mean', 'median', 'none']],
      axinthales: ['ang', ['30', '45', '60', '90', '120', '150']],
      axpendulum: ['amp', ['10', '20', '30', '40']],
      axincline: ['ang', ['15', '30', '45', '60']],
      axcircuit: ['mode', ['series', 'parallel']],
      axoptics: ['kind', ['convex', 'concave']],
      axdna: ['mode', ['helix', 'flat']],
      axcell: ['kind', ['animal', 'plant']],
      axmapgeo: ['kind', ['compass', 'map']],
      axsentence: ['mode', ['verbal', 'nominal']],
      axvectors: ['mode', ['sum', 'diff']],
      axintegral: ['f', ['quad', 'sin', 'line']],
      axstack: ['mode', ['stack', 'queue']],
      axsort: ['step', ['0', '1', '2', '3']]
    };
    var NUMSTEP = {
      axcaliper: ['val', 1, 0, 50],
      axorbital: ['z', 1, 1, 36],
      axmohr: ['txy', 5, 0, 120],
      axunitconv: ['val', 1, 1, 20],
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
        var deg = Math.atan2(140 - p.y, p.x - 176) * 180 / Math.PI;
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
    'axunitconv:X.kind': [['len', 'طول: کیلومتر → متر → میلی‌متر'], ['mass', 'جرم: تن → کیلوگرم → گرم'], ['area', 'مساحت: m² → dm² → cm²']],
    'axperimeter:X.shape': [['rect', 'مستطیل'], ['tri', 'مثلث قائم‌الزاویه'], ['circle', 'دایره'], ['poly', 'چندضلعی منتظم']],
    'axpictograph:X.mode': [['picto', 'تصویری (شکل‌ها)'], ['bar', 'میله‌ای']],
    'axrounding:X.mode': [['ten', 'دهگان'], ['hundred', 'صدگان']],
    'axshapeid:X.kind': [['tri', 'مثلث'], ['square', 'مربع'], ['rect', 'مستطیل'], ['circle', 'دایره'], ['poly', 'چندضلعی منتظم']],
    'axcircleparts:X.show': [['all', 'همهٔ اجزا'], ['radius', 'شعاع'], ['diameter', 'قطر'], ['chord', 'وتر'], ['tangent', 'مماس'], ['arc', 'کمان']],
    'axcylinder:X.kind': [['cylinder', 'استوانه'], ['cone', 'مخروط']],
    'axbarchart:X.show': [['mean', 'میانگین'], ['median', 'میانه'], ['none', 'بدون شاخص']],
    'axcircuit:X.mode': [['series', 'سری'], ['parallel', 'موازی']],
    'axoptics:X.kind': [['convex', 'محدب (همگرا)'], ['concave', 'مقعر (واگرا)']],
    'axdna:X.mode': [['helix', 'مارپیچ دوگانه'], ['flat', 'نردبان صاف']],
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
      ['shape', 'شکلی و عددی (● ▲ ■ ★)'],
      ['color', 'الگوی رنگ‌آمیزی — ویژهٔ اول ابتدایی'],
      ['bead', 'نخ و مهره‌های رنگی']
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
    'axtiles:X.neg': [['0', 'مثبت (آبی)'], ['1', 'منفی (قرمز)']],
    'axkenken:X.size': [['3', '۳×۳'], ['4', '۴×۴'], ['5', '۵×۵'], ['6', '۶×۶']],
    'axdicefold:X.mode': [['dots', 'نقطه‌ها'], ['num', 'عددها']],
    'axdicefold:X.top': [['1', '1'], ['2', '2'], ['3', '3'], ['4', '4'], ['5', '5'], ['6', '6']],
    'axdicefold:X.ask': [['0', 'نمایش همهٔ وجه‌ها'], ['1', 'چند وجه جای خالی (؟)']],
    'axtransform:X.shape': [['tri', 'مثلث'], ['sq', 'مربع'], ['rect', 'مستطیل']],
    'axtransform:X.op': [['translate', 'انتقال (بردار)'], ['rotate', 'دوران'], ['reflect', 'بازتاب']],
    'axtransform:X.axis': [['x', 'نسبت به محور x'], ['y', 'نسبت به محور y']],
    'axcaliper:X.kind': [['vernier', 'کولیس ورنیه (دقت ۰٫۱)'], ['micrometer', 'ریزسنج (دقت ۰٫۰۱)']],
    'axsigntable:X.lead': [['1', 'مثبت (۱)'], ['-1', 'منفی (-۱)']],
    'axtrigcircle:X.show': [['1', 'با تصویر sin و cos'], ['0', 'فقط ضلع زاویه']],
    'axprobtree:X.mode': [['prob', 'نمایش درصد احتمال'], ['count', 'نمایش نتیجهٔ هر شاخه']],
    'axconic:X.kind': [['ellipse', 'بیضی'], ['hyperbola', 'هذلولی'], ['parabola', 'سهمی']],
    'axorbital:X.mode': [['box', 'جعبه‌های اوربیتال'], ['lewis', 'ساختار لوئیس']],
    'axpunnett:X.kind': [['punnett', 'مربع پانت'], ['pedigree', 'شجره‌نامه']],
    'axpunnett:X.mode': [['genotype', 'ژنوتیپ'], ['ratio', 'نسبت فنوتیپی']],
    'axecg:X.kind': [['ecg', 'نوار قلب (ECG)'], ['shm', 'نوسان هماهنگ ساده']],
    'axecg:X.label': [['1', 'با برچسب P و QRS و T'], ['0', 'بدون برچسب']],
    'axecg:X.n': [['1', '۱ ضربان'], ['2', '۲ ضربان'], ['3', '۳ ضربان']],
    'axprosody:X.mode': [['arkan', 'نام ارکان + هجاها'], ['taqti', 'فقط تقطیع (∪ و −)']],
    'axtruthtable:X.kind': [['truth', 'جدول ارزش گزاره‌ها'], ['square', 'مربع تقابل ارسطویی']],
    'axstaff:X.kind': [['staff', 'خطوط حامل'], ['color', 'چرخهٔ ۱۲ رنگ ایتن']],
    'axstaff:X.clef': [['treble', 'کلید سل'], ['bass', 'کلید فا']],
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
    'axsym:X.mirror': [['1', 'نمایش کامل شکل قرینه'], ['0', 'جای خالی (؟) برای سؤال']],
    'axplace:X.op2': [['', 'ندارد'], ['+', 'جمع (+)'], ['-', 'تفریق (−)']],
    'axplace:X.op3': [['', 'ندارد'], ['+', 'جمع (+)'], ['-', 'تفریق (−)']],
    'axplace:X.rows': [['1', '۱ ردیف'], ['2', '۲ ردیف'], ['3', '۳ ردیف'], ['4', '۴ ردیف']],
    'axplace:X.blank': [['0', 'نمایش عددها'], ['1', 'جای خالی (برای طرح سؤال)']],
    'axabacus:X.blank': [['0', 'نمایش عددها'], ['1', 'جای خالی (برای طرح سؤال)']],
    'axmularea:X.blank': [['0', 'نمایش عددها'], ['1', 'جای خالی (برای طرح سؤال)']],
    'axabacus:X.cols': [['2', '۲ ستون (تا دهگان)'], ['3', '۳ ستون (تا صدگان)'], ['4', '۴ ستون (تا هزار)'], ['5', '۵ ستون (تا ده‌هزار)'], ['6', '۶ ستون (تا صدهزار)']],
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
    host.innerHTML = '<h4>داده‌ها و بازه</h4><div class="gf-grid">' + fieldsFor(state.t).map(function (r) {
      var v = getPath(state, r[0]);
      var ddOpts = GF_DROPDOWNS[state.t + ':' + r[0]];
      if (r[0] === 'X.shape' || ddOpts) {
        // V310.7 / V310.9 / V315.1 — گزینه‌های چندحالته به‌صورت منوی کشویی (در div.gf-field.gf-span تا label کلیک دکمه را به select بومی نفرستد و عرض کامل بگیرد)
        var opts = ddOpts || [['circle', 'دایره‌ای (قطاع)'], ['ring', 'حلقه‌ای'], ['bar', 'نواری'], ['square', 'مربعی (شبکه)']];
        var cur = (v != null && v !== '') ? String(v) : opts[0][0];
        var selTag = r[0] === 'X.shape' ? '<select data-k="X.shape" class="gf-sel">' : '<select data-k="' + r[0] + '" class="gf-sel">';
        return '<div class="gf-field gf-span"><span class="gf-lbl">' + esc(r[1]) + '</span>' + selTag + opts.map(function (o) {
          return '<option value="' + esc(o[0]) + '"' + (cur === o[0] ? ' selected' : '') + '>' + esc(o[1]) + '</option>';
        }).join('') + '</select></div>';
      }
      var wide = /labs|vals|title|xs|ys|zs|rows|cols|mins|q1s|meds|q3s|maxs|opens|highs|lows|closes|cells|pts|seq|parts|dice|left|right|mid/.test(r[0]);
      return '<label class="' + (wide ? 'gf-span' : '') + '">' + esc(r[1]) +
        '<input data-k="' + r[0] + '" value="' + esc(v == null ? '' : v) + '"></label>';
    }).join('') + '</div>';
    host.querySelectorAll('input,select').forEach(function (inp) {
      inp.addEventListener(inp.tagName === 'SELECT' ? 'change' : 'input', function () {
        setPath(state, inp.getAttribute('data-k'), inp.value);
        paint();
      });
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
    { id: 'elementary', name: '🧮 دبستان', hint: 'پایهٔ اول تا ششم — شمارش، جمع و تفریق، ضرب، شکل و اندازه', ids: ['axtally', 'axbase10', 'axplace', 'axclock', 'axruler', 'axfrac', 'axmoney', 'axabacus', 'axjump', 'axbalance', 'axthermo', 'axsaw', 'axpattern', 'axtenframe', 'axsym', 'axspin', 'axbeaker', 'axgeoboard', 'axdice', 'axmularea', 'axvenn', 'axnumgrid', 'axfacttree', 'axfnmachine', 'axfracwall', 'axtangram', 'axangle', 'axrod', 'axtower', 'axmulwheel', 'axmultgrid', 'axnet', 'axcubes', 'axcalendar', 'axlever', 'axcycle', 'axwordtrain'] },
    { id: 'middle', name: '📐 متوسطهٔ اول', hint: 'پایهٔ هفتم تا نهم — جبر، هندسه، اندازه‌گیری و آمار مقدماتی', ids: ['axtiles', 'axkenken', 'axdicefold', 'axtransform', 'axcaliper', 'axunitconv', 'axperimeter', 'axpictograph', 'axrounding', 'axdivision', 'axshapeid', 'axcircleparts', 'axcylinder', 'axproportion', 'axlinear'] },
    { id: 'high', name: '🧪 متوسطهٔ دوم و کنکور', hint: 'ریاضی، فیزیک، شیمی و زیست — از تابع و مثلثات تا مدار و سلول', ids: ['axsigntable', 'axtrigcircle', 'axmapping', 'axprobtree', 'axconic', 'axorbital', 'axpunnett', 'axecg', 'axprosody', 'axtruthtable', 'axledger', 'axstaff', 'axbarchart', 'axparabola', 'axinthales', 'axpendulum', 'axincline', 'axcircuit', 'axoptics', 'axheatcurve', 'axdna', 'axcell'] },
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
    var t0 = math ? PRIMARY_MATH_FIRST_ID : 'axnum';
    try { state = spec ? JSON.parse(JSON.stringify(spec)) : (axis ? defAxis(t0) : def('col')); }
    catch (e) { state = axis ? defAxis(t0) : def('col'); }
    state.k = 'g';
    if (!state.t) state.t = axis ? t0 : 'col';
    if (!state.X) state.X = (isAxisType(state.t) ? defAxis(state.t) : def(state.t)).X;
    ensureModal();
    shapesMode = null;
    /* V320 — بازکردن تازه: از مرحلهٔ انتخاب دسته؛ ویرایش شکل موجودِ ریاضی: همان دسته از پیش انتخاب */
    var presel = (spec && spec.t) ? mathCatOfType(spec.t) : null;
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
