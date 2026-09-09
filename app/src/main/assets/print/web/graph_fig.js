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
  function num(v, fb) {
    var n = parseFloat(String(v == null ? '' : v).replace('،', '.'));
    return isFinite(n) ? n : fb;
  }
  function wrap(inner) {
    return '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 360 280" width="320" height="250" overflow="hidden">' +
      '<style>text{font:700 12px Vazirmatn,Tahoma,sans-serif;fill:#243044}.tk{font:600 11px Vazirmatn,Tahoma,sans-serif;fill:#4a5870}.ttl{font:800 13px Vazirmatn,Tahoma,sans-serif;fill:#1a2433}.fa{font:700 11px Vazirmatn,Tahoma,sans-serif;fill:#1a2433}.lg{font:700 11px Vazirmatn,Tahoma,sans-serif;fill:#1a2433}.onf{font:800 11px Vazirmatn,Tahoma,sans-serif;fill:#fff;paint-order:stroke;stroke:#1a2433;stroke-width:2.6;stroke-linejoin:round}</style>' +
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

  function svgOf(spec) {
    spec = spec || {};
    var t = spec.t || 'line';
    var X = spec.X || {};
    var title = X.title || '';
    var head = title ? '<text class="ttl" x="180" y="14" text-anchor="middle">' + esc(title) + '</text>' : '';

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
      word: '<text x="4" y="14" font-size="9" fill="currentColor">Aa</text><text x="16" y="22" font-size="7" fill="currentColor">کلمه</text>'
    };
    return '<svg viewBox="0 0 36 30" aria-hidden="true">' + (m[id] || m.plot) + '</svg>';
  }

  function fieldsFor(t) {
    var common = [['X.title', 'عنوان نمودار']];
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

  var state = def('col');
  var replaceEl = null;
  function $(id) { return document.getElementById(id); }

  function paint() {
    var box = $('grPreview');
    if (box) box.innerHTML = svgOf(state);
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
  function renderFields() {
    var host = $('grFields');
    if (!host) return;
    host.innerHTML = '<h4>داده‌ها و بازه</h4><div class="gf-grid">' + fieldsFor(state.t).map(function (r) {
      var v = getPath(state, r[0]);
      var wide = /labs|vals|title|xs|ys|zs|rows|cols|mins|q1s|meds|q3s|maxs|opens|highs|lows|closes/.test(r[0]);
      return '<label class="' + (wide ? 'gf-span' : '') + '">' + esc(r[1]) +
        '<input data-k="' + r[0] + '" value="' + esc(v == null ? '' : v) + '"></label>';
    }).join('') + '</div>';
    host.querySelectorAll('input').forEach(function (inp) {
      inp.addEventListener('input', function () {
        setPath(state, inp.getAttribute('data-k'), inp.value);
        paint();
      });
    });
  }
  function setType(id) {
    var prev = state;
    state = def(id);
    Object.keys(prev.X || {}).forEach(function (k) {
      if (prev.X[k] != null && prev.X[k] !== '') state.X[k] = prev.X[k];
    });
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
    TYPES.forEach(function (s) {
      var b = document.createElement('button');
      b.type = 'button'; b.className = 'gf-shape'; b.setAttribute('data-t', s.id);
      b.innerHTML = iconOf(s.id) + '<span>' + s.name + '</span>';
      b.addEventListener('click', function () { setType(s.id); });
      $('grShapes').appendChild(b);
    });
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
  function open(spec, el) {
    replaceEl = el || null;
    try { state = spec ? JSON.parse(JSON.stringify(spec)) : def('col'); }
    catch (e) { state = def('col'); }
    state.k = 'g';
    if (!state.t) state.t = 'col';
    if (!state.X) state.X = def(state.t).X;
    ensureModal();
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
