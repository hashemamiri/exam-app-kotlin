(function () {
  var PHYS_CATS = [
    { id: 'all', name: 'همه' },
    { id: 'circ', name: 'مدار' },
    { id: 'force', name: 'نیرو' },
    { id: 'optic', name: 'نور' },
    { id: 'wave', name: 'موج' },
    { id: 'mag', name: 'مغناطیس' },
    { id: 'heat', name: 'گرما' },
    { id: 'nuc', name: 'هسته' }
  ];
  var CHEM_CATS = [
    { id: 'all', name: 'همه' },
    { id: 'lab', name: 'آزمایشگاه' },
    { id: 'mol', name: 'مولکول' },
    { id: 'atom', name: 'اتم' },
    { id: 'nrg', name: 'انرژی' },
    { id: 'org', name: 'آلی' }
  ];
  var PHYS_SET = { circ:1, force:1, optic:1, wave:1, mag:1, heat:1, nuc:1 };
  var CHEM_SET = { lab:1, mol:1, atom:1, nrg:1, org:1 };
  var domain = 'phys';
  function catsOf() { return domain === 'chem' ? CHEM_CATS : PHYS_CATS; }
  function typesOf() {
    var set = domain === 'chem' ? CHEM_SET : PHYS_SET;
    return TYPES.filter(function (s) { return set[s.cat]; });
  }
  function inferDomain(id) {
    var m = metaOf(id);
    if (CHEM_SET[m.cat] && !PHYS_SET[m.cat]) return 'chem';
    if (PHYS_SET[m.cat]) return 'phys';
    return domain;
  }
  var TYPES = [
    { id: 'cSer', cat: 'circ', name: 'مدار سری' },
    { id: 'cPar', cat: 'circ', name: 'مدار موازی' },
    { id: 'cSim', cat: 'circ', name: 'مدار ساده' },
    { id: 'cSym', cat: 'circ', name: 'نمادهای مدار' },
    { id: 'fbd', cat: 'force', name: 'جسم آزاد' },
    { id: 'inc', cat: 'force', name: 'سطح شیب‌دار' },
    { id: 'pul', cat: 'force', name: 'قرقره' },
    { id: 'lenC', cat: 'optic', name: 'عدسی همگرا' },
    { id: 'lenD', cat: 'optic', name: 'عدسی واگرا' },
    { id: 'mirP', cat: 'optic', name: 'آینه تخت' },
    { id: 'refr', cat: 'optic', name: 'شکست نور' },
    { id: 'wavT', cat: 'wave', name: 'موج عرضی' },
    { id: 'wavL', cat: 'wave', name: 'موج طولی' },
    { id: 'magB', cat: 'mag', name: 'آهنربا' },
    { id: 'coil', cat: 'mag', name: 'سیم‌لوله' },
    { id: 'beak', cat: 'lab', name: 'بشر' },
    { id: 'erl', cat: 'lab', name: 'ارلن' },
    { id: 'rbf', cat: 'lab', name: 'بالن' },
    { id: 'ttub', cat: 'lab', name: 'لوله آزمایش' },
    { id: 'buns', cat: 'lab', name: 'چراغ بونزن' },
    { id: 'bur', cat: 'lab', name: 'بورت' },
    { id: 'h2o', cat: 'mol', name: 'آب' },
    { id: 'co2', cat: 'mol', name: 'دی‌اکسید کربن' },
    { id: 'ch4', cat: 'mol', name: 'متان' },
    { id: 'nh3', cat: 'mol', name: 'آمونیاک' },
    { id: 'o2', cat: 'mol', name: 'اکسیژن' },
    { id: 'bohr', cat: 'atom', name: 'مدل بور' },
    { id: 'shell', cat: 'atom', name: 'لایه‌های الکترونی' },
    { id: 'cMix', cat: 'circ', name: 'مدار ترکیبی' },
    { id: 'lev', cat: 'force', name: 'اهرم' },
    { id: 'vec', cat: 'force', name: 'برآیند بردار' },
    { id: 'mirC', cat: 'optic', name: 'آینه کاو' },
    { id: 'mirV', cat: 'optic', name: 'آینه کوژ' },
    { id: 'wavS', cat: 'wave', name: 'موج ایستاده' },
    { id: 'comp', cat: 'mag', name: 'قطب‌نما' },
    { id: 'therm', cat: 'heat', name: 'دماسنج' },
    { id: 'expan', cat: 'heat', name: 'انبساط' },
    { id: 'pip', cat: 'lab', name: 'پیپت' },
    { id: 'dist', cat: 'lab', name: 'تقطیر' },
    { id: 'elec', cat: 'lab', name: 'الکترولیز' },
    { id: 'nacl', cat: 'mol', name: 'کلرید سدیم' },
    { id: 'ion', cat: 'atom', name: 'یون' },
    { id: 'ph', cat: 'atom', name: 'مقیاس pH' },
    { id: 'decay', cat: 'nuc', name: 'واپاشی هسته‌ای' },
    { id: 'safe', cat: 'lab', name: 'علائم ایمنی' },
    { id: 'spr', cat: 'force', name: 'فنر' },
    { id: 'pend', cat: 'force', name: 'آونگ' },
    { id: 'hydr', cat: 'force', name: 'پرس هیدرولیک' },
    { id: 'float', cat: 'force', name: 'شناوری' },
    { id: 'tir', cat: 'optic', name: 'فیبر نوری' },
    { id: 'prism', cat: 'optic', name: 'منشور' },
    { id: 'wireB', cat: 'mag', name: 'میدان سیم راست' },
    { id: 'trans', cat: 'circ', name: 'ترانسفورماتور' },
    { id: 'mot', cat: 'circ', name: 'موتور ساده' },
    { id: 'titr', cat: 'lab', name: 'تیتراسیون' },
    { id: 'filt', cat: 'lab', name: 'صاف کردن' },
    { id: 'sep', cat: 'lab', name: 'قیف جداکننده' },
    { id: 'volt', cat: 'lab', name: 'پیل گالوانی' },
    { id: 'exo', cat: 'nrg', name: 'واکنش گرمازا' },
    { id: 'endo', cat: 'nrg', name: 'واکنش گرماگیر' },
    { id: 'benz', cat: 'org', name: 'بنزن' },
    { id: 'alk', cat: 'org', name: 'آلکان' },
    { id: 'func', cat: 'org', name: 'گروه عاملی' },
    { id: 'echo', cat: 'wave', name: 'پژواک' },
    { id: 'calor', cat: 'heat', name: 'کالریمتر' },
    { id: 'led', cat: 'circ', name: 'دیود نورانی' },
    { id: 'dyno', cat: 'force', name: 'نیوتن‌سنج' },
    { id: 'baro', cat: 'heat', name: 'فشارسنج' },
    { id: 'litm', cat: 'lab', name: 'تورنسل' },
    { id: 'hcl', cat: 'mol', name: 'هیدروکلریک' }
  ];

  function esc(s) {
    return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) {
      return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c];
    });
  }
  function faNum(n) {
    return String(n).replace(/[0-9]/g, function (d) { return '۰۱۲۳۴۵۶۷۸۹'[+d]; });
  }
  function $(id) { return document.getElementById(id); }
  function metaOf(id) {
    for (var i = 0; i < TYPES.length; i++) if (TYPES[i].id === id) return TYPES[i];
    return TYPES[0];
  }
  function wrap(inner) {
    return '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 360 250" class="sc-svg" overflow="visible">' +
      '<style>text{font:700 12px Vazirmatn,Tahoma,sans-serif;fill:#1e293b}.sm{font:700 11px Vazirmatn,Tahoma,sans-serif;fill:#334155}.w{fill:#fff;stroke:#1e293b;stroke-width:1.6}</style>' +
      inner + '</svg>';
  }
  function T(x, y, s, c) {
    return '<text x="' + x + '" y="' + y + '" text-anchor="middle"' + (c ? ' class="' + c + '"' : '') + '>' + s + '</text>';
  }
  function arr(x1, y1, x2, y2, col) {
    col = col || '#0f172a';
    var id = 'ah' + Math.abs((x1 * 13 + y2 * 7) | 0);
    return '<defs><marker id="' + id + '" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto"><path d="M0,1 L10,5 L0,9 Z" fill="' + col + '"/></marker></defs>' +
      '<line x1="' + x1 + '" y1="' + y1 + '" x2="' + x2 + '" y2="' + y2 + '" stroke="' + col + '" stroke-width="1.8" marker-end="url(#' + id + ')"/>';
  }

  function svgOf(spec) {
    spec = spec || {};
    var t = spec.t || 'cSim';
    var X = spec.X || {};
    var lab = String(X.lab || '1') !== '0';
    var ttl = X.title ? T(180, 16, esc(X.title), '') : '';

    function bat(x, y) {
      return '<line x1="' + x + '" y1="' + (y - 14) + '" x2="' + x + '" y2="' + (y + 14) + '" stroke="#1e293b" stroke-width="3"/>' +
        '<line x1="' + (x + 8) + '" y1="' + (y - 8) + '" x2="' + (x + 8) + '" y2="' + (y + 8) + '" stroke="#1e293b" stroke-width="2"/>';
    }
    function res(x, y) {
      return '<path d="M' + x + ' ' + y + ' l6-7 8 14 8-14 8 14 8-14 6 7" fill="none" stroke="#1e293b" stroke-width="1.8"/>';
    }
    function lamp(x, y) {
      return '<circle cx="' + x + '" cy="' + y + '" r="12" fill="#fef9c3" stroke="#1e293b" stroke-width="1.6"/>' +
        '<path d="M' + (x - 7) + ' ' + (y - 7) + ' L' + (x + 7) + ' ' + (y + 7) + ' M' + (x + 7) + ' ' + (y - 7) + ' L' + (x - 7) + ' ' + (y + 7) + '" stroke="#1e293b" stroke-width="1.4"/>';
    }
    function sw(x, y, open) {
      return '<circle cx="' + x + '" cy="' + y + '" r="3" fill="#1e293b"/>' +
        '<circle cx="' + (x + 22) + '" cy="' + y + '" r="3" fill="#1e293b"/>' +
        '<line x1="' + x + '" y1="' + y + '" x2="' + (x + (open ? 16 : 22)) + '" y2="' + (open ? (y - 10) : y) + '" stroke="#1e293b" stroke-width="1.8"/>';
    }

    if (t === 'cSim') {
      var h = ttl +
        '<path d="M50 80 H140" stroke="#1e293b" stroke-width="1.8"/>' + bat(148, 80) +
        '<path d="M164 80 H230" stroke="#1e293b" stroke-width="1.8"/>' + sw(238, 80, 1) +
        '<path d="M268 80 H310 V160 H230" stroke="#1e293b" stroke-width="1.8"/>' + lamp(210, 160) +
        '<path d="M198 160 H50 V80" stroke="#1e293b" stroke-width="1.8"/>';
      if (lab) h += T(148, 58, 'باتری') + T(250, 58, 'کلید') + T(210, 190, 'لامپ');
      return wrap(h);
    }
    if (t === 'cSer') {
      var h = ttl +
        '<path d="M40 90 H90" stroke="#1e293b" stroke-width="1.8"/>' + bat(98, 90) +
        '<path d="M114 90 H150" stroke="#1e293b" stroke-width="1.8"/>' + res(150, 90) +
        '<path d="M194 90 H230" stroke="#1e293b" stroke-width="1.8"/>' + res(230, 90) +
        '<path d="M274 90 H320 V170 H40 V90" stroke="#1e293b" stroke-width="1.8"/>';
      if (lab) h += T(98, 68, 'باتری') + T(172, 68, 'R₁') + T(252, 68, 'R₂');
      return wrap(h);
    }
    if (t === 'cPar') {
      var h = ttl +
        '<path d="M50 70 H120 V50 H200" stroke="#1e293b" stroke-width="1.8"/>' + res(200, 50) +
        '<path d="M244 50 H300 V190 H50 V70" stroke="#1e293b" stroke-width="1.8"/>' +
        '<path d="M120 70 V160 H200" stroke="#1e293b" stroke-width="1.8"/>' + res(200, 160) +
        '<path d="M244 160 H300" stroke="#1e293b" stroke-width="1.8"/>' +
        bat(70, 120);
      if (lab) h += T(70, 98, 'باتری') + T(222, 38, 'R₁') + T(222, 148, 'R₂');
      return wrap(h);
    }
    if (t === 'cSym') {
      var h = ttl +
        bat(50, 70) + T(54, 100, 'باتری') +
        res(120, 70) + T(150, 100, 'مقاومت') +
        lamp(230, 70) + T(230, 100, 'لامپ') +
        sw(280, 70, 1) + T(292, 100, 'کلید') +
        '<circle cx="70" cy="170" r="16" class="w"/><text x="70" y="175" text-anchor="middle">A</text>' + T(70, 202, 'آمپرمتر') +
        '<circle cx="160" cy="170" r="16" class="w"/><text x="160" y="175" text-anchor="middle">V</text>' + T(160, 202, 'ولتمتر');
      return wrap(h);
    }
    if (t === 'fbd') {
      var h = ttl +
        '<rect x="140" y="100" width="80" height="50" rx="4" fill="#dbeafe" stroke="#1e3a5f" stroke-width="1.6"/>' +
        '<line x1="60" y1="150" x2="300" y2="150" stroke="#64748b" stroke-width="2"/>' +
        arr(180, 100, 180, 52, '#2563eb') +
        arr(180, 150, 180, 210, '#dc2626') +
        arr(140, 125, 80, 125, '#16a34a') +
        arr(220, 125, 280, 125, '#ca8a04');
      if (lab) h += T(198, 48, 'N') + T(198, 228, 'W') + T(70, 118, 'f') + T(292, 118, 'F');
      return wrap(h);
    }
    if (t === 'inc') {
      var h = ttl +
        '<path d="M50 200 H310 L50 80 Z" fill="#f1f5f9" stroke="#64748b" stroke-width="1.8"/>' +
        '<rect x="118" y="112" width="70" height="36" rx="3" fill="#dbeafe" stroke="#1e3a5f" transform="rotate(-22 153 130)"/>' +
        arr(160, 118, 160, 70, '#2563eb') +
        arr(160, 148, 160, 200, '#dc2626') +
        arr(130, 140, 90, 158, '#16a34a');
      if (lab) h += T(178, 66, 'N') + T(178, 216, 'W') + T(78, 152, 'f');
      return wrap(h);
    }
    if (t === 'pul') {
      var h = ttl +
        '<circle cx="180" cy="50" r="18" class="w"/>' +
        '<path d="M162 50 V140 H130 V180 H210 V140 H198 V50" fill="none" stroke="#1e293b" stroke-width="1.8"/>' +
        '<rect x="118" y="180" width="28" height="28" fill="#93c5fd" stroke="#1e3a5f"/>' +
        '<rect x="206" y="180" width="28" height="36" fill="#fca5a5" stroke="#7f1d1d"/>' +
        arr(132, 180, 132, 150, '#2563eb') +
        arr(220, 216, 220, 246, '#dc2626');
      if (lab) h += T(180, 36, 'قرقره') + T(132, 140, 'T') + T(236, 244, 'W');
      return wrap(h);
    }
    if (t === 'lenC') {
      var h = ttl +
        '<path d="M176 40 Q210 125 176 210 M184 40 Q150 125 184 210" fill="#e0f2fe" stroke="#0369a1" stroke-width="1.8"/>' +
        '<line x1="40" y1="125" x2="320" y2="125" stroke="#94a3b8" stroke-dasharray="4 3"/>' +
        '<path d="M50 80 H176 L250 125 M50 170 H176 L250 125" fill="none" stroke="#dc2626" stroke-width="1.6"/>';
      if (lab) h += T(180, 30, 'عدسی همگرا') + T(250, 118, 'کانون');
      return wrap(h);
    }
    if (t === 'lenD') {
      var h = ttl +
        '<path d="M168 40 Q148 125 168 210 M192 40 Q212 125 192 210" fill="#e0f2fe" stroke="#0369a1" stroke-width="1.8"/>' +
        '<line x1="40" y1="125" x2="320" y2="125" stroke="#94a3b8" stroke-dasharray="4 3"/>' +
        '<path d="M50 80 H170 L230 55 M50 170 H170 L230 195" fill="none" stroke="#dc2626" stroke-width="1.6"/>';
      if (lab) h += T(180, 30, 'عدسی واگرا');
      return wrap(h);
    }
    if (t === 'mirP') {
      var h = ttl +
        '<line x1="250" y1="40" x2="250" y2="210" stroke="#0f766e" stroke-width="4"/>' +
        '<path d="M246 40 l8 0 M246 55 l8 0 M246 70 l8 0 M246 85 l8 0 M246 100 l8 0 M246 115 l8 0 M246 130 l8 0 M246 145 l8 0 M246 160 l8 0 M246 175 l8 0 M246 190 l8 0 M246 205 l8 0" stroke="#0f766e" stroke-width="1.4"/>' +
        '<path d="M70 70 L250 125 L70 180" fill="none" stroke="#dc2626" stroke-width="1.6"/>';
      if (lab) h += T(250, 28, 'آینه') + T(120, 64, 'تابش') + T(120, 198, 'بازتاب');
      return wrap(h);
    }
    if (t === 'refr') {
      var h = ttl +
        '<rect x="40" y="125" width="280" height="90" fill="#e0f2fe" opacity=".8"/>' +
        '<line x1="40" y1="125" x2="320" y2="125" stroke="#0369a1" stroke-width="2"/>' +
        '<line x1="180" y1="40" x2="180" y2="220" stroke="#94a3b8" stroke-dasharray="4 3"/>' +
        '<path d="M80 50 L180 125 L230 210" fill="none" stroke="#dc2626" stroke-width="1.8"/>';
      if (lab) h += T(100, 46, 'هوا') + T(80, 200, 'شیشه') + T(210, 80, 'عمود');
      return wrap(h);
    }
    if (t === 'wavT') {
      var h = ttl +
        '<path d="M30 125 C 60 50, 90 50, 120 125 S 180 200, 210 125 S 270 50, 300 125 S 340 200, 350 160" fill="none" stroke="#2563eb" stroke-width="2.2"/>' +
        '<line x1="30" y1="125" x2="340" y2="125" stroke="#94a3b8" stroke-dasharray="4 3"/>' +
        arr(120, 125, 120, 58, '#dc2626') + arr(210, 125, 300, 125, '#16a34a');
      if (lab) h += T(140, 52, 'دامنه') + T(250, 112, 'طول موج');
      return wrap(h);
    }
    if (t === 'wavL') {
      var h = ttl;
      for (var i = 0; i < 14; i++) {
        var x = 40 + i * 20;
        var dense = (i % 7 < 3);
        h += '<line x1="' + x + '" y1="70" x2="' + x + '" y2="180" stroke="#1e293b" stroke-width="' + (dense ? 2.6 : 1.2) + '"/>';
      }
      if (lab) h += T(80, 200, 'تراکم') + T(180, 200, 'انبساط');
      return wrap(h);
    }
    if (t === 'magB') {
      var h = ttl +
        '<rect x="120" y="100" width="55" height="40" fill="#ef4444" stroke="#7f1d1d"/>' +
        '<rect x="175" y="100" width="55" height="40" fill="#3b82f6" stroke="#1e3a8a"/>' +
        T(147, 125, 'N') + T(203, 125, 'S');
      for (var i = 0; i < 5; i++) {
        var y = 70 + i * 22;
        h += '<path d="M120 ' + (110 + (i - 2) * 8) + ' C 80 ' + y + ', 80 ' + (250 - y) + ', 120 ' + (130 + (2 - i) * 8) + '" fill="none" stroke="#64748b"/>';
        h += '<path d="M230 ' + (110 + (i - 2) * 8) + ' C 270 ' + y + ', 270 ' + (250 - y) + ', 230 ' + (130 + (2 - i) * 8) + '" fill="none" stroke="#64748b"/>';
      }
      if (lab) h += T(180, 90, 'آهنربای میله‌ای');
      return wrap(h);
    }
    if (t === 'coil') {
      var h = ttl + '<path d="M80 80 H280" stroke="#1e293b" stroke-width="1.6"/>';
      for (var i = 0; i < 8; i++) {
        h += '<ellipse cx="' + (110 + i * 20) + '" cy="125" rx="10" ry="36" fill="none" stroke="#2563eb" stroke-width="1.8"/>';
      }
      h += arr(70, 125, 50, 125, '#dc2626') + arr(290, 125, 310, 125, '#dc2626');
      if (lab) h += T(180, 210, 'سیم‌لوله') + T(50, 112, 'I');
      return wrap(h);
    }
    if (t === 'beak') {
      var h = ttl +
        '<path d="M120 60 H240 L228 200 H132 Z" fill="#e0f2fe" stroke="#0369a1" stroke-width="2"/>' +
        '<path d="M128 140 H232" fill="none" stroke="#38bdf8" stroke-width="10" opacity=".45"/>' +
        '<path d="M118 60 H248" stroke="#0369a1" stroke-width="2.4"/>';
      if (lab) h += T(180, 226, 'بشر');
      return wrap(h);
    }
    if (t === 'erl') {
      var h = ttl +
        '<path d="M160 50 H200 V90 L250 200 H110 L160 90 Z" fill="#e0f2fe" stroke="#0369a1" stroke-width="2"/>' +
        '<path d="M128 160 H232" stroke="#38bdf8" stroke-width="12" opacity=".4"/>';
      if (lab) h += T(180, 226, 'ارلن');
      return wrap(h);
    }
    if (t === 'rbf') {
      var h = ttl +
        '<rect x="164" y="40" width="32" height="50" fill="#e0f2fe" stroke="#0369a1" stroke-width="1.8"/>' +
        '<circle cx="180" cy="150" r="58" fill="#e0f2fe" stroke="#0369a1" stroke-width="2"/>' +
        '<path d="M140 150 H220" stroke="#38bdf8" stroke-width="14" opacity=".35"/>';
      if (lab) h += T(180, 230, 'بالن ته گرد');
      return wrap(h);
    }
    if (t === 'ttub') {
      var h = ttl +
        '<path d="M155 40 H205 V180 Q180 210 155 180 Z" fill="#e0f2fe" stroke="#0369a1" stroke-width="2"/>' +
        '<path d="M160 130 H200" stroke="#38bdf8" stroke-width="10" opacity=".4"/>';
      if (lab) h += T(180, 230, 'لوله آزمایش');
      return wrap(h);
    }
    if (t === 'buns') {
      var h = ttl +
        '<rect x="150" y="150" width="60" height="50" rx="4" fill="#64748b"/>' +
        '<rect x="168" y="100" width="24" height="50" fill="#94a3b8"/>' +
        '<path d="M180 100 C170 70 190 40 180 28 C190 50 170 70 180 100" fill="#f97316" stroke="#c2410c"/>' +
        '<path d="M180 90 C176 60 184 40 180 32" fill="#fde68a"/>';
      if (lab) h += T(180, 220, 'چراغ بونزن') + T(210, 50, 'شعله');
      return wrap(h);
    }
    if (t === 'bur') {
      var h = ttl +
        '<rect x="170" y="30" width="20" height="180" rx="3" fill="#e0f2fe" stroke="#0369a1" stroke-width="1.8"/>' +
        '<rect x="166" y="200" width="28" height="10" fill="#64748b"/>' +
        '<path d="M174 80 H186" stroke="#38bdf8" stroke-width="8" opacity=".4"/>';
      if (lab) for (var i = 0; i < 6; i++) h += '<text class="sm" x="198" y="' + (50 + i * 28) + '">' + faNum(i * 10) + '</text>';
      if (lab) h += T(180, 230, 'بورت');
      return wrap(h);
    }
    function ball(x, y, r, fill, labt) {
      return '<circle cx="' + x + '" cy="' + y + '" r="' + r + '" fill="' + fill + '" stroke="#0f172a" stroke-width="1.4"/>' +
        (labt ? T(x, y + 5, labt) : '');
    }
    function stick(x1, y1, x2, y2) {
      return '<line x1="' + x1 + '" y1="' + y1 + '" x2="' + x2 + '" y2="' + y2 + '" stroke="#334155" stroke-width="6" stroke-linecap="round"/>';
    }
    if (t === 'h2o') {
      var h = ttl + stick(180, 120, 120, 180) + stick(180, 120, 240, 180) +
        ball(180, 118, 28, '#ef4444', 'O') + ball(118, 184, 18, '#e2e8f0', 'H') + ball(242, 184, 18, '#e2e8f0', 'H');
      if (lab) h += T(180, 230, 'H₂O');
      return wrap(h);
    }
    if (t === 'co2') {
      var h = ttl + stick(180, 130, 90, 130) + stick(180, 130, 270, 130) +
        ball(180, 130, 26, '#334155', 'C') + ball(86, 130, 22, '#ef4444', 'O') + ball(274, 130, 22, '#ef4444', 'O');
      if (lab) h += T(180, 200, 'CO₂');
      return wrap(h);
    }
    if (t === 'ch4') {
      var h = ttl +
        stick(180, 130, 180, 60) + stick(180, 130, 110, 180) + stick(180, 130, 250, 180) + stick(180, 130, 180, 200) +
        ball(180, 128, 26, '#334155', 'C') +
        ball(180, 56, 16, '#e2e8f0', 'H') + ball(108, 184, 16, '#e2e8f0', 'H') +
        ball(252, 184, 16, '#e2e8f0', 'H') + ball(180, 206, 16, '#e2e8f0', 'H');
      if (lab) h += T(180, 236, 'CH₄');
      return wrap(h);
    }
    if (t === 'nh3') {
      var h = ttl +
        stick(180, 120, 120, 180) + stick(180, 120, 240, 180) + stick(180, 120, 180, 60) +
        ball(180, 120, 26, '#3b82f6', 'N') +
        ball(118, 184, 16, '#e2e8f0', 'H') + ball(242, 184, 16, '#e2e8f0', 'H') + ball(180, 56, 16, '#e2e8f0', 'H');
      if (lab) h += T(180, 230, 'NH₃');
      return wrap(h);
    }
    if (t === 'o2') {
      var h = ttl + stick(140, 130, 220, 130) +
        ball(132, 130, 26, '#ef4444', 'O') + ball(228, 130, 26, '#ef4444', 'O');
      if (lab) h += T(180, 190, 'O₂');
      return wrap(h);
    }
    if (t === 'bohr') {
      var h = ttl +
        '<circle cx="180" cy="130" r="10" fill="#f59e0b"/>' +
        '<ellipse cx="180" cy="130" rx="40" ry="22" fill="none" stroke="#64748b"/>' +
        '<ellipse cx="180" cy="130" rx="70" ry="38" fill="none" stroke="#64748b"/>' +
        '<ellipse cx="180" cy="130" rx="100" ry="54" fill="none" stroke="#64748b"/>' +
        '<circle cx="220" cy="130" r="5" fill="#2563eb"/>' +
        '<circle cx="250" cy="130" r="5" fill="#2563eb"/>' +
        '<circle cx="280" cy="130" r="5" fill="#2563eb"/>';
      if (lab) h += T(180, 118, 'هسته') + T(300, 118, 'e⁻');
      return wrap(h);
    }
    if (t === 'shell') {
      var h = ttl +
        '<circle cx="180" cy="130" r="16" fill="#fde68a" stroke="#b45309"/>' + T(180, 134, 'K') +
        '<circle cx="180" cy="130" r="48" fill="none" stroke="#2563eb" stroke-width="2"/>' +
        '<circle cx="180" cy="130" r="80" fill="none" stroke="#16a34a" stroke-width="2"/>' +
        '<circle cx="180" cy="130" r="110" fill="none" stroke="#dc2626" stroke-width="2"/>';
      if (lab) h += T(236, 130, 'L') + T(268, 130, 'M') + T(300, 130, 'N');
      return wrap(h);
    }
    if (t === 'cMix') {
      var h = ttl +
        '<path d="M40 80 H90" stroke="#1e293b" stroke-width="1.8"/>' + bat(98, 80) +
        '<path d="M114 80 H160 V50 H220" stroke="#1e293b" stroke-width="1.8"/>' + res(220, 50) +
        '<path d="M264 50 H310 V190 H40 V80" stroke="#1e293b" stroke-width="1.8"/>' +
        '<path d="M160 80 V160 H220" stroke="#1e293b" stroke-width="1.8"/>' + res(220, 160) +
        '<path d="M264 160 H310" stroke="#1e293b" stroke-width="1.8"/>';
      if (lab) h += T(98, 60, 'باتری') + T(242, 38, 'R₁') + T(242, 148, 'R₂');
      return wrap(h);
    }
    if (t === 'lev') {
      var h = ttl +
        '<polygon points="180,170 150,210 210,210" fill="#94a3b8"/>' +
        '<line x1="60" y1="168" x2="300" y2="168" stroke="#1e293b" stroke-width="6" stroke-linecap="round"/>' +
        '<rect x="80" y="128" width="36" height="36" fill="#93c5fd" stroke="#1e3a5f"/>' +
        '<rect x="240" y="138" width="28" height="26" fill="#fca5a5" stroke="#7f1d1d"/>' +
        arr(98, 128, 98, 100, '#2563eb') + arr(254, 164, 254, 196, '#dc2626');
      if (lab) h += T(180, 228, 'تکیهگاه') + T(98, 92, 'F') + T(270, 198, 'W');
      return wrap(h);
    }
    if (t === 'vec') {
      var h = ttl +
        arr(80, 180, 200, 80, '#2563eb') +
        arr(200, 80, 280, 180, '#16a34a') +
        arr(80, 180, 280, 180, '#dc2626');
      if (lab) h += T(130, 110, 'a⃗') + T(260, 120, 'b⃗') + T(180, 200, 'a⃗+b⃗');
      return wrap(h);
    }
    if (t === 'mirC') {
      var h = ttl +
        '<path d="M240 40 Q170 125 240 210" fill="none" stroke="#0f766e" stroke-width="4"/>' +
        '<line x1="40" y1="125" x2="300" y2="125" stroke="#94a3b8" stroke-dasharray="4 3"/>' +
        '<path d="M50 70 H200 Q210 125 200 180 H50" fill="none" stroke="#dc2626" stroke-width="1.6"/>';
      if (lab) h += T(230, 30, 'آینه کاو') + T(210, 118, 'C');
      return wrap(h);
    }
    if (t === 'mirV') {
      var h = ttl +
        '<path d="M210 40 Q270 125 210 210" fill="none" stroke="#0f766e" stroke-width="4"/>' +
        '<line x1="40" y1="125" x2="300" y2="125" stroke="#94a3b8" stroke-dasharray="4 3"/>' +
        '<path d="M50 70 H200 L250 50 M50 180 H200 L250 200" fill="none" stroke="#dc2626" stroke-width="1.6"/>';
      if (lab) h += T(230, 30, 'آینه کوژ');
      return wrap(h);
    }
    if (t === 'wavS') {
      var h = ttl +
        '<line x1="40" y1="125" x2="320" y2="125" stroke="#94a3b8"/>' +
        '<path d="M40 125 C 70 50, 110 50, 140 125 S 210 200, 240 125 S 310 50, 320 125" fill="none" stroke="#2563eb" stroke-width="2"/>' +
        '<circle cx="40" cy="125" r="4" fill="#1e293b"/><circle cx="140" cy="125" r="4" fill="#1e293b"/>' +
        '<circle cx="240" cy="125" r="4" fill="#1e293b"/><circle cx="320" cy="125" r="4" fill="#1e293b"/>';
      if (lab) h += T(140, 210, 'گره') + T(190, 55, 'شکم');
      return wrap(h);
    }
    if (t === 'comp') {
      var h = ttl +
        '<circle cx="180" cy="130" r="70" fill="#fff" stroke="#1e293b" stroke-width="2"/>' +
        '<polygon points="180,70 168,130 180,122 192,130" fill="#ef4444"/>' +
        '<polygon points="180,190 168,130 180,138 192,130" fill="#3b82f6"/>';
      if (lab) h += T(180, 62, 'N') + T(180, 210, 'S') + T(180, 236, 'قطب‌نما');
      return wrap(h);
    }
    if (t === 'therm') {
      var h = ttl +
        '<rect x="168" y="36" width="24" height="150" rx="12" fill="#e2e8f0" stroke="#334155"/>' +
        '<circle cx="180" cy="200" r="22" fill="#ef4444" stroke="#7f1d1d"/>' +
        '<rect x="174" y="80" width="12" height="120" rx="6" fill="#ef4444"/>';
      if (lab) {
        h += T(210, 50, '°C');
        for (var i = 0; i < 5; i++) h += '<line x1="192" y1="' + (50 + i * 28) + '" x2="200" y2="' + (50 + i * 28) + '" stroke="#334155"/>' +
          '<text class="sm" x="214" y="' + (54 + i * 28) + '">' + faNum(100 - i * 25) + '</text>';
      }
      return wrap(h);
    }
    if (t === 'expan') {
      var h = ttl +
        '<rect x="50" y="140" width="110" height="28" fill="#93c5fd" stroke="#1e3a8a"/>' +
        '<rect x="200" y="132" width="130" height="36" fill="#fca5a5" stroke="#7f1d1d"/>' +
        arr(160, 154, 198, 154, '#dc2626');
      if (lab) h += T(105, 126, 'سرد') + T(265, 118, 'گرم') + T(180, 200, 'انبساط طولی');
      return wrap(h);
    }
    if (t === 'pip') {
      var h = ttl +
        '<path d="M176 30 H184 V170 L180 210 L176 170 Z" fill="#e0f2fe" stroke="#0369a1" stroke-width="1.8"/>' +
        '<rect x="170" y="24" width="20" height="14" rx="3" fill="#64748b"/>' +
        '<path d="M177 90 H183" stroke="#38bdf8" stroke-width="6" opacity=".5"/>';
      if (lab) h += T(180, 230, 'پیپت');
      return wrap(h);
    }
    if (t === 'dist') {
      var h = ttl +
        '<rect x="50" y="150" width="70" height="40" rx="4" fill="#64748b"/>' +
        '<circle cx="85" cy="110" r="36" fill="#e0f2fe" stroke="#0369a1" stroke-width="2"/>' +
        '<path d="M115 90 H200 V70 H260 V160" fill="none" stroke="#0369a1" stroke-width="2"/>' +
        '<rect x="200" y="70" width="60" height="20" fill="#bae6fd" stroke="#0369a1"/>' +
        '<path d="M248 160 H248  " />' +
        '<path d="M230 90 Q230 160 270 190" fill="none" stroke="#0369a1" stroke-width="2"/>' +
        '<rect x="250" y="190" width="40" height="30" fill="#e0f2fe" stroke="#0369a1"/>';
      if (lab) h += T(85, 200, 'بالن') + T(230, 62, 'سردکننده') + T(270, 234, 'جمع‌آوری');
      return wrap(h);
    }
    if (t === 'elec') {
      var h = ttl +
        '<rect x="80" y="80" width="200" height="110" rx="8" fill="#e0f2fe" stroke="#0369a1" stroke-width="2"/>' +
        '<rect x="110" y="70" width="14" height="90" fill="#94a3b8"/>' +
        '<rect x="236" y="70" width="14" height="90" fill="#fbbf24"/>' +
        '<path d="M117 70 V40 H243 V70" fill="none" stroke="#1e293b" stroke-width="1.8"/>' +
        bat(175, 40);
      if (lab) h += T(117, 175, 'کاتد') + T(243, 175, 'آند') + T(180, 210, 'الکترولیز');
      return wrap(h);
    }
    if (t === 'nacl') {
      var h = ttl;
      var pts = [[120,90],[180,90],[240,90],[120,150],[180,150],[240,150],[120,210],[180,210],[240,210]];
      for (var i = 0; i < pts.length; i++) {
        var col = i % 2 ? '#3b82f6' : '#ef4444';
        var lb = i % 2 ? 'Na⁺' : 'Cl⁻';
        h += '<circle cx="' + pts[i][0] + '" cy="' + pts[i][1] + '" r="20" fill="' + col + '" stroke="#0f172a"/>' +
          T(pts[i][0], pts[i][1] + 4, lb);
      }
      if (lab) h += T(180, 36, 'شبکه یونی NaCl');
      return wrap(h);
    }
    if (t === 'ion') {
      var h = ttl +
        '<circle cx="110" cy="130" r="40" fill="#dbeafe" stroke="#1d4ed8" stroke-width="2"/>' +
        T(110, 126, 'Na') + T(110, 146, '⁺') +
        '<circle cx="250" cy="130" r="46" fill="#fee2e2" stroke="#b91c1c" stroke-width="2"/>' +
        T(250, 126, 'Cl') + T(250, 146, '⁻');
      if (lab) h += T(110, 190, 'کاتیون') + T(250, 196, 'آنیون');
      return wrap(h);
    }
    if (t === 'ph') {
      var h = ttl;
      var cols = ['#ef4444','#f97316','#f59e0b','#eab308','#84cc16','#22c55e','#14b8a6','#06b6d4','#3b82f6','#6366f1','#8b5cf6','#a855f7','#d946ef','#ec4899'];
      for (var i = 0; i < 14; i++) {
        var x = 30 + i * 22;
        h += '<rect x="' + x + '" y="90" width="20" height="70" rx="3" fill="' + cols[i] + '"/>' +
          '<text class="sm" x="' + (x + 10) + '" y="178">' + faNum(i) + '</text>';
      }
      if (lab) h += T(70, 80, 'اسیدی') + T(180, 80, 'خنثی') + T(290, 80, 'بازی') + T(180, 210, 'مقیاس pH');
      return wrap(h);
    }
    if (t === 'decay') {
      var h = ttl +
        '<circle cx="90" cy="130" r="34" fill="#fde68a" stroke="#b45309" stroke-width="2"/>' + T(90, 134, 'هسته') +
        arr(128, 118, 200, 80, '#dc2626') + T(230, 78, 'α') +
        arr(128, 130, 220, 130, '#2563eb') + T(240, 134, 'β') +
        arr(128, 142, 200, 186, '#16a34a') + T(230, 194, 'γ');
      if (lab) h += T(180, 230, 'واپاشی α ، β ، γ');
      return wrap(h);
    }
    if (t === 'safe') {
      var h = ttl +
        '<polygon points="70,70 100,120 40,120" fill="#facc15" stroke="#854d0e" stroke-width="2"/>' + T(70, 112, '!') + T(70, 142, 'خطر') +
        '<circle cx="180" cy="96" r="28" fill="#ef4444" stroke="#7f1d1d" stroke-width="2"/>' + T(180, 100, '🔥') + T(180, 142, 'آتش') +
        '<rect x="246" y="68" width="56" height="56" rx="8" fill="#22c55e" stroke="#14532d" stroke-width="2"/>' + T(274, 100, '☣') + T(274, 142, 'زیستی');
      if (lab) h += T(180, 190, 'علائم ایمنی آزمایشگاه');
      return wrap(h);
    }
    if (t === 'spr') {
      var h = ttl + '<path d="M80 130 H120 l8-16 12 32 12-32 12 32 12-32 12 32 8-16 H280" fill="none" stroke="#1e293b" stroke-width="2"/>' +
        '<rect x="60" y="118" width="22" height="24" fill="#94a3b8"/>' +
        '<rect x="278" y="110" width="28" height="40" fill="#93c5fd" stroke="#1e3a8a"/>' +
        arr(292, 110, 292, 78, '#2563eb');
      if (lab) h += T(180, 90, 'فنر') + T(308, 72, 'F') + T(180, 180, 'F = kx');
      return wrap(h);
    }
    if (t === 'pend') {
      var h = ttl +
        '<line x1="60" y1="40" x2="300" y2="40" stroke="#64748b" stroke-width="3"/>' +
        '<line x1="180" y1="40" x2="180" y2="170" stroke="#94a3b8" stroke-dasharray="4 3"/>' +
        '<line x1="180" y1="40" x2="250" y2="160" stroke="#1e293b" stroke-width="1.8"/>' +
        '<circle cx="250" cy="168" r="14" fill="#3b82f6" stroke="#1e3a8a"/>';
      if (lab) h += T(210, 90, 'θ') + T(250, 200, 'جرم') + T(180, 230, 'آونگ ساده');
      return wrap(h);
    }
    if (t === 'hydr') {
      var h = ttl +
        '<path d="M70 200 H160 V120 H70 Z" fill="#7dd3fc" stroke="#0369a1"/>' +
        '<path d="M200 200 H300 V80 H200 Z" fill="#7dd3fc" stroke="#0369a1"/>' +
        '<path d="M160 190 H200" stroke="#0369a1" stroke-width="8"/>' +
        '<rect x="88" y="88" width="54" height="32" fill="#94a3b8"/>' +
        '<rect x="218" y="48" width="64" height="32" fill="#94a3b8"/>' +
        arr(115, 88, 115, 58, '#dc2626') + arr(250, 48, 250, 22, '#16a34a');
      if (lab) h += T(115, 50, 'f') + T(268, 18, 'F') + T(180, 230, 'پرس هیدرولیک');
      return wrap(h);
    }
    if (t === 'float') {
      var h = ttl +
        '<rect x="50" y="130" width="260" height="80" fill="#bae6fd" stroke="#0284c7"/>' +
        '<rect x="140" y="100" width="80" height="50" fill="#fbbf24" stroke="#b45309"/>' +
        arr(180, 100, 180, 68, '#2563eb') + arr(180, 150, 180, 190, '#dc2626');
      if (lab) h += T(200, 62, 'شناوری') + T(210, 204, 'وزن') + T(80, 160, 'مایع');
      return wrap(h);
    }
    if (t === 'tir') {
      var h = ttl +
        '<path d="M50 80 H310 L290 160 H70 Z" fill="#e0f2fe" stroke="#0369a1" stroke-width="2"/>' +
        '<path d="M70 120 L120 90 L170 130 L220 95 L270 125" fill="none" stroke="#dc2626" stroke-width="1.8"/>';
      if (lab) h += T(180, 70, 'هسته') + T(180, 180, 'غلاف') + T(180, 210, 'بازتاب کلی / فیبر نوری');
      return wrap(h);
    }
    if (t === 'prism') {
      var h = ttl +
        '<polygon points="180,50 80,190 280,190" fill="#e0f2fe" stroke="#0369a1" stroke-width="2" opacity=".9"/>' +
        '<path d="M40 110 L130 130" stroke="#1e293b" stroke-width="2"/>' +
        '<path d="M200 140 L320 90" stroke="#ef4444" stroke-width="1.6"/>' +
        '<path d="M200 145 L320 120" stroke="#eab308" stroke-width="1.6"/>' +
        '<path d="M200 150 L320 150" stroke="#22c55e" stroke-width="1.6"/>' +
        '<path d="M200 155 L320 180" stroke="#3b82f6" stroke-width="1.6"/>';
      if (lab) h += T(80, 100, 'سفید') + T(300, 78, 'طیف') + T(180, 220, 'منشور');
      return wrap(h);
    }
    if (t === 'wireB') {
      var h = ttl +
        '<circle cx="180" cy="130" r="8" fill="#1e293b"/>' +
        '<circle cx="180" cy="130" r="28" fill="none" stroke="#64748b"/>' +
        '<circle cx="180" cy="130" r="48" fill="none" stroke="#64748b"/>' +
        '<circle cx="180" cy="130" r="68" fill="none" stroke="#64748b"/>' +
        '<text x="186" y="126" font-size="16">×</text>';
      if (lab) h += T(180, 40, 'جریان داخل صفحه') + T(180, 220, 'خطوط میدان دایره‌ای');
      return wrap(h);
    }
    if (t === 'trans') {
      var h = ttl +
        '<rect x="168" y="70" width="24" height="110" rx="3" fill="#94a3b8"/>';
      for (var i = 0; i < 5; i++) h += '<ellipse cx="158" cy="' + (85 + i * 18) + '" rx="10" ry="9" fill="none" stroke="#2563eb" stroke-width="1.6"/>';
      for (var i = 0; i < 8; i++) h += '<ellipse cx="202" cy="' + (80 + i * 12) + '" rx="10" ry="7" fill="none" stroke="#dc2626" stroke-width="1.6"/>';
      h += '<path d="M148 85 H80 V160 H148 M212 80 H280 V170 H212" fill="none" stroke="#1e293b" stroke-width="1.6"/>';
      if (lab) h += T(100, 200, 'اولیه') + T(260, 200, 'ثانویه') + T(180, 230, 'ترانسفورماتور');
      return wrap(h);
    }
    if (t === 'mot') {
      var h = ttl +
        '<rect x="70" y="80" width="28" height="90" fill="#ef4444"/>' +
        '<rect x="262" y="80" width="28" height="90" fill="#3b82f6"/>' +
        T(84, 128, 'N') + T(276, 128, 'S') +
        '<rect x="130" y="100" width="100" height="50" fill="none" stroke="#1e293b" stroke-width="2"/>' +
        '<circle cx="180" cy="125" r="6" fill="#1e293b"/>';
      if (lab) h += T(180, 200, 'قاب در میدان') + T(180, 222, 'موتور الکتریکی');
      return wrap(h);
    }
    if (t === 'titr') {
      var h = ttl +
        '<rect x="168" y="30" width="22" height="90" rx="3" fill="#e0f2fe" stroke="#0369a1"/>' +
        '<path d="M174 50 H184" stroke="#f472b6" stroke-width="6" opacity=".5"/>' +
        '<path d="M140 140 H220 L210 210 H150 Z" fill="#e0f2fe" stroke="#0369a1" stroke-width="2"/>' +
        '<ellipse cx="180" cy="188" rx="26" ry="8" fill="#f9a8d4" opacity=".7"/>';
      if (lab) h += T(210, 70, 'بورت') + T(180, 230, 'ارلن / تیتراسیون');
      return wrap(h);
    }
    if (t === 'filt') {
      var h = ttl +
        '<path d="M110 50 H250 L180 150 Z" fill="none" stroke="#0369a1" stroke-width="2"/>' +
        '<path d="M120 58 L180 140 L240 58" fill="#fef3c7" stroke="#b45309"/>' +
        '<path d="M150 170 H210 L200 220 H160 Z" fill="#e0f2fe" stroke="#0369a1"/>';
      if (lab) h += T(180, 40, 'قیف') + T(180, 100, 'کاغذ صافی') + T(180, 238, 'صاف‌کردن');
      return wrap(h);
    }
    if (t === 'sep') {
      var h = ttl +
        '<path d="M140 40 H220 V90 L190 160 V200 H170 V160 L140 90 Z" fill="#e0f2fe" stroke="#0369a1" stroke-width="2"/>' +
        '<path d="M148 70 H212" stroke="#fbbf24" stroke-width="16" opacity=".45"/>' +
        '<path d="M152 110 H208" stroke="#38bdf8" stroke-width="14" opacity=".45"/>' +
        '<rect x="168" y="198" width="24" height="10" fill="#64748b"/>';
      if (lab) h += T(250, 74, 'روغن') + T(256, 114, 'آب') + T(180, 230, 'قیف جداکننده');
      return wrap(h);
    }
    if (t === 'volt') {
      var h = ttl +
        '<rect x="50" y="90" width="110" height="90" rx="6" fill="#dbeafe" stroke="#1d4ed8"/>' +
        '<rect x="200" y="90" width="110" height="90" rx="6" fill="#fee2e2" stroke="#b91c1c"/>' +
        '<rect x="90" y="80" width="12" height="70" fill="#94a3b8"/>' +
        '<rect x="258" y="80" width="12" height="70" fill="#fbbf24"/>' +
        '<path d="M96 80 V50 H264 V80" fill="none" stroke="#1e293b" stroke-width="1.6"/>' +
        '<path d="M160 135 H200" stroke="#1e293b" stroke-width="3"/>';
      if (lab) h += T(105, 200, 'Zn / نمک') + T(255, 200, 'Cu / نمک') + T(180, 40, 'پیل گالوانی');
      return wrap(h);
    }
    if (t === 'exo') {
      var h = ttl +
        '<line x1="50" y1="210" x2="320" y2="210" stroke="#64748b"/>' +
        '<line x1="50" y1="210" x2="50" y2="40" stroke="#64748b"/>' +
        '<path d="M70 70 H150 L210 160 H300" fill="none" stroke="#dc2626" stroke-width="2.2"/>';
      if (lab) h += T(110, 60, 'واکنش‌دهنده‌ها') + T(260, 150, 'فراورده‌ها') + T(30, 40, 'E') + T(180, 232, 'گرمازا');
      return wrap(h);
    }
    if (t === 'endo') {
      var h = ttl +
        '<line x1="50" y1="210" x2="320" y2="210" stroke="#64748b"/>' +
        '<line x1="50" y1="210" x2="50" y2="40" stroke="#64748b"/>' +
        '<path d="M70 170 H150 L210 70 H300" fill="none" stroke="#2563eb" stroke-width="2.2"/>';
      if (lab) h += T(110, 160, 'واکنش‌دهنده‌ها') + T(260, 60, 'فراورده‌ها') + T(30, 40, 'E') + T(180, 232, 'گرماگیر');
      return wrap(h);
    }
    if (t === 'benz') {
      var h = ttl +
        '<polygon points="180,60 250,100 250,170 180,210 110,170 110,100" fill="none" stroke="#1e293b" stroke-width="2.2"/>' +
        '<circle cx="180" cy="135" r="28" fill="none" stroke="#1e293b" stroke-width="2"/>';
      if (lab) h += T(180, 236, 'بنزن  C₆H₆');
      return wrap(h);
    }
    if (t === 'alk') {
      var h = ttl +
        '<circle cx="80" cy="130" r="20" fill="#334155"/><text x="80" y="135" text-anchor="middle" fill="#fff">C</text>' +
        '<line x1="100" y1="130" x2="140" y2="130" stroke="#334155" stroke-width="5"/>' +
        '<circle cx="160" cy="130" r="20" fill="#334155"/><text x="160" y="135" text-anchor="middle" fill="#fff">C</text>' +
        '<line x1="180" y1="130" x2="220" y2="130" stroke="#334155" stroke-width="5"/>' +
        '<circle cx="240" cy="130" r="20" fill="#334155"/><text x="240" y="135" text-anchor="middle" fill="#fff">C</text>';
      if (lab) h += T(180, 190, 'زنجیره آلکان') + T(80, 90, 'CH₃') + T(160, 90, 'CH₂') + T(240, 90, 'CH₃');
      return wrap(h);
    }
    if (t === 'func') {
      var h = ttl +
        '<circle cx="90" cy="120" r="22" fill="#334155"/><text x="90" y="125" text-anchor="middle" fill="#fff">R</text>' +
        '<line x1="112" y1="120" x2="150" y2="120" stroke="#334155" stroke-width="5"/>' +
        '<circle cx="176" cy="120" r="24" fill="#ef4444"/><text x="176" y="125" text-anchor="middle" fill="#fff">OH</text>' +
        '<circle cx="270" cy="120" r="24" fill="#3b82f6"/><text x="270" y="125" text-anchor="middle" fill="#fff">COOH</text>';
      if (lab) h += T(176, 170, 'الکل') + T(270, 170, 'اسید') + T(180, 210, 'گروه‌های عاملی');
      return wrap(h);
    }
    if (t === 'echo') {
      var h = ttl +
        '<rect x="40" y="80" width="40" height="70" rx="6" fill="#93c5fd"/>' + T(60, 120, '🔊') +
        '<path d="M100 90 Q160 115 100 140" fill="none" stroke="#2563eb" stroke-width="1.8"/>' +
        '<path d="M110 80 Q190 115 110 150" fill="none" stroke="#2563eb" stroke-width="1.8"/>' +
        '<rect x="280" y="50" width="18" height="150" fill="#94a3b8"/>' +
        '<path d="M270 90 Q210 115 270 140" fill="none" stroke="#dc2626" stroke-width="1.8"/>';
      if (lab) h += T(150, 80, 'تابش') + T(230, 80, 'پژواک') + T(180, 220, 'صوت و دیوار');
      return wrap(h);
    }
    if (t === 'calor') {
      var h = ttl +
        '<rect x="90" y="70" width="180" height="130" rx="8" fill="#e2e8f0" stroke="#334155" stroke-width="2"/>' +
        '<rect x="110" y="90" width="140" height="90" rx="6" fill="#e0f2fe" stroke="#0369a1"/>' +
        '<rect x="170" y="40" width="20" height="50" fill="#94a3b8"/>' +
        '<circle cx="180" cy="160" r="10" fill="#f97316"/>';
      if (lab) h += T(180, 36, 'دماسنج') + T(180, 220, 'کالریمتر');
      return wrap(h);
    }
    return wrap(ttl + T(180, 130, '—'));
  }

  function clampPct(v) {
    v = +v;
    if (isNaN(v)) return 50;
    return Math.max(1, Math.min(99, v));
  }
  function marksOf(spec) {
    var X = (spec && spec.X) || {};
    return Array.isArray(X.marks) ? X.marks : [];
  }
  function nextMarkN(marks) {
    var used = {};
    marks.forEach(function (m) { used[m.n] = 1; });
    var n = 1;
    while (used[n]) n++;
    return n;
  }
  function plateOf(spec) {
    var marks = marksOf(spec);
    var mid = 'scAh' + Math.floor(Math.random() * 1e9);
    var ov = '<svg class="an-ov" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">' +
      '<defs><marker id="' + mid + '" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="4.2" markerHeight="4.2" orient="auto"><path d="M0,1 L10,5 L0,9 Z" fill="#4f46e5"/></marker></defs>';
    marks.forEach(function (m) {
      var dx = m.x2 - m.x1, dy = m.y2 - m.y1, len = Math.sqrt(dx * dx + dy * dy) || 1;
      ov += '<line x1="' + m.x1 + '" y1="' + m.y1 + '" x2="' + (m.x2 - dx / len * 3) + '" y2="' + (m.y2 - dy / len * 3) +
        '" stroke="#4f46e5" stroke-width="2" vector-effect="non-scaling-stroke" marker-end="url(#' + mid + ')"/>';
    });
    ov += '</svg>';
    marks.forEach(function (m, i) {
      ov += '<span class="an-tail" data-i="' + i + '" data-end="tail" style="left:' + m.x1 + '%;top:' + m.y1 + '%"></span>';
      ov += '<span class="an-num" data-i="' + i + '" data-end="tip" style="left:' + m.x2 + '%;top:' + m.y2 + '%">' + faNum(m.n) + '</span>';
    });
    var fields = '';
    if (String((spec.X || {}).blank || '1') !== '0' && marks.length) {
      fields = '<div class="an-af an-af-paper">';
      marks.forEach(function (m) {
        fields += '<div class="an-af-row"><span class="an-af-n">' + faNum(m.n) + '</span><span class="an-af-box"></span></div>';
      });
      fields += '</div>';
    }
    var ttl = (spec.X && spec.X.title) ? '<div class="tbx-cap">' + esc(spec.X.title) + '</div>' : '';
        var media;
    if (window.SCIENCE_ATLAS && window.SCIENCE_ATLAS[spec.t]) {
      media = '<img class="sc-svg an-svg" alt="" src="' + window.SCIENCE_ATLAS[spec.t] + '">';
    } else media = svgOf(spec);
    return ttl + '<div class="an-plate"><div class="an-stage"><div class="an-frame">' + media + ov + '</div></div>' + fields + '</div>';
  }

  var state = { k: 's', t: 'cSim', X: { title: '', lab: '1', marks: [], blank: '1' } };
  var replaceEl = null, cat = 'all';

  function fitScImage() {
    var pane = document.getElementById('scPreview');
    var img = pane && pane.querySelector('img.sc-svg, img.an-svg');
    var frame = pane && pane.querySelector('.an-frame');
    if (!pane || !img) return;
    function apply() {
      if (!img.isConnected) return;
      var mid = document.querySelector('#scOverlay .an-mid');
      var r = (mid || pane).getBoundingClientRect();
      var w = Math.max(80, Math.floor(r.width - 24));
      var h = Math.max(80, Math.floor(r.height - 24));
      if (h < 80) { requestAnimationFrame(apply); return; }
      var nw = img.naturalWidth || 0, nh = img.naturalHeight || 0;
      if (nw && nh) {
        var sca = Math.min(w / nw, h / nh);
        if (sca > 0 && isFinite(sca)) {
          img.style.width = Math.floor(nw * sca) + 'px';
          img.style.height = Math.floor(nh * sca) + 'px';
        }
      }
      img.style.maxWidth = w + 'px';
      img.style.maxHeight = h + 'px';
      img.style.objectFit = 'contain';
      if (frame) {
        frame.style.maxWidth = w + 'px';
        frame.style.maxHeight = h + 'px';
        frame.style.overflow = 'hidden';
      }
    }
    if (!img.complete) img.addEventListener('load', apply, { once: true });
    apply();
    requestAnimationFrame(apply);
  }
  function paint() {
    var box = $('scPreview');
    if (box) {
      var spec = JSON.parse(JSON.stringify(state));
      if (!spec.X) spec.X = {};
      spec.X.blank = '0';
      box.innerHTML = plateOf(spec);
      var cap = box.querySelector('.tbx-cap');
      if (cap) cap.remove();
      fitScImage();
      requestAnimationFrame(function () {
        bindDraw(box.querySelector('.an-frame'));
      });
    }
    var tit = $('scTitle');
    if (tit && document.activeElement !== tit) tit.value = (state.X && state.X.title) || '';
    var lab = $('scLab');
    if (lab) lab.checked = String((state.X && state.X.lab) || '1') !== '0';
    renderAnswerFields();
  }

  function pctOnImg(e, box) {
    var r = box.getBoundingClientRect();
    var cs = window.getComputedStyle(box);
    var bl = parseFloat(cs.borderLeftWidth) || 0;
    var bt = parseFloat(cs.borderTopWidth) || 0;
    var br = parseFloat(cs.borderRightWidth) || 0;
    var bb = parseFloat(cs.borderBottomWidth) || 0;
    var w = r.width - bl - br;
    var h = r.height - bt - bb;
    if (w < 2 || h < 2) return null;
    return {
      x: clampPct((e.clientX - r.left - bl) / w * 100),
      y: clampPct((e.clientY - r.top - bt) / h * 100)
    };
  }
  function setDraftLine(frame, a, b) {
    var svg = frame.querySelector('svg.an-draft');
    if (!svg) {
      svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
      svg.setAttribute('class', 'an-draft');
      svg.setAttribute('viewBox', '0 0 100 100');
      svg.setAttribute('preserveAspectRatio', 'none');
      var ln = document.createElementNS('http://www.w3.org/2000/svg', 'line');
      ln.setAttribute('stroke', '#4f46e5');
      ln.setAttribute('stroke-width', '2');
      ln.setAttribute('stroke-linecap', 'round');
      ln.setAttribute('vector-effect', 'non-scaling-stroke');
      svg.appendChild(ln);
      frame.appendChild(svg);
    }
    var ln = svg.firstChild;
    ln.setAttribute('x1', a.x); ln.setAttribute('y1', a.y);
    ln.setAttribute('x2', b.x); ln.setAttribute('y2', b.y);
    svg.style.display = '';
  }
  function bindDraw(frame) {
    if (!frame) return;
    frame.classList.add('is-edit');
    var drawing = null, drag = null;
    function onMove(ev) {
      if (drag) {
        var p = pctOnImg(ev, frame);
        if (!p) return;
        var el = frame.querySelector((drag.end === 'tip' ? '.an-num' : '.an-tail') + '[data-i="' + drag.i + '"]');
        if (el) { el.style.left = p.x + '%'; el.style.top = p.y + '%'; }
        var m = state.X.marks[drag.i];
        if (m) {
          if (drag.end === 'tip') { m.x2 = p.x; m.y2 = p.y; }
          else { m.x1 = p.x; m.y1 = p.y; }
        }
        return;
      }
      if (drawing) {
        var p = pctOnImg(ev, frame);
        if (p) { drawing.b = p; setDraftLine(frame, drawing.a, drawing.b); }
      }
    }
    function onUp(ev) {
      document.removeEventListener('mousemove', onMove);
      document.removeEventListener('mouseup', onUp);
      if (drag) {
        var p = pctOnImg(ev, frame);
        if (p && state.X.marks[drag.i]) {
          if (drag.end === 'tip') { state.X.marks[drag.i].x2 = p.x; state.X.marks[drag.i].y2 = p.y; }
          else { state.X.marks[drag.i].x1 = p.x; state.X.marks[drag.i].y1 = p.y; }
        }
        drag = null;
        paint();
        return;
      }
      if (!drawing) return;
      var start = drawing.a;
      var p = pctOnImg(ev, frame) || drawing.b || start;
      var dx = p.x - start.x, dy = p.y - start.y;
      var ok = Math.sqrt(dx * dx + dy * dy) >= 4;
      drawing = null;
      var draft = frame.querySelector('svg.an-draft');
      if (draft) draft.remove();
      if (!ok) return;
      if (!state.X) state.X = {};
      if (!Array.isArray(state.X.marks)) state.X.marks = [];
      if (state.X.marks.length >= 12) return;
      state.X.marks.push({ x1: start.x, y1: start.y, x2: p.x, y2: p.y, n: nextMarkN(state.X.marks), lbl: '' });
      state._sel = state.X.marks.length - 1;
      paint();
    }
    frame.addEventListener('mousedown', function (e) {
      if (e.button !== 0) return;
      var handle = e.target.closest('.an-num, .an-tail');
      var p = pctOnImg(e, frame);
      if (handle) {
        e.preventDefault();
        e.stopPropagation();
        drag = { i: +handle.getAttribute('data-i'), end: handle.getAttribute('data-end') };
        state._sel = drag.i;
        document.addEventListener('mousemove', onMove);
        document.addEventListener('mouseup', onUp);
        return;
      }
      if (!p) return;
      e.preventDefault();
      drawing = { a: p, b: p };
      setDraftLine(frame, p, p);
      document.addEventListener('mousemove', onMove);
      document.addEventListener('mouseup', onUp);
    });
  }

  function renderAnswerFields() {
    var host = $('scFieldsList');
    if (!host) return;
    var marks = marksOf(state);
    if (!marks.length) {
      host.innerHTML = '<p class="muted" style="font-size:11px;margin:6px 0 0">روی تصویر کلیک کنید و بکشید تا فلش شماره‌دار بگذارید.</p>';
      return;
    }
    host.innerHTML = marks.map(function (m, i) {
      return '<div class="an-af-row"><span class="an-af-n">' + faNum(m.n) + '</span>' +
        '<input class="an-af-in" data-i="' + i + '" placeholder="نام این قسمت" value="' + esc(m.lbl || '') + '">' +
        '<button type="button" class="an-del" data-i="' + i + '">حذف</button></div>';
    }).join('');
    host.querySelectorAll('.an-af-in').forEach(function (inp) {
      inp.addEventListener('input', function () {
        var i = +inp.getAttribute('data-i');
        if (state.X.marks[i]) state.X.marks[i].lbl = inp.value;
      });
    });
    host.querySelectorAll('.an-del').forEach(function (b) {
      b.onclick = function () {
        state.X.marks.splice(+b.getAttribute('data-i'), 1);
        paint();
      };
    });
  }

  function renderCats() {
    var host = $('scCats');
    if (!host) return;
    host.innerHTML = '';
    catsOf().forEach(function (c) {
      var b = document.createElement('button');
      b.type = 'button'; b.className = 'an-cat' + (c.id === cat ? ' on' : '');
      b.setAttribute('data-c', c.id); b.textContent = c.name;
      b.addEventListener('click', function () { cat = c.id; renderList(); setType(state.t); });
      host.appendChild(b);
    });
  }
  function renderList() {
    var host = $('scShapes');
    if (!host) return;
    host.innerHTML = '';
    typesOf().filter(function (s) { return cat === 'all' || s.cat === cat; }).forEach(function (s) {
      var b = document.createElement('button');
      b.type = 'button';
      b.className = 'gf-shape' + (s.id === state.t ? ' on' : '');
      b.setAttribute('data-t', s.id);
      var thumb = (window.SCIENCE_ATLAS && window.SCIENCE_ATLAS[s.id])
        ? '<img class="an-thumb" alt="" src="' + window.SCIENCE_ATLAS[s.id] + '">'
        : '';
      b.innerHTML = thumb + '<span>' + s.name + '</span>';
      b.addEventListener('click', function () { setType(s.id); });
      host.appendChild(b);
    });
  }
  function setType(id) {
    var prev = state.t;
    state.t = id; state.k = 's';
    if (!state.X) state.X = { title: '', lab: '1', marks: [], blank: '1' };
    var meta = metaOf(id);
    if (!state.X.title || state.X.title === metaOf(prev).name) state.X.title = meta.name;
    if (prev && prev !== id) state.X.marks = [];
    document.querySelectorAll('#scShapes .gf-shape').forEach(function (b) {
      b.classList.toggle('on', b.getAttribute('data-t') === id);
    });
    document.querySelectorAll('#scCats .an-cat').forEach(function (b) {
      b.classList.toggle('on', b.getAttribute('data-c') === cat);
    });
    paint();
  }

  function ensureModal() {
    if ($('scOverlay')) return;
    var ov = document.createElement('div');
    ov.id = 'scOverlay';
    ov.className = 'gf-overlay';
    ov.innerHTML =
      '<div class="gf-modal gf-3 sc-modal" role="dialog" aria-label="فیزیک و شیمی">' +
        '<div class="gf-head"><h3>فیزیک و شیمی</h3><button type="button" class="gf-x" id="scClose">×</button></div>' +
        '<div class="gf-split an-split">' +
          '<aside class="gf-types"><div class="gf-types-h">نوع شکل</div><div class="an-cats" id="scCats"></div><div class="gf-shapes" id="scShapes"></div></aside>' +
          '<section class="an-mid"><div class="gf-preview" id="scPreview"></div></section>' +
          '<aside class="an-side">' +
            '<div class="an-side-h">تنظیمات و شماره‌گذاری</div>' +
            '<div class="gf-fields">' +
              '<label class="gf-span">عنوان<input id="scTitle" placeholder="اختیاری"></label>' +
              '<label class="gf-span" style="display:flex;flex-direction:row;align-items:center;gap:8px"><input id="scLab" type="checkbox" checked style="width:auto">نمایش برچسب‌ها</label>' +
              '<p class="muted" style="font-size:11px;margin:0">برای سؤال نام‌گذاری، روی شکل کلیک کنید و فلش بکشید.</p>' +
              '<div id="scFieldsList" class="an-af"></div>' +
              '<button type="button" class="gf-btn ghost" id="scClear">پاک‌کردن فلش‌ها</button>' +
            '</div>' +
          '</aside>' +
        '</div>' +
        '<div class="gf-foot"><button type="button" class="gf-btn ghost" id="scCancel">انصراف</button>' +
        '<button type="button" class="gf-btn ok" id="scApply">درج در سؤال</button></div>' +
      '</div>';
    document.body.appendChild(ov);
    renderCats();
    renderList();
    ov.addEventListener('click', function (e) { if (e.target === ov) close(); });
    $('scClose').onclick = close;
    $('scCancel').onclick = close;
    $('scApply').onclick = apply;
    $('scTitle').addEventListener('input', function () {
      if (!state.X) state.X = {};
      state.X.title = this.value;
    });
    $('scLab').addEventListener('change', function () {
      if (!state.X) state.X = {};
      state.X.lab = this.checked ? '1' : '0';
      paint();
    });
    $('scClear').onclick = function () {
      if (state.X) state.X.marks = [];
      paint();
    };
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
    var tit = $('scTitle');
    if (!state.X) state.X = {};
    if (tit) state.X.title = tit.value;
    var lab = $('scLab');
    state.X.lab = lab && !lab.checked ? '0' : '1';
    state.k = 's';
    if (replaceEl && replaceEl.classList && replaceEl.classList.contains('qmf-fig')) {
      replaceEl.dataset.fig = JSON.stringify(state);
      replaceEl.setAttribute('data-fig', JSON.stringify(state));
      replaceEl.innerHTML = plateOf(state);
      var t = targetTextarea();
      try { if (t && window.QMF) QMF.syncFromSurface(t); } catch (e) {}
      try { if (t && window.__examBuilderUpdateQuestionText) window.__examBuilderUpdateQuestionText(t); } catch (e) {}
      try { if (typeof renderPreview === 'function') renderPreview(); } catch (e) {}
    } else insertToken(tokenOf(state));
    close();
  }
  function open(spec, el, dom) {
    replaceEl = el || null;
    var defId = (dom === 'chem') ? 'beak' : 'cSim';
    try { state = spec ? JSON.parse(JSON.stringify(spec)) : { k: 's', t: defId, X: { title: metaOf(defId).name, lab: '1', marks: [], blank: '1' } }; }
    catch (e) { state = { k: 's', t: defId, X: { title: metaOf(defId).name, lab: '1', marks: [], blank: '1' } }; }
    state.k = 's';
    if (!state.t) state.t = defId;
    if (!state.X) state.X = { title: '', lab: '1', marks: [], blank: '1' };
    if (!Array.isArray(state.X.marks)) state.X.marks = [];
    domain = dom || inferDomain(state.t);
    cat = 'all';
    ensureModal();
    var h3 = document.querySelector('#scOverlay h3');
    if (h3) h3.textContent = domain === 'chem' ? 'شیمی' : 'فیزیک';
    $('scOverlay').classList.add('open');
    var ok = $('scApply');
    if (ok) ok.textContent = replaceEl ? 'اعمال تغییرات' : 'درج در سؤال';
    renderCats();
    renderList();
    setType(state.t);
  }
  function openFromEl(fig) {
    if (!fig) return false;
    var spec = {};
    try { spec = JSON.parse(fig.getAttribute('data-fig') || fig.dataset.fig || '{}'); }
    catch (e) { spec = { k: 's', t: 'cSim' }; }
    open(spec, fig);
    return true;
  }
  function close() {
    var ov = $('scOverlay');
    if (ov) ov.classList.remove('open');
    replaceEl = null;
  }
  function make(raw) {
    var el = document.createElement('span');
    el.className = 'qmf-fig qmf-sc';
    el.contentEditable = 'false';
    el.setAttribute('data-fig', raw);
    el.dataset.fig = raw;
    el.setAttribute('role', 'img');
    el.setAttribute('aria-label', 'فیزیک و شیمی');
    el.title = 'برای ویرایش دوبار کلیک کنید';
    try { el.innerHTML = plateOf(JSON.parse(raw)); }
    catch (e) { el.textContent = '[فیزیک/شیمی]'; }
    return el;
  }
  function bind() {
    var bp = $('openPhysicsEditor');
    if (bp && !bp._scBound) {
      bp._scBound = true;
      bp.addEventListener('click', function (e) { e.preventDefault(); open(null, null, 'phys'); });
    }
    var bc = $('openChemistryEditor');
    if (bc && !bc._scBound) {
      bc._scBound = true;
      bc.addEventListener('click', function (e) { e.preventDefault(); open(null, null, 'chem'); });
    }
  }

  window.ScienceFig = { svg: svgOf, make: make, open: open, openFromEl: openFromEl, close: close };

  if (window.GeoFig) {
    var oldMake = window.GeoFig.make;
    var oldOpen = window.GeoFig.openFromEl;
    window.GeoFig.make = function (raw) {
      try {
        var s = JSON.parse(raw);
        if (s && s.k === 's') return make(raw);
      } catch (e) {}
      return oldMake ? oldMake(raw) : make(raw);
    };
    window.GeoFig.openFromEl = function (fig) {
      try {
        var s = JSON.parse(fig.getAttribute('data-fig') || fig.dataset.fig || '{}');
        if (s && s.k === 's') return openFromEl(fig);
      } catch (e) {}
      return oldOpen ? oldOpen(fig) : openFromEl(fig);
    };
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', bind);
  else bind();
})();
