/* V358 — بانک سؤال: درختواره، پالایش دشواری، انتخاب خودکار با نقشهٔ آزمون.
 *
 * این پیمانه «خالص» است: نه شبکه، نه DOM، نه ساعت. تنها ورودی/خروجی.
 * آینهٔ دقیق آن در اپ: domain/bank/BankPicker.kt
 *
 * قاعدهٔ اعتماد: انتخاب تصادفی با «بذر» انجام می‌شود تا نتیجه
 * بازتولیدپذیر باشد؛ همان بذر در سایت و اپ همان گزینش را می‌دهد.
 */
(function (root) {
  'use strict';

  var MAX_DEPTH = 3;
  var MAX_RULES = 40;
  var MAX_PICK = 500;
  var MAX_COUNT = 200;

  /* سطح‌ها: صفر یعنی تعیین‌نشده. چهار سطح، تصمیم V360 کاربر. */
  var DIFFICULTY_LABELS = ['تعیین‌نشده', 'ساده', 'متوسط', 'دشوار', 'خیلی دشوار'];

  function latinDigits(s) {
    var out = '';
    for (var i = 0; i < String(s == null ? '' : s).length; i++) {
      var c = String(s).charCodeAt(i);
      if (c >= 0x6F0 && c <= 0x6F9) out += String(c - 0x6F0);
      else if (c >= 0x660 && c <= 0x669) out += String(c - 0x660);
      else out += String(s)[i];
    }
    return out;
  }

  function norm(s) {
    return String(s == null ? '' : s).trim().toLowerCase();
  }

  /* --- درخت ------------------------------------------------------- */

  function byId(categories) {
    var m = {};
    (categories || []).forEach(function (c) { m[String(c.id)] = c; });
    return m;
  }

  /* همهٔ شناسه‌های زیردرخت، شامل خود گره. حلقه‌ها بی‌خطر شکسته می‌شوند. */
  function subtreeIds(categories, rootId) {
    if (rootId == null || rootId === '') return [];
    var kids = {};
    (categories || []).forEach(function (c) {
      var p = c.parent_id == null ? '' : String(c.parent_id);
      (kids[p] = kids[p] || []).push(String(c.id));
    });
    var out = [], seen = {}, stack = [String(rootId)];
    while (stack.length) {
      var id = stack.pop();
      if (seen[id]) continue;
      seen[id] = true;
      out.push(id);
      (kids[id] || []).forEach(function (k) { if (!seen[k]) stack.push(k); });
    }
    return out;
  }

  function categoryPath(categories, id) {
    var m = byId(categories), parts = [], cur = String(id), guard = 0;
    while (cur && m[cur] && guard++ < MAX_DEPTH + 2) {
      parts.unshift(String(m[cur].name || ''));
      cur = m[cur].parent_id == null ? '' : String(m[cur].parent_id);
    }
    return parts.join(' / ');
  }

  function depthOf(categories, id) {
    var m = byId(categories), d = 0, cur = String(id), guard = 0;
    while (cur && m[cur] && guard++ < MAX_DEPTH + 2) {
      d++;
      cur = m[cur].parent_id == null ? '' : String(m[cur].parent_id);
    }
    return d;
  }

  /* --- پالایش ------------------------------------------------------ */

  function itemDifficulty(it) {
    var q = (it && it.question) || {};
    var d = it && it.difficulty != null ? it.difficulty : q.difficulty;
    d = Number(d) || 0;
    return d < 0 || d > 4 ? 0 : d;
  }

  function itemTopic(it) {
    var q = (it && it.question) || {};
    return String((it && it.topic) || q.topic || '').trim();
  }

  /* opts: {text, categoryId, difficulty, subject, grade}
   * difficulty: null/'' یعنی همه؛ ۰ یعنی فقط تعیین‌نشده‌ها. */
  function filterItems(items, categories, opts) {
    opts = opts || {};
    var s = norm(latinDigits(opts.text || ''));
    var allow = null;
    if (opts.categoryId != null && opts.categoryId !== '') {
      allow = {};
      subtreeIds(categories, opts.categoryId).forEach(function (id) { allow[id] = true; });
    }
    var diff = (opts.difficulty == null || opts.difficulty === '') ? null : Number(opts.difficulty);
    var subj = String(opts.subject || '');
    var grade = (opts.grade == null || opts.grade === '') ? null : Number(opts.grade);
    return (items || []).filter(function (it) {
      var q = (it && it.question) || {};
      if (s) {
        var hay = norm(latinDigits(String(q.text || '') + ' ' + String(it.subject || q.subject || '') + ' ' + itemTopic(it)));
        if (hay.indexOf(s) < 0) return false;
      }
      if (allow) {
        var hit = (it.cat_ids || []).some(function (c) { return allow[String(c)]; });
        if (!hit) return false;
      }
      if (diff != null && itemDifficulty(it) !== diff) return false;
      if (subj && norm(String(it.subject || q.subject || '')) !== norm(subj)) return false;
      if (grade != null && Number(q.grade || 0) !== grade) return false;
      return true;
    });
  }

  /* --- نقشهٔ آزمون -------------------------------------------------- */

  function difficultyFromWord(w) {
    var t = norm(latinDigits(w));
    if (!t || t === 'هر' || t === 'همه' || t === 'any' || t === '*' || t === '-') return -1;
    if (t === '0' || t === 'تعیین‌نشده' || t === 'نامشخص' || t === 'unset') return 0;
    if (t === '1' || t === 'ساده' || t === 'آسان' || t === 'easy') return 1;
    if (t === '2' || t === 'متوسط' || t === 'medium') return 2;
    if (t === '3' || t === 'دشوار' || t === 'سخت' || t === 'hard') return 3;
    if (t === '4' || t === 'خیلی دشوار' || t === 'بسیار دشوار' || t === 'veryhard') return 4;
    return null;
  }

  function findCategory(categories, name) {
    var t = norm(latinDigits(name));
    if (!t || t === 'همه' || t === 'هر' || t === 'all' || t === '*') return { id: null };
    /* نام دسته هم ممکن است رقم فارسی داشته باشد؛ هر دو طرف باید
     * یکسان‌سازی شوند وگرنه «فصل ۱» با ورودی «فصل 1» نمی‌خواند. */
    var exact = (categories || []).filter(function (c) { return norm(latinDigits(c.name)) === t; });
    if (exact.length === 1) return { id: exact[0].id };
    if (exact.length > 1) return { error: 'چند دسته با نام «' + name + '» هست؛ از مسیر کامل استفاده کنید.' };
    var byPath = (categories || []).filter(function (c) { return norm(latinDigits(categoryPath(categories, c.id))) === t; });
    if (byPath.length === 1) return { id: byPath[0].id };
    return { error: 'دستهٔ «' + name + '» پیدا نشد.' };
  }

  /* قالب هر خط:  دسته | دشواری | تعداد
   * نمونه:       فصل ۱ | آسان | ۵
   *              همه | هر | ۱۰
   * خط‌های خالی و # نادیده گرفته می‌شوند. */
  function parseBlueprint(text, categories) {
    var rules = [], errors = [];
    var lines = String(text == null ? '' : text).split('\n');
    for (var i = 0; i < lines.length; i++) {
      var raw = lines[i].trim();
      if (!raw || raw.charAt(0) === '#') continue;
      var parts = raw.split('|').map(function (x) { return x.trim(); });
      if (parts.length !== 3) {
        errors.push('خط ' + (i + 1) + ': قالب باید «دسته | دشواری | تعداد» باشد.');
        continue;
      }
      var cat = findCategory(categories || [], parts[0]);
      if (cat.error) { errors.push('خط ' + (i + 1) + ': ' + cat.error); continue; }
      var d = difficultyFromWord(parts[1]);
      if (d == null) { errors.push('خط ' + (i + 1) + ': دشواری باید ساده، متوسط، دشوار یا «هر» باشد.'); continue; }
      /* فقط رقم انگلیسیِ یکسان‌سازی‌شده پذیرفته می‌شود. Number در
       * جاوااسکریپت '0x10' را ۱۶ می‌خواند ولی کاتلین رد می‌کند؛
       * این سخت‌گیری دو طرف را هم‌رفتار می‌کند. */
      var digits = latinDigits(parts[2]);
      var n = /^[0-9]+$/.test(digits) ? Number(digits) : NaN;
      if (!isFinite(n) || Math.floor(n) !== n || n < 1 || n > MAX_COUNT) {
        errors.push('خط ' + (i + 1) + ': تعداد باید عددی بین ۱ تا ' + MAX_COUNT + ' باشد.');
        continue;
      }
      rules.push({ category: parts[0], categoryId: cat.id, difficulty: d, count: n });
      if (rules.length > MAX_RULES) { errors.push('تعداد قاعده‌ها بیش از ' + MAX_RULES + ' است.'); break; }
    }
    var total = rules.reduce(function (a, r) { return a + r.count; }, 0);
    if (total > MAX_PICK) errors.push('مجموع درخواست ' + total + ' سؤال است؛ سقف ' + MAX_PICK + ' است.');
    return { rules: rules, errors: errors, total: total };
  }

  function formatBlueprint(rules) {
    return (rules || []).map(function (r) {
      var d = r.difficulty < 0 ? 'هر' : DIFFICULTY_LABELS[r.difficulty];
      return (r.category || 'همه') + ' | ' + d + ' | ' + r.count;
    }).join('\n');
  }

  /* --- انتخاب ------------------------------------------------------ */

  /* مولد شبه‌تصادفی ۳۲ بیتی — عیناً در کاتلین تکرار می‌شود. */
  function rng(seed) {
    var s = (Number(seed) | 0) || 1;
    return function () {
      s = (Math.imul(s, 1103515245) + 12345) & 0x7fffffff;
      return s;
    };
  }

  function shuffled(arr, seed) {
    var a = arr.slice(), next = rng(seed);
    for (var i = a.length - 1; i > 0; i--) {
      var j = next() % (i + 1);
      var t = a[i]; a[i] = a[j]; a[j] = t;
    }
    return a;
  }

  /* قواعد به ترتیب نوشته‌شده اجرا می‌شوند؛ هیچ سؤالی دو بار برداشته
   * نمی‌شود. کمبود هر قاعده جدا گزارش می‌شود تا معلم بداند کدام
   * بخش بانک خالی است. */
  function pick(items, categories, rules, seed) {
    var used = {}, picked = [], perRule = [];
    (rules || []).forEach(function (r, idx) {
      var pool = filterItems(items, categories, {
        categoryId: r.categoryId,
        difficulty: r.difficulty < 0 ? null : r.difficulty
      }).filter(function (it) { return !used[String(it.id)]; });
      var order = shuffled(pool, (Number(seed) | 0) + idx * 7919);
      var take = order.slice(0, Math.min(r.count, order.length));
      take.forEach(function (it) { used[String(it.id)] = true; picked.push(it); });
      perRule.push({
        category: r.category || 'همه',
        difficulty: r.difficulty,
        wanted: r.count,
        got: take.length,
        shortage: Math.max(0, r.count - take.length)
      });
    });
    return { picked: picked, perRule: perRule, total: picked.length };
  }

  function summary(result) {
    var short = (result.perRule || []).filter(function (r) { return r.shortage > 0; });
    if (!short.length) return 'همهٔ ' + result.total + ' سؤال انتخاب شد.';
    return result.total + ' سؤال انتخاب شد؛ ' +
      short.map(function (r) { return r.category + ' (' + r.shortage + ' کمبود)' ; }).join('، ');
  }

  var API = {
    MAX_DEPTH: MAX_DEPTH, MAX_RULES: MAX_RULES, MAX_PICK: MAX_PICK, MAX_COUNT: MAX_COUNT,
    DIFFICULTY_LABELS: DIFFICULTY_LABELS,
    latinDigits: latinDigits,
    subtreeIds: subtreeIds, categoryPath: categoryPath, depthOf: depthOf,
    itemDifficulty: itemDifficulty, itemTopic: itemTopic, filterItems: filterItems,
    difficultyFromWord: difficultyFromWord, findCategory: findCategory,
    parseBlueprint: parseBlueprint, formatBlueprint: formatBlueprint,
    rng: rng, shuffled: shuffled, pick: pick, summary: summary
  };

  if (typeof module !== 'undefined' && module.exports) module.exports = API;
  root.BankPicker = API;
})(typeof window !== 'undefined' ? window : globalThis);
