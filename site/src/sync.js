/* ===================================================================
   V327 — همگام‌سازی تنظیمات با حساب (بین دسکتاپ، گوشی و اپ اندروید)
   تنظیمات ظاهر (localStorage: examsite.appearance.v1)، سربرگ چاپ
   (examsite.printheader…) و تنظیمات کاغذ (pgs_pagesetup_v20) تا امروز فقط
   روی همین مرورگر می‌ماندند. حالا در جدول user_settings (V327) ذخیره می‌شوند.

   • pull(): در شروع هر نشست، تنظیمات سرور روی دستگاه اعمال می‌شود
     (اگر روی سرور چیزی نبود، همان مقدار محلی می‌ماند).
   • push(): هر تغییر ظاهر/سربرگ، با تأخیر ۱٫۵ ثانیه یک‌بار فرستاده می‌شود.
   • تعارض: «آخرین نوشتن برنده» است؛ pull فقط یک‌بار در شروع نشست اجرا می‌شود.
   =================================================================== */
(function () {
  var S = window.ExamSite;
  if (!S) return;
  var K_APPEAR = 'examsite.appearance.v1';
  var K_HEADER_PREFIX = 'examsite.printheader';
  var K_PAGE = 'pgs_pagesetup_v20';
  var pending = null, timer = null, enabled = true;

  function read(key) { try { var v = localStorage.getItem(key); return v ? JSON.parse(v) : null; } catch (e) { return null; } }
  function write(key, obj) { try { localStorage.setItem(key, JSON.stringify(obj)); return true; } catch (e) { return false; } }
  function headerKey() {
    for (var i = 0; i < localStorage.length; i++) {
      var k = localStorage.key(i);
      if (k && k.indexOf(K_HEADER_PREFIX) === 0) return k;
    }
    return K_HEADER_PREFIX + '.v1';
  }
  function loggedIn() { try { var u = S.user && S.user(); return !!(u && u.id); } catch (e) { return false; } }

  async function pull() {
    if (!enabled || !loggedIn()) return false;
    try {
      var r = await S.rpcObj('native_settings_get_v327', {});
      if (!r || r.ok === false || r.error) return false;
      var touched = false;
      if (r.appearance && Object.keys(r.appearance).length) {
        var local = read(K_APPEAR) || {};
        if (JSON.stringify(local) !== JSON.stringify(r.appearance)) {
          write(K_APPEAR, r.appearance);
          if (window.SiteMobile && SiteMobile.setAppearance) {
            try { SiteMobile.setAppearance({}); } catch (e) {}
          }
          touched = true;
        }
      }
      if (r.print_header && Object.keys(r.print_header).length) {
        var hk = headerKey();
        var lh = read(hk) || {};
        if (JSON.stringify(lh) !== JSON.stringify(r.print_header)) { write(hk, r.print_header); touched = true; }
      }
      if (r.page_setup && Object.keys(r.page_setup).length) {
        var lp = read(K_PAGE) || {};
        if (JSON.stringify(lp) !== JSON.stringify(r.page_setup)) { write(K_PAGE, r.page_setup); touched = true; }
      }
      return touched;
    } catch (e) { return false; }
  }

  function snapshot() {
    return {
      appearance: read(K_APPEAR) || {},
      print_header: read(headerKey()) || {},
      page_setup: read(K_PAGE) || {}
    };
  }

  async function flush() {
    timer = null;
    if (!loggedIn() || !pending) return;
    var body = pending; pending = null;
    try {
      await S.rpcObj('native_settings_put_v327', {
        p_appearance: body.appearance || null,
        p_print_header: body.print_header || null,
        p_page_setup: body.page_setup || null
      });
    } catch (e) { /* بی‌سر‌و‌صدا: همگام‌سازی نباید کار کاربر را بشکند */ }
  }

  function push(part) {
    if (!loggedIn()) return;
    var snap = snapshot();
    pending = pending || {};
    if (!part || part === 'appearance') pending.appearance = snap.appearance;
    if (!part || part === 'header' || part === 'all') pending.print_header = snap.print_header;
    if (!part || part === 'page' || part === 'all') pending.page_setup = snap.page_setup;
    if (timer) clearTimeout(timer);
    timer = setTimeout(flush, 1500);
  }

  /* pull یک‌بار در شروع نشست؛ push هنگام تغییر ظاهر و خروج از صفحه */
  function boot() {
    if (!loggedIn()) { setTimeout(boot, 2500); return; }
    pull().then(function (touched) {
      if (touched && window.SiteMobile && SiteMobile.setAppearance) {
        try { SiteMobile.setAppearance({}); } catch (e) {}
      }
    });
  }
  document.addEventListener('visibilitychange', function () { if (document.hidden) push('all'); });
  window.addEventListener('beforeunload', function () { push('all'); });
  setTimeout(boot, 1200);

  window.SiteSettingsSync = {pull: pull, push: push, snapshot: snapshot,
    enable: function (v) { enabled = !!v; }, enabled: function () { return enabled; }};
})();
