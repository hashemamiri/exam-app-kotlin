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
      /* V128 — بازه‌های قالب‌بندیِ متنِ سؤال (نوارِ B/I/U/رنگ/اندازه/فونتِ پیش‌نمایش؛ در بیلدرِ بومی ذخیره می‌شوند). */
      __spans: cleanSpans(src.textSpans, text(src.text).length),
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
      var size = Math.round(num(item.size, 0)); if (!(size >= 8 && size <= 40)) size = 0;
      var font = text(item.font); if (font === 'default') font = '';
      return {start: start, end: end, bold: item.bold === true, italic: item.italic === true, underline: item.underline === true, color: color, size: size, font: font};
    }).filter(function (item) { return item.end > item.start && (item.bold || item.italic || item.underline || item.color || item.size || item.font); });
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
      var out = '', last = 0, m, figIndex = 0;
      while ((m = tokenRe.exec(src))) {
        out += styledPlainWithMath(src.slice(last, m.index), last, q);
        if (m[1] != null) out += window.renderFigToken(m[1], q, figIndex++);
        else out += window.renderVisualTool(typeMap[m[2]], m[3] || '', m[4] || '');
        last = tokenRe.lastIndex;
      }
      return out + styledPlainWithMath(src.slice(last), last, q);
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
  function coversAxis(spans, s, e, key) {
    var cursor = s, active = spans.filter(function (x) { return x[key]; }).sort(function (x, y) { return x.start - y.start; });
    for (var i = 0; i < active.length; i++) { if (active[i].start > cursor) return false; cursor = Math.max(cursor, active[i].end); if (cursor >= e) return true; }
    return cursor >= e;
  }
  function applyStyle(sel, patch) {
    var q = sel.question, s = sel.start, e = sel.end, out = [];
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
    q.__spans = merged;
    try { window.getSelection().removeAllRanges(); } catch (e2) {}
    lastSel = null; updateFmtBar(null);
    /* باز-رندر + صفحه‌بندیِ PGS با حفظِ اسکرول (موج‌های paginate در ۱۶۰/۵۵۰/۱۳۰۰ms) */
    var wrap = $('pgsCanvasWrap'), top = wrap ? wrap.scrollTop : 0;
    try { window.renderPreview(); } catch (e3) {}
    [80, 300, 700, 1450].forEach(function (t) { setTimeout(function () { var w = $('pgsCanvasWrap'); if (w) w.scrollTop = top; }, t); });
  }
  function currentSel() { var r = selectionRange() || lastSel; if (!r) toast('اول بخشی از متنِ سؤال را انتخاب کنید (لمسِ طولانی روی کلمه)'); return r; }
  function updateFmtBar(sel) {
    ['bold', 'italic', 'underline'].forEach(function (key) {
      var btn = document.querySelector('#hostFmt .hf-btn[data-fmt="' + key + '"]'); if (!btn) return;
      btn.classList.toggle('on', !!(sel && coversAxis(sel.question.__spans || [], sel.start, sel.end, key)));
    });
    var bar = $('hostFmt'); if (bar) bar.classList.toggle('has-sel', !!sel);
  }
  var FONTS = [['default', 'پیش‌فرض'], ['Vazirmatn', 'وزیرمتن'], ['Shabnam', 'شبنم'], ['Sahel', 'ساحل'], ['BNazanin', 'ب نازنین'], ['Tahoma', 'تاهوما'], ['serif', 'سریف']];
  var SIZES = [10, 11, 12, 13, 14, 16, 18, 20, 24, 28];
  function faNum(n) { return String(n).replace(/\d/g, function (d) { return '۰۱۲۳۴۵۶۷۸۹'[+d]; }); }
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
      '<select class="hf-sel" id="hfSize" title="اندازهٔ متن"><option value="">اندازه</option>' + SIZES.map(function (n) { return '<option value="' + n + '">' + faNum(n) + '</option>'; }).join('') + '</select>' +
      '<select class="hf-sel" id="hfFont" title="فونت"><option value="">فونت</option>' + FONTS.map(function (f) { return '<option value="' + f[0] + '">' + f[1] + '</option>'; }).join('') + '</select>' +
      '<button type="button" class="hf-btn hf-clear" data-fmt="clear" title="حذف قالب‌بندی">پاک</button>' +
      '<span class="hf-hint">متنِ سؤال را انتخاب کنید</span>';
    var doc = new DOMParser().parseFromString('<div>' + markup + '</div>', 'text/html');
    Array.prototype.slice.call(doc.body.firstChild.childNodes).forEach(function (n) { bar.appendChild(document.importNode(n, true)); });
    if (ribbon) ribbon.parentNode.insertBefore(bar, ribbon.nextSibling); else v.insertBefore(bar, v.firstChild);
    /* پنلِ 📐 (top ثابت در webhost.css) باید زیرِ هر دو نوار باز شود */
    function syncTop() { try { document.documentElement.style.setProperty('--host-top', Math.round(bar.getBoundingClientRect().bottom) + 'px'); } catch (e) {} }
    syncTop(); setTimeout(syncTop, 300); window.addEventListener('resize', syncTop);
    Array.prototype.forEach.call(bar.querySelectorAll('.hf-btn[data-fmt]'), function (btn) {
      /* pointerdown پیش‌فرض گرفته می‌شود تا انتخابِ متن با لمسِ دکمه از بین نرود */
      btn.addEventListener('pointerdown', function (ev) { ev.preventDefault(); rememberSelection(); });
      btn.addEventListener('click', function () {
        var key = btn.dataset.fmt, sel = currentSel(); if (!sel) return;
        if (key === 'clear') { applyStyle(sel, {bold: false, italic: false, underline: false, color: '', size: 0, font: ''}); return; }
        var patch = {}; patch[key] = !coversAxis(sel.question.__spans || [], sel.start, sel.end, key); applyStyle(sel, patch);
      });
    });
    var color = $('hfColor'), size = $('hfSize'), font = $('hfFont');
    [color, size, font].forEach(function (el) { el.addEventListener('pointerdown', rememberSelection); el.addEventListener('focus', rememberSelection); });
    color.addEventListener('change', function () {
      var sw = $('hfSwatch'); if (sw) sw.style.background = color.value;
      var sel = currentSel(); if (sel) applyStyle(sel, {color: /^#[0-9a-fA-F]{6}$/.test(color.value) ? color.value : ''});
    });
    size.addEventListener('change', function () { var n = Number(size.value); size.value = ''; var sel = currentSel(); if (sel && n) applyStyle(sel, {size: n}); });
    font.addEventListener('change', function () { var f = font.value; font.value = ''; var sel = currentSel(); if (sel && f) applyStyle(sel, {font: f === 'default' ? '' : f}); });
    if (!document.__hostSelBound) {
      document.__hostSelBound = true;
      document.addEventListener('selectionchange', function () { var r = selectionRange(); if (r) lastSel = r; updateFmtBar(r); });
    }
  }
  function formatSelection(kind, value) {
    var sel = selectionRange() || lastSel; if (!sel) { toast('اول بخشی از متنِ سؤال را انتخاب کنید'); return 'noselection'; }
    value = value == null ? '' : String(value);
    if (kind === 'clear') { applyStyle(sel, {bold: false, italic: false, underline: false, color: '', size: 0, font: ''}); return 'ok'; }
    if (kind === 'bold' || kind === 'italic' || kind === 'underline') { var patch = {}; patch[kind] = !coversAxis(sel.question.__spans || [], sel.start, sel.end, kind); applyStyle(sel, patch); return 'ok'; }
    if (kind === 'color') { applyStyle(sel, {color: /^#[0-9a-fA-F]{6}$/.test(value) ? value : ''}); return 'ok'; }
    if (kind === 'size') { var n = Number(value); applyStyle(sel, {size: (n >= 8 && n <= 40) ? Math.round(n) : 0}); return 'ok'; }
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
      })};
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
    installRichTextOverride();
    installPrintOverrides();
    hookPreviewClose();
    hookPreviewOpen();
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
    formatSelection: formatSelection
  };
})();
