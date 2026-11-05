/* ===================================================================
   V339 — انتخاب قالب کارنامه در سایت (انفرادی و کلاسی).
   دکمهٔ «ذخیرهٔ PDF» کارنامه به‌جای چاپ خام جدول، ابتدا ۱۰ قالب را نشان می‌دهد،
   بعد پیش‌نمایش A4 و سپس چاپ. موتور رندر همان فایل اپ است
   (app/src/main/assets/reportcard) که build_site.py آن را داخل engines می‌گذارد؛
   پس خروجی سایت و اپ یکسان است.
   =================================================================== */
(function () {
  var S = window.ExamSite;
  if (!S) return;
  var el = S.el, esc = S.esc, fa = S.fa;
  var ctx = null;

  /* ---- اطلاعات سربرگ از پروفایل/تنظیمات چاپ ---- */
  function header() {
    var h = {};
    try { h = S.readPrintHeader() || {}; } catch (e) {}
    var u = S.user() || {};
    return {
      name: h.school || u.school_name || u.schoolName || '',
      province: h.province || u.province || '',
      city: h.city || u.city || '',
      district: h.district || '',
      code: h.schoolCode || '',
      stage: h.stage || ''
    };
  }
  /* V340 — jalaliDisplay شیء Date می‌خواهد (نه رشتهٔ ISO) و خروجی‌اش رقم فارسی است؛
     پیش از این تاریخ صدور و سال تحصیلی کارنامهٔ سایت خالی می‌ماند. */
  function today() { try { return S.jalaliDisplay(new Date(), false); } catch (e) { return ''; } }
  function yearText() {
    var d = today();
    try { if (S.en) d = S.en(d); } catch (e) {}
    var y = parseInt(String(d).slice(0, 4), 10);
    if (!isFinite(y)) return '';
    var month = parseInt(String(d).slice(5, 7), 10);
    if (isFinite(month) && month < 7) y -= 1;
    return 'سال تحصیلی ' + fa(y) + '–' + fa(y + 1);
  }
  function base(scope, extra) {
    var p = {
      scope: scope, school: header(), year: yearText(), date: today(),
      className: '', grade: '', field: '', teacher: (S.user() || {}).name || '',
      levels: levelsOf(), code: String(Date.now()).slice(-6), structure: 'combined'
    };
    try { var st = JSON.parse(localStorage.getItem('rc_levels') || 'null'); if (st && st.length === 3) p.levels = st; } catch (e) {}
    Object.keys(extra || {}).forEach(function (k) { p[k] = extra[k]; });
    return p;
  }
  function levelsOf() {
    try {
      var o = JSON.parse(localStorage.getItem('exam_builder_opts') || '{}');
      if (o && o.levels && o.levels.length === 3) return o.levels.map(Number);
    } catch (e) {}
    return [85, 70, 50];
  }
  function colsFromExams(exams, maxOf) {
    return (exams || []).map(function (e) {
      return { key: String(e.id), title: e.title || 'آزمون', subject: e.subject || e.title || 'آزمون', max: Number(maxOf ? maxOf(e) : e.total_score) || 20 };
    });
  }

  /* ---- آداپتور ۱: کارنامهٔ دانش‌آموز (نتایج من) ---- */
  function fromStudentGrades(grades) {
    var u = S.user() || {};
    /* شکل دادهٔ my_grades در سایت: total_grade نمره، total_score بارم، graded_at یعنی تصحیح شده */
    var list = grades || [];
    var cols = list.map(function (g, i) {
      return { key: 'g' + i, title: g.title || 'آزمون', subject: g.subject || g.title || 'درس', max: Number(g.total_score) || 20 };
    });
    var vals = {}, pcts = {};
    list.forEach(function (g, i) {
      var mx = Number(g.total_score) || 0;
      var sc = g.graded_at ? Number(g.total_grade) || 0 : null;
      vals['g' + i] = sc;
      pcts['g' + i] = (sc != null && mx > 0) ? sc * 100 / mx : null;
    });
    var fb = list.map(function (g) { return g.feedback; }).filter(Boolean).slice(0, 3).join(' · ');
    return base('student', {
      student: { name: u.name || 'دانش‌آموز' }, className: u.class_name || '', grade: u.grade || '', teacher: '',
      cols: cols, rows: [{ name: u.name || 'دانش‌آموز', vals: vals, pcts: pcts }], feedback: fb
    });
  }

  /* ---- آداپتور ۲ و ۳: کارنامهٔ تکی و کلاسی معلم (پنل کارنامه) ---- */
  function fromTeacher(rows, exams, cls, oneName) {
    var cols = colsFromExams(exams, function (e) {
      var m = 0;
      (rows || []).forEach(function (r) { if (r.maxes && Number(r.maxes[e.id]) > m) m = Number(r.maxes[e.id]); });
      return m || Number(e.total_score) || 20;
    });
    var out = (rows || []).map(function (r) {
      var vals = {};
      (exams || []).forEach(function (e) { vals[String(e.id)] = r.scores && r.scores[e.id] != null ? Number(r.scores[e.id]) : null; });
      return { name: r.name, vals: vals };
    });
    var pcts = (rows || []).map(function (r) { return r.avg; }).filter(function (v) { return v != null; });
    var avg = pcts.length ? pcts.reduce(function (a, b) { return a + b; }, 0) / pcts.length : null;
    return base(oneName ? 'student' : 'class', {
      className: cls ? cls.name : '', grade: (cls && cls.grade) || '',
      student: oneName ? { name: oneName } : undefined,
      classInfo: { count: out.length, average: avg },
      cols: cols, rows: out
    });
  }

  /* ---- آداپتور ۴: کارنامهٔ کلاس مدیر ---- */
  function fromManagerClass(d) {
    var exams = d.exams || [], students = d.students || [];
    var cols = exams.map(function (e) {
      var m = 0;
      students.forEach(function (st) { var x = (st.grades || {})[e.id]; if (x && Number(x.max) > m) m = Number(x.max); });
      return { key: String(e.id), title: e.title || 'آزمون', subject: e.subject || e.title || 'آزمون', max: m || Number(e.total_score) || 20 };
    });
    var rows = students.map(function (st) {
      var vals = {};
      exams.forEach(function (e) { var x = (st.grades || {})[e.id]; vals[String(e.id)] = x && x.graded && x.grade != null ? Number(x.grade) : null; });
      return { name: st.full_name || st.username || '', vals: vals };
    });
    return base('class', { className: d.class_name || '', cols: cols, rows: rows, classInfo: { count: rows.length } });
  }

  /* V340 — کارنامهٔ انفرادی یک دانش‌آموز از دادهٔ کارنامهٔ کلاس مدیر */
  function fromManagerStudent(d, name) {
    var p = fromManagerClass(d);
    p.scope = 'student';
    p.student = { name: name || '' };
    p.teacher = '';
    p.rows = p.rows.filter(function (r) { return r.name === name; });
    if (!p.rows.length) p.rows = [{ name: name || '', vals: {} }];
    return p;
  }
  /* V340 — دفترچهٔ مدرسه: خلاصهٔ کلاس‌ها + کارنامهٔ کامل هر کلاس.
     reports آرایه‌ای از خروجی fromManagerClass برای هر کلاس است. */
  function fromManagerSchool(stats, reports) {
    var p = base('school', {});
    p.classes = (stats && stats.classes || []).map(function (k) {
      return {
        id: k.id, name: k.name || '', grade: k.grade || '', field: k.field || '',
        school: k.school || '', teacher: k.teacher || '',
        students: k.students || 0, exams: k.exams || 0, answers: k.answers || 0, graded: k.graded || 0,
        average_percent: k.average_percent == null ? null : Number(k.average_percent),
        spread_percent: k.spread_percent == null ? null : Number(k.spread_percent),
        below_half: k.below_half || 0
      };
    });
    p.months = (stats && stats.months || []).slice();
    p.topExams = (stats && stats.top_exams || []).slice();
    p.reports = reports || [];
    if (p.classes.length && p.classes[0].school) p.school.name = p.classes[0].school;
    return p;
  }

  /* ---- پنجرهٔ انتخاب قالب → پیش‌نمایش → چاپ ---- */
  function close() {
    if (!ctx) return;
    var c = ctx; ctx = null;
    try { c.overlay.remove(); } catch (e) {}
    document.body.style.overflow = '';
    document.body.classList.remove('engine-open');
    window.__reportCardBridge = null;
  }
  function open(payload) {
    close();
    var overlay = el('div', {class: 'engine-bg'});
    var iframe = el('iframe', {title: 'report-card-templates'});
    var wait = el('div', {class: 'engine-wait'}, [el('div', {class: 'engine-wait-spin'}),
      el('div', {class: 'engine-wait-txt', text: 'در حال آماده‌سازی قالب‌های کارنامه…'}),
      el('button', {type: 'button', class: 'btn ghost', text: 'انصراف', onclick: close})]);
    overlay.appendChild(wait);
    document.body.appendChild(overlay);
    document.body.style.overflow = 'hidden';
    document.body.classList.add('engine-open');
    ctx = { overlay: overlay, iframe: iframe, payload: payload };
    window.__reportCardBridge = {
      print: function () {
        try {
          var w = iframe.contentWindow;
          w.focus();
          w.print();
        } catch (e) { S.toast('پنجرهٔ چاپ باز نشد؛ از منوی مرورگر گزینهٔ چاپ را بزنید.', 'err'); }
      },
      close: close
    };
    iframe.addEventListener('load', function () {
      var tries = 0;
      (function push() {
        tries++;
        try {
          var w = iframe.contentWindow;
          if (w && w.ReportCard && typeof w.ReportCard.open === 'function') {
            /* iframe از قبل به overlay اضافه شده؛ appendChild دوباره = بارگذاری مجدد و حلقهٔ بی‌پایان */
            if (wait.parentNode) wait.remove();
            w.ReportCard.open(payload);
            return;
          }
        } catch (e) {}
        if (tries < 200) setTimeout(push, 50);
        else { S.toast('موتور کارنامه آماده نشد.', 'err'); close(); }
      })();
    });
    S.engineHtml('reportcard').then(function (h) {
      if (!h) { close(); return; }
      if (!ctx || ctx.iframe !== iframe) return;
      iframe.srcdoc = h;
      overlay.appendChild(iframe);
    });
  }
  /* اگر موتور در دسترس نبود، همان چاپ سادهٔ قبلی کار کند تا کاربر بی‌خروجی نماند */
  function withFallback(payload, title, node) {
    /* V340 — دفترچهٔ مدرسه ستون/سطر ندارد؛ دادهٔ آن کلاس‌ها و کارنامهٔ کلاس‌هاست */
    var hasData = payload && (payload.scope === 'school'
      ? ((payload.classes && payload.classes.length) || (payload.reports && payload.reports.length))
      : (payload.cols && payload.cols.length && payload.rows && payload.rows.length));
    if (!hasData) {
      if (S.printSection && node) { S.printSection(title, node); return; }
      S.toast('برای این کارنامه داده‌ای پیدا نشد.', 'err');
      return;
    }
    open(payload);
  }
  window.ExamReportCard = {
    open: open, close: close, withFallback: withFallback,
    fromStudentGrades: fromStudentGrades, fromTeacher: fromTeacher, fromManagerClass: fromManagerClass, fromManagerStudent: fromManagerStudent, fromManagerSchool: fromManagerSchool
  };
  S.reportCard = open;
  S.reportCardPayload = { student: fromStudentGrades, teacher: fromTeacher, manager: fromManagerClass };
})();
