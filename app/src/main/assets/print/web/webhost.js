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
    if ($('opt_paper') && !setupBound) { setupBound = true; bindSetupReport(); }
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
    return true;
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
  function showPreview() {
    try { printMode = 'student'; } catch (e) {}
    try { window.openPreviewWindow(); } catch (e) { return 'err'; }
    return 'ok';
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
    (questions || []).forEach(function (q) { result[String(q.id)] = {figLayouts: q.figLayouts || {}, sepExtraPx: Math.round(num(q.sepExtraPx, 0))}; });
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
  window.addEventListener('pointerup', function (e) {
    var fig = figureOf(e.target); if (!fig || e.pointerType === 'mouse') return;
    var now = Date.now();
    if (fig === lastTapFig && now - lastTap < 320) { lastTap = 0; lastTapFig = null; editFigure(fig); return; }
    lastTap = now; lastTapFig = fig;
  }, true);

  /* ---------------------------------------------------------------- API برنامه */
  function setPageSetup(value) {
    if (!writePageSetup(value)) return 'missing';
    try { window.renderPreview(); } catch (e) {}
    return 'ok';
  }
  function getPageSetup() { return JSON.stringify(readPageSetup()); }

  function install() {
    ensurePgs();
    installPrintOverrides();
    hookPreviewClose();
    wrapRenderPreviewOnce();
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', function () { setTimeout(install, 0); });
  else setTimeout(install, 0);

  window.setExamData = setExamData;
  window.printStudent = function () { return requestPrint('student'); };
  window.printTeacher = function () { return requestPrint('teacher'); };
  window.ExamPrintRenderer = {
    showPreview: showPreview, layoutSnapshot: snapshot, figureAt: figureAt, replaceFigure: replaceFigure,
    restorePreview: restorePreview, setPageSetup: setPageSetup, getPageSetup: getPageSetup
  };
})();
