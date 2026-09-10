/* ===================================================================
   فاز ۵ سایت — مدیریت دانش‌آموزان (ساخت حساب، ویرایش، رمز، حذف)،
   افزودن به کلاس، اشتراک با مدیر، پیوستن معلم به مدرسه با کد دعوت،
   درخواست‌های مدیر، مدیریت کامل بانک سؤال.
   قراردادها عیناً مطابق SupabaseSchoolRepository / SupabaseSchoolJoinRepository /
   SupabaseTeacherDashboardRepository / SupabaseExamBuilderRepository (بانک) و
   Edge Function manage-student.
   =================================================================== */
(function () {
  var S = window.ExamSite;
  if (!S) return;
  var el = S.el, esc = S.esc, fa = S.fa, toast = S.toast, errMsg = S.errMsg;
  function chk(r) { if (r && typeof r === 'object' && r.error) throw new Error(String(r.error)); return r || {}; }
  function fld(label, input, hint) { return el('div', {class: 'field'}, [el('label', {text: label}), input, hint ? el('div', {class: 'muted', style: 'font-size:12px', text: hint}) : null]); }
  function inp(attrs) { return el('input', Object.assign({type: 'text'}, attrs || {})); }
  function copyText(t) { try { navigator.clipboard.writeText(t); toast('کپی شد.', 'ok'); } catch (e) { toast('کپی ممکن نشد.', 'err'); } }
  function genPassword() { var a = 'abcdefghjkmnpqrstuvwxyz', d = '23456789', out = ''; for (var i = 0; i < 6; i++) out += a[Math.floor(Math.random() * a.length)]; for (var j = 0; j < 3; j++) out += d[Math.floor(Math.random() * d.length)]; return out; }
  var USERNAME_RE = /^[a-z0-9_]{4,20}$/;
  var GRADES = ['', 'اول', 'دوم', 'سوم', 'چهارم', 'پنجم', 'ششم', 'هفتم', 'هشتم', 'نهم', 'دهم', 'یازدهم', 'دوازدهم'];
  var FIELDS = ['', 'ریاضی', 'تجربی', 'انسانی', 'فنی و حرفه‌ای', 'کاردانش', 'معارف', 'هنر', 'عمومی'];
  function selectOf(list, value, emptyLabel) { var s = el('select'); list.forEach(function (v) { s.appendChild(el('option', {value: v, text: v || (emptyLabel || '—')})); }); if (value && list.indexOf(value) < 0) s.appendChild(el('option', {value: value, text: value})); s.value = value || ''; return s; }

  /* ---------------- Edge Function manage-student ---------------- */
  async function manageStudent(body) {
    var r = await S.http('/functions/v1/manage-student', {method: 'POST', body: body});
    return chk(r);
  }
  async function saveExtra(id, username, father, grade, field) {
    if (!father && !grade && !field) return;
    chk(await S.rpcObj('native_save_student_extra_v28', {p_student: id, p_username: username, p_father_name: father, p_grade: grade, p_field: field}));
  }

  /* ---------------- کارت «اطلاعات ورود» (رمز یک‌بارنمایش) ---------------- */
  function credentialDlg(title, creds) {
    var bg = el('div', {class: 'modal-bg'});
    var lines = creds.map(function (c) { return (c.name ? c.name + ' — ' : '') + 'نام کاربری: ' + c.username + '  رمز: ' + c.password; });
    var box = el('pre', {class: 'code', style: 'direction:ltr;text-align:left;white-space:pre-wrap;padding:12px;border-radius:10px;background:var(--bg);font-size:14px', text: lines.join('\n')});
    bg.appendChild(el('div', {class: 'modal'}, [el('h2', {text: title}), el('div', {class: 'alert warn', text: 'رمز فقط همین یک بار نمایش داده می‌شود و روی سرور به‌صورت خوانا ذخیره نیست. آن را کپی کنید و به دانش‌آموز بدهید.'}), box,
      el('div', {class: 'row'}, [el('button', {class: 'btn', text: '📋 کپی اطلاعات', onclick: function () { copyText(lines.join('\n')); }}), el('button', {class: 'btn light', text: 'بستن', onclick: function () { bg.remove(); }})])]));
    document.body.appendChild(bg);
  }

  /* ---------------- فرم دانش‌آموز (ساخت / ویرایش) ---------------- */
  async function studentForm(s, classes, defaultClass, done) {
    var isEdit = !!s;
    var bg = el('div', {class: 'modal-bg'}); var msg = el('div');
    var first = inp({value: s ? (s.first_name || '') : ''}), last = inp({value: s ? (s.last_name || '') : ''});
    if (isEdit && !s.first_name && s.full_name) { var parts = s.full_name.trim().split(/\s+/); first.value = parts.shift() || ''; last.value = parts.join(' '); }
    var un = inp({value: s ? (s.username || '') : '', style: 'direction:ltr', placeholder: 'a-z 0-9 _ (۴ تا ۲۰)'});
    var pw = inp({value: isEdit ? '' : genPassword(), style: 'direction:ltr', placeholder: isEdit ? 'خالی = بدون تغییر' : ''});
    var gen = el('div', {class: 'row', style: 'gap:6px'}); var gender = s ? (s.gender || '') : '';
    function drawGen() { gen.innerHTML = ''; [['male', 'پسر'], ['female', 'دختر']].forEach(function (g) { gen.appendChild(el('button', {type: 'button', class: 'chip ' + (gender === g[0] ? 'brand' : 'off'), text: g[1], onclick: function () { gender = g[0]; drawGen(); }})); }); }
    drawGen();
    var father = inp({value: s ? (s.father_name || '') : ''}), grade = selectOf(GRADES, s ? s.grade : ''), field = selectOf(FIELDS, s ? s.field_of_study : '');
    var cls = el('select'); cls.appendChild(el('option', {value: '', text: '— بدون کلاس —'})); (classes || []).forEach(function (k) { cls.appendChild(el('option', {value: k.id, text: k.name})); }); if (defaultClass) cls.value = defaultClass;
    var b = el('button', {class: 'btn', text: isEdit ? 'ذخیره' : 'ساخت حساب'});
    b.addEventListener('click', async function () {
      msg.innerHTML = '';
      try {
        var u = un.value.trim().toLowerCase();
        if (!first.value.trim()) throw new Error('نام دانش‌آموز را وارد کنید.');
        if (!USERNAME_RE.test(u)) throw new Error('نام کاربری باید ۴ تا ۲۰ کاراکتر انگلیسی، عدد یا _ باشد.');
        if (!isEdit && (pw.value.length < 8 || pw.value.length > 72)) throw new Error('رمز عبور باید بین ۸ تا ۷۲ کاراکتر باشد.');
        if (isEdit && pw.value && (pw.value.length < 8 || pw.value.length > 72)) throw new Error('رمز جدید باید بین ۸ تا ۷۲ کاراکتر باشد.');
        if (gender !== 'male' && gender !== 'female') throw new Error('جنسیت را انتخاب کنید.');
        b.disabled = true;
        if (isEdit) {
          await manageStudent({action: 'update', id: s.id, first_name: first.value.trim(), last_name: last.value.trim(), username: u, gender: gender, password: pw.value || ''});
          chk(await S.rpcObj('native_save_student_extra_v28', {p_student: s.id, p_username: u, p_father_name: father.value.trim(), p_grade: grade.value.trim(), p_field: field.value.trim()}));
          bg.remove(); toast('ذخیره شد.', 'ok');
          if (pw.value) credentialDlg('رمز جدید دانش‌آموز', [{name: first.value.trim() + ' ' + last.value.trim(), username: u, password: pw.value}]);
        } else {
          var r = await manageStudent({action: 'create', first_name: first.value.trim(), last_name: last.value.trim(), username: u, password: pw.value, gender: gender, class_id: cls.value || ''});
          if (!r.id) throw new Error('شناسه دانش‌آموز از سرور دریافت نشد.');
          await saveExtra(r.id, u, father.value.trim(), grade.value.trim(), field.value.trim());
          bg.remove(); toast('حساب ساخته شد.', 'ok');
          credentialDlg('اطلاعات ورود دانش‌آموز', [{name: first.value.trim() + ' ' + last.value.trim(), username: u, password: pw.value}]);
        }
        done();
      } catch (e) { msg.innerHTML = ''; msg.appendChild(el('div', {class: 'alert error', text: errMsg(e)})); }
      b.disabled = false;
    });
    bg.appendChild(el('div', {class: 'modal'}, [el('button', {class: 'x', text: '✕', onclick: function () { bg.remove(); }}), el('h2', {text: isEdit ? 'ویرایش دانش‌آموز' : 'دانش‌آموز جدید'}), msg,
      el('div', {class: 'grid2'}, [fld('نام', first), fld('نام خانوادگی', last)]),
      el('div', {class: 'grid2'}, [fld('نام کاربری', un), fld(isEdit ? 'رمز جدید (اختیاری)' : 'رمز عبور', el('div', {class: 'row', style: 'gap:6px'}, [pw, el('button', {type: 'button', class: 'btn light sm', text: '🎲', title: 'رمز تصادفی', onclick: function () { pw.value = genPassword(); }})]))]),
      fld('جنسیت', gen),
      el('div', {class: 'grid3'}, [fld('نام پدر (اختیاری)', father), fld('پایه', grade), fld('رشته', field)]),
      isEdit ? null : fld('کلاس', cls),
      el('div', {class: 'row', style: 'margin-top:8px'}, [b, el('button', {class: 'btn light', text: 'انصراف', onclick: function () { bg.remove(); }})])]));
    document.body.appendChild(bg);
  }

  /* ---------------- افزودن گروهی ---------------- */
  function bulkForm(classes, defaultClass, done) {
    var bg = el('div', {class: 'modal-bg'}); var msg = el('div');
    var ta = el('textarea', {rows: 8, placeholder: 'هر خط یک دانش‌آموز:\nنام، نام خانوادگی، نام کاربری، جنسیت(پسر/دختر)، رمز(اختیاری)\nمثال: علی، رضایی، ali_r, پسر', style: 'direction:rtl'});
    var cls = el('select'); cls.appendChild(el('option', {value: '', text: '— بدون کلاس —'})); (classes || []).forEach(function (k) { cls.appendChild(el('option', {value: k.id, text: k.name})); }); if (defaultClass) cls.value = defaultClass;
    var b = el('button', {class: 'btn', text: 'ساخت حساب‌ها'});
    b.addEventListener('click', async function () {
      msg.innerHTML = '';
      try {
        var rows = [], errors = [];
        ta.value.split(/\n/).map(function (l) { return l.trim(); }).filter(Boolean).forEach(function (line, i) {
          var p = line.split(/[،,;\t]+/).map(function (x) { return x.trim(); });
          var g = (p[3] || '').toLowerCase(); g = g === 'پسر' || g === 'male' || g === 'm' ? 'male' : (g === 'دختر' || g === 'female' || g === 'f' ? 'female' : '');
          var u = (p[2] || '').toLowerCase();
          if (!p[0] || !USERNAME_RE.test(u) || !g) { errors.push('خط ' + fa(i + 1) + ': ' + line); return; }
          rows.push({first_name: p[0], last_name: p[1] || '', username: u, password: p[4] && p[4].length >= 8 ? p[4] : genPassword(), gender: g});
        });
        if (!rows.length) throw new Error('هیچ ردیف معتبری پیدا نشد.' + (errors.length ? '\n' + errors.join('\n') : ''));
        if (errors.length && !(await S.confirmDlg('ردیف‌های نامعتبر', esc(errors.join('<br>')) + '<br><br>این ردیف‌ها نادیده گرفته شوند و بقیه ساخته شوند؟', 'ادامه'))) return;
        b.disabled = true;
        var r = await manageStudent({action: 'bulk', class_id: cls.value || '', rows: rows.map(function (x) { return {first_name: x.first_name, last_name: x.last_name, username: x.username, password: x.password, gender: x.gender}; })});
        var created = r.created || r.results || r.items || [], fails = r.failures || r.errors || [];
        var creds = [];
        (Array.isArray(created) ? created : []).forEach(function (c) { var row = rows.filter(function (x) { return x.username === (c.username || '').toLowerCase(); })[0]; if (row) creds.push({name: row.first_name + ' ' + row.last_name, username: row.username, password: row.password}); });
        if (!creds.length && !fails.length) creds = rows.map(function (x) { return {name: x.first_name + ' ' + x.last_name, username: x.username, password: x.password}; });
        bg.remove(); done();
        if (creds.length) credentialDlg('اطلاعات ورود ' + fa(creds.length) + ' دانش‌آموز', creds);
        if (fails.length) toast('ناموفق: ' + (Array.isArray(fails) ? fails.map(function (f) { return typeof f === 'string' ? f : (f.username || '') + ' ' + (f.error || ''); }).join('، ') : String(fails)), 'err');
      } catch (e) { msg.appendChild(el('div', {class: 'alert error', style: 'white-space:pre-wrap', text: errMsg(e)})); }
      b.disabled = false;
    });
    bg.appendChild(el('div', {class: 'modal'}, [el('button', {class: 'x', text: '✕', onclick: function () { bg.remove(); }}), el('h2', {text: 'افزودن گروهی دانش‌آموزان'}), msg, fld('فهرست', ta, 'جداکننده: ویرگول فارسی/انگلیسی یا Tab. رمز خالی = تولید خودکار.'), fld('کلاس', cls), el('div', {class: 'row'}, [b, el('button', {class: 'btn light', text: 'انصراف', onclick: function () { bg.remove(); }})])]));
    document.body.appendChild(bg);
  }

  /* ---------------- انتخاب کلاس‌ها برای یک دانش‌آموز ---------------- */
  async function classPickDlg(s, classes, done) {
    var bg = el('div', {class: 'modal-bg'}); var msg = el('div');
    var current = String(s.class_names || '').split(/[،,]/).map(function (x) { return x.trim(); }).filter(Boolean);
    var checks = classes.map(function (k) { var c = el('input', {type: 'checkbox'}); c.checked = current.indexOf(k.name) >= 0; return {k: k, c: c}; });
    var b = el('button', {class: 'btn', text: 'ذخیره'});
    b.addEventListener('click', async function () {
      try { b.disabled = true; var ids = checks.filter(function (x) { return x.c.checked; }).map(function (x) { return x.k.id; }); chk(await S.rpcObj('native_add_student_to_classes_v22', {p_student: s.id, p_classes: ids})); bg.remove(); toast('ذخیره شد.', 'ok'); done(); }
      catch (e) { msg.innerHTML = ''; msg.appendChild(el('div', {class: 'alert error', text: errMsg(e)})); b.disabled = false; }
    });
    bg.appendChild(el('div', {class: 'modal'}, [el('button', {class: 'x', text: '✕', onclick: function () { bg.remove(); }}), el('h2', {text: 'کلاس‌های ' + (s.full_name || '')}), msg,
      classes.length ? el('div', {class: 'g-list', style: 'max-height:50vh'}, checks.map(function (x) { return el('label', {class: 'g-item'}, [x.c, el('span', {text: x.k.name + (x.k.grade ? ' · ' + x.k.grade : '')})]); })) : S.emptyBox('🏫', 'کلاسی ندارید.'),
      el('div', {class: 'row', style: 'margin-top:8px'}, [b, el('button', {class: 'btn light', text: 'انصراف', onclick: function () { bg.remove(); }})])]));
    document.body.appendChild(bg);
  }

  /* ---------------- «افزودن موجود» به کلاس ---------------- */
  async function addExistingDlg(k, refresh) {
    var bg = el('div', {class: 'modal-bg'}); var body = el('div'); S.loading(body);
    var picked = {};
    var add = el('button', {class: 'btn', text: 'افزودن'});
    var head = el('div', {class: 'row', style: 'margin-bottom:10px'}, [el('button', {class: 'btn light', style: 'color:#c62828', text: 'انصراف', onclick: function () { bg.remove(); }}), el('h2', {class: 'grow', style: 'margin:0;text-align:center;font-size:16px', text: 'افزودن به ' + k.name}), add]);
    bg.appendChild(el('div', {class: 'modal wide'}, [head, body])); document.body.appendChild(bg);
    try {
      var r = await Promise.all([S.rpc('my_students', {}), S.rpc('class_roster', {p_class: k.id})]);
      var inClass = {}; (r[1] || []).forEach(function (s) { inClass[s.id] = true; });
      var all = (r[0] || []).filter(function (s) { return !inClass[s.id]; });
      var q = el('input', {type: 'search', placeholder: 'جست‌وجو…', style: 'flex:1'}); var gsel = selectOf(['', 'پسر', 'دختر'], '', 'جنسیت'), grsel = selectOf(GRADES, '', 'پایه'), fsel = selectOf(FIELDS, '', 'رشته');
      var lst = el('div', {class: 'g-list', style: 'max-height:55vh'}); var cnt = el('span', {class: 'muted'});
      function draw() {
        var s = q.value.trim().toLowerCase(), g = gsel.value === 'پسر' ? 'male' : (gsel.value === 'دختر' ? 'female' : '');
        lst.innerHTML = '';
        var f = all.filter(function (x) { return (!s || [x.full_name, x.username, x.class_names].join(' ').toLowerCase().indexOf(s) >= 0) && (!g || x.gender === g) && (!grsel.value || x.grade === grsel.value) && (!fsel.value || x.field_of_study === fsel.value); });
        if (!f.length) lst.appendChild(S.emptyBox('🎓', 'دانش‌آموزی برای افزودن نیست.'));
        f.forEach(function (x) { var c = el('input', {type: 'checkbox'}); c.checked = !!picked[x.id]; c.addEventListener('change', function () { if (c.checked) picked[x.id] = true; else delete picked[x.id]; cnt.textContent = fa(Object.keys(picked).length) + ' انتخاب'; }); lst.appendChild(el('label', {class: 'g-item'}, [c, el('span', {class: 'grow', text: x.full_name}), el('span', {class: 'muted', style: 'font-size:12px', text: [x.username, x.grade, x.class_names].filter(Boolean).join(' · ')})])); });
      }
      [q, gsel, grsel, fsel].forEach(function (x) { x.addEventListener('input', draw); x.addEventListener('change', draw); });
      body.innerHTML = ''; body.appendChild(el('div', {class: 'row', style: 'margin-bottom:10px;flex-wrap:wrap'}, [q, gsel, grsel, fsel, cnt])); body.appendChild(lst); draw();
      add.addEventListener('click', async function () {
        var ids = Object.keys(picked); if (!ids.length) return toast('کسی انتخاب نشده.', 'err');
        try { add.disabled = true; var res = chk(await S.rpcObj('add_students_to_class', {p_class: k.id, p_students: ids})); bg.remove(); toast(fa(res.added != null ? res.added : ids.length) + ' نفر افزوده شد.', 'ok'); refresh(); } catch (e) { toast(errMsg(e), 'err'); add.disabled = false; }
      });
    } catch (e) { S.showErr(body, e); }
  }

  /* ---------------- جدول دانش‌آموزان با عملیات ---------------- */
  function studentTable(list, classes, ctx) {
    /* ctx: {classId?, refresh} */
    if (!list || !list.length) return S.emptyBox('🎓', 'دانش‌آموزی ثبت نشده است.');
    return el('table', {class: 'tbl'}, [
      el('thead', {}, [el('tr', {}, ['نام', 'نام کاربری', 'جنسیت', 'پایه', 'رشته', 'کلاس‌ها', 'وضعیت', 'اشتراک با مدیر', ''].map(function (h) { return el('th', {text: h}); }))]),
      el('tbody', {}, list.map(function (s) {
        var canManage = s.can_manage !== false;
        var acts = el('div', {class: 'acts'});
        if (canManage) acts.appendChild(el('button', {class: 'icon-btn', title: 'ویرایش', html: '✎', onclick: function () { studentForm(s, classes, null, ctx.refresh); }}));
        acts.appendChild(el('button', {class: 'icon-btn', title: 'کلاس‌ها', html: '🏫', onclick: function () { classPickDlg(s, classes, ctx.refresh); }}));
        if (canManage) acts.appendChild(el('button', {class: 'icon-btn', title: 'رمز جدید', html: '🔑', onclick: async function () {
          var np = genPassword(); if (!(await S.confirmDlg('رمز جدید', 'رمز جدید برای «' + esc(s.full_name || '') + '» ساخته شود؟ رمز قبلی از کار می‌افتد.', 'بساز'))) return;
          try { await manageStudent({action: 'reset_password', id: s.id, password: np}); credentialDlg('رمز جدید دانش‌آموز', [{name: s.full_name, username: s.username, password: np}]); } catch (e) { toast(errMsg(e), 'err'); }
        }}));
        acts.appendChild(el('button', {class: 'icon-btn', title: s.is_active !== false ? 'غیرفعال کردن' : 'فعال کردن', html: s.is_active !== false ? '⏸' : '▶', onclick: async function () { try { chk(await S.rpcObj('set_student_active', {p_student: s.id, p_active: s.is_active === false})); ctx.refresh(); } catch (e) { toast(errMsg(e), 'err'); } }}));
        if (ctx.classId) acts.appendChild(el('button', {class: 'icon-btn danger', title: 'خروج از این کلاس', html: '➖', onclick: async function () { if (!(await S.confirmDlg('حذف از کلاس', '«' + esc(s.full_name || '') + '» از این کلاس خارج شود؟ حساب حفظ می‌ماند.', 'خروج', true))) return; try { chk(await S.rpcObj('remove_student_from_class', {p_class: ctx.classId, p_student: s.id})); ctx.refresh(); } catch (e) { toast(errMsg(e), 'err'); } }}));
        if (ctx.classId && s.in_my_list === false) acts.appendChild(el('button', {class: 'icon-btn', title: 'افزودن به فهرست من', html: '➕', onclick: async function () { try { chk(await S.rpcObj('native_teacher_add_class_student_to_list_v43', {p_class: ctx.classId, p_student: s.id})); toast('به فهرست شما افزوده شد.', 'ok'); ctx.refresh(); } catch (e) { toast(errMsg(e), 'err'); } }}));
        if (canManage) acts.appendChild(el('button', {class: 'icon-btn danger', title: 'حذف حساب', html: '🗑', onclick: async function () { if (!(await S.confirmDlg('حذف حساب دانش‌آموز', 'حساب «' + esc(s.full_name || '') + '» و پاسخ‌هایش برای همیشه حذف می‌شود.', 'حذف کامل', true))) return; try { await manageStudent({action: 'delete', id: s.id}); toast('حذف شد.', 'ok'); ctx.refresh(); } catch (e) { toast(errMsg(e), 'err'); } }}));
        var shareBtn = el('button', {class: 'chip ' + (s.shared_with_manager ? 'ok' : 'off'), text: s.shared_with_manager ? 'بله' : 'خیر', title: 'تغییر اشتراک با مدیر', onclick: async function () { try { var r = chk(await S.rpcObj('native_teacher_share_student_v136', {p_student: s.id, p_share: !s.shared_with_manager})); var eff = r.shared != null ? String(r.shared) === 'true' : !s.shared_with_manager; toast(eff ? 'با مدیر به اشتراک گذاشته شد.' : 'اشتراک برداشته شد.', 'ok'); if (r.message) toast(String(r.message), 'info'); ctx.refresh(); } catch (e) { toast(errMsg(e), 'err'); } }});
        return el('tr', {}, [el('td', {html: '<b>' + esc(s.full_name || ((s.first_name || '') + ' ' + (s.last_name || ''))) + '</b>' + (s.father_name ? '<div class="muted" style="font-size:11px">فرزند ' + esc(s.father_name) + '</div>' : '')}), el('td', {}, [el('span', {class: 'code', text: s.username || '—'})]),
          el('td', {text: s.gender === 'male' ? 'پسر' : (s.gender === 'female' ? 'دختر' : '—')}), el('td', {text: s.grade || '—'}), el('td', {text: s.field_of_study || '—'}), el('td', {class: 'muted', text: s.class_names || '—'}),
          el('td', {}, [el('span', {class: 'chip ' + (s.is_active !== false ? 'ok' : 'off'), text: s.is_active !== false ? 'فعال' : 'غیرفعال'})]), el('td', {}, [shareBtn]), el('td', {}, [acts])]);
      }))
    ]);
  }

  /* ---------------- صفحهٔ دانش‌آموزان ---------------- */
  async function studentsPage(c) {
    S.loading(c);
    try {
      var r = await Promise.all([S.rpc('my_students', {}), S.rpc('native_my_classes_v28', {})]);
      var list = r[0] || [], classes = r[1] || [];
      c.innerHTML = '';
      var q = el('input', {type: 'search', placeholder: 'جست‌وجوی نام، نام کاربری، پایه یا کلاس…', style: 'min-width:260px'});
      var gsel = selectOf(['', 'پسر', 'دختر'], '', 'همه (جنسیت)'), grsel = selectOf(GRADES, '', 'همهٔ پایه‌ها'), csel = el('select'); csel.appendChild(el('option', {value: '', text: 'همهٔ کلاس‌ها'})); classes.forEach(function (k) { csel.appendChild(el('option', {value: k.name, text: k.name})); });
      var box = el('div', {class: 'card'}); var cnt = el('span', {class: 'muted'});
      function refresh() { studentsPage(c); }
      function draw() {
        var s = q.value.trim().toLowerCase(), g = gsel.value === 'پسر' ? 'male' : (gsel.value === 'دختر' ? 'female' : '');
        var f = list.filter(function (x) { return (!s || [x.full_name, x.username, x.class_names, x.grade].join(' ').toLowerCase().indexOf(s) >= 0) && (!g || x.gender === g) && (!grsel.value || x.grade === grsel.value) && (!csel.value || String(x.class_names || '').indexOf(csel.value) >= 0); });
        cnt.textContent = fa(f.length) + ' از ' + fa(list.length) + ' دانش‌آموز';
        box.innerHTML = ''; box.appendChild(studentTable(f, classes, {refresh: refresh}));
      }
      [q, gsel, grsel, csel].forEach(function (x) { x.addEventListener('input', draw); x.addEventListener('change', draw); });
      c.appendChild(el('div', {class: 'row', style: 'margin-bottom:8px'}, [el('span', {class: 'grow'}), cnt, el('button', {class: 'btn light', text: '👥 افزودن گروهی', onclick: function () { bulkForm(classes, null, refresh); }}), el('button', {class: 'btn', text: '➕ دانش‌آموز جدید', onclick: function () { studentForm(null, classes, null, refresh); }})]));
      c.appendChild(el('div', {class: 'row', style: 'margin-bottom:12px;flex-wrap:wrap'}, [q, gsel, grsel, csel]));
      c.appendChild(box); draw();
    } catch (e) { S.showErr(c, e); }
  }

  /* ---------------- فهرست کلاس (roster) ---------------- */
  async function rosterDlg(k) {
    var bg = el('div', {class: 'modal-bg', onclick: function (e) { if (e.target === bg) bg.remove(); }});
    var body = el('div'); S.loading(body);
    bg.appendChild(el('div', {class: 'modal wide'}, [el('button', {class: 'x', text: '✕', onclick: function () { bg.remove(); }}), el('h2', {text: '👥 دانش‌آموزان کلاس ' + k.name}), body]));
    document.body.appendChild(bg);
    async function load() {
      S.loading(body);
      try {
        var r = await Promise.all([S.rpc('class_roster', {p_class: k.id}), S.rpc('native_my_classes_v28', {})]);
        var classes = r[1] || [];
        body.innerHTML = '';
        body.appendChild(el('div', {class: 'row', style: 'margin-bottom:10px'}, [el('span', {class: 'muted', text: fa((r[0] || []).length) + ' نفر'}), el('span', {class: 'grow'}), el('button', {class: 'btn light sm', text: '📋 افزودن موجود', onclick: function () { addExistingDlg(k, load); }}), el('button', {class: 'btn light sm', text: '👥 گروهی', onclick: function () { bulkForm(classes, k.id, load); }}), el('button', {class: 'btn sm', text: '➕ دانش‌آموز جدید', onclick: function () { studentForm(null, classes, k.id, load); }})]));
        body.appendChild(studentTable(r[0] || [], classes, {classId: k.id, refresh: load}));
      } catch (e) { S.showErr(body, e); }
    }
    load();
  }
  /* دکمهٔ اشتراک کلاس با مدیر (در جدول کلاس‌ها) */
  function classShareChip(k, refresh) {
    return el('button', {class: 'chip ' + (k.shared_with_manager ? 'ok' : 'off'), text: k.shared_with_manager ? 'بله' : 'خیر', title: 'تغییر اشتراک با مدیر', onclick: async function () {
      try { var r = chk(await S.rpcObj('native_teacher_share_class_v137', {p_class: k.id, p_share: !k.shared_with_manager})); var eff = r.shared != null ? String(r.shared) === 'true' : !k.shared_with_manager; toast(eff ? 'کلاس با مدیر به اشتراک گذاشته شد.' : 'اشتراک برداشته شد.', 'ok'); if (r.message) toast(String(r.message), 'info'); refresh(); } catch (e) { toast(errMsg(e), 'err'); }
    }});
  }

  /* ---------------- پیوستن به مدرسه (پروفایل معلم) ---------------- */
  function joinSchoolCard(refresh) {
    var card = el('div', {class: 'card'}, [el('h3', {text: '🏫 مدرسه‌های من'})]);
    var list = el('div'); card.appendChild(list);
    S.rpcObj('native_teacher_schools_v61', {}).then(function (r) { var items = r.items || []; list.innerHTML = ''; if (!items.length) list.appendChild(el('p', {class: 'muted', text: 'هنوز عضو مدرسه‌ای نیستید.'})); else items.forEach(function (s) { list.appendChild(el('div', {class: 'chip brand', style: 'margin:2px', text: s.name + (s.city ? ' — ' + s.city : '')})); }); }).catch(function () {});
    var code = inp({style: 'direction:ltr;text-transform:uppercase;max-width:180px', maxlength: '6', placeholder: 'ABC123'}); var msg = el('div'); var prev = el('div');
    var bp = el('button', {class: 'btn light', text: 'بررسی کد'});
    var bj = el('button', {class: 'btn', text: 'تأیید و پیوستن', style: 'display:none'});
    bp.addEventListener('click', async function () {
      msg.innerHTML = ''; prev.innerHTML = ''; bj.style.display = 'none';
      var cd = code.value.trim().toUpperCase();
      if (!/^[A-Z0-9]{6}$/.test(cd)) return msg.appendChild(el('div', {class: 'alert error', text: 'کد دعوت باید ۶ حرف یا عدد باشد.'}));
      try { bp.disabled = true; var r = chk(await S.rpcObj('native_school_invite_preview_v39', {p_code: cd})); prev.appendChild(el('div', {class: 'alert info', text: 'مدرسه: ' + (r.school_name || '—') + (r.province || r.city ? ' (' + [r.province, r.city].filter(Boolean).join('، ') + ')' : '')})); bj.style.display = ''; }
      catch (e) { msg.appendChild(el('div', {class: 'alert error', text: errMsg(e)})); }
      bp.disabled = false;
    });
    bj.addEventListener('click', async function () {
      try { bj.disabled = true; var r = chk(await S.rpcObj('native_join_school_v39', {p_code: code.value.trim().toUpperCase()})); toast('به مدرسهٔ «' + (r.school_name || '') + '» پیوستید.', 'ok'); if (refresh) refresh(); }
      catch (e) { msg.innerHTML = ''; msg.appendChild(el('div', {class: 'alert error', text: errMsg(e)})); bj.disabled = false; }
    });
    card.appendChild(el('p', {class: 'muted', style: 'font-size:13px;margin-top:8px', text: 'کد دعوت ۶ کاراکتری مدیر/معاون را وارد کنید.'}));
    card.appendChild(el('div', {class: 'row', style: 'flex-wrap:wrap'}, [code, bp, bj])); card.appendChild(prev); card.appendChild(msg);
    return card;
  }

  /* ---------------- درخواست‌های مدیر (داشبورد معلم) ---------------- */
  async function managerRequestsCard() {
    var card = el('div', {class: 'card'}, [el('h3', {text: '📩 درخواست‌های مدیر'})]);
    var body = el('div'); card.appendChild(body);
    var STATUS = {pending: 'در انتظار', approved: 'تأیید‌شده', rejected: 'رد‌شده', expired: 'منقضی', applied: 'اعمال‌شده'};
    async function load() {
      S.loading(body);
      try {
        var r = chk(await S.rpcObj('native_teacher_manager_requests_v41', {}));
        var items = r.items || [];
        body.innerHTML = '';
        if (!items.length) { body.appendChild(el('p', {class: 'muted', text: 'درخواست در انتظار تأیید ندارید.'})); card.style.display = 'none'; return; }
        card.style.display = '';
        items.forEach(function (it) {
          var pending = it.status === 'pending' || !it.status;
          body.appendChild(el('div', {class: 'g-q'}, [el('div', {class: 'row'}, [el('b', {class: 'grow', text: (it.target_type === 'class' ? 'کلاس' : 'حساب دانش‌آموز') + ' · ' + (it.action === 'delete' ? 'حذف' : 'ویرایش') + (it.manager_name ? ' · از طرف ' + it.manager_name : '')}), el('span', {class: 'chip ' + (pending ? 'warn' : 'off'), text: STATUS[it.status] || it.status || 'در انتظار'})]),
            it.expires_at ? el('div', {class: 'muted', style: 'font-size:12px', text: 'مهلت: ' + S.fmtDate(it.expires_at)}) : null,
            pending ? el('div', {class: 'row', style: 'margin-top:6px'}, [el('button', {class: 'btn sm', text: 'تأیید', onclick: function () { decide(it.id, true); }}), el('button', {class: 'btn light sm', text: 'رد', onclick: function () { decide(it.id, false); }})]) : null]));
        });
      } catch (e) { body.innerHTML = ''; body.appendChild(el('p', {class: 'muted', style: 'font-size:12px', text: 'درخواست‌های مدیر در دسترس نیست.'})); card.style.display = 'none'; }
    }
    async function decide(id, ok) { try { chk(await S.rpcObj('native_teacher_decide_manager_request_v41', {p_request: id, p_approve: ok})); toast(ok ? 'تأیید شد.' : 'رد شد.', 'ok'); load(); } catch (e) { toast(errMsg(e), 'err'); } }
    load();
    return card;
  }

  /* ---------------- بانک سؤال (صفحهٔ مستقل) ---------------- */
  async function bankPage(c) {
    S.loading(c);
    var B = window.SiteBuilder;
    try {
      var raw = chk(await S.rpcObj('native_bank_snapshot_v1', {}));
      var items = raw.items || [], cats = raw.categories || [];
      c.innerHTML = '';
      var TYPE = {multiple: 'چندگزینه‌ای', truefalse: 'صحیح/غلط', fill: 'جای‌خالی', numeric: 'عددی', matching: 'جورکردنی', essay: 'تشریحی', long: 'تشریحی'};
      var q = el('input', {type: 'search', placeholder: 'جست‌وجو در متن یا درس…', style: 'min-width:240px'});
      var cs = el('select'); cs.appendChild(el('option', {value: '', text: 'همهٔ دسته‌ها'})); cats.forEach(function (k) { cs.appendChild(el('option', {value: String(k.id), text: k.name + ' (' + fa(k.count || 0) + ')'})); });
      var cnt = el('span', {class: 'muted'}); var lst = el('div', {class: 'card'});
      function refresh() { bankPage(c); }
      function draw() {
        var s = q.value.trim().toLowerCase(), cid = cs.value ? Number(cs.value) : null;
        var f = items.filter(function (it) { var qq = it.question || {}; return (!s || String(qq.text || '').toLowerCase().indexOf(s) >= 0 || String(it.subject || '').toLowerCase().indexOf(s) >= 0) && (!cid || (it.cat_ids || []).indexOf(cid) >= 0); });
        cnt.textContent = fa(f.length) + ' از ' + fa(items.length) + ' سؤال';
        lst.innerHTML = '';
        if (!f.length) { lst.appendChild(S.emptyBox('🏦', 'سؤالی در بانک نیست. از سازندهٔ آزمون با «ذخیرهٔ سؤال جاری در بانک» اضافه کنید.')); return; }
        f.forEach(function (it) {
          var qq = it.question || {};
          lst.appendChild(el('div', {class: 'b-bank-item'}, [el('div', {class: 'grow'}, [el('div', {text: String(qq.text || '').replace(/\$/g, '').slice(0, 160)}), el('div', {class: 'muted', style: 'font-size:12px', text: [TYPE[S.qType(qq.type)] || '', it.subject, (it.cat_names || []).join('، '), it.created_at ? S.fmtDate(it.created_at) : ''].filter(Boolean).join(' · ')})]),
            el('div', {class: 'acts'}, [
              el('button', {class: 'icon-btn', title: 'دسته‌ها', html: '🏷', onclick: function () { catPick(it); }}),
              el('button', {class: 'icon-btn', title: 'ویرایش در سازنده', html: '✎', onclick: function () { if (!B) return toast('سازندهٔ آزمون در دسترس نیست.', 'err'); S.go('builder', {bankEdit: {id: it.id, subject: it.subject || '', cats: it.cat_ids || [], question: qq}}); }}),
              el('button', {class: 'icon-btn danger', title: 'حذف', html: '🗑', onclick: async function () { if (!(await S.confirmDlg('حذف سؤال', 'این سؤال از بانک حذف شود؟', 'حذف', true))) return; try { chk(await S.rpcObj('native_bank_delete_question_v1', {p_id: it.id})); toast('حذف شد.', 'ok'); refresh(); } catch (e) { toast(errMsg(e), 'err'); } }})])]));
        });
      }
      function catPick(it) {
        var bg = el('div', {class: 'modal-bg'}); var checks = cats.map(function (k) { var c = el('input', {type: 'checkbox'}); c.checked = (it.cat_ids || []).indexOf(k.id) >= 0; return {k: k, c: c}; });
        bg.appendChild(el('div', {class: 'modal'}, [el('button', {class: 'x', text: '✕', onclick: function () { bg.remove(); }}), el('h2', {text: 'دسته‌های سؤال'}), checks.length ? el('div', {class: 'g-list', style: 'max-height:50vh'}, checks.map(function (x) { return el('label', {class: 'g-item'}, [x.c, el('span', {text: x.k.name})]); })) : el('p', {class: 'muted', text: 'دسته‌ای ندارید.'}),
          el('div', {class: 'row', style: 'margin-top:8px'}, [el('button', {class: 'btn', text: 'ذخیره', onclick: async function () { try { chk(await S.rpcObj('native_bank_set_categories_v1', {p_id: it.id, p_cats: checks.filter(function (x) { return x.c.checked; }).map(function (x) { return x.k.id; }).sort(function (a, b) { return a - b; })})); bg.remove(); toast('ذخیره شد.', 'ok'); refresh(); } catch (e) { toast(errMsg(e), 'err'); } }}), el('button', {class: 'btn light', text: 'انصراف', onclick: function () { bg.remove(); }})])]));
        document.body.appendChild(bg);
      }
      function manageCats() {
        var bg = el('div', {class: 'modal-bg'}); var body = el('div');
        function drawCats() {
          body.innerHTML = '';
          if (!cats.length) body.appendChild(el('p', {class: 'muted', text: 'هنوز دسته‌ای نساخته‌اید.'}));
          cats.forEach(function (k) { body.appendChild(el('div', {class: 'g-item'}, [el('span', {class: 'grow', text: k.name}), el('span', {class: 'muted', style: 'font-size:12px', text: fa(k.count || 0) + ' سؤال'}), el('button', {class: 'icon-btn danger', html: '🗑', onclick: async function () {
            var delQ = false;
            if ((k.count || 0) > 0) { var r = await S.confirmDlg('حذف دسته', 'دستهٔ «' + esc(k.name) + '» ' + fa(k.count) + ' سؤال دارد. سؤال‌ها هم حذف شوند؟ (با «فقط دسته» سؤال‌ها بدون دسته می‌مانند)', 'حذف با سؤال‌ها', true); if (r) delQ = true; else if (!(await S.confirmDlg('حذف دسته', 'فقط خودِ دسته حذف شود؟', 'فقط دسته'))) return; }
            else if (!(await S.confirmDlg('حذف دسته', 'دستهٔ «' + esc(k.name) + '» حذف شود؟', 'حذف', true))) return;
            try { chk(await S.rpcObj('native_bank_category_delete_v1', {p_id: k.id, p_delete_questions: delQ})); bg.remove(); toast('حذف شد.', 'ok'); refresh(); } catch (e) { toast(errMsg(e), 'err'); }
          }})])); });
        }
        drawCats();
        var nm = inp({placeholder: 'نام دستهٔ جدید'});
        bg.appendChild(el('div', {class: 'modal'}, [el('button', {class: 'x', text: '✕', onclick: function () { bg.remove(); }}), el('h2', {text: '🏷 دسته‌های بانک'}), body,
          el('div', {class: 'row', style: 'margin-top:10px'}, [nm, el('button', {class: 'btn sm', text: 'افزودن', onclick: async function () { if (!nm.value.trim()) return; try { chk(await S.rpcObj('native_bank_category_add_v1', {p_name: nm.value.trim()})); bg.remove(); toast('دسته افزوده شد.', 'ok'); refresh(); } catch (e) { toast(errMsg(e), 'err'); } }})])]));
        document.body.appendChild(bg);
      }
      q.addEventListener('input', draw); cs.addEventListener('change', draw);
      c.appendChild(el('div', {class: 'row', style: 'margin-bottom:12px;flex-wrap:wrap'}, [q, cs, el('span', {class: 'grow'}), cnt, el('button', {class: 'btn light', text: '🏷 دسته‌ها', onclick: manageCats}), el('button', {class: 'btn', text: '➕ سؤال جدید در بانک', onclick: function () { S.go('builder', {bankEdit: {id: null, subject: '', cats: [], question: null}}); }})]));
      c.appendChild(lst); draw();
    } catch (e) { S.showErr(c, e); }
  }

  window.SiteSchool = {studentsPage: studentsPage, rosterDlg: rosterDlg, classShareChip: classShareChip, joinSchoolCard: joinSchoolCard, managerRequestsCard: managerRequestsCard, bankPage: bankPage, studentForm: studentForm, bulkForm: bulkForm, manageStudent: manageStudent};
})();
