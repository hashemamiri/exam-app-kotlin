/* V360.3 — بانک سؤال مرجع، فقطخواندنی از روی فضای ابری.
 *
 * قید کاربر: هیچ سؤال یا تصویری وارد باندل سایت یا APK نشود.
 * این پیمانه فقط یک شارد کوچک «فهرست» برای پایهٔ انتخابشده میگیرد؛
 * متن کامل سؤال و تصویرها تنها هنگام پیشنمایش یا افزودن خوانده میشوند.
 *
 * فیلتر مرحلهای: پایه ← رشته (فقط ۱۰ تا ۱۲) ← درس ← فصل ← نوع ← دشواری
 * ← تأیید ← فهرست سؤالها با انتخاب گروهی. پاسخ و تصویر پاسخ با تیک کاربر
 * و فقط در «بخش پاسخ» (کلید پاسخ) کپی میشوند، هرگز در متن/تصویر سؤال.
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
  var cache = {catalog: {}, questions: {}};

  function url(path) { return BASE + '/' + path; }
  function getJSON(path) {
    return fetch(url(path), {cache: 'force-cache'}).then(function (r) {
      if (!r.ok) throw new Error('HTTP ' + r.status);
      return r.json();
    });
  }
  function catalog(grade) {
    var k = String(grade);
    if (!cache.catalog[k]) cache.catalog[k] = getJSON('catalog/g' + k + '.json');
    return cache.catalog[k];
  }
  function questions(grade) {
    var k = String(grade);
    if (!cache.questions[k]) cache.questions[k] = getJSON('questions/g' + k + '.json');
    return cache.questions[k];
  }
  function uniq(rows, key) {
    var seen = {}, out = [];
    rows.forEach(function (r) { var v = String(r[key] || '').trim(); if (v && !seen[v]) { seen[v] = 1; out.push(v); } });
    return out.sort(function (a, b) { return a.localeCompare(b, 'fa'); });
  }
  function gradeLabel(g) { return 'پایهٔ ' + fa(g); }
  function tracksFor(g) { return g >= 10 ? TRACKS : []; }
  function subjectsFor(g, track) {
    if (g >= 1 && g <= 6) return PRIMARY;
    if (g >= 7 && g <= 9) return MIDDLE;
    if (g >= 10 && g <= 12) return track ? (HIGH[track] || ALL_HIGH) : ALL_HIGH;
    return [];
  }

  /* V360.3 — تصویر پاسخ از answer_html شناخته میشود؛ نیازی به شارد تازهای نیست. */
  function imgUrls(html) {
    var out = [], m, re = /<img[^>]+src=["']([^"']+)["']/gi;
    while ((m = re.exec(html || '')) !== null) if (out.indexOf(m[1]) < 0) out.push(m[1]);
    return out;
  }
  function splitImages(p) {
    var all = p.image_urls || [], ans = imgUrls(p.answer_html || '').filter(function (u) { return all.indexOf(u) >= 0; });
    return {question: all.filter(function (u) { return ans.indexOf(u) < 0; }), answer: ans};
  }
  function plain(html) {
    return String(html || '').replace(/<[^>]*>/g, ' ').replace(/&zwnj;/g, ' ').replace(/&nbsp;/g, ' ')
      .replace(/&amp;/g, '&').replace(/[\u200c\u00a0]/g, ' ').replace(/\s+/g, ' ').trim();
  }

  /* تبدیل سؤال مرجع به شیء قابل decodeQuestion در سازندهٔ آزمون. */
  function draft(p, includeAnswer) {
    var inc = includeAnswer !== false, sp = splitImages(p);
    var q = {
      type: p.type || 'essay', text: p.text || '', grade: p.grade || 0,
      subject: p.subject || '', topic: p.topic || '', difficulty: p.difficulty || 0,
      score: 1, options: p.options || [], correctIndex: p.correct_index == null ? null : p.correct_index,
      expectedText: '', images: sp.question.slice(0, 8),
      answerExplanation: inc ? (plain(p.answer_html) || p.answer_text || '') : '',
      answerImages: inc ? sp.answer.slice(0, 8) : []
    };
    if (q.type === 'multiple' && (!q.options || q.options.length < 2)) q.type = 'essay';
    if (q.type === 'fill') q.expectedText = p.answer_text || '';
    if (q.type === 'truefalse') q.expectedText = /غلط|نادرست/.test(p.answer_text || '') ? 'false' : 'true';
    return q;
  }

  function open(opts) {
    opts = opts || {};
    var append = !!opts.append;
    var bg = el('div', {class: 'modal-bg'});
    var card = el('div', {class: 'modal wide', style: 'max-width:960px;width:96vw;max-height:90vh;overflow:auto'});
    bg.appendChild(card); document.body.appendChild(bg);
    bg.addEventListener('click', function (e) { if (e.target === bg) bg.remove(); });
    var st = {
      grade: 0, gradeChosen: false, track: '', trackChosen: false, subject: '', subjectChosen: false,
      chapter: '', type: '', difficulty: '', query: '', page: 0, size: 25,
      rows: [], picked: {}, includeAnswer: true, stage: 0, results: false, missing: false, loading: false
    };

    function head() {
      var row = [el('h2', {class: 'grow', text: 'بانک سؤال مرجع'})];
      if (st.results) row.push(el('button', {class: 'btn light sm', text: '🔍 فیلتر', title: 'بازگشت به فیلتر مرحلهای', onclick: function () { st.results = false; st.stage = 0; st.gradeChosen = false; st.trackChosen = false; st.subjectChosen = false; draw(); }}));
      row.push(el('button', {class: 'x', text: '✕', onclick: function () { bg.remove(); }}));
      card.appendChild(el('div', {class: 'row', style: 'gap:8px;align-items:center;flex-wrap:wrap'}, row));
      card.appendChild(el('p', {class: 'muted', text: 'سؤالها و تصویرها در فضای ابریاند؛ فقط نتیجهٔ پالایششده دانلود میشود.'}));
    }

    function chip(text, selected, onPick) {
      return el('button', {
        class: 'chip' + (selected ? ' on' : ''), text: text,
        style: 'cursor:pointer;margin:2px;white-space:nowrap', type: 'button',
        onclick: onPick
      });
    }
    function stepBox(title, values, current, chosen, label, onPick) {
      card.appendChild(el('h4', {text: title, style: 'margin:10px 0 4px'}));
      var box = el('div', {class: 'row', style: 'gap:4px;flex-wrap:wrap'});
      values.forEach(function (v) { box.appendChild(chip(label(v), chosen && current === v, function () { onPick(v); draw(); })); });
      card.appendChild(box);
    }
    function subjectOptions() {
      if (!st.rows.length) return subjectsFor(st.grade, st.track);
      var base = st.rows.filter(function (r) { return !st.track || String(r.track || '') === st.track; });
      var out = uniq(base, 'subject');
      return out.length ? out : subjectsFor(st.grade, st.track);
    }
    function chapterOptions() {
      return uniq(st.rows.filter(function (r) {
        return (!st.track || String(r.track || '') === st.track) && (!st.subject || r.subject === st.subject);
      }), 'chapter');
    }
    function typeOptions() {
      var out = uniq(st.rows, 'type_fa');
      return out.length ? out : ['تستی', 'تشریحی', 'جایخالی', 'صحیح/غلط'];
    }
    function ready() {
      if (st.stage === 0) return st.gradeChosen;
      if (st.stage === 1) return st.trackChosen;
      if (st.stage === 2) return st.subjectChosen;
      return true;
    }
    function nextStep() {
      if (!ready()) { toast('نخست همین گام را انتخاب کنید.', 'err'); return; }
      if (st.stage === 0) st.stage = st.grade >= 10 ? 1 : 2;
      else st.stage = Math.min(5, st.stage + 1);
      draw();
    }
    function prevStep() {
      if (st.stage === 2) st.stage = st.grade >= 10 ? 1 : 0;
      else st.stage = Math.max(0, st.stage - 1);
      draw();
    }

    function wizard() {
      card.appendChild(el('p', {class: 'muted', style: 'font-size:12px', text: 'ترتیب فیلتر: پایه ← رشته (فقط پایهٔ ۱۰ تا ۱۲) ← درس ← فصل ← نوع سؤال ← دشواری'}));
      if (st.missing) card.appendChild(el('div', {class: 'alert warn', text: 'فهرست این پایه هنوز روی فضای ابری بارگذاری نشده است؛ فیلترها قابل دیدناند ولی سؤالی نمایش داده نمیشود.'}));
      if (st.loading) card.appendChild(el('div', {class: 'muted', text: 'در حال خواندن فهرست پایه…'}));

      if (st.stage >= 0) {
        var gs = []; for (var g = 1; g <= 12; g++) gs.push(g);
        stepBox('۱) پایه را تعیین کنید', gs, st.grade, st.gradeChosen, gradeLabel, function (v) {
          st.grade = v; st.gradeChosen = true; st.track = ''; st.trackChosen = false;
          st.subject = ''; st.subjectChosen = false; st.chapter = ''; st.type = '';
          st.rows = []; st.picked = {}; st.missing = false; load();
        });
      }
      if (st.stage >= 1 && st.grade >= 10) {
        stepBox('۲) رشته را انتخاب کنید', TRACKS, st.track, st.trackChosen, function (t) { return t || 'همهٔ رشتهها'; }, function (v) {
          st.track = v; st.trackChosen = true; st.subject = ''; st.subjectChosen = false; st.chapter = '';
        });
      }
      if (st.stage >= 2) {
        stepBox('۳) درس', [''].concat(subjectOptions()), st.subject, st.subjectChosen, function (s) { return s || 'همهٔ درسها'; }, function (v) {
          st.subject = v; st.subjectChosen = true; st.chapter = '';
        });
      }
      if (st.stage >= 3) {
        stepBox('۴) فصل', [''].concat(chapterOptions()), st.chapter, true, function (c) { return c || 'همهٔ فصلها'; }, function (v) { st.chapter = v; });
      }
      if (st.stage >= 4) {
        stepBox('۵) نوع سؤال', [''].concat(typeOptions()), st.type, true, function (t) { return t || 'همهٔ انواع'; }, function (v) { st.type = v; });
      }
      if (st.stage >= 5) {
        stepBox('۶) دشواری', [''].concat(DIFF.map(function (_, i) { return String(i); })), st.difficulty, true,
          function (d) { return d === '' ? 'همهٔ سطحها' : DIFF[Number(d)]; }, function (v) { st.difficulty = v; });
      }
      var q = el('input', {type: 'search', placeholder: 'جستوجو در متن سؤال…', style: 'min-width:220px;flex:1;margin-top:10px'});
      q.value = st.query; q.addEventListener('input', function () { st.query = q.value.trim(); st.page = 0; });
      card.appendChild(q);

      var inc = el('input', {type: 'checkbox'}); inc.checked = st.includeAnswer;
      inc.addEventListener('change', function () { st.includeAnswer = inc.checked; });
      card.appendChild(el('label', {class: 'row', style: 'gap:6px;align-items:center;margin-top:8px'}, [inc, el('span', {text: 'افزودن پاسخ و تصویر پاسخ — فقط در بخش پاسخ'})]));

      var foot = el('div', {class: 'row', style: 'gap:8px;margin-top:14px;align-items:center'});
      if (st.stage > 0) foot.appendChild(el('button', {class: 'btn light sm', text: 'گام قبل', onclick: prevStep}));
      foot.appendChild(el('span', {class: 'grow'}));
      var last = st.stage === 5;
      foot.appendChild(el('button', {
        class: 'btn sm', text: last ? 'تأیید فیلتر و نمایش سؤالها' : 'بعدی',
        onclick: function () { if (last) { st.results = true; st.page = 0; draw(); } else nextStep(); }
      }));
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
    function pickedRows() {
      return filtered().filter(function (r) { return st.picked[r.id]; });
    }
    function addRows(rows) {
      if (!rows.length) { toast('سؤالی انتخاب نشده است.', 'err'); return; }
      questions(st.grade).then(function (all) {
        var out = rows.map(function (r) { return all[String(r.id)] ? draft(all[String(r.id)], st.includeAnswer) : null; }).filter(Boolean);
        if (!out.length) { toast('محتوای سؤالها در دسترس نیست.', 'err'); return; }
        if (append && root.SiteBuilder && root.SiteBuilder.addRefDrafts && root.SiteBuilder.addRefDrafts(out)) {
          bg.remove(); toast(fa(out.length) + ' سؤال به همین آزمون افزوده شد.', 'ok'); return;
        }
        S.go('builder', { refAdd: out });
        bg.remove();
        toast(fa(out.length) + ' سؤال به آزمون افزوده شد.', 'ok');
      }).catch(function (e) { toast('خواندن سؤالها ناموفق بود: ' + String(e.message || e), 'err'); });
    }

    function preview(r) {
      questions(st.grade).then(function (all) {
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
        /* تصویرهای سؤال در متن سؤال؛ تصویرهای پاسخ فقط در بخش پاسخ. */
        var qv = el('div', {style: 'margin:10px 0;line-height:1.9'}); qv.innerHTML = p.q_html || '';
        pc.appendChild(qv);
        if ((p.options || []).length) {
          pc.appendChild(el('div', {style: 'margin:8px 0', html: p.options.map(function (o, i) {
            return '<div>' + fa(i + 1) + ') ' + esc(o) + '</div>';
          }).join('')}));
        }
        if (st.includeAnswer && (p.answer_html || p.answer_text)) {
          var det = el('details', {}, [el('summary', {text: 'پاسخ / راهنما'})]);
          var av = el('div', {style: 'margin-top:8px;line-height:1.9'}); av.innerHTML = p.answer_html || esc(p.answer_text || '');
          det.appendChild(av); pc.appendChild(det);
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
      var f = filtered(), pages = Math.max(1, Math.ceil(f.length / st.size));
      if (st.page >= pages) st.page = pages - 1;
      var slice = f.slice(st.page * st.size, (st.page + 1) * st.size);
      var bar = el('div', {class: 'row', style: 'gap:8px;align-items:center;flex-wrap:wrap'});
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
      if (!slice.length) body.appendChild(S.emptyBox('🏛', st.missing ? 'فهرست این پایه هنوز بارگذاری نشده است.' : 'سؤالی با این فیلترها پیدا نشد؛ دکمهٔ فیلتر را بزنید.'));
      slice.forEach(function (r) {
        var cb = el('input', {type: 'checkbox', title: 'انتخاب'}); cb.checked = !!st.picked[r.id];
        cb.addEventListener('change', function () { if (cb.checked) st.picked[r.id] = 1; else delete st.picked[r.id]; draw(); });
        var imgs = (r.image_urls || []).slice(0, 1).map(function (u) {
          return el('img', {src: u, alt: '', loading: 'lazy', style: 'max-width:100%;max-height:120px;border-radius:8px'});
        });
        body.appendChild(el('div', {class: 'card', style: 'margin:8px 0;padding:10px'}, [
          el('div', {class: 'row', style: 'gap:8px;align-items:center'}, [
            cb,
            el('span', {class: 'muted', style: 'font-size:12px', text: [r.subject, r.chapter, r.type_fa, r.difficulty_fa].filter(Boolean).join(' · ')}),
            el('span', {class: 'grow'}),
            r.has_image ? el('span', {class: 'chip', text: '🖼 تصویر'}) : null
          ].filter(Boolean)),
          el('div', {style: 'margin:6px 0', text: String(r.text || '').slice(0, 220)}),
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

    function draw() { card.innerHTML = ''; head(); if (st.results) results(); else wizard(); }
    function load() {
      if (!st.grade) return draw();
      st.loading = true; draw();
      catalog(st.grade).then(function (rows) {
        st.rows = Array.isArray(rows) ? rows : [];
        st.picked = {}; st.loading = false; st.missing = !st.rows.length;
        draw();
      }).catch(function (e) {
        st.rows = []; st.loading = false; st.missing = true;
        st.error = 'فهرست این پایه هنوز بارگذاری نشده یا قابل دسترسی نیست: ' + String(e.message || e);
        draw();
        card.appendChild(el('div', {class: 'alert warn', text: st.error}));
      });
    }

    draw();
  }

  root.RefBank = {open: open, base: BASE, catalog: catalog, questions: questions, draft: draft};
})(window);
