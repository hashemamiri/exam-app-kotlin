/* V147 — Service Worker آزمون‌ساز: پوستهٔ سایت آفلاین/فوری، دادهٔ Supabase همیشه از شبکه.
   نسخه در build (CI) با هش index.html جایگزین می‌شود تا هر انتشار، کش قدیمی را دور بریزد. */
var VERSION = '__SW_VERSION__';
var CACHE = 'azmoon-shell-' + VERSION;
var SHELL = ['/', '/pwa/manifest.webmanifest', '/pwa/v2/icon-192.png', '/pwa/v2/icon-512.png']; /* V222.2 */
self.addEventListener('install', function (e) {
  e.waitUntil(caches.open(CACHE).then(function (c) { return c.addAll(SHELL); }).then(function () { return self.skipWaiting(); }));
});
self.addEventListener('activate', function (e) {
  e.waitUntil(caches.keys().then(function (keys) {
    return Promise.all(keys.filter(function (k) { return k.indexOf('azmoon-shell-') === 0 && k !== CACHE; }).map(function (k) { return caches.delete(k); }));
  }).then(function () { return self.clients.claim(); }));
});
self.addEventListener('message', function (e) { if (e.data === 'skipWaiting') self.skipWaiting(); });
/* V232.9 — اعلان وب: پیام data-only از FCM → نمایش؛ ضربه → تمرکز روی پنجرهٔ باز (پیام push-nav) یا باز کردن /?page=… */
self.addEventListener('push', function (e) {
  var p = {}; try { p = e.data ? e.data.json() : {}; } catch (x) { p = {}; }
  var d = p.data || p; var n = p.notification || {};
  var title = d.title || n.title || 'آزمون آنلاین'; var body = d.body || n.body || '';
  var data = {page: d.page || '', exam_id: d.exam_id || '', code: d.code || ''};
  e.waitUntil(self.registration.showNotification(title, {body: body, icon: '/pwa/v2/icon-192.png', badge: '/pwa/v2/icon-192.png', dir: 'rtl', lang: 'fa', tag: d.outbox_id ? 'ox-' + d.outbox_id : undefined, data: data}));
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
      var copy = r.clone(); caches.open(CACHE).then(function (c) { c.put('/', copy); }); return r;
    }).catch(function () { return caches.match('/'); }));
    return;
  }
  /* V162 — engines.<hash>.js: کش-اول (نام با هش عوض می‌شود)؛ نسخه‌های قدیمی موتور پاک می‌شوند */
  if (/^\/engines\.[0-9a-f]+\.js$/.test(url.pathname)) {
    e.respondWith(caches.match(req).then(function (r) { return r || fetch(req).then(function (n) { if (n.ok) { var copy = n.clone(); caches.open(CACHE).then(function (c) { c.keys().then(function (ks) { ks.forEach(function (k) { if (/\/engines\./.test(k.url) && k.url !== req.url) c.delete(k); }); }); c.put(req, copy); }); } return n; }); }));
    return;
  }
  /* V164 — قلم‌ها و پوشهٔ pwa: کش-اول */
  if (url.pathname.indexOf('/pwa/') === 0 || url.pathname.indexOf('/fonts/') === 0) {
    e.respondWith(caches.match(req).then(function (r) { return r || fetch(req).then(function (n) { var copy = n.clone(); caches.open(CACHE).then(function (c) { c.put(req, copy); }); return n; }); }));
  }
});
