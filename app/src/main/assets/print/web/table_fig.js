(function () {
  var TYPES = [
    { id: 'header', name: 'سرستون' },
    { id: 'head2', name: 'سرستون+سرردیف' },
    { id: 'simple', name: 'ساده' },
    { id: 'striped', name: 'راه‌راه' },
    { id: 'lined', name: 'خط‌کشی افقی' },
    { id: 'boxed', name: 'کادر ضخیم' },
    { id: 'exam', name: 'آزمونی' },
    { id: 'matrix', name: 'ماتریس' },
    { id: 'truth', name: 'جدول ارزش' },
    { id: 'freq', name: 'فراوانی' },
    { id: 'check', name: 'چک‌لیست' },
    { id: 'color', name: 'رنگی ستونی' },
    { id: 'account', name: 'حسابداری' },
    { id: 'round', name: 'مدرن گرد' },
    { id: 'grid', name: 'خانه‌ای' },
    { id: 'note', name: 'یادداشت' },
    { id: 'blue', name: 'آبی آموزشی' },
    { id: 'compact', name: 'فشرده' }
  ];
  var MIN_R = 1, MAX_R = 15, MIN_C = 1, MAX_C = 10;

  function esc(s) {
    return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }
  function clamp(n, a, b) { n = parseInt(n, 10); return isFinite(n) ? Math.max(a, Math.min(b, n)) : a; }
  function emptyRow(n) { var a = [], i; for (i = 0; i < n; i++) a.push(''); return a; }
  function resizeGrid(C, r, c) {
    C = (C || []).map(function (row) { return (row || []).slice(); });
    while (C.length < r) C.push(emptyRow(c));
    if (C.length > r) C = C.slice(0, r);
    C = C.map(function (row) {
      row = row.slice();
      while (row.length < c) row.push('');
      return row.slice(0, c);
    });
    return C;
  }
  function sample(t, r, c) {
    var C = [], i, j;
    for (i = 0; i < r; i++) {
      C[i] = [];
      for (j = 0; j < c; j++) C[i][j] = '';
    }
    if (t === 'truth') {
      C = [['p', 'q', 'p∧q', 'p∨q'], ['د', 'د', 'د', 'د'], ['د', 'ن', 'ن', 'د'], ['ن', 'د', 'ن', 'د'], ['ن', 'ن', 'ن', 'ن']];
    } else if (t === 'freq') {
      C = [['داده', 'فراوانی', 'نسبی'], ['A', '۸', '۰٫۴'], ['B', '۶', '۰٫۳'], ['C', '۶', '۰٫۳'], ['جمع', '۲۰', '۱']];
    } else if (t === 'exam') {
      C = [['#', 'گزینه ۱', 'گزینه ۲', 'گزینه ۳'], ['۱', '', '', ''], ['۲', '', '', ''], ['۳', '', '', '']];
    } else if (t === 'check') {
      C = [['☐', 'مورد', 'توضیح'], ['☐', '', ''], ['☑', '', ''], ['☐', '', '']];
    } else if (t === 'matrix') {
      C = [['a', 'b'], ['c', 'd']];
    } else if (t === 'account') {
      C = [['شرح', 'بدهکار', 'بستانکار'], ['', '', ''], ['', '', ''], ['جمع', '', '']];
    } else {
      for (j = 0; j < c; j++) C[0][j] = 'ستون ' + (j + 1);
      for (i = 1; i < r; i++) C[i][0] = String(i);
    }
    return resizeGrid(C, r, c);
  }
  function def(t) {
    t = t || 'header';
    var r = (t === 'truth') ? 5 : (t === 'matrix' ? 2 : 4);
    var c = (t === 'matrix') ? 2 : (t === 'account' || t === 'freq' ? 3 : 4);
    return { k: 't', t: t, X: { title: '' }, C: sample(t, r, c) };
  }
  function isHead(spec, r, c) {
    var t = spec.t || 'header';
    if (t === 'simple' || t === 'matrix' || t === 'grid') return false;
    if (t === 'head2') return r === 0 || c === 0;
    if (t === 'check') return r === 0;
    return r === 0;
  }
  function htmlOf(spec, editable) {
    spec = spec || def('header');
    var t = spec.t || 'header';
    var C = spec.C || [['']];
    if (!C.length) C = [['']];
    if (!(C[0] && C[0].length)) C[0] = [''];
    var title = (spec.X && spec.X.title) || '';
    var rows = C.length, cols = (C[0] || []).length;
    var h = '<div class="tbx tbx-' + t + (editable ? ' tb-ed' : '') + '" dir="rtl">';
    if (title) h += '<div class="tbx-cap">' + esc(title) + '</div>';
    if (t === 'matrix') h += '<div class="tbx-wrap"><span class="tbx-br l"></span>';
    h += '<table class="tbx-t"><tbody>';
    var r, c;
    for (r = 0; r < rows; r++) {
      h += '<tr>';
      for (c = 0; c < cols; c++) {
        var tag = isHead(spec, r, c) ? 'th' : 'td';
        var val = (C[r] && C[r][c] != null) ? String(C[r][c]) : '';
        if (editable) {
          h += '<' + tag + '><input data-r="' + r + '" data-c="' + c + '" value="' + esc(val) + '" spellcheck="false"></' + tag + '>';
        } else {
          h += '<' + tag + '>' + esc(val) + '</' + tag + '>';
        }
      }
      h += '</tr>';
    }
    h += '</tbody></table>';
    if (t === 'matrix') h += '<span class="tbx-br r"></span></div>';
    h += '</div>';
    return h;
  }
  function iconOf(id) {
    var m = {
      header: '<path d="M4,6 H32 V24 H4 Z M4,11 H32" fill="none" stroke="currentColor" stroke-width="1.6"/><path d="M4,6 H32 V11 H4 Z" fill="currentColor" opacity=".35"/>',
      head2: '<path d="M4,6 H32 V24 H4 Z M4,11 H32 M12,6 V24" fill="none" stroke="currentColor" stroke-width="1.6"/><path d="M4,6 H32 V11 H4 Z M4,11 H12 V24 H4 Z" fill="currentColor" opacity=".3"/>',
      simple: '<path d="M4,6 H32 V24 H4 Z M4,12 H32 M4,18 H32 M13,6 V24 M23,6 V24" fill="none" stroke="currentColor" stroke-width="1.5"/>',
      striped: '<path d="M4,6 H32 V24 H4 Z" fill="none" stroke="currentColor"/><path d="M4,12 H32 V18 H4 Z" fill="currentColor" opacity=".28"/>',
      lined: '<path d="M6,8 H30 M6,14 H30 M6,20 H30" stroke="currentColor" stroke-width="1.7"/>',
      boxed: '<rect x="4" y="6" width="28" height="18" fill="none" stroke="currentColor" stroke-width="2.4"/><path d="M4,12 H32 M16,6 V24" stroke="currentColor"/>',
      exam: '<path d="M4,6 H32 V24 H4 Z M10,6 V24 M4,12 H32 M4,18 H32" fill="none" stroke="currentColor"/><path d="M4,6 H10 V24 H4 Z" fill="currentColor" opacity=".3"/>',
      matrix: '<path d="M8,6 V24 M28,6 V24" stroke="currentColor" stroke-width="2"/><path d="M12,10 H16 M20,10 H24 M12,20 H16 M20,20 H24" stroke="currentColor"/>',
      truth: '<path d="M4,6 H32 V24 H4 Z M4,12 H32 M16,6 V24" fill="none" stroke="currentColor"/><path d="M4,6 H32 V12 H4 Z" fill="currentColor" opacity=".35"/>',
      freq: '<path d="M4,6 H32 V24 H4 Z M4,18 H32 M15,6 V24" fill="none" stroke="currentColor"/><path d="M4,18 H32 V24 H4 Z" fill="currentColor" opacity=".28"/>',
      check: '<rect x="5" y="8" width="6" height="6" fill="none" stroke="currentColor"/><path d="M14,11 H30 M5,18 H11 M14,21 H28" stroke="currentColor"/>',
      color: '<rect x="5" y="7" width="7" height="16" fill="currentColor" opacity=".25"/><rect x="14" y="7" width="7" height="16" fill="currentColor" opacity=".45"/><rect x="23" y="7" width="7" height="16" fill="currentColor" opacity=".7"/>',
      account: '<path d="M6,8 H30 M6,14 H30 M6,22 H30" stroke="currentColor"/><path d="M6,20 H30" stroke="currentColor" stroke-width="2.4"/>',
      round: '<rect x="4" y="6" width="28" height="18" rx="5" fill="none" stroke="currentColor"/><path d="M4,12 H32" stroke="currentColor"/>',
      grid: '<path d="M5,6 H31 V24 H5 Z M5,12 H31 M5,18 H31 M14,6 V24 M23,6 V24" fill="none" stroke="currentColor"/>',
      note: '<rect x="5" y="6" width="26" height="18" fill="currentColor" opacity=".2" stroke="currentColor"/><path d="M5,11 H31" stroke="currentColor"/>',
      blue: '<path d="M4,6 H32 V24 H4 Z M4,11 H32 M4,17 H32" fill="none" stroke="currentColor"/><path d="M4,6 H32 V11 H4 Z" fill="currentColor" opacity=".4"/>',
      compact: '<path d="M5,8 H31 V22 H5 Z M5,13 H31 M5,18 H31 M14,8 V22 M23,8 V22" fill="none" stroke="currentColor" stroke-width="1.2"/>'
    };
    return '<svg viewBox="0 0 36 30" aria-hidden="true">' + (m[id] || m.header) + '</svg>';
  }

  var state = def('header');
  var replaceEl = null;
  function $(id) { return document.getElementById(id); }

  function rows() { return (state.C || []).length || 1; }
  function cols() { return ((state.C && state.C[0]) || []).length || 1; }

  function readGrid() {
    var box = $('tbGrid');
    if (!box) return;
    box.querySelectorAll('input[data-r]').forEach(function (inp) {
      var r = +inp.getAttribute('data-r'), c = +inp.getAttribute('data-c');
      if (!state.C[r]) state.C[r] = [];
      state.C[r][c] = inp.value;
    });
    var tit = $('tbTitle');
    if (tit) {
      if (!state.X) state.X = {};
      state.X.title = tit.value;
    }
  }
  function paint() {
    var g = $('tbGrid');
    if (g) g.innerHTML = htmlOf(state, true);
    var rs = $('tbRows'), cs = $('tbCols');
    if (rs) rs.value = rows();
    if (cs) cs.value = cols();
    var tit = $('tbTitle');
    if (tit && document.activeElement !== tit) tit.value = (state.X && state.X.title) || '';
    if (g) {
      g.querySelectorAll('input[data-r]').forEach(function (inp) {
        inp.addEventListener('input', function () {
          var r = +inp.getAttribute('data-r'), c = +inp.getAttribute('data-c');
          if (!state.C[r]) state.C[r] = [];
          state.C[r][c] = inp.value;
        });
      });
    }
  }
  function setSize(r, c) {
    readGrid();
    r = clamp(r, MIN_R, MAX_R);
    c = clamp(c, MIN_C, MAX_C);
    state.C = resizeGrid(state.C, r, c);
    paint();
  }
  function setType(id) {
    readGrid();
    var prevC = state.C, prevX = state.X || {};
    var keep = prevC && prevC.length && prevC.some(function (row) {
      return (row || []).some(function (v) { return String(v || '').trim(); });
    });
    state.t = id;
    state.k = 't';
    if (!keep) {
      var d = def(id);
      state.C = d.C;
    }
    if (!state.X) state.X = {};
    state.X.title = prevX.title || '';
    document.querySelectorAll('#tbShapes .gf-shape').forEach(function (b) {
      b.classList.toggle('on', b.getAttribute('data-t') === id);
    });
    paint();
    try {
      var on = document.querySelector('#tbShapes .gf-shape.on');
      if (on && on.scrollIntoView) on.scrollIntoView({ block: 'nearest' });
    } catch (e) {}
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
    readGrid();
    state.k = 't';
    if (replaceEl && replaceEl.classList && replaceEl.classList.contains('qmf-fig')) {
      replaceEl.dataset.fig = JSON.stringify(state);
      replaceEl.setAttribute('data-fig', JSON.stringify(state));
      replaceEl.className = 'qmf-fig qmf-tab';
      replaceEl.innerHTML = htmlOf(state, false);
      var t = targetTextarea();
      try { if (t && window.QMF) QMF.syncFromSurface(t); } catch (e) {}
      try { if (t && window.__examBuilderUpdateQuestionText) window.__examBuilderUpdateQuestionText(t); } catch (e) {}
      try { if (typeof renderPreview === 'function') renderPreview(); } catch (e) {}
    } else insertToken(tokenOf(state));
    close();
  }
  function ensureModal() {
    if ($('tbOverlay')) return;
    var ov = document.createElement('div');
    ov.id = 'tbOverlay';
    ov.className = 'gf-overlay';
    ov.innerHTML =
      '<div class="gf-modal" role="dialog" aria-label="درج جدول">' +
        '<div class="gf-head"><h3>درج جدول</h3><button type="button" class="gf-x" id="tbClose">×</button></div>' +
        '<div class="gf-split">' +
          '<aside class="gf-types"><div class="gf-types-h">نوع جدول</div><div class="gf-shapes" id="tbShapes"></div></aside>' +
          '<section class="gf-body">' +
            '<div class="tb-size">' +
              '<div class="tb-step"><span>سطر</span><button type="button" id="tbRowMinus">−</button><input id="tbRows" type="number" min="1" max="15"><button type="button" id="tbRowPlus">+</button></div>' +
              '<div class="tb-step"><span>ستون</span><button type="button" id="tbColMinus">−</button><input id="tbCols" type="number" min="1" max="10"><button type="button" id="tbColPlus">+</button></div>' +
            '</div>' +
            '<label class="field"><span>عنوان جدول</span><input id="tbTitle" class="input" placeholder="اختیاری"></label>' +
            '<div id="tbGrid" class="tb-edit"></div>' +
          '</section>' +
        '</div>' +
        '<div class="gf-foot"><button type="button" class="gf-btn ghost" id="tbCancel">انصراف</button>' +
        '<button type="button" class="gf-btn ok" id="tbApply">درج در سؤال</button></div>' +
      '</div>';
    document.body.appendChild(ov);
    TYPES.forEach(function (s) {
      var b = document.createElement('button');
      b.type = 'button'; b.className = 'gf-shape'; b.setAttribute('data-t', s.id);
      b.innerHTML = iconOf(s.id) + '<span>' + s.name + '</span>';
      b.addEventListener('click', function () { setType(s.id); });
      $('tbShapes').appendChild(b);
    });
    ov.addEventListener('click', function (e) { if (e.target === ov) close(); });
    $('tbClose').onclick = close;
    $('tbCancel').onclick = close;
    $('tbApply').onclick = apply;
    $('tbRowMinus').onclick = function () { setSize(rows() - 1, cols()); };
    $('tbRowPlus').onclick = function () { setSize(rows() + 1, cols()); };
    $('tbColMinus').onclick = function () { setSize(rows(), cols() - 1); };
    $('tbColPlus').onclick = function () { setSize(rows(), cols() + 1); };
    $('tbRows').addEventListener('change', function () { setSize(this.value, cols()); });
    $('tbCols').addEventListener('change', function () { setSize(rows(), this.value); });
    $('tbTitle').addEventListener('input', function () {
      if (!state.X) state.X = {};
      state.X.title = this.value;
      var cap = document.querySelector('#tbGrid .tbx-cap');
      if (this.value) {
        if (!cap) {
          cap = document.createElement('div');
          cap.className = 'tbx-cap';
          var host = document.querySelector('#tbGrid .tbx');
          if (host) host.insertBefore(cap, host.firstChild);
        }
        if (cap) cap.textContent = this.value;
      } else if (cap && cap.parentNode) cap.parentNode.removeChild(cap);
    });
  }
  function open(spec, el) {
    replaceEl = el || null;
    try { state = spec ? JSON.parse(JSON.stringify(spec)) : def('header'); }
    catch (e) { state = def('header'); }
    state.k = 't';
    if (!state.t) state.t = 'header';
    if (!state.C) state.C = sample(state.t, 4, 4);
    if (!state.X) state.X = { title: '' };
    ensureModal();
    $('tbOverlay').classList.add('open');
    var ok = $('tbApply');
    if (ok) ok.textContent = replaceEl ? 'اعمال تغییرات' : 'درج در سؤال';
    setType(state.t);
  }
  function openFromEl(fig) {
    if (!fig) return false;
    var spec = {};
    try { spec = JSON.parse(fig.getAttribute('data-fig') || fig.dataset.fig || '{}'); }
    catch (e) { spec = def('header'); }
    open(spec, fig);
    return true;
  }
  function close() {
    var ov = $('tbOverlay');
    if (ov) ov.classList.remove('open');
    replaceEl = null;
  }
  function make(raw) {
    var el = document.createElement('span');
    el.className = 'qmf-fig qmf-tab';
    el.contentEditable = 'false';
    el.setAttribute('data-fig', raw);
    el.dataset.fig = raw;
    el.setAttribute('role', 'img');
    el.setAttribute('aria-label', 'جدول');
    el.title = 'برای ویرایش دوبار کلیک کنید';
    try { el.innerHTML = htmlOf(JSON.parse(raw), false); }
    catch (e) { el.textContent = '[جدول]'; }
    return el;
  }
  function bind() {
    var btn = $('openTableEditor');
    if (btn && !btn._tbBound) {
      btn._tbBound = true;
      btn.addEventListener('click', function (e) { e.preventDefault(); open(null, null); });
    }
  }

  window.TableFig = { html: htmlOf, make: make, open: open, openFromEl: openFromEl, close: close };

  if (window.GeoFig) {
    var oldMake = window.GeoFig.make;
    var oldOpen = window.GeoFig.openFromEl;
    window.GeoFig.make = function (raw) {
      try {
        var s = JSON.parse(raw);
        if (s && s.k === 't') return make(raw);
      } catch (e) {}
      return oldMake ? oldMake(raw) : make(raw);
    };
    window.GeoFig.openFromEl = function (fig) {
      try {
        var s = JSON.parse(fig.getAttribute('data-fig') || fig.dataset.fig || '{}');
        if (s && s.k === 't') return openFromEl(fig);
      } catch (e) {}
      return oldOpen ? oldOpen(fig) : openFromEl(fig);
    };
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', bind);
  else bind();
})();
