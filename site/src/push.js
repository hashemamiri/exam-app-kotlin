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
    if (id && id !== lastUser) { lastUser = id; sync(false); setTimeout(offer, 2500); }
    if (!id) lastUser = null;
    if (id && pendingNav) { var n = pendingNav; pendingNav = null; navigate(n.page, n.extra); }
  }, 2000);

  window.SitePush = {available: available, isEnabled: function () { return isEnabled() && Notification.permission === 'granted' && !!ls('push.token'); }, enable: enable, disable: disable, sync: sync, navigate: navigate};
})();
