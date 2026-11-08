/* =====================================================================
 * V356 — بستهٔ ۳: تصحیح کد، فاز صفر (بدون اجرای کد)
 *
 * این پیمانه «خالص» است: نه شبکه، نه DOM، و مهم‌تر از همه **کد دانش‌آموز
 * هرگز اجرا نمی‌شود**. معلم خودش کد را می‌خواند یا در محیط خودش اجرا
 * می‌کند و نتیجهٔ هر مورد آزمون را علامت می‌زند؛ این پیمانه فقط از
 * وزن موردهای قبول‌شده یک «پیشنهاد نمره» می‌سازد.
 *
 * همان الگوریتم عیناً در اپ (CodeGrading.kt) پیاده شده و بردارهای آزمون
 * مشترک هر دو را به هم می‌بندد.
 * ===================================================================== */
(function (root) {
  'use strict';

  var STEP = 0.25;
  var MAX_TESTS = 40;
  var MAX_TEXT = 4000;
  var MAX_LABEL = 120;
  var MAX_REFERENCE = 20000;

  var DEFAULT_OPTS = {trimLines: true, collapseBlank: true, ignoreCase: false};

  function opts(o) {
    var s = o || {};
    return {
      trimLines: s.trimLines == null ? DEFAULT_OPTS.trimLines : !!s.trimLines,
      collapseBlank: s.collapseBlank == null ? DEFAULT_OPTS.collapseBlank : !!s.collapseBlank,
      ignoreCase: s.ignoreCase == null ? DEFAULT_OPTS.ignoreCase : !!s.ignoreCase
    };
  }

  /**
   * یکسان‌سازی خروجی برای مقایسه.
   * trimLines: فاصلهٔ ابتدا/انتهای هر خط برداشته می‌شود.
   * collapseBlank: خط‌های خالی حذف می‌شوند.
   * ignoreCase: بزرگی/کوچکی حرف لاتین نادیده گرفته می‌شود.
   * پایان خط‌های ویندوز/مک هم یکسان می‌شوند.
   */
  function normalizeOutput(text, o) {
    var s = opts(o);
    var lines = String(text == null ? '' : text).split(/\r\n|\r|\n/);
    var out = [];
    for (var i = 0; i < lines.length; i++) {
      var line = s.trimLines ? lines[i].replace(/^[ \t]+|[ \t]+$/g, '') : lines[i];
      if (s.collapseBlank && line.replace(/[ \t]/g, '') === '') continue;
      out.push(line);
    }
    var joined = out.join('\n');
    if (s.ignoreCase) joined = joined.toLowerCase();
    /* پیرایش پایانی تنها وقتی اعمال می‌شود که معلم پیرایش را روشن
       گذاشته باشد؛ وگرنه خاموش‌کردن آن بی‌اثر می‌شد. */
    return s.trimLines ? joined.replace(/^[ \t\n]+|[ \t\n]+$/g, '') : joined;
  }

  /** آیا خروجی واقعی معلم با خروجی انتظار یکی است؟ */
  function outputMatches(expected, actual, o) {
    return normalizeOutput(expected, o) === normalizeOutput(actual, o);
  }

  function roundStep(v) { return Math.round(v / STEP) * STEP; }

  /**
   * پیشنهاد نمره از چک‌لیست معلم.
   * tests: [{id, weight}]  checks: {testId: {passed:bool}}
   * موردهای علامت‌نزده «قبول» حساب نمی‌شوند، ولی complete=false می‌شود
   * تا معلم بداند هنوز کار تمام نشده است.
   */
  function scoreFromTests(tests, checks, maxScore) {
    var list = tests || [], map = checks || {};
    var total = 0, got = 0, checked = 0, passed = 0;
    for (var i = 0; i < list.length; i++) {
      var w = Number(list[i].weight);
      if (!(w > 0)) w = 1;
      total += w;
      var c = map[list[i].id];
      if (c) {
        checked++;
        if (c.passed) { got += w; passed++; }
      }
    }
    var ratio = total > 0 ? got / total : 0;
    var max = Number(maxScore) || 0;
    var suggested = Math.max(0, Math.min(max, roundStep(max * ratio)));
    return {
      suggested: suggested, ratio: ratio,
      passedWeight: got, totalWeight: total,
      checkedCount: checked, passedCount: passed, totalCount: list.length,
      complete: list.length > 0 && checked === list.length,
      hasTests: list.length > 0
    };
  }

  /* پویش تک‌گذر: اگر با replace پشت‌سرهم انجام شود، رشتهٔ «\\n»
     (بک‌اسلش تحت‌اللفظی + حرف n) اشتباهاً به خط جدید تبدیل می‌شود. */
  function unescapeText(s) {
    var src = String(s == null ? '' : s), out = '', i = 0;
    while (i < src.length) {
      if (src.charAt(i) === '\\' && i + 1 < src.length) {
        var c = src.charAt(i + 1);
        if (c === 'n') out += '\n';
        else if (c === 't') out += '\t';
        else out += c;
        i += 2;
        continue;
      }
      out += src.charAt(i);
      i++;
    }
    return out;
  }

  function escapeText(s) {
    return String(s == null ? '' : s)
      .replace(/\\/g, '\\\\').replace(/\n/g, '\\n').replace(/\t/g, '\\t');
  }

  function isWeight(s) {
    if (!/^[0-9]+(\.[0-9]+)?$/.test(s)) return false;
    var v = Number(s);
    return isFinite(v) && v > 0 && v <= 100;
  }

  /**
   * قالب موارد آزمون — هر خط یک مورد:
   *   برچسب | وزن | ورودی => خروجی
   *   برچسب | وزن | پنهان | ورودی => خروجی
   * ترتیب بخش‌ها آزاد است؛ بخشی که «=>» دارد ورودی/خروجی است،
   * بخش عددی وزن است، «پنهان» یا «hidden» یعنی مورد پنهان، و
   * نخستین بخش متنی دیگر برچسب است. برای خط جدید «\n» بنویسید.
   */
  function parseTests(text) {
    var tests = [], errors = [];
    var lines = String(text == null ? '' : text).split(/\r\n|\r|\n/);
    for (var i = 0; i < lines.length; i++) {
      var raw = lines[i].trim();
      if (!raw || raw.charAt(0) === '#') continue;
      var parts = raw.split('|').map(function (p) { return p.trim(); });
      var io = null, weight = 1, hidden = false, label = '';
      for (var j = 0; j < parts.length; j++) {
        var p = parts[j];
        if (p.indexOf('=>') >= 0) {
          if (io !== null) { errors.push({line: i + 1, message: 'در هر خط فقط یک «=>» بگذارید.'}); io = false; break; }
          io = p;
        } else if (isWeight(p)) {
          weight = Number(p);
        } else if (p.toLowerCase() === 'hidden' || p === 'پنهان') {
          hidden = true;
        } else if (p && !label) {
          label = p;
        }
      }
      if (io === false) continue;
      if (io === null) { errors.push({line: i + 1, message: 'ورودی و خروجی را با «=>» جدا کنید.'}); continue; }
      var cut = io.indexOf('=>');
      var input = unescapeText(io.slice(0, cut).trim());
      var expected = unescapeText(io.slice(cut + 2).trim());
      if (!expected) { errors.push({line: i + 1, message: 'خروجی انتظار خالی است.'}); continue; }
      if (label.length > MAX_LABEL) { errors.push({line: i + 1, message: 'برچسب بلندتر از ۱۲۰ نویسه است.'}); continue; }
      if (input.length > MAX_TEXT || expected.length > MAX_TEXT) {
        errors.push({line: i + 1, message: 'ورودی یا خروجی بیش از حد بلند است.'}); continue;
      }
      tests.push({label: label, input: input, expected: expected, weight: weight, hidden: hidden});
      if (tests.length > MAX_TESTS) {
        errors.push({line: i + 1, message: 'بیش از ۴۰ مورد آزمون مجاز نیست.'});
        tests.pop();
        break;
      }
    }
    return {tests: tests, errors: errors};
  }

  /** برگرداندن موارد به همان قالب متنی (رفت‌وبرگشت پایدار). */
  function formatTests(tests) {
    return (tests || []).map(function (t) {
      var bits = [];
      if (t.label) bits.push(String(t.label));
      var w = Number(t.weight);
      if (!(w > 0)) w = 1;
      if (w !== 1) bits.push(String(w));
      if (t.hidden) bits.push('پنهان');
      bits.push(escapeText(t.input) + ' => ' + escapeText(t.expected));
      return bits.join(' | ');
    }).join('\n');
  }

  /** خلاصهٔ وضعیت یک سؤال کد برای کارت معلم. */
  function summary(tests, checks) {
    var r = scoreFromTests(tests, checks, 1);
    if (!r.hasTests) return 'مورد آزمونی تعریف نشده';
    if (r.checkedCount === 0) return 'هنوز بررسی نشده';
    if (!r.complete) return 'بررسی ناقص';
    return 'بررسی کامل';
  }

  var api = {
    STEP: STEP, MAX_TESTS: MAX_TESTS, MAX_TEXT: MAX_TEXT,
    MAX_LABEL: MAX_LABEL, MAX_REFERENCE: MAX_REFERENCE, DEFAULT_OPTS: DEFAULT_OPTS,
    normalizeOutput: normalizeOutput, outputMatches: outputMatches,
    scoreFromTests: scoreFromTests, parseTests: parseTests, formatTests: formatTests,
    escapeText: escapeText, unescapeText: unescapeText, summary: summary
  };

  root.ExamCodeGrade = api;
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
})(typeof window !== 'undefined' ? window : globalThis);
