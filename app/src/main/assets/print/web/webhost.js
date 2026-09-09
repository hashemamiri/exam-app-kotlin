/* ============================================================================
   V125 — لایهٔ میزبانِ برنامه برای موتورِ پیش‌نمایش/چاپِ «آزمون‌ساز v20».
   هیچ‌کدام از فایل‌های web/*.js و web/*.css تغییر نکرده‌اند (به‌جز یک خطِ
   loadSample در mainscript.js که فقط خارج از برنامه اجرا می‌شود). این فایل:
     ۱) دادهٔ برنامه (setExamData) را به مدلِ وب می‌ریزد: آرایهٔ سراسریِ
        `questions`، فیلدهای سربرگ (h*_ / f_*)، و opt_* (تنظیمات صفحهٔ PGS)؛
     ۲) همان renderPreview / openPreviewWindow (بینندهٔ PGS) را صدا می‌زند؛
     ۳) چاپ را به‌جای window.print/iframe (که در WebView کار نمی‌کند) به پلِ
        بومی ExamPrintBridge.print(mode) می‌رساند — با همان مسیرِ رسمیِ PGS
        برای چاپِ سیستم: رویدادِ beforeprint → body.pgs-fallback + #pgsPrintRoot؛
     ۴) همان API قبلیِ برنامه را نگه می‌دارد (window.ExamPrintRenderer)؛
     ۵) (V126) تغییراتِ پنلِ «تنظیمات صفحه»ی وب را به برنامه برمی‌گرداند.
   ============================================================================ */
(function () {
  'use strict';
  var HOST_KEYS = ['paper','orient','customW','customH','mT','mB','mR','mL','border','pageNumbers','repeatHeader','font','spacing','showScores'];
  var OPT_OF = {paper:'opt_paper', orient:'opt_orientation', customW:'opt_customW', customH:'opt_customH',
    mT:'opt_mT', mB:'opt_mB', mR:'opt_mR', mL:'opt_mL', border:'opt_pageBorder', pageNumbers:'opt_showPageNumbers',
    repeatHeader:'opt_repeatHeader', font:'opt_baseFont', spacing:'opt_questionSpacing', showScores:'opt_showScores'};
  var PAPERS = {a4:1, a5:1, b5:1, letter:1, f4:1, legal:1, custom:1};

  function $(id) { return document.getElementById(id); }
  function text(v) { return v == null ? '' : String(v); }
  function num(v, d) { var n = parseFloat(v); return isFinite(n) ? n : d; }
  function asObject(v) { return v && typeof v === 'object' && !Array.isArray(v) ? v : {}; }
  function parseMaybe(v) { if (typeof v !== 'string') return v; try { return JSON.parse(v); } catch (e) { return {}; } }
  function bridge() { return window.ExamPrintBridge || null; }
  function callBridge(name) {
    try {
      var target = bridge();
      if (target && typeof target[name] === 'function') return target[name].apply(target, Array.prototype.slice.call(arguments, 1));
    } catch (ignored) {}
    return '';
  }
  function toast(message) {
    if (!callBridge('toast', message) && typeof window.notify === 'function') { try { window.notify(message); } catch (e) {} }
  }

  /* ---------------------------------------------------------------- DOM پایهٔ PGS */
  var setupBound = false;
  function ensurePgs() {
    /* pgsToggleSetup(false) همان ensureBaseDom را صدا می‌زند و پنل را بسته نگه می‌دارد. */
    try { if (typeof window.pgsToggleSetup === 'function') window.pgsToggleSetup(false); } catch (e) {}
    if ($('opt_paper') && !setupBound) { setupBound = true; bindSetupReport(); try { syncNativeSelects(); } catch (e) {} }
    return !!$('opt_paper');
  }
  /* V126 — پنلِ 📐 خودِ موتورِ وب تنها جای تنظیمِ صفحه است؛ وب آن را در حافظهٔ مرورگر نگه می‌داشت،
     اینجا هر تغییر به برنامه برمی‌گردد (ExamPrintBridge.pageSetupChanged) تا روی دستگاه بماند و
     پنلِ چاپِ اندروید هم‌اندازهٔ همان کاغذ باز شود. */
  function reportPageSetup() { callBridge('pageSetupChanged', JSON.stringify(readPageSetup())); }
  function bindSetupReport() {
    var panel = $('pgsPageSetup'); if (!panel) return;
    panel.addEventListener('change', reportPageSetup);
    panel.addEventListener('input', reportPageSetup);
  }

  /* ---------------------------------------------------------------- تنظیمات صفحه */
  function readPageSetup() {
    var out = {};
    HOST_KEYS.forEach(function (k) {
      var el = $(OPT_OF[k]); if (!el) return;
      out[k] = el.type === 'checkbox' ? !!el.checked : el.value;
    });
    if (!PAPERS[out.paper]) out.paper = 'a4';
    out.orient = out.orient === 'landscape' ? 'landscape' : 'portrait';
    ['customW','customH','mT','mB','mR','mL','font'].forEach(function (k) { out[k] = num(out[k], k === 'customW' ? 210 : (k === 'customH' ? 297 : (k === 'font' ? 10.5 : 10))); });
    if (['compact','normal','open'].indexOf(out.spacing) < 0) out.spacing = 'normal';
    return out;
  }
  function writePageSetup(value) {
    var src = asObject(parseMaybe(value));
    if (!ensurePgs()) return false;
    HOST_KEYS.forEach(function (k) {
      if (src[k] === undefined || src[k] === null) return;
      var el = $(OPT_OF[k]); if (!el) return;
      if (el.type === 'checkbox') el.checked = src[k] === true || src[k] === 'true';
      else el.value = String(src[k]);
    });
    var preset = $('opt_marginPreset'); if (preset) preset.value = 'custom';
    var paper = $('opt_paper'), isCustom = paper && paper.value === 'custom';
    ['opt_customW','opt_customH'].forEach(function (id) { var el = $(id); if (el) el.disabled = !isCustom; });
    try { if (window.__pgsSyncCustomState) window.__pgsSyncCustomState(); } catch (e) {}
    try { syncNativeSelects(); } catch (e) {}
    return true;
  }

  /* ================================================================ V131 — انتخابگرهای بومیِ پنلِ 📐
     select‌های «اندازهٔ کاغذ / جهت / حاشیه‌ها / فاصلهٔ بین سؤالات» پنهان و به‌جایشان دکمه‌ای می‌نشیند که
     پنجرهٔ بومی (ExamPrintBridge.pickOption) را باز می‌کند؛ نتیجه با ExamPrintRenderer.setOption برمی‌گردد
     و روی همان select با رویدادِ change اعمال می‌شود (saveSetup/pageSetupChanged/preset حاشیه مثل قبل). */
  var NATIVE_SELECTS = ['opt_paper', 'opt_orientation', 'opt_marginPreset', 'opt_questionSpacing'];
  function selectOptions(sel) {
    return Array.prototype.map.call(sel.options, function (o) { return {value: o.value, label: o.textContent}; });
  }
  function selectLabel(sel) { var o = sel.options[sel.selectedIndex]; return o ? o.textContent : ''; }
  function syncNativeSelects() {
    NATIVE_SELECTS.forEach(function (id) {
      var sel = $(id); if (!sel) return;
      var btn = sel.__hostBtn;
      if (!btn) {
        btn = document.createElement('button'); btn.type = 'button'; btn.className = 'host-sel';
        btn.setAttribute('data-for', id);
        sel.parentNode.insertBefore(btn, sel.nextSibling);
        sel.classList.add('host-sel-hidden');
        sel.__hostBtn = btn;
        btn.addEventListener('click', function () {
          var b = bridge();
          if (b && typeof b.pickOption === 'function') {
            b.pickOption(id, sel.value, JSON.stringify(selectOptions(sel)));
          } else {
            /* مرورگر: به select اصلی برگرد */
            sel.classList.remove('host-sel-hidden'); btn.style.display = 'none';
          }
        });
        sel.addEventListener('change', function () { btn.textContent = selectLabel(sel); });
      }
      btn.textContent = selectLabel(sel);
    });
  }
  function setOption(id, value) {
    var sel = $(id); if (!sel || NATIVE_SELECTS.indexOf(id) < 0) return 'missing';
    value = text(value);
    var ok = Array.prototype.some.call(sel.options, function (o) { return o.value === value; });
    if (!ok) return 'badvalue';
    if (sel.value !== value) {
      sel.value = value;
      try { sel.dispatchEvent(new Event('change', {bubbles: true})); } catch (e) {}
    } else if (sel.__hostBtn) sel.__hostBtn.textContent = selectLabel(sel);
    return 'ok';
  }

  /* ================================================================ V131 — محو/نمایشِ نوارِ قالب‌بندی
     - با بازشدنِ پنلِ 📐 نوار محو و با بستنش برمی‌گردد.
     - در پیش‌نمایش: اسکرولِ تند/زیاد به بالا (پیمایش به پایینِ برگه) نوار را محو و اسکرول به پایین آن را
       نمایان می‌کند؛ پیش‌فرض باز. */
  var fmtHiddenBySetup = false;
  function setFmtHidden(hidden) {
    var bar = $('hostFmt'); if (!bar) return;
    bar.classList.toggle('hf-hidden', !!hidden);
    if (typeof bar.__syncTop === 'function') { bar.__syncTop(); setTimeout(bar.__syncTop, 260); }
  }
  function hookSetupToggle() {
    var base = window.pgsToggleSetup;
    if (typeof base !== 'function' || base.__appHostFmt) return false;
    var wrapped = function () {
      var r = base.apply(this, arguments);
      try {
        var p = $('pgsPageSetup'), open = !!(p && p.style.display !== 'none' && p.style.display);
        fmtHiddenBySetup = open; setFmtHidden(open);
        if (open) syncNativeSelects();
      } catch (e) {}
      return r;
    };
    wrapped.__appHostFmt = true;
    window.pgsToggleSetup = wrapped;
    return true;
  }
  function bindFmtScrollHide(bar) {
    var wrap = $('pgsCanvasWrap'); if (!wrap || wrap.__hostFmtScroll) return;
    wrap.__hostFmtScroll = true;
    var lastTop = wrap.scrollTop, lastT = Date.now(), acc = 0;
    wrap.addEventListener('scroll', function () {
      if (fmtHiddenBySetup) return;
      var top = wrap.scrollTop, now = Date.now(), dy = top - lastTop, dt = Math.max(1, now - lastT);
      lastTop = top; lastT = now;
      if (dy > 0) { acc = acc < 0 ? dy : acc + dy; if (acc > 160 || dy / dt > 1.6) setFmtHidden(true); }
      else if (dy < 0) { acc = acc > 0 ? dy : acc + dy; if (acc < -40 || top <= 0) setFmtHidden(false); }
    }, {passive: true});
  }

  /* ---------------------------------------------------------------- نگاشتِ سؤال‌ها */
  function optionOf(o) {
    o = asObject(o);
    return {text: text(o.text), correct: o.correct === true};
  }
  function toWebQuestion(src, index) {
    src = asObject(src);
    var type = text(src.type) || 'long';
    var q = {
      id: src.id != null && src.id !== '' ? src.id : index + 1,
      type: type,
      text: text(src.text),
      score: text(src.score),
      sepExtraPx: Math.max(0, Math.min(600, num(src.sepExtraPx, 0))),
      figLayouts: asObject(parseMaybe(src.figLayoutsJson !== undefined ? src.figLayoutsJson : src.figLayouts)),
      /* V128 — بازه‌های قالب‌بندیِ متنِ سؤال (نوارِ B/I/U/رنگ/اندازه/فونتِ پیش‌نمایش؛ در بیلدرِ بومی ذخیره می‌شوند). */
      __spans: cleanSpans(src.textSpans, text(src.text).length),
      /* V133 — ترازِ پاراگراف‌ها (راست/وسط/چپ/دوطرفه) که از نوارِ پیش‌نمایش انتخاب می‌شود و در آزمون ذخیره می‌ماند. */
      __aligns: cleanAligns(src.alignSpans, text(src.text).length),
      /* قالب‌بندیِ سطحِ سؤال از بیلدرِ بومی؛ در لایهٔ میزبان روی .q-rich-content اعمال می‌شود. */
      __style: {bold: src.bold === true, italic: src.italic === true, fontFamily: text(src.fontFamily), fontSizeSp: num(src.fontSizeSp, 0), textAlign: text(src.textAlign)}
    };
    if (type === 'multiple' || type === 'truefalse') {
      q.options = (Array.isArray(src.options) ? src.options : []).map(optionOf);
      if (type === 'truefalse' && q.options.length !== 2) q.options = [{text: 'صحیح', correct: false}, {text: 'غلط', correct: false}];
      q.optionsLayout = ['1row','2rows','4rows'].indexOf(text(src.optionsLayout)) >= 0 ? text(src.optionsLayout) : '2rows';
    } else if (type === 'matching') {
      q.pairs = (Array.isArray(src.pairs) ? src.pairs : []).map(function (p) { p = asObject(p); return {left: text(p.left), right: text(p.right)}; });
    } else {
      /* long / fill / numeric — فضای پاسخ؛ وب فقط «خط‌دار» و «ساده» دارد (شطرنجی → خط‌دار). */
      q.answerLines = Math.max(1, Math.min(30, Math.round(num(src.answerLines, type === 'long' ? 6 : (type === 'fill' ? 2 : 1)))));
      q.answerStyle = text(src.answerStyle) === 'plain' ? 'plain' : 'lined';
      q.answerLineHeightCm = Math.max(0.5, Math.min(1.6, num(src.answerLineSpacingCm !== undefined ? src.answerLineSpacingCm : src.answerLineHeightCm, 0.75)));
      if (type === 'numeric') q.answer = text(src.answer);
    }
    return q;
  }

  /* ---------------------------------------------------------------- فیلدهای سربرگ */
  function applyFields(fields) {
    fields = asObject(fields);
    Object.keys(fields).forEach(function (id) {
      if (!/^(f_|h[2-7]_|opt_footerText$)/.test(id)) return;
      var el = $(id); if (!el) return;
      el.value = text(fields[id]);
    });
    var tpl = $('f_headerTemplate');
    if (tpl && !fields.f_headerTemplate) tpl.value = 'classic';
  }

  /* ---------------------------------------------------------------- استایلِ سطحِ سؤال (لایهٔ میزبان) */
  function applyQuestionStyles() {
    var area = $('previewArea'); if (!area) return;
    Array.prototype.forEach.call(area.querySelectorAll('.question-print-row[data-qid]'), function (row) {
      var q = questions.find(function (x) { return String(x.id) === String(row.dataset.qid); });
      var box = row.querySelector('.q-rich-content');
      if (!q || !box || !q.__style) return;
      var s = q.__style;
      if (s.bold) box.style.fontWeight = '700';
      if (s.italic) box.style.fontStyle = 'italic';
      if (s.fontFamily && s.fontFamily !== 'default') box.style.fontFamily = s.fontFamily === 'serif' ? 'serif' : ('"' + s.fontFamily + '", Vazirmatn, Tahoma, sans-serif');
      if (s.fontSizeSp > 0) box.style.fontSize = Math.max(8, Math.min(40, s.fontSizeSp)) + 'px';
      if (['right','left','center','justify'].indexOf(s.textAlign) >= 0) box.style.textAlign = s.textAlign;
    });
  }
  function wrapRenderPreviewOnce() {
    if (typeof window.renderPreview !== 'function' || window.renderPreview.__appHostStyled) return;
    var base = window.renderPreview;
    var wrapped = function () { var r = base.apply(this, arguments); try { applyQuestionStyles(); } catch (e) {} return r; };
    wrapped.__appHostStyled = true; wrapped.__pgs = base.__pgs; wrapped.__qimg = base.__qimg;
    window.renderPreview = wrapped;
  }

  /* ---------------------------------------------------------------- ورودیِ داده */
  var lastData = null;
  function setExamData(data) {
    data = asObject(data);
    if (typeof window.renderPreview !== 'function' || !window.renderPreview.__pgs) {
      /* موتورِ PGS در DOMContentLoaded نصب می‌شود؛ اگر زودتر رسیدیم کمی صبر می‌کنیم. */
      lastData = data; setTimeout(function () { if (lastData === data) setExamData(data); }, 40); return 'wait';
    }
    lastData = null;
    wrapRenderPreviewOnce();
    if (data.pageSetup !== undefined) writePageSetup(data.pageSetup);
    var fields = asObject(data.fields);
    if (!text(fields.opt_footerText).trim() && text(data.footerNote).trim()) fields.opt_footerText = text(data.footerNote).trim();
    applyFields(fields);
    questions = data.reset === true ? [] : (Array.isArray(data.questions) ? data.questions.map(toWebQuestion) : []);
    qIdCounter = questions.reduce(function (m, q) { return Math.max(m, num(q.id, 0)); }, 0);
    try { updateHeaderSettingsVisibility(); } catch (e) { try { window.renderPreview(); } catch (e2) {} }
    return 'ok';
  }

  /* ---------------------------------------------------------------- چاپ از راهِ پلِ بومی */
  function waitForReady(done) {
    var finished = false;
    function fin() { if (!finished) { finished = true; done(); } }
    var area = $('previewArea');
    var images = area ? Array.prototype.slice.call(area.querySelectorAll('img')) : [];
    var pending = images.length;
    function afterImages() {
      var fontsReady = document.fonts && document.fonts.ready ? document.fonts.ready : Promise.resolve();
      /* موج‌های صفحه‌بندیِ PGS (۱۶۰/۵۵۰ms) پس از renderPreview باید بنشینند. */
      fontsReady.then(function () { setTimeout(fin, 700); }, function () { setTimeout(fin, 700); });
    }
    if (!pending) afterImages();
    else images.forEach(function (im) {
      if (im.complete) { if (--pending <= 0) afterImages(); }
      else im.addEventListener('load', function () { if (--pending <= 0) afterImages(); }, {once: true}), im.addEventListener('error', function () { if (--pending <= 0) afterImages(); }, {once: true});
    });
    setTimeout(fin, 4000);
  }
  var printing = false, printRootReady = false;
  /* WebView/PrintManager (و Chrome) هنگامِ ساختِ سند، خودشان هم beforeprint می‌فرستند؛ اگر
     #pgsPrintRoot از پیش ساخته شده، اجرای دوبارهٔ paginate در میانهٔ چاپ (با رسانهٔ print)
     خروجی را خراب می‌کرد. شنوندهٔ capture پیش از شنوندهٔ PGS اجرا می‌شود و آن را می‌بندد. */
  window.addEventListener('beforeprint', function (e) {
    if (printRootReady && !e.__appHost) { e.stopImmediatePropagation(); document.body.classList.add('pgs-fallback'); }
  }, true);
  window.addEventListener('afterprint', function (e) {
    if (printRootReady && !e.__appHost) { e.stopImmediatePropagation(); }
  }, true);
  /* V127 — بازهٔ صفحات و تعداد نسخه از دیالوگِ چاپِ خودِ وب (مثل نسخهٔ وب): پس از آنکه PGS
     #pgsPrintRoot را ساخت، فقط برگه‌های انتخابی می‌مانند و به تعدادِ نسخه تکرار می‌شوند. */
  function parseRange(str, total) {
    var set = {}, any = false;
    String(str || '').replace(/[۰-۹]/g, function (d) { return '۰۱۲۳۴۵۶۷۸۹'.indexOf(d); }).split(/[،,;]+/).forEach(function (part) {
      part = part.trim(); if (!part) return;
      var m = /^(\d+)\s*-\s*(\d+)$/.exec(part), a, b;
      if (m) { a = Math.max(1, Math.min(total, +m[1])); b = Math.max(1, Math.min(total, +m[2])); }
      else if (/^\d+$/.test(part)) { a = b = Math.max(1, Math.min(total, +part)); }
      else return;
      for (var i = Math.min(a, b); i <= Math.max(a, b); i++) { set[i] = 1; any = true; }
    });
    return any ? set : null;
  }
  function applyRangeAndCopies(opts) {
    var root = $('pgsPrintRoot'); if (!root) return;
    var sheets = Array.prototype.slice.call(root.querySelectorAll('.pgs-sheet'));
    /* بازه پس از ساختِ برگه‌های نهایی (نسخهٔ استاد ممکن است برگهٔ کلید اضافه داشته باشد) حل می‌شود. */
    var range = null;
    if (opts.rangeKind === 'range') range = parseRange(opts.rangeText, sheets.length);
    else if (opts.rangeKind === 'current') { range = {}; range[Math.max(1, Math.min(sheets.length, num(opts.current, 1)))] = 1; }
    if (range) sheets.forEach(function (el, i) { if (!range[i + 1]) el.remove(); });
    var copies = Math.max(1, Math.min(20, num(opts.copies, 1)));
    if (copies > 1) {
      var once = Array.prototype.slice.call(root.querySelectorAll('.pgs-sheet'));
      for (var c = 1; c < copies; c++) once.forEach(function (el) { root.appendChild(el.cloneNode(true)); });
    }
  }
  function requestPrint(mode, opts) {
    mode = mode === 'teacher' ? 'teacher' : 'student';
    opts = opts || {};
    if (printing) return 'busy';
    printing = true;
    try { printMode = mode; } catch (e) {}
    try { window.renderPreview(); } catch (e) {}
    waitForReady(function () {
      /* مسیرِ رسمیِ PGS برای چاپِ سیستم: beforeprint → body.pgs-fallback + بازسازیِ #pgsPrintRoot
         (برگه‌های صفحه‌بندی‌شده). PrintManagerِ اندروید همین سند را با @media print چاپ می‌کند. */
      printRootReady = false;
      try { var ev = new Event('beforeprint'); ev.__appHost = true; window.dispatchEvent(ev); } catch (e) {}
      try { applyRangeAndCopies(opts); } catch (e) {}
      printRootReady = true;
      printing = false;
      callBridge('print', mode);
    });
    return 'queued';
  }
  function restorePreview() {
    printing = false; printRootReady = false;
    try { var ev = new Event('afterprint'); ev.__appHost = true; window.dispatchEvent(ev); } catch (e) {}
    try { document.body.classList.remove('pgs-fallback'); } catch (e) {}
    try { printMode = 'student'; window.renderPreview(); } catch (e) {}
    return 'ok';
  }

  /* ---------------------------------------------------------------- پیش‌نمایش (بینندهٔ PGS) */
  var hintShown = false;
  function showPreview() {
    try { printMode = 'student'; } catch (e) {}
    try { window.openPreviewWindow(); } catch (e) { return 'err'; }
    try { ensureFmtBar(); } catch (e) {}
    if (!hintShown) { hintShown = true; setTimeout(function () { toast('برای قالب‌بندی، متنِ سؤال را انتخاب کنید؛ فاصلهٔ هر سؤال را با کشیدنِ دستگیرهٔ آبیِ زیرِ آن کم/زیاد کنید'); }, 900); }
    return 'ok';
  }

  /* ================================================================ V128 — قالب‌بندیِ متنِ انتخاب‌شده
     متنِ هر سؤال به تکه‌های <span class="txt" data-off data-q> با آفستِ متنِ اصلی رندر می‌شود تا انتخابِ
     کاربر (Selection) به بازهٔ [start,end) نگاشت شود؛ بازه‌ها در q.__spans نگه داشته و با snapshot به
     بیلدرِ بومی برمی‌گردند (همان مدلِ StyleSpan که V114 داشت). فرمول‌ها و شکل‌ها بدونِ تغییر رندر می‌شوند. */
  function cleanSpans(value, length) {
    if (!Array.isArray(value)) return [];
    return value.map(function (item) {
      item = asObject(item);
      var start = Math.max(0, Math.min(length, Math.floor(num(item.start, 0))));
      var end = Math.max(0, Math.min(length, Math.floor(num(item.end, 0))));
      var color = text(item.color); if (!/^#[0-9a-fA-F]{6}$/.test(color)) color = '';
      var size = Math.round(num(item.size, 0)); if (!(size >= 1 && size <= 100)) size = 0;
      var font = text(item.font); if (font === 'default') font = '';
      return {start: start, end: end, bold: item.bold === true, italic: item.italic === true, underline: item.underline === true, color: color, size: size, font: font};
    }).filter(function (item) { return item.end > item.start && (item.bold || item.italic || item.underline || item.color || item.size || item.font); });
  }
  var ALIGNS = ['right', 'center', 'left', 'justify'];
  function cleanAligns(value, length) {
    if (!Array.isArray(value)) return [];
    return value.map(function (item) {
      item = asObject(item);
      var start = Math.max(0, Math.min(length, Math.floor(num(item.start, 0))));
      var end = Math.max(0, Math.min(length, Math.floor(num(item.end, 0))));
      return {start: start, end: end, align: text(item.align)};
    }).filter(function (item) { return item.end > item.start && ALIGNS.indexOf(item.align) >= 0; }).sort(function (a, b) { return a.start - b.start; });
  }
  /* مرزِ پاراگرافِ دربرگیرندهٔ بازهٔ [s,e) در متن (پاراگراف = بینِ دو خطِ جدید) */
  function paragraphBounds(src, s, e) {
    var a = src.lastIndexOf('\n', Math.max(0, s - 1)) + 1;
    var b = src.indexOf('\n', Math.max(s, e - 1)); if (b < 0) b = src.length;
    if (e > s && src.charAt(e - 1) === '\n') { b = e - 1; }
    return [a, Math.max(a, b)];
  }
  function alignOfParagraph(q, ps, pe) {
    var hit = (q.__aligns || []).find(function (x) { return x.start < Math.max(pe, ps + 1) && x.end > ps; });
    return hit ? hit.align : '';
  }
  function setAlignRange(q, s, e, align) {
    var src = text(q.text), pb = paragraphBounds(src, s, e), a = pb[0], b = pb[1];
    var out = [];
    (q.__aligns || []).forEach(function (x) {
      if (x.end <= a || x.start >= b) { out.push(x); return; }
      if (x.start < a) out.push({start: x.start, end: a, align: x.align});
      if (x.end > b) out.push({start: b, end: x.end, align: x.align});
    });
    if (align) out.push({start: a, end: Math.max(b, a + 1), align: align});
    out = out.filter(function (x) { return x.end > x.start; }).sort(function (x, y) { return x.start - y.start; });
    var merged = [];
    out.forEach(function (x) { var l = merged[merged.length - 1]; if (l && x.start <= l.end && l.align === x.align) l.end = Math.max(l.end, x.end); else merged.push({start: x.start, end: x.end, align: x.align}); });
    return merged;
  }
  function fontCss(font) { return font === 'serif' ? 'serif' : ('"' + font + '", Vazirmatn, Tahoma, sans-serif'); }
  function styledTextHtml(piece, off, q) {
    var spans = (q && q.__spans) || [], bounds = [0, piece.length];
    spans.forEach(function (sp) { var l = Math.max(0, sp.start - off), r = Math.min(piece.length, sp.end - off); if (r > l) bounds.push(l, r); });
    bounds.sort(function (a, b) { return a - b; });
    var out = '', qid = q ? String(q.id) : '';
    for (var i = 0; i < bounds.length - 1; i++) {
      var from = bounds[i], to = bounds[i + 1];
      if (to <= from) continue;
      var st = '';
      var b = false, it = false, u = false, color = '', size = 0, font = '';
      spans.forEach(function (sp) {
        if (sp.start < off + to && sp.end > off + from) { b = b || sp.bold; it = it || sp.italic; u = u || sp.underline; if (sp.color) color = sp.color; if (sp.size) size = sp.size; if (sp.font) font = sp.font; }
      });
      if (b) st += 'font-weight:700;'; if (it) st += 'font-style:italic;'; if (u) st += 'text-decoration:underline;';
      if (color) st += 'color:' + color + ';'; if (size) st += 'font-size:' + size + 'px;'; if (font) st += 'font-family:' + fontCss(font).replace(/"/g, '\'') + ';';
      out += '<span class="txt" data-off="' + (off + from) + '" data-q="' + qid + '"' + (st ? ' style="' + st + '"' : '') + '>' + window.plainTextHtml(piece.slice(from, to)) + '</span>';
    }
    return out;
  }
  function styledPlainWithMath(seg, off, q) {
    var re = /\\\(([\s\S]*?)\\\)|\$\$([\s\S]*?)\$\$|\$([^$\n]+)\$/g, out = '', last = 0, m;
    while ((m = re.exec(seg))) {
      out += styledTextHtml(seg.slice(last, m.index), off + last, q);
      var tex = m[1] != null ? m[1] : (m[2] != null ? m[2] : m[3]);
      try { out += (typeof window.mathToHtml === 'function') ? window.mathToHtml(tex) : ('<span class="math-inline">' + window.renderMathTex(tex) + '</span>'); }
      catch (e) { out += '<span class="math-inline">' + window.renderMathTex(tex) + '</span>'; }
      last = re.lastIndex;
    }
    return out + styledTextHtml(seg.slice(last), off + last, q);
  }
  function installRichTextOverride() {
    var base = window.renderRichText;
    if (typeof base !== 'function' || base.__appHost) return;
    var typeMap = {FIG: 'figure', GRAPH: 'graph', TABLE: 'table', ANATOMY: 'anatomy', PERIODIC: 'periodic', PHYSICS: 'physics', CHEMISTRY: 'chemistry'};
    var wrapped = function (value, q) {
      if (!q) return base.apply(this, arguments);
      var src = String(value == null ? '' : value); if (!src) return '';
      var tokenRe = /%%FIG:([\s\S]*?)%%|\[\[(FIG|GRAPH|TABLE|ANATOMY|PERIODIC|PHYSICS|CHEMISTRY):([^\]|]*)(?:\|([^\]]*))?\]\]/g;
      var parts = [], last = 0, m, figIndex = 0;
      while ((m = tokenRe.exec(src))) {
        parts.push({from: last, to: m.index, html: null});
        var tokHtml = (m[1] != null) ? window.renderFigToken(m[1], q, figIndex++) : window.renderVisualTool(typeMap[m[2]], m[3] || '', m[4] || '');
        parts.push({from: m.index, to: tokenRe.lastIndex, html: tokHtml});
        last = tokenRe.lastIndex;
      }
      parts.push({from: last, to: src.length, html: null});
      var renderRange = function (a, b) {
        var h = '';
        parts.forEach(function (p) {
          var l = Math.max(a, p.from), r = Math.min(b, p.to);
          if (p.html != null) { if (p.from >= a && p.to <= b) h += p.html; return; }
          if (r > l) h += styledPlainWithMath(src.slice(l, r), l, q);
        });
        return h;
      };
      if (!(q.__aligns && q.__aligns.length)) return renderRange(0, src.length);
      /* V133 — با وجودِ تراز، هر پاراگراف در یک div با text-align خودش می‌نشیند (پاراگراف = بین دو خطِ جدید؛
         خطِ جدیدِ داخلِ توکن‌های شکل شمرده نمی‌شود). آفست‌های .txt دست‌نخورده می‌مانند. */
      var out = '', ps = 0;
      var inToken = function (i) { return parts.some(function (p) { return p.html != null && i >= p.from && i < p.to; }); };
      for (var i = 0; i <= src.length; i++) {
        if (i < src.length && (src.charAt(i) !== '\n' || inToken(i))) continue;
        var al = alignOfParagraph(q, ps, i), body = renderRange(ps, i);
        out += '<div class="q-para"' + (al ? ' style="text-align:' + al + '"' : '') + '>' + (body || '<br>') + '</div>';
        ps = i + 1;
      }
      return out;
    };
    wrapped.__appHost = true;
    window.renderRichText = wrapped;
  }

  /* --- نگاشتِ انتخاب به بازه --- */
  function posInTxt(txt, node, offset) {
    var pos = 0, kids = txt.childNodes;
    for (var i = 0; i < kids.length; i++) {
      var k = kids[i];
      if (node === txt && i === offset) return pos;
      if (k === node) return pos + (k.nodeType === 3 ? offset : 0);
      pos += k.nodeType === 3 ? k.textContent.length : 1;
    }
    return pos;
  }
  function offsetOf(rich, node, offset) {
    if (node.nodeType === 1 && node.childNodes.length && !(node.closest && node.closest('.txt'))) {
      if (offset >= node.childNodes.length) { node = node.childNodes[node.childNodes.length - 1]; offset = node.nodeType === 3 ? node.textContent.length : node.childNodes.length; }
      else { node = node.childNodes[offset]; offset = 0; }
    }
    var el = node.nodeType === 1 ? node : node.parentNode;
    var txt = el && el.closest ? el.closest('.txt') : null;
    if (txt) return Number(txt.dataset.off) + posInTxt(txt, node, offset);
    var all = rich.querySelectorAll('.txt'), best = null;
    for (var i = 0; i < all.length; i++) { if (node.compareDocumentPosition(all[i]) & Node.DOCUMENT_POSITION_PRECEDING) best = all[i]; }
    return best ? Number(best.dataset.off) + best.textContent.length : 0;
  }
  var lastSel = null;
  function selectionRange() {
    var sel = window.getSelection && window.getSelection();
    if (!sel || sel.rangeCount === 0 || sel.isCollapsed) return null;
    var range = sel.getRangeAt(0);
    var n = range.commonAncestorContainer; if (n.nodeType !== 1) n = n.parentNode;
    var rich = n && n.closest ? n.closest('.q-rich-content') : null;
    if (!rich || !rich.contains(range.startContainer) || !rich.contains(range.endContainer)) return null;
    var first = rich.querySelector('.txt[data-q]'); if (!first) return null;
    var q = (questions || []).find(function (x) { return String(x.id) === first.dataset.q; }); if (!q) return null;
    var a = offsetOf(rich, range.startContainer, range.startOffset), b = offsetOf(rich, range.endContainer, range.endOffset);
    var s = Math.max(0, Math.min(a, b)), e = Math.min(text(q.text).length, Math.max(a, b));
    return e > s ? {question: q, start: s, end: e} : null;
  }
  function rememberSelection() { var r = selectionRange(); if (r) lastSel = r; }
  function selCovers(sel, key) {
    if (sel.all) { var qs = questions || []; return qs.length > 0 && qs.every(function (q0) { return coversAxis(q0.__spans || [], 0, text(q0.text).length, key); }); }
    return coversAxis(sel.question.__spans || [], sel.start, sel.end, key);
  }
  function coversAxis(spans, s, e, key) {
    var cursor = s, active = spans.filter(function (x) { return x[key]; }).sort(function (x, y) { return x.start - y.start; });
    for (var i = 0; i < active.length; i++) { if (active[i].start > cursor) return false; cursor = Math.max(cursor, active[i].end); if (cursor >= e) return true; }
    return cursor >= e;
  }
  function applyStyle(sel, patch) {
    /* V130 — «همه»: قالب روی متنِ کاملِ همهٔ سؤال‌ها یک‌جا اعمال می‌شود */
    if (sel.all) {
      (questions || []).forEach(function (q0) { q0.__spans = patchSpans(q0, 0, text(q0.text).length, patch); });
      lastSel = {all: true, sticky: true};
      rerenderKeepScroll();
      updateFmtBar(lastSel);
      return;
    }
    var q = sel.question, s = sel.start, e = sel.end;
    q.__spans = patchSpans(q, s, e, patch);
    /* V129 — انتخاب تا وقتی کاربر جای دیگری را لمس نکند «پابرجا» می‌ماند: بازه نگه داشته می‌شود و پس از
       باز-رندر، همان بازه دوباره هایلایت (کلاس hf-selected) و Selection مرورگر هم روی آن بازسازی می‌شود تا
       بتوان چند ابزار را پشتِ‌سرِهم روی همان متن زد. */
    lastSel = {question: q, start: s, end: e, sticky: true};
    rerenderKeepScroll();
    updateFmtBar(lastSel);
  }
  function rerenderKeepScroll() {
    var wrap = $('pgsCanvasWrap'), top = wrap ? wrap.scrollTop : 0;
    try { window.renderPreview(); } catch (e3) {}
    [80, 300, 700, 1450].forEach(function (t) { setTimeout(function () { var w = $('pgsCanvasWrap'); if (w) w.scrollTop = top; restoreSelection(); }, t); });
  }
  function patchSpans(q, s, e, patch) {
    var out = [];
    var blank = {bold: false, italic: false, underline: false, color: '', size: 0, font: ''};
    (q.__spans || []).forEach(function (span) {
      if (span.end <= s || span.start >= e) { out.push(span); return; }
      if (span.start < s) out.push(Object.assign({}, span, {end: s}));
      if (span.end > e) out.push(Object.assign({}, span, {start: e}));
      out.push(Object.assign({}, span, {start: Math.max(span.start, s), end: Math.min(span.end, e)}, patch));
    });
    var covered = out.filter(function (x) { return x.start >= s && x.end <= e; }).sort(function (x, y) { return x.start - y.start; }), cursor = s;
    covered.forEach(function (x) { if (x.start > cursor) out.push(Object.assign({start: cursor, end: x.start}, blank, patch)); cursor = Math.max(cursor, x.end); });
    if (cursor < e) out.push(Object.assign({start: cursor, end: e}, blank, patch));
    out = out.filter(function (x) { return x.end > x.start && (x.bold || x.italic || x.underline || x.color || x.size || x.font); }).sort(function (x, y) { return x.start - y.start; });
    var merged = [];
    out.forEach(function (x) {
      var l = merged[merged.length - 1];
      if (l && x.start <= l.end && l.bold === x.bold && l.italic === x.italic && l.underline === x.underline && l.color === x.color && l.size === x.size && l.font === x.font) l.end = Math.max(l.end, x.end);
      else merged.push(Object.assign({}, x));
    });
    return merged;
  }
  function clearSticky() {
    if (!lastSel) return;
    lastSel = null;
    Array.prototype.forEach.call(document.querySelectorAll('.hf-selected'), function (el) { el.classList.remove('hf-selected'); });
    updateFmtBar(null);
  }
  function restoreSelection() {
    var sel = lastSel; if (!sel || !sel.sticky) return;
    Array.prototype.forEach.call(document.querySelectorAll('.hf-selected'), function (el) { el.classList.remove('hf-selected'); });
    if (sel.all) { Array.prototype.forEach.call(document.querySelectorAll('#previewArea .q-rich-content .txt'), function (t) { t.classList.add('hf-selected'); }); return; }
    var rows = document.querySelectorAll('#previewArea .question-print-row[data-qid="' + String(sel.question.id) + '"] .q-rich-content .txt');
    var first = null, last = null, firstOff = 0, lastOff = 0;
    Array.prototype.forEach.call(rows, function (t) {
      var off = Number(t.dataset.off), len = t.textContent.length;
      if (off + len <= sel.start || off >= sel.end) return;
      if (off >= sel.start && off + len <= sel.end) t.classList.add('hf-selected');
      else {
        /* تکهٔ مرزی: فقط بخشِ داخلِ بازه هایلایت شود → تکه به دو/سه span تقسیم می‌شود */
        var a = Math.max(0, sel.start - off), b = Math.min(len, sel.end - off), txt = t.textContent, frag = document.createDocumentFragment();
        function piece(str, o, mark) { var sp = document.createElement('span'); sp.className = 'txt' + (mark ? ' hf-selected' : ''); sp.dataset.off = String(o); sp.dataset.q = t.dataset.q; sp.setAttribute('style', t.getAttribute('style') || ''); sp.textContent = str; return sp; }
        if (a > 0) frag.appendChild(piece(txt.slice(0, a), off, false));
        frag.appendChild(piece(txt.slice(a, b), off + a, true));
        if (b < len) frag.appendChild(piece(txt.slice(b), off + b, false));
        t.parentNode.replaceChild(frag, t);
      }
    });
    var marked = document.querySelectorAll('#previewArea .question-print-row[data-qid="' + String(sel.question.id) + '"] .hf-selected');
    if (marked.length) {
      try {
        var r = document.createRange(); r.setStartBefore(marked[0]); r.setEndAfter(marked[marked.length - 1]);
        var ws = window.getSelection(); ws.removeAllRanges(); ws.addRange(r);
      } catch (e) {}
    }
  }
  function currentSel() { var r = (lastSel && lastSel.sticky) ? lastSel : (selectionRange() || lastSel); if (!r) toast('بخشی از متن را انتخاب کنید یا «همه» را بزنید'); return r; }
  function updateFmtBar(sel) {
    ['bold', 'italic', 'underline'].forEach(function (key) {
      var btn = document.querySelector('#hostFmt .hf-btn[data-fmt="' + key + '"]'); if (!btn) return;
      var q0 = sel && (sel.all ? (questions || [])[0] : sel.question);
      btn.classList.toggle('on', !!(sel && q0 && (sel.all ? coversAxis(q0.__spans || [], 0, text(q0.text).length, key) : coversAxis(q0.__spans || [], sel.start, sel.end, key))));
    });
    var curAl = '';
    if (sel) { var qa = sel.all ? (questions || [])[0] : sel.question; if (qa) { var pba = sel.all ? [0, text(qa.text).length] : paragraphBounds(text(qa.text), sel.start, sel.end); curAl = alignOfParagraph(qa, pba[0], pba[1]); } }
    Array.prototype.forEach.call(document.querySelectorAll('#hostFmt .hf-btn.hf-al'), function (b) { b.classList.toggle('on', !!curAl && b.dataset.fmt === 'align-' + curAl); });
    var bar = $('hostFmt'); if (bar) bar.classList.toggle('has-sel', !!sel);
    var allBtn = $('hfAll'); if (allBtn) allBtn.classList.toggle('on', !!(sel && sel.all));
  }
  var FONTS = [['default', 'پیش‌فرض'], ['Vazirmatn', 'وزیرمتن'], ['Shabnam', 'شبنم'], ['Sahel', 'ساحل'], ['BNazanin', 'ب نازنین'], ['Tahoma', 'تاهوما'], ['serif', 'سریف']];
  function currentSize(sel) {
    var q0 = sel.all ? (questions || [])[0] : sel.question, s0 = sel.all ? 0 : sel.start; if (!q0) return 0;
    var hit = (q0.__spans || []).find(function (x) { return x.start <= s0 && x.end > s0 && x.size; }); return hit ? hit.size : 0;
  }
  function faNum(n) { return String(n).replace(/\d/g, function (d) { return '۰۱۲۳۴۵۶۷۸۹'[+d]; }); }
  /* V133 — آیکونِ ترازِ متن: چهار خطِ افقی (x1..x2 در viewBox 20) */
  function alignBtn(kind, title, lines) {
    var svg = '<svg viewBox="0 0 20 16" width="18" height="15" aria-hidden="true">' + lines.map(function (ln, i) {
      return '<line x1="' + ln[0] + '" y1="' + (2 + i * 4) + '" x2="' + ln[1] + '" y2="' + (2 + i * 4) + '" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>';
    }).join('') + '</svg>';
    return '<button type="button" class="hf-btn hf-al" data-fmt="align-' + kind + '" title="' + title + '">' + svg + '</button>';
  }
  function ensureFmtBar() {
    var v = $('pgsViewer'); if (!v || $('hostFmt')) return;
    var ribbon = v.querySelector('.pgs-ribbon');
    var bar = document.createElement('div');
    bar.id = 'hostFmt'; bar.className = 'host-fmt';
    /* (ساختِ DOM با DOMParser + importNode — قاعدهٔ verify) */
    var markup =
      '<button type="button" class="hf-btn hf-b" data-fmt="bold" title="بلد">B</button>' +
      '<button type="button" class="hf-btn hf-i" data-fmt="italic" title="ایتالیک">I</button>' +
      '<button type="button" class="hf-btn hf-u" data-fmt="underline" title="زیرخط">U</button>' +
      '<label class="hf-color" title="رنگ متن"><span class="hf-a">A</span><span class="hf-swatch" id="hfSwatch"></span><input type="color" id="hfColor" value="#000000"></label>' +
      '<button type="button" class="hf-btn hf-pick" id="hfSize" title="اندازهٔ متن">اندازه</button>' +
      '<button type="button" class="hf-btn hf-pick" id="hfFont" title="فونت">فونت</button>' +
      alignBtn('right', 'راست‌چین', [[4,20],[8,20],[4,20],[10,20]]) +
      alignBtn('center', 'وسط‌چین', [[2,18],[5,15],[2,18],[6,14]]) +
      alignBtn('left', 'چپ‌چین', [[0,16],[0,12],[0,16],[0,10]]) +
      alignBtn('justify', 'دوطرفه (جاستیفای)', [[0,20],[0,20],[0,20],[0,20]]) +
      '<button type="button" class="hf-btn hf-clear" data-fmt="clear" title="حذف قالب‌بندی">پاک</button>' +
      '<button type="button" class="hf-btn hf-all" id="hfAll" title="انتخابِ همهٔ متن">همه</button>';
    var doc = new DOMParser().parseFromString('<div>' + markup + '</div>', 'text/html');
    Array.prototype.slice.call(doc.body.firstChild.childNodes).forEach(function (n) { bar.appendChild(document.importNode(n, true)); });
    if (ribbon) ribbon.parentNode.insertBefore(bar, ribbon.nextSibling); else v.insertBefore(bar, v.firstChild);
    /* پنلِ 📐 (top ثابت در webhost.css) باید زیرِ هر دو نوار باز شود */
    function syncTop() {
      try {
        var hidden = bar.classList.contains('hf-hidden');
        var b = hidden ? (ribbon ? ribbon.getBoundingClientRect().bottom : 0) : bar.getBoundingClientRect().bottom;
        document.documentElement.style.setProperty('--host-top', Math.round(b) + 'px');
      } catch (e) {}
    }
    bar.__syncTop = syncTop;
    syncTop(); setTimeout(syncTop, 300); window.addEventListener('resize', syncTop);
    /* V131 — نوار پیش‌فرض باز؛ محو با اسکرولِ تند/زیاد و بازگشت با اسکرول به عقب */
    bar.classList.remove('hf-hidden'); fmtHiddenBySetup = false;
    bindFmtScrollHide(bar);
    Array.prototype.forEach.call(bar.querySelectorAll('.hf-btn[data-fmt]'), function (btn) {
      /* pointerdown پیش‌فرض گرفته می‌شود تا انتخابِ متن با لمسِ دکمه از بین نرود */
      btn.addEventListener('pointerdown', function (ev) { ev.preventDefault(); rememberSelection(); });
      btn.addEventListener('click', function () {
        var key = btn.dataset.fmt, sel = currentSel(); if (!sel) return;
        if (key === 'clear') { applyStyle(sel, {bold: false, italic: false, underline: false, color: '', size: 0, font: ''}); return; }
        if (key.indexOf('align-') === 0) { formatSelection(key, ''); return; }
        var patch = {}; patch[key] = !selCovers(sel, key); applyStyle(sel, patch);
      });
    });
    /* V130 — «همه»: انتخابِ تمامِ متنِ سؤال‌های برگه برای اعمالِ یک‌بارهٔ ابزارها */
    var allBtn = bar.querySelector('#hfAll');
    allBtn.addEventListener('pointerdown', function (ev) { ev.preventDefault(); });
    allBtn.addEventListener('click', function () {
      if (lastSel && lastSel.all) { clearSticky(); return; }
      try { window.getSelection().removeAllRanges(); } catch (e) {}
      lastSel = {all: true, sticky: true}; restoreSelection(); updateFmtBar(lastSel);
    });
    var color = $('hfColor'), size = $('hfSize'), font = $('hfFont');
    [color, size, font].forEach(function (el) { el.addEventListener('pointerdown', function (ev) { if (el !== color) ev.preventDefault(); rememberSelection(); }); el.addEventListener('focus', rememberSelection); });
    color.addEventListener('change', function () {
      var sw = $('hfSwatch'); if (sw) sw.style.background = color.value;
      var sel = currentSel(); if (sel) applyStyle(sel, {color: /^#[0-9a-fA-F]{6}$/.test(color.value) ? color.value : ''});
    });
    /* V130 — پنجرهٔ اندازه/فونت بومی (Compose) از طریقِ پل؛ نتیجه با ExamPrintRenderer.formatSelection برمی‌گردد.
       اگر پل نبود (مرورگر)، prompt ساده. */
    size.addEventListener('click', function () {
      var sel = currentSel(); if (!sel) return;
      var b = window.ExamPrintBridge;
      if (b && typeof b.pickSize === 'function') { b.pickSize(String(currentSize(sel) || '')); return; }
      var v = Number(window.prompt('اندازه (۱ تا ۱۰۰)', String(currentSize(sel) || 12))); if (v >= 1 && v <= 100) applyStyle(sel, {size: Math.round(v)});
    });
    font.addEventListener('click', function () {
      var sel = currentSel(); if (!sel) return;
      var b = window.ExamPrintBridge;
      if (b && typeof b.pickFont === 'function') { b.pickFont(JSON.stringify(FONTS)); return; }
      var v = window.prompt('نام فونت', ''); if (v != null) applyStyle(sel, {font: v === 'default' ? '' : v});
    });
    if (!document.__hostSelBound) {
      document.__hostSelBound = true;
      document.addEventListener('selectionchange', function () {
        var r = selectionRange();
        if (r) { if (!(lastSel && lastSel.sticky && !lastSel.all && lastSel.question === r.question && lastSel.start === r.start && lastSel.end === r.end)) { Array.prototype.forEach.call(document.querySelectorAll('.hf-selected'), function (el) { el.classList.remove('hf-selected'); }); lastSel = r; } updateFmtBar(r); }
        else if (!(lastSel && lastSel.sticky)) updateFmtBar(null);
      });
      /* لمس روی جای دیگرِ برگه (نه نوار و نه متنِ انتخاب‌شده) → پایانِ انتخابِ پابرجا */
      document.addEventListener('pointerdown', function (ev) {
        var t = ev.target;
        if (t && t.closest && (t.closest('#hostFmt') || t.closest('.hf-selected'))) return;
        if (lastSel && lastSel.sticky) clearSticky();
      }, true);
    }
  }
  function formatSelection(kind, value) {
    var sel = (lastSel && lastSel.sticky) ? lastSel : (selectionRange() || lastSel); if (!sel) { toast('اول بخشی از متنِ سؤال را انتخاب کنید یا «همه» را بزنید'); return 'noselection'; }
    value = value == null ? '' : String(value);
    if (kind === 'clear') { applyStyle(sel, {bold: false, italic: false, underline: false, color: '', size: 0, font: ''}); return 'ok'; }
    if (kind.indexOf('align-') === 0 || kind === 'align') {
      var al = kind === 'align' ? value : kind.slice(6); if (ALIGNS.indexOf(al) < 0) return 'unknown';
      /* V133 — تراز روی پاراگرافِ دربرگیرندهٔ انتخاب؛ دوباره‌زدنِ همان تراز → برگشت به پیش‌فرض */
      if (sel.all) { (questions || []).forEach(function (q0) { var L = text(q0.text).length; q0.__aligns = L ? [{start: 0, end: L, align: al}] : []; }); lastSel = {all: true, sticky: true}; }
      else { var q1 = sel.question, pb1 = paragraphBounds(text(q1.text), sel.start, sel.end), cur = alignOfParagraph(q1, pb1[0], pb1[1]); q1.__aligns = setAlignRange(q1, sel.start, sel.end, cur === al ? '' : al); lastSel = {question: q1, start: sel.start, end: sel.end, sticky: true}; }
      rerenderKeepScroll(); updateFmtBar(lastSel); return 'ok';
    }
    if (kind === 'bold' || kind === 'italic' || kind === 'underline') { var patch = {}; patch[kind] = !selCovers(sel, kind); applyStyle(sel, patch); return 'ok'; }
    if (kind === 'color') { applyStyle(sel, {color: /^#[0-9a-fA-F]{6}$/.test(value) ? value : ''}); return 'ok'; }
    if (kind === 'size') { var n = Number(value); applyStyle(sel, {size: (n >= 1 && n <= 100) ? Math.round(n) : 0}); return 'ok'; }
    if (kind === 'font') { applyStyle(sel, {font: (value && value !== 'default') ? value.slice(0, 30) : ''}); return 'ok'; }
    return 'unknown';
  }
  /* V128.1 — نوارِ قالب‌بندی نباید فقط به showPreview وابسته باشد: هر بار که بینندهٔ PGS ساخته شد
     (openPreviewWindow، بازگشت از چاپ، بازِ دوباره) نوار هم ساخته می‌شود؛ MutationObserver پشتیبان است. */
  function hookPreviewOpen() {
    var base = window.openPreviewWindow;
    if (typeof base !== 'function' || base.__appHostFmt) return false;
    var wrapped = function () { var r = base.apply(this, arguments); try { ensureFmtBar(); } catch (e) {} setTimeout(function () { try { ensureFmtBar(); } catch (e) {} }, 150); return r; };
    wrapped.__appHostFmt = true;
    window.openPreviewWindow = wrapped;
    return true;
  }
  function watchViewer() {
    if (window.__hostFmtObserver || typeof MutationObserver !== 'function') return;
    window.__hostFmtObserver = new MutationObserver(function () { if ($('pgsViewer') && !$('hostFmt')) { try { ensureFmtBar(); } catch (e) {} } });
    window.__hostFmtObserver.observe(document.body, {childList: true});
  }
  function hookPreviewClose() {
    var base = window.closePreviewWindow;
    if (typeof base !== 'function' || base.__appHost) return false;
    var wrapped = function () { var r = base.apply(this, arguments); callBridge('previewClosed'); return r; };
    wrapped.__appHost = true;
    window.closePreviewWindow = wrapped;
    return true;
  }
  function installPrintOverrides() {
    /* دکمه‌های نوارِ PGS (چاپ دانشجو/استاد، دیالوگِ چاپ) و Ctrl+P → پلِ بومی. */
    window.pgsPrintNow = function (mode) { var m = $('pgsPrintMenu'); if (m) m.classList.remove('open'); requestPrint(mode || 'student'); };
    window.pgsDoPrintFromDlg = function () {
      var mode = (document.querySelector('input[name="pgsMode"]:checked') || {}).value || 'student';
      var rangeKind = (document.querySelector('input[name="pgsRange"]:checked') || {}).value || 'all';
      var copies = num($('pgsCopies') && $('pgsCopies').value, 1);
      var rangeText = ($('pgsRangeVal') && $('pgsRangeVal').value) || '';
      if (rangeKind === 'range' && !parseRange(rangeText, 999)) { toast('بازهٔ صفحات معتبر نیست. نمونه: ۱،۳-۵'); return; }
      var current = num(String(($('pgsPageInput') && $('pgsPageInput').value) || '1').replace(/[۰-۹]/g, function (d) { return '۰۱۲۳۴۵۶۷۸۹'.indexOf(d); }), 1);
      try { window.pgsClosePrintDlg(); } catch (e) {}
      requestPrint(mode, {rangeKind: rangeKind, rangeText: rangeText, current: current, copies: copies});
    };
    window.printStudent = function () { return requestPrint('student'); };
    window.printTeacher = function () { return requestPrint('teacher'); };
    window.print = function () { try { requestPrint(printMode); } catch (e) { requestPrint('student'); } };
  }

  /* ---------------------------------------------------------------- snapshot / ویرایشِ شکل */
  function snapshot() {
    var result = {};
    (questions || []).forEach(function (q) {
      result[String(q.id)] = {figLayouts: q.figLayouts || {}, sepExtraPx: Math.round(num(q.sepExtraPx, 0)), spans: (q.__spans || []).map(function (x) {
        return {start: x.start, end: x.end, bold: !!x.bold, italic: !!x.italic, underline: !!x.underline, color: x.color || '', size: x.size || 0, font: x.font || ''};
      }), alignSpans: (q.__aligns || []).map(function (x) { return {start: x.start, end: x.end, align: x.align}; })};
    });
    return JSON.stringify(result);
  }
  function figTokens(source) {
    var list = [], from = 0;
    while (true) {
      var start = source.indexOf('%%FIG:', from); if (start < 0) break;
      var end = source.indexOf('%%', start + 6); if (end < 0) break;
      list.push({start: start, end: end + 2, raw: source.slice(start + 6, end)});
      from = end + 2;
    }
    return list;
  }
  function figureAt(questionId, figureIndex) {
    var q = (questions || []).find(function (x) { return String(x.id) === String(questionId); });
    if (!q) return '';
    var token = figTokens(q.text)[Number(figureIndex)];
    return token ? JSON.stringify({spec: token.raw, start: token.start, end: token.end}) : '';
  }
  function decodeBase64(value) {
    try {
      var binary = atob(text(value)), bytes = new Uint8Array(binary.length);
      for (var i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
      return new TextDecoder('utf-8').decode(bytes);
    } catch (ignored) { return ''; }
  }
  function replaceFigure(questionId, start, end, tokenBase64) {
    var q = (questions || []).find(function (x) { return String(x.id) === String(questionId); });
    var token = decodeBase64(tokenBase64), from = Number(start), to = Number(end);
    if (!q || !/^%%FIG:\{[\s\S]*\}%%$/.test(token) || !Number.isInteger(from) || !Number.isInteger(to) || from < 0 || to <= from || to > q.text.length) return 'invalid';
    try { JSON.parse(token.slice(6, -2)); } catch (ignored) { return 'invalid'; }
    q.text = q.text.slice(0, from) + token + q.text.slice(to);
    try { window.renderPreview(); } catch (e) {}
    return 'ok';
  }
  /* دوبار لمس/دابل‌کلیک روی شکلِ سؤال → ابزارِ بومیِ برنامه (نه ویرایشگرهای وب). روی window و در
     فازِ capture تا پیش از شنونده‌های geo-fig/mainscript اجرا شود. */
  var lastTap = 0, lastTapFig = null;
  function figureOf(target) {
    return target && target.closest ? target.closest('#previewArea .interactive-figure') : null;
  }
  function editFigure(fig) {
    var qid = fig.dataset.qid, index = Number(fig.dataset.figIndex);
    if (!qid || !Number.isInteger(index)) return;
    callBridge('editFigureTool', String(qid), index);
  }
  window.addEventListener('dblclick', function (e) {
    var fig = figureOf(e.target); if (!fig) return;
    e.preventDefault(); e.stopImmediatePropagation(); editFigure(fig);
  }, true);
  /* V129 — کلیک/لمسِ اول: انتخاب (کادر + ۸ دستگیرهٔ تغییر اندازه روی اضلاع و گوشه‌ها)؛ کلیک/لمسِ دوم روی
     شکلِ از‌پیش‌انتخاب‌شده (بدونِ کشیدن و نه روی دستگیره): بازکردنِ ویرایشگرِ بومی. */
  var figDown = null;
  window.addEventListener('pointerdown', function (e) {
    var fig = figureOf(e.target);
    figDown = fig ? {fig: fig, x: e.clientX, y: e.clientY, wasSelected: fig.classList.contains('selected'), onHandle: !!(e.target.closest && e.target.closest('.fig-resize-handle'))} : null;
  }, true);
  window.addEventListener('pointerup', function (e) {
    var d = figDown; figDown = null;
    if (!d) return;
    var fig = figureOf(e.target); if (fig !== d.fig) return;
    var moved = Math.hypot(e.clientX - d.x, e.clientY - d.y) > 8;
    /* V132 — لمسِ روی/نزدیکِ دستگیره (mainscript با تحملِ ۲۴px آن را resize می‌گیرد) هرگز ویرایشگر را باز نمی‌کند */
    if (moved || d.onHandle || fig.__lastResize) return;
    if (d.wasSelected) { e.preventDefault(); e.stopImmediatePropagation(); editFigure(fig); }
  }, true);

  /* ---------------------------------------------------------------- API برنامه */
  function setPageSetup(value) {
    if (!writePageSetup(value)) return 'missing';
    try { window.renderPreview(); } catch (e) {}
    return 'ok';
  }
  function getPageSetup() { return JSON.stringify(readPageSetup()); }

  /* ================================================================ V129 — دلیمترهای عینِ ویرایشگرِ فرمول
     ویرایشگر (formula.html، MB_DELIM_SHAPE) هر پرانتز/کروشه/آکولاد/… را از قطعه‌های SVGِ کش‌پذیر (سرِ ثابت +
     بازوی flex) می‌سازد؛ موتورِ وب در پیش‌نمایش گلیفِ فونت را با scaleY می‌کشید (و برای [ ] | مسیرِ stroke).
     اینجا makeDelimWrap با همان جدولِ ویرایشگر جایگزین می‌شود تا پیش‌نمایش و چاپ عیناً مثلِ ویرایشگر باشند. */
  var DELIM_SHAPE = {
    paren: ['.44em', [['c', 'M9 1 C3.2 3.1 1.6 6 1.6 10'], ['b', 'M1.6 0 L1.6 10'], ['c', 'M1.6 0 C1.6 4 3.2 6.9 9 9']]],
    bow: ['.42em', [['b', 'M8.8 0.5 C2.1 3.2 2.1 6.8 8.8 9.5']]],
    brk: ['.36em', [['c', 'M9 1.2 L1.6 1.2 L1.6 10'], ['b', 'M1.6 0 L1.6 10'], ['c', 'M1.6 0 L1.6 8.8 L9 8.8']]],
    brace: ['.5em', [['c', 'M9 0.8 Q4.6 0.8 4.6 10'], ['b', 'M4.6 0 L4.6 10'], ['m', 'M4.6 0 Q4.6 5 0.7 5 Q4.6 5 4.6 10'], ['b', 'M4.6 0 L4.6 10'], ['c', 'M4.6 0 Q4.6 9.2 9 9.2']]],
    ceil: ['.36em', [['c', 'M9 1.2 L1.6 1.2 L1.6 10'], ['b', 'M1.6 0 L1.6 10'], ['c', 'M1.6 0 L1.6 10']]],
    floor: ['.36em', [['c', 'M1.6 0 L1.6 10'], ['b', 'M1.6 0 L1.6 10'], ['c', 'M1.6 0 L1.6 8.8 L9 8.8']]],
    bar: ['.28em', [['b', 'M5 0 L5 10']]],
    dbar: ['.4em', [['b', 'M3 0 L3 10 M7 0 L7 10']]],
    ang: ['.42em', [['b', 'M8.4 0 L1.6 5 L8.4 10']]]
  };
  var DELIM_MAP = {'(': ['bow', 0], ')': ['bow', 1], '⟮': ['paren', 0], '⟯': ['paren', 1], '[': ['brk', 0], ']': ['brk', 1],
    '{': ['brace', 0], '}': ['brace', 1], '⌈': ['ceil', 0], '⌉': ['ceil', 1], '⌊': ['floor', 0], '⌋': ['floor', 1],
    '⟨': ['ang', 0], '⟩': ['ang', 1], '〈': ['ang', 0], '〉': ['ang', 1], '|': ['bar', 0], '‖': ['dbar', 0], '∥': ['dbar', 0]};
  function esc(v) { return String(v == null ? '' : v).replace(/[&<>"']/g, function (c) { return ({'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'})[c]; }); }
  function delimSvg(ch) {
    var info = DELIM_MAP[String(ch)]; if (!info) return '';
    var sh = DELIM_SHAPE[info[0]]; if (!sh) return '';
    var out = '<span class="mdelim-x mdx-' + info[0] + (info[1] ? ' mdx-flip' : '') + '" style="--mdw:' + sh[0] + '" aria-hidden="true">';
    for (var i = 0; i < sh[1].length; i++) out += '<svg class="mdx mdx-' + sh[1][i][0] + '" viewBox="0 0 10 10" preserveAspectRatio="none"><path d="' + sh[1][i][1] + '"/></svg>';
    return out + '</span>';
  }
  function delimHtml(ch) {
    var c0 = String(ch == null ? '' : ch); if (!c0) return '';
    /* data-delim عمداً گذاشته نمی‌شود تا اصلاح‌گرِ strokeِ موتورِ وب (fixDom) SVG را با مسیرِ خودش عوض نکند؛
       یک .mdelim-glyphِ خالی و پنهان هم می‌ماند تا fitMathStretchers (که فقط دنبالِ آن کلاس می‌گردد) محتوای SVG را پاک نکند */
    if (DELIM_MAP[c0]) return '<span class="mdelim mdx-host" data-dl="' + esc(c0) + '">' + delimSvg(c0) + '<span class="mdelim-glyph"></span></span>';
    return '<span class="mdelim" data-delim="' + esc(c0) + '"><span class="mdelim-glyph">' + esc(c0) + '</span></span>';
  }
  function installDelimOverride() {
    /* V133 — ماتریس‌ها (pmatrix/bmatrix/…) هم از همین دلیمترهای SVGِ ویرایشگر استفاده می‌کنند (math_host.js) */
    window.__hostDelimHtml = delimHtml;
    if (typeof window.MathParser !== 'function' || !window.MathParser.prototype || window.MathParser.prototype.__hostDelim) return;
    var P = window.MathParser.prototype;
    P.__hostDelim = true;
    P.makeDelimWrap = function (o, c, inner) {
      var kind = this.delimKind(o, c);
      if (kind === 'floor' || kind === 'ceil') {
        var fo = kind === 'floor' ? '⌊' : '⌈', fc = kind === 'floor' ? '⌋' : '⌉';
        return '<span class="mbrk-box mbrk-' + kind + '">' + delimHtml(fo) + '<span class="mpar-body">' + (inner || '') + '</span>' + delimHtml(fc) + '</span>';
      }
      return '<span class="mparbox" data-kind="' + esc(kind) + '">' + delimHtml(o) + '<span class="mpar-body">' + (inner || '') + '</span>' + delimHtml(c) + '</span>';
    };
  }

  function install() {
    ensurePgs();
    installDelimOverride();
    installRichTextOverride();
    installPrintOverrides();
    hookPreviewClose();
    hookPreviewOpen();
    hookSetupToggle();
    try { syncNativeSelects(); } catch (e) {}
    watchViewer();
    wrapRenderPreviewOnce();
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', function () { setTimeout(install, 0); });
  else setTimeout(install, 0);

  window.setExamData = setExamData;
  window.printStudent = function () { return requestPrint('student'); };
  window.printTeacher = function () { return requestPrint('teacher'); };
  window.ExamPrintRenderer = {
    showPreview: showPreview, layoutSnapshot: snapshot, figureAt: figureAt, replaceFigure: replaceFigure,
    restorePreview: restorePreview, setPageSetup: setPageSetup, getPageSetup: getPageSetup,
    formatSelection: formatSelection, setOption: setOption
  };
})();
