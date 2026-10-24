/* V147 — Service Worker آزمون‌ساز: پوستهٔ سایت آفلاین/فوری، دادهٔ Supabase همیشه از شبکه.
   نسخه در build (CI) با هش index.html جایگزین می‌شود تا هر انتشار، کش قدیمی را دور بریزد. */
var VERSION = 'bf4493e92418';
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
/* V325 — دسته‌بندی اعلان: انتخاب‌های کاربر در IndexedDB (examsite-push/prefs/cats) است
   چون Service Worker به localStorage دسترسی ندارد. اگر دسته‌ای خاموش باشد، اعلان نمایش داده نمی‌شود. */
function pushCats() {
  return new Promise(function (resolve) {
    try {
      var r = indexedDB.open('examsite-push', 1);
      r.onupgradeneeded = function () { try { r.result.createObjectStore('prefs'); } catch (e) {} };
      r.onerror = function () { resolve(null); };
      r.onsuccess = function () {
        try {
          var tx = r.result.transaction('prefs', 'readonly').objectStore('prefs').get('cats');
          tx.onsuccess = function () { resolve(tx.result || null); };
          tx.onerror = function () { resolve(null); };
        } catch (e) { resolve(null); }
      };
    } catch (e) { resolve(null); }
  });
}
var PUSH_CAT = {grades: 'grades', exam: 'exam', calendar: 'calendar', requests: 'requests', grading: 'requests', students: 'requests'};
self.addEventListener('push', function (e) {
  var p = {}; try { p = e.data ? e.data.json() : {}; } catch (x) { p = {}; }
  var d = p.data || p; var n = p.notification || {};
  var title = d.title || n.title || 'آزمون آنلاین'; var body = d.body || n.body || '';
  var data = {page: d.page || '', exam_id: d.exam_id || '', code: d.code || ''};
  e.waitUntil(pushCats().then(function (cats) {
    var key = PUSH_CAT[data.page] || '';
    if (cats && key && cats[key] === false) return; /* دسته خاموش است */
    return self.registration.showNotification(title, {body: body, icon: '/pwa/v3/icon-192.png', badge: '/pwa/v3/icon-192.png', dir: 'rtl', lang: 'fa', tag: d.outbox_id ? 'ox-' + d.outbox_id : undefined, data: data});
  }));
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
function fetchTimed(req, ms) {
  if (typeof AbortController === 'undefined') return fetch(req);
  var ctrl = new AbortController(), tid = setTimeout(function () { try { ctrl.abort(); } catch (e) {} }, ms);
  return fetch(req, {signal: ctrl.signal}).finally(function () { clearTimeout(tid); });
}
self.addEventListener('fetch', function (e) {
  var req = e.request;
  if (req.method !== 'GET') return;
  var url = new URL(req.url);
  if (url.origin !== self.location.origin) return; /* Supabase، آروان، فونت‌ها: مستقیم شبکه */
  if ((req.mode === 'navigate' && (url.pathname === '/' || url.pathname === '/index.html')) || url.pathname === '/' || url.pathname === '/index.html') {
    /* V312.0 — ضد قفل اپراتورهای ایران: اگر پوسته در کش هست حداکثر ۳٫۵ ثانیه منتظر شبکه می‌ماند و در صورت کندی/قطعی فوراً کش را نشان داده و در پس‌زمینه به‌روز می‌کند؛ اگر کش نبود با مهلت ۱۰ ثانیه و یک تکرار خودکار تلاش می‌کند. */
    e.respondWith(caches.match('/').then(function (cached) {
      var bgUpdate = fetch(req).then(function (r) {
        if (r && r.ok) { var copy = r.clone(); caches.open(CACHE).then(function (c) { c.put('/', copy); }); }
        return r;
      });
      if (cached) {
        return Promise.race([
          bgUpdate.catch(function () { return cached; }),
          new Promise(function (resolve) { setTimeout(function () { resolve(cached); }, 3500); })
        ]);
      }
      return fetchTimed(req, 10000).catch(function () { return fetchTimed(req, 12000); }).then(function (r) {
        if (r && r.ok) { var copy = r.clone(); caches.open(CACHE).then(function (c) { c.put('/', copy); }); }
        return r;
      }).catch(function () { return caches.match('/').then(function (c) { return c || offlinePage(); }); });
    }));
    return;
  }
  /* V162 / V312.1 — engines.<hash>.js و panels.<hash>.js: کش-اول (نام با هش عوض می‌شود)؛ نسخه‌های قدیمی پاک می‌شوند */
  if (/^\/engines\.[0-9a-f]+\.js$/.test(url.pathname)) {
    e.respondWith(caches.match(req).then(function (r) { return r || fetch(req).then(function (n) { if (n.ok) { var copy = n.clone(); caches.open(CACHE).then(function (c) { c.keys().then(function (ks) { ks.forEach(function (k) { if (/\/engines\./.test(k.url) && k.url !== req.url) c.delete(k); }); }); c.put(req, copy); }); } return n; }); }));
    return;
  }
  if (/^\/panels\.[0-9a-f]+\.js$/.test(url.pathname)) {
    e.respondWith(caches.match(req).then(function (r) { return r || fetch(req).then(function (n) { if (n.ok) { var copy = n.clone(); caches.open(CACHE).then(function (c) { c.keys().then(function (ks) { ks.forEach(function (k) { if (/\/panels\./.test(k.url) && k.url !== req.url) c.delete(k); }); }); c.put(req, copy); }); } return n; }); }));
    return;
  }
  if (/^\/(s[0-9]|c[0-9]|p[0-9])\.[0-9a-f]+\.(css|js)$/.test(url.pathname)) {
    e.respondWith(caches.match(req).then(function (r) { return r || fetch(req).then(function (n) { if (n.ok) { var copy = n.clone(); caches.open(CACHE).then(function (c) { c.put(req, copy); }); } return n; }); }));
    return;
  }
  /* V164 — قلم‌ها و پوشهٔ pwa: کش-اول */
  if (url.pathname.indexOf('/pwa/') === 0 || url.pathname.indexOf('/fonts/') === 0) {
    e.respondWith(caches.match(req).then(function (r) { return r || fetch(req).then(function (n) { if (n.ok) { var copy = n.clone(); caches.open(CACHE).then(function (c) { c.put(req, copy); }); } return n; }); }).catch(function () { return new Response('', {status: 503}); }));
  }
});
