/* V147 — Service Worker آزمون‌ساز: پوستهٔ سایت آفلاین/فوری، دادهٔ Supabase همیشه از شبکه.
   نسخه در build (CI) با هش index.html جایگزین می‌شود تا هر انتشار، کش قدیمی را دور بریزد. */
var VERSION = '__SW_VERSION__';
var CACHE = 'azmoon-shell-' + VERSION;
var SHELL = ['/', '/pwa/manifest.webmanifest', '/pwa/icon-192.png', '/pwa/icon-512.png'];
self.addEventListener('install', function (e) {
  e.waitUntil(caches.open(CACHE).then(function (c) { return c.addAll(SHELL); }).then(function () { return self.skipWaiting(); }));
});
self.addEventListener('activate', function (e) {
  e.waitUntil(caches.keys().then(function (keys) {
    return Promise.all(keys.filter(function (k) { return k.indexOf('azmoon-shell-') === 0 && k !== CACHE; }).map(function (k) { return caches.delete(k); }));
  }).then(function () { return self.clients.claim(); }));
});
self.addEventListener('message', function (e) { if (e.data === 'skipWaiting') self.skipWaiting(); });
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
  if (url.pathname.indexOf('/pwa/') === 0) {
    e.respondWith(caches.match(req).then(function (r) { return r || fetch(req).then(function (n) { var copy = n.clone(); caches.open(CACHE).then(function (c) { c.put(req, copy); }); return n; }); }));
  }
});
