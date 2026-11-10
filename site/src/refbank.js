/* V360.5 — بانک سؤال مرجع، فقطخواندنی از روی فضای ابری.
 *
 * قید کاربر: هیچ سؤال یا تصویری وارد باندل سایت یا APK نشود.
 *
 * تصمیم تازهٔ کاربر: پنجرهٔ فیلتر **همه کشویی** است و همه پیشفرض «همه».
 * نیازی به پیمودن گامها نیست؛ کاربر میتواند فقط پایه را بگزیند و فیلتر
 * را ببندد تا سؤالها را ببیند. تا پیش از انتخاب پایه، درس فقط «همه»
 * دارد؛ فصل تا انتخاب درس فقط «همه»؛ ولی نوع سؤال و دشواری همیشه
 * همهٔ گزینهها را دارند. رشته فقط برای پایهٔ ۱۰ تا ۱۲ دیده میشود.
 * پایهٔ «همه» یعنی فقط شاردهایی که واقعاً بارگذاری شدهاند.
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
  var TYPES = ['تستی', 'تشریحی', 'جایخالی', 'صحیح/غلط', 'دستهبندی', 'مرتبسازی', 'کوتاهپاسخ'];
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
  var SHARD_VERSION = 'v3609';
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
  function gradeLabel(g) { return Number(g) === 0 ? 'همهٔ پایهها' : 'پایهٔ ' + fa(g); }
  function tracksFor(g) { return Number(g) >= 10 ? TRACKS : []; }
  function subjectsFor(g, track) {
    g = Number(g);
    if (g >= 1 && g <= 6) return PRIMARY;
    if (g >= 7 && g <= 9) return MIDDLE;
    if (g >= 10 && g <= 12) return track ? (HIGH[track] || ALL_HIGH) : ALL_HIGH;
    return [];
  }

  /* تصویر پاسخ از answer_html شناخته میشود؛ نیازی به شارد تازهای نیست. */
  function imgUrls(html) {
    var out = [], m, re = /<img[^>]+src=["']([^"']+)["']/gi;
    while ((m = re.exec(html || '')) !== null) if (out.indexOf(m[1]) < 0) out.push(m[1]);
    return out;
  }
  function splitImages(p) {
    var all = p.image_urls || [];
    /* V360.8 — صورت سؤال فقط تصویرهای q_html را میگیرد؛ تصویر گزینهها
       جداگانه از option_images خوانده میشود و هرگز وارد متن سؤال نمیشود. */
    var q = imgUrls(p.q_html || '').filter(function (u) { return all.indexOf(u) >= 0; });
    var ans = imgUrls(p.answer_html || '').filter(function (u) { return all.indexOf(u) >= 0; });
    var opts = (p.option_images || []).map(function (u) { return u || null; });
    return {question: q, answer: ans, options: opts};
  }
  function plain(html) {
    return String(html || '').replace(/<[^>]*>/g, ' ').replace(/&zwnj;/g, ' ').replace(/&nbsp;/g, ' ')
      .replace(/&amp;/g, '&').replace(/[\u200c\u00a0]/g, ' ').replace(/\s+/g, ' ').trim();
  }
  function draft(p, includeAnswer) {
    var inc = includeAnswer !== false, sp = splitImages(p);
    /* V361 — حتی اگر شارد خام مستقیم رسید، فرمولها به توکن ویرایشگر بروند. */
    var textTokens = mathJaxToTokens(p.text || '');
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
    if (q.type === 'multiple' && (!q.options || q.options.length < 2)) q.type = 'essay';
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
    /* V360.9 — هر بار پنجره باز میشود، حافظهٔ درونبرنامهای پاک شود. */
    cache.catalog = {}; cache.questions = {}; cache.catalogAll = null;
    var bg = el('div', {class: 'modal-bg'});
    var card = el('div', {class: 'modal wide', style: 'max-width:960px;width:96vw;max-height:90vh;overflow:auto'});
    bg.appendChild(card); document.body.appendChild(bg);
    bg.addEventListener('click', function (e) { if (e.target === bg) bg.remove(); });
    var st = {
      grade: 0, track: '', subject: '', chapter: '', type: '', difficulty: '', query: '',
      page: 0, size: 25, rows: [], loaded: [], picked: {}, includeAnswer: true,
      filterOpen: true, missing: false, loading: false, note: null, error: null
    };

    function head() {
      var row = [el('h2', {class: 'grow', text: 'بانک سؤال مرجع'})];
      if (!st.filterOpen) row.push(el('button', {class: 'btn light sm', text: '🔍 فیلتر', title: 'بازگشت به فیلتر کشویی', onclick: function () { st.filterOpen = true; draw(); }}));
      row.push(el('button', {class: 'x', text: '✕', onclick: function () { bg.remove(); }}));
      card.appendChild(el('div', {class: 'row', style: 'gap:8px;align-items:center;flex-wrap:wrap'}, row));
      card.appendChild(el('p', {class: 'muted', text: 'سؤالها و تصویرها در فضای ابریاند؛ فقط نتیجهٔ پالایششده دانلود میشود.'}));
    }

    function selectBox(title, values, current, label, onPick) {
      var s = el('select', {title: title, style: 'border:1px solid var(--line);border-radius:10px;padding:8px;min-width:180px'});
      values.forEach(function (v) { s.appendChild(el('option', {value: String(v), text: label(v)})); });
      s.value = String(current);
      s.addEventListener('change', function () { onPick(s.value); });
      return el('label', {class: 'field', style: 'display:flex;flex-direction:column;gap:4px;margin-bottom:8px'}, [el('span', {text: title}), s]);
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
      card.appendChild(el('p', {class: 'muted', style: 'font-size:12px', text: 'همهٔ فیلترها کشویی و پیشفرض «همه»اند؛ میتوانید فقط پایه را بگزینید و فیلتر را ببندید.'}));
      /* V360.9 — نسخهٔ شارد و دکمهٔ بررسی تازگی، قابل دیدن برای کاربر. */
      card.appendChild(el('div', {class: 'row', style: 'gap:8px;align-items:center;flex-wrap:wrap;margin-bottom:8px'}, [
        el('span', {class: 'muted', style: 'font-size:12px', text: 'نسخهٔ شارد: ' + SHARD_VERSION + ' (بدون کش)'}),
        el('button', {class: 'btn light sm', text: '🔄 بررسی شارد', onclick: function () {
          var g = Number(st.grade) || 1;
          questions(g).then(function (all) {
            var vals = Object.keys(all || {}).map(function (k) { return all[k]; });
            if (!vals.length) return toast('شاردی برای این پایه یافت نشد.', 'err');
            var withField = vals.filter(function (p) { return p && p.option_images !== undefined; }).length;
            if (!withField) return toast('شارد قدیمی است؛ فیلد تصویر گزینهها وجود ندارد.', 'err');
            var withImg = vals.filter(function (p) {
              return p && Array.isArray(p.option_images) && p.option_images.some(Boolean);
            }).length;
            toast('شارد تازه: ' + fa(withImg) + ' سؤال تصویر گزینه دارند.', 'ok');
          }).catch(function (e) { toast('خواندن شارد ناموفق: ' + String(e.message || e), 'err'); });
        }})
      ]));
      if (st.note) card.appendChild(el('p', {class: 'muted', style: 'font-size:12px', text: st.note}));
      if (st.loading) card.appendChild(el('div', {class: 'muted', text: 'در حال خواندن فهرست پایهها…'}));
      if (st.missing) card.appendChild(el('div', {class: 'alert warn', text: 'فهرست این پایه هنوز روی فضای ابری بارگذاری نشده است.'}));
      if (st.error) card.appendChild(el('div', {class: 'alert warn', text: st.error}));

      var gs = [0]; for (var g = 1; g <= 12; g++) gs.push(g);
      card.appendChild(selectBox('پایه', gs, st.grade, gradeLabel, function (v) {
        st.grade = Number(v) || 0; st.track = ''; st.subject = ''; st.chapter = '';
        st.page = 0; st.picked = {}; st.error = null; load();
      }));
      if (Number(st.grade) >= 10) {
        card.appendChild(selectBox('رشته', TRACKS, st.track, function (t) { return t || 'همهٔ رشتهها'; }, function (v) {
          st.track = v; st.subject = ''; st.chapter = ''; st.page = 0; draw();
        }));
      }
      card.appendChild(selectBox('درس', [''].concat(subjectOptions()), st.subject, function (x) { return x || 'همهٔ درسها'; }, function (v) {
        st.subject = v; st.chapter = ''; st.page = 0; draw();
      }));
      card.appendChild(selectBox('فصل', [''].concat(chapterOptions()), st.chapter, function (x) { return x || 'همهٔ فصلها'; }, function (v) {
        st.chapter = v; st.page = 0; draw();
      }));
      card.appendChild(selectBox('نوع سؤال', [''].concat(typeOptions()), st.type, function (x) { return x || 'همهٔ انواع'; }, function (v) {
        st.type = v; st.page = 0; draw();
      }));
      var ds = ['']; DIFF.forEach(function (_, i) { ds.push(String(i)); });
      card.appendChild(selectBox('دشواری', ds, st.difficulty, function (d) { return d === '' ? 'همهٔ سطحها' : DIFF[Number(d)]; }, function (v) {
        st.difficulty = v; st.page = 0; draw();
      }));

      var inc = el('input', {type: 'checkbox'}); inc.checked = st.includeAnswer;
      inc.addEventListener('change', function () { st.includeAnswer = inc.checked; });
      card.appendChild(el('label', {class: 'row', style: 'gap:6px;align-items:center;margin-top:8px'}, [inc, el('span', {text: 'افزودن پاسخ و تصویر پاسخ — فقط در بخش پاسخ'})]));

      var foot = el('div', {class: 'row', style: 'gap:8px;margin-top:14px;align-items:center;flex-wrap:wrap'});
      foot.appendChild(el('button', {class: 'btn light sm', text: 'بستن پنجره', onclick: function () { bg.remove(); }}));
      foot.appendChild(el('span', {class: 'grow'}));
      foot.appendChild(el('button', {class: 'btn sm', text: 'بستن فیلتر و نمایش سؤالها', onclick: function () { st.filterOpen = false; st.page = 0; draw(); }}));
      card.appendChild(foot);
    }

    function filtered() {
      var s = String(st.query || '').toLowerCase();
      return st.rows.filter(function (r) {
        if (st.track && String(r.track || '') !== st.track) return false;
        if (st.subject && r.subject !== st.subject) return false;
        if (st.chapter && r.chapter !== st.chapter) return false;
        if (st.type && r.type_fa !== st.type) return false;
        if (st.difficulty !== '' && Number(r.difficulty) !== Number(st.difficulty)) return false;
        if (s && String(r.text || '').toLowerCase().indexOf(s) < 0) return false;
        return true;
      });
    }
    function pickedRows() { return filtered().filter(function (r) { return st.picked[r.id]; }); }
    function rowGrade(r) { return Math.max(1, Math.min(12, Number(r.grade) || Number(st.grade) || 1)); }

    function addRows(rows) {
      if (!rows.length) { toast('سؤالی انتخاب نشده است.', 'err'); return; }
      var byGrade = {};
      rows.forEach(function (r) { var g = rowGrade(r); (byGrade[g] = byGrade[g] || []).push(r); });
      var tasks = Object.keys(byGrade).map(function (g) {
        return questions(Number(g)).then(function (all) {
          return byGrade[g].map(function (r) { return all[String(r.id)] ? draft(all[String(r.id)], st.includeAnswer) : null; }).filter(Boolean);
        }).catch(function () { return []; });
      });
      Promise.all(tasks).then(function (groups) {
        var out = []; groups.forEach(function (list) { out = out.concat(list); });
        if (!out.length) { toast('محتوای سؤالها در دسترس نیست.', 'err'); return; }
        if (append && root.SiteBuilder && root.SiteBuilder.addRefDrafts && root.SiteBuilder.addRefDrafts(out)) {
          bg.remove(); toast(fa(out.length) + ' سؤال به همین آزمون افزوده شد.', 'ok'); return;
        }
        S.go('builder', { refAdd: out });
        bg.remove();
        toast(fa(out.length) + ' سؤال به آزمون افزوده شد.', 'ok');
      });
    }

    function preview(r) {
      questions(rowGrade(r)).then(function (all) {
        var p = all[String(r.id)];
        if (!p) { toast('محتوای این سؤال یافت نشد.', 'err'); return; }
        var pbg = el('div', {class: 'modal-bg'});
        var pc = el('div', {class: 'modal wide', style: 'max-width:820px;width:94vw;max-height:88vh;overflow:auto'});
        pbg.appendChild(pc); document.body.appendChild(pbg);
        pbg.addEventListener('click', function (e) { if (e.target === pbg) pbg.remove(); });
        pc.appendChild(el('div', {class: 'row'}, [
          el('h3', {class: 'grow', text: 'پیشنمایش سؤال'}),
          el('button', {class: 'x', text: '✕', onclick: function () { pbg.remove(); }})
        ]));
        pc.appendChild(el('div', {class: 'muted', style: 'font-size:12px', text: [r.subject, r.chapter, r.type_fa, r.difficulty_fa].filter(Boolean).join(' · ')}));
        /* V360.9 — تشخیص قابل دیدن شارد کهنه. */
        if (p.option_images === undefined) {
          pc.appendChild(el('div', {class: 'alert warn', text: 'هشدار: شارد قدیمی است؛ فیلد تصویر گزینهها وجود ندارد. پنجره را ببندید و دوباره باز کنید.'}));
        } else {
          var n = (p.option_images || []).filter(Boolean).length;
          if (n) pc.appendChild(el('div', {class: 'muted', style: 'font-size:12px', text: 'تصویر گزینهها: ' + fa(n)}));
        }
        var qv = el('div', {style: 'margin:10px 0;line-height:1.9'});
        qv.innerHTML = mathJaxToTokens(p.q_html || '');
        pc.appendChild(qv);
        renderFormulaSpans(qv);
        if ((p.options || []).length) {
          var obox = el('div', {style: 'margin:8px 0'});
          (p.options || []).forEach(function (o, i) {
            obox.appendChild(richInto(el('div'), fa(i + 1) + ') ' + String(o || '')));
            var ou = (p.option_images || [])[i];
            if (ou) obox.appendChild(el('img', {src: ou, alt: 'تصویر گزینه', loading: 'lazy',
              style: 'max-width:100%;max-height:180px;border-radius:8px;margin:4px 0'}));
          });
          pc.appendChild(obox);
        }
        if (st.includeAnswer && (p.answer_html || p.answer_text)) {
          var det = el('details', {}, [el('summary', {text: 'پاسخ / راهنما'})]);
          var av = el('div', {style: 'margin-top:8px;line-height:1.9'});
          av.innerHTML = mathJaxToTokens(p.answer_html || esc(p.answer_text || ''));
          det.appendChild(av); pc.appendChild(det);
          renderFormulaSpans(av);
          splitImages(p).answer.forEach(function (u) {
            det.appendChild(el('img', {src: u, alt: 'تصویر پاسخ', loading: 'lazy', style: 'max-width:100%;max-height:220px;border-radius:8px;margin-top:6px'}));
          });
        }
        pc.appendChild(el('div', {class: 'row', style: 'margin-top:12px'}, [
          el('button', {class: 'btn', text: '➕ افزودن به آزمون', onclick: function () { pbg.remove(); addRows([r]); }}),
          el('button', {class: 'btn light', text: 'بستن', onclick: function () { pbg.remove(); }})
        ]));
      }).catch(function (e) { toast('پیشنمایش ناموفق بود: ' + String(e.message || e), 'err'); });
    }

    function results() {
      if (!st.grade) card.appendChild(el('p', {class: 'muted', style: 'font-size:12px', text: st.note || 'نمایش سؤالهای پایههای بارگذاریشده'}));
      var q = el('input', {type: 'search', placeholder: 'جستوجو در متن سؤال…', style: 'min-width:220px;flex:1'});
      q.value = st.query; q.addEventListener('input', function () { st.query = q.value.trim(); st.page = 0; draw(); });
      card.appendChild(q);
      var f = filtered(), pages = Math.max(1, Math.ceil(f.length / st.size));
      if (st.page >= pages) st.page = pages - 1;
      var slice = f.slice(st.page * st.size, (st.page + 1) * st.size);
      var bar = el('div', {class: 'row', style: 'gap:8px;align-items:center;flex-wrap:wrap;margin-top:8px'});
      bar.appendChild(el('span', {class: 'muted', text: fa(f.length) + ' سؤال · صفحهٔ ' + fa(st.page + 1) + ' از ' + fa(pages) + ' · ' + fa(pickedRows().length) + ' انتخابشده'}));
      bar.appendChild(el('button', {class: 'btn light sm', text: 'انتخاب/برداشتن صفحه', onclick: function () {
        var ids = slice.map(function (r) { return r.id; }), all = ids.every(function (id) { return st.picked[id]; });
        ids.forEach(function (id) { if (all) delete st.picked[id]; else st.picked[id] = 1; }); draw();
      }}));
      bar.appendChild(el('button', {class: 'btn light sm', text: 'پاک کردن انتخاب', onclick: function () { st.picked = {}; draw(); }}));
      var inc = el('input', {type: 'checkbox'}); inc.checked = st.includeAnswer;
      inc.addEventListener('change', function () { st.includeAnswer = inc.checked; });
      bar.appendChild(el('label', {class: 'row', style: 'gap:6px;align-items:center'}, [inc, el('span', {text: 'پاسخ و تصویر پاسخ فقط در بخش پاسخ'})]));
      card.appendChild(bar);

      var body = el('div');
      if (!slice.length) body.appendChild(S.emptyBox('🏛', st.missing ? 'فهرست این پایه هنوز بارگذاری نشده است.' : 'سؤالی با این فیلترها پیدا نشد؛ آیکن فیلتر را بزنید.'));
      slice.forEach(function (r) {
        var cb = el('input', {type: 'checkbox', title: 'انتخاب'}); cb.checked = !!st.picked[r.id];
        cb.addEventListener('change', function () { if (cb.checked) st.picked[r.id] = 1; else delete st.picked[r.id]; draw(); });
        var imgs = (r.image_urls || []).slice(0, 1).map(function (u) {
          return el('img', {src: u, alt: '', loading: 'lazy', style: 'max-width:100%;max-height:120px;border-radius:8px'});
        });
        body.appendChild(el('div', {class: 'card', style: 'margin:8px 0;padding:10px'}, [
          el('div', {class: 'row', style: 'gap:8px;align-items:center'}, [
            cb,
            el('span', {class: 'muted', style: 'font-size:12px', text: ['پایه ' + fa(r.grade || st.grade || 1), r.subject, r.chapter, r.type_fa, r.difficulty_fa].filter(Boolean).join(' · ')}),
            el('span', {class: 'grow'}),
            r.has_image ? el('span', {class: 'chip', text: '🖼 تصویر'}) : null
          ].filter(Boolean)),
          richInto(el('div', {style: 'margin:6px 0'}), String(r.text || '').slice(0, 220)),
          imgs.length ? el('div', {}, imgs) : null,
          el('div', {class: 'row', style: 'gap:6px;margin-top:6px;flex-wrap:wrap'}, [
            el('button', {class: 'btn light sm', text: '👁 پیشنمایش', onclick: function () { preview(r); }}),
            el('button', {class: 'btn sm', text: '➕ افزودن به آزمون', onclick: function () { addRows([r]); }})
          ])
        ]));
      });
      card.appendChild(body);
      var prev = el('button', {class: 'btn light sm', text: 'قبلی', disabled: st.page === 0 ? 'disabled' : null});
      var next = el('button', {class: 'btn light sm', text: 'بعدی', disabled: st.page >= pages - 1 ? 'disabled' : null});
      prev.addEventListener('click', function () { st.page--; draw(); });
      next.addEventListener('click', function () { st.page++; draw(); });
      var foot = el('div', {class: 'row', style: 'gap:8px;margin-top:10px;align-items:center;flex-wrap:wrap'});
      foot.appendChild(prev); foot.appendChild(next); foot.appendChild(el('span', {class: 'grow'}));
      foot.appendChild(el('button', {class: 'btn', text: 'افزودن گروهی (' + fa(pickedRows().length) + ') به آزمون', onclick: function () { addRows(pickedRows()); }}));
      card.appendChild(foot);
    }

    function draw() { card.innerHTML = ''; head(); if (st.filterOpen) filterPanel(); else results(); }
    function load() {
      st.loading = true; st.error = null; draw();
      var task = st.grade === 0 ? catalogAll() : catalog(st.grade).then(function (rows) {
        return {rows: Array.isArray(rows) ? rows : [], loaded: (rows && rows.length) ? [st.grade] : []};
      }).catch(function (e) { return {rows: [], loaded: [], error: String(e.message || e)}; });
      task.then(function (res) {
        st.rows = res.rows || []; st.loaded = res.loaded || [];
        st.picked = {}; st.loading = false; st.missing = !st.rows.length; st.error = res.error || null;
        st.note = st.grade === 0
          ? (st.loaded.length ? 'پایههای بارگذاریشده: ' + st.loaded.map(fa).join('، ') : 'هیچ پایهای بارگذاری نشده است.')
          : (st.rows.length ? null : 'فهرست این پایه هنوز بارگذاری نشده است.');
        draw();
      });
    }

    load();
  }

  root.RefBank = {open: open, base: BASE, catalog: catalog, questions: questions, draft: draft,
    mathJaxToTokens: mathJaxToTokens};
})(window);
