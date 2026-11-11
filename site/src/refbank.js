/* V360.5 — بانک سؤال مرجع، فقط‌خواندنی از روی فضای ابری.
 *
 * قید کاربر: هیچ سؤال یا تصویری وارد باندل سایت یا APK نشود.
 *
 * تصمیم تازهٔ کاربر: پنجرهٔ فیلتر **همه کشویی** است و همه پیش‌فرض «همه».
 * نیازی به پیمودن گامها نیست؛ کاربر میتواند فقط پایه را بگزیند و فیلتر
 * را ببندد تا سؤال ها را ببیند. تا پیش از انتخاب پایه، درس فقط «همه»
 * دارد؛ فصل تا انتخاب درس فقط «همه»؛ ولی نوع سؤال و دشواری همیشه
 * همهٔ گزینه ها را دارند. رشته فقط برای پایهٔ ۱۰ تا ۱۲ دیده می شود.
 * پایهٔ «همه» یعنی فقط شاردهایی که واقعاً بارگذاری شده اند.
 * پاسخ و تصویر پاسخ با تیک کاربر و فقط در «بخش پاسخ» کپی میشوند.
 */
(function (root) {
  'use strict';
  var S = root.ExamSite;
  if (!S) return;
  var el = S.el, fa = S.fa, esc = S.esc, toast = S.toast;
  var BASE = String(root.__REF_BANK_BASE__ || 'https://azmoon-media.s3.ir-thr-at1.arvanstorage.ir/reference-bank').replace(/\/+$/, '');
  var BP = root.BankPicker;
  var DIFF = (BP && BP.DIFFICULTY_LABELS) || ['تعییننشده', 'ساده', 'متوسط', 'دشوار', 'خیلی دشوار'];
  var TRACKS = ['', 'عمومی', 'ریاضی فیزیک', 'علوم تجربی', 'انسانی', 'هنرستان'];
  var TYPES = ['تستی', 'تشریحی', 'جای خالی', 'صحیح/غلط', 'دسته‌بندی', 'مرتب‌سازی', 'کوتاه‌پاسخ'];
  var PRIMARY = ['فارسی', 'ریاضی', 'علوم تجربی', 'قرآن و تعلیمات اسلامی', 'مطالعات اجتماعی', 'هنر', 'تربیت بدنی'];
  var MIDDLE = ['فارسی', 'ریاضی', 'علوم تجربی', 'عربی', 'انگلیسی', 'مطالعات اجتماعی', 'قرآن و تعلیمات اسلامی', 'کامپیوتر', 'هنر', 'تربیت بدنی'];
  var HIGH = {
    'عمومی': ['فارسی', 'ریاضی', 'فیزیک', 'شیمی', 'زیستشناسی', 'عربی', 'انگلیسی', 'دین و زندگی', 'تاریخ', 'جغرافیا', 'علوم اجتماعی', 'فلسفه و منطق'],
    'ریاضی فیزیک': ['ریاضی', 'فیزیک', 'شیمی', 'فارسی', 'عربی', 'انگلیسی', 'دین و زندگی'],
    'علوم تجربی': ['زیستشناسی', 'فیزیک', 'شیمی', 'ریاضی', 'فارسی', 'عربی', 'انگلیسی', 'دین و زندگی'],
    'انسانی': ['فارسی', 'عربی', 'تاریخ', 'جغرافیا', 'علوم اجتماعی', 'فلسفه و منطق', 'اقتصاد', 'ریاضی', 'انگلیسی', 'دین و زندگی'],
    'هنرستان': ['فنی و حرفهای', 'کاردانش', 'ریاضی', 'فارسی', 'دین و زندگی']
  };
  var ALL_HIGH = []; Object.keys(HIGH).forEach(function (k) { HIGH[k].forEach(function (x) { if (ALL_HIGH.indexOf(x) < 0) ALL_HIGH.push(x); }); }); ALL_HIGH.sort();
  var SHARD_VERSION = 'v3630';
  var cache = {catalog: {}, questions: {}, catalogAll: null};
  var FORMULA_PAIRS = [['\\(', '\\)'], ['\\[', '\\]'], ['$$', '$$']];

  /* V361 — فرمول MathJax \(...\)، \[...\] و $$...$$ → توکن $...$ ویرایشگر فرمول. */
  function mathJaxToTokens(raw) {
    if (raw === null || raw === undefined) return '';
    raw = String(raw);
    if (!raw) return raw;
    var out = [], i = 0;
    while (i < raw.length) {
      var hit = null;
      for (var j = 0; j < FORMULA_PAIRS.length; j++) {
        if (raw.substr(i, FORMULA_PAIRS[j][0].length) === FORMULA_PAIRS[j][0]) { hit = FORMULA_PAIRS[j]; break; }
      }
      if (hit) {
        var end = raw.indexOf(hit[1], i + hit[0].length);
        if (end >= 0) {
          var body = raw.slice(i + hit[0].length, end).replace(/\s+/g, ' ').trim();
          if (body) out.push('$' + body + '$');
          i = end + hit[1].length;
          continue;
        }
      }
      out.push(raw.charAt(i));
      i++;
    }
    var value = out.join('');
    if (value.indexOf('$$') < 0) return value;
    var out2 = [], inside = false;
    for (var k = 0; k < value.length; k++) {
      var ch = value.charAt(k);
      out2.push(ch);
      if (ch === '$') {
        if (inside) { inside = false; if (value.charAt(k + 1) === '$') out2.push(' '); }
        else inside = true;
      }
    }
    return out2.join('');
  }
  function tokenizeQuestion(q) {
    if (!q || typeof q !== 'object') return q;
    var c = {};
    Object.keys(q).forEach(function (k) { c[k] = q[k]; });
    ['text', 'q_html', 'answer_html', 'answer_text'].forEach(function (k) {
      if (typeof c[k] === 'string') c[k] = mathJaxToTokens(c[k]);
    });
    if (Array.isArray(c.options)) c.options = c.options.map(function (o) {
      return typeof o === 'string' ? mathJaxToTokens(o) : o;
    });
    return c;
  }
  function tokenizeRows(rows) {
    return Array.isArray(rows) ? rows.map(function (r) {
      if (!r || typeof r !== 'object') return r;
      var c = {};
      Object.keys(r).forEach(function (k) { c[k] = r[k]; });
      if (typeof c.text === 'string') c.text = mathJaxToTokens(c.text);
      return c;
    }) : rows;
  }
  /* V361 — رندر توکن به نماد با همان موتور ویرایشگر فرمول سایت. */
  function richInto(node, raw) {
    var tokens = mathJaxToTokens(String(raw || ''));
    if (window.SiteStudent && window.SiteStudent.richHtml) {
      window.SiteStudent.richHtml(tokens).then(function (h) {
        node.innerHTML = h;
        if (window.SiteStudent.fitMath) window.SiteStudent.fitMath(node);
      }).catch(function () { node.textContent = tokens; });
    } else {
      node.textContent = tokens;
    }
    return node;
  }

  /* V361 — هر span فرمول MathJax در HTML شارد به نماد SVG ویرایشگر تبدیل شود. */
  function renderFormulaSpans(root) {
    if (!root || !window.SiteStudent || !window.SiteStudent.richHtml) return;
    var spans = root.querySelectorAll('.rz-formula, .cz-formula, .az-formula');
    Array.prototype.forEach.call(spans, function (sp) {
      var tokens = mathJaxToTokens(sp.textContent || '');
      window.SiteStudent.richHtml(tokens).then(function (h) {
        sp.innerHTML = h;
        if (window.SiteStudent.fitMath) window.SiteStudent.fitMath(sp);
      }).catch(function () { sp.textContent = tokens; });
    });
  }

  function url(path) { return BASE + '/' + path + '?v=' + SHARD_VERSION; }
  function getJSON(path) {
    /* V360.9 — بدون کش: شارد تازه بعد از آپلود حتماً خوانده شود. */
    return fetch(url(path), {cache: 'no-store'}).then(function (r) {
      if (!r.ok) throw new Error('HTTP ' + r.status);
      return r.json();
    });
  }
  function catalog(grade) {
    var k = String(grade);
    if (!cache.catalog[k]) cache.catalog[k] = getJSON('catalog/g' + k + '.json').then(tokenizeRows);
    return cache.catalog[k];
  }
  function questions(grade) {
    var k = String(grade);
    if (!cache.questions[k]) cache.questions[k] = getJSON('questions/g' + k + '.json').then(function (all) {
      var out = {};
      Object.keys(all || {}).forEach(function (id) { out[id] = tokenizeQuestion(all[id]); });
      return out;
    });
    return cache.questions[k];
  }
  function catalogAll() {
    if (!cache.catalogAll) {
      var ps = [];
      for (var g = 1; g <= 12; g++) ps.push(catalog(g).catch(function () { return []; }));
      cache.catalogAll = Promise.all(ps).then(function (lists) {
        var rows = [], loaded = [];
        lists.forEach(function (list, i) {
          if (Array.isArray(list) && list.length) { rows = rows.concat(list); loaded.push(i + 1); }
        });
        return {rows: rows, loaded: loaded};
      });
    }
    return cache.catalogAll;
  }
  function uniq(rows, key) {
    var seen = {}, out = [];
    rows.forEach(function (r) { var v = String(r[key] || '').trim(); if (v && !seen[v]) { seen[v] = 1; out.push(v); } });
    return out.sort(function (a, b) { return a.localeCompare(b, 'fa'); });
  }
  function gradeLabel(g) { return Number(g) === 0 ? 'همهٔ پایه ها' : 'پایهٔ ' + fa(g); }
  function tracksFor(g) { return Number(g) >= 10 ? TRACKS : []; }
  function subjectsFor(g, track) {
    g = Number(g);
    if (g >= 1 && g <= 6) return PRIMARY;
    if (g >= 7 && g <= 9) return MIDDLE;
    if (g >= 10 && g <= 12) return track ? (HIGH[track] || ALL_HIGH) : ALL_HIGH;
    return [];
  }

  /* تصویر پاسخ از answer_html شناخته می شود؛ نیازی به شارد تازه ای نیست. */
  function imgUrls(html) {
    var out = [], m, re = /<img[^>]+src=["']([^"']+)["']/gi;
    while ((m = re.exec(html || '')) !== null) if (out.indexOf(m[1]) < 0) out.push(m[1]);
    return out;
  }
  function splitImages(p) {
    var all = p.image_urls || [];
    /* V360.8 — صورت سؤال فقط تصویر های q_html را میگیرد؛ تصویر گزینه ها
       جداگانه از option_images خوانده می شود و هرگز وارد متن سؤال نمی شود. */
    var q = imgUrls(p.q_html || '').filter(function (u) { return all.indexOf(u) >= 0; });
    var ans = imgUrls(p.answer_html || '').filter(function (u) { return all.indexOf(u) >= 0; });
    var opts = (p.option_images || []).map(function (u) { return u || null; });
    return {question: q, answer: ans, options: opts};
  }
  /* V363 — برش امن: نه وسط «$»، نه نصفه‌کلمه، نه فرمولِ باز. */
  function safeSnippet(value, limit) {
    var v = String(value || '').trim();
    var odd = function (s) { return (s.split('$').length - 1) % 2 === 1; };
    var balance = function (s) {
      s = s.replace(/\s+$/, '');
      if (!odd(s)) return s;
      var i = s.lastIndexOf('$');
      return i > 0 ? s.slice(0, i).replace(/\s+$/, '') + '…' : s;
    };
    if (limit <= 0 || v.length <= limit) return balance(v);
    var cut = v.slice(0, limit);
    if (odd(cut)) cut = cut.slice(0, cut.lastIndexOf('$'));
    if (cut.indexOf(' ') > limit / 2) cut = cut.slice(0, cut.lastIndexOf(' '));
    return balance(cut.replace(/\s+$/, '') + '…');
  }
  /* V363 — متن سؤال از HTML؛ برخلاف plain نیم‌فاصله را نمی‌شکند. */
  function plainQuestion(html) {
    return String(html || '').replace(/<[^>]*>/g, ' ').replace(/&zwnj;/g, '\u200c')
      .replace(/&nbsp;/g, ' ').replace(/&amp;/g, '&').replace(/&lt;/g, '<')
      .replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&#39;/g, "'")
      .replace(/\u00a0/g, ' ').replace(/\s+/g, ' ').trim();
  }
  function plain(html) {
    return String(html || '').replace(/<[^>]*>/g, ' ').replace(/&zwnj;/g, ' ').replace(/&nbsp;/g, ' ')
      .replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&#39;/g, "'")
      .replace(/[\u200c\u00a0]/g, ' ').replace(/\s+/g, ' ').trim();
  }
  function draft(p, includeAnswer) {
    var inc = includeAnswer !== false, sp = splitImages(p);
    /* V361 — حتی اگر شارد خام مستقیم رسید، فرمولها به توکن ویرایشگر بروند. */
    /* V363 — اول text (متن آمادهٔ سازنده)، بعد q_html؛ قبلاً برعکس بود و نیم‌فاصله می‌شکست. */
    var textTokens = mathJaxToTokens(p.text || plainQuestion(p.q_html || ''));
    var answerTokens = mathJaxToTokens(plain(p.answer_html || '') || p.answer_text || '');
    var optionTokens = (p.options || []).map(function (o) { return mathJaxToTokens(String(o || '')); });
    var q = {
      type: p.type || 'essay', text: textTokens, grade: p.grade || 0,
      subject: p.subject || '', topic: p.topic || '', difficulty: p.difficulty || 0,
      score: 1, options: optionTokens, correctIndex: p.correct_index == null ? null : p.correct_index,
      expectedText: '', images: sp.question.slice(0, 8), optionImages: [],
      answerExplanation: inc ? answerTokens : '',
      answerImages: inc ? sp.answer.slice(0, 8) : []
    };
    /* V363 — گزینه‌های کاملاً خالی و بی‌تصویر «تشریحی» می‌شوند، نه تستیِ بی‌متن. */
    var optsBlank = (p.options || []).length > 0 &&
      (p.options || []).every(function (o) { return !String(o || '').trim(); });
    var hasOptImg = (sp.options || []).some(function (u) { return !!u; });
    if (q.type === 'multiple' && ((!q.options || q.options.length < 2) || (optsBlank && !hasOptImg))) q.type = 'essay';
    /* V360.8 — تصویر هر گزینه دقیقاً همتراز با همان گزینه. */
    q.optionImages = q.type === 'multiple'
      ? q.options.map(function (_, i) { return sp.options[i] || null; })
      : [];
    if (q.type === 'fill') q.expectedText = answerTokens;
    if (q.type === 'truefalse') q.expectedText = /غلط|نادرست/.test(p.answer_text || '') ? 'false' : 'true';
    return q;
  }

  function open(opts) {
    opts = opts || {};
    var append = !!opts.append;
    cache.catalog = {}; cache.questions = {}; cache.catalogAll = null;
    var bg = el('div', {class: 'modal-bg'});
    var card = el('div', {class: 'modal wide refbank-modal', style: 'max-width:1180px;width:98vw;height:94vh;max-height:94vh;overflow:hidden;display:flex;flex-direction:column;position:relative'});
    bg.appendChild(card); document.body.appendChild(bg);
    bg.addEventListener('click', function (e) { if (e.target === bg) bg.remove(); });
    var st = {
      grade: 0, track: '', subject: '', chapter: '', type: '', difficulty: '', query: '',
      page: 0, size: 25, rows: [], loaded: [], picked: {}, includeAnswer: true,
      includeAnswerById: {}, details: {}, detailLoaded: {}, detailPending: {},
      filterOpen: true, searchOpen: false, missing: false, loading: false, note: null, error: null
    };

    function answerOn(row) {
      return Object.prototype.hasOwnProperty.call(st.includeAnswerById, row.id)
        ? !!st.includeAnswerById[row.id] : st.includeAnswer;
    }
    function setAnswer(row) { st.includeAnswerById[row.id] = !answerOn(row); draw(); }
    function closeBank() { bg.remove(); }

    function head() {
      var h = el('div', {class: 'refbank-header'});
      var first = el('div', {class: 'refbank-title-row'});
      first.appendChild(el('h2', {class: 'grow', text: 'بانک سؤال مرجع'}));
      if (!st.filterOpen) {
        first.appendChild(el('button', {class: 'ref-icon-btn', type: 'button', title: 'جست‌وجو', 'aria-label': 'جست‌وجو', text: '⌕', onclick: function () { st.searchOpen = !st.searchOpen; draw(); if (st.searchOpen) { var x = card.querySelector('.refbank-search-input'); if (x) x.focus(); } }}));
        first.appendChild(el('button', {class: 'ref-icon-btn', type: 'button', title: 'فیلتر', 'aria-label': 'فیلتر', text: '☷', onclick: function () { st.filterOpen = true; draw(); }}));
      }
      first.appendChild(el('button', {class: 'x refbank-close', type: 'button', title: 'بستن بانک سؤال مرجع', 'aria-label': 'بستن بانک سؤال مرجع', text: '✕', onclick: closeBank}));
      h.appendChild(first);
      if (!st.filterOpen) {
        var toolbar = el('div', {class: 'refbank-toolbar', role: 'toolbar', 'aria-label': 'ابزارهای بانک سؤال'});
        var f = filtered(), pages = Math.max(1, Math.ceil(f.length / st.size));
        if (st.page >= pages) st.page = pages - 1;
        var slice = f.slice(st.page * st.size, (st.page + 1) * st.size);
        toolbar.appendChild(el('span', {class: 'chip brand refbank-count', text: fa(f.length) + ' سؤال · صفحهٔ ' + fa(st.page + 1) + ' از ' + fa(pages) + ' · ' + fa(pickedRows().length) + ' انتخاب شده'}));
        toolbar.appendChild(el('button', {class: 'btn light sm', type: 'button', title: 'انتخاب/برداشتن صفحه', 'aria-label': 'انتخاب/برداشتن صفحه', text: '☑', onclick: function () {
          var ids = slice.map(function (r) { return r.id; }), all = ids.length > 0 && ids.every(function (id) { return st.picked[id]; });
          ids.forEach(function (id) { if (all) delete st.picked[id]; else st.picked[id] = 1; }); draw();
        }}));
        toolbar.appendChild(el('button', {class: 'btn light sm', type: 'button', title: 'قبلی', 'aria-label': 'قبلی', text: '‹', disabled: st.page === 0 ? 'disabled' : null, onclick: function () { st.page = Math.max(0, st.page - 1); draw(); }}));
        toolbar.appendChild(el('button', {class: 'btn light sm', type: 'button', title: 'بعدی', 'aria-label': 'بعدی', text: '›', disabled: st.page >= pages - 1 ? 'disabled' : null, onclick: function () { st.page = Math.min(pages - 1, st.page + 1); draw(); }}));
        var ans = el('button', {class: 'chip ref-answer-chip' + (st.includeAnswer ? ' on' : ''), type: 'button', title: 'افزودن پاسخ', 'aria-label': 'افزودن پاسخ', text: (st.includeAnswer ? '✓ ' : '+ ') + 'افزودن پاسخ', onclick: function () { st.includeAnswer = !st.includeAnswer; draw(); }});
        toolbar.appendChild(ans);
        toolbar.appendChild(el('button', {class: 'btn ref-bulk-add sm', type: 'button', text: '＋ افزودن گروهی (' + fa(pickedRows().length) + ') به آزمون', onclick: function () { addRows(pickedRows()); }}));
        h.appendChild(toolbar);
        if (st.searchOpen) {
          var sr = el('div', {class: 'refbank-search-row'});
          var qi = el('input', {class: 'refbank-search-input', type: 'search', placeholder: 'جست‌وجو در متن سؤال…', value: st.query, 'aria-label': 'جست‌وجو در متن سؤال'});
          qi.addEventListener('input', function () {
            var pos = qi.selectionStart; st.query = qi.value; st.page = 0; draw();
            var fresh = card.querySelector('.refbank-search-input');
            if (fresh) { fresh.focus(); try { fresh.setSelectionRange(pos, pos); } catch (_) {} }
          });
          sr.appendChild(qi);
          sr.appendChild(el('button', {class: 'ref-icon-btn', type: 'button', title: 'پاک‌کردن جست‌وجو', 'aria-label': 'پاک‌کردن جست‌وجو', text: '✕', onclick: function () { st.query = ''; draw(); var x = card.querySelector('.refbank-search-input'); if (x) x.focus(); }}));
          h.appendChild(sr);
        }
      }
      card.appendChild(h);
    }

    function selectBox(title, values, current, label, onPick) {
      var s = el('select', {title: title, style: 'border:1px solid var(--line);border-radius:10px;padding:8px;min-width:180px'});
      values.forEach(function (v) { s.appendChild(el('option', {value: String(v), text: label(v)})); });
      s.value = String(current);
      s.addEventListener('change', function () { onPick(s.value); });
      return el('label', {class: 'field refbank-filter-field'}, [el('span', {text: title}), s]);
    }
    function subjectOptions() {
      if (!st.grade) return [];
      var base = st.rows.filter(function (r) { return !st.track || String(r.track || '') === st.track; });
      var out = uniq(base, 'subject');
      return out.length ? out : subjectsFor(st.grade, st.track);
    }
    function chapterOptions() {
      if (!st.subject) return [];
      return uniq(st.rows.filter(function (r) {
        return (!st.track || String(r.track || '') === st.track) && r.subject === st.subject;
      }), 'chapter');
    }
    function typeOptions() {
      var out = TYPES.slice();
      uniq(st.rows, 'type_fa').forEach(function (t) { if (out.indexOf(t) < 0) out.push(t); });
      return out.sort(function (a, b) { return a.localeCompare(b, 'fa'); });
    }

    function filterPanel() {
      var scrim = el('div', {class: 'ref-filter-scrim'});
      scrim.addEventListener('click', function (e) { if (e.target === scrim) { /* فیلتر تا تأیید باز می‌ماند */ } });
      var popup = el('div', {class: 'ref-filter-popup', role: 'dialog', 'aria-label': 'فیلتر بانک سؤال مرجع'});
      var ph = el('div', {class: 'ref-filter-head'});
      ph.appendChild(el('strong', {class: 'grow', text: 'فیلتر بانک سؤال مرجع'}));
      var ans = el('button', {class: 'chip ref-answer-chip' + (st.includeAnswer ? ' on' : ''), type: 'button', text: (st.includeAnswer ? '✓ ' : '+ ') + 'افزودن پاسخ', title: 'افزودن پاسخ', onclick: function () { st.includeAnswer = !st.includeAnswer; draw(); }});
      ph.appendChild(ans);
      ph.appendChild(el('button', {class: 'btn ref-confirm sm', type: 'button', text: 'تأیید', onclick: function () { st.filterOpen = false; st.page = 0; draw(); loadDetails(); }}));
      ph.appendChild(el('button', {class: 'x refbank-close', type: 'button', title: 'بستن بانک سؤال مرجع', 'aria-label': 'بستن بانک سؤال مرجع', text: '✕', onclick: closeBank}));
      popup.appendChild(ph);
      var body = el('div', {class: 'ref-filter-body'});
      if (st.loading) body.appendChild(el('div', {class: 'ref-loading', role: 'status', 'aria-label': 'در حال خواندن فهرست پایه‌ها'}, [el('span', {class: 'ref-spinner'})]));
      if (st.error) body.appendChild(el('div', {class: 'alert warn', text: st.error}));
      if (st.missing) body.appendChild(el('div', {class: 'alert warn', text: 'فهرست این پایه هنوز بارگذاری نشده است.'}));
      var gs = [0]; for (var g = 1; g <= 12; g++) gs.push(g);
      body.appendChild(selectBox('پایه', gs, st.grade, gradeLabel, function (v) {
        st.grade = Number(v) || 0; st.track = ''; st.subject = ''; st.chapter = '';
        st.page = 0; st.picked = {}; st.includeAnswerById = {}; st.error = null; load();
      }));
      if (Number(st.grade) >= 10) body.appendChild(selectBox('رشته', TRACKS, st.track, function (t) { return t || 'همهٔ رشته ها'; }, function (v) {
        st.track = v; st.subject = ''; st.chapter = ''; st.page = 0; draw();
      }));
      body.appendChild(selectBox('درس', [''].concat(subjectOptions()), st.subject, function (x) { return x || 'همهٔ درس ها'; }, function (v) {
        st.subject = v; st.chapter = ''; st.page = 0; draw();
      }));
      body.appendChild(selectBox('فصل', [''].concat(chapterOptions()), st.chapter, function (x) { return x || 'همهٔ فصل ها'; }, function (v) {
        st.chapter = v; st.page = 0; draw();
      }));
      body.appendChild(selectBox('نوع سؤال', [''].concat(typeOptions()), st.type, function (x) { return x || 'همهٔ نوع ها'; }, function (v) {
        st.type = v; st.page = 0; draw();
      }));
      var ds = ['']; DIFF.forEach(function (_, i) { ds.push(String(i)); });
      body.appendChild(selectBox('دشواری', ds, st.difficulty, function (d) { return d === '' ? 'همهٔ سطح ها' : DIFF[Number(d)]; }, function (v) {
        st.difficulty = v; st.page = 0; draw();
      }));
      popup.appendChild(body); scrim.appendChild(popup); card.appendChild(scrim);
    }

    function filtered() {
      var s = String(st.query || '').toLowerCase();
      return st.rows.filter(function (r) {
        if (st.track && String(r.track || '') !== st.track) return false;
        if (st.subject && r.subject !== st.subject) return false;
        if (st.chapter && r.chapter !== st.chapter) return false;
        if (st.type && r.type_fa !== st.type) return false;
        if (st.difficulty !== '' && Number(r.difficulty) !== Number(st.difficulty)) return false;
        if (s) {
          var p = st.details[r.id], detailText = p ? plain(p.q_html || '') + ' ' + (p.options || []).join(' ') : '';
          if ((String(r.text || '') + ' ' + detailText).toLowerCase().indexOf(s) < 0) return false;
        }
        return true;
      });
    }
    function pickedRows() { return filtered().filter(function (r) { return st.picked[r.id]; }); }
    function rowGrade(r) { return Math.max(1, Math.min(12, Number(r.grade) || Number(st.grade) || 1)); }

    function loadDetails() {
      var grades = st.grade ? [st.grade] : uniq(st.rows, 'grade').map(Number);
      grades.forEach(function (g) {
        if (st.detailLoaded[g] || st.detailPending[g]) return;
        st.detailPending[g] = true; draw();
        questions(g).then(function (all) {
          st.detailLoaded[g] = true;
          st.rows.forEach(function (r) { if (rowGrade(r) === g && all[String(r.id)]) st.details[r.id] = all[String(r.id)]; });
        }).catch(function (e) {
          st.detailLoaded[g] = true;
          st.error = st.error || ('بارگیری محتوای پایه ' + fa(g) + ' ناموفق بود.');
        }).then(function () { st.detailPending[g] = false; draw(); });
      });
    }

    function addRows(rows) {
      if (!rows.length) { toast('سؤالی انتخاب نشده است.', 'err'); return; }
      var groups = {};
      rows.forEach(function (r) { var g = rowGrade(r); (groups[g] = groups[g] || []).push(r); });
      var tasks = Object.keys(groups).map(function (g) {
        return questions(Number(g)).then(function (all) {
          return groups[g].map(function (r) { return all[String(r.id)] ? draft(all[String(r.id)], answerOn(r)) : null; }).filter(Boolean);
        }).catch(function () { return []; });
      });
      Promise.all(tasks).then(function (groupsOut) {
        var out = []; groupsOut.forEach(function (list) { out = out.concat(list); });
        if (!out.length) { toast('محتوای سؤال ها در دسترس نیست.', 'err'); return; }
        if (append && root.SiteBuilder && root.SiteBuilder.addRefDrafts && root.SiteBuilder.addRefDrafts(out)) {
          bg.remove(); toast(fa(out.length) + ' سؤال به همین آزمون افزوده شد.', 'ok'); return;
        }
        S.go('builder', {refAdd: out}); bg.remove(); toast(fa(out.length) + ' سؤال به آزمون افزوده شد.', 'ok');
      });
    }

    function preview(r) {
      questions(rowGrade(r)).then(function (all) {
        var p = all[String(r.id)];
        if (!p) { toast('محتوای این سؤال یافت نشد.', 'err'); return; }
        var pbg = el('div', {class: 'modal-bg'});
        var pc = el('div', {class: 'modal wide refbank-preview'});
        pbg.appendChild(pc); document.body.appendChild(pbg);
        pbg.addEventListener('click', function (e) { if (e.target === pbg) pbg.remove(); });
        pc.appendChild(el('div', {class: 'row'}, [el('h3', {class: 'grow', text: 'پیش‌نمایش سؤال'}), el('button', {class: 'x refbank-close', text: '✕', title: 'بستن', onclick: function () { pbg.remove(); }})]));
        pc.appendChild(el('div', {class: 'muted', style: 'font-size:12px', text: metaText(r)}));
        if (p.option_images === undefined) pc.appendChild(el('div', {class: 'alert warn', text: 'این شارد قدیمی است و اطلاعات تصویر گزینه‌ها را ندارد. پنجره را ببندید و دوباره باز کنید.'}));
        var qv = el('div', {class: 'ref-question-body'});
        if (p.q_html) { qv.innerHTML = mathJaxToTokens(p.q_html); renderFormulaSpans(qv); }
        else richInto(qv, p.text || r.text || '');
        pc.appendChild(qv);
        appendOptions(pc, p);
        if (answerOn(r) && (p.answer_html || p.answer_text)) appendAnswer(pc, p);
        pc.appendChild(el('div', {class: 'row refbank-preview-actions'}, [
          el('button', {class: 'btn ref-bulk-add', text: '＋ افزودن به آزمون', onclick: function () { pbg.remove(); addRows([r]); }}),
          el('button', {class: 'btn light', text: 'بستن', onclick: function () { pbg.remove(); }})
        ]));
      }).catch(function (e) { toast('پیش‌نمایش ناموفق بود: ' + String(e.message || e), 'err'); });
    }
    function metaText(r) {
      var bits = ['پایه ' + fa(r.grade || st.grade || 1), r.subject, r.chapter, r.type_fa, r.difficulty_fa];
      if (r.area) bits.push(r.area);
      return bits.filter(Boolean).join(' · ');
    }
    function appendOptions(parent, p) {
      (p.options || []).forEach(function (o, i) {
        var item = el('div', {class: 'ref-option'});
        item.appendChild(richInto(el('div', {class: 'ref-option-text'}), fa(i + 1) + ') ' + String(o || '')));
        var u = (p.option_images || [])[i];
        if (u) item.appendChild(el('img', {src: u, alt: 'تصویر گزینه ' + fa(i + 1), loading: 'lazy'}));
        parent.appendChild(item);
      });
    }
    function appendAnswer(parent, p) {
      if (!p.answer_html && !p.answer_text) return;
      var ans = el('details', {class: 'ref-answer-details'});
      ans.appendChild(el('summary', {text: 'پاسخ'}));
      var av = el('div', {class: 'ref-question-body'});
      if (p.answer_html) { av.innerHTML = mathJaxToTokens(p.answer_html); renderFormulaSpans(av); }
      else richInto(av, p.answer_text);
      ans.appendChild(av);
      splitImages(p).answer.forEach(function (u) { ans.appendChild(el('img', {src: u, alt: 'تصویر پاسخ', loading: 'lazy'})); });
      parent.appendChild(ans);
    }

    function results() {
      var content = el('div', {class: 'refbank-scroll'});
      var f = filtered(), pages = Math.max(1, Math.ceil(f.length / st.size));
      if (st.page >= pages) st.page = pages - 1;
      var slice = f.slice(st.page * st.size, (st.page + 1) * st.size);
      if (!slice.length) content.appendChild(S.emptyBox('🏛', st.missing ? 'فهرست این پایه هنوز بارگذاری نشده است.' : 'سؤالی با این فیلترها پیدا نشد.'));
      slice.forEach(function (r) {
        var detail = st.details[r.id];
        var cb = el('input', {type: 'checkbox', title: 'انتخاب', 'aria-label': 'انتخاب سؤال'}); cb.checked = !!st.picked[r.id];
        cb.addEventListener('change', function () { if (cb.checked) st.picked[r.id] = 1; else delete st.picked[r.id]; draw(); });
        var info = el('div', {class: 'ref-card-meta'});
        info.appendChild(cb);
        info.appendChild(el('span', {text: metaText(r)}));
        var item = el('article', {class: 'card ref-question-card'});
        item.appendChild(info);
        if (detail) {
          var qv = el('div', {class: 'ref-question-body'});
          if (detail.q_html) { qv.innerHTML = mathJaxToTokens(detail.q_html); renderFormulaSpans(qv); }
          else richInto(qv, detail.text || r.text || '');
          item.appendChild(qv);
          appendOptions(item, detail);
          if (answerOn(r)) appendAnswer(item, detail);
        } else if (st.detailPending[rowGrade(r)]) {
          item.appendChild(el('div', {class: 'ref-loading', role: 'status', 'aria-label': 'در حال بارگیری سؤال'}, [el('span', {class: 'ref-spinner'})]));
        } else {
          /* V363 — سطر بی‌متن: راهنما، نه کارت سفید. */
          var rowText = String(r.text || '').trim();
          if (rowText) item.appendChild(richInto(el('div', {class: 'ref-question-body'}), safeSnippet(rowText, 220)));
          else item.appendChild(el('div', {class: 'ref-question-body', style: 'opacity:.7;font-size:12px',
            text: 'متن این سؤال در فهرست این پایه نیست؛ «پیش‌نمایش» متن کامل را نشان می‌دهد.'}));
        }
        var actions = el('div', {class: 'ref-card-actions'});
        actions.appendChild(el('button', {class: 'chip ref-answer-chip' + (answerOn(r) ? ' on' : ''), type: 'button', text: (answerOn(r) ? '✓ ' : '+ ') + 'افزودن پاسخ', title: 'افزودن پاسخ', onclick: function () { setAnswer(r); }}));
        actions.appendChild(el('button', {class: 'btn light sm', type: 'button', text: 'پیش‌نمایش', onclick: function () { preview(r); }}));
        actions.appendChild(el('button', {class: 'btn ref-bulk-add sm', type: 'button', text: '＋ افزودن به آزمون', onclick: function () { addRows([r]); }}));
        item.appendChild(actions); content.appendChild(item);
      });
      return content;
    }

    function draw() {
      card.innerHTML = ''; head();
      var body = el('div', {class: 'refbank-content'});
      if (!st.filterOpen) body.appendChild(results());
      card.appendChild(body);
      if (st.filterOpen) filterPanel();
    }
    function load() {
      st.loading = true; st.error = null; draw();
      var task = st.grade === 0 ? catalogAll() : catalog(st.grade).then(function (rows) {
        return {rows: Array.isArray(rows) ? rows : [], loaded: (rows && rows.length) ? [st.grade] : []};
      }).catch(function (e) { return {rows: [], loaded: [], error: String(e.message || e)}; });
      task.then(function (res) {
        st.rows = res.rows || []; st.loaded = res.loaded || [];
        st.picked = {}; st.loading = false; st.missing = !st.rows.length; st.error = res.error || null;
        st.note = null; draw();
      });
    }

    load();
  }

  root.RefBank = {open: open, base: BASE, catalog: catalog, questions: questions, draft: draft,
    mathJaxToTokens: mathJaxToTokens};
})(window);
