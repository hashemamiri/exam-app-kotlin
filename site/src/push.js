/* ===================================================================
   V232.9 — اعلان‌های وب (PWA/مرورگر) بدون کتابخانهٔ فایربیس:
   اشتراک Web Push مرورگر → ثبت در FCM (Installations + Registrations API با
   همان درخواست‌هایی که SDK رسمی می‌فرستد) → ثبت توکن با RPC native_push_register_v1
   (platform = web). پیام‌ها را Service Worker (pwa/sw.js) نشان می‌دهد و ضربه
   روی اعلان با ?page=… یا پیام push-nav به صفحهٔ مقصد می‌رود.
   پیکربندی (apiKey/projectId/appId عمومی فایربیس + کلید VAPID) در CI از Secrets
   به index.html تزریق می‌شود؛ بدون آن این ماژول خاموش است.
   =================================================================== */
(function () {
  var S = window.ExamSite;
  if (!S) return;
  var el = S.el, toast = S.toast;
  var CFG = window.SITE_CONFIG || {};
  var FB = CFG.FIREBASE_WEB && typeof CFG.FIREBASE_WEB === 'object' ? CFG.FIREBASE_WEB : null;
  var VAPID = String(CFG.FIREBASE_VAPID_KEY || '').trim();
  var FIS_URL = 'https://firebaseinstallations.googleapis.com/v1';
  var FCM_URL = 'https://fcmregistrations.googleapis.com/v1';
  var SDK = 'w:0.6.18';
  function available() {
    return !!(FB && FB.apiKey && FB.projectId && FB.appId && VAPID && location.protocol === 'https:' &&
      'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window);
  }
  function ls(k, v) { try { if (v === undefined) return localStorage.getItem(k); if (v === null) localStorage.removeItem(k); else localStorage.setItem(k, v); } catch (e) { return null; } }
  function isEnabled() { return ls('push.enabled') !== '0'; }
  function b64url(buf) { var s = btoa(String.fromCharCode.apply(null, new Uint8Array(buf))); return s.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, ''); }
  function vapidBytes(key) { var p = '='.repeat((4 - key.length % 4) % 4); var b = atob((key + p).replace(/-/g, '+').replace(/_/g, '/')); var out = new Uint8Array(b.length); for (var i = 0; i < b.length; i++) out[i] = b.charCodeAt(i); return out; }
  function genFid() { var a = new Uint8Array(17); crypto.getRandomValues(a); a[0] = 0x70 + (a[0] % 16); return b64url(a.buffer).substr(0, 22); }
  function headers(extra) { var h = {'Content-Type': 'application/json', Accept: 'application/json', 'x-goog-api-key': FB.apiKey}; if (extra) for (var k in extra) h[k] = extra[k]; return h; }
  async function jsonOrThrow(r) { var j = null; try { j = await r.json(); } catch (e) {} if (!r.ok) { var err = new Error((j && j.error && j.error.message) || ('HTTP ' + r.status)); err.status = r.status; throw err; } return j || {}; }

  /* --- Firebase Installations: fid + refreshToken در localStorage؛ authToken هفت‌روزه --- */
  async function createInstallation() {
    var fid = genFid();
    var j = await jsonOrThrow(await fetch(FIS_URL + '/projects/' + FB.projectId + '/installations', {method: 'POST', headers: headers(), body: JSON.stringify({fid: fid, authVersion: 'FIS_v2', appId: FB.appId, sdkVersion: SDK})}));
    var inst = {fid: j.fid || fid, refreshToken: j.refreshToken, token: j.authToken && j.authToken.token, exp: Date.now() + expMs(j.authToken && j.authToken.expiresIn)};
    ls('push.fis', JSON.stringify(inst)); return inst;
  }
  function expMs(s) { var n = parseInt(String(s || '604800s'), 10); return (isFinite(n) ? n : 604800) * 1000; }
  async function installationToken() {
    var inst = null; try { inst = JSON.parse(ls('push.fis') || 'null'); } catch (e) {}
    if (inst && inst.token && inst.exp - Date.now() > 3600 * 1000) return inst.token;
    if (inst && inst.fid && inst.refreshToken) {
      try {
        var j = await jsonOrThrow(await fetch(FIS_URL + '/projects/' + FB.projectId + '/installations/' + inst.fid + '/authTokens:generate', {method: 'POST', headers: headers({Authorization: 'FIS_v2 ' + inst.refreshToken}), body: JSON.stringify({installation: {sdkVersion: SDK, appId: FB.appId}})}));
        inst.token = j.token; inst.exp = Date.now() + expMs(j.expiresIn); ls('push.fis', JSON.stringify(inst)); return inst.token;
      } catch (e) { /* نصب قدیمی نامعتبر → نصب تازه */ }
    }
    return (await createInstallation()).token;
  }

  /* --- اشتراک Web Push + ثبت در FCM → توکن --- */
  async function subscription(reg) {
    var sub = await reg.pushManager.getSubscription();
    if (!sub) sub = await reg.pushManager.subscribe({userVisibleOnly: true, applicationServerKey: vapidBytes(VAPID)});
    return sub;
  }
  async function fcmToken(reg) {
    var sub = await subscription(reg);
    var cached = null; try { cached = JSON.parse(ls('push.fcm') || 'null'); } catch (e) {}
    if (cached && cached.token && cached.endpoint === sub.endpoint && Date.now() - (cached.at || 0) < 7 * 86400 * 1000) return cached.token;
    var fis = await installationToken();
    var body = {web: {origin: location.host, endpoint: sub.endpoint, p256dh: b64url(sub.getKey('p256dh')), auth: b64url(sub.getKey('auth')), applicationPubKey: VAPID}};
    var j = await jsonOrThrow(await fetch(FCM_URL + '/projects/' + FB.projectId + '/registrations', {method: 'POST', headers: headers({'x-goog-firebase-installations-auth': 'FIS ' + fis}), body: JSON.stringify(body)}));
    if (!j.token) throw new Error('توکن اعلان دریافت نشد.');
    ls('push.fcm', JSON.stringify({token: j.token, endpoint: sub.endpoint, at: Date.now()}));
    return j.token;
  }

  /* --- همگام‌سازی با سرور (مثل PushRegistrar اپ) --- */
  var syncing = false;
  async function sync(force) {
    if (!available() || !isEnabled() || Notification.permission !== 'granted' || syncing) return;
    var u = S.user(); if (!u || !u.id) return;
    syncing = true;
    try {
      var reg = await navigator.serviceWorker.ready;
      var token = await fcmToken(reg);
      if (!force && ls('push.token') === token && ls('push.user') === u.id && Date.now() - (+ls('push.at') || 0) < 86400 * 1000) return;
      var r = await S.rpcObj('native_push_register_v1', {p_token: token, p_platform: 'web'});
      if (r && r.error) throw new Error(String(r.error));
      ls('push.token', token); ls('push.user', u.id); ls('push.at', String(Date.now()));
    } catch (e) { console.warn('push sync', e); } finally { syncing = false; }
  }
  async function enable() {
    if (!available()) { toast('اعلان در این مرورگر در دسترس نیست.', 'err'); return false; }
    var p = Notification.permission;
    if (p !== 'granted') { try { p = await Notification.requestPermission(); } catch (e) { p = 'denied'; } }
    if (p !== 'granted') { toast('اجازهٔ اعلان داده نشد. از تنظیمات مرورگر می‌توانید آن را فعال کنید.', 'err'); return false; }
    ls('push.enabled', '1'); ls('push.asked', '1');
    await sync(true);
    if (ls('push.token')) { toast('اعلان‌ها فعال شد.', 'ok'); return true; }
    toast('فعال‌سازی اعلان انجام نشد؛ بعداً دوباره تلاش کنید.', 'err'); return false;
  }
  async function disable() {
    ls('push.enabled', '0');
    var t = ls('push.token');
    if (t) { try { await S.rpcObj('native_push_unregister_v1', {p_token: t}); } catch (e) {} }
    ls('push.token', null); ls('push.user', null); ls('push.at', null);
    toast('اعلان‌ها خاموش شد.', 'ok');
  }

  /* --- کارت پیشنهاد پس از ورود (یک بار) --- */
  function offer() {
    if (!available() || Notification.permission !== 'default' || ls('push.asked') || document.getElementById('push-offer')) return;
    var bar = el('div', {id: 'push-offer', class: 'card', style: 'position:fixed;bottom:16px;right:16px;left:16px;max-width:460px;margin:0 auto;z-index:60;display:flex;gap:10px;align-items:center;padding:12px 14px'}, [
      el('span', {class: 'grow', text: '🔔 اعلان نمره، باز شدن آزمون و پیام‌های تقویم را دریافت کنید؟'}),
      el('button', {class: 'btn sm', text: 'بله', onclick: function () { bar.remove(); enable(); }}),
      el('button', {class: 'btn light sm', text: 'بعداً', onclick: function () { ls('push.asked', '1'); bar.remove(); }})
    ]);
    document.body.appendChild(bar);
  }

  /* --- مقصد اعلان: ?page=&exam_id=&code= یا پیام SW --- */
  var pendingNav = null;
  function panelFor(page, u, extra) {
    var role = u.role;
    if (page === 'grades') return role === 'student' ? ['grades'] : (role === 'teacher' ? ['grading'] : null);
    if (page === 'grading') return role === 'teacher' ? ['grading'] : null;
    if (page === 'exam') return role === 'student' ? ['join', extra.code ? {code: extra.code} : null] : (role === 'teacher' ? ['dashboard'] : null);
    if (page === 'requests') return role === 'teacher' ? ['requests'] : (role === 'manager' ? ['dashboard'] : null);
    if (page === 'students') return role === 'teacher' ? ['students'] : null; /* V282 */
    if (page === 'calendar') return ['calendar'];
    return null;
  }
  function navigate(page, extra) {
    var u = S.user(); if (!u) { pendingNav = {page: page, extra: extra || {}}; return; }
    var t = panelFor(page, u, extra || {}); if (!t) return;
    S.go(t[0], t[1] || null);
  }
  (function readQuery() {
    try {
      var q = new URLSearchParams(location.search); var page = q.get('page'); if (!page) return;
      pendingNav = {page: page, extra: {exam_id: q.get('exam_id') || '', code: q.get('code') || ''}};
      q.delete('page'); q.delete('exam_id'); q.delete('code');
      history.replaceState(null, '', location.pathname + (q.toString() ? '?' + q.toString() : '') + location.hash);
    } catch (e) {}
  })();
  if ('serviceWorker' in navigator) navigator.serviceWorker.addEventListener('message', function (e) { var d = e.data || {}; if (d.type === 'push-nav') navigate(d.page, {exam_id: d.exam_id, code: d.code}); });

  /* --- ناظر ورود: هر ۲ ثانیه؛ با ورود کاربر → همگام‌سازی، پیشنهاد، مقصد معلق --- */
  var lastUser = null;
  setInterval(function () {
    var u = S.user(); var id = u && u.id || null;
    if (id && id !== lastUser) { lastUser = id; sync(false); setTimeout(offer, 2500); setTimeout(ncRefresh, 800); }
    if (!id && document.getElementById('nc-bell')) document.getElementById('nc-bell').remove();
    if (!id) lastUser = null;
    if (id && pendingNav) { var n = pendingNav; pendingNav = null; navigate(n.page, n.extra); }
  }, 2000);

  /* --- V284 — مرکز اعلان: زنگولهٔ شناور با شمارندهٔ خوانده‌نشده؛ فهرست اعلان‌های گذشته (push_outbox) --- */
  var ncUnread = 0, ncTimer = null;
  var NC_CATS = [['آزمون‌ها', ['exam_open', 'exam_soon', 'exam_closing', 'submit']], ['نمره‌ها', ['grade']], ['تقویم', ['calendar', 'calendar_reminder']], ['کلاس و درخواست‌ها', ['class_added', 'manager_request', 'request_decided', 'student_delete_request']]];
  function ncCat(kind) { for (var i = 0; i < NC_CATS.length; i++) if (NC_CATS[i][1].indexOf(kind) >= 0) return NC_CATS[i][0]; return 'سایر'; }
  function ncBell() {
    var b = document.getElementById('nc-bell');
    if (!b) {
      b = el('button', {id: 'nc-bell', class: 'nc-bell', 'aria-label': 'اعلان‌ها', title: 'اعلان‌ها', onclick: ncOpen}, [
        el('span', {class: 'nc-ic', html: '<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.7 21a2 2 0 0 1-3.4 0"/></svg>'}),
        el('span', {class: 'nc-badge', id: 'nc-badge'})
      ]);
      document.body.appendChild(b);
    }
    /* V284.4 — دسکتاپ: زنگوله داخل ریل (آخرین خانه) تا با محتوا هم‌پوشانی نکند؛ گوشی: شناور */
    var rail = document.body.classList.contains('dk') ? document.querySelector('.dk-rail') : null;
    if (rail) {
      if (b.parentNode !== rail) { b.classList.add('dk-rail-item', 'nc-rail'); rail.appendChild(b); }
      if (!rail.dataset.ncN) { rail.dataset.ncN = '1'; var n = parseInt((rail.style.getPropertyValue('--n') || '13'), 10); rail.style.setProperty('--n', String((isFinite(n) ? n : 13) + 1)); }
    } else if (b.parentNode !== document.body) { b.classList.remove('dk-rail-item', 'nc-rail'); document.body.appendChild(b); }
    ncPaintBadge();
    return b;
  }
  function ncPaintBadge() {
    var b = document.getElementById('nc-badge'); if (!b) return;
    b.textContent = ncUnread > 0 ? S.fa(Math.min(ncUnread, 99)) : '';
    b.style.display = ncUnread > 0 ? '' : 'none';
  }
  async function ncRefresh() {
    var u = S.user(); if (!u || !u.id) { var old = document.getElementById('nc-bell'); if (old) old.remove(); return null; }
    var inExam = window.SiteStudent && window.SiteStudent.inExam && window.SiteStudent.inExam();
    var bell = ncBell(); bell.style.display = inExam ? 'none' : '';
    try { var r = await S.rpc('native_my_notifications_v284', {p_limit: 50}); ncUnread = (r && r.unread) || 0; ncPaintBadge(); return r; }
    catch (e) { return null; }
  }
  function ncWhen(iso) { try { return S.jalaliDisplay(new Date(iso), true); } catch (e) { return ''; } }
  async function ncOpen() {
    if (document.getElementById('nc-modal')) return;
    var bg = el('div', {class: 'modal-bg', id: 'nc-modal', onclick: function (e) { if (e.target === bg) bg.remove(); }});
    var tabsBox = el('div', {class: 'nc-tabs'});
    var list = el('div', {class: 'nc-list'}, [el('p', {class: 'muted', text: 'در حال دریافت…'})]);
    var allBtn = el('button', {class: 'btn light', text: 'همه خوانده شد', style: 'display:none', onclick: async function () {
      try { await S.rpc('native_notifications_read_v284', {}); items.forEach(function (n) { n.read_at = n.read_at || new Date().toISOString(); }); ncUnread = 0; ncPaintBadge(); draw(); } catch (e) { toast(S.errMsg(e), 'err'); }
    }});
    var m = el('div', {class: 'modal'}, [
      el('h2', {text: 'اعلان‌ها'}), tabsBox, list,
      el('div', {class: 'row', style: 'justify-content:flex-start;margin-top:14px;gap:8px'}, [el('button', {class: 'btn light', text: 'بستن', onclick: function () { bg.remove(); }}), allBtn])
    ]);
    bg.appendChild(m); document.body.appendChild(bg);
    var r = await ncRefresh();
    var items = (r && r.items) || [];
    var tab = 'همه';
    function unreadIn(c) { return items.filter(function (n) { return !n.read_at && (c === 'همه' || ncCat(n.kind) === c); }).length; }
    function draw() {
      /* V284.4 — دسته‌بندی: تب‌ها فقط برای دسته‌هایی که اعلان دارند */
      var cats = ['همه'].concat(NC_CATS.map(function (c) { return c[0]; }).concat(['سایر']).filter(function (c) { return items.some(function (n) { return ncCat(n.kind) === c; }); }));
      tabsBox.innerHTML = '';
      if (cats.length > 2) cats.forEach(function (c) {
        var u = unreadIn(c);
        tabsBox.appendChild(el('button', {class: 'nc-tab' + (tab === c ? ' on' : ''), text: u ? c + ' (' + S.fa(u) + ')' : c, onclick: function () { tab = c; draw(); }}));
      });
      allBtn.style.display = unreadIn('همه') ? '' : 'none';
      list.innerHTML = '';
      var shown = items.filter(function (n) { return tab === 'همه' || ncCat(n.kind) === tab; });
      if (!shown.length) { list.appendChild(el('p', {class: 'muted', text: r ? 'هنوز اعلانی ندارید.' : 'دریافت اعلان‌ها ممکن نشد.'})); return; }
      shown.forEach(function (n) {
        var d = n.data || {};
        list.appendChild(el('button', {class: 'nc-item' + (n.read_at ? '' : ' unread'), onclick: function () {
          /* فقط همین اعلان خوانده‌شده می‌شود */
          if (!n.read_at) { n.read_at = new Date().toISOString(); ncUnread = Math.max(0, ncUnread - 1); ncPaintBadge(); S.rpc('native_notifications_read_v284', {p_ids: [n.id]}).catch(function () {}); }
          bg.remove(); navigate(d.page, {exam_id: d.exam_id, code: d.code});
        }}, [
          el('div', {class: 'nc-row'}, [el('b', {text: n.title}), el('small', {text: ncWhen(n.created_at)})]),
          el('div', {class: 'nc-body', text: n.body})
        ]));
      });
    }
    draw();
  }
  ncTimer = setInterval(ncRefresh, 90000);
  setTimeout(ncRefresh, 1500);

  window.SitePush = {available: available, notifications: {refresh: ncRefresh, open: ncOpen}, isEnabled: function () { return isEnabled() && Notification.permission === 'granted' && !!ls('push.token'); }, enable: enable, disable: disable, sync: sync, navigate: navigate};
})();
