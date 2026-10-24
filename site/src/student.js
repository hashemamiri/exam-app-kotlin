/* V140 — فاز ۳ سایت: پنل دانش‌آموز (شرکت در آزمون با کد + تختهٔ سفید)
   قراردادها عیناً از اپ: get_exam_for_student(p_code) → StudentExamPayloadCodec (پاک‌سازی کلید پاسخ،
   به‌هم‌ریختن پایدار StableExamShuffle با seed «student:exam:index»)، ارسال با native_submit_queued_answer_v1
   (p_operation, p_exam, p_responses به ترتیب اصلی سؤال‌ها، p_images، p_meta{native,queued,created_at_epoch_ms,monitor_report})،
   گزارش نظارتی با native_monitor_upsert_v1، تصاویر پاسخ در exam-images/answers/<student>/<exam>/<question>/. */
(function () {
  'use strict';
  var S = window.ExamSite;
  var el = S.el, esc = S.esc, fa = S.fa, en = S.en, toast = S.toast, errMsg = S.errMsg, uuid = S.uuid;
  function num(v) { v = Number(en(v)); return isFinite(v) ? v : 0; }
  var LS_ACTIVE = 'examsite.student.active.v1';   /* ActiveExamSessionEntity */
  var LS_DRAFT = 'examsite.student.draft.v1.';    /* + examId → {answers, images} */
  var BUCKET = 'exam-images';
  var WHITEBOARD_MAX_PAGES = 6;
  var ANSWER_FIELDS = ['correctOption', 'correctIndex', 'correctAnswer', 'accept', 'answer', 'tolerance', 'matchAnswer', 'explanation', 'answer_key', 'rubric'].concat(['partRubrics', 'constructionTargets', 'constructionConstraints']);

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
      var q = {id: obj.id || ('q-' + oi), type: type, text: obj.text || '', sectionTitle: obj.sectionTitle || '', topic: obj.topic || '', passage: obj.passage || '', solutionSteps: Array.isArray(obj.solutionSteps) ? obj.solutionSteps.map(String) : [], assessmentParts: Array.isArray(obj.assessmentParts) ? obj.assessmentParts : [], proofAssumptions: String(obj.proofAssumptions||''), proofGoal: String(obj.proofGoal||''), constructionPrompt: String(obj.constructionPrompt||''), constructionLabels: Array.isArray(obj.constructionLabels)?obj.constructionLabels.map(String).slice(0,12):[], constructionAngleLabels: Array.isArray(obj.constructionAngleLabels)?obj.constructionAngleLabels.map(String).slice(0,8):[], mode: obj.mode || '', score: Number(obj.score) || 0, images: images, maxAnswerImages: maxImg, answerImagesRequired: allow === 'required', originalIndex: oi,
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
    var serverNowMs = parseMs(raw.server_now);
    var clockDelta = serverNowMs != null ? now - serverNowMs : 0;
    var serverSectionDeadlines = {}, serverExpiredSections = {};
    if (raw.section_deadlines && typeof raw.section_deadlines === 'object') {
      Object.keys(raw.section_deadlines).forEach(function (k) {
        var dl = parseMs(raw.section_deadlines[k]);
        if (dl != null) serverSectionDeadlines[String(k).trim()] = dl + clockDelta;
      });
    }
    if (Array.isArray(raw.expired_sections)) raw.expired_sections.forEach(function (k) { var t = String(k || '').trim(); if (t) serverExpiredSections[t] = true; });
    return {id: examId, primary: !!(raw.options && raw.options.primary), sectionDurations: sectionDurations, serverSectionDeadlines: serverSectionDeadlines, serverExpiredSections: serverExpiredSections, /* V301 — حالت ابتدایی */ title: (raw.title || '').trim() || 'آزمون', code: raw.code || '', duration: duration, questions: ordered, subject: raw.subject || '', teacherMessage: (raw.teacher_message || '').trim() || null, deadline: deadline,
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
    var mergedDeadlines = Object.assign({}, d.sectionDeadlines || {}, exam.serverSectionDeadlines || {});
    var mergedExpired = Object.assign({}, d.expiredSections || {}, exam.serverExpiredSections || {});
    Object.keys(mergedDeadlines).forEach(function (k) { if (mergedDeadlines[k] <= Date.now()) mergedExpired[k] = true; });
    var startIdx = 0;
    while (startIdx < exam.questions.length && exam.questions[startIdx].sectionTitle && mergedExpired[String(exam.questions[startIdx].sectionTitle).trim()]) startIdx++;
    if (startIdx >= exam.questions.length) startIdx = 0;
    run = {exam: exam, practice: !!exam.practice, index: startIdx, answers: d.answers || {}, images: d.images || {}, started: true, finished: false, submitting: false, enteredAt: Date.now(), qEnter: {}, qTime: {}, qVisits: {}, events: {}, sectionDeadlines: mergedDeadlines, expiredSections: mergedExpired, syncedSections: {}, timer: null, monitorTimer: null};
    run.qEnter[startIdx] = Date.now(); run.qVisits[exam.questions[startIdx] && exam.questions[startIdx].id] = 1;
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
    if ((e.ctrlKey || e.metaKey) && (k === 'c' || k === 'x' || (k === 'a' && !document.querySelector('.engine-bg.wb'))) && !inField(e.target)) { e.preventDefault(); secEvent('copy_attempt'); }
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
      var ansHost = el('div');
      var rebuildAns = function () { var nextA = answerArea(q); ansHost.innerHTML = ''; ansHost.appendChild(nextA); };
      richHtml(q.text).then(function (h) { if (ex.questions[run.index] === q) { txt.innerHTML = h; fitMath(txt); bindInteractiveQuestionFigs(txt, q, rebuildAns); } });
      if (q.passage) {
        var passageBox = el('div', {class:'card', style:'padding:14px;margin:8px 0;background:var(--soft)'}, [el('b',{text:'متن/صورت مشترک'}), el('div',{style:'margin-top:8px;white-space:pre-wrap',text:q.passage})]);
        body.insertBefore(passageBox, txt);
      }
      if (p.audio) body.appendChild(el('audio', {controls: 'controls', src: p.audio, style: 'width:100%;margin:8px 0'}));
      else if (ex.primary || q.mode === 'dictation') body.appendChild(readAloudButton(q)); /* V302 — ابتدایی: «بخوان» با صدای مرورگر وقتی معلم صدا نگذاشته */
      if (q.images.length) body.appendChild(el('div', {class: 'st-imgs'}, q.images.map(function (u) { return el('img', {src: u, onclick: function () { lightbox(u); }}); })));
      rebuildAns();
      body.appendChild(ansHost);
    }
    /* V335.8 / V335.11 — تعامل مستقیم دانش‌آموز روی خانه‌های خالی شکل‌های تعاملی در همهٔ پایه‌ها */
    function bindInteractiveQuestionFigs(container, q, onSyncAns) {
      if (!container || !(q.type === 'fill' || q.type === 'numeric' || q.type === 'essay')) return;
      ensureMathFrame().then(function (w) {
        if (!w || !w.GraphFig) return;
        Array.prototype.forEach.call(container.querySelectorAll('.qmf-fig'), function (figEl) {
          var baseSpec = null;
          try { baseSpec = JSON.parse(figEl.getAttribute('data-fig') || '{}'); } catch (_e) { return; }
          if (!baseSpec || !baseSpec.t) return;
          var t = baseSpec.t, X = baseSpec.X || {};
          var isSym = t === 'axsym' && Math.floor(Number(en(String(X.mirror == null ? '1' : X.mirror)))) === 0;
          var isPaint = t === 'axpaintgrid';
          var isDomKeyed = (t === 'axdicefold' || t === 'axunitconv' || t === 'axprobtree' || t === 'axorbital' || t === 'axpunnett' || t === 'axkenken' || t === 'axkmap');
          var isKeyed = (t === 'axcalendar' || t === 'axmultgrid' || t === 'axmulwheel' || t === 'axtower') || isDomKeyed;
          if (!isSym && !isPaint && !isKeyed) return;
          if (isDomKeyed && !figEl.querySelector('svg')) figEl.innerHTML = w.GraphFig.svg(baseSpec);
          var listEmptyKeys = function () {
            if (isDomKeyed) {
              var domKeys = [];
              Array.prototype.forEach.call(figEl.querySelectorAll('[data-cell-empty="1"]'), function (nd) {
                var k0 = nd.getAttribute('data-cell-key');
                if (k0 && domKeys.indexOf(k0) < 0) domKeys.push(k0);
              });
              return domKeys;
            }
            var empRaw = String(X.empty || '').split(/[,،;؛\s]+/).map(function (z) { return en(z.trim()); }).filter(Boolean);
            if (empRaw.length) return empRaw;
            var out = [];
            if (t === 'axcalendar' && String(X.blank || '0') === '1') {
              var ds = Math.max(20, Math.min(31, Math.floor(Number(en(String(X.days || '30'))) || 30)));
              for (var d0 = 1; d0 <= ds; d0++) if (d0 % 5 === 0) out.push(String(d0));
            } else if (t === 'axmultgrid') {
              var mb = Math.floor(Number(en(String(X.blank || '0'))) || 0);
              var rn = String(X.rows || '2,4,7').split(/[,،;]/).filter(Boolean).slice(0, 4).length || 3;
              var cn = String(X.cols || '3,5,8').split(/[,،;]/).filter(Boolean).slice(0, 4).length || 3;
              if (mb === 2) {
                for (var c0 = 0; c0 < cn; c0++) out.push('c' + c0);
                for (var r0 = 0; r0 < rn; r0++) out.push('r' + r0);
              } else if (mb === 1) {
                for (var r1 = 0; r1 < rn; r1++) for (var c1 = 0; c1 < cn; c1++) if ((r1 + c1) % 2 === 1) out.push(r1 + '_' + c1);
              }
            } else if (t === 'axmulwheel' && String(X.hide || '0') === '1') {
              var wn = Math.max(3, Math.min(12, Math.floor(Number(en(String(X.n || '10'))) || 10)));
              for (var wj = 0; wj < wn; wj++) if (wj % 3 === 2) out.push('s_' + wj);
            } else if (t === 'axtower' && String(X.blank || '0') === '1') {
              var bLen = Math.max(2, Math.min(6, String(X.base || '1,2,3,4').split(/[,،;]/).filter(Boolean).length || 4));
              for (var lv = bLen - 1; lv >= 0; lv--) {
                var cnt = bLen - lv;
                for (var tk = 0; tk < cnt; tk++) if ((lv + tk) % 3 === 1) out.push(lv + '_' + tk);
              }
            }
            return out;
          };
          var emptyKeys = isKeyed ? listEmptyKeys() : [];
          if (isKeyed && !emptyKeys.length) return;
          var parseStuMap = function (raw) {
            var s0 = String(raw == null ? '' : raw).trim(), m = {};
            if (!s0) return m;
            if (s0.indexOf(':') >= 0 || s0.indexOf('=') >= 0) {
              s0.split(/[;؛]/).forEach(function (p0) {
                var ix = p0.indexOf(':') > 0 ? p0.indexOf(':') : p0.indexOf('=');
                if (ix > 0) { var k0 = en(p0.slice(0, ix).trim()), v0 = p0.slice(ix + 1).trim(); if (k0 && v0) m[k0] = v0; }
              });
              return m;
            }
            if (emptyKeys.length === 1) { m[emptyKeys[0]] = s0; return m; }
            var sp = s0.split(/[,،;؛]+/).map(function (z) { return z.trim(); });
            emptyKeys.forEach(function (k0, i0) { if (sp[i0]) m[k0] = sp[i0]; });
            return m;
          };
          var encodeStuMap = function (m) {
            if (emptyKeys.length === 1) return (m[emptyKeys[0]] || '').trim();
            var arr = emptyKeys.map(function (k0) { return (m[k0] || '').trim(); });
            if (arr.every(function (x) { return !x; })) return '';
            if (arr.some(function (x) { return x.indexOf(',') >= 0 || x.indexOf('،') >= 0; })) {
              return emptyKeys.filter(function (k0) { return !!(m[k0] || '').trim(); }).map(function (k0) { return k0 + ':' + m[k0].trim(); }).join(';');
            }
            return arr.join('، ');
          };
          var stuQuickChips = function (cellKey) {
            if (t === 'axorbital' && cellKey !== 'cfg') return [['↑↓', '↑↓ (جفت)'], ['↑', '↑ (تک بالا)'], ['↓', '↓ (تک پایین)']];
            if (t === 'axdicefold') return [['1', '۱'], ['2', '۲'], ['3', '۳'], ['4', '۴'], ['5', '۵'], ['6', '۶']];
            if (t === 'axpunnett') {
              if (String(X.kind || '').toLowerCase() === 'pedigree') {
                return [['M', '□ مرد سالم'], ['M*', '■ مرد مبتلا'], ['Mc', '◧ مرد ناقل'], ['F', '○ زن سالم'], ['F*', '● زن مبتلا'], ['Fc', '◐ زن ناقل']];
              }
              if (cellKey.indexOf('c_') === 0) return [['AA', 'AA'], ['Aa', 'Aa'], ['aa', 'aa'], ['AABB', 'AABB'], ['AaBb', 'AaBb'], ['aabb', 'aabb']];
              if (cellKey.indexOf('h') === 0) return [['A', 'A'], ['a', 'a'], ['AB', 'AB'], ['Ab', 'Ab'], ['aB', 'aB'], ['ab', 'ab']];
            }
            if (t === 'axkenken') {
              var ksz = Math.max(3, Math.min(6, Math.floor(Number(en(String(X.size || '4'))) || 4))), kr = [];
              for (var kd = 1; kd <= ksz; kd++) kr.push([String(kd), fa(kd)]);
              return kr;
            }
            if (t === 'axkmap') {
              return cellKey === 'expr' ? null : [['1', '۱'], ['0', '۰'], ['X', 'X']];
            }
            return null;
          };
          var saveAns = function (v) {
            if (!v) delete run.answers[q.id]; else run.answers[q.id] = v;
            saveDraft(); drawNav();
            if (onSyncAns) onSyncAns();
            repaintFig();
          };
          var repaintFig = function () {
            var curAns = String(run.answers[q.id] == null ? '' : run.answers[q.id]);
            var clone = JSON.parse(JSON.stringify(baseSpec));
            clone.X = clone.X || {};
            if (isSym || isPaint) clone.X.ansCells = curAns;
            else if (isKeyed) {
              var mp = parseStuMap(curAns);
              clone.X.ans = Object.keys(mp).map(function (k0) { return k0 + ':' + mp[k0]; }).join(';');
            }
            figEl.innerHTML = w.GraphFig.svg(clone);
            var sv = figEl.querySelector('svg');
            if (sv) { sv.style.cursor = 'pointer'; sv.style.touchAction = 'manipulation'; }
          };
          var svgPoint = function (ev) {
            var sv = figEl.querySelector('svg');
            if (!sv || !sv.createSVGPoint || !sv.getScreenCTM) return null;
            var pt = sv.createSVGPoint(); pt.x = ev.clientX; pt.y = ev.clientY;
            return pt.matrixTransform(sv.getScreenCTM().inverse());
          };
          var openStuCellModal = function (cellKey, titleLbl) {
            var mp = parseStuMap(run.answers[q.id]);
            var bg = el('div', {class: 'modal-bg'});
            var textMode = (t === 'axorbital' || t === 'axpunnett' || t === 'axprobtree' || t === 'axkmap');
            var inp = el('input', {type: 'text', class: 'st-input', inputmode: textMode ? 'text' : 'decimal', placeholder: 'مقدار این خانه', value: mp[cellKey] || '', style: 'text-align:center;font-size:20px;font-weight:700'});
            var commit = function (val) {
              var clean = String(val == null ? '' : val).replace(/[;؛]/g, ',').trim();
              if (clean === '') delete mp[cellKey]; else mp[cellKey] = clean;
              bg.remove();
              saveAns(encodeStuMap(mp));
            };
            inp.addEventListener('keydown', function (ev) { if (ev.key === 'Enter') { ev.preventDefault(); commit(inp.value); } });
            var chips = stuQuickChips(cellKey);
            var chipsRow = chips ? el('div', {class: 'row', style: 'flex-wrap:wrap;gap:6px;margin-bottom:10px;justify-content:center'}, chips.map(function (ch) {
              return el('button', {type: 'button', class: 'btn light sm', text: ch[1], onclick: function () { commit(ch[0]); }});
            })) : null;
            var modal = el('div', {class: 'modal', style: 'max-width:380px'}, [
              el('h3', {text: titleLbl, style: 'margin-top:0'}),
              chipsRow,
              inp,
              el('div', {class: 'row', style: 'margin-top:12px;gap:8px'}, [
                el('button', {type: 'button', class: 'btn', text: 'ثبت', onclick: function () { commit(inp.value); }}),
                el('button', {type: 'button', class: 'btn light', text: 'پاک‌کردن خانه', onclick: function () { commit(''); }}),
                el('button', {type: 'button', class: 'btn light', text: 'انصراف', onclick: function () { bg.remove(); }})
              ])
            ].filter(Boolean));
            bg.addEventListener('click', function (ev) { if (ev.target === bg) bg.remove(); });
            bg.appendChild(modal); document.body.appendChild(bg);
            setTimeout(function () { try { inp.focus(); inp.select(); } catch (_e) {} }, 20);
          };
          figEl.onclick = function (ev) {
            if (isDomKeyed) {
              var domEl = ev.target && ev.target.closest ? ev.target.closest('[data-cell-empty="1"]') : null;
              if (!domEl || !figEl.contains(domEl)) return;
              var ck = domEl.getAttribute('data-cell-key') || '', cl = domEl.getAttribute('data-cell-label') || 'این خانه';
              if (ck && emptyKeys.indexOf(ck) >= 0) openStuCellModal(ck, 'مقدار «' + cl + '» را وارد کن');
              return;
            }
            var p = svgPoint(ev); if (!p) return;
            if (isSym) {
              if (p.x < 76 || p.x > 284 || p.y < 32 || p.y > 240) return;
              var cc = Math.max(1, Math.min(8, Math.floor((p.x - 76) / 26) + 1));
              var rr = Math.max(1, Math.min(8, Math.floor((p.y - 32) / 26) + 1));
              var key = rr + ',' + cc;
              var list = String(run.answers[q.id] || '').split(/[;؛]/).map(function (z) { return en(z.trim()); }).filter(Boolean);
              var ix = list.indexOf(key);
              if (ix >= 0) list.splice(ix, 1); else list.push(key);
              saveAns(list.join(';'));
            } else if (isPaint) {
              var rows = Math.max(1, Math.min(8, Math.round(Number(en(String(X.rows || '4'))) || 4)));
              var cols = Math.max(1, Math.min(8, Math.round(Number(en(String(X.cols || '4'))) || 4)));
              var goal = Math.max(0, Math.round(Number(en(String(X.goal || '0'))) || 0));
              var pw = Math.min(300, cols * 42), ph = Math.min(150, rows * 34);
              var px0 = (360 - pw) / 2, py0 = 62 + (150 - ph) / 2;
              if (p.x < px0 || p.x > px0 + pw || p.y < py0 || p.y > py0 + ph) return;
              var pc = Math.max(1, Math.min(cols, Math.floor((p.x - px0) / (pw / cols)) + 1));
              var pr = Math.max(1, Math.min(rows, Math.floor((p.y - py0) / (ph / rows)) + 1));
              var pkey = pr + ',' + pc;
              var plist = String(run.answers[q.id] || '').split(/[;؛]/).map(function (z) { return en(z.trim()); }).filter(Boolean);
              var pix = plist.indexOf(pkey);
              if (pix >= 0) plist.splice(pix, 1);
              else if (goal <= 0 || plist.length < goal) plist.push(pkey);
              saveAns(plist.join(';'));
            } else if (t === 'axcalendar') {
              var clStart = Math.max(0, Math.min(6, Math.floor(Number(en(String(X.start || '3'))) || 0)));
              var clDays = Math.max(20, Math.min(31, Math.floor(Number(en(String(X.days || '30'))) || 30)));
              var clX0 = 180 - 44 * 3.5, clY0 = 76;
              if (p.x < clX0 || p.x > clX0 + 308 || p.y < clY0) return;
              var col2 = Math.floor((p.x - clX0) / 44), row2 = Math.floor((p.y - clY0) / 30);
              var dayN = row2 * 7 + (6 - col2) - clStart + 1;
              if (dayN >= 1 && dayN <= clDays && emptyKeys.indexOf(String(dayN)) >= 0) openStuCellModal(String(dayN), 'عدد این روز تقویم را بنویس');
            } else if (t === 'axmultgrid') {
              var rn2 = String(X.rows || '2,4,7').split(/[,،;]/).filter(Boolean).slice(0, 4).length || 3;
              var cn2 = String(X.cols || '3,5,8').split(/[,،;]/).filter(Boolean).slice(0, 4).length || 3;
              var mcw = Math.min(56, 268 / (cn2 + 1)), mch = Math.min(42, 216 / (rn2 + 1));
              var mx0 = 180 - mcw * (cn2 + 1) / 2, my0 = 26 + (218 - mch * (rn2 + 1)) / 2;
              if (p.x < mx0 || p.x > mx0 + (cn2 + 1) * mcw || p.y < my0 || p.y > my0 + (rn2 + 1) * mch) return;
              var gc = Math.floor((p.x - mx0) / mcw), gr = Math.floor((p.y - my0) / mch);
              var mkey = (gr === 0 && gc > 0) ? ('c' + (gc - 1)) : (gc === 0 && gr > 0) ? ('r' + (gr - 1)) : (gr > 0 && gc > 0) ? ((gr - 1) + '_' + (gc - 1)) : '';
              if (mkey && emptyKeys.indexOf(mkey) >= 0) openStuCellModal(mkey, 'مقدار خانهٔ خالی را بنویس');
            } else if (t === 'axmulwheel') {
              var wn2 = Math.max(3, Math.min(12, Math.floor(Number(en(String(X.n || '10'))) || 10)));
              if (Math.hypot(p.x - 180, p.y - 142) <= 34 && emptyKeys.indexOf('center') >= 0) {
                openStuCellModal('center', 'عدد مرکز چرخ را بنویس'); return;
              }
              for (var j2 = 0; j2 < wn2; j2++) {
                var a2 = -Math.PI / 2 + j2 * 2 * Math.PI / wn2, ox = 180 + 96 * Math.cos(a2), oy = 142 + 96 * Math.sin(a2);
                if (Math.hypot(p.x - ox, p.y - oy) <= 22 && emptyKeys.indexOf('s_' + j2) >= 0) {
                  openStuCellModal('s_' + j2, 'عدد این خانهٔ چرخ را بنویس'); return;
                }
              }
            } else if (t === 'axtower') {
              var bL = Math.max(2, Math.min(6, String(X.base || '1,2,3,4').split(/[,،;]/).filter(Boolean).length || 4));
              var tbh = Math.min(34, 216 / bL), tbw = Math.min(58, 300 / bL);
              for (var trow = 0; trow < bL; trow++) {
                var lvI = bL - 1 - trow, cntI = bL - lvI, tly = 24 + trow * tbh;
                if (p.y >= tly && p.y <= tly + tbh) {
                  for (var tk2 = 0; tk2 < cntI; tk2++) {
                    var tbx = 180 + (tk2 - (cntI - 1) / 2) * tbw - tbw / 2, tkey = lvI + '_' + tk2;
                    if (p.x >= tbx && p.x <= tbx + tbw && emptyKeys.indexOf(tkey) >= 0) {
                      openStuCellModal(tkey, 'عدد این آجر هرم را بنویس'); return;
                    }
                  }
                }
              }
            }
          };
          repaintFig();
        });
      });
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
    /* V314.0 / V335.8 — بخش‌کردن و صداکشی: کادرهای پویا (+ افزودن کادر / حذف کادر) + نشانه‌های فارسی */
    function syllableAnswer(q, saved, set) {
      var minBoxes = (q.syllableCount || 0) >= 2 ? Math.min(8, q.syllableCount) : 1;
      var rawParts = saved ? String(saved).split('-') : [];
      var initCount = Math.max(minBoxes, Math.min(8, rawParts.length || minBoxes));
      var parts = []; for (var i = 0; i < initCount; i++) parts.push(rawParts[i] || '');
      var activeIdx = 0;
      var wrap = el('div', {class: 'pri-syllable', style: 'display:flex;flex-direction:column;gap:8px'});
      wrap.appendChild(el('b', {style: 'font-size:15px', text: 'هر بخش (هجا) را در یک کادر بنویس (با دکمهٔ + کادر اضافه کن):'}));
      var boxesRow = el('div', {class: 'row', style: 'gap:6px;flex-wrap:wrap;align-items:center'});
      var inputs = [];
      function emit() { set(parts.map(function (x) { return String(x || '').replace(/-/g, '').trim(); }).join('-')); }
      function renderBoxes() {
        boxesRow.innerHTML = '';
        inputs = [];
        parts.forEach(function (val, idx) {
          if (idx > 0) boxesRow.appendChild(el('b', {style: 'font-size:20px;padding:0 2px', text: ' - '}));
          var inp = el('input', {type: 'text', value: val, placeholder: 'بخش ' + fa(idx + 1), style: 'width:96px;text-align:center;font-size:18px;font-weight:bold;border:2px solid #0E7490;border-radius:10px;padding:6px'});
          inp.addEventListener('focus', function () { activeIdx = idx; });
          inp.addEventListener('input', function () { activeIdx = idx; parts[idx] = inp.value; emit(); });
          inputs.push(inp);
          boxesRow.appendChild(inp);
        });
        if (parts.length < 8) {
          boxesRow.appendChild(el('button', {type: 'button', class: 'btn soft sm', text: '+ افزودن کادر', onclick: function () {
            parts.push(''); activeIdx = parts.length - 1; emit(); renderBoxes();
            if (inputs[activeIdx]) inputs[activeIdx].focus();
          }}));
        }
        if (parts.length > 1) {
          boxesRow.appendChild(el('button', {type: 'button', class: 'btn light sm', text: '✕ حذف کادر', onclick: function () {
            parts.pop(); activeIdx = Math.max(0, Math.min(parts.length - 1, activeIdx)); emit(); renderBoxes();
          }}));
        }
      }
      renderBoxes();
      wrap.appendChild(boxesRow);
      var signsRow = el('div', {class: 'row', style: 'gap:6px;flex-wrap:wrap'});
      ['ـَ', 'ـِ', 'ـُ', 'ـّ', 'ا', 'آ', 'ای', 'او', 'ی', 'و'].forEach(function (sign) {
        var ch = sign.replace(/^ـ/, '');
        signsRow.appendChild(el('button', {type: 'button', class: 'btn soft sm', text: sign, onclick: function () {
          var idx = Math.max(0, Math.min(parts.length - 1, activeIdx));
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
      var carryRefs=[];
      (q.assessmentParts||[]).forEach(function(part,pi){
        var cur=data[part.id]||{answer:'',reason:''};
        var card=el('div',{class:'card',style:'padding:9px;margin:7px 0'});
        card.appendChild(el('b',{text:'گام '+(part.label||fa(pi+1))+': '+(part.prompt||'')}));
        var answer=el('textarea',{class:'st-input',rows:2,placeholder:'نتیجه / گزارهٔ این گام'});answer.value=cur.answer||'';answer.addEventListener('input',function(){cur.answer=answer.value;data[part.id]=cur;save();carryRefs.forEach(function(fn){fn();});});card.appendChild(answer);
        if(part.requiresReason){var reason=el('textarea',{class:'st-input',rows:2,placeholder:part.reasonPrompt||'دلیل این گام'});reason.value=cur.reason||'';reason.addEventListener('input',function(){cur.reason=reason.value;data[part.id]=cur;save();});card.appendChild(reason);}
        if(part.carriesForward&&pi>0){var prevPart=q.assessmentParts[pi-1],prevEl=el('div',{class:'muted',text:(data[prevPart.id]||{}).answer||'پاسخ گام قبل هنوز ثبت نشده است.'});carryRefs.push(function(){prevEl.textContent=(data[prevPart.id]||{}).answer||'پاسخ گام قبل هنوز ثبت نشده است.';});card.appendChild(el('div',{class:'card',style:'padding:7px;background:var(--soft)'},[el('b',{text:'ارجاع به نتیجهٔ گام قبل'}),prevEl]));}
        wrap.appendChild(card);
      });
      return wrap;
    }

    function parseAngleVertsJs(raw, knownLabels) {
      var clean = String(raw || '').replace(/^∠/, '').trim();
      if (!clean) return null;
      if (/[,،\-\s]/.test(clean)) { var ps = clean.split(/[,،\-\s]+/).map(function (x) { return x.trim(); }).filter(Boolean); return ps.length === 3 ? ps : null; }
      if (clean.length === 3) return [clean[0], clean[1], clean[2]];
      var sorted = (knownLabels || []).slice().sort(function (a, b) { return b.length - a.length; }), out = [], rest = clean;
      while (rest.length && out.length < 3) {
        var m2 = sorted.find(function (lb) { return rest.indexOf(lb) === 0; });
        if (!m2) return null;
        out.push(m2); rest = rest.slice(m2.length);
      }
      return out.length === 3 && !rest.length ? out : null;
    }

    function constructionAnswer(q, saved, set) {
      var labels=(q.constructionLabels||[]).slice(0,12), points={}, savedAngles=[], activeLabel=null;
      var snapHalf = q.__snapHalf !== false;
      try {
        var rawObj=JSON.parse(String(saved||'{}'))||{};
        Object.keys(rawObj).forEach(function(k){
          if(k==='_angles'){
            if(Array.isArray(rawObj[k])) savedAngles=rawObj[k].map(String).filter(Boolean);
            else if(rawObj[k]&&typeof rawObj[k]==='object') savedAngles=Object.keys(rawObj[k]).filter(Boolean);
          } else if(rawObj[k]&&typeof rawObj[k]==='object'&&isFinite(Number(rawObj[k].x))&&isFinite(Number(rawObj[k].y))) points[k]={x:Number(rawObj[k].x),y:Number(rawObj[k].y)};
        });
      } catch (_) { points={}; }
      var angles = Array.from(new Set((q.constructionAngleLabels||[]).concat(savedAngles).map(function(x){return String(x||'').replace(/^∠/,'').trim();}).filter(function(x){return x.length>=3;})));
      function serializeState(nextPts, nextAngs) {
        var payload = {};
        Object.keys(nextPts).forEach(function(k){ payload[k] = {x: Math.round(Number(nextPts[k].x)*100)/100, y: Math.round(Number(nextPts[k].y)*100)/100}; });
        if (nextAngs && nextAngs.length) {
          var angMap = {};
          nextAngs.forEach(function(tok){
            var verts = parseAngleVertsJs(tok, labels);
            var dg = verts && nextPts[verts[0]] && nextPts[verts[1]] && nextPts[verts[2]] ? angleDeg(nextPts[verts[0]], nextPts[verts[1]], nextPts[verts[2]]) : null;
            angMap[tok] = dg != null ? Math.round(dg * 10) / 10 : -1;
          });
          payload._angles = angMap;
        }
        return JSON.stringify(payload);
      }
      function angleDeg(p0, pv, p2) {
        var ux=p0.x-pv.x, uy=p0.y-pv.y, vx=p2.x-pv.x, vy=p2.y-pv.y;
        var nu=Math.hypot(ux,uy), nv=Math.hypot(vx,vy);
        if (nu<1e-6 || nv<1e-6) return null;
        return Math.acos(Math.max(-1, Math.min(1, (ux*vx+uy*vy)/(nu*nv)))) * 180 / Math.PI;
      }
      var wrap=el('div',{class:'st-construct'});
      wrap.appendChild(el('p',{class:'st-hint',text:q.constructionPrompt||'نقاط خواسته‌شده را روی صفحهٔ مختصات مشخص کنید.'}));
      var canvas=el('canvas',{width:640,height:340,style:'width:100%;height:290px;border:1px solid #9aa5b1;border-radius:8px;touch-action:manipulation;background:#fff'});
      var ctx=canvas.getContext('2d'), m=30, w=canvas.width-2*m, h=canvas.height-2*m;
      function px(x){return m+(Number(x)+5)/10*w;} function py(y){return m+(5-Number(y))/10*h;}
      ctx.clearRect(0,0,canvas.width,canvas.height);ctx.strokeStyle='#d9dee6';ctx.lineWidth=1;ctx.fillStyle='#6e7884';ctx.font='11px sans-serif';
      for(var i=-5;i<=5;i++){
        ctx.beginPath();ctx.moveTo(px(i),m);ctx.lineTo(px(i),m+h);ctx.stroke();
        ctx.beginPath();ctx.moveTo(m,py(i));ctx.lineTo(m+w,py(i));ctx.stroke();
        if(i!==0){ctx.fillText(String(i),px(i)-4,py(0)+13);ctx.fillText(String(i),px(0)+4,py(i)+4);}
      }
      ctx.strokeStyle='#263445';ctx.lineWidth=2;ctx.beginPath();ctx.moveTo(px(0),m);ctx.lineTo(px(0),m+h);ctx.stroke();ctx.beginPath();ctx.moveTo(m,py(0));ctx.lineTo(m+w,py(0));ctx.stroke();
      angles.forEach(function(tok){
        var verts=parseAngleVertsJs(tok,labels);if(!verts)return;
        var p0=points[verts[0]],pv=points[verts[1]],p2=points[verts[2]];
        if(p0&&pv&&p2){
          ctx.strokeStyle='#d97706';ctx.lineWidth=2;
          ctx.beginPath();ctx.moveTo(px(pv.x),py(pv.y));ctx.lineTo(px(p0.x),py(p0.y));ctx.moveTo(px(pv.x),py(pv.y));ctx.lineTo(px(p2.x),py(p2.y));ctx.stroke();
          var dg=angleDeg(p0,pv,p2);
          if(dg!=null){ctx.fillStyle='#b45309';ctx.font='bold 13px sans-serif';ctx.fillText('∠'+tok+'='+dg.toFixed(1)+'°',px(pv.x)+8,py(pv.y)+16);}
        }
      });
      labels.forEach(function(label){var p=points[label];if(!p)return;ctx.fillStyle='#1e53aa';ctx.beginPath();ctx.arc(px(p.x),py(p.y),7,0,Math.PI*2);ctx.fill();ctx.font='bold 17px sans-serif';ctx.fillText(label,px(p.x)+9,py(p.y)-8);});
      canvas.addEventListener('pointerdown',function(ev){
        var r=canvas.getBoundingClientRect();var sx=(ev.clientX-r.left)*(canvas.width/r.width),sy=(ev.clientY-r.top)*(canvas.height/r.height);
        var label=activeLabel||labels.find(function(x){return !Object.prototype.hasOwnProperty.call(points,x);})||labels[labels.length-1];if(!label)return;
        var rx=Math.max(-5,Math.min(5,((sx-m)/w)*10-5)), ry=Math.max(-5,Math.min(5,5-((sy-m)/h)*10));
        if(snapHalf){rx=Math.round(rx*2)/2;ry=Math.round(ry*2)/2;}
        points=Object.assign({},points);points[label]={x:rx,y:ry};
        set(serializeState(points, angles));wrap.replaceWith(answerArea(q));
      });
      wrap.appendChild(canvas);
      var metaRow=el('div',{class:'row',style:'gap:8px;align-items:center;margin-top:4px'},[
        el('span',{class:'st-hint',text:'ثبت‌شده: '+fa(Object.keys(points).filter(function(k){return labels.indexOf(k)>=0;}).length)+' از '+fa(labels.length)}),
        el('button',{type:'button',class:'chip'+(snapHalf?' brand':''),text:'چسبیدن به شبکه ۰٫۵',onclick:function(){q.__snapHalf=!snapHalf;wrap.replaceWith(answerArea(q));}})
      ]);
      wrap.appendChild(metaRow);
      wrap.appendChild(el('div',{class:'st-hint',text:'برای جابه‌جایی، برچسب نقطه را انتخاب و محل تازه را روی نمودار لمس کنید.'}));
      var chips=el('div',{class:'row',style:'flex-wrap:wrap;gap:6px'});labels.forEach(function(label){var p=points[label], chip=el('button',{type:'button',class:'chip'+(p?' brand':''),text:label+(p?' ('+fa(Number(p.x).toFixed(1))+', '+fa(Number(p.y).toFixed(1))+')':' —'),onclick:function(){activeLabel=activeLabel===label?null:label;Array.prototype.forEach.call(chips.children,function(x){x.classList.toggle('on',x.dataset.pointLabel===activeLabel);});}});chip.dataset.pointLabel=label;chips.appendChild(chip);});
      chips.appendChild(el('button',{class:'btn light sm',text:'پاک‌کردن',onclick:function(){set('{}');wrap.replaceWith(answerArea(q));}}));wrap.appendChild(chips);
      if(angles.length||labels.length>=3){
        var angRow=el('div',{class:'row',style:'flex-wrap:wrap;gap:6px;margin-top:6px'});
        angles.forEach(function(tok){
          var verts=parseAngleVertsJs(tok,labels), dg=verts&&points[verts[0]]&&points[verts[1]]&&points[verts[2]]?angleDeg(points[verts[0]],points[verts[1]],points[verts[2]]):null;
          angRow.appendChild(el('button',{type:'button',class:'chip'+(dg!=null?' brand':''),text:'∠'+tok+(dg!=null?' = '+fa(dg.toFixed(1))+'°':''),onclick:function(){
            if((q.constructionAngleLabels||[]).indexOf(tok)<0){var nextA=angles.filter(function(x){return x!==tok;});set(serializeState(points,nextA));wrap.replaceWith(answerArea(q));}
          }}));
        });
        var angInp=el('input',{type:'text',placeholder:'برچسب زاویه (مثل ABC)',style:'width:150px;border:1px solid var(--line);border-radius:8px;padding:4px 8px'});
        angRow.appendChild(angInp);
        angRow.appendChild(el('button',{type:'button',class:'btn light sm',text:'ثبت زاویه',onclick:function(){
          var v=parseAngleVertsJs(angInp.value,labels);
          if(v){var tok=v.join('');if(angles.indexOf(tok)<0){angles.push(tok);set(serializeState(points,angles));wrap.replaceWith(answerArea(q));}}
        }}));
        wrap.appendChild(angRow);
      }
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
      } else if (q.type === 'fill' && ((ex.primary && q.mode === 'syllable' && (q.syllableCount || 0) >= 2) || String(q.text || '').indexOf('"axsyllablechart"') >= 0)) {
        /* V314.0 / V335.8 — بخش‌کردن و صداکشی با کادرهای پویا (+) */
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
        var vals = String(a || '').split('\n'), stepRefs = [];
        prompts.forEach(function (pr, si) {
          var st = el('textarea', {class:'st-input', rows:2, placeholder:pr || ('گام '+fa(si+1))});
          st.value = vals[si] || '';
          st.addEventListener('input', function () { vals[si] = st.value.replace(/\n/g,' '); set(vals.join('\n')); stepRefs.forEach(function(fn){fn();}); });
          box.appendChild(st);
          if(q.assessmentParts&&q.assessmentParts[si]&&q.assessmentParts[si].carriesForward&&si>0){var prevNode=el('div',{class:'muted',text:vals[si-1]||'پاسخ گام قبل هنوز ثبت نشده است.'});stepRefs.push(function(){prevNode.textContent=vals[si-1]||'پاسخ گام قبل هنوز ثبت نشده است.';});box.appendChild(el('div',{class:'card',style:'padding:7px;background:var(--soft)'},[el('b',{text:'ارجاع به نتیجهٔ گام قبل'}),prevNode]));}
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
      /* V306 — پاسخ با تخته: تخته همیشه (بزرگ) نشان داده می‌شود؛ در حالت مختصاتی (constructionLabels) تختهٔ اضافی نشان داده نمی‌شود مگر allowAnswerGraph روشن باشد */
      var drawMode = q.type === 'essay' && (q.mode === 'draw' || q.mode === 'construct');
      if (drawMode && q.mode === 'construct' && q.constructionLabels && q.constructionLabels.length) drawMode = false;
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
    function syncSectionToServer(sectionTitle, lockSection) {
      if (!run || run.practice || !sectionTitle) return;
      var snap = {};
      if (lockSection) {
        ex.questions.filter(function (x) { return String(x.sectionTitle || '').trim() === sectionTitle; }).forEach(function (x) {
          var v = run.answers[x.id];
          snap[String(x.originalIndex)] = v == null ? null : v;
        });
      }
      S.rpcObj('native_sync_exam_section_v1', {p_exam: ex.id, p_section: sectionTitle, p_lock: !!lockSection, p_section_responses: snap}).then(function (res) {
        if (!run || run.finished || !res) return;
        var sn = parseMs(res.server_now), delta = sn != null ? Date.now() - sn : 0;
        var dl = res.section_deadlines && parseMs(res.section_deadlines[sectionTitle]);
        if (dl != null) { run.sectionDeadlines[sectionTitle] = dl + delta; saveDraft(); }
        if (Array.isArray(res.expired_sections) && res.expired_sections.indexOf(sectionTitle) >= 0) { run.expiredSections[sectionTitle] = true; saveDraft(); }
      }).catch(function () {});
    }
    function tick() {
      if (!run || run.finished) return;
      var now = Date.now(), q = ex.questions[run.index], title = q && String(q.sectionTitle || '').trim(), sectionMinutes = title ? Number(ex.sectionDurations && ex.sectionDurations[title]) || 0 : 0;
      var sectionRem = null;
      if (title && sectionMinutes > 0 && !run.expiredSections[title]) {
        if (!run.sectionDeadlines[title]) {
          run.sectionDeadlines[title] = now + Math.min(1440, sectionMinutes) * 60000;
          saveDraft();
        }
        if (!run.syncedSections[title]) {
          run.syncedSections[title] = true;
          syncSectionToServer(title, false);
        }
        sectionRem = Math.max(0, run.sectionDeadlines[title] - now);
        if (sectionRem <= 0) {
          run.expiredSections[title] = true; saveDraft();
          syncSectionToServer(title, true);
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
    var pages = [], cur = 0, tool = 'pen', color = '#1a237e', size = 3, grid = 'blank';
    var lineStyle = 'solid', fillShape = false, zoomPct = 100;
    var items = [], selectedIds = [], nextItemId = 1;
    var undoStack = [], redoStack = [], drawing = false, last = null, startPt = null, endPt = null;
    var autoShape = false, penPts = [], rulerSnapEdge = null;
    var selDrag = null;

    var canvas = el('canvas', {width: W, height: H, class: 'wb-canvas'});
    var ctx = canvas.getContext('2d');
    var inkCanvas = document.createElement('canvas');
    inkCanvas.width = W; inkCanvas.height = H;
    var inkCtx = inkCanvas.getContext('2d');
    var imgCache = {};

    var status = el('span', {class: 'muted', style: 'font-size:12px'});
    var hud = el('div', {class: 'wb-hud', style: 'display:none'});
    var selBadge = el('div', {class: 'wb-sel-badge', text: 'موردی انتخاب نشده است'});

    /* خط‌کش دیجیتال تعاملی (فقط روی نمایشگر؛ وارد خروجی ثبت پاسخ نمی‌شود) */
    var rulerOn = false, rulerState = {x: W / 2, y: H / 2 + 120, deg: 0, len: 680, halfH: 45};
    var rulerDegLbl = el('span', {class: 'wb-ruler-deg', text: '۰°'});
    var rulerEl = el('div', {class: 'wb-ruler', style: 'display:none'});

    var SHAPE_NAMES = {
      line: 'خط', arrow: 'پیکان', dblarrow: 'پیکان دوسر', rect: 'مستطیل',
      circle: 'دایره', ellipse: 'بیضی', triangle: 'مثلث',
      rtriangle: 'مثلث قائم‌الزاویه', diamond: 'لوزی', axes: 'محور مختصات'
    };

    function uid() { return 'i' + (nextItemId++); }
    function newPage() { return {items: [], ink: null, data: null, grid: 'blank'}; }
    function clearInk() { inkCtx.clearRect(0, 0, W, H); }

    function cloneItem(it) {
      var c = {};
      Object.keys(it).forEach(function (k) {
        if (k === 'pts') c.pts = it.pts.map(function (pt) { return {x: pt.x, y: pt.y}; });
        else if (k === 'a' || k === 'b') c[k] = it[k] ? {x: it[k].x, y: it[k].y} : null;
        else c[k] = it[k];
      });
      return c;
    }
    function cloneItems(list) { return (list || []).map(cloneItem); }

    function ensureImg(srcUrl) {
      if (!srcUrl) return null;
      if (imgCache[srcUrl]) return imgCache[srcUrl];
      var im = new Image();
      imgCache[srcUrl] = im;
      im.onload = function () { renderComposite(true); };
      im.src = srcUrl;
      return im;
    }

    function drawGrid() {
      ctx.save();
      ctx.strokeStyle = '#d7dce5'; ctx.lineWidth = 1;
      if (grid === 'mm') {
        ctx.strokeStyle = '#eef2f7'; ctx.lineWidth = 0.8;
        for (var mx = 0; mx <= W; mx += 10) { ctx.beginPath(); ctx.moveTo(mx, 0); ctx.lineTo(mx, H); ctx.stroke(); }
        for (var my = 0; my <= H; my += 10) { ctx.beginPath(); ctx.moveTo(0, my); ctx.lineTo(W, my); ctx.stroke(); }
        ctx.strokeStyle = '#cbd5e1'; ctx.lineWidth = 1.2;
        for (var mx5 = 0; mx5 <= W; mx5 += 50) { ctx.beginPath(); ctx.moveTo(mx5, 0); ctx.lineTo(mx5, H); ctx.stroke(); }
        for (var my5 = 0; my5 <= H; my5 += 50) { ctx.beginPath(); ctx.moveTo(0, my5); ctx.lineTo(W, my5); ctx.stroke(); }
      }
      if (grid === 'dots') {
        ctx.fillStyle = '#94a3b8';
        for (var dx = 40; dx < W; dx += 40) {
          for (var dy = 40; dy < H; dy += 40) {
            ctx.beginPath(); ctx.arc(dx, dy, 1.8, 0, Math.PI * 2); ctx.fill();
          }
        }
      }
      if (grid === 'iso') {
        ctx.fillStyle = '#94a3b8';
        var rowH = 34.64, colW = 40, rowIdx = 0;
        for (var iy = 36; iy < H - 20; iy += rowH, rowIdx++) {
          var off = (rowIdx % 2) * (colW / 2);
          for (var ix = 36 + off; ix < W - 20; ix += colW) {
            ctx.beginPath(); ctx.arc(ix, iy, 1.8, 0, Math.PI * 2); ctx.fill();
          }
        }
      }
      if (grid === 'grid' || grid === 'quad' || grid === 'first') {
        for (var x = 0; x <= W; x += 40) { ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, H); ctx.stroke(); }
        for (var y = 0; y <= H; y += 40) { ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(W, y); ctx.stroke(); }
      }
      ctx.strokeStyle = '#333'; ctx.lineWidth = 2; ctx.fillStyle = '#333'; ctx.font = '18px Vazirmatn, sans-serif';
      function arrow(x1, y1, x2, y2) {
        ctx.beginPath(); ctx.moveTo(x1, y1); ctx.lineTo(x2, y2); ctx.stroke();
        var a = Math.atan2(y2 - y1, x2 - x1);
        ctx.beginPath(); ctx.moveTo(x2, y2);
        ctx.lineTo(x2 - 12 * Math.cos(a - 0.4), y2 - 12 * Math.sin(a - 0.4));
        ctx.lineTo(x2 - 12 * Math.cos(a + 0.4), y2 - 12 * Math.sin(a + 0.4));
        ctx.closePath(); ctx.fill();
      }
      if (grid === 'quad') {
        var cx = W / 2, cy = H / 2;
        arrow(20, cy, W - 20, cy); arrow(cx, H - 20, cx, 20);
        ctx.fillText('x', W - 30, cy - 10); ctx.fillText('y', cx + 10, 30);
        for (var i = -15; i <= 15; i++) {
          if (!i) continue;
          var px = cx + i * 40, py = cy - i * 40;
          if (px > 0 && px < W) { ctx.beginPath(); ctx.moveTo(px, cy - 5); ctx.lineTo(px, cy + 5); ctx.stroke(); if (i % 2 === 0) ctx.fillText(fa(i), px - 8, cy + 24); }
          if (py > 0 && py < H) { ctx.beginPath(); ctx.moveTo(cx - 5, py); ctx.lineTo(cx + 5, py); ctx.stroke(); if (i % 2 === 0) ctx.fillText(fa(i), cx + 10, py + 6); }
        }
      }
      if (grid === 'first') {
        var ox = 80, oy = H - 80;
        arrow(ox, oy, W - 20, oy); arrow(ox, oy, ox, 20);
        ctx.fillText('x', W - 30, oy - 10); ctx.fillText('y', ox + 10, 30);
        for (var j = 1; j <= 30; j++) {
          var qx = ox + j * 40, qy = oy - j * 40;
          if (qx < W - 30) { ctx.beginPath(); ctx.moveTo(qx, oy - 5); ctx.lineTo(qx, oy + 5); ctx.stroke(); if (j % 2 === 0) ctx.fillText(fa(j), qx - 8, oy + 24); }
          if (qy > 30) { ctx.beginPath(); ctx.moveTo(ox - 5, qy); ctx.lineTo(ox + 5, qy); ctx.stroke(); if (j % 2 === 0) ctx.fillText(fa(j), ox - 34, qy + 6); }
        }
      }
      if (grid === 'line') {
        var ly = H / 2; arrow(20, ly, W - 20, ly);
        for (var n = -16; n <= 16; n++) {
          var nx = W / 2 + n * 40;
          if (nx > 30 && nx < W - 30) { ctx.beginPath(); ctx.moveTo(nx, ly - 8); ctx.lineTo(nx, ly + 8); ctx.stroke(); ctx.fillText(fa(n), nx - 6, ly + 30); }
        }
      }
      if (grid === 'polar') {
        var pcx = W / 2, pcy = H / 2; ctx.strokeStyle = '#c5cad6';
        for (var r = 60; r < 520; r += 60) { ctx.beginPath(); ctx.arc(pcx, pcy, r, 0, Math.PI * 2); ctx.stroke(); }
        for (var d = 0; d < 360; d += 30) {
          var rad = d * Math.PI / 180;
          ctx.beginPath(); ctx.moveTo(pcx, pcy); ctx.lineTo(pcx + 480 * Math.cos(rad), pcy - 480 * Math.sin(rad)); ctx.stroke();
          ctx.fillText(fa(d) + '°', pcx + 500 * Math.cos(rad) - 12, pcy - 500 * Math.sin(rad) + 6);
        }
        ctx.strokeStyle = '#333'; arrow(pcx - 500, pcy, pcx + 500, pcy); arrow(pcx, pcy + 500, pcx, pcy - 500);
      }
      if (grid === 'lined') {
        ctx.strokeStyle = '#cfd6e4';
        for (var yy = 60; yy < H; yy += 50) { ctx.beginPath(); ctx.moveTo(40, yy); ctx.lineTo(W - 40, yy); ctx.stroke(); }
      }
      ctx.restore();
    }

    function applyDashFor(c, w, st) {
      if (st === 'dashed') c.setLineDash([w * 3.5 + 7, w * 2.2 + 5]);
      else if (st === 'dotted') c.setLineDash([Math.max(1.5, w * 0.9), w * 2.4 + 4]);
      else c.setLineDash([]);
    }
    function hexRgba(hex, alpha) {
      var h = (hex || '#1a237e').replace('#', '');
      if (h.length === 3) h = h[0] + h[0] + h[1] + h[1] + h[2] + h[2];
      var rv = parseInt(h.slice(0, 2), 16) || 0, gv = parseInt(h.slice(2, 4), 16) || 0, bv = parseInt(h.slice(4, 6), 16) || 0;
      return 'rgba(' + rv + ',' + gv + ',' + bv + ',' + alpha + ')';
    }

    function drawArrowHead(c, x1, y1, x2, y2, headLen) {
      var an = Math.atan2(y2 - y1, x2 - x1);
      c.save(); c.setLineDash([]);
      c.beginPath();
      c.moveTo(x2, y2); c.lineTo(x2 - headLen * Math.cos(an - 0.45), y2 - headLen * Math.sin(an - 0.45));
      c.moveTo(x2, y2); c.lineTo(x2 - headLen * Math.cos(an + 0.45), y2 - headLen * Math.sin(an + 0.45));
      c.stroke(); c.restore();
    }

    function measureTextDims(txt, fontPx) {
      var lines = String(txt || '').split('\n');
      ctx.save();
      ctx.font = fontPx + 'px Vazirmatn, sans-serif';
      var maxW = 24;
      for (var i = 0; i < lines.length; i++) {
        var mw = ctx.measureText(lines[i]).width;
        if (mw > maxW) maxW = mw;
      }
      ctx.restore();
      var lh = Math.round(fontPx * 1.32);
      return {w: Math.ceil(maxW) + 8, h: Math.max(lh, lines.length * lh), lh: lh, lines: lines};
    }

    function drawItemLabel(c, label, cx, cy, col, sz) {
      if (!label) return;
      var fpx = Math.max(15, (sz || 3) * 3 + 12);
      c.save();
      c.setLineDash([]);
      c.font = 'bold ' + fpx + 'px Vazirmatn, sans-serif';
      c.direction = 'rtl';
      c.textAlign = 'center';
      c.textBaseline = 'middle';
      var tw = c.measureText(label).width + 14, th = fpx + 8;
      c.fillStyle = 'rgba(255,255,255,0.86)';
      c.fillRect(cx - tw / 2, cy - th / 2, tw, th);
      c.fillStyle = col || '#1a237e';
      c.fillText(label, cx, cy);
      c.restore();
    }

    function drawShapeItem(c, shKind, a, b, col, sz, lStyle, filled, label) {
      c.save();
      c.strokeStyle = col;
      c.lineWidth = sz;
      c.lineCap = 'round';
      c.lineJoin = 'round';
      applyDashFor(c, sz, lStyle);
      var headLen = Math.max(13, sz * 3 + 6);
      var closed = (shKind === 'rect' || shKind === 'circle' || shKind === 'ellipse' || shKind === 'triangle' || shKind === 'rtriangle' || shKind === 'diamond');
      c.beginPath();
      if (shKind === 'line' || shKind === 'arrow' || shKind === 'dblarrow') {
        c.moveTo(a.x, a.y); c.lineTo(b.x, b.y);
      } else if (shKind === 'rect') {
        c.rect(a.x, a.y, b.x - a.x, b.y - a.y);
      } else if (shKind === 'circle' || shKind === 'ellipse') {
        var ecx = (a.x + b.x) / 2, ecy = (a.y + b.y) / 2;
        c.ellipse(ecx, ecy, Math.max(1, Math.abs(b.x - a.x) / 2), Math.max(1, Math.abs(b.y - a.y) / 2), 0, 0, Math.PI * 2);
      } else if (shKind === 'triangle') {
        c.moveTo((a.x + b.x) / 2, a.y); c.lineTo(b.x, b.y); c.lineTo(a.x, b.y); c.closePath();
      } else if (shKind === 'rtriangle') {
        c.moveTo(a.x, a.y); c.lineTo(a.x, b.y); c.lineTo(b.x, b.y); c.closePath();
      } else if (shKind === 'diamond') {
        var dcx = (a.x + b.x) / 2, dcy = (a.y + b.y) / 2;
        c.moveTo(dcx, a.y); c.lineTo(b.x, dcy); c.lineTo(dcx, b.y); c.lineTo(a.x, dcy); c.closePath();
      } else if (shKind === 'axes') {
        var xMin = Math.min(a.x, b.x), xMax = Math.max(a.x, b.x), yMin = Math.min(a.y, b.y), yMax = Math.max(a.y, b.y);
        var axCx = (xMin + xMax) / 2, axCy = (yMin + yMax) / 2;
        c.moveTo(xMin, axCy); c.lineTo(xMax, axCy);
        c.moveTo(axCx, yMax); c.lineTo(axCx, yMin);
      }
      if (closed && filled) {
        c.fillStyle = hexRgba(col, 0.16);
        c.fill();
      }
      c.stroke();
      if (shKind === 'arrow') drawArrowHead(c, a.x, a.y, b.x, b.y, headLen);
      else if (shKind === 'dblarrow') {
        drawArrowHead(c, a.x, a.y, b.x, b.y, headLen);
        drawArrowHead(c, b.x, b.y, a.x, a.y, headLen);
      } else if (shKind === 'rtriangle' && Math.abs(b.x - a.x) > 22 && Math.abs(b.y - a.y) > 22) {
        var sq = Math.min(18, Math.abs(b.x - a.x) * 0.25, Math.abs(b.y - a.y) * 0.25);
        var sx = b.x >= a.x ? 1 : -1, sy = a.y <= b.y ? -1 : 1;
        c.save(); c.setLineDash([]); c.lineWidth = Math.max(1.5, sz * 0.75);
        c.beginPath();
        c.moveTo(a.x + sx * sq, b.y);
        c.lineTo(a.x + sx * sq, b.y + sy * sq);
        c.lineTo(a.x, b.y + sy * sq);
        c.stroke(); c.restore();
      } else if (shKind === 'axes' && Math.abs(b.x - a.x) > 30 && Math.abs(b.y - a.y) > 30) {
        var x0 = Math.min(a.x, b.x), x1 = Math.max(a.x, b.x), y0 = Math.min(a.y, b.y), y1 = Math.max(a.y, b.y);
        var mCx = (x0 + x1) / 2, mCy = (y0 + y1) / 2;
        drawArrowHead(c, x0, mCy, x1, mCy, 11);
        drawArrowHead(c, mCx, y1, mCx, y0, 11);
        c.save(); c.setLineDash([]); c.fillStyle = col; c.font = 'bold 16px Vazirmatn, sans-serif';
        c.fillText('x', x1 - 12, mCy - 8);
        c.fillText('y', mCx + 8, y0 + 16);
        c.restore();
      }
      if (label) drawItemLabel(c, label, (a.x + b.x) / 2, (a.y + b.y) / 2, col, sz);
      c.restore();
    }

    function drawStrokePts(c, pts, col, sz, lStyle, isHi, label) {
      if (!pts || !pts.length) return;
      c.save();
      if (isHi) {
        c.globalAlpha = 0.30;
        c.lineWidth = Math.max(16, sz * 6);
        c.setLineDash([]);
      } else {
        c.lineWidth = sz;
        applyDashFor(c, sz, lStyle);
      }
      c.strokeStyle = col;
      c.lineCap = 'round';
      c.lineJoin = 'round';
      c.beginPath();
      c.moveTo(pts[0].x, pts[0].y);
      for (var k = 1; k < pts.length; k++) c.lineTo(pts[k].x, pts[k].y);
      if (pts.length === 1) c.lineTo(pts[0].x + 0.5, pts[0].y);
      c.stroke();
      c.restore();
      if (label) {
        var bx = getStrokeBox(pts);
        drawItemLabel(c, label, bx.x + bx.w / 2, bx.y + bx.h / 2, col, sz);
      }
    }

    function drawSingleItem(c, it) {
      if (it.kind === 'stroke') {
        drawStrokePts(c, it.pts, it.color, it.size, it.lineStyle || 'solid', it.sub === 'highlighter', it.label);
      } else if (it.kind === 'shape') {
        drawShapeItem(c, it.shape, it.a, it.b, it.color, it.size, it.lineStyle || 'solid', !!it.fill, it.label);
      } else if (it.kind === 'text') {
        var fpx = it.fontPx || ((it.size || 3) * 6 + 14);
        var md = measureTextDims(it.text, fpx);
        it.w = md.w; it.h = md.h;
        c.save();
        c.fillStyle = it.color || '#1a237e';
        c.font = fpx + 'px Vazirmatn, sans-serif';
        c.direction = 'rtl';
        c.textAlign = 'right';
        c.textBaseline = 'top';
        for (var li = 0; li < md.lines.length; li++) {
          c.fillText(md.lines[li], it.x + md.w - 4, it.y + li * md.lh);
        }
        c.restore();
      } else if (it.kind === 'image') {
        var im = ensureImg(it.src);
        if (im && im.complete && im.naturalWidth > 0) {
          c.save();
          if (it.flipX) {
            c.translate(it.x + it.w, it.y);
            c.scale(-1, 1);
            c.drawImage(im, 0, 0, it.w, it.h);
          } else {
            c.drawImage(im, it.x, it.y, it.w, it.h);
          }
          c.restore();
        }
        if (it.label) drawItemLabel(c, it.label, it.x + it.w / 2, it.y + it.h / 2, it.color || '#1a237e', it.size || 3);
      }
    }

    function eraseAtRaster(x0, y0, x1, y1, rad) {
      inkCtx.save();
      inkCtx.globalCompositeOperation = 'destination-out';
      inkCtx.lineCap = 'round'; inkCtx.lineJoin = 'round';
      inkCtx.lineWidth = rad * 2;
      inkCtx.beginPath(); inkCtx.moveTo(x0, y0); inkCtx.lineTo(x1, y1); inkCtx.stroke();
      inkCtx.restore();
    }

    function getStrokeBox(pts) {
      if (!pts || !pts.length) return {x: 0, y: 0, w: 0, h: 0};
      var minX = pts[0].x, maxX = pts[0].x, minY = pts[0].y, maxY = pts[0].y;
      for (var i = 1; i < pts.length; i++) {
        if (pts[i].x < minX) minX = pts[i].x;
        if (pts[i].x > maxX) maxX = pts[i].x;
        if (pts[i].y < minY) minY = pts[i].y;
        if (pts[i].y > maxY) maxY = pts[i].y;
      }
      return {x: minX, y: minY, w: maxX - minX, h: maxY - minY};
    }

    function getItemBox(it) {
      if (it.kind === 'stroke') return getStrokeBox(it.pts);
      if (it.kind === 'shape') {
        return {
          x: Math.min(it.a.x, it.b.x),
          y: Math.min(it.a.y, it.b.y),
          w: Math.abs(it.b.x - it.a.x),
          h: Math.abs(it.b.y - it.a.y)
        };
      }
      if (it.kind === 'text') {
        var fpx = it.fontPx || ((it.size || 3) * 6 + 14);
        var md = measureTextDims(it.text, fpx);
        it.w = md.w; it.h = md.h;
        return {x: it.x, y: it.y, w: md.w, h: md.h};
      }
      if (it.kind === 'image') return {x: it.x, y: it.y, w: it.w, h: it.h};
      return {x: 0, y: 0, w: 0, h: 0};
    }

    function unionBox(list) {
      if (!list || !list.length) return null;
      var b0 = getItemBox(list[0]);
      var minX = b0.x, minY = b0.y, maxX = b0.x + b0.w, maxY = b0.y + b0.h;
      for (var i = 1; i < list.length; i++) {
        var bi = getItemBox(list[i]);
        if (bi.x < minX) minX = bi.x;
        if (bi.y < minY) minY = bi.y;
        if (bi.x + bi.w > maxX) maxX = bi.x + bi.w;
        if (bi.y + bi.h > maxY) maxY = bi.y + bi.h;
      }
      return {x: minX, y: minY, w: maxX - minX, h: maxY - minY};
    }

    function distToSeg(px, py, x1, y1, x2, y2) {
      var dx = x2 - x1, dy = y2 - y1;
      var l2 = dx * dx + dy * dy;
      if (l2 < 1e-4) return Math.hypot(px - x1, py - y1);
      var t = Math.max(0, Math.min(1, ((px - x1) * dx + (py - y1) * dy) / l2));
      return Math.hypot(px - (x1 + t * dx), py - (y1 + t * dy));
    }

    function hitItem(it, px, py) {
      var pad = Math.max(12, (it.size || 3) * 2 + 8);
      if (it.kind === 'stroke') {
        var extra = it.sub === 'highlighter' ? Math.max(12, (it.size || 3) * 3) : pad;
        var sb = getStrokeBox(it.pts);
        if (px < sb.x - extra || px > sb.x + sb.w + extra || py < sb.y - extra || py > sb.y + sb.h + extra) return false;
        if (it.pts.length === 1) return Math.hypot(px - it.pts[0].x, py - it.pts[0].y) <= extra;
        for (var i = 1; i < it.pts.length; i++) {
          if (distToSeg(px, py, it.pts[i - 1].x, it.pts[i - 1].y, it.pts[i].x, it.pts[i].y) <= extra) return true;
        }
        return false;
      }
      if (it.kind === 'shape') {
        if (it.shape === 'line' || it.shape === 'arrow' || it.shape === 'dblarrow') {
          if (distToSeg(px, py, it.a.x, it.a.y, it.b.x, it.b.y) <= pad) return true;
          if (it.label && Math.hypot(px - (it.a.x + it.b.x) / 2, py - (it.a.y + it.b.y) / 2) <= 32) return true;
          return false;
        }
        var bx = getItemBox(it);
        return px >= bx.x - pad && px <= bx.x + bx.w + pad && py >= bx.y - pad && py <= bx.y + bx.h + pad;
      }
      var b = getItemBox(it);
      return px >= b.x - 8 && px <= b.x + b.w + 8 && py >= b.y - 8 && py <= b.y + b.h + 8;
    }

    function selectedItems() {
      if (!selectedIds.length) return [];
      var set = {};
      selectedIds.forEach(function (id) { set[id] = true; });
      return items.filter(function (it) { return !!set[it.id]; });
    }

    function isSingleLineLike(sel) {
      return sel.length === 1 && sel[0].kind === 'shape' &&
        (sel[0].shape === 'line' || sel[0].shape === 'arrow' || sel[0].shape === 'dblarrow');
    }

    function getHandlesForBox(ub) {
      var pad = 6;
      var x = ub.x - pad, y = ub.y - pad, w = Math.max(12, ub.w + pad * 2), h = Math.max(12, ub.h + pad * 2);
      return [
        {id: 'nw', x: x, y: y, cur: 'nwse-resize'},
        {id: 'n', x: x + w / 2, y: y, cur: 'ns-resize'},
        {id: 'ne', x: x + w, y: y, cur: 'nesw-resize'},
        {id: 'e', x: x + w, y: y + h / 2, cur: 'ew-resize'},
        {id: 'se', x: x + w, y: y + h, cur: 'nwse-resize'},
        {id: 's', x: x + w / 2, y: y + h, cur: 'ns-resize'},
        {id: 'sw', x: x, y: y + h, cur: 'nesw-resize'},
        {id: 'w', x: x, y: y + h / 2, cur: 'ew-resize'}
      ];
    }

    function drawSelectionOverlay(c) {
      var sel = selectedItems();
      if (sel.length) {
        c.save();
        if (isSingleLineLike(sel)) {
          var ln = sel[0];
          c.setLineDash([6, 5]);
          c.strokeStyle = '#1e5eff';
          c.lineWidth = 1.6;
          c.beginPath(); c.moveTo(ln.a.x, ln.a.y); c.lineTo(ln.b.x, ln.b.y); c.stroke();
          c.setLineDash([]);
          [ln.a, ln.b].forEach(function (ep) {
            c.fillStyle = '#FFFFFF';
            c.strokeStyle = '#1e5eff';
            c.lineWidth = 2.5;
            c.beginPath(); c.arc(ep.x, ep.y, 8, 0, Math.PI * 2); c.fill(); c.stroke();
          });
        } else {
          var ub = unionBox(sel);
          if (ub) {
            var pad = 6;
            var rx = ub.x - pad, ry = ub.y - pad, rw = Math.max(12, ub.w + pad * 2), rh = Math.max(12, ub.h + pad * 2);
            c.strokeStyle = '#1e5eff';
            c.lineWidth = 2;
            c.setLineDash([7, 5]);
            c.strokeRect(rx, ry, rw, rh);
            c.setLineDash([]);
            getHandlesForBox(ub).forEach(function (hd) {
              c.fillStyle = '#FFFFFF';
              c.strokeStyle = '#1e5eff';
              c.lineWidth = 2.2;
              c.beginPath();
              c.rect(hd.x - 6.5, hd.y - 6.5, 13, 13);
              c.fill();
              c.stroke();
            });
          }
        }
        c.restore();
      }
      if (selDrag && selDrag.mode === 'marquee' && selDrag.cur) {
        var mx = Math.min(selDrag.start.x, selDrag.cur.x), my = Math.min(selDrag.start.y, selDrag.cur.y);
        var mw = Math.abs(selDrag.cur.x - selDrag.start.x), mh = Math.abs(selDrag.cur.y - selDrag.start.y);
        c.save();
        c.fillStyle = 'rgba(30,94,255,0.09)';
        c.strokeStyle = '#1e5eff';
        c.lineWidth = 1.8;
        c.setLineDash([6, 4]);
        c.fillRect(mx, my, mw, mh);
        c.strokeRect(mx, my, mw, mh);
        c.restore();
      }
    }

    function renderComposite(showUi, previewFn) {
      if (showUi === undefined) showUi = true;
      clearInk();
      for (var i = 0; i < items.length; i++) drawSingleItem(inkCtx, items[i]);
      ctx.fillStyle = '#FFFFFF';
      ctx.fillRect(0, 0, W, H);
      drawGrid();
      ctx.drawImage(inkCanvas, 0, 0);
      if (previewFn) previewFn(ctx);
      if (showUi && tool === 'select') drawSelectionOverlay(ctx);
    }

    function blank() { items = []; selectedIds = []; syncSelectionUi(); renderComposite(true); }

    function updateHistoryBtns() {
      if (undoBtn) undoBtn.disabled = !undoStack.length;
      if (redoBtn) redoBtn.disabled = !redoStack.length;
    }
    function pushUndo() {
      undoStack.push(cloneItems(items));
      if (undoStack.length > 40) undoStack.shift();
      redoStack = [];
      updateHistoryBtns();
    }
    function doUndo() {
      if (!undoStack.length) return;
      redoStack.push(cloneItems(items));
      items = undoStack.pop();
      selectedIds = selectedIds.filter(function (id) { return items.some(function (it) { return it.id === id; }); });
      updateHistoryBtns();
      syncSelectionUi();
      renderComposite(true);
    }
    function doRedo() {
      if (!redoStack.length) return;
      undoStack.push(cloneItems(items));
      items = redoStack.pop();
      selectedIds = selectedIds.filter(function (id) { return items.some(function (it) { return it.id === id; }); });
      updateHistoryBtns();
      syncSelectionUi();
      renderComposite(true);
    }

    function translateItem(it, dx, dy) {
      if (it.kind === 'stroke') {
        for (var i = 0; i < it.pts.length; i++) { it.pts[i].x += dx; it.pts[i].y += dy; }
      } else if (it.kind === 'shape') {
        it.a.x += dx; it.a.y += dy; it.b.x += dx; it.b.y += dy;
      } else if (it.kind === 'text' || it.kind === 'image') {
        it.x += dx; it.y += dy;
      }
    }

    function applyScaledFromOrig(it, orig, ob, nb) {
      function mx(vx) { return ob.w > 1 ? nb.x + (vx - ob.x) / ob.w * nb.w : nb.x + nb.w / 2; }
      function my(vy) { return ob.h > 1 ? nb.y + (vy - ob.y) / ob.h * nb.h : nb.y + nb.h / 2; }
      if (orig.kind === 'stroke') {
        it.pts = orig.pts.map(function (pt) { return {x: mx(pt.x), y: my(pt.y)}; });
      } else if (orig.kind === 'shape') {
        it.a = {x: mx(orig.a.x), y: my(orig.a.y)};
        it.b = {x: mx(orig.b.x), y: my(orig.b.y)};
      } else if (orig.kind === 'image') {
        var x1 = mx(orig.x), y1 = my(orig.y), x2 = mx(orig.x + orig.w), y2 = my(orig.y + orig.h);
        it.x = Math.min(x1, x2); it.y = Math.min(y1, y2);
        it.w = Math.max(20, Math.abs(x2 - x1)); it.h = Math.max(20, Math.abs(y2 - y1));
      } else if (orig.kind === 'text') {
        var origFpx = orig.fontPx || ((orig.size || 3) * 6 + 14);
        var obItem = getItemBox(cloneItem(orig));
        var sx = ob.w > 1 ? nb.w / ob.w : 1;
        var sy = ob.h > 1 ? nb.h / ob.h : 1;
        var sc = Math.max(0.25, (sx + sy) / 2);
        it.fontPx = Math.max(12, Math.min(160, Math.round(origFpx * sc)));
        it.x = mx(obItem.x);
        it.y = my(obItem.y);
      }
    }

    function scaleSelectedBy(factor) {
      var sel = selectedItems();
      if (!sel.length) return;
      var ub = unionBox(sel);
      if (!ub) return;
      pushUndo();
      var cx = ub.x + ub.w / 2, cy = ub.y + ub.h / 2;
      var nw = Math.max(16, ub.w * factor), nh = Math.max(16, ub.h * factor);
      var nb = {x: cx - nw / 2, y: cy - nh / 2, w: nw, h: nh};
      sel.forEach(function (it) {
        if (sel.length === 1 && it.kind === 'text') {
          var f0 = it.fontPx || ((it.size || 3) * 6 + 14);
          var nf = Math.max(12, Math.min(160, Math.round(f0 * factor)));
          var oldD = measureTextDims(it.text, f0), newD = measureTextDims(it.text, nf);
          it.fontPx = nf;
          it.x = it.x + (oldD.w - newD.w) / 2;
          it.y = it.y + (oldD.h - newD.h) / 2;
        } else {
          applyScaledFromOrig(it, cloneItem(it), ub, nb);
        }
      });
      renderComposite(true);
    }

    function flipSelectedHoriz() {
      var sel = selectedItems();
      if (!sel.length) return;
      var ub = unionBox(sel);
      if (!ub) return;
      pushUndo();
      var cx = ub.x + ub.w / 2;
      sel.forEach(function (it) {
        if (it.kind === 'stroke') {
          it.pts.forEach(function (pt) { pt.x = 2 * cx - pt.x; });
        } else if (it.kind === 'shape') {
          it.a.x = 2 * cx - it.a.x;
          it.b.x = 2 * cx - it.b.x;
        } else if (it.kind === 'image') {
          it.x = 2 * cx - (it.x + it.w);
          it.flipX = !it.flipX;
        } else if (it.kind === 'text') {
          var bx = getItemBox(it);
          it.x = 2 * cx - (bx.x + bx.w);
        }
      });
      renderComposite(true);
    }

    function deleteSelected() {
      if (!selectedIds.length) return;
      pushUndo();
      var set = {};
      selectedIds.forEach(function (id) { set[id] = true; });
      items = items.filter(function (it) { return !set[it.id]; });
      selectedIds = [];
      syncSelectionUi();
      renderComposite(true);
    }

    function duplicateSelected() {
      var sel = selectedItems();
      if (!sel.length) return;
      pushUndo();
      var newIds = [];
      sel.forEach(function (it) {
        var cp = cloneItem(it);
        cp.id = uid();
        translateItem(cp, 26, 26);
        items.push(cp);
        newIds.push(cp.id);
      });
      selectedIds = newIds;
      setActiveTool('select', true);
      syncSelectionUi();
      renderComposite(true);
    }

    function wbPromptText(title, body, label, initialVal, allowEmpty) {
      return new Promise(function (resolve) {
        var done = false;
        var mbg = el('div', {class: 'modal-bg over-engine'});
        function finish(val) { if (done) return; done = true; mbg.remove(); resolve(val); }
        mbg._onCancel = function () { finish(null); };
        var inp = el('input', {type: 'text', value: initialVal || '', placeholder: allowEmpty ? 'خالی بگذارید تا برچسب حذف شود' : ''});
        var okBtn = el('button', {class: 'btn', text: 'ثبت', onclick: function () {
          if (!allowEmpty && !inp.value.trim()) return;
          finish(inp.value);
        }});
        if (!allowEmpty) okBtn.disabled = !(initialVal || '').trim();
        inp.addEventListener('input', function () {
          if (!allowEmpty) okBtn.disabled = !inp.value.trim();
        });
        inp.addEventListener('keydown', function (ev) {
          if (ev.key === 'Enter') { ev.preventDefault(); if (allowEmpty || inp.value.trim()) okBtn.click(); }
          if (ev.key === 'Escape') { ev.preventDefault(); ev.stopPropagation(); finish(null); }
        });
        var modal = el('div', {class: 'modal'}, [
          el('h2', {text: title}),
          body ? el('p', {class: 'muted', text: body}) : null,
          el('div', {class: 'field'}, [el('label', {text: label || ''}), inp]),
          el('div', {class: 'row', style: 'justify-content:flex-start;margin-top:14px'}, [
            okBtn,
            el('button', {class: 'btn light', text: 'انصراف', onclick: function () { finish(null); }})
          ])
        ]);
        mbg.addEventListener('click', function (ev) { if (ev.target === mbg) finish(null); });
        mbg.appendChild(modal);
        document.body.appendChild(mbg);
        setTimeout(function () { inp.focus(); inp.select(); }, 25);
      });
    }

    async function editSelectedItem() {
      var sel = selectedItems();
      if (!sel.length) return;
      var target = sel[0];
      if (target.kind === 'text') {
        var nv = await wbPromptText('ویرایش متن درج‌شده', 'متن جدید را وارد کنید:', 'متن', target.text || '', false);
        if (nv !== null && nv !== undefined && nv.trim()) {
          pushUndo();
          target.text = nv.trim();
          var md = measureTextDims(target.text, target.fontPx || (target.size * 6 + 14));
          target.w = md.w; target.h = md.h;
          syncSelectionUi();
          renderComposite(true);
        }
      } else {
        var curLbl = target.label || '';
        var lv = await wbPromptText('ویرایش برچسب / متن روی مورد', 'متن دلخواه روی این شکل یا مورد را بنویسید:', 'برچسب', curLbl, true);
        if (lv !== null && lv !== undefined) {
          pushUndo();
          target.label = lv.trim();
          syncSelectionUi();
          renderComposite(true);
        }
      }
    }

    function itemTitleFa(it) {
      if (it.kind === 'text') return 'متن «' + (it.text.length > 14 ? it.text.slice(0, 14) + '…' : it.text) + '»';
      if (it.kind === 'image') return 'تصویر';
      if (it.kind === 'stroke') return it.sub === 'highlighter' ? 'ماژیک هایلایت' : 'دست‌خط قلم';
      if (it.kind === 'shape') return SHAPE_NAMES[it.shape] || 'شکل';
      return 'مورد';
    }

    var selActionBtns = [];
    function syncSelectionUi() {
      var sel = selectedItems();
      var has = sel.length > 0;
      selActionBtns.forEach(function (b) { b.disabled = !has; });
      if (!has) {
        selBadge.textContent = tool === 'select' ? 'روی یک مورد کلیک کنید یا کادر بکشید' : 'موردی انتخاب نشده است';
        selBadge.classList.remove('on');
      } else {
        selBadge.textContent = sel.length === 1 ? ('✓ انتخاب‌شده: ' + itemTitleFa(sel[0])) : ('✓ ' + fa(sel.length) + ' مورد انتخاب شده');
        selBadge.classList.add('on');
        var f = sel[0];
        if (f.color) syncColorUiOnly(f.color);
        if (f.size) { size = f.size; sizeIn.value = size; sizeVal.textContent = fa(size); }
        if (f.lineStyle && styleBtns[f.lineStyle]) {
          lineStyle = f.lineStyle;
          Object.keys(styleBtns).forEach(function (k) { styleBtns[k].classList.toggle('on', k === lineStyle); });
        }
        if (f.kind === 'shape') {
          fillShape = !!f.fill;
          if (fillBtn) fillBtn.classList.toggle('on', fillShape);
        }
      }
    }

    function snapPointToRuler(p, checkOnly) {
      if (!rulerOn) return null;
      var rad = rulerState.deg * Math.PI / 180;
      var cos = Math.cos(rad), sin = Math.sin(rad);
      var dx = p.x - rulerState.x, dy = p.y - rulerState.y;
      var lx = dx * cos + dy * sin;
      var ly = -dx * sin + dy * cos;
      if (Math.abs(lx) > rulerState.len / 2 + 24) return null;
      if (checkOnly) {
        if (Math.abs(ly + rulerState.halfH) <= 28) return -rulerState.halfH;
        if (Math.abs(ly - rulerState.halfH) <= 28) return rulerState.halfH;
        return null;
      }
      var clampedX = Math.max(-rulerState.len / 2, Math.min(rulerState.len / 2, lx));
      var edgeY = rulerSnapEdge;
      return {
        x: rulerState.x + clampedX * cos - edgeY * sin,
        y: rulerState.y + clampedX * sin + edgeY * cos
      };
    }

    function constrainPoint(a, b, shiftKey) {
      if (!shiftKey) return b;
      var dx = b.x - a.x, dy = b.y - a.y;
      if (tool === 'line' || tool === 'arrow' || tool === 'dblarrow' || tool === 'select') {
        var dist = Math.hypot(dx, dy);
        var ang = Math.round(Math.atan2(dy, dx) / (Math.PI / 12)) * (Math.PI / 12);
        return {x: a.x + dist * Math.cos(ang), y: a.y + dist * Math.sin(ang)};
      }
      if (tool === 'rect' || tool === 'ellipse' || tool === 'triangle' || tool === 'rtriangle' || tool === 'diamond' || tool === 'axes') {
        var m = Math.max(Math.abs(dx), Math.abs(dy));
        return {x: a.x + (dx >= 0 ? m : -m), y: a.y + (dy >= 0 ? m : -m)};
      }
      return b;
    }

    function updateHud(a, b) {
      var dx = b.x - a.x, dy = b.y - a.y;
      var txt = '';
      if (tool === 'line' || tool === 'arrow' || tool === 'dblarrow') {
        var lenU = (Math.hypot(dx, dy) / 40).toFixed(1);
        var deg = Math.round((-Math.atan2(dy, dx) * 180 / Math.PI + 360) % 360);
        txt = 'طول: ' + fa(lenU) + ' واحد  •  زاویه: ' + fa(deg) + '°';
      } else if (tool === 'circle') {
        txt = 'شعاع: ' + fa((Math.hypot(dx, dy) / 40).toFixed(1)) + ' واحد';
      } else if (tool !== 'pen' && tool !== 'highlighter' && tool !== 'eraser' && tool !== 'text' && tool !== 'select') {
        txt = 'ابعاد: ' + fa((Math.abs(dx) / 40).toFixed(1)) + ' × ' + fa((Math.abs(dy) / 40).toFixed(1)) + ' واحد';
      }
      if (txt) { hud.textContent = txt; hud.style.display = ''; }
      else hud.style.display = 'none';
    }

    function pos(e) {
      var r = canvas.getBoundingClientRect();
      var t = e.touches ? e.touches[0] : e;
      return {x: (t.clientX - r.left) * W / r.width, y: (t.clientY - r.top) * H / r.height};
    }

    function hitHandle(p) {
      var sel = selectedItems();
      if (!sel.length) return null;
      var rHit = 16;
      if (isSingleLineLike(sel)) {
        if (Math.hypot(p.x - sel[0].a.x, p.y - sel[0].a.y) <= rHit) return {mode: 'end-a', cur: 'pointer'};
        if (Math.hypot(p.x - sel[0].b.x, p.y - sel[0].b.y) <= rHit) return {mode: 'end-b', cur: 'pointer'};
        return null;
      }
      var ub = unionBox(sel);
      if (!ub) return null;
      var hs = getHandlesForBox(ub);
      for (var i = 0; i < hs.length; i++) {
        if (Math.hypot(p.x - hs[i].x, p.y - hs[i].y) <= rHit) return {mode: 'resize', handle: hs[i].id, cur: hs[i].cur};
      }
      return null;
    }

    function topHitItem(p) {
      for (var i = items.length - 1; i >= 0; i--) {
        if (hitItem(items[i], p.x, p.y)) return items[i];
      }
      return null;
    }

    function eraseStrokesSegment(p0, p1, rad) {
      eraseAtRaster(p0.x, p0.y, p1.x, p1.y, rad);
      var out = [];
      for (var i = 0; i < items.length; i++) {
        var it = items[i];
        if (it.kind !== 'stroke') { out.push(it); continue; }
        var curSeg = [];
        for (var k = 0; k < it.pts.length; k++) {
          var pt = it.pts[k];
          if (distToSeg(pt.x, pt.y, p0.x, p0.y, p1.x, p1.y) <= rad) {
            if (curSeg.length >= 2) {
              var s1 = cloneItem(it); s1.id = uid(); s1.pts = curSeg; out.push(s1);
            }
            curSeg = [];
          } else {
            curSeg.push(pt);
          }
        }
        if (curSeg.length === it.pts.length) out.push(it);
        else if (curSeg.length >= 2) {
          var s2 = cloneItem(it); s2.id = uid(); s2.pts = curSeg; out.push(s2);
        }
      }
      items = out;
    }

    function computeResizedBox(ob, handle, dx, dy, keepRatio) {
      var x1 = ob.x, y1 = ob.y, x2 = ob.x + ob.w, y2 = ob.y + ob.h;
      if (handle.indexOf('w') >= 0) x1 += dx;
      if (handle.indexOf('e') >= 0) x2 += dx;
      if (handle.indexOf('n') >= 0) y1 += dy;
      if (handle.indexOf('s') >= 0) y2 += dy;
      var nx = Math.min(x1, x2), ny = Math.min(y1, y2);
      var nw = Math.max(16, Math.abs(x2 - x1)), nh = Math.max(16, Math.abs(y2 - y1));
      if (keepRatio && ob.w > 4 && ob.h > 4 && handle.length === 2) {
        var ratio = ob.w / ob.h;
        if (nw / nh > ratio) nh = Math.max(16, nw / ratio);
        else nw = Math.max(16, nh * ratio);
        if (handle.indexOf('w') >= 0) nx = x2 - nw;
        if (handle.indexOf('n') >= 0) ny = y2 - nh;
      }
      return {x: nx, y: ny, w: nw, h: nh};
    }

    var eraserUndoPushed = false;

    function down(e) {
      e.preventDefault();
      var p0 = pos(e);

      if (tool === 'select') {
        var hd = hitHandle(p0);
        if (hd) {
          var sel0 = selectedItems();
          var map0 = {};
          sel0.forEach(function (it) { map0[it.id] = cloneItem(it); });
          selDrag = {
            mode: hd.mode, handle: hd.handle, start: p0, cur: p0,
            origBox: unionBox(sel0), origMap: map0, moved: false
          };
          drawing = true;
          return;
        }
        var hit = topHitItem(p0);
        if (hit) {
          if (e.shiftKey) {
            var idx = selectedIds.indexOf(hit.id);
            if (idx >= 0) selectedIds.splice(idx, 1);
            else selectedIds.push(hit.id);
          } else if (selectedIds.indexOf(hit.id) < 0) {
            selectedIds = [hit.id];
          }
          syncSelectionUi();
          renderComposite(true);
          var selM = selectedItems();
          var mapM = {};
          selM.forEach(function (it) { mapM[it.id] = cloneItem(it); });
          selDrag = {mode: 'move', start: p0, cur: p0, origMap: mapM, moved: false};
          drawing = true;
          return;
        }
        var ubCur = unionBox(selectedItems());
        if (ubCur && p0.x >= ubCur.x && p0.x <= ubCur.x + ubCur.w && p0.y >= ubCur.y && p0.y <= ubCur.y + ubCur.h) {
          var selB = selectedItems();
          var mapB = {};
          selB.forEach(function (it) { mapB[it.id] = cloneItem(it); });
          selDrag = {mode: 'move', start: p0, cur: p0, origMap: mapB, moved: false};
          drawing = true;
          return;
        }
        if (!e.shiftKey) selectedIds = [];
        syncSelectionUi();
        selDrag = {mode: 'marquee', start: p0, cur: p0, baseIds: selectedIds.slice()};
        drawing = true;
        renderComposite(true);
        return;
      }

      if (tool === 'text') {
        drawing = false;
        wbPromptText('درج متن روی تخته', 'متن مورد نظر را وارد کنید:', 'متن', '', false).then(function (t) {
          if (t && t.trim()) {
            pushUndo();
            var fpx = size * 6 + 14;
            var md = measureTextDims(t.trim(), fpx);
            var tx = Math.max(10, Math.min(W - md.w - 10, p0.x - md.w));
            var ty = Math.max(10, Math.min(H - md.h - 10, p0.y - Math.round(fpx * 0.75)));
            var newTxt = {id: uid(), kind: 'text', x: tx, y: ty, w: md.w, h: md.h, fontPx: fpx, text: t.trim(), color: color, size: size};
            items.push(newTxt);
            renderComposite(true);
          }
        });
        return;
      }

      drawing = true;
      if (tool === 'eraser') {
        eraserUndoPushed = false;
        last = p0;
        return;
      }
      rulerSnapEdge = (tool === 'pen') ? snapPointToRuler(p0, true) : null;
      if (rulerSnapEdge !== null) p0 = snapPointToRuler(p0, false) || p0;
      last = startPt = endPt = p0;
      penPts = [p0];
      if (tool === 'pen' || tool === 'highlighter') {
        renderComposite(false, function (c) {
          drawStrokePts(c, penPts, color, size, lineStyle, tool === 'highlighter', '');
        });
      }
    }

    function move(e) {
      var p = pos(e);
      if (!drawing) {
        if (tool === 'select') {
          var hh = hitHandle(p);
          if (hh) canvas.style.cursor = hh.cur;
          else if (topHitItem(p)) canvas.style.cursor = 'move';
          else canvas.style.cursor = 'default';
        } else {
          canvas.style.cursor = 'crosshair';
        }
        return;
      }
      e.preventDefault();

      if (tool === 'select' && selDrag) {
        selDrag.cur = p;
        var dx = p.x - selDrag.start.x, dy = p.y - selDrag.start.y;
        if (!selDrag.moved && selDrag.mode !== 'marquee' && Math.hypot(dx, dy) > 2) {
          pushUndo();
          selDrag.moved = true;
        }
        if (selDrag.mode === 'move' && selDrag.moved) {
          selectedItems().forEach(function (it) {
            var o = selDrag.origMap[it.id];
            if (!o) return;
            var cp = cloneItem(o);
            translateItem(cp, dx, dy);
            Object.keys(cp).forEach(function (k) { it[k] = cp[k]; });
          });
          hud.textContent = 'جابه‌جایی: Δx=' + fa(Math.round(dx)) + ' ، Δy=' + fa(Math.round(dy));
          hud.style.display = '';
        } else if ((selDrag.mode === 'end-a' || selDrag.mode === 'end-b') && selDrag.moved) {
          var ln = selectedItems()[0];
          if (ln) {
            var oLn = selDrag.origMap[ln.id];
            if (selDrag.mode === 'end-a') {
              ln.b = {x: oLn.b.x, y: oLn.b.y};
              ln.a = constrainPoint(ln.b, p, !!e.shiftKey);
            } else {
              ln.a = {x: oLn.a.x, y: oLn.a.y};
              ln.b = constrainPoint(ln.a, p, !!e.shiftKey);
            }
          }
        } else if (selDrag.mode === 'resize' && selDrag.moved && selDrag.origBox) {
          var nb = computeResizedBox(selDrag.origBox, selDrag.handle, dx, dy, !!e.shiftKey);
          selectedItems().forEach(function (it) {
            var orig = selDrag.origMap[it.id];
            if (orig) applyScaledFromOrig(it, orig, selDrag.origBox, nb);
          });
          hud.textContent = 'اندازه: ' + fa((nb.w / 40).toFixed(1)) + ' × ' + fa((nb.h / 40).toFixed(1)) + ' واحد';
          hud.style.display = '';
        } else if (selDrag.mode === 'marquee') {
          var mx = Math.min(selDrag.start.x, p.x), my = Math.min(selDrag.start.y, p.y);
          var mw = Math.abs(p.x - selDrag.start.x), mh = Math.abs(p.y - selDrag.start.y);
          var picked = selDrag.baseIds.slice();
          if (mw > 4 || mh > 4) {
            items.forEach(function (it) {
              var b = getItemBox(it);
              var inter = !(b.x + b.w < mx || b.x > mx + mw || b.y + b.h < my || b.y > my + mh);
              if (inter && picked.indexOf(it.id) < 0) picked.push(it.id);
            });
          }
          selectedIds = picked;
          syncSelectionUi();
        }
        renderComposite(true);
        return;
      }

      if (tool === 'pen' || tool === 'highlighter') {
        if (tool === 'pen' && rulerSnapEdge !== null) p = snapPointToRuler(p, false) || p;
        penPts.push(p);
        last = p;
        renderComposite(false, function (c) {
          drawStrokePts(c, penPts, color, size, lineStyle, tool === 'highlighter', '');
        });
      } else if (tool === 'eraser') {
        if (!eraserUndoPushed) { pushUndo(); eraserUndoPushed = true; }
        eraseStrokesSegment(last, p, Math.max(12, size * 5));
        last = p;
        renderComposite(false);
      } else {
        endPt = constrainPoint(startPt, p, !!e.shiftKey);
        updateHud(startPt, endPt);
        renderComposite(false, function (c) {
          var pa = startPt, pb = endPt;
          if (tool === 'circle') {
            var cr = Math.hypot(pb.x - pa.x, pb.y - pa.y);
            pa = {x: startPt.x - cr, y: startPt.y - cr};
            pb = {x: startPt.x + cr, y: startPt.y + cr};
          }
          drawShapeItem(c, tool, pa, pb, color, size, lineStyle, fillShape, '');
        });
      }
    }

    function up() {
      if (!drawing) return;
      drawing = false;
      hud.style.display = 'none';

      if (tool === 'select') {
        selDrag = null;
        renderComposite(true);
        return;
      }

      if (tool === 'pen' || tool === 'highlighter') {
        if (penPts.length) {
          pushUndo();
          if (autoShape && tool === 'pen' && rulerSnapEdge === null && penPts.length >= 8) {
            var sh = recognizeShape(penPts);
            if (sh) {
              items.push({
                id: uid(), kind: 'shape', shape: sh.kind,
                a: {x: sh.a.x, y: sh.a.y}, b: {x: sh.b.x, y: sh.b.y},
                color: color, size: size, lineStyle: lineStyle, fill: fillShape, label: ''
              });
              penPts = []; rulerSnapEdge = null;
              renderComposite(true);
              return;
            }
          }
          items.push({
            id: uid(), kind: 'stroke', sub: tool,
            pts: penPts.slice(), color: color, size: size, lineStyle: lineStyle, label: ''
          });
        }
        penPts = []; rulerSnapEdge = null;
        renderComposite(true);
      } else if (tool !== 'eraser' && tool !== 'text' && startPt && endPt) {
        if (Math.hypot(endPt.x - startPt.x, endPt.y - startPt.y) >= 3) {
          pushUndo();
          var sa = {x: startPt.x, y: startPt.y}, sb = {x: endPt.x, y: endPt.y};
          if (tool === 'circle') {
            var cr = Math.hypot(sb.x - sa.x, sb.y - sa.y);
            sa = {x: startPt.x - cr, y: startPt.y - cr};
            sb = {x: startPt.x + cr, y: startPt.y + cr};
          }
          items.push({
            id: uid(), kind: 'shape', shape: tool,
            a: sa, b: sb, color: color, size: size, lineStyle: lineStyle, fill: fillShape, label: ''
          });
        }
        startPt = endPt = null;
        renderComposite(true);
      }
    }

    canvas.addEventListener('dblclick', function (ev) {
      var p = pos(ev);
      var hit = topHitItem(p);
      if (hit) {
        ev.preventDefault();
        selectedIds = [hit.id];
        setActiveTool('select', true);
        syncSelectionUi();
        renderComposite(true);
        editSelectedItem();
      }
    });

    /* خط‌کش دیجیتال قابل جابه‌جایی و چرخش */
    function syncRulerDom() {
      rulerEl.style.display = rulerOn ? '' : 'none';
      if (!rulerOn) return;
      rulerEl.style.left = (rulerState.x / W * 100) + '%';
      rulerEl.style.top = (rulerState.y / H * 100) + '%';
      rulerEl.style.width = (rulerState.len / W * 100) + '%';
      rulerEl.style.height = (rulerState.halfH * 2 / H * 100) + '%';
      rulerEl.style.transform = 'translate(-50%, -50%) rotate(' + rulerState.deg + 'deg)';
      var normDeg = ((rulerState.deg % 360) + 360) % 360;
      if (normDeg > 180) normDeg -= 360;
      rulerDegLbl.textContent = fa(Math.round(normDeg)) + '°';
    }
    function rotateRuler(delta, exact) {
      rulerState.deg = exact !== undefined ? exact : (rulerState.deg + delta);
      syncRulerDom();
    }
    (function buildRuler() {
      var ticks = el('div', {class: 'wb-ruler-ticks'});
      for (var u = 0; u <= 16; u++) {
        var tk = el('span', {class: 'wb-rtick' + (u % 2 === 0 ? ' maj' : ''), style: 'left:' + (u / 16 * 100) + '%'},
          u % 2 === 0 ? [el('b', {text: fa(u)})] : []);
        ticks.appendChild(tk);
      }
      var ctrls = el('div', {class: 'wb-ruler-ctrls'}, [
        el('button', {type: 'button', text: '−۱۵°', title: 'چرخش ۱۵ درجه پادساعتگرد', onclick: function (ev) { ev.stopPropagation(); rotateRuler(-15); }}),
        rulerDegLbl,
        el('button', {type: 'button', text: '+۱۵°', title: 'چرخش ۱۵ درجه ساعتگرد', onclick: function (ev) { ev.stopPropagation(); rotateRuler(15); }}),
        el('button', {type: 'button', text: '۰°', title: 'افقی (۰ درجه)', onclick: function (ev) { ev.stopPropagation(); rotateRuler(0, 0); }}),
        el('button', {type: 'button', text: '۹۰°', title: 'عمودی (۹۰ درجه)', onclick: function (ev) { ev.stopPropagation(); rotateRuler(0, 90); }})
      ]);
      rulerEl.appendChild(ticks);
      rulerEl.appendChild(ctrls);
      var rDrag = null;
      rulerEl.addEventListener('pointerdown', function (ev) {
        if (ev.target && ev.target.tagName === 'BUTTON') return;
        ev.preventDefault(); ev.stopPropagation();
        var p = pos(ev);
        rDrag = {ox: p.x - rulerState.x, oy: p.y - rulerState.y};
        rulerEl.setPointerCapture(ev.pointerId);
      });
      rulerEl.addEventListener('pointermove', function (ev) {
        if (!rDrag) return;
        var p = pos(ev);
        rulerState.x = Math.max(80, Math.min(W - 80, p.x - rDrag.ox));
        rulerState.y = Math.max(60, Math.min(H - 60, p.y - rDrag.oy));
        syncRulerDom();
      });
      rulerEl.addEventListener('pointerup', function () { rDrag = null; });
      rulerEl.addEventListener('wheel', function (ev) {
        ev.preventDefault();
        rotateRuler((ev.deltaY > 0 ? 1 : -1) * (ev.shiftKey ? 1 : 5));
      }, {passive: false});
    })();

    function insertImageUrl(url) {
      var im = new Image();
      imgCache[url] = im;
      im.onload = function () {
        pushUndo();
        var maxW = 720, maxH = 520;
        var sc = Math.min(1, maxW / Math.max(1, im.naturalWidth), maxH / Math.max(1, im.naturalHeight));
        var dw = Math.max(40, Math.round(im.naturalWidth * sc)), dh = Math.max(40, Math.round(im.naturalHeight * sc));
        var imgIt = {
          id: uid(), kind: 'image',
          x: Math.round((W - dw) / 2), y: Math.round((H - dh) / 2),
          w: dw, h: dh, src: url, flipX: false, label: ''
        };
        items.push(imgIt);
        selectedIds = [imgIt.id];
        setActiveTool('select', true);
        syncSelectionUi();
        renderComposite(true);
      };
      im.src = url;
    }

    canvas.addEventListener('pointerdown', down);
    canvas.addEventListener('pointermove', move);
    window.addEventListener('pointerup', up);

    function savePage() {
      renderComposite(false);
      pages[cur].items = cloneItems(items);
      pages[cur].ink = inkCanvas.toDataURL('image/png');
      pages[cur].data = canvas.toDataURL('image/webp', 0.85);
      pages[cur].grid = grid;
      renderComposite(true);
    }
    function loadPage(i) {
      cur = i;
      grid = pages[i].grid || 'blank';
      gridSel.value = grid;
      undoStack = []; redoStack = [];
      selectedIds = [];
      updateHistoryBtns();
      status.textContent = 'صفحهٔ ' + fa(i + 1) + ' از ' + fa(pages.length);
      if (pages[i].items && pages[i].items.length) {
        items = cloneItems(pages[i].items);
        syncSelectionUi();
        renderComposite(true);
      } else if (pages[i].data) {
        var bgImg = ensureImg(pages[i].data);
        items = [{id: uid(), kind: 'image', x: 0, y: 0, w: W, h: H, src: pages[i].data, flipX: false, label: ''}];
        if (bgImg && bgImg.complete) renderComposite(true);
        syncSelectionUi();
      } else {
        items = [];
        syncSelectionUi();
        renderComposite(true);
      }
    }

    var toolBtns = {};
    function setActiveTool(id, keepSelection) {
      tool = id;
      Object.keys(toolBtns).forEach(function (k) { toolBtns[k].classList.toggle('on', k === id); });
      if (selModeQuickBtn) selModeQuickBtn.classList.toggle('on', id === 'select');
      if (!keepSelection && id !== 'select' && selectedIds.length) {
        selectedIds = [];
      }
      canvas.style.cursor = id === 'select' ? 'default' : 'crosshair';
      syncSelectionUi();
      renderComposite(true);
    }

    function tb(id, icon, title, dkOnly) {
      var b = el('button', {
        type: 'button',
        class: 'wb-tool' + (tool === id ? ' on' : '') + (dkOnly ? ' wb-dk-only' : ''),
        title: title,
        'aria-label': title,
        onclick: function () { setActiveTool(id, false); }
      }, [
        el('span', {class: 'wb-tool-ic', text: icon}),
        el('span', {class: 'wb-tool-txt', text: title})
      ]);
      toolBtns[id] = b;
      return b;
    }

    var gridSel = el('select', {class: 'wb-sel', title: 'زمینهٔ صفحه (تغییر آنی بدون پاک شدن رسم‌ها)', 'aria-label': 'زمینه'});
    [
      ['blank', 'زمینهٔ سفید (خالی)'],
      ['lined', 'خط‌دار (دفتر)'],
      ['dots', 'نقطه‌ای (Dot Grid)'],
      ['grid', 'شطرنجی'],
      ['mm', 'کاغذ میلی‌متری'],
      ['iso', 'نقطه‌ای سه‌بعدی (ایزومتریک)'],
      ['line', 'محور اعداد'],
      ['quad', 'محور مختصات ۴ ناحیه'],
      ['first', 'محور مختصات ناحیهٔ اول'],
      ['polar', 'مختصات قطبی']
    ].forEach(function (o) {
      gridSel.appendChild(el('option', {value: o[0], text: o[1]}));
    });
    gridSel.addEventListener('change', function () {
      grid = gridSel.value;
      renderComposite(true);
    });

    var swatchBtns = [];
    var colorIn = el('input', {type: 'color', value: color, class: 'wb-color', title: 'رنگ دلخواه'});
    function syncColorUiOnly(c) {
      color = c;
      if (/^#[0-9a-fA-F]{6}$/.test(c)) colorIn.value = c;
      swatchBtns.forEach(function (sb) { sb.classList.toggle('on', sb.getAttribute('data-col') === String(c).toLowerCase()); });
    }
    function applyColor(c) {
      syncColorUiOnly(c);
      var sel = selectedItems();
      if (sel.length) {
        pushUndo();
        sel.forEach(function (it) { it.color = c; });
        renderComposite(true);
      }
    }
    colorIn.addEventListener('change', function () { applyColor(colorIn.value); });
    colorIn.addEventListener('input', function () {
      syncColorUiOnly(colorIn.value);
      var sel = selectedItems();
      if (sel.length) { sel.forEach(function (it) { it.color = colorIn.value; }); renderComposite(true); }
    });
    var swatches = ['#1a237e', '#111827', '#dc2626', '#059669', '#0284c7', '#7c3aed', '#d97706'].map(function (hex) {
      var sw = el('button', {
        type: 'button',
        class: 'wb-swatch' + (hex === color ? ' on' : ''),
        'data-col': hex,
        title: 'رنگ قلم / مورد انتخاب‌شده',
        style: 'background:' + hex,
        onclick: function () { applyColor(hex); }
      });
      swatchBtns.push(sw);
      return sw;
    });

    var sizeVal = el('span', {class: 'wb-size-val', text: fa(size)});
    var sizeIn = el('input', {type: 'range', min: 1, max: 16, value: size, class: 'wb-size', title: 'ضخامت قلم / اندازهٔ مورد انتخاب‌شده'});
    var sizeUndoArmed = true;
    sizeIn.addEventListener('pointerdown', function () { sizeUndoArmed = true; });
    sizeIn.addEventListener('input', function () {
      size = Number(sizeIn.value);
      sizeVal.textContent = fa(size);
      var sel = selectedItems();
      if (sel.length) {
        if (sizeUndoArmed) { pushUndo(); sizeUndoArmed = false; }
        sel.forEach(function (it) {
          it.size = size;
          if (it.kind === 'text') it.fontPx = size * 6 + 14;
        });
        renderComposite(true);
      }
    });

    var autoShapeBtn = (function () {
      var b = el('button', {
        type: 'button',
        class: 'wb-tool',
        title: 'تشخیص شکل: خط/دایره/مثلث/مستطیل دست‌آزاد به شکل منظم تبدیل می‌شود',
        'aria-label': 'تشخیص شکل',
        onclick: function () {
          autoShape = !autoShape;
          b.classList.toggle('on', autoShape);
          if (autoShape && toolBtns.pen) setActiveTool('pen', false);
        }
      }, [
        el('span', {class: 'wb-tool-ic', text: '✨'}),
        el('span', {class: 'wb-tool-txt', text: 'تشخیص شکل'})
      ]);
      return b;
    })(); /* V264 */

    var fillBtn = (function () {
      var b = el('button', {
        type: 'button',
        class: 'wb-tool wb-dk-only',
        title: 'پر کردن ملایم داخل اشکال بسته (برای رسم جدید یا شکل انتخاب‌شده)',
        'aria-label': 'توپر ملایم',
        onclick: function () {
          fillShape = !fillShape;
          b.classList.toggle('on', fillShape);
          var sel = selectedItems().filter(function (it) { return it.kind === 'shape'; });
          if (sel.length) {
            pushUndo();
            sel.forEach(function (it) { it.fill = fillShape; });
            renderComposite(true);
          }
        }
      }, [
        el('span', {class: 'wb-tool-ic', text: '▣'}),
        el('span', {class: 'wb-tool-txt', text: 'توپر ملایم'})
      ]);
      return b;
    })();

    var styleBtns = {};
    var strokeStyleRow = el('div', {class: 'wb-seg wb-dk-only', role: 'group', 'aria-label': 'نوع خط'}, [
      ['solid', '─── ممتد'],
      ['dashed', '- - خط‌چین'],
      ['dotted', '··· نقطه‌چین']
    ].map(function (st) {
      var sb = el('button', {
        type: 'button',
        class: 'wb-seg-btn' + (lineStyle === st[0] ? ' on' : ''),
        text: st[1],
        onclick: function () {
          lineStyle = st[0];
          Object.keys(styleBtns).forEach(function (k) { styleBtns[k].classList.toggle('on', k === st[0]); });
          var sel = selectedItems().filter(function (it) { return it.kind === 'shape' || it.kind === 'stroke'; });
          if (sel.length) {
            pushUndo();
            sel.forEach(function (it) { it.lineStyle = lineStyle; });
            renderComposite(true);
          }
        }
      });
      styleBtns[st[0]] = sb;
      return sb;
    }));

    var imgBtn = el('button', {
      type: 'button',
      class: 'wb-tool wb-act wb-dk-only',
      title: 'درج تصویر روی تخته (یا چسباندن با Ctrl+V)',
      'aria-label': 'درج تصویر',
      onclick: async function () {
        var f = await pickFile();
        if (!f) return;
        var url = await fileToDataUrl(f);
        insertImageUrl(url);
      }
    }, [el('span', {class: 'wb-tool-ic', text: '🖼'}), el('span', {class: 'wb-tool-txt', text: 'درج تصویر'})]);

    var qIdx = (run && run.exam && Array.isArray(run.exam.questions)) ? run.exam.questions.indexOf(q) : -1;
    var boardTitle = opts && opts.title ? '🖍 ' + opts.title : ('🖍 تختهٔ سفید' + (qIdx >= 0 ? ' — سؤال ' + fa(qIdx + 1) : ''));

    function onKeyDown(ev) {
      if (!document.body.contains(bg)) return;
      if (document.querySelector('.modal-bg')) return;
      var tag = ev.target && ev.target.tagName;
      if (tag === 'INPUT' || tag === 'SELECT' || tag === 'TEXTAREA') return;
      var mod = ev.ctrlKey || ev.metaKey;
      if (mod && (ev.key === 'z' || ev.key === 'Z')) {
        ev.preventDefault();
        if (ev.shiftKey) doRedo(); else doUndo();
      } else if (mod && (ev.key === 'y' || ev.key === 'Y')) {
        ev.preventDefault();
        doRedo();
      } else if (mod && (ev.key === 'a' || ev.key === 'A')) {
        if (items.length) {
          ev.preventDefault();
          selectedIds = items.map(function (it) { return it.id; });
          setActiveTool('select', true);
          syncSelectionUi();
          renderComposite(true);
        }
      } else if (mod && (ev.key === 'd' || ev.key === 'D')) {
        if (selectedIds.length) { ev.preventDefault(); duplicateSelected(); }
      } else if ((ev.key === 'Delete' || ev.key === 'Backspace') && selectedIds.length) {
        ev.preventDefault();
        deleteSelected();
      } else if (ev.key === 'Escape' && selectedIds.length) {
        ev.preventDefault();
        selectedIds = [];
        syncSelectionUi();
        renderComposite(true);
      } else if (selectedIds.length && (ev.key === 'ArrowUp' || ev.key === 'ArrowDown' || ev.key === 'ArrowLeft' || ev.key === 'ArrowRight')) {
        ev.preventDefault();
        var step = ev.shiftKey ? 12 : 3;
        var ndx = ev.key === 'ArrowLeft' ? -step : (ev.key === 'ArrowRight' ? step : 0);
        var ndy = ev.key === 'ArrowUp' ? -step : (ev.key === 'ArrowDown' ? step : 0);
        pushUndo();
        selectedItems().forEach(function (it) { translateItem(it, ndx, ndy); });
        renderComposite(true);
      }
    }
    function onPaste(ev) {
      if (!document.body.contains(bg) || document.querySelector('.modal-bg')) return;
      var cItems = ev.clipboardData && ev.clipboardData.items;
      if (!cItems) return;
      for (var idx = 0; idx < cItems.length; idx++) {
        if (cItems[idx].type && cItems[idx].type.indexOf('image/') === 0) {
          var fl = cItems[idx].getAsFile();
          if (fl) { ev.preventDefault(); fileToDataUrl(fl).then(insertImageUrl); break; }
        }
      }
    }
    window.addEventListener('keydown', onKeyDown);
    window.addEventListener('paste', onPaste);

    function close() {
      window.removeEventListener('pointerup', up);
      window.removeEventListener('keydown', onKeyDown);
      window.removeEventListener('paste', onPaste);
      bg.remove();
      document.body.style.overflow = '';
    }
    async function requestClose() {
      if (await S.confirmDlg('بستن تخته', 'تخته بدون ثبت بسته شود؟', 'بستن', true)) close();
    }
    bg.__onBack = requestClose;

    /* ستون راست: انتخاب، قلم‌ها، اشکال هندسی، سبک خط و رنگ */
    var rightSide = el('aside', {class: 'wb-side wb-right', 'aria-label': 'ابزارهای ترسیم تخته'}, [
      el('div', {class: 'wb-title'}, [el('span', {text: boardTitle})]),
      el('div', {class: 'wb-sec'}, [
        el('div', {class: 'wb-sec-lbl', text: 'انتخاب، قلم و درج'}),
        el('div', {class: 'wb-tools-grid'}, [
          tb('select', '↖', 'انتخاب'),
          tb('pen', '✏️', 'قلم'),
          tb('highlighter', '🖍', 'ماژیک هایلایت'),
          tb('eraser', '🧽', 'پاک‌کن'),
          tb('text', 'T', 'متن'),
          imgBtn
        ])
      ]),
      el('div', {class: 'wb-sec'}, [
        el('div', {class: 'wb-sec-lbl', text: 'اشکال هندسی و محور (Shift=دقیق)'}),
        el('div', {class: 'wb-tools-grid'}, [
          tb('line', '╱', 'خط'),
          tb('arrow', '➚', 'پیکان'),
          tb('dblarrow', '↔', 'پیکان دوسر', true),
          tb('rect', '▭', 'مستطیل'),
          tb('circle', '◯', 'دایره'),
          tb('ellipse', '⬭', 'بیضی', true),
          tb('triangle', '△', 'مثلث', true),
          tb('rtriangle', '◿', 'قائم‌الزاویه', true),
          tb('diamond', '◇', 'لوزی', true),
          tb('axes', '✚', 'محور سریع', true)
        ])
      ]),
      el('div', {class: 'wb-sec'}, [
        el('div', {class: 'wb-sec-lbl', text: 'کمک‌رسم و سبک خط'}),
        el('div', {class: 'wb-tools-grid'}, [autoShapeBtn, fillBtn]),
        strokeStyleRow
      ]),
      el('div', {class: 'wb-sec wb-stroke-sec'}, [
        el('div', {class: 'wb-sec-lbl', text: 'رنگ و ضخامت (رسم / مورد انتخابی)'}),
        el('div', {class: 'wb-swatches'}, swatches.concat([colorIn])),
        el('div', {class: 'wb-size-row'}, [
          el('span', {class: 'wb-size-lbl', text: 'ضخامت'}),
          sizeIn,
          sizeVal
        ])
      ])
    ]);

    /* بخش اختصاصی انتخاب و ویرایش موارد درج‌شده (در ستون چپ دسکتاپ) */
    var selModeQuickBtn = el('button', {
      type: 'button',
      class: 'wb-tool wb-act',
      title: 'فعال کردن ابزار انتخاب، جابه‌جایی و تغییر اندازه روی بوم',
      'aria-label': 'ابزار انتخاب',
      onclick: function () { setActiveTool('select', true); }
    }, [el('span', {class: 'wb-tool-ic', text: '↖'}), el('span', {class: 'wb-tool-txt', text: 'ابزار انتخاب'})]);

    var selAllBtn = el('button', {
      type: 'button',
      class: 'wb-tool wb-act',
      title: 'انتخاب همهٔ موارد صفحه (Ctrl+A)',
      'aria-label': 'انتخاب همه',
      onclick: function () {
        if (!items.length) { toast('هنوز موردی روی تخته درج نشده است.', 'err'); return; }
        selectedIds = items.map(function (it) { return it.id; });
        setActiveTool('select', true);
        syncSelectionUi();
        renderComposite(true);
      }
    }, [el('span', {class: 'wb-tool-ic', text: '⊞'}), el('span', {class: 'wb-tool-txt', text: 'انتخاب همه'})]);

    function selActBtn(icon, label, titleTxt, fn, extraCls) {
      var b = el('button', {
        type: 'button',
        class: 'wb-tool wb-act' + (extraCls ? ' ' + extraCls : ''),
        title: titleTxt,
        'aria-label': label,
        disabled: true,
        onclick: fn
      }, [el('span', {class: 'wb-tool-ic', text: icon}), el('span', {class: 'wb-tool-txt', text: label})]);
      selActionBtns.push(b);
      return b;
    }

    var editItemBtn = selActBtn('✏️', 'ویرایش', 'ویرایش متن یا افزودن/ویرایش برچسب روی شکل انتخاب‌شده (یا دوبار کلیک روی بوم)', editSelectedItem);
    var dupItemBtn = selActBtn('⧉', 'تکثیر مورد', 'کپی کردن مورد انتخاب‌شده (Ctrl+D)', duplicateSelected);
    var growItemBtn = selActBtn('➕', 'بزرگ‌تر', 'بزرگ‌تر کردن مورد انتخاب‌شده (+۱۵٪)', function () { scaleSelectedBy(1.15); });
    var shrinkItemBtn = selActBtn('➖', 'کوچک‌تر', 'کوچک‌تر کردن مورد انتخاب‌شده (−۱۵٪)', function () { scaleSelectedBy(0.86); });
    var flipItemBtn = selActBtn('⇄', 'قرینه', 'قرینهٔ افقی مورد انتخاب‌شده', flipSelectedHoriz);
    var delItemBtn = selActBtn('🗑', 'حذف مورد', 'حذف مورد انتخاب‌شده (Delete)', deleteSelected, 'is-danger');

    var undoBtn = el('button', {
      type: 'button',
      class: 'wb-tool wb-act',
      title: 'برگرداندن آخرین تغییر (Ctrl+Z)',
      'aria-label': 'برگرداندن',
      onclick: doUndo
    }, [el('span', {class: 'wb-tool-ic', text: '↶'}), el('span', {class: 'wb-tool-txt', text: 'برگرداندن'})]);

    var redoBtn = el('button', {
      type: 'button',
      class: 'wb-tool wb-act',
      title: 'انجام مجدد (Ctrl+Y)',
      'aria-label': 'انجام مجدد',
      onclick: doRedo
    }, [el('span', {class: 'wb-tool-ic', text: '↷'}), el('span', {class: 'wb-tool-txt', text: 'انجام مجدد'})]);

    var rulerBtn = el('button', {
      type: 'button',
      class: 'wb-tool wb-act wb-dk-only',
      title: 'نمایش/مخفی کردن خط‌کش و زاویه‌سنج کمکی روی تخته (چرخ ماوس = چرخش)',
      'aria-label': 'خط‌کش کمکی',
      onclick: function () {
        rulerOn = !rulerOn;
        rulerBtn.classList.toggle('on', rulerOn);
        syncRulerDom();
      }
    }, [el('span', {class: 'wb-tool-ic', text: '📏'}), el('span', {class: 'wb-tool-txt', text: 'خط‌کش کمکی'})]);

    var clearBtn = el('button', {
      type: 'button',
      class: 'wb-tool wb-act is-danger',
      title: 'پاک کردن همهٔ رسم‌های این صفحه (زمینه حفظ می‌شود)',
      onclick: async function () {
        if (await S.confirmDlg('پاک کردن صفحه', 'کل رسم‌های این صفحه پاک شود؟', 'پاک شود', true)) { pushUndo(); blank(); }
      }
    }, [el('span', {class: 'wb-tool-ic', text: '🗑'}), el('span', {class: 'wb-tool-txt', text: 'پاک کل صفحه'})]);

    var stage = el('div', {class: 'wb-stage'}, [canvas, rulerEl, hud]);
    var zoomLbl = el('button', {
      type: 'button',
      class: 'btn light sm wb-zoom-Mid',
      text: '۱۰۰٪',
      title: 'بازنشانی اندازهٔ بوم به ۱۰۰٪',
      onclick: function () { setZoom(100); }
    });
    function setZoom(z) {
      zoomPct = Math.max(75, Math.min(200, z));
      zoomLbl.textContent = fa(zoomPct) + '٪';
      if (zoomPct === 100) {
        stage.style.width = '';
        stage.style.maxWidth = '100%';
        stage.style.maxHeight = '100%';
      } else {
        stage.style.width = Math.round(920 * zoomPct / 100) + 'px';
        stage.style.maxWidth = 'none';
        stage.style.maxHeight = 'none';
      }
    }
    var zoomRow = el('div', {class: 'wb-zoom-row wb-dk-only'}, [
      el('button', {type: 'button', class: 'btn light sm', text: '−', title: 'کوچک‌نمایی', onclick: function () { setZoom(zoomPct - 25); }}),
      zoomLbl,
      el('button', {type: 'button', class: 'btn light sm', text: '+', title: 'بزرگ‌نمایی', onclick: function () { setZoom(zoomPct + 25); }})
    ]);

    var prevBtn = el('button', {
      type: 'button',
      class: 'btn light sm wb-nav-btn',
      text: '◀ قبل',
      title: 'صفحهٔ قبل',
      onclick: function () { if (cur > 0) { savePage(); loadPage(cur - 1); } }
    });
    var nextBtn = el('button', {
      type: 'button',
      class: 'btn light sm wb-nav-btn',
      text: 'بعد ▶',
      title: 'صفحهٔ بعد / صفحهٔ جدید',
      onclick: function () {
        savePage();
        if (cur < pages.length - 1) loadPage(cur + 1);
        else if (pages.length < WHITEBOARD_MAX_PAGES) { pages.push(newPage()); loadPage(pages.length - 1); }
        else toast('حداکثر ' + fa(WHITEBOARD_MAX_PAGES) + ' صفحه.', 'err');
      }
    });
    var dupBtn = el('button', {
      type: 'button',
      class: 'btn light sm wb-nav-btn wb-dk-only',
      text: '⧉ کپی صفحه',
      title: 'کپی کردن صفحهٔ فعلی در یک صفحهٔ جدید',
      onclick: function () {
        if (pages.length >= WHITEBOARD_MAX_PAGES) { toast('حداکثر ' + fa(WHITEBOARD_MAX_PAGES) + ' صفحه.', 'err'); return; }
        savePage();
        var copy = {items: cloneItems(pages[cur].items), ink: pages[cur].ink, data: pages[cur].data, grid: pages[cur].grid};
        pages.splice(cur + 1, 0, copy);
        loadPage(cur + 1);
        toast('صفحه کپی شد.', 'ok');
      }
    });
    var dlBtn = el('button', {
      type: 'button',
      class: 'btn light sm wb-nav-btn wb-dk-only',
      text: '⬇ ذخیرهٔ عکس',
      title: 'دانلود تصویر صفحهٔ فعلی تخته (PNG)',
      onclick: function () {
        renderComposite(false);
        var a = document.createElement('a');
        a.href = canvas.toDataURL('image/png');
        a.download = 'whiteboard-page-' + (cur + 1) + '.png';
        a.click();
        renderComposite(true);
      }
    });
    var doneBtn = el('button', {
      type: 'button',
      class: 'btn sm wb-done-btn',
      text: '✅ ' + (opts && opts.doneLabel ? opts.doneLabel : 'ثبت به‌عنوان پاسخ'),
      onclick: function () {
        savePage();
        var out = pages.filter(function (p) { return p.data; }).map(function (p) { return p.data; });
        close();
        if (out.length) onDone(out);
      }
    });
    var closeBtn = el('button', {
      type: 'button',
      class: 'btn light sm wb-close-btn',
      text: '✕ بستن',
      title: 'بستن بدون ثبت',
      onclick: requestClose
    });

    var leftSide = el('aside', {class: 'wb-side wb-left', 'aria-label': 'مدیریت صفحه و ثبت تخته'}, [
      el('div', {class: 'wb-top-close'}, [closeBtn]),
      el('div', {class: 'wb-sec wb-inspector wb-dk-only'}, [
        el('div', {class: 'wb-sec-lbl', text: 'انتخاب و ویرایش موارد درج‌شده'}),
        selBadge,
        el('div', {class: 'wb-tools-grid'}, [
          selModeQuickBtn, selAllBtn,
          editItemBtn, dupItemBtn,
          growItemBtn, shrinkItemBtn,
          flipItemBtn, delItemBtn
        ])
      ]),
      el('div', {class: 'wb-sec'}, [
        el('div', {class: 'wb-sec-lbl', text: 'زمینهٔ صفحه (بدون پاک شدن رسم)'}),
        gridSel
      ]),
      el('div', {class: 'wb-sec'}, [
        el('div', {class: 'wb-sec-lbl', text: 'تاریخچه و ابزار کمکی'}),
        el('div', {class: 'wb-tools-grid wb-edit-grid'}, [undoBtn, redoBtn, rulerBtn, clearBtn])
      ]),
      el('div', {class: 'wb-sec wb-dk-only'}, [
        el('div', {class: 'wb-sec-lbl', text: 'بزرگ‌نمایی بوم'}),
        zoomRow
      ]),
      el('div', {class: 'wb-sec wb-pages-sec'}, [
        el('div', {class: 'wb-sec-lbl', text: 'صفحه‌ها و خروجی'}),
        status,
        el('div', {class: 'wb-nav-row'}, [prevBtn, nextBtn]),
        el('div', {class: 'wb-nav-row wb-dk-only'}, [dupBtn, dlBtn])
      ]),
      el('div', {class: 'wb-spacer'}),
      doneBtn
    ]);

    var area = el('div', {class: 'wb-area'}, [stage]);
    bg.appendChild(rightSide); bg.appendChild(area); bg.appendChild(leftSide);
    document.body.appendChild(bg); document.body.style.overflow = 'hidden';
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
