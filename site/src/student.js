/* V140 — فاز ۳ سایت: پنل دانش‌آموز (شرکت در آزمون با کد + تختهٔ سفید)
   قراردادها عیناً از اپ: get_exam_for_student(p_code) → StudentExamPayloadCodec (پاک‌سازی کلید پاسخ،
   به‌هم‌ریختن پایدار StableExamShuffle با seed «student:exam:index»)، ارسال با native_submit_queued_answer_v1
   (p_operation, p_exam, p_responses به ترتیب اصلی سؤال‌ها، p_images، p_meta{native,queued,created_at_epoch_ms,monitor_report})،
   گزارش نظارتی با native_monitor_upsert_v1، تصاویر پاسخ در exam-images/answers/<student>/<exam>/<question>/. */
(function () {
  'use strict';
  var S = window.ExamSite;
  var el = S.el, esc = S.esc, fa = S.fa, en = S.en, toast = S.toast, errMsg = S.errMsg, uuid = S.uuid;
  var LS_ACTIVE = 'examsite.student.active.v1';   /* ActiveExamSessionEntity */
  var LS_DRAFT = 'examsite.student.draft.v1.';    /* + examId → {answers, images} */
  var BUCKET = 'exam-images';
  var WHITEBOARD_MAX_PAGES = 6;
  var ANSWER_FIELDS = ['correctOption', 'correctIndex', 'correctAnswer', 'accept', 'answer', 'tolerance', 'matchAnswer', 'explanation', 'answer_key'];

  /* ================================================================ StableExamShuffle (بیت‌به‌بیت مثل Kotlin) */
  function stableShuffle(values, seed) {
    if (values.length < 2) return values.slice();
    var r = values.slice(), st = 0;
    for (var i = 0; i < seed.length; i++) st = Number((BigInt(st) * 31n + BigInt(seed.charCodeAt(i))) & 0xffffffffn);
    for (var idx = r.length - 1; idx >= 1; idx--) {
      st = Number((BigInt(st) * 1664525n + 1013904223n) & 0xffffffffn);
      var rnd = st / 4294967296;
      var swap = Math.min(idx, Math.max(0, Math.floor(rnd * (idx + 1))));
      var o = r[idx]; r[idx] = r[swap]; r[swap] = o;
    }
    return r;
  }
  /* ================================================================ codec */
  function sanitize(raw) {
    var v = {}; Object.keys(raw).forEach(function (k) { if (ANSWER_FIELDS.indexOf(k) < 0) v[k] = raw[k]; });
    v.questions = (Array.isArray(raw.questions) ? raw.questions : []).map(function (q) { if (!q || typeof q !== 'object') return q; var o = {}; Object.keys(q).forEach(function (k) { if (ANSWER_FIELDS.indexOf(k) < 0) o[k] = q[k]; }); return o; });
    return v;
  }
  function strs(a) { return Array.isArray(a) ? a.filter(function (x) { return x != null; }).map(String) : []; }
  function nstrs(a) { return Array.isArray(a) ? a.map(function (x) { return x != null && String(x).trim() ? String(x) : null; }) : []; }
  function parseMs(v) { if (!v) return null; var t = Date.parse(v); return isNaN(t) ? null : t; }
  function decodeExam(raw, studentId, now, deadlineOverride) {
    raw = sanitize(raw);
    if (raw.error) throw new Error(String(raw.error));
    var examId = raw.id; if (!examId) throw new Error('شناسه آزمون در پاسخ سرور موجود نیست.');
    var shq = raw.shuffle_q === true, sho = raw.shuffle_opt === true;
    var rq = (raw.questions || []).filter(function (q) { return q && typeof q === 'object'; });
    var parsed = rq.map(function (obj, index) {
      var oi = obj.i != null ? Number(obj.i) : index;
      var seed = studentId + ':' + examId + ':' + index + ':options';
      var type = S.qType(obj.type); if (type === 'long') type = 'essay';
      var images = strs(obj.images); if (!images.length && obj.image) images = [String(obj.image)];
      var allow = obj.allowImages || obj.allow_images || null;
      var maxImg = (!allow || allow === 'no') ? 0 : Math.max(0, Math.min(10, Number(obj.maxImages != null ? obj.maxImages : (obj.max_images != null ? obj.max_images : 1))));
      var q = {id: obj.id || ('q-' + oi), type: type, text: obj.text || '', score: Number(obj.score) || 0, images: images, maxAnswerImages: maxImg, answerImagesRequired: allow === 'required', originalIndex: oi,
        pres: {align: ['right', 'center', 'left', 'justify'].indexOf(obj.align) >= 0 ? obj.align : 'right', font: obj.font || 'default', fontSize: Math.max(8, Math.min(40, Number(obj.fontSize) || 16)), bold: obj.bold === true, italic: obj.italic === true, allowAnswerGraph: obj.allowAnswerGraph === true, audio: obj.audio || null, spans: obj.spans || [], alignSpans: obj.alignSpans || []}};
      if (type === 'multiple') {
        var opts = strs(obj.options), oimg = nstrs(obj.optionImages);
        var idxs = opts.map(function (_, i) { return i; });
        var order = sho ? stableShuffle(idxs, seed + ':' + oi) : idxs;
        q.options = order.map(function (i) { return opts[i]; }); q.optionImages = order.map(function (i) { return oimg[i] || null; }); q.optionOriginalIndices = order;
      } else if (type === 'matching') {
        var right = strs(obj.rightItems), rimg = nstrs(obj.rightImages);
        var ridx = right.map(function (_, i) { return i; });
        var rorder = sho ? stableShuffle(ridx, seed + ':' + oi + ':matching') : ridx;
        q.leftItems = strs(obj.leftItems); q.leftImages = nstrs(obj.leftImages); q.rightItems = rorder.map(function (i) { return right[i]; }); q.rightImages = rorder.map(function (i) { return rimg[i] || null; }); q.rightOriginalIndices = rorder;
      }
      return q;
    });
    var ordered = shq ? stableShuffle(parsed, studentId + ':' + examId + ':questions') : parsed;
    var duration = Math.max(0, Math.min(1440, Number(raw.duration) || 0));
    var deadline;
    if (deadlineOverride !== undefined) deadline = deadlineOverride;
    else {
      var exp = parseMs(raw.expires_at);
      if (exp != null) { var sn = parseMs(raw.server_now); deadline = now + (sn != null ? exp - sn : exp - now); }
      else deadline = duration > 0 ? now + duration * 60000 : null;
    }
    return {id: examId, title: (raw.title || '').trim() || 'آزمون', code: raw.code || '', duration: duration, questions: ordered, subject: raw.subject || '', teacherMessage: (raw.teacher_message || '').trim() || null, deadline: deadline,
      attemptsAllowed: Math.max(1, Math.min(5, Number(raw.attempts_allowed) || 1)), attemptNumber: raw.attempt_no != null ? Number(raw.attempt_no) : (raw.attempt_number != null ? Number(raw.attempt_number) : null), attemptsRemaining: raw.attempts_remaining != null ? Number(raw.attempts_remaining) : null, raw: raw};
  }

  /* ================================================================ نشست فعال / پیش‌نویس (Room ↔ localStorage) */
  function activeGet() { try { var a = JSON.parse(localStorage.getItem(LS_ACTIVE) || 'null'); return a && a.owner === S.user().id ? a : null; } catch (e) { return null; } }
  function activeSet(a) { if (a) localStorage.setItem(LS_ACTIVE, JSON.stringify(a)); else localStorage.removeItem(LS_ACTIVE); }
  function draftGet(examId) { try { return JSON.parse(localStorage.getItem(LS_DRAFT + examId) || 'null') || {answers: {}, images: {}}; } catch (e) { return {answers: {}, images: {}}; } }
  function draftSet(examId, d) { try { localStorage.setItem(LS_DRAFT + examId, JSON.stringify(d)); } catch (e) {} }
  function draftClear(examId) { localStorage.removeItem(LS_DRAFT + examId); }

  async function joinByCode(code) {
    var raw = await S.rpcObj('get_exam_for_student', {p_code: code.trim()});
    if (raw && raw.error) throw new Error(String(raw.error));
    var now = Date.now(), safe = sanitize(raw);
    var exam = decodeExam(safe, S.user().id, now);
    activeSet({owner: S.user().id, examId: exam.id, code: exam.code, payload: safe, deadline: exam.deadline, savedAt: now});
    return exam;
  }
  function restoreActive() {
    var a = activeGet(); if (!a) return null;
    try { var exam = decodeExam(a.payload, S.user().id, Date.now(), a.deadline); if (exam.id !== a.examId) return null; return exam; } catch (e) { return null; }
  }

  /* ================================================================ رندر متن سؤال (فرمول $…$ + شکل %%FIG%%) با موتور چاپ داخل iframe سبک */
  var mathFrame = null, mathReady = null;
  function ensureMathFrame() {
    if (mathReady) return mathReady;
    mathReady = new Promise(function (resolve) {
      var f = el('iframe', {style: 'position:fixed;width:0;height:0;border:0;opacity:0;pointer-events:none', title: 'math-renderer'});
      document.body.appendChild(f); mathFrame = f;
      f.addEventListener('load', function () {
        var w = f.contentWindow, tries = 0;
        (function go() { tries++; if (w.renderRichText && w.GeoFig) return resolve(w); if (tries < 200) setTimeout(go, 50); else resolve(null); })();
      });
      f.srcdoc = S.engineHtml('print');
    });
    return mathReady;
  }
  async function richHtml(text) {
    var w = await ensureMathFrame();
    if (!w) return esc(text).replace(/\n/g, '<br>');
    try { return w.renderRichText(String(text || ''), null); } catch (e) { return esc(text).replace(/\n/g, '<br>'); }
  }
  function mathCss() {
    /* فقط قواعد ریاضی/شکل موتور چاپ، محدود به ناحیهٔ سؤال (تا با استایل سایت تداخل نکند) */
    if (document.getElementById('stMathCss')) return;
    ensureMathFrame().then(function (w) {
      if (!w) return; var out = [];
      var keep = /\.(mathx|mfrac|mnum|mden|msqrt|mroot|msup|msub|mrow|mtable|mtr|mtd|mover|munder|mo|mi|mn|math-[a-z-]+|qmf-fig|fig-[a-z-]+|interactive-figure|vt-[a-z-]+|tf-[a-z-]+|gf-svg|pt-[a-z-]+)\b/;
      Array.prototype.forEach.call(w.document.styleSheets, function (sh) {
        var rules; try { rules = sh.cssRules; } catch (e) { return; }
        Array.prototype.forEach.call(rules, function (r) {
          if (r.type === 1 && r.selectorText && keep.test(r.selectorText) && !/^(html|body|\*)/.test(r.selectorText)) out.push(r.selectorText.split(',').map(function (x) { return '.st-exam ' + x.trim(); }).join(',') + '{' + r.style.cssText + '}');
          else if (r.type === 5 && /math|mfrac|frac/i.test(r.cssText)) out.push(r.cssText);
        });
      });
      var st = document.createElement('style'); st.id = 'stMathCss'; st.textContent = out.join('\n'); document.head.appendChild(st);
    });
  }

  /* ================================================================ صفحهٔ «شرکت در آزمون» */
  var run = null; /* وضعیت آزمون جاری */
  async function page(c, arg) {
    c.innerHTML = '';
    if (run && !run.finished) { c.appendChild(examUI(c)); return; }
    var pending = restoreActive();
    var card = el('div', {class: 'card', style: 'max-width:520px;margin:0 auto'});
    card.appendChild(el('h3', {text: '🔑 شرکت در آزمون'}));
    if (pending) {
      card.appendChild(el('div', {class: 'alert warn'}, [el('b', {text: 'آزمون نیمه‌تمام دارید: '}), el('span', {text: pending.title + ' (' + pending.code + ')'}),
        el('div', {style: 'margin-top:8px'}, [el('button', {class: 'btn sm', text: 'پیوستن به آزمون', onclick: function () { startExam(c, pending, true); }}), ' ', el('button', {class: 'btn light sm', text: 'انصراف از این آزمون', onclick: function () { activeSet(null); page(c); }})])]));
    }
    var inp = el('input', {type: 'text', placeholder: 'کد ۶ حرفی آزمون', maxlength: 8, style: 'text-align:center;letter-spacing:4px;font-size:22px;direction:ltr;text-transform:uppercase'});
    var msg = el('div');
    var btn = el('button', {class: 'btn', style: 'width:100%', text: 'ورود به آزمون', onclick: async function () {
      var code = en(inp.value).trim().toUpperCase(); if (code.length < 4) return toast('کد آزمون را وارد کنید.', 'err');
      btn.disabled = true; msg.innerHTML = '<div class="loading"><span class="spinner"></span> در حال دریافت آزمون…</div>';
      try { var exam = await joinByCode(code); msg.innerHTML = ''; startExam(c, exam, false); } catch (e) { msg.innerHTML = ''; msg.appendChild(el('div', {class: 'alert error', text: errMsg(e)})); btn.disabled = false; }
    }});
    inp.addEventListener('keydown', function (e) { if (e.key === 'Enter') btn.click(); });
    card.appendChild(el('div', {class: 'field'}, [el('label', {text: 'کد آزمون'}), inp]));
    card.appendChild(btn); card.appendChild(msg);
    card.appendChild(el('p', {class: 'muted', style: 'font-size:13px;margin-top:14px', text: 'کد را از معلم بگیرید. پس از ورود، زمان‌سنج شروع می‌شود و پاسخ‌ها به‌صورت خودکار روی همین مرورگر ذخیره می‌شوند.'}));
    c.appendChild(card);
  }

  /* ================================================================ اجرای آزمون */
  function startExam(c, exam, resumed) {
    var d = draftGet(exam.id);
    run = {exam: exam, index: 0, answers: d.answers || {}, images: d.images || {}, started: true, finished: false, submitting: false, enteredAt: Date.now(), qEnter: {}, qTime: {}, qVisits: {}, events: {}, timer: null, monitorTimer: null};
    run.qEnter[0] = Date.now(); run.qVisits[exam.questions[0] && exam.questions[0].id] = 1;
    document.addEventListener('visibilitychange', onVis);
    window.addEventListener('blur', onBlur);
    run.monitorTimer = setInterval(function () { reportMonitor(false); }, 60000);
    reportMonitor(false);
    /* V149 — در پوستهٔ موبایل، آزمون تمام‌صفحه می‌شود (نوار بالا حذف؛ مثل StudentExamScreen اپ) */
    if (window.SiteMobile && window.SiteMobile.active()) { window.SiteMobile.paint(); c = document.getElementById('content'); }
    c.innerHTML = ''; c.appendChild(examUI(c));
    if (resumed) toast('به آزمون نیمه‌تمام پیوستید.', 'ok');
  }
  function onVis() { if (document.hidden) secEvent('app_leave'); }
  function onBlur() { secEvent('window_blur'); }
  function secEvent(k) { if (!run || run.finished || !run.started) return; run.events[k] = (run.events[k] || 0) + 1; reportMonitor(false); }
  function monitorReport(final) {
    var now = Date.now(), times = Object.assign({}, run.qTime);
    var q = run.exam.questions[run.index]; var ent = run.qEnter[run.index];
    if (q && ent != null) times[q.id] = (times[q.id] || 0) + (now - ent);
    var labels = {}; run.exam.questions.forEach(function (x) { labels[x.id] = x.originalIndex + 1; });
    return {entered_at_epoch_ms: run.enteredAt, left_at_epoch_ms: now, submitted: !!final, events: run.events, question_time_ms: times, question_visits: run.qVisits, question_labels: labels};
  }
  var lastReport = 0;
  function reportMonitor(final) {
    if (!run) return Promise.resolve();
    if (!final && Date.now() - lastReport < 5000) return Promise.resolve();
    lastReport = Date.now();
    return S.rpcObj('native_monitor_upsert_v1', {p_exam: run.exam.id, p_report: monitorReport(final)}).catch(function () {});
  }
  function goTo(i) {
    var now = Date.now(), q = run.exam.questions[run.index], ent = run.qEnter[run.index];
    if (q && ent != null) { run.qTime[q.id] = (run.qTime[q.id] || 0) + (now - ent); }
    run.index = Math.max(0, Math.min(run.exam.questions.length - 1, i));
    run.qEnter[run.index] = now;
    var nq = run.exam.questions[run.index]; if (nq) run.qVisits[nq.id] = (run.qVisits[nq.id] || 0) + 1;
  }
  function saveDraft() { draftSet(run.exam.id, {answers: run.answers, images: run.images}); }
  function stopAll() { clearInterval(run.timer); clearInterval(run.monitorTimer); document.removeEventListener('visibilitychange', onVis); window.removeEventListener('blur', onBlur); }

  function examUI(c) {
    var ex = run.exam;
    var wrap = el('div', {class: 'st-exam'});
    var timerEl = el('span', {class: 'st-timer'});
    var head = el('div', {class: 'card st-head'}, [
      el('div', {}, [el('b', {text: ex.title}), el('div', {class: 'muted', style: 'font-size:12px', text: [ex.subject, 'کد ' + ex.code, ex.attemptNumber ? 'تلاش ' + fa(ex.attemptNumber) + ' از ' + fa(ex.attemptsAllowed) : ''].filter(Boolean).join(' · ')})]),
      el('span', {class: 'grow'}), timerEl,
      el('button', {class: 'btn sm', text: '✅ ارسال پاسخ‌ها', onclick: function () { review(c); }})
    ]);
    wrap.appendChild(head);
    if (ex.teacherMessage) wrap.appendChild(el('div', {class: 'alert info', text: '💬 ' + ex.teacherMessage}));
    var nav = el('div', {class: 'st-nav'});
    var body = el('div', {class: 'card st-body'});
    var foot = el('div', {class: 'row', style: 'margin-top:12px'});
    wrap.appendChild(nav); wrap.appendChild(body); wrap.appendChild(foot);
    function answered(q) { var a = run.answers[q.id]; return (a != null && a !== '' && !(typeof a === 'object' && !Object.keys(a).length)) || (run.images[q.id] || []).length > 0; }
    function drawNav() {
      nav.innerHTML = '';
      ex.questions.forEach(function (q, i) { nav.appendChild(el('button', {class: 'st-dot' + (i === run.index ? ' on' : '') + (answered(q) ? ' done' : ''), text: fa(i + 1), onclick: function () { goTo(i); drawAll(); }})); });
    }
    function drawFoot() {
      foot.innerHTML = '';
      foot.appendChild(el('button', {class: 'btn light', text: '→ قبلی', disabled: run.index === 0 ? 'disabled' : null, onclick: function () { goTo(run.index - 1); drawAll(); }}));
      foot.appendChild(el('span', {class: 'grow muted', style: 'text-align:center;font-size:13px', text: 'سؤال ' + fa(run.index + 1) + ' از ' + fa(ex.questions.length)}));
      if (run.index < ex.questions.length - 1) foot.appendChild(el('button', {class: 'btn light', text: 'بعدی ←', onclick: function () { goTo(run.index + 1); drawAll(); }}));
      else foot.appendChild(el('button', {class: 'btn', text: '✅ مرور و ارسال', onclick: function () { review(c); }}));
    }
    async function drawBody() {
      var q = ex.questions[run.index]; body.innerHTML = '';
      if (!q) return;
      var p = q.pres;
      var txt = el('div', {class: 'st-qtext', style: 'text-align:' + p.align + ';font-size:' + p.fontSize + 'px;' + (p.bold ? 'font-weight:700;' : '') + (p.italic ? 'font-style:italic;' : '') + (p.font && p.font !== 'default' ? 'font-family:' + p.font + ',Vazirmatn;' : ''), html: esc(q.text).replace(/\n/g, '<br>')});
      body.appendChild(el('div', {class: 'row', style: 'margin-bottom:10px'}, [el('span', {class: 'chip brand', text: 'سؤال ' + fa(run.index + 1)}), el('span', {class: 'chip', text: 'بارم ' + fa(S.fmtScore(q.score))}), el('span', {class: 'chip', text: {multiple: 'چندگزینه‌ای', truefalse: 'صحیح/غلط', fill: 'جای‌خالی', numeric: 'عددی', matching: 'جورکردنی', essay: 'تشریحی'}[q.type] || ''})]));
      body.appendChild(txt);
      richHtml(q.text).then(function (h) { if (ex.questions[run.index] === q) txt.innerHTML = h; });
      if (p.audio) body.appendChild(el('audio', {controls: 'controls', src: p.audio, style: 'width:100%;margin:8px 0'}));
      if (q.images.length) body.appendChild(el('div', {class: 'st-imgs'}, q.images.map(function (u) { return el('img', {src: u, onclick: function () { lightbox(u); }}); })));
      body.appendChild(answerArea(q));
    }
    function answerArea(q) {
      var box = el('div', {class: 'st-answer'});
      var a = run.answers[q.id];
      function set(v) { if (v === null || v === undefined || v === '') delete run.answers[q.id]; else run.answers[q.id] = v; saveDraft(); drawNav(); }
      if (q.type === 'multiple') {
        q.options.forEach(function (o, di) {
          var oi = q.optionOriginalIndices[di] != null ? q.optionOriginalIndices[di] : di;
          var r = el('input', {type: 'radio', name: 'st_' + q.id}); r.checked = a === oi; r.addEventListener('change', function () { set(oi); });
          var lab = el('label', {class: 'st-opt' + (a === oi ? ' on' : '')}, [r, el('span', {class: 'st-optlabel', text: 'ابجد'.split('')[di] ? ['الف', 'ب', 'ج', 'د', 'ه', 'و', 'ز', 'ح'][di] + ')' : fa(di + 1) + ')'}), el('span', {class: 'grow', html: esc(o)}), q.optionImages[di] ? el('img', {src: q.optionImages[di], class: 'thumb', onclick: function (e) { e.preventDefault(); lightbox(q.optionImages[di]); }}) : null]);
          lab.addEventListener('click', function () { setTimeout(function () { Array.prototype.forEach.call(box.querySelectorAll('.st-opt'), function (x) { x.classList.toggle('on', x.querySelector('input').checked); }); }, 0); });
          richHtml(o).then(function (h) { lab.querySelector('.grow').innerHTML = h; });
          box.appendChild(lab);
        });
      } else if (q.type === 'truefalse') {
        box.appendChild(el('div', {class: 'row'}, [[true, '✓ صحیح'], [false, '✗ غلط']].map(function (o) { return el('button', {class: 'btn ' + (a === o[0] ? '' : 'light'), text: o[1], onclick: function () { set(o[0]); box.replaceWith(answerArea(q)); }}); })));
      } else if (q.type === 'fill' || q.type === 'numeric') {
        var i = el('input', {type: 'text', class: 'st-input', placeholder: q.type === 'numeric' ? 'پاسخ عددی' : 'پاسخ جای خالی', value: a != null ? String(a) : '', style: q.type === 'numeric' ? 'direction:ltr;text-align:left' : ''});
        i.addEventListener('input', function () { var v = i.value; if (q.type === 'numeric') { v = en(v).replace(/[^0-9.\-]/g, ''); i.value = v; } set(v); });
        box.appendChild(i);
      } else if (q.type === 'matching') {
        var cur = (a && typeof a === 'object') ? a : {};
        box.appendChild(el('div', {class: 'st-match-right'}, q.rightItems.map(function (r, di) { var d = el('div', {class: 'st-mr'}, [el('b', {text: fa(di + 1) + '. '}), el('span', {html: esc(r)}), q.rightImages[di] ? el('img', {src: q.rightImages[di], class: 'thumb'}) : null]); richHtml(r).then(function (h) { d.querySelector('span').innerHTML = h; }); return d; })));
        q.leftItems.forEach(function (l, li) {
          var row = el('div', {class: 'st-ml'}, [el('span', {class: 'grow', html: '<b>' + fa(li + 1) + '.</b> ' + esc(l)}), q.leftImages[li] ? el('img', {src: q.leftImages[li], class: 'thumb'}) : null,
            el('div', {class: 'st-chips'}, q.rightItems.map(function (_, di) { var oi = q.rightOriginalIndices[di] != null ? q.rightOriginalIndices[di] : di; return el('button', {class: 'chip' + (cur[li] === oi ? ' brand' : ''), text: fa(di + 1), onclick: function () { cur = Object.assign({}, cur); cur[li] = oi; set(cur); box.replaceWith(answerArea(q)); }}); }))]);
          richHtml(l).then(function (h) { row.querySelector('.grow').innerHTML = '<b>' + fa(li + 1) + '.</b> ' + h; });
          box.appendChild(row);
        });
      } else {
        var ta = el('textarea', {class: 'st-input', rows: 6, placeholder: 'پاسخ تشریحی خود را بنویسید…'}); ta.value = a != null ? String(a) : '';
        ta.addEventListener('input', function () { set(ta.value); });
        box.appendChild(ta);
      }
      /* تصویر پاسخ / تخته */
      var whiteboardOnly = q.pres.allowAnswerGraph && q.maxAnswerImages <= 0;
      if (q.maxAnswerImages > 0 || q.pres.allowAnswerGraph) {
        var imgs = run.images[q.id] || [];
        var sec = el('div', {class: 'st-imgsec'});
        sec.appendChild(el('div', {class: 'row'}, [el('b', {style: 'font-size:13px', text: whiteboardOnly ? '🖍 تختهٔ سفید (پاسخ ترسیمی)' : ('📷 تصویر پاسخ' + (q.answerImagesRequired ? ' (اجباری)' : ' (اختیاری)') + ' — تا ' + fa(q.maxAnswerImages) + ' تصویر')}), el('span', {class: 'grow'}),
          q.maxAnswerImages > 0 && imgs.length < q.maxAnswerImages ? el('button', {class: 'btn light sm', text: '📁 انتخاب تصویر', onclick: async function () { var f = await pickFile(); if (!f) return; try { var du = await fileToDataUrl(await downscale(f, 2200)); addImages(q, [du], false); box.replaceWith(answerArea(q)); } catch (e) { toast(errMsg(e), 'err'); } }}) : null,
          el('button', {class: 'btn soft sm', text: '🖍 تخته', onclick: function () { openWhiteboard(q, function (pages) { addImages(q, pages, true); box.replaceWith(answerArea(q)); }); }})]));
        if (imgs.length) sec.appendChild(el('div', {class: 'st-imgs'}, imgs.map(function (u) { return el('div', {class: 'b-img'}, [el('img', {src: u, onclick: function () { lightbox(u); }}), el('button', {class: 'x', text: '✕', onclick: function () { run.images[q.id] = (run.images[q.id] || []).filter(function (x) { return x !== u; }); saveDraft(); box.replaceWith(answerArea(q)); }})]); })));
        box.appendChild(sec);
      }
      return box;
    }
    function addImages(q, uris, fromBoard) {
      var whiteboardOnly = (fromBoard || q.pres.allowAnswerGraph) && q.maxAnswerImages <= 0;
      var max = whiteboardOnly ? WHITEBOARD_MAX_PAGES : q.maxAnswerImages; if (max <= 0) return;
      var cur = whiteboardOnly ? [] : (run.images[q.id] || []);
      run.images[q.id] = cur.concat(uris.slice(0, Math.max(0, max - cur.length)));
      saveDraft(); drawNav();
    }
    function drawAll() { drawNav(); drawBody(); drawFoot(); }
    function tick() {
      if (!run || run.finished) return;
      if (ex.deadline == null) { timerEl.textContent = '⏱ بدون محدودیت'; return; }
      var rem = Math.max(0, ex.deadline - Date.now()); var m = Math.floor(rem / 60000), s = Math.floor(rem % 60000 / 1000);
      timerEl.textContent = '⏱ ' + fa((m < 10 ? '0' : '') + m + ':' + (s < 10 ? '0' : '') + s);
      timerEl.classList.toggle('danger', rem < 120000);
      if (rem <= 0) { clearInterval(run.timer); toast('زمان آزمون تمام شد؛ پاسخ‌ها ارسال می‌شود.', 'err'); submit(c, true); }
    }
    run.timer = setInterval(tick, 1000); tick();
    mathCss(); drawAll();
    return wrap;
  }
  function lightbox(u) { var bg = el('div', {class: 'modal-bg', onclick: function () { bg.remove(); }}, [el('img', {src: u, style: 'max-width:92vw;max-height:90vh;border-radius:12px;background:#fff'})]); document.body.appendChild(bg); }

  /* ================================================================ مرور و ارسال */
  function review(c) {
    var ex = run.exam; var un = ex.questions.filter(function (q) { var a = run.answers[q.id]; return (a == null || a === '' || (typeof a === 'object' && !Object.keys(a).length)) && !(run.images[q.id] || []).length; });
    var missingImg = ex.questions.findIndex(function (q) { return q.answerImagesRequired && !(run.images[q.id] || []).length; });
    if (missingImg >= 0) { goTo(missingImg); c.innerHTML = ''; c.appendChild(examUI(c)); return toast('ارسال تصویر پاسخ برای این سؤال اجباری است.', 'err'); }
    var bg = el('div', {class: 'modal-bg'});
    var m = el('div', {class: 'modal'}, [el('h2', {text: 'ارسال پاسخ‌ها'}),
      el('p', {text: 'از ' + fa(ex.questions.length) + ' سؤال، ' + fa(ex.questions.length - un.length) + ' سؤال پاسخ داده شده است.'}),
      un.length ? el('div', {class: 'alert warn', text: 'بدون پاسخ: ' + un.map(function (q) { return fa(ex.questions.indexOf(q) + 1); }).join('، ')}) : el('div', {class: 'alert ok', text: 'همهٔ سؤال‌ها پاسخ دارند.'}),
      el('p', {class: 'muted', style: 'font-size:13px', text: 'پس از ارسال امکان تغییر پاسخ‌ها نیست.'}),
      el('div', {class: 'row'}, [el('button', {class: 'btn', text: 'ارسال نهایی', onclick: function () { bg.remove(); submit(c, false); }}), el('button', {class: 'btn light', text: 'بازگشت', onclick: function () { bg.remove(); }})])]);
    bg.appendChild(m); document.body.appendChild(bg);
  }
  async function uploadAnswerImage(dataUrl, examId, questionId) {
    if (/^https:\/\//i.test(dataUrl)) return dataUrl;
    var blob = await (await fetch(dataUrl)).blob();
    var path = 'answers/' + S.user().id + '/' + examId + '/' + questionId + '/' + uuid() + '.webp';
    var sess = S.session();
    var res = await fetch(S.config.url + '/storage/v1/object/' + BUCKET + '/' + path, {method: 'POST', headers: {'apikey': S.config.anon, 'Authorization': 'Bearer ' + (sess ? sess.access_token : S.config.anon), 'Content-Type': blob.type || 'image/webp', 'x-upsert': 'false'}, body: blob});
    if (!res.ok) throw new Error('آپلود تصویر پاسخ ناموفق بود: ' + (await res.text()).slice(0, 120));
    return S.config.url + '/storage/v1/object/public/' + BUCKET + '/' + path;
  }
  async function submit(c, auto) {
    if (!run || run.submitting || run.finished) return;
    run.submitting = true; clearInterval(run.timer);
    var ex = run.exam;
    var ov = el('div', {class: 'modal-bg'}, [el('div', {class: 'modal', style: 'text-align:center'}, [el('div', {class: 'loading'}, [el('span', {class: 'spinner'}), ' در حال ارسال پاسخ‌ها…'])])]);
    document.body.appendChild(ov);
    try {
      /* ترتیب اصلی سؤال‌ها (PendingSubmissionCodec) */
      var oi = ex.questions.map(function (q) { return q.originalIndex; });
      var canonical = (oi.every(function (x) { return x >= 0; }) && new Set(oi).size === ex.questions.length) ? ex.questions.slice().sort(function (a, b) { return a.originalIndex - b.originalIndex; }) : ex.questions;
      var responses = canonical.map(function (q) { var a = run.answers[q.id]; if (a == null) return ''; if (typeof a === 'object') { var o = {}; Object.keys(a).forEach(function (k) { o[String(k)] = a[k]; }); return o; } return a; });
      var images = {};
      for (var i = 0; i < ex.questions.length; i++) { var q = ex.questions[i]; var list = run.images[q.id] || []; if (!list.length) continue; images[q.id] = []; for (var k = 0; k < list.length; k++) images[q.id].push(await uploadAnswerImage(list[k], ex.id, q.id)); }
      var report = monitorReport(true);
      await S.rpcObj('native_monitor_upsert_v1', {p_exam: ex.id, p_report: report}).catch(function () {});
      var raw = await S.rpcObj('native_submit_queued_answer_v1', {p_operation: uuid(), p_exam: ex.id, p_responses: responses, p_images: images, p_meta: {native: true, queued: true, created_at_epoch_ms: Date.now(), monitor_report: report, web: true}});
      if (raw && raw.error) throw new Error(String(raw.error));
      run.finished = true; stopAll(); activeSet(null); draftClear(ex.id);
      ov.remove();
      c.innerHTML = '';
      c.appendChild(el('div', {class: 'card', style: 'max-width:520px;margin:0 auto;text-align:center'}, [el('div', {style: 'font-size:48px', text: '✅'}), el('h3', {text: 'پاسخ‌های شما ثبت شد'}), raw && raw.receipt ? el('p', {class: 'muted', text: 'کد رهگیری: ' + raw.receipt}) : null,
        auto ? el('p', {class: 'muted', text: 'زمان آزمون به پایان رسید و پاسخ‌ها به‌صورت خودکار ارسال شد.'}) : null,
        el('div', {class: 'row', style: 'justify-content:center;margin-top:12px'}, [el('button', {class: 'btn', text: 'کارنامه', onclick: function () { run = null; S.go('grades'); }}), el('button', {class: 'btn light', text: 'آزمون دیگر', onclick: function () { run = null; S.go('join'); }})])]));
    } catch (e) {
      ov.remove(); run.submitting = false;
      toast(errMsg(e), 'err');
      c.innerHTML = ''; c.appendChild(examUI(c));
      c.insertBefore(el('div', {class: 'alert error', text: 'ارسال ناموفق بود: ' + errMsg(e) + ' — پاسخ‌ها روی مرورگر ذخیره‌اند؛ دوباره تلاش کنید.'}), c.firstChild);
    }
  }

  /* ================================================================ تختهٔ سفید (رسم آزاد → تصویر پاسخ؛ چند صفحه؛ زمینه: خالی/شطرنجی/محور) */
  function openWhiteboard(q, onDone) {
    var W = 1400, H = 1000;
    var bg = el('div', {class: 'engine-bg wb'});
    var pages = [], cur = 0, tool = 'pen', color = '#1a237e', size = 3, grid = 'blank', undo = [], drawing = false, last = null, startPt = null, snapshotImg = null;
    var canvas = el('canvas', {width: W, height: H, class: 'wb-canvas'}); var ctx = canvas.getContext('2d');
    var status = el('span', {class: 'muted', style: 'font-size:12px'});
    function newPage() { return {strokes: null, data: null}; }
    function blank() { ctx.fillStyle = '#fff'; ctx.fillRect(0, 0, W, H); drawGrid(); }
    function drawGrid() {
      ctx.save(); ctx.strokeStyle = '#d7dce5'; ctx.lineWidth = 1;
      if (grid === 'grid' || grid === 'quad' || grid === 'first') { for (var x = 0; x <= W; x += 40) { ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, H); ctx.stroke(); } for (var y = 0; y <= H; y += 40) { ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(W, y); ctx.stroke(); } }
      ctx.strokeStyle = '#333'; ctx.lineWidth = 2; ctx.fillStyle = '#333'; ctx.font = '18px Vazirmatn, sans-serif';
      function arrow(x1, y1, x2, y2) { ctx.beginPath(); ctx.moveTo(x1, y1); ctx.lineTo(x2, y2); ctx.stroke(); var a = Math.atan2(y2 - y1, x2 - x1); ctx.beginPath(); ctx.moveTo(x2, y2); ctx.lineTo(x2 - 12 * Math.cos(a - 0.4), y2 - 12 * Math.sin(a - 0.4)); ctx.lineTo(x2 - 12 * Math.cos(a + 0.4), y2 - 12 * Math.sin(a + 0.4)); ctx.closePath(); ctx.fill(); }
      if (grid === 'quad') { var cx = W / 2, cy = H / 2; arrow(20, cy, W - 20, cy); arrow(cx, H - 20, cx, 20); ctx.fillText('x', W - 30, cy - 10); ctx.fillText('y', cx + 10, 30); for (var i = -15; i <= 15; i++) { if (!i) continue; var px = cx + i * 40, py = cy - i * 40; if (px > 0 && px < W) { ctx.beginPath(); ctx.moveTo(px, cy - 5); ctx.lineTo(px, cy + 5); ctx.stroke(); if (i % 2 === 0) ctx.fillText(fa(i), px - 8, cy + 24); } if (py > 0 && py < H) { ctx.beginPath(); ctx.moveTo(cx - 5, py); ctx.lineTo(cx + 5, py); ctx.stroke(); if (i % 2 === 0) ctx.fillText(fa(i), cx + 10, py + 6); } } }
      if (grid === 'first') { var ox = 80, oy = H - 80; arrow(ox, oy, W - 20, oy); arrow(ox, oy, ox, 20); ctx.fillText('x', W - 30, oy - 10); ctx.fillText('y', ox + 10, 30); for (var j = 1; j <= 30; j++) { var qx = ox + j * 40, qy = oy - j * 40; if (qx < W - 30) { ctx.beginPath(); ctx.moveTo(qx, oy - 5); ctx.lineTo(qx, oy + 5); ctx.stroke(); if (j % 2 === 0) ctx.fillText(fa(j), qx - 8, oy + 24); } if (qy > 30) { ctx.beginPath(); ctx.moveTo(ox - 5, qy); ctx.lineTo(ox + 5, qy); ctx.stroke(); if (j % 2 === 0) ctx.fillText(fa(j), ox - 34, qy + 6); } } }
      if (grid === 'line') { var ly = H / 2; arrow(20, ly, W - 20, ly); for (var n = -16; n <= 16; n++) { var nx = W / 2 + n * 40; if (nx > 30 && nx < W - 30) { ctx.beginPath(); ctx.moveTo(nx, ly - 8); ctx.lineTo(nx, ly + 8); ctx.stroke(); ctx.fillText(fa(n), nx - 6, ly + 30); } } }
      if (grid === 'polar') { var pcx = W / 2, pcy = H / 2; ctx.strokeStyle = '#c5cad6'; for (var r = 60; r < 520; r += 60) { ctx.beginPath(); ctx.arc(pcx, pcy, r, 0, Math.PI * 2); ctx.stroke(); } for (var d = 0; d < 360; d += 30) { var rad = d * Math.PI / 180; ctx.beginPath(); ctx.moveTo(pcx, pcy); ctx.lineTo(pcx + 480 * Math.cos(rad), pcy - 480 * Math.sin(rad)); ctx.stroke(); ctx.fillText(fa(d) + '°', pcx + 500 * Math.cos(rad) - 12, pcy - 500 * Math.sin(rad) + 6); } ctx.strokeStyle = '#333'; arrow(pcx - 500, pcy, pcx + 500, pcy); arrow(pcx, pcy + 500, pcx, pcy - 500); }
      if (grid === 'lined') { ctx.strokeStyle = '#cfd6e4'; for (var yy = 60; yy < H; yy += 50) { ctx.beginPath(); ctx.moveTo(40, yy); ctx.lineTo(W - 40, yy); ctx.stroke(); } }
      ctx.restore();
    }
    function push() { undo.push(canvas.toDataURL('image/png')); if (undo.length > 30) undo.shift(); }
    function pos(e) { var r = canvas.getBoundingClientRect(); var t = e.touches ? e.touches[0] : e; return {x: (t.clientX - r.left) * W / r.width, y: (t.clientY - r.top) * H / r.height}; }
    function down(e) { e.preventDefault(); drawing = true; last = startPt = pos(e); push(); if (tool !== 'pen' && tool !== 'eraser') { snapshotImg = new Image(); snapshotImg.src = canvas.toDataURL(); } if (tool === 'text') { drawing = false; var t = prompt('متن:'); if (t) { ctx.fillStyle = color; ctx.font = (size * 6 + 10) + 'px Vazirmatn, sans-serif'; ctx.direction = 'rtl'; ctx.fillText(t, last.x, last.y); } } }
    function move(e) {
      if (!drawing) return; e.preventDefault(); var p = pos(e);
      if (tool === 'pen' || tool === 'eraser') { ctx.save(); ctx.lineCap = 'round'; ctx.lineJoin = 'round'; ctx.strokeStyle = tool === 'eraser' ? '#fff' : color; ctx.lineWidth = tool === 'eraser' ? size * 8 : size; ctx.beginPath(); ctx.moveTo(last.x, last.y); ctx.lineTo(p.x, p.y); ctx.stroke(); ctx.restore(); last = p; }
      else { if (snapshotImg && snapshotImg.complete) ctx.drawImage(snapshotImg, 0, 0); shape(startPt, p); }
    }
    function shape(a, b) {
      ctx.save(); ctx.strokeStyle = color; ctx.lineWidth = size; ctx.beginPath();
      if (tool === 'line') { ctx.moveTo(a.x, a.y); ctx.lineTo(b.x, b.y); }
      if (tool === 'rect') ctx.rect(a.x, a.y, b.x - a.x, b.y - a.y);
      if (tool === 'circle') { var r = Math.hypot(b.x - a.x, b.y - a.y); ctx.arc(a.x, a.y, r, 0, Math.PI * 2); }
      if (tool === 'arrow') { ctx.moveTo(a.x, a.y); ctx.lineTo(b.x, b.y); var an = Math.atan2(b.y - a.y, b.x - a.x); ctx.moveTo(b.x, b.y); ctx.lineTo(b.x - 14 * Math.cos(an - 0.45), b.y - 14 * Math.sin(an - 0.45)); ctx.moveTo(b.x, b.y); ctx.lineTo(b.x - 14 * Math.cos(an + 0.45), b.y - 14 * Math.sin(an + 0.45)); }
      ctx.stroke(); ctx.restore();
    }
    function up(e) { if (!drawing) return; drawing = false; snapshotImg = null; }
    canvas.addEventListener('pointerdown', down); canvas.addEventListener('pointermove', move); window.addEventListener('pointerup', up);
    function savePage() { pages[cur].data = canvas.toDataURL('image/webp', 0.85); pages[cur].grid = grid; }
    function loadPage(i) { cur = i; grid = pages[i].grid || 'blank'; gridSel.value = grid; if (pages[i].data) { var im = new Image(); im.onload = function () { ctx.drawImage(im, 0, 0); }; im.src = pages[i].data; ctx.fillStyle = '#fff'; ctx.fillRect(0, 0, W, H); } else blank(); undo = []; status.textContent = 'صفحهٔ ' + fa(i + 1) + ' از ' + fa(pages.length); }
    var toolBtns = {};
    function tb(id, label, title) { var b = el('button', {class: 'wb-tool' + (tool === id ? ' on' : ''), text: label, title: title, onclick: function () { tool = id; Object.keys(toolBtns).forEach(function (k) { toolBtns[k].classList.toggle('on', k === id); }); }}); toolBtns[id] = b; return b; }
    var gridSel = el('select', {class: 'wb-sel', title: 'زمینه'}); [['blank', 'خالی'], ['lined', 'خط‌دار'], ['grid', 'شطرنجی'], ['line', 'محور اعداد'], ['quad', 'محور ۴ ناحیه'], ['first', 'ناحیهٔ اول'], ['polar', 'قطبی']].forEach(function (o) { gridSel.appendChild(el('option', {value: o[0], text: o[1]})); });
    gridSel.addEventListener('change', function () { if (!confirm('تغییر زمینه، رسم‌های این صفحه را پاک می‌کند. ادامه؟')) { gridSel.value = grid; return; } grid = gridSel.value; blank(); });
    var colorIn = el('input', {type: 'color', value: color, class: 'wb-color', title: 'رنگ'}); colorIn.addEventListener('input', function () { color = colorIn.value; });
    var sizeIn = el('input', {type: 'range', min: 1, max: 12, value: size, class: 'wb-size', title: 'ضخامت'}); sizeIn.addEventListener('input', function () { size = Number(sizeIn.value); });
    var bar = el('div', {class: 'engine-bar wb-bar'}, [
      el('span', {text: '🖍 تختهٔ سفید — سؤال ' + fa(run.exam.questions.indexOf(q) + 1)}),
      tb('pen', '✏️', 'قلم'), tb('eraser', '🧽', 'پاک‌کن'), tb('line', '╱', 'خط'), tb('arrow', '➚', 'پیکان'), tb('rect', '▭', 'مستطیل'), tb('circle', '◯', 'دایره'), tb('text', 'T', 'متن'),
      colorIn, sizeIn, gridSel,
      el('button', {class: 'wb-tool', text: '↶', title: 'برگرداندن', onclick: function () { var d = undo.pop(); if (!d) return; var im = new Image(); im.onload = function () { ctx.drawImage(im, 0, 0); }; im.src = d; }}),
      el('button', {class: 'wb-tool', text: '🗑', title: 'پاک کردن صفحه', onclick: function () { if (confirm('کل این صفحه پاک شود؟')) { push(); blank(); } }}),
      el('span', {class: 'grow'}), status,
      el('button', {class: 'btn light sm', text: '◀', title: 'صفحهٔ قبل', onclick: function () { if (cur > 0) { savePage(); loadPage(cur - 1); } }}),
      el('button', {class: 'btn light sm', text: '▶', title: 'صفحهٔ بعد', onclick: function () { savePage(); if (cur < pages.length - 1) loadPage(cur + 1); else if (pages.length < WHITEBOARD_MAX_PAGES) { pages.push(newPage()); loadPage(pages.length - 1); } else toast('حداکثر ' + fa(WHITEBOARD_MAX_PAGES) + ' صفحه.', 'err'); }}),
      el('button', {class: 'btn sm', text: '✅ ثبت به‌عنوان پاسخ', onclick: function () { savePage(); var out = pages.filter(function (p) { return p.data; }).map(function (p) { return p.data; }); close(); if (out.length) onDone(out); }}),
      el('button', {class: 'btn light sm', text: '✕', title: 'بستن بدون ثبت', onclick: function () { if (confirm('تخته بدون ثبت بسته شود؟')) close(); }})
    ]);
    function close() { window.removeEventListener('pointerup', up); bg.remove(); document.body.style.overflow = ''; }
    var area = el('div', {class: 'wb-area'}, [canvas]);
    bg.appendChild(bar); bg.appendChild(area); document.body.appendChild(bg); document.body.style.overflow = 'hidden';
    /* صفحه‌های قبلی تخته (اگر پاسخ فعلی از تخته آمده) بارگذاری می‌شوند */
    var prev = (run.images[q.id] || []).filter(function (u) { return /^data:/.test(u); });
    if (prev.length && q.maxAnswerImages <= 0) prev.forEach(function (d) { var p = newPage(); p.data = d; pages.push(p); }); else pages.push(newPage());
    loadPage(0);
  }

  /* ================================================================ ابزارها */
  function pickFile() { return new Promise(function (resolve) { var i = el('input', {type: 'file', accept: 'image/*', style: 'display:none'}); i.addEventListener('change', function () { resolve(i.files && i.files[0] ? i.files[0] : null); i.remove(); }); document.body.appendChild(i); i.click(); }); }
  function fileToDataUrl(file) { return new Promise(function (res, rej) { var r = new FileReader(); r.onload = function () { res(r.result); }; r.onerror = rej; r.readAsDataURL(file); }); }
  function downscale(file, maxDim) {
    return new Promise(function (resolve, reject) {
      var img = new Image(); var url = URL.createObjectURL(file);
      img.onload = function () { URL.revokeObjectURL(url); var w = img.naturalWidth, h = img.naturalHeight, k = Math.min(1, maxDim / Math.max(w, h)); var cv = document.createElement('canvas'); cv.width = Math.max(1, Math.round(w * k)); cv.height = Math.max(1, Math.round(h * k)); var c2 = cv.getContext('2d'); c2.fillStyle = '#fff'; c2.fillRect(0, 0, cv.width, cv.height); c2.drawImage(img, 0, 0, cv.width, cv.height); cv.toBlob(function (b) { b ? resolve(b) : reject(new Error('فشرده‌سازی تصویر ناموفق بود.')); }, 'image/webp', 0.9); };
      img.onerror = function () { URL.revokeObjectURL(url); reject(new Error('تصویر قابل خواندن نیست.')); };
      img.src = url;
    });
  }
  /* هشدار خروج هنگام آزمون باز */
  window.addEventListener('beforeunload', function (e) { if (run && !run.finished) { e.preventDefault(); e.returnValue = ''; } });

  window.SiteStudent = {page: page, decodeExam: decodeExam, stableShuffle: stableShuffle, sanitize: sanitize, hasActive: function () { return !!restoreActive(); }, inExam: function () { return !!(run && !run.finished); }};
})();
