/* ===================================================================
   V348 — ابزار کدنویسی (محیط شبیه‌سازی ویرایشگر)

   چه هست: یک ویرایشگر کد با رنگی‌شدن دستورها، شمارهٔ خط، تورفتگی خودکار و
   انتخاب زبان. استاد در متن سؤال کد می‌گذارد و دانش‌آموز در پاسخ کد می‌نویسد.

   چه نیست: کد اجرا نمی‌شود. این فقط محیط نوشتن و نمایش است؛ تصحیح مثل
   سؤال تشریحی دستی انجام می‌شود. هیچ کدی به سرور فرستاده یا اجرا نمی‌شود.

   قالب ذخیره‌سازی: بلوک استاندارد ``` که هم برای انسان خواناست و هم اگر
   جایی به‌صورت متن خام نشان داده شود (مثلاً اپ اندروید) بی‌معنی نمی‌شود:
       ```python
       print("سلام")
       ```
   به همین دلیل هیچ تغییر پایگاه داده لازم نیست.
   =================================================================== */
(function () {
  var S = window.ExamSite;
  if (!S) return;
  var el = S.el, esc = S.esc, toast = S.toast;
  var MAX = 20000; /* سقف نویسهٔ کد تا پاسخ‌ها و چاپ سنگین نشوند */

  /* زبان‌ها: نام، نشان کوتاه، رنگ، پسوند، توضیح کوتاه و نمونهٔ آغاز */
  var LANGS = [
    {id: 'python', name: 'پایتون', short: 'Py', color: '#3776AB', ext: 'py', indent: 4, line: '#', block: null,
     kw: 'def class return if elif else for while in not and or is None True False import from as with try except finally raise lambda yield pass break continue global nonlocal assert del async await print',
     sample: 'def greet(name):\n    return f"سلام {name}"\n\nprint(greet("دنیا"))\n'},
    {id: 'javascript', name: 'جاوااسکریپت', short: 'JS', color: '#F7DF1E', ext: 'js', indent: 2, line: '//', block: ['/*', '*/'],
     kw: 'function return if else for while do switch case break continue const let var class new this typeof instanceof in of try catch finally throw async await import export from default null undefined true false delete void yield extends super static get set',
     sample: 'function greet(name) {\n  return `سلام ${name}`;\n}\n\nconsole.log(greet("دنیا"));\n'},
    {id: 'typescript', name: 'تایپ‌اسکریپت', short: 'TS', color: '#3178C6', ext: 'ts', indent: 2, line: '//', block: ['/*', '*/'],
     kw: 'function return if else for while const let var class new this interface type enum implements extends public private protected readonly static async await import export from as null undefined true false number string boolean any unknown never void',
     sample: 'function greet(name: string): string {\n  return `سلام ${name}`;\n}\n\nconsole.log(greet("دنیا"));\n'},
    {id: 'java', name: 'جاوا', short: 'Jv', color: '#E76F00', ext: 'java', indent: 4, line: '//', block: ['/*', '*/'],
     kw: 'public private protected class interface extends implements static final void new this super return if else for while do switch case break continue try catch finally throw throws import package abstract synchronized volatile transient int long double float boolean char byte String null true false',
     sample: 'public class Main {\n    public static void main(String[] args) {\n        System.out.println("سلام دنیا");\n    }\n}\n'},
    {id: 'c', name: 'سی', short: 'C', color: '#5C6BC0', ext: 'c', indent: 4, line: '//', block: ['/*', '*/'],
     kw: 'include define int long short char float double void unsigned signed signed const static struct union enum typedef sizeof return if else for while do switch case break continue goto extern register volatile NULL',
     sample: '#include <stdio.h>\n\nint main(void) {\n    printf("سلام دنیا\\n");\n    return 0;\n}\n'},
    {id: 'cpp', name: 'سی‌پلاس‌پلاس', short: 'C++', color: '#00599C', ext: 'cpp', indent: 4, line: '//', block: ['/*', '*/'],
     kw: 'include define int long char float double bool void class struct public private protected virtual override new delete this template typename namespace using const constexpr static return if else for while do switch case break continue try catch throw nullptr true false auto',
     sample: '#include <iostream>\n\nint main() {\n    std::cout << "سلام دنیا" << std::endl;\n    return 0;\n}\n'},
    {id: 'csharp', name: 'سی‌شارپ', short: 'C#', color: '#68217A', ext: 'cs', indent: 4, line: '//', block: ['/*', '*/'],
     kw: 'using namespace class struct interface enum public private protected internal static readonly const void var new this base return if else for foreach while do switch case break continue try catch finally throw async await int string bool double float decimal null true false override virtual sealed partial get set',
     sample: 'using System;\n\nclass Program {\n    static void Main() {\n        Console.WriteLine("سلام دنیا");\n    }\n}\n'},
    {id: 'php', name: 'پی‌اچ‌پی', short: 'PHP', color: '#777BB4', ext: 'php', indent: 4, line: '//', block: ['/*', '*/'],
     kw: 'function return if else elseif for foreach while do switch case break continue class new this public private protected static const echo print require include require_once include_once use namespace try catch finally throw array null true false extends implements interface trait',
     sample: '<?php\n\nfunction greet(string $name): string {\n    return "سلام " . $name;\n}\n\necho greet("دنیا");\n'},
    {id: 'go', name: 'گو', short: 'Go', color: '#00ADD8', ext: 'go', indent: 4, line: '//', block: ['/*', '*/'],
     kw: 'package import func return if else for range switch case default break continue go defer chan select map struct interface type var const nil true false string int int64 float64 bool byte rune error make new len cap append',
     sample: 'package main\n\nimport "fmt"\n\nfunc main() {\n    fmt.Println("سلام دنیا")\n}\n'},
    {id: 'kotlin', name: 'کاتلین', short: 'Kt', color: '#7F52FF', ext: 'kt', indent: 4, line: '//', block: ['/*', '*/'],
     kw: 'fun val var return if else when for while do break continue class object interface data sealed open override private internal public protected companion init constructor import package this super null true false is in as by lazy suspend coroutine try catch finally throw typealias',
     sample: 'fun greet(name: String): String {\n    return "سلام $name"\n}\n\nfun main() {\n    println(greet("دنیا"))\n}\n'},
    {id: 'sql', name: 'اس‌کیوال', short: 'SQL', color: '#E38C00', ext: 'sql', indent: 2, line: '--', block: ['/*', '*/'],
     kw: 'select from where insert into values update set delete create table alter drop index view join inner left right full outer on group by order having limit offset union all distinct as and or not null is in between like exists case when then else end primary key foreign references default constraint count sum avg min max',
     sample: 'SELECT name, score\nFROM students\nWHERE score >= 10\nORDER BY score DESC;\n'},
    {id: 'r', name: 'آر', short: 'R', color: '#276DC3', ext: 'r', indent: 2, line: '#', block: null,
     kw: 'function return if else for while repeat break next in TRUE FALSE NULL NA Inf NaN library require source print c list data.frame matrix vector factor length nrow ncol names summary mean median sd',
     sample: 'greet <- function(name) {\n  paste("سلام", name)\n}\n\nprint(greet("دنیا"))\n'}
  ];
  var BY_ID = {};
  LANGS.forEach(function (L) { BY_ID[L.id] = L; });
  /* نام‌های جایگزین که کاربر ممکن است در بلوک بنویسد */
  var ALIAS = {py: 'python', 'python3': 'python', js: 'javascript', node: 'javascript', ts: 'typescript',
    'c++': 'cpp', cxx: 'cpp', cc: 'cpp', 'c#': 'csharp', cs: 'csharp', dotnet: 'csharp', kt: 'kotlin', kts: 'kotlin',
    golang: 'go', postgres: 'sql', postgresql: 'sql', mysql: 'sql', sqlite: 'sql', php8: 'php', rlang: 'r'};
  function lang(id) {
    var k = String(id || '').trim().toLowerCase();
    return BY_ID[k] || BY_ID[ALIAS[k]] || BY_ID.python;
  }

  /* ---------------- رنگی‌کردن کد ---------------- */
  /* یک گذر روی متن: توضیح، رشته، عدد، کلیدواژه، تابع. خروجی HTML امن است
     چون هر تکه پیش از افزودن esc می‌شود. */
  function highlight(code, langId) {
    var L = lang(langId);
    var src = String(code == null ? '' : code);
    var kw = {};
    L.kw.split(/\s+/).forEach(function (w) { if (w) kw[L.id === 'sql' ? w.toLowerCase() : w] = 1; });
    var out = '', i = 0, n = src.length;
    function push(cls, text) { out += cls ? '<span class="cx-' + cls + '">' + esc(text) + '</span>' : esc(text); }
    var isWord = function (c) { return /[A-Za-z0-9_$\u00C0-\u024F]/.test(c || ''); };
    while (i < n) {
      var c = src[i], two = src.substr(i, 2);
      /* توضیح بلوکی */
      if (L.block && two === L.block[0]) {
        var end = src.indexOf(L.block[1], i + 2);
        end = end < 0 ? n : end + L.block[1].length;
        push('cm', src.slice(i, end)); i = end; continue;
      }
      /* توضیح خطی */
      if (L.line && src.substr(i, L.line.length) === L.line) {
        var nl = src.indexOf('\n', i); if (nl < 0) nl = n;
        push('cm', src.slice(i, nl)); i = nl; continue;
      }
      /* رشته‌ها */
      if (c === '"' || c === "'" || c === '`') {
        var j = i + 1;
        while (j < n) {
          if (src[j] === '\\') { j += 2; continue; }
          if (src[j] === c) { j++; break; }
          if (src[j] === '\n' && c !== '`') break;
          j++;
        }
        push('st', src.slice(i, j)); i = j; continue;
      }
      /* عدد */
      if (/[0-9]/.test(c) && !isWord(src[i - 1])) {
        var m = /^(0[xXbBoO][0-9a-fA-F_]+|[0-9][0-9_]*(\.[0-9_]+)?([eE][+-]?[0-9]+)?)/.exec(src.slice(i));
        if (m) { push('nu', m[0]); i += m[0].length; continue; }
      }
      /* واژه: کلیدواژه یا فراخوانی تابع */
      if (isWord(c) && !isWord(src[i - 1])) {
        var w = /^[\w$\u00C0-\u024F]+/.exec(src.slice(i))[0];
        var probe = L.id === 'sql' ? w.toLowerCase() : w;
        if (kw[probe]) push('kw', w);
        else if (/^\s*\(/.test(src.slice(i + w.length))) push('fn', w);
        else if (/^[A-Z]/.test(w)) push('ty', w);
        else push('', w);
        i += w.length; continue;
      }
      /* پیش‌پردازنده و متغیرهای زبان‌هایی مثل پی‌اچ‌پی */
      if ((c === '#' && L.id !== 'python' && L.id !== 'r') || c === '$') {
        var v = /^[#$][\w]+/.exec(src.slice(i));
        if (v) { push('pp', v[0]); i += v[0].length; continue; }
      }
      if (/[{}()[\];,.]/.test(c)) { push('pn', c); i++; continue; }
      if (/[+\-*/%=<>!&|^~?:]/.test(c)) { push('op', c); i++; continue; }
      push('', c); i++;
    }
    return out;
  }

  /* ---------------- بلوک نمایش (فقط‌خواندنی) ---------------- */
  function blockHtml(code, langId) {
    var L = lang(langId), src = String(code == null ? '' : code).replace(/\s+$/, '');
    var lines = src.split('\n');
    var nums = lines.map(function (_, i) { return i + 1; }).join('\n');
    return '<div class="cx-block" dir="ltr" data-lang="' + esc(L.id) + '">'
      + '<div class="cx-bar"><span class="cx-badge" style="--cx:' + L.color + '">' + esc(L.short) + '</span>'
      + '<span class="cx-name">' + esc(L.name) + '</span><span class="cx-grow"></span>'
      + '<span class="cx-meta">' + esc(String(lines.length)) + ' lines</span>'
      + '<button type="button" class="cx-copy" data-cx-copy="1">کپی</button></div>'
      + '<div class="cx-body"><pre class="cx-num" aria-hidden="true">' + esc(nums) + '</pre>'
      + '<pre class="cx-code"><code>' + highlight(src, L.id) + '</code></pre></div></div>';
  }

  /* ---------------- قالب ذخیره‌سازی ---------------- */
  var FENCE = /```([A-Za-z0-9_+#.\-]*)[ \t]*\r?\n([\s\S]*?)```/g;
  function fence(code, langId) {
    var L = lang(langId), body = String(code == null ? '' : code).replace(/\s+$/, '');
    /* اگر خود کد ``` داشته باشد، بلوک را نمی‌شکنیم؛ نویسهٔ کنترل را جایگزین می‌کنیم */
    body = body.replace(/```/g, '\u02CB\u02CB\u02CB');
    return '```' + L.id + '\n' + body + '\n```';
  }
  function parseFence(text) {
    var m = /```([A-Za-z0-9_+#.\-]*)[ \t]*\r?\n([\s\S]*?)```/.exec(String(text || ''));
    if (!m) return null;
    return {lang: lang(m[1]).id, code: m[2].replace(/\u02CB\u02CB\u02CB/g, '```').replace(/\s+$/, '')};
  }
  function hasFence(text) { return /```[A-Za-z0-9_+#.\-]*[ \t]*\r?\n/.test(String(text || '')); }
  /* متن را به تکه‌های متن/کد می‌شکند تا هر بخش با رندر درست خودش نشان داده شود */
  function split(text) {
    var src = String(text == null ? '' : text), parts = [], last = 0, m;
    FENCE.lastIndex = 0;
    while ((m = FENCE.exec(src))) {
      if (m.index > last) parts.push({code: false, text: src.slice(last, m.index)});
      parts.push({code: true, lang: lang(m[1]).id, text: m[2].replace(/\u02CB\u02CB\u02CB/g, '```')});
      last = m.index + m[0].length;
    }
    if (last < src.length) parts.push({code: false, text: src.slice(last)});
    return parts;
  }
  function html(text) {
    return split(text).map(function (p) {
      return p.code ? blockHtml(p.text, p.lang) : '<span class="cx-text">' + esc(p.text).replace(/\n/g, '<br>') + '</span>';
    }).join('');
  }
  /* کپی با یک کلیک روی هر بلوک کد در سایت */
  document.addEventListener('click', function (e) {
    var b = e.target && e.target.closest ? e.target.closest('[data-cx-copy]') : null;
    if (!b) return;
    e.preventDefault(); e.stopPropagation();
    var blk = b.closest('.cx-block'), pre = blk && blk.querySelector('.cx-code');
    var txt = pre ? pre.textContent : '';
    if (!txt) return;
    var done = function () { b.textContent = 'کپی شد'; setTimeout(function () { b.textContent = 'کپی'; }, 1400); };
    if (navigator.clipboard && navigator.clipboard.writeText) navigator.clipboard.writeText(txt).then(done, function () { toast('کپی نشد.', 'err'); });
    else {
      try { var ta = el('textarea', {style: 'position:fixed;opacity:0'}); ta.value = txt; document.body.appendChild(ta); ta.select(); document.execCommand('copy'); ta.remove(); done(); }
      catch (x) { toast('کپی نشد.', 'err'); }
    }
  });

  /* ---------------- انتخاب زبان ---------------- */
  function pickLanguage(onPick, opts) {
    opts = opts || {};
    var bg = el('div', {class: 'modal-bg'});
    function close() { bg.remove(); }
    bg._onCancel = close;
    var grid = el('div', {class: 'cx-grid'}, LANGS.map(function (L) {
      return el('button', {type: 'button', class: 'cx-tile', style: '--cx:' + L.color, onclick: function () { close(); onPick(L.id); }}, [
        el('span', {class: 'cx-tile-badge', text: L.short}),
        el('span', {class: 'cx-tile-name', text: L.name}),
        el('span', {class: 'cx-tile-ext', text: '.' + L.ext})
      ]);
    }));
    var m = el('div', {class: 'modal wide cx-modal'}, [
      el('button', {class: 'x', text: '✕', onclick: close}),
      el('h2', {text: opts.title || 'زبان برنامه‌نویسی را انتخاب کنید'}),
      el('p', {class: 'muted', text: opts.hint || 'محیط نوشتن کد با رنگی‌شدن دستورها و شمارهٔ خط باز می‌شود. کد اجرا نمی‌شود؛ فقط نوشته و ذخیره می‌شود.'}),
      grid
    ]);
    bg.appendChild(m);
    bg.addEventListener('click', function (e) { if (e.target === bg) close(); });
    document.body.appendChild(bg);
    return close;
  }

  /* ---------------- ویرایشگر ---------------- */
  function open(o) {
    o = o || {};
    var L = lang(o.lang);
    var readOnly = !!o.readOnly;
    var startCode = o.code != null && String(o.code) !== '' ? String(o.code) : (o.starter != null ? String(o.starter) : (o.blank ? '' : L.sample));
    var bg = el('div', {class: 'modal-bg cx-editor-bg'});
    var dirty = false, closed = false;

    var gutter = el('pre', {class: 'cx-num', 'aria-hidden': 'true'});
    var pre = el('pre', {class: 'cx-code cx-hl', 'aria-hidden': 'true'});
    var codeEl = el('code');
    pre.appendChild(codeEl);
    var ta = el('textarea', {class: 'cx-ta', spellcheck: 'false', autocapitalize: 'off', autocomplete: 'off',
      autocorrect: 'off', wrap: 'off', dir: 'ltr', maxlength: String(MAX)});
    ta.value = startCode;
    if (readOnly) ta.setAttribute('readonly', 'readonly');
    var pane = el('div', {class: 'cx-pane'}, [gutter, pre, ta]);

    var badge = el('span', {class: 'cx-badge', text: L.short});
    var nameEl = el('span', {class: 'cx-name', text: L.name});
    var pos = el('span', {class: 'cx-meta', text: ''});
    var langBtn = el('button', {type: 'button', class: 'btn light sm', text: 'تغییر زبان'});
    var bar = el('div', {class: 'cx-bar'}, [badge, nameEl, el('span', {class: 'cx-grow'}), pos, langBtn]);

    function setLang(id) {
      L = lang(id);
      badge.textContent = L.short; badge.style.setProperty('--cx', L.color);
      nameEl.textContent = L.name;
      bar.style.setProperty('--cx', L.color);
      paint();
    }
    function paint() {
      var v = ta.value;
      codeEl.innerHTML = highlight(v, L.id) + '\n';
      var count = v.split('\n').length;
      var nums = [];
      for (var i = 1; i <= count; i++) nums.push(i);
      gutter.textContent = nums.join('\n');
      var upto = v.slice(0, ta.selectionStart || 0).split('\n');
      pos.textContent = 'Ln ' + upto.length + ', Col ' + (upto[upto.length - 1].length + 1) + ' · ' + v.length + '/' + MAX;
      sync();
    }
    function sync() { pre.scrollTop = ta.scrollTop; pre.scrollLeft = ta.scrollLeft; gutter.scrollTop = ta.scrollTop; }

    ta.addEventListener('input', function () { dirty = true; paint(); });
    ta.addEventListener('scroll', sync);
    ta.addEventListener('click', paint);
    ta.addEventListener('keyup', function (e) { if (e.key && e.key.indexOf('Arrow') === 0) paint(); });
    ta.addEventListener('keydown', function (e) {
      if (readOnly) return;
      var pad = new Array(L.indent + 1).join(' ');
      var s = ta.selectionStart, t = ta.selectionEnd, v = ta.value;
      /* Tab / Shift+Tab: تورفتگی یک یا چند خط */
      if (e.key === 'Tab') {
        e.preventDefault();
        var lineStart = v.lastIndexOf('\n', s - 1) + 1;
        if (e.shiftKey) {
          var block = v.slice(lineStart, t);
          var out = block.split('\n').map(function (ln) { return ln.replace(new RegExp('^ {1,' + L.indent + '}'), ''); }).join('\n');
          ta.value = v.slice(0, lineStart) + out + v.slice(t);
          ta.selectionStart = Math.max(lineStart, s - (block.length - out.length > 0 ? Math.min(L.indent, block.length - out.length) : 0));
          ta.selectionEnd = ta.selectionStart + out.length;
        } else if (s !== t && v.slice(s, t).indexOf('\n') >= 0) {
          var seg = v.slice(lineStart, t);
          var indented = seg.split('\n').map(function (ln) { return pad + ln; }).join('\n');
          ta.value = v.slice(0, lineStart) + indented + v.slice(t);
          ta.selectionStart = s + L.indent; ta.selectionEnd = t + (indented.length - seg.length);
        } else {
          ta.value = v.slice(0, s) + pad + v.slice(t);
          ta.selectionStart = ta.selectionEnd = s + L.indent;
        }
        dirty = true; paint(); return;
      }
      /* Enter: تورفتگی خط قبل حفظ می‌شود؛ بعد از { یا : یک پله بیشتر */
      if (e.key === 'Enter') {
        e.preventDefault();
        var ls = v.lastIndexOf('\n', s - 1) + 1;
        var cur = v.slice(ls, s);
        var lead = (/^[ \t]*/.exec(cur) || [''])[0];
        var extra = /[{:(\[]\s*$/.test(cur) ? pad : '';
        var closing = /^[ \t]*[})\]]/.test(v.slice(t)) && extra;
        var ins = '\n' + lead + extra + (closing ? '\n' + lead : '');
        ta.value = v.slice(0, s) + ins + v.slice(t);
        ta.selectionStart = ta.selectionEnd = s + 1 + lead.length + extra.length;
        dirty = true; paint(); return;
      }
      /* بستن خودکار پرانتز و کوتیشن */
      var pairs = {'(': ')', '[': ']', '{': '}'};
      if (pairs[e.key] && s === t) {
        e.preventDefault();
        ta.value = v.slice(0, s) + e.key + pairs[e.key] + v.slice(t);
        ta.selectionStart = ta.selectionEnd = s + 1;
        dirty = true; paint(); return;
      }
      if ((e.ctrlKey || e.metaKey) && (e.key === 's' || e.key === 'S')) { e.preventDefault(); save(); return; }
    });

    function close() { if (closed) return; closed = true; bg.remove(); if (o.onCancel) o.onCancel(); }
    function save() {
      if (closed) return;
      var v = ta.value;
      if (v.length > MAX) return toast('کد بلندتر از حد مجاز است.', 'err');
      closed = true; bg.remove();
      if (o.onDone) o.onDone({lang: L.id, code: v});
    }
    function askClose() {
      if (!dirty || readOnly) return close();
      if (S.confirmDlg) S.confirmDlg('بستن ویرایشگر', 'تغییرها ذخیره نشده‌اند. بسته شود؟', 'بستن').then(function (ok) { if (ok) close(); });
      else close();
    }
    bg._onCancel = askClose;
    bg.addEventListener('click', function (e) { if (e.target === bg) askClose(); });
    langBtn.addEventListener('click', function () {
      pickLanguage(function (id) {
        var wasSample = ta.value === L.sample;
        setLang(id);
        if (wasSample && !readOnly) { ta.value = L.sample; dirty = true; paint(); }
      }, {title: 'تغییر زبان', hint: 'اگر کد خالی یا نمونه باشد، نمونهٔ زبان تازه گذاشته می‌شود؛ کد نوشته‌شده دست‌نخورده می‌ماند.'});
    });

    var foot = el('div', {class: 'row cx-foot'}, [
      el('span', {class: 'muted cx-note', text: readOnly ? 'فقط نمایش — این کد پاسخ دانش‌آموز است.' : 'کد اجرا نمی‌شود؛ فقط نوشته و ذخیره می‌شود. تصحیح دستی است.'}),
      el('span', {class: 'grow'}),
      el('button', {type: 'button', class: 'btn light', text: readOnly ? 'بستن' : 'انصراف', onclick: askClose}),
      readOnly ? null : el('button', {type: 'button', class: 'btn', text: o.doneLabel || 'ذخیرهٔ کد', onclick: save})
    ]);
    var m = el('div', {class: 'modal wide cx-modal cx-editor'}, [
      el('button', {class: 'x', text: '✕', onclick: askClose}),
      el('h2', {text: o.title || 'محیط کدنویسی'}),
      bar, pane, foot
    ]);
    bg.appendChild(m);
    document.body.appendChild(bg);
    setLang(L.id);
    paint();
    if (!readOnly) setTimeout(function () { try { ta.focus(); ta.selectionStart = ta.selectionEnd = ta.value.length; } catch (x) {} }, 30);
    return {close: close, save: save};
  }

  /* باز کردن با انتخاب زبان (همان چیزی که کاربر خواست: لمس ابزار → فهرست زبان‌ها → محیط) */
  function openWithPicker(o) {
    o = o || {};
    if (o.lang) return open(o);
    return pickLanguage(function (id) { open(Object.assign({}, o, {lang: id})); }, o);
  }

  window.SiteCode = {
    LANGS: LANGS, lang: lang, highlight: highlight, blockHtml: blockHtml, html: html, split: split,
    fence: fence, parseFence: parseFence, hasFence: hasFence, open: open, openWithPicker: openWithPicker,
    pickLanguage: pickLanguage, MAX: MAX
  };
})();
