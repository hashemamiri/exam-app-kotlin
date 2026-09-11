/* ===================================================================
   فاز ۶ سایت — گزارش کلاس + Excel/CSV، صدور/وارد کردن فایل آزمون (.azmoon)،
   پشتیبان کامل و بازیابی، ضبط/آپلود صوت سؤال، ورود با گوگل، بازیابی رمز،
   حذف حساب.
   قراردادها مطابق SupabasePortabilityRepository / ExamPackageCodec /
   ReportsViewModel / SupabaseAuthRepository / SupabaseProfileRepository.
   =================================================================== */
(function () {
  var S = window.ExamSite;
  if (!S) return;
  var el = S.el, esc = S.esc, fa = S.fa, toast = S.toast, errMsg = S.errMsg, uuid = S.uuid;
  function chk(r) { if (r && typeof r === 'object' && r.error) throw new Error(String(r.error)); return r || {}; }
  function download(name, content, mime) { return (window.SiteExtras && window.SiteExtras.download !== download ? window.SiteExtras.download : downloadImpl)(name, content, mime); }
  function downloadImpl(name, content, mime) {
    var blob = content instanceof Blob ? content : new Blob([content], {type: mime || 'application/octet-stream'});
    var a = el('a', {href: URL.createObjectURL(blob), download: name, style: 'display:none'}); document.body.appendChild(a); a.click(); setTimeout(function () { URL.revokeObjectURL(a.href); a.remove(); }, 2000);
  }
  function pickFile(accept) {
    return new Promise(function (resolve) { var i = el('input', {type: 'file', accept: accept || '*/*', style: 'display:none'}); i.addEventListener('change', function () { resolve(i.files && i.files[0] ? i.files[0] : null); i.remove(); }); document.body.appendChild(i); i.click(); });
  }
  function readText(file) { return new Promise(function (res, rej) { var r = new FileReader(); r.onload = function () { res(String(r.result)); }; r.onerror = function () { rej(new Error('خواندن فایل ناموفق بود.')); }; r.readAsText(file, 'utf-8'); }); }
  function utf8b64(s) { return btoa(unescape(encodeURIComponent(s))); }
  function b64utf8(b) { return decodeURIComponent(escape(atob(b.replace(/\s+/g, '')))); }
  function safeName(t) { return String(t || '').replace(/[\\/:*?"<>|]/g, '_').trim().slice(0, 48) || 'exam'; }
  function todayJ() { var J = window.SiteAdmin && window.SiteAdmin.J; if (!J) return new Date().toISOString().slice(0, 10); var d = J.fromGregorian(new Date()); return d.jy + '-' + (d.jm < 10 ? '0' : '') + d.jm + '-' + (d.jd < 10 ? '0' : '') + d.jd; }

  /* ================================================================ XLSX ساده (بدون کتابخانه) */
  var crcTable = (function () { var t = [], c; for (var n = 0; n < 256; n++) { c = n; for (var k = 0; k < 8; k++) c = c & 1 ? 0xEDB88320 ^ (c >>> 1) : c >>> 1; t[n] = c >>> 0; } return t; })();
  function crc32(u8) { var c = 0xFFFFFFFF; for (var i = 0; i < u8.length; i++) c = crcTable[(c ^ u8[i]) & 0xFF] ^ (c >>> 8); return (c ^ 0xFFFFFFFF) >>> 0; }
  function zipStore(files) {
    /* files: [{name, data(Uint8Array)}] → Blob (ZIP بدون فشرده‌سازی) */
    var enc = new TextEncoder(), parts = [], central = [], offset = 0;
    function u16(n) { return [n & 255, (n >> 8) & 255]; }
    function u32(n) { return [n & 255, (n >> 8) & 255, (n >> 16) & 255, (n >>> 24) & 255]; }
    files.forEach(function (f) {
      var name = enc.encode(f.name), data = f.data, crc = crc32(data);
      var local = new Uint8Array([].concat([0x50, 0x4b, 0x03, 0x04], u16(20), u16(0x0800), u16(0), u16(0), u16(0), u32(crc), u32(data.length), u32(data.length), u16(name.length), u16(0)));
      parts.push(local, name, data);
      central.push(new Uint8Array([].concat([0x50, 0x4b, 0x01, 0x02], u16(20), u16(20), u16(0x0800), u16(0), u16(0), u16(0), u32(crc), u32(data.length), u32(data.length), u16(name.length), u16(0), u16(0), u16(0), u16(0), u32(0), u32(offset))), name);
      offset += local.length + name.length + data.length;
    });
    var cdSize = central.reduce(function (s, p) { return s + p.length; }, 0);
    var end = new Uint8Array([].concat([0x50, 0x4b, 0x05, 0x06], u16(0), u16(0), u16(files.length), u16(files.length), u32(cdSize), u32(offset), u16(0)));
    return new Blob(parts.concat(central, [end]), {type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'});
  }
  function xmlEsc(s) { return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;'); }
  function colName(i) { var s = ''; i++; while (i > 0) { var m = (i - 1) % 26; s = String.fromCharCode(65 + m) + s; i = Math.floor((i - 1) / 26); } return s; }
  function xlsx(sheets) {
    /* sheets: [{name, rows:[[cell,...]]}] ؛ عدد → عدد، بقیه → رشته‌ی درون‌خطی؛ ردیف اول پررنگ؛ راست‌به‌چپ */
    var enc = new TextEncoder();
    var sheetXml = sheets.map(function (sh) {
      var rows = sh.rows.map(function (r, ri) {
        return '<row r="' + (ri + 1) + '">' + r.map(function (v, ci) {
          var ref = colName(ci) + (ri + 1), st = ri === 0 ? ' s="1"' : '';
          if (v == null || v === '') return '';
          if (typeof v === 'number' && isFinite(v)) return '<c r="' + ref + '"' + st + '><v>' + v + '</v></c>';
          return '<c r="' + ref + '"' + st + ' t="inlineStr"><is><t xml:space="preserve">' + xmlEsc(v) + '</t></is></c>';
        }).join('') + '</row>';
      }).join('');
      var widths = '<cols>' + (sh.rows[0] || []).map(function (_, ci) { return '<col min="' + (ci + 1) + '" max="' + (ci + 1) + '" width="' + (ci === 0 ? 28 : 16) + '" customWidth="1"/>'; }).join('') + '</cols>';
      return '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetViews><sheetView rightToLeft="1" workbookViewId="0"/></sheetViews>' + widths + '<sheetData>' + rows + '</sheetData></worksheet>';
    });
    var wb = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>' + sheets.map(function (sh, i) { return '<sheet name="' + xmlEsc(sh.name.slice(0, 31)) + '" sheetId="' + (i + 1) + '" r:id="rId' + (i + 1) + '"/>'; }).join('') + '</sheets></workbook>';
    var wbRels = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">' + sheets.map(function (_, i) { return '<Relationship Id="rId' + (i + 1) + '" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet' + (i + 1) + '.xml"/>'; }).join('') + '<Relationship Id="rId' + (sheets.length + 1) + '" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>';
    var styles = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts><fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills><borders count="1"><border/></borders><cellXfs count="2"><xf fontId="0" fillId="0" borderId="0"/><xf fontId="1" fillId="0" borderId="0" applyFont="1"/></cellXfs></styleSheet>';
    var ct = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>' + sheets.map(function (_, i) { return '<Override PartName="/xl/worksheets/sheet' + (i + 1) + '.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>'; }).join('') + '</Types>';
    var rels = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>';
    var files = [{name: '[Content_Types].xml', data: enc.encode(ct)}, {name: '_rels/.rels', data: enc.encode(rels)}, {name: 'xl/workbook.xml', data: enc.encode(wb)}, {name: 'xl/_rels/workbook.xml.rels', data: enc.encode(wbRels)}, {name: 'xl/styles.xml', data: enc.encode(styles)}];
    sheetXml.forEach(function (x, i) { files.push({name: 'xl/worksheets/sheet' + (i + 1) + '.xml', data: enc.encode(x)}); });
    return zipStore(files);
  }
  function csv(rows) { return '\uFEFF' + rows.map(function (r) { return r.map(function (v) { return '"' + String(v == null ? '' : v).replace(/"/g, '""') + '"'; }).join(','); }).join('\n'); }

  /* ================================================================ گزارش کلاس (ReportsScreen) */
  async function reportsPage(c) {
    S.loading(c);
    try {
      var r = await Promise.all([S.select('exams', 'select=id,title,subject,total_score,created_at&teacher_id=eq.' + S.user().id + '&order=created_at.desc'), S.rpc('native_my_classes_v28', {})]);
      var exams = r[0] || [], classes = r[1] || [];
      c.innerHTML = '';
      var st = {classId: '', selected: {}, rows: null, roster: []};
      exams.forEach(function (e) { st.selected[e.id] = true; });
      /* خلاصهٔ کلی (analytics) */
      var summary = el('div', {class: 'grid4'}); c.appendChild(summary);
      var answersByExam = {};
      (async function () {
        try {
          var all = await S.select('answers', 'select=id,exam_id,student_id,total_grade,graded&exam_id=in.(' + exams.map(function (e) { return e.id; }).join(',') + ')');
          (all || []).forEach(function (a) { (answersByExam[a.exam_id] = answersByExam[a.exam_id] || []).push(a); });
          var graded = (all || []).filter(function (a) { return a.graded; }), max = {}; exams.forEach(function (e) { max[e.id] = Number(e.total_score) || 0; });
          var pct = graded.map(function (a) { return max[a.exam_id] > 0 ? Number(a.total_grade) * 100 / max[a.exam_id] : null; }).filter(function (x) { return x != null; });
          var avg = pct.length ? pct.reduce(function (s, x) { return s + x; }, 0) / pct.length : null;
          summary.innerHTML = '';
          [[fa(exams.length), 'آزمون'], [fa((all || []).length), 'پاسخ'], [fa(graded.length), 'تصحیح‌شده'], [avg == null ? '—' : fa(avg.toFixed(1)) + '٪', 'میانگین درصد']].forEach(function (x) { summary.appendChild(el('div', {class: 'card stat'}, [el('div', {class: 'v', text: x[0]}), el('div', {class: 'l', text: x[1]})])); });
          st.answersLoaded = true; if (st.classId) compute();
        } catch (e) { summary.innerHTML = ''; summary.appendChild(el('div', {class: 'alert error', text: errMsg(e)})); }
      })();
      if (!exams.length) { c.appendChild(el('div', {class: 'card'}, [S.emptyBox('📈', 'هنوز آزمونی ندارید.')])); return; }
      /* انتخاب کلاس و آزمون‌ها */
      var csel = el('select'); csel.appendChild(el('option', {value: '', text: '— انتخاب کلاس —'})); classes.forEach(function (k) { csel.appendChild(el('option', {value: k.id, text: k.name})); });
      var exBox = el('div', {class: 'row', style: 'flex-wrap:wrap;gap:6px'});
      function drawExams() { exBox.innerHTML = ''; exams.forEach(function (e) { exBox.appendChild(el('button', {class: 'chip ' + (st.selected[e.id] ? 'brand' : 'off'), text: e.title, onclick: function () { st.selected[e.id] = !st.selected[e.id]; drawExams(); if (st.classId) compute(); }})); }); }
      drawExams();
      var out = el('div', {class: 'card'}); var tools = el('div', {class: 'row', style: 'margin-bottom:10px'});
      c.appendChild(el('div', {class: 'card'}, [el('h3', {text: '📈 گزارش کلاس'}), el('div', {class: 'grid2'}, [el('div', {class: 'field'}, [el('label', {text: 'کلاس'}), csel]), el('div', {class: 'field'}, [el('label', {text: 'آزمون‌های گزارش (برای حذف/اضافه کلیک کنید)'}), exBox])])]));
      c.appendChild(out);
      csel.addEventListener('change', async function () { st.classId = csel.value; if (!st.classId) { out.innerHTML = ''; return; } S.loading(out); try { st.roster = await S.rpc('class_roster', {p_class: st.classId}); compute(); } catch (e) { S.showErr(out, e); } });
      function compute() {
        if (!st.answersLoaded) { S.loading(out); return; }
        var sel = exams.filter(function (e) { return st.selected[e.id]; });
        var rows = st.roster.map(function (s) {
          var scores = {}, pcts = [];
          sel.forEach(function (e) { var mine = (answersByExam[e.id] || []).filter(function (a) { return a.graded && a.student_id === s.id; }); if (mine.length) { var best = Math.max.apply(null, mine.map(function (a) { return Number(a.total_grade) || 0; })); scores[e.id] = best; if (Number(e.total_score) > 0) pcts.push(best * 100 / Number(e.total_score)); } });
          return {id: s.id, name: s.full_name, scores: scores, avg: pcts.length ? pcts.reduce(function (a, b) { return a + b; }, 0) / pcts.length : null};
        });
        st.rows = rows;
        out.innerHTML = '';
        var header = ['نام دانش‌آموز'].concat(sel.map(function (e) { return e.title; }), ['میانگین درصد']);
        function tableRows() { return [header].concat(rows.map(function (r) { return [r.name].concat(sel.map(function (e) { return r.scores[e.id] != null ? r.scores[e.id] : ''; }), [r.avg == null ? '' : Number(r.avg.toFixed(2))]); })); }
        var cls = classes.filter(function (k) { return k.id === st.classId; })[0];
        out.appendChild(el('div', {class: 'row', style: 'margin-bottom:10px'}, [el('b', {class: 'grow', text: (cls ? cls.name : '') + ' — ' + fa(rows.length) + ' دانش‌آموز، ' + fa(sel.length) + ' آزمون'}),
          el('button', {class: 'btn light sm', text: '📥 Excel', onclick: function () { var summaryRows = [['شاخص', 'مقدار'], ['کلاس', cls ? cls.name : ''], ['تعداد آزمون', sel.length], ['تعداد دانش‌آموز', rows.length], ['تاریخ', todayJ()]]; download('گزارش-' + safeName(cls ? cls.name : 'کلاس') + '.xlsx', xlsx([{name: 'لیست نمرات', rows: tableRows()}, {name: 'خلاصه', rows: summaryRows}])); }}),
          el('button', {class: 'btn light sm', text: '📄 CSV', onclick: function () { download('گزارش-' + safeName(cls ? cls.name : 'کلاس') + '.csv', csv(tableRows()), 'text/csv;charset=utf-8'); }}),
          el('button', {class: 'btn light sm', text: '🖨 چاپ / PDF', onclick: function () { printTable((cls ? 'گزارش کلاس ' + cls.name : 'گزارش کلاس'), tableRows()); }})]));
        if (!rows.length) { out.appendChild(S.emptyBox('🎓', 'این کلاس دانش‌آموزی ندارد.')); return; }
        out.appendChild(el('div', {style: 'overflow:auto'}, [el('table', {class: 'tbl'}, [el('thead', {}, [el('tr', {}, header.map(function (h) { return el('th', {text: h}); }))]),
          el('tbody', {}, rows.map(function (r) { return el('tr', {}, [el('td', {html: '<b>' + esc(r.name) + '</b>'})].concat(sel.map(function (e) { return el('td', {text: r.scores[e.id] != null ? fa(S.fmtScore(r.scores[e.id])) : '—'}); }), [el('td', {}, [r.avg == null ? el('span', {class: 'muted', text: '—'}) : el('span', {class: 'chip ' + (r.avg >= 50 ? 'ok' : 'danger'), text: fa(r.avg.toFixed(1)) + '٪'})])])); }))])]));
        /* نمودار سادهٔ توزیع */
        var buckets = [0, 0, 0, 0, 0]; rows.forEach(function (r) { if (r.avg != null) buckets[Math.min(4, Math.floor(r.avg / 20))]++; });
        var mx = Math.max.apply(null, buckets.concat([1]));
        out.appendChild(el('div', {style: 'margin-top:16px'}, [el('div', {class: 'muted', style: 'font-size:12px;margin-bottom:6px', text: 'توزیع میانگین درصد'}), el('div', {class: 'row', style: 'align-items:flex-end;gap:10px;height:110px'}, buckets.map(function (b, i) { return el('div', {style: 'flex:1;text-align:center;font-size:12px'}, [el('div', {text: fa(b)}), el('div', {style: 'background:var(--brand);border-radius:6px 6px 0 0;height:' + Math.round(b / mx * 70) + 'px;margin:2px 6px 0'}), el('div', {class: 'muted', text: fa(i * 20) + '–' + fa(i * 20 + 20)})]); }))]));
      }
    } catch (e) { S.showErr(c, e); }
  }
  function printTable(title, rows) {
    var w = window.open('', '_blank'); if (!w) return toast('پنجرهٔ چاپ باز نشد؛ پاپ‌آپ را اجازه دهید.', 'err');
    var html = '<!doctype html><html dir="rtl" lang="fa"><head><meta charset="utf-8"><title>' + xmlEsc(title) + '</title><style>body{font-family:Vazirmatn,Tahoma,sans-serif;padding:20px}table{border-collapse:collapse;width:100%;font-size:13px}th,td{border:1px solid #999;padding:6px 8px;text-align:right}th{background:#eee}h1{font-size:18px}</style></head><body><h1>' + xmlEsc(title) + '</h1><table>' + rows.map(function (r, i) { return '<tr>' + r.map(function (v) { return (i ? '<td>' : '<th>') + xmlEsc(v == null ? '' : v) + (i ? '</td>' : '</th>'); }).join('') + '</tr>'; }).join('') + '</table><script>window.onload=function(){window.print();}<\/script></body></html>';
    w.document.open(); w.document.write(html); w.document.close();
  }
  /* کارنامهٔ دانش‌آموز → Excel */
  function gradesExcelButton(grades) {
    return el('button', {class: 'btn light sm', text: '📥 Excel', onclick: function () { var rows = [['درس', 'آزمون', 'نمره', 'از', 'درصد', 'بازخورد']]; (grades || []).forEach(function (g) { var t = Number(g.total_score) || 0, s = Number(g.total_grade) || 0; rows.push([g.subject || '', g.title || g.exam_title || '', s, t, t > 0 ? Number((s * 100 / t).toFixed(2)) : '', g.feedback || '']); }); download('my-grades.xlsx', xlsx([{name: 'نمرات من', rows: rows}])); }});
  }

  /* ================================================================ صدور / وارد کردن فایل آزمون (ExamPackageCodec) */
  var PKG_TAG = 'EXAMPKG1', PKG_EXT = '.azmoon';
  async function exportExam(exam, includeKey) {
    var B = window.SiteBuilder; if (!B) throw new Error('سازندهٔ آزمون در دسترس نیست.');
    var full = await S.api.examDetail(exam.id);
    var keys = {}; (includeKey && Array.isArray(full.__answers) ? full.__answers : []).forEach(function (k, i) { if (k && typeof k === 'object') keys[k.i != null ? k.i : i] = k; });
    var qs = (Array.isArray(full.questions) ? full.questions : []).map(function (q, i) { return B.decodeQuestion(q, keys[i] || {}); });
    var enc = B.encodeQuestions(qs);
    var combined = enc.publicQuestions.map(function (p, i) { var o = Object.assign({}, p, includeKey ? (enc.answerKey[i] || {}) : {}); delete o.i; return o; });
    var prof = null; try { prof = await S.api.profile(); } catch (e) {}
    var root = {_app: 'exam-system', _kind: 'exam', _v: 2, exported_at: new Date().toISOString(), by: ((prof && (prof.displayName || prof.fullName)) || '').slice(0, 120),
      exam: {title: String(full.title || '').slice(0, 250), subject: String(full.subject || '').slice(0, 250), duration: Math.max(0, Math.min(1440, Number(full.duration) || 0)), opens_at: full.opens_at || null, closes_at: full.closes_at || null, neg_marking: Math.max(0, Number(full.neg_marking) || 0), shuffle_q: !!full.shuffle_q, shuffle_opt: !!full.shuffle_opt, teacher_message: String(full.teacher_message || '').slice(0, 1000), attempts_allowed: Math.max(1, Math.min(5, Number(full.attempts_allowed) || 1)), attempt_on_timeout: !!full.attempt_on_timeout, grade_policy: full.grade_policy || 'last', answer_key: !!includeKey, attempt_cooldown: Math.max(0, Math.min(1440, Number(full.attempt_cooldown) || 0)), questions: combined}};
    var content = PKG_TAG + '\n' + utf8b64(JSON.stringify(root));
    download('آزمون-' + safeName(full.title) + (includeKey ? '' : '-بدون-پاسخنامه') + PKG_EXT, content, 'application/octet-stream');
  }
  function exportExamDlg(exam) {
    var bg = el('div', {class: 'modal-bg'});
    bg.appendChild(el('div', {class: 'modal'}, [el('button', {class: 'x', text: '✕', onclick: function () { bg.remove(); }}), el('h2', {text: 'صدور فایل آزمون'}), el('p', {class: 'muted', text: 'فایل .azmoon را می‌توانید به معلم دیگری بدهید تا در برنامه یا سایت وارد کند.'}),
      el('div', {class: 'row'}, [el('button', {class: 'btn', text: 'همراه پاسخنامه', onclick: async function () { try { await exportExam(exam, true); bg.remove(); } catch (e) { toast(errMsg(e), 'err'); } }}), el('button', {class: 'btn light', text: 'بدون پاسخنامه', onclick: async function () { try { await exportExam(exam, false); bg.remove(); } catch (e) { toast(errMsg(e), 'err'); } }})])]));
    document.body.appendChild(bg);
  }
  function parseExamPackage(raw) {
    if (new Blob([raw]).size > 8 * 1024 * 1024) throw new Error('حجم فایل آزمون بیش از ۸ مگابایت است.');
    var t = raw.trim(); if (!t) throw new Error('فایل آزمون خالی است.');
    var jsonText = t.indexOf(PKG_TAG) === 0 ? b64utf8(t.slice(PKG_TAG.length).trim()) : (t[0] === '{' ? t : b64utf8(t));
    var root = JSON.parse(jsonText);
    if (root._app !== 'exam-system') throw new Error('این فایل متعلق به آزمون آنلاین نیست.');
    if (root._kind !== 'exam') throw new Error('نوع فایل، بسته آزمون نیست.');
    var ex = root.exam; if (!ex) throw new Error('بدنه آزمون در فایل وجود ندارد.');
    var qs = Array.isArray(ex.questions) ? ex.questions : [];
    if (qs.length < 1 || qs.length > 500) throw new Error('تعداد سؤال‌های فایل باید بین ۱ و ۵۰۰ باشد.');
    return {title: String(ex.title || '').slice(0, 250) || 'آزمون واردشده', subject: String(ex.subject || '').slice(0, 250), duration: Math.max(0, Math.min(1440, Number(ex.duration) || 0)), negativeMarking: Math.max(0, Number(ex.neg_marking) || 0), shuffleQuestions: !!ex.shuffle_q, shuffleOptions: !!ex.shuffle_opt, teacherMessage: String(ex.teacher_message || '').slice(0, 1000), attemptsAllowed: Math.max(1, Math.min(5, Number(ex.attempts_allowed) || 1)), attemptOnTimeout: !!ex.attempt_on_timeout, gradePolicy: ['last', 'best', 'all'].indexOf(ex.grade_policy) >= 0 ? ex.grade_policy : 'last', attemptCooldown: Math.max(0, Math.min(1440, Number(ex.attempt_cooldown) || 0)), questions: qs, hasKey: ex.answer_key !== false, by: root.by || ''};
  }
  async function importExam() {
    var f = await pickFile('.azmoon,.json,.txt,application/octet-stream'); if (!f) return;
    try {
      var pkg = parseExamPackage(await readText(f));
      if (!(await S.confirmDlg('وارد کردن آزمون', 'آزمون «' + esc(pkg.title) + '» با ' + fa(pkg.questions.length) + ' سؤال' + (pkg.by ? ' (صادرشده توسط ' + esc(pkg.by) + ')' : '') + (pkg.hasKey ? '' : ' — <b>بدون پاسخنامه</b>') + ' در سازنده باز شود؟ پس از بررسی، با «ذخیره در سرور» به‌عنوان آزمون جدید ثبت می‌شود.', 'باز کردن'))) return;
      S.go('builder', {importPkg: pkg, fresh: true});
    } catch (e) { toast(errMsg(e), 'err'); }
  }

  /* ================================================================ پشتیبان کامل / بازیابی */
  var MAX_BACKUP = 20 * 1024 * 1024;
  function backupCard() {
    var card = el('div', {class: 'card'}, [el('h3', {text: '🗄 پشتیبان و بازیابی داده‌ها'}), el('p', {class: 'muted', style: 'font-size:13px', text: 'پشتیبان شامل آزمون‌ها، کلاس‌ها و عضویت‌ها و سربرگ چاپ است (حداکثر ۲۰ مگابایت). بازیابی، آزمون‌ها را به‌صورت نسخهٔ جدید می‌سازد و مثل ساخت آزمون هزینه دارد.'})]);
    var msg = el('div'); card.appendChild(msg);
    card.appendChild(el('div', {class: 'row', style: 'flex-wrap:wrap'}, [
      el('button', {class: 'btn light', text: '⬇ دریافت پشتیبان کامل', onclick: async function () { try { var raw = chk(await S.rpcObj('native_export_backup_v3', {})); var content = JSON.stringify(raw, null, 2); if (new Blob([content]).size > MAX_BACKUP) throw new Error('حجم پشتیبان از سقف ۲۰ مگابایت بیشتر است.'); download('پشتیبان-سامانه-آزمون-' + todayJ() + '.json', content, 'application/json'); toast('پشتیبان دانلود شد.', 'ok'); } catch (e) { toast(errMsg(e), 'err'); } }}),
      el('button', {class: 'btn light', text: '⬆ بازیابی از فایل', onclick: async function () { var f = await pickFile('.json,application/json'); if (!f) return; try { var raw = await readText(f); if (new Blob([raw]).size > MAX_BACKUP) throw new Error('حجم پشتیبان بیش از ۲۰ مگابایت است.'); var root = JSON.parse(raw); if (root._app !== 'exam-native') throw new Error('فایل پشتیبان متعلق به نسخه Native نیست.'); if (root._kind !== 'backup') throw new Error('نوع فایل، پشتیبان کامل نیست.'); var v = Number(root._version) || 0; if (v < 1 || v > 4) throw new Error('نسخه فایل پشتیبان پشتیبانی نمی‌شود.'); var exams = root.exams || [], classes = root.classes || []; if (exams.length > 200) throw new Error('تعداد آزمون‌های پشتیبان بیش از حد مجاز است.'); if (classes.length > 500) throw new Error('تعداد کلاس‌های پشتیبان بیش از حد مجاز است.'); var tq = exams.reduce(function (s, e) { return s + ((e.questions || []).length); }, 0); if (tq > 10000) throw new Error('مجموع سؤال‌های پشتیبان بیش از حد مجاز است.'); var mem = classes.reduce(function (s, k) { return s + ((k.members || []).length); }, 0); restoreDlg({bundle: root, exams: exams.length, questions: tq, classes: classes.length, memberships: mem, hasHeader: !!(root.profile && root.profile.header && typeof root.profile.header === 'object'), teacher: root.profile && (root.profile.display_name || root.profile.full_name), createdAt: root.created_at}); } catch (e) { toast(errMsg(e), 'err'); } }}),
      el('button', {class: 'btn light', text: '📥 وارد کردن فایل آزمون (.azmoon)', onclick: importExam})]));
    return card;
  }
  function restoreDlg(p) {
    var bg = el('div', {class: 'modal-bg'}); var msg = el('div');
    function cb(label, on) { var c = el('input', {type: 'checkbox'}); c.checked = on; return {c: c, row: el('label', {class: 'g-item'}, [c, el('span', {text: label})])}; }
    var ex = cb('آزمون‌ها (' + fa(p.exams) + ' آزمون، ' + fa(p.questions) + ' سؤال)', p.exams > 0), cl = cb('کلاس‌ها (' + fa(p.classes) + ')', p.classes > 0), mb = cb('عضویت دانش‌آموزان در کلاس‌ها (' + fa(p.memberships) + ')', p.memberships > 0), hd = cb('سربرگ چاپ', p.hasHeader);
    var b = el('button', {class: 'btn', text: 'بازیابی'});
    b.addEventListener('click', async function () {
      msg.innerHTML = '';
      try {
        if (!ex.c.checked && !cl.c.checked && !hd.c.checked) throw new Error('حداقل یک بخش برای بازیابی انتخاب کنید.');
        if (!(await S.confirmDlg('تأیید بازیابی', 'بازیابی آزمون‌ها مثل ساخت آزمون جدید هزینه دارد و از کیف پول کسر می‌شود. ادامه می‌دهید؟', 'بله، بازیابی کن'))) return;
        b.disabled = true;
        var r = chk(await S.rpcObj('native_restore_backup_v3', {p_operation: uuid(), p_bundle: p.bundle, p_options: {exams: ex.c.checked, classes: cl.c.checked, memberships: mb.c.checked && cl.c.checked, header: hd.c.checked}}));
        bg.remove();
        var lines = ['آزمون‌های ساخته‌شده: ' + fa(r.exams_created || 0), 'کلاس‌های ساخته‌شده: ' + fa(r.classes_created || 0), 'عضویت‌های بازیابی‌شده: ' + fa(r.memberships_restored || 0) + (r.memberships_missing ? ' (یافت‌نشده: ' + fa(r.memberships_missing) + ')' : ''), 'هزینه: ' + S.money(r.cost || 0), 'موجودی: ' + S.money(r.balance || 0)];
        var bg2 = el('div', {class: 'modal-bg'}); bg2.appendChild(el('div', {class: 'modal'}, [el('h2', {text: '✅ بازیابی انجام شد'}), el('ul', {}, lines.map(function (l) { return el('li', {text: l}); })), el('button', {class: 'btn', text: 'باشه', onclick: function () { bg2.remove(); S.go('dashboard'); }})])); document.body.appendChild(bg2);
      } catch (e) { msg.appendChild(el('div', {class: 'alert error', text: errMsg(e)})); b.disabled = false; }
    });
    bg.appendChild(el('div', {class: 'modal'}, [el('button', {class: 'x', text: '✕', onclick: function () { bg.remove(); }}), el('h2', {text: 'بازیابی پشتیبان'}), el('p', {class: 'muted', style: 'font-size:13px', text: (p.teacher ? 'صادرشده توسط ' + p.teacher : '') + (p.createdAt ? ' · ' + S.fmtDate(p.createdAt) : '')}), msg, el('div', {class: 'g-list'}, [ex.row, cl.row, mb.row, hd.row]), el('div', {class: 'row', style: 'margin-top:10px'}, [b, el('button', {class: 'btn light', text: 'انصراف', onclick: function () { bg.remove(); }})])]));
    document.body.appendChild(bg);
  }

  /* ================================================================ صوت سؤال: ضبط / انتخاب فایل / آپلود (audio/<teacher>/<exam>/<uuid>.m4a) */
  var MAX_AUDIO = 3 * 1024 * 1024;
  async function uploadAudio(blob, examId, ext) {
    if (blob.size > MAX_AUDIO) throw new Error('حجم صوت حداکثر ۳ مگابایت است.');
    return S.uploadMedia(blob, 'audio', 'audio', examId, ext || 'm4a', blob.type || 'audio/mp4');
  }
  /* V145 — برای builder.js: تبدیل صوت data: هنگام ذخیرهٔ آزمون آنلاین */
  S.uploadAudioBlob = function (blob, examId) { return uploadAudio(blob, examId, extOf(blob)); };
  function extOf(blob) { var t = (blob.type || '').toLowerCase(); if (/mp4|m4a|aac/.test(t)) return 'm4a'; if (/webm/.test(t)) return 'webm'; if (/ogg|opus/.test(t)) return 'ogg'; if (/mpeg|mp3/.test(t)) return 'mp3'; if (/wav/.test(t)) return 'wav'; return 'm4a'; }
  function audioDlg(q, examId, isPrint, done) {
    var bg = el('div', {class: 'modal-bg'}); var msg = el('div'); var cur = el('div');
    var rec = null, chunks = [], stream = null, timer = null, startAt = 0, pending = null;
    var status = el('div', {class: 'muted', style: 'font-size:13px;min-height:20px'});
    var player = el('audio', {controls: 'controls', style: 'width:100%;display:none'});
    function showCur() { cur.innerHTML = ''; if (q.audio) cur.appendChild(el('div', {class: 'row'}, [el('audio', {controls: 'controls', src: q.audio, style: 'flex:1'}), el('button', {class: 'btn light sm', style: 'color:#c62828', text: 'حذف صوت', onclick: function () { q.audio = null; q.audioBytes = 0; q.audioMs = 0; done(); showCur(); }})])); else cur.appendChild(el('p', {class: 'muted', text: 'این سؤال صوت ندارد.'})); }
    showCur();
    var bRec = el('button', {class: 'btn', text: '⏺ شروع ضبط'});
    var bStop = el('button', {class: 'btn light', text: '⏹ پایان', disabled: 'disabled'});
    var bPick = el('button', {class: 'btn light', text: '📁 انتخاب فایل صوتی'});
    var bSave = el('button', {class: 'btn', text: '💾 ذخیره روی سؤال', style: 'display:none'});
    function setPending(blob) { pending = blob; player.src = URL.createObjectURL(blob); player.style.display = ''; bSave.style.display = ''; status.textContent = 'حجم: ' + fa((blob.size / 1024).toFixed(0)) + ' کیلوبایت' + (blob.size > MAX_AUDIO ? ' — بیش از حد مجاز (۳ مگابایت)' : ''); }
    bRec.addEventListener('click', async function () {
      msg.innerHTML = '';
      try {
        if (!navigator.mediaDevices || !window.MediaRecorder) throw new Error('مرورگر از ضبط صدا پشتیبانی نمی‌کند.');
        stream = await navigator.mediaDevices.getUserMedia({audio: true});
        var mime = ['audio/mp4', 'audio/webm;codecs=opus', 'audio/webm', 'audio/ogg;codecs=opus'].filter(function (m) { return MediaRecorder.isTypeSupported(m); })[0];
        rec = new MediaRecorder(stream, mime ? {mimeType: mime, audioBitsPerSecond: 48000} : undefined); chunks = [];
        rec.ondataavailable = function (e) { if (e.data && e.data.size) chunks.push(e.data); };
        rec.onstop = function () { stream.getTracks().forEach(function (t) { t.stop(); }); clearInterval(timer); setPending(new Blob(chunks, {type: rec.mimeType || mime || 'audio/webm'})); bRec.disabled = false; bStop.disabled = true; };
        rec.start(250); startAt = Date.now(); bRec.disabled = true; bStop.disabled = false;
        timer = setInterval(function () { var s = Math.floor((Date.now() - startAt) / 1000); status.textContent = '● در حال ضبط… ' + fa(Math.floor(s / 60)) + ':' + fa((s % 60 < 10 ? '0' : '') + (s % 60)); if (s >= 600) bStop.click(); }, 500);
      } catch (e) { msg.appendChild(el('div', {class: 'alert error', text: /Permission|NotAllowed/i.test(String(e && e.name || e)) ? 'دسترسی به میکروفون داده نشد.' : errMsg(e)})); }
    });
    bStop.addEventListener('click', function () { if (rec && rec.state !== 'inactive') rec.stop(); });
    bPick.addEventListener('click', async function () { var f = await pickFile('audio/*'); if (f) setPending(f); });
    bSave.addEventListener('click', async function () {
      msg.innerHTML = '';
      try {
        if (!pending) return;
        if (pending.size > MAX_AUDIO) throw new Error('حجم فایل صوتی بیش از ۳ مگابایت است.');
        bSave.disabled = true;
        var ms = await audioDuration(pending);
        var url;
        if (isPrint) url = await new Promise(function (res) { var r = new FileReader(); r.onload = function () { res(String(r.result)); }; r.readAsDataURL(pending); });
        else url = await uploadAudio(pending, examId, extOf(pending));
        q.audio = url; q.audioBytes = pending.size; q.audioMs = ms; done();
        bg.remove(); toast('صوت سؤال ذخیره شد.', 'ok');
      } catch (e) { msg.appendChild(el('div', {class: 'alert error', text: errMsg(e)})); bSave.disabled = false; }
    });
    bg.appendChild(el('div', {class: 'modal'}, [el('button', {class: 'x', text: '✕', onclick: function () { if (rec && rec.state !== 'inactive') rec.stop(); bg.remove(); }}), el('h2', {text: '🎙 صوت سؤال'}), msg, cur, el('hr'), el('div', {class: 'row', style: 'flex-wrap:wrap'}, [bRec, bStop, bPick]), status, player, el('div', {class: 'row', style: 'margin-top:8px'}, [bSave]), el('p', {class: 'muted', style: 'font-size:12px', text: 'حداکثر ۳ مگابایت (حدود ۱۰ دقیقه ضبط). دانش‌آموز صوت را کنار سؤال می‌شنود.'})]));
    document.body.appendChild(bg);
  }

  /* ================================================================ ورود با گوگل (OAuth مرورگر) + بازیابی رمز + حذف حساب */
  function googleButton(role) {
    return el('button', {class: 'btn light', style: 'width:100%;margin-top:8px', html: '<svg width="18" height="18" viewBox="0 0 48 48" style="vertical-align:-4px;margin-left:6px"><path fill="#EA4335" d="M24 9.5c3.5 0 6.6 1.2 9.1 3.6l6.8-6.8C35.8 2.4 30.3 0 24 0 14.6 0 6.5 5.4 2.6 13.2l7.9 6.1C12.4 13.4 17.7 9.5 24 9.5z"/><path fill="#4285F4" d="M46.5 24.5c0-1.6-.1-3.1-.4-4.5H24v9h12.7c-.6 3-2.3 5.5-4.8 7.2l7.7 6c4.5-4.2 6.9-10.3 6.9-17.7z"/><path fill="#FBBC05" d="M10.5 28.7A14.5 14.5 0 0 1 9.5 24c0-1.6.3-3.2.8-4.7l-7.9-6.1A24 24 0 0 0 0 24c0 3.9.9 7.5 2.6 10.8l7.9-6.1z"/><path fill="#34A853" d="M24 48c6.3 0 11.7-2.1 15.6-5.8l-7.7-6c-2.1 1.4-4.8 2.3-7.9 2.3-6.3 0-11.6-3.9-13.5-9.3l-7.9 6.1C6.5 42.6 14.6 48 24 48z"/></svg> ورود با گوگل', onclick: function () {
      try { sessionStorage.setItem('examsite.google.role', role || 'teacher'); } catch (e) {}
      var redirect = location.href.split('#')[0];
      if (/^file:/.test(redirect)) { toast('ورود با گوگل فقط وقتی سایت روی یک آدرس اینترنتی (https) باشد کار می‌کند، نه از روی فایل محلی.', 'err'); return; }
      location.href = S.config.url + '/auth/v1/authorize?provider=google&redirect_to=' + encodeURIComponent(redirect);
    }});
  }
  /* پس از بازگشت از گوگل: توکن‌ها در hash هستند */
  async function handleOAuthReturn() {
    var h = location.hash || '';
    if (!/access_token=/.test(h)) return false;
    var p = {}; h.replace(/^#/, '').split('&').forEach(function (kv) { var i = kv.indexOf('='); if (i > 0) p[decodeURIComponent(kv.slice(0, i))] = decodeURIComponent(kv.slice(i + 1)); });
    history.replaceState(null, '', location.pathname + location.search);
    if (!p.access_token) return false;
    var session = {access_token: p.access_token, refresh_token: p.refresh_token, expires_in: Number(p.expires_in) || 3600, expires_at: Math.floor(Date.now() / 1000) + (Number(p.expires_in) || 3600), token_type: p.token_type || 'bearer'};
    S.__setSession(session);
    /* hash فقط توکن دارد؛ کاربر را از سرور بگیر (currentProfile به session.user نیاز دارد) */
    try { session.user = await S.http('/auth/v1/user', {method: 'GET'}); S.__setSession(session); }
    catch (e) { S.__setSession(null); toast('ورود با گوگل ناتمام ماند: ' + errMsg(e), 'err'); return false; }
    var role = 'teacher'; try { role = sessionStorage.getItem('examsite.google.role') || 'teacher'; sessionStorage.removeItem('examsite.google.role'); } catch (e) {}
    try { var rr = await S.rpcObj('native_set_registration_role_v1', {p_role: role}); if (rr && rr.error) throw new Error(String(rr.error)); } catch (e) { if (!/function|not found|404|PGRST202/i.test(errMsg(e))) toast(errMsg(e), 'err'); }
    return true;
  }
  function recoveryFlow(m, api, setMsg, busy, onDone) {
    /* گام ۱: ایمیل → کد؛ گام ۲: کد → ورود؛ گام ۳: رمز جدید */
    var st = {step: 'email', email: ''};
    var box = el('div'); m.appendChild(box);
    function draw() {
      box.innerHTML = '';
      if (st.step === 'email') {
        var em = el('input', {type: 'email', placeholder: 'name@example.com', style: 'direction:ltr'});
        var b = el('button', {class: 'btn', text: 'ارسال کد بازیابی', style: 'width:100%'});
        b.addEventListener('click', async function () { setMsg(''); busy(b, true); try { st.email = em.value.trim(); if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(st.email)) throw new Error('ایمیل معتبر وارد کنید.'); await api.sendRecoveryOtp(st.email); st.step = 'code'; draw(); } catch (e) { setMsg(errMsg(e)); } busy(b, false); });
        box.appendChild(el('div', {class: 'field'}, [el('label', {text: 'ایمیل حساب'}), em])); box.appendChild(b);
      } else if (st.step === 'code') {
        box.appendChild(el('div', {class: 'alert info', text: 'کد بازیابی به ' + st.email + ' فرستاده شد.'}));
        var code = el('input', {type: 'text', placeholder: 'کد', style: 'direction:ltr'});
        var b2 = el('button', {class: 'btn', text: 'تأیید کد', style: 'width:100%'});
        b2.addEventListener('click', async function () { setMsg(''); busy(b2, true); try { st.username = await api.verifyRecoveryOtp(st.email, code.value); st.step = 'pw'; draw(); } catch (e) { setMsg(errMsg(e)); } busy(b2, false); });
        box.appendChild(el('div', {class: 'field'}, [el('label', {text: 'کد یک‌بارمصرف'}), code])); box.appendChild(b2);
      } else {
        if (st.username) box.appendChild(el('div', {class: 'alert ok', text: 'نام کاربری شما: ' + st.username}));
        var p1 = el('input', {type: 'password', placeholder: 'رمز جدید (حداقل ۸ کاراکتر)', style: 'direction:ltr'}), p2 = el('input', {type: 'password', placeholder: 'تکرار رمز', style: 'direction:ltr'});
        var b3 = el('button', {class: 'btn', text: 'ثبت رمز جدید و ورود', style: 'width:100%'});
        b3.addEventListener('click', async function () { setMsg(''); busy(b3, true); try { if (p1.value.length < 8) throw new Error('رمز باید حداقل ۸ کاراکتر باشد.'); if (p1.value !== p2.value) throw new Error('تکرار رمز یکسان نیست.'); await api.changePassword(p1.value); onDone(); } catch (e) { setMsg(errMsg(e)); } busy(b3, false); });
        box.appendChild(el('div', {class: 'field'}, [el('label', {text: 'رمز جدید'}), p1])); box.appendChild(el('div', {class: 'field'}, [el('label', {text: 'تکرار رمز'}), p2])); box.appendChild(b3);
      }
    }
    draw();
  }
  function deleteAccountCard(onDeleted) {
    var card = el('div', {class: 'card', style: 'border-color:#f3c1bd'}, [el('h3', {text: '🗑 حذف حساب'}), el('p', {class: 'muted', style: 'font-size:13px', text: 'همهٔ آزمون‌ها، کلاس‌ها و دانش‌آموزانِ فقط متعلق به شما برای همیشه حذف می‌شوند. دانش‌آموزانی که معلم دیگری هم دارند، حفظ می‌شوند. این کار برگشت‌ناپذیر است.'})]);
    var conf = el('input', {type: 'text', placeholder: 'برای تأیید بنویسید: حذف', style: 'max-width:200px'});
    var b = el('button', {class: 'btn danger', text: 'حذف کامل حساب', onclick: async function () {
      if (conf.value.trim() !== 'حذف') return toast('برای تأیید، کلمهٔ «حذف» را بنویسید.', 'err');
      if (!(await S.confirmDlg('حذف حساب', 'آیا مطمئن هستید؟ این کار برگشت‌ناپذیر است.', 'بله، حذف کن', true))) return;
      try { b.disabled = true; var r = await S.http('/functions/v1/manage-student', {method: 'POST', body: {action: 'delete_account'}}); if (r && r.error) { if (/عملیات ناشناخته/.test(String(r.error))) throw new Error('نسخهٔ سرور به‌روز نیست؛ تابع manage-student باید دوباره منتشر (deploy) شود.'); throw new Error(String(r.error)); } toast('حساب حذف شد.', 'ok'); onDeleted(); } catch (e) { toast(errMsg(e), 'err'); b.disabled = false; }
    }});
    card.appendChild(el('div', {class: 'row'}, [conf, b]));
    return card;
  }

  window.SiteExtras = {reportsPage: reportsPage, gradesExcelButton: gradesExcelButton, exportExamDlg: exportExamDlg, importExam: importExam, parseExamPackage: parseExamPackage, backupCard: backupCard, audioDlg: audioDlg, googleButton: googleButton, handleOAuthReturn: handleOAuthReturn, recoveryFlow: recoveryFlow, deleteAccountCard: deleteAccountCard, xlsx: xlsx, csv: csv, download: download};
})();
