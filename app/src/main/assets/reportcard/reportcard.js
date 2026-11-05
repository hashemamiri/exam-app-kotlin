/* V339 — موتور قالب‌های کارنامه (مشترک سایت و اپ).
   یک منبع، دو میزبان: سایت این فایل را از engines.<hash>.js داخل iframe می‌گیرد و
   اپ همان فایل را از assets/reportcard با WebView بارگذاری می‌کند؛ پس خروجی یکسان است.

   قرارداد داده (payload):
   {
     scope: 'student' | 'class',
     school: {name, province, city, district, code, stage},
     year, date, className, grade, field, teacher,
     student: {name, father, no, nationalId},        // فقط scope=student
     cols: [{key, title, subject, max, stats:{questions,correct,wrong,blank}}],
     rows: [{name, vals:{key:number|null}, pcts:{key:number|null}}],
     classInfo: {count, average},                    // زمینهٔ رتبه برای کارنامهٔ تکی
     levels: [85,70,50], feedback, code,
     roles: {examKey:'mostamar'|'payani'|'nobat1'|'nobat2'|'barge'},
     structure: 'matrix' | 'subjects' | 'combined'   // فقط scope=class
   }
   خروجی رندر یک یا چند <section class="page"> روی A4 است. */
(function () {
  'use strict';

  var FA = String.fromCharCode(0x6F0,0x6F1,0x6F2,0x6F3,0x6F4,0x6F5,0x6F6,0x6F7,0x6F8,0x6F9);
  function fa(x) {
    if (x === null || x === undefined || x === '') return '—';
    var s = String(x);
    return s.replace(/[0-9]/g, function (d) { return FA.charAt(+d); }).replace(/\./g, '٫');
  }
  function esc(s) {
    return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  }
  function isNum(v) { return typeof v === 'number' && isFinite(v); }
  function round(v, d) { var k = Math.pow(10, d || 0); return Math.round(v * k) / k; }
  function mean(a) { return a.length ? a.reduce(function (x, y) { return x + y; }, 0) / a.length : 0; }
  function nums(a) { return a.filter(isNum); }

  var LEVELS = ['نیاز به تلاش بیشتر', 'قابل قبول', 'خوب', 'خیلی خوب'];
  var LEVEL_COLORS = ['#dc2626', '#d97706', '#2563eb', '#16a34a'];
  function stars(n) { return '★'.repeat(n) + '☆'.repeat(4 - n); }
  function levelOf(pct, th) {
    var t = th && th.length === 3 ? th : [85, 70, 50];
    return pct >= t[0] ? 4 : pct >= t[1] ? 3 : pct >= t[2] ? 2 : 1;
  }
  function stateOf(pct, th) { return LEVELS[levelOf(pct, th) - 1]; }

  /* ------------------------------------------------------------ داده‌سازی */
  function normalize(p) {
    p = p || {};
    var m = {
      scope: p.scope === 'class' ? 'class' : 'student',
      school: p.school || {},
      year: p.year || '',
      date: p.date || '',
      className: p.className || '',
      grade: p.grade || '',
      field: p.field || '',
      teacher: p.teacher || '',
      student: p.student || {},
      classInfo: p.classInfo || {},
      levels: (p.levels && p.levels.length === 3) ? p.levels : [85, 70, 50],
      feedback: p.feedback || '',
      code: p.code || '',
      roles: p.roles || {},
      structure: p.structure || 'combined',
      attendance: p.attendance || null,
      behavior: p.behavior || null,
      /* V340 — برچسب‌های قابل جایگزینی: برای دفترچهٔ مدرسه، ردیف‌ها «کلاس» هستند نه «دانش‌آموز».
         پیش‌فرض‌ها دقیقاً همان متن قبلی‌اند، پس کارنامهٔ کلاس/دانش‌آموز تغییر نمی‌کند. */
      rowLabel: p.rowLabel || 'دانش‌آموز',
      nameLabel: p.nameLabel || 'نام دانش‌آموز',
      countLabel: p.countLabel || 'تعداد دانش‌آموز',
      bestLabel: p.bestLabel || 'برترین دانش‌آموز',
      groupLabel: p.groupLabel || 'کلاس',
      avgLabel: p.avgLabel || 'میانگین کلاس',
      cols: [], rows: []
    };
    m.school.name = m.school.name || 'مدرسه';
    m.cols = (p.cols || []).map(function (c, i) {
      return {
        key: String(c.key == null ? i : c.key),
        title: c.title || ('آزمون ' + fa(i + 1)),
        subject: c.subject || c.title || ('آزمون ' + fa(i + 1)),
        max: isNum(c.max) && c.max > 0 ? c.max : 20,
        stats: c.stats || null
      };
    });
    m.rows = (p.rows || []).map(function (r) {
      var vals = {}, pcts = {};
      m.cols.forEach(function (c) {
        var v = r.vals ? r.vals[c.key] : null;
        v = isNum(v) ? v : (typeof v === 'string' && v !== '' && isFinite(+v) ? +v : null);
        vals[c.key] = v;
        var pc = r.pcts ? r.pcts[c.key] : null;
        pcts[c.key] = isNum(pc) ? pc : (isNum(v) ? round(v / c.max * 100, 1) : null);
      });
      return { name: r.name || '', vals: vals, pcts: pcts, stats: r.stats || {} };
    });
    m.me = m.rows[0] || { name: m.student.name || '', vals: {}, pcts: {}, stats: {} };
    if (m.scope === 'student' && m.student.name) {
      for (var ri = 0; ri < m.rows.length; ri++) if (m.rows[ri].name === m.student.name) { m.me = m.rows[ri]; break; }
    }
    if (m.scope === 'student' && !m.student.name) m.student.name = m.me.name;
    // رتبه‌ها بر پایهٔ میانگین درصد (نزولی)
    var order = m.rows.map(function (r, i) {
      return { i: i, avg: mean(nums(m.cols.map(function (c) { return r.pcts[c.key]; }))) };
    }).sort(function (a, b) { return b.avg - a.avg; });
    m.rankOf = {};
    order.forEach(function (o, k) { m.rankOf[o.i] = k + 1; });
    m.rows.forEach(function (r, i) {
      r.avgPercent = mean(nums(m.cols.map(function (c) { return r.pcts[c.key]; })));
      r.avgScore = mean(nums(m.cols.map(function (c) { return isNum(r.vals[c.key]) ? r.vals[c.key] / c.max * 20 : null; })));
      r.rank = m.rankOf[i];
    });
    var allPct = [];
    m.rows.forEach(function (r) { m.cols.forEach(function (c) { if (isNum(r.pcts[c.key])) allPct.push(r.pcts[c.key]); }); });
    m.stats = {
      count: m.rows.length,
      avg: round(mean(allPct), 1),
      max: allPct.length ? round(Math.max.apply(null, allPct), 1) : 0,
      min: allPct.length ? round(Math.min.apply(null, allPct), 1) : 0,
      pass: allPct.length ? round(allPct.filter(function (v) { return v >= m.levels[2]; }).length / allPct.length * 100) : 0
    };
    m.subjects = subjectRows(m);
    return m;
  }

  /* سطرهای «درس» برای فرم‌های رسمی: آزمون‌ها بر پایهٔ درس گروه می‌شوند و
     نقش هر آزمون (مستمر/پایانی/نوبت اول/دوم/برگهٔ نهایی) از m.roles می‌آید. */
  function subjectRows(m) {
    var groups = [], index = {};
    m.cols.forEach(function (c) {
      var name = c.subject || c.title;
      if (!(name in index)) { index[name] = groups.length; groups.push({ name: name, cols: [] }); }
      groups[index[name]].cols.push(c);
    });
    groups.forEach(function (g) {
      g.byRole = {};
      g.cols.forEach(function (c) { var r = m.roles[c.key]; if (r) g.byRole[r] = c; });
    });
    return groups;
  }
  /* نمرهٔ یک دانش‌آموز در یک درس با توجه به نقش‌ها و فرمول هر فرم.
     formula: 'nobat' = (مستمر + ۲×پایانی)÷۳ ، 'annual' = (نوبت۱ + ۲×نوبت۲)÷۳ ،
              'final' = (مستمر + ۲×برگه)÷۳ ، 'plain' = میانگین آزمون‌های همان درس */
  function subjectScore(m, g, row, formula, scale) {
    function val(role) { var c = g.byRole[role]; return c ? row.vals[c.key] : null; }
    function norm(role) {
      var c = g.byRole[role];
      if (!c || !isNum(row.vals[c.key])) return null;
      return row.vals[c.key] / c.max * scale;
    }
    var a, b;
    if (formula === 'nobat' || formula === 'final') { a = norm(formula === 'nobat' ? 'mostamar' : 'mostamar'); b = norm(formula === 'nobat' ? 'payani' : 'barge'); }
    else if (formula === 'annual') { a = norm('nobat1'); b = norm('nobat2'); }
    if (isNum(a) && isNum(b)) return round((a + 2 * b) / 3, 1);
    if (isNum(b)) return round(b, 1);
    if (isNum(a)) return round(a, 1);
    var plain = nums(g.cols.map(function (c) { return isNum(row.vals[c.key]) ? row.vals[c.key] / c.max * scale : null; }));
    return plain.length ? round(mean(plain), 1) : null;
  }
  function formulaOf(id) { return id === 6 ? 'nobat' : id === 7 ? 'annual' : id === 8 ? 'final' : 'plain'; }

  /* ------------------------------------------------------------ اجزای مشترک */
  var LOGO = '<div class="brand-mark" style="background:ACC"><svg viewBox="0 0 24 24" width="22" height="22" fill="none" ' +
    'stroke="#fff" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">' +
    '<path d="M4 5.5A1.5 1.5 0 0 1 5.5 4H10v16H5.5A1.5 1.5 0 0 1 4 18.5z"/>' +
    '<path d="M20 5.5A1.5 1.5 0 0 0 18.5 4H14v16h4.5a1.5 1.5 0 0 0 1.5-1.5z"/>' +
    '<path d="M7 9h1.5M7 12h1.5M16 9h1.5M16 12h1.5"/></svg></div>';

  function head(m, title, subtitle, accent) {
    return '<header class="rep-head"><div class="rep-head-right">' + LOGO.replace('ACC', accent) +
      '<div><div class="rep-school">' + esc(m.school.name) + '</div><div class="rep-year">' + esc(m.year) + '</div></div></div>' +
      '<div class="rep-head-left"><div class="rep-title" style="color:' + accent + '">' + esc(title) + '</div>' +
      '<div class="rep-sub">' + esc(subtitle) + '</div></div></header>';
  }
  function infoGrid(items, cols) {
    return '<div class="info-grid" style="grid-template-columns:repeat(' + (cols || 4) + ',1fr)">' +
      items.map(function (it) {
        return '<div class="ig-cell"><span class="ig-k">' + esc(it[0]) + '</span><span class="ig-v">' + esc(it[1] || '—') + '</span></div>';
      }).join('') + '</div>';
  }
  function signatures(labels) {
    return '<div class="sign-row">' + labels.map(function (x) {
      return '<div class="sign-box"><div class="sign-line"></div><div class="sign-lbl">' + esc(x) + '</div></div>';
    }).join('') + '</div>';
  }
  function footer(m) {
    return '<footer class="rep-foot"><span>این کارنامه توسط سامانهٔ آزمون آنلاین onlineexam.ir صادر شده است.</span>' +
      '<span>کد پیگیری: ' + esc(fa(m.code)) + '</span></footer>';
  }
  function govRight(m) {
    var s = m.school, parts = ['وزارت آموزش و پرورش'];
    if (s.province) parts.push('اداره کل استان ' + s.province);
    if (s.district) parts.push('منطقهٔ ' + s.district);
    else if (s.city) parts.push(s.city);
    return parts.map(esc).join('<br>');
  }
  function ofGov(m, title, formNo, extra, stage) {
    return '<div class="of-gov"><div>' + govRight(m) + '</div>' +
      '<div class="of-gov-mid">بسمه تعالی<br><b>' + esc(title) + '</b>' + (extra || '') + '</div>' +
      '<div>' + esc(m.school.name) + (m.school.code ? '<br>کد مدرسه: ' + esc(fa(m.school.code)) : '') +
      '<br>' + esc(m.year) + '</div></div>' +
      '<div class="of-meta"><span>فرم شمار: ' + fa(formNo) + '</span><span>دوره: ' + esc(stage || m.school.stage || 'متوسطهٔ دوم') + '</span>' +
      '<span>نوع: کارنامهٔ رسمی</span><span>تاریخ صدور: ' + esc(m.date) + '</span></div>';
  }
  function ofId(items, cols) {
    return '<div class="of-id" style="grid-template-columns:repeat(' + (cols || 4) + ',1fr)">' +
      items.map(function (it) { return '<div class="of-id-cell"><span>' + esc(it[0]) + '</span><b>' + esc(it[1] || '—') + '</b></div>'; }).join('') + '</div>';
  }
  function ofSign(labels, seal) {
    return '<div class="of-sign">' + labels.map(function (x) {
      return '<div class="of-sign-box"><div class="of-sign-line"></div><div>' + esc(x) + '</div></div>';
    }).join('') + (seal === false ? '' : '<div class="of-sign-box"><div class="seal">مهر<br>مدرسه</div></div>') + '</div>';
  }
  function ofNote(text) { return '<div class="of-note"><b>تبصره:</b> ' + esc(text) + '</div>'; }
  function ofSum(items) {
    return '<div class="of-sum">' + items.map(function (it) {
      return '<div><span>' + esc(it[0]) + '</span><b' + (it[2] ? ' class="ok"' : '') + '>' + esc(it[1]) + '</b></div>';
    }).join('') + '</div>';
  }
  function scopeLabel(m) { return m.scope === 'class' ? (m.groupLabel + ' ' + m.className) : ('کل ' + m.groupLabel); }

  /* ------------------------------------------------------------ جدول‌های تجمیعی */
  function cellOf(m, v) {
    if (!isNum(v)) return '—';
    if (m.metric === 'level') return '<span class="lv">' + stars(Math.max(1, Math.min(4, Math.round(v)))) + '</span>';
    return fa(round(v, 1));
  }
  function tblMatrix(m, rows, cols, rowLabel, compact) {
    var head = '<th style="width:5%">ردیف</th><th>' + esc(rowLabel) + '</th>' +
      cols.map(function (c) { return '<th>' + esc(c.title) + '</th>'; }).join('') + '<th>میانگین</th>';
    var body = rows.map(function (r, i) {
      var vals = cols.map(function (c) { return r.vals[c.key]; });
      var avg = mean(nums(vals));
      return '<tr><td>' + fa(i + 1) + '</td><td class="r nm">' + esc(r.name) + '</td>' +
        vals.map(function (v) { return '<td>' + cellOf(m, v) + '</td>'; }).join('') +
        '<td class="b">' + (isNum(avg) ? fa(round(avg, m.metric === 'level' ? 2 : 1)) : '—') + '</td></tr>';
    }).join('');
    return '<table class="agg-table matrix' + (compact ? ' compact' : '') + '"><thead><tr>' + head + '</tr></thead><tbody>' + body + '</tbody></table>';
  }
  function tblSubjects(m, rows, cols, compact) {
    var isLv = m.metric === 'level';
    var head = '<th>' + esc(m.unit || 'درس') + '</th>' +
      (isLv ? '<th>خیلی خوب</th><th>خوب</th><th>قابل قبول</th><th>نیاز به تلاش</th><th>میانگین سطح</th>'
            : '<th>میانگین نمره</th><th>بیشترین</th><th>کمترین</th><th>نرخ قبولی</th><th>تعداد</th>');
    var body = cols.map(function (c) {
      var vals = nums(rows.map(function (r) { return r.vals[c.key]; }));
      if (!vals.length) return '<tr><td class="r">' + esc(c.title) + '</td><td colspan="5">—</td></tr>';
      if (isLv) {
        var cnt = [1, 2, 3, 4].map(function (L) { return vals.filter(function (v) { return Math.round(v) === L; }).length; });
        return '<tr><td class="r">' + esc(c.title) + '</td>' + cnt.slice().reverse().map(function (n) { return '<td>' + fa(n) + '</td>'; }).join('') +
          '<td class="b">' + fa(round(mean(vals), 2)) + '</td></tr>';
      }
      var pass = round(vals.filter(function (v) { return v >= m.passMark; }).length / vals.length * 100);
      return '<tr><td class="r">' + esc(c.title) + '</td><td class="b">' + fa(round(mean(vals), 1)) + '</td>' +
        '<td>' + fa(round(Math.max.apply(null, vals), 1)) + '</td><td>' + fa(round(Math.min.apply(null, vals), 1)) + '</td>' +
        '<td>' + fa(pass) + '٪</td><td>' + fa(vals.length) + '</td></tr>';
    }).join('');
    return '<table class="agg-table' + (compact ? ' compact' : '') + '"><thead><tr>' + head + '</tr></thead><tbody>' + body + '</tbody></table>';
  }
  function statStrip(m, accent, items) {
    return '<div class="agg-stats" style="--acc:' + accent + '">' + items.map(function (it) {
      return '<div class="agg-stat"><span>' + esc(it[0]) + '</span><b>' + esc(it[1]) + '</b></div>';
    }).join('') + '</div>';
  }
  function aggBlock(m, rows, cols, rowLabel) {
    var s = '<div class="agg-h">تحلیل ' + esc(m.unit || 'درس') + 'ها</div>' + tblSubjects(m, rows, cols, true);
    var mx = '<div class="agg-h">ماتریس نمرات</div>' + tblMatrix(m, rows, cols, rowLabel, true);
    if (m.structure === 'subjects') return s;
    if (m.structure === 'matrix') return mx;
    return s + mx;
  }
  function aggStats(m, rows, cols, accent) {
    var all = [];
    rows.forEach(function (r) { cols.forEach(function (c) { if (isNum(r.vals[c.key])) all.push(r.vals[c.key]); }); });
    var isLv = m.metric === 'level';
    var best = rows.slice().sort(function (a, b) {
      return mean(nums(cols.map(function (c) { return b.vals[c.key]; }))) - mean(nums(cols.map(function (c) { return a.vals[c.key]; })));
    })[0];
    return statStrip(m, accent, isLv ? [
      ['میانگین سطح', fa(round(mean(all), 2))],
      ['نمرهٔ خیلی خوب', fa(all.filter(function (v) { return Math.round(v) === 4; }).length)],
      ['نمرهٔ نیاز به تلاش', fa(all.filter(function (v) { return Math.round(v) === 1; }).length)],
      [m.countLabel, fa(rows.length)]
    ] : [
      ['میانگین کل', fa(round(mean(all), 1))],
      ['نرخ قبولی درس‌ها', fa(all.length ? round(all.filter(function (v) { return v >= m.passMark; }).length / all.length * 100) : 0) + '٪'],
      [m.bestLabel, best ? best.name : '—'],
      ['تعداد ' + (m.unit || 'درس'), fa(cols.length)]
    ]);
  }
  /* برگهٔ تجمیعی مشترک (کلاس) — با پوستهٔ رسمی یا مدرن */
  function classPage(m, cfg) {
    var cols = m.cols, rows = m.rows;
    var scale = m.metric === 'level' ? 4 : m.metric === 'percent' ? 100 : 20;
    if (cfg.official && m.subjects.length) {
      cols = m.subjects.map(function (g, i) { return { key: 'g' + i, title: g.name, max: 20, group: g }; });
      rows = m.rows.map(function (r) {
        var vals = {}, pcts = {};
        cols.forEach(function (c) {
          var v = subjectScore(m, c.group, r, formulaOf(cfg.id), m.metric === 'level' ? 4 : 20);
          vals[c.key] = v; pcts[c.key] = isNum(v) ? v / (m.metric === 'level' ? 4 : 20) * 100 : null;
        });
        return { name: r.name, vals: vals, pcts: pcts };
      });
    }
    if (!cfg.official || !m.subjects.length) {
      rows = m.rows.map(function (r) {
        var vals = {}, pcts = {};
        cols.forEach(function (c) {
          var v = r.vals[c.key];
          vals[c.key] = isNum(v) ? (m.metric === 'level' ? levelOf(v / c.max * 100, m.levels) : round(v / c.max * scale, 1)) : null;
          pcts[c.key] = r.pcts[c.key];
        });
        return { name: r.name, vals: vals, pcts: pcts, stats: r.stats };
      });
    }
    if (m._cont) return (cfg.official ? ofGov(m, cfg.title + ' — ' + m.groupLabel, cfg.form, '', cfg.stage) : head(m, cfg.title + ' — ' + m.groupLabel, scopeLabel(m), cfg.accent)) +
      '<div class="agg-h">ماتریس نمرات</div>' + tblMatrix(m, rows, cols, m.rowLabel, true) + footer(m);
    var body = aggStats(m, rows, cols, cfg.accent) + aggBlock(m, rows, cols, m.rowLabel);
    var note = '<div class="agg-note" style="border-color:' + cfg.accent + '"><b>یادداشت:</b> این کارنامهٔ تجمیعی کلاس است؛ ' +
      'میانگین‌ها از نمرهٔ واقعی همین آزمون‌ها محاسبه شده و «نرخ قبولی» سهم نمره‌های بالای حدنصاب است.</div>';
    if (cfg.official) {
      return ofGov(m, cfg.title + ' — ' + m.groupLabel, cfg.form, '<br><small>' + esc(scopeLabel(m)) + '</small>', cfg.stage) +
        ofId([['مدرسه', m.school.name], ['پایه', m.grade], [m.groupLabel, m.className], [m.countLabel, fa(m.rows.length)],
              ['تعداد آزمون', fa(m.cols.length)], ['سال تحصیلی', m.year], ['تاریخ صدور', m.date], ['ساختار جدول', structName(m.structure)]]) +
        body + ofNote(cfg.note || 'میانگین‌ها از نمرات واقعی همان بازه محاسبه شده است.') +
        ofSign(['معاون آموزشی', 'مدیر مدرسه', 'کارشناس سنجش']) + footer(m);
    }
    return head(m, cfg.title + ' — ' + m.groupLabel, scopeLabel(m) + ' · ' + m.year, cfg.accent) +
      infoGrid([['پایه', m.grade], [m.groupLabel, m.className], [m.countLabel, fa(m.rows.length)], ['سال تحصیلی', m.year]]) +
      body + note + footer(m);
  }
  function structName(s) { return s === 'matrix' ? 'ماتریس نمرات' : s === 'subjects' ? 'تحلیل درس‌ها' : 'ترکیبی'; }

  /* ============================================================ قالب ۱ — رسمی اداری */
  function tpl1(m) {
    if (m.scope === 'class') return classPage(m, CFG[0]);
    var rows = '', total = 0, maxTotal = 0;
    (m._allCols || m.cols).forEach(function (c) {
      var v = m.me.vals[c.key];
      maxTotal += c.max; if (isNum(v)) total += v;
    });
    m.cols.forEach(function (c, i) {
      var v = m.me.vals[c.key], pct = m.me.pcts[c.key];
      rows += '<tr><td>' + fa(i + 1) + '</td><td class="r">' + esc(c.title) + '</td><td>' + fa(c.max) + '</td>' +
        '<td class="b">' + (isNum(v) ? fa(round(v, 2)) : '—') + '</td>' +
        '<td>' + (isNum(pct) ? fa(Math.round(pct)) + '٪' : '—') + '</td>' +
        '<td>' + (isNum(pct) ? esc(stateOf(pct, m.levels)) : 'غایب') + '</td></tr>';
    });
    var pct = maxTotal ? round(total / maxTotal * 100) : 0;
    return '<div class="t1-frame">' +
      '<div class="t1-gov"><div>' + govRight(m) + '</div>' +
      '<div class="t1-gov-mid">بسمه تعالی<br><b>کارنامهٔ رسمی آزمون</b></div>' +
      '<div>' + esc(m.school.name) + '<br>' + esc(m.year) + '</div></div>' +
      infoGrid([['نام و نام خانوادگی', m.student.name], ['پایه و کلاس', (m.grade ? m.grade + ' — ' : '') + m.className],
                ['تعداد آزمون', fa(m.cols.length)], ['تاریخ صدور', m.date],
                ['دبیر', m.teacher], ['سال تحصیلی', m.year], [m.avgLabel, isNum(m.classInfo.average) ? fa(round(m.classInfo.average, 1)) + '٪' : '—'],
                ['کد پیگیری', fa(m.code)]]) +
      '<table class="grid"><thead><tr><th style="width:8%">ردیف</th><th>عنوان آزمون / مبحث</th><th style="width:12%">بارم</th>' +
      '<th style="width:14%">نمره</th><th style="width:12%">درصد</th><th style="width:18%">وضعیت</th></tr></thead>' +
      '<tbody>' + rows + '</tbody><tfoot><tr><td colspan="2" class="b">جمع کل</td><td class="b">' + fa(round(maxTotal, 1)) + '</td>' +
      '<td class="b">' + fa(round(total, 2)) + '</td><td class="b">' + fa(pct) + '٪</td><td class="b">' + esc(stateOf(pct, m.levels)) + '</td></tr></tfoot></table>' +
      '<div class="t1-summary"><div><span>نمرهٔ کل</span><b>' + fa(round(total, 2)) + ' از ' + fa(round(maxTotal, 1)) + '</b></div>' +
      '<div><span>درصد کل</span><b>' + fa(pct) + '٪</b></div>' +
      '<div><span>رتبه در کلاس</span><b>' + (m.me.rank ? fa(m.me.rank) + ' از ' + fa(m.classInfo.count || m.rows.length) : '—') + '</b></div>' +
      '<div><span>میانگین کلاس</span><b>' + (isNum(m.classInfo.average) ? fa(round(m.classInfo.average, 1)) + '٪' : '—') + '</b></div></div>' +
      (m.feedback ? '<div class="t1-note"><b>بازخورد دبیر:</b> ' + esc(m.feedback) + '</div>' : '') +
      signatures(['امضای معلم', 'امضای مدیر', 'امضای ولی']) + footer(m) + '</div>';
  }

  /* ============================================================ قالب ۲ — مدرن رنگی */
  function bars(m, items) {
    return items.map(function (it) {
      var pct = isNum(it.pct) ? Math.max(0, Math.min(100, Math.round(it.pct))) : 0;
      var color = pct >= m.levels[0] ? '#16a34a' : pct >= m.levels[1] ? '#2563eb' : pct >= m.levels[2] ? '#d97706' : '#dc2626';
      return '<div class="bar-row"><div class="bar-lbl">' + esc(it.title) + '</div>' +
        '<div class="bar-track"><div class="bar-fill" style="width:' + pct + '%;background:' + color + '"></div></div>' +
        '<div class="bar-val">' + (isNum(it.pct) ? fa(pct) + '٪' : '—') + '</div></div>';
    }).join('');
  }
  function tpl2(m) {
    var isClass = m.scope === 'class';
    var items = m.cols.map(function (c) {
      return { title: c.title, pct: isClass ? mean(nums(m.rows.map(function (r) { return r.pcts[c.key]; }))) : m.me.pcts[c.key] };
    });
    var avg = isClass ? m.stats.avg : m.me.avgPercent;
    var strip = '<div class="stat-strip">' +
      '<div class="stat hi"><span>' + (isClass ? m.avgLabel : 'درصد کل') + '</span><b>' + fa(round(avg, 1)) + '٪</b><small>عملکرد کلی</small></div>' +
      '<div class="stat"><span>بیشترین</span><b>' + fa(isClass ? m.stats.max : round(Math.max.apply(null, nums(m.cols.map(function (c) { return m.me.pcts[c.key]; })).concat([0])), 1)) + '٪</b><small>بهترین عملکرد</small></div>' +
      '<div class="stat"><span>' + (isClass ? 'نرخ قبولی' : 'رتبه در کلاس') + '</span><b>' +
        (isClass ? fa(m.stats.pass) + '٪' : (m.me.rank ? fa(m.me.rank) : '—')) + '</b><small>' +
        (isClass ? 'نمره‌های بالای حدنصاب' : 'از ' + fa(m.classInfo.count || m.rows.length) + ' ' + m.rowLabel) + '</small></div>' +
      '<div class="stat"><span>' + (isClass ? m.countLabel : m.avgLabel) + '</span><b>' +
        (isClass ? fa(m.rows.length) : (isNum(m.classInfo.average) ? fa(round(m.classInfo.average, 1)) + '٪' : '—')) + '</b><small>' +
        (isClass ? 'در این کارنامه' : 'برای مقایسه') + '</small></div></div>';
    var qrows = m.cols.map(function (c, i) {
      var v = isClass ? mean(nums(m.rows.map(function (r) { return r.vals[c.key]; }))) : m.me.vals[c.key];
      return '<tr><td>' + fa(i + 1) + '</td><td class="r">' + esc(c.title) + '</td><td>' + esc(c.subject) + '</td>' +
        '<td>' + fa(c.max) + '</td><td class="b">' + (isNum(v) ? fa(round(v, 2)) : '—') + '</td></tr>';
    }).join('');
    return head(m, 'کارنامهٔ آزمون' + (isClass ? ' — ' + m.groupLabel : ''), (isClass ? scopeLabel(m) : m.student.name) + ' · ' + m.date, '#4338ca') +
      infoGrid(isClass ? [[m.groupLabel, m.className], ['پایه', m.grade], ['تعداد آزمون', fa(m.cols.length)], [m.countLabel, fa(m.rows.length)]]
                       : [['دانش‌آموز', m.student.name], [m.groupLabel, m.className], ['دبیر', m.teacher], ['کد پیگیری', fa(m.code)]]) +
      strip +
      '<div class="t2-card"><div class="t2-h">' + (isClass ? 'میانگین کلاس به تفکیک آزمون' : 'عملکرد به تفکیک آزمون') + '</div>' + bars(m, items) + '</div>' +
      (isClass ? '' : '<div class="t2-card"><div class="t2-h">ریز نمرات</div><table class="mini">' +
      '<thead><tr><th style="width:8%">#</th><th>عنوان</th><th style="width:20%">درس</th><th style="width:12%">بارم</th><th style="width:12%">نمره</th></tr></thead>' +
      '<tbody>' + qrows + '</tbody></table></div>') +
      (isClass ? classTableOnly(m) : '') +
      (m.feedback ? '<div class="t2-feedback"><b>بازخورد دبیر:</b> ' + esc(m.feedback) + '</div>' : '') + footer(m);
  }

  /* ============================================================ قالب ۳ — روند پیشرفت */
  function trendSvg(m, vals, labels, accent, h) {
    var w = 660, pad = 34, hh = h || 190, n = vals.length;
    if (!n) return '';
    var xs = vals.map(function (_, i) { return n === 1 ? w / 2 : w - pad - i * (w - 2 * pad) / (n - 1); });
    var ys = vals.map(function (v) { return hh - pad - (Math.max(0, Math.min(100, v)) / 100) * (hh - 2 * pad); });
    var grid = [0, 25, 50, 75, 100].map(function (v) {
      var y = hh - pad - v / 100 * (hh - 2 * pad);
      return '<line x1="' + pad + '" y1="' + y.toFixed(1) + '" x2="' + (w - pad) + '" y2="' + y.toFixed(1) + '" stroke="#e5e7eb" stroke-width="1"/>';
    }).join('');
    var pts = xs.map(function (x, i) { return x.toFixed(1) + ',' + ys[i].toFixed(1); }).join(' ');
    var dots = xs.map(function (x, i) {
      return '<circle cx="' + x.toFixed(1) + '" cy="' + ys[i].toFixed(1) + '" r="4.5" fill="' + accent + '"/>' +
        '<text x="' + x.toFixed(1) + '" y="' + (ys[i] - 11).toFixed(1) + '" font-size="11" font-weight="700" fill="' + accent + '" text-anchor="middle">' + fa(Math.round(vals[i])) + '٪</text>';
    }).join('');
    var lbl = xs.map(function (x, i) {
      var t = String(labels[i] || ''); if (t.length > 16) t = t.slice(0, 15) + '…';
      return '<text x="' + x.toFixed(1) + '" y="' + (hh - pad + 16) + '" font-size="9.5" fill="#64748b" text-anchor="middle">' + esc(t) + '</text>';
    }).join('');
    return '<svg viewBox="0 0 ' + w + ' ' + hh + '" class="chart" role="img" aria-label="نمودار روند">' + grid +
      '<polyline points="' + pts + '" fill="none" stroke="' + accent + '" stroke-width="2.5" stroke-linejoin="round"/>' + dots + lbl + '</svg>';
  }
  function tpl3(m) {
    var isClass = m.scope === 'class';
    var vals = m.cols.map(function (c) { return isClass ? mean(nums(m.rows.map(function (r) { return r.pcts[c.key]; }))) : m.me.pcts[c.key]; });
    var allVals = (m._allCols || m.cols).map(function (c) { return isClass ? mean(nums(m.rows.map(function (r) { return r.pcts[c.key]; }))) : m.me.pcts[c.key]; });
    var rows = '', prev = null;
    m.cols.forEach(function (c, i) {
      var v = vals[i], trend;
      if (!isNum(v)) trend = '<span class="tr-flat">—</span>';
      else if (prev === null) trend = '<span class="tr-flat">—</span>';
      else if (v > prev) trend = '<span class="tr-up">▲ ' + fa(Math.round(v - prev)) + '٪</span>';
      else if (v < prev) trend = '<span class="tr-down">▼ ' + fa(Math.round(prev - v)) + '٪</span>';
      else trend = '<span class="tr-flat">ثابت</span>';
      if (isNum(v)) prev = v;
      var score = isClass ? mean(nums(m.rows.map(function (r) { return r.vals[c.key]; }))) : m.me.vals[c.key];
      rows += '<tr><td>' + fa(i + 1) + '</td><td class="r">' + esc(c.title) + '</td><td class="b">' + (isNum(score) ? fa(round(score, 2)) : '—') + '</td>' +
        '<td>' + (isNum(v) ? fa(Math.round(v)) + '٪' : '—') + '</td><td>' + fa(c.max) + '</td><td>' + trend + '</td></tr>';
    });
    var ok = nums(allVals), first = ok[0], last = ok[ok.length - 1];
    var growth = ok.length > 1 ? round(last - first, 1) : 0;
    return head(m, 'کارنامهٔ روند پیشرفت' + (isClass ? ' — ' + m.groupLabel : ''), (isClass ? scopeLabel(m) : m.student.name) + ' · ' + m.year, '#0d9488') +
      infoGrid(isClass ? [[m.groupLabel, m.className], ['پایه', m.grade], ['تعداد آزمون', fa(m.cols.length)], [m.countLabel, fa(m.rows.length)]]
                       : [['دانش‌آموز', m.student.name], [m.groupLabel, m.className], ['دبیر', m.teacher], ['تعداد آزمون', fa(m.cols.length)]]) +
      '<div class="stat-strip t3-stats">' +
      '<div class="stat hi"><span>میانگین کل</span><b>' + fa(round(mean(ok), 1)) + '٪</b><small>همهٔ آزمون‌ها</small></div>' +
      '<div class="stat"><span>بهترین آزمون</span><b>' + (ok.length ? fa(round(Math.max.apply(null, ok), 1)) + '٪' : '—') + '</b><small>بیشترین درصد</small></div>' +
      '<div class="stat"><span>تغییر از آغاز</span><b>' + (growth > 0 ? '+' : '') + fa(growth) + '٪</b><small>' + fa(round(first || 0, 1)) + ' ← ' + fa(round(last || 0, 1)) + '</small></div>' +
      '<div class="stat"><span>' + (isClass ? 'نرخ قبولی' : 'رتبه در کلاس') + '</span><b>' + (isClass ? fa(m.stats.pass) + '٪' : (m.me.rank ? fa(m.me.rank) : '—')) + '</b><small>' +
      (isClass ? 'نمره‌های بالای حدنصاب' : 'از ' + fa(m.classInfo.count || m.rows.length)) + '</small></div></div>' +
      '<div class="t3-card"><div class="t3-h">' + (isClass ? 'نمودار میانگین کلاس' : 'نمودار روند نمره') + ' (قدیمی‌ترین سمت راست)</div>' +
      trendSvg(m, vals.map(function (v) { return isNum(v) ? v : 0; }), m.cols.map(function (c) { return c.title; }), '#0d9488', isClass ? 140 : 190) + '</div>' +
      (isClass ? '' : '<table class="grid t3-grid"><thead><tr><th style="width:8%">#</th><th>عنوان آزمون</th><th style="width:12%">نمره</th>' +
      '<th style="width:12%">درصد</th><th style="width:12%">بارم</th><th style="width:16%">تغییر</th></tr></thead><tbody>' + rows + '</tbody></table>') +
      (isClass ? classTableOnly(m) : '') +
      (m.feedback ? '<div class="t3-sum"><b>جمع‌بندی دبیر:</b> ' + esc(m.feedback) + '</div>' : '') +
      signatures(['امضای دبیر', 'امضای مدیر', 'امضای ولی']) + footer(m);
  }

  /* برای قالب‌هایی که خودشان نمودار دارند: فقط یک جدول، بر پایهٔ ساختار انتخابی */
  function classTableOnly(m) {
    if (m.structure === 'subjects') return '<div class="agg-h">تحلیل ' + esc(m.unit || 'درس') + 'ها</div>' + tblSubjects(m, m.rows, m.cols, true);
    return '<div class="agg-h">ماتریس نمرات</div>' + tblMatrix(m, m.rows, m.cols, m.rowLabel, true);
  }

  /* ============================================================ قالب ۴ — کارنامهٔ کلاس / رتبه */
  function histogram(m, vals, highlight) {
    var edges = [0, 25, 50, 75, 90, 100];
    var labels = ['۰ تا ۲۵٪', '۲۵ تا ۵۰٪', '۵۰ تا ۷۵٪', '۷۵ تا ۹۰٪', '۹۰ تا ۱۰۰٪'];
    var buckets = [0, 0, 0, 0, 0];
    vals.forEach(function (v) {
      for (var i = 0; i < 5; i++) if (v >= edges[i] && (v < edges[i + 1] || i === 4)) { buckets[i]++; break; }
    });
    var top = Math.max.apply(null, buckets.concat([1]));
    var bw = 74, gap = 22, maxh = 110, bars = '';
    buckets.forEach(function (cnt, i) {
      var bh = cnt / top * maxh, x = 30 + i * (bw + gap);
      var mine = highlight != null && highlight >= edges[i] && (highlight < edges[i + 1] || i === 4);
      bars += '<rect x="' + x + '" y="' + (maxh + 20 - bh).toFixed(0) + '" width="' + bw + '" height="' + bh.toFixed(0) + '" rx="6" fill="' +
        (mine ? '#f59e0b' : '#2563eb') + '" opacity="' + (mine ? 1 : (0.45 + i * 0.13).toFixed(2)) + '"/>' +
        '<text x="' + (x + bw / 2) + '" y="' + (maxh + 12 - bh).toFixed(0) + '" font-size="12" font-weight="700" fill="#1e3a8a" text-anchor="middle">' + fa(cnt) + '</text>' +
        '<text x="' + (x + bw / 2) + '" y="' + (maxh + 38) + '" font-size="10" fill="#64748b" text-anchor="middle">' + labels[i] + '</text>';
    });
    return '<svg viewBox="0 0 520 165" class="chart" role="img" aria-label="نمودار توزیع نمرات">' + bars + '</svg>';
  }
  function tpl4(m) {
    var isClass = m.scope === 'class';
    var ranked = m.rows.slice().sort(function (a, b) { return (b.avgPercent || 0) - (a.avgPercent || 0); });
    var rows = '', list = isClass ? ranked : ranked;
    list.forEach(function (r, i) {
      var pct = r.avgPercent, ok = pct >= m.levels[2];
      var medal = i === 0 ? ' 🥇' : i === 1 ? ' 🥈' : i === 2 ? ' 🥉' : '';
      var mine = !isClass && r.name === m.me.name;
      rows += '<tr' + (mine ? ' style="background:#fff7ed"' : '') + '><td>' + fa((m._offset || 0) + i + 1) + medal + '</td><td class="r">' + esc(r.name) + (mine ? ' ←' : '') + '</td>' +
        '<td class="b">' + fa(round(r.avgScore, 2)) + '</td><td>' + fa(round(pct, 1)) + '٪</td>' +
        '<td><span class="pill ' + (ok ? 'ok' : 'bad') + '">' + (ok ? 'قبول' : 'نیاز به تلاش') + '</span></td>' +
        '<td>' + (ok ? '—' : 'کلاس جبرانی') + '</td></tr>';
    });
    var vals = nums(m.rows.map(function (r) { return r.avgPercent; }));
    var avg = mean(vals), passCount = vals.filter(function (v) { return v >= m.levels[2]; }).length;
    var best = ranked[0], worst = ranked[ranked.length - 1];
    if (m._cont) return head(m, 'کارنامهٔ ' + m.groupLabel + ' — ادامه', scopeLabel(m), '#1d4ed8') +
      '<table class="grid t4-grid"><thead><tr><th style="width:11%">رتبه</th><th>' + m.nameLabel + '</th><th style="width:12%">میانگین (۲۰)</th>' +
      '<th style="width:11%">درصد</th><th style="width:17%">وضعیت</th><th style="width:14%">توضیح</th></tr></thead><tbody>' + rows + '</tbody></table>' + footer(m);
    return head(m, isClass ? 'کارنامهٔ ' + m.groupLabel : 'کارنامهٔ رتبه و توزیع', scopeLabel(m) + ' · ' + m.date, '#1d4ed8') +
      infoGrid([[m.groupLabel, m.className], ['دبیر', m.teacher], [m.countLabel, fa(m.rows.length)], ['تعداد آزمون', fa(m.cols.length)]]) +
      '<div class="stat-strip t4-stats">' +
      '<div class="stat hi"><span>میانگین کلاس</span><b>' + fa(round(avg, 1)) + '٪</b><small>' + fa(round(avg / 100 * 20, 1)) + ' از ۲۰</small></div>' +
      '<div class="stat"><span>بیشترین</span><b>' + (best ? fa(round(best.avgPercent, 1)) + '٪' : '—') + '</b><small>' + esc(best ? best.name : '') + '</small></div>' +
      '<div class="stat"><span>کمترین</span><b>' + (worst ? fa(round(worst.avgPercent, 1)) + '٪' : '—') + '</b><small>' + esc(worst ? worst.name : '') + '</small></div>' +
      '<div class="stat"><span>نرخ قبولی</span><b>' + fa(vals.length ? round(passCount / vals.length * 100) : 0) + '٪</b><small>' + fa(passCount) + ' از ' + fa(vals.length) + '</small></div></div>' +
      '<table class="grid t4-grid"><thead><tr><th style="width:11%">رتبه</th><th>' + m.nameLabel + '</th><th style="width:12%">میانگین (۲۰)</th>' +
      '<th style="width:11%">درصد</th><th style="width:17%">وضعیت</th><th style="width:14%">توضیح</th></tr></thead><tbody>' + rows + '</tbody></table>' +
      '<div class="t4-card"><div class="t4-h">توزیع نمرات ' + m.groupLabel + (isClass ? '' : ' — جایگاه شما با رنگ نارنجی') + '</div>' +
      histogram(m, vals, isClass ? null : m.me.avgPercent) + '</div>' + footer(m);
  }

  /* ============================================================ قالب ۵ — توصیفی ابتدایی */
  function tpl5(m) {
    if (m.scope === 'class') return classPage(m, CFG[4]);
    var cards = '', good = 0;
    m.cols.forEach(function (c) {
      var pct = m.me.pcts[c.key], L = isNum(pct) ? levelOf(pct, m.levels) : 1;
      if (L >= 3) good++;
      var color = LEVEL_COLORS[L - 1];
      cards += '<div class="p-card" style="border-color:' + color + '33"><div class="p-sub">' + esc(c.subject || c.title) + '</div>' +
        '<div class="p-stars" style="color:' + color + '">' + stars(L) + '</div>' +
        '<div class="p-lvl" style="background:' + color + '15;color:' + color + '">' + LEVELS[L - 1] + '</div></div>';
    });
    var legend = [3, 2, 1, 0].map(function (i) {
      var d = i === 3 ? fa(m.levels[0]) + '٪ و بالاتر' : i === 2 ? fa(m.levels[1]) + ' تا ' + fa(m.levels[0]) + '٪'
            : i === 1 ? fa(m.levels[2]) + ' تا ' + fa(m.levels[1]) + '٪' : 'کمتر از ' + fa(m.levels[2]) + '٪';
      return '<div class="lg-item"><span class="lg-stars" style="color:' + LEVEL_COLORS[i] + '">' + stars(i + 1) + '</span>' +
        '<span>' + LEVELS[i] + '</span><small>' + d + '</small></div>';
    }).join('');
    return head(m, 'کارنامهٔ توصیفی', 'دورهٔ ابتدایی · ' + m.year, '#ea580c') +
      infoGrid([['دانش‌آموز', m.student.name], ['پایه و کلاس', (m.grade ? m.grade + ' — ' : '') + m.className],
                ['آموزگار', m.teacher], ['مدرسه', m.school.name]]) +
      '<div class="t5-banner"><div class="t5-banner-t">آفرین! تلاش تو در این نوبت دیده شد 🌟</div>' +
      '<div class="t5-banner-s">در ' + fa(good) + ' درس از ' + fa(m.cols.length) + ' درس به سطح «خیلی خوب» یا «خوب» رسیدی.</div></div>' +
      '<div class="p-grid">' + cards + '</div>' +
      '<div class="t5-legend"><div class="t5-h">راهنمای سطح‌ها</div><div class="lg-row">' + legend + '</div></div>' +
      (m.feedback ? '<div class="t5-note"><b>توضیح آموزگار:</b> ' + esc(m.feedback) + '</div>' : '') +
      signatures(['امضای آموزگار', 'امضای مدیر', 'امضای ولی']) + footer(m);
  }

  /* ============================================================ قالب ۶ — فرم ۱۰۱ نوبت اول */
  function officialStudent(m, cfg) {
    var scale = cfg.metric === 'level' ? 4 : 20;
    var formula = formulaOf(cfg.id);
    var roleA = cfg.id === 7 ? 'nobat1' : 'mostamar';
    var roleB = cfg.id === 7 ? 'nobat2' : cfg.id === 8 ? 'barge' : 'payani';
    var rows = '', vals = [], fail = 0;
    (m._allSubjects || m.subjects).forEach(function (g) {
      var v0 = subjectScore(m, g, m.me, formula, scale);
      var b0 = g.byRole[roleB];
      var nb0 = b0 && isNum(m.me.vals[b0.key]) ? m.me.vals[b0.key] / b0.max * scale : null;
      var ok0 = isNum(v0) && v0 >= (cfg.metric === 'level' ? 2 : 10) && (cfg.id !== 8 || !isNum(nb0) || nb0 >= 7);
      if (isNum(v0)) vals.push(v0);
      if (!ok0) fail++;
    });
    m.subjects.forEach(function (g, i) {
      var ca = g.byRole[roleA], cb = g.byRole[roleB];
      var a = ca ? m.me.vals[ca.key] : null, b = cb ? m.me.vals[cb.key] : null;
      var na = isNum(a) && ca ? a / ca.max * scale : null, nb = isNum(b) && cb ? b / cb.max * scale : null;
      var v = subjectScore(m, g, m.me, formula, scale);
      var ok = isNum(v) && v >= (cfg.metric === 'level' ? 2 : 10) && (cfg.id !== 8 || !isNum(nb) || nb >= 7);
      rows += '<tr><td>' + fa(i + 1) + '</td><td class="r">' + esc(g.name) + '</td>' +
        '<td>' + (isNum(na) ? fa(round(na, cfg.id === 8 ? 2 : 1)) : '—') + '</td>' +
        '<td>' + (isNum(nb) ? fa(round(nb, cfg.id === 8 ? 2 : 1)) : '—') + '</td>' +
        '<td class="b">' + (isNum(v) ? fa(v) : '—') + '</td>' +
        '<td>' + (isNum(v) ? (ok ? 'قبول' : (cfg.id === 7 ? 'تک‌ماده' : 'نیازمند تلاش')) : 'غایب') + '</td></tr>';
    });
    var avg = vals.length ? round(mean(vals), 2) : 0;
    var passCount = vals.length - fail;
    var subjectCount = (m._allSubjects || m.subjects).length;
    var sums = {
      6: [['معدل نوبت اول', fa(avg)], ['تعداد دروس', fa(subjectCount)], ['دروس زیر ۱۰', fa(fail)], ['تعداد آزمون', fa(m.cols.length)]],
      7: [['معدل کل سالانه', fa(avg)], ['دروس قبول', fa(passCount) + ' از ' + fa(subjectCount)],
          ['وضعیت نهایی', fail ? 'نیازمند بررسی شورای مدرسه' : 'قبول — ارتقا به پایهٔ بالاتر', !fail], ['رتبه در کلاس', m.me.rank ? fa(m.me.rank) : '—']],
      8: [['معدل کل نهایی', fa(avg)], ['دروس قبول', fa(passCount) + ' از ' + fa(subjectCount)],
          ['کمترین نمرهٔ برگه', (function () { var b = nums(m.subjects.map(function (g) { var c = g.byRole.barge; return c && isNum(m.me.vals[c.key]) ? m.me.vals[c.key] / c.max * 20 : null; })); return b.length ? fa(round(Math.min.apply(null, b), 2)) : '—'; })()],
          ['وضعیت', fail ? 'نیازمند بررسی' : 'احراز شده', !fail]]
    }[cfg.id];
    return '<div class="of-frame">' + ofGov(m, cfg.title, cfg.form, cfg.extra || '', cfg.stage) +
      ofId([['نام و نام خانوادگی', m.student.name], ['نام پدر', m.student.father],
            ['شمارهٔ دانش‌آموزی', fa(m.student.no)], ['کد ملی', fa(m.student.nationalId)],
            ['پایه', m.grade], ['رشته', m.field], [m.groupLabel, m.className], ['سال تحصیلی', m.year]]) +
      '<table class="of-table"><thead><tr><th style="width:7%">ردیف</th><th>نام درس</th><th style="width:14%">' + cfg.colA + '</th>' +
      '<th style="width:15%">' + cfg.colB + '</th><th style="width:14%">' + cfg.colC + '</th><th style="width:16%">وضعیت</th></tr></thead>' +
      '<tbody>' + rows + '</tbody></table>' + ofSum(sums) + ofNote(cfg.note) +
      (cfg.id === 6 ? '<div class="of-parent"><b>نظر ولی:</b> ....................................................................................................<span>امضای ولی: ..................</span></div>' : '') +
      (cfg.id === 8 ? '<div class="of-cert">گواهی می‌شود نمرات فوق از آزمون‌های ثبت‌شده در سامانه استخراج شده است؛ اعتبار نهایی این فرم منوط به مهر و امضای مدرسه و ادارهٔ منطقه است.</div>' : '') +
      ofSign(cfg.signs) + footer(m) + '</div>';
  }

  /* ============================================================ قالب ۹ — فرم ۱۰۴ توصیفی رسمی */
  function tpl9(m) {
    if (m.scope === 'class') return classPage(m, CFG[8]);
    var rows = '';
    m.subjects.forEach(function (g, i) {
      var c1 = g.byRole.nobat1, c2 = g.byRole.nobat2;
      var p1 = c1 ? m.me.pcts[c1.key] : null, p2 = c2 ? m.me.pcts[c2.key] : null;
      if (!isNum(p1) && !isNum(p2)) { var pl = nums(g.cols.map(function (c) { return m.me.pcts[c.key]; })); p1 = pl.length ? pl[0] : null; p2 = pl.length > 1 ? pl[pl.length - 1] : p1; }
      var a = isNum(p1) ? levelOf(p1, m.levels) : null, b = isNum(p2) ? levelOf(p2, m.levels) : null;
      rows += '<tr><td>' + fa(i + 1) + '</td><td class="r">' + esc(g.name) + '</td>' +
        '<td>' + (a ? LEVELS[a - 1] : '—') + '</td><td>' + (b ? LEVELS[b - 1] : '—') + '</td>' +
        '<td>' + (a && b ? (b > a ? 'پیشرفت' : b === a ? 'تثبیت' : 'افت نسبی') : '—') + '</td></tr>';
    });
    var beh = (m.behavior && m.behavior.length) ? m.behavior : null;
    var att = m.attendance;
    var two = '';
    if (beh || att) {
      two = '<div class="of-two">' +
        (beh ? '<table class="of-table of-mini"><thead><tr><th>ارزشیابی رفتاری</th><th style="width:34%">درجه</th></tr></thead><tbody>' +
          beh.map(function (b) { return '<tr><td class="r">' + esc(b[0]) + '</td><td>' + esc(b[1]) + '</td></tr>'; }).join('') + '</tbody></table>' : '<div></div>') +
        (att ? '<div class="of-attend"><div class="of-attend-h">حضور و غیاب</div>' +
          [['روزهای سال تحصیلی', att.days], ['غیبت موجه', att.absent], ['غیبت غیرموجه', att.unjustified], ['تأخیر ورود', att.late]]
            .map(function (r) { return '<div><span>' + r[0] + '</span><b>' + fa(r[1]) + '</b></div>'; }).join('') + '</div>' : '') + '</div>';
    }
    return '<div class="of-frame">' +
      ofGov(m, 'کارنامهٔ رسمی توصیفی', 104, '<br><small>دورهٔ ابتدایی — ارزشیابی سالانه</small>', 'ابتدایی') +
      ofId([['نام و نام خانوادگی', m.student.name], ['نام پدر', m.student.father],
            ['شمارهٔ دانش‌آموزی', fa(m.student.no)], ['کد ملی', fa(m.student.nationalId)],
            ['پایه', m.grade], [m.groupLabel, m.className], ['آموزگار', m.teacher], ['سال تحصیلی', m.year]]) +
      '<table class="of-table"><thead><tr><th style="width:7%">ردیف</th><th>درس</th><th style="width:20%">سطح نوبت اول</th>' +
      '<th style="width:20%">سطح نوبت دوم</th><th style="width:17%">روند</th></tr></thead><tbody>' + rows + '</tbody></table>' + two +
      ofNote('سطوح چهارگانه بر پایهٔ آستانه‌های ' + fa(m.levels[0]) + '٪، ' + fa(m.levels[1]) + '٪ و ' + fa(m.levels[2]) + '٪ تعیین شده‌اند: خیلی خوب / خوب / قابل قبول / نیاز به تلاش بیشتر. تصمیم ارتقا با شورای مدرسه است.') +
      (m.feedback ? '<div class="of-decision"><b>توضیح آموزگار:</b> ' + esc(m.feedback) + '</div>' : '') +
      ofSign(['آموزگار', 'مدیر مدرسه', 'امضای ولی']) + footer(m) + '</div>';
  }

  /* ============================================================ قالب ۱۰ — فرم ۱۰۵ سنجشی */
  function tpl10(m) {
    if (m.scope === 'class') return classPage(m, CFG[9]);
    var rows = '', tq = 0, tc = 0, tw = 0, hasStats = false, hasTaraz = false, pcts = [];
    (m._allCols || m.cols).forEach(function (c) {
      var st0 = (m.me.stats && m.me.stats[c.key]) || c.stats || null;
      if (st0 && isNum(st0.questions)) { hasStats = true; tq += st0.questions; }
      if (st0 && isNum(st0.correct)) tc += st0.correct;
      if (st0 && isNum(st0.wrong)) tw += st0.wrong;
      if (st0 && isNum(st0.taraz)) hasTaraz = true;
    });
    m.cols.forEach(function (c, i) {
      var st = (m.me.stats && m.me.stats[c.key]) || c.stats || null;
      var n = st && isNum(st.questions) ? st.questions : null;
      var correct = st && isNum(st.correct) ? st.correct : null;
      var wrong = st && isNum(st.wrong) ? st.wrong : null;
      var blank = isNum(n) && isNum(correct) && isNum(wrong) ? n - correct - wrong : null;
      var net = isNum(correct) && isNum(wrong) ? correct - wrong / 3 : null;
      var pct = isNum(net) && isNum(n) && n > 0 ? round(net / n * 100, 1) : m.me.pcts[c.key];
      var taraz = st && isNum(st.taraz) ? st.taraz : null;
      if (isNum(pct)) pcts.push(pct);
      rows += '<tr><td>' + fa(i + 1) + '</td><td class="r">' + esc(c.subject || c.title) + '</td>' +
        (hasStatsCol(m) ? '<td>' + fa(n) + '</td><td>' + fa(correct) + '</td><td>' + fa(wrong) + '</td><td>' + fa(blank) + '</td><td>' + (isNum(net) ? fa(round(net, 1)) : '—') + '</td>' : '') +
        '<td class="b">' + (isNum(pct) ? fa(pct) + '٪' : '—') + '</td>' + (hasTarazCol(m) ? '<td>' + fa(taraz) + '</td>' : '') + '</tr>';
    });
    var avgPct = pcts.length ? round(mean(pcts), 1) : 0;
    return '<div class="of-frame">' +
      ofGov(m, 'کارنامهٔ رسمی آزمون سنجشی', 105, '<br><small>' + esc(m.className || '') + '</small>', m.school.stage) +
      ofId([['نام و نام خانوادگی', m.student.name], ['کد دانش‌آموزی', fa(m.student.no)],
            ['پایه', m.grade], ['رشته', m.field], ['تاریخ آزمون', m.date],
            ['تعداد آزمون', fa(m.cols.length)], ['تعداد سؤال', hasStats ? fa(tq) : '—'], ['کد پیگیری', fa(m.code)]]) +
      '<table class="of-table of-sanjesh"><thead><tr><th style="width:6%">ردیف</th><th>درس</th>' +
      (hasStatsCol(m) ? '<th style="width:9%">تعداد</th><th style="width:9%">درست</th><th style="width:9%">نادرست</th><th style="width:9%">نزده</th><th style="width:11%">نمرهٔ خام</th>' : '') +
      '<th style="width:12%">درصد</th>' + (hasTarazCol(m) ? '<th style="width:11%">تراز</th>' : '') + '</tr></thead><tbody>' + rows + '</tbody>' +
      (hasStatsCol(m) ? '<tfoot><tr><td colspan="2" class="b">جمع</td><td class="b">' + fa(tq) + '</td><td class="b">' + fa(tc) + '</td><td class="b">' + fa(tw) + '</td><td class="b">' + fa(tq - tc - tw) + '</td><td colspan="' + (hasTarazCol(m) ? 3 : 2) + '"></td></tr></tfoot>' : '') +
      '</table>' +
      ofSum([['میانگین درصد', fa(avgPct) + '٪'], ['رتبه در کلاس', m.me.rank ? fa(m.me.rank) + ' از ' + fa(m.classInfo.count || m.rows.length) : '—'],
             [m.avgLabel, isNum(m.classInfo.average) ? fa(round(m.classInfo.average, 1)) + '٪' : '—'],
             ['تعداد درس', fa(m.cols.length)]]) +
      ofNote(hasStatsCol(m) ? 'نمرهٔ خام با احتساب یک‌سوم نمرهٔ منفی برای پاسخ نادرست محاسبه شده است (درست − نادرست ÷ ۳).'
                            : 'برای این آزمون ریز پاسخ‌ها (درست/نادرست/نزده) در دسترس نبود؛ فقط درصد و نمرهٔ ثبت‌شده نمایش داده شد.') +
      (m.feedback ? '<div class="of-decision"><b>تحلیل دبیر:</b> ' + esc(m.feedback) + '</div>' : '') +
      ofSign(['کارشناس سنجش', 'مشاور تحصیلی', 'مدیر مدرسه']) + footer(m) + '</div>';
  }
  function hasStatsCol(m) {
    return m.cols.some(function (c) { var s = (m.me.stats && m.me.stats[c.key]) || c.stats; return s && isNum(s.questions) && isNum(s.correct); });
  }
  function hasTarazCol(m) {
    return m.cols.some(function (c) { var s = (m.me.stats && m.me.stats[c.key]) || c.stats; return s && isNum(s.taraz); });
  }

  /* ============================================================ پیکربندی ۱۰ قالب */
  var CFG = [
    { id: 1, name: 'رسمی اداری', desc: 'سربرگ اداری، جدول نمرات و جمع کل', accent: '#1e5eff', style: 't1', metric: 'score', unit: 'آزمون', title: 'کارنامهٔ رسمی آزمون', render: tpl1 },
    { id: 2, name: 'مدرن رنگی', desc: 'نوار درصد هر آزمون + ریز نمرات', accent: '#4338ca', style: 't2', metric: 'percent', unit: 'آزمون', title: 'کارنامهٔ تحلیلی', render: tpl2 },
    { id: 3, name: 'روند پیشرفت', desc: 'نمودار روند + جدول تغییرات', accent: '#0d9488', style: 't3', metric: 'percent', unit: 'آزمون', title: 'کارنامهٔ روند پیشرفت', render: tpl3 },
    { id: 4, name: 'رتبه و توزیع', desc: 'رتبه‌بندی کلاس + نمودار توزیع', accent: '#1d4ed8', style: 't4', metric: 'score', unit: 'آزمون', title: 'کارنامهٔ کلاس', render: tpl4 },
    { id: 5, name: 'توصیفی ابتدایی', desc: 'کارت هر درس با سطح چهارگانه و ستاره', accent: '#ea580c', style: 't5', metric: 'level', unit: 'درس', stage: 'ابتدایی', title: 'کارنامهٔ توصیفی', render: tpl5 },
    { id: 6, name: 'فرم ۱۰۱ — نوبت اول', desc: 'مستمر + پایانی ← نمرهٔ نوبت', accent: '#111827', official: true, form: 101, metric: 'score', unit: 'درس', title: 'کارنامهٔ رسمی نوبت اول',
      colA: 'نمرهٔ مستمر', colB: 'نمرهٔ پایانی', colC: 'نمرهٔ نوبت', roles: ['mostamar', 'payani'], signs: ['معاون آموزشی', 'مدیر مدرسه', 'کارشناس سنجش'],
      note: 'نمرهٔ نوبت از رابطهٔ (مستمر + ۲ × پایانی) ÷ ۳ محاسبه شده است؛ شرایط قبولی طبق آیین‌نامهٔ آموزشی سال تحصیلی جاری اعمال می‌شود.', render: function (m) { return m.scope === 'class' ? classPage(m, CFG[5]) : officialStudent(m, CFG[5]); } },
    { id: 7, name: 'فرم ۱۰۲ — سالانه', desc: 'نوبت اول + نوبت دوم ← نمرهٔ سالانه', accent: '#111827', official: true, form: 102, metric: 'score', unit: 'درس', title: 'کارنامهٔ رسمی سالانه',
      colA: 'نوبت اول', colB: 'نوبت دوم', colC: 'نمرهٔ سالانه', roles: ['nobat1', 'nobat2'], signs: ['معاون آموزشی', 'مدیر مدرسه', 'نمایندهٔ اداره'],
      note: 'نمرهٔ سالانه از رابطهٔ (نوبت اول + ۲ × نوبت دوم) ÷ ۳ به دست آمده است. تصمیم ارتقا با شورای مدرسه است.', render: function (m) { return m.scope === 'class' ? classPage(m, CFG[6]) : officialStudent(m, CFG[6]); } },
    { id: 8, name: 'فرم ۱۰۳ — نهایی', desc: 'مستمر + برگهٔ نهایی ← نمرهٔ سالانه', accent: '#111827', official: true, form: 103, metric: 'score', unit: 'درس', title: 'کارنامهٔ امتحانات نهایی',
      colA: 'نمرهٔ مستمر', colB: 'نمرهٔ برگهٔ نهایی', colC: 'نمرهٔ سالانه', roles: ['mostamar', 'barge'], signs: ['مسئول امتحانات', 'مدیر مدرسه', 'رئیس ادارهٔ منطقه'], extra: '<br><small>پایهٔ دوازدهم</small>',
      note: 'شرط قبولی هر درس: نمرهٔ سالانهٔ حداقل ۱۰ و نمرهٔ برگهٔ نهایی حداقل ۷. این فرم بدون مهر اداره فاقد اعتبار است.', render: function (m) { return m.scope === 'class' ? classPage(m, CFG[7]) : officialStudent(m, CFG[7]); } },
    { id: 9, name: 'فرم ۱۰۴ — توصیفی رسمی', desc: 'سطح نوبت اول/دوم + روند هر درس', accent: '#111827', official: true, form: 104, metric: 'level', unit: 'درس', stage: 'ابتدایی', title: 'کارنامهٔ رسمی توصیفی',
      roles: ['nobat1', 'nobat2'], signs: ['آموزگار', 'مدیر مدرسه', 'امضای ولی'], render: tpl9 },
    { id: 10, name: 'فرم ۱۰۵ — سنجشی', desc: 'درست/نادرست/نزده، نمرهٔ خام و درصد', accent: '#111827', official: true, form: 105, metric: 'percent', unit: 'درس', title: 'کارنامهٔ رسمی آزمون سنجشی',
      signs: ['کارشناس سنجش', 'مشاور تحصیلی', 'مدیر مرکز'], render: tpl10 }
  ];
  var ROLE_NAMES = { mostamar: 'مستمر', payani: 'پایانی', nobat1: 'نوبت اول', nobat2: 'نوبت دوم', barge: 'برگهٔ نهایی' };


  /* صفحهٔ ادامه: فقط جدول اصلی همان قالب (سربرگ + جدول + پاصفحه) */
  function contBody(m, cfg) {
    var off = m._offset || 0;
    if (cfg.id === 1) {
      var r1 = m.cols.map(function (c, i) {
        var v = m.me.vals[c.key], pc = m.me.pcts[c.key];
        return '<tr><td>' + fa(off + i + 1) + '</td><td class="r">' + esc(c.title) + '</td><td>' + fa(c.max) + '</td>' +
          '<td class="b">' + (isNum(v) ? fa(round(v, 2)) : '—') + '</td><td>' + (isNum(pc) ? fa(Math.round(pc)) + '٪' : '—') + '</td>' +
          '<td>' + (isNum(pc) ? esc(stateOf(pc, m.levels)) : 'غایب') + '</td></tr>';
      }).join('');
      return '<table class="grid"><thead><tr><th style="width:8%">ردیف</th><th>عنوان آزمون / مبحث</th><th style="width:12%">بارم</th>' +
        '<th style="width:14%">نمره</th><th style="width:12%">درصد</th><th style="width:18%">وضعیت</th></tr></thead><tbody>' + r1 + '</tbody></table>';
    }
    if (cfg.id === 2) {
      var r2 = m.cols.map(function (c, i) {
        var v = m.scope === 'class' ? mean(nums(m.rows.map(function (r) { return r.vals[c.key]; }))) : m.me.vals[c.key];
        return '<tr><td>' + fa(off + i + 1) + '</td><td class="r">' + esc(c.title) + '</td><td>' + esc(c.subject) + '</td>' +
          '<td>' + fa(c.max) + '</td><td class="b">' + (isNum(v) ? fa(round(v, 2)) : '—') + '</td></tr>';
      }).join('');
      return '<table class="mini"><thead><tr><th style="width:8%">#</th><th>عنوان</th><th style="width:20%">درس</th>' +
        '<th style="width:12%">بارم</th><th style="width:12%">نمره</th></tr></thead><tbody>' + r2 + '</tbody></table>';
    }
    if (cfg.id === 3) {
      var r3 = m.cols.map(function (c, i) {
        var v = m.scope === 'class' ? mean(nums(m.rows.map(function (r) { return r.vals[c.key]; }))) : m.me.vals[c.key];
        var pc = m.scope === 'class' ? mean(nums(m.rows.map(function (r) { return r.pcts[c.key]; }))) : m.me.pcts[c.key];
        return '<tr><td>' + fa(off + i + 1) + '</td><td class="r">' + esc(c.title) + '</td><td class="b">' + (isNum(v) ? fa(round(v, 2)) : '—') + '</td>' +
          '<td>' + (isNum(pc) ? fa(Math.round(pc)) + '٪' : '—') + '</td><td>' + fa(c.max) + '</td></tr>';
      }).join('');
      return '<table class="grid t3-grid"><thead><tr><th style="width:8%">#</th><th>عنوان آزمون</th><th style="width:14%">نمره</th>' +
        '<th style="width:14%">درصد</th><th style="width:14%">بارم</th></tr></thead><tbody>' + r3 + '</tbody></table>';
    }
    if (cfg.id === 5) {
      return '<div class="p-grid">' + m.cols.map(function (c) {
        var pc = m.me.pcts[c.key], L = isNum(pc) ? levelOf(pc, m.levels) : 1, color = LEVEL_COLORS[L - 1];
        return '<div class="p-card" style="border-color:' + color + '33"><div class="p-sub">' + esc(c.subject || c.title) + '</div>' +
          '<div class="p-stars" style="color:' + color + '">' + stars(L) + '</div>' +
          '<div class="p-lvl" style="background:' + color + '15;color:' + color + '">' + LEVELS[L - 1] + '</div></div>';
      }).join('') + '</div>';
    }
    if (cfg.id === 9) {
      var r9 = m.subjects.map(function (g, i) {
        var c1 = g.byRole.nobat1, c2 = g.byRole.nobat2;
        var p1 = c1 ? m.me.pcts[c1.key] : null, p2 = c2 ? m.me.pcts[c2.key] : null;
        if (!isNum(p1) && !isNum(p2)) { var pl = nums(g.cols.map(function (c) { return m.me.pcts[c.key]; })); p1 = pl.length ? pl[0] : null; p2 = pl.length > 1 ? pl[pl.length - 1] : p1; }
        var a = isNum(p1) ? levelOf(p1, m.levels) : null, b = isNum(p2) ? levelOf(p2, m.levels) : null;
        return '<tr><td>' + fa(off + i + 1) + '</td><td class="r">' + esc(g.name) + '</td><td>' + (a ? LEVELS[a - 1] : '—') + '</td>' +
          '<td>' + (b ? LEVELS[b - 1] : '—') + '</td><td>' + (a && b ? (b > a ? 'پیشرفت' : b === a ? 'تثبیت' : 'افت نسبی') : '—') + '</td></tr>';
      }).join('');
      return '<table class="of-table"><thead><tr><th style="width:7%">ردیف</th><th>درس</th><th style="width:20%">سطح نوبت اول</th>' +
        '<th style="width:20%">سطح نوبت دوم</th><th style="width:17%">روند</th></tr></thead><tbody>' + r9 + '</tbody></table>';
    }
    if (cfg.id === 10) {
      var stats = hasStatsCol(m), taraz = hasTarazCol(m);
      var r10 = m.cols.map(function (c, i) {
        var st = (m.me.stats && m.me.stats[c.key]) || c.stats || null;
        var n = st && isNum(st.questions) ? st.questions : null;
        var co = st && isNum(st.correct) ? st.correct : null, wr = st && isNum(st.wrong) ? st.wrong : null;
        var bl = isNum(n) && isNum(co) && isNum(wr) ? n - co - wr : null;
        var net = isNum(co) && isNum(wr) ? co - wr / 3 : null;
        var pc = isNum(net) && isNum(n) && n > 0 ? round(net / n * 100, 1) : m.me.pcts[c.key];
        return '<tr><td>' + fa(off + i + 1) + '</td><td class="r">' + esc(c.subject || c.title) + '</td>' +
          (stats ? '<td>' + fa(n) + '</td><td>' + fa(co) + '</td><td>' + fa(wr) + '</td><td>' + fa(bl) + '</td><td>' + (isNum(net) ? fa(round(net, 1)) : '—') + '</td>' : '') +
          '<td class="b">' + (isNum(pc) ? fa(pc) + '٪' : '—') + '</td>' + (taraz ? '<td>' + fa(st && isNum(st.taraz) ? st.taraz : null) + '</td>' : '') + '</tr>';
      }).join('');
      return '<table class="of-table of-sanjesh"><thead><tr><th style="width:6%">ردیف</th><th>درس</th>' +
        (stats ? '<th>تعداد</th><th>درست</th><th>نادرست</th><th>نزده</th><th>نمرهٔ خام</th>' : '') + '<th>درصد</th>' +
        (taraz ? '<th>تراز</th>' : '') + '</tr></thead><tbody>' + r10 + '</tbody></table>';
    }
    // فرم‌های ۶ تا ۸
    var scale = 20, formula = formulaOf(cfg.id);
    var roleA = cfg.id === 7 ? 'nobat1' : 'mostamar';
    var roleB = cfg.id === 7 ? 'nobat2' : cfg.id === 8 ? 'barge' : 'payani';
    var rr = m.subjects.map(function (g, i) {
      var ca = g.byRole[roleA], cb = g.byRole[roleB];
      var na = ca && isNum(m.me.vals[ca.key]) ? m.me.vals[ca.key] / ca.max * scale : null;
      var nb = cb && isNum(m.me.vals[cb.key]) ? m.me.vals[cb.key] / cb.max * scale : null;
      var v = subjectScore(m, g, m.me, formula, scale);
      var ok = isNum(v) && v >= 10 && (cfg.id !== 8 || !isNum(nb) || nb >= 7);
      return '<tr><td>' + fa(off + i + 1) + '</td><td class="r">' + esc(g.name) + '</td>' +
        '<td>' + (isNum(na) ? fa(round(na, cfg.id === 8 ? 2 : 1)) : '—') + '</td>' +
        '<td>' + (isNum(nb) ? fa(round(nb, cfg.id === 8 ? 2 : 1)) : '—') + '</td>' +
        '<td class="b">' + (isNum(v) ? fa(v) : '—') + '</td><td>' + (isNum(v) ? (ok ? 'قبول' : 'نیازمند تلاش') : 'غایب') + '</td></tr>';
    }).join('');
    return '<table class="of-table"><thead><tr><th style="width:7%">ردیف</th><th>نام درس</th><th style="width:14%">' + cfg.colA + '</th>' +
      '<th style="width:15%">' + cfg.colB + '</th><th style="width:14%">' + cfg.colC + '</th><th style="width:16%">وضعیت</th></tr></thead><tbody>' + rr + '</tbody></table>';
  }

  function sectionClass(cfg, m) {
    return 'page ' + (cfg.official ? 'of' + (cfg.id === 9 ? ' of-primary' : '') : cfg.style) + (m.scope === 'class' ? ' agg' : '');
  }
  function section(m, cfg, inner, pageNote) {
    return '<section class="' + sectionClass(cfg, m) + '">' + (pageNote || '') + inner + '</section>';
  }
  function applyCfg(m, cfg) {
    m.metric = cfg.metric; m.unit = cfg.unit;
    m.passMark = cfg.metric === 'level' ? 2 : cfg.metric === 'percent' ? 50 : 10;
    return m;
  }
  var lastLimit = 0;
  /* رندر خالص: آرایه‌ای از HTML صفحه‌های A4 (برای تست و برای میزبان) */
  function defaultLimit(cfg, m, byRows) {
    if (byRows) return cfg.id === 4 ? 16 : (cfg.id === 2 || cfg.id === 3) && m.scope === 'class' ? 16 : m.structure === 'subjects' ? 40 : m.structure === 'matrix' ? 30 : 20;
    if (cfg.id === 5) return 12;
    if (cfg.id === 2) return 8;
    if (cfg.id === 3) return 12;
    if (cfg.id === 1 || cfg.id === 10) return 14;
    return 11;
  }
  /* ستون‌ها را طوری تکه می‌کند که آزمون‌های هم‌درس (مستمر/پایانی) از هم جدا نشوند */
  function chunkCols(m, cfg, limit) {
    if (!cfg.roles) {
      var out = [], i;
      for (i = 0; i < m.cols.length; i += limit) out.push(m.cols.slice(i, i + limit));
      return out;
    }
    var groups = m.subjects.map(function (g) { return g.cols; });
    var chunks = [], cur = [];
    groups.forEach(function (g) {
      if (cur.length && cur.length + 1 > limit) { chunks.push(cur); cur = []; }
      cur.push(g);
    });
    if (cur.length) chunks.push(cur);
    return chunks.map(function (gs) { return gs.reduce(function (a, g) { return a.concat(g); }, []); });
  }
  /* ============================================================ V340 — دفترچهٔ کارنامهٔ مدرسه
     ورودی: {scope:'school', school, year, date, classes:[{name,teacher,students,exams,answers,graded,
     average_percent,spread_percent,below_half,grade,field}], months, topExams, reports:[payload کارنامهٔ هر کلاس]}
     خروجی: صفحهٔ نمای کلی مدرسه + خلاصهٔ کلاس‌ها با همان قالب + کارنامهٔ کامل هر کلاس. */
  function schoolSummaryPayload(p) {
    var classes = p.classes || [];
    return {
      scope: 'class', school: p.school, year: p.year, date: p.date,
      className: (p.school && p.school.name) || '', grade: '', teacher: '',
      structure: p.structure || 'combined', levels: p.levels,
      rowLabel: 'کلاس', nameLabel: 'نام کلاس', countLabel: 'تعداد کلاس',
      bestLabel: 'برترین کلاس', groupLabel: 'مدرسه', avgLabel: 'میانگین مدرسه', unit: 'شاخص',
      cols: [{ key: 'avg', title: 'میانگین درصد', subject: 'میانگین کل', max: 100 }],
      rows: classes.map(function (k) {
        return { name: k.name + (k.teacher ? ' — ' + k.teacher : ''), vals: { avg: isNum(k.average_percent) ? k.average_percent : null } };
      })
    };
  }
  /* یک ستون برای هر عنوان آزمون مشترک بین کلاس‌ها؛ نقش انتخابی معلم/مدیر با عنوان اعمال می‌شود */
  function schoolCols(p) {
    var seen = {}, out = [];
    (p.reports || []).forEach(function (r) {
      (r.cols || []).forEach(function (c) {
        var t = c.title || '';
        if (!seen[t]) { seen[t] = 1; out.push({ key: t, title: t, subject: c.subject || t, max: c.max || 20 }); }
      });
    });
    return out;
  }
  function schoolClassPayload(p, r) {
    var q = {}, titleRoles = p.roles || {};
    var copy = {
      scope: 'class', school: p.school, year: p.year, date: p.date,
      className: r.className || '', grade: r.grade || '', field: r.field || '', teacher: r.teacher || '',
      structure: p.structure || r.structure || 'combined', levels: p.levels,
      cols: r.cols || [], rows: r.rows || [], classInfo: r.classInfo || {}, roles: q
    };
    (r.cols || []).forEach(function (c) {
      var role = titleRoles[c.title] || (r.roles || {})[c.key];
      if (role) q[c.key] = role;
    });
    return copy;
  }
  function schoolOverview(p) {
    var m = normalize({ scope: 'class', school: p.school, year: p.year, date: p.date, className: (p.school || {}).name || '', groupLabel: 'مدرسه' });
    var classes = p.classes || [];
    var tot = { students: 0, exams: 0, answers: 0, graded: 0 };
    classes.forEach(function (k) {
      tot.students += Number(k.students) || 0; tot.exams += Number(k.exams) || 0;
      tot.answers += Number(k.answers) || 0; tot.graded += Number(k.graded) || 0;
    });
    var rows = classes.map(function (k, i) {
      return '<tr><td>' + fa(i + 1) + '</td><td class="r nm">' + esc(k.name || '—') + '</td>' +
        '<td class="r">' + esc(k.teacher || 'بدون معلم') + '</td><td>' + fa(k.students || 0) + '</td>' +
        '<td>' + fa(k.exams || 0) + '</td><td>' + fa(k.graded || 0) + ' / ' + fa(k.answers || 0) + '</td>' +
        '<td class="b">' + (isNum(k.average_percent) ? fa(round(k.average_percent, 1)) + '٪' : '—') + '</td>' +
        '<td>' + (isNum(k.spread_percent) ? '±' + fa(k.spread_percent) + '٪' : '—') + '</td>' +
        '<td>' + fa(k.below_half || 0) + '</td></tr>';
    }).join('');
    var tbl = '<table class="grid"><thead><tr><th style="width:5%">ردیف</th><th>کلاس</th><th>معلم</th>' +
      '<th>دانش‌آموز</th><th>آزمون</th><th>تصحیح / پاسخ</th><th>میانگین</th><th>پراکندگی</th><th>زیر ۵۰٪</th>' +
      '</tr></thead><tbody>' + rows + '</tbody></table>';
    var strip = statStrip(m, '#0f766e', [
      ['تعداد کلاس', fa(classes.length)], ['تعداد دانش‌آموز', fa(tot.students)],
      ['تعداد آزمون', fa(tot.exams)], ['پاسخ تصحیح‌شده', fa(tot.graded) + ' / ' + fa(tot.answers)]
    ]);
    var months = p.months || [];
    var trend = '';
    if (months.length) {
      trend = '<div class="agg-h">روند ۶ ماه اخیر</div><table class="mini"><thead><tr>' +
        months.map(function (x) { return '<th>' + esc(String(x.month || '')) + '</th>'; }).join('') +
        '</tr></thead><tbody><tr>' +
        months.map(function (x) { return '<td>' + (isNum(x.average_percent) ? fa(x.average_percent) + '٪' : '—') + '<br><small>' + fa(x.answers || 0) + ' پاسخ</small></td>'; }).join('') +
        '</tr></tbody></table>';
    }
    var top = p.topExams || [];
    var topTbl = '';
    if (top.length) {
      topTbl = '<div class="agg-h">آزمون‌های با بیشترین شرکت‌کننده</div><table class="mini"><thead><tr>' +
        '<th>آزمون</th><th>معلم</th><th>پاسخ</th><th>میانگین</th></tr></thead><tbody>' +
        top.map(function (t) {
          return '<tr><td class="r">' + esc(t.title || '') + '</td><td>' + esc(t.teacher || '—') + '</td>' +
            '<td>' + fa(t.answers || 0) + '</td><td>' + (isNum(t.average_percent) ? fa(t.average_percent) + '٪' : '—') + '</td></tr>';
        }).join('') + '</tbody></table>';
    }
    return section(m, CFG[0], head(m, 'کارنامهٔ مدرسه — نمای کلی', esc((p.school || {}).name || '') + ' · ' + esc(p.year || ''), '#0f766e') +
      infoGrid([['مدرسه', (p.school || {}).name || ''], ['تعداد کلاس', fa(classes.length)], ['سال تحصیلی', p.year], ['تاریخ صدور', p.date]]) +
      strip + '<div class="agg-h">جدول کلاس‌ها</div>' + tbl + trend + topTbl +
      signatures(['معاون آموزشی', 'مدیر مدرسه', 'کارشناس سنجش']) + footer(m));
  }
  function renderSchool(id, p, limitOverride) {
    var pages = [schoolOverview(p)];
    var summary = schoolSummaryPayload(p);
    if (summary.rows.length) pages = pages.concat(render(id, summary, limitOverride));
    (p.reports || []).forEach(function (r) {
      pages = pages.concat(render(id, schoolClassPayload(p, r), limitOverride));
    });
    return pages;
  }

  function render(id, payload, limitOverride) {
    if (payload && payload.scope === 'school') return renderSchool(id, payload, limitOverride);
    var cfg = CFG[Math.max(1, Math.min(10, id | 0)) - 1];
    var m = applyCfg(normalize(payload), cfg);
    if (m.scope === 'class') m.rows = m.rows.slice().sort(function (a, b) { return (b.avgPercent || 0) - (a.avgPercent || 0); });
    if (!m.cols.length || !m.rows.length) return [];
    var byRows = m.scope === 'class' || cfg.id === 4;
    var units = byRows ? m.rows.length : (cfg.roles ? m.subjects.length : m.cols.length);
    var limit = Math.max(2, limitOverride || defaultLimit(cfg, m, byRows));
    lastLimit = limit;
    if (units <= limit) return [section(m, cfg, cfg.render(m))];
    var chunks = [], k;
    if (byRows) { for (k = 0; k < m.rows.length; k += limit) chunks.push(m.rows.slice(k, k + limit)); }
    else chunks = chunkCols(m, cfg, limit);
    var offset = 0;
    return chunks.map(function (ch, i) {
      var mm = Object.create(m);
      mm._allCols = m.cols; mm._allSubjects = m.subjects; mm._cont = i > 0; mm._offset = offset;
      if (byRows) mm.rows = ch;
      else { mm.cols = ch; mm.subjects = subjectRows(mm); }
      offset += byRows ? ch.length : (cfg.roles ? mm.subjects.length : ch.length);
      var note = '<div class="agg-h">ادامه — صفحهٔ ' + fa(i + 1) + ' از ' + fa(chunks.length) + '</div>';
      if (i === 0) return section(mm, cfg, cfg.render(mm));
      var body = cfg.official
        ? '<div class="of-frame">' + ofGov(m, cfg.title, cfg.form, '<br><small>ادامه — صفحهٔ ' + fa(i + 1) + ' از ' + fa(chunks.length) + '</small>', cfg.stage) + contBody(mm, cfg) + footer(m) + '</div>'
        : head(m, cfg.title, (m.scope === 'class' ? scopeLabel(m) : m.student.name) + ' — ادامهٔ صفحهٔ ' + fa(i + 1), cfg.accent) + note + contBody(mm, cfg) + footer(m);
      return section(mm, cfg, body);
    });
  }

  /* ============================================================ رابط کاربری: انتخابگر → تناظر → پیش‌نمایش */
  var host = null, state = { payload: null, id: 0, step: 'pick', skin: 'web', zoom: 0 };
  function bridge() {
    try { if (window.ReportCardBridge) return window.ReportCardBridge; } catch (e) {}
    try { if (window.parent && window.parent.__reportCardBridge) return window.parent.__reportCardBridge; } catch (e) {}
    return null;
  }
  function thumb(cfg) {
    var a = cfg.accent, official = cfg.official;
    var body = '';
    if (cfg.id === 3) body = '<polyline points="18,66 42,52 66,58 90,34 114,26" fill="none" stroke="' + a + '" stroke-width="3"/>';
    else if (cfg.id === 4) body = [22, 40, 58, 76, 94].map(function (x, i) { var h = [16, 30, 44, 26, 12][i]; return '<rect x="' + x + '" y="' + (74 - h) + '" width="13" height="' + h + '" rx="2" fill="' + a + '" opacity="' + (0.45 + i * 0.12) + '"/>'; }).join('');
    else if (cfg.id === 2) body = [0, 1, 2, 3].map(function (i) { return '<rect x="16" y="' + (34 + i * 11) + '" width="' + [90, 66, 78, 50][i] + '" height="6" rx="3" fill="' + a + '" opacity="' + (0.9 - i * 0.15) + '"/>'; }).join('');
    else if (cfg.id === 5 || cfg.id === 9) body = [0, 1, 2, 3, 4, 5].map(function (i) { return '<rect x="' + (16 + (i % 3) * 34) + '" y="' + (34 + Math.floor(i / 3) * 22) + '" width="28" height="17" rx="3" fill="' + a + '" opacity="0.18"/><text x="' + (30 + (i % 3) * 34) + '" y="' + (46 + Math.floor(i / 3) * 22) + '" font-size="8" fill="' + a + '" text-anchor="middle">★★★</text>'; }).join('');
    else body = [0, 1, 2, 3, 4].map(function (i) { return '<rect x="16" y="' + (32 + i * 9) + '" width="100" height="7" fill="' + (i === 0 ? a : '#fff') + '" opacity="' + (i === 0 ? 0.85 : 1) + '" stroke="#cbd5e1" stroke-width=".6"/>'; }).join('');
    return '<svg viewBox="0 0 132 88" aria-hidden="true">' +
      (official ? '<rect x="8" y="8" width="116" height="72" fill="#fff" stroke="#111827" stroke-width="1.4"/>' : '<rect x="4" y="4" width="124" height="80" rx="5" fill="#fff"/>') +
      '<rect x="16" y="' + (official ? 16 : 12) + '" width="100" height="10" rx="2" fill="' + a + '" opacity="' + (official ? 0.12 : 0.9) + '"/>' +
      body + '</svg>';
  }
  function showPicker() {
    state.step = 'pick';
    var scope = state.payload.scope === 'school' ? 'مدرسه (دفترچه)' : state.payload.scope === 'class' ? 'کلاس' : 'دانش‌آموز';
    var cards = CFG.map(function (c) {
      return '<button type="button" class="rc-card" data-id="' + c.id + '"><div class="rc-thumb">' + thumb(c) +
        '<span class="rc-num">' + fa(c.id) + '</span></div><div class="rc-name">' + esc(c.name) + '</div>' +
        '<div class="rc-desc">' + esc(c.desc) + '</div>' + (c.official ? '<span class="rc-chip">فرم رسمی</span>' : '<span class="rc-chip">عمومی</span>') + '</button>';
    }).join('');
    document.getElementById('rc-root').innerHTML =
      '<div class="rc-picker"><div class="rc-h1">قالب کارنامه را انتخاب کنید</div>' +
      '<div class="rc-hint">کارنامهٔ ' + esc(scope) + ' · ' + esc(state.payload.className || state.payload.school && state.payload.school.name || '') +
      ' — هر ۱۰ قالب در دسترس است؛ پس از انتخاب، پیش‌نمایش A4 را می‌بینید و بعد چاپ می‌کنید.</div>' +
      '<div class="rc-grid">' + cards + '</div></div>';
    Array.prototype.forEach.call(document.querySelectorAll('.rc-card'), function (b) {
      b.addEventListener('click', function () { choose(+b.getAttribute('data-id')); });
    });
    setBar('انتخاب قالب کارنامه', [{ label: 'بستن', kind: 'ghost', act: closeHost }]);
  }
  function guessRoles(cols) {
    var roles = {};
    cols.forEach(function (c) {
      var t = String(c.title || '') + ' ' + String(c.subject || '');
      if (/مستمر/.test(t)) roles[c.key] = 'mostamar';
      else if (/نوبت\s*(اول|۱)|دی\b/.test(t)) roles[c.key] = 'nobat1';
      else if (/نوبت\s*(دوم|۲)|خرداد/.test(t)) roles[c.key] = 'nobat2';
      else if (/نهایی|برگه/.test(t)) roles[c.key] = 'barge';
      else if (/پایانی|پایان/.test(t)) roles[c.key] = 'payani';
    });
    return roles;
  }
  function choose(id) {
    state.id = id;
    state.limit = 0;
    var cfg = CFG[id - 1];
    var hasCols = state.payload.scope === 'school' ? schoolCols(state.payload).length : (state.payload.cols && state.payload.cols.length);
    if (cfg.roles && hasCols) { showMapping(cfg); return; }
    showPreview();
  }
  function showMapping(cfg) {
    state.step = 'map';
    /* V340 — در دفترچهٔ مدرسه، ستون‌ها عنوان‌های مشترک بین کلاس‌ها هستند */
    var mapCols = state.payload.scope === 'school' ? schoolCols(state.payload) : state.payload.cols;
    var roles = state.payload.roles || guessRoles(mapCols);
    state.payload.roles = roles;
    var opts = cfg.roles.map(function (r) { return '<option value="' + r + '">' + ROLE_NAMES[r] + '</option>'; }).join('');
    var rows = mapCols.map(function (c) {
      return '<tr><td>' + esc(c.title) + '</td><td>' + esc(c.subject || '—') + '</td>' +
        '<td><select data-key="' + esc(c.key) + '"><option value="">— استفاده نشود —</option>' + opts + '</select></td></tr>';
    }).join('');
    document.getElementById('rc-root').innerHTML =
      '<div class="rc-map"><div class="rc-h1">تنظیم «' + esc(cfg.name) + '»</div>' +
      '<div class="rc-hint">مشخص کنید هر آزمون نقش کدام ستون این فرم را دارد. آزمون‌هایی که هم‌درس هستند کنار هم قرار می‌گیرند و نمرهٔ فرم از رابطهٔ زیر حساب می‌شود:</div>' +
      '<div class="rc-note">' + esc(cfg.note || (cfg.id === 9 ? 'سطح هر نوبت از درصد همان آزمون و آستانه‌های چهارگانه ساخته می‌شود.' : '')) + '</div>' +
      '<table><thead><tr><th>آزمون</th><th>درس</th><th>نقش در فرم</th></tr></thead><tbody>' + rows + '</tbody></table>' +
      '<div class="rc-actions"><button type="button" class="rc-btn" id="rc-go">پیش‌نمایش</button>' +
      '<button type="button" class="rc-btn light" id="rc-back">بازگشت به قالب‌ها</button></div></div>';
    Array.prototype.forEach.call(document.querySelectorAll('.rc-map select'), function (s) {
      s.value = roles[s.getAttribute('data-key')] || '';
      s.addEventListener('change', function () { roles[s.getAttribute('data-key')] = s.value; });
    });
    document.getElementById('rc-go').addEventListener('click', showPreview);
    document.getElementById('rc-back').addEventListener('click', showPicker);
    setBar('تنظیم فرم رسمی', [{ label: 'قالب‌ها', kind: 'ghost', act: showPicker }, { label: 'بستن', kind: 'ghost', act: closeHost }]);
  }
  function showPreview() {
    state.step = 'preview';
    var cfg = CFG[state.id - 1];
    var pages = render(state.id, state.payload);
    if (!pages.length) {
      document.getElementById('rc-root').innerHTML = '<div class="rc-empty">برای این کارنامه داده‌ای پیدا نشد.<br>ابتدا باید آزمون تصحیح‌شده داشته باشید.</div>';
      setBar('پیش‌نمایش', [{ label: 'قالب‌ها', kind: 'ghost', act: showPicker }, { label: 'بستن', kind: 'ghost', act: closeHost }]);
      return;
    }
    var structInner = '' + [['matrix', 'ماتریس نمرات'], ['subjects', 'تحلیل درس‌ها'], ['combined', 'ترکیبی']].map(function (s) {
      return '<button type="button" data-s="' + s[0] + '" class="' + ((state.payload.structure || 'combined') === s[0] ? 'on' : '') + '">' + s[1] + '</button>';
    }).join('');
    var struct = (state.payload.scope === 'class' || state.payload.scope === 'school') && !isApp() ? '<div class="rc-struct">ساختار جدول: ' + structInner + '</div>' : '';
    var root = document.getElementById('rc-root');
    /* جاگذاری خودکار روی A4: اگر برگه‌ای سرریز کرد، تعداد ردیف/ستون هر برگه کم و دوباره رندر می‌شود */
    var tries = 0, prevMax = Infinity, bestLimit = state.limit, bestMax = Infinity;
    function draw() {
      pages = render(state.id, state.payload, state.limit);
      var sheet = '<div class="wrap">' + pages.join('') + '</div>';
      root.innerHTML = struct + (isApp() ? '<div class="rc-sizer"><div class="rc-scale">' + sheet + '</div></div>' : sheet);
      var max = 0;
      /* offsetHeight ارتفاع چیدمان است و با transform زوم تغییر نمی‌کند */
      Array.prototype.forEach.call(root.querySelectorAll('.page'), function (p) { max = Math.max(max, p.offsetHeight); });
      return max;
    }
    /* V340 — بعضی برگه‌ها (مثلاً تحلیل درس‌ها) با کم‌شدن ردیف کوتاه‌تر نمی‌شوند؛
       در آن حالت تکه‌کردن فقط تعداد صفحه را زیاد می‌کند. پس بهترین حالت قبلی
       برمی‌گردد و به‌جای آن، فاصله‌های همان برگه فشرده می‌شود. */
    function tighten() {
      var sheets = root.querySelectorAll('.page');
      Array.prototype.forEach.call(sheets, function (p) { p.classList.add('tight'); });
      var max = 0;
      Array.prototype.forEach.call(sheets, function (p) { max = Math.max(max, p.offsetHeight); });
      if (max > 1124) Array.prototype.forEach.call(sheets, function (p) { p.classList.add('tighter'); });
    }
    (function paint() {
      var max = draw();
      if (max < bestMax) { bestMax = max; bestLimit = state.limit; }
      if (max <= 1124) return;
      var cur = state.limit || lastLimit;
      var next = Math.max(2, Math.floor(cur * 0.78));
      if (tries >= 8 || max >= prevMax || next >= cur) {
        state.limit = bestLimit;
        draw();
        tighten();
        return;
      }
      prevMax = max; tries++; state.limit = next;
      paint();
    })();
    var barTitle = (isApp() ? '' : 'پیش‌نمایش — ') + cfg.name + ' · ' + fa(pages.length) + ' صفحه';
    setBar(barTitle, [
      { label: 'تغییر قالب', kind: 'ghost', act: showPicker },
      { label: isApp() ? '🖨 چاپ' : '🖨 چاپ / PDF', kind: '', act: doPrint },
      { label: 'بستن', kind: 'ghost', act: closeHost }
    ]);
    if (isApp()) {
      var slot = document.getElementById('rc-struct-slot');
      if (slot) slot.innerHTML = (state.payload.scope === 'class' || state.payload.scope === 'school') ? '<span class="rc-lbl">ساختار:</span>' + structInner : '';
      barPad();
      fitStage(true);
      root.scrollTop = 0; root.scrollLeft = 0;
    }
    wireStruct();
    window.scrollTo(0, 0);
  }
  /* ---- V339.2 — پوستهٔ اپ: هدر ثابت سه‌ردیفه، برگه هم‌عرض صفحه، زوم فقط روی برگه ---- */
  var PAGE_W = 794; /* ۲۱ میلی‌متر روی ۹۶dpi */
  function isApp() { return state.skin === 'app'; }
  function barPad() {
    var bar = document.getElementById('rc-bar');
    if (!bar) return;
    document.documentElement.style.setProperty('--rc-bar-h', (bar.offsetHeight || 0) + 'px');
  }
  /* برگه داخل یک sizer به اندازهٔ واقعیِ زوم‌شده می‌نشیند تا اسکرول افقی/عمودی فقط
     داخل ناحیهٔ برگه بماند؛ هدر بیرون این ناحیه است و هرگز جابه‌جا یا بزرگ نمی‌شود. */
  function fitStage(reset) {
    if (!isApp()) return;
    var root = document.getElementById('rc-root');
    var sizer = root.querySelector('.rc-sizer'), scale = root.querySelector('.rc-scale');
    if (!sizer || !scale) return;
    var fit = Math.min(1, Math.max(0.1, (root.clientWidth - 16) / PAGE_W));
    if (reset || !state.zoom) state.zoom = fit;
    state.zoom = Math.min(3, Math.max(fit, state.zoom));
    var k = state.zoom;
    scale.style.transform = 'scale(' + k + ')';
    sizer.style.width = Math.ceil(PAGE_W * k) + 'px';
    sizer.style.height = Math.ceil(scale.offsetHeight * k) + 'px';
  }
  function initAppSkin() {
    var root = document.getElementById('rc-root');
    if (!root || root.__rcPinch) return;
    root.__rcPinch = true;
    var d0 = 0, z0 = 1;
    function dist(t) {
      var dx = t[0].clientX - t[1].clientX, dy = t[0].clientY - t[1].clientY;
      return Math.sqrt(dx * dx + dy * dy);
    }
    root.addEventListener('touchstart', function (e) {
      if (e.touches.length !== 2 || state.step !== 'preview') return;
      d0 = dist(e.touches); z0 = state.zoom || 1;
    }, { passive: true });
    root.addEventListener('touchmove', function (e) {
      if (e.touches.length !== 2 || !d0 || state.step !== 'preview') return;
      e.preventDefault();
      state.zoom = z0 * (dist(e.touches) / d0);
      fitStage(false);
    }, { passive: false });
    root.addEventListener('touchend', function (e) { if (e.touches.length < 2) d0 = 0; }, { passive: true });
    window.addEventListener('resize', function () { barPad(); fitStage(true); });
  }
  function wireStruct() {
    Array.prototype.forEach.call(document.querySelectorAll('.rc-struct button, .rc-bar-struct button'), function (b) {
      b.addEventListener('click', function () { state.payload.structure = b.getAttribute('data-s'); state.limit = 0; showPreview(); });
    });
  }
  function setBar(title, actions) {
    var bar = document.getElementById('rc-bar');
    if (isApp()) {
      bar.innerHTML = '';
      var row = document.createElement('div');
      row.className = 'rc-bar-row';
      actions.forEach(function (a) {
        var b = document.createElement('button');
        b.type = 'button'; b.className = 'rc-btn ' + (a.kind || ''); b.textContent = a.label;
        b.addEventListener('click', a.act); row.appendChild(b);
      });
      var t = document.createElement('div');
      t.className = 'rc-bar-title'; t.textContent = title;
      var slot = document.createElement('div');
      slot.className = 'rc-bar-struct'; slot.id = 'rc-struct-slot';
      bar.appendChild(row); bar.appendChild(t); bar.appendChild(slot);
      barPad();
      return;
    }
    bar.innerHTML = '<b>' + esc(title) + '</b><span class="grow"></span>';
    actions.forEach(function (a) {
      var b = document.createElement('button');
      b.type = 'button'; b.className = 'rc-btn ' + (a.kind || ''); b.textContent = a.label;
      b.addEventListener('click', a.act); bar.appendChild(b);
    });
  }
  function doPrint() {
    var b = bridge();
    if (b && typeof b.print === 'function') { b.print(String(state.id)); return; }
    try { window.print(); } catch (e) { }
  }
  function closeHost() {
    var b = bridge();
    if (b && typeof b.close === 'function') { b.close(); return; }
    document.getElementById('rc-root').innerHTML = '<div class="rc-empty">پنجره را ببندید.</div>';
  }
  function open(payload, opts) {
    state.payload = payload || {};
    /* پوستهٔ اپ فقط وقتی فعال می‌شود که میزبان اندروید آن را خواسته باشد؛ سایت مثل قبل می‌ماند */
    state.skin = (opts && opts.skin === 'app') ? 'app' : 'web';
    state.zoom = 0;
    try { document.body.classList.toggle('rc-app', isApp()); } catch (e) {}
    if (isApp()) initAppSkin();
    if (!state.payload.structure) state.payload.structure = 'combined';
    showPicker();
  }
  window.ReportCard = {
    open: open, render: render, normalize: normalize, templates: CFG.map(function (c) { return { id: c.id, name: c.name, desc: c.desc, official: !!c.official, roles: c.roles || null }; }),
    preview: showPreview, pick: choose, state: state
  };
})();
