/* FIX-EXAM-UI2: پنجره نوع سوال، پیش‌نمایش، نوار شماره و آکاردئون سوالات */
(function () {
  if (window.__qUI2Installed) return;
  window.__qUI2Installed = true;
  function $(id) { return document.getElementById(id); }
  function faNum(n) { return String(n).replace(/\d/g, function (d) { return '٠١٢٣٤٥٦٧٨٩'[+d]; }); }
  var state = { openId: null };
  window.__qUI = state;

  /* ---------- 1) پنجره انتخاب نوع سوال ---------- */
  var TYPES = [
    ['multiple', '🔘 چندگزینه‌ای'],
    ['truefalse', '✓ صحیح/غلط'],
    ['long', '📝 تشریحی'],
    ['fill', '___ جای‌خالی'],
    ['numeric', '🔢 عددی'],
    ['matching', '↔ جورکردنی']
  ];
  function ensureOverlay() {
    var ov = $('qtypeOverlay');
    if (ov) return ov;
    ov = document.createElement('div');
    ov.id = 'qtypeOverlay';
    ov.className = 'qtype-overlay';
    var html = '<div class="qtype-box"><div class="qtype-head"><span>انتخاب نوع سوال</span><button type="button" class="qtype-x" onclick="closeQuestionTypePicker()">✕</button></div><div class="qtype-grid">';
    TYPES.forEach(function (t) { html += '<button type="button" class="qtype-item" onclick="pickQuestionType(\'' + t[0] + '\')">' + t[1] + '</button>'; });
    html += '</div></div>';
    ov.innerHTML = html;
    ov.addEventListener('click', function (e) { if (e.target === ov) closeQuestionTypePicker(); });
    document.body.appendChild(ov);
    return ov;
  }
  window.openQuestionTypePicker = function () {
    var ov = ensureOverlay();
    ov.style.display = 'flex';
    try { closePreviewWindow(); } catch (e) {}
  };
  window.closeQuestionTypePicker = function () {
    var ov = $('qtypeOverlay');
    if (ov) ov.style.display = 'none';
  };
  window.pickQuestionType = function (type) {
    closeQuestionTypePicker();
    try { if (typeof addQuestion === 'function') addQuestion(type); } catch (e) {}
  };

  /* ---------- بستن پنجره تنظیمات سربرگ ---------- */
  function closeSettingsPanel() {
    var p = $('settingsPanel');
    if (p && p.style.display !== 'none') p.style.display = 'none';
  }

  /* ---------- 2) پنجره پیش‌نمایش ---------- */
  var prevInfo = null;
  window.isPreviewOpen = function () { return !!$('previewWinOverlay'); };
  window.togglePreviewWindow = function () {
    if ($('previewWinOverlay')) closePreviewWindow();
    else openPreviewWindow();
  };
  function openPreviewWindow() {
    var pc = $('printContent');
    if (!pc || $('previewWinOverlay')) return;
    prevInfo = { parent: pc.parentNode, next: pc.nextSibling };
    var ov = document.createElement('div');
    ov.id = 'previewWinOverlay';
    ov.innerHTML = '<div class="pwo-box"><div class="pwo-head"><span>👁 پیش‌نمایش آزمون</span><button type="button" class="pwo-x" onclick="closePreviewWindow()">✕ بستن</button></div><div class="pwo-body"></div></div>';
    ov.addEventListener('click', function (e) { if (e.target === ov) closePreviewWindow(); });
    document.body.appendChild(ov);
    ov.querySelector('.pwo-body').appendChild(pc);
    pc.classList.add('in-window');
    try { setTimeout(function(){ if (window.mbFitAllSurds) window.mbFitAllSurds(document); }, 80); } catch (e) {}
    try { pc.scrollIntoView({ block: 'start' }); } catch (e) {}
  }
  window.openPreviewWindow = openPreviewWindow;
  window.closePreviewWindow = function () {
    var ov = $('previewWinOverlay');
    if (!ov) return;
    var pc = $('printContent');
    if (pc && prevInfo && prevInfo.parent) {
      pc.classList.remove('in-window');
      prevInfo.parent.insertBefore(pc, prevInfo.next);
    }
    prevInfo = null;
    if (ov.parentNode) ov.parentNode.removeChild(ov);
  };

  /* ---------- 3و4) آکاردئون کارت‌ها و نوار شماره ---------- */
  function cardHas(card, id) { return !!(card.querySelector('#q_text_' + id) || card.querySelector('#qbody_' + id)); }
  function collapseCard(card, collapsed) {
    /* FIX-EXAM-SINGLE-WINDOW: سوال‌ها کارت نیستند — پنجرهٔ باز، جایگزینِ قبلی است */
    card.classList.toggle('collapsed', collapsed);
    card.style.display = collapsed ? 'none' : '';
  }
  function allCards() { return Array.prototype.slice.call(document.querySelectorAll('#questionsContainer .question-card')); }
  function applyAccordion() {
    var cards = allCards();
    if (!cards.length) return;
    var openId = state.openId;
    var found = false;
    if (openId != null) cards.forEach(function (c) { if (cardHas(c, openId)) found = true; });
    /* FIX-EXAM-SINGLE-WINDOW: اگر سوالی باز نیست، اولین سوال باز شود */
    if (openId == null || !found) {
      var fc = cards[0];
      var fe = fc.querySelector('[id^="q_text_"]') || fc.querySelector('[id^="qbody_"]');
      openId = fe ? fe.id.replace(/^q_text_|^qbody_/, '') : null;
      state.openId = openId;
    }
    if (openId == null) return;
    cards.forEach(function (c) { collapseCard(c, !cardHas(c, openId)); });
  }
  function syncStrip(toEnd) {
    var wrap = $('questionNumberStripWrap');
    var strip = $('questionNumberStrip');
    if (!wrap || !strip) return;
    var qs = (typeof questions !== 'undefined') ? questions : [];
    if (!qs.length) { wrap.style.display = 'none'; strip.innerHTML = ''; return; }
    wrap.style.display = 'block';
    var openId = state.openId;
    var html = '';
    qs.forEach(function (q, i) {
      var act = (openId != null && String(openId) === String(q.id));
      html += '<button type="button" class="qnum-chip' + (act ? ' active' : '') + '" data-qid="' + q.id + '" onclick="window.__qUI && window.__qUI.open && window.__qUI.open(' + q.id + ')" title="باز کردن سوال">' + faNum(i + 1) + '</button>';
    });
    strip.innerHTML = html;
    if (toEnd) {
      /* FIX: در نوار RTL مقدار scrollLeft قابل اتکا نیست؛ چیپ آخر را داخل دید اسکرول کن */
      try {
        var lastEl = strip.lastElementChild;
        if (lastEl && typeof lastEl.scrollIntoView === 'function') lastEl.scrollIntoView({ block: 'nearest', inline: 'end' });
        else strip.scrollLeft = strip.scrollWidth;
      } catch (e) { try { strip.scrollLeft = strip.scrollWidth; } catch (e2) {} }
    }
  }
  function openQuestionId(id, scroll) {
    state.openId = id;
    var cards = allCards();
    var target = null;
    cards.forEach(function (c) {
      var has = cardHas(c, id);
      if (has) target = c;
      collapseCard(c, !has);
    });
    syncStrip(false);
    if (target && scroll) {
      try { target.scrollIntoView({ behavior: 'smooth', block: 'start' }); } catch (e) { try { target.scrollIntoView(); } catch (e2) {} }
      var qt = target.querySelector('textarea[id^="q_text_"]');
      if (qt) { try { if (typeof qMathSync === 'function') qMathSync(qt.id); } catch (e) {} }
    }
  }
  state.open = function (id) { openQuestionId(id, true); };

  /* ---------- هوک‌ها ---------- */
  function hook(name, fn) {
    try {
      var orig = window[name];
      if (typeof orig !== 'function') return;
      window[name] = function () { var r = orig.apply(this, arguments); try { fn(); } catch (e) {} return r; };
    } catch (e) {}
  }
  hook('addQuestion', function () {
    closeSettingsPanel();
    closePreviewWindow();
    var qs = (typeof questions !== 'undefined') ? questions : [];
    var id = qs.length ? qs[qs.length - 1].id : null;
    if (id != null) { openQuestionId(id, true); syncStrip(true); }
  });
  hook('removeQuestion', function () {
    var qs = (typeof questions !== 'undefined') ? questions : [];
    var still = false;
    if (state.openId != null) allCards().forEach(function (c) { if (cardHas(c, state.openId)) still = true; });
    if (!still) {
      if (qs.length) { state.openId = qs[0].id; openQuestionId(qs[0].id, false); }
      else { state.openId = null; syncStrip(false); }
    } else { syncStrip(false); applyAccordion(); }
  });
  hook('moveQuestion', function () { syncStrip(false); applyAccordion(); });
  hook('renderAll', function () { syncStrip(false); applyAccordion(); });
  /* بستن پیش‌نمایش پیش از اجرای چاپ (اگر پنجرهٔ چشم باز باشد) تا چاپ خالی نشود */
  function hookBefore(name, fn) {
    try {
      var orig = window[name];
      if (typeof orig !== 'function') return;
      window[name] = function () { try { fn(); } catch (e) {} return orig.apply(this, arguments); };
    } catch (e) {}
  }
  hookBefore('printStudent', closePreviewWindow);
  hookBefore('printTeacher', closePreviewWindow);
  /* پشتیبان: بعد از هر تغییر DOM رادیکال‌ها دوباره اندازه بگیرند (خطِ رویین تک‌مسیر است) */
  if (window.MutationObserver) {
    var __fitT = null;
    var __fitMo = new MutationObserver(function (ms) {
      /* V179 — کارایی: بندانگشتی‌ها clone برگه‌های آماده‌اند؛ نیازی به اندازه‌گیری دوباره ندارند */
      var relevant = false;
      for (var i = 0; i < ms.length; i++) { var t = ms[i].target; if (!(t && t.closest && t.closest('.pgs-thumbs'))) { relevant = true; break; } }
      if (!relevant) return;
      clearTimeout(__fitT);
      __fitT = setTimeout(function () {
        try { if (window.mbFitAllSurds) mbFitAllSurds(document); } catch (e) {}
      }, 90);
    });
    try { __fitMo.observe(document.body, { childList: true, subtree: true }); } catch (e) {}
  }
  /* FIX-EXAM-SINGLE-WINDOW: پس از بارگذاری اولین سوال باز باشد (بقیه پنهان) */
  try {
    var __sw = (typeof questions !== 'undefined') ? questions : [];
    if (__sw.length && state.openId == null) state.openId = __sw[0].id;
  } catch (e) {}
  try { applyAccordion(); syncStrip(false); } catch (e) {}
})();
