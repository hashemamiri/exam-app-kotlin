(function () {
  const SHAPES = [
    { id: 'tri', name: 'مثلث' },
    { id: 'rtri', name: 'قائم‌الزاویه' },
    { id: 'iso', name: 'متساوی‌الساقین' },
    { id: 'eq', name: 'متساوی‌الاضلاع' },
    { id: 'scal', name: 'مختلف‌الاضلاع' },
    { id: 'sq', name: 'مربع' },
    { id: 'rect', name: 'مستطیل' },
    { id: 'para', name: 'متوازی‌الاضلاع' },
    { id: 'rhomb', name: 'لوزی' },
    { id: 'trap', name: 'ذوزنقه' },
    { id: 'itrap', name: 'ذوزنقه متساوی‌الساقین' },
    { id: 'rtrap', name: 'ذوزنقه قائم' },
    { id: 'circ', name: 'دایره' },
    { id: 'ang', name: 'زاویه' },
    { id: 'parll', name: 'خطوط موازی' },
    { id: 'cube', name: 'مکعب' },
    { id: 'box', name: 'مکعب‌مستطیل' },
    { id: 'cyl', name: 'استوانه' },
    { id: 'cone', name: 'مخروط' },
    { id: 'sph', name: 'کره' },
    { id: 'pyr', name: 'هرم' },
    { id: 'pris', name: 'منشور' },
    { id: 'hex', name: 'شش‌ضلعی' },
    { id: 'kite', name: 'بادبادک' },
    { id: 'semi', name: 'نیم‌دایره' },
    { id: 'sec', name: 'قطاع' },
    { id: 'ell', name: 'بیضی' },
    { id: 'hemi', name: 'نیم‌کره' },
    { id: 'frust', name: 'مخروط ناقص' },
    { id: 'pent', name: 'پنج‌ضلعی' },
    { id: 'hept', name: 'هفت‌ضلعی' },
    { id: 'oct', name: 'هشت‌ضلعی' },
    { id: 'deca', name: 'ده‌ضلعی' },
    { id: 'ngon', name: 'nضلعی منتظم' },
    { id: 'ring', name: 'حلقه' },
    { id: 'segm', name: 'قطعه دایره' },
    { id: 'arc', name: 'کمان' },
    { id: 'pseg', name: 'پاره‌خط' },
    { id: 'ray', name: 'نیم‌خط' },
    { id: 'ln', name: 'خط' },
    { id: 'tet', name: 'چهاروجهی' },
    { id: 'hpris', name: 'منشور شش‌پهلو' },
    { id: 'netc', name: 'گسترده مکعب' },
    { id: 'thales', name: 'تالس' },
    { id: 'chord', name: 'وتر و محاطی' },
    { id: 'tang', name: 'مماس' },
    { id: 'acut', name: 'مثلث حاده' },
    { id: 'obt', name: 'مثلث منفرجه' },
    { id: 'star', name: 'ستاره پنج‌پر' },
    { id: 'torus', name: 'چنبره' },
    { id: 'elps', name: 'بیضی‌گون' },
    { id: 'octa', name: 'هشت‌وجهی' },
    { id: 'dodec', name: 'دوازده‌وجهی' },
    { id: 'icosa', name: 'بیست‌وجهی' }
  ];

  function esc(s) {
    return String(s == null ? '' : s)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;')
      .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }
  function clean(s) {
    return String(s == null ? '' : s).replace(/%/g, '').trim();
  }
  function num(v, fallback) {
    var n = parseFloat(String(v == null ? '' : v).replace('°', '').replace('،', '.'));
    return isFinite(n) ? n : fallback;
  }
  function clamp(n, a, b) { return Math.max(a, Math.min(b, n)); }
  function mid(a, b) { return [(a[0] + b[0]) / 2, (a[1] + b[1]) / 2]; }
  function off(a, b, k) {
    var dx = b[0] - a[0], dy = b[1] - a[1];
    var L = Math.hypot(dx, dy) || 1;
    return [-dy / L * k, dx / L * k];
  }
  function txt(x, y, s, extra) {
    if (!s) return '';
    return '<text x="' + x + '" y="' + y + '" ' + (extra || '') + '>' + esc(s) + '</text>';
  }
  function poly(pts) {
    return pts.map(function (p) { return p[0] + ',' + p[1]; }).join(' ');
  }
  function wrap(inner, vb) {
    vb = vb || '0 0 360 280';
    return '<svg xmlns="http://www.w3.org/2000/svg" viewBox="' + vb + '" width="320" height="250" overflow="hidden">' +
      '<style>text{font:700 14px Vazirmatn,Tahoma,sans-serif;fill:#243044} .s{font:700 14px Times New Roman,serif;fill:#15607a} .a{font:700 13px Times New Roman,serif;fill:#5b52e0} .v{fill:#243044}</style>' +
      inner + '</svg>';
  }
  function stroke(pts) {
    return '<polygon points="' + poly(pts) + '" fill="rgba(108,99,245,.06)" stroke="#2c3a50" stroke-width="2.2" stroke-linejoin="round"/>';
  }
  function dots(pts) {
    return pts.map(function (p) {
      return '<circle cx="' + p[0] + '" cy="' + p[1] + '" r="3.2" fill="#2c3a50"/>';
    }).join('');
  }
  function centroid(pts) {
    var x = 0, y = 0;
    pts.forEach(function (p) { x += p[0]; y += p[1]; });
    return [x / pts.length, y / pts.length];
  }
  function unit(from, to) {
    var dx = to[0] - from[0], dy = to[1] - from[1];
    var L = Math.hypot(dx, dy) || 1;
    return [dx / L, dy / L];
  }
  function inbox(x, y, pad) {
    pad = pad == null ? 16 : pad;
    return [Math.max(pad, Math.min(344, x)), Math.max(pad, Math.min(266, y))];
  }
  function sideLabOut(p, q, s, C, dist) {
    if (!s) return '';
    dist = dist == null ? 24 : dist;
    var m = mid(p, q);
    var nrm = off(p, q, 1);
    if (nrm[0] * (C[0] - m[0]) + nrm[1] * (C[1] - m[1]) > 0) nrm = [-nrm[0], -nrm[1]];
    var pt = inbox(m[0] + nrm[0] * dist, m[1] + nrm[1] * dist + 4, 14);
    return txt(pt[0], pt[1], s, 'class="s" text-anchor="middle"');
  }
  function vertLabOut(p, s, C, dist) {
    if (!s) return '';
    dist = dist == null ? 22 : dist;
    var v = [p[0] - C[0], p[1] - C[1]];
    var L = Math.hypot(v[0], v[1]) || 1;
    var pt = inbox(p[0] + v[0] / L * dist, p[1] + v[1] / L * dist + 4, 14);
    return txt(pt[0], pt[1], s, 'class="v" text-anchor="middle"');
  }
  function degLabel(s) {
    if (s == null || s === '') return '';
    return /°/.test(String(s)) ? String(s) : (s + '°');
  }
  function isRight(val) { return num(val, NaN) === 90; }
  function rightMark(c, u, w, size) {
    size = size || 16;
    var x = [c[0] + u[0] * size, c[1] + u[1] * size];
    var y = [c[0] + w[0] * size, c[1] + w[1] * size];
    var xy = [x[0] + w[0] * size, x[1] + w[1] * size];
    return '<polyline points="' + poly([x, xy, y]) + '" fill="none" stroke="#2c3a50" stroke-width="1.6"/>';
  }

  /* 0° = +x ، ۹۰° = بالا. y در SVG رو به پایین است. */
  function polar(O, deg, r) {
    var rad = deg * Math.PI / 180;
    return [O[0] + r * Math.cos(rad), O[1] - r * Math.sin(rad)];
  }
  function heading(from, to) {
    return Math.atan2(from[1] - to[1], to[0] - from[0]) * 180 / Math.PI;
  }
  function normDeg(d) { return ((d % 360) + 360) % 360; }
  function rayEnd(O, deg, box) {
    var rad = deg * Math.PI / 180;
    var dx = Math.cos(rad), dy = -Math.sin(rad);
    var t = 1e6;
    if (dx > 1e-6) t = Math.min(t, (box.x1 - O[0]) / dx);
    if (dx < -1e-6) t = Math.min(t, (box.x0 - O[0]) / dx);
    if (dy > 1e-6) t = Math.min(t, (box.y1 - O[1]) / dy);
    if (dy < -1e-6) t = Math.min(t, (box.y0 - O[1]) / dy);
    t = Math.max(36, t - 10);
    return [O[0] + dx * t, O[1] + dy * t];
  }
  function rightOnly(V, P, Q, label) {
    var u = unit(V, P), w = unit(V, Q);
    var mark = rightMark(V, u, w, 16);
    var bx = V[0] + (u[0] + w[0]) * 22;
    var by = V[1] + (u[1] + w[1]) * 22 + 4;
    return mark + (label ? txt(bx, by, degLabel(label), 'class="a" text-anchor="middle"') : '');
  }
  function arcByDeg(O, startDeg, spanDeg, r, label) {
    spanDeg = clamp(num(spanDeg, 0), 0, 360);
    if (Math.abs(spanDeg - 90) < 0.51) {
      var P = polar(O, startDeg, 80);
      var Q = polar(O, startDeg + 90, 80);
      return rightOnly(O, P, Q, label == null ? '90' : label);
    }
    if (spanDeg <= 0.01) {
      var p0 = polar(O, startDeg, r);
      return '<circle cx="' + p0[0].toFixed(1) + '" cy="' + p0[1].toFixed(1) + '" r="2" fill="#6c63f5"/>' +
        (label ? txt(p0[0], p0[1] - 10, degLabel(label), 'class="a" text-anchor="middle"') : '');
    }
    var s = polar(O, startDeg, r);
    var e = polar(O, startDeg + spanDeg, r);
    var large = spanDeg > 180 ? 1 : 0;
    var midH = startDeg + spanDeg / 2;
    var lp = polar(O, midH, r + 15);
    var d = 'M' + s[0].toFixed(1) + ',' + s[1].toFixed(1) +
      ' A' + r + ',' + r + ' 0 ' + large + ' 0 ' + e[0].toFixed(1) + ',' + e[1].toFixed(1);
    return '<path d="' + d + '" fill="none" stroke="#6c63f5" stroke-width="1.8"/>' +
      (label ? txt(lp[0], lp[1] + 4, degLabel(label), 'class="a" text-anchor="middle"') : '');
  }
  function angleArc(V, P, Q, r, label, forceRight) {
    if (forceRight || isRight(label)) return rightOnly(V, P, Q, label || '90');
    if (!label) return '';
    var hP = heading(V, P), hQ = heading(V, Q);
    var span = normDeg(hQ - hP);
    if (span > 180) { var tmp = hP; hP = hQ; hQ = tmp; span = normDeg(hQ - hP); }
    return arcByDeg(V, hP, span, r, label);
  }

  function svgOf(spec) {
    spec = spec || {};
    var t = spec.t || 'tri';
    var V = spec.V || {};
    var S = spec.S || {};
    var Ang = spec.A || {};
    var X = spec.X || {};

    if (t === 'tri' || t === 'iso' || t === 'eq' || t === 'scal' || t === 'acut' || t === 'obt') {
      var P;
      if (t === 'eq') P = [[180, 48], [64, 208], [296, 208]];
      else if (t === 'iso') P = [[180, 42], [78, 208], [282, 208]];
      else if (t === 'scal') P = [[214, 36], [46, 222], [322, 164]];
      else if (t === 'acut') P = [[180, 36], [86, 214], [270, 204]];
      else if (t === 'obt') P = [[52, 72], [88, 222], [318, 188]];
      else P = [[188, 44], [58, 212], [308, 196]];
      var C = centroid(P);
      return wrap(
        stroke(P) + dots(P) +
        vertLabOut(P[0], V.A || 'A', C, 22) +
        vertLabOut(P[1], V.B || 'B', C, 22) +
        vertLabOut(P[2], V.C || 'C', C, 22) +
        sideLabOut(P[1], P[2], S.a, C, 20) +
        sideLabOut(P[0], P[2], S.b, C, 20) +
        sideLabOut(P[0], P[1], S.c, C, 20) +
        angleArc(P[0], P[1], P[2], 24, Ang.A, isRight(Ang.A)) +
        angleArc(P[1], P[0], P[2], 24, Ang.B, isRight(Ang.B)) +
        angleArc(P[2], P[0], P[1], 24, Ang.C, isRight(Ang.C))
      );
    }
    if (t === 'rtri') {
      var RtC = [70, 214], RtB = [300, 214], RtA = [70, 50];
      var Rt = [RtA, RtB, RtC], Rc = centroid(Rt);
      return wrap(
        stroke(Rt) + dots(Rt) +
        rightOnly(RtC, RtB, RtA, '90') +
        vertLabOut(RtA, V.A || 'A', Rc, 20) +
        vertLabOut(RtB, V.B || 'B', Rc, 20) +
        vertLabOut(RtC, V.C || 'C', Rc, 20) +
        sideLabOut(RtB, RtC, S.a, Rc, 20) +
        sideLabOut(RtA, RtC, S.b, Rc, 22) +
        sideLabOut(RtA, RtB, S.c, Rc, 20) +
        (isRight(Ang.A) ? rightOnly(RtA, RtC, RtB, '90') : angleArc(RtA, RtC, RtB, 24, Ang.A)) +
        (isRight(Ang.B) ? rightOnly(RtB, RtC, RtA, '90') : angleArc(RtB, RtC, RtA, 24, Ang.B))
      );
    }
    if (t === 'sq' || t === 'rect') {
      var ww = t === 'sq' ? 148 : 196, hh = t === 'sq' ? 148 : 116;
      var QA = [180 - ww / 2, 140 - hh / 2], QB = [180 + ww / 2, 140 - hh / 2];
      var QC = [180 + ww / 2, 140 + hh / 2], QD = [180 - ww / 2, 140 + hh / 2];
      var QP = [QA, QB, QC, QD], Qc = centroid(QP);
      var top = S.a || (t === 'sq' ? S.s : '');
      var side = S.b || (t === 'sq' ? S.s : '');
      return wrap(
        stroke(QP) + dots(QP) +
        vertLabOut(QA, V.A || 'A', Qc, 20) +
        vertLabOut(QB, V.B || 'B', Qc, 20) +
        vertLabOut(QC, V.C || 'C', Qc, 20) +
        vertLabOut(QD, V.D || 'D', Qc, 20) +
        sideLabOut(QA, QB, top, Qc, 20) +
        sideLabOut(QB, QC, side, Qc, 22) +
        sideLabOut(QC, QD, t === 'sq' ? top : S.a, Qc, 20) +
        sideLabOut(QD, QA, t === 'sq' ? side : S.b, Qc, 22) +
        rightOnly(QA, QD, QB, '90') +
        rightOnly(QB, QA, QC, '90') +
        rightOnly(QC, QB, QD, '90') +
        rightOnly(QD, QC, QA, '90')
      );
    }
    if (t === 'para' || t === 'rhomb') {
      var PA = [78, 72], PB = [248, 72], PC = [292, 208], PD = [122, 208];
      if (t === 'rhomb') { PA = [118, 68]; PB = [268, 68]; PC = [232, 212]; PD = [82, 212]; }
      var PP = [PA, PB, PC, PD], Pc = centroid(PP);
      return wrap(
        stroke(PP) + dots(PP) +
        vertLabOut(PA, V.A || 'A', Pc, 20) +
        vertLabOut(PB, V.B || 'B', Pc, 20) +
        vertLabOut(PC, V.C || 'C', Pc, 20) +
        vertLabOut(PD, V.D || 'D', Pc, 20) +
        sideLabOut(PA, PB, S.a, Pc, 20) +
        sideLabOut(PB, PC, S.b, Pc, 20) +
        sideLabOut(PC, PD, S.a, Pc, 20) +
        sideLabOut(PD, PA, S.b, Pc, 20) +
        angleArc(PA, PD, PB, 22, Ang.A, isRight(Ang.A))
      );
    }
    if (t === 'trap' || t === 'itrap' || t === 'rtrap') {
      var TA, TB, TC, TD;
      if (t === 'itrap') { TA = [112, 78]; TB = [248, 78]; TC = [292, 210]; TD = [68, 210]; }
      else if (t === 'rtrap') { TA = [88, 78]; TB = [236, 78]; TC = [236, 210]; TD = [64, 210]; }
      else { TA = [112, 78]; TB = [244, 78]; TC = [292, 210]; TD = [64, 210]; }
      var TP = [TA, TB, TC, TD], Tc = centroid(TP);
      var extra = '';
      if (t === 'rtrap') extra = rightOnly(TB, TA, TC, '90') + rightOnly(TC, TB, TD, '90');
      return wrap(
        stroke(TP) + dots(TP) + extra +
        vertLabOut(TA, V.A || 'A', Tc, 22) +
        vertLabOut(TB, V.B || 'B', Tc, 22) +
        vertLabOut(TC, V.C || 'C', Tc, 22) +
        vertLabOut(TD, V.D || 'D', Tc, 22) +
        sideLabOut(TA, TB, S.a, Tc, 24) +
        sideLabOut(TD, TC, S.b, Tc, 24) +
        sideLabOut(TA, TD, S.c, Tc, 24) +
        sideLabOut(TB, TC, S.d, Tc, 24) +
        (X.h ? txt(318, 148, 'h=' + X.h, 'class="s"') : '')
      );
    }
    if (t === 'circ') {
      return wrap(
        '<circle cx="180" cy="140" r="78" fill="rgba(39,196,168,.06)" stroke="#2c3a50" stroke-width="2.2"/>' +
        '<line x1="180" y1="140" x2="258" y2="140" stroke="#1f8a78" stroke-width="1.8"/>' +
        '<circle cx="180" cy="140" r="3.2" fill="#2c3a50"/>' +
        txt(166, 132, V.O || 'O', 'class="v" text-anchor="middle"') +
        txt(248, 118, X.r ? ('r=' + X.r) : 'r', 'class="s"')
      );
    }
    if (t === 'ang') {
      var measure = clamp(num(X.m != null && X.m !== '' ? X.m : Ang.O, 50), 0, 360);
      var O = [180, 158];
      var startH = 0;
      var rayLen = 96;
      var Bpt = polar(O, startH, rayLen);
      var Apt = polar(O, startH + measure, rayLen);
      var Alab = inbox(polar(O, startH + measure, rayLen + 20)[0], polar(O, startH + measure, rayLen + 20)[1], 16);
      var Blab = inbox(polar(O, startH, rayLen + 20)[0], polar(O, startH, rayLen + 20)[1] + 6, 16);
      var Olab = inbox(O[0] - 16, O[1] + 20, 16);
      var html =
        '<line x1="' + O[0] + '" y1="' + O[1] + '" x2="' + Bpt[0].toFixed(1) + '" y2="' + Bpt[1].toFixed(1) + '" stroke="#2c3a50" stroke-width="2.3" stroke-linecap="round"/>' +
        '<line x1="' + O[0] + '" y1="' + O[1] + '" x2="' + Apt[0].toFixed(1) + '" y2="' + Apt[1].toFixed(1) + '" stroke="#2c3a50" stroke-width="2.3" stroke-linecap="round"/>';
      if (measure >= 359.5) {
        html += '<circle cx="' + O[0] + '" cy="' + O[1] + '" r="34" fill="none" stroke="#6c63f5" stroke-width="1.8"/>';
        html += txt(O[0], O[1] - 46, degLabel(measure), 'class="a" text-anchor="middle"');
      } else if (isRight(measure)) {
        html += rightOnly(O, Bpt, Apt, '90');
      } else {
        html += arcByDeg(O, startH, measure, 32, measure);
      }
      html += dots([O]);
      html += txt(Olab[0], Olab[1], V.O || 'O', 'class="v" text-anchor="middle"');
      html += txt(Alab[0], Alab[1], V.A || 'A', 'class="v" text-anchor="middle"');
      html += txt(Blab[0], Blab[1], V.B || 'B', 'class="v" text-anchor="middle"');
      return wrap(html);
    }
    if (t === 'parll') {
      var n = clamp(Math.round(num(X.n, 1)), 1, 6);
      var y1 = 86, y2 = 196;
      var htmlP =
        '<line x1="24" y1="' + y1 + '" x2="336" y2="' + y1 + '" stroke="#2c3a50" stroke-width="2.2"/>' +
        '<line x1="24" y1="' + y2 + '" x2="336" y2="' + y2 + '" stroke="#2c3a50" stroke-width="2.2"/>' +
        txt(346, y1 + 5, V.d1 || 'd₁', 'class="v"') +
        txt(346, y2 + 5, V.d2 || 'd₂', 'class="v"');
      var gap = n === 1 ? 0 : 190 / (n - 1);
      var x0 = n === 1 ? 180 : 80;
      for (var i = 0; i < n; i++) {
        var tilt = clamp(num(X['t' + i], num(X.tilt, 60)), 5, 175);
        var sinT = Math.sin(tilt * Math.PI / 180);
        var cosT = Math.cos(tilt * Math.PI / 180);
        if (Math.abs(sinT) < 0.02) sinT = 0.02;
        var pad = 22;
        var dy = (y2 + pad) - (y1 - pad);
        var tLen = dy / sinT;
        var xc = x0 + i * (n === 1 ? 0 : gap);
        var xt = xc - tLen * cosT / 2;
        var xb = xc + tLen * cosT / 2;
        htmlP += '<line x1="' + xt.toFixed(1) + '" y1="' + (y1 - pad) + '" x2="' + xb.toFixed(1) + '" y2="' + (y2 + pad) + '" stroke="#6c63f5" stroke-width="2"/>';
        var hit = [xt + (pad) * cosT / sinT, y1];
        if (isRight(tilt)) {
          htmlP += rightOnly(hit, [hit[0] + 40, hit[1]], [xb, y2 + pad], '90');
        } else {
          htmlP += arcByDeg(hit, -tilt, tilt, 24, tilt);
        }
      }
      return wrap(htmlP, '0 0 380 280');
    }
    if (t === 'cube' || t === 'box') {
      var iso = function (x, y, z) { return [180 + (x - z) * 0.86, 168 - y * 0.92 + (x + z) * 0.32]; };
      var L = t === 'cube' ? 88 : 108, W = t === 'cube' ? 88 : 70, H = t === 'cube' ? 88 : 78;
      var A = iso(0, 0, 0), B = iso(L, 0, 0), C = iso(L, 0, W), D = iso(0, 0, W);
      var E = iso(0, H, 0), F = iso(L, H, 0), G = iso(L, H, W), Hh = iso(0, H, W);
      var hid = '<polyline points="' + poly([D, C, G]) + '" fill="none" stroke="#2c3a50" stroke-width="1.4" stroke-dasharray="5 4"/>';
      hid += '<line x1="' + D[0] + '" y1="' + D[1] + '" x2="' + Hh[0] + '" y2="' + Hh[1] + '" stroke="#2c3a50" stroke-width="1.4" stroke-dasharray="5 4"/>';
      var vis = stroke([E, F, B, A]) + stroke([F, G, C, B]).replace('rgba(108,99,245,.06)', 'rgba(108,99,245,.10)') +
        stroke([E, F, G, Hh]).replace('rgba(108,99,245,.06)', 'rgba(39,196,168,.08)');
      vis += '<polyline points="' + poly([A, B, F, E, A]) + '" fill="none" stroke="#2c3a50" stroke-width="2.1"/>';
      vis += '<polyline points="' + poly([B, C]) + '" fill="none" stroke="#2c3a50" stroke-width="2.1"/>';
      vis += '<polyline points="' + poly([F, G, Hh, E]) + '" fill="none" stroke="#2c3a50" stroke-width="2.1"/>';
      var midAB = mid(A, B), midAE = mid(A, E), midAD = mid(A, D);
      return wrap(hid + vis +
        txt(midAB[0], midAB[1] + 16, t === 'cube' ? (S.s || 'a') : (S.a || 'a'), 'class="s" text-anchor="middle"') +
        txt(midAE[0] - 14, midAE[1], t === 'cube' ? (S.s || '') : (S.h || 'h'), 'class="s" text-anchor="middle"') +
        txt(midAD[0] + 16, midAD[1] + 4, t === 'cube' ? '' : (S.b || 'b'), 'class="s" text-anchor="middle"')
      );
    }
    if (t === 'cyl') {
      var cx = 180, cy = 148, rx = 64, ry = 22, h = 96;
      var top = cy - h / 2, bot = cy + h / 2;
      return wrap(
        '<ellipse cx="' + cx + '" cy="' + bot + '" rx="' + rx + '" ry="' + ry + '" fill="rgba(108,99,245,.05)" stroke="#2c3a50" stroke-width="2"/>' +
        '<path d="M' + (cx - rx) + ',' + top + ' L' + (cx - rx) + ',' + bot + ' M' + (cx + rx) + ',' + top + ' L' + (cx + rx) + ',' + bot + '" stroke="#2c3a50" stroke-width="2"/>' +
        '<ellipse cx="' + cx + '" cy="' + top + '" rx="' + rx + '" ry="' + ry + '" fill="rgba(39,196,168,.10)" stroke="#2c3a50" stroke-width="2"/>' +
        '<path d="M' + (cx - rx) + ',' + bot + ' A' + rx + ',' + ry + ' 0 0 0 ' + (cx + rx) + ',' + bot + '" fill="none" stroke="#2c3a50" stroke-width="1.4" stroke-dasharray="5 4"/>' +
        txt(cx + rx + 18, (top + bot) / 2, X.h || S.h || 'h', 'class="s"') +
        txt(cx, bot + 28, X.r ? ('r=' + X.r) : 'r', 'class="s" text-anchor="middle"')
      );
    }
    if (t === 'cone') {
      var cx = 180, baseY = 210, tip = [180, 48], rx = 78, ry = 24;
      return wrap(
        '<ellipse cx="' + cx + '" cy="' + baseY + '" rx="' + rx + '" ry="' + ry + '" fill="rgba(108,99,245,.06)" stroke="#2c3a50" stroke-width="2"/>' +
        '<path d="M' + (cx - rx) + ',' + baseY + ' A' + rx + ',' + ry + ' 0 0 0 ' + (cx + rx) + ',' + baseY + '" fill="none" stroke="#2c3a50" stroke-width="1.4" stroke-dasharray="5 4"/>' +
        '<line x1="' + tip[0] + '" y1="' + tip[1] + '" x2="' + (cx - rx) + '" y2="' + baseY + '" stroke="#2c3a50" stroke-width="2.1"/>' +
        '<line x1="' + tip[0] + '" y1="' + tip[1] + '" x2="' + (cx + rx) + '" y2="' + baseY + '" stroke="#2c3a50" stroke-width="2.1"/>' +
        '<line x1="' + tip[0] + '" y1="' + tip[1] + '" x2="' + cx + '" y2="' + baseY + '" stroke="#1f8a78" stroke-width="1.5" stroke-dasharray="4 3"/>' +
        txt(cx + rx + 16, (tip[1] + baseY) / 2, X.h || 'h', 'class="s"') +
        txt(cx, baseY + 30, X.r ? ('r=' + X.r) : 'r', 'class="s" text-anchor="middle"') +
        txt(tip[0], tip[1] - 10, V.S || 'S', 'class="v" text-anchor="middle"')
      );
    }
    if (t === 'sph') {
      return wrap(
        '<ellipse cx="180" cy="140" rx="86" ry="86" fill="rgba(39,196,168,.07)" stroke="#2c3a50" stroke-width="2.2"/>' +
        '<ellipse cx="180" cy="140" rx="86" ry="28" fill="none" stroke="#2c3a50" stroke-width="1.5"/>' +
        '<ellipse cx="180" cy="140" rx="86" ry="28" fill="none" stroke="#2c3a50" stroke-width="1.3" stroke-dasharray="5 4" transform="rotate(90 180 140)"/>' +
        '<line x1="180" y1="140" x2="266" y2="140" stroke="#1f8a78" stroke-width="1.7"/>' +
        '<circle cx="180" cy="140" r="3" fill="#2c3a50"/>' +
        txt(168, 132, V.O || 'O', 'class="v"') +
        txt(228, 128, X.r ? ('r=' + X.r) : 'r', 'class="s"')
      );
    }
    if (t === 'pyr') {
      var tip = [180, 42], A = [78, 214], B = [262, 200], C = [292, 146], D = [118, 158];
      return wrap(
        '<polygon points="' + poly([A, B, C, D]) + '" fill="rgba(108,99,245,.06)" stroke="#2c3a50" stroke-width="2"/>' +
        '<line x1="' + D[0] + '" y1="' + D[1] + '" x2="' + C[0] + '" y2="' + C[1] + '" stroke="#2c3a50" stroke-width="1.4" stroke-dasharray="5 4"/>' +
        '<line x1="' + tip[0] + '" y1="' + tip[1] + '" x2="' + D[0] + '" y2="' + D[1] + '" stroke="#2c3a50" stroke-width="1.4" stroke-dasharray="5 4"/>' +
        '<line x1="' + tip[0] + '" y1="' + tip[1] + '" x2="' + A[0] + '" y2="' + A[1] + '" stroke="#2c3a50" stroke-width="2.1"/>' +
        '<line x1="' + tip[0] + '" y1="' + tip[1] + '" x2="' + B[0] + '" y2="' + B[1] + '" stroke="#2c3a50" stroke-width="2.1"/>' +
        '<line x1="' + tip[0] + '" y1="' + tip[1] + '" x2="' + C[0] + '" y2="' + C[1] + '" stroke="#2c3a50" stroke-width="2.1"/>' +
        txt(tip[0], tip[1] - 10, V.S || 'S', 'class="v" text-anchor="middle"') +
        txt(A[0] - 10, A[1] + 16, V.A || 'A', 'class="v"') +
        txt(B[0] + 10, B[1] + 16, V.B || 'B', 'class="v"') +
        txt(180, 228, S.a || 'a', 'class="s" text-anchor="middle"') +
        txt(310, 120, X.h || 'h', 'class="s"')
      );
    }
    if (t === 'pris') {
      var A = [90, 206], B = [230, 214], C = [156, 150];
      var A2 = [128, 92], B2 = [268, 100], C2 = [194, 36];
      return wrap(
        '<polygon points="' + poly([A, B, C]) + '" fill="rgba(108,99,245,.05)" stroke="#2c3a50" stroke-width="2"/>' +
        '<polygon points="' + poly([A2, B2, C2]) + '" fill="rgba(39,196,168,.08)" stroke="#2c3a50" stroke-width="2"/>' +
        '<line x1="' + A[0] + '" y1="' + A[1] + '" x2="' + A2[0] + '" y2="' + A2[1] + '" stroke="#2c3a50" stroke-width="2"/>' +
        '<line x1="' + B[0] + '" y1="' + B[1] + '" x2="' + B2[0] + '" y2="' + B2[1] + '" stroke="#2c3a50" stroke-width="2"/>' +
        '<line x1="' + C[0] + '" y1="' + C[1] + '" x2="' + C2[0] + '" y2="' + C2[1] + '" stroke="#2c3a50" stroke-width="1.4" stroke-dasharray="5 4"/>' +
        txt(A[0] - 10, A[1] + 16, V.A || 'A', 'class="v"') +
        txt(B[0] + 10, B[1] + 16, V.B || 'B', 'class="v"') +
        txt(C[0] - 16, C[1], V.C || 'C', 'class="v"') +
        txt(A2[0] - 8, A2[1] - 6, V.A2 || "A'", 'class="v"') +
        txt(300, 150, X.h || 'h', 'class="s"')
      );
    }
    if (t === 'hex') {
      var cx = 180, cy = 140, R = 88;
      var HP = [];
      for (var hi = 0; hi < 6; hi++) {
        var hd = -90 + hi * 60;
        HP.push(polar([cx, cy], hd, R));
      }
      var Hc = centroid(HP);
      var hexH = stroke(HP) + dots(HP);
      var labs = ['A', 'B', 'C', 'D', 'E', 'F'];
      for (var hj = 0; hj < 6; hj++) {
        hexH += vertLabOut(HP[hj], V[labs[hj]] || labs[hj], Hc, 18);
        hexH += sideLabOut(HP[hj], HP[(hj + 1) % 6], hj === 0 ? (S.a || 'a') : '', Hc, 16);
      }
      return wrap(hexH);
    }
    if (t === 'kite') {
      var K = [[180, 36], [268, 120], [180, 236], [92, 120]];
      var Kc = centroid(K);
      return wrap(
        stroke(K) + dots(K) +
        '<line x1="180" y1="36" x2="180" y2="236" stroke="#1f8a78" stroke-width="1.3" stroke-dasharray="4 3"/>' +
        '<line x1="92" y1="120" x2="268" y2="120" stroke="#1f8a78" stroke-width="1.3" stroke-dasharray="4 3"/>' +
        vertLabOut(K[0], V.A || 'A', Kc, 18) +
        vertLabOut(K[1], V.B || 'B', Kc, 18) +
        vertLabOut(K[2], V.C || 'C', Kc, 18) +
        vertLabOut(K[3], V.D || 'D', Kc, 18) +
        sideLabOut(K[0], K[1], S.a, Kc, 16) +
        sideLabOut(K[1], K[2], S.b, Kc, 16) +
        txt(196, 86, X.d1 || '', 'class="s"') +
        txt(200, 116, X.d2 || '', 'class="s"')
      );
    }
    if (t === 'semi') {
      return wrap(
        '<path d="M70,180 A110,110 0 0 1 290,180 L70,180 Z" fill="rgba(108,99,245,.06)" stroke="#2c3a50" stroke-width="2.2"/>' +
        '<line x1="70" y1="180" x2="290" y2="180" stroke="#2c3a50" stroke-width="2.2"/>' +
        '<circle cx="180" cy="180" r="3" fill="#2c3a50"/>' +
        txt(180, 198, V.O || 'O', 'class="v" text-anchor="middle"') +
        txt(180, 78, X.r ? ('r=' + X.r) : 'r', 'class="s" text-anchor="middle"')
      );
    }
    if (t === 'sec') {
      var span = clamp(num(X.m, 70), 10, 350);
      var O = [180, 148], rr = 72;
      var a0 = 180 - span / 2, a1 = 180 + span / 2;
      var p1 = polar(O, a0, rr), p2 = polar(O, a1, rr);
      var large = span > 180 ? 1 : 0;
      var sweep = 0;
      var Apos = inbox(polar(O, a0, rr + 16)[0], polar(O, a0, rr + 16)[1], 22);
      var Bpos = inbox(polar(O, a1, rr + 16)[0], polar(O, a1, rr + 16)[1], 22);
      var Opos = inbox(O[0], O[1] + 20, 22);
      var rLab = inbox(polar(O, (a0 + a1) / 2, 34)[0], polar(O, (a0 + a1) / 2, 34)[1], 22);
      return wrap(
        '<rect x="14" y="14" width="332" height="252" rx="10" fill="#fbfcfe" stroke="#d5dce6"/>' +
        '<path d="M' + O[0] + ',' + O[1] + ' L' + p1[0].toFixed(1) + ',' + p1[1].toFixed(1) +
        ' A' + rr + ',' + rr + ' 0 ' + large + ' ' + sweep + ' ' + p2[0].toFixed(1) + ',' + p2[1].toFixed(1) +
        ' Z" fill="rgba(108,99,245,.10)" stroke="#2c3a50" stroke-width="2"/>' +
        dots([O]) +
        txt(Opos[0], Opos[1], V.O || 'O', 'class="v" text-anchor="middle"') +
        txt(Apos[0], Apos[1], V.A || 'A', 'class="v" text-anchor="middle"') +
        txt(Bpos[0], Bpos[1], V.B || 'B', 'class="v" text-anchor="middle"') +
        arcByDeg(O, a0, span, 24, span) +
        txt(rLab[0], rLab[1], X.r ? ('r=' + X.r) : '', 'class="s" text-anchor="middle"')
      );
    }
    if (t === 'ell') {
      return wrap(
        '<ellipse cx="180" cy="140" rx="120" ry="62" fill="rgba(108,99,245,.06)" stroke="#2c3a50" stroke-width="2.2"/>' +
        '<line x1="60" y1="140" x2="300" y2="140" stroke="#1f8a78" stroke-width="1.4"/>' +
        '<line x1="180" y1="78" x2="180" y2="202" stroke="#1f8a78" stroke-width="1.4"/>' +
        '<circle cx="180" cy="140" r="3" fill="#2c3a50"/>' +
        txt(180, 156, V.O || 'O', 'class="v" text-anchor="middle"') +
        txt(300, 128, X.a ? ('a=' + X.a) : 'a', 'class="s"') +
        txt(192, 78, X.b ? ('b=' + X.b) : 'b', 'class="s"')
      );
    }
    if (t === 'hemi') {
      return wrap(
        '<path d="M64,176 A96,96 0 0 1 296,176 L64,176 Z" fill="rgba(39,196,168,.08)" stroke="#2c3a50" stroke-width="2.1"/>' +
        '<ellipse cx="180" cy="176" rx="116" ry="28" fill="rgba(108,99,245,.05)" stroke="#2c3a50" stroke-width="2"/>' +
        '<path d="M64,176 A116,28 0 0 0 296,176" fill="none" stroke="#2c3a50" stroke-width="1.3" stroke-dasharray="5 4"/>' +
        '<line x1="180" y1="176" x2="180" y2="80" stroke="#1f8a78" stroke-width="1.5"/>' +
        txt(180, 198, V.O || 'O', 'class="v" text-anchor="middle"') +
        txt(196, 120, X.r ? ('r=' + X.r) : 'r', 'class="s"')
      );
    }
    if (t === 'frust') {
      return wrap(
        '<ellipse cx="180" cy="214" rx="110" ry="26" fill="rgba(108,99,245,.05)" stroke="#2c3a50" stroke-width="2"/>' +
        '<path d="M70,214 A110,26 0 0 0 290,214" fill="none" stroke="#2c3a50" stroke-width="1.3" stroke-dasharray="5 4"/>' +
        '<line x1="70" y1="214" x2="118" y2="78" stroke="#2c3a50" stroke-width="2.1"/>' +
        '<line x1="290" y1="214" x2="242" y2="78" stroke="#2c3a50" stroke-width="2.1"/>' +
        '<ellipse cx="180" cy="78" rx="62" ry="16" fill="rgba(39,196,168,.10)" stroke="#2c3a50" stroke-width="2"/>' +
        txt(180, 62, X.r ? ('r=' + X.r) : 'r', 'class="s" text-anchor="middle"') +
        txt(180, 250, X.R ? ('R=' + X.R) : 'R', 'class="s" text-anchor="middle"') +
        txt(304, 146, X.h || 'h', 'class="s"')
      );
    }
    if (t === 'pent' || t === 'oct' || t === 'hept' || t === 'deca' || t === 'ngon') {
      var n = t === 'pent' ? 5 : t === 'hept' ? 7 : t === 'oct' ? 8 : t === 'deca' ? 10 : clamp(Math.round(num(X.n, 5)), 3, 12);
      var cx = 180, cy = 140, R = n > 8 ? 82 : (n === 5 ? 92 : 86);
      var PP = [];
      for (var i = 0; i < n; i++) PP.push(polar([cx, cy], 90 + i * 360 / n, R));
      var Pc = centroid(PP);
      var labs = 'ABCDEFGHIJKL';
      var h = stroke(PP) + dots(PP);
      for (var j = 0; j < n; j++) {
        h += vertLabOut(PP[j], V[labs[j]] || labs[j], Pc, n > 8 ? 14 : 18);
        if (j === 0) h += sideLabOut(PP[j], PP[(j + 1) % n], S.a || 'a', Pc, 16);
      }
      return wrap(h);
    }
    if (t === 'ring') {
      return wrap(
        '<circle cx="180" cy="140" r="92" fill="rgba(108,99,245,.08)" stroke="#2c3a50" stroke-width="2.1"/>' +
        '<circle cx="180" cy="140" r="48" fill="#fff" stroke="#2c3a50" stroke-width="2.1"/>' +
        '<line x1="180" y1="140" x2="272" y2="140" stroke="#1f8a78" stroke-width="1.5"/>' +
        '<line x1="180" y1="140" x2="228" y2="140" stroke="#6c63f5" stroke-width="1.5"/>' +
        '<circle cx="180" cy="140" r="3" fill="#2c3a50"/>' +
        txt(166, 132, V.O || 'O', 'class="v" text-anchor="middle"') +
        txt(250, 128, X.R ? ('R=' + X.R) : 'R', 'class="s"') +
        txt(204, 128, X.r ? ('r=' + X.r) : 'r', 'class="s"')
      );
    }
    if (t === 'segm') {
      var span = clamp(num(X.m, 80), 20, 320);
      var O = [180, 150], rr = 86;
      var a0 = 90 - span / 2, a1 = 90 + span / 2;
      var p1 = polar(O, a0, rr), p2 = polar(O, a1, rr);
      var large = span > 180 ? 1 : 0;
      var sweep = 0;
      var Apos = inbox(polar(O, a0, rr + 18)[0], polar(O, a0, rr + 18)[1], 18);
      var Bpos = inbox(polar(O, a1, rr + 18)[0], polar(O, a1, rr + 18)[1], 18);
      var midLab = polar(O, 90, span > 180 ? rr * 0.35 : rr + 18);
      midLab = inbox(midLab[0], midLab[1], 18);
      return wrap(
        '<rect x="16" y="16" width="328" height="248" rx="12" fill="#fbfcfe" stroke="#d5dce6"/>' +
        '<path d="M' + p1[0].toFixed(1) + ',' + p1[1].toFixed(1) +
        ' A' + rr + ',' + rr + ' 0 ' + large + ' ' + sweep + ' ' + p2[0].toFixed(1) + ',' + p2[1].toFixed(1) +
        ' Z" fill="rgba(108,99,245,.12)" stroke="#2c3a50" stroke-width="2.1"/>' +
        '<line x1="' + p1[0].toFixed(1) + '" y1="' + p1[1].toFixed(1) + '" x2="' + p2[0].toFixed(1) + '" y2="' + p2[1].toFixed(1) + '" stroke="#1f8a78" stroke-width="2"/>' +
        dots([p1, p2]) +
        txt(Apos[0], Apos[1], V.A || 'A', 'class="v" text-anchor="middle"') +
        txt(Bpos[0], Bpos[1], V.B || 'B', 'class="v" text-anchor="middle"') +
        txt(midLab[0], midLab[1], X.r ? ('r=' + X.r) : '', 'class="s" text-anchor="middle"')
      );
    }
    if (t === 'arc') {
      var span = clamp(num(X.m, 80), 10, 350);
      var O = [180, 148], rr = 72;
      var a0 = 180 - span / 2, a1 = 180 + span / 2;
      var p1 = polar(O, a0, rr), p2 = polar(O, a1, rr);
      var large = span > 180 ? 1 : 0;
      var sweep = 0;
      var Apos = inbox(polar(O, a0, rr + 16)[0], polar(O, a0, rr + 16)[1], 22);
      var Bpos = inbox(polar(O, a1, rr + 16)[0], polar(O, a1, rr + 16)[1], 22);
      var Opos = inbox(O[0], O[1] + 20, 22);
      return wrap(
        '<rect x="14" y="14" width="332" height="252" rx="10" fill="#fbfcfe" stroke="#d5dce6"/>' +
        '<path d="M' + p1[0].toFixed(1) + ',' + p1[1].toFixed(1) +
        ' A' + rr + ',' + rr + ' 0 ' + large + ' ' + sweep + ' ' + p2[0].toFixed(1) + ',' + p2[1].toFixed(1) +
        '" fill="none" stroke="#2c3a50" stroke-width="2.3" stroke-linecap="round"/>' +
        '<line x1="' + O[0] + '" y1="' + O[1] + '" x2="' + p1[0].toFixed(1) + '" y2="' + p1[1].toFixed(1) + '" stroke="#c5cedb" stroke-dasharray="4 3"/>' +
        '<line x1="' + O[0] + '" y1="' + O[1] + '" x2="' + p2[0].toFixed(1) + '" y2="' + p2[1].toFixed(1) + '" stroke="#c5cedb" stroke-dasharray="4 3"/>' +
        dots([O, p1, p2]) +
        txt(Opos[0], Opos[1], V.O || 'O', 'class="v" text-anchor="middle"') +
        txt(Apos[0], Apos[1], V.A || 'A', 'class="v" text-anchor="middle"') +
        txt(Bpos[0], Bpos[1], V.B || 'B', 'class="v" text-anchor="middle"') +
        arcByDeg(O, a0, span, 24, span)
      );
    }
    if (t === 'pseg' || t === 'ray' || t === 'ln') {
      var A = [70, 150], B = [290, 150];
      var htmlL = '<line x1="' + A[0] + '" y1="' + A[1] + '" x2="' + B[0] + '" y2="' + B[1] + '" stroke="#2c3a50" stroke-width="2.4" stroke-linecap="round"/>';
      if (t === 'ray' || t === 'ln') {
        htmlL += '<polygon points="304,150 288,143 288,157" fill="#2c3a50"/>';
      }
      if (t === 'ln') {
        htmlL += '<polygon points="56,150 72,143 72,157" fill="#2c3a50"/>';
      }
      htmlL += dots([A, B]);
      htmlL += txt(A[0], A[1] + 24, V.A || 'A', 'class="v" text-anchor="middle"');
      htmlL += txt(B[0], B[1] + 24, V.B || 'B', 'class="v" text-anchor="middle"');
      if (S.a) htmlL += txt(180, 132, S.a, 'class="s" text-anchor="middle"');
      return wrap(htmlL);
    }
    if (t === 'tet') {
      var Spt = [180, 40], A = [70, 214], B = [278, 214], C = [214, 128];
      return wrap(
        '<polygon points="' + poly([Spt, A, B]) + '" fill="rgba(108,99,245,.06)" stroke="#2c3a50" stroke-width="2.1"/>' +
        '<line x1="' + Spt[0] + '" y1="' + Spt[1] + '" x2="' + C[0] + '" y2="' + C[1] + '" stroke="#2c3a50" stroke-width="1.5" stroke-dasharray="5 4"/>' +
        '<line x1="' + A[0] + '" y1="' + A[1] + '" x2="' + C[0] + '" y2="' + C[1] + '" stroke="#2c3a50" stroke-width="1.5" stroke-dasharray="5 4"/>' +
        '<line x1="' + B[0] + '" y1="' + B[1] + '" x2="' + C[0] + '" y2="' + C[1] + '" stroke="#2c3a50" stroke-width="2.1"/>' +
        dots([Spt, A, B, C]) +
        txt(Spt[0], Spt[1] - 10, V.S || 'S', 'class="v" text-anchor="middle"') +
        txt(A[0] - 8, A[1] + 16, V.A || 'A', 'class="v"') +
        txt(B[0] + 8, B[1] + 16, V.B || 'B', 'class="v"') +
        txt(C[0] + 12, C[1] - 4, V.C || 'C', 'class="v"') +
        txt(310, 130, X.h || 'h', 'class="s"')
      );
    }
    if (t === 'hpris') {
      var hex = function (cx, cy, R) {
        var pts = [];
        for (var i = 0; i < 6; i++) pts.push(polar([cx, cy], 30 + i * 60, R));
        return pts;
      };
      var bot = hex(168, 198, 56), top = hex(200, 86, 56);
      var hp = '<polygon points="' + poly(bot) + '" fill="rgba(108,99,245,.05)" stroke="#2c3a50" stroke-width="1.8"/>';
      hp += '<polygon points="' + poly(top) + '" fill="rgba(39,196,168,.08)" stroke="#2c3a50" stroke-width="1.8"/>';
      for (var hi = 0; hi < 6; hi++) {
        var dash = hi === 4 || hi === 5;
        hp += '<line x1="' + bot[hi][0] + '" y1="' + bot[hi][1] + '" x2="' + top[hi][0] + '" y2="' + top[hi][1] +
          '" stroke="#2c3a50" stroke-width="' + (dash ? '1.3' : '1.8') + '"' + (dash ? ' stroke-dasharray="5 4"' : '') + '/>';
      }
      hp += txt(320, 140, X.h || 'h', 'class="s"');
      return wrap(hp);
    }
    if (t === 'netc') {
      var s = 52, ox = 78, oy = 88;
      function sq(x, y) {
        return '<rect x="' + x + '" y="' + y + '" width="' + s + '" height="' + s + '" fill="rgba(108,99,245,.07)" stroke="#2c3a50" stroke-width="1.8"/>';
      }
      return wrap(
        sq(ox + s, oy - s) + sq(ox, oy) + sq(ox + s, oy) + sq(ox + 2 * s, oy) + sq(ox + 3 * s, oy) + sq(ox + s, oy + s) +
        txt(ox + s + 26, oy - s + 32, '۱', 'class="s" text-anchor="middle"') +
        txt(ox + 26, oy + 32, '۲', 'class="s" text-anchor="middle"') +
        txt(ox + s + 26, oy + 32, '۳', 'class="s" text-anchor="middle"') +
        txt(ox + 2 * s + 26, oy + 32, '۴', 'class="s" text-anchor="middle"') +
        txt(ox + 3 * s + 26, oy + 32, '۵', 'class="s" text-anchor="middle"') +
        txt(ox + s + 26, oy + s + 32, '۶', 'class="s" text-anchor="middle"')
      );
    }
    if (t === 'thales') {
      var A = [180, 40], B = [56, 230], C = [304, 230], D = [108, 148], E = [252, 148];
      return wrap(
        stroke([A, B, C]) +
        '<line x1="' + D[0] + '" y1="' + D[1] + '" x2="' + E[0] + '" y2="' + E[1] + '" stroke="#6c63f5" stroke-width="2.2"/>' +
        dots([A, B, C, D, E]) +
        txt(180, 28, V.A || 'A', 'class="v" text-anchor="middle"') +
        txt(42, 246, V.B || 'B', 'class="v"') +
        txt(310, 246, V.C || 'C', 'class="v"') +
        txt(96, 140, V.D || 'D', 'class="v"') +
        txt(258, 140, V.E || 'E', 'class="v"')
      );
    }
    if (t === 'chord') {
      var O = [180, 150], r = 88, B = polar(O, 210, r), C = polar(O, 330, r), A = polar(O, 90, r);
      return wrap(
        '<circle cx="180" cy="150" r="88" fill="rgba(108,99,245,.05)" stroke="#2c3a50" stroke-width="2.1"/>' +
        '<line x1="' + B[0] + '" y1="' + B[1] + '" x2="' + C[0] + '" y2="' + C[1] + '" stroke="#1f8a78" stroke-width="2"/>' +
        '<line x1="' + A[0] + '" y1="' + A[1] + '" x2="' + B[0] + '" y2="' + B[1] + '" stroke="#2c3a50" stroke-width="1.7"/>' +
        '<line x1="' + A[0] + '" y1="' + A[1] + '" x2="' + C[0] + '" y2="' + C[1] + '" stroke="#2c3a50" stroke-width="1.7"/>' +
        '<line x1="' + O[0] + '" y1="' + O[1] + '" x2="' + B[0] + '" y2="' + B[1] + '" stroke="#6c63f5" stroke-width="1.4"/>' +
        '<line x1="' + O[0] + '" y1="' + O[1] + '" x2="' + C[0] + '" y2="' + C[1] + '" stroke="#6c63f5" stroke-width="1.4"/>' +
        dots([O, A, B, C]) +
        txt(O[0] + 8, O[1] + 16, V.O || 'O', 'class="v"') +
        txt(A[0], A[1] - 10, V.A || 'A', 'class="v" text-anchor="middle"') +
        txt(B[0] - 12, B[1] + 16, V.B || 'B', 'class="v"') +
        txt(C[0] + 12, C[1] + 16, V.C || 'C', 'class="v"') +
        angleArc(A, B, C, 18, Ang.A || '')
      );
    }
    if (t === 'tang') {
      var O = [150, 150], r = 78, Tpt = polar(O, 40, r);
      var tx = Tpt[0] - O[0], ty = Tpt[1] - O[1];
      var L = Math.hypot(tx, ty) || 1;
      var px = -ty / L, py = tx / L;
      var P1 = [Tpt[0] + px * 90, Tpt[1] + py * 90], P2 = [Tpt[0] - px * 90, Tpt[1] - py * 90];
      return wrap(
        '<circle cx="150" cy="150" r="78" fill="rgba(108,99,245,.05)" stroke="#2c3a50" stroke-width="2.1"/>' +
        '<line x1="' + P1[0] + '" y1="' + P1[1] + '" x2="' + P2[0] + '" y2="' + P2[1] + '" stroke="#e4572e" stroke-width="2.1"/>' +
        '<line x1="' + O[0] + '" y1="' + O[1] + '" x2="' + Tpt[0] + '" y2="' + Tpt[1] + '" stroke="#1f8a78" stroke-width="1.6"/>' +
        dots([O, Tpt]) +
        rightOnly(Tpt, O, P1, '90') +
        txt(O[0] - 14, O[1] + 6, V.O || 'O', 'class="v"') +
        txt(Tpt[0] + 12, Tpt[1] - 8, V.T || 'T', 'class="v"')
      );
    }

    if (t === 'star') {
      var cxS = 180, cyS = 142, Ro = 96, Ri = 38, SP = [], si;
      for (si = 0; si < 10; si++) {
        SP.push(polar([cxS, cyS], -90 + si * 36, si % 2 === 0 ? Ro : Ri));
      }
      var Sc = centroid(SP);
      var sh = stroke(SP) + dots([SP[0], SP[2], SP[4], SP[6], SP[8]]);
      var sLabs = ['A', 'B', 'C', 'D', 'E'];
      for (si = 0; si < 5; si++) sh += vertLabOut(SP[si * 2], V[sLabs[si]] || sLabs[si], Sc, 18);
      sh += sideLabOut(SP[0], SP[2], S.a || 'a', Sc, 16);
      return wrap(sh);
    }
    if (t === 'torus') {
      var cxT = 180, cyT = 148;
      return wrap(
        '<ellipse cx="' + cxT + '" cy="' + cyT + '" rx="118" ry="72" fill="rgba(108,99,245,.06)" stroke="#2c3a50" stroke-width="2.1"/>' +
        '<ellipse cx="' + cxT + '" cy="' + cyT + '" rx="48" ry="22" fill="#eef2f7" stroke="#2c3a50" stroke-width="2"/>' +
        '<ellipse cx="' + cxT + '" cy="' + (cyT - 6) + '" rx="118" ry="28" fill="none" stroke="#2c3a50" stroke-width="1.4"/>' +
        '<path d="M62,148 A118,72 0 0 0 298,148" fill="none" stroke="#2c3a50" stroke-width="1.3" stroke-dasharray="5 4"/>' +
        txt(cxT + 132, cyT, X.R ? ('R=' + X.R) : 'R', 'class="s"') +
        txt(cxT, cyT + 8, X.r ? ('r=' + X.r) : 'r', 'class="s" text-anchor="middle"')
      );
    }
    if (t === 'elps') {
      return wrap(
        '<ellipse cx="180" cy="146" rx="120" ry="72" fill="rgba(39,196,168,.07)" stroke="#2c3a50" stroke-width="2.2"/>' +
        '<ellipse cx="180" cy="146" rx="120" ry="24" fill="none" stroke="#2c3a50" stroke-width="1.5"/>' +
        '<ellipse cx="180" cy="146" rx="42" ry="72" fill="none" stroke="#2c3a50" stroke-width="1.4"/>' +
        '<path d="M180,74 A42,72 0 0 0 180,218" fill="none" stroke="#2c3a50" stroke-width="1.2" stroke-dasharray="5 4"/>' +
        '<circle cx="180" cy="146" r="3" fill="#2c3a50"/>' +
        txt(168, 138, V.O || 'O', 'class="v"') +
        txt(250, 132, X.a ? ('a=' + X.a) : 'a', 'class="s"') +
        txt(198, 88, X.b ? ('b=' + X.b) : 'b', 'class="s"') +
        txt(318, 150, X.c ? ('c=' + X.c) : '', 'class="s"')
      );
    }
    if (t === 'octa') {
      var OT = [180, 34], OB = [180, 246], OL = [64, 156], OR = [296, 148], OF = [180, 188], OK = [180, 112];
      return wrap(
        '<polygon points="' + poly([OT, OL, OF]) + '" fill="rgba(108,99,245,.07)" stroke="#2c3a50" stroke-width="2.1"/>' +
        '<polygon points="' + poly([OT, OF, OR]) + '" fill="rgba(39,196,168,.08)" stroke="#2c3a50" stroke-width="2.1"/>' +
        '<polygon points="' + poly([OL, OF, OB]) + '" fill="rgba(108,99,245,.04)" stroke="#2c3a50" stroke-width="2"/>' +
        '<polygon points="' + poly([OF, OR, OB]) + '" fill="rgba(39,196,168,.05)" stroke="#2c3a50" stroke-width="2"/>' +
        '<line x1="' + OT[0] + '" y1="' + OT[1] + '" x2="' + OK[0] + '" y2="' + OK[1] + '" stroke="#2c3a50" stroke-width="1.4" stroke-dasharray="5 4"/>' +
        '<line x1="' + OL[0] + '" y1="' + OL[1] + '" x2="' + OR[0] + '" y2="' + OR[1] + '" stroke="#2c3a50" stroke-width="1.4" stroke-dasharray="5 4"/>' +
        '<line x1="' + OK[0] + '" y1="' + OK[1] + '" x2="' + OB[0] + '" y2="' + OB[1] + '" stroke="#2c3a50" stroke-width="1.3" stroke-dasharray="5 4"/>' +
        dots([OT, OL, OR, OF, OB]) +
        txt(OT[0], OT[1] - 10, V.S || 'S', 'class="v" text-anchor="middle"') +
        txt(OL[0] - 12, OL[1] + 4, V.A || 'A', 'class="v"') +
        txt(OR[0] + 10, OR[1] + 4, V.B || 'B', 'class="v"') +
        txt(OF[0] + 10, OF[1] + 16, V.C || 'C', 'class="v"') +
        txt(OB[0], OB[1] + 16, V.D || 'D', 'class="v" text-anchor="middle"') +
        txt(240, 90, S.a || 'a', 'class="s"')
      );
    }
    if (t === 'dodec') {
      var cxD = 180, cyD = 142, RD = 108, rD = 58, outer = [], inner = [], di;
      for (di = 0; di < 5; di++) {
        outer.push(polar([cxD, cyD], -90 + di * 72, RD));
        inner.push(polar([cxD, cyD], -90 + 36 + di * 72, rD));
      }
      var dh = stroke(outer);
      dh += '<polygon points="' + poly(inner) + '" fill="rgba(39,196,168,.08)" stroke="#2c3a50" stroke-width="1.8"/>';
      for (di = 0; di < 5; di++) {
        dh += '<line x1="' + outer[di][0] + '" y1="' + outer[di][1] + '" x2="' + inner[di][0] + '" y2="' + inner[di][1] + '" stroke="#2c3a50" stroke-width="1.6"/>';
        dh += '<line x1="' + outer[di][0] + '" y1="' + outer[di][1] + '" x2="' + inner[(di + 4) % 5][0] + '" y2="' + inner[(di + 4) % 5][1] + '" stroke="#2c3a50" stroke-width="1.6"/>';
      }
      dh += dots(outer);
      dh += txt(outer[0][0], outer[0][1] - 10, V.A || 'A', 'class="v" text-anchor="middle"');
      dh += txt(cxD, cyD + 4, S.a || 'a', 'class="s" text-anchor="middle"');
      return wrap(dh);
    }
    if (t === 'icosa') {
      var IT = [180, 28], IB = [180, 252];
      var IU = [[92, 88], [180, 70], [268, 88], [236, 130], [124, 130]];
      var IL = [[92, 192], [124, 150], [236, 150], [268, 192], [180, 210]];
      var ih = '<polygon points="' + poly([IT, IU[0], IU[1]]) + '" fill="rgba(108,99,245,.07)" stroke="#2c3a50" stroke-width="1.7"/>';
      ih += '<polygon points="' + poly([IT, IU[1], IU[2]]) + '" fill="rgba(39,196,168,.08)" stroke="#2c3a50" stroke-width="1.7"/>';
      ih += '<polygon points="' + poly([IU[0], IU[1], IU[4]]) + '" fill="rgba(108,99,245,.05)" stroke="#2c3a50" stroke-width="1.6"/>';
      ih += '<polygon points="' + poly([IU[1], IU[2], IU[3]]) + '" fill="rgba(39,196,168,.05)" stroke="#2c3a50" stroke-width="1.6"/>';
      ih += '<polygon points="' + poly([IU[1], IU[3], IU[4]]) + '" fill="rgba(108,99,245,.04)" stroke="#2c3a50" stroke-width="1.6"/>';
      ih += '<polygon points="' + poly([IU[0], IU[4], IL[0]]) + '" fill="rgba(108,99,245,.06)" stroke="#2c3a50" stroke-width="1.6"/>';
      ih += '<polygon points="' + poly([IU[2], IU[3], IL[3]]) + '" fill="rgba(39,196,168,.06)" stroke="#2c3a50" stroke-width="1.6"/>';
      ih += '<polygon points="' + poly([IU[4], IL[1], IL[0]]) + '" fill="rgba(108,99,245,.04)" stroke="#2c3a50" stroke-width="1.6"/>';
      ih += '<polygon points="' + poly([IU[3], IL[2], IL[3]]) + '" fill="rgba(39,196,168,.04)" stroke="#2c3a50" stroke-width="1.6"/>';
      ih += '<polygon points="' + poly([IU[4], IU[3], IL[2], IL[1]]) + '" fill="rgba(108,99,245,.03)" stroke="#2c3a50" stroke-width="1.5"/>';
      ih += '<polygon points="' + poly([IL[0], IL[4], IB]) + '" fill="rgba(108,99,245,.06)" stroke="#2c3a50" stroke-width="1.7"/>';
      ih += '<polygon points="' + poly([IL[4], IL[3], IB]) + '" fill="rgba(39,196,168,.06)" stroke="#2c3a50" stroke-width="1.7"/>';
      ih += '<line x1="' + IU[0][0] + '" y1="' + IU[0][1] + '" x2="' + IU[2][0] + '" y2="' + IU[2][1] + '" stroke="#2c3a50" stroke-width="1.2" stroke-dasharray="4 3"/>';
      ih += dots([IT, IB, IU[0], IU[2]]) +
        txt(IT[0], IT[1] - 10, V.S || 'S', 'class="v" text-anchor="middle"') +
        txt(IB[0], IB[1] + 16, V.A || 'A', 'class="v" text-anchor="middle"') +
        txt(250, 48, S.a || 'a', 'class="s"');
      return wrap(ih);
    }
    return wrap(stroke([[180, 48], [64, 214], [300, 214]]));
  }

  function iconOf(id) {
    const mini = {
      tri: '<polygon points="18,4 4,24 32,24" fill="none" stroke="currentColor" stroke-width="2"/>',
      rtri: '<path d="M6,6 V24 H28 Z" fill="none" stroke="currentColor" stroke-width="2"/><path d="M6,18 H12 V24" fill="none" stroke="currentColor"/>',
      iso: '<polygon points="18,5 7,24 29,24" fill="none" stroke="currentColor" stroke-width="2"/>',
      eq: '<polygon points="18,5 5,24 31,24" fill="none" stroke="currentColor" stroke-width="2"/>',
      sq: '<rect x="7" y="6" width="22" height="18" fill="none" stroke="currentColor" stroke-width="2"/>',
      rect: '<rect x="5" y="8" width="26" height="14" fill="none" stroke="currentColor" stroke-width="2"/>',
      para: '<polygon points="10,7 30,7 26,23 6,23" fill="none" stroke="currentColor" stroke-width="2"/>',
      rhomb: '<polygon points="18,5 30,15 18,25 6,15" fill="none" stroke="currentColor" stroke-width="2"/>',
      trap: '<polygon points="11,8 25,8 31,23 5,23" fill="none" stroke="currentColor" stroke-width="2"/>',
      circ: '<circle cx="18" cy="15" r="10" fill="none" stroke="currentColor" stroke-width="2"/>',
      ang: '<path d="M6,24 H32 M6,24 L26,6" fill="none" stroke="currentColor" stroke-width="2"/>',
      parll: '<path d="M4,8 H32 M4,22 H32 M10,4 L22,26" fill="none" stroke="currentColor" stroke-width="2"/>',
      cube: '<g fill="none" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round">' +
        '<polygon points="18,4 30,10 18,16 6,10"/>' +
        '<polygon points="6,10 18,16 18,28 6,22"/>' +
        '<polygon points="18,16 30,10 30,22 18,28"/>' +
        '</g>',
      box: '<g fill="none" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round">' +
        '<polygon points="10,6 32,8 26,14 4,12"/>' +
        '<polygon points="4,12 26,14 26,26 4,24"/>' +
        '<polygon points="26,14 32,8 32,20 26,26"/>' +
        '</g>',
      cyl: '<g fill="none" stroke="currentColor" stroke-width="1.7">' +
        '<ellipse cx="18" cy="7" rx="11" ry="4"/>' +
        '<path d="M7,7 V21"/>' +
        '<path d="M29,7 V21"/>' +
        '<ellipse cx="18" cy="21" rx="11" ry="4"/>' +
        '<path d="M7,21 A11,4 0 0 0 29,21" stroke-dasharray="2 2"/>' +
        '</g>',
      cone: '<g fill="none" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round">' +
        '<path d="M18,3 L6,22"/>' +
        '<path d="M18,3 L30,22"/>' +
        '<ellipse cx="18" cy="22" rx="12" ry="4.2"/>' +
        '<path d="M6,22 A12,4.2 0 0 0 30,22" stroke-dasharray="2 2"/>' +
        '</g>',
      sph: '<g fill="none" stroke="currentColor" stroke-width="1.7">' +
        '<circle cx="18" cy="15" r="11"/>' +
        '<ellipse cx="18" cy="15" rx="11" ry="4.4"/>' +
        '<ellipse cx="18" cy="15" rx="4.4" ry="11"/>' +
        '</g>',
      pyr: '<g fill="none" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round">' +
        '<polygon points="18,3 5,24 31,24"/>' +
        '<path d="M18,3 L22,24"/>' +
        '<path d="M5,24 L22,24 L31,24" />' +
        '<path d="M18,3 L8,20" stroke-dasharray="2 2"/>' +
        '</g>',
      pris: '<g fill="none" stroke="currentColor" stroke-width="1.65" stroke-linejoin="round">' +
        '<polygon points="8,26 20,26 14,17"/>' +
        '<polygon points="14,8 26,8 20,0"/>' +
        '<path d="M8,26 L14,8"/>' +
        '<path d="M20,26 L26,8"/>' +
        '<path d="M14,17 L20,0" stroke-dasharray="2 2"/>' +
        '</g>',
      hex: '<polygon points="18,3 29,9 29,21 18,27 7,21 7,9" fill="none" stroke="currentColor" stroke-width="1.7"/>',
      kite: '<polygon points="18,3 30,13 18,27 6,13" fill="none" stroke="currentColor" stroke-width="1.7"/>',
      semi: '<path d="M6,22 A12,12 0 0 1 30,22 Z" fill="none" stroke="currentColor" stroke-width="1.7"/>',
      sec: '<path d="M18,24 L8,8 A16,16 0 0 1 30,12 Z" fill="none" stroke="currentColor" stroke-width="1.7"/>',
      ell: '<ellipse cx="18" cy="15" rx="14" ry="8" fill="none" stroke="currentColor" stroke-width="1.7"/>',
      hemi: '<path d="M6,20 A12,12 0 0 1 30,20" fill="none" stroke="currentColor" stroke-width="1.7"/><ellipse cx="18" cy="20" rx="12" ry="4" fill="none" stroke="currentColor" stroke-width="1.6"/>',
      frust: '<g fill="none" stroke="currentColor" stroke-width="1.6"><ellipse cx="18" cy="8" rx="7" ry="2.4"/><path d="M11,8 L6,22 M25,8 L30,22"/><ellipse cx="18" cy="22" rx="12" ry="3.4"/></g>',
      pent: '<polygon points="18,3 32,13 26,28 10,28 4,13" fill="none" stroke="currentColor" stroke-width="1.7"/>',
      oct: '<polygon points="13,3 23,3 31,10 31,20 23,27 13,27 5,20 5,10" fill="none" stroke="currentColor" stroke-width="1.6"/>',
      thales: '<path d="M18,4 L4,26 H32 Z M10,16 H26" fill="none" stroke="currentColor" stroke-width="1.6"/>',
      chord: '<circle cx="18" cy="15" r="10" fill="none" stroke="currentColor" stroke-width="1.6"/><path d="M10,21 L26,21 M18,5 L10,21 L26,21" fill="none" stroke="currentColor" stroke-width="1.4"/>',
      tang: '<circle cx="15" cy="16" r="8" fill="none" stroke="currentColor" stroke-width="1.6"/><path d="M8,6 L32,22" fill="none" stroke="currentColor" stroke-width="1.6"/>',
      scal: '<polygon points="26,4 4,26 32,20" fill="none" stroke="currentColor" stroke-width="1.7"/>',
      itrap: '<polygon points="10,8 26,8 32,24 4,24" fill="none" stroke="currentColor" stroke-width="1.7"/>',
      rtrap: '<path d="M8,8 H24 V24 H4 Z" fill="none" stroke="currentColor" stroke-width="1.7"/>',
      hept: '<polygon points="18,3 28,7 32,16 26,26 10,26 4,16 8,7" fill="none" stroke="currentColor" stroke-width="1.5"/>',
      deca: '<polygon points="18,3 24,5 29,10 31,16 29,22 24,26 18,27 12,26 7,22 5,16 7,10 12,5" fill="none" stroke="currentColor" stroke-width="1.4"/>',
      ngon: '<polygon points="18,4 29,10 29,20 18,26 7,20 7,10" fill="none" stroke="currentColor" stroke-width="1.6"/>',
      ring: '<circle cx="18" cy="15" r="10" fill="none" stroke="currentColor" stroke-width="1.6"/><circle cx="18" cy="15" r="5" fill="none" stroke="currentColor" stroke-width="1.6"/>',
      segm: '<path d="M6,20 A14,14 0 0 1 30,20 Z" fill="none" stroke="currentColor" stroke-width="1.6"/>',
      arc: '<path d="M6,20 A14,14 0 0 1 30,10" fill="none" stroke="currentColor" stroke-width="1.8"/>',
      pseg: '<path d="M6,15 H30" stroke="currentColor" stroke-width="1.8"/><circle cx="6" cy="15" r="2" fill="currentColor"/><circle cx="30" cy="15" r="2" fill="currentColor"/>',
      ray: '<path d="M6,15 H28" stroke="currentColor" stroke-width="1.8"/><polygon points="32,15 26,12 26,18" fill="currentColor"/><circle cx="6" cy="15" r="2" fill="currentColor"/>',
      ln: '<path d="M8,15 H28" stroke="currentColor" stroke-width="1.8"/><polygon points="4,15 10,12 10,18" fill="currentColor"/><polygon points="32,15 26,12 26,18" fill="currentColor"/>',
      tet: '<polygon points="18,4 5,26 31,26" fill="none" stroke="currentColor" stroke-width="1.6"/><path d="M18,4 L22,18 L5,26 M22,18 L31,26" fill="none" stroke="currentColor"/>',
      hpris: '<polygon points="10,22 16,26 24,24 22,18 16,14 8,16" fill="none" stroke="currentColor" stroke-width="1.3"/><polygon points="14,6 20,10 28,8 26,2 20,-1 12,1" fill="none" stroke="currentColor" stroke-width="1.3" transform="translate(0,4)"/>',
      netc: '<path d="M14,2 H22 V10 H30 V18 H22 V26 H14 V18 H6 V10 H14 Z" fill="none" stroke="currentColor" stroke-width="1.4"/>',
      acut: '<polygon points="18,4 6,26 30,24" fill="none" stroke="currentColor" stroke-width="1.7"/>',
      obt: '<polygon points="4,8 8,26 32,20" fill="none" stroke="currentColor" stroke-width="1.7"/>',
      star: '<polygon points="18,3 21,12 31,12 23,18 26,27 18,21 10,27 13,18 5,12 15,12" fill="none" stroke="currentColor" stroke-width="1.4"/>',
      torus: '<ellipse cx="18" cy="15" rx="13" ry="8" fill="none" stroke="currentColor" stroke-width="1.6"/><ellipse cx="18" cy="15" rx="5" ry="3" fill="none" stroke="currentColor" stroke-width="1.5"/>',
      elps: '<ellipse cx="18" cy="15" rx="14" ry="8" fill="none" stroke="currentColor" stroke-width="1.6"/><ellipse cx="18" cy="15" rx="14" ry="3" fill="none" stroke="currentColor"/><ellipse cx="18" cy="15" rx="5" ry="8" fill="none" stroke="currentColor"/>',
      octa: '<polygon points="18,3 6,15 18,27 30,15" fill="none" stroke="currentColor" stroke-width="1.6"/><path d="M18,3 L18,27 M6,15 L30,15" fill="none" stroke="currentColor"/>',
      dodec: '<polygon points="18,3 30,12 26,26 10,26 6,12" fill="none" stroke="currentColor" stroke-width="1.5"/><polygon points="18,10 24,14 22,21 14,21 12,14" fill="none" stroke="currentColor" stroke-width="1.3"/>',
      icosa: '<polygon points="18,3 6,24 30,24" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M18,3 L18,24 M6,24 L22,10 L30,24 M6,24 L14,10" fill="none" stroke="currentColor"/>'
    };
    return '<svg viewBox="0 0 36 30" aria-hidden="true">' + (mini[id] || mini.tri) + '</svg>';
  }

  function fieldsFor(t) {
    const commonV = [['V.A', 'رأس A'], ['V.B', 'رأس B'], ['V.C', 'رأس C']];
    if (t === 'sq' || t === 'rect' || t === 'para' || t === 'rhomb' || t === 'trap' || t === 'itrap' || t === 'rtrap')
      commonV.push(['V.D', 'رأس D']);
    if (t === 'circ') return [['V.O', 'مرکز'], ['X.r', 'شعاع r']];
    if (t === 'cube') return [['S.s', 'طول یال a']];
    if (t === 'box') return [['S.a', 'طول a'], ['S.b', 'عرض b'], ['S.h', 'ارتفاع h']];
    if (t === 'cyl') return [['X.r', 'شعاع قاعده r'], ['X.h', 'ارتفاع h']];
    if (t === 'cone') return [['V.S', 'رأس'], ['X.r', 'شعاع قاعده r'], ['X.h', 'ارتفاع h']];
    if (t === 'sph') return [['V.O', 'مرکز'], ['X.r', 'شعاع r']];
    if (t === 'pyr') return [['V.S', 'رأس'], ['V.A', 'رأس قاعده A'], ['V.B', 'رأس قاعده B'], ['S.a', 'ضلع قاعده'], ['X.h', 'ارتفاع h']];
    if (t === 'pris') return [['V.A', 'A'], ['V.B', 'B'], ['V.C', 'C'], ['X.h', 'ارتفاع h']];
    if (t === 'hex' || t === 'pent' || t === 'hept' || t === 'oct' || t === 'deca') return [['S.a', 'طول ضلع a']];
    if (t === 'ngon') return [['X.n', 'تعداد اضلاع n', 'range', 3, 12, 1], ['S.a', 'طول ضلع a']];
    if (t === 'ring') return [['V.O', 'مرکز'], ['X.R', 'شعاع بیرونی R'], ['X.r', 'شعاع درونی r']];
    if (t === 'segm') return [['V.A', 'A'], ['V.B', 'B'], ['X.r', 'شعاع'], ['X.m', 'زاویهٔ کمان', 'range', 20, 320, 1]];
    if (t === 'arc') return [['V.O', 'مرکز'], ['V.A', 'A'], ['V.B', 'B'], ['X.m', 'اندازه کمان', 'range', 10, 350, 1]];
    if (t === 'pseg' || t === 'ray' || t === 'ln') return [['V.A', 'نقطه A'], ['V.B', 'نقطه B'], ['S.a', 'طول']];
    if (t === 'tet') return [['V.S', 'رأس'], ['V.A', 'A'], ['V.B', 'B'], ['V.C', 'C'], ['X.h', 'ارتفاع']];
    if (t === 'hpris') return [['X.h', 'ارتفاع h'], ['S.a', 'ضلع قاعده']];
    if (t === 'netc') return [];
    if (t === 'kite') return [['V.A', 'A'], ['V.B', 'B'], ['V.C', 'C'], ['V.D', 'D'], ['S.a', 'ضلع AB'], ['S.b', 'ضلع BC'], ['X.d1', 'قطر AC'], ['X.d2', 'قطر BD']];
    if (t === 'semi') return [['V.O', 'مرکز'], ['X.r', 'شعاع r']];
    if (t === 'sec') return [['V.O', 'مرکز'], ['X.r', 'شعاع r'], ['X.m', 'زاویهٔ مرکزی', 'range', 10, 350, 1]];
    if (t === 'ell') return [['V.O', 'مرکز'], ['X.a', 'نیم‌قطر بزرگ a'], ['X.b', 'نیم‌قطر کوچک b']];
    if (t === 'hemi') return [['V.O', 'مرکز'], ['X.r', 'شعاع r']];
    if (t === 'frust') return [['X.R', 'شعاع قاعده بزرگ R'], ['X.r', 'شعاع قاعده کوچک r'], ['X.h', 'ارتفاع h']];
    if (t === 'star') return [['V.A', 'A'], ['V.B', 'B'], ['V.C', 'C'], ['V.D', 'D'], ['V.E', 'E'], ['S.a', 'طول ضلع']];
    if (t === 'torus') return [['X.R', 'شعاع بزرگ R'], ['X.r', 'شعاع لوله r']];
    if (t === 'elps') return [['V.O', 'مرکز'], ['X.a', 'نیم‌قطر a'], ['X.b', 'نیم‌قطر b'], ['X.c', 'نیم‌قطر c']];
    if (t === 'octa' || t === 'dodec' || t === 'icosa') return [['V.S', 'رأس'], ['V.A', 'A'], ['V.B', 'B'], ['S.a', 'طول یال a']];
    if (t === 'ang') return [
      ['V.O', 'رأس زاویه'], ['V.A', 'ضلع اول'], ['V.B', 'ضلع دوم'],
      ['X.m', 'اندازه زاویه (۰ تا ۳۶۰)', 'range', 0, 360, 1]
    ];
    if (t === 'parll') {
      var nLines = clamp(Math.round(num((state && state.X && state.X.n), 1)), 1, 6);
      var prow = [
        ['V.d1', 'خط بالا'], ['V.d2', 'خط پایین'],
        ['X.n', 'تعداد خطوط مورب', 'range', 1, 6, 1]
      ];
      for (var pi = 0; pi < nLines; pi++) {
        prow.push(['X.t' + pi, 'زاویهٔ مورب ' + (pi + 1), 'range', 5, 175, 1]);
      }
      return prow;
    }
    const sides = [['S.a', 'ضلع a (روبه‌روی A)'], ['S.b', 'ضلع b'], ['S.c', 'ضلع c']];
    const angs = [
      ['A.A', 'زاویه A', 'range', 0, 360, 1],
      ['A.B', 'زاویه B', 'range', 0, 360, 1],
      ['A.C', 'زاویه C', 'range', 0, 360, 1]
    ];
    if (t === 'sq') return commonV.concat([['S.s', 'طول ضلع']]);
    if (t === 'rect') return commonV.concat([['S.a', 'طول'], ['S.b', 'عرض']]);
    if (t === 'rtri') return commonV.concat(sides).concat([
      ['A.A', 'زاویه A', 'range', 0, 360, 1],
      ['A.B', 'زاویه B', 'range', 0, 360, 1]
    ]);
    if (t === 'trap' || t === 'itrap' || t === 'rtrap') return commonV.concat([['S.a', 'قاعده بالا'], ['S.b', 'قاعده پایین'], ['S.c', 'ساق چپ'], ['S.d', 'ساق راست'], ['X.h', 'ارتفاع']]);
    if (t === 'para' || t === 'rhomb') return commonV.concat(sides.slice(0, 2)).concat([['A.A', 'زاویه A', 'range', 0, 360, 1]]);
    return commonV.concat(sides).concat(angs);
  }

  function getPath(obj, path) {
    return path.split('.').reduce(function (o, k) { return o && o[k]; }, obj);
  }
  function setPath(obj, path, val) {
    const ks = path.split('.');
    let o = obj;
    for (let i = 0; i < ks.length - 1; i++) {
      if (!o[ks[i]] || typeof o[ks[i]] !== 'object') o[ks[i]] = {};
      o = o[ks[i]];
    }
    o[ks[ks.length - 1]] = val;
  }

  function defaultSpec(t) {
    return {
      t: t || 'tri',
      V: { A: 'A', B: 'B', C: 'C', D: 'D', O: 'O', d1: 'd₁', d2: 'd₂' },
      S: {},
      A: {},
      X: { m: '50', n: '2', tilt: '60', t0: '60', t1: '110', t2: '70', t3: '50', t4: '130', t5: '80' }
    };
  }

  let state = defaultSpec('tri');
  let replaceEl = null;
  function $(id) { return document.getElementById(id); }

  function paint() {
    const box = $('gfPreview');
    if (box) box.innerHTML = svgOf(state);
  }

  function bindField(inp) {
    function applyVal(val, src) {
      var key = inp.getAttribute('data-k');
      var min = inp.min, max = inp.max;
      if (inp.type === 'number' || inp.type === 'range') {
        var n = parseFloat(val);
        if (isFinite(n)) {
          if (min !== '' && n < parseFloat(min)) n = parseFloat(min);
          if (max !== '' && n > parseFloat(max)) n = parseFloat(max);
          val = String(Math.round(n * 1000) / 1000);
        }
      }
      setPath(state, key, clean(val));
      var wrap = inp.closest ? inp.closest('.gf-range') : inp.parentNode;
      if (wrap) {
        var range = wrap.querySelector('input[type=range]');
        var box = wrap.querySelector('input[type=number]');
        if (range && src !== range) range.value = val;
        if (box && src !== box) box.value = val;
      }
      paint();
      if (key === 'X.n') renderFields();
    }
    inp.addEventListener('input', function () { applyVal(inp.value, inp); });
    inp.addEventListener('change', function () { applyVal(inp.value, inp); });
  }

  function renderFields() {
    const host = $('gfFields');
    if (!host) return;
    const rows = fieldsFor(state.t);
    host.innerHTML = '<h4>برچسب‌ها و مقدارها</h4><div class="gf-grid">' + rows.map(function (r) {
      const val = getPath(state, r[0]);
      const shown = val == null ? '' : val;
      if (r[2] === 'range') {
        var cur = shown === '' ? String(r[3]) : shown;
        return '<label class="gf-span">' + esc(r[1]) +
          '<span class="gf-range">' +
          '<input type="range" min="' + r[3] + '" max="' + r[4] + '" step="' + r[5] +
          '" data-k="' + r[0] + '" value="' + esc(cur) + '">' +
          '<input type="number" class="gf-num" min="' + r[3] + '" max="' + r[4] + '" step="' + r[5] +
          '" data-k="' + r[0] + '" value="' + esc(cur) + '" inputmode="decimal">' +
          '</span></label>';
      }
      return '<label>' + esc(r[1]) + '<input data-k="' + r[0] + '" value="' + esc(shown) + '" placeholder="اختیاری"></label>';
    }).join('') + '</div>';
    host.querySelectorAll('input').forEach(bindField);
  }

  function setType(id) {
    const prev = state;
    state = defaultSpec(id);
    ['V', 'S', 'A', 'X'].forEach(function (k) {
      Object.keys(prev[k] || {}).forEach(function (kk) {
        if (prev[k][kk] != null && prev[k][kk] !== '') state[k][kk] = prev[k][kk];
      });
    });
    state.t = id;
    document.querySelectorAll('#gfShapes .gf-shape').forEach(function (b) {
      b.classList.toggle('on', b.getAttribute('data-t') === id);
    });
    renderFields();
    paint();
  }

  function tokenOf(spec) {
    return '%%FIG:' + JSON.stringify(spec) + '%%';
  }

  function targetTextarea() {
    var t = null;
    try { if (window.__qmfActiveField && document.body.contains(window.__qmfActiveField)) t = window.__qmfActiveField; } catch (e) {}
    try { if (!t && typeof activeMathTextarea !== 'undefined' && activeMathTextarea && document.body.contains(activeMathTextarea)) t = activeMathTextarea; } catch (e) {}
    try { if (!t && typeof extractedActiveTextarea !== 'undefined' && extractedActiveTextarea && document.body.contains(extractedActiveTextarea)) t = extractedActiveTextarea; } catch (e) {}
    return t || document.getElementById('qTxt_main') ||
      document.querySelector('.screen[data-view="builder"] textarea');
  }

  function insertToken(token) {
    const t = targetTextarea();
    if (!t) {
      try { notify('فیلد سؤال پیدا نشد', 'warning'); } catch (e) {}
      return;
    }
    try { if (window.QMF) QMF.upgrade(t); } catch (e) {}
    const start = t.selectionStart != null ? t.selectionStart : (t.value || '').length;
    const end = t.selectionEnd != null ? t.selectionEnd : start;
    const v = t.value || '';
    const left = v.slice(0, start);
    const needNl = left.length && !/[\n]$/.test(left);
    const next = (needNl ? '\n' : '') + token + '\n';
    t.value = left + next + v.slice(end);
    const caret = (left + next).length;
    try { t.setSelectionRange(caret, caret); } catch (e) {}
    t.dispatchEvent(new Event('input', { bubbles: true }));
    try { if (window.__examBuilderUpdateQuestionText) window.__examBuilderUpdateQuestionText(t); } catch(e) {}
    if (typeof qMathSync === 'function') qMathSync(t.id);
    try { if (t._qmfSurface) t._qmfSurface.focus(); } catch (e) {}
  }

  function apply() {
    if (replaceEl && replaceEl.classList && replaceEl.classList.contains('qmf-fig')) {
      replaceEl.dataset.fig = JSON.stringify(state);
      replaceEl.innerHTML = svgOf(state);
      const t = targetTextarea();
      try { if (t && window.QMF) QMF.syncFromSurface(t); } catch (e) {}
      try { if (t && window.__examBuilderUpdateQuestionText) window.__examBuilderUpdateQuestionText(t); } catch (e) {}
      try { if (typeof renderPreview === 'function') renderPreview(); } catch (e) {}
    } else {
      insertToken(tokenOf(state));
    }
    close();
  }

  function ensureModal() {
    if ($('gfOverlay')) return;
    const ov = document.createElement('div');
    ov.id = 'gfOverlay';
    ov.className = 'gf-overlay';
    ov.innerHTML =
      '<div class="gf-modal gf-3" role="dialog" aria-label="درج شکل">' +
        '<div class="gf-head"><h3>درج شکل هندسی</h3><button type="button" class="gf-x" id="gfClose">×</button></div>' +
        '<div class="gf-split an-split">' +
          '<aside class="gf-types"><div class="gf-types-h">نوع شکل</div><div class="gf-shapes" id="gfShapes"></div></aside>' +
          '<section class="an-mid"><div class="gf-preview" id="gfPreview"></div></section>' +
          '<aside class="an-side"><div class="an-side-h">تنظیمات شکل</div><div class="gf-fields" id="gfFields"></div></aside>' +
        '</div>' +
        '<div class="gf-foot"><button type="button" class="gf-btn ghost" id="gfCancel">انصراف</button>' +
        '<button type="button" class="gf-btn ok" id="gfApply">درج در سؤال</button></div>' +
      '</div>';
    document.body.appendChild(ov);
    const sh = $('gfShapes');
    SHAPES.forEach(function (s) {
      const b = document.createElement('button');
      b.type = 'button';
      b.className = 'gf-shape';
      b.setAttribute('data-t', s.id);
      b.innerHTML = iconOf(s.id) + '<span>' + s.name + '</span>';
      b.addEventListener('click', function () { setType(s.id); });
      sh.appendChild(b);
    });
    ov.addEventListener('click', function (e) { if (e.target === ov) close(); });
    $('gfClose').onclick = close;
    $('gfCancel').onclick = close;
    $('gfApply').onclick = apply;
  }

  function open(spec, el) {
    replaceEl = el || null;
    try {
      state = spec ? JSON.parse(JSON.stringify(spec)) : defaultSpec('tri');
    } catch (e) {
      state = defaultSpec('tri');
    }
    if (!state.t) state.t = 'tri';
    if (!state.V) state.V = {};
    if (!state.S) state.S = {};
    if (!state.A) state.A = {};
    if (!state.X) state.X = {};
    ensureModal();
    const ov = $('gfOverlay');
    ov.classList.add('open');
    var ok = $('gfApply');
    if (ok) ok.textContent = replaceEl ? 'اعمال تغییرات' : 'درج در سؤال';
    setType(state.t);
  }

  function openFromEl(fig) {
    if (!fig) return false;
    var spec = {};
    try { spec = JSON.parse(fig.getAttribute('data-fig') || fig.dataset.fig || '{}'); }
    catch (e) { spec = defaultSpec('tri'); }
    open(spec, fig);
    return true;
  }

  function close() {
    const ov = $('gfOverlay');
    if (ov) ov.classList.remove('open');
    replaceEl = null;
  }

  function makeFig(raw) {
    const el = document.createElement('span');
    el.className = 'qmf-fig';
    el.contentEditable = 'false';
    el.setAttribute('data-fig', raw);
    el.dataset.fig = raw;
    el.setAttribute('role', 'img');
    el.setAttribute('aria-label', 'شکل هندسی');
    el.title = 'برای ویرایش دوبار کلیک کنید';
    try { el.innerHTML = svgOf(JSON.parse(raw)); }
    catch (e) { el.textContent = '[شکل]'; }
    return el;
  }

  function bindToolbar() {
    const btn = $('openFigureEditor');
    if (btn && !btn._gfBound) {
      btn._gfBound = true;
      btn.addEventListener('click', function (e) {
        e.preventDefault();
        open(null, null);
      });
    }
    if (!document._gfDeleg) {
      document._gfDeleg = true;
      function pickFig(e) {
        var n = e.target;
        while (n && n !== document) {
          if (n.classList && n.classList.contains('qmf-fig')) return n;
          n = n.parentNode || n.parentElement;
        }
        return null;
      }
      document.addEventListener('dblclick', function (e) {
        var fig = pickFig(e);
        if (!fig) return;
        try {
          var sp = JSON.parse(fig.getAttribute('data-fig') || fig.dataset.fig || '{}');
          // اگر آیتم از نوع نمودار/جدول/آناتومی/تناوبی/علوم است، هندسه حق ندارد ویرایشگر شکل را باز کند.
          if (sp && sp.k) return;
        } catch (_e) {}
        e.preventDefault();
        e.stopPropagation();
        openFromEl(fig);
      }, true);
      document.addEventListener('click', function (e) {
        var fig = pickFig(e);
        document.querySelectorAll('.qmf-fig.is-on').forEach(function (n) {
          if (!fig || n !== fig) n.classList.remove('is-on');
        });
        if (fig) fig.classList.add('is-on');
      }, true);
    }
  }

  window.GeoFig = {
    svg: svgOf,
    make: makeFig,
    open: open,
    openFromEl: openFromEl,
    close: close,
    token: tokenOf
  };

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', bindToolbar);
  else bindToolbar();
})();
