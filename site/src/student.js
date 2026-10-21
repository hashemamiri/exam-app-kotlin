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
  var ANSWER_FIELDS = ['correctOption', 'correctIndex', 'correctAnswer', 'accept', 'answer', 'tolerance', 'matchAnswer', 'explanation', 'answer_key', 'rubric'];

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
      var q = {id: obj.id || ('q-' + oi), type: type, text: obj.text || '', sectionTitle: obj.sectionTitle || '', topic: obj.topic || '', passage: obj.passage || '', solutionSteps: Array.isArray(obj.solutionSteps) ? obj.solutionSteps.map(String) : [], assessmentParts: Array.isArray(obj.assessmentParts) ? obj.assessmentParts : [], proofAssumptions: String(obj.proofAssumptions||''), proofGoal: String(obj.proofGoal||''), constructionPrompt: String(obj.constructionPrompt||''), constructionLabels: Array.isArray(obj.constructionLabels)?obj.constructionLabels.map(String).slice(0,8):[], mode: obj.mode || '', score: Number(obj.score) || 0, images: images, maxAnswerImages: maxImg, answerImagesRequired: allow === 'required', originalIndex: oi,
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
      /* V305 — ابتدایی: مرتب‌سازی (موارد بُرخورده) و انتخاب روی تصویر (ناحیه‌ها به اندیس اصلی گزینه) */
      if (obj.mode === 'order' && type === 'fill') { q.mode = 'order'; q.orderItems = strs(obj.items).map(function (x) { return x.trim(); }).filter(Boolean); }
      /* V314.0 — بخش‌کردن و صداکشی + دسته‌بندی در سبد */
      if (obj.mode === 'syllable' && type === 'fill') { q.mode = 'syllable'; q.syllableCount = Math.max(2, Math.min(10, Number(obj.syllableCount) || 2)); }
      if (obj.mode === 'bucket' && type === 'fill') { q.mode = 'bucket'; q.buckets = strs(obj.buckets).map(function (x) { return x.trim(); }).filter(Boolean); q.bucketItems = strs(obj.items).map(function (x) { return x.trim(); }).filter(Boolean); }
      /* V306 — پاسخ با تخته (تشریحی ابتدایی) و املا (جای خالی + متن «بخوان») */
      if (obj.mode === 'draw' && type === 'essay') q.mode = 'draw';
      /* V309 — پاسخ صوتی با سقف مدت معلم */
      if (obj.mode === 'voice' && type === 'essay') { q.mode = 'voice'; q.voiceMaxSeconds = Math.max(10, Math.min(300, Number(obj.voice_max_seconds) || 60)); }
      if (obj.mode === 'dictation' && type === 'fill') { q.mode = 'dictation'; q.dictationText = obj.speak ? String(obj.speak) : ''; }
      if (obj.mode === 'hotspot' && type === 'multiple') { q.mode = 'hotspot'; q.hotspots = (Array.isArray(obj.hotspots) ? obj.hotspots : []).map(function (h) { return {x: Number(h.x) || 0, y: Number(h.y) || 0, w: Number(h.w) || 0, h: Number(h.h) || 0}; }); }
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
    var sectionDurations = raw.options && raw.options.sectionDurations && typeof raw.options.sectionDurations === 'object' ? raw.options.sectionDurations : {};
    return {id: examId, primary: !!(raw.options && raw.options.primary), sectionDurations: sectionDurations, /* V301 — حالت ابتدایی */ title: (raw.title || '').trim() || 'آزمون', code: raw.code || '', duration: duration, questions: ordered, subject: raw.subject || '', teacherMessage: (raw.teacher_message || '').trim() || null, deadline: deadline,
      attemptsAllowed: Math.max(1, Math.min(5, Number(raw.attempts_allowed) || 1)), attemptNumber: raw.attempt_no != null ? Number(raw.attempt_no) : (raw.attempt_number != null ? Number(raw.attempt_number) : null), attemptsRemaining: raw.attempts_remaining != null ? Number(raw.attempts_remaining) : null, raw: raw};
  }

  /* ================================================================ نشست فعال / پیش‌نویس (Room ↔ localStorage) */
  function activeGet() { try { var a = JSON.parse(localStorage.getItem(LS_ACTIVE) || 'null'); return a && a.owner === S.user().id ? a : null; } catch (e) { return null; } }
  function activeSet(a) { if (a) localStorage.setItem(LS_ACTIVE, JSON.stringify(a)); else localStorage.removeItem(LS_ACTIVE); }
  function draftGet(examId) { try { return JSON.parse(localStorage.getItem(LS_DRAFT + examId) || 'null') || {answers: {}, images: {}}; } catch (e) { return {answers: {}, images: {}}; } }
  function draftSet(examId, d) { try { localStorage.setItem(LS_DRAFT + examId, JSON.stringify(d)); } catch (e) {} }
  function draftClear(examId) { localStorage.removeItem(LS_DRAFT + examId); }
  /* V231 — صف ارسال آفلاین (آینهٔ PendingAction اپ): اگر ارسال نهایی به‌خاطر قطع اینترنت شکست بخورد، بستهٔ کامل
     (همان p_operation برای جلوگیری از دوبار ثبت) در localStorage می‌ماند و با رویداد online / باز شدن صفحه / دکمهٔ «ارسال اکنون» دوباره فرستاده می‌شود. */
  var LS_QUEUE = 'examsite.student.queue.v1';
  function queueAll() { try { var l = JSON.parse(localStorage.getItem(LS_QUEUE) || '[]'); return Array.isArray(l) ? l : []; } catch (e) { return []; } }
  function queueMine() { var u = S.user(); return queueAll().filter(function (q) { return u && q.owner === u.id; }); }
  function queueSave(list) { try { localStorage.setItem(LS_QUEUE, JSON.stringify(list)); return true; } catch (e) { return false; } }
  function queuePut(item) { var l = queueAll().filter(function (q) { return q.operation !== item.operation; }); l.push(item); return queueSave(l); }
  function queueDrop(op) { queueSave(queueAll().filter(function (q) { return q.operation !== op; })); }
  function isNetErr(e) { var m = String(e && e.message || e || '').toLowerCase(); return !navigator.onLine || /failed to fetch|networkerror|network request failed|load failed|timed? ?out|connection|err_internet|اینترنت/.test(m); }
  var flushing = false;
  async function flushQueue(manual) {
    if (flushing) return; var items = queueMine(); if (!items.length) return;
    if (!navigator.onLine) { if (manual) toast('هنوز اینترنت وصل نیست.', 'err'); return; }
    flushing = true; var sent = 0, lastErr = null;
    for (var i = 0; i < items.length; i++) {
      var it = items[i];
      try {
        var images = {};
        var qids = Object.keys(it.images || {});
        for (var a = 0; a < qids.length; a++) { var list = it.images[qids[a]] || []; images[qids[a]] = []; for (var k = 0; k < list.length; k++) { var url = await uploadAnswerImage(list[k], it.exam, qids[a]); images[qids[a]].push(url); list[k] = url; } }
        queuePut(it); /* آدرس تصاویر آپلودشده ذخیره شود تا در تلاش بعدی دوباره آپلود نشوند */
        if (it.report) await S.rpcObj('native_monitor_upsert_v1', {p_exam: it.exam, p_report: it.report}).catch(function () {});
        var raw = await S.rpcObj('native_submit_queued_answer_v1', {p_operation: it.operation, p_exam: it.exam, p_responses: it.responses, p_images: images, p_meta: Object.assign({}, it.meta, {flushed_at_epoch_ms: Date.now(), queued_offline: true})});
        if (raw && raw.error && !/قبلاً|تکراری|already|duplicate/i.test(String(raw.error))) throw new Error(String(raw.error));
        queueDrop(it.operation); draftClear(it.exam); sent++;
      } catch (e) { lastErr = e; it.attempts = (it.attempts || 0) + 1; it.lastError = errMsg(e); try { queuePut(it); } catch (x) {} if (isNetErr(e)) break; }
    }
    flushing = false; /* V250 — هر خطای پیش‌بینی‌نشده هم flushing را قفل نمی‌کند (queuePut در try) */
    if (sent) toast(fa(sent) + ' پاسخ صف‌شده ارسال شد.', 'ok');
    else if (manual && lastErr) toast('ارسال نشد: ' + errMsg(lastErr), 'err');
    if (sent && typeof S.render === 'function' && S.view && S.view.panel === 'exam' && !(run && !run.finished)) S.render();
  }
  window.addEventListener('online', function () { setTimeout(function () { flushQueue(false); }, 1500); });
  setInterval(function () { if (queueMine().length) flushQueue(false); }, 60000);
  function queueBanner() {
    var items = queueMine(); if (!items.length) return null;
    return el('div', {class: 'alert warn', style: 'max-width:520px;margin:0 auto 12px'}, [el('b', {text: fa(items.length) + ' پاسخ در صف ارسال است. '}),
      el('span', {text: navigator.onLine ? 'با اتصال اینترنت خودکار ارسال می‌شود.' : 'اینترنت قطع است؛ به‌محض اتصال خودکار ارسال می‌شود.'}),
      items[0].lastError ? el('div', {class: 'muted', style: 'font-size:12px;margin-top:4px', text: 'آخرین خطا: ' + items[0].lastError}) : null,
      el('div', {style: 'margin-top:8px'}, [el('button', {class: 'btn sm', text: 'ارسال اکنون', onclick: function () { flushQueue(true); }})])]);
  }
  /* نوار وضعیت اتصال داخل آزمون */
  function netBanner() {
    var b = el('div', {class: 'alert error st-net', style: 'display:' + (navigator.onLine ? 'none' : 'block'), text: 'اینترنت قطع است. نگران نباشید: پاسخ‌ها روی همین دستگاه ذخیره می‌شوند و زمان‌سنج ادامه دارد؛ ارسال نهایی هم در صورت قطعی، صف می‌شود و با اتصال خودکار فرستاده می‌شود.'});
    var on = function () { b.style.display = 'none'; }, off = function () { b.style.display = 'block'; };
    window.addEventListener('online', on); window.addEventListener('offline', off);
    return b;
  }

  function isTeacher() { return !!(S.user() && S.user().role === 'teacher'); }
  function buildTeacherPracticeExam(raw) {
    var keysMap = {};
    (Array.isArray(raw.__answers) ? raw.__answers : []).forEach(function (k, i) { if (k && typeof k === 'object') keysMap[k.i != null ? Number(k.i) : i] = k; });
    var rawQs = Array.isArray(raw.questions) ? raw.questions : [];
    var now = Date.now(), safe = sanitize(raw);
    var exam = decodeExam(safe, S.user().id, now);
    exam.practice = true;
    exam.rawQuestions = rawQs;
    exam.answerKeys = keysMap;
    return exam;
  }
  async function joinTeacherPracticeById(examId) {
    var raw = await S.api.examDetail(examId);
    if (!Array.isArray(raw.questions) || !raw.questions.length) throw new Error('این آزمون هنوز سؤالی ندارد.');
    return buildTeacherPracticeExam(raw);
  }
  async function joinTeacherPracticeByCode(code) {
    var clean = code.trim().toUpperCase();
    try {
      var list = await S.api.exams();
      var own = (list || []).filter(function (x) { return String(x.code || '').trim().toUpperCase() === clean; })[0];
      if (own) return await joinTeacherPracticeById(own.id);
    } catch (e) {}
    var raw = await S.rpcObj('get_exam_for_student', {p_code: clean});
    if (raw && raw.error) throw new Error(String(raw.error));
    var exam = decodeExam(sanitize(raw), S.user().id, Date.now());
    exam.practice = true;
    exam.rawQuestions = Array.isArray(raw.questions) ? raw.questions : [];
    exam.answerKeys = {};
    return exam;
  }
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
        (function go() { tries++; if (w.renderRichText && w.GeoFig && typeof w.__hostDelimHtml === 'function') return resolve(w); /* V303.1 — تا دلیمترهای SVG هم نصب شوند */ if (tries < 200) setTimeout(go, 50); else resolve(null); })();
      });
      S.engineHtml('print').then(function (h) { if (h) f.srcdoc = h; else resolve(null); });
    });
    return mathReady;
  }
  async function richHtml(text) {
    var w = await ensureMathFrame();
    if (!w) return esc(text).replace(/\n/g, '<br>');
    try { var t = document.createElement('template'); t.innerHTML = w.renderRichText(String(text || ''), null); simplifyFigs(t.content); return t.innerHTML; } catch (e) { return esc(text).replace(/\n/g, '<br>'); }
  }
  /* V192 — شکل‌های تصویری موتور (آناتومی/فیزیک/شیمی/تصویر کاربر) خارج از صفحهٔ چاپ CSS قاب‌هایشان را ندارند (an-stage با height:0)
     → بازسازی مثل AtlasBitmapRenderer اپ: عنوان، تصویر با فلش‌ها و شماره‌ها (مختصات درصدی روی خودِ تصویر)، و زیر آن ردیف‌های
     «شماره + خط‌چین» برای نوشتن نام هر قسمت (وقتی X.blank≠'0'). آدرس تصویر: X.img (data:/http) یا src خود <img> موتور. */
  function simplifyFigs(root) {
    if (!root || !root.querySelectorAll) return;
    Array.prototype.forEach.call(root.querySelectorAll('.qmf-fig'), function (f) {
      if (!f.querySelector('.an-plate, .an-frame, img.an-svg, img.sc-svg')) return;
      var spec = null; try { spec = JSON.parse(f.getAttribute('data-fig') || '{}'); } catch (e) { spec = {}; }
      var X = (spec && spec.X) || {}, src = '';
      if (typeof X.img === 'string' && /^(data:image\/|https?:\/\/|blob:)/.test(X.img)) src = X.img;
      if (!src) { var im = f.querySelector('img.an-svg, img.sc-svg, img'); if (im) src = im.getAttribute('src') || ''; }
      if (!src || /^anatomy\/photo\.svg$/.test(src)) return;
      var cap = f.querySelector('.tbx-cap'); var title = cap ? cap.textContent : '';
      var ov = f.querySelector('svg.an-ov'); var pins = f.querySelectorAll('.an-num, .an-tail'); var af = f.querySelector('.an-af');
      var frame = document.createElement('span'); frame.className = 'b-fig-frame';
      var img = document.createElement('img'); img.src = src; img.alt = title || 'شکل'; img.draggable = false; frame.appendChild(img);
      if (ov) frame.appendChild(ov);
      Array.prototype.forEach.call(pins, function (p) { frame.appendChild(p); });
      f.innerHTML = ''; f.classList.add('b-fig-simple'); f.classList.remove('qmf-sc'); /* qmf-sc در موتور zoom:.42 دارد */
      if (title) { var t = document.createElement('div'); t.className = 'b-fig-cap'; t.textContent = title; f.appendChild(t); }
      f.appendChild(frame);
      if (af) { af.className = 'b-fig-af'; f.appendChild(af); }
    });
  }
  /* V218 — اندازه‌دهی علامت √ و جداکننده‌های کشسان موتور پس از درج در DOM (بدون آن رادیکال نمایش داده نمی‌شد) */
  function fitMath(root) {
    if (!root || !mathFrame || !mathFrame.contentWindow) return;
    try { var w = mathFrame.contentWindow; if (typeof w.fitMathStretchers === 'function') w.fitMathStretchers(root); } catch (e) {}
  }
  function mathCss() {
    /* فقط قواعد ریاضی/شکل موتور چاپ، محدود به ناحیهٔ سؤال (تا با استایل سایت تداخل نکند) */
    if (document.getElementById('stMathCss')) return;
    ensureMathFrame().then(function (w) {
      if (!w) return; var out = [];
      var keep = /\.(mathx|mfrac|mnum|mden|msqrt|mroot|msup|msub|mrow|mtable|mtr|mtd|mover|munder|mo|mi|mn|msurd|surd-svg|root-line|mrad|mrad-empty|mrootidx|msym|mvar|mfun|mtext|mgrp|msfrac|mslash|mbrk|mbrk-[a-z]+|mbigop|mbigsym|mdelim|mdelim-glyph|mmatrix|mmatrix-[lr]|mmixed|mword|mparbox|mpar-body|moverset|moverset-t|mhspace|mubrace|mubrace-[a-z]+|mb-delim-svg|mbb|mcal|mm-w|math-[a-z-]+|qmf-fig|fig-[a-z-]+|interactive-figure|vt-[a-z-]+|tf-[a-z-]+|gf-svg|pt-[a-z-]+|msubsup|msubsup-[a-z]+|ss-base|tbx|tbx-[a-z0-9-]+|qmf-tab|tb-[a-z-]+)\b/; /* V288.2 — ظرف توان+اندیس هم‌زمان (.msubsup/.ss-base) هم منتقل می‌شود؛ قبلاً d²ₚ در سایت زیر هم می‌شکست */ /* V259 — جدول (tbx-*) هم منتقل می‌شود؛ پیش‌تر جدول‌ها در سایت بدون استایل بودند */
      Array.prototype.forEach.call(w.document.styleSheets, function (sh) {
        var rules; try { rules = sh.cssRules; } catch (e) { return; }
        Array.prototype.forEach.call(rules, function (r) {
          if (r.type === 1 && r.selectorText && keep.test(r.selectorText) && !/^(html|body|\*)/.test(r.selectorText)) out.push(r.selectorText.split(',').map(function (x) { return '.st-exam ' + x.trim() + ',.math-rich ' + x.trim(); }).join(',') + '{' + r.style.cssText + '}');
          else if (r.type === 5 && /math|mfrac|frac/i.test(r.cssText)) out.push(r.cssText);
        });
      });
      /* V218.2 — همان قواعد .qmf-atom موتور چاپ (tools_styles.css): خط کسر به عرض کامل صورت/مخرج و رادیکال کشیده؛ بیرون از .qmf-atom اعمال نمی‌شدند */
      out.push('.st-exam .mathx .mfrac,.math-rich .mathx .mfrac{align-items:stretch !important}.st-exam .mathx .mfrac>.mnum,.st-exam .mathx .mfrac>.mden,.math-rich .mathx .mfrac>.mnum,.math-rich .mathx .mfrac>.mden{width:100%;box-sizing:border-box;text-align:center}.st-exam .msqrt,.math-rich .msqrt{overflow:visible;align-items:stretch}');
      var st = document.createElement('style'); st.id = 'stMathCss'; st.textContent = out.join('\n'); document.head.appendChild(st);
    });
  }

  /* ================================================================ صفحهٔ «شرکت در آزمون» */
  var run = null; /* وضعیت آزمون جاری */
  async function page(c, arg) {
    c.innerHTML = '';
    if (run && !run.finished) { c.appendChild(examUI(c)); return; }
    var pending = restoreActive();
    var qb = queueBanner(); if (qb) c.appendChild(qb); /* V231 */
    if (navigator.onLine && queueMine().length) setTimeout(function () { flushQueue(false); }, 300);
    var card = el('div', {class: 'card', style: 'max-width:520px;margin:0 auto'});
    card.appendChild(el('h3', {text: '🔑 شرکت در آزمون'}));
    if (pending) {
      card.appendChild(el('div', {class: 'alert warn'}, [el('b', {text: 'آزمون نیمه‌تمام دارید: '}), el('span', {text: pending.title + ' (' + pending.code + ')'}),
        el('div', {style: 'margin-top:8px'}, [el('button', {class: 'btn sm', text: 'پیوستن به آزمون', onclick: function () { startExam(c, pending, true); }}), ' ', el('button', {class: 'btn light sm', text: 'انصراف از این آزمون', onclick: function () { activeSet(null); page(c); }})])]));
    }
    var inp = el('input', {type: 'text', placeholder: 'کد ۶ حرفی آزمون', maxlength: 8, style: 'text-align:center;letter-spacing:4px;font-size:22px;direction:ltr;text-transform:uppercase'});
    if (arg && arg.code) inp.value = String(arg.code); /* V232.9 — از اعلان «آزمون باز شد» */
    var msg = el('div');
    var btn = el('button', {class: 'btn', style: 'width:100%', text: 'ورود به آزمون', onclick: async function () {
      var code = en(inp.value).trim().toUpperCase(); if (code.length < 4) return toast('کد آزمون را وارد کنید.', 'err');
      btn.disabled = true; msg.innerHTML = '<div class="loading"><span class="spinner"></span> در حال دریافت آزمون…</div>';
      try { var exam = isTeacher() ? await joinTeacherPracticeByCode(code) : await joinByCode(code); msg.innerHTML = ''; startExam(c, exam, false); } catch (e) { msg.innerHTML = ''; msg.appendChild(el('div', {class: 'alert error', text: errMsg(e)})); btn.disabled = false; }
    }});
    inp.addEventListener('keydown', function (e) { if (e.key === 'Enter') btn.click(); });
    card.appendChild(el('div', {class: 'field'}, [el('label', {text: 'کد آزمون'}), inp]));
    card.appendChild(btn); card.appendChild(msg);
    card.appendChild(el('p', {class: 'muted', style: 'font-size:13px;margin-top:14px', text: isTeacher() ? 'کد ۶ حرفی آزمون را وارد کنید یا یکی از آزمون‌های خود را از فهرست زیر انتخاب کنید (بدون ثبت در آمار کلاس).' : 'کد را از معلم بگیرید. پس از ورود، زمان‌سنج شروع می‌شود و پاسخ‌ها به‌صورت خودکار روی همین مرورگر ذخیره می‌شوند.'}));
    c.appendChild(card);
    if (isTeacher()) {
      var listCard = el('div', {class: 'card', style: 'max-width:720px;margin:16px auto 0'});
      listCard.appendChild(el('h3', {text: '📝 آزمون‌های من (پاسخ‌گویی آزمایشی در محیط دانش‌آموز)'}));
      var listBox = el('div'); S.loading(listBox); listCard.appendChild(listBox);
      c.appendChild(listCard);
      S.api.exams().then(function (exams) {
        listBox.innerHTML = '';
        if (!exams || !exams.length) { listBox.appendChild(el('p', {class: 'muted', text: 'هنوز آزمونی نساخته‌اید.'})); return; }
        exams.forEach(function (x) {
          var startBtn = el('button', {class: 'btn sm', text: '▶ شروع آزمون', onclick: async function () {
            startBtn.disabled = true; startBtn.textContent = 'در حال باز کردن…';
            try { var ex = await joinTeacherPracticeById(x.id); startExam(c, ex, false); }
            catch (e) { toast(errMsg(e), 'err'); startBtn.disabled = false; startBtn.textContent = '▶ شروع آزمون'; }
          }});
          listBox.appendChild(el('div', {class: 'row', style: 'padding:10px 0;border-bottom:1px solid var(--line,rgba(128,128,128,.18));align-items:center;gap:10px'}, [
            el('div', {class: 'grow'}, [
              el('div', {class: 'row', style: 'gap:8px;align-items:center'}, [
                el('b', {text: x.title || 'بدون عنوان'}),
                el('span', {class: 'chip ' + (x.is_open ? 'ok' : 'warn'), text: x.is_open ? 'باز' : 'بسته'})
              ]),
              el('div', {class: 'muted', style: 'font-size:12px;margin-top:4px', text: [(x.subject || 'بدون درس'), 'کد ' + (x.code || '—'), fa(x.duration || 0) + ' دقیقه', 'بارم ' + fa(S.fmtScore(x.total_score))].join(' · ')})
            ]),
            startBtn
          ]));
        });
      }).catch(function (e) { listBox.innerHTML = ''; listBox.appendChild(el('div', {class: 'alert error', text: errMsg(e)})); });
    }
  }

  /* ================================================================ اجرای آزمون */
  /* V302 — خواندن سؤال با SpeechSynthesis مرورگر (فارسی اگر صدایی باشد). فرمول‌ها به گفتار ساده تبدیل می‌شوند. */
  function spokenTex(t) {
    var i; for (i = 0; i < 3; i++) { t = t.replace(/\\[dt]?frac\{([^{}]*)\}\{([^{}]*)\}/g, ' $1 تقسیم بر $2 ').replace(/\\sqrt\{([^{}]*)\}/g, ' جذر $1 ').replace(/\^\{([^{}]*)\}/g, ' به توان $1 ').replace(/\^(\w)/g, ' به توان $1 ').replace(/_\{([^{}]*)\}/g, ' اندیس $1 '); }
    var W = [['\\times', ' ضرب در '], ['\\cdot', ' ضرب در '], ['\\div', ' تقسیم بر '], ['\\pm', ' مثبت یا منفی '], ['\\leq', ' کوچکتر یا مساوی '], ['\\geq', ' بزرگتر یا مساوی '], ['\\neq', ' مخالف '], ['\\approx', ' تقریباً '], ['\\le', ' کوچکتر یا مساوی '], ['\\ge', ' بزرگتر یا مساوی '], ['\\ne', ' مخالف '], ['\\pi', ' پی '], ['\\infty', ' بی‌نهایت '], ['\\degree', ' درجه '], ['\\%', ' درصد '], ['\\left', ''], ['\\right', ''], ['\\text', ''], ['\\mathrm', ''], ['\\Box', ' جای خالی '], ['\\square', ' جای خالی '], ['=', ' مساوی '], ['+', ' به‌علاوهٔ '], ['-', ' منهای '], ['−', ' منهای '], ['×', ' ضرب در '], ['÷', ' تقسیم بر '], ['<', ' کوچکتر از '], ['>', ' بزرگتر از '], ['*', ' ضرب در '], ['/', ' تقسیم بر ']];
    W.forEach(function (w) { t = t.split(w[0]).join(w[1]); });
    return t.replace(/\\[a-zA-Z]+/g, ' ').replace(/[{}\\]/g, ' ').replace(/ +/g, ' ').trim();
  }
  function speechPlain(src) {
    src = String(src || '').replace(/%%FIG:[\s\S]*?%%/g, ' شکل ').replace(/%%[\s\S]*?%%/g, ' ');
    src = src.replace(/\$\$([\s\S]*?)\$\$/g, function (m, t) { return ' ' + spokenTex(t) + ' '; }).replace(/\$([^$\n]*?)\$/g, function (m, t) { return ' ' + spokenTex(t) + ' '; });
    return src.replace(/<[^>]+>/g, ' ').replace(/[ \t]+/g, ' ').trim();
  }
  function speechForQuestion(q) {
    var s = speechPlain(q.text), L = ['الف', 'ب', 'ج', 'د', 'ه', 'و'];
    if (q.type === 'multiple') (q.options || []).forEach(function (o, i) { var p = speechPlain(o); if (p) s += '. گزینهٔ ' + (L[i] || String(i + 1)) + ': ' + p; });
    else if (q.type === 'truefalse') s += '. درست یا نادرست؟';
    return s;
  }
  var speechState = {speaking: false, btn: null};
  function speechStop() { try { if (window.speechSynthesis) window.speechSynthesis.cancel(); } catch (e) {} speechState.speaking = false; if (speechState.btn) speechState.btn.textContent = '🔊 سؤال را برایم بخوان'; }
  function readAloudButton(q) {
    if (!window.speechSynthesis || !window.SpeechSynthesisUtterance) return el('span');
    speechStop();
    var b = el('button', {class: 'btn st-read', text: '🔊 سؤال را برایم بخوان'});
    speechState.btn = b;
    b.addEventListener('click', function () {
      if (speechState.speaking) { speechStop(); return; }
      var text = (q.mode === 'dictation' && q.dictationText) ? q.dictationText : speechForQuestion(q); if (!text) return;
      var u = new SpeechSynthesisUtterance(text); u.lang = 'fa-IR'; u.rate = 0.85;
      var voices = window.speechSynthesis.getVoices() || [];
      var v = voices.filter(function (x) { return /^fa/i.test(x.lang); })[0]; if (v) u.voice = v;
      u.onend = u.onerror = function () { speechStop(); };
      speechState.speaking = true; b.textContent = '⏹ بس است';
      window.speechSynthesis.cancel(); window.speechSynthesis.speak(u);
    });
    return b;
  }
  /* V303 — همهٔ گزینه‌ها «تصویری»اند؟ (تصویر دارند یا متن کوتاه ≤۴ نویسه بدون فرمول) */
  function isPictureOptionSet(q) {
    if (!q.options || q.options.length < 2 || q.options.length > 6) return false;
    return q.options.every(function (o, i) { var img = q.optionImages && q.optionImages[i]; var t = String(o || '').trim(); return !!img || (t && t.indexOf('$') < 0 && t.indexOf('%%') < 0 && Array.from(t).length <= 4); });
  }
  function startExam(c, exam, resumed) {
    var d = exam.practice ? {answers: {}, images: {}} : draftGet(exam.id);
    run = {exam: exam, practice: !!exam.practice, index: 0, answers: d.answers || {}, images: d.images || {}, started: true, finished: false, submitting: false, enteredAt: Date.now(), qEnter: {}, qTime: {}, qVisits: {}, events: {}, sectionDeadlines: d.sectionDeadlines || {}, expiredSections: d.expiredSections || {}, timer: null, monitorTimer: null};
    run.qEnter[0] = Date.now(); run.qVisits[exam.questions[0] && exam.questions[0].id] = 1;
    document.addEventListener('visibilitychange', onVis);
    window.addEventListener('blur', onBlur);
    guardsOn();
    run.monitorTimer = setInterval(function () { reportMonitor(false); }, 60000);
    reportMonitor(false);
    /* V149 — در پوستهٔ موبایل، آزمون تمام‌صفحه می‌شود (نوار بالا حذف؛ مثل StudentExamScreen اپ) */
    if (window.SiteMobile && window.SiteMobile.active()) { window.SiteMobile.paint(); c = document.getElementById('content'); }
    c.innerHTML = ''; c.appendChild(examUI(c));
    if (resumed) toast('به آزمون نیمه‌تمام پیوستید.', 'ok');
  }
  /* V269 — بستر آزمون برای گزارش نظارتی معلم */
  function platformName() { return window.SiteMobile && window.SiteMobile.active() ? 'mobile' : 'desktop'; }
  /* V269 — جلوگیری از کپی متن سؤال‌ها و ثبت تلاش‌ها (کپی/برش/منوی راست‌کلیک/کشیدن، PrintScreen، Ctrl+P / Ctrl+S)؛
     کادرهای پاسخ (input/textarea) آزادند. مرورگر امکان مسدود کردن اسکرین‌شات را نمی‌دهد؛ فقط تلاش گزارش می‌شود. */
  function inField(t) { return !!(t && t.closest && t.closest('input, textarea, [contenteditable=true]')); }
  function onCopyGuard(e) { if (!run || run.finished || inField(e.target)) return; e.preventDefault(); secEvent('copy_attempt'); toast('کپی متن آزمون مجاز نیست.', 'err'); }
  function onCtxGuard(e) { if (!run || run.finished || inField(e.target)) return; e.preventDefault(); secEvent('copy_attempt'); }
  function onKeyGuard(e) {
    if (!run || run.finished) return;
    var k = (e.key || '').toLowerCase();
    if (k === 'printscreen') { secEvent('screenshot_attempt'); toast('اسکرین‌شات از آزمون گزارش می‌شود.', 'err'); try { navigator.clipboard && navigator.clipboard.writeText(''); } catch (x) {} return; }
    if ((e.ctrlKey || e.metaKey) && (k === 'p' || k === 's' || (e.shiftKey && k === 's'))) { e.preventDefault(); secEvent('print_attempt'); toast('چاپ/ذخیرهٔ صفحهٔ آزمون مجاز نیست.', 'err'); return; }
    if ((e.ctrlKey || e.metaKey) && (k === 'c' || k === 'x' || k === 'a') && !inField(e.target)) { e.preventDefault(); secEvent('copy_attempt'); }
  }
  function guardsOn() { document.addEventListener('copy', onCopyGuard); document.addEventListener('cut', onCopyGuard); document.addEventListener('contextmenu', onCtxGuard); document.addEventListener('dragstart', onCtxGuard); document.addEventListener('keydown', onKeyGuard); document.body.classList.add('st-nocopy'); }
  function guardsOff() { document.removeEventListener('copy', onCopyGuard); document.removeEventListener('cut', onCopyGuard); document.removeEventListener('contextmenu', onCtxGuard); document.removeEventListener('dragstart', onCtxGuard); document.removeEventListener('keydown', onKeyGuard); document.body.classList.remove('st-nocopy'); }
  function onVis() { if (document.hidden) secEvent('app_leave'); }
  function onBlur() { secEvent('window_blur'); }
  function secEvent(k) { if (!run || run.finished || !run.started) return; run.events[k] = (run.events[k] || 0) + 1; reportMonitor(false); }
  function monitorReport(final) {
    var now = Date.now(), times = Object.assign({}, run.qTime);
    var q = run.exam.questions[run.index]; var ent = run.qEnter[run.index];
    if (q && ent != null) times[q.id] = (times[q.id] || 0) + (now - ent);
    var labels = {}; run.exam.questions.forEach(function (x) { labels[x.id] = x.originalIndex + 1; });
    return {entered_at_epoch_ms: run.enteredAt, left_at_epoch_ms: now, submitted: !!final, platform: platformName(), events: run.events, question_time_ms: times, question_visits: run.qVisits, question_labels: labels};
  }
  var lastReport = 0;
  function reportMonitor(final) {
    if (!run || run.practice) return Promise.resolve();
    if (!final && Date.now() - lastReport < 5000) return Promise.resolve();
    lastReport = Date.now();
    return S.rpcObj('native_monitor_upsert_v1', {p_exam: run.exam.id, p_report: monitorReport(final)}).catch(function () {});
  }
  function goTo(i) {
    var target = run && run.exam.questions[Math.max(0, Math.min(run.exam.questions.length - 1, i))];
    if (target && target.sectionTitle && run.expiredSections[String(target.sectionTitle).trim()]) { toast('زمان این بخش تمام شده است و امکان بازگشت نیست.', 'err'); return; }
    speechStop(); /* V302 */
    var now = Date.now(), q = run.exam.questions[run.index], ent = run.qEnter[run.index];
    if (q && ent != null) { run.qTime[q.id] = (run.qTime[q.id] || 0) + (now - ent); }
    run.index = Math.max(0, Math.min(run.exam.questions.length - 1, i));
    run.qEnter[run.index] = now;
    var nq = run.exam.questions[run.index]; if (nq) run.qVisits[nq.id] = (run.qVisits[nq.id] || 0) + 1;
  }
  function saveDraft() { if (run && !run.practice) draftSet(run.exam.id, {answers: run.answers, images: run.images, sectionDeadlines: run.sectionDeadlines, expiredSections: run.expiredSections}); }
  function stopAll() { guardsOff(); clearInterval(run.timer); clearInterval(run.monitorTimer); document.removeEventListener('visibilitychange', onVis); window.removeEventListener('blur', onBlur); }

  function examUI(c) {
    var ex = run.exam;
    var wrap = el('div', {class: 'st-exam' + (ex.primary ? ' primary' : '')}); /* V301 — کلاس primary: متن درشت و گزینه‌های بزرگ */
    wrap.appendChild(netBanner()); /* V231 — نوار قطع اینترنت */
    var timerEl = el('span', {class: 'st-timer'});
    var head = el('div', {class: 'card st-head'}, [
      el('div', {}, [el('b', {text: ex.title}), el('div', {class: 'muted', style: 'font-size:12px', text: [ex.subject, 'کد ' + ex.code, run.practice ? 'حالت آزمایشی معلم' : (ex.attemptNumber ? 'تلاش ' + fa(ex.attemptNumber) + ' از ' + fa(ex.attemptsAllowed) : '')].filter(Boolean).join(' · ')})]),
      el('span', {class: 'grow'}), timerEl,
      run.practice ? el('button', {class: 'btn light sm', text: '✕ خروج', onclick: async function () { if (await S.confirmDlg('خروج از آزمون آزمایشی', 'از آزمون آزمایشی خارج می‌شوید؟', 'خروج', true)) { run.finished = true; stopAll(); run = null; if (window.SiteMobile && window.SiteMobile.active()) window.SiteMobile.paint(); else page(c); } }}) : null,
      el('button', {class: 'btn sm', text: '✅ ارسال پاسخ‌ها', onclick: function () { review(c); }})
    ]);
    wrap.appendChild(head);
    if (ex.teacherMessage) wrap.appendChild(el('div', {class: 'alert info', text: '💬 ' + ex.teacherMessage}));
    var nav = el('div', {class: 'st-nav'});
    var body = el('div', {class: 'card st-body'});
    var foot = el('div', {class: 'row', style: 'margin-top:12px'});
    if (ex.primary) nav.style.display = 'none'; /* V301 — ابتدایی: نوار شماره‌ها پنهان؛ فقط قبلی/بعدی بزرگ */
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
      if (q.sectionTitle && run.expiredSections[String(q.sectionTitle).trim()]) { body.appendChild(el('div',{class:'alert warn',text:'زمان این بخش تمام شد؛ پاسخ‌های ثبت‌شده حفظ شده‌اند و امکان ویرایش یا بازگشت نیست.'})); return; }
      var p = q.pres;
      var txt = el('div', {class: 'st-qtext', style: 'text-align:' + p.align + ';font-size:' + p.fontSize + 'px;' + (p.bold ? 'font-weight:700;' : '') + (p.italic ? 'font-style:italic;' : '') + (p.font && p.font !== 'default' ? 'font-family:' + p.font + ',Vazirmatn;' : ''), html: esc(q.text).replace(/\n/g, '<br>')});
      if (q.sectionTitle) { var sq = ex.questions.filter(function (x) { return x.sectionTitle === q.sectionTitle; }); var ss = sq.reduce(function (n, x) { return n + num(x.score); }, 0); body.appendChild(el('div', {class: 'card', style: 'padding:10px;margin-bottom:8px;color:var(--brand)'}, [el('b',{text:q.sectionTitle}),el('div',{class:'muted',style:'font-size:12px;margin-top:3px',text:fa(sq.length)+' سؤال · جمع بارم '+fa(S.fmtScore(ss))+(ex.sectionDurations&&ex.sectionDurations[String(q.sectionTitle).trim()]?' · زمان '+fa(ex.sectionDurations[String(q.sectionTitle).trim()])+' دقیقه':'' )})])); }
      body.appendChild(el('div', {class: 'row', style: 'margin-bottom:10px'}, [el('span', {class: 'chip brand', text: 'سؤال ' + fa(run.index + 1)}), el('span', {class: 'chip', text: 'بارم ' + fa(S.fmtScore(q.score))}), el('span', {class: 'chip', text: {multiple: 'چندگزینه‌ای', truefalse: 'صحیح/غلط', fill: 'جای‌خالی', numeric: 'عددی', matching: 'جورکردنی', essay: 'تشریحی'}[q.type] || ''})]));
      body.appendChild(txt);
      richHtml(q.text).then(function (h) { if (ex.questions[run.index] === q) { txt.innerHTML = h; fitMath(txt); } });
      if (q.passage) {
        var passageBox = el('div', {class:'card', style:'padding:14px;margin:8px 0;background:var(--soft)'}, [el('b',{text:'متن/صورت مشترک'}), el('div',{style:'margin-top:8px;white-space:pre-wrap',text:q.passage})]);
        body.insertBefore(passageBox, txt);
      }
      if (p.audio) body.appendChild(el('audio', {controls: 'controls', src: p.audio, style: 'width:100%;margin:8px 0'}));
      else if (ex.primary || q.mode === 'dictation') body.appendChild(readAloudButton(q)); /* V302 — ابتدایی: «بخوان» با صدای مرورگر وقتی معلم صدا نگذاشته */
      if (q.images.length) body.appendChild(el('div', {class: 'st-imgs'}, q.images.map(function (u) { return el('img', {src: u, onclick: function () { lightbox(u); }}); })));
      body.appendChild(answerArea(q));
    }
    /* ===== V305 — ابزارهای ابتدایی: انتخاب روی تصویر، مرتب‌سازی با کشیدن، پد عددی بزرگ (همان قراردادهای اپ) ===== */
    var PRI_L = ['الف', 'ب', 'ج', 'د', 'ه', 'و', 'ز', 'ح'];
    function priL(i) { return PRI_L[i] || fa(i + 1); }
    function hasZone(z) { return !!z && z.w > 0 && z.h > 0; }
    function hotspotAnswer(q, a, onPick) {
      var wrap = el('div', {class: 'st-hs'});
      wrap.appendChild(el('div', {style: 'font-weight:700;margin-bottom:8px', text: 'روی ناحیهٔ درست تصویر بزنید:'}));
      var pic = el('div', {style: 'position:relative;display:inline-block;max-width:100%'});
      if (q.images && q.images[0]) pic.appendChild(el('img', {src: q.images[0], alt: '', style: 'display:block;max-width:100%;height:auto'}));
      q.options.forEach(function (o, di) {
        var oi = q.optionOriginalIndices && q.optionOriginalIndices[di] != null ? q.optionOriginalIndices[di] : di;
        var z = (q.hotspots || [])[oi];
        if (!hasZone(z)) return;
        var on = a === oi;
        pic.appendChild(el('button', {type: 'button', 'aria-label': 'گزینهٔ ' + priL(di), style: 'position:absolute;left:' + z.x + '%;top:' + z.y + '%;width:' + z.w + '%;height:' + z.h + '%;border:' + (on ? '4px' : '2px') + ' solid ' + (on ? '#0a7a50' : '#e8a33d') + ';border-radius:10px;background:' + (on ? 'rgba(10,122,80,.3)' : 'rgba(0,0,0,.12)') + ';cursor:pointer;font-weight:800;color:#fff;text-shadow:0 1px 2px #000;font-size:18px', text: (on ? '✓ ' : '') + priL(di), onclick: function () { onPick(oi); }}));
      });
      wrap.appendChild(pic);
      q.options.forEach(function (o, di) {
        var oi = q.optionOriginalIndices && q.optionOriginalIndices[di] != null ? q.optionOriginalIndices[di] : di;
        var on = a === oi;
        wrap.appendChild(el('div', {style: 'margin-top:6px;font-size:17px;font-weight:' + (on ? '800' : '400') + ';color:' + (on ? '#0a7a50' : 'inherit'), html: priL(di) + ') ' + esc(o)}));
      });
      return wrap;
    }
    /* مرتب‌سازی: ترتیب فعلی از پاسخ ذخیره‌شده (اگر موارد جور باشند) وگرنه ترتیب اولیه؛ کشیدن با دستگیرهٔ ☰ یا ▲▼ */
    function orderFromAnswerJs(saved, items) {
      if (!saved) return items.slice();
      var parts = String(saved).split(',').map(function (x) { return x.trim(); });
      var a = parts.slice().sort().join('\u0001'), b = items.slice().sort().join('\u0001');
      return (parts.length === items.length && a === b) ? parts : items.slice();
    }
    function orderAnswer(q, a, onChange) {
      var cur = orderFromAnswerJs(a, q.orderItems);
      var box = el('div', {class: 'st-order'});
      box.appendChild(el('div', {style: 'font-weight:700;margin-bottom:6px', text: 'موارد را به ترتیب درست بچینید (با ☰ بکشید یا ▲ ▼):'}));
      cur.forEach(function (item, i) {
        var handle = el('span', {class: 'st-handle', text: '☰', style: 'cursor:grab;font-size:24px;padding:4px 8px;touch-action:none;user-select:none'});
        var up = el('button', {type: 'button', class: 'btn light sm', text: '▲', onclick: function () { var n = cur.slice(); var t = n[i - 1]; n[i - 1] = n[i]; n[i] = t; onChange(n); }});
        var dn = el('button', {type: 'button', class: 'btn light sm', text: '▼', onclick: function () { var n = cur.slice(); var t = n[i + 1]; n[i + 1] = n[i]; n[i] = t; onChange(n); }});
        if (i === 0) up.disabled = true;
        if (i === cur.length - 1) dn.disabled = true;
        var row = el('div', {class: 'st-orow', style: 'display:flex;align-items:center;gap:8px;padding:10px;margin:6px 0;border:2px solid var(--line,#ccc);border-radius:12px;background:var(--card,#fff)'}, [handle, el('b', {text: fa(i + 1) + '.'}), el('span', {style: 'flex:1;font-size:17px', text: item}), up, dn]);
        handle.addEventListener('pointerdown', function (e) {
          e.preventDefault();
          if (handle.setPointerCapture) { try { handle.setPointerCapture(e.pointerId); } catch (err) {} }
          var rows = box.querySelectorAll('.st-orow'), from = i, to = i;
          row.style.opacity = '.55';
          function mv(ev) {
            var t = 0;
            for (var k = 0; k < rows.length; k++) { var r = rows[k].getBoundingClientRect(); if (ev.clientY > r.top + r.height / 2) t = k; }
            to = t;
            for (var m = 0; m < rows.length; m++) rows[m].style.borderColor = m === to ? '#0a7a50' : 'var(--line,#ccc)';
          }
          function up2() {
            handle.removeEventListener('pointermove', mv); handle.removeEventListener('pointerup', up2); handle.removeEventListener('pointercancel', up2);
            if (to !== from) { var n = cur.slice(); n.splice(to, 0, n.splice(from, 1)[0]); onChange(n); }
            else row.style.opacity = '';
          }
          handle.addEventListener('pointermove', mv); handle.addEventListener('pointerup', up2); handle.addEventListener('pointercancel', up2);
        });
        box.appendChild(row);
      });
      return box;
    }
    /* V314.0 — بخش‌کردن و صداکشی: کادرهای رنگی مجزا + صفحه‌کلید نشانه‌ها و مصوت‌های فارسی */
    function syllableAnswer(q, saved, set) {
      var count = Math.max(2, Math.min(10, q.syllableCount || 2));
      var rawParts = saved ? String(saved).split('-') : [];
      var parts = []; for (var i = 0; i < count; i++) parts.push(rawParts[i] || '');
      var activeIdx = 0;
      var wrap = el('div', {class: 'pri-syllable', style: 'display:flex;flex-direction:column;gap:8px'});
      wrap.appendChild(el('b', {style: 'font-size:15px', text: 'هر بخش یا صدا را در کادر خودش بنویس:'}));
      var boxesRow = el('div', {class: 'row', style: 'gap:6px;flex-wrap:wrap;align-items:center'});
      var inputs = [];
      function emit() { set(parts.map(function (x) { return String(x || '').replace(/-/g, '').trim(); }).join('-')); }
      parts.forEach(function (val, idx) {
        if (idx > 0) boxesRow.appendChild(el('b', {style: 'font-size:20px;padding:0 2px', text: ' - '}));
        var inp = el('input', {type: 'text', value: val, placeholder: 'بخش ' + fa(idx + 1), style: 'width:92px;text-align:center;font-size:18px;font-weight:bold;border:2px solid #0E7490;border-radius:10px;padding:6px'});
        inp.addEventListener('focus', function () { activeIdx = idx; });
        inp.addEventListener('input', function () { activeIdx = idx; parts[idx] = inp.value; emit(); });
        inputs.push(inp);
        boxesRow.appendChild(inp);
      });
      wrap.appendChild(boxesRow);
      var signsRow = el('div', {class: 'row', style: 'gap:6px;flex-wrap:wrap'});
      ['ـَ', 'ـِ', 'ـُ', 'ـّ', 'ا', 'آ', 'ای', 'او', 'ی', 'و'].forEach(function (sign) {
        var ch = sign.replace(/^ـ/, '');
        signsRow.appendChild(el('button', {type: 'button', class: 'btn soft sm', text: sign, onclick: function () {
          var idx = Math.max(0, Math.min(count - 1, activeIdx));
          parts[idx] = (parts[idx] || '') + ch;
          if (inputs[idx]) { inputs[idx].value = parts[idx]; inputs[idx].focus(); }
          emit();
        }}));
      });
      wrap.appendChild(signsRow);
      return wrap;
    }
    /* V314.0 — دسته‌بندی در سبد: انتخاب سبد مناسب برای هر مورد با لمس */
    function bucketAnswer(q, saved, set) {
      var buckets = q.buckets || [], items = q.bucketItems || [];
      var assigned = {};
      if (saved) {
        String(saved).split('|').forEach(function (seg) {
          var ci = seg.indexOf(':');
          if (ci > 0) {
            var b = seg.slice(0, ci).trim();
            seg.slice(ci + 1).split('+').map(function (x) { return x.trim(); }).filter(Boolean).forEach(function (it) { assigned[it] = b; });
          }
        });
      }
      var wrap = el('div', {class: 'pri-bucket', style: 'display:flex;flex-direction:column;gap:8px'});
      wrap.appendChild(el('b', {style: 'font-size:15px', text: 'برای هر مورد، سبد درست را انتخاب کن:'}));
      var listBox = el('div', {style: 'display:flex;flex-direction:column;gap:8px'});
      wrap.appendChild(listBox);
      function emit() {
        var out = buckets.map(function (b) {
          var chosen = items.filter(function (it) { return assigned[it] === b; }).map(function (x) { return x.trim(); }).sort();
          return b + ':' + chosen.join('+');
        }).join('|');
        set(out);
      }
      function paint() {
        listBox.innerHTML = '';
        items.forEach(function (item) {
          var cur = assigned[item] || '';
          var bRow = el('div', {class: 'row', style: 'gap:6px;flex-wrap:wrap;margin-top:6px'});
          buckets.forEach(function (b) {
            var on = cur === b;
            bRow.appendChild(el('button', {type: 'button', class: 'btn sm ' + (on ? '' : 'light'), text: '🧺 ' + b + (on ? ' ✓' : ''), onclick: function () {
              if (on) delete assigned[item]; else assigned[item] = b;
              emit(); paint();
            }}));
          });
          listBox.appendChild(el('div', {class: 'card', style: 'padding:10px;margin:0'}, [el('b', {style: 'font-size:16px', text: item}), bRow]));
        });
      }
      paint();
      return wrap;
    }
    /* پد عددی بزرگ ۰–۹ برای ابتدایی؛ کادر ورودی هم کار می‌کند */
    function numericBigPad(q, a, set) {
      var wrap = el('div', {class: 'st-numpad'});
      var inp = el('input', {type: 'text', class: 'st-input', inputmode: 'decimal', placeholder: 'پاسخ عددی', value: a != null ? String(a) : '', style: 'direction:ltr;text-align:center;font-size:26px;font-weight:700;width:100%'});
      inp.addEventListener('input', function () { var v = en(inp.value).replace(/[^0-9.\-]/g, ''); inp.value = v; set(v); });
      wrap.appendChild(inp);
      var grid = el('div', {style: 'display:grid;grid-template-columns:repeat(3,1fr);gap:8px;margin-top:10px;max-width:420px'});
      ['1', '2', '3', '4', '5', '6', '7', '8', '9', '.', '0', '⌫'].forEach(function (k) {
        grid.appendChild(el('button', {type: 'button', class: 'btn light', style: 'font-size:26px;font-weight:700;min-height:68px;border-radius:14px', text: k === '⌫' ? '⌫ پاک' : (k === '.' ? '.' : fa(k)), 'aria-label': k === '⌫' ? 'پاک کردن' : 'کلید ' + k, onclick: function () {
          var st = inp.selectionStart == null ? inp.value.length : inp.selectionStart, end = inp.selectionEnd == null ? st : inp.selectionEnd;
          if (k === '⌫') { if (st === end && st > 0) st -= 1; inp.setRangeText('', st, end, 'end'); }
          else if (k === '.' && inp.value.indexOf('.') >= 0) { inp.focus(); return; }
          else inp.setRangeText(k, st, end, 'end');
          inp.dispatchEvent(new Event('input')); inp.focus();
        }}));
      });
      wrap.appendChild(grid);
      return wrap;
    }
    function proofAnswer(q, saved, set) {
      var data={};try{var parsed=JSON.parse(String(saved||'{}'));data=parsed.parts||{};}catch(_){data={};}
      var wrap=el('div',{class:'st-proof'});
      if(q.proofAssumptions)wrap.appendChild(el('div',{class:'card',style:'padding:9px;margin:6px 0'},[el('b',{text:'فرض‌ها / داده‌ها'}),el('div',{text:q.proofAssumptions})]));
      if(q.proofGoal)wrap.appendChild(el('div',{class:'card',style:'padding:9px;margin:6px 0'},[el('b',{text:'خواسته'}),el('div',{text:q.proofGoal})]));
      function save(){set(JSON.stringify({parts:data}));}
      (q.assessmentParts||[]).forEach(function(part,pi){
        var cur=data[part.id]||{answer:'',reason:''};
        var card=el('div',{class:'card',style:'padding:9px;margin:7px 0'});
        card.appendChild(el('b',{text:'گام '+(part.label||fa(pi+1))+': '+(part.prompt||'')}));
        var answer=el('textarea',{class:'st-input',rows:2,placeholder:'نتیجه / گزارهٔ این گام'});answer.value=cur.answer||'';answer.addEventListener('input',function(){cur.answer=answer.value;data[part.id]=cur;save();});card.appendChild(answer);
        if(part.requiresReason){var reason=el('textarea',{class:'st-input',rows:2,placeholder:part.reasonPrompt||'دلیل این گام'});reason.value=cur.reason||'';reason.addEventListener('input',function(){cur.reason=reason.value;data[part.id]=cur;save();});card.appendChild(reason);}
        if(part.carriesForward&&pi>0)card.appendChild(el('small',{class:'muted',text:'می‌توانید از نتیجهٔ گام قبلی استفاده کنید.'}));
        wrap.appendChild(card);
      });
      return wrap;
    }

    function constructionAnswer(q, saved, set) {
      var labels=(q.constructionLabels||[]).slice(0,8), points={}, activeLabel=null;
      try { points=JSON.parse(String(saved||'{}'))||{}; } catch (_) { points={}; }
      var wrap=el('div',{class:'st-construct'});
      wrap.appendChild(el('p',{class:'st-hint',text:q.constructionPrompt||'نقاط خواسته‌شده را روی صفحهٔ مختصات مشخص کنید.'}));
      var canvas=el('canvas',{width:640,height:320,style:'width:100%;height:280px;border:1px solid #9aa5b1;border-radius:8px;touch-action:manipulation;background:#fff'});
      var ctx=canvas.getContext('2d'), m=28, w=canvas.width-2*m, h=canvas.height-2*m;
      function px(x){return m+(Number(x)+5)/10*w;} function py(y){return m+(5-Number(y))/10*h;}
      ctx.clearRect(0,0,canvas.width,canvas.height);ctx.strokeStyle='#d9dee6';ctx.lineWidth=1;
      for(var i=-5;i<=5;i++){ctx.beginPath();ctx.moveTo(px(i),m);ctx.lineTo(px(i),m+h);ctx.stroke();ctx.beginPath();ctx.moveTo(m,py(i));ctx.lineTo(m+w,py(i));ctx.stroke();}
      ctx.strokeStyle='#263445';ctx.lineWidth=2;ctx.beginPath();ctx.moveTo(px(0),m);ctx.lineTo(px(0),m+h);ctx.stroke();ctx.beginPath();ctx.moveTo(m,py(0));ctx.lineTo(m+w,py(0));ctx.stroke();
      labels.forEach(function(label){var p=points[label];if(!p)return;ctx.fillStyle='#1e53aa';ctx.beginPath();ctx.arc(px(p.x),py(p.y),7,0,Math.PI*2);ctx.fill();ctx.font='bold 17px sans-serif';ctx.fillText(label,px(p.x)+9,py(p.y)-8);});
      canvas.addEventListener('pointerdown',function(ev){var r=canvas.getBoundingClientRect();var sx=(ev.clientX-r.left)*(canvas.width/r.width),sy=(ev.clientY-r.top)*(canvas.height/r.height);var label=activeLabel||labels.find(function(x){return !Object.prototype.hasOwnProperty.call(points,x);})||labels[labels.length-1];if(!label)return;points=Object.assign({},points);points[label]={x:Math.max(-5,Math.min(5,((sx-m)/w)*10-5)),y:Math.max(-5,Math.min(5,5-((sy-m)/h)*10))};set(JSON.stringify(points));wrap.replaceWith(answerArea(q));});
      wrap.appendChild(canvas);wrap.appendChild(el('div',{class:'st-hint',text:'ثبت‌شده: '+fa(Object.keys(points).filter(function(k){return labels.indexOf(k)>=0;}).length)+' از '+fa(labels.length)}));
      wrap.appendChild(el('div',{class:'st-hint',text:'برای جابه‌جایی، برچسب نقطه را انتخاب و محل تازه را روی نمودار لمس کنید.'}));
      var chips=el('div',{class:'row'});labels.forEach(function(label){var p=points[label], chip=el('button',{type:'button',class:'chip'+(p?' brand':''),text:label+(p?' ('+fa(Number(p.x).toFixed(1))+', '+fa(Number(p.y).toFixed(1))+')':' —'),onclick:function(){activeLabel=activeLabel===label?null:label;Array.prototype.forEach.call(chips.children,function(x){x.classList.toggle('on',x.dataset.pointLabel===activeLabel);});}});chip.dataset.pointLabel=label;chips.appendChild(chip);});
      chips.appendChild(el('button',{class:'btn light sm',text:'پاک‌کردن',onclick:function(){set('{}');wrap.replaceWith(answerArea(q));}}));wrap.appendChild(chips);
      return wrap;
    }

    function answerArea(q) {
      var box = el('div', {class: 'st-answer'});
      var a = run.answers[q.id];
      function set(v) { if (v === null || v === undefined || v === '') delete run.answers[q.id]; else run.answers[q.id] = v; saveDraft(); drawNav(); }
      if (ex.primary && q.type === 'multiple' && q.hotspots && q.hotspots.some(hasZone) && q.images && q.images.length) {
        /* V305 — ابتدایی: انتخاب روی تصویر */
        box.appendChild(hotspotAnswer(q, a, function (oi) { set(oi); box.replaceWith(answerArea(q)); }));
      } else if (q.type === 'multiple' && ex.primary && isPictureOptionSet(q)) {
        /* V303 — ابتدایی: همهٔ گزینه‌ها تصویر/ایموجی → شبکهٔ ۲×۲ کارت‌های بزرگ */
        var grid = el('div', {class: 'st-picgrid'}), L3 = ['الف', 'ب', 'ج', 'د', 'ه', 'و'];
        q.options.forEach(function (o, di) {
          var oi = q.optionOriginalIndices[di] != null ? q.optionOriginalIndices[di] : di, on = a === oi, img = q.optionImages[di];
          var card = el('button', {type: 'button', class: 'st-pic' + (on ? ' on' : ''), 'aria-pressed': on ? 'true' : 'false', onclick: function () { set(oi); box.replaceWith(answerArea(q)); }}, [
            el('span', {class: 'st-piclabel', text: on ? '✓' : (L3[di] || fa(di + 1))}),
            img ? el('img', {src: img, alt: ''}) : el('span', {class: 'st-picemoji', text: o}),
            img && String(o).trim() ? el('span', {class: 'st-pictext', text: o}) : null
          ]);
          grid.appendChild(card);
        });
        box.appendChild(grid);
      } else if (q.type === 'multiple') {
        q.options.forEach(function (o, di) {
          var oi = q.optionOriginalIndices[di] != null ? q.optionOriginalIndices[di] : di;
          var r = el('input', {type: 'radio', name: 'st_' + q.id}); r.checked = a === oi; r.addEventListener('change', function () { set(oi); });
          var lab = el('label', {class: 'st-opt' + (a === oi ? ' on' : '')}, [r, el('span', {class: 'st-optlabel', text: 'ابجد'.split('')[di] ? ['الف', 'ب', 'ج', 'د', 'ه', 'و', 'ز', 'ح'][di] + ')' : fa(di + 1) + ')'}), el('span', {class: 'grow', html: esc(o)}), q.optionImages[di] ? el('img', {src: q.optionImages[di], class: 'thumb', onclick: function (e) { e.preventDefault(); lightbox(q.optionImages[di]); }}) : null]);
          lab.addEventListener('click', function () { setTimeout(function () { Array.prototype.forEach.call(box.querySelectorAll('.st-opt'), function (x) { x.classList.toggle('on', x.querySelector('input').checked); }); }, 0); });
          richHtml(o).then(function (h) { var g = lab.querySelector('.grow'); g.innerHTML = h; fitMath(g); });
          box.appendChild(lab);
        });
      } else if (q.type === 'truefalse') {
        box.appendChild(el('div', {class: 'row st-tf'}, [[true, ex.primary ? '✅ درست' : '✓ صحیح'], [false, ex.primary ? '❌ نادرست' : '✗ غلط']].map(function (o) { return el('button', {class: 'btn ' + (a === o[0] ? '' : 'light'), text: o[1], onclick: function () { set(o[0]); box.replaceWith(answerArea(q)); }}); })));
      } else if (ex.primary && q.type === 'fill' && q.mode === 'order') {
        /* V305 — ابتدایی: مرتب‌سازی با کشیدن؛ پاسخ = ترتیب فعلی با «,» */
        box.appendChild(orderAnswer(q, a, function (arr) { set(arr.join(',')); box.replaceWith(answerArea(q)); }));
      } else if (ex.primary && q.type === 'fill' && q.mode === 'syllable' && (q.syllableCount || 0) >= 2) {
        /* V314.0 — بخش‌کردن و صداکشی */
        box.appendChild(syllableAnswer(q, a, set));
      } else if (ex.primary && q.type === 'fill' && q.mode === 'bucket' && q.buckets && q.buckets.length && q.bucketItems && q.bucketItems.length) {
        /* V314.0 — دسته‌بندی در سبد */
        box.appendChild(bucketAnswer(q, a, set));
      } else if (ex.primary && q.type === 'numeric') {
        /* V305 — ابتدایی: کادر پاسخ + پد عددی بزرگ */
        box.appendChild(numericBigPad(q, a, set));
      } else if (q.type === 'fill' || q.type === 'numeric') {
        var i = el('input', {type: 'text', class: 'st-input', placeholder: q.type === 'numeric' ? 'پاسخ عددی' : 'پاسخ جای خالی', value: a != null ? String(a) : '', style: (q.type === 'numeric' ? 'direction:ltr;text-align:left;' : '') + 'flex:1;min-width:0'});
        i.addEventListener('input', function () { var v = i.value; if (q.type === 'numeric') { v = en(v).replace(/[^0-9.\-]/g, ''); i.value = v; } set(v); });
        /* V264 — صفحه‌کلید ریاضی (آیتم ۱۲): عددی = رقم/ممیز/منفی؛ جای خالی = رقم + نمادها؛ درج در محل مکان‌نما */
        var pad = el('div', {class: 'math-pad', style: 'display:none'});
        var keys = ['7', '8', '9', '4', '5', '6', '1', '2', '3', '.', '0', '-'].concat(q.type === 'numeric' ? [] : ['+', '−', '×', '÷', '=', '≠', '<', '>', '≤', '≥', '(', ')', '√', 'π', '²', '³', '^', '/', '%', '°', '∞', 'θ', 'α', 'β', 'x', 'y']).concat(['⌫']);
        keys.forEach(function (k) { pad.appendChild(el('button', {type: 'button', class: 'math-key', text: k, 'aria-label': k === '⌫' ? 'پاک کردن' : 'کلید ' + k, onclick: function () {
          var st = i.selectionStart == null ? i.value.length : i.selectionStart, en2 = i.selectionEnd == null ? st : i.selectionEnd;
          if (k === '⌫') { if (st === en2 && st > 0) st -= 1; i.setRangeText('', st, en2, 'end'); }
          else if (q.type === 'numeric' && ((k === '-' && i.value.length) || (k === '.' && i.value.indexOf('.') >= 0))) { i.focus(); return; }
          else i.setRangeText(k, st, en2, 'end');
          i.dispatchEvent(new Event('input')); i.focus();
        }})); });
        var padBtn = el('button', {type: 'button', class: 'btn light sm', text: '⌨ کلید ریاضی', 'aria-label': 'صفحه‌کلید ریاضی', onclick: function () { var on = pad.style.display === 'none'; pad.style.display = on ? 'flex' : 'none'; padBtn.classList.toggle('on', on); }});
        box.appendChild(el('div', {class: 'row', style: 'gap:8px;align-items:center'}, [i, padBtn]));
        box.appendChild(pad);
      } else if (q.type === 'matching') {
        var cur = (a && typeof a === 'object') ? a : {};
        /* V259.5 — موارد ستون راست با حرف (الف، ب…) تا با شمارهٔ موارد ستون چپ اشتباه نشود (مثل اپ) */
        var ABm = ['الف', 'ب', 'ج', 'د', 'ه', 'و', 'ز', 'ح', 'ط', 'ی'];
        box.appendChild(el('div', {class: 'st-match-right'}, q.rightItems.map(function (r, di) { var d = el('div', {class: 'st-mr'}, [el('b', {text: (ABm[di] || fa(di + 1)) + ') '}), el('span', {html: esc(r)}), q.rightImages[di] ? el('img', {src: q.rightImages[di], class: 'thumb'}) : null]); richHtml(r).then(function (h) { d.querySelector('span').innerHTML = h; }); return d; })));
        q.leftItems.forEach(function (l, li) {
          var row = el('div', {class: 'st-ml'}, [el('span', {class: 'grow', html: '<b>' + fa(li + 1) + '.</b> ' + esc(l)}), q.leftImages[li] ? el('img', {src: q.leftImages[li], class: 'thumb'}) : null,
            el('div', {class: 'st-chips'}, q.rightItems.map(function (_, di) { var oi = q.rightOriginalIndices[di] != null ? q.rightOriginalIndices[di] : di; return el('button', {class: 'chip' + (cur[li] === oi ? ' brand' : ''), text: ABm[di] || fa(di + 1), onclick: function () { cur = Object.assign({}, cur); cur[li] = oi; set(cur); box.replaceWith(answerArea(q)); }}); }))]);
          richHtml(l).then(function (h) { var g = row.querySelector('.grow'); g.innerHTML = '<b>' + fa(li + 1) + '.</b> ' + h; fitMath(g); });
          box.appendChild(row);
        });
      } else if (q.type === 'essay' && q.mode === 'construct' && q.constructionLabels && q.constructionLabels.length) {
        box.appendChild(constructionAnswer(q, a, set));
      } else if (q.type === 'essay' && q.mode === 'proof' && q.assessmentParts && q.assessmentParts.length) {
        box.appendChild(proofAnswer(q,a,set));
      } else if (q.type === 'essay' && ((q.assessmentParts && q.assessmentParts.length) || q.mode === 'steps' || q.mode === 'proof')) {
        var prompts = q.assessmentParts && q.assessmentParts.length ? q.assessmentParts.map(function(p){return [p.label,p.prompt].filter(Boolean).join(' · ')||'زیربخش';}) : (q.solutionSteps && q.solutionSteps.length ? q.solutionSteps : ['گام اول','گام دوم','پاسخ نهایی']);
        var vals = String(a || '').split('\n');
        prompts.forEach(function (pr, si) {
          var st = el('textarea', {class:'st-input', rows:2, placeholder:pr || ('گام '+fa(si+1))});
          st.value = vals[si] || '';
          st.addEventListener('input', function () { vals[si] = st.value.replace(/\n/g,' '); set(vals.join('\n')); });
          box.appendChild(st);
          if(q.assessmentParts&&q.assessmentParts[si]&&q.assessmentParts[si].carriesForward)box.appendChild(el('small',{class:'muted',text:'در صورت نیاز، نتیجهٔ بخش قبلی را اینجا به‌کار ببر.'}));
        });
      } else if (ex.primary && q.type === 'essay' && q.mode === 'voice') {
        /* V309 — پاسخ صوتی: ضبط با سقف مدت معلم */
        box.appendChild(voiceAnswerBlock(q, box));
      } else if (q.type === 'essay' && (q.mode === 'draw' || q.mode === 'construct')) {
        /* V306 — ابتدایی: پاسخ با تخته؛ کادر متنی نیست */
        box.appendChild(el('p', {class: 'st-hint', text: 'پاسخ را روی تخته بکشید و ذخیره کنید؛ با دکمهٔ تخته زیر باز کنید.'}));
      } else {
        var ta = el('textarea', {class: 'st-input', rows: 6, placeholder: 'پاسخ تشریحی خود را بنویسید…'}); ta.value = a != null ? String(a) : '';
        ta.addEventListener('input', function () { set(ta.value); });
        box.appendChild(ta);
      }
      /* تصویر پاسخ / تخته */
      /* V306 — پاسخ با تخته: تخته همیشه (بزرگ) نشان داده می‌شود */
      var drawMode = q.type === 'essay' && (q.mode === 'draw' || q.mode === 'construct');
      var whiteboardOnly = (q.pres.allowAnswerGraph || drawMode) && q.maxAnswerImages <= 0;
      if (q.maxAnswerImages > 0 || q.pres.allowAnswerGraph || drawMode) {
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
    /* V309 — پاسخ صوتی: ضبط با MediaRecorder؛ پایان خودکار در سقف مدت. فایل به‌صورت dataURL در پیش‌نویس می‌ماند و هنگام ارسال آپلود می‌شود */
    function voiceAnswerBlock(q, box) {
      var wrap = el('div', {class: 'st-voice'});
      var max = Math.max(10, Math.min(300, Number(q.voiceMaxSeconds) || 60));
      var cur = run.images[q.id] || [];
      wrap.appendChild(el('p', {class: 'st-hint', text: 'پاسخ صوتی — سقف ضبط: ' + fa(max) + ' ثانیه'}));
      if (cur.length) {
        wrap.appendChild(el('audio', {src: cur[0], controls: 'controls', preload: 'metadata'}));
        wrap.appendChild(el('button', {class: 'btn light sm', text: '🗑 حذف و ضبط دوباره', onclick: function () { run.images[q.id] = []; saveDraft(); box.replaceWith(answerArea(q)); }}));
        return wrap;
      }
      var status = el('div', {class: 'muted', style: 'font-size:13px', text: ''});
      var rec = el('button', {class: 'btn soft sm', text: '🎙 شروع ضبط'});
      var mediaRec = null, chunks = [], timer = null, startedAt = 0;
      function stopNow() { if (mediaRec && mediaRec.state !== 'inactive') mediaRec.stop(); }
      rec.addEventListener('click', async function () {
        if (mediaRec) { stopNow(); return; }
        if (!navigator.mediaDevices || !window.MediaRecorder) { S.toast('مرورگر شما ضبط صدا را پشتیبانی نمی‌کند.', 'err'); return; }
        try {
          var stream = await navigator.mediaDevices.getUserMedia({audio: true});
          var type = ['audio/mp4', 'audio/webm;codecs=opus', 'audio/webm', 'audio/ogg;codecs=opus'].filter(function (t) { return MediaRecorder.isTypeSupported && MediaRecorder.isTypeSupported(t); })[0] || '';
          chunks = [];
          var opts = type ? {mimeType: type, audioBitsPerSecond: 32000} : {audioBitsPerSecond: 32000};
          var active = new MediaRecorder(stream, opts);
          mediaRec = active;
          active.ondataavailable = function (e) { if (e.data && e.data.size) chunks.push(e.data); };
          active.onstop = function () {
            clearInterval(timer); stream.getTracks().forEach(function (t) { t.stop(); });
            var blob = new Blob(chunks, {type: active.mimeType || type || 'audio/webm'});
            mediaRec = null;
            if (blob.size === 0) { S.toast('ضبط خیلی کوتاه بود؛ دوباره ضبط کنید.', 'err'); box.replaceWith(answerArea(q)); return; }
            if (blob.size > 3 * 1024 * 1024) { S.toast('حجم پاسخ صوتی بیش از حد است؛ مدت ضبط را کوتاه‌تر کنید.', 'err'); box.replaceWith(answerArea(q)); return; }
            var reader = new FileReader();
            reader.onload = function () { run.images[q.id] = [String(reader.result)]; saveDraft(); box.replaceWith(answerArea(q)); };
            reader.readAsDataURL(blob);
          };
          active.start(1000); startedAt = Date.now(); rec.textContent = '⏹ پایان ضبط';
          timer = setInterval(function () {
            var passed = Math.floor((Date.now() - startedAt) / 1000);
            status.textContent = '● در حال ضبط: ' + fa(Math.min(passed, max)) + ' از ' + fa(max) + ' ثانیه';
            if (passed >= max) stopNow();
          }, 250);
        } catch (err) { mediaRec = null; S.toast('اجازهٔ میکروفون داده نشد یا ضبط شروع نشد.', 'err'); }
      });
      wrap.appendChild(rec); wrap.appendChild(status);
      return wrap;
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
      var now = Date.now(), q = ex.questions[run.index], title = q && String(q.sectionTitle || '').trim(), sectionMinutes = title ? Number(ex.sectionDurations && ex.sectionDurations[title]) || 0 : 0;
      var sectionRem = null;
      if (title && sectionMinutes > 0 && !run.expiredSections[title]) {
        if (!run.sectionDeadlines[title]) { run.sectionDeadlines[title] = now + Math.min(1440, sectionMinutes) * 60000; saveDraft(); }
        sectionRem = Math.max(0, run.sectionDeadlines[title] - now);
        if (sectionRem <= 0) {
          run.expiredSections[title] = true; saveDraft();
          toast('زمان بخش «' + title + '» تمام شد؛ پاسخ‌های ثبت‌شده حفظ شدند.', 'err');
          var next = ex.questions.findIndex(function(x,i){return i>run.index && String(x.sectionTitle||'').trim()!==title && !(x.sectionTitle && run.expiredSections[String(x.sectionTitle).trim()]);});
          if (next < 0) next = ex.questions.findIndex(function(x){return !(x.sectionTitle && run.expiredSections[String(x.sectionTitle).trim()]);});
          if (next < 0) { submit(c, true); return; }
          goTo(next); drawAll(); tick(); return;
        }
      }
      var globalRem = ex.deadline == null ? null : Math.max(0, ex.deadline - now), rem = globalRem == null ? sectionRem : (sectionRem == null ? globalRem : Math.min(globalRem, sectionRem));
      if (rem == null) { timerEl.textContent = '⏱ بدون محدودیت'; return; }
      var m = Math.floor(rem / 60000), s = Math.floor(rem % 60000 / 1000);
      timerEl.textContent = (sectionRem != null ? '⏱ بخش ' : '⏱ ') + fa((m < 10 ? '0' : '') + m + ':' + (s < 10 ? '0' : '') + s);
      timerEl.classList.toggle('danger', rem < 120000);
      if (globalRem != null && globalRem <= 0) { clearInterval(run.timer); toast('زمان آزمون تمام شد؛ پاسخ‌ها ارسال می‌شود.', 'err'); submit(c, true); }
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
  function audioExtOf(t) { t = String(t || '').toLowerCase(); return t.indexOf('mp4') >= 0 ? 'm4a' : t.indexOf('ogg') >= 0 ? 'ogg' : 'webm'; }
  async function uploadAnswerImage(dataUrl, examId, questionId) {
    if (/^https:\/\//i.test(dataUrl)) return dataUrl;
    var blob = await (await fetch(dataUrl)).blob();
    var isAudio = /^data:audio\//i.test(dataUrl); /* V309 — پاسخ صوتی با پسوند و نوع خودش */
    var path = 'answers/' + S.user().id + '/' + examId + '/' + questionId + '/' + uuid() + (isAudio ? '.' + audioExtOf(blob.type) : '.webp');
    var sess = S.session();
    var res = await fetch(S.config.url + '/storage/v1/object/' + BUCKET + '/' + path, {method: 'POST', headers: {'apikey': S.config.anon, 'Authorization': 'Bearer ' + (sess ? sess.access_token : S.config.anon), 'Content-Type': (blob.type || 'image/webp').split(';')[0], 'x-upsert': 'false'}, body: blob});
    if (!res.ok) throw new Error('آپلود تصویر پاسخ ناموفق بود: ' + (await res.text()).slice(0, 120));
    return S.config.url + '/storage/v1/object/public/' + BUCKET + '/' + path;
  }
  function practiceGradeQuestion(q, key, r) {
    var s = Number(q.score) || 0, k = key || {}, L = ['الف', 'ب', 'ج', 'د', 'ه', 'و', 'ز', 'ح', 'ط', 'ی'];
    var origOpts = (run.exam.rawQuestions && run.exam.rawQuestions[q.originalIndex] && run.exam.rawQuestions[q.originalIndex].options) || q.options || [];
    var hasAns = r != null && r !== '' && !(typeof r === 'object' && !Object.keys(r).length);
    var userStr = 'بدون پاسخ', correctStr = '—', earned = null;
    if (q.type === 'multiple') {
      var ci = k.correctOption != null ? Number(k.correctOption) : (k.correctIndex != null ? Number(k.correctIndex) : null);
      if (ci != null) correctStr = (L[ci] || fa(ci + 1)) + ') ' + (origOpts[ci] != null ? origOpts[ci] : '');
      if (hasAns) { var ui = Number(r); userStr = (L[ui] || fa(ui + 1)) + ') ' + (origOpts[ui] != null ? origOpts[ui] : ''); earned = ci != null ? (ui === ci ? s : 0) : null; }
      else if (ci != null) earned = 0;
    } else if (q.type === 'truefalse') {
      var ca = k.correctAnswer === true || k.correctAnswer === 'true';
      correctStr = ca ? 'صحیح' : 'غلط';
      if (hasAns) { var ua = r === true || r === 'true'; userStr = ua ? 'صحیح' : 'غلط'; earned = ua === ca ? s : 0; }
      else earned = 0;
    } else if (q.type === 'fill') {
      var acc = Array.isArray(k.accept) ? k.accept : [];
      correctStr = acc.join(' یا ') || '—';
      if (hasAns) {
        userStr = String(r);
        var norm = acc.map(function (x) { return k.caseSensitive ? String(x).trim() : String(x).trim().toLowerCase(); });
        var uv = k.caseSensitive ? String(r).trim() : String(r).trim().toLowerCase();
        earned = norm.length ? (norm.indexOf(uv) >= 0 ? s : 0) : null;
      } else if (acc.length) earned = 0;
    } else if (q.type === 'numeric') {
      var na = parseFloat(k.answer), tol = parseFloat(k.tolerance) || 0;
      correctStr = k.answer != null ? String(k.answer) + (tol ? ' ± ' + tol : '') : '—';
      if (hasAns) { userStr = String(r); var ux = parseFloat(en(String(r))); earned = isFinite(na) ? (isFinite(ux) && Math.abs(ux - na) <= tol ? s : 0) : null; }
      else if (isFinite(na)) earned = 0;
    } else if (q.type === 'matching') {
      var ma = k.matchAnswer || {}, ks = Object.keys(ma);
      correctStr = ks.sort(function (a, b) { return a - b; }).map(function (l) { return fa(Number(l) + 1) + ' ← ' + (L[Number(ma[l])] || fa(Number(ma[l]) + 1)); }).join('، ') || '—';
      if (hasAns && typeof r === 'object') {
        userStr = Object.keys(r).sort(function (a, b) { return a - b; }).map(function (l) { return fa(Number(l) + 1) + ' ← ' + (L[Number(r[l])] || fa(Number(r[l]) + 1)); }).join('، ');
        if (ks.length) { var ok = 0; ks.forEach(function (l) { if (Number(r[l]) === Number(ma[l])) ok++; }); earned = Math.round(s * ok / ks.length * 100) / 100; }
      } else if (ks.length) earned = 0;
    } else {
      correctStr = 'تصحیح تشریحی توسط معلم';
      if (hasAns) userStr = String(r);
    }
    return {earned: earned, score: s, userStr: userStr, correctStr: correctStr, hasAns: hasAns};
  }
  function renderPracticeResult(c, ex, auto) {
    run.finished = true; stopAll();
    if (window.SiteMobile && window.SiteMobile.active()) { window.SiteMobile.paint(); c = document.getElementById('content'); }
    c.innerHTML = '';
    var keys = ex.answerKeys || {};
    var totalScore = 0, earnedScore = 0, okCnt = 0, wrongCnt = 0, essayCnt = 0;
    var rows = ex.questions.map(function (q, idx) {
      var res = practiceGradeQuestion(q, keys[q.originalIndex], run.answers[q.id]);
      totalScore += res.score;
      if (res.earned != null) { earnedScore += res.earned; if (res.earned >= res.score && res.score > 0) okCnt++; else wrongCnt++; }
      else essayCnt++;
      var qBox = el('div', {class: 'card', style: 'margin-top:10px;text-align:right'}, [
        el('div', {class: 'row', style: 'margin-bottom:6px;gap:8px;align-items:center'}, [
          el('span', {class: 'chip brand', text: 'سؤال ' + fa(idx + 1)}),
          el('span', {class: 'chip ' + (res.earned == null ? '' : (res.earned >= res.score && res.score > 0 ? 'ok' : 'warn')), text: res.earned == null ? 'تشریحی (' + fa(S.fmtScore(res.score)) + ' نمره)' : fa(S.fmtScore(res.earned)) + ' از ' + fa(S.fmtScore(res.score))})
        ]),
        (function () { var d = el('div', {style: 'margin-bottom:8px', html: esc(q.text).replace(/\n/g, '<br>')}); richHtml(q.text).then(function (h) { d.innerHTML = h; fitMath(d); }); return d; })(),
        el('div', {class: 'muted', style: 'font-size:13px'}, [el('b', {text: 'پاسخ شما: '}), el('span', {text: res.userStr})]),
        el('div', {class: 'muted', style: 'font-size:13px;margin-top:4px'}, [el('b', {text: 'پاسخ صحیح: '}), el('span', {text: res.correctStr})])
      ]);
      return qBox;
    });
    var summary = el('div', {class: 'card', style: 'max-width:720px;margin:0 auto;text-align:center'}, [
      el('div', {style: 'font-size:46px', text: '🎯'}),
      el('h3', {text: 'نتیجهٔ آزمایشی آزمون: ' + ex.title}),
      el('div', {style: 'font-size:20px;font-weight:700;margin:8px 0'}, [el('span', {text: 'نمرهٔ کسب‌شده: ' + fa(S.fmtScore(Math.round(earnedScore * 100) / 100)) + ' از ' + fa(S.fmtScore(Math.round(totalScore * 100) / 100))})]),
      el('div', {class: 'row', style: 'justify-content:center;gap:8px;margin:8px 0'}, [
        el('span', {class: 'chip ok', text: 'صحیح: ' + fa(okCnt)}),
        el('span', {class: 'chip warn', text: 'نادرست/بی‌پاسخ: ' + fa(wrongCnt)}),
        essayCnt ? el('span', {class: 'chip', text: 'تشریحی: ' + fa(essayCnt)}) : null
      ]),
      el('div', {class: 'alert info', style: 'margin-top:10px;text-align:right', text: 'این آزمون در حالت آزمایشی معلم انجام شد و هیچ نمره یا تلاشی در لیست دانش‌آموزان و آمار کلاس ثبت نشده است.'}),
      auto ? el('p', {class: 'muted', text: 'زمان آزمون به پایان رسید.'}) : null,
      el('div', {class: 'row', style: 'justify-content:center;margin-top:12px;gap:8px'}, [
        el('button', {class: 'btn', text: '🔄 شرکت مجدد در همین آزمون', onclick: async function () { try { var fresh = await joinTeacherPracticeById(ex.id); startExam(c, fresh, false); } catch (e) { run = null; page(c); } }}),
        el('button', {class: 'btn light', text: 'آزمون دیگر', onclick: function () { run = null; page(c); }}),
        el('button', {class: 'btn light', text: 'بازگشت به کارت‌ها', onclick: function () { run = null; S.go('cards'); }})
      ])
    ]);
    var wrap = el('div', {style: 'max-width:720px;margin:0 auto'}, [summary].concat(rows));
    c.appendChild(wrap);
  }
  async function submit(c, auto) {
    if (!run || run.submitting || run.finished) return;
    if (run.practice) { renderPracticeResult(c, run.exam, auto); return; }
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
      c.appendChild(el('div', {class: 'card', style: 'max-width:520px;margin:0 auto;text-align:center'}, [el('div', {style: 'font-size:48px', text: ex.primary ? '🌟' : '✅'}), el('h3', {text: ex.primary ? 'آفرین! پاسخ‌هایت ثبت شد' : 'پاسخ‌های شما ثبت شد'}), raw && raw.receipt ? el('p', {class: 'muted', text: 'کد رهگیری: ' + raw.receipt}) : null,
        auto ? el('p', {class: 'muted', text: 'زمان آزمون به پایان رسید و پاسخ‌ها به‌صورت خودکار ارسال شد.'}) : null,
        el('div', {class: 'row', style: 'justify-content:center;margin-top:12px'}, [el('button', {class: 'btn', text: 'کارنامه', onclick: function () { run = null; S.go('grades'); }}), el('button', {class: 'btn light', text: 'آزمون دیگر', onclick: function () { run = null; S.go('join'); }})])]));
    } catch (e) {
      ov.remove();
      /* V231 — قطع اینترنت (یا پایان زمان بدون اینترنت): بستهٔ کامل صف می‌شود؛ آزمون برای دانش‌آموز تمام‌شده است */
      if ((isNetErr(e) || auto) && responses) {
        var op = uuid();
        var ok = queuePut({operation: op, owner: S.user().id, exam: ex.id, title: ex.title, code: ex.code, responses: responses, images: run.images || {}, report: monitorReport(true), meta: {native: true, queued: true, created_at_epoch_ms: Date.now(), web: true}, createdAt: Date.now(), attempts: 0});
        if (ok) {
          run.finished = true; stopAll(); activeSet(null);
          c.innerHTML = '';
          c.appendChild(el('div', {class: 'card', style: 'max-width:520px;margin:0 auto;text-align:center'}, [el('div', {style: 'font-size:48px', text: '📦'}), el('h3', {text: 'پاسخ‌ها در صف ارسال قرار گرفت'}),
            el('p', {class: 'muted', text: 'اینترنت در دسترس نبود. پاسخ‌های شما روی همین مرورگر ذخیره شد و به‌محض اتصال، خودکار ارسال می‌شود. این صفحه یا سایت را از مرورگر پاک نکنید.'}),
            auto ? el('p', {class: 'muted', text: 'زمان آزمون به پایان رسید.'}) : null,
            el('div', {class: 'row', style: 'justify-content:center;margin-top:12px'}, [el('button', {class: 'btn', text: 'تلاش برای ارسال اکنون', onclick: function () { flushQueue(true); }}), el('button', {class: 'btn light', text: 'بازگشت', onclick: function () { run = null; S.go('exam'); }})])]));
          return;
        }
      }
      run.submitting = false;
      toast(errMsg(e), 'err');
      c.innerHTML = ''; c.appendChild(examUI(c));
      c.insertBefore(el('div', {class: 'alert error', text: 'ارسال ناموفق بود: ' + errMsg(e) + ' — پاسخ‌ها روی مرورگر ذخیره‌اند؛ دوباره تلاش کنید.'}), c.firstChild);
    }
  }

  /* ================================================================ تختهٔ سفید (رسم آزاد → تصویر پاسخ؛ چند صفحه؛ زمینه: خالی/شطرنجی/محور) */
  /* V206 — opts (اختیاری؛ سازندهٔ معلم): {title, doneLabel, prev:[dataURL]} — بدون opts همان تختهٔ دانش‌آموز است */
  /* V264 — تشخیص شکل دست‌آزاد (آینهٔ core/geometry/ShapeRecognizer.kt): {kind:'line'|'circle'|'triangle'|'rect', a, b} یا null */
  function recognizeShape(pts, minSize) {
    minSize = minSize || 24;
    if (!pts || pts.length < 8) return null;
    function dist(p, q) { return Math.hypot(p.x - q.x, p.y - q.y); }
    function p2s(p, a, b) { var vx = b.x - a.x, vy = b.y - a.y, l2 = vx * vx + vy * vy; if (!l2) return dist(p, a); var t = Math.max(0, Math.min(1, ((p.x - a.x) * vx + (p.y - a.y) * vy) / l2)); return dist(p, {x: a.x + t * vx, y: a.y + t * vy}); }
    function simplify(list, eps) { if (list.length < 3) return list; var a = list[0], b = list[list.length - 1], md = 0, idx = 0; for (var i = 1; i < list.length - 1; i++) { var d = p2s(list[i], a, b); if (d > md) { md = d; idx = i; } } if (md > eps) { var l = simplify(list.slice(0, idx + 1), eps), r = simplify(list.slice(idx), eps); return l.slice(0, -1).concat(r); } return [a, b]; }
    var f = pts[0], l = pts[pts.length - 1], minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity, len = 0;
    pts.forEach(function (p, i) { minX = Math.min(minX, p.x); minY = Math.min(minY, p.y); maxX = Math.max(maxX, p.x); maxY = Math.max(maxY, p.y); if (i) len += dist(pts[i - 1], p); });
    var w = maxX - minX, h = maxY - minY, size = Math.max(w, h);
    if (size < minSize || len <= 0) return null;
    var chord = dist(f, l);
    if (chord > 0.7 * len) { var maxDev = 0; pts.forEach(function (p) { maxDev = Math.max(maxDev, p2s(p, f, l)); }); if (maxDev < 0.08 * len + 2) return {kind: 'line', a: f, b: l}; }
    if (!(chord < 0.25 * len || chord < 0.3 * size)) return null;
    var tl = {x: minX, y: minY}, br = {x: maxX, y: maxY}, cx = (minX + maxX) / 2, cy = (minY + maxY) / 2, rx = Math.max(w / 2, 1), ry = Math.max(h / 2, 1);
    var radii = pts.map(function (p) { var dx = (p.x - cx) / rx, dy = (p.y - cy) / ry; return Math.sqrt(dx * dx + dy * dy); });
    var mean = radii.reduce(function (s, v) { return s + v; }, 0) / radii.length, std = Math.sqrt(radii.reduce(function (s, v) { return s + (v - mean) * (v - mean); }, 0) / radii.length);
    var sp = simplify(pts, 0.07 * size), n = sp.length;
    if (n >= 3 && (dist(sp[0], sp[n - 1]) < 0.001 || dist(sp[0], sp[n - 1]) < 0.35 * dist(sp[0], sp[1]))) n -= 1;
    if (std < 0.14 && mean >= 0.75 && mean <= 1.15 && n >= 5) return {kind: 'circle', a: tl, b: br};
    if (n === 3) return {kind: 'triangle', a: tl, b: br};
    if (n === 4) return {kind: 'rect', a: tl, b: br};
    return (std < 0.2 && mean >= 0.7 && mean <= 1.2) ? {kind: 'circle', a: tl, b: br} : null;
  }
  function openWhiteboard(q, onDone, opts) {
    opts = opts || null;
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
    function down(e) { e.preventDefault(); drawing = true; last = startPt = pos(e); push(); penPts = [last]; if (tool !== 'pen' && tool !== 'eraser') { snapshotImg = new Image(); snapshotImg.src = canvas.toDataURL(); } if (tool === 'text') { drawing = false; var at = last; S.promptDlg('متن روی تخته', '', 'متن', '').then(function (t) { if (t && t.trim()) { ctx.fillStyle = color; ctx.font = (size * 6 + 10) + 'px Vazirmatn, sans-serif'; ctx.direction = 'rtl'; ctx.fillText(t.trim(), at.x, at.y); } }); } }
    function move(e) {
      if (!drawing) return; e.preventDefault(); var p = pos(e);
      if (tool === 'pen') penPts.push(p);
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
    /* V264 — تشخیص شکل (آیتم ۱۲): همان الگوریتم ShapeRecognizer.kt اپ؛ وقتی «تشخیص شکل» روشن است ضربهٔ قلم پس از رها شدن به خط/دایره/مثلث/مستطیل تبدیل می‌شود */
    var autoShape = false, penPts = [];
    function up(e) {
      if (!drawing) return; drawing = false; snapshotImg = null;
      if (autoShape && tool === 'pen' && penPts.length >= 8) {
        var sh = recognizeShape(penPts); penPts = [];
        if (sh) { var base = undo[undo.length - 1]; if (!base) return; var im = new Image(); im.onload = function () { ctx.drawImage(im, 0, 0); drawRecognized(sh); }; im.src = base; }
      }
      penPts = [];
    }
    function drawRecognized(sh) {
      ctx.save(); ctx.strokeStyle = color; ctx.lineWidth = size; ctx.beginPath();
      if (sh.kind === 'line') { ctx.moveTo(sh.a.x, sh.a.y); ctx.lineTo(sh.b.x, sh.b.y); }
      else if (sh.kind === 'rect') ctx.rect(sh.a.x, sh.a.y, sh.b.x - sh.a.x, sh.b.y - sh.a.y);
      else if (sh.kind === 'circle') ctx.ellipse((sh.a.x + sh.b.x) / 2, (sh.a.y + sh.b.y) / 2, Math.max(1, (sh.b.x - sh.a.x) / 2), Math.max(1, (sh.b.y - sh.a.y) / 2), 0, 0, Math.PI * 2);
      else if (sh.kind === 'triangle') { ctx.moveTo((sh.a.x + sh.b.x) / 2, sh.a.y); ctx.lineTo(sh.b.x, sh.b.y); ctx.lineTo(sh.a.x, sh.b.y); ctx.closePath(); }
      ctx.stroke(); ctx.restore();
    }
    canvas.addEventListener('pointerdown', down); canvas.addEventListener('pointermove', move); window.addEventListener('pointerup', up);
    function savePage() { pages[cur].data = canvas.toDataURL('image/webp', 0.85); pages[cur].grid = grid; }
    function loadPage(i) { cur = i; grid = pages[i].grid || 'blank'; gridSel.value = grid; if (pages[i].data) { var im = new Image(); im.onload = function () { ctx.drawImage(im, 0, 0); }; im.src = pages[i].data; ctx.fillStyle = '#fff'; ctx.fillRect(0, 0, W, H); } else blank(); undo = []; status.textContent = 'صفحهٔ ' + fa(i + 1) + ' از ' + fa(pages.length); }
    var toolBtns = {};
    function tb(id, label, title) { var b = el('button', {class: 'wb-tool' + (tool === id ? ' on' : ''), text: label, title: title, onclick: function () { tool = id; Object.keys(toolBtns).forEach(function (k) { toolBtns[k].classList.toggle('on', k === id); }); }}); toolBtns[id] = b; return b; }
    var gridSel = el('select', {class: 'wb-sel', title: 'زمینه'}); [['blank', 'خالی'], ['lined', 'خط‌دار'], ['grid', 'شطرنجی'], ['line', 'محور اعداد'], ['quad', 'محور ۴ ناحیه'], ['first', 'ناحیهٔ اول'], ['polar', 'قطبی']].forEach(function (o) { gridSel.appendChild(el('option', {value: o[0], text: o[1]})); });
    gridSel.addEventListener('change', async function () { if (!(await S.confirmDlg('تغییر زمینه', 'تغییر زمینه، رسم‌های این صفحه را پاک می‌کند. ادامه؟', 'ادامه', true))) { gridSel.value = grid; return; } grid = gridSel.value; blank(); }); /* V250 — بدون confirm مرورگر */
    var colorIn = el('input', {type: 'color', value: color, class: 'wb-color', title: 'رنگ'}); colorIn.addEventListener('input', function () { color = colorIn.value; });
    var sizeIn = el('input', {type: 'range', min: 1, max: 12, value: size, class: 'wb-size', title: 'ضخامت'}); sizeIn.addEventListener('input', function () { size = Number(sizeIn.value); });
    var bar = el('div', {class: 'engine-bar wb-bar'}, [
      el('span', {text: opts && opts.title ? '🖍 ' + opts.title : '🖍 تختهٔ سفید — سؤال ' + fa(run.exam.questions.indexOf(q) + 1)}),
      tb('pen', '✏️', 'قلم'), tb('eraser', '🧽', 'پاک‌کن'), tb('line', '╱', 'خط'), tb('arrow', '➚', 'پیکان'), tb('rect', '▭', 'مستطیل'), tb('circle', '◯', 'دایره'), tb('text', 'T', 'متن'),
      (function () { var b = el('button', {class: 'wb-tool', text: '✨', title: 'تشخیص شکل: خط/دایره/مثلث/مستطیل دست‌آزاد به شکل منظم تبدیل می‌شود', 'aria-label': 'تشخیص شکل', onclick: function () { autoShape = !autoShape; b.classList.toggle('on', autoShape); if (autoShape && toolBtns.pen) toolBtns.pen.click(); }}); return b; })(), /* V264 */
      colorIn, sizeIn, gridSel,
      el('button', {class: 'wb-tool', text: '↶', title: 'برگرداندن', onclick: function () { var d = undo.pop(); if (!d) return; var im = new Image(); im.onload = function () { ctx.drawImage(im, 0, 0); }; im.src = d; }}),
      el('button', {class: 'wb-tool', text: '🗑', title: 'پاک کردن صفحه', onclick: async function () { if (await S.confirmDlg('پاک کردن صفحه', 'کل این صفحه پاک شود؟', 'پاک شود', true)) { push(); blank(); } }}),
      el('span', {class: 'grow'}), status,
      el('button', {class: 'btn light sm', text: '◀', title: 'صفحهٔ قبل', onclick: function () { if (cur > 0) { savePage(); loadPage(cur - 1); } }}),
      el('button', {class: 'btn light sm', text: '▶', title: 'صفحهٔ بعد', onclick: function () { savePage(); if (cur < pages.length - 1) loadPage(cur + 1); else if (pages.length < WHITEBOARD_MAX_PAGES) { pages.push(newPage()); loadPage(pages.length - 1); } else toast('حداکثر ' + fa(WHITEBOARD_MAX_PAGES) + ' صفحه.', 'err'); }}),
      el('button', {class: 'btn sm', text: '✅ ' + (opts && opts.doneLabel ? opts.doneLabel : 'ثبت به‌عنوان پاسخ'), onclick: function () { savePage(); var out = pages.filter(function (p) { return p.data; }).map(function (p) { return p.data; }); close(); if (out.length) onDone(out); }}),
      el('button', {class: 'btn light sm', text: '✕', title: 'بستن بدون ثبت', onclick: async function () { if (await S.confirmDlg('بستن تخته', 'تخته بدون ثبت بسته شود؟', 'بستن', true)) close(); }})
    ]);
    function close() { window.removeEventListener('pointerup', up); bg.remove(); document.body.style.overflow = ''; }
    var area = el('div', {class: 'wb-area'}, [canvas]);
    bg.appendChild(bar); bg.appendChild(area); document.body.appendChild(bg); document.body.style.overflow = 'hidden';
    /* صفحه‌های قبلی تخته (اگر پاسخ فعلی از تخته آمده) بارگذاری می‌شوند */
    var prev = opts ? (opts.prev || []) : (run.images[q.id] || []).filter(function (u) { return /^data:/.test(u); });
    if (prev.length && (opts || q.maxAnswerImages <= 0)) prev.forEach(function (d) { var p = newPage(); p.data = d; pages.push(p); }); else pages.push(newPage());
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

  window.SiteStudent = {page: page, richHtml: richHtml, fitMath: fitMath, mathCss: mathCss, openWhiteboard: openWhiteboard, simplifyFigs: simplifyFigs, decodeExam: decodeExam, stableShuffle: stableShuffle, sanitize: sanitize, hasActive: function () { return !!restoreActive(); }, inExam: function () { return !!(run && !run.finished); }};
})();
