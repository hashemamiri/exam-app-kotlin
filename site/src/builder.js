/* V139 — فاز ۲ سایت: سازندهٔ آزمون (آنلاین + چاپی)
   قرارداد ذخیره عیناً SupabaseExamBuilderRepository.save → native_save_exam_v2(p_payload) و
   کدگذاری سؤال‌ها عیناً ExamQuestionCodec.encode (کلیدهای عمومی + answer_key جداگانه).
   آزمون چاپی (بدون سرور، مثل PrintExamStore اندروید) در localStorage همین مرورگر ذخیره می‌شود. */
(function () {
  'use strict';
  var S = window.ExamSite;
  var el = S.el, esc = S.esc, fa = S.fa, en = S.en, toast = S.toast, errMsg = S.errMsg, uuid = S.uuid;
  var LS_PRINT = 'examsite.printexams.v1';
  var LS_DRAFT = 'examsite.builderdraft.v1';
  var TYPES = [['multiple', 'چندگزینه‌ای', '◉'], ['truefalse', 'صحیح / غلط', '✓'], ['fill', 'جای‌خالی', '▁'], ['numeric', 'عددی', '#'], ['matching', 'جورکردنی', '⇄'], ['essay', 'تشریحی', '✎']];
  var TYPE_LABEL = {}; TYPES.forEach(function (t) { TYPE_LABEL[t[0]] = t[1]; });

  /* ================================================================ مدل سؤال (آینهٔ QuestionDraft) */
  function newQuestion(type) {
    var q = {id: uuid(), type: type || 'multiple', text: '', score: 1, options: ['', '', '', ''], optionImages: [null, null, null, null], correctIndex: null, expectedText: '', expectedNumber: '', tolerance: '0', caseSensitive: false,
      matchingLeft: ['', ''], matchingRight: ['', ''], matchingPairs: {}, images: [], answerImageMode: 'no', allowAnswerGraph: false, maxAnswerImages: 1,
      textAlign: 'right', imagePosition: 'below', fontFamily: 'default', fontSizeSp: 16, bold: false, italic: false, answerLines: type === 'essay' ? 5 : 2, answerLineStyle: 'lined', answerLineSpacingCm: 1.0, spans: [], alignSpans: [], raw: {}, rawKey: {}};
    if (type === 'truefalse') { q.options = []; q.optionImages = []; q.expectedText = 'true'; }
    if (type === 'fill' || type === 'numeric' || type === 'essay') { q.options = []; q.optionImages = []; }
    return q;
  }
  /* decode: همان ExamQuestionCodec.decode */
  function decodeQuestion(obj, key) {
    obj = obj || {}; key = key || {};
    var type = S.qType(obj.type); if (type === 'long') type = 'essay';
    var q = newQuestion(type);
    q.id = obj.id || q.id; q.text = obj.text || ''; q.score = Number(obj.score) || 1;
    q.options = (obj.options || []).map(function (x) { return x == null ? '' : String(x); });
    q.optionImages = q.options.map(function (_, i) { var v = (obj.optionImages || [])[i]; return v ? String(v) : null; });
    q.optionStyles = obj.optionStyles || null; q.leftStyles = obj.leftStyles || null; q.rightStyles = obj.rightStyles || null;
    q.spans = obj.spans || []; q.alignSpans = obj.alignSpans || [];
    q.correctIndex = key.correctOption != null ? Number(key.correctOption) : (obj.correctIndex != null ? Number(obj.correctIndex) : null);
    if (type === 'truefalse') q.expectedText = String(key.correctAnswer === true || key.correctAnswer === 'true');
    else if (type === 'fill') q.expectedText = (key.accept || []).join('|');
    else q.expectedText = key.correctAnswer != null ? String(key.correctAnswer) : (obj.expectedText || '');
    q.expectedNumber = key.answer != null ? String(key.answer) : (obj.expectedNumber || '');
    q.tolerance = key.tolerance != null ? String(key.tolerance) : (obj.tolerance != null ? String(obj.tolerance) : '0');
    q.caseSensitive = key.caseSensitive === true || obj.caseSensitive === true;
    q.matchingLeft = (obj.leftItems || []).map(String); q.matchingRight = (obj.rightItems || []).map(String);
    q.matchingLeftImages = obj.leftImages || []; q.matchingRightImages = obj.rightImages || [];
    q.matchingPairs = {}; if (key.matchAnswer && typeof key.matchAnswer === 'object') Object.keys(key.matchAnswer).forEach(function (k) { q.matchingPairs[k] = Number(key.matchAnswer[k]); });
    q.answerImageMode = obj.allowImages || 'no'; q.allowAnswerGraph = obj.allowAnswerGraph === true; q.maxAnswerImages = Number(obj.maxImages) || 0;
    var imgs = (obj.images || []).filter(Boolean).map(String); if (obj.image && imgs.indexOf(obj.image) < 0) imgs.unshift(obj.image);
    var pos = obj.imgFreePositions || [];
    q.images = imgs.map(function (u, i) { var p = pos[i] || {}; return {uri: u, xMm: p.x != null ? p.x : 20, yMm: p.y != null ? p.y : 30, widthMm: p.w != null ? p.w : 55}; });
    q.textAlign = obj.align || 'right'; q.imagePosition = obj.imgPos || 'below'; q.fontFamily = obj.font || 'default'; q.fontSizeSp = Number(obj.fontSize) || 16; q.bold = obj.bold === true; q.italic = obj.italic === true;
    q.answerLines = obj.answerLines != null ? Number(obj.answerLines) : (type === 'essay' ? 5 : 2);
    q.answerLineStyle = ['lined', 'blank', 'grid'].indexOf(obj.answerLineStyle) >= 0 ? obj.answerLineStyle : 'lined';
    q.answerLineSpacingCm = obj.answerLineSpacingCm != null ? Number(obj.answerLineSpacingCm) : 1.0;
    q.audio = obj.audio || null; q.audioBytes = obj.audioBytes; q.audioMs = obj.audioMs;
    q.raw = obj; q.rawKey = key;
    return q;
  }
  var ANSWER_FIELDS = ['correctOption', 'correctAnswer', 'accept', 'answer', 'tolerance', 'caseSensitive', 'matchAnswer', 'pairs'];
  /* encode: همان ExamQuestionCodec.encode */
  function encodeQuestions(list) {
    var pub = [], keys = [];
    list.forEach(function (q, index) {
      var v = {}; Object.keys(q.raw || {}).forEach(function (k) { v[k] = q.raw[k]; });
      ANSWER_FIELDS.forEach(function (k) { delete v[k]; });
      v.id = q.id; v.type = q.type; v.text = q.text.trim(); v.score = Number(q.score) || 0;
      v.images = q.images.map(function (m) { return m.uri; }); v.image = q.images.length ? q.images[0].uri : null;
      v.imgFreePositions = q.images.map(function (m) { return {x: m.xMm, y: m.yMm, w: m.widthMm}; });
      if (q.audio) { v.audio = q.audio; v.audioBytes = q.audioBytes || 0; v.audioMs = q.audioMs || 0; } else { delete v.audio; delete v.audioBytes; delete v.audioMs; }
      v.allowImages = q.answerImageMode; v.allowAnswerGraph = !!q.allowAnswerGraph; v.maxImages = q.answerImageMode === 'no' ? 0 : Math.max(1, Math.min(10, Number(q.maxAnswerImages) || 1));
      v.align = q.textAlign; v.imgPos = q.imagePosition; v.font = q.fontFamily; v.fontSize = Math.max(8, Math.min(40, Number(q.fontSizeSp) || 16)); v.bold = !!q.bold; v.italic = !!q.italic;
      v.answerLines = Math.max(0, Math.min(12, Number(q.answerLines) || 0)); v.answerLineStyle = q.answerLineStyle; v.answerLineSpacingCm = Math.max(0.5, Math.min(2, Number(q.answerLineSpacingCm) || 1));
      if (q.spans && q.spans.length) v.spans = q.spans; else delete v.spans;
      if (q.alignSpans && q.alignSpans.length) v.alignSpans = q.alignSpans; else delete v.alignSpans;
      if (q.type === 'multiple') { v.options = q.options.slice(); v.optionImages = q.options.map(function (_, i) { return q.optionImages[i] || ''; }); if (q.optionStyles) v.optionStyles = q.optionStyles; }
      if (q.type === 'matching') {
        v.leftItems = q.matchingLeft.slice(); v.rightItems = q.matchingRight.slice();
        if (q.leftStyles) v.leftStyles = q.leftStyles; if (q.rightStyles) v.rightStyles = q.rightStyles;
        v.leftImages = q.matchingLeft.map(function (_, i) { return (q.matchingLeftImages || [])[i] || ''; }); v.rightImages = q.matchingRight.map(function (_, i) { return (q.matchingRightImages || [])[i] || ''; });
      }
      pub.push(v);
      var k = {}; Object.keys(q.rawKey || {}).forEach(function (x) { k[x] = q.rawKey[x]; }); k.i = index;
      if (q.type === 'multiple') k.correctOption = q.correctIndex != null ? q.correctIndex : 0;
      else if (q.type === 'truefalse') k.correctAnswer = q.expectedText === 'true';
      else if (q.type === 'fill') { k.accept = q.expectedText.split('|').map(function (x) { return x.trim(); }).filter(Boolean); k.caseSensitive = !!q.caseSensitive; }
      else if (q.type === 'numeric') { var n = parseFloat(en(q.expectedNumber)); k.answer = isFinite(n) ? n : null; var t = parseFloat(en(q.tolerance)); k.tolerance = isFinite(t) ? t : 0; }
      else if (q.type === 'matching') { k.matchAnswer = {}; Object.keys(q.matchingPairs).forEach(function (l) { k.matchAnswer[String(l)] = q.matchingPairs[l]; }); }
      keys.push(k);
    });
    return {publicQuestions: pub, answerKey: keys};
  }
  function toServerExam(state) {
    var enc = encodeQuestions(state.questions);
    return {id: state.examId || uuid(), title: state.title, subject: state.subject, duration: Number(state.duration) || 0, total_score: state.questions.reduce(function (s, q) { return s + (Number(q.score) || 0); }, 0), questions: enc.publicQuestions, __answers: enc.answerKey};
  }

  /* ================================================================ آپلود تصویر (Storage exam-images، مسیر مثل SupabaseQuestionImageUploader) */
  function downscale(file, maxDim) {
    return new Promise(function (resolve, reject) {
      var img = new Image(); var url = URL.createObjectURL(file);
      img.onload = function () {
        URL.revokeObjectURL(url);
        var w = img.naturalWidth, h = img.naturalHeight, k = Math.min(1, maxDim / Math.max(w, h));
        var cv = document.createElement('canvas'); cv.width = Math.max(1, Math.round(w * k)); cv.height = Math.max(1, Math.round(h * k));
        var ctx = cv.getContext('2d'); ctx.fillStyle = '#fff'; ctx.fillRect(0, 0, cv.width, cv.height); ctx.drawImage(img, 0, 0, cv.width, cv.height);
        cv.toBlob(function (b) { b ? resolve(b) : reject(new Error('فشرده‌سازی تصویر ناموفق بود.')); }, 'image/webp', 0.9);
      };
      img.onerror = function () { URL.revokeObjectURL(url); reject(new Error('تصویر انتخاب‌شده قابل خواندن نیست.')); };
      img.src = url;
    });
  }
  async function uploadImage(file, folder, examId) {
    var blob = await downscale(file, 2200);
    if (blob.size > 8 * 1024 * 1024) throw new Error('حجم تصویر پس از فشرده‌سازی بیش از ۸ مگابایت است.');
    return S.uploadMedia(blob, 'image', folder, examId, 'webp', 'image/webp');
  }
  function pickFile(accept) {
    return new Promise(function (resolve) { var i = el('input', {type: 'file', accept: accept || 'image/*', style: 'display:none'}); i.addEventListener('change', function () { resolve(i.files && i.files[0] ? i.files[0] : null); i.remove(); }); document.body.appendChild(i); i.click(); });
  }
  function fileToDataUrl(file) { return new Promise(function (res, rej) { var r = new FileReader(); r.onload = function () { res(r.result); }; r.onerror = rej; r.readAsDataURL(file); }); }

  /* ================================================================ V163 — آزمون‌های چاپی روی سرور (print_exams، جدا از exams)
     قبلاً فقط در localStorage همین مرورگر بودند؛ حالا در جدول print_exams تا اپ/دسکتاپ/گوشی یکی باشند.
     رکورد سرور: {id,title,subject,duration,questions:[public+key ادغام‌شده],source_exam_id,saved_at}. هزینه فقط هر تصویر جدید ۱۰۰۰ تومان. */
  var LS_PRINT_MIGRATED = 'examsite.printexams.migrated.v163';
  function combinedQuestions(list) { var enc = encodeQuestions(list); return enc.publicQuestions.map(function (q, i) { var c = Object.assign({}, q, enc.answerKey[i] || {}); delete c.i; return c; }); }
  function draftsFromCombined(arr) { return (Array.isArray(arr) ? arr : []).map(function (q) { return decodeQuestion(q, q); }); }
  async function printExamsList() { var r = await S.rpcObj('native_print_exams_list_v163', {}); if (r && r.error) throw new Error(String(r.error)); return Array.isArray(r) ? r : []; }
  async function printExamGet(id) { var r = await S.rpcObj('native_print_exam_get_v163', {p_id: id}); if (!r || r.error) throw new Error(r && r.error ? String(r.error) : 'آزمون چاپی پیدا نشد.'); return r; }
  /* تصاویر data: باید قبل از ذخیرهٔ سرور آپلود شوند (مثل مسیر آنلاین) */
  async function uploadPrintImages(questions, printId) {
    var dataBlob = async function (u) { return await (await fetch(u)).blob(); };
    for (var i = 0; i < questions.length; i++) {
      var q = questions[i];
      for (var k = 0; k < (q.images || []).length; k++) if (/^data:/.test(q.images[k].uri)) q.images[k].uri = await uploadImage(await dataBlob(q.images[k].uri), 'questions', printId);
      for (var o = 0; o < (q.optionImages || []).length; o++) if (q.optionImages[o] && /^data:/.test(q.optionImages[o])) q.optionImages[o] = await uploadImage(await dataBlob(q.optionImages[o]), 'option_images', printId);
      var ml = q.matchingLeftImages || [], mr = q.matchingRightImages || [];
      for (var a = 0; a < ml.length; a++) if (ml[a] && /^data:/.test(ml[a])) ml[a] = await uploadImage(await dataBlob(ml[a]), 'matching_images', printId);
      for (var b = 0; b < mr.length; b++) if (mr[b] && /^data:/.test(mr[b])) mr[b] = await uploadImage(await dataBlob(mr[b]), 'matching_images', printId);
    }
  }
  function countNewImages(questions) { var n = 0; questions.forEach(function (q) { (q.images || []).forEach(function (m) { if (/^data:/.test(m.uri)) n++; }); ['optionImages', 'matchingLeftImages', 'matchingRightImages'].forEach(function (k) { (q[k] || []).forEach(function (u) { if (u && /^data:/.test(u)) n++; }); }); }); return n; }
  async function printExamSave(rec, opts) {
    opts = opts || {};
    var newImgs = countNewImages(rec.questions);
    if (newImgs > 0 && !opts.silent) { if (!(await S.confirmDlg('هزینهٔ تصاویر', fa(newImgs) + ' تصویر جدید در این آزمون چاپی هست؛ برای ذخیره روی سرور، به‌ازای هر تصویر ' + fa('1,000') + ' تومان (جمعاً ' + S.money(newImgs * 1000) + ') از کیف پول کسر می‌شود. سؤال‌ها و خود ذخیره رایگان است.', 'تأیید و ذخیره'))) return null; }
    await uploadPrintImages(rec.questions, rec.id);
    var payload = {id: rec.id, operation_id: S.uuid(), title: rec.title || '', subject: rec.subject || '', duration: parseInt(rec.duration, 10) || 0, questions: combinedQuestions(rec.questions), source_exam_id: rec.sourceExamId || null};
    var raw = await S.rpcObj('native_print_exam_save_v163', {p_payload: payload});
    if (raw && raw.error) { var m = String(raw.error); if (raw.balance != null && raw.required != null) m += '؛ موجودی ' + fa(raw.balance) + ' تومان و مبلغ لازم ' + fa(raw.required) + ' تومان است.'; throw new Error(m); }
    return raw;
  }
  async function printExamDelete(id) { var r = await S.rpcObj('native_print_exam_delete_v163', {p_id: id}); if (r && r.error) throw new Error(String(r.error)); }
  /* انتقال یک‌بارهٔ آزمون‌های چاپی قدیمیِ این مرورگر به سرور (بدون پرسش هزینه: تصاویر قدیمی data: آپلود و شمرده می‌شوند) */
  async function migrateLocalPrintExams() {
    var list = []; try { list = JSON.parse(localStorage.getItem(LS_PRINT) || '[]') || []; } catch (e) {}
    if (!list.length) { try { localStorage.setItem(LS_PRINT_MIGRATED, '1'); } catch (e) {} return 0; }
    if (localStorage.getItem(LS_PRINT_MIGRATED)) return 0;
    var n = 0;
    for (var i = 0; i < list.length; i++) { try { await printExamSave({id: list[i].id, title: list[i].title, subject: list[i].subject, duration: list[i].duration, questions: list[i].questions || [], sourceExamId: list[i].sourceExamId}, {silent: true}); n++; } catch (e) { console.warn('print migrate', e); return n; } }
    try { localStorage.setItem(LS_PRINT_MIGRATED, '1'); localStorage.removeItem(LS_PRINT); } catch (e) {}
    if (n) toast(fa(n) + ' آزمون چاپی این مرورگر به سرور منتقل شد.', 'ok');
    return n;
  }
  function printExamsSection(refresh) {
    var card = el('div', {class: 'card', style: 'margin-top:20px'}, [el('div', {class: 'row'}, [el('h3', {class: 'grow', text: '🖨 آزمون‌های چاپی'}), el('button', {class: 'btn soft sm', text: '➕ آزمون چاپی جدید', onclick: function () { S.go('builder', {mode: 'print'}); }})])]);
    var body = el('div'); card.appendChild(body); S.loading(body);
    (async function () {
      try {
        await migrateLocalPrintExams();
        var list = await printExamsList();
        body.innerHTML = '';
        if (!list.length) { body.appendChild(S.emptyBox('🖨', 'آزمون چاپی ذخیره نشده است. آزمون چاپی بدون کد و بدون مخاطب است و برای چاپ برگه ساخته می‌شود؛ روی سرور ذخیره می‌شود و در اپ و سایت یکی است.')); return; }
        body.appendChild(el('table', {class: 'tbl'}, [
          el('thead', {}, [el('tr', {}, ['عنوان', 'درس', 'سؤال', 'ذخیره', ''].map(function (h) { return el('th', {text: h}); }))]),
          el('tbody', {}, list.map(function (r) {
            return el('tr', {}, [el('td', {html: '<b>' + esc(r.title || 'بدون عنوان') + '</b>'}), el('td', {text: r.subject || '—'}), el('td', {text: fa(r.question_count || 0)}), el('td', {class: 'muted', style: 'font-size:12px', text: S.fmtDate(new Date(r.saved_at || Date.now()).toISOString())}),
              el('td', {}, [el('div', {class: 'acts'}, [
                el('button', {class: 'icon-btn', title: 'ویرایش', html: '✎', onclick: function () { S.go('builder', {mode: 'print', printId: r.id}); }}),
                el('button', {class: 'icon-btn', title: 'پیش‌نمایش و چاپ', html: '🖨', onclick: async function () { try { var full = await printExamGet(r.id); var st = {title: full.title, subject: full.subject, duration: full.duration, questions: draftsFromCombined(full.questions)}; S.openPrintPreview(S.buildPrintPayload(toServerExam(st)), {title: full.title, examId: r.id}); } catch (e) { toast(errMsg(e), 'err'); } }}),
                el('button', {class: 'icon-btn danger', title: 'حذف', html: '🗑', onclick: async function () { if (!(await S.confirmDlg('حذف آزمون چاپی', 'آزمون «' + esc(r.title) + '» برای همیشه حذف شود؟ این کار برگشت‌پذیر نیست.', 'حذف', true))) return; try { await printExamDelete(r.id); toast('حذف شد.', 'ok'); refresh(); } catch (e) { toast(errMsg(e), 'err'); } }})
              ])])]);
          }))
        ]));
      } catch (e) { S.showErr(body, e); }
    })();
    return card;
  }

  var state = null;
  function blankState(mode) {
    return {mode: mode || 'online', examId: null, code: null, title: '', subject: '', duration: '', opensAt: '', closesAt: '', questions: [], shuffleQuestions: false, shuffleOptions: false, negativeMarking: '', teacherMessage: '', attemptsAllowed: 1, attemptOnTimeout: false, gradePolicy: 'last', attemptCooldown: '',
      audienceMode: 'all', audienceClasses: [], audienceStudents: [], audienceSchools: [], availableClasses: [], availableStudents: [], availableSchools: [], selected: 0, dirty: false, printId: null, bankEdit: null};
  }
  function isoToLocal(iso) { if (!iso) return ''; try { var d = new Date(iso); var p = function (n) { return (n < 10 ? '0' : '') + n; }; return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + 'T' + p(d.getHours()) + ':' + p(d.getMinutes()); } catch (e) { return ''; } }
  function localToIso(v) { if (!v) return null; var d = new Date(v); return isNaN(d.getTime()) ? null : d.toISOString(); }
  function saveDraft() { if (!state || state.bankEdit) return; try { localStorage.setItem(LS_DRAFT, JSON.stringify(state)); } catch (e) {} }

  async function page(c, arg) {
    arg = arg || {};
    S.loading(c);
    try {
      var draft = null; try { draft = JSON.parse(localStorage.getItem(LS_DRAFT) || 'null'); } catch (e) {}
      if (arg.importPkg) { var pk = arg.importPkg; state = blankState('online'); state.title = pk.title; state.subject = pk.subject; state.duration = pk.duration ? String(pk.duration) : ''; state.negativeMarking = pk.negativeMarking ? String(pk.negativeMarking) : ''; state.attemptCooldown = pk.attemptCooldown ? String(pk.attemptCooldown) : ''; state.shuffleQuestions = pk.shuffleQuestions; state.shuffleOptions = pk.shuffleOptions; state.teacherMessage = pk.teacherMessage; state.attemptsAllowed = pk.attemptsAllowed; state.attemptOnTimeout = pk.attemptOnTimeout; state.gradePolicy = pk.gradePolicy; state.opensAt = isoToLocal(pk.opensAtIso); state.closesAt = isoToLocal(pk.closesAtIso); state.questions = pk.questions.map(function (q) { var d = decodeQuestion(q, q); d.id = uuid(); return d; }); state.selected = 0; state.dirty = true; }
      else if (arg.bankEdit) { state = blankState('online'); state.bankEdit = {id: arg.bankEdit.id, cats: arg.bankEdit.cats || []}; state.subject = arg.bankEdit.subject || ''; state.title = 'سؤال بانک'; if (arg.bankEdit.question) state.questions = [decodeQuestion(arg.bankEdit.question, arg.bankEdit.question)]; else state.questions = [newQuestion('multiple')]; state.selected = 0; }
      else if (arg.examId) state = await loadOnline(arg.examId);
      else if (arg.printId) { var rec = await printExamGet(arg.printId); state = blankState('print'); state.printId = rec.id; state.sourceExamId = rec.source_exam_id || null; state.title = rec.title || ''; state.subject = rec.subject || ''; state.duration = rec.duration ? String(rec.duration) : ''; state.questions = draftsFromCombined(rec.questions); }
      else if (arg.mode === 'print') state = blankState('print');
      /* V158.1 — پیش‌نویس فقط وقتی برمی‌گردد که همان مسیر (آنلاین) باشد؛ پیش‌نویس چاپی نباید «آزمون جدید» آنلاین را به سازندهٔ چاپی ببرد (ExamApp.createExam: builderCameFromPrint=false) */
      else if (draft && draft.dirty && draft.mode !== 'print' && !draft.bankEdit && !arg.fresh && (await S.confirmDlg('پیش‌نویس ذخیره‌نشده', 'یک پیش‌نویس آزمون از قبل در این مرورگر مانده است («' + esc(draft.title || 'بدون عنوان') + '»، ' + fa((draft.questions || []).length) + ' سؤال). ادامه می‌دهید؟', 'ادامهٔ پیش‌نویس'))) state = draft;
      else state = blankState('online');
      if (state.mode === 'online' && !state.availableClasses.length) await loadOptions(state);
      c.innerHTML = ''; c.appendChild(buildUI(c));
    } catch (e) { S.showErr(c, e); }
  }
  async function loadOptions(st) {
    var r = await Promise.all([S.rpc('my_classes', {}).catch(function () { return []; }), S.rpc('my_students_for_pick', {}).catch(function () { return []; }), S.rpcObj('native_teacher_schools_v61', {}).catch(function () { return {items: []}; })]);
    st.availableClasses = (r[0] || []).map(function (k) { return {id: k.id, name: k.name}; });
    st.availableStudents = (r[1] || []).map(function (s) { return {id: s.id, name: s.full_name, classNames: s.class_names}; });
    st.availableSchools = (r[2].items || []).map(function (s) { return {id: s.id, name: s.name, city: s.city}; });
  }
  async function loadOnline(examId) {
    var st = blankState('online');
    var exam = await S.api.examDetail(examId);
    var keys = {}; (Array.isArray(exam.__answers) ? exam.__answers : []).forEach(function (k, i) { if (k && typeof k === 'object') keys[k.i != null ? k.i : i] = k; });
    st.examId = exam.id; st.code = exam.code; st.title = exam.title || ''; st.subject = exam.subject || ''; st.duration = exam.duration != null ? String(exam.duration) : ''; st.opensAt = isoToLocal(exam.opens_at); st.closesAt = isoToLocal(exam.closes_at);
    st.questions = (Array.isArray(exam.questions) ? exam.questions : []).map(function (q, i) { return decodeQuestion(q, keys[i]); });
    st.shuffleQuestions = !!exam.shuffle_q; st.shuffleOptions = !!exam.shuffle_opt; st.negativeMarking = exam.neg_marking ? String(exam.neg_marking) : ''; st.teacherMessage = exam.teacher_message || '';
    st.attemptsAllowed = Math.max(1, Math.min(5, Number(exam.attempts_allowed) || 1)); st.attemptOnTimeout = !!exam.attempt_on_timeout; st.gradePolicy = exam.grade_policy || 'last'; st.attemptCooldown = exam.attempt_cooldown ? String(exam.attempt_cooldown) : '';
    await loadOptions(st);
    try { var a = await S.rpcObj('get_exam_audience', {p_exam: examId}); st.audienceMode = a.mode || 'all'; st.audienceClasses = a.classes || []; st.audienceStudents = a.students || []; } catch (e) {}
    try { var sc = await S.rpcObj('native_exam_audience_schools_v61', {p_exam: examId}); if (sc.schools && sc.schools.length) { st.audienceMode = 'schools'; st.audienceSchools = sc.schools; st.audienceStudents = []; } } catch (e) {}
    return st;
  }


  /* ================================================================ نمایش توکن‌ها در کادر متن (آینهٔ FigTokenVisuals اپ)
     مقدار واقعی textarea همان متن خام است؛ لایهٔ روکش، %%FIG:{…}%% را «⟦نوع⟧» و $…$ را «⟦فرمول⟧» نشان می‌دهد
     و پیش‌نمایش واقعی زیر کادر رندر می‌شود. */
  var FIG_RE = /%%FIG:(\{[\s\S]*?\})%%/g, TEX_RE = /\$([^$]+)\$/g;
  function figChipLabel(json) {
    var spec = null; try { spec = JSON.parse(json); } catch (e) { return 'شکل'; }
    var k = spec && spec.k, X = (spec && spec.X) || {};
    if (k === 't') return 'جدول'; if (k === 'p') return 'جدول تناوبی'; if (k === 'a') return spec.t === 'photo' ? 'تصویر' : 'آناتومی'; if (k === 's') return 'فیزیک/شیمی';
    return (X.title && String(X.title).trim()) || 'شکل/نمودار';
  }
  function maskedHtml(text) {
    var out = '', last = 0, re = /%%FIG:(\{[\s\S]*?\})%%|\$([^$]+)\$/g, m;
    while ((m = re.exec(text))) {
      out += esc(text.slice(last, m.index));
      if (m[1] != null) out += '<span class="b-chip fig">⟦' + esc(figChipLabel(m[1])) + '⟧</span>';
      else out += '<span class="b-chip tex" title="' + esc(m[2]) + '">⟦فرمول⟧</span>';
      last = m.index + m[0].length;
    }
    out += esc(text.slice(last));
    return out.replace(/\n/g, '<br>') + (/\n$/.test(text) ? '&nbsp;' : '');
  }
  var previewFrame = null, previewReady = null;
  function ensurePreviewFrame() {
    if (previewReady) return previewReady;
    previewReady = new Promise(function (resolve) {
      var f = el('iframe', {style: 'position:fixed;width:0;height:0;border:0;opacity:0;pointer-events:none', title: 'builder-math'});
      document.body.appendChild(f); previewFrame = f;
      f.addEventListener('load', function () { var w = f.contentWindow, tries = 0; (function go() { tries++; if (w.renderRichText && w.GeoFig) return resolve(w); if (tries < 200) setTimeout(go, 50); else resolve(null); })(); });
      S.engineHtml('print').then(function (h) { if (h) f.srcdoc = h; else resolve(null); });
    });
    return previewReady;
  }
  function previewCss() {
    if (document.getElementById('bMathCss')) return;
    ensurePreviewFrame().then(function (w) {
      if (!w) return; var out = [];
      var keep = /\.(mathx|mfrac|mnum|mden|msqrt|mroot|msup|msub|mrow|mtable|mtr|mtd|mover|munder|mo|mi|mn|math-[a-z-]+|qmf-fig|fig-[a-z-]+|interactive-figure|vt-[a-z-]+|tf-[a-z-]+|gf-svg|pt-[a-z-]+)\b/;
      Array.prototype.forEach.call(w.document.styleSheets, function (sh) {
        var rules; try { rules = sh.cssRules; } catch (e) { return; }
        Array.prototype.forEach.call(rules, function (r) {
          if (r.type === 1 && r.selectorText && keep.test(r.selectorText) && !/^(html|body|\*)/.test(r.selectorText)) out.push(r.selectorText.split(',').map(function (x) { return '.b-live ' + x.trim(); }).join(',') + '{' + r.style.cssText + '}');
          else if (r.type === 5 && /math|mfrac|frac/i.test(r.cssText)) out.push(r.cssText);
        });
      });
      var st = document.createElement('style'); st.id = 'bMathCss'; st.textContent = out.join('\n'); document.head.appendChild(st);
    });
  }
  var previewTimer = null;
  function livePreview(box, text) {
    clearTimeout(previewTimer);
    if (!/\$[^$]+\$|%%FIG:/.test(text)) { box.style.display = 'none'; box.innerHTML = ''; return; }
    box.style.display = '';
    previewTimer = setTimeout(async function () {
      var w = await ensurePreviewFrame(); if (!w) { box.style.display = 'none'; return; }
      try { box.innerHTML = w.renderRichText(String(text), null); } catch (e) { box.style.display = 'none'; }
    }, 200);
  }
  /* ویرایشگر تراشه‌ای (همیشه، حتی حین تایپ، توکن‌ها تراشه‌اند — مثل VisualTransformation اپ).
     textarea مخفی منبع حقیقت و نقطهٔ اتصال ابزارها (selectionStart/End) می‌ماند؛ div contenteditable «نما» است. */
  function tokenTextarea(ta) {
    var wrap = el('div', {class: 'b-ta-wrap'});
    var rich = el('div', {class: 'b-rich', contenteditable: 'true', dir: 'rtl', spellcheck: 'false', 'data-ph': ta.placeholder || ''});
    var live = el('div', {class: 'b-live', style: 'display:none'});
    previewCss();
    ta.style.display = 'none';
    var TOKEN_RE = /%%FIG:(\{[\s\S]*?\})%%|\$([^$]+)\$/g;
    function chip(tok, label, cls) { var c = el('span', {class: 'b-chip ' + cls, contenteditable: 'false', title: cls === 'tex' ? tok.slice(1, -1) : label, text: '⟦' + label + '⟧'}); c.setAttribute('data-tok', tok); return c; }
    function render(raw) {
      rich.innerHTML = ''; var last = 0, m; TOKEN_RE.lastIndex = 0;
      while ((m = TOKEN_RE.exec(raw))) {
        if (m.index > last) rich.appendChild(document.createTextNode(raw.slice(last, m.index)));
        rich.appendChild(m[1] != null ? chip(m[0], figChipLabel(m[1]), 'fig') : chip(m[0], 'فرمول', 'tex'));
        last = m.index + m[0].length;
      }
      if (last < raw.length) rich.appendChild(document.createTextNode(raw.slice(last)));
    }
    function serialize(node) {
      var out = '';
      Array.prototype.forEach.call(node.childNodes, function (n) {
        if (n.nodeType === 3) out += n.nodeValue;
        else if (n.nodeType === 1) {
          if (n.getAttribute && n.getAttribute('data-tok')) out += n.getAttribute('data-tok');
          else if (n.tagName === 'BR') out += '\n';
          else { var block = /^(DIV|P)$/.test(n.tagName); if (block && out && !/\n$/.test(out)) out += '\n'; out += serialize(n); }
        }
      });
      return out;
    }
    /* موقعیت مکان‌نما بر حسب متن خام → روی textarea مخفی می‌نشیند تا ابزارها همان‌جا درج کنند */
    function caretToRaw() {
      var sel = document.getSelection(); if (!sel || !sel.rangeCount || !rich.contains(sel.anchorNode)) return;
      function off(node, o) { var r = document.createRange(); r.setStart(rich, 0); r.setEnd(node, o); var f = r.cloneContents(); var d = document.createElement('div'); d.appendChild(f); return serialize(d).length; }
      try { var a = off(sel.anchorNode, sel.anchorOffset), b = off(sel.focusNode, sel.focusOffset); ta.selectionStart = Math.min(a, b); ta.selectionEnd = Math.max(a, b); } catch (e) {}
    }
    function placeCaret(rawPos) {
      /* مکان‌نما را در نما روی موقعیت خام می‌گذارد (بعد از درج ابزار) */
      var pos = 0, sel = document.getSelection(), r = document.createRange(); var done = false;
      function walk(n) {
        if (done) return;
        if (n.nodeType === 3) { var len = n.nodeValue.length; if (pos + len >= rawPos) { r.setStart(n, Math.max(0, rawPos - pos)); done = true; return; } pos += len; }
        else if (n.nodeType === 1 && n.getAttribute && n.getAttribute('data-tok')) { var l2 = n.getAttribute('data-tok').length; if (pos + l2 >= rawPos) { r.setStartAfter(n); done = true; return; } pos += l2; }
        else if (n.nodeType === 1 && n.tagName === 'BR') { pos += 1; if (pos >= rawPos) { r.setStartAfter(n); done = true; return; } }
        else Array.prototype.forEach.call(n.childNodes, walk);
      }
      walk(rich);
      if (!done) { r.selectNodeContents(rich); r.collapse(false); } else r.collapse(true);
      try { sel.removeAllRanges(); sel.addRange(r); } catch (e) {}
    }
    var fromRich = false;
    rich.addEventListener('input', function () {
      var raw = serialize(rich);
      /* اگر کاربر خودش $…$ یا %%FIG…%% کامل تایپ کرد، به تراشه تبدیل شود */
      var hasNewTok = false; TOKEN_RE.lastIndex = 0; var m; while ((m = TOKEN_RE.exec(raw))) { hasNewTok = true; break; }
      var chipCount = rich.querySelectorAll('[data-tok]').length, tokCount = (raw.match(TOKEN_RE) || []).length;
      fromRich = true; ta.value = raw; ta.dispatchEvent(new Event('input')); fromRich = false;
      if (hasNewTok && tokCount !== chipCount) { caretToRaw(); var p = ta.selectionStart; render(raw); placeCaret(p); }
      livePreview(live, raw);
    });
    ['keyup', 'mouseup', 'focus'].forEach(function (ev) { rich.addEventListener(ev, caretToRaw); });
    /* V181 — یک شنوندهٔ سراسری به‌جای یکی برای هر بار رندر ویرایشگر (نشت: با هر جابه‌جایی سؤال یکی اضافه می‌شد و تایپ کند می‌شد) */
    if (!window.__bRichSel) { window.__bRichSel = function () { var a = document.activeElement; if (a && a.__caretToRaw && a.classList.contains('b-rich')) a.__caretToRaw(); }; document.addEventListener('selectionchange', window.__bRichSel); }
    rich.__caretToRaw = caretToRaw;
    rich.addEventListener('paste', function (e) { e.preventDefault(); var t = (e.clipboardData || window.clipboardData).getData('text/plain'); document.execCommand('insertText', false, t); });
    /* کلیک روی تراشهٔ فرمول → ویرایش همان فرمول */
    rich.addEventListener('click', function (e) {
      var c = e.target.closest && e.target.closest('[data-tok]'); if (!c) return;
      var tok = c.getAttribute('data-tok'); var raw = ta.value; var idx = raw.indexOf(tok); if (idx < 0) return;
      if (c.classList.contains('tex')) { S.openFormulaEditor(raw, idx, idx + tok.length).then(function (t) { if (t != null && t !== ta.value) { ta.value = t; ta.selectionStart = ta.selectionEnd = idx; ta.dispatchEvent(new Event('input')); } }); }
      else { var r = document.createRange(); r.selectNode(c); var sel = document.getSelection(); sel.removeAllRanges(); sel.addRange(r); caretToRaw(); }
    });
    /* تغییر برنامه‌ای متن (ابزارها) → بازسازی نما */
    ta.addEventListener('input', function () { if (fromRich) return; var p = ta.selectionStart; render(ta.value); rich.focus(); placeCaret(p == null ? ta.value.length : p); livePreview(live, ta.value); });
    render(ta.value); livePreview(live, ta.value);
    wrap.appendChild(ta); wrap.appendChild(rich);
    return {wrap: wrap, live: live, sync: function () { render(ta.value); }};
  }
  function buildUI(container) {
    var wrap = el('div', {class: 'builder'});
    var msg = el('div');
    function mark() { state.dirty = true; saveDraft(); }
    /* --- نوار بالا --- */
    var top = el('div', {class: 'card b-top'});
    var title = inp('عنوان آزمون', state.title, function (v) { state.title = v; mark(); });
    var subject = inp('درس', state.subject, function (v) { state.subject = v; mark(); });
    var duration = inp('مدت (دقیقه)', state.duration, function (v) { state.duration = en(v); mark(); }, 'number');
    top.appendChild(el('div', {class: 'row', style: 'margin-bottom:8px'}, [
      el('span', {class: 'chip brand', text: state.bankEdit ? (state.bankEdit.id ? '🏦 ویرایش سؤال بانک' : '🏦 سؤال جدید بانک') : state.mode === 'print' ? '🖨 آزمون چاپی (محلی)' : (state.examId ? '✎ ویرایش آزمون آنلاین' : '➕ آزمون آنلاین جدید')}),
      state.code ? el('span', {class: 'code', text: state.code}) : null,
      el('span', {class: 'grow'}),
      state.mode === 'online' && !state.bankEdit ? el('button', {class: 'btn light sm', text: '⚙ مشخصات آزمون', onclick: openSettings}) : null,
      /* V157 — مسیر چاپ مثل ExamBuilderScreen(printMode): «تنظیمات سربرگ» به‌جای «مشخصات آزمون» */
      state.mode === 'print' && !state.bankEdit ? el('button', {class: 'btn light sm', text: '🏷 تنظیمات سربرگ', onclick: function () { S.openHeaderSettings(); }}) : null,
      state.bankEdit ? el('button', {class: 'btn light sm', text: '↩ بازگشت به بانک', onclick: function () { S.go('bank'); }}) : null,
      el('button', {class: 'btn soft sm', text: '👁 پیش‌نمایش / چاپ', onclick: function () { preview(); }}),
      el('button', {class: 'btn sm', text: state.bankEdit ? '🏦 ذخیره در بانک' : state.mode === 'print' ? '☁ ذخیره آزمون چاپی' : '☁ ذخیره در سرور', onclick: save})
    ]));
    /* V159 — مثل ExamBuilderScreen: آنلاین → عنوان/درس/مدت داخل «مشخصات آزمون» (ExamSettingsCard)؛ چاپی → بدون این فیلدها (نام هنگام ذخیره پرسیده می‌شود) */
    if (state.bankEdit) top.appendChild(el('div', {class: 'grid3'}, [subject]));
    else if (state.mode === 'online') top.appendChild(el('div', {class: 'b-meta muted', text: [state.title, state.subject, state.duration ? fa(state.duration) + ' دقیقه' : ''].filter(Boolean).join(' · ') || 'عنوان، درس و مدت را در «مشخصات آزمون» وارد کنید.'}));
    top.appendChild(msg);
    wrap.appendChild(top);

    /* --- بدنه: فهرست سؤال‌ها + ویرایشگر --- */
    var body = el('div', {class: 'b-body' + (!state.bankEdit && (state.mode === 'online' || (state.mode === 'print' && S.headerSettingsForm)) ? ' has-settings' : '')});
    var list = el('div', {class: 'b-list card'});
    var editor = el('div', {class: 'b-editor card'});
    /* V166 — دسکتاپ (body.dk): ستون راست «مشخصات آزمون» همیشه باز + ریل چپ (افزودن/بعدی/ذخیره/چاپ/پیش‌نمایش + شمارهٔ سؤال‌ها)؛ فهرست b-list فقط در گوشی/تبلت */
    var settings = null;
    if (state.mode === 'online' && !state.bankEdit) { settings = el('aside', {class: 'b-settings card'}, [el('h3', {text: 'مشخصات آزمون'})]); settingsForm(settings); settings.addEventListener('input', syncMeta); }
    /* V173 — سازندهٔ چاپی دسکتاپ مثل آنلاین: ستون راست = «تنظیمات سربرگ» (ExamBuilderScreen printMode) با ذخیرهٔ خودکار */
    else if (state.mode === 'print' && !state.bankEdit && S.headerSettingsForm) { settings = el('aside', {class: 'b-settings b-hdr card'}, [el('h3', {text: 'تنظیمات سربرگ'})]); S.headerSettingsForm(settings, {autosave: true}); }
    var rail = el('nav', {class: 'b-rail', 'aria-label': 'ابزار سازنده'});
    if (settings) body.appendChild(settings);
    body.appendChild(list); body.appendChild(editor); wrap.appendChild(body); wrap.appendChild(rail);
    function addMenu(anchor) {
      /* V178 — دسکتاپ: همان منوی شعاعی اپ (BuilderRadialMenuOverlay) به‌جای فهرست کوچک */
      if (window.SiteMobile && window.SiteMobile.radialMenu) {
        if (document.querySelector('.m-radial-bg')) { document.querySelector('.m-radial-bg').remove(); return; }
        window.SiteMobile.radialMenu(function (key) {
          if (key === 'import') { if (window.SiteExtras) window.SiteExtras.importExam(); return; }
          if (key === 'bank') { if (state.mode === 'online') openBank(); else toast('بانک سؤال فقط در آزمون آنلاین در دسترس است.', 'err'); return; }
          state.questions.push(newQuestion(key)); state.selected = state.questions.length - 1; mark(); drawList(); drawEditor();
        }, null, {cls: 'dk-radial', emoji: true});
        return;
      }
      var old = document.getElementById('b-addmenu'); if (old) { old.remove(); return; }
      var menu = el('div', {class: 'b-addmenu card', id: 'b-addmenu'}, TYPES.map(function (t) { return el('button', {class: 'btn light sm', text: t[2] + ' ' + t[1], onclick: function () { menu.remove(); state.questions.push(newQuestion(t[0])); state.selected = state.questions.length - 1; mark(); drawList(); drawEditor(); }}); })
        .concat(state.mode === 'online' ? [el('button', {class: 'btn soft sm', text: '🏦 از بانک سؤال', onclick: function () { menu.remove(); openBank(); }})] : []));
      var r = anchor.getBoundingClientRect(); menu.style.top = r.top + 'px'; menu.style.left = (r.right + 10) + 'px';
      document.body.appendChild(menu);
      setTimeout(function () { document.addEventListener('click', function h(e) { if (!menu.contains(e.target) && e.target !== anchor) { menu.remove(); document.removeEventListener('click', h); } }); }, 0);
    }
    /* V173 — منوی دکمهٔ چاپ ریل (V97 اپ: «چاپ آزمون (دانش‌آموز)» / «چاپ با کلید (پاسخ‌نامه)») */
    function printMenu(anchor) {
      var old = document.getElementById('b-addmenu'); if (old) { old.remove(); return; }
      var menu = el('div', {class: 'b-addmenu card', id: 'b-addmenu'}, [
        el('button', {class: 'btn light sm', text: '🖨 چاپ آزمون (دانش‌آموز)', onclick: function () { menu.remove(); preview('student'); }}),
        el('button', {class: 'btn light sm', text: '✅ چاپ با کلید (پاسخ‌نامه)', onclick: function () { menu.remove(); preview('teacher'); }})
      ]);
      var r = anchor.getBoundingClientRect(); menu.style.top = r.top + 'px'; menu.style.left = (r.right + 10) + 'px';
      document.body.appendChild(menu);
      setTimeout(function () { document.addEventListener('click', function h(e) { if (!menu.contains(e.target) && e.target !== anchor) { menu.remove(); document.removeEventListener('click', h); } }); }, 0);
    }
    /* V173 — پیش‌نمایش دانش‌آموزی سؤال جاری (StudentQuestionPreviewDialog اپ): فقط نمایشی و غیرفعال */
    function studentPreview() {
      var q = state.questions[state.selected]; if (!q) return toast('ابتدا یک سؤال اضافه کنید.', 'err');
      var n = state.selected + 1;
      var bg = el('div', {class: 'modal-bg', onclick: function (e) { if (e.target === bg) bg.remove(); }});
      var card = el('div', {class: 'card b-sp-card'});
      card.appendChild(el('div', {class: 'b-sp-num', text: 'سؤال ' + fa(n) + ' (' + fa(S.fmtScore(q.score)) + ' نمره)'}));
      var txt = el('div', {class: 'b-sp-text', html: esc(q.text || 'متن سؤال').replace(/\n/g, '<br>')}); card.appendChild(txt);
      if (window.SiteStudent && window.SiteStudent.richHtml) window.SiteStudent.richHtml(q.text || 'متن سؤال').then(function (h) { txt.innerHTML = h; });
      (q.images || []).forEach(function (u) { card.appendChild(el('img', {src: u, alt: 'تصویر سؤال', class: 'b-sp-img'})); });
      var AB = ['الف', 'ب', 'ج', 'د', 'ه', 'و', 'ز', 'ح'];
      if (q.type === 'multiple') (q.options || []).forEach(function (o, i) { card.appendChild(el('label', {class: 'st-opt'}, [el('input', {type: 'radio', disabled: 'disabled'}), el('span', {class: 'st-optlabel', text: (AB[i] || fa(i + 1)) + ')'}), el('span', {class: 'grow', text: o || ('گزینه ' + fa(i + 1))}), q.optionImages && q.optionImages[i] ? el('img', {src: q.optionImages[i], class: 'thumb'}) : null])); });
      else if (q.type === 'truefalse') card.appendChild(el('div', {class: 'row'}, [el('button', {class: 'btn light', text: '✓ صحیح', disabled: 'disabled'}), el('button', {class: 'btn light', text: '✗ غلط', disabled: 'disabled'})]));
      else if (q.type === 'fill' || q.type === 'numeric') card.appendChild(el('input', {type: 'text', class: 'st-input', disabled: 'disabled', placeholder: q.type === 'numeric' ? 'پاسخ عددی' : 'پاسخ جای خالی'}));
      else if (q.type === 'matching') {
        (q.matchingLeft || []).forEach(function (l, li) { card.appendChild(el('div', {class: 'st-ml'}, [el('span', {class: 'grow', text: fa(li + 1) + '. ' + (l || 'ستون راست')}), el('div', {class: 'st-chips'}, (q.matchingRight || []).map(function (_, ri) { return el('button', {class: 'chip', text: AB[ri] || fa(ri + 1), disabled: 'disabled'}); }))])); });
      } else card.appendChild(el('textarea', {class: 'st-input', rows: 5, disabled: 'disabled', placeholder: 'پاسخ تشریحی'}));
      var box = el('div', {class: 'modal b-sp-modal'}, [
        el('div', {class: 'row', style: 'margin-bottom:8px'}, [el('h2', {class: 'grow', text: 'پیش‌نمایش دانش‌آموز', style: 'margin:0'}), el('button', {class: 'x', text: '✕', 'aria-label': 'بستن', onclick: function () { bg.remove(); }})]),
        el('div', {class: 'b-sp-body'}, [card]),
        el('div', {class: 'row', style: 'margin-top:10px;justify-content:flex-start'}, [el('button', {class: 'btn light', text: 'بستن', onclick: function () { bg.remove(); }})])
      ]);
      bg.appendChild(box); document.body.appendChild(bg);
    }
    function drawRail() {
      rail.innerHTML = '';
      function rb(icon, label, on, cls) { return el('button', {class: 'b-rail-btn ' + (cls || ''), title: label, 'aria-label': label, html: icon, onclick: on}); }
      rail.appendChild(rb('<svg viewBox="0 0 24 24"><path d="M12 5v14M5 12h14"/></svg>', 'افزودن سؤال', function (e) { addMenu(e.currentTarget); }, 'add'));
      /* V173 — مثل FABهای ExamBuilderScreen: آنلاین = ذخیره + چشم (پیش‌نمایش دانش‌آموزی سؤال، V62.7)؛ چاپی = ذخیره + چشم (پیش‌نمایش برگه) + چاپ (منوی چاپ آزمون/چاپ با کلید) */
      var EYE = '<svg viewBox="0 0 24 24"><path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12z"/><circle cx="12" cy="12" r="3"/></svg>';
      rail.appendChild(rb('<svg viewBox="0 0 24 24"><path d="M5 12l5 5L20 7"/></svg>', state.bankEdit ? 'ذخیره در بانک' : 'ذخیره', save, 'ok'));
      if (state.mode === 'print') {
        rail.appendChild(rb(EYE, 'پیش‌نمایش آزمون', function () { preview(); }));
        rail.appendChild(rb('<svg viewBox="0 0 24 24"><path d="M7 8V4h10v4M5 8h14a2 2 0 0 1 2 2v6h-4v4H7v-4H3v-6a2 2 0 0 1 2-2z"/></svg>', 'چاپ آزمون', function (e) { printMenu(e.currentTarget); }));
      } else {
        rail.appendChild(rb(EYE, 'پیش‌نمایش دانش‌آموز', function () { studentPreview(); }));
      }
      rail.appendChild(el('span', {class: 'b-rail-sep'}));
      var nums = el('div', {class: 'b-rail-nums'});
      state.questions.forEach(function (q, i) { nums.appendChild(el('button', {class: 'b-rail-num' + (i === state.selected ? ' active' : ''), title: 'سؤال ' + fa(i + 1) + ' — ' + TYPE_LABEL[q.type], text: fa(i + 1), onclick: function () { state.selected = i; drawList(); drawEditor(); }})); });
      rail.appendChild(nums);
    }

    function drawList() {
      list.innerHTML = '';
      list.appendChild(el('div', {class: 'row', style: 'margin-bottom:10px'}, [el('b', {class: 'grow', text: 'سؤال‌ها (' + fa(state.questions.length) + ')'}), el('span', {class: 'muted', style: 'font-size:12px', text: 'جمع بارم: ' + fa(S.fmtScore(state.questions.reduce(function (s, q) { return s + (Number(q.score) || 0); }, 0)))})]));
      var addRow = el('div', {class: 'b-add'});
      TYPES.forEach(function (t) { addRow.appendChild(el('button', {class: 'btn light sm', title: t[1], text: t[2] + ' ' + t[1], onclick: function () { state.questions.push(newQuestion(t[0])); state.selected = state.questions.length - 1; mark(); drawList(); drawEditor(); }})); });
      list.appendChild(addRow);
      if (state.mode === 'online') list.appendChild(el('button', {class: 'btn soft sm', style: 'width:100%;margin-top:8px', text: '🏦 از بانک سؤال', onclick: openBank}));
      var ul = el('div', {class: 'b-qs'});
      state.questions.forEach(function (q, i) {
        var row = el('div', {class: 'b-q' + (i === state.selected ? ' on' : ''), onclick: function () { state.selected = i; drawList(); drawEditor(); }}, [
          el('span', {class: 'n', text: fa(i + 1)}),
          el('span', {class: 'grow t', text: (q.text || '').replace(/%%FIG:[\s\S]*?%%/g, '[شکل]').replace(/\$[^$]*\$/g, '[فرمول]').slice(0, 60) || '— بدون متن —'}),
          el('span', {class: 'chip', text: TYPE_LABEL[q.type]}),
          el('span', {class: 'chip brand', text: fa(S.fmtScore(q.score))})
        ]);
        ul.appendChild(row);
      });
      if (!state.questions.length) ul.appendChild(el('div', {class: 'empty', style: 'padding:20px', text: (document.getElementById('m-shell') ? 'با دکمهٔ + سؤال اضافه کنید.' : 'با دکمه‌های بالا سؤال اضافه کنید.')}));
      list.appendChild(ul);
      drawRail();
    }
    function drawEditor() {
      editor.innerHTML = '';
      var q = state.questions[state.selected];
      if (!q) { editor.appendChild(el('div', {class: 'empty'}, [el('div', {class: 'big', text: '✎'}), el('div', {text: 'سؤالی انتخاب نشده است. با دکمهٔ + سؤال اضافه کنید.'})])); return; }
      var i = state.selected;
      editor.appendChild(el('div', {class: 'row b-qhead', style: 'margin-bottom:10px'}, [
        el('h3', {class: 'grow', text: 'سؤال ' + fa(i + 1) + ' — ' + TYPE_LABEL[q.type]}),
        el('button', {class: 'icon-btn', title: 'بالا', html: '↑', onclick: function () { if (i > 0) { swap(i, i - 1); } }}),
        el('button', {class: 'icon-btn', title: 'پایین', html: '↓', onclick: function () { if (i < state.questions.length - 1) swap(i, i + 1); }}),
        el('button', {class: 'icon-btn', title: 'کپی', html: '⧉', onclick: function () { var cp = JSON.parse(JSON.stringify(q)); cp.id = uuid(); state.questions.splice(i + 1, 0, cp); state.selected = i + 1; mark(); drawList(); drawEditor(); }}),
        el('button', {class: 'icon-btn danger', title: 'حذف', html: '🗑', onclick: async function () { if (!(await S.confirmDlg('حذف سؤال', 'سؤال ' + fa(i + 1) + ' حذف شود؟', 'حذف', true))) return; state.questions.splice(i, 1); state.selected = Math.max(0, Math.min(i, state.questions.length - 1)); mark(); drawList(); drawEditor(); }})
      ]));
      /* متن سؤال + ابزار درج */
      var ta = el('textarea', {rows: 4, style: 'width:100%;border:1px solid var(--line);border-radius:10px;padding:10px;font-size:15px', placeholder: 'متن سؤال… (فرمول‌ها بین $…$، شکل‌ها به‌صورت %%FIG:{…}%%)'});
      ta.value = q.text;
      ta.addEventListener('input', function () { q.text = ta.value; mark(); drawListSoft(); });
      var tools = el('div', {class: 'b-tools'}, [
        el('button', {class: 'tool-btn', title: 'فرمول', 'aria-label': 'فرمول', text: '🧮', onclick: function () { var s = ta.selectionStart, e = ta.selectionEnd; S.openFormulaEditor(ta.value, s, e).then(function (t) { if (t != null && t !== ta.value) { ta.value = t; ta.dispatchEvent(new Event('input')); } }); }}),
        el('button', {class: 'tool-btn', title: 'شکل هندسی', 'aria-label': 'شکل هندسی', text: '📐', onclick: function () { insertFigure('geo', ta, q); }}),
        el('button', {class: 'tool-btn', title: 'نمودار / محور', 'aria-label': 'نمودار / محور', text: '📈', onclick: function () { insertFigure('graph', ta, q); }}),
        el('button', {class: 'tool-btn', title: 'جدول', 'aria-label': 'جدول', text: '▦', onclick: function () { insertFigure('table', ta, q); }}),
        el('button', {class: 'tool-btn', title: 'جدول تناوبی', 'aria-label': 'جدول تناوبی', text: '⚛', onclick: function () { insertFigure('periodic', ta, q); }}),
        el('button', {class: 'tool-btn', title: 'آناتومی', 'aria-label': 'آناتومی', text: '🫀', onclick: function () { insertFigure('anatomy', ta, q); }}),
        el('button', {class: 'tool-btn', title: 'علوم', 'aria-label': 'علوم', text: '🔬', onclick: function () { insertFigure('science', ta, q); }}),
        /* V159 — استودیوی ویرایش و تصویر (ExamImageStudioDialog)؛ dataURL برمی‌گرداند؛ در مسیر آنلاین هنگام ذخیره آپلود می‌شود */
        el('button', {class: 'tool-btn', title: 'استودیوی ویرایش و تصویر', 'aria-label': 'استودیوی ویرایش و تصویر', text: '🖼', onclick: function () {
          var addImg = function (u) { return {uri: u, xMm: 20, yMm: 30, widthMm: 55}; };
          window.SiteStudio.open({existing: q.images,
            onInsert: function (u) { q.images.push(addImg(u)); mark(); drawEditor(); toast('تصویر افزوده شد.', 'ok'); },
            onReplace: function (k, u) { if (q.images[k]) q.images[k].uri = u; mark(); drawEditor(); toast('تصویر جایگزین شد.', 'ok'); },
            onDelete: function (k) { q.images.splice(k, 1); mark(); drawEditor(); },
            onSplitToSame: function (urls) { urls.forEach(function (u) { q.images.push(addImg(u)); }); mark(); drawEditor(); toast(fa(urls.length) + ' تصویر افزوده شد.', 'ok'); },
            onSplitToQuestions: function (urls) { urls.forEach(function (u, j) { if (j === 0) { q.images.push(addImg(u)); return; } var nq = newQuestion(q.type); nq.images.push(addImg(u)); state.questions.splice(i + j, 0, nq); }); mark(); drawList(); drawEditor(); toast(fa(urls.length) + ' سؤال ساخته شد.', 'ok'); }});
        }}),
        el('button', {class: 'tool-btn', title: 'گفتار به متن', 'aria-label': 'گفتار به متن', text: '🎤', onclick: function () { dictate(ta, q); }}),
        window.SiteExtras ? el('button', {class: 'tool-btn' + (q.audio ? ' on' : ''), title: q.audio ? 'صوت سؤال (دارد)' : 'صوت سؤال', 'aria-label': 'صوت سؤال', text: '🎙', onclick: function () { window.SiteExtras.audioDlg(q, state.examId || (state.examId = uuid()), state.mode === 'print', function () { mark(); drawEditor(); }); }}) : null
      ]);
      var tt = tokenTextarea(ta);
      editor.appendChild(el('div', {class: 'field'}, [el('label', {text: 'متن سؤال'}), tt.wrap, tools, tt.live]));
      /* V166 — تغییر نوع سؤال با تراشه‌ها (مثل QuestionTypeChips اپ) */
      editor.appendChild(el('div', {class: 'b-types'}, TYPES.map(function (t) { return el('button', {class: 'chip' + (q.type === t[0] ? ' on' : ''), text: t[1], onclick: function () { if (q.type === t[0]) return; changeType(q, t[0]); mark(); drawList(); drawEditor(); }}); })));
      if (q.images.length) {
        var ig = el('div', {class: 'b-imgs'});
        q.images.forEach(function (m, k) { ig.appendChild(el('div', {class: 'b-img'}, [el('img', {src: m.uri}), el('button', {class: 'x', text: '✕', onclick: function () { q.images.splice(k, 1); mark(); drawEditor(); }})])); });
        editor.appendChild(ig);
      }
      /* بارم + قالب */
      var fmt = el('div', {class: 'grid4'}, [
        inp('بارم', String(q.score), function (v) { q.score = parseFloat(en(v)) || 0; mark(); drawListSoft(); }, 'number'),
        sel('تراز متن', q.textAlign, [['right', 'راست'], ['center', 'وسط'], ['left', 'چپ'], ['justify', 'دوطرفه']], function (v) { q.textAlign = v; mark(); }),
        sel('فونت', q.fontFamily, [['default', 'پیش‌فرض'], ['Vazirmatn', 'وزیرمتن'], ['Shabnam', 'شبنم'], ['Sahel', 'ساحل'], ['BNazanin', 'ب نازنین'], ['Tahoma', 'تاهوما']], function (v) { q.fontFamily = v; mark(); }),
        inp('اندازهٔ فونت', String(q.fontSizeSp), function (v) { q.fontSizeSp = Math.max(8, Math.min(40, parseFloat(en(v)) || 16)); mark(); }, 'number')
      ]);
      editor.appendChild(fmt);
      editor.appendChild(el('div', {class: 'row', style: 'margin:-6px 0 12px'}, [chk('ضخیم', q.bold, function (v) { q.bold = v; mark(); }), chk('مورب', q.italic, function (v) { q.italic = v; mark(); })]));
      /* بخش مخصوص نوع */
      editor.appendChild(typeSection(q));
      /* فضای پاسخ (چاپ) */
      if (q.type === 'essay' || q.type === 'fill' || q.type === 'numeric') {
        editor.appendChild(el('h4', {text: 'فضای پاسخ در برگهٔ چاپی', style: 'margin-top:14px'}));
        editor.appendChild(el('div', {class: 'grid3'}, [
          inp('تعداد خط', String(q.answerLines), function (v) { q.answerLines = Math.max(0, Math.min(12, parseInt(en(v), 10) || 0)); mark(); }, 'number'),
          sel('نوع خط', q.answerLineStyle, [['lined', 'خط‌دار'], ['blank', 'ساده'], ['grid', 'شطرنجی']], function (v) { q.answerLineStyle = v; mark(); }),
          inp('فاصلهٔ خط (cm)', String(q.answerLineSpacingCm), function (v) { q.answerLineSpacingCm = Math.max(0.5, Math.min(2, parseFloat(en(v)) || 1)); mark(); }, 'number')
        ]));
      }
      /* پاسخ تصویری (آنلاین) */
      if (state.mode === 'online' && (q.type === 'essay' || q.type === 'numeric' || q.type === 'fill')) {
        editor.appendChild(el('h4', {text: 'پاسخ تصویری / تختهٔ دانش‌آموز', style: 'margin-top:14px'}));
        editor.appendChild(el('div', {class: 'grid3'}, [
          sel('ارسال تصویر پاسخ', q.answerImageMode, [['no', 'غیرفعال'], ['optional', 'اختیاری'], ['required', 'اجباری']], function (v) { q.answerImageMode = v; mark(); drawEditor(); }),
          q.answerImageMode !== 'no' ? inp('حداکثر تصویر', String(q.maxAnswerImages || 1), function (v) { q.maxAnswerImages = Math.max(1, Math.min(10, parseInt(en(v), 10) || 1)); mark(); }, 'number') : el('div'),
          el('div', {class: 'field'}, [el('label', {text: ' '}), chk('اجازهٔ رسم نمودار/تخته', q.allowAnswerGraph, function (v) { q.allowAnswerGraph = v; mark(); })])
        ]));
      }
      function drawListSoft() { var rows = list.querySelectorAll('.b-q'); var r = rows[state.selected]; if (r) { r.querySelector('.t').textContent = (q.text || '').replace(/%%FIG:[\s\S]*?%%/g, '[شکل]').replace(/\$[^$]*\$/g, '[فرمول]').slice(0, 60) || '— بدون متن —'; r.querySelector('.chip.brand').textContent = fa(S.fmtScore(q.score)); } }
    }
    /* V166 — تغییر نوع بدون از دست رفتن متن/بارم/تصاویر؛ فیلدهای نوع مثل newQuestion */
    function changeType(q, t) {
      var fresh = newQuestion(t);
      q.type = t; q.options = fresh.options; q.optionImages = fresh.optionImages; q.correctIndex = null; q.expectedText = fresh.expectedText; q.expectedNumber = ''; q.matchingPairs = {};
      if (t === 'matching' && (!q.matchingLeft || q.matchingLeft.length < 2)) { q.matchingLeft = ['', '']; q.matchingRight = ['', '']; }
      if (t === 'essay' && !(q.answerLines > 2)) q.answerLines = 5;
    }
    function swap(a, b) { var t = state.questions[a]; state.questions[a] = state.questions[b]; state.questions[b] = t; state.selected = b; mark(); drawList(); drawEditor(); }
    function typeSection(q) {
      var box = el('div', {class: 'b-type'});
      function redraw() { var n = typeSection(q); box.replaceWith(n); }
      if (q.type === 'multiple') {
        box.appendChild(el('h4', {text: 'گزینه‌ها (پاسخ درست را علامت بزنید)'}));
        q.options.forEach(function (o, k) {
          var r = el('input', {type: 'radio', name: 'correct_' + q.id}); r.checked = q.correctIndex === k; r.addEventListener('change', function () { q.correctIndex = k; mark(); });
          var t = el('input', {type: 'text', value: o, placeholder: 'گزینهٔ ' + fa(k + 1), style: 'flex:1'}); t.addEventListener('input', function () { q.options[k] = t.value; mark(); });
          var img = q.optionImages[k];
          box.appendChild(el('div', {class: 'b-opt'}, [r, t,
            img ? el('img', {src: img, class: 'thumb'}) : null,
            el('button', {class: 'icon-btn', title: 'تصویر گزینه', html: img ? '🖼✕' : '🖼', onclick: function () { if (img) { q.optionImages[k] = null; mark(); redraw(); return; } window.SiteStudio.open({onInsert: function (u) { q.optionImages[k] = u; mark(); redraw(); }}); }}),
            el('button', {class: 'icon-btn', title: 'فرمول', html: '∑', onclick: function () { S.openFormulaEditor(q.options[k], null, null).then(function (v) { if (v != null) { q.options[k] = v; mark(); redraw(); } }); }}),
            el('button', {class: 'icon-btn danger', html: '✕', title: 'حذف گزینه', onclick: function () { if (q.options.length <= 2) return toast('حداقل دو گزینه لازم است.', 'err'); q.options.splice(k, 1); q.optionImages.splice(k, 1); if (q.correctIndex === k) q.correctIndex = null; else if (q.correctIndex > k) q.correctIndex--; mark(); redraw(); }})
          ]));
        });
        box.appendChild(el('button', {class: 'btn light sm', text: '➕ گزینه', onclick: function () { if (q.options.length >= 8) return; q.options.push(''); q.optionImages.push(null); mark(); redraw(); }}));
      } else if (q.type === 'truefalse') {
        box.appendChild(el('h4', {text: 'پاسخ درست'}));
        box.appendChild(el('div', {class: 'row'}, [['true', 'صحیح'], ['false', 'غلط']].map(function (o) { var r = el('input', {type: 'radio', name: 'tf_' + q.id}); r.checked = q.expectedText === o[0]; r.addEventListener('change', function () { q.expectedText = o[0]; mark(); }); return el('label', {class: 'row', style: 'gap:6px'}, [r, o[1]]); })));
      } else if (q.type === 'fill') {
        box.appendChild(inp('پاسخ‌های قابل‌قبول (با | جدا کنید)', q.expectedText, function (v) { q.expectedText = v; mark(); }));
        box.appendChild(chk('حساس به بزرگی/کوچکی حروف', q.caseSensitive, function (v) { q.caseSensitive = v; mark(); }));
      } else if (q.type === 'numeric') {
        box.appendChild(el('div', {class: 'grid2'}, [inp('پاسخ عددی', q.expectedNumber, function (v) { q.expectedNumber = en(v); mark(); }), inp('خطای مجاز (±)', q.tolerance, function (v) { q.tolerance = en(v); mark(); })]));
      } else if (q.type === 'matching') {
        box.appendChild(el('h4', {text: 'ستون راست ← ستون چپ (پاسخ درست را از فهرست انتخاب کنید)'}));
        var n = Math.max(q.matchingLeft.length, q.matchingRight.length);
        for (var k = 0; k < n; k++) (function (k) {
          var l = el('input', {type: 'text', value: q.matchingLeft[k] || '', placeholder: 'راست ' + fa(k + 1)}); l.addEventListener('input', function () { q.matchingLeft[k] = l.value; mark(); });
          var r = el('input', {type: 'text', value: q.matchingRight[k] || '', placeholder: 'چپ ' + fa(k + 1)}); r.addEventListener('input', function () { q.matchingRight[k] = r.value; mark(); });
          var s = el('select'); s.appendChild(el('option', {value: '', text: '— جفت —'}));
          q.matchingRight.forEach(function (_, j) { var o = el('option', {value: String(j), text: 'چپ ' + fa(j + 1)}); if (q.matchingPairs[k] === j) o.selected = true; s.appendChild(o); });
          s.addEventListener('change', function () { if (s.value === '') delete q.matchingPairs[k]; else q.matchingPairs[k] = Number(s.value); mark(); });
          box.appendChild(el('div', {class: 'b-opt'}, [l, s, r, el('button', {class: 'icon-btn danger', html: '✕', onclick: function () { if (n <= 2) return; q.matchingLeft.splice(k, 1); q.matchingRight.splice(k, 1); q.matchingPairs = {}; mark(); redraw(); }})]));
        })(k);
        box.appendChild(el('button', {class: 'btn light sm', text: '➕ ردیف', onclick: function () { q.matchingLeft.push(''); q.matchingRight.push(''); mark(); redraw(); }}));
      } else {
        box.appendChild(el('p', {class: 'muted', style: 'font-size:13px', text: 'سؤال تشریحی به‌صورت دستی تصحیح می‌شود.'}));
      }
      return box;
    }
    /* --- مشخصات آزمون (آنلاین) --- */
    /* V166 — فرم «مشخصات آزمون» مشترک: پنجرهٔ بازشو (گوشی/تبلت) و ستون راست سازندهٔ دسکتاپ */
    function settingsForm(m_) {
      m_.appendChild(el('div', {class: 'grid3'}, [
        inp('عنوان آزمون', state.title, function (v) { state.title = v; mark(); }),
        inp('درس', state.subject, function (v) { state.subject = v; mark(); }),
        inp('مدت (دقیقه)', state.duration, function (v) { state.duration = en(v); mark(); }, 'number')
      ]));
      m_.appendChild(el('div', {class: 'grid2'}, [
        inp('زمان شروع (اختیاری)', state.opensAt, function (v) { state.opensAt = v; mark(); }, 'datetime-local'),
        inp('زمان پایان (اختیاری)', state.closesAt, function (v) { state.closesAt = v; mark(); }, 'datetime-local'),
        inp('نمرهٔ منفی (۰ تا ۱)', state.negativeMarking, function (v) { state.negativeMarking = en(v); mark(); }, 'number'),
        sel('تعداد دفعات مجاز', String(state.attemptsAllowed), [['1', '۱'], ['2', '۲'], ['3', '۳'], ['4', '۴'], ['5', '۵']], function (v) { state.attemptsAllowed = Number(v); mark(); }),
        sel('نمرهٔ نهایی', state.gradePolicy, [['last', 'آخرین تلاش'], ['best', 'بهترین تلاش'], ['all', 'همهٔ تلاش‌ها']], function (v) { state.gradePolicy = v; mark(); }),
        inp('فاصلهٔ بین تلاش‌ها (دقیقه)', state.attemptCooldown, function (v) { state.attemptCooldown = en(v); mark(); }, 'number')
      ]));
      m_.appendChild(el('div', {class: 'row', style: 'margin-bottom:12px'}, [chk('به‌هم‌ریختن سؤال‌ها', state.shuffleQuestions, function (v) { state.shuffleQuestions = v; mark(); }), chk('به‌هم‌ریختن گزینه‌ها', state.shuffleOptions, function (v) { state.shuffleOptions = v; mark(); }), chk('ثبت خودکار با پایان زمان', state.attemptOnTimeout, function (v) { state.attemptOnTimeout = v; mark(); })]));
      var tm = el('textarea', {rows: 2, style: 'width:100%;border:1px solid var(--line);border-radius:10px;padding:8px'}); tm.value = state.teacherMessage; tm.addEventListener('input', function () { state.teacherMessage = tm.value; mark(); });
      m_.appendChild(el('div', {class: 'field'}, [el('label', {text: 'پیام معلم به دانش‌آموز (اختیاری)'}), tm]));
      /* مخاطبان */
      m_.appendChild(el('h3', {text: '👥 مخاطبان آزمون'}));
      var aud = el('div');
      function drawAud() {
        aud.innerHTML = '';
        aud.appendChild(el('div', {class: 'tabs'}, [['all', 'همهٔ دانش‌آموزان من'], ['classes', 'کلاس‌های خاص'], ['students', 'دانش‌آموزان خاص'], ['schools', 'مدرسه‌ها']].map(function (o) { return el('button', {class: state.audienceMode === o[0] ? 'on' : '', text: o[1], onclick: function () { state.audienceMode = o[0]; mark(); drawAud(); }}); })));
        var src = state.audienceMode === 'classes' ? state.availableClasses : state.audienceMode === 'students' ? state.availableStudents : state.audienceMode === 'schools' ? state.availableSchools : null;
        var sel_ = state.audienceMode === 'classes' ? state.audienceClasses : state.audienceMode === 'students' ? state.audienceStudents : state.audienceSchools;
        if (!src) return;
        if (!src.length) { aud.appendChild(el('div', {class: 'alert warn', text: 'موردی برای انتخاب وجود ندارد.'})); return; }
        var box = el('div', {class: 'b-aud'});
        src.forEach(function (o) { var c = el('input', {type: 'checkbox'}); c.checked = sel_.indexOf(o.id) >= 0; c.addEventListener('change', function () { var ix = sel_.indexOf(o.id); if (c.checked && ix < 0) sel_.push(o.id); if (!c.checked && ix >= 0) sel_.splice(ix, 1); mark(); }); box.appendChild(el('label', {class: 'row', style: 'gap:6px'}, [c, o.name + (o.classNames ? ' (' + o.classNames + ')' : '') + (o.city ? ' — ' + o.city : '')])); });
        aud.appendChild(box);
      }
      drawAud(); m_.appendChild(aud);
    }
    function openSettings() {
      var bg = el('div', {class: 'modal-bg'});
      var m = el('div', {class: 'modal wide'});
      m.appendChild(el('button', {class: 'x', text: '✕', onclick: function () { bg.remove(); syncMeta(); }}));
      m.appendChild(el('h2', {text: '⚙ مشخصات آزمون'}));
      settingsForm(m);
      m.appendChild(el('div', {class: 'row', style: 'margin-top:14px'}, [el('button', {class: 'btn', text: 'بستن', onclick: function () { bg.remove(); syncMeta(); }})]));
      bg.appendChild(m); document.body.appendChild(bg);
    }
    function syncMeta() { var meta = top.querySelector('.b-meta'); if (meta) meta.textContent = [state.title, state.subject, state.duration ? fa(state.duration) + ' دقیقه' : ''].filter(Boolean).join(' · ') || 'عنوان، درس و مدت را در «مشخصات آزمون» وارد کنید.'; }
    /* --- بانک سؤال --- */
    async function openBank() {
      var bg = el('div', {class: 'modal-bg'});
      var m = el('div', {class: 'modal wide'}, [el('button', {class: 'x', text: '✕', onclick: function () { bg.remove(); }}), el('h2', {text: '🏦 بانک سؤال'})]);
      var body = el('div'); S.loading(body); m.appendChild(body); bg.appendChild(m); document.body.appendChild(bg);
      try {
        var raw = await S.rpcObj('native_bank_snapshot_v1', {});
        var items = raw.items || [], cats = raw.categories || [];
        var q = el('input', {type: 'search', placeholder: 'جست‌وجو…', style: 'border:1px solid var(--line);border-radius:10px;padding:8px 12px;flex:1'});
        var cs = el('select', {style: 'border:1px solid var(--line);border-radius:10px;padding:8px'}); cs.appendChild(el('option', {value: '', text: 'همهٔ دسته‌ها'})); cats.forEach(function (c) { cs.appendChild(el('option', {value: String(c.id), text: c.name + ' (' + fa(c.count || 0) + ')'})); });
        var lst = el('div', {class: 'b-bank'});
        function draw() {
          var s = q.value.trim().toLowerCase(), cid = cs.value ? Number(cs.value) : null;
          lst.innerHTML = '';
          var f = items.filter(function (it) { var qq = it.question || {}; return (!s || String(qq.text || '').toLowerCase().indexOf(s) >= 0 || String(it.subject || '').toLowerCase().indexOf(s) >= 0) && (!cid || (it.cat_ids || []).indexOf(cid) >= 0); });
          if (!f.length) { lst.appendChild(S.emptyBox('🏦', 'سؤالی پیدا نشد.')); return; }
          f.forEach(function (it) { var qq = it.question || {}; lst.appendChild(el('div', {class: 'b-bank-item'}, [el('div', {class: 'grow'}, [el('div', {text: String(qq.text || '').slice(0, 120)}), el('div', {class: 'muted', style: 'font-size:12px', text: [TYPE_LABEL[S.qType(qq.type) === 'long' ? 'essay' : S.qType(qq.type)], it.subject, (it.cat_names || []).join('، ')].filter(Boolean).join(' · ')})]), el('button', {class: 'btn soft sm', text: 'افزودن', onclick: function () { var d = decodeQuestion(qq, qq); d.id = uuid(); state.questions.push(d); state.selected = state.questions.length - 1; mark(); drawList(); drawEditor(); toast('به آزمون افزوده شد.', 'ok'); }})])); });
        }
        q.addEventListener('input', draw); cs.addEventListener('change', draw);
        body.innerHTML = ''; body.appendChild(el('div', {class: 'row', style: 'margin-bottom:10px'}, [q, cs])); body.appendChild(lst); draw();
        var cur = state.questions[state.selected];
        if (cur) body.appendChild(el('div', {class: 'row', style: 'margin-top:12px'}, [el('button', {class: 'btn light sm', text: '⬆ ذخیرهٔ سؤال جاری در بانک', onclick: async function () { try { var enc = encodeQuestions([cur]); var comb = Object.assign({}, enc.publicQuestions[0], enc.answerKey[0]); delete comb.i; await S.rpcObj('native_bank_add_v2', {p_question: comb, p_subject: state.subject.trim(), p_cats: []}); toast('در بانک ذخیره شد.', 'ok'); } catch (e) { toast(errMsg(e), 'err'); } }})]));
      } catch (e) { S.showErr(body, e); }
    }
    /* --- پیش‌نمایش --- */
    function preview(printMode) {
      if (!state.questions.length) return toast('حداقل یک سؤال اضافه کنید.', 'err');
      var payload = S.buildPrintPayload(toServerExam(state));
      /* V156 — printMode: 'student' | 'teacher' (FAB چاپ در گوشی، مثل منوی «چاپ آزمون/چاپ با کلید» اپ) */
      S.openPrintPreview(payload, {title: state.title || 'آزمون', examId: state.mode === 'online' ? state.examId : (state.printId || 'local'), printMode: typeof printMode === 'string' ? printMode : '', onSnapshot: function (snap) { applySnapshot(snap); }});
    }
    window.__builderPreview = preview;
    function applySnapshot(snap) {
      /* بازگشت چیدمان/قالب‌بندی پیش‌نمایش به سؤال‌ها (مثل ExamBuilderViewModel.applyLayoutSnapshot) */
      try {
        var obj = typeof snap === 'string' ? JSON.parse(snap) : snap;
        Object.keys(obj || {}).forEach(function (id) {
          var q = state.questions[Number(id) - 1]; if (!q) return; var e = obj[id];
          q.spans = (e.spans || []).map(function (x) { var o = {s: x.start, e: x.end}; if (x.bold) o.b = true; if (x.italic) o.i = true; if (x.underline) o.u = true; if (x.color) o.c = x.color; if (x.size) o.z = x.size; if (x.font) o.f = x.font; return o; });
          q.alignSpans = (e.alignSpans || []).map(function (x) { return {s: x.start, e: x.end, a: x.align}; });
          q.raw = q.raw || {}; if (e.figLayouts && Object.keys(e.figLayouts).length) q.raw.figLayouts = e.figLayouts; if (e.sepExtraPx) q.raw.sepExtraPx = e.sepExtraPx;
        });
        mark();
      } catch (e) {}
    }
    /* --- ذخیره --- */
    async function save() {
      msg.innerHTML = '';
      try {
        if (state.bankEdit) {
          var bq = state.questions[state.selected] || state.questions[0];
          if (!bq || !bq.text.trim()) throw new Error('متن سؤال را وارد کنید.');
          var benc = encodeQuestions([bq]); var comb = Object.assign({}, benc.publicQuestions[0], benc.answerKey[0]); delete comb.i;
          var bcats = (state.bankEdit.cats || []).slice().sort(function (a, b) { return a - b; });
          var braw = state.bankEdit.id ? await S.rpcObj('native_bank_update_question_v1', {p_id: state.bankEdit.id, p_question: comb, p_subject: state.subject.trim(), p_cats: bcats}) : await S.rpcObj('native_bank_add_v2', {p_question: comb, p_subject: state.subject.trim(), p_cats: bcats});
          if (braw && braw.error) throw new Error(String(braw.error));
          state.dirty = false; saveDraft(); toast('در بانک ذخیره شد.', 'ok'); S.go('bank'); return;
        }
        if (state.mode !== 'print' && !state.title.trim()) throw new Error('عنوان آزمون را در «مشخصات آزمون» وارد کنید.');
        if (!state.questions.length) throw new Error('حداقل یک سؤال اضافه کنید.');
        if (state.questions.some(function (q) { return !q.text.trim(); })) throw new Error('متن همه سؤال‌ها را وارد کنید.');
        if (state.mode === 'print') {
          /* V159 — مثل پنجرهٔ «ذخیره آزمون چاپی» اپ (askPrintName): نام آزمون پرسیده می‌شود */
          var suggested = (state.title || '').trim() || (state.subject || '').trim() || 'آزمون چاپی';
          var name = await S.promptDlg('ذخیره آزمون چاپی', 'این آزمون روی سرور ذخیره می‌شود و در بخش چاپ آزمونِ اپ و سایت دیده خواهد شد. فقط تصاویر جدید هزینه دارند (هر تصویر ۱۰۰۰ تومان).', 'نام آزمون', suggested, 'ذخیره');
          if (name == null || !name.trim()) return;
          state.title = name.trim();
          var id = state.printId || uuid(); state.printId = id;
          var pr = await printExamSave({id: id, title: state.title, subject: (state.subject || '').trim(), duration: state.duration, questions: state.questions, sourceExamId: state.sourceExamId || null});
          if (!pr) return;
          state.dirty = false; saveDraft(); toast('آزمون «' + state.title + '» ذخیره شد ✓' + (pr.cost ? ' · هزینهٔ تصاویر: ' + S.money(pr.cost) : ''), 'ok'); return;
        }
        if (state.audienceMode === 'classes' && !state.audienceClasses.length) throw new Error('حداقل یک کلاس انتخاب کنید.');
        if (state.audienceMode === 'students' && !state.audienceStudents.length) throw new Error('حداقل یک دانش‌آموز انتخاب کنید.');
        if (state.audienceMode === 'schools' && !state.audienceSchools.length) throw new Error('حداقل یک مدرسه انتخاب کنید.');
        var opens = localToIso(state.opensAt), closes = localToIso(state.closesAt);
        if (opens && closes && new Date(closes) < new Date(opens)) throw new Error('زمان پایان نمی‌تواند قبل از زمان شروع باشد.');
        var examId = state.examId || uuid();
        /* V145 — هیچ رسانهٔ data: نباید در آزمون آنلاین ذخیره شود (برنامهٔ اندروید فقط آدرس https را باز می‌کند):
           تصاویر سؤال/گزینه/جورکردنی و صوت (اگر از حالت چاپی آمده) همه آپلود می‌شوند */
        var dataBlob = async function (u) { return await (await fetch(u)).blob(); };
        for (var i = 0; i < state.questions.length; i++) {
          var q = state.questions[i];
          for (var k = 0; k < q.images.length; k++) if (/^data:/.test(q.images[k].uri)) q.images[k].uri = await uploadImage(await dataBlob(q.images[k].uri), 'questions', examId);
          for (var o = 0; o < q.optionImages.length; o++) if (q.optionImages[o] && /^data:/.test(q.optionImages[o])) q.optionImages[o] = await uploadImage(await dataBlob(q.optionImages[o]), 'option_images', examId);
          var ml = q.matchingLeftImages || [], mr = q.matchingRightImages || [];
          for (var a = 0; a < ml.length; a++) if (ml[a] && /^data:/.test(ml[a])) ml[a] = await uploadImage(await dataBlob(ml[a]), 'matching_images', examId);
          for (var b = 0; b < mr.length; b++) if (mr[b] && /^data:/.test(mr[b])) mr[b] = await uploadImage(await dataBlob(mr[b]), 'matching_images', examId);
          if (q.audio && /^data:/.test(q.audio) && S.uploadAudioBlob) { var ab = await dataBlob(q.audio); q.audio = await S.uploadAudioBlob(ab, examId); q.audioBytes = ab.size; }
        }
        var enc = encodeQuestions(state.questions);
        var code = state.code || genCode();
        var payload = {operation_id: uuid(), id: examId, code: code, title: state.title.trim(), subject: state.subject.trim(), duration: Math.max(0, Math.min(1440, parseInt(state.duration, 10) || 0)), opens_at: opens, closes_at: closes,
          total_score: state.questions.reduce(function (s, q) { return s + (Number(q.score) || 0); }, 0), shuffle_q: !!state.shuffleQuestions, shuffle_opt: !!state.shuffleOptions, neg_marking: parseFloat(state.negativeMarking) || 0,
          teacher_message: state.teacherMessage.trim() || null, attempts_allowed: Math.max(1, Math.min(5, state.attemptsAllowed)), attempt_on_timeout: !!state.attemptOnTimeout, grade_policy: state.gradePolicy, attempt_cooldown: Math.max(0, Math.min(1440, parseInt(state.attemptCooldown, 10) || 0)),
          questions: enc.publicQuestions, answer_key: enc.answerKey, audience: state.audienceMode, classes: state.audienceClasses.slice().sort(), students: state.audienceStudents.slice().sort(), schools: state.audienceSchools.slice().sort()};
        var n = state.questions.length;
        if (!(await S.confirmDlg('ذخیرهٔ آزمون', 'آزمون «' + esc(state.title) + '» با ' + fa(n) + ' سؤال ذخیره می‌شود. هزینهٔ سؤال‌های جدید (' + fa('1,000') + ' تومان/سؤال) و رسانه‌ها از کیف پول کسر می‌شود.', 'ذخیره'))) return;
        var raw = await S.rpc('native_save_exam_v2', {p_payload: payload});
        if (raw && raw.error) { var m = String(raw.error); if (raw.balance != null && raw.required != null) m += '؛ موجودی ' + fa(raw.balance) + ' تومان و مبلغ لازم ' + fa(raw.required) + ' تومان است.'; throw new Error(m); }
        state.examId = examId; state.code = (raw && raw.code) || code; state.dirty = false; saveDraft();
        msg.appendChild(el('div', {class: 'alert ok', html: '✅ ذخیره شد. کد آزمون: <b class="code">' + esc(state.code) + '</b>' + (raw && raw.cost ? ' · هزینه: ' + S.money(raw.cost) : '') + (raw && raw.balance != null ? ' · موجودی: ' + S.money(raw.balance) : '')}));
        toast('آزمون ذخیره شد.', 'ok');
      } catch (e) { msg.appendChild(el('div', {class: 'alert error', text: errMsg(e)})); }
    }
    drawList(); drawEditor();
    return wrap;
  }
  function genCode() { var ch = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789', s = ''; for (var i = 0; i < 6; i++) s += ch[Math.floor(Math.random() * ch.length)]; return s; }

  /* ================================================================ درج شکل با ویرایشگرهای وب (داخل iframe موتور چاپ) */
  var figFrame = null;
  function insertFigure(kind, ta, q) {
    var overlay = el('div', {class: 'engine-bg'});
    var bar = el('div', {class: 'engine-bar'}, [el('span', {text: '📐 درج شکل در سؤال'}), el('span', {class: 'grow'}), el('button', {class: 'btn light sm', text: '✕ بستن', onclick: close})]);
    var iframe = el('iframe', {class: 'with-bar'});
    overlay.appendChild(bar); overlay.appendChild(iframe); document.body.appendChild(overlay);
    document.body.style.overflow = 'hidden';
    function close() { overlay.remove(); document.body.style.overflow = ''; }
    iframe.addEventListener('load', function () {
      var w = iframe.contentWindow, d = iframe.contentDocument, tries = 0;
      (function go() {
        tries++;
        var api = {geo: w.GeoFig, graph: w.GraphFig, table: w.TableFig, periodic: w.PeriodicFig, anatomy: w.AnatomyFig, science: w.ScienceFig}[kind];
        if (!api || typeof api.open !== 'function' || !w.renderPreview) { if (tries < 100) return setTimeout(go, 60); toast('ویرایشگر شکل آماده نشد.', 'err'); return close(); }
        /* textarea هدفِ ویرایشگرهای وب: یک textarea مخفی که متنِ سؤال ما را دارد */
        var hidden = d.createElement('textarea'); hidden.id = 'qTxt_main'; hidden.style.cssText = 'position:fixed;opacity:0;pointer-events:none;width:1px;height:1px';
        hidden.value = ta.value; d.body.appendChild(hidden);
        try { hidden.setSelectionRange(ta.selectionStart, ta.selectionEnd); } catch (e) {}
        w.__qmfActiveField = hidden;
        hidden.addEventListener('input', function () { ta.value = hidden.value; q.text = hidden.value; ta.dispatchEvent(new Event('input')); setTimeout(close, 150); });
        /* استایل: روکش‌ها باید روی زمینهٔ خالی دیده شوند */
        var st = d.createElement('style'); st.textContent = 'body{background:#eef1f6}#printContent,#previewArea{display:none}'; d.head.appendChild(st);
        api.open(null, null);
        /* بستن روکش وب بدون درج → بستن ما */
        var ovId = {geo: 'gfOverlay', graph: 'grOverlay', table: 'tbOverlay'}[kind];
        var poll = setInterval(function () { if (!d.body.contains(overlay) && false) return; var anyOpen = Array.prototype.some.call(d.querySelectorAll('[id$="Overlay"], .gf-overlay'), function (o) { return o.classList.contains('open'); }); if (!anyOpen && tries > 1) { clearInterval(poll); if (document.body.contains(overlay)) close(); } tries++; }, 300);
      })();
    });
    S.engineHtml('print').then(function (h) { if (h) iframe.srcdoc = h; else close(); });
  }

  /* ================================================================ گفتار به متن (Web Speech، فارسی/انگلیسی) */
  function dictate(ta, q) {
    var SR = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SR) return toast('مرورگر شما تبدیل گفتار به متن را پشتیبانی نمی‌کند (Chrome/Edge).', 'err');
    var bg = el('div', {class: 'modal-bg'});
    var lang = 'fa-IR', rec = null, live = el('div', {class: 'alert info', text: '…'}), acc = '';
    var m = el('div', {class: 'modal'}, [el('h2', {text: '🎤 گفتار به متن'}), el('div', {class: 'tabs'}, [el('button', {class: 'on', text: 'فارسی', onclick: function (e) { lang = 'fa-IR'; setTab(e.target); restart(); }}), el('button', {text: 'English', onclick: function (e) { lang = 'en-US'; setTab(e.target); restart(); }})]), live,
      el('div', {class: 'row'}, [el('button', {class: 'btn', text: 'درج و بستن', onclick: function () { stop(); var t = (acc).trim(); if (t) { var s = ta.selectionStart, e2 = ta.selectionEnd; ta.value = ta.value.slice(0, s) + t + ta.value.slice(e2); q.text = ta.value; ta.dispatchEvent(new Event('input')); } bg.remove(); }}), el('button', {class: 'btn light', text: 'انصراف', onclick: function () { stop(); bg.remove(); }})])]);
    function setTab(b) { Array.prototype.forEach.call(m.querySelectorAll('.tabs button'), function (x) { x.classList.toggle('on', x === b); }); }
    function stop() { try { if (rec) { rec.onend = null; rec.stop(); } } catch (e) {} }
    function restart() { stop(); start(); }
    function start() {
      rec = new SR(); rec.lang = lang; rec.continuous = true; rec.interimResults = true;
      rec.onresult = function (ev) { var interim = ''; for (var i = ev.resultIndex; i < ev.results.length; i++) { if (ev.results[i].isFinal) acc += ev.results[i][0].transcript + ' '; else interim += ev.results[i][0].transcript; } live.textContent = acc + interim || '…'; };
      rec.onerror = function (ev) { if (ev.error !== 'no-speech' && ev.error !== 'aborted') live.textContent = 'خطا: ' + ev.error; };
      rec.onend = function () { try { rec.start(); } catch (e) {} }; /* V109 — پنجره هرگز خودش بسته نمی‌شود */
      try { rec.start(); } catch (e) {}
    }
    bg.appendChild(m); document.body.appendChild(bg); start();
  }

  /* ================================================================ ابزارهای فرم */
  function inp(label, val, on, type) { var i = el('input', {type: type || 'text', value: val == null ? '' : val}); i.addEventListener('input', function () { on(i.value); }); return el('div', {class: 'field'}, [el('label', {text: label}), i]); }
  function sel(label, val, opts, on) { var s = el('select'); opts.forEach(function (o) { var op = el('option', {value: o[0], text: o[1]}); if (o[0] === val) op.selected = true; s.appendChild(op); }); s.addEventListener('change', function () { on(s.value); }); return el('div', {class: 'field'}, [el('label', {text: label}), s]); }
  function chk(label, val, on) { var c = el('input', {type: 'checkbox'}); c.checked = !!val; c.addEventListener('change', function () { on(c.checked); }); return el('label', {class: 'row', style: 'gap:6px;font-size:14px'}, [c, label]); }

  window.SiteBuilder = {page: page, printExamsSection: printExamsSection, printExamsList: printExamsList, printExamGet: printExamGet, printExamSave: printExamSave, printExamDelete: printExamDelete, draftsFromCombined: draftsFromCombined, migrateLocalPrintExams: migrateLocalPrintExams, encodeQuestions: encodeQuestions, decodeQuestion: decodeQuestion, newQuestion: newQuestion, toServerExam: toServerExam};
})();
