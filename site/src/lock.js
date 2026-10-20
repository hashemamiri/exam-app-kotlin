/* ===================================================================
   V325 — قفل میز کار (PIN) برای نسخهٔ وب/دسکتاپ
   اپ اندروید از V56 قفل بیومتریک/رمز دستگاه دارد (AppLockUi.kt)؛ سایت هیچ قفلی نداشت
   در حالی که معمولاً روی کامپیوتر مشترک مدرسه باز می‌ماند.

   • PIN ۴ تا ۶ رقمی، ذخیره‌شده به‌صورت هش SHA-256 با نمک (خودِ PIN ذخیره نمی‌شود).
   • قفل خودکار پس از بی‌کاری (پیش‌فرض ۵ دقیقه؛ ۱/۵/۱۵/۳۰ دقیقه یا «فقط دستی»).
   • با بارگذاری دوبارهٔ صفحه، اگر در همین نشست باز نشده باشد، قفل می‌ماند.
   • دکمهٔ «قفل فوری» در بخش «امنیت» تنظیمات و در منوی صفحهٔ «منو».
   • این قفل برای جلوگیری از دسترسی اتفاقی همکار/دانش‌آموز است، نه رمزنگاری داده؛
     داده‌ها همان‌طور که هستند در مرورگر می‌مانند.
   =================================================================== */
(function () {
  var S = window.ExamSite;
  if (!S) return;
  var el = S.el, toast = S.toast;
  var LS = 'examsite.lock.v1';          // تنظیمات قفل
  var SS = 'examsite.lock.open.v1';     // «در این نشست باز شده» (sessionStorage)
  var DEF = {enabled: false, hash: '', salt: '', timeoutMin: 5};
  var TIMEOUTS = [[1, '۱ دقیقه'], [5, '۵ دقیقه'], [15, '۱۵ دقیقه'], [30, '۳۰ دقیقه'], [0, 'فقط دستی']];

  function cfg() {
    try { return Object.assign({}, DEF, JSON.parse(localStorage.getItem(LS) || '{}')); }
    catch (e) { return Object.assign({}, DEF); }
  }
  function save(c) { try { localStorage.setItem(LS, JSON.stringify(c)); } catch (e) {} }
  function isEnabled() { var c = cfg(); return !!(c.enabled && c.hash); }
  function loggedIn() { try { return !!(S.user && S.user()); } catch (e) { return false; } }

  /* هش SHA-256 (بدون crypto.subtle — مرورگرهای قدیمی/HTTP — همان خلاصه‌سازی سبک) */
  function hex(buf) { return Array.prototype.map.call(new Uint8Array(buf), function (b) { return ('0' + b.toString(16)).slice(-2); }).join(''); }
  function randomHex(n) { var a = new Uint8Array(n); (window.crypto || {}).getRandomValues ? crypto.getRandomValues(a) : a.forEach(function (_, i) { a[i] = Math.floor(Math.random() * 256); }); return hex(a.buffer); }
  function fallbackHash(text) {
    var h1 = 0x811c9dc5, h2 = 0x1000193;
    for (var i = 0; i < text.length; i++) { var c = text.charCodeAt(i); h1 = (h1 ^ c) * 16777619 >>> 0; h2 = ((h2 + c) * 2654435761) >>> 0; }
    return ('00000000' + h1.toString(16)).slice(-8) + ('00000000' + h2.toString(16)).slice(-8);
  }
  function hash(text) {
    if (window.crypto && crypto.subtle && crypto.subtle.digest) {
      try {
        return crypto.subtle.digest('SHA-256', new TextEncoder().encode(text)).then(hex);
      } catch (e) { /* ادامه با fallback */ }
    }
    return Promise.resolve(fallbackHash(text));
  }
  function npin(p) { return String(p || '').replace(/[۰-۹]/g, function (d) { return String('۰۱۲۳۴۵۶۷۸۹'.indexOf(d)); }).replace(/\D/g, ''); }

  var locked = false, overlay = null, idleTimer = null, lastActive = Date.now();
  var onUnlockCbs = [];

  function markActive() { lastActive = Date.now(); }
  function setupIdle() {
    ['pointerdown', 'keydown', 'touchstart', 'scroll'].forEach(function (ev) {
      window.addEventListener(ev, markActive, {passive: true});
    });
    document.addEventListener('visibilitychange', function () { if (!document.hidden) { markActive(); lastActive = Date.now(); } });
    if (idleTimer) clearInterval(idleTimer);
    idleTimer = setInterval(function () {
      var c = cfg();
      if (!c.enabled || locked || !c.timeoutMin || !loggedIn()) return;
      if (Date.now() - lastActive > c.timeoutMin * 60000) lockNow();
    }, 15000);
  }

  function buildOverlay() {
    if (overlay) return overlay;
    var inp = el('input', {type: 'password', inputmode: 'numeric', autocomplete: 'off', maxlength: '6',
      placeholder: '••••', 'aria-label': 'رمز قفل', style: 'letter-spacing:10px;text-align:center;font-size:24px;padding:14px;width:100%;border-radius:14px;border:1px solid var(--line,#dbe4ee)'});
    var msg = el('p', {class: 'muted', style: 'margin:6px 0 0;min-height:20px;color:#c2412d;font-size:13px'});
    function tryUnlock() {
      var pin = npin(inp.value);
      if (pin.length < 4) { msg.textContent = 'رمز ۴ تا ۶ رقمی است.'; return; }
      var c = cfg();
      hash(c.salt + ':' + pin).then(function (h) {
        if (h !== c.hash) { msg.textContent = 'رمز نادرست است.'; inp.value = ''; inp.focus(); return; }
        unlock();
      });
    }
    inp.addEventListener('keydown', function (e) { if (e.key === 'Enter') tryUnlock(); });
    var card = el('div', {class: 'site-lock-card'}, [
      el('div', {class: 'site-lock-badge', html: '🔒'}),
      el('h2', {text: 'صفحه قفل است', style: 'margin:6px 0 2px;font-size:19px'}),
      el('p', {class: 'muted', text: 'برای ادامه، رمز قفل را وارد کنید.', style: 'margin:0 0 12px;font-size:13px'}),
      inp, msg,
      el('button', {class: 'btn', style: 'width:100%;margin-top:12px', text: 'باز کردن', onclick: tryUnlock}),
      el('button', {class: 'btn light', style: 'width:100%;margin-top:8px', text: 'خروج از حساب',
        onclick: function () { try { sessionStorage.removeItem(SS); } catch (e) {} location.reload(); }})
    ]);
    var ov = el('div', {class: 'site-lock', id: 'siteLockOverlay', role: 'dialog', 'aria-modal': 'true', 'aria-label': 'صفحه قفل'}, [card]);
    ov.addEventListener('pointerdown', function (e) { e.stopPropagation(); });
    document.body.appendChild(ov);
    setTimeout(function () { inp.focus(); }, 30);
    overlay = ov;
    return ov;
  }

  function lockNow() {
    if (!isEnabled() || locked) return false;
    locked = true;
    try { sessionStorage.removeItem(SS); } catch (e) {}
    buildOverlay().classList.add('on');
    document.body.classList.add('site-locked');
    var i = overlay.querySelector('input'); if (i) { i.value = ''; setTimeout(function () { i.focus(); }, 40); }
    return true;
  }
  function unlock() {
    locked = false;
    try { sessionStorage.setItem(SS, '1'); sessionStorage.setItem(SS + '.at', String(Date.now())); } catch (e) {}
    if (overlay) overlay.classList.remove('on');
    document.body.classList.remove('site-locked');
    lastActive = Date.now();
    onUnlockCbs.forEach(function (f) { try { f(); } catch (e) {} });
    toast && toast('باز شد.', 'ok');
  }
  function lockAfterReload() {
    if (!isEnabled() || !loggedIn()) return false;
    var open = false;
    try { open = sessionStorage.getItem(SS) === '1'; } catch (e) {}
    if (open) return false;
    return lockNow();
  }

  /* API برای بخش «امنیت» تنظیمات */
  async function setPin(pin) {
    pin = npin(pin);
    if (pin.length < 4 || pin.length > 6) throw new Error('رمز باید ۴ تا ۶ رقم باشد.');
    var salt = randomHex(8);
    var c = cfg(); c.salt = salt; c.hash = await hash(salt + ':' + pin); c.enabled = true; save(c);
    try { sessionStorage.setItem(SS, '1'); } catch (e) {}
    return true;
  }
  async function checkPin(pin) { var c = cfg(); return (await hash(c.salt + ':' + npin(pin))) === c.hash; }
  function disable() { var c = cfg(); c.enabled = false; c.hash = ''; c.salt = ''; save(c); try { sessionStorage.removeItem(SS); } catch (e) {} }
  function setTimeoutMin(min) { var c = cfg(); c.timeoutMin = Math.max(0, Math.min(120, +min || 0)); save(c); }
  function onChange(fn) { onUnlockCbs.push(fn); }

  setupIdle();
  document.addEventListener('DOMContentLoaded', function () { setTimeout(lockAfterReload, 400); });
  window.SiteLock = {cfg: cfg, isEnabled: isEnabled, setPin: setPin, checkPin: checkPin, disable: disable,
    setTimeoutMin: setTimeoutMin, lockNow: lockNow, unlock: unlock, onChange: onChange, TIMEOUTS: TIMEOUTS};
})();
