/* =====================================================================
   PGS ENGINE — موتور پیش‌نمایش و چاپ نسخهٔ ۲۰
   - صفحه‌بندی واقعی (شیت‌های mm-محور) با اندازه‌گیری زندهٔ DOM
   - رابط پیش‌نمایش شبیه Word (زوم/fit/ناوبری/بندانگشتی/نوار وضعیت)
   - چاپ از iframe اختصاصی با @page دقیق ⇒ پیش‌نمایش == چاپ (۱۰۰٪)
   - سازگاری کامل با هوک‌های قبلی (qimg/QMF/autosave/جابه‌جایی سؤال)
   ===================================================================== */
(function () {
  'use strict';
  if (window.__pgsInstalled) return;
  window.__pgsInstalled = true;

  /* ============================ ابزارها ============================ */
  var $ = function (id) { return document.getElementById(id); };
  var faNum = function (n) { return String(n).replace(/\d/g, function (d) { return '۰۱۲۳۴۵۶۷۸۹'[+d]; }); };
  var enNum = function (s) {
    return String(s == null ? '' : s)
      .replace(/[۰-۹]/g, function (d) { return String('۰۱۲۳۴۵۶۷۸۹'.indexOf(d)); })
      .replace(/[٠-٩]/g, function (d) { return String('٠١٢٣٤٥٦٧٨٩'.indexOf(d)); });
  };
  var num = function (v, def) { v = parseFloat(enNum(v)); return isFinite(v) ? v : def; };
  var clamp = function (v, a, b) { return Math.max(a, Math.min(b, v)); };
  var childArr = function (n) { return Array.prototype.slice.call(n.children); };

  var PAPERS = {
    a4:     { w: 210,   h: 297,   label: 'A4 (۲۱۰×۲۹۷)' },
    a5:     { w: 148,   h: 210,   label: 'A5 (۱۴۸×۲۱۰)' },
    b5:     { w: 176,   h: 250,   label: 'B5 (۱۷۶×۲۵۰)' },
    letter: { w: 215.9, h: 279.4, label: 'Letter (۲۱۵.۹×۲۷۹.۴)' },
    f4:     { w: 210,   h: 330,   label: 'F4 فولسکاپ (۲۱۰×۳۳۰)' },
    legal:  { w: 215.9, h: 355.6, label: 'Legal (۲۱۵.۹×۳۵۵.۶)' },
    custom: { w: 210,   h: 297,   label: 'سفارشی' }
  };
  var MARGIN_PRESETS = {
    normal: { t: 12, b: 12, r: 10, l: 10 },
    narrow: { t: 6,  b: 6,  r: 6,  l: 6 },
    wide:   { t: 20, b: 20, r: 20, l: 20 }
  };
  var LS_SETUP = 'pgs_pagesetup_v20';
  var LS_ZOOM = 'pgs_zoom_v20';

  /* ============================ تنظیمات صفحه ============================ */
  function readCfg() {
    var paperKey = ($('opt_paper') && $('opt_paper').value) || 'a4';
    var paper = PAPERS[paperKey] || PAPERS.a4;
    var W, H;
    if (paperKey === 'custom') {
      W = clamp(num($('opt_customW') && $('opt_customW').value, 210), 60, 600);
      H = clamp(num($('opt_customH') && $('opt_customH').value, 297), 60, 600);
    } else { W = paper.w; H = paper.h; }
    var orient = ($('opt_orientation') && $('opt_orientation').value) || 'portrait';
    if (orient === 'landscape' && W < H) { var t = W; W = H; H = t; }
    var m = {
      t: clamp(num($('opt_mT') && $('opt_mT').value, 12), 0, 80),
      b: clamp(num($('opt_mB') && $('opt_mB').value, 12), 0, 80),
      r: clamp(num($('opt_mR') && $('opt_mR').value, 10), 0, 80),
      l: clamp(num($('opt_mL') && $('opt_mL').value, 10), 0, 80)
    };
    return {
      paperKey: paperKey,
      paperLabel: paperKey === 'custom' ? ('سفارشی ' + faNum(W) + '×' + faNum(H)) : paper.label,
      W: W, H: H, orient: orient, m: m,
      border: $('opt_pageBorder') ? $('opt_pageBorder').checked !== false : true,
      pageNumbers: $('opt_showPageNumbers') ? $('opt_showPageNumbers').checked !== false : true,
      repeatHeader: $('opt_repeatHeader') ? $('opt_repeatHeader').checked !== false : true,
      font: clamp(num($('opt_baseFont') && $('opt_baseFont').value, 10.5), 6, 20)
    };
  }
  function saveSetup() {
    try {
      var ids = ['opt_paper','opt_customW','opt_customH','opt_orientation','opt_mT','opt_mB','opt_mR','opt_mL',
                 'opt_pageBorder','opt_showPageNumbers','opt_repeatHeader','opt_showScores','opt_questionSpacing','opt_baseFont'];
      var data = {};
      ids.forEach(function (id) {
        var el = $(id); if (!el) return;
        data[id] = (el.type === 'checkbox') ? el.checked : el.value;
      });
      localStorage.setItem(LS_SETUP, JSON.stringify(data));
    } catch (e) {}
  }
  function applySetup(data) {
    Object.keys(data || {}).forEach(function (id) {
      var el = $(id); if (!el) return;
      if (el.type === 'checkbox') el.checked = !!data[id];
      else el.value = data[id];
    });
  }

  /* ============================ DOM پایه ============================ */
  var PAGESETUP_HTML =
    '<div id="pgsPageSetup" class="exam-tools no-print" style="display:none">' +
      '<h2>📐 تنظیمات صفحه و چاپ <span class="toggle" onclick="pgsToggleSetup(false)">بستن ✕</span></h2>' +
      '<div class="grid-form">' +
        '<div class="field"><label>اندازهٔ کاغذ:</label><select id="opt_paper">' +
          '<option value="a4">A4 (۲۱۰×۲۹۷ میلی‌متر) — پیش‌فرض</option>' +
          '<option value="a5">A5 (۱۴۸×۲۱۰)</option>' +
          '<option value="b5">B5 (۱۷۶×۲۵۰)</option>' +
          '<option value="letter">Letter (۲۱۵.۹×۲۷۹.۴)</option>' +
          '<option value="f4">F4 فولسکاپ (۲۱۰×۳۳۰)</option>' +
          '<option value="legal">Legal (۲۱۵.۹×۳۵۵.۶)</option>' +
          '<option value="custom">سفارشی…</option>' +
        '</select></div>' +
        '<div class="field"><label>جهت کاغذ:</label><select id="opt_orientation">' +
          '<option value="portrait">عمودی</option><option value="landscape">افقی</option>' +
        '</select></div>' +
        '<div class="field"><label>عرض (mm):</label><input type="number" id="opt_customW" value="210" min="60" max="600" step="1"></div>' +
        '<div class="field"><label>ارتفاع (mm):</label><input type="number" id="opt_customH" value="297" min="60" max="600" step="1"></div>' +
        '<div class="field"><label>حاشیه‌ها:</label><select id="opt_marginPreset">' +
          '<option value="normal">معمولی (بالا/پایین ۱۲ — راست/چپ ۱۰)</option>' +
          '<option value="narrow">باریک (۶ میلی‌متر)</option>' +
          '<option value="wide">عریض (۲۰ میلی‌متر)</option>' +
          '<option value="custom">سفارشی</option>' +
        '</select></div>' +
        '<div class="field"><label>اندازهٔ فونت پایه (pt):</label><input type="number" id="opt_baseFont" value="10.5" min="6" max="20" step="0.5"></div>' +
        '<div class="field"><label>حاشیهٔ بالا (mm):</label><input type="number" id="opt_mT" value="12" min="0" max="80" step="0.5"></div>' +
        '<div class="field"><label>حاشیهٔ پایین (mm):</label><input type="number" id="opt_mB" value="12" min="0" max="80" step="0.5"></div>' +
        '<div class="field"><label>حاشیهٔ راست (mm):</label><input type="number" id="opt_mR" value="10" min="0" max="80" step="0.5"></div>' +
        '<div class="field"><label>حاشیهٔ چپ (mm):</label><input type="number" id="opt_mL" value="10" min="0" max="80" step="0.5"></div>' +
        '<div class="field pgs-setup-full" style="display:flex;gap:18px;flex-wrap:wrap;align-items:center;">' +
          '<label style="display:flex;align-items:center;gap:6px;font-size:12.5px;"><input type="checkbox" id="opt_pageBorder" checked> کادر دور صفحه</label>' +
          '<label style="display:flex;align-items:center;gap:6px;font-size:12.5px;"><input type="checkbox" id="opt_showPageNumbers" checked> شمارهٔ صفحه</label>' +
          '<label style="display:flex;align-items:center;gap:6px;font-size:12.5px;"><input type="checkbox" id="opt_repeatHeader" checked> تکرار سرستون جدول در هر صفحه</label>' +
          '<label style="display:flex;align-items:center;gap:6px;font-size:12.5px;"><input type="checkbox" id="opt_showScores" checked> نمایش ستون بارم</label>' +
        '</div>' +
        '<div class="field pgs-setup-full"><label>فاصلهٔ بین سؤالات:</label><select id="opt_questionSpacing">' +
          '<option value="compact">فشرده</option><option value="normal" selected>معمولی</option><option value="open">باز</option>' +
        '</select></div>' +
        '<div class="pgs-setup-hint">💡 این تنظیمات هم روی پیش‌نمایش اعمال می‌شوند هم روی چاپ — خروجی چاپ دقیقاً همان چیزی است که در پیش‌نمایش می‌بینید. با «💾 ذخیره آزمون (JSON)» همراه آزمون ذخیره می‌شوند.</div>' +
      '</div>' +
    '</div>';

  var DIALOG_HTML =
    '<div id="pgsPrintDlg" class="no-print">' +
      '<div class="pgs-dlg-box" role="dialog" aria-label="تنظیمات چاپ">' +
        '<h3>🖨 چاپ آزمون <button type="button" class="qtype-x" onclick="pgsClosePrintDlg()">✕</button></h3>' +
        '<div class="pgs-dlg-row"><b>نسخه:</b>' +
          '<label><input type="radio" name="pgsMode" value="student" checked> دانشجو</label>' +
          '<label><input type="radio" name="pgsMode" value="teacher"> استاد / کلید</label>' +
        '</div>' +
        '<div class="pgs-dlg-row"><b>بازهٔ صفحات:</b>' +
          '<label><input type="radio" name="pgsRange" value="all" checked> همه</label>' +
          '<label><input type="radio" name="pgsRange" value="range"> صفحات:</label>' +
          '<input type="text" id="pgsRangeVal" placeholder="مثلاً ۱،۳-۵" dir="ltr">' +
          '<label><input type="radio" name="pgsRange" value="current"> صفحهٔ جاری</label>' +
        '</div>' +
        '<div class="pgs-dlg-row"><b>تعداد نسخه:</b><input type="number" id="pgsCopies" value="1" min="1" max="20"></div>' +
        '<div class="pgs-dlg-row" style="color:#64748b;font-size:11.5px;">برای خروجی PDF در پنجرهٔ چاپ، مقصد <b>Save as PDF</b> را انتخاب کنید.</div>' +
        '<div class="pgs-dlg-actions">' +
          '<button type="button" class="ok" onclick="pgsDoPrintFromDlg()">چاپ</button>' +
          '<button type="button" class="cancel" onclick="pgsClosePrintDlg()">انصراف</button>' +
        '</div>' +
      '</div>' +
    '</div>';

  function ensureBaseDom() {
    if ($('pgsDock')) return;
    var dock = document.createElement('div'); dock.id = 'pgsDock'; document.body.appendChild(dock);
    var root = document.createElement('div'); root.id = 'pgsPrintRoot'; document.body.appendChild(root);
    var fr = document.createElement('iframe');
    fr.id = 'pgsPrintFrame'; fr.setAttribute('aria-hidden', 'true');
    fr.style.cssText = 'position:fixed;left:-20000px;top:0;width:800px;height:1000px;border:0;visibility:hidden;';
    document.body.appendChild(fr);
    var st = document.createElement('style'); st.id = 'pgs-dynpage'; document.head.appendChild(st);
    document.body.insertAdjacentHTML('beforeend', PAGESETUP_HTML);
    document.body.insertAdjacentHTML('beforeend', DIALOG_HTML);

    try {
      var btns = document.querySelector('.toolbar .btns');
      if (btns && !$('pgsSetupBtn')) {
        var b = document.createElement('button');
        b.type = 'button'; b.className = 'btn ghost'; b.id = 'pgsSetupBtn';
        b.textContent = '📐 تنظیمات صفحه';
        b.setAttribute('onclick', 'pgsToggleSetup()');
        var printBtn = btns.querySelector('[onclick="printStudent()"]');
        if (printBtn) btns.insertBefore(b, printBtn); else btns.appendChild(b);
      }
      var eye = document.querySelector('[onclick="togglePreviewWindow()"]');
      if (eye) { eye.textContent = '👁 پیش‌نمایش و چاپ'; eye.title = 'پیش‌نمایش حرفه‌ای با صفحه‌بندی واقعی (مثل Word)'; }
    } catch (e) {}

    var pc = $('printContent');
    if (pc && pc.parentNode !== dock) {
      pc.classList.remove('live-preview', 'in-window');
      dock.appendChild(pc);
    }
    bindSetupEvents();
  }

  function bindSetupEvents() {
    var paper = $('opt_paper'), preset = $('opt_marginPreset');
    function syncCustomState() {
      var isCustom = paper && paper.value === 'custom';
      /* V131 — عرض/ارتفاع فقط وقتی «اندازهٔ کاغذ = سفارشی» است دیده می‌شوند (و آن‌وقت برچسبِ «سفارشی» می‌گیرند) */
      ['opt_customW','opt_customH'].forEach(function (id) {
        var el = $(id); if (!el) return;
        el.disabled = !isCustom;
        var f = el.closest ? el.closest('.field') : null; if (f) f.classList.toggle('pgs-field-off', !isCustom);
        var lb = f ? f.querySelector('label') : null;
        if (lb) lb.textContent = (id === 'opt_customW' ? 'عرض' : 'ارتفاع') + (isCustom ? ' سفارشی' : '') + ' (mm):';
      });
    }
    window.__pgsSyncCustomState = syncCustomState;
    if (paper) paper.addEventListener('change', function () { syncCustomState(); saveSetup(); });
    if (preset) preset.addEventListener('change', function () {
      var p = MARGIN_PRESETS[preset.value];
      if (p) {
        $('opt_mT').value = p.t; $('opt_mB').value = p.b; $('opt_mR').value = p.r; $('opt_mL').value = p.l;
      }
      saveSetup();
    });
    ['opt_mT','opt_mB','opt_mR','opt_mL'].forEach(function (id) {
      var el = $(id);
      if (el) el.addEventListener('input', function () { if (preset) preset.value = 'custom'; });
    });
    ['opt_customW','opt_customH','opt_orientation','opt_baseFont','opt_pageBorder','opt_showPageNumbers',
     'opt_repeatHeader','opt_showScores','opt_questionSpacing'].forEach(function (id) {
      var el = $(id);
      if (el) { el.addEventListener('change', saveSetup); el.addEventListener('input', saveSetup); }
    });
    syncCustomState();
  }

  window.pgsToggleSetup = function (force) {
    ensureBaseDom();
    var p = $('pgsPageSetup'); if (!p) return;
    var show = (force === undefined) ? (p.style.display === 'none' || !p.style.display) : !!force;
    p.style.display = show ? 'block' : 'none';
  };

  /* ============================ موتور صفحه‌بندی ============================ */
  var pgsState = { paginating: false, zoom: 1, page: 1, thumbs: true, sheets: 0, anyTable: false };
  var cfgCache = null;
  function readCfgCached() { return cfgCache || (cfgCache = readCfg()); }

  function collectFlow(area) {
    var frag = document.createDocumentFragment();
    childArr(area).forEach(function (ch) {
      if (ch.classList.contains('pgs-page-label')) { ch.remove(); return; }
      if (ch.classList.contains('pgs-sheet')) {
        var body = ch.querySelector('.pgs-sheet-body');
        if (body) childArr(body).forEach(function (x) { frag.appendChild(x); });
        ch.remove();
      } else {
        frag.appendChild(ch);
      }
    });
    return frag;
  }

  function makeSheet(cfg, no) {
    var el = document.createElement('div');
    el.className = 'pgs-sheet';
    el.dataset.page = String(no);
    el.style.width = cfg.W + 'mm';
    el.style.height = cfg.H + 'mm';
    el.style.padding = cfg.m.t + 'mm ' + cfg.m.r + 'mm ' + cfg.m.b + 'mm ' + cfg.m.l + 'mm';
    el.style.setProperty('--pgs-font', String(cfg.font));
    var frame = '';
    if (cfg.border) {
      frame = '<div class="pgs-frame" style="top:' + cfg.m.t + 'mm;bottom:' + cfg.m.b + 'mm;right:' + cfg.m.r + 'mm;left:' + cfg.m.l + 'mm;"></div>';
    }
    el.innerHTML = frame + '<div class="pgs-sheet-body"></div>' +
      '<div class="pgs-pageno" style="bottom:' + Math.max(0.8, cfg.m.b / 2 - 1.6) + 'mm;"></div>';
    var label = document.createElement('div');
    label.className = 'pgs-page-label pgs-screen-only pgs-no-print';
    label.textContent = 'صفحهٔ ' + faNum(no);
    return { el: el, body: el.querySelector('.pgs-sheet-body'), label: label, hasUnits: false, tableCount: 0 };
  }

  function overflows(sheet) {
    var b = sheet.body;
    return b.scrollHeight > b.clientHeight + 2;
  }

  function openTableOn(sheet, tpl) {
    /* اگر صفحه از قبل جدول سؤالات دارد (ادغام واحدهای متوالی)، همان را ادامه بده */
    var existing = sheet.body.lastElementChild;
    if (existing && existing.tagName === 'TABLE' && existing.classList.contains('questions-print-table')) {
      var etb = existing.querySelector('tbody');
      if (etb) return { sheet: sheet, table: existing, tbody: etb };
    }
    var t = document.createElement('table');
    t.className = tpl.className;
    var cg = tpl.querySelector('colgroup');
    if (cg) t.appendChild(cg.cloneNode(true));
    var th = tpl.querySelector('thead');
    if (th && (readCfgCached().repeatHeader || !pgsState.anyTable)) t.appendChild(th.cloneNode(true));
    pgsState.anyTable = true;
    var tb = document.createElement('tbody');
    t.appendChild(tb);
    sheet.body.appendChild(t);
    sheet.tableCount++;
    return { sheet: sheet, table: t, tbody: tb };
  }

  function newSheet(sheets, area, cfg) {
    var s = makeSheet(cfg, sheets.length + 1);
    area.appendChild(s.el);
    area.appendChild(s.label);
    sheets.push(s);
    return s;
  }

  /* ---------- تقسیم سطر بلند سؤال بین دو صفحه ---------- */
  function makeContinuationRow(tr) {
    var tr2 = document.createElement('tr');
    tr2.className = 'question-print-row q-cont';
    var tds = tr.children;
    for (var i = 0; i < tds.length; i++) {
      var td = document.createElement('td');
      td.className = tds[i].className;
      if (tds[i].classList.contains('question-main-td')) td.style.cssText = tds[i].style.cssText;
      tr2.appendChild(td);
    }
    return tr2;
  }

  function movableKids(mainTd) {
    return childArr(mainTd).filter(function (k) { return !k.classList.contains('question-sep-drag'); });
  }

  /* V130.1 — متنِ بلندِ سؤال (.q-rich-content) که خودش از یک صفحه بلندتر است، قبلاً «آخرین تکه» بود و
     بریده (clip) می‌شد. اکنون از انتها تکه‌تکه (چند کلمه از تکه‌های .txt، یا یک گرهٔ کامل مثل فرمول/شکل)
     به ادامهٔ همان بلوک در سطرِ صفحهٔ بعد منتقل می‌شود. */
  var RICH_WORDS_PER_STEP = 6;
  function richCanSplit(k) {
    if (!k.classList || !k.classList.contains('q-rich-content')) return false;
    var last = k.lastChild;
    while (last && last.nodeType === 3 && !last.textContent.trim()) last = last.previousSibling;
    if (!last) return false;
    if (k.childNodes.length > 1) return true;
    var t = last.nodeType === 3 ? last.textContent : (last.classList && last.classList.contains('txt') ? last.textContent : '');
    return t.trim().split(/\s+/).length > 1;
  }
  function splitRichPiece(k, tr2main) {
    var cont = tr2main.querySelector('.q-rich-content');
    if (!cont) { cont = k.cloneNode(false); cont.classList.add('q-rich-cont'); tr2main.insertBefore(cont, tr2main.firstChild); }
    var last = k.lastChild;
    while (last && last.nodeType === 3 && !last.textContent.trim()) { var ws = last; last = last.previousSibling; k.removeChild(ws); }
    if (!last) return false;
    var isTxt = last.nodeType === 1 && last.classList.contains('txt');
    var isText = last.nodeType === 3;
    if (isTxt || isText) {
      var full = last.textContent, words = full.split(/(\s+)/);
      /* تعداد کلمه‌های واقعی (نه جداکننده) */
      var real = 0; for (var i = 0; i < words.length; i++) if (words[i].trim()) real++;
      if (real > 1) {
        var take = Math.min(RICH_WORDS_PER_STEP, real - 1), cut = words.length, n = 0;
        for (var j = words.length - 1; j >= 0 && n < take; j--) { if (words[j].trim()) n++; cut = j; }
        var head = words.slice(0, cut).join(''), tail = words.slice(cut).join('');
        if (isTxt) {
          var sp = last.cloneNode(false); sp.textContent = tail;
          if (last.dataset && last.dataset.off !== undefined) sp.dataset.off = String(Number(last.dataset.off) + head.length);
          last.textContent = head;
          /* اگر اولین تکهٔ ادامه همان span بود (ادامهٔ همین تکه)، ادغام کن تا آفست‌ها پیوسته بمانند */
          var f = cont.firstChild;
          if (f && f.nodeType === 1 && f.classList.contains('txt') && f.dataset.off === String(Number(sp.dataset.off) + tail.length) && f.getAttribute('style') === sp.getAttribute('style')) { f.textContent = tail + f.textContent; f.dataset.off = sp.dataset.off; }
          else cont.insertBefore(sp, cont.firstChild);
        } else {
          last.textContent = head;
          var f2 = cont.firstChild;
          if (f2 && f2.nodeType === 3) f2.textContent = tail + f2.textContent; else cont.insertBefore(document.createTextNode(tail), cont.firstChild);
        }
        return true;
      }
    }
    cont.insertBefore(last, cont.firstChild);
    return true;
  }

  function moveLastPiece(mainTd, tr2main) {
    var kids = movableKids(mainTd);
    for (var i = kids.length - 1; i >= 0; i--) {
      var k = kids[i];
      if (richCanSplit(k)) return splitRichPiece(k, tr2main);
      if (k.classList.contains('answer-space') && k.classList.contains('lined')) {
        var lines = k.querySelectorAll('.answer-line-row');
        if (lines.length > 1) {
          var space2 = tr2main.querySelector('.answer-space.lined');
          if (!space2) {
            space2 = k.cloneNode(false);
            tr2main.insertBefore(space2, tr2main.firstChild);
          }
          space2.insertBefore(lines[lines.length - 1], space2.firstChild);
          return true;
        }
      }
      tr2main.insertBefore(k, tr2main.firstChild);
      return true;
    }
    return false;
  }

  /* tr داخل tbody جاری است و صفحه را سرریز کرده؛ تکه‌تکه به سطر دوم منتقل می‌شود */
  function splitOversizeRow(ctx, sheets, area, cfg) {
    var tr = ctx.tbody.lastElementChild;
    if (!tr) return null;
    var main = tr.querySelector('.question-main-td');
    if (!main) return null;
    var tr2 = makeContinuationRow(tr);
    var tr2main = tr2.querySelector('.question-main-td');
    var guard = 0;
    while (overflows(ctx.sheet) && guard++ < 3000) {
      var mk = movableKids(main);
      if (mk.length === 0) break;
      if (mk.length === 1) {
        /* آخرین تکه: فقط در صورتی بشکن که خط‌های پاسخ داشته باشد یا متنِ چندکلمه‌ای/چندگرهی باشد (V130.1)؛ وگرنه clip */
        var only = mk[0];
        var canSplitLines = only.classList.contains('answer-space') &&
          only.querySelectorAll('.answer-line-row').length > 1;
        if (!canSplitLines && !richCanSplit(only)) break;
      }
      if (!moveLastPiece(main, tr2main)) break;
    }
    if (!tr2main.children.length) return null;
    var s2 = newSheet(sheets, area, cfg);
    var ctx2 = openTableOn(s2, cfg.__qTpl || ctx.table);
    ctx2.tbody.appendChild(tr2);
    if (overflows(s2)) {
      var more = splitOversizeRow(ctx2, sheets, area, cfg);
      if (more) return more;
    }
    return ctx2;
  }

  /* ---------- تقسیم بلوک answer-key ---------- */
  function splitAnswerKey(node, sheet, sheets, area, cfg) {
    var tbl = node.querySelector('table');
    if (!tbl) return false;
    var guard = 0;
    while (overflows(sheet) && guard++ < 400) {
      var rows = tbl.querySelectorAll('tr');
      if (rows.length <= 2) break;
      var last = rows[rows.length - 1];
      var cont = sheet.__akCont;
      if (!cont) {
        var s2 = newSheet(sheets, area, cfg);
        var div2 = node.cloneNode(false);
        var tbl2 = tbl.cloneNode(false);
        var cg2 = tbl.querySelector('colgroup'); if (cg2) tbl2.appendChild(cg2.cloneNode(true));
        div2.appendChild(tbl2);
        s2.body.appendChild(div2);
        cont = { sheet: s2, tbl: tbl2 };
        sheet.__akCont = cont;
      }
      cont.tbl.insertBefore(last, cont.tbl.firstChild);
      if (overflows(cont.sheet)) {
        if (cont.tbl.querySelectorAll('tr').length > 2) {
          var s3 = newSheet(sheets, area, cfg);
          var div3 = node.cloneNode(false);
          var tbl3 = tbl.cloneNode(false);
          div3.appendChild(tbl3);
          s3.body.appendChild(div3);
          tbl3.insertBefore(cont.tbl.lastElementChild, tbl3.firstChild);
          cont.sheet = s3; cont.tbl = tbl3;
          sheet.__akCont = cont;
        } else break;
      }
    }
    var tail = node.querySelector('div[style*="text-align:center"]');
    if (tail && sheet.__akCont) {
      var host = sheet.__akCont.sheet.body.querySelector('div');
      if (host) host.appendChild(tail);
    }
    return true;
  }

  function placeNode(node, cur, sheets, area, cfg) {
    cur.body.appendChild(node);
    if (!overflows(cur)) { cur.hasUnits = true; return cur; }
    cur.body.removeChild(node);
    if (cur.body.children.length > 0) {
      cur = newSheet(sheets, area, cfg);
      cur.body.appendChild(node);
      cur.hasUnits = true;
      if (overflows(cur) && node.classList.contains('answer-key')) splitAnswerKey(node, cur, sheets, area, cfg);
    } else {
      cur.body.appendChild(node);
      cur.hasUnits = true;
      if (overflows(cur) && node.classList.contains('answer-key')) splitAnswerKey(node, cur, sheets, area, cfg);
    }
    return cur;
  }

  function willTableJump(cur, tpl, firstRow) {
    var probe = openTableOn(cur, tpl);
    probe.tbody.appendChild(firstRow);
    var bad = overflows(cur);
    probe.tbody.removeChild(firstRow);
    probe.table.remove();
    cur.tableCount--;
    pgsState.anyTable = false;
    return bad;
  }

  function placeRows(tpl, rows, cur, sheets, area, cfg) {
    var ctx = null;
    for (var i = 0; i < rows.length; i++) {
      var row = rows[i];
      if (!ctx) {
        /* keep-with-next: اگر intro آخرین بلوک صفحه است و جدول جا نمی‌شود، intro هم منتقل شود */
        var lastPrev = cur.body.lastElementChild;
        if (lastPrev && lastPrev.classList.contains('exam-intro') && i === 0 &&
            cur.body.scrollHeight > 2 && willTableJump(cur, tpl, row)) {
          cur.body.removeChild(lastPrev);
          cur = newSheet(sheets, area, cfg);
          cur.body.appendChild(lastPrev);
          cur.hasUnits = true;
        }
        ctx = openTableOn(cur, tpl);
      }
      ctx.tbody.appendChild(row);
      cur.hasUnits = true;
      if (overflows(ctx.sheet)) {
        var hadOther = ctx.tbody.children.length > 1 || ctx.sheet.body.children.length > 1;
        ctx.tbody.removeChild(row);
        if (hadOther) {
          /* V130.1 — اگر سطر حتی در یک صفحهٔ خالی هم جا نمی‌شود، همین‌جا (زیرِ سربرگ/سؤال‌های قبلی) شروع
             و شکسته شود؛ قبلاً اول به صفحهٔ خالیِ بعدی می‌رفت و صفحهٔ جاری نیمه‌خالی می‌ماند. */
          var probe = newSheet(sheets, area, cfg);
          var pctx = openTableOn(probe, tpl);
          pctx.tbody.appendChild(row);
          var tooTall = overflows(probe);
          pctx.tbody.removeChild(row);
          probe.el.remove(); probe.label.remove(); sheets.pop();
          var used = 0; childArr(ctx.sheet.body).forEach(function (c) { used += c.offsetHeight; });
          if (tooTall && ctx.sheet.body.clientHeight - used > 120) {
            ctx.tbody.appendChild(row);
            var c1 = splitOversizeRow(ctx, sheets, area, cfg);
            if (c1) { ctx = c1; cur = c1.sheet; }
            continue;
          }
          cur = newSheet(sheets, area, cfg);
          ctx = openTableOn(cur, tpl);
          ctx.tbody.appendChild(row);
          cur.hasUnits = true;
          if (overflows(ctx.sheet)) {
            var c2 = splitOversizeRow(ctx, sheets, area, cfg);
            if (c2) { ctx = c2; cur = c2.sheet; }
          }
        } else {
          ctx.tbody.appendChild(row);
          var c3 = splitOversizeRow(ctx, sheets, area, cfg);
          if (c3) { ctx = c3; cur = c3.sheet; }
        }
      }
    }
    return cur;
  }

  function paginate() {
    if (pgsState.paginating) return;
    var area = $('previewArea');
    if (!area) return;
    pgsState.paginating = true;
    cfgCache = null;
    pgsState.anyTable = false;
    try {
      var cfg = readCfgCached();
      var dp = $('pgs-dynpage');
      if (dp) dp.textContent = '@page{size:' + cfg.W + 'mm ' + cfg.H + 'mm;margin:0;}';
      area.style.setProperty('--pgs-font', String(cfg.font));

      var flow = collectFlow(area);
      area.innerHTML = '';
      area.classList.add('pgs-area');

      /* فهرست واحدها: جدول‌های سؤالِ متوالی (بقایای صفحه‌بندی قبلی) در یک جدول ادغام می‌شوند */
      var units = [];
      childArr(flow).forEach(function (node) {
        if (node.tagName === 'TABLE' && node.classList.contains('questions-print-table')) {
          var rows = Array.prototype.slice.call(node.querySelectorAll('tbody > tr'));
          var last = units[units.length - 1];
          if (last && last.kind === 'qtable') last.rows = last.rows.concat(rows);
          else units.push({ kind: 'qtable', tpl: node, rows: rows });
        } else {
          units.push({ kind: 'node', node: node });
        }
      });

      var sheets = [];
      var cur = newSheet(sheets, area, cfg);

      units.forEach(function (u) {
        if (u.kind === 'qtable') {
          cfg.__qTpl = u.tpl;
          cur = placeRows(u.tpl, u.rows, cur, sheets, area, cfg);
        } else {
          cur = placeNode(u.node, cur, sheets, area, cfg);
        }
      });

      finalizeSheets(sheets, cfg);
      pgsState.sheets = sheets.length;
      updateViewer();
    } catch (e) {
      if (window.console) console.error('PGS paginate error:', e);
    } finally {
      pgsState.paginating = false;
    }
  }

  function finalizeSheets(sheets, cfg) {
    var total = sheets.length;
    sheets.forEach(function (s, i) {
      var pn = s.el.querySelector('.pgs-pageno');
      if (pn) pn.textContent = cfg.pageNumbers ? ('صفحهٔ ' + faNum(i + 1) + ' از ' + faNum(total)) : '';
      s.label.textContent = 'صفحهٔ ' + faNum(i + 1) + ' از ' + faNum(total);
    });
  }

  var pagTimer = null;
  function schedulePaginate(delay) {
    clearTimeout(pagTimer);
    pagTimer = setTimeout(function () {
      paginate();
      /* امواج باز-صفحه‌بندی: بعد از fit شدن فرمول‌ها/تصاویر ارتفاع‌ها عوض می‌شود */
      setTimeout(function () { if (!pgsState.paginating) paginate(); }, 160);
      setTimeout(function () { if (!pgsState.paginating) paginate(); }, 550);
      setTimeout(function () { if (!pgsState.paginating) paginate(); }, 1300);
    }, delay || 0);
  }

  /* ============================ بینندهٔ پیش‌نمایش ============================ */
  function viewerOpen() { return !!$('pgsViewer'); }
  function isMobile() {
    try { return window.matchMedia('(max-width:820px)').matches; } catch (e) { return false; }
  }

  function buildViewer() {
    var v = document.createElement('div');
    v.id = 'pgsViewer';
    v.innerHTML =
      '<div class="pgs-ribbon">' +
        '<button type="button" class="pgs-btn" onclick="closePreviewWindow()" title="بستن (Esc)">✕<span class="pgs-t"> بستن</span></button>' +
        '<span class="pgs-sep"></span>' +
        '<span class="pgs-menu-wrap">' +
          '<button type="button" class="pgs-btn primary" onclick="pgsToggleMenu(event)">🖨<span class="pgs-t"> چاپ ▾</span></button>' +
          '<span class="pgs-menu" id="pgsPrintMenu">' +
            '<button type="button" onclick="pgsPrintNow(\'student\')">🖨 چاپ نسخهٔ دانشجو</button>' +
            '<button type="button" onclick="pgsPrintNow(\'teacher\')">✅ چاپ نسخهٔ استاد / کلید</button>' +
            '<button type="button" onclick="pgsOpenPrintDlg()">⚙️ تنظیمات چاپ (بازه / تعداد نسخه)…</button>' +
          '</span>' +
        '</span>' +
        '<button type="button" class="pgs-btn" id="pgsModeBtn" onclick="pgsToggleMode()" title="جابجایی بین نسخهٔ دانشجو و استاد">👤<span class="pgs-t"> نسخه: دانشجو</span></button>' +
        '<button type="button" class="pgs-btn" onclick="pgsToggleSetup()" title="کاغذ، جهت، حاشیه، کادر، فونت…">📐<span class="pgs-t"> تنظیمات صفحه</span></button>' +
        '<span class="pgs-sep"></span>' +
        '<button type="button" class="pgs-btn icon" onclick="pgsZoomBy(-0.1)" title="کوچک‌نمایی (Ctrl+−)">−</button>' +
        '<span class="pgs-zoomval" id="pgsZoomVal" title="برای بزرگ‌نمایی ۱۰۰٪ کلیک کنید" onclick="pgsSetZoom(1)">۱۰۰٪</span>' +
        '<button type="button" class="pgs-btn icon" onclick="pgsZoomBy(0.1)" title="بزرگ‌نمایی (Ctrl++)">+</button>' +
        '<button type="button" class="pgs-btn" onclick="pgsFit(\'width\')" title="به اندازهٔ عرض (Ctrl+0)">↔<span class="pgs-t"> عرض صفحه</span></button>' +
        '<button type="button" class="pgs-btn" onclick="pgsFit(\'page\')" title="نمایش کامل یک صفحه">⛶<span class="pgs-t"> کل صفحه</span></button>' +
        '<span class="pgs-sep"></span>' +
        '<span class="pgs-pageind">' +
          '<button type="button" class="pgs-btn icon" onclick="pgsGoPage(-1)" title="صفحهٔ قبل (PageUp)">◀</button>' +
          '<input type="text" id="pgsPageInput" dir="ltr" onkeydown="if(event.key===\'Enter\')pgsGoToInput()">' +
          '<span id="pgsPageOf">از ۱</span>' +
          '<button type="button" class="pgs-btn icon" onclick="pgsGoPage(1)" title="صفحهٔ بعد (PageDown)">▶</button>' +
        '</span>' +
        '<span class="pgs-spacer"></span>' +
        '<button type="button" class="pgs-btn" id="pgsThumbsBtn" onclick="pgsToggleThumbs()" title="نمایش بندانگشتی صفحات">🗂<span class="pgs-t"> بندانگشتی</span></button>' +
        '<button type="button" class="pgs-btn" onclick="pgsFullscreen()" title="تمام‌صفحه">⤢<span class="pgs-t"> تمام‌صفحه</span></button>' +
      '</div>' +
      '<div class="pgs-main">' +
        '<div class="pgs-thumbs" id="pgsThumbs"></div>' +
        '<div class="pgs-canvas-wrap" id="pgsCanvasWrap">' +
          '<div class="pgs-canvas-sizer" id="pgsCanvasSizer">' +
            '<div class="pgs-canvas" id="pgsCanvas"></div>' +
          '</div>' +
        '</div>' +
      '</div>' +
      '<div class="pgs-status" id="pgsStatus"></div>';
    document.body.appendChild(v);
    $('pgsCanvas').appendChild($('printContent'));

    var wrap = $('pgsCanvasWrap');
    wrap.addEventListener('scroll', onCanvasScroll, { passive: true });
    wrap.addEventListener('wheel', function (e) {
      if (e.ctrlKey) { e.preventDefault(); pgsZoomBy(e.deltaY < 0 ? 0.1 : -0.1); }
    }, { passive: false });
    bindTouchGestures(wrap);
    syncThumbClasses();
    window.addEventListener('resize', onWinResize);
  }

  /* ---------- ژست‌های لمسی: پینچ‌زوم و دوبار-ضربه ---------- */
  function bindTouchGestures(wrap) {
    var pts = {};
    var pinchBase = null;
    wrap.addEventListener('pointerdown', function (e) {
      if (e.pointerType !== 'touch') return;
      pts[e.pointerId] = { x: e.clientX, y: e.clientY };
      var ids = Object.keys(pts);
      if (ids.length === 2) {
        var a = pts[ids[0]], b = pts[ids[1]];
        pinchBase = { dist: Math.hypot(a.x - b.x, a.y - b.y), zoom: pgsState.zoom };
        wrap.classList.add('pgs-pinch');
      }
    });
    wrap.addEventListener('pointermove', function (e) {
      if (!pts[e.pointerId]) return;
      pts[e.pointerId] = { x: e.clientX, y: e.clientY };
      var ids = Object.keys(pts);
      if (ids.length === 2 && pinchBase && pinchBase.dist > 8) {
        var a = pts[ids[0]], b = pts[ids[1]];
        var d = Math.hypot(a.x - b.x, a.y - b.y);
        pgsSetZoom(pinchBase.zoom * d / pinchBase.dist);
      }
    });
    function endPt(e) {
      delete pts[e.pointerId];
      if (Object.keys(pts).length < 2) { pinchBase = null; wrap.classList.remove('pgs-pinch'); }
    }
    wrap.addEventListener('pointerup', endPt);
    wrap.addEventListener('pointercancel', endPt);

    /* دوبار ضربه: جابجایی بین «به اندازهٔ عرض» و «۱۰۰٪» */
    var lastTap = 0, lastX = 0, lastY = 0;
    wrap.addEventListener('pointerup', function (e) {
      if (e.pointerType !== 'touch') return;
      var now = Date.now();
      if (now - lastTap < 320 && Math.hypot(e.clientX - lastX, e.clientY - lastY) < 40) {
        lastTap = 0;
        if (pgsState.zoom > 0.9) pgsFit('width'); else pgsSetZoom(1);
      } else { lastTap = now; lastX = e.clientX; lastY = e.clientY; }
    });
  }

  function destroyViewer() {
    var v = $('pgsViewer');
    if (!v) return;
    var dock = $('pgsDock');
    var pc = $('printContent');
    if (pc && dock) dock.appendChild(pc);
    v.remove();
    window.removeEventListener('resize', onWinResize);
    document.body.classList.remove('pgs-viewer-open');
  }

  function onWinResize() {
    if (!viewerOpen()) return;
    applyZoom();
    syncThumbClasses();
  }

  function sheetPxW() {
    var s = document.querySelector('#previewArea .pgs-sheet');
    return s ? s.offsetWidth : 794;
  }
  function sheetPxH() {
    var s = document.querySelector('#previewArea .pgs-sheet');
    return s ? s.offsetHeight : 1123;
  }

  function applyZoom() {
    var canvas = $('pgsCanvas'), sizer = $('pgsCanvasSizer');
    if (!canvas || !sizer) return;
    var z = pgsState.zoom;
    canvas.style.transform = 'scale(' + z + ')';
    sizer.style.width = Math.ceil(sheetPxW() * z) + 'px';
    sizer.style.height = Math.ceil(canvas.offsetHeight * z) + 'px';
    var zv = $('pgsZoomVal');
    if (zv) zv.textContent = faNum(Math.round(z * 100)) + '٪';
  }

  window.pgsSetZoom = function (z) {
    pgsState.zoom = clamp(z, 0.25, 3);
    try { localStorage.setItem(LS_ZOOM, String(pgsState.zoom)); } catch (e) {}
    applyZoom();
  };
  window.pgsZoomBy = function (d) { pgsSetZoom(Math.round((pgsState.zoom + d) * 10) / 10); };
  window.pgsFit = function (mode) {
    var wrap = $('pgsCanvasWrap');
    if (!wrap) return;
    var zw = (wrap.clientWidth - 56) / sheetPxW();
    var zh = (wrap.clientHeight - 56) / sheetPxH();
    pgsSetZoom(mode === 'page' ? Math.min(zw, zh) : zw);
  };

  function sheetList() {
    return Array.prototype.slice.call(document.querySelectorAll('#previewArea .pgs-sheet'));
  }

  var thumbsTimer = null;
  function updateViewer() {
    if (!viewerOpen()) return;
    var sheets = sheetList();
    var inp = $('pgsPageInput'), of = $('pgsPageOf');
    if (inp && document.activeElement !== inp) inp.value = faNum(clamp(pgsState.page, 1, Math.max(1, sheets.length)));
    if (of) of.textContent = 'از ' + faNum(sheets.length);
    var mb = $('pgsModeBtn');
    var teacher = false;
    try { teacher = (typeof printMode !== 'undefined') && printMode === 'teacher'; } catch (e) {}
    if (mb) {
      mb.innerHTML = (teacher ? '✅' : '👤') + '<span class="pgs-t"> ' +
        (teacher ? 'نسخه: استاد / کلید' : 'نسخه: دانشجو') + '</span>';
      mb.classList.toggle('on', teacher);
    }
    var cfg = readCfg();
    var qn = 0, tot = '—';
    try { qn = (typeof questions !== 'undefined' && questions) ? questions.length : 0; } catch (e) {}
    try { tot = (typeof calcTotalScore === 'function' && typeof formatScore === 'function') ? formatScore(calcTotalScore()) : '—'; } catch (e) {}
    var st = $('pgsStatus');
    if (st) {
      st.innerHTML =
        '<span>📄 <b>' + faNum(sheets.length) + '</b> صفحه</span><span class="pgs-dot"></span>' +
        '<span>❓ <b>' + faNum(qn) + '</b> سؤال</span><span class="pgs-dot"></span>' +
        '<span>💯 جمع بارم: <b>' + tot + '</b></span><span class="pgs-dot"></span>' +
        '<span class="pgs-hide-m">📃 ' + cfg.paperLabel + (cfg.orient === 'landscape' ? ' افقی' : ' عمودی') + '</span><span class="pgs-dot pgs-hide-m"></span>' +
        '<span class="pgs-hide-m">↔ حاشیه: ' + faNum(cfg.m.t) + '/' + faNum(cfg.m.r) + '/' + faNum(cfg.m.b) + '/' + faNum(cfg.m.l) + ' mm</span><span class="pgs-dot pgs-hide-m"></span>' +
        '<span>🔍 ' + faNum(Math.round(pgsState.zoom * 100)) + '٪</span>' +
        (teacher ? '<span class="pgs-dot"></span><span style="color:#4ade80">حالت نمایش: نسخهٔ استاد</span>' : '');
    }
    applyZoom();
    clearTimeout(thumbsTimer);
    thumbsTimer = setTimeout(function () {
      var box = $('pgsThumbs');
      if (!box || !viewerOpen()) return;
      var visible = isMobile() ? box.classList.contains('open') : !box.classList.contains('hidden');
      if (visible) buildThumbs(sheetList());
    }, 260);
  }

  /* کلاس‌های بندانگشتی بر اساس دسکتاپ/موبایل: دسکتاپ=ستون ثابت، موبایل=کشوی کناری */
  function syncThumbClasses() {
    var box = $('pgsThumbs');
    if (!box) return;
    if (isMobile()) {
      box.classList.remove('hidden');
      if (!box.classList.contains('open')) box.classList.remove('open');
    } else {
      box.classList.remove('open');
      box.classList.toggle('hidden', !pgsState.thumbs);
    }
  }

  function buildThumbs(sheets) {
    var box = $('pgsThumbs');
    if (!box || !viewerOpen()) return;
    box.innerHTML = '';
    var TW = 138;
    sheets.forEach(function (el, i) {
      var k = TW / Math.max(1, el.offsetWidth);
      var th = document.createElement('div');
      th.className = 'pgs-thumb' + (i + 1 === pgsState.page ? ' active' : '');
      th.title = 'صفحهٔ ' + faNum(i + 1);
      var sz = document.createElement('div');
      sz.className = 'pgs-thumb-sizer';
      sz.style.width = TW + 'px';
      sz.style.height = Math.round(el.offsetHeight * k) + 'px';
      sz.style.position = 'relative';
      sz.style.overflow = 'hidden';
      var cl = el.cloneNode(true);
      cl.style.transform = 'scale(' + k + ')';
      cl.style.transformOrigin = 'top right';
      cl.style.boxShadow = 'none';
      cl.style.position = 'absolute';
      cl.style.top = '0';
      cl.style.right = '0';
      sz.appendChild(cl);
      var cap = document.createElement('span');
      cap.className = 'pgs-thumb-cap';
      cap.textContent = faNum(i + 1);
      th.appendChild(sz); th.appendChild(cap);
      th.addEventListener('click', function () {
        goToSheet(i + 1);
        if (isMobile()) box.classList.remove('open');
      });
      box.appendChild(th);
    });
  }

  window.pgsToggleThumbs = function () {
    var box = $('pgsThumbs');
    if (!box) return;
    if (isMobile()) {
      var open = !box.classList.contains('open');
      box.classList.toggle('open', open);
      if (open) buildThumbs(sheetList());
    } else {
      pgsState.thumbs = !pgsState.thumbs;
      var b = $('pgsThumbsBtn');
      if (b) b.classList.toggle('on', pgsState.thumbs);
      box.classList.toggle('hidden', !pgsState.thumbs);
      if (pgsState.thumbs) buildThumbs(sheetList());
    }
  };

  function goToSheet(n) {
    var sheets = sheetList();
    n = clamp(n, 1, sheets.length);
    pgsState.page = n;
    var wrap = $('pgsCanvasWrap'), s = sheets[n - 1];
    if (wrap && s) {
      var top = s.getBoundingClientRect().top - wrap.getBoundingClientRect().top + wrap.scrollTop;
      wrap.scrollTo({ top: Math.max(0, top - 20), behavior: 'smooth' });
    }
    var inp = $('pgsPageInput');
    if (inp) inp.value = faNum(n);
    Array.prototype.forEach.call(document.querySelectorAll('.pgs-thumb'), function (t, i) {
      t.classList.toggle('active', i + 1 === n);
    });
  }
  window.pgsGoPage = function (d) { goToSheet(pgsState.page + d); };
  window.pgsGoToInput = function () {
    var v = parseInt(enNum($('pgsPageInput').value), 10);
    if (isFinite(v)) goToSheet(v);
  };

  var spyRaf = null;
  function onCanvasScroll() {
    if (spyRaf) return;
    spyRaf = requestAnimationFrame(function () {
      spyRaf = null;
      var wrap = $('pgsCanvasWrap');
      if (!wrap || !viewerOpen()) return;
      var wr = wrap.getBoundingClientRect();
      var sheets = sheetList();
      var cur = 1;
      for (var i = 0; i < sheets.length; i++) {
        var r = sheets[i].getBoundingClientRect();
        if (r.top - wr.top <= wr.height * 0.4 && r.bottom - wr.top > 0) cur = i + 1;
      }
      if (cur !== pgsState.page) {
        pgsState.page = cur;
        var inp = $('pgsPageInput');
        if (inp && document.activeElement !== inp) inp.value = faNum(cur);
        Array.prototype.forEach.call(document.querySelectorAll('.pgs-thumb'), function (t, i) {
          t.classList.toggle('active', i + 1 === cur);
        });
      }
    });
  }

  window.pgsToggleMenu = function (e) {
    if (e) e.stopPropagation();
    var m = $('pgsPrintMenu');
    if (m) m.classList.toggle('open');
  };
  document.addEventListener('click', function (e) {
    var m = $('pgsPrintMenu');
    if (m && m.classList.contains('open') && !(e.target.closest && e.target.closest('.pgs-menu-wrap'))) {
      m.classList.remove('open');
    }
  });

  window.pgsToggleMode = function () {
    var teacher = false;
    try { teacher = printMode === 'teacher'; } catch (e) {}
    try { printMode = teacher ? 'student' : 'teacher'; } catch (e) {}
    try { window.renderPreview(); } catch (e) {}
  };

  window.pgsFullscreen = function () {
    try {
      if (!document.fullscreenElement) document.documentElement.requestFullscreen();
      else document.exitFullscreen();
    } catch (e) {}
  };

  /* ============================ دیالوگ چاپ ============================ */
  window.pgsOpenPrintDlg = function () {
    var m = $('pgsPrintMenu'); if (m) m.classList.remove('open');
    var d = $('pgsPrintDlg'); if (d) d.classList.add('open');
  };
  window.pgsClosePrintDlg = function () {
    var d = $('pgsPrintDlg'); if (d) d.classList.remove('open');
  };

  function parseRange(str, total) {
    var set = {};
    String(str || '').split(/[،,;]+/).forEach(function (part) {
      part = enNum(part).trim();
      if (!part) return;
      var m = part.match(/^(\d+)\s*-\s*(\d+)$/);
      if (m) {
        var a = clamp(parseInt(m[1], 10), 1, total), b = clamp(parseInt(m[2], 10), 1, total);
        for (var i = Math.min(a, b); i <= Math.max(a, b); i++) set[i] = 1;
      } else if (/^\d+$/.test(part)) {
        var p = clamp(parseInt(part, 10), 1, total);
        set[p] = 1;
      }
    });
    return Object.keys(set).length ? set : null;
  }

  window.pgsDoPrintFromDlg = function () {
    var mode = (document.querySelector('input[name="pgsMode"]:checked') || {}).value || 'student';
    var rangeKind = (document.querySelector('input[name="pgsRange"]:checked') || {}).value || 'all';
    var copies = clamp(parseInt(enNum($('pgsCopies').value), 10) || 1, 1, 20);
    var range = null;
    if (rangeKind === 'range') {
      range = parseRange($('pgsRangeVal').value, pgsState.sheets || sheetList().length);
      if (!range) { alert('بازهٔ صفحات معتبر نیست. نمونه: ۱،۳-۵'); return; }
    } else if (rangeKind === 'current') {
      range = {}; range[pgsState.page || 1] = 1;
    }
    pgsClosePrintDlg();
    doPrint({ mode: mode, range: range, copies: copies });
  };

  window.pgsPrintNow = function (mode) {
    var m = $('pgsPrintMenu'); if (m) m.classList.remove('open');
    doPrint({ mode: mode || 'student' });
  };

  /* ============================ لولهٔ چاپ ============================ */
  function cleanSheetClone(el) {
    var c = el.cloneNode(true);
    c.querySelectorAll('.question-sep-drag,.fig-resize-handle,.fig-move-hint,.fig-size-badge,.qimg-h,.qimg-tip,.qmf-del,.qmf-edit,.pgs-screen-only,.pgs-no-print')
      .forEach(function (x) { x.remove(); });
    c.querySelectorAll('.selected,.sel,.is-on')
      .forEach(function (x) { x.classList.remove('selected', 'sel', 'is-on'); });
    c.style.boxShadow = 'none';
    c.style.margin = '0';
    return c;
  }

  var PRINT_IFRAME_CSS =
    '<style id="pgs-iframe-print">' +
      'html,body{margin:0!important;padding:0!important;background:#fff!important;}' +
      'body::before,body::after{content:none!important;display:none!important;}' +
      '.pgs-sheet{margin:0!important;box-shadow:none!important;break-after:page;page-break-after:page;}' +
      '.pgs-sheet:last-child{break-after:auto;page-break-after:auto;}' +
      '.question-sep-drag,.fig-resize-handle,.fig-move-hint,.fig-size-badge,.qimg-h,.qimg-tip,.qmf-del,.qmf-edit,.pgs-screen-only,.pgs-no-print{display:none!important;}' +
      '.interactive-figure{outline:none!important;}' +
    '</style>';

  function buildPrintHtml(sheetsHtml, cfg, copies) {
    var styles = '';
    Array.prototype.forEach.call(document.querySelectorAll('style'), function (s) {
      if (s.id === 'pgs-dynpage') return;
      styles += s.outerHTML;
    });
    var body = '';
    for (var c = 0; c < copies; c++) body += sheetsHtml;
    return '<!DOCTYPE html><html lang="fa" dir="rtl"><head><meta charset="utf-8">' +
      '<title>چاپ آزمون</title>' + styles + PRINT_IFRAME_CSS +
      '<style>@page{size:' + cfg.W + 'mm ' + cfg.H + 'mm;margin:0;}</style>' +
      '</head><body>' + body + '</body></html>';
  }

  function waitForDocReady(doc) {
    return new Promise(function (res) {
      var done = false;
      var fin = function () { if (!done) { done = true; res(); } };
      var imgs = Array.prototype.slice.call(doc.images || []);
      var pending = imgs.length;
      function afterImgs() {
        try {
          if (doc.fonts && doc.fonts.ready) doc.fonts.ready.then(function () { setTimeout(fin, 80); });
          else setTimeout(fin, 200);
        } catch (e) { setTimeout(fin, 200); }
      }
      if (!pending) { afterImgs(); }
      else {
        imgs.forEach(function (im) {
          if (im.complete) { if (--pending <= 0) afterImgs(); }
          else { im.onload = im.onerror = function () { if (--pending <= 0) afterImgs(); }; }
        });
      }
      setTimeout(fin, 3000);
    });
  }

  function doPrint(opts) {
    opts = opts || {};
    ensureBaseDom();
    var restoreMode;
    try { restoreMode = printMode; } catch (e) { restoreMode = 'student'; }
    if (opts.mode && opts.mode !== restoreMode) {
      try { printMode = opts.mode; } catch (e) {}
      try { window.renderPreview(); } catch (e) {}
    }
    try { paginate(); } catch (e) {}
    var cfg = readCfg();
    var sheets = sheetList();
    var picked = sheets.filter(function (s, i) { return !opts.range || opts.range[i + 1]; });
    if (!picked.length) { alert('صفحه‌ای برای چاپ در این بازه وجود ندارد.'); return; }
    var html = picked.map(function (el) { return cleanSheetClone(el).outerHTML; }).join('');

    var fr = $('pgsPrintFrame');
    var doc = fr.contentDocument || fr.contentWindow.document;
    doc.open();
    doc.write(buildPrintHtml(html, cfg, opts.copies || 1));
    doc.close();

    var restore = function () {
      try {
        if (typeof printMode !== 'undefined' && printMode !== restoreMode) {
          printMode = restoreMode;
          window.renderPreview();
        }
      } catch (e) {}
    };
    try { fr.contentWindow.onafterprint = restore; } catch (e) {}

    waitForDocReady(doc).then(function () {
      setTimeout(function () {
        try {
          fr.contentWindow.focus();
          fr.contentWindow.print();
        } catch (e) {
          alert('خطا در شروع چاپ: ' + e.message);
        }
        setTimeout(restore, 1500);
      }, 150);
    });
  }

  function rebuildPrintRoot() {
    var root = $('pgsPrintRoot');
    if (!root) return;
    try { paginate(); } catch (e) {}
    root.innerHTML = sheetList().map(function (el) { return cleanSheetClone(el).outerHTML; }).join('');
  }

  /* ============================ جبران زوم هنگام کشیدن ============================
     وقتی بوم با transform scale زوم شده، deltas نشانگر باید بر z تقسیم شوند تا
     جابه‌جایی/تغییر اندازهٔ شکل‌ها و خطوط جداکننده دقیق بماند. در طول drag،
     getterهای clientX/clientY روی MouseEvent.prototype اصلاح می‌شوند. */
  var zoomPatch = null;
  function installZoomPatch(origin, z) {
    if (zoomPatch || z === 1) return;
    try {
      var proto = window.MouseEvent ? MouseEvent.prototype : null;
      if (!proto) return;
      var dX = Object.getOwnPropertyDescriptor(proto, 'clientX');
      var dY = Object.getOwnPropertyDescriptor(proto, 'clientY');
      if (!dX || !dX.get || !dY || !dY.get) return;
      var oX = dX.get, oY = dY.get;
      Object.defineProperty(proto, 'clientX', {
        configurable: true,
        get: function () { return origin.x + (oX.call(this) - origin.x) / z; }
      });
      Object.defineProperty(proto, 'clientY', {
        configurable: true,
        get: function () { return origin.y + (oY.call(this) - origin.y) / z; }
      });
      zoomPatch = { dX: dX, dY: dY };
    } catch (e) {}
  }
  function removeZoomPatch() {
    if (!zoomPatch) return;
    try {
      Object.defineProperty(MouseEvent.prototype, 'clientX', zoomPatch.dX);
      Object.defineProperty(MouseEvent.prototype, 'clientY', zoomPatch.dY);
    } catch (e) {}
    zoomPatch = null;
  }

  /* ============================ جایگزینی توابع قدیمی ============================ */
  function installOverrides() {
    if (typeof window.renderPreview === 'function' && !window.renderPreview.__pgs) {
      var base = window.renderPreview;
      var wrapped = function () {
        var r = base.apply(this, arguments);
        try { schedulePaginate(0); } catch (e) {}
        return r;
      };
      wrapped.__pgs = true;
      wrapped.__qimg = true; /* جلوی بسته‌بندی مجدد توسط qimg را می‌گیرد */
      window.renderPreview = wrapped;
    }

    window.printStudent = function () { doPrint({ mode: 'student' }); };
    window.printTeacher = function () { doPrint({ mode: 'teacher' }); };

    window.openPreviewWindow = function () {
      ensureBaseDom();
      if (viewerOpen()) return;
      buildViewer();
      document.body.classList.add('pgs-viewer-open');
      var saved = parseFloat(localStorage.getItem(LS_ZOOM) || '');
      pgsState.page = 1;
      try { window.renderPreview(); } catch (e) {}
      if (!isMobile() && isFinite(saved) && saved > 0) pgsSetZoom(saved);
      else setTimeout(function () { pgsFit('width'); }, 80);
      updateViewer();
      var tb = $('pgsThumbsBtn');
      if (tb) tb.classList.toggle('on', pgsState.thumbs);
    };
    window.closePreviewWindow = function () { destroyViewer(); };
    window.togglePreviewWindow = function () {
      if (viewerOpen()) window.closePreviewWindow();
      else window.openPreviewWindow();
    };
    window.isPreviewOpen = function () { return viewerOpen(); };
  }

  function bindKeys() {
    document.addEventListener('keydown', function (e) {
      var ctrl = e.ctrlKey || e.metaKey;
      if (ctrl && (e.key === 'p' || e.key === 'P' || e.key === 'ح')) {
        e.preventDefault();
        var m = 'student';
        try { if (typeof printMode !== 'undefined' && printMode) m = printMode; } catch (err) {}
        doPrint({ mode: m });
        return;
      }
      if (!viewerOpen()) return;
      if (e.key === 'Escape') {
        var d = $('pgsPrintDlg');
        if (d && d.classList.contains('open')) { pgsClosePrintDlg(); return; }
        window.closePreviewWindow();
        return;
      }
      if (ctrl && (e.key === '=' || e.key === '+')) { e.preventDefault(); pgsZoomBy(0.1); }
      else if (ctrl && e.key === '-') { e.preventDefault(); pgsZoomBy(-0.1); }
      else if (ctrl && e.key === '0') { e.preventDefault(); pgsFit('width'); }
      else if (e.key === 'PageDown') { e.preventDefault(); pgsGoPage(1); }
      else if (e.key === 'PageUp') { e.preventDefault(); pgsGoPage(-1); }
      else if (e.key === 'Home') { e.preventDefault(); goToSheet(1); }
      else if (e.key === 'End') { e.preventDefault(); goToSheet(pgsState.sheets || 1); }
    }, true);

    /* تغییرات UIِ خودِ بیننده/دیالوگ نباید renderPreview را trigger کنند */
    document.addEventListener('change', function (e) {
      var t = e.target;
      if (t && t.closest && t.closest('#pgsViewer .pgs-ribbon, #pgsViewer .pgs-status, #pgsPrintDlg')) {
        e.stopImmediatePropagation();
      }
    }, true);

    window.addEventListener('beforeprint', function () {
      try {
        document.body.classList.add('pgs-fallback');
        rebuildPrintRoot();
      } catch (e) {}
    });
    window.addEventListener('afterprint', function () {
      try { document.body.classList.remove('pgs-fallback'); } catch (e) {}
    });
  }

  function bindRepaginateTriggers() {
    var area = $('previewArea');
    if (!area) return;
    var dragArmed = false;
    area.addEventListener('pointerdown', function (e) {
      var t = e.target;
      var onDrag = !!(t && t.closest && t.closest('.question-sep-drag,.interactive-figure,.qimg-fig'));
      dragArmed = onDrag;
      if (onDrag && pgsState.zoom !== 1) {
        installZoomPatch({ x: e.clientX, y: e.clientY }, pgsState.zoom);
      }
    }, true);
    area.addEventListener('pointerup', function () {
      removeZoomPatch();
      if (dragArmed) { dragArmed = false; schedulePaginate(320); }
    }, true);
    area.addEventListener('pointercancel', function () {
      removeZoomPatch();
      dragArmed = false;
    }, true);
    window.addEventListener('pointerup', function () { setTimeout(removeZoomPatch, 400); }, true);
  }

  /* ============================ راه‌اندازی ============================ */
  function init() {
    ensureBaseDom();
    try {
      var saved = JSON.parse(localStorage.getItem(LS_SETUP) || 'null');
      if (saved) applySetup(saved);
    } catch (e) {}
    installOverrides();
    bindKeys();
    bindRepaginateTriggers();
    schedulePaginate(400);
    setTimeout(function () { schedulePaginate(0); }, 1500);
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
  else init();
})();
