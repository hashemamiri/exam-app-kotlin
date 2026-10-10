/* V147 — Service Worker آزمون‌ساز: پوستهٔ سایت آفلاین/فوری، دادهٔ Supabase همیشه از شبکه.
   نسخه در build (CI) با هش index.html جایگزین می‌شود تا هر انتشار، کش قدیمی را دور بریزد. */
var VERSION = '__SW_VERSION__';
var CACHE = 'azmoon-shell-' + VERSION;
var SHELL = ['/', '/pwa/manifest.webmanifest', '/pwa/v3/icon-192.png', '/pwa/v3/icon-512.png']; /* V253 */
self.addEventListener('install', function (e) {
  e.waitUntil(caches.open(CACHE).then(function (c) { return c.addAll(SHELL); }).then(function () { return self.skipWaiting(); }));
});
self.addEventListener('activate', function (e) {
  e.waitUntil(caches.keys().then(function (keys) {
    return Promise.all(keys.filter(function (k) { return k.indexOf('azmoon-shell-') === 0 && k !== CACHE; }).map(function (k) { return caches.delete(k); }));
  }).then(function () { return self.clients.claim(); }));
});
self.addEventListener('message', function (e) { if (e.data === 'skipWaiting') self.skipWaiting(); });
/* V238 — وقتی شبکه قطع/ریست می‌شود و پوسته هنوز کش نشده (مرورگر تازه): به‌جای «Content unavailable. Resource was not cached»
   یک صفحهٔ فارسی با دکمهٔ «تلاش دوباره» */
function offlinePage() {
  var html = '<!DOCTYPE html><html lang="fa" dir="rtl"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>آزمون آنلاین</title>' +
    '<style>body{margin:0;font-family:Tahoma,sans-serif;background:#D6ECFC;color:#1F3B57;display:flex;align-items:center;justify-content:center;min-height:100vh}.c{background:#fff;border-radius:22px;padding:28px 26px;max-width:420px;text-align:center;box-shadow:0 20px 60px rgba(60,120,170,.18)}h1{font-size:20px;margin:0 0 10px}p{font-size:14px;line-height:1.9;margin:0 0 16px}button{font:inherit;background:linear-gradient(135deg,#7CC0EE,#5FAEE6);color:#fff;border:0;border-radius:12px;padding:10px 22px;cursor:pointer}</style></head>' +
    '<body><div class="c"><h1>اتصال به سایت برقرار نشد</h1><p>اینترنت قطع است یا اتصال در میانهٔ راه بسته شد. چند لحظه بعد دوباره تلاش کنید؛ اگر ادامه داشت، شبکهٔ دیگری (مثلاً اینترنت گوشی) را امتحان کنید.</p><button onclick="location.reload()">تلاش دوباره</button></div></body></html>';
  return new Response(html, {status: 503, headers: {'Content-Type': 'text/html; charset=utf-8', 'Cache-Control': 'no-store'}});
}
/* V232.9 — اعلان وب: پیام data-only از FCM → نمایش؛ ضربه → تمرکز روی پنجرهٔ باز (پیام push-nav) یا باز کردن /?page=… */
self.addEventListener('push', function (e) {
  var p = {}; try { p = e.data ? e.data.json() : {}; } catch (x) { p = {}; }
  var d = p.data || p; var n = p.notification || {};
  var title = d.title || n.title || 'آزمون آنلاین'; var body = d.body || n.body || '';
  var data = {page: d.page || '', exam_id: d.exam_id || '', code: d.code || ''};
  e.waitUntil(self.registration.showNotification(title, {body: body, icon: '/pwa/v3/icon-192.png', badge: '/pwa/v3/icon-192.png', dir: 'rtl', lang: 'fa', tag: d.outbox_id ? 'ox-' + d.outbox_id : undefined, data: data}));
});
self.addEventListener('notificationclick', function (e) {
  e.notification.close();
  var d = e.notification.data || {};
  var q = d.page ? '?page=' + encodeURIComponent(d.page) + (d.exam_id ? '&exam_id=' + encodeURIComponent(d.exam_id) : '') + (d.code ? '&code=' + encodeURIComponent(d.code) : '') : '';
  e.waitUntil(self.clients.matchAll({type: 'window', includeUncontrolled: true}).then(function (list) {
    for (var i = 0; i < list.length; i++) {
      var c = list[i];
      if (new URL(c.url).origin === self.location.origin) { c.postMessage({type: 'push-nav', page: d.page, exam_id: d.exam_id, code: d.code}); return c.focus(); }
    }
    return self.clients.openWindow('/' + q);
  }));
});
self.addEventListener('fetch', function (e) {
  var req = e.request;
  if (req.method !== 'GET') return;
  var url = new URL(req.url);
  if (url.origin !== self.location.origin) return; /* Supabase، آروان، فونت‌ها: مستقیم شبکه */
  if (req.mode === 'navigate' || url.pathname === '/' || url.pathname === '/index.html') {
    /* شبکه اول (تا به‌روزرسانی فوری دیده شود)، در نبود شبکه پوستهٔ کش‌شده */
    e.respondWith(fetch(req).then(function (r) {
      if (r.ok) { var copy = r.clone(); caches.open(CACHE).then(function (c) { c.put('/', copy); }); }
      return r;
    }).catch(function () { return caches.match('/').then(function (c) { return c || offlinePage(); }); }));
    return;
  }
  /* V162 — engines.<hash>.js: کش-اول (نام با هش عوض می‌شود)؛ نسخه‌های قدیمی موتور پاک می‌شوند */
  if (/^\/engines\.[0-9a-f]+\.js$/.test(url.pathname)) {
    e.respondWith(caches.match(req).then(function (r) { return r || fetch(req).then(function (n) { if (n.ok) { var copy = n.clone(); caches.open(CACHE).then(function (c) { c.keys().then(function (ks) { ks.forEach(function (k) { if (/\/engines\./.test(k.url) && k.url !== req.url) c.delete(k); }); }); c.put(req, copy); }); } return n; }); }));
    return;
  }
  /* V164 — قلم‌ها و پوشهٔ pwa: کش-اول */
  if (url.pathname.indexOf('/pwa/') === 0 || url.pathname.indexOf('/fonts/') === 0) {
    e.respondWith(caches.match(req).then(function (r) { return r || fetch(req).then(function (n) { if (n.ok) { var copy = n.clone(); caches.open(CACHE).then(function (c) { c.put(req, copy); }); } return n; }); }).catch(function () { return new Response('', {status: 503}); }));
  }
});
