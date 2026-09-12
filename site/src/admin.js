/* V141 — فاز ۴ سایت: تصحیح و بازخورد، حضور/وضعیت زنده، گزارش نظارتی، تحلیل سؤال، شارژ کیف پول،
   پنل مدیر (معلم‌ها، دعوت، انتقال اعتبار، کلاس‌های معلم، دانش‌آموزان مدرسه)، تقویم/پیام‌ها (شمسی).
   قراردادها عیناً از SupabaseGradingRepository / SupabaseBillingRepository / SupabaseManagerRepository / SupabaseCalendarRepository. */
(function () {
  'use strict';
  var S = window.ExamSite;
  var el = S.el, esc = S.esc, fa = S.fa, en = S.en, toast = S.toast, errMsg = S.errMsg, uuid = S.uuid;
  var TYPE_LABEL = {multiple: 'چندگزینه‌ای', truefalse: 'صحیح/غلط', fill: 'جای‌خالی', numeric: 'عددی', matching: 'جورکردنی', essay: 'تشریحی', long: 'تشریحی'};
  function qt(t) { var x = S.qType(t); return x === 'long' ? 'essay' : x; }
  function num(v, d) { var n = Number(v); return isFinite(n) ? n : (d || 0); }
  function chk(raw) { if (raw && raw.error) throw new Error(String(raw.error)); return raw; }

  /* ================================================================ تصحیح */
  async function gradingPage(c, arg) {
    arg = arg || {};
    if (arg.examId) return gradingExam(c, arg.examId, arg.tab || 'grading');
    S.loading(c);
    try {
      var exams = await S.select('exams', 'select=id,title,subject,total_score,created_at,is_open&teacher_id=eq.' + encodeURIComponent(S.user().id) + '&order=created_at.desc');
      var counts = {};
      try { var ans = await S.select('answers', 'select=exam_id,graded'); (ans || []).forEach(function (a) { var k = counts[a.exam_id] || (counts[a.exam_id] = {n: 0, g: 0}); k.n++; if (a.graded) k.g++; }); } catch (e) {}
      c.innerHTML = '';
      /* V148 — فیلتر کارت‌های «مانده» (فقط آزمون‌های دارای پاسخ در انتظار) و «پاسخ» (فقط تصحیح‌شده‌ها)، مثل اپ */
      var filt = arg.filter || '';
      if (filt === 'pending') exams = exams.filter(function (x) { var k = counts[x.id]; return k && k.n > k.g; });
      if (filt === 'graded') exams = exams.filter(function (x) { var k = counts[x.id]; return k && k.g > 0; });
      var card = el('div', {class: 'card'}, [el('h3', {text: filt === 'pending' ? '⏳ مانده (در انتظار تصحیح)' : filt === 'graded' ? '✅ پاسخ‌های تصحیح‌شده' : '✅ تصحیح و بازخورد'})]);
      if (!exams.length) { card.appendChild(S.emptyBox('📝', filt === 'pending' ? 'پاسخی در انتظار تصحیح نیست.' : filt === 'graded' ? 'هنوز پاسخی تصحیح نشده است.' : 'هنوز آزمونی نساخته‌اید.')); c.appendChild(card); return; }
      card.appendChild(el('table', {class: 'tbl'}, [
        el('thead', {}, [el('tr', {}, ['آزمون', 'درس', 'بارم', 'پاسخ‌ها', 'تصحیح‌شده', ''].map(function (h) { return el('th', {text: h}); }))]),
        el('tbody', {}, exams.map(function (x) { var k = counts[x.id] || {n: 0, g: 0}; var pend = k.n - k.g; return el('tr', {}, [el('td', {html: '<b>' + esc(x.title) + '</b>'}), el('td', {text: x.subject || '—'}), el('td', {text: fa(S.fmtScore(x.total_score))}), el('td', {text: fa(k.n)}),
          el('td', {}, [el('span', {class: 'chip ' + (pend ? 'warn' : 'ok'), text: fa(k.g) + ' / ' + fa(k.n) + (pend ? ' (' + fa(pend) + ' در انتظار)' : '')})]),
          el('td', {}, [el('div', {class: 'acts'}, [el('button', {class: 'btn sm', text: 'تصحیح', onclick: function () { S.go('grading', {examId: x.id}); }}), el('button', {class: 'btn light sm', text: 'حضور', onclick: function () { S.go('grading', {examId: x.id, tab: 'attendance'}); }})])])]); }))
      ]));
      c.appendChild(card);
    } catch (e) { S.showErr(c, e); }
  }
  async function gradingExam(c, examId, tab) {
    S.loading(c);
    try {
      var r = await Promise.all([S.api.examDetail(examId), S.select('answers', 'select=*&exam_id=eq.' + encodeURIComponent(examId) + '&order=submitted_at.desc')]);
      var exam = r[0], answers = r[1] || [];
      var keys = {}; (Array.isArray(exam.__answers) ? exam.__answers : []).forEach(function (k, i) { if (k && typeof k === 'object') keys[k.i != null ? k.i : i] = k; });
      var questions = (Array.isArray(exam.questions) ? exam.questions : []).map(function (q, i) { return Object.assign({}, q, {__key: keys[i] || {}, __type: qt(q.type)}); });
      c.innerHTML = '';
      var head = el('div', {class: 'row', style: 'margin-bottom:12px'}, [el('button', {class: 'btn light sm', text: '→ همهٔ آزمون‌ها', onclick: function () { S.go('grading'); }}), el('h2', {class: 'grow', style: 'margin:0;font-size:18px', text: exam.title}), el('span', {class: 'chip', text: 'بارم ' + fa(S.fmtScore(exam.total_score))}), el('span', {class: 'chip', text: fa(answers.length) + ' پاسخ'})]);
      c.appendChild(head);
      var tabs = el('div', {class: 'tabs'}, [['grading', '✅ تصحیح'], ['question', '📋 تصحیح سؤال‌به‌سؤال'], ['attendance', '👥 حضور و وضعیت زنده'], ['monitor', '🛡 گزارش نظارتی'], ['analysis', '📈 تحلیل سؤال‌ها'], ['feedback', '💬 بانک بازخورد']].map(function (t) { return el('button', {class: tab === t[0] ? 'on' : '', text: t[1], onclick: function () { S.go('grading', {examId: examId, tab: t[0]}); }}); }));
      c.appendChild(tabs);
      var body = el('div'); c.appendChild(body);
      var ctx = {exam: exam, questions: questions, answers: answers, examId: examId, refresh: function () { gradingExam(c, examId, tab); }};
      if (tab === 'grading') tabGrading(body, ctx);
      else if (tab === 'question') tabByQuestion(body, ctx);
      else if (tab === 'attendance') tabAttendance(body, ctx);
      else if (tab === 'monitor') tabMonitor(body, ctx);
      else if (tab === 'analysis') tabAnalysis(body, ctx);
      else tabFeedbackBank(body, ctx);
    } catch (e) { S.showErr(c, e); }
  }
  function correctText(q) {
    var k = q.__key || {};
    switch (q.__type) {
      case 'multiple': var ci = k.correctOption != null ? k.correctOption : q.correctIndex; return ci != null && (q.options || [])[ci] != null ? (['الف', 'ب', 'ج', 'د', 'ه', 'و', 'ز', 'ح'][ci] || fa(ci + 1)) + ') ' + q.options[ci] : '—';
      case 'truefalse': return k.correctAnswer === true || k.correctAnswer === 'true' ? 'صحیح' : 'غلط';
      case 'fill': return (k.accept || []).join(' یا ') || '—';
      case 'numeric': return k.answer != null ? String(k.answer) + (k.tolerance && Number(k.tolerance) ? ' ± ' + k.tolerance : '') : '—';
      case 'matching': var m = k.matchAnswer || {}; return Object.keys(m).sort(function (a, b) { return a - b; }).map(function (l) { return fa(Number(l) + 1) + ' ← ' + fa(Number(m[l]) + 1); }).join('، ') || '—';
      default: return null;
    }
  }
  function responseText(q, r) {
    if (r == null || r === '') return null;
    switch (q.__type) {
      case 'multiple': var i = Number(r); return (q.options || [])[i] != null ? (['الف', 'ب', 'ج', 'د', 'ه', 'و', 'ز', 'ح'][i] || fa(i + 1)) + ') ' + q.options[i] : String(r);
      case 'truefalse': return r === true || r === 'true' ? 'صحیح' : 'غلط';
      case 'matching': if (typeof r !== 'object') return String(r); return Object.keys(r).sort(function (a, b) { return a - b; }).map(function (l) { return fa(Number(l) + 1) + ' ← ' + fa(Number(r[l]) + 1); }).join('، ');
      default: return String(r);
    }
  }
  /* پیشنهاد نمرهٔ خودکار (فقط پیشنهاد؛ نمرهٔ نهایی را معلم ذخیره می‌کند — تأیید گروهی با approve_auto_grades سرور) */
  function autoScore(q, r) {
    var k = q.__key || {}, s = num(q.score);
    if (r == null || r === '') return q.__type === 'essay' ? null : 0;
    switch (q.__type) {
      case 'multiple': var ci = k.correctOption != null ? k.correctOption : q.correctIndex; return ci != null ? (Number(r) === Number(ci) ? s : 0) : null;
      case 'truefalse': return (r === true || r === 'true') === (k.correctAnswer === true || k.correctAnswer === 'true') ? s : 0;
      case 'fill': var acc = (k.accept || []).map(function (x) { return k.caseSensitive ? String(x).trim() : String(x).trim().toLowerCase(); }); var v = k.caseSensitive ? String(r).trim() : String(r).trim().toLowerCase(); return acc.length ? (acc.indexOf(v) >= 0 ? s : 0) : null;
      case 'numeric': var a = parseFloat(k.answer), t = parseFloat(k.tolerance) || 0, x = parseFloat(en(String(r))); return isFinite(a) ? (isFinite(x) && Math.abs(x - a) <= t ? s : 0) : null;
      case 'matching': var m = k.matchAnswer || {}, ks = Object.keys(m); if (!ks.length || typeof r !== 'object') return null; var ok = 0; ks.forEach(function (l) { if (Number(r[l]) === Number(m[l])) ok++; }); return Math.round(s * ok / ks.length * 100) / 100;
      default: return null;
    }
  }
  function tabGrading(body, ctx) {
    var answers = ctx.answers, questions = ctx.questions;
    if (!answers.length) return body.appendChild(S.emptyBox('🗂', 'هنوز پاسخی برای این آزمون ارسال نشده است.'));
    var pend = answers.filter(function (a) { return !a.graded; }).length;
    body.appendChild(el('div', {class: 'row', style: 'margin-bottom:10px'}, [
      el('span', {class: 'muted', text: fa(pend) + ' پاسخ در انتظار تصحیح'}), el('span', {class: 'grow'}),
      el('button', {class: 'btn soft sm', text: '⚡ تأیید نمره‌های خودکار (سؤال‌های عینی)', onclick: async function () { try { chk(await S.rpcObj('exam_autograde_info', {p_exam: ctx.examId})); if (!(await S.confirmDlg('تأیید نمره‌های خودکار', 'پاسخ‌هایی که همهٔ سؤال‌هایشان عینی است، به‌صورت خودکار نمره‌گذاری و تأیید می‌شوند. ادامه؟', 'تأیید'))) return; chk(await S.rpcObj('approve_auto_grades', {p_exam: ctx.examId, p_mode: 'auto_only'})); toast('نمره‌های خودکار تأیید شدند.', 'ok'); ctx.refresh(); } catch (e) { toast(errMsg(e), 'err'); } }})
    ]));
    var list = el('div', {class: 'g-list'}), detail = el('div', {class: 'g-detail card'});
    var sel = null;
    function drawList() {
      list.innerHTML = '';
      answers.forEach(function (a) { list.appendChild(el('div', {class: 'g-item' + (sel === a ? ' on' : ''), onclick: function () { sel = a; drawList(); drawDetail(); }}, [el('div', {class: 'grow'}, [el('b', {text: a.student_name || 'دانش‌آموز'}), el('div', {class: 'muted', style: 'font-size:12px', text: S.fmtDate(a.submitted_at) + (a.attempt_no > 1 ? ' · تلاش ' + fa(a.attempt_no) : '')})]), el('span', {class: 'chip ' + (a.graded ? 'ok' : 'warn'), text: a.graded ? fa(S.fmtScore(a.total_grade)) + ' / ' + fa(S.fmtScore(ctx.exam.total_score)) : 'در انتظار'})])); });
    }
    async function drawDetail() {
      detail.innerHTML = '';
      var a = sel; if (!a) return detail.appendChild(el('div', {class: 'empty', text: 'یک پاسخ را از فهرست انتخاب کنید.'}));
      var responses = Array.isArray(a.responses) ? a.responses : [], grades = Array.isArray(a.grades) ? a.grades.slice() : [], imgs = a.response_images || {};
      var inputs = [];
      detail.appendChild(el('div', {class: 'row', style: 'margin-bottom:8px'}, [el('h3', {class: 'grow', style: 'margin:0', text: a.student_name || 'دانش‌آموز'}), a.graded ? el('button', {class: 'btn light sm', text: 'برداشتن تأیید', onclick: async function () { try { chk(await S.rpcObj('unapprove_grade', {p_answer: a.id})); toast('تأیید برداشته شد.', 'ok'); ctx.refresh(); } catch (e) { toast(errMsg(e), 'err'); } }}) : null]));
      questions.forEach(function (q, i) {
        var r = responses[i], auto = autoScore(q, r), cur = grades[i] != null ? grades[i] : (auto != null ? auto : '');
        var inp = el('input', {type: 'number', step: '0.25', min: 0, max: q.score, value: cur, style: 'width:90px;border:1px solid var(--line);border-radius:8px;padding:6px;direction:ltr'});
        inputs.push(inp);
        var rt = responseText(q, r), ct = correctText(q), qi = imgs[q.id] || imgs[String(i)] || [];
        var box = el('div', {class: 'g-q'}, [
          el('div', {class: 'row'}, [el('b', {text: 'سؤال ' + fa(i + 1)}), el('span', {class: 'chip', text: TYPE_LABEL[q.__type]}), el('span', {class: 'grow'}), el('span', {class: 'muted', style: 'font-size:12px', text: 'از ' + fa(S.fmtScore(q.score))}), inp,
            auto != null ? el('button', {class: 'btn light sm', title: 'نمرهٔ پیشنهادی', text: '⚡ ' + fa(S.fmtScore(auto)), onclick: function () { inp.value = auto; }}) : null, el('button', {class: 'btn light sm', text: 'کامل', onclick: function () { inp.value = q.score; }}), el('button', {class: 'btn light sm', text: '۰', onclick: function () { inp.value = 0; }})]),
          el('div', {class: 'g-qtext', text: (q.text || '').replace(/%%FIG:[\s\S]*?%%/g, '[شکل]')}),
          el('div', {class: 'g-resp'}, [el('span', {class: 'muted', text: 'پاسخ دانش‌آموز: '}), rt != null ? el('span', {class: auto == null ? '' : (auto >= num(q.score) ? 'ok-t' : (auto > 0 ? 'warn-t' : 'err-t')), text: rt}) : el('i', {class: 'muted', text: 'بدون پاسخ'})]),
          ct ? el('div', {class: 'g-resp muted', style: 'font-size:13px', text: 'پاسخ درست: ' + ct}) : null,
          qi.length ? el('div', {class: 'st-imgs'}, qi.map(function (u) { return el('img', {src: u, onclick: function () { lightbox(u); }}); })) : null
        ]);
        detail.appendChild(box);
      });
      var fb = el('textarea', {rows: 2, style: 'width:100%;border:1px solid var(--line);border-radius:10px;padding:8px', placeholder: 'بازخورد به دانش‌آموز (اختیاری)'}); fb.value = a.feedback || '';
      var fbBank = el('div', {class: 'row', style: 'flex-wrap:wrap;gap:4px;margin:6px 0'});
      S.rpc('fb_list', {}).then(function (list) { (list || []).slice(0, 12).forEach(function (p) { fbBank.appendChild(el('button', {class: 'chip', style: 'cursor:pointer', text: p.text, onclick: function () { fb.value = (fb.value ? fb.value + ' ' : '') + p.text; }})); }); }).catch(function () {});
      var total = el('b');
      function sum() { var s = 0; inputs.forEach(function (x) { s += num(x.value); }); total.textContent = 'جمع: ' + fa(S.fmtScore(s)) + ' از ' + fa(S.fmtScore(ctx.exam.total_score)); }
      inputs.forEach(function (x) { x.addEventListener('input', sum); }); sum();
      detail.appendChild(el('div', {class: 'field', style: 'margin-top:12px'}, [el('label', {text: 'بازخورد'}), fb, fbBank]));
      detail.appendChild(el('div', {class: 'row'}, [total, el('span', {class: 'grow'}), el('button', {class: 'btn', text: '💾 ذخیرهٔ نمره', onclick: async function () {
        var g = inputs.map(function (x, i) { var v = num(x.value); if (v < 0 || v > num(questions[i].score)) throw new Error('نمرهٔ سؤال ' + fa(i + 1) + ' خارج از بازه است.'); return v; });
        try { chk(await S.rpcObj('native_save_grade', {p_answer: a.id, p_grades: g, p_feedback: fb.value.trim()})); toast('نمره ذخیره شد.', 'ok'); var idx = answers.indexOf(a); a.graded = true; a.grades = g; a.total_grade = g.reduce(function (s, v) { return s + v; }, 0); a.feedback = fb.value.trim(); if (idx < answers.length - 1) { sel = answers[idx + 1]; } drawList(); drawDetail(); } catch (e) { toast(errMsg(e), 'err'); }
      }})]));
    }
    body.appendChild(el('div', {class: 'g-wrap'}, [list, detail]));
    sel = answers.filter(function (a) { return !a.graded; })[0] || answers[0]; drawList(); drawDetail();
  }
  function tabByQuestion(body, ctx) {
    var answers = ctx.answers, questions = ctx.questions;
    if (!answers.length) return body.appendChild(S.emptyBox('🗂', 'پاسخی وجود ندارد.'));
    var qi = 0, holder = el('div');
    var selQ = el('select', {style: 'border:1px solid var(--line);border-radius:10px;padding:8px;max-width:100%'});
    questions.forEach(function (q, i) { selQ.appendChild(el('option', {value: String(i), text: 'سؤال ' + fa(i + 1) + ' — ' + TYPE_LABEL[q.__type] + ' — ' + (q.text || '').replace(/%%FIG:[\s\S]*?%%/g, '[شکل]').slice(0, 60)})); });
    selQ.addEventListener('change', function () { qi = Number(selQ.value); draw(); });
    body.appendChild(el('div', {class: 'row', style: 'margin-bottom:10px'}, [selQ, el('span', {class: 'grow'}), el('button', {class: 'btn soft sm', text: '🏁 نهایی‌سازی نمره‌های گروهی', onclick: async function () { if (!(await S.confirmDlg('نهایی‌سازی', 'نمره‌های ذخیره‌شدهٔ سؤال‌به‌سؤال برای همهٔ پاسخ‌ها جمع و تأیید می‌شود. ادامه؟', 'نهایی‌سازی'))) return; try { chk(await S.rpcObj('native_finalize_bulk_grades_v1', {p_exam: ctx.examId})); toast('نمره‌ها نهایی شدند.', 'ok'); ctx.refresh(); } catch (e) { toast(errMsg(e), 'err'); } }})]));
    body.appendChild(holder);
    function draw() {
      holder.innerHTML = ''; var q = questions[qi]; var inputs = {};
      holder.appendChild(el('div', {class: 'card', style: 'margin-bottom:10px'}, [el('div', {class: 'g-qtext', text: (q.text || '').replace(/%%FIG:[\s\S]*?%%/g, '[شکل]')}), correctText(q) ? el('div', {class: 'muted', style: 'font-size:13px', text: 'پاسخ درست: ' + correctText(q)}) : null]));
      var tbl = el('table', {class: 'tbl'}, [el('thead', {}, [el('tr', {}, ['دانش‌آموز', 'پاسخ', 'تصویر', 'نمره (از ' + fa(S.fmtScore(q.score)) + ')'].map(function (h) { return el('th', {text: h}); }))]),
        el('tbody', {}, answers.map(function (a) { var r = (a.responses || [])[qi], auto = autoScore(q, r), cur = (a.grades || [])[qi]; var inp = el('input', {type: 'number', step: '0.25', min: 0, max: q.score, value: cur != null ? cur : (auto != null ? auto : ''), style: 'width:80px;border:1px solid var(--line);border-radius:8px;padding:5px;direction:ltr'}); inputs[a.id] = inp; var im = (a.response_images || {})[q.id] || (a.response_images || {})[String(qi)] || [];
          return el('tr', {}, [el('td', {text: a.student_name || '—'}), el('td', {text: responseText(q, r) || '—'}), el('td', {}, im.map(function (u) { return el('img', {src: u, style: 'width:48px;height:40px;object-fit:cover;border-radius:6px;cursor:zoom-in;margin-inline-end:4px', onclick: function () { lightbox(u); }}); })), el('td', {}, [inp])]); }))]);
      holder.appendChild(el('div', {class: 'card'}, [tbl, el('div', {class: 'row', style: 'margin-top:10px'}, [el('span', {class: 'grow'}), el('button', {class: 'btn', text: '💾 ذخیرهٔ نمره‌های این سؤال', onclick: async function () { var items = []; Object.keys(inputs).forEach(function (id) { var v = inputs[id].value; if (v !== '') items.push({answer_id: id, score: num(v)}); }); if (!items.length) return toast('حداقل یک نمره وارد کنید.', 'err'); try { chk(await S.rpcObj('native_bulk_save_question_grades_v1', {p_exam: ctx.examId, p_question_index: qi, p_items: items})); toast('ذخیره شد.', 'ok'); } catch (e) { toast(errMsg(e), 'err'); } }})])]));
    }
    draw();
  }
  async function tabAttendance(body, ctx) {
    S.loading(body);
    var timer;
    async function draw() {
      try {
        var r = await Promise.all([S.rpc('exam_attendance', {p_exam: ctx.examId}), S.rpcObj('exam_live_status', {p_exam: ctx.examId}).catch(function () { return {}; })]);
        var rows = r[0] || [], live = r[1] || {}, sm = live.summary || {};
        body.innerHTML = '';
        body.appendChild(el('div', {class: 'grid4', style: 'margin-bottom:12px'}, [stat(fa(rows.length), 'مخاطب'), stat(fa(sm.submitted != null ? sm.submitted : rows.filter(function (x) { return x.status === 'submitted'; }).length), 'ارسال‌کرده'), stat(fa(sm.in_progress != null ? sm.in_progress : rows.filter(function (x) { return x.status === 'in_progress'; }).length), 'در حال آزمون'), stat(fa(sm.not_started != null ? sm.not_started : rows.filter(function (x) { return x.status === 'absent' || x.status === 'not_started'; }).length), 'شروع‌نکرده')]));
        var ST = {submitted: ['ارسال‌شده', 'ok'], in_progress: ['در حال آزمون', 'brand'], absent: ['غایب', ''], not_started: ['شروع‌نکرده', ''], expired: ['مهلت تمام', 'warn']};
        body.appendChild(el('div', {class: 'card'}, [el('div', {class: 'row'}, [el('h3', {class: 'grow', text: '👥 حضور و وضعیت زنده'}), el('span', {class: 'muted', style: 'font-size:12px', text: 'به‌روزرسانی هر ۳۰ ثانیه'})]),
          rows.length ? el('table', {class: 'tbl'}, [el('thead', {}, [el('tr', {}, ['دانش‌آموز', 'نام کاربری', 'وضعیت', 'نمره', 'ارسال', 'زمان باقی', 'تلاش', ''].map(function (h) { return el('th', {text: h}); }))]),
            el('tbody', {}, rows.map(function (x) { var st = ST[x.status] || [x.status, '']; return el('tr', {}, [el('td', {html: '<b>' + esc(x.full_name || '') + '</b>'}), el('td', {}, [el('span', {class: 'code', text: x.username || '—'})]), el('td', {}, [el('span', {class: 'chip ' + st[1], text: st[0] + (x.abandoned ? ' · ترک ' + fa(x.abandoned) : '')})]), el('td', {text: x.total_grade != null ? fa(S.fmtScore(x.total_grade)) : '—'}), el('td', {class: 'muted', style: 'font-size:12px', text: x.submitted_at ? S.fmtDate(x.submitted_at) : '—'}), el('td', {text: x.minutes_left != null ? fa(x.minutes_left) + ' دقیقه' : '—'}), el('td', {text: fa(x.attempts || 0) + ' / ' + fa(x.attempts_allowed || 1)}),
              el('td', {}, [el('div', {class: 'acts'}, [el('button', {class: 'icon-btn', title: 'تمدید ۱۰ دقیقه', html: '⏱+', onclick: async function () { var m = prompt('چند دقیقه تمدید شود؟ (۱ تا ۲۴۰)', '10'); if (!m) return; m = parseInt(en(m), 10); if (!(m >= 1 && m <= 240)) return toast('زمان تمدید باید بین ۱ تا ۲۴۰ دقیقه باشد.', 'err'); try { chk(await S.rpcObj('extend_student_time', {p_exam: ctx.examId, p_student: x.student_id, p_minutes: m})); toast('زمان تمدید شد.', 'ok'); draw(); } catch (e) { toast(errMsg(e), 'err'); } }}),
                el('button', {class: 'icon-btn', title: 'اجازهٔ تلاش مجدد', html: '↻', onclick: async function () { if (!(await S.confirmDlg('تلاش مجدد', 'به «' + esc(x.full_name) + '» اجازهٔ تلاش مجدد داده شود؟ (نسخهٔ قبلی نگه داشته می‌شود)', 'اجازه'))) return; try { chk(await S.rpcObj('reset_student_attempt', {p_exam: ctx.examId, p_student: x.student_id, p_keep_copy: true})); toast('ثبت شد.', 'ok'); draw(); } catch (e) { toast(errMsg(e), 'err'); } }})])])]); }))]) : S.emptyBox('👥', 'مخاطبی برای این آزمون تعریف نشده یا کسی هنوز وارد نشده است.')]));
      } catch (e) { S.showErr(body, e); }
    }
    await draw();
    timer = setInterval(function () { if (!document.body.contains(body)) return clearInterval(timer); draw(); }, 30000);
  }
  async function tabMonitor(body, ctx) {
    S.loading(body);
    try {
      var raw = chk(await S.rpcObj('native_monitor_list_v1', {p_exam: ctx.examId}));
      var rows = raw.rows || [];
      body.innerHTML = '';
      if (!rows.length) return body.appendChild(S.emptyBox('🛡', 'گزارشی ثبت نشده است.'));
      var EV = {app_leave: 'ترک صفحه/برنامه', window_blur: 'خروج از پنجره', screen_record_attempt: 'تلاش ضبط صفحه', multi_window: 'چندپنجره', screenshot: 'اسکرین‌شات'};
      rows.forEach(function (row) {
        var rep = row.report || {}, ev = rep.events || {}, times = rep.question_time_ms || {}, visits = rep.question_visits || {}, labels = rep.question_labels || {};
        var score = Object.keys(ev).reduce(function (s, k) { return s + num(ev[k]); }, 0);
        var det = el('div', {style: 'display:none;margin-top:8px'});
        var qrows = Object.keys(times).sort(function (a, b) { return num(labels[a]) - num(labels[b]); });
        det.appendChild(el('div', {class: 'row', style: 'flex-wrap:wrap;gap:6px'}, Object.keys(ev).map(function (k) { return el('span', {class: 'chip warn', text: (EV[k] || k) + ': ' + fa(ev[k])}); })));
        det.appendChild(el('div', {class: 'muted', style: 'font-size:12px;margin:6px 0', text: 'ورود: ' + S.fmtDate(new Date(num(rep.entered_at_epoch_ms)).toISOString()) + ' · آخرین فعالیت: ' + S.fmtDate(new Date(num(rep.left_at_epoch_ms)).toISOString()) + (rep.submitted ? ' · ارسال‌شده' : '')}));
        if (qrows.length) det.appendChild(el('table', {class: 'tbl'}, [el('thead', {}, [el('tr', {}, ['سؤال', 'مدت پاسخ‌گویی', 'بازدید'].map(function (h) { return el('th', {text: h}); }))]), el('tbody', {}, qrows.map(function (id) { var ms = num(times[id]); return el('tr', {}, [el('td', {text: fa(labels[id] || '?')}), el('td', {text: fa(Math.floor(ms / 60000)) + ':' + fa(String(Math.floor(ms % 60000 / 1000)).padStart(2, '0'))}), el('td', {text: fa(visits[id] || 0)})]); }))]));
        body.appendChild(el('div', {class: 'card', style: 'margin-bottom:8px'}, [el('div', {class: 'row', style: 'cursor:pointer', onclick: function () { det.style.display = det.style.display === 'none' ? '' : 'none'; }}, [el('b', {class: 'grow', text: row.student_name || 'دانش‌آموز'}), el('span', {class: 'chip ' + (score ? 'warn' : 'ok'), text: score ? fa(score) + ' رویداد مشکوک' : 'بدون تخلف'}), el('span', {class: 'muted', text: '▾'})]), det]));
      });
    } catch (e) { S.showErr(body, e); }
  }
  async function tabAnalysis(body, ctx) {
    S.loading(body);
    try {
      var raw = chk(await S.rpcObj('native_question_analysis_v1', {p_exam: ctx.examId}));
      body.innerHTML = '';
      body.appendChild(el('div', {class: 'grid3', style: 'margin-bottom:12px'}, [stat(fa(raw.answer_count || 0), 'پاسخ'), stat(raw.cronbach_alpha != null ? fa(Math.round(raw.cronbach_alpha * 100) / 100) : '—', 'آلفای کرونباخ'), stat(fa((raw.questions || []).length), 'سؤال')]));
      var LV = {easy: ['آسان', 'ok'], hard: ['سخت', 'warn'], balanced: ['متعادل', 'brand'], weak: ['ضعیف', 'danger']};
      body.appendChild(el('div', {class: 'card'}, [el('table', {class: 'tbl'}, [el('thead', {}, [el('tr', {}, ['#', 'سؤال', 'بارم', 'پاسخ‌داده', 'میانگین ٪', 'بی‌پاسخ ٪', 'تمیز', 'همبستگی', 'سطح'].map(function (h) { return el('th', {text: h}); }))]),
        el('tbody', {}, (raw.questions || []).map(function (q) { var lv = LV[q.level] || [q.level || '—', '']; return el('tr', {}, [el('td', {text: fa(q.index)}), el('td', {style: 'max-width:320px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap', text: (q.text || '').replace(/%%FIG:[\s\S]*?%%/g, '[شکل]')}), el('td', {text: fa(S.fmtScore(q.max_score))}), el('td', {text: fa(q.answered_count || 0) + ' / ' + fa(q.graded_count || 0)}), el('td', {}, [bar(num(q.average_percent))]), el('td', {text: fa(Math.round(num(q.omit_percent)))}), el('td', {text: q.discrimination != null ? fa(Math.round(q.discrimination * 100) / 100) : '—'}), el('td', {text: q.point_biserial != null ? fa(Math.round(q.point_biserial * 100) / 100) : '—'}), el('td', {}, [el('span', {class: 'chip ' + lv[1], text: lv[0]})])]); }))])]));
    } catch (e) { S.showErr(body, e); }
  }
  async function tabFeedbackBank(body, ctx) {
    S.loading(body);
    try {
      var list = await S.rpc('fb_list', {});
      body.innerHTML = '';
      var inp = el('input', {type: 'text', placeholder: 'عبارت بازخورد جدید…', style: 'flex:1;border:1px solid var(--line);border-radius:10px;padding:8px 12px'});
      body.appendChild(el('div', {class: 'card'}, [el('h3', {text: '💬 بانک بازخورد'}), el('div', {class: 'row', style: 'margin-bottom:10px'}, [inp, el('button', {class: 'btn sm', text: '➕ افزودن', onclick: async function () { if (!inp.value.trim()) return toast('متن بازخورد خالی است.', 'err'); try { chk(await S.rpcObj('fb_add', {p_text: inp.value.trim()})); ctx.refresh(); } catch (e) { toast(errMsg(e), 'err'); } }})]),
        (list || []).length ? el('div', {}, list.map(function (p) { return el('div', {class: 'row', style: 'padding:8px 0;border-bottom:1px solid var(--line)'}, [el('span', {class: 'grow', text: p.text}), el('button', {class: 'icon-btn', html: '✎', onclick: async function () { var t = prompt('ویرایش بازخورد:', p.text); if (t == null || !t.trim()) return; try { chk(await S.rpcObj('native_feedback_update_v1', {p_id: p.id, p_text: t.trim()})); ctx.refresh(); } catch (e) { toast(errMsg(e), 'err'); } }}), el('button', {class: 'icon-btn danger', html: '🗑', onclick: async function () { if (!(await S.confirmDlg('حذف بازخورد', esc(p.text), 'حذف', true))) return; try { chk(await S.rpcObj('native_feedback_delete_v1', {p_id: p.id})); ctx.refresh(); } catch (e) { toast(errMsg(e), 'err'); } }})]); })) : S.emptyBox('💬', 'هنوز عبارتی ثبت نشده است.')]));
    } catch (e) { S.showErr(body, e); }
  }
  function stat(v, l) { return el('div', {class: 'stat'}, [el('div', {class: 'v', text: v}), el('div', {class: 'l', text: l})]); }
  function bar(p) { p = Math.max(0, Math.min(100, p)); return el('div', {class: 'g-bar', title: fa(Math.round(p)) + '٪'}, [el('div', {style: 'width:' + p + '%;background:' + (p >= 70 ? 'var(--ok)' : p >= 40 ? 'var(--warn)' : 'var(--danger)')}), el('span', {text: fa(Math.round(p)) + '٪'})]); }
  function lightbox(u) { var bg = el('div', {class: 'modal-bg', onclick: function () { bg.remove(); }}, [el('img', {src: u, style: 'max-width:92vw;max-height:90vh;border-radius:12px;background:#fff'})]); document.body.appendChild(bg); }

  /* ================================================================ کارنامهٔ دانش‌آموز — جزئیات پاسخ */
  async function answerDetail(answerId) {
    var bg = el('div', {class: 'modal-bg'}); var m = el('div', {class: 'modal wide'}, [el('button', {class: 'x', text: '✕', onclick: function () { bg.remove(); }})]); bg.appendChild(m); document.body.appendChild(bg);
    var body = el('div'); m.appendChild(body); S.loading(body);
    try {
      var raw = chk(await S.rpcObj('native_my_answer_detail_v1', {p_answer: answerId}));
      body.innerHTML = '';
      body.appendChild(el('h2', {text: raw.title || 'آزمون'}));
      body.appendChild(el('div', {class: 'row', style: 'margin-bottom:10px'}, [el('span', {class: 'chip ' + (raw.graded ? 'ok' : 'warn'), text: raw.graded ? 'نمره: ' + fa(S.fmtScore(raw.total_grade)) + ' از ' + fa(S.fmtScore(raw.total_score)) : 'در انتظار تصحیح'}), el('span', {class: 'muted', style: 'font-size:12px', text: S.fmtDate(raw.submitted_at)})]));
      if (raw.feedback) body.appendChild(el('div', {class: 'alert info', text: '💬 ' + raw.feedback}));
      var responses = raw.responses || [], grades = raw.grades || [], imgs = raw.response_images || {};
      (raw.questions || []).forEach(function (q, i) {
        var qq = Object.assign({}, q, {__key: q, __type: qt(q.type)});
        var r = responses[i], rt = responseText(qq, r), ct = raw.graded ? correctText(qq) : null, g = raw.graded ? grades[i] : null;
        body.appendChild(el('div', {class: 'g-q'}, [el('div', {class: 'row'}, [el('b', {text: 'سؤال ' + fa(i + 1)}), el('span', {class: 'grow'}), g != null ? el('span', {class: 'chip ' + (num(g) >= num(q.score) ? 'ok' : num(g) > 0 ? 'warn' : 'danger'), text: fa(S.fmtScore(g)) + ' / ' + fa(S.fmtScore(q.score))}) : el('span', {class: 'chip', text: 'از ' + fa(S.fmtScore(q.score))})]),
          el('div', {class: 'g-qtext', text: (q.text || '').replace(/%%FIG:[\s\S]*?%%/g, '[شکل]')}),
          el('div', {class: 'g-resp'}, [el('span', {class: 'muted', text: 'پاسخ شما: '}), rt != null ? el('span', {text: rt}) : el('i', {class: 'muted', text: 'بدون پاسخ'})]),
          ct ? el('div', {class: 'g-resp muted', style: 'font-size:13px', text: 'پاسخ درست: ' + ct}) : null,
          raw.graded && q.explanation ? el('div', {class: 'alert info', style: 'font-size:13px', text: 'توضیح: ' + q.explanation}) : null,
          (imgs[q.id] || imgs[String(i)] || []).length ? el('div', {class: 'st-imgs'}, (imgs[q.id] || imgs[String(i)]).map(function (u) { return el('img', {src: u, onclick: function () { lightbox(u); }}); })) : null]));
      });
    } catch (e) { S.showErr(body, e); }
  }

  /* ================================================================ شارژ کیف پول (Edge Function wallet-payment) */
  var MIN_TOP_UP = 100000, STEP = 10000, MAX_BALANCE = 10000000;
  function topUpCard(balance, refresh) {
    var amt = el('input', {type: 'number', step: STEP, min: MIN_TOP_UP, value: 200000, style: 'direction:ltr'});
    var msg = el('div');
    var card = el('div', {class: 'card', style: 'margin-top:16px'}, [el('h3', {text: '💳 شارژ امن کیف پول'}), el('p', {class: 'muted', style: 'font-size:13px;margin:0 0 10px', text: 'پرداخت فقط در سرور تأیید می‌شود؛ حداقل ۱۰۰٬۰۰۰ · مضرب ۱۰٬۰۰۰ · سقف موجودی ۱۰٬۰۰۰٬۰۰۰ تومان.'}),
      el('div', {class: 'row', style: 'flex-wrap:wrap;gap:6px;margin-bottom:8px'}, [100000, 200000, 500000, 1000000].map(function (v) { return el('button', {class: 'chip', style: 'cursor:pointer', text: S.money(v), onclick: function () { amt.value = v; }}); })),
      el('div', {class: 'grid2'}, [el('div', {class: 'field'}, [el('label', {text: 'مبلغ (تومان)'}), amt]), el('div', {class: 'field'}, [el('label', {text: ' '}), el('button', {class: 'btn', text: 'رفتن به درگاه امن', onclick: async function () {
        var a = num(en(amt.value)); msg.innerHTML = '';
        try {
          if (a < MIN_TOP_UP) throw new Error('حداقل شارژ ۱۰۰٬۰۰۰ تومان است.');
          if (a % STEP !== 0) throw new Error('مبلغ شارژ باید مضربی از ۱۰٬۰۰۰ تومان باشد.');
          if (a > MAX_BALANCE || num(balance) + a > MAX_BALANCE) throw new Error('موجودی پس از شارژ از سقف ۱۰٬۰۰۰٬۰۰۰ تومان بیشتر می‌شود.');
          var d = await S.http('/functions/v1/wallet-payment', {method: 'POST', body: {amount_toman: a}});
          if (d && d.error) throw new Error((d.code ? d.code + ': ' : '') + d.error);
          if (d.credited) { msg.appendChild(el('div', {class: 'alert ok', text: '✅ شارژ انجام شد (حالت آزمایشی). موجودی: ' + S.money(d.balance)})); refresh(); return; }
          var u; try { u = new URL(d.url); } catch (e) { throw new Error('نشانی درگاه معتبر نیست.'); }
          var h = u.hostname.toLowerCase(), sb = new URL(S.config.url).hostname;
          if (u.protocol !== 'https:' || !(h === 'payment.zarinpal.com' || h === 'idpay.ir' || /\.idpay\.ir$/.test(h) || h === sb)) throw new Error('نشانی درگاه در فهرست مجاز نیست.');
          msg.appendChild(el('div', {class: 'alert info', html: 'سفارش ' + fa(d.order_id) + ' ثبت شد؛ در حال انتقال به درگاه امن' + (d.provider === 'zarinpal' ? ' زرین‌پال' : d.provider === 'idpay' ? ' آیدی‌پی' : '') + (d.sandbox ? ' (آزمایشی)' : '') + '… <a href="' + esc(d.url) + '" target="_blank" rel="noopener">اگر منتقل نشدید اینجا بزنید</a>'}));
          window.open(d.url, '_blank', 'noopener');
        } catch (e) { msg.appendChild(el('div', {class: 'alert error', text: errMsg(e)})); }
      }})])]), msg,
      el('p', {class: 'muted', style: 'font-size:12px', text: 'پس از بازگشت از درگاه، این صفحه را نوسازی کنید تا موجودی به‌روز شود. هر سؤال آزمون آنلاین ۱٬۰۰۰ تومان.'})]);
    return card;
  }

  /* ================================================================ پنل مدیر */
  async function managerTeachersPage(c) {
    S.loading(c);
    try {
      var r = await Promise.all([S.rpcObj('native_manager_teachers_v37', {}), S.rpcObj('native_manager_invites_v40b', {}).catch(function () { return {items: []}; }), S.rpcObj('native_teacher_schools_v61', {}).catch(function () { return {items: []}; })]);
      var list = chk(r[0]).items || [], invites = r[1].items || [], schools = r[2].items || [];
      c.innerHTML = '';
      var refresh = function () { managerTeachersPage(c); };
      /* دعوت */
      var cnt = el('select', {style: 'border:1px solid var(--line);border-radius:10px;padding:8px'}); [1, 2, 3, 4, 5].forEach(function (n) { cnt.appendChild(el('option', {value: String(n), text: fa(n) + ' کد'})); });
      var sch = el('select', {style: 'border:1px solid var(--line);border-radius:10px;padding:8px'}); sch.appendChild(el('option', {value: '', text: 'مدرسهٔ اصلی'})); schools.forEach(function (s) { sch.appendChild(el('option', {value: s.id, text: s.name})); });
      var inv = el('div', {class: 'card'}, [el('h3', {text: '✉ دعوت معلم'}), el('div', {class: 'row', style: 'margin-bottom:10px'}, [cnt, schools.length > 1 ? sch : null, el('button', {class: 'btn sm', text: '➕ ساخت کد دعوت', onclick: async function () { try { var n = Number(cnt.value); var raw = sch.value ? await S.rpcObj('native_manager_create_teacher_invites_v62', {p_count: n, p_school: sch.value}) : await S.rpcObj('native_manager_create_teacher_invites_v40b', {p_count: n}); chk(raw); toast('کد دعوت ساخته شد.', 'ok'); refresh(); } catch (e) { toast(errMsg(e), 'err'); } }})])]);
      if (invites.length) inv.appendChild(el('table', {class: 'tbl'}, [el('thead', {}, [el('tr', {}, ['کد', 'انقضا', 'وضعیت', ''].map(function (h) { return el('th', {text: h}); }))]), el('tbody', {}, invites.map(function (i) { var st = i.used ? ['استفاده‌شده' + (i.used_at ? ' · ' + S.fmtDate(i.used_at) : ''), 'ok'] : i.revoked ? ['باطل‌شده', ''] : ['فعال', 'brand']; return el('tr', {}, [el('td', {}, [el('span', {class: 'code', text: i.code}), el('button', {class: 'icon-btn', title: 'کپی', html: '⧉', onclick: function () { navigator.clipboard && navigator.clipboard.writeText(i.code); toast('کپی شد.', 'ok'); }})]), el('td', {class: 'muted', style: 'font-size:12px', text: S.fmtDate(i.expires_at)}), el('td', {}, [el('span', {class: 'chip ' + st[1], text: st[0]})]), el('td', {}, [!i.used && !i.revoked ? el('button', {class: 'icon-btn danger', title: 'ابطال', html: '⛔', onclick: async function () { try { chk(await S.rpcObj('native_manager_revoke_invite_v40b', {p_invite: i.id})); refresh(); } catch (e) { toast(errMsg(e), 'err'); } }}) : el('button', {class: 'icon-btn danger', title: 'حذف', html: '🗑', onclick: async function () { try { try { chk(await S.rpcObj('native_manager_delete_invite_v61', {p_invite: i.id})); } catch (e1) { chk(await S.rpcObj('native_manager_revoke_invite_v40b', {p_invite: i.id})); } refresh(); } catch (e) { toast(errMsg(e), 'err'); } }})])]); }))]));
      c.appendChild(inv);
      /* معلم‌ها */
      var card = el('div', {class: 'card', style: 'margin-top:16px'}, [el('h3', {text: '👩‍🏫 معلم‌های مدرسه (' + fa(list.length) + ')'})]);
      if (!list.length) card.appendChild(S.emptyBox('👩‍🏫', 'هنوز معلمی عضو نشده است. با کد دعوت بالا معلم اضافه کنید.'));
      else card.appendChild(el('table', {class: 'tbl'}, [el('thead', {}, [el('tr', {}, ['نام', 'نام کاربری', 'کد پرسنلی', 'تلفن', 'کیف پول', 'وضعیت', ''].map(function (h) { return el('th', {text: h}); }))]),
        el('tbody', {}, list.map(function (t) { var active = t.status === 'active'; return el('tr', {}, [el('td', {html: '<b>' + esc(t.full_name || '') + '</b><div class="muted" style="font-size:12px">' + esc(t.email || '') + '</div>'}), el('td', {}, [el('span', {class: 'code', text: t.username || '—'})]), el('td', {text: t.employee_code || '—'}), el('td', {text: t.phone || '—'}), el('td', {text: S.money(t.wallet_balance || 0)}), el('td', {}, [el('span', {class: 'chip ' + (active ? 'ok' : ''), text: active ? 'فعال' : 'غیرفعال'})]),
          el('td', {}, [el('div', {class: 'acts'}, [
            el('button', {class: 'icon-btn', title: 'انتقال اعتبار', html: '💸', onclick: function () { transferDlg(t, refresh); }}),
            el('button', {class: 'icon-btn', title: 'کلاس‌های معلم', html: '🏫', onclick: function () { S.go('school', {teacherId: t.id, teacherName: t.full_name}); }}),
            el('button', {class: 'icon-btn', title: active ? 'غیرفعال‌سازی' : 'فعال‌سازی', html: active ? '⏸' : '▶', onclick: async function () { try { chk(await S.rpcObj('native_manager_set_teacher_active_v40b', {p_teacher: t.id, p_active: !active})); refresh(); } catch (e) { toast(errMsg(e), 'err'); } }}),
            el('button', {class: 'icon-btn danger', title: 'حذف از مدرسه', html: '🗑', onclick: async function () { if (!(await S.confirmDlg('حذف معلم', '«' + esc(t.full_name) + '» از مدرسه حذف شود؟', 'حذف', true))) return; try { chk(await S.rpcObj('native_manager_remove_teacher_v40b', {p_teacher: t.id})); toast('حذف شد.', 'ok'); refresh(); } catch (e) { toast(errMsg(e), 'err'); } }})])])]); }))]));
      c.appendChild(card);
    } catch (e) { S.showErr(c, e); }
  }
  function transferDlg(t, refresh) {
    var bg = el('div', {class: 'modal-bg'}); var amt = el('input', {type: 'number', step: 1000, min: 1000, value: 50000, style: 'direction:ltr'}); var msg = el('div');
    var m = el('div', {class: 'modal'}, [el('button', {class: 'x', text: '✕', onclick: function () { bg.remove(); }}), el('h2', {text: '💸 انتقال اعتبار به ' + (t.full_name || 'معلم')}), el('div', {class: 'field'}, [el('label', {text: 'مبلغ (تومان، مضرب ۱٬۰۰۰)'}), amt]), msg,
      el('div', {class: 'row'}, [el('button', {class: 'btn', text: 'انتقال', onclick: async function () { var a = num(en(amt.value)); msg.innerHTML = ''; if (!(a > 0 && a % 1000 === 0)) return msg.appendChild(el('div', {class: 'alert error', text: 'مبلغ انتقال باید مثبت و مضرب ۱٬۰۰۰ تومان باشد.'})); try { var raw = chk(await S.rpcObj('native_manager_transfer_wallet_v38', {p_teacher: t.id, p_amount_toman: a, p_operation: uuid()})); toast('انتقال انجام شد.', 'ok'); msg.appendChild(el('div', {class: 'alert ok', text: 'انتقال ' + S.money(raw.amount || a) + ' انجام شد. موجودی شما: ' + S.money(raw.manager_balance || 0) + ' · موجودی معلم: ' + S.money(raw.teacher_balance || 0)})); refresh(); } catch (e) { msg.appendChild(el('div', {class: 'alert error', text: errMsg(e)})); } }}), el('button', {class: 'btn light', text: 'بستن', onclick: function () { bg.remove(); }})])]);
    bg.appendChild(m); document.body.appendChild(bg);
  }
  async function managerSchoolPage(c, arg) {
    arg = arg || {};
    if (arg.classId) return managerRoster(c, arg);
    if (arg.teacherId) return managerTeacherClasses(c, arg);
    S.loading(c);
    try {
      var r = await Promise.all([S.rpcObj('native_manager_school_summary_v36', {}).catch(function () { return {}; }), S.rpcObj('native_manager_teachers_v37', {}), S.rpcObj('native_manager_school_students_v40c', {}).catch(function () { return {items: []}; })]);
      var s = r[0] || {}, teachers = chk(r[1]).items || [], students = r[2].items || [];
      c.innerHTML = '';
      c.appendChild(el('div', {class: 'card'}, [el('h3', {text: '🏫 ' + (s.school_name || 'مدرسه')}), el('div', {class: 'muted', text: [s.province, s.city].filter(Boolean).join('، ')})]));
      c.appendChild(el('div', {class: 'grid4', style: 'margin-top:16px'}, [stat(fa(teachers.length), 'معلم'), stat(fa(students.length || s.students || 0), 'دانش‌آموز'), stat(fa(s.classes || 0), 'کلاس'), stat(fa(s.exams || 0), 'آزمون')]));
      var tc = el('div', {class: 'card', style: 'margin-top:16px'}, [el('h3', {text: '👩‍🏫 کلاس‌های معلم‌ها'})]);
      if (!teachers.length) tc.appendChild(S.emptyBox('👩‍🏫', 'معلمی وجود ندارد.'));
      else tc.appendChild(el('div', {class: 'exam-grid'}, teachers.map(function (t) { return el('div', {class: 'exam-card', style: 'cursor:pointer', onclick: function () { S.go('school', {teacherId: t.id, teacherName: t.full_name}); }}, [el('b', {text: t.full_name || '—'}), el('div', {class: 'muted', style: 'font-size:12px', text: '@' + (t.username || '—')}), el('div', {style: 'margin-top:6px'}, [el('span', {class: 'chip brand', text: 'مدیریت کلاس‌ها ←'})])]); })));
      c.appendChild(tc);
      var q = el('input', {type: 'search', placeholder: 'جست‌وجوی دانش‌آموز…', style: 'border:1px solid var(--line);border-radius:10px;padding:8px 12px;flex:1'});
      var tb = el('tbody');
      function draw() { var sx = q.value.trim().toLowerCase(); tb.innerHTML = ''; students.filter(function (x) { return !sx || (x.full_name || '').toLowerCase().indexOf(sx) >= 0 || (x.username || '').toLowerCase().indexOf(sx) >= 0; }).slice(0, 300).forEach(function (x) { tb.appendChild(el('tr', {}, [el('td', {text: x.full_name || '—'}), el('td', {}, [el('span', {class: 'code', text: x.username || '—'})])])); }); }
      q.addEventListener('input', draw); draw();
      /* V150 — کارت «دانش‌آموزان» منوی موبایل مدیر: مستقیم به بخش دانش‌آموزان مدرسه */
      if (arg.students) setTimeout(function () { var last = c.lastElementChild; if (last && last.scrollIntoView) last.scrollIntoView({behavior: 'smooth'}); }, 50);
      c.appendChild(el('div', {class: 'card', style: 'margin-top:16px'}, [el('div', {class: 'row', style: 'margin-bottom:10px'}, [el('h3', {class: 'grow', text: '🎓 دانش‌آموزان مدرسه (' + fa(students.length) + ')'}), q]), students.length ? el('table', {class: 'tbl'}, [el('thead', {}, [el('tr', {}, ['نام', 'نام کاربری'].map(function (h) { return el('th', {text: h}); }))]), tb]) : S.emptyBox('🎓', 'دانش‌آموزی ثبت نشده است.')]));
    } catch (e) { S.showErr(c, e); }
  }
  async function managerTeacherClasses(c, arg) {
    S.loading(c);
    try {
      var raw = chk(await S.rpcObj('native_manager_teacher_classes_v40c', {p_teacher: arg.teacherId}));
      var items = raw.items || [];
      c.innerHTML = '';
      var refresh = function () { managerTeacherClasses(c, arg); };
      c.appendChild(el('div', {class: 'row', style: 'margin-bottom:12px'}, [el('button', {class: 'btn light sm', text: '→ مدرسه', onclick: function () { S.go('school'); }}), el('h2', {class: 'grow', style: 'margin:0;font-size:18px', text: 'کلاس‌های ' + (raw.teacher_name || arg.teacherName || 'معلم')}), el('button', {class: 'btn sm', text: '➕ کلاس جدید', onclick: function () { classDlg(null, async function (v) { chk(await S.rpcObj('native_manager_save_teacher_class_v40c', {p_teacher: arg.teacherId, p_name: v.name, p_grade: v.grade, p_field: v.field})); refresh(); }); }})]));
      var card = el('div', {class: 'card'});
      if (!items.length) card.appendChild(S.emptyBox('🏫', 'این معلم هنوز کلاسی ندارد.'));
      else card.appendChild(el('table', {class: 'tbl'}, [el('thead', {}, [el('tr', {}, ['نام کلاس', 'پایه', 'رشته', 'دانش‌آموز', ''].map(function (h) { return el('th', {text: h}); }))]),
        el('tbody', {}, items.map(function (k) { return el('tr', {}, [el('td', {html: '<b>' + esc(k.name || '') + '</b>'}), el('td', {text: k.grade || '—'}), el('td', {text: k.field_of_study || '—'}), el('td', {text: fa(k.total || 0)}), el('td', {}, [el('div', {class: 'acts'}, [
          el('button', {class: 'icon-btn', title: 'فهرست دانش‌آموزان', html: '👥', onclick: function () { S.go('school', {teacherId: arg.teacherId, teacherName: arg.teacherName, classId: k.id, className: k.name}); }}),
          el('button', {class: 'icon-btn', title: 'ویرایش', html: '✎', onclick: function () { classDlg(k, async function (v) { var r = chk(await S.rpcObj('native_manager_change_teacher_class_v41', {p_class: k.id, p_action: 'edit', p_payload: {name: v.name, grade: v.grade, field: v.field}})); if (r.approval_required) throw new Error(r.message || 'نیاز به تأیید معلم دارد.'); refresh(); }); }}),
          el('button', {class: 'icon-btn danger', title: 'حذف', html: '🗑', onclick: async function () { if (!(await S.confirmDlg('حذف کلاس', 'کلاس «' + esc(k.name) + '» حذف شود؟', 'حذف', true))) return; try { var r = chk(await S.rpcObj('native_manager_change_teacher_class_v41', {p_class: k.id, p_action: 'delete'})); if (r.approval_required) throw new Error(r.message || 'نیاز به تأیید معلم دارد.'); refresh(); } catch (e) { toast(errMsg(e), 'err'); } }})])])]); }))]));
      c.appendChild(card);
    } catch (e) { S.showErr(c, e); }
  }
  function classDlg(k, onSave) {
    var bg = el('div', {class: 'modal-bg'}); var name = el('input', {type: 'text', value: k ? k.name || '' : ''}), grade = el('input', {type: 'text', value: k ? k.grade || '' : '', placeholder: 'مثلاً دهم'}), field = el('input', {type: 'text', value: k ? k.field_of_study || '' : '', placeholder: 'مثلاً ریاضی'}); var msg = el('div');
    var m = el('div', {class: 'modal'}, [el('button', {class: 'x', text: '✕', onclick: function () { bg.remove(); }}), el('h2', {text: k ? 'ویرایش کلاس' : 'کلاس جدید'}), el('div', {class: 'field'}, [el('label', {text: 'نام کلاس'}), name]), el('div', {class: 'grid2'}, [el('div', {class: 'field'}, [el('label', {text: 'پایه'}), grade]), el('div', {class: 'field'}, [el('label', {text: 'رشته'}), field])]), msg,
      el('div', {class: 'row'}, [el('button', {class: 'btn', text: 'ذخیره', onclick: async function () { msg.innerHTML = ''; if (!name.value.trim()) return msg.appendChild(el('div', {class: 'alert error', text: 'نام کلاس را وارد کنید.'})); try { await onSave({name: name.value.trim(), grade: grade.value.trim(), field: field.value.trim()}); bg.remove(); toast('ذخیره شد.', 'ok'); } catch (e) { msg.appendChild(el('div', {class: 'alert error', text: errMsg(e)})); } }}), el('button', {class: 'btn light', text: 'انصراف', onclick: function () { bg.remove(); }})])]);
    bg.appendChild(m); document.body.appendChild(bg);
  }
  async function managerRoster(c, arg) {
    S.loading(c);
    try {
      var r = await Promise.all([S.rpcObj('native_manager_class_roster_v40c', {p_class: arg.classId}), S.rpcObj('native_manager_school_students_v40c', {})]);
      var roster = chk(r[0]), all = chk(r[1]).items || [], inClass = roster.items || [], ids = {}; inClass.forEach(function (x) { ids[x.id] = true; });
      c.innerHTML = '';
      var refresh = function () { managerRoster(c, arg); };
      c.appendChild(el('div', {class: 'row', style: 'margin-bottom:12px'}, [el('button', {class: 'btn light sm', text: '→ کلاس‌ها', onclick: function () { S.go('school', {teacherId: arg.teacherId, teacherName: arg.teacherName}); }}), el('h2', {class: 'grow', style: 'margin:0;font-size:18px', text: 'دانش‌آموزان کلاس ' + (roster.class_name || arg.className || '')})]));
      var q = el('input', {type: 'search', placeholder: 'جست‌وجو برای افزودن…', style: 'border:1px solid var(--line);border-radius:10px;padding:8px 12px;flex:1'});
      var addList = el('div', {class: 'b-aud'});
      function drawAdd() { var sx = q.value.trim().toLowerCase(); addList.innerHTML = ''; all.filter(function (x) { return !ids[x.id] && (!sx || (x.full_name || '').toLowerCase().indexOf(sx) >= 0 || (x.username || '').toLowerCase().indexOf(sx) >= 0); }).slice(0, 60).forEach(function (x) { addList.appendChild(el('div', {class: 'row'}, [el('span', {class: 'grow', text: (x.full_name || '—') + ' (' + (x.username || '') + ')'}), el('button', {class: 'btn light sm', text: '➕', onclick: async function () { try { chk(await S.rpcObj('native_manager_set_class_student_v40c', {p_class: arg.classId, p_student: x.id, p_add: true})); refresh(); } catch (e) { toast(errMsg(e), 'err'); } }})])); }); if (!addList.children.length) addList.appendChild(el('div', {class: 'muted', text: 'موردی نیست.'})); }
      q.addEventListener('input', drawAdd); drawAdd();
      c.appendChild(el('div', {class: 'b-body'}, [
        el('div', {class: 'card'}, [el('h3', {text: '➕ افزودن از دانش‌آموزان مدرسه'}), q, el('div', {style: 'margin-top:8px'}, [addList])]),
        el('div', {class: 'card'}, [el('h3', {text: '👥 اعضای کلاس (' + fa(inClass.length) + ')'}), inClass.length ? el('table', {class: 'tbl'}, [el('thead', {}, [el('tr', {}, ['نام', 'نام کاربری', ''].map(function (h) { return el('th', {text: h}); }))]), el('tbody', {}, inClass.map(function (x) { return el('tr', {}, [el('td', {text: x.full_name || '—'}), el('td', {}, [el('span', {class: 'code', text: x.username || '—'})]), el('td', {}, [el('button', {class: 'icon-btn danger', title: 'حذف از کلاس', html: '✕', onclick: async function () { try { chk(await S.rpcObj('native_manager_set_class_student_v40c', {p_class: arg.classId, p_student: x.id, p_add: false})); refresh(); } catch (e) { toast(errMsg(e), 'err'); } }})])]); }))]) : S.emptyBox('👥', 'کلاس خالی است.')])
      ]));
    } catch (e) { S.showErr(c, e); }
  }

  /* ================================================================ تقویم شمسی (JalaliCalendar — الگوریتم جلالی استاندارد) */
  var J = (function () {
    /* الگوریتم jalaali-js (Behrooz Kamali) */
    function div(a, b) { return ~~(a / b); }
    function mod(a, b) { return a - ~~(a / b) * b; }
    var breaks = [-61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178];
    function jalCal(jy) {
      var bl = breaks.length, gy = jy + 621, leapJ = -14, jp = breaks[0], jm, jump, leap, n, i;
      for (i = 1; i < bl; i += 1) { jm = breaks[i]; jump = jm - jp; if (jy < jm) break; leapJ = leapJ + div(jump, 33) * 8 + div(mod(jump, 33), 4); jp = jm; }
      n = jy - jp;
      leapJ = leapJ + div(n, 33) * 8 + div(mod(n, 33) + 3, 4);
      if (mod(jump, 33) === 4 && jump - n === 4) leapJ += 1;
      var leapG = div(gy, 4) - div((div(gy, 100) + 1) * 3, 4) - 150;
      var march = 20 + leapJ - leapG;
      if (jump - n < 6) n = n - jump + div(jump + 4, 33) * 33;
      leap = mod(mod(n + 1, 33) - 1, 4); if (leap === -1) leap = 4;
      return {leap: leap, gy: gy, march: march};
    }
    function g2d(gy, gm, gd) { var d = div((gy + div(gm - 8, 6) + 100100) * 1461, 4) + div(153 * mod(gm + 9, 12) + 2, 5) + gd - 34840408; d = d - div(div(gy + 100100 + div(gm - 8, 6), 100) * 3, 4) + 752; return d; }
    function d2g(jdn) { var j = 4 * jdn + 139361631; j = j + div(div(4 * jdn + 183187720, 146097) * 3, 4) * 4 - 3908; var i = div(mod(j, 1461), 4) * 5 + 308; var gd = div(mod(i, 153), 5) + 1, gm = mod(div(i, 153), 12) + 1, gy = div(j, 1461) - 100100 + div(8 - gm, 6); return {gy: gy, gm: gm, gd: gd}; }
    function j2d(jy, jm, jd) { var r = jalCal(jy); return g2d(r.gy, 3, r.march) + (jm - 1) * 31 - div(jm, 7) * (jm - 7) + jd - 1; }
    function d2j(jdn) { var gy = d2g(jdn).gy, jy = gy - 621, r = jalCal(jy), jdn1f = g2d(gy, 3, r.march), jd, jm, k; k = jdn - jdn1f; if (k >= 0) { if (k <= 185) { jm = 1 + div(k, 31); jd = mod(k, 31) + 1; return {jy: jy, jm: jm, jd: jd}; } else k -= 186; } else { jy -= 1; k += 179; if (r.leap === 1) k += 1; } jm = 7 + div(k, 30); jd = mod(k, 30) + 1; return {jy: jy, jm: jm, jd: jd}; }
    var jdnToG = d2g;
    return {
      isLeap: function (jy) { return jalCal(jy).leap === 0; },
      monthLength: function (jy, jm) { return jm <= 6 ? 31 : jm <= 11 ? 30 : (jalCal(jy).leap === 0 ? 30 : 29); },
      toGregorian: function (jy, jm, jd) { var g = jdnToG(j2d(jy, jm, jd)); return new Date(Date.UTC(g.gy, g.gm - 1, g.gd)); },
      fromGregorian: function (d) { return d2j(g2d(d.getFullYear(), d.getMonth() + 1, d.getDate())); },
      iso: function (jy, jm, jd) { return J.toGregorian(jy, jm, jd).toISOString().slice(0, 10); },
      MONTHS: ['فروردین', 'اردیبهشت', 'خرداد', 'تیر', 'مرداد', 'شهریور', 'مهر', 'آبان', 'آذر', 'دی', 'بهمن', 'اسفند'],
      DAYS: ['ش', 'ی', 'د', 'س', 'چ', 'پ', 'ج']
    };
  })();
  async function calendarPage(c, arg) {
    arg = arg || {};
    var today = J.fromGregorian(new Date());
    var y = arg.y || today.jy, m = arg.m || today.jm;
    S.loading(c);
    try {
      var from = J.iso(y, m, 1), to = J.iso(y, m, J.monthLength(y, m));
      var r = await Promise.all([S.rpcObj('cal_month', {p_from: from, p_to: to}), S.rpcObj('holidays_for', {p_from: from, p_to: to}).catch(function () { return null; })]);
      var notes = chk(r[0]).notes || [], hol = r[1] && !r[1].error ? r[1] : null;
      var byDate = {}; notes.forEach(function (n) { (byDate[n.on_date] = byDate[n.on_date] || []).push(n); });
      var hols = {}; (hol && hol.days || []).forEach(function (h) { var k = h.jy + '-' + h.jm + '-' + h.jd; (hols[k] = hols[k] || []).push(h); });
      c.innerHTML = '';
      var isTeacher = S.user().role === 'teacher';
      c.appendChild(el('div', {class: 'row', style: 'margin-bottom:12px'}, [el('button', {class: 'btn light sm', text: '‹ ماه بعد', onclick: function () { var nm = m + 1, ny = y; if (nm > 12) { nm = 1; ny++; } S.go('calendar', {y: ny, m: nm}); }}), el('h2', {class: 'grow', style: 'margin:0;text-align:center;font-size:18px', text: J.MONTHS[m - 1] + ' ' + fa(y)}), el('button', {class: 'btn light sm', text: 'ماه قبل ›', onclick: function () { var nm = m - 1, ny = y; if (nm < 1) { nm = 12; ny--; } S.go('calendar', {y: ny, m: nm}); }}), el('button', {class: 'btn light sm', text: 'امروز', onclick: function () { S.go('calendar'); }}), isTeacher ? el('button', {class: 'btn sm', text: '➕ پیام جدید', onclick: function () { noteDlg({on_date: J.iso(today.jy, today.jm, today.jd)}, function () { S.go('calendar', {y: y, m: m}); }); }}) : null]));
      var grid = el('div', {class: 'cal'});
      J.DAYS.forEach(function (d) { grid.appendChild(el('div', {class: 'cal-h', text: d})); });
      var first = J.toGregorian(y, m, 1); var off = (first.getUTCDay() + 1) % 7; /* شنبه=0 */
      for (var i = 0; i < off; i++) grid.appendChild(el('div', {class: 'cal-d empty'}));
      for (var d = 1; d <= J.monthLength(y, m); d++) (function (d) {
        var iso = J.iso(y, m, d), ns = byDate[iso] || [], hs = hols[y + '-' + m + '-' + d] || [], isHol = hs.some(function (h) { return h.holiday !== false; }) || (off + d - 1) % 7 === 6;
        var cell = el('div', {class: 'cal-d' + (isHol ? ' hol' : '') + (today.jy === y && today.jm === m && today.jd === d ? ' today' : ''), onclick: function () { dayDlg(iso, y, m, d, ns, hs, isTeacher, function () { S.go('calendar', {y: y, m: m}); }); }}, [el('div', {class: 'n', text: fa(d)})]);
        hs.slice(0, 1).forEach(function (h) { cell.appendChild(el('div', {class: 'cal-ev hol', text: h.title})); });
        ns.slice(0, 2).forEach(function (n) { cell.appendChild(el('div', {class: 'cal-ev', text: n.title})); });
        if (ns.length > 2) cell.appendChild(el('div', {class: 'muted', style: 'font-size:11px', text: '+' + fa(ns.length - 2)}));
        grid.appendChild(cell);
      })(d);
      c.appendChild(el('div', {class: 'card'}, [grid]));
      if (hol && hol.years && hol.years[String(y)] === false) c.appendChild(el('div', {class: 'alert warn', style: 'margin-top:10px', text: 'تعطیلات رسمی این سال تقریبی است.'}));
      if (!hol) c.appendChild(el('div', {class: 'muted', style: 'font-size:12px;margin-top:8px', text: 'اطلاعات تعطیلات رسمی در دسترس نیست.'}));
      if (!isTeacher) S.rpcObj('cal_unseen_v59', {}).then(function (u) { (u.notes || []).forEach(function (n) { S.rpcObj('cal_mark_seen_v59', {p_note: n.id}).catch(function () {}); }); }).catch(function () {});
    } catch (e) { S.showErr(c, e); }
  }
  function dayDlg(iso, y, m, d, notes, hols, isTeacher, refresh) {
    var bg = el('div', {class: 'modal-bg'}); var body = el('div');
    var mm = el('div', {class: 'modal'}, [el('button', {class: 'x', text: '✕', onclick: function () { bg.remove(); }}), el('h2', {text: fa(d) + ' ' + J.MONTHS[m - 1] + ' ' + fa(y)}), body]);
    hols.forEach(function (h) { body.appendChild(el('div', {class: 'alert ' + (h.holiday !== false ? 'error' : 'info'), text: (h.holiday !== false ? '🔴 تعطیل: ' : '📌 ') + h.title})); });
    if (!notes.length) body.appendChild(el('p', {class: 'muted', text: 'پیامی برای این روز نیست.'}));
    notes.forEach(function (n) { body.appendChild(el('div', {class: 'g-q'}, [el('div', {class: 'row'}, [el('b', {class: 'grow', text: n.title}), isTeacher ? el('button', {class: 'icon-btn', html: '✎', onclick: async function () { try { var full = await S.rpcObj('cal_day', {p_id: n.id}); bg.remove(); noteDlg(chk(full), refresh); } catch (e) { toast(errMsg(e), 'err'); } }}) : null, isTeacher ? el('button', {class: 'icon-btn danger', html: '🗑', onclick: async function () { if (!(await S.confirmDlg('حذف پیام', esc(n.title), 'حذف', true))) return; try { chk(await S.rpcObj('cal_delete_note', {p_id: n.id})); bg.remove(); refresh(); } catch (e) { toast(errMsg(e), 'err'); } }}) : null]), n.body ? el('div', {style: 'white-space:pre-wrap;font-size:14px', text: n.body}) : null, n.audience ? el('div', {class: 'muted', style: 'font-size:12px', text: 'مخاطب: ' + ({all: 'همه', classes: 'کلاس‌ها', students: 'دانش‌آموزان', schools: 'مدرسه‌ها'}[n.audience] || n.audience)}) : null])); });
    if (isTeacher) body.appendChild(el('div', {class: 'row', style: 'margin-top:10px'}, [el('button', {class: 'btn sm', text: '➕ پیام برای این روز', onclick: function () { bg.remove(); noteDlg({on_date: iso}, refresh); }})]));
    bg.appendChild(mm); document.body.appendChild(bg);
  }
  async function noteDlg(note, refresh) {
    var bg = el('div', {class: 'modal-bg'}); var st = {audience: note.audience || 'all', classes: (note.classes || []).slice(), students: (note.students || []).slice(), schools: (note.schools || []).slice()};
    var title = el('input', {type: 'text', value: note.title || ''}), body = el('textarea', {rows: 4}); body.value = note.body || '';
    var jd = J.fromGregorian(new Date(note.on_date + 'T12:00:00'));
    var dy = el('input', {type: 'number', value: jd.jy, style: 'direction:ltr'}), dm = el('select'), dd = el('select');
    J.MONTHS.forEach(function (n, i) { dm.appendChild(el('option', {value: String(i + 1), text: n})); }); dm.value = String(jd.jm);
    function fillDays() { dd.innerHTML = ''; var n = J.monthLength(num(dy.value, jd.jy), num(dm.value, 1)); for (var i = 1; i <= n; i++) dd.appendChild(el('option', {value: String(i), text: fa(i)})); dd.value = String(Math.min(jd.jd, n)); }
    fillDays(); dm.addEventListener('change', fillDays); dy.addEventListener('change', fillDays);
    var aud = el('div'); var msg = el('div');
    var opts = {classes: [], students: [], schools: []};
    try { var r = await Promise.all([S.rpc('my_classes', {}).catch(function () { return []; }), S.rpc('my_students_for_pick', {}).catch(function () { return []; }), S.rpcObj('native_teacher_schools_v61', {}).catch(function () { return {items: []}; })]); opts.classes = (r[0] || []).map(function (k) { return {id: k.id, name: k.name}; }); opts.students = (r[1] || []).map(function (s) { return {id: s.id, name: s.full_name + (s.class_names ? ' (' + s.class_names + ')' : '')}; }); opts.schools = (r[2].items || []).map(function (s) { return {id: s.id, name: s.name + (s.city ? ' — ' + s.city : '')}; }); } catch (e) {}
    function drawAud() {
      aud.innerHTML = '';
      aud.appendChild(el('div', {class: 'tabs'}, [['all', 'همه'], ['classes', 'کلاس‌ها'], ['students', 'دانش‌آموزان'], ['schools', 'مدرسه‌ها']].map(function (o) { return el('button', {class: st.audience === o[0] ? 'on' : '', text: o[1], onclick: function () { st.audience = o[0]; drawAud(); }}); })));
      if (st.audience === 'all') return;
      var src = opts[st.audience], sel = st[st.audience];
      if (!src.length) return aud.appendChild(el('div', {class: 'alert warn', text: 'موردی برای انتخاب نیست.'}));
      aud.appendChild(el('div', {class: 'b-aud'}, src.map(function (o) { var cb = el('input', {type: 'checkbox'}); cb.checked = sel.indexOf(o.id) >= 0; cb.addEventListener('change', function () { var i = sel.indexOf(o.id); if (cb.checked && i < 0) sel.push(o.id); if (!cb.checked && i >= 0) sel.splice(i, 1); }); return el('label', {class: 'row', style: 'gap:6px'}, [cb, o.name]); })));
    }
    drawAud();
    var m = el('div', {class: 'modal wide'}, [el('button', {class: 'x', text: '✕', onclick: function () { bg.remove(); }}), el('h2', {text: note.id ? 'ویرایش پیام' : 'پیام جدید'}),
      el('div', {class: 'grid3'}, [el('div', {class: 'field'}, [el('label', {text: 'سال'}), dy]), el('div', {class: 'field'}, [el('label', {text: 'ماه'}), dm]), el('div', {class: 'field'}, [el('label', {text: 'روز'}), dd])]),
      el('div', {class: 'field'}, [el('label', {text: 'عنوان'}), title]), el('div', {class: 'field'}, [el('label', {text: 'متن (اختیاری)'}), body]), el('h3', {text: '👥 مخاطبان'}), aud, msg,
      el('div', {class: 'row', style: 'margin-top:12px'}, [el('button', {class: 'btn', text: 'ذخیره', onclick: async function () {
        msg.innerHTML = '';
        try {
          if (!title.value.trim()) throw new Error('عنوان پیام را وارد کنید.');
          if (st.audience !== 'all' && !st[st.audience].length) throw new Error('حداقل یک مخاطب انتخاب کنید.');
          var iso = J.iso(num(dy.value), num(dm.value), num(dd.value));
          chk(await S.rpcObj('cal_save_note', {p_date: iso, p_title: title.value.trim(), p_body: body.value.trim() || null, p_audience: st.audience, p_classes: st.classes.slice().sort(), p_students: st.students.slice().sort(), p_schools: st.schools.slice().sort(), p_id: note.id || null}));
          toast('پیام ذخیره شد.', 'ok'); bg.remove(); refresh();
        } catch (e) { msg.appendChild(el('div', {class: 'alert error', text: errMsg(e)})); }
      }}), el('button', {class: 'btn light', text: 'انصراف', onclick: function () { bg.remove(); }})])]);
    bg.appendChild(m); document.body.appendChild(bg);
  }

  window.SiteAdmin = {gradingPage: gradingPage, questionAnalysis: function (body, examId) { return tabAnalysis(body, {examId: examId}); }, answerDetail: answerDetail, topUpCard: topUpCard, managerTeachersPage: managerTeachersPage, managerSchoolPage: managerSchoolPage, calendarPage: calendarPage, J: J, autoScore: autoScore, correctText: correctText};
})();
