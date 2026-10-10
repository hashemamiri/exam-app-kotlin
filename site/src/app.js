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
    /* V228 — دسترس‌پذیری: دکمه‌های آیکنی با title به‌طور خودکار aria-label می‌گیرند؛ دکمهٔ بستن (✕) برچسب «بستن» */
    if (tag === 'button' && attrs) { if (attrs.title && !attrs['aria-label']) e.setAttribute('aria-label', attrs.title); else if (attrs['class'] === 'x' && !attrs['aria-label']) e.setAttribute('aria-label', 'بستن'); }
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
  /* V209 — همان قاعدهٔ اپ (UserFacingError): هیچ متن فنی (Failed to fetch، HTTP 5xx، JSON خام، آدرس سرور) به کاربر نشان داده نمی‌شود؛
     خطای شبکه → پیام اینترنت؛ متن غیرفارسی/فنی → پیام عمومی سرور؛ فقط پیام‌های فارسی معنادار عیناً می‌مانند. */
  var ERR_NETWORK = 'خطا در ارتباط با سرور؛ از اتصال به اینترنت مطمئن شوید و دوباره تلاش کنید.';
  var ERR_SERVER = 'خطا در سرور؛ از اتصال به اینترنت مطمئن شوید و دوباره تلاش کنید.';
  var ERR_TECH = ['http request', 'failed to fetch', 'networkerror', 'load failed', 'failed with message', 'unable to resolve', 'exception', 'supabase.co', 'supabase.in',
    'timeout', 'timed out', 'connection', 'ssl', 'socket', 'pgrst', 'jwt', 'postgrest', 'status code', 'internal server error', 'bad gateway', 'service unavailable',
    'gateway timeout', 'cloudflare', 'typeerror', 'undefined', 'null', '{', '}', '<html'];
  function errText(raw) {
    var t = String(raw == null ? '' : raw).replace(/https?:\/\/\S+/g, '').trim();
    if (!t) return '';
    var lower = t.toLowerCase();
    if (/failed to fetch|networkerror|network request failed|load failed|unable to resolve|timed? ?out|connection (reset|refused)|err_internet|err_name_not_resolved/.test(lower)) return ERR_NETWORK;
    var hasFa = /[\u0600-\u06FF]/.test(t);
    var tech = !hasFa || ERR_TECH.some(function (k) { return lower.indexOf(k) >= 0; });
    if (!tech) {
      var letters = (t.match(/[A-Za-z\u0600-\u06FF]/g) || []).length, latin = (t.match(/[A-Za-z]/g) || []).length;
      tech = letters > 0 && latin * 100 / letters >= 40;
    }
    if (!tech) return t.slice(0, 260);
    var head = t.split(':')[0].trim();
    if (head && /[\u0600-\u06FF]/.test(head) && !ERR_TECH.some(function (k) { return head.toLowerCase().indexOf(k) >= 0; })) return head.slice(0, 260);
    return ERR_SERVER;
  }
  function errMsg(e) {
    /* V255.1 — متن فنی فقط در Console مرورگر (برای عیب‌یابی)؛ به کاربر همچنان پیام فارسی نشان داده می‌شود. */
    try { if (window.console && e && typeof e !== 'string') console.warn('[onlineexam] خطای فنی:', e && (e.stack || e.message || JSON.stringify(e))); else if (e) console.warn('[onlineexam] خطای فنی:', e); } catch (x) {}
    if (!e) return ERR_SERVER;
    if (typeof e === 'string') return errText(e) || ERR_SERVER;
    var raw = e.message || e.error_description || e.msg || e.error || e.hint || e.details || '';
    if (typeof raw !== 'string') raw = '';
    return errText(raw) || ERR_SERVER;
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
      /* V180 — روی موتور چاپ/فرمول (z-index 60) هم دیده شود؛ پیش از این پنجرهٔ هزینهٔ چاپ پشتِ پیش‌نمایش پنهان می‌ماند */
      var bg = el('div', {class: 'modal-bg' + (document.querySelector('.engine-bg') ? ' over-engine' : '')});
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
  /* V202 — پیام یکسانِ «کسر هزینه» برای همهٔ مسیرهای کسر از کیف پول: وسط صفحه، فقط با دکمهٔ «تأیید» محو می‌شود (نه toast) */
  function infoDlg(title, body, okLabel) {
    return new Promise(function (resolve) {
      var bg = el('div', {class: 'modal-bg' + (document.querySelector('.engine-bg') ? ' over-engine' : '')});
      var m = el('div', {class: 'modal'}, [
        el('h2', {text: title}),
        el('p', {class: 'muted', html: body}),
        el('div', {class: 'row', style: 'justify-content:flex-start;margin-top:14px'}, [
          el('button', {class: 'btn', text: okLabel || 'تأیید', onclick: function () { bg.remove(); resolve(true); }})
        ])
      ]);
      bg.appendChild(m); document.body.appendChild(bg);
    });
  }
  function costDoneDlg(cost, balance, extra) {
    return infoDlg('کسر هزینه انجام شد', 'کسر <b>' + money(cost || 0) + '</b> از کیف پول با موفقیت انجام شد.' + (balance != null ? '<br>موجودی: ' + money(balance) : '') + (extra ? '<br>' + extra : ''));
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
  /* V250 — تازه‌سازی نشست: (۱) درخواست‌های هم‌زمان یک تازه‌سازی مشترک دارند (refresh_token یک‌بارمصرف است)؛
     (۲) قطع اینترنت/خطای سرور دیگر کاربر را خارج نمی‌کند — فقط پاسخ ۴۰۰/۴۰۱/۴۰۳ (توکن نامعتبر) نشست را پاک می‌کند. */
  var refreshing = null;
  function ensureFreshSession() {
    if (!session || !session.refresh_token) return Promise.resolve();
    var exp = Number(session.expires_at || 0) * 1000;
    if (exp && exp - Date.now() > 60 * 1000) return Promise.resolve();
    if (refreshing) return refreshing;
    refreshing = (async function () {
      try {
        var res = await fetch(SUPABASE_URL + '/auth/v1/token?grant_type=refresh_token', {method: 'POST', headers: {'apikey': ANON, 'Content-Type': 'application/json'}, body: JSON.stringify({refresh_token: session.refresh_token})});
        var data = null; try { data = await res.json(); } catch (e) { data = null; }
        if (res.ok && data && data.access_token) saveSession(data);
        else if (res.status === 400 || res.status === 401 || res.status === 403) saveSession(null);
      } catch (e) { /* شبکه: نشست می‌ماند؛ درخواست بعدی دوباره تلاش می‌کند */ }
      finally { refreshing = null; }
    })();
    return refreshing;
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
    /* V161 — مثل SupabaseProfileRepository.verifyCurrentPassword / changeEmail */
    verifyCurrentPassword: async function (email, pw) { if (!email) throw new Error('نشست ورود پیدا نشد.'); await http('/auth/v1/token?grant_type=password', {method: 'POST', auth: false, body: {email: email, password: pw}}); },
    updateEmail: function (email) { return authApi.updateUser({email: email}); },
    /* V202.3 — همان منبع اپ (جدول عمومی app_version، بدون نیاز به نشست): آخرین نسخهٔ فعال با نشانی مستقیم APK */
    latestApk: async function () {
      var rows = await http('/rest/v1/app_version?select=version_code,version_name,apk_url,apk_size_bytes&is_active=eq.true&order=version_code.desc&limit=1', {auth: false});
      var r = Array.isArray(rows) && rows[0]; if (!r || !/^https:\/\//i.test(String(r.apk_url || ''))) return null;
      return {code: r.version_code, name: r.version_name, url: String(r.apk_url).trim(), size: Number(r.apk_size_bytes) || 0};
    },
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
    /* V202.1 — v2: پرداخت چاپ آزمون آنلاین/محلی روی سرور ثبت می‌شود؛ همان محتوا در اپ/سایت دسکتاپ/سایت گوشی دوباره کسر نمی‌شود */
    chargePrint: function (examId, count, mode, content) { return rpcObj('native_charge_print_v2', {p_exam: examId, p_operation: uuid(), p_questions: count, p_mode: mode, p_content: content || ''}); },
    printChargeQuote: function (examId, count, content) { return rpcObj('native_charge_print_quote_v2', {p_exam: examId, p_questions: count, p_content: content || ''}); },
    /* V199 — پرداخت چاپ آزمون چاپی: برآورد / کسر / وضعیت کارت‌ها (سربرگ = اثر انگشت فیلدهای f_*) */
    printQuote: function (id, header) { return rpcObj('native_print_quote_v199', {p_id: id, p_header: header || ''}); },
    printPay: function (id, header) { return rpcObj('native_print_pay_v199', {p_id: id, p_operation: uuid(), p_header: header || ''}); },
    printPayStatus: function (header) { return rpcObj('native_print_pay_status_v199', {p_header: header || ''}); },
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
    managerTeachers: async function () { var d = await rpcObj('native_manager_teachers_v37', {}); return d.items || []; },
    managerClassStats: function () { return rpcObj('native_manager_class_stats_v1', {}); } /* V230 */
  };
  /* V230 — «ذخیرهٔ PDF» بدون پاپ‌آپ: نسخه‌ای از بخش داخل #print-root می‌رود، بقیهٔ صفحه با CSS چاپ پنهان می‌شود و پنجرهٔ چاپ مرورگر (گزینهٔ «ذخیره به‌صورت PDF») باز می‌شود */
  function printSection(title, node) {
    var old = document.getElementById('print-root'); if (old) old.remove();
    var root = el('div', {id: 'print-root'}, [el('h1', {text: title}), el('div', {class: 'muted', style: 'font-size:12px;margin-bottom:10px', text: 'سامانهٔ آزمون آنلاین · ' + jalaliDisplay(new Date().toISOString())}), node.cloneNode(true)]);
    root.querySelectorAll('button,input[type=search],.no-print').forEach(function (b) { b.remove(); });
    document.body.appendChild(root); document.body.classList.add('printing');
    var done = function () { document.body.classList.remove('printing'); root.remove(); window.removeEventListener('afterprint', done); };
    window.addEventListener('afterprint', done);
    setTimeout(function () { try { window.print(); } catch (e) { done(); toast('چاپ در این مرورگر ممکن نشد.', 'err'); } }, 60);
    setTimeout(function () { if (document.body.classList.contains('printing')) done(); }, 60000);
  }
  function requireEmail(v) { var c = String(v || '').trim().toLowerCase(); if (!EMAIL_RE.test(c)) throw new Error('ایمیل معتبر وارد کنید.'); return c; }
  function cleanCode(c) { var d = en(c).replace(/\D/g, ''); if (d.length < 6 || d.length > 8) throw new Error('کد یک‌بارمصرف باید ۶ تا ۸ رقم باشد.'); return d; }
  function validatePassword(v) { if (!v || v.length < 8 || v.length > 72) throw new Error('رمز عبور باید ۸ تا ۷۲ کاراکتر باشد.'); }

  /* ================================================================ موتور چاپ (assets/print) و ویرایشگر فرمول — تعبیه‌شده */
  /* V162 — موتورها در فایل جداگانهٔ engines.<hash>.js (کش دائمی) هستند و فقط هنگام اولین نیاز بارگذاری می‌شوند. */
  var enginesReady = null;
  function loadEngines() {
    if (window.__ENGINES) return Promise.resolve(window.__ENGINES);
    if (enginesReady) return enginesReady;
    enginesReady = new Promise(function (resolve, reject) {
      var url = window.__ENGINES_URL; if (!url) { reject(new Error('نشانی موتورها در این فایل نیست.')); return; }
      var sc = document.createElement('script'); sc.src = url; sc.async = true;
      sc.onload = function () { if (window.__ENGINES) resolve(window.__ENGINES); else reject(new Error('موتورها بارگذاری نشد.')); };
      sc.onerror = function () { enginesReady = null; reject(new Error('دانلود موتور چاپ/فرمول ناموفق بود؛ اینترنت را بررسی کنید.')); };
      document.head.appendChild(sc);
    });
    return enginesReady;
  }
  function engineHtml(kind) {
    return loadEngines().then(function (E) {
      var src = E[kind] || '';
      if (!src) toast('موتور ' + (kind === 'print' ? 'چاپ' : 'فرمول') + ' در این فایل تعبیه نشده است.', 'err');
      return src;
    }, function (e) { toast(errMsg(e), 'err'); return ''; });
  }
  /* پیش‌بارگذاری آرام بعد از ورود (پس از بیکار شدن صفحه) تا اولین چاپ/فرمول معطل نشود */
  function prefetchEngines() { var idle = window.requestIdleCallback || function (f) { setTimeout(f, 2500); }; idle(function () { loadEngines().then(function () { prewarmFormula(); /* V216 */ }).catch(function () {}); }); }
  /* V199 — اثر انگشت سربرگ: فیلدهای f_* پیش‌نمایش، مرتب، بدون خالی‌ها؛ عیناً مثل PrintPayFingerprint.kt اپ (سرور md5 می‌کند) */
  function headerFingerprint(fields) {
    fields = fields && typeof fields === 'object' ? fields : {};
    /* f_course/f_duration از خود آزمون چاپی می‌آیند؛ سرور درس/مدت را خودش به هش می‌افزاید (native_print_header_hash_v199) */
    return Object.keys(fields).filter(function (k) { return k.indexOf('f_') === 0 && k !== 'f_course' && k !== 'f_duration' && String(fields[k] == null ? '' : fields[k]).trim() !== ''; }).sort().map(function (k) { return k + '=' + String(fields[k]).trim(); }).join('\n');
  }
  /* اثر انگشت سربرگ فعلی (همان فیلدهای buildPrintPayload بدون سؤال‌ها؛ برای همهٔ آزمون‌های چاپی یکی است) */
  function printHeaderFp() { return headerFingerprint(buildPrintPayload({title: '', subject: '', duration: 0, questions: []}).fields); }
  function printDueText(q) {
    var parts = [];
    if (q.header_changed) parts.push('سربرگ تغییر کرده → کل آزمون دوباره محاسبه می‌شود');
    else if (q.never_paid) parts.push('این آزمون هنوز پرداخت نشده است');
    parts.push('سؤال: ' + fa(q.questions_due || 0) + ' × ' + fa(1000) + ' تومان');
    parts.push('تصویر: ' + fa(q.images_due || 0) + ' × ' + fa(1000) + ' تومان');
    parts.push('<b>مبلغ قابل کسر از کیف پول: ' + money(q.due || 0) + '</b>');
    return parts.join('<br>');
  }
  /* برآورد → (در صورت بدهی) تأیید → کسر. resolve(true) یعنی پرداخت‌شده/چاپ مجاز؛ false یعنی انصراف یا خطا (پیام داده شده) */
  async function ensurePrintPaid(printId, headerFp, title) {
    var q = await api.printQuote(printId, headerFp);
    if (q && q.error) throw new Error(String(q.error));
    if (!q.due) return {paid: true, cost: 0};
    if (!(await confirmDlg('پرداخت هزینهٔ چاپ' + (title ? ' — ' + esc(title) : ''), printDueText(q), 'پرداخت'))) return {paid: false};
    var r = await api.printPay(printId, headerFp);
    if (r && r.error) { var m = String(r.error); if (r.balance != null && r.required != null) m += '؛ موجودی ' + money(r.balance) + ' و مبلغ لازم ' + money(r.required) + ' است.'; throw new Error(m); }
    await costDoneDlg(r.cost || q.due, r.balance);
    return {paid: true, cost: r.cost || q.due};
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
      var ctx = printCtx, w = ctx.iframe.contentWindow;
      var doNative = function () {
        ctx.busy = true;
        /* V180.1 — فقط چاپ بومی مرورگر؛ w.print() موتور دوباره به همین پل برمی‌گشت (حلقهٔ پنجرهٔ هزینه) */
        /* V180.4 — __nativePrint در سرآیند موتور (build_site.py) پیش از بازنویسی webhost.js ذخیره شده؛ Window.prototype.print در Chrome وجود ندارد */
        try { w.focus(); if (typeof w.__nativePrint === 'function') w.__nativePrint.call(w); else if (w.Window && typeof w.Window.prototype.print === 'function') w.Window.prototype.print.call(w); else throw new Error('چاپ بومی مرورگر در دسترس نیست'); } catch (e) { console.warn('print', e); toast('پنجرهٔ چاپ مرورگر باز نشد.', 'err'); }
        setTimeout(function () {
          ctx.busy = false;
          /* V180.2 — مثل ExamHtmlPrintDialog: چاپ مستقیم (printMode از منوی چاپ بیلدر) پس از پنجرهٔ چاپ بسته می‌شود؛
             وگرنه برگه به پیش‌نمایش برمی‌گردد و اگر بینندهٔ PGS باز نبود دوباره باز می‌شود (پیش‌تر صفحهٔ خاکستری خالی می‌ماند) */
          if (ctx.direct) { if (printCtx === ctx) closePrintOverlay(); return; }
          try { w.ExamPrintRenderer.restorePreview(); } catch (e) {}
          try { if (!(w.isPreviewOpen && w.isPreviewOpen())) w.ExamPrintRenderer.showPreview(); } catch (e) {}
        }, 400);
      };
      var restore = function () { ctx.busy = false; try { w.ExamPrintRenderer.restorePreview(); } catch (e) {} };
      /* V180.1 — یک درخواست در جریان (پنجرهٔ هزینه باز / چاپ بومی) → درخواست دوباره نادیده گرفته می‌شود */
      if (ctx.busy) return;
      /* V180 — مثل PrintCostConfirmDialog اپ: هر چاپی (آنلاین، چاپی، محلی) اول پنجرهٔ هزینه؛ فقط مهمانِ صفحهٔ نمونه بدون کسر */
      if (!user) { doNative(); return; }
      /* V180.1 — مثل printPrepaid اپ: هزینهٔ این نسخه در همین پنجرهٔ پیش‌نمایش یک‌بار پرداخت شده → چاپ دوباره بدون کسر */
      ctx.paid = ctx.paid || {};
      if (ctx.paid[mode]) { doNative(); return; }
      /* V199 — آزمون چاپی: هزینه به‌ازای سؤال/تصویر روی سرور (print_exam_payments)؛ یک پرداخت برای هر دو نسخه؛ تا پرداخت نشده چاپ نمی‌شود */
      if (ctx.printExam !== undefined) {
        if (!ctx.printExam) { toast('برای چاپ، ابتدا آزمون چاپی را ذخیره کنید.', 'err'); restore(); return; }
        if (ctx.printDirty) { toast('تغییرات ذخیره نشده است؛ ابتدا آزمون چاپی را ذخیره کنید.', 'err'); restore(); return; }
        ctx.busy = true;
        ensurePrintPaid(ctx.printExam, headerFingerprint(ctx.payload && ctx.payload.fields), ctx.title).then(function (r) {
          if (!r.paid) { restore(); return; }
          ctx.paid.student = ctx.paid.teacher = true; setPreviewPaid(ctx, true);
          if (printCtx === ctx) doNative(); else ctx.busy = false;
        }).catch(function (e) { restore(); toast(errMsg(e), 'err'); });
        return;
      }
      ctx.busy = true;
      var examRef = ctx.examId || 'local';
      /* V132 — هزینهٔ چاپ: ۱۰۰۰ تومان به‌ازای هر سؤال، تأیید پیش از پنجرهٔ چاپ */
      var n = ctx.questionCount || 0, cost = n * PRINT_COST_PER_Q, content = printContentKey(ctx.payload);
      /* V202.1 — اول برآورد سرور: اگر همین محتوا قبلاً (اپ/سایت) پرداخت شده، بدون کسر دوباره چاپ می‌شود */
      api.printChargeQuote(examRef, n, content).then(function (q) {
        if (q && q.paid) { ctx.paid.student = ctx.paid.teacher = true; setPreviewPaid(ctx, true); if (printCtx === ctx) doNative(); else ctx.busy = false; return; }
        return askAndCharge();
      }).catch(function () { return askAndCharge(); });
      function askAndCharge() {
      confirmDlg(mode === 'teacher' ? 'چاپ با کلید (پاسخ‌نامه)' : 'چاپ آزمون', 'هزینهٔ چاپ: ' + fa(PRINT_COST_PER_Q) + ' تومان به‌ازای هر سؤال<br>تعداد سؤال: ' + fa(n) + '<br><b>مبلغ قابل کسر از کیف پول: ' + money(cost) + '</b>', 'پرداخت و چاپ').then(function (ok) {
        if (!ok) { restore(); return; }
        api.chargePrint(examRef, n, mode, content).then(function (r) {
          ctx.paid.student = ctx.paid.teacher = true; setPreviewPaid(ctx, true);
          if (r && r.already_paid) { if (printCtx === ctx) doNative(); else ctx.busy = false; return; }
          /* V202 — پیام کسر وسط صفحه؛ چاپ پس از «تأیید» */
          return costDoneDlg(r.cost || cost, r.balance).then(function () { if (printCtx === ctx) doNative(); else ctx.busy = false; });
        }).catch(function (e) { restore(); toast(errMsg(e), 'err'); });
      });
      }
    }
  };
  /* V202.1 — کلید محتوا برای هش پیش‌نویس محلی (اپ: printContentKey در BillingRepository.kt — متن سؤال‌ها با خط جدید) */
  function printContentKey(payload) { return ((payload && payload.questions) || []).map(function (q) { return String((q && q.text) || '').trim(); }).join('\n'); }
  /* V199 — رنگ دکمهٔ چاپ پیش‌نمایش: قرمز تا پرداخت نشده، سبز پس از پرداخت (webhost.js: setPrintPaid) */
  function setPreviewPaid(ctx, paid) { try { var w = ctx.iframe.contentWindow; if (w && typeof w.setPrintPaid === 'function') w.setPrintPaid(!!paid); } catch (e) {} }
  function refreshPreviewPaid(ctx) {
    if (!ctx.printExam && !ctx.direct) {
      /* V202.1 — آزمون آنلاین/محلی: اگر همین محتوا قبلاً پرداخت شده، دکمهٔ چاپ از ابتدا سبز */
      setPreviewPaid(ctx, false);
      api.printChargeQuote(ctx.examId || 'local', ctx.questionCount || 0, printContentKey(ctx.payload)).then(function (q) { if (printCtx !== ctx) return; if (q && q.paid) { ctx.paid = {student: true, teacher: true}; setPreviewPaid(ctx, true); } }).catch(function () {});
      return;
    }
    if (!ctx.printExam || ctx.printDirty) { setPreviewPaid(ctx, false); return; }
    api.printQuote(ctx.printExam, headerFingerprint(ctx.payload && ctx.payload.fields)).then(function (q) { if (printCtx !== ctx) return; var paid = !!(q && !q.error && !q.due); if (paid) ctx.paid = {student: true, teacher: true}; setPreviewPaid(ctx, paid); }).catch(function () {});
  }
  function closePrintOverlay() {
    if (!printCtx) return;
    var c = printCtx; printCtx = null;
    if (c.onSnapshot) { try { var w = c.iframe.contentWindow; var snap = w.ExamPrintRenderer && w.ExamPrintRenderer.layoutSnapshot ? w.ExamPrintRenderer.layoutSnapshot() : '{}'; if (snap && snap !== '{}') c.onSnapshot(snap); } catch (e) {} }
    try { c.overlay.remove(); } catch (e) {}
    document.body.style.overflow = ''; document.body.classList.remove('engine-open');
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
        jobs.push(mediaBlobUrl(it[1].src).then(function (u) { return new Promise(function (res) { var im = new Image(); if (/^https?:/i.test(u)) { im.crossOrigin = 'anonymous'; u = u + (u.indexOf('?') >= 0 ? '&' : '?') + 'cors=' + Date.now(); } im.onload = function () { res(im); }; im.onerror = function () { res(null); }; im.src = u; }); }).then(function (im) {
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
    document.body.style.overflow = 'hidden'; document.body.classList.add('engine-open'); /* V179 */
    printCtx = {overlay: overlay, iframe: iframe, direct: opts.printMode === 'student' || opts.printMode === 'teacher', examId: opts.examId || '', questionCount: (payload.questions || []).length, onClosed: opts.onClosed, onSnapshot: opts.onSnapshot,
      payload: payload, title: opts.title || '', printExam: opts.printExam, printDirty: !!opts.printDirty}; /* V199 */
    try { var ps = localStorage.getItem(LS_PAGESETUP); if (ps && payload.pageSetup === undefined) payload.pageSetup = JSON.parse(ps); } catch (e) {}
    var inlined = inlinePrintImages(payload);
    var loadedOnce = false;
    iframe.addEventListener('load', function () {
      /* V180.1 — اگر مرورگر iframe را دوباره بارگذاری کند (مثلاً پس از پنجرهٔ چاپ)، داده دوباره تزریق می‌شود ولی چاپِ خودکار (printMode) تکرار نمی‌شود */
      var again = loadedOnce; loadedOnce = true;
      var w = iframe.contentWindow, tries = 0;
      (function push() {
        tries++;
        try {
          if (typeof w.setExamData === 'function' && w.renderPreview && w.renderPreview.__pgs && inlined.done) {
            w.setExamData(payload);
            if (printCtx && printCtx.iframe === iframe) { setPreviewPaid(printCtx, false); refreshPreviewPaid(printCtx); } /* V199 / V202.1 */
            /* V156 — مثل ExamHtmlPrintDialog: printMode=student/teacher یعنی بدون توقف در پیش‌نمایش، مستقیم چاپ */
            setTimeout(function () { try { if (!again && opts.printMode === 'teacher' && typeof w.printTeacher === 'function') w.printTeacher(); else if (!again && opts.printMode === 'student' && typeof w.printStudent === 'function') w.printStudent(); else w.ExamPrintRenderer.showPreview(); } catch (e) { console.warn(e); } }, 120);
            return;
          }
        } catch (e) {}
        if (tries < 200 || (!inlined.done && tries < 1200)) setTimeout(push, 50); else toast(inlined.done ? 'موتور چاپ آماده نشد.' : 'بارگذاری تصویرهای آزمون طول کشید.', 'err');
      })();
    });
    engineHtml('print').then(function (h) { if (h) iframe.srcdoc = h; else closePrintOverlay(); });
  }
  /* ---- ویرایشگر فرمول: پل ExamEditorNative (FormulaHostDialog.kt) ---- */
  var formulaCtx = null;
  window.__formulaBridge = {
    onTextChanged: function (v) { if (formulaCtx) formulaCtx.text = String(v == null ? '' : v); },
    onEditorClosed: function () { if (!formulaCtx) return; var c = formulaCtx; formulaCtx = null; try { c.overlay.remove(); } catch (e) {} document.body.style.overflow = ''; document.body.classList.remove('engine-open'); c.resolve(c.text); prewarmFormula(); /* V216 */ },
    onError: function (code) { console.warn('formula editor:', code); }
  };
  /* V216 — ویرایشگر فرمول «گرم»: فایل ۱٫۸MB ویرایشگر یک بار در iframe پنهان parse می‌شود و هر باز شدن
     فقط overlay را نشان می‌دهد و begin را صدا می‌زند؛ پس از بستن، iframe بعدی در بیکاری آماده می‌شود. */
  var warmFormula = null;
  function makeFormulaOverlay() {
    var overlay = el('div', {class: 'engine-bg'}); overlay.style.display = 'none';
    var iframe = el('iframe', {title: 'formula-editor'});
    overlay.appendChild(iframe); document.body.appendChild(overlay);
    var w = {overlay: overlay, iframe: iframe, loaded: false, failed: false};
    iframe.addEventListener('load', function () { w.loaded = true; });
    engineHtml('formula').then(function (h) { if (h) iframe.srcdoc = h; else w.failed = true; });
    return w;
  }
  function prewarmFormula() {
    if (warmFormula || formulaCtx || !user || user.role === 'student') return;
    var idle = window.requestIdleCallback || function (f) { setTimeout(f, 1500); };
    idle(function () { if (!warmFormula && !formulaCtx) warmFormula = makeFormulaOverlay(); });
  }
  function openFormulaEditor(text, selStart, selEnd) {
    return new Promise(function (resolve) {
      if (formulaCtx) { try { formulaCtx.overlay.remove(); } catch (e) {} }
      var w = warmFormula && !warmFormula.failed ? warmFormula : makeFormulaOverlay();
      warmFormula = null;
      var overlay = w.overlay, iframe = w.iframe;
      overlay.style.display = '';
      document.body.style.overflow = 'hidden'; document.body.classList.add('engine-open');
      formulaCtx = {overlay: overlay, iframe: iframe, text: text || '', resolve: resolve};
      var s = selStart == null ? (text || '').length : selStart, e = selEnd == null ? s : selEnd;
      var tries = 0;
      (function tryBegin() {
        tries++;
        if (formulaCtx && formulaCtx.iframe !== iframe) return;
        try { var cw = iframe.contentWindow; if (w.loaded && cw && cw.ExamFormulaHost && typeof cw.ExamFormulaHost.begin === 'function') { cw.ExamFormulaHost.begin(text || '', s, e); return; } } catch (er) {}
        if (w.failed || tries > 134) { toast('ویرایشگر فرمول آماده نشد.', 'err'); window.__formulaBridge.onEditorClosed(); return; }
        setTimeout(tryBegin, w.loaded ? 150 : 75);
      })();
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
  window.SiteJalali = J;
  /* V196 — انتخابگر تاریخ/ساعت شمسی مشترک (معادل JalaliDateTimeDialog اپ): ماه‌گردی، شبکهٔ روزها با حداقل تاریخ، ساعت/دقیقه، «اکنون»، پاک‌کردن.
     mode: 'datetime' → Date | 'date' → {jy,jm,jd} | 'time' → {h,m}.  min: Date (فقط datetime/date). resolve(null) = انصراف؛ resolve('') = پاک‌کردن */
  function jalaliPicker(o) {
    o = o || {}; var mode = o.mode || 'datetime';
    return new Promise(function (resolve) {
      var now = new Date(), init = o.value instanceof Date && !isNaN(o.value) ? o.value : null;
      var base = init || now, jt = J.fromGregorian(base), vy = jt.jy, vm = jt.jm;
      var selDay = init ? {jy: jt.jy, jm: jt.jm, jd: jt.jd} : (o.selected || null);
      var hh = init ? base.getHours() : (o.hour != null ? o.hour : now.getHours()), mm = init ? base.getMinutes() : (o.minute != null ? o.minute : 0);
      var minD = o.min instanceof Date && !isNaN(o.min) ? o.min : null, minJ = minD ? J.fromGregorian(minD) : null;
      var bg = el('div', {class: 'modal-bg jdp-bg', onclick: function (e) { if (e.target === bg) done(null); }});
      var box = el('div', {class: 'modal jdp'}), err = el('div', {class: 'jdp-err'});
      function done(v) { bg.remove(); resolve(v); }
      function cmpDay(a, b) { return (a.jy - b.jy) || (a.jm - b.jm) || (a.jd - b.jd); }
      function dayDisabled(d) { return minJ && cmpDay({jy: vy, jm: vm, jd: d}, minJ) < 0; }
      var head = el('div', {class: 'jdp-head'}), grid = el('div', {class: 'jdp-grid'});
      function draw() {
        head.innerHTML = ''; grid.innerHTML = '';
        head.appendChild(el('button', {type: 'button', class: 'jdp-nav', 'aria-label': 'ماه قبل', text: '›', onclick: function () { vm--; if (vm < 1) { vm = 12; vy--; } draw(); }}));
        head.appendChild(el('div', {class: 'jdp-title', text: J.MONTHS[vm - 1] + ' ' + fa(vy)}));
        head.appendChild(el('button', {type: 'button', class: 'jdp-nav', 'aria-label': 'ماه بعد', text: '‹', onclick: function () { vm++; if (vm > 12) { vm = 1; vy++; } draw(); }}));
        J.DAYS.forEach(function (d) { grid.appendChild(el('div', {class: 'jdp-h', text: d})); });
        var first = J.toGregorian(vy, vm, 1), off = (first.getUTCDay() + 1) % 7, len = J.monthLength(vy, vm), today = J.fromGregorian(new Date());
        for (var i = 0; i < off; i++) grid.appendChild(el('div', {class: 'jdp-d empty'}));
        for (var d = 1; d <= len; d++) (function (d) {
          var cls = 'jdp-d' + (selDay && selDay.jy === vy && selDay.jm === vm && selDay.jd === d ? ' sel' : '') + (today.jy === vy && today.jm === vm && today.jd === d ? ' today' : '') + (((off + d - 1) % 7) === 6 ? ' fri' : '') + (dayDisabled(d) ? ' dis' : '');
          grid.appendChild(el('button', {type: 'button', class: cls, text: fa(d), disabled: dayDisabled(d) ? 'disabled' : null, onclick: function () { selDay = {jy: vy, jm: vm, jd: d}; err.textContent = ''; draw(); }}));
        })(d);
      }
      var hIn = el('input', {type: 'text', inputmode: 'numeric', class: 'jdp-t', value: fa(String(hh).padStart(2, '0')), 'aria-label': 'ساعت'});
      var mIn = el('input', {type: 'text', inputmode: 'numeric', class: 'jdp-t', value: fa(String(mm).padStart(2, '0')), 'aria-label': 'دقیقه'});
      [hIn, mIn].forEach(function (x) { x.addEventListener('focus', function () { x.select(); }); x.addEventListener('input', function () { x.value = fa(en(x.value).replace(/\D/g, '').slice(0, 2)); }); });
      function readTime() { var h = parseInt(en(hIn.value), 10), m = parseInt(en(mIn.value), 10); if (isNaN(h) || h < 0 || h > 23 || isNaN(m) || m < 0 || m > 59) return null; return {h: h, m: m}; }
      function confirm() {
        if (mode === 'time') { var t0 = readTime(); if (!t0) { err.textContent = 'ساعت باید ۰ تا ۲۳ و دقیقه ۰ تا ۵۹ باشد.'; return; } return done(t0); }
        if (!selDay) { err.textContent = 'یک روز را انتخاب کنید.'; return; }
        if (mode === 'date') return done(selDay);
        var t = readTime(); if (!t) { err.textContent = 'ساعت باید ۰ تا ۲۳ و دقیقه ۰ تا ۵۹ باشد.'; return; }
        var g = J.toGregorian(selDay.jy, selDay.jm, selDay.jd), dt = new Date(g.getUTCFullYear(), g.getUTCMonth(), g.getUTCDate(), t.h, t.m, 0, 0);
        if (minD && dt.getTime() < minD.getTime()) { err.textContent = 'زمان پایان نمی‌تواند قبل از زمان شروع باشد (' + jalaliDisplay(minD) + ').'; return; }
        done(dt);
      }
      box.appendChild(el('div', {class: 'row', style: 'margin-bottom:6px'}, [el('h2', {class: 'grow', text: o.title || 'انتخاب تاریخ', style: 'margin:0;font-size:17px'}), el('button', {class: 'x', text: '✕', 'aria-label': 'بستن', onclick: function () { done(null); }})]));
      if (mode !== 'time') { box.appendChild(head); box.appendChild(grid); draw(); }
      if (mode !== 'date') box.appendChild(el('div', {class: 'jdp-time'}, [el('label', {text: 'ساعت'}), hIn, el('span', {class: 'jdp-colon', text: ':'}), mIn, el('label', {text: 'دقیقه'})]));
      if (minD && mode !== 'time') box.appendChild(el('div', {class: 'muted', style: 'font-size:12px', text: 'زودتر از ' + jalaliDisplay(minD) + ' قابل انتخاب نیست.'}));
      box.appendChild(err);
      var acts = el('div', {class: 'jdp-acts'});
      acts.appendChild(el('button', {type: 'button', class: 'btn', text: 'تأیید', onclick: confirm}));
      acts.appendChild(el('button', {type: 'button', class: 'btn light', text: 'اکنون', onclick: function () { var n = new Date(), jn = J.fromGregorian(n); selDay = {jy: jn.jy, jm: jn.jm, jd: jn.jd}; vy = jn.jy; vm = jn.jm; hIn.value = fa(String(n.getHours()).padStart(2, '0')); mIn.value = fa(String(n.getMinutes()).padStart(2, '0')); err.textContent = ''; if (mode !== 'time') draw(); }}));
      if (o.canClear) acts.appendChild(el('button', {type: 'button', class: 'btn light danger-text', text: 'پاک‌کردن', onclick: function () { done(''); }}));
      acts.appendChild(el('span', {class: 'grow'}));
      acts.appendChild(el('button', {type: 'button', class: 'btn light', text: 'انصراف', onclick: function () { done(null); }}));
      box.appendChild(acts);
      bg.appendChild(box); document.body.appendChild(bg);
    });
  }
  function jalaliDisplay(d, withTime) {
    if (!(d instanceof Date) || isNaN(d)) return '';
    var j = J.fromGregorian(d), s = fa(j.jy) + '/' + fa(String(j.jm).padStart(2, '0')) + '/' + fa(String(j.jd).padStart(2, '0'));
    if (withTime === false) return s;
    return s + ' ' + fa(String(d.getHours()).padStart(2, '0')) + ':' + fa(String(d.getMinutes()).padStart(2, '0'));
  }
  window.SiteJalali.picker = jalaliPicker; window.SiteJalali.display = jalaliDisplay;
  window.SiteJalali.WEEKDAYS = ['شنبه', 'یکشنبه', 'دوشنبه', 'سه‌شنبه', 'چهارشنبه', 'پنجشنبه', 'جمعه'];
  /* V173 — فرم سربرگ مشترک: پنجرهٔ بازشو (openHeaderSettings) و ستون راست سازندهٔ چاپی دسکتاپ (headerSettingsForm؛ ذخیرهٔ خودکار) */
  function headerSettingsForm(box, opts) {
    opts = opts || {};
    var schema = window.__HEADER_SCHEMA;
    if (!schema || !Array.isArray(schema.templates) || !schema.templates.length) { box.appendChild(el('p', {class: 'muted', text: 'قالب‌های سربرگ خوانده نشد. لطفاً دوباره تلاش کنید.'})); return null; }
    var cur = readPrintHeader(), values = Object.assign({}, cur);
    var tplId = schema.templates.some(function (t) { return t.id === cur.f_headerTemplate; }) ? cur.f_headerTemplate : 'classic'; /* V208 — «ایجاد سربرگ» اول فهرست است ولی پیش‌فرضِ بدون ذخیره همان classic می‌ماند */
    var list = el('div', {class: 'hdr-fields'});
    var sel = el('select', {class: 'hdr-tpl'});
    schema.templates.forEach(function (t) { sel.appendChild(el('option', {value: t.id, text: t.label})); });
    sel.value = tplId;
    function payload() { var t = schema.templates.filter(function (x) { return x.id === tplId; })[0]; var p = {f_headerTemplate: tplId}; ((t && t.fields) || []).forEach(function (f) { p[f.id] = values[f.id] || ''; }); return p; }
    function persist() { try { localStorage.setItem(LS_PRINTHEADER, JSON.stringify(payload())); } catch (e) {} }
    function draw() {
      var t = schema.templates.filter(function (x) { return x.id === tplId; })[0] || schema.templates[0];
      list.innerHTML = '';
      /* V204 — فیلد شرطی (showIf: فقط وقتی مقدار فیلد کنترل‌کننده در فهرست باشد؛ همان HeaderField.isVisible اپ) */
      function visible(f) { return !f.showIf || (f.showIf['in'] || []).indexOf(values[f.showIf.field] || '') >= 0; }
      var hasCond = (t.fields || []).some(function (f) { return !!f.showIf; });
      /* V204.4 — فیلدهای پشت‌سرهم با group یکسان (ردیف‌های سربرگ ۸) کنار هم در یک ردیف؛ جای فیلد پنهان خالی می‌ماند */
      var groups = []; (t.fields || []).forEach(function (f) { var last = groups[groups.length - 1]; if (f.group && last && last[0].group === f.group) last.push(f); else groups.push([f]); });
      var target = list;
      groups.forEach(function (g) {
        if (!g.some(visible)) return;
        if (g.length === 1) { if (visible(g[0])) drawField(g[0], list); return; }
        /* V204.5 — ستون‌ها کنار هم؛ ردیف‌های هر ستون زیر هم (col/colLabel)؛ جای خانهٔ پنهان (لوگو) فیلد غیرفعال «لوگو» */
        var cols = []; g.forEach(function (f) { var key = f.col || f.id; var c = cols.filter(function (x) { return x.key === key; })[0]; if (!c) { c = {key: key, label: f.colLabel || '', fields: []}; cols.push(c); } c.fields.push(f); });
        /* V204.6 — بلوک‌های ستون زیر هم (نه کنار هم) */
        var wrap = el('div', {class: 'hdr-group', style: 'grid-column:1/-1;display:grid;grid-template-columns:1fr;gap:12px'});
        cols.forEach(function (c) {
          var colBox = el('div', {style: 'display:grid;gap:8px'});
          if (c.label) colBox.appendChild(el('div', {style: 'font-weight:700;font-size:13px', text: c.label}));
          c.fields.forEach(function (f) { if (visible(f)) drawField(f, colBox); else colBox.appendChild(el('div', {class: 'field'}, [el('label', {text: f.label}), el('input', {type: 'text', value: 'لوگو', disabled: 'disabled'})])); });
          wrap.appendChild(colBox);
        });
        list.appendChild(wrap);
      });
      function drawField(f, list) {
        var input;
        if (f.kind === 'select') { input = el('select'); if (!(f.options || []).some(function (o) { return o.v === ''; })) input.appendChild(el('option', {value: '', text: '—'})); (f.options || []).forEach(function (o) { input.appendChild(el('option', {value: o.v, text: o.t})); }); input.value = values[f.id] || ''; if (hasCond) input.addEventListener('change', function () { values[f.id] = input.value; if (opts.autosave) persist(); draw(); }); }
        else if (f.kind === 'image') {
          /* V204 — لوگوی دلخواه سربرگ ۸: فایل محلی → کوچک‌سازی تا ۳۲۰px → data-URL PNG (فقط در مرورگر ذخیره می‌شود، آپلود نمی‌شود) */
          var file = el('input', {type: 'file', accept: 'image/*', style: 'display:none'});
          var img = el('img', {style: 'max-height:56px;max-width:140px;object-fit:contain;border:1px solid var(--line);border-radius:8px;padding:2px;background:#fff' + (values[f.id] ? '' : ';display:none')}); if (values[f.id]) img.src = values[f.id];
          var pick = el('button', {type: 'button', class: 'btn light sm', text: values[f.id] ? 'تغییر' : 'انتخاب تصویر', onclick: function () { file.click(); }});
          var rm = el('button', {type: 'button', class: 'btn light sm', text: 'حذف', style: values[f.id] ? '' : 'display:none', onclick: function () { values[f.id] = ''; if (opts.autosave) persist(); draw(); }});
          file.addEventListener('change', function () {
            var fl = file.files && file.files[0]; if (!fl) return;
            var rd = new FileReader();
            rd.onload = function () {
              var im = new Image();
              im.onload = function () { var m = 320, sc = Math.min(1, m / Math.max(im.width, im.height, 1)); var cv = document.createElement('canvas'); cv.width = Math.max(1, Math.round(im.width * sc)); cv.height = Math.max(1, Math.round(im.height * sc)); cv.getContext('2d').drawImage(im, 0, 0, cv.width, cv.height); values[f.id] = cv.toDataURL('image/png'); if (opts.autosave) persist(); draw(); };
              im.onerror = function () { toast('فایل تصویر خوانده نشد.', 'err'); };
              im.src = String(rd.result);
            };
            rd.readAsDataURL(fl);
          });
          input = el('div', {class: 'row', style: 'gap:8px;align-items:center'}, [img, pick, rm, file]);
          list.appendChild(el('div', {class: 'field full'}, [el('label', {text: f.label}), input]));
          return;
        }
        else if (f.kind === 'textarea') input = el('textarea', {rows: String(f.rows || 3), text: values[f.id] || ''});
        else input = el('input', {type: 'text', value: values[f.id] || '', placeholder: f.placeholder || ''});
        /* V196 — مثل اپ: کلیک روی تاریخ‌ها تقویم شمسی و روی ساعت‌ها انتخابگر ساعت باز می‌کند (تایپ دستی همچنان آزاد است) */
        var pk = /examDate|gradesDate|examDay/i.test(f.id) ? 'date' : /examTime|startTime/i.test(f.id) ? 'time' : null;
        if (pk && input.tagName === 'INPUT') {
          input.classList.add('has-picker'); input.title = pk === 'date' ? 'برای انتخاب از تقویم شمسی کلیک کنید' : 'برای انتخاب ساعت کلیک کنید';
          input.addEventListener('click', function () {
            var cur = null;
            if (pk === 'date') { var m = /^(\d{4})\/(\d{1,2})\/(\d{1,2})/.exec(en(input.value || '')); if (m) cur = {jy: +m[1], jm: +m[2], jd: +m[3]}; }
            var hm = pk === 'time' ? /^(\d{1,2}):(\d{2})/.exec(en(input.value || '')) : null;
            window.SiteJalali.picker({mode: pk, title: f.label.replace(/[:：]\s*$/, ''), selected: cur, hour: hm ? +hm[1] : 8, minute: hm ? +hm[2] : 0, canClear: !!input.value}).then(function (r) {
              if (r === null) return;
              if (r === '') input.value = '';
              else if (pk === 'time') input.value = fa(String(r.h).padStart(2, '0')) + ':' + fa(String(r.m).padStart(2, '0'));
              else if (/examDay/i.test(f.id)) { var g = window.SiteJalali.toGregorian(r.jy, r.jm, r.jd); input.value = window.SiteJalali.WEEKDAYS[(g.getUTCDay() + 1) % 7]; }
              else input.value = fa(r.jy) + '/' + fa(String(r.jm).padStart(2, '0')) + '/' + fa(String(r.jd).padStart(2, '0'));
              values[f.id] = input.value; if (opts.autosave) persist(); input.dispatchEvent(new Event('change'));
            });
          });
        }
        input.addEventListener('input', function () { values[f.id] = input.value; if (opts.autosave) persist(); });
        input.addEventListener('change', function () { values[f.id] = input.value; if (opts.autosave) persist(); });
        list.appendChild(el('div', {class: 'field' + (f.full ? ' full' : '')}, [el('label', {text: f.label}), input]));
      }
    }
    sel.addEventListener('change', function () { tplId = sel.value; draw(); if (opts.autosave) persist(); });
    box.appendChild(el('div', {class: 'field'}, [el('label', {text: 'انتخاب نوع سربرگ'}), sel]));
    box.appendChild(list);
    draw();
    return {payload: payload, persist: persist};
  }
  function openHeaderSettings(onApply) {
    var schema = window.__HEADER_SCHEMA;
    if (!schema || !Array.isArray(schema.templates) || !schema.templates.length) { confirmDlg('تنظیمات سربرگ', 'قالب‌های سربرگ خوانده نشد. لطفاً دوباره تلاش کنید.', 'باشد'); return; }
    var bg = el('div', {class: 'modal-bg hdr-bg', onclick: function (e) { if (e.target === bg) bg.remove(); }});
    var box = el('div', {class: 'modal hdr-modal'});
    box.appendChild(el('div', {class: 'row', style: 'margin-bottom:8px'}, [el('h2', {class: 'grow', text: 'اطلاعات سربرگ آزمون', style: 'margin:0'}), el('button', {class: 'x', text: '✕', 'aria-label': 'بستن', onclick: function () { bg.remove(); }})]));
    var form = headerSettingsForm(box);
    box.appendChild(el('div', {class: 'row hdr-actions', style: 'margin-top:10px'}, [
      el('button', {class: 'btn light', text: 'انصراف', onclick: function () { bg.remove(); }}),
      el('button', {class: 'btn', text: 'اعمال', onclick: function () {
        if (form) form.persist();
        bg.remove(); toast('سربرگ ذخیره شد.', 'ok'); if (onApply) onApply(form ? form.payload() : readPrintHeader());
      }})
    ]));
    bg.appendChild(box); document.body.appendChild(bg);
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
        /* V208 — چیدمان خودِ سؤال؛ خالی → پیش‌فرض آزمون (تنظیمات صفحه)؛ دو گزینه‌ای مثل قبل یک ردیف */
        o.optionsLayout = ['1row', '2rows', '4rows'].indexOf(q.optionsLayout) >= 0 ? q.optionsLayout : (o.options.length <= 2 ? '1row' : '');
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
  /* V220 — تاریخچهٔ صفحات برای دکمهٔ برگشت (دسکتاپ و گوشی): هر تغییر پنل در render ثبت می‌شود؛
     برگشت = صفحهٔ قبلی (نه خانه) و هرگز خروج از سایت. mobile.js روی popstate، S.navBack را صدا می‌زند. */
  var navStack = [], navLast = null, navPopping = false;
  function navKey() { var a = null; try { a = JSON.stringify(view.arg || null); } catch (e) { a = String(view.arg); } return view.panel + '|' + a; }
  function navTrack() {
    if (!user) { navLast = null; navStack = []; return; }
    var k = navKey();
    if (navLast && k !== navLast.key) {
      if (navPopping) navPopping = false;
      else { navStack.push(navLast); if (navStack.length > 30) navStack.shift(); try { history.pushState({nav: 1}, ''); } catch (e) {} }
    }
    navLast = {key: k, panel: view.panel, arg: view.arg};
  }
  function navBack() {
    var prev = navStack.pop();
    if (!prev) return false;
    navPopping = true; view.panel = prev.panel; view.arg = prev.arg; render(); return true;
  }
  function render() {
    root = $('root');
    navTrack();
    document.body.classList.remove('dk');
    /* V148 — پوستهٔ موبایل (mobile.js) در حالت گوشی/معلم جای پنل دسکتاپ را می‌گیرد */
    /* V202.6 — گوشی: با نشست فعال هم اول صفحهٔ اول (دریافت برنامه / ورود به سامانه) تا کاربر «ورود به سامانه» بزند */
    if (user && !user.requiresSetup && window.SiteMobile && window.SiteMobile.gateActive()) { document.body.classList.remove('m-mode'); closeAuth(); root.innerHTML = ''; window.SiteMobile.paintAuth(root); return; }
    if (user && !user.requiresSetup && window.SiteMobile && window.SiteMobile.active()) { document.body.classList.add('m-mode'); window.SiteMobile.paint(); return; }
    document.body.classList.remove('m-mode');
    /* V154 — ورود/ثبت‌نام در گوشی به سبک SignInScreen اپ (پوستهٔ یخی) */
    if (!user && window.SiteMobile && window.SiteMobile.authActive()) { closeAuth(); root.innerHTML = ''; window.SiteMobile.paintAuth(root); return; }
    root.innerHTML = '';
    if (user && !user.requiresSetup) { document.body.classList.remove('lp-body'); renderPanel(); } else renderLanding();
  }

  /* ---------------- لندینگ (V164: صفحهٔ ورود به سبک اپ؛ فرم داخل کارت، بدون پنجرهٔ بازشو) ---------------- */
  function renderLanding(mode) {
    document.body.classList.add('lp-body');
    var keyWarn = KEY_READY ? null : el('div', {class: 'warn-key', html: '⚠️ کلید اتصال (<code class="k">SUPABASE_ANON_KEY</code>) هنوز در بالای فایل <code class="k">index.html</code> وارد نشده است؛ تا آن زمان ورود و ثبت‌نام کار نمی‌کند.'});
    /* V226 — طرح ۲۶ «آسمانی و ابری»: کاشی‌های شیشه‌ای با آیکن خطی آبی (ترتیب تصویر مرجع) */
    var LP_ICO = {
      report: '<path d="M4 20h16"/><rect x="6" y="11" width="3" height="7" rx="1"/><rect x="11" y="7" width="3" height="11" rx="1"/><rect x="16" y="13" width="3" height="5" rx="1"/><circle cx="17.5" cy="6.5" r="3"/><path d="M16.3 7.7l2.4-2.4"/>',
      print: '<path d="M7 3h8l4 4v8H7z"/><path d="M15 3v4h4"/><path d="M9 10h5M9 13h5"/><rect x="5" y="17" width="10" height="4" rx="1"/><path d="M8 17v-2M12 17v-2"/>',
      formula: '<path d="M4 4v16h16"/><path d="M8 7h5M10.5 7l-3 10"/><path d="M13 9.5l3 3M16 9.5l-3 3"/><path d="M12 15h5"/>',
      periodic: '<path d="M4 20h16"/><path d="M5 20V9h3v11M8 13h3v7M11 11h3v9M14 13h3v7M17 9h3v11"/><path d="M5 9V5h2v4M18 9V5h2v4"/>',
      board: '<path d="M4 6l12-2v12L4 18z"/><path d="M4 18l4 3M16 16l4 2"/><path d="M8 12l4-1.2"/>',
      school: '<path d="M3 21h18"/><path d="M5 21V10l7-5 7 5v11"/><path d="M12 5V3M12 3h3"/><rect x="10" y="15" width="4" height="6"/><path d="M8 12h2M14 12h2"/>'
    };
    var tiles = [['report', 'کارنامهٔ خودکار'], ['print', 'چاپ رسمی'], ['formula', 'ویرایشگر فرمول'], ['periodic', 'جدول تناوبی'], ['board', 'تختهٔ سفید'], ['school', 'مدیریت مدرسه']];
    var left = el('div', {class: 'lp-l'}, [
      brandEl('آزمون آنلاین'), /* V222.2 — نام در صفحهٔ ورود دسکتاپ */
      el('h1', {html: 'آزمون بسازید،<br><span>هوشمند برگزار کنید.</span>'}),
      el('p', {text: 'فرمول ریاضی، شکل هندسی، تختهٔ سفید و چاپ رسمی A4 — یک حساب برای اپ اندروید و وب.'}),
      el('div', {class: 'tiles'}, tiles.map(function (t) { return el('div', {class: 'tile neo'}, [el('i', {'aria-hidden': 'true', html: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">' + LP_ICO[t[0]] + '</svg>'}), el('span', {text: t[1]})]); }))
    ]);
    var card = el('div', {class: 'card neo'});
    drawAuthInto(card, mode || 'login');
    var right = el('div', {class: 'lp-r'}, [card]);
    var page = el('div', {class: 'lp lp-sky'}, [
      el('div', {class: 'glow g1'}), el('div', {class: 'glow g2'}), el('div', {class: 'cloud c1'}), el('div', {class: 'cloud c2'}), el('div', {class: 'cloud c3'}), el('div', {class: 'cloud c4'}),
      left, right
    ]);
    if (keyWarn) page.appendChild(keyWarn);
    root.appendChild(page);
  }
  var APP_MARK_SVG = '<svg viewBox="0 0 108 108" width="100%" height="100%" aria-hidden="true"><rect x="0" y="0" width="108" height="108" rx="22" fill="#fff"/><image href="/pwa/v3/mark-256.png" x="10" y="10" width="88" height="88"/></svg>'; /* V253 — لوگوی جدید «آزمون» (همان آیکون اپ اندروید) */
  function brandEl(label) { return el('div', {class: 'logo'}, [el('span', {class: 'mark', html: APP_MARK_SVG}), el('span', {text: label || 'آزمون آنلاین'})]); }

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
  /* V164 — دیگر پنجرهٔ بازشو نداریم: فرم داخل کارت صفحهٔ ورود رسم می‌شود. openAuth/closeAuth برای سازگاری مانده‌اند. */
  var authModal = null;
  function closeAuth() { if (authModal) { authModal.remove(); authModal = null; } }
  function openAuth(mode) { closeAuth(); if (!user) { root = $('root'); root.innerHTML = ''; renderLanding(mode); } }
  function drawAuthInto(m, mode) {
    var state = {mode: mode === 'register-manager' ? 'register' : (mode || 'login'), role: mode === 'register-manager' ? 'manager' : 'teacher', step: 'form', email: '', otpMode: false};
    function draw() {
      m.innerHTML = '';
      var isLogin = state.mode === 'login';
      m.appendChild(el('h2', {text: isLogin ? 'ورود به حساب' : 'ساخت حساب جدید'}));
      m.appendChild(el('p', {class: 's', text: isLogin ? 'معلم، مدیر و دانش‌آموز' : 'برای معلم و مدیر / معاون'}));
      if (!KEY_READY) m.appendChild(el('div', {class: 'alert warn', text: 'کلید SUPABASE_ANON_KEY در فایل وارد نشده؛ ورود ممکن نیست.'}));
      var tabs = el('div', {class: 'tabs neo-in', role: 'tablist'}, [
        el('button', {type: 'button', role: 'tab', 'aria-selected': String(isLogin), class: isLogin ? 'on' : '', text: 'ورود', onclick: function () { state.mode = 'login'; state.step = 'form'; state.otpMode = false; draw(); }}),
        el('button', {type: 'button', role: 'tab', 'aria-selected': String(!isLogin), class: !isLogin ? 'on' : '', text: 'ثبت‌نام', onclick: function () { state.mode = 'register'; state.step = 'form'; draw(); }})
      ]);
      m.appendChild(tabs);
      if (isLogin) drawLogin(); else drawRegister();
      var f = m.querySelector('input'); if (f && window.innerWidth > 1024) f.focus();
    }
    var msg = el('div');
    function setMsg(t, kind) { msg.innerHTML = ''; if (t) msg.appendChild(el('div', {class: 'alert ' + (kind || 'error'), text: t})); }
    function busy(btn, on) { btn.disabled = on; btn.textContent = on ? 'لطفاً صبر کنید…' : btn.dataset.label; }
    function drawLogin() {
      m.appendChild(msg); setMsg('');
      if (!state.otpMode) {
        var id = input('نام کاربری یا ایمیل', '', 'text', true); id.classList.add('with-ico'); id.appendChild(el('span', {class: 'fld-ico', 'aria-hidden': 'true', html: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="8" r="4"/><path d="M4 20c0-3.6 3.6-6 8-6s8 2.4 8 6"/></svg>'})); /* V222 — بدون مثال، با آیکون کاربر */
        id.querySelector('input').setAttribute('autocomplete', 'username'); id.querySelector('input').setAttribute('inputmode', 'email');
        var pw = input('رمز عبور', '', 'password', true);
        var b = el('button', {class: 'btn', text: 'ورود', 'data-label': 'ورود', style: 'width:100%'});
        b.addEventListener('click', async function () {
          setMsg(''); busy(b, true);
          try { user = await api.signInWithPassword(id.querySelector('input').value, pw.querySelector('input').value); state.fails = 0; afterLogin(); }
          catch (e) {
            /* V228 — پس از ۳ تلاش ناموفق: یادآوری «فراموشی رمز»؛ پس از ۵ تلاش: ۳۰ ثانیه مکث (فقط سمت مرورگر) */
            state.fails = (state.fails || 0) + 1;
            var m = errMsg(e);
            if (state.fails >= 3) m += ' — اگر رمز را فراموش کرده‌اید، از «فراموشی رمز» پایین صفحه استفاده کنید.';
            setMsg(m);
            if (state.fails >= 5) { b.disabled = true; var left = 30, t0 = b.textContent; var iv = setInterval(function () { left--; b.textContent = 'لطفاً ' + fa(left) + ' ثانیه صبر کنید'; if (left <= 0) { clearInterval(iv); b.disabled = false; b.textContent = t0; state.fails = 3; } }, 1000); b.textContent = 'لطفاً ' + fa(left) + ' ثانیه صبر کنید'; }
          }
          busy(b, false);
        });
        pw.querySelector('input').addEventListener('keydown', function (e) { if (e.key === 'Enter') b.click(); });
        /* V236 — «ورود با گوگل» بالای فرم */
        if (window.SiteExtras) { var gbl = window.SiteExtras.googleButton('teacher'); gbl.style.marginTop = '0'; m.appendChild(gbl); m.appendChild(el('div', {class: 'sep', text: 'یا'})); }
        m.appendChild(id); m.appendChild(pw); m.appendChild(b);
        m.appendChild(el('div', {class: 'lk'}, [
          el('a', {href: '#', text: 'ورود با کد ایمیل', onclick: function (e) { e.preventDefault(); state.otpMode = true; state.step = 'form'; draw(); }}),
          el('a', {href: '#', text: 'فراموشی رمز', onclick: function (e) { e.preventDefault(); state.otpMode = 'recovery'; draw(); }})
        ]));
        m.appendChild(el('p', {class: 'hint', text: 'دانش‌آموزان با نام کاربری و رمزی که معلم داده وارد می‌شوند.'}));
      } else if (state.otpMode === 'recovery' && window.SiteExtras) {
        m.appendChild(el('p', {class: 'muted', style: 'font-size:13px', text: 'بازیابی رمز عبور با ایمیل حساب. پس از تأیید کد، رمز جدید بگذارید.'}));
        window.SiteExtras.recoveryFlow(m, api, setMsg, busy, async function () { user = await currentProfile(); afterLogin(); });
        m.appendChild(el('div', {class: 'lk', style: 'justify-content:center'}, [el('a', {href: '#', text: '← بازگشت به ورود با رمز', onclick: function (e) { e.preventDefault(); state.otpMode = false; state.step = 'form'; draw(); }})]));
      } else {
        if (state.step === 'form') {
          var em = input('ایمیل', '', 'email', true);
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
        m.appendChild(el('div', {class: 'lk', style: 'justify-content:center'}, [el('a', {href: '#', text: '← بازگشت به ورود با رمز', onclick: function (e) { e.preventDefault(); state.otpMode = false; state.step = 'form'; draw(); }})]));
      }
    }
    function drawRegister() {
      m.appendChild(el('div', {class: 'tabs neo-in', style: 'margin-top:-6px'}, [
        el('button', {class: state.role === 'teacher' ? 'on' : '', text: '👩‍🏫 معلم', onclick: function () { state.role = 'teacher'; state.step = 'form'; draw(); }}),
        el('button', {class: state.role === 'manager' ? 'on' : '', text: '🏫 مدیر / معاون', onclick: function () { state.role = 'manager'; state.step = 'form'; draw(); }})
      ]));
      m.appendChild(msg); setMsg('');
      if (state.step === 'form') {
        var name = input('نام و نام خانوادگی', '', 'text', false);
        var em = input('ایمیل', '', 'email', true);
        var b1 = el('button', {class: 'btn', text: 'ارسال کد تأیید', 'data-label': 'ارسال کد تأیید', style: 'width:100%'});
        b1.addEventListener('click', async function () {
          setMsg(''); busy(b1, true);
          try { state.fullName = name.querySelector('input').value; state.email = requireEmail(em.querySelector('input').value); await api.sendRegistrationOtp(state.email, state.fullName, state.role); state.step = 'code'; draw(); }
          catch (e) { setMsg(errMsg(e)); }
          busy(b1, false);
        });
        /* V234 — ثبت‌نام با گوگل (همان OAuth ورود؛ نقش انتخاب‌شده ذخیره می‌شود)؛ V236 — بالای فرم */
        if (window.SiteExtras) { var gb = window.SiteExtras.googleButton(state.role || 'teacher', true); gb.innerHTML = gb.innerHTML.replace('ورود با گوگل', 'ثبت‌نام با گوگل'); gb.style.marginTop = '0'; m.appendChild(gb); m.appendChild(el('div', {class: 'sep', text: 'یا'})); }
        m.appendChild(name); m.appendChild(em); m.appendChild(b1);
        m.appendChild(el('p', {class: 'hint', text: 'دانش‌آموزان نیازی به ثبت‌نام ندارند؛ معلم برایشان حساب می‌سازد.'}));
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
      var inp = el('input', {type: type || 'text', placeholder: ph || '', autocomplete: type === 'password' ? 'current-password' : 'on'});
      var f = el('div', {class: 'field' + (ltr ? ' ltr' : '') + (type === 'password' ? ' pw' : '')}, [el('label', {text: label}), inp]);
      if (type === 'password') { inp.style.cssText += ';direction:ltr;text-align:left;padding-right:50px;padding-left:14px'; f.appendChild(eyeButton(inp)); } /* V202.4 — مستقل از cascade */
      return f;
    }
    draw();
  }
  /* V164 — دکمهٔ نمایش/پنهان رمز */
  function eyeButton(inp) {
    var b = el('button', {type: 'button', class: 'eye', style: 'position:absolute;right:4px;left:auto;bottom:4px', 'aria-label': 'نمایش رمز عبور', 'aria-pressed': 'false', html: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M2 12s3.6-6.5 10-6.5S22 12 22 12s-3.6 6.5-10 6.5S2 12 2 12z"/><circle cx="12" cy="12" r="2.7"/></svg>'});
    b.addEventListener('click', function () { var show = inp.type === 'password'; inp.type = show ? 'text' : 'password'; b.setAttribute('aria-pressed', String(show)); b.setAttribute('aria-label', show ? 'پنهان کردن رمز عبور' : 'نمایش رمز عبور'); inp.focus(); });
    return b;
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
    function fld(label, val, ltr, type) { var inp = el('input', {type: type || 'text', value: val || ''}); var f = el('div', {class: 'field' + (ltr ? ' ltr' : '') + (type === 'password' ? ' pw' : '')}, [el('label', {text: label}), inp]); if (type === 'password') { inp.style.cssText += ';direction:ltr;text-align:left;padding-right:50px;padding-left:14px'; f.appendChild(eyeButton(inp)); } return f; }
  }
  function afterLogin() {
    closeAuth();
    if (window.SiteMobile && window.SiteMobile.enter) window.SiteMobile.enter(); /* V202.6 — پس از ورود، دروازهٔ صفحهٔ اول در این نشست بسته است */
    if (user && user.requiresSetup) { renderSetupGate(); return; }
    prefetchEngines();
    view.panel = 'dashboard';
    toast('خوش آمدید، ' + (user.name || ''), 'ok');
    render();
  }
  function renderSetupGate() {
    root = $('root'); root.innerHTML = ''; document.body.classList.add('lp-body');
    var m = el('div', {class: 'card neo', style: 'max-width:480px;margin:40px auto;padding:32px 30px'});
    var wrap = el('div', {class: 'lp', style: 'display:block'}); wrap.appendChild(m); root.appendChild(wrap);
    m.appendChild(el('h2', {text: 'تکمیل ثبت‌نام ' + (user.pendingRole === 'manager' ? 'مدیر / معاون' : 'معلم')}));
    var msg = el('div'); m.appendChild(msg);
    drawCompletion(m, user.pendingRole || 'teacher', user.name, msg, function (t) { msg.innerHTML = ''; if (t) msg.appendChild(el('div', {class: 'alert error', text: t})); });
    m.appendChild(el('button', {class: 'btn light', style: 'width:100%;margin-top:10px', text: 'خروج', onclick: doLogout}));
  }
  async function doLogout() { await authApi.signOut(); user = null; view.panel = 'dashboard'; render(); }

  /* ---------------- پنل ---------------- */
  var MENUS = {
    teacher: [
      ['dashboard', '🏠', 'داشبورد'], ['exams', '📝', 'آزمون‌ها'], ['builder', '➕', 'آزمون جدید'], ['wallet', '👛', 'کیف پول'], ['cards', '🃏', 'کارت‌ها']
    ],
    /* V168 — صفحهٔ «منو»ی دسکتاپ معلم = منوی همبرگری اپ/گوشی (menuScreen در mobile.js) */
    teacherMenu: [['calendar', '📅', 'تقویم', 'رویدادها و پیام‌ها'], ['print', '🖨', 'چاپ آزمون', 'آزمون‌های چاپی و برگه'], ['students', '🎓', 'دانش‌آموزان', 'فهرست و وضعیت'], ['classes', '🏫', 'کلاس‌ها', 'فهرست و مدیریت'], ['account', '👤', 'حساب', 'مشخصات و امنیت حساب'], ['settings', '⚙', 'تنظیمات', 'ظاهر، داده و درباره']],
    /* V183 — ریل دانش‌آموز بدون «ابزارها» و «پروفایل» (حساب از صفحهٔ منو) */
    student: [['dashboard', '🏠', 'داشبورد'], ['join', '🔑', 'شرکت در آزمون'], ['grades', '📊', 'کارنامه'], ['calendar', '📅', 'تقویم و پیام‌ها']],
    /* V184 — ریل مدیر/معاون بدون «ابزارها» و «پروفایل» */
    manager: [['dashboard', '🏠', 'داشبورد'], ['teachers', '👩‍🏫', 'معلم‌ها'], ['school', '🏫', 'مدرسه'], ['classes', '🏫', 'کلاس‌ها'], ['students', '🎓', 'دانش‌آموزان'], ['calendar', '📅', 'تقویم'], ['wallet', '👛', 'کیف پول']], /* V203 — هم‌تراز اپ: کلاس‌ها، دانش‌آموزان، تقویم */
    /* V171 — صفحهٔ «منو»ی مدیر و دانش‌آموز = منوی همبرگری اپ (ExamApp.kt:995-1060) */
    managerMenu: [['classes', '🏫', 'کلاس‌ها', 'فهرست و مدیریت'], ['students', '🎓', 'دانش‌آموزان', 'فهرست و مدیریت'], ['calendar', '📅', 'تقویم', 'رویدادها و پیام‌ها'], ['account', '👤', 'حساب', 'مشخصات و امنیت حساب'], ['site', '🌐', 'سایت', 'onlineexam.ir'], ['settings', '⚙', 'تنظیمات', 'ظاهر، داده و درباره']],
    studentMenu: [['join', '🔑', 'آزمون', 'ورود با کد آزمون'], ['grades', '📊', 'نتایج من', 'پاسخ‌ها و کارنامه'], ['calendar', '📅', 'تقویم', 'رویدادها و پیام‌ها'], ['account', '👤', 'حساب', 'مشخصات و امنیت حساب'], ['settings', '⚙', 'تنظیمات', 'ظاهر، داده و درباره']]
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
      el('img', {src: '/pwa/v3/icon-192.png', alt: ''}),
      el('div', {class: 't'}, [el('b', {text: 'نصب آزمون آنلاین روی گوشی'}), el('span', {text: ios ? 'در Safari دکمهٔ «اشتراک» و سپس «Add to Home Screen» را بزنید.' : 'مثل یک برنامه، تمام‌صفحه و با آیکون روی صفحهٔ اصلی.'})]),
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
  /* V165 — پوستهٔ دسکتاپ به سبک «نسخهٔ یکپارچه»: ریل عمودی سمت راست (همهٔ بخش‌ها + منو)، نوار بالا با نشان و دکمهٔ ☰،
     صفحهٔ «منو» = کارت پروفایل + شبکهٔ کارت‌ها. سایدبار/منوی پایین قدیمی فقط برای عرض ≤ 860 ساخته می‌شود (پوستهٔ گوشی جای آن را می‌گیرد). */
  var DK_ICONS = {
    menu: '<path d="M4 7h16M4 12h16M4 17h16"/>',
    dashboard: '<rect x="3" y="4" width="8" height="7" rx="2"/><rect x="13" y="4" width="8" height="7" rx="2"/><rect x="3" y="13" width="8" height="7" rx="2"/><rect x="13" y="13" width="8" height="7" rx="2"/>',
    exams: '<rect x="5" y="3" width="14" height="18" rx="2"/><path d="M9 8h6M9 12h6M9 16h4"/>',
    builder: '<path d="M12 5v14M5 12h14"/>',
    classes: '<rect x="3" y="4" width="18" height="12" rx="2"/><path d="M8 20h8M12 16v4M7 9h6M7 12h4"/>',
    students: '<circle cx="9" cy="8" r="3.2"/><path d="M3 20c0-3.3 2.7-6 6-6s6 2.7 6 6"/><circle cx="17" cy="9" r="2.4"/><path d="M15.5 14.5c2.8.2 5.5 2.3 5.5 5.5"/>',
    bank: '<path d="M3 10l9-5 9 5H3zM5 10v7M10 10v7M14 10v7M19 10v7M3 20h18"/>',
    reports: '<path d="M4 20V10M10 20V4M16 20v-7M22 20H2"/>',
    grading: '<rect x="5" y="4" width="14" height="17" rx="2"/><path d="M9 4h6v3H9zM8.5 13l2.5 2.5 4.5-4.5"/>',
    site: '<circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3c3 3.5 3 14.5 0 18M12 3c-3 3.5-3 14.5 0 18"/>',
    calendar: '<rect x="3" y="5" width="18" height="16" rx="3"/><path d="M3 10h18M8 3v4M16 3v4"/>',
    wallet: '<rect x="3" y="6" width="18" height="13" rx="3"/><path d="M3 10h18M16 14h2"/>',
    tools: '<path d="M4 20l6-6M14 4l6 6M10 14l4-4M13 3l8 8-4 4-8-8z"/>',
    cards: '<rect x="4" y="6" width="11" height="15" rx="2" transform="rotate(-8 9.5 13.5)"/><rect x="10" y="4" width="11" height="15" rx="2" transform="rotate(8 15.5 11.5)"/>',
    profile: '<circle cx="12" cy="8" r="4"/><path d="M4 21c0-4 3.6-7 8-7s8 3 8 7"/>',
    join: '<circle cx="8" cy="12" r="4"/><path d="M12 12h9M18 12v3M15 12v2"/>',
    grades: '<path d="M4 4h16v13H4zM8 21h8M12 17v4M8 12l3-3 2 2 3-4"/>',
    teachers: '<circle cx="12" cy="7" r="3.5"/><path d="M5 20c0-3.9 3.1-7 7-7s7 3.1 7 7M3 4l4 1M21 4l-4 1"/>',
    school: '<path d="M2 9l10-5 10 5-10 5z"/><path d="M6 11.5V16c0 1.5 3 3 6 3s6-1.5 6-3v-4.5M22 9v6"/>',
    account: '<circle cx="12" cy="8" r="4"/><path d="M4 21c0-4 3.6-7 8-7s8 3 8 7"/>',
    settings: '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z"/>',
    print: '<path d="M7 9V4.5A1.5 1.5 0 0 1 8.5 3h7A1.5 1.5 0 0 1 17 4.5V9"/><rect x="3" y="9" width="18" height="8.5" rx="2.2"/><path d="M7 14.5h10V21H7z"/><path d="M9.5 17.3h5M9.5 19.2h3.5"/><circle cx="17.2" cy="12.2" r="0.9" fill="currentColor" stroke="none"/>',
    logout: '<path d="M10 4H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h4M15 8l4 4-4 4M19 12H9"/>',
    back: '<path d="M15 6l-6 6 6 6"/>',
    brand: '<rect x="5" y="3" width="14" height="18" rx="2"/><path d="M9 4h6v3H9zM8.5 13l2.5 2.5 4.5-4.5"/>'
  };
  var DK_SUBS = {dashboard: 'خلاصهٔ وضعیت و آمار', exams: 'فهرست و مدیریت', builder: 'ساخت آزمون آنلاین / چاپی', classes: 'فهرست و اعضا', students: 'فهرست و وضعیت', bank: 'سؤال‌های ذخیره‌شده',
    reports: 'کارنامه و لیست نمرات', grading: 'تشریحی و نظارت', calendar: 'رویدادها و پیام‌ها', wallet: 'موجودی و شارژ', tools: 'فرمول، جدول، تناوبی', cards: 'آمار، کارنامه، بانک، تصحیح، درخواست‌ها', profile: 'مشخصات و امنیت حساب',
    join: 'ورود با کد معلم', grades: 'نمرات و نتایج', teachers: 'معلم‌های مدرسه', school: 'مشخصات مدرسه'};
  function dkIcon(name, cls) { return el('span', {class: 'dk-ic ' + (cls || ''), html: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">' + (DK_ICONS[name] || DK_ICONS.dashboard) + '</svg>'}); }
  function dkGo(panel) { view.panel = panel; view.arg = null; closeSidebar(); render(); }
  /* V226 — «افزودن سریع» دسکتاپ = منوی دایره‌ای وسط صفحه مثل انواع سؤال (radialMenu اپ/سایت)؛ ۵ کارت پاستلی بزرگ‌تر حول حلقهٔ خط‌چین؛
     اعمال = SiteMobile.quickAddItems (همان Design69QuickAddOverlay اپ) + «آزمون چاپی جدید» */
  var DK_QA_STYLE = {'آزمون جدید': ['exam', '📝', '#AEC6CF'], 'دعوت معلم': ['invite', '✉️', '#AEC6CF'], 'دانش‌آموز جدید': ['student', '🎓', '#B4EEB4'], 'کلاس جدید': ['class', '🏫', '#FDFD96'], 'مدرسه جدید': ['school', '🏛', '#C3B1E1'], 'آزمون چاپی جدید': ['print', '🖨', '#FFDAB9']};
  function dkQuickAdd(anchor) {
    var open = document.querySelector('.m-radial-bg.dk-qa-radial'); if (open) { open.remove(); return; }
    if (!window.SiteMobile || !window.SiteMobile.quickAddItems || !window.SiteMobile.radialMenu) return dkGo('builder');
    var items = window.SiteMobile.quickAddItems({withPrint: true});
    var byKey = {}, colors = {}, radialItems = items.map(function (it, i) { var st = DK_QA_STYLE[it[0]] || ['k' + i, '➕', '#E6E6FA']; byKey[st[0]] = it[3]; colors[st[0]] = st[2]; return [st[0], it[0], st[1]]; });
    if (anchor) anchor.classList.add('on');
    window.SiteMobile.radialMenu(function (key) { if (byKey[key]) byKey[key](); }, function () { if (anchor) anchor.classList.remove('on'); }, {items: radialItems, colors: colors, cls: 'dk-qa-radial'});
  }
  function renderPanel() {
    var menu = MENUS[user.role] || MENUS.student;
    document.body.classList.add('dk');
    var side = el('aside', {class: 'sidebar', id: 'sidebar'}, [
      el('div', {class: 'brand'}, [brandEl()]),
      el('div', {class: 'user'}, [
        el('div', {class: 'avatar', text: (user.name || '?').trim().charAt(0)}),
        el('div', {}, [el('div', {class: 'n', text: user.name || ''}), el('div', {class: 'r', text: ROLE_LABEL[user.role] + (user.username ? ' · ' + user.username : '')})])
      ]),
      el('div', {class: 'menu'}, menu.map(function (it) {
        if (it === '-') return el('div', {class: 'sep'});
        return el('button', {class: view.panel === it[0] ? 'on' : '', onclick: function () { dkGo(it[0]); }}, [el('span', {class: 'i', text: it[1]}), el('span', {text: it[2]})]);
      })),
      el('div', {class: 'foot'}, [el('button', {class: 'btn light', style: 'width:100%', text: 'خروج از حساب', onclick: doLogout})])
    ]);
    var items = menu.filter(function (x) { return x !== '-'; });
    var title = view.panel === 'menu' ? 'منو' : ((MENUS[user.role + 'Menu'] || []).concat(items, MENUS.teacherMenu).filter(function (x) { return x[0] === view.panel; })[0] || ['', '', ''])[2];
    /* V172 — عنوان صفحه‌های کارت‌ها مثل اپ (ExamApp.kt:381,1220,1226) */
    if (!title) { var a = view.arg || {}; title = {reports: a.section === 'grades' ? 'کارنامه' : 'آمار', grading: a.filter === 'pending' ? 'مانده' : (a.filter === 'graded' ? 'پاسخ' : 'تصحیح'), bank: 'بانک سؤال', requests: 'درخواست‌ها'}[view.panel] || ''; }
    /* V165 — ریل عمودی: «منو» + همهٔ بخش‌های نقش؛ نام هر مورد با نگه‌داشتن ماوس باز می‌شود */
    /* V176 — صفحه‌هایی که از «منو» باز می‌شوند (چاپ، تقویم، حساب، تنظیمات، …) در ریل، «منو» را روشن نگه می‌دارند */
    /* V197 — ریل دسکتاپ معلم: «چاپ آزمون» بالای «کیف پول» (فقط ریل؛ منوی پایین گوشی/تبلت بدون تغییر) */
    var railItems = items.slice();
    if (user.role === 'teacher' && !railItems.some(function (it) { return it[0] === 'print'; })) { var wi = railItems.findIndex(function (it) { return it[0] === 'wallet'; }); railItems.splice(wi < 0 ? railItems.length : wi, 0, ['print', '🖨', 'چاپ آزمون']); }
    var railKeys = ['menu'].concat(railItems.map(function (it) { return it[0]; }));
    var railActive = railKeys.indexOf(view.panel) >= 0 ? view.panel : 'menu';
    function railItem(key, label) {
      /* V225 — دکمهٔ «+» ریل بدون متن زیر خود؛ لمس آن منوی بادبزنی ۵ عمل (آزمون/دانش‌آموز/مدرسه/کلاس/آزمون چاپی جدید) را باز می‌کند */
      if (key === 'builder' && user.role === 'teacher') return el('button', {class: 'dk-rail-item dk-rail-plus' + (railActive === key ? ' active' : ''), 'aria-label': 'افزودن', 'aria-haspopup': 'menu', onclick: function (ev) { dkQuickAdd(ev.currentTarget); }}, [dkIcon(key)]);
      return el('button', {class: 'dk-rail-item' + (railActive === key ? ' active' : ''), 'aria-label': label, 'aria-current': railActive === key ? 'page' : null, onclick: function () { dkGo(key); }}, [dkIcon(key), el('span', {class: 'dk-rail-label', text: label})]);
    }
    var rail = el('nav', {class: 'dk-rail', 'aria-label': 'نوار اصلی', style: '--n:' + (railItems.length + 1)}, [railItem('menu', 'منو')].concat(railItems.map(function (it) { return railItem(it[0], it[2]); })));
    var main = el('main', {class: 'main'}, [
      el('div', {class: 'head dk-top'}, [
        el('button', {class: 'icon-btn hamb', html: '☰', 'aria-label': 'منو', onclick: toggleSidebar}),
        el('div', {class: 'dk-title'}, [el('h1', {text: title}), el('p', {text: 'سامانه آزمون آنلاین'})]),
        el('span', {class: 'dk-mark', html: APP_MARK_SVG}),
        el('button', {class: 'dk-burger' + (view.panel === 'menu' ? ' on' : ''), 'aria-label': 'منو', onclick: function () { dkGo(view.panel === 'menu' ? 'dashboard' : 'menu'); }}, [dkIcon('menu')])
      ]),
      el('div', {id: 'content'})
    ]);
    /* V146 — گوشی/تبلت: پس‌زمینهٔ سایدبار + منوی پایین (۴ مورد اول + «بیشتر» که سایدبار را باز می‌کند) */
    var sbBg = el('div', {class: 'sb-bg', id: 'sb-bg', onclick: closeSidebar});
    var primary = items.slice(0, 4);
    var bottom = el('nav', {class: 'bottom-nav', 'aria-label': 'منوی پایین'}, primary.map(function (it) {
      return el('button', {class: view.panel === it[0] ? 'on' : '', onclick: function () { view.panel = it[0]; view.arg = null; render(); }}, [el('span', {class: 'i', text: it[1]}), el('span', {text: it[2]})]);
    }).concat([el('button', {class: primary.some(function (it) { return it[0] === view.panel; }) ? '' : 'on', onclick: toggleSidebar}, [el('span', {class: 'i', text: '☰'}), el('span', {text: 'بیشتر'})])]));
    root.appendChild(el('div', {class: 'app'}, [side, sbBg, main, bottom, rail]));
    renderPage($('content'));
  }
  /* V165 — صفحهٔ «منو»: کارت پروفایل (نام، نقش، ایمیل/نام کاربری) + شبکهٔ کارت‌های همهٔ بخش‌ها + خروج */
  function pageMenu(c) {
    c.innerHTML = '';
    var menu = MENUS[user.role + 'Menu'] || MENUS.teacherMenu;
    var sub = user.email && !/student\.exam\.local$/.test(user.email) ? user.email : (user.username || '');
    /* V169 — کارت پروفایل هم‌اندازهٔ بقیهٔ کارت‌ها (اولین کارت شبکه) */
    var profileCard = el('button', {class: 'dk-mcard dk-mcard-profile', onclick: function () { view.panel = 'account'; view.arg = {tab: 'profile'}; render(); }}, [
      el('span', {class: 'dk-mhead'}, [el('span', {class: 'avatar dk-av', text: (user.name || '?').trim().charAt(0)}), el('span', {class: 'dk-pinfo'}, [el('small', {text: 'پروفایل ' + ROLE_LABEL[user.role]}), el('strong', {text: user.name || ''})])]),
      el('small', {text: sub})
    ]);
    function mcard(key, label, subLabel, danger, on) {
      return el('button', {class: 'dk-mcard' + (danger ? ' danger' : '') + (view.arg === key ? ' selected' : ''), onclick: on}, [el('span', {class: 'dk-mhead'}, [dkIcon(key, 'dk-mini'), el('strong', {text: label})]), el('small', {text: subLabel})]);
    }
    c.appendChild(el('div', {class: 'dk-grid'}, [profileCard].concat(menu.map(function (it) { return mcard(it[0], it[2], it[3] || DK_SUBS[it[0]] || '', false, function () { if (it[0] === 'account') { view.panel = 'account'; view.arg = {tab: 'account'}; render(); } else if (it[0] === 'site') { toast('شما هم‌اکنون در سایت هستید.', 'ok'); } else if (it[4]) { view.panel = it[4]; view.arg = it[5] || null; render(); } else dkGo(it[0]); }); }))
      .concat([mcard('logout', 'خروج', 'خروج امن و تعویض حساب', true, async function () { if (await confirmDlg('خروج از حساب', 'از حساب خارج می‌شوید؟', 'خروج', true)) doLogout(); })])));
  }
  /* V148 — رندر محتوای پنل جاری در هر ظرفی (پنل دسکتاپ یا پوستهٔ موبایل) */
  function renderPage(c) {
    var pages = {menu: pageMenu, cards: pageCards, print: pagePrint, account: pageAccount, settings: pageSettings, dashboard: pageDashboard, exams: pageExams, classes: pageClasses, students: pageStudents, wallet: pageWallet, tools: pageTools, profile: pageProfile, grades: pageGrades, teachers: pageTeachers,
      builder: function (c) { if (window.SiteBuilder) window.SiteBuilder.page(c, view.arg); else soon('سازندهٔ آزمون', 'فاز ۲')(c); }, bank: function (c) { if (window.SiteSchool) window.SiteSchool.bankPage(c); }, reports: function (c) { if (window.SiteExtras) window.SiteExtras.reportsPage(c, view.arg); }, requests: pageRequests, grading: function (c) { if (window.SiteAdmin) window.SiteAdmin.gradingPage(c, view.arg); else soon('تصحیح', 'فاز ۴')(c); }, calendar: function (c) { if (window.SiteAdmin) window.SiteAdmin.calendarPage(c, view.arg); }, join: function (c) { if (window.SiteStudent) window.SiteStudent.page(c, view.arg); else soon('شرکت در آزمون', 'فاز ۳')(c); }, school: function (c) { if (window.SiteAdmin) window.SiteAdmin.managerSchoolPage(c, view.arg); else soon('مدرسه', 'فاز ۴')(c); }};
    (pages[view.panel] || pageDashboard)(c);
  }
  function soon(title, phase) { return function (c) { c.appendChild(el('div', {class: 'soon', html: '<div style="font-size:40px">🚧</div><h3>' + esc(title) + '</h3>این بخش در <b>' + esc(phase) + '</b> سایت فعال می‌شود. فعلاً از برنامهٔ اندروید استفاده کنید.'})); }; }
  function loading(c) { c.innerHTML = '<div class="loading"><span class="spinner"></span> در حال دریافت…</div>'; }
  function showErr(c, e) { c.innerHTML = ''; c.appendChild(el('div', {class: 'alert error', text: errMsg(e)})); }
  /* V251 — هشدار بالای داشبورد وقتی بخشی از داده‌ها نیامده + دکمهٔ «تلاش دوباره» */
  function dashRetryAlert(failed, c) {
    var names = failed.map(function (f) { return f.label; }).join('، ');
    return el('div', {class: 'alert warn row', style: 'margin-bottom:12px;align-items:center;gap:10px'}, [
      el('span', {class: 'grow', text: 'بخشی از اطلاعات (' + names + ') بارگذاری نشد: ' + errMsg(failed[0].e)}),
      el('button', {class: 'btn light sm', text: 'تلاش دوباره', onclick: function () { pageDashboard(c); }})]);
  }
  function emptyBox(icon, text) { return el('div', {class: 'empty'}, [el('div', {class: 'big', text: icon}), el('div', {text: text})]); }
  /* V186 — کارت آمار: هم‌اندازه (min-height در CSS) و در صورت داشتن مقصد، کلیک‌پذیر (button) */
  /* V255 — پیک تصادفی: «از N» کارنامه از native_my_answers_v1 (meta.max_score) گرفته می‌شود؛ my_grades قدیمی بارم کل آزمون را می‌دهد. */
  function mergeAnswerMax(grades, answers) {
    var byId = {}, byKey = {};
    (answers || []).forEach(function (a) { if (a.id) byId[String(a.id)] = a; byKey[String(a.exam_id) + '|' + String(Date.parse(a.submitted_at) || '')] = a; });
    return (grades || []).map(function (g) {
      var a = (g.id && byId[String(g.id)]) || (g.answer_id && byId[String(g.answer_id)]) || byKey[String(g.exam_id) + '|' + String(Date.parse(g.submitted_at) || '')];
      if (a && Number(a.total_score) > 0 && Number(a.total_score) !== Number(g.total_score)) { var c = Object.assign({}, g); c.total_score = a.total_score; return c; }
      return g;
    });
  }
  function statCard(v, l, target) {
    var kids = [el('div', {class: 'v num', text: v}), el('div', {class: 'l', text: l})];
    if (!target) return el('div', {class: 'card stat'}, kids);
    return el('button', {class: 'card stat stat-link', type: 'button', 'aria-label': l, onclick: function () { if (typeof target.onclick === 'function') return target.onclick(); view.panel = target.panel; view.arg = target.arg || null; render(); }}, kids);
  }

  /* ---- داشبورد ---- */
  async function openManagerRequests() {
    if (!window.SiteSchool) return;
    var bg = el('div', {class: 'modal-bg', onclick: function (e) { if (e.target === bg) close(); }});
    var box = el('div', {class: 'modal', style: 'max-width:640px'});
    function close() { bg.remove(); render(); }
    box.appendChild(el('div', {class: 'row', style: 'margin-bottom:8px'}, [el('h2', {class: 'grow', text: 'درخواست‌های مدیر', style: 'margin:0'}), el('button', {class: 'x', text: '✕', 'aria-label': 'بستن', onclick: close})]));
    var card = await window.SiteSchool.managerRequestsCard(true); card.classList.add('mr-embed');
    box.appendChild(card); bg.appendChild(box); document.body.appendChild(bg);
  }
  async function pageDashboard(c) {
    loading(c);
    try {
      if (user.role === 'teacher') {
        /* V251 — خطای سرور/اینترنت دیگر «۰ آزمون / ۰ تومان» نشان نمی‌دهد: اگر همهٔ فراخوانی‌های اصلی شکست بخورند پیام خطا؛
           اگر بعضی شکست بخورند، همان کارت «—» می‌شود و هشدار «تلاش دوباره» بالای داشبورد می‌آید */
        var failed = [];
        function soft(pr, label) { return pr.catch(function (e) { failed.push({label: label, e: e}); return null; }); }
        var r = await Promise.all([soft(api.exams(), 'آزمون‌ها'), soft(api.classes(), 'کلاس‌ها'), soft(api.students(), 'دانش‌آموزان'), soft(api.wallet(), 'کیف پول'),
          rpcObj('native_teacher_manager_requests_v41', {}).then(function (x) { return x && !x.error ? x : null; }).catch(function () { return null; })]);
        if (failed.length === 4) throw failed[0].e;
        c.innerHTML = '';
        if (failed.length) c.appendChild(dashRetryAlert(failed, c));
        function cnt(x) { return Array.isArray(x) ? fa(x.length) : '—'; }
        /* V198 — «درخواست‌های مدیر» کارت پنجم هم‌شکل بقیه (تعداد در انتظار)؛ کلیک → پنجرهٔ فهرست با تأیید/رد */
        var reqs = (r[4] && r[4].items) || [], pendingN = reqs.filter(function (it) { return it.status === 'pending' || !it.status; }).length;
        var reqCard = statCard(r[4] ? fa(pendingN) : '—', 'درخواست مدیر', {onclick: function () { openManagerRequests(); }});
        if (pendingN) reqCard.classList.add('stat-attn');
        c.appendChild(el('div', {class: 'grid5'}, [statCard(cnt(r[0]), 'آزمون', {panel: 'exams'}), statCard(cnt(r[1]), 'کلاس', {panel: 'classes'}), statCard(cnt(r[2]), 'دانش‌آموز', {panel: 'students'}), statCard(r[3] ? money(r[3].balance) : '—', 'موجودی کیف پول', {panel: 'wallet'}), reqCard]));
        var card = el('div', {class: 'card', style: 'margin-top:16px'}, [el('h3', {text: '📝 آخرین آزمون‌ها'})]);
        if (!Array.isArray(r[0])) card.appendChild(el('div', {class: 'alert error', text: failed.length ? errMsg(failed[0].e) : 'فهرست آزمون‌ها دریافت نشد.'}));
        else if (!r[0].length) card.appendChild(emptyBox('📄', 'هنوز آزمونی نساخته‌اید.'));
        else card.appendChild(examTable(r[0].slice(0, 6), c));
        c.appendChild(card);
      } else if (user.role === 'student') {
        var g = (await api.myGrades()) || []; g = mergeAnswerMax(g, await api.myAnswers().catch(function () { return []; })); /* V255 — سقف نمرهٔ پیک تصادفی */ /* V251 — خطا به catch پایین می‌رود و پیام می‌دهد؛ نه «۰ آزمون شرکت‌کرده» */
        c.innerHTML = '';
        var graded = g.filter(function (x) { return x.graded_at; });
        var avg = graded.length ? graded.reduce(function (s, x) { return s + (Number(x.total_score) ? Number(x.total_grade) / Number(x.total_score) * 100 : 0); }, 0) / graded.length : 0;
        c.appendChild(el('div', {class: 'grid3'}, [statCard(fa(g.length), 'آزمون شرکت‌کرده', {panel: 'grades'}), statCard(fa(graded.length), 'تصحیح‌شده', {panel: 'grades'}), statCard(fa(Math.round(avg)) + '٪', 'میانگین درصد', {panel: 'grades'})]));
        c.appendChild(el('div', {class: 'alert info', style: 'margin-top:16px', html: 'برای شرکت در آزمون، کد معلم را در بخش <b>شرکت در آزمون</b> وارد کنید.'}));
        c.appendChild(el('div', {class: 'row', style: 'margin-top:12px'}, [el('button', {class: 'btn', text: '🔑 شرکت در آزمون', onclick: function () { view.panel = 'join'; view.arg = null; render(); }}), (window.SiteStudent && window.SiteStudent.hasActive()) ? el('span', {class: 'chip warn', text: 'آزمون نیمه‌تمام دارید'}) : null]));
      } else {
        var s = await api.managerSummary();
        c.innerHTML = '';
        c.appendChild(el('div', {class: 'card'}, [el('h3', {text: '🏫 ' + (s.school_name || 'مدرسه')}), el('div', {class: 'muted', text: [s.province, s.city].filter(Boolean).join('، ')})]));
        c.appendChild(el('div', {class: 'grid4', style: 'margin-top:16px'}, [statCard(fa(s.teachers || 0), 'معلم', {panel: 'teachers'}), statCard(fa(s.students || 0), 'دانش‌آموز', {panel: 'students'}), statCard(fa(s.classes || 0), 'کلاس', {panel: 'classes'}), statCard(fa(s.exams || 0), 'آزمون', {panel: 'teachers'})]));
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
    return el('table', {class: 'tbl tbl-cyan'}, [
      el('thead', {}, [el('tr', {}, ['عنوان', 'درس', 'کد', 'وضعیت', 'بارم', 'تاریخ', ''].map(function (h) { return el('th', {text: h}); }))]),
      el('tbody', {}, list.map(function (x) {
        return el('tr', {}, [el('td', {html: '<b>' + esc(x.title || 'بدون عنوان') + '</b>'}), el('td', {text: x.subject || '—'}), el('td', {}, [el('span', {class: 'code', text: x.code || '—'})]),
          el('td', {}, [el('span', {class: 'chip ' + (x.is_open ? 'ok' : 'off'), text: x.is_open ? 'باز' : 'بسته'})]), el('td', {text: fa(fmtScore(x.total_score))}), el('td', {class: 'muted', style: 'font-size:12px', text: fmtDate(x.created_at)}),
          el('td', {}, [examActions(x, function () { render(); })])]);
      }))
    ]);
  }
  /* V186 — دسکتاپ: دکمه‌های عملیات آزمون با متن (نه آیکن)؛ گوشی همان آیکن‌ها */
  function examActions(x, refresh) {
    var wrap = el('div', {class: 'acts acts-text'});
    wrap.appendChild(el('button', {class: 'icon-btn', title: 'ویرایش', html: '<i>✎</i><span>ویرایش</span>', onclick: function () { view.panel = 'builder'; view.arg = {examId: x.id}; render(); }}));
    wrap.appendChild(el('button', {class: 'icon-btn', title: 'پیش‌نمایش و چاپ', html: '<i>🖨</i><span>چاپ</span>', onclick: function () { printExam(x); }}));
    wrap.appendChild(el('button', {class: 'icon-btn', title: x.is_open ? 'بستن آزمون' : 'بازکردن آزمون', html: x.is_open ? '<i>🔒</i><span>بستن</span>' : '<i>🔓</i><span>بازکردن</span>', onclick: async function () {
      try { await api.setExamOpen(x.id, !x.is_open); toast(x.is_open ? 'آزمون بسته شد.' : 'آزمون باز شد.', 'ok'); refresh(); } catch (e) { toast(errMsg(e), 'err'); }
    }}));
    if (window.SiteExtras) wrap.appendChild(el('button', {class: 'icon-btn', title: 'صدور فایل آزمون', html: '<i>📤</i><span>صدور</span>', onclick: function () { window.SiteExtras.exportExamDlg(x); }}));
    wrap.appendChild(el('button', {class: 'icon-btn', title: 'کپی آزمون', html: '<i>⧉</i><span>کپی</span>', onclick: async function () {
      if (!(await confirmDlg('کپی آزمون', 'از «' + esc(x.title) + '» یک نسخهٔ جدید ساخته می‌شود (هزینهٔ سؤال‌ها طبق تعرفه کسر می‌شود).', 'کپی'))) return;
      try { var r = await api.duplicateExam(x.id); await costDoneDlg(r.cost || 0, r.balance, 'کپی ساخته شد؛ کد جدید: <b class="code">' + esc(r.code || '') + '</b>'); refresh(); } catch (e) { toast(errMsg(e), 'err'); }
    }}));
    wrap.appendChild(el('button', {class: 'icon-btn danger', title: 'حذف', html: '<i>🗑</i><span>حذف</span>', onclick: async function () {
      if (!(await confirmDlg('حذف آزمون', 'آزمون «' + esc(x.title) + '» و پاسخ‌های آن برای همیشه حذف می‌شود.', 'حذف', true))) return;
      /* V221 — قبل از حذف آزمون، آدرس رسانه‌هایش گرفته و پس از حذف از فضای ابری پاک می‌شود (best-effort) */
      var mediaUrls = []; try { var mr = await rpcObj('native_exam_media_urls_v221', {p_exam: x.id}); if (mr && Array.isArray(mr.urls)) mediaUrls = mr.urls; } catch (e0) { try { var mr2 = await rpcObj('native_exam_image_paths_v59', {p_exam: x.id}); if (mr2 && Array.isArray(mr2.urls)) mediaUrls = mr2.urls; } catch (e1) {} }
      try { await api.deleteExam(x.id); toast('حذف شد.', 'ok'); refresh(); deleteMedia(mediaUrls); } catch (e) { toast(errMsg(e), 'err'); }
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
      c.appendChild(el('div', {class: 'row', style: 'margin-bottom:16px'}, [q, el('span', {class: 'grow'}), el('span', {class: 'muted', text: fa(list.length) + ' آزمون'}), window.SiteExtras ? el('button', {class: 'btn light', text: '📥 وارد کردن', onclick: window.SiteExtras.importExam}) : null, el('button', {class: 'btn', text: '➕ آزمون جدید', onclick: function () { view.panel = 'builder'; view.arg = null; render(); }}), el('button', {class: 'btn light', text: '🖨 آزمون‌های چاپی', onclick: function () { var M = window.SiteMobile; if (M && M.printExamsSheet) M.printExamsSheet({online: true}); else { view.panel = 'print'; view.arg = null; render(); } }}) /* V223.1 — مثل اپ: فهرست آزمون‌های چاپی → سازندهٔ آنلاین */]));
      c.appendChild(grid); draw();
    } catch (e) { showErr(c, e); }
    /* V223 — آزمون‌های چاپی دیگر زیر فهرست آزمون‌ها نیست؛ دکمهٔ «آزمون‌های چاپی» کنار «آزمون جدید» به بخش «چاپ آزمون» می‌رود */
  }

  /* ---- کلاس‌ها ---- */
  async function pageClasses(c) {
    /* V203 — مدیر: همهٔ کلاس‌های مدرسه + ساخت کلاس برای معلم (admin.js) */
    if (user && user.role === 'manager' && window.SiteAdmin && window.SiteAdmin.managerClassesPage) return window.SiteAdmin.managerClassesPage(c);
    loading(c);
    if (view.arg && view.arg.create) { view.arg = null; classForm(null, function () { pageClasses(c); }); }
    try {
      var list = await api.classes();
      c.innerHTML = '';
      /* V227 — مثل ClassesContent اپ: «ساخت کلاس جدید» + «مدارس» کنار هم؛ «مدارس» نمای مدرسه → کلاس‌های مدرسه → دانش‌آموزان را باز می‌کند */
      c.appendChild(el('div', {class: 'row', style: 'margin-bottom:16px'}, [el('span', {class: 'muted', text: fa(list.length) + ' کلاس'}), el('span', {class: 'grow'}), el('button', {class: 'btn light', text: '🏫 مدارس', onclick: function () { pageSchools(c); }}), el('button', {class: 'btn', text: '➕ کلاس جدید', onclick: function () { classForm(null, function () { pageClasses(c); }); }})]));
      if (!list.length) { c.appendChild(el('div', {class: 'card'}, [emptyBox('🏫', 'هنوز کلاسی نساخته‌اید.')])); return; }
      c.appendChild(el('div', {class: 'card'}, [el('table', {class: 'tbl tbl-cyan'}, [
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
  /* V227 — نمای «مدارس» معلم (SchoolsContent اپ): ردیف «پیوستن به مدرسه» / «بازگشت به کلاس‌ها»؛ کارت هر مدرسه (نام، استان · شهر، تعداد کلاس‌های من)؛
     لمس کارت → کلاس‌های من در آن مدرسه (SchoolClassesContent: نام، پایه · رشته، اعضا/پسر/دختر؛ «بازگشت به مدارس»)؛ لمس کلاس → دانش‌آموزان همان کلاس */
  async function pageSchools(c) {
    loading(c);
    try {
      var r = await rpcObj('native_teacher_schools_v61', {});
      if (r && r.error) throw new Error(r.error);
      var schools = (r && r.items) || [];
      c.innerHTML = '';
      c.appendChild(el('div', {class: 'row', style: 'margin-bottom:16px'}, [
        el('button', {class: 'btn', text: 'پیوستن به مدرسه', onclick: function () { if (window.SiteMobile && window.SiteMobile.joinSchoolDialog) window.SiteMobile.joinSchoolDialog(); }}),
        el('span', {class: 'grow'}),
        el('button', {class: 'btn light', text: 'بازگشت به کلاس‌ها', onclick: function () { pageClasses(c); }})
      ]));
      if (!schools.length) { c.appendChild(el('div', {class: 'card'}, [emptyBox('🏫', 'هنوز عضو مدرسه‌ای نیستید. با کد دعوت مدیر به مدرسه بپیوندید.')])); return; }
      c.appendChild(el('div', {class: 'school-cards'}, schools.map(function (s) {
        return el('div', {class: 'card school-card', role: 'button', tabindex: '0', onclick: function () { pageSchoolClasses(c, s); }}, [
          el('h3', {text: s.name || 'مدرسه'}),
          el('p', {class: 'muted', text: [s.province, s.city].filter(Boolean).join(' · ') || '—'}),
          el('p', {text: 'کلاس‌های من در این مدرسه: ' + fa(s.classes || 0)})
        ]);
      })));
    } catch (e) { showErr(c, e); }
  }
  async function pageSchoolClasses(c, school) {
    loading(c);
    try {
      var list = await rpc('native_teacher_school_classes_v61', {p_school: school.id});
      list = list || [];
      c.innerHTML = '';
      c.appendChild(el('div', {class: 'row', style: 'margin-bottom:16px'}, [el('h3', {style: 'margin:0', text: 'کلاس‌های ' + (school.name || 'مدرسه')}), el('span', {class: 'grow'}), el('button', {class: 'btn light', text: 'بازگشت به مدارس', onclick: function () { pageSchools(c); }})]));
      if (!list.length) { c.appendChild(el('div', {class: 'card'}, [emptyBox('🏫', 'در این مدرسه کلاسی ندارید.')])); return; }
      c.appendChild(el('div', {class: 'school-cards'}, list.map(function (k) {
        return el('div', {class: 'card school-card', role: 'button', tabindex: '0', onclick: function () { rosterDlg(k); }}, [
          el('div', {class: 'row'}, [el('h3', {class: 'grow', style: 'margin:0', text: k.name || '—'}), el('span', {class: 'muted', text: 'پایه: ' + (k.grade || '—') + (k.field_of_study ? ' · ' + k.field_of_study : '')})]),
          el('p', {text: 'اعضا: ' + fa(k.total || 0) + ' نفر · پسر: ' + fa(k.boys || 0) + ' · دختر: ' + fa(k.girls || 0)})
        ]);
      })));
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
    return el('table', {class: 'tbl tbl-cyan'}, [
      el('thead', {}, [el('tr', {}, ['نام', 'نام کاربری', 'جنسیت', 'پایه', 'رشته', 'کلاس‌ها', 'وضعیت'].map(function (h) { return el('th', {text: h}); }))]),
      el('tbody', {}, list.map(function (s) {
        return el('tr', {}, [el('td', {html: '<b>' + esc(s.full_name || ((s.first_name || '') + ' ' + (s.last_name || ''))) + '</b>'}), el('td', {}, [el('span', {class: 'code', text: s.username || '—'})]),
          el('td', {text: s.gender === 'male' ? 'پسر' : (s.gender === 'female' ? 'دختر' : '—')}), el('td', {text: s.grade || '—'}), el('td', {text: s.field_of_study || '—'}), el('td', {class: 'muted', text: s.class_names || '—'}),
          el('td', {}, [el('span', {class: 'chip ' + (s.is_active !== false ? 'ok' : 'off'), text: s.is_active !== false ? 'فعال' : 'غیرفعال'})])]);
      }))
    ]);
  }
  async function pageStudents(c) {
    /* V203 — مدیر: دانش‌آموزان مدرسه + ساخت تکی/گروهی + افزودن به کلاس (admin.js) */
    if (user && user.role === 'manager' && window.SiteAdmin && window.SiteAdmin.managerStudentsPage) return window.SiteAdmin.managerStudentsPage(c, view.arg);
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
      /* V244 — دسکتاپ: موجودی (یک‌سوم) و شارژ (دو‌سوم) در یک ردیف؛ گوشی زیر هم */
      var balBox = el('div', {class: 'balance'}, [el('div', {class: 'row', style: 'align-items:center'}, [el('div', {class: 'grow', style: 'opacity:.9;font-weight:700', text: '👛 موجودی کیف پول'}), eye]), val, el('button', {class: 'btn light bal-tariff', text: 'تعرفه‌ها', onclick: function () { if (window.SiteAdmin && window.SiteAdmin.tariffDlg) window.SiteAdmin.tariffDlg(); }})]); /* V246 — دکمهٔ تعرفه‌ها زیر عدد */
      var topRow = el('div', {class: 'wallet-top'}, [balBox]);
      if (window.SiteAdmin && user.role !== 'student') topRow.appendChild(window.SiteAdmin.topUpCard(w.balance, function () { pageWallet(c); }));
      c.appendChild(topRow);
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

  /* V167 — «کارت‌ها»ی دسکتاپ: همان ۷ کارت گرادیانی گوشی (SiteMobile.teacherCards) به‌صورت شبکه؛ کلیک = همان مقصد */
  function pageCards(c) {
    c.innerHTML = '';
    var M = window.SiteMobile; if (!M || !M.teacherCards) { c.appendChild(el('div', {class: 'alert error', text: 'پوستهٔ کارت‌ها بارگذاری نشد.'})); return; }
    var grid = el('div', {class: 'dk-cards'});
    M.teacherCards().forEach(function (k) {
      grid.appendChild(el('button', {class: 'dk-gcard', style: 'background:' + k[3], onclick: k[4]}, [
        el('div', {class: 'm-deck-top'}, [el('span', {class: 'm-deck-ic', html: M.icons[k[2]] || ''}), el('small', {text: 'آزمون آنلاین'})]),
        el('b', {text: k[0]}), el('span', {class: 'dk-gdesc', text: k[1]})
      ]));
    });
    c.appendChild(grid);
  }

  /* V172 — کارت «درخواست‌ها» → صفحهٔ مستقل (TeacherManagerRequestsScreen اپ)، نه داشبورد */
  async function pageRequests(c) {
    c.innerHTML = '';
    if (!window.SiteSchool) return;
    c.appendChild(await window.SiteSchool.managerRequestsCard(true));
  }
  /* V168 — «چاپ آزمون» دسکتاپ: فهرست آزمون‌های چاپی روی سرور + ساخت آزمون چاپی جدید (همان printExamsSection) */
  function pagePrint(c) {
    c.innerHTML = '';
    /* V223 — مثل اپ: «آزمون چاپی جدید» + «آزمون‌های آنلاین» (ساخت نسخهٔ چاپی از آزمون آنلاین)؛ دکمهٔ تکراری داخل کارت حذف شد */
    var status = el('p', {class: 'm-note', style: 'display:none;padding:4px 0'});
    c.appendChild(el('div', {class: 'row', style: 'margin-bottom:6px'}, [
      el('button', {class: 'btn', text: '➕ آزمون چاپی جدید', onclick: function () { view.panel = 'builder'; view.arg = {mode: 'print', fresh: true}; render(); }}),
      el('button', {class: 'btn light', text: '📝 آزمون‌های آنلاین', onclick: async function () { var M = window.SiteMobile; if (!M || !M.printOnlineSheet) { view.panel = 'exams'; view.arg = null; return render(); } var list = []; try { list = await window.SiteBuilder.printExamsList(); } catch (e) {} M.printOnlineSheet(list, status); }})
    ]));
    c.appendChild(status);
    if (window.SiteBuilder) c.appendChild(window.SiteBuilder.printExamsSection(function () { pagePrint(c); }, {noNewButton: true}));
  }

  /* V170 — «حساب» و «تنظیمات» دسکتاپ = دقیقاً همان صفحه‌های گوشی/اپ (ProfileSettingsScreen با مقصد ACCOUNT / SETTINGS) */
  function pageAccount(c) { var M = window.SiteMobile; if (!M || !M.profileScreen) return pageProfile(c); c.innerHTML = ''; var w = el('div', {class: 'dk-mwrap m-mode'}); c.appendChild(w); if (view.arg && view.arg.tab) { M.setProfileTab(view.arg.tab); view.arg = null; } M.profileScreen(w); }
  function pageSettings(c) { var M = window.SiteMobile; if (!M || !M.settingsScreen) return pageTools(c); c.innerHTML = ''; var w = el('div', {class: 'dk-mwrap m-mode'}); c.appendChild(w); M.settingsScreen(w); }

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
      /* V251 — خطای نمرات به کاربر گفته می‌شود (نه «هنوز نمره‌ای ثبت نشده»)؛ پاسخ‌ها اختیاری می‌مانند */
      var r = await Promise.all([api.myGrades(), api.myAnswers().catch(function () { return []; })]);
      var grades = mergeAnswerMax(r[0] || [], r[1] || []), answers = r[1] || [];
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
    /* V237 — بازگشت از گوگل با حساب کاملِ موجود: اگر نقش با پنل انتخابی فرق دارد یا در حال «ثبت‌نام» بود،
       پنجرهٔ «ورود به‌عنوان …» / «انصراف» (انصراف = خروج از نشست گوگل و ماندن در صفحهٔ ورود) */
    var gr = window.__googleReturn; window.__googleReturn = null;
    if (gr && user && !user.requiresSetup && (user.role === 'teacher' || user.role === 'manager')) {
      var actualName = user.role === 'manager' ? 'مدیر/معاون' : 'معلم';
      var ask = user.role !== gr.role ? 'این ایمیل قبلاً به‌عنوان «' + actualName + '» ثبت‌نام شده است.' : (gr.mode === 'register' ? 'این ایمیل قبلاً به‌عنوان «' + actualName + '» ثبت‌نام شده است و نیازی به ثبت‌نام دوباره نیست.' : '');
      if (ask) {
        var pendingUser = user; user = null; render(); /* پشت پنجره، صفحهٔ ورود */
        var go = await confirmDlg('حساب از قبل وجود دارد', ask, pendingUser.role === 'manager' ? 'ورود به‌عنوان مدیر' : 'ورود به‌عنوان معلم');
        if (!go) { try { await http('/auth/v1/logout', {method: 'POST'}); } catch (e) {} saveSession(null); user = null; render(); return; }
        user = pendingUser;
      }
    }
    if (user && user.requiresSetup) { renderSetupGate(); return; }
    render();
    if (user) prefetchEngines();
  }
  /* V144 — بارگذاری رسانه: اول R2 (لینک موقت از Edge Function media-upload)، در نبود پیکربندی → Supabase Storage */
  var MEDIA_BUCKET = 'exam-images', r2Disabled = false;
  /* V221 — حذف فایل‌های آپلودشده از فضای ابری وقتی کاربر آن‌ها را حذف می‌کند (آینهٔ StorageImageCleaner اپ):
     آدرس‌های باکت سوپابیس و S3/R2 با مسیر <پوشه>/<کاربر>/… به تابع لبهٔ media-upload (action=delete) می‌روند؛
     اگر تابع در دسترس نبود، آدرس‌های سوپابیس مستقیم با توکن کاربر حذف می‌شوند (policy مالک). best-effort. */
  var OWNED_MEDIA_RE = /^https:\/\/[^\s"']+\/(questions|option_images|matching_images|audio|answers|profiles)\/[0-9a-fA-F-]{36}\/[^\s"']+$/;
  function isOwnedMediaUrl(u) { if (typeof u !== 'string') return false; u = u.split('?')[0]; return isOwnStorageUrl(u) || OWNED_MEDIA_RE.test(u); }
  function mediaUrlsIn(value) {
    var out = {}; (function walk(v) { if (typeof v === 'string') { if (isOwnedMediaUrl(v)) out[v.split('?')[0]] = 1; else if (v.indexOf('https://') >= 0) (v.match(/https:\/\/[^"'\s,)\]\\]+/g) || []).forEach(function (u) { if (isOwnedMediaUrl(u)) out[u.split('?')[0]] = 1; }); }
      else if (Array.isArray(v)) v.forEach(walk); else if (v && typeof v === 'object') Object.keys(v).forEach(function (k) { walk(v[k]); }); })(value);
    return Object.keys(out);
  }
  async function deleteMedia(urls) {
    urls = (urls || []).filter(isOwnedMediaUrl).map(function (u) { return u.split('?')[0]; }).filter(function (u, i, a) { return a.indexOf(u) === i; });
    if (!urls.length || !session) return 0;
    var done = 0;
    for (var i = 0; i < urls.length; i += 50) {
      var chunk = urls.slice(i, i + 50);
      try { var r = await http('/functions/v1/media-upload', {method: 'POST', body: {action: 'delete', urls: chunk}}); if (r && typeof r.deleted === 'number') { done += r.deleted; continue; } } catch (e) { console.warn('media-delete', e); }
      /* بازگشت: فقط آدرس‌های سوپابیس، مستقیم */
      for (var j = 0; j < chunk.length; j++) { var u = chunk[j]; if (!isOwnStorageUrl(u)) continue; var m = u.indexOf('/storage/v1/object/public/'); var rel = u.slice(m + '/storage/v1/object/public/'.length);
        try { var res = await fetch(SUPABASE_URL + '/storage/v1/object/' + rel, {method: 'DELETE', headers: {'apikey': ANON, 'Authorization': 'Bearer ' + session.access_token}}); if (res.ok || res.status === 404) done++; } catch (e2) {} }
    }
    return done;
  }
  async function uploadMedia(blob, kind, folder, examId, ext, contentType) {
    if (!r2Disabled) {
      try {
        var t = await http('/functions/v1/media-upload', {method: 'POST', body: {kind: kind, folder: folder, exam_id: examId, ext: ext, size: blob.size, acl: 'public-read'}});
        if (t && t.upload_url) {
          /* نوع محتوا باید دقیقاً همان مقدار امضاشدهٔ سرور باشد (نه blob.type که ممکن است ;codecs=… داشته باشد) */
          var ct = (t.headers && t.headers['Content-Type']) || contentType;
          /* V160.2 — همهٔ هدرهای امضاشدهٔ سرور (Content-Type + x-amz-acl: public-read) عیناً ارسال می‌شوند */
          var hdrs = Object.assign({}, t.headers || {}, {'Content-Type': ct});
          var put = await fetch(t.upload_url, {method: 'PUT', headers: hdrs, body: blob.type === ct ? blob : new Blob([blob], {type: ct})});
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
  window.ExamSite = {headerFingerprint: headerFingerprint, printHeaderFp: printHeaderFp, ensurePrintPaid: ensurePrintPaid, jalaliPicker: jalaliPicker, jalaliDisplay: jalaliDisplay, openFormulaEditor: openFormulaEditor, openHeaderSettings: openHeaderSettings, headerSettingsForm: headerSettingsForm, readPrintHeader: readPrintHeader, faReason: faReason, uploadMedia: uploadMedia, openPrintPreview: openPrintPreview, buildPrintPayload: buildPrintPayload, api: api, demoPrint: demoPrint,
    el: el, esc: esc, fa: fa, en: en, toast: toast, confirmDlg: confirmDlg, infoDlg: infoDlg, costDoneDlg: costDoneDlg, promptDlg: promptDlg, mediaBlobUrl: mediaBlobUrl, isOwnStorageUrl: isOwnStorageUrl, deleteMedia: deleteMedia, mediaUrlsIn: mediaUrlsIn, isOwnedMediaUrl: isOwnedMediaUrl, rpc: rpc, rpcObj: rpcObj, select: select, http: http, uuid: uuid, fmtScore: fmtScore, fmtDate: fmtDate, money: money, errMsg: errMsg,
    localState: localState, setLocalState: setLocalState, loading: loading, showErr: showErr, emptyBox: emptyBox, qType: qType, engineHtml: engineHtml, loadEngines: loadEngines,
    user: function () { return user; }, session: function () { return session; }, config: {url: SUPABASE_URL, anon: ANON},
    go: function (panel, arg) { view.panel = panel; view.arg = arg; render(); }, navBack: navBack, view: view, examActions: examActions,
    render: render, renderPage: renderPage, printExam: printExam, logout: doLogout, printSection: printSection, /* V230 */
    __setSession: function (s) { saveSession(s); }, __setUser: function (u) { user = u; view.page = 'panel'; render(); },
    auth: {keyReady: KEY_READY, login: function (u) { user = u; afterLogin(); }, currentProfile: currentProfile, requireEmail: requireEmail, drawCompletion: drawCompletion, logout: doLogout} /* برای تست خودکار بدون سرور */};
  /* V234 — کلیک روی هر کد آزمون (.code) در سراسر سایت = کپی در حافظهٔ موقت */
  document.addEventListener('click', function (e) {
    var t = e.target && e.target.closest ? e.target.closest('.code') : null;
    if (!t || t.closest('input,textarea,button,a')) return;
    var code = (t.textContent || '').trim(); if (!code || code === '—') return;
    var done = function () { toast('کد آزمون ' + code + ' کپی شد.', 'ok'); };
    if (navigator.clipboard && navigator.clipboard.writeText) navigator.clipboard.writeText(code).then(done, function () { legacyCopy(code); done(); }); else { legacyCopy(code); done(); }
  });
  function legacyCopy(text) { var ta = el('textarea', {style: 'position:fixed;opacity:0', value: text}); document.body.appendChild(ta); ta.select(); try { document.execCommand('copy'); } catch (x) {} ta.remove(); }
  /* V240 — منوهای کشویی دسکتاپ: فهرست بازشو هم کروی/تراشه‌ای. <select> بومی پنهان می‌ماند (مقدار، change، disabled همان است)
     و یک دکمهٔ گرد + فهرست چیپ‌ها جایش را می‌گیرد. پوستهٔ موبایل (m-mode) و select چندانتخابی دست‌نخورده. */
  var ddOpen = null;
  function ddClose() { if (ddOpen) { ddOpen.menu.remove(); ddOpen.btn.classList.remove('open'); ddOpen = null; } }
  function ddEnhance(s) {
    if (s.dataset.dd || s.multiple || s.size > 1 || document.body.classList.contains('m-mode')) return;
    s.dataset.dd = '1';
    var wrap = el('span', {class: 'dd-wrap'}), btn = el('button', {type: 'button', class: 'dd-btn', 'aria-haspopup': 'listbox'});
    s.parentNode.insertBefore(wrap, s); wrap.appendChild(s); wrap.appendChild(btn); s.classList.add('dd-native');
    function label() { var o = s.options[s.selectedIndex]; btn.textContent = o ? o.text : '\u00a0'; btn.disabled = s.disabled; btn.classList.toggle('empty', !o || !o.value); }
    label();
    s.addEventListener('change', label);
    new MutationObserver(label).observe(s, {childList: true, subtree: true, attributes: true});
    btn.addEventListener('mouseenter', label);
    btn.addEventListener('click', function (e) {
      e.stopPropagation(); label();
      if (ddOpen && ddOpen.s === s) { ddClose(); return; }
      ddClose();
      var menu = el('div', {class: 'dd-menu', role: 'listbox'});
      Array.prototype.forEach.call(s.options, function (o, i) {
        if (o.hidden) return;
        var it = el('button', {type: 'button', class: 'dd-item' + (i === s.selectedIndex ? ' on' : ''), text: o.text, role: 'option'});
        it.disabled = o.disabled;
        it.addEventListener('click', function (ev) { ev.stopPropagation(); s.selectedIndex = i; s.dispatchEvent(new Event('change', {bubbles: true})); label(); ddClose(); });
        menu.appendChild(it);
      });
      document.body.appendChild(menu);
      var r = btn.getBoundingClientRect(), below = window.innerHeight - r.bottom - 12, above = r.top - 12;
      var maxH = Math.min(360, Math.max(below, above)); /* V241 — ارتفاع بر اساس جای واقعی؛ بقیه با اسکرول */
      menu.style.minWidth = Math.max(r.width, 140) + 'px'; menu.style.maxHeight = maxH + 'px';
      var mh = Math.min(menu.scrollHeight, maxH);
      menu.style.left = Math.max(8, Math.min(r.left, window.innerWidth - Math.max(r.width, 160) - 8)) + 'px';
      if (below < mh && above > below) { menu.style.top = Math.max(8, r.top - mh - 6) + 'px'; menu.classList.add('up'); } else menu.style.top = (r.bottom + 6) + 'px';
      menu.addEventListener('wheel', function (ev) { ev.stopPropagation(); if (menu.scrollHeight <= menu.clientHeight) ev.preventDefault(); }, {passive: false}); /* V242 — چرخ روی فهرست، صفحهٔ پشت را نمی‌لغزاند (و فهرست بسته نمی‌شود) */
      btn.classList.add('open'); ddOpen = {s: s, btn: btn, menu: menu};
      var on = menu.querySelector('.dd-item.on'); if (on) on.scrollIntoView({block: 'nearest'});
    });
  }
  function ddEnhanceAll(root) { if (document.body.classList.contains('m-mode')) return; (root || document).querySelectorAll('select').forEach(ddEnhance); }
  document.addEventListener('click', function (e) { if (ddOpen && !ddOpen.menu.contains(e.target)) ddClose(); });
  document.addEventListener('keydown', function (e) { if (e.key === 'Escape') ddClose(); });
  window.addEventListener('resize', ddClose); window.addEventListener('scroll', function (e) { if (ddOpen && ddOpen.menu.contains(e.target)) return; ddClose(); }, true); /* V241 — اسکرول داخل فهرست آن را نمی‌بندد */
  var ddTimer = null;
  new MutationObserver(function () { if (ddTimer) return; ddTimer = setTimeout(function () { ddTimer = null; ddEnhanceAll(); }, 30); }).observe(document.documentElement, {childList: true, subtree: true});
  /* V250 — تور ایمنی: خطای پیش‌بینی‌نشده (Promise بدون catch یا استثنای همگام) به‌جای سکوت، یک پیام کوتاه فارسی؛ حداکثر یکی در ۴ ثانیه */
  var lastErrToast = 0;
  function softErr(e) { var now = Date.now(); if (now - lastErrToast < 4000) return; lastErrToast = now; try { toast(errMsg(e), 'err'); } catch (x) {} }
  window.addEventListener('unhandledrejection', function (ev) { softErr(ev && ev.reason); });
  window.addEventListener('error', function (ev) { if (ev && ev.error) softErr(ev.error); });
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot); else boot();
})();
