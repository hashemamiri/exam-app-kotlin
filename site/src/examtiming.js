/* =====================================================================
 * V357 — منطق زمان‌بندی آزمون و حالت آفلاین (پیمانهٔ خالص)
 *
 * این پیمانه شبکه و DOM ندارد. همان الگوریتم عیناً در اپ
 * (ExamTiming.kt) پیاده شده و بردارهای مشترک هر دو را می‌بندند.
 *
 * قاعدهٔ اعتماد: مهلت را **سرور** تعیین می‌کند. این پیمانه فقط مهلت
 * سرور را با ساعت محلی تراز می‌کند و وضعیت را حساب می‌کند؛ هیچ‌گاه
 * مهلت را از خودش نمی‌سازد و قفل را دور نمی‌زند.
 * ===================================================================== */
(function (root) {
  'use strict';

  var MAX_EXTRA_MINUTES = 240;
  var MIN_QUESTION_SECONDS = 10;
  var MAX_QUESTION_SECONDS = 7200;
  var MIN_REASON = 5;
  var MAX_REASON = 500;
  var OFFLINE_GRACE_MS = 120000; /* همان رواداری سمت سرور */

  /**
   * تراز کردن مهلت سرور با ساعت محلی.
   * اگر ساعت دستگاه کاربر جلو یا عقب باشد، «فاصلهٔ» سرور حفظ می‌شود.
   */
  function alignDeadline(expiresAtMs, serverNowMs, localNowMs) {
    if (!(expiresAtMs > 0)) return 0;
    if (!(serverNowMs > 0)) return expiresAtMs;
    return localNowMs + (expiresAtMs - serverNowMs);
  }

  /** میلی‌ثانیهٔ باقی‌مانده؛ هرگز منفی نمی‌شود. */
  function remainingMs(alignedDeadlineMs, localNowMs) {
    if (!(alignedDeadlineMs > 0)) return -1; /* بی‌نهایت */
    return Math.max(0, alignedDeadlineMs - localNowMs);
  }

  /** «۱:۰۵» یا «۱:۰۲:۰۳» — رقم لاتین؛ نمایش فارسی کار رابط است. */
  function formatClock(ms) {
    if (ms < 0) return '∞';
    var total = Math.floor(ms / 1000);
    var h = Math.floor(total / 3600);
    var m = Math.floor((total % 3600) / 60);
    var s = total % 60;
    function pad(n) { return n < 10 ? '0' + n : String(n); }
    return h > 0 ? h + ':' + pad(m) + ':' + pad(s) : m + ':' + pad(s);
  }

  /**
   * وضعیت یک سؤال.
   * limited=false یعنی این سؤال محدودیت زمانی ندارد.
   * locked از سرور می‌آید؛ اگر مهلت گذشته باشد هم قفل حساب می‌شود تا
   * رابط بی‌درنگ واکنش دهد، ولی اعتبار نهایی با سرور است.
   */
  function questionState(sync, alignedDeadlineMs, localNowMs, serverLocked) {
    var limited = !!(sync && sync.limited);
    var left = limited ? remainingMs(alignedDeadlineMs, localNowMs) : -1;
    var expired = limited && left === 0;
    return {
      limited: limited,
      locked: !!serverLocked || expired,
      remainingMs: left,
      clock: limited ? formatClock(left) : '',
      warn: limited && left > 0 && left <= 30000
    };
  }

  /** اعتبارسنجی مجوز زمان اضافه — پیش از فرستادن به سرور. */
  function validateGrant(minutes, reason) {
    var m = Number(minutes);
    var r = String(reason == null ? '' : reason).trim();
    if (!isFinite(m) || Math.floor(m) !== m || m < 0 || m > MAX_EXTRA_MINUTES) {
      return 'زمان اضافه باید عدد صحیح بین ۰ تا ۲۴۰ دقیقه باشد.';
    }
    if (r.length < MIN_REASON || r.length > MAX_REASON) {
      return 'دلیل زمان اضافه باید بین ۵ تا ۵۰۰ نویسه باشد.';
    }
    return '';
  }

  /**
   * قالب محدودیت سؤال‌ها — هر خط: «شمارهٔ سؤال = ثانیه»
   * «همه = ۳۰۰» یعنی پیش‌فرض همهٔ سؤال‌ها (کلید -1).
   * شمارهٔ سؤال از ۱ شمرده می‌شود؛ در ذخیره به شاخص صفرم تبدیل می‌شود.
   */
  /* رقم فارسی/عربی به لاتین — معلم فارسی‌زبان طبعاً «۲ = ۰» می‌نویسد. */
  function latinDigits(s) {
    var out = '', src = String(s == null ? '' : s);
    for (var i = 0; i < src.length; i++) {
      var c = src.charCodeAt(i);
      if (c >= 0x06F0 && c <= 0x06F9) out += String(c - 0x06F0);
      else if (c >= 0x0660 && c <= 0x0669) out += String(c - 0x0660);
      else out += src.charAt(i);
    }
    return out;
  }

  function parseLimits(text) {
    var limits = {}, errors = [], count = 0;
    var lines = latinDigits(String(text == null ? '' : text)).split(/\r\n|\r|\n/);
    for (var i = 0; i < lines.length; i++) {
      var raw = lines[i].trim();
      if (!raw || raw.charAt(0) === '#') continue;
      var cut = raw.indexOf('=');
      if (cut < 0) { errors.push({line: i + 1, message: 'شمارهٔ سؤال و ثانیه را با «=» جدا کنید.'}); continue; }
      var key = raw.slice(0, cut).trim();
      var val = raw.slice(cut + 1).trim();
      var isAll = (key === 'همه' || key.toLowerCase() === 'all' || key === '*');
      var qi;
      if (isAll) {
        qi = -1;
      } else {
        if (!/^[0-9]+$/.test(key)) { errors.push({line: i + 1, message: 'شمارهٔ سؤال باید عدد باشد یا «همه».'}); continue; }
        qi = Number(key) - 1;
        if (qi < 0) { errors.push({line: i + 1, message: 'شمارهٔ سؤال از ۱ شروع می‌شود.'}); continue; }
      }
      if (!/^[0-9]+$/.test(val)) { errors.push({line: i + 1, message: 'ثانیه باید عدد صحیح باشد.'}); continue; }
      var sec = Number(val);
      if (sec < MIN_QUESTION_SECONDS || sec > MAX_QUESTION_SECONDS) {
        errors.push({line: i + 1, message: 'زمان هر سؤال باید بین ۱۰ تا ۷۲۰۰ ثانیه باشد.'});
        continue;
      }
      limits[String(qi)] = sec;
      count++;
    }
    return {limits: limits, errors: errors, count: count};
  }

  /** برگرداندن محدودیت‌ها به همان قالب (رفت‌وبرگشت پایدار). */
  function formatLimits(limits) {
    var keys = Object.keys(limits || {}).map(Number).sort(function (a, b) { return a - b; });
    return keys.map(function (k) {
      return (k === -1 ? 'همه' : String(k + 1)) + ' = ' + limits[String(k)];
    }).join('\n');
  }

  /**
   * آیا برگهٔ آفلاین هنوز قابل ارسال است؟
   * مهلت + رواداری، همان قاعدهٔ سرور. توکن باید موجود باشد.
   */
  function offlineSubmittable(pack, localNowMs, alignedDeadlineMs) {
    if (!pack || !pack.token) return {ok: false, reason: 'بستهٔ آفلاین گرفته نشده است.'};
    if (pack.used) return {ok: false, reason: 'این بستهٔ آفلاین پیش‌تر مصرف شده است.'};
    if (!(alignedDeadlineMs > 0)) return {ok: false, reason: 'مهلت آزمون معلوم نیست.'};
    if (localNowMs > alignedDeadlineMs + OFFLINE_GRACE_MS) {
      return {ok: false, reason: 'مهلت آزمون گذشته است؛ برگهٔ آفلاین پذیرفته نمی‌شود.'};
    }
    return {ok: true, reason: ''};
  }

  /**
   * دفتر رویداد آفلاین. چون نظارت زنده ممکن نیست، رویدادها نگه داشته
   * می‌شوند تا معلم پس از ارسال ببیند. سقف تعداد برای مهار حافظه.
   */
  var MAX_EVENTS = 500;
  function pushEvent(events, kind, atMs, detail) {
    var list = (events || []).slice();
    var k = String(kind || '').trim().slice(0, 40);
    if (!k) return list;
    list.push({t: k, at: Math.max(0, Math.floor(atMs || 0)), d: String(detail == null ? '' : detail).slice(0, 200)});
    if (list.length > MAX_EVENTS) list = list.slice(list.length - MAX_EVENTS);
    return list;
  }

  var api = {
    MAX_EXTRA_MINUTES: MAX_EXTRA_MINUTES,
    MIN_QUESTION_SECONDS: MIN_QUESTION_SECONDS, MAX_QUESTION_SECONDS: MAX_QUESTION_SECONDS,
    MIN_REASON: MIN_REASON, MAX_REASON: MAX_REASON,
    OFFLINE_GRACE_MS: OFFLINE_GRACE_MS, MAX_EVENTS: MAX_EVENTS,
    latinDigits: latinDigits,
    alignDeadline: alignDeadline, remainingMs: remainingMs, formatClock: formatClock,
    questionState: questionState, validateGrant: validateGrant,
    parseLimits: parseLimits, formatLimits: formatLimits,
    offlineSubmittable: offlineSubmittable, pushEvent: pushEvent
  };

  root.ExamTiming = api;
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
})(typeof window !== 'undefined' ? window : globalThis);
