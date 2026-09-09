(function () {
  var CATS = {
    alk: '#fecaca', ae: '#fed7aa', tm: '#c7d2fe', ptm: '#e9d5ff',
    met: '#a7f3d0', nm: '#bbf7d0', hal: '#fde68a', ng: '#a5f3fc',
    lan: '#fbcfe8', act: '#f5d0fe', un: '#e2e8f0'
  };
  var CN = {
    alk: 'قلیایی', ae: 'قلیایی خاکی', tm: 'واسطه', ptm: 'پس‌واسطه',
    met: 'شبه فلز', nm: 'نافلز', hal: 'هالوژن', ng: 'نجیب',
    lan: 'لانتانید', act: 'اکتینید', un: 'نامشخص'
  };
  var EL = [
    [1,'H','هیدروژن',1,1,'nm'],[2,'He','هلیم',18,1,'ng'],
    [3,'Li','لیتیوم',1,2,'alk'],[4,'Be','بریلیوم',2,2,'ae'],[5,'B','بور',13,2,'met'],[6,'C','کربن',14,2,'nm'],[7,'N','نیتروژن',15,2,'nm'],[8,'O','اکسیژن',16,2,'nm'],[9,'F','فلوئور',17,2,'hal'],[10,'Ne','نئون',18,2,'ng'],
    [11,'Na','سدیم',1,3,'alk'],[12,'Mg','منیزیم',2,3,'ae'],[13,'Al','آلومینیم',13,3,'ptm'],[14,'Si','سیلیسیم',14,3,'met'],[15,'P','فسفر',15,3,'nm'],[16,'S','گوگرد',16,3,'nm'],[17,'Cl','کلر',17,3,'hal'],[18,'Ar','آرگون',18,3,'ng'],
    [19,'K','پتاسیم',1,4,'alk'],[20,'Ca','کلسیم',2,4,'ae'],[21,'Sc','اسکاندیم',3,4,'tm'],[22,'Ti','تیتانیم',4,4,'tm'],[23,'V','وانادیم',5,4,'tm'],[24,'Cr','کروم',6,4,'tm'],[25,'Mn','منگنز',7,4,'tm'],[26,'Fe','آهن',8,4,'tm'],[27,'Co','کبالت',9,4,'tm'],[28,'Ni','نیکل',10,4,'tm'],[29,'Cu','مس',11,4,'tm'],[30,'Zn','روی',12,4,'tm'],[31,'Ga','گالیم',13,4,'ptm'],[32,'Ge','ژرمانیم',14,4,'met'],[33,'As','آرسنیک',15,4,'met'],[34,'Se','سلنیم',16,4,'nm'],[35,'Br','برم',17,4,'hal'],[36,'Kr','کریپتون',18,4,'ng'],
    [37,'Rb','روبیدیم',1,5,'alk'],[38,'Sr','استرانسیم',2,5,'ae'],[39,'Y','ایتریم',3,5,'tm'],[40,'Zr','زیرکونیم',4,5,'tm'],[41,'Nb','نیوبیم',5,5,'tm'],[42,'Mo','مولیبدن',6,5,'tm'],[43,'Tc','تکنسیم',7,5,'tm'],[44,'Ru','روتنیم',8,5,'tm'],[45,'Rh','رودیم',9,5,'tm'],[46,'Pd','پالادیم',10,5,'tm'],[47,'Ag','نقره',11,5,'tm'],[48,'Cd','کادمیم',12,5,'tm'],[49,'In','ایندیم',13,5,'ptm'],[50,'Sn','قلع',14,5,'ptm'],[51,'Sb','آنتیموان',15,5,'met'],[52,'Te','تلوریم',16,5,'met'],[53,'I','ید',17,5,'hal'],[54,'Xe','زنون',18,5,'ng'],
    [55,'Cs','سزیم',1,6,'alk'],[56,'Ba','باریم',2,6,'ae'],[57,'La','لانتان',3,8,'lan'],[72,'Hf','هافنیم',4,6,'tm'],[73,'Ta','تانتال',5,6,'tm'],[74,'W','تنگستن',6,6,'tm'],[75,'Re','رنیم',7,6,'tm'],[76,'Os','اسمیم',8,6,'tm'],[77,'Ir','ایریدیم',9,6,'tm'],[78,'Pt','پلاتین',10,6,'tm'],[79,'Au','طلا',11,6,'tm'],[80,'Hg','جیوه',12,6,'tm'],[81,'Tl','تالیم',13,6,'ptm'],[82,'Pb','سرب',14,6,'ptm'],[83,'Bi','بیسموت',15,6,'ptm'],[84,'Po','پولونیم',16,6,'met'],[85,'At','استاتین',17,6,'hal'],[86,'Rn','رادون',18,6,'ng'],
    [87,'Fr','فرانسیم',1,7,'alk'],[88,'Ra','رادیم',2,7,'ae'],[89,'Ac','اکتینیم',3,9,'act'],[104,'Rf','رادرفوردیم',4,7,'tm'],[105,'Db','دوبنیم',5,7,'tm'],[106,'Sg','سیبورگیم',6,7,'tm'],[107,'Bh','بوریم',7,7,'un'],[108,'Hs','هاسیم',8,7,'un'],[109,'Mt','مایتنریم',9,7,'un'],[110,'Ds','دارمشتادیم',10,7,'un'],[111,'Rg','رونتگنیم',11,7,'un'],[112,'Cn','کوپرنیسیم',12,7,'tm'],[113,'Nh','نیهونیم',13,7,'un'],[114,'Fl','فلروویم',14,7,'un'],[115,'Mc','مسکوویم',15,7,'un'],[116,'Lv','لیورموریوم',16,7,'un'],[117,'Ts','تنسین',17,7,'un'],[118,'Og','اوگانسون',18,7,'ng'],
    [58,'Ce','سریم',4,8,'lan'],[59,'Pr','پرازئودیمیم',5,8,'lan'],[60,'Nd','نئودیمیم',6,8,'lan'],[61,'Pm','پرومتیم',7,8,'lan'],[62,'Sm','ساماریم',8,8,'lan'],[63,'Eu','یوروپیم',9,8,'lan'],[64,'Gd','گادولینیم',10,8,'lan'],[65,'Tb','تربیم',11,8,'lan'],[66,'Dy','دیسپروزیم',12,8,'lan'],[67,'Ho','هولمیم',13,8,'lan'],[68,'Er','اربیم',14,8,'lan'],[69,'Tm','تولیم',15,8,'lan'],[70,'Yb','ایتربیم',16,8,'lan'],[71,'Lu','لوتتیم',17,8,'lan'],
    [90,'Th','توریم',4,9,'act'],[91,'Pa','پروتاکتینیم',5,9,'act'],[92,'U','اورانیم',6,9,'act'],[93,'Np','نپتونیم',7,9,'act'],[94,'Pu','پلوتونیم',8,9,'act'],[95,'Am','آمریکیم',9,9,'act'],[96,'Cm','کوریم',10,9,'act'],[97,'Bk','برکلیم',11,9,'act'],[98,'Cf','کالیفرنیم',12,9,'act'],[99,'Es','اینشتینیم',13,9,'act'],[100,'Fm','فرمیم',14,9,'act'],[101,'Md','مندلیفیم',15,9,'act'],[102,'No','نوبلیم',16,9,'act'],[103,'Lr','لارنسیم',17,9,'act']
  ];
  var BYZ = {};
  EL.forEach(function (e) { BYZ[e[0]] = { z: e[0], s: e[1], n: e[2], g: e[3], p: e[4], c: e[5] }; });

  var PRESETS = [
    { id: 'full', name: 'کامل' },
    { id: 'main', name: 'گروه اصلی' },
    { id: 'noF', name: 'بدون f' },
    { id: 'noZ', name: 'بدون عدد اتمی' }
  ];

  function def() {
    return { k: 'p', t: 'full', X: { title: 'جدول تناوبی', Z: '1', hid: [], hidZ: [], hideCols: [], hideRows: [], hideF: '0' } };
  }
  function arr(x) { return Array.isArray(x) ? x.slice() : []; }
  function has(a, v) { return arr(a).indexOf(+v) >= 0 || arr(a).indexOf(v) >= 0; }
  function tog(a, v) {
    a = arr(a).map(Number);
    v = +v;
    var i = a.indexOf(v);
    if (i >= 0) a.splice(i, 1); else a.push(v);
    return a;
  }
  function esc(s) {
    return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) {
      return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c];
    });
  }
  function faNum(n) {
    return String(n).replace(/[0-9]/g, function (d) { return '۰۱۲۳۴۵۶۷۸۹'[+d]; });
  }
  function $(id) { return document.getElementById(id); }

  function visCols(X) {
    var hide = arr(X.hideCols).map(Number);
    var cols = [];
    for (var g = 1; g <= 18; g++) if (hide.indexOf(g) < 0) cols.push(g);
    return cols;
  }
  function visRows(X) {
    var hide = arr(X.hideRows).map(Number);
    var rows = [];
    for (var p = 1; p <= 7; p++) if (hide.indexOf(p) < 0) rows.push(p);
    return rows;
  }
  function elAt(g, p) {
    for (var i = 0; i < EL.length; i++) {
      if (EL[i][3] === g && EL[i][4] === p) return BYZ[EL[i][0]];
    }
    return null;
  }

  function cellOf(el, X, edit) {
    if (!el) return '<div class="pt-gap"></div>';
    var tag = edit ? 'button type="button"' : 'div';
    var end = edit ? 'button' : 'div';
    if (has(X.hid, el.z)) {
      return '<' + tag + ' class="pt-el is-off" data-z="' + el.z + '" title="' + esc(el.n) + '">' +
        (edit ? '<span class="pt-sy">+</span>' : '') + '</' + end + '>';
    }
    var showZ = String(X.Z || '1') !== '0' && !has(X.hidZ, el.z);
    return '<' + tag + ' class="pt-el cat-' + el.c + '" data-z="' + el.z + '" title="' + esc(el.n) + '">' +
      (showZ ? '<span class="pt-z">' + faNum(el.z) + '</span>' : '<span class="pt-z is-blank"></span>') +
      '<span class="pt-sy">' + el.s + '</span></' + end + '>';
  }

  function htmlOf(spec, edit) {
    spec = spec || def();
    var X = spec.X || {};
    var cols = visCols(X);
    var rows = visRows(X);
    var ttl = X.title ? '<div class="tbx-cap">' + esc(X.title) + '</div>' : '';
    var h = '<div class="ptb">';
    h += '<div class="pt-wrap" style="--pc:' + cols.length + '">';
    h += '<div class="pt-row pt-head"><div class="pt-lab"></div>';
    cols.forEach(function (g) {
      h += edit
        ? '<button type="button" class="pt-gh" data-g="' + g + '" title="حذف/نمایش گروه">' + faNum(g) + '</button>'
        : '<div class="pt-gh">' + faNum(g) + '</div>';
    });
    h += '</div>';
    rows.forEach(function (p) {
      h += '<div class="pt-row"><div class="pt-lab"' + (edit ? ' data-p="' + p + '" title="حذف/نمایش دوره"' : '') + '>' + faNum(p) + '</div>';
      cols.forEach(function (g) {
        var el = elAt(g, p);
        if ((p === 6 && g === 3) || (p === 7 && g === 3)) {
          if (String(X.hideF || '0') === '1') h += '<div class="pt-gap pt-star"></div>';
          else h += '<div class="pt-gap pt-star">' + (p === 6 ? '*' : '**') + '</div>';
        } else h += cellOf(el, X, edit);
      });
      h += '</div>';
    });
    h += '</div>';
    if (String(X.hideF || '0') !== '1') {
      h += '<div class="pt-f">';
      [8, 9].forEach(function (p) {
        var mark = p === 8 ? '*' : '**';
        h += '<div class="pt-row"><div class="pt-lab">' + mark + '</div>';
        for (var g = 3; g <= 17; g++) h += cellOf(elAt(g, p), X, edit);
        h += '</div>';
      });
      h += '</div>';
    }
    h += '</div>';
    return ttl + h;
  }

  var state = def();
  var replaceEl = null;
  var mode = 'el';

  function applyPreset(id) {
    var X = state.X || {};
    if (id === 'full') {
      X.hideCols = []; X.hideRows = []; X.hid = []; X.hidZ = []; X.hideF = '0'; X.Z = '1';
    } else if (id === 'main') {
      X.hideCols = [3, 4, 5, 6, 7, 8, 9, 10, 11, 12]; X.hideF = '1'; X.hideRows = [];
    } else if (id === 'noF') {
      X.hideF = '1';
    } else if (id === 'noZ') {
      X.Z = '0';
    }
    state.t = id;
    state.X = X;
  }

  function paint() {
    var box = $('ptPreview');
    if (box) box.innerHTML = htmlOf(state, true);
    var tit = $('ptTitle');
    if (tit && document.activeElement !== tit) tit.value = (state.X && state.X.title) || '';
    var z = $('ptShowZ');
    if (z) z.checked = String((state.X && state.X.Z) || '1') !== '0';
    var f = $('ptShowF');
    if (f) f.checked = String((state.X && state.X.hideF) || '0') !== '1';
    document.querySelectorAll('#ptModes .pt-mode').forEach(function (b) {
      b.classList.toggle('on', b.getAttribute('data-m') === mode);
    });
    document.querySelectorAll('#ptPresets .gf-shape').forEach(function (b) {
      b.classList.toggle('on', b.getAttribute('data-t') === state.t);
    });
    renderHidden();
    bindGrid();
  }

  function bindGrid() {
    var box = $('ptPreview');
    if (!box) return;
    box.querySelectorAll('.pt-el').forEach(function (b) {
      b.addEventListener('click', function (e) {
        e.preventDefault();
        var z = +b.getAttribute('data-z');
        if (!state.X) state.X = {};
        if (mode === 'z') state.X.hidZ = tog(state.X.hidZ, z);
        else state.X.hid = tog(state.X.hid, z);
        paint();
      });
    });
    box.querySelectorAll('.pt-gh').forEach(function (b) {
      b.addEventListener('click', function (e) {
        e.preventDefault();
        if (!state.X) state.X = {};
        state.X.hideCols = tog(state.X.hideCols, +b.getAttribute('data-g'));
        paint();
      });
    });
    box.querySelectorAll('.pt-lab[data-p]').forEach(function (b) {
      b.addEventListener('click', function (e) {
        e.preventDefault();
        if (!state.X) state.X = {};
        state.X.hideRows = tog(state.X.hideRows, +b.getAttribute('data-p'));
        paint();
      });
    });
  }

  function renderHidden() {
    var host = $('ptHidden');
    if (!host) return;
    var X = state.X || {};
    var bits = [];
    arr(X.hideCols).forEach(function (g) {
      bits.push('<button type="button" class="pt-chip" data-k="hideCols" data-v="' + g + '">گروه ' + faNum(g) + ' ×</button>');
    });
    arr(X.hideRows).forEach(function (p) {
      bits.push('<button type="button" class="pt-chip" data-k="hideRows" data-v="' + p + '">دوره ' + faNum(p) + ' ×</button>');
    });
    arr(X.hid).forEach(function (z) {
      var el = BYZ[+z];
      bits.push('<button type="button" class="pt-chip" data-k="hid" data-v="' + z + '">' + (el ? el.s : z) + ' ×</button>');
    });
    arr(X.hidZ).forEach(function (z) {
      var el = BYZ[+z];
      bits.push('<button type="button" class="pt-chip" data-k="hidZ" data-v="' + z + '">Z ' + (el ? el.s : z) + ' ×</button>');
    });
    host.innerHTML = bits.length ? bits.join('') : '<p class="muted" style="font-size:11px;margin:0">چیزی حذف نشده. روی عنصر، سرستون یا شماره دوره کلیک کنید.</p>';
    host.querySelectorAll('.pt-chip').forEach(function (b) {
      b.onclick = function () {
        var k = b.getAttribute('data-k');
        state.X[k] = tog(state.X[k], +b.getAttribute('data-v'));
        paint();
      };
    });
  }

  function ensureModal() {
    if ($('ptOverlay')) return;
    var ov = document.createElement('div');
    ov.id = 'ptOverlay';
    ov.className = 'gf-overlay';
    ov.innerHTML =
      '<div class="gf-modal gf-3 pt-modal" role="dialog" aria-label="جدول تناوبی">' +
        '<div class="gf-head"><h3>جدول تناوبی</h3><button type="button" class="gf-x" id="ptClose">×</button></div>' +
        '<div class="gf-split an-split">' +
          '<aside class="gf-types"><div class="gf-types-h">حالت جدول</div><div class="gf-shapes" id="ptPresets"></div></aside>' +
          '<section class="an-mid"><div class="gf-preview pt-preview" id="ptPreview"></div></section>' +
          '<aside class="an-side">' +
            '<div class="an-side-h">ویرایش</div>' +
            '<div class="gf-fields">' +
              '<label class="gf-span">عنوان<input id="ptTitle" placeholder="جدول تناوبی"></label>' +
              '<div id="ptModes" class="pt-modes">' +
                '<button type="button" class="pt-mode on" data-m="el">حذف عنصر</button>' +
                '<button type="button" class="pt-mode" data-m="z">حذف عدد اتمی</button>' +
              '</div>' +
              '<label class="gf-span" style="display:flex;flex-direction:row;align-items:center;gap:8px">' +
                '<input id="ptShowZ" type="checkbox" checked style="width:auto">نمایش عدد اتمی' +
              '</label>' +
              '<label class="gf-span" style="display:flex;flex-direction:row;align-items:center;gap:8px">' +
                '<input id="ptShowF" type="checkbox" checked style="width:auto">لانتانید و اکتینید' +
              '</label>' +
              '<p class="muted" style="font-size:11px;margin:0">روی سرستون (۱…۱۸) کلیک کنید تا ستون حذف شود. روی شماره دوره کلیک کنید تا سطر حذف شود.</p>' +
              '<div id="ptHidden" class="pt-hidden"></div>' +
              '<button type="button" class="gf-btn ghost" id="ptRestore">بازگردانی همه</button>' +
            '</div>' +
          '</aside>' +
        '</div>' +
        '<div class="gf-foot"><button type="button" class="gf-btn ghost" id="ptCancel">انصراف</button>' +
        '<button type="button" class="gf-btn ok" id="ptApply">درج در سؤال</button></div>' +
      '</div>';
    document.body.appendChild(ov);
    PRESETS.forEach(function (s) {
      var b = document.createElement('button');
      b.type = 'button'; b.className = 'gf-shape'; b.setAttribute('data-t', s.id);
      b.innerHTML = '<span>' + s.name + '</span>';
      b.addEventListener('click', function () { applyPreset(s.id); paint(); });
      $('ptPresets').appendChild(b);
    });
    ov.addEventListener('click', function (e) { if (e.target === ov) close(); });
    $('ptClose').onclick = close;
    $('ptCancel').onclick = close;
    $('ptApply').onclick = apply;
    $('ptTitle').addEventListener('input', function () {
      if (!state.X) state.X = {};
      state.X.title = this.value;
      var cap = document.querySelector('#ptPreview .tbx-cap');
      if (cap) cap.textContent = this.value;
    });
    $('ptShowZ').addEventListener('change', function () {
      if (!state.X) state.X = {};
      state.X.Z = this.checked ? '1' : '0';
      paint();
    });
    $('ptShowF').addEventListener('change', function () {
      if (!state.X) state.X = {};
      state.X.hideF = this.checked ? '0' : '1';
      paint();
    });
    $('ptRestore').onclick = function () {
      applyPreset('full');
      paint();
    };
    document.querySelectorAll('#ptModes .pt-mode').forEach(function (b) {
      b.onclick = function () { mode = b.getAttribute('data-m'); paint(); };
    });
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
    var tit = $('ptTitle');
    if (!state.X) state.X = {};
    if (tit) state.X.title = tit.value;
    state.k = 'p';
    if (replaceEl && replaceEl.classList && replaceEl.classList.contains('qmf-fig')) {
      replaceEl.dataset.fig = JSON.stringify(state);
      replaceEl.setAttribute('data-fig', JSON.stringify(state));
      replaceEl.innerHTML = htmlOf(state, false);
      var t = targetTextarea();
      try { if (t && window.QMF) QMF.syncFromSurface(t); } catch (e) {}
      try { if (t && window.__examBuilderUpdateQuestionText) window.__examBuilderUpdateQuestionText(t); } catch (e) {}
      try { if (typeof renderPreview === 'function') renderPreview(); } catch (e) {}
    } else insertToken(tokenOf(state));
    close();
  }
  function open(spec, el) {
    replaceEl = el || null;
    try { state = spec ? JSON.parse(JSON.stringify(spec)) : def(); }
    catch (e) { state = def(); }
    state.k = 'p';
    if (!state.X) state.X = def().X;
    ['hid', 'hidZ', 'hideCols', 'hideRows'].forEach(function (k) {
      if (!Array.isArray(state.X[k])) state.X[k] = [];
    });
    ensureModal();
    $('ptOverlay').classList.add('open');
    var ok = $('ptApply');
    if (ok) ok.textContent = replaceEl ? 'اعمال تغییرات' : 'درج در سؤال';
    paint();
  }
  function openFromEl(fig) {
    if (!fig) return false;
    var spec = {};
    try { spec = JSON.parse(fig.getAttribute('data-fig') || fig.dataset.fig || '{}'); }
    catch (e) { spec = def(); }
    open(spec, fig);
    return true;
  }
  function close() {
    var ov = $('ptOverlay');
    if (ov) ov.classList.remove('open');
    replaceEl = null;
  }
  function make(raw) {
    var el = document.createElement('span');
    el.className = 'qmf-fig qmf-pt';
    el.contentEditable = 'false';
    el.setAttribute('data-fig', raw);
    el.dataset.fig = raw;
    el.setAttribute('role', 'img');
    el.setAttribute('aria-label', 'جدول تناوبی');
    el.title = 'برای ویرایش دوبار کلیک کنید';
    try { el.innerHTML = htmlOf(JSON.parse(raw), false); }
    catch (e) { el.textContent = '[جدول تناوبی]'; }
    return el;
  }
  function bind() {
    var btn = $('openPeriodicEditor');
    if (btn && !btn._ptBound) {
      btn._ptBound = true;
      btn.addEventListener('click', function (e) { e.preventDefault(); open(null, null); });
    }
  }

  window.PeriodicFig = { html: htmlOf, make: make, open: open, openFromEl: openFromEl, close: close };

  if (window.GeoFig) {
    var oldMake = window.GeoFig.make;
    var oldOpen = window.GeoFig.openFromEl;
    window.GeoFig.make = function (raw) {
      try {
        var s = JSON.parse(raw);
        if (s && s.k === 'p') return make(raw);
      } catch (e) {}
      return oldMake ? oldMake(raw) : make(raw);
    };
    window.GeoFig.openFromEl = function (fig) {
      try {
        var s = JSON.parse(fig.getAttribute('data-fig') || fig.dataset.fig || '{}');
        if (s && s.k === 'p') return openFromEl(fig);
      } catch (e) {}
      return oldOpen ? oldOpen(fig) : openFromEl(fig);
    };
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', bind);
  else bind();
})();
