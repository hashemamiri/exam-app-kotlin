/* V138 — سایت تک‌فایلی آزمون‌ساز (فاز ۱)
   همان بک‌اندِ Supabase برنامهٔ اندروید؛ کلید anon را کاربر در بالای فایل index.html می‌گذارد.
   قراردادها عیناً از data/repository/*.kt آینه شده‌اند (نام RPCها و کلیدهای JSON). */
(function () {
  'use strict';

  /* ================================================================ پیکربندی */
  var CFG = window.SITE_CONFIG || {};
  var SUPABASE_URL = String(CFG.SUPABASE_URL || '').replace(/\/+$/, '');
  var ANON = String(CFG.SUPABASE_ANON_KEY || '').trim();
  var KEY_READY = ANON && ANON !== '…' && ANON.indexOf('اینجا') < 0 && ANON.length > 20;
  var STUDENT_DOMAIN = 'student.exam.local';
  var USERNAME_RE = /^[a-z0-9_]{4,20}$/;
  var EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  var LS_SESSION = 'examsite.session.v1';
  var LS_PAGESETUP = 'examsite.pagesetup.v1';
  var LS_LOCALSTATE = 'examsite.local.v1';
  var PRINT_COST_PER_Q = 1000;

  /* ================================================================ ابزار */
  function $(id) { return document.getElementById(id); }
  /* V160 — رسانهٔ Storage خصوصی (باکت exam-images از V75.8 public=false): <img src> و <audio src> ساده 400 می‌گیرند.
     مثل SupabaseAuthImageInterceptor/QuestionAudioPlayer اپ، با هدر نشست دانلود و به blob: تبدیل می‌شود (کش در حافظه). */
  var mediaCache = {}, lastMediaError = '';
  function isOwnStorageUrl(u) { return typeof u === 'string' && SUPABASE_URL && u.indexOf(SUPABASE_URL + '/storage/v1/object/') === 0; }
  function mediaBlobUrl(u) {
    if (!isOwnStorageUrl(u)) return Promise.resolve(u);
    if (mediaCache[u]) return mediaCache[u];
    var authed = u.replace('/storage/v1/object/public/', '/storage/v1/object/authenticated/');
    mediaCache[u] = fetch(authed, {headers: {'apikey': ANON, 'Authorization': 'Bearer ' + (session ? session.access_token : ANON)}}).then(function (r) {
      if (!r.ok) throw new Error('HTTP ' + r.status);
      return r.blob();
    }).then(function (b) { return URL.createObjectURL(b); }).catch(function (e) { delete mediaCache[u]; console.error('media', u, e); lastMediaError = 'Storage ' + (e && e.message || ''); return u; });
    return mediaCache[u];
  }
  function el(tag, attrs, children) {
    var e = document.createElement(tag);
    if (attrs) Object.keys(attrs).forEach(function (k) {
      if (k === 'src' && (tag === 'img' || tag === 'audio') && isOwnStorageUrl(attrs[k])) { e.setAttribute('data-src', attrs[k]); mediaBlobUrl(attrs[k]).then(function (u) { e.src = u; if (u === attrs[k]) e.title = 'بارگذاری نشد: ' + lastMediaError; }); }
      else if (k === 'src' && tag === 'img' && /^https?:/i.test(String(attrs[k]))) { e.setAttribute(k, attrs[k]); e.addEventListener('error', function () { var h = ''; try { h = new URL(attrs[k]).host; } catch (x) {} e.title = 'تصویر بارگذاری نشد (' + h + ')'; e.classList.add('img-broken'); }); }
      else if (k === 'class') e.className = attrs[k];
      else if (k === 'html') e.innerHTML = attrs[k];
      else if (k === 'text') e.textContent = attrs[k];
      else if (k.indexOf('on') === 0) e.addEventListener(k.slice(2), attrs[k]);
      else if (attrs[k] !== null && attrs[k] !== undefined) e.setAttribute(k, attrs[k]);
    });
    (children || []).forEach(function (c) { if (c == null) return; e.appendChild(typeof c === 'string' ? document.createTextNode(c) : c); });
    return e;
  }
  function esc(s) { return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) { return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]; }); }
  function fa(n) { return String(n == null ? '' : n).replace(/[0-9]/g, function (d) { return '۰۱۲۳۴۵۶۷۸۹'[d]; }); }
  function en(s) { return String(s == null ? '' : s).replace(/[۰-۹]/g, function (d) { return String('۰۱۲۳۴۵۶۷۸۹'.indexOf(d)); }).replace(/[٠-٩]/g, function (d) { return String('٠١٢٣٤٥٦٧٨٩'.indexOf(d)); }); }
  /* V156 — آینهٔ WalletScreen.faReason اپ: هیچ کلید انگلیسی wallet_tx.reason به کاربر نشان داده نمی‌شود */
  function faReason(r) {
    r = String(r || '');
    function prov(x) { x = x.split(':')[0].toLowerCase(); return x === 'zarinpal' ? ' (زرین‌پال)' : x === 'idpay' ? ' (آیدی‌پی)' : ''; }
    if (r.indexOf('payment:') === 0) return 'شارژ از درگاه' + prov(r.slice(8));
    if (r.indexOf('exam:create:') === 0) return 'ساخت آزمون';
    if (r.indexOf('exam:update:') === 0) return 'ویرایش آزمون';
    if (r.indexOf('exam:duplicate:') === 0) return 'تکثیر آزمون';
    if (r.indexOf('exam:print:teacher') === 0) return 'چاپ آزمون با کلید';
    if (r.indexOf('exam:print:') === 0) return 'چاپ آزمون';
    if (r.indexOf('backup:restore') === 0) return 'بازیابی نسخهٔ پشتیبان';
    if (r.indexOf('school_transfer_to_teacher') === 0) return 'انتقال به کیف پول معلم';
    if (r.indexOf('school_transfer_from_manager') === 0) return 'دریافت از مدیر مدرسه';
    if (r.indexOf('wallet_transfer') === 0) return 'انتقال کیف پول';
    if (r.indexOf('refund') === 0) return 'بازگشت وجه';
    if (r.indexOf('topup') === 0) return 'شارژ کیف پول';
    if (r.indexOf('admin') === 0 || r.indexOf('manual') === 0) return 'اصلاح توسط پشتیبانی';
    if (r.indexOf('gift') === 0 || r.indexOf('bonus') === 0) return 'هدیه / اعتبار رایگان';
    return 'تراکنش کیف پول';
  }
  function money(n) { n = Number(n) || 0; return fa(n.toLocaleString('en-US')) + ' تومان'; }
  function fmtDate(iso) {
    if (!iso) return '—';
    try { return new Intl.DateTimeFormat('fa-IR', {year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit'}).format(new Date(iso)); } catch (e) { return String(iso).slice(0, 16); }
  }
  function fmtScore(v) { v = Number(v) || 0; return v % 1 === 0 ? String(v) : String(Math.round(v * 100) / 100); }
  function uuid() {
    if (window.crypto && crypto.randomUUID) return crypto.randomUUID();
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) { var r = Math.random() * 16 | 0; return (c === 'x' ? r : (r & 3 | 8)).toString(16); });
  }
  function toast(msg, kind) {
    var wrap = $('toasts'); if (!wrap) return;
    var t = el('div', {class: 'toast ' + (kind || ''), text: msg});
    wrap.appendChild(t);
    setTimeout(function () { t.style.opacity = '0'; t.style.transition = '.3s'; setTimeout(function () { t.remove(); }, 350); }, 3200);
  }
  function errMsg(e) {
    if (!e) return 'خطای ناشناخته';
    if (typeof e === 'string') return e;
    return e.message || e.error_description || e.msg || e.error || e.hint || JSON.stringify(e);
  }
  /* V159 — پنجرهٔ ورودی متن (AlertDialog با OutlinedTextField در اپ) */
  function promptDlg(title, body, label, value, okLabel) {
    return new Promise(function (resolve) {
      var bg = el('div', {class: 'modal-bg'});
      var i = el('input', {type: 'text', value: value || ''});
      var ok = el('button', {class: 'btn', text: okLabel || 'تأیید', onclick: function () { bg.remove(); resolve(i.value); }});
      i.addEventListener('input', function () { ok.disabled = !i.value.trim(); });
      i.addEventListener('keydown', function (e) { if (e.key === 'Enter' && i.value.trim()) ok.click(); });
      var m = el('div', {class: 'modal'}, [el('h2', {text: title}), body ? el('p', {class: 'muted', text: body}) : null, el('div', {class: 'field'}, [el('label', {text: label || ''}), i]),
        el('div', {class: 'row', style: 'justify-content:flex-start;margin-top:14px'}, [ok, el('button', {class: 'btn light', text: 'انصراف', onclick: function () { bg.remove(); resolve(null); }})])]);
      bg.appendChild(m); document.body.appendChild(bg); setTimeout(function () { i.focus(); i.select(); }, 30);
    });
  }
  function confirmDlg(title, body, okLabel, danger) {
    return new Promise(function (resolve) {
      var bg = el('div', {class: 'modal-bg'});
      var m = el('div', {class: 'modal'}, [
        el('h2', {text: title}),
        el('p', {class: 'muted', html: body}),
        el('div', {class: 'row', style: 'justify-content:flex-start;margin-top:14px'}, [
          el('button', {class: 'btn ' + (danger ? 'danger' : ''), text: okLabel || 'تأیید', onclick: function () { bg.remove(); resolve(true); }}),
          el('button', {class: 'btn light', text: 'انصراف', onclick: function () { bg.remove(); resolve(false); }})
        ])
      ]);
      bg.appendChild(m); document.body.appendChild(bg);
    });
  }
  function localState() { try { return JSON.parse(localStorage.getItem(LS_LOCALSTATE) || '{}') || {}; } catch (e) { return {}; } }
  function setLocalState(patch) { var s = localState(); Object.keys(patch).forEach(function (k) { s[k] = patch[k]; }); try { localStorage.setItem(LS_LOCALSTATE, JSON.stringify(s)); } catch (e) {} }

  /* ================================================================ کلاینتِ سبکِ Supabase (بدون کتابخانه) */
  var session = null;
  function loadSession() { try { session = JSON.parse(localStorage.getItem(LS_SESSION) || 'null'); } catch (e) { session = null; } }
  function saveSession(s) { session = s; try { if (s) localStorage.setItem(LS_SESSION, JSON.stringify(s)); else localStorage.removeItem(LS_SESSION); } catch (e) {} }
  function requireKey() { if (!KEY_READY) throw new Error('کلید SUPABASE_ANON_KEY در بالای فایل index.html وارد نشده است.'); }

  async function http(path, opts) {
    requireKey();
    opts = opts || {};
    var headers = {'apikey': ANON, 'Content-Type': 'application/json', 'Accept': 'application/json'};
    if (opts.auth !== false) {
      await ensureFreshSession();
      headers['Authorization'] = 'Bearer ' + (session && session.access_token ? session.access_token : ANON);
    }
    if (opts.headers) Object.keys(opts.headers).forEach(function (k) { headers[k] = opts.headers[k]; });
    var res;
    try {
      res = await fetch(SUPABASE_URL + path, {method: opts.method || 'GET', headers: headers, body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined});
    } catch (e) { throw new Error('اتصال به سرور برقرار نشد. اینترنت را بررسی کنید.'); }
    var text = await res.text();
    var data = null;
    try { data = text ? JSON.parse(text) : null; } catch (e) { data = text; }
    if (!res.ok) {
      var m = (data && (data.msg || data.message || data.error_description || data.error)) || ('HTTP ' + res.status);
      if (data && data.code === 'PGRST202') m = 'تابع «' + path.replace('/rest/v1/rpc/', '') + '» روی سرور پیدا نشد (مهاجرت اجرا نشده).';
      var err = new Error(translateAuthError(m)); err.status = res.status; err.data = data; throw err;
    }
    return data;
  }
  function translateAuthError(m) {
    m = String(m);
    if (/invalid login credentials/i.test(m)) return 'نام کاربری/ایمیل یا رمز عبور نادرست است.';
    if (/email not confirmed/i.test(m)) return 'ایمیل هنوز تأیید نشده است.';
    if (/token has expired|otp_expired/i.test(m)) return 'کد منقضی شده است؛ دوباره درخواست کنید.';
    if (/invalid.*token|Token has expired or is invalid/i.test(m)) return 'کد واردشده نادرست یا منقضی است.';
    if (/rate limit|too many/i.test(m)) return 'درخواست‌ها بیش از حد مجاز است؛ کمی بعد دوباره تلاش کنید.';
    if (/signups not allowed|Signups not allowed for otp/i.test(m)) return 'حسابی با این ایمیل وجود ندارد.';
    if (/User already registered/i.test(m)) return 'این ایمیل قبلاً ثبت شده است.';
    if (/JWT expired|invalid JWT/i.test(m)) return 'نشست ورود منقضی شده؛ دوباره وارد شوید.';
    if (/New password should be different/i.test(m)) return 'رمز جدید باید با رمز قبلی متفاوت باشد.';
    if (/Password should be at least/i.test(m)) return 'رمز عبور باید حداقل ۸ کاراکتر باشد.';
    return m;
  }
  async function ensureFreshSession() {
    if (!session || !session.refresh_token) return;
    var exp = Number(session.expires_at || 0) * 1000;
    if (exp && exp - Date.now() > 60 * 1000) return;
    try {
      var res = await fetch(SUPABASE_URL + '/auth/v1/token?grant_type=refresh_token', {method: 'POST', headers: {'apikey': ANON, 'Content-Type': 'application/json'}, body: JSON.stringify({refresh_token: session.refresh_token})});
      var data = await res.json();
      if (!res.ok) throw new Error(data.msg || data.error_description || 'refresh failed');
      saveSession(data);
    } catch (e) { saveSession(null); }
  }
  function rpc(name, params) { return http('/rest/v1/rpc/' + name, {method: 'POST', body: params || {}}); }
  async function rpcObj(name, params) {
    var raw = await rpc(name, params);
    if (raw && typeof raw === 'object' && !Array.isArray(raw) && raw.error) throw new Error(String(raw.error));
    return raw || {};
  }
  function select(table, query) { return http('/rest/v1/' + table + '?' + query); }
  var authApi = {
    signInPassword: async function (email, password) {
      var data = await http('/auth/v1/token?grant_type=password', {method: 'POST', auth: false, body: {email: email, password: password}});
      saveSession(data); return data;
    },
    sendOtp: function (email, createUser, data) {
      var body = {email: email, create_user: !!createUser};
      if (data) body.data = data;
      return http('/auth/v1/otp', {method: 'POST', auth: false, body: body});
    },
    verifyOtp: async function (email, code) {
      var data = await http('/auth/v1/verify', {method: 'POST', auth: false, body: {type: 'email', email: email, token: code}});
      saveSession(data); return data;
    },
    updateUser: async function (patch) {
      var u = await http('/auth/v1/user', {method: 'PUT', body: patch});
      if (session) { session.user = u; saveSession(session); }
      return u;
    },
    signOut: async function () {
      try { await http('/auth/v1/logout', {method: 'POST'}); } catch (e) {}
      saveSession(null);
    },
    user: function () { return session && session.user ? session.user : null; }
  };

  /* ================================================================ لایهٔ داده (آینهٔ Repositoryهای Kotlin) */
  var user = null; // {id,name,email,role,username,requiresSetup,pendingRole}
  function passwordLoginEmail(identifier) {
    var clean = String(identifier || '').trim().toLowerCase();
    if (clean.indexOf('@') >= 0) { if (!EMAIL_RE.test(clean)) throw new Error('ایمیل معتبر وارد کنید.'); return clean; }
    if (!USERNAME_RE.test(clean)) throw new Error('نام کاربری باید ۴ تا ۲۰ حرف انگلیسی، عدد یا زیرخط باشد.');
    return clean + '@' + STUDENT_DOMAIN;
  }
  async function staffLoginEmail(username) {
    var raw;
    try { raw = await rpc('native_staff_login_email_v1', {p_username: username}); }
    catch (e) {
      var msg = errMsg(e);
      if (/native_staff_login_email_v1|PGRST202|404/.test(msg)) throw new Error('ورود با نام کاربری روی سرور فعال نیست. با ایمیل وارد شوید.');
      throw new Error('بررسی نام کاربری ناموفق بود: ' + msg.slice(0, 160));
    }
    var email = raw && raw.email ? String(raw.email).trim() : '';
    if (email) return email;
    var err = raw && raw.error ? String(raw.error) : '';
    if (/بیش از حد|نامعتبر/.test(err)) throw new Error(err);
    return null;
  }
  async function currentProfile() {
    var su = authApi.user(); if (!su) throw new Error('نشست ورود پیدا نشد. دوباره وارد شوید.');
    var fallback = (su.email || '').split('@')[0] || 'کاربر';
    var p = await rpcObj('native_ensure_profile_v1', {p_fallback_name: fallback});
    var role = /^manager$/i.test(p.role || '') ? 'manager' : (/^teacher$/i.test(p.role || '') ? 'teacher' : 'student');
    var realEmail = !/@student\.exam\.local$/i.test(su.email || '');
    var setupCandidate = role === 'student' || (role === 'teacher' && !(p.username || '').trim());
    var requiresSetup = false, pendingRole = null;
    if (realEmail && setupCandidate) {
      var st = await rpcObj('native_my_registration_state_v1', {});
      requiresSetup = st.requires_teacher_setup === true;
      if (requiresSetup) pendingRole = /^manager$/i.test(st.pending_role || '') ? 'manager' : 'teacher';
    }
    return {id: p.id || su.id, name: (p.display_name || '').trim() || (p.full_name || '').trim() || fallback, email: su.email, role: role, username: p.username || '', requiresSetup: requiresSetup, pendingRole: pendingRole, avatarUrl: p.avatar_url || null};
  }
  var api = {
    signInWithPassword: async function (identifier, password) {
      if (!String(password || '').trim()) throw new Error('رمز عبور را وارد کنید.');
      var clean = String(identifier || '').trim().toLowerCase(), email;
      if (clean.indexOf('@') < 0 && USERNAME_RE.test(clean)) email = (await staffLoginEmail(clean)) || passwordLoginEmail(clean);
      else email = passwordLoginEmail(identifier);
      await authApi.signInPassword(email, password);
      return currentProfile();
    },
    sendLoginOtp: function (email) { return authApi.sendOtp(requireEmail(email), false); },
    verifyLoginOtp: async function (email, code) { await authApi.verifyOtp(requireEmail(email), cleanCode(code)); return currentProfile(); },
    sendRegistrationOtp: function (email, fullName, role) {
      if (String(fullName || '').trim().length < 2) throw new Error('نام و نام خانوادگی را کامل وارد کنید.');
      return authApi.sendOtp(requireEmail(email), true, {full_name: fullName.trim(), registration_role: role});
    },
    verifyRegistrationOtp: function (email, code) { return authApi.verifyOtp(requireEmail(email), cleanCode(code)); },
    completeTeacher: async function (fullName, username, password, inviteCode) {
      var name = fullName.trim(), u = username.trim().toLowerCase(), code = (inviteCode || '').trim();
      if (name.length < 2) throw new Error('نام و نام خانوادگی را کامل وارد کنید.');
      if (!USERNAME_RE.test(u)) throw new Error('نام کاربری باید ۴ تا ۲۰ حرف انگلیسی، عدد یا زیرخط باشد.');
      validatePassword(password);
      if (!code) {
        var r = await rpcObj('native_complete_teacher_registration_v1', {p_full_name: name, p_username: u});
        if (!(r.ok && /^teacher$/i.test(r.role || ''))) throw new Error('تکمیل حساب معلم ناموفق بود.');
      } else if (/^[A-Z0-9]{6}$/.test(code.toUpperCase())) {
        var r1 = await rpcObj('native_complete_teacher_registration_v1', {p_full_name: name, p_username: u});
        if (!(r1.ok && /^teacher$/i.test(r1.role || ''))) throw new Error('تکمیل حساب معلم ناموفق بود.');
        await rpcObj('native_join_school_v39', {p_code: code.toUpperCase()});
      } else if (code.indexOf('TCH-') === 0 && code.length >= 60) {
        var r2 = await rpcObj('native_complete_teacher_registration_v37', {p_full_name: name, p_username: u, p_invite_code: code});
        if (!(r2.ok && /^teacher$/i.test(r2.role || ''))) throw new Error('عضویت معلم در مدرسه کامل نشد.');
      } else throw new Error('کد دعوت معتبر نیست.');
      await authApi.updateUser({password: password, data: {full_name: name, registration_role: 'teacher'}});
      return currentProfile();
    },
    completeManager: async function (fullName, username, password, schoolName, province, city) {
      var name = fullName.trim(), u = username.trim().toLowerCase();
      if (name.length < 2) throw new Error('نام و نام خانوادگی را کامل وارد کنید.');
      if (!USERNAME_RE.test(u)) throw new Error('نام کاربری معتبر نیست.');
      if (schoolName.trim().length < 2) throw new Error('نام مدرسه را وارد کنید.');
      validatePassword(password);
      var r = await rpcObj('native_complete_manager_registration_v36', {p_full_name: name, p_username: u, p_school_name: schoolName.trim(), p_province: province.trim(), p_city: city.trim()});
      if (!(r.ok && /^manager$/i.test(r.role || ''))) throw new Error('تکمیل حساب مدیر/معاون ناموفق بود.');
      await authApi.updateUser({password: password, data: {full_name: name, registration_role: 'manager'}});
      return currentProfile();
    },
    sendRecoveryOtp: function (email) { return authApi.sendOtp(requireEmail(email), false); },
    verifyRecoveryOtp: async function (email, code) { await authApi.verifyOtp(requireEmail(email), cleanCode(code)); var p = await rpcObj('native_my_profile', {}); if (p && p.error) throw new Error(String(p.error)); return p && p.username ? p.username : null; },
    changePassword: async function (pw) { validatePassword(pw); await authApi.updateUser({password: pw}); },
    updateUsername: function (u) { return rpcObj('native_update_my_username_v1', {p_username: u.trim().toLowerCase()}); },
    // معلم
    exams: async function () {
      var rows = await select('exams', 'select=id,title,subject,duration,code,is_open,total_score,created_at&teacher_id=eq.' + encodeURIComponent(user.id));
      return (rows || []).sort(function (a, b) { return (b.created_at ? 1 : 0) - (a.created_at ? 1 : 0) || String(b.created_at || '').localeCompare(String(a.created_at || '')); });
    },
    examDetail: async function (examId) {
      var rows = await select('exams', 'select=*&id=eq.' + encodeURIComponent(examId) + '&teacher_id=eq.' + encodeURIComponent(user.id));
      var exam = rows && rows[0]; if (!exam) throw new Error('آزمون پیدا نشد یا متعلق به این حساب نیست.');
      var keys = await select('exam_keys', 'select=exam_id,answers&exam_id=eq.' + encodeURIComponent(examId));
      exam.__answers = keys && keys[0] ? keys[0].answers : [];
      return exam;
    },
    setExamOpen: function (id, open) { return rpcObj('native_set_exam_open_v1', {p_exam: id, p_open: !!open}); },
    deleteExam: function (id) { return rpcObj('native_delete_exam', {p_exam: id}); },
    duplicateExam: function (id) { return rpcObj('native_duplicate_exam_v2', {p_exam: id, p_operation: uuid()}); },
    chargePrint: function (examId, count, mode) { return rpcObj('native_charge_print_v1', {p_exam: examId, p_operation: uuid(), p_questions: count, p_mode: mode}); },
    classes: function () { return rpc('native_my_classes_v28', {}); },
    saveClass: function (id, name, grade, field) { if (!name.trim()) throw new Error('نام کلاس را وارد کنید.'); return rpcObj('native_save_class_v28', {p_class: id || null, p_name: name.trim(), p_grade: grade.trim(), p_field: field.trim()}); },
    deleteClass: function (id) { return rpcObj('delete_class', {p_class: id}); },
    students: function () { return rpc('my_students', {}); },
    roster: function (classId) { return rpc('class_roster', {p_class: classId}); },
    wallet: async function () { var d = await rpcObj('native_wallet_snapshot', {}); return {balance: Math.max(0, Number(d.balance) || 0), currency: d.currency || 'toman', transactions: d.transactions || []}; },
    profile: async function () {
      var d = await rpcObj('native_my_profile', {});
      var role = /^manager$/i.test(d.role || '') ? 'manager' : (/^teacher$/i.test(d.role || '') ? 'teacher' : 'student');
      var details = {};
      if (role === 'teacher') { try { details = await rpcObj('native_my_teacher_details_v40', {}); } catch (e) { details = {}; } }
      var full = d.full_name || '';
      return {id: d.id, role: role, fullName: full || 'کاربر', firstName: details.first_name || full.split(' ')[0] || '', lastName: details.last_name || full.split(' ').slice(1).join(' '), employeeCode: details.employee_code || '', phone: details.phone || '',
        displayName: d.display_name || '', username: d.username || '', avatarUrl: d.avatar_url || null, avatarPublic: d.avatar_public !== false,
        header: {province: d.hdr_province || '', city: d.hdr_city || '', district: d.hdr_district || '', school: d.hdr_school || '', grade: d.hdr_grade || '', fieldOfStudy: d.hdr_field || ''}};
    },
    saveProfile: async function (p) {
      if (p.displayName.length > 100) throw new Error('نام نمایشی حداکثر ۱۰۰ نویسه است.');
      if (p.employeeCode && !/^[A-Za-z0-9_-]{1,30}$/.test(p.employeeCode)) throw new Error('کد پرسنلی معتبر نیست.');
      if (p.phone && !/^09[0-9]{9}$/.test(en(p.phone))) throw new Error('شماره تلفن باید ۱۱ رقم و با 09 شروع شود.');
      await rpcObj('native_save_profile_v28', {p_display_name: p.displayName.trim() || null, p_avatar_url: p.avatarUrl, p_avatar_public: p.avatarPublic, p_hdr_province: p.header.province.trim(), p_hdr_city: p.header.city.trim(), p_hdr_district: p.header.district.trim(), p_hdr_school: p.header.school.trim(), p_hdr_grade: p.header.grade.trim(), p_hdr_field: p.header.fieldOfStudy.trim()});
      if (p.role === 'teacher') await rpcObj('native_save_teacher_details_v40', {p_first_name: p.firstName.trim(), p_last_name: p.lastName.trim(), p_employee_code: p.employeeCode.trim(), p_phone: en(p.phone).trim()});
    },
    // دانش‌آموز
    myGrades: function () { return rpc('my_grades', {}); },
    myAnswers: async function () { var d = await rpcObj('native_my_answers_v1', {}); return d.items || []; },
    // مدیر
    managerSummary: function () { return rpcObj('native_manager_school_summary_v36', {}); },
    managerTeachers: async function () { var d = await rpcObj('native_manager_teachers_v37', {}); return d.items || []; }
  };
  function requireEmail(v) { var c = String(v || '').trim().toLowerCase(); if (!EMAIL_RE.test(c)) throw new Error('ایمیل معتبر وارد کنید.'); return c; }
  function cleanCode(c) { var d = en(c).replace(/\D/g, ''); if (d.length < 6 || d.length > 8) throw new Error('کد یک‌بارمصرف باید ۶ تا ۸ رقم باشد.'); return d; }
  function validatePassword(v) { if (!v || v.length < 8 || v.length > 72) throw new Error('رمز عبور باید ۸ تا ۷۲ کاراکتر باشد.'); }

  /* ================================================================ موتور چاپ (assets/print) و ویرایشگر فرمول — تعبیه‌شده */
  function engineHtml(kind) {
    var src = (window.__ENGINES || {})[kind] || '';
    if (!src) toast('موتور ' + (kind === 'print' ? 'چاپ' : 'فرمول') + ' در این فایل تعبیه نشده است.', 'err');
    return src;
  }
  /* ---- پل چاپ: همان متدهای ExamPrintBridge اندروید (ExamHtmlPrintDialog.kt) در مرورگر ---- */
  var printCtx = null; // {overlay, iframe, examId, questionCount, onClosed}
  window.__printBridge = {
    toast: function (m) { toast(String(m || '')); return true; },
    previewClosed: function () { closePrintOverlay(); },
    pageSetupChanged: function (json) { try { localStorage.setItem(LS_PAGESETUP, String(json || '')); } catch (e) {} },
    renderFigure: function () { return ''; },
    renderFormula: function () { return ''; },
    onError: function (code) { console.warn('print engine:', code); },
    editFigureTool: function (qid, index) {
      try {
        var w = printCtx && printCtx.iframe.contentWindow; if (!w) return;
        var fig = w.document.querySelector('#previewArea .interactive-figure[data-qid="' + qid + '"][data-fig-index="' + index + '"]');
        if (fig && typeof w.openQmfFigEditorBody === 'function') w.openQmfFigEditorBody(fig);
      } catch (e) {}
    },
    print: function (mode) {
      if (!printCtx) return;
      var w = printCtx.iframe.contentWindow;
      var doNative = function () {
        try { w.focus(); w.Window.prototype.print.call(w); } catch (e) { try { w.print(); } catch (e2) {} }
        setTimeout(function () { try { w.ExamPrintRenderer.restorePreview(); } catch (e) {} }, 400);
      };
      var restore = function () { try { w.ExamPrintRenderer.restorePreview(); } catch (e) {} };
      if (!printCtx.examId) { doNative(); return; }
      /* V132 — هزینهٔ چاپ: ۱۰۰۰ تومان به‌ازای هر سؤال، تأیید پیش از پنجرهٔ چاپ */
      var n = printCtx.questionCount || 0, cost = n * PRINT_COST_PER_Q;
      confirmDlg('هزینهٔ چاپ', 'چاپ نسخهٔ ' + (mode === 'teacher' ? 'استاد (با پاسخ‌نامه)' : 'دانش‌آموز') + ' با ' + fa(n) + ' سؤال، مبلغ <b>' + money(cost) + '</b> از کیف پول شما کسر می‌کند. ادامه می‌دهید؟', 'پرداخت و چاپ').then(function (ok) {
        if (!ok) { restore(); return; }
        api.chargePrint(printCtx.examId, n, mode).then(function (r) {
          toast('کسر ' + money(r.cost || cost) + ' با موفقیت انجام شد.', 'ok');
          doNative();
        }).catch(function (e) { restore(); toast(errMsg(e), 'err'); });
      });
    }
  };
  function closePrintOverlay() {
    if (!printCtx) return;
    var c = printCtx; printCtx = null;
    if (c.onSnapshot) { try { var w = c.iframe.contentWindow; var snap = w.ExamPrintRenderer && w.ExamPrintRenderer.layoutSnapshot ? w.ExamPrintRenderer.layoutSnapshot() : '{}'; if (snap && snap !== '{}') c.onSnapshot(snap); } catch (e) {} }
    try { c.overlay.remove(); } catch (e) {}
    document.body.style.overflow = '';
    if (c.onClosed) c.onClosed();
  }
  /* V160 — آینهٔ ExamHtmlImageInliner: نشانی‌های https تصویر (Storage خصوصی با هدر نشست / S3 عمومی) → data:image/jpeg
     حداکثر ۲۴ تصویر، ضلع ≤۱۲۸۰، کیفیت ۸۵؛ شکست هر تصویر فقط همان تصویر را حذف می‌کند. */
  function inlinePrintImages(payload) {
    var st = {done: false}, jobs = [], used = 0, RE = /%%FIG:(\{[\s\S]*?\})%%/g;
    (payload.questions || []).forEach(function (q) {
      if (!q.text || q.text.indexOf('"k":"img"') < 0) return;
      var m, list = [];
      RE.lastIndex = 0; while ((m = RE.exec(q.text))) { try { var o = JSON.parse(m[1]); if (o.k === 'img' && /^https?:/i.test(o.src)) list.push([m[0], o]); } catch (e) {} }
      list.forEach(function (it) {
        if (used >= 24) { q.text = q.text.replace(it[0], ''); return; }
        used++;
        jobs.push(mediaBlobUrl(it[1].src).then(function (u) { return new Promise(function (res) { var im = new Image(); if (/^https?:/i.test(u)) im.crossOrigin = 'anonymous'; im.onload = function () { res(im); }; im.onerror = function () { res(null); }; im.src = u; }); }).then(function (im) {
          if (!im) { q.text = q.text.replace(it[0], ''); return; }
          var k = Math.min(1, 1280 / Math.max(im.naturalWidth, im.naturalHeight)), c = document.createElement('canvas'); c.width = Math.max(1, Math.round(im.naturalWidth * k)); c.height = Math.max(1, Math.round(im.naturalHeight * k));
          var x = c.getContext('2d'); x.fillStyle = '#fff'; x.fillRect(0, 0, c.width, c.height); x.drawImage(im, 0, 0, c.width, c.height);
          var data; try { data = c.toDataURL('image/jpeg', 0.85); } catch (e) { data = null; }
          if (!data) { q.text = q.text.replace(it[0], ''); return; }
          it[1].src = data; q.text = q.text.replace(it[0], ' %%FIG:' + JSON.stringify(it[1]) + '%%');
        }).catch(function () { q.text = q.text.replace(it[0], ''); }));
      });
    });
    Promise.all(jobs).then(function () { st.done = true; }, function () { st.done = true; });
    if (!jobs.length) st.done = true;
    return st;
  }
  function openPrintPreview(payload, opts) {
    opts = opts || {};
    closePrintOverlay();
    var overlay = el('div', {class: 'engine-bg'});
    var bar = el('div', {class: 'engine-bar'}, [
      el('span', {text: '🖨 پیش‌نمایش و چاپ — ' + (opts.title || 'آزمون')}),
      el('span', {class: 'grow'}),
      el('button', {class: 'btn light sm', text: '✕ بستن', onclick: closePrintOverlay})
    ]);
    var iframe = el('iframe', {class: 'with-bar', title: 'print-engine'});
    overlay.appendChild(bar); overlay.appendChild(iframe); document.body.appendChild(overlay);
    document.body.style.overflow = 'hidden';
    printCtx = {overlay: overlay, iframe: iframe, examId: opts.examId || '', questionCount: (payload.questions || []).length, onClosed: opts.onClosed, onSnapshot: opts.onSnapshot};
    try { var ps = localStorage.getItem(LS_PAGESETUP); if (ps && payload.pageSetup === undefined) payload.pageSetup = JSON.parse(ps); } catch (e) {}
    var inlined = inlinePrintImages(payload);
    iframe.addEventListener('load', function () {
      var w = iframe.contentWindow, tries = 0;
      (function push() {
        tries++;
        try {
          if (typeof w.setExamData === 'function' && w.renderPreview && w.renderPreview.__pgs && inlined.done) {
            w.setExamData(payload);
            /* V156 — مثل ExamHtmlPrintDialog: printMode=student/teacher یعنی بدون توقف در پیش‌نمایش، مستقیم چاپ */
            setTimeout(function () { try { if (opts.printMode === 'teacher' && typeof w.printTeacher === 'function') w.printTeacher(); else if (opts.printMode === 'student' && typeof w.printStudent === 'function') w.printStudent(); else w.ExamPrintRenderer.showPreview(); } catch (e) { console.warn(e); } }, 120);
            return;
          }
        } catch (e) {}
        if (tries < 200 || (!inlined.done && tries < 1200)) setTimeout(push, 50); else toast(inlined.done ? 'موتور چاپ آماده نشد.' : 'بارگذاری تصویرهای آزمون طول کشید.', 'err');
      })();
    });
    iframe.srcdoc = engineHtml('print');
  }
  /* ---- ویرایشگر فرمول: پل ExamEditorNative (FormulaHostDialog.kt) ---- */
  var formulaCtx = null;
  window.__formulaBridge = {
    onTextChanged: function (v) { if (formulaCtx) formulaCtx.text = String(v == null ? '' : v); },
    onEditorClosed: function () { if (!formulaCtx) return; var c = formulaCtx; formulaCtx = null; try { c.overlay.remove(); } catch (e) {} document.body.style.overflow = ''; c.resolve(c.text); },
    onError: function (code) { console.warn('formula editor:', code); }
  };
  function openFormulaEditor(text, selStart, selEnd) {
    return new Promise(function (resolve) {
      if (formulaCtx) { try { formulaCtx.overlay.remove(); } catch (e) {} }
      var overlay = el('div', {class: 'engine-bg'});
      var iframe = el('iframe', {title: 'formula-editor'});
      overlay.appendChild(iframe); document.body.appendChild(overlay);
      document.body.style.overflow = 'hidden';
      formulaCtx = {overlay: overlay, iframe: iframe, text: text || '', resolve: resolve};
      var s = selStart == null ? (text || '').length : selStart, e = selEnd == null ? s : selEnd;
      iframe.addEventListener('load', function () {
        var w = iframe.contentWindow, tries = 0;
        (function tryBegin() {
          tries++;
          try { if (w.ExamFormulaHost && typeof w.ExamFormulaHost.begin === 'function') { w.ExamFormulaHost.begin(text || '', s, e); return; } } catch (er) {}
          if (tries < 67) setTimeout(tryBegin, 150); else { toast('ویرایشگر فرمول آماده نشد.', 'err'); window.__formulaBridge.onEditorClosed(); }
        })();
      });
      iframe.srcdoc = engineHtml('formula');
    });
  }

  /* ---- نگاشتِ سؤالِ سرور → payloadِ موتور چاپ (ExamQuestionCodec + PrintableFromDrafts + ExamHtmlPrintPayload) ---- */
  function qType(v) {
    v = String(v || '').toLowerCase();
    if (v === 'multiple' || v === 'multiple_choice' || v === 'multiplechoice') return 'multiple';
    if (v === 'truefalse' || v === 'true_false') return 'truefalse';
    if (v === 'fill' || v === 'fill_blank') return 'fill';
    if (v === 'numeric' || v === 'number') return 'numeric';
    if (v === 'matching' || v === 'match') return 'matching';
    return 'long';
  }
  function styleTriple(s) { return s && typeof s === 'object' && Object.keys(s).length ? {bold: !!s.b, italic: !!s.i, size: s.s != null ? Number(s.s) : undefined} : null; }
  /* ---- V157: تنظیمات سربرگ (آینهٔ PrintHeaderStore + HeaderSettingsDialog اپ؛ schema همان header_settings_schema.json) ---- */
  var LS_PRINTHEADER = 'examsite.printheader.v1';
  function readPrintHeader() { try { var o = JSON.parse(localStorage.getItem(LS_PRINTHEADER) || '{}'); return o && typeof o === 'object' ? o : {}; } catch (e) { return {}; } }
  function openHeaderSettings(onApply) {
    var schema = window.__HEADER_SCHEMA;
    if (!schema || !Array.isArray(schema.templates) || !schema.templates.length) { confirmDlg('تنظیمات سربرگ', 'قالب‌های سربرگ خوانده نشد. لطفاً دوباره تلاش کنید.', 'باشد'); return; }
    var cur = readPrintHeader(), values = Object.assign({}, cur);
    var tplId = schema.templates.some(function (t) { return t.id === cur.f_headerTemplate; }) ? cur.f_headerTemplate : schema.templates[0].id;
    var bg = el('div', {class: 'modal-bg hdr-bg', onclick: function (e) { if (e.target === bg) bg.remove(); }});
    var box = el('div', {class: 'modal hdr-modal'});
    var list = el('div', {class: 'hdr-fields'});
    var sel = el('select', {class: 'hdr-tpl'});
    schema.templates.forEach(function (t) { sel.appendChild(el('option', {value: t.id, text: t.label})); });
    sel.value = tplId;
    function draw() {
      var t = schema.templates.filter(function (x) { return x.id === tplId; })[0] || schema.templates[0];
      list.innerHTML = '';
      (t.fields || []).forEach(function (f) {
        var input;
        if (f.kind === 'select') { input = el('select'); input.appendChild(el('option', {value: '', text: '—'})); (f.options || []).forEach(function (o) { input.appendChild(el('option', {value: o.v, text: o.t})); }); input.value = values[f.id] || ''; }
        else if (f.kind === 'textarea') input = el('textarea', {rows: String(f.rows || 3), text: values[f.id] || ''});
        else input = el('input', {type: 'text', value: values[f.id] || '', placeholder: f.placeholder || ''});
        input.addEventListener('input', function () { values[f.id] = input.value; });
        input.addEventListener('change', function () { values[f.id] = input.value; });
        list.appendChild(el('div', {class: 'field' + (f.full ? ' full' : '')}, [el('label', {text: f.label}), input]));
      });
    }
    sel.addEventListener('change', function () { tplId = sel.value; draw(); });
    box.appendChild(el('div', {class: 'row', style: 'margin-bottom:8px'}, [el('h2', {class: 'grow', text: 'اطلاعات سربرگ آزمون', style: 'margin:0'}), el('button', {class: 'x', text: '✕', 'aria-label': 'بستن', onclick: function () { bg.remove(); }})]));
    box.appendChild(el('div', {class: 'field'}, [el('label', {text: 'انتخاب نوع سربرگ'}), sel]));
    box.appendChild(list);
    box.appendChild(el('div', {class: 'row hdr-actions', style: 'margin-top:10px'}, [
      el('button', {class: 'btn light', text: 'انصراف', onclick: function () { bg.remove(); }}),
      el('button', {class: 'btn', text: 'اعمال', onclick: function () {
        var t = schema.templates.filter(function (x) { return x.id === tplId; })[0];
        var payload = {f_headerTemplate: tplId}; (t.fields || []).forEach(function (f) { payload[f.id] = values[f.id] || ''; });
        try { localStorage.setItem(LS_PRINTHEADER, JSON.stringify(payload)); } catch (e) {}
        bg.remove(); toast('سربرگ ذخیره شد.', 'ok'); if (onApply) onApply(payload);
      }})
    ]));
    draw(); bg.appendChild(box); document.body.appendChild(bg);
  }
  function buildPrintPayload(exam, opts) {
    opts = opts || {};
    var questions = Array.isArray(exam.questions) ? exam.questions : [];
    var keysArr = Array.isArray(exam.__answers) ? exam.__answers : [];
    var keys = {}; keysArr.forEach(function (k, i) { if (k && typeof k === 'object') keys[k.i != null ? k.i : i] = k; });
    var total = 0;
    var out = questions.map(function (q, index) {
      q = q || {}; var key = keys[index] || {};
      var type = qType(q.type), score = Number(q.score) || 0; total += score;
      /* V160 — مثل ExamHtmlImageInliner: تصویرهای سؤال به‌صورت توکن %%FIG:{k:img}%% به انتهای متن؛ نشانی‌های راه‌دور در openPrintPreview درون‌خطی می‌شوند */
      var qImgs = (Array.isArray(q.images) ? q.images : []).filter(Boolean).map(String); if (q.image && qImgs.indexOf(q.image) < 0) qImgs.unshift(q.image);
      var imgTokens = qImgs.map(function (u) { return ' %%FIG:' + JSON.stringify({k: 'img', src: u, w: 420}) + '%%'; }).join('');
      var o = {id: index + 1, text: (q.text || '') + imgTokens, score: fmtScore(score), textAlign: q.align || 'right', fontFamily: q.font || 'default', fontSizeSp: Number(q.fontSize) || 16, bold: q.bold === true, italic: q.italic === true};
      if (Array.isArray(q.spans) && q.spans.length) o.textSpans = q.spans.map(function (s) { return {start: s.s, end: s.e, bold: !!s.b, italic: !!s.i, underline: !!s.u, color: s.c, size: s.z, font: s.f}; });
      if (Array.isArray(q.alignSpans) && q.alignSpans.length) o.alignSpans = q.alignSpans.map(function (s) { return {start: s.s, end: s.e, align: s.a}; });
      if (q.figLayouts && typeof q.figLayouts === 'object' && Object.keys(q.figLayouts).length) o.figLayoutsJson = JSON.stringify(q.figLayouts);
      if (Number(q.sepExtraPx) > 0) o.sepExtraPx = Number(q.sepExtraPx);
      var lines = q.answerLines != null ? Number(q.answerLines) : (type === 'long' ? 5 : 2);
      var style = q.answerLineStyle === 'blank' ? 'plain' : (q.answerLineStyle === 'grid' ? 'grid' : 'lined');
      var spacing = q.answerLineSpacingCm != null ? Number(q.answerLineSpacingCm) : 1.0;
      if (type === 'multiple') {
        var ci = key.correctOption != null ? Number(key.correctOption) : (q.correctIndex != null ? Number(q.correctIndex) : -1);
        var optStyles = Array.isArray(q.optionStyles) ? q.optionStyles : [];
        o.type = 'multiple';
        o.options = (q.options || []).map(function (t, i) { var st = styleTriple(optStyles[i]); var r = {text: String(t == null ? '' : t), correct: i === ci}; if (st) { r.bold = st.bold; r.italic = st.italic; if (st.size) r.size = st.size; } return r; });
        o.optionsLayout = o.options.length > 2 ? '2rows' : '1row';
        if (ci >= 0 && q.options && q.options[ci] != null) o.answer = String(q.options[ci]);
      } else if (type === 'truefalse') {
        var t = key.correctAnswer === true || key.correctAnswer === 'true';
        o.type = 'truefalse'; o.options = [{text: 'صحیح', correct: t}, {text: 'غلط', correct: !t}]; o.answer = t ? 'صحیح' : 'غلط';
      } else if (type === 'matching') {
        var L = q.leftItems || [], R = q.rightItems || [], LS = q.leftStyles || [], RS = q.rightStyles || [];
        o.type = 'matching';
        o.pairs = []; for (var i = 0; i < Math.max(L.length, R.length); i++) {
          var p = {left: L[i] != null ? String(L[i]) : '', right: R[i] != null ? String(R[i]) : ''};
          var ls = styleTriple(LS[i]), rs = styleTriple(RS[i]);
          if (ls) { p.leftBold = ls.bold; p.leftItalic = ls.italic; if (ls.size) p.leftSize = ls.size; }
          if (rs) { p.rightBold = rs.bold; p.rightItalic = rs.italic; if (rs.size) p.rightSize = rs.size; }
          o.pairs.push(p);
        }
        var ma = key.matchAnswer && typeof key.matchAnswer === 'object' ? key.matchAnswer : {};
        o.answer = Object.keys(ma).sort(function (a, b) { return a - b; }).map(function (k) { return (Number(k) + 1) + '←' + (Number(ma[k]) + 1); }).join('، ');
      } else {
        o.type = type; o.answerLines = Math.max(0, Math.min(30, lines)); o.answerStyle = style; o.answerLineSpacingCm = Math.max(0.5, Math.min(2, spacing));
        if (type === 'numeric') o.answer = (key.answer != null ? String(key.answer) : '') + ' ± ' + (key.tolerance != null ? String(key.tolerance) : '0');
        else if (type === 'fill') o.answer = (Array.isArray(key.accept) ? key.accept : []).join('، ');
      }
      return o;
    });
    var fields = {f_headerTemplate: 'classic', f_course: exam.subject || exam.title || 'آزمون'};
    /* V157 — مثل ExamHtmlPrintDialog: همهٔ فیلدهای خام «تنظیمات سربرگ» (PrintHeaderStore) روی پیش‌فرض‌ها می‌نشینند */
    var savedHeader = readPrintHeader(); Object.keys(savedHeader).forEach(function (k) { if (savedHeader[k] !== '' || k === 'f_headerTemplate') fields[k] = savedHeader[k]; });
    if (exam.duration > 0) fields.f_duration = exam.duration + ' دقیقه';
    if (opts.header) { if (opts.header.school) fields.f_branch = opts.header.school; }
    return {reset: false, documentTitle: exam.title || 'آزمون', footerNote: '', totalScore: fmtScore(exam.total_score || total), includeAnswerKey: true, persianDigits: true, fields: fields, questions: out};
  }

  /* ================================================================ UI: وضعیت و مسیریابی */
  var view = {page: 'landing', panel: 'dashboard'};
  var root;
  function render() {
    root = $('root');
    /* V148 — پوستهٔ موبایل (mobile.js) در حالت گوشی/معلم جای پنل دسکتاپ را می‌گیرد */
    if (user && !user.requiresSetup && window.SiteMobile && window.SiteMobile.active()) { document.body.classList.add('m-mode'); window.SiteMobile.paint(); return; }
    document.body.classList.remove('m-mode');
    /* V154 — ورود/ثبت‌نام در گوشی به سبک SignInScreen اپ (پوستهٔ یخی) */
    if (!user && window.SiteMobile && window.SiteMobile.authActive()) { closeAuth(); root.innerHTML = ''; window.SiteMobile.paintAuth(root); return; }
    root.innerHTML = '';
    if (user && !user.requiresSetup) renderPanel(); else renderLanding();
  }

  /* ---------------- لندینگ ---------------- */
  function renderLanding() {
    var keyWarn = KEY_READY ? null : el('div', {class: 'warn-key', html: '⚠️ کلید اتصال (<code class="k">SUPABASE_ANON_KEY</code>) هنوز در بالای فایل <code class="k">index.html</code> وارد نشده است؛ تا آن زمان ورود و ثبت‌نام کار نمی‌کند.'});
    var top = el('header', {class: 'topbar'}, [el('div', {class: 'in'}, [
      brandEl(),
      el('nav', {class: 'nav'}, [
        el('a', {href: '#features', text: 'امکانات'}),
        el('a', {href: '#roles', text: 'نقش‌ها'}),
        el('a', {href: '#tools', text: 'ابزارها'})
      ]),
      el('span', {class: 'grow'}),
      el('button', {class: 'btn ghost', text: 'ورود', onclick: function () { openAuth('login'); }}),
      el('button', {class: 'btn', text: 'ثبت‌نام معلم/مدیر', onclick: function () { openAuth('register'); }})
    ])]);
    var hero = el('section', {class: 'hero'}, [
      el('div', {}, [
        el('h1', {html: 'آزمون‌ساز آنلاین و چاپی<br><span>برای معلم، مدرسه و دانش‌آموز</span>'}),
        el('p', {text: 'طراحی آزمون با فرمول ریاضی، شکل هندسی، نمودار، جدول تناوبی و اطلس علوم؛ برگزاری آنلاین با تختهٔ سفید؛ چاپ برگهٔ رسمی با هفت قالب سربرگ. همان حسابِ برنامهٔ اندروید، حالا در مرورگر.'}),
        el('div', {class: 'actions'}, [
          el('button', {class: 'btn lg', text: 'ورود به پنل', onclick: function () { openAuth('login'); }}),
          el('button', {class: 'btn lg light', text: '🧮 امتحان ویرایشگر فرمول', onclick: function () { demoFormula(); }}),
          el('button', {class: 'btn lg light', text: '🖨 نمونهٔ پیش‌نمایش چاپ', onclick: function () { demoPrint(); }})
        ])
      ]),
      el('div', {class: 'hero-art'}, [
        el('div', {class: 'sheet'}, [
          el('div', {class: 'h'}, [el('span', {text: 'آزمون ریاضی — پایهٔ دهم'}), el('span', {text: 'زمان: ۶۰ دقیقه'})]),
          el('div', {class: 'q'}, [el('b', {text: '۱)'}), el('span', {html: 'حاصل عبارت <span class="f">x² − 4x + 4</span> را به‌صورت مربع کامل بنویسید.'})]),
          el('div', {class: 'line'}), el('div', {class: 'line'}),
          el('div', {class: 'q'}, [el('b', {text: '۲)'}), el('span', {text: 'کدام گزینه ریشهٔ معادله است؟'})]),
          el('div', {class: 'opts'}, [el('span', {text: '○ ۲'}), el('span', {text: '○ −۲'}), el('span', {text: '○ ۴'}), el('span', {text: '○ صفر'})]),
          el('div', {class: 'q'}, [el('b', {text: '۳)'}), el('span', {text: 'نمودار تابع را روی محور مختصات رسم کنید.'})]),
          el('div', {class: 'line'}), el('div', {class: 'line'}), el('div', {class: 'line'})
        ]),
        el('div', {class: 'badge-float a', text: '✓ تصحیح خودکار'}),
        el('div', {class: 'badge-float b', text: '∑ فرمول‌نویسی ریاضی'})
      ])
    ]);
    var features = el('section', {class: 'section', id: 'features'}, [
      el('h2', {text: 'همهٔ امکانات برنامه، در مرورگر'}),
      el('p', {class: 'sub', text: 'همان حساب کاربری، همان کلاس‌ها و آزمون‌ها؛ بدون نصب.'}),
      el('div', {class: 'features'}, [
        ['📝', 'سازندهٔ آزمون', 'چندگزینه‌ای، صحیح/غلط، جای‌خالی، عددی، جورکردنی و تشریحی با بارم و کلید پاسخ.'],
        ['🧮', 'ویرایشگر فرمول', 'همان ویرایشگر فرمول برنامه: کسر، رادیکال، انتگرال، ماتریس و نمادهای ریاضی.'],
        ['📐', 'شکل و نمودار', 'اشکال هندسی، محورهای مختصات، جدول، جدول تناوبی، اطلس آناتومی و علوم.'],
        ['🖨', 'چاپ رسمی', 'هفت قالب سربرگ (دانشگاه، مدرسه، اداره، وزارت)، صفحه‌بندی A4/A5، نسخهٔ دانش‌آموز و استاد.'],
        ['🏫', 'کلاس و دانش‌آموز', 'ساخت کلاس، افزودن دانش‌آموز، فهرست حضور، اشتراک با مدیر مدرسه.'],
        ['📊', 'کارنامه و تصحیح', 'تصحیح خودکار تستی، تصحیح دستی تشریحی، بازخورد و کارنامهٔ دانش‌آموز.']
      ].map(function (f) { return el('div', {class: 'feature'}, [el('div', {class: 'ic', text: f[0]}), el('h3', {text: f[1]}), el('p', {text: f[2]})]); }))
    ]);
    var roles = el('section', {class: 'section', id: 'roles'}, [
      el('h2', {text: 'برای چه کسانی؟'}),
      el('div', {class: 'roles'}, [
        ['👩‍🏫', 'معلم', ['ساخت آزمون آنلاین و چاپی', 'مدیریت کلاس‌ها و دانش‌آموزان', 'تصحیح و کارنامه', 'کیف پول و شارژ'], 'ثبت‌نام معلم', 'register'],
        ['🎓', 'دانش‌آموز', ['ورود با نام کاربریِ دریافتی از معلم', 'شرکت در آزمون با کد', 'تختهٔ سفید برای پاسخ تشریحی', 'مشاهدهٔ کارنامه'], 'ورود دانش‌آموز', 'login'],
        ['🏫', 'مدیر / معاون', ['ثبت مدرسه و دعوت معلم‌ها', 'گزارش فعالیت معلمان', 'دانش‌آموزان مدرسه', 'توزیع اعتبار'], 'ثبت‌نام مدیر', 'register-manager']
      ].map(function (r) { return el('div', {class: 'role'}, [el('div', {class: 'em', text: r[0]}), el('h3', {text: r[1]}), el('ul', {}, r[2].map(function (t) { return el('li', {text: t}); })), el('button', {class: 'btn soft', text: r[3], onclick: function () { openAuth(r[4]); }})]); }))
    ]);
    var tools = el('section', {class: 'section', id: 'tools'}, [
      el('h2', {text: 'ابزارها را همین حالا امتحان کنید'}),
      el('p', {class: 'sub', text: 'بدون ورود: ویرایشگر فرمول و موتور پیش‌نمایش چاپ همین‌جا اجرا می‌شوند.'}),
      el('div', {class: 'row', style: 'justify-content:center'}, [
        el('button', {class: 'btn lg', text: '🧮 ویرایشگر فرمول', onclick: demoFormula}),
        el('button', {class: 'btn lg soft', text: '🖨 پیش‌نمایش چاپ نمونه', onclick: demoPrint})
      ])
    ]);
    var foot = el('footer', {html: 'آزمون‌ساز — نسخهٔ وب (فاز ۶) · همان بک‌اند برنامهٔ اندروید · <a href="https://github.com/hashemamiri/exam-app-kotlin" target="_blank" rel="noopener">مخزن پروژه</a>'});
    if (keyWarn) root.appendChild(el('div', {style: 'padding-top:14px'}, [keyWarn]));
    root.appendChild(top); root.appendChild(hero); root.appendChild(features); root.appendChild(roles); root.appendChild(tools); root.appendChild(foot);
  }
  function brandEl() { return el('div', {class: 'logo'}, [el('span', {class: 'mark', text: '✎'}), el('span', {text: 'آزمون‌ساز'})]); }

  function demoFormula() {
    openFormulaEditor('مساحت دایره برابر است با $\\pi r^2$ و ', null, null).then(function (t) { if (t != null) toast('متن نهایی: ' + String(t).slice(0, 80)); });
  }
  function demoPrint() {
    var exam = {title: 'آزمون نمونه', subject: 'ریاضی', duration: 60, total_score: 6,
      questions: [
        {type: 'multiple', text: 'حاصل $\\frac{3}{4}+\\frac{1}{4}$ کدام است؟', score: 1, options: ['۱', '۲', '۰٫۵', '۴']},
        {type: 'truefalse', text: 'مجموع زوایای داخلی مثلث ۱۸۰ درجه است.', score: 1},
        {type: 'fill', text: 'ریشهٔ دوم ۱۴۴ برابر است با ___ .', score: 1, answerLines: 1},
        {type: 'matching', text: 'هر شکل را به تعداد ضلع‌هایش وصل کنید.', score: 1, leftItems: ['مثلث', 'مربع', 'شش‌ضلعی'], rightItems: ['۶', '۳', '۴']},
        {type: 'essay', text: 'نمودار تابع $y=x^2$ را رسم کنید و رأس آن را مشخص نمایید.', score: 2, answerLines: 6}
      ],
      __answers: [{i: 0, correctOption: 0}, {i: 1, correctAnswer: true}, {i: 2, accept: ['12', '۱۲']}, {i: 3, matchAnswer: {'0': 1, '1': 2, '2': 0}}, {i: 4}]
    };
    openPrintPreview(buildPrintPayload(exam), {title: exam.title});
  }

  /* ---------------- ورود / ثبت‌نام ---------------- */
  var authModal = null;
  function closeAuth() { if (authModal) { authModal.remove(); authModal = null; } }
  function openAuth(mode) {
    closeAuth();
    var state = {mode: mode === 'register-manager' ? 'register' : (mode || 'login'), role: mode === 'register-manager' ? 'manager' : 'teacher', step: 'form', email: '', otpMode: false};
    var bg = el('div', {class: 'modal-bg', onclick: function (e) { if (e.target === bg) closeAuth(); }});
    var m = el('div', {class: 'modal'});
    bg.appendChild(m); document.body.appendChild(bg); authModal = bg;
    function draw() {
      m.innerHTML = '';
      m.appendChild(el('button', {class: 'x', text: '✕', onclick: closeAuth}));
      if (!KEY_READY) m.appendChild(el('div', {class: 'alert warn', text: 'کلید SUPABASE_ANON_KEY در فایل وارد نشده؛ ورود ممکن نیست.'}));
      var tabs = el('div', {class: 'tabs'}, [
        el('button', {class: state.mode === 'login' ? 'on' : '', text: 'ورود', onclick: function () { state.mode = 'login'; state.step = 'form'; draw(); }}),
        el('button', {class: state.mode === 'register' ? 'on' : '', text: 'ثبت‌نام', onclick: function () { state.mode = 'register'; state.step = 'form'; draw(); }})
      ]);
      m.appendChild(tabs);
      if (state.mode === 'login') drawLogin(); else drawRegister();
    }
    var msg = el('div');
    function setMsg(t, kind) { msg.innerHTML = ''; if (t) msg.appendChild(el('div', {class: 'alert ' + (kind || 'error'), text: t})); }
    function busy(btn, on) { btn.disabled = on; btn.textContent = on ? 'لطفاً صبر کنید…' : btn.dataset.label; }
    function drawLogin() {
      m.appendChild(el('h2', {text: 'ورود به حساب'}));
      m.appendChild(msg); setMsg('');
      if (!state.otpMode) {
        var id = input('نام کاربری یا ایمیل', 'مثلاً ali_1385 یا name@example.com', 'text', true);
        var pw = input('رمز عبور', '', 'password', true);
        var b = el('button', {class: 'btn', text: 'ورود', 'data-label': 'ورود', style: 'width:100%'});
        b.addEventListener('click', async function () {
          setMsg(''); busy(b, true);
          try { user = await api.signInWithPassword(id.querySelector('input').value, pw.querySelector('input').value); afterLogin(); }
          catch (e) { setMsg(errMsg(e)); }
          busy(b, false);
        });
        pw.querySelector('input').addEventListener('keydown', function (e) { if (e.key === 'Enter') b.click(); });
        m.appendChild(id); m.appendChild(pw); m.appendChild(b);
        if (window.SiteExtras) m.appendChild(window.SiteExtras.googleButton('teacher'));
        m.appendChild(el('p', {class: 'center muted', style: 'margin:14px 0 0;font-size:13px'}, [
          el('a', {href: '#', text: 'ورود با کد ایمیل', onclick: function (e) { e.preventDefault(); state.otpMode = true; state.step = 'form'; draw(); }}), el('span', {text: ' · '}),
          el('a', {href: '#', text: 'فراموشی رمز', onclick: function (e) { e.preventDefault(); state.otpMode = 'recovery'; draw(); }})
        ]));
        m.appendChild(el('p', {class: 'muted', style: 'font-size:12px;margin-top:12px', text: 'دانش‌آموزان با نام کاربری و رمزی که معلم داده وارد می‌شوند. معلم و مدیر با نام کاربری یا ایمیل.'}));
      } else if (state.otpMode === 'recovery' && window.SiteExtras) {
        m.appendChild(el('p', {class: 'muted', style: 'font-size:13px', text: 'بازیابی رمز عبور با ایمیل حساب. پس از تأیید کد، رمز جدید بگذارید.'}));
        window.SiteExtras.recoveryFlow(m, api, setMsg, busy, async function () { user = await currentProfile(); afterLogin(); });
        m.appendChild(el('p', {class: 'center', style: 'margin-top:12px;font-size:13px'}, [el('a', {href: '#', text: 'بازگشت به ورود با رمز', onclick: function (e) { e.preventDefault(); state.otpMode = false; state.step = 'form'; draw(); }})]));
      } else {
        if (state.step === 'form') {
          var em = input('ایمیل', 'name@example.com', 'email', true);
          var b1 = el('button', {class: 'btn', text: 'ارسال کد', 'data-label': 'ارسال کد', style: 'width:100%'});
          b1.addEventListener('click', async function () {
            setMsg(''); busy(b1, true);
            try { state.email = requireEmail(em.querySelector('input').value); await api.sendLoginOtp(state.email); state.step = 'code'; draw(); }
            catch (e) { setMsg(errMsg(e)); }
            busy(b1, false);
          });
          m.appendChild(em); m.appendChild(b1);
        } else {
          m.appendChild(el('div', {class: 'alert info', text: 'کد ۶ تا ۸ رقمی به ' + state.email + ' فرستاده شد.'}));
          var code = input('کد یک‌بارمصرف', '', 'text', true);
          var b2 = el('button', {class: 'btn', text: 'تأیید و ورود', 'data-label': 'تأیید و ورود', style: 'width:100%'});
          b2.addEventListener('click', async function () {
            setMsg(''); busy(b2, true);
            try { user = await api.verifyLoginOtp(state.email, code.querySelector('input').value); afterLogin(); state.needPw = true; }
            catch (e) { setMsg(errMsg(e)); }
            busy(b2, false);
          });
          m.appendChild(code); m.appendChild(b2);
        }
        m.appendChild(el('p', {class: 'center', style: 'margin-top:12px;font-size:13px'}, [el('a', {href: '#', text: 'بازگشت به ورود با رمز', onclick: function (e) { e.preventDefault(); state.otpMode = false; state.step = 'form'; draw(); }})]));
      }
    }
    function drawRegister() {
      m.appendChild(el('h2', {text: 'ثبت‌نام ' + (state.role === 'manager' ? 'مدیر / معاون' : 'معلم')}));
      m.appendChild(el('div', {class: 'tabs'}, [
        el('button', {class: state.role === 'teacher' ? 'on' : '', text: '👩‍🏫 معلم', onclick: function () { state.role = 'teacher'; state.step = 'form'; draw(); }}),
        el('button', {class: state.role === 'manager' ? 'on' : '', text: '🏫 مدیر / معاون', onclick: function () { state.role = 'manager'; state.step = 'form'; draw(); }})
      ]));
      m.appendChild(msg); setMsg('');
      if (state.step === 'form') {
        var name = input('نام و نام خانوادگی', '', 'text', false);
        var em = input('ایمیل', 'name@example.com', 'email', true);
        var b1 = el('button', {class: 'btn', text: 'ارسال کد تأیید', 'data-label': 'ارسال کد تأیید', style: 'width:100%'});
        b1.addEventListener('click', async function () {
          setMsg(''); busy(b1, true);
          try { state.fullName = name.querySelector('input').value; state.email = requireEmail(em.querySelector('input').value); await api.sendRegistrationOtp(state.email, state.fullName, state.role); state.step = 'code'; draw(); }
          catch (e) { setMsg(errMsg(e)); }
          busy(b1, false);
        });
        m.appendChild(name); m.appendChild(em); m.appendChild(b1);
        m.appendChild(el('p', {class: 'muted', style: 'font-size:12px;margin-top:12px', text: 'دانش‌آموزان نیازی به ثبت‌نام ندارند؛ معلم برایشان حساب می‌سازد.'}));
      } else if (state.step === 'code') {
        m.appendChild(el('div', {class: 'alert info', text: 'کد تأیید به ' + state.email + ' فرستاده شد.'}));
        var code = input('کد تأیید ایمیل', '', 'text', true);
        var b2 = el('button', {class: 'btn', text: 'تأیید کد', 'data-label': 'تأیید کد', style: 'width:100%'});
        b2.addEventListener('click', async function () {
          setMsg(''); busy(b2, true);
          try { await api.verifyRegistrationOtp(state.email, code.querySelector('input').value); state.step = 'complete'; draw(); }
          catch (e) { setMsg(errMsg(e)); }
          busy(b2, false);
        });
        m.appendChild(code); m.appendChild(b2);
      } else {
        drawCompletion(m, state.role, state.fullName, msg, setMsg);
      }
    }
    function input(label, ph, type, ltr) {
      return el('div', {class: 'field' + (ltr ? ' ltr' : '')}, [el('label', {text: label}), el('input', {type: type || 'text', placeholder: ph || '', autocomplete: type === 'password' ? 'current-password' : 'on'})]);
    }
    draw();
  }
  /* تکمیل ثبت‌نام (نام کاربری + رمز + مدرسه) — هم پس از OTP و هم برای حساب‌های نیمه‌کاره (requires_teacher_setup) */
  function drawCompletion(container, role, fullName, msg, setMsg) {
    container.appendChild(el('div', {class: 'alert ok', text: 'ایمیل تأیید شد. حساب خود را کامل کنید.'}));
    var name = fld('نام و نام خانوادگی', fullName || '');
    var un = fld('نام کاربری (انگلیسی، ۴ تا ۲۰ حرف)', '', true);
    var pw = fld('رمز عبور (حداقل ۸ کاراکتر)', '', true, 'password');
    container.appendChild(name); container.appendChild(un); container.appendChild(pw);
    var extra = {};
    if (role === 'manager') {
      extra.school = fld('نام مدرسه', ''); extra.province = fld('استان', ''); extra.city = fld('شهر', '');
      container.appendChild(extra.school); container.appendChild(el('div', {class: 'grid2'}, [extra.province, extra.city]));
    } else {
      extra.invite = fld('کد دعوت مدرسه (اختیاری)', '', true);
      container.appendChild(extra.invite);
    }
    var b = el('button', {class: 'btn', text: 'تکمیل ثبت‌نام', style: 'width:100%'});
    b.addEventListener('click', async function () {
      setMsg(''); b.disabled = true;
      try {
        var v = function (f) { return f.querySelector('input').value; };
        if (role === 'manager') user = await api.completeManager(v(name), v(un), v(pw), v(extra.school), v(extra.province), v(extra.city));
        else user = await api.completeTeacher(v(name), v(un), v(pw), v(extra.invite));
        afterLogin();
      } catch (e) { setMsg(errMsg(e)); }
      b.disabled = false;
    });
    container.appendChild(b);
    function fld(label, val, ltr, type) { return el('div', {class: 'field' + (ltr ? ' ltr' : '')}, [el('label', {text: label}), el('input', {type: type || 'text', value: val || ''})]); }
  }
  function afterLogin() {
    closeAuth();
    if (user && user.requiresSetup) { renderSetupGate(); return; }
    view.panel = 'dashboard';
    toast('خوش آمدید، ' + (user.name || ''), 'ok');
    render();
  }
  function renderSetupGate() {
    root = $('root'); root.innerHTML = '';
    var m = el('div', {class: 'modal', style: 'margin:60px auto'});
    m.appendChild(el('h2', {text: 'تکمیل ثبت‌نام ' + (user.pendingRole === 'manager' ? 'مدیر / معاون' : 'معلم')}));
    var msg = el('div'); m.appendChild(msg);
    drawCompletion(m, user.pendingRole || 'teacher', user.name, msg, function (t) { msg.innerHTML = ''; if (t) msg.appendChild(el('div', {class: 'alert error', text: t})); });
    m.appendChild(el('button', {class: 'btn light', style: 'width:100%;margin-top:10px', text: 'خروج', onclick: doLogout}));
    root.appendChild(m);
  }
  async function doLogout() { await authApi.signOut(); user = null; view.panel = 'dashboard'; render(); }

  /* ---------------- پنل ---------------- */
  var MENUS = {
    teacher: [
      ['dashboard', '🏠', 'داشبورد'], ['exams', '📝', 'آزمون‌ها'], ['builder', '➕', 'آزمون جدید'], ['classes', '🏫', 'کلاس‌ها'], ['students', '🎓', 'دانش‌آموزان'], ['bank', '🏦', 'بانک سؤال'], ['reports', '📈', 'گزارش‌ها'],
      ['grading', '✅', 'تصحیح'], ['calendar', '📅', 'تقویم و پیام‌ها'], ['wallet', '👛', 'کیف پول'], ['tools', '🧮', 'ابزارها'], '-', ['profile', '👤', 'پروفایل']
    ],
    student: [['dashboard', '🏠', 'داشبورد'], ['join', '🔑', 'شرکت در آزمون'], ['grades', '📊', 'کارنامه'], ['calendar', '📅', 'تقویم و پیام‌ها'], ['tools', '🧮', 'ابزارها'], '-', ['profile', '👤', 'پروفایل']],
    manager: [['dashboard', '🏠', 'داشبورد'], ['teachers', '👩‍🏫', 'معلم‌ها'], ['school', '🏫', 'مدرسه'], ['wallet', '👛', 'کیف پول'], ['tools', '🧮', 'ابزارها'], '-', ['profile', '👤', 'پروفایل']]
  };
  var ROLE_LABEL = {teacher: 'معلم', student: 'دانش‌آموز', manager: 'مدیر / معاون'};
  /* V147 — PWA: ثبت Service Worker، پیشنهاد نصب (اندروید/کروم) و راهنمای iOS؛ اعلان نسخهٔ جدید */
  var deferredInstall = null;
  function pwaInit() {
    if (!('serviceWorker' in navigator) || location.protocol !== 'https:') return;
    navigator.serviceWorker.register('/pwa/sw.js').then(function (reg) {
      reg.addEventListener('updatefound', function () {
        var nw = reg.installing; if (!nw) return;
        nw.addEventListener('statechange', function () { if (nw.state === 'installed' && navigator.serviceWorker.controller) toast('نسخهٔ جدید سایت آماده است؛ صفحه را دوباره باز کنید.', 'ok'); });
      });
    }).catch(function (e) { console.warn('sw', e); });
    window.addEventListener('beforeinstallprompt', function (e) { e.preventDefault(); deferredInstall = e; pwaOffer(); });
    window.addEventListener('appinstalled', function () { deferredInstall = null; var b = $('pwa-bar'); if (b) b.remove(); try { localStorage.setItem('pwa.installed', '1'); } catch (x) {} toast('آزمون‌ساز روی دستگاه نصب شد.', 'ok'); });
    var ios = /iphone|ipad|ipod/i.test(navigator.userAgent) && !window.MSStream;
    var standalone = window.matchMedia('(display-mode: standalone)').matches || navigator.standalone === true;
    if (ios && !standalone) setTimeout(pwaOffer, 4000);
  }
  function pwaOffer() {
    if ($('pwa-bar')) return;
    try { if (localStorage.getItem('pwa.dismiss') && Date.now() - Number(localStorage.getItem('pwa.dismiss')) < 7 * 864e5) return; } catch (e) {}
    if (window.matchMedia('(display-mode: standalone)').matches || navigator.standalone === true) return;
    var ios = !deferredInstall;
    var bar = el('div', {class: 'pwa-bar', id: 'pwa-bar'}, [
      el('img', {src: '/pwa/icon-192.png', alt: ''}),
      el('div', {class: 't'}, [el('b', {text: 'نصب آزمون‌ساز روی گوشی'}), el('span', {text: ios ? 'در Safari دکمهٔ «اشتراک» و سپس «Add to Home Screen» را بزنید.' : 'مثل یک برنامه، تمام‌صفحه و با آیکون روی صفحهٔ اصلی.'})]),
      ios ? null : el('button', {class: 'btn', text: 'نصب', onclick: function () { if (!deferredInstall) return; deferredInstall.prompt(); deferredInstall.userChoice.then(function () { deferredInstall = null; bar.remove(); }); }}),
      el('button', {class: 'x', text: '✕', 'aria-label': 'بستن', onclick: function () { bar.remove(); try { localStorage.setItem('pwa.dismiss', String(Date.now())); } catch (e) {} }})
    ].filter(Boolean));
    document.body.appendChild(bar);
  }
  function toggleSidebar() { var sb = $('sidebar'), bg = $('sb-bg'); if (!sb) return; var open = sb.classList.toggle('open'); if (bg) bg.classList.toggle('on', open); }
  function closeSidebar() { var sb = $('sidebar'), bg = $('sb-bg'); if (sb) sb.classList.remove('open'); if (bg) bg.classList.remove('on'); }
  /* V146 — جدول‌های پهن روی گوشی به‌صورت افقی اسکرول می‌شوند (بعد از هر render) */
  function wrapTables(rootEl) {
    (rootEl || document).querySelectorAll('table.tbl').forEach(function (t) {
      if (t.parentElement && t.parentElement.classList.contains('tbl-wrap')) return;
      var w = document.createElement('div'); w.className = 'tbl-wrap'; t.parentNode.insertBefore(w, t); w.appendChild(t);
    });
  }
  if (window.MutationObserver) new MutationObserver(function () { wrapTables(document); }).observe(document.documentElement, {childList: true, subtree: true});
  function renderPanel() {
    var menu = MENUS[user.role] || MENUS.student;
    var side = el('aside', {class: 'sidebar', id: 'sidebar'}, [
      el('div', {class: 'brand'}, [brandEl()]),
      el('div', {class: 'user'}, [
        el('div', {class: 'avatar', text: (user.name || '?').trim().charAt(0)}),
        el('div', {}, [el('div', {class: 'n', text: user.name || ''}), el('div', {class: 'r', text: ROLE_LABEL[user.role] + (user.username ? ' · ' + user.username : '')})])
      ]),
      el('div', {class: 'menu'}, menu.map(function (it) {
        if (it === '-') return el('div', {class: 'sep'});
        return el('button', {class: view.panel === it[0] ? 'on' : '', onclick: function () { view.panel = it[0]; view.arg = null; closeSidebar(); render(); }}, [el('span', {class: 'i', text: it[1]}), el('span', {text: it[2]})]);
      })),
      el('div', {class: 'foot'}, [el('button', {class: 'btn light', style: 'width:100%', text: 'خروج از حساب', onclick: doLogout})])
    ]);
    var title = (menu.filter(function (x) { return x !== '-' && x[0] === view.panel; })[0] || ['', '', ''])[2];
    var main = el('main', {class: 'main'}, [
      el('div', {class: 'head'}, [
        el('button', {class: 'icon-btn hamb', html: '☰', 'aria-label': 'منو', onclick: toggleSidebar}),
        el('h1', {text: title}),
        el('span', {class: 'chip brand', text: 'نسخهٔ وب · فاز ۶'})
      ]),
      el('div', {id: 'content'})
    ]);
    /* V146 — گوشی/تبلت: پس‌زمینهٔ سایدبار + منوی پایین (۴ مورد اول + «بیشتر» که سایدبار را باز می‌کند) */
    var sbBg = el('div', {class: 'sb-bg', id: 'sb-bg', onclick: closeSidebar});
    var items = menu.filter(function (x) { return x !== '-'; });
    var primary = items.slice(0, 4);
    var bottom = el('nav', {class: 'bottom-nav', 'aria-label': 'منوی پایین'}, primary.map(function (it) {
      return el('button', {class: view.panel === it[0] ? 'on' : '', onclick: function () { view.panel = it[0]; view.arg = null; render(); }}, [el('span', {class: 'i', text: it[1]}), el('span', {text: it[2]})]);
    }).concat([el('button', {class: primary.some(function (it) { return it[0] === view.panel; }) ? '' : 'on', onclick: toggleSidebar}, [el('span', {class: 'i', text: '☰'}), el('span', {text: 'بیشتر'})])]));
    root.appendChild(el('div', {class: 'app'}, [side, sbBg, main, bottom]));
    renderPage($('content'));
  }
  /* V148 — رندر محتوای پنل جاری در هر ظرفی (پنل دسکتاپ یا پوستهٔ موبایل) */
  function renderPage(c) {
    var pages = {dashboard: pageDashboard, exams: pageExams, classes: pageClasses, students: pageStudents, wallet: pageWallet, tools: pageTools, profile: pageProfile, grades: pageGrades, teachers: pageTeachers,
      builder: function (c) { if (window.SiteBuilder) window.SiteBuilder.page(c, view.arg); else soon('سازندهٔ آزمون', 'فاز ۲')(c); }, bank: function (c) { if (window.SiteSchool) window.SiteSchool.bankPage(c); }, reports: function (c) { if (window.SiteExtras) window.SiteExtras.reportsPage(c); }, grading: function (c) { if (window.SiteAdmin) window.SiteAdmin.gradingPage(c, view.arg); else soon('تصحیح', 'فاز ۴')(c); }, calendar: function (c) { if (window.SiteAdmin) window.SiteAdmin.calendarPage(c, view.arg); }, join: function (c) { if (window.SiteStudent) window.SiteStudent.page(c, view.arg); else soon('شرکت در آزمون', 'فاز ۳')(c); }, school: function (c) { if (window.SiteAdmin) window.SiteAdmin.managerSchoolPage(c, view.arg); else soon('مدرسه', 'فاز ۴')(c); }};
    (pages[view.panel] || pageDashboard)(c);
  }
  function soon(title, phase) { return function (c) { c.appendChild(el('div', {class: 'soon', html: '<div style="font-size:40px">🚧</div><h3>' + esc(title) + '</h3>این بخش در <b>' + esc(phase) + '</b> سایت فعال می‌شود. فعلاً از برنامهٔ اندروید استفاده کنید.'})); }; }
  function loading(c) { c.innerHTML = '<div class="loading"><span class="spinner"></span> در حال دریافت…</div>'; }
  function showErr(c, e) { c.innerHTML = ''; c.appendChild(el('div', {class: 'alert error', text: errMsg(e)})); }
  function emptyBox(icon, text) { return el('div', {class: 'empty'}, [el('div', {class: 'big', text: icon}), el('div', {text: text})]); }
  function statCard(v, l) { return el('div', {class: 'card stat'}, [el('div', {class: 'v num', text: v}), el('div', {class: 'l', text: l})]); }

  /* ---- داشبورد ---- */
  async function pageDashboard(c) {
    loading(c);
    try {
      if (user.role === 'teacher') {
        var r = await Promise.all([api.exams().catch(function () { return []; }), api.classes().catch(function () { return []; }), api.students().catch(function () { return []; }), api.wallet().catch(function () { return {balance: 0}; })]);
        c.innerHTML = '';
        if (window.SiteSchool) c.appendChild(await window.SiteSchool.managerRequestsCard());
        c.appendChild(el('div', {class: 'grid4'}, [statCard(fa(r[0].length), 'آزمون'), statCard(fa(r[1].length), 'کلاس'), statCard(fa(r[2].length), 'دانش‌آموز'), statCard(money(r[3].balance), 'موجودی کیف پول')]));
        var open = r[0].filter(function (x) { return x.is_open; });
        var card = el('div', {class: 'card', style: 'margin-top:16px'}, [el('h3', {text: '📝 آخرین آزمون‌ها'})]);
        if (!r[0].length) card.appendChild(emptyBox('📄', 'هنوز آزمونی نساخته‌اید.'));
        else card.appendChild(examTable(r[0].slice(0, 6), c));
        c.appendChild(card);
        c.appendChild(el('div', {class: 'alert info', style: 'margin-top:16px', text: fa(open.length) + ' آزمون هم‌اکنون باز است. برای ساخت آزمون جدید فعلاً از برنامهٔ اندروید استفاده کنید (فاز ۲ سایت).'}));
      } else if (user.role === 'student') {
        var g = await api.myGrades().catch(function () { return []; });
        c.innerHTML = '';
        var graded = g.filter(function (x) { return x.graded_at; });
        var avg = graded.length ? graded.reduce(function (s, x) { return s + (Number(x.total_score) ? Number(x.total_grade) / Number(x.total_score) * 100 : 0); }, 0) / graded.length : 0;
        c.appendChild(el('div', {class: 'grid3'}, [statCard(fa(g.length), 'آزمون شرکت‌کرده'), statCard(fa(graded.length), 'تصحیح‌شده'), statCard(fa(Math.round(avg)) + '٪', 'میانگین درصد')]));
        c.appendChild(el('div', {class: 'alert info', style: 'margin-top:16px', html: 'برای شرکت در آزمون، کد معلم را در بخش <b>شرکت در آزمون</b> وارد کنید.'}));
        c.appendChild(el('div', {class: 'row', style: 'margin-top:12px'}, [el('button', {class: 'btn', text: '🔑 شرکت در آزمون', onclick: function () { view.panel = 'join'; view.arg = null; render(); }}), (window.SiteStudent && window.SiteStudent.hasActive()) ? el('span', {class: 'chip warn', text: 'آزمون نیمه‌تمام دارید'}) : null]));
      } else {
        var s = await api.managerSummary();
        c.innerHTML = '';
        c.appendChild(el('div', {class: 'card'}, [el('h3', {text: '🏫 ' + (s.school_name || 'مدرسه')}), el('div', {class: 'muted', text: [s.province, s.city].filter(Boolean).join('، ')})]));
        c.appendChild(el('div', {class: 'grid4', style: 'margin-top:16px'}, [statCard(fa(s.teachers || 0), 'معلم'), statCard(fa(s.students || 0), 'دانش‌آموز'), statCard(fa(s.classes || 0), 'کلاس'), statCard(fa(s.exams || 0), 'آزمون')]));
        c.appendChild(el('div', {class: 'grid3', style: 'margin-top:16px'}, [statCard(fa(s.answers || 0), 'پاسخ ثبت‌شده'), statCard(fa(Math.round(Number(s.average_percent) || 0)) + '٪', 'میانگین مدرسه'), statCard(money(s.distributed_toman || 0), 'اعتبار توزیع‌شده')]));
        var acts = s.teacher_activity || [];
        if (acts.length) {
          var t = el('table', {class: 'tbl'}, [el('thead', {}, [el('tr', {}, ['معلم', 'آزمون', 'کلاس', 'دانش‌آموز', 'کیف پول'].map(function (h) { return el('th', {text: h}); }))]),
            el('tbody', {}, acts.map(function (a) { return el('tr', {}, [el('td', {text: a.name || ''}), el('td', {text: fa(a.exams || 0)}), el('td', {text: fa(a.classes || 0)}), el('td', {text: fa(a.students || 0)}), el('td', {text: money(a.wallet_balance || 0)})]); }))]);
          c.appendChild(el('div', {class: 'card', style: 'margin-top:16px'}, [el('h3', {text: '📈 فعالیت معلم‌ها'}), t]));
        }
      }
    } catch (e) { showErr(c, e); }
  }

  /* ---- آزمون‌ها ---- */
  function examTable(list, c) {
    return el('table', {class: 'tbl'}, [
      el('thead', {}, [el('tr', {}, ['عنوان', 'درس', 'کد', 'وضعیت', 'بارم', 'تاریخ', ''].map(function (h) { return el('th', {text: h}); }))]),
      el('tbody', {}, list.map(function (x) {
        return el('tr', {}, [el('td', {html: '<b>' + esc(x.title || 'بدون عنوان') + '</b>'}), el('td', {text: x.subject || '—'}), el('td', {}, [el('span', {class: 'code', text: x.code || '—'})]),
          el('td', {}, [el('span', {class: 'chip ' + (x.is_open ? 'ok' : 'off'), text: x.is_open ? 'باز' : 'بسته'})]), el('td', {text: fa(fmtScore(x.total_score))}), el('td', {class: 'muted', style: 'font-size:12px', text: fmtDate(x.created_at)}),
          el('td', {}, [examActions(x, function () { render(); })])]);
      }))
    ]);
  }
  function examActions(x, refresh) {
    var wrap = el('div', {class: 'acts'});
    wrap.appendChild(el('button', {class: 'icon-btn', title: 'ویرایش', html: '✎', onclick: function () { view.panel = 'builder'; view.arg = {examId: x.id}; render(); }}));
    wrap.appendChild(el('button', {class: 'icon-btn', title: 'پیش‌نمایش و چاپ', html: '🖨', onclick: function () { printExam(x); }}));
    wrap.appendChild(el('button', {class: 'icon-btn', title: x.is_open ? 'بستن آزمون' : 'بازکردن آزمون', html: x.is_open ? '🔒' : '🔓', onclick: async function () {
      try { await api.setExamOpen(x.id, !x.is_open); toast(x.is_open ? 'آزمون بسته شد.' : 'آزمون باز شد.', 'ok'); refresh(); } catch (e) { toast(errMsg(e), 'err'); }
    }}));
    if (window.SiteExtras) wrap.appendChild(el('button', {class: 'icon-btn', title: 'صدور فایل آزمون', html: '📤', onclick: function () { window.SiteExtras.exportExamDlg(x); }}));
    wrap.appendChild(el('button', {class: 'icon-btn', title: 'کپی آزمون', html: '⧉', onclick: async function () {
      if (!(await confirmDlg('کپی آزمون', 'از «' + esc(x.title) + '» یک نسخهٔ جدید ساخته می‌شود (هزینهٔ سؤال‌ها طبق تعرفه کسر می‌شود).', 'کپی'))) return;
      try { var r = await api.duplicateExam(x.id); toast('کپی شد؛ کد جدید: ' + (r.code || '') + (r.cost ? ' · هزینه ' + money(r.cost) : ''), 'ok'); refresh(); } catch (e) { toast(errMsg(e), 'err'); }
    }}));
    wrap.appendChild(el('button', {class: 'icon-btn danger', title: 'حذف', html: '🗑', onclick: async function () {
      if (!(await confirmDlg('حذف آزمون', 'آزمون «' + esc(x.title) + '» و پاسخ‌های آن برای همیشه حذف می‌شود.', 'حذف', true))) return;
      try { await api.deleteExam(x.id); toast('حذف شد.', 'ok'); refresh(); } catch (e) { toast(errMsg(e), 'err'); }
    }}));
    return wrap;
  }
  async function printExam(x) {
    toast('در حال آماده‌سازی پیش‌نمایش…');
    try {
      var exam = await api.examDetail(x.id);
      var header = null; try { header = (await api.profile()).header; } catch (e) {}
      openPrintPreview(buildPrintPayload(exam, {header: header}), {title: exam.title, examId: exam.id});
    } catch (e) { toast(errMsg(e), 'err'); }
  }
  async function pageExams(c) {
    loading(c);
    try {
      var list = await api.exams();
      c.innerHTML = '';
      var q = el('input', {type: 'search', placeholder: 'جست‌وجو در عنوان/درس/کد…', style: 'border:1px solid var(--line);border-radius:10px;padding:9px 12px;min-width:260px'});
      var grid = el('div', {class: 'exam-grid'});
      function draw() {
        var s = q.value.trim().toLowerCase();
        var f = list.filter(function (x) { return !s || [x.title, x.subject, x.code].join(' ').toLowerCase().indexOf(s) >= 0; });
        grid.innerHTML = '';
        if (!f.length) { grid.appendChild(emptyBox('📄', list.length ? 'موردی یافت نشد.' : 'هنوز آزمونی نساخته‌اید.')); return; }
        f.forEach(function (x) {
          grid.appendChild(el('div', {class: 'exam'}, [
            el('div', {class: 'row'}, [el('div', {class: 't grow', text: x.title || 'بدون عنوان'}), el('span', {class: 'chip ' + (x.is_open ? 'ok' : 'off'), text: x.is_open ? 'باز' : 'بسته'})]),
            el('div', {class: 'meta'}, [el('span', {text: '📚 ' + (x.subject || '—')}), el('span', {text: '⏱ ' + fa(x.duration || 0) + ' دقیقه'}), el('span', {text: '∑ ' + fa(fmtScore(x.total_score))})]),
            el('div', {class: 'row'}, [el('span', {class: 'muted', style: 'font-size:12px', text: 'کد:'}), el('span', {class: 'code', text: x.code || '—'}), el('span', {class: 'grow'}), el('span', {class: 'muted', style: 'font-size:12px', text: fmtDate(x.created_at)})]),
            examActions(x, function () { pageExams(c); })
          ]));
        });
      }
      q.addEventListener('input', draw);
      c.appendChild(el('div', {class: 'row', style: 'margin-bottom:16px'}, [q, el('span', {class: 'grow'}), el('span', {class: 'muted', text: fa(list.length) + ' آزمون'}), window.SiteExtras ? el('button', {class: 'btn light', text: '📥 وارد کردن', onclick: window.SiteExtras.importExam}) : null, el('button', {class: 'btn', text: '➕ آزمون جدید', onclick: function () { view.panel = 'builder'; view.arg = null; render(); }})]));
      c.appendChild(grid); draw();
    } catch (e) { showErr(c, e); }
    if (window.SiteBuilder) c.appendChild(window.SiteBuilder.printExamsSection(function () { pageExams(c); }));
  }

  /* ---- کلاس‌ها ---- */
  async function pageClasses(c) {
    loading(c);
    if (view.arg && view.arg.create) { view.arg = null; classForm(null, function () { pageClasses(c); }); }
    try {
      var list = await api.classes();
      c.innerHTML = '';
      c.appendChild(el('div', {class: 'row', style: 'margin-bottom:16px'}, [el('span', {class: 'muted', text: fa(list.length) + ' کلاس'}), el('span', {class: 'grow'}), el('button', {class: 'btn', text: '➕ کلاس جدید', onclick: function () { classForm(null, function () { pageClasses(c); }); }})]));
      if (!list.length) { c.appendChild(el('div', {class: 'card'}, [emptyBox('🏫', 'هنوز کلاسی نساخته‌اید.')])); return; }
      c.appendChild(el('div', {class: 'card'}, [el('table', {class: 'tbl'}, [
        el('thead', {}, [el('tr', {}, ['نام کلاس', 'پایه', 'رشته', 'پسر', 'دختر', 'کل', 'اشتراک با مدیر', ''].map(function (h) { return el('th', {text: h}); }))]),
        el('tbody', {}, list.map(function (k) {
          return el('tr', {}, [el('td', {html: '<b>' + esc(k.name) + '</b>'}), el('td', {text: k.grade || '—'}), el('td', {text: k.field_of_study || '—'}), el('td', {text: fa(k.boys || 0)}), el('td', {text: fa(k.girls || 0)}), el('td', {text: fa(k.total || 0)}),
            el('td', {}, [window.SiteSchool ? window.SiteSchool.classShareChip(k, function () { pageClasses(c); }) : el('span', {class: 'chip ' + (k.shared_with_manager ? 'ok' : 'off'), text: k.shared_with_manager ? 'بله' : 'خیر'})]),
            el('td', {}, [el('div', {class: 'acts'}, [
              el('button', {class: 'icon-btn', title: 'فهرست دانش‌آموزان', html: '👥', onclick: function () { rosterDlg(k); }}),
              el('button', {class: 'icon-btn', title: 'ویرایش', html: '✎', onclick: function () { classForm(k, function () { pageClasses(c); }); }}),
              el('button', {class: 'icon-btn danger', title: 'حذف', html: '🗑', onclick: async function () {
                if (!(await confirmDlg('حذف کلاس', 'کلاس «' + esc(k.name) + '» حذف می‌شود؛ حساب دانش‌آموزان حفظ می‌ماند.', 'حذف', true))) return;
                try { await api.deleteClass(k.id); toast('حذف شد.', 'ok'); pageClasses(c); } catch (e) { toast(errMsg(e), 'err'); }
              }})
            ])])]);
        }))
      ])]));
    } catch (e) { showErr(c, e); }
  }
  function classForm(k, done) {
    var bg = el('div', {class: 'modal-bg'});
    var name = fld('نام کلاس', k ? k.name : ''), grade = fld('پایه', k ? (k.grade || '') : ''), field = fld('رشته', k ? (k.field_of_study || '') : '');
    var msg = el('div');
    var b = el('button', {class: 'btn', text: k ? 'ذخیره' : 'ایجاد'});
    b.addEventListener('click', async function () {
      b.disabled = true; msg.innerHTML = '';
      try { await api.saveClass(k ? k.id : null, name.querySelector('input').value, grade.querySelector('input').value, field.querySelector('input').value); bg.remove(); toast('ذخیره شد.', 'ok'); done(); }
      catch (e) { msg.appendChild(el('div', {class: 'alert error', text: errMsg(e)})); }
      b.disabled = false;
    });
    bg.appendChild(el('div', {class: 'modal'}, [el('button', {class: 'x', text: '✕', onclick: function () { bg.remove(); }}), el('h2', {text: k ? 'ویرایش کلاس' : 'کلاس جدید'}), msg, name, el('div', {class: 'grid2'}, [grade, field]), el('div', {class: 'row'}, [b, el('button', {class: 'btn light', text: 'انصراف', onclick: function () { bg.remove(); }})])]));
    document.body.appendChild(bg);
    function fld(l, v) { return el('div', {class: 'field'}, [el('label', {text: l}), el('input', {type: 'text', value: v})]); }
  }
  async function rosterDlg(k) {
    if (window.SiteSchool) return window.SiteSchool.rosterDlg(k);
    var bg = el('div', {class: 'modal-bg', onclick: function (e) { if (e.target === bg) bg.remove(); }});
    var body = el('div'); loading(body);
    bg.appendChild(el('div', {class: 'modal wide'}, [el('button', {class: 'x', text: '✕', onclick: function () { bg.remove(); }}), el('h2', {text: '👥 دانش‌آموزان کلاس ' + k.name}), body]));
    document.body.appendChild(bg);
    try { var list = await api.roster(k.id); body.innerHTML = ''; body.appendChild(studentTable(list)); } catch (e) { showErr(body, e); }
  }
  function studentTable(list) {
    if (!list || !list.length) return emptyBox('🎓', 'دانش‌آموزی ثبت نشده است.');
    return el('table', {class: 'tbl'}, [
      el('thead', {}, [el('tr', {}, ['نام', 'نام کاربری', 'جنسیت', 'پایه', 'رشته', 'کلاس‌ها', 'وضعیت'].map(function (h) { return el('th', {text: h}); }))]),
      el('tbody', {}, list.map(function (s) {
        return el('tr', {}, [el('td', {html: '<b>' + esc(s.full_name || ((s.first_name || '') + ' ' + (s.last_name || ''))) + '</b>'}), el('td', {}, [el('span', {class: 'code', text: s.username || '—'})]),
          el('td', {text: s.gender === 'male' ? 'پسر' : (s.gender === 'female' ? 'دختر' : '—')}), el('td', {text: s.grade || '—'}), el('td', {text: s.field_of_study || '—'}), el('td', {class: 'muted', text: s.class_names || '—'}),
          el('td', {}, [el('span', {class: 'chip ' + (s.is_active !== false ? 'ok' : 'off'), text: s.is_active !== false ? 'فعال' : 'غیرفعال'})])]);
      }))
    ]);
  }
  async function pageStudents(c) {
    if (window.SiteSchool) return window.SiteSchool.studentsPage(c);
    loading(c);
    try {
      var list = await api.students();
      c.innerHTML = '';
      var q = el('input', {type: 'search', placeholder: 'جست‌وجوی نام یا نام کاربری…', style: 'border:1px solid var(--line);border-radius:10px;padding:9px 12px;min-width:260px'});
      var box = el('div', {class: 'card'});
      function draw() { var s = q.value.trim().toLowerCase(); box.innerHTML = ''; box.appendChild(studentTable(list.filter(function (x) { return !s || [x.full_name, x.username, x.class_names].join(' ').toLowerCase().indexOf(s) >= 0; }))); }
      q.addEventListener('input', draw);
      c.appendChild(el('div', {class: 'row', style: 'margin-bottom:16px'}, [q, el('span', {class: 'grow'}), el('span', {class: 'muted', text: fa(list.length) + ' دانش‌آموز'})]));
      c.appendChild(el('div', {class: 'alert info', text: 'افزودن/ویرایش دانش‌آموز (ساخت حساب) در فاز ۴ سایت فعال می‌شود.'}));
      c.appendChild(box); draw();
    } catch (e) { showErr(c, e); }
  }

  /* ---- کیف پول ---- */
  async function pageWallet(c) {
    loading(c);
    try {
      var w = await api.wallet();
      c.innerHTML = '';
      /* V156 — مثل WalletScreen اپ: کارت موجودی (با چشم مخفی‌کردن)، شارژ امن، گردش‌های اخیر با شرح فارسی */
      var hidden = false, val = el('div', {class: 'v num', text: money(w.balance)});
      var eye = el('button', {class: 'bal-eye', 'aria-label': 'مخفی‌کردن موجودی', text: '👁', onclick: function () { hidden = !hidden; val.textContent = hidden ? '••••••••' : money(w.balance); eye.textContent = hidden ? '🙈' : '👁'; }});
      c.appendChild(el('div', {class: 'balance'}, [el('div', {class: 'row', style: 'align-items:center'}, [el('div', {class: 'grow', style: 'opacity:.9;font-weight:700', text: '👛 موجودی کیف پول'}), eye]), val, el('div', {style: 'font-size:13px;opacity:.85', text: 'هزینه هر سؤال: ' + fa('1,000') + ' تومان'})]));
      if (window.SiteAdmin && user.role !== 'student') c.appendChild(window.SiteAdmin.topUpCard(w.balance, function () { pageWallet(c); }));
      var card = el('div', {class: 'card', style: 'margin-top:16px'}, [el('h3', {text: '🧾 گردش‌های اخیر'})]);
      if (!w.transactions.length) card.appendChild(emptyBox('🧾', 'هنوز تراکنشی ثبت نشده است.'));
      else card.appendChild(el('table', {class: 'tbl'}, [
        el('thead', {}, [el('tr', {}, ['تاریخ', 'شرح', 'مبلغ', 'مانده'].map(function (h) { return el('th', {text: h}); }))]),
        el('tbody', {}, w.transactions.map(function (t) { var a = Number(t.amount) || 0; return el('tr', {}, [el('td', {class: 'muted', style: 'font-size:12px', text: fmtDate(t.created_at)}), el('td', {text: faReason(t.reason)}), el('td', {class: 'tx-amt ' + (a >= 0 ? 'pos' : 'neg'), text: (a >= 0 ? '+ ' : '− ') + money(Math.abs(a))}), el('td', {text: money(t.balance_after || 0)})]); }))
      ]));
      c.appendChild(card);
    } catch (e) { showErr(c, e); }
  }

  /* ---- ابزارها ---- */
  function pageTools(c) {
    var ta = el('textarea', {rows: 5, style: 'width:100%;border:1px solid var(--line);border-radius:10px;padding:10px', placeholder: 'متن سؤال را بنویسید؛ سپس «باز کردن ویرایشگر فرمول» را بزنید. فرمول‌ها بین $…$ درج می‌شوند.'});
    ta.value = localState().formulaDraft || '';
    var out = el('div', {class: 'muted', style: 'font-size:13px;margin-top:8px'});
    var b = el('button', {class: 'btn', text: '🧮 باز کردن ویرایشگر فرمول', onclick: function () {
      var s = ta.selectionStart, e = ta.selectionEnd;
      openFormulaEditor(ta.value, s, e).then(function (t) { if (t != null) { ta.value = t; setLocalState({formulaDraft: t}); out.textContent = 'متن به‌روز شد (' + fa(t.length) + ' نویسه).'; } });
    }});
    var p = el('button', {class: 'btn soft', text: '🖨 پیش‌نمایش چاپ این متن', onclick: function () {
      var exam = {title: 'پیش‌نمایش', subject: '', duration: 0, total_score: 1, questions: [{type: 'essay', text: ta.value || 'متن نمونه', score: 1, answerLines: 5}], __answers: []};
      openPrintPreview(buildPrintPayload(exam), {title: 'پیش‌نمایش'});
    }});
    ta.addEventListener('input', function () { setLocalState({formulaDraft: ta.value}); });
    c.appendChild(el('div', {class: 'card formula-demo'}, [el('h3', {text: '🧮 ویرایشگر فرمول و پیش‌نمایش چاپ'}), el('p', {class: 'muted', text: 'همان دو موتور برنامهٔ اندروید (formula.html و exam_print_renderer) این‌جا اجرا می‌شوند. متن با فرمول را بنویسید، در پیش‌نمایش ببینید و چاپ بگیرید.'}), ta, el('div', {class: 'row', style: 'margin-top:12px'}, [b, p]), out]));
    c.appendChild(el('div', {class: 'card'}, [el('h3', {text: '🖨 نمونهٔ آزمون چاپی'}), el('p', {class: 'muted', text: 'یک آزمون نمونه با همهٔ انواع سؤال برای آشنایی با موتور صفحه‌بندی، سربرگ‌ها و تنظیمات کاغذ.'}), el('button', {class: 'btn light', text: 'باز کردن نمونه', onclick: demoPrint})]));
  }

  /* ---- پروفایل ---- */
  async function pageProfile(c) {
    loading(c);
    try {
      var p = await api.profile();
      c.innerHTML = '';
      var msg = el('div');
      var f = {};
      function fld(key, label, val, ltr) { var i = el('input', {type: 'text', value: val || ''}); f[key] = i; return el('div', {class: 'field' + (ltr ? ' ltr' : '')}, [el('label', {text: label}), i]); }
      var card = el('div', {class: 'card'}, [
        el('div', {class: 'row', style: 'margin-bottom:16px'}, [el('div', {class: 'avatar lg', text: (p.fullName || '?').charAt(0)}), el('div', {}, [el('h3', {text: p.fullName}), el('div', {class: 'muted', text: ROLE_LABEL[p.role] + (p.username ? ' · ' + p.username : '') + (user.email && !/student\.exam\.local$/.test(user.email) ? ' · ' + user.email : '')})])]),
        el('p', {class: 'muted', style: 'font-size:12px', text: 'عکس پروفایل فقط روی دستگاه ذخیره می‌شود و به سرور نمی‌رود (V137.6).'}),
        msg
      ]);
      var grid = el('div', {class: 'grid2'});
      grid.appendChild(fld('displayName', 'نام نمایشی', p.displayName));
      if (p.role === 'teacher') { grid.appendChild(fld('firstName', 'نام', p.firstName)); grid.appendChild(fld('lastName', 'نام خانوادگی', p.lastName)); grid.appendChild(fld('employeeCode', 'کد پرسنلی', p.employeeCode, true)); grid.appendChild(fld('phone', 'تلفن همراه', p.phone, true)); }
      card.appendChild(grid);
      card.appendChild(el('h3', {text: '🏷 سربرگ چاپ', style: 'margin-top:8px'}));
      var hg = el('div', {class: 'grid3'});
      [['province', 'استان'], ['city', 'شهر'], ['district', 'منطقه'], ['school', 'مدرسه / واحد'], ['grade', 'پایه'], ['fieldOfStudy', 'رشته']].forEach(function (h) { hg.appendChild(fld('h_' + h[0], h[1], p.header[h[0]])); });
      card.appendChild(hg);
      var save = el('button', {class: 'btn', text: 'ذخیرهٔ تغییرات'});
      save.addEventListener('click', async function () {
        save.disabled = true; msg.innerHTML = '';
        try {
          await api.saveProfile({role: p.role, displayName: f.displayName.value, firstName: f.firstName ? f.firstName.value : '', lastName: f.lastName ? f.lastName.value : '', employeeCode: f.employeeCode ? f.employeeCode.value : '', phone: f.phone ? f.phone.value : '', avatarUrl: p.avatarUrl, avatarPublic: p.avatarPublic,
            header: {province: f.h_province.value, city: f.h_city.value, district: f.h_district.value, school: f.h_school.value, grade: f.h_grade.value, fieldOfStudy: f.h_fieldOfStudy.value}});
          toast('ذخیره شد.', 'ok'); user.name = f.displayName.value.trim() || p.fullName;
        } catch (e) { msg.appendChild(el('div', {class: 'alert error', text: errMsg(e)})); }
        save.disabled = false;
      });
      card.appendChild(el('div', {class: 'row', style: 'margin-top:10px'}, [save]));
      c.appendChild(card);
      // امنیت
      var sec = el('div', {class: 'card'}, [el('h3', {text: '🔐 امنیت حساب'})]);
      var smsg = el('div'); sec.appendChild(smsg);
      var pw1 = el('input', {type: 'password', placeholder: 'رمز جدید (حداقل ۸ کاراکتر)'}), pw2 = el('input', {type: 'password', placeholder: 'تکرار رمز جدید'});
      var bpw = el('button', {class: 'btn light', text: 'تغییر رمز عبور', onclick: async function () {
        smsg.innerHTML = '';
        try { if (pw1.value !== pw2.value) throw new Error('تکرار رمز یکسان نیست.'); await api.changePassword(pw1.value); toast('رمز عبور تغییر کرد.', 'ok'); pw1.value = pw2.value = ''; } catch (e) { smsg.appendChild(el('div', {class: 'alert error', text: errMsg(e)})); }
      }});
      sec.appendChild(el('div', {class: 'grid3'}, [el('div', {class: 'field ltr'}, [el('label', {text: 'رمز جدید'}), pw1]), el('div', {class: 'field ltr'}, [el('label', {text: 'تکرار'}), pw2]), el('div', {class: 'field'}, [el('label', {text: ' '}), bpw])]));
      if (p.role !== 'student') {
        var un = el('input', {type: 'text', value: p.username || '', placeholder: 'username'});
        var bun = el('button', {class: 'btn light', text: 'تغییر نام کاربری', onclick: async function () {
          smsg.innerHTML = '';
          try { if (!USERNAME_RE.test(un.value.trim().toLowerCase())) throw new Error('نام کاربری باید ۴ تا ۲۰ حرف انگلیسی، عدد یا زیرخط باشد.'); await api.updateUsername(un.value); toast('نام کاربری تغییر کرد.', 'ok'); user.username = un.value.trim().toLowerCase(); } catch (e) { smsg.appendChild(el('div', {class: 'alert error', text: errMsg(e)})); }
        }});
        sec.appendChild(el('div', {class: 'grid3'}, [el('div', {class: 'field ltr'}, [el('label', {text: 'نام کاربری'}), un]), el('div', {class: 'field'}, [el('label', {text: ' '}), bun])]));
      }
      c.appendChild(sec);
      if (p.role === 'teacher' && window.SiteSchool) c.appendChild(window.SiteSchool.joinSchoolCard(function () { pageProfile(c); }));
      if (p.role === 'teacher' && window.SiteExtras) { c.appendChild(window.SiteExtras.backupCard()); c.appendChild(window.SiteExtras.mediaDiagCard()); }
      if (p.role !== 'student' && window.SiteExtras) c.appendChild(window.SiteExtras.deleteAccountCard(async function () { saveSession(null); user = null; view.panel = 'dashboard'; render(); }));
    } catch (e) { showErr(c, e); }
  }

  /* ---- کارنامه (دانش‌آموز) ---- */
  async function pageGrades(c) {
    loading(c);
    try {
      var r = await Promise.all([api.myGrades().catch(function () { return []; }), api.myAnswers().catch(function () { return []; })]);
      var grades = r[0], answers = r[1];
      c.innerHTML = '';
      var card = el('div', {class: 'card'}, [el('div', {class: 'row'}, [el('h3', {class: 'grow', text: '📊 نمرات'}), window.SiteExtras && grades.length ? window.SiteExtras.gradesExcelButton(grades) : null])]);
      if (!grades.length) card.appendChild(emptyBox('📊', 'هنوز نمره‌ای ثبت نشده است.'));
      else card.appendChild(el('table', {class: 'tbl'}, [
        el('thead', {}, [el('tr', {}, ['آزمون', 'درس', 'ارسال', 'نمره', 'درصد', 'بازخورد'].map(function (h) { return el('th', {text: h}); }))]),
        el('tbody', {}, grades.map(function (g) { var ts = Number(g.total_score) || 0, tg = Number(g.total_grade) || 0; var pct = ts ? Math.round(tg / ts * 100) : 0; return el('tr', {}, [el('td', {html: '<b>' + esc(g.title || 'آزمون') + '</b>'}), el('td', {text: g.subject || '—'}), el('td', {class: 'muted', style: 'font-size:12px', text: fmtDate(g.submitted_at)}), el('td', {text: g.graded_at ? fa(fmtScore(tg)) + ' از ' + fa(fmtScore(ts)) : 'در انتظار تصحیح'}), el('td', {}, [g.graded_at ? el('span', {class: 'chip ' + (pct >= 50 ? 'ok' : 'off'), text: fa(pct) + '٪'}) : el('span', {class: 'chip off', text: '—'})]), el('td', {class: 'muted', text: g.feedback || '—'})]); }))
      ]));
      c.appendChild(card);
      if (answers.length) {
        var card2 = el('div', {class: 'card'}, [el('h3', {text: '🗂 پاسخ‌های ارسال‌شده'})]);
        card2.appendChild(el('table', {class: 'tbl'}, [
          el('thead', {}, [el('tr', {}, ['آزمون', 'درس', 'ارسال', 'وضعیت', 'نمره', ''].map(function (h) { return el('th', {text: h}); }))]),
          el('tbody', {}, answers.map(function (a) { return el('tr', {}, [el('td', {text: a.title || 'آزمون'}), el('td', {text: a.subject || '—'}), el('td', {class: 'muted', style: 'font-size:12px', text: fmtDate(a.submitted_at)}), el('td', {}, [el('span', {class: 'chip ' + (a.graded ? 'ok' : 'off'), text: a.graded ? 'تصحیح‌شده' : 'در انتظار'})]), el('td', {text: a.graded ? fa(fmtScore(a.total_grade)) + ' / ' + fa(fmtScore(a.total_score)) : '—'}), el('td', {}, [el('button', {class: 'btn light sm', text: 'جزئیات', onclick: function () { if (window.SiteAdmin) window.SiteAdmin.answerDetail(a.id); }})])]); }))
        ]));
        c.appendChild(card2);
      }
    } catch (e) { showErr(c, e); }
  }

  /* ---- معلم‌ها (مدیر) ---- */
  async function pageTeachers(c) {
    if (window.SiteAdmin) return window.SiteAdmin.managerTeachersPage(c);
    loading(c);
    try {
      var list = await api.managerTeachers();
      c.innerHTML = '';
      var card = el('div', {class: 'card'}, [el('h3', {text: '👩‍🏫 معلم‌های مدرسه'})]);
      if (!list.length) card.appendChild(emptyBox('👩‍🏫', 'هنوز معلمی عضو نشده است. دعوت معلم در فاز ۴ سایت.'));
      else card.appendChild(el('table', {class: 'tbl'}, [
        el('thead', {}, [el('tr', {}, ['نام', 'نام کاربری', 'ایمیل', 'کد پرسنلی', 'تلفن', 'وضعیت'].map(function (h) { return el('th', {text: h}); }))]),
        el('tbody', {}, list.map(function (t) { return el('tr', {}, [el('td', {html: '<b>' + esc(t.full_name || '') + '</b>'}), el('td', {}, [el('span', {class: 'code', text: t.username || '—'})]), el('td', {style: 'direction:ltr;text-align:right', text: t.email || '—'}), el('td', {text: t.employee_code || '—'}), el('td', {text: fa(t.phone || '—')}), el('td', {}, [el('span', {class: 'chip ' + (t.status === 'active' ? 'ok' : 'off'), text: t.status === 'active' ? 'فعال' : (t.status || '—')})])]); }))
      ]));
      c.appendChild(card);
    } catch (e) { showErr(c, e); }
  }

  /* ================================================================ راه‌اندازی */
  async function boot() {
    loadSession();
    try { pwaInit(); } catch (e) { console.warn('pwa', e); }
    if (window.SiteExtras && KEY_READY) { try { await window.SiteExtras.handleOAuthReturn(); } catch (e) { console.warn(e); } }
    document.addEventListener('click', function (e) { var sb = $('sidebar'); if (sb && sb.classList.contains('open') && !sb.contains(e.target) && !e.target.closest('.hamb') && !e.target.closest('.bottom-nav')) closeSidebar(); });
    document.addEventListener('keydown', function (e) { if (e.key === 'Escape') { closeAuth(); if (formulaCtx) return; if (printCtx) closePrintOverlay(); } });
    if (session && KEY_READY) {
      try { user = await currentProfile(); } catch (e) { user = null; if (/نشست|JWT|401/.test(errMsg(e))) saveSession(null); else setTimeout(function () { toast(errMsg(e), 'err'); }, 300); }
    }
    if (user && user.requiresSetup) { renderSetupGate(); return; }
    render();
  }
  /* V144 — بارگذاری رسانه: اول R2 (لینک موقت از Edge Function media-upload)، در نبود پیکربندی → Supabase Storage */
  var MEDIA_BUCKET = 'exam-images', r2Disabled = false;
  async function uploadMedia(blob, kind, folder, examId, ext, contentType) {
    if (!r2Disabled) {
      try {
        var t = await http('/functions/v1/media-upload', {method: 'POST', body: {kind: kind, folder: folder, exam_id: examId, ext: ext, size: blob.size}});
        if (t && t.upload_url) {
          /* نوع محتوا باید دقیقاً همان مقدار امضاشدهٔ سرور باشد (نه blob.type که ممکن است ;codecs=… داشته باشد) */
          var ct = (t.headers && t.headers['Content-Type']) || contentType;
          var put = await fetch(t.upload_url, {method: 'PUT', headers: {'Content-Type': ct}, body: blob.type === ct ? blob : new Blob([blob], {type: ct})});
          if (!put.ok) { var pt = ''; try { pt = (await put.text()).slice(0, 200); } catch (e2) {} throw new Error('S3 PUT ' + put.status + (pt ? ' ' + pt.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim() : '')); }
          return t.public_url;
        }
        if (t && t.error === 'r2_not_configured') r2Disabled = true; else throw new Error((t && (t.message || t.error)) || 'media-upload');
      } catch (e) { if (e && e.status === 503) r2Disabled = true; else { console.error('media-upload', e); throw new Error('آپلود فایل به فضای ابری ناموفق بود: ' + errMsg(e)); } }
    }
    var path = folder + '/' + user.id + '/' + examId + '/' + uuid() + '.' + ext;
    var res = await fetch(SUPABASE_URL + '/storage/v1/object/' + MEDIA_BUCKET + '/' + path, {method: 'POST', headers: {'apikey': ANON, 'Authorization': 'Bearer ' + (session ? session.access_token : ANON), 'Content-Type': contentType, 'x-upsert': 'false'}, body: blob});
    if (!res.ok) { var tx = await res.text(); throw new Error('آپلود فایل ناموفق بود: ' + tx.slice(0, 120)); }
    return SUPABASE_URL + '/storage/v1/object/public/' + MEDIA_BUCKET + '/' + path;
  }
  window.ExamSite = {openFormulaEditor: openFormulaEditor, openHeaderSettings: openHeaderSettings, readPrintHeader: readPrintHeader, faReason: faReason, uploadMedia: uploadMedia, openPrintPreview: openPrintPreview, buildPrintPayload: buildPrintPayload, api: api, demoPrint: demoPrint,
    el: el, esc: esc, fa: fa, en: en, toast: toast, confirmDlg: confirmDlg, promptDlg: promptDlg, mediaBlobUrl: mediaBlobUrl, isOwnStorageUrl: isOwnStorageUrl, rpc: rpc, rpcObj: rpcObj, select: select, http: http, uuid: uuid, fmtScore: fmtScore, fmtDate: fmtDate, money: money, errMsg: errMsg,
    localState: localState, setLocalState: setLocalState, loading: loading, showErr: showErr, emptyBox: emptyBox, qType: qType, engineHtml: engineHtml,
    user: function () { return user; }, session: function () { return session; }, config: {url: SUPABASE_URL, anon: ANON},
    go: function (panel, arg) { view.panel = panel; view.arg = arg; render(); }, view: view, examActions: examActions,
    render: render, renderPage: renderPage, printExam: printExam, logout: doLogout,
    __setSession: function (s) { saveSession(s); }, __setUser: function (u) { user = u; view.page = 'panel'; render(); },
    auth: {keyReady: KEY_READY, login: function (u) { user = u; afterLogin(); }, currentProfile: currentProfile, requireEmail: requireEmail, drawCompletion: drawCompletion, logout: doLogout} /* برای تست خودکار بدون سرور */};
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot); else boot();
})();
