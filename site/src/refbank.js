/* V360 — بانک سؤال مرجع، فقطخواندنی از روی فضای ابری.
 *
 * قید کاربر: هیچ سؤال یا تصویری وارد باندل سایت یا APK نشود.
 * این پیمانه فقط یک شارد کوچک «فهرست» برای پایهٔ انتخابشده میگیرد؛
 * متن کامل سؤال و تصویرها تنها هنگام پیشنمایش یا افزودن خوانده میشوند.
 *
 * آدرس پایه در window.__REF_BANK_BASE__ است و با همان S3سازگار فعلی
 * (azmoon-media.s3.ir-thr-at1.arvanstorage.ir/reference-bank) تنظیم میشود.
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
  var cache = { catalog: {}, questions: {} };

  function url(path) { return BASE + '/' + path; }
  function getJSON(path) {
    return fetch(url(path), { cache: 'force-cache' }).then(function (r) {
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

  /* تبدیل سؤال مرجع به شیء قابل decodeQuestion در سازندهٔ آزمون. */
  function draft(p) {
    var q = {
      type: p.type || 'essay', text: p.text || '', grade: p.grade || 0,
      subject: p.subject || '', topic: p.topic || '', difficulty: p.difficulty || 0,
      score: 1, options: p.options || [], correctIndex: p.correct_index == null ? null : p.correct_index,
      expectedText: p.answer_text || '', images: (p.image_urls || []).slice(0, 8)
    };
    if (q.type === 'multiple' && (!q.options || q.options.length < 2)) q.type = 'essay';
    return q;
  }

  function open() {
    var bg = el('div', { class: 'modal-bg' });
    var card = el('div', { class: 'modal wide', style: 'max-width:960px;width:96vw;max-height:90vh;overflow:auto' });
    bg.appendChild(card); document.body.appendChild(bg);
    bg.addEventListener('click', function (e) { if (e.target === bg) bg.remove(); });
    var st = { grade: 1, track: '', subject: '', chapter: '', type: '', difficulty: '', query: '', page: 0, size: 25, rows: [], picked: {} };

    function head() {
      card.appendChild(el('div', { class: 'row', style: 'gap:8px;align-items:center;flex-wrap:wrap' }, [
        el('h2', { class: 'grow', text: 'بانک سؤال مرجع' }),
        el('button', { class: 'x', text: '✕', onclick: function () { bg.remove(); } })
      ]));
      card.appendChild(el('p', { class: 'muted', text: 'سؤالها و تصویرها در فضای ابریاند؛ فقط نتیجهٔ پالایششده دانلود میشود.' }));
    }

    function controls() {
      var gs = el('select', { title: 'پایه' });
      for (var g = 1; g <= 12; g++) gs.appendChild(el('option', { value: String(g), text: gradeLabel(g) }));
      gs.value = String(st.grade);
      var ts = el('select', { title: 'رشته' });
      TRACKS.forEach(function (t) { ts.appendChild(el('option', { value: t, text: t || 'همهٔ رشتهها' })); });
      var ss = el('select', { title: 'درس' }), cs = el('select', { title: 'فصل' }), tps = el('select', { title: 'نوع سؤال' }), ds = el('select', { title: 'دشواری' });
      var q = el('input', { type: 'search', placeholder: 'جستوجو در متن سؤال…', style: 'min-width:220px;flex:1' });
      var sz = el('select', { title: 'شمار سؤال در هر صفحه' });
      [25, 50, 100].forEach(function (n) { sz.appendChild(el('option', { value: String(n), text: fa(n) + ' سؤال در صفحه' })); });
      sz.value = String(st.size);

      function fill(sel, values, all, keep) {
        sel.innerHTML = '';
        sel.appendChild(el('option', { value: '', text: all }));
        values.forEach(function (v) { sel.appendChild(el('option', { value: v, text: v })); });
        if (keep && values.indexOf(keep) >= 0) sel.value = keep; else sel.value = '';
      }
      function refreshSelects() {
        var base = st.rows;
        fill(ss, uniq(base, 'subject'), 'همهٔ درسها', st.subject);
        var subRows = st.subject ? base.filter(function (r) { return r.subject === st.subject; }) : base;
        fill(cs, uniq(subRows, 'chapter'), 'همهٔ فصلها', st.chapter);
        fill(tps, uniq(base, 'type_fa'), 'همهٔ انواع', st.type);
        ds.innerHTML = '';
        ds.appendChild(el('option', { value: '', text: 'همهٔ سطحها' }));
        DIFF.forEach(function (lb, i) { ds.appendChild(el('option', { value: String(i), text: lb })); });
        ds.value = st.difficulty;
      }
      gs.addEventListener('change', function () { st.grade = Number(gs.value) || 1; st.page = 0; st.subject = st.chapter = st.type = ''; load(); });
      ts.addEventListener('change', function () { st.track = ts.value; st.page = 0; draw(); });
      ss.addEventListener('change', function () { st.subject = ss.value; st.chapter = ''; refreshSelects(); st.page = 0; draw(); });
      cs.addEventListener('change', function () { st.chapter = cs.value; st.page = 0; draw(); });
      tps.addEventListener('change', function () { st.type = tps.value; st.page = 0; draw(); });
      ds.addEventListener('change', function () { st.difficulty = ds.value; st.page = 0; draw(); });
      q.addEventListener('input', function () { st.query = q.value.trim(); st.page = 0; draw(); });
      sz.addEventListener('change', function () { st.size = Number(sz.value) || 25; st.page = 0; draw(); });
      card.appendChild(el('div', { class: 'row', style: 'gap:6px;flex-wrap:wrap;margin-bottom:8px' },
        [gs, ts, ss, cs, tps, ds, q, sz]));
      return refreshSelects;
    }

    var body = el('div'), foot = el('div', { class: 'row', style: 'gap:8px;margin-top:10px;align-items:center' });
    var count = el('span', { class: 'muted' });
    var addSel = el('button', { class: 'btn sm', text: 'افزودن انتخابشدهها' });
    var refreshSelects = null;

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
    function payloadFor(r) {
      return questions(st.grade).then(function (all) { return all[String(r.id)] || null; });
    }
    function addRows(rows) {
      if (!rows.length) { toast('سؤالی انتخاب نشده است.', 'err'); return; }
      questions(st.grade).then(function (all) {
        var out = rows.map(function (r) { return all[String(r.id)] ? draft(all[String(r.id)]) : null; }).filter(Boolean);
        if (!out.length) { toast('محتوای سؤالها در دسترس نیست.', 'err'); return; }
        S.go('builder', { refAdd: out });
        bg.remove();
        toast(fa(out.length) + ' سؤال به آزمون افزوده شد.', 'ok');
      }).catch(function (e) { toast('خواندن سؤالها ناموفق بود: ' + String(e.message || e), 'err'); });
    }
    addSel.addEventListener('click', function () {
      var f = filtered(), chosen = f.filter(function (r) { return st.picked[r.id]; });
      addRows(chosen);
    });

    function preview(r) {
      payloadFor(r).then(function (p) {
        if (!p) { toast('محتوای این سؤال یافت نشد.', 'err'); return; }
        var pbg = el('div', { class: 'modal-bg' });
        var pc = el('div', { class: 'modal wide', style: 'max-width:820px;width:94vw;max-height:88vh;overflow:auto' });
        pbg.appendChild(pc); document.body.appendChild(pbg);
        pbg.addEventListener('click', function (e) { if (e.target === pbg) pbg.remove(); });
        pc.appendChild(el('div', { class: 'row' }, [
          el('h3', { class: 'grow', text: 'پیشنمایش سؤال' }),
          el('button', { class: 'x', text: '✕', onclick: function () { pbg.remove(); } })
        ]));
        pc.appendChild(el('div', { class: 'muted', style: 'font-size:12px', text: [r.subject, r.chapter, r.type_fa, r.difficulty_fa].filter(Boolean).join(' · ') }));
        var qv = el('div', { style: 'margin:10px 0;line-height:1.9' }); qv.innerHTML = p.q_html || '';
        pc.appendChild(qv);
        if ((p.options || []).length) {
          pc.appendChild(el('div', { style: 'margin:8px 0', html: p.options.map(function (o, i) {
            return '<div>' + fa(i + 1) + ') ' + esc(o) + '</div>';
          }).join('') }));
        }
        if (p.answer_html || p.answer_text) {
          var det = el('details', {}, [el('summary', { text: 'پاسخ / راهنما' })]);
          var av = el('div', { style: 'margin-top:8px;line-height:1.9' }); av.innerHTML = p.answer_html || esc(p.answer_text || '');
          det.appendChild(av); pc.appendChild(det);
        }
        pc.appendChild(el('div', { class: 'row', style: 'margin-top:12px' }, [
          el('button', { class: 'btn', text: '➕ افزودن به آزمون', onclick: function () { pbg.remove(); addRows([r]); } }),
          el('button', { class: 'btn light', text: 'بستن', onclick: function () { pbg.remove(); } })
        ]));
      }).catch(function (e) { toast('پیشنمایش ناموفق بود: ' + String(e.message || e), 'err'); });
    }

    function draw() {
      var f = filtered();
      var pages = Math.max(1, Math.ceil(f.length / st.size));
      if (st.page >= pages) st.page = pages - 1;
      var slice = f.slice(st.page * st.size, (st.page + 1) * st.size);
      count.textContent = fa(f.length) + ' سؤال · صفحهٔ ' + fa(st.page + 1) + ' از ' + fa(pages);
      body.innerHTML = '';
      if (!slice.length) { body.appendChild(S.emptyBox('🏛', 'سؤالی با این فیلترها پیدا نشد.')); return; }
      slice.forEach(function (r) {
        var cb = el('input', { type: 'checkbox', title: 'انتخاب' });
        cb.checked = !!st.picked[r.id];
        cb.addEventListener('change', function () { if (cb.checked) st.picked[r.id] = 1; else delete st.picked[r.id]; });
        var imgs = (r.image_urls || []).slice(0, 1).map(function (u) {
          return el('img', { src: u, alt: '', loading: 'lazy', style: 'max-width:100%;max-height:120px;border-radius:8px' });
        });
        body.appendChild(el('div', { class: 'card', style: 'margin:8px 0;padding:10px' }, [
          el('div', { class: 'row', style: 'gap:8px;align-items:center' }, [
            cb,
            el('span', { class: 'muted', style: 'font-size:12px', text: [r.subject, r.chapter, r.type_fa, r.difficulty_fa].filter(Boolean).join(' · ') }),
            el('span', { class: 'grow' }),
            r.has_image ? el('span', { class: 'chip', text: '🖼 تصویر' }) : null
          ].filter(Boolean)),
          el('div', { style: 'margin:6px 0', text: String(r.text || '').slice(0, 220) }),
          imgs.length ? el('div', {}, imgs) : null,
          el('div', { class: 'row', style: 'gap:6px;margin-top:6px' }, [
            el('button', { class: 'btn light sm', text: '👁 پیشنمایش', onclick: function () { preview(r); } }),
            el('button', { class: 'btn sm', text: '➕ افزودن به آزمون', onclick: function () { addRows([r]); } })
          ])
        ]));
      });
      var prev = el('button', { class: 'btn light sm', text: 'قبلی', disabled: st.page === 0 ? 'disabled' : null });
      var next = el('button', { class: 'btn light sm', text: 'بعدی', disabled: st.page >= pages - 1 ? 'disabled' : null });
      prev.addEventListener('click', function () { st.page--; draw(); });
      next.addEventListener('click', function () { st.page++; draw(); });
      foot.innerHTML = '';
      foot.appendChild(prev); foot.appendChild(next); foot.appendChild(count);
      foot.appendChild(el('span', { class: 'grow' })); foot.appendChild(addSel);
    }

    function load() {
      S.loading(body);
      catalog(st.grade).then(function (rows) {
        st.rows = Array.isArray(rows) ? rows : [];
        st.picked = {};
        if (refreshSelects) refreshSelects();
        card.innerHTML = '';
        head();
        refreshSelects = controls();
        refreshSelects();
        card.appendChild(body); card.appendChild(foot);
        draw();
      }).catch(function (e) {
        body.innerHTML = '';
        body.appendChild(el('div', { class: 'alert warn', text: 'فهرست این پایه هنوز بارگذاری نشده یا قابل دسترسی نیست: ' + String(e.message || e) }));
      });
    }

    head();
    card.appendChild(body);
    load();
  }

  root.RefBank = { open: open, base: BASE, catalog: catalog, questions: questions, draft: draft };
})(window);
