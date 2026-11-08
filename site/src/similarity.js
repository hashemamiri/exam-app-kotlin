/* =====================================================================
 * V355 — بستهٔ ۲: تشخیص شباهت پاسخ‌ها + دستیار کلیدواژه‌ای تشریحی
 *
 * این پیمانه «خالص» است: نه شبکه، نه DOM، نه اجرای کد دانش‌آموز.
 * همان الگوریتم عیناً در اپ (SimilarityModels.kt) پیاده شده و بردارهای
 * آزمون مشترک هر دو را به هم می‌بندد؛ اگر یکی عوض شود، آزمون می‌شکند.
 *
 * هشدار مهم: شباهت فقط «پرچم هشدار» است. هیچ نمره‌ای خودکار کم نمی‌شود.
 * دو دانش‌آموز ممکن است واقعاً شبیه هم پاسخ دهند؛ تصمیم با معلم است.
 * ===================================================================== */
(function (root) {
  'use strict';

  var SHINGLE_K = 5;        // طول پنجرهٔ توکن
  var MIN_TOKENS = 8;       // پاسخ کوتاه‌تر از این مقایسه نمی‌شود
  var THRESHOLD = 0.75;     // آستانهٔ پیش‌فرض هشدار
  var STEP = 0.25;          // گِرد کردن نمرهٔ پیشنهادی

  /* کلیدواژه‌های مشترک زبان‌ها؛ شناسه‌های غیرکلیدواژه به ID تبدیل می‌شوند
     تا کپی با تغییر نام متغیر هم گرفته شود. */
  var KEYWORDS = ('abstract,and,as,assert,async,await,begin,bool,boolean,break,by,case,catch,char,' +
    'class,const,continue,def,defer,default,delete,do,double,elif,else,end,enum,except,export,' +
    'extends,extern,false,final,finally,float,fn,for,foreach,friend,from,func,function,global,' +
    'go,goto,if,impl,import,in,inline,int,interface,is,lambda,let,long,loop,match,mod,module,' +
    'mut,namespace,new,nil,none,not,null,nullptr,object,or,override,package,pass,private,' +
    'protected,public,raise,range,record,ref,return,select,self,short,signed,sizeof,static,' +
    'struct,super,switch,template,then,this,throw,throws,trait,true,try,type,typedef,typename,' +
    'union,unless,until,use,using,val,var,virtual,void,volatile,when,where,while,with,yield').split(',');
  var KW = Object.create(null);
  KEYWORDS.forEach(function (k) { KW[k] = true; });

  var LETTER = /[\p{L}\p{N}]/u;

  /** یکسان‌سازی نویسه‌های فارسی/عربی و رقم‌ها. */
  function foldChar(ch) {
    var c = ch.charCodeAt(0);
    if (c === 0x064A) return '\u06CC'; // ي → ی
    if (c === 0x0643) return '\u06A9'; // ك → ک
    if (c === 0x200C) return ' ';      // نیم‌فاصله → فاصله
    if (c >= 0x0660 && c <= 0x0669) return String.fromCharCode(48 + c - 0x0660);
    if (c >= 0x06F0 && c <= 0x06F9) return String.fromCharCode(48 + c - 0x06F0);
    return ch;
  }

  function normalizeText(s) {
    var src = String(s == null ? '' : s);
    var out = '';
    for (var i = 0; i < src.length; i++) {
      var ch = foldChar(src.charAt(i)).toLowerCase();
      out += LETTER.test(ch) ? ch : ' ';
    }
    return out.replace(/\s+/g, ' ').trim();
  }

  function essayTokens(s) {
    var n = normalizeText(s);
    return n ? n.split(' ') : [];
  }

  /** برداشتن کد از قالب سه‌بک‌تیک؛ اگر نبود، همان متن. */
  function stripFence(text) {
    var src = String(text == null ? '' : text);
    var re = /```[A-Za-z0-9_+#.\-]*[ \t]*\r?\n([\s\S]*?)```/g;
    var m, parts = [];
    while ((m = re.exec(src))) parts.push(m[1]);
    return parts.length ? parts.join('\n') : src;
  }

  /* فضای خالی دقیقاً مانند \s جاوااسکریپت؛ در JVM معنای \s فرق دارد،
     پس در هر دو زبان همین فهرست صریح به کار می‌رود. */
  function isWs(c) {
    var x = c.charCodeAt(0);
    return (x >= 0x09 && x <= 0x0D) || x === 0x20 || x === 0x85 || x === 0xA0 ||
      x === 0x1680 || (x >= 0x2000 && x <= 0x200A) || x === 0x2028 || x === 0x2029 ||
      x === 0x202F || x === 0x205F || x === 0x3000;
  }

  function isDigit(c) { return c >= '0' && c <= '9'; }
  function isIdentStart(c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c === '_' || c === '$';
  }
  function isIdentPart(c) { return isIdentStart(c) || isDigit(c); }

  /** حذف توضیحات کد تا توضیح متفاوت، شباهت را پنهان نکند. پویش دستی. */
  function stripComments(text) {
    var s = String(text == null ? '' : text);
    var out = '', i = 0, n = s.length;
    while (i < n) {
      var c = s.charAt(i);
      if (c === '/' && s.charAt(i + 1) === '*') {
        var close = s.indexOf('*/', i + 2);
        i = close < 0 ? n : close + 2;
        out += ' ';
        continue;
      }
      if ((c === '/' && s.charAt(i + 1) === '/') || c === '#') {
        while (i < n && s.charAt(i) !== '\n' && s.charAt(i) !== '\r') i++;
        out += ' ';
        continue;
      }
      out += c;
      i++;
    }
    return out;
  }

  /**
   * توکن‌سازی کد با پویش دستی (بدون regex) تا سایت و اپ دقیقاً یکی باشند.
   * رشته → STR، عدد → NUM، شناسهٔ غیرکلیدواژه → ID،و بقیهٔ نویسه‌ها عیناً.
   */
  function codeTokens(s) {
    var src = stripComments(stripFence(s));
    var out = [], i = 0, n = src.length;
    while (i < n) {
      var c = src.charAt(i);
      if (isWs(c)) { i++; continue; }
      if (c === '"' || c === "'") {
        var q = c; i++;
        while (i < n) {
          if (src.charAt(i) === '\\') { i += 2; continue; }
          if (src.charAt(i) === q) { i++; break; }
          i++;
        }
        out.push('STR');
        continue;
      }
      if (isDigit(c)) {
        while (i < n && isDigit(src.charAt(i))) i++;
        if (src.charAt(i) === '.' && isDigit(src.charAt(i + 1))) {
          i++;
          while (i < n && isDigit(src.charAt(i))) i++;
        }
        out.push('NUM');
        continue;
      }
      if (isIdentStart(c)) {
        var j = i;
        while (i < n && isIdentPart(src.charAt(i))) i++;
        var low = src.slice(j, i).toLowerCase();
        out.push(KW[low] ? low : 'ID');
        continue;
      }
      out.push(c);
      i++;
    }
    return out;
  }

  /** همان هش ۳۲ بیتی سایت/اپ: x = x*31 + کد نویسه. */
  function hash32(s) {
    var x = 0;
    var str = String(s == null ? '' : s);
    for (var i = 0; i < str.length; i++) x = (x * 31 + str.charCodeAt(i)) | 0;
    return x;
  }

  function shingles(tokens, k) {
    var n = k || SHINGLE_K;
    var set = Object.create(null);
    var arr = [];
    if (!tokens || !tokens.length) return arr;
    if (tokens.length < n) {
      var h0 = hash32(tokens.join('\u0001'));
      return [h0];
    }
    for (var i = 0; i + n <= tokens.length; i++) {
      var h = hash32(tokens.slice(i, i + n).join('\u0001'));
      if (!set[h]) { set[h] = true; arr.push(h); }
    }
    return arr;
  }

  function compare(aTokens, bTokens, minTokens) {
    var min = minTokens == null ? MIN_TOKENS : minTokens;
    if (!aTokens || !bTokens) return null;
    if (aTokens.length < min || bTokens.length < min) return null;
    var A = shingles(aTokens), B = shingles(bTokens);
    if (!A.length || !B.length) return null;
    var inA = Object.create(null);
    A.forEach(function (h) { inA[h] = true; });
    var shared = 0;
    B.forEach(function (h) { if (inA[h]) shared++; });
    var union = A.length + B.length - shared;
    var jac = union > 0 ? shared / union : 0;
    var small = Math.min(A.length, B.length);
    var con = small > 0 ? shared / small : 0;
    return {
      jaccard: jac, containment: con, score: Math.max(jac, con),
      shared: shared, sizeA: A.length, sizeB: B.length
    };
  }

  /** کلید پایدار جفت پاسخ؛ ترتیب شناسه‌ها مهم نیست. */
  function pairKey(a, b) {
    var x = String(a == null ? '' : a), y = String(b == null ? '' : b);
    return x <= y ? x + '|' + y : y + '|' + x;
  }

  /** تشخیص کد بودن پاسخ: قالب سه‌بک‌تیک یا نوع سؤال کد. */
  function isCodeAnswer(question, raw) {
    if (question && (question.mode === 'code' || question.type === 'code')) return true;
    return /```[A-Za-z0-9_+#.\-]*[ \t]*\r?\n/.test(String(raw == null ? '' : raw));
  }

  function plain(raw) {
    if (raw == null) return '';
    if (typeof raw === 'string') return raw;
    if (typeof raw === 'object') {
      if (typeof raw.text === 'string') return raw.text;
      if (typeof raw.answer === 'string') return raw.answer;
      return '';
    }
    return String(raw);
  }

  /** پاسخ یک زیربخش از رشتهٔ JSON سؤال چندبخشی. */
  function partText(raw, partId, withReason) {
    var obj = raw;
    if (typeof raw === 'string') {
      try { obj = JSON.parse(raw); } catch (e) { return ''; }
    }
    if (!obj || typeof obj !== 'object' || !obj.parts) return '';
    var entry = obj.parts[partId];
    if (!entry) return '';
    var t = String(entry.answer || '');
    if (withReason && entry.reason) t += ' ' + String(entry.reason);
    return t;
  }

  /**
   * ساخت واحدهای مقایسه از پاسخ‌های یک آزمون.
   * هر واحد = یک سؤال یا یک زیربخش، با توکن‌های هر دانش‌آموز.
   * پاسخ خالی یا خیلی کوتاه کنار گذاشته می‌شود تا هشدار الکی ندهد.
   */
  function extractUnits(questions, answers, minTokens) {
    var min = minTokens == null ? MIN_TOKENS : minTokens;
    var units = [];
    (questions || []).forEach(function (q, qi) {
      var parts = (q && q.assessmentParts) || [];
      if (parts.length) {
        parts.forEach(function (p) {
          var byAnswer = {};
          (answers || []).forEach(function (a) {
            var raw = (a.responses || [])[qi];
            var text = partText(raw, p.id, !!p.requiresReason);
            var tk = essayTokens(text);
            if (tk.length >= min) byAnswer[a.id] = tk;
          });
          if (Object.keys(byAnswer).length >= 2) {
            units.push({ questionIndex: qi, partKey: String(p.id), kind: 'essay', byAnswer: byAnswer });
          }
        });
        return;
      }
      var byAnswer = {}, kinds = {};
      (answers || []).forEach(function (a) {
        var raw = (a.responses || [])[qi];
        var code = isCodeAnswer(q, plain(raw));
        var tk = code ? codeTokens(plain(raw)) : essayTokens(plain(raw));
        if (tk.length >= min) { byAnswer[a.id] = tk; kinds[a.id] = code ? 'code' : 'essay'; }
      });
      var ids = Object.keys(byAnswer);
      if (ids.length >= 2) {
        var allCode = ids.every(function (id) { return kinds[id] === 'code'; });
        units.push({ questionIndex: qi, partKey: '', kind: allCode ? 'code' : 'essay', byAnswer: byAnswer });
      }
    });
    return units;
  }

  /** یافتن جفت‌های مشکوک؛ خروجی بر اساس امتیاز نزولی مرتب است. */
  function findSimilar(units, opts) {
    var o = opts || {};
    var th = o.threshold == null ? THRESHOLD : o.threshold;
    var out = [];
    (units || []).forEach(function (u) {
      var ids = Object.keys(u.byAnswer);
      for (var i = 0; i < ids.length; i++) {
        for (var j = i + 1; j < ids.length; j++) {
          var r = compare(u.byAnswer[ids[i]], u.byAnswer[ids[j]], 0);
          if (!r || r.score < th) continue;
          out.push({
            pairKey: pairKey(ids[i], ids[j]),
            aId: ids[i] <= ids[j] ? ids[i] : ids[j],
            bId: ids[i] <= ids[j] ? ids[j] : ids[i],
            questionIndex: u.questionIndex, partKey: u.partKey, kind: u.kind,
            jaccard: r.jaccard, containment: r.containment, score: r.score
          });
        }
      }
    });
    out.sort(function (x, y) {
      if (y.score !== x.score) return y.score - x.score;
      if (x.questionIndex !== y.questionIndex) return x.questionIndex - y.questionIndex;
      return x.pairKey < y.pairKey ? -1 : (x.pairKey > y.pairKey ? 1 : 0);
    });
    return out;
  }

  /** آیا دنبالهٔ توکن‌های کلیدواژه پشت‌سرهم در پاسخ آمده است؟ */
  function containsSeq(hay, needle) {
    if (!needle.length || needle.length > hay.length) return false;
    for (var i = 0; i + needle.length <= hay.length; i++) {
      var okAll = true;
      for (var j = 0; j < needle.length; j++) {
        if (hay[i + j] !== needle[j]) { okAll = false; break; }
      }
      if (okAll) return true;
    }
    return false;
  }

  /** کلیدواژه می‌تواند چند بدل داشته باشد: «وتر|ضلع بزرگ». */
  function keywordMatch(keyword, answerTokens) {
    var alts = String(keyword == null ? '' : keyword).split('|');
    for (var i = 0; i < alts.length; i++) {
      var tk = essayTokens(alts[i]);
      if (tk.length && containsSeq(answerTokens, tk)) return true;
    }
    return false;
  }

  function roundStep(v) { return Math.round(v / STEP) * STEP; }

  /**
   * پیشنهاد نمره بر پایهٔ کلیدواژه‌های وزن‌دار.
   * این فقط پیشنهاد است؛ ذخیره نمی‌شود و معلم آن را می‌پذیرد یا ویرایش می‌کند.
   */
  function essayAssist(keywords, answerText, maxScore) {
    var tk = essayTokens(answerText);
    var list = keywords || [];
    var total = 0, got = 0, matched = [], missing = [];
    list.forEach(function (k) {
      var w = Number(k.weight == null ? 1 : k.weight);
      if (!(w > 0)) w = 1;
      total += w;
      if (keywordMatch(k.keyword, tk)) { got += w; matched.push(k.keyword); }
      else missing.push(k.keyword);
    });
    var ratio = total > 0 ? got / total : 0;
    var max = Number(maxScore) || 0;
    var suggested = Math.max(0, Math.min(max, roundStep(max * ratio)));
    return { matched: matched, missing: missing, ratio: ratio, suggested: suggested, weightTotal: total, weightGot: got };
  }

  var api = {
    SHINGLE_K: SHINGLE_K, MIN_TOKENS: MIN_TOKENS, THRESHOLD: THRESHOLD, STEP: STEP,
    normalizeText: normalizeText, essayTokens: essayTokens, codeTokens: codeTokens,
    stripFence: stripFence, stripComments: stripComments, hash32: hash32,
    shingles: shingles, compare: compare, pairKey: pairKey,
    isCodeAnswer: isCodeAnswer, partText: partText,
    extractUnits: extractUnits, findSimilar: findSimilar,
    keywordMatch: keywordMatch, essayAssist: essayAssist
  };

  root.ExamSimilarity = api;
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
})(typeof window !== 'undefined' ? window : globalThis);
